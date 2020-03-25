package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_StartSoundShader;
import static neo.Game.Entity.TH_ALL;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.developer;
import static neo.Game.GameSys.SysCvar.g_fov;
import static neo.Game.GameSys.SysCvar.pm_stamina;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_DEMONIC;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_VOICE;
import static neo.Game.Player.EV_Player_DisableWeapon;
import static neo.Game.Player.EV_Player_EnableWeapon;
import static neo.Game.Player.EV_Player_SelectWeapon;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.BuildDefines.ID_DEMO_BUILD;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import static neo.framework.UsercmdGen.BUTTON_ATTACK;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec4_zero;
import static neo.ui.UserInterface.uiManager;

import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel_local;
import neo.Game.Entity.idEntity;
import neo.Game.Item.idItem;
import neo.Game.Light.idLight;
import neo.Game.Misc.idStaticEntity;
import neo.Game.Mover.idDoor;
import neo.Game.Player.idPlayer;
import neo.Game.Sound.idSound;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Thread.idThread;
import neo.Sound.snd_shader.idSoundShader;
import neo.framework.DeclPDA.idDeclPDA;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Interpolate.idInterpolate;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Target {

    public static final idEventDef EV_RestoreInfluence = new idEventDef("<RestoreInfluece>");
    public static final idEventDef EV_GatherEntities   = new idEventDef("<GatherEntities>");
    public static final idEventDef EV_Flash            = new idEventDef("<Flash>", "fd");
    public static final idEventDef EV_ClearFlash       = new idEventDef("<ClearFlash>", "f");
    //
    public static final idEventDef EV_TipOff           = new idEventDef("<TipOff>");
    public static final idEventDef EV_GetPlayerPos     = new idEventDef("<getplayerpos>");
    //
    public static final idEventDef EV_RestoreVolume    = new idEventDef("<RestoreVolume>");

    /*
     ===============================================================================

     idTarget

     ===============================================================================
     */
    public static class idTarget extends idEntity {
//	CLASS_PROTOTYPE( idTarget );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

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

     idTarget_Remove

     ===============================================================================
     */
    public static class idTarget_Remove extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_Remove );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_Remove>) idTarget_Remove::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            idEntity ent;

            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.PostEventMS(EV_Remove, 0);
                }
            }

            // delete our self when done
            PostEventMS(EV_Remove, 0);
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

     idTarget_Show

     ===============================================================================
     */
    public static class idTarget_Show extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_Show );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_Show>) idTarget_Show::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            idEntity ent;

            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.Show();
                }
            }

            // delete our self when done
            PostEventMS(EV_Remove, 0);
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

     idTarget_Damage

     ===============================================================================
     */
    public static class idTarget_Damage extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_Damage );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_Damage>) idTarget_Damage::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            String damage;
            idEntity ent;

            damage = this.spawnArgs.GetString("def_damage", "damage_generic");
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.Damage(this, this, getVec3_origin(), damage, 1.0f, INVALID_JOINT);
                }
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

     idTarget_SessionCommand

     ===============================================================================
     */
    public static class idTarget_SessionCommand extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		//	CLASS_PROTOTYPE(idTarget_SessionCommand );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SessionCommand>) idTarget_SessionCommand::Event_Activate);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            gameLocal.sessionCommand.oSet(this.spawnArgs.GetString("command"));
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

     idTarget_EndLevel

     Just a modified form of idTarget_SessionCommand
     ===============================================================================
     */
    public static class idTarget_EndLevel extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_EndLevel );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_EndLevel>) idTarget_EndLevel::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            final String[] nextMap = {null};

            if (ID_DEMO_BUILD) {
                if (this.spawnArgs.GetBool("endOfGame")) {
                    cvarSystem.SetCVarBool("g_nightmare", true);
                    gameLocal.sessionCommand.oSet("endofDemo");
                    return;
                }
            } else {
                if (this.spawnArgs.GetBool("endOfGame")) {
                    cvarSystem.SetCVarBool("g_nightmare", true);
                    gameLocal.sessionCommand.oSet("disconnect");
                    return;
                }
            }
            if (!this.spawnArgs.GetString("nextMap", "", nextMap)) {
                gameLocal.Printf("idTarget_SessionCommand::Event_Activate: no nextMap key\n");
                return;
            }

            if (this.spawnArgs.GetInt("devmap", "0") != 0) {
                gameLocal.sessionCommand.oSet("devmap ");	// only for special demos
            } else {
                gameLocal.sessionCommand.oSet("map ");
            }

            gameLocal.sessionCommand.oPluSet(nextMap[0]);
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

     idTarget_WaitForButton

     ===============================================================================
     */
    public static class idTarget_WaitForButton extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_WaitForButton );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_WaitForButton>) idTarget_WaitForButton::Event_Activate);
        }


        @Override
        public void Think() {
            idPlayer player;

            if ((this.thinkFlags & TH_THINK) != 0) {
                player = gameLocal.GetLocalPlayer();
                if ((player != null) && (NOT(player.oldButtons) & (BUTTON_ATTACK != 0)) && ((player.usercmd.buttons & BUTTON_ATTACK) != 0)) {
                    player.usercmd.buttons &= ~BUTTON_ATTACK;
                    BecomeInactive(TH_THINK);
                    ActivateTargets(player);
                }
            } else {
                BecomeInactive(TH_ALL);
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            if ((this.thinkFlags & TH_THINK) != 0) {
                BecomeInactive(TH_THINK);
            } else {
                // always allow during cinematics
                this.cinematic = true;
                BecomeActive(TH_THINK);
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

     idTarget_SetGlobalShaderTime

     ===============================================================================
     */
    public static class idTarget_SetGlobalShaderTime extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_SetGlobalShaderTime );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SetGlobalShaderTime>) idTarget_SetGlobalShaderTime::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            final int parm = this.spawnArgs.GetInt("globalParm");
            final float time = -MS2SEC(gameLocal.time);
            if ((parm >= 0) && (parm < MAX_GLOBAL_SHADER_PARMS)) {
                gameLocal.globalShaderParms[parm] = time;
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

     idTarget_SetShaderParm

     ===============================================================================
     */
    public static class idTarget_SetShaderParm extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_SetShaderParm );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SetShaderParm>) idTarget_SetShaderParm::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            idEntity ent;
            final float[] value = {0};
            final idVec3 color = new idVec3();
            int parmnum;

            // set the color on the targets
            if (this.spawnArgs.GetVector("_color", "1 1 1", color)) {
                for (i = 0; i < this.targets.Num(); i++) {
                    ent = this.targets.oGet(i).GetEntity();
                    if (ent != null) {
                        ent.SetColor(color.oGet(0), color.oGet(1), color.oGet(2));
                    }
                }
            }

            // set any shader parms on the targets
            for (parmnum = 0; parmnum < MAX_ENTITY_SHADER_PARMS; parmnum++) {
                if (this.spawnArgs.GetFloat(va("shaderParm%d", parmnum), "0", value)) {
                    for (i = 0; i < this.targets.Num(); i++) {
                        ent = this.targets.oGet(i).GetEntity();
                        if (ent != null) {
                            ent.SetShaderParm(parmnum, value[0]);
                        }
                    }
                    if (this.spawnArgs.GetBool("toggle") && ((value[0] == 0) || (value[0] == 1))) {
                        int val = (int) value[0];
                        val ^= 1;
                        value[0] = val;
                        this.spawnArgs.SetFloat(va("shaderParm%d", parmnum), value[0]);
                    }
                }
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

     idTarget_SetShaderTime

     ===============================================================================
     */
    public static class idTarget_SetShaderTime extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_SetShaderTime );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SetShaderTime>) idTarget_SetShaderTime::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            idEntity ent;
            float time;

            time = -MS2SEC(gameLocal.time);
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.SetShaderParm(SHADERPARM_TIMEOFFSET, time);
                    if (ent.IsType(idLight.class)) {
                        ((idLight) ent).SetLightParm(SHADERPARM_TIMEOFFSET, time);
                    }
                }
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

     idTarget_FadeEntity

     ===============================================================================
     */
    public static class idTarget_FadeEntity extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_FadeEntity );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_FadeEntity>) idTarget_FadeEntity::Event_Activate);
        }

        private final idVec4 fadeFrom;
        private int    fadeStart;
        private int    fadeEnd;
        //
        //

        public idTarget_FadeEntity() {
            this.fadeFrom = new idVec4();
            this.fadeStart = 0;
            this.fadeEnd = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec4(this.fadeFrom);
            savefile.WriteInt(this.fadeStart);
            savefile.WriteInt(this.fadeEnd);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec4(this.fadeFrom);
            this.fadeStart = savefile.ReadInt();
            this.fadeEnd = savefile.ReadInt();
        }

        @Override
        public void Think() {
            int i;
            idEntity ent;
            idVec4 color = new idVec4();
            final idVec4 fadeTo = new idVec4();
            float frac;

            if ((this.thinkFlags & TH_THINK) != 0) {
                GetColor(fadeTo);
                if (gameLocal.time >= this.fadeEnd) {
                    color = fadeTo;
                    BecomeInactive(TH_THINK);
                } else {
                    frac = (float) (gameLocal.time - this.fadeStart) / (float) (this.fadeEnd - this.fadeStart);
                    color.Lerp(this.fadeFrom, fadeTo, frac);
                }

                // set the color on the targets
                for (i = 0; i < this.targets.Num(); i++) {
                    ent = this.targets.oGet(i).GetEntity();
                    if (ent != null) {
                        ent.SetColor(color);
                    }
                }
            } else {
                BecomeInactive(TH_ALL);
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            idEntity ent;
            int i;

            if (0 == this.targets.Num()) {
                return;
            }

            // always allow during cinematics
            this.cinematic = true;
            BecomeActive(TH_THINK);

//	ent = this;
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.GetColor(this.fadeFrom);
                    break;
                }
            }

            this.fadeStart = gameLocal.time;
            this.fadeEnd = (int) (gameLocal.time + SEC2MS(this.spawnArgs.GetFloat("fadetime")));
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

     idTarget_LightFadeIn

     ===============================================================================
     */
    public static class idTarget_LightFadeIn extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_LightFadeIn );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_LightFadeIn>) idTarget_LightFadeIn::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            idEntity ent;
            idLight light;
            int i;
            float time;

            if (0 == this.targets.Num()) {
                return;
            }

            time = this.spawnArgs.GetFloat("fadetime");
//	ent = this;
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (null == ent) {
                    continue;
                }
                if (ent.IsType(idLight.class)) {
                    light = (idLight) ent;
                    light.FadeIn(time);
                } else {
                    gameLocal.Printf("'%s' targets non-light '%s'", this.name, ent.GetName());
                }
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

     idTarget_LightFadeOut

     ===============================================================================
     */
    public static class idTarget_LightFadeOut extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_LightFadeOut );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_LightFadeOut>) idTarget_LightFadeOut::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            idEntity ent;
            idLight light;
            int i;
            float time;

            if (0 == this.targets.Num()) {
                return;
            }

            time = this.spawnArgs.GetFloat("fadetime");
//	ent = this;
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (null == ent) {
                    continue;
                }
                if (ent.IsType(idLight.class)) {
                    light = (idLight) ent;
                    light.FadeOut(time);
                } else {
                    gameLocal.Printf("'%s' targets non-light '%s'", this.name, ent.GetName());
                }
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

     idTarget_Give

     ===============================================================================
     */
    public static class idTarget_Give extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_Give );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_Give>) idTarget_Give::Event_Activate);
        }


        @Override
        public void Spawn() {
            if (this.spawnArgs.GetBool("onSpawn")) {
                PostEventMS(EV_Activate, 50);
            }
        }
        private static int giveNum = 0;

        private void Event_Activate(idEventArg<idEntity> activator) {

            if (this.spawnArgs.GetBool("development") && (developer.GetInteger() == 0)) {
                return;
            }

            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                idKeyValue kv = this.spawnArgs.MatchPrefix("item", null);
                while (kv != null) {
                    final idDict dict = gameLocal.FindEntityDefDict(kv.GetValue().getData(), false);
                    if (dict != null) {
                        final idDict d2 = new idDict();
                        d2.Copy(dict);
                        d2.Set("name", va("givenitem_%d", giveNum++));
                        final idEntity[] ent = {null};
                        if (gameLocal.SpawnEntityDef(d2, ent) && (ent[0] != null) && ent[0].IsType(idItem.class)) {
                            final idItem item = (idItem) ent[0];
                            item.GiveToPlayer(gameLocal.GetLocalPlayer());
                        }
                    }
                    kv = this.spawnArgs.MatchPrefix("item", kv);
                }
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

     idTarget_GiveEmail

     ===============================================================================
     */
    public static class idTarget_GiveEmail extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_GiveEmail );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_GiveEmail>) idTarget_GiveEmail::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            final idPlayer player = gameLocal.GetLocalPlayer();
            final idDeclPDA pda = player.GetPDA();
            if (pda != null) {
                player.GiveEmail(this.spawnArgs.GetString("email"));
            } else {
                player.ShowTip(this.spawnArgs.GetString("text_infoTitle"), this.spawnArgs.GetString("text_PDANeeded"), true);
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

     idTarget_SetModel

     ===============================================================================
     */
    public static class idTarget_SetModel extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_SetModel );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SetModel>) idTarget_SetModel::Event_Activate);
        }


        @Override
        public void Spawn() {
            idStr model;

            model = new idStr(this.spawnArgs.GetString("newmodel"));
            if (declManager.FindType(DECL_MODELDEF, model, false) == null) {
                // precache the render model
                renderModelManager.FindModel(model);
                // precache .cm files only
                CollisionModel_local.collisionModelManager.LoadModel(model, true);
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            for (int i = 0; i < this.targets.Num(); i++) {
                final idEntity ent = this.targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.SetModel(this.spawnArgs.GetString("newmodel"));
                }
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

     idTarget_SetInfluence

     ===============================================================================
     */
    public static class idTarget_SetInfluence extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_SetInfluence );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SetInfluence>) idTarget_SetInfluence::Event_Activate);
            eventCallbacks.put(EV_RestoreInfluence, (eventCallback_t0<idTarget_SetInfluence>) idTarget_SetInfluence::Event_RestoreInfluence);
            eventCallbacks.put(EV_GatherEntities, (eventCallback_t0<idTarget_SetInfluence>) idTarget_SetInfluence::Event_GatherEntities);
            eventCallbacks.put(EV_Flash, (eventCallback_t2<idTarget_SetInfluence>) idTarget_SetInfluence::Event_Flash);
            eventCallbacks.put(EV_ClearFlash, (eventCallback_t1<idTarget_SetInfluence>) idTarget_SetInfluence::Event_ClearFlash);
        }


        private final idList<Integer>      lightList;
        private final idList<Integer>      guiList;
        private final idList<Integer>      soundList;
        private final idList<Integer>      genericList;
        private float                flashIn;
        private float                flashOut;
        private float                delay;
        private idStr                flashInSound;
        private idStr                flashOutSound;
        private idEntity             switchToCamera;
        private final idInterpolate<Float> fovSetting;
        private boolean              soundFaded;
        private boolean              restoreOnTrigger;
        //
        //

        public idTarget_SetInfluence() {
            this.lightList = new idList<>();
            this.guiList = new idList<>();
            this.soundList = new idList<>();
            this.genericList = new idList<>();
            this.flashIn = 0.0f;
            this.flashOut = 0.0f;
            this.delay = 0.0f;
            this.switchToCamera = null;
            this.fovSetting = new idInterpolate<>();
            this.soundFaded = false;
            this.restoreOnTrigger = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(this.lightList.Num());
            for (i = 0; i < this.lightList.Num(); i++) {
                savefile.WriteInt(this.lightList.oGet(i));
            }

            savefile.WriteInt(this.guiList.Num());
            for (i = 0; i < this.guiList.Num(); i++) {
                savefile.WriteInt(this.guiList.oGet(i));
            }

            savefile.WriteInt(this.soundList.Num());
            for (i = 0; i < this.soundList.Num(); i++) {
                savefile.WriteInt(this.soundList.oGet(i));
            }

            savefile.WriteInt(this.genericList.Num());
            for (i = 0; i < this.genericList.Num(); i++) {
                savefile.WriteInt(this.genericList.oGet(i));
            }

            savefile.WriteFloat(this.flashIn);
            savefile.WriteFloat(this.flashOut);

            savefile.WriteFloat(this.delay);

            savefile.WriteString(this.flashInSound);
            savefile.WriteString(this.flashOutSound);

            savefile.WriteObject(this.switchToCamera);

            savefile.WriteFloat(this.fovSetting.GetStartTime());
            savefile.WriteFloat(this.fovSetting.GetDuration());
            savefile.WriteFloat(this.fovSetting.GetStartValue());
            savefile.WriteFloat(this.fovSetting.GetEndValue());

            savefile.WriteBool(this.soundFaded);
            savefile.WriteBool(this.restoreOnTrigger);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            final int[] num = {0};
            final int[] itemNum = new int[1];
            final float[] set = new float[1];

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadInt(itemNum);
                this.lightList.Append(itemNum[0]);
            }

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadInt(itemNum);
                this.guiList.Append(itemNum[0]);
            }

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadInt(itemNum);
                this.soundList.Append(itemNum[0]);
            }

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadInt(itemNum);
                this.genericList.Append(itemNum[0]);
            }

            this.flashIn = savefile.ReadFloat();
            this.flashOut = savefile.ReadFloat();

            this.delay = savefile.ReadFloat();

            savefile.ReadString(this.flashInSound);
            savefile.ReadString(this.flashOutSound);

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/switchToCamera);

            savefile.ReadFloat(set);
            this.fovSetting.SetStartTime(set[0]);
            savefile.ReadFloat(set);
            this.fovSetting.SetDuration(set[0]);
            savefile.ReadFloat(set);
            this.fovSetting.SetStartValue(set[0]);
            savefile.ReadFloat(set);
            this.fovSetting.SetEndValue(set[0]);

            this.soundFaded = savefile.ReadBool();
            this.restoreOnTrigger = savefile.ReadBool();
        }

        @Override
        public void Spawn() {
            super.Spawn();

            PostEventMS(EV_GatherEntities, 0);
            this.flashIn = this.spawnArgs.GetFloat("flashIn", "0");
            this.flashOut = this.spawnArgs.GetFloat("flashOut", "0");
            this.flashInSound = new idStr(this.spawnArgs.GetString("snd_flashin"));
            this.flashOutSound = new idStr(this.spawnArgs.GetString("snd_flashout"));
            this.delay = this.spawnArgs.GetFloat("delay");
            this.soundFaded = false;
            this.restoreOnTrigger = false;

            // always allow during cinematics
            this.cinematic = true;
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            int i, j;
            idEntity ent;
            idLight light;
            idSound sound;
            idStaticEntity generic;
            String parm;
            String skin;
            boolean update;
            idVec3 color;
            final idVec4 colorTo = new idVec4();
            idPlayer player;

            player = gameLocal.GetLocalPlayer();

            if (this.spawnArgs.GetBool("triggerActivate")) {
                if (this.restoreOnTrigger) {
                    ProcessEvent(EV_RestoreInfluence);
                    this.restoreOnTrigger = false;
                    return;
                }
                this.restoreOnTrigger = true;
            }

            final float fadeTime = this.spawnArgs.GetFloat("fadeWorldSounds");

            if (this.delay > 0.0f) {
                PostEventSec(EV_Activate, this.delay, activator.value);
                this.delay = 0.0f;
                // start any sound fading now
                if (fadeTime != 0) {
                    gameSoundWorld.FadeSoundClasses(0, -40.0f, fadeTime);
                    this.soundFaded = true;
                }
                return;
            } else if ((fadeTime != 0) && !this.soundFaded) {
                gameSoundWorld.FadeSoundClasses(0, -40.0f, fadeTime);
                this.soundFaded = true;
            }

            if (this.spawnArgs.GetBool("triggerTargets")) {
                ActivateTargets(activator.value);
            }

            if (this.flashIn != 0) {
                PostEventSec(EV_Flash, 0.0f, this.flashIn, 0);
            }

            parm = this.spawnArgs.GetString("snd_influence");
            if (isNotNullOrEmpty(parm)) {
                PostEventSec(EV_StartSoundShader, this.flashIn, parm, SND_CHANNEL_ANY);
            }

            if (this.switchToCamera != null) {
                this.switchToCamera.PostEventSec(EV_Activate, this.flashIn + 0.05f, this);
            }

            final float fov = this.spawnArgs.GetInt("fov");
            if (fov != 0) {
                this.fovSetting.Init(gameLocal.time, SEC2MS(this.spawnArgs.GetFloat("fovTime")), player.DefaultFov(), fov);
                BecomeActive(TH_THINK);
            }

            for (i = 0; i < this.genericList.Num(); i++) {
                ent = gameLocal.entities[this.genericList.oGet(i)];
                if (ent == null) {
                    continue;
                }
                generic = (idStaticEntity) ent;
                color = generic.spawnArgs.GetVector("color_demonic");
                colorTo.Set(color.x, color.y, color.z, 1.0f);
                generic.Fade(colorTo, this.spawnArgs.GetFloat("fade_time", "0.25"));
            }

            for (i = 0; i < this.lightList.Num(); i++) {
                ent = gameLocal.entities[this.lightList.oGet(i)];
                if ((ent == null) || !ent.IsType(idLight.class)) {
                    continue;
                }
                light = (idLight) ent;
                parm = light.spawnArgs.GetString("mat_demonic");
                if (isNotNullOrEmpty(parm)) {
                    light.SetShader(parm);
                }

                color = light.spawnArgs.GetVector("_color");
                color = light.spawnArgs.GetVector("color_demonic", color.ToString());
                colorTo.Set(color.x, color.y, color.z, 1.0f);
                light.Fade(colorTo, this.spawnArgs.GetFloat("fade_time", "0.25"));
            }

            for (i = 0; i < this.soundList.Num(); i++) {
                ent = gameLocal.entities[this.soundList.oGet(i)];
                if ((ent == null) || !ent.IsType(idSound.class)) {
                    continue;
                }
                sound = (idSound) ent;
                parm = sound.spawnArgs.GetString("snd_demonic");
                if (isNotNullOrEmpty(parm)) {
                    if (sound.spawnArgs.GetBool("overlayDemonic")) {
                        sound.StartSound("snd_demonic", SND_CHANNEL_DEMONIC, 0, false, null);
                    } else {
                        sound.StopSound(etoi(SND_CHANNEL_ANY), false);
                        sound.SetSound(parm);
                    }
                }
            }

            for (i = 0; i < this.guiList.Num(); i++) {
                ent = gameLocal.entities[this.guiList.oGet(i)];
                if ((ent == null) || (ent.GetRenderEntity() == null)) {
                    continue;
                }
                update = false;
                for (j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                    if ((ent.GetRenderEntity().gui[j] != null)
                            && (ent.spawnArgs.FindKey(j == 0 ? "gui_demonic" : va("gui_demonic%d", j + 1)) != null)) {
                        ent.GetRenderEntity().gui[j] = uiManager.FindGui(ent.spawnArgs.GetString(j == 0 ? "gui_demonic" : va("gui_demonic%d", j + 1)), true);
                        update = true;
                    }
                }

                if (update) {
                    ent.UpdateVisuals();
                    ent.Present();
                }
            }

            player.SetInfluenceLevel(this.spawnArgs.GetInt("influenceLevel"));

            final int snapAngle = this.spawnArgs.GetInt("snapAngle");
            if (snapAngle != 0) {
                final idAngles ang = new idAngles(0, snapAngle, 0);
                player.SetViewAngles(ang);
                player.SetAngles(ang);
            }

            if (this.spawnArgs.GetBool("effect_vision")) {
                parm = this.spawnArgs.GetString("mtrVision");
                skin = this.spawnArgs.GetString("skinVision");
                player.SetInfluenceView(parm, skin, this.spawnArgs.GetInt("visionRadius"), this);
            }

            parm = this.spawnArgs.GetString("mtrWorld");
            if (isNotNullOrEmpty(parm)) {
                gameLocal.SetGlobalMaterial(declManager.FindMaterial(parm));
            }

            if (!this.restoreOnTrigger) {
                PostEventMS(EV_RestoreInfluence, (int) SEC2MS(this.spawnArgs.GetFloat("time")));
            }
        }

        private void Event_RestoreInfluence() {
            int i, j;
            idEntity ent;
            idLight light;
            idSound sound;
            idStaticEntity generic;
            boolean update;
            idVec3 color;
            final idVec4 colorTo = new idVec4();

            if (this.flashOut != 0) {
                PostEventSec(EV_Flash, 0.0f, this.flashOut, 1);
            }

            if (this.switchToCamera != null) {
                this.switchToCamera.PostEventMS(EV_Activate, 0.0f, this);
            }

            for (i = 0; i < this.genericList.Num(); i++) {
                ent = gameLocal.entities[this.genericList.oGet(i)];
                if (ent == null) {
                    continue;
                }
                generic = (idStaticEntity) ent;
                colorTo.Set(1.0f, 1.0f, 1.0f, 1.0f);
                generic.Fade(colorTo, this.spawnArgs.GetFloat("fade_time", "0.25"));
            }

            for (i = 0; i < this.lightList.Num(); i++) {
                ent = gameLocal.entities[this.lightList.oGet(i)];
                if ((ent == null) || !ent.IsType(idLight.class)) {
                    continue;
                }
                light = (idLight) ent;
                if (!light.spawnArgs.GetBool("leave_demonic_mat")) {
                    final String texture = light.spawnArgs.GetString("texture", "lights/squarelight1");
                    light.SetShader(texture);
                }
                color = light.spawnArgs.GetVector("_color");
                colorTo.Set(color.x, color.y, color.z, 1.0f);
                light.Fade(colorTo, this.spawnArgs.GetFloat("fade_time", "0.25"));
            }

            for (i = 0; i < this.soundList.Num(); i++) {
                ent = gameLocal.entities[this.soundList.oGet(i)];
                if ((ent == null) || !ent.IsType(idSound.class)) {
                    continue;
                }
                sound = (idSound) ent;
                sound.StopSound(etoi(SND_CHANNEL_ANY), false);
                sound.SetSound(sound.spawnArgs.GetString("s_shader"));
            }

            for (i = 0; i < this.guiList.Num(); i++) {
                ent = gameLocal.entities[this.guiList.oGet(i)];
                if ((ent == null) || (GetRenderEntity() == null)) {
                    continue;
                }
                update = false;
                for (j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                    if (ent.GetRenderEntity().gui[j] != null) {
                        ent.GetRenderEntity().gui[j] = uiManager.FindGui(ent.spawnArgs.GetString(j == 0 ? "gui" : va("gui%d", j + 1)));
                        update = true;
                    }
                }
                if (update) {
                    ent.UpdateVisuals();
                    ent.Present();
                }
            }

            final idPlayer player = gameLocal.GetLocalPlayer();
            player.SetInfluenceLevel(0);
            player.SetInfluenceView(null, null, 0.0f, null);
            player.SetInfluenceFov(0);
            gameLocal.SetGlobalMaterial(null);
            final float fadeTime = this.spawnArgs.GetFloat("fadeWorldSounds");
            if (fadeTime != 0) {
                gameSoundWorld.FadeSoundClasses(0, 0.0f, fadeTime / 2.0f);
            }

        }

        private void Event_GatherEntities() {
            int i, listedEntities;
            final idEntity[] entityList = new idEntity[MAX_GENTITIES];

            final boolean demonicOnly = this.spawnArgs.GetBool("effect_demonic");
            boolean lights = this.spawnArgs.GetBool("effect_lights");
            boolean sounds = this.spawnArgs.GetBool("effect_sounds");
            boolean guis = this.spawnArgs.GetBool("effect_guis");
            boolean models = this.spawnArgs.GetBool("effect_models");
            boolean vision = this.spawnArgs.GetBool("effect_vision");
            final boolean targetsOnly = this.spawnArgs.GetBool("targetsOnly");

            this.lightList.Clear();
            this.guiList.Clear();
            this.soundList.Clear();

            if (this.spawnArgs.GetBool("effect_all")) {
                lights = sounds = guis = models = vision = true;
            }

            if (targetsOnly) {
                listedEntities = this.targets.Num();
                for (i = 0; i < listedEntities; i++) {
                    entityList[i] = this.targets.oGet(i).GetEntity();
                }
            } else {
                final float radius = this.spawnArgs.GetFloat("radius");
                listedEntities = gameLocal.EntitiesWithinRadius(GetPhysics().GetOrigin(), radius, entityList, MAX_GENTITIES);
            }

            for (i = 0; i < listedEntities; i++) {
                final idEntity ent = entityList[i];
                if (ent != null) {
                    if (lights && ent.IsType(idLight.class) && (ent.spawnArgs.FindKey("color_demonic") != null)) {
                        this.lightList.Append(ent.entityNumber);
                        continue;
                    }
                    if (sounds && ent.IsType(idSound.class) && (ent.spawnArgs.FindKey("snd_demonic") != null)) {
                        this.soundList.Append(ent.entityNumber);
                        continue;
                    }
                    if (guis && (ent.GetRenderEntity() != null) && (ent.GetRenderEntity().gui[0] != null) && (ent.spawnArgs.FindKey("gui_demonic") != null)) {
                        this.guiList.Append(ent.entityNumber);
                        continue;
                    }
                    if (ent.IsType(idStaticEntity.class) && (ent.spawnArgs.FindKey("color_demonic") != null)) {
                        this.genericList.Append(ent.entityNumber);
//                        continue;
                    }
                }
            }
            String temp;
            temp = this.spawnArgs.GetString("switchToView");
            this.switchToCamera = (temp.length() != 0) ? gameLocal.FindEntity(temp) : null;

        }

        private void Event_Flash(idEventArg<Float> _flash, idEventArg<Integer> _out) {
            final float flash = _flash.value;
            final int out = _out.value;
            final idPlayer player = gameLocal.GetLocalPlayer();
            player.playerView.Fade(new idVec4(1, 1, 1, 1), (int) flash);
            idSoundShader shader;
            if ((0 == out) && (this.flashInSound.Length() != 0)) {
                shader = declManager.FindSound(this.flashInSound);
                player.StartSoundShader(shader, SND_CHANNEL_VOICE, 0, false, null);
            } else if ((out != 0) && ((this.flashOutSound.Length() != 0) || (this.flashInSound.Length() != 0))) {
                shader = declManager.FindSound(this.flashOutSound.Length() != 0 ? this.flashOutSound : this.flashInSound);
                player.StartSoundShader(shader, SND_CHANNEL_VOICE, 0, false, null);
            }
            PostEventSec(EV_ClearFlash, flash, flash);
        }

        private void Event_ClearFlash(idEventArg<Float> flash) {
            final idPlayer player = gameLocal.GetLocalPlayer();
            player.playerView.Fade(getVec4_zero(), flash.value.intValue());
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                final idPlayer player = gameLocal.GetLocalPlayer();
                player.SetInfluenceFov(this.fovSetting.GetCurrentValue(gameLocal.time));
                if (this.fovSetting.IsDone(gameLocal.time)) {
                    if (!this.spawnArgs.GetBool("leaveFOV")) {
                        player.SetInfluenceFov(0);
                    }
                    BecomeInactive(TH_THINK);
                }
            } else {
                BecomeInactive(TH_ALL);
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

     idTarget_SetKeyVal

     ===============================================================================
     */
    public static class idTarget_SetKeyVal extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_SetKeyVal );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SetKeyVal>) idTarget_SetKeyVal::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            String key, val;
            idEntity ent;
            idKeyValue kv;
            int n;

            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if (ent != null) {
                    kv = this.spawnArgs.MatchPrefix("keyval");
                    while (kv != null) {
                        n = kv.GetValue().Find(";");
                        if (n > 0) {
                            key = kv.GetValue().Left(n).getData();
                            val = kv.GetValue().Right(kv.GetValue().Length() - n - 1).getData();
                            ent.spawnArgs.Set(key, val);
                            for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                                if (ent.GetRenderEntity().gui[j] != null) {
                                    if (idStr.Icmpn(key, "gui_", 4) == 0) {
                                        ent.GetRenderEntity().gui[j].SetStateString(key, val);
                                        ent.GetRenderEntity().gui[j].StateChanged(gameLocal.time);
                                    }
                                }
                            }
                        }
                        kv = this.spawnArgs.MatchPrefix("keyval", kv);
                    }
                    ent.UpdateChangeableSpawnArgs(null);
                    ent.UpdateVisuals();
                    ent.Present();
                }
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

     idTarget_SetFov

     ===============================================================================
     */
    public static class idTarget_SetFov extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_SetFov );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SetFov>) idTarget_SetFov::Event_Activate);
        }


        private idInterpolate<Integer> fovSetting;
        //
        //

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteFloat(this.fovSetting.GetStartTime());
            savefile.WriteFloat(this.fovSetting.GetDuration());
            savefile.WriteFloat(this.fovSetting.GetStartValue());
            savefile.WriteFloat(this.fovSetting.GetEndValue());
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final float[] setting = new float[1];

            savefile.ReadFloat(setting);
            this.fovSetting.SetStartTime(setting[0]);
            savefile.ReadFloat(setting);
            this.fovSetting.SetDuration(setting[0]);
            savefile.ReadFloat(setting);
            this.fovSetting.SetStartValue((int) setting[0]);
            savefile.ReadFloat(setting);
            this.fovSetting.SetEndValue((int) setting[0]);

            this.fovSetting.GetCurrentValue(gameLocal.time);
        }

        @Override
        public void Think() {
            if ((this.thinkFlags & TH_THINK) != 0) {
                final idPlayer player = gameLocal.GetLocalPlayer();
                player.SetInfluenceFov(this.fovSetting.GetCurrentValue(gameLocal.time));
                if (this.fovSetting.IsDone(gameLocal.time)) {
                    player.SetInfluenceFov(0.0f);
                    BecomeInactive(TH_THINK);
                }
            } else {
                BecomeInactive(TH_ALL);
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            // always allow during cinematics
            this.cinematic = true;

            final idPlayer player = gameLocal.GetLocalPlayer();
            this.fovSetting.Init(gameLocal.time, SEC2MS(this.spawnArgs.GetFloat("time")), (int) (player != null ? player.DefaultFov() : g_fov.GetFloat()), (int) this.spawnArgs.GetFloat("fov"));
            BecomeActive(TH_THINK);
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

     idTarget_SetPrimaryObjective

     ===============================================================================
     */
    public static class idTarget_SetPrimaryObjective extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_SetPrimaryObjective );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_SetPrimaryObjective>) idTarget_SetPrimaryObjective::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if ((player != null) && (player.objectiveSystem != null)) {
                player.objectiveSystem.SetStateString("missionobjective", this.spawnArgs.GetString("text", common.GetLanguageDict().GetString("#str_04253")));
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

     idTarget_LockDoor

     ===============================================================================
     */
    public static class idTarget_LockDoor extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_LockDoor );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_LockDoor>) idTarget_LockDoor::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            idEntity ent;
            int lock;

            lock = this.spawnArgs.GetInt("locked", "1");
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if ((ent != null) && ent.IsType(idDoor.class)) {
                    if (((idDoor) ent).IsLocked() != 0) {
                        ((idDoor) ent).Lock(0);
                    } else {
                        ((idDoor) ent).Lock(lock);
                    }
                }
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

     idTarget_CallObjectFunction

     ===============================================================================
     */
    public static class idTarget_CallObjectFunction extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_CallObjectFunction );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_CallObjectFunction>) idTarget_CallObjectFunction::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            idEntity ent;
            function_t func;
            String funcName;
            idThread thread;

            funcName = this.spawnArgs.GetString("call");
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if ((ent != null) && ent.scriptObject.HasObject()) {
                    func = ent.scriptObject.GetFunction(funcName);
                    if (NOT(func)) {
                        gameLocal.Error("Function '%s' not found on entity '%s' for function call from '%s'", funcName, ent.name, this.name);
                    }
                    if (func.type.NumParameters() != 1) {
                        gameLocal.Error("Function '%s' on entity '%s' has the wrong number of parameters for function call from '%s'", funcName, ent.name, this.name);
                    }
                    if (!ent.scriptObject.GetTypeDef().Inherits(func.type.GetParmType(0))) {
                        gameLocal.Error("Function '%s' on entity '%s' is the wrong type for function call from '%s'", funcName, ent.name, this.name);
                    }
                    // create a thread and call the function
                    thread = new idThread();
                    thread.CallFunction(ent, func, true);
                    thread.Start();
                }
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

     idTarget_LockDoor

     ===============================================================================
     */
    public static class idTarget_EnableLevelWeapons extends idTarget {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idTarget_EnableLevelWeapons );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_EnableLevelWeapons>) idTarget_EnableLevelWeapons::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            int i;
            String weap;

            gameLocal.world.spawnArgs.SetBool("no_Weapons", this.spawnArgs.GetBool("disable"));

            if (this.spawnArgs.GetBool("disable")) {
                for (i = 0; i < gameLocal.numClients; i++) {
                    if (gameLocal.entities[ i] != null) {
                        gameLocal.entities[ i].ProcessEvent(EV_Player_DisableWeapon);
                    }
                }
            } else {
                weap = this.spawnArgs.GetString("weapon");
                for (i = 0; i < gameLocal.numClients; i++) {
                    if (gameLocal.entities[ i] != null) {
                        gameLocal.entities[ i].ProcessEvent(EV_Player_EnableWeapon);
                        if (isNotNullOrEmpty(weap)) {
                            gameLocal.entities[ i].PostEventSec(EV_Player_SelectWeapon, 0.5f, weap);
                        }
                    }
                }
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

     idTarget_Tip

     ===============================================================================
     */
    public static class idTarget_Tip extends idTarget {
        // CLASS_PROTOTYPE( idTarget_Tip );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_Tip>) idTarget_Tip::Event_Activate);
            eventCallbacks.put(EV_TipOff, (eventCallback_t0<idTarget_Tip>) idTarget_Tip::Event_TipOff);
            eventCallbacks.put(EV_GetPlayerPos, (eventCallback_t0<idTarget_Tip>) idTarget_Tip::Event_GetPlayerPos);
        }


        private idVec3 playerPos;
        //
        //

        public idTarget_Tip() {
            this.playerPos = new idVec3();
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec3(this.playerPos);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(this.playerPos);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                if (player.IsTipVisible()) {
                    PostEventSec(EV_Activate, 5.1f, activator.value);
                    return;
                }
                player.ShowTip(this.spawnArgs.GetString("text_title"), this.spawnArgs.GetString("text_tip"), false);
                PostEventMS(EV_GetPlayerPos, 2000);
            }
        }

        private void Event_TipOff() {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                final idVec3 v = player.GetPhysics().GetOrigin().oMinus(this.playerPos);
                if (v.Length() > 96.0f) {
                    player.HideTip();
                } else {
                    PostEventMS(EV_TipOff, 100);
                }
            }
        }

        private void Event_GetPlayerPos() {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                this.playerPos = player.GetPhysics().GetOrigin();
                PostEventMS(EV_TipOff, 100);
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

     idTarget_GiveSecurity

     ===============================================================================
     */
    public static class idTarget_GiveSecurity extends idTarget {
        // CLASS_PROTOTYPE( idTarget_GiveSecurity );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_GiveSecurity>) idTarget_GiveSecurity::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            final idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                player.GiveSecurity(this.spawnArgs.GetString("text_security"));
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

     idTarget_RemoveWeapons

     ===============================================================================
     */
    public static class idTarget_RemoveWeapons extends idTarget {
        // CLASS_PROTOTYPE( idTarget_RemoveWeapons );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_RemoveWeapons>) idTarget_RemoveWeapons::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            for (int i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.entities[ i] != null) {
                    final idPlayer player = (idPlayer) gameLocal.entities[i];
                    idKeyValue kv = this.spawnArgs.MatchPrefix("weapon", null);
                    while (kv != null) {
                        player.RemoveWeapon(kv.GetValue().getData());
                        kv = this.spawnArgs.MatchPrefix("weapon", kv);
                    }
                    player.SelectWeapon(player.weapon_fists, true);
                }
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

     idTarget_LevelTrigger

     ===============================================================================
     */
    public static class idTarget_LevelTrigger extends idTarget {
        // CLASS_PROTOTYPE( idTarget_LevelTrigger );//TODO:understand this fucking macro
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_LevelTrigger>) idTarget_LevelTrigger::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            for (int i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.entities[ i] != null) {
                    final idPlayer player = (idPlayer) gameLocal.entities[i];
                    player.SetLevelTrigger(this.spawnArgs.GetString("levelName"), this.spawnArgs.GetString("triggerName"));
                }
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

     idTarget_EnableStamina

     ===============================================================================
     */
    public static class idTarget_EnableStamina extends idTarget {
        // CLASS_PROTOTYPE( idTarget_EnableStamina );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_EnableStamina>) idTarget_EnableStamina::Event_Activate);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            for (int i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.entities[ i] != null) {
                    final idPlayer player = (idPlayer) gameLocal.entities[i];
                    if (this.spawnArgs.GetBool("enable")) {
                        pm_stamina.SetFloat(player.spawnArgs.GetFloat("pm_stamina"));
                    } else {
                        pm_stamina.SetFloat(0.0f);
                    }
                }
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

     idTarget_FadeSoundClass

     ===============================================================================
     */
    public static class idTarget_FadeSoundClass extends idTarget {
        // CLASS_PROTOTYPE( idTarget_FadeSoundClass );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idTarget.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idTarget_FadeSoundClass>) idTarget_FadeSoundClass::Event_Activate);
            eventCallbacks.put(EV_RestoreVolume, (eventCallback_t0<idTarget_FadeSoundClass>) idTarget_FadeSoundClass::Event_RestoreVolume);
        }


        private void Event_Activate(idEventArg<idEntity> activator) {
            final float fadeTime = this.spawnArgs.GetFloat("fadeTime");
            final float fadeDB = this.spawnArgs.GetFloat("fadeDB");
            final float fadeDuration = this.spawnArgs.GetFloat("fadeDuration");
            final int fadeClass = this.spawnArgs.GetInt("fadeClass");
            // start any sound fading now
            if (fadeTime != 0) {
                gameSoundWorld.FadeSoundClasses(fadeClass, this.spawnArgs.GetBool("fadeIn") ? fadeDB : /*0.0f */ -fadeDB, fadeTime);
                if (fadeDuration != 0) {
                    PostEventSec(EV_RestoreVolume, fadeDuration);
                }
            }
        }

        private void Event_RestoreVolume() {
            final float fadeTime = this.spawnArgs.GetFloat("fadeTime");
            final float fadeDB = this.spawnArgs.GetFloat("fadeDB");
            final int fadeClass = this.spawnArgs.GetInt("fadeClass");
            // restore volume
            gameSoundWorld.FadeSoundClasses(0, fadeDB, fadeTime);
        }

        @Override
        public eventCallback_t getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }
}
