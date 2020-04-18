package neo.Game.Physics;

import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.GameSys.SysCvar.rb_showActive;
import static neo.Game.GameSys.SysCvar.rb_showBodies;
import static neo.Game.GameSys.SysCvar.rb_showInertia;
import static neo.Game.GameSys.SysCvar.rb_showMass;
import static neo.Game.GameSys.SysCvar.rb_showTimings;
import static neo.Game.GameSys.SysCvar.rb_showVelocity;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Physics.Physics.CONTACT_EPSILON;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.FLOAT_IS_NAN;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.Min3Index;
import static neo.idlib.math.Matrix.idMat3.SkewSymmetric;
import static neo.idlib.math.Matrix.idMat3.TransposeMultiply;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;

import java.nio.FloatBuffer;

import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_Base.idPhysics_Base;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Timer.idTimer;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Ode.deriveFunction_t;
import neo.idlib.math.Ode.idODE;
import neo.idlib.math.Ode.idODE_Euler;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec6;
import neo.idlib.math.Matrix.idMat3;
import neo.open.Nio;

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
            this.position = new idVec3();
            this.orientation = new idMat3();
            this.linearMomentum = new idVec3();
            this.angularMomentum = new idVec3();
        }
        
        private rigidBodyIState_s(float[] state) {
            this();
            this.fromFloats(state);
        }

        private rigidBodyIState_s(rigidBodyIState_s r) {
            this.position = new idVec3(r.position);
            this.orientation = new idMat3(r.orientation);
            this.linearMomentum = new idVec3(r.linearMomentum);
            this.angularMomentum = new idVec3(r.angularMomentum);
        }

        private float[] toFloats() {
            final FloatBuffer buffer = FloatBuffer.allocate(BYTES / Float.BYTES);
            buffer.put(this.position.ToFloatPtr())
                    .put(this.orientation.ToFloatPtr())
                    .put(this.linearMomentum.ToFloatPtr())
                    .put(this.angularMomentum.ToFloatPtr());

            return buffer.array();
        }

        private void fromFloats(final float[] state) {
            final FloatBuffer b = FloatBuffer.wrap(state);
            if (b.hasRemaining()) {
                this.position.oSet(new idVec3(b.get(), b.get(), b.get()));
            }
            if (b.hasRemaining()) {
                this.orientation.oSet(new idMat3(
                        b.get(), b.get(), b.get(),
                        b.get(), b.get(), b.get(),
                        b.get(), b.get(), b.get()));
            }
            if (b.hasRemaining()) {
                this.linearMomentum.oSet(new idVec3(b.get(), b.get(), b.get()));
            }
            if (b.hasRemaining()) {
                this.angularMomentum.oSet(new idVec3(b.get(), b.get(), b.get()));
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

        public rigidBodyPState_s(rigidBodyPState_s r) {
            this.atRest = r.atRest;
            this.lastTimeStep = r.lastTimeStep;
            this.localOrigin = new idVec3(r.localOrigin);
            this.localAxis = new idMat3(r.localAxis);
            this.pushVelocity = new idVec6(r.pushVelocity);
            this.externalForce = new idVec3(r.externalForce);
            this.externalTorque = new idVec3(r.externalTorque);
            this.i = new rigidBodyIState_s(r.i);
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
        private final idVec3            centerOfMass;         // center of mass of trace model
        private final idMat3            inertiaTensor;        // mass distribution
        private idMat3            inverseInertiaTensor; // inverse inertia tensor
        //
        private final idODE             integrator;           // integrator
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
            this.clipModel = null;

//	memset( &current, 0, sizeof( current ) );
            this.current = new rigidBodyPState_s();

            this.current.atRest = -1;
            this.current.lastTimeStep = USERCMD_MSEC;
            this.current.i = new rigidBodyIState_s();
            this.current.i.orientation.oSet(getMat3_identity());

            this.saved = this.current;

            this.mass = 1.0f;
            this.inverseMass = 1.0f;
            this.centerOfMass = new idVec3();
            this.inertiaTensor = getMat3_identity();
            this.inverseInertiaTensor = idMat3.getMat3_identity();

            // use the least expensive euler integrator
            this.integrator = new idODE_Euler(rigidBodyIState_s.BYTES / Float.BYTES, RigidBodyDerivatives.INSTANCE, this);

            this.dropToFloor = false;
            this.noImpact = false;
            this.noContact = false;

            this.hasMaster = false;
            this.isOrientated = false;

            if (RB_TIMINGS) {
                lastTimerReset = 0;
            }
        }

        // ~idPhysics_RigidBody();
        @Override
        protected void _deconstructor(){
            if ( this.clipModel != null ) {
                idClipModel.delete(this.clipModel);
            }
//            delete integrator;

            super._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {

            idPhysics_RigidBody_SavePState(savefile, this.current);
            idPhysics_RigidBody_SavePState(savefile, this.saved);

            savefile.WriteFloat(this.linearFriction);
            savefile.WriteFloat(this.angularFriction);
            savefile.WriteFloat(this.contactFriction);
            savefile.WriteFloat(this.bouncyness);
            savefile.WriteClipModel(this.clipModel);

            savefile.WriteFloat(this.mass);
            savefile.WriteFloat(this.inverseMass);
            savefile.WriteVec3(this.centerOfMass);
            savefile.WriteMat3(this.inertiaTensor);
            savefile.WriteMat3(this.inverseInertiaTensor);

            savefile.WriteBool(this.dropToFloor);
            savefile.WriteBool(this.testSolid);
            savefile.WriteBool(this.noImpact);
            savefile.WriteBool(this.noContact);

            savefile.WriteBool(this.hasMaster);
            savefile.WriteBool(this.isOrientated);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            idPhysics_RigidBody_RestorePState(savefile, this.current);
            idPhysics_RigidBody_RestorePState(savefile, this.saved);

            this.linearFriction = savefile.ReadFloat();
            this.angularFriction = savefile.ReadFloat();
            this.contactFriction = savefile.ReadFloat();
            this.bouncyness = savefile.ReadFloat();
            savefile.ReadClipModel(this.clipModel);

            this.mass = savefile.ReadFloat();
            this.inverseMass = savefile.ReadFloat();
            savefile.ReadVec3(this.centerOfMass);
            savefile.ReadMat3(this.inertiaTensor);
            savefile.ReadMat3(this.inverseInertiaTensor);

            this.dropToFloor = savefile.ReadBool();
            this.testSolid = savefile.ReadBool();
            this.noImpact = savefile.ReadBool();
            this.noContact = savefile.ReadBool();

            this.hasMaster = savefile.ReadBool();
            this.isOrientated = savefile.ReadBool();
        }

        // initialisation
        public void SetFriction(final float linear, final float angular, final float contact) {
            if ((linear < 0.0f) || (linear > 1.0f)
                    || (angular < 0.0f) || (angular > 1.0f)
                    || (contact < 0.0f) || (contact > 1.0f)) {
                return;
            }
            this.linearFriction = linear;
            this.angularFriction = angular;
            this.contactFriction = contact;
        }

        public void SetBouncyness(final float b) {
            if ((b < 0.0f) || (b > 1.0f)) {
                return;
            }
            this.bouncyness = b;
        }

        // same as above but drop to the floor first
        public void DropToFloor() {
            this.dropToFloor = true;
            this.testSolid = true;
        }

        // no contact determination and contact friction
        public void NoContact() {
            this.noContact = true;
        }

        // enable/disable activation by impact
        public void EnableImpact() {
            this.noImpact = false;
        }

        public void DisableImpact() {
            this.noImpact = true;
        }

        static final float MAX_INERTIA_SCALE = 10.0f;

        // common physics interface
        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
            int minIndex;
            final idMat3 inertiaScale = new idMat3();

            assert (this.self != null);
            assert (model != null);                    // we need a clip model
            assert (model.IsTraceModel());    // and it should be a trace model
            assert (density > 0.0f);            // density should be valid

            if ((this.clipModel != null) && (this.clipModel != model) && freeOld) {
                idClipModel.delete(this.clipModel);
            }
            this.clipModel = model;
            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.i.position, this.current.i.orientation);

            {// get mass properties from the trace model
                final float[] mass = {0};
                this.clipModel.GetMassProperties(density, mass, this.centerOfMass, this.inertiaTensor);
                this.mass = mass[0];
            }

            // check whether or not the clip model has valid mass properties
            if ((this.mass <= 0.0f) || FLOAT_IS_NAN(this.mass)) {
                gameLocal.Warning("idPhysics_RigidBody::SetClipModel: invalid mass for entity '%s' type '%s'",
                        this.self.name, this.self.GetType().getName());
                this.mass = 1.0f;
                this.centerOfMass.Zero();
                this.inertiaTensor.Identity();
            }

            // check whether or not the inertia tensor is balanced
            minIndex = Min3Index(this.inertiaTensor.oGet(0, 0), this.inertiaTensor.oGet(1, 1), this.inertiaTensor.oGet(2, 2));
            inertiaScale.Identity();
            inertiaScale.oSet(0, 0, this.inertiaTensor.oGet(0, 0) / this.inertiaTensor.oGet(minIndex, minIndex));
            inertiaScale.oSet(1, 1, this.inertiaTensor.oGet(1, 1) / this.inertiaTensor.oGet(minIndex, minIndex));
            inertiaScale.oSet(2, 2, this.inertiaTensor.oGet(2, 2) / this.inertiaTensor.oGet(minIndex, minIndex));

            if ((inertiaScale.oGet(0, 0) > MAX_INERTIA_SCALE) || (inertiaScale.oGet(1, 1) > MAX_INERTIA_SCALE) || (inertiaScale.oGet(2, 2) > MAX_INERTIA_SCALE)) {
                gameLocal.DWarning("idPhysics_RigidBody::SetClipModel: unbalanced inertia tensor for entity '%s' type '%s'",
                        this.self.name, this.self.GetType().getName());
                final float min = this.inertiaTensor.oGet(minIndex, minIndex) * MAX_INERTIA_SCALE;
                inertiaScale.oSet((minIndex + 1) % 3, (minIndex + 1) % 3, min / this.inertiaTensor.oGet((minIndex + 1) % 3, (minIndex + 1) % 3));
                inertiaScale.oSet((minIndex + 2) % 3, (minIndex + 2) % 3, min / this.inertiaTensor.oGet((minIndex + 2) % 3, (minIndex + 2) % 3));
                this.inertiaTensor.oMulSet(inertiaScale);
            }

            this.inverseMass = 1.0f / this.mass;
            this.inverseInertiaTensor = this.inertiaTensor.Inverse().oMultiply(1.0f / 6.0f);

            this.current.i.linearMomentum.Zero();
            this.current.i.angularMomentum.Zero();
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            return this.clipModel;
        }

        @Override
        public int GetNumClipModels() {
            return 1;
        }

        @Override
        public void SetMass(float mass, int id /*= -1*/) {
            assert (mass > 0.0f);
            this.inertiaTensor.oMulSet(mass / this.mass);
            this.inverseInertiaTensor = this.inertiaTensor.Inverse().oMultiply(1.0f / 6.0f);
            this.mass = mass;
            this.inverseMass = 1.0f / mass;
        }

        @Override
        public float GetMass(int id /*= -1*/) {
            return this.mass;
        }

        @Override
        public void SetContents(int contents, int id /*= -1*/) {
            this.clipModel.SetContents(contents);
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            return this.clipModel.GetContents();
        }

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            return this.clipModel.GetBounds();
        }

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {
            return this.clipModel.GetAbsBounds();
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
            final idAngles angles;
            final trace_s[] collision = {new trace_s()};
            final idVec3 impulse = new idVec3();
            idEntity ent;
            idVec3 oldOrigin;
			final idVec3 masterOrigin = new idVec3();
            idMat3 oldAxis;
			final idMat3 masterAxis = new idMat3();
            float timeStep;
            boolean collided, cameToRest = false;

            timeStep = MS2SEC(timeStepMSec);
            this.current.lastTimeStep = timeStep;

            if (this.hasMaster) {
                oldOrigin = new idVec3(this.current.i.position);
                oldAxis = new idMat3(this.current.i.orientation);
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.i.position.oSet(masterOrigin.oPlus(this.current.localOrigin.oMultiply(masterAxis)));
                if (this.isOrientated) {
                    this.current.i.orientation.oSet(this.current.localAxis.oMultiply(masterAxis));
                } else {
                    this.current.i.orientation.oSet(this.current.localAxis);
                }
                this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.current.i.position, this.current.i.orientation);
                this.current.i.linearMomentum.oSet(((this.current.i.position.oMinus(oldOrigin)).oDivide(timeStep)).oMultiply(this.mass));
                this.current.i.angularMomentum.oSet(this.inertiaTensor.oMultiply((this.current.i.orientation.oMultiply(oldAxis.Transpose())).ToAngularVelocity().oDivide(timeStep)));
                this.current.externalForce.Zero();
                this.current.externalTorque.Zero();

                return (!this.current.i.position.equals(oldOrigin) || !this.current.i.orientation.equals(oldAxis));
            }

            // if the body is at rest
            if ((this.current.atRest >= 0) || (timeStep <= 0.0f)) {
                DebugDraw();
                return false;
            }

            // if putting the body to rest
            if (this.dropToFloor) {
                DropToFloorAndRest();
                this.current.externalForce.Zero();
                this.current.externalTorque.Zero();
                return true;
            }

            if (RB_TIMINGS) {
                timer_total.Start();
            }

            // move the rigid body velocity into the frame of a pusher
//	current.i.linearMomentum -= current.pushVelocity.SubVec3( 0 ) * mass;
//	current.i.angularMomentum -= current.pushVelocity.SubVec3( 1 ) * inertiaTensor;
            this.clipModel.Unlink();

            next = new rigidBodyPState_s(this.current);

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
            this.current = new rigidBodyPState_s(next);

            if (collided) {
                // apply collision impulse
                if (CollisionImpulse(collision[0], impulse)) {
                    this.current.atRest = gameLocal.time;
                }
            }

            // update the position of the clip model
            this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.current.i.position, this.current.i.orientation);

            DebugDraw();

            if (!this.noContact) {

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

            if (this.current.atRest < 0) {
                ActivateContactEntities();
            }

            if (collided) {
                // if the rigid body didn't come to rest or the other entity is not at rest
                ent = gameLocal.entities[collision[0].c.entityNum];
                if ((ent != null) && (!cameToRest || !ent.IsAtRest())) {
                    // apply impact to other entity
                    ent.ApplyImpulse(this.self, collision[0].c.id, collision[0].c.point, impulse.oNegative());
                }
            }

            // move the rigid body velocity back into the world frame
//	current.i.linearMomentum += current.pushVelocity.SubVec3( 0 ) * mass;
//	current.i.angularMomentum += current.pushVelocity.SubVec3( 1 ) * inertiaTensor;
            this.current.pushVelocity.Zero();

            this.current.lastTimeStep = timeStep;
            this.current.externalForce.Zero();
            this.current.externalTorque.Zero();

            if (IsOutsideWorld()) {
                gameLocal.Warning("rigid body moved outside world bounds for entity '%s' type '%s' at (%s)",
                        this.self.name, this.self.GetType().getName(), this.current.i.position.ToString(0));
                Rest();
            }

            if (RB_TIMINGS) {
                timer_total.Stop();

                if (rb_showTimings.GetInteger() == 1) {
                    gameLocal.Printf("%12s: t %1.4f cd %1.4f\n",
                            this.self.name,
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
        public impactInfo_s GetImpactInfo(final int id, final idVec3 point) {
            idVec3 linearVelocity, angularVelocity;
            idMat3 inverseWorldInertiaTensor;
            final impactInfo_s info = new impactInfo_s();

            linearVelocity = this.current.i.linearMomentum.oMultiply(this.inverseMass);
            inverseWorldInertiaTensor = this.current.i.orientation.Transpose().oMultiply(this.inverseInertiaTensor.oMultiply(this.current.i.orientation));
            angularVelocity = inverseWorldInertiaTensor.oMultiply(this.current.i.angularMomentum);

            info.invMass = this.inverseMass;
            info.invInertiaTensor.oSet(inverseWorldInertiaTensor);
            info.position = point.oMinus(this.current.i.position.oPlus(this.centerOfMass.oMultiply(this.current.i.orientation)));
            info.velocity = linearVelocity.oPlus(angularVelocity.Cross(info.position));
            return info;
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
            if (this.noImpact) {
                return;
            }
            this.current.i.linearMomentum.oPluSet(impulse);
            this.current.i.angularMomentum.oPluSet((point.oMinus(this.current.i.position.oPlus(this.centerOfMass.oMultiply(this.current.i.orientation)))).Cross(impulse));
            Activate();
        }

        @Override
        public void AddForce(final int id, final idVec3 point, final idVec3 force) {
            if (this.noImpact) {
                return;
            }
            this.current.externalForce.oPluSet(force);
            this.current.externalTorque.oPluSet((point.oMinus(this.current.i.position.oPlus(this.centerOfMass.oMultiply(this.current.i.orientation)))).Cross(force));
            Activate();
        }

        @Override
        public void Activate() {
            this.current.atRest = -1;
            this.self.BecomeActive(TH_PHYSICS);
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
            return this.current.atRest >= 0;
        }

        @Override
        public int GetRestStartTime() {
            return this.current.atRest;
        }

        @Override
        public boolean IsPushable() {
            return (!this.noImpact && !this.hasMaster);
        }

        @Override
        public void SaveState() {
            this.saved = this.current;
        }

        @Override
        public void RestoreState() {
            this.current = this.saved;

            this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.current.i.position, this.current.i.orientation);

            EvaluateContacts();
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.localOrigin.oSet(newOrigin);
            if (this.hasMaster) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.i.position.oSet(masterOrigin.oPlus(newOrigin.oMultiply(masterAxis)));
            } else {
                this.current.i.position.oSet(newOrigin);
            }

            this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.current.i.position, this.clipModel.GetAxis());

            Activate();
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.localAxis.oSet(newAxis);
            if (this.hasMaster && this.isOrientated) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.i.orientation.oSet(newAxis.oMultiply(masterAxis));
            } else {
                this.current.i.orientation.oSet(newAxis);
            }

            this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.clipModel.GetOrigin(), this.current.i.orientation);

            Activate();
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {

            this.current.localOrigin.oPluSet(translation);
            this.current.i.position.oPluSet(translation);

            this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.current.i.position, this.clipModel.GetAxis());

            Activate();
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.i.orientation.oMulSet(rotation.ToMat3());
            this.current.i.position.oMulSet(rotation);

            if (this.hasMaster) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.localAxis.oMulSet(rotation.ToMat3());
                this.current.localOrigin.oSet((this.current.i.position.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
            } else {
                this.current.localAxis.oSet(this.current.i.orientation);
                this.current.localOrigin.oSet(this.current.i.position);
            }

            this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.current.i.position, this.current.i.orientation);

            Activate();
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            return new idVec3(this.current.i.position);
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return new idMat3(this.current.i.orientation);
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            this.current.i.linearMomentum.oSet(newLinearVelocity.oMultiply(this.mass));
            Activate();
        }

        @Override
        public void SetAngularVelocity(final idVec3 newAngularVelocity, int id /*= 0*/) {
            this.current.i.angularMomentum.oSet(newAngularVelocity.oMultiply(this.inertiaTensor));
            Activate();
        }
        static idVec3 curLinearVelocity;

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            curLinearVelocity = this.current.i.linearMomentum.oMultiply(this.inverseMass);
            return curLinearVelocity;
        }
        static idVec3 curAngularVelocity;

        @Override
        public idVec3 GetAngularVelocity(int id /*= 0*/) {
            idMat3 inverseWorldInertiaTensor;

            inverseWorldInertiaTensor = this.current.i.orientation.Transpose().oMultiply(this.inverseInertiaTensor.oMultiply(this.current.i.orientation));
            curAngularVelocity = inverseWorldInertiaTensor.oMultiply(this.current.i.angularMomentum);
            return curAngularVelocity;
        }

        @Override
        public void ClipTranslation(trace_s[] results, final idVec3 translation, final idClipModel model) {
            if (model != null) {
                gameLocal.clip.TranslationModel(results, this.clipModel.GetOrigin(), this.clipModel.GetOrigin().oPlus(translation),
                        this.clipModel, this.clipModel.GetAxis(), this.clipMask, model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                gameLocal.clip.Translation(results, this.clipModel.GetOrigin(), this.clipModel.GetOrigin().oPlus(translation),
                        this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
            }
        }

        @Override
        public void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model) {
            if (model != null) {
                gameLocal.clip.RotationModel(results, this.clipModel.GetOrigin(), rotation,
                        this.clipModel, this.clipModel.GetAxis(), this.clipMask, model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                gameLocal.clip.Rotation(results, this.clipModel.GetOrigin(), rotation,
                        this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
            }
        }

        @Override
        public int ClipContents(final idClipModel model) {
            if (model != null) {
                return gameLocal.clip.ContentsModel(this.clipModel.GetOrigin(), this.clipModel, this.clipModel.GetAxis(), -1,
                        model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                return gameLocal.clip.Contents(this.clipModel.GetOrigin(), this.clipModel, this.clipModel.GetAxis(), -1, null);
            }
        }

        @Override
        public void DisableClip() {
            this.clipModel.Disable();
        }

        @Override
        public void EnableClip() {
            this.clipModel.Enable();
        }

        @Override
        public void UnlinkClip() {
            this.clipModel.Unlink();
        }

        @Override
        public void LinkClip() {
            this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.current.i.position, this.current.i.orientation);
        }

        @Override
        public boolean EvaluateContacts() {
            final idVec6 dir = new idVec6();
            int num;

            ClearContacts();

            this.contacts.SetNum(10, false);

            dir.SubVec3_oSet(0, this.current.i.linearMomentum.oPlus(this.gravityVector.oMultiply(this.current.lastTimeStep * this.mass)));
            dir.SubVec3_oSet(1, this.current.i.angularMomentum);
            dir.SubVec3_Normalize(0);
            dir.SubVec3_Normalize(1);
            final contactInfo_t[] contactz = this.contacts.Ptr(contactInfo_t[].class);
            num = gameLocal.clip.Contacts(contactz, 10, this.clipModel.GetOrigin(),
                    dir, CONTACT_EPSILON, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
            for (int i = 0; i < num; i++) {
                this.contacts.oSet(i, contactz[i]);
            }
            this.contacts.SetNum(num, false);

            AddContactEntitiesForContacts();

            return (this.contacts.Num() != 0);
        }

        @Override
        public void SetPushed(int deltaTime) {
            idRotation rotation;

            rotation = (this.saved.i.orientation.oMultiply(this.current.i.orientation)).ToRotation();

            // velocity with which the af is pushed
            this.current.pushVelocity.SubVec3_oPluSet(0, (this.current.i.position.oMinus(this.saved.i.position)).oDivide(deltaTime * idMath.M_MS2SEC));
            this.current.pushVelocity.SubVec3_oPluSet(1, rotation.GetVec().oMultiply(-DEG2RAD(rotation.GetAngle())).oDivide(deltaTime * idMath.M_MS2SEC));
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return this.current.pushVelocity.SubVec3(0);
        }

        @Override
        public idVec3 GetPushedAngularVelocity(final int id /*= 0*/) {
            return this.current.pushVelocity.SubVec3(1);
        }

        @Override
        public void SetMaster(idEntity master, final boolean orientated) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (!this.hasMaster) {
                    // transform from world space to master space
                    this.self.GetMasterPosition(masterOrigin, masterAxis);
                    this.current.localOrigin.oSet((this.current.i.position.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                    if (orientated) {
                        this.current.localAxis.oSet(this.current.i.orientation.oMultiply(masterAxis.Transpose()));
                    } else {
                        this.current.localAxis.oSet(this.current.i.orientation);
                    }
                    this.hasMaster = true;
                    this.isOrientated = orientated;
                    ClearContacts();
                }
            } else {
                if (this.hasMaster) {
                    this.hasMaster = false;
                    Activate();
                }
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            idCQuat quat, localQuat;

            quat = this.current.i.orientation.ToCQuat();
            localQuat = this.current.localAxis.ToCQuat();

            msg.WriteLong(this.current.atRest);
            msg.WriteFloat(this.current.i.position.oGet(0));
            msg.WriteFloat(this.current.i.position.oGet(1));
            msg.WriteFloat(this.current.i.position.oGet(2));
            msg.WriteFloat(quat.x);
            msg.WriteFloat(quat.y);
            msg.WriteFloat(quat.z);
            msg.WriteFloat(this.current.i.linearMomentum.oGet(0), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(this.current.i.linearMomentum.oGet(1), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(this.current.i.linearMomentum.oGet(2), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(this.current.i.angularMomentum.oGet(0), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(this.current.i.angularMomentum.oGet(1), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteFloat(this.current.i.angularMomentum.oGet(2), RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS);
            msg.WriteDeltaFloat(this.current.i.position.oGet(0), this.current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(this.current.i.position.oGet(1), this.current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(this.current.i.position.oGet(2), this.current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(quat.x, localQuat.x);
            msg.WriteDeltaFloat(quat.y, localQuat.y);
            msg.WriteDeltaFloat(quat.z, localQuat.z);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(0), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(1), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(2), RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.externalForce.oGet(0), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.externalForce.oGet(1), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.externalForce.oGet(2), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.externalTorque.oGet(0), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.externalTorque.oGet(1), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.externalTorque.oGet(2), RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            final idCQuat quat = new idCQuat(), localQuat = new idCQuat();

            this.current.atRest = msg.ReadLong();
            this.current.i.position.oSet(0, msg.ReadFloat());
            this.current.i.position.oSet(1, msg.ReadFloat());
            this.current.i.position.oSet(2, msg.ReadFloat());
            quat.x = msg.ReadFloat();
            quat.y = msg.ReadFloat();
            quat.z = msg.ReadFloat();
            this.current.i.linearMomentum.oSet(0, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            this.current.i.linearMomentum.oSet(1, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            this.current.i.linearMomentum.oSet(2, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            this.current.i.angularMomentum.oSet(0, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            this.current.i.angularMomentum.oSet(1, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            this.current.i.angularMomentum.oSet(2, msg.ReadFloat(RB_MOMENTUM_EXPONENT_BITS, RB_MOMENTUM_MANTISSA_BITS));
            this.current.localOrigin.oSet(0, msg.ReadDeltaFloat(this.current.i.position.oGet(0)));
            this.current.localOrigin.oSet(1, msg.ReadDeltaFloat(this.current.i.position.oGet(1)));
            this.current.localOrigin.oSet(2, msg.ReadDeltaFloat(this.current.i.position.oGet(2)));
            localQuat.x = msg.ReadDeltaFloat(quat.x);
            localQuat.y = msg.ReadDeltaFloat(quat.y);
            localQuat.z = msg.ReadDeltaFloat(quat.z);
            this.current.pushVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, RB_VELOCITY_EXPONENT_BITS, RB_VELOCITY_MANTISSA_BITS));
            this.current.externalForce.oSet(0, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            this.current.externalForce.oSet(1, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            this.current.externalForce.oSet(2, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            this.current.externalTorque.oSet(0, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            this.current.externalTorque.oSet(1, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));
            this.current.externalTorque.oSet(2, msg.ReadDeltaFloat(0.0f, RB_FORCE_EXPONENT_BITS, RB_FORCE_MANTISSA_BITS));

            this.current.i.orientation.oSet(quat.ToMat3());
            this.current.localAxis.oSet(localQuat.ToMat3());

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), this.current.i.position, this.current.i.orientation);
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
                final FloatBuffer b = FloatBuffer.wrap(derivatives);
                if (b.hasRemaining()) {
                    this.linearVelocity = new idVec3(b.get(), b.get(), b.get());
                }
                if (b.hasRemaining()) {
                    this.angularMatrix = new idMat3(
                            b.get(), b.get(), b.get(),
                            b.get(), b.get(), b.get(),
                            b.get(), b.get(), b.get());
                }
                if (b.hasRemaining()) {
                    this.force = new idVec3(b.get(), b.get(), b.get());
                }
                if (b.hasRemaining()) {
                    this.torque = new idVec3(b.get(), b.get(), b.get());
                }
            }

            private float[] toFloats() {
                final FloatBuffer buffer = FloatBuffer.allocate(BYTES / Float.BYTES);
                buffer.put(this.linearVelocity.ToFloatPtr())
                        .put(this.angularMatrix.ToFloatPtr())
                        .put(this.force.ToFloatPtr())
                        .put(this.torque.ToFloatPtr());

                return buffer.array();
            }
        }

        private /*friend*/ static class RigidBodyDerivatives extends deriveFunction_t {

            public static final deriveFunction_t INSTANCE = new RigidBodyDerivatives();

            private RigidBodyDerivatives() {
            }

            @Override
            public void run(final float t, final Object clientData, final float[] state, float[] derivatives) {
                final idPhysics_RigidBody p = (idPhysics_RigidBody) clientData;
                final rigidBodyIState_s s = new rigidBodyIState_s(state);//TODO:from float array to object
                // NOTE: this struct should be build conform rigidBodyIState_t
                final rigidBodyDerivatives_s d = new rigidBodyDerivatives_s(derivatives);
                idVec3 angularVelocity;
                idMat3 inverseWorldInertiaTensor;

                inverseWorldInertiaTensor = s.orientation.oMultiply(p.inverseInertiaTensor.oMultiply(s.orientation.Transpose()));
                angularVelocity = inverseWorldInertiaTensor.oMultiply(s.angularMomentum);
                // derivatives
                d.linearVelocity = s.linearMomentum.oMultiply(p.inverseMass);
                d.angularMatrix = SkewSymmetric(angularVelocity).oMultiply(s.orientation);
                d.force = s.linearMomentum.oMultiply(-p.linearFriction).oPlus(p.current.externalForce);
                d.torque = s.angularMomentum.oMultiply(-p.angularFriction).oPlus(p.current.externalTorque);

                Nio.arraycopy(d.toFloats(), 0, derivatives, 0 ,derivatives.length);
            }
        }


        /*
         ================
         idPhysics_RigidBody::Integrate

         Calculate next state from the current state using an integrator.
         ================
         */
        private void Integrate(final float deltaTime, rigidBodyPState_s next) {
            idVec3 position;

            position = new idVec3(this.current.i.position);
            this.current.i.position.oPluSet(this.centerOfMass.oMultiply(this.current.i.orientation));

            this.current.i.orientation.TransposeSelf();

            final float[] newState = next.i.toFloats();
            this.integrator.Evaluate(this.current.i.toFloats(), newState, 0, deltaTime);
            next.i.fromFloats(newState);
            next.i.orientation.OrthoNormalizeSelf();

            // apply gravity
            next.i.linearMomentum.oPluSet(this.gravityVector.oMultiply(this.mass * deltaTime));

            this.current.i.orientation.TransposeSelf();
            next.i.orientation.TransposeSelf();

            this.current.i.position.oSet(position);
            next.i.position.oMinSet(this.centerOfMass.oMultiply(next.i.orientation));

            next.atRest = this.current.atRest;
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
            final idMat3 axis = new idMat3();
            idRotation rotation;
            boolean collided = false;
            boolean startsolid;

            if (TEST_COLLISION_DETECTION) {
                if (gameLocal.clip.Contents(this.current.i.position, this.clipModel, this.current.i.orientation, this.clipMask, this.self) != 0) {
                    startsolid = true;
                }
            }

            TransposeMultiply(this.current.i.orientation, next.i.orientation, axis);
            rotation = axis.ToRotation();
            rotation.SetOrigin(this.current.i.position);

            // if there was a collision
            if (gameLocal.clip.Motion(collision, this.current.i.position, next.i.position, rotation, this.clipModel, this.current.i.orientation, this.clipMask, this.self)) {
                // set the next state to the state at the moment of impact
                next.i.position.oSet(collision[0].endpos);
                next.i.orientation.oSet(collision[0].endAxis);
                next.i.linearMomentum.oSet(this.current.i.linearMomentum);
                next.i.angularMomentum.oSet(this.current.i.angularMomentum);
                collided = true;
            }

            if (TEST_COLLISION_DETECTION) {
                if (gameLocal.clip.Contents(next.i.position, this.clipModel, next.i.orientation, this.clipMask, this.self) != 0) {
                    if (!startsolid) {
                        final int bah = 1;
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
            impactInfo_s info;
            idEntity ent;

            // get info from other entity involved
            ent = gameLocal.entities[collision.c.entityNum];
            info = ent.GetImpactInfo(this.self, collision.c.id, collision.c.point);

            // collision point relative to the body center of mass
            r = collision.c.point.oMinus(this.current.i.position.oPlus(this.centerOfMass.oMultiply(this.current.i.orientation)));
            // the velocity at the collision point
            linearVelocity = this.current.i.linearMomentum.oMultiply(this.inverseMass);
            inverseWorldInertiaTensor = this.current.i.orientation.Transpose().oMultiply(this.inverseInertiaTensor.oMultiply(this.current.i.orientation));
            angularVelocity = inverseWorldInertiaTensor.oMultiply(this.current.i.angularMomentum);
            velocity = linearVelocity.oPlus(angularVelocity.Cross(r));
            // subtract velocity of other entity
            velocity.oMinSet(info.velocity);

            // velocity in normal direction
            vel = velocity.oMultiply(collision.c.normal);

            if (vel > -STOP_SPEED) {
                impulseNumerator = STOP_SPEED;
            } else {
                impulseNumerator = -(1.0f + this.bouncyness) * vel;
            }
            impulseDenominator = this.inverseMass + ((inverseWorldInertiaTensor.oMultiply(r.Cross(collision.c.normal)).Cross(r)).oMultiply(collision.c.normal));
            if (info.invMass != 0) {
                impulseDenominator += info.invMass + ((info.invInertiaTensor.oMultiply(info.position.Cross(collision.c.normal))).Cross(info.position).oMultiply(collision.c.normal));
            }
            impulse.oSet(collision.c.normal.oMultiply((impulseNumerator / impulseDenominator)));

            // update linear and angular momentum with impulse
            this.current.i.linearMomentum.oPluSet(impulse);
            this.current.i.angularMomentum.oPluSet(r.Cross(impulse));

            // if no movement at all don't blow up
            if (collision.fraction < 0.0001f) {
                this.current.i.linearMomentum.oMulSet(0.5f);
                this.current.i.angularMomentum.oMulSet(0.5f);
            }

            // callback to self to let the entity know about the collision
            return this.self.Collide(collision, velocity);
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

            inverseWorldInertiaTensor = this.current.i.orientation.Transpose().oMultiply(this.inverseInertiaTensor.oMultiply(this.current.i.orientation));

            massCenter = this.current.i.position.oPlus(this.centerOfMass.oMultiply(this.current.i.orientation));

            for (i = 0; i < this.contacts.Num(); i++) {

                r = this.contacts.oGet(i).point.oMinus(massCenter);

                // calculate velocity at contact point
                linearVelocity = this.current.i.linearMomentum.oMultiply(this.inverseMass);
                angularVelocity = inverseWorldInertiaTensor.oMultiply(this.current.i.angularMomentum);
                velocity = linearVelocity.oPlus(angularVelocity.Cross(r));

                // velocity along normal vector
                normalVelocity = this.contacts.oGet(i).normal.oMultiply(velocity.oMultiply(this.contacts.oGet(i).normal));

                // calculate friction impulse
                normal = (velocity.oMinus(normalVelocity)).oNegative();
                magnitude = normal.Normalize();
                impulseNumerator = this.contactFriction * magnitude;
                impulseDenominator = this.inverseMass + ((inverseWorldInertiaTensor.oMultiply(r.Cross(normal))).Cross(r).oMultiply(normal));
                impulse = normal.oMultiply((impulseNumerator / impulseDenominator));

                // apply friction impulse
                this.current.i.linearMomentum.oPluSet(impulse);
                this.current.i.angularMomentum.oPluSet(r.Cross(impulse));

                // if moving towards the surface at the contact point
                if (normalVelocity.oMultiply(this.contacts.oGet(i).normal) < 0.0f) {
                    // calculate impulse
                    normal = normalVelocity.oNegative();
                    impulseNumerator = normal.Normalize();
                    impulseDenominator = this.inverseMass + ((inverseWorldInertiaTensor.oMultiply(r.Cross(normal))).Cross(r).oMultiply(normal));
                    impulse = normal.oMultiply((impulseNumerator / impulseDenominator));

                    // apply impulse
                    this.current.i.linearMomentum.oPluSet(impulse);
                    this.current.i.angularMomentum.oPluSet(r.Cross(impulse));
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
            final trace_s[] tr = {null};

            if(this.DBG_count==8209){
                final int bla = 1;
            }

            if (this.testSolid) {

                this.testSolid = false;

                if (gameLocal.clip.Contents(this.current.i.position, this.clipModel, this.current.i.orientation, this.clipMask, this.self) != 0) {
                    gameLocal.DWarning("rigid body in solid for entity '%s' type '%s' at (%s)",
                            this.self.name, this.self.GetType().getName(), this.current.i.position.ToString(0));
                    Rest();
                    this.dropToFloor = false;
                    return;
                }
            }


            // put the body on the floor
            down = this.current.i.position.oPlus(this.gravityNormal.oMultiply(128.0f));
            gameLocal.clip.Translation(tr, this.current.i.position, down, this.clipModel, this.current.i.orientation, this.clipMask, this.self);
            this.current.i.position.oSet(tr[0].endpos);
            this.clipModel.Link(gameLocal.clip, this.self, this.clipModel.GetId(), tr[0].endpos, this.current.i.orientation);

            // if on the floor already
            if (tr[0].fraction == 0.0f) {
                // test if we are really at rest
                EvaluateContacts();
                if (!TestIfAtRest()) {
                    gameLocal.DWarning("rigid body not at rest for entity '%s' type '%s' at (%s)",
                            this.self.name, this.self.GetType().getName(), this.current.i.position.ToString(0));
                }
                Rest();
                this.dropToFloor = false;
            } else if (IsOutsideWorld()) {
                gameLocal.Warning("rigid body outside world bounds for entity '%s' type '%s' at (%s)",
                        this.self.name, this.self.GetType().getName(), this.current.i.position.ToString(0));
                Rest();
                this.dropToFloor = false;
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
            idVec3 v, av;
			final idVec3 normal = new idVec3();
			idVec3 point;
            idMat3 inverseWorldInertiaTensor;
            final idFixedWinding contactWinding = new idFixedWinding();

            if (this.current.atRest >= 0) {
                return true;
            }

            // need at least 3 contact points to come to rest
            if (this.contacts.Num() < 3) {
                return false;
            }

            // get average contact plane normal
            normal.Zero();
            for (i = 0; i < this.contacts.Num(); i++) {
                normal.oPluSet(this.contacts.oGet(i).normal);
            }
            normal.oDivSet(this.contacts.Num());
            normal.Normalize();

            // if on a too steep surface
            if ((normal.oMultiply(this.gravityNormal)) > -0.7f) {
                return false;
            }

            // create bounds for contact points
            contactWinding.Clear();
            for (i = 0; i < this.contacts.Num(); i++) {
                // project point onto plane through origin orthogonal to the gravity
                point = this.contacts.oGet(i).point.oMinus(this.gravityNormal.oMultiply(this.contacts.oGet(i).point.oMultiply(this.gravityNormal)));
                contactWinding.AddToConvexHull(point, this.gravityNormal);
            }

            // need at least 3 contact points to come to rest
            if (contactWinding.GetNumPoints() < 3) {
                return false;
            }

            // center of mass in world space
            point = this.current.i.position.oPlus(this.centerOfMass.oMultiply(this.current.i.orientation));
            point.oMinSet(this.gravityNormal.oMultiply(point.oMultiply(this.gravityNormal)));

            // if the point is not inside the winding
            if (!contactWinding.PointInside(this.gravityNormal, point, 0)) {
                return false;
            }

            // linear velocity of body
            v = this.current.i.linearMomentum.oMultiply(this.inverseMass);
            // linear velocity in gravity direction
            gv = v.oMultiply(this.gravityNormal);
            // linear velocity orthogonal to gravity direction
            v.oMinSet(this.gravityNormal.oMultiply(gv));

            // if too much velocity orthogonal to gravity direction
            if (v.Length() > STOP_SPEED) {
                return false;
            }
            // if too much velocity in gravity direction
            if ((gv > (2.0f * STOP_SPEED)) || (gv < (-2.0f * STOP_SPEED))) {
                return false;
            }

            // calculate rotational velocity
            inverseWorldInertiaTensor = this.current.i.orientation.oMultiply(this.inverseInertiaTensor.oMultiply(this.current.i.orientation.Transpose()));
            av = inverseWorldInertiaTensor.oMultiply(this.current.i.angularMomentum);

            // if too much rotational velocity
            if (av.LengthSqr() > STOP_SPEED) {
                return false;
            }

            return true;
        }

        private void Rest() {
            this.current.atRest = gameLocal.time;
            this.current.i.linearMomentum.Zero();
            this.current.i.angularMomentum.Zero();
            this.self.BecomeInactive(TH_PHYSICS);
        }

        private void DebugDraw() {

            if (rb_showBodies.GetBool() || (rb_showActive.GetBool() && (this.current.atRest < 0))) {
                CollisionModel_local.collisionModelManager.DrawModel(this.clipModel.Handle(), this.clipModel.GetOrigin(), this.clipModel.GetAxis(), getVec3_origin(), 0.0f);
            }

            if (rb_showMass.GetBool()) {
                gameRenderWorld.DrawText(va("\n%1.2f", this.mass), this.current.i.position, 0.08f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
            }

            if (rb_showInertia.GetBool()) {
                final idMat3 I = this.inertiaTensor;
                gameRenderWorld.DrawText(va("\n\n\n( %.1f %.1f %.1f )\n( %.1f %.1f %.1f )\n( %.1f %.1f %.1f )",
                        I.oGet(0).x, I.oGet(0).y, I.oGet(0).z,
                        I.oGet(1).x, I.oGet(1).y, I.oGet(1).z,
                        I.oGet(2).x, I.oGet(2).y, I.oGet(2).z),
                        this.current.i.position, 0.05f, colorCyan, gameLocal.GetLocalPlayer().viewAngles.ToMat3(), 1);
            }

            if (rb_showVelocity.GetBool()) {
                DrawVelocity(this.clipModel.GetId(), 0.1f, 4.0f);
            }
        }
    }

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
        final int[] atRest = {0};
        final float[] lastTimeStep = {0};

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
