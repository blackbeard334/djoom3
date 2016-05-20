package neo.Game;

import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.AFEntity.idAFEntity_Gibbable;
import neo.Game.AI.AAS.idAAS;
import neo.Game.Actor.idActor;
import static neo.Game.Animation.Anim.ANIMCHANNEL_ALL;
import static neo.Game.Animation.Anim.ANIMCHANNEL_EYELIDS;
import static neo.Game.Animation.Anim.ANIMCHANNEL_HEAD;
import static neo.Game.Animation.Anim.ANIMCHANNEL_LEGS;
import static neo.Game.Animation.Anim.ANIMCHANNEL_TORSO;
import static neo.Game.Animation.Anim.FRAME2MS;
import neo.Game.Animation.Anim.animFlags_t;
import neo.Game.Animation.Anim.jointModTransform_t;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_LOCAL_OVERRIDE;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD_OVERRIDE;
import neo.Game.Animation.Anim_Blend.idAnimBlend;
import neo.Game.Animation.Anim_Blend.idAnimator;

import static neo.Game.Entity.EV_StopSound;
import static neo.Game.Entity.TH_PHYSICS;
import neo.Game.Entity.idEntity;
import static neo.Game.GameSys.Class.EV_Remove;

import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t3;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Class.idEventFunc;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.ai_debugScript;
import static neo.Game.GameSys.SysCvar.g_debugDamage;
import static neo.Game.Game_local.MASK_OPAQUE;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_VOICE;
import neo.Game.Game_local.idEntityPtr;
import static neo.Game.IK.IK_ANIM;
import neo.Game.IK.idIK_Walk;
import neo.Game.Item.idMoveableItem;
import neo.Game.Light.idLight;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Projectile.idSoulCubeMissile;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_FLESH;
import static neo.Renderer.Model.INVALID_JOINT;
import neo.Renderer.RenderWorld.renderView_s;
import static neo.TempDump.NOT;
import static neo.TempDump.atof;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_WALK;
import static neo.framework.DeclManager.declManager;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.LinkList.idLinkList;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

import java.util.HashMap;
import java.util.Map;

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

        public boolean idleAnim;
        public idStr   state;
        public int     animBlendFrames;
        public int     lastAnimBlendFrames;        // allows override anims to blend based on the last transition time
        //
        //

        public idAnimState() {
            self = null;
            animator = null;
            thread = null;
            idleAnim = true;
            disabled = true;
            channel = ANIMCHANNEL_ALL;
            animBlendFrames = 0;
            lastAnimBlendFrames = 0;
        }
        // ~idAnimState();

        public void Save(idSaveGame savefile) {

            savefile.WriteObject(self);

            // Save the entity owner of the animator
            savefile.WriteObject(animator.GetEntity());

            savefile.WriteObject(thread);

            savefile.WriteString(state);

            savefile.WriteInt(animBlendFrames);
            savefile.WriteInt(lastAnimBlendFrames);
            savefile.WriteInt(channel);
            savefile.WriteBool(idleAnim);
            savefile.WriteBool(disabled);
        }

        public void Restore(idRestoreGame savefile) {
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/self);

            idEntity animOwner = new idEntity();
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/animOwner);
            if (animOwner != null) {
                animator = animOwner.GetAnimator();
            }

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/thread);

            savefile.ReadString(state);

            animBlendFrames = savefile.ReadInt();
            lastAnimBlendFrames = savefile.ReadInt();
            channel = savefile.ReadInt();
            idleAnim = savefile.ReadBool();
            disabled = savefile.ReadBool();
        }

        public void Init(idActor owner, idAnimator _animator, int animchannel) {
            assert (owner != null);
            assert (_animator != null);
            self = owner;
            animator = _animator;
            channel = animchannel;

            if (NOT(thread)) {
                thread = new idThread();
                thread.ManualDelete();
            }
            thread.EndThread();
            thread.ManualControl();
        }

        public void Shutdown() {
//	delete thread;
            thread = null;
        }

        public void SetState(final String statename, int blendFrames) {
            function_t func;

            func = self.scriptObject.GetFunction(statename);
            if (null == func) {
                assert (false);
                gameLocal.Error("Can't find function '%s' in object '%s'", statename, self.scriptObject.GetTypeName());
            }

            state.oSet(statename);
            disabled = false;
            animBlendFrames = blendFrames;
            lastAnimBlendFrames = blendFrames;
            thread.CallFunction(self, func, true);

            animBlendFrames = blendFrames;
            lastAnimBlendFrames = blendFrames;
            disabled = false;
            idleAnim = false;

            if (ai_debugScript.GetInteger() == self.entityNumber) {
                gameLocal.Printf("%d: %s: Animstate: %s\n", gameLocal.time, self.name, state);
            }
        }

        public void StopAnim(int frames) {
            animBlendFrames = 0;
            animator.Clear(channel, gameLocal.time, FRAME2MS(frames));
        }

        public void PlayAnim(int anim) {
            if (anim != 0) {
                animator.PlayAnim(channel, anim, gameLocal.time, FRAME2MS(animBlendFrames));
            }
            animBlendFrames = 0;
        }

        public void CycleAnim(int anim) {
            if (anim != 0) {
                animator.CycleAnim(channel, anim, gameLocal.time, FRAME2MS(animBlendFrames));
            }
            animBlendFrames = 0;
        }

        public void BecomeIdle() {
            idleAnim = true;
        }

        public boolean UpdateState() {
            if (disabled) {
                return false;
            }

            if (ai_debugScript.GetInteger() == self.entityNumber) {
                thread.EnableDebugInfo();
            } else {
                thread.DisableDebugInfo();
            }

            thread.Execute();

            return true;
        }

        public boolean Disabled() {
            return disabled;
        }

        public void Enable(int blendFrames) {
            if (disabled) {
                disabled = false;
                animBlendFrames = blendFrames;
                lastAnimBlendFrames = blendFrames;
                if (state.Length() != 0) {
                    SetState(state.toString(), blendFrames);
                }
            }
        }

        public void Disable() {
            disabled = true;
            idleAnim = false;
        }

        public boolean AnimDone(int blendFrames) {
            int animDoneTime;

            animDoneTime = animator.CurrentAnim(channel).GetEndTime();
            if (animDoneTime < 0) {
                // playing a cycle
                return false;
            } else if (animDoneTime - FRAME2MS(blendFrames) <= gameLocal.time) {
                return true;
            } else {
                return false;
            }
        }

        public boolean IsIdle() {
            return disabled || idleAnim;
        }

        public animFlags_t GetAnimFlags() {
            animFlags_t flags = new animFlags_t();

//            memset(flags, 0, sizeof(flags));
            if (!disabled && !AnimDone(0)) {
                flags = animator.GetAnimFlags(animator.CurrentAnim(channel).AnimNum());
            }

            return flags;
        }
//
//
        private idActor self;
        private idAnimator animator;
        private idThread thread;
        private int channel;
        private boolean disabled;
    };

    public static class idAttachInfo {

        public idEntityPtr<idEntity> ent;
        public int channel;

        public idAttachInfo() {
            this.ent = new idEntityPtr<>();
        }
    };

    public static class copyJoints_t {

        public jointModTransform_t    mod;
        public int[]/*jointHandle_t*/ from = {0};
        public int[]/*jointHandle_t*/ to = {0};
    };

    /* **********************************************************************

     idActor

     ***********************************************************************/
    public static class idActor extends idAFEntity_Gibbable {
        //public	CLASS_PROTOTYPE( idActor );

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
            viewAxis = idMat3.getMat3_identity();

            scriptThread = null;		// initialized by ConstructScriptObject, which is called by idEntity::Spawn

            use_combat_bbox = false;
            head = new idEntityPtr<>();

            team = 0;
            rank = 0;
            fovDot = 0;
            eyeOffset = new idVec3();
            pain_debounce_time = 0;
            pain_delay = 0;
            pain_threshold = 0;
            
            copyJoints = new idList<>();

            state = null;
            idealState = null;

            leftEyeJoint = INVALID_JOINT;
            rightEyeJoint = INVALID_JOINT;
            soundJoint = INVALID_JOINT;

            modelOffset = new idVec3();
            deltaViewAngles = new idAngles();

            painTime = 0;
            allowPain = false;
            allowEyeFocus = false;
            
            damageGroups = new idStrList();
            damageScale = new idList<>();

            waitState = new idStr();
            headAnim = new idAnimState();
            torsoAnim = new idAnimState();
            legsAnim = new idAnimState();
            
            walkIK = new idIK_Walk();
            
            animPrefix = new idStr();
            painAnim = new idStr();

            blink_anim = 0;//null;
            blink_time = 0;
            blink_min = 0;
            blink_max = 0;

            finalBoss = false;

            attachments.SetGranularity(1);

            enemyNode = new idLinkList<>(this);
            enemyList = new idLinkList<>(this);
        }
        // virtual					~idActor( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            idEntity[] ent = {null};
            idStr jointName = new idStr();
            float[] fovDegrees = {0};
            copyJoints_t copyJoint = new copyJoints_t();
            int[] rank = {0}, team = {0};
            boolean[] use_combat_bbox = {false};

            animPrefix.oSet("");
            state = null;
            idealState = null;

            spawnArgs.GetInt("rank", "0", rank);
            spawnArgs.GetInt("team", "0", team);
            this.rank = rank[0];
            this.team = team[0];

            spawnArgs.GetVector("offsetModel", "0 0 0", modelOffset);

            spawnArgs.GetBool("use_combat_bbox", "0", use_combat_bbox);
            this.use_combat_bbox = use_combat_bbox[0];

            viewAxis.oSet(GetPhysics().GetAxis());

            spawnArgs.GetFloat("fov", "90", fovDegrees);
            SetFOV(fovDegrees[0]);

            pain_debounce_time = 0;

            pain_delay = (int) SEC2MS(spawnArgs.GetFloat("pain_delay"));
            pain_threshold = spawnArgs.GetInt("pain_threshold");

            LoadAF();

            walkIK.Init(this, IK_ANIM, modelOffset);

            // the animation used to be set to the IK_ANIM at this point, but that was fixed, resulting in
            // attachments not binding correctly, so we're stuck setting the IK_ANIM before attaching things.
            animator.ClearAllAnims(gameLocal.time, 0);
            animator.SetFrame(ANIMCHANNEL_ALL, animator.GetAnim(IK_ANIM), 0, 0, 0);

            // spawn any attachments we might have
            idKeyValue kv = spawnArgs.MatchPrefix("def_attach", null);
            while (kv != null) {
                idDict args = new idDict();

                args.Set("classname", kv.GetValue());

                // make items non-touchable so the player can't take them out of the character's hands
                args.Set("no_touch", "1");

                // don't let them drop to the floor
                args.Set("dropToFloor", "0");

                gameLocal.SpawnEntityDef(args, ent);
                if (NOT(ent[0])) {
                    gameLocal.Error("Couldn't spawn '%s' to attach to entity '%s'", kv.GetValue(), name);
                } else {
                    Attach(ent[0]);
                }
                kv = spawnArgs.MatchPrefix("def_attach", kv);
            }

            SetupDamageGroups();
            SetupHead();

            // clear the bind anim
            animator.ClearAllAnims(gameLocal.time, 0);

            idEntity headEnt = head.GetEntity();
            idAnimator headAnimator;
            if (headEnt != null) {
                headAnimator = headEnt.GetAnimator();
            } else {
                headAnimator = animator;
            }

            if (headEnt != null) {
                // set up the list of joints to copy to the head
                for (kv = spawnArgs.MatchPrefix("copy_joint", null); kv != null; kv = spawnArgs.MatchPrefix("copy_joint", kv)) {
                    if (kv.GetValue().IsEmpty()) {
                        // probably clearing out inherited key, so skip it
                        continue;
                    }

                    jointName.oSet(kv.GetKey());
                    if (jointName.StripLeadingOnce("copy_joint_world ")) {
                        copyJoint.mod = JOINTMOD_WORLD_OVERRIDE;
                    } else {
                        jointName.StripLeadingOnce("copy_joint ");
                        copyJoint.mod = JOINTMOD_LOCAL_OVERRIDE;
                    }

                    copyJoint.from[0] = animator.GetJointHandle(jointName);
                    if (copyJoint.from[0] == INVALID_JOINT) {
                        gameLocal.Warning("Unknown copy_joint '%s' on entity %s", jointName, name);
                        continue;
                    }

                    jointName.oSet(kv.GetValue());
                    copyJoint.to[0] = headAnimator.GetJointHandle(jointName);
                    if (copyJoint.to[0] == INVALID_JOINT) {
                        gameLocal.Warning("Unknown copy_joint '%s' on head of entity %s", jointName, name);
                        continue;
                    }

                    copyJoints.Append(copyJoint);
                }
            }

            // set up blinking
            blink_anim = headAnimator.GetAnim("blink");
            blink_time = 0;	// it's ok to blink right away
            blink_min = (int) SEC2MS(spawnArgs.GetFloat("blink_min", "0.5"));
            blink_max = (int) SEC2MS(spawnArgs.GetFloat("blink_max", "8"));

            // set up the head anim if necessary
            int headAnim = headAnimator.GetAnim("def_head");
            if (headAnim != 0) {
                if (headEnt != null) {
                    headAnimator.CycleAnim(ANIMCHANNEL_ALL, headAnim, gameLocal.time, 0);
                } else {
                    headAnimator.CycleAnim(ANIMCHANNEL_HEAD, headAnim, gameLocal.time, 0);
                }
            }

            if (spawnArgs.GetString("sound_bone", "", jointName)) {
                soundJoint = animator.GetJointHandle(jointName);
                if (soundJoint == INVALID_JOINT) {
                    gameLocal.Warning("idAnimated '%s' at (%s): cannot find joint '%s' for sound playback", name, GetPhysics().GetOrigin().ToString(0), jointName);
                }
            }

            finalBoss = spawnArgs.GetBool("finalBoss");

            FinishSetup();
        }

        public void Restart() {
            assert (NOT(head.GetEntity()));
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

            savefile.WriteInt(team);
            savefile.WriteInt(rank);
            savefile.WriteMat3(viewAxis);

            savefile.WriteInt(enemyList.Num());
            for (ent = enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
                savefile.WriteObject(ent);
            }

            savefile.WriteFloat(fovDot);
            savefile.WriteVec3(eyeOffset);
            savefile.WriteVec3(modelOffset);
            savefile.WriteAngles(deltaViewAngles);

            savefile.WriteInt(pain_debounce_time);
            savefile.WriteInt(pain_delay);
            savefile.WriteInt(pain_threshold);

            savefile.WriteInt(damageGroups.Num());
            for (i = 0; i < damageGroups.Num(); i++) {
                savefile.WriteString(damageGroups.oGet(i));
            }

            savefile.WriteInt(damageScale.Num());
            for (i = 0; i < damageScale.Num(); i++) {
                savefile.WriteFloat(damageScale.oGet(i));
            }

            savefile.WriteBool(use_combat_bbox);
            head.Save(savefile);

            savefile.WriteInt(copyJoints.Num());
            for (i = 0; i < copyJoints.Num(); i++) {
                savefile.WriteInt(etoi(copyJoints.oGet(i).mod));
                savefile.WriteJoint(copyJoints.oGet(i).from[0]);
                savefile.WriteJoint(copyJoints.oGet(i).to[0]);
            }

            savefile.WriteJoint(leftEyeJoint);
            savefile.WriteJoint(rightEyeJoint);
            savefile.WriteJoint(soundJoint);

            walkIK.Save(savefile);

            savefile.WriteString(animPrefix);
            savefile.WriteString(painAnim);

            savefile.WriteInt(blink_anim);
            savefile.WriteInt(blink_time);
            savefile.WriteInt(blink_min);
            savefile.WriteInt(blink_max);

            // script variables
            savefile.WriteObject(scriptThread);

            savefile.WriteString(waitState);

            headAnim.Save(savefile);
            torsoAnim.Save(savefile);
            legsAnim.Save(savefile);

            savefile.WriteBool(allowPain);
            savefile.WriteBool(allowEyeFocus);

            savefile.WriteInt(painTime);

            savefile.WriteInt(attachments.Num());
            for (i = 0; i < attachments.Num(); i++) {
                attachments.oGet(i).ent.Save(savefile);
                savefile.WriteInt(attachments.oGet(i).channel);
            }

            savefile.WriteBool(finalBoss);

            idToken token = new idToken();

            //FIXME: this is unneccesary
            if (state != null) {
                idLexer src = new idLexer(state.Name(), state.Name().length(), "idAI::Save");

                src.ReadTokenOnLine(token);
                src.ExpectTokenString("::");
                src.ReadTokenOnLine(token);

                savefile.WriteString(token);
            } else {
                savefile.WriteString("");
            }

            if (idealState != null) {
                idLexer src = new idLexer(idealState.Name(), idealState.Name().length(), "idAI::Save");

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
            int[] num = {0};
            idActor ent = new idActor();

            team = savefile.ReadInt();
            rank = savefile.ReadInt();
            savefile.ReadMat3(viewAxis);

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadObject(/*reinterpret_cast<idClass *&>*/ent);
                assert (ent != null);
                if (ent != null) {
                    ent.enemyNode.AddToEnd(enemyList);
                }
            }

            fovDot = savefile.ReadFloat();
            savefile.ReadVec3(eyeOffset);
            savefile.ReadVec3(modelOffset);
            savefile.ReadAngles(deltaViewAngles);

            pain_debounce_time = savefile.ReadInt();
            pain_delay = savefile.ReadInt();
            pain_threshold = savefile.ReadInt();

            savefile.ReadInt(num);
            damageGroups.SetGranularity(1);
            damageGroups.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadString(damageGroups.oGet(i));
            }

            savefile.ReadInt(num);
            damageScale.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                damageScale.oSet(i, savefile.ReadFloat());
            }

            use_combat_bbox = savefile.ReadBool();
            head.Restore(savefile);

            savefile.ReadInt(num);
            copyJoints.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                int[] val = {0};
                savefile.ReadInt(val);
                copyJoints.oGet(i).mod = jointModTransform_t.values()[val[0]];
                savefile.ReadJoint(copyJoints.oGet(i).from);
                savefile.ReadJoint(copyJoints.oGet(i).to);
            }

            leftEyeJoint = savefile.ReadJoint();
            rightEyeJoint = savefile.ReadJoint();
            soundJoint = savefile.ReadJoint();

            walkIK.Restore(savefile);

            savefile.ReadString(animPrefix);
            savefile.ReadString(painAnim);

            blink_anim = savefile.ReadInt();
            blink_time = savefile.ReadInt();
            blink_min = savefile.ReadInt();
            blink_max = savefile.ReadInt();

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/(scriptThread));

            savefile.ReadString(waitState);

            headAnim.Restore(savefile);
            torsoAnim.Restore(savefile);
            legsAnim.Restore(savefile);

            allowPain = savefile.ReadBool();
            allowEyeFocus = savefile.ReadBool();

            painTime = savefile.ReadInt();

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                idAttachInfo attach = attachments.Alloc();
                attach.ent.Restore(savefile);
                attach.channel = savefile.ReadInt();
            }

            finalBoss = savefile.ReadBool();

            idStr stateName = new idStr();

            savefile.ReadString(stateName);
            if (stateName.Length() > 0) {
                state = GetScriptFunction(stateName.toString());
            }

            savefile.ReadString(stateName);
            if (stateName.Length() > 0) {
                idealState = GetScriptFunction(stateName.toString());
            }
        }

        @Override
        public void Hide() {
            idEntity ent;
            idEntity next;

            idAFEntity_Base_Hide();//TODO:super size me
            if (head.GetEntity() != null) {
                head.GetEntity().Hide();
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
            if (head.GetEntity() != null) {
                head.GetEntity().Show();
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
            idStr fileName = new idStr();

            if (!spawnArgs.GetString("ragdoll", "*unknown*", fileName) || 0 == fileName.Length()) {
                return false;
            }
            af.SetAnimator(GetAnimator());
            return af.Load(this, fileName);
        }

        public void SetupBody() {
            String jointname;

            animator.ClearAllAnims(gameLocal.time, 0);
            animator.ClearAllJoints();

            idEntity headEnt = head.GetEntity();
            if (headEnt != null) {
                jointname = spawnArgs.GetString("bone_leftEye");
                leftEyeJoint = headEnt.GetAnimator().GetJointHandle(jointname);

                jointname = spawnArgs.GetString("bone_rightEye");
                rightEyeJoint = headEnt.GetAnimator().GetJointHandle(jointname);

                // set up the eye height.  check if it's specified in the def.
                if (!spawnArgs.GetFloat("eye_height", "0", new float[]{eyeOffset.z})) {
                    // if not in the def, then try to base it off the idle animation
                    int anim = headEnt.GetAnimator().GetAnim("idle");
                    if (anim != 0 && (leftEyeJoint != INVALID_JOINT)) {
                        idVec3 pos = new idVec3();
                        idMat3 axis = new idMat3();
                        headEnt.GetAnimator().PlayAnim(ANIMCHANNEL_ALL, anim, gameLocal.time, 0);
                        headEnt.GetAnimator().GetJointTransform(leftEyeJoint, gameLocal.time, pos, axis);
                        headEnt.GetAnimator().ClearAllAnims(gameLocal.time, 0);
                        headEnt.GetAnimator().ForceUpdate();
                        pos.oPluSet(headEnt.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin()));
                        eyeOffset = pos.oPlus(modelOffset);
                    } else {
                        // just base it off the bounding box size
                        eyeOffset.z = GetPhysics().GetBounds().oGet(1).z - 6;
                    }
                }
                headAnim.Init(this, headEnt.GetAnimator(), ANIMCHANNEL_ALL);
            } else {
                jointname = spawnArgs.GetString("bone_leftEye");
                leftEyeJoint = animator.GetJointHandle(jointname);

                jointname = spawnArgs.GetString("bone_rightEye");
                rightEyeJoint = animator.GetJointHandle(jointname);

                // set up the eye height.  check if it's specified in the def.
                if (!spawnArgs.GetFloat("eye_height", "0", new float[]{eyeOffset.z})) {
                    // if not in the def, then try to base it off the idle animation
                    int anim = animator.GetAnim("idle");
                    if (anim != 0 && (leftEyeJoint != INVALID_JOINT)) {
                        idVec3 pos = new idVec3();
                        idMat3 axis = new idMat3();
                        animator.PlayAnim(ANIMCHANNEL_ALL, anim, gameLocal.time, 0);
                        animator.GetJointTransform(leftEyeJoint, gameLocal.time, pos, axis);
                        animator.ClearAllAnims(gameLocal.time, 0);
                        animator.ForceUpdate();
                        eyeOffset = pos.oPlus(modelOffset);
                    } else {
                        // just base it off the bounding box size
                        eyeOffset.z = GetPhysics().GetBounds().oGet(1).z - 6;
                    }
                }
                headAnim.Init(this, animator, ANIMCHANNEL_HEAD);
            }

            waitState.oSet("");

            torsoAnim.Init(this, animator, ANIMCHANNEL_TORSO);
            legsAnim.Init(this, animator, ANIMCHANNEL_LEGS);
        }

        public void CheckBlink() {
            // check if it's time to blink
            if (0 == blink_anim || (health <= 0) || !allowEyeFocus || (blink_time > gameLocal.time)) {
                return;
            }

            idEntity headEnt = head.GetEntity();
            if (headEnt != null) {
                headEnt.GetAnimator().PlayAnim(ANIMCHANNEL_EYELIDS, blink_anim, gameLocal.time, 1);
            } else {
                animator.PlayAnim(ANIMCHANNEL_EYELIDS, blink_anim, gameLocal.time, 1);
            }

            // set the next blink time
            blink_time = (int) (gameLocal.time + blink_min + gameLocal.random.RandomFloat() * (blink_max - blink_min));
        }

        @Override
        public boolean GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            if (af.IsActive()) {
                af.GetPhysicsToVisualTransform(origin, axis);
                return true;
            }
            origin.oSet(modelOffset);
            axis.oSet(viewAxis);
            return true;
        }

        @Override
        public boolean GetPhysicsToSoundTransform(idVec3 origin, idMat3 axis) {
            if (soundJoint != INVALID_JOINT) {
                animator.GetJointTransform(soundJoint, gameLocal.time, origin, axis);
                origin.oPluSet(modelOffset);
                axis.oSet(viewAxis);
            } else {
                origin.oSet(GetPhysics().GetGravityNormal().oMultiply(-eyeOffset.z));
                axis.Identity();
            }
            return true;
        }

        /* **********************************************************************

         script state management

         ***********************************************************************/
        // script state management
        public void ShutdownThreads() {
            headAnim.Shutdown();
            torsoAnim.Shutdown();
            legsAnim.Shutdown();

            if (scriptThread != null) {
                scriptThread.EndThread();
                scriptThread.PostEventMS(EV_Remove, 0);
//		delete scriptThread;
                scriptThread = null;
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
            if (!scriptObject.HasObject()) {
                gameLocal.Error("No scriptobject set on '%s'.  Check the '%s' entityDef.", name, GetEntityDefName());
            }

            if (NOT(scriptThread)) {
                // create script thread
                scriptThread = new idThread();
                scriptThread.ManualDelete();
                scriptThread.ManualControl();
                scriptThread.SetThreadName(name.toString());
            } else {
                scriptThread.EndThread();
            }

            // call script object's constructor
            constructor = scriptObject.GetConstructor();
            if (NOT(constructor)) {
                gameLocal.Error("Missing constructor on '%s' for entity '%s'", scriptObject.GetTypeName(), name);
            }

            // init the script object's data
            scriptObject.ClearObject();

            // just set the current function on the script.  we'll execute in the subclasses.
            scriptThread.CallFunction(this, constructor, true);

            return scriptThread;
        }

        public void UpdateScript() {
            int i;

            if (ai_debugScript.GetInteger() == entityNumber) {
                scriptThread.EnableDebugInfo();
            } else {
                scriptThread.DisableDebugInfo();
            }

            // a series of state changes can happen in a single frame.
            // this loop limits them in case we've entered an infinite loop.
            for (i = 0; i < 20; i++) {
                if (idealState != state) {
                    SetState(idealState);
                }

                // don't call script until it's done waiting
                if (scriptThread.IsWaiting()) {
                    break;
                }

                scriptThread.Execute();
                if (idealState == state) {
                    break;
                }
            }

            if (i == 20) {
                scriptThread.Warning("idActor::UpdateScript: exited loop to prevent lockup");
            }
        }

        public function_t GetScriptFunction(final String funcname) {
            final function_t func;

            func = scriptObject.GetFunction(funcname);
            if (null == func) {
                scriptThread.Error("Unknown function '%s' in '%s'", funcname, scriptObject.GetTypeName());
            }

            return func;
        }

        public void SetState(final function_t newState) {
            if (NOT(newState)) {
                gameLocal.Error("idActor::SetState: Null state");
            }

            if (ai_debugScript.GetInteger() == entityNumber) {
                gameLocal.Printf("%d: %s: State: %s\n", gameLocal.time, name, newState.Name());
            }

            state = newState;
            idealState = state;
            scriptThread.CallFunction(this, state, true);
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
            eyeOffset.z = height;
        }

        public float EyeHeight() {
            return eyeOffset.z;
        }

        public idVec3 EyeOffset() {
            return GetPhysics().GetGravityNormal().oMultiply(-eyeOffset.z);
        }

        public idVec3 GetEyePosition() {
            return GetPhysics().GetOrigin().oPlus((GetPhysics().GetGravityNormal().oMultiply(-eyeOffset.z)));
        }

        public void GetViewPos(idVec3 origin, idMat3 axis) {
            origin.oSet(GetEyePosition());
            axis.oSet(viewAxis);
        }

        public void SetFOV(float fov) {
            fovDot = (float) Math.cos(DEG2RAD(fov * 0.5f));
        }

        public boolean CheckFOV(final idVec3 pos) {
            if (fovDot == 1.0f) {
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
            dot = viewAxis.oGet(0).oMultiply(delta);

            return (dot >= fovDot);
        }

        public boolean CanSee(idEntity ent, boolean useFOV) {
            trace_s[] tr = {null};
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
            if (tr[0].fraction >= 1.0f || (gameLocal.GetTraceEntity(tr[0]) == ent)) {
                return true;
            }

            return false;
        }

        public boolean PointVisible(final idVec3 point) {
            trace_s[] results = {null};
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
            idStr groupname;
            idList<Integer/*jointHandle_t*/> jointList = new idList<>();
            int jointnum;
            float scale;

            // create damage zones
            damageGroups.SetNum(animator.NumJoints());
            arg = spawnArgs.MatchPrefix("damage_zone ", null);
            while (arg != null) {
                groupname = arg.GetKey();
                groupname.Strip("damage_zone ");
                animator.GetJointList(arg.GetValue(), jointList);
                for (i = 0; i < jointList.Num(); i++) {
                    jointnum = jointList.oGet(i);
                    damageGroups.oSet(jointnum, groupname);
                }
                jointList.Clear();
                arg = spawnArgs.MatchPrefix("damage_zone ", arg);
            }

            // initilize the damage zones to normal damage
            damageScale.SetNum(animator.NumJoints());
            for (i = 0; i < damageScale.Num(); i++) {
                damageScale.oSet(i, 1.0f);
            }

            // set the percentage on damage zones
            arg = spawnArgs.MatchPrefix("damage_scale ", null);
            while (arg != null) {
                scale = atof(arg.GetValue());
                groupname = arg.GetKey();
                groupname.Strip("damage_scale ");
                for (i = 0; i < damageScale.Num(); i++) {
                    if (groupname.equals(damageGroups.oGet(i))) {
                        damageScale.oSet(i, scale);
                    }
                }
                arg = spawnArgs.MatchPrefix("damage_scale ", arg);
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
            if (!fl.takedamage) {
                return;
            }

            if (null == inflictor) {
                inflictor = gameLocal.world;//TODO:oSet
            }
            if (null == attacker) {
                attacker = gameLocal.world;
            }

            if (finalBoss && !inflictor.IsType(idSoulCubeMissile.class)) {
                return;
            }

            final idDict damageDef = gameLocal.FindEntityDefDict(damageDefName);
            if (null == damageDef) {
                gameLocal.Error("Unknown damageDef '%s'", damageDefName);
            }

            int[] damage = {(int) (damageDef.GetInt("damage") * damageScale)};
            damage[0] = GetDamageForLocation(damage[0], location);

            // inform the attacker that they hit someone
            attacker.DamageFeedback(this, inflictor, damage);
            if (damage[0] > 0) {
                health -= damage[0];
                if (health <= 0) {
                    if (health < -999) {
                        health = -999;
                    }
                    Killed(inflictor, attacker, damage[0], dir, location);
                    if ((health < -20) && spawnArgs.GetBool("gib") && damageDef.GetBool("gib")) {
                        Gib(dir, damageDefName);
                    }
                } else {
                    Pain(inflictor, attacker, damage[0], dir, location);
                }
            } else {
                // don't accumulate knockback
                if (af.IsLoaded()) {
                    // clear impacts
                    af.Rest();

                    // physics is turned off by calling af.Rest()
                    BecomeActive(TH_PHYSICS);
                }
            }
        }

        public int GetDamageForLocation(int damage, int location) {
            if ((location < 0) || (location >= damageScale.Num())) {
                return damage;
            }

            return (int) Math.ceil(damage * damageScale.oGet(location));
        }

        public String GetDamageGroup(int location) {
            if ((location < 0) || (location >= damageGroups.Num())) {
                return "";
            }

            return damageGroups.oGet(location).toString();
        }

        public void ClearPain() {
            pain_debounce_time = 0;
        }

        @Override
        public boolean Pain(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            if (af.IsLoaded()) {
                // clear impacts
                af.Rest();

                // physics is turned off by calling af.Rest()
                BecomeActive(TH_PHYSICS);
            }

            if (gameLocal.time < pain_debounce_time) {
                return false;
            }

            // don't play pain sounds more than necessary
            pain_debounce_time = gameLocal.time + pain_delay;

            if (health > 75) {
                StartSound("snd_pain_small", SND_CHANNEL_VOICE, 0, false, null);
            } else if (health > 50) {
                StartSound("snd_pain_medium", SND_CHANNEL_VOICE, 0, false, null);
            } else if (health > 25) {
                StartSound("snd_pain_large", SND_CHANNEL_VOICE, 0, false, null);
            } else {
                StartSound("snd_pain_huge", SND_CHANNEL_VOICE, 0, false, null);
            }

            if (!allowPain || (gameLocal.time < painTime)) {
                // don't play a pain anim
                return false;
            }

            if (pain_threshold != 0 && (damage < pain_threshold)) {
                return false;
            }

            // set the pain anim
            String damageGroup = GetDamageGroup(location);

            painAnim.oSet("");
            if (animPrefix.Length() != 0) {
                if (isNotNullOrEmpty(damageGroup) && !damageGroup.equals("legs")) {
                    painAnim.oSet(String.format("%s_pain_%s", animPrefix.toString(), damageGroup));
                    if (!animator.HasAnim(painAnim)) {
                        painAnim.oSet(String.format("pain_%s", damageGroup));
                        if (!animator.HasAnim(painAnim)) {
                            painAnim.oSet("");
                        }
                    }
                }

                if (0 == painAnim.Length()) {
                    painAnim.oSet(String.format("%s_pain", animPrefix.toString()));
                    if (!animator.HasAnim(painAnim)) {
                        painAnim.oSet("");
                    }
                }
            } else if (isNotNullOrEmpty(damageGroup) && (!damageGroup.equals("legs"))) {
                painAnim.oSet(String.format("pain_%s", damageGroup));
                if (!animator.HasAnim(painAnim)) {
                    painAnim.oSet(String.format("pain_%s", damageGroup));
                    if (!animator.HasAnim(painAnim)) {
                        painAnim.oSet("");
                    }
                }
            }

            if (0 == painAnim.Length()) {
                painAnim.oSet("pain");
            }

            if (g_debugDamage.GetBool()) {
                gameLocal.Printf("Damage: joint: '%s', zone '%s', anim '%s'\n", animator.GetJointName((int/*jointHandle_t*/) location),
                        damageGroup, painAnim);
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

            if (!use_combat_bbox) {
                if (combatModel != null) {
                    combatModel.Unlink();
                    combatModel.LoadModel(modelDefHandle);
                } else {
                    combatModel = new idClipModel(modelDefHandle);
                }

                headEnt = head.GetEntity();
                if (headEnt != null) {
                    headEnt.SetCombatModel();
                }
            }
        }

        @Override
        public idClipModel GetCombatModel() {
            return combatModel;
        }

        @Override
        public void LinkCombat() {
            idAFAttachment headEnt;

            if (fl.hidden || use_combat_bbox) {
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

        public boolean StartRagdoll() {
            float slomoStart, slomoEnd;
            float jointFrictionDent, jointFrictionDentStart, jointFrictionDentEnd;
            float contactFrictionDent, contactFrictionDentStart, contactFrictionDentEnd;

            // if no AF loaded
            if (!af.IsLoaded()) {
                return false;
            }

            // if the AF is already active
            if (af.IsActive()) {
                return true;
            }

            // disable the monster bounding box
            GetPhysics().DisableClip();

            // start using the AF
            af.StartFromCurrentPose(spawnArgs.GetInt("velocityTime", "0"));

            slomoStart = MS2SEC(gameLocal.time) + spawnArgs.GetFloat("ragdoll_slomoStart", "-1.6");
            slomoEnd = MS2SEC(gameLocal.time) + spawnArgs.GetFloat("ragdoll_slomoEnd", "0.8");

            // do the first part of the death in slow motion
            af.GetPhysics().SetTimeScaleRamp(slomoStart, slomoEnd);

            jointFrictionDent = spawnArgs.GetFloat("ragdoll_jointFrictionDent", "0.1");
            jointFrictionDentStart = MS2SEC(gameLocal.time) + spawnArgs.GetFloat("ragdoll_jointFrictionStart", "0.2");
            jointFrictionDentEnd = MS2SEC(gameLocal.time) + spawnArgs.GetFloat("ragdoll_jointFrictionEnd", "1.2");

            // set joint friction dent
            af.GetPhysics().SetJointFrictionDent(jointFrictionDent, jointFrictionDentStart, jointFrictionDentEnd);

            contactFrictionDent = spawnArgs.GetFloat("ragdoll_contactFrictionDent", "0.1");
            contactFrictionDentStart = MS2SEC(gameLocal.time) + spawnArgs.GetFloat("ragdoll_contactFrictionStart", "1.0");
            contactFrictionDentEnd = MS2SEC(gameLocal.time) + spawnArgs.GetFloat("ragdoll_contactFrictionEnd", "2.0");

            // set contact friction dent
            af.GetPhysics().SetContactFrictionDent(contactFrictionDent, contactFrictionDentStart, contactFrictionDentEnd);

            // drop any items the actor is holding
            idMoveableItem.DropItems(this, "death", null);

            // drop any articulated figures the actor is holding
            idAFEntity_Base.DropAFs(this, "death", null);

            RemoveAttachments();

            return true;
        }

        public void StopRagdoll() {
            if (af.IsActive()) {
                af.Stop();
            }
        }

        @Override
        public boolean UpdateAnimationControllers() {

            if (af.IsActive()) {
                return idAFEntity_Base_UpdateAnimationControllers();
            } else {
                animator.ClearAFPose();
            }

            if (walkIK.IsInitialized()) {
                walkIK.Evaluate();
                return true;
            }

            return false;
        }

        // delta view angles to allow movers to rotate the view of the actor
        public idAngles GetDeltaViewAngles() {
            return deltaViewAngles;
        }

        public void SetDeltaViewAngles(final idAngles delta) {
            deltaViewAngles = delta;
        }

        public boolean HasEnemies() {
            idActor ent;

            for (ent = enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
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
            for (ent = enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
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
            for (ent = enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
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
            idBounds bounds = new idBounds();

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
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();
            int/*jointHandle_t*/ joint;
            String jointName;
            idAttachInfo attach = attachments.Alloc();
            idAngles angleOffset;
            idVec3 originOffset;

            jointName = ent.spawnArgs.GetString("joint");
            joint = animator.GetJointHandle(jointName);
            if (joint == INVALID_JOINT) {
                gameLocal.Error("Joint '%s' not found for attaching '%s' on '%s'", jointName, ent.GetClassname(), name);
            }

            angleOffset = ent.spawnArgs.GetAngles("angles");
            originOffset = ent.spawnArgs.GetVector("origin");

            attach.channel = animator.GetChannelForJoint(joint);
            GetJointWorldTransform(joint, gameLocal.time, origin, axis);
            attach.ent.oSet(ent);

            ent.SetOrigin(origin.oPlus(originOffset.oMultiply(renderEntity.axis)));
            idMat3 rotate = angleOffset.ToMat3();
            idMat3 newAxis = rotate.oMultiply(axis);
            ent.SetAxis(newAxis);
            ent.BindToJoint(this, joint, true);
            ent.cinematic = cinematic;
        }

        @Override
        public void Teleport(final idVec3 origin, final idAngles angles, idEntity destination) {
            GetPhysics().SetOrigin(origin.oPlus(new idVec3(0, 0, CM_CLIP_EPSILON)));
            GetPhysics().SetLinearVelocity(getVec3_origin());

            viewAxis = angles.ToMat3();

            UpdateVisuals();

            if (!IsHidden()) {
                // kill anything at the new position
                gameLocal.KillBox(this);
            }
        }

        @Override
        public renderView_s GetRenderView() {
            renderView_s rv = super.GetRenderView();//TODO:super.super....
            rv.viewaxis = new idMat3(viewAxis);
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
                if (NOT(head.GetEntity())) {
                    return 0;
                }
                animatorPtr = head.GetEntity().GetAnimator();
            } else {
                animatorPtr = animator;
            }

            if (animPrefix.Length() != 0) {
                temp = va("%s_%s", animPrefix, animName);
                anim = animatorPtr.GetAnim(temp);
                if (anim != 0) {
                    return anim;
                }
            }

            anim = animatorPtr.GetAnim(animName);

            return anim;
        }

        public void UpdateAnimState() {
            headAnim.UpdateState();
            torsoAnim.UpdateState();
            legsAnim.UpdateState();
        }

        public void SetAnimState(int channel, final String statename, int blendFrames) {
            function_t func;

            func = scriptObject.GetFunction(statename);
            if (null == func) {
                assert (false);
                gameLocal.Error("Can't find function '%s' in object '%s'", statename, scriptObject.GetTypeName());
            }

            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    headAnim.SetState(statename, blendFrames);
                    allowEyeFocus = true;
                    break;

                case ANIMCHANNEL_TORSO:
                    torsoAnim.SetState(statename, blendFrames);
                    legsAnim.Enable(blendFrames);
                    allowPain = true;
                    allowEyeFocus = true;
                    break;

                case ANIMCHANNEL_LEGS:
                    legsAnim.SetState(statename, blendFrames);
                    torsoAnim.Enable(blendFrames);
                    allowPain = true;
                    allowEyeFocus = true;
                    break;

                default:
                    gameLocal.Error("idActor::SetAnimState: Unknown anim group");
                    break;
            }
        }

        public idStr GetAnimState(int channel) {
            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    return headAnim.state;
                case ANIMCHANNEL_TORSO:
                    return torsoAnim.state;
                case ANIMCHANNEL_LEGS:
                    return legsAnim.state;
                default:
                    gameLocal.Error("idActor::GetAnimState: Unknown anim group");
                    return null;
            }
        }

        public boolean InAnimState(int channel, final String stateName) {
            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    if (headAnim.state.equals(stateName)) {
                        return true;
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    if (torsoAnim.state.equals(stateName)) {
                        return true;
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    if (legsAnim.state.equals(stateName)) {
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
            if (waitState.Length() != 0) {
                return waitState.toString();
            } else {
                return null;
            }
        }

        public void SetWaitState(final String _waitstate) {
            waitState.oSet(_waitstate);
        }

        public boolean AnimDone(int channel, int blendFrames) {
            int animDoneTime;

            animDoneTime = animator.CurrentAnim(channel).GetEndTime();
            if (animDoneTime < 0) {
                // playing a cycle
                return false;
            } else if (animDoneTime - FRAME2MS(blendFrames) <= gameLocal.time) {
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
            if (gibbed) {
                return;
            }
            super.Gib(dir, damageDefName);
            if (head.GetEntity() != null) {
                head.GetEntity().Hide();
            }
            StopSound(etoi(SND_CHANNEL_VOICE), false);
        }

        // removes attachments with "remove" set for when character dies
        protected void RemoveAttachments() {
            int i;
            idEntity ent;

            // remove any attached entities
            for (i = 0; i < attachments.Num(); i++) {
                ent = attachments.oGet(i).ent.GetEntity();
                if (ent != null && ent.spawnArgs.GetBool("remove")) {
                    ent.PostEventMS(EV_Remove, 0);
                }
            }
        }

        // copies animation from body to head joints
        protected void CopyJointsFromBodyToHead() {
            idEntity headEnt = head.GetEntity();
            idAnimator headAnimator;
            int i;
            idMat3 mat;
            idMat3 axis = new idMat3();
            idVec3 pos = new idVec3();

            if (null == headEnt) {
                return;
            }

            headAnimator = headEnt.GetAnimator();

            // copy the animation from the body to the head
            for (i = 0; i < copyJoints.Num(); i++) {
                if (copyJoints.oGet(i).mod == JOINTMOD_WORLD_OVERRIDE) {
                    mat = headEnt.GetPhysics().GetAxis().Transpose();
                    GetJointWorldTransform(copyJoints.oGet(i).from[0], gameLocal.time, pos, axis);
                    pos.oMinSet(headEnt.GetPhysics().GetOrigin());
                    headAnimator.SetJointPos(copyJoints.oGet(i).to[0], copyJoints.oGet(i).mod, pos.oMultiply(mat));
                    headAnimator.SetJointAxis(copyJoints.oGet(i).to[0], copyJoints.oGet(i).mod, axis.oMultiply(mat));
                } else {
                    animator.GetJointLocalTransform(copyJoints.oGet(i).from[0], gameLocal.time, pos, axis);
                    headAnimator.SetJointPos(copyJoints.oGet(i).to[0], copyJoints.oGet(i).mod, pos);
                    headAnimator.SetJointAxis(copyJoints.oGet(i).to[0], copyJoints.oGet(i).mod, axis);
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
                headEnt = head.GetEntity();
                if (headEnt != null) {
                    headAnimator = headEnt.GetAnimator();
                    syncAnim = animator.CurrentAnim(syncToChannel);
                    if (syncAnim != null) {
                        anim = headAnimator.GetAnim(syncAnim.AnimFullName());
                        if (0 == anim) {
                            anim = headAnimator.GetAnim(syncAnim.AnimName());
                        }
                        if (anim != 0) {
                            cycle = animator.CurrentAnim(syncToChannel).GetCycleCount();
                            starttime = animator.CurrentAnim(syncToChannel).GetStartTime();
                            headAnimator.PlayAnim(ANIMCHANNEL_ALL, anim, gameLocal.time, blendTime);
                            headAnimator.CurrentAnim(ANIMCHANNEL_ALL).SetCycleCount(cycle);
                            headAnimator.CurrentAnim(ANIMCHANNEL_ALL).SetStartTime(starttime);
                        } else {
                            headEnt.PlayIdleAnim(blendTime);
                        }
                    }
                }
            } else if (syncToChannel == ANIMCHANNEL_HEAD) {
                headEnt = head.GetEntity();
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
                            animator.PlayAnim(channel, anim, gameLocal.time, blendTime);
                            animator.CurrentAnim(channel).SetCycleCount(cycle);
                            animator.CurrentAnim(channel).SetStartTime(starttime);
                        }
                    }
                }
            } else {
                animator.SyncAnimChannels(channel, syncToChannel, gameLocal.time, blendTime);
            }
        }

        private void FinishSetup() {
            String[] scriptObjectName = {null};

            // setup script object
            if (spawnArgs.GetString("scriptobject", null, scriptObjectName)) {
                if (!scriptObject.SetType(scriptObjectName[0])) {
                    gameLocal.Error("Script object '%s' not found on entity '%s'.", scriptObjectName, name);
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

            headModel = spawnArgs.GetString("def_head", "");
            if (!headModel.isEmpty()) {
                jointName = spawnArgs.GetString("head_joint");
                joint = animator.GetJointHandle(jointName);
                if (joint == INVALID_JOINT) {
                    gameLocal.Error("Joint '%s' not found for 'head_joint' on '%s'", jointName, name);
                }

                // set the damage joint to be part of the head damage group
                damageJoint = joint;
                for (i = 0; i < damageGroups.Num(); i++) {
                    final idStr d = damageGroups.oGet(i);
                    if (d != null && d.equals("head")) {
                        damageJoint = /*(jointHandle_t)*/ i;
                        break;
                    }
                }

                // copy any sounds in case we have frame commands on the head
                idDict args = new idDict();
                sndKV = spawnArgs.MatchPrefix("snd_", null);
                while (sndKV != null) {
                    args.Set(sndKV.GetKey(), sndKV.GetValue());
                    sndKV = spawnArgs.MatchPrefix("snd_", sndKV);
                }

                headEnt = (idAFAttachment) gameLocal.SpawnEntityType(idAFAttachment.class, null);
                headEnt.SetName(va("%s_head", name));
                headEnt.SetBody(this, headModel, damageJoint);
                head.oSet(headEnt);

                idVec3 origin = new idVec3();
                idMat3 axis = new idMat3();
                idAttachInfo attach = attachments.Alloc();
                attach.channel = animator.GetChannelForJoint(joint);
                animator.GetJointTransform(joint, gameLocal.time, origin, axis);
                origin = renderEntity.origin.oPlus((origin.oPlus(modelOffset)).oMultiply(renderEntity.axis));
                attach.ent = new idEntityPtr<>();
                attach.ent.oSet(headEnt);
                headEnt.SetOrigin(origin);
                headEnt.SetAxis(renderEntity.axis);
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
                sound = spawnArgs.GetString(va("snd_footstep_%s", gameLocal.sufaceTypeNames[etoi(material.GetSurfaceType())]));
            }
            if (sound.isEmpty()) {// == '\0' ) {
                sound = spawnArgs.GetString("snd_footstep");
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
                sound = spawnArgs.GetString(va("snd_footstep_%s", gameLocal.sufaceTypeNames[etoi(material.GetSurfaceType())]));
            }
            if (sound.isEmpty()) {// == '\0' ) {
                sound = spawnArgs.GetString("snd_footstep");
            }
            if (!sound.isEmpty()) {// != '\0' ) {
                StartSoundShader(declManager.FindSound(sound), etoi(SND_CHANNEL_BODY), 0, false, null);
            }
        }

        private void Event_DisableEyeFocus() {
            allowEyeFocus = false;

            idEntity headEnt = head.GetEntity();
            if (headEnt != null) {
                headEnt.GetAnimator().Clear(ANIMCHANNEL_EYELIDS, gameLocal.time, FRAME2MS(2));
            } else {
                animator.Clear(ANIMCHANNEL_EYELIDS, gameLocal.time, FRAME2MS(2));
            }
        }

        private void Event_Footstep() {
            PlayFootStepSound();
        }

        private void Event_EnableWalkIK() {
            walkIK.EnableAll();
        }

        private void Event_DisableWalkIK() {
            walkIK.DisableAll();
        }

        private void Event_EnableLegIK(idEventArg<Integer> num) {
            walkIK.EnableLeg(num.value);
        }

        private void Event_DisableLegIK(idEventArg<Integer> num) {
            walkIK.DisableLeg(num.value);
        }

        private void Event_SetAnimPrefix(final idEventArg<String> prefix) {
            animPrefix.oSet(prefix.value);
        }

//        private void Event_LookAtEntity(idEntity ent, float duration);
        private void Event_PreventPain(idEventArg<Float> duration) {
            painTime = (int) (gameLocal.time + SEC2MS(duration.value));
        }

        private void Event_DisablePain() {
            allowPain = false;
        }

        private void Event_EnablePain() {
            allowPain = true;
        }

        private void Event_GetPainAnim() {
            if (0 == painAnim.Length()) {
                idThread.ReturnString("pain");
            } else {
                idThread.ReturnString(painAnim);
            }
        }

        private void Event_StopAnim(idEventArg<Integer> channel, idEventArg<Integer> frames) {
            switch (channel.value) {
                case ANIMCHANNEL_HEAD:
                    headAnim.StopAnim(frames.value);
                    break;

                case ANIMCHANNEL_TORSO:
                    torsoAnim.StopAnim(frames.value);
                    break;

                case ANIMCHANNEL_LEGS:
                    legsAnim.StopAnim(frames.value);
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
                if ((channel == ANIMCHANNEL_HEAD) && head.GetEntity() != null) {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, name.toString(), spawnArgs.GetString("def_head", ""));
                } else {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, name.toString(), GetEntityDefName());
                }
                idThread.ReturnInt(0);
                return;
            }

            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    headEnt = head.GetEntity();
                    if (headEnt != null) {
                        headAnim.idleAnim = false;
                        headAnim.PlayAnim(anim);
                        flags = headAnim.GetAnimFlags();
                        if (!flags.prevent_idle_override) {
                            if (torsoAnim.IsIdle()) {
                                torsoAnim.animBlendFrames = headAnim.lastAnimBlendFrames;
                                SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_HEAD, headAnim.lastAnimBlendFrames);
                                if (legsAnim.IsIdle()) {
                                    legsAnim.animBlendFrames = headAnim.lastAnimBlendFrames;
                                    SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_HEAD, headAnim.lastAnimBlendFrames);
                                }
                            }
                        }
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    torsoAnim.idleAnim = false;
                    torsoAnim.PlayAnim(anim);
                    flags = torsoAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (headAnim.IsIdle()) {
                            headAnim.animBlendFrames = torsoAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
                        }
                        if (legsAnim.IsIdle()) {
                            legsAnim.animBlendFrames = torsoAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
                        }
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    legsAnim.idleAnim = false;
                    legsAnim.PlayAnim(anim);
                    flags = legsAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (torsoAnim.IsIdle()) {
                            torsoAnim.animBlendFrames = legsAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, legsAnim.lastAnimBlendFrames);
                            if (headAnim.IsIdle()) {
                                headAnim.animBlendFrames = legsAnim.lastAnimBlendFrames;
                                SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, legsAnim.lastAnimBlendFrames);
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
                if ((channel == ANIMCHANNEL_HEAD) && head.GetEntity() != null) {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, name, spawnArgs.GetString("def_head", ""));
                } else {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, name, GetEntityDefName());
                }
                idThread.ReturnInt(false);
                return;
            }

            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    headAnim.idleAnim = false;
                    headAnim.CycleAnim(anim);
                    flags = headAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (torsoAnim.IsIdle() && legsAnim.IsIdle()) {
                            torsoAnim.animBlendFrames = headAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_HEAD, headAnim.lastAnimBlendFrames);
                            legsAnim.animBlendFrames = headAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_HEAD, headAnim.lastAnimBlendFrames);
                        }
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    torsoAnim.idleAnim = false;
                    torsoAnim.CycleAnim(anim);
                    flags = torsoAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (headAnim.IsIdle()) {
                            headAnim.animBlendFrames = torsoAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
                        }
                        if (legsAnim.IsIdle()) {
                            legsAnim.animBlendFrames = torsoAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
                        }
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    legsAnim.idleAnim = false;
                    legsAnim.CycleAnim(anim);
                    flags = legsAnim.GetAnimFlags();
                    if (!flags.prevent_idle_override) {
                        if (torsoAnim.IsIdle()) {
                            torsoAnim.animBlendFrames = legsAnim.lastAnimBlendFrames;
                            SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, legsAnim.lastAnimBlendFrames);
                            if (headAnim.IsIdle()) {
                                headAnim.animBlendFrames = legsAnim.lastAnimBlendFrames;
                                SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, legsAnim.lastAnimBlendFrames);
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
                if ((channel == ANIMCHANNEL_HEAD) && head.GetEntity() != null) {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, name, spawnArgs.GetString("def_head", ""));
                } else {
                    gameLocal.DPrintf("missing '%s' animation on '%s' (%s)\n", animName, name, GetEntityDefName());
                }

                switch (channel) {
                    case ANIMCHANNEL_HEAD:
                        headAnim.BecomeIdle();
                        break;

                    case ANIMCHANNEL_TORSO:
                        torsoAnim.BecomeIdle();
                        break;

                    case ANIMCHANNEL_LEGS:
                        legsAnim.BecomeIdle();
                        break;

                    default:
                        gameLocal.Error("Unknown anim group");
                }

                idThread.ReturnInt(false);
                return;
            }

            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    headAnim.BecomeIdle();
                    if (torsoAnim.GetAnimFlags().prevent_idle_override) {
                        // don't sync to torso body if it doesn't override idle anims
                        headAnim.CycleAnim(anim);
                    } else if (torsoAnim.IsIdle() && legsAnim.IsIdle()) {
                        // everything is idle, so play the anim on the head and copy it to the torso and legs
                        headAnim.CycleAnim(anim);
                        torsoAnim.animBlendFrames = headAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_HEAD, headAnim.lastAnimBlendFrames);
                        legsAnim.animBlendFrames = headAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_HEAD, headAnim.lastAnimBlendFrames);
                    } else if (torsoAnim.IsIdle()) {
                        // sync the head and torso to the legs
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, headAnim.animBlendFrames);
                        torsoAnim.animBlendFrames = headAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, torsoAnim.animBlendFrames);
                    } else {
                        // sync the head to the torso
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, headAnim.animBlendFrames);
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    torsoAnim.BecomeIdle();
                    if (legsAnim.GetAnimFlags().prevent_idle_override) {
                        // don't sync to legs if legs anim doesn't override idle anims
                        torsoAnim.CycleAnim(anim);
                    } else if (legsAnim.IsIdle()) {
                        // play the anim in both legs and torso
                        torsoAnim.CycleAnim(anim);
                        legsAnim.animBlendFrames = torsoAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
                    } else {
                        // sync the anim to the legs
                        SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, torsoAnim.animBlendFrames);
                    }

                    if (headAnim.IsIdle()) {
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    legsAnim.BecomeIdle();
                    if (torsoAnim.GetAnimFlags().prevent_idle_override) {
                        // don't sync to torso if torso anim doesn't override idle anims
                        legsAnim.CycleAnim(anim);
                    } else if (torsoAnim.IsIdle()) {
                        // play the anim in both legs and torso
                        legsAnim.CycleAnim(anim);
                        torsoAnim.animBlendFrames = legsAnim.lastAnimBlendFrames;
                        SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, legsAnim.lastAnimBlendFrames);
                        if (headAnim.IsIdle()) {
                            SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, legsAnim.lastAnimBlendFrames);
                        }
                    } else {
                        // sync the anim to the torso
                        SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, legsAnim.animBlendFrames);
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

            headEnt = head.GetEntity();
            switch (channel) {
                case ANIMCHANNEL_HEAD:
                    if (headEnt != null) {
                        animator.CurrentAnim(ANIMCHANNEL_ALL).SetSyncedAnimWeight(anim, weight);
                    } else {
                        animator.CurrentAnim(ANIMCHANNEL_HEAD).SetSyncedAnimWeight(anim, weight);
                    }
                    if (torsoAnim.IsIdle()) {
                        animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(anim, weight);
                        if (legsAnim.IsIdle()) {
                            animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(anim, weight);
                        }
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(anim, weight);
                    if (legsAnim.IsIdle()) {
                        animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(anim, weight);
                    }
                    if (headEnt != null && headAnim.IsIdle()) {
                        animator.CurrentAnim(ANIMCHANNEL_ALL).SetSyncedAnimWeight(anim, weight);
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(anim, weight);
                    if (torsoAnim.IsIdle()) {
                        animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(anim, weight);
                        if (headEnt != null && headAnim.IsIdle()) {
                            animator.CurrentAnim(ANIMCHANNEL_ALL).SetSyncedAnimWeight(anim, weight);
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
                    headAnim.Disable();
                    if (!torsoAnim.IsIdle()) {
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
                    } else {
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_LEGS, legsAnim.lastAnimBlendFrames);
                    }
                    break;

                case ANIMCHANNEL_TORSO:
                    torsoAnim.Disable();
                    SyncAnimChannels(ANIMCHANNEL_TORSO, ANIMCHANNEL_LEGS, legsAnim.lastAnimBlendFrames);
                    if (headAnim.IsIdle()) {
                        SyncAnimChannels(ANIMCHANNEL_HEAD, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
                    }
                    break;

                case ANIMCHANNEL_LEGS:
                    legsAnim.Disable();
                    SyncAnimChannels(ANIMCHANNEL_LEGS, ANIMCHANNEL_TORSO, torsoAnim.lastAnimBlendFrames);
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
                    headAnim.Enable(blendFrames);
                    break;

                case ANIMCHANNEL_TORSO:
                    torsoAnim.Enable(blendFrames);
                    break;

                case ANIMCHANNEL_LEGS:
                    legsAnim.Enable(blendFrames);
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
                    headAnim.animBlendFrames = blendFrames;
                    headAnim.lastAnimBlendFrames = blendFrames;
                    break;

                case ANIMCHANNEL_TORSO:
                    torsoAnim.animBlendFrames = blendFrames;
                    torsoAnim.lastAnimBlendFrames = blendFrames;
                    break;

                case ANIMCHANNEL_LEGS:
                    legsAnim.animBlendFrames = blendFrames;
                    legsAnim.lastAnimBlendFrames = blendFrames;
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
                    idThread.ReturnInt(headAnim.animBlendFrames);
                    break;

                case ANIMCHANNEL_TORSO:
                    idThread.ReturnInt(torsoAnim.animBlendFrames);
                    break;

                case ANIMCHANNEL_LEGS:
                    idThread.ReturnInt(legsAnim.animBlendFrames);
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
            if (waitState.equals(actionname.value)) {
                SetWaitState("");
            }
        }

        private void Event_AnimDone(idEventArg<Integer> channel, idEventArg<Integer> _blendFrames) {
            int blendFrames = _blendFrames.value;
            boolean result;

            switch (channel.value) {
                case ANIMCHANNEL_HEAD:
                    result = headAnim.AnimDone(blendFrames);
                    idThread.ReturnInt(result);
                    break;

                case ANIMCHANNEL_TORSO:
                    result = torsoAnim.AnimDone(blendFrames);
                    idThread.ReturnInt(result);
                    break;

                case ANIMCHANNEL_LEGS:
                    result = legsAnim.AnimDone(blendFrames);
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
                if (animPrefix.Length() != 0) {
                    gameLocal.Error("Can't find anim '%s_%s' for '%s'", animPrefix, animname, name);
                } else {
                    gameLocal.Error("Can't find anim '%s' for '%s'", animname, name);
                }
            }
        }

        private void Event_ChooseAnim(idEventArg<Integer> channel, final idEventArg<String> animname) {
            int anim;

            anim = GetAnim(channel.value, animname.value);
            if (anim != 0) {
                if (channel.value == ANIMCHANNEL_HEAD) {
                    if (head.GetEntity() != null) {
                        idThread.ReturnString(head.GetEntity().GetAnimator().AnimFullName(anim));
                        return;
                    }
                } else {
                    idThread.ReturnString(animator.AnimFullName(anim));
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
                    if (head.GetEntity() != null) {
                        idThread.ReturnFloat(MS2SEC(head.GetEntity().GetAnimator().AnimLength(anim)));
                        return;
                    }
                } else {
                    idThread.ReturnFloat(MS2SEC(animator.AnimLength(anim)));
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
                    if (head.GetEntity() != null) {
                        idThread.ReturnFloat(head.GetEntity().GetAnimator().TotalMovementDelta(anim).Length());
                        return;
                    }
                } else {
                    idThread.ReturnFloat(animator.TotalMovementDelta(anim).Length());
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
            idEntity ent = _ent.value;
            idActor actor;

            if (null == ent || (ent.equals(this))) {
                actor = enemyList.Next();
            } else {
                if (!ent.IsType(idActor.class)) {
                    gameLocal.Error("'%s' cannot be an enemy", ent.name);
                }

                actor = (idActor) ent;
                if (actor.enemyNode.ListHead() != enemyList) {
                    gameLocal.Error("'%s' is not in '%s' enemy list", actor.name, name);
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
            idActor bestEnt = ClosestEnemyToPoint(pos.value);
            idThread.ReturnEntity(bestEnt);
        }

        private void Event_StopSound(idEventArg<Integer> channel, idEventArg<Integer> netSync) {
            if (channel.value == etoi(SND_CHANNEL_VOICE)) {
                idEntity headEnt = head.GetEntity();
                if (headEnt != null) {
                    headEnt.StopSound(channel.value, (netSync.value != 0));
                }
            }
            StopSound(channel.value, (netSync.value != 0));
        }

        private void Event_SetNextState(final idEventArg<String> name) {
            idealState = GetScriptFunction(name.value);
            if (idealState == state) {
                state = null;
            }
        }

        private void Event_SetState(final idEventArg<String> name) {
            idealState = GetScriptFunction(name.value);
            if (idealState == state) {
                state = null;
            }
            scriptThread.DoneProcessing();
        }

        private void Event_GetState() {
            if (state != null) {
                idThread.ReturnString(state.Name());
            } else {
                idThread.ReturnString("");
            }
        }

        private void Event_GetHead() {
            idThread.ReturnEntity(head.GetEntity());
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    };
}
