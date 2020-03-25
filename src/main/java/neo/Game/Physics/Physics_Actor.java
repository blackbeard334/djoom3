package neo.Game.Physics;

import static neo.Game.Game_local.gameLocal;
import static neo.idlib.math.Vector.getVec3_zero;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_Base.idPhysics_Base;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Physics_Actor {

    /*
     ===================================================================================

     Actor physics base class

     An actor typically uses one collision model which is aligned with the gravity
     direction. The collision model is usually a simple box with the origin at the
     bottom center.

     ===================================================================================
     */
    public static class idPhysics_Actor extends idPhysics_Base {
        // CLASS_PROTOTYPE( idPhysics_Actor );

        protected idClipModel           clipModel;    // clip model used for collision detection
        protected idMat3                clipModelAxis;// axis of clip model aligned with gravity direction
        //
        // derived properties
        protected float                 mass;
        protected float                 invMass;
        //
        // master
        protected idEntity              masterEntity;
        protected float                 masterYaw;
        protected float                 masterDeltaYaw;
        //
        // results of last evaluate
        protected idEntityPtr<idEntity> groundEntityPtr;
        //
        //

        public idPhysics_Actor() {
            this.clipModel = null;
            this.clipModelAxis = new idMat3();
            SetClipModelAxis();
            this.mass = 100.0f;
            this.invMass = 1.0f / this.mass;
            this.masterEntity = null;
            this.masterYaw = 0.0f;
            this.masterDeltaYaw = 0.0f;
            this.groundEntityPtr = new idEntityPtr<>(null);
        }

        // ~idPhysics_Actor();
        @Override
        protected void _deconstructor(){
            idClipModel.delete(this.clipModel);
            this.clipModel = null;

            super._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteClipModel(this.clipModel);
            savefile.WriteMat3(this.clipModelAxis);

            savefile.WriteFloat(this.mass);
            savefile.WriteFloat(this.invMass);

            savefile.WriteObject(this.masterEntity);
            savefile.WriteFloat(this.masterYaw);
            savefile.WriteFloat(this.masterDeltaYaw);

            this.groundEntityPtr.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            savefile.ReadClipModel(this.clipModel);
            savefile.ReadMat3(this.clipModelAxis);

            this.mass = savefile.ReadFloat();
            this.invMass = savefile.ReadFloat();

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/masterEntity);
            this.masterYaw = savefile.ReadFloat();
            this.masterDeltaYaw = savefile.ReadFloat();

            this.groundEntityPtr.Restore(savefile);
        }

        // get delta yaw of master
        public float GetMasterDeltaYaw() {
            return this.masterDeltaYaw;
        }

        // returns the ground entity
        public idEntity GetGroundEntity() {
            return this.groundEntityPtr.GetEntity();
        }

        // align the clip model with the gravity direction
        public void SetClipModelAxis() {
            // align clip model to gravity direction
            if ((this.gravityNormal.oGet(2) == -1.0f) || (this.gravityNormal.equals(getVec3_zero()))) {
                this.clipModelAxis.Identity();
            } else {
                this.clipModelAxis.oSet(2, this.gravityNormal.oNegative());
                this.clipModelAxis.oGet(2).NormalVectors(this.clipModelAxis.oGet(0), this.clipModelAxis.oGet(1));
                this.clipModelAxis.oSet(1, this.clipModelAxis.oGet(1).oNegative());
            }

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.clipModel.GetOrigin(), this.clipModelAxis);
            }
        }

        // common physics interface
        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
            assert (this.self != null);
            assert (model != null);           // a clip model is required
            assert (model.IsTraceModel());    // and it should be a trace model
            assert (density > 0.0f);          // density should be valid

            if ((this.clipModel != null) && (this.clipModel != model) && freeOld) {
                idClipModel.delete(this.clipModel);
            }
            this.clipModel = model;
            this.clipModel.Link(gameLocal.clip, this.self, 0, this.clipModel.GetOrigin(), this.clipModelAxis);
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
        public void SetMass(float _mass, int id /*= -1*/) {
            assert (_mass > 0.0f);
            this.mass = _mass;
            this.invMass = 1.0f / _mass;
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

        @Override
        public boolean IsPushable() {
            return (this.masterEntity == null);
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            return this.clipModel.GetOrigin();
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return this.clipModel.GetAxis();
        }

        @Override
        public void SetGravity(final idVec3 newGravity) {
            if (!newGravity.equals(this.gravityVector)) {
                super.SetGravity(newGravity);
                SetClipModelAxis();
            }
        }

        public idMat3 GetGravityAxis() {
            return this.clipModelAxis;
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
                return gameLocal.clip.ContentsModel(this.clipModel.GetOrigin(), this.clipModel, this.clipModel.GetAxis(), -1, model.Handle(), model.GetOrigin(), model.GetAxis());
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
            this.clipModel.Link(gameLocal.clip, this.self, 0, this.clipModel.GetOrigin(), this.clipModel.GetAxis());
        }

        @Override
        public boolean EvaluateContacts() {

            // get all the ground contacts
            ClearContacts();
            AddGroundContacts(this.clipModel);
            AddContactEntitiesForContacts();

            return (this.contacts.Num() != 0);
        }

    }
}
