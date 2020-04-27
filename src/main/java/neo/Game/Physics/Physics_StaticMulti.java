package neo.Game.Physics;

import static neo.Game.GameSys.SysCvar.g_gravity;
import static neo.Game.Game_local.gameLocal;
import static neo.idlib.BV.Bounds.bounds_zero;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
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
import neo.Game.Physics.Physics_Static.staticPState_s;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Physics_StaticMulti {

    static staticPState_s defaultState;//TODO:?

    /*
     ===============================================================================

     Physics for a non moving object using no or multiple collision models.

     ===============================================================================
     */
    public static class idPhysics_StaticMulti extends idPhysics {
        // CLASS_PROTOTYPE( idPhysics_StaticMulti );

        protected idEntity               self;            // entity using this physics object
        protected idList<staticPState_s> current;    // physics state
        protected idList<idClipModel>    clipModels;    // collision model
        //
        // master
        protected boolean                hasMaster;
        protected boolean                isOrientated;
        //
        //

        public idPhysics_StaticMulti() {
            self = null;
            hasMaster = false;
            isOrientated = false;

            defaultState.origin.Zero();
            defaultState.axis.Identity();
            defaultState.localOrigin.Zero();
            defaultState.localAxis.Identity();

            current.SetNum(1);
            current.oSet(0, defaultState);
            clipModels.SetNum(1);
            clipModels.oSet(0, null);
        }

        // ~idPhysics_StaticMulti();
        @Override
        protected void _deconstructor() {
            if (self != null && self.GetPhysics() == this) {
                self.SetPhysics(null);
            }
            idForce.DeletePhysics(this);
            for (int i = 0; i < clipModels.Num(); i++) {
                idClipModel.delete(clipModels.oGet(i));
            }

            super._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteObject(self);

            savefile.WriteInt(current.Num());
            for (i = 0; i < current.Num(); i++) {
                savefile.WriteVec3(current.oGet(i).origin);
                savefile.WriteMat3(current.oGet(i).axis);
                savefile.WriteVec3(current.oGet(i).localOrigin);
                savefile.WriteMat3(current.oGet(i).localAxis);
            }

            savefile.WriteInt(clipModels.Num());
            for (i = 0; i < clipModels.Num(); i++) {
                savefile.WriteClipModel(clipModels.oGet(i));
            }

            savefile.WriteBool(hasMaster);
            savefile.WriteBool(isOrientated);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int[] num = {0};

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/self);

            savefile.ReadInt(num);
            current.AssureSize(num[0]);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadVec3(current.oGet(i).origin);
                savefile.ReadMat3(current.oGet(i).axis);
                savefile.ReadVec3(current.oGet(i).localOrigin);
                savefile.ReadMat3(current.oGet(i).localAxis);
            }

            savefile.ReadInt(num);
            clipModels.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadClipModel(clipModels.oGet(i));
            }

            hasMaster = savefile.ReadBool();
            isOrientated = savefile.ReadBool();
        }

        public void RemoveIndex(int id /*= 0*/, boolean freeClipModel /*= true*/) {
            if (id < 0 || id >= clipModels.Num()) {
                return;
            }
            if (clipModels.oGet(id) != null && freeClipModel) {
                idClipModel.delete(clipModels.oGet(id));
                clipModels.oSet(id, null);
            }
            clipModels.RemoveIndex(id);
            current.RemoveIndex(id);
        }

        public void RemoveIndex(int id /*= 0*/) {
            RemoveIndex(id, true);
        }

        // common physics interface
        @Override
        public void SetSelf(idEntity e) {
            assert (e != null);
            self = e;
        }

        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
            int i;

            assert (self != null);

            if (id >= clipModels.Num()) {
                current.AssureSize(id + 1, defaultState);
                clipModels.AssureSize(id + 1, null);
            }

            if (clipModels.oGet(id) != null && clipModels.oGet(id) != model && freeOld) {
                idClipModel.delete(clipModels.oGet(id));
            }
            clipModels.oSet(id, model);
            if (clipModels.oGet(id) != null) {
                clipModels.oGet(id).Link(gameLocal.clip, self, id, current.oGet(id).origin, current.oGet(id).axis);
            }

            for (i = clipModels.Num() - 1; i >= 1; i--) {
                if (clipModels.oGet(i) != null) {
                    break;
                }
            }
            current.SetNum(i + 1, false);
            clipModels.SetNum(i + 1, false);
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            if (id >= 0 && id < clipModels.Num() && clipModels.oGet(id) != null) {
                return clipModels.oGet(id);
            }
            return gameLocal.clip.DefaultClipModel();
        }

        @Override
        public int GetNumClipModels() {
            return clipModels.Num();
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
            int i;

            if (id >= 0 && id < clipModels.Num()) {
                if (clipModels.oGet(id) != null) {
                    clipModels.oGet(id).SetContents(contents);
                }
            } else if (id == -1) {
                for (i = 0; i < clipModels.Num(); i++) {
                    if (clipModels.oGet(i) != null) {
                        clipModels.oGet(i).SetContents(contents);
                    }
                }
            }
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            int i, contents = 0;

            if (id >= 0 && id < clipModels.Num()) {
                if (clipModels.oGet(id) != null) {
                    contents = clipModels.oGet(id).GetContents();
                }
            } else if (id == -1) {
                for (i = 0; i < clipModels.Num(); i++) {
                    if (clipModels.oGet(i) != null) {
                        contents |= clipModels.oGet(i).GetContents();
                    }
                }
            }
            return contents;
        }

        @Override
        public void SetClipMask(int mask, int id /*= -1*/) {
        }

        @Override
        public int GetClipMask(int id /*= -1*/) {
            return 0;
        }
        private static idBounds bounds;

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            int i;

            if (id >= 0 && id < clipModels.Num()) {
                if (clipModels.oGet(id) != null) {
                    return clipModels.oGet(id).GetBounds();
                }
            }
            if (id == -1) {
                bounds.Clear();
                for (i = 0; i < clipModels.Num(); i++) {
                    if (clipModels.oGet(i) != null) {
                        bounds.AddBounds(clipModels.oGet(i).GetAbsBounds());
                    }
                }
                for (i = 0; i < clipModels.Num(); i++) {
                    if (clipModels.oGet(i) != null) {
                        bounds.oMinSet(0, clipModels.oGet(i).GetOrigin());
                        bounds.oMinSet(1, clipModels.oGet(i).GetOrigin());
                        break;
                    }
                }
                return bounds;
            }
            return bounds_zero;
        }
        private static idBounds absBounds;

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {
            int i;

            if (id >= 0 && id < clipModels.Num()) {
                if (clipModels.oGet(id) != null) {
                    return clipModels.oGet(id).GetAbsBounds();
                }
            }
            if (id == -1) {
                absBounds.Clear();
                for (i = 0; i < clipModels.Num(); i++) {
                    if (clipModels.oGet(i) != null) {
                        absBounds.AddBounds(clipModels.oGet(i).GetAbsBounds());
                    }
                }
                return absBounds;
            }
            return bounds_zero;
        }

        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            int i;
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (hasMaster) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                for (i = 0; i < clipModels.Num(); i++) {
                    current.oGet(i).origin.oSet(masterOrigin.oPlus(current.oGet(i).localOrigin.oMultiply(masterAxis)));
                    if (isOrientated) {
                        current.oGet(i).axis.oSet(current.oGet(i).localAxis.oMultiply(masterAxis));
                    } else {
                        current.oGet(i).axis.oSet(current.oGet(i).localAxis);
                    }
                    if (clipModels.oGet(i) != null) {
                        clipModels.oGet(i).Link(gameLocal.clip, self, i, current.oGet(i).origin, current.oGet(i).axis);
                    }
                }

                // FIXME: return false if master did not move
                return true;
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
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (id >= 0 && id < clipModels.Num()) {
                current.oGet(id).localOrigin.oSet(newOrigin);
                if (hasMaster) {
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    current.oGet(id).origin.oSet(masterOrigin.oPlus(newOrigin.oMultiply(masterAxis)));
                } else {
                    current.oGet(id).origin.oSet(newOrigin);
                }
                if (clipModels.oGet(id) != null) {
                    clipModels.oGet(id).Link(gameLocal.clip, self, id, current.oGet(id).origin, current.oGet(id).axis);
                }
            } else if (id == -1) {
                if (hasMaster) {
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    Translate(masterOrigin.oPlus(masterAxis.oMultiply(newOrigin).oMinus(current.oGet(0).origin)));
                } else {
                    Translate(newOrigin.oMinus(current.oGet(0).origin));
                }
            }
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (id >= 0 && id < clipModels.Num()) {
                current.oGet(id).localAxis.oSet(newAxis);
                if (hasMaster && isOrientated) {
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    current.oGet(id).axis.oSet(newAxis.oMultiply(masterAxis));
                } else {
                    current.oGet(id).axis.oSet(newAxis);
                }
                if (clipModels.oGet(id) != null) {
                    clipModels.oGet(id).Link(gameLocal.clip, self, id, current.oGet(id).origin, current.oGet(id).axis);
                }
            } else if (id == -1) {
                idMat3 axis;
                idRotation rotation;

                if (hasMaster) {
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    axis = current.oGet(0).axis.Transpose().oMultiply(newAxis.oMultiply(masterAxis));
                } else {
                    axis = current.oGet(0).axis.Transpose().oMultiply(newAxis);
                }
                rotation = axis.ToRotation();
                rotation.SetOrigin(current.oGet(0).origin);

                Rotate(rotation);
            }
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {
            int i;

            if (id >= 0 && id < clipModels.Num()) {
                current.oGet(id).localOrigin.oPluSet(translation);
                current.oGet(id).origin.oPluSet(translation);

                if (clipModels.oGet(id) != null) {
                    clipModels.oGet(id).Link(gameLocal.clip, self, id, current.oGet(id).origin, current.oGet(id).axis);
                }
            } else if (id == -1) {
                for (i = 0; i < clipModels.Num(); i++) {
                    current.oGet(i).localOrigin.oPluSet(translation);
                    current.oGet(i).origin.oPluSet(translation);

                    if (clipModels.oGet(i) != null) {
                        clipModels.oGet(i).Link(gameLocal.clip, self, i, current.oGet(i).origin, current.oGet(i).axis);
                    }
                }
            }
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            int i;
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (id >= 0 && id < clipModels.Num()) {
                current.oGet(id).origin.oMulSet(rotation);
                current.oGet(id).axis.oMulSet(rotation.ToMat3());

                if (hasMaster) {
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    current.oGet(id).localAxis.oMulSet(rotation.ToMat3());
                    current.oGet(id).localOrigin.oSet((current.oGet(id).origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                } else {
                    current.oGet(id).localAxis.oSet(current.oGet(id).axis);
                    current.oGet(id).localOrigin.oSet(current.oGet(id).origin);
                }

                if (clipModels.oGet(id) != null) {
                    clipModels.oGet(id).Link(gameLocal.clip, self, id, current.oGet(id).origin, current.oGet(id).axis);
                }
            } else if (id == -1) {
                for (i = 0; i < clipModels.Num(); i++) {
                    current.oGet(i).origin.oMulSet(rotation);
                    current.oGet(i).axis.oMulSet(rotation.ToMat3());

                    if (hasMaster) {
                        self.GetMasterPosition(masterOrigin, masterAxis);
                        current.oGet(i).localAxis.oMulSet(rotation.ToMat3());
                        current.oGet(i).localOrigin.oSet((current.oGet(i).origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                    } else {
                        current.oGet(i).localAxis.oSet(current.oGet(i).axis);
                        current.oGet(i).localOrigin.oSet(current.oGet(i).origin);
                    }

                    if (clipModels.oGet(i) != null) {
                        clipModels.oGet(i).Link(gameLocal.clip, self, i, current.oGet(i).origin, current.oGet(i).axis);
                    }
                }
            }
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            if (id >= 0 && id < clipModels.Num()) {
                return new idVec3(current.oGet(id).origin);
            }
            if (clipModels.Num() != 0) {
                return new idVec3(current.oGet(0).origin);
            } else {
                return getVec3_origin();
            }
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            if (id >= 0 && id < clipModels.Num()) {
                return new idMat3(current.oGet(id).axis);
            }
            if (clipModels.Num() != 0) {
                return new idMat3(current.oGet(0).axis);
            } else {
                return getMat3_identity();
            }
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
//	memset( &results, 0, sizeof( trace_t ) );//TODO:
            gameLocal.Warning("idPhysics_StaticMulti::ClipTranslation called");
        }

        @Override
        public void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model) {
//	memset( &results, 0, sizeof( trace_t ) );//TODO:
            gameLocal.Warning("idPhysics_StaticMulti::ClipRotation called");
        }

        @Override
        public int ClipContents(final idClipModel model) {
            int i, contents;

            contents = 0;
            for (i = 0; i < clipModels.Num(); i++) {
                if (clipModels.oGet(i) != null) {
                    if (model != null) {
                        contents |= gameLocal.clip.ContentsModel(clipModels.oGet(i).GetOrigin(), clipModels.oGet(i), clipModels.oGet(i).GetAxis(), -1,
                                model.Handle(), model.GetOrigin(), model.GetAxis());
                    } else {
                        contents |= gameLocal.clip.Contents(clipModels.oGet(i).GetOrigin(), clipModels.oGet(i), clipModels.oGet(i).GetAxis(), -1, null);
                    }
                }
            }
            return contents;
        }

        @Override
        public void DisableClip() {
            int i;

            for (i = 0; i < clipModels.Num(); i++) {
                if (clipModels.oGet(i) != null) {
                    clipModels.oGet(i).Disable();
                }
            }
        }

        @Override
        public void EnableClip() {
            int i;

            for (i = 0; i < clipModels.Num(); i++) {
                if (clipModels.oGet(i) != null) {
                    clipModels.oGet(i).Enable();
                }
            }
        }

        @Override
        public void UnlinkClip() {
            int i;

            for (i = 0; i < clipModels.Num(); i++) {
                if (clipModels.oGet(i) != null) {
                    clipModels.oGet(i).Unlink();
                }
            }
        }

        @Override
        public void LinkClip() {
            int i;

            for (i = 0; i < clipModels.Num(); i++) {
                if (clipModels.oGet(i) != null) {
                    clipModels.oGet(i).Link(gameLocal.clip, self, i, current.oGet(i).origin, current.oGet(i).axis);
                }
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
            info = new contactInfo_t();
//	memset( &info, 0, sizeof( info ) );
            return info;
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
            int i;
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (!hasMaster) {
                    // transform from world space to master space
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    for (i = 0; i < clipModels.Num(); i++) {
                        current.oGet(i).localOrigin.oSet((current.oGet(i).origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                        if (orientated) {
                            current.oGet(i).localAxis.oSet(current.oGet(i).axis.oMultiply(masterAxis.Transpose()));
                        } else {
                            current.oGet(i).localAxis.oSet(current.oGet(i).axis);
                        }
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
            int i;
            idCQuat quat, localQuat;

            msg.WriteByte(current.Num());

            for (i = 0; i < current.Num(); i++) {
                quat = current.oGet(i).axis.ToCQuat();
                localQuat = current.oGet(i).localAxis.ToCQuat();

                msg.WriteFloat(current.oGet(i).origin.oGet(0));
                msg.WriteFloat(current.oGet(i).origin.oGet(1));
                msg.WriteFloat(current.oGet(i).origin.oGet(2));
                msg.WriteFloat(quat.x);
                msg.WriteFloat(quat.y);
                msg.WriteFloat(quat.z);
                msg.WriteDeltaFloat(current.oGet(i).origin.oGet(0), current.oGet(i).localOrigin.oGet(0));
                msg.WriteDeltaFloat(current.oGet(i).origin.oGet(1), current.oGet(i).localOrigin.oGet(1));
                msg.WriteDeltaFloat(current.oGet(i).origin.oGet(2), current.oGet(i).localOrigin.oGet(2));
                msg.WriteDeltaFloat(quat.x, localQuat.x);
                msg.WriteDeltaFloat(quat.y, localQuat.y);
                msg.WriteDeltaFloat(quat.z, localQuat.z);
            }
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            int i, num;
            idCQuat quat = new idCQuat(), localQuat = new idCQuat();

            num = msg.ReadByte();
            assert (num == current.Num());

            for (i = 0; i < current.Num(); i++) {
                current.oGet(i).origin.oSet(0, msg.ReadFloat());
                current.oGet(i).origin.oSet(1, msg.ReadFloat());
                current.oGet(i).origin.oSet(2, msg.ReadFloat());
                quat.x = msg.ReadFloat();
                quat.y = msg.ReadFloat();
                quat.z = msg.ReadFloat();
                current.oGet(i).localOrigin.oSet(0, msg.ReadDeltaFloat(current.oGet(i).origin.oGet(0)));
                current.oGet(i).localOrigin.oSet(1, msg.ReadDeltaFloat(current.oGet(i).origin.oGet(1)));
                current.oGet(i).localOrigin.oSet(2, msg.ReadDeltaFloat(current.oGet(i).origin.oGet(2)));
                localQuat.x = msg.ReadDeltaFloat(quat.x);
                localQuat.y = msg.ReadDeltaFloat(quat.y);
                localQuat.z = msg.ReadDeltaFloat(quat.z);

                current.oGet(i).axis.oSet(quat.ToMat3());
                current.oGet(i).localAxis.oSet(localQuat.ToMat3());
            }
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
