package neo.Game;

import java.util.Arrays;
import neo.CM.CollisionModel.trace_s;
import neo.Game.AF.idAF;
import neo.Game.AFEntity.jointTransformData_t;
import static neo.Game.Animation.Anim.ANIMCHANNEL_ALL;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD_OVERRIDE;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import static neo.Game.Entity.EV_SetAngularVelocity;
import static neo.Game.Entity.EV_SetLinearVelocity;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEPARTICLES;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import neo.Game.Entity.idAnimatedEntity;
import neo.Game.Entity.idEntity;
import static neo.Game.GameSys.Class.EV_Remove;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.g_bloodEffects;
import static neo.Game.GameSys.SysCvar.g_vehicleForce;
import static neo.Game.GameSys.SysCvar.g_vehicleSuspensionDamping;
import static neo.Game.GameSys.SysCvar.g_vehicleSuspensionDown;
import static neo.Game.GameSys.SysCvar.g_vehicleSuspensionKCompress;
import static neo.Game.GameSys.SysCvar.g_vehicleSuspensionUp;
import static neo.Game.GameSys.SysCvar.g_vehicleTireFriction;
import static neo.Game.GameSys.SysCvar.g_vehicleVelocity;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Item.idMoveableItem;
import static neo.Game.Physics.Clip.JOINT_HANDLE_TO_CLIPMODEL_ID;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force_Constant.idForce_Constant;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_AF.idAFBody;
import neo.Game.Physics.Physics_AF.idAFConstraint_BallAndSocketJoint;
import neo.Game.Physics.Physics_AF.idAFConstraint_Contact;
import neo.Game.Physics.Physics_AF.idAFConstraint_Hinge;
import neo.Game.Physics.Physics_AF.idAFConstraint_Suspension;
import neo.Game.Physics.Physics_AF.idAFConstraint_UniversalJoint;
import neo.Game.Physics.Physics_AF.idPhysics_AF;
import neo.Game.Player.idPlayer;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Model.INVALID_JOINT;
import neo.Renderer.Model.idMD5Joint;
import neo.Renderer.Model.idRenderModel;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIME_OF_DEATH;
import neo.Renderer.RenderWorld.renderEntity_s;
import static neo.TempDump.NOT;
import static neo.framework.Common.EDITOR_AF;
import neo.framework.DeclAF.getJointTransform_t;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclSkin.idDeclSkin;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.TraceModel.idTraceModel;
import static neo.idlib.math.Math_h.MS2SEC;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.RAD2DEG;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class AFEntity {

    public static final idEventDef EV_SetConstraintPosition = new idEventDef("SetConstraintPosition", "sv");
    //
    public static final idEventDef EV_Gib = new idEventDef("gib", "s");
    public static final idEventDef EV_Gibbed = new idEventDef("<gibbed>");
//    
    public static final idEventDef EV_SetFingerAngle = new idEventDef("setFingerAngle", "f");
    public static final idEventDef EV_StopFingers = new idEventDef("stopFingers");

    /*
     ===============================================================================

     idMultiModelAF

     Entity using multiple separate visual models animated with a single
     articulated figure. Only used for debugging!

     ===============================================================================
     */
    public static final int GIB_DELAY = 200;  // only gib this often to keep performace hits when blowing up several mobs

    public static class idMultiModelAF extends idEntity {
//        public CLASS_PROTOTYPE(idMultiModelAF );//TODO:include this?

        protected idPhysics_AF physicsObj;
        //
        private idList<idRenderModel> modelHandles;
        private idList<Integer> modelDefHandles;
        //
        //

        @Override
        public void Spawn() {
            physicsObj.SetSelf(this);
        }

//							~idMultiModelAF( void );
        @Override
        public void Think() {
            RunPhysics();
            Present();
        }

        @Override
        public void Present() {
            int i;

            // don't present to the renderer if the entity hasn't changed
            if (0 == (thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }
            BecomeInactive(TH_UPDATEVISUALS);

            for (i = 0; i < modelHandles.Num(); i++) {

                if (null == modelHandles.oGet(i)) {
                    continue;
                }

                renderEntity.origin = new idVec3(physicsObj.GetOrigin(i));
                renderEntity.axis = new idMat3(physicsObj.GetAxis(i));
                renderEntity.hModel = modelHandles.oGet(i);
                renderEntity.bodyId = i;

                // add to refresh list
                if (modelDefHandles.oGet(i) == -1) {
                    modelDefHandles.oSet(i, gameRenderWorld.AddEntityDef(renderEntity));
                } else {
                    gameRenderWorld.UpdateEntityDef(modelDefHandles.oGet(i), renderEntity);
                }
            }
        }

        protected void SetModelForId(int id, final String modelName) {
            modelHandles.AssureSize(id + 1, null);
            modelDefHandles.AssureSize(id + 1, -1);
            modelHandles.oSet(id, renderModelManager.FindModel(modelName));
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    /*
     ===============================================================================

     idChain

     Chain hanging down from the ceiling. Only used for debugging!

     ===============================================================================
     */
    public static class idChain extends idMultiModelAF {
//public	CLASS_PROTOTYPE( idChain );

        @Override
        public void Spawn() {
            int[] numLinks = new int[1];
            float[] length = new float[1], linkWidth = new float[1], density = new float[1];
            float linkLength;
            boolean[] drop = {false};
            idVec3 origin;

            spawnArgs.GetBool("drop", "0", drop);
            spawnArgs.GetInt("links", "3", numLinks);
            spawnArgs.GetFloat("length", "" + (numLinks[0] * 32.0f), length);
            spawnArgs.GetFloat("width", "8", linkWidth);
            spawnArgs.GetFloat("density", "0.2", density);
            linkLength = length[0] / numLinks[0];
            origin = GetPhysics().GetOrigin();

            // initialize physics
            physicsObj.SetSelf(this);
            physicsObj.SetGravity(gameLocal.GetGravity());
            physicsObj.SetClipMask(MASK_SOLID | CONTENTS_BODY);
            SetPhysics(physicsObj);

            BuildChain("link", origin, linkLength, linkWidth[0], density[0], numLinks[0], !drop[0]);
        }

        /*
         ================
         idChain::BuildChain

         builds a chain hanging down from the ceiling
         the highest link is a child of the link below it etc.
         this allows an object to be attached to multiple chains while keeping a single tree structure
         ================
         */
        protected void BuildChain(final String name, final idVec3 origin, float linkLength, float linkWidth, float density, int numLinks, boolean bindToWorld /*= true*/) {
            int i;
            float halfLinkLength = linkLength * 0.5f;
            idTraceModel trm;
            idClipModel clip;
            idAFBody body, lastBody;
            idAFConstraint_BallAndSocketJoint bsj;
            idAFConstraint_UniversalJoint uj;
            idVec3 org;

            // create a trace model
            trm = new idTraceModel(linkLength, linkWidth);
            trm.Translate(trm.offset.oNegative());

            org = origin.oMinus(new idVec3(0, 0, halfLinkLength));

            lastBody = null;
            for (i = 0; i < numLinks; i++) {

                // add body
                clip = new idClipModel(trm);
                clip.SetContents(CONTENTS_SOLID);
                clip.Link(gameLocal.clip, this, 0, org, getMat3_identity());
                body = new idAFBody(new idStr(name + i), clip, density);
                physicsObj.AddBody(body);

                // visual model for body
                SetModelForId(physicsObj.GetBodyId(body), spawnArgs.GetString("model"));

                // add constraint
                if (bindToWorld) {
                    if (NOT(lastBody)) {
                        uj = new idAFConstraint_UniversalJoint(new idStr(name + i), body, lastBody);
                        uj.SetShafts(new idVec3(0, 0, -1), new idVec3(0, 0, 1));
                        //uj.SetConeLimit( idVec3( 0, 0, -1 ), 30 );
                        //uj.SetPyramidLimit( idVec3( 0, 0, -1 ), idVec3( 1, 0, 0 ), 90, 30 );
                    } else {
                        uj = new idAFConstraint_UniversalJoint(new idStr(name + i), lastBody, body);
                        uj.SetShafts(new idVec3(0, 0, 1), new idVec3(0, 0, -1));
                        //uj.SetConeLimit( idVec3( 0, 0, 1 ), 30 );
                    }
                    uj.SetAnchor(org.oPlus(new idVec3(0, 0, halfLinkLength)));
                    uj.SetFriction(0.9f);
                    physicsObj.AddConstraint(uj);
                } else {
                    if (lastBody != null) {
                        bsj = new idAFConstraint_BallAndSocketJoint(new idStr("joint" + i), lastBody, body);
                        bsj.SetAnchor(org.oPlus(new idVec3(0, 0, halfLinkLength)));
                        bsj.SetConeLimit(new idVec3(0, 0, 1), 60, new idVec3(0, 0, 1));
                        physicsObj.AddConstraint(bsj);
                    }
                }

                org.oMinSet(2, linkLength);

                lastBody = body;
            }
        }
    };

    /*
     ===============================================================================

     idAFAttachment

     ===============================================================================
     */
    public static class idAFAttachment extends idAnimatedEntity {
// public	CLASS_PROTOTYPE( idAFAttachment );

        protected idEntity body;
        protected idClipModel combatModel;	// render model for hit detection of head
        protected int idleAnim;
        protected int/*jointHandle_t*/ attachJoint;
        //
        //

        public idAFAttachment() {
            body = null;
            combatModel = null;
            idleAnim = 0;
            attachJoint = INVALID_JOINT;
        }
        // virtual					~idAFAttachment( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            idleAnim = animator.GetAnim("idle");
        }

        /*
         ================
         idAFAttachment::Save

         archive object for savegame file
         ================
         */
        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteObject(body);
            savefile.WriteInt(idleAnim);
            savefile.WriteJoint(attachJoint);
        }

        /*
         ================
         idAFAttachment::Restore

         unarchives object from save game file
         ================
         */
        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadObject(/*reinterpret_cast<idClass*&>*/body);
            idleAnim = savefile.ReadInt();
            attachJoint = savefile.ReadJoint();

            SetCombatModel();
            LinkCombat();
        }

        public void SetBody(idEntity bodyEnt, final String headModel, int/*jointHandle_t*/ attachJoint) {
            boolean bleed;

            body = bodyEnt;
            this.attachJoint = attachJoint;
            SetModel(headModel);
            fl.takedamage = true;

            bleed = body.spawnArgs.GetBool("bleed");
            spawnArgs.SetBool("bleed", bleed);
        }

        public void ClearBody() {
            body = null;
            attachJoint = INVALID_JOINT;
            Hide();
        }

        public idEntity GetBody() {
            return body;
        }

        @Override
        public void Think() {
            super.Think();
            if ((thinkFlags & TH_UPDATEPARTICLES) != 0) {
                UpdateDamageEffects();
            }
        }

        @Override
        public void Hide() {
            idEntity_Hide();
            UnlinkCombat();
        }

        @Override
        public void Show() {
            idEntity_Show();
            LinkCombat();
        }

        public void PlayIdleAnim(int blendTime) {
            if (idleAnim != 0 && (idleAnim != animator.CurrentAnim(ANIMCHANNEL_ALL).AnimNum())) {
                animator.CycleAnim(ANIMCHANNEL_ALL, idleAnim, gameLocal.time, blendTime);
            }
        }

        @Override
        public void GetImpactInfo(idEntity ent, int id, final idVec3 point, impactInfo_s info) {
            if (body != null) {
                body.GetImpactInfo(ent, JOINT_HANDLE_TO_CLIPMODEL_ID(attachJoint), point, info);
            } else {
                idEntity_GetImpactInfo(ent, id, point, info);
            }
        }

        @Override
        public void ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {
            if (body != null) {
                body.ApplyImpulse(ent, JOINT_HANDLE_TO_CLIPMODEL_ID(attachJoint), point, impulse);
            } else {
                idEntity_ApplyImpulse(ent, id, point, impulse);
            }
        }

        @Override
        public void AddForce(idEntity ent, int id, final idVec3 point, final idVec3 force) {
            if (body != null) {
                body.AddForce(ent, JOINT_HANDLE_TO_CLIPMODEL_ID(attachJoint), point, force);
            } else {
                idEntity_AddForce(ent, id, point, force);
            }
        }

        /*
         ============
         idAFAttachment::Damage

         Pass damage to body at the bindjoint
         ============
         */
        @Override
        public void Damage(idEntity inflictor, idEntity attacker, final idVec3 dir, final String damageDefName, final float damageScale, final int location) {

            if (body != null) {
                body.Damage(inflictor, attacker, dir, damageDefName, damageScale, attachJoint);
            }
        }

        @Override
        public void AddDamageEffect(final trace_s collision, final idVec3 velocity, final String damageDefName) {
            if (body != null) {
                trace_s c = collision;
                c.c.id = JOINT_HANDLE_TO_CLIPMODEL_ID(attachJoint);
                body.AddDamageEffect(c, velocity, damageDefName);
            }
        }

        public void SetCombatModel() {
            if (combatModel != null) {
                combatModel.Unlink();
                combatModel.LoadModel(modelDefHandle);
            } else {
                combatModel = new idClipModel(modelDefHandle);
            }
            combatModel.SetOwner(body);
        }

        public idClipModel GetCombatModel() {
            return combatModel;
        }

        public void LinkCombat() {
            if (fl.hidden) {
                return;
            }

            if (combatModel != null) {
                combatModel.Link(gameLocal.clip, this, 0, renderEntity.origin, renderEntity.axis, modelDefHandle);
            }
        }

        public void UnlinkCombat() {
            if (combatModel != null) {
                combatModel.Unlink();
            }
        }
    };

    /*
     ===============================================================================

     idAFEntity_Base

     ===============================================================================
     */
    public static final float BOUNCE_SOUND_MIN_VELOCITY = 80;
    public static final float BOUNCE_SOUND_MAX_VELOCITY = 200;
//
//

    public static class idAFEntity_Base extends idAnimatedEntity {
// public	CLASS_PROTOTYPE( idAFEntity_Base );

        protected idAF        af;                   // articulated figure
        protected idClipModel combatModel;          // render model for hit detection
        protected int         combatModelContents;
        protected idVec3      spawnOrigin;          // spawn origin
        protected idMat3      spawnAxis;            // rotation axis used when spawned
        protected int         nextSoundTime;        // next time this can make a sound
        //
        //

        public idAFEntity_Base() {
            af = new idAF();
            combatModel = null;
            combatModelContents = 0;
            nextSoundTime = 0;
            spawnOrigin = new idVec3();
            spawnAxis = getMat3_identity();
        }
        // virtual					~idAFEntity_Base( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            spawnOrigin.oSet(GetPhysics().GetOrigin());
            spawnAxis.oSet(GetPhysics().GetAxis());
            nextSoundTime = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(combatModelContents);
            savefile.WriteClipModel(combatModel);
            savefile.WriteVec3(spawnOrigin);
            savefile.WriteMat3(spawnAxis);
            savefile.WriteInt(nextSoundTime);
            af.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            combatModelContents = savefile.ReadInt();
            savefile.ReadClipModel(combatModel);
            savefile.ReadVec3(spawnOrigin);
            savefile.ReadMat3(spawnAxis);
            nextSoundTime = savefile.ReadInt();
            LinkCombat();

            af.Restore(savefile);
        }

        @Override
        public void Think() {
            RunPhysics();
            UpdateAnimation();
            if ((thinkFlags & TH_UPDATEVISUALS) != 0) {
                Present();
                LinkCombat();
            }
        }

        @Override
        public void GetImpactInfo(idEntity ent, int id, final idVec3 point, impactInfo_s info) {
            if (af.IsActive()) {
                af.GetImpactInfo(ent, id, point, info);
            } else {
                idEntity_GetImpactInfo(ent, id, point, info);
            }
        }

        @Override
        public void ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {
            if (af.IsLoaded()) {
                af.ApplyImpulse(ent, id, point, impulse);
            }
            if (!af.IsActive()) {
                idEntity_ApplyImpulse(ent, id, point, impulse);
            }
        }

        @Override
        public void AddForce(idEntity ent, int id, final idVec3 point, final idVec3 force) {
            if (af.IsLoaded()) {
                af.AddForce(ent, id, point, force);
            }
            if (!af.IsActive()) {
                idEntity_AddForce(ent, id, point, force);
            }
        }

        @Override
        public boolean Collide(final trace_s collision, final idVec3 velocity) {
            float v, f;

            if (af.IsActive()) {
                v = -(velocity.oMultiply(collision.c.normal));
                if (v > BOUNCE_SOUND_MIN_VELOCITY && gameLocal.time > nextSoundTime) {
                    f = v > BOUNCE_SOUND_MAX_VELOCITY ? 1.0f : idMath.Sqrt(v - BOUNCE_SOUND_MIN_VELOCITY) * (1.0f / idMath.Sqrt(BOUNCE_SOUND_MAX_VELOCITY - BOUNCE_SOUND_MIN_VELOCITY));
                    if (StartSound("snd_bounce", SND_CHANNEL_ANY, 0, false, null)) {
                        // don't set the volume unless there is a bounce sound as it overrides the entire channel
                        // which causes footsteps on ai's to not honor their shader parms
                        SetSoundVolume(f);
                    }
                    nextSoundTime = gameLocal.time + 500;
                }
            }

            return false;
        }

        @Override
        public boolean GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            if (af.IsActive()) {
                af.GetPhysicsToVisualTransform(origin, axis);
                return true;
            }
            return idEntity_GetPhysicsToVisualTransform(origin, axis);
        }

        @Override
        public boolean UpdateAnimationControllers() {
            if (af.IsActive()) {
                if (af.UpdateAnimation()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void FreeModelDef() {
            UnlinkCombat();
            idEntity_FreeModelDef();
        }

        public boolean LoadAF() {
            String[] fileName = new String[1];

            if (!spawnArgs.GetString("articulatedFigure", "*unknown*", fileName)) {
                return false;
            }

            af.SetAnimator(GetAnimator());
            if (!af.Load(this, fileName[0])) {
                gameLocal.Error("idAFEntity_Base::LoadAF: Couldn't load af file '%s' on entity '%s'", fileName[0], name);
            }

            af.Start();

            af.GetPhysics().Rotate(spawnAxis.ToRotation());
            af.GetPhysics().Translate(spawnOrigin);

            LoadState(spawnArgs);

            af.UpdateAnimation();
            animator.CreateFrame(gameLocal.time, true);
            UpdateVisuals();

            return true;
        }

        public boolean IsActiveAF() {
            return af.IsActive();
        }

        public String GetAFName() {
            return af.GetName();
        }

        public idPhysics_AF GetAFPhysics() {
            return af.GetPhysics();
        }

        public void SetCombatModel() {
            if (combatModel != null) {
                combatModel.Unlink();
                combatModel.LoadModel(modelDefHandle);
            } else {
                combatModel = new idClipModel(modelDefHandle);
            }
        }

        public idClipModel GetCombatModel() {
            return combatModel;
        }

        // contents of combatModel can be set to 0 or re-enabled (mp)
        public void SetCombatContents(boolean enable) {
            assert (combatModel != null);
            if (enable && combatModelContents != 0) {
                assert (0 == combatModel.GetContents());
                combatModel.SetContents(combatModelContents);
                combatModelContents = 0;
            } else if (!enable && combatModel.GetContents() != 0) {
                assert (0 == combatModelContents);
                combatModelContents = combatModel.GetContents();
                combatModel.SetContents(0);
            }
        }

        public void LinkCombat() {
            if (fl.hidden) {
                return;
            }
            if (combatModel != null) {
                combatModel.Link(gameLocal.clip, this, 0, renderEntity.origin, renderEntity.axis, modelDefHandle);
            }
        }

        public void UnlinkCombat() {
            if (combatModel != null) {
                combatModel.Unlink();
            }
        }

        public int BodyForClipModelId(int id) {
            return af.BodyForClipModelId(id);
        }

        public void SaveState(idDict args) {
            idKeyValue kv;

            // save the ragdoll pose
            af.SaveState(args);

            // save all the bind constraints
            kv = spawnArgs.MatchPrefix("bindConstraint ", null);
            while (kv != null) {
                args.Set(kv.GetKey(), kv.GetValue());
                kv = spawnArgs.MatchPrefix("bindConstraint ", kv);
            }

            // save the bind if it exists
            kv = spawnArgs.FindKey("bind");
            if (kv != null) {
                args.Set(kv.GetKey(), kv.GetValue());
            }
            kv = spawnArgs.FindKey("bindToJoint");
            if (kv != null) {
                args.Set(kv.GetKey(), kv.GetValue());
            }
            kv = spawnArgs.FindKey("bindToBody");
            if (kv != null) {
                args.Set(kv.GetKey(), kv.GetValue());
            }
        }

        public void LoadState(final idDict args) {
            af.LoadState(args);
        }

        public void AddBindConstraints() {
            af.AddBindConstraints();
        }

        public void RemoveBindConstraints() {
            af.RemoveBindConstraints();
        }

        @Override
        public void ShowEditingDialog() {
            common.InitTool(EDITOR_AF, spawnArgs);
        }

        public static void DropAFs(idEntity ent, final String type, idList<idEntity> list) {
            idKeyValue kv;
            String skinName;
            idEntity[] newEnt = {null};
            idAFEntity_Base af;
            idDict args = new idDict();
            idDeclSkin skin;

            // drop the articulated figures
            kv = ent.spawnArgs.MatchPrefix(va("def_drop%sAF", type), null);
            while (kv != null) {

                args.Set("classname", kv.GetValue());
                gameLocal.SpawnEntityDef(args, newEnt);

                if (newEnt[0] != null && newEnt[0].IsType(idAFEntity_Base.class)) {
                    af = (idAFEntity_Base) newEnt[0];
                    af.GetPhysics().SetOrigin(ent.GetPhysics().GetOrigin());
                    af.GetPhysics().SetAxis(ent.GetPhysics().GetAxis());
                    af.af.SetupPose(ent, gameLocal.time);
                    if (list != null) {
                        list.Append(af);
                    }
                }

                kv = ent.spawnArgs.MatchPrefix(va("def_drop%sAF", type), kv);
            }

            // change the skin to hide all the dropped articulated figures
            skinName = ent.spawnArgs.GetString(va("skin_drop%s", type));
            if (!skinName.isEmpty()) {
                skin = declManager.FindSkin(skinName);
                ent.SetSkin(skin);
            }
        }

        protected void Event_SetConstraintPosition(final String name, final idVec3 pos) {
            af.SetConstraintPosition(name, pos);
        }
    };
    /*
     ===============================================================================

     idAFEntity_Gibbable

     ===============================================================================
     */

    public static class idAFEntity_Gibbable extends idAFEntity_Base {
        // CLASS_PROTOTYPE( idAFEntity_Gibbable );

        protected idRenderModel skeletonModel;
        protected int           skeletonModelDefHandle;
        protected boolean       gibbed;
        //
        //

        public idAFEntity_Gibbable() {
            skeletonModel = null;
            skeletonModelDefHandle = -1;
            gibbed = false;
        }
        // ~idAFEntity_Gibbable( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            InitSkeletonModel();

            gibbed = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(gibbed);
            savefile.WriteBool(combatModel != null);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            boolean[] hasCombatModel = {false};
            boolean[] gibbed = {false};

            savefile.ReadBool(gibbed);
            savefile.ReadBool(hasCombatModel);

            this.gibbed = gibbed[0];
            InitSkeletonModel();

            if (hasCombatModel[0]) {
                SetCombatModel();
                LinkCombat();
            }
        }

        @Override
        public void Present() {
            renderEntity_s skeleton;

            if (!gameLocal.isNewFrame) {
                return;
            }

            // don't present to the renderer if the entity hasn't changed
            if (0 == (thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }

            // update skeleton model
            if (gibbed && !IsHidden() && skeletonModel != null) {
                skeleton = renderEntity;
                skeleton.hModel = skeletonModel;
                // add to refresh list
                if (skeletonModelDefHandle == -1) {
                    skeletonModelDefHandle = gameRenderWorld.AddEntityDef(skeleton);
                } else {
                    gameRenderWorld.UpdateEntityDef(skeletonModelDefHandle, skeleton);
                }
            }

            idEntity_Present();
        }

        @Override
        public void Damage(idEntity inflictor, idEntity attacker, final idVec3 dir, final String damageDefName, final float damageScale, final int location) {
            if (!fl.takedamage) {
                return;
            }
            super.Damage(inflictor, attacker, dir, damageDefName, damageScale, location);
            if (health < -20 && spawnArgs.GetBool("gib")) {
                Gib(dir, damageDefName);
            }
        }

        public void SpawnGibs(idVec3 dir, final String damageDefName) {
            int i;
            boolean gibNonSolid;
            idVec3 entityCenter, velocity;
            idList<idEntity> list = new idList<>();

            assert (!gameLocal.isClient);

            idDict damageDef = gameLocal.FindEntityDefDict(damageDefName);
            if (null == damageDef) {
                gameLocal.Error("Unknown damageDef '%s'", damageDefName);
            }

            // spawn gib articulated figures
            idAFEntity_Base.DropAFs(this, "gib", list);

            // spawn gib items
            idMoveableItem.DropItems(this, "gib", list);

            // blow out the gibs in the given direction away from the center of the entity
            entityCenter = GetPhysics().GetAbsBounds().GetCenter();
            gibNonSolid = damageDef.GetBool("gibNonSolid");
            for (i = 0; i < list.Num(); i++) {
                if (gibNonSolid) {
                    list.oGet(i).GetPhysics().SetContents(0);
                    list.oGet(i).GetPhysics().SetClipMask(0);
                    list.oGet(i).GetPhysics().UnlinkClip();
                    list.oGet(i).GetPhysics().PutToRest();
                } else {
                    list.oGet(i).GetPhysics().SetContents(CONTENTS_CORPSE);
                    list.oGet(i).GetPhysics().SetClipMask(CONTENTS_SOLID);
                    velocity = list.oGet(i).GetPhysics().GetAbsBounds().GetCenter().oMinus(entityCenter);
                    velocity.NormalizeFast();
                    velocity.oPluSet((i & 1) == 1 ? dir : dir.oNegative());
                    list.oGet(i).GetPhysics().SetLinearVelocity(velocity.oMultiply(75f));
                }
                list.oGet(i).GetRenderEntity().noShadow = true;
                list.oGet(i).GetRenderEntity().shaderParms[SHADERPARM_TIME_OF_DEATH] = gameLocal.time * 0.001f;
                list.oGet(i).PostEventSec(EV_Remove, 4.0f);
            }
        }

        protected void Gib(final idVec3 dir, final String damageDefName) {
            // only gib once
            if (gibbed) {
                return;
            }

            final idDict damageDef = gameLocal.FindEntityDefDict(damageDefName);
            if (null == damageDef) {
                gameLocal.Error("Unknown damageDef '%s'", damageDefName);
            }

            if (damageDef.GetBool("gibNonSolid")) {
                GetAFPhysics().SetContents(0);
                GetAFPhysics().SetClipMask(0);
                GetAFPhysics().UnlinkClip();
                GetAFPhysics().PutToRest();
            } else {
                GetAFPhysics().SetContents(CONTENTS_CORPSE);
                GetAFPhysics().SetClipMask(CONTENTS_SOLID);
            }

            UnlinkCombat();

            if (g_bloodEffects.GetBool()) {
                if (gameLocal.time > gameLocal.GetGibTime()) {
                    gameLocal.SetGibTime(gameLocal.time + GIB_DELAY);
                    SpawnGibs(dir, damageDefName);
                    renderEntity.noShadow = true;
                    renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = gameLocal.time * 0.001f;
                    StartSound("snd_gibbed", SND_CHANNEL_ANY, 0, false, null);
                    gibbed = true;
                }
            } else {
                gibbed = true;
            }

            PostEventSec(EV_Gibbed, 4.0f);
        }

        protected void InitSkeletonModel() {
            String modelName;
            idDeclModelDef modelDef;

            skeletonModel = null;
            skeletonModelDefHandle = -1;

            modelName = spawnArgs.GetString("model_gib");

            if (!modelName.isEmpty()) {//[0] != '\0' ) {
                modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, modelName, false);
                if (modelDef != null) {
                    skeletonModel = modelDef.ModelHandle();
                } else {
                    skeletonModel = renderModelManager.FindModel(modelName);
                }
                if (skeletonModel != null && renderEntity.hModel != null) {
                    if (skeletonModel.NumJoints() != renderEntity.hModel.NumJoints()) {
                        gameLocal.Error("gib model '%s' has different number of joints than model '%s'",
                                skeletonModel.Name(), renderEntity.hModel.Name());
                    }
                }
            }
        }

        protected void Event_Gib(final String damageDefName) {
            Gib(new idVec3(0, 0, 1), damageDefName);
        }

        /**
         *
         *
         * inherited grandfather functions.
         *
         *
         */
        public final void idAFEntity_Base_Think() {
            super.Think();
        }

        public final void idAFEntity_Base_Hide() {
            super.Hide();
        }

        public final void idAFEntity_Base_Show() {
            super.Show();
        }

        public final boolean idAFEntity_Base_UpdateAnimationControllers() {
            return super.UpdateAnimationControllers();
        }
    };

    /*
     ===============================================================================

     idAFEntity_Generic

     ===============================================================================
     */
    public static class idAFEntity_Generic extends idAFEntity_Gibbable {
        // CLASS_PROTOTYPE( idAFEntity_Generic );

        private final boolean[] keepRunningPhysics = {false};
        //
        //

        public idAFEntity_Generic() {
            keepRunningPhysics[0] = false;
        }
        // ~idAFEntity_Generic( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            if (!LoadAF()) {
                gameLocal.Error("Couldn't load af file on entity '%s'", name);
            }

            SetCombatModel();

            SetPhysics(af.GetPhysics());

            af.GetPhysics().PutToRest();
            if (!spawnArgs.GetBool("nodrop", "0")) {
                af.GetPhysics().Activate();
            }

            fl.takedamage = true;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(keepRunningPhysics[0]);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadBool(keepRunningPhysics);
        }

        @Override
        public void Think() {
            idAFEntity_Base_Think();

            if (keepRunningPhysics[0]) {
                BecomeActive(TH_PHYSICS);
            }
        }

        public void KeepRunningPhysics() {
            keepRunningPhysics[0] = true;
        }

        private void Event_Activate(idEntity activator) {
            float delay;
            idVec3 init_velocity = new idVec3(), init_avelocity = new idVec3();

            Show();

            af.GetPhysics().EnableImpact();
            af.GetPhysics().Activate();

            spawnArgs.GetVector("init_velocity", "0 0 0", init_velocity);
            spawnArgs.GetVector("init_avelocity", "0 0 0", init_avelocity);

            delay = spawnArgs.GetFloat("init_velocityDelay", "0");
            if (delay == 0) {
                af.GetPhysics().SetLinearVelocity(init_velocity);
            } else {
                PostEventMS(EV_SetLinearVelocity, delay, init_velocity);
            }

            delay = spawnArgs.GetFloat("init_avelocityDelay", "0");
            if (delay == 0) {
                af.GetPhysics().SetAngularVelocity(init_avelocity);
            } else {
                PostEventSec(EV_SetAngularVelocity, delay, init_avelocity);
            }
        }
    };

    /*
     ===============================================================================

     idAFEntity_WithAttachedHead

     ===============================================================================
     */
    public static class idAFEntity_WithAttachedHead extends idAFEntity_Gibbable {
        // CLASS_PROTOTYPE( idAFEntity_WithAttachedHead );

        private idEntityPtr<idAFAttachment> head;
        //
        //

        public idAFEntity_WithAttachedHead() {
            head = null;
        }
        // ~idAFEntity_WithAttachedHead();

        @Override
        public void Spawn() {
            SetupHead();

            LoadAF();

            SetCombatModel();

            SetPhysics(af.GetPhysics());

            af.GetPhysics().PutToRest();
            if (!spawnArgs.GetBool("nodrop", "0")) {
                af.GetPhysics().Activate();
            }

            fl.takedamage = true;

            if (head.GetEntity() != null) {
                int anim = head.GetEntity().GetAnimator().GetAnim("dead");

                if (anim != 0) {
                    head.GetEntity().GetAnimator().SetFrame(ANIMCHANNEL_ALL, anim, 0, gameLocal.time, 0);
                }
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            head.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            head.Restore(savefile);
        }

        public void SetupHead() {
            idAFAttachment headEnt;
            String jointName;
            final String headModel;
            int/*jointHandle_t*/ joint;
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            headModel = spawnArgs.GetString("def_head", "");
            if (!headModel.isEmpty()) {//[ 0 ] ) {
                jointName = spawnArgs.GetString("head_joint");
                joint = animator.GetJointHandle(jointName);
                if (joint == INVALID_JOINT) {
                    gameLocal.Error("Joint '%s' not found for 'head_joint' on '%s'", jointName, name.toString());
                }

                headEnt = (idAFAttachment) gameLocal.SpawnEntityType(idAFAttachment.class, null);
                headEnt.SetName(va("%s_head", name));
                headEnt.SetBody(this, headModel, joint);
                headEnt.SetCombatModel();
                head.oSet(headEnt);

                animator.GetJointTransform(joint, gameLocal.time, origin, axis);
                origin = renderEntity.origin.oPlus(origin.oMultiply(renderEntity.axis));
                headEnt.SetOrigin(origin);
                headEnt.SetAxis(renderEntity.axis);
                headEnt.BindToJoint(this, joint, true);
            }
        }

        @Override
        public void Think() {
            idAFEntity_Base_Think();
        }

        @Override
        public void Hide() {
            idAFEntity_Base_Hide();
            if (head.GetEntity() != null) {
                head.GetEntity().Hide();
            }
            UnlinkCombat();
        }

        @Override
        public void Show() {
            idAFEntity_Base_Show();
            if (head.GetEntity() != null) {
                head.GetEntity().Show();
            }
            LinkCombat();
        }

        @Override
        public void ProjectOverlay(final idVec3 origin, final idVec3 dir, float size, final String material) {

            idEntity_ProjectOverlay(origin, dir, size, material);

            if (head.GetEntity() != null) {
                head.GetEntity().ProjectOverlay(origin, dir, size, material);
            }
        }

        @Override
        public void LinkCombat() {
            idAFAttachment headEnt;

            if (fl.hidden) {
                return;
            }

            if (combatModel != null) {
                combatModel.Link(gameLocal.clip, this, 0, renderEntity.origin, renderEntity.axis, modelDefHandle);
            }
            headEnt = head.GetEntity();
            if (headEnt != null) {
                headEnt.LinkCombat();
            }
        }

        @Override
        public void UnlinkCombat() {
            idAFAttachment headEnt;

            if (combatModel != null) {
                combatModel.Unlink();
            }
            headEnt = head.GetEntity();
            if (headEnt != null) {
                headEnt.UnlinkCombat();
            }
        }

        @Override
        protected void Gib(final idVec3 dir, final String damageDefName) {
            // only gib once
            if (gibbed) {
                return;
            }
            super.Gib(dir, damageDefName);
            if (head.GetEntity() != null) {
                head.GetEntity().Hide();
            }
        }

        @Override
        protected void Event_Gib(final String damageDefName) {
            Gib(new idVec3(0, 0, 1), damageDefName);
        }

        private void Event_Activate(idEntity activator) {
            float delay;
            idVec3 init_velocity = new idVec3(), init_avelocity = new idVec3();

            Show();

            af.GetPhysics().EnableImpact();
            af.GetPhysics().Activate();

            spawnArgs.GetVector("init_velocity", "0 0 0", init_velocity);
            spawnArgs.GetVector("init_avelocity", "0 0 0", init_avelocity);

            delay = spawnArgs.GetFloat("init_velocityDelay", "0");
            if (delay == 0) {
                af.GetPhysics().SetLinearVelocity(init_velocity);
            } else {
                PostEventSec(EV_SetLinearVelocity, delay, init_velocity);
            }

            delay = spawnArgs.GetFloat("init_avelocityDelay", "0");
            if (delay == 0) {
                af.GetPhysics().SetAngularVelocity(init_avelocity);
            } else {
                PostEventSec(EV_SetAngularVelocity, delay, init_avelocity);
            }
        }
    };

    /*
     ===============================================================================

     idAFEntity_Vehicle

     ===============================================================================
     */
    public static class idAFEntity_Vehicle extends idAFEntity_Base {
        // CLASS_PROTOTYPE( idAFEntity_Vehicle );

        protected idPlayer player;
        protected int/*jointHandle_t*/ eyesJoint;
        protected int/*jointHandle_t*/ steeringWheelJoint;
        protected float wheelRadius;
        protected float steerAngle;
        protected float steerSpeed;
        protected idDeclParticle dustSmoke;
        //
        //

        public idAFEntity_Vehicle() {
            player = null;
            eyesJoint = INVALID_JOINT;
            steeringWheelJoint = INVALID_JOINT;
            wheelRadius = 0;
            steerAngle = 0;
            steerSpeed = 0;
            dustSmoke = null;
        }

        @Override
        public void Spawn() {
            final String eyesJointName = spawnArgs.GetString("eyesJoint", "eyes");
            final String steeringWheelJointName = spawnArgs.GetString("steeringWheelJoint", "steeringWheel");
            float[] wheel = new float[1], steer = new float[1];

            LoadAF();

            SetCombatModel();

            SetPhysics(af.GetPhysics());

            fl.takedamage = true;

//	if ( !eyesJointName[0] ) {
            if (eyesJointName.isEmpty()) {
                gameLocal.Error("idAFEntity_Vehicle '%s' no eyes joint specified", name);
            }
            eyesJoint = animator.GetJointHandle(eyesJointName);
//	if ( !steeringWheelJointName[0] ) {
            if (steeringWheelJointName.isEmpty()) {
                gameLocal.Error("idAFEntity_Vehicle '%s' no steering wheel joint specified", name);
            }
            steeringWheelJoint = animator.GetJointHandle(steeringWheelJointName);

            spawnArgs.GetFloat("wheelRadius", "20", wheel);
            spawnArgs.GetFloat("steerSpeed", "5", steer);
            wheelRadius = wheel[0];
            steerSpeed = steer[0];

            player = null;
            steerAngle = 0;

            final String smokeName = spawnArgs.GetString("smoke_vehicle_dust", "muzzlesmoke");
            if (!smokeName.isEmpty()) {// != '\0' ) {
                dustSmoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
            }
        }

        public void Use(idPlayer other) {
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            if (player != null) {
                if (player.equals(other)) {
                    other.Unbind();
                    player = null;

                    af.GetPhysics().SetComeToRest(true);
                }
            } else {
                player = other;
                animator.GetJointTransform(eyesJoint, gameLocal.time, origin, axis);
                origin = renderEntity.origin.oPlus(origin.oMultiply(renderEntity.axis));
                player.GetPhysics().SetOrigin(origin);
                player.BindToBody(this, 0, true);

                af.GetPhysics().SetComeToRest(false);
                af.GetPhysics().Activate();
            }
        }

        protected float GetSteerAngle() {
            final float idealSteerAngle, angleDelta;

            idealSteerAngle = player.usercmd.rightmove * (30 / 128.0f);
            angleDelta = idealSteerAngle - steerAngle;

            if (angleDelta > steerSpeed) {
                steerAngle += steerSpeed;
            } else if (angleDelta < -steerSpeed) {
                steerAngle -= steerSpeed;
            } else {
                steerAngle = idealSteerAngle;
            }

            return steerAngle;
        }
    };

    /*
     ===============================================================================
     idAFEntity_VehicleSimple
     ===============================================================================
     */
    public static class idAFEntity_VehicleSimple extends idAFEntity_Vehicle {

        protected idClipModel wheelModel;
        protected final idAFConstraint_Suspension[] suspension = new idAFConstraint_Suspension[4];
        protected final int/*jointHandle_t*/[] wheelJoints = new int[4];
        protected final float[] wheelAngles = new float[4];
        //
        //

        // public:
        // CLASS_PROTOTYPE( idAFEntity_VehicleSimple );
        public idAFEntity_VehicleSimple() {
            int i;
            for (i = 0; i < 4; i++) {
                suspension[i] = null;
            }
        }
        // ~idAFEntity_VehicleSimple();
        private static final String[] wheelJointKeys = {
            "wheelJointFrontLeft",
            "wheelJointFrontRight",
            "wheelJointRearLeft",
            "wheelJointRearRight"
        };
        private static final idVec3[] wheelPoly/*[4]*/ = {
                    new idVec3(2, 2, 0),
                    new idVec3(2, -2, 0),
                    new idVec3(-2, -2, 0),
                    new idVec3(-2, 2, 0)
                };

        @Override
        public void Spawn() {

            int i;
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();
            idTraceModel trm = new idTraceModel();

            trm.SetupPolygon(wheelPoly, 4);
            trm.Translate(new idVec3(0, 0, -wheelRadius));
            wheelModel = new idClipModel(trm);

            for (i = 0; i < 4; i++) {
                final String wheelJointName = spawnArgs.GetString(wheelJointKeys[i], "");
//		if ( !wheelJointName[0] ) {
                if (wheelJointName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleSimple '%s' no '%s' specified", name, wheelJointKeys[i]);
                }
                wheelJoints[i] = animator.GetJointHandle(wheelJointName);
                if (wheelJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idAFEntity_VehicleSimple '%s' can't find wheel joint '%s'", name, wheelJointName);
                }

                GetAnimator().GetJointTransform(wheelJoints[i], 0, origin, axis);
                origin = renderEntity.origin.oPlus(origin.oMultiply(renderEntity.axis));

                suspension[i] = new idAFConstraint_Suspension();
                suspension[i].Setup(va("suspension%d", i), af.GetPhysics().GetBody(0), origin, af.GetPhysics().GetAxis(0), wheelModel);
                suspension[i].SetSuspension(
                        g_vehicleSuspensionUp.GetFloat(),
                        g_vehicleSuspensionDown.GetFloat(),
                        g_vehicleSuspensionKCompress.GetFloat(),
                        g_vehicleSuspensionDamping.GetFloat(),
                        g_vehicleTireFriction.GetFloat());

                af.GetPhysics().AddConstraint(suspension[i]);
            }

//            memset(wheelAngles, 0, sizeof(wheelAngles));
            Arrays.fill(wheelAngles, 0);
            BecomeActive(TH_THINK);
        }

        @Override
        public void Think() {
            int i;
            float force = 0, velocity = 0, steerAngle = 0;
            idVec3 origin;
            idMat3 axis = new idMat3();
            idRotation wheelRotation = new idRotation(), steerRotation = new idRotation();

            if ((thinkFlags & TH_THINK) != 0) {

                if (player != null) {
                    // capture the input from a player
                    velocity = g_vehicleVelocity.GetFloat();
                    if (player.usercmd.forwardmove < 0) {
                        velocity = -velocity;
                    }
                    force = idMath.Fabs(player.usercmd.forwardmove * g_vehicleForce.GetFloat()) * (1.0f / 128.0f);
                    steerAngle = GetSteerAngle();
                }

                // update the wheel motor force and steering
                for (i = 0; i < 2; i++) {

                    // front wheel drive
                    if (velocity != 0) {
                        suspension[i].EnableMotor(true);
                    } else {
                        suspension[i].EnableMotor(false);
                    }
                    suspension[i].SetMotorVelocity(velocity);
                    suspension[i].SetMotorForce(force);

                    // update the wheel steering
                    suspension[i].SetSteerAngle(steerAngle);
                }

                // adjust wheel velocity for better steering because there are no differentials between the wheels
                if (steerAngle < 0) {
                    suspension[0].SetMotorVelocity(velocity * 0.5f);
                } else if (steerAngle > 0) {
                    suspension[1].SetMotorVelocity(velocity * 0.5f);
                }

                // update suspension with latest cvar settings
                for (i = 0; i < 4; i++) {
                    suspension[i].SetSuspension(
                            g_vehicleSuspensionUp.GetFloat(),
                            g_vehicleSuspensionDown.GetFloat(),
                            g_vehicleSuspensionKCompress.GetFloat(),
                            g_vehicleSuspensionDamping.GetFloat(),
                            g_vehicleTireFriction.GetFloat());
                }

                // run the physics
                RunPhysics();

                // move and rotate the wheels visually
                for (i = 0; i < 4; i++) {
                    idAFBody body = af.GetPhysics().GetBody(0);

                    origin = suspension[i].GetWheelOrigin();
                    velocity = body.GetPointVelocity(origin).oMultiply(body.GetWorldAxis().oGet(0));
                    wheelAngles[i] += velocity * MS2SEC(gameLocal.msec) / wheelRadius;

                    // additional rotation about the wheel axis
                    wheelRotation.SetAngle(RAD2DEG(wheelAngles[i]));
                    wheelRotation.SetVec(0, -1, 0);

                    if (i < 2) {
                        // rotate the wheel for steering
                        steerRotation.SetAngle(steerAngle);
                        steerRotation.SetVec(0, 0, 1);
                        // set wheel rotation
                        animator.SetJointAxis(wheelJoints[i], JOINTMOD_WORLD, wheelRotation.ToMat3().oMultiply(steerRotation.ToMat3()));
                    } else {
                        // set wheel rotation
                        animator.SetJointAxis(wheelJoints[i], JOINTMOD_WORLD, wheelRotation.ToMat3());
                    }

                    // set wheel position for suspension
                    origin = (origin.oMinus(renderEntity.origin)).oMultiply(renderEntity.axis.Transpose());
                    GetAnimator().SetJointPos(wheelJoints[i], JOINTMOD_WORLD_OVERRIDE, origin);
                }
                /*
                 // spawn dust particle effects
                 if ( force != 0 && !( gameLocal.framenum & 7 ) ) {
                 int numContacts;
                 idAFConstraint_Contact *contacts[2];
                 for ( i = 0; i < 4; i++ ) {
                 numContacts = af.GetPhysics().GetBodyContactConstraints( wheels[i].GetClipModel().GetId(), contacts, 2 );
                 for ( int j = 0; j < numContacts; j++ ) {
                 gameLocal.smokeParticles.EmitSmoke( dustSmoke, gameLocal.time, gameLocal.random.RandomFloat(), contacts[j].GetContact().point, contacts[j].GetContact().normal.ToMat3() );
                 }
                 }
                 }
                 */
            }

            UpdateAnimation();
            if ((thinkFlags & TH_UPDATEVISUALS) != 0) {
                Present();
                LinkCombat();
            }
        }

    };

    /*
     ===============================================================================
     idAFEntity_VehicleFourWheels
     ===============================================================================
     */
    public static class idAFEntity_VehicleFourWheels extends idAFEntity_Vehicle {

        protected final idAFBody[] wheels = new idAFBody[4];
        protected final idAFConstraint_Hinge[] steering = new idAFConstraint_Hinge[2];
        protected final int/*jointHandle_t*/[] wheelJoints = new int[4];
        protected final float[] wheelAngles = new float[4];
        //
        //

        // public:
        // CLASS_PROTOTYPE( idAFEntity_VehicleFourWheels );
        public idAFEntity_VehicleFourWheels() {
            int i;

            for (i = 0; i < 4; i++) {
                wheels[i] = null;
                wheelJoints[i] = INVALID_JOINT;
                wheelAngles[i] = 0;
            }
            steering[0] = null;
            steering[1] = null;
        }
        private static final String[] wheelBodyKeys = {
            "wheelBodyFrontLeft",
            "wheelBodyFrontRight",
            "wheelBodyRearLeft",
            "wheelBodyRearRight"
        };
        private static final String[] wheelJointKeys = {
            "wheelJointFrontLeft",
            "wheelJointFrontRight",
            "wheelJointRearLeft",
            "wheelJointRearRight"
        };
        private static final String[] steeringHingeKeys = {
            "steeringHingeFrontLeft",
            "steeringHingeFrontRight"
        };

        @Override
        public void Spawn() {
            int i;
            String wheelBodyName, wheelJointName, steeringHingeName;

            for (i = 0; i < 4; i++) {
                wheelBodyName = spawnArgs.GetString(wheelBodyKeys[i], "");
//		if ( !wheelBodyName[0] ) {
                if (wheelBodyName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' no '%s' specified", name, wheelBodyKeys[i]);
                }
                wheels[i] = af.GetPhysics().GetBody(wheelBodyName);
                if (null == wheels[i]) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' can't find wheel body '%s'", name, wheelBodyName);
                }
                wheelJointName = spawnArgs.GetString(wheelJointKeys[i], "");
//		if ( !wheelJointName[0] ) {
                if (wheelJointName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' no '%s' specified", name, wheelJointKeys[i]);
                }
                wheelJoints[i] = animator.GetJointHandle(wheelJointName);
                if (wheelJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' can't find wheel joint '%s'", name, wheelJointName);
                }
            }

            for (i = 0; i < 2; i++) {
                steeringHingeName = spawnArgs.GetString(steeringHingeKeys[i], "");
//		if ( !steeringHingeName[0] ) {
                if (steeringHingeName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' no '%s' specified", name, steeringHingeKeys[i]);
                }
                steering[i] = (idAFConstraint_Hinge) af.GetPhysics().GetConstraint(steeringHingeName);
                if (NOT(steering[i])) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s': can't find steering hinge '%s'", name, steeringHingeName);
                }
            }

//	memset( wheelAngles, 0, sizeof( wheelAngles ) );
            Arrays.fill(wheelAngles, 0);
            BecomeActive(TH_THINK);
        }

        @Override
        public void Think() {
            int i;
            float force = 0, velocity = 0, steerAngle = 0;
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();
            idRotation rotation = new idRotation();

            if ((thinkFlags & TH_THINK) != 0) {

                if (player != null) {
                    // capture the input from a player
                    velocity = g_vehicleVelocity.GetFloat();
                    if (player.usercmd.forwardmove < 0) {
                        velocity = -velocity;
                    }
                    force = idMath.Fabs(player.usercmd.forwardmove * g_vehicleForce.GetFloat()) * (1.0f / 128.0f);
                    steerAngle = GetSteerAngle();
                }

                // update the wheel motor force
                for (i = 0; i < 2; i++) {
                    wheels[2 + i].SetContactMotorVelocity(velocity);
                    wheels[2 + i].SetContactMotorForce(force);
                }

                // adjust wheel velocity for better steering because there are no differentials between the wheels
                if (steerAngle < 0) {
                    wheels[2].SetContactMotorVelocity(velocity * 0.5f);
                } else if (steerAngle > 0) {
                    wheels[3].SetContactMotorVelocity(velocity * 0.5f);
                }

                // update the wheel steering
                steering[0].SetSteerAngle(steerAngle);
                steering[1].SetSteerAngle(steerAngle);
                for (i = 0; i < 2; i++) {
                    steering[i].SetSteerSpeed(3.0f);
                }

                // update the steering wheel
                animator.GetJointTransform(steeringWheelJoint, gameLocal.time, origin, axis);
                rotation.SetVec(axis.oGet(2));
                rotation.SetAngle(-steerAngle);
                animator.SetJointAxis(steeringWheelJoint, JOINTMOD_WORLD, rotation.ToMat3());

                // run the physics
                RunPhysics();

                // rotate the wheels visually
                for (i = 0; i < 4; i++) {
                    if (force == 0) {
                        velocity = wheels[i].GetLinearVelocity().oMultiply(wheels[i].GetWorldAxis().oGet(0));
                    }
                    wheelAngles[i] += velocity * MS2SEC(gameLocal.msec) / wheelRadius;
                    // give the wheel joint an additional rotation about the wheel axis
                    rotation.SetAngle(RAD2DEG(wheelAngles[i]));
                    axis = af.GetPhysics().GetAxis(0);
                    rotation.SetVec((wheels[i].GetWorldAxis().oMultiply(axis.Transpose())).oGet(2));
                    animator.SetJointAxis(wheelJoints[i], JOINTMOD_WORLD, rotation.ToMat3());
                }

                // spawn dust particle effects
                if (force != 0 && (0 == (gameLocal.framenum & 7))) {
                    int numContacts;
                    idAFConstraint_Contact[] contacts = new idAFConstraint_Contact[2];
                    for (i = 0; i < 4; i++) {
                        numContacts = af.GetPhysics().GetBodyContactConstraints(wheels[i].GetClipModel().GetId(), contacts, 2);
                        for (int j = 0; j < numContacts; j++) {
                            gameLocal.smokeParticles.EmitSmoke(dustSmoke, gameLocal.time, gameLocal.random.RandomFloat(), contacts[j].GetContact().point, contacts[j].GetContact().normal.ToMat3());
                        }
                    }
                }
            }

            UpdateAnimation();
            if ((thinkFlags & TH_UPDATEVISUALS) != 0) {
                Present();
                LinkCombat();
            }
        }
    };

    /*
     ===============================================================================
     idAFEntity_VehicleSixWheels
     ===============================================================================
     */
    public static class idAFEntity_VehicleSixWheels extends idAFEntity_Vehicle {

        private final idAFBody[] wheels = new idAFBody[6];
        private final idAFConstraint_Hinge[] steering = new idAFConstraint_Hinge[4];
        private final int/*jointHandle_t*/[] wheelJoints = new int[6];
        private final float[] wheelAngles = new float[6];
        //
        //

        // public:
        // CLASS_PROTOTYPE( idAFEntity_VehicleSixWheels );
        public idAFEntity_VehicleSixWheels() {
            int i;

            for (i = 0; i < 6; i++) {
                wheels[i] = null;
                wheelJoints[i] = INVALID_JOINT;
                wheelAngles[i] = 0;
            }
            steering[0] = null;
            steering[1] = null;
            steering[2] = null;
            steering[3] = null;
        }
        private static final String[] wheelBodyKeys = {
            "wheelBodyFrontLeft",
            "wheelBodyFrontRight",
            "wheelBodyMiddleLeft",
            "wheelBodyMiddleRight",
            "wheelBodyRearLeft",
            "wheelBodyRearRight"
        };
        private static final String[] wheelJointKeys = {
            "wheelJointFrontLeft",
            "wheelJointFrontRight",
            "wheelJointMiddleLeft",
            "wheelJointMiddleRight",
            "wheelJointRearLeft",
            "wheelJointRearRight"
        };
        private static final String[] steeringHingeKeys = {
            "steeringHingeFrontLeft",
            "steeringHingeFrontRight",
            "steeringHingeRearLeft",
            "steeringHingeRearRight"
        };

        @Override
        public void Spawn() {
            int i;

            String wheelBodyName, wheelJointName, steeringHingeName;

            for (i = 0; i < 6; i++) {
                wheelBodyName = spawnArgs.GetString(wheelBodyKeys[i], "");
//		if ( !wheelBodyName[0] ) {
                if (wheelBodyName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' no '%s' specified", name, wheelBodyKeys[i]);
                }
                wheels[i] = af.GetPhysics().GetBody(wheelBodyName);
                if (NOT(wheels[i])) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' can't find wheel body '%s'", name, wheelBodyName);
                }
                wheelJointName = spawnArgs.GetString(wheelJointKeys[i], "");
//		if ( !wheelJointName[0] ) {
                if (wheelJointName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' no '%s' specified", name, wheelJointKeys[i]);
                }
                wheelJoints[i] = animator.GetJointHandle(wheelJointName);
                if (wheelJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' can't find wheel joint '%s'", name, wheelJointName);
                }
            }

            for (i = 0; i < 4; i++) {
                steeringHingeName = spawnArgs.GetString(steeringHingeKeys[i], "");
//		if ( !steeringHingeName[0] ) {
                if (steeringHingeName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' no '%s' specified", name, steeringHingeKeys[i]);
                }
                steering[i] = (idAFConstraint_Hinge) af.GetPhysics().GetConstraint(steeringHingeName);
                if (NOT(steering[i])) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s': can't find steering hinge '%s'", name, steeringHingeName);
                }
            }

//	memset( wheelAngles, 0, sizeof( wheelAngles ) );
            Arrays.fill(wheelAngles, 0);
            BecomeActive(TH_THINK);
        }

        @Override
        public void Think() {
            int i;
            float force = 0, velocity = 0, steerAngle = 0;
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();
            idRotation rotation = new idRotation();

            if ((thinkFlags & TH_THINK) != 0) {

                if (player != null) {
                    // capture the input from a player
                    velocity = g_vehicleVelocity.GetFloat();
                    if (player.usercmd.forwardmove < 0) {
                        velocity = -velocity;
                    }
                    force = idMath.Fabs(player.usercmd.forwardmove * g_vehicleForce.GetFloat()) * (1.0f / 128.0f);
                    steerAngle = GetSteerAngle();
                }

                // update the wheel motor force
                for (i = 0; i < 6; i++) {
                    wheels[i].SetContactMotorVelocity(velocity);
                    wheels[i].SetContactMotorForce(force);
                }

                // adjust wheel velocity for better steering because there are no differentials between the wheels
                if (steerAngle < 0) {
                    for (i = 0; i < 3; i++) {
                        wheels[(i << 1)].SetContactMotorVelocity(velocity * 0.5f);
                    }
                } else if (steerAngle > 0) {
                    for (i = 0; i < 3; i++) {
                        wheels[1 + (i << 1)].SetContactMotorVelocity(velocity * 0.5f);
                    }
                }

                // update the wheel steering
                steering[0].SetSteerAngle(steerAngle);
                steering[1].SetSteerAngle(steerAngle);
                steering[2].SetSteerAngle(-steerAngle);
                steering[3].SetSteerAngle(-steerAngle);
                for (i = 0; i < 4; i++) {
                    steering[i].SetSteerSpeed(3.0f);
                }

                // update the steering wheel
                animator.GetJointTransform(steeringWheelJoint, gameLocal.time, origin, axis);
                rotation.SetVec(axis.oGet(2));
                rotation.SetAngle(-steerAngle);
                animator.SetJointAxis(steeringWheelJoint, JOINTMOD_WORLD, rotation.ToMat3());

                // run the physics
                RunPhysics();

                // rotate the wheels visually
                for (i = 0; i < 6; i++) {
                    if (force == 0) {
                        velocity = wheels[i].GetLinearVelocity().oMultiply(wheels[i].GetWorldAxis().oGet(0));
                    }
                    wheelAngles[i] += velocity * MS2SEC(gameLocal.msec) / wheelRadius;
                    // give the wheel joint an additional rotation about the wheel axis
                    rotation.SetAngle(RAD2DEG(wheelAngles[i]));
                    axis = af.GetPhysics().GetAxis(0);
                    rotation.SetVec((wheels[i].GetWorldAxis().oMultiply(axis.Transpose()).oGet(2)));
                    animator.SetJointAxis(wheelJoints[i], JOINTMOD_WORLD, rotation.ToMat3());
                }

                // spawn dust particle effects
                if (force != 0 && (0 == (gameLocal.framenum & 7))) {
                    int numContacts;
                    idAFConstraint_Contact[] contacts = new idAFConstraint_Contact[2];
                    for (i = 0; i < 6; i++) {
                        numContacts = af.GetPhysics().GetBodyContactConstraints(wheels[i].GetClipModel().GetId(), contacts, 2);
                        for (int j = 0; j < numContacts; j++) {
                            gameLocal.smokeParticles.EmitSmoke(dustSmoke, gameLocal.time, gameLocal.random.RandomFloat(), contacts[j].GetContact().point, contacts[j].GetContact().normal.ToMat3());
                        }
                    }
                }
            }

            UpdateAnimation();
            if ((thinkFlags & TH_UPDATEVISUALS) != 0) {
                Present();
                LinkCombat();
            }
        }
    };

    /*
     ===============================================================================
     idAFEntity_SteamPipe
     ===============================================================================
     */
    public static class idAFEntity_SteamPipe extends idAFEntity_Base {
        // CLASS_PROTOTYPE( idAFEntity_SteamPipe );

        private int steamBody;
        private float steamForce;
        private float steamUpForce;
        private idForce_Constant force;
        private renderEntity_s steamRenderEntity;
        private int/*qhandle_t*/ steamModelDefHandle;
        //
        //

        public idAFEntity_SteamPipe() {
            steamBody = 0;
            steamForce = 0;
            steamUpForce = 0;
            steamModelDefHandle = -1;
//	memset( &steamRenderEntity, 0, sizeof( steamRenderEntity ) );
            steamRenderEntity = new renderEntity_s();
        }
        // ~idAFEntity_SteamPipe();

        @Override
        public void Spawn() {
            idVec3 steamDir;
            final String steamBodyName;

            LoadAF();

            SetCombatModel();

            SetPhysics(af.GetPhysics());

            fl.takedamage = true;

            steamBodyName = spawnArgs.GetString("steamBody", "");
            steamForce = spawnArgs.GetFloat("steamForce", "2000");
            steamUpForce = spawnArgs.GetFloat("steamUpForce", "10");
            steamDir = af.GetPhysics().GetAxis(steamBody).oGet(2);//[2];
            steamBody = af.GetPhysics().GetBodyId(steamBodyName);
            force.SetPosition(af.GetPhysics(), steamBody, af.GetPhysics().GetOrigin(steamBody));
            force.SetForce(steamDir.oMultiply(-steamForce));

            InitSteamRenderEntity();

            BecomeActive(TH_THINK);
        }

        @Override
        public void Save(idSaveGame savefile) {
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            Spawn();
        }

        @Override
        public void Think() {
            idVec3 steamDir = new idVec3();

            if ((thinkFlags & TH_THINK) != 0) {
                steamDir.x = gameLocal.random.CRandomFloat() * steamForce;
                steamDir.y = gameLocal.random.CRandomFloat() * steamForce;
                steamDir.z = steamUpForce;
                force.SetForce(steamDir);
                force.Evaluate(gameLocal.time);
                //gameRenderWorld.DebugArrow( colorWhite, af.GetPhysics().GetOrigin( steamBody ), af.GetPhysics().GetOrigin( steamBody ) - 10 * steamDir, 4 );
            }

            if (steamModelDefHandle >= 0) {
                steamRenderEntity.origin = new idVec3(af.GetPhysics().GetOrigin(steamBody));
                steamRenderEntity.axis = new idMat3(af.GetPhysics().GetAxis(steamBody));
                gameRenderWorld.UpdateEntityDef(steamModelDefHandle, steamRenderEntity);
            }

            super.Think();
        }

        private void InitSteamRenderEntity() {
            final String temp;
            idDeclModelDef modelDef;

//	memset( steamRenderEntity, 0, sizeof( steamRenderEntity ) );
            steamRenderEntity = new renderEntity_s();
            steamRenderEntity.shaderParms[SHADERPARM_RED] = 1.0f;
            steamRenderEntity.shaderParms[SHADERPARM_GREEN] = 1.0f;
            steamRenderEntity.shaderParms[SHADERPARM_BLUE] = 1.0f;
//            modelDef = null;
            temp = spawnArgs.GetString("model_steam");
            if (!temp.isEmpty()) {// != '\0' ) {
//		if ( !strstr( temp, "." ) ) {
                if (!temp.contains(".")) {
                    modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, temp, false);
                    if (modelDef != null) {
                        steamRenderEntity.hModel = modelDef.ModelHandle();
                    }
                }

                if (null == steamRenderEntity.hModel) {
                    steamRenderEntity.hModel = renderModelManager.FindModel(temp);
                }

                if (steamRenderEntity.hModel != null) {
                    steamRenderEntity.bounds = steamRenderEntity.hModel.Bounds(steamRenderEntity);
                } else {
                    steamRenderEntity.bounds.Zero();
                }
                steamRenderEntity.origin = new idVec3(af.GetPhysics().GetOrigin(steamBody));
                steamRenderEntity.axis = new idMat3(af.GetPhysics().GetAxis(steamBody));
                steamModelDefHandle = gameRenderWorld.AddEntityDef(steamRenderEntity);
            }
        }
    };

//
    public static final String[] clawConstraintNames = {
        "claw1", "claw2", "claw3", "claw4"
    };

    /*
     ===============================================================================
     idAFEntity_ClawFourFingers
     ===============================================================================
     */
    public static class idAFEntity_ClawFourFingers extends idAFEntity_Base {
        // public:
        // CLASS_PROTOTYPE( idAFEntity_ClawFourFingers );

        public idAFEntity_ClawFourFingers() {
            fingers[0] = null;
            fingers[1] = null;
            fingers[2] = null;
            fingers[3] = null;
        }

        @Override
        public void Spawn() {
            int i;

            LoadAF();

            SetCombatModel();

            af.GetPhysics().LockWorldConstraints(true);
            af.GetPhysics().SetForcePushable(true);
            SetPhysics(af.GetPhysics());

            fl.takedamage = true;

            for (i = 0; i < 4; i++) {
                fingers[i] = (idAFConstraint_Hinge) af.GetPhysics().GetConstraint(clawConstraintNames[i]);
                if (NOT(fingers[i])) {
                    gameLocal.Error("idClaw_FourFingers '%s': can't find claw constraint '%s'", name, clawConstraintNames[i]);
                }
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            for (i = 0; i < 4; i++) {
                fingers[i].Save(savefile);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;

            for (i = 0; i < 4; i++) {
                fingers[i] = (idAFConstraint_Hinge) af.GetPhysics().GetConstraint(clawConstraintNames[i]);
                fingers[i].Restore(savefile);
            }

            SetCombatModel();
            LinkCombat();
        }
        //
        //
        private final idAFConstraint_Hinge[] fingers = new idAFConstraint_Hinge[4];
        //
        //

        private void Event_SetFingerAngle(float angle) {
            int i;

            for (i = 0; i < 4; i++) {
                fingers[i].SetSteerAngle(angle);
                fingers[i].SetSteerSpeed(0.5f);
            }
            af.GetPhysics().Activate();
        }

        private void Event_StopFingers() {
            int i;

            for (i = 0; i < 4; i++) {
                fingers[i].SetSteerAngle(fingers[i].GetAngle());
            }
        }
    };

    /*
     ===============================================================================

     editor support routines

     ===============================================================================
     */
    /*
     ================
     GetJointTransform
     ================
     */
    public static class jointTransformData_t {

        renderEntity_s ent;
        idMD5Joint[] joints;
    };

    static class GetJointTransform extends getJointTransform_t {

        public static final getJointTransform_t INSTANCE = new GetJointTransform();

        private GetJointTransform() {
        }

        @Override
        public boolean run(Object model, idJointMat[] frame, String jointName, idVec3 origin, idMat3 axis) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean run(Object model, idJointMat[] frame, idStr jointName, idVec3 origin, idMat3 axis) {

            int i;
//        jointTransformData_t *data = reinterpret_cast<jointTransformData_t *>(model);
            jointTransformData_t data = (jointTransformData_t) model;

            for (i = 0; i < data.ent.numJoints; i++) {
                if (data.joints[i].name.Icmp(jointName) == 0) {
                    break;
                }
            }
            if (i >= data.ent.numJoints) {
                return false;
            }
            origin = frame[i].ToVec3();
            axis = frame[i].ToMat3();
            return true;
        }
    };

    /*
     ================
     GetArgString
     ================
     */
    public static String GetArgString(final idDict args, final idDict defArgs, final String key) {
        String s;

        s = args.GetString(key);
//	if ( !s[0] && defArgs ) {
        if (s.isEmpty() && defArgs != null) {
            s = defArgs.GetString(key);
        }
        return s;
    }
}
