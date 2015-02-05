package neo.Game;

import neo.CM.CollisionModel_local;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_StartSoundShader;
import static neo.Game.Entity.TH_ALL;
import static neo.Game.Entity.TH_THINK;
import neo.Game.Entity.idEntity;
import static neo.Game.GameSys.Class.EV_Remove;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.developer;
import static neo.Game.GameSys.SysCvar.g_fov;
import static neo.Game.GameSys.SysCvar.pm_stamina;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_DEMONIC;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_VOICE;
import static neo.Game.Game_local.gameSoundWorld;
import neo.Game.Item.idItem;
import neo.Game.Light.idLight;
import neo.Game.Misc.idStaticEntity;
import neo.Game.Mover.idDoor;
import static neo.Game.Player.EV_Player_DisableWeapon;
import static neo.Game.Player.EV_Player_EnableWeapon;
import static neo.Game.Player.EV_Player_SelectWeapon;
import neo.Game.Player.idPlayer;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Thread.idThread;
import neo.Game.Sound.idSound;
import neo.Game.Target.idTarget;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import neo.Sound.snd_shader.idSoundShader;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.BuildDefines.ID_DEMO_BUILD;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import neo.framework.DeclPDA.idDeclPDA;
import static neo.framework.UsercmdGen.BUTTON_ATTACK;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Interpolate.idInterpolate;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import static neo.idlib.math.Vector.vec3_origin;
import static neo.idlib.math.Vector.vec4_zero;
import static neo.ui.UserInterface.uiManager;

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

     idTarget_Remove

     ===============================================================================
     */
    public static class idTarget_Remove extends idTarget {
        // CLASS_PROTOTYPE( idTarget_Remove );

        private void Event_Activate(idEntity activator) {
            int i;
            idEntity ent;

            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.PostEventMS(EV_Remove, 0);
                }
            }

            // delete our self when done
            PostEventMS(EV_Remove, 0);
        }

    };


    /*
     ===============================================================================

     idTarget_Show

     ===============================================================================
     */
    public static class idTarget_Show extends idTarget {
        // CLASS_PROTOTYPE( idTarget_Show );

        private void Event_Activate(idEntity activator) {
            int i;
            idEntity ent;

            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.Show();
                }
            }

            // delete our self when done
            PostEventMS(EV_Remove, 0);
        }
    };


    /*
     ===============================================================================

     idTarget_Damage

     ===============================================================================
     */
    public static class idTarget_Damage extends idTarget {
        // CLASS_PROTOTYPE( idTarget_Damage );

        private void Event_Activate(idEntity activator) {
            int i;
            String damage;
            idEntity ent;

            damage = spawnArgs.GetString("def_damage", "damage_generic");
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.Damage(this, this, vec3_origin, damage, 1.0f, INVALID_JOINT);
                }
            }
        }
    };


    /*
     ===============================================================================

     idTarget_SessionCommand

     ===============================================================================
     */
    public static class idTarget_SessionCommand extends idTarget {
//	CLASS_PROTOTYPE(idTarget_SessionCommand );

        private void Event_Activate(idEntity activator) {
            gameLocal.sessionCommand.oSet(spawnArgs.GetString("command"));
        }
    };


    /*
     ===============================================================================

     idTarget_EndLevel

     Just a modified form of idTarget_SessionCommand
     ===============================================================================
     */
    public static class idTarget_EndLevel extends idTarget {
        // CLASS_PROTOTYPE( idTarget_EndLevel );

        private void Event_Activate(idEntity activator) {
            String[] nextMap = {null};

            if (ID_DEMO_BUILD) {
                if (spawnArgs.GetBool("endOfGame")) {
                    cvarSystem.SetCVarBool("g_nightmare", true);
                    gameLocal.sessionCommand.oSet("endofDemo");
                    return;
                }
            } else {
                if (spawnArgs.GetBool("endOfGame")) {
                    cvarSystem.SetCVarBool("g_nightmare", true);
                    gameLocal.sessionCommand.oSet("disconnect");
                    return;
                }
            }
            if (!spawnArgs.GetString("nextMap", "", nextMap)) {
                gameLocal.Printf("idTarget_SessionCommand::Event_Activate: no nextMap key\n");
                return;
            }

            if (spawnArgs.GetInt("devmap", "0") != 0) {
                gameLocal.sessionCommand.oSet("devmap ");	// only for special demos
            } else {
                gameLocal.sessionCommand.oSet("map ");
            }

            gameLocal.sessionCommand.oPluSet(nextMap[0]);
        }
    };


    /*
     ===============================================================================

     idTarget_WaitForButton

     ===============================================================================
     */
    public static class idTarget_WaitForButton extends idTarget {
        // CLASS_PROTOTYPE( idTarget_WaitForButton );

        @Override
        public void Think() {
            idPlayer player;

            if ((thinkFlags & TH_THINK) != 0) {
                player = gameLocal.GetLocalPlayer();
                if (player != null && (NOT(player.oldButtons) & BUTTON_ATTACK != 0) && ((player.usercmd.buttons & BUTTON_ATTACK) != 0)) {
                    player.usercmd.buttons &= ~BUTTON_ATTACK;
                    BecomeInactive(TH_THINK);
                    ActivateTargets(player);
                }
            } else {
                BecomeInactive(TH_ALL);
            }
        }

        private void Event_Activate(idEntity activator) {
            if ((thinkFlags & TH_THINK) != 0) {
                BecomeInactive(TH_THINK);
            } else {
                // always allow during cinematics
                cinematic = true;
                BecomeActive(TH_THINK);
            }
        }
    };

    /*
     ===============================================================================

     idTarget_SetGlobalShaderTime

     ===============================================================================
     */
    public static class idTarget_SetGlobalShaderTime extends idTarget {
        // CLASS_PROTOTYPE( idTarget_SetGlobalShaderTime );

        private void Event_Activate(idEntity activator) {
            int parm = spawnArgs.GetInt("globalParm");
            float time = -MS2SEC(gameLocal.time);
            if (parm >= 0 && parm < MAX_GLOBAL_SHADER_PARMS) {
                gameLocal.globalShaderParms[parm] = time;
            }
        }
    };


    /*
     ===============================================================================

     idTarget_SetShaderParm

     ===============================================================================
     */
    public static class idTarget_SetShaderParm extends idTarget {
        // CLASS_PROTOTYPE( idTarget_SetShaderParm );

        private void Event_Activate(idEntity activator) {
            int i;
            idEntity ent;
            float[] value = {0};
            idVec3 color = new idVec3();
            int parmnum;

            // set the color on the targets
            if (spawnArgs.GetVector("_color", "1 1 1", color)) {
                for (i = 0; i < targets.Num(); i++) {
                    ent = targets.oGet(i).GetEntity();
                    if (ent != null) {
                        ent.SetColor(color.oGet(0), color.oGet(1), color.oGet(2));
                    }
                }
            }

            // set any shader parms on the targets
            for (parmnum = 0; parmnum < MAX_ENTITY_SHADER_PARMS; parmnum++) {
                if (spawnArgs.GetFloat(va("shaderParm%d", parmnum), "0", value)) {
                    for (i = 0; i < targets.Num(); i++) {
                        ent = targets.oGet(i).GetEntity();
                        if (ent != null) {
                            ent.SetShaderParm(parmnum, value[0]);
                        }
                    }
                    if (spawnArgs.GetBool("toggle") && (value[0] == 0 || value[0] == 1)) {
                        int val = (int) value[0];
                        val ^= 1;
                        value[0] = val;
                        spawnArgs.SetFloat(va("shaderParm%d", parmnum), value[0]);
                    }
                }
            }
        }
    };


    /*
     ===============================================================================

     idTarget_SetShaderTime

     ===============================================================================
     */
    public static class idTarget_SetShaderTime extends idTarget {
        // CLASS_PROTOTYPE( idTarget_SetShaderTime );

        private void Event_Activate(idEntity activator) {
            int i;
            idEntity ent;
            float time;

            time = -MS2SEC(gameLocal.time);
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.SetShaderParm(SHADERPARM_TIMEOFFSET, time);
                    if (ent.IsType(idLight.class)) {
                        ((idLight) ent).SetLightParm(SHADERPARM_TIMEOFFSET, time);
                    }
                }
            }
        }
    };

    /*
     ===============================================================================

     idTarget_FadeEntity

     ===============================================================================
     */
    public static class idTarget_FadeEntity extends idTarget {
        // CLASS_PROTOTYPE( idTarget_FadeEntity );

        private idVec4 fadeFrom;
        private int fadeStart;
        private int fadeEnd;
        //
        //

        public idTarget_FadeEntity() {
            fadeFrom.Zero();
            fadeStart = 0;
            fadeEnd = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec4(fadeFrom);
            savefile.WriteInt(fadeStart);
            savefile.WriteInt(fadeEnd);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec4(fadeFrom);
            fadeStart = savefile.ReadInt();
            fadeEnd = savefile.ReadInt();
        }

        @Override
        public void Think() {
            int i;
            idEntity ent;
            idVec4 color = new idVec4();
            idVec4 fadeTo = new idVec4();
            float frac;

            if ((thinkFlags & TH_THINK) != 0) {
                GetColor(fadeTo);
                if (gameLocal.time >= fadeEnd) {
                    color = fadeTo;
                    BecomeInactive(TH_THINK);
                } else {
                    frac = (float) (gameLocal.time - fadeStart) / (float) (fadeEnd - fadeStart);
                    color.Lerp(fadeFrom, fadeTo, frac);
                }

                // set the color on the targets
                for (i = 0; i < targets.Num(); i++) {
                    ent = targets.oGet(i).GetEntity();
                    if (ent != null) {
                        ent.SetColor(color);
                    }
                }
            } else {
                BecomeInactive(TH_ALL);
            }
        }

        private void Event_Activate(idEntity activator) {
            idEntity ent;
            int i;

            if (0 == targets.Num()) {
                return;
            }

            // always allow during cinematics
            cinematic = true;
            BecomeActive(TH_THINK);

//	ent = this;
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.GetColor(fadeFrom);
                    break;
                }
            }

            fadeStart = gameLocal.time;
            fadeEnd = (int) (gameLocal.time + SEC2MS(spawnArgs.GetFloat("fadetime")));
        }
    };

    /*
     ===============================================================================

     idTarget_LightFadeIn

     ===============================================================================
     */
    public static class idTarget_LightFadeIn extends idTarget {
        // CLASS_PROTOTYPE( idTarget_LightFadeIn );

        private void Event_Activate(idEntity activator) {
            idEntity ent;
            idLight light;
            int i;
            float time;

            if (0 == targets.Num()) {
                return;
            }

            time = spawnArgs.GetFloat("fadetime");
//	ent = this;
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (null == ent) {
                    continue;
                }
                if (ent.IsType(idLight.class)) {
                    light = (idLight) ent;
                    light.FadeIn(time);
                } else {
                    gameLocal.Printf("'%s' targets non-light '%s'", name, ent.GetName());
                }
            }
        }
    };

    /*
     ===============================================================================

     idTarget_LightFadeOut

     ===============================================================================
     */
    public static class idTarget_LightFadeOut extends idTarget {
        // CLASS_PROTOTYPE( idTarget_LightFadeOut );

        private void Event_Activate(idEntity activator) {
            idEntity ent;
            idLight light;
            int i;
            float time;

            if (0 == targets.Num()) {
                return;
            }

            time = spawnArgs.GetFloat("fadetime");
//	ent = this;
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (null == ent) {
                    continue;
                }
                if (ent.IsType(idLight.class)) {
                    light = (idLight) ent;
                    light.FadeOut(time);
                } else {
                    gameLocal.Printf("'%s' targets non-light '%s'", name, ent.GetName());
                }
            }
        }
    };

    /*
     ===============================================================================

     idTarget_Give

     ===============================================================================
     */
    public static class idTarget_Give extends idTarget {
        // CLASS_PROTOTYPE( idTarget_Give );

        @Override
        public void Spawn() {
            if (spawnArgs.GetBool("onSpawn")) {
                PostEventMS(EV_Activate, 50);
            }
        }
        private static int giveNum = 0;

        private void Event_Activate(idEntity activator) {

            if (spawnArgs.GetBool("development") && developer.GetInteger() == 0) {
                return;
            }

            idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                idKeyValue kv = spawnArgs.MatchPrefix("item", null);
                while (kv != null) {
                    final idDict dict = gameLocal.FindEntityDefDict(kv.GetValue().toString(), false);
                    if (dict != null) {
                        idDict d2 = new idDict();
                        d2.Copy(dict);
                        d2.Set("name", va("givenitem_%d", giveNum++));
                        idEntity[] ent = {null};
                        if (gameLocal.SpawnEntityDef(d2, ent) && ent[0] != null && ent[0].IsType(idItem.class)) {
                            idItem item = (idItem) ent[0];
                            item.GiveToPlayer(gameLocal.GetLocalPlayer());
                        }
                    }
                    kv = spawnArgs.MatchPrefix("item", kv);
                }
            }
        }
    };


    /*
     ===============================================================================

     idTarget_GiveEmail

     ===============================================================================
     */
    public static class idTarget_GiveEmail extends idTarget {
        // CLASS_PROTOTYPE( idTarget_GiveEmail );

        @Override
        public void Spawn() {
        }

        private void Event_Activate(idEntity activator) {
            idPlayer player = gameLocal.GetLocalPlayer();
            idDeclPDA pda = player.GetPDA();
            if (pda != null) {
                player.GiveEmail(spawnArgs.GetString("email"));
            } else {
                player.ShowTip(spawnArgs.GetString("text_infoTitle"), spawnArgs.GetString("text_PDANeeded"), true);
            }
        }
    };

    /*
     ===============================================================================

     idTarget_SetModel

     ===============================================================================
     */
    public static class idTarget_SetModel extends idTarget {
        // CLASS_PROTOTYPE( idTarget_SetModel );

        @Override
        public void Spawn() {
            idStr model;

            model = new idStr(spawnArgs.GetString("newmodel"));
            if (declManager.FindType(DECL_MODELDEF, model, false) == null) {
                // precache the render model
                renderModelManager.FindModel(model);
                // precache .cm files only
                CollisionModel_local.collisionModelManager.LoadModel(model, true);
            }
        }

        private void Event_Activate(idEntity activator) {
            for (int i = 0; i < targets.Num(); i++) {
                idEntity ent = targets.oGet(i).GetEntity();
                if (ent != null) {
                    ent.SetModel(spawnArgs.GetString("newmodel"));
                }
            }
        }
    };

    /*
     ===============================================================================

     idTarget_SetInfluence

     ===============================================================================
     */
    public static class idTarget_SetInfluence extends idTarget {
        // CLASS_PROTOTYPE( idTarget_SetInfluence );

        private idList<Integer>      lightList;
        private idList<Integer>      guiList;
        private idList<Integer>      soundList;
        private idList<Integer>      genericList;
        private float                flashIn;
        private float                flashOut;
        private float                delay;
        private idStr                flashInSound;
        private idStr                flashOutSound;
        private idEntity             switchToCamera;
        private idInterpolate<Float> fovSetting;
        private boolean              soundFaded;
        private boolean              restoreOnTrigger;
        //
        //

        public idTarget_SetInfluence() {
            flashIn = 0.0f;
            flashOut = 0.0f;
            delay = 0.0f;
            switchToCamera = null;
            soundFaded = false;
            restoreOnTrigger = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(lightList.Num());
            for (i = 0; i < lightList.Num(); i++) {
                savefile.WriteInt(lightList.oGet(i));
            }

            savefile.WriteInt(guiList.Num());
            for (i = 0; i < guiList.Num(); i++) {
                savefile.WriteInt(guiList.oGet(i));
            }

            savefile.WriteInt(soundList.Num());
            for (i = 0; i < soundList.Num(); i++) {
                savefile.WriteInt(soundList.oGet(i));
            }

            savefile.WriteInt(genericList.Num());
            for (i = 0; i < genericList.Num(); i++) {
                savefile.WriteInt(genericList.oGet(i));
            }

            savefile.WriteFloat(flashIn);
            savefile.WriteFloat(flashOut);

            savefile.WriteFloat(delay);

            savefile.WriteString(flashInSound);
            savefile.WriteString(flashOutSound);

            savefile.WriteObject(switchToCamera);

            savefile.WriteFloat(fovSetting.GetStartTime());
            savefile.WriteFloat(fovSetting.GetDuration());
            savefile.WriteFloat(fovSetting.GetStartValue());
            savefile.WriteFloat(fovSetting.GetEndValue());

            savefile.WriteBool(soundFaded);
            savefile.WriteBool(restoreOnTrigger);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int[] num = {0};
            int[] itemNum = new int[1];
            float[] set = new float[1];

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadInt(itemNum);
                lightList.Append(itemNum[0]);
            }

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadInt(itemNum);
                guiList.Append(itemNum[0]);
            }

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadInt(itemNum);
                soundList.Append(itemNum[0]);
            }

            savefile.ReadInt(num);
            for (i = 0; i < num[0]; i++) {
                savefile.ReadInt(itemNum);
                genericList.Append(itemNum[0]);
            }

            flashIn = savefile.ReadFloat();
            flashOut = savefile.ReadFloat();

            delay = savefile.ReadFloat();

            savefile.ReadString(flashInSound);
            savefile.ReadString(flashOutSound);

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/switchToCamera);

            savefile.ReadFloat(set);
            fovSetting.SetStartTime(set[0]);
            savefile.ReadFloat(set);
            fovSetting.SetDuration(set[0]);
            savefile.ReadFloat(set);
            fovSetting.SetStartValue(set[0]);
            savefile.ReadFloat(set);
            fovSetting.SetEndValue(set[0]);

            soundFaded = savefile.ReadBool();
            restoreOnTrigger = savefile.ReadBool();
        }

        @Override
        public void Spawn() {
            PostEventMS(EV_GatherEntities, 0);
            flashIn = spawnArgs.GetFloat("flashIn", "0");
            flashOut = spawnArgs.GetFloat("flashOut", "0");
            flashInSound.oSet(spawnArgs.GetString("snd_flashin"));
            flashOutSound.oSet(spawnArgs.GetString("snd_flashout"));
            delay = spawnArgs.GetFloat("delay");
            soundFaded = false;
            restoreOnTrigger = false;

            // always allow during cinematics
            cinematic = true;
        }

        private void Event_Activate(idEntity activator) {
            int i, j;
            idEntity ent;
            idLight light;
            idSound sound;
            idStaticEntity generic;
            String parm;
            String skin;
            boolean update;
            idVec3 color;
            idVec4 colorTo = new idVec4();
            idPlayer player;

            player = gameLocal.GetLocalPlayer();

            if (spawnArgs.GetBool("triggerActivate")) {
                if (restoreOnTrigger) {
                    ProcessEvent(EV_RestoreInfluence);
                    restoreOnTrigger = false;
                    return;
                }
                restoreOnTrigger = true;
            }

            float fadeTime = spawnArgs.GetFloat("fadeWorldSounds");

            if (delay > 0.0f) {
                PostEventSec(EV_Activate, delay, activator);
                delay = 0.0f;
                // start any sound fading now
                if (fadeTime != 0) {
                    gameSoundWorld.FadeSoundClasses(0, -40.0f, fadeTime);
                    soundFaded = true;
                }
                return;
            } else if (fadeTime != 0 && !soundFaded) {
                gameSoundWorld.FadeSoundClasses(0, -40.0f, fadeTime);
                soundFaded = true;
            }

            if (spawnArgs.GetBool("triggerTargets")) {
                ActivateTargets(activator);
            }

            if (flashIn != 0) {
                PostEventSec(EV_Flash, 0.0f, flashIn, 0);
            }

            parm = spawnArgs.GetString("snd_influence");
            if (isNotNullOrEmpty(parm)) {
                PostEventSec(EV_StartSoundShader, flashIn, parm, SND_CHANNEL_ANY);
            }

            if (switchToCamera != null) {
                switchToCamera.PostEventSec(EV_Activate, flashIn + 0.05f, this);
            }

            float fov = spawnArgs.GetInt("fov");
            if (fov != 0) {
                fovSetting.Init(gameLocal.time, (float) SEC2MS(spawnArgs.GetFloat("fovTime")), player.DefaultFov(), fov);
                BecomeActive(TH_THINK);
            }

            for (i = 0; i < genericList.Num(); i++) {
                ent = gameLocal.entities[genericList.oGet(i)];
                if (ent == null) {
                    continue;
                }
                generic = (idStaticEntity) ent;
                color = generic.spawnArgs.GetVector("color_demonic");
                colorTo.Set(color.x, color.y, color.z, 1.0f);
                generic.Fade(colorTo, spawnArgs.GetFloat("fade_time", "0.25"));
            }

            for (i = 0; i < lightList.Num(); i++) {
                ent = gameLocal.entities[lightList.oGet(i)];
                if (ent == null || !ent.IsType(idLight.class)) {
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
                light.Fade(colorTo, spawnArgs.GetFloat("fade_time", "0.25"));
            }

            for (i = 0; i < soundList.Num(); i++) {
                ent = gameLocal.entities[soundList.oGet(i)];
                if (ent == null || !ent.IsType(idSound.class)) {
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

            for (i = 0; i < guiList.Num(); i++) {
                ent = gameLocal.entities[guiList.oGet(i)];
                if (ent == null || ent.GetRenderEntity() == null) {
                    continue;
                }
                update = false;
                for (j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                    if (ent.GetRenderEntity().gui[j] != null
                            && ent.spawnArgs.FindKey(j == 0 ? "gui_demonic" : va("gui_demonic%d", j + 1)) != null) {
                        ent.GetRenderEntity().gui[j] = uiManager.FindGui(ent.spawnArgs.GetString(j == 0 ? "gui_demonic" : va("gui_demonic%d", j + 1)), true);
                        update = true;
                    }
                }

                if (update) {
                    ent.UpdateVisuals();
                    ent.Present();
                }
            }

            player.SetInfluenceLevel(spawnArgs.GetInt("influenceLevel"));

            int snapAngle = spawnArgs.GetInt("snapAngle");
            if (snapAngle != 0) {
                idAngles ang = new idAngles(0, snapAngle, 0);
                player.SetViewAngles(ang);
                player.SetAngles(ang);
            }

            if (spawnArgs.GetBool("effect_vision")) {
                parm = spawnArgs.GetString("mtrVision");
                skin = spawnArgs.GetString("skinVision");
                player.SetInfluenceView(parm, skin, spawnArgs.GetInt("visionRadius"), this);
            }

            parm = spawnArgs.GetString("mtrWorld");
            if (isNotNullOrEmpty(parm)) {
                gameLocal.SetGlobalMaterial(declManager.FindMaterial(parm));
            }

            if (!restoreOnTrigger) {
                PostEventMS(EV_RestoreInfluence, (int) SEC2MS(spawnArgs.GetFloat("time")));
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
            idVec4 colorTo = new idVec4();

            if (flashOut != 0) {
                PostEventSec(EV_Flash, 0.0f, flashOut, 1);
            }

            if (switchToCamera != null) {
                switchToCamera.PostEventMS(EV_Activate, 0.0f, this);
            }

            for (i = 0; i < genericList.Num(); i++) {
                ent = gameLocal.entities[genericList.oGet(i)];
                if (ent == null) {
                    continue;
                }
                generic = (idStaticEntity) ent;
                colorTo.Set(1.0f, 1.0f, 1.0f, 1.0f);
                generic.Fade(colorTo, spawnArgs.GetFloat("fade_time", "0.25"));
            }

            for (i = 0; i < lightList.Num(); i++) {
                ent = gameLocal.entities[lightList.oGet(i)];
                if (ent == null || !ent.IsType(idLight.class)) {
                    continue;
                }
                light = (idLight) ent;
                if (!light.spawnArgs.GetBool("leave_demonic_mat")) {
                    final String texture = light.spawnArgs.GetString("texture", "lights/squarelight1");
                    light.SetShader(texture);
                }
                color = light.spawnArgs.GetVector("_color");
                colorTo.Set(color.x, color.y, color.z, 1.0f);
                light.Fade(colorTo, spawnArgs.GetFloat("fade_time", "0.25"));
            }

            for (i = 0; i < soundList.Num(); i++) {
                ent = gameLocal.entities[soundList.oGet(i)];
                if (ent == null || !ent.IsType(idSound.class)) {
                    continue;
                }
                sound = (idSound) ent;
                sound.StopSound(etoi(SND_CHANNEL_ANY), false);
                sound.SetSound(sound.spawnArgs.GetString("s_shader"));
            }

            for (i = 0; i < guiList.Num(); i++) {
                ent = gameLocal.entities[guiList.oGet(i)];
                if (ent == null || GetRenderEntity() == null) {
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

            idPlayer player = gameLocal.GetLocalPlayer();
            player.SetInfluenceLevel(0);
            player.SetInfluenceView(null, null, 0.0f, null);
            player.SetInfluenceFov(0);
            gameLocal.SetGlobalMaterial(null);
            float fadeTime = spawnArgs.GetFloat("fadeWorldSounds");
            if (fadeTime != 0) {
                gameSoundWorld.FadeSoundClasses(0, 0.0f, fadeTime / 2.0f);
            }

        }

        private void Event_GatherEntities() {
            int i, listedEntities;
            idEntity[] entityList = new idEntity[MAX_GENTITIES];

            boolean demonicOnly = spawnArgs.GetBool("effect_demonic");
            boolean lights = spawnArgs.GetBool("effect_lights");
            boolean sounds = spawnArgs.GetBool("effect_sounds");
            boolean guis = spawnArgs.GetBool("effect_guis");
            boolean models = spawnArgs.GetBool("effect_models");
            boolean vision = spawnArgs.GetBool("effect_vision");
            boolean targetsOnly = spawnArgs.GetBool("targetsOnly");

            lightList.Clear();
            guiList.Clear();
            soundList.Clear();

            if (spawnArgs.GetBool("effect_all")) {
                lights = sounds = guis = models = vision = true;
            }

            if (targetsOnly) {
                listedEntities = targets.Num();
                for (i = 0; i < listedEntities; i++) {
                    entityList[i] = targets.oGet(i).GetEntity();
                }
            } else {
                float radius = spawnArgs.GetFloat("radius");
                listedEntities = gameLocal.EntitiesWithinRadius(GetPhysics().GetOrigin(), radius, entityList, MAX_GENTITIES);
            }

            for (i = 0; i < listedEntities; i++) {
                idEntity ent = entityList[i];
                if (ent != null) {
                    if (lights && ent.IsType(idLight.class) && ent.spawnArgs.FindKey("color_demonic") != null) {
                        lightList.Append(ent.entityNumber);
                        continue;
                    }
                    if (sounds && ent.IsType(idSound.class) && ent.spawnArgs.FindKey("snd_demonic") != null) {
                        soundList.Append(ent.entityNumber);
                        continue;
                    }
                    if (guis && ent.GetRenderEntity() != null && ent.GetRenderEntity().gui[0] != null && ent.spawnArgs.FindKey("gui_demonic") != null) {
                        guiList.Append(ent.entityNumber);
                        continue;
                    }
                    if (ent.IsType(idStaticEntity.class) && ent.spawnArgs.FindKey("color_demonic") != null) {
                        genericList.Append(ent.entityNumber);
//                        continue;
                    }
                }
            }
            String temp;
            temp = spawnArgs.GetString("switchToView");
            switchToCamera = (temp.length() != 0) ? gameLocal.FindEntity(temp) : null;

        }

        private void Event_Flash(float flash, int out) {
            idPlayer player = gameLocal.GetLocalPlayer();
            player.playerView.Fade(new idVec4(1, 1, 1, 1), (int) flash);
            idSoundShader shader;
            if (0 == out && flashInSound.Length() != 0) {
                shader = declManager.FindSound(flashInSound);
                player.StartSoundShader(shader, SND_CHANNEL_VOICE, 0, false, null);
            } else if (out != 0 && (flashOutSound.Length() != 0 || flashInSound.Length() != 0)) {
                shader = declManager.FindSound(flashOutSound.Length() != 0 ? flashOutSound : flashInSound);
                player.StartSoundShader(shader, SND_CHANNEL_VOICE, 0, false, null);
            }
            PostEventSec(EV_ClearFlash, flash, flash);
        }

        private void Event_ClearFlash(float flash) {
            idPlayer player = gameLocal.GetLocalPlayer();
            player.playerView.Fade(vec4_zero, (int) flash);
        }

        @Override
        public void Think() {
            if ((thinkFlags & TH_THINK) != 0) {
                idPlayer player = gameLocal.GetLocalPlayer();
                player.SetInfluenceFov(fovSetting.GetCurrentValue(gameLocal.time));
                if (fovSetting.IsDone(gameLocal.time)) {
                    if (!spawnArgs.GetBool("leaveFOV")) {
                        player.SetInfluenceFov(0);
                    }
                    BecomeInactive(TH_THINK);
                }
            } else {
                BecomeInactive(TH_ALL);
            }
        }
    }

    ;


    /*
     ===============================================================================

     idTarget_SetKeyVal

     ===============================================================================
     */
    public static class idTarget_SetKeyVal extends idTarget {
        // CLASS_PROTOTYPE( idTarget_SetKeyVal );

        private void Event_Activate(idEntity activator) {
            int i;
            String key, val;
            idEntity ent;
            idKeyValue kv;
            int n;

            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null) {
                    kv = spawnArgs.MatchPrefix("keyval");
                    while (kv != null) {
                        n = kv.GetValue().Find(";");
                        if (n > 0) {
                            key = kv.GetValue().Left(n).toString();
                            val = kv.GetValue().Right(kv.GetValue().Length() - n - 1).toString();
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
                        kv = spawnArgs.MatchPrefix("keyval", kv);
                    }
                    ent.UpdateChangeableSpawnArgs(null);
                    ent.UpdateVisuals();
                    ent.Present();
                }
            }
        }
    }

    ;


    /*
     ===============================================================================

     idTarget_SetFov

     ===============================================================================
     */
    public static class idTarget_SetFov extends idTarget {
        // CLASS_PROTOTYPE( idTarget_SetFov );

        private idInterpolate<Integer> fovSetting;
        //
        //

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteFloat(fovSetting.GetStartTime());
            savefile.WriteFloat(fovSetting.GetDuration());
            savefile.WriteFloat(fovSetting.GetStartValue());
            savefile.WriteFloat(fovSetting.GetEndValue());
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            float[] setting = new float[1];

            savefile.ReadFloat(setting);
            fovSetting.SetStartTime(setting[0]);
            savefile.ReadFloat(setting);
            fovSetting.SetDuration(setting[0]);
            savefile.ReadFloat(setting);
            fovSetting.SetStartValue((int) setting[0]);
            savefile.ReadFloat(setting);
            fovSetting.SetEndValue((int) setting[0]);

            fovSetting.GetCurrentValue(gameLocal.time);
        }

        @Override
        public void Think() {
            if ((thinkFlags & TH_THINK) != 0) {
                idPlayer player = gameLocal.GetLocalPlayer();
                player.SetInfluenceFov(fovSetting.GetCurrentValue(gameLocal.time));
                if (fovSetting.IsDone(gameLocal.time)) {
                    player.SetInfluenceFov(0.0f);
                    BecomeInactive(TH_THINK);
                }
            } else {
                BecomeInactive(TH_ALL);
            }
        }

        private void Event_Activate(idEntity activator) {
            // always allow during cinematics
            cinematic = true;

            idPlayer player = gameLocal.GetLocalPlayer();
            fovSetting.Init(gameLocal.time, (float) SEC2MS(spawnArgs.GetFloat("time")), (int) (player != null ? player.DefaultFov() : g_fov.GetFloat()), (int) spawnArgs.GetFloat("fov"));
            BecomeActive(TH_THINK);
        }
    }

    ;


    /*
     ===============================================================================

     idTarget_SetPrimaryObjective

     ===============================================================================
     */
    public static class idTarget_SetPrimaryObjective extends idTarget {
// public:
        // CLASS_PROTOTYPE( idTarget_SetPrimaryObjective );

        private void Event_Activate(idEntity activator) {
            idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null && player.objectiveSystem != null) {
                player.objectiveSystem.SetStateString("missionobjective", spawnArgs.GetString("text", common.GetLanguageDict().GetString("#str_04253")));
            }
        }
    }

    ;

    /*
     ===============================================================================

     idTarget_LockDoor

     ===============================================================================
     */
    public static class idTarget_LockDoor extends idTarget {
// public:
        // CLASS_PROTOTYPE( idTarget_LockDoor );

        private void Event_Activate(idEntity activator) {
            int i;
            idEntity ent;
            int lock;

            lock = spawnArgs.GetInt("locked", "1");
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null && ent.IsType(idDoor.class)) {
                    if (((idDoor) ent).IsLocked() != 0) {
                        ((idDoor) ent).Lock(0);
                    } else {
                        ((idDoor) ent).Lock(lock);
                    }
                }
            }
        }
    };

    /*
     ===============================================================================

     idTarget_CallObjectFunction

     ===============================================================================
     */
    public static class idTarget_CallObjectFunction extends idTarget {
// public:
        // CLASS_PROTOTYPE( idTarget_CallObjectFunction );

        private void Event_Activate(idEntity activator) {
            int i;
            idEntity ent;
            function_t func;
            String funcName;
            idThread thread;

            funcName = spawnArgs.GetString("call");
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null && ent.scriptObject.HasObject()) {
                    func = ent.scriptObject.GetFunction(funcName);
                    if (NOT(func)) {
                        gameLocal.Error("Function '%s' not found on entity '%s' for function call from '%s'", funcName, ent.name, name);
                    }
                    if (func.type.NumParameters() != 1) {
                        gameLocal.Error("Function '%s' on entity '%s' has the wrong number of parameters for function call from '%s'", funcName, ent.name, name);
                    }
                    if (!ent.scriptObject.GetTypeDef().Inherits(func.type.GetParmType(0))) {
                        gameLocal.Error("Function '%s' on entity '%s' is the wrong type for function call from '%s'", funcName, ent.name, name);
                    }
                    // create a thread and call the function
                    thread = new idThread();
                    thread.CallFunction(ent, func, true);
                    thread.Start();
                }
            }
        }
    };


    /*
     ===============================================================================

     idTarget_LockDoor

     ===============================================================================
     */
    public static class idTarget_EnableLevelWeapons extends idTarget {
        // CLASS_PROTOTYPE( idTarget_EnableLevelWeapons );

        private void Event_Activate(idEntity activator) {
            int i;
            String weap;

            gameLocal.world.spawnArgs.SetBool("no_Weapons", spawnArgs.GetBool("disable"));

            if (spawnArgs.GetBool("disable")) {
                for (i = 0; i < gameLocal.numClients; i++) {
                    if (gameLocal.entities[ i] != null) {
                        gameLocal.entities[ i].ProcessEvent(EV_Player_DisableWeapon);
                    }
                }
            } else {
                weap = spawnArgs.GetString("weapon");
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
    };
    /*
     ===============================================================================

     idTarget_Tip

     ===============================================================================
     */

    public static class idTarget_Tip extends idTarget {
        // CLASS_PROTOTYPE( idTarget_Tip );

        public idTarget_Tip() {
            playerPos.Zero();
        }

        @Override
        public void Spawn() {
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteVec3(playerPos);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadVec3(playerPos);
        }
//
//
        private idVec3 playerPos;
//
//

        private void Event_Activate(idEntity activator) {
            idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                if (player.IsTipVisible()) {
                    PostEventSec(EV_Activate, 5.1f, activator);
                    return;
                }
                player.ShowTip(spawnArgs.GetString("text_title"), spawnArgs.GetString("text_tip"), false);
                PostEventMS(EV_GetPlayerPos, 2000);
            }
        }

        private void Event_TipOff() {
            idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                idVec3 v = player.GetPhysics().GetOrigin().oMinus(playerPos);
                if (v.Length() > 96.0f) {
                    player.HideTip();
                } else {
                    PostEventMS(EV_TipOff, 100);
                }
            }
        }

        private void Event_GetPlayerPos() {
            idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                playerPos = player.GetPhysics().GetOrigin();
                PostEventMS(EV_TipOff, 100);
            }
        }
    };

    /*
     ===============================================================================

     idTarget_GiveSecurity

     ===============================================================================
     */
    public static class idTarget_GiveSecurity extends idTarget {
        // CLASS_PROTOTYPE( idTarget_GiveSecurity );

        private void Event_Activate(idEntity activator) {
            idPlayer player = gameLocal.GetLocalPlayer();
            if (player != null) {
                player.GiveSecurity(spawnArgs.GetString("text_security"));
            }
        }
    };


    /*
     ===============================================================================

     idTarget_RemoveWeapons

     ===============================================================================
     */
    public static class idTarget_RemoveWeapons extends idTarget {
        // CLASS_PROTOTYPE( idTarget_RemoveWeapons );

        private void Event_Activate(idEntity activator) {
            for (int i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.entities[ i] != null) {
                    idPlayer player = (idPlayer) gameLocal.entities[i];
                    idKeyValue kv = spawnArgs.MatchPrefix("weapon", null);
                    while (kv != null) {
                        player.RemoveWeapon(kv.GetValue().toString());
                        kv = spawnArgs.MatchPrefix("weapon", kv);
                    }
                    player.SelectWeapon(player.weapon_fists, true);
                }
            }
        }
    };


    /*
     ===============================================================================

     idTarget_LevelTrigger

     ===============================================================================
     */
    public static class idTarget_LevelTrigger extends idTarget {
        // CLASS_PROTOTYPE( idTarget_LevelTrigger );//TODO:understand this fucking macro

        private void Event_Activate(idEntity activator) {
            for (int i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.entities[ i] != null) {
                    idPlayer player = (idPlayer) gameLocal.entities[i];
                    player.SetLevelTrigger(spawnArgs.GetString("levelName"), spawnArgs.GetString("triggerName"));
                }
            }
        }
    };

    /*
     ===============================================================================

     idTarget_EnableStamina

     ===============================================================================
     */
    public static class idTarget_EnableStamina extends idTarget {
        // CLASS_PROTOTYPE( idTarget_EnableStamina );

        private void Event_Activate(idEntity activator) {
            for (int i = 0; i < gameLocal.numClients; i++) {
                if (gameLocal.entities[ i] != null) {
                    idPlayer player = (idPlayer) gameLocal.entities[i];
                    if (spawnArgs.GetBool("enable")) {
                        pm_stamina.SetFloat(player.spawnArgs.GetFloat("pm_stamina"));
                    } else {
                        pm_stamina.SetFloat(0.0f);
                    }
                }
            }
        }
    };

    /*
     ===============================================================================

     idTarget_FadeSoundClass

     ===============================================================================
     */
    public static class idTarget_FadeSoundClass extends idTarget {
        // CLASS_PROTOTYPE( idTarget_FadeSoundClass );

        private void Event_Activate(idEntity activator) {
            float fadeTime = spawnArgs.GetFloat("fadeTime");
            float fadeDB = spawnArgs.GetFloat("fadeDB");
            float fadeDuration = spawnArgs.GetFloat("fadeDuration");
            int fadeClass = spawnArgs.GetInt("fadeClass");
            // start any sound fading now
            if (fadeTime != 0) {
                gameSoundWorld.FadeSoundClasses(fadeClass, spawnArgs.GetBool("fadeIn") ? fadeDB : /*0.0f */ -fadeDB, fadeTime);
                if (fadeDuration != 0) {
                    PostEventSec(EV_RestoreVolume, fadeDuration);
                }
            }
        }

        private void Event_RestoreVolume() {
            float fadeTime = spawnArgs.GetFloat("fadeTime");
            float fadeDB = spawnArgs.GetFloat("fadeDB");
            int fadeClass = spawnArgs.GetInt("fadeClass");
            // restore volume
            gameSoundWorld.FadeSoundClasses(0, fadeDB, fadeTime);
        }
    };
}
