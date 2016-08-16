package neo.Game.Physics;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.gameLocal;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_Base.idPhysics_Base;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_zero;
import neo.idlib.math.Vector.idVec3;

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
            clipModel = null;
            clipModelAxis = new idMat3();
            SetClipModelAxis();
            mass = 100.0f;
            invMass = 1.0f / mass;
            masterEntity = null;
            masterYaw = 0.0f;
            masterDeltaYaw = 0.0f;
            groundEntityPtr = new idEntityPtr<>(null);
        }

        // ~idPhysics_Actor();
        private void _deconstructor(){
            idClipModel.delete(clipModel);
            clipModel = null;
        }

        public static void delete(idPhysics_Actor actor) {
            actor._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteClipModel(clipModel);
            savefile.WriteMat3(clipModelAxis);

            savefile.WriteFloat(mass);
            savefile.WriteFloat(invMass);

            savefile.WriteObject(masterEntity);
            savefile.WriteFloat(masterYaw);
            savefile.WriteFloat(masterDeltaYaw);

            groundEntityPtr.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            savefile.ReadClipModel(clipModel);
            savefile.ReadMat3(clipModelAxis);

            mass = savefile.ReadFloat();
            invMass = savefile.ReadFloat();

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/masterEntity);
            masterYaw = savefile.ReadFloat();
            masterDeltaYaw = savefile.ReadFloat();

            groundEntityPtr.Restore(savefile);
        }

        // get delta yaw of master
        public float GetMasterDeltaYaw() {
            return masterDeltaYaw;
        }

        // returns the ground entity
        public idEntity GetGroundEntity() {
            return groundEntityPtr.GetEntity();
        }

        // align the clip model with the gravity direction
        public void SetClipModelAxis() {
            // align clip model to gravity direction
            if ((gravityNormal.oGet(2) == -1.0f) || (gravityNormal.equals(getVec3_zero()))) {
                clipModelAxis.Identity();
            } else {
                clipModelAxis.oSet(2, gravityNormal.oNegative());
                clipModelAxis.oGet(2).NormalVectors(clipModelAxis.oGet(0), clipModelAxis.oGet(1));
                clipModelAxis.oSet(1, clipModelAxis.oGet(1).oNegative());
            }

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, clipModel.GetOrigin(), clipModelAxis);
            }
        }

        // common physics interface
        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
            assert (self != null);
            assert (model != null);           // a clip model is required
            assert (model.IsTraceModel());    // and it should be a trace model
            assert (density > 0.0f);          // density should be valid

            if (clipModel != null && clipModel != model && freeOld) {
                idClipModel.delete(clipModel);
            }
            clipModel = model;
            clipModel.Link(gameLocal.clip, self, 0, clipModel.GetOrigin(), clipModelAxis);
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
        public void SetMass(float _mass, int id /*= -1*/) {
            assert (_mass > 0.0f);
            mass = _mass;
            invMass = 1.0f / _mass;
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

        @Override
        public boolean IsPushable() {
            return (masterEntity == null);
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            return clipModel.GetOrigin();
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return clipModel.GetAxis();
        }

        @Override
        public void SetGravity(final idVec3 newGravity) {
            if (!newGravity.equals(gravityVector)) {
                super.SetGravity(newGravity);
                SetClipModelAxis();
            }
        }

        public idMat3 GetGravityAxis() {
            return clipModelAxis;
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
                return gameLocal.clip.ContentsModel(clipModel.GetOrigin(), clipModel, clipModel.GetAxis(), -1, model.Handle(), model.GetOrigin(), model.GetAxis());
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
            clipModel.Link(gameLocal.clip, self, 0, clipModel.GetOrigin(), clipModel.GetAxis());
        }

        @Override
        public boolean EvaluateContacts() {

            // get all the ground contacts
            ClearContacts();
            AddGroundContacts(clipModel);
            AddContactEntitiesForContacts();

            return (contacts.Num() != 0);
        }

    };
}
