package neo.Game;

import static java.lang.Math.atan2;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static neo.CM.CollisionModel.CM_BOX_EPSILON;
import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import static neo.Game.AFEntity.EV_Gibbed;
import static neo.Game.AI.AI.talkState_t.TALK_OK;
import static neo.Game.Animation.Anim.ANIMCHANNEL_LEGS;
import static neo.Game.Animation.Anim.ANIMCHANNEL_TORSO;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_ActivateTargets;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.signalNum_t.SIG_TOUCH;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.g_armorProtection;
import static neo.Game.GameSys.SysCvar.g_armorProtectionMP;
import static neo.Game.GameSys.SysCvar.g_balanceTDM;
import static neo.Game.GameSys.SysCvar.g_damageScale;
import static neo.Game.GameSys.SysCvar.g_debugDamage;
import static neo.Game.GameSys.SysCvar.g_debugMove;
import static neo.Game.GameSys.SysCvar.g_dragEntity;
import static neo.Game.GameSys.SysCvar.g_editEntityMode;
import static neo.Game.GameSys.SysCvar.g_fov;
import static neo.Game.GameSys.SysCvar.g_gun_x;
import static neo.Game.GameSys.SysCvar.g_gun_y;
import static neo.Game.GameSys.SysCvar.g_gun_z;
import static neo.Game.GameSys.SysCvar.g_healthTakeAmt;
import static neo.Game.GameSys.SysCvar.g_healthTakeLimit;
import static neo.Game.GameSys.SysCvar.g_healthTakeTime;
import static neo.Game.GameSys.SysCvar.g_knockback;
import static neo.Game.GameSys.SysCvar.g_mpWeaponAngleScale;
import static neo.Game.GameSys.SysCvar.g_showEnemies;
import static neo.Game.GameSys.SysCvar.g_showHud;
import static neo.Game.GameSys.SysCvar.g_showPlayerShadow;
import static neo.Game.GameSys.SysCvar.g_showProjectilePct;
import static neo.Game.GameSys.SysCvar.g_showviewpos;
import static neo.Game.GameSys.SysCvar.g_skill;
import static neo.Game.GameSys.SysCvar.g_stopTime;
import static neo.Game.GameSys.SysCvar.g_testDeath;
import static neo.Game.GameSys.SysCvar.g_useDynamicProtection;
import static neo.Game.GameSys.SysCvar.g_viewNodalX;
import static neo.Game.GameSys.SysCvar.g_viewNodalZ;
import static neo.Game.GameSys.SysCvar.net_clientPredictGUI;
import static neo.Game.GameSys.SysCvar.pm_airTics;
import static neo.Game.GameSys.SysCvar.pm_bboxwidth;
import static neo.Game.GameSys.SysCvar.pm_bobpitch;
import static neo.Game.GameSys.SysCvar.pm_bobroll;
import static neo.Game.GameSys.SysCvar.pm_bobup;
import static neo.Game.GameSys.SysCvar.pm_crouchbob;
import static neo.Game.GameSys.SysCvar.pm_crouchrate;
import static neo.Game.GameSys.SysCvar.pm_crouchspeed;
import static neo.Game.GameSys.SysCvar.pm_crouchviewheight;
import static neo.Game.GameSys.SysCvar.pm_deadviewheight;
import static neo.Game.GameSys.SysCvar.pm_jumpheight;
import static neo.Game.GameSys.SysCvar.pm_maxviewpitch;
import static neo.Game.GameSys.SysCvar.pm_minviewpitch;
import static neo.Game.GameSys.SysCvar.pm_modelView;
import static neo.Game.GameSys.SysCvar.pm_noclipspeed;
import static neo.Game.GameSys.SysCvar.pm_normalheight;
import static neo.Game.GameSys.SysCvar.pm_normalviewheight;
import static neo.Game.GameSys.SysCvar.pm_runbob;
import static neo.Game.GameSys.SysCvar.pm_runpitch;
import static neo.Game.GameSys.SysCvar.pm_runroll;
import static neo.Game.GameSys.SysCvar.pm_runspeed;
import static neo.Game.GameSys.SysCvar.pm_spectatebbox;
import static neo.Game.GameSys.SysCvar.pm_spectatespeed;
import static neo.Game.GameSys.SysCvar.pm_stamina;
import static neo.Game.GameSys.SysCvar.pm_staminarate;
import static neo.Game.GameSys.SysCvar.pm_staminathreshold;
import static neo.Game.GameSys.SysCvar.pm_stepsize;
import static neo.Game.GameSys.SysCvar.pm_thirdPerson;
import static neo.Game.GameSys.SysCvar.pm_thirdPersonAngle;
import static neo.Game.GameSys.SysCvar.pm_thirdPersonClip;
import static neo.Game.GameSys.SysCvar.pm_thirdPersonDeath;
import static neo.Game.GameSys.SysCvar.pm_thirdPersonHeight;
import static neo.Game.GameSys.SysCvar.pm_thirdPersonRange;
import static neo.Game.GameSys.SysCvar.pm_usecylinder;
import static neo.Game.GameSys.SysCvar.pm_walkbob;
import static neo.Game.GameSys.SysCvar.pm_walkspeed;
import static neo.Game.Game_local.MASK_DEADSOLID;
import static neo.Game.Game_local.MASK_PLAYERSOLID;
import static neo.Game.Game_local.MASK_SHOT_BOUNDINGBOX;
import static neo.Game.Game_local.MASK_SHOT_RENDERMODEL;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_CLIENTS;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY2;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY3;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_DEMONIC;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_HEART;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ITEM;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_PDA;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_VOICE;
import static neo.Game.Game_local.gameState_t.GAMESTATE_STARTUP;
import static neo.Game.Game_network.net_clientSelfSmoothing;
import static neo.Game.MultiplayerGame.gameType_t.GAME_DM;
import static neo.Game.MultiplayerGame.gameType_t.GAME_TDM;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.SUDDENDEATH;
import static neo.Game.MultiplayerGame.idMultiplayerGame.gameState_t.WARMUP;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_DEAD;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_FREEZE;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_NOCLIP;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_NORMAL;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_SPECTATOR;
import static neo.Game.Physics.Physics_Player.waterLevel_t.WATERLEVEL_FEET;
import static neo.Game.Physics.Physics_Player.waterLevel_t.WATERLEVEL_HEAD;
import static neo.Game.Physics.Physics_Player.waterLevel_t.WATERLEVEL_WAIST;
import static neo.Game.Sound.SSF_GLOBAL;
import static neo.Game.Sound.SSF_PRIVATE_SOUND;
import static neo.Game.Weapon.AMMO_NUMTYPES;
import static neo.Game.Weapon.LIGHTID_VIEW_MUZZLE_FLASH;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_CORPSE;
import static neo.Renderer.Material.CONTENTS_MONSTERCLIP;
import static neo.Renderer.Material.SURF_NODAMAGE;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIME_OF_DEATH;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_AIR;
import static neo.TempDump.NOT;
import static neo.TempDump.atoi;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itob;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_WALK;
import static neo.framework.Async.NetworkSystem.networkSystem;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.Common.STRTABLE_ID;
import static neo.framework.Common.STRTABLE_ID_LENGTH;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_AUDIO;
import static neo.framework.DeclManager.declType_t.DECL_EMAIL;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;
import static neo.framework.DeclManager.declType_t.DECL_PDA;
import static neo.framework.DeclManager.declType_t.DECL_VIDEO;
import static neo.framework.UsercmdGen.BUTTON_ATTACK;
import static neo.framework.UsercmdGen.BUTTON_MLOOK;
import static neo.framework.UsercmdGen.BUTTON_RUN;
import static neo.framework.UsercmdGen.BUTTON_SCORES;
import static neo.framework.UsercmdGen.BUTTON_ZOOM;
import static neo.framework.UsercmdGen.IMPULSE_0;
import static neo.framework.UsercmdGen.IMPULSE_12;
import static neo.framework.UsercmdGen.IMPULSE_13;
import static neo.framework.UsercmdGen.IMPULSE_14;
import static neo.framework.UsercmdGen.IMPULSE_15;
import static neo.framework.UsercmdGen.IMPULSE_17;
import static neo.framework.UsercmdGen.IMPULSE_18;
import static neo.framework.UsercmdGen.IMPULSE_19;
import static neo.framework.UsercmdGen.IMPULSE_20;
import static neo.framework.UsercmdGen.IMPULSE_22;
import static neo.framework.UsercmdGen.IMPULSE_28;
import static neo.framework.UsercmdGen.IMPULSE_29;
import static neo.framework.UsercmdGen.IMPULSE_40;
import static neo.framework.UsercmdGen.UCF_IMPULSE_SEQUENCE;
import static neo.idlib.Lib.colorBlack;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.idLib.sys;
import static neo.idlib.Text.Str.S_COLOR_GRAY;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Angles.getAng_zero;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Math_h.SHORT2ANGLE;
import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.RAD2DEG;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import static neo.ui.UserInterface.uiManager;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import neo.CM.CollisionModel.contactInfo_t;
import neo.CM.CollisionModel.trace_s;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.AFEntity.idAFEntity_Vehicle;
import neo.Game.Actor.idActor;
import neo.Game.Camera.idCamera;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.GameEdit.idDragEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Item.idItem;
import neo.Game.Misc.idLocationEntity;
import neo.Game.PlayerIcon.idPlayerIcon;
import neo.Game.PlayerView.idPlayerView;
import neo.Game.Projectile.idProjectile;
import neo.Game.Weapon.idWeapon;
import neo.Game.AI.AAS.idAAS;
import neo.Game.AI.AI.idAI;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Player.idPhysics_Player;
import neo.Game.Physics.Physics_Player.waterLevel_t;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Program.idScriptBool;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.shaderStage_t;
import neo.Renderer.RenderWorld.guiPoint_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.snd_shader.soundShaderParms_t;
import neo.framework.BuildDefines;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import neo.framework.DeclManager.declType_t;
import neo.framework.DeclPDA.idDeclAudio;
import neo.framework.DeclPDA.idDeclEmail;
import neo.framework.DeclPDA.idDeclPDA;
import neo.framework.DeclPDA.idDeclVideo;
import neo.framework.DeclSkin.idDeclSkin;
import neo.framework.UsercmdGen.usercmd_t;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Interpolate.idInterpolate;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;
import neo.sys.sys_public.sysEvent_s;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class Player {

    /*
     ===============================================================================

     Player control of the Doom Marine.
     This object handles all player movement and world interaction.

     ===============================================================================
     */
    public static final  idEventDef EV_Player_GetButtons        = new idEventDef("getButtons", null, 'd');
    public static final  idEventDef EV_Player_GetMove           = new idEventDef("getMove", null, 'v');
    public static final  idEventDef EV_Player_GetViewAngles     = new idEventDef("getViewAngles", null, 'v');
    public static final  idEventDef EV_Player_StopFxFov         = new idEventDef("stopFxFov");
    public static final  idEventDef EV_Player_EnableWeapon      = new idEventDef("enableWeapon");
    public static final  idEventDef EV_Player_DisableWeapon     = new idEventDef("disableWeapon");
    public static final  idEventDef EV_Player_GetCurrentWeapon  = new idEventDef("getCurrentWeapon", null, 's');
    public static final  idEventDef EV_Player_GetPreviousWeapon = new idEventDef("getPreviousWeapon", null, 's');
    public static final  idEventDef EV_Player_SelectWeapon      = new idEventDef("selectWeapon", "s");
    public static final  idEventDef EV_Player_GetWeaponEntity   = new idEventDef("getWeaponEntity", null, 'e');
    public static final  idEventDef EV_Player_OpenPDA           = new idEventDef("openPDA");
    public static final  idEventDef EV_Player_InPDA             = new idEventDef("inPDA", null, 'd');
    public static final  idEventDef EV_Player_ExitTeleporter    = new idEventDef("exitTeleporter");
    public static final  idEventDef EV_Player_StopAudioLog      = new idEventDef("stopAudioLog");
    public static final  idEventDef EV_Player_HideTip           = new idEventDef("hideTip");
    public static final  idEventDef EV_Player_LevelTrigger      = new idEventDef("levelTrigger");
    public static final  idEventDef EV_SpectatorTouch           = new idEventDef("spectatorTouch", "et");
    public static final  idEventDef EV_Player_GetIdealWeapon    = new idEventDef("getIdealWeapon", null, 's');
    //
    public static final  float      THIRD_PERSON_FOCUS_DISTANCE = 512.0f;
    public static final  int        LAND_DEFLECT_TIME           = 150;
    public static final  int        LAND_RETURN_TIME            = 300;
    public static final  int        FOCUS_TIME                  = 300;
    public static final  int        FOCUS_GUI_TIME              = 500;
    //
    public static final  int        MAX_WEAPONS                 = 16;
    //
    public static final  int        DEAD_HEARTRATE              = 0;        // fall to as you die
    public static final  int        LOWHEALTH_HEARTRATE_ADJ     = 20;       //
    public static final  int        DYING_HEARTRATE             = 30;       // used for volumen calc when dying/dead
    public static final  int        BASE_HEARTRATE              = 70;       // default
    public static final  int        ZEROSTAMINA_HEARTRATE       = 115;      // no stamina
    public static final  int        MAX_HEARTRATE               = 130;      // maximum
    public static final  int        ZERO_VOLUME                 = -40;      // volume at zero
    public static final  int        DMG_VOLUME                  = 5;        // volume when taking damage
    public static final  int        DEATH_VOLUME                = 15;       // volume at death
    //
    public static final  int        SAVING_THROW_TIME           = 5000;     // maximum one "saving throw" every five seconds
    //
    public static final  int        ASYNC_PLAYER_INV_AMMO_BITS  = idMath.BitsForInteger(999);    // 9 bits to cover the range [0, 999]
    public static final  int        ASYNC_PLAYER_INV_CLIP_BITS  = -7;       // -7 bits to cover the range [-1, 60]
    //
    // distance between ladder rungs (actually is half that distance, but this sounds better)
    public static final  int        LADDER_RUNG_DISTANCE        = 32;
    //
    // amount of health per dose from the health station
    public static final  int        HEALTH_PER_DOSE             = 10;
    //
    // time before a weapon dropped to the floor disappears
    public static final  int        WEAPON_DROP_TIME            = 20 * 1000;
    //
    // time before a next or prev weapon switch happens
    public static final  int        WEAPON_SWITCH_DELAY         = 150;
    //
    // how many units to raise spectator above default view height so it's in the head of someone
    public static final  int        SPECTATE_RAISE              = 25;
    //
    public static final  int        HEALTHPULSE_TIME            = 333;
    //
    // minimum speed to bob and play run/walk animations at
    public static final  float      MIN_BOB_SPEED               = 5.0f;
    //
//
//    
    public static final  int        MAX_RESPAWN_TIME            = 10000;
    public static final  int        RAGDOLL_DEATH_TIME          = 3000;
    public static final  int        MAX_PDAS                    = 64;
    public static final  int        MAX_PDA_ITEMS               = 128;
    public static final  int        STEPUP_TIME                 = 200;
    public static final  int        MAX_INVENTORY_ITEMS         = 20;
//    

    public static class idItemInfo {

        idStr name = new idStr();
        idStr icon = new idStr();
    }

    public static class idObjectiveInfo {

        idStr title = new idStr();
        idStr text = new idStr();
        idStr screenshot = new idStr();
    }

    public static class idLevelTriggerInfo {

        idStr levelName = new idStr();
        idStr triggerName = new idStr();
    }

    ;
    //
// powerups - the "type" in item .def must match
// enum {
    public static final int BERSERK           = 0;
    public static final int INVISIBILITY      = 1;
    public static final int MEGAHEALTH        = 2;
    public static final int ADRENALINE        = 3;
    public static final int MAX_POWERUPS      = 4;
    // };
//
// powerup modifiers
// enum {
    public static final int SPEED             = 0;
    public static final int PROJECTILE_DAMAGE = 1;
    public static final int MELEE_DAMAGE      = 2;
    public static final int MELEE_DISTANCE    = 3;
    // };
//
// influence levels
// enum {
    public static final int INFLUENCE_NONE    = 0;            // none
    public static final int INFLUENCE_LEVEL1  = 1;            // no gun or hud
    public static final int INFLUENCE_LEVEL2  = 2;            // no gun, hud, movement
    public static final int INFLUENCE_LEVEL3  = 3;            // slow player movement
// };

    public static class idInventory {

        public int maxHealth;
        public int weapons;
        public int powerups;
        public int armor;
        public int maxarmor;
        public final int[] ammo           = new int[AMMO_NUMTYPES];
        public final int[] clip           = new int[MAX_WEAPONS];
        public final int[] powerupEndTime = new int[MAX_POWERUPS];
        //
        // mp
        public int   ammoPredictTime;
        //
        public int   deplete_armor;
        public float deplete_rate;
        public int   deplete_ammount;
        public int   nextArmorDepleteTime;
        //
        public final int[] pdasViewed = new int[4]; // 128 bit flags for indicating if a pda has been viewed
        //
        public int                        selPDA;
        public int                        selEMail;
        public int                        selVideo;
        public int                        selAudio;
        public boolean                    pdaOpened;
        public boolean                    turkeyScore;
        public idList<idDict>             items;
        public idStrList                  pdas;
        public idStrList                  pdaSecurity;
        public idStrList                  videos;
        public idStrList                  emails;
        //
        public boolean                    ammoPulse;
        public boolean                    weaponPulse;
        public boolean                    armorPulse;
        public int                        lastGiveTime;
        //
        public idList<idLevelTriggerInfo> levelTriggers;
        //
        public int nextItemPickup;
        public int nextItemNum;
        public int onePickupTime;
        public idList<idItemInfo>      pickupItemNames = new idList<>(idItemInfo.class);
        public idList<idObjectiveInfo> objectiveNames  = new idList<>();

        public idInventory() {
            items = new idList<>();
            pdas = new idStrList();
            pdaSecurity = new idStrList();
            videos = new idStrList();
            emails = new idStrList();
            levelTriggers = new idList<>();
            Clear();
        }
        // ~idInventory() { Clear(); }
        // save games

        // archives object for save game file
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(maxHealth);
            savefile.WriteInt(weapons);
            savefile.WriteInt(powerups);
            savefile.WriteInt(armor);
            savefile.WriteInt(maxarmor);
            savefile.WriteInt(ammoPredictTime);
            savefile.WriteInt(deplete_armor);
            savefile.WriteFloat(deplete_rate);
            savefile.WriteInt(deplete_ammount);
            savefile.WriteInt(nextArmorDepleteTime);

            for (i = 0; i < AMMO_NUMTYPES; i++) {
                savefile.WriteInt(ammo[i]);
            }
            for (i = 0; i < MAX_WEAPONS; i++) {
                savefile.WriteInt(clip[i]);
            }
            for (i = 0; i < MAX_POWERUPS; i++) {
                savefile.WriteInt(powerupEndTime[i]);
            }

            savefile.WriteInt(items.Num());
            for (i = 0; i < items.Num(); i++) {
                savefile.WriteDict(items.oGet(i));
            }

            savefile.WriteInt(pdasViewed[0]);
            savefile.WriteInt(pdasViewed[1]);
            savefile.WriteInt(pdasViewed[2]);
            savefile.WriteInt(pdasViewed[3]);

            savefile.WriteInt(selPDA);
            savefile.WriteInt(selVideo);
            savefile.WriteInt(selEMail);
            savefile.WriteInt(selAudio);
            savefile.WriteBool(pdaOpened);
            savefile.WriteBool(turkeyScore);

            savefile.WriteInt(pdas.Num());
            for (i = 0; i < pdas.Num(); i++) {
                savefile.WriteString(pdas.oGet(i));
            }

            savefile.WriteInt(pdaSecurity.Num());
            for (i = 0; i < pdaSecurity.Num(); i++) {
                savefile.WriteString(pdaSecurity.oGet(i));
            }

            savefile.WriteInt(videos.Num());
            for (i = 0; i < videos.Num(); i++) {
                savefile.WriteString(videos.oGet(i));
            }

            savefile.WriteInt(emails.Num());
            for (i = 0; i < emails.Num(); i++) {
                savefile.WriteString(emails.oGet(i));
            }

            savefile.WriteInt(nextItemPickup);
            savefile.WriteInt(nextItemNum);
            savefile.WriteInt(onePickupTime);

            savefile.WriteInt(pickupItemNames.Num());
            for (i = 0; i < pickupItemNames.Num(); i++) {
                savefile.WriteString(pickupItemNames.oGet(i).icon);
                savefile.WriteString(pickupItemNames.oGet(i).name);
            }

            savefile.WriteInt(objectiveNames.Num());
            for (i = 0; i < objectiveNames.Num(); i++) {
                savefile.WriteString(objectiveNames.oGet(i).screenshot);
                savefile.WriteString(objectiveNames.oGet(i).text);
                savefile.WriteString(objectiveNames.oGet(i).title);
            }

            savefile.WriteInt(levelTriggers.Num());
            for (i = 0; i < levelTriggers.Num(); i++) {
                savefile.WriteString(levelTriggers.oGet(i).levelName);
                savefile.WriteString(levelTriggers.oGet(i).triggerName);
            }

            savefile.WriteBool(ammoPulse);
            savefile.WriteBool(weaponPulse);
            savefile.WriteBool(armorPulse);

            savefile.WriteInt(lastGiveTime);
        }

        // unarchives object from save game file
        public void Restore(idRestoreGame savefile) {
            int i;
            int num;

            maxHealth = savefile.ReadInt();
            weapons = savefile.ReadInt();
            powerups = savefile.ReadInt();
            armor = savefile.ReadInt();
            maxarmor = savefile.ReadInt();
            ammoPredictTime = savefile.ReadInt();
            deplete_armor = savefile.ReadInt();
            deplete_rate = savefile.ReadFloat();
            deplete_ammount = savefile.ReadInt();
            nextArmorDepleteTime = savefile.ReadInt();

            for (i = 0; i < AMMO_NUMTYPES; i++) {
                ammo[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_WEAPONS; i++) {
                clip[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_POWERUPS; i++) {
                powerupEndTime[i] = savefile.ReadInt();
            }

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idDict itemdict = new idDict();

                savefile.ReadDict(itemdict);
                items.Append(itemdict);
            }

            // pdas
            pdasViewed[0] = savefile.ReadInt();
            pdasViewed[1] = savefile.ReadInt();
            pdasViewed[2] = savefile.ReadInt();
            pdasViewed[3] = savefile.ReadInt();

            selPDA = savefile.ReadInt();
            selVideo = savefile.ReadInt();
            selEMail = savefile.ReadInt();
            selAudio = savefile.ReadInt();
            pdaOpened = savefile.ReadBool();
            turkeyScore = savefile.ReadBool();

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idStr strPda = new idStr();
                savefile.ReadString(strPda);
                pdas.Append(strPda);
            }

            // pda security clearances
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idStr invName = new idStr();
                savefile.ReadString(invName);
                pdaSecurity.Append(invName);
            }

            // videos
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idStr strVideo = new idStr();
                savefile.ReadString(strVideo);
                videos.Append(strVideo);
            }

            // email
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idStr strEmail = new idStr();
                savefile.ReadString(strEmail);
                emails.Append(strEmail);
            }

            nextItemPickup = savefile.ReadInt();
            nextItemNum = savefile.ReadInt();
            onePickupTime = savefile.ReadInt();
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idItemInfo info = new idItemInfo();

                savefile.ReadString(info.icon);
                savefile.ReadString(info.name);

                pickupItemNames.Append(info);
            }

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idObjectiveInfo obj = new idObjectiveInfo();

                savefile.ReadString(obj.screenshot);
                savefile.ReadString(obj.text);
                savefile.ReadString(obj.title);

                objectiveNames.Append(obj);
            }

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                idLevelTriggerInfo lti = new idLevelTriggerInfo();
                savefile.ReadString(lti.levelName);
                savefile.ReadString(lti.triggerName);
                levelTriggers.Append(lti);
            }

            ammoPulse = savefile.ReadBool();
            weaponPulse = savefile.ReadBool();
            armorPulse = savefile.ReadBool();

            lastGiveTime = savefile.ReadInt();
        }

        public void Clear() {
            maxHealth = 0;
            weapons = 0;
            powerups = 0;
            armor = 0;
            maxarmor = 0;
            deplete_armor = 0;
            deplete_rate = 0;
            deplete_ammount = 0;
            nextArmorDepleteTime = 0;

//	memset( ammo, 0, sizeof( ammo ) );
            ClearPowerUps();

            // set to -1 so that the gun knows to have a full clip the first time we get it and at the start of the level
//	memset( clip, -1, sizeof( clip ) );
            Arrays.asList(clip, -1);

            items.DeleteContents(true);
//	memset(pdasViewed, 0, 4 * sizeof( pdasViewed[0] ) );
            pdas.Clear();
            videos.Clear();
            emails.Clear();
            selVideo = 0;
            selEMail = 0;
            selPDA = 0;
            selAudio = 0;
            pdaOpened = false;
            turkeyScore = false;

            levelTriggers.Clear();

            nextItemPickup = 0;
            nextItemNum = 1;
            onePickupTime = 0;
            pickupItemNames.Clear();
            objectiveNames.Clear();

            ammoPredictTime = 0;

            lastGiveTime = 0;

            ammoPulse = false;
            weaponPulse = false;
            armorPulse = false;
        }

        public void GivePowerUp(idPlayer player, int powerup, int msec) {
            if (0 == msec) {
                // get the duration from the .def files
                idDeclEntityDef def = null;
                switch (powerup) {
                    case BERSERK:
                        def = gameLocal.FindEntityDef("powerup_berserk", false);
                        break;
                    case INVISIBILITY:
                        def = gameLocal.FindEntityDef("powerup_invisibility", false);
                        break;
                    case MEGAHEALTH:
                        def = gameLocal.FindEntityDef("powerup_megahealth", false);
                        break;
                    case ADRENALINE:
                        def = gameLocal.FindEntityDef("powerup_adrenaline", false);
                        break;
                }
                assert (def != null);
                msec = def.dict.GetInt("time") * 1000;
            }
            powerups |= 1 << powerup;
            powerupEndTime[powerup] = gameLocal.time + msec;
        }

        public void ClearPowerUps() {
            int i;
            for (i = 0; i < MAX_POWERUPS; i++) {
                powerupEndTime[i] = 0;
            }
            powerups = 0;
        }

        public void GetPersistantData(idDict dict) {
            int i;
            int num;
            idDict item;
            String key;
            idKeyValue kv;
            String name;

            // armor
            dict.SetInt("armor", armor);

            // don't bother with powerups, maxhealth, maxarmor, or the clip
            // ammo
            for (i = 0; i < AMMO_NUMTYPES; i++) {
                name = idWeapon.GetAmmoNameForNum(i);
                if (name != null) {
                    dict.SetInt(name, ammo[i]);
                }
            }

            // items
            num = 0;
            for (i = 0; i < items.Num(); i++) {
                item = items.oGet(i);

                // copy all keys with "inv_"
                kv = item.MatchPrefix("inv_");
                if (kv != null) {
                    while (kv != null) {
                        key = String.format("item_%d %s", num, kv.GetKey());
                        dict.Set(key, kv.GetValue());
                        kv = item.MatchPrefix("inv_", kv);
                    }
                    num++;
                }
            }
            dict.SetInt("items", num);

            // pdas viewed
            for (i = 0; i < 4; i++) {
                dict.SetInt(va("pdasViewed_%d", i), pdasViewed[i]);
            }

            dict.SetInt("selPDA", selPDA);
            dict.SetInt("selVideo", selVideo);
            dict.SetInt("selEmail", selEMail);
            dict.SetInt("selAudio", selAudio);
            dict.SetInt("pdaOpened", btoi(pdaOpened));
            dict.SetInt("turkeyScore", btoi(turkeyScore));

            // pdas
            for (i = 0; i < pdas.Num(); i++) {
                key = String.format("pda_%d", i);
                dict.Set(key, pdas.oGet(i));
            }
            dict.SetInt("pdas", pdas.Num());

            // video cds
            for (i = 0; i < videos.Num(); i++) {
                key = String.format("video_%d", i);
                dict.Set(key, videos.oGet(i));
            }
            dict.SetInt("videos", videos.Num());

            // emails
            for (i = 0; i < emails.Num(); i++) {
                key = String.format("email_%d", i);
                dict.Set(key, emails.oGet(i));
            }
            dict.SetInt("emails", emails.Num());

            // weapons
            dict.SetInt("weapon_bits", weapons);

            dict.SetInt("levelTriggers", levelTriggers.Num());
            for (i = 0; i < levelTriggers.Num(); i++) {
                key = String.format("levelTrigger_Level_%d", i);
                dict.Set(key, levelTriggers.oGet(i).levelName);
                key = String.format("levelTrigger_Trigger_%d", i);
                dict.Set(key, levelTriggers.oGet(i).triggerName);
            }
        }

        public void RestoreInventory(idPlayer owner, final idDict dict) {
            int i;
            int num;
            idDict item;
            idStr key = new idStr();
            String itemname;
            idKeyValue kv;
            String name;

            Clear();

            // health/armor
            maxHealth = dict.GetInt("maxhealth", "100");
            armor = dict.GetInt("armor", "50");
            maxarmor = dict.GetInt("maxarmor", "100");
            deplete_armor = dict.GetInt("deplete_armor", "0");
            deplete_rate = dict.GetFloat("deplete_rate", "2.0");
            deplete_ammount = dict.GetInt("deplete_ammount", "1");

            // the clip and powerups aren't restored
            // ammo
            for (i = 0; i < AMMO_NUMTYPES; i++) {
                name = idWeapon.GetAmmoNameForNum(i);
                if (name != null) {
                    ammo[i] = dict.GetInt(name);
                }
            }

            // items
            num = dict.GetInt("items");
            items.SetNum(num);
            for (i = 0; i < num; i++) {
                item = new idDict();
                items.oSet(i, item);
                itemname = String.format("item_%d ", i);
                kv = dict.MatchPrefix(itemname);
                while (kv != null) {
                    key.oSet(kv.GetKey());
                    key.Strip(itemname);
                    item.Set(key, kv.GetValue());
                    kv = dict.MatchPrefix(itemname, kv);
                }
            }

            // pdas viewed
            for (i = 0; i < 4; i++) {
                pdasViewed[i] = dict.GetInt(va("pdasViewed_%d", i));
            }

            selPDA = dict.GetInt("selPDA");
            selEMail = dict.GetInt("selEmail");
            selVideo = dict.GetInt("selVideo");
            selAudio = dict.GetInt("selAudio");
            pdaOpened = dict.GetBool("pdaOpened");
            turkeyScore = dict.GetBool("turkeyScore");

            // pdas
            num = dict.GetInt("pdas");
            pdas.SetNum(num);
            for (i = 0; i < num; i++) {
                itemname = String.format("pda_%d", i);
                pdas.oSet(i, dict.GetString(itemname, "default"));
            }

            // videos
            num = dict.GetInt("videos");
            videos.SetNum(num);
            for (i = 0; i < num; i++) {
                itemname = String.format("video_%d", i);
                videos.oSet(i, dict.GetString(itemname, "default"));
            }

            // emails
            num = dict.GetInt("emails");
            emails.SetNum(num);
            for (i = 0; i < num; i++) {
                itemname = String.format("email_%d", i);
                emails.oSet(i, dict.GetString(itemname, "default"));
            }

            // weapons are stored as a number for persistant data, but as strings in the entityDef
            weapons = dict.GetInt("weapon_bits", "0");

            if (BuildDefines.ID_DEMO_BUILD) {
                Give(owner, dict, "weapon", dict.GetString("weapon"), null, false);
            } else {
                if (g_skill.GetInteger() >= 3) {
                    Give(owner, dict, "weapon", dict.GetString("weapon_nightmare"), null, false);
                } else {
                    Give(owner, dict, "weapon", dict.GetString("weapon"), null, false);
                }
            }

            num = dict.GetInt("levelTriggers");
            for (i = 0; i < num; i++) {
                itemname = String.format("levelTrigger_Level_%d", i);
                idLevelTriggerInfo lti = new idLevelTriggerInfo();
                lti.levelName.oSet(dict.GetString(itemname));
                itemname = String.format("levelTrigger_Trigger_%d", i);
                lti.triggerName.oSet(dict.GetString(itemname));
                levelTriggers.Append(lti);
            }

        }

        public boolean Give(idPlayer owner, final idDict spawnArgs, final String statname, final String value, int[] idealWeapon, boolean updateHud) {
            int i;
            int pos;
            int end;
            int len;
            idStr weaponString;
            int max;
            idDeclEntityDef weaponDecl;
            boolean tookWeapon;
            int amount;
            idItemInfo info;
            final String name;

            if (0 == idStr.Icmpn(statname, "ammo_", 5)) {
                i = AmmoIndexForAmmoClass(statname);
                max = MaxAmmoForAmmoClass(owner, statname);
                if (ammo[i] >= max) {
                    return false;
                }
                amount = Integer.parseInt(value);
                if (amount != 0) {
                    ammo[i] += amount;
                    if ((max > 0) && (ammo[i] > max)) {
                        ammo[i] = max;
                    }
                    ammoPulse = true;

                    name = AmmoPickupNameForIndex(i);
                    if (name != null && !name.isEmpty()) {
                        AddPickupName(name, "");
                    }
                }
            } else if (0 == idStr.Icmp(statname, "armor")) {
                if (armor >= maxarmor) {
                    return false;	// can't hold any more, so leave the item
                }
                amount = Integer.parseInt(value);
                if (amount != 0) {
                    armor += amount;
                    if (armor > maxarmor) {
                        armor = maxarmor;
                    }
                    nextArmorDepleteTime = 0;
                    armorPulse = true;
                }
            } else if (idStr.FindText(statname, "inclip_") == 0) {
                i = WeaponIndexForAmmoClass(spawnArgs, statname + 7);
                if (i != -1) {
                    // set, don't add. not going over the clip size limit.
                    clip[i] = Integer.parseInt(value);
                }
            } else if (0 == idStr.Icmp(statname, "berserk")) {
                GivePowerUp(owner, BERSERK, (int) SEC2MS(Float.parseFloat(value)));
            } else if (0 == idStr.Icmp(statname, "mega")) {
                GivePowerUp(owner, MEGAHEALTH, (int) SEC2MS(Float.parseFloat(value)));
            } else if (0 == idStr.Icmp(statname, "weapon")) {
                tookWeapon = false;
                for (pos = 0; pos != -1; pos = end) {
                    end = value.indexOf(',', pos);
                    if (end != -1) {
                        len = end - pos;
                        end++;
                    } else {
                        len = value.length() - pos;
                    }

//                        idStr weaponName( pos, 0, len );
                    final String weaponName = value.substring(pos, pos + len);

                    // find the number of the matching weapon name
                    for (i = 0; i < MAX_WEAPONS; i++) {
                        if (weaponName.equals(spawnArgs.GetString(va("def_weapon%d", i)))) {
                            break;
                        }
                    }

                    if (i >= MAX_WEAPONS) {
                        gameLocal.Error("Unknown weapon '%s'", weaponName);
                    }

                    // cache the media for this weapon
                    weaponDecl = gameLocal.FindEntityDef(weaponName, false);

                    // don't pickup "no ammo" weapon types twice
                    // not for D3 SP .. there is only one case in the game where you can get a no ammo
                    // weapon when you might already have it, in that case it is more conistent to pick it up
                    if (gameLocal.isMultiplayer && weaponDecl != null && ((weapons & (1 << i)) != 0) && 0 == weaponDecl.dict.GetInt("ammoRequired")) {
                        continue;
                    }

                    if (!gameLocal.world.spawnArgs.GetBool("no_Weapons") || ("weapon_fists".equals(weaponName)) || ("weapon_soulcube".equals(weaponName))) {//TODO:string in global vars, or local constants.
                        if ((weapons & (1 << i)) == 0 || gameLocal.isMultiplayer) {
                            if (owner.GetUserInfo().GetBool("ui_autoSwitch") && idealWeapon != null) {
                                assert (!gameLocal.isClient);
                                idealWeapon[0] = i;
                            }
                            if (owner.hud != null && updateHud && lastGiveTime + 1000 < gameLocal.time) {
                                owner.hud.SetStateInt("newWeapon", i);
                                owner.hud.HandleNamedEvent("newWeapon");
                                lastGiveTime = gameLocal.time;
                            }
                            weaponPulse = true;
                            weapons |= (1 << i);
                            tookWeapon = true;
                        }
                    }
                }
                return tookWeapon;
            } else if (0 == idStr.Icmp(statname, "item") || 0 == idStr.Icmp(statname, "icon") || 0 == idStr.Icmp(statname, "name")) {
                // ignore these as they're handled elsewhere
                return false;
            } else {
                // unknown item
                gameLocal.Warning("Unknown stat '%s' added to player's inventory", statname);
                return false;
            }

            return true;
        }

        public void Drop(final idDict spawnArgs, final String[] weapon_classname, int weapon_index) {
            // remove the weapon bit
            // also remove the ammo associated with the weapon as we pushed it in the item
            assert (weapon_index != -1 || weapon_classname[0] != null);
            if (weapon_index == -1) {
                for (weapon_index = 0; weapon_index < MAX_WEAPONS; weapon_index++) {
                    if (NOT(idStr.Icmp(weapon_classname[0], spawnArgs.GetString(va("def_weapon%d", weapon_index))))) {
                        break;
                    }
                }
                if (weapon_index >= MAX_WEAPONS) {
                    gameLocal.Error("Unknown weapon '%s'", weapon_classname[0]);
                }
            } else if (null == weapon_classname[0]) {
                weapon_classname[0] = spawnArgs.GetString(va("def_weapon%d", weapon_index));
            }
            weapons &= (0xffffffff ^ (1 << weapon_index));
            int ammo_i = AmmoIndexForWeaponClass(weapon_classname[0], null);
            if (ammo_i != 0) {
                clip[weapon_index] = -1;
                ammo[ammo_i] = 0;
            }
        }

        public int/*ammo_t*/ AmmoIndexForAmmoClass(final String ammo_classname) {
            return idWeapon.GetAmmoNumForName(ammo_classname);
        }

        public int MaxAmmoForAmmoClass(idPlayer owner, final String ammo_classname) {
            return owner.spawnArgs.GetInt(va("max_%s", ammo_classname), "0");
        }

        /*
         ==============
         idInventory::WeaponIndexForAmmoClass
         mapping could be prepared in the constructor
         ==============
         */
        public int WeaponIndexForAmmoClass(final idDict spawnArgs, final String ammo_classname) {
            int i;
            String weapon_classname;
            for (i = 0; i < MAX_WEAPONS; i++) {
                weapon_classname = spawnArgs.GetString(va("def_weapon%d", i));
                if (null == weapon_classname) {
                    continue;
                }
                final idDeclEntityDef decl = gameLocal.FindEntityDef(weapon_classname, false);
                if (null == decl) {
                    continue;
                }
                if (0 == idStr.Icmp(ammo_classname, decl.dict.GetString("ammoType"))) {
                    return i;
                }
            }
            return -1;
        }

        public int/*ammo_t*/ AmmoIndexForWeaponClass(final String weapon_classname, int[] ammoRequired) {
            final idDeclEntityDef decl = gameLocal.FindEntityDef(weapon_classname, false);
            if (null == decl) {
                gameLocal.Error("Unknown weapon in decl '%s'", weapon_classname);
            }
            if (ammoRequired != null) {
                ammoRequired[0] = decl.dict.GetInt("ammoRequired");
            }
            int ammo_i = AmmoIndexForAmmoClass(decl.dict.GetString("ammoType"));
            return ammo_i;
        }

        public String AmmoPickupNameForIndex(int ammonum) {
            return idWeapon.GetAmmoPickupNameForNum(ammonum);
        }

        public void AddPickupName(final String name, final String icon) {
            int num;

            num = pickupItemNames.Num();
            if ((num == 0) || (pickupItemNames.oGet(num - 1).name.Icmp(name) != 0)) {
                idItemInfo info = pickupItemNames.Alloc();

                if (idStr.Cmpn(name, STRTABLE_ID, STRTABLE_ID_LENGTH) == 0) {
                    info.name = new idStr(common.GetLanguageDict().GetString(name));
                } else {
                    info.name = new idStr(name);
                }
                info.icon = new idStr(icon);
            }
        }

        public int HasAmmo(int type, int amount) {
            if ((type == 0) || 0 == amount) {
                // always allow weapons that don't use ammo to fire
                return -1;
            }

            // check if we have infinite ammo
            if (ammo[type] < 0) {
                return -1;
            }

            // return how many shots we can fire
            return ammo[type] / amount;
        }

        public boolean UseAmmo(int type, int amount) {
            if (NOT(HasAmmo(type, amount))) {
                return false;
            }

            // take an ammo away if not infinite
            if (ammo[type] >= 0) {
                ammo[type] -= amount;
                ammoPredictTime = gameLocal.time; // mp client: we predict this. mark time so we're not confused by snapshots
            }

            return true;
        }

        public int HasAmmo(final String weapon_classname) {			// looks up the ammo information for the weapon class first
            int[] ammoRequired = new int[1];
            int ammo_i = AmmoIndexForWeaponClass(weapon_classname, ammoRequired);
            return HasAmmo(ammo_i, ammoRequired[0]);
        }

        public void UpdateArmor() {
            if (deplete_armor != 0 && deplete_armor < armor) {
                if (0 == nextArmorDepleteTime) {
                    nextArmorDepleteTime = (int) (gameLocal.time + deplete_rate * 1000);
                } else if (gameLocal.time > nextArmorDepleteTime) {
                    armor -= deplete_ammount;
                    if (armor < deplete_armor) {
                        armor = deplete_armor;
                    }
                    nextArmorDepleteTime = (int) (gameLocal.time + deplete_rate * 1000);
                }
            }
        }
    };

    public static class loggedAccel_t {

        int    time;
        idVec3 dir;        // scaled larger for running

        public loggedAccel_t() {
            dir = new idVec3();
        }
    };

    public static class aasLocation_t {

        int areaNum;
        idVec3 pos;
    };

    public static class idPlayer extends idActor {
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();

        static {
            eventCallbacks.putAll(idActor.getEventCallBacks());
            eventCallbacks.put(EV_Player_GetButtons, (eventCallback_t0<idPlayer>) idPlayer::Event_GetButtons);
            eventCallbacks.put(EV_Player_GetMove, (eventCallback_t0<idPlayer>) idPlayer::Event_GetMove);
            eventCallbacks.put(EV_Player_GetViewAngles, (eventCallback_t0<idPlayer>) idPlayer::Event_GetViewAngles);
            eventCallbacks.put(EV_Player_StopFxFov, (eventCallback_t0<idPlayer>) idPlayer::Event_StopFxFov);
            eventCallbacks.put(EV_Player_EnableWeapon, (eventCallback_t0<idPlayer>) idPlayer::Event_EnableWeapon);
            eventCallbacks.put(EV_Player_DisableWeapon, (eventCallback_t0<idPlayer>) idPlayer::Event_DisableWeapon);
            eventCallbacks.put(EV_Player_GetCurrentWeapon, (eventCallback_t0<idPlayer>) idPlayer::Event_GetCurrentWeapon);
            eventCallbacks.put(EV_Player_GetPreviousWeapon, (eventCallback_t0<idPlayer>) idPlayer::Event_GetPreviousWeapon);
            eventCallbacks.put(EV_Player_SelectWeapon, (eventCallback_t1<idPlayer>) idPlayer::Event_SelectWeapon);
            eventCallbacks.put(EV_Player_GetWeaponEntity, (eventCallback_t0<idPlayer>) idPlayer::Event_GetWeaponEntity);
            eventCallbacks.put(EV_Player_OpenPDA, (eventCallback_t0<idPlayer>) idPlayer::Event_OpenPDA);
            eventCallbacks.put(EV_Player_InPDA, (eventCallback_t0<idPlayer>) idPlayer::Event_InPDA);
            eventCallbacks.put(EV_Player_ExitTeleporter, (eventCallback_t0<idPlayer>) idPlayer::Event_ExitTeleporter);
            eventCallbacks.put(EV_Player_StopAudioLog, (eventCallback_t0<idPlayer>) idPlayer::Event_StopAudioLog);
            eventCallbacks.put(EV_Player_HideTip, (eventCallback_t0<idPlayer>) idPlayer::Event_HideTip);
            eventCallbacks.put(EV_Player_LevelTrigger, (eventCallback_t0<idPlayer>) idPlayer::Event_LevelTrigger);
            eventCallbacks.put(EV_Gibbed, (eventCallback_t0<idPlayer>) idPlayer::Event_Gibbed);
            eventCallbacks.put(EV_Player_GetIdealWeapon, (eventCallback_t0<idPlayer>) idPlayer::Event_GetIdealWeapon);
        }

        // enum {
        public static final int EVENT_IMPULSE          = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_EXIT_TELEPORTER  = EVENT_IMPULSE + 1;
        public static final int EVENT_ABORT_TELEPORTER = EVENT_IMPULSE + 2;
        public static final int EVENT_POWERUP          = EVENT_IMPULSE + 3;
        public static final int EVENT_SPECTATE         = EVENT_IMPULSE + 4;
        public static final int EVENT_MAXEVENTS        = EVENT_IMPULSE + 5;
        // };
        //
        public usercmd_t                 usercmd;
        //
        public idPlayerView              playerView;            // handles damage kicks and effects
        //
        public boolean                   noclip;
        public boolean                   godmode;
        //
        public boolean                   spawnAnglesSet;        // on first usercmd, we must set deltaAngles
        public idAngles                  spawnAngles;
        public idAngles                  viewAngles;            // player view angles
        public idAngles                  cmdAngles;             // player cmd angles
        //
        public int                       buttonMask;
        public int                       oldButtons;
        public int                       oldFlags;
        //
        public int                       lastHitTime;           // last time projectile fired by player hit target
        public int                       lastSndHitTime;        // MP hit sound - != lastHitTime because we throttle
        public int                       lastSavingThrowTime;   // for the "free miss" effect
        //
        public idScriptBool AI_FORWARD      = new idScriptBool();
        public idScriptBool AI_BACKWARD     = new idScriptBool();
        public idScriptBool AI_STRAFE_LEFT  = new idScriptBool();
        public idScriptBool AI_STRAFE_RIGHT = new idScriptBool();
        public idScriptBool AI_ATTACK_HELD  = new idScriptBool();
        public idScriptBool AI_WEAPON_FIRED = new idScriptBool();
        public idScriptBool AI_JUMP         = new idScriptBool();
        public idScriptBool AI_CROUCH       = new idScriptBool();
        public idScriptBool AI_ONGROUND     = new idScriptBool();
        public idScriptBool AI_ONLADDER     = new idScriptBool();
        public idScriptBool AI_DEAD         = new idScriptBool();
        public idScriptBool AI_RUN          = new idScriptBool();
        public idScriptBool AI_PAIN         = new idScriptBool();
        public idScriptBool AI_HARDLANDING  = new idScriptBool();
        public idScriptBool AI_SOFTLANDING  = new idScriptBool();
        public idScriptBool AI_RELOAD       = new idScriptBool();
        public idScriptBool AI_TELEPORT     = new idScriptBool();
        public idScriptBool AI_TURN_LEFT    = new idScriptBool();
        public idScriptBool AI_TURN_RIGHT   = new idScriptBool();
        //
        // inventory
        public idInventory               inventory;
        //
        public idEntityPtr<idWeapon>     weapon;
        public idUserInterface           hud;                   // MP: is NULL if not local player
        public idUserInterface           objectiveSystem;
        public boolean                   objectiveSystemOpen;
        //
        public int                       weapon_soulcube;
        public int                       weapon_pda;
        public int                       weapon_fists;
        //
        public int                       heartRate;
        public idInterpolate<Float>      heartInfo;
        public int                       lastHeartAdjust;
        public int                       lastHeartBeat;
        public int                       lastDmgTime;
        public int                       deathClearContentsTime;
        public boolean                   doingDeathSkin;
        public int                       lastArmorPulse;        // lastDmgTime if we had armor at time of hit
        public float                     stamina;
        public float                     healthPool;            // amount of health to give over time
        public int                       nextHealthPulse;
        public boolean                   healthPulse;
        public boolean                   healthTake;
        public int                       nextHealthTake;
        //
        //
        public boolean                   hiddenWeapon;          // if the weapon is hidden ( in noWeapons maps )
        public idEntityPtr<idProjectile> soulCubeProjectile;
        //
        // mp stuff
        //        public static final idVec3[] colorBarTable = new idVec3[5];
        public static final idVec3[] colorBarTable = {
                new idVec3(0.25f, 0.25f, 0.25f),
                new idVec3(1.00f, 0.00f, 0.00f),
                new idVec3(0.00f, 0.80f, 0.10f),
                new idVec3(0.20f, 0.50f, 0.80f),
                new idVec3(1.00f, 0.80f, 0.10f)
        };
        public  int                   spectator;
        public  idVec3                colorBar;                 // used for scoreboard and hud display
        public  int                   colorBarIndex;
        public  boolean               scoreBoardOpen;
        public  boolean               forceScoreBoard;
        public  boolean               forceRespawn;
        public  boolean               spectating;
        public  int                   lastSpectateTeleport;
        public  boolean               lastHitToggle;
        public  boolean               forcedReady;
        public  boolean               wantSpectate;             // from userInfo
        public  boolean               weaponGone;               // force stop firing
        public  boolean               useInitialSpawns;         // toggled by a map restart to be active for the first game spawn
        public  int                   latchedTeam;              // need to track when team gets changed
        public  int                   tourneyRank;              // for tourney cycling - the higher, the more likely to play next - server
        public  int                   tourneyLine;              // client side - our spot in the wait line. 0 means no info.
        public  int                   spawnedTime;              // when client first enters the game
        //
        public  idEntityPtr<idEntity> teleportEntity;           // while being teleported, this is set to the entity we'll use for exit
        public  int                   teleportKiller;           // entity number of an entity killing us at teleporter exit
        public  boolean               lastManOver;              // can't respawn in last man anymore (srv only)
        public  boolean               lastManPlayAgain;         // play again when end game delay is cancelled out before expiring (srv only)
        public  boolean               lastManPresent;           // true when player was in when game started (spectators can't join a running LMS)
        public  boolean               isLagged;                 // replicated from server, true if packets haven't been received from client.
        public  boolean               isChatting;               // replicated from server, true if the player is chatting.
        //
        // timers
        public  int                   minRespawnTime;           // can respawn when time > this, force after g_forcerespawn
        public  int                   maxRespawnTime;           // force respawn after this time
        //
        // the first person view values are always calculated, even
        // if a third person view is used
        public  idVec3                firstPersonViewOrigin;
        public  idMat3                firstPersonViewAxis;
        //
        public  idDragEntity          dragEntity;
        //
        private int/*jointHandle_t*/  hipJoint;
        private int/*jointHandle_t*/  chestJoint;
        private int/*jointHandle_t*/  headJoint;
        //
        private idPhysics_Player      physicsObj;               // player physics
        //
        private idList<aasLocation_t> aasLocation;              // for AI tracking the player
        //
        private int                   bobFoot;
        private float                 bobFrac;
        private float                 bobfracsin;
        private int                   bobCycle;                 // for view bobbing and footstep generation
        private float                 xyspeed;
        private int                   stepUpTime;
        private float                 stepUpDelta;
        private float                 idealLegsYaw;
        private float                 legsYaw;
        private boolean               legsForward;
        private float                 oldViewYaw;
        private idAngles              viewBobAngles;
        private idVec3                viewBob;
        private int                   landChange;
        private int                   landTime;
        //
        private int                   currentWeapon;
        private int                   idealWeapon;
        private int                   previousWeapon;
        private int                   weaponSwitchTime;
        private boolean               weaponEnabled;
        private boolean               showWeaponViewModel;
        //
        private idDeclSkin            skin;
        private idDeclSkin            powerUpSkin;
        private idStr                 baseSkinName;
        //
        private int                   numProjectilesFired;      // number of projectiles fired
        private int                   numProjectileHits;        // number of hits on mobs
        //
        private boolean               airless;
        private int                   airTics;                  // set to pm_airTics at start, drops in vacuum
        private int                   lastAirDamage;
        //
        private boolean               gibDeath;
        private boolean               gibsLaunched;
        private idVec3                gibsDir;
        //
        private idInterpolate<Float>  zoomFov;
        private idInterpolate<Float>  centerView;
        private boolean               fxFov;
        //
        private float                 influenceFov;
        private int                   influenceActive;          // level of influence.. 1 == no gun or hud .. 2 == 1 + no movement
        private idEntity              influenceEntity;
        private idMaterial            influenceMaterial;
        private float                 influenceRadius;
        private idDeclSkin            influenceSkin;
        //
        private idCamera              privateCameraView;
        //
        private static final int             NUM_LOGGED_VIEW_ANGLES = 64;   // for weapon turning angle offsets
        private        final idAngles[]      loggedViewAngles;              // [gameLocal.framenum&(LOGGED_VIEW_ANGLES-1)]
        private static final int             NUM_LOGGED_ACCELS      = 16;   // for weapon turning angle offsets
        private        final loggedAccel_t[] loggedAccel;                   // [currentLoggedAccel & (NUM_LOGGED_ACCELS-1)]
        private              int             currentLoggedAccel;
        //
        // if there is a focusGUIent, the attack button will be changed into mouse clicks
        private              idEntity           focusGUIent;
        private              idUserInterface    focusUI;        // focusGUIent->renderEntity.gui, gui2, or gui3
        private              idAI               focusCharacter;
        private              int                talkCursor;     // show the state of the focusCharacter (0 == can't talk/dead, 1 == ready to talk, 2 == busy talking)
        private              int                focusTime;
        private              idAFEntity_Vehicle focusVehicle;
        private              idUserInterface    cursor;
        //	
        // full screen guis track mouse movements directly
        private              int                oldMouseX;
        private              int                oldMouseY;
        //
        private              idStr              pdaAudio;
        private              idStr              pdaVideo;
        private              idStr              pdaVideoWave;
        //
        private              boolean            tipUp;
        private              boolean            objectiveUp;
        //
        private              int                lastDamageDef;
        private              idVec3             lastDamageDir;
        private              int                lastDamageLocation;
        private              int                smoothedFrame;
        private              boolean            smoothedOriginUpdated;
        private              idVec3             smoothedOrigin;
        private              idAngles           smoothedAngles;
        //
        // mp
        private              boolean            ready;           // from userInfo
        private              boolean            respawning;      // set to true while in SpawnToPoint for telefrag checks
        private              boolean            leader;          // for sudden death situations
        private              int                lastSpectateChange;
        private              int                lastTeleFX;
        private /*unsigned*/ int                lastSnapshotSequence;    // track state hitches on clients
        private              boolean            weaponCatchup;           // raise up the weapon silently ( state catchups )
        private              int                MPAim;                   // player num in aim
        private              int                lastMPAim;
        private              int                lastMPAimTime;  // last time the aim changed
        private              int                MPAimFadeTime;  // for GUI fade
        private              boolean            MPAimHighlight;
        private              boolean            isTelefragged;  // proper obituaries
        //
        private              idPlayerIcon       playerIcon;
        //
        private              boolean            selfSmooth;
        //
        //

        public idPlayer() {
            usercmd = new usercmd_t();//memset( &usercmd, 0, sizeof( usercmd ) );

            playerView = new idPlayerView();

            noclip = false;
            godmode = false;

            spawnAnglesSet = false;
            spawnAngles = getAng_zero();
            viewAngles = getAng_zero();
            cmdAngles = getAng_zero();

            oldButtons = 0;
            buttonMask = 0;
            oldFlags = 0;

            lastHitTime = 0;
            lastSndHitTime = 0;
            lastSavingThrowTime = 0;

            inventory = new idInventory();

            weapon = new idEntityPtr<>(null);

            hud = null;
            objectiveSystem = null;
            objectiveSystemOpen = false;

            heartRate = BASE_HEARTRATE;
            heartInfo = new idInterpolate<>();
            heartInfo.Init(0, 0, 0f, 0f);
            lastHeartAdjust = 0;
            lastHeartBeat = 0;
            lastDmgTime = 0;
            deathClearContentsTime = 0;
            lastArmorPulse = -10000;
            stamina = 0;
            healthPool = 0;
            nextHealthPulse = 0;
            healthPulse = false;
            nextHealthTake = 0;
            healthTake = false;

            scoreBoardOpen = false;
            forceScoreBoard = false;
            forceRespawn = false;
            spectating = false;
            spectator = 0;
            colorBar = getVec3_zero();
            colorBarIndex = 0;
            forcedReady = false;
            wantSpectate = false;

            lastHitToggle = false;

            minRespawnTime = 0;
            maxRespawnTime = 0;

            firstPersonViewOrigin = getVec3_zero();
            firstPersonViewAxis = getMat3_identity();
            
            dragEntity = new idDragEntity();
            
            physicsObj = new idPhysics_Player();
            
            aasLocation = new idList<>();

            hipJoint = INVALID_JOINT;
            chestJoint = INVALID_JOINT;
            headJoint = INVALID_JOINT;

            bobFoot = 0;
            bobFrac = 0;
            bobfracsin = 0;
            bobCycle = 0;
            xyspeed = 0;
            stepUpTime = 0;
            stepUpDelta = 0;
            idealLegsYaw = 0;
            legsYaw = 0;
            legsForward = true;
            oldViewYaw = 0;
            viewBobAngles = getAng_zero();
            viewBob = getVec3_zero();
            landChange = 0;
            landTime = 0;

            currentWeapon = -1;
            idealWeapon = -1;
            previousWeapon = -1;
            weaponSwitchTime = 0;
            weaponEnabled = true;
            weapon_soulcube = -1;
            weapon_pda = -1;
            weapon_fists = -1;
            showWeaponViewModel = true;

            skin = new idDeclSkin();
            powerUpSkin = new idDeclSkin();
            baseSkinName = new idStr("");

            numProjectilesFired = 0;
            numProjectileHits = 0;

            airless = false;
            airTics = 0;
            lastAirDamage = 0;

            gibDeath = false;
            gibsLaunched = false;
            gibsDir = getVec3_zero();

            zoomFov = new idInterpolate<>();
            zoomFov.Init(0, 0, 0f, 0f);
            centerView = new idInterpolate<>();
            centerView.Init(0, 0, 0f, 0f);
            fxFov = false;

            influenceFov = 0;
            influenceActive = 0;
            influenceRadius = 0;
            influenceEntity = null;
            influenceMaterial = null;
            influenceSkin = null;

            privateCameraView = null;

//	memset( loggedViewAngles, 0, sizeof( loggedViewAngles ) );
            loggedViewAngles = Stream.generate(idAngles::new).limit(NUM_LOGGED_VIEW_ANGLES).toArray(idAngles[]::new);
//	memset( loggedAccel, 0, sizeof( loggedAccel ) );
            loggedAccel = Stream.generate(loggedAccel_t::new).limit(NUM_LOGGED_ACCELS).toArray(loggedAccel_t[]::new);
            currentLoggedAccel = 0;

            focusTime = 0;
            focusGUIent = null;
            focusUI = null;
            focusCharacter = null;
            talkCursor = 0;
            focusVehicle = null;
            cursor = null;

            oldMouseX = 0;
            oldMouseY = 0;

            pdaAudio = new idStr("");
            pdaVideo = new idStr("");
            pdaVideoWave = new idStr("");

            lastDamageDef = 0;
            lastDamageDir = getVec3_zero();
            lastDamageLocation = 0;
            smoothedFrame = 0;
            smoothedOriginUpdated = false;
            smoothedOrigin = getVec3_zero();
            smoothedAngles = getAng_zero();

            fl.networkSync = true;

            latchedTeam = -1;
            doingDeathSkin = false;
            weaponGone = false;
            useInitialSpawns = false;
            tourneyRank = 0;
            lastSpectateTeleport = 0;
            tourneyLine = 0;
            hiddenWeapon = false;
            tipUp = false;
            objectiveUp = false;
            teleportEntity = new idEntityPtr<>(null);
            teleportKiller = -1;
            respawning = false;
            ready = false;
            leader = false;
            lastSpectateChange = 0;
            lastTeleFX = -9999;
            weaponCatchup = false;
            lastSnapshotSequence = 0;

            MPAim = -1;
            lastMPAim = -1;
            lastMPAimTime = 0;
            MPAimFadeTime = 0;
            MPAimHighlight = false;

            spawnedTime = 0;
            lastManOver = false;
            lastManPlayAgain = false;
            lastManPresent = false;

            isTelefragged = false;

            isLagged = false;
            isChatting = false;

            selfSmooth = false;
        }
//	virtual					~idPlayer();
//

        /*
         ==============
         idPlayer::Spawn

         Prepare any resources used by the player.
         ==============
         */
        @Override
        public void Spawn() {
            super.Spawn();
            
            idStr temp = new idStr();
//            idBounds bounds;

            if (entityNumber >= MAX_CLIENTS) {
                gameLocal.Error("entityNum > MAX_CLIENTS for player.  Player may only be spawned with a client.");
            }

            // allow thinking during cinematics
            cinematic = true;

            if (gameLocal.isMultiplayer) {
                // always start in spectating state waiting to be spawned in
                // do this before SetClipModel to get the right bounding box
                spectating = true;
            }

            // set our collision model
            physicsObj.SetSelf(this);
            SetClipModel();
            physicsObj.SetMass(spawnArgs.GetFloat("mass", "100"));
            physicsObj.SetContents(CONTENTS_BODY);
            physicsObj.SetClipMask(MASK_PLAYERSOLID);
            SetPhysics(physicsObj);
            InitAASLocation();

            skin.oSet(renderEntity.customSkin);

            // only the local player needs guis
            if (!gameLocal.isMultiplayer || entityNumber == gameLocal.localClientNum) {

                // load HUD
                if (gameLocal.isMultiplayer) {
                    hud = uiManager.FindGui("guis/mphud.gui", true, false, true);
                } else if (spawnArgs.GetString("hud", "", temp)) {
                    hud = uiManager.FindGui(temp.toString(), true, false, true);
                }
                if (hud != null) {
                    hud.Activate(true, gameLocal.time);
                }

                // load cursor
                if (spawnArgs.GetString("cursor", "", temp)) {
                    cursor = uiManager.FindGui(temp.toString(), true, gameLocal.isMultiplayer, gameLocal.isMultiplayer);
                }
                if (cursor != null) {
                    cursor.Activate(true, gameLocal.time);
                }

                objectiveSystem = uiManager.FindGui("guis/pda.gui", true, false, true);
                objectiveSystemOpen = false;
            }

            SetLastHitTime(0);

            // load the armor sound feedback
            declManager.FindSound("player_sounds_hitArmor");

            // set up conditions for animation
            LinkScriptVariables();

            animator.RemoveOriginOffset(true);

            // initialize user info related settings
            // on server, we wait for the userinfo broadcast, as this controls when the player is initially spawned in game
            if (gameLocal.isClient || entityNumber == gameLocal.localClientNum) {
                UserInfoChanged(false);
            }

            // create combat collision hull for exact collision detection
            SetCombatModel();

            // init the damage effects
            playerView.SetPlayerEntity(this);

            // supress model in non-player views, but allow it in mirrors and remote views
            renderEntity.suppressSurfaceInViewID = entityNumber + 1;

            // don't project shadow on self or weapon
            renderEntity.noSelfShadow = true;

            idAFAttachment headEnt = head.GetEntity();
            if (headEnt != null) {
                headEnt.GetRenderEntity().suppressSurfaceInViewID = entityNumber + 1;
                headEnt.GetRenderEntity().noSelfShadow = true;
            }

            if (gameLocal.isMultiplayer) {
                Init();
                Hide();    // properly hidden if starting as a spectator
                if (!gameLocal.isClient) {
                    // set yourself ready to spawn. idMultiplayerGame will decide when/if appropriate and call SpawnFromSpawnSpot
                    SetupWeaponEntity();
                    SpawnFromSpawnSpot();
                    forceRespawn = true;
                    assert (spectating);
                }
            } else {
                SetupWeaponEntity();
                SpawnFromSpawnSpot();
            }

            // trigger playtesting item gives, if we didn't get here from a previous level
            // the devmap key will be set on the first devmap, but cleared on any level
            // transitions
            if (!gameLocal.isMultiplayer && gameLocal.serverInfo.FindKey("devmap") != null) {
                // fire a trigger with the name "devmap"
                idEntity ent = gameLocal.FindEntity("devmap");
                if (ent != null) {
                    ent.ActivateTargets(this);
                }
            }
            if (hud != null) {
                // We can spawn with a full soul cube, so we need to make sure the hud knows this
                if (weapon_soulcube > 0 && (inventory.weapons & (1 << weapon_soulcube)) != 0) {
                    int max_souls = inventory.MaxAmmoForAmmoClass(this, "ammo_souls");
                    if (inventory.ammo[idWeapon.GetAmmoNumForName("ammo_souls")] >= max_souls) {
                        hud.HandleNamedEvent("soulCubeReady");
                    }
                }
                hud.HandleNamedEvent("itemPickup");
            }

            if (GetPDA() != null) {
                // Add any emails from the inventory
                for (int i = 0; i < inventory.emails.Num(); i++) {
                    GetPDA().AddEmail(inventory.emails.oGet(i).toString());
                }
                GetPDA().SetSecurity(common.GetLanguageDict().GetString("#str_00066"));
            }

            if (gameLocal.world.spawnArgs.GetBool("no_Weapons")) {
                hiddenWeapon = true;
                if (weapon.GetEntity() != null) {
                    weapon.GetEntity().LowerWeapon();
                }
                idealWeapon = 0;
            } else {
                hiddenWeapon = false;
            }

            if (hud != null) {
                UpdateHudWeapon();
                hud.StateChanged(gameLocal.time);
            }

            tipUp = false;
            objectiveUp = false;

            if (inventory.levelTriggers.Num() != 0) {
                PostEventMS(EV_Player_LevelTrigger, 0);
            }

            inventory.pdaOpened = false;
            inventory.selPDA = 0;

            if (!gameLocal.isMultiplayer) {
                if (g_skill.GetInteger() < 2) {
                    if (health < 25) {
                        health = 25;
                    }
                    if (g_useDynamicProtection.GetBool()) {
                        g_damageScale.SetFloat(1.0f);
                    }
                } else {
                    g_damageScale.SetFloat(1.0f);
                    g_armorProtection.SetFloat((g_skill.GetInteger() < 2) ? 0.4f : 0.2f);
                    if (BuildDefines.ID_DEMO_BUILD) {
                        if (g_skill.GetInteger() == 3) {
                            healthTake = true;
                            nextHealthTake = gameLocal.time + g_healthTakeTime.GetInteger() * 1000;
                        }
                    }
                }
            }
        }

        /*
         ==============
         idPlayer::Think

         Called every tic for each player
         ==============
         */
        @Override
        public void Think() {
            renderEntity_s headRenderEnt;

            UpdatePlayerIcons();

            // latch button actions
            oldButtons = usercmd.buttons;

            // grab out usercmd
            usercmd_t oldCmd = usercmd;
            usercmd = gameLocal.usercmds[entityNumber];
            buttonMask &= usercmd.buttons;
            usercmd.buttons &= ~buttonMask;

            if (gameLocal.inCinematic && gameLocal.skipCinematic) {
                return;
            }

            // clear the ik before we do anything else so the skeleton doesn't get updated twice
            walkIK.ClearJointMods();

            // if this is the very first frame of the map, set the delta view angles
            // based on the usercmd angles
            if (!spawnAnglesSet && (gameLocal.GameState() != GAMESTATE_STARTUP)) {
                spawnAnglesSet = true;
                SetViewAngles(spawnAngles);
                oldFlags = usercmd.flags;
            }

            if (objectiveSystemOpen || gameLocal.inCinematic || influenceActive != 0) {
                if (objectiveSystemOpen && AI_PAIN._()) {
                    TogglePDA();
                }
                usercmd.forwardmove = 0;
                usercmd.rightmove = 0;
                usercmd.upmove = 0;
            }

            // log movement changes for weapon bobbing effects
            if (usercmd.forwardmove != oldCmd.forwardmove) {
                loggedAccel_t acc = loggedAccel[currentLoggedAccel & (NUM_LOGGED_ACCELS - 1)];
                currentLoggedAccel++;
                acc.time = gameLocal.time;
                acc.dir.oSet(0, usercmd.forwardmove - oldCmd.forwardmove);
                acc.dir.oSet(1, acc.dir.oSet(2, 0));
            }

            if (usercmd.rightmove != oldCmd.rightmove) {
                loggedAccel_t acc = loggedAccel[currentLoggedAccel & (NUM_LOGGED_ACCELS - 1)];
                currentLoggedAccel++;
                acc.time = gameLocal.time;
                acc.dir.oSet(0, usercmd.forwardmove - oldCmd.forwardmove);
                acc.dir.oSet(1, acc.dir.oSet(2, 0));
            }

            // freelook centering
            if (((usercmd.buttons ^ oldCmd.buttons) & BUTTON_MLOOK) != 0) {
                centerView.Init(gameLocal.time, 200, viewAngles.pitch, 0f);
            }

            // zooming
            if (((usercmd.buttons ^ oldCmd.buttons) & BUTTON_ZOOM) != 0) {
                if (((usercmd.buttons & BUTTON_ZOOM) != 0) && weapon.GetEntity() != null) {
                    zoomFov.Init(gameLocal.time, 200, CalcFov(false), (float) weapon.GetEntity().GetZoomFov());
                } else {
                    zoomFov.Init(gameLocal.time, 200, zoomFov.GetCurrentValue(gameLocal.time), DefaultFov());
                }
            }

            // if we have an active gui, we will unrotate the view angles as
            // we turn the mouse movements into gui events
            idUserInterface gui = ActiveGui();
            if (gui != null && gui != focusUI) {
                RouteGuiMouse(gui);
            }

            // set the push velocity on the weapon before running the physics
            if (weapon.GetEntity() != null) {
                weapon.GetEntity().SetPushVelocity(physicsObj.GetPushedLinearVelocity());
            }

            EvaluateControls();

            if (!af.IsActive()) {
                AdjustBodyAngles();
                CopyJointsFromBodyToHead();
            }

            Move();

            if (!g_stopTime.GetBool()) {

                if (!noclip && !spectating && (health > 0) && !IsHidden()) {
                    TouchTriggers();
                }

                // not done on clients for various reasons. don't do it on server and save the sound channel for other things
                if (!gameLocal.isMultiplayer) {
                    SetCurrentHeartRate();
                    float scale = g_damageScale.GetFloat();
                    if (g_useDynamicProtection.GetBool() && scale < 1.0f && gameLocal.time - lastDmgTime > 500) {
                        if (scale < 1.0f) {
                            scale += 0.05f;
                        }
                        if (scale > 1.0f) {
                            scale = 1.0f;
                        }
                        g_damageScale.SetFloat(scale);
                    }
                }

                // update GUIs, Items, and character interactions
                UpdateFocus();

                UpdateLocation();

                // update player script
                UpdateScript();

                // service animations
                if (!spectating && !af.IsActive() && !gameLocal.inCinematic) {
                    UpdateConditions();
                    UpdateAnimState();
                    CheckBlink();
                }

                // clear out our pain flag so we can tell if we recieve any damage between now and the next time we think
                AI_PAIN._(false);
            }

            // calculate the exact bobbed view position, which is used to
            // position the view weapon, among other things
            CalculateFirstPersonView();

            // this may use firstPersonView, or a thirdPeroson / camera view
            CalculateRenderView();

            inventory.UpdateArmor();

            if (spectating) {
                UpdateSpectating();
            } else if (health > 0) {
                UpdateWeapon();
            }

            UpdateAir();

            UpdateHud();

            UpdatePowerUps();

            UpdateDeathSkin(false);

            if (gameLocal.isMultiplayer) {
                DrawPlayerIcons();
            }

            if (head.GetEntity() != null) {
                headRenderEnt = head.GetEntity().GetRenderEntity();
            } else {
                headRenderEnt = null;
            }

            if (headRenderEnt != null) {
                if (influenceSkin != null) {
                    headRenderEnt.customSkin = influenceSkin;
                } else {
                    headRenderEnt.customSkin = null;
                }
            }

            if (gameLocal.isMultiplayer || g_showPlayerShadow.GetBool()) {
                renderEntity.suppressShadowInViewID = 0;
                if (headRenderEnt != null) {
                    headRenderEnt.suppressShadowInViewID = 0;
                }
            } else {
                renderEntity.suppressShadowInViewID = entityNumber + 1;
                if (headRenderEnt != null) {
                    headRenderEnt.suppressShadowInViewID = entityNumber + 1;
                }
            }
            // never cast shadows from our first-person muzzle flashes
            renderEntity.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + entityNumber;
            if (headRenderEnt != null) {
                headRenderEnt.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + entityNumber;
            }

            if (!g_stopTime.GetBool()) {
                UpdateAnimation();

                Present();

                UpdateDamageEffects();

                LinkCombat();

                playerView.CalculateShake();
            }

            if (0 == (thinkFlags & TH_THINK)) {
                gameLocal.Printf("player %d not thinking?\n", entityNumber);
            }

            if (g_showEnemies.GetBool()) {
                idActor ent;
                int num = 0;
                for (ent = enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
                    gameLocal.Printf("enemy (%d)'%s'\n", ent.entityNumber, ent.name);
                    gameRenderWorld.DebugBounds(colorRed, ent.GetPhysics().GetBounds().Expand(2), ent.GetPhysics().GetOrigin());
                    num++;
                }
                gameLocal.Printf("%d: enemies\n", num);
            }
        }

        // save games
        @Override
        public void Save(idSaveGame savefile) {                    // archives object for save game file
            int i;

            savefile.WriteUsercmd(usercmd);
            playerView.Save(savefile);

            savefile.WriteBool(noclip);
            savefile.WriteBool(godmode);

            // don't save spawnAnglesSet, since we'll have to reset them after loading the savegame
            savefile.WriteAngles(spawnAngles);
            savefile.WriteAngles(viewAngles);
            savefile.WriteAngles(cmdAngles);

            savefile.WriteInt(buttonMask);
            savefile.WriteInt(oldButtons);
            savefile.WriteInt(oldFlags);

            savefile.WriteInt(lastHitTime);
            savefile.WriteInt(lastSndHitTime);
            savefile.WriteInt(lastSavingThrowTime);

            // idBoolFields don't need to be saved, just re-linked in Restore
            inventory.Save(savefile);
            weapon.Save(savefile);

            savefile.WriteUserInterface(hud, false);
            savefile.WriteUserInterface(objectiveSystem, false);
            savefile.WriteBool(objectiveSystemOpen);

            savefile.WriteInt(weapon_soulcube);
            savefile.WriteInt(weapon_pda);
            savefile.WriteInt(weapon_fists);

            savefile.WriteInt(heartRate);

            savefile.WriteFloat(heartInfo.GetStartTime());
            savefile.WriteFloat(heartInfo.GetDuration());
            savefile.WriteFloat(heartInfo.GetStartValue());
            savefile.WriteFloat(heartInfo.GetEndValue());

            savefile.WriteInt(lastHeartAdjust);
            savefile.WriteInt(lastHeartBeat);
            savefile.WriteInt(lastDmgTime);
            savefile.WriteInt(deathClearContentsTime);
            savefile.WriteBool(doingDeathSkin);
            savefile.WriteInt(lastArmorPulse);
            savefile.WriteFloat(stamina);
            savefile.WriteFloat(healthPool);
            savefile.WriteInt(nextHealthPulse);
            savefile.WriteBool(healthPulse);
            savefile.WriteInt(nextHealthTake);
            savefile.WriteBool(healthTake);

            savefile.WriteBool(hiddenWeapon);
            soulCubeProjectile.Save(savefile);

            savefile.WriteInt(spectator);
            savefile.WriteVec3(colorBar);
            savefile.WriteInt(colorBarIndex);
            savefile.WriteBool(scoreBoardOpen);
            savefile.WriteBool(forceScoreBoard);
            savefile.WriteBool(forceRespawn);
            savefile.WriteBool(spectating);
            savefile.WriteInt(lastSpectateTeleport);
            savefile.WriteBool(lastHitToggle);
            savefile.WriteBool(forcedReady);
            savefile.WriteBool(wantSpectate);
            savefile.WriteBool(weaponGone);
            savefile.WriteBool(useInitialSpawns);
            savefile.WriteInt(latchedTeam);
            savefile.WriteInt(tourneyRank);
            savefile.WriteInt(tourneyLine);

            teleportEntity.Save(savefile);
            savefile.WriteInt(teleportKiller);

            savefile.WriteInt(minRespawnTime);
            savefile.WriteInt(maxRespawnTime);

            savefile.WriteVec3(firstPersonViewOrigin);
            savefile.WriteMat3(firstPersonViewAxis);

            // don't bother saving dragEntity since it's a dev tool
            savefile.WriteJoint(hipJoint);
            savefile.WriteJoint(chestJoint);
            savefile.WriteJoint(headJoint);

            savefile.WriteStaticObject(physicsObj);

            savefile.WriteInt(aasLocation.Num());
            for (i = 0; i < aasLocation.Num(); i++) {
                savefile.WriteInt(aasLocation.oGet(i).areaNum);
                savefile.WriteVec3(aasLocation.oGet(i).pos);
            }

            savefile.WriteInt(bobFoot);
            savefile.WriteFloat(bobFrac);
            savefile.WriteFloat(bobfracsin);
            savefile.WriteInt(bobCycle);
            savefile.WriteFloat(xyspeed);
            savefile.WriteInt(stepUpTime);
            savefile.WriteFloat(stepUpDelta);
            savefile.WriteFloat(idealLegsYaw);
            savefile.WriteFloat(legsYaw);
            savefile.WriteBool(legsForward);
            savefile.WriteFloat(oldViewYaw);
            savefile.WriteAngles(viewBobAngles);
            savefile.WriteVec3(viewBob);
            savefile.WriteInt(landChange);
            savefile.WriteInt(landTime);

            savefile.WriteInt(currentWeapon);
            savefile.WriteInt(idealWeapon);
            savefile.WriteInt(previousWeapon);
            savefile.WriteInt(weaponSwitchTime);
            savefile.WriteBool(weaponEnabled);
            savefile.WriteBool(showWeaponViewModel);

            savefile.WriteSkin(skin);
            savefile.WriteSkin(powerUpSkin);
            savefile.WriteString(baseSkinName);

            savefile.WriteInt(numProjectilesFired);
            savefile.WriteInt(numProjectileHits);

            savefile.WriteBool(airless);
            savefile.WriteInt(airTics);
            savefile.WriteInt(lastAirDamage);

            savefile.WriteBool(gibDeath);
            savefile.WriteBool(gibsLaunched);
            savefile.WriteVec3(gibsDir);

            savefile.WriteFloat(zoomFov.GetStartTime());
            savefile.WriteFloat(zoomFov.GetDuration());
            savefile.WriteFloat(zoomFov.GetStartValue());
            savefile.WriteFloat(zoomFov.GetEndValue());

            savefile.WriteFloat(centerView.GetStartTime());
            savefile.WriteFloat(centerView.GetDuration());
            savefile.WriteFloat(centerView.GetStartValue());
            savefile.WriteFloat(centerView.GetEndValue());

            savefile.WriteBool(fxFov);

            savefile.WriteFloat(influenceFov);
            savefile.WriteInt(influenceActive);
            savefile.WriteFloat(influenceRadius);
            savefile.WriteObject(influenceEntity);
            savefile.WriteMaterial(influenceMaterial);
            savefile.WriteSkin(influenceSkin);

            savefile.WriteObject(privateCameraView);

            for (i = 0; i < NUM_LOGGED_VIEW_ANGLES; i++) {
                savefile.WriteAngles(loggedViewAngles[i]);
            }
            for (i = 0; i < NUM_LOGGED_ACCELS; i++) {
                savefile.WriteInt(loggedAccel[i].time);
                savefile.WriteVec3(loggedAccel[i].dir);
            }
            savefile.WriteInt(currentLoggedAccel);

            savefile.WriteObject(focusGUIent);
            // can't save focusUI
            savefile.WriteObject(focusCharacter);
            savefile.WriteInt(talkCursor);
            savefile.WriteInt(focusTime);
            savefile.WriteObject(focusVehicle);
            savefile.WriteUserInterface(cursor, false);

            savefile.WriteInt(oldMouseX);
            savefile.WriteInt(oldMouseY);

            savefile.WriteString(pdaAudio);
            savefile.WriteString(pdaVideo);
            savefile.WriteString(pdaVideoWave);

            savefile.WriteBool(tipUp);
            savefile.WriteBool(objectiveUp);

            savefile.WriteInt(lastDamageDef);
            savefile.WriteVec3(lastDamageDir);
            savefile.WriteInt(lastDamageLocation);
            savefile.WriteInt(smoothedFrame);
            savefile.WriteBool(smoothedOriginUpdated);
            savefile.WriteVec3(smoothedOrigin);
            savefile.WriteAngles(smoothedAngles);

            savefile.WriteBool(ready);
            savefile.WriteBool(respawning);
            savefile.WriteBool(leader);
            savefile.WriteInt(lastSpectateChange);
            savefile.WriteInt(lastTeleFX);

            savefile.WriteFloat(pm_stamina.GetFloat());

            if (hud != null) {
                hud.SetStateString("message", common.GetLanguageDict().GetString("#str_02916"));
                hud.HandleNamedEvent("Message");
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {                    // unarchives object from save game file
            int i;
            int[] num = {0};
            float[] set = {0};

            savefile.ReadUsercmd(usercmd);
            playerView.Restore(savefile);

            noclip = savefile.ReadBool();
            godmode = savefile.ReadBool();

            savefile.ReadAngles(spawnAngles);
            savefile.ReadAngles(viewAngles);
            savefile.ReadAngles(cmdAngles);

//	memset( usercmd.angles, 0, sizeof( usercmd.angles ) );
            Arrays.fill(usercmd.angles, (short) 0);//damn you type safety!!
            SetViewAngles(viewAngles);
            spawnAnglesSet = true;

            buttonMask = savefile.ReadInt();
            oldButtons = savefile.ReadInt();
            oldFlags = savefile.ReadInt();

            usercmd.flags = 0;
            oldFlags = 0;

            lastHitTime = savefile.ReadInt();
            lastSndHitTime = savefile.ReadInt();
            lastSavingThrowTime = savefile.ReadInt();

            // Re-link idBoolFields to the scriptObject, values will be restored in scriptObject's restore
            LinkScriptVariables();

            inventory.Restore(savefile);
            weapon.Restore(savefile);

            for (i = 0; i < inventory.emails.Num(); i++) {
                GetPDA().AddEmail(inventory.emails.oGet(i).toString());
            }

            savefile.ReadUserInterface(hud);
            savefile.ReadUserInterface(objectiveSystem);
            objectiveSystemOpen = savefile.ReadBool();

            weapon_soulcube = savefile.ReadInt();
            weapon_pda = savefile.ReadInt();
            weapon_fists = savefile.ReadInt();

            heartRate = savefile.ReadInt();

            savefile.ReadFloat(set);
            heartInfo.SetStartTime(set[0]);
            savefile.ReadFloat(set);
            heartInfo.SetDuration(set[0]);
            savefile.ReadFloat(set);
            heartInfo.SetStartValue(set[0]);
            savefile.ReadFloat(set);
            heartInfo.SetEndValue(set[0]);

            lastHeartAdjust = savefile.ReadInt();
            lastHeartBeat = savefile.ReadInt();
            lastDmgTime = savefile.ReadInt();
            deathClearContentsTime = savefile.ReadInt();
            doingDeathSkin = savefile.ReadBool();
            lastArmorPulse = savefile.ReadInt();
            stamina = savefile.ReadFloat();
            healthPool = savefile.ReadFloat();
            nextHealthPulse = savefile.ReadInt();
            healthPulse = savefile.ReadBool();
            nextHealthTake = savefile.ReadInt();
            healthTake = savefile.ReadBool();

            hiddenWeapon = savefile.ReadBool();
            soulCubeProjectile.Restore(savefile);

            spectator = savefile.ReadInt();
            savefile.ReadVec3(colorBar);
            colorBarIndex = savefile.ReadInt();
            scoreBoardOpen = savefile.ReadBool();
            forceScoreBoard = savefile.ReadBool();
            forceRespawn = savefile.ReadBool();
            spectating = savefile.ReadBool();
            lastSpectateTeleport = savefile.ReadInt();
            lastHitToggle = savefile.ReadBool();
            forcedReady = savefile.ReadBool();
            wantSpectate = savefile.ReadBool();
            weaponGone = savefile.ReadBool();
            useInitialSpawns = savefile.ReadBool();
            latchedTeam = savefile.ReadInt();
            tourneyRank = savefile.ReadInt();
            tourneyLine = savefile.ReadInt();

            teleportEntity.Restore(savefile);
            teleportKiller = savefile.ReadInt();

            minRespawnTime = savefile.ReadInt();
            maxRespawnTime = savefile.ReadInt();

            savefile.ReadVec3(firstPersonViewOrigin);
            savefile.ReadMat3(firstPersonViewAxis);

            // don't bother saving dragEntity since it's a dev tool
            dragEntity.Clear();

            hipJoint = savefile.ReadJoint();
            chestJoint = savefile.ReadJoint();
            headJoint = savefile.ReadJoint();

            savefile.ReadStaticObject(physicsObj);
            RestorePhysics(physicsObj);

            savefile.ReadInt(num);
            aasLocation.SetGranularity(1);
            aasLocation.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                aasLocation.oGet(i).areaNum = savefile.ReadInt();
                savefile.ReadVec3(aasLocation.oGet(i).pos);
            }

            bobFoot = savefile.ReadInt();
            bobFrac = savefile.ReadFloat();
            bobfracsin = savefile.ReadFloat();
            bobCycle = savefile.ReadInt();
            xyspeed = savefile.ReadFloat();
            stepUpTime = savefile.ReadInt();
            stepUpDelta = savefile.ReadFloat();
            idealLegsYaw = savefile.ReadFloat();
            legsYaw = savefile.ReadFloat();
            legsForward = savefile.ReadBool();
            oldViewYaw = savefile.ReadFloat();
            savefile.ReadAngles(viewBobAngles);
            savefile.ReadVec3(viewBob);
            landChange = savefile.ReadInt();
            landTime = savefile.ReadInt();

            currentWeapon = savefile.ReadInt();
            idealWeapon = savefile.ReadInt();
            previousWeapon = savefile.ReadInt();
            weaponSwitchTime = savefile.ReadInt();
            weaponEnabled = savefile.ReadBool();
            showWeaponViewModel = savefile.ReadBool();

            savefile.ReadSkin(skin);
            savefile.ReadSkin(powerUpSkin);
            savefile.ReadString(baseSkinName);

            numProjectilesFired = savefile.ReadInt();
            numProjectileHits = savefile.ReadInt();

            airless = savefile.ReadBool();
            airTics = savefile.ReadInt();
            lastAirDamage = savefile.ReadInt();

            gibDeath = savefile.ReadBool();
            gibsLaunched = savefile.ReadBool();
            savefile.ReadVec3(gibsDir);

            savefile.ReadFloat(set);
            zoomFov.SetStartTime(set[0]);
            savefile.ReadFloat(set);
            zoomFov.SetDuration(set[0]);
            savefile.ReadFloat(set);
            zoomFov.SetStartValue(set[0]);
            savefile.ReadFloat(set);
            zoomFov.SetEndValue(set[0]);

            savefile.ReadFloat(set);
            centerView.SetStartTime(set[0]);
            savefile.ReadFloat(set);
            centerView.SetDuration(set[0]);
            savefile.ReadFloat(set);
            centerView.SetStartValue(set[0]);
            savefile.ReadFloat(set);
            centerView.SetEndValue(set[0]);

            fxFov = savefile.ReadBool();

            influenceFov = savefile.ReadFloat();
            influenceActive = savefile.ReadInt();
            influenceRadius = savefile.ReadFloat();
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/influenceEntity);
            savefile.ReadMaterial(influenceMaterial);
            savefile.ReadSkin(influenceSkin);

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/privateCameraView);

            for (i = 0; i < NUM_LOGGED_VIEW_ANGLES; i++) {
                savefile.ReadAngles(loggedViewAngles[i]);
            }
            for (i = 0; i < NUM_LOGGED_ACCELS; i++) {
                loggedAccel[i].time = savefile.ReadInt();
                savefile.ReadVec3(loggedAccel[i].dir);
            }
            currentLoggedAccel = savefile.ReadInt();

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/focusGUIent);
            // can't save focusUI
            focusUI = null;
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/focusCharacter);
            talkCursor = savefile.ReadInt();
            focusTime = savefile.ReadInt();
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/focusVehicle);
            savefile.ReadUserInterface(cursor);

            oldMouseX = savefile.ReadInt();
            oldMouseY = savefile.ReadInt();

            savefile.ReadString(pdaAudio);
            savefile.ReadString(pdaVideo);
            savefile.ReadString(pdaVideoWave);

            tipUp = savefile.ReadBool();
            objectiveUp = savefile.ReadBool();

            lastDamageDef = savefile.ReadInt();
            savefile.ReadVec3(lastDamageDir);
            lastDamageLocation = savefile.ReadInt();
            smoothedFrame = savefile.ReadInt();
            smoothedOriginUpdated = savefile.ReadBool();
            savefile.ReadVec3(smoothedOrigin);
            savefile.ReadAngles(smoothedAngles);

            ready = savefile.ReadBool();
            respawning = savefile.ReadBool();
            leader = savefile.ReadBool();
            lastSpectateChange = savefile.ReadInt();
            lastTeleFX = savefile.ReadInt();

            // set the pm_ cvars
            idKeyValue kv;
            kv = spawnArgs.MatchPrefix("pm_", null);
            while (kv != null) {
                cvarSystem.SetCVarString(kv.GetKey().toString(), kv.GetValue().toString());
                kv = spawnArgs.MatchPrefix("pm_", kv);
            }

            savefile.ReadFloat(set);
            pm_stamina.SetFloat(set[0]);

            // create combat collision hull for exact collision detection
            SetCombatModel();
        }

        @Override
        public void Hide() {
            idWeapon weap;

            super.Hide();
            weap = weapon.GetEntity();
            if (weap != null) {
                weap.HideWorldModel();
            }
        }

        @Override
        public void Show() {
            idWeapon weap;

            super.Show();
            weap = weapon.GetEntity();
            if (weap != null) {
                weap.ShowWorldModel();
            }
        }

        @Override
        public void Init() {
            String[] value = {null};
            idKeyValue kv;

            noclip = false;
            godmode = false;

            oldButtons = 0;
            oldFlags = 0;

            currentWeapon = -1;
            idealWeapon = -1;
            previousWeapon = -1;
            weaponSwitchTime = 0;
            weaponEnabled = true;
            weapon_soulcube = SlotForWeapon("weapon_soulcube");
            weapon_pda = SlotForWeapon("weapon_pda");
            weapon_fists = SlotForWeapon("weapon_fists");
            showWeaponViewModel = GetUserInfo().GetBool("ui_showGun");

            lastDmgTime = 0;
            lastArmorPulse = -10000;
            lastHeartAdjust = 0;
            lastHeartBeat = 0;
            heartInfo.Init(0, 0, 0f, 0f);

            bobCycle = 0;
            bobFrac = 0;
            landChange = 0;
            landTime = 0;
            zoomFov.Init(0, 0, 0f, 0f);
            centerView.Init(0, 0, 0f, 0f);
            fxFov = false;

            influenceFov = 0;
            influenceActive = 0;
            influenceRadius = 0;
            influenceEntity = null;
            influenceMaterial = null;
            influenceSkin = null;

            currentLoggedAccel = 0;

            focusTime = 0;
            focusGUIent = null;
            focusUI = null;
            focusCharacter = null;
            talkCursor = 0;
            focusVehicle = null;

            // remove any damage effects
            playerView.ClearEffects();

            // damage values
            fl.takedamage = true;
            ClearPain();

            // restore persistent data
            RestorePersistantInfo();

            bobCycle = 0;
            stamina = 0;
            healthPool = 0;
            nextHealthPulse = 0;
            healthPulse = false;
            nextHealthTake = 0;
            healthTake = false;

            SetupWeaponEntity();
            currentWeapon = -1;
            previousWeapon = -1;

            heartRate = BASE_HEARTRATE;
            AdjustHeartRate(BASE_HEARTRATE, 0, 0, true);

            idealLegsYaw = 0;
            legsYaw = 0;
            legsForward = true;
            oldViewYaw = 0;

            // set the pm_ cvars
            if (!gameLocal.isMultiplayer || gameLocal.isServer) {
                kv = spawnArgs.MatchPrefix("pm_", null);
                while (kv != null) {
                    cvarSystem.SetCVarString(kv.GetKey().toString(), kv.GetValue().toString());
                    kv = spawnArgs.MatchPrefix("pm_", kv);
                }
            }

            // disable stamina on hell levels
            if (gameLocal.world != null && gameLocal.world.spawnArgs.GetBool("no_stamina")) {
                pm_stamina.SetFloat(0);
            }

            // stamina always initialized to maximum
            stamina = pm_stamina.GetFloat();

            // air always initialized to maximum too
            airTics = (int) pm_airTics.GetFloat();
            airless = false;

            gibDeath = false;
            gibsLaunched = false;
            gibsDir.Zero();

            // set the gravity
            physicsObj.SetGravity(gameLocal.GetGravity());

            // start out standing
            SetEyeHeight(pm_normalviewheight.GetFloat());

            stepUpTime = 0;
            stepUpDelta = 0;
            viewBobAngles.Zero();
            viewBob.Zero();

            value[0] = spawnArgs.GetString("model");
            if (value[0] != null && (!value[0].isEmpty())) {
                SetModel(value[0]);
            }

            if (cursor != null) {
                cursor.SetStateInt("talkcursor", 0);
                cursor.SetStateString("combatcursor", "1");
                cursor.SetStateString("itemcursor", "0");
                cursor.SetStateString("guicursor", "0");
            }

            if ((gameLocal.isMultiplayer || g_testDeath.GetBool()) && skin != null) {
                SetSkin(skin);
                renderEntity.shaderParms[6] = 0;
            } else if (spawnArgs.GetString("spawn_skin", null, value)) {
                skin.oSet(declManager.FindSkin(value[0]));
                SetSkin(skin);
                renderEntity.shaderParms[6] = 0;
            }

            value[0] = spawnArgs.GetString("bone_hips", "");
            hipJoint = animator.GetJointHandle(value[0]);
            if (hipJoint == INVALID_JOINT) {
                gameLocal.Error("Joint '%s' not found for 'bone_hips' on '%s'", value[0], name);
            }

            value[0] = spawnArgs.GetString("bone_chest", "");
            chestJoint = animator.GetJointHandle(value[0]);
            if (chestJoint == INVALID_JOINT) {
                gameLocal.Error("Joint '%s' not found for 'bone_chest' on '%s'", value[0], name);
            }

            value[0] = spawnArgs.GetString("bone_head", "");
            headJoint = animator.GetJointHandle(value[0]);
            if (headJoint == INVALID_JOINT) {
                gameLocal.Error("Joint '%s' not found for 'bone_head' on '%s'", value[0], name);
            }

            // initialize the script variables
            AI_FORWARD._(false);
            AI_BACKWARD._(false);
            AI_STRAFE_LEFT._(false);
            AI_STRAFE_RIGHT._(false);
            AI_ATTACK_HELD._(false);
            AI_WEAPON_FIRED._(false);
            AI_JUMP._(false);
            AI_DEAD._(false);
            AI_CROUCH._(false);
            AI_ONGROUND._(false);
            AI_ONLADDER._(false);
            AI_HARDLANDING._(false);
            AI_SOFTLANDING._(false);
            AI_RUN._(false);
            AI_PAIN._(false);
            AI_RELOAD._(false);
            AI_TELEPORT._(false);
            AI_TURN_LEFT._(false);
            AI_TURN_RIGHT._(false);

            // reset the script object
            ConstructScriptObject();

            // execute the script so the script object's constructor takes effect immediately
            scriptThread.Execute();

            forceScoreBoard = false;
            forcedReady = false;

            privateCameraView = null;

            lastSpectateChange = 0;
            lastTeleFX = -9999;

            hiddenWeapon = false;
            tipUp = false;
            objectiveUp = false;
            teleportEntity.oSet(null);
            teleportKiller = -1;
            leader = false;

            SetPrivateCameraView(null);

            lastSnapshotSequence = 0;

            MPAim = -1;
            lastMPAim = -1;
            lastMPAimTime = 0;
            MPAimFadeTime = 0;
            MPAimHighlight = false;

            if (hud != null) {
                hud.HandleNamedEvent("aim_clear");
            }

            cvarSystem.SetCVarBool("ui_chat", false);
        }

        public void PrepareForRestart() {
            ClearPowerUps();
            Spectate(true);
            forceRespawn = true;

            // we will be restarting program, clear the client entities from program-related things first
            ShutdownThreads();

            // the sound world is going to be cleared, don't keep references to emitters
            FreeSoundEmitter(false);
        }

        @Override
        public void Restart() {
            super.Restart();

            // client needs to setup the animation script object again
            if (gameLocal.isClient) {
                Init();
            } else {
                // choose a random spot and prepare the point of view in case player is left spectating
                assert (spectating);
                SpawnFromSpawnSpot();
            }

            useInitialSpawns = true;
            UpdateSkinSetup(true);
        }

        /*
         ==============
         idPlayer::LinkScriptVariables

         set up conditions for animation
         ==============
         */
        public void LinkScriptVariables() {
            AI_FORWARD.LinkTo(scriptObject, "AI_FORWARD");
            AI_BACKWARD.LinkTo(scriptObject, "AI_BACKWARD");
            AI_STRAFE_LEFT.LinkTo(scriptObject, "AI_STRAFE_LEFT");
            AI_STRAFE_RIGHT.LinkTo(scriptObject, "AI_STRAFE_RIGHT");
            AI_ATTACK_HELD.LinkTo(scriptObject, "AI_ATTACK_HELD");
            AI_WEAPON_FIRED.LinkTo(scriptObject, "AI_WEAPON_FIRED");
            AI_JUMP.LinkTo(scriptObject, "AI_JUMP");
            AI_DEAD.LinkTo(scriptObject, "AI_DEAD");
            AI_CROUCH.LinkTo(scriptObject, "AI_CROUCH");
            AI_ONGROUND.LinkTo(scriptObject, "AI_ONGROUND");
            AI_ONLADDER.LinkTo(scriptObject, "AI_ONLADDER");
            AI_HARDLANDING.LinkTo(scriptObject, "AI_HARDLANDING");
            AI_SOFTLANDING.LinkTo(scriptObject, "AI_SOFTLANDING");
            AI_RUN.LinkTo(scriptObject, "AI_RUN");
            AI_PAIN.LinkTo(scriptObject, "AI_PAIN");
            AI_RELOAD.LinkTo(scriptObject, "AI_RELOAD");
            AI_TELEPORT.LinkTo(scriptObject, "AI_TELEPORT");
            AI_TURN_LEFT.LinkTo(scriptObject, "AI_TURN_LEFT");
            AI_TURN_RIGHT.LinkTo(scriptObject, "AI_TURN_RIGHT");
        }

        public void SetupWeaponEntity() {
            int w;
            String weap;

            if (weapon.GetEntity() != null) {
                // get rid of old weapon
                weapon.GetEntity().Clear();
                currentWeapon = -1;
            } else if (!gameLocal.isClient) {
                weapon.oSet((idWeapon) gameLocal.SpawnEntityType(idWeapon.class, null));
                weapon.GetEntity().SetOwner(this);
                currentWeapon = -1;
            }

            for (w = 0; w < MAX_WEAPONS; w++) {
                weap = spawnArgs.GetString(va("def_weapon%d", w));
                if (weap != null && !weap.isEmpty()) {
                    idWeapon.CacheWeapon(weap);
                }
            }
        }


        /*
         ===========
         idPlayer::SelectInitialSpawnPoint

         Try to find a spawn point marked 'initial', otherwise
         use normal spawn selection.
         ============
         */
        public void SelectInitialSpawnPoint(idVec3 origin, idAngles angles) {
            idEntity spot;
            idStr skin = new idStr();

            spot = gameLocal.SelectInitialSpawnPoint(this);

            // set the player skin from the spawn location
            if (spot.spawnArgs.GetString("skin", null, skin)) {
                spawnArgs.Set("spawn_skin", skin);
            }

            // activate the spawn locations targets
            spot.PostEventMS(EV_ActivateTargets, 0, this);

            origin.oSet(spot.GetPhysics().GetOrigin());
            origin.oPluSet(2, 4.0f + CM_BOX_EPSILON);        // move up to make sure the player is at least an epsilon above the floor
            angles.oSet(spot.GetPhysics().GetAxis().ToAngles());
        }

        /*
         ===========
         idPlayer::SpawnFromSpawnSpot

         Chooses a spawn location and spawns the player
         ============
         */
        public void SpawnFromSpawnSpot() {
            idVec3 spawn_origin = new idVec3();
            idAngles spawn_angles = new idAngles();

            SelectInitialSpawnPoint(spawn_origin, spawn_angles);
            SpawnToPoint(spawn_origin, spawn_angles);
        }

        /*
         ===========
         idPlayer::SpawnToPoint

         Called every time a client is placed fresh in the world:
         after the first ClientBegin, and after each respawn
         Initializes all non-persistant parts of playerState

         when called here with spectating set to true, just place yourself and init
         ============
         */
        public void SpawnToPoint(final idVec3 spawn_origin, final idAngles spawn_angles) {
            idVec3 spec_origin;

            assert (!gameLocal.isClient);

            respawning = true;

            Init();

            fl.noknockback = false;

            // stop any ragdolls being used
            StopRagdoll();

            // set back the player physics
            SetPhysics(physicsObj);

            physicsObj.SetClipModelAxis();
            physicsObj.EnableClip();

            if (!spectating) {
                SetCombatContents(true);
            }

            physicsObj.SetLinearVelocity(getVec3_origin());

            // setup our initial view
            if (!spectating) {
                SetOrigin(spawn_origin);
            } else {
                spec_origin = spawn_origin;
                spec_origin.oPluSet(2, pm_normalheight.GetFloat());
                spec_origin.oPluSet(2, SPECTATE_RAISE);
                SetOrigin(spec_origin);
            }

            // if this is the first spawn of the map, we don't have a usercmd yet,
            // so the delta angles won't be correct.  This will be fixed on the first think.
            viewAngles = getAng_zero();
            SetDeltaViewAngles(getAng_zero());
            SetViewAngles(spawn_angles);
            spawnAngles = spawn_angles;
            spawnAnglesSet = false;

            legsForward = true;
            legsYaw = 0;
            idealLegsYaw = 0;
            oldViewYaw = viewAngles.yaw;

            if (spectating) {
                Hide();
            } else {
                Show();
            }

            if (gameLocal.isMultiplayer) {
                if (!spectating) {
                    // we may be called twice in a row in some situations. avoid a double fx and 'fly to the roof'
                    if (lastTeleFX < gameLocal.time - 1000) {
                        idEntityFx.StartFx(spawnArgs.GetString("fx_spawn"), spawn_origin, null, this, true);
                        lastTeleFX = gameLocal.time;
                    }
                }
                AI_TELEPORT._(true);
            } else {
                AI_TELEPORT._(false);
            }

            // kill anything at the new position
            if (!spectating) {
                physicsObj.SetClipMask(MASK_PLAYERSOLID); // the clip mask is usually maintained in Move(), but KillBox requires it
                gameLocal.KillBox(this);
            }

            // don't allow full run speed for a bit
            physicsObj.SetKnockBack(100);

            // set our respawn time and buttons so that if we're killed we don't respawn immediately
            minRespawnTime = gameLocal.time;
            maxRespawnTime = gameLocal.time;
            if (!spectating) {
                forceRespawn = false;
            }

            privateCameraView = null;

            BecomeActive(TH_THINK);

            // run a client frame to drop exactly to the floor,
            // initialize animations and other things
            Think();

            respawning = false;
            lastManOver = false;
            lastManPlayAgain = false;
            isTelefragged = false;
        }

        public void SetClipModel() {
            idBounds bounds = new idBounds();

            if (spectating) {
                bounds = new idBounds(getVec3_origin()).Expand(pm_spectatebbox.GetFloat() * 0.5f);
            } else {
                bounds.oGet(0).Set(-pm_bboxwidth.GetFloat() * 0.5f, -pm_bboxwidth.GetFloat() * 0.5f, 0);
                bounds.oGet(1).Set(pm_bboxwidth.GetFloat() * 0.5f, pm_bboxwidth.GetFloat() * 0.5f, pm_normalheight.GetFloat());
            }
            // the origin of the clip model needs to be set before calling SetClipModel
            // otherwise our physics object's current origin value gets reset to 0
            idClipModel newClip;
            if (pm_usecylinder.GetBool()) {
                newClip = new idClipModel(new idTraceModel(bounds, 8));
                newClip.Translate(physicsObj.PlayerGetOrigin());
                physicsObj.SetClipModel(newClip, 1.0f);
            } else {
                newClip = new idClipModel(new idTraceModel(bounds));
                newClip.Translate(physicsObj.PlayerGetOrigin());
                physicsObj.SetClipModel(newClip, 1.0f);
            }
        }

        /*
         ===============
         idPlayer::SavePersistantInfo

         Saves any inventory and player stats when changing levels.
         ===============
         */
        public void SavePersistantInfo() {
            idDict playerInfo = gameLocal.persistentPlayerInfo[entityNumber];

            playerInfo.Clear();
            inventory.GetPersistantData(playerInfo);
            playerInfo.SetInt("health", health);
            playerInfo.SetInt("current_weapon", currentWeapon);
        }

        /*
         ===============
         idPlayer::RestorePersistantInfo

         Restores any inventory and player stats when changing levels.
         ===============
         */
        public void RestorePersistantInfo() {
            if (gameLocal.isMultiplayer) {
                gameLocal.persistentPlayerInfo[entityNumber].Clear();
            }

            spawnArgs.Copy(gameLocal.persistentPlayerInfo[entityNumber]);

            inventory.RestoreInventory(this, spawnArgs);
            health = spawnArgs.GetInt("health", "100");
            if (!gameLocal.isClient) {
                idealWeapon = spawnArgs.GetInt("current_weapon", "1");
            }
        }

        public void SetLevelTrigger(final String levelName, final String triggerName) {
            if (levelName != null && !levelName.isEmpty() && triggerName != null && !triggerName.isEmpty()) {
                idLevelTriggerInfo lti = new idLevelTriggerInfo();
                lti.levelName.oSet(levelName);
                lti.triggerName.oSet(triggerName);
                inventory.levelTriggers.Append(lti);
            }
        }

        public boolean UserInfoChanged(boolean canModify) {
            idDict userInfo;
            boolean modifiedInfo;
            boolean spec;
            boolean newready;

            userInfo = GetUserInfo();
            showWeaponViewModel = userInfo.GetBool("ui_showGun");

            if (!gameLocal.isMultiplayer) {
                return false;
            }

            modifiedInfo = false;

            spec = (idStr.Icmp(userInfo.GetString("ui_spectate"), "Spectate") == 0);
            if (gameLocal.serverInfo.GetBool("si_spectators")) {
                // never let spectators go back to game while sudden death is on
                if (canModify && gameLocal.mpGame.GetGameState() == SUDDENDEATH && !spec && wantSpectate == true) {
                    userInfo.Set("ui_spectate", "Spectate");
                    modifiedInfo |= true;
                } else {
                    if (spec != wantSpectate && !spec) {
                        // returning from spectate, set forceRespawn so we don't get stuck in spectate forever
                        forceRespawn = true;
                    }
                    wantSpectate = spec;
                }
            } else {
                if (canModify && spec) {
                    userInfo.Set("ui_spectate", "Play");
                    modifiedInfo |= true;
                } else if (spectating) {
                    // allow player to leaving spectator mode if they were in it when si_spectators got turned off
                    forceRespawn = true;
                }
                wantSpectate = false;
            }

            newready = (idStr.Icmp(userInfo.GetString("ui_ready"), "Ready") == 0);
            if (ready != newready && gameLocal.mpGame.GetGameState() == WARMUP && !wantSpectate) {
                gameLocal.mpGame.AddChatLine(common.GetLanguageDict().GetString("#str_07180"), userInfo.GetString("ui_name"), newready ? common.GetLanguageDict().GetString("#str_04300") : common.GetLanguageDict().GetString("#str_04301"));
            }
            ready = newready;
            team = (idStr.Icmp(userInfo.GetString("ui_team"), "Blue") & 1);//== 0);
            // server maintains TDM balance
            if (canModify && gameLocal.gameType == GAME_TDM && !gameLocal.mpGame.IsInGame(entityNumber) && g_balanceTDM.GetBool()) {
                modifiedInfo |= BalanceTDM();
            }
            UpdateSkinSetup(false);

            isChatting = userInfo.GetBool("ui_chat", "0");
            if (canModify && isChatting && AI_DEAD._()) {
                // if dead, always force chat icon off.
                isChatting = false;
                userInfo.SetBool("ui_chat", false);
                modifiedInfo |= true;
            }

            return modifiedInfo;
        }

        public idDict GetUserInfo() {
            return gameLocal.userInfo[entityNumber];
        }

        public boolean BalanceTDM() {
            int i, balanceTeam;
            int[] teamCount = new int[2];
            idEntity ent;

            teamCount[ 0] = teamCount[ 1] = 0;
            for (i = 0; i < gameLocal.numClients; i++) {
                ent = gameLocal.entities[i];
                if (ent != null && ent.IsType(idPlayer.class)) {
                    teamCount[((idPlayer) ent).team]++;
                }
            }
            balanceTeam = -1;
            if (teamCount[ 0] < teamCount[ 1]) {
                balanceTeam = 0;
            } else if (teamCount[ 0] > teamCount[ 1]) {
                balanceTeam = 1;
            }
            if (balanceTeam != -1 && team != balanceTeam) {
                common.DPrintf("team balance: forcing player %d to %s team\n", entityNumber, itob(balanceTeam) ? "blue" : "red");
                team = balanceTeam;
                GetUserInfo().Set("ui_team", itob(team) ? "Blue" : "Red");
                return true;
            }
            return false;
        }

        public void CacheWeapons() {
            String weap;
            int w;

            // check if we have any weapons
            if (0 == inventory.weapons) {
                return;
            }

            for (w = 0; w < MAX_WEAPONS; w++) {
                if ((inventory.weapons & (1 << w)) != 0) {
                    weap = spawnArgs.GetString(va("def_weapon%d", w));
                    if (!"".equals(weap)) {
                        idWeapon.CacheWeapon(weap);
                    } else {
                        inventory.weapons &= ~(1 << w);
                    }
                }
            }
        }

        public void EnterCinematic() {
            Hide();
            StopAudioLog();
            StopSound(etoi(SND_CHANNEL_PDA), false);
            if (hud != null) {
                hud.HandleNamedEvent("radioChatterDown");
            }

            physicsObj.SetLinearVelocity(getVec3_origin());

            SetState("EnterCinematic");
            UpdateScript();

            if (weaponEnabled && weapon.GetEntity() != null) {
                weapon.GetEntity().EnterCinematic();
            }

            AI_FORWARD._(false);
            AI_BACKWARD._(false);
            AI_STRAFE_LEFT._(false);
            AI_STRAFE_RIGHT._(false);
            AI_RUN._(false);
            AI_ATTACK_HELD._(false);
            AI_WEAPON_FIRED._(false);
            AI_JUMP._(false);
            AI_CROUCH._(false);
            AI_ONGROUND._(true);
            AI_ONLADDER._(false);
            AI_DEAD._(health <= 0);
            AI_RUN._(false);
            AI_PAIN._(false);
            AI_HARDLANDING._(false);
            AI_SOFTLANDING._(false);
            AI_RELOAD._(false);
            AI_TELEPORT._(false);
            AI_TURN_LEFT._(false);
            AI_TURN_RIGHT._(false);
        }

        public void ExitCinematic() {
            Show();

            if (weaponEnabled && weapon.GetEntity() != null) {
                weapon.GetEntity().ExitCinematic();
            }

            SetState("ExitCinematic");
            UpdateScript();
        }

        public boolean HandleESC() {
            if (gameLocal.inCinematic) {
                return SkipCinematic();
            }

            if (objectiveSystemOpen) {
                TogglePDA();
                return true;
            }

            return false;
        }

        public boolean SkipCinematic() {
            StartSound("snd_skipcinematic", SND_CHANNEL_ANY, 0, false, null);
            return gameLocal.SkipCinematic();
        }

        public void UpdateConditions() {
            idVec3 velocity;
            float fallspeed;
            float forwardspeed;
            float sidespeed;

            // minus the push velocity to avoid playing the walking animation and sounds when riding a mover
            velocity = physicsObj.GetLinearVelocity().oMinus(physicsObj.GetPushedLinearVelocity());
            fallspeed = velocity.oMultiply(physicsObj.GetGravityNormal());

            if (influenceActive != 0) {
                AI_FORWARD._(false);
                AI_BACKWARD._(false);
                AI_STRAFE_LEFT._(false);
                AI_STRAFE_RIGHT._(false);
            } else if (gameLocal.time - lastDmgTime < 500) {
                forwardspeed = velocity.oMultiply(viewAxis.oGet(0));
                sidespeed = velocity.oMultiply(viewAxis.oGet(1));
                AI_FORWARD._(AI_ONGROUND._() && (forwardspeed > 20.01f));
                AI_BACKWARD._(AI_ONGROUND._() && (forwardspeed < -20.01f));
                AI_STRAFE_LEFT._(AI_ONGROUND._() && (sidespeed > 20.01f));
                AI_STRAFE_RIGHT._(AI_ONGROUND._() && (sidespeed < -20.01f));
            } else if (xyspeed > MIN_BOB_SPEED) {
                AI_FORWARD._(AI_ONGROUND._() && (usercmd.forwardmove > 0));
                AI_BACKWARD._(AI_ONGROUND._() && (usercmd.forwardmove < 0));
                AI_STRAFE_LEFT._(AI_ONGROUND._() && (usercmd.rightmove < 0));
                AI_STRAFE_RIGHT._(AI_ONGROUND._() && (usercmd.rightmove > 0));
            } else {
                AI_FORWARD._(false);
                AI_BACKWARD._(false);
                AI_STRAFE_LEFT._(false);
                AI_STRAFE_RIGHT._(false);
            }

            AI_RUN._(((usercmd.buttons & BUTTON_RUN) != 0) && ((NOT(pm_stamina.GetFloat())) || (stamina > pm_staminathreshold.GetFloat())));
            AI_DEAD._(health <= 0);
        }

        public void SetViewAngles(final idAngles angles) {
            UpdateDeltaViewAngles(angles);
            viewAngles = angles;
        }

        // delta view angles to allow movers to rotate the view of the player
        public void UpdateDeltaViewAngles(final idAngles angles) {
            // set the delta angle
            idAngles delta = new idAngles();
            for (int i = 0; i < 3; i++) {
                delta.oSet(i, (float) (angles.oGet(i) - SHORT2ANGLE(usercmd.angles[i])));
            }
            SetDeltaViewAngles(delta);
        }

        @Override
        public boolean Collide(final trace_s collision, final idVec3 velocity) {
            idEntity other;

            if (gameLocal.isClient) {
                return false;
            }

            other = gameLocal.entities[collision.c.entityNum];
            if (other != null) {
                other.Signal(SIG_TOUCH);
                if (!spectating) {
                    if (other.RespondsTo(EV_Touch)) {
                        other.ProcessEvent(EV_Touch, this, collision);
                    }
                } else {
                    if (other.RespondsTo(EV_SpectatorTouch)) {
                        other.ProcessEvent(EV_SpectatorTouch, this, collision);
                    }
                }
            }
            return false;
        }

        @Override
        public void GetAASLocation(idAAS aas, idVec3 pos, int[] areaNum) {
            int i;

            if (aas != null) {
                for (i = 0; i < aasLocation.Num(); i++) {
                    if (aas == gameLocal.GetAAS(i)) {
                        areaNum[0] = aasLocation.oGet(i).areaNum;
                        pos = aasLocation.oGet(i).pos;
                        return;
                    }
                }
            }

            areaNum[0] = 0;
            pos = physicsObj.GetOrigin();
        }

        /*
         =====================
         idPlayer::GetAIAimTargets

         Returns positions for the AI to aim at.
         =====================
         */
        @Override
        public void GetAIAimTargets(final idVec3 lastSightPos, idVec3 headPos, idVec3 chestPos) {
            idVec3 offset = new idVec3();
            idMat3 axis = new idMat3();
            idVec3 origin;

            origin = lastSightPos.oMinus(physicsObj.GetOrigin());

            GetJointWorldTransform(chestJoint, gameLocal.time, offset, axis);
            headPos = offset.oPlus(origin);

            GetJointWorldTransform(headJoint, gameLocal.time, offset, axis);
            chestPos = offset.oPlus(origin);
        }

        /*
         ================
         idPlayer::DamageFeedback

         callback function for when another entity received damage from this entity.  damage can be adjusted and returned to the caller.
         ================
         */
        @Override
        public void DamageFeedback(idEntity victim, idEntity inflictor, int[] damage) {
            assert (!gameLocal.isClient);
            damage[0] *= PowerUpModifier(BERSERK);
            if (damage[0] != 0 && (victim != this) && victim.IsType(idActor.class)) {
                SetLastHitTime(gameLocal.time);
            }
        }

        /*
         =================
         idPlayer::CalcDamagePoints

         Calculates how many health and armor points will be inflicted, but
         doesn't actually do anything with them.  This is used to tell when an attack
         would have killed the player, possibly allowing a "saving throw"
         =================
         */
        public void CalcDamagePoints(idEntity inflictor, idEntity attacker, final idDict damageDef,
                final float damageScale, final int location, int[] health, int[] armor) {
            int[] damage = {0};
            int armorSave;

            damageDef.GetInt("damage", "20", damage);
            damage[0] = GetDamageForLocation(damage[0], location);

            idPlayer player = attacker.IsType(idPlayer.class) ? (idPlayer) attacker : null;
            if (!gameLocal.isMultiplayer) {
                if (inflictor != gameLocal.world) {
                    switch (g_skill.GetInteger()) {
                        case 0:
                            damage[0] *= 0.80f;
                            if (damage[0] < 1) {
                                damage[0] = 1;
                            }
                            break;
                        case 2:
                            damage[0] *= 1.70f;
                            break;
                        case 3:
                            damage[0] *= 3.5f;
                            break;
                        default:
                            break;
                    }
                }
            }

            damage[0] *= damageScale;

            // always give half damage if hurting self
            if (attacker.equals(this)) {
                if (gameLocal.isMultiplayer) {
                    // only do this in mp so single player plasma and rocket splash is very dangerous in close quarters
                    damage[0] *= damageDef.GetFloat("selfDamageScale", "0.5");
                } else {
                    damage[0] *= damageDef.GetFloat("selfDamageScale", "1");
                }
            }

            // check for completely getting out of the damage
            if (!damageDef.GetBool("noGod")) {
                // check for godmode
                if (godmode) {
                    damage[0] = 0;
                }
            }

            // inform the attacker that they hit someone
            attacker.DamageFeedback(this, inflictor, damage);

            // save some from armor
            if (!damageDef.GetBool("noArmor")) {
                float armor_protection;

                armor_protection = gameLocal.isMultiplayer ? g_armorProtectionMP.GetFloat() : g_armorProtection.GetFloat();

                armorSave = (int) ceil(damage[0] * armor_protection);
                if (armorSave >= inventory.armor) {
                    armorSave = inventory.armor;
                }

                if (0 == damage[0]) {
                    armorSave = 0;
                } else if (armorSave >= damage[0]) {
                    armorSave = damage[0] - 1;
                    damage[0] = 1;
                } else {
                    damage[0] -= armorSave;
                }
            } else {
                armorSave = 0;
            }

            // check for team damage
            if (gameLocal.gameType == GAME_TDM
                    && !gameLocal.serverInfo.GetBool("si_teamDamage")
                    && !damageDef.GetBool("noTeam")
                    && player != null
                    && !player.equals(this)// you get self damage no matter what
                    && player.team == team) {
                damage[0] = 0;
            }

            health[0] = damage[0];
            armor[0] = armorSave;
        }

        /*
         ============
         Damage

         this		entity that is being damaged
         inflictor	entity that is causing the damage
         attacker	entity that caused the inflictor to damage targ
         example: this=monster, inflictor=rocket, attacker=player

         dir			direction of the attack for knockback in global space

         damageDef	an idDict with all the options for damage effects

         inflictor, attacker, dir, and point can be NULL for environmental effects
         ============
         */
        @Override
        public void Damage(idEntity inflictor, idEntity attacker, final idVec3 dir, final String damageDefName, final float damageScale, final int location) {
            idVec3 kick;
            int[] damage = {0};
            int[] armorSave = {0};
            int[] knockback = {0};
            idVec3 damage_from;
            idVec3 localDamageVector = new idVec3();
            float[] attackerPushScale = {0};

            // damage is only processed on server
            if (gameLocal.isClient) {
                return;
            }

            if (!fl.takedamage || noclip || spectating || gameLocal.inCinematic) {
                return;
            }

            if (NOT(inflictor)) {
                inflictor = gameLocal.world;
            }
            if (NOT(attacker)) {
                attacker = gameLocal.world;
            }

            if (attacker.IsType(idAI.class)) {
                if (PowerUpActive(BERSERK)) {
                    return;
                }
                // don't take damage from monsters during influences
                if (influenceActive != 0) {
                    return;
                }
            }

            final idDeclEntityDef damageDef = gameLocal.FindEntityDef(damageDefName, false);
            if (null == damageDef) {
                gameLocal.Warning("Unknown damageDef '%s'", damageDefName);
                return;
            }

            if (damageDef.dict.GetBool("ignore_player")) {
                return;
            }

            CalcDamagePoints(inflictor, attacker, damageDef.dict, damageScale, location, damage, armorSave);

            // determine knockback
            damageDef.dict.GetInt("knockback", "20", knockback);

            if (knockback[0] != 0 && !fl.noknockback) {
                if (attacker == this) {
                    damageDef.dict.GetFloat("attackerPushScale", "0", attackerPushScale);
                } else {
                    attackerPushScale[0] = 1.0f;
                }

                kick = dir;
                kick.Normalize();
                kick.oMulSet(g_knockback.GetFloat() * knockback[0] * attackerPushScale[0] / 200);
                physicsObj.SetLinearVelocity(physicsObj.GetLinearVelocity().oPlus(kick));

                // set the timer so that the player can't cancel out the movement immediately
                physicsObj.SetKnockBack(idMath.ClampInt(50, 200, knockback[0] * 2));
            }

            // give feedback on the player view and audibly when armor is helping
            if (armorSave[0] != 0) {
                inventory.armor -= armorSave[0];

                if (gameLocal.time > lastArmorPulse + 200) {
                    StartSound("snd_hitArmor", SND_CHANNEL_ITEM, 0, false, null);
                }
                lastArmorPulse = gameLocal.time;
            }

            if (damageDef.dict.GetBool("burn")) {
                StartSound("snd_burn", SND_CHANNEL_BODY3, 0, false, null);
            } else if (damageDef.dict.GetBool("no_air")) {
                if (0 == armorSave[0] && health > 0) {
                    StartSound("snd_airGasp", SND_CHANNEL_ITEM, 0, false, null);
                }
            }

            if (g_debugDamage.GetInteger() != 0) {
                gameLocal.Printf("client:%d health:%d damage:%d armor:%d\n",
                        entityNumber, health, damage[0], armorSave[0]);
            }

            // move the world direction vector to local coordinates
            damage_from = dir;
            damage_from.Normalize();

            viewAxis.ProjectVector(damage_from, localDamageVector);

            // add to the damage inflicted on a player this frame
            // the total will be turned into screen blends and view angle kicks
            // at the end of the frame
            if (health > 0) {
                playerView.DamageImpulse(localDamageVector, damageDef.dict);
            }

            // do the damage
            if (damage[0] > 0) {

                if (!gameLocal.isMultiplayer) {
                    float scale = g_damageScale.GetFloat();
                    if (g_useDynamicProtection.GetBool() && g_skill.GetInteger() < 2) {
                        if (gameLocal.time > lastDmgTime + 500 && scale > 0.25f) {
                            scale -= 0.05f;
                            g_damageScale.SetFloat(scale);
                        }
                    }

                    if (scale > 0) {
                        damage[0] *= scale;
                    }
                }

                if (damage[0] < 1) {
                    damage[0] = 1;
                }

                int oldHealth = health;
                health -= damage[0];

                if (health <= 0) {

                    if (health < -999) {
                        health = -999;
                    }

                    isTelefragged = damageDef.dict.GetBool("telefrag");

                    lastDmgTime = gameLocal.time;
                    Killed(inflictor, attacker, damage[0], dir, location);

                } else {
                    // force a blink
                    blink_time = 0;

                    // let the anim script know we took damage
                    AI_PAIN._(Pain(inflictor, attacker, damage[0], dir, location));
                    if (!g_testDeath.GetBool()) {
                        lastDmgTime = gameLocal.time;
                    }
                }
            } else {
                // don't accumulate impulses
                if (af.IsLoaded()) {
                    // clear impacts
                    af.Rest();

                    // physics is turned off by calling af.Rest()
                    BecomeActive(TH_PHYSICS);
                }
            }

            lastDamageDef = damageDef.Index();
            lastDamageDir = damage_from;
            lastDamageLocation = location;
        }

        // use exitEntityNum to specify a teleport with private camera view and delayed exit
        @Override
        public void Teleport(final idVec3 origin, final idAngles angles, idEntity destination) {
            idVec3 org = new idVec3();

            if (weapon.GetEntity() != null) {
                weapon.GetEntity().LowerWeapon();
            }

            SetOrigin(origin.oPlus(new idVec3(0, 0, CM_CLIP_EPSILON)));
            if (!gameLocal.isMultiplayer && GetFloorPos(16.0f, org)) {
                SetOrigin(org);
            }

            // clear the ik heights so model doesn't appear in the wrong place
            walkIK.EnableAll();

            GetPhysics().SetLinearVelocity(getVec3_origin());

            SetViewAngles(angles);

            legsYaw = 0;
            idealLegsYaw = 0;
            oldViewYaw = viewAngles.yaw;

            if (gameLocal.isMultiplayer) {
                playerView.Flash(colorWhite, 140);
            }

            UpdateVisuals();

            teleportEntity.oSet(destination);

            if (!gameLocal.isClient && !noclip) {
                if (gameLocal.isMultiplayer) {
                    // kill anything at the new position or mark for kill depending on immediate or delayed teleport
                    gameLocal.KillBox(this, destination != null);
                } else {
                    // kill anything at the new position
                    gameLocal.KillBox(this, true);
                }
            }
        }

        public void Kill(boolean delayRespawn, boolean nodamage) {
            if (spectating) {
                SpectateFreeFly(false);
            } else if (health > 0) {
                godmode = false;
                if (nodamage) {
                    ServerSpectate(true);
                    forceRespawn = true;
                } else {
                    Damage(this, this, getVec3_origin(), "damage_suicide", 1.0f, INVALID_JOINT);
                    if (delayRespawn) {
                        forceRespawn = false;
                        float delay = spawnArgs.GetFloat("respawn_delay");
                        minRespawnTime = (int) (gameLocal.time + SEC2MS(delay));
                        maxRespawnTime = minRespawnTime + MAX_RESPAWN_TIME;
                    }
                }
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            float delay;

            assert (!gameLocal.isClient);

            // stop taking knockback once dead
            fl.noknockback = true;
            if (health < -999) {
                health = -999;
            }

            if (AI_DEAD._()) {
                AI_PAIN._(true);
                return;
            }

            heartInfo.Init(0, 0, 0f, 0f + BASE_HEARTRATE);
            AdjustHeartRate(DEAD_HEARTRATE, 10, 0, true);

            if (!g_testDeath.GetBool()) {
                playerView.Fade(colorBlack, 12000);
            }

            AI_DEAD._(true);
            SetAnimState(ANIMCHANNEL_LEGS, "Legs_Death", 4);
            SetAnimState(ANIMCHANNEL_TORSO, "Torso_Death", 4);
            SetWaitState("");

            animator.ClearAllJoints();

            if (StartRagdoll()) {
                pm_modelView.SetInteger(0);
                minRespawnTime = gameLocal.time + RAGDOLL_DEATH_TIME;
                maxRespawnTime = minRespawnTime + MAX_RESPAWN_TIME;
            } else {
                // don't allow respawn until the death anim is done
                // g_forcerespawn may force spawning at some later time
                delay = spawnArgs.GetFloat("respawn_delay");
                minRespawnTime = (int) (gameLocal.time + SEC2MS(delay));
                maxRespawnTime = minRespawnTime + MAX_RESPAWN_TIME;
            }

            physicsObj.SetMovementType(PM_DEAD);
            StartSound("snd_death", SND_CHANNEL_VOICE, 0, false, null);
            StopSound(etoi(SND_CHANNEL_BODY2), false);

            fl.takedamage = true;		// can still be gibbed

            // get rid of weapon
            weapon.GetEntity().OwnerDied();

            // drop the weapon as an item
            DropWeapon(true);

            if (!g_testDeath.GetBool()) {
                LookAtKiller(inflictor, attacker);
            }

            if (gameLocal.isMultiplayer || g_testDeath.GetBool()) {
                idPlayer killer = null;
                // no gibbing in MP. Event_Gib will early out in MP
                if (attacker.IsType(idPlayer.class)) {
                    killer = (idPlayer) attacker;
                    if (health < -20 || killer.PowerUpActive(BERSERK)) {
                        gibDeath = true;
                        gibsDir = dir;
                        gibsLaunched = false;
                    }
                }
                gameLocal.mpGame.PlayerDeath(this, killer, isTelefragged);
            } else {
                physicsObj.SetContents(CONTENTS_CORPSE | CONTENTS_MONSTERCLIP);
            }

            ClearPowerUps();

            UpdateVisuals();

            isChatting = false;
        }

        public void StartFxOnBone(final String fx, final String bone) {
            idVec3 offset = new idVec3();
            idMat3 axis = new idMat3();
            int/*jointHandle_t*/ jointHandle = GetAnimator().GetJointHandle(bone);

            if (jointHandle == INVALID_JOINT) {
                gameLocal.Printf("Cannot find bone %s\n", bone);
                return;
            }

            if (GetAnimator().GetJointTransform(jointHandle, gameLocal.time, offset, axis)) {
                offset = GetPhysics().GetOrigin().oPlus(offset.oMultiply(GetPhysics().GetAxis()));
                axis = axis.oMultiply(GetPhysics().GetAxis());
            }

            idEntityFx.StartFx(fx, offset, axis, this, true);
        }

        /*
         ==================
         idPlayer::GetRenderView

         Returns the renderView that was calculated for this tic
         ==================
         */
        @Override
        public renderView_s GetRenderView() {
            return renderView;
        }

        /*
         ==================
         idPlayer::CalculateRenderView

         create the renderView for the current tic
         ==================
         */
        public void CalculateRenderView() {	// called every tic by player code
            int i;
            float range;

            if (NOT(renderView)) {
                renderView = new renderView_s();
            }
//	memset( renderView, 0, sizeof( *renderView ) );

            // copy global shader parms
            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                renderView.shaderParms[i] = gameLocal.globalShaderParms[i];
            }
            renderView.globalMaterial = gameLocal.GetGlobalMaterial();
            renderView.time = gameLocal.time;

            // calculate size of 3D view
            renderView.x = 0;
            renderView.y = 0;
            renderView.width = SCREEN_WIDTH;
            renderView.height = SCREEN_HEIGHT;
            renderView.viewID = 0;

            // check if we should be drawing from a camera's POV
            if (!noclip && (gameLocal.GetCamera() != null || privateCameraView != null)) {
                // get origin, axis, and fov
                if (privateCameraView != null) {
                    privateCameraView.GetViewParms(renderView);
                } else {
                    gameLocal.GetCamera().GetViewParms(renderView);
                }
            } else {
                if (g_stopTime.GetBool()) {
                    renderView.vieworg = new idVec3(firstPersonViewOrigin);
                    renderView.viewaxis = new idMat3(firstPersonViewAxis);

                    if (!pm_thirdPerson.GetBool()) {
                        // set the viewID to the clientNum + 1, so we can suppress the right player bodies and
                        // allow the right player view weapons
                        renderView.viewID = entityNumber + 1;
                    }
                } else if (pm_thirdPerson.GetBool()) {
                    OffsetThirdPersonView(pm_thirdPersonAngle.GetFloat(), pm_thirdPersonRange.GetFloat(), pm_thirdPersonHeight.GetFloat(), pm_thirdPersonClip.GetBool());
                } else if (pm_thirdPersonDeath.GetBool()) {
                    range = gameLocal.time < minRespawnTime ? (gameLocal.time + RAGDOLL_DEATH_TIME - minRespawnTime) * (120 / RAGDOLL_DEATH_TIME) : 120;
                    OffsetThirdPersonView(0, 20 + range, 0, false);
                } else {
                    renderView.vieworg = new idVec3(firstPersonViewOrigin);
                    renderView.viewaxis = new idMat3(firstPersonViewAxis);

                    // set the viewID to the clientNum + 1, so we can suppress the right player bodies and
                    // allow the right player view weapons
                    renderView.viewID = entityNumber + 1;
                }

                // field of view
                {
                    float[] fov_x = {renderView.fov_x};
                    float[] fov_y = {renderView.fov_y};
                    gameLocal.CalcFov(CalcFov(true), fov_x, fov_y);
                    renderView.fov_x = fov_x[0];
                    renderView.fov_y = fov_y[0];
                }
            }

            if (renderView.fov_y == 0) {
                common.Error("renderView.fov_y == 0");
            }

            if (g_showviewpos.GetBool()) {
                gameLocal.Printf("%s : %s\n", renderView.vieworg.ToString(), renderView.viewaxis.ToAngles().ToString());
            }
        }

        /*
        ===============
        idPlayer::CalculateFirstPersonView
        ===============
        */
        public void CalculateFirstPersonView() {
            if ((pm_modelView.GetInteger() == 1) || ((pm_modelView.GetInteger() == 2) && (health <= 0))) {
                //	Displays the view from the point of view of the "camera" joint in the player model

                idMat3 axis = new idMat3();
                idVec3 origin = new idVec3();
                idAngles ang;

                ang = viewBobAngles.oPlus(playerView.AngleOffset());
                ang.yaw += viewAxis.oGet(0).ToYaw();

                int joint = animator.GetJointHandle("camera");
                animator.GetJointTransform(joint, gameLocal.time, origin, axis);
                firstPersonViewOrigin = (origin.oPlus(modelOffset)).oMultiply(viewAxis.oMultiply(physicsObj.GetGravityAxis())).oPlus(physicsObj.GetOrigin()).oPlus(viewBob);
                firstPersonViewAxis = axis.oMultiply(ang.ToMat3()).oMultiply(physicsObj.GetGravityAxis());
            } else {
                // offset for local bobbing and kicks
                GetViewPos(firstPersonViewOrigin, firstPersonViewAxis);
                if (false) {
                    // shakefrom sound stuff only happens in first person
                    firstPersonViewAxis = firstPersonViewAxis.oMultiply(playerView.ShakeAxis());
                }
            }
        }

        public void DrawHUD(idUserInterface _hud) {

            if (NOT(weapon.GetEntity()) || influenceActive != INFLUENCE_NONE || privateCameraView != null || gameLocal.GetCamera() != null || NOT(_hud) || !g_showHud.GetBool()) {
                return;
            }

            UpdateHudStats(_hud);

            _hud.SetStateString("weapicon", weapon.GetEntity().Icon());

            // FIXME: this is temp to allow the sound meter to show up in the hud
            // it should be commented out before shipping but the code can remain
            // for mod developers to enable for the same functionality
            _hud.SetStateInt("s_debug", cvarSystem.GetCVarInteger("s_showLevelMeter"));

            weapon.GetEntity().UpdateGUI();

            _hud.Redraw(gameLocal.realClientTime);

            // weapon targeting crosshair
            if (!GuiActive()) {
                if (cursor != null && weapon.GetEntity().ShowCrosshair()) {
                    cursor.Redraw(gameLocal.realClientTime);
                }
            }
        }

        /*
         ==================
         WeaponFireFeedback

         Called when a weapon fires, generates head twitches, etc
         ==================
         */
        public void WeaponFireFeedback(final idDict weaponDef) {
            // force a blink
            blink_time = 0;

            // play the fire animation
            AI_WEAPON_FIRED._(true);

            // update view feedback
            playerView.WeaponFireFeedback(weaponDef);
        }


        /*
         ====================
         idPlayer::DefaultFov

         Returns the base FOV
         ====================
         */
        public float DefaultFov() {
            float fov;

            fov = g_fov.GetFloat();
            if (gameLocal.isMultiplayer) {
                if (fov < 90) {
                    return 90;
                } else if (fov > 110) {
                    return 110;
                }
            }

            return fov;
        }

        /*
         ====================
         idPlayer::CalcFov

         Fixed fov at intermissions, otherwise account for fov variable and zooms.
         ====================
         */
        public float CalcFov(boolean honorZoom) {
            float fov;

            if (fxFov) {
                return (float) (DefaultFov() + 10 + cos((gameLocal.time + 2000) * 0.01) * 10);
            }

            if (influenceFov != 0) {
                return influenceFov;
            }

            if (zoomFov.IsDone(gameLocal.time)) {
                fov = ((honorZoom && ((usercmd.buttons & BUTTON_ZOOM) != 0)) && weapon.GetEntity() != null) ? weapon.GetEntity().GetZoomFov() : DefaultFov();
            } else {
                fov = zoomFov.GetCurrentValue(gameLocal.time);
            }

            // bound normal viewsize
            if (fov < 1) {
                fov = 1;
            } else if (fov > 179) {
                fov = 179;
            }

            return fov;
        }

        /*
         ==============
         idPlayer::CalculateViewWeaponPos

         Calculate the bobbing position of the view weapon
         ==============
         */
        public void CalculateViewWeaponPos(idVec3 origin, idMat3 axis) {
            float scale;
            float fracsin;
            idAngles angles = new idAngles();
            int delta;

            // CalculateRenderView must have been called first
            final idVec3 viewOrigin = firstPersonViewOrigin;
            final idMat3 viewAxis = firstPersonViewAxis;

            // these cvars are just for hand tweaking before moving a value to the weapon def
            idVec3 gunpos = new idVec3(g_gun_x.GetFloat(), g_gun_y.GetFloat(), g_gun_z.GetFloat());

            // as the player changes direction, the gun will take a small lag
            idVec3 gunOfs = GunAcceleratingOffset();
            origin.oSet(viewOrigin.oPlus(gunpos.oPlus(gunOfs).oMultiply(viewAxis)));

            // on odd legs, invert some angles
            if ((bobCycle & 128) != 0) {
                scale = -xyspeed;
            } else {
                scale = xyspeed;
            }

            // gun angles from bobbing
            angles.roll = scale * bobfracsin * 0.005f;
            angles.yaw = scale * bobfracsin * 0.01f;
            angles.pitch = xyspeed * bobfracsin * 0.005f;

            // gun angles from turning
            if (gameLocal.isMultiplayer) {
                idAngles offset = GunTurningOffset();
                offset.oMulSet(g_mpWeaponAngleScale.GetFloat());
                angles.oPluSet(offset);
            } else {
                angles.oPluSet(GunTurningOffset());
            }

            idVec3 gravity = physicsObj.GetGravityNormal();

            // drop the weapon when landing after a jump / fall
            delta = gameLocal.time - landTime;
            if (delta < LAND_DEFLECT_TIME) {
                origin.oMinSet(gravity.oMultiply(landChange * 0.25f * delta / LAND_DEFLECT_TIME));
            } else if (delta < LAND_DEFLECT_TIME + LAND_RETURN_TIME) {
                origin.oMinSet(gravity.oMultiply(landChange * 0.25f * (LAND_DEFLECT_TIME + LAND_RETURN_TIME - delta) / LAND_RETURN_TIME));
            }

            // speed sensitive idle drift
            scale = xyspeed + 40;
            fracsin = (float) (scale * sin(MS2SEC(gameLocal.time)) * 0.01f);
            angles.roll += fracsin;
            angles.yaw += fracsin;
            angles.pitch += fracsin;

            axis.oSet(angles.ToMat3().oMultiply(viewAxis));
        }

        @Override
        public idVec3 GetEyePosition() {
            idVec3 org;

            // use the smoothed origin if spectating another player in multiplayer
            if (gameLocal.isClient && entityNumber != gameLocal.localClientNum) {
                org = smoothedOrigin;
            } else {
                org = GetPhysics().GetOrigin();
            }
            return org.oPlus(GetPhysics().GetGravityNormal().oMultiply(-eyeOffset.z));
        }

        @Override
        public void GetViewPos(idVec3 origin, idMat3 axis) {
            idAngles angles = new idAngles();

            // if dead, fix the angle and don't add any kick
            if (health <= 0) {
                angles.yaw = viewAngles.yaw;
                angles.roll = 40;
                angles.pitch = -15;
                axis.oSet(angles.ToMat3());//TODO:null check
                origin.oSet(GetEyePosition());
            } else {
                origin.oSet(GetEyePosition().oPlus(viewBob));
                angles = viewAngles.oPlus(viewBobAngles).oPlus(playerView.AngleOffset());

                axis.oSet(angles.ToMat3().oMultiply(physicsObj.GetGravityAxis()));

                // adjust the origin based on the camera nodal distance (eye distance from neck)
                origin.oPluSet(physicsObj.GetGravityNormal().oMultiply(g_viewNodalZ.GetFloat()));
                origin.oPluSet(axis.oGet(0).oMultiply(g_viewNodalX.GetFloat()).oPlus(axis.oGet(2).oMultiply(g_viewNodalZ.GetFloat())));
            }
        }

        public void OffsetThirdPersonView(float angle, float range, float height, boolean clip) {
            idVec3 view;
//            idVec3 focusAngles;
            trace_s[] trace = {null};
            idVec3 focusPoint;
            float focusDist;
            float[] forwardScale = {0}, sideScale = {0};
            idVec3 origin = new idVec3();
            idAngles angles;
            idMat3 axis = new idMat3();
            idBounds bounds;

            angles = viewAngles;
            GetViewPos(origin, axis);

            if (angle != 0) {
                if (angles.pitch > 45.0f) {
                    angles.pitch = 45.0f;		// don't go too far overhead
                } else {
                    angles.pitch = 0;
                }
            }

            focusPoint = origin.oPlus(angles.ToForward().oMultiply(THIRD_PERSON_FOCUS_DISTANCE));
            focusPoint.z += height;
            view = origin;
            view.z += 8 + height;

            angles.pitch *= 0.5f;
            renderView.viewaxis = angles.ToMat3().oMultiply(physicsObj.GetGravityAxis());

            idMath.SinCos((float) DEG2RAD(angle), sideScale, forwardScale);
            view.oMinSet(renderView.viewaxis.oGet(0).oMultiply(range * forwardScale[0]));
            view.oPluSet(renderView.viewaxis.oGet(1).oMultiply(range * sideScale[0]));

            if (clip) {
                // trace a ray from the origin to the viewpoint to make sure the view isn't
                // in a solid block.  Use an 8 by 8 block to prevent the view from near clipping anything
                bounds = new idBounds(new idVec3(-4, -4, -4), new idVec3(4, 4, 4));
                gameLocal.clip.TraceBounds(trace, origin, view, bounds, MASK_SOLID, this);
                if (trace[0].fraction != 1.0f) {
                    view = trace[0].endpos;
                    view.z += (1.0f - trace[0].fraction) * 32.0f;

                    // try another trace to this position, because a tunnel may have the ceiling
                    // close enough that this is poking out
                    gameLocal.clip.TraceBounds(trace, origin, view, bounds, MASK_SOLID, this);
                    view = trace[0].endpos;
                }
            }

            // select pitch to look at focus point from vieword
            focusPoint.oMinSet(view);
            focusDist = idMath.Sqrt(focusPoint.oGet(0) * focusPoint.oGet(0) + focusPoint.oGet(1) * focusPoint.oGet(1));
            if (focusDist < 1.0f) {
                focusDist = 1.0f;	// should never happen
            }

            angles.pitch = -RAD2DEG(atan2(focusPoint.z, focusDist));
            angles.yaw -= angle;

            renderView.vieworg = new idVec3(view);
            renderView.viewaxis = angles.ToMat3().oMulSet(physicsObj.GetGravityAxis());
            renderView.viewID = 0;
        }

        public boolean Give(final String statname, final String value) {
            int amount;

            if (AI_DEAD._()) {
                return false;
            }

            if (0 == idStr.Icmp(statname, "health")) {
                if (health >= inventory.maxHealth) {
                    return false;
                }
                amount = Integer.parseInt(value);
                if (amount != 0) {
                    health += amount;
                    if (health > inventory.maxHealth) {
                        health = inventory.maxHealth;
                    }
                    if (hud != null) {
                        hud.HandleNamedEvent("healthPulse");
                    }
                }

            } else if (0 == idStr.Icmp(statname, "stamina")) {
                if (stamina >= 100) {
                    return false;
                }
                stamina += Float.parseFloat(value);
                if (stamina > 100) {
                    stamina = 100;
                }

            } else if (0 == idStr.Icmp(statname, "heartRate")) {
                heartRate += Integer.parseInt(value);
                if (heartRate > MAX_HEARTRATE) {
                    heartRate = MAX_HEARTRATE;
                }

            } else if (0 == idStr.Icmp(statname, "air")) {
                if (airTics >= pm_airTics.GetInteger()) {
                    return false;
                }
                airTics += Integer.parseInt(value) / 100.0 * pm_airTics.GetInteger();
                if (airTics > pm_airTics.GetInteger()) {
                    airTics = pm_airTics.GetInteger();
                }
            } else {
                int[] idealWeapon = {this.idealWeapon};
                boolean result = inventory.Give(this, spawnArgs, statname, value, idealWeapon, true);
                this.idealWeapon = idealWeapon[0];
                return result;
            }
            return true;
        }

        public boolean Give(final idStr statname, final idStr value) {
            return this.Give(statname.toString(), value.toString());
        }


        /*
         ===============
         idPlayer::GiveItem

         Returns false if the item shouldn't be picked up
         ===============
         */
        public boolean GiveItem(idItem item) {
            int i;
            idKeyValue arg;
            idDict attr = new idDict();
            boolean gave;
            int numPickup;

            if (gameLocal.isMultiplayer && spectating) {
                return false;
            }

            item.GetAttributes(attr);

            gave = false;
            numPickup = inventory.pickupItemNames.Num();
            for (i = 0; i < attr.GetNumKeyVals(); i++) {
                arg = attr.GetKeyVal(i);
                if (Give(arg.GetKey(), arg.GetValue())) {
                    gave = true;
                }
            }

            arg = item.spawnArgs.MatchPrefix("inv_weapon", null);
            if (arg != null && hud != null) {
                // We need to update the weapon hud manually, but not
                // the armor/ammo/health because they are updated every
                // frame no matter what
                UpdateHudWeapon(false);
                hud.HandleNamedEvent("weaponPulse");
            }

            // display the pickup feedback on the hud
            if (gave && (numPickup == inventory.pickupItemNames.Num())) {
                inventory.AddPickupName(item.spawnArgs.GetString("inv_name"), item.spawnArgs.GetString("inv_icon"));
            }

            return gave;
        }

        public void GiveItem(final String itemName) {
            idDict args = new idDict();

            args.Set("classname", itemName);
            args.Set("owner", name);
            gameLocal.SpawnEntityDef(args);
            if (hud != null) {
                hud.HandleNamedEvent("itemPickup");
            }
        }

        /*
         ===============
         idPlayer::GiveHealthPool

         adds health to the player health pool
         ===============
         */
        public void GiveHealthPool(float amt) {

            if (AI_DEAD._()) {
                return;
            }

            if (health > 0) {
                healthPool += amt;
                if (healthPool > inventory.maxHealth - health) {
                    healthPool = inventory.maxHealth - health;
                }
                nextHealthPulse = gameLocal.time;
            }
        }

        public boolean GiveInventoryItem(idDict item) {
            if (gameLocal.isMultiplayer && spectating) {
                return false;
            }
            inventory.items.Append(new idDict(item));
            idItemInfo info = new idItemInfo();
            final String itemName = item.GetString("inv_name");
            if (idStr.Cmpn(itemName, STRTABLE_ID, STRTABLE_ID_LENGTH) == 0) {
                info.name.oSet(common.GetLanguageDict().GetString(itemName));
            } else {
                info.name.oSet(itemName);
            }
            info.icon.oSet(item.GetString("inv_icon"));
            inventory.pickupItemNames.Append(info);
            if (hud != null) {
                hud.SetStateString("itemicon", info.icon.toString());
                hud.HandleNamedEvent("invPickup");
            }
            return true;
        }

        public void RemoveInventoryItem(idDict item) {
            inventory.items.Remove(item);
//	delete item;
        }

        public boolean GiveInventoryItem(final String name) {
            idDict args = new idDict();

            args.Set("classname", name);
            args.Set("owner", this.name);
            gameLocal.SpawnEntityDef(args);
            return true;
        }

        public void RemoveInventoryItem(final String name) {
            idDict item = FindInventoryItem(name);
            if (item != null) {
                RemoveInventoryItem(item);
            }
        }

        public idDict FindInventoryItem(final String name) {
            for (int i = 0; i < inventory.items.Num(); i++) {
                final String iname = inventory.items.oGet(i).GetString("inv_name");
                if (iname != null && !iname.isEmpty()) {
                    if (idStr.Icmp(name, iname) == 0) {
                        return inventory.items.oGet(i);
                    }
                }
            }
            return null;
        }

        public idDict FindInventoryItem(final idStr name) {
            return FindInventoryItem(name.toString());
        }

        public void GivePDA(final idStr pdaName, idDict item) {
            if (gameLocal.isMultiplayer && spectating) {
                return;
            }

            if (item != null) {
                inventory.pdaSecurity.AddUnique(item.GetString("inv_name"));
            }

            if (isNotNullOrEmpty(pdaName)) {
                pdaName.oSet("personal");
            }

            idDeclPDA pda = (idDeclPDA) declManager.FindType(DECL_PDA, pdaName);

            inventory.pdas.AddUnique(pdaName);

            // Copy any videos over
            for (int i = 0; i < pda.GetNumVideos(); i++) {
                final idDeclVideo video = pda.GetVideoByIndex(i);
                if (video != null) {
                    inventory.videos.AddUnique(video.GetName());
                }
            }

            // This is kind of a hack, but it works nicely
            // We don't want to display the 'you got a new pda' message during a map load
            if (gameLocal.GetFrameNum() > 10) {
                if (pda != null && hud != null) {
                    pdaName.oSet(pda.GetPdaName());
                    pdaName.RemoveColors();
                    hud.SetStateString("pda", "1");
                    hud.SetStateString("pda_text", pdaName.toString());
                    final String sec = pda.GetSecurity();
                    hud.SetStateString("pda_security", (sec != null && !sec.isEmpty()) ? "1" : "0");//TODO:!= null and !usEmpty, check that this combination isn't the wrong way around anywhere. null== instead of !=null
                    hud.HandleNamedEvent("pdaPickup");
                }

                if (inventory.pdas.Num() == 1) {
                    GetPDA().RemoveAddedEmailsAndVideos();
                    if (!objectiveSystemOpen) {
                        TogglePDA();
                    }
                    objectiveSystem.HandleNamedEvent("showPDATip");
                    //ShowTip( spawnArgs.GetString( "text_infoTitle" ), spawnArgs.GetString( "text_firstPDA" ), true );
                }

                if (inventory.pdas.Num() > 1 && pda.GetNumVideos() > 0 && hud != null) {
                    hud.HandleNamedEvent("videoPickup");
                }
            }
        }

        public void GiveVideo(final String videoName, idDict item) {

            if (videoName == null || videoName.isEmpty()) {
                return;
            }

            inventory.videos.AddUnique(videoName);

            if (item != null) {
                idItemInfo info = new idItemInfo();
                info.name.oSet(item.GetString("inv_name"));
                info.icon.oSet(item.GetString("inv_icon"));
                inventory.pickupItemNames.Append(info);
            }
            if (hud != null) {
                hud.HandleNamedEvent("videoPickup");
            }
        }

        public void GiveEmail(final String emailName) {

            if (emailName == null || emailName.isEmpty()) {
                return;
            }

            inventory.emails.AddUnique(emailName);
            GetPDA().AddEmail(emailName);

            if (hud != null) {
                hud.HandleNamedEvent("emailPickup");
            }
        }

        public void GiveSecurity(final String security) {
            GetPDA().SetSecurity(security);
            if (hud != null) {
                hud.SetStateString("pda_security", "1");
                hud.HandleNamedEvent("securityPickup");
            }
        }

        public void GiveObjective(final String title, final String text, final String screenshot) {
            idObjectiveInfo info = new idObjectiveInfo();
            info.title = new idStr(title);
            info.text = new idStr(text);
            info.screenshot = new idStr(screenshot);
            inventory.objectiveNames.Append(info);
            ShowObjective("newObjective");
            if (hud != null) {
                hud.HandleNamedEvent("newObjective");
            }
        }

        public void CompleteObjective(final String title) {
            int c = inventory.objectiveNames.Num();
            for (int i = 0; i < c; i++) {
                if (idStr.Icmp(inventory.objectiveNames.oGet(i).title.toString(), title) == 0) {
                    inventory.objectiveNames.RemoveIndex(i);
                    break;
                }
            }
            ShowObjective("newObjectiveComplete");

            if (hud != null) {
                hud.HandleNamedEvent("newObjectiveComplete");
            }
        }

        public boolean GivePowerUp(int powerup, int time) {
            String[] sound = {null};
            String[] skin = {null};

            if (powerup >= 0 && powerup < MAX_POWERUPS) {

                if (gameLocal.isServer) {
                    idBitMsg msg = new idBitMsg();
                    final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                    msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                    msg.WriteShort(powerup);
                    msg.WriteBits(1, 1);
                    ServerSendEvent(EVENT_POWERUP, msg, false, -1);
                }

                if (powerup != MEGAHEALTH) {
                    inventory.GivePowerUp(this, powerup, time);
                }

                idDeclEntityDef def;

                switch (powerup) {
                    case BERSERK: {
                        if (spawnArgs.GetString("snd_berserk_third", "", sound)) {
                            StartSoundShader(declManager.FindSound(sound[0]), SND_CHANNEL_DEMONIC, 0, false, null);
                        }
                        if (baseSkinName.Length() != 0) {
                            powerUpSkin.oSet(declManager.FindSkin(baseSkinName + "_berserk"));
                        }
                        if (!gameLocal.isClient) {
                            idealWeapon = 0;
                        }
                        break;
                    }
                    case INVISIBILITY: {
                        spawnArgs.GetString("skin_invisibility", "", skin);
                        powerUpSkin.oSet(declManager.FindSkin(skin[0]));
                        // remove any decals from the model
                        if (modelDefHandle != -1) {
                            gameRenderWorld.RemoveDecals(modelDefHandle);
                        }
                        if (weapon.GetEntity() != null) {
                            weapon.GetEntity().UpdateSkin();
                        }
                        if (spawnArgs.GetString("snd_invisibility", "", sound)) {
                            StartSoundShader(declManager.FindSound(sound[0]), SND_CHANNEL_ANY, 0, false, null);
                        }
                        break;
                    }
                    case ADRENALINE: {
                        stamina = 100;
                        break;
                    }
                    case MEGAHEALTH: {
                        if (spawnArgs.GetString("snd_megahealth", "", sound)) {
                            StartSoundShader(declManager.FindSound(sound[0]), SND_CHANNEL_ANY, 0, false, null);
                        }
                        def = gameLocal.FindEntityDef("powerup_megahealth", false);
                        if (def != null) {
                            health = def.dict.GetInt("inv_health");
                        }
                        break;
                    }
                }

                if (hud != null) {
                    hud.HandleNamedEvent("itemPickup");
                }

                return true;
            } else {
                gameLocal.Warning("Player given power up %d\n which is out of range", powerup);
            }
            return false;
        }

        public void ClearPowerUps() {
            int i;
            for (i = 0; i < MAX_POWERUPS; i++) {
                if (PowerUpActive(i)) {
                    ClearPowerup(i);
                }
            }
            inventory.ClearPowerUps();
        }

        public boolean PowerUpActive(int powerup) {
            return (inventory.powerups & (1 << powerup)) != 0;
        }

        public float PowerUpModifier(int type) {
            float mod = 1.0f;

            if (PowerUpActive(BERSERK)) {
                switch (type) {
                    case SPEED: {
                        mod *= 1.7f;
                        break;
                    }
                    case PROJECTILE_DAMAGE: {
                        mod *= 2.0f;
                        break;
                    }
                    case MELEE_DAMAGE: {
                        mod *= 30;
                        break;
                    }
                    case MELEE_DISTANCE: {
                        mod *= 2.0f;
                        break;
                    }
                }
            }

            if (gameLocal.isMultiplayer && !gameLocal.isClient) {
                if (PowerUpActive(MEGAHEALTH)) {
                    if (healthPool <= 0) {
                        GiveHealthPool(100);
                    }
                } else {
                    healthPool = 0;
                }
            }

            return mod;
        }

        public int SlotForWeapon(final String weaponName) {
            int i;

            for (i = 0; i < MAX_WEAPONS; i++) {
                final String weap = spawnArgs.GetString(va("def_weapon%d", i));
                if (0 == idStr.Cmp(weap, weaponName)) {
                    return i;
                }
            }

            // not found
            return -1;
        }

        public void Reload() {
            if (gameLocal.isClient) {
                return;
            }

            if (spectating || gameLocal.inCinematic || influenceActive != 0) {
                return;
            }

            if (weapon.GetEntity() != null && weapon.GetEntity().IsLinked()) {
                weapon.GetEntity().Reload();
            }
        }

        public void NextWeapon() {
            String weap;
            int w;

            if (!weaponEnabled || spectating || hiddenWeapon || gameLocal.inCinematic || gameLocal.world.spawnArgs.GetBool("no_Weapons") || health < 0) {
                return;
            }

            if (gameLocal.isClient) {
                return;
            }

            // check if we have any weapons
            if (0 == inventory.weapons) {
                return;
            }

            w = idealWeapon;
            while (true) {
                w++;
                if (w >= MAX_WEAPONS) {
                    w = 0;
                }
                weap = spawnArgs.GetString(va("def_weapon%d", w));
                if (!spawnArgs.GetBool(va("weapon%d_cycle", w))) {
                    continue;
                }
                if (weap.isEmpty()) {
                    continue;
                }
                if ((inventory.weapons & (1 << w)) == 0) {
                    continue;
                }
                if (inventory.HasAmmo(weap) != 0) {
                    break;
                }
            }

            if ((w != currentWeapon) && (w != idealWeapon)) {
                idealWeapon = w;
                weaponSwitchTime = gameLocal.time + WEAPON_SWITCH_DELAY;
                UpdateHudWeapon();
            }
        }

        public void NextBestWeapon() {
            String weap;
            int w = MAX_WEAPONS;

            if (gameLocal.isClient || !weaponEnabled) {
                return;
            }

            while (w > 0) {
                w--;
                weap = spawnArgs.GetString(va("def_weapon%d", w));
                if (weap.isEmpty() || ((inventory.weapons & (1 << w)) == 0) || (0 == inventory.HasAmmo(weap))) {
                    continue;
                }
                if (!spawnArgs.GetBool(va("weapon%d_best", w))) {
                    continue;
                }
                break;
            }
            idealWeapon = w;
            weaponSwitchTime = gameLocal.time + WEAPON_SWITCH_DELAY;
            UpdateHudWeapon();
        }

        public void PrevWeapon() {
            String weap;
            int w;

            if (!weaponEnabled || spectating || hiddenWeapon || gameLocal.inCinematic || gameLocal.world.spawnArgs.GetBool("no_Weapons") || health < 0) {
                return;
            }

            if (gameLocal.isClient) {
                return;
            }

            // check if we have any weapons
            if (0 == inventory.weapons) {
                return;
            }

            w = idealWeapon;
            while (true) {
                w--;
                if (w < 0) {
                    w = MAX_WEAPONS - 1;
                }
                weap = spawnArgs.GetString(va("def_weapon%d", w));
                if (!spawnArgs.GetBool(va("weapon%d_cycle", w))) {
                    continue;
                }
                if (weap.isEmpty()) {
                    continue;
                }
                if ((inventory.weapons & (1 << w)) == 0) {
                    continue;
                }
                if (inventory.HasAmmo(weap) != 0) {
                    break;
                }
            }

            if ((w != currentWeapon) && (w != idealWeapon)) {
                idealWeapon = w;
                weaponSwitchTime = gameLocal.time + WEAPON_SWITCH_DELAY;
                UpdateHudWeapon();
            }
        }

        public void SelectWeapon(int num, boolean force) {
            String weap;

            if (!weaponEnabled || spectating || gameLocal.inCinematic || health < 0) {
                return;
            }

            if ((num < 0) || (num >= MAX_WEAPONS)) {
                return;
            }

            if (gameLocal.isClient) {
                return;
            }

            if ((num != weapon_pda) && gameLocal.world.spawnArgs.GetBool("no_Weapons")) {
                num = weapon_fists;
                hiddenWeapon ^= true;//1;
                if (hiddenWeapon && weapon.GetEntity() != null) {
                    weapon.GetEntity().LowerWeapon();
                } else {
                    weapon.GetEntity().RaiseWeapon();
                }
            }

            weap = spawnArgs.GetString(va("def_weapon%d", num));
            if (weap.isEmpty()) {
                gameLocal.Printf("Invalid weapon\n");
                return;
            }

            if (force || (inventory.weapons & (1 << num)) != 0) {
                if (0 == inventory.HasAmmo(weap) && !spawnArgs.GetBool(va("weapon%d_allowempty", num))) {
                    return;
                }
                if ((previousWeapon >= 0) && (idealWeapon == num) && (spawnArgs.GetBool(va("weapon%d_toggle", num)))) {
                    weap = spawnArgs.GetString(va("def_weapon%d", previousWeapon));
                    if (0 == inventory.HasAmmo(weap) && !spawnArgs.GetBool(va("weapon%d_allowempty", previousWeapon))) {
                        return;
                    }
                    idealWeapon = previousWeapon;
                } else if ((weapon_pda >= 0) && (num == weapon_pda) && (inventory.pdas.Num() == 0)) {
                    ShowTip(spawnArgs.GetString("text_infoTitle"), spawnArgs.GetString("text_noPDA"), true);
                    return;
                } else {
                    idealWeapon = num;
                }
                UpdateHudWeapon();
            }
        }

        public void DropWeapon(boolean died) {
            idVec3 forward = new idVec3(), up = new idVec3();
            int inclip, ammoavailable;

            assert (!gameLocal.isClient);

            if (spectating || weaponGone || weapon.GetEntity() == null) {
                return;
            }

            if ((!died && !weapon.GetEntity().IsReady()) || weapon.GetEntity().IsReloading()) {
                return;
            }
            // ammoavailable is how many shots we can fire
            // inclip is which amount is in clip right now
            ammoavailable = weapon.GetEntity().AmmoAvailable();
            inclip = weapon.GetEntity().AmmoInClip();

            // don't drop a grenade if we have none left
            if (NOT(idStr.Icmp(idWeapon.GetAmmoNameForNum(weapon.GetEntity().GetAmmoType()), "ammo_grenades")) && (ammoavailable - inclip <= 0)) {
                return;
            }

            // expect an ammo setup that makes sense before doing any dropping
            // ammoavailable is -1 for infinite ammo, and weapons like chainsaw
            // a bad ammo config usually indicates a bad weapon state, so we should not drop
            // used to be an assertion check, but it still happens in edge cases
            if ((ammoavailable != -1) && (ammoavailable - inclip < 0)) {
                common.DPrintf("idPlayer::DropWeapon: bad ammo setup\n");
                return;
            }
            idEntity item;
            if (died) {
                // ain't gonna throw you no weapon if I'm dead
                item = weapon.GetEntity().DropItem(getVec3_origin(), 0, WEAPON_DROP_TIME, died);
            } else {
                viewAngles.ToVectors(forward, null, up);
                item = weapon.GetEntity().DropItem(forward.oMultiply(250).oPlus(up.oMultiply(150)), 500, WEAPON_DROP_TIME, died);
            }
            if (null == item) {
                return;
            }
            // set the appropriate ammo in the dropped object
            final idKeyValue keyval = item.spawnArgs.MatchPrefix("inv_ammo_");
            if (keyval != null) {
                item.spawnArgs.SetInt(keyval.GetKey().toString(), ammoavailable);
                idStr inclipKey = keyval.GetKey();
                inclipKey.Insert("inclip_", 4);
                item.spawnArgs.SetInt(inclipKey.toString(), inclip);
            }
            if (!died) {
                // remove from our local inventory completely
                {
                    String[] inv_weapon = {item.spawnArgs.GetString("inv_weapon")};
                    inventory.Drop(spawnArgs, inv_weapon, -1);
                    item.spawnArgs.Set("inv_weapon", inv_weapon[0]);
                }
                weapon.GetEntity().ResetAmmoClip();
                NextWeapon();
                weapon.GetEntity().WeaponStolen();
                weaponGone = true;
            }
        }

        /*
         =================
         idPlayer::StealWeapon
         steal the target player's current weapon
         =================
         */
        public void StealWeapon(idPlayer player) {
            assert (!gameLocal.isClient);

            // make sure there's something to steal
            idWeapon player_weapon = (idWeapon) player.weapon.GetEntity();
            if (NOT(player_weapon) || !player_weapon.CanDrop() || weaponGone) {
                return;
            }
            // steal - we need to effectively force the other player to abandon his weapon
            int newweap = player.currentWeapon;
            if (newweap == -1) {
                return;
            }
            // might be just dropped - check inventory
            if (0 == (player.inventory.weapons & (1 << newweap))) {
                return;
            }
            final String weapon_classname = spawnArgs.GetString(va("def_weapon%d", newweap));
            assert (weapon_classname != null);
            int ammoavailable = player.weapon.GetEntity().AmmoAvailable();
            int inclip = player.weapon.GetEntity().AmmoInClip();
            if ((ammoavailable != -1) && (ammoavailable - inclip < 0)) {
                // see DropWeapon
                common.DPrintf("idPlayer::StealWeapon: bad ammo setup\n");
                // we still steal the weapon, so let's use the default ammo levels
                inclip = -1;
                final idDeclEntityDef decl = gameLocal.FindEntityDef(weapon_classname);
                assert (decl != null);
                final idKeyValue keypair = decl.dict.MatchPrefix("inv_ammo_");
                assert (keypair != null);
                ammoavailable = atoi(keypair.GetValue());
            }

            player.weapon.GetEntity().WeaponStolen();
            player.inventory.Drop(player.spawnArgs, null, newweap);
            player.SelectWeapon(weapon_fists, false);
            // in case the robbed player is firing rounds with a continuous fire weapon like the chaingun/plasma etc.
            // this will ensure the firing actually stops
            player.weaponGone = true;

            // give weapon, setup the ammo count
            Give("weapon", weapon_classname);
            int ammo_i = player.inventory.AmmoIndexForWeaponClass(weapon_classname, null);
            idealWeapon = newweap;
            inventory.ammo[ ammo_i] += ammoavailable;
            inventory.clip[ newweap] = inclip;
        }

        public void AddProjectilesFired(int count) {
            numProjectilesFired += count;
        }

        public void AddProjectileHits(int count) {
            numProjectileHits += count;
        }

        public void SetLastHitTime(int time) {
            idPlayer aimed = null;

            if (time != 0 && lastHitTime != time) {
                lastHitToggle ^= true;//1;
            }
            lastHitTime = time;
            if (0 == time) {
                // level start and inits
                return;
            }
            if (gameLocal.isMultiplayer && (time - lastSndHitTime) > 10) {
                lastSndHitTime = time;
                StartSound("snd_hit_feedback", SND_CHANNEL_ANY, SSF_PRIVATE_SOUND, false, null);
            }
            if (cursor != null) {
                cursor.HandleNamedEvent("hitTime");
            }
            if (hud != null) {
                if (MPAim != -1) {
                    if (gameLocal.entities[MPAim] != null && gameLocal.entities[MPAim].IsType(idPlayer.class)) {
                        aimed = (idPlayer) gameLocal.entities[MPAim];
                    }
                    assert (aimed != null);
                    // full highlight, no fade till loosing aim
                    hud.SetStateString("aim_text", gameLocal.userInfo[ MPAim].GetString("ui_name"));
                    if (aimed != null) {
                        hud.SetStateFloat("aim_color", aimed.colorBarIndex);
                    }
                    hud.HandleNamedEvent("aim_flash");
                    MPAimHighlight = true;
                    MPAimFadeTime = 0;
                } else if (lastMPAim != -1) {
                    if (gameLocal.entities[lastMPAim] != null && gameLocal.entities[lastMPAim].IsType(idPlayer.class)) {
                        aimed = (idPlayer) gameLocal.entities[lastMPAim];
                    }
                    assert (aimed != null);
                    // start fading right away
                    hud.SetStateString("aim_text", gameLocal.userInfo[ lastMPAim].GetString("ui_name"));
                    if (aimed != null) {
                        hud.SetStateFloat("aim_color", aimed.colorBarIndex);
                    }
                    hud.HandleNamedEvent("aim_flash");
                    hud.HandleNamedEvent("aim_fade");
                    MPAimHighlight = false;
                    MPAimFadeTime = gameLocal.realClientTime;
                }
            }
        }

        public void LowerWeapon() {
            if (weapon.GetEntity() != null && !weapon.GetEntity().IsHidden()) {
                weapon.GetEntity().LowerWeapon();
            }
        }

        public void RaiseWeapon() {
            if (weapon.GetEntity() != null && weapon.GetEntity().IsHidden()) {
                weapon.GetEntity().RaiseWeapon();
            }
        }

        public void WeaponLoweringCallback() {
            SetState("LowerWeapon");
            UpdateScript();
        }

        public void WeaponRisingCallback() {
            SetState("RaiseWeapon");
            UpdateScript();
        }

        public void RemoveWeapon(final String weap) {
            if (weap != null && !weap.isEmpty()) {
                String[] w = {spawnArgs.GetString(weap)};
                inventory.Drop(spawnArgs, w, -1);
                spawnArgs.Set(weap, w[0]);
            }
        }

        public boolean CanShowWeaponViewmodel() {
            return showWeaponViewModel;
        }

        public void AddAIKill() {
            int max_souls;
            int ammo_souls;

            if ((weapon_soulcube < 0) || (inventory.weapons & (1 << weapon_soulcube)) == 0) {
                return;
            }

            assert (hud != null);

            ammo_souls = idWeapon.GetAmmoNumForName("ammo_souls");
            max_souls = inventory.MaxAmmoForAmmoClass(this, "ammo_souls");
            if (inventory.ammo[ ammo_souls] < max_souls) {
                inventory.ammo[ ammo_souls]++;
                if (inventory.ammo[ ammo_souls] >= max_souls) {
                    hud.HandleNamedEvent("soulCubeReady");
                    StartSound("snd_soulcube_ready", SND_CHANNEL_ANY, 0, false, null);
                }
            }
        }

        public void SetSoulCubeProjectile(idProjectile projectile) {
            soulCubeProjectile.oSet(projectile);
        }

        /*
         ==============
         idPlayer::AdjustHeartRate

         Player heartrate works as follows

         DEF_HEARTRATE is resting heartrate

         Taking damage when health is above 75 adjusts heart rate by 1 beat per second
         Taking damage when health is below 75 adjusts heart rate by 5 beats per second
         Maximum heartrate from damage is MAX_HEARTRATE

         Firing a weapon adds 1 beat per second up to a maximum of COMBAT_HEARTRATE

         Being at less than 25% stamina adds 5 beats per second up to ZEROSTAMINA_HEARTRATE

         All heartrates are target rates.. the heart rate will start falling as soon as there have been no adjustments for 5 seconds
         Once it starts falling it always tries to get to DEF_HEARTRATE

         The exception to the above rule is upon death at which point the rate is set to DYING_HEARTRATE and starts falling 
         immediately to zero

         Heart rate volumes go from zero ( -40 db for DEF_HEARTRATE to 5 db for MAX_HEARTRATE ) the volume is 
         scaled linearly based on the actual rate

         Exception to the above rule is once the player is dead, the dying heart rate starts at either the current volume if
         it is audible or -10db and scales to 8db on the last few beats
         ==============
         */
        public void AdjustHeartRate(int target, float timeInSecs, float delay, boolean force) {

            if (heartInfo.GetEndValue() == target) {
                return;
            }

            if (AI_DEAD._() && !force) {
                return;
            }

            lastHeartAdjust = gameLocal.time;

            heartInfo.Init((int) (gameLocal.time + delay * 1000), (int) (timeInSecs * 1000), 0f + heartRate, (float) target);
        }

        public void SetCurrentHeartRate() {

            int base = idMath.FtoiFast((BASE_HEARTRATE + LOWHEALTH_HEARTRATE_ADJ) - ((float) health / 100) * LOWHEALTH_HEARTRATE_ADJ);

            if (PowerUpActive(ADRENALINE)) {
                heartRate = 135;
            } else {
                heartRate = idMath.FtoiFast(heartInfo.GetCurrentValue(gameLocal.time));
                int currentRate = GetBaseHeartRate();
                if (health >= 0 && gameLocal.time > lastHeartAdjust + 2500) {
                    AdjustHeartRate(currentRate, 2.5f, 0, false);
                }
            }

            int bps = idMath.FtoiFast(60f / heartRate * 1000f);
            if (gameLocal.time - lastHeartBeat > bps) {
                int dmgVol = DMG_VOLUME;
                int deathVol = DEATH_VOLUME;
                int zeroVol = ZERO_VOLUME;
                float pct = 0;
                if (heartRate > BASE_HEARTRATE && health > 0) {
                    pct = (float) (heartRate - base) / (MAX_HEARTRATE - base);
                    pct *= ((float) dmgVol - (float) zeroVol);
                } else if (health <= 0) {
                    pct = (float) (heartRate - DYING_HEARTRATE) / (BASE_HEARTRATE - DYING_HEARTRATE);
                    if (pct > 1.0f) {
                        pct = 1.0f;
                    } else if (pct < 0) {
                        pct = 0;
                    }
                    pct *= ((float) deathVol - (float) zeroVol);
                }

                pct += (float) zeroVol;

                if (pct != zeroVol) {
                    StartSound("snd_heartbeat", SND_CHANNEL_HEART, SSF_PRIVATE_SOUND, false, null);
                    // modify just this channel to a custom volume
                    soundShaderParms_t parms = new soundShaderParms_t();//memset( &parms, 0, sizeof( parms ) );
                    parms.volume = pct;
                    refSound.referenceSound.ModifySound(etoi(SND_CHANNEL_HEART), parms);
                }

                lastHeartBeat = gameLocal.time;
            }
        }

        public int GetBaseHeartRate() {
            int base = idMath.FtoiFast((BASE_HEARTRATE + LOWHEALTH_HEARTRATE_ADJ) - ((float) health / 100) * LOWHEALTH_HEARTRATE_ADJ);
            int rate = idMath.FtoiFast(base + (ZEROSTAMINA_HEARTRATE - base) * (1.0f - stamina / pm_stamina.GetFloat()));
            int diff = (lastDmgTime != 0) ? gameLocal.time - lastDmgTime : 99999;
            rate += (diff < 5000) ? (diff < 2500) ? (diff < 1000) ? 15 : 10 : 5 : 0;
            return rate;
        }

        public void UpdateAir() {
            if (health <= 0) {
                return;
            }

            // see if the player is connected to the info_vacuum
            boolean newAirless = false;

            if (gameLocal.vacuumAreaNum != -1) {
                int num = GetNumPVSAreas();
                if (num > 0) {
                    int areaNum;

                    // if the player box spans multiple areas, get the area from the origin point instead,
                    // otherwise a rotating player box may poke into an outside area
                    if (num == 1) {
                        final int[] pvsAreas = GetPVSAreas();
                        areaNum = pvsAreas[0];
                    } else {
                        areaNum = gameRenderWorld.PointInArea(this.GetPhysics().GetOrigin());
                    }
                    newAirless = gameRenderWorld.AreasAreConnected(gameLocal.vacuumAreaNum, areaNum, PS_BLOCK_AIR);
                }
            }

            if (newAirless) {
                if (!airless) {
                    StartSound("snd_decompress", SND_CHANNEL_ANY, SSF_GLOBAL, false, null);
                    StartSound("snd_noAir", SND_CHANNEL_BODY2, 0, false, null);
                    if (hud != null) {
                        hud.HandleNamedEvent("noAir");
                    }
                }
                airTics--;
                if (airTics < 0) {
                    airTics = 0;
                    // check for damage
                    final idDict damageDef = gameLocal.FindEntityDefDict("damage_noair", false);
                    int dmgTiming = (int) (1000 * ((damageDef != null) ? damageDef.GetFloat("delay", "3.0") : 3));
                    if (gameLocal.time > lastAirDamage + dmgTiming) {
                        Damage(null, null, getVec3_origin(), "damage_noair", 1.0f, 0);
                        lastAirDamage = gameLocal.time;
                    }
                }

            } else {
                if (airless) {
                    StartSound("snd_recompress", SND_CHANNEL_ANY, SSF_GLOBAL, false, null);
                    StopSound(etoi(SND_CHANNEL_BODY2), false);
                    if (hud != null) {
                        hud.HandleNamedEvent("Air");
                    }
                }
                airTics += 2;	// regain twice as fast as lose
                if (airTics > pm_airTics.GetInteger()) {
                    airTics = pm_airTics.GetInteger();
                }
            }

            airless = newAirless;

            if (hud != null) {
                hud.SetStateInt("player_air", 100 * airTics / pm_airTics.GetInteger());
            }
        }

        @Override
        public boolean HandleSingleGuiCommand(idEntity entityGui, idLexer src) {
            idToken token = new idToken();

            if (!src.ReadToken(token)) {
                return false;
            }

            if (token.equals(";")) {
                return false;
            }

            if (token.Icmp("addhealth") == 0) {
                if (entityGui != null && health < 100) {
                    int _health = entityGui.spawnArgs.GetInt("gui_parm1");
                    int amt = Math.min(_health, HEALTH_PER_DOSE);
                    _health -= amt;
                    entityGui.spawnArgs.SetInt("gui_parm1", _health);
                    if (entityGui.GetRenderEntity() != null && entityGui.GetRenderEntity().gui[0] != null) {
                        entityGui.GetRenderEntity().gui[ 0].SetStateInt("gui_parm1", _health);
                    }
                    health += amt;
                    if (health > 100) {
                        health = 100;
                    }
                }
                return true;
            }

            if (token.Icmp("ready") == 0) {
                PerformImpulse(IMPULSE_17);
                return true;
            }

            if (token.Icmp("updatepda") == 0) {
                UpdatePDAInfo(true);
                return true;
            }

            if (token.Icmp("updatepda2") == 0) {
                UpdatePDAInfo(false);
                return true;
            }

            if (token.Icmp("stoppdavideo") == 0) {
                if (objectiveSystem != null && objectiveSystemOpen && pdaVideoWave.Length() > 0) {
                    StopSound(etoi(SND_CHANNEL_PDA), false);
                }
                return true;
            }

            if (token.Icmp("close") == 0) {
                if (objectiveSystem != null && objectiveSystemOpen) {
                    TogglePDA();
                }
            }

            if (token.Icmp("playpdavideo") == 0) {
                if (objectiveSystem != null && objectiveSystemOpen && pdaVideo.Length() > 0) {
                    final idMaterial mat = declManager.FindMaterial(pdaVideo);
                    if (mat != null) {
                        int c = mat.GetNumStages();
                        for (int i = 0; i < c; i++) {
                            final shaderStage_t stage = mat.GetStage(i);
                            if (stage != null && stage.texture.cinematic[0] != null) {
                                stage.texture.cinematic[0].ResetTime(gameLocal.time);
                            }
                        }
                        if (pdaVideoWave.Length() != 0) {
                            final idSoundShader shader = declManager.FindSound(pdaVideoWave);
                            StartSoundShader(shader, SND_CHANNEL_PDA, 0, false, null);
                        }
                    }
                }
            }

            if (token.Icmp("playpdaaudio") == 0) {
                if (objectiveSystem != null && objectiveSystemOpen && pdaAudio.Length() > 0) {
                    final idSoundShader shader = declManager.FindSound(pdaAudio);
                    final int[] ms = new int[1];
                    StartSoundShader(shader, SND_CHANNEL_PDA, 0, false, ms);
                    StartAudioLog();
                    CancelEvents(EV_Player_StopAudioLog);
                    PostEventMS(EV_Player_StopAudioLog, ms[0] + 150);
                }
                return true;
            }

            if (token.Icmp("stoppdaaudio") == 0) {
                if (objectiveSystem != null && objectiveSystemOpen && pdaAudio.Length() > 0) {
                    // idSoundShader *shader = declManager.FindSound( pdaAudio );
                    StopAudioLog();
                    StopSound(etoi(SND_CHANNEL_PDA), false);
                }
                return true;
            }

            src.UnreadToken(token);
            return false;
        }

        public boolean GuiActive() {
            return focusGUIent != null;
        }

        public void PerformImpulse(int impulse) {

            if (gameLocal.isClient) {
                idBitMsg msg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                assert (entityNumber == gameLocal.localClientNum);
                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.BeginWriting();
                msg.WriteBits(impulse, 6);
                ClientSendEvent(EVENT_IMPULSE, msg);
            }

            if (impulse >= IMPULSE_0 && impulse <= IMPULSE_12) {
                SelectWeapon(impulse, false);
                return;
            }

            switch (impulse) {
                case IMPULSE_13: {
                    Reload();
                    break;
                }
                case IMPULSE_14: {
                    NextWeapon();
                    break;
                }
                case IMPULSE_15: {
                    PrevWeapon();
                    break;
                }
                case IMPULSE_17: {
                    if (gameLocal.isClient || entityNumber == gameLocal.localClientNum) {
                        gameLocal.mpGame.ToggleReady();
                    }
                    break;
                }
                case IMPULSE_18: {
                    centerView.Init(gameLocal.time, 200, viewAngles.pitch, 0f);
                    break;
                }
                case IMPULSE_19: {
                    // when we're not in single player, IMPULSE_19 is used for showScores
                    // otherwise it opens the pda
                    if (!gameLocal.isMultiplayer) {
                        if (objectiveSystemOpen) {
                            TogglePDA();
                        } else if (weapon_pda >= 0) {
                            SelectWeapon(weapon_pda, true);
                        }
                    }
                    break;
                }
                case IMPULSE_20: {
                    if (gameLocal.isClient || entityNumber == gameLocal.localClientNum) {
                        gameLocal.mpGame.ToggleTeam();
                    }
                    break;
                }
                case IMPULSE_22: {
                    if (gameLocal.isClient || entityNumber == gameLocal.localClientNum) {
                        gameLocal.mpGame.ToggleSpectate();
                    }
                    break;
                }
                case IMPULSE_28: {
                    if (gameLocal.isClient || entityNumber == gameLocal.localClientNum) {
                        gameLocal.mpGame.CastVote(gameLocal.localClientNum, true);
                    }
                    break;
                }
                case IMPULSE_29: {
                    if (gameLocal.isClient || entityNumber == gameLocal.localClientNum) {
                        gameLocal.mpGame.CastVote(gameLocal.localClientNum, false);
                    }
                    break;
                }
                case IMPULSE_40: {
                    UseVehicle();
                    break;
                }
            }
        }

        public void Spectate(boolean spectate) {
            idBitMsg msg = new idBitMsg();
            ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

            // track invisible player bug
            // all hiding and showing should be performed through Spectate calls
            // except for the private camera view, which is used for teleports
            assert ((teleportEntity.GetEntity() != null) || (IsHidden() == spectating));

            if (spectating == spectate) {
                return;
            }

            spectating = spectate;

            if (gameLocal.isServer) {
                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.WriteBits(btoi(spectating), 1);
                ServerSendEvent(EVENT_SPECTATE, msg, false, -1);
            }

            if (spectating) {
                // join the spectators
                ClearPowerUps();
                spectator = this.entityNumber;
                Init();
                StopRagdoll();
                SetPhysics(physicsObj);
                physicsObj.DisableClip();
                Hide();
                Event_DisableWeapon();
                if (hud != null) {
                    hud.HandleNamedEvent("aim_clear");
                    MPAimFadeTime = 0;
                }
            } else {
                // put everything back together again
                currentWeapon = -1;	// to make sure the def will be loaded if necessary
                Show();
                Event_EnableWeapon();
            }
            SetClipModel();
        }

        public void TogglePDA() {
            if (objectiveSystem == null) {
                return;
            }

            if (inventory.pdas.Num() == 0) {
                ShowTip(spawnArgs.GetString("text_infoTitle"), spawnArgs.GetString("text_noPDA"), true);
                return;
            }

            assert (hud != null);

            if (!objectiveSystemOpen) {
                int j, c = inventory.items.Num();
                objectiveSystem.SetStateInt("inv_count", c);
                for (j = 0; j < MAX_INVENTORY_ITEMS; j++) {
                    objectiveSystem.SetStateString(va("inv_name_%d", j), "");
                    objectiveSystem.SetStateString(va("inv_icon_%d", j), "");
                    objectiveSystem.SetStateString(va("inv_text_%d", j), "");
                }
                for (j = 0; j < c; j++) {
                    idDict item = inventory.items.oGet(j);
                    if (!item.GetBool("inv_pda")) {
                        final String iname = item.GetString("inv_name");
                        final String iicon = item.GetString("inv_icon");
                        final String itext = item.GetString("inv_text");
                        objectiveSystem.SetStateString(va("inv_name_%d", j), iname);
                        objectiveSystem.SetStateString(va("inv_icon_%d", j), iicon);
                        objectiveSystem.SetStateString(va("inv_text_%d", j), itext);
                        final idKeyValue kv = item.MatchPrefix("inv_id", null);
                        if (kv != null) {
                            objectiveSystem.SetStateString(va("inv_id_%d", j), kv.GetValue().toString());
                        }
                    }
                }

                for (j = 0; j < MAX_WEAPONS; j++) {
                    final String weapnum = va("def_weapon%d", j);
                    final String hudWeap = va("weapon%d", j);
                    int weapstate = 0;
                    if ((inventory.weapons & (1 << j)) != 0) {
                        final String weap = spawnArgs.GetString(weapnum);
                        if (weap != null && !weap.isEmpty()) {
                            weapstate++;
                        }
                    }
                    objectiveSystem.SetStateInt(hudWeap, weapstate);
                }

                objectiveSystem.SetStateInt("listPDA_sel_0", inventory.selPDA);
                objectiveSystem.SetStateInt("listPDAVideo_sel_0", inventory.selVideo);
                objectiveSystem.SetStateInt("listPDAAudio_sel_0", inventory.selAudio);
                objectiveSystem.SetStateInt("listPDAEmail_sel_0", inventory.selEMail);
                UpdatePDAInfo(false);
                UpdateObjectiveInfo();
                objectiveSystem.Activate(true, gameLocal.time);
                hud.HandleNamedEvent("pdaPickupHide");
                hud.HandleNamedEvent("videoPickupHide");
            } else {
                inventory.selPDA = objectiveSystem.State().GetInt("listPDA_sel_0");
                inventory.selVideo = objectiveSystem.State().GetInt("listPDAVideo_sel_0");
                inventory.selAudio = objectiveSystem.State().GetInt("listPDAAudio_sel_0");
                inventory.selEMail = objectiveSystem.State().GetInt("listPDAEmail_sel_0");
                objectiveSystem.Activate(false, gameLocal.time);
            }
            objectiveSystemOpen ^= true;//1;
        }

        public void ToggleScoreboard() {
            scoreBoardOpen ^= true;//1;
        }

        public void RouteGuiMouse(idUserInterface gui) {
            sysEvent_s ev;
            String command;

            if (usercmd.mx != oldMouseX || usercmd.my != oldMouseY) {
                ev = sys.GenerateMouseMoveEvent(usercmd.mx - oldMouseX, usercmd.my - oldMouseY);
                command = gui.HandleEvent(ev, gameLocal.time);
                oldMouseX = usercmd.mx;
                oldMouseY = usercmd.my;
            }
        }

        public void UpdateHud() {
            idPlayer aimed;

            if (null == hud) {
                return;
            }

            if (entityNumber != gameLocal.localClientNum) {
                return;
            }

            int c = inventory.pickupItemNames.Num();
            if (c > 0) {
                if (gameLocal.time > inventory.nextItemPickup) {
                    if (inventory.nextItemPickup != 0 && gameLocal.time - inventory.nextItemPickup > 2000) {
                        inventory.nextItemNum = 1;
                    }
                    int i;
                    for (i = 0; i < 5 && i < c; i++) {
                        hud.SetStateString(va("itemtext%d", inventory.nextItemNum), inventory.pickupItemNames.oGet(0).name.toString());
                        hud.SetStateString(va("itemicon%d", inventory.nextItemNum), inventory.pickupItemNames.oGet(0).icon.toString());
                        hud.HandleNamedEvent(va("itemPickup%d", inventory.nextItemNum++));
                        inventory.pickupItemNames.RemoveIndex(0);
                        if (inventory.nextItemNum == 1) {
                            inventory.onePickupTime = gameLocal.time;
                        } else if (inventory.nextItemNum > 5) {
                            inventory.nextItemNum = 1;
                            inventory.nextItemPickup = inventory.onePickupTime + 2000;
                        } else {
                            inventory.nextItemPickup = gameLocal.time + 400;
                        }
                    }
                }
            }

            if (gameLocal.realClientTime == lastMPAimTime) {
                if (MPAim != -1 && gameLocal.gameType == GAME_TDM
                        && gameLocal.entities[MPAim] != null && gameLocal.entities[MPAim].IsType(idPlayer.class)
                        && ((idPlayer) gameLocal.entities[MPAim]).team == team) {
                    aimed = (idPlayer) gameLocal.entities[MPAim];
                    hud.SetStateString("aim_text", gameLocal.userInfo[MPAim].GetString("ui_name"));
                    hud.SetStateFloat("aim_color", aimed.colorBarIndex);
                    hud.HandleNamedEvent("aim_flash");
                    MPAimHighlight = true;
                    MPAimFadeTime = 0;	// no fade till loosing focus
                } else if (MPAimHighlight) {
                    hud.HandleNamedEvent("aim_fade");
                    MPAimFadeTime = gameLocal.realClientTime;
                    MPAimHighlight = false;
                }
            }
            if (MPAimFadeTime != 0) {
                assert (!MPAimHighlight);
                if (gameLocal.realClientTime - MPAimFadeTime > 2000) {
                    MPAimFadeTime = 0;
                }
            }

            hud.SetStateInt("g_showProjectilePct", g_showProjectilePct.GetInteger());
            if (numProjectilesFired != 0) {
                hud.SetStateString("projectilepct", va("Hit %% %.1f", ((float) numProjectileHits / numProjectilesFired) * 100));
            } else {
                hud.SetStateString("projectilepct", "Hit % 0.0");
            }

            if (isLagged && gameLocal.isMultiplayer && gameLocal.localClientNum == entityNumber) {
                hud.SetStateString("hudLag", "1");
            } else {
                hud.SetStateString("hudLag", "0");
            }
        }

        public idDeclPDA GetPDA() {
            if (inventory.pdas.Num() != 0) {
                return (idDeclPDA) declManager.FindType(DECL_PDA, inventory.pdas.oGet(0));
            } else {
                return null;
            }
        }

        public idDeclVideo GetVideo(int index) {
            if (index >= 0 && index < inventory.videos.Num()) {
                return (idDeclVideo) declManager.FindType(DECL_VIDEO, inventory.videos.oGet(index), false);
            }
            return null;
        }

        public void SetInfluenceFov(float fov) {
            influenceFov = fov;
        }

        public void SetInfluenceView(final String mtr, final String skinname, float radius, idEntity ent) {
            influenceMaterial = null;
            influenceEntity = null;
            influenceSkin = null;
            if (mtr != null && !mtr.isEmpty()) {
                influenceMaterial = declManager.FindMaterial(mtr);
            }
            if (skinname != null && !skinname.isEmpty()) {
                influenceSkin = declManager.FindSkin(skinname);
                if (head.GetEntity() != null) {
                    head.GetEntity().GetRenderEntity().shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                }
                UpdateVisuals();
            }
            influenceRadius = radius;
            if (radius > 0) {
                influenceEntity = ent;
            }
        }

        public void SetInfluenceLevel(int level) {
            if (level != influenceActive) {
                if (level != 0) {
                    for (idEntity ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                        if (ent.IsType(idProjectile.class)) {
                            // remove all projectiles
                            ent.PostEventMS(EV_Remove, 0);
                        }
                    }
                    if (weaponEnabled && weapon.GetEntity() != null) {
                        weapon.GetEntity().EnterCinematic();
                    }
                } else {
                    physicsObj.SetLinearVelocity(getVec3_origin());
                    if (weaponEnabled && weapon.GetEntity() != null) {
                        weapon.GetEntity().ExitCinematic();
                    }
                }
                influenceActive = level;
            }
        }

        public int GetInfluenceLevel() {
            return influenceActive;
        }

        public void SetPrivateCameraView(idCamera camView) {
            privateCameraView = camView;
            if (camView != null) {
                StopFiring();
                Hide();
            } else {
                if (!spectating) {
                    Show();
                }
            }
        }

        public idCamera GetPrivateCameraView() {
            return privateCameraView;
        }

        public void StartFxFov(float duration) {
            fxFov = true;
            PostEventSec(EV_Player_StopFxFov, duration);
        }

        public void UpdateHudWeapon(boolean flashWeapon /*= true*/) {
            idUserInterface hud = this.hud;

            // if updating the hud of a followed client
            if (gameLocal.localClientNum >= 0 && gameLocal.entities[gameLocal.localClientNum] != null && gameLocal.entities[gameLocal.localClientNum].IsType(idPlayer.class)) {
                idPlayer p = (idPlayer) gameLocal.entities[gameLocal.localClientNum];
                if (p.spectating && p.spectator == entityNumber) {
                    assert (p.hud != null);
                    hud = p.hud;
                }
            }

            if (null == hud) {
                return;
            }

            for (int i = 0; i < MAX_WEAPONS; i++) {
                final String weapnum = va("def_weapon%d", i);
                final String hudWeap = va("weapon%d", i);
                int weapstate = 0;
                if ((inventory.weapons & (1 << i)) != 0) {
                    final String weap = spawnArgs.GetString(weapnum);
                    if (weap != null && !weap.isEmpty()) {
                        weapstate++;
                    }
                    if (idealWeapon == i) {
                        weapstate++;
                    }
                }
                hud.SetStateInt(hudWeap, weapstate);
            }
            if (flashWeapon) {
                hud.HandleNamedEvent("weaponChange");
            }
        }

        public void UpdateHudWeapon() {
            this.UpdateHudWeapon(true);
        }

        public void UpdateHudStats(idUserInterface _hud) {
            int staminapercentage;
            float max_stamina;

            assert (_hud != null);

            max_stamina = pm_stamina.GetFloat();
            if (0 == max_stamina) {
                // stamina disabled, so show full stamina bar
                staminapercentage = 100;
            } else {
                staminapercentage = idMath.FtoiFast(100 * stamina / max_stamina);
            }

            _hud.SetStateInt("player_health", health);
            _hud.SetStateInt("player_stamina", staminapercentage);
            _hud.SetStateInt("player_armor", inventory.armor);
            _hud.SetStateInt("player_hr", heartRate);
            _hud.SetStateInt("player_nostamina", (max_stamina == 0) ? 1 : 0);

            _hud.HandleNamedEvent("updateArmorHealthAir");

            if (healthPulse) {
                _hud.HandleNamedEvent("healthPulse");
                StartSound("snd_healthpulse", SND_CHANNEL_ITEM, 0, false, null);
                healthPulse = false;
            }

            if (healthTake) {
                _hud.HandleNamedEvent("healthPulse");
                StartSound("snd_healthtake", SND_CHANNEL_ITEM, 0, false, null);
                healthTake = false;
            }

            if (inventory.ammoPulse) {
                _hud.HandleNamedEvent("ammoPulse");
                inventory.ammoPulse = false;
            }
            if (inventory.weaponPulse) {
                // We need to update the weapon hud manually, but not
                // the armor/ammo/health because they are updated every
                // frame no matter what
                UpdateHudWeapon();
                _hud.HandleNamedEvent("weaponPulse");
                inventory.weaponPulse = false;
            }
            if (inventory.armorPulse) {
                _hud.HandleNamedEvent("armorPulse");
                inventory.armorPulse = false;
            }

            UpdateHudAmmo(_hud);
        }

        public void UpdateHudAmmo(idUserInterface _hud) {
            int inclip;
            int ammoamount;

            assert (weapon.GetEntity() != null);
            assert (_hud != null);

            inclip = weapon.GetEntity().AmmoInClip();
            ammoamount = weapon.GetEntity().AmmoAvailable();
            if (ammoamount < 0 || !weapon.GetEntity().IsReady()) {
                // show infinite ammo
                _hud.SetStateString("player_ammo", "");
                _hud.SetStateString("player_totalammo", "");
            } else {
                // show remaining ammo
                _hud.SetStateString("player_totalammo", va("%d", ammoamount - inclip));
                _hud.SetStateString("player_ammo", (weapon.GetEntity().ClipSize() != 0) ? va("%d", inclip) : "--");		// how much in the current clip
                _hud.SetStateString("player_clips", (weapon.GetEntity().ClipSize() != 0) ? va("%d", ammoamount / weapon.GetEntity().ClipSize()) : "--");
                _hud.SetStateString("player_allammo", va("%d/%d", inclip, ammoamount - inclip));
            }

            _hud.SetStateBool("player_ammo_empty", (ammoamount == 0));
            _hud.SetStateBool("player_clip_empty", ((weapon.GetEntity().ClipSize() != 0) ? inclip == 0 : false));
            _hud.SetStateBool("player_clip_low", ((weapon.GetEntity().ClipSize() != 0) ? inclip <= weapon.GetEntity().LowAmmo() : false));

            _hud.HandleNamedEvent("updateAmmo");
        }

        public void Event_StopAudioLog() {
            StopAudioLog();
        }

        public void StartAudioLog() {
            if (hud != null) {
                hud.HandleNamedEvent("audioLogUp");
            }
        }

        public void StopAudioLog() {
            if (hud != null) {
                hud.HandleNamedEvent("audioLogDown");
            }
        }

        public void ShowTip(final String title, final String tip, boolean autoHide) {
            if (tipUp) {
                return;
            }
            hud.SetStateString("tip", tip);
            hud.SetStateString("tiptitle", title);
            hud.HandleNamedEvent("tipWindowUp");
            if (autoHide) {
                PostEventSec(EV_Player_HideTip, 5.0f);
            }
            tipUp = true;
        }

        public void HideTip() {
            hud.HandleNamedEvent("tipWindowDown");
            tipUp = false;
        }

        public boolean IsTipVisible() {
            return tipUp;
        }

        public void ShowObjective(final String obj) {
            hud.HandleNamedEvent(obj);
            objectiveUp = true;
        }

        public void HideObjective() {
            hud.HandleNamedEvent("closeObjective");
            objectiveUp = false;
        }

        @Override
        public void ClientPredictionThink() {
            renderEntity_s headRenderEnt;

            oldFlags = usercmd.flags;
            oldButtons = usercmd.buttons;

            usercmd = gameLocal.usercmds[entityNumber];

            if (entityNumber != gameLocal.localClientNum) {
                // ignore attack button of other clients. that's no good for predictions
                usercmd.buttons &= ~BUTTON_ATTACK;
            }

            buttonMask &= usercmd.buttons;
            usercmd.buttons &= ~buttonMask;

            if (objectiveSystemOpen) {
                usercmd.forwardmove = 0;
                usercmd.rightmove = 0;
                usercmd.upmove = 0;
            }

            // clear the ik before we do anything else so the skeleton doesn't get updated twice
            walkIK.ClearJointMods();

            if (gameLocal.isNewFrame) {
                if ((usercmd.flags & UCF_IMPULSE_SEQUENCE) != (oldFlags & UCF_IMPULSE_SEQUENCE)) {
                    PerformImpulse(usercmd.impulse);
                }
            }

            scoreBoardOpen = ((usercmd.buttons & BUTTON_SCORES) != 0 || forceScoreBoard);

            AdjustSpeed();

            UpdateViewAngles();

            // update the smoothed view angles
            if (gameLocal.framenum >= smoothedFrame && entityNumber != gameLocal.localClientNum) {
                idAngles anglesDiff = viewAngles.oMinus(smoothedAngles);
                anglesDiff.Normalize180();
                if (idMath.Fabs(anglesDiff.yaw) < 90 && idMath.Fabs(anglesDiff.pitch) < 90) {
                    // smoothen by pushing back to the previous angles
                    viewAngles.oMinSet(anglesDiff.oMultiply(gameLocal.clientSmoothing));
                    viewAngles.Normalize180();
                }
                smoothedAngles = viewAngles;
            }
            smoothedOriginUpdated = false;

            if (!af.IsActive()) {
                AdjustBodyAngles();
            }

            if (!isLagged) {
                // don't allow client to move when lagged
                Move();
            }

            // update GUIs, Items, and character interactions
            UpdateFocus();

            // service animations
            if (!spectating && !af.IsActive()) {
                UpdateConditions();
                UpdateAnimState();
                CheckBlink();
            }

            // clear out our pain flag so we can tell if we recieve any damage between now and the next time we think
            AI_PAIN._(false);

            // calculate the exact bobbed view position, which is used to
            // position the view weapon, among other things
            CalculateFirstPersonView();

            // this may use firstPersonView, or a thirdPerson / camera view
            CalculateRenderView();

            if (!gameLocal.inCinematic && weapon.GetEntity() != null && (health > 0) && !(gameLocal.isMultiplayer && spectating)) {
                UpdateWeapon();
            }

            UpdateHud();

            if (gameLocal.isNewFrame) {
                UpdatePowerUps();
            }

            UpdateDeathSkin(false);

            if (head.GetEntity() != null) {
                headRenderEnt = head.GetEntity().GetRenderEntity();
            } else {
                headRenderEnt = null;
            }

            if (headRenderEnt != null) {
                if (influenceSkin != null) {
                    headRenderEnt.customSkin = influenceSkin;
                } else {
                    headRenderEnt.customSkin = null;
                }
            }

            if (gameLocal.isMultiplayer || g_showPlayerShadow.GetBool()) {
                renderEntity.suppressShadowInViewID = 0;
                if (headRenderEnt != null) {
                    headRenderEnt.suppressShadowInViewID = 0;
                }
            } else {
                renderEntity.suppressShadowInViewID = entityNumber + 1;
                if (headRenderEnt != null) {
                    headRenderEnt.suppressShadowInViewID = entityNumber + 1;
                }
            }
            // never cast shadows from our first-person muzzle flashes
            renderEntity.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + entityNumber;
            if (headRenderEnt != null) {
                headRenderEnt.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + entityNumber;
            }

            if (!gameLocal.inCinematic) {
                UpdateAnimation();
            }

            if (gameLocal.isMultiplayer) {
                DrawPlayerIcons();
            }

            Present();

            UpdateDamageEffects();

            LinkCombat();

            if (gameLocal.isNewFrame && entityNumber == gameLocal.localClientNum) {
                playerView.CalculateShake();
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            physicsObj.WriteToSnapshot(msg);
            WriteBindToSnapshot(msg);
            msg.WriteDeltaFloat(0, deltaViewAngles.oGet(0));
            msg.WriteDeltaFloat(0, deltaViewAngles.oGet(1));
            msg.WriteDeltaFloat(0, deltaViewAngles.oGet(2));
            msg.WriteShort(health);
            msg.WriteBits(gameLocal.ServerRemapDecl(-1, DECL_ENTITYDEF, lastDamageDef), gameLocal.entityDefBits);
            msg.WriteDir(lastDamageDir, 9);
            msg.WriteShort(lastDamageLocation);
            msg.WriteBits(idealWeapon, idMath.BitsForInteger(MAX_WEAPONS));
            msg.WriteBits(inventory.weapons, MAX_WEAPONS);
            msg.WriteBits(weapon.GetSpawnId(), 32);
            msg.WriteBits(spectator, idMath.BitsForInteger(MAX_CLIENTS));
            msg.WriteBits(btoi(lastHitToggle), 1);
            msg.WriteBits(btoi(weaponGone), 1);
            msg.WriteBits(btoi(isLagged), 1);
            msg.WriteBits(btoi(isChatting), 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            int i, oldHealth, newIdealWeapon, weaponSpawnId;
            boolean newHitToggle, stateHitch;

            stateHitch = snapshotSequence - lastSnapshotSequence > 1;
            lastSnapshotSequence = snapshotSequence;

            oldHealth = health;

            physicsObj.ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);
            deltaViewAngles.oSet(0, msg.ReadDeltaFloat(0));
            deltaViewAngles.oSet(1, msg.ReadDeltaFloat(0));
            deltaViewAngles.oSet(2, msg.ReadDeltaFloat(0));
            health = msg.ReadShort();
            lastDamageDef = gameLocal.ClientRemapDecl(DECL_ENTITYDEF, msg.ReadBits(gameLocal.entityDefBits));
            lastDamageDir = msg.ReadDir(9);
            lastDamageLocation = msg.ReadShort();
            newIdealWeapon = msg.ReadBits(idMath.BitsForInteger(MAX_WEAPONS));
            inventory.weapons = msg.ReadBits(MAX_WEAPONS);
            weaponSpawnId = msg.ReadBits(32);
            spectator = msg.ReadBits(idMath.BitsForInteger(MAX_CLIENTS));
            newHitToggle = msg.ReadBits(1) != 0;
            weaponGone = msg.ReadBits(1) != 0;
            isLagged = msg.ReadBits(1) != 0;
            isChatting = msg.ReadBits(1) != 0;

            // no msg reading below this
            if (weapon.SetSpawnId(weaponSpawnId)) {
                if (weapon.GetEntity() != null) {
                    // maintain ownership locally
                    weapon.GetEntity().SetOwner(this);
                }
                currentWeapon = -1;
            }
            // if not a local client assume the client has all ammo types
            if (entityNumber != gameLocal.localClientNum) {
                for (i = 0; i < AMMO_NUMTYPES; i++) {
                    inventory.ammo[ i] = 999;
                }
            }

            if (oldHealth > 0 && health <= 0) {
                if (stateHitch) {
                    // so we just hide and don't show a death skin
                    UpdateDeathSkin(true);
                }
                // die
                AI_DEAD._(true);
                ClearPowerUps();
                SetAnimState(ANIMCHANNEL_LEGS, "Legs_Death", 4);
                SetAnimState(ANIMCHANNEL_TORSO, "Torso_Death", 4);
                SetWaitState("");
                animator.ClearAllJoints();
                if (entityNumber == gameLocal.localClientNum) {
                    playerView.Fade(colorBlack, 12000);
                }
                StartRagdoll();
                physicsObj.SetMovementType(PM_DEAD);
                if (!stateHitch) {
                    StartSound("snd_death", SND_CHANNEL_VOICE, 0, false, null);
                }
                if (weapon.GetEntity() != null) {
                    weapon.GetEntity().OwnerDied();
                }
            } else if (oldHealth <= 0 && health > 0) {
                // respawn
                Init();
                StopRagdoll();
                SetPhysics(physicsObj);
                physicsObj.EnableClip();
                SetCombatContents(true);
            } else if (health < oldHealth && health > 0) {
                if (stateHitch) {
                    lastDmgTime = gameLocal.time;
                } else {
                    // damage feedback
                    final idDeclEntityDef def = (idDeclEntityDef) declManager.DeclByIndex(DECL_ENTITYDEF, lastDamageDef, false);
                    if (def != null) {
                        playerView.DamageImpulse(lastDamageDir.oMultiply(viewAxis.Transpose()), def.dict);
                        AI_PAIN._(Pain(null, null, oldHealth - health, lastDamageDir, lastDamageLocation));
                        lastDmgTime = gameLocal.time;
                    } else {
                        common.Warning("NET: no damage def for damage feedback '%d'\n", lastDamageDef);
                    }
                }
            } else if (health > oldHealth && PowerUpActive(MEGAHEALTH) && !stateHitch) {
                // just pulse, for any health raise
                healthPulse = true;
            }

            // If the player is alive, restore proper physics object
            if (health > 0 && IsActiveAF()) {
                StopRagdoll();
                SetPhysics(physicsObj);
                physicsObj.EnableClip();
                SetCombatContents(true);
            }

            if (idealWeapon != newIdealWeapon) {
                if (stateHitch) {
                    weaponCatchup = true;
                }
                idealWeapon = newIdealWeapon;
                UpdateHudWeapon();
            }

            if (lastHitToggle != newHitToggle) {
                SetLastHitTime(gameLocal.realClientTime);
            }

            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        public void WritePlayerStateToSnapshot(idBitMsgDelta msg) {
            int i;

            msg.WriteByte(bobCycle);
            msg.WriteLong(stepUpTime);
            msg.WriteFloat(stepUpDelta);
            msg.WriteShort(inventory.weapons);
            msg.WriteByte(inventory.armor);

            for (i = 0; i < AMMO_NUMTYPES; i++) {
                msg.WriteBits(inventory.ammo[i], ASYNC_PLAYER_INV_AMMO_BITS);
            }
            for (i = 0; i < MAX_WEAPONS; i++) {
                msg.WriteBits(inventory.clip[i], ASYNC_PLAYER_INV_CLIP_BITS);
            }
        }

        public void ReadPlayerStateFromSnapshot(final idBitMsgDelta msg) {
            int i, ammo;

            bobCycle = msg.ReadByte();
            stepUpTime = msg.ReadLong();
            stepUpDelta = msg.ReadFloat();
            inventory.weapons = msg.ReadShort();
            inventory.armor = msg.ReadByte();

            for (i = 0; i < AMMO_NUMTYPES; i++) {
                ammo = msg.ReadBits(ASYNC_PLAYER_INV_AMMO_BITS);
                if (gameLocal.time >= inventory.ammoPredictTime) {
                    inventory.ammo[ i] = ammo;
                }
            }
            for (i = 0; i < MAX_WEAPONS; i++) {
                inventory.clip[i] = msg.ReadBits(ASYNC_PLAYER_INV_CLIP_BITS);
            }
        }

        @Override
        public boolean ServerReceiveEvent(int event, int time, final idBitMsg msg) {

            if (idEntity_ServerReceiveEvent(event, time, msg)) {
                return true;
            }

            // client->server events
            switch (event) {
                case EVENT_IMPULSE: {
                    PerformImpulse(msg.ReadBits(6));
                    return true;
                }
                default: {
                    return false;
                }
            }
        }

        @Override
        public boolean GetPhysicsToVisualTransform(idVec3 origin, idMat3 axis) {
            if (af.IsActive()) {
                af.GetPhysicsToVisualTransform(origin, axis);
                return true;
            }

            // smoothen the rendered origin and angles of other clients
            // smooth self origin if snapshots are telling us prediction is off
            if (gameLocal.isClient && gameLocal.framenum >= smoothedFrame && (entityNumber != gameLocal.localClientNum || selfSmooth)) {
                // render origin and axis
                idMat3 renderAxis = viewAxis.oMultiply(GetPhysics().GetAxis());
                idVec3 renderOrigin = GetPhysics().GetOrigin().oPlus(modelOffset.oMultiply(renderAxis));

                // update the smoothed origin
                if (!smoothedOriginUpdated) {
                    idVec2 originDiff = renderOrigin.ToVec2().oMinus(smoothedOrigin.ToVec2());
                    if (originDiff.LengthSqr() < Square(100)) {
                        // smoothen by pushing back to the previous position
                        if (selfSmooth) {
                            assert (entityNumber == gameLocal.localClientNum);
                            renderOrigin.ToVec2_oMinSet(originDiff.oMultiply(net_clientSelfSmoothing.GetFloat()));
                        } else {
                            renderOrigin.ToVec2_oMinSet(originDiff.oMultiply(gameLocal.clientSmoothing));
                        }
                    }
                    smoothedOrigin = renderOrigin;

                    smoothedFrame = gameLocal.framenum;
                    smoothedOriginUpdated = true;
                }

                axis.oSet(new idAngles(0, smoothedAngles.yaw, 0).ToMat3());
                origin.oSet(axis.Transpose().oMultiply(smoothedOrigin.oMinus(GetPhysics().GetOrigin())));

            } else {

                axis.oSet(viewAxis);
                origin.oSet(modelOffset);
            }
            return true;
        }

        @Override
        public boolean GetPhysicsToSoundTransform(idVec3 origin, idMat3 axis) {
            idCamera camera;

            if (privateCameraView != null) {
                camera = privateCameraView;
            } else {
                camera = gameLocal.GetCamera();
            }

            if (camera != null) {
                renderView_s view = new renderView_s();

//		memset( &view, 0, sizeof( view ) );
                camera.GetViewParms(view);
                origin.oSet(view.vieworg);
                axis.oSet(view.viewaxis);
                return true;
            } else {
                return super.GetPhysicsToSoundTransform(origin, axis);
            }
        }

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {
            int powerup;
            boolean start;

            switch (event) {
                case EVENT_EXIT_TELEPORTER:
                    Event_ExitTeleporter();
                    return true;
                case EVENT_ABORT_TELEPORTER:
                    SetPrivateCameraView(null);
                    return true;
                case EVENT_POWERUP: {
                    powerup = msg.ReadShort();
                    start = msg.ReadBits(1) != 0;
                    if (start) {
                        GivePowerUp(powerup, 0);
                    } else {
                        ClearPowerup(powerup);
                    }
                    return true;
                }
                case EVENT_SPECTATE: {
                    boolean spectate = (msg.ReadBits(1) != 0);
                    Spectate(spectate);
                    return true;
                }
                case EVENT_ADD_DAMAGE_EFFECT: {
                    if (spectating) {
                        // if we're spectating, ignore
                        // happens if the event and the spectate change are written on the server during the same frame (fraglimit)
                        return true;
                    }
                    return super.ClientReceiveEvent(event, time, msg);
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//            return false;
        }

        public boolean IsReady() {
            return ready || forcedReady;
        }

        public boolean IsRespawning() {
            return respawning;
        }

        public boolean IsInTeleport() {
            return (teleportEntity.GetEntity() != null);
        }

        public idEntity GetInfluenceEntity() {
            return influenceEntity;
        }

        public idMaterial GetInfluenceMaterial() {
            return influenceMaterial;
        }

        public float GetInfluenceRadius() {
            return influenceRadius;
        }

        // server side work for in/out of spectate. takes care of spawning it into the world as well
        public void ServerSpectate(boolean spectate) {
            assert (!gameLocal.isClient);

            if (spectating != spectate) {
                Spectate(spectate);
                if (spectate) {
                    SetSpectateOrigin();
                } else {
                    if (gameLocal.gameType == GAME_DM) {
                        // make sure the scores are reset so you can't exploit by spectating and entering the game back
                        // other game types don't matter, as you either can't join back, or it's team scores
                        gameLocal.mpGame.ClearFrags(entityNumber);
                    }
                }
            }
            if (!spectate) {
                SpawnFromSpawnSpot();
            }
        }

        // for very specific usage. != GetPhysics()
        public idPhysics GetPlayerPhysics() {
            return physicsObj;
        }

        public void TeleportDeath(int killer) {
            teleportKiller = killer;
        }

        public void SetLeader(boolean lead) {
            leader = lead;
        }

        public boolean IsLeader() {
            return leader;
        }

        public void UpdateSkinSetup(boolean restart) {
            if (restart) {
                team = (idStr.Icmp(GetUserInfo().GetString("ui_team"), "Blue") == 0) ? 1 : 0;
            }
            if (gameLocal.gameType == GAME_TDM) {
                if (team != 0) {
                    baseSkinName.oSet("skins/characters/player/marine_mp_blue");
                } else {
                    baseSkinName.oSet("skins/characters/player/marine_mp_red");
                }
                if (!gameLocal.isClient && team != latchedTeam) {
                    gameLocal.mpGame.SwitchToTeam(entityNumber, latchedTeam, team);
                }
                latchedTeam = team;
            } else {
                baseSkinName.oSet(GetUserInfo().GetString("ui_skin"));
            }
            if (0 == baseSkinName.Length()) {
                baseSkinName.oSet("skins/characters/player/marine_mp");
            }
            skin.oSet(declManager.FindSkin(baseSkinName, false));
            assert (skin != null);
            // match the skin to a color band for scoreboard
            if (baseSkinName.Find("red") != -1) {
                colorBarIndex = 1;
            } else if (baseSkinName.Find("green") != -1) {
                colorBarIndex = 2;
            } else if (baseSkinName.Find("blue") != -1) {
                colorBarIndex = 3;
            } else if (baseSkinName.Find("yellow") != -1) {
                colorBarIndex = 4;
            } else {
                colorBarIndex = 0;
            }
            colorBar = colorBarTable[ colorBarIndex];
            if (PowerUpActive(BERSERK)) {
                powerUpSkin.oSet(declManager.FindSkin(baseSkinName + "_berserk"));
            }
        }

        @Override
        public boolean OnLadder() {
            return physicsObj.OnLadder();
        }

        public void UpdatePlayerIcons() {
            int time = networkSystem.ServerGetClientTimeSinceLastPacket(entityNumber);
            if (time > cvarSystem.GetCVarInteger("net_clientMaxPrediction")) {
                isLagged = true;
            } else {
                isLagged = false;
            }
        }

        public void DrawPlayerIcons() {
            if (!NeedsIcon()) {
                playerIcon.FreeIcon();
                return;
            }
            playerIcon.Draw(this, headJoint);
        }

        public void HidePlayerIcons() {
            playerIcon.FreeIcon();
        }

        public boolean NeedsIcon() {
            // local clients don't render their own icons... they're only info for other clients
            return entityNumber != gameLocal.localClientNum && (isLagged || isChatting);
        }

        public boolean SelfSmooth() {
            return selfSmooth;
        }

        public void SetSelfSmooth(boolean b) {
            selfSmooth = b;
        }

        private void LookAtKiller(idEntity inflictor, idEntity attacker) {
            idVec3 dir;

            if (!this.equals(attacker)) {
                dir = attacker.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
            } else if (!this.equals(inflictor)) {
                dir = inflictor.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
            } else {
                dir = viewAxis.oGet(0);
            }

            idAngles ang = new idAngles(0, dir.ToYaw(), 0);
            SetViewAngles(ang);
        }

        private void StopFiring() {
            AI_ATTACK_HELD._(false);
            AI_WEAPON_FIRED._(false);
            AI_RELOAD._(false);
            if (weapon.GetEntity() != null) {
                weapon.GetEntity().EndAttack();
            }
        }

        private void FireWeapon() {
            idMat3 axis = new idMat3();
            idVec3 muzzle = new idVec3();

            if (privateCameraView != null) {
                return;
            }

            if (g_editEntityMode.GetInteger() != 0) {
                GetViewPos(muzzle, axis);
                if (gameLocal.editEntities.SelectEntity(muzzle, axis.oGet(0), this)) {
                    return;
                }
            }

            if (!hiddenWeapon && weapon.GetEntity().IsReady()) {
                if (weapon.GetEntity().AmmoInClip() != 0 || weapon.GetEntity().AmmoAvailable() != 0) {
                    AI_ATTACK_HELD._(true);
                    weapon.GetEntity().BeginAttack();
                    if ((weapon_soulcube >= 0) && (currentWeapon == weapon_soulcube)) {
                        if (hud != null) {
                            hud.HandleNamedEvent("soulCubeNotReady");
                        }
                        SelectWeapon(previousWeapon, false);
                    }
                } else {
                    NextBestWeapon();
                }
            }

            if (hud != null) {
                if (tipUp) {
                    HideTip();
                }
                // may want to track with with a bool as well
                // keep from looking up named events so often
                if (objectiveUp) {
                    HideObjective();
                }
            }
        }

        private void Weapon_Combat() {
            if (influenceActive != 0 || !weaponEnabled || gameLocal.inCinematic || privateCameraView != null) {
                return;
            }

            weapon.GetEntity().RaiseWeapon();
            if (weapon.GetEntity().IsReloading()) {
                if (!AI_RELOAD._()) {
                    AI_RELOAD._(true);
                    SetState("ReloadWeapon");
                    UpdateScript();
                }
            } else {
                AI_RELOAD._(false);
            }

            if (idealWeapon == weapon_soulcube && soulCubeProjectile.GetEntity() != null) {
                idealWeapon = currentWeapon;
            }

            if (idealWeapon != currentWeapon) {
                if (weaponCatchup) {
                    assert (gameLocal.isClient);

                    currentWeapon = idealWeapon;
                    weaponGone = false;
                    animPrefix.oSet(spawnArgs.GetString(va("def_weapon%d", currentWeapon)));
                    weapon.GetEntity().GetWeaponDef(animPrefix.toString(), inventory.clip[ currentWeapon]);
                    animPrefix.Strip("weapon_");

                    weapon.GetEntity().NetCatchup();
                    final function_t newstate = GetScriptFunction("NetCatchup");
                    if (newstate != null) {
                        SetState(newstate);
                        UpdateScript();
                    }
                    weaponCatchup = false;
                } else {
                    if (weapon.GetEntity().IsReady()) {
                        weapon.GetEntity().PutAway();
                    }

                    if (weapon.GetEntity().IsHolstered()) {
                        assert (idealWeapon >= 0);
                        assert (idealWeapon < MAX_WEAPONS);

                        if (currentWeapon != weapon_pda && !spawnArgs.GetBool(va("weapon%d_toggle", currentWeapon))) {
                            previousWeapon = currentWeapon;
                        }
                        currentWeapon = idealWeapon;
                        weaponGone = false;
                        animPrefix.oSet(spawnArgs.GetString(va("def_weapon%d", currentWeapon)));
                        weapon.GetEntity().GetWeaponDef(animPrefix.toString(), inventory.clip[ currentWeapon]);
                        animPrefix.Strip("weapon_");

                        weapon.GetEntity().Raise();
                    }
                }
            } else {
                weaponGone = false;	// if you drop and re-get weap, you may miss the = false above 
                if (weapon.GetEntity().IsHolstered()) {
                    if (NOT(weapon.GetEntity().AmmoAvailable())) {
                        // weapons can switch automatically if they have no more ammo
                        NextBestWeapon();
                    } else {
                        weapon.GetEntity().Raise();
                        state = GetScriptFunction("RaiseWeapon");
                        if (state != null) {
                            SetState(state);
                        }
                    }
                }
            }

            // check for attack
            AI_WEAPON_FIRED._(false);
            if (0 == influenceActive) {
                if (((usercmd.buttons & BUTTON_ATTACK) != 0) && !weaponGone) {
                    FireWeapon();
                } else if ((oldButtons & BUTTON_ATTACK) != 0) {
                    AI_ATTACK_HELD._(false);
                    weapon.GetEntity().EndAttack();
                }
            }

            // update our ammo clip in our inventory
            if ((currentWeapon >= 0) && (currentWeapon < MAX_WEAPONS)) {
                inventory.clip[ currentWeapon] = weapon.GetEntity().AmmoInClip();
                if (hud != null && (currentWeapon == idealWeapon)) {
                    UpdateHudAmmo(hud);
                }
            }
        }

        private void Weapon_NPC() {
            if (idealWeapon != currentWeapon) {
                Weapon_Combat();
            }
            StopFiring();
            weapon.GetEntity().LowerWeapon();

            if (((usercmd.buttons & BUTTON_ATTACK) != 0) && (0 == (oldButtons & BUTTON_ATTACK))) {
                buttonMask |= BUTTON_ATTACK;
                focusCharacter.TalkTo(this);
            }
        }

        private void Weapon_GUI() {

            if (!objectiveSystemOpen) {
                if (idealWeapon != currentWeapon) {
                    Weapon_Combat();
                }
                StopFiring();
                weapon.GetEntity().LowerWeapon();
            }

            // disable click prediction for the GUIs. handy to check the state sync does the right thing
            if (gameLocal.isClient && !net_clientPredictGUI.GetBool()) {
                return;
            }

            if (((oldButtons ^ usercmd.buttons) & BUTTON_ATTACK) != 0) {
                sysEvent_s ev;
                String command = null;
                boolean[] updateVisuals = {false};

                idUserInterface ui = ActiveGui();
                if (ui != null) {
                    ev = sys.GenerateMouseButtonEvent(1, (usercmd.buttons & BUTTON_ATTACK) != 0);
                    command = ui.HandleEvent(ev, gameLocal.time, updateVisuals);
                    if (updateVisuals[0] && focusGUIent != null && ui.equals(focusUI)) {
                        focusGUIent.UpdateVisuals();
                    }
                }
                if (gameLocal.isClient) {
                    // we predict enough, but don't want to execute commands
                    return;
                }
                if (focusGUIent != null) {
                    HandleGuiCommands(focusGUIent, command);
                } else {
                    HandleGuiCommands(this, command);
                }
            }
        }

        private void UpdateWeapon() {
            if (health <= 0) {
                return;
            }

            assert (!spectating);

            if (gameLocal.isClient) {
                // clients need to wait till the weapon and it's world model entity
                // are present and synchronized ( weapon.worldModel idEntityPtr to idAnimatedEntity )
                if (!weapon.GetEntity().IsWorldModelReady()) {
                    return;
                }
            }

            // always make sure the weapon is correctly setup before accessing it
            if (!weapon.GetEntity().IsLinked()) {
                if (idealWeapon != -1) {
                    animPrefix.oSet(spawnArgs.GetString(va("def_weapon%d", idealWeapon)));
                    weapon.GetEntity().GetWeaponDef(animPrefix.toString(), inventory.clip[ idealWeapon]);
                    assert (weapon.GetEntity().IsLinked());
                } else {
                    return;
                }
            }

            if (hiddenWeapon && tipUp && (usercmd.buttons & BUTTON_ATTACK) != 0) {
                HideTip();
            }

            if (g_dragEntity.GetBool()) {
                StopFiring();
                weapon.GetEntity().LowerWeapon();
                dragEntity.Update(this);
            } else if (ActiveGui() != null) {
                // gui handling overrides weapon use
                Weapon_GUI();
            } else if (focusCharacter != null && (focusCharacter.health > 0)) {
                Weapon_NPC();
            } else {
                Weapon_Combat();
            }

            if (hiddenWeapon) {
                weapon.GetEntity().LowerWeapon();
            }

            // update weapon state, particles, dlights, etc
            weapon.GetEntity().PresentWeapon(showWeaponViewModel);
        }

        private void UpdateSpectating() {
            assert (spectating);
            assert (!gameLocal.isClient);
            assert (IsHidden());
            idPlayer player;
            if (!gameLocal.isMultiplayer) {
                return;
            }
            player = gameLocal.GetClientByNum(spectator);
            if (null == player || (player.spectating && player != this)) {//TODO:equals instead of != or ==
                SpectateFreeFly(true);
            } else if (usercmd.upmove > 0) {
                SpectateFreeFly(false);
            } else if ((usercmd.buttons & BUTTON_ATTACK) != 0) {
                SpectateCycle();
            }
        }

        private void SpectateFreeFly(boolean force) {	// ignore the timeout to force when followed spec is no longer valid
            idPlayer player;
            idVec3 newOrig;
            idVec3 spawn_origin = new idVec3();
            idAngles spawn_angles = new idAngles();

            player = gameLocal.GetClientByNum(spectator);
            if (force || gameLocal.time > lastSpectateChange) {
                spectator = entityNumber;
                if (player != null && player != this && !player.spectating && !player.IsInTeleport()) {
                    newOrig = player.GetPhysics().GetOrigin();
                    if (player.physicsObj.IsCrouching()) {
                        newOrig.oPluSet(2, pm_crouchviewheight.GetFloat());
                    } else {
                        newOrig.oPluSet(2, pm_normalviewheight.GetFloat());
                    }
                    newOrig.oPluSet(2, SPECTATE_RAISE);
                    idBounds b = new idBounds(getVec3_origin()).Expand(pm_spectatebbox.GetFloat() * 0.5f);
                    idVec3 start = player.GetPhysics().GetOrigin();
                    start.oPluSet(2, pm_spectatebbox.GetFloat() * 0.5f);
                    trace_s[] t = {null};
                    // assuming spectate bbox is inside stand or crouch box
                    gameLocal.clip.TraceBounds(t, start, newOrig, b, MASK_PLAYERSOLID, player);
                    newOrig.Lerp(start, newOrig, t[0].fraction);
                    SetOrigin(newOrig);
                    idAngles angle = player.viewAngles;
                    angle.oSet(2, 0);
                    SetViewAngles(angle);
                } else {
                    SelectInitialSpawnPoint(spawn_origin, spawn_angles);
                    spawn_origin.oPluSet(2, pm_normalviewheight.GetFloat());
                    spawn_origin.oPluSet(2, SPECTATE_RAISE);
                    SetOrigin(spawn_origin);
                    SetViewAngles(spawn_angles);
                }
                lastSpectateChange = gameLocal.time + 500;
            }
        }

        private void SpectateCycle() {
            idPlayer player;

            if (gameLocal.time > lastSpectateChange) {
                int latchedSpectator = spectator;
                spectator = gameLocal.GetNextClientNum(spectator);
                player = gameLocal.GetClientByNum(spectator);
                assert (player != null); // never call here when the current spectator is wrong
                // ignore other spectators
                while (latchedSpectator != spectator && player.spectating) {
                    spectator = gameLocal.GetNextClientNum(spectator);
                    player = gameLocal.GetClientByNum(spectator);
                }
                lastSpectateChange = gameLocal.time + 500;
            }
        }

        /*
         ==============
         idPlayer::GunTurningOffset

         generate a rotational offset for the gun based on the view angle
         history in loggedViewAngles
         ==============
         */
        private idAngles GunTurningOffset() {
            idAngles a = new idAngles();

//            a.Zero();
            if (gameLocal.framenum < NUM_LOGGED_VIEW_ANGLES) {
                return a;
            }

            idAngles current = loggedViewAngles[ gameLocal.framenum & (NUM_LOGGED_VIEW_ANGLES - 1)];

            idAngles av;//, base;
            int[] weaponAngleOffsetAverages = {0};
            float[] weaponAngleOffsetScale = {0}, weaponAngleOffsetMax = {0};

            weapon.GetEntity().GetWeaponAngleOffsets(weaponAngleOffsetAverages, weaponAngleOffsetScale, weaponAngleOffsetMax);

            av = current;

            // calcualte this so the wrap arounds work properly
            for (int j = 1; j < weaponAngleOffsetAverages[0]; j++) {
                idAngles a2 = loggedViewAngles[ (gameLocal.framenum - j) & (NUM_LOGGED_VIEW_ANGLES - 1)];

                idAngles delta = a2.oMinus(current);

                if (delta.oGet(1) > 180) {
                    delta.oMinSet(1, 360);
                } else if (delta.oGet(1) < -180) {
                    delta.oPluSet(1, 360);
                }

                av.oPluSet(delta.oMultiply(1.0f / weaponAngleOffsetAverages[0]));
            }

            a = (av.oMinus(current)).oMultiply(weaponAngleOffsetScale[0]);

            for (int i = 0; i < 3; i++) {
                if (a.oGet(i) < -weaponAngleOffsetMax[0]) {
                    a.oSet(i, -weaponAngleOffsetMax[0]);
                } else if (a.oGet(i) > weaponAngleOffsetMax[0]) {
                    a.oSet(i, weaponAngleOffsetMax[0]);
                }
            }

            return a;
        }

        /*
         ==============
         idPlayer::GunAcceleratingOffset

         generate a positional offset for the gun based on the movement
         history in loggedAccelerations
         ==============
         */
        private idVec3 GunAcceleratingOffset() {
            idVec3 ofs = new idVec3();

            float[] weaponOffsetTime = {0}, weaponOffsetScale = {0};

            ofs.Zero();

            weapon.GetEntity().GetWeaponTimeOffsets(weaponOffsetTime, weaponOffsetScale);

            int stop = currentLoggedAccel - NUM_LOGGED_ACCELS;
            if (stop < 0) {
                stop = 0;
            }
            for (int i = currentLoggedAccel - 1; i > stop; i--) {
                loggedAccel_t acc = loggedAccel[i & (NUM_LOGGED_ACCELS - 1)];

                float f;
                float t = gameLocal.time - acc.time;
                if (t >= weaponOffsetTime[0]) {
                    break;	// remainder are too old to care about
                }

                f = t / weaponOffsetTime[0];
                f = (float) ((Math.cos(f * 2.0f * idMath.PI) - 1.0f) * 0.5f);
                ofs.oPluSet(acc.dir.oMultiply(f * weaponOffsetScale[0]));
            }

            return ofs;
        }
//
//        private void UseObjects();
//
//

        /*
         =================
         idPlayer::CrashLand

         Check for hard landings that generate sound events
         =================
         */
        private void CrashLand(final idVec3 oldOrigin, final idVec3 oldVelocity) {
            idVec3 origin, velocity;
            idVec3 gravityVector, gravityNormal;
            float delta;
            float hardDelta, fatalDelta;
            float dist;
            float vel, acc;
            float t;
            float a, b, c, den;
            waterLevel_t waterLevel;
            boolean noDamage;

            AI_SOFTLANDING._(false);
            AI_HARDLANDING._(false);

            // if the player is not on the ground
            if (!physicsObj.HasGroundContacts()) {
                return;
            }

            gravityNormal = physicsObj.GetGravityNormal();

            // if the player wasn't going down
            if ((oldVelocity.oMultiply(gravityNormal.oNegative())) >= 0) {
                return;
            }

            waterLevel = physicsObj.GetWaterLevel();

            // never take falling damage if completely underwater
            if (waterLevel == WATERLEVEL_HEAD) {
                return;
            }

            // no falling damage if touching a nodamage surface
            noDamage = false;
            for (int i = 0; i < physicsObj.GetNumContacts(); i++) {
                final contactInfo_t contact = physicsObj.GetContact(i);
                if ((contact.material.GetSurfaceFlags() & SURF_NODAMAGE) != 0) {
                    noDamage = true;
                    StartSound("snd_land_hard", SND_CHANNEL_ANY, 0, false, null);
                    break;
                }
            }

            origin = GetPhysics().GetOrigin();
            gravityVector = physicsObj.GetGravity();

            // calculate the exact velocity on landing
            dist = (origin.oMinus(oldOrigin)).oMultiply(gravityNormal.oNegative());
            vel = oldVelocity.oMultiply(gravityNormal.oNegative());
            acc = -gravityVector.Length();

            a = acc / 2.0f;
            b = vel;
            c = -dist;

            den = b * b - 4.0f * a * c;
            if (den < 0) {
                return;
            }
            t = (-b - idMath.Sqrt(den)) / (2.0f * a);

            delta = vel + t * acc;
            delta = delta * delta * 0.0001f;

            // reduce falling damage if there is standing water
            if (waterLevel == WATERLEVEL_WAIST) {
                delta *= 0.25f;
            }
            if (waterLevel == WATERLEVEL_FEET) {
                delta *= 0.5f;
            }

            if (delta < 1.0f) {
                return;
            }

            // allow falling a bit further for multiplayer
            if (gameLocal.isMultiplayer) {
                fatalDelta = 75.0f;
                hardDelta = 50;
            } else {
                fatalDelta = 65.0f;
                hardDelta = 45.0f;
            }

            if (delta > fatalDelta) {
                AI_HARDLANDING._(true);
                landChange = -32;
                landTime = gameLocal.time;
                if (!noDamage) {
                    pain_debounce_time = gameLocal.time + pain_delay + 1;  // ignore pain since we'll play our landing anim
                    Damage(null, null, new idVec3(0, 0, -1), "damage_fatalfall", 1.0f, 0);
                }
            } else if (delta > hardDelta) {
                AI_HARDLANDING._(true);
                landChange = -24;
                landTime = gameLocal.time;
                if (!noDamage) {
                    pain_debounce_time = gameLocal.time + pain_delay + 1;  // ignore pain since we'll play our landing anim
                    Damage(null, null, new idVec3(0, 0, -1), "damage_hardfall", 1.0f, 0);
                }
            } else if (delta > 30) {
                AI_HARDLANDING._(true);
                landChange = -16;
                landTime = gameLocal.time;
                if (!noDamage) {
                    pain_debounce_time = gameLocal.time + pain_delay + 1;  // ignore pain since we'll play our landing anim
                    Damage(null, null, new idVec3(0, 0, -1), "damage_softfall", 1.0f, 0);
                }
            } else if (delta > 7) {
                AI_SOFTLANDING._(true);
                landChange = -8;
                landTime = gameLocal.time;
            } else if (delta > 3) {
                // just walk on
            }
        }

        private void BobCycle(final idVec3 pushVelocity) {
            float bobmove;
            int old, deltaTime;
            idVec3 vel, gravityDir, velocity;
            idMat3 viewaxis;
            float bob;
            float delta;
            float speed;
            float f;

            //
            // calculate speed and cycle to be used for
            // all cyclic walking effects
            //
            velocity = physicsObj.GetLinearVelocity().oMinus(pushVelocity);

            gravityDir = physicsObj.GetGravityNormal();
            vel = velocity.oMinus(gravityDir.oMultiply(velocity.oMultiply(gravityDir)));
            xyspeed = vel.LengthFast();

            // do not evaluate the bob for other clients
            // when doing a spectate follow, don't do any weapon bobbing
            if (gameLocal.isClient && entityNumber != gameLocal.localClientNum) {
                viewBobAngles.Zero();
                viewBob.Zero();
                return;
            }

            if (!physicsObj.HasGroundContacts() || influenceActive == INFLUENCE_LEVEL2 || (gameLocal.isMultiplayer && spectating)) {
                // airborne
                bobCycle = 0;
                bobFoot = 0;
                bobfracsin = 0;
            } else if ((0 == usercmd.forwardmove && 0 == usercmd.rightmove) || (xyspeed <= MIN_BOB_SPEED)) {
                // start at beginning of cycle again
                bobCycle = 0;
                bobFoot = 0;
                bobfracsin = 0;
            } else {
                if (physicsObj.IsCrouching()) {
                    bobmove = pm_crouchbob.GetFloat();
                    // ducked characters never play footsteps
                } else {
                    // vary the bobbing based on the speed of the player
                    bobmove = pm_walkbob.GetFloat() * (1.0f - bobFrac) + pm_runbob.GetFloat() * bobFrac;
                }

                // check for footstep / splash sounds
                old = bobCycle;
                bobCycle = (int) (old + bobmove * gameLocal.msec) & 255;
                bobFoot = (bobCycle & 128) >> 7;
                bobfracsin = idMath.Fabs((float) Math.sin((bobCycle & 127) / 127.0 * idMath.PI));
            }

            // calculate angles for view bobbing
            viewBobAngles.Zero();

            viewaxis = viewAngles.ToMat3().oMultiply(physicsObj.GetGravityAxis());

            // add angles based on velocity
            delta = velocity.oMultiply(viewaxis.oGet(0));
            viewBobAngles.pitch += delta * pm_runpitch.GetFloat();

            delta = velocity.oMultiply(viewaxis.oGet(1));
            viewBobAngles.roll -= delta * pm_runroll.GetFloat();

            // add angles based on bob
            // make sure the bob is visible even at low speeds
            speed = xyspeed > 200 ? xyspeed : 200;

            delta = bobfracsin * pm_bobpitch.GetFloat() * speed;
            if (physicsObj.IsCrouching()) {
                delta *= 3;		// crouching
            }
            viewBobAngles.pitch += delta;
            delta = bobfracsin * pm_bobroll.GetFloat() * speed;
            if (physicsObj.IsCrouching()) {
                delta *= 3;		// crouching accentuates roll
            }
            if ((bobFoot & 1) != 0) {
                delta = -delta;
            }
            viewBobAngles.roll += delta;

            // calculate position for view bobbing
            viewBob.Zero();

            if (physicsObj.HasSteppedUp()) {

                // check for stepping up before a previous step is completed
                deltaTime = gameLocal.time - stepUpTime;
                if (deltaTime < STEPUP_TIME) {
                    stepUpDelta = stepUpDelta * (STEPUP_TIME - deltaTime) / STEPUP_TIME + physicsObj.GetStepUp();
                } else {
                    stepUpDelta = physicsObj.GetStepUp();
                }
                if (stepUpDelta > 2.0f * pm_stepsize.GetFloat()) {
                    stepUpDelta = 2.0f * pm_stepsize.GetFloat();
                }
                stepUpTime = gameLocal.time;
            }

            idVec3 gravity = physicsObj.GetGravityNormal();

            // if the player stepped up recently
            deltaTime = gameLocal.time - stepUpTime;
            if (deltaTime < STEPUP_TIME) {
                viewBob.oPluSet(gravity.oMultiply(stepUpDelta * (STEPUP_TIME - deltaTime) / STEPUP_TIME));
            }

            // add bob height after any movement smoothing
            bob = bobfracsin * xyspeed * pm_bobup.GetFloat();
            if (bob > 6) {
                bob = 6;
            }
            viewBob.oPluSet(2, bob);

            // add fall height
            delta = gameLocal.time - landTime;
            if (delta < LAND_DEFLECT_TIME) {
                f = delta / LAND_DEFLECT_TIME;
                viewBob.oMinSet(gravity.oMultiply(landChange * f));
            } else if (delta < LAND_DEFLECT_TIME + LAND_RETURN_TIME) {
                delta -= LAND_DEFLECT_TIME;
                f = 1.0f - (delta / LAND_RETURN_TIME);
                viewBob.oMinSet(gravity.oMultiply(landChange * f));
            }
        }

        private void UpdateViewAngles() {
            int i;
            idAngles delta = new idAngles();

            if (!noclip && (gameLocal.inCinematic || privateCameraView != null || gameLocal.GetCamera() != null || influenceActive == INFLUENCE_LEVEL2 || objectiveSystemOpen)) {
                // no view changes at all, but we still want to update the deltas or else when
                // we get out of this mode, our view will snap to a kind of random angle
                UpdateDeltaViewAngles(viewAngles);
                return;
            }

            // if dead
            if (health <= 0) {
                if (pm_thirdPersonDeath.GetBool()) {
                    viewAngles.roll = 0.0f;
                    viewAngles.pitch = 30.0f;
                } else {
                    viewAngles.roll = 40.0f;
                    viewAngles.pitch = -15.0f;
                }
                return;
            }

            // circularly clamp the angles with deltas
            for (i = 0; i < 3; i++) {
                cmdAngles.oSet(i, (float) SHORT2ANGLE(usercmd.angles[i]));
                if (influenceActive == INFLUENCE_LEVEL3) {
                    viewAngles.oPluSet(i, idMath.ClampFloat(-1.0f, 1.0f, idMath.AngleDelta(idMath.AngleNormalize180((float) (SHORT2ANGLE(usercmd.angles[i]) + deltaViewAngles.oGet(i))), viewAngles.oGet(i))));
                } else {
                    viewAngles.oSet(i, idMath.AngleNormalize180((float) (SHORT2ANGLE(usercmd.angles[i]) + deltaViewAngles.oGet(i))));
                }
            }
            if (!centerView.IsDone(gameLocal.time)) {
                viewAngles.pitch = centerView.GetCurrentValue(gameLocal.time);
            }

            // clamp the pitch
            if (noclip) {
                if (viewAngles.pitch > 89.0f) {
                    // don't let the player look down more than 89 degrees while noclipping
                    viewAngles.pitch = 89.0f;
                } else if (viewAngles.pitch < -89.0f) {
                    // don't let the player look up more than 89 degrees while noclipping
                    viewAngles.pitch = -89.0f;
                }
            } else {
                if (viewAngles.pitch > pm_maxviewpitch.GetFloat()) {
                    // don't let the player look down enough to see the shadow of his (non-existant) feet
                    viewAngles.pitch = pm_maxviewpitch.GetFloat();
                } else if (viewAngles.pitch < pm_minviewpitch.GetFloat()) {
                    // don't let the player look up more than 89 degrees
                    viewAngles.pitch = pm_minviewpitch.GetFloat();
                }
            }

            UpdateDeltaViewAngles(viewAngles);

            // orient the model towards the direction we're looking
            SetAngles(new idAngles(0, viewAngles.yaw, 0));

            // save in the log for analyzing weapon angle offsets
            loggedViewAngles[ gameLocal.framenum & (NUM_LOGGED_VIEW_ANGLES - 1)] = viewAngles;
        }

        private void EvaluateControls() {
            // check for respawning
            if (health <= 0) {
                if ((gameLocal.time > minRespawnTime) && ((usercmd.buttons & BUTTON_ATTACK) != 0)) {
                    forceRespawn = true;
                } else if (gameLocal.time > maxRespawnTime) {
                    forceRespawn = true;
                }
            }

            // in MP, idMultiplayerGame decides spawns
            if (forceRespawn && !gameLocal.isMultiplayer && !g_testDeath.GetBool()) {
                // in single player, we let the session handle restarting the level or loading a game
                gameLocal.sessionCommand.oSet("died");
            }

            if ((usercmd.flags & UCF_IMPULSE_SEQUENCE) != (oldFlags & UCF_IMPULSE_SEQUENCE)) {
                PerformImpulse(usercmd.impulse);
            }

            scoreBoardOpen = ((usercmd.buttons & BUTTON_SCORES) != 0 || forceScoreBoard);

            oldFlags = usercmd.flags;

            AdjustSpeed();

            // update the viewangles
            UpdateViewAngles();
        }

        private void AdjustSpeed() {
            float speed;
            float rate;

            if (spectating) {
                speed = pm_spectatespeed.GetFloat();
                bobFrac = 0;
            } else if (noclip) {
                speed = pm_noclipspeed.GetFloat();
                bobFrac = 0;
            } else if (!physicsObj.OnLadder() && ((usercmd.buttons & BUTTON_RUN) != 0) && (usercmd.forwardmove != 0 || usercmd.rightmove != 0) && (usercmd.upmove >= 0)) {
                if (!gameLocal.isMultiplayer && !physicsObj.IsCrouching() && !PowerUpActive(ADRENALINE)) {
                    stamina -= MS2SEC(gameLocal.msec);
                }
                if (stamina < 0) {
                    stamina = 0;
                }
                if ((NOT(pm_stamina.GetFloat())) || (stamina > pm_staminathreshold.GetFloat())) {
                    bobFrac = 1.0f;
                } else if (pm_staminathreshold.GetFloat() <= 0.0001f) {
                    bobFrac = 0;
                } else {
                    bobFrac = stamina / pm_staminathreshold.GetFloat();
                }
                speed = pm_walkspeed.GetFloat() * (1.0f - bobFrac) + pm_runspeed.GetFloat() * bobFrac;
            } else {
                rate = pm_staminarate.GetFloat();

                // increase 25% faster when not moving
                if ((usercmd.forwardmove == 0) && (usercmd.rightmove == 0) && (!physicsObj.OnLadder() || (usercmd.upmove == 0))) {
                    rate *= 1.25f;
                }

                stamina += rate * MS2SEC(gameLocal.msec);
                if (stamina > pm_stamina.GetFloat()) {
                    stamina = pm_stamina.GetFloat();
                }
                speed = pm_walkspeed.GetFloat();
                bobFrac = 0;
            }

            speed *= PowerUpModifier(SPEED);

            if (influenceActive == INFLUENCE_LEVEL3) {
                speed *= 0.33f;
            }

            physicsObj.SetSpeed(speed, pm_crouchspeed.GetFloat());
        }

        private void AdjustBodyAngles() {
//            idMat3 lookAxis;
            idMat3 legsAxis;
            boolean blend;
            float diff;
            float frac;
            float upBlend;
            float forwardBlend;
            float downBlend;

            if (health < 0) {
                return;
            }

            blend = true;

            if (!physicsObj.HasGroundContacts()) {
                idealLegsYaw = 0;
                legsForward = true;
            } else if (usercmd.forwardmove < 0) {
                idealLegsYaw = idMath.AngleNormalize180(new idVec3(-usercmd.forwardmove, usercmd.rightmove, 0).ToYaw());
                legsForward = false;
            } else if (usercmd.forwardmove > 0) {
                idealLegsYaw = idMath.AngleNormalize180(new idVec3(usercmd.forwardmove, -usercmd.rightmove, 0).ToYaw());
                legsForward = true;
            } else if ((usercmd.rightmove != 0) && physicsObj.IsCrouching()) {
                if (!legsForward) {
                    idealLegsYaw = idMath.AngleNormalize180(new idVec3(idMath.Abs(usercmd.rightmove), usercmd.rightmove, 0).ToYaw());
                } else {
                    idealLegsYaw = idMath.AngleNormalize180(new idVec3(idMath.Abs(usercmd.rightmove), -usercmd.rightmove, 0).ToYaw());
                }
            } else if (usercmd.rightmove != 0) {
                idealLegsYaw = 0;
                legsForward = true;
            } else {
                legsForward = true;
                diff = idMath.Fabs(idealLegsYaw - legsYaw);
                idealLegsYaw = idealLegsYaw - idMath.AngleNormalize180(viewAngles.yaw - oldViewYaw);
                if (diff < 0.1f) {
                    legsYaw = idealLegsYaw;
                    blend = false;
                }
            }

            if (!physicsObj.IsCrouching()) {
                legsForward = true;
            }

            oldViewYaw = viewAngles.yaw;

            AI_TURN_LEFT._(false);
            AI_TURN_RIGHT._(false);
            if (idealLegsYaw < -45.0f) {
                idealLegsYaw = 0;
                AI_TURN_RIGHT._(true);
                blend = true;
            } else if (idealLegsYaw > 45.0f) {
                idealLegsYaw = 0;
                AI_TURN_LEFT._(true);
                blend = true;
            }

            if (blend) {
                legsYaw = legsYaw * 0.9f + idealLegsYaw * 0.1f;
            }
            legsAxis = new idAngles(0, legsYaw, 0).ToMat3();
            animator.SetJointAxis(hipJoint, JOINTMOD_WORLD, legsAxis);

            // calculate the blending between down, straight, and up
            frac = viewAngles.pitch / 90;
            if (frac > 0) {
                downBlend = frac;
                forwardBlend = 1.0f - frac;
                upBlend = 0;
            } else {
                downBlend = 0;
                forwardBlend = 1.0f + frac;
                upBlend = -frac;
            }

            animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(0, downBlend);
            animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(1, forwardBlend);
            animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(2, upBlend);

            animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(0, downBlend);
            animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(1, forwardBlend);
            animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(2, upBlend);
        }

        private void InitAASLocation() {
            int i;
            int num;
            idVec3 size;
            idBounds bounds = new idBounds();
            idAAS aas;
            idVec3 origin = new idVec3();

            GetFloorPos(64.0f, origin);

            num = gameLocal.NumAAS();
            aasLocation.SetGranularity(1);
            aasLocation.SetNum(num);
            for (i = 0; i < aasLocation.Num(); i++) {
                aasLocation.oSet(i, new aasLocation_t());
                aasLocation.oGet(i).areaNum = 0;
                aasLocation.oGet(i).pos = new idVec3(origin);
                aas = gameLocal.GetAAS(i);
                if (aas != null && aas.GetSettings() != null) {
                    size = aas.GetSettings().boundingBoxes[0].oGet(1);
                    bounds.oSet(0, size.oNegative());
                    size.z = 32.0f;
                    bounds.oSet(1, size);

                    aasLocation.oGet(i).areaNum = aas.PointReachableAreaNum(origin, bounds, AREA_REACHABLE_WALK);
                }
            }
        }

        private void SetAASLocation() {
            int i;
            int areaNum;
            idVec3 size;
            idBounds bounds = new idBounds();
            idAAS aas;
            idVec3 origin = new idVec3();

            if (!GetFloorPos(64.0f, origin)) {
                return;
            }

            for (i = 0; i < aasLocation.Num(); i++) {
                aas = gameLocal.GetAAS(i);
                if (NOT(aas)) {
                    continue;
                }

                size = aas.GetSettings().boundingBoxes[0].oGet(1);
                bounds.oSet(0, size.oNegative());
                size.z = 32.0f;
                bounds.oSet(1, size);

                areaNum = aas.PointReachableAreaNum(origin, bounds, AREA_REACHABLE_WALK);
                if (areaNum != 0) {
                    aasLocation.oGet(i).pos = origin;
                    aasLocation.oGet(i).areaNum = areaNum;
                }
            }
        }

        private void Move() {
            float newEyeOffset;
            idVec3 oldOrigin;
            idVec3 oldVelocity;
            idVec3 pushVelocity;

            // save old origin and velocity for crashlanding
            oldOrigin = physicsObj.GetOrigin();
            oldVelocity = physicsObj.GetLinearVelocity();
            pushVelocity = physicsObj.GetPushedLinearVelocity();

            // set physics variables
            physicsObj.SetMaxStepHeight(pm_stepsize.GetFloat());
            physicsObj.SetMaxJumpHeight(pm_jumpheight.GetFloat());

            if (noclip) {
                physicsObj.SetContents(0);
                physicsObj.SetMovementType(PM_NOCLIP);
            } else if (spectating) {
                physicsObj.SetContents(0);
                physicsObj.SetMovementType(PM_SPECTATOR);
            } else if (health <= 0) {
                physicsObj.SetContents(CONTENTS_CORPSE | CONTENTS_MONSTERCLIP);
                physicsObj.SetMovementType(PM_DEAD);
            } else if (gameLocal.inCinematic || gameLocal.GetCamera() != null || privateCameraView != null || (influenceActive == INFLUENCE_LEVEL2)) {
                physicsObj.SetContents(CONTENTS_BODY);
                physicsObj.SetMovementType(PM_FREEZE);
            } else {
                physicsObj.SetContents(CONTENTS_BODY);
                physicsObj.SetMovementType(PM_NORMAL);
            }

            if (spectating) {
                physicsObj.SetClipMask(MASK_DEADSOLID);
            } else if (health <= 0) {
                physicsObj.SetClipMask(MASK_DEADSOLID);
            } else {
                physicsObj.SetClipMask(MASK_PLAYERSOLID);
            }

            physicsObj.SetDebugLevel(g_debugMove.GetBool());
            physicsObj.SetPlayerInput(usercmd, viewAngles);

            // FIXME: physics gets disabled somehow
            BecomeActive(TH_PHYSICS);
            RunPhysics();

            // update our last valid AAS location for the AI
            SetAASLocation();

            if (spectating) {
                newEyeOffset = 0.0f;
            } else if (health <= 0) {
                newEyeOffset = pm_deadviewheight.GetFloat();
            } else if (physicsObj.IsCrouching()) {
                newEyeOffset = pm_crouchviewheight.GetFloat();
            } else if (GetBindMaster() != null && GetBindMaster().IsType(idAFEntity_Vehicle.class)) {
                newEyeOffset = 0.0f;
            } else {
                newEyeOffset = pm_normalviewheight.GetFloat();
            }

            if (EyeHeight() != newEyeOffset) {
                if (spectating) {
                    SetEyeHeight(newEyeOffset);
                } else {
                    // smooth out duck height changes
                    SetEyeHeight(EyeHeight() * pm_crouchrate.GetFloat() + newEyeOffset * (1.0f - pm_crouchrate.GetFloat()));
                }
            }

            if (noclip || gameLocal.inCinematic || (influenceActive == INFLUENCE_LEVEL2)) {
                AI_CROUCH._(false);
                AI_ONGROUND._(influenceActive == INFLUENCE_LEVEL2);
                AI_ONLADDER._(false);
                AI_JUMP._(false);
            } else {
                AI_CROUCH._(physicsObj.IsCrouching());
                AI_ONGROUND._(physicsObj.HasGroundContacts());
                AI_ONLADDER._(physicsObj.OnLadder());
                AI_JUMP._(physicsObj.HasJumped());

                // check if we're standing on top of a monster and give a push if we are
                idEntity groundEnt = physicsObj.GetGroundEntity();
                if (groundEnt != null && groundEnt.IsType(idAI.class)) {
                    idVec3 vel = physicsObj.GetLinearVelocity();
                    if (vel.ToVec2().LengthSqr() < 0.1f) {
                        vel.oSet(physicsObj.GetOrigin().ToVec2().oMinus(groundEnt.GetPhysics().GetAbsBounds().GetCenter().ToVec2()));
                        vel.ToVec2_NormalizeFast();
                        vel.ToVec2_oMulSet(pm_walkspeed.GetFloat());//TODO:ToVec2 back ref.
                    } else {
                        // give em a push in the direction they're going
                        vel.oMulSet(1.1f);
                    }
                    physicsObj.SetLinearVelocity(vel);
                }
            }

            if (AI_JUMP._()) {
                // bounce the view weapon
                loggedAccel_t acc = loggedAccel[currentLoggedAccel & (NUM_LOGGED_ACCELS - 1)];
                currentLoggedAccel++;
                acc.time = gameLocal.time;
                acc.dir.oSet(2, 200);
                acc.dir.oSet(0, acc.dir.oSet(1, 0));
            }

            if (AI_ONLADDER._()) {
                int old_rung = (int) (oldOrigin.z / LADDER_RUNG_DISTANCE);
                int new_rung = (int) (physicsObj.GetOrigin().z / LADDER_RUNG_DISTANCE);

                if (old_rung != new_rung) {
                    StartSound("snd_stepladder", SND_CHANNEL_ANY, 0, false, null);
                }
            }

            BobCycle(pushVelocity);
            CrashLand(oldOrigin, oldVelocity);
        }

        private void UpdatePowerUps() {
            int i;

            if (!gameLocal.isClient) {
                for (i = 0; i < MAX_POWERUPS; i++) {
                    if (PowerUpActive(i) && inventory.powerupEndTime[i] <= gameLocal.time) {
                        ClearPowerup(i);
                    }
                }
            }

            if (health > 0) {
                if (powerUpSkin != null) {
                    renderEntity.customSkin = powerUpSkin;
                } else {
                    renderEntity.customSkin = skin;
                }
            }

            if (healthPool != 0 && gameLocal.time > nextHealthPulse && !AI_DEAD._() && health > 0) {
                assert (!gameLocal.isClient);	// healthPool never be set on client
                int amt = (int) ((healthPool > 5) ? 5 : healthPool);
                health += amt;
                if (health > inventory.maxHealth) {
                    health = inventory.maxHealth;
                    healthPool = 0;
                } else {
                    healthPool -= amt;
                }
                nextHealthPulse = gameLocal.time + HEALTHPULSE_TIME;
                healthPulse = true;
            }
            if (BuildDefines.ID_DEMO_BUILD) {
                if (!gameLocal.inCinematic && influenceActive == 0 && g_skill.GetInteger() == 3 && gameLocal.time > nextHealthTake && !AI_DEAD._() && health > g_healthTakeLimit.GetInteger()) {
                    assert (!gameLocal.isClient);	// healthPool never be set on client
                    health -= g_healthTakeAmt.GetInteger();
                    if (health < g_healthTakeLimit.GetInteger()) {
                        health = g_healthTakeLimit.GetInteger();
                    }
                    nextHealthTake = gameLocal.time + g_healthTakeTime.GetInteger() * 1000;
                    healthTake = true;
                }
            }
        }

        private void UpdateDeathSkin(boolean state_hitch) {
            if (!(gameLocal.isMultiplayer || g_testDeath.GetBool())) {
                return;
            }
            if (health <= 0) {
                if (!doingDeathSkin) {
                    deathClearContentsTime = spawnArgs.GetInt("deathSkinTime");
                    doingDeathSkin = true;
                    renderEntity.noShadow = true;
                    if (state_hitch) {
                        renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = gameLocal.time * 0.001f - 2.0f;
                    } else {
                        renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = gameLocal.time * 0.001f;
                    }
                    UpdateVisuals();
                }

                // wait a bit before switching off the content
                if (deathClearContentsTime != 0 && gameLocal.time > deathClearContentsTime) {
                    SetCombatContents(false);
                    deathClearContentsTime = 0;
                }
            } else {
                renderEntity.noShadow = false;
                renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = 0.0f;
                UpdateVisuals();
                doingDeathSkin = false;
            }
        }

        private void ClearPowerup(int i) {

            if (gameLocal.isServer) {
                idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.WriteShort(i);
                msg.WriteBits(0, 1);
                ServerSendEvent(EVENT_POWERUP, msg, false, -1);
            }

            powerUpSkin = null;
            inventory.powerups &= ~(1 << i);
            inventory.powerupEndTime[ i] = 0;
            switch (i) {
                case BERSERK: {
                    StopSound(etoi(SND_CHANNEL_DEMONIC), false);
                    break;
                }
                case INVISIBILITY: {
                    if (weapon.GetEntity() != null) {
                        weapon.GetEntity().UpdateSkin();
                    }
                    break;
                }
            }
        }

        private void SetSpectateOrigin() {
            idVec3 neworig;

            neworig = GetPhysics().GetOrigin();
            neworig.oPluSet(2, EyeHeight());
            neworig.oPluSet(2, 25);
            SetOrigin(neworig);
        }

        /*
         ================
         idPlayer::ClearFocus

         Clears the focus cursor
         ================
         */
        private void ClearFocus() {
            focusCharacter = null;
            focusGUIent = null;
            focusUI = null;
            focusVehicle = null;
            talkCursor = 0;
        }

        /*
         ================
         idPlayer::UpdateFocus

         Searches nearby entities for interactive guis, possibly making one of them
         the focus and sending it a mouse move event
         ================
         */
        private void UpdateFocus() {
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            idClipModel clip;
            int listedClipModels;
            idEntity oldFocus;
            idEntity ent;
            idUserInterface oldUI;
            idAI oldChar;
            int oldTalkCursor;
            idAFEntity_Vehicle oldVehicle;
            int i, j;
            idVec3 start, end;
            boolean allowFocus;
            String command;
            trace_s[] trace = {null};
            guiPoint_t pt;
            idKeyValue kv;
            sysEvent_s ev;
            idUserInterface ui;

            if (gameLocal.inCinematic) {
                return;
            }

            // only update the focus character when attack button isn't pressed so players
            // can still chainsaw NPC's
            if (gameLocal.isMultiplayer || (NOT(focusCharacter) && ((usercmd.buttons & BUTTON_ATTACK) != 0))) {
                allowFocus = false;
            } else {
                allowFocus = true;
            }

            oldFocus = focusGUIent;
            oldUI = focusUI;
            oldChar = focusCharacter;
            oldTalkCursor = talkCursor;
            oldVehicle = focusVehicle;

            if (focusTime <= gameLocal.time) {
                ClearFocus();
            }

            // don't let spectators interact with GUIs
            if (spectating) {
                return;
            }

            start = GetEyePosition();
            end = start.oPlus(viewAngles.ToForward().oMultiply(80.0f));

            // player identification . names to the hud
            if (gameLocal.isMultiplayer && entityNumber == gameLocal.localClientNum) {
                idVec3 end2 = start.oPlus(viewAngles.ToForward().oMultiply(768.0f));
                gameLocal.clip.TracePoint(trace, start, end2, MASK_SHOT_BOUNDINGBOX, this);
                int iclient = -1;
                if ((trace[0].fraction < 1.0f) && (trace[0].c.entityNum < MAX_CLIENTS)) {
                    iclient = trace[0].c.entityNum;
                }
                if (MPAim != iclient) {
                    lastMPAim = MPAim;
                    MPAim = iclient;
                    lastMPAimTime = gameLocal.realClientTime;
                }
            }

            idBounds bounds = new idBounds(start);
            bounds.AddPoint(end);

            listedClipModels = gameLocal.clip.ClipModelsTouchingBounds(bounds, -1, clipModelList, MAX_GENTITIES);

            // no pretense at sorting here, just assume that there will only be one active
            // gui within range along the trace
            for (i = 0; i < listedClipModels; i++) {
                clip = clipModelList[ i];
                ent = clip.GetEntity();

                if (ent.IsHidden()) {
                    continue;
                }

                if (allowFocus) {
                    if (ent.IsType(idAFAttachment.class)) {
                        idEntity body = ((idAFAttachment) ent).GetBody();
                        if (body != null && body.IsType(idAI.class)
                                && etoi(((idAI) body).GetTalkState()) >= etoi(TALK_OK)) {
                            gameLocal.clip.TracePoint(trace, start, end, MASK_SHOT_RENDERMODEL, this);
                            if ((trace[0].fraction < 1.0f) && (trace[0].c.entityNum == ent.entityNumber)) {
                                ClearFocus();
                                focusCharacter = (idAI) body;
                                talkCursor = 1;
                                focusTime = gameLocal.time + FOCUS_TIME;
                                break;
                            }
                        }
                        continue;
                    }

                    if (ent.IsType(idAI.class)) {
                        if (etoi(((idAI) ent).GetTalkState()) >= etoi(TALK_OK)) {
                            gameLocal.clip.TracePoint(trace, start, end, MASK_SHOT_RENDERMODEL, this);
                            if ((trace[0].fraction < 1.0f) && (trace[0].c.entityNum == ent.entityNumber)) {
                                ClearFocus();
                                focusCharacter = (idAI) ent;
                                talkCursor = 1;
                                focusTime = gameLocal.time + FOCUS_TIME;
                                break;
                            }
                        }
                        continue;
                    }

                    if (ent.IsType(idAFEntity_Vehicle.class)) {
                        gameLocal.clip.TracePoint(trace, start, end, MASK_SHOT_RENDERMODEL, this);
                        if ((trace[0].fraction < 1.0f) && (trace[0].c.entityNum == ent.entityNumber)) {
                            ClearFocus();
                            focusVehicle = (idAFEntity_Vehicle) ent;
                            focusTime = gameLocal.time + FOCUS_TIME;
                            break;
                        }
                        continue;
                    }
                }

                if (NOT(ent.GetRenderEntity()) || NOT(ent.GetRenderEntity().gui[0]) || !ent.GetRenderEntity().gui[0].IsInteractive()) {
                    continue;
                }

                if (ent.spawnArgs.GetBool("inv_item")) {
                    // don't allow guis on pickup items focus
                    continue;
                }

                pt = gameRenderWorld.GuiTrace(ent.GetModelDefHandle(), start, end);
                if (pt.x != -1) {
                    // we have a hit
                    renderEntity_s focusGUIrenderEntity = ent.GetRenderEntity();
                    if (NOT(focusGUIrenderEntity)) {
                        continue;
                    }

                    if (pt.guiId == 1) {
                        ui = focusGUIrenderEntity.gui[0];
                    } else if (pt.guiId == 2) {
                        ui = focusGUIrenderEntity.gui[1];
                    } else {
                        ui = focusGUIrenderEntity.gui[2];
                    }

                    if (ui == null) {
                        continue;
                    }

                    ClearFocus();
                    focusGUIent = ent;
                    focusUI = ui;

                    if (oldFocus != ent) {
                        // new activation
                        // going to see if we have anything in inventory a gui might be interested in
                        // need to enumerate inventory items
                        focusUI.SetStateInt("inv_count", inventory.items.Num());
                        for (j = 0; j < inventory.items.Num(); j++) {
                            idDict item = inventory.items.oGet(j);
                            final String iname = item.GetString("inv_name");
                            final String iicon = item.GetString("inv_icon");
                            final String itext = item.GetString("inv_text");

                            focusUI.SetStateString(va("inv_name_%d", j), iname);
                            focusUI.SetStateString(va("inv_icon_%d", j), iicon);
                            focusUI.SetStateString(va("inv_text_%d", j), itext);
                            kv = item.MatchPrefix("inv_id", null);
                            if (kv != null) {
                                focusUI.SetStateString(va("inv_id_%d", j), kv.GetValue().toString());
                            }
                            focusUI.SetStateInt(iname, 1);
                        }

                        for (j = 0; j < inventory.pdaSecurity.Num(); j++) {
                            final String p = inventory.pdaSecurity.oGet(j).toString();
                            if (isNotNullOrEmpty(p)) {
                                focusUI.SetStateInt(p, 1);
                            }
                        }

                        int staminapercentage = (int) (100.0f * stamina / pm_stamina.GetFloat());
                        focusUI.SetStateString("player_health", va("%d", health));
                        focusUI.SetStateString("player_stamina", va("%d%%", staminapercentage));
                        focusUI.SetStateString("player_armor", va("%d%%", inventory.armor));

                        kv = focusGUIent.spawnArgs.MatchPrefix("gui_parm", null);
                        while (kv != null) {
                            focusUI.SetStateString(kv.GetKey().toString(), kv.GetValue().toString());
                            kv = focusGUIent.spawnArgs.MatchPrefix("gui_parm", kv);
                        }
                    }

                    // clamp the mouse to the corner
                    ev = sys.GenerateMouseMoveEvent(-2000, -2000);
                    command = focusUI.HandleEvent(ev, gameLocal.time);
                    HandleGuiCommands(focusGUIent, command);

                    // move to an absolute position
                    ev = sys.GenerateMouseMoveEvent((int) (pt.x * SCREEN_WIDTH), (int) (pt.y * SCREEN_HEIGHT));
                    command = focusUI.HandleEvent(ev, gameLocal.time);
                    HandleGuiCommands(focusGUIent, command);
                    focusTime = gameLocal.time + FOCUS_GUI_TIME;
                    break;
                }
            }

            if (focusGUIent != null && focusUI != null) {
                if (NOT(oldFocus) || !oldFocus.equals(focusGUIent)) {
                    command = focusUI.Activate(true, gameLocal.time);
                    HandleGuiCommands(focusGUIent, command);
                    StartSound("snd_guienter", SND_CHANNEL_ANY, 0, false, null);
                    // HideTip();
                    // HideObjective();
                }
            } else if (oldFocus != null && oldUI != null) {
                command = oldUI.Activate(false, gameLocal.time);
                HandleGuiCommands(oldFocus, command);
                StartSound("snd_guiexit", SND_CHANNEL_ANY, 0, false, null);
            }

            if (cursor != null && (oldTalkCursor != talkCursor)) {
                cursor.SetStateInt("talkcursor", talkCursor);
            }

            if (oldChar != focusCharacter && hud != null) {
                if (focusCharacter != null) {
                    hud.SetStateString("npc", focusCharacter.spawnArgs.GetString("npc_name", "Joe"));
                    hud.HandleNamedEvent("showNPC");
                    // HideTip();
                    // HideObjective();
                } else {
                    hud.SetStateString("npc", "");
                    hud.HandleNamedEvent("hideNPC");
                }
            }
        }

        /*
         ================
         idPlayer::UpdateLocation

         Searches nearby locations 
         ================
         */
        private void UpdateLocation() {
            if (hud != null) {
                idLocationEntity locationEntity = gameLocal.LocationForPoint(GetEyePosition());
                if (locationEntity != null) {
                    hud.SetStateString("location", locationEntity.GetLocation());
                } else {
                    hud.SetStateString("location", common.GetLanguageDict().GetString("#str_02911"));
                }
            }
        }

        private idUserInterface ActiveGui() {
            if (objectiveSystemOpen) {
                return objectiveSystem;
            }

            return focusUI;
        }

        private void UpdatePDAInfo(boolean updatePDASel) {
            int j, sel;

            if (objectiveSystem == null) {
                return;
            }

            assert (hud != null);

            int currentPDA = objectiveSystem.State().GetInt("listPDA_sel_0", "0");
            if (currentPDA == -1) {
                currentPDA = 0;
            }

            if (updatePDASel) {
                objectiveSystem.SetStateInt("listPDAVideo_sel_0", 0);
                objectiveSystem.SetStateInt("listPDAEmail_sel_0", 0);
                objectiveSystem.SetStateInt("listPDAAudio_sel_0", 0);
            }

            if (currentPDA > 0) {
                currentPDA = inventory.pdas.Num() - currentPDA;
            }

            // Mark in the bit array that this pda has been read
            if (currentPDA < 128) {
                inventory.pdasViewed[currentPDA >> 5] |= 1 << (currentPDA & 31);
            }

            pdaAudio.oSet("");
            pdaVideo.oSet("");
            pdaVideoWave.oSet("");
            String name, data, preview, info, wave;
            for (j = 0; j < MAX_PDAS; j++) {
                objectiveSystem.SetStateString(va("listPDA_item_%d", j), "");
            }
            for (j = 0; j < MAX_PDA_ITEMS; j++) {
                objectiveSystem.SetStateString(va("listPDAVideo_item_%d", j), "");
                objectiveSystem.SetStateString(va("listPDAAudio_item_%d", j), "");
                objectiveSystem.SetStateString(va("listPDAEmail_item_%d", j), "");
                objectiveSystem.SetStateString(va("listPDASecurity_item_%d", j), "");
            }
            for (j = 0; j < inventory.pdas.Num(); j++) {

                final idDeclPDA pda = (idDeclPDA) declManager.FindType(DECL_PDA, inventory.pdas.oGet(j), false);

                if (pda == null) {
                    continue;
                }

                int index = inventory.pdas.Num() - j;
                if (j == 0) {
                    // Special case for the first PDA
                    index = 0;
                }

                if (j != currentPDA && j < 128 && (inventory.pdasViewed[j >> 5] & (1 << (j & 31))) != 0) {
                    // This pda has been read already, mark in gray
                    objectiveSystem.SetStateString(va("listPDA_item_%d", index), va(S_COLOR_GRAY, "%s", pda.GetPdaName()));
                } else {
                    // This pda has not been read yet
                    objectiveSystem.SetStateString(va("listPDA_item_%d", index), pda.GetPdaName());
                }

                String security = pda.GetSecurity();
                if (j == currentPDA || (currentPDA == 0 && security != null && !security.isEmpty())) {
                    if (security.isEmpty()) {
                        security = common.GetLanguageDict().GetString("#str_00066");
                    }
                    objectiveSystem.SetStateString("PDASecurityClearance", security);
                }

                if (j == currentPDA) {

                    objectiveSystem.SetStateString("pda_icon", pda.GetIcon());
                    objectiveSystem.SetStateString("pda_id", pda.GetID());
                    objectiveSystem.SetStateString("pda_title", pda.GetTitle());

                    if (j == 0) {
                        // Selected, personal pda
                        // Add videos
                        if (updatePDASel || !inventory.pdaOpened) {
                            objectiveSystem.HandleNamedEvent("playerPDAActive");
                            objectiveSystem.SetStateString("pda_personal", "1");
                            inventory.pdaOpened = true;
                        }
                        objectiveSystem.SetStateString("pda_location", hud.State().GetString("location"));
                        objectiveSystem.SetStateString("pda_name", cvarSystem.GetCVarString("ui_name"));
                        AddGuiPDAData(DECL_VIDEO, "listPDAVideo", pda, objectiveSystem);
                        sel = objectiveSystem.State().GetInt("listPDAVideo_sel_0", "0");
                        idDeclVideo vid = null;
                        if (sel >= 0 && sel < inventory.videos.Num()) {
                            vid = (idDeclVideo) declManager.FindType(DECL_VIDEO, inventory.videos.oGet(sel), false);
                        }
                        if (vid != null) {
                            pdaVideo.oSet(vid.GetRoq());
                            pdaVideoWave.oSet(vid.GetWave());
                            objectiveSystem.SetStateString("PDAVideoTitle", vid.GetVideoName());
                            objectiveSystem.SetStateString("PDAVideoVid", vid.GetRoq());
                            objectiveSystem.SetStateString("PDAVideoIcon", vid.GetPreview());
                            objectiveSystem.SetStateString("PDAVideoInfo", vid.GetInfo());
                        } else {
                            //FIXME: need to precache these in the player def
                            objectiveSystem.SetStateString("PDAVideoVid", "sound/vo/video/welcome.tga");
                            objectiveSystem.SetStateString("PDAVideoIcon", "sound/vo/video/welcome.tga");
                            objectiveSystem.SetStateString("PDAVideoTitle", "");
                            objectiveSystem.SetStateString("PDAVideoInfo", "");
                        }
                    } else {
                        // Selected, non-personal pda
                        // Add audio logs
                        if (updatePDASel) {
                            objectiveSystem.HandleNamedEvent("playerPDANotActive");
                            objectiveSystem.SetStateString("pda_personal", "0");
                            inventory.pdaOpened = true;
                        }
                        objectiveSystem.SetStateString("pda_location", pda.GetPost());
                        objectiveSystem.SetStateString("pda_name", pda.GetFullName());
                        int audioCount = AddGuiPDAData(DECL_AUDIO, "listPDAAudio", pda, objectiveSystem);
                        objectiveSystem.SetStateInt("audioLogCount", audioCount);
                        sel = objectiveSystem.State().GetInt("listPDAAudio_sel_0", "0");
                        idDeclAudio aud = null;
                        if (sel >= 0) {
                            aud = pda.GetAudioByIndex(sel);
                        }
                        if (aud != null) {
                            pdaAudio.oSet(aud.GetWave());
                            objectiveSystem.SetStateString("PDAAudioTitle", aud.GetAudioName());
                            objectiveSystem.SetStateString("PDAAudioIcon", aud.GetPreview());
                            objectiveSystem.SetStateString("PDAAudioInfo", aud.GetInfo());
                        } else {
                            objectiveSystem.SetStateString("PDAAudioIcon", "sound/vo/video/welcome.tga");
                            objectiveSystem.SetStateString("PDAAutioTitle", "");
                            objectiveSystem.SetStateString("PDAAudioInfo", "");
                        }
                    }
                    // add emails
                    name = "";
                    data = "";
                    int numEmails = pda.GetNumEmails();
                    if (numEmails > 0) {
                        AddGuiPDAData(DECL_EMAIL, "listPDAEmail", pda, objectiveSystem);
                        sel = objectiveSystem.State().GetInt("listPDAEmail_sel_0", "-1");
                        if (sel >= 0 && sel < numEmails) {
                            final idDeclEmail email = pda.GetEmailByIndex(sel);
                            name = email.GetSubject();
                            data = email.GetBody();
                        }
                    }
                    objectiveSystem.SetStateString("PDAEmailTitle", name);
                    objectiveSystem.SetStateString("PDAEmailText", data);
                }
            }
            if (objectiveSystem.State().GetInt("listPDA_sel_0", "-1") == -1) {
                objectiveSystem.SetStateInt("listPDA_sel_0", 0);
            }
            objectiveSystem.StateChanged(gameLocal.time);
        }

        private int AddGuiPDAData(final declType_t dataType, final String listName, final idDeclPDA src, idUserInterface gui) {
            int c, i;
            String work;
            if (dataType == DECL_EMAIL) {
                c = src.GetNumEmails();
                for (i = 0; i < c; i++) {
                    final idDeclEmail email = src.GetEmailByIndex(i);
                    if (email == null) {
                        work = va("-\tEmail %d not found\t-", i);
                    } else {
                        work = email.GetFrom();
                        work += "\t";
                        work += email.GetSubject();
                        work += "\t";
                        work += email.GetDate();
                    }
                    gui.SetStateString(va("%s_item_%d", listName, i), work);
                }
                return c;
            } else if (dataType == DECL_AUDIO) {
                c = src.GetNumAudios();
                for (i = 0; i < c; i++) {
                    final idDeclAudio audio = src.GetAudioByIndex(i);
                    if (audio == null) {
                        work = va("Audio Log %d not found", i);
                    } else {
                        work = audio.GetAudioName();
                    }
                    gui.SetStateString(va("%s_item_%d", listName, i), work);
                }
                return c;
            } else if (dataType == DECL_VIDEO) {
                c = inventory.videos.Num();
                for (i = 0; i < c; i++) {
                    final idDeclVideo video = GetVideo(i);
                    if (video == null) {
                        work = va("Video CD %s not found", inventory.videos.oGet(i).toString());
                    } else {
                        work = video.GetVideoName();
                    }
                    gui.SetStateString(va("%s_item_%d", listName, i), work);
                }
                return c;
            }
            return 0;
        }
//
//        private void ExtractEmailInfo(final idStr email, final String scan, idStr out);
//

        private void UpdateObjectiveInfo() {
            if (objectiveSystem == null) {
                return;
            }
            objectiveSystem.SetStateString("objective1", "");
            objectiveSystem.SetStateString("objective2", "");
            objectiveSystem.SetStateString("objective3", "");
            for (int i = 0; i < inventory.objectiveNames.Num(); i++) {
                objectiveSystem.SetStateString(va("objective%d", i + 1), "1");
                objectiveSystem.SetStateString(va("objectivetitle%d", i + 1), inventory.objectiveNames.oGet(i).title.toString());
                objectiveSystem.SetStateString(va("objectivetext%d", i + 1), inventory.objectiveNames.oGet(i).text.toString());
                objectiveSystem.SetStateString(va("objectiveshot%d", i + 1), inventory.objectiveNames.oGet(i).screenshot.toString());
            }
            objectiveSystem.StateChanged(gameLocal.time);
        }

        private void UseVehicle() {
            trace_s[] trace = {null};
            idVec3 start, end;
            idEntity ent;

            if (GetBindMaster() != null && GetBindMaster().IsType(idAFEntity_Vehicle.class)) {
                Show();
                ((idAFEntity_Vehicle) GetBindMaster()).Use(this);
            } else {
                start = GetEyePosition();
                end = start.oPlus(viewAngles.ToForward().oMultiply(80.0f));
                gameLocal.clip.TracePoint(trace, start, end, MASK_SHOT_RENDERMODEL, this);
                if (trace[0].fraction < 1.0f) {
                    ent = gameLocal.entities[ trace[0].c.entityNum];
                    if (ent != null && ent.IsType(idAFEntity_Vehicle.class)) {
                        Hide();
                        ((idAFEntity_Vehicle) ent).Use(this);
                    }
                }
            }
        }

        private void Event_GetButtons() {
            idThread.ReturnInt(usercmd.buttons);
        }

        private void Event_GetMove() {
            idVec3 move = new idVec3(usercmd.forwardmove, usercmd.rightmove, usercmd.upmove);
            idThread.ReturnVector(move);
        }

        private void Event_GetViewAngles() {
            idThread.ReturnVector(new idVec3(viewAngles.oGet(0), viewAngles.oGet(1), viewAngles.oGet(2)));
        }

        private void Event_StopFxFov() {
            fxFov = false;
        }

        private void Event_EnableWeapon() {
            hiddenWeapon = gameLocal.world.spawnArgs.GetBool("no_Weapons");
            weaponEnabled = true;
            if (weapon.GetEntity() != null) {
                weapon.GetEntity().ExitCinematic();
            }
        }

        private void Event_DisableWeapon() {
            hiddenWeapon = gameLocal.world.spawnArgs.GetBool("no_Weapons");
            weaponEnabled = false;
            if (weapon.GetEntity() != null) {
                weapon.GetEntity().EnterCinematic();
            }
        }

        private void Event_GetCurrentWeapon() {
            final String weapon;

            if (currentWeapon >= 0) {
                weapon = spawnArgs.GetString(va("def_weapon%d", currentWeapon));
                idThread.ReturnString(weapon);
            } else {
                idThread.ReturnString("");
            }
        }

        private void Event_GetPreviousWeapon() {
            final String weapon;

            if (previousWeapon >= 0) {
                int pw = (gameLocal.world.spawnArgs.GetBool("no_Weapons")) ? 0 : previousWeapon;
                weapon = spawnArgs.GetString(va("def_weapon%d", pw));
                idThread.ReturnString(weapon);
            } else {
                idThread.ReturnString(spawnArgs.GetString("def_weapon0"));
            }
        }

        private void Event_SelectWeapon(final idEventArg<String> weaponName) {
            int i;
            int weaponNum;

            if (gameLocal.isClient) {
                gameLocal.Warning("Cannot switch weapons from script in multiplayer");
                return;
            }

            if (hiddenWeapon && gameLocal.world.spawnArgs.GetBool("no_Weapons")) {
                idealWeapon = weapon_fists;
                weapon.GetEntity().HideWeapon();
                return;
            }

            weaponNum = -1;
            for (i = 0; i < MAX_WEAPONS; i++) {
                if ((inventory.weapons & (1 << i)) != 0) {
                    final String weap = spawnArgs.GetString(va("def_weapon%d", i));
                    if (NOT(idStr.Cmp(weap, weaponName.value))) {
                        weaponNum = i;
                        break;
                    }
                }
            }

            if (weaponNum < 0) {
                gameLocal.Warning("%s is not carrying weapon '%s'", name, weaponName.value);
                return;
            }

            hiddenWeapon = false;
            idealWeapon = weaponNum;

            UpdateHudWeapon();
        }

        private void Event_GetWeaponEntity() {
            idThread.ReturnEntity(weapon.GetEntity());
        }

        private void Event_OpenPDA() {
            if (!gameLocal.isMultiplayer) {
                TogglePDA();
            }
        }
//
//        private void Event_PDAAvailable();
//

        private void Event_InPDA() {
            idThread.ReturnInt(objectiveSystemOpen);
        }

        private void Event_ExitTeleporter() {
            idEntity exitEnt;
            float pushVel;

            // verify and setup
            exitEnt = teleportEntity.GetEntity();
            if (NOT(exitEnt)) {
                common.DPrintf("Event_ExitTeleporter player %d while not being teleported\n", entityNumber);
                return;
            }

            pushVel = exitEnt.spawnArgs.GetFloat("push", "300");

            if (gameLocal.isServer) {
                ServerSendEvent(EVENT_EXIT_TELEPORTER, null, false, -1);
            }

            SetPrivateCameraView(null);
            // setup origin and push according to the exit target
            SetOrigin(exitEnt.GetPhysics().GetOrigin().oPlus(new idVec3(0, 0, CM_CLIP_EPSILON)));
            SetViewAngles(exitEnt.GetPhysics().GetAxis().ToAngles());
            physicsObj.SetLinearVelocity(exitEnt.GetPhysics().GetAxis().oGet(0).oMultiply(pushVel));
            physicsObj.ClearPushedVelocity();
            // teleport fx
            playerView.Flash(colorWhite, 120);

            // clear the ik heights so model doesn't appear in the wrong place
            walkIK.EnableAll();

            UpdateVisuals();

            StartSound("snd_teleport_exit", SND_CHANNEL_ANY, 0, false, null);

            if (teleportKiller != -1) {
                // we got killed while being teleported
                Damage(gameLocal.entities[ teleportKiller], gameLocal.entities[ teleportKiller], getVec3_origin(), "damage_telefrag", 1.0f, INVALID_JOINT);
                teleportKiller = -1;
            } else {
                // kill anything that would have waited at teleport exit
                gameLocal.KillBox(this);
            }
            teleportEntity.oSet(null);
        }

        private void Event_HideTip() {
            HideTip();
        }

        private void Event_LevelTrigger() {
            idStr mapName = new idStr(gameLocal.GetMapName());
            mapName.StripPath();
            mapName.StripFileExtension();
            for (int i = inventory.levelTriggers.Num() - 1; i >= 0; i--) {
                if (idStr.Icmp(mapName, inventory.levelTriggers.oGet(i).levelName) == 0) {
                    idEntity ent = gameLocal.FindEntity(inventory.levelTriggers.oGet(i).triggerName);
                    if (ent != null) {
                        ent.PostEventMS(EV_Activate, 1, this);
                    }
                }
            }
        }

        private void Event_Gibbed() {
        }

        private void Event_GetIdealWeapon() {
            final String weapon;

            if (idealWeapon >= 0) {
                weapon = spawnArgs.GetString(va("def_weapon%d", idealWeapon));
                idThread.ReturnString(weapon);
            } else {
                idThread.ReturnString("");
            }
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
