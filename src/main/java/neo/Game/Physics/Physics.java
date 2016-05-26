package neo.Game.Physics;

import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Physics {
    /*
     ===============================================================================

     Physics abstract class

     A physics object is a tool to manipulate the position and orientation of
     an entity. The physics object is a container for idClipModels used for
     collision detection. The physics deals with moving these collision models
     through the world according to the laws of physics or other rules.

     The mass of a clip model is the volume of the clip model times the density.
     An arbitrary mass can however be set for specific clip models or the
     whole physics object. The contents of a clip model is a set of bit flags
     that define the contents. The clip mask defines the contents a clip model
     collides with.

     The linear velocity of a physics object is a vector that defines the
     translation of the center of mass in units per second. The angular velocity
     of a physics object is a vector that passes through the center of mass. The
     direction of this vector defines the axis of rotation and the magnitude
     defines the rate of rotation about the axis in radians per second.
     The gravity is the change in velocity per second due to gravitational force.

     Entities update their visual position and orientation from the physics
     using GetOrigin() and GetAxis(). Direct origin and axis changes of
     entities should go through the physics. In other words the physics origin
     and axis are updated first and the entity updates it's visual position
     from the physics.

     ===============================================================================
     */

    public static final float CONTACT_EPSILON = 0.25f;				// maximum contact seperation distance

    public static class impactInfo_s {

        float invMass;			// inverse mass
        idMat3 invInertiaTensor;	// inverse inertia tensor
        idVec3 position;		// impact position relative to center of mass
        idVec3 velocity;		// velocity at the impact position

        void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    public static abstract class idPhysics extends idClass {
        // ABSTRACT_PROTOTYPE( idPhysics );
        private static  int DBG_counter = 0;
        protected final int DBG_count   = DBG_counter++;

        // virtual						~idPhysics();
        public static int SnapTimeToPhysicsFrame(int t) {
            int s;
            s = t + USERCMD_MSEC - 1;
            return (s - s % USERCMD_MSEC);
        }

        // Must not be virtual
        @Override
        public void Save(idSaveGame savefile) {
        }

        @Override
        public void Restore(idRestoreGame savefile) {
        }

        // common physics interface
        // set pointer to entity using physics
        public abstract void SetSelf(idEntity e);

        // clip models
        public abstract void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/);

        public void SetClipModel(idClipModel model, float density, int id /*= 0*/) {
            SetClipModel(model, density, id, true);
        }

        public void SetClipModel(idClipModel model, float density) {
            SetClipModel(model, density, 0);
        }

        public void SetClipBox(final idBounds bounds, float density) {
            SetClipModel(new idClipModel(new idTraceModel(bounds)), density);
        }

        public abstract idClipModel GetClipModel(int id /*= 0*/);

        public idClipModel GetClipModel() {
            return GetClipModel(0);
        }

        public abstract int GetNumClipModels();

        // get/set the mass of a specific clip model or the whole physics object
        public abstract void SetMass(float mass, int id /*= -1*/);

        public void SetMass(float mass) {
            SetMass(mass, -1);
        }

        public final float GetMass() {//TODO:make sure this shouldn't be overrided.
            return GetMass(-1);
        }

        public abstract float GetMass(int id /*= -1*/);

        // get/set the contents of a specific clip model or the whole physics object
        public abstract void SetContents(int contents, int id /*= -1*/);

        public void SetContents(int contents) {
            SetContents(contents, -1);
        }

        public abstract int GetContents(int id /*= -1*/);

        public int GetContents() {
            return GetContents(-1);
        }

        // get/set the contents a specific clip model or the whole physics object collides with
        public abstract void SetClipMask(int mask, int id /*= -1*/);

        public void SetClipMask(int mask) {
            SetClipMask(mask, -1);
        }

        public abstract int GetClipMask(int id /*= -1*/);

        public int GetClipMask() {
            return GetClipMask(-1);
        }

        // get the bounds of a specific clip model or the whole physics object
        public abstract idBounds GetBounds(int id /*= -1*/);

        public idBounds GetBounds() {
            return GetBounds(-1);
        }

        public abstract idBounds GetAbsBounds(int id /*= -1*/);

        public idBounds GetAbsBounds() {
            return GetAbsBounds(-1);
        }

        // evaluate the physics with the given time step, returns true if the object moved
        public abstract boolean Evaluate(int timeStepMSec, int endTimeMSec);

        // update the time without moving
        public abstract void UpdateTime(int endTimeMSec);

        // get the last physics update time
        public abstract int GetTime();
        // collision interaction between different physics objects

        public abstract void GetImpactInfo(final int id, final idVec3 point, impactInfo_s info);

        public abstract void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse);

        public abstract void AddForce(final int id, final idVec3 point, final idVec3 force);

        public abstract void Activate();

        public abstract void PutToRest();

        public abstract boolean IsAtRest();

        public abstract int GetRestStartTime();

        public abstract boolean IsPushable();

        // save and restore the physics state
        public abstract void SaveState();

        public abstract void RestoreState();

        // set the position and orientation in master space or world space if no master set
        public abstract void SetOrigin(final idVec3 newOrigin, int id /*= -1*/);

        public void SetOrigin(final idVec3 newOrigin) {
            SetOrigin(newOrigin, -1);
        }

        public abstract void SetAxis(final idMat3 newAxis, int id /*= -1*/);

        public void SetAxis(final idMat3 newAxis) {
            SetAxis(newAxis, -1);
        }

        // translate or rotate the physics object in world space
        public abstract void Translate(final idVec3 translation, int id /*= -1*/);

        public void Translate(final idVec3 translation) {
            Translate(translation, -1);
        }

        public abstract void Rotate(final idRotation rotation, int id /*= -1*/);

        public void Rotate(final idRotation rotation) {
            Rotate(rotation, -1);
        }

        // get the position and orientation in world space
        public abstract idVec3 GetOrigin(int id /*= 0*/);

        public idVec3 GetOrigin() {
            return GetOrigin(0);
        }

        public final idMat3 GetAxis() {
            return GetAxis(0);
        }

        public abstract idMat3 GetAxis(int id /*= 0*/);

        // set linear and angular velocity
        public abstract void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/);

        public void SetLinearVelocity(final idVec3 newLinearVelocity) {
            SetLinearVelocity(newLinearVelocity, 0);
        }

        public abstract void SetAngularVelocity(final idVec3 newAngularVelocity, int id /*= 0*/);

        public void SetAngularVelocity(final idVec3 newAngularVelocity) {
            SetAngularVelocity(newAngularVelocity, 0);
        }

        // get linear and angular velocity
        public abstract idVec3 GetLinearVelocity(int id /*= 0*/);

        public idVec3 GetLinearVelocity() {
            return GetLinearVelocity(00);
        }

        public abstract idVec3 GetAngularVelocity(int id /*= 0*/);

        public idVec3 GetAngularVelocity() {
            return GetAngularVelocity(0);
        }

        // gravity
        public abstract void SetGravity(final idVec3 newGravity);

        public abstract idVec3 GetGravity();

        public abstract idVec3 GetGravityNormal();

        // get first collision when translating or rotating this physics object
        public abstract void ClipTranslation(trace_s[] results, final idVec3 translation, final idClipModel model);

        public abstract void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model);

        public abstract int ClipContents(final idClipModel model);

        // disable/enable the clip models contained by this physics object
        public abstract void DisableClip();

        public abstract void EnableClip();

        // link/unlink the clip models contained by this physics object
        public abstract void UnlinkClip();

        public abstract void LinkClip();

        // contacts
        public abstract boolean EvaluateContacts();

        public abstract int GetNumContacts();

        public abstract contactInfo_t GetContact(int num);

        public abstract void ClearContacts();

        public abstract void AddContactEntity(idEntity e);

        public abstract void RemoveContactEntity(idEntity e);

        // ground contacts
        public abstract boolean HasGroundContacts();

        public abstract boolean IsGroundEntity(int entityNum);

        public abstract boolean IsGroundClipModel(int entityNum, int id);

        // set the master entity for objects bound to a master
        public abstract void SetMaster(idEntity master, final boolean orientated /*= true*/);

        // set pushed state
        public abstract void SetPushed(int deltaTime);

        public abstract idVec3 GetPushedLinearVelocity(final int id /*= 0*/);

        public abstract idVec3 GetPushedAngularVelocity(final int id /*= 0*/);

        // get blocking info, returns NULL if the object is not blocked
        public abstract trace_s GetBlockingInfo();

        public abstract idEntity GetBlockingEntity();

        // movement end times in msec for reached events at the end of predefined motion
        public abstract int GetLinearEndTime();

        public abstract int GetAngularEndTime();

        // networking
        public abstract void WriteToSnapshot(idBitMsgDelta msg);

        public abstract void ReadFromSnapshot(final idBitMsgDelta msg);

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return null;
        }
    };
}
