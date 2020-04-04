package neo.Game;

import static neo.Game.Animation.Anim.ANIMCHANNEL_ALL;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD_OVERRIDE;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_SetAngularVelocity;
import static neo.Game.Entity.EV_SetLinearVelocity;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEPARTICLES;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import static neo.Game.GameSys.Class.EV_Remove;
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
import static neo.Game.Physics.Clip.JOINT_HANDLE_TO_CLIPMODEL_ID;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIME_OF_DEATH;
import static neo.TempDump.NOT;
import static neo.framework.Common.EDITOR_AF;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.RAD2DEG;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.Game.AF.idAF;
import neo.Game.Entity.idAnimatedEntity;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Item.idMoveableItem;
import neo.Game.Player.idPlayer;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
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
import neo.Renderer.Model.idMD5Joint;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.framework.DeclAF.getJointTransform_t;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclSkin.idDeclSkin;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

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

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		protected idPhysics_AF physicsObj;
        //
        private idList<idRenderModel> modelHandles;
        private idList<Integer> modelDefHandles;
        //
        //

        @Override
        public void Spawn() {
            this.physicsObj.SetSelf(this);
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
            if (0 == (this.thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }
            BecomeInactive(TH_UPDATEVISUALS);

            for (i = 0; i < this.modelHandles.Num(); i++) {

                if (null == this.modelHandles.oGet(i)) {
                    continue;
                }

                this.renderEntity.origin.oSet(this.physicsObj.GetOrigin(i));
                this.renderEntity.axis.oSet(this.physicsObj.GetAxis(i));
                this.renderEntity.hModel = this.modelHandles.oGet(i);
                this.renderEntity.bodyId = i;

                // add to refresh list
                if (this.modelDefHandles.oGet(i) == -1) {
                    this.modelDefHandles.oSet(i, gameRenderWorld.AddEntityDef(this.renderEntity));
                } else {
                    gameRenderWorld.UpdateEntityDef(this.modelDefHandles.oGet(i), this.renderEntity);
                }
            }
        }

        protected void SetModelForId(int id, final String modelName) {
            this.modelHandles.AssureSize(id + 1, null);
            this.modelDefHandles.AssureSize(id + 1, -1);
            this.modelHandles.oSet(id, renderModelManager.FindModel(modelName));
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     ===============================================================================

     idChain

     Chain hanging down from the ceiling. Only used for debugging!

     ===============================================================================
     */
    public static class idChain extends idMultiModelAF {
//public	CLASS_PROTOTYPE( idChain );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public void Spawn() {
            final int[] numLinks = new int[1];
            final float[] length = new float[1], linkWidth = new float[1], density = new float[1];
            float linkLength;
            final boolean[] drop = {false};
            idVec3 origin;

            this.spawnArgs.GetBool("drop", "0", drop);
            this.spawnArgs.GetInt("links", "3", numLinks);
            this.spawnArgs.GetFloat("length", "" + (numLinks[0] * 32.0f), length);
            this.spawnArgs.GetFloat("width", "8", linkWidth);
            this.spawnArgs.GetFloat("density", "0.2", density);
            linkLength = length[0] / numLinks[0];
            origin = GetPhysics().GetOrigin();

            // initialize physics
            this.physicsObj.SetSelf(this);
            this.physicsObj.SetGravity(gameLocal.GetGravity());
            this.physicsObj.SetClipMask(MASK_SOLID | CONTENTS_BODY);
            SetPhysics(this.physicsObj);

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
            final float halfLinkLength = linkLength * 0.5f;
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
                this.physicsObj.AddBody(body);

                // visual model for body
                SetModelForId(this.physicsObj.GetBodyId(body), this.spawnArgs.GetString("model"));

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
                    this.physicsObj.AddConstraint(uj);
                } else {
                    if (lastBody != null) {
                        bsj = new idAFConstraint_BallAndSocketJoint(new idStr("joint" + i), lastBody, body);
                        bsj.SetAnchor(org.oPlus(new idVec3(0, 0, halfLinkLength)));
                        bsj.SetConeLimit(new idVec3(0, 0, 1), 60, new idVec3(0, 0, 1));
                        this.physicsObj.AddConstraint(bsj);
                    }
                }

                org.oMinSet(2, linkLength);

                lastBody = body;
            }
        }
    }

    /*
     ===============================================================================

     idAFAttachment

     ===============================================================================
     */
    public static class idAFAttachment extends idAnimatedEntity {
// public	CLASS_PROTOTYPE( idAFAttachment );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		protected idEntity body;
        protected idClipModel combatModel;	// render model for hit detection of head
        protected int idleAnim;
        protected int/*jointHandle_t*/ attachJoint;
        //
        //

        public idAFAttachment() {
            this.body = null;
            this.combatModel = null;
            this.idleAnim = 0;
            this.attachJoint = INVALID_JOINT;
        }
        // virtual					~idAFAttachment( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            this.idleAnim = this.animator.GetAnim("idle");
        }

        /*
         ================
         idAFAttachment::Save

         archive object for savegame file
         ================
         */
        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteObject(this.body);
            savefile.WriteInt(this.idleAnim);
            savefile.WriteJoint(this.attachJoint);
        }

        /*
         ================
         idAFAttachment::Restore

         unarchives object from save game file
         ================
         */
        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadObject(this./*reinterpret_cast<idClass*&>*/body);
            this.idleAnim = savefile.ReadInt();
            this.attachJoint = savefile.ReadJoint();

            SetCombatModel();
            LinkCombat();
        }

        public void SetBody(idEntity bodyEnt, final String headModel, int/*jointHandle_t*/ attachJoint) {
            boolean bleed;

            this.body = bodyEnt;
            this.attachJoint = attachJoint;
            SetModel(headModel);
            this.fl.takedamage = true;

            bleed = this.body.spawnArgs.GetBool("bleed");
            this.spawnArgs.SetBool("bleed", bleed);
        }

        public void ClearBody() {
            this.body = null;
            this.attachJoint = INVALID_JOINT;
            Hide();
        }

        public idEntity GetBody() {
            return this.body;
        }

        @Override
        public void Think() {
            super.Think();
            if ((this.thinkFlags & TH_UPDATEPARTICLES) != 0) {
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
            if ((this.idleAnim != 0) && (this.idleAnim != this.animator.CurrentAnim(ANIMCHANNEL_ALL).AnimNum())) {
                this.animator.CycleAnim(ANIMCHANNEL_ALL, this.idleAnim, gameLocal.time, blendTime);
            }
        }

        @Override
        public impactInfo_s GetImpactInfo(idEntity ent, int id, final idVec3 point) {
            if (this.body != null) {
               return  this.body.GetImpactInfo(ent, JOINT_HANDLE_TO_CLIPMODEL_ID(this.attachJoint), point);
            } else {
                return idEntity_GetImpactInfo(ent, id, point);
            }
        }

        @Override
        public void ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {
            if (this.body != null) {
                this.body.ApplyImpulse(ent, JOINT_HANDLE_TO_CLIPMODEL_ID(this.attachJoint), point, impulse);
            } else {
                idEntity_ApplyImpulse(ent, id, point, impulse);
            }
        }

        @Override
        public void AddForce(idEntity ent, int id, final idVec3 point, final idVec3 force) {
            if (this.body != null) {
                this.body.AddForce(ent, JOINT_HANDLE_TO_CLIPMODEL_ID(this.attachJoint), point, force);
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

            if (this.body != null) {
                this.body.Damage(inflictor, attacker, dir, damageDefName, damageScale, this.attachJoint);
            }
        }

        @Override
        public void AddDamageEffect(final trace_s collision, final idVec3 velocity, final String damageDefName) {
            if (this.body != null) {
                final trace_s c = collision;
                c.c.id = JOINT_HANDLE_TO_CLIPMODEL_ID(this.attachJoint);
                this.body.AddDamageEffect(c, velocity, damageDefName);
            }
        }

        public void SetCombatModel() {
            if (this.combatModel != null) {
                this.combatModel.Unlink();
                this.combatModel.LoadModel(this.modelDefHandle);
            } else {
                this.combatModel = new idClipModel(this.modelDefHandle);
            }
            this.combatModel.SetOwner(this.body);
        }

        public idClipModel GetCombatModel() {
            return this.combatModel;
        }

        public void LinkCombat() {
            if (this.fl.hidden) {
                return;
            }

            if (this.combatModel != null) {
                this.combatModel.Link(gameLocal.clip, this, 0, this.renderEntity.origin, this.renderEntity.axis, this.modelDefHandle);
            }
        }

        public void UnlinkCombat() {
            if (this.combatModel != null) {
                this.combatModel.Unlink();
            }
        }
    }

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
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public	CLASS_PROTOTYPE( idAFEntity_Base );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idAnimatedEntity.getEventCallBacks());
            eventCallbacks.put(EV_SetConstraintPosition, (eventCallback_t2<idAFEntity_Base>) idAFEntity_Base::Event_SetConstraintPosition);
        }

        protected idAF        af;                   // articulated figure
        protected idClipModel combatModel;          // render model for hit detection
        protected int         combatModelContents;
        protected idVec3      spawnOrigin;          // spawn origin
        protected idMat3      spawnAxis;            // rotation axis used when spawned
        protected int         nextSoundTime;        // next time this can make a sound
        //
        //

        public idAFEntity_Base() {
            this.af = new idAF();
            this.combatModel = null;
            this.combatModelContents = 0;
            this.nextSoundTime = 0;
            this.spawnOrigin = new idVec3();
            this.spawnAxis = getMat3_identity();
        }
        // virtual					~idAFEntity_Base( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            this.spawnOrigin.oSet(GetPhysics().GetOrigin());
            this.spawnAxis.oSet(GetPhysics().GetAxis());
            this.nextSoundTime = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.combatModelContents);
            savefile.WriteClipModel(this.combatModel);
            savefile.WriteVec3(this.spawnOrigin);
            savefile.WriteMat3(this.spawnAxis);
            savefile.WriteInt(this.nextSoundTime);
            this.af.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.combatModelContents = savefile.ReadInt();
            savefile.ReadClipModel(this.combatModel);
            savefile.ReadVec3(this.spawnOrigin);
            savefile.ReadMat3(this.spawnAxis);
            this.nextSoundTime = savefile.ReadInt();
            LinkCombat();

            this.af.Restore(savefile);
        }

        @Override
        public void Think() {
            RunPhysics();
            UpdateAnimation();
            if ((this.thinkFlags & TH_UPDATEVISUALS) != 0) {
                Present();
                LinkCombat();
            }
        }

        @Override
        public impactInfo_s GetImpactInfo(idEntity ent, int id, final idVec3 point) {
            if (this.af.IsActive()) {
                return this.af.GetImpactInfo(ent, id, point);
            } else {
                return idEntity_GetImpactInfo(ent, id, point);
            }
        }

        @Override
        public void ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {
            if (this.af.IsLoaded()) {
                this.af.ApplyImpulse(ent, id, point, impulse);
            }
            if (!this.af.IsActive()) {
                idEntity_ApplyImpulse(ent, id, point, impulse);
            }
        }

        @Override
        public void AddForce(idEntity ent, int id, final idVec3 point, final idVec3 force) {
            if (this.af.IsLoaded()) {
                this.af.AddForce(ent, id, point, force);
            }
            if (!this.af.IsActive()) {
                idEntity_AddForce(ent, id, point, force);
            }
        }

        @Override
        public boolean Collide(final trace_s collision, final idVec3 velocity) {
            float v, f;

            if (this.af.IsActive()) {
                v = -(velocity.oMultiply(collision.c.normal));
                if ((v > BOUNCE_SOUND_MIN_VELOCITY) && (gameLocal.time > this.nextSoundTime)) {
                    f = v > BOUNCE_SOUND_MAX_VELOCITY ? 1.0f : idMath.Sqrt(v - BOUNCE_SOUND_MIN_VELOCITY) * (1.0f / idMath.Sqrt(BOUNCE_SOUND_MAX_VELOCITY - BOUNCE_SOUND_MIN_VELOCITY));
                    if (StartSound("snd_bounce", SND_CHANNEL_ANY, 0, false, null)) {
                        // don't set the volume unless there is a bounce sound as it overrides the entire channel
                        // which causes footsteps on ai's to not honor their shader parms
                        SetSoundVolume(f);
                    }
                    this.nextSoundTime = gameLocal.time + 500;
                }
            }

            return false;
        }

        @Override
        public boolean GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            if (this.af.IsActive()) {
                this.af.GetPhysicsToVisualTransform(origin, axis);
                return true;
            }
            return idEntity_GetPhysicsToVisualTransform(origin, axis);
        }

        @Override
        public boolean UpdateAnimationControllers() {
            if (this.af.IsActive()) {
                if (this.af.UpdateAnimation()) {
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
            final String[] fileName = new String[1];

            if (!this.spawnArgs.GetString("articulatedFigure", "*unknown*", fileName)) {
                return false;
            }

            this.af.SetAnimator(GetAnimator());
            if (!this.af.Load(this, fileName[0])) {
                gameLocal.Error("idAFEntity_Base::LoadAF: Couldn't load af file '%s' on entity '%s'", fileName[0], this.name);
            }

            this.af.Start();

            this.af.GetPhysics().Rotate(this.spawnAxis.ToRotation());
            this.af.GetPhysics().Translate(this.spawnOrigin);

            LoadState(this.spawnArgs);

            this.af.UpdateAnimation();
            this.animator.CreateFrame(gameLocal.time, true);
            UpdateVisuals();

            return true;
        }

        public boolean IsActiveAF() {
            return this.af.IsActive();
        }

        public String GetAFName() {
            return this.af.GetName();
        }

        public idPhysics_AF GetAFPhysics() {
            return this.af.GetPhysics();
        }

        public void SetCombatModel() {
            if (this.combatModel != null) {
                this.combatModel.Unlink();
                this.combatModel.LoadModel(this.modelDefHandle);
            } else {
                this.combatModel = new idClipModel(this.modelDefHandle);
            }
        }

        public idClipModel GetCombatModel() {
            return this.combatModel;
        }

        // contents of combatModel can be set to 0 or re-enabled (mp)
        public void SetCombatContents(boolean enable) {
            assert (this.combatModel != null);
            if (enable && (this.combatModelContents != 0)) {
                assert (0 == this.combatModel.GetContents());
                this.combatModel.SetContents(this.combatModelContents);
                this.combatModelContents = 0;
            } else if (!enable && (this.combatModel.GetContents() != 0)) {
                assert (0 == this.combatModelContents);
                this.combatModelContents = this.combatModel.GetContents();
                this.combatModel.SetContents(0);
            }
        }

        public void LinkCombat() {
            if (this.fl.hidden) {
                return;
            }
            if (this.combatModel != null) {
                this.combatModel.Link(gameLocal.clip, this, 0, this.renderEntity.origin, this.renderEntity.axis, this.modelDefHandle);
            }
        }

        public void UnlinkCombat() {
            if (this.combatModel != null) {
                this.combatModel.Unlink();
            }
        }

        public int BodyForClipModelId(int id) {
            return this.af.BodyForClipModelId(id);
        }

        public void SaveState(idDict args) {
            idKeyValue kv;

            // save the ragdoll pose
            this.af.SaveState(args);

            // save all the bind constraints
            kv = this.spawnArgs.MatchPrefix("bindConstraint ", null);
            while (kv != null) {
                args.Set(kv.GetKey(), kv.GetValue());
                kv = this.spawnArgs.MatchPrefix("bindConstraint ", kv);
            }

            // save the bind if it exists
            kv = this.spawnArgs.FindKey("bind");
            if (kv != null) {
                args.Set(kv.GetKey(), kv.GetValue());
            }
            kv = this.spawnArgs.FindKey("bindToJoint");
            if (kv != null) {
                args.Set(kv.GetKey(), kv.GetValue());
            }
            kv = this.spawnArgs.FindKey("bindToBody");
            if (kv != null) {
                args.Set(kv.GetKey(), kv.GetValue());
            }
        }

        public void LoadState(final idDict args) {
            this.af.LoadState(args);
        }

        public void AddBindConstraints() {
            this.af.AddBindConstraints();
        }

        public void RemoveBindConstraints() {
            this.af.RemoveBindConstraints();
        }

        @Override
        public void ShowEditingDialog() {
            common.InitTool(EDITOR_AF, this.spawnArgs);
        }

        public static void DropAFs(idEntity ent, final String type, idList<idEntity> list) {
            idKeyValue kv;
            String skinName;
            final idEntity[] newEnt = {null};
            idAFEntity_Base af;
            final idDict args = new idDict();
            idDeclSkin skin;

            // drop the articulated figures
            kv = ent.spawnArgs.MatchPrefix(va("def_drop%sAF", type), null);
            while (kv != null) {

                args.Set("classname", kv.GetValue());
                gameLocal.SpawnEntityDef(args, newEnt);

                if ((newEnt[0] != null) && newEnt[0].IsType(idAFEntity_Base.class)) {
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

        protected void Event_SetConstraintPosition(final idEventArg<String> name, final idEventArg<idVec3> pos) {
            this.af.SetConstraintPosition(name.value, pos.value);
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }
    }
    /*
     ===============================================================================

     idAFEntity_Gibbable

     ===============================================================================
     */

    public static class idAFEntity_Gibbable extends idAFEntity_Base {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idAFEntity_Gibbable );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idAFEntity_Base.getEventCallBacks());
            eventCallbacks.put(EV_Gib, (eventCallback_t1<idAFEntity_Gibbable>) idAFEntity_Gibbable::Event_Gib);
            eventCallbacks.put(EV_Gibbed, (eventCallback_t0<idAFEntity_Gibbable>) idAFEntity_Base::Event_Remove);
        }

        protected idRenderModel skeletonModel;
        protected int           skeletonModelDefHandle;
        protected boolean       gibbed;
        //
        //

        public idAFEntity_Gibbable() {
            this.skeletonModel = null;
            this.skeletonModelDefHandle = -1;
            this.gibbed = false;
        }
        // ~idAFEntity_Gibbable( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            InitSkeletonModel();

            this.gibbed = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.gibbed);
            savefile.WriteBool(this.combatModel != null);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final boolean[] hasCombatModel = {false};
            final boolean[] gibbed = {false};

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
            if (0 == (this.thinkFlags & TH_UPDATEVISUALS)) {
                return;
            }

            // update skeleton model
            if (this.gibbed && !IsHidden() && (this.skeletonModel != null)) {
                skeleton = this.renderEntity;
                skeleton.hModel = this.skeletonModel;
                // add to refresh list
                if (this.skeletonModelDefHandle == -1) {
                    this.skeletonModelDefHandle = gameRenderWorld.AddEntityDef(skeleton);
                } else {
                    gameRenderWorld.UpdateEntityDef(this.skeletonModelDefHandle, skeleton);
                }
            }

            idEntity_Present();
        }

        @Override
        public void Damage(idEntity inflictor, idEntity attacker, final idVec3 dir, final String damageDefName, final float damageScale, final int location) {
            if (!this.fl.takedamage) {
                return;
            }
            super.Damage(inflictor, attacker, dir, damageDefName, damageScale, location);
            if ((this.health < -20) && this.spawnArgs.GetBool("gib")) {
                Gib(dir, damageDefName);
            }
        }

        public void SpawnGibs(idVec3 dir, final String damageDefName) {
            int i;
            boolean gibNonSolid;
            idVec3 entityCenter, velocity;
            final idList<idEntity> list = new idList<>();

            assert (!gameLocal.isClient);

            final idDict damageDef = gameLocal.FindEntityDefDict(damageDefName);
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
            if (this.gibbed) {
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
                    this.renderEntity.noShadow = true;
                    this.renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = gameLocal.time * 0.001f;
                    StartSound("snd_gibbed", SND_CHANNEL_ANY, 0, false, null);
                    this.gibbed = true;
                }
            } else {
                this.gibbed = true;
            }

            PostEventSec(EV_Gibbed, 4.0f);
        }

        protected void InitSkeletonModel() {
            String modelName;
            idDeclModelDef modelDef;

            this.skeletonModel = null;
            this.skeletonModelDefHandle = -1;

            modelName = this.spawnArgs.GetString("model_gib");

            if (!modelName.isEmpty()) {//[0] != '\0' ) {
                modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, modelName, false);
                if (modelDef != null) {
                    this.skeletonModel = modelDef.ModelHandle();
                } else {
                    this.skeletonModel = renderModelManager.FindModel(modelName);
                }
                if ((this.skeletonModel != null) && (this.renderEntity.hModel != null)) {
                    if (this.skeletonModel.NumJoints() != this.renderEntity.hModel.NumJoints()) {
                        gameLocal.Error("gib model '%s' has different number of joints than model '%s'",
                                this.skeletonModel.Name(), this.renderEntity.hModel.Name());
                    }
                }
            }
        }

        protected void Event_Gib(final idEventArg<String> damageDefName) {
            Gib(new idVec3(0, 0, 1), damageDefName.value);
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

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

        @Override
        protected void _deconstructor() {
            if (this.skeletonModelDefHandle != -1) {
                gameRenderWorld.FreeEntityDef(this.skeletonModelDefHandle);
                this.skeletonModelDefHandle = -1;
            }

            super._deconstructor();
        }
    }

    /*
     ===============================================================================

     idAFEntity_Generic

     ===============================================================================
     */
    public static class idAFEntity_Generic extends idAFEntity_Gibbable {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idAFEntity_Generic );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idAFEntity_Gibbable.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idAFEntity_Generic>) idAFEntity_Generic::Event_Activate);
        }

        private final boolean[] keepRunningPhysics = {false};
        //
        //

        public idAFEntity_Generic() {
            this.keepRunningPhysics[0] = false;
        }
        // ~idAFEntity_Generic( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            if (!LoadAF()) {
                gameLocal.Error("Couldn't load af file on entity '%s'", this.name);
            }

            SetCombatModel();

            SetPhysics(this.af.GetPhysics());

            this.af.GetPhysics().PutToRest();
            if (!this.spawnArgs.GetBool("nodrop", "0")) {
                this.af.GetPhysics().Activate();
            }

            this.fl.takedamage = true;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteBool(this.keepRunningPhysics[0]);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadBool(this.keepRunningPhysics);
        }

        @Override
        public void Think() {
            idAFEntity_Base_Think();

            if (this.keepRunningPhysics[0]) {
                BecomeActive(TH_PHYSICS);
            }
        }

        public void KeepRunningPhysics() {
            this.keepRunningPhysics[0] = true;
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            float delay;
            final idVec3 init_velocity = new idVec3(), init_avelocity = new idVec3();

            Show();

            this.af.GetPhysics().EnableImpact();
            this.af.GetPhysics().Activate();

            this.spawnArgs.GetVector("init_velocity", "0 0 0", init_velocity);
            this.spawnArgs.GetVector("init_avelocity", "0 0 0", init_avelocity);

            delay = this.spawnArgs.GetFloat("init_velocityDelay", "0");
            if (delay == 0) {
                this.af.GetPhysics().SetLinearVelocity(init_velocity);
            } else {
                PostEventMS(EV_SetLinearVelocity, delay, init_velocity);
            }

            delay = this.spawnArgs.GetFloat("init_avelocityDelay", "0");
            if (delay == 0) {
                this.af.GetPhysics().SetAngularVelocity(init_avelocity);
            } else {
                PostEventSec(EV_SetAngularVelocity, delay, init_avelocity);
            }
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }

    /*
     ===============================================================================

     idAFEntity_WithAttachedHead

     ===============================================================================
     */
    public static class idAFEntity_WithAttachedHead extends idAFEntity_Gibbable {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idAFEntity_WithAttachedHead );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idAFEntity_Gibbable.getEventCallBacks());
            eventCallbacks.put(EV_Gib, (eventCallback_t1<idAFEntity_WithAttachedHead>) idAFEntity_WithAttachedHead::Event_Gib);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idAFEntity_WithAttachedHead>) idAFEntity_WithAttachedHead::Event_Activate);
        }

        private final idEntityPtr<idAFAttachment> head;
        //
        //

        public idAFEntity_WithAttachedHead() {
            this.head = new idEntityPtr<>(null);
        }

        // ~idAFEntity_WithAttachedHead();
        @Override
        protected void _deconstructor() {
            if (this.head.GetEntity() != null) {
                this.head.GetEntity().ClearBody();
                this.head.GetEntity().PostEventMS(EV_Remove, 0);
            }
            super._deconstructor();
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            SetupHead();

            LoadAF();

            SetCombatModel();

            SetPhysics(this.af.GetPhysics());

            this.af.GetPhysics().PutToRest();
            if (!this.spawnArgs.GetBool("nodrop", "0")) {
                this.af.GetPhysics().Activate();
            }

            this.fl.takedamage = true;

            if (this.head.GetEntity() != null) {
                final int anim = this.head.GetEntity().GetAnimator().GetAnim("dead");

                if (anim != 0) {
                    this.head.GetEntity().GetAnimator().SetFrame(ANIMCHANNEL_ALL, anim, 0, gameLocal.time, 0);
                }
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            this.head.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.head.Restore(savefile);
        }

        public void SetupHead() {
            idAFAttachment headEnt;
            String jointName;
            final String headModel;
            int/*jointHandle_t*/ joint;
            idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();

            headModel = this.spawnArgs.GetString("def_head", "");
            if (!headModel.isEmpty()) {//[ 0 ] ) {
                jointName = this.spawnArgs.GetString("head_joint");
                joint = this.animator.GetJointHandle(jointName);
                if (joint == INVALID_JOINT) {
                    gameLocal.Error("Joint '%s' not found for 'head_joint' on '%s'", jointName, this.name.getData());
                }

                headEnt = (idAFAttachment) gameLocal.SpawnEntityType(idAFAttachment.class, null);
                headEnt.SetName(va("%s_head", this.name));
                headEnt.SetBody(this, headModel, joint);
                headEnt.SetCombatModel();
                this.head.oSet(headEnt);

                this.animator.GetJointTransform(joint, gameLocal.time, origin, axis);
                origin = this.renderEntity.origin.oPlus(origin.oMultiply(this.renderEntity.axis));
                headEnt.SetOrigin(origin);
                headEnt.SetAxis(this.renderEntity.axis);
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
            if (this.head.GetEntity() != null) {
                this.head.GetEntity().Hide();
            }
            UnlinkCombat();
        }

        @Override
        public void Show() {
            idAFEntity_Base_Show();
            if (this.head.GetEntity() != null) {
                this.head.GetEntity().Show();
            }
            LinkCombat();
        }

        @Override
        public void ProjectOverlay(final idVec3 origin, final idVec3 dir, float size, final String material) {

            idEntity_ProjectOverlay(origin, dir, size, material);

            if (this.head.GetEntity() != null) {
                this.head.GetEntity().ProjectOverlay(origin, dir, size, material);
            }
        }

        @Override
        public void LinkCombat() {
            idAFAttachment headEnt;

            if (this.fl.hidden) {
                return;
            }

            if (this.combatModel != null) {
                this.combatModel.Link(gameLocal.clip, this, 0, this.renderEntity.origin, this.renderEntity.axis, this.modelDefHandle);
            }
            headEnt = this.head.GetEntity();
            if (headEnt != null) {
                headEnt.LinkCombat();
            }
        }

        @Override
        public void UnlinkCombat() {
            idAFAttachment headEnt;

            if (this.combatModel != null) {
                this.combatModel.Unlink();
            }
            headEnt = this.head.GetEntity();
            if (headEnt != null) {
                headEnt.UnlinkCombat();
            }
        }

        @Override
        protected void Gib(final idVec3 dir, final String damageDefName) {
            // only gib once
            if (this.gibbed) {
                return;
            }
            super.Gib(dir, damageDefName);
            if (this.head.GetEntity() != null) {
                this.head.GetEntity().Hide();
            }
        }

        @Override
        protected void Event_Gib(final idEventArg<String> damageDefName) {
            Gib(new idVec3(0, 0, 1), damageDefName.value);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            float delay;
            final idVec3 init_velocity = new idVec3(), init_avelocity = new idVec3();

            Show();

            this.af.GetPhysics().EnableImpact();
            this.af.GetPhysics().Activate();

            this.spawnArgs.GetVector("init_velocity", "0 0 0", init_velocity);
            this.spawnArgs.GetVector("init_avelocity", "0 0 0", init_avelocity);

            delay = this.spawnArgs.GetFloat("init_velocityDelay", "0");
            if (delay == 0) {
                this.af.GetPhysics().SetLinearVelocity(init_velocity);
            } else {
                PostEventSec(EV_SetLinearVelocity, delay, init_velocity);
            }

            delay = this.spawnArgs.GetFloat("init_avelocityDelay", "0");
            if (delay == 0) {
                this.af.GetPhysics().SetAngularVelocity(init_avelocity);
            } else {
                PostEventSec(EV_SetAngularVelocity, delay, init_avelocity);
            }
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }

    /*
     ===============================================================================

     idAFEntity_Vehicle

     ===============================================================================
     */
    public static class idAFEntity_Vehicle extends idAFEntity_Base {
        // CLASS_PROTOTYPE( idAFEntity_Vehicle );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
            this.player = null;
            this.eyesJoint = INVALID_JOINT;
            this.steeringWheelJoint = INVALID_JOINT;
            this.wheelRadius = 0;
            this.steerAngle = 0;
            this.steerSpeed = 0;
            this.dustSmoke = null;
        }

        @Override
        public void Spawn() {
            final String eyesJointName = this.spawnArgs.GetString("eyesJoint", "eyes");
            final String steeringWheelJointName = this.spawnArgs.GetString("steeringWheelJoint", "steeringWheel");
            final float[] wheel = new float[1], steer = new float[1];

            LoadAF();

            SetCombatModel();

            SetPhysics(this.af.GetPhysics());

            this.fl.takedamage = true;

//	if ( !eyesJointName[0] ) {
            if (eyesJointName.isEmpty()) {
                gameLocal.Error("idAFEntity_Vehicle '%s' no eyes joint specified", this.name);
            }
            this.eyesJoint = this.animator.GetJointHandle(eyesJointName);
//	if ( !steeringWheelJointName[0] ) {
            if (steeringWheelJointName.isEmpty()) {
                gameLocal.Error("idAFEntity_Vehicle '%s' no steering wheel joint specified", this.name);
            }
            this.steeringWheelJoint = this.animator.GetJointHandle(steeringWheelJointName);

            this.spawnArgs.GetFloat("wheelRadius", "20", wheel);
            this.spawnArgs.GetFloat("steerSpeed", "5", steer);
            this.wheelRadius = wheel[0];
            this.steerSpeed = steer[0];

            this.player = null;
            this.steerAngle = 0;

            final String smokeName = this.spawnArgs.GetString("smoke_vehicle_dust", "muzzlesmoke");
            if (!smokeName.isEmpty()) {// != '\0' ) {
                this.dustSmoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
            }
        }

        public void Use(idPlayer other) {
            idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();

            if (this.player != null) {
                if (this.player.equals(other)) {
                    other.Unbind();
                    this.player = null;

                    this.af.GetPhysics().SetComeToRest(true);
                }
            } else {
                this.player = other;
                this.animator.GetJointTransform(this.eyesJoint, gameLocal.time, origin, axis);
                origin = this.renderEntity.origin.oPlus(origin.oMultiply(this.renderEntity.axis));
                this.player.GetPhysics().SetOrigin(origin);
                this.player.BindToBody(this, 0, true);

                this.af.GetPhysics().SetComeToRest(false);
                this.af.GetPhysics().Activate();
            }
        }

        protected float GetSteerAngle() {
            final float idealSteerAngle, angleDelta;

            idealSteerAngle = this.player.usercmd.rightmove * (30 / 128.0f);
            angleDelta = idealSteerAngle - this.steerAngle;

            if (angleDelta > this.steerSpeed) {
                this.steerAngle += this.steerSpeed;
            } else if (angleDelta < -this.steerSpeed) {
                this.steerAngle -= this.steerSpeed;
            } else {
                this.steerAngle = idealSteerAngle;
            }

            return this.steerAngle;
        }
    }

    /*
     ===============================================================================
     idAFEntity_VehicleSimple
     ===============================================================================
     */
    public static class idAFEntity_VehicleSimple extends idAFEntity_Vehicle {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
                this.suspension[i] = null;
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
            final idMat3 axis = new idMat3();
            final idTraceModel trm = new idTraceModel();

            trm.SetupPolygon(wheelPoly, 4);
            trm.Translate(new idVec3(0, 0, -this.wheelRadius));
            this.wheelModel = new idClipModel(trm);

            for (i = 0; i < 4; i++) {
                final String wheelJointName = this.spawnArgs.GetString(wheelJointKeys[i], "");
//		if ( !wheelJointName[0] ) {
                if (wheelJointName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleSimple '%s' no '%s' specified", this.name, wheelJointKeys[i]);
                }
                this.wheelJoints[i] = this.animator.GetJointHandle(wheelJointName);
                if (this.wheelJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idAFEntity_VehicleSimple '%s' can't find wheel joint '%s'", this.name, wheelJointName);
                }

                GetAnimator().GetJointTransform(this.wheelJoints[i], 0, origin, axis);
                origin = this.renderEntity.origin.oPlus(origin.oMultiply(this.renderEntity.axis));

                this.suspension[i] = new idAFConstraint_Suspension();
                this.suspension[i].Setup(va("suspension%d", i), this.af.GetPhysics().GetBody(0), origin, this.af.GetPhysics().GetAxis(0), this.wheelModel);
                this.suspension[i].SetSuspension(
                        g_vehicleSuspensionUp.GetFloat(),
                        g_vehicleSuspensionDown.GetFloat(),
                        g_vehicleSuspensionKCompress.GetFloat(),
                        g_vehicleSuspensionDamping.GetFloat(),
                        g_vehicleTireFriction.GetFloat());

                this.af.GetPhysics().AddConstraint(this.suspension[i]);
            }

//            memset(wheelAngles, 0, sizeof(wheelAngles));
            Arrays.fill(this.wheelAngles, 0);
            BecomeActive(TH_THINK);
        }

        @Override
        public void Think() {
            int i;
            float force = 0, velocity = 0, steerAngle = 0;
            idVec3 origin;
            final idMat3 axis = new idMat3();
            final idRotation wheelRotation = new idRotation(), steerRotation = new idRotation();

            if ((this.thinkFlags & TH_THINK) != 0) {

                if (this.player != null) {
                    // capture the input from a player
                    velocity = g_vehicleVelocity.GetFloat();
                    if (this.player.usercmd.forwardmove < 0) {
                        velocity = -velocity;
                    }
                    force = idMath.Fabs(this.player.usercmd.forwardmove * g_vehicleForce.GetFloat()) * (1.0f / 128.0f);
                    steerAngle = GetSteerAngle();
                }

                // update the wheel motor force and steering
                for (i = 0; i < 2; i++) {

                    // front wheel drive
                    if (velocity != 0) {
                        this.suspension[i].EnableMotor(true);
                    } else {
                        this.suspension[i].EnableMotor(false);
                    }
                    this.suspension[i].SetMotorVelocity(velocity);
                    this.suspension[i].SetMotorForce(force);

                    // update the wheel steering
                    this.suspension[i].SetSteerAngle(steerAngle);
                }

                // adjust wheel velocity for better steering because there are no differentials between the wheels
                if (steerAngle < 0) {
                    this.suspension[0].SetMotorVelocity(velocity * 0.5f);
                } else if (steerAngle > 0) {
                    this.suspension[1].SetMotorVelocity(velocity * 0.5f);
                }

                // update suspension with latest cvar settings
                for (i = 0; i < 4; i++) {
                    this.suspension[i].SetSuspension(
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
                    final idAFBody body = this.af.GetPhysics().GetBody(0);

                    origin = this.suspension[i].GetWheelOrigin();
                    velocity = body.GetPointVelocity(origin).oMultiply(body.GetWorldAxis().oGet(0));
                    this.wheelAngles[i] += (velocity * MS2SEC(gameLocal.msec)) / this.wheelRadius;

                    // additional rotation about the wheel axis
                    wheelRotation.SetAngle(RAD2DEG(this.wheelAngles[i]));
                    wheelRotation.SetVec(0, -1, 0);

                    if (i < 2) {
                        // rotate the wheel for steering
                        steerRotation.SetAngle(steerAngle);
                        steerRotation.SetVec(0, 0, 1);
                        // set wheel rotation
                        this.animator.SetJointAxis(this.wheelJoints[i], JOINTMOD_WORLD, wheelRotation.ToMat3().oMultiply(steerRotation.ToMat3()));
                    } else {
                        // set wheel rotation
                        this.animator.SetJointAxis(this.wheelJoints[i], JOINTMOD_WORLD, wheelRotation.ToMat3());
                    }

                    // set wheel position for suspension
                    origin = (origin.oMinus(this.renderEntity.origin)).oMultiply(this.renderEntity.axis.Transpose());
                    GetAnimator().SetJointPos(this.wheelJoints[i], JOINTMOD_WORLD_OVERRIDE, origin);
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
            if ((this.thinkFlags & TH_UPDATEVISUALS) != 0) {
                Present();
                LinkCombat();
            }
        }

    }

    /*
     ===============================================================================
     idAFEntity_VehicleFourWheels
     ===============================================================================
     */
    public static class idAFEntity_VehicleFourWheels extends idAFEntity_Vehicle {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
                this.wheels[i] = null;
                this.wheelJoints[i] = INVALID_JOINT;
                this.wheelAngles[i] = 0;
            }
            this.steering[0] = null;
            this.steering[1] = null;
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
                wheelBodyName = this.spawnArgs.GetString(wheelBodyKeys[i], "");
//		if ( !wheelBodyName[0] ) {
                if (wheelBodyName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' no '%s' specified", this.name, wheelBodyKeys[i]);
                }
                this.wheels[i] = this.af.GetPhysics().GetBody(wheelBodyName);
                if (null == this.wheels[i]) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' can't find wheel body '%s'", this.name, wheelBodyName);
                }
                wheelJointName = this.spawnArgs.GetString(wheelJointKeys[i], "");
//		if ( !wheelJointName[0] ) {
                if (wheelJointName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' no '%s' specified", this.name, wheelJointKeys[i]);
                }
                this.wheelJoints[i] = this.animator.GetJointHandle(wheelJointName);
                if (this.wheelJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' can't find wheel joint '%s'", this.name, wheelJointName);
                }
            }

            for (i = 0; i < 2; i++) {
                steeringHingeName = this.spawnArgs.GetString(steeringHingeKeys[i], "");
//		if ( !steeringHingeName[0] ) {
                if (steeringHingeName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s' no '%s' specified", this.name, steeringHingeKeys[i]);
                }
                this.steering[i] = (idAFConstraint_Hinge) this.af.GetPhysics().GetConstraint(steeringHingeName);
                if (NOT(this.steering[i])) {
                    gameLocal.Error("idAFEntity_VehicleFourWheels '%s': can't find steering hinge '%s'", this.name, steeringHingeName);
                }
            }

//	memset( wheelAngles, 0, sizeof( wheelAngles ) );
            Arrays.fill(this.wheelAngles, 0);
            BecomeActive(TH_THINK);
        }

        @Override
        public void Think() {
            int i;
            float force = 0, velocity = 0, steerAngle = 0;
            final idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();
            final idRotation rotation = new idRotation();

            if ((this.thinkFlags & TH_THINK) != 0) {

                if (this.player != null) {
                    // capture the input from a player
                    velocity = g_vehicleVelocity.GetFloat();
                    if (this.player.usercmd.forwardmove < 0) {
                        velocity = -velocity;
                    }
                    force = idMath.Fabs(this.player.usercmd.forwardmove * g_vehicleForce.GetFloat()) * (1.0f / 128.0f);
                    steerAngle = GetSteerAngle();
                }

                // update the wheel motor force
                for (i = 0; i < 2; i++) {
                    this.wheels[2 + i].SetContactMotorVelocity(velocity);
                    this.wheels[2 + i].SetContactMotorForce(force);
                }

                // adjust wheel velocity for better steering because there are no differentials between the wheels
                if (steerAngle < 0) {
                    this.wheels[2].SetContactMotorVelocity(velocity * 0.5f);
                } else if (steerAngle > 0) {
                    this.wheels[3].SetContactMotorVelocity(velocity * 0.5f);
                }

                // update the wheel steering
                this.steering[0].SetSteerAngle(steerAngle);
                this.steering[1].SetSteerAngle(steerAngle);
                for (i = 0; i < 2; i++) {
                    this.steering[i].SetSteerSpeed(3.0f);
                }

                // update the steering wheel
                this.animator.GetJointTransform(this.steeringWheelJoint, gameLocal.time, origin, axis);
                rotation.SetVec(axis.oGet(2));
                rotation.SetAngle(-steerAngle);
                this.animator.SetJointAxis(this.steeringWheelJoint, JOINTMOD_WORLD, rotation.ToMat3());

                // run the physics
                RunPhysics();

                // rotate the wheels visually
                for (i = 0; i < 4; i++) {
                    if (force == 0) {
                        velocity = this.wheels[i].GetLinearVelocity().oMultiply(this.wheels[i].GetWorldAxis().oGet(0));
                    }
                    this.wheelAngles[i] += (velocity * MS2SEC(gameLocal.msec)) / this.wheelRadius;
                    // give the wheel joint an additional rotation about the wheel axis
                    rotation.SetAngle(RAD2DEG(this.wheelAngles[i]));
                    axis = this.af.GetPhysics().GetAxis(0);
                    rotation.SetVec((this.wheels[i].GetWorldAxis().oMultiply(axis.Transpose())).oGet(2));
                    this.animator.SetJointAxis(this.wheelJoints[i], JOINTMOD_WORLD, rotation.ToMat3());
                }

                // spawn dust particle effects
                if ((force != 0) && (0 == (gameLocal.framenum & 7))) {
                    int numContacts;
                    final idAFConstraint_Contact[] contacts = new idAFConstraint_Contact[2];
                    for (i = 0; i < 4; i++) {
                        numContacts = this.af.GetPhysics().GetBodyContactConstraints(this.wheels[i].GetClipModel().GetId(), contacts, 2);
                        for (int j = 0; j < numContacts; j++) {
                            gameLocal.smokeParticles.EmitSmoke(this.dustSmoke, gameLocal.time, gameLocal.random.RandomFloat(), contacts[j].GetContact().point, contacts[j].GetContact().normal.ToMat3());
                        }
                    }
                }
            }

            UpdateAnimation();
            if ((this.thinkFlags & TH_UPDATEVISUALS) != 0) {
                Present();
                LinkCombat();
            }
        }
    }

    /*
     ===============================================================================
     idAFEntity_VehicleSixWheels
     ===============================================================================
     */
    public static class idAFEntity_VehicleSixWheels extends idAFEntity_Vehicle {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
                this.wheels[i] = null;
                this.wheelJoints[i] = INVALID_JOINT;
                this.wheelAngles[i] = 0;
            }
            this.steering[0] = null;
            this.steering[1] = null;
            this.steering[2] = null;
            this.steering[3] = null;
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
                wheelBodyName = this.spawnArgs.GetString(wheelBodyKeys[i], "");
//		if ( !wheelBodyName[0] ) {
                if (wheelBodyName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' no '%s' specified", this.name, wheelBodyKeys[i]);
                }
                this.wheels[i] = this.af.GetPhysics().GetBody(wheelBodyName);
                if (NOT(this.wheels[i])) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' can't find wheel body '%s'", this.name, wheelBodyName);
                }
                wheelJointName = this.spawnArgs.GetString(wheelJointKeys[i], "");
//		if ( !wheelJointName[0] ) {
                if (wheelJointName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' no '%s' specified", this.name, wheelJointKeys[i]);
                }
                this.wheelJoints[i] = this.animator.GetJointHandle(wheelJointName);
                if (this.wheelJoints[i] == INVALID_JOINT) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' can't find wheel joint '%s'", this.name, wheelJointName);
                }
            }

            for (i = 0; i < 4; i++) {
                steeringHingeName = this.spawnArgs.GetString(steeringHingeKeys[i], "");
//		if ( !steeringHingeName[0] ) {
                if (steeringHingeName.isEmpty()) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s' no '%s' specified", this.name, steeringHingeKeys[i]);
                }
                this.steering[i] = (idAFConstraint_Hinge) this.af.GetPhysics().GetConstraint(steeringHingeName);
                if (NOT(this.steering[i])) {
                    gameLocal.Error("idAFEntity_VehicleSixWheels '%s': can't find steering hinge '%s'", this.name, steeringHingeName);
                }
            }

//	memset( wheelAngles, 0, sizeof( wheelAngles ) );
            Arrays.fill(this.wheelAngles, 0);
            BecomeActive(TH_THINK);
        }

        @Override
        public void Think() {
            int i;
            float force = 0, velocity = 0, steerAngle = 0;
            final idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();
            final idRotation rotation = new idRotation();

            if ((this.thinkFlags & TH_THINK) != 0) {

                if (this.player != null) {
                    // capture the input from a player
                    velocity = g_vehicleVelocity.GetFloat();
                    if (this.player.usercmd.forwardmove < 0) {
                        velocity = -velocity;
                    }
                    force = idMath.Fabs(this.player.usercmd.forwardmove * g_vehicleForce.GetFloat()) * (1.0f / 128.0f);
                    steerAngle = GetSteerAngle();
                }

                // update the wheel motor force
                for (i = 0; i < 6; i++) {
                    this.wheels[i].SetContactMotorVelocity(velocity);
                    this.wheels[i].SetContactMotorForce(force);
                }

                // adjust wheel velocity for better steering because there are no differentials between the wheels
                if (steerAngle < 0) {
                    for (i = 0; i < 3; i++) {
                        this.wheels[(i << 1)].SetContactMotorVelocity(velocity * 0.5f);
                    }
                } else if (steerAngle > 0) {
                    for (i = 0; i < 3; i++) {
                        this.wheels[1 + (i << 1)].SetContactMotorVelocity(velocity * 0.5f);
                    }
                }

                // update the wheel steering
                this.steering[0].SetSteerAngle(steerAngle);
                this.steering[1].SetSteerAngle(steerAngle);
                this.steering[2].SetSteerAngle(-steerAngle);
                this.steering[3].SetSteerAngle(-steerAngle);
                for (i = 0; i < 4; i++) {
                    this.steering[i].SetSteerSpeed(3.0f);
                }

                // update the steering wheel
                this.animator.GetJointTransform(this.steeringWheelJoint, gameLocal.time, origin, axis);
                rotation.SetVec(axis.oGet(2));
                rotation.SetAngle(-steerAngle);
                this.animator.SetJointAxis(this.steeringWheelJoint, JOINTMOD_WORLD, rotation.ToMat3());

                // run the physics
                RunPhysics();

                // rotate the wheels visually
                for (i = 0; i < 6; i++) {
                    if (force == 0) {
                        velocity = this.wheels[i].GetLinearVelocity().oMultiply(this.wheels[i].GetWorldAxis().oGet(0));
                    }
                    this.wheelAngles[i] += (velocity * MS2SEC(gameLocal.msec)) / this.wheelRadius;
                    // give the wheel joint an additional rotation about the wheel axis
                    rotation.SetAngle(RAD2DEG(this.wheelAngles[i]));
                    axis = this.af.GetPhysics().GetAxis(0);
                    rotation.SetVec((this.wheels[i].GetWorldAxis().oMultiply(axis.Transpose()).oGet(2)));
                    this.animator.SetJointAxis(this.wheelJoints[i], JOINTMOD_WORLD, rotation.ToMat3());
                }

                // spawn dust particle effects
                if ((force != 0) && (0 == (gameLocal.framenum & 7))) {
                    int numContacts;
                    final idAFConstraint_Contact[] contacts = new idAFConstraint_Contact[2];
                    for (i = 0; i < 6; i++) {
                        numContacts = this.af.GetPhysics().GetBodyContactConstraints(this.wheels[i].GetClipModel().GetId(), contacts, 2);
                        for (int j = 0; j < numContacts; j++) {
                            gameLocal.smokeParticles.EmitSmoke(this.dustSmoke, gameLocal.time, gameLocal.random.RandomFloat(), contacts[j].GetContact().point, contacts[j].GetContact().normal.ToMat3());
                        }
                    }
                }
            }

            UpdateAnimation();
            if ((this.thinkFlags & TH_UPDATEVISUALS) != 0) {
                Present();
                LinkCombat();
            }
        }
    }

    /*
     ===============================================================================
     idAFEntity_SteamPipe
     ===============================================================================
     */
    public static class idAFEntity_SteamPipe extends idAFEntity_Base {
        // CLASS_PROTOTYPE( idAFEntity_SteamPipe );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int steamBody;
        private float steamForce;
        private float steamUpForce;
        private idForce_Constant force;
        private renderEntity_s steamRenderEntity;
        private int/*qhandle_t*/ steamModelDefHandle;
        //
        //

        public idAFEntity_SteamPipe() {
            this.steamBody = 0;
            this.steamForce = 0;
            this.steamUpForce = 0;
            this.steamModelDefHandle = -1;
//	memset( &steamRenderEntity, 0, sizeof( steamRenderEntity ) );
            this.steamRenderEntity = new renderEntity_s();
        }
        // ~idAFEntity_SteamPipe();

        @Override
        public void Spawn() {
            idVec3 steamDir;
            final String steamBodyName;

            LoadAF();

            SetCombatModel();

            SetPhysics(this.af.GetPhysics());

            this.fl.takedamage = true;

            steamBodyName = this.spawnArgs.GetString("steamBody", "");
            this.steamForce = this.spawnArgs.GetFloat("steamForce", "2000");
            this.steamUpForce = this.spawnArgs.GetFloat("steamUpForce", "10");
            steamDir = this.af.GetPhysics().GetAxis(this.steamBody).oGet(2);//[2];
            this.steamBody = this.af.GetPhysics().GetBodyId(steamBodyName);
            this.force.SetPosition(this.af.GetPhysics(), this.steamBody, this.af.GetPhysics().GetOrigin(this.steamBody));
            this.force.SetForce(steamDir.oMultiply(-this.steamForce));

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
            final idVec3 steamDir = new idVec3();

            if ((this.thinkFlags & TH_THINK) != 0) {
                steamDir.x = gameLocal.random.CRandomFloat() * this.steamForce;
                steamDir.y = gameLocal.random.CRandomFloat() * this.steamForce;
                steamDir.z = this.steamUpForce;
                this.force.SetForce(steamDir);
                this.force.Evaluate(gameLocal.time);
                //gameRenderWorld.DebugArrow( colorWhite, af.GetPhysics().GetOrigin( steamBody ), af.GetPhysics().GetOrigin( steamBody ) - 10 * steamDir, 4 );
            }

            if (this.steamModelDefHandle >= 0) {
                this.steamRenderEntity.origin.oSet(this.af.GetPhysics().GetOrigin(this.steamBody));
                this.steamRenderEntity.axis.oSet(this.af.GetPhysics().GetAxis(this.steamBody));
                gameRenderWorld.UpdateEntityDef(this.steamModelDefHandle, this.steamRenderEntity);
            }

            super.Think();
        }

        private void InitSteamRenderEntity() {
            final String temp;
            idDeclModelDef modelDef;

//	memset( steamRenderEntity, 0, sizeof( steamRenderEntity ) );
            this.steamRenderEntity = new renderEntity_s();
            this.steamRenderEntity.shaderParms[SHADERPARM_RED] = 1.0f;
            this.steamRenderEntity.shaderParms[SHADERPARM_GREEN] = 1.0f;
            this.steamRenderEntity.shaderParms[SHADERPARM_BLUE] = 1.0f;
//            modelDef = null;
            temp = this.spawnArgs.GetString("model_steam");
            if (!temp.isEmpty()) {// != '\0' ) {
//		if ( !strstr( temp, "." ) ) {
                if (!temp.contains(".")) {
                    modelDef = (idDeclModelDef) declManager.FindType(DECL_MODELDEF, temp, false);
                    if (modelDef != null) {
                        this.steamRenderEntity.hModel = modelDef.ModelHandle();
                    }
                }

                if (null == this.steamRenderEntity.hModel) {
                    this.steamRenderEntity.hModel = renderModelManager.FindModel(temp);
                }

                if (this.steamRenderEntity.hModel != null) {
                    this.steamRenderEntity.bounds.oSet(this.steamRenderEntity.hModel.Bounds(this.steamRenderEntity));
                } else {
                    this.steamRenderEntity.bounds.Zero();
                }
                this.steamRenderEntity.origin.oSet(this.af.GetPhysics().GetOrigin(this.steamBody));
                this.steamRenderEntity.axis.oSet(this.af.GetPhysics().GetAxis(this.steamBody));
                this.steamModelDefHandle = gameRenderWorld.AddEntityDef(this.steamRenderEntity);
            }
        }
    }

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
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// public:
        // CLASS_PROTOTYPE( idAFEntity_ClawFourFingers );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idAFEntity_Base.getEventCallBacks());
            eventCallbacks.put(EV_SetFingerAngle, (eventCallback_t1<idAFEntity_ClawFourFingers>) idAFEntity_ClawFourFingers::Event_SetFingerAngle);
            eventCallbacks.put(EV_StopFingers, (eventCallback_t0<idAFEntity_ClawFourFingers>) idAFEntity_ClawFourFingers::Event_StopFingers);
        }

        public idAFEntity_ClawFourFingers() {
            this.fingers[0] = null;
            this.fingers[1] = null;
            this.fingers[2] = null;
            this.fingers[3] = null;
        }

        @Override
        public void Spawn() {
            int i;

            LoadAF();

            SetCombatModel();

            this.af.GetPhysics().LockWorldConstraints(true);
            this.af.GetPhysics().SetForcePushable(true);
            SetPhysics(this.af.GetPhysics());

            this.fl.takedamage = true;

            for (i = 0; i < 4; i++) {
                this.fingers[i] = (idAFConstraint_Hinge) this.af.GetPhysics().GetConstraint(clawConstraintNames[i]);
                if (NOT(this.fingers[i])) {
                    gameLocal.Error("idClaw_FourFingers '%s': can't find claw constraint '%s'", this.name, clawConstraintNames[i]);
                }
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            for (i = 0; i < 4; i++) {
                this.fingers[i].Save(savefile);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;

            for (i = 0; i < 4; i++) {
                this.fingers[i] = (idAFConstraint_Hinge) this.af.GetPhysics().GetConstraint(clawConstraintNames[i]);
                this.fingers[i].Restore(savefile);
            }

            SetCombatModel();
            LinkCombat();
        }
        //
        //
        private final idAFConstraint_Hinge[] fingers = new idAFConstraint_Hinge[4];
        //
        //

        private void Event_SetFingerAngle(idEventArg<Float> angle) {
            int i;

            for (i = 0; i < 4; i++) {
                this.fingers[i].SetSteerAngle(angle.value);
                this.fingers[i].SetSteerSpeed(0.5f);
            }
            this.af.GetPhysics().Activate();
        }

        private void Event_StopFingers() {
            int i;

            for (i = 0; i < 4; i++) {
                this.fingers[i].SetSteerAngle(this.fingers[i].GetAngle());
            }
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }

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
    }

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
            final jointTransformData_t data = (jointTransformData_t) model;

            for (i = 0; i < data.ent.numJoints; i++) {
                if (data.joints[i].name.Icmp(jointName) == 0) {
                    break;
                }
            }
            if (i >= data.ent.numJoints) {
                return false;
            }
            origin.oSet(frame[i].ToVec3());
            axis.oSet(frame[i].ToMat3());
            return true;
        }
    }

    /*
     ================
     GetArgString
     ================
     */
    public static String GetArgString(final idDict args, final idDict defArgs, final String key) {
        String s;

        s = args.GetString(key);
//	if ( !s[0] && defArgs ) {
        if (s.isEmpty() && (defArgs != null)) {
            s = defArgs.GetString(key);
        }
        return s;
    }
}
