package neo.Game;

import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import static neo.Game.Animation.Anim.ANIMCHANNEL_ALL;
import static neo.Game.Animation.Anim.ANIMCHANNEL_EYELIDS;
import static neo.Game.Animation.Anim.ANIMCHANNEL_HEAD;
import static neo.Game.Animation.Anim.ANIMCHANNEL_LEGS;
import static neo.Game.Animation.Anim.ANIMCHANNEL_TORSO;
import static neo.Game.Animation.Anim.FRAME2MS;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_LOCAL_OVERRIDE;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD_OVERRIDE;
import static neo.Game.Entity.EV_StopSound;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.ai_debugScript;
import static neo.Game.GameSys.SysCvar.g_debugDamage;
import static neo.Game.Game_local.MASK_OPAQUE;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_VOICE;
import static neo.Game.IK.IK_ANIM;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_FLESH;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.TempDump.NOT;
import static neo.TempDump.atof;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_WALK;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Vector.getVec3_origin;

import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.AFEntity.idAFEntity_Gibbable;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.IK.idIK_Walk;
import neo.Game.Item.idMoveableItem;
import neo.Game.Light.idLight;
import neo.Game.Projectile.idSoulCubeMissile;
import neo.Game.AI.AAS.idAAS;
import neo.Game.Animation.Anim.animFlags_t;
import neo.Game.Animation.Anim.jointModTransform_t;
import neo.Game.Animation.Anim_Blend.idAnimBlend;
import neo.Game.Animation.Anim_Blend.idAnimator;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t3;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderWorld.renderView_s;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.LinkList.idLinkList;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Actor {
    public static final idEventDef AI_EnableEyeFocus      = new idEventDef("enableEyeFocus");
    public static final idEventDef AI_DisableEyeFocus     = new idEventDef("disableEyeFocus");
    public static final idEventDef EV_Footstep            = new idEventDef("footstep");
    public static final idEventDef EV_FootstepLeft        = new idEventDef("leftFoot");
    public static final idEventDef EV_FootstepRight       = new idEventDef("rightFoot");
    public static final idEventDef EV_EnableWalkIK        = new idEventDef("EnableWalkIK");
    public static final idEventDef EV_DisableWalkIK       = new idEventDef("DisableWalkIK");
    public static final idEventDef EV_EnableLegIK         = new idEventDef("EnableLegIK", "d");
    public static final idEventDef EV_DisableLegIK        = new idEventDef("DisableLegIK", "d");
    public static final idEventDef AI_StopAnim            = new idEventDef("stopAnim", "dd");
    public static final idEventDef AI_PlayAnim            = new idEventDef("playAnim", "ds", 'd');
    public static final idEventDef AI_PlayCycle           = new idEventDef("playCycle", "ds", 'd');
    public static final idEventDef AI_IdleAnim            = new idEventDef("idleAnim", "ds", 'd');
    public static final idEventDef AI_SetSyncedAnimWeight = new idEventDef("setSyncedAnimWeight", "ddf");
    public static final idEventDef AI_SetBlendFrames      = new idEventDef("setBlendFrames", "dd");
    public static final idEventDef AI_GetBlendFrames      = new idEventDef("getBlendFrames", "d", 'd');
    public static final idEventDef AI_AnimState           = new idEventDef("animState", "dsd");
    public static final idEventDef AI_GetAnimState        = new idEventDef("getAnimState", "d", 's');
    public static final idEventDef AI_InAnimState         = new idEventDef("inAnimState", "ds", 'd');
    public static final idEventDef AI_FinishAction        = new idEventDef("finishAction", "s");
    public static final idEventDef AI_AnimDone            = new idEventDef("animDone", "dd", 'd');
    public static final idEventDef AI_OverrideAnim        = new idEventDef("overrideAnim", "d");
    public static final idEventDef AI_EnableAnim          = new idEventDef("enableAnim", "dd");
    public static final idEventDef AI_PreventPain         = new idEventDef("preventPain", "f");
    public static final idEventDef AI_DisablePain         = new idEventDef("disablePain");
    public static final idEventDef AI_EnablePain          = new idEventDef("enablePain");
    public static final idEventDef AI_GetPainAnim         = new idEventDef("getPainAnim", null, 's');
    public static final idEventDef AI_SetAnimPrefix       = new idEventDef("setAnimPrefix", "s");
    public static final idEventDef AI_HasAnim             = new idEventDef("hasAnim", "ds", 'f');
    public static final idEventDef AI_CheckAnim           = new idEventDef("checkAnim", "ds");
    public static final idEventDef AI_ChooseAnim          = new idEventDef("chooseAnim", "ds", 's');
    public static final idEventDef AI_AnimLength          = new idEventDef("animLength", "ds", 'f');
    public static final idEventDef AI_AnimDistance        = new idEventDef("animDistance", "ds", 'f');
    public static final idEventDef AI_HasEnemies          = new idEventDef("hasEnemies", null, 'd');
    public static final idEventDef AI_NextEnemy           = new idEventDef("nextEnemy", "E", 'e');
    public static final idEventDef AI_ClosestEnemyToPoint = new idEventDef("closestEnemyToPoint", "v", 'e');
    public static final idEventDef AI_SetNextState        = new idEventDef("setNextState", "s");
    public static final idEventDef AI_SetState            = new idEventDef("setState", "s");
    public static final idEventDef AI_GetState            = new idEventDef("getState", null, 's');
    public static final idEventDef AI_GetHead             = new idEventDef("getHead", null, 'e');
    //    
    //    

    /* **********************************************************************

     idAnimState

     ***********************************************************************/
    public static class idAnimState {

        public  boolean    idleAnim;
        public  idStr      state;
        public  int        animBlendFrames;
        public  int        lastAnimBlendFrames;        // allows override anims to blend based on the last transition time
        private idActor    self;
        private idAnimator animator;
        private idThread   thread;
        private int        channel;
        private boolean    disabled;
        //
        //

        public idAnimState() {
            this.state = new idStr();
            this.self = null;
            this.animator = null;
            this.thread = null;
            this.idleAnim = true;
            this.disabled = true;
            this.channel = ANIMCHANNEL_ALL;
            this.animBlendFrames = 0;
            this.lastAnimBlendFrames = 0;
        }
        // ~idAnimState();

        public void Save(idSaveGame savefile) {

            savefile.WriteObject(this.self);

            // Save the entity owner of the animator
            savefile.WriteObject(this.animator.GetEntity());

            savefile.WriteObject(this.thread);

            savefile.WriteString(this.state);

            savefile.WriteInt(this.animBlendFrames);
            savefile.WriteInt(this.lastAnimBlendFrames);
            savefile.WriteInt(this.channel);
            savefile.WriteBool(this.idleAnim);
            savefile.WriteBool(this.disabled);
        }

        public void Restore(idRestoreGame savefile) {
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/self);

            final idEntity animOwner = new idEntity();
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/animOwner);
            if (animOwner != null) {
                this.animator = animOwner.GetAnimator();
            }

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/thread);

            savefile.ReadString(this.state);

            this.animBlendFrames = savefile.ReadInt();
            this.lastAnimBlendFrames = savefile.ReadInt();
            this.channel = savefile.ReadInt();
            this.idleAnim = savefile.ReadBool();
            this.disabled = savefile.ReadBool();
        }

        public void Init(idActor owner, idAnimator _animator, int animchannel) {
            assert (owner != null);
            assert (_animator != null);
            this.self = owner;
            this.animator = _animator;
            this.channel = animchannel;

            if (NOT(this.thread)) {
                this.thread = new idThread();
                this.thread.ManualDelete();
            }
            this.thread.EndThread();
            this.thread.ManualControl();
        }

        public void Shutdown() {
//	delete thread;
            this.thread = null;
        }

        public void SetState(final String statename, int blendFrames) {
            function_t func;

            func = this.self.scriptObject.GetFunction(statename);
            if (null == func) {
                assert (false);
                gameLocal.Error("Can't find function '%s' in object '%s'", statename, this.self.scriptObject.GetTypeName());
            }

            this.state.oSet(statename);
            this.disabled = false;
            this.animBlendFrames = blendFrames;
            this.lastAnimBlendFrames = blendFrames;
            this.thread.CallFunction(this.self, func, true);

            this.animBlendFrames = blendFrames;
            this.lastAnimBlendFrames = blendFrames;
            this.disabled = false;
            this.idleAnim = false;

            if (ai_debugScript.GetInteger() == this.self.entityNumber) {
                gameLocal.Printf("%d: %s: Animstate: %s\n", gameLocal.time, this.self.name, this.state);
            }
        }

        public void StopAnim(int frames) {
            this.animBlendFrames = 0;
            this.animator.Clear(this.channel, gameLocal.time, FRAME2MS(frames));
        }

        public void PlayAnim(int anim) {
            if (anim != 0) {
                this.animator.PlayAnim(this.channel, anim, gameLocal.time, FRAME2MS(this.animBlendFrames));
            }
            this.animBlendFrames = 0;
        }

        public void CycleAnim(int anim) {
            if (anim != 0) {
                this.animator.CycleAnim(this.channel, anim, gameLocal.time, FRAME2MS(this.animBlendFrames));
            }
            this.animBlendFrames = 0;
        }

        public void BecomeIdle() {
            this.idleAnim = true;
        }

        public boolean UpdateState() {
            if (this.disabled) {
                return false;
            }

            if (ai_debugScript.GetInteger() == this.self.entityNumber) {
                this.thread.EnableDebugInfo();
            } else {
                this.thread.DisableDebugInfo();
            }

            this.thread.Execute();

            return true;
        }

        public boolean Disabled() {
            return this.disabled;
        }

        public void Enable(int blendFrames) {
            if (this.disabled) {
                this.disabled = false;
                this.animBlendFrames = blendFrames;
                this.lastAnimBlendFrames = blendFrames;
                if (this.state.Length() != 0) {
                    SetState(this.state.getData(), blendFrames);
                }
            }
        }

        public void Disable() {
            this.disabled = true;
            this.idleAnim = false;
        }

        public boolean AnimDone(int blendFrames) {
            int animDoneTime;

            animDoneTime = this.animator.CurrentAnim(this.channel).GetEndTime();
            if (animDoneTime < 0) {
                // playing a cycle
                return false;
            } else if ((animDoneTime - FRAME2MS(blendFrames)) <= gameLocal.time) {
                return true;
            } else {
                return false;
            }
        }

        public boolean IsIdle() {
            return this.disabled || this.idleAnim;
        }

        public animFlags_t GetAnimFlags() {
            animFlags_t flags = new animFlags_t();

//            memset(flags, 0, sizeof(flags));
            if (!this.disabled && !AnimDone(0)) {
                flags = this.animator.GetAnimFlags(this.animator.CurrentAnim(this.channel).AnimNum());
            }

            return flags;
        }
    }

    public static class idAttachInfo {

        public idEntityPtr<idEntity> ent;
        public int channel;

        public idAttachInfo() {
            this.ent = new idEntityPtr<>();
        }
    }

    public static class copyJoints_t {

        public jointModTransform_t    mod;
        public int[]/*jointHandle_t*/ from = {0};
        public int[]/*jointHandle_t*/ to = {0};
    }

    /* **********************************************************************

     idActor

     ***********************************************************************/
    public static class idActor extends idAFEntity_Gibbable {
        //public	CLASS_PROTOTYPE( idActor );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//        public static idTypeInfo Type;
//
//        public static idClass CreateInstance();
        //        public idTypeInfo GetType();
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idAFEntity_Gibbable.getEventCallBacks());
            eventCallbacks.put(AI_EnableEyeFocus, (eventCallback_t0<idActor>) idActor::Event_EnableEyeFocus);
            eventCallbacks.put(AI_DisableEyeFocus, (eventCallback_t0<idActor>) idActor::Event_DisableEyeFocus);
            eventCallbacks.put(EV_Footstep, (eventCallback_t0<idActor>) idActor::Event_Footstep);
            eventCallbacks.put(EV_FootstepLeft, (eventCallback_t0<idActor>) idActor::Event_Footstep);
            eventCallbacks.put(EV_FootstepRight, (eventCallback_t0<idActor>) idActor::Event_Footstep);
            eventCallbacks.put(EV_EnableWalkIK, (eventCallback_t0<idActor>) idActor::Event_EnableWalkIK);
            eventCallbacks.put(EV_DisableWalkIK, (eventCallback_t0<idActor>) idActor::Event_DisableWalkIK);
            eventCallbacks.put(EV_EnableLegIK, (eventCallback_t1<idActor>) idActor::Event_EnableLegIK);
            eventCallbacks.put(EV_DisableLegIK, (eventCallback_t1<idActor>) idActor::Event_DisableLegIK);
            eventCallbacks.put(AI_PreventPain, (eventCallback_t1<idActor>) idActor::Event_PreventPain);
            eventCallbacks.put(AI_DisablePain, (eventCallback_t0<idActor>) idActor::Event_DisablePain);
            eventCallbacks.put(AI_EnablePain, (eventCallback_t0<idActor>) idActor::Event_EnablePain);
            eventCallbacks.put(AI_GetPainAnim, (eventCallback_t0<idActor>) idActor::Event_GetPainAnim);
            eventCallbacks.put(AI_SetAnimPrefix, (eventCallback_t1<idActor>) idActor::Event_SetAnimPrefix);
            eventCallbacks.put(AI_StopAnim, (eventCallback_t2<idActor>) idActor::Event_StopAnim);
            eventCallbacks.put(AI_PlayAnim, (eventCallback_t2<idActor>) idActor::Event_PlayAnim);
            eventCallbacks.put(AI_PlayCycle, (eventCallback_t2<idActor>) idActor::Event_PlayCycle);
            eventCallbacks.put(AI_IdleAnim, (eventCallback_t2<idActor>) idActor::Event_IdleAnim);
            eventCallbacks.put(AI_SetSyncedAnimWeight, (eventCallback_t3<idActor>) idActor::Event_SetSyncedAnimWeight);
            eventCallbacks.put(AI_SetBlendFrames, (eventCallback_t2<idActor>) idActor::Event_SetBlendFrames);
            eventCallbacks.put(AI_GetBlendFrames, (eventCallback_t1<idActor>) idActor::Event_GetBlendFrames);
            eventCallbacks.put(AI_AnimState, (eventCallback_t3<idActor>) idActor::Event_AnimState);
            eventCallbacks.put(AI_GetAnimState, (eventCallback_t1<idActor>) idActor::Event_GetAnimState);
            eventCallbacks.put(AI_InAnimState, (eventCallback_t2<idActor>) idActor::Event_InAnimState);
            eventCallbacks.put(AI_FinishAction, (eventCallback_t1<idActor>) idActor::Event_FinishAction);
            eventCallbacks.put(AI_AnimDone, (eventCallback_t2<idActor>) idActor::Event_AnimDone);
            eventCallbacks.put(AI_OverrideAnim, (eventCallback_t1<idActor>) idActor::Event_OverrideAnim);
            eventCallbacks.put(AI_EnableAnim, (eventCallback_t2<idActor>) idActor::Event_EnableAnim);
            eventCallbacks.put(AI_HasAnim, (eventCallback_t2<idActor>) idActor::Event_HasAnim);
            eventCallbacks.put(AI_CheckAnim, (eventCallback_t2<idActor>) idActor::Event_CheckAnim);
            eventCallbacks.put(AI_ChooseAnim, (eventCallback_t2<idActor>) idActor::Event_ChooseAnim);
            eventCallbacks.put(AI_AnimLength, (eventCallback_t2<idActor>) idActor::Event_AnimLength);
            eventCallbacks.put(AI_AnimDistance, (eventCallback_t2<idActor>) idActor::Event_AnimDistance);
            eventCallbacks.put(AI_HasEnemies, (eventCallback_t0<idActor>) idActor::Event_HasEnemies);
            eventCallbacks.put(AI_NextEnemy, (eventCallback_t1<idActor>) idActor::Event_NextEnemy);
            eventCallbacks.put(AI_ClosestEnemyToPoint, (eventCallback_t1<idActor>) idActor::Event_ClosestEnemyToPoint);
            eventCallbacks.put(EV_StopSound, (eventCallback_t2<idActor>) idActor::Event_StopSound);
            eventCallbacks.put(AI_SetNextState, (eventCallback_t1<idActor>) idActor::Event_SetNextState);
            eventCallbacks.put(AI_SetState, (eventCallback_t1<idActor>) idActor::Event_SetState);
            eventCallbacks.put(AI_GetState, (eventCallback_t0<idActor>) idActor::Event_GetState);
            eventCallbacks.put(AI_GetHead, (eventCallback_t0<idActor>) idActor::Event_GetHead);
        }

        //
        public        int                         team;
        public        int                         rank;                 // monsters don't fight back if the attacker's rank is higher
        public        idMat3                      viewAxis;             // view axis of the actor
        //
        public        idLinkList<idActor>         enemyNode;            // node linked into an entity's enemy list for quick lookups of who is attacking him
        public        idLinkList<idActor>         enemyList;            // list of characters that have targeted the player as their enemy
        //
        // friend class			idAnimState;
        //
        //
        protected     float                       fovDot;               // cos( fovDegrees )
        protected     idVec3                      eyeOffset;            // offset of eye relative to physics origin
        protected     idVec3                      modelOffset;          // offset of visual model relative to the physics origin
        //
        protected     idAngles                    deltaViewAngles;      // delta angles relative to view input angles
        //
        protected     int                         pain_debounce_time;   // next time the actor can show pain
        protected     int                         pain_delay;           // time between playing pain sound
        protected     int                         pain_threshold;       // how much damage monster can take at any one time before playing pain animation
        //
        protected     idStrList                   damageGroups;         // body damage groups
        protected     idList<Float>               damageScale;          // damage scale per damage gruop
        //
        protected     boolean                     use_combat_bbox;      // whether to use the bounding box for combat collision
        protected     idEntityPtr<idAFAttachment> head;
        protected     idList<copyJoints_t>        copyJoints;           // copied from the body animation to the head model
        //
        // state variables
        protected     function_t                  state;
        protected     function_t                  idealState;
        //
        // joint handles
        protected     int/*jointHandle_t*/        leftEyeJoint;
        protected     int/*jointHandle_t*/        rightEyeJoint;
        protected     int/*jointHandle_t*/        soundJoint;
        //
        protected     idIK_Walk                   walkIK;
        //
        protected     idStr                       animPrefix;
        protected     idStr                       painAnim;
        //
        // blinking
        protected     int                         blink_anim;
        protected     int                         blink_time;
        protected     int                         blink_min;
        protected     int                         blink_max;
        //
        // script variables
        protected     idThread                    scriptThread;
        protected     idStr                       waitState;
        protected     idAnimState                 headAnim;
        protected     idAnimState                 torsoAnim;
        protected     idAnimState                 legsAnim;
        //
        protected     boolean                     allowPain;
        protected     boolean                     allowEyeFocus;
        protected     boolean                     finalBoss;
        //
        protected     int                         painTime;
        //
        protected idList<idAttachInfo> attachments = new idList<>(idAttachInfo.class);
        //
        //

        public idActor() {
            this.viewAxis = idMat3.getMat3_identity();

            this.scriptThread = null;		// initialized by ConstructScriptObject, which is called by idEntity::Spawn

            this.use_combat_bbox = false;
            this.head = new idEntityPtr<>();

            this.team = 0;
            this.rank = 0;
            this.fovDot = 0;
            this.eyeOffset = new idVec3();
            this.pain_debounce_time = 0;
            this.pain_delay = 0;
            this.pain_threshold = 0;
            
            this.copyJoints = new idList<>();

            this.state = null;
            this.idealState = null;

            this.leftEyeJoint = INVALID_JOINT;
            this.rightEyeJoint = INVALID_JOINT;
            this.soundJoint = INVALID_JOINT;

            this.modelOffset = new idVec3();
            this.deltaViewAngles = new idAngles();

            this.painTime = 0;
            this.allowPain = false;
            this.allowEyeFocus = false;
            
            this.damageGroups = new idStrList();
            this.damageScale = new idList<>();

            this.waitState = new idStr();
            this.headAnim = new idAnimState();
            this.torsoAnim = new idAnimState();
            this.legsAnim = new idAnimState();
            
            this.walkIK = new idIK_Walk();
            
            this.animPrefix = new idStr();
            this.painAnim = new idStr();

            this.blink_anim = 0;//null;
            this.blink_time = 0;
            this.blink_min = 0;
            this.blink_max = 0;

            this.finalBoss = false;

            this.attachments.SetGranularity(1);

            this.enemyNode = new idLinkList<>(this);
            this.enemyList = new idLinkList<>(this);
        }
        // virtual					~idActor( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            final idEntity[] ent = {null};
            final idStr jointName = new idStr();
            final float[] fovDegrees = {0};
            final int[] rank = {0}, team = {0};
            final boolean[] use_combat_bbox = {false};

            this.animPrefix.oSet("");
            this.state = null;
            this.idealState = null;

            this.spawnArgs.GetInt("rank", "0", rank);
            this.spawnArgs.GetInt("team", "0", team);
            this.rank = rank[0];
            this.team = team[0];

            this.spawnArgs.GetVector("offsetModel", "0 0 0", this.modelOffset);

            this.spawnArgs.GetBool("use_combat_bbox", "0", use_combat_bbox);
            this.use_combat_bbox = use_combat_bbox[0];

            this.viewAxis.oSet(GetPhysics().GetAxis());

            this.spawnArgs.GetFloat("fov", "90", fovDegrees);
            SetFOV(fovDegrees[0]);

            this.pain_debounce_time = 0;

            this.pain_delay = (int) SEC2MS(this.spawnArgs.GetFloat("pain_delay"));
            this.pain_threshold = this.spawnArgs.GetInt("pain_threshold");

            LoadAF();

            this.walkIK.Init(this, IK_ANIM, this.modelOffset);

            // the animation used to be set to the IK_ANIM at this point, but that was fixed, resulting in
            // attachments not binding correctly, so we're stuck setting the IK_ANIM before attaching things.
            this.animator.ClearAllAnims(gameLocal.time, 0);
            this.animator.SetFrame(ANIMCHANNEL_ALL, this.animator.GetAnim(IK_ANIM), 0, 0, 0);

            // spawn any attachments we might have
            idKeyValue kv = this.spawnArgs.MatchPrefix("def_attach", null);
            while (kv != null) {
                final idDict args = new idDict();

                args.Set("classname", kv.GetValue());

                // make items non-touchable so the player can't take them out of the character's hands
                args.Set("no_touch", "1");

                // don't let them drop to the floor
                args.Set("dropToFloor", "0");

                gameLocal.SpawnEntityDef(args, ent);
                if (NOT(ent[0])) {
                    gameLocal.Error("Couldn't spawn '%s' to attach to entity '%s'", kv.GetValue(), this.name);
                } else {
                    Attach(ent[0]);
                }
                kv = this.spawnArgs.MatchPrefix("def_attach", kv);
            }

            SetupDamageGroups();
            SetupHead();

            // clear the bind anim
            this.animator.ClearAllAnims(gameLocal.time, 0);

            final idEntity headEnt = this.head.GetEntity();
            idAnimator headAnimator;
            if (headEnt != null) {
                headAnimator = headEnt.GetAnimator();
            } else {
                headAnimator = this.animator;
            }

            if (headEnt != null) {
                // set up the list of joints to copy to the head
                for (kv = this.spawnArgs.MatchPrefix("copy_joint", null); kv != null; kv = this.spawnArgs.MatchPrefix("copy_joint", kv)) {
                    if (kv.GetValue().IsEmpty()) {
                        // probably clearing out inherited key, so skip it
                        continue;
                    }

                    final copyJoints_t copyJoint = new copyJoints_t();
                    jointName.oSet(kv.GetKey());
                    if (jointName.StripLeadingOnce("copy_joint_world ")) {
                        copyJoint.mod = JOINTMOD_WORLD_OVERRIDE;
                    } else {
                        jointName.StripLeadingOnce("copy_joint ");
                        copyJoint.mod = JOINTMOD_LOCAL_OVERRIDE;
                    }

                    copyJoint.from[0] = this.animator.GetJointHandle(jointName);
                    if (copyJoint.from[0] == INVALID_JOINT) {
                        gameLocal.Warning("Unknown copy_joint '%s' on entity %s", jointName, this.name);
                        continue;
                    }

                    jointName.oSet(kv.GetValue());
                    copyJoint.to[0] = headAnimator.GetJointHandle(jointName);
                    if (copyJoint.to[0] == INVALID_JOINT) {
                        gameLocal.Warning("Unknown copy_joint '%s' on head of entity %s", jointName, this.name);
                        continue;
                    }

                    this.copyJoints.Append(copyJoint);
                }
            }

            // set up blinking
            this.blink_anim = headAnimator.GetAnim("blink");
            this.blink_time = 0;	// it's ok to blink right away
            this.blink_min = (int) SEC2MS(this.spawnArgs.GetFloat("blink_min", "0.5"));
            this.blink_max = (int) SEC2MS(this.spawnArgs.GetFloat("blink_max", "8"));

            // set up the head anim if necessary
            final int headAnim = headAnimator.GetAnim("def_head");
            if (headAnim != 0) {
                if (headEnt != null) {
                    headAnimator.CycleAnim(ANIMCHANNEL_ALL, headAnim, gameLocal.time, 0);
                } else {
                    headAnimator.CycleAnim(ANIMCHANNEL_HEAD, headAnim, gameLocal.time, 0);
                }
            }

            if (this.spawnArgs.GetString("sound_bone", "", jointName)) {
                this.soundJoint = this.animator.GetJointHandle(jointName);
                if (this.soundJoint == INVALID_JOINT) {
                    gameLocal.Warning("idAnimated '%s' at (%s): cannot find joint '%s' for sound playback", this.name, GetPhysics().GetOrigin().ToString(0), jointName);
                }
            }

            this.finalBoss = this.spawnArgs.GetBool("finalBoss");

            FinishSetup();
        }

        public void Restart() {
            assert (NOT(this.head.GetEntity()));
            SetupHead();
            FinishSetup();
        }

        /*
         ================
         idActor::Save

         archive object for savegame file
         ================
         */
        @Override
        public void Save(idSaveGame savefile) {
            idActor ent;
            int i;

            savefile.WriteInt(this.team);
            savefile.WriteInt(this.rank);
            savefile.WriteMat3(this.viewAxis);

            savefile.WriteInt(this.enemyList.Num());
            for (ent = this.enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
                savefile.WriteObject(ent);
            }

            savefile.WriteFloat(this.fovDot);
            savefile.WriteVec3(this.eyeOffset);
            savefile.WriteVec3(this.modelOffset);
            savefile.WriteAngles(this.deltaViewAngles);

            savefile.WriteInt(this.pain_debounce_time);
            savefile.WriteInt(this.pain_delay);
            savefile.WriteInt(this.pain_threshold);

            savefile.WriteInt(this.damageGroups.Num());
            for (i = 0; i < this.damageGroups.Num(); i++) {
                savefile.WriteString(this.damageGroups.oGet(i));
            }

            savefile.WriteInt(this.damageScale.Num());
            for (i = 0; i < this.damageScale.Num(); i++) {
                savefile.WriteFloat(this.damageScale.oGet(i));
            }

            savefile.WriteBool(this.use_combat_bbox);
            this.head.Save(savefile);

            savefile.WriteInt(this.copyJoints.Num());
            for (i = 0; i < this.copyJoints.Num(); i++) {
                savefile.WriteInt(etoi(this.copyJoints.oGet(i).mod));
                savefile.WriteJoint(this.copyJoints.oGet(i).from[0]);
                savefile.WriteJoint(this.copyJoints.oGet(i).to[0]);
            }

            savefile.WriteJoint(this.leftEyeJoint);
            savefile.WriteJoint(this.rightEyeJoint);
            savefile.WriteJoint(this.soundJoint);

            this.walkIK.Save(savefile);

            savefile.WriteString(this.animPrefix);
            savefile.WriteString(this.painAnim);

            savefile.WriteInt(this.blink_anim);
            savefile.WriteInt(this.blink_time);
            savefile.WriteInt(this.blink_min);
            savefile.WriteInt(this.blink_max);

            // script variables
            savefile.WriteObject(this.scriptThread);

            savefile.WriteString(this.waitState);

            this.headAnim.Save(savefile);
            this.torsoAnim.Save(savefile);
            this.legsAnim.Save(savefile);

            savefile.WriteBool(this.allowPain);
            savefile.WriteBool(this.allowEyeFocus);

            savefile.WriteInt(this.painTime);

            savefile.WriteInt(this.attachments.Num());
            for (i = 0; i < this.attachments.Num(); i++) {
                this.attachments.oGet(i).ent.Save(savefile);
                savefile.WriteInt(this.attachments.oGet(i).channel);
            }

            savefile.WriteBool(this.finalBoss);

            final idToken token = new idToken();

            //FIXME: this is unneccesary
            if (this.state != null) {
                final idLexer src = new idLexer(this.state.Name(), this.state.Name().length(), "idAI::Save");

                src.ReadTokenOnLine(token);
                src.ExpectTokenString("::");
                src.ReadTokenOnLine(token);

                savefile.WriteString(token);
            } else {
                savefile.WriteString("");
            }

            if (this.idealState != null) {
                final idLexer src = new idLexer(this.idealState.Name(), this.idealState.Name().length(), "idAI::Save");

                src.ReadTokenOnLine(token);
                src.ExpectTokenString("::");
                src.ReadTokenOnLine(token);

                savefile.WriteString(token);
            } else {
                savefile.WriteString("");
            }

        }

        /*
         ================
         idActor::Restore

         unarchives object from save game file
         ================
         */
        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            final int[] num = {0};
            final idActor ent = new idActor();

            this.team = savefile.ReadInt();
            this.rank = savefile.ReadInt();
            savefile.ReadMat3(this.viewAxis);

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadObject(/*reinterpret_cast<idClass *&>*/ent);
                assert (ent != null);
                if (ent != null) {
                    ent.enemyNode.AddToEnd(this.enemyList);
                }
            }

            this.fovDot = savefile.ReadFloat();
            savefile.ReadVec3(this.eyeOffset);
            savefile.ReadVec3(this.modelOffset);
            savefile.ReadAngles(this.deltaViewAngles);

            this.pain_debounce_time = savefile.ReadInt();
            this.pain_delay = savefile.ReadInt();
            this.pain_threshold = savefile.ReadInt();

            savefile.ReadInt(num);
            this.damageGroups.SetGranularity(1);
            this.damageGroups.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadString(this.damageGroups.oGet(i));
            }

            savefile.ReadInt(num);
            this.damageScale.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                this.damageScale.oSet(i, savefile.ReadFloat());
            }

            this.use_combat_bbox = savefile.ReadBool();
            this.head.Restore(savefile);

            savefile.ReadInt(num);
            this.copyJoints.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                final int[] val = {0};
                savefile.ReadInt(val);
                this.copyJoints.oGet(i).mod = jointModTransform_t.values()[val[0]];
                savefile.ReadJoint(this.copyJoints.oGet(i).from);
                savefile.ReadJoint(this.copyJoints.oGet(i).to);
            }

            this.leftEyeJoint = savefile.ReadJoint();
            this.rightEyeJoint = savefile.ReadJoint();
            this.soundJoint = savefile.ReadJoint();

            this.walkIK.Restore(savefile);

            savefile.ReadString(this.animPrefix);
            savefile.ReadString(this.painAnim);

            this.blink_anim = savefile.ReadInt();
            this.blink_time = savefile.ReadInt();
            this.blink_min = savefile.ReadInt();
            this.blink_max = savefile.ReadInt();

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/(this.scriptThread));

            savefile.ReadString(this.waitState);

            this.headAnim.Restore(savefile);
            this.torsoAnim.Restore(savefile);
            this.legsAnim.Restore(savefile);

            this.allowPain = savefile.ReadBool();
            this.allowEyeFocus = savefile.ReadBool();

            this.painTime = savefile.ReadInt();

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                final idAttachInfo attach = this.attachments.Alloc();
                attach.ent.Restore(savefile);
                attach.channel = savefile.ReadInt();
            }

            this.finalBoss = savefile.ReadBool();

            final idStr stateName = new idStr();

            savefile.ReadString(stateName);
            if (stateName.Length() > 0) {
                this.state = GetScriptFunction(stateName.getData());
            }

            savefile.ReadString(stateName);
            if (stateName.Length() > 0) {
                this.idealState = GetScriptFunction(stateName.getData());
            }
        }

        @Override
        public void Hide() {
            idEntity ent;
            idEntity next;

            idAFEntity_Base_Hide();//TODO:super size me
            if (this.head.GetEntity() != null) {
                this.head.GetEntity().Hide();
            }

            for (ent = GetNextTeamEntity(); ent != null; ent = next) {
                next = ent.GetNextTeamEntity();
                if (ent.GetBindMaster().equals(this)) {
                    ent.Hide();
                    if (ent.IsType(idLight.class)) {
                        ((idLight) ent).Off();
                    }
                }
            }
            UnlinkCombat();
        }

        @Override
        public void Show() {
            idEntity ent;
            idEntity next;

            idAFEntity_Base_Show();//TODO:super size me
            if (this.head.GetEntity() != null) {
                this.head.GetEntity().Show();
            }

            for (ent = GetNextTeamEntity(); ent != null; ent = next) {
                next = ent.GetNextTeamEntity();
                if (ent.GetBindMaster() == this) {
                    ent.Show();
                    if (ent.IsType(idLight.class)) {
                        ((idLight) ent).On();
                    }
                }
            }
            UnlinkCombat();
        }

        @Override
        public int GetDefaultSurfaceType() {
            return etoi(SURFTYPE_FLESH);
        }

        @Override
        public void ProjectOverlay(final idVec3 origin, final idVec3 dir, float size, final String material) {
            idEntity ent;
            idEntity next;

            idEntity_ProjectOverlay(origin, dir, size, material);

            for (ent = GetNextTeamEntity(); ent != null; ent = next) {
                next = ent.GetNextTeamEntity();
                if (ent.GetBindMaster() == this) {
                    if (ent.fl.takedamage && ent.spawnArgs.GetBool("bleed")) {
                        ent.ProjectOverlay(origin, dir, size, material);
                    }
                }
            }
        }

        @Override
        public boolean LoadAF() {
            final idStr fileName = new idStr();

            if (!this.spawnArgs.GetString("ragdoll", "*unknown*", fileName) || (0 == fileName.Length())) {
                return false;
            }
            this.af.SetAnimator(GetAnimator());
            return this.af.Load(this, fileName);
        }

        public void SetupBody() {
            String jointname;

            this.animator.ClearAllAnims(gameLocal.time, 0);
            this.animator.ClearAllJoints();

            final idEntity headEnt = this.head.GetEntity();
            if (headEnt != null) {
                jointname = this.spawnArgs.GetString("bone_leftEye");
                this.leftEyeJoint = headEnt.GetAnimator().GetJointHandle(jointname);

                jointname = this.spawnArgs.GetString("bone_rightEye");
                this.rightEyeJoint = headEnt.GetAnimator().GetJointHandle(jointname);

                // set up the eye height.  check if it's specified in the def.
                if (!this.spawnArgs.GetFloat("eye_height", "0", new float[]{this.eyeOffset.z})) {
                    // if not in the def, then try to base it off the idle animation
                    final int anim = headEnt.GetAnimator().GetAnim("idle");
                    if ((anim != 0) && (this.leftEyeJoint != INVALID_JOINT)) {
                        final idVec3 pos = new idVec3();
                        final idMat3 axis = new idMat3();
                        headEnt.GetAnimator().PlayAnim(ANIMCHANNEL_ALL, anim, gameLocal.time, 0);
                        headEnt.GetAnimator().GetJointTransform(this.leftEyeJoint, gameLocal.time, pos, axis);
                        headEnt.GetAnimator().ClearAllAnims(gameLocal.time, 0);
                        headEnt.GetAnimator().ForceUpdate();
                        pos.oPluSet(headEnt.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin()));
                        this.eyeOffset = pos.oPlus(this.modelOffset);
                    } else {
                        // just base it off the bounding box size
                        this.eyeOffset.z = GetPhysics().GetBounds().oGet(1).z - 6;
                    }
                }
                this.headAnim.Init(this, headEnt.GetAnimator(), ANIMCHANNEL_ALL);
            } else {
                jointname = this.spawnArgs.GetString("bone_leftEye");
                this.leftEyeJoint = this.animator.GetJointHandle(jointname);

                jointname = this.spawnArgs.GetString("bone_rightEye");
                this.rightEyeJoint = this.animator.GetJointHandle(jointname);

                // set up the eye height.  check if it's specified in the def.
                if (!this.spawnArgs.GetFloat("eye_height", "0", new float[]{this.eyeOffset.z})) {
                    // if not in the def, then try to base it off the idle animation
                    final int anim = this.animator.GetAnim("idle");
                    if ((anim != 0) && (this.leftEyeJoint != INVALID_JOINT)) {
                        final idVec3 pos = new idVec3();
                        final idMat3 axis = new idMat3();
                        this.animator.PlayAnim(ANIMCHANNEL_ALL, anim, gameLocal.time, 0);
                        this.animator.GetJointTransform(this.leftEyeJoint, gameLocal.time, pos, axis);
                        this.animator.ClearAllAnims(gameLocal.time, 0);
                        this.animator.ForceUpdate();
                        this.eyeOffset = pos.oPlus(this.modelOffset);
                    } else {
                        // just base it off the bounding box size
                        this.eyeOffset.z = GetPhysics().GetBounds().oGet(1).z - 6;
                    }
                }
                this.headAnim.Init(this, this.animator, ANIMCHANNEL_HEAD);
            }

            this.waitState.oSet("");

            this.torsoAnim.Init(this, this.animator, ANIMCHANNEL_TORSO);
            this.legsAnim.Init(this, this.animator, ANIMCHANNEL_LEGS);
        }

        public void CheckBlink() {
            // check if it's time to blink
            if ((0 == this.blink_anim) || (this.health <= 0) || !this.allowEyeFocus || (this.blink_time > gameLocal.time)) {
                return;
            }

            final idEntity headEnt = this.head.GetEntity();
            if (headEnt != null) {
                headEnt.GetAnimator().PlayAnim(ANIMCHANNEL_EYELIDS, this.blink_anim, gameLocal.time, 1);
            } else {
                this.animator.PlayAnim(ANIMCHANNEL_EYELIDS, this.blink_anim, gameLocal.time, 1);
            }

            // set the next blink time
            this.blink_time = (int) (gameLocal.time + this.blink_min + (gameLocal.random.RandomFloat() * (this.blink_max - this.blink_min)));
        }

        @Override
        public boolean GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            if (this.af.IsActive()) {
                this.af.GetPhysicsToVisualTransform(origin, axis);
                return true;
            }
            origin.oSet(this.modelOffset);
            axis.oSet(this.viewAxis);
            return true;
        }

        @Override
        public boolean GetPhysicsToSoundTransform(idVec3 origin, idMat3 axis) {
            if (this.soundJoint != INVALID_JOINT) {
                this.animator.GetJointTransform(this.soundJoint, gameLocal.time, origin, axis);
                origin.oPluSet(this.modelOffset);
                axis.oSet(this.viewAxis);
            } else {
                origin.oSet(GetPhysics().GetGravityNormal().oMultiply(-this.eyeOffset.z));
                axis.Identity();
            }
            return true;
        }

        /* **********************************************************************

         script state management

         ***********************************************************************/
        // script state management
        public void ShutdownThreads() {
            this.headAnim.Shutdown();
            this.torsoAnim.Shutdown();
            this.legsAnim.Shutdown();

            if (this.scriptThread != null) {
                this.scriptThread.EndThread();
                this.scriptThread.PostEventMS(EV_Remove, 0);
//		delete scriptThread;
                this.scriptThread = null;
            }
        }

        /*
         ================
         idActor::ShouldConstructScriptObjectAtSpawn

         Called during idEntity::Spawn to see if it should construct the script object or not.
         Overridden by subclasses that need to spawn the script object themselves.
         ================
         */
        @Override
        public boolean ShouldConstructScriptObjectAtSpawn() {
            return false;
        }

        /*
         ================
         idActor::ConstructScriptObject

         Called during idEntity::Spawn.  Calls the constructor on the script object.
         Can be overridden by subclasses when a thread doesn't need to be allocated.
         ================
         */
        @Override
        public idThread ConstructScriptObject() {
            function_t constructor;

            // make sure we have a scriptObject
            if (!this.scriptObject.HasObject()) {
                gameLocal.Error("No scriptobject set on '%s'.  Check the '%s' entityDef.", this.name, GetEntityDefName());
            }

            if (NOT(this.scriptThread)) {
                // create script thread
                this.scriptThread = new idThread();
                this.scriptThread.ManualDelete();
                this.scriptThread.ManualControl();
                this.scriptThread.SetThreadName(this.name.getData());
            } else {
                this.scriptThread.EndThread();
            }

            // call script object's constructor
            constructor = this.scriptObject.GetConstructor();
            if (NOT(constructor)) {
                gameLocal.Error("Missing constructor on '%s' for entity '%s'", this.scriptObject.GetTypeName(), this.name);
            }

            // init the script object's data
            this.scriptObject.ClearObject();

            // just set the current function on the script.  we'll execute in the subclasses.
            this.scriptThread.CallFunction(this, constructor, true);

            return this.scriptThread;
        }

        public void UpdateScript() {
            int i;

            if (ai_debugScript.GetInteger() == this.entityNumber) {
                this.scriptThread.EnableDebugInfo();
            } else {
                this.scriptThread.DisableDebugInfo();
            }

            // a series of state changes can happen in a single frame.
            // this loop limits them in case we've entered an infinite loop.
            for (i = 0; i < 20; i++) {
                if (this.idealState != this.state) {
                    SetState(this.idealState);
                }

                // don't call script until it's done waiting
                if (this.scriptThread.IsWaiting()) {
                    break;
                }

                this.scriptThread.Execute();
                if (this.idealState == this.state) {
                    break;
                }
            }

            if (i == 20) {
                this.scriptThread.Warning("idActor::UpdateScript: exited loop to prevent lockup");
            }
        }

        public function_t GetScriptFunction(final String funcname) {
            final function_t func;

            func = this.scriptObject.GetFunction(funcname);
            if (null == func) {
                this.scriptThread.Error("Unknown function '%s' in '%s'", funcname, this.scriptObject.GetTypeName());
            }

            return func;
        }

        public void SetState(final function_t newState) {
            if (NOT(newState)) {
                gameLocal.Error("idActor::SetState: Null state");
            }

            if (ai_debugScript.GetInteger() == this.entityNumber) {
                gameLocal.Printf("%d: %s: State: %s\n", gameLocal.time, this.name, newState.Name());
            }

            this.state = newState;
            this.idealState = this.state;
            this.scriptThread.CallFunction(this, this.state, true);
        }

        public void SetState(final String statename) {
            final function_t newState;

            newState = GetScriptFunction(statename);
            SetState(newState);
        }

        /* **********************************************************************

         vision

         ***********************************************************************/
        // vision testing
        public void SetEyeHeight(float height) {
            this.eyeOffset.z = height;
        }

        public float EyeHeight() {
            return this.eyeOffset.z;
        }

        public idVec3 EyeOffset() {
            return GetPhysics().GetGravityNormal().oMultiply(-this.eyeOffset.z);
        }

        public idVec3 GetEyePosition() {
            return GetPhysics().GetOrigin().oPlus((GetPhysics().GetGravityNormal().oMultiply(-this.eyeOffset.z)));
        }

        public void GetViewPos(idVec3 origin, idMat3 axis) {
            origin.oSet(GetEyePosition());
            axis.oSet(this.viewAxis);
        }

        public void SetFOV(float fov) {
            this.fovDot = (float) Math.cos(DEG2RAD(fov * 0.5f));
        }

        public boolean CheckFOV(final idVec3 pos) {
            if (this.fovDot == 1.0f) {
                return true;
            }

            float dot;
            idVec3 delta;

            delta = pos.oMinus(GetEyePosition());

            // get our gravity normal
            final idVec3 gravityDir = GetPhysics().GetGravityNormal();

            // infinite vertical vision, so project it onto our orientation plane
            delta.oMinSet(gravityDir.oMultiply(gravityDir.oMultiply(delta)));

            delta.Normalize();
            dot = this.viewAxis.oGet(0).oMultiply(delta);

            return (dot >= this.fovDot);
        }

        public boolean CanSee(idEntity ent, boolean useFOV) {
            final trace_s[] tr = {null};
            idVec3 eye;
            idVec3 toPos;

            if (ent.IsHidden()) {
                return false;
            }

            if (ent.IsType(idActor.class)) {
                toPos = ((idActor) ent).GetEyePosition();
            } else {
                toPos = ent.GetPhysics().GetOrigin();
            }

            if (useFOV && !CheckFOV(toPos)) {
                return false;
            }

            eye = GetEyePosition();

            gameLocal.clip.TracePoint(tr, eye, toPos, MASK_OPAQUE, this);
            if ((tr[0].fraction >= 1.0f) || (gameLocal.GetTraceEntity(tr[0]) == ent)) {
                return true;
            }

            return false;
        }

        public boolean PointVisible(final idVec3 point) {
            final trace_s[] results = {null};
            idVec3 start, end;

            start = GetEyePosition();
            end = point;
            end.oPluSet(2, 1.0f);

            gameLocal.clip.TracePoint(results, start, end, MASK_OPAQUE, this);
            return (results[0].fraction >= 1.0f);
        }

        /*
         =====================
         idActor::GetAIAimTargets

         Returns positions for the AI to aim at.
         =====================
         */
        public void GetAIAimTargets(final idVec3 lastSightPos, idVec3 headPos, idVec3 chestPos) {
            headPos.oSet(lastSightPos.oPlus(EyeOffset()));
            chestPos.oSet(headPos.oPlus(lastSightPos).oPlus(GetPhysics().GetBounds().GetCenter()).oMultiply(0.5f));
        }

        /* **********************************************************************

         Damage

         ***********************************************************************/
        // damage
        /*
         =====================
         idActor::SetupDamageGroups

         FIXME: only store group names once and store an index for each joint
         =====================
         */
        public void SetupDamageGroups() {
            int i;
            idKeyValue arg;
            final idStr groupname = new idStr();
            final idList<Integer/*jointHandle_t*/> jointList = new idList<>();
            int jointnum;
            float scale;

            // create damage zones
            this.damageGroups.SetNum(this.animator.NumJoints());
            arg = this.spawnArgs.MatchPrefix("damage_zone ", null);
            while (arg != null) {
                groupname.oSet(arg.GetKey());
                groupname.Strip("damage_zone ");
                this.animator.GetJointList(arg.GetValue(), jointList);
                for (i = 0; i < jointList.Num(); i++) {
                    jointnum = jointList.oGet(i);
                    this.damageGroups.oSet(jointnum, groupname);
                }
                jointList.Clear();
                arg = this.spawnArgs.MatchPrefix("damage_zone ", arg);
            }

            // initilize the damage zones to normal damage
            this.damageScale.SetNum(this.animator.NumJoints());
            for (i = 0; i < this.damageScale.Num(); i++) {
                this.damageScale.oSet(i, 1.0f);
            }

            // set the percentage on damage zones
            arg = this.spawnArgs.MatchPrefix("damage_scale ", null);
            while (arg != null) {
                scale = atof(arg.GetValue());
                groupname.oSet(arg.GetKey());
                groupname.Strip("damage_scale ");
                for (i = 0; i < this.damageScale.Num(); i++) {
                    if (groupname.equals(this.damageGroups.oGet(i))) {
                        this.damageScale.oSet(i, scale);
                    }
                }
                arg = this.spawnArgs.MatchPrefix("damage_scale ", arg);
            }
        }

        /*
         ============
         idActor::Damage

         this		entity that is being damaged
         inflictor	entity that is causing the damage
         attacker	entity that caused the inflictor to damage targ
         example: this=monster, inflictor=rocket, attacker=player

         dir			direction of the attack for knockback in global space
         point		point at which the damage is being inflicted, used for headshots
         damage		amount of damage being inflicted

         inflictor, attacker, dir, and point can be NULL for environmental effects

         Bleeding wounds and surface overlays are applied in the collision code that
         calls Damage()
         ============
         */
        @Override
        public void Damage(idEntity inflictor, idEntity attacker, final idVec3 dir, final String damageDefName, final float damageScale, final int location) {
            if (!this.fl.takedamage) {
                return;
            }

            if (null == inflictor) {
                inflictor = gameLocal.world;//TODO:oSet
            }
            if (null == attacker) {
                attacker = gameLocal.world;
            }

            if (this.finalBoss && !inflictor.IsType(idSoulCubeMissile.class)) {
                return;
            }

            final idDict damageDef = gameLocal.FindEntityDefDict(damageDefName);
            if (null == damageDef) {
                gameLocal.Error("Unknown damageDef '%s'", damageDefName);
            }

            final int[] damage = {(int) (damageDef.GetInt("damage") * damageScale)};
            damage[0] = GetDamageForLocation(damage[0], location);

            // inform the attacker that they hit someone
            attacker.DamageFeedback(this, inflictor, damage);
            if (damage[0] > 0) {
                this.health -= damage[0];
                if (this.health <= 0) {
                    if (this.health < -999) {
                        this.health = -999;
                    }
                    Killed(inflictor, attacker, damage[0], dir, location);
                    if ((this.health < -20) && this.spawnArgs.GetBool("gib") && damageDef.GetBool("gib")) {
                        Gib(dir, damageDefName);
                    }
                } else {
                    Pain(inflictor, attacker, damage[0], dir, location);
                }
            } else {
                // don't accumulate knockback
                if (this.af.IsLoaded()) {
                    // clear impacts
                    this.af.Rest();

                    // physics is turned off by calling af.Rest()
                    BecomeActive(TH_PHYSICS);
                }
            }
        }

        public int GetDamageForLocation(int damage, int location) {
            if ((location < 0) || (location >= this.damageScale.Num())) {
                return damage;
            }

            return (int) Math.ceil(damage * this.damageScale.oGet(location));
        }

        public String GetDamageGroup(int location) {
            if ((location < 0) || (location >= this.damageGroups.Num())) {
                return "";
            }

            return this.damageGroups.oGet(location).getData();
        }

        public void ClearPain() {
            this.pain_debounce_time = 0;
        }

        @Override
        public boolean Pain(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (this.af.IsLoaded()) {
                // clear impacts
                this.af.Rest();

                // physics is turned off by calling af.Rest()
                BecomeActive(TH_PHYSICS);
            }

            if (gameLocal.time < this.pain_debounce_time) {
                return false;
            }

            // don't play pain sounds more than necessary
            this.pain_debounce_time = gameLocal.time + this.pain_delay;

            if (this.health > 75) {
                StartSound("snd_pain_small", SND_CHANNEL_VOICE, 0, false, null);
            } else if (this.health > 50) {
                StartSound("snd_pain_medium", SND_CHANNEL_VOICE, 0, false, null);
            } else if (this.health > 25) {
                StartSound("snd_pain_large", SND_CHANNEL_VOICE, 0, false, null);
            } else {
                StartSound("snd_pain_huge", SND_CHANNEL_VOICE, 0, false, null);
            }

            if (!this.allowPain || (gameLocal.time < this.painTime)) {
                // don't play a pain anim
                return false;
            }

            if ((this.pain_threshold != 0) && (damage < this.pain_threshold)) {
                return false;
            }

            // set the pain anim
            final String damageGroup = GetDamageGroup(location);

            this.painAnim.oSet("");
            if (this.animPrefix.Length() != 0) {
                if (isNotNullOrEmpty(damageGroup) && !damageGroup.equals("legs")) {
                    this.painAnim.oSet(String.format("%s_pain_%s", this.animPrefix.getData(), damageGroup));
                    if (!this.animator.HasAnim(this.painAnim)) {
                        this.painAnim.oSet(String.format("pain_%s", damageGroup));
                        if (!this.animator.HasAnim(this.painAnim)) {
                            this.painAnim.oSet("");
                        }
                    }
                }

                if (0 == this.painAnim.Length()) {
                    this.painAnim.oSet(String.format("%s_pain", this.animPrefix.getData()));
                    if (!this.animator.HasAnim(this.painAnim)) {
                        this.painAnim.oSet("");
                    }
                }
            } else if (isNotNullOrEmpty(damageGroup) && (!damageGroup.equals("legs"))) {
                this.painAnim.oSet(String.format("pain_%s", damageGroup));
                if (!this.animator.HasAnim(this.painAnim)) {
                    this.painAnim.oSet(String.format("pain_%s", damageGroup));
                    if (!this.animator.HasAnim(this.painAnim)) {
                        this.painAnim.oSet("");
                    }
                }
            }

            if (0 == this.painAnim.Length()) {
                this.painAnim.oSet("pain");
            }

            if (g_debugDamage.GetBool()) {
                gameLocal.Printf("Damage: joint: '%s', zone '%s', anim '%s'\n", this.animator.GetJointName(location),
                        damageGroup, this.painAnim);
            }

            return true;
        }


        /* **********************************************************************

         Model/Ragdoll

         ***********************************************************************/
        // model/combat model/ragdoll
        @Override
        public void SetCombatModel() {
            idAFAttachment headEnt;

            if (!this.use_combat_bbox) {
                if (this.combatModel != null) {
                    this.combatModel.Unlink();
                    this.combatModel.LoadModel(this.modelDefHandle);
                } else {
                    this.combatModel = new idClipModel(this.modelDefHandle);
                }

                headEnt = this.head.GetEntity();
                if (headEnt != null) {
                    headEnt.SetCombatModel();
                }
            }
        }

        @Override
        public idClipModel GetCombatModel() {
            return this.combatModel;
        }

        @Override
        public void LinkCombat() {
            idAFAttachment headEnt;

            if (this.fl.hidden || this.use_combat_bbox) {
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

        public boolean StartRagdoll() {
            float slomoStart, slomoEnd;
            float jointFrictionDent, jointFrictionDentStart, jointFrictionDentEnd;
            float contactFrictionDent, contactFrictionDentStart, contactFrictionDentEnd;

            // if no AF loaded
            if (!this.af.IsLoaded()) {
                return false;
            }

            // if the AF is already active
            if (this.af.IsActive()) {
                return true;
            }

            // disable the monster bounding box
            GetPhysics().DisableClip();

            // start using the AF
            this.af.StartFromCurrentPose(this.spawnArgs.GetInt("velocityTime", "0"));

            slomoStart = MS2SEC(gameLocal.time) + this.spawnArgs.GetFloat("ragdoll_slomoStart", "-1.6");
            slomoEnd = MS2SEC(gameLocal.time) + this.spawnArgs.GetFloat("ragdoll_slomoEnd", "0.8");

            // do the first part of the death in slow motion
            this.af.GetPhysics().SetTimeScaleRamp(slomoStart, slomoEnd);

            jointFrictionDent = this.spawnArgs.GetFloat("ragdoll_jointFrictionDent", "0.1");
            jointFrictionDentStart = MS2SEC(gameLocal.time) + this.spawnArgs.GetFloat("ragdoll_jointFrictionStart", "0.2");
            jointFrictionDentEnd = MS2SEC(gameLocal.time) + this.spawnArgs.GetFloat("ragdoll_jointFrictionEnd", "1.2");

            // set joint friction dent
            this.af.GetPhysics().SetJointFrictionDent(jointFrictionDent, jointFrictionDentStart, jointFrictionDentEnd);

            contactFrictionDent = this.spawnArgs.GetFloat("ragdoll_contactFrictionDent", "0.1");
            contactFrictionDentStart = MS2SEC(gameLocal.time) + this.spawnArgs.GetFloat("ragdoll_contactFrictionStart", "1.0");
            contactFrictionDentEnd = MS2SEC(gameLocal.time) + this.spawnArgs.GetFloat("ragdoll_contactFrictionEnd", "2.0");

            // set contact friction dent
            this.af.GetPhysics().SetContactFrictionDent(contactFrictionDent, contactFrictionDentStart, contactFrictionDentEnd);

            // drop any items the actor is holding
            idMoveableItem.DropItems(this, "death", null);

            // drop any articulated figures the actor is holding
            idAFEntity_Base.DropAFs(this, "death", null);

            RemoveAttachments();

            return true;
        }

        public void StopRagdoll() {
            if (this.af.IsActive()) {
                this.af.Stop();
            }
        }

        @Override
        public boolean UpdateAnimationControllers() {

            if (this.af.IsActive()) {
                return idAFEntity_Base_UpdateAnimationControllers();
            } else {
                this.animator.ClearAFPose();
            }

            if (this.walkIK.IsInitialized()) {
                this.walkIK.Evaluate();
                return true;
            }

            return false;
        }

        // delta view angles to allow movers to rotate the view of the actor
        public idAngles GetDeltaViewAngles() {
            return this.deltaViewAngles;
        }

        public void SetDeltaViewAngles(final idAngles delta) {
            this.deltaViewAngles = delta;
        }

        public boolean HasEnemies() {
            idActor ent;

            for (ent = this.enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
                if (!ent.fl.hidden) {
                    return true;
                }
            }

            return false;
        }

        public idActor ClosestEnemyToPoint(final idVec3 pos) {
            idActor ent;
            idActor bestEnt;
            float bestDistSquared;
            float distSquared;
            idVec3 delta;

            bestDistSquared = idMath.INFINITY;
            bestEnt = null;
            for (ent = this.enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
                if (ent.fl.hidden) {
                    continue;
                }
                delta = ent.GetPhysics().GetOrigin().oMinus(pos);
                distSquared = delta.LengthSqr();
                if (distSquared < bestDistSquared) {
                    bestEnt = ent;
                    bestDistSquared = distSquared;
                }
            }

            return bestEnt;
        }

        public idActor EnemyWithMostHealth() {
            idActor ent;
            idActor bestEnt;

            int most = -9999;
            bestEnt = null;
            for (ent = this.enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
                if (!ent.fl.hidden && (ent.health > most)) {
                    bestEnt = ent;
                    most = ent.health;
                }
            }
            return bestEnt;
        }

        public boolean OnLadder() {
            return false;
        }

        public void GetAASLocation(idAAS aas, idVec3 pos, int[] areaNum) {
            idVec3 size;
            final idBounds bounds = new idBounds();

            GetFloorPos(64.0f, pos);
            if (NOT(aas)) {
                areaNum[0] = 0;
                return;
            }

            size = aas.GetSettings().boundingBoxes[0].oGet(1);
            bounds.oSet(0, size.oNegative());
            size.z = 32.0f;
            bounds.oSet(1, size);

            areaNum[0] = aas.PointReachableAreaNum(pos, bounds, AREA_REACHABLE_WALK);
            if (areaNum[0] != 0) {
                aas.PushPointIntoAreaNum(areaNum[0], pos);
            }
        }

        public void Attach(idEntity ent) {
            final idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();
            int/*jointHandle_t*/ joint;
            String jointName;
            final idAttachInfo attach = this.attachments.Alloc();
            idAngles angleOffset;
            idVec3 originOffset;

            jointName = ent.spawnArgs.GetString("joint");
            joint = this.animator.GetJointHandle(jointName);
            if (joint == INVALID_JOINT) {
                gameLocal.Error("Joint '%s' not found for attaching '%s' on '%s'", jointName, ent.GetClassname(), this.name);
            }

            angleOffset = ent.spawnArgs.GetAngles("angles");
            originOffset = ent.spawnArgs.GetVector("origin");

            attach.channel = this.animator.GetChannelForJoint(joint);
            GetJointWorldTransform(joint, gameLocal.time, origin, axis);
            attach.ent.oSet(ent);

            ent.SetOrigin(origin.oPlus(originOffset.oMultiply(this.renderEntity.axis)));
            final idMat3 rotate = angleOffset.ToMat3();
            final idMat3 newAxis = rotate.oMultiply(axis);
            ent.SetAxis(newAxis);
            ent.BindToJoint(this, joint, true);
            ent.cinematic = this.cinematic;
        }

        @Override
        public void Teleport(final idVec3 origin, final idAngles angles, idEntity destination) {
            GetPhysics().SetOrigin(origin.oPlus(new idVec3(0, 0, CM_CLIP_EPSILON)));
            GetPhysics().SetLinearVelocity(getVec3_origin());

            this.viewAxis = angles.ToMat3();

            UpdateVisuals();

            if (!IsHidden()) {
                // kill anything at the new position
                gameLocal.KillBox(this);
            }
        }

        @Override
        public renderView_s GetRenderView() {
            final renderView_s rv = super.GetRenderView();//TODO:super.super....
            rv.viewaxis = new idMat3(this.viewAxis);
            rv.vieworg = GetEyePosition();
            return rv;
        }

        /* **********************************************************************

         animation state

         ***********************************************************************/
        // animation state control
        public int GetAnim(int channel, final String animName) {
            int anim;
            String temp;
            idAnimator animatorPtr;

            if (channel == ANIMCHANNEL_HEAD) {
                if (NOT(this.head.GetEntity())) {
                    return 0;
                }
                animatorPtr = this.head.GetEntity().GetAnimator();
            } else {
                animatorPtr = this.animator;
            }

            if (this.animPrefix.Length() != 0) {
                temp = va("%s_%s", this.animPrefix, animName);
                anim = animatorPtr.GetAnim(temp);
                if (anim != 0) {
                    return anim;
                }
            }

            anim = animatorPtr.GetAnim(animName);

            return anim;
        }

        public void UpdateAnimState() {
            this.headAnim.UpdateState();
            this.torsoAnim.UpdateState();
            this.legsAnim.UpdateState();
        }

        public void SetAnimState(int channel, final String statename, int blendFrames) {
            function_t func;

            func = this.scriptObject.GetFunction(statename);
            if (null == func) {
                assert (false);
                gameLocal.Error("Can't find function '%s' in object '%s'", statename, this.scriptObject.GetTypeName());
            }

            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    this.headAnim.SetState(statename, blendFrames);
                    this.allowEyeFocus = true;
                    break;

                case ANIMCHANNEL_TORSO:
                    this.torsoAnim.SetState(statename, blendFrames);
                    this.legsAnim.Enable(blendFrames);
                    this.allowPain = true;
                    this.allowEyeFocus = true;
                    break;

                case ANIMCHANNEL_LEGS:
                    this.legsAnim.SetState(statename, blendFrames);
                    this.torsoAnim.Enable(blendFrames);
                    this.allowPain = true;
                    this.allowEyeFocus = true;
                    break;

                default:
                    gameLocal.Error("idActor::SetAnimState: Unknown anim group");
                    break;
            }
        }

        public idStr GetAnimState(int channel) {
            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    return this.headAnim.state;
                case ANIMCHANNEL_TORSO:
                    return this.torsoAnim.state;
                case ANIMCHANNEL_LEGS:
                    return this.legsAnim.state;
                default:
                    gameLocal.Error("idActor::GetAnimState: Unknown anim group");
                    return null;
            }
        }

        public boolean InAnimState(int channel, final String stateName) {
            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    if (this.headAnim.state.equals(stateName)) {
                        return true;
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    if (this.torsoAnim.state.equals(stateName)) {
                        return true;
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    if (this.legsAnim.state.equals(stateName)) {
                        return true;
                    }
                    break;

                default:
                    gameLocal.Error("idActor::InAnimState: Unknown anim group");
                    break;
            }

            return false;
        }

        public String WaitState() {
            if (this.waitState.Length() != 0) {
                return this.waitState.getData();
            } else {
                return null;
            }
        }

        public void SetWaitState(final String _waitstate) {
            this.waitState.oSet(_waitstate);
        }

        public boolean AnimDone(int channel, int blendFrames) {
            int animDoneTime;

            animDoneTime = this.animator.CurrentAnim(channel).GetEndTime();
            if (animDoneTime < 0) {
                // playing a cycle
                return false;
            } else if ((animDoneTime - FRAME2MS(blendFrames)) <= gameLocal.time) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void SpawnGibs(final idVec3 dir, final String damageDefName) {
            super.SpawnGibs(dir, damageDefName);
            RemoveAttachments();
        }

        @Override
        protected void Gib(final idVec3 dir, final String damageDefName) {
            // no gibbing in multiplayer - by self damage or by moving objects
            if (gameLocal.isMultiplayer) {
                return;
            }
            // only gib once
            if (this.gibbed) {
                return;
            }
            super.Gib(dir, damageDefName);
            if (this.head.GetEntity() != null) {
                this.head.GetEntity().Hide();
            }
            StopSound(etoi(SND_CHANNEL_VOICE), false);
        }

        // removes attachments with "remove" set for when character dies
        protected void RemoveAttachments() {
            int i;
            idEntity ent;

            // remove any attached entities
            for (i = 0; i < this.attachments.Num(); i++) {
                ent = this.attachments.oGet(i).ent.GetEntity();
                if ((ent != null) && ent.spawnArgs.GetBool("remove")) {
                    ent.PostEventMS(EV_Remove, 0);
                }
            }
        }

        // copies animation from body to head joints
        protected void CopyJointsFromBodyToHead() {
            final idEntity headEnt = this.head.GetEntity();
            idAnimator headAnimator;
            int i;
            idMat3 mat;
            final idMat3 axis = new idMat3();
            final idVec3 pos = new idVec3();

            if (null == headEnt) {
                return;
            }

            headAnimator = headEnt.GetAnimator();

            // copy the animation from the body to the head
            for (i = 0; i < this.copyJoints.Num(); i++) {
                if (this.copyJoints.oGet(i).mod == JOINTMOD_WORLD_OVERRIDE) {
                    mat = headEnt.GetPhysics().GetAxis().Transpose();
                    GetJointWorldTransform(this.copyJoints.oGet(i).from[0], gameLocal.time, pos, axis);
                    pos.oMinSet(headEnt.GetPhysics().GetOrigin());
                    headAnimator.SetJointPos(this.copyJoints.oGet(i).to[0], this.copyJoints.oGet(i).mod, pos.oMultiply(mat));
                    headAnimator.SetJointAxis(this.copyJoints.oGet(i).to[0], this.copyJoints.oGet(i).mod, axis.oMultiply(mat));
                } else {
                    this.animator.GetJointLocalTransform(this.copyJoints.oGet(i).from[0], gameLocal.time, pos, axis);
                    headAnimator.SetJointPos(this.copyJoints.oGet(i).to[0], this.copyJoints.oGet(i).mod, pos);
                    headAnimator.SetJointAxis(this.copyJoints.oGet(i).to[0], this.copyJoints.oGet(i).mod, axis);
                }
            }
        }

        private void SyncAnimChannels(int channel, int syncToChannel, int blendFrames) {
            idAnimator headAnimator;
            idAFAttachment headEnt;
            int anim;
            idAnimBlend syncAnim;
            int starttime;
            int blendTime;
            int cycle;

            blendTime = FRAME2MS(blendFrames);
            if (channel == ANIMCHANNEL_HEAD) {
                headEnt = this.head.GetEntity();
                if (headEnt != null) {
                    headAnimator = headEnt.GetAnimator();
                    syncAnim = this.animator.CurrentAnim(syncToChannel);
                    if (syncAnim != null) {
                        anim = headAnimator.GetAnim(syncAnim.AnimFullName());
                        if (0 == anim) {
                            anim = headAnimator.GetAnim(syncAnim.AnimName());
                        }
                        if (anim != 0) {
                            cycle = this.animator.CurrentAnim(syncToChannel).GetCycleCount();
                            starttime = this.animator.CurrentAnim(syncToChannel).GetStartTime();
                            headAnimator.PlayAnim(ANIMCHANNEL_ALL, anim, gameLocal.time, blendTime);
                            headAnimator.CurrentAnim(ANIMCHANNEL_ALL).SetCycleCount(cycle);
                            headAnimator.CurrentAnim(ANIMCHANNEL_ALL).SetStartTime(starttime);
                        } else {
                            headEnt.PlayIdleAnim(blendTime);
                        }
                    }
                }
            } else if (syncToChannel == ANIMCHANNEL_HEAD) {
                headEnt = this.head.GetEntity();
                if (headEnt != null) {
                    headAnimator = headEnt.GetAnimator();
                    syncAnim = headAnimator.CurrentAnim(ANIMCHANNEL_ALL);
                    if (syncAnim != null) {
                        anim = GetAnim(channel, syncAnim.AnimFullName());
                        if (0 == anim) {
                            anim = GetAnim(channel, syncAnim.AnimName());
                        }
                        if (anim != 0) {
                            cycle = headAnimator.CurrentAnim(ANIMCHANNEL_ALL).GetCycleCount();
                            starttime = headAnimator.CurrentAnim(ANIMCHANNEL_ALL).GetStartTime();
                            this.animator.PlayAnim(channel, anim, gameLocal.time, blendTime);
                            this.animator.CurrentAnim(channel).SetCycleCount(cycle);
                            this.animator.CurrentAnim(channel).SetStartTime(starttime);
                        }
                    }
                }
            } else {
                this.animator.SyncAnimChannels(channel, syncToChannel, gameLocal.time, blendTime);
            }
        }

        private void FinishSetup() {
            final String[] scriptObjectName = {null};

            // setup script object
            if (this.spawnArgs.GetString("scriptobject", null, scriptObjectName)) {
                if (!this.scriptObject.SetType(scriptObjectName[0])) {
                    gameLocal.Error("Script object '%s' not found on entity '%s'.", scriptObjectName, this.name);
                }

                ConstructScriptObject();
            }

            SetupBody();
        }

        private void SetupHead() {
            idAFAttachment headEnt;
            String jointName;
            String headModel;
            int/*jointHandle_t*/ joint;
            int/*jointHandle_t*/ damageJoint;
            int i;
            idKeyValue sndKV;

            if (gameLocal.isClient) {
                return;
            }

            headModel = this.spawnArgs.GetString("def_head", "");
            if (!headModel.isEmpty()) {
                jointName = this.spawnArgs.GetString("head_joint");
                joint = this.animator.GetJointHandle(jointName);
                if (joint == INVALID_JOINT) {
                    gameLocal.Error("Joint '%s' not found for 'head_joint' on '%s'", jointName, this.name);
                }

                // set the damage joint to be part of the head damage group
                damageJoint = joint;
                for (i = 0; i < this.damageGroups.Num(); i++) {
                    final idStr d = this.damageGroups.oGet(i);
                    if ((d != null) && d.equals("head")) {
                        damageJoint = /*(jointHandle_t)*/ i;
                        break;
                    }
                }

                // copy any sounds in case we have frame commands on the head
                final idDict args = new idDict();
                sndKV = this.spawnArgs.MatchPrefix("snd_", null);
                while (sndKV != null) {
                    args.Set(sndKV.GetKey(), sndKV.GetValue());
                    sndKV = this.spawnArgs.MatchPrefix("snd_", sndKV);
                }

                headEnt = (idAFAttachment) gameLocal.SpawnEntityType(idAFAttachment.class, args);
                headEnt.SetName(va("%s_head", this.name));
                headEnt.SetBody(this, headModel, damageJoint);
                this.head.oSet(headEnt);

                idVec3 origin = new idVec3();
                final idMat3 axis = new idMat3();
                final idAttachInfo attach = this.attachments.Alloc();
                attach.channel = this.animator.GetChannelForJoint(joint);
                this.animator.GetJointTransform(joint, gameLocal.time, origin, axis);
                origin = this.renderEntity.origin.oPlus((origin.oPlus(this.modelOffset)).oMultiply(this.renderEntity.axis));
                attach.ent = new idEntityPtr<>();
                attach.ent.oSet(headEnt);
                headEnt.SetOrigin(origin);
                headEnt.SetAxis(this.renderEntity.axis);
                headEnt.BindToJoint(this, joint, true);
            }
        }

        private void PlayFootStepSound() {
            String sound = null;
            idMaterial material;

            if (!GetPhysics().HasGroundContacts()) {
                return;
            }

            // start footstep sound based on material type
            material = GetPhysics().GetContact(0).material;
            if (material != null) {
                sound = this.spawnArgs.GetString(va("snd_footstep_%s", gameLocal.sufaceTypeNames[etoi(material.GetSurfaceType())]));
            }
            if (sound.isEmpty()) {// == '\0' ) {
                sound = this.spawnArgs.GetString("snd_footstep");
            }
            if (!sound.isEmpty()) {// != '\0' ) {
                StartSoundShader(declManager.FindSound(sound), etoi(SND_CHANNEL_BODY), 0, false, null);
            }
        }

        private void Event_EnableEyeFocus() {
            String sound = null;
            idMaterial material;

            if (!GetPhysics().HasGroundContacts()) {
                return;
            }

            // start footstep sound based on material type
            material = GetPhysics().GetContact(0).material;
            if (material != null) {
                sound = this.spawnArgs.GetString(va("snd_footstep_%s", gameLocal.sufaceTypeNames[etoi(material.GetSurfaceType())]));
            }
            if (sound.isEmpty()) {// == '\0' ) {
                sound = this.spawnArgs.GetString("snd_footstep");
            }
            if (!sound.isEmpty()) {// != '\0' ) {
                StartSoundShader(declManager.FindSound(sound), etoi(SND_CHANNEL_BODY), 0, false, null);
            }
        }

        private void Event_DisableEyeFocus() {
            this.allowEyeFocus = false;

            final idEntity headEnt = this.head.GetEntity();
            if (headEnt != null) {
                headEnt.GetAnimator().Clear(ANIMCHANNEL_EYELIDS, gameLocal.time, FRAME2MS(2));
            } else {
                this.animator.Clear(ANIMCHANNEL_EYELIDS, gameLocal.time, FRAME2MS(2));
            }
        }

        private void Event_Footstep() {
            PlayFootStepSound();
        }

        private void Event_EnableWalkIK() {
            this.walkIK.EnableAll();
        }

        private void Event_DisableWalkIK() {
            this.walkIK.DisableAll();
        }

        private void Event_EnableLegIK(idEventArg<Integer> num) {
            this.walkIK.EnableLeg(num.value);
        }

        private void Event_DisableLegIK(idEventArg<Integer> num) {
            this.walkIK.DisableLeg(num.value);
        }

        private void Event_SetAnimPrefix(final idEventArg<String> prefix) {
            this.animPrefix.oSet(prefix.value);
        }

//        private void Event_LookAtEntity(idEntity ent, float duration);
        private void Event_PreventPain(idEventArg<Float> duration) {
            this.painTime = (int) (gameLocal.time + SEC2MS(duration.value));
        }

        private void Event_DisablePain() {
            this.allowPain = false;
        }

        private void Event_EnablePain() {
            this.allowPain = true;
        }

        private void Event_GetPainAnim() {
            if (0 == this.painAnim.Length()) {
                idThread.ReturnString("pain");
            } else {
                idThread.ReturnString(this.painAnim);
            }
        }

        private void Event_StopAnim(idEventArg<Integer> channel, idEventArg<Integer> frames) {
            switch (channel.value) {
                case ANIMCHANNEL_HEAD:
                    this.headAnim.StopAnim(frames.value);
                    break;

                case ANIMCHANNEL_TORSO:
                    this.torsoAnim.StopAnim(frames.value);
                    break;

                case ANIMCHANNEL_LEGS:
                    this.legsAnim.StopAnim(frames.value);
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
                    break;
            }
        }

        private void Event_PlayAnim(idEventArg<Integer> _channel, final idEventArg<String> _animName) {
            final int channel = _channel.value;
            final String animName = _animName.value;
            animFlags_t flags;
            idEntity headEnt;
            int anim;

            anim = GetAnim(channel, animName);
            if (0 == anim) {
                if ((channel == ANIMCHANNEL_HEAD) && (this.head.GetEntity() != null)) {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, this.name.getData(), this.spawnArgs.GetString("def_head", ""));
                } else {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, this.name.getData(), GetEntityDefName());
                }
                idThread.ReturnInt(0);
                return;
            }

            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    headEnt = this.head.GetEntity();
                    if (headEnt != null) {
                        this.headAnim.idleAnim = false;
                        this.headAnim.PlayAnim(anim);
                        flags = this.headAnim.GetAnimFlags();
                        if (!flags.prevent_idle_override) {
                            if (this.torsoAnim.IsIdle()) {
                                this.torsoAnim.animBlendFrames = this.headAnim.lastAnimBlendFrames;
                                SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_HEAD, this.headAnim.lastAnimBlendFrames);
                                if (this.legsAnim.IsIdle()) {
                                    this.legsAnim.animBlendFrames = this.headAnim.lastAnimBlendFrames;
                                    SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_HEAD, this.headAnim.lastAnimBlendFrames);
                                }
                            }
                        }
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    this.torsoAnim.idleAnim = false;
                    this.torsoAnim.PlayAnim(anim);
                    flags = this.torsoAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (this.headAnim.IsIdle()) {
                            this.headAnim.animBlendFrames = this.torsoAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                        }
                        if (this.legsAnim.IsIdle()) {
                            this.legsAnim.animBlendFrames = this.torsoAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                        }
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    this.legsAnim.idleAnim = false;
                    this.legsAnim.PlayAnim(anim);
                    flags = this.legsAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (this.torsoAnim.IsIdle()) {
                            this.torsoAnim.animBlendFrames = this.legsAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, this.legsAnim.lastAnimBlendFrames);
                            if (this.headAnim.IsIdle()) {
                                this.headAnim.animBlendFrames = this.legsAnim.lastAnimBlendFrames;
                                SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, this.legsAnim.lastAnimBlendFrames);
                            }
                        }
                    }
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
                    break;
            }
            idThread.ReturnInt(1);
        }

        private void Event_PlayCycle(idEventArg<Integer> _channel, final idEventArg<String> _animName) {
            final int channel = _channel.value;
            final String animName = _animName.value;
            animFlags_t flags;
            int anim;

            anim = GetAnim(channel, animName);
            if (0 == anim) {
                if ((channel == ANIMCHANNEL_HEAD) && (this.head.GetEntity() != null)) {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, this.name, this.spawnArgs.GetString("def_head", ""));
                } else {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, this.name, GetEntityDefName());
                }
                idThread.ReturnInt(false);
                return;
            }

            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    this.headAnim.idleAnim = false;
                    this.headAnim.CycleAnim(anim);
                    flags = this.headAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (this.torsoAnim.IsIdle() && this.legsAnim.IsIdle()) {
                            this.torsoAnim.animBlendFrames = this.headAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_HEAD, this.headAnim.lastAnimBlendFrames);
                            this.legsAnim.animBlendFrames = this.headAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_HEAD, this.headAnim.lastAnimBlendFrames);
                        }
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    this.torsoAnim.idleAnim = false;
                    this.torsoAnim.CycleAnim(anim);
                    flags = this.torsoAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (this.headAnim.IsIdle()) {
                            this.headAnim.animBlendFrames = this.torsoAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                        }
                        if (this.legsAnim.IsIdle()) {
                            this.legsAnim.animBlendFrames = this.torsoAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                        }
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    this.legsAnim.idleAnim = false;
                    this.legsAnim.CycleAnim(anim);
                    flags = this.legsAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (this.torsoAnim.IsIdle()) {
                            this.torsoAnim.animBlendFrames = this.legsAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, this.legsAnim.lastAnimBlendFrames);
                            if (this.headAnim.IsIdle()) {
                                this.headAnim.animBlendFrames = this.legsAnim.lastAnimBlendFrames;
                                SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, this.legsAnim.lastAnimBlendFrames);
                            }
                        }
                    }
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
            }

            idThread.ReturnInt(true);
        }

        private void Event_IdleAnim(idEventArg<Integer> _channel, final idEventArg<String> _animName) {
            final int channel = _channel.value;
            final String animName = _animName.value;
            int anim;

            anim = GetAnim(channel, animName);
            if (0 == anim) {
                if ((channel == ANIMCHANNEL_HEAD) && (this.head.GetEntity() != null)) {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, this.name, this.spawnArgs.GetString("def_head", ""));
                } else {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, this.name, GetEntityDefName());
                }

                switch (channel) {
                    case ANIMCHANNEL_HEAD:
                        this.headAnim.BecomeIdle();
                        break;

                    case ANIMCHANNEL_TORSO:
                        this.torsoAnim.BecomeIdle();
                        break;

                    case ANIMCHANNEL_LEGS:
                        this.legsAnim.BecomeIdle();
                        break;

                    default:
                        gameLocal.Error("Unknown anim group");
                }

                idThread.ReturnInt(false);
                return;
            }

            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    this.headAnim.BecomeIdle();
                    if (this.torsoAnim.GetAnimFlags().prevent_idle_override) {
                        // don't sync to torso body if it doesn't override idle anims
                        this.headAnim.CycleAnim(anim);
                    } else if (this.torsoAnim.IsIdle() && this.legsAnim.IsIdle()) {
                        // everything is idle, so play the anim on the head and copy it to the torso and legs
                        this.headAnim.CycleAnim(anim);
                        this.torsoAnim.animBlendFrames = this.headAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_HEAD, this.headAnim.lastAnimBlendFrames);
                        this.legsAnim.animBlendFrames = this.headAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_HEAD, this.headAnim.lastAnimBlendFrames);
                    } else if (this.torsoAnim.IsIdle()) {
                        // sync the head and torso to the legs
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, this.headAnim.animBlendFrames);
                        this.torsoAnim.animBlendFrames = this.headAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, this.torsoAnim.animBlendFrames);
                    } else {
                        // sync the head to the torso
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, this.headAnim.animBlendFrames);
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    this.torsoAnim.BecomeIdle();
                    if (this.legsAnim.GetAnimFlags().prevent_idle_override) {
                        // don't sync to legs if legs anim doesn't override idle anims
                        this.torsoAnim.CycleAnim(anim);
                    } else if (this.legsAnim.IsIdle()) {
                        // play the anim in both legs and torso
                        this.torsoAnim.CycleAnim(anim);
                        this.legsAnim.animBlendFrames = this.torsoAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                    } else {
                        // sync the anim to the legs
                        SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, this.torsoAnim.animBlendFrames);
                    }

                    if (this.headAnim.IsIdle()) {
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    this.legsAnim.BecomeIdle();
                    if (this.torsoAnim.GetAnimFlags().prevent_idle_override) {
                        // don't sync to torso if torso anim doesn't override idle anims
                        this.legsAnim.CycleAnim(anim);
                    } else if (this.torsoAnim.IsIdle()) {
                        // play the anim in both legs and torso
                        this.legsAnim.CycleAnim(anim);
                        this.torsoAnim.animBlendFrames = this.legsAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, this.legsAnim.lastAnimBlendFrames);
                        if (this.headAnim.IsIdle()) {
                            SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, this.legsAnim.lastAnimBlendFrames);
                        }
                    } else {
                        // sync the anim to the torso
                        SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, this.legsAnim.animBlendFrames);
                    }
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
            }

            idThread.ReturnInt(true);
        }

        private void Event_SetSyncedAnimWeight(idEventArg<Integer> _channel, final idEventArg<Integer> _anim, final idEventArg<Float> _weight) {
            final int channel = _channel.value;
            final int anim = _anim.value;
            final float weight = _weight.value;
            idEntity headEnt;

            headEnt = this.head.GetEntity();
            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    if (headEnt != null) {
                        this.animator.CurrentAnim(ANIMCHANNEL_ALL).SetSyncedAnimWeight(anim, weight);
                    } else {
                        this.animator.CurrentAnim(ANIMCHANNEL_HEAD).SetSyncedAnimWeight(anim, weight);
                    }
                    if (this.torsoAnim.IsIdle()) {
                        this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(anim, weight);
                        if (this.legsAnim.IsIdle()) {
                            this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(anim, weight);
                        }
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(anim, weight);
                    if (this.legsAnim.IsIdle()) {
                        this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(anim, weight);
                    }
                    if ((headEnt != null) && this.headAnim.IsIdle()) {
                        this.animator.CurrentAnim(ANIMCHANNEL_ALL).SetSyncedAnimWeight(anim, weight);
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(anim, weight);
                    if (this.torsoAnim.IsIdle()) {
                        this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(anim, weight);
                        if ((headEnt != null) && this.headAnim.IsIdle()) {
                            this.animator.CurrentAnim(ANIMCHANNEL_ALL).SetSyncedAnimWeight(anim, weight);
                        }
                    }
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
            }
        }

        private void Event_OverrideAnim(idEventArg<Integer> channel) {
            switch (channel.value) {
                case ANIMCHANNEL_HEAD:
                    this.headAnim.Disable();
                    if (!this.torsoAnim.IsIdle()) {
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                    } else {
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, this.legsAnim.lastAnimBlendFrames);
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    this.torsoAnim.Disable();
                    SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, this.legsAnim.lastAnimBlendFrames);
                    if (this.headAnim.IsIdle()) {
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    this.legsAnim.Disable();
                    SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, this.torsoAnim.lastAnimBlendFrames);
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
                    break;
            }
        }

        private void Event_EnableAnim(idEventArg<Integer> channel, idEventArg<Integer> _blendFrames) {
            final int blendFrames = _blendFrames.value;
            switch (channel.value) {
                case ANIMCHANNEL_HEAD:
                    this.headAnim.Enable(blendFrames);
                    break;

                case ANIMCHANNEL_TORSO:
                    this.torsoAnim.Enable(blendFrames);
                    break;

                case ANIMCHANNEL_LEGS:
                    this.legsAnim.Enable(blendFrames);
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
                    break;
            }
        }

        private void Event_SetBlendFrames(idEventArg<Integer> _channel, final idEventArg<Integer> _blendFrames) {
            final int channel = _channel.value;
            final int blendFrames = _blendFrames.value;
            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    this.headAnim.animBlendFrames = blendFrames;
                    this.headAnim.lastAnimBlendFrames = blendFrames;
                    break;

                case ANIMCHANNEL_TORSO:
                    this.torsoAnim.animBlendFrames = blendFrames;
                    this.torsoAnim.lastAnimBlendFrames = blendFrames;
                    break;

                case ANIMCHANNEL_LEGS:
                    this.legsAnim.animBlendFrames = blendFrames;
                    this.legsAnim.lastAnimBlendFrames = blendFrames;
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
                    break;
            }
        }

        private void Event_GetBlendFrames(idEventArg<Integer> _channel) {
            final int channel = _channel.value;
            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    idThread.ReturnInt(this.headAnim.animBlendFrames);
                    break;

                case ANIMCHANNEL_TORSO:
                    idThread.ReturnInt(this.torsoAnim.animBlendFrames);
                    break;

                case ANIMCHANNEL_LEGS:
                    idThread.ReturnInt(this.legsAnim.animBlendFrames);
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
                    break;
            }
        }

        private void Event_AnimState(idEventArg<Integer> channel, final idEventArg<String> statename, idEventArg<Integer> blendFrames) {
            SetAnimState(channel.value, statename.value, blendFrames.value);
        }

        private void Event_GetAnimState(idEventArg<Integer> channel) {
            final idStr state;

            state = GetAnimState(channel.value);
            idThread.ReturnString(state);
        }

        private void Event_InAnimState(idEventArg<Integer> channel, final idEventArg<String> statename) {
            boolean instate;

            instate = InAnimState(channel.value, statename.value);
            idThread.ReturnInt(instate);
        }

        private void Event_FinishAction(final idEventArg<String> actionname) {
            if (this.waitState.equals(actionname.value)) {
                SetWaitState("");
            }
        }

        private void Event_AnimDone(idEventArg<Integer> channel, idEventArg<Integer> _blendFrames) {
            final int blendFrames = _blendFrames.value;
            boolean result;

            switch (channel.value) {
                case ANIMCHANNEL_HEAD:
                    result = this.headAnim.AnimDone(blendFrames);
                    idThread.ReturnInt(result);
                    break;

                case ANIMCHANNEL_TORSO:
                    result = this.torsoAnim.AnimDone(blendFrames);
                    idThread.ReturnInt(result);
                    break;

                case ANIMCHANNEL_LEGS:
                    result = this.legsAnim.AnimDone(blendFrames);
                    idThread.ReturnInt(result);
                    break;

                default:
                    gameLocal.Error("Unknown anim group");
            }
        }

        private void Event_HasAnim(idEventArg<Integer> channel, final idEventArg<String> animName) {
            if (GetAnim(channel.value, animName.value) != 0) {
                idThread.ReturnFloat(1.0f);
            } else {
                idThread.ReturnFloat(0);
            }
        }

        private void Event_CheckAnim(idEventArg<Integer> channel, final idEventArg<String> animname) {
            if (0 == GetAnim(channel.value, animname.value)) {
                if (this.animPrefix.Length() != 0) {
                    gameLocal.Error("Can't find anim '%s_%s' for '%s'", this.animPrefix, animname, this.name);
                } else {
                    gameLocal.Error("Can't find anim '%s' for '%s'", animname, this.name);
                }
            }
        }

        private void Event_ChooseAnim(idEventArg<Integer> channel, final idEventArg<String> animname) {
            int anim;

            anim = GetAnim(channel.value, animname.value);
            if (anim != 0) {
                if (channel.value == ANIMCHANNEL_HEAD) {
                    if (this.head.GetEntity() != null) {
                        idThread.ReturnString(this.head.GetEntity().GetAnimator().AnimFullName(anim));
                        return;
                    }
                } else {
                    idThread.ReturnString(this.animator.AnimFullName(anim));
                    return;
                }
            }

            idThread.ReturnString("");
        }

        private void Event_AnimLength(idEventArg<Integer> channel, final idEventArg<String> animname) {
            int anim;

            anim = GetAnim(channel.value, animname.value);
            if (anim != 0) {
                if (channel.value == ANIMCHANNEL_HEAD) {
                    if (this.head.GetEntity() != null) {
                        idThread.ReturnFloat(MS2SEC(this.head.GetEntity().GetAnimator().AnimLength(anim)));
                        return;
                    }
                } else {
                    idThread.ReturnFloat(MS2SEC(this.animator.AnimLength(anim)));
                    return;
                }
            }

            idThread.ReturnFloat(0);
        }

        private void Event_AnimDistance(idEventArg<Integer> channel, final idEventArg<String> animname) {
            int anim;

            anim = GetAnim(channel.value, animname.value);
            if (anim != 0) {
                if (channel.value == ANIMCHANNEL_HEAD) {
                    if (this.head.GetEntity() != null) {
                        idThread.ReturnFloat(this.head.GetEntity().GetAnimator().TotalMovementDelta(anim).Length());
                        return;
                    }
                } else {
                    idThread.ReturnFloat(this.animator.TotalMovementDelta(anim).Length());
                    return;
                }
            }

            idThread.ReturnFloat(0);
        }

        private void Event_HasEnemies() {
            boolean hasEnemy;

            hasEnemy = HasEnemies();
            idThread.ReturnInt(hasEnemy);
        }

        private void Event_NextEnemy(idEventArg<idEntity> _ent) {
            final idEntity ent = _ent.value;
            idActor actor;

            if ((null == ent) || (ent.equals(this))) {
                actor = this.enemyList.Next();
            } else {
                if (!ent.IsType(idActor.class)) {
                    gameLocal.Error("'%s' cannot be an enemy", ent.name);
                }

                actor = (idActor) ent;
                if (actor.enemyNode.ListHead() != this.enemyList) {
                    gameLocal.Error("'%s' is not in '%s' enemy list", actor.name, this.name);
                }
            }

            for (; actor != null; actor = actor.enemyNode.Next()) {
                if (!actor.fl.hidden) {
                    idThread.ReturnEntity(actor);
                    return;
                }
            }

            idThread.ReturnEntity(null);
        }

        private void Event_ClosestEnemyToPoint(final idEventArg<idVec3> pos) {
            final idActor bestEnt = ClosestEnemyToPoint(pos.value);
            idThread.ReturnEntity(bestEnt);
        }

        private void Event_StopSound(idEventArg<Integer> channel, idEventArg<Integer> netSync) {
            if (channel.value == etoi(SND_CHANNEL_VOICE)) {
                final idEntity headEnt = this.head.GetEntity();
                if (headEnt != null) {
                    headEnt.StopSound(channel.value, (netSync.value != 0));
                }
            }
            StopSound(channel.value, (netSync.value != 0));
        }

        private void Event_SetNextState(final idEventArg<String> name) {
            this.idealState = GetScriptFunction(name.value);
            if (this.idealState == this.state) {
                this.state = null;
            }
        }

        private void Event_SetState(final idEventArg<String> name) {
            this.idealState = GetScriptFunction(name.value);
            if (this.idealState == this.state) {
                this.state = null;
            }
            this.scriptThread.DoneProcessing();
        }

        private void Event_GetState() {
            if (this.state != null) {
                idThread.ReturnString(this.state.Name());
            } else {
                idThread.ReturnString("");
            }
        }

        private void Event_GetHead() {
            idThread.ReturnEntity(this.head.GetEntity());
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
            int i;
            idEntity ent;

            DeconstructScriptObject();
            this.scriptObject.Free();

            StopSound(SND_CHANNEL_ANY.ordinal(), false);

            idClipModel.delete(this.combatModel);
            this.combatModel = null;

            if (this.head.GetEntity() != null) {
                this.head.GetEntity().ClearBody();
                this.head.GetEntity().PostEventMS(EV_Remove, 0);
            }

            // remove any attached entities
            for (i = 0; i < this.attachments.Num(); i++) {
                ent = this.attachments.oGet(i).ent.GetEntity();
                if (ent != null) {
                    ent.PostEventMS(EV_Remove, 0);
                }
            }

            ShutdownThreads();

            super._deconstructor();
        }
    }
}
