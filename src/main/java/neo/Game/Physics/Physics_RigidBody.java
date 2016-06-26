package neo.Game.Physics;

import neo.CM.CollisionModel;
import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import static neo.Game.Entity.TH_PHYSICS;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.rb_showActive;
import static neo.Game.GameSys.SysCvar.rb_showBodies;
import static neo.Game.GameSys.SysCvar.rb_showInertia;
import static neo.Game.GameSys.SysCvar.rb_showMass;
import static neo.Game.GameSys.SysCvar.rb_showTimings;
import static neo.Game.GameSys.SysCvar.rb_showVelocity;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import neo.Game.Physics.Clip.idClipModel;
import static neo.Game.Physics.Physics.CONTACT_EPSILON;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_Base.idPhysics_Base;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsgDelta;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Text.Str.va;
import neo.idlib.Timer.idTimer;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.FLOAT_IS_NAN;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.Min3Index;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.SkewSymmetric;
import static neo.idlib.math.Matrix.idMat3.TransposeMultiply;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Ode.deriveFunction_t;
import neo.idlib.math.Ode.idODE;
import neo.idlib.math.Ode.idODE_Euler;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec6;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

/**
 *
 */
public class Physics_RigidBody {

    /*
     ===================================================================================

     Rigid body physics

     Employs an impulse based dynamic simulation which is not very accurate but
     relatively fast and still reliable due to the continuous collision detection.

     ===================================================================================
     */
    public static final  float   RB_VELOCITY_MAX           = 16000;
    public static final  int     RB_VELOCITY_TOTAL_BITS    = 16;
    public static final  int     RB_VELOCITY_EXPONENT_BITS = idMath.BitsForInteger(idMath.BitsForFloat(RB_VELOCITY_MAX)) + 1;
    public static final  int     RB_VELOCITY_MANTISSA_BITS = RB_VELOCITY_TOTAL_BITS - 1 - RB_VELOCITY_EXPONENT_BITS;
    public static final  float   RB_MOMENTUM_MAX           = 1e20f;
    public static final  int     RB_MOMENTUM_TOTAL_BITS    = 16;
    public static final  int     RB_MOMENTUM_EXPONENT_BITS = idMath.BitsForInteger(idMath.BitsForFloat(RB_MOMENTUM_MAX)) + 1;
    public static final  int     RB_MOMENTUM_MANTISSA_BITS = RB_MOMENTUM_TOTAL_BITS - 1 - RB_MOMENTUM_EXPONENT_BITS;
    public static final  float   RB_FORCE_MAX              = 1e20f;
    public static final  int     RB_FORCE_TOTAL_BITS       = 16;
    public static final  int     RB_FORCE_EXPONENT_BITS    = idMath.BitsForInteger(idMath.BitsForFloat(RB_FORCE_MAX)) + 1;
    public static final  int     RB_FORCE_MANTISSA_BITS    = RB_FORCE_TOTAL_BITS - 1 - RB_FORCE_EXPONENT_BITS;
    //
    static final         float   STOP_SPEED                = 10.0f;
    //
    private static final boolean RB_TIMINGS                = false;
    private static final boolean TEST_COLLISION_DETECTION  = false;
    //
    static               int     lastTimerReset            = 0;
    static               int     numRigidBodies            = 0;
    static idTimer timer_total, timer_collision;
//

    public static class rigidBodyIState_s {

        public static final int BYTES
                = idVec3.BYTES
                + idMat3.BYTES
                + idVec3.BYTES
                + idVec3.BYTES;

        idVec3 position;                    // position of trace model
        idMat3 orientation;                 // orientation of trace model
        idVec3 linearMomentum;              // translational momentum relative to center of mass
        idVec3 angularMomentum;             // rotational momentum relative to center of mass

        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        private rigidBodyIState_s() {
            position = new idVec3();
            orientation = new idMat3();
            linearMomentum = new idVec3();
            angularMomentum = new idVec3();
        }
        
        private rigidBodyIState_s(float[] state) {
            this();
            this.fromFloats(state);
        }

        private float[] toFloats() {
            FloatBuffer buffer = FloatBuffer.allocate(BYTES / Float.BYTES);
            buffer.put(position.ToFloatPtr())
                    .put(orientation.ToFloatPtr())
                    .put(linearMomentum.ToFloatPtr())
                    .put(angularMomentum.ToFloatPtr());

            return buffer.array();
        }

        private void fromFloats(final float[] state) {
            FloatBuffer b = FloatBuffer.wrap(state);
            if (b.hasRemaining()) {
                position.oSet(new idVec3(b.get(), b.get(), b.get()));
            }
            if (b.hasRemaining()) {
                orientation.oSet(new idMat3(
                        b.get(), b.get(), b.get(),
                        b.get(), b.get(), b.get(),
                        b.get(), b.get(), b.get()));
            }
            if (b.hasRemaining()) {
                linearMomentum.oSet(new idVec3(b.get(), b.get(), b.get()));
            }
            if (b.hasRemaining()) {
                angularMomentum.oSet(new idVec3(b.get(), b.get(), b.get()));
            }
        }
    }

    public static class rigidBodyPState_s {

        int               atRest;           // set when simulation is suspended
        float             lastTimeStep;     // length of last time step
        idVec3            localOrigin;      // origin relative to master
        idMat3            localAxis;        // axis relative to master
        idVec6            pushVelocity;     // push velocity
        idVec3            externalForce;    // external force relative to center of mass
        idVec3            externalTorque;   // external torque relative to center of mass
        rigidBodyIState_s i;                // state used for integration

        public rigidBodyPState_s() {
            this.localOrigin = new idVec3();
            this.localAxis = new idMat3();
            this.pushVelocity = new idVec6();
            this.externalForce = new idVec3();
            this.externalTorque = new idVec3();
        }
    }

    public static class idPhysics_RigidBody extends idPhysics_Base {
        // CLASS_PROTOTYPE( idPhysics_RigidBody );

        // state of the rigid body
        private rigidBodyPState_s current;
        private rigidBodyPState_s saved;
        //
        // rigid body properties
        private float             linearFriction;       // translational friction
        private float             angularFriction;      // rotational friction
        private float             contactFriction;      // friction with contact surfaces
        private float             bouncyness;           // bouncyness
        private idClipModel       clipModel;            // clip model used for collision detection
        //
        // derived properties
        private float             mass;                 // mass of body
        private float             inverseMass;          // 1 / mass
        private idVec3            centerOfMass;         // center of mass of trace model
        private idMat3            inertiaTensor;        // mass distribution
        private idMat3            inverseInertiaTensor; // inverse inertia tensor
        //
        private idODE             integrator;           // integrator
        private boolean           dropToFloor;          // true if dropping to the floor and putting to rest
        private boolean           testSolid;            // true if testing for solid when dropping to the floor
        private boolean           noImpact;             // if true do not activate when another object collides
        private boolean           noContact;            // if true do not determine contacts and no contact friction
        //
        // master
        private boolean           hasMaster;
        private boolean           isOrientated;
        //
        //

        public idPhysics_RigidBody() {

            // set default rigid body properties
            SetClipMask(MASK_SOLID);
            SetBouncyness(0.6f);
            SetFriction(0.6f, 0.6f, 0.0f);
            clipModel = null;

//	memset( &current, 0, sizeof( current ) );
            current = new rigidBodyPState_s();

            current.atRest = -1;
            current.lastTimeStep = USERCMD_MSEC;
            current.i = new rigidBodyIState_s();
            current.i.orientation.oSet(getMat3_identity());

            saved = current;

            mass = 1.0f;
            inverseMass = 1.0f;
            centerOfMass = new idVec3();
            inertiaTensor = getMat3_identity();
            inverseInertiaTensor = idMat3.getMat3_identity();

            // use the least expensive euler integrator
            integrator = new idODE_Euler(rigidBodyIState_s.BYTES / Float.BYTES, RigidBodyDerivatives.INSTANCE, this);

            dropToFloor = false;
            noImpact = false;
            noContact = false;

            hasMaster = false;
            isOrientated = false;

            if (RB_TIMINGS) {
                lastTimerReset = 0;
            }
        }
        // ~idPhysics_RigidBody();

        @Override
        public void Save(idSaveGame savefile) {

            idPhysics_RigidBody_SavePState(savefile, current);
            idPhysics_RigidBody_SavePState(savefile, saved);

            savefile.WriteFloat(linearFriction);
            savefile.WriteFloat(angularFriction);
            savefile.WriteFloat(contactFriction);
            savefile.WriteFloat(bouncyness);
            savefile.WriteClipModel(clipModel);

            savefile.WriteFloat(mass);
            savefile.WriteFloat(inverseMass);
            savefile.WriteVec3(centerOfMass);
            savefile.WriteMat3(inertiaTensor);
            savefile.WriteMat3(inverseInertiaTensor);

            savefile.WriteBool(dropToFloor);
            savefile.WriteBool(testSolid);
            savefile.WriteBool(noImpact);
            savefile.WriteBool(noContact);

            savefile.WriteBool(hasMaster);
            savefile.WriteBool(isOrientated);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            idPhysics_RigidBody_RestorePState(savefile, current);
            idPhysics_RigidBody_RestorePState(savefile, saved);

            linearFriction = savefile.ReadFloat();
            angularFriction = savefile.ReadFloat();
            contactFriction = savefile.ReadFloat();
            bouncyness = savefile.ReadFloat();
            savefile.ReadClipModel(clipModel);

            mass = savefile.ReadFloat();
            inverseMass = savefile.ReadFloat();
            savefile.ReadVec3(centerOfMass);
            savefile.ReadMat3(inertiaTensor);
            savefile.ReadMat3(inverseInertiaTensor);

            dropToFloor = savefile.ReadBool();
            testSolid = savefile.ReadBool();
            noImpact = savefile.ReadBool();
            noContact = savefile.ReadBool();

            hasMaster = savefile.ReadBool();
            isOrientated = savefile.ReadBool();
        }

        // initialisation
        public void SetFriction(final float linear, final float angular, final float contact) {
            if (linear < 0.0f || linear > 1.0f
                    || angular < 0.0f || angular > 1.0f
                    || contact < 0.0f || contact > 1.0f) {
                return;
            }
            linearFriction = linear;
            angularFriction = angular;
            contactFriction = contact;
        }

        public void SetBouncyness(final float b) {
            if (b < 0.0f || b > 1.0f) {
                return;
            }
            bouncyness = b;
        }

        // same as above but drop to the floor first
        public void DropToFloor() {
            dropToFloor = true;
            testSolid = true;
        }

        // no contact determination and contact friction
        public void NoContact() {
            noContact = true;
        }

        // enable/disable activation by impact
        public void EnableImpact() {
            noImpact = false;
        }

        public void DisableImpact() {
            noImpact = true;
        }

        static final float MAX_INERTIA_SCALE = 10.0f;

        // common physics interface
        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
            int minIndex;
            idMat3 inertiaScale = new idMat3();

            assert (self != null);
            assert (model != null);                    // we need a clip model
            assert (model.IsTraceModel());    // and it should be a trace model
            assert (density > 0.0f);            // density should be valid

            if (clipModel != null && !clipModel.equals(model) && freeOld) {
//		delete clipModel;
                clipModel = null;
            }
            clipModel = model;
            clipModel.Link(gameLocal.clip, self, 0, current.i.position, current.i.orientation);

            {// get mass properties from the trace model
                float[] mass = {0};
                clipModel.GetMassProperties(density, mass, centerOfMass, inertiaTensor);
                this.mass = mass[0];
            }

            // check whether or not the clip model has valid mass properties
            if (mass <= 0.0f || FLOAT_IS_NAN(mass)) {
                gameLocal.Warning("idPhysics_RigidBody::SetClipModel: invalid mass for entity '%s' type '%s'",
                        self.name, self.GetType().getName());
                mass = 1.0f;
                centerOfMass.Zero();
                inertiaTensor.Identity();
            }

            // check whether or not the inertia tensor is balanced
            minIndex = Min3Index(inertiaTensor.oGet(0, 0), inertiaTensor.oGet(1, 1), inertiaTensor.oGet(2, 2));
            inertiaScale.Identity();
            inertiaScale.oSet(0, 0, inertiaTensor.oGet(0, 0) / inertiaTensor.oGet(minIndex, minIndex));
            inertiaScale.oSet(1, 1, inertiaTensor.oGet(1, 1) / inertiaTensor.oGet(minIndex, minIndex));
            inertiaScale.oSet(2, 2, inertiaTensor.oGet(2, 2) / inertiaTensor.oGet(minIndex, minIndex));

            if (inertiaScale.oGet(0, 0) > MAX_INERTIA_SCALE || inertiaScale.oGet(1, 1) > MAX_INERTIA_SCALE || inertiaScale.oGet(2, 2) > MAX_INERTIA_SCALE) {
                gameLocal.DWarning("idPhysics_RigidBody::SetClipModel: unbalanced inertia tensor for entity '%s' type '%s'",
                        self.name, self.GetType().getName());
                float min = inertiaTensor.oGet(minIndex, minIndex) * MAX_INERTIA_SCALE;
                inertiaScale.oSet((minIndex + 1) % 3, (minIndex + 1) % 3, min / inertiaTensor.oGet((minIndex + 1) % 3, (minIndex + 1) % 3));
                inertiaScale.oSet((minIndex + 2) % 3, (minIndex + 2) % 3, min / inertiaTensor.oGet((minIndex + 2) % 3, (minIndex + 2) % 3));
                inertiaTensor.oMulSet(inertiaScale);
            }

            inverseMass = 1.0f / mass;
            inverseInertiaTensor = inertiaTensor.Inverse().oMultiply(1.0f / 6.0f);

            current.i.linearMomentum.Zero();
            current.i.angularMomentum.Zero();
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            return clipModel;
        }

        @Override
        public int GetNumClipModels() {
            return 1;
        }

        @Override
        public void SetMass(float mass, int id /*= -1*/) {
            assert (mass > 0.0f);
            inertiaTensor.oMulSet(mass / this.mass);
            inverseInertiaTensor = inertiaTensor.Inverse().oMultiply(1.0f / 6.0f);
            this.mass = mass;
            inverseMass = 1.0f / mass;
        }

        @Override
        public float GetMass(int id /*= -1*/) {
            return mass;
        }

        @Override
        public void SetContents(int contents, int id /*= -1*/) {
            clipModel.SetContents(contents);
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            return clipModel.GetContents();
        }

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            return clipModel.GetBounds();
        }

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {
            return clipModel.GetAbsBounds();
        }


        /*
         ================
         idPhysics_RigidBody::Evaluate

         Evaluate the impulse based rigid body physics.
         When a collision occurs an impulse is applied at the moment of impact but
         the remaining time after the collision is ignored.
         ================
         */
        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            rigidBodyPState_s next;
            idAngles angles;
            trace_s[] collision = {new trace_s()};
            idVec3 impulse = new idVec3();
            idEntity ent;
            idVec3 oldOrigin, masterOrigin = new idVec3();
            idMat3 oldAxis, masterAxis = new idMat3();
            float timeStep;
            boolean collided, cameToRest = false;

            timeStep = MS2SEC(timeStepMSec);
            current.lastTimeStep = timeStep;

            if (hasMaster) {
                oldOrigin = new idVec3(current.i.position);
                oldAxis = new idMat3(current.i.orientation);
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.i.position.oSet(masterOrigin.oPlus(current.localOrigin.oMultiply(masterAxis)));
                if (isOrientated) {
                    current.i.orientation.oSet(current.localAxis.oMultiply(masterAxis));
                } else {
                    current.i.orientation.oSet(current.localAxis);
                }
                clipModel.Link(gameLocal.clip, self, clipModel.GetId(), current.i.position, current.i.orientation);
                current.i.linearMomentum.oSet(((current.i.position.oMinus(oldOrigin)).oDivide(timeStep)).oMultiply(mass));
                current.i.angularMomentum.oSet(inertiaTensor.oMultiply((current.i.orientation.oMultiply(oldAxis.Transpose())).ToAngularVelocity().oDivide(timeStep)));
                current.externalForce.Zero();
                current.externalTorque.Zero();

                return (!current.i.position.equals(oldOrigin) || !current.i.orientation.equals(oldAxis));
            }

            // if the body is at rest
            if (current.atRest >= 0 || timeStep <= 0.0f) {
                DebugDraw();
                return false;
            }

            // if putting the body to rest
            if (dropToFloor) {
                DropToFloorAndRest();
                current.externalForce.Zero();
                current.externalTorque.Zero();
                return true;
            }

            if (RB_TIMINGS) {
                timer_total.Start();
            }

            // move the rigid body velocity into the frame of a pusher
//	current.i.linearMomentum -= current.pushVelocity.SubVec3( 0 ) * mass;
//	current.i.angularMomentum -= current.pushVelocity.SubVec3( 1 ) * inertiaTensor;
            clipModel.Unlink();

            next = current;

            // calculate next position and orientation
            Integrate(timeStep, next);

            if (RB_TIMINGS) {
                timer_collision.Start();
            }

            // check for collisions from the current to the next state
            collided = CheckForCollisions(timeStep, next, collision);

            if (RB_TIMINGS) {
                timer_collision.Stop();
            }

            // set the new state
            current = next;

            if (collided) {
                // apply collision impulse
                if (CollisionImpulse(collision[0], impulse)) {
                    current.atRest = gameLocal.time;
                }
            }

            // update the position of the clip model
            clipModel.Link(gameLocal.clip, self, clipModel.GetId(), current.i.position, current.i.orientation);

            DebugDraw();

            if (!noContact) {

                if (RB_TIMINGS) {
                    timer_collision.Start();
                }
                // get contacts
                EvaluateContacts();

                if (RB_TIMINGS) {
                    timer_collision.Stop();
                }

                // check if the body has come to rest
                if (TestIfAtRest()) {
                    // put to rest
                    Rest();
                    cameToRest = true;
                } else {
                    // apply contact friction
                    ContactFriction(timeStep);
                }
            }

            if (current.atRest < 0) {
                ActivateContactEntities();
            }

            if (collided) {
                // if the rigid body didn't come to rest or the other entity is not at rest
                ent = gameLocal.entities[collision[0].c.entityNum];
                if (ent != null && (!cameToRest || !ent.IsAtRest())) {
                    // apply impact to other entity
                    ent.ApplyImpulse(self, collision[0].c.id, collision[0].c.point, impulse.oNegative());
                }
            }

            // move the rigid body velocity back into the world frame
//	current.i.linearMomentum += current.pushVelocity.SubVec3( 0 ) * mass;
//	current.i.angularMomentum += current.pushVelocity.SubVec3( 1 ) * inertiaTensor;
            current.pushVelocity.Zero();

            current.lastTimeStep = timeStep;
            current.externalForce.Zero();
            current.externalTorque.Zero();

            if (IsOutsideWorld()) {
                gameLocal.Warning("rigid body moved outside world bounds for entity '%s' type '%s' at (%s)",
                        self.name, self.GetType().getName(), current.i.position.ToString(0));
                Rest();
            }

            if (RB_TIMINGS) {
                timer_total.Stop();

                if (rb_showTimings.GetInteger() == 1) {
                    gameLocal.Printf("%12s: t %1.4f cd %1.4f\n",
                            self.name,
                            timer_total.Milliseconds(), timer_collision.Milliseconds());
                    lastTimerReset = 0;
                } else if (rb_showTimings.GetInteger() == 2) {
                    numRigidBodies++;
                    if (endTimeMSec > lastTimerReset) {
                        gameLocal.Printf("rb %d: t %1.4f cd %1.4f\n",
                                numRigidBodies,
                                timer_total.Milliseconds(), timer_collision.Milliseconds());
                    }
                }
                if (endTimeMSec > lastTimerReset) {
                    lastTimerReset = endTimeMSec;
                    numRigidBodies = 0;
                    timer_total.Clear();
                    timer_collision.Clear();
                }
            }

            return true;
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
            idVec3 linearVelocity, angularVelocity;
            idMat3 inverseWorldInertiaTensor;

            linearVelocity = current.i.linearMomentum.oMultiply(inverseMass);
            inverseWorldInertiaTensor = current.i.orientation.Transpose().oMultiply(inverseInertiaTensor.oMultiply(current.i.orientation));
            angularVelocity = inverseWorldInertiaTensor.oMultiply(current.i.angularMomentum);

            info.invMass = inverseMass;
            info.invInertiaTensor = inverseWorldInertiaTensor;
            info.position = point.oMinus(current.i.position.oPlus(centerOfMass.oMultiply(current.i.orientation)));
            info.velocity = linearVelocity.oPlus(angularVelocity.Cross(info.position));
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
            if (noImpact) {
                return;
            }
            current.i.linearMomentum.oPluSet(impulse);
            current.i.angularMomentum.oPluSet((point.oMinus(current.i.position.oPlus(centerOfMass.oMultiply(current.i.orientation)))).Cross(impulse));
            Activate();
        }

        @Override
        public void AddForce(final int id, final idVec3 point, final idVec3 force) {
            if (noImpact) {
                return;
            }
            current.externalForce.oPluSet(force);
            current.externalTorque.oPluSet((point.oMinus(current.i.position.oPlus(centerOfMass.oMultiply(current.i.orientation)))).Cross(force));
            Activate();
        }

        @Override
        public void Activate() {
            current.atRest = -1;
            self.BecomeActive(TH_PHYSICS);
        }

        /*
         ================
         idPhysics_RigidBody::PutToRest

         put to rest untill something collides with this physics object
         ================
         */
        @Override
        public void PutToRest() {
            Rest();
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
        public boolean IsPushable() {
            return (!noImpact && !hasMaster);
        }

        @Override
        public void SaveState() {
            saved = current;
        }

        @Override
        public void RestoreState() {
            current = saved;

            clipModel.Link(gameLocal.clip, self, clipModel.GetId(), current.i.position, current.i.orientation);

            EvaluateContacts();
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.localOrigin.oSet(newOrigin);
            if (hasMaster) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.i.position.oSet(masterOrigin.oPlus(newOrigin.oMultiply(masterAxis)));
            } else {
                current.i.position.oSet(newOrigin);
            }

            clipModel.Link(gameLocal.clip, self, clipModel.GetId(), current.i.position, clipModel.GetAxis());

            Activate();
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.localAxis.oSet(newAxis);
            if (hasMaster && isOrientated) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.i.orientation.oSet(newAxis.oMultiply(masterAxis));
            } else {
                current.i.orientation.oSet(newAxis);
            }

            clipModel.Link(gameLocal.clip, self, clipModel.GetId(), clipModel.GetOrigin(), current.i.orientation);

            Activate();
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {

            current.localOrigin.oPluSet(translation);
            current.i.position.oPluSet(translation);

            clipModel.Link(gameLocal.clip, self, clipModel.GetId(), current.i.position, clipModel.GetAxis());

            Activate();
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.i.orientation.oMulSet(rotation.ToMat3());
            current.i.position.oMulSet(rotation);

            if (hasMaster) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.localAxis.oMulSet(rotation.ToMat3());
                current.localOrigin.oSet((current.i.position.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
            } else {
                current.localAxis.oSet(current.i.orientation);
                current.localOrigin.oSet(current.i.position);
            }

            clipModel.Link(gameLocal.clip, self, clipModel.GetId(), current.i.position, current.i.orientation);

            Activate();
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            return current.i.position;
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return current.i.orientation;
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            current.i.linearMomentum.oSet(newLinearVelocity.oMultiply(mass));
            Activate();
        }

        @Override
        public void SetAngularVelocity(final idVec3 newAngularVelocity, int id /*= 0*/) {
            current.i.angularMomentum.oSet(newAngularVelocity.oMultiply(inertiaTensor));
            Activate();
        }
        static idVec3 curLinearVelocity;

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            curLinearVelocity = current.i.linearMomentum.oMultiply(inverseMass);
            return curLinearVelocity;
        }
        static idVec3 curAngularVelocity;

        @Override
        public idVec3 GetAngularVelocity(int id /*= 0*/) {
            idMat3 inverseWorldInertiaTensor;

            inverseWorldInertiaTensor = current.i.orientation.Transpose().oMultiply(inverseInertiaTensor.oMultiply(current.i.orientation));
            curAngularVelocity = inverseWorldInertiaTensor.oMultiply(current.i.angularMomentum);
            return curAngularVelocity;
        }

        @Override
        public void ClipTranslation(trace_s[] results, final idVec3 translation, final idClipModel model) {
            if (model != null) {
                gameLocal.clip.TranslationModel(results, clipModel.GetOrigin(), clipModel.GetOrigin().oPlus(translation),
                        clipModel, clipModel.GetAxis(), clipMask, model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                gameLocal.clip.Translation(results, clipModel.GetOrigin(), clipModel.GetOrigin().oPlus(translation),
                        clipModel, clipModel.GetAxis(), clipMask, self);
            }
        }

        @Override
        public void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model) {
            if (model != null) {
                gameLocal.clip.RotationModel(results, clipModel.GetOrigin(), rotation,
                        clipModel, clipModel.GetAxis(), clipMask, model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                gameLocal.clip.Rotation(results, clipModel.GetOrigin(), rotation,
                        clipModel, clipModel.GetAxis(), clipMask, self);
            }
        }

        @Override
        public int ClipContents(final idClipModel model) {
            if (model != null) {
                return gameLocal.clip.ContentsModel(clipModel.GetOrigin(), clipModel, clipModel.GetAxis(), -1,
                        model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                return gameLocal.clip.Contents(clipModel.GetOrigin(), clipModel, clipModel.GetAxis(), -1, null);
            }
        }

        @Override
        public void DisableClip() {
            clipModel.Disable();
        }

        @Override
        public void EnableClip() {
            clipModel.Enable();
        }

        @Override
        public void UnlinkClip() {
            clipModel.Unlink();
        }

        @Override
        public void LinkClip() {
            clipModel.Link(gameLocal.clip, self, clipModel.GetId(), current.i.position, current.i.orientation);
        }

        @Override
        public boolean EvaluateContacts() {
            idVec6 dir = new idVec6();
            int num;

            ClearContacts();

            contacts.SetNum(10, false);

            dir.SubVec3_oSet(0, current.i.linearMomentum.oPlus(gravityVector.oMultiply(current.lastTimeStep * mass)));
            dir.SubVec3_oSet(1, current.i.angularMomentum);
            dir.SubVec3_Normalize(0);
            dir.SubVec3_Normalize(1);
            final contactInfo_t[] contactz = contacts.Ptr(contactInfo_t[].class);
            num = gameLocal.clip.Contacts(contactz, 10, clipModel.GetOrigin(),
                    dir, CONTACT_EPSILON, clipModel, clipModel.GetAxis(), clipMask, self);
            for (int i = 0; i < num; i++) {
                contacts.oSet(i, contactz[i]);
            }
            contacts.SetNum(num, false);

            AddContactEntitiesForContacts();

            return (contacts.Num() != 0);
        }

        @Override
        public void SetPushed(int deltaTime) {
            idRotation rotation;

            rotation = (saved.i.orientation.oMultiply(current.i.orientation)).ToRotation();

            // velocity with which the af is pushed
            current.pushVelocity.SubVec3_oPluSet(0, (current.i.position.oMinus(saved.i.position)).oDivide(deltaTime * idMath.M_MS2SEC));
            current.pushVelocity.SubVec3_oPluSet(1, rotation.GetVec().oMultiply((float) -DEG2RAD(rotation.GetAngle())).oDivide(deltaTime * idMath.M_MS2SEC));
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return current.pushVelocity.SubVec3(0);
        }

        @Override
        public idVec3 GetPushedAngularVelocity(final int id /*= 0*/) {
            return current.pushVelocity.SubVec3(1);
        }

        @Override
        public void SetMaster(idEntity master, final boolean orientated) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (!hasMaster) {
                    // transform from world space to master space
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    current.localOrigin.oSet((current.i.position.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                    if (orientated) {
                        current.localAxis.oSet(current.i.orientation.oMultiply(masterAxis.Transpose()));
                    } else {
                        current.localAxis.oSet(current.i.orientation);
                    }
                    hasMaster = true;
                    isOrientated = orientated;
                    ClearContacts();
                }
            } else {
                if (hasMaster) {
                    hasMaster = false;
                    Activate();
                }
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            idCQuat quat, localQuat;

            quat = current.i.orientation.ToCQuat();
            localQuat = current.localAxis.ToCQuat();

            msg.WriteLong(current.atRest);
            msg.WriteFloat(current.i.position.oGet(0));
            msg.WriteFloat(current.i.position.oGet(1));
            msg.WriteFloat(current.i.position.oGet(2));
            msg.WriteFloat(quat.x);
            msg.WriteFloat(quat.y);
            msg.WriteFloat(quat.z);
            msg.WriteFloat(current.i.linearMomentum.oGet(0), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(current.i.linearMomentum.oGet(1), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(current.i.linearMomentum.oGet(2), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(current.i.angularMomentum.oGet(0), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(current.i.angularMomentum.oGet(1), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(current.i.angularMomentum.oGet(2), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteDeltaFloat(current.i.position.oGet(0), current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(current.i.position.oGet(1), current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(current.i.position.oGet(2), current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(quat.x, localQuat.x);
            msg.WriteDeltaFloat(quat.y, localQuat.y);
            msg.WriteDeltaFloat(quat.z, localQuat.z);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(0), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(1), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(2), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.externalForce.oGet(0), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.externalForce.oGet(1), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.externalForce.oGet(2), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.externalTorque.oGet(0), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.externalTorque.oGet(1), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.externalTorque.oGet(2), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            idCQuat quat = new idCQuat(), localQuat = new idCQuat();

            current.atRest = msg.ReadLong();
            current.i.position.oSet(0, msg.ReadFloat());
            current.i.position.oSet(1, msg.ReadFloat());
            current.i.position.oSet(2, msg.ReadFloat());
            quat.x = msg.ReadFloat();
            quat.y = msg.ReadFloat();
            quat.z = msg.ReadFloat();
            current.i.linearMomentum.oSet(0, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            current.i.linearMomentum.oSet(1, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            current.i.linearMomentum.oSet(2, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            current.i.angularMomentum.oSet(0, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            current.i.angularMomentum.oSet(1, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            current.i.angularMomentum.oSet(2, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            current.localOrigin.oSet(0, msg.ReadDeltaFloat(current.i.position.oGet(0)));
            current.localOrigin.oSet(1, msg.ReadDeltaFloat(current.i.position.oGet(1)));
            current.localOrigin.oSet(2, msg.ReadDeltaFloat(current.i.position.oGet(2)));
            localQuat.x = msg.ReadDeltaFloat(quat.x);
            localQuat.y = msg.ReadDeltaFloat(quat.y);
            localQuat.z = msg.ReadDeltaFloat(quat.z);
            current.pushVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS));
            current.externalForce.oSet(0, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            current.externalForce.oSet(1, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            current.externalForce.oSet(2, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            current.externalTorque.oSet(0, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            current.externalTorque.oSet(1, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            current.externalTorque.oSet(2, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));

            current.i.orientation.oSet(quat.ToMat3());
            current.localAxis.oSet(localQuat.ToMat3());

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, clipModel.GetId(), current.i.position, current.i.orientation);
            }
        }

        private static class rigidBodyDerivatives_s {
            public static final int BYTES =
                            idVec3.BYTES +
                            idMat3.BYTES +
                            idVec3.BYTES +
                            idVec3.BYTES;

            idVec3 linearVelocity;
            idMat3 angularMatrix;
            idVec3 force;
            idVec3 torque;

            private rigidBodyDerivatives_s(float[] derivatives) {
                FloatBuffer b = FloatBuffer.wrap(derivatives);
                if (b.hasRemaining()) {
                    linearVelocity = new idVec3(b.get(), b.get(), b.get());
                }
                if (b.hasRemaining()) {
                    angularMatrix = new idMat3(
                            b.get(), b.get(), b.get(),
                            b.get(), b.get(), b.get(),
                            b.get(), b.get(), b.get());
                }
                if (b.hasRemaining()) {
                    force = new idVec3(b.get(), b.get(), b.get());
                }
                if (b.hasRemaining()) {
                    torque = new idVec3(b.get(), b.get(), b.get());
                }
            }

            private float[] toFloats() {
                final FloatBuffer buffer = FloatBuffer.allocate(BYTES / Float.BYTES);
                buffer.put(linearVelocity.ToFloatPtr())
                        .put(angularMatrix.ToFloatPtr())
                        .put(force.ToFloatPtr())
                        .put(torque.ToFloatPtr());

                return buffer.array();
            }
        };

        private /*friend*/ static class RigidBodyDerivatives extends deriveFunction_t {

            public static final deriveFunction_t INSTANCE = new RigidBodyDerivatives();

            private RigidBodyDerivatives() {
            }

            @Override
            public void run(final float t, final Object clientData, final float[] state, float[] derivatives) {
                idPhysics_RigidBody p = (idPhysics_RigidBody) clientData;
                rigidBodyIState_s s = new rigidBodyIState_s(state);//TODO:from float array to object
                // NOTE: this struct should be build conform rigidBodyIState_t
                rigidBodyDerivatives_s d = new rigidBodyDerivatives_s(derivatives);
                idVec3 angularVelocity;
                idMat3 inverseWorldInertiaTensor;

                inverseWorldInertiaTensor = s.orientation.oMultiply(p.inverseInertiaTensor.oMultiply(s.orientation.Transpose()));
                angularVelocity = inverseWorldInertiaTensor.oMultiply(s.angularMomentum);
                // derivatives
                d.linearVelocity = s.linearMomentum.oMultiply(p.inverseMass);
                d.angularMatrix = SkewSymmetric(angularVelocity).oMultiply(s.orientation);
                d.force = s.linearMomentum.oMultiply(-p.linearFriction).oPlus(p.current.externalForce);
                d.torque = s.angularMomentum.oMultiply(-p.angularFriction).oPlus(p.current.externalTorque);

                System.arraycopy(d.toFloats(), 0, derivatives, 0 ,derivatives.length);
            }
        };


        /*
         ================
         idPhysics_RigidBody::Integrate

         Calculate next state from the current state using an integrator.
         ================
         */
        private void Integrate(final float deltaTime, rigidBodyPState_s next) {
            idVec3 position;

            position = current.i.position;
            current.i.position.oPluSet(centerOfMass.oMultiply(current.i.orientation));

            current.i.orientation.TransposeSelf();

            final float[] newState = next.i.toFloats();
            integrator.Evaluate(current.i.toFloats(), newState, 0, deltaTime);
            next.i.fromFloats(newState);
            next.i.orientation.OrthoNormalizeSelf();

            // apply gravity
            next.i.linearMomentum.oPluSet(gravityVector.oMultiply(mass * deltaTime));

            current.i.orientation.TransposeSelf();
            next.i.orientation.TransposeSelf();

            current.i.position.oSet(position);
            next.i.position.oMinSet(centerOfMass.oMultiply(next.i.orientation));

            next.atRest = current.atRest;
        }

        /*
         ================
         idPhysics_RigidBody::CheckForCollisions

         Check for collisions between the current and next state.
         If there is a collision the next state is set to the state at the moment of impact.
         ================
         */
        private boolean CheckForCollisions(final float deltaTime, rigidBodyPState_s next, trace_s[] collision) {
//#define TEST_COLLISION_DETECTION
            idMat3 axis = new idMat3();
            idRotation rotation;
            boolean collided = false;
            boolean startsolid;

            if (TEST_COLLISION_DETECTION) {
                if (gameLocal.clip.Contents(current.i.position, clipModel, current.i.orientation, clipMask, self) != 0) {
                    startsolid = true;
                }
            }

            TransposeMultiply(current.i.orientation, next.i.orientation, axis);
            rotation = axis.ToRotation();
            rotation.SetOrigin(current.i.position);

            // if there was a collision
            if (gameLocal.clip.Motion(collision, current.i.position, next.i.position, rotation, clipModel, current.i.orientation, clipMask, self)) {
                // set the next state to the state at the moment of impact
                next.i.position.oSet(collision[0].endpos);
                next.i.orientation.oSet(collision[0].endAxis);
                next.i.linearMomentum.oSet(current.i.linearMomentum);
                next.i.angularMomentum.oSet(current.i.angularMomentum);
                collided = true;
            }

            if (TEST_COLLISION_DETECTION) {
                if (gameLocal.clip.Contents(next.i.position, clipModel, next.i.orientation, clipMask, self) != 0) {
                    if (!startsolid) {
                        int bah = 1;
                    }
                }
            }
            return collided;
        }

        /*
         ================
         idPhysics_RigidBody::CollisionImpulse

         Calculates the collision impulse using the velocity relative to the collision object.
         The current state should be set to the moment of impact.
         ================
         */
        private boolean CollisionImpulse(final trace_s collision, idVec3 impulse) {
            idVec3 r, linearVelocity, angularVelocity, velocity;
            idMat3 inverseWorldInertiaTensor;
            float impulseNumerator, impulseDenominator, vel;
            impactInfo_s info = new impactInfo_s();
            idEntity ent;

            // get info from other entity involved
            ent = gameLocal.entities[collision.c.entityNum];
            ent.GetImpactInfo(self, collision.c.id, collision.c.point, info);

            // collision point relative to the body center of mass
            r = collision.c.point.oMinus(current.i.position.oPlus(centerOfMass.oMultiply(current.i.orientation)));
            // the velocity at the collision point
            linearVelocity = current.i.linearMomentum.oMultiply(inverseMass);
            inverseWorldInertiaTensor = current.i.orientation.Transpose().oMultiply(inverseInertiaTensor.oMultiply(current.i.orientation));
            angularVelocity = inverseWorldInertiaTensor.oMultiply(current.i.angularMomentum);
            velocity = linearVelocity.oPlus(angularVelocity.Cross(r));
            // subtract velocity of other entity
            velocity.oMinSet(info.velocity);

            // velocity in normal direction
            vel = velocity.oMultiply(collision.c.normal);

            if (vel > -STOP_SPEED) {
                impulseNumerator = STOP_SPEED;
            } else {
                impulseNumerator = -(1.0f + bouncyness) * vel;
            }
            impulseDenominator = inverseMass + ((inverseWorldInertiaTensor.oMultiply(r.Cross(collision.c.normal)).Cross(r)).oMultiply(collision.c.normal));
            if (info.invMass != 0) {
                impulseDenominator += info.invMass + ((info.invInertiaTensor.oMultiply(info.position.Cross(collision.c.normal))).Cross(info.position).oMultiply(collision.c.normal));
            }
            impulse.oSet(collision.c.normal.oMultiply((impulseNumerator / impulseDenominator)));

            // update linear and angular momentum with impulse
            current.i.linearMomentum.oPluSet(impulse);
            current.i.angularMomentum.oPluSet(r.Cross(impulse));

            // if no movement at all don't blow up
            if (collision.fraction < 0.0001f) {
                current.i.linearMomentum.oMulSet(0.5f);
                current.i.angularMomentum.oMulSet(0.5f);
            }

            // callback to self to let the entity know about the collision
            return self.Collide(collision, velocity);
        }

        /*
         ================
         idPhysics_RigidBody::ContactFriction

         Does not solve friction for multiple simultaneous contacts but applies contact friction in isolation.
         Uses absolute velocity at the contact points instead of the velocity relative to the contact object.
         ================
         */
        private void ContactFriction(float deltaTime) {
            int i;
            float magnitude, impulseNumerator, impulseDenominator;
            idMat3 inverseWorldInertiaTensor;
            idVec3 linearVelocity, angularVelocity;
            idVec3 massCenter, r, velocity, normal, impulse, normalVelocity;

            inverseWorldInertiaTensor = current.i.orientation.Transpose().oMultiply(inverseInertiaTensor.oMultiply(current.i.orientation));

            massCenter = current.i.position.oPlus(centerOfMass.oMultiply(current.i.orientation));

            for (i = 0; i < contacts.Num(); i++) {

                r = contacts.oGet(i).point.oMinus(massCenter);

                // calculate velocity at contact point
                linearVelocity = current.i.linearMomentum.oMultiply(inverseMass);
                angularVelocity = inverseWorldInertiaTensor.oMultiply(current.i.angularMomentum);
                velocity = linearVelocity.oPlus(angularVelocity.Cross(r));

                // velocity along normal vector
                normalVelocity = contacts.oGet(i).normal.oMultiply(velocity.oMultiply(contacts.oGet(i).normal));

                // calculate friction impulse
                normal = (velocity.oMinus(normalVelocity)).oNegative();
                magnitude = normal.Normalize();
                impulseNumerator = contactFriction * magnitude;
                impulseDenominator = inverseMass + ((inverseWorldInertiaTensor.oMultiply(r.Cross(normal))).Cross(r).oMultiply(normal));
                impulse = normal.oMultiply((impulseNumerator / impulseDenominator));

                // apply friction impulse
                current.i.linearMomentum.oPluSet(impulse);
                current.i.angularMomentum.oPluSet(r.Cross(impulse));

                // if moving towards the surface at the contact point
                if (normalVelocity.oMultiply(contacts.oGet(i).normal) < 0.0f) {
                    // calculate impulse
                    normal = normalVelocity.oNegative();
                    impulseNumerator = normal.Normalize();
                    impulseDenominator = inverseMass + ((inverseWorldInertiaTensor.oMultiply(r.Cross(normal))).Cross(r).oMultiply(normal));
                    impulse = normal.oMultiply((impulseNumerator / impulseDenominator));

                    // apply impulse
                    current.i.linearMomentum.oPluSet(impulse);
                    current.i.angularMomentum.oPluSet(r.Cross(impulse));
                }
            }
        }

        /*
         ================
         idPhysics_RigidBody::DropToFloorAndRest

         Drops the object straight down to the floor and verifies if the object is at rest on the floor.
         ================
         */                   private static int DBG_DropToFloorAndRest = 0;
        private void DropToFloorAndRest() {                DBG_DropToFloorAndRest++;
            idVec3 down;
            trace_s[] tr = {null};

            if(this.DBG_count==8209){
                int bla = 1;
            }

            if (testSolid) {

                testSolid = false;

                if (gameLocal.clip.Contents(current.i.position, clipModel, current.i.orientation, clipMask, self) != 0) {
                    gameLocal.DWarning("rigid body in solid for entity '%s' type '%s' at (%s)",
                            self.name, self.GetType().getName(), current.i.position.ToString(0));
                    Rest();
                    dropToFloor = false;
                    return;
                }
            }


            // put the body on the floor
            down = current.i.position.oPlus(gravityNormal.oMultiply(128.0f));
            gameLocal.clip.Translation(tr, current.i.position, down, clipModel, current.i.orientation, clipMask, self);
            current.i.position.oSet(tr[0].endpos);
            clipModel.Link(gameLocal.clip, self, clipModel.GetId(), tr[0].endpos, current.i.orientation);

            // if on the floor already
            if (tr[0].fraction == 0.0f) {
                // test if we are really at rest
                EvaluateContacts();
                if (!TestIfAtRest()) {
                    gameLocal.DWarning("rigid body not at rest for entity '%s' type '%s' at (%s)",
                            self.name, self.GetType().getName(), current.i.position.ToString(0));
                }
                Rest();
                dropToFloor = false;
            } else if (IsOutsideWorld()) {
                gameLocal.Warning("rigid body outside world bounds for entity '%s' type '%s' at (%s)",
                        self.name, self.GetType().getName(), current.i.position.ToString(0));
                Rest();
                dropToFloor = false;
            }
        }

        /*
         ================
         idPhysics_RigidBody::TestIfAtRest

         Returns true if the body is considered at rest.
         Does not catch all cases where the body is at rest but is generally good enough.
         ================
         */
        private boolean TestIfAtRest() {
            int i;
            float gv;
            idVec3 v, av, normal = new idVec3(), point;
            idMat3 inverseWorldInertiaTensor;
            idFixedWinding contactWinding = new idFixedWinding();

            if (current.atRest >= 0) {
                return true;
            }

            // need at least 3 contact points to come to rest
            if (contacts.Num() < 3) {
                return false;
            }

            // get average contact plane normal
            normal.Zero();
            for (i = 0; i < contacts.Num(); i++) {
                normal.oPluSet(contacts.oGet(i).normal);
            }
            normal.oDivSet((float) contacts.Num());
            normal.Normalize();

            // if on a too steep surface
            if ((normal.oMultiply(gravityNormal)) > -0.7f) {
                return false;
            }

            // create bounds for contact points
            contactWinding.Clear();
            for (i = 0; i < contacts.Num(); i++) {
                // project point onto plane through origin orthogonal to the gravity
                point = contacts.oGet(i).point.oMinus(gravityNormal.oMultiply(contacts.oGet(i).point.oMultiply(gravityNormal)));
                contactWinding.AddToConvexHull(point, gravityNormal);
            }

            // need at least 3 contact points to come to rest
            if (contactWinding.GetNumPoints() < 3) {
                return false;
            }

            // center of mass in world space
            point = current.i.position.oPlus(centerOfMass.oMultiply(current.i.orientation));
            point.oMinSet(point.oMultiply(gravityNormal.oMultiply(gravityNormal)));

            // if the point is not inside the winding
            if (!contactWinding.PointInside(gravityNormal, point, 0)) {
                return false;
            }

            // linear velocity of body
            v = current.i.linearMomentum.oMultiply(inverseMass);
            // linear velocity in gravity direction
            gv = v.oMultiply(gravityNormal);
            // linear velocity orthogonal to gravity direction
            v.oMinSet(gravityNormal.oMultiply(gv));

            // if too much velocity orthogonal to gravity direction
            if (v.Length() > STOP_SPEED) {
                return false;
            }
            // if too much velocity in gravity direction
            if (gv > 2.0f * STOP_SPEED || gv < -2.0f * STOP_SPEED) {
                return false;
            }

            // calculate rotational velocity
            inverseWorldInertiaTensor = current.i.orientation.oMultiply(inverseInertiaTensor.oMultiply(current.i.orientation.Transpose()));
            av = inverseWorldInertiaTensor.oMultiply(current.i.angularMomentum);

            // if too much rotational velocity
            if (av.LengthSqr() > STOP_SPEED) {
                return false;
            }

            return true;
        }

        private void Rest() {
            current.atRest = gameLocal.time;
            current.i.linearMomentum.Zero();
            current.i.angularMomentum.Zero();
            self.BecomeInactive(TH_PHYSICS);
        }

        private void DebugDraw() {

            if (rb_showBodies.GetBool() || (rb_showActive.GetBool() && current.atRest < 0)) {
                CollisionModel_local.collisionModelManager.DrawModel(clipModel.Handle(), clipModel.GetOrigin(), clipModel.GetAxis(), getVec3_origin(), 0.0f);
            }

            if (rb_showMass.GetBool()) {
                gameRenderWorld.DrawText(va("\n%1.2f", mass), current.i.position, 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
            }

            if (rb_showInertia.GetBool()) {
                idMat3 I = inertiaTensor;
                gameRenderWorld.DrawText(va("\n\n\n( %.1f %.1f %.1f )\n( %.1f %.1f %.1f )\n( %.1f %.1f %.1f )",
                        I.oGet(0).x, I.oGet(0).y, I.oGet(0).z,
                        I.oGet(1).x, I.oGet(1).y, I.oGet(1).z,
                        I.oGet(2).x, I.oGet(2).y, I.oGet(2).z),
                        current.i.position, 0.05f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
            }

            if (rb_showVelocity.GetBool()) {
                DrawVelocity(clipModel.GetId(), 0.1f, 4.0f);
            }
        }
    };

    /*
     ================
     idPhysics_RigidBody_SavePState
     ================
     */
    static void idPhysics_RigidBody_SavePState(idSaveGame savefile, final rigidBodyPState_s state) {
        savefile.WriteInt(state.atRest);
        savefile.WriteFloat(state.lastTimeStep);
        savefile.WriteVec3(state.localOrigin);
        savefile.WriteMat3(state.localAxis);
        savefile.WriteVec6(state.pushVelocity);
        savefile.WriteVec3(state.externalForce);
        savefile.WriteVec3(state.externalTorque);

        savefile.WriteVec3(state.i.position);
        savefile.WriteMat3(state.i.orientation);
        savefile.WriteVec3(state.i.linearMomentum);
        savefile.WriteVec3(state.i.angularMomentum);
    }

    /*
     ================
     idPhysics_RigidBody_RestorePState
     ================
     */
    static void idPhysics_RigidBody_RestorePState(idRestoreGame savefile, rigidBodyPState_s state) {
        int[] atRest = {0};
        float[] lastTimeStep = {0};

        savefile.ReadInt(atRest);
        savefile.ReadFloat(lastTimeStep);
        savefile.ReadVec3(state.localOrigin);
        savefile.ReadMat3(state.localAxis);
        savefile.ReadVec6(state.pushVelocity);
        savefile.ReadVec3(state.externalForce);
        savefile.ReadVec3(state.externalTorque);

        savefile.ReadVec3(state.i.position);
        savefile.ReadMat3(state.i.orientation);
        savefile.ReadVec3(state.i.linearMomentum);
        savefile.ReadVec3(state.i.angularMomentum);

        state.atRest = atRest[0];
        state.lastTimeStep = lastTimeStep[0];
    }

}
