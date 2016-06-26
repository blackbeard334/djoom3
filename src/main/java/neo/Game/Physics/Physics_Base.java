package neo.Game.Physics;

import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Physics.Clip.idClipModel;
import static neo.Game.Physics.Physics.CONTACT_EPSILON;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics.impactInfo_s;
import static neo.idlib.BV.Bounds.bounds_zero;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsgDelta;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorRed;
import neo.idlib.containers.List.idList;
import static neo.idlib.math.Math_h.Square;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec6;

/**
 *
 */
public class Physics_Base {

    /*
     ===============================================================================

     Physics base for a moving object using one or more collision models.

     ===============================================================================
     */
    public static class contactEntity_t extends idEntityPtr<idEntity> {

    };

    public static class idPhysics_Base extends idPhysics {
        // CLASS_PROTOTYPE( idPhysics_Base );

        protected idEntity                self;             // entity using this physics object
        protected int                     clipMask;         // contents the physics object collides with
        protected idVec3                  gravityVector;    // direction and magnitude of gravity
        protected idVec3                  gravityNormal;    // normalized direction of gravity
        protected idList<contactInfo_t>   contacts;         // contacts with other physics objects
        protected idList<contactEntity_t> contactEntities;  // entities touching this physics object
        //
        //

        public idPhysics_Base() {
            self = null;
            clipMask = 0;
            this.contacts = new idList<>(contactInfo_t.class);
            this.contactEntities = new idList<>(contactEntity_t.class);
//            SetGravity(gameLocal.GetGravity());
            gravityVector = new idVec3(gameLocal.GetGravity());
            gravityNormal = new idVec3(gameLocal.GetGravity());
            gravityNormal.Normalize();
            ClearContacts();
        }
        // ~idPhysics_Base( void );

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteObject(self);
            savefile.WriteInt(clipMask);
            savefile.WriteVec3(gravityVector);
            savefile.WriteVec3(gravityNormal);

            savefile.WriteInt(contacts.Num());
            for (i = 0; i < contacts.Num(); i++) {
                savefile.WriteContactInfo(contacts.oGet(i));
            }

            savefile.WriteInt(contactEntities.Num());
            for (i = 0; i < contactEntities.Num(); i++) {
                contactEntities.oGet(i).Save(savefile);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int[] num = {0};

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/self);
            clipMask = savefile.ReadInt();
            savefile.ReadVec3(gravityVector);
            savefile.ReadVec3(gravityNormal);

            savefile.ReadInt(num);
            contacts.SetNum(num[0]);
            for (i = 0; i < contacts.Num(); i++) {
                savefile.ReadContactInfo(contacts.oGet(i));
            }

            savefile.ReadInt(num);
            contactEntities.SetNum(num[0]);
            for (i = 0; i < contactEntities.Num(); i++) {
                contactEntities.oGet(i).Restore(savefile);
            }
        }

        // common physics interface
        @Override
        public void SetSelf(idEntity e) {
            assert (e != null);
            self = e;
        }

        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            return null;
        }

        @Override
        public int GetNumClipModels() {
            return 0;
        }

        @Override
        public void SetMass(float mass, int id /*= -1*/) {
        }

        @Override
        public float GetMass(int id /*= -1*/) {
            return 0;
        }

        @Override
        public void SetContents(int contents, int id /*= -1*/) {
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            return 0;
        }

        @Override
        public void SetClipMask(int mask, int id /*= -1*/) {
            clipMask = mask;
        }

        @Override
        public int GetClipMask(int id /*= -1*/) {
            return clipMask;
        }

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            return bounds_zero;
        }

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {
            return bounds_zero;
        }

        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
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
//	memset( info, 0, sizeof( *info ) );
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
            return true;
        }

        @Override
        public void SaveState() {
        }

        @Override
        public void RestoreState() {
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {
        }

        @Override
        public void Translate(final idVec3 translation) {
            Translate(translation, -1);
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
        }

        @Override
        public void Rotate(final idRotation rotation) {
            Rotate(rotation, -1);
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            return getVec3_origin();
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return getMat3_identity();
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
            gravityVector.oSet(newGravity);
            gravityNormal.oSet(newGravity);
            gravityNormal.Normalize();
        }

        @Override
        public idVec3 GetGravity() {
            return gravityVector;
        }

        @Override
        public idVec3 GetGravityNormal() {
            return gravityNormal;
        }

        @Override
        public void ClipTranslation(trace_s[] results, final idVec3 translation, final idClipModel model) {
//	memset( &results, 0, sizeof( trace_t ) );
            results[0] = new trace_s();
        }

        @Override
        public void ClipRotation(trace_s[] results, final idRotation rotation, final idClipModel model) {
//	memset( &results, 0, sizeof( trace_t ) );
            results[0] = new trace_s();
        }

        @Override
        public int ClipContents(final idClipModel model) {
            return 0;
        }

        @Override
        public void DisableClip() {
        }

        @Override
        public void EnableClip() {
        }

        @Override
        public void UnlinkClip() {
        }

        @Override
        public void LinkClip() {
        }

        @Override
        public boolean EvaluateContacts() {
            return false;
        }

        @Override
        public int GetNumContacts() {
            return contacts.Num();
        }

        @Override
        public contactInfo_t GetContact(int num) {
            return contacts.oGet(num);
        }

        @Override
        public void ClearContacts() {
            int i;
            idEntity ent;

            for (i = 0; i < contacts.Num(); i++) {
                ent = gameLocal.entities[ contacts.oGet(i).entityNum];
                if (ent != null) {
                    ent.RemoveContactEntity(self);
                }
            }
            contacts.SetNum(0, false);
        }

        @Override
        public void AddContactEntity(idEntity e) {
            int i;
            idEntity ent;
            boolean found = false;

            for (i = 0; i < contactEntities.Num(); i++) {
                ent = contactEntities.oGet(i).GetEntity();
                if (ent == null) {
                    contactEntities.RemoveIndex(i--);
                }
                if (ent == e) {
                    found = true;
                }
            }
            if (!found) {
                contactEntities.Alloc().oSet(e);
            }
        }

        @Override
        public void RemoveContactEntity(idEntity e) {
            int i;
            idEntity ent;

            for (i = 0; i < contactEntities.Num(); i++) {
                ent = contactEntities.oGet(i).GetEntity();
                if (null == ent) {
                    contactEntities.RemoveIndex(i--);
                    continue;
                }
                if (ent == e) {
                    contactEntities.RemoveIndex(i--);
                    return;
                }
            }
        }

        @Override
        public boolean HasGroundContacts() {
            int i;

            for (i = 0; i < contacts.Num(); i++) {
                if (contacts.oGet(i).normal.oMultiply(gravityNormal.oNegative()) > 0.0f) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean IsGroundEntity(int entityNum) {
            int i;

            for (i = 0; i < contacts.Num(); i++) {
                if (contacts.oGet(i).entityNum == entityNum && (contacts.oGet(i).normal.oMultiply(gravityNormal.oNegative()) > 0.0f)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean IsGroundClipModel(int entityNum, int id) {
            int i;

            for (i = 0; i < contacts.Num(); i++) {
                if (contacts.oGet(i).entityNum == entityNum && contacts.oGet(i).id == id && (contacts.oGet(i).normal.oMultiply(gravityNormal.oNegative()) > 0.0f)) {
                    return true;
                }
            }
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
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
        }

        // add ground contacts for the clip model
        protected void AddGroundContacts(final idClipModel clipModel) {
            idVec6 dir = new idVec6();
            int index, num;

            index = contacts.Num();
            contacts.SetNum(index + 10, false);

            contactInfo_t[] contactz = new contactInfo_t[10];

            dir.SubVec3_oSet(0, gravityNormal);
            dir.SubVec3_oSet(1, getVec3_origin());
            num = gameLocal.clip.Contacts(contactz, 10, clipModel.GetOrigin(), dir, CONTACT_EPSILON, clipModel, clipModel.GetAxis(), clipMask, self);
            for (int i = 0; i < num; i++) {
                contacts.oSet(index + i, contactz[i]);
            }
            contacts.SetNum(index + num, false);
        }

        // add contact entity links to contact entities
        protected void AddContactEntitiesForContacts() {
            int i;
            idEntity ent;

            for (i = 0; i < contacts.Num(); i++) {
                ent = gameLocal.entities[ contacts.oGet(i).entityNum];
                if (ent != null && !ent.equals(self)) {
                    ent.AddContactEntity(self);
                }
            }
        }

        // active all contact entities
        protected void ActivateContactEntities() {
            int i;
            idEntity ent;

            for (i = 0; i < contactEntities.Num(); i++) {
                ent = contactEntities.oGet(i).GetEntity();
                if (ent != null) {
                    ent.ActivatePhysics(self);
                } else {
                    contactEntities.RemoveIndex(i--);
                }
            }
        }

        private static int DBG_IsOutsideWorld = 0;
        // returns true if the whole physics object is outside the world bounds
        protected boolean IsOutsideWorld() {       DBG_IsOutsideWorld++;
            if (!gameLocal.clip.GetWorldBounds().Expand(128.0f).IntersectsBounds(GetAbsBounds())) {
                return true;
            }
            return false;
        }

        // draw linear and angular velocity
        protected void DrawVelocity(int id, float linearScale, float angularScale) {
            idVec3 dir, org = new idVec3(), vec, start, end;
            idMat3 axis;
            float length, a;

            dir = GetLinearVelocity(id);
            dir.oMulSet(linearScale);
            if (dir.LengthSqr() > Square(0.1f)) {
                dir.Truncate(10.0f);
                org = GetOrigin(id);
                gameRenderWorld.DebugArrow(colorRed, org, org.oPlus(dir), 1);
            }

            dir = GetAngularVelocity(id);
            length = dir.Normalize();
            length *= angularScale;
            if (length > 0.1f) {
                if (length < 60.0f) {
                    length = 60.0f;
                } else if (length > 360.0f) {
                    length = 360.0f;
                }
                axis = GetAxis(id);
                vec = axis.oGet(2);
                if (idMath.Fabs(dir.oMultiply(vec)) > 0.99f) {
                    vec = axis.oGet(0);
                }
                vec.oMinSet(vec.oMultiply(dir.oMultiply(vec)));
                vec.Normalize();
                vec.oMulSet(4.0f);
                start = org.oPlus(vec);
                for (a = 20.0f; a < length; a += 20.0f) {
                    end = org.oPlus(new idRotation(getVec3_origin(), dir, -a).ToMat3().oMultiply(vec));
                    gameRenderWorld.DebugLine(colorBlue, start, end, 1);
                    start = end;
                }
                end = org.oPlus(new idRotation(getVec3_origin(), dir, -length).ToMat3().oMultiply(vec));
                gameRenderWorld.DebugArrow(colorBlue, start, end, 1);
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
