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
import neo.Game.Game_local.idGameLocal;
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
        public idList<idItemInfo>      pickupItemNames = new idList<idItemInfo>(idItemInfo.class);
        public idList<idObjectiveInfo> objectiveNames  = new idList<>();

        public idInventory() {
            this.items = new idList<>();
            this.pdas = new idStrList();
            this.pdaSecurity = new idStrList();
            this.videos = new idStrList();
            this.emails = new idStrList();
            this.levelTriggers = new idList<>();
            Clear();
        }
        // ~idInventory() { Clear(); }
        // save games

        // archives object for save game file
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(this.maxHealth);
            savefile.WriteInt(this.weapons);
            savefile.WriteInt(this.powerups);
            savefile.WriteInt(this.armor);
            savefile.WriteInt(this.maxarmor);
            savefile.WriteInt(this.ammoPredictTime);
            savefile.WriteInt(this.deplete_armor);
            savefile.WriteFloat(this.deplete_rate);
            savefile.WriteInt(this.deplete_ammount);
            savefile.WriteInt(this.nextArmorDepleteTime);

            for (i = 0; i < AMMO_NUMTYPES; i++) {
                savefile.WriteInt(this.ammo[i]);
            }
            for (i = 0; i < MAX_WEAPONS; i++) {
                savefile.WriteInt(this.clip[i]);
            }
            for (i = 0; i < MAX_POWERUPS; i++) {
                savefile.WriteInt(this.powerupEndTime[i]);
            }

            savefile.WriteInt(this.items.Num());
            for (i = 0; i < this.items.Num(); i++) {
                savefile.WriteDict(this.items.oGet(i));
            }

            savefile.WriteInt(this.pdasViewed[0]);
            savefile.WriteInt(this.pdasViewed[1]);
            savefile.WriteInt(this.pdasViewed[2]);
            savefile.WriteInt(this.pdasViewed[3]);

            savefile.WriteInt(this.selPDA);
            savefile.WriteInt(this.selVideo);
            savefile.WriteInt(this.selEMail);
            savefile.WriteInt(this.selAudio);
            savefile.WriteBool(this.pdaOpened);
            savefile.WriteBool(this.turkeyScore);

            savefile.WriteInt(this.pdas.Num());
            for (i = 0; i < this.pdas.Num(); i++) {
                savefile.WriteString(this.pdas.oGet(i));
            }

            savefile.WriteInt(this.pdaSecurity.Num());
            for (i = 0; i < this.pdaSecurity.Num(); i++) {
                savefile.WriteString(this.pdaSecurity.oGet(i));
            }

            savefile.WriteInt(this.videos.Num());
            for (i = 0; i < this.videos.Num(); i++) {
                savefile.WriteString(this.videos.oGet(i));
            }

            savefile.WriteInt(this.emails.Num());
            for (i = 0; i < this.emails.Num(); i++) {
                savefile.WriteString(this.emails.oGet(i));
            }

            savefile.WriteInt(this.nextItemPickup);
            savefile.WriteInt(this.nextItemNum);
            savefile.WriteInt(this.onePickupTime);

            savefile.WriteInt(this.pickupItemNames.Num());
            for (i = 0; i < this.pickupItemNames.Num(); i++) {
                savefile.WriteString(this.pickupItemNames.oGet(i).icon);
                savefile.WriteString(this.pickupItemNames.oGet(i).name);
            }

            savefile.WriteInt(this.objectiveNames.Num());
            for (i = 0; i < this.objectiveNames.Num(); i++) {
                savefile.WriteString(this.objectiveNames.oGet(i).screenshot);
                savefile.WriteString(this.objectiveNames.oGet(i).text);
                savefile.WriteString(this.objectiveNames.oGet(i).title);
            }

            savefile.WriteInt(this.levelTriggers.Num());
            for (i = 0; i < this.levelTriggers.Num(); i++) {
                savefile.WriteString(this.levelTriggers.oGet(i).levelName);
                savefile.WriteString(this.levelTriggers.oGet(i).triggerName);
            }

            savefile.WriteBool(this.ammoPulse);
            savefile.WriteBool(this.weaponPulse);
            savefile.WriteBool(this.armorPulse);

            savefile.WriteInt(this.lastGiveTime);
        }

        // unarchives object from save game file
        public void Restore(idRestoreGame savefile) {
            int i;
            int num;

            this.maxHealth = savefile.ReadInt();
            this.weapons = savefile.ReadInt();
            this.powerups = savefile.ReadInt();
            this.armor = savefile.ReadInt();
            this.maxarmor = savefile.ReadInt();
            this.ammoPredictTime = savefile.ReadInt();
            this.deplete_armor = savefile.ReadInt();
            this.deplete_rate = savefile.ReadFloat();
            this.deplete_ammount = savefile.ReadInt();
            this.nextArmorDepleteTime = savefile.ReadInt();

            for (i = 0; i < AMMO_NUMTYPES; i++) {
                this.ammo[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_WEAPONS; i++) {
                this.clip[i] = savefile.ReadInt();
            }
            for (i = 0; i < MAX_POWERUPS; i++) {
                this.powerupEndTime[i] = savefile.ReadInt();
            }

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final idDict itemdict = new idDict();

                savefile.ReadDict(itemdict);
                this.items.Append(itemdict);
            }

            // pdas
            this.pdasViewed[0] = savefile.ReadInt();
            this.pdasViewed[1] = savefile.ReadInt();
            this.pdasViewed[2] = savefile.ReadInt();
            this.pdasViewed[3] = savefile.ReadInt();

            this.selPDA = savefile.ReadInt();
            this.selVideo = savefile.ReadInt();
            this.selEMail = savefile.ReadInt();
            this.selAudio = savefile.ReadInt();
            this.pdaOpened = savefile.ReadBool();
            this.turkeyScore = savefile.ReadBool();

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final idStr strPda = new idStr();
                savefile.ReadString(strPda);
                this.pdas.Append(strPda);
            }

            // pda security clearances
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final idStr invName = new idStr();
                savefile.ReadString(invName);
                this.pdaSecurity.Append(invName);
            }

            // videos
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final idStr strVideo = new idStr();
                savefile.ReadString(strVideo);
                this.videos.Append(strVideo);
            }

            // email
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final idStr strEmail = new idStr();
                savefile.ReadString(strEmail);
                this.emails.Append(strEmail);
            }

            this.nextItemPickup = savefile.ReadInt();
            this.nextItemNum = savefile.ReadInt();
            this.onePickupTime = savefile.ReadInt();
            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final idItemInfo info = new idItemInfo();

                savefile.ReadString(info.icon);
                savefile.ReadString(info.name);

                this.pickupItemNames.Append(info);
            }

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final idObjectiveInfo obj = new idObjectiveInfo();

                savefile.ReadString(obj.screenshot);
                savefile.ReadString(obj.text);
                savefile.ReadString(obj.title);

                this.objectiveNames.Append(obj);
            }

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final idLevelTriggerInfo lti = new idLevelTriggerInfo();
                savefile.ReadString(lti.levelName);
                savefile.ReadString(lti.triggerName);
                this.levelTriggers.Append(lti);
            }

            this.ammoPulse = savefile.ReadBool();
            this.weaponPulse = savefile.ReadBool();
            this.armorPulse = savefile.ReadBool();

            this.lastGiveTime = savefile.ReadInt();
        }

        public void Clear() {
            this.maxHealth = 0;
            this.weapons = 0;
            this.powerups = 0;
            this.armor = 0;
            this.maxarmor = 0;
            this.deplete_armor = 0;
            this.deplete_rate = 0;
            this.deplete_ammount = 0;
            this.nextArmorDepleteTime = 0;

//	memset( ammo, 0, sizeof( ammo ) );
            ClearPowerUps();

            // set to -1 so that the gun knows to have a full clip the first time we get it and at the start of the level
//	memset( clip, -1, sizeof( clip ) );
            Arrays.asList(this.clip, -1);

            this.items.DeleteContents(true);
//	memset(pdasViewed, 0, 4 * sizeof( pdasViewed[0] ) );
            this.pdas.Clear();
            this.videos.Clear();
            this.emails.Clear();
            this.selVideo = 0;
            this.selEMail = 0;
            this.selPDA = 0;
            this.selAudio = 0;
            this.pdaOpened = false;
            this.turkeyScore = false;

            this.levelTriggers.Clear();

            this.nextItemPickup = 0;
            this.nextItemNum = 1;
            this.onePickupTime = 0;
            this.pickupItemNames.Clear();
            this.objectiveNames.Clear();

            this.ammoPredictTime = 0;

            this.lastGiveTime = 0;

            this.ammoPulse = false;
            this.weaponPulse = false;
            this.armorPulse = false;
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
            this.powerups |= 1 << powerup;
            this.powerupEndTime[powerup] = gameLocal.time + msec;
        }

        public void ClearPowerUps() {
            int i;
            for (i = 0; i < MAX_POWERUPS; i++) {
                this.powerupEndTime[i] = 0;
            }
            this.powerups = 0;
        }

        public void GetPersistantData(idDict dict) {
            int i;
            int num;
            idDict item;
            String key;
            idKeyValue kv;
            String name;

            // armor
            dict.SetInt("armor", this.armor);

            // don't bother with powerups, maxhealth, maxarmor, or the clip
            // ammo
            for (i = 0; i < AMMO_NUMTYPES; i++) {
                name = idWeapon.GetAmmoNameForNum(i);
                if (name != null) {
                    dict.SetInt(name, this.ammo[i]);
                }
            }

            // items
            num = 0;
            for (i = 0; i < this.items.Num(); i++) {
                item = this.items.oGet(i);

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
                dict.SetInt(va("pdasViewed_%d", i), this.pdasViewed[i]);
            }

            dict.SetInt("selPDA", this.selPDA);
            dict.SetInt("selVideo", this.selVideo);
            dict.SetInt("selEmail", this.selEMail);
            dict.SetInt("selAudio", this.selAudio);
            dict.SetInt("pdaOpened", btoi(this.pdaOpened));
            dict.SetInt("turkeyScore", btoi(this.turkeyScore));

            // pdas
            for (i = 0; i < this.pdas.Num(); i++) {
                key = String.format("pda_%d", i);
                dict.Set(key, this.pdas.oGet(i));
            }
            dict.SetInt("pdas", this.pdas.Num());

            // video cds
            for (i = 0; i < this.videos.Num(); i++) {
                key = String.format("video_%d", i);
                dict.Set(key, this.videos.oGet(i));
            }
            dict.SetInt("videos", this.videos.Num());

            // emails
            for (i = 0; i < this.emails.Num(); i++) {
                key = String.format("email_%d", i);
                dict.Set(key, this.emails.oGet(i));
            }
            dict.SetInt("emails", this.emails.Num());

            // weapons
            dict.SetInt("weapon_bits", this.weapons);

            dict.SetInt("levelTriggers", this.levelTriggers.Num());
            for (i = 0; i < this.levelTriggers.Num(); i++) {
                key = String.format("levelTrigger_Level_%d", i);
                dict.Set(key, this.levelTriggers.oGet(i).levelName);
                key = String.format("levelTrigger_Trigger_%d", i);
                dict.Set(key, this.levelTriggers.oGet(i).triggerName);
            }
        }

        public void RestoreInventory(idPlayer owner, final idDict dict) {
            int i;
            int num;
            idDict item;
            final idStr key = new idStr();
            String itemname;
            idKeyValue kv;
            String name;

            Clear();

            // health/armor
            this.maxHealth = dict.GetInt("maxhealth", "100");
            this.armor = dict.GetInt("armor", "50");
            this.maxarmor = dict.GetInt("maxarmor", "100");
            this.deplete_armor = dict.GetInt("deplete_armor", "0");
            this.deplete_rate = dict.GetFloat("deplete_rate", "2.0");
            this.deplete_ammount = dict.GetInt("deplete_ammount", "1");

            // the clip and powerups aren't restored
            // ammo
            for (i = 0; i < AMMO_NUMTYPES; i++) {
                name = idWeapon.GetAmmoNameForNum(i);
                if (name != null) {
                    this.ammo[i] = dict.GetInt(name);
                }
            }

            // items
            num = dict.GetInt("items");
            this.items.SetNum(num);
            for (i = 0; i < num; i++) {
                item = new idDict();
                this.items.oSet(i, item);
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
                this.pdasViewed[i] = dict.GetInt(va("pdasViewed_%d", i));
            }

            this.selPDA = dict.GetInt("selPDA");
            this.selEMail = dict.GetInt("selEmail");
            this.selVideo = dict.GetInt("selVideo");
            this.selAudio = dict.GetInt("selAudio");
            this.pdaOpened = dict.GetBool("pdaOpened");
            this.turkeyScore = dict.GetBool("turkeyScore");

            // pdas
            num = dict.GetInt("pdas");
            this.pdas.SetNum(num);
            for (i = 0; i < num; i++) {
                itemname = String.format("pda_%d", i);
                this.pdas.oSetType(i, dict.GetString(itemname, "default"));
            }

            // videos
            num = dict.GetInt("videos");
            this.videos.SetNum(num);
            for (i = 0; i < num; i++) {
                itemname = String.format("video_%d", i);
                this.videos.oSetType(i, dict.GetString(itemname, "default"));
            }

            // emails
            num = dict.GetInt("emails");
            this.emails.SetNum(num);
            for (i = 0; i < num; i++) {
                itemname = String.format("email_%d", i);
                this.emails.oSetType(i, dict.GetString(itemname, "default"));
            }

            // weapons are stored as a number for persistant data, but as strings in the entityDef
            this.weapons = dict.GetInt("weapon_bits", "0");

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
                final idLevelTriggerInfo lti = new idLevelTriggerInfo();
                lti.levelName.oSet(dict.GetString(itemname));
                itemname = String.format("levelTrigger_Trigger_%d", i);
                lti.triggerName.oSet(dict.GetString(itemname));
                this.levelTriggers.Append(lti);
            }

        }

        public boolean Give(idPlayer owner, final idDict spawnArgs, final String statname, final String value, int[] idealWeapon, boolean updateHud) {
            int i;
            int pos;
            int end;
            int len;
            final idStr weaponString;
            int max;
            idDeclEntityDef weaponDecl;
            boolean tookWeapon;
            int amount;
            final idItemInfo info;
            final String name;

            if (0 == idStr.Icmpn(statname, "ammo_", 5)) {
                i = AmmoIndexForAmmoClass(statname);
                max = MaxAmmoForAmmoClass(owner, statname);
                if (this.ammo[i] >= max) {
                    return false;
                }
                amount = Integer.parseInt(value);
                if (amount != 0) {
                    this.ammo[i] += amount;
                    if ((max > 0) && (this.ammo[i] > max)) {
                        this.ammo[i] = max;
                    }
                    this.ammoPulse = true;

                    name = AmmoPickupNameForIndex(i);
                    if ((name != null) && !name.isEmpty()) {
                        AddPickupName(name, "");
                    }
                }
            } else if (0 == idStr.Icmp(statname, "armor")) {
                if (this.armor >= this.maxarmor) {
                    return false;	// can't hold any more, so leave the item
                }
                amount = Integer.parseInt(value);
                if (amount != 0) {
                    this.armor += amount;
                    if (this.armor > this.maxarmor) {
                        this.armor = this.maxarmor;
                    }
                    this.nextArmorDepleteTime = 0;
                    this.armorPulse = true;
                }
            } else if (idStr.FindText(statname, "inclip_") == 0) {
                i = WeaponIndexForAmmoClass(spawnArgs, statname + 7);
                if (i != -1) {
                    // set, don't add. not going over the clip size limit.
                    this.clip[i] = Integer.parseInt(value);
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
                        idGameLocal.Error("Unknown weapon '%s'", weaponName);
                    }

                    // cache the media for this weapon
                    weaponDecl = gameLocal.FindEntityDef(weaponName, false);

                    // don't pickup "no ammo" weapon types twice
                    // not for D3 SP .. there is only one case in the game where you can get a no ammo
                    // weapon when you might already have it, in that case it is more conistent to pick it up
                    if (gameLocal.isMultiplayer && (weaponDecl != null) && ((this.weapons & (1 << i)) != 0) && (0 == weaponDecl.dict.GetInt("ammoRequired"))) {
                        continue;
                    }

                    if (!gameLocal.world.spawnArgs.GetBool("no_Weapons") || ("weapon_fists".equals(weaponName)) || ("weapon_soulcube".equals(weaponName))) {//TODO:string in global vars, or local constants.
                        if (((this.weapons & (1 << i)) == 0) || gameLocal.isMultiplayer) {
                            if (owner.GetUserInfo().GetBool("ui_autoSwitch") && (idealWeapon != null)) {
                                assert (!gameLocal.isClient);
                                idealWeapon[0] = i;
                            }
                            if ((owner.hud != null) && updateHud && ((this.lastGiveTime + 1000) < gameLocal.time)) {
                                owner.hud.SetStateInt("newWeapon", i);
                                owner.hud.HandleNamedEvent("newWeapon");
                                this.lastGiveTime = gameLocal.time;
                            }
                            this.weaponPulse = true;
                            this.weapons |= (1 << i);
                            tookWeapon = true;
                        }
                    }
                }
                return tookWeapon;
            } else if ((0 == idStr.Icmp(statname, "item")) || (0 == idStr.Icmp(statname, "icon")) || (0 == idStr.Icmp(statname, "name"))) {
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
            assert ((weapon_index != -1) || (weapon_classname[0] != null));
            if (weapon_index == -1) {
                for (weapon_index = 0; weapon_index < MAX_WEAPONS; weapon_index++) {
                    if (NOT(idStr.Icmp(weapon_classname[0], spawnArgs.GetString(va("def_weapon%d", weapon_index))))) {
                        break;
                    }
                }
                if (weapon_index >= MAX_WEAPONS) {
                    idGameLocal.Error("Unknown weapon '%s'", weapon_classname[0]);
                }
            } else if (null == weapon_classname[0]) {
                weapon_classname[0] = spawnArgs.GetString(va("def_weapon%d", weapon_index));
            }
            this.weapons &= (0xffffffff ^ (1 << weapon_index));
            final int ammo_i = AmmoIndexForWeaponClass(weapon_classname[0], null);
            if (ammo_i != 0) {
                this.clip[weapon_index] = -1;
                this.ammo[ammo_i] = 0;
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
                idGameLocal.Error("Unknown weapon in decl '%s'", weapon_classname);
            }
            if (ammoRequired != null) {
                ammoRequired[0] = decl.dict.GetInt("ammoRequired");
            }
            final int ammo_i = AmmoIndexForAmmoClass(decl.dict.GetString("ammoType"));
            return ammo_i;
        }

        public String AmmoPickupNameForIndex(int ammonum) {
            return idWeapon.GetAmmoPickupNameForNum(ammonum);
        }

        public void AddPickupName(final String name, final String icon) {
            int num;

            num = this.pickupItemNames.Num();
            if ((num == 0) || (this.pickupItemNames.oGet(num - 1).name.Icmp(name) != 0)) {
                final idItemInfo info = this.pickupItemNames.Alloc();

                if (idStr.Cmpn(name, STRTABLE_ID, STRTABLE_ID_LENGTH) == 0) {
                    info.name = new idStr(common.GetLanguageDict().GetString(name));
                } else {
                    info.name = new idStr(name);
                }
                info.icon = new idStr(icon);
            }
        }

        public int HasAmmo(int type, int amount) {
            if ((type == 0) || (0 == amount)) {
                // always allow weapons that don't use ammo to fire
                return -1;
            }

            // check if we have infinite ammo
            if (this.ammo[type] < 0) {
                return -1;
            }

            // return how many shots we can fire
            return this.ammo[type] / amount;
        }

        public boolean UseAmmo(int type, int amount) {
            if (NOT(HasAmmo(type, amount))) {
                return false;
            }

            // take an ammo away if not infinite
            if (this.ammo[type] >= 0) {
                this.ammo[type] -= amount;
                this.ammoPredictTime = gameLocal.time; // mp client: we predict this. mark time so we're not confused by snapshots
            }

            return true;
        }

        public int HasAmmo(final String weapon_classname) {			// looks up the ammo information for the weapon class first
            final int[] ammoRequired = new int[1];
            final int ammo_i = AmmoIndexForWeaponClass(weapon_classname, ammoRequired);
            return HasAmmo(ammo_i, ammoRequired[0]);
        }

        public void UpdateArmor() {
            if ((this.deplete_armor != 0) && (this.deplete_armor < this.armor)) {
                if (0 == this.nextArmorDepleteTime) {
                    this.nextArmorDepleteTime = (int) (gameLocal.time + (this.deplete_rate * 1000));
                } else if (gameLocal.time > this.nextArmorDepleteTime) {
                    this.armor -= this.deplete_ammount;
                    if (this.armor < this.deplete_armor) {
                        this.armor = this.deplete_armor;
                    }
                    this.nextArmorDepleteTime = (int) (gameLocal.time + (this.deplete_rate * 1000));
                }
            }
        }
    }

    public static class loggedAccel_t {

        int    time;
        idVec3 dir;        // scaled larger for running

        public loggedAccel_t() {
            this.dir = new idVec3();
        }
    }

    public static class aasLocation_t {

        int areaNum;
        idVec3 pos;
    }

    public static class idPlayer extends idActor {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
        private final idPhysics_Player      physicsObj;               // player physics
        //
        private final idList<aasLocation_t> aasLocation;              // for AI tracking the player
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
        private final idAngles              viewBobAngles;
        private final idVec3                viewBob;
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
        private final idDeclSkin            skin;
        private idDeclSkin            powerUpSkin;
        private final idStr                 baseSkinName;
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
        private final idInterpolate<Float>  zoomFov;
        private final idInterpolate<Float>  centerView;
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
        private final              idStr              pdaAudio;
        private final              idStr              pdaVideo;
        private final              idStr              pdaVideoWave;
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
            this.usercmd = new usercmd_t();//memset( &usercmd, 0, sizeof( usercmd ) );

            this.playerView = new idPlayerView();

            this.noclip = false;
            this.godmode = false;

            this.spawnAnglesSet = false;
            this.spawnAngles = getAng_zero();
            this.viewAngles = getAng_zero();
            this.cmdAngles = getAng_zero();

            this.oldButtons = 0;
            this.buttonMask = 0;
            this.oldFlags = 0;

            this.lastHitTime = 0;
            this.lastSndHitTime = 0;
            this.lastSavingThrowTime = 0;

            this.inventory = new idInventory();

            this.weapon = new idEntityPtr<>(null);

            this.hud = null;
            this.objectiveSystem = null;
            this.objectiveSystemOpen = false;

            this.heartRate = BASE_HEARTRATE;
            this.heartInfo = new idInterpolate<>();
            this.heartInfo.Init(0, 0, 0f, 0f);
            this.lastHeartAdjust = 0;
            this.lastHeartBeat = 0;
            this.lastDmgTime = 0;
            this.deathClearContentsTime = 0;
            this.lastArmorPulse = -10000;
            this.stamina = 0;
            this.healthPool = 0;
            this.nextHealthPulse = 0;
            this.healthPulse = false;
            this.nextHealthTake = 0;
            this.healthTake = false;

            this.scoreBoardOpen = false;
            this.forceScoreBoard = false;
            this.forceRespawn = false;
            this.spectating = false;
            this.spectator = 0;
            this.colorBar = getVec3_zero();
            this.colorBarIndex = 0;
            this.forcedReady = false;
            this.wantSpectate = false;

            this.lastHitToggle = false;

            this.minRespawnTime = 0;
            this.maxRespawnTime = 0;

            this.firstPersonViewOrigin = getVec3_zero();
            this.firstPersonViewAxis = getMat3_identity();
            
            this.dragEntity = new idDragEntity();
            
            this.physicsObj = new idPhysics_Player();
            
            this.aasLocation = new idList<>();

            this.hipJoint = INVALID_JOINT;
            this.chestJoint = INVALID_JOINT;
            this.headJoint = INVALID_JOINT;

            this.bobFoot = 0;
            this.bobFrac = 0;
            this.bobfracsin = 0;
            this.bobCycle = 0;
            this.xyspeed = 0;
            this.stepUpTime = 0;
            this.stepUpDelta = 0;
            this.idealLegsYaw = 0;
            this.legsYaw = 0;
            this.legsForward = true;
            this.oldViewYaw = 0;
            this.viewBobAngles = getAng_zero();
            this.viewBob = getVec3_zero();
            this.landChange = 0;
            this.landTime = 0;

            this.currentWeapon = -1;
            this.idealWeapon = -1;
            this.previousWeapon = -1;
            this.weaponSwitchTime = 0;
            this.weaponEnabled = true;
            this.weapon_soulcube = -1;
            this.weapon_pda = -1;
            this.weapon_fists = -1;
            this.showWeaponViewModel = true;

            this.skin = new idDeclSkin();
            this.powerUpSkin = new idDeclSkin();
            this.baseSkinName = new idStr("");

            this.numProjectilesFired = 0;
            this.numProjectileHits = 0;

            this.airless = false;
            this.airTics = 0;
            this.lastAirDamage = 0;

            this.gibDeath = false;
            this.gibsLaunched = false;
            this.gibsDir = getVec3_zero();

            this.zoomFov = new idInterpolate<>();
            this.zoomFov.Init(0, 0, 0f, 0f);
            this.centerView = new idInterpolate<>();
            this.centerView.Init(0, 0, 0f, 0f);
            this.fxFov = false;

            this.influenceFov = 0;
            this.influenceActive = 0;
            this.influenceRadius = 0;
            this.influenceEntity = null;
            this.influenceMaterial = null;
            this.influenceSkin = null;

            this.privateCameraView = null;

//	memset( loggedViewAngles, 0, sizeof( loggedViewAngles ) );
            this.loggedViewAngles = Stream.generate(idAngles::new).limit(NUM_LOGGED_VIEW_ANGLES).toArray(idAngles[]::new);
//	memset( loggedAccel, 0, sizeof( loggedAccel ) );
            this.loggedAccel = Stream.generate(loggedAccel_t::new).limit(NUM_LOGGED_ACCELS).toArray(loggedAccel_t[]::new);
            this.currentLoggedAccel = 0;

            this.focusTime = 0;
            this.focusGUIent = null;
            this.focusUI = null;
            this.focusCharacter = null;
            this.talkCursor = 0;
            this.focusVehicle = null;
            this.cursor = null;

            this.oldMouseX = 0;
            this.oldMouseY = 0;

            this.pdaAudio = new idStr("");
            this.pdaVideo = new idStr("");
            this.pdaVideoWave = new idStr("");

            this.lastDamageDef = 0;
            this.lastDamageDir = getVec3_zero();
            this.lastDamageLocation = 0;
            this.smoothedFrame = 0;
            this.smoothedOriginUpdated = false;
            this.smoothedOrigin = getVec3_zero();
            this.smoothedAngles = getAng_zero();

            this.fl.networkSync = true;

            this.latchedTeam = -1;
            this.doingDeathSkin = false;
            this.weaponGone = false;
            this.useInitialSpawns = false;
            this.tourneyRank = 0;
            this.lastSpectateTeleport = 0;
            this.tourneyLine = 0;
            this.hiddenWeapon = false;
            this.tipUp = false;
            this.objectiveUp = false;
            this.teleportEntity = new idEntityPtr<>(null);
            this.teleportKiller = -1;
            this.respawning = false;
            this.ready = false;
            this.leader = false;
            this.lastSpectateChange = 0;
            this.lastTeleFX = -9999;
            this.weaponCatchup = false;
            this.lastSnapshotSequence = 0;

            this.MPAim = -1;
            this.lastMPAim = -1;
            this.lastMPAimTime = 0;
            this.MPAimFadeTime = 0;
            this.MPAimHighlight = false;

            this.spawnedTime = 0;
            this.lastManOver = false;
            this.lastManPlayAgain = false;
            this.lastManPresent = false;

            this.isTelefragged = false;

            this.isLagged = false;
            this.isChatting = false;

            this.selfSmooth = false;
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
            
            final idStr temp = new idStr();
//            idBounds bounds;

            if (this.entityNumber >= MAX_CLIENTS) {
                idGameLocal.Error("entityNum > MAX_CLIENTS for player.  Player may only be spawned with a client.");
            }

            // allow thinking during cinematics
            this.cinematic = true;

            if (gameLocal.isMultiplayer) {
                // always start in spectating state waiting to be spawned in
                // do this before SetClipModel to get the right bounding box
                this.spectating = true;
            }

            // set our collision model
            this.physicsObj.SetSelf(this);
            SetClipModel();
            this.physicsObj.SetMass(this.spawnArgs.GetFloat("mass", "100"));
            this.physicsObj.SetContents(CONTENTS_BODY);
            this.physicsObj.SetClipMask(MASK_PLAYERSOLID);
            SetPhysics(this.physicsObj);
            InitAASLocation();

            this.skin.oSet(this.renderEntity.customSkin);

            // only the local player needs guis
            if (!gameLocal.isMultiplayer || (this.entityNumber == gameLocal.localClientNum)) {

                // load HUD
                if (gameLocal.isMultiplayer) {
                    this.hud = uiManager.FindGui("guis/mphud.gui", true, false, true);
                } else if (this.spawnArgs.GetString("hud", "", temp)) {
                    this.hud = uiManager.FindGui(temp.getData(), true, false, true);
                }
                if (this.hud != null) {
                    this.hud.Activate(true, gameLocal.time);
                }

                // load cursor
                if (this.spawnArgs.GetString("cursor", "", temp)) {
                    this.cursor = uiManager.FindGui(temp.getData(), true, gameLocal.isMultiplayer, gameLocal.isMultiplayer);
                }
                if (this.cursor != null) {
                    this.cursor.Activate(true, gameLocal.time);
                }

                this.objectiveSystem = uiManager.FindGui("guis/pda.gui", true, false, true);
                this.objectiveSystemOpen = false;
            }

            SetLastHitTime(0);

            // load the armor sound feedback
            declManager.FindSound("player_sounds_hitArmor");

            // set up conditions for animation
            LinkScriptVariables();

            this.animator.RemoveOriginOffset(true);

            // initialize user info related settings
            // on server, we wait for the userinfo broadcast, as this controls when the player is initially spawned in game
            if (gameLocal.isClient || (this.entityNumber == gameLocal.localClientNum)) {
                UserInfoChanged(false);
            }

            // create combat collision hull for exact collision detection
            SetCombatModel();

            // init the damage effects
            this.playerView.SetPlayerEntity(this);

            // supress model in non-player views, but allow it in mirrors and remote views
            this.renderEntity.suppressSurfaceInViewID = this.entityNumber + 1;

            // don't project shadow on self or weapon
            this.renderEntity.noSelfShadow = true;

            final idAFAttachment headEnt = this.head.GetEntity();
            if (headEnt != null) {
                headEnt.GetRenderEntity().suppressSurfaceInViewID = this.entityNumber + 1;
                headEnt.GetRenderEntity().noSelfShadow = true;
            }

            if (gameLocal.isMultiplayer) {
                Init();
                Hide();    // properly hidden if starting as a spectator
                if (!gameLocal.isClient) {
                    // set yourself ready to spawn. idMultiplayerGame will decide when/if appropriate and call SpawnFromSpawnSpot
                    SetupWeaponEntity();
                    SpawnFromSpawnSpot();
                    this.forceRespawn = true;
                    assert (this.spectating);
                }
            } else {
                SetupWeaponEntity();
                SpawnFromSpawnSpot();
            }

            // trigger playtesting item gives, if we didn't get here from a previous level
            // the devmap key will be set on the first devmap, but cleared on any level
            // transitions
            if (!gameLocal.isMultiplayer && (gameLocal.serverInfo.FindKey("devmap") != null)) {
                // fire a trigger with the name "devmap"
                final idEntity ent = gameLocal.FindEntity("devmap");
                if (ent != null) {
                    ent.ActivateTargets(this);
                }
            }
            if (this.hud != null) {
                // We can spawn with a full soul cube, so we need to make sure the hud knows this
                if ((this.weapon_soulcube > 0) && ((this.inventory.weapons & (1 << this.weapon_soulcube)) != 0)) {
                    final int max_souls = this.inventory.MaxAmmoForAmmoClass(this, "ammo_souls");
                    if (this.inventory.ammo[idWeapon.GetAmmoNumForName("ammo_souls")] >= max_souls) {
                        this.hud.HandleNamedEvent("soulCubeReady");
                    }
                }
                this.hud.HandleNamedEvent("itemPickup");
            }

            if (GetPDA() != null) {
                // Add any emails from the inventory
                for (int i = 0; i < this.inventory.emails.Num(); i++) {
                    GetPDA().AddEmail(this.inventory.emails.oGet(i).getData());
                }
                GetPDA().SetSecurity(common.GetLanguageDict().GetString("#str_00066"));
            }

            if (gameLocal.world.spawnArgs.GetBool("no_Weapons")) {
                this.hiddenWeapon = true;
                if (this.weapon.GetEntity() != null) {
                    this.weapon.GetEntity().LowerWeapon();
                }
                this.idealWeapon = 0;
            } else {
                this.hiddenWeapon = false;
            }

            if (this.hud != null) {
                UpdateHudWeapon();
                this.hud.StateChanged(gameLocal.time);
            }

            this.tipUp = false;
            this.objectiveUp = false;

            if (this.inventory.levelTriggers.Num() != 0) {
                PostEventMS(EV_Player_LevelTrigger, 0);
            }

            this.inventory.pdaOpened = false;
            this.inventory.selPDA = 0;

            if (!gameLocal.isMultiplayer) {
                if (g_skill.GetInteger() < 2) {
                    if (this.health < 25) {
                        this.health = 25;
                    }
                    if (g_useDynamicProtection.GetBool()) {
                        g_damageScale.SetFloat(1.0f);
                    }
                } else {
                    g_damageScale.SetFloat(1.0f);
                    g_armorProtection.SetFloat((g_skill.GetInteger() < 2) ? 0.4f : 0.2f);
                    if (BuildDefines.ID_DEMO_BUILD) {
                        if (g_skill.GetInteger() == 3) {
                            this.healthTake = true;
                            this.nextHealthTake = gameLocal.time + (g_healthTakeTime.GetInteger() * 1000);
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
            this.oldButtons = this.usercmd.buttons;

            // grab out usercmd
            final usercmd_t oldCmd = this.usercmd;
            this.usercmd = gameLocal.usercmds[this.entityNumber];
            this.buttonMask &= this.usercmd.buttons;
            this.usercmd.buttons &= ~this.buttonMask;

            if (gameLocal.inCinematic && gameLocal.skipCinematic) {
                return;
            }

            // clear the ik before we do anything else so the skeleton doesn't get updated twice
            this.walkIK.ClearJointMods();

            // if this is the very first frame of the map, set the delta view angles
            // based on the usercmd angles
            if (!this.spawnAnglesSet && (gameLocal.GameState() != GAMESTATE_STARTUP)) {
                this.spawnAnglesSet = true;
                SetViewAngles(this.spawnAngles);
                this.oldFlags = this.usercmd.flags;
            }

            if (this.objectiveSystemOpen || gameLocal.inCinematic || (this.influenceActive != 0)) {
                if (this.objectiveSystemOpen && this.AI_PAIN.operator()) {
                    TogglePDA();
                }
                this.usercmd.forwardmove = 0;
                this.usercmd.rightmove = 0;
                this.usercmd.upmove = 0;
            }

            // log movement changes for weapon bobbing effects
            if (this.usercmd.forwardmove != oldCmd.forwardmove) {
                final loggedAccel_t acc = this.loggedAccel[this.currentLoggedAccel & (NUM_LOGGED_ACCELS - 1)];
                this.currentLoggedAccel++;
                acc.time = gameLocal.time;
                acc.dir.oSet(0, this.usercmd.forwardmove - oldCmd.forwardmove);
                acc.dir.oSet(1, acc.dir.oSet(2, 0));
            }

            if (this.usercmd.rightmove != oldCmd.rightmove) {
                final loggedAccel_t acc = this.loggedAccel[this.currentLoggedAccel & (NUM_LOGGED_ACCELS - 1)];
                this.currentLoggedAccel++;
                acc.time = gameLocal.time;
                acc.dir.oSet(0, this.usercmd.forwardmove - oldCmd.forwardmove);
                acc.dir.oSet(1, acc.dir.oSet(2, 0));
            }

            // freelook centering
            if (((this.usercmd.buttons ^ oldCmd.buttons) & BUTTON_MLOOK) != 0) {
                this.centerView.Init(gameLocal.time, 200, this.viewAngles.pitch, 0f);
            }

            // zooming
            if (((this.usercmd.buttons ^ oldCmd.buttons) & BUTTON_ZOOM) != 0) {
                if (((this.usercmd.buttons & BUTTON_ZOOM) != 0) && (this.weapon.GetEntity() != null)) {
                    this.zoomFov.Init(gameLocal.time, 200, CalcFov(false), (float) this.weapon.GetEntity().GetZoomFov());
                } else {
                    this.zoomFov.Init(gameLocal.time, 200, this.zoomFov.GetCurrentValue(gameLocal.time), DefaultFov());
                }
            }

            // if we have an active gui, we will unrotate the view angles as
            // we turn the mouse movements into gui events
            final idUserInterface gui = ActiveGui();
            if ((gui != null) && (gui != this.focusUI)) {
                RouteGuiMouse(gui);
            }

            // set the push velocity on the weapon before running the physics
            if (this.weapon.GetEntity() != null) {
                this.weapon.GetEntity().SetPushVelocity(this.physicsObj.GetPushedLinearVelocity());
            }

            EvaluateControls();

            if (!this.af.IsActive()) {
                AdjustBodyAngles();
                CopyJointsFromBodyToHead();
            }

            Move();

            if (!g_stopTime.GetBool()) {

                if (!this.noclip && !this.spectating && (this.health > 0) && !IsHidden()) {
                    TouchTriggers();
                }

                // not done on clients for various reasons. don't do it on server and save the sound channel for other things
                if (!gameLocal.isMultiplayer) {
                    SetCurrentHeartRate();
                    float scale = g_damageScale.GetFloat();
                    if (g_useDynamicProtection.GetBool() && (scale < 1.0f) && ((gameLocal.time - this.lastDmgTime) > 500)) {
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
                if (!this.spectating && !this.af.IsActive() && !gameLocal.inCinematic) {
                    UpdateConditions();
                    UpdateAnimState();
                    CheckBlink();
                }

                // clear out our pain flag so we can tell if we recieve any damage between now and the next time we think
                this.AI_PAIN.operator(false);
            }

            // calculate the exact bobbed view position, which is used to
            // position the view weapon, among other things
            CalculateFirstPersonView();

            // this may use firstPersonView, or a thirdPeroson / camera view
            CalculateRenderView();

            this.inventory.UpdateArmor();

            if (this.spectating) {
                UpdateSpectating();
            } else if (this.health > 0) {
                UpdateWeapon();
            }

            UpdateAir();

            UpdateHud();

            UpdatePowerUps();

            UpdateDeathSkin(false);

            if (gameLocal.isMultiplayer) {
                DrawPlayerIcons();
            }

            if (this.head.GetEntity() != null) {
                headRenderEnt = this.head.GetEntity().GetRenderEntity();
            } else {
                headRenderEnt = null;
            }

            if (headRenderEnt != null) {
                if (this.influenceSkin != null) {
                    headRenderEnt.customSkin = this.influenceSkin;
                } else {
                    headRenderEnt.customSkin = null;
                }
            }

            if (gameLocal.isMultiplayer || g_showPlayerShadow.GetBool()) {
                this.renderEntity.suppressShadowInViewID = 0;
                if (headRenderEnt != null) {
                    headRenderEnt.suppressShadowInViewID = 0;
                }
            } else {
                this.renderEntity.suppressShadowInViewID = this.entityNumber + 1;
                if (headRenderEnt != null) {
                    headRenderEnt.suppressShadowInViewID = this.entityNumber + 1;
                }
            }
            // never cast shadows from our first-person muzzle flashes
            this.renderEntity.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + this.entityNumber;
            if (headRenderEnt != null) {
                headRenderEnt.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + this.entityNumber;
            }

            if (!g_stopTime.GetBool()) {
                UpdateAnimation();

                Present();

                UpdateDamageEffects();

                LinkCombat();

                this.playerView.CalculateShake();
            }

            if (0 == (this.thinkFlags & TH_THINK)) {
                gameLocal.Printf("player %d not thinking?\n", this.entityNumber);
            }

            if (g_showEnemies.GetBool()) {
                idActor ent;
                int num = 0;
                for (ent = this.enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
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

            savefile.WriteUsercmd(this.usercmd);
            this.playerView.Save(savefile);

            savefile.WriteBool(this.noclip);
            savefile.WriteBool(this.godmode);

            // don't save spawnAnglesSet, since we'll have to reset them after loading the savegame
            savefile.WriteAngles(this.spawnAngles);
            savefile.WriteAngles(this.viewAngles);
            savefile.WriteAngles(this.cmdAngles);

            savefile.WriteInt(this.buttonMask);
            savefile.WriteInt(this.oldButtons);
            savefile.WriteInt(this.oldFlags);

            savefile.WriteInt(this.lastHitTime);
            savefile.WriteInt(this.lastSndHitTime);
            savefile.WriteInt(this.lastSavingThrowTime);

            // idBoolFields don't need to be saved, just re-linked in Restore
            this.inventory.Save(savefile);
            this.weapon.Save(savefile);

            savefile.WriteUserInterface(this.hud, false);
            savefile.WriteUserInterface(this.objectiveSystem, false);
            savefile.WriteBool(this.objectiveSystemOpen);

            savefile.WriteInt(this.weapon_soulcube);
            savefile.WriteInt(this.weapon_pda);
            savefile.WriteInt(this.weapon_fists);

            savefile.WriteInt(this.heartRate);

            savefile.WriteFloat(this.heartInfo.GetStartTime());
            savefile.WriteFloat(this.heartInfo.GetDuration());
            savefile.WriteFloat(this.heartInfo.GetStartValue());
            savefile.WriteFloat(this.heartInfo.GetEndValue());

            savefile.WriteInt(this.lastHeartAdjust);
            savefile.WriteInt(this.lastHeartBeat);
            savefile.WriteInt(this.lastDmgTime);
            savefile.WriteInt(this.deathClearContentsTime);
            savefile.WriteBool(this.doingDeathSkin);
            savefile.WriteInt(this.lastArmorPulse);
            savefile.WriteFloat(this.stamina);
            savefile.WriteFloat(this.healthPool);
            savefile.WriteInt(this.nextHealthPulse);
            savefile.WriteBool(this.healthPulse);
            savefile.WriteInt(this.nextHealthTake);
            savefile.WriteBool(this.healthTake);

            savefile.WriteBool(this.hiddenWeapon);
            this.soulCubeProjectile.Save(savefile);

            savefile.WriteInt(this.spectator);
            savefile.WriteVec3(this.colorBar);
            savefile.WriteInt(this.colorBarIndex);
            savefile.WriteBool(this.scoreBoardOpen);
            savefile.WriteBool(this.forceScoreBoard);
            savefile.WriteBool(this.forceRespawn);
            savefile.WriteBool(this.spectating);
            savefile.WriteInt(this.lastSpectateTeleport);
            savefile.WriteBool(this.lastHitToggle);
            savefile.WriteBool(this.forcedReady);
            savefile.WriteBool(this.wantSpectate);
            savefile.WriteBool(this.weaponGone);
            savefile.WriteBool(this.useInitialSpawns);
            savefile.WriteInt(this.latchedTeam);
            savefile.WriteInt(this.tourneyRank);
            savefile.WriteInt(this.tourneyLine);

            this.teleportEntity.Save(savefile);
            savefile.WriteInt(this.teleportKiller);

            savefile.WriteInt(this.minRespawnTime);
            savefile.WriteInt(this.maxRespawnTime);

            savefile.WriteVec3(this.firstPersonViewOrigin);
            savefile.WriteMat3(this.firstPersonViewAxis);

            // don't bother saving dragEntity since it's a dev tool
            savefile.WriteJoint(this.hipJoint);
            savefile.WriteJoint(this.chestJoint);
            savefile.WriteJoint(this.headJoint);

            savefile.WriteStaticObject(this.physicsObj);

            savefile.WriteInt(this.aasLocation.Num());
            for (i = 0; i < this.aasLocation.Num(); i++) {
                savefile.WriteInt(this.aasLocation.oGet(i).areaNum);
                savefile.WriteVec3(this.aasLocation.oGet(i).pos);
            }

            savefile.WriteInt(this.bobFoot);
            savefile.WriteFloat(this.bobFrac);
            savefile.WriteFloat(this.bobfracsin);
            savefile.WriteInt(this.bobCycle);
            savefile.WriteFloat(this.xyspeed);
            savefile.WriteInt(this.stepUpTime);
            savefile.WriteFloat(this.stepUpDelta);
            savefile.WriteFloat(this.idealLegsYaw);
            savefile.WriteFloat(this.legsYaw);
            savefile.WriteBool(this.legsForward);
            savefile.WriteFloat(this.oldViewYaw);
            savefile.WriteAngles(this.viewBobAngles);
            savefile.WriteVec3(this.viewBob);
            savefile.WriteInt(this.landChange);
            savefile.WriteInt(this.landTime);

            savefile.WriteInt(this.currentWeapon);
            savefile.WriteInt(this.idealWeapon);
            savefile.WriteInt(this.previousWeapon);
            savefile.WriteInt(this.weaponSwitchTime);
            savefile.WriteBool(this.weaponEnabled);
            savefile.WriteBool(this.showWeaponViewModel);

            savefile.WriteSkin(this.skin);
            savefile.WriteSkin(this.powerUpSkin);
            savefile.WriteString(this.baseSkinName);

            savefile.WriteInt(this.numProjectilesFired);
            savefile.WriteInt(this.numProjectileHits);

            savefile.WriteBool(this.airless);
            savefile.WriteInt(this.airTics);
            savefile.WriteInt(this.lastAirDamage);

            savefile.WriteBool(this.gibDeath);
            savefile.WriteBool(this.gibsLaunched);
            savefile.WriteVec3(this.gibsDir);

            savefile.WriteFloat(this.zoomFov.GetStartTime());
            savefile.WriteFloat(this.zoomFov.GetDuration());
            savefile.WriteFloat(this.zoomFov.GetStartValue());
            savefile.WriteFloat(this.zoomFov.GetEndValue());

            savefile.WriteFloat(this.centerView.GetStartTime());
            savefile.WriteFloat(this.centerView.GetDuration());
            savefile.WriteFloat(this.centerView.GetStartValue());
            savefile.WriteFloat(this.centerView.GetEndValue());

            savefile.WriteBool(this.fxFov);

            savefile.WriteFloat(this.influenceFov);
            savefile.WriteInt(this.influenceActive);
            savefile.WriteFloat(this.influenceRadius);
            savefile.WriteObject(this.influenceEntity);
            savefile.WriteMaterial(this.influenceMaterial);
            savefile.WriteSkin(this.influenceSkin);

            savefile.WriteObject(this.privateCameraView);

            for (i = 0; i < NUM_LOGGED_VIEW_ANGLES; i++) {
                savefile.WriteAngles(this.loggedViewAngles[i]);
            }
            for (i = 0; i < NUM_LOGGED_ACCELS; i++) {
                savefile.WriteInt(this.loggedAccel[i].time);
                savefile.WriteVec3(this.loggedAccel[i].dir);
            }
            savefile.WriteInt(this.currentLoggedAccel);

            savefile.WriteObject(this.focusGUIent);
            // can't save focusUI
            savefile.WriteObject(this.focusCharacter);
            savefile.WriteInt(this.talkCursor);
            savefile.WriteInt(this.focusTime);
            savefile.WriteObject(this.focusVehicle);
            savefile.WriteUserInterface(this.cursor, false);

            savefile.WriteInt(this.oldMouseX);
            savefile.WriteInt(this.oldMouseY);

            savefile.WriteString(this.pdaAudio);
            savefile.WriteString(this.pdaVideo);
            savefile.WriteString(this.pdaVideoWave);

            savefile.WriteBool(this.tipUp);
            savefile.WriteBool(this.objectiveUp);

            savefile.WriteInt(this.lastDamageDef);
            savefile.WriteVec3(this.lastDamageDir);
            savefile.WriteInt(this.lastDamageLocation);
            savefile.WriteInt(this.smoothedFrame);
            savefile.WriteBool(this.smoothedOriginUpdated);
            savefile.WriteVec3(this.smoothedOrigin);
            savefile.WriteAngles(this.smoothedAngles);

            savefile.WriteBool(this.ready);
            savefile.WriteBool(this.respawning);
            savefile.WriteBool(this.leader);
            savefile.WriteInt(this.lastSpectateChange);
            savefile.WriteInt(this.lastTeleFX);

            savefile.WriteFloat(pm_stamina.GetFloat());

            if (this.hud != null) {
                this.hud.SetStateString("message", common.GetLanguageDict().GetString("#str_02916"));
                this.hud.HandleNamedEvent("Message");
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {                    // unarchives object from save game file
            int i;
            final int[] num = {0};
            final float[] set = {0};

            savefile.ReadUsercmd(this.usercmd);
            this.playerView.Restore(savefile);

            this.noclip = savefile.ReadBool();
            this.godmode = savefile.ReadBool();

            savefile.ReadAngles(this.spawnAngles);
            savefile.ReadAngles(this.viewAngles);
            savefile.ReadAngles(this.cmdAngles);

//	memset( usercmd.angles, 0, sizeof( usercmd.angles ) );
            Arrays.fill(this.usercmd.angles, (short) 0);//damn you type safety!!
            SetViewAngles(this.viewAngles);
            this.spawnAnglesSet = true;

            this.buttonMask = savefile.ReadInt();
            this.oldButtons = savefile.ReadInt();
            this.oldFlags = savefile.ReadInt();

            this.usercmd.flags = 0;
            this.oldFlags = 0;

            this.lastHitTime = savefile.ReadInt();
            this.lastSndHitTime = savefile.ReadInt();
            this.lastSavingThrowTime = savefile.ReadInt();

            // Re-link idBoolFields to the scriptObject, values will be restored in scriptObject's restore
            LinkScriptVariables();

            this.inventory.Restore(savefile);
            this.weapon.Restore(savefile);

            for (i = 0; i < this.inventory.emails.Num(); i++) {
                GetPDA().AddEmail(this.inventory.emails.oGet(i).getData());
            }

            savefile.ReadUserInterface(this.hud);
            savefile.ReadUserInterface(this.objectiveSystem);
            this.objectiveSystemOpen = savefile.ReadBool();

            this.weapon_soulcube = savefile.ReadInt();
            this.weapon_pda = savefile.ReadInt();
            this.weapon_fists = savefile.ReadInt();

            this.heartRate = savefile.ReadInt();

            savefile.ReadFloat(set);
            this.heartInfo.SetStartTime(set[0]);
            savefile.ReadFloat(set);
            this.heartInfo.SetDuration(set[0]);
            savefile.ReadFloat(set);
            this.heartInfo.SetStartValue(set[0]);
            savefile.ReadFloat(set);
            this.heartInfo.SetEndValue(set[0]);

            this.lastHeartAdjust = savefile.ReadInt();
            this.lastHeartBeat = savefile.ReadInt();
            this.lastDmgTime = savefile.ReadInt();
            this.deathClearContentsTime = savefile.ReadInt();
            this.doingDeathSkin = savefile.ReadBool();
            this.lastArmorPulse = savefile.ReadInt();
            this.stamina = savefile.ReadFloat();
            this.healthPool = savefile.ReadFloat();
            this.nextHealthPulse = savefile.ReadInt();
            this.healthPulse = savefile.ReadBool();
            this.nextHealthTake = savefile.ReadInt();
            this.healthTake = savefile.ReadBool();

            this.hiddenWeapon = savefile.ReadBool();
            this.soulCubeProjectile.Restore(savefile);

            this.spectator = savefile.ReadInt();
            savefile.ReadVec3(this.colorBar);
            this.colorBarIndex = savefile.ReadInt();
            this.scoreBoardOpen = savefile.ReadBool();
            this.forceScoreBoard = savefile.ReadBool();
            this.forceRespawn = savefile.ReadBool();
            this.spectating = savefile.ReadBool();
            this.lastSpectateTeleport = savefile.ReadInt();
            this.lastHitToggle = savefile.ReadBool();
            this.forcedReady = savefile.ReadBool();
            this.wantSpectate = savefile.ReadBool();
            this.weaponGone = savefile.ReadBool();
            this.useInitialSpawns = savefile.ReadBool();
            this.latchedTeam = savefile.ReadInt();
            this.tourneyRank = savefile.ReadInt();
            this.tourneyLine = savefile.ReadInt();

            this.teleportEntity.Restore(savefile);
            this.teleportKiller = savefile.ReadInt();

            this.minRespawnTime = savefile.ReadInt();
            this.maxRespawnTime = savefile.ReadInt();

            savefile.ReadVec3(this.firstPersonViewOrigin);
            savefile.ReadMat3(this.firstPersonViewAxis);

            // don't bother saving dragEntity since it's a dev tool
            this.dragEntity.Clear();

            this.hipJoint = savefile.ReadJoint();
            this.chestJoint = savefile.ReadJoint();
            this.headJoint = savefile.ReadJoint();

            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);

            savefile.ReadInt(num);
            this.aasLocation.SetGranularity(1);
            this.aasLocation.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                this.aasLocation.oGet(i).areaNum = savefile.ReadInt();
                savefile.ReadVec3(this.aasLocation.oGet(i).pos);
            }

            this.bobFoot = savefile.ReadInt();
            this.bobFrac = savefile.ReadFloat();
            this.bobfracsin = savefile.ReadFloat();
            this.bobCycle = savefile.ReadInt();
            this.xyspeed = savefile.ReadFloat();
            this.stepUpTime = savefile.ReadInt();
            this.stepUpDelta = savefile.ReadFloat();
            this.idealLegsYaw = savefile.ReadFloat();
            this.legsYaw = savefile.ReadFloat();
            this.legsForward = savefile.ReadBool();
            this.oldViewYaw = savefile.ReadFloat();
            savefile.ReadAngles(this.viewBobAngles);
            savefile.ReadVec3(this.viewBob);
            this.landChange = savefile.ReadInt();
            this.landTime = savefile.ReadInt();

            this.currentWeapon = savefile.ReadInt();
            this.idealWeapon = savefile.ReadInt();
            this.previousWeapon = savefile.ReadInt();
            this.weaponSwitchTime = savefile.ReadInt();
            this.weaponEnabled = savefile.ReadBool();
            this.showWeaponViewModel = savefile.ReadBool();

            savefile.ReadSkin(this.skin);
            savefile.ReadSkin(this.powerUpSkin);
            savefile.ReadString(this.baseSkinName);

            this.numProjectilesFired = savefile.ReadInt();
            this.numProjectileHits = savefile.ReadInt();

            this.airless = savefile.ReadBool();
            this.airTics = savefile.ReadInt();
            this.lastAirDamage = savefile.ReadInt();

            this.gibDeath = savefile.ReadBool();
            this.gibsLaunched = savefile.ReadBool();
            savefile.ReadVec3(this.gibsDir);

            savefile.ReadFloat(set);
            this.zoomFov.SetStartTime(set[0]);
            savefile.ReadFloat(set);
            this.zoomFov.SetDuration(set[0]);
            savefile.ReadFloat(set);
            this.zoomFov.SetStartValue(set[0]);
            savefile.ReadFloat(set);
            this.zoomFov.SetEndValue(set[0]);

            savefile.ReadFloat(set);
            this.centerView.SetStartTime(set[0]);
            savefile.ReadFloat(set);
            this.centerView.SetDuration(set[0]);
            savefile.ReadFloat(set);
            this.centerView.SetStartValue(set[0]);
            savefile.ReadFloat(set);
            this.centerView.SetEndValue(set[0]);

            this.fxFov = savefile.ReadBool();

            this.influenceFov = savefile.ReadFloat();
            this.influenceActive = savefile.ReadInt();
            this.influenceRadius = savefile.ReadFloat();
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/influenceEntity);
            savefile.ReadMaterial(this.influenceMaterial);
            savefile.ReadSkin(this.influenceSkin);

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/privateCameraView);

            for (i = 0; i < NUM_LOGGED_VIEW_ANGLES; i++) {
                savefile.ReadAngles(this.loggedViewAngles[i]);
            }
            for (i = 0; i < NUM_LOGGED_ACCELS; i++) {
                this.loggedAccel[i].time = savefile.ReadInt();
                savefile.ReadVec3(this.loggedAccel[i].dir);
            }
            this.currentLoggedAccel = savefile.ReadInt();

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/focusGUIent);
            // can't save focusUI
            this.focusUI = null;
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/focusCharacter);
            this.talkCursor = savefile.ReadInt();
            this.focusTime = savefile.ReadInt();
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/focusVehicle);
            savefile.ReadUserInterface(this.cursor);

            this.oldMouseX = savefile.ReadInt();
            this.oldMouseY = savefile.ReadInt();

            savefile.ReadString(this.pdaAudio);
            savefile.ReadString(this.pdaVideo);
            savefile.ReadString(this.pdaVideoWave);

            this.tipUp = savefile.ReadBool();
            this.objectiveUp = savefile.ReadBool();

            this.lastDamageDef = savefile.ReadInt();
            savefile.ReadVec3(this.lastDamageDir);
            this.lastDamageLocation = savefile.ReadInt();
            this.smoothedFrame = savefile.ReadInt();
            this.smoothedOriginUpdated = savefile.ReadBool();
            savefile.ReadVec3(this.smoothedOrigin);
            savefile.ReadAngles(this.smoothedAngles);

            this.ready = savefile.ReadBool();
            this.respawning = savefile.ReadBool();
            this.leader = savefile.ReadBool();
            this.lastSpectateChange = savefile.ReadInt();
            this.lastTeleFX = savefile.ReadInt();

            // set the pm_ cvars
            idKeyValue kv;
            kv = this.spawnArgs.MatchPrefix("pm_", null);
            while (kv != null) {
                cvarSystem.SetCVarString(kv.GetKey().getData(), kv.GetValue().getData());
                kv = this.spawnArgs.MatchPrefix("pm_", kv);
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
            weap = this.weapon.GetEntity();
            if (weap != null) {
                weap.HideWorldModel();
            }
        }

        @Override
        public void Show() {
            idWeapon weap;

            super.Show();
            weap = this.weapon.GetEntity();
            if (weap != null) {
                weap.ShowWorldModel();
            }
        }

        @Override
        public void Init() {
            final String[] value = {null};
            idKeyValue kv;

            this.noclip = false;
            this.godmode = false;

            this.oldButtons = 0;
            this.oldFlags = 0;

            this.currentWeapon = -1;
            this.idealWeapon = -1;
            this.previousWeapon = -1;
            this.weaponSwitchTime = 0;
            this.weaponEnabled = true;
            this.weapon_soulcube = SlotForWeapon("weapon_soulcube");
            this.weapon_pda = SlotForWeapon("weapon_pda");
            this.weapon_fists = SlotForWeapon("weapon_fists");
            this.showWeaponViewModel = GetUserInfo().GetBool("ui_showGun");

            this.lastDmgTime = 0;
            this.lastArmorPulse = -10000;
            this.lastHeartAdjust = 0;
            this.lastHeartBeat = 0;
            this.heartInfo.Init(0, 0, 0f, 0f);

            this.bobCycle = 0;
            this.bobFrac = 0;
            this.landChange = 0;
            this.landTime = 0;
            this.zoomFov.Init(0, 0, 0f, 0f);
            this.centerView.Init(0, 0, 0f, 0f);
            this.fxFov = false;

            this.influenceFov = 0;
            this.influenceActive = 0;
            this.influenceRadius = 0;
            this.influenceEntity = null;
            this.influenceMaterial = null;
            this.influenceSkin = null;

            this.currentLoggedAccel = 0;

            this.focusTime = 0;
            this.focusGUIent = null;
            this.focusUI = null;
            this.focusCharacter = null;
            this.talkCursor = 0;
            this.focusVehicle = null;

            // remove any damage effects
            this.playerView.ClearEffects();

            // damage values
            this.fl.takedamage = true;
            ClearPain();

            // restore persistent data
            RestorePersistantInfo();

            this.bobCycle = 0;
            this.stamina = 0;
            this.healthPool = 0;
            this.nextHealthPulse = 0;
            this.healthPulse = false;
            this.nextHealthTake = 0;
            this.healthTake = false;

            SetupWeaponEntity();
            this.currentWeapon = -1;
            this.previousWeapon = -1;

            this.heartRate = BASE_HEARTRATE;
            AdjustHeartRate(BASE_HEARTRATE, 0, 0, true);

            this.idealLegsYaw = 0;
            this.legsYaw = 0;
            this.legsForward = true;
            this.oldViewYaw = 0;

            // set the pm_ cvars
            if (!gameLocal.isMultiplayer || gameLocal.isServer) {
                kv = this.spawnArgs.MatchPrefix("pm_", null);
                while (kv != null) {
                    cvarSystem.SetCVarString(kv.GetKey().getData(), kv.GetValue().getData());
                    kv = this.spawnArgs.MatchPrefix("pm_", kv);
                }
            }

            // disable stamina on hell levels
            if ((gameLocal.world != null) && gameLocal.world.spawnArgs.GetBool("no_stamina")) {
                pm_stamina.SetFloat(0);
            }

            // stamina always initialized to maximum
            this.stamina = pm_stamina.GetFloat();

            // air always initialized to maximum too
            this.airTics = (int) pm_airTics.GetFloat();
            this.airless = false;

            this.gibDeath = false;
            this.gibsLaunched = false;
            this.gibsDir.Zero();

            // set the gravity
            this.physicsObj.SetGravity(gameLocal.GetGravity());

            // start out standing
            SetEyeHeight(pm_normalviewheight.GetFloat());

            this.stepUpTime = 0;
            this.stepUpDelta = 0;
            this.viewBobAngles.Zero();
            this.viewBob.Zero();

            value[0] = this.spawnArgs.GetString("model");
            if ((value[0] != null) && (!value[0].isEmpty())) {
                SetModel(value[0]);
            }

            if (this.cursor != null) {
                this.cursor.SetStateInt("talkcursor", 0);
                this.cursor.SetStateString("combatcursor", "1");
                this.cursor.SetStateString("itemcursor", "0");
                this.cursor.SetStateString("guicursor", "0");
            }

            if ((gameLocal.isMultiplayer || g_testDeath.GetBool()) && (this.skin != null)) {
                SetSkin(this.skin);
                this.renderEntity.shaderParms[6] = 0;
            } else if (this.spawnArgs.GetString("spawn_skin", null, value)) {
                this.skin.oSet(declManager.FindSkin(value[0]));
                SetSkin(this.skin);
                this.renderEntity.shaderParms[6] = 0;
            }

            value[0] = this.spawnArgs.GetString("bone_hips", "");
            this.hipJoint = this.animator.GetJointHandle(value[0]);
            if (this.hipJoint == INVALID_JOINT) {
                idGameLocal.Error("Joint '%s' not found for 'bone_hips' on '%s'", value[0], this.name);
            }

            value[0] = this.spawnArgs.GetString("bone_chest", "");
            this.chestJoint = this.animator.GetJointHandle(value[0]);
            if (this.chestJoint == INVALID_JOINT) {
                idGameLocal.Error("Joint '%s' not found for 'bone_chest' on '%s'", value[0], this.name);
            }

            value[0] = this.spawnArgs.GetString("bone_head", "");
            this.headJoint = this.animator.GetJointHandle(value[0]);
            if (this.headJoint == INVALID_JOINT) {
                idGameLocal.Error("Joint '%s' not found for 'bone_head' on '%s'", value[0], this.name);
            }

            // initialize the script variables
            this.AI_FORWARD.operator(false);
            this.AI_BACKWARD.operator(false);
            this.AI_STRAFE_LEFT.operator(false);
            this.AI_STRAFE_RIGHT.operator(false);
            this.AI_ATTACK_HELD.operator(false);
            this.AI_WEAPON_FIRED.operator(false);
            this.AI_JUMP.operator(false);
            this.AI_DEAD.operator(false);
            this.AI_CROUCH.operator(false);
            this.AI_ONGROUND.operator(false);
            this.AI_ONLADDER.operator(false);
            this.AI_HARDLANDING.operator(false);
            this.AI_SOFTLANDING.operator(false);
            this.AI_RUN.operator(false);
            this.AI_PAIN.operator(false);
            this.AI_RELOAD.operator(false);
            this.AI_TELEPORT.operator(false);
            this.AI_TURN_LEFT.operator(false);
            this.AI_TURN_RIGHT.operator(false);

            // reset the script object
            ConstructScriptObject();

            // execute the script so the script object's constructor takes effect immediately
            this.scriptThread.Execute();

            this.forceScoreBoard = false;
            this.forcedReady = false;

            this.privateCameraView = null;

            this.lastSpectateChange = 0;
            this.lastTeleFX = -9999;

            this.hiddenWeapon = false;
            this.tipUp = false;
            this.objectiveUp = false;
            this.teleportEntity.oSet(null);
            this.teleportKiller = -1;
            this.leader = false;

            SetPrivateCameraView(null);

            this.lastSnapshotSequence = 0;

            this.MPAim = -1;
            this.lastMPAim = -1;
            this.lastMPAimTime = 0;
            this.MPAimFadeTime = 0;
            this.MPAimHighlight = false;

            if (this.hud != null) {
                this.hud.HandleNamedEvent("aim_clear");
            }

            cvarSystem.SetCVarBool("ui_chat", false);
        }

        public void PrepareForRestart() {
            ClearPowerUps();
            Spectate(true);
            this.forceRespawn = true;

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
                assert (this.spectating);
                SpawnFromSpawnSpot();
            }

            this.useInitialSpawns = true;
            UpdateSkinSetup(true);
        }

        /*
         ==============
         idPlayer::LinkScriptVariables

         set up conditions for animation
         ==============
         */
        public void LinkScriptVariables() {
            this.AI_FORWARD.LinkTo(this.scriptObject, "AI_FORWARD");
            this.AI_BACKWARD.LinkTo(this.scriptObject, "AI_BACKWARD");
            this.AI_STRAFE_LEFT.LinkTo(this.scriptObject, "AI_STRAFE_LEFT");
            this.AI_STRAFE_RIGHT.LinkTo(this.scriptObject, "AI_STRAFE_RIGHT");
            this.AI_ATTACK_HELD.LinkTo(this.scriptObject, "AI_ATTACK_HELD");
            this.AI_WEAPON_FIRED.LinkTo(this.scriptObject, "AI_WEAPON_FIRED");
            this.AI_JUMP.LinkTo(this.scriptObject, "AI_JUMP");
            this.AI_DEAD.LinkTo(this.scriptObject, "AI_DEAD");
            this.AI_CROUCH.LinkTo(this.scriptObject, "AI_CROUCH");
            this.AI_ONGROUND.LinkTo(this.scriptObject, "AI_ONGROUND");
            this.AI_ONLADDER.LinkTo(this.scriptObject, "AI_ONLADDER");
            this.AI_HARDLANDING.LinkTo(this.scriptObject, "AI_HARDLANDING");
            this.AI_SOFTLANDING.LinkTo(this.scriptObject, "AI_SOFTLANDING");
            this.AI_RUN.LinkTo(this.scriptObject, "AI_RUN");
            this.AI_PAIN.LinkTo(this.scriptObject, "AI_PAIN");
            this.AI_RELOAD.LinkTo(this.scriptObject, "AI_RELOAD");
            this.AI_TELEPORT.LinkTo(this.scriptObject, "AI_TELEPORT");
            this.AI_TURN_LEFT.LinkTo(this.scriptObject, "AI_TURN_LEFT");
            this.AI_TURN_RIGHT.LinkTo(this.scriptObject, "AI_TURN_RIGHT");
        }

        public void SetupWeaponEntity() {
            int w;
            String weap;

            if (this.weapon.GetEntity() != null) {
                // get rid of old weapon
                this.weapon.GetEntity().Clear();
                this.currentWeapon = -1;
            } else if (!gameLocal.isClient) {
                this.weapon.oSet((idWeapon) gameLocal.SpawnEntityType(idWeapon.class, null));
                this.weapon.GetEntity().SetOwner(this);
                this.currentWeapon = -1;
            }

            for (w = 0; w < MAX_WEAPONS; w++) {
                weap = this.spawnArgs.GetString(va("def_weapon%d", w));
                if ((weap != null) && !weap.isEmpty()) {
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
            final idStr skin = new idStr();

            spot = gameLocal.SelectInitialSpawnPoint(this);

            // set the player skin from the spawn location
            if (spot.spawnArgs.GetString("skin", null, skin)) {
                this.spawnArgs.Set("spawn_skin", skin);
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
            final idVec3 spawn_origin = new idVec3();
            final idAngles spawn_angles = new idAngles();

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

            this.respawning = true;

            Init();

            this.fl.noknockback = false;

            // stop any ragdolls being used
            StopRagdoll();

            // set back the player physics
            SetPhysics(this.physicsObj);

            this.physicsObj.SetClipModelAxis();
            this.physicsObj.EnableClip();

            if (!this.spectating) {
                SetCombatContents(true);
            }

            this.physicsObj.SetLinearVelocity(getVec3_origin());

            // setup our initial view
            if (!this.spectating) {
                SetOrigin(spawn_origin);
            } else {
                spec_origin = spawn_origin;
                spec_origin.oPluSet(2, pm_normalheight.GetFloat());
                spec_origin.oPluSet(2, SPECTATE_RAISE);
                SetOrigin(spec_origin);
            }

            // if this is the first spawn of the map, we don't have a usercmd yet,
            // so the delta angles won't be correct.  This will be fixed on the first think.
            this.viewAngles = getAng_zero();
            SetDeltaViewAngles(getAng_zero());
            SetViewAngles(spawn_angles);
            this.spawnAngles = spawn_angles;
            this.spawnAnglesSet = false;

            this.legsForward = true;
            this.legsYaw = 0;
            this.idealLegsYaw = 0;
            this.oldViewYaw = this.viewAngles.yaw;

            if (this.spectating) {
                Hide();
            } else {
                Show();
            }

            if (gameLocal.isMultiplayer) {
                if (!this.spectating) {
                    // we may be called twice in a row in some situations. avoid a double fx and 'fly to the roof'
                    if (this.lastTeleFX < (gameLocal.time - 1000)) {
                        idEntityFx.StartFx(this.spawnArgs.GetString("fx_spawn"), spawn_origin, null, this, true);
                        this.lastTeleFX = gameLocal.time;
                    }
                }
                this.AI_TELEPORT.operator(true);
            } else {
                this.AI_TELEPORT.operator(false);
            }

            // kill anything at the new position
            if (!this.spectating) {
                this.physicsObj.SetClipMask(MASK_PLAYERSOLID); // the clip mask is usually maintained in Move(), but KillBox requires it
                gameLocal.KillBox(this);
            }

            // don't allow full run speed for a bit
            this.physicsObj.SetKnockBack(100);

            // set our respawn time and buttons so that if we're killed we don't respawn immediately
            this.minRespawnTime = gameLocal.time;
            this.maxRespawnTime = gameLocal.time;
            if (!this.spectating) {
                this.forceRespawn = false;
            }

            this.privateCameraView = null;

            BecomeActive(TH_THINK);

            // run a client frame to drop exactly to the floor,
            // initialize animations and other things
            Think();

            this.respawning = false;
            this.lastManOver = false;
            this.lastManPlayAgain = false;
            this.isTelefragged = false;
        }

        public void SetClipModel() {
            idBounds bounds = new idBounds();

            if (this.spectating) {
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
                newClip.Translate(this.physicsObj.PlayerGetOrigin());
                this.physicsObj.SetClipModel(newClip, 1.0f);
            } else {
                newClip = new idClipModel(new idTraceModel(bounds));
                newClip.Translate(this.physicsObj.PlayerGetOrigin());
                this.physicsObj.SetClipModel(newClip, 1.0f);
            }
        }

        /*
         ===============
         idPlayer::SavePersistantInfo

         Saves any inventory and player stats when changing levels.
         ===============
         */
        public void SavePersistantInfo() {
            final idDict playerInfo = gameLocal.persistentPlayerInfo[this.entityNumber];

            playerInfo.Clear();
            this.inventory.GetPersistantData(playerInfo);
            playerInfo.SetInt("health", this.health);
            playerInfo.SetInt("current_weapon", this.currentWeapon);
        }

        /*
         ===============
         idPlayer::RestorePersistantInfo

         Restores any inventory and player stats when changing levels.
         ===============
         */
        public void RestorePersistantInfo() {
            if (gameLocal.isMultiplayer) {
                gameLocal.persistentPlayerInfo[this.entityNumber].Clear();
            }

            this.spawnArgs.Copy(gameLocal.persistentPlayerInfo[this.entityNumber]);

            this.inventory.RestoreInventory(this, this.spawnArgs);
            this.health = this.spawnArgs.GetInt("health", "100");
            if (!gameLocal.isClient) {
                this.idealWeapon = this.spawnArgs.GetInt("current_weapon", "1");
            }
        }

        public void SetLevelTrigger(final String levelName, final String triggerName) {
            if ((levelName != null) && !levelName.isEmpty() && (triggerName != null) && !triggerName.isEmpty()) {
                final idLevelTriggerInfo lti = new idLevelTriggerInfo();
                lti.levelName.oSet(levelName);
                lti.triggerName.oSet(triggerName);
                this.inventory.levelTriggers.Append(lti);
            }
        }

        public boolean UserInfoChanged(boolean canModify) {
            idDict userInfo;
            boolean modifiedInfo;
            boolean spec;
            boolean newready;

            userInfo = GetUserInfo();
            this.showWeaponViewModel = userInfo.GetBool("ui_showGun");

            if (!gameLocal.isMultiplayer) {
                return false;
            }

            modifiedInfo = false;

            spec = (idStr.Icmp(userInfo.GetString("ui_spectate"), "Spectate") == 0);
            if (gameLocal.serverInfo.GetBool("si_spectators")) {
                // never let spectators go back to game while sudden death is on
                if (canModify && (gameLocal.mpGame.GetGameState() == SUDDENDEATH) && !spec && (this.wantSpectate == true)) {
                    userInfo.Set("ui_spectate", "Spectate");
                    modifiedInfo |= true;
                } else {
                    if ((spec != this.wantSpectate) && !spec) {
                        // returning from spectate, set forceRespawn so we don't get stuck in spectate forever
                        this.forceRespawn = true;
                    }
                    this.wantSpectate = spec;
                }
            } else {
                if (canModify && spec) {
                    userInfo.Set("ui_spectate", "Play");
                    modifiedInfo |= true;
                } else if (this.spectating) {
                    // allow player to leaving spectator mode if they were in it when si_spectators got turned off
                    this.forceRespawn = true;
                }
                this.wantSpectate = false;
            }

            newready = (idStr.Icmp(userInfo.GetString("ui_ready"), "Ready") == 0);
            if ((this.ready != newready) && (gameLocal.mpGame.GetGameState() == WARMUP) && !this.wantSpectate) {
                gameLocal.mpGame.AddChatLine(common.GetLanguageDict().GetString("#str_07180"), userInfo.GetString("ui_name"), newready ? common.GetLanguageDict().GetString("#str_04300") : common.GetLanguageDict().GetString("#str_04301"));
            }
            this.ready = newready;
            this.team = (idStr.Icmp(userInfo.GetString("ui_team"), "Blue") & 1);//== 0);
            // server maintains TDM balance
            if (canModify && (gameLocal.gameType == GAME_TDM) && !gameLocal.mpGame.IsInGame(this.entityNumber) && g_balanceTDM.GetBool()) {
                modifiedInfo |= BalanceTDM();
            }
            UpdateSkinSetup(false);

            this.isChatting = userInfo.GetBool("ui_chat", "0");
            if (canModify && this.isChatting && this.AI_DEAD.operator()) {
                // if dead, always force chat icon off.
                this.isChatting = false;
                userInfo.SetBool("ui_chat", false);
                modifiedInfo |= true;
            }

            return modifiedInfo;
        }

        public idDict GetUserInfo() {
            return gameLocal.userInfo[this.entityNumber];
        }

        public boolean BalanceTDM() {
            int i, balanceTeam;
            final int[] teamCount = new int[2];
            idEntity ent;

            teamCount[ 0] = teamCount[ 1] = 0;
            for (i = 0; i < gameLocal.numClients; i++) {
                ent = gameLocal.entities[i];
                if ((ent != null) && ent.IsType(idPlayer.class)) {
                    teamCount[((idPlayer) ent).team]++;
                }
            }
            balanceTeam = -1;
            if (teamCount[ 0] < teamCount[ 1]) {
                balanceTeam = 0;
            } else if (teamCount[ 0] > teamCount[ 1]) {
                balanceTeam = 1;
            }
            if ((balanceTeam != -1) && (this.team != balanceTeam)) {
                common.DPrintf("team balance: forcing player %d to %s team\n", this.entityNumber, itob(balanceTeam) ? "blue" : "red");
                this.team = balanceTeam;
                GetUserInfo().Set("ui_team", itob(this.team) ? "Blue" : "Red");
                return true;
            }
            return false;
        }

        public void CacheWeapons() {
            String weap;
            int w;

            // check if we have any weapons
            if (0 == this.inventory.weapons) {
                return;
            }

            for (w = 0; w < MAX_WEAPONS; w++) {
                if ((this.inventory.weapons & (1 << w)) != 0) {
                    weap = this.spawnArgs.GetString(va("def_weapon%d", w));
                    if (!"".equals(weap)) {
                        idWeapon.CacheWeapon(weap);
                    } else {
                        this.inventory.weapons &= ~(1 << w);
                    }
                }
            }
        }

        public void EnterCinematic() {
            Hide();
            StopAudioLog();
            StopSound(etoi(SND_CHANNEL_PDA), false);
            if (this.hud != null) {
                this.hud.HandleNamedEvent("radioChatterDown");
            }

            this.physicsObj.SetLinearVelocity(getVec3_origin());

            SetState("EnterCinematic");
            UpdateScript();

            if (this.weaponEnabled && (this.weapon.GetEntity() != null)) {
                this.weapon.GetEntity().EnterCinematic();
            }

            this.AI_FORWARD.operator(false);
            this.AI_BACKWARD.operator(false);
            this.AI_STRAFE_LEFT.operator(false);
            this.AI_STRAFE_RIGHT.operator(false);
            this.AI_RUN.operator(false);
            this.AI_ATTACK_HELD.operator(false);
            this.AI_WEAPON_FIRED.operator(false);
            this.AI_JUMP.operator(false);
            this.AI_CROUCH.operator(false);
            this.AI_ONGROUND.operator(true);
            this.AI_ONLADDER.operator(false);
            this.AI_DEAD.operator(this.health <= 0);
            this.AI_RUN.operator(false);
            this.AI_PAIN.operator(false);
            this.AI_HARDLANDING.operator(false);
            this.AI_SOFTLANDING.operator(false);
            this.AI_RELOAD.operator(false);
            this.AI_TELEPORT.operator(false);
            this.AI_TURN_LEFT.operator(false);
            this.AI_TURN_RIGHT.operator(false);
        }

        public void ExitCinematic() {
            Show();

            if (this.weaponEnabled && (this.weapon.GetEntity() != null)) {
                this.weapon.GetEntity().ExitCinematic();
            }

            SetState("ExitCinematic");
            UpdateScript();
        }

        public boolean HandleESC() {
            if (gameLocal.inCinematic) {
                return SkipCinematic();
            }

            if (this.objectiveSystemOpen) {
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
            velocity = this.physicsObj.GetLinearVelocity().oMinus(this.physicsObj.GetPushedLinearVelocity());
            fallspeed = velocity.oMultiply(this.physicsObj.GetGravityNormal());

            if (this.influenceActive != 0) {
                this.AI_FORWARD.operator(false);
                this.AI_BACKWARD.operator(false);
                this.AI_STRAFE_LEFT.operator(false);
                this.AI_STRAFE_RIGHT.operator(false);
            } else if ((gameLocal.time - this.lastDmgTime) < 500) {
                forwardspeed = velocity.oMultiply(this.viewAxis.oGet(0));
                sidespeed = velocity.oMultiply(this.viewAxis.oGet(1));
                this.AI_FORWARD.operator(this.AI_ONGROUND.operator() && (forwardspeed > 20.01f));
                this.AI_BACKWARD.operator(this.AI_ONGROUND.operator() && (forwardspeed < -20.01f));
                this.AI_STRAFE_LEFT.operator(this.AI_ONGROUND.operator() && (sidespeed > 20.01f));
                this.AI_STRAFE_RIGHT.operator(this.AI_ONGROUND.operator() && (sidespeed < -20.01f));
            } else if (this.xyspeed > MIN_BOB_SPEED) {
                this.AI_FORWARD.operator(this.AI_ONGROUND.operator() && (this.usercmd.forwardmove > 0));
                this.AI_BACKWARD.operator(this.AI_ONGROUND.operator() && (this.usercmd.forwardmove < 0));
                this.AI_STRAFE_LEFT.operator(this.AI_ONGROUND.operator() && (this.usercmd.rightmove < 0));
                this.AI_STRAFE_RIGHT.operator(this.AI_ONGROUND.operator() && (this.usercmd.rightmove > 0));
            } else {
                this.AI_FORWARD.operator(false);
                this.AI_BACKWARD.operator(false);
                this.AI_STRAFE_LEFT.operator(false);
                this.AI_STRAFE_RIGHT.operator(false);
            }

            this.AI_RUN.operator(((this.usercmd.buttons & BUTTON_RUN) != 0) && ((NOT(pm_stamina.GetFloat())) || (this.stamina > pm_staminathreshold.GetFloat())));
            this.AI_DEAD.operator(this.health <= 0);
        }

        public void SetViewAngles(final idAngles angles) {
            UpdateDeltaViewAngles(angles);
            this.viewAngles = angles;
        }

        // delta view angles to allow movers to rotate the view of the player
        public void UpdateDeltaViewAngles(final idAngles angles) {
            // set the delta angle
            final idAngles delta = new idAngles();
            for (int i = 0; i < 3; i++) {
                delta.oSet(i, angles.oGet(i) - SHORT2ANGLE(this.usercmd.angles[i]));
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
                if (!this.spectating) {
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
                for (i = 0; i < this.aasLocation.Num(); i++) {
                    if (aas == gameLocal.GetAAS(i)) {
                        areaNum[0] = this.aasLocation.oGet(i).areaNum;
                        pos = this.aasLocation.oGet(i).pos;
                        return;
                    }
                }
            }

            areaNum[0] = 0;
            pos = this.physicsObj.GetOrigin();
        }

        /*
         =====================
         idPlayer::GetAIAimTargets

         Returns positions for the AI to aim at.
         =====================
         */
        @Override
        public void GetAIAimTargets(final idVec3 lastSightPos, idVec3 headPos, idVec3 chestPos) {
            final idVec3 offset = new idVec3();
            final idMat3 axis = new idMat3();
            idVec3 origin;

            origin = lastSightPos.oMinus(this.physicsObj.GetOrigin());

            GetJointWorldTransform(this.chestJoint, gameLocal.time, offset, axis);
            headPos = offset.oPlus(origin);

            GetJointWorldTransform(this.headJoint, gameLocal.time, offset, axis);
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
            if ((damage[0] != 0) && (victim != this) && victim.IsType(idActor.class)) {
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
            final int[] damage = {0};
            int armorSave;

            damageDef.GetInt("damage", "20", damage);
            damage[0] = GetDamageForLocation(damage[0], location);

            final idPlayer player = attacker.IsType(idPlayer.class) ? (idPlayer) attacker : null;
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
                if (this.godmode) {
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
                if (armorSave >= this.inventory.armor) {
                    armorSave = this.inventory.armor;
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
            if ((gameLocal.gameType == GAME_TDM)
                    && !gameLocal.serverInfo.GetBool("si_teamDamage")
                    && !damageDef.GetBool("noTeam")
                    && (player != null)
                    && !player.equals(this)// you get self damage no matter what
                    && (player.team == this.team)) {
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
            final int[] damage = {0};
            final int[] armorSave = {0};
            final int[] knockback = {0};
            idVec3 damage_from;
            final idVec3 localDamageVector = new idVec3();
            final float[] attackerPushScale = {0};

            // damage is only processed on server
            if (gameLocal.isClient) {
                return;
            }

            if (!this.fl.takedamage || this.noclip || this.spectating || gameLocal.inCinematic) {
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
                if (this.influenceActive != 0) {
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

            if ((knockback[0] != 0) && !this.fl.noknockback) {
                if (attacker == this) {
                    damageDef.dict.GetFloat("attackerPushScale", "0", attackerPushScale);
                } else {
                    attackerPushScale[0] = 1.0f;
                }

                kick = dir;
                kick.Normalize();
                kick.oMulSet((g_knockback.GetFloat() * knockback[0] * attackerPushScale[0]) / 200);
                this.physicsObj.SetLinearVelocity(this.physicsObj.GetLinearVelocity().oPlus(kick));

                // set the timer so that the player can't cancel out the movement immediately
                this.physicsObj.SetKnockBack(idMath.ClampInt(50, 200, knockback[0] * 2));
            }

            // give feedback on the player view and audibly when armor is helping
            if (armorSave[0] != 0) {
                this.inventory.armor -= armorSave[0];

                if (gameLocal.time > (this.lastArmorPulse + 200)) {
                    StartSound("snd_hitArmor", SND_CHANNEL_ITEM, 0, false, null);
                }
                this.lastArmorPulse = gameLocal.time;
            }

            if (damageDef.dict.GetBool("burn")) {
                StartSound("snd_burn", SND_CHANNEL_BODY3, 0, false, null);
            } else if (damageDef.dict.GetBool("no_air")) {
                if ((0 == armorSave[0]) && (this.health > 0)) {
                    StartSound("snd_airGasp", SND_CHANNEL_ITEM, 0, false, null);
                }
            }

            if (g_debugDamage.GetInteger() != 0) {
                gameLocal.Printf("client:%d health:%d damage:%d armor:%d\n",
                        this.entityNumber, this.health, damage[0], armorSave[0]);
            }

            // move the world direction vector to local coordinates
            damage_from = dir;
            damage_from.Normalize();

            this.viewAxis.ProjectVector(damage_from, localDamageVector);

            // add to the damage inflicted on a player this frame
            // the total will be turned into screen blends and view angle kicks
            // at the end of the frame
            if (this.health > 0) {
                this.playerView.DamageImpulse(localDamageVector, damageDef.dict);
            }

            // do the damage
            if (damage[0] > 0) {

                if (!gameLocal.isMultiplayer) {
                    float scale = g_damageScale.GetFloat();
                    if (g_useDynamicProtection.GetBool() && (g_skill.GetInteger() < 2)) {
                        if ((gameLocal.time > (this.lastDmgTime + 500)) && (scale > 0.25f)) {
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

                final int oldHealth = this.health;
                this.health -= damage[0];

                if (this.health <= 0) {

                    if (this.health < -999) {
                        this.health = -999;
                    }

                    this.isTelefragged = damageDef.dict.GetBool("telefrag");

                    this.lastDmgTime = gameLocal.time;
                    Killed(inflictor, attacker, damage[0], dir, location);

                } else {
                    // force a blink
                    this.blink_time = 0;

                    // let the anim script know we took damage
                    this.AI_PAIN.operator(Pain(inflictor, attacker, damage[0], dir, location));
                    if (!g_testDeath.GetBool()) {
                        this.lastDmgTime = gameLocal.time;
                    }
                }
            } else {
                // don't accumulate impulses
                if (this.af.IsLoaded()) {
                    // clear impacts
                    this.af.Rest();

                    // physics is turned off by calling af.Rest()
                    BecomeActive(TH_PHYSICS);
                }
            }

            this.lastDamageDef = damageDef.Index();
            this.lastDamageDir = damage_from;
            this.lastDamageLocation = location;
        }

        // use exitEntityNum to specify a teleport with private camera view and delayed exit
        @Override
        public void Teleport(final idVec3 origin, final idAngles angles, idEntity destination) {
            final idVec3 org = new idVec3();

            if (this.weapon.GetEntity() != null) {
                this.weapon.GetEntity().LowerWeapon();
            }

            SetOrigin(origin.oPlus(new idVec3(0, 0, CM_CLIP_EPSILON)));
            if (!gameLocal.isMultiplayer && GetFloorPos(16.0f, org)) {
                SetOrigin(org);
            }

            // clear the ik heights so model doesn't appear in the wrong place
            this.walkIK.EnableAll();

            GetPhysics().SetLinearVelocity(getVec3_origin());

            SetViewAngles(angles);

            this.legsYaw = 0;
            this.idealLegsYaw = 0;
            this.oldViewYaw = this.viewAngles.yaw;

            if (gameLocal.isMultiplayer) {
                this.playerView.Flash(colorWhite, 140);
            }

            UpdateVisuals();

            this.teleportEntity.oSet(destination);

            if (!gameLocal.isClient && !this.noclip) {
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
            if (this.spectating) {
                SpectateFreeFly(false);
            } else if (this.health > 0) {
                this.godmode = false;
                if (nodamage) {
                    ServerSpectate(true);
                    this.forceRespawn = true;
                } else {
                    Damage(this, this, getVec3_origin(), "damage_suicide", 1.0f, INVALID_JOINT);
                    if (delayRespawn) {
                        this.forceRespawn = false;
                        final float delay = this.spawnArgs.GetFloat("respawn_delay");
                        this.minRespawnTime = (int) (gameLocal.time + SEC2MS(delay));
                        this.maxRespawnTime = this.minRespawnTime + MAX_RESPAWN_TIME;
                    }
                }
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            float delay;

            assert (!gameLocal.isClient);

            // stop taking knockback once dead
            this.fl.noknockback = true;
            if (this.health < -999) {
                this.health = -999;
            }

            if (this.AI_DEAD.operator()) {
                this.AI_PAIN.operator(true);
                return;
            }

            this.heartInfo.Init(0, 0, 0f, 0f + BASE_HEARTRATE);
            AdjustHeartRate(DEAD_HEARTRATE, 10, 0, true);

            if (!g_testDeath.GetBool()) {
                this.playerView.Fade(colorBlack, 12000);
            }

            this.AI_DEAD.operator(true);
            SetAnimState(ANIMCHANNEL_LEGS, "Legs_Death", 4);
            SetAnimState(ANIMCHANNEL_TORSO, "Torso_Death", 4);
            SetWaitState("");

            this.animator.ClearAllJoints();

            if (StartRagdoll()) {
                pm_modelView.SetInteger(0);
                this.minRespawnTime = gameLocal.time + RAGDOLL_DEATH_TIME;
                this.maxRespawnTime = this.minRespawnTime + MAX_RESPAWN_TIME;
            } else {
                // don't allow respawn until the death anim is done
                // g_forcerespawn may force spawning at some later time
                delay = this.spawnArgs.GetFloat("respawn_delay");
                this.minRespawnTime = (int) (gameLocal.time + SEC2MS(delay));
                this.maxRespawnTime = this.minRespawnTime + MAX_RESPAWN_TIME;
            }

            this.physicsObj.SetMovementType(PM_DEAD);
            StartSound("snd_death", SND_CHANNEL_VOICE, 0, false, null);
            StopSound(etoi(SND_CHANNEL_BODY2), false);

            this.fl.takedamage = true;		// can still be gibbed

            // get rid of weapon
            this.weapon.GetEntity().OwnerDied();

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
                    if ((this.health < -20) || killer.PowerUpActive(BERSERK)) {
                        this.gibDeath = true;
                        this.gibsDir = dir;
                        this.gibsLaunched = false;
                    }
                }
                gameLocal.mpGame.PlayerDeath(this, killer, this.isTelefragged);
            } else {
                this.physicsObj.SetContents(CONTENTS_CORPSE | CONTENTS_MONSTERCLIP);
            }

            ClearPowerUps();

            UpdateVisuals();

            this.isChatting = false;
        }

        public void StartFxOnBone(final String fx, final String bone) {
            idVec3 offset = new idVec3();
            idMat3 axis = new idMat3();
            final int/*jointHandle_t*/ jointHandle = GetAnimator().GetJointHandle(bone);

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
            return this.renderView;
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

            if (NOT(this.renderView)) {
                this.renderView = new renderView_s();
            }
//	memset( renderView, 0, sizeof( *renderView ) );

            // copy global shader parms
            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                this.renderView.shaderParms[i] = gameLocal.globalShaderParms[i];
            }
            this.renderView.globalMaterial = gameLocal.GetGlobalMaterial();
            this.renderView.time = gameLocal.time;

            // calculate size of 3D view
            this.renderView.x = 0;
            this.renderView.y = 0;
            this.renderView.width = SCREEN_WIDTH;
            this.renderView.height = SCREEN_HEIGHT;
            this.renderView.viewID = 0;

            // check if we should be drawing from a camera's POV
            if (!this.noclip && ((gameLocal.GetCamera() != null) || (this.privateCameraView != null))) {
                // get origin, axis, and fov
                if (this.privateCameraView != null) {
                    this.privateCameraView.GetViewParms(this.renderView);
                } else {
                    gameLocal.GetCamera().GetViewParms(this.renderView);
                }
            } else {
                if (g_stopTime.GetBool()) {
                    this.renderView.vieworg = new idVec3(this.firstPersonViewOrigin);
                    this.renderView.viewaxis = new idMat3(this.firstPersonViewAxis);

                    if (!pm_thirdPerson.GetBool()) {
                        // set the viewID to the clientNum + 1, so we can suppress the right player bodies and
                        // allow the right player view weapons
                        this.renderView.viewID = this.entityNumber + 1;
                    }
                } else if (pm_thirdPerson.GetBool()) {
                    OffsetThirdPersonView(pm_thirdPersonAngle.GetFloat(), pm_thirdPersonRange.GetFloat(), pm_thirdPersonHeight.GetFloat(), pm_thirdPersonClip.GetBool());
                } else if (pm_thirdPersonDeath.GetBool()) {
                    range = gameLocal.time < this.minRespawnTime ? ((gameLocal.time + RAGDOLL_DEATH_TIME) - this.minRespawnTime) * (120 / RAGDOLL_DEATH_TIME) : 120;
                    OffsetThirdPersonView(0, 20 + range, 0, false);
                } else {
                    this.renderView.vieworg = new idVec3(this.firstPersonViewOrigin);
                    this.renderView.viewaxis = new idMat3(this.firstPersonViewAxis);

                    // set the viewID to the clientNum + 1, so we can suppress the right player bodies and
                    // allow the right player view weapons
                    this.renderView.viewID = this.entityNumber + 1;
                }

                // field of view
                {
                    final float[] fov_x = {this.renderView.fov_x};
                    final float[] fov_y = {this.renderView.fov_y};
                    gameLocal.CalcFov(CalcFov(true), fov_x, fov_y);
                    this.renderView.fov_x = fov_x[0];
                    this.renderView.fov_y = fov_y[0];
                }
            }

            if (this.renderView.fov_y == 0) {
                common.Error("renderView.fov_y == 0");
            }

            if (g_showviewpos.GetBool()) {
                gameLocal.Printf("%s : %s\n", this.renderView.vieworg.ToString(), this.renderView.viewaxis.ToAngles().ToString());
            }
        }

        /*
        ===============
        idPlayer::CalculateFirstPersonView
        ===============
        */
        public void CalculateFirstPersonView() {
            if ((pm_modelView.GetInteger() == 1) || ((pm_modelView.GetInteger() == 2) && (this.health <= 0))) {
                //	Displays the view from the point of view of the "camera" joint in the player model

                final idMat3 axis = new idMat3();
                final idVec3 origin = new idVec3();
                idAngles ang;

                ang = this.viewBobAngles.oPlus(this.playerView.AngleOffset());
                ang.yaw += this.viewAxis.oGet(0).ToYaw();

                final int joint = this.animator.GetJointHandle("camera");
                this.animator.GetJointTransform(joint, gameLocal.time, origin, axis);
                this.firstPersonViewOrigin = (origin.oPlus(this.modelOffset)).oMultiply(this.viewAxis.oMultiply(this.physicsObj.GetGravityAxis())).oPlus(this.physicsObj.GetOrigin()).oPlus(this.viewBob);
                this.firstPersonViewAxis = axis.oMultiply(ang.ToMat3()).oMultiply(this.physicsObj.GetGravityAxis());
            } else {
                // offset for local bobbing and kicks
                GetViewPos(this.firstPersonViewOrigin, this.firstPersonViewAxis);
                if (false) {
                    // shakefrom sound stuff only happens in first person
                    this.firstPersonViewAxis = this.firstPersonViewAxis.oMultiply(this.playerView.ShakeAxis());
                }
            }
        }

        public void DrawHUD(idUserInterface _hud) {

            if (NOT(this.weapon.GetEntity()) || (this.influenceActive != INFLUENCE_NONE) || (this.privateCameraView != null) || (gameLocal.GetCamera() != null) || NOT(_hud) || !g_showHud.GetBool()) {
                return;
            }

            UpdateHudStats(_hud);

            _hud.SetStateString("weapicon", this.weapon.GetEntity().Icon());

            // FIXME: this is temp to allow the sound meter to show up in the hud
            // it should be commented out before shipping but the code can remain
            // for mod developers to enable for the same functionality
            _hud.SetStateInt("s_debug", cvarSystem.GetCVarInteger("s_showLevelMeter"));

            this.weapon.GetEntity().UpdateGUI();

            _hud.Redraw(gameLocal.realClientTime);

            // weapon targeting crosshair
            if (!GuiActive()) {
                if ((this.cursor != null) && this.weapon.GetEntity().ShowCrosshair()) {
                    this.cursor.Redraw(gameLocal.realClientTime);
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
            this.blink_time = 0;

            // play the fire animation
            this.AI_WEAPON_FIRED.operator(true);

            // update view feedback
            this.playerView.WeaponFireFeedback(weaponDef);
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

            if (this.fxFov) {
                return (float) (DefaultFov() + 10 + (cos((gameLocal.time + 2000) * 0.01) * 10));
            }

            if (this.influenceFov != 0) {
                return this.influenceFov;
            }

            if (this.zoomFov.IsDone(gameLocal.time)) {
                fov = ((honorZoom && ((this.usercmd.buttons & BUTTON_ZOOM) != 0)) && (this.weapon.GetEntity() != null)) ? this.weapon.GetEntity().GetZoomFov() : DefaultFov();
            } else {
                fov = this.zoomFov.GetCurrentValue(gameLocal.time);
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
            final idAngles angles = new idAngles();
            int delta;

            // CalculateRenderView must have been called first
            final idVec3 viewOrigin = this.firstPersonViewOrigin;
            final idMat3 viewAxis = this.firstPersonViewAxis;

            // these cvars are just for hand tweaking before moving a value to the weapon def
            final idVec3 gunpos = new idVec3(g_gun_x.GetFloat(), g_gun_y.GetFloat(), g_gun_z.GetFloat());

            // as the player changes direction, the gun will take a small lag
            final idVec3 gunOfs = GunAcceleratingOffset();
            origin.oSet(viewOrigin.oPlus(gunpos.oPlus(gunOfs).oMultiply(viewAxis)));

            // on odd legs, invert some angles
            if ((this.bobCycle & 128) != 0) {
                scale = -this.xyspeed;
            } else {
                scale = this.xyspeed;
            }

            // gun angles from bobbing
            angles.roll = scale * this.bobfracsin * 0.005f;
            angles.yaw = scale * this.bobfracsin * 0.01f;
            angles.pitch = this.xyspeed * this.bobfracsin * 0.005f;

            // gun angles from turning
            if (gameLocal.isMultiplayer) {
                final idAngles offset = GunTurningOffset();
                offset.oMulSet(g_mpWeaponAngleScale.GetFloat());
                angles.oPluSet(offset);
            } else {
                angles.oPluSet(GunTurningOffset());
            }

            final idVec3 gravity = this.physicsObj.GetGravityNormal();

            // drop the weapon when landing after a jump / fall
            delta = gameLocal.time - this.landTime;
            if (delta < LAND_DEFLECT_TIME) {
                origin.oMinSet(gravity.oMultiply((this.landChange * 0.25f * delta) / LAND_DEFLECT_TIME));
            } else if (delta < (LAND_DEFLECT_TIME + LAND_RETURN_TIME)) {
                origin.oMinSet(gravity.oMultiply((this.landChange * 0.25f * ((LAND_DEFLECT_TIME + LAND_RETURN_TIME) - delta)) / LAND_RETURN_TIME));
            }

            // speed sensitive idle drift
            scale = this.xyspeed + 40;
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
            if (gameLocal.isClient && (this.entityNumber != gameLocal.localClientNum)) {
                org = this.smoothedOrigin;
            } else {
                org = GetPhysics().GetOrigin();
            }
            return org.oPlus(GetPhysics().GetGravityNormal().oMultiply(-this.eyeOffset.z));
        }

        @Override
        public void GetViewPos(idVec3 origin, idMat3 axis) {
            idAngles angles = new idAngles();

            // if dead, fix the angle and don't add any kick
            if (this.health <= 0) {
                angles.yaw = this.viewAngles.yaw;
                angles.roll = 40;
                angles.pitch = -15;
                axis.oSet(angles.ToMat3());//TODO:null check
                origin.oSet(GetEyePosition());
            } else {
                origin.oSet(GetEyePosition().oPlus(this.viewBob));
                angles = this.viewAngles.oPlus(this.viewBobAngles).oPlus(this.playerView.AngleOffset());

                axis.oSet(angles.ToMat3().oMultiply(this.physicsObj.GetGravityAxis()));

                // adjust the origin based on the camera nodal distance (eye distance from neck)
                origin.oPluSet(this.physicsObj.GetGravityNormal().oMultiply(g_viewNodalZ.GetFloat()));
                origin.oPluSet(axis.oGet(0).oMultiply(g_viewNodalX.GetFloat()).oPlus(axis.oGet(2).oMultiply(g_viewNodalZ.GetFloat())));
            }
        }

        public void OffsetThirdPersonView(float angle, float range, float height, boolean clip) {
            idVec3 view;
//            idVec3 focusAngles;
            final trace_s[] trace = {null};
            idVec3 focusPoint;
            float focusDist;
            final float[] forwardScale = {0}, sideScale = {0};
            final idVec3 origin = new idVec3();
            idAngles angles;
            final idMat3 axis = new idMat3();
            idBounds bounds;

            angles = this.viewAngles;
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
            this.renderView.viewaxis = angles.ToMat3().oMultiply(this.physicsObj.GetGravityAxis());

            idMath.SinCos(DEG2RAD(angle), sideScale, forwardScale);
            view.oMinSet(this.renderView.viewaxis.oGet(0).oMultiply(range * forwardScale[0]));
            view.oPluSet(this.renderView.viewaxis.oGet(1).oMultiply(range * sideScale[0]));

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
            focusDist = idMath.Sqrt((focusPoint.oGet(0) * focusPoint.oGet(0)) + (focusPoint.oGet(1) * focusPoint.oGet(1)));
            if (focusDist < 1.0f) {
                focusDist = 1.0f;	// should never happen
            }

            angles.pitch = -RAD2DEG(atan2(focusPoint.z, focusDist));
            angles.yaw -= angle;

            this.renderView.vieworg = new idVec3(view);
            this.renderView.viewaxis = angles.ToMat3().oMulSet(this.physicsObj.GetGravityAxis());
            this.renderView.viewID = 0;
        }

        public boolean Give(final String statname, final String value) {
            int amount;

            if (this.AI_DEAD.operator()) {
                return false;
            }

            if (0 == idStr.Icmp(statname, "health")) {
                if (this.health >= this.inventory.maxHealth) {
                    return false;
                }
                amount = Integer.parseInt(value);
                if (amount != 0) {
                    this.health += amount;
                    if (this.health > this.inventory.maxHealth) {
                        this.health = this.inventory.maxHealth;
                    }
                    if (this.hud != null) {
                        this.hud.HandleNamedEvent("healthPulse");
                    }
                }

            } else if (0 == idStr.Icmp(statname, "stamina")) {
                if (this.stamina >= 100) {
                    return false;
                }
                this.stamina += Float.parseFloat(value);
                if (this.stamina > 100) {
                    this.stamina = 100;
                }

            } else if (0 == idStr.Icmp(statname, "heartRate")) {
                this.heartRate += Integer.parseInt(value);
                if (this.heartRate > MAX_HEARTRATE) {
                    this.heartRate = MAX_HEARTRATE;
                }

            } else if (0 == idStr.Icmp(statname, "air")) {
                if (this.airTics >= pm_airTics.GetInteger()) {
                    return false;
                }
                this.airTics += (Integer.parseInt(value) / 100.0) * pm_airTics.GetInteger();
                if (this.airTics > pm_airTics.GetInteger()) {
                    this.airTics = pm_airTics.GetInteger();
                }
            } else {
                final int[] idealWeapon = {this.idealWeapon};
                final boolean result = this.inventory.Give(this, this.spawnArgs, statname, value, idealWeapon, true);
                this.idealWeapon = idealWeapon[0];
                return result;
            }
            return true;
        }

        public boolean Give(final idStr statname, final idStr value) {
            return this.Give(statname.getData(), value.getData());
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
            final idDict attr = new idDict();
            boolean gave;
            int numPickup;

            if (gameLocal.isMultiplayer && this.spectating) {
                return false;
            }

            item.GetAttributes(attr);

            gave = false;
            numPickup = this.inventory.pickupItemNames.Num();
            for (i = 0; i < attr.GetNumKeyVals(); i++) {
                arg = attr.GetKeyVal(i);
                if (Give(arg.GetKey(), arg.GetValue())) {
                    gave = true;
                }
            }

            arg = item.spawnArgs.MatchPrefix("inv_weapon", null);
            if ((arg != null) && (this.hud != null)) {
                // We need to update the weapon hud manually, but not
                // the armor/ammo/health because they are updated every
                // frame no matter what
                UpdateHudWeapon(false);
                this.hud.HandleNamedEvent("weaponPulse");
            }

            // display the pickup feedback on the hud
            if (gave && (numPickup == this.inventory.pickupItemNames.Num())) {
                this.inventory.AddPickupName(item.spawnArgs.GetString("inv_name"), item.spawnArgs.GetString("inv_icon"));
            }

            return gave;
        }

        public void GiveItem(final String itemName) {
            final idDict args = new idDict();

            args.Set("classname", itemName);
            args.Set("owner", this.name);
            gameLocal.SpawnEntityDef(args);
            if (this.hud != null) {
                this.hud.HandleNamedEvent("itemPickup");
            }
        }

        /*
         ===============
         idPlayer::GiveHealthPool

         adds health to the player health pool
         ===============
         */
        public void GiveHealthPool(float amt) {

            if (this.AI_DEAD.operator()) {
                return;
            }

            if (this.health > 0) {
                this.healthPool += amt;
                if (this.healthPool > (this.inventory.maxHealth - this.health)) {
                    this.healthPool = this.inventory.maxHealth - this.health;
                }
                this.nextHealthPulse = gameLocal.time;
            }
        }

        public boolean GiveInventoryItem(idDict item) {
            if (gameLocal.isMultiplayer && this.spectating) {
                return false;
            }
            this.inventory.items.Append(new idDict(item));
            final idItemInfo info = new idItemInfo();
            final String itemName = item.GetString("inv_name");
            if (idStr.Cmpn(itemName, STRTABLE_ID, STRTABLE_ID_LENGTH) == 0) {
                info.name.oSet(common.GetLanguageDict().GetString(itemName));
            } else {
                info.name.oSet(itemName);
            }
            info.icon.oSet(item.GetString("inv_icon"));
            this.inventory.pickupItemNames.Append(info);
            if (this.hud != null) {
                this.hud.SetStateString("itemicon", info.icon.getData());
                this.hud.HandleNamedEvent("invPickup");
            }
            return true;
        }

        public void RemoveInventoryItem(idDict item) {
            this.inventory.items.Remove(item);
//	delete item;
        }

        public boolean GiveInventoryItem(final String name) {
            final idDict args = new idDict();

            args.Set("classname", name);
            args.Set("owner", this.name);
            gameLocal.SpawnEntityDef(args);
            return true;
        }

        public void RemoveInventoryItem(final String name) {
            final idDict item = FindInventoryItem(name);
            if (item != null) {
                RemoveInventoryItem(item);
            }
        }

        public idDict FindInventoryItem(final String name) {
            for (int i = 0; i < this.inventory.items.Num(); i++) {
                final String iname = this.inventory.items.oGet(i).GetString("inv_name");
                if ((iname != null) && !iname.isEmpty()) {
                    if (idStr.Icmp(name, iname) == 0) {
                        return this.inventory.items.oGet(i);
                    }
                }
            }
            return null;
        }

        public idDict FindInventoryItem(final idStr name) {
            return FindInventoryItem(name.getData());
        }

        public void GivePDA(final idStr pdaName, idDict item) {
            if (gameLocal.isMultiplayer && this.spectating) {
                return;
            }

            if (item != null) {
                this.inventory.pdaSecurity.AddUnique(item.GetString("inv_name"));
            }

            if (isNotNullOrEmpty(pdaName)) {
                pdaName.oSet("personal");
            }

            final idDeclPDA pda = (idDeclPDA) declManager.FindType(DECL_PDA, pdaName);

            this.inventory.pdas.AddUnique(pdaName);

            // Copy any videos over
            for (int i = 0; i < pda.GetNumVideos(); i++) {
                final idDeclVideo video = pda.GetVideoByIndex(i);
                if (video != null) {
                    this.inventory.videos.AddUnique(video.GetName());
                }
            }

            // This is kind of a hack, but it works nicely
            // We don't want to display the 'you got a new pda' message during a map load
            if (gameLocal.GetFrameNum() > 10) {
                if ((pda != null) && (this.hud != null)) {
                    pdaName.oSet(pda.GetPdaName());
                    pdaName.RemoveColors();
                    this.hud.SetStateString("pda", "1");
                    this.hud.SetStateString("pda_text", pdaName.getData());
                    final String sec = pda.GetSecurity();
                    this.hud.SetStateString("pda_security", ((sec != null) && !sec.isEmpty()) ? "1" : "0");//TODO:!= null and !usEmpty, check that this combination isn't the wrong way around anywhere. null== instead of !=null
                    this.hud.HandleNamedEvent("pdaPickup");
                }

                if (this.inventory.pdas.Num() == 1) {
                    GetPDA().RemoveAddedEmailsAndVideos();
                    if (!this.objectiveSystemOpen) {
                        TogglePDA();
                    }
                    this.objectiveSystem.HandleNamedEvent("showPDATip");
                    //ShowTip( spawnArgs.GetString( "text_infoTitle" ), spawnArgs.GetString( "text_firstPDA" ), true );
                }

                if ((this.inventory.pdas.Num() > 1) && (pda.GetNumVideos() > 0) && (this.hud != null)) {
                    this.hud.HandleNamedEvent("videoPickup");
                }
            }
        }

        public void GiveVideo(final String videoName, idDict item) {

            if ((videoName == null) || videoName.isEmpty()) {
                return;
            }

            this.inventory.videos.AddUnique(videoName);

            if (item != null) {
                final idItemInfo info = new idItemInfo();
                info.name.oSet(item.GetString("inv_name"));
                info.icon.oSet(item.GetString("inv_icon"));
                this.inventory.pickupItemNames.Append(info);
            }
            if (this.hud != null) {
                this.hud.HandleNamedEvent("videoPickup");
            }
        }

        public void GiveEmail(final String emailName) {

            if ((emailName == null) || emailName.isEmpty()) {
                return;
            }

            this.inventory.emails.AddUnique(emailName);
            GetPDA().AddEmail(emailName);

            if (this.hud != null) {
                this.hud.HandleNamedEvent("emailPickup");
            }
        }

        public void GiveSecurity(final String security) {
            GetPDA().SetSecurity(security);
            if (this.hud != null) {
                this.hud.SetStateString("pda_security", "1");
                this.hud.HandleNamedEvent("securityPickup");
            }
        }

        public void GiveObjective(final String title, final String text, final String screenshot) {
            final idObjectiveInfo info = new idObjectiveInfo();
            info.title = new idStr(title);
            info.text = new idStr(text);
            info.screenshot = new idStr(screenshot);
            this.inventory.objectiveNames.Append(info);
            ShowObjective("newObjective");
            if (this.hud != null) {
                this.hud.HandleNamedEvent("newObjective");
            }
        }

        public void CompleteObjective(final String title) {
            final int c = this.inventory.objectiveNames.Num();
            for (int i = 0; i < c; i++) {
                if (idStr.Icmp(this.inventory.objectiveNames.oGet(i).title.getData(), title) == 0) {
                    this.inventory.objectiveNames.RemoveIndex(i);
                    break;
                }
            }
            ShowObjective("newObjectiveComplete");

            if (this.hud != null) {
                this.hud.HandleNamedEvent("newObjectiveComplete");
            }
        }

        public boolean GivePowerUp(int powerup, int time) {
            final String[] sound = {null};
            final String[] skin = {null};

            if ((powerup >= 0) && (powerup < MAX_POWERUPS)) {

                if (gameLocal.isServer) {
                    final idBitMsg msg = new idBitMsg();
                    final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                    msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                    msg.WriteShort(powerup);
                    msg.WriteBits(1, 1);
                    ServerSendEvent(EVENT_POWERUP, msg, false, -1);
                }

                if (powerup != MEGAHEALTH) {
                    this.inventory.GivePowerUp(this, powerup, time);
                }

                idDeclEntityDef def;

                switch (powerup) {
                    case BERSERK: {
                        if (this.spawnArgs.GetString("snd_berserk_third", "", sound)) {
                            StartSoundShader(declManager.FindSound(sound[0]), SND_CHANNEL_DEMONIC, 0, false, null);
                        }
                        if (this.baseSkinName.Length() != 0) {
                            this.powerUpSkin.oSet(declManager.FindSkin(this.baseSkinName + "_berserk"));
                        }
                        if (!gameLocal.isClient) {
                            this.idealWeapon = 0;
                        }
                        break;
                    }
                    case INVISIBILITY: {
                        this.spawnArgs.GetString("skin_invisibility", "", skin);
                        this.powerUpSkin.oSet(declManager.FindSkin(skin[0]));
                        // remove any decals from the model
                        if (this.modelDefHandle != -1) {
                            gameRenderWorld.RemoveDecals(this.modelDefHandle);
                        }
                        if (this.weapon.GetEntity() != null) {
                            this.weapon.GetEntity().UpdateSkin();
                        }
                        if (this.spawnArgs.GetString("snd_invisibility", "", sound)) {
                            StartSoundShader(declManager.FindSound(sound[0]), SND_CHANNEL_ANY, 0, false, null);
                        }
                        break;
                    }
                    case ADRENALINE: {
                        this.stamina = 100;
                        break;
                    }
                    case MEGAHEALTH: {
                        if (this.spawnArgs.GetString("snd_megahealth", "", sound)) {
                            StartSoundShader(declManager.FindSound(sound[0]), SND_CHANNEL_ANY, 0, false, null);
                        }
                        def = gameLocal.FindEntityDef("powerup_megahealth", false);
                        if (def != null) {
                            this.health = def.dict.GetInt("inv_health");
                        }
                        break;
                    }
                }

                if (this.hud != null) {
                    this.hud.HandleNamedEvent("itemPickup");
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
            this.inventory.ClearPowerUps();
        }

        public boolean PowerUpActive(int powerup) {
            return (this.inventory.powerups & (1 << powerup)) != 0;
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
                    if (this.healthPool <= 0) {
                        GiveHealthPool(100);
                    }
                } else {
                    this.healthPool = 0;
                }
            }

            return mod;
        }

        public int SlotForWeapon(final String weaponName) {
            int i;

            for (i = 0; i < MAX_WEAPONS; i++) {
                final String weap = this.spawnArgs.GetString(va("def_weapon%d", i));
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

            if (this.spectating || gameLocal.inCinematic || (this.influenceActive != 0)) {
                return;
            }

            if ((this.weapon.GetEntity() != null) && this.weapon.GetEntity().IsLinked()) {
                this.weapon.GetEntity().Reload();
            }
        }

        public void NextWeapon() {
            String weap;
            int w;

            if (!this.weaponEnabled || this.spectating || this.hiddenWeapon || gameLocal.inCinematic || gameLocal.world.spawnArgs.GetBool("no_Weapons") || (this.health < 0)) {
                return;
            }

            if (gameLocal.isClient) {
                return;
            }

            // check if we have any weapons
            if (0 == this.inventory.weapons) {
                return;
            }

            w = this.idealWeapon;
            while (true) {
                w++;
                if (w >= MAX_WEAPONS) {
                    w = 0;
                }
                weap = this.spawnArgs.GetString(va("def_weapon%d", w));
                if (!this.spawnArgs.GetBool(va("weapon%d_cycle", w))) {
                    continue;
                }
                if (weap.isEmpty()) {
                    continue;
                }
                if ((this.inventory.weapons & (1 << w)) == 0) {
                    continue;
                }
                if (this.inventory.HasAmmo(weap) != 0) {
                    break;
                }
            }

            if ((w != this.currentWeapon) && (w != this.idealWeapon)) {
                this.idealWeapon = w;
                this.weaponSwitchTime = gameLocal.time + WEAPON_SWITCH_DELAY;
                UpdateHudWeapon();
            }
        }

        public void NextBestWeapon() {
            String weap;
            int w = MAX_WEAPONS;

            if (gameLocal.isClient || !this.weaponEnabled) {
                return;
            }

            while (w > 0) {
                w--;
                weap = this.spawnArgs.GetString(va("def_weapon%d", w));
                if (weap.isEmpty() || ((this.inventory.weapons & (1 << w)) == 0) || (0 == this.inventory.HasAmmo(weap))) {
                    continue;
                }
                if (!this.spawnArgs.GetBool(va("weapon%d_best", w))) {
                    continue;
                }
                break;
            }
            this.idealWeapon = w;
            this.weaponSwitchTime = gameLocal.time + WEAPON_SWITCH_DELAY;
            UpdateHudWeapon();
        }

        public void PrevWeapon() {
            String weap;
            int w;

            if (!this.weaponEnabled || this.spectating || this.hiddenWeapon || gameLocal.inCinematic || gameLocal.world.spawnArgs.GetBool("no_Weapons") || (this.health < 0)) {
                return;
            }

            if (gameLocal.isClient) {
                return;
            }

            // check if we have any weapons
            if (0 == this.inventory.weapons) {
                return;
            }

            w = this.idealWeapon;
            while (true) {
                w--;
                if (w < 0) {
                    w = MAX_WEAPONS - 1;
                }
                weap = this.spawnArgs.GetString(va("def_weapon%d", w));
                if (!this.spawnArgs.GetBool(va("weapon%d_cycle", w))) {
                    continue;
                }
                if (weap.isEmpty()) {
                    continue;
                }
                if ((this.inventory.weapons & (1 << w)) == 0) {
                    continue;
                }
                if (this.inventory.HasAmmo(weap) != 0) {
                    break;
                }
            }

            if ((w != this.currentWeapon) && (w != this.idealWeapon)) {
                this.idealWeapon = w;
                this.weaponSwitchTime = gameLocal.time + WEAPON_SWITCH_DELAY;
                UpdateHudWeapon();
            }
        }

        public void SelectWeapon(int num, boolean force) {
            String weap;

            if (!this.weaponEnabled || this.spectating || gameLocal.inCinematic || (this.health < 0)) {
                return;
            }

            if ((num < 0) || (num >= MAX_WEAPONS)) {
                return;
            }

            if (gameLocal.isClient) {
                return;
            }

            if ((num != this.weapon_pda) && gameLocal.world.spawnArgs.GetBool("no_Weapons")) {
                num = this.weapon_fists;
                this.hiddenWeapon ^= true;//1;
                if (this.hiddenWeapon && (this.weapon.GetEntity() != null)) {
                    this.weapon.GetEntity().LowerWeapon();
                } else {
                    this.weapon.GetEntity().RaiseWeapon();
                }
            }

            weap = this.spawnArgs.GetString(va("def_weapon%d", num));
            if (weap.isEmpty()) {
                gameLocal.Printf("Invalid weapon\n");
                return;
            }

            if (force || ((this.inventory.weapons & (1 << num)) != 0)) {
                if ((0 == this.inventory.HasAmmo(weap)) && !this.spawnArgs.GetBool(va("weapon%d_allowempty", num))) {
                    return;
                }
                if ((this.previousWeapon >= 0) && (this.idealWeapon == num) && (this.spawnArgs.GetBool(va("weapon%d_toggle", num)))) {
                    weap = this.spawnArgs.GetString(va("def_weapon%d", this.previousWeapon));
                    if ((0 == this.inventory.HasAmmo(weap)) && !this.spawnArgs.GetBool(va("weapon%d_allowempty", this.previousWeapon))) {
                        return;
                    }
                    this.idealWeapon = this.previousWeapon;
                } else if ((this.weapon_pda >= 0) && (num == this.weapon_pda) && (this.inventory.pdas.Num() == 0)) {
                    ShowTip(this.spawnArgs.GetString("text_infoTitle"), this.spawnArgs.GetString("text_noPDA"), true);
                    return;
                } else {
                    this.idealWeapon = num;
                }
                UpdateHudWeapon();
            }
        }

        public void DropWeapon(boolean died) {
            final idVec3 forward = new idVec3(), up = new idVec3();
            int inclip, ammoavailable;

            assert (!gameLocal.isClient);

            if (this.spectating || this.weaponGone || (this.weapon.GetEntity() == null)) {
                return;
            }

            if ((!died && !this.weapon.GetEntity().IsReady()) || this.weapon.GetEntity().IsReloading()) {
                return;
            }
            // ammoavailable is how many shots we can fire
            // inclip is which amount is in clip right now
            ammoavailable = this.weapon.GetEntity().AmmoAvailable();
            inclip = this.weapon.GetEntity().AmmoInClip();

            // don't drop a grenade if we have none left
            if (NOT(idStr.Icmp(idWeapon.GetAmmoNameForNum(this.weapon.GetEntity().GetAmmoType()), "ammo_grenades")) && ((ammoavailable - inclip) <= 0)) {
                return;
            }

            // expect an ammo setup that makes sense before doing any dropping
            // ammoavailable is -1 for infinite ammo, and weapons like chainsaw
            // a bad ammo config usually indicates a bad weapon state, so we should not drop
            // used to be an assertion check, but it still happens in edge cases
            if ((ammoavailable != -1) && ((ammoavailable - inclip) < 0)) {
                common.DPrintf("idPlayer::DropWeapon: bad ammo setup\n");
                return;
            }
            idEntity item;
            if (died) {
                // ain't gonna throw you no weapon if I'm dead
                item = this.weapon.GetEntity().DropItem(getVec3_origin(), 0, WEAPON_DROP_TIME, died);
            } else {
                this.viewAngles.ToVectors(forward, null, up);
                item = this.weapon.GetEntity().DropItem(forward.oMultiply(250).oPlus(up.oMultiply(150)), 500, WEAPON_DROP_TIME, died);
            }
            if (null == item) {
                return;
            }
            // set the appropriate ammo in the dropped object
            final idKeyValue keyval = item.spawnArgs.MatchPrefix("inv_ammo_");
            if (keyval != null) {
                item.spawnArgs.SetInt(keyval.GetKey().getData(), ammoavailable);
                final idStr inclipKey = keyval.GetKey();
                inclipKey.Insert("inclip_", 4);
                item.spawnArgs.SetInt(inclipKey.getData(), inclip);
            }
            if (!died) {
                // remove from our local inventory completely
                {
                    final String[] inv_weapon = {item.spawnArgs.GetString("inv_weapon")};
                    this.inventory.Drop(this.spawnArgs, inv_weapon, -1);
                    item.spawnArgs.Set("inv_weapon", inv_weapon[0]);
                }
                this.weapon.GetEntity().ResetAmmoClip();
                NextWeapon();
                this.weapon.GetEntity().WeaponStolen();
                this.weaponGone = true;
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
            final idWeapon player_weapon = player.weapon.GetEntity();
            if (NOT(player_weapon) || !player_weapon.CanDrop() || this.weaponGone) {
                return;
            }
            // steal - we need to effectively force the other player to abandon his weapon
            final int newweap = player.currentWeapon;
            if (newweap == -1) {
                return;
            }
            // might be just dropped - check inventory
            if (0 == (player.inventory.weapons & (1 << newweap))) {
                return;
            }
            final String weapon_classname = this.spawnArgs.GetString(va("def_weapon%d", newweap));
            assert (weapon_classname != null);
            int ammoavailable = player.weapon.GetEntity().AmmoAvailable();
            int inclip = player.weapon.GetEntity().AmmoInClip();
            if ((ammoavailable != -1) && ((ammoavailable - inclip) < 0)) {
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
            player.SelectWeapon(this.weapon_fists, false);
            // in case the robbed player is firing rounds with a continuous fire weapon like the chaingun/plasma etc.
            // this will ensure the firing actually stops
            player.weaponGone = true;

            // give weapon, setup the ammo count
            Give("weapon", weapon_classname);
            final int ammo_i = player.inventory.AmmoIndexForWeaponClass(weapon_classname, null);
            this.idealWeapon = newweap;
            this.inventory.ammo[ ammo_i] += ammoavailable;
            this.inventory.clip[ newweap] = inclip;
        }

        public void AddProjectilesFired(int count) {
            this.numProjectilesFired += count;
        }

        public void AddProjectileHits(int count) {
            this.numProjectileHits += count;
        }

        public void SetLastHitTime(int time) {
            idPlayer aimed = null;

            if ((time != 0) && (this.lastHitTime != time)) {
                this.lastHitToggle ^= true;//1;
            }
            this.lastHitTime = time;
            if (0 == time) {
                // level start and inits
                return;
            }
            if (gameLocal.isMultiplayer && ((time - this.lastSndHitTime) > 10)) {
                this.lastSndHitTime = time;
                StartSound("snd_hit_feedback", SND_CHANNEL_ANY, SSF_PRIVATE_SOUND, false, null);
            }
            if (this.cursor != null) {
                this.cursor.HandleNamedEvent("hitTime");
            }
            if (this.hud != null) {
                if (this.MPAim != -1) {
                    if ((gameLocal.entities[this.MPAim] != null) && gameLocal.entities[this.MPAim].IsType(idPlayer.class)) {
                        aimed = (idPlayer) gameLocal.entities[this.MPAim];
                    }
                    assert (aimed != null);
                    // full highlight, no fade till loosing aim
                    this.hud.SetStateString("aim_text", gameLocal.userInfo[ this.MPAim].GetString("ui_name"));
                    if (aimed != null) {
                        this.hud.SetStateFloat("aim_color", aimed.colorBarIndex);
                    }
                    this.hud.HandleNamedEvent("aim_flash");
                    this.MPAimHighlight = true;
                    this.MPAimFadeTime = 0;
                } else if (this.lastMPAim != -1) {
                    if ((gameLocal.entities[this.lastMPAim] != null) && gameLocal.entities[this.lastMPAim].IsType(idPlayer.class)) {
                        aimed = (idPlayer) gameLocal.entities[this.lastMPAim];
                    }
                    assert (aimed != null);
                    // start fading right away
                    this.hud.SetStateString("aim_text", gameLocal.userInfo[ this.lastMPAim].GetString("ui_name"));
                    if (aimed != null) {
                        this.hud.SetStateFloat("aim_color", aimed.colorBarIndex);
                    }
                    this.hud.HandleNamedEvent("aim_flash");
                    this.hud.HandleNamedEvent("aim_fade");
                    this.MPAimHighlight = false;
                    this.MPAimFadeTime = gameLocal.realClientTime;
                }
            }
        }

        public void LowerWeapon() {
            if ((this.weapon.GetEntity() != null) && !this.weapon.GetEntity().IsHidden()) {
                this.weapon.GetEntity().LowerWeapon();
            }
        }

        public void RaiseWeapon() {
            if ((this.weapon.GetEntity() != null) && this.weapon.GetEntity().IsHidden()) {
                this.weapon.GetEntity().RaiseWeapon();
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
            if ((weap != null) && !weap.isEmpty()) {
                final String[] w = {this.spawnArgs.GetString(weap)};
                this.inventory.Drop(this.spawnArgs, w, -1);
                this.spawnArgs.Set(weap, w[0]);
            }
        }

        public boolean CanShowWeaponViewmodel() {
            return this.showWeaponViewModel;
        }

        public void AddAIKill() {
            int max_souls;
            int ammo_souls;

            if ((this.weapon_soulcube < 0) || ((this.inventory.weapons & (1 << this.weapon_soulcube)) == 0)) {
                return;
            }

            assert (this.hud != null);

            ammo_souls = idWeapon.GetAmmoNumForName("ammo_souls");
            max_souls = this.inventory.MaxAmmoForAmmoClass(this, "ammo_souls");
            if (this.inventory.ammo[ ammo_souls] < max_souls) {
                this.inventory.ammo[ ammo_souls]++;
                if (this.inventory.ammo[ ammo_souls] >= max_souls) {
                    this.hud.HandleNamedEvent("soulCubeReady");
                    StartSound("snd_soulcube_ready", SND_CHANNEL_ANY, 0, false, null);
                }
            }
        }

        public void SetSoulCubeProjectile(idProjectile projectile) {
            this.soulCubeProjectile.oSet(projectile);
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

            if (this.heartInfo.GetEndValue() == target) {
                return;
            }

            if (this.AI_DEAD.operator() && !force) {
                return;
            }

            this.lastHeartAdjust = gameLocal.time;

            this.heartInfo.Init((int) (gameLocal.time + (delay * 1000)), (int) (timeInSecs * 1000), 0f + this.heartRate, (float) target);
        }

        public void SetCurrentHeartRate() {

            final int base = idMath.FtoiFast((BASE_HEARTRATE + LOWHEALTH_HEARTRATE_ADJ) - (((float) this.health / 100) * LOWHEALTH_HEARTRATE_ADJ));

            if (PowerUpActive(ADRENALINE)) {
                this.heartRate = 135;
            } else {
                this.heartRate = idMath.FtoiFast(this.heartInfo.GetCurrentValue(gameLocal.time));
                final int currentRate = GetBaseHeartRate();
                if ((this.health >= 0) && (gameLocal.time > (this.lastHeartAdjust + 2500))) {
                    AdjustHeartRate(currentRate, 2.5f, 0, false);
                }
            }

            final int bps = idMath.FtoiFast((60f / this.heartRate) * 1000f);
            if ((gameLocal.time - this.lastHeartBeat) > bps) {
                final int dmgVol = DMG_VOLUME;
                final int deathVol = DEATH_VOLUME;
                final int zeroVol = ZERO_VOLUME;
                float pct = 0;
                if ((this.heartRate > BASE_HEARTRATE) && (this.health > 0)) {
                    pct = (float) (this.heartRate - base) / (MAX_HEARTRATE - base);
                    pct *= ((float) dmgVol - (float) zeroVol);
                } else if (this.health <= 0) {
                    pct = (float) (this.heartRate - DYING_HEARTRATE) / (BASE_HEARTRATE - DYING_HEARTRATE);
                    if (pct > 1.0f) {
                        pct = 1.0f;
                    } else if (pct < 0) {
                        pct = 0;
                    }
                    pct *= ((float) deathVol - (float) zeroVol);
                }

                pct += zeroVol;

                if (pct != zeroVol) {
                    StartSound("snd_heartbeat", SND_CHANNEL_HEART, SSF_PRIVATE_SOUND, false, null);
                    // modify just this channel to a custom volume
                    final soundShaderParms_t parms = new soundShaderParms_t();//memset( &parms, 0, sizeof( parms ) );
                    parms.volume = pct;
                    this.refSound.referenceSound.ModifySound(etoi(SND_CHANNEL_HEART), parms);
                }

                this.lastHeartBeat = gameLocal.time;
            }
        }

        public int GetBaseHeartRate() {
            final int base = idMath.FtoiFast((BASE_HEARTRATE + LOWHEALTH_HEARTRATE_ADJ) - (((float) this.health / 100) * LOWHEALTH_HEARTRATE_ADJ));
            int rate = idMath.FtoiFast(base + ((ZEROSTAMINA_HEARTRATE - base) * (1.0f - (this.stamina / pm_stamina.GetFloat()))));
            final int diff = (this.lastDmgTime != 0) ? gameLocal.time - this.lastDmgTime : 99999;
            rate += (diff < 5000) ? (diff < 2500) ? (diff < 1000) ? 15 : 10 : 5 : 0;
            return rate;
        }

        public void UpdateAir() {
            if (this.health <= 0) {
                return;
            }

            // see if the player is connected to the info_vacuum
            boolean newAirless = false;

            if (gameLocal.vacuumAreaNum != -1) {
                final int num = GetNumPVSAreas();
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
                if (!this.airless) {
                    StartSound("snd_decompress", SND_CHANNEL_ANY, SSF_GLOBAL, false, null);
                    StartSound("snd_noAir", SND_CHANNEL_BODY2, 0, false, null);
                    if (this.hud != null) {
                        this.hud.HandleNamedEvent("noAir");
                    }
                }
                this.airTics--;
                if (this.airTics < 0) {
                    this.airTics = 0;
                    // check for damage
                    final idDict damageDef = gameLocal.FindEntityDefDict("damage_noair", false);
                    final int dmgTiming = (int) (1000 * ((damageDef != null) ? damageDef.GetFloat("delay", "3.0") : 3));
                    if (gameLocal.time > (this.lastAirDamage + dmgTiming)) {
                        Damage(null, null, getVec3_origin(), "damage_noair", 1.0f, 0);
                        this.lastAirDamage = gameLocal.time;
                    }
                }

            } else {
                if (this.airless) {
                    StartSound("snd_recompress", SND_CHANNEL_ANY, SSF_GLOBAL, false, null);
                    StopSound(etoi(SND_CHANNEL_BODY2), false);
                    if (this.hud != null) {
                        this.hud.HandleNamedEvent("Air");
                    }
                }
                this.airTics += 2;	// regain twice as fast as lose
                if (this.airTics > pm_airTics.GetInteger()) {
                    this.airTics = pm_airTics.GetInteger();
                }
            }

            this.airless = newAirless;

            if (this.hud != null) {
                this.hud.SetStateInt("player_air", (100 * this.airTics) / pm_airTics.GetInteger());
            }
        }

        @Override
        public boolean HandleSingleGuiCommand(idEntity entityGui, idLexer src) {
            final idToken token = new idToken();

            if (!src.ReadToken(token)) {
                return false;
            }

            if (token.equals(";")) {
                return false;
            }

            if (token.Icmp("addhealth") == 0) {
                if ((entityGui != null) && (this.health < 100)) {
                    int _health = entityGui.spawnArgs.GetInt("gui_parm1");
                    final int amt = Math.min(_health, HEALTH_PER_DOSE);
                    _health -= amt;
                    entityGui.spawnArgs.SetInt("gui_parm1", _health);
                    if ((entityGui.GetRenderEntity() != null) && (entityGui.GetRenderEntity().gui[0] != null)) {
                        entityGui.GetRenderEntity().gui[ 0].SetStateInt("gui_parm1", _health);
                    }
                    this.health += amt;
                    if (this.health > 100) {
                        this.health = 100;
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
                if ((this.objectiveSystem != null) && this.objectiveSystemOpen && (this.pdaVideoWave.Length() > 0)) {
                    StopSound(etoi(SND_CHANNEL_PDA), false);
                }
                return true;
            }

            if (token.Icmp("close") == 0) {
                if ((this.objectiveSystem != null) && this.objectiveSystemOpen) {
                    TogglePDA();
                }
            }

            if (token.Icmp("playpdavideo") == 0) {
                if ((this.objectiveSystem != null) && this.objectiveSystemOpen && (this.pdaVideo.Length() > 0)) {
                    final idMaterial mat = declManager.FindMaterial(this.pdaVideo);
                    if (mat != null) {
                        final int c = mat.GetNumStages();
                        for (int i = 0; i < c; i++) {
                            final shaderStage_t stage = mat.GetStage(i);
                            if ((stage != null) && (stage.texture.cinematic[0] != null)) {
                                stage.texture.cinematic[0].ResetTime(gameLocal.time);
                            }
                        }
                        if (this.pdaVideoWave.Length() != 0) {
                            final idSoundShader shader = declManager.FindSound(this.pdaVideoWave);
                            StartSoundShader(shader, SND_CHANNEL_PDA, 0, false, null);
                        }
                    }
                }
            }

            if (token.Icmp("playpdaaudio") == 0) {
                if ((this.objectiveSystem != null) && this.objectiveSystemOpen && (this.pdaAudio.Length() > 0)) {
                    final idSoundShader shader = declManager.FindSound(this.pdaAudio);
                    final int[] ms = new int[1];
                    StartSoundShader(shader, SND_CHANNEL_PDA, 0, false, ms);
                    StartAudioLog();
                    CancelEvents(EV_Player_StopAudioLog);
                    PostEventMS(EV_Player_StopAudioLog, ms[0] + 150);
                }
                return true;
            }

            if (token.Icmp("stoppdaaudio") == 0) {
                if ((this.objectiveSystem != null) && this.objectiveSystemOpen && (this.pdaAudio.Length() > 0)) {
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
            return this.focusGUIent != null;
        }

        public void PerformImpulse(int impulse) {

            if (gameLocal.isClient) {
                final idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                assert (this.entityNumber == gameLocal.localClientNum);
                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.BeginWriting();
                msg.WriteBits(impulse, 6);
                ClientSendEvent(EVENT_IMPULSE, msg);
            }

            if ((impulse >= IMPULSE_0) && (impulse <= IMPULSE_12)) {
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
                    if (gameLocal.isClient || (this.entityNumber == gameLocal.localClientNum)) {
                        gameLocal.mpGame.ToggleReady();
                    }
                    break;
                }
                case IMPULSE_18: {
                    this.centerView.Init(gameLocal.time, 200, this.viewAngles.pitch, 0f);
                    break;
                }
                case IMPULSE_19: {
                    // when we're not in single player, IMPULSE_19 is used for showScores
                    // otherwise it opens the pda
                    if (!gameLocal.isMultiplayer) {
                        if (this.objectiveSystemOpen) {
                            TogglePDA();
                        } else if (this.weapon_pda >= 0) {
                            SelectWeapon(this.weapon_pda, true);
                        }
                    }
                    break;
                }
                case IMPULSE_20: {
                    if (gameLocal.isClient || (this.entityNumber == gameLocal.localClientNum)) {
                        gameLocal.mpGame.ToggleTeam();
                    }
                    break;
                }
                case IMPULSE_22: {
                    if (gameLocal.isClient || (this.entityNumber == gameLocal.localClientNum)) {
                        gameLocal.mpGame.ToggleSpectate();
                    }
                    break;
                }
                case IMPULSE_28: {
                    if (gameLocal.isClient || (this.entityNumber == gameLocal.localClientNum)) {
                        gameLocal.mpGame.CastVote(gameLocal.localClientNum, true);
                    }
                    break;
                }
                case IMPULSE_29: {
                    if (gameLocal.isClient || (this.entityNumber == gameLocal.localClientNum)) {
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
            final idBitMsg msg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

            // track invisible player bug
            // all hiding and showing should be performed through Spectate calls
            // except for the private camera view, which is used for teleports
            assert ((this.teleportEntity.GetEntity() != null) || (IsHidden() == this.spectating));

            if (this.spectating == spectate) {
                return;
            }

            this.spectating = spectate;

            if (gameLocal.isServer) {
                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.WriteBits(btoi(this.spectating), 1);
                ServerSendEvent(EVENT_SPECTATE, msg, false, -1);
            }

            if (this.spectating) {
                // join the spectators
                ClearPowerUps();
                this.spectator = this.entityNumber;
                Init();
                StopRagdoll();
                SetPhysics(this.physicsObj);
                this.physicsObj.DisableClip();
                Hide();
                Event_DisableWeapon();
                if (this.hud != null) {
                    this.hud.HandleNamedEvent("aim_clear");
                    this.MPAimFadeTime = 0;
                }
            } else {
                // put everything back together again
                this.currentWeapon = -1;	// to make sure the def will be loaded if necessary
                Show();
                Event_EnableWeapon();
            }
            SetClipModel();
        }

        public void TogglePDA() {
            if (this.objectiveSystem == null) {
                return;
            }

            if (this.inventory.pdas.Num() == 0) {
                ShowTip(this.spawnArgs.GetString("text_infoTitle"), this.spawnArgs.GetString("text_noPDA"), true);
                return;
            }

            assert (this.hud != null);

            if (!this.objectiveSystemOpen) {
                int j;
				final int c = this.inventory.items.Num();
                this.objectiveSystem.SetStateInt("inv_count", c);
                for (j = 0; j < MAX_INVENTORY_ITEMS; j++) {
                    this.objectiveSystem.SetStateString(va("inv_name_%d", j), "");
                    this.objectiveSystem.SetStateString(va("inv_icon_%d", j), "");
                    this.objectiveSystem.SetStateString(va("inv_text_%d", j), "");
                }
                for (j = 0; j < c; j++) {
                    final idDict item = this.inventory.items.oGet(j);
                    if (!item.GetBool("inv_pda")) {
                        final String iname = item.GetString("inv_name");
                        final String iicon = item.GetString("inv_icon");
                        final String itext = item.GetString("inv_text");
                        this.objectiveSystem.SetStateString(va("inv_name_%d", j), iname);
                        this.objectiveSystem.SetStateString(va("inv_icon_%d", j), iicon);
                        this.objectiveSystem.SetStateString(va("inv_text_%d", j), itext);
                        final idKeyValue kv = item.MatchPrefix("inv_id", null);
                        if (kv != null) {
                            this.objectiveSystem.SetStateString(va("inv_id_%d", j), kv.GetValue().getData());
                        }
                    }
                }

                for (j = 0; j < MAX_WEAPONS; j++) {
                    final String weapnum = va("def_weapon%d", j);
                    final String hudWeap = va("weapon%d", j);
                    int weapstate = 0;
                    if ((this.inventory.weapons & (1 << j)) != 0) {
                        final String weap = this.spawnArgs.GetString(weapnum);
                        if ((weap != null) && !weap.isEmpty()) {
                            weapstate++;
                        }
                    }
                    this.objectiveSystem.SetStateInt(hudWeap, weapstate);
                }

                this.objectiveSystem.SetStateInt("listPDA_sel_0", this.inventory.selPDA);
                this.objectiveSystem.SetStateInt("listPDAVideo_sel_0", this.inventory.selVideo);
                this.objectiveSystem.SetStateInt("listPDAAudio_sel_0", this.inventory.selAudio);
                this.objectiveSystem.SetStateInt("listPDAEmail_sel_0", this.inventory.selEMail);
                UpdatePDAInfo(false);
                UpdateObjectiveInfo();
                this.objectiveSystem.Activate(true, gameLocal.time);
                this.hud.HandleNamedEvent("pdaPickupHide");
                this.hud.HandleNamedEvent("videoPickupHide");
            } else {
                this.inventory.selPDA = this.objectiveSystem.State().GetInt("listPDA_sel_0");
                this.inventory.selVideo = this.objectiveSystem.State().GetInt("listPDAVideo_sel_0");
                this.inventory.selAudio = this.objectiveSystem.State().GetInt("listPDAAudio_sel_0");
                this.inventory.selEMail = this.objectiveSystem.State().GetInt("listPDAEmail_sel_0");
                this.objectiveSystem.Activate(false, gameLocal.time);
            }
            this.objectiveSystemOpen ^= true;//1;
        }

        public void ToggleScoreboard() {
            this.scoreBoardOpen ^= true;//1;
        }

        public void RouteGuiMouse(idUserInterface gui) {
            sysEvent_s ev;
            String command;

            if ((this.usercmd.mx != this.oldMouseX) || (this.usercmd.my != this.oldMouseY)) {
                ev = sys.GenerateMouseMoveEvent(this.usercmd.mx - this.oldMouseX, this.usercmd.my - this.oldMouseY);
                command = gui.HandleEvent(ev, gameLocal.time);
                this.oldMouseX = this.usercmd.mx;
                this.oldMouseY = this.usercmd.my;
            }
        }

        public void UpdateHud() {
            idPlayer aimed;

            if (null == this.hud) {
                return;
            }

            if (this.entityNumber != gameLocal.localClientNum) {
                return;
            }

            final int c = this.inventory.pickupItemNames.Num();
            if (c > 0) {
                if (gameLocal.time > this.inventory.nextItemPickup) {
                    if ((this.inventory.nextItemPickup != 0) && ((gameLocal.time - this.inventory.nextItemPickup) > 2000)) {
                        this.inventory.nextItemNum = 1;
                    }
                    int i;
                    for (i = 0; (i < 5) && (i < c); i++) {
                        this.hud.SetStateString(va("itemtext%d", this.inventory.nextItemNum), this.inventory.pickupItemNames.oGet(0).name.getData());
                        this.hud.SetStateString(va("itemicon%d", this.inventory.nextItemNum), this.inventory.pickupItemNames.oGet(0).icon.getData());
                        this.hud.HandleNamedEvent(va("itemPickup%d", this.inventory.nextItemNum++));
                        this.inventory.pickupItemNames.RemoveIndex(0);
                        if (this.inventory.nextItemNum == 1) {
                            this.inventory.onePickupTime = gameLocal.time;
                        } else if (this.inventory.nextItemNum > 5) {
                            this.inventory.nextItemNum = 1;
                            this.inventory.nextItemPickup = this.inventory.onePickupTime + 2000;
                        } else {
                            this.inventory.nextItemPickup = gameLocal.time + 400;
                        }
                    }
                }
            }

            if (gameLocal.realClientTime == this.lastMPAimTime) {
                if ((this.MPAim != -1) && (gameLocal.gameType == GAME_TDM)
                        && (gameLocal.entities[this.MPAim] != null) && gameLocal.entities[this.MPAim].IsType(idPlayer.class)
                        && (((idPlayer) gameLocal.entities[this.MPAim]).team == this.team)) {
                    aimed = (idPlayer) gameLocal.entities[this.MPAim];
                    this.hud.SetStateString("aim_text", gameLocal.userInfo[this.MPAim].GetString("ui_name"));
                    this.hud.SetStateFloat("aim_color", aimed.colorBarIndex);
                    this.hud.HandleNamedEvent("aim_flash");
                    this.MPAimHighlight = true;
                    this.MPAimFadeTime = 0;	// no fade till loosing focus
                } else if (this.MPAimHighlight) {
                    this.hud.HandleNamedEvent("aim_fade");
                    this.MPAimFadeTime = gameLocal.realClientTime;
                    this.MPAimHighlight = false;
                }
            }
            if (this.MPAimFadeTime != 0) {
                assert (!this.MPAimHighlight);
                if ((gameLocal.realClientTime - this.MPAimFadeTime) > 2000) {
                    this.MPAimFadeTime = 0;
                }
            }

            this.hud.SetStateInt("g_showProjectilePct", g_showProjectilePct.GetInteger());
            if (this.numProjectilesFired != 0) {
                this.hud.SetStateString("projectilepct", va("Hit %% %.1f", ((float) this.numProjectileHits / this.numProjectilesFired) * 100));
            } else {
                this.hud.SetStateString("projectilepct", "Hit % 0.0");
            }

            if (this.isLagged && gameLocal.isMultiplayer && (gameLocal.localClientNum == this.entityNumber)) {
                this.hud.SetStateString("hudLag", "1");
            } else {
                this.hud.SetStateString("hudLag", "0");
            }
        }

        public idDeclPDA GetPDA() {
            if (this.inventory.pdas.Num() != 0) {
                return (idDeclPDA) declManager.FindType(DECL_PDA, this.inventory.pdas.oGet(0));
            } else {
                return null;
            }
        }

        public idDeclVideo GetVideo(int index) {
            if ((index >= 0) && (index < this.inventory.videos.Num())) {
                return (idDeclVideo) declManager.FindType(DECL_VIDEO, this.inventory.videos.oGet(index), false);
            }
            return null;
        }

        public void SetInfluenceFov(float fov) {
            this.influenceFov = fov;
        }

        public void SetInfluenceView(final String mtr, final String skinname, float radius, idEntity ent) {
            this.influenceMaterial = null;
            this.influenceEntity = null;
            this.influenceSkin = null;
            if ((mtr != null) && !mtr.isEmpty()) {
                this.influenceMaterial = declManager.FindMaterial(mtr);
            }
            if ((skinname != null) && !skinname.isEmpty()) {
                this.influenceSkin = declManager.FindSkin(skinname);
                if (this.head.GetEntity() != null) {
                    this.head.GetEntity().GetRenderEntity().shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                }
                UpdateVisuals();
            }
            this.influenceRadius = radius;
            if (radius > 0) {
                this.influenceEntity = ent;
            }
        }

        public void SetInfluenceLevel(int level) {
            if (level != this.influenceActive) {
                if (level != 0) {
                    for (idEntity ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                        if (ent.IsType(idProjectile.class)) {
                            // remove all projectiles
                            ent.PostEventMS(EV_Remove, 0);
                        }
                    }
                    if (this.weaponEnabled && (this.weapon.GetEntity() != null)) {
                        this.weapon.GetEntity().EnterCinematic();
                    }
                } else {
                    this.physicsObj.SetLinearVelocity(getVec3_origin());
                    if (this.weaponEnabled && (this.weapon.GetEntity() != null)) {
                        this.weapon.GetEntity().ExitCinematic();
                    }
                }
                this.influenceActive = level;
            }
        }

        public int GetInfluenceLevel() {
            return this.influenceActive;
        }

        public void SetPrivateCameraView(idCamera camView) {
            this.privateCameraView = camView;
            if (camView != null) {
                StopFiring();
                Hide();
            } else {
                if (!this.spectating) {
                    Show();
                }
            }
        }

        public idCamera GetPrivateCameraView() {
            return this.privateCameraView;
        }

        public void StartFxFov(float duration) {
            this.fxFov = true;
            PostEventSec(EV_Player_StopFxFov, duration);
        }

        public void UpdateHudWeapon(boolean flashWeapon /*= true*/) {
            idUserInterface hud = this.hud;

            // if updating the hud of a followed client
            if ((gameLocal.localClientNum >= 0) && (gameLocal.entities[gameLocal.localClientNum] != null) && gameLocal.entities[gameLocal.localClientNum].IsType(idPlayer.class)) {
                final idPlayer p = (idPlayer) gameLocal.entities[gameLocal.localClientNum];
                if (p.spectating && (p.spectator == this.entityNumber)) {
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
                if ((this.inventory.weapons & (1 << i)) != 0) {
                    final String weap = this.spawnArgs.GetString(weapnum);
                    if ((weap != null) && !weap.isEmpty()) {
                        weapstate++;
                    }
                    if (this.idealWeapon == i) {
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
                staminapercentage = idMath.FtoiFast((100 * this.stamina) / max_stamina);
            }

            _hud.SetStateInt("player_health", this.health);
            _hud.SetStateInt("player_stamina", staminapercentage);
            _hud.SetStateInt("player_armor", this.inventory.armor);
            _hud.SetStateInt("player_hr", this.heartRate);
            _hud.SetStateInt("player_nostamina", (max_stamina == 0) ? 1 : 0);

            _hud.HandleNamedEvent("updateArmorHealthAir");

            if (this.healthPulse) {
                _hud.HandleNamedEvent("healthPulse");
                StartSound("snd_healthpulse", SND_CHANNEL_ITEM, 0, false, null);
                this.healthPulse = false;
            }

            if (this.healthTake) {
                _hud.HandleNamedEvent("healthPulse");
                StartSound("snd_healthtake", SND_CHANNEL_ITEM, 0, false, null);
                this.healthTake = false;
            }

            if (this.inventory.ammoPulse) {
                _hud.HandleNamedEvent("ammoPulse");
                this.inventory.ammoPulse = false;
            }
            if (this.inventory.weaponPulse) {
                // We need to update the weapon hud manually, but not
                // the armor/ammo/health because they are updated every
                // frame no matter what
                UpdateHudWeapon();
                _hud.HandleNamedEvent("weaponPulse");
                this.inventory.weaponPulse = false;
            }
            if (this.inventory.armorPulse) {
                _hud.HandleNamedEvent("armorPulse");
                this.inventory.armorPulse = false;
            }

            UpdateHudAmmo(_hud);
        }

        public void UpdateHudAmmo(idUserInterface _hud) {
            int inclip;
            int ammoamount;

            assert (this.weapon.GetEntity() != null);
            assert (_hud != null);

            inclip = this.weapon.GetEntity().AmmoInClip();
            ammoamount = this.weapon.GetEntity().AmmoAvailable();
            if ((ammoamount < 0) || !this.weapon.GetEntity().IsReady()) {
                // show infinite ammo
                _hud.SetStateString("player_ammo", "");
                _hud.SetStateString("player_totalammo", "");
            } else {
                // show remaining ammo
                _hud.SetStateString("player_totalammo", va("%d", ammoamount - inclip));
                _hud.SetStateString("player_ammo", (this.weapon.GetEntity().ClipSize() != 0) ? va("%d", inclip) : "--");		// how much in the current clip
                _hud.SetStateString("player_clips", (this.weapon.GetEntity().ClipSize() != 0) ? va("%d", ammoamount / this.weapon.GetEntity().ClipSize()) : "--");
                _hud.SetStateString("player_allammo", va("%d/%d", inclip, ammoamount - inclip));
            }

            _hud.SetStateBool("player_ammo_empty", (ammoamount == 0));
            _hud.SetStateBool("player_clip_empty", ((this.weapon.GetEntity().ClipSize() != 0) ? inclip == 0 : false));
            _hud.SetStateBool("player_clip_low", ((this.weapon.GetEntity().ClipSize() != 0) ? inclip <= this.weapon.GetEntity().LowAmmo() : false));

            _hud.HandleNamedEvent("updateAmmo");
        }

        public void Event_StopAudioLog() {
            StopAudioLog();
        }

        public void StartAudioLog() {
            if (this.hud != null) {
                this.hud.HandleNamedEvent("audioLogUp");
            }
        }

        public void StopAudioLog() {
            if (this.hud != null) {
                this.hud.HandleNamedEvent("audioLogDown");
            }
        }

        public void ShowTip(final String title, final String tip, boolean autoHide) {
            if (this.tipUp) {
                return;
            }
            this.hud.SetStateString("tip", tip);
            this.hud.SetStateString("tiptitle", title);
            this.hud.HandleNamedEvent("tipWindowUp");
            if (autoHide) {
                PostEventSec(EV_Player_HideTip, 5.0f);
            }
            this.tipUp = true;
        }

        public void HideTip() {
            this.hud.HandleNamedEvent("tipWindowDown");
            this.tipUp = false;
        }

        public boolean IsTipVisible() {
            return this.tipUp;
        }

        public void ShowObjective(final String obj) {
            this.hud.HandleNamedEvent(obj);
            this.objectiveUp = true;
        }

        public void HideObjective() {
            this.hud.HandleNamedEvent("closeObjective");
            this.objectiveUp = false;
        }

        @Override
        public void ClientPredictionThink() {
            renderEntity_s headRenderEnt;

            this.oldFlags = this.usercmd.flags;
            this.oldButtons = this.usercmd.buttons;

            this.usercmd = gameLocal.usercmds[this.entityNumber];

            if (this.entityNumber != gameLocal.localClientNum) {
                // ignore attack button of other clients. that's no good for predictions
                this.usercmd.buttons &= ~BUTTON_ATTACK;
            }

            this.buttonMask &= this.usercmd.buttons;
            this.usercmd.buttons &= ~this.buttonMask;

            if (this.objectiveSystemOpen) {
                this.usercmd.forwardmove = 0;
                this.usercmd.rightmove = 0;
                this.usercmd.upmove = 0;
            }

            // clear the ik before we do anything else so the skeleton doesn't get updated twice
            this.walkIK.ClearJointMods();

            if (gameLocal.isNewFrame) {
                if ((this.usercmd.flags & UCF_IMPULSE_SEQUENCE) != (this.oldFlags & UCF_IMPULSE_SEQUENCE)) {
                    PerformImpulse(this.usercmd.impulse);
                }
            }

            this.scoreBoardOpen = (((this.usercmd.buttons & BUTTON_SCORES) != 0) || this.forceScoreBoard);

            AdjustSpeed();

            UpdateViewAngles();

            // update the smoothed view angles
            if ((gameLocal.framenum >= this.smoothedFrame) && (this.entityNumber != gameLocal.localClientNum)) {
                final idAngles anglesDiff = this.viewAngles.oMinus(this.smoothedAngles);
                anglesDiff.Normalize180();
                if ((idMath.Fabs(anglesDiff.yaw) < 90) && (idMath.Fabs(anglesDiff.pitch) < 90)) {
                    // smoothen by pushing back to the previous angles
                    this.viewAngles.oMinSet(anglesDiff.oMultiply(gameLocal.clientSmoothing));
                    this.viewAngles.Normalize180();
                }
                this.smoothedAngles = this.viewAngles;
            }
            this.smoothedOriginUpdated = false;

            if (!this.af.IsActive()) {
                AdjustBodyAngles();
            }

            if (!this.isLagged) {
                // don't allow client to move when lagged
                Move();
            }

            // update GUIs, Items, and character interactions
            UpdateFocus();

            // service animations
            if (!this.spectating && !this.af.IsActive()) {
                UpdateConditions();
                UpdateAnimState();
                CheckBlink();
            }

            // clear out our pain flag so we can tell if we recieve any damage between now and the next time we think
            this.AI_PAIN.operator(false);

            // calculate the exact bobbed view position, which is used to
            // position the view weapon, among other things
            CalculateFirstPersonView();

            // this may use firstPersonView, or a thirdPerson / camera view
            CalculateRenderView();

            if (!gameLocal.inCinematic && (this.weapon.GetEntity() != null) && (this.health > 0) && !(gameLocal.isMultiplayer && this.spectating)) {
                UpdateWeapon();
            }

            UpdateHud();

            if (gameLocal.isNewFrame) {
                UpdatePowerUps();
            }

            UpdateDeathSkin(false);

            if (this.head.GetEntity() != null) {
                headRenderEnt = this.head.GetEntity().GetRenderEntity();
            } else {
                headRenderEnt = null;
            }

            if (headRenderEnt != null) {
                if (this.influenceSkin != null) {
                    headRenderEnt.customSkin = this.influenceSkin;
                } else {
                    headRenderEnt.customSkin = null;
                }
            }

            if (gameLocal.isMultiplayer || g_showPlayerShadow.GetBool()) {
                this.renderEntity.suppressShadowInViewID = 0;
                if (headRenderEnt != null) {
                    headRenderEnt.suppressShadowInViewID = 0;
                }
            } else {
                this.renderEntity.suppressShadowInViewID = this.entityNumber + 1;
                if (headRenderEnt != null) {
                    headRenderEnt.suppressShadowInViewID = this.entityNumber + 1;
                }
            }
            // never cast shadows from our first-person muzzle flashes
            this.renderEntity.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + this.entityNumber;
            if (headRenderEnt != null) {
                headRenderEnt.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + this.entityNumber;
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

            if (gameLocal.isNewFrame && (this.entityNumber == gameLocal.localClientNum)) {
                this.playerView.CalculateShake();
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            this.physicsObj.WriteToSnapshot(msg);
            WriteBindToSnapshot(msg);
            msg.WriteDeltaFloat(0, this.deltaViewAngles.oGet(0));
            msg.WriteDeltaFloat(0, this.deltaViewAngles.oGet(1));
            msg.WriteDeltaFloat(0, this.deltaViewAngles.oGet(2));
            msg.WriteShort(this.health);
            msg.WriteBits(gameLocal.ServerRemapDecl(-1, DECL_ENTITYDEF, this.lastDamageDef), gameLocal.entityDefBits);
            msg.WriteDir(this.lastDamageDir, 9);
            msg.WriteShort(this.lastDamageLocation);
            msg.WriteBits(this.idealWeapon, idMath.BitsForInteger(MAX_WEAPONS));
            msg.WriteBits(this.inventory.weapons, MAX_WEAPONS);
            msg.WriteBits(this.weapon.GetSpawnId(), 32);
            msg.WriteBits(this.spectator, idMath.BitsForInteger(MAX_CLIENTS));
            msg.WriteBits(btoi(this.lastHitToggle), 1);
            msg.WriteBits(btoi(this.weaponGone), 1);
            msg.WriteBits(btoi(this.isLagged), 1);
            msg.WriteBits(btoi(this.isChatting), 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            int i, oldHealth, newIdealWeapon, weaponSpawnId;
            boolean newHitToggle, stateHitch;

            stateHitch = (this.snapshotSequence - this.lastSnapshotSequence) > 1;
            this.lastSnapshotSequence = this.snapshotSequence;

            oldHealth = this.health;

            this.physicsObj.ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);
            this.deltaViewAngles.oSet(0, msg.ReadDeltaFloat(0));
            this.deltaViewAngles.oSet(1, msg.ReadDeltaFloat(0));
            this.deltaViewAngles.oSet(2, msg.ReadDeltaFloat(0));
            this.health = msg.ReadShort();
            this.lastDamageDef = gameLocal.ClientRemapDecl(DECL_ENTITYDEF, msg.ReadBits(gameLocal.entityDefBits));
            this.lastDamageDir = msg.ReadDir(9);
            this.lastDamageLocation = msg.ReadShort();
            newIdealWeapon = msg.ReadBits(idMath.BitsForInteger(MAX_WEAPONS));
            this.inventory.weapons = msg.ReadBits(MAX_WEAPONS);
            weaponSpawnId = msg.ReadBits(32);
            this.spectator = msg.ReadBits(idMath.BitsForInteger(MAX_CLIENTS));
            newHitToggle = msg.ReadBits(1) != 0;
            this.weaponGone = msg.ReadBits(1) != 0;
            this.isLagged = msg.ReadBits(1) != 0;
            this.isChatting = msg.ReadBits(1) != 0;

            // no msg reading below this
            if (this.weapon.SetSpawnId(weaponSpawnId)) {
                if (this.weapon.GetEntity() != null) {
                    // maintain ownership locally
                    this.weapon.GetEntity().SetOwner(this);
                }
                this.currentWeapon = -1;
            }
            // if not a local client assume the client has all ammo types
            if (this.entityNumber != gameLocal.localClientNum) {
                for (i = 0; i < AMMO_NUMTYPES; i++) {
                    this.inventory.ammo[ i] = 999;
                }
            }

            if ((oldHealth > 0) && (this.health <= 0)) {
                if (stateHitch) {
                    // so we just hide and don't show a death skin
                    UpdateDeathSkin(true);
                }
                // die
                this.AI_DEAD.operator(true);
                ClearPowerUps();
                SetAnimState(ANIMCHANNEL_LEGS, "Legs_Death", 4);
                SetAnimState(ANIMCHANNEL_TORSO, "Torso_Death", 4);
                SetWaitState("");
                this.animator.ClearAllJoints();
                if (this.entityNumber == gameLocal.localClientNum) {
                    this.playerView.Fade(colorBlack, 12000);
                }
                StartRagdoll();
                this.physicsObj.SetMovementType(PM_DEAD);
                if (!stateHitch) {
                    StartSound("snd_death", SND_CHANNEL_VOICE, 0, false, null);
                }
                if (this.weapon.GetEntity() != null) {
                    this.weapon.GetEntity().OwnerDied();
                }
            } else if ((oldHealth <= 0) && (this.health > 0)) {
                // respawn
                Init();
                StopRagdoll();
                SetPhysics(this.physicsObj);
                this.physicsObj.EnableClip();
                SetCombatContents(true);
            } else if ((this.health < oldHealth) && (this.health > 0)) {
                if (stateHitch) {
                    this.lastDmgTime = gameLocal.time;
                } else {
                    // damage feedback
                    final idDeclEntityDef def = (idDeclEntityDef) declManager.DeclByIndex(DECL_ENTITYDEF, this.lastDamageDef, false);
                    if (def != null) {
                        this.playerView.DamageImpulse(this.lastDamageDir.oMultiply(this.viewAxis.Transpose()), def.dict);
                        this.AI_PAIN.operator(Pain(null, null, oldHealth - this.health, this.lastDamageDir, this.lastDamageLocation));
                        this.lastDmgTime = gameLocal.time;
                    } else {
                        common.Warning("NET: no damage def for damage feedback '%d'\n", this.lastDamageDef);
                    }
                }
            } else if ((this.health > oldHealth) && PowerUpActive(MEGAHEALTH) && !stateHitch) {
                // just pulse, for any health raise
                this.healthPulse = true;
            }

            // If the player is alive, restore proper physics object
            if ((this.health > 0) && IsActiveAF()) {
                StopRagdoll();
                SetPhysics(this.physicsObj);
                this.physicsObj.EnableClip();
                SetCombatContents(true);
            }

            if (this.idealWeapon != newIdealWeapon) {
                if (stateHitch) {
                    this.weaponCatchup = true;
                }
                this.idealWeapon = newIdealWeapon;
                UpdateHudWeapon();
            }

            if (this.lastHitToggle != newHitToggle) {
                SetLastHitTime(gameLocal.realClientTime);
            }

            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        public void WritePlayerStateToSnapshot(idBitMsgDelta msg) {
            int i;

            msg.WriteByte(this.bobCycle);
            msg.WriteLong(this.stepUpTime);
            msg.WriteFloat(this.stepUpDelta);
            msg.WriteShort(this.inventory.weapons);
            msg.WriteByte(this.inventory.armor);

            for (i = 0; i < AMMO_NUMTYPES; i++) {
                msg.WriteBits(this.inventory.ammo[i], ASYNC_PLAYER_INV_AMMO_BITS);
            }
            for (i = 0; i < MAX_WEAPONS; i++) {
                msg.WriteBits(this.inventory.clip[i], ASYNC_PLAYER_INV_CLIP_BITS);
            }
        }

        public void ReadPlayerStateFromSnapshot(final idBitMsgDelta msg) {
            int i, ammo;

            this.bobCycle = msg.ReadByte();
            this.stepUpTime = msg.ReadLong();
            this.stepUpDelta = msg.ReadFloat();
            this.inventory.weapons = msg.ReadShort();
            this.inventory.armor = msg.ReadByte();

            for (i = 0; i < AMMO_NUMTYPES; i++) {
                ammo = msg.ReadBits(ASYNC_PLAYER_INV_AMMO_BITS);
                if (gameLocal.time >= this.inventory.ammoPredictTime) {
                    this.inventory.ammo[ i] = ammo;
                }
            }
            for (i = 0; i < MAX_WEAPONS; i++) {
                this.inventory.clip[i] = msg.ReadBits(ASYNC_PLAYER_INV_CLIP_BITS);
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
            if (this.af.IsActive()) {
                this.af.GetPhysicsToVisualTransform(origin, axis);
                return true;
            }

            // smoothen the rendered origin and angles of other clients
            // smooth self origin if snapshots are telling us prediction is off
            if (gameLocal.isClient && (gameLocal.framenum >= this.smoothedFrame) && ((this.entityNumber != gameLocal.localClientNum) || this.selfSmooth)) {
                // render origin and axis
                final idMat3 renderAxis = this.viewAxis.oMultiply(GetPhysics().GetAxis());
                final idVec3 renderOrigin = GetPhysics().GetOrigin().oPlus(this.modelOffset.oMultiply(renderAxis));

                // update the smoothed origin
                if (!this.smoothedOriginUpdated) {
                    final idVec2 originDiff = renderOrigin.ToVec2().oMinus(this.smoothedOrigin.ToVec2());
                    if (originDiff.LengthSqr() < Square(100)) {
                        // smoothen by pushing back to the previous position
                        if (this.selfSmooth) {
                            assert (this.entityNumber == gameLocal.localClientNum);
                            renderOrigin.ToVec2_oMinSet(originDiff.oMultiply(net_clientSelfSmoothing.GetFloat()));
                        } else {
                            renderOrigin.ToVec2_oMinSet(originDiff.oMultiply(gameLocal.clientSmoothing));
                        }
                    }
                    this.smoothedOrigin = renderOrigin;

                    this.smoothedFrame = gameLocal.framenum;
                    this.smoothedOriginUpdated = true;
                }

                axis.oSet(new idAngles(0, this.smoothedAngles.yaw, 0).ToMat3());
                origin.oSet(axis.Transpose().oMultiply(this.smoothedOrigin.oMinus(GetPhysics().GetOrigin())));

            } else {

                axis.oSet(this.viewAxis);
                origin.oSet(this.modelOffset);
            }
            return true;
        }

        @Override
        public boolean GetPhysicsToSoundTransform(idVec3 origin, idMat3 axis) {
            idCamera camera;

            if (this.privateCameraView != null) {
                camera = this.privateCameraView;
            } else {
                camera = gameLocal.GetCamera();
            }

            if (camera != null) {
                final renderView_s view = new renderView_s();

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
                    final boolean spectate = (msg.ReadBits(1) != 0);
                    Spectate(spectate);
                    return true;
                }
                case EVENT_ADD_DAMAGE_EFFECT: {
                    if (this.spectating) {
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
            return this.ready || this.forcedReady;
        }

        public boolean IsRespawning() {
            return this.respawning;
        }

        public boolean IsInTeleport() {
            return (this.teleportEntity.GetEntity() != null);
        }

        public idEntity GetInfluenceEntity() {
            return this.influenceEntity;
        }

        public idMaterial GetInfluenceMaterial() {
            return this.influenceMaterial;
        }

        public float GetInfluenceRadius() {
            return this.influenceRadius;
        }

        // server side work for in/out of spectate. takes care of spawning it into the world as well
        public void ServerSpectate(boolean spectate) {
            assert (!gameLocal.isClient);

            if (this.spectating != spectate) {
                Spectate(spectate);
                if (spectate) {
                    SetSpectateOrigin();
                } else {
                    if (gameLocal.gameType == GAME_DM) {
                        // make sure the scores are reset so you can't exploit by spectating and entering the game back
                        // other game types don't matter, as you either can't join back, or it's team scores
                        gameLocal.mpGame.ClearFrags(this.entityNumber);
                    }
                }
            }
            if (!spectate) {
                SpawnFromSpawnSpot();
            }
        }

        // for very specific usage. != GetPhysics()
        public idPhysics GetPlayerPhysics() {
            return this.physicsObj;
        }

        public void TeleportDeath(int killer) {
            this.teleportKiller = killer;
        }

        public void SetLeader(boolean lead) {
            this.leader = lead;
        }

        public boolean IsLeader() {
            return this.leader;
        }

        public void UpdateSkinSetup(boolean restart) {
            if (restart) {
                this.team = (idStr.Icmp(GetUserInfo().GetString("ui_team"), "Blue") == 0) ? 1 : 0;
            }
            if (gameLocal.gameType == GAME_TDM) {
                if (this.team != 0) {
                    this.baseSkinName.oSet("skins/characters/player/marine_mp_blue");
                } else {
                    this.baseSkinName.oSet("skins/characters/player/marine_mp_red");
                }
                if (!gameLocal.isClient && (this.team != this.latchedTeam)) {
                    gameLocal.mpGame.SwitchToTeam(this.entityNumber, this.latchedTeam, this.team);
                }
                this.latchedTeam = this.team;
            } else {
                this.baseSkinName.oSet(GetUserInfo().GetString("ui_skin"));
            }
            if (0 == this.baseSkinName.Length()) {
                this.baseSkinName.oSet("skins/characters/player/marine_mp");
            }
            this.skin.oSet(declManager.FindSkin(this.baseSkinName, false));
            assert (this.skin != null);
            // match the skin to a color band for scoreboard
            if (this.baseSkinName.Find("red") != -1) {
                this.colorBarIndex = 1;
            } else if (this.baseSkinName.Find("green") != -1) {
                this.colorBarIndex = 2;
            } else if (this.baseSkinName.Find("blue") != -1) {
                this.colorBarIndex = 3;
            } else if (this.baseSkinName.Find("yellow") != -1) {
                this.colorBarIndex = 4;
            } else {
                this.colorBarIndex = 0;
            }
            this.colorBar = colorBarTable[ this.colorBarIndex];
            if (PowerUpActive(BERSERK)) {
                this.powerUpSkin.oSet(declManager.FindSkin(this.baseSkinName + "_berserk"));
            }
        }

        @Override
        public boolean OnLadder() {
            return this.physicsObj.OnLadder();
        }

        public void UpdatePlayerIcons() {
            final int time = networkSystem.ServerGetClientTimeSinceLastPacket(this.entityNumber);
            if (time > cvarSystem.GetCVarInteger("net_clientMaxPrediction")) {
                this.isLagged = true;
            } else {
                this.isLagged = false;
            }
        }

        public void DrawPlayerIcons() {
            if (!NeedsIcon()) {
                this.playerIcon.FreeIcon();
                return;
            }
            this.playerIcon.Draw(this, this.headJoint);
        }

        public void HidePlayerIcons() {
            this.playerIcon.FreeIcon();
        }

        public boolean NeedsIcon() {
            // local clients don't render their own icons... they're only info for other clients
            return (this.entityNumber != gameLocal.localClientNum) && (this.isLagged || this.isChatting);
        }

        public boolean SelfSmooth() {
            return this.selfSmooth;
        }

        public void SetSelfSmooth(boolean b) {
            this.selfSmooth = b;
        }

        private void LookAtKiller(idEntity inflictor, idEntity attacker) {
            idVec3 dir;

            if (!this.equals(attacker)) {
                dir = attacker.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
            } else if (!this.equals(inflictor)) {
                dir = inflictor.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin());
            } else {
                dir = this.viewAxis.oGet(0);
            }

            final idAngles ang = new idAngles(0, dir.ToYaw(), 0);
            SetViewAngles(ang);
        }

        private void StopFiring() {
            this.AI_ATTACK_HELD.operator(false);
            this.AI_WEAPON_FIRED.operator(false);
            this.AI_RELOAD.operator(false);
            if (this.weapon.GetEntity() != null) {
                this.weapon.GetEntity().EndAttack();
            }
        }

        private void FireWeapon() {
            final idMat3 axis = new idMat3();
            final idVec3 muzzle = new idVec3();

            if (this.privateCameraView != null) {
                return;
            }

            if (g_editEntityMode.GetInteger() != 0) {
                GetViewPos(muzzle, axis);
                if (gameLocal.editEntities.SelectEntity(muzzle, axis.oGet(0), this)) {
                    return;
                }
            }

            if (!this.hiddenWeapon && this.weapon.GetEntity().IsReady()) {
                if ((this.weapon.GetEntity().AmmoInClip() != 0) || (this.weapon.GetEntity().AmmoAvailable() != 0)) {
                    this.AI_ATTACK_HELD.operator(true);
                    this.weapon.GetEntity().BeginAttack();
                    if ((this.weapon_soulcube >= 0) && (this.currentWeapon == this.weapon_soulcube)) {
                        if (this.hud != null) {
                            this.hud.HandleNamedEvent("soulCubeNotReady");
                        }
                        SelectWeapon(this.previousWeapon, false);
                    }
                } else {
                    NextBestWeapon();
                }
            }

            if (this.hud != null) {
                if (this.tipUp) {
                    HideTip();
                }
                // may want to track with with a bool as well
                // keep from looking up named events so often
                if (this.objectiveUp) {
                    HideObjective();
                }
            }
        }

        private void Weapon_Combat() {
            if ((this.influenceActive != 0) || !this.weaponEnabled || gameLocal.inCinematic || (this.privateCameraView != null)) {
                return;
            }

            this.weapon.GetEntity().RaiseWeapon();
            if (this.weapon.GetEntity().IsReloading()) {
                if (!this.AI_RELOAD.operator()) {
                    this.AI_RELOAD.operator(true);
                    SetState("ReloadWeapon");
                    UpdateScript();
                }
            } else {
                this.AI_RELOAD.operator(false);
            }

            if ((this.idealWeapon == this.weapon_soulcube) && (this.soulCubeProjectile.GetEntity() != null)) {
                this.idealWeapon = this.currentWeapon;
            }

            if (this.idealWeapon != this.currentWeapon) {
                if (this.weaponCatchup) {
                    assert (gameLocal.isClient);

                    this.currentWeapon = this.idealWeapon;
                    this.weaponGone = false;
                    this.animPrefix.oSet(this.spawnArgs.GetString(va("def_weapon%d", this.currentWeapon)));
                    this.weapon.GetEntity().GetWeaponDef(this.animPrefix.getData(), this.inventory.clip[ this.currentWeapon]);
                    this.animPrefix.Strip("weapon_");

                    this.weapon.GetEntity().NetCatchup();
                    final function_t newstate = GetScriptFunction("NetCatchup");
                    if (newstate != null) {
                        SetState(newstate);
                        UpdateScript();
                    }
                    this.weaponCatchup = false;
                } else {
                    if (this.weapon.GetEntity().IsReady()) {
                        this.weapon.GetEntity().PutAway();
                    }

                    if (this.weapon.GetEntity().IsHolstered()) {
                        assert (this.idealWeapon >= 0);
                        assert (this.idealWeapon < MAX_WEAPONS);

                        if ((this.currentWeapon != this.weapon_pda) && !this.spawnArgs.GetBool(va("weapon%d_toggle", this.currentWeapon))) {
                            this.previousWeapon = this.currentWeapon;
                        }
                        this.currentWeapon = this.idealWeapon;
                        this.weaponGone = false;
                        this.animPrefix.oSet(this.spawnArgs.GetString(va("def_weapon%d", this.currentWeapon)));
                        this.weapon.GetEntity().GetWeaponDef(this.animPrefix.getData(), this.inventory.clip[ this.currentWeapon]);
                        this.animPrefix.Strip("weapon_");

                        this.weapon.GetEntity().Raise();
                    }
                }
            } else {
                this.weaponGone = false;	// if you drop and re-get weap, you may miss the = false above 
                if (this.weapon.GetEntity().IsHolstered()) {
                    if (NOT(this.weapon.GetEntity().AmmoAvailable())) {
                        // weapons can switch automatically if they have no more ammo
                        NextBestWeapon();
                    } else {
                        this.weapon.GetEntity().Raise();
                        this.state = GetScriptFunction("RaiseWeapon");
                        if (this.state != null) {
                            SetState(this.state);
                        }
                    }
                }
            }

            // check for attack
            this.AI_WEAPON_FIRED.operator(false);
            if (0 == this.influenceActive) {
                if (((this.usercmd.buttons & BUTTON_ATTACK) != 0) && !this.weaponGone) {
                    FireWeapon();
                } else if ((this.oldButtons & BUTTON_ATTACK) != 0) {
                    this.AI_ATTACK_HELD.operator(false);
                    this.weapon.GetEntity().EndAttack();
                }
            }

            // update our ammo clip in our inventory
            if ((this.currentWeapon >= 0) && (this.currentWeapon < MAX_WEAPONS)) {
                this.inventory.clip[ this.currentWeapon] = this.weapon.GetEntity().AmmoInClip();
                if ((this.hud != null) && (this.currentWeapon == this.idealWeapon)) {
                    UpdateHudAmmo(this.hud);
                }
            }
        }

        private void Weapon_NPC() {
            if (this.idealWeapon != this.currentWeapon) {
                Weapon_Combat();
            }
            StopFiring();
            this.weapon.GetEntity().LowerWeapon();

            if (((this.usercmd.buttons & BUTTON_ATTACK) != 0) && (0 == (this.oldButtons & BUTTON_ATTACK))) {
                this.buttonMask |= BUTTON_ATTACK;
                this.focusCharacter.TalkTo(this);
            }
        }

        private void Weapon_GUI() {

            if (!this.objectiveSystemOpen) {
                if (this.idealWeapon != this.currentWeapon) {
                    Weapon_Combat();
                }
                StopFiring();
                this.weapon.GetEntity().LowerWeapon();
            }

            // disable click prediction for the GUIs. handy to check the state sync does the right thing
            if (gameLocal.isClient && !net_clientPredictGUI.GetBool()) {
                return;
            }

            if (((this.oldButtons ^ this.usercmd.buttons) & BUTTON_ATTACK) != 0) {
                sysEvent_s ev;
                String command = null;
                final boolean[] updateVisuals = {false};

                final idUserInterface ui = ActiveGui();
                if (ui != null) {
                    ev = sys.GenerateMouseButtonEvent(1, (this.usercmd.buttons & BUTTON_ATTACK) != 0);
                    command = ui.HandleEvent(ev, gameLocal.time, updateVisuals);
                    if (updateVisuals[0] && (this.focusGUIent != null) && ui.equals(this.focusUI)) {
                        this.focusGUIent.UpdateVisuals();
                    }
                }
                if (gameLocal.isClient) {
                    // we predict enough, but don't want to execute commands
                    return;
                }
                if (this.focusGUIent != null) {
                    HandleGuiCommands(this.focusGUIent, command);
                } else {
                    HandleGuiCommands(this, command);
                }
            }
        }

        private void UpdateWeapon() {
            if (this.health <= 0) {
                return;
            }

            assert (!this.spectating);

            if (gameLocal.isClient) {
                // clients need to wait till the weapon and it's world model entity
                // are present and synchronized ( weapon.worldModel idEntityPtr to idAnimatedEntity )
                if (!this.weapon.GetEntity().IsWorldModelReady()) {
                    return;
                }
            }

            // always make sure the weapon is correctly setup before accessing it
            if (!this.weapon.GetEntity().IsLinked()) {
                if (this.idealWeapon != -1) {
                    this.animPrefix.oSet(this.spawnArgs.GetString(va("def_weapon%d", this.idealWeapon)));
                    this.weapon.GetEntity().GetWeaponDef(this.animPrefix.getData(), this.inventory.clip[ this.idealWeapon]);
                    assert (this.weapon.GetEntity().IsLinked());
                } else {
                    return;
                }
            }

            if (this.hiddenWeapon && this.tipUp && ((this.usercmd.buttons & BUTTON_ATTACK) != 0)) {
                HideTip();
            }

            if (g_dragEntity.GetBool()) {
                StopFiring();
                this.weapon.GetEntity().LowerWeapon();
                this.dragEntity.Update(this);
            } else if (ActiveGui() != null) {
                // gui handling overrides weapon use
                Weapon_GUI();
            } else if ((this.focusCharacter != null) && (this.focusCharacter.health > 0)) {
                Weapon_NPC();
            } else {
                Weapon_Combat();
            }

            if (this.hiddenWeapon) {
                this.weapon.GetEntity().LowerWeapon();
            }

            // update weapon state, particles, dlights, etc
            this.weapon.GetEntity().PresentWeapon(this.showWeaponViewModel);
        }

        private void UpdateSpectating() {
            assert (this.spectating);
            assert (!gameLocal.isClient);
            assert (IsHidden());
            idPlayer player;
            if (!gameLocal.isMultiplayer) {
                return;
            }
            player = gameLocal.GetClientByNum(this.spectator);
            if ((null == player) || (player.spectating && (player != this))) {//TODO:equals instead of != or ==
                SpectateFreeFly(true);
            } else if (this.usercmd.upmove > 0) {
                SpectateFreeFly(false);
            } else if ((this.usercmd.buttons & BUTTON_ATTACK) != 0) {
                SpectateCycle();
            }
        }

        private void SpectateFreeFly(boolean force) {	// ignore the timeout to force when followed spec is no longer valid
            idPlayer player;
            idVec3 newOrig;
            final idVec3 spawn_origin = new idVec3();
            final idAngles spawn_angles = new idAngles();

            player = gameLocal.GetClientByNum(this.spectator);
            if (force || (gameLocal.time > this.lastSpectateChange)) {
                this.spectator = this.entityNumber;
                if ((player != null) && (player != this) && !player.spectating && !player.IsInTeleport()) {
                    newOrig = player.GetPhysics().GetOrigin();
                    if (player.physicsObj.IsCrouching()) {
                        newOrig.oPluSet(2, pm_crouchviewheight.GetFloat());
                    } else {
                        newOrig.oPluSet(2, pm_normalviewheight.GetFloat());
                    }
                    newOrig.oPluSet(2, SPECTATE_RAISE);
                    final idBounds b = new idBounds(getVec3_origin()).Expand(pm_spectatebbox.GetFloat() * 0.5f);
                    final idVec3 start = player.GetPhysics().GetOrigin();
                    start.oPluSet(2, pm_spectatebbox.GetFloat() * 0.5f);
                    final trace_s[] t = {null};
                    // assuming spectate bbox is inside stand or crouch box
                    gameLocal.clip.TraceBounds(t, start, newOrig, b, MASK_PLAYERSOLID, player);
                    newOrig.Lerp(start, newOrig, t[0].fraction);
                    SetOrigin(newOrig);
                    final idAngles angle = player.viewAngles;
                    angle.oSet(2, 0);
                    SetViewAngles(angle);
                } else {
                    SelectInitialSpawnPoint(spawn_origin, spawn_angles);
                    spawn_origin.oPluSet(2, pm_normalviewheight.GetFloat());
                    spawn_origin.oPluSet(2, SPECTATE_RAISE);
                    SetOrigin(spawn_origin);
                    SetViewAngles(spawn_angles);
                }
                this.lastSpectateChange = gameLocal.time + 500;
            }
        }

        private void SpectateCycle() {
            idPlayer player;

            if (gameLocal.time > this.lastSpectateChange) {
                final int latchedSpectator = this.spectator;
                this.spectator = gameLocal.GetNextClientNum(this.spectator);
                player = gameLocal.GetClientByNum(this.spectator);
                assert (player != null); // never call here when the current spectator is wrong
                // ignore other spectators
                while ((latchedSpectator != this.spectator) && player.spectating) {
                    this.spectator = gameLocal.GetNextClientNum(this.spectator);
                    player = gameLocal.GetClientByNum(this.spectator);
                }
                this.lastSpectateChange = gameLocal.time + 500;
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

            final idAngles current = this.loggedViewAngles[ gameLocal.framenum & (NUM_LOGGED_VIEW_ANGLES - 1)];

            idAngles av;//, base;
            final int[] weaponAngleOffsetAverages = {0};
            final float[] weaponAngleOffsetScale = {0}, weaponAngleOffsetMax = {0};

            this.weapon.GetEntity().GetWeaponAngleOffsets(weaponAngleOffsetAverages, weaponAngleOffsetScale, weaponAngleOffsetMax);

            av = current;

            // calcualte this so the wrap arounds work properly
            for (int j = 1; j < weaponAngleOffsetAverages[0]; j++) {
                final idAngles a2 = this.loggedViewAngles[ (gameLocal.framenum - j) & (NUM_LOGGED_VIEW_ANGLES - 1)];

                final idAngles delta = a2.oMinus(current);

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
            final idVec3 ofs = new idVec3();

            final float[] weaponOffsetTime = {0}, weaponOffsetScale = {0};

            ofs.Zero();

            this.weapon.GetEntity().GetWeaponTimeOffsets(weaponOffsetTime, weaponOffsetScale);

            int stop = this.currentLoggedAccel - NUM_LOGGED_ACCELS;
            if (stop < 0) {
                stop = 0;
            }
            for (int i = this.currentLoggedAccel - 1; i > stop; i--) {
                final loggedAccel_t acc = this.loggedAccel[i & (NUM_LOGGED_ACCELS - 1)];

                float f;
                final float t = gameLocal.time - acc.time;
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
            idVec3 origin;
			final idVec3 velocity;
            idVec3 gravityVector, gravityNormal;
            float delta;
            float hardDelta, fatalDelta;
            float dist;
            float vel, acc;
            float t;
            float a, b, c, den;
            waterLevel_t waterLevel;
            boolean noDamage;

            this.AI_SOFTLANDING.operator(false);
            this.AI_HARDLANDING.operator(false);

            // if the player is not on the ground
            if (!this.physicsObj.HasGroundContacts()) {
                return;
            }

            gravityNormal = this.physicsObj.GetGravityNormal();

            // if the player wasn't going down
            if ((oldVelocity.oMultiply(gravityNormal.oNegative())) >= 0) {
                return;
            }

            waterLevel = this.physicsObj.GetWaterLevel();

            // never take falling damage if completely underwater
            if (waterLevel == WATERLEVEL_HEAD) {
                return;
            }

            // no falling damage if touching a nodamage surface
            noDamage = false;
            for (int i = 0; i < this.physicsObj.GetNumContacts(); i++) {
                final contactInfo_t contact = this.physicsObj.GetContact(i);
                if ((contact.material.GetSurfaceFlags() & SURF_NODAMAGE) != 0) {
                    noDamage = true;
                    StartSound("snd_land_hard", SND_CHANNEL_ANY, 0, false, null);
                    break;
                }
            }

            origin = GetPhysics().GetOrigin();
            gravityVector = this.physicsObj.GetGravity();

            // calculate the exact velocity on landing
            dist = (origin.oMinus(oldOrigin)).oMultiply(gravityNormal.oNegative());
            vel = oldVelocity.oMultiply(gravityNormal.oNegative());
            acc = -gravityVector.Length();

            a = acc / 2.0f;
            b = vel;
            c = -dist;

            den = (b * b) - (4.0f * a * c);
            if (den < 0) {
                return;
            }
            t = (-b - idMath.Sqrt(den)) / (2.0f * a);

            delta = vel + (t * acc);
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
                this.AI_HARDLANDING.operator(true);
                this.landChange = -32;
                this.landTime = gameLocal.time;
                if (!noDamage) {
                    this.pain_debounce_time = gameLocal.time + this.pain_delay + 1;  // ignore pain since we'll play our landing anim
                    Damage(null, null, new idVec3(0, 0, -1), "damage_fatalfall", 1.0f, 0);
                }
            } else if (delta > hardDelta) {
                this.AI_HARDLANDING.operator(true);
                this.landChange = -24;
                this.landTime = gameLocal.time;
                if (!noDamage) {
                    this.pain_debounce_time = gameLocal.time + this.pain_delay + 1;  // ignore pain since we'll play our landing anim
                    Damage(null, null, new idVec3(0, 0, -1), "damage_hardfall", 1.0f, 0);
                }
            } else if (delta > 30) {
                this.AI_HARDLANDING.operator(true);
                this.landChange = -16;
                this.landTime = gameLocal.time;
                if (!noDamage) {
                    this.pain_debounce_time = gameLocal.time + this.pain_delay + 1;  // ignore pain since we'll play our landing anim
                    Damage(null, null, new idVec3(0, 0, -1), "damage_softfall", 1.0f, 0);
                }
            } else if (delta > 7) {
                this.AI_SOFTLANDING.operator(true);
                this.landChange = -8;
                this.landTime = gameLocal.time;
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
            velocity = this.physicsObj.GetLinearVelocity().oMinus(pushVelocity);

            gravityDir = this.physicsObj.GetGravityNormal();
            vel = velocity.oMinus(gravityDir.oMultiply(velocity.oMultiply(gravityDir)));
            this.xyspeed = vel.LengthFast();

            // do not evaluate the bob for other clients
            // when doing a spectate follow, don't do any weapon bobbing
            if (gameLocal.isClient && (this.entityNumber != gameLocal.localClientNum)) {
                this.viewBobAngles.Zero();
                this.viewBob.Zero();
                return;
            }

            if (!this.physicsObj.HasGroundContacts() || (this.influenceActive == INFLUENCE_LEVEL2) || (gameLocal.isMultiplayer && this.spectating)) {
                // airborne
                this.bobCycle = 0;
                this.bobFoot = 0;
                this.bobfracsin = 0;
            } else if (((0 == this.usercmd.forwardmove) && (0 == this.usercmd.rightmove)) || (this.xyspeed <= MIN_BOB_SPEED)) {
                // start at beginning of cycle again
                this.bobCycle = 0;
                this.bobFoot = 0;
                this.bobfracsin = 0;
            } else {
                if (this.physicsObj.IsCrouching()) {
                    bobmove = pm_crouchbob.GetFloat();
                    // ducked characters never play footsteps
                } else {
                    // vary the bobbing based on the speed of the player
                    bobmove = (pm_walkbob.GetFloat() * (1.0f - this.bobFrac)) + (pm_runbob.GetFloat() * this.bobFrac);
                }

                // check for footstep / splash sounds
                old = this.bobCycle;
                this.bobCycle = (int) (old + (bobmove * idGameLocal.msec)) & 255;
                this.bobFoot = (this.bobCycle & 128) >> 7;
                this.bobfracsin = idMath.Fabs((float) Math.sin(((this.bobCycle & 127) / 127.0) * idMath.PI));
            }

            // calculate angles for view bobbing
            this.viewBobAngles.Zero();

            viewaxis = this.viewAngles.ToMat3().oMultiply(this.physicsObj.GetGravityAxis());

            // add angles based on velocity
            delta = velocity.oMultiply(viewaxis.oGet(0));
            this.viewBobAngles.pitch += delta * pm_runpitch.GetFloat();

            delta = velocity.oMultiply(viewaxis.oGet(1));
            this.viewBobAngles.roll -= delta * pm_runroll.GetFloat();

            // add angles based on bob
            // make sure the bob is visible even at low speeds
            speed = this.xyspeed > 200 ? this.xyspeed : 200;

            delta = this.bobfracsin * pm_bobpitch.GetFloat() * speed;
            if (this.physicsObj.IsCrouching()) {
                delta *= 3;		// crouching
            }
            this.viewBobAngles.pitch += delta;
            delta = this.bobfracsin * pm_bobroll.GetFloat() * speed;
            if (this.physicsObj.IsCrouching()) {
                delta *= 3;		// crouching accentuates roll
            }
            if ((this.bobFoot & 1) != 0) {
                delta = -delta;
            }
            this.viewBobAngles.roll += delta;

            // calculate position for view bobbing
            this.viewBob.Zero();

            if (this.physicsObj.HasSteppedUp()) {

                // check for stepping up before a previous step is completed
                deltaTime = gameLocal.time - this.stepUpTime;
                if (deltaTime < STEPUP_TIME) {
                    this.stepUpDelta = ((this.stepUpDelta * (STEPUP_TIME - deltaTime)) / STEPUP_TIME) + this.physicsObj.GetStepUp();
                } else {
                    this.stepUpDelta = this.physicsObj.GetStepUp();
                }
                if (this.stepUpDelta > (2.0f * pm_stepsize.GetFloat())) {
                    this.stepUpDelta = 2.0f * pm_stepsize.GetFloat();
                }
                this.stepUpTime = gameLocal.time;
            }

            final idVec3 gravity = this.physicsObj.GetGravityNormal();

            // if the player stepped up recently
            deltaTime = gameLocal.time - this.stepUpTime;
            if (deltaTime < STEPUP_TIME) {
                this.viewBob.oPluSet(gravity.oMultiply((this.stepUpDelta * (STEPUP_TIME - deltaTime)) / STEPUP_TIME));
            }

            // add bob height after any movement smoothing
            bob = this.bobfracsin * this.xyspeed * pm_bobup.GetFloat();
            if (bob > 6) {
                bob = 6;
            }
            this.viewBob.oPluSet(2, bob);

            // add fall height
            delta = gameLocal.time - this.landTime;
            if (delta < LAND_DEFLECT_TIME) {
                f = delta / LAND_DEFLECT_TIME;
                this.viewBob.oMinSet(gravity.oMultiply(this.landChange * f));
            } else if (delta < (LAND_DEFLECT_TIME + LAND_RETURN_TIME)) {
                delta -= LAND_DEFLECT_TIME;
                f = 1.0f - (delta / LAND_RETURN_TIME);
                this.viewBob.oMinSet(gravity.oMultiply(this.landChange * f));
            }
        }

        private void UpdateViewAngles() {
            int i;
            final idAngles delta = new idAngles();

            if (!this.noclip && (gameLocal.inCinematic || (this.privateCameraView != null) || (gameLocal.GetCamera() != null) || (this.influenceActive == INFLUENCE_LEVEL2) || this.objectiveSystemOpen)) {
                // no view changes at all, but we still want to update the deltas or else when
                // we get out of this mode, our view will snap to a kind of random angle
                UpdateDeltaViewAngles(this.viewAngles);
                return;
            }

            // if dead
            if (this.health <= 0) {
                if (pm_thirdPersonDeath.GetBool()) {
                    this.viewAngles.roll = 0.0f;
                    this.viewAngles.pitch = 30.0f;
                } else {
                    this.viewAngles.roll = 40.0f;
                    this.viewAngles.pitch = -15.0f;
                }
                return;
            }

            // circularly clamp the angles with deltas
            for (i = 0; i < 3; i++) {
                this.cmdAngles.oSet(i, SHORT2ANGLE(this.usercmd.angles[i]));
                if (this.influenceActive == INFLUENCE_LEVEL3) {
                    this.viewAngles.oPluSet(i, idMath.ClampFloat(-1.0f, 1.0f, idMath.AngleDelta(idMath.AngleNormalize180(SHORT2ANGLE(this.usercmd.angles[i]) + this.deltaViewAngles.oGet(i)), this.viewAngles.oGet(i))));
                } else {
                    this.viewAngles.oSet(i, idMath.AngleNormalize180(SHORT2ANGLE(this.usercmd.angles[i]) + this.deltaViewAngles.oGet(i)));
                }
            }
            if (!this.centerView.IsDone(gameLocal.time)) {
                this.viewAngles.pitch = this.centerView.GetCurrentValue(gameLocal.time);
            }

            // clamp the pitch
            if (this.noclip) {
                if (this.viewAngles.pitch > 89.0f) {
                    // don't let the player look down more than 89 degrees while noclipping
                    this.viewAngles.pitch = 89.0f;
                } else if (this.viewAngles.pitch < -89.0f) {
                    // don't let the player look up more than 89 degrees while noclipping
                    this.viewAngles.pitch = -89.0f;
                }
            } else {
                if (this.viewAngles.pitch > pm_maxviewpitch.GetFloat()) {
                    // don't let the player look down enough to see the shadow of his (non-existant) feet
                    this.viewAngles.pitch = pm_maxviewpitch.GetFloat();
                } else if (this.viewAngles.pitch < pm_minviewpitch.GetFloat()) {
                    // don't let the player look up more than 89 degrees
                    this.viewAngles.pitch = pm_minviewpitch.GetFloat();
                }
            }

            UpdateDeltaViewAngles(this.viewAngles);

            // orient the model towards the direction we're looking
            SetAngles(new idAngles(0, this.viewAngles.yaw, 0));

            // save in the log for analyzing weapon angle offsets
            this.loggedViewAngles[ gameLocal.framenum & (NUM_LOGGED_VIEW_ANGLES - 1)] = this.viewAngles;
        }

        private void EvaluateControls() {
            // check for respawning
            if (this.health <= 0) {
                if ((gameLocal.time > this.minRespawnTime) && ((this.usercmd.buttons & BUTTON_ATTACK) != 0)) {
                    this.forceRespawn = true;
                } else if (gameLocal.time > this.maxRespawnTime) {
                    this.forceRespawn = true;
                }
            }

            // in MP, idMultiplayerGame decides spawns
            if (this.forceRespawn && !gameLocal.isMultiplayer && !g_testDeath.GetBool()) {
                // in single player, we let the session handle restarting the level or loading a game
                gameLocal.sessionCommand.oSet("died");
            }

            if ((this.usercmd.flags & UCF_IMPULSE_SEQUENCE) != (this.oldFlags & UCF_IMPULSE_SEQUENCE)) {
                PerformImpulse(this.usercmd.impulse);
            }

            this.scoreBoardOpen = (((this.usercmd.buttons & BUTTON_SCORES) != 0) || this.forceScoreBoard);

            this.oldFlags = this.usercmd.flags;

            AdjustSpeed();

            // update the viewangles
            UpdateViewAngles();
        }

        private void AdjustSpeed() {
            float speed;
            float rate;

            if (this.spectating) {
                speed = pm_spectatespeed.GetFloat();
                this.bobFrac = 0;
            } else if (this.noclip) {
                speed = pm_noclipspeed.GetFloat();
                this.bobFrac = 0;
            } else if (!this.physicsObj.OnLadder() && ((this.usercmd.buttons & BUTTON_RUN) != 0) && ((this.usercmd.forwardmove != 0) || (this.usercmd.rightmove != 0)) && (this.usercmd.upmove >= 0)) {
                if (!gameLocal.isMultiplayer && !this.physicsObj.IsCrouching() && !PowerUpActive(ADRENALINE)) {
                    this.stamina -= MS2SEC(idGameLocal.msec);
                }
                if (this.stamina < 0) {
                    this.stamina = 0;
                }
                if ((NOT(pm_stamina.GetFloat())) || (this.stamina > pm_staminathreshold.GetFloat())) {
                    this.bobFrac = 1.0f;
                } else if (pm_staminathreshold.GetFloat() <= 0.0001f) {
                    this.bobFrac = 0;
                } else {
                    this.bobFrac = this.stamina / pm_staminathreshold.GetFloat();
                }
                speed = (pm_walkspeed.GetFloat() * (1.0f - this.bobFrac)) + (pm_runspeed.GetFloat() * this.bobFrac);
            } else {
                rate = pm_staminarate.GetFloat();

                // increase 25% faster when not moving
                if ((this.usercmd.forwardmove == 0) && (this.usercmd.rightmove == 0) && (!this.physicsObj.OnLadder() || (this.usercmd.upmove == 0))) {
                    rate *= 1.25f;
                }

                this.stamina += rate * MS2SEC(idGameLocal.msec);
                if (this.stamina > pm_stamina.GetFloat()) {
                    this.stamina = pm_stamina.GetFloat();
                }
                speed = pm_walkspeed.GetFloat();
                this.bobFrac = 0;
            }

            speed *= PowerUpModifier(SPEED);

            if (this.influenceActive == INFLUENCE_LEVEL3) {
                speed *= 0.33f;
            }

            this.physicsObj.SetSpeed(speed, pm_crouchspeed.GetFloat());
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

            if (this.health < 0) {
                return;
            }

            blend = true;

            if (!this.physicsObj.HasGroundContacts()) {
                this.idealLegsYaw = 0;
                this.legsForward = true;
            } else if (this.usercmd.forwardmove < 0) {
                this.idealLegsYaw = idMath.AngleNormalize180(new idVec3(-this.usercmd.forwardmove, this.usercmd.rightmove, 0).ToYaw());
                this.legsForward = false;
            } else if (this.usercmd.forwardmove > 0) {
                this.idealLegsYaw = idMath.AngleNormalize180(new idVec3(this.usercmd.forwardmove, -this.usercmd.rightmove, 0).ToYaw());
                this.legsForward = true;
            } else if ((this.usercmd.rightmove != 0) && this.physicsObj.IsCrouching()) {
                if (!this.legsForward) {
                    this.idealLegsYaw = idMath.AngleNormalize180(new idVec3(idMath.Abs(this.usercmd.rightmove), this.usercmd.rightmove, 0).ToYaw());
                } else {
                    this.idealLegsYaw = idMath.AngleNormalize180(new idVec3(idMath.Abs(this.usercmd.rightmove), -this.usercmd.rightmove, 0).ToYaw());
                }
            } else if (this.usercmd.rightmove != 0) {
                this.idealLegsYaw = 0;
                this.legsForward = true;
            } else {
                this.legsForward = true;
                diff = idMath.Fabs(this.idealLegsYaw - this.legsYaw);
                this.idealLegsYaw = this.idealLegsYaw - idMath.AngleNormalize180(this.viewAngles.yaw - this.oldViewYaw);
                if (diff < 0.1f) {
                    this.legsYaw = this.idealLegsYaw;
                    blend = false;
                }
            }

            if (!this.physicsObj.IsCrouching()) {
                this.legsForward = true;
            }

            this.oldViewYaw = this.viewAngles.yaw;

            this.AI_TURN_LEFT.operator(false);
            this.AI_TURN_RIGHT.operator(false);
            if (this.idealLegsYaw < -45.0f) {
                this.idealLegsYaw = 0;
                this.AI_TURN_RIGHT.operator(true);
                blend = true;
            } else if (this.idealLegsYaw > 45.0f) {
                this.idealLegsYaw = 0;
                this.AI_TURN_LEFT.operator(true);
                blend = true;
            }

            if (blend) {
                this.legsYaw = (this.legsYaw * 0.9f) + (this.idealLegsYaw * 0.1f);
            }
            legsAxis = new idAngles(0, this.legsYaw, 0).ToMat3();
            this.animator.SetJointAxis(this.hipJoint, JOINTMOD_WORLD, legsAxis);

            // calculate the blending between down, straight, and up
            frac = this.viewAngles.pitch / 90;
            if (frac > 0) {
                downBlend = frac;
                forwardBlend = 1.0f - frac;
                upBlend = 0;
            } else {
                downBlend = 0;
                forwardBlend = 1.0f + frac;
                upBlend = -frac;
            }

            this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(0, downBlend);
            this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(1, forwardBlend);
            this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(2, upBlend);

            this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(0, downBlend);
            this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(1, forwardBlend);
            this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(2, upBlend);
        }

        private void InitAASLocation() {
            int i;
            int num;
            idVec3 size;
            final idBounds bounds = new idBounds();
            idAAS aas;
            final idVec3 origin = new idVec3();

            GetFloorPos(64.0f, origin);

            num = gameLocal.NumAAS();
            this.aasLocation.SetGranularity(1);
            this.aasLocation.SetNum(num);
            for (i = 0; i < this.aasLocation.Num(); i++) {
                this.aasLocation.oSet(i, new aasLocation_t());
                this.aasLocation.oGet(i).areaNum = 0;
                this.aasLocation.oGet(i).pos = new idVec3(origin);
                aas = gameLocal.GetAAS(i);
                if ((aas != null) && (aas.GetSettings() != null)) {
                    size = aas.GetSettings().boundingBoxes[0].oGet(1);
                    bounds.oSet(0, size.oNegative());
                    size.z = 32.0f;
                    bounds.oSet(1, size);

                    this.aasLocation.oGet(i).areaNum = aas.PointReachableAreaNum(origin, bounds, AREA_REACHABLE_WALK);
                }
            }
        }

        private void SetAASLocation() {
            int i;
            int areaNum;
            idVec3 size;
            final idBounds bounds = new idBounds();
            idAAS aas;
            final idVec3 origin = new idVec3();

            if (!GetFloorPos(64.0f, origin)) {
                return;
            }

            for (i = 0; i < this.aasLocation.Num(); i++) {
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
                    this.aasLocation.oGet(i).pos = origin;
                    this.aasLocation.oGet(i).areaNum = areaNum;
                }
            }
        }

        private void Move() {
            float newEyeOffset;
            idVec3 oldOrigin;
            idVec3 oldVelocity;
            idVec3 pushVelocity;

            // save old origin and velocity for crashlanding
            oldOrigin = this.physicsObj.GetOrigin();
            oldVelocity = this.physicsObj.GetLinearVelocity();
            pushVelocity = this.physicsObj.GetPushedLinearVelocity();

            // set physics variables
            this.physicsObj.SetMaxStepHeight(pm_stepsize.GetFloat());
            this.physicsObj.SetMaxJumpHeight(pm_jumpheight.GetFloat());

            if (this.noclip) {
                this.physicsObj.SetContents(0);
                this.physicsObj.SetMovementType(PM_NOCLIP);
            } else if (this.spectating) {
                this.physicsObj.SetContents(0);
                this.physicsObj.SetMovementType(PM_SPECTATOR);
            } else if (this.health <= 0) {
                this.physicsObj.SetContents(CONTENTS_CORPSE | CONTENTS_MONSTERCLIP);
                this.physicsObj.SetMovementType(PM_DEAD);
            } else if (gameLocal.inCinematic || (gameLocal.GetCamera() != null) || (this.privateCameraView != null) || (this.influenceActive == INFLUENCE_LEVEL2)) {
                this.physicsObj.SetContents(CONTENTS_BODY);
                this.physicsObj.SetMovementType(PM_FREEZE);
            } else {
                this.physicsObj.SetContents(CONTENTS_BODY);
                this.physicsObj.SetMovementType(PM_NORMAL);
            }

            if (this.spectating) {
                this.physicsObj.SetClipMask(MASK_DEADSOLID);
            } else if (this.health <= 0) {
                this.physicsObj.SetClipMask(MASK_DEADSOLID);
            } else {
                this.physicsObj.SetClipMask(MASK_PLAYERSOLID);
            }

            this.physicsObj.SetDebugLevel(g_debugMove.GetBool());
            this.physicsObj.SetPlayerInput(this.usercmd, this.viewAngles);

            // FIXME: physics gets disabled somehow
            BecomeActive(TH_PHYSICS);
            RunPhysics();

            // update our last valid AAS location for the AI
            SetAASLocation();

            if (this.spectating) {
                newEyeOffset = 0.0f;
            } else if (this.health <= 0) {
                newEyeOffset = pm_deadviewheight.GetFloat();
            } else if (this.physicsObj.IsCrouching()) {
                newEyeOffset = pm_crouchviewheight.GetFloat();
            } else if ((GetBindMaster() != null) && GetBindMaster().IsType(idAFEntity_Vehicle.class)) {
                newEyeOffset = 0.0f;
            } else {
                newEyeOffset = pm_normalviewheight.GetFloat();
            }

            if (EyeHeight() != newEyeOffset) {
                if (this.spectating) {
                    SetEyeHeight(newEyeOffset);
                } else {
                    // smooth out duck height changes
                    SetEyeHeight((EyeHeight() * pm_crouchrate.GetFloat()) + (newEyeOffset * (1.0f - pm_crouchrate.GetFloat())));
                }
            }

            if (this.noclip || gameLocal.inCinematic || (this.influenceActive == INFLUENCE_LEVEL2)) {
                this.AI_CROUCH.operator(false);
                this.AI_ONGROUND.operator(this.influenceActive == INFLUENCE_LEVEL2);
                this.AI_ONLADDER.operator(false);
                this.AI_JUMP.operator(false);
            } else {
                this.AI_CROUCH.operator(this.physicsObj.IsCrouching());
                this.AI_ONGROUND.operator(this.physicsObj.HasGroundContacts());
                this.AI_ONLADDER.operator(this.physicsObj.OnLadder());
                this.AI_JUMP.operator(this.physicsObj.HasJumped());

                // check if we're standing on top of a monster and give a push if we are
                final idEntity groundEnt = this.physicsObj.GetGroundEntity();
                if ((groundEnt != null) && groundEnt.IsType(idAI.class)) {
                    final idVec3 vel = this.physicsObj.GetLinearVelocity();
                    if (vel.ToVec2().LengthSqr() < 0.1f) {
                        vel.oSet(this.physicsObj.GetOrigin().ToVec2().oMinus(groundEnt.GetPhysics().GetAbsBounds().GetCenter().ToVec2()));
                        vel.ToVec2_NormalizeFast();
                        vel.ToVec2_oMulSet(pm_walkspeed.GetFloat());//TODO:ToVec2 back ref.
                    } else {
                        // give em a push in the direction they're going
                        vel.oMulSet(1.1f);
                    }
                    this.physicsObj.SetLinearVelocity(vel);
                }
            }

            if (this.AI_JUMP.operator()) {
                // bounce the view weapon
                final loggedAccel_t acc = this.loggedAccel[this.currentLoggedAccel & (NUM_LOGGED_ACCELS - 1)];
                this.currentLoggedAccel++;
                acc.time = gameLocal.time;
                acc.dir.oSet(2, 200);
                acc.dir.oSet(0, acc.dir.oSet(1, 0));
            }

            if (this.AI_ONLADDER.operator()) {
                final int old_rung = (int) (oldOrigin.z / LADDER_RUNG_DISTANCE);
                final int new_rung = (int) (this.physicsObj.GetOrigin().z / LADDER_RUNG_DISTANCE);

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
                    if (PowerUpActive(i) && (this.inventory.powerupEndTime[i] <= gameLocal.time)) {
                        ClearPowerup(i);
                    }
                }
            }

            if (this.health > 0) {
                if (this.powerUpSkin != null) {
                    this.renderEntity.customSkin = this.powerUpSkin;
                } else {
                    this.renderEntity.customSkin = this.skin;
                }
            }

            if ((this.healthPool != 0) && (gameLocal.time > this.nextHealthPulse) && !this.AI_DEAD.operator() && (this.health > 0)) {
                assert (!gameLocal.isClient);	// healthPool never be set on client
                final int amt = (int) ((this.healthPool > 5) ? 5 : this.healthPool);
                this.health += amt;
                if (this.health > this.inventory.maxHealth) {
                    this.health = this.inventory.maxHealth;
                    this.healthPool = 0;
                } else {
                    this.healthPool -= amt;
                }
                this.nextHealthPulse = gameLocal.time + HEALTHPULSE_TIME;
                this.healthPulse = true;
            }
            if (BuildDefines.ID_DEMO_BUILD) {
                if (!gameLocal.inCinematic && (this.influenceActive == 0) && (g_skill.GetInteger() == 3) && (gameLocal.time > this.nextHealthTake) && !this.AI_DEAD.operator() && (this.health > g_healthTakeLimit.GetInteger())) {
                    assert (!gameLocal.isClient);	// healthPool never be set on client
                    this.health -= g_healthTakeAmt.GetInteger();
                    if (this.health < g_healthTakeLimit.GetInteger()) {
                        this.health = g_healthTakeLimit.GetInteger();
                    }
                    this.nextHealthTake = gameLocal.time + (g_healthTakeTime.GetInteger() * 1000);
                    this.healthTake = true;
                }
            }
        }

        private void UpdateDeathSkin(boolean state_hitch) {
            if (!(gameLocal.isMultiplayer || g_testDeath.GetBool())) {
                return;
            }
            if (this.health <= 0) {
                if (!this.doingDeathSkin) {
                    this.deathClearContentsTime = this.spawnArgs.GetInt("deathSkinTime");
                    this.doingDeathSkin = true;
                    this.renderEntity.noShadow = true;
                    if (state_hitch) {
                        this.renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = (gameLocal.time * 0.001f) - 2.0f;
                    } else {
                        this.renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = gameLocal.time * 0.001f;
                    }
                    UpdateVisuals();
                }

                // wait a bit before switching off the content
                if ((this.deathClearContentsTime != 0) && (gameLocal.time > this.deathClearContentsTime)) {
                    SetCombatContents(false);
                    this.deathClearContentsTime = 0;
                }
            } else {
                this.renderEntity.noShadow = false;
                this.renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = 0.0f;
                UpdateVisuals();
                this.doingDeathSkin = false;
            }
        }

        private void ClearPowerup(int i) {

            if (gameLocal.isServer) {
                final idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.WriteShort(i);
                msg.WriteBits(0, 1);
                ServerSendEvent(EVENT_POWERUP, msg, false, -1);
            }

            this.powerUpSkin = null;
            this.inventory.powerups &= ~(1 << i);
            this.inventory.powerupEndTime[ i] = 0;
            switch (i) {
                case BERSERK: {
                    StopSound(etoi(SND_CHANNEL_DEMONIC), false);
                    break;
                }
                case INVISIBILITY: {
                    if (this.weapon.GetEntity() != null) {
                        this.weapon.GetEntity().UpdateSkin();
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
            this.focusCharacter = null;
            this.focusGUIent = null;
            this.focusUI = null;
            this.focusVehicle = null;
            this.talkCursor = 0;
        }

        /*
         ================
         idPlayer::UpdateFocus

         Searches nearby entities for interactive guis, possibly making one of them
         the focus and sending it a mouse move event
         ================
         */
        private void UpdateFocus() {
            final idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
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
            final trace_s[] trace = {null};
            guiPoint_t pt;
            idKeyValue kv;
            sysEvent_s ev;
            idUserInterface ui;

            if (gameLocal.inCinematic) {
                return;
            }

            // only update the focus character when attack button isn't pressed so players
            // can still chainsaw NPC's
            if (gameLocal.isMultiplayer || (NOT(this.focusCharacter) && ((this.usercmd.buttons & BUTTON_ATTACK) != 0))) {
                allowFocus = false;
            } else {
                allowFocus = true;
            }

            oldFocus = this.focusGUIent;
            oldUI = this.focusUI;
            oldChar = this.focusCharacter;
            oldTalkCursor = this.talkCursor;
            oldVehicle = this.focusVehicle;

            if (this.focusTime <= gameLocal.time) {
                ClearFocus();
            }

            // don't let spectators interact with GUIs
            if (this.spectating) {
                return;
            }

            start = GetEyePosition();
            end = start.oPlus(this.viewAngles.ToForward().oMultiply(80.0f));

            // player identification . names to the hud
            if (gameLocal.isMultiplayer && (this.entityNumber == gameLocal.localClientNum)) {
                final idVec3 end2 = start.oPlus(this.viewAngles.ToForward().oMultiply(768.0f));
                gameLocal.clip.TracePoint(trace, start, end2, MASK_SHOT_BOUNDINGBOX, this);
                int iclient = -1;
                if ((trace[0].fraction < 1.0f) && (trace[0].c.entityNum < MAX_CLIENTS)) {
                    iclient = trace[0].c.entityNum;
                }
                if (this.MPAim != iclient) {
                    this.lastMPAim = this.MPAim;
                    this.MPAim = iclient;
                    this.lastMPAimTime = gameLocal.realClientTime;
                }
            }

            final idBounds bounds = new idBounds(start);
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
                        final idEntity body = ((idAFAttachment) ent).GetBody();
                        if ((body != null) && body.IsType(idAI.class)
                                && (etoi(((idAI) body).GetTalkState()) >= etoi(TALK_OK))) {
                            gameLocal.clip.TracePoint(trace, start, end, MASK_SHOT_RENDERMODEL, this);
                            if ((trace[0].fraction < 1.0f) && (trace[0].c.entityNum == ent.entityNumber)) {
                                ClearFocus();
                                this.focusCharacter = (idAI) body;
                                this.talkCursor = 1;
                                this.focusTime = gameLocal.time + FOCUS_TIME;
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
                                this.focusCharacter = (idAI) ent;
                                this.talkCursor = 1;
                                this.focusTime = gameLocal.time + FOCUS_TIME;
                                break;
                            }
                        }
                        continue;
                    }

                    if (ent.IsType(idAFEntity_Vehicle.class)) {
                        gameLocal.clip.TracePoint(trace, start, end, MASK_SHOT_RENDERMODEL, this);
                        if ((trace[0].fraction < 1.0f) && (trace[0].c.entityNum == ent.entityNumber)) {
                            ClearFocus();
                            this.focusVehicle = (idAFEntity_Vehicle) ent;
                            this.focusTime = gameLocal.time + FOCUS_TIME;
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
                    final renderEntity_s focusGUIrenderEntity = ent.GetRenderEntity();
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
                    this.focusGUIent = ent;
                    this.focusUI = ui;

                    if (oldFocus != ent) {
                        // new activation
                        // going to see if we have anything in inventory a gui might be interested in
                        // need to enumerate inventory items
                        this.focusUI.SetStateInt("inv_count", this.inventory.items.Num());
                        for (j = 0; j < this.inventory.items.Num(); j++) {
                            final idDict item = this.inventory.items.oGet(j);
                            final String iname = item.GetString("inv_name");
                            final String iicon = item.GetString("inv_icon");
                            final String itext = item.GetString("inv_text");

                            this.focusUI.SetStateString(va("inv_name_%d", j), iname);
                            this.focusUI.SetStateString(va("inv_icon_%d", j), iicon);
                            this.focusUI.SetStateString(va("inv_text_%d", j), itext);
                            kv = item.MatchPrefix("inv_id", null);
                            if (kv != null) {
                                this.focusUI.SetStateString(va("inv_id_%d", j), kv.GetValue().getData());
                            }
                            this.focusUI.SetStateInt(iname, 1);
                        }

                        for (j = 0; j < this.inventory.pdaSecurity.Num(); j++) {
                            final String p = this.inventory.pdaSecurity.oGet(j).getData();
                            if (isNotNullOrEmpty(p)) {
                                this.focusUI.SetStateInt(p, 1);
                            }
                        }

                        final int staminapercentage = (int) ((100.0f * this.stamina) / pm_stamina.GetFloat());
                        this.focusUI.SetStateString("player_health", va("%d", this.health));
                        this.focusUI.SetStateString("player_stamina", va("%d%%", staminapercentage));
                        this.focusUI.SetStateString("player_armor", va("%d%%", this.inventory.armor));

                        kv = this.focusGUIent.spawnArgs.MatchPrefix("gui_parm", null);
                        while (kv != null) {
                            this.focusUI.SetStateString(kv.GetKey().getData(), kv.GetValue().getData());
                            kv = this.focusGUIent.spawnArgs.MatchPrefix("gui_parm", kv);
                        }
                    }

                    // clamp the mouse to the corner
                    ev = sys.GenerateMouseMoveEvent(-2000, -2000);
                    command = this.focusUI.HandleEvent(ev, gameLocal.time);
                    HandleGuiCommands(this.focusGUIent, command);

                    // move to an absolute position
                    ev = sys.GenerateMouseMoveEvent((int) (pt.x * SCREEN_WIDTH), (int) (pt.y * SCREEN_HEIGHT));
                    command = this.focusUI.HandleEvent(ev, gameLocal.time);
                    HandleGuiCommands(this.focusGUIent, command);
                    this.focusTime = gameLocal.time + FOCUS_GUI_TIME;
                    break;
                }
            }

            if ((this.focusGUIent != null) && (this.focusUI != null)) {
                if (NOT(oldFocus) || !oldFocus.equals(this.focusGUIent)) {
                    command = this.focusUI.Activate(true, gameLocal.time);
                    HandleGuiCommands(this.focusGUIent, command);
                    StartSound("snd_guienter", SND_CHANNEL_ANY, 0, false, null);
                    // HideTip();
                    // HideObjective();
                }
            } else if ((oldFocus != null) && (oldUI != null)) {
                command = oldUI.Activate(false, gameLocal.time);
                HandleGuiCommands(oldFocus, command);
                StartSound("snd_guiexit", SND_CHANNEL_ANY, 0, false, null);
            }

            if ((this.cursor != null) && (oldTalkCursor != this.talkCursor)) {
                this.cursor.SetStateInt("talkcursor", this.talkCursor);
            }

            if ((oldChar != this.focusCharacter) && (this.hud != null)) {
                if (this.focusCharacter != null) {
                    this.hud.SetStateString("npc", this.focusCharacter.spawnArgs.GetString("npc_name", "Joe"));
                    this.hud.HandleNamedEvent("showNPC");
                    // HideTip();
                    // HideObjective();
                } else {
                    this.hud.SetStateString("npc", "");
                    this.hud.HandleNamedEvent("hideNPC");
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
            if (this.hud != null) {
                final idLocationEntity locationEntity = gameLocal.LocationForPoint(GetEyePosition());
                if (locationEntity != null) {
                    this.hud.SetStateString("location", locationEntity.GetLocation());
                } else {
                    this.hud.SetStateString("location", common.GetLanguageDict().GetString("#str_02911"));
                }
            }
        }

        private idUserInterface ActiveGui() {
            if (this.objectiveSystemOpen) {
                return this.objectiveSystem;
            }

            return this.focusUI;
        }

        private void UpdatePDAInfo(boolean updatePDASel) {
            int j, sel;

            if (this.objectiveSystem == null) {
                return;
            }

            assert (this.hud != null);

            int currentPDA = this.objectiveSystem.State().GetInt("listPDA_sel_0", "0");
            if (currentPDA == -1) {
                currentPDA = 0;
            }

            if (updatePDASel) {
                this.objectiveSystem.SetStateInt("listPDAVideo_sel_0", 0);
                this.objectiveSystem.SetStateInt("listPDAEmail_sel_0", 0);
                this.objectiveSystem.SetStateInt("listPDAAudio_sel_0", 0);
            }

            if (currentPDA > 0) {
                currentPDA = this.inventory.pdas.Num() - currentPDA;
            }

            // Mark in the bit array that this pda has been read
            if (currentPDA < 128) {
                this.inventory.pdasViewed[currentPDA >> 5] |= 1 << (currentPDA & 31);
            }

            this.pdaAudio.oSet("");
            this.pdaVideo.oSet("");
            this.pdaVideoWave.oSet("");
            String name, data;
			final String preview, info, wave;
            for (j = 0; j < MAX_PDAS; j++) {
                this.objectiveSystem.SetStateString(va("listPDA_item_%d", j), "");
            }
            for (j = 0; j < MAX_PDA_ITEMS; j++) {
                this.objectiveSystem.SetStateString(va("listPDAVideo_item_%d", j), "");
                this.objectiveSystem.SetStateString(va("listPDAAudio_item_%d", j), "");
                this.objectiveSystem.SetStateString(va("listPDAEmail_item_%d", j), "");
                this.objectiveSystem.SetStateString(va("listPDASecurity_item_%d", j), "");
            }
            for (j = 0; j < this.inventory.pdas.Num(); j++) {

                final idDeclPDA pda = (idDeclPDA) declManager.FindType(DECL_PDA, this.inventory.pdas.oGet(j), false);

                if (pda == null) {
                    continue;
                }

                int index = this.inventory.pdas.Num() - j;
                if (j == 0) {
                    // Special case for the first PDA
                    index = 0;
                }

                if ((j != currentPDA) && (j < 128) && ((this.inventory.pdasViewed[j >> 5] & (1 << (j & 31))) != 0)) {
                    // This pda has been read already, mark in gray
                    this.objectiveSystem.SetStateString(va("listPDA_item_%d", index), va(S_COLOR_GRAY, "%s", pda.GetPdaName()));
                } else {
                    // This pda has not been read yet
                    this.objectiveSystem.SetStateString(va("listPDA_item_%d", index), pda.GetPdaName());
                }

                String security = pda.GetSecurity();
                if ((j == currentPDA) || ((currentPDA == 0) && (security != null) && !security.isEmpty())) {
                    if (security.isEmpty()) {
                        security = common.GetLanguageDict().GetString("#str_00066");
                    }
                    this.objectiveSystem.SetStateString("PDASecurityClearance", security);
                }

                if (j == currentPDA) {

                    this.objectiveSystem.SetStateString("pda_icon", pda.GetIcon());
                    this.objectiveSystem.SetStateString("pda_id", pda.GetID());
                    this.objectiveSystem.SetStateString("pda_title", pda.GetTitle());

                    if (j == 0) {
                        // Selected, personal pda
                        // Add videos
                        if (updatePDASel || !this.inventory.pdaOpened) {
                            this.objectiveSystem.HandleNamedEvent("playerPDAActive");
                            this.objectiveSystem.SetStateString("pda_personal", "1");
                            this.inventory.pdaOpened = true;
                        }
                        this.objectiveSystem.SetStateString("pda_location", this.hud.State().GetString("location"));
                        this.objectiveSystem.SetStateString("pda_name", cvarSystem.GetCVarString("ui_name"));
                        AddGuiPDAData(DECL_VIDEO, "listPDAVideo", pda, this.objectiveSystem);
                        sel = this.objectiveSystem.State().GetInt("listPDAVideo_sel_0", "0");
                        idDeclVideo vid = null;
                        if ((sel >= 0) && (sel < this.inventory.videos.Num())) {
                            vid = (idDeclVideo) declManager.FindType(DECL_VIDEO, this.inventory.videos.oGet(sel), false);
                        }
                        if (vid != null) {
                            this.pdaVideo.oSet(vid.GetRoq());
                            this.pdaVideoWave.oSet(vid.GetWave());
                            this.objectiveSystem.SetStateString("PDAVideoTitle", vid.GetVideoName());
                            this.objectiveSystem.SetStateString("PDAVideoVid", vid.GetRoq());
                            this.objectiveSystem.SetStateString("PDAVideoIcon", vid.GetPreview());
                            this.objectiveSystem.SetStateString("PDAVideoInfo", vid.GetInfo());
                        } else {
                            //FIXME: need to precache these in the player def
                            this.objectiveSystem.SetStateString("PDAVideoVid", "sound/vo/video/welcome.tga");
                            this.objectiveSystem.SetStateString("PDAVideoIcon", "sound/vo/video/welcome.tga");
                            this.objectiveSystem.SetStateString("PDAVideoTitle", "");
                            this.objectiveSystem.SetStateString("PDAVideoInfo", "");
                        }
                    } else {
                        // Selected, non-personal pda
                        // Add audio logs
                        if (updatePDASel) {
                            this.objectiveSystem.HandleNamedEvent("playerPDANotActive");
                            this.objectiveSystem.SetStateString("pda_personal", "0");
                            this.inventory.pdaOpened = true;
                        }
                        this.objectiveSystem.SetStateString("pda_location", pda.GetPost());
                        this.objectiveSystem.SetStateString("pda_name", pda.GetFullName());
                        final int audioCount = AddGuiPDAData(DECL_AUDIO, "listPDAAudio", pda, this.objectiveSystem);
                        this.objectiveSystem.SetStateInt("audioLogCount", audioCount);
                        sel = this.objectiveSystem.State().GetInt("listPDAAudio_sel_0", "0");
                        idDeclAudio aud = null;
                        if (sel >= 0) {
                            aud = pda.GetAudioByIndex(sel);
                        }
                        if (aud != null) {
                            this.pdaAudio.oSet(aud.GetWave());
                            this.objectiveSystem.SetStateString("PDAAudioTitle", aud.GetAudioName());
                            this.objectiveSystem.SetStateString("PDAAudioIcon", aud.GetPreview());
                            this.objectiveSystem.SetStateString("PDAAudioInfo", aud.GetInfo());
                        } else {
                            this.objectiveSystem.SetStateString("PDAAudioIcon", "sound/vo/video/welcome.tga");
                            this.objectiveSystem.SetStateString("PDAAutioTitle", "");
                            this.objectiveSystem.SetStateString("PDAAudioInfo", "");
                        }
                    }
                    // add emails
                    name = "";
                    data = "";
                    final int numEmails = pda.GetNumEmails();
                    if (numEmails > 0) {
                        AddGuiPDAData(DECL_EMAIL, "listPDAEmail", pda, this.objectiveSystem);
                        sel = this.objectiveSystem.State().GetInt("listPDAEmail_sel_0", "-1");
                        if ((sel >= 0) && (sel < numEmails)) {
                            final idDeclEmail email = pda.GetEmailByIndex(sel);
                            name = email.GetSubject();
                            data = email.GetBody();
                        }
                    }
                    this.objectiveSystem.SetStateString("PDAEmailTitle", name);
                    this.objectiveSystem.SetStateString("PDAEmailText", data);
                }
            }
            if (this.objectiveSystem.State().GetInt("listPDA_sel_0", "-1") == -1) {
                this.objectiveSystem.SetStateInt("listPDA_sel_0", 0);
            }
            this.objectiveSystem.StateChanged(gameLocal.time);
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
                c = this.inventory.videos.Num();
                for (i = 0; i < c; i++) {
                    final idDeclVideo video = GetVideo(i);
                    if (video == null) {
                        work = va("Video CD %s not found", this.inventory.videos.oGet(i).getData());
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
            if (this.objectiveSystem == null) {
                return;
            }
            this.objectiveSystem.SetStateString("objective1", "");
            this.objectiveSystem.SetStateString("objective2", "");
            this.objectiveSystem.SetStateString("objective3", "");
            for (int i = 0; i < this.inventory.objectiveNames.Num(); i++) {
                this.objectiveSystem.SetStateString(va("objective%d", i + 1), "1");
                this.objectiveSystem.SetStateString(va("objectivetitle%d", i + 1), this.inventory.objectiveNames.oGet(i).title.getData());
                this.objectiveSystem.SetStateString(va("objectivetext%d", i + 1), this.inventory.objectiveNames.oGet(i).text.getData());
                this.objectiveSystem.SetStateString(va("objectiveshot%d", i + 1), this.inventory.objectiveNames.oGet(i).screenshot.getData());
            }
            this.objectiveSystem.StateChanged(gameLocal.time);
        }

        private void UseVehicle() {
            final trace_s[] trace = {null};
            idVec3 start, end;
            idEntity ent;

            if ((GetBindMaster() != null) && GetBindMaster().IsType(idAFEntity_Vehicle.class)) {
                Show();
                ((idAFEntity_Vehicle) GetBindMaster()).Use(this);
            } else {
                start = GetEyePosition();
                end = start.oPlus(this.viewAngles.ToForward().oMultiply(80.0f));
                gameLocal.clip.TracePoint(trace, start, end, MASK_SHOT_RENDERMODEL, this);
                if (trace[0].fraction < 1.0f) {
                    ent = gameLocal.entities[ trace[0].c.entityNum];
                    if ((ent != null) && ent.IsType(idAFEntity_Vehicle.class)) {
                        Hide();
                        ((idAFEntity_Vehicle) ent).Use(this);
                    }
                }
            }
        }

        private void Event_GetButtons() {
            idThread.ReturnInt(this.usercmd.buttons);
        }

        private void Event_GetMove() {
            final idVec3 move = new idVec3(this.usercmd.forwardmove, this.usercmd.rightmove, this.usercmd.upmove);
            idThread.ReturnVector(move);
        }

        private void Event_GetViewAngles() {
            idThread.ReturnVector(new idVec3(this.viewAngles.oGet(0), this.viewAngles.oGet(1), this.viewAngles.oGet(2)));
        }

        private void Event_StopFxFov() {
            this.fxFov = false;
        }

        private void Event_EnableWeapon() {
            this.hiddenWeapon = gameLocal.world.spawnArgs.GetBool("no_Weapons");
            this.weaponEnabled = true;
            if (this.weapon.GetEntity() != null) {
                this.weapon.GetEntity().ExitCinematic();
            }
        }

        private void Event_DisableWeapon() {
            this.hiddenWeapon = gameLocal.world.spawnArgs.GetBool("no_Weapons");
            this.weaponEnabled = false;
            if (this.weapon.GetEntity() != null) {
                this.weapon.GetEntity().EnterCinematic();
            }
        }

        private void Event_GetCurrentWeapon() {
            final String weapon;

            if (this.currentWeapon >= 0) {
                weapon = this.spawnArgs.GetString(va("def_weapon%d", this.currentWeapon));
                idThread.ReturnString(weapon);
            } else {
                idThread.ReturnString("");
            }
        }

        private void Event_GetPreviousWeapon() {
            final String weapon;

            if (this.previousWeapon >= 0) {
                final int pw = (gameLocal.world.spawnArgs.GetBool("no_Weapons")) ? 0 : this.previousWeapon;
                weapon = this.spawnArgs.GetString(va("def_weapon%d", pw));
                idThread.ReturnString(weapon);
            } else {
                idThread.ReturnString(this.spawnArgs.GetString("def_weapon0"));
            }
        }

        private void Event_SelectWeapon(final idEventArg<String> weaponName) {
            int i;
            int weaponNum;

            if (gameLocal.isClient) {
                gameLocal.Warning("Cannot switch weapons from script in multiplayer");
                return;
            }

            if (this.hiddenWeapon && gameLocal.world.spawnArgs.GetBool("no_Weapons")) {
                this.idealWeapon = this.weapon_fists;
                this.weapon.GetEntity().HideWeapon();
                return;
            }

            weaponNum = -1;
            for (i = 0; i < MAX_WEAPONS; i++) {
                if ((this.inventory.weapons & (1 << i)) != 0) {
                    final String weap = this.spawnArgs.GetString(va("def_weapon%d", i));
                    if (NOT(idStr.Cmp(weap, weaponName.value))) {
                        weaponNum = i;
                        break;
                    }
                }
            }

            if (weaponNum < 0) {
                gameLocal.Warning("%s is not carrying weapon '%s'", this.name, weaponName.value);
                return;
            }

            this.hiddenWeapon = false;
            this.idealWeapon = weaponNum;

            UpdateHudWeapon();
        }

        private void Event_GetWeaponEntity() {
            idThread.ReturnEntity(this.weapon.GetEntity());
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
            idThread.ReturnInt(this.objectiveSystemOpen);
        }

        private void Event_ExitTeleporter() {
            idEntity exitEnt;
            float pushVel;

            // verify and setup
            exitEnt = this.teleportEntity.GetEntity();
            if (NOT(exitEnt)) {
                common.DPrintf("Event_ExitTeleporter player %d while not being teleported\n", this.entityNumber);
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
            this.physicsObj.SetLinearVelocity(exitEnt.GetPhysics().GetAxis().oGet(0).oMultiply(pushVel));
            this.physicsObj.ClearPushedVelocity();
            // teleport fx
            this.playerView.Flash(colorWhite, 120);

            // clear the ik heights so model doesn't appear in the wrong place
            this.walkIK.EnableAll();

            UpdateVisuals();

            StartSound("snd_teleport_exit", SND_CHANNEL_ANY, 0, false, null);

            if (this.teleportKiller != -1) {
                // we got killed while being teleported
                Damage(gameLocal.entities[ this.teleportKiller], gameLocal.entities[ this.teleportKiller], getVec3_origin(), "damage_telefrag", 1.0f, INVALID_JOINT);
                this.teleportKiller = -1;
            } else {
                // kill anything that would have waited at teleport exit
                gameLocal.KillBox(this);
            }
            this.teleportEntity.oSet(null);
        }

        private void Event_HideTip() {
            HideTip();
        }

        private void Event_LevelTrigger() {
            final idStr mapName = new idStr(gameLocal.GetMapName());
            mapName.StripPath();
            mapName.StripFileExtension();
            for (int i = this.inventory.levelTriggers.Num() - 1; i >= 0; i--) {
                if (idStr.Icmp(mapName, this.inventory.levelTriggers.oGet(i).levelName) == 0) {
                    final idEntity ent = gameLocal.FindEntity(this.inventory.levelTriggers.oGet(i).triggerName);
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

            if (this.idealWeapon >= 0) {
                weapon = this.spawnArgs.GetString(va("def_weapon%d", this.idealWeapon));
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

    }
}
