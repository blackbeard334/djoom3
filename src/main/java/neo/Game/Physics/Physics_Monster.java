package neo.Game.Physics;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Actor.idActor;
import static neo.Game.Entity.TH_PHYSICS;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.ENTITYNUM_NONE;
import static neo.Game.Game_local.ENTITYNUM_WORLD;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Physics.Physics.CONTACT_EPSILON;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_Actor.idPhysics_Actor;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_BLOCKED;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_FALLING;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_OK;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_SLIDING;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_STEPPED;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import neo.idlib.BitMsg.idBitMsgDelta;
import static neo.idlib.math.Math_h.MS2SEC;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import neo.idlib.math.Vector.idVec3;

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
    };

    public static class monsterPState_s {

        int     atRest;
        boolean onGround;
        idVec3  origin;
        idVec3  velocity;
        idVec3  localOrigin;
        idVec3  pushVelocity;
    };
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
        private idVec3              delta;            // delta for next move
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
            current = new monsterPState_s();
            current.atRest = -1;
            saved = current;

            delta = new idVec3();
            maxStepHeight = 18.0f;
            minFloorCosine = 0.7f;
            moveResult = MM_OK;
            forceDeltaMove = false;
            fly = false;
            useVelocityMove = false;
            noImpact = false;
            blockingEntity = null;
        }

        @Override
        public void Save(idSaveGame savefile) {

            idPhysics_Monster_SavePState(savefile, current);
            idPhysics_Monster_SavePState(savefile, saved);

            savefile.WriteFloat(maxStepHeight);
            savefile.WriteFloat(minFloorCosine);
            savefile.WriteVec3(delta);

            savefile.WriteBool(forceDeltaMove);
            savefile.WriteBool(fly);
            savefile.WriteBool(useVelocityMove);
            savefile.WriteBool(noImpact);

            savefile.WriteInt(etoi(moveResult));
            savefile.WriteObject(blockingEntity);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            idPhysics_Monster_RestorePState(savefile, current);
            idPhysics_Monster_RestorePState(savefile, saved);

            maxStepHeight = savefile.ReadFloat();
            minFloorCosine = savefile.ReadFloat();
            savefile.ReadVec3(delta);

            forceDeltaMove = savefile.ReadBool();
            fly = savefile.ReadBool();
            useVelocityMove = savefile.ReadBool();
            noImpact = savefile.ReadBool();

            moveResult = monsterMoveResult_t.values()[savefile.ReadInt()];
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/blockingEntity);
        }

        // maximum step up the monster can take, default 18 units
        public void SetMaxStepHeight(final float newMaxStepHeight) {
            maxStepHeight = newMaxStepHeight;
        }

        public float GetMaxStepHeight() {
            return maxStepHeight;
        }
//
//        // minimum cosine of floor angle to be able to stand on the floor
//        public void SetMinFloorCosine(final float newMinFloorCosine);
//

        // set delta for next move
        public void SetDelta(final idVec3 d) {
            delta = d;
            if (!delta.equals(getVec3_origin())) {
                Activate();
            }
        }

        // returns true if monster is standing on the ground
        public boolean OnGround() {
            return current.onGround;
        }

        // returns the movement result
        public monsterMoveResult_t GetMoveResult() {
            return moveResult;
        }

        // overrides any velocity for pure delta movement
        public void ForceDeltaMove(boolean force) {
            forceDeltaMove = force;
        }

        // whether velocity should be affected by gravity
        public void UseFlyMove(boolean force) {
            fly = force;
        }

        // don't use delta movement
        public void UseVelocityMove(boolean force) {
            useVelocityMove = force;
        }

        // get entity blocking the move
        public idEntity GetSlideMoveEntity() {
            return blockingEntity;
        }

        // enable/disable activation by impact
        public void EnableImpact() {
            noImpact = false;
        }

        public void DisableImpact() {
            noImpact = true;
        }

        // common physics interface
        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            idVec3 masterOrigin = new idVec3(), oldOrigin;
            idMat3 masterAxis = new idMat3();
            float timeStep;

            timeStep = MS2SEC(timeStepMSec);

            moveResult = MM_OK;
            blockingEntity = null;
            oldOrigin = current.origin;

            // if bound to a master
            if (masterEntity != null) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.origin = masterOrigin.oPlus(current.localOrigin.oMultiply(masterAxis));
                clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());
                current.velocity = (current.origin.oMinus(oldOrigin)).oDivide(timeStep);
                masterDeltaYaw = masterYaw;
                masterYaw = masterAxis.oGet(0).ToYaw();
                masterDeltaYaw = masterYaw - masterDeltaYaw;
                return true;
            }

            // if the monster is at rest
            if (current.atRest >= 0) {
                return false;
            }

            ActivateContactEntities();

            // move the monster velocity into the frame of a pusher
            current.velocity.oMinSet(current.pushVelocity);

            clipModel.Unlink();

            // check if on the ground
            this.CheckGround(current);

            // if not on the ground or moving upwards
            float upspeed;
            if (!gravityNormal.equals(getVec3_zero())) {
                upspeed = -(current.velocity.oMultiply(gravityNormal));
            } else {
                upspeed = current.velocity.z;
            }
            if (fly || (!forceDeltaMove && (!current.onGround || upspeed > 1.0f))) {
                if (upspeed < 0.0f) {
                    moveResult = MM_FALLING;
                } else {
                    current.onGround = false;
                    moveResult = MM_OK;
                }
                delta = current.velocity.oMultiply(timeStep);
                if (!delta.equals(getVec3_origin())) {
                    moveResult = this.SlideMove(current.origin, current.velocity, delta);
                    delta.Zero();
                }

                if (!fly) {
                    current.velocity.oPluSet(gravityVector.oMultiply(timeStep));
                }
            } else {
                if (useVelocityMove) {
                    delta = current.velocity.oMultiply(timeStep);
                } else {
                    current.velocity = delta.oDivide(timeStep);
                }

                current.velocity.oMinSet(current.velocity.oMultiply(gravityNormal.oMultiply(gravityNormal)));

                if (delta.equals(getVec3_origin())) {
                    Rest();
                } else {
                    // try moving into the desired direction
                    moveResult = this.StepMove(current.origin, current.velocity, delta
                    );
                    delta.Zero();
                }
            }

            clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());

            // get all the ground contacts
            EvaluateContacts();

            // move the monster velocity back into the world frame
            current.velocity.oPluSet(current.pushVelocity);
            current.pushVelocity.Zero();

            if (IsOutsideWorld()) {
                gameLocal.Warning("clip model outside world bounds for entity '%s' at (%s)", self.name, current.origin.ToString(0));
                Rest();
            }

            return (!current.origin.equals(oldOrigin));
        }

        @Override
        public void UpdateTime(int endTimeMSec) {
        }

        @Override
        public int GetTime() {
            return gameLocal.time;
        }

        @Override
        public void GetImpactInfo(final int id, final idVec3 point, impactInfo_s info) {
            info.invMass = invMass;
            info.invInertiaTensor.Zero();
            info.position.Zero();
            info.velocity = current.velocity;
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
            if (noImpact) {
                return;
            }
            current.velocity.oPluSet(impulse.oMultiply(invMass));
            Activate();
        }

        @Override
        public void Activate() {
            current.atRest = -1;
            self.BecomeActive(TH_PHYSICS);
        }

        @Override
        public void PutToRest() {
            current.atRest = gameLocal.time;
            current.velocity.Zero();
            self.BecomeInactive(TH_PHYSICS);
        }

        @Override
        public boolean IsAtRest() {
            return current.atRest >= 0;
        }

        @Override
        public int GetRestStartTime() {
            return current.atRest;
        }

        @Override
        public void SaveState() {
            saved = current;
        }

        @Override
        public void RestoreState() {
            current = saved;

            clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());

            EvaluateContacts();
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.localOrigin = newOrigin;
            if (masterEntity != null) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.origin = masterOrigin.oPlus(newOrigin.oMultiply(masterAxis));
            } else {
                current.origin = newOrigin;
            }
            clipModel.Link(gameLocal.clip, self, 0, newOrigin, clipModel.GetAxis());
            Activate();
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            clipModel.Link(gameLocal.clip, self, 0, clipModel.GetOrigin(), newAxis);
            Activate();
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {

            current.localOrigin.oPluSet(translation);
            current.origin.oPluSet(translation);
            clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());
            Activate();
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.origin.oMulSet(rotation);
            if (masterEntity != null) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.localOrigin = (current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose());
            } else {
                current.localOrigin = current.origin;
            }
            clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis().oMultiply(rotation.ToMat3()));
            Activate();
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            current.velocity = newLinearVelocity;
            Activate();
        }

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            return current.velocity;
        }

        @Override
        public void SetPushed(int deltaTime) {
            // velocity with which the monster is pushed
            current.pushVelocity.oPluSet((current.origin.oMinus(saved.origin)).oDivide(deltaTime * idMath.M_MS2SEC));
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return current.pushVelocity;
        }

        /*
         ================
         idPhysics_Monster::SetMaster

         the binding is never orientated
         ================
         */
        @Override
        public void SetMaster(idEntity master, final boolean orientated /*= true*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (null == masterEntity) {
                    // transform from world space to master space
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    current.localOrigin = (current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose());
                    masterEntity = master;
                    masterYaw = masterAxis.oGet(0).ToYaw();
                }
                ClearContacts();
            } else {
                if (masterEntity != null) {
                    masterEntity = null;
                    Activate();
                }
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteFloat(current.origin.oGet(0));
            msg.WriteFloat(current.origin.oGet(1));
            msg.WriteFloat(current.origin.oGet(2));
            msg.WriteFloat(current.velocity.oGet(0), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteFloat(current.velocity.oGet(1), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteFloat(current.velocity.oGet(2), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(current.origin.oGet(0), current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(current.origin.oGet(1), current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(current.origin.oGet(2), current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(0), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(1), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(2), MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS);
            msg.WriteLong(current.atRest);
            msg.WriteBits(btoi(current.onGround), 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            current.origin.oSet(0, msg.ReadFloat());
            current.origin.oSet(1, msg.ReadFloat());
            current.origin.oSet(2, msg.ReadFloat());
            current.velocity.oSet(0, msg.ReadFloat(MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            current.velocity.oSet(1, msg.ReadFloat(MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            current.velocity.oSet(2, msg.ReadFloat(MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            current.localOrigin.oSet(0, msg.ReadDeltaFloat(current.origin.oGet(0)));
            current.localOrigin.oSet(1, msg.ReadDeltaFloat(current.origin.oGet(1)));
            current.localOrigin.oSet(2, msg.ReadDeltaFloat(current.origin.oGet(2)));
            current.pushVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, MONSTER_VELOCITY_EXPONENT_BITS, MONSTER_VELOCITY_MANTISSA_BITS));
            current.atRest = msg.ReadLong();
            current.onGround = msg.ReadBits(1) != 0;
        }

        private void CheckGround(monsterPState_s state) {
            trace_s[] groundTrace = {null};
            idVec3 down;

            if (gravityNormal.equals(getVec3_zero())) {
                state.onGround = false;
                groundEntityPtr = null;
                return;
            }

            down = state.origin.oPlus(gravityNormal.oMultiply(CONTACT_EPSILON));
            gameLocal.clip.Translation(groundTrace, state.origin, down, clipModel, clipModel.GetAxis(), clipMask, self);

            if (groundTrace[0].fraction == 1.0f) {
                state.onGround = false;
                groundEntityPtr = null;
                return;
            }

            groundEntityPtr.oSet(gameLocal.entities[groundTrace[0].c.entityNum]);

            if ((groundTrace[0].c.normal.oMultiply(gravityNormal.oNegative())) < minFloorCosine) {
                state.onGround = false;
                return;
            }

            state.onGround = true;

            // let the entity know about the collision
            self.Collide(groundTrace[0], state.velocity);

            // apply impact to a non world floor entity
            if (groundTrace[0].c.entityNum != ENTITYNUM_WORLD && groundEntityPtr.GetEntity() != null) {
                impactInfo_s info = new impactInfo_s();
                groundEntityPtr.GetEntity().GetImpactInfo(self, groundTrace[0].c.id, groundTrace[0].c.point, info);
                if (info.invMass != 0.0f) {
                    groundEntityPtr.GetEntity().ApplyImpulse(self, 0, groundTrace[0].c.point, state.velocity.oDivide(info.invMass * 10.0f));
                }
            }
        }

        private monsterMoveResult_t SlideMove(idVec3 start, idVec3 velocity, final idVec3 delta) {
            int i;
            trace_s[] tr = {null};
            idVec3 move;

            blockingEntity = null;
            move = delta;
            for (i = 0; i < 3; i++) {
                gameLocal.clip.Translation(tr, start, start.oPlus(move), clipModel, clipModel.GetAxis(), clipMask, self);

                start.oSet(tr[0].endpos);

                if (tr[0].fraction == 1.0f) {
                    if (i > 0) {
                        return MM_SLIDING;
                    }
                    return MM_OK;
                }

                if (tr[0].c.entityNum != ENTITYNUM_NONE) {
                    blockingEntity = gameLocal.entities[ tr[0].c.entityNum];
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
            trace_s[] tr = {null};
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
                if (gravityNormal.equals(getVec3_zero())) {
                    start.oSet(noStepPos);
                    return MM_OK;
                }

                // try to step down so that we walk down slopes and stairs at a normal rate
                down = noStepPos.oPlus(gravityNormal.oMultiply(maxStepHeight));
                gameLocal.clip.Translation(tr, noStepPos, down, clipModel, clipModel.GetAxis(), clipMask, self);
                if (tr[0].fraction < 1.0f) {
                    start.oSet(tr[0].endpos);
                    return MM_STEPPED;
                } else {
                    start.oSet(noStepPos);
                    return MM_OK;
                }
            }

            if (blockingEntity != null && blockingEntity.IsType(idActor.class)) {
                // try to step down in case walking into an actor while going down steps
                down = noStepPos.oPlus(gravityNormal.oMultiply(maxStepHeight));
                gameLocal.clip.Translation(tr, noStepPos, down, clipModel, clipModel.GetAxis(), clipMask, self);
                start.oSet(tr[0].endpos);
                velocity.oSet(noStepVel);
                return MM_BLOCKED;
            }

            if (gravityNormal.equals(getVec3_zero())) {
                return result1;
            }

            // try to step up
            up = start.oMinus(gravityNormal.oMultiply(maxStepHeight));
            gameLocal.clip.Translation(tr, start, up, clipModel, clipModel.GetAxis(), clipMask, self);
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
            down = stepPos.oPlus(gravityNormal.oMultiply(maxStepHeight));
            gameLocal.clip.Translation(tr, stepPos, down, clipModel, clipModel.GetAxis(), clipMask, self);
            stepPos = tr[0].endpos;

            // if the move is further without stepping up, or the slope is too steap, don't step up
            nostepdist = (noStepPos.oMinus(start)).LengthSqr();
            stepdist = (stepPos.oMinus(start)).LengthSqr();
            if ((nostepdist >= stepdist) || ((tr[0].c.normal.oMultiply(gravityNormal.oNegative())) < minFloorCosine)) {
                start.oSet(noStepPos);
                velocity.oSet(noStepVel);
                return MM_SLIDING;
            }

            start.oSet(stepPos);
            velocity.oSet(stepVel);

            return MM_STEPPED;
        }

        private void Rest() {
            current.atRest = gameLocal.time;
            current.velocity.Zero();
            self.BecomeInactive(TH_PHYSICS);
        }
    };

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
        boolean[] onGround = {false};
        int[] atRest = {0};

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
