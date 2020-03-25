package neo.Game.Physics;

import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Game_local.ENTITYNUM_NONE;
import static neo.Game.Game_local.ENTITYNUM_WORLD;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Physics.Physics.CONTACT_EPSILON;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_BLOCKED;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_FALLING;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_OK;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_SLIDING;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_STEPPED;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Actor.idActor;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_Actor.idPhysics_Actor;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Physics_Monster {

    /*
     ===================================================================================

     Monster physics

     Simulates the motion of a monster through the environment. The monster motion
     is typically driven by animations.

     ===================================================================================
     */
    public enum monsterMoveResult_t {

        MM_OK,
        MM_SLIDING,
        MM_BLOCKED,
        MM_STEPPED,
        MM_FALLING
    }

    private static class monsterPState_s {

        int     atRest;
        boolean onGround;
        idVec3  origin;
        idVec3  velocity;
        idVec3  localOrigin;
        idVec3  pushVelocity;

        public monsterPState_s(){
            this.origin = new idVec3();
            this.velocity = new idVec3();
            this.localOrigin = new idVec3();
            this.pushVelocity = new idVec3();
        }
    }
    static final float OVERCLIP                       = 1.001f;
    //
    static final float MONSTER_VELOCITY_MAX           = 4000;
    static final int   MONSTER_VELOCITY_TOTAL_BITS    = 16;
    static final int   MONSTER_VELOCITY_EXPONENT_BITS = idMath.BitsForInteger(idMath.BitsForFloat(MONSTER_VELOCITY_MAX)) + 1;
    static final int   MONSTER_VELOCITY_MANTISSA_BITS = MONSTER_VELOCITY_TOTAL_BITS - 1 - MONSTER_VELOCITY_EXPONENT_BITS;
//

    public static class idPhysics_Monster extends idPhysics_Actor {
        // CLASS_PROTOTYPE( idPhysics_Monster );

        // monster physics state
        private monsterPState_s     current;
        private monsterPState_s     saved;
        //
        // properties
        private float               maxStepHeight;    // maximum step height
        private float               minFloorCosine;   // minimum cosine of floor angle
        private final idVec3              delta;            // delta for next move
        //
        private boolean             forceDeltaMove;
        private boolean             fly;
        private boolean             useVelocityMove;
        private boolean             noImpact;         // if true do not activate when another object collides
        //
        // results of last evaluate
        private monsterMoveResult_t moveResult;
        private idEntity            blockingEntity;
        //
        //

        public idPhysics_Monster() {

//	memset( &current, 0, sizeof( current ) );
            this.current = new monsterPState_s();
            this.current.atRest = -1;
            this.saved = this.current;

            this.delta = new idVec3();
            this.maxStepHeight = 18.0f;
            this.minFloorCosine = 0.7f;
            this.moveResult = MM_OK;
            this.forceDeltaMove = false;
            this.fly = false;
            this.useVelocityMove = false;
            this.noImpact = false;
            this.blockingEntity = null;
        }

        @Override
        public void Save(idSaveGame savefile) {

            idPhysics_Monster_SavePState(savefile, this.current);
            idPhysics_Monster_SavePState(savefile, this.saved);

            savefile.WriteFloat(this.maxStepHeight);
            savefile.WriteFloat(this.minFloorCosine);
            savefile.WriteVec3(this.delta);

            savefile.WriteBool(this.forceDeltaMove);
            savefile.WriteBool(this.fly);
            savefile.WriteBool(this.useVelocityMove);
            savefile.WriteBool(this.noImpact);

            savefile.WriteInt(etoi(this.moveResult));
            savefile.WriteObject(this.blockingEntity);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            idPhysics_Monster_RestorePState(savefile, this.current);
            idPhysics_Monster_RestorePState(savefile, this.saved);

            this.maxStepHeight = savefile.ReadFloat();
            this.minFloorCosine = savefile.ReadFloat();
            savefile.ReadVec3(this.delta);

            this.forceDeltaMove = savefile.ReadBool();
            this.fly = savefile.ReadBool();
            this.useVelocityMove = savefile.ReadBool();
            this.noImpact = savefile.ReadBool();

            this.moveResult = monsterMoveResult_t.values()[savefile.ReadInt()];
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/blockingEntity);
        }

        // maximum step up the monster can take, default 18 units
        public void SetMaxStepHeight(final float newMaxStepHeight) {
            this.maxStepHeight = newMaxStepHeight;
        }

        public float GetMaxStepHeight() {
            return this.maxStepHeight;
        }
//
//        // minimum cosine of floor angle to be able to stand on the floor
//        public void SetMinFloorCosine(final float newMinFloorCosine);
//

        // set delta for next move
        public void SetDelta(final idVec3 d) {
            this.delta.oSet(d);
            if (!this.delta.equals(getVec3_origin())) {
                Activate();
            }
        }

        // returns true if monster is standing on the ground
        public boolean OnGround() {
            return this.current.onGround;
        }

        // returns the movement result
        public monsterMoveResult_t GetMoveResult() {
            return this.moveResult;
        }

        // overrides any velocity for pure delta movement
        public void ForceDeltaMove(boolean force) {
            this.forceDeltaMove = force;
        }

        // whether velocity should be affected by gravity
        public void UseFlyMove(boolean force) {
            this.fly = force;
        }

        // don't use delta movement
        public void UseVelocityMove(boolean force) {
            this.useVelocityMove = force;
        }

        // get entity blocking the move
        public idEntity GetSlideMoveEntity() {
            return this.blockingEntity;
        }

        // enable/disable activation by impact
        public void EnableImpact() {
            this.noImpact = false;
        }

        public void DisableImpact() {
            this.noImpact = true;
        }

        private static int DBG_Evaluate = 0;
        // common physics interface
        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {   DBG_Evaluate++;
            final idVec3 masterOrigin = new idVec3();
			idVec3 oldOrigin;
            final idMat3 masterAxis = new idMat3();
            float timeStep;

            timeStep = MS2SEC(timeStepMSec);

            this.moveResult = MM_OK;
            this.blockingEntity = null;
            oldOrigin = this.current.origin;

            // if bound to a master
            if (this.masterEntity != null) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.origin.oSet(masterOrigin.oPlus(this.current.localOrigin.oMultiply(masterAxis)));
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());
                this.current.velocity.oSet((this.current.origin.oMinus(oldOrigin)).oDivide(timeStep));
                this.masterDeltaYaw = this.masterYaw;
                this.masterYaw = masterAxis.oGet(0).ToYaw();
                this.masterDeltaYaw = this.masterYaw - this.masterDeltaYaw;
                return true;
            }

            // if the monster is at rest
            if (this.current.atRest >= 0) {
                return false;
            }

            ActivateContactEntities();

            // move the monster velocity into the frame of a pusher
            this.current.velocity.oMinSet(this.current.pushVelocity);

            this.clipModel.Unlink();

            // check if on the ground
            this.CheckGround(this.current);

            // if not on the ground or moving upwards
            float upspeed;
            if (!this.gravityNormal.equals(getVec3_zero())) {
                upspeed = -(this.current.velocity.oMultiply(this.gravityNormal));
            } else {
                upspeed = this.current.velocity.z;
            }
            if (this.fly || (!this.forceDeltaMove && (!this.current.onGround || (upspeed > 1.0f)))) {
                if (upspeed < 0.0f) {
                    this.moveResult = MM_FALLING;
                } else {
                    this.current.onGround = false;
                    this.moveResult = MM_OK;
                }
                this.delta.oSet(this.current.velocity.oMultiply(timeStep));
                if (!this.delta.equals(getVec3_origin())) {
                    this.moveResult = this.SlideMove(this.current.origin, this.current.velocity, this.delta);
                    this.delta.Zero();
                }

                if (!this.fly) {
                    this.current.velocity.oPluSet(this.gravityVector.oMultiply(timeStep));
                }
            } else {
                if (this.useVelocityMove) {
                    this.delta.oSet(this.current.velocity.oMultiply(timeStep));
                } else {
                    this.current.velocity.oSet(this.delta.oDivide(timeStep));
                }

                this.current.velocity.oMinSet(this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal)));

                if (this.delta.equals(getVec3_origin())) {
                    Rest();
                } else {
                    // try moving into the desired direction
                    this.moveResult = this.StepMove(this.current.origin, this.current.velocity, this.delta);
                    this.delta.Zero();
                }
            }

            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());

            // get all the ground contacts
            EvaluateContacts();

            // move the monster velocity back into the world frame
            this.current.velocity.oPluSet(this.current.pushVelocity);
            this.current.pushVelocity.Zero();

            if (IsOutsideWorld()) {
                gameLocal.Warning("clip model outside world bounds for entity '%s' at (%s)", this.self.name, this.current.origin.ToString(0));
                Rest();
            }

            return (!this.current.origin.equals(oldOrigin));
        }

        @Override
        public void UpdateTime(int endTimeMSec) {
        }

        @Override
        public int GetTime() {
            return gameLocal.time;
        }

        @Override
        public impactInfo_s GetImpactInfo(final int id, final idVec3 point) {
            final impactInfo_s info = new impactInfo_s();
            info.invMass = this.invMass;
            info.invInertiaTensor.Zero();
            info.position.Zero();
            info.velocity.oSet(this.current.velocity);
            return info;
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
            if (this.noImpact) {
                return;
            }
            this.current.velocity.oPluSet(impulse.oMultiply(this.invMass));
            Activate();
        }

        @Override
        public void Activate() {
            this.current.atRest = -1;
            this.self.BecomeActive(TH_PHYSICS);
        }

        @Override
        public void PutToRest() {
            this.current.atRest = gameLocal.time;
            this.current.velocity.Zero();
            this.self.BecomeInactive(TH_PHYSICS);
        }

        @Override
        public boolean IsAtRest() {
            return this.current.atRest >= 0;
        }

        @Override
        public int GetRestStartTime() {
            return this.current.atRest;
        }

        @Override
        public void SaveState() {
            this.saved = this.current;
        }

        @Override
        public void RestoreState() {
            this.current = this.saved;

            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());

            EvaluateContacts();
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.localOrigin.oSet(newOrigin);
            if (this.masterEntity != null) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.origin.oSet(masterOrigin.oPlus(newOrigin.oMultiply(masterAxis)));
            } else {
                this.current.origin.oSet(newOrigin);
            }
            this.clipModel.Link(gameLocal.clip, this.self, 0, newOrigin, this.clipModel.GetAxis());
            Activate();
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            this.clipModel.Link(gameLocal.clip, this.self, 0, this.clipModel.GetOrigin(), newAxis);
            Activate();
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {

            this.current.localOrigin.oPluSet(translation);
            this.current.origin.oPluSet(translation);
            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());
            Activate();
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.origin.oMulSet(rotation);
            if (this.masterEntity != null) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.localOrigin.oSet((this.current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
            } else {
                this.current.localOrigin.oSet(this.current.origin);
            }
            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis().oMultiply(rotation.ToMat3()));
            Activate();
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            this.current.velocity.oSet(newLinearVelocity);
            Activate();
        }

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            return new idVec3(this.current.velocity);
        }

        @Override
        public void SetPushed(int deltaTime) {
            // velocity with which the monster is pushed
            this.current.pushVelocity.oPluSet((this.current.origin.oMinus(this.saved.origin)).oDivide(deltaTime * idMath.M_MS2SEC));
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return this.current.pushVelocity;
        }

        /*
         ================
         idPhysics_Monster::SetMaster

         the binding is never orientated
         ================
         */
        @Override
        public void SetMaster(idEntity master, final boolean orientated /*= true*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (null == this.masterEntity) {
                    // transform from world space to master space
                    this.self.GetMasterPosition(masterOrigin, masterAxis);
                    this.current.localOrigin.oSet((this.current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                    this.masterEntity = master;
                    this.masterYaw = masterAxis.oGet(0).ToYaw();
                }
                ClearContacts();
            } else {
                if (this.masterEntity != null) {
                    this.masterEntity = null;
                    Activate();
                }
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteFloat(this.current.origin.oGet(0));
            msg.WriteFloat(this.current.origin.oGet(1));
            msg.WriteFloat(this.current.origin.oGet(2));
            msg.WriteFloat(this.current.velocity.oGet(0), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteFloat(this.current.velocity.oGet(1), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteFloat(this.current.velocity.oGet(2), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(this.current.origin.oGet(0), this.current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(this.current.origin.oGet(1), this.current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(this.current.origin.oGet(2), this.current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(0), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(1), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(2), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteLong(this.current.atRest);
            msg.WriteBits(btoi(this.current.onGround), 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            this.current.origin.oSet(0, msg.ReadFloat());
            this.current.origin.oSet(1, msg.ReadFloat());
            this.current.origin.oSet(2, msg.ReadFloat());
            this.current.velocity.oSet(0, msg.ReadFloat(MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            this.current.velocity.oSet(1, msg.ReadFloat(MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            this.current.velocity.oSet(2, msg.ReadFloat(MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            this.current.localOrigin.oSet(0, msg.ReadDeltaFloat(this.current.origin.oGet(0)));
            this.current.localOrigin.oSet(1, msg.ReadDeltaFloat(this.current.origin.oGet(1)));
            this.current.localOrigin.oSet(2, msg.ReadDeltaFloat(this.current.origin.oGet(2)));
            this.current.pushVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            this.current.atRest = msg.ReadLong();
            this.current.onGround = msg.ReadBits(1) != 0;
        }

        private void CheckGround(monsterPState_s state) {
            final trace_s[] groundTrace = {null};
            idVec3 down;

            if (this.gravityNormal.equals(getVec3_zero())) {
                state.onGround = false;
                this.groundEntityPtr = new idEntityPtr<>(null);
                return;
            }

            down = state.origin.oPlus(this.gravityNormal.oMultiply(CONTACT_EPSILON));
            gameLocal.clip.Translation(groundTrace, state.origin, down, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);

            if (groundTrace[0].fraction == 1.0f) {
                state.onGround = false;
                this.groundEntityPtr = new idEntityPtr<>(null);
                return;
            }

            this.groundEntityPtr.oSet(gameLocal.entities[groundTrace[0].c.entityNum]);

            if ((groundTrace[0].c.normal.oMultiply(this.gravityNormal.oNegative())) < this.minFloorCosine) {
                state.onGround = false;
                return;
            }

            state.onGround = true;

            // let the entity know about the collision
            this.self.Collide(groundTrace[0], state.velocity);

            // apply impact to a non world floor entity
            if ((groundTrace[0].c.entityNum != ENTITYNUM_WORLD) && (this.groundEntityPtr.GetEntity() != null)) {
                final impactInfo_s info = this.groundEntityPtr.GetEntity().GetImpactInfo(this.self, groundTrace[0].c.id, groundTrace[0].c.point);
                if (info.invMass != 0.0f) {
                    this.groundEntityPtr.GetEntity().ApplyImpulse(this.self, 0, groundTrace[0].c.point, state.velocity.oDivide(info.invMass * 10.0f));
                }
            }
        }

        private monsterMoveResult_t SlideMove(idVec3 start, idVec3 velocity, final idVec3 delta) {
            int i;
            final trace_s[] tr = {null};
            final idVec3 move = new idVec3();

            this.blockingEntity = null;
            move.oSet(delta);
            for (i = 0; i < 3; i++) {
                gameLocal.clip.Translation(tr, start, start.oPlus(move), this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);

                start.oSet(tr[0].endpos);

                if (tr[0].fraction == 1.0f) {
                    if (i > 0) {
                        return MM_SLIDING;
                    }
                    return MM_OK;
                }

                if (tr[0].c.entityNum != ENTITYNUM_NONE) {
                    this.blockingEntity = gameLocal.entities[ tr[0].c.entityNum];
                }

                // clip the movement delta and velocity
                move.ProjectOntoPlane(tr[0].c.normal, OVERCLIP);
                velocity.ProjectOntoPlane(tr[0].c.normal, OVERCLIP);
            }

            return MM_BLOCKED;
        }

        /*
         =====================
         idPhysics_Monster::StepMove

         move start into the delta direction
         the velocity is clipped conform any collisions
         =====================
         */
        private monsterMoveResult_t StepMove(idVec3 start, idVec3 velocity, final idVec3 delta) {
            final trace_s[] tr = {null};
            idVec3 up, down, noStepPos, noStepVel, stepPos, stepVel;
            monsterMoveResult_t result1, result2;
            float stepdist;
            float nostepdist;

            if (delta.equals(getVec3_origin())) {
                return MM_OK;
            }

            // try to move without stepping up
            noStepPos = start;
            noStepVel = velocity;
            result1 = SlideMove(noStepPos, noStepVel, delta);
            if (result1 == MM_OK) {
                velocity.oSet(noStepVel);
                if (this.gravityNormal.equals(getVec3_zero())) {
                    start.oSet(noStepPos);
                    return MM_OK;
                }

                // try to step down so that we walk down slopes and stairs at a normal rate
                down = noStepPos.oPlus(this.gravityNormal.oMultiply(this.maxStepHeight));
                gameLocal.clip.Translation(tr, noStepPos, down, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
                if (tr[0].fraction < 1.0f) {
                    start.oSet(tr[0].endpos);
                    return MM_STEPPED;
                } else {
                    start.oSet(noStepPos);
                    return MM_OK;
                }
            }

            if ((this.blockingEntity != null) && this.blockingEntity.IsType(idActor.class)) {
                // try to step down in case walking into an actor while going down steps
                down = noStepPos.oPlus(this.gravityNormal.oMultiply(this.maxStepHeight));
                gameLocal.clip.Translation(tr, noStepPos, down, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
                start.oSet(tr[0].endpos);
                velocity.oSet(noStepVel);
                return MM_BLOCKED;
            }

            if (this.gravityNormal.equals(getVec3_zero())) {
                return result1;
            }

            // try to step up
            up = start.oMinus(this.gravityNormal.oMultiply(this.maxStepHeight));
            gameLocal.clip.Translation(tr, start, up, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
            if (tr[0].fraction == 0.0f) {
                start.oSet(noStepPos);
                velocity.oSet(noStepVel);
                return result1;
            }

            // try to move at the stepped up position
            stepPos = tr[0].endpos;
            stepVel = velocity;
            result2 = SlideMove(stepPos, stepVel, delta);
            if (result2 == MM_BLOCKED) {
                start.oSet(noStepPos);
                velocity.oSet(noStepVel);
                return result1;
            }

            // step down again
            down = stepPos.oPlus(this.gravityNormal.oMultiply(this.maxStepHeight));
            gameLocal.clip.Translation(tr, stepPos, down, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
            stepPos = tr[0].endpos;

            // if the move is further without stepping up, or the slope is too steap, don't step up
            nostepdist = (noStepPos.oMinus(start)).LengthSqr();
            stepdist = (stepPos.oMinus(start)).LengthSqr();
            if ((nostepdist >= stepdist) || ((tr[0].c.normal.oMultiply(this.gravityNormal.oNegative())) < this.minFloorCosine)) {
                start.oSet(noStepPos);
                velocity.oSet(noStepVel);
                return MM_SLIDING;
            }

            start.oSet(stepPos);
            velocity.oSet(stepVel);

            return MM_STEPPED;
        }

        private void Rest() {
            this.current.atRest = gameLocal.time;
            this.current.velocity.Zero();
            this.self.BecomeInactive(TH_PHYSICS);
        }
    }

    /*
     ================
     idPhysics_Monster_SavePState
     ================
     */
    static void idPhysics_Monster_SavePState(idSaveGame savefile, final monsterPState_s state) {
        savefile.WriteVec3(state.origin);
        savefile.WriteVec3(state.velocity);
        savefile.WriteVec3(state.localOrigin);
        savefile.WriteVec3(state.pushVelocity);
        savefile.WriteBool(state.onGround);
        savefile.WriteInt(state.atRest);
    }

    /*
     ================
     idPhysics_Monster_RestorePState
     ================
     */
    static void idPhysics_Monster_RestorePState(idRestoreGame savefile, monsterPState_s state) {
        final boolean[] onGround = {false};
        final int[] atRest = {0};

        savefile.ReadVec3(state.origin);
        savefile.ReadVec3(state.velocity);
        savefile.ReadVec3(state.localOrigin);
        savefile.ReadVec3(state.pushVelocity);
        savefile.ReadBool(onGround);
        savefile.ReadInt(atRest);

        state.onGround = onGround[0];
        state.atRest = atRest[0];
    }
}
