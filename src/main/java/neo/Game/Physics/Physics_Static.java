package neo.Game.Physics;

import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.g_gravity;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics.impactInfo_s;
import static neo.idlib.BV.Bounds.bounds_zero;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

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
    };

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
            self = null;
            clipModel = null;
            current = new staticPState_s();
            current.origin.Zero();
            current.axis.Identity();
            current.localOrigin.Zero();
            current.localAxis.Identity();
            hasMaster = false;
            isOrientated = false;
        }
        // ~idPhysics_Static();

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteObject(self);

            savefile.WriteVec3(current.origin);
            savefile.WriteMat3(current.axis);
            savefile.WriteVec3(current.localOrigin);
            savefile.WriteMat3(current.localAxis);
            savefile.WriteClipModel(clipModel);

            savefile.WriteBool(hasMaster);
            savefile.WriteBool(isOrientated);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadObject(/*reinterpret_cast<idClass*&>*/self);

            savefile.ReadVec3(current.origin);
            savefile.ReadMat3(current.axis);
            savefile.ReadVec3(current.localOrigin);
            savefile.ReadMat3(current.localAxis);
            savefile.ReadClipModel(clipModel);

            hasMaster = savefile.ReadBool();
            isOrientated = savefile.ReadBool();
        }

        // common physics interface
        @Override
        public void SetSelf(idEntity e) {
            assert (e != null);
            self = e;
        }

        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
            assert (self != null);

//	if ( clipModel && clipModel != model && freeOld ) {
//		delete clipModel;
//	}
            clipModel = model;
            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            if (clipModel != null) {
                return clipModel;
            }
            return gameLocal.clip.DefaultClipModel();
        }

        @Override
        public int GetNumClipModels() {
            return (clipModel != null ? 1 : 0);
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
            if (clipModel != null) {
                clipModel.SetContents(contents);
            }
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            if (clipModel != null) {
                return clipModel.GetContents();
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
            if (clipModel != null) {
                return clipModel.GetBounds();
            }
            return bounds_zero;
        }
        private static idBounds absBounds;

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {

            if (clipModel != null) {
                return clipModel.GetAbsBounds();
            }
            absBounds = new idBounds(current.origin, current.origin);
            return absBounds;
        }

        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            idVec3 masterOrigin = new idVec3(), oldOrigin;
            idMat3 masterAxis = new idMat3(), oldAxis;

            if (hasMaster) {
                oldOrigin = current.origin;
                oldAxis = current.axis;

                self.GetMasterPosition(masterOrigin, masterAxis);
                current.origin.oSet(masterOrigin.oPlus(current.localOrigin.oMultiply(masterAxis)));
                if (isOrientated) {
                    current.axis.oSet(current.localAxis.oMultiply(masterAxis));
                } else {
                    current.axis.oSet(current.localAxis);
                }
                if (clipModel != null) {
                    clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
                }

                return (current.origin != oldOrigin || current.axis != oldAxis);
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
        public void GetImpactInfo(final int id, final idVec3 point, impactInfo_s info) {
//	memset( info, 0, sizeof( *info ) );//TODO:
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
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.localOrigin.oSet(newOrigin);

            if (hasMaster) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.origin.oSet(masterOrigin.oPlus(newOrigin.oMultiply(masterAxis)));
            } else {
                current.origin.oSet(newOrigin);
            }

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.localAxis.oSet(newAxis);

            if (hasMaster && isOrientated) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.axis.oSet(newAxis.oMultiply(masterAxis));
            } else {
                current.axis.oSet(newAxis);
            }

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {
            current.localOrigin.oPluSet(translation);
            current.origin.oPluSet(translation);

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.origin.oMulSet(rotation);
            current.axis.oMulSet(rotation.ToMat3());

            if (hasMaster) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.localAxis.oMulSet(rotation.ToMat3());
                current.localOrigin.oSet((current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
            } else {
                current.localAxis.oSet(current.axis);
                current.localOrigin.oSet(current.origin);
            }

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            return current.origin;
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return current.axis;
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
                gameLocal.clip.TranslationModel(results, current.origin, current.origin.oPlus(translation),
                        clipModel, current.axis, MASK_SOLID, model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                gameLocal.clip.Translation(results, current.origin, current.origin.oPlus(translation),
                        clipModel, current.axis, MASK_SOLID, self);
            }
        }

        @Override
        public void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model) {
            if (model != null) {
                gameLocal.clip.RotationModel(results, current.origin, rotation,
                        clipModel, current.axis, MASK_SOLID, model.Handle(), model.GetOrigin(), model.GetAxis());
            } else {
                gameLocal.clip.Rotation(results, current.origin, rotation, clipModel, current.axis, MASK_SOLID, self);
            }
        }

        @Override
        public int ClipContents(final idClipModel model) {
            if (clipModel != null) {
                if (model != null) {
                    return gameLocal.clip.ContentsModel(clipModel.GetOrigin(), clipModel, clipModel.GetAxis(), -1,
                            model.Handle(), model.GetOrigin(), model.GetAxis());
                } else {
                    return gameLocal.clip.Contents(clipModel.GetOrigin(), clipModel, clipModel.GetAxis(), -1, null);
                }
            }
            return 0;
        }

        @Override
        public void DisableClip() {
            if (clipModel != null) {
                clipModel.Disable();
            }
        }

        @Override
        public void EnableClip() {
            if (clipModel != null) {
                clipModel.Enable();
            }
        }

        @Override
        public void UnlinkClip() {
            if (clipModel != null) {
                clipModel.Unlink();
            }
        }

        @Override
        public void LinkClip() {
            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
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
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (!hasMaster) {
                    // transform from world space to master space
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    current.localOrigin.oSet((current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                    if (orientated) {
                        current.localAxis.oSet(current.axis.oMultiply(masterAxis.Transpose()));
                    } else {
                        current.localAxis.oSet(current.axis);
                    }
                    hasMaster = true;
                    isOrientated = orientated;
                }
            } else {
                if (hasMaster) {
                    hasMaster = false;
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

            quat = current.axis.ToCQuat();
            localQuat = current.localAxis.ToCQuat();

            msg.WriteFloat(current.origin.oGet(0));
            msg.WriteFloat(current.origin.oGet(1));
            msg.WriteFloat(current.origin.oGet(2));
            msg.WriteFloat(quat.x);
            msg.WriteFloat(quat.y);
            msg.WriteFloat(quat.z);
            msg.WriteDeltaFloat(current.origin.oGet(0), current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(current.origin.oGet(1), current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(current.origin.oGet(2), current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(quat.x, localQuat.x);
            msg.WriteDeltaFloat(quat.y, localQuat.y);
            msg.WriteDeltaFloat(quat.z, localQuat.z);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            idCQuat quat = new idCQuat(), localQuat = new idCQuat();

            current.origin.oSet(0, msg.ReadFloat());
            current.origin.oSet(1, msg.ReadFloat());
            current.origin.oSet(2, msg.ReadFloat());
            quat.x = msg.ReadFloat();
            quat.y = msg.ReadFloat();
            quat.z = msg.ReadFloat();
            current.localOrigin.oSet(0, msg.ReadDeltaFloat(current.origin.oGet(0)));
            current.localOrigin.oSet(1, msg.ReadDeltaFloat(current.origin.oGet(1)));
            current.localOrigin.oSet(2, msg.ReadDeltaFloat(current.origin.oGet(2)));
            localQuat.x = msg.ReadDeltaFloat(quat.x);
            localQuat.y = msg.ReadDeltaFloat(quat.y);
            localQuat.z = msg.ReadDeltaFloat(quat.z);

            current.axis.oSet(quat.ToMat3());
            current.localAxis.oSet(localQuat.ToMat3());
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
}
