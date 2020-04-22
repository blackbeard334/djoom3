package neo.Game.Physics;

import static neo.Game.GameSys.SysCvar.g_gravity;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.idlib.BV.Bounds.bounds_zero;
import static neo.idlib.math.Vector.getVec3_origin;

import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force.idForce;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Physics_Static {

    /*
     ===============================================================================

     Physics for a non moving object using at most one collision model.

     ===============================================================================
     */
    public static class staticPState_s {

        idVec3 origin;
        idMat3 axis;
        idVec3 localOrigin;
        idMat3 localAxis;

        public staticPState_s() {
            this.origin = new idVec3();
            this.axis = new idMat3();
            this.localOrigin = new idVec3();
            this.localAxis = new idMat3();
        }
    }

    public static class idPhysics_Static extends idPhysics {
        // CLASS_PROTOTYPE( idPhysics_Static );

        protected idEntity       self;             // entity using this physics object
        protected staticPState_s current;          // physics state
        protected idClipModel    clipModel;        // collision model
        //
        // master
        protected boolean        hasMaster;
        protected boolean        isOrientated;
        //
        //

        public idPhysics_Static() {
            this.self = null;
            this.clipModel = null;
            this.current = new staticPState_s();
            this.current.origin.Zero();
            this.current.axis.Identity();
            this.current.localOrigin.Zero();
            this.current.localAxis.Identity();
            this.hasMaster = false;
            this.isOrientated = false;
        }

        // ~idPhysics_Static();
        @Override
        protected void _deconstructor() {
            if ((this.self != null) && (this.self.GetPhysics() == this)) {
                this.self.SetPhysics(null);
            }
            idForce.DeletePhysics(this);
            if (this.clipModel != null) {
                idClipModel.delete(this.clipModel);
            }

            super._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteObject(this.self);

            savefile.WriteVec3(this.current.origin);
            savefile.WriteMat3(this.current.axis);
            savefile.WriteVec3(this.current.localOrigin);
            savefile.WriteMat3(this.current.localAxis);
            savefile.WriteClipModel(this.clipModel);

            savefile.WriteBool(this.hasMaster);
            savefile.WriteBool(this.isOrientated);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadObject(this./*reinterpret_cast<idClass*&>*/self);

            savefile.ReadVec3(this.current.origin);
            savefile.ReadMat3(this.current.axis);
            savefile.ReadVec3(this.current.localOrigin);
            savefile.ReadMat3(this.current.localAxis);
            savefile.ReadClipModel(this.clipModel);

            this.hasMaster = savefile.ReadBool();
            this.isOrientated = savefile.ReadBool();
        }

        // common physics interface
        @Override
        public void SetSelf(idEntity e) {
            assert (e != null);
            this.self = e;
        }

        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
            assert (this.self != null);

            if ((this.clipModel != null) && (this.clipModel != model) && freeOld) {
                idClipModel.delete(this.clipModel);
            }
            this.clipModel = model;
            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            if (this.clipModel != null) {
                return this.clipModel;
            }
            return gameLocal.clip.DefaultClipModel();
        }

        @Override
        public int GetNumClipModels() {
            return (this.clipModel != null ? 1 : 0);
        }

        @Override
        public void SetMass(float mass, int id /*= -1*/) {
        }

        @Override
        public float GetMass(int id /*= -1*/) {
            return 0.0f;
        }

        @Override
        public void SetContents(int contents, int id /*= -1*/) {
            if (this.clipModel != null) {
                this.clipModel.SetContents(contents);
            }
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            if (this.clipModel != null) {
                return this.clipModel.GetContents();
            }
            return 0;
        }

        @Override
        public void SetClipMask(int mask, int id /*= -1*/) {
        }

        @Override
        public int GetClipMask(int id /*= -1*/) {
            return 0;
        }

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            if (this.clipModel != null) {
                return this.clipModel.GetBounds();
            }
            return bounds_zero;
        }
        private static idBounds absBounds;

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {

            if (this.clipModel != null) {
                return this.clipModel.GetAbsBounds();
            }
            absBounds = new idBounds(this.current.origin, this.current.origin);
            return absBounds;
        }

        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            final idVec3 masterOrigin = new idVec3(), oldOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3(), oldAxis = new idMat3();

            if (this.hasMaster) {
                oldOrigin.oSet(this.current.origin);
                oldAxis.oSet(this.current.axis);

                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.origin.oSet(masterOrigin.oPlus(this.current.localOrigin.oMultiply(masterAxis)));
                if (this.isOrientated) {
                    this.current.axis.oSet(this.current.localAxis.oMultiply(masterAxis));
                } else {
                    this.current.axis.oSet(this.current.localAxis);
                }
                if (this.clipModel != null) {
                    this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
                }

                return (!this.current.origin.equals(oldOrigin) || !this.current.axis.equals(oldAxis));
            }
            return false;
        }

        @Override
        public void UpdateTime(int endTimeMSec) {
        }

        @Override
        public int GetTime() {
            return 0;
        }

        @Override
        public impactInfo_s GetImpactInfo(final int id, final idVec3 point) {
            return new impactInfo_s();
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
        }

        @Override
        public void AddForce(final int id, final idVec3 point, final idVec3 force) {
        }

        @Override
        public void Activate() {
        }

        @Override
        public void PutToRest() {
        }

        @Override
        public boolean IsAtRest() {
            return true;
        }

        @Override
        public int GetRestStartTime() {
            return 0;
        }

        @Override
        public boolean IsPushable() {
            return false;
        }

        @Override
        public void SaveState() {
        }

        @Override
        public void RestoreState() {
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.localOrigin.oSet(newOrigin);

            if (this.hasMaster) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.origin.oSet(masterOrigin.oPlus(newOrigin.oMultiply(masterAxis)));
            } else {
                this.current.origin.oSet(newOrigin);
            }

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.localAxis.oSet(newAxis);

            if (this.hasMaster && this.isOrientated) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.axis.oSet(newAxis.oMultiply(masterAxis));
            } else {
                this.current.axis.oSet(newAxis);
            }

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {
            this.current.localOrigin.oPluSet(translation);
            this.current.origin.oPluSet(translation);

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.origin.oMulSet(rotation);
            this.current.axis.oMulSet(rotation.ToMat3());

            if (this.hasMaster) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.localAxis.oMulSet(rotation.ToMat3());
                this.current.localOrigin.oSet((this.current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
            } else {
                this.current.localAxis.oSet(this.current.axis);
                this.current.localOrigin.oSet(this.current.origin);
            }

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            return new idVec3(this.current.origin);
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return new idMat3(this.current.axis);
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
        }

        @Override
        public void SetAngularVelocity(final idVec3 newAngularVelocity, int id /*= 0*/) {
        }

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            return getVec3_origin();
        }

        @Override
        public idVec3 GetAngularVelocity(int id /*= 0*/) {
            return getVec3_origin();
        }

        @Override
        public void SetGravity(final idVec3 newGravity) {
        }
        private static final idVec3 gravity = new idVec3(0, 0, -g_gravity.GetFloat());

        @Override
        public idVec3 GetGravity() {
            return gravity;
        }
        private static final idVec3 gravityNormal = new idVec3(0, 0, -1);

        @Override
        public idVec3 GetGravityNormal() {
            return gravityNormal;
        }

        @Override
        public void ClipTranslation(trace_s[] results, final idVec3 translation, final idClipModel model) {
            if (model != null) {
                gameLocal.clip.TranslationModel(results, this.current.origin, this.current.origin.oPlus(translation),
                        this.clipModel, this.current.axis, MASK_SOLID, model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                gameLocal.clip.Translation(results, this.current.origin, this.current.origin.oPlus(translation),
                        this.clipModel, this.current.axis, MASK_SOLID, this.self);
            }
        }

        @Override
        public void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model) {
            if (model != null) {
                gameLocal.clip.RotationModel(results, this.current.origin, rotation,
                        this.clipModel, this.current.axis, MASK_SOLID, model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                gameLocal.clip.Rotation(results, this.current.origin, rotation, this.clipModel, this.current.axis, MASK_SOLID, this.self);
            }
        }

        @Override
        public int ClipContents(final idClipModel model) {
            if (this.clipModel != null) {
                if (model != null) {
                    return gameLocal.clip.ContentsModel(this.clipModel.GetOrigin(), this.clipModel, this.clipModel.GetAxis(), -1,
                            model.Handle(), model.GetOrigin(), model.GetAxis());
                } else {
                    return gameLocal.clip.Contents(this.clipModel.GetOrigin(), this.clipModel, this.clipModel.GetAxis(), -1, null);
                }
            }
            return 0;
        }

        @Override
        public void DisableClip() {
            if (this.clipModel != null) {
                this.clipModel.Disable();
            }
        }

        @Override
        public void EnableClip() {
            if (this.clipModel != null) {
                this.clipModel.Enable();
            }
        }

        @Override
        public void UnlinkClip() {
            if (this.clipModel != null) {
                this.clipModel.Unlink();
            }
        }

        @Override
        public void LinkClip() {
            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        @Override
        public boolean EvaluateContacts() {
            return false;
        }

        @Override
        public int GetNumContacts() {
            return 0;
        }
        private static contactInfo_t info;

        @Override
        public contactInfo_t GetContact(int num) {
//	memset( &info, 0, sizeof( info ) );
            return info = new contactInfo_t();
        }

        @Override
        public void ClearContacts() {
        }

        @Override
        public void AddContactEntity(idEntity e) {
        }

        @Override
        public void RemoveContactEntity(idEntity e) {
        }

        @Override
        public boolean HasGroundContacts() {
            return false;
        }

        @Override
        public boolean IsGroundEntity(int entityNum) {
            return false;
        }

        @Override
        public boolean IsGroundClipModel(int entityNum, int id) {
            return false;
        }

        @Override
        public void SetPushed(int deltaTime) {
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return getVec3_origin();
        }

        @Override
        public idVec3 GetPushedAngularVelocity(final int id /*= 0*/) {
            return getVec3_origin();
        }

        @Override
        public void SetMaster(idEntity master, final boolean orientated /*= true*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (!this.hasMaster) {
                    // transform from world space to master space
                    this.self.GetMasterPosition(masterOrigin, masterAxis);
                    this.current.localOrigin.oSet((this.current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                    if (orientated) {
                        this.current.localAxis.oSet(this.current.axis.oMultiply(masterAxis.Transpose()));
                    } else {
                        this.current.localAxis.oSet(this.current.axis);
                    }
                    this.hasMaster = true;
                    this.isOrientated = orientated;
                }
            } else {
                if (this.hasMaster) {
                    this.hasMaster = false;
                }
            }
        }

        @Override
        public trace_s GetBlockingInfo() {
            return null;
        }

        @Override
        public idEntity GetBlockingEntity() {
            return null;
        }

        @Override
        public int GetLinearEndTime() {
            return 0;
        }

        @Override
        public int GetAngularEndTime() {
            return 0;
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            idCQuat quat, localQuat;

            quat = this.current.axis.ToCQuat();
            localQuat = this.current.localAxis.ToCQuat();

            msg.WriteFloat(this.current.origin.oGet(0));
            msg.WriteFloat(this.current.origin.oGet(1));
            msg.WriteFloat(this.current.origin.oGet(2));
            msg.WriteFloat(quat.x);
            msg.WriteFloat(quat.y);
            msg.WriteFloat(quat.z);
            msg.WriteDeltaFloat(this.current.origin.oGet(0), this.current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(this.current.origin.oGet(1), this.current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(this.current.origin.oGet(2), this.current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(quat.x, localQuat.x);
            msg.WriteDeltaFloat(quat.y, localQuat.y);
            msg.WriteDeltaFloat(quat.z, localQuat.z);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            final idCQuat quat = new idCQuat(), localQuat = new idCQuat();

            this.current.origin.oSet(0, msg.ReadFloat());
            this.current.origin.oSet(1, msg.ReadFloat());
            this.current.origin.oSet(2, msg.ReadFloat());
            quat.x = msg.ReadFloat();
            quat.y = msg.ReadFloat();
            quat.z = msg.ReadFloat();
            this.current.localOrigin.oSet(0, msg.ReadDeltaFloat(this.current.origin.oGet(0)));
            this.current.localOrigin.oSet(1, msg.ReadDeltaFloat(this.current.origin.oGet(1)));
            this.current.localOrigin.oSet(2, msg.ReadDeltaFloat(this.current.origin.oGet(2)));
            localQuat.x = msg.ReadDeltaFloat(quat.x);
            localQuat.y = msg.ReadDeltaFloat(quat.y);
            localQuat.z = msg.ReadDeltaFloat(quat.z);

            this.current.axis.oSet(quat.ToMat3());
            this.current.localAxis.oSet(localQuat.ToMat3());
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class<?> /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
