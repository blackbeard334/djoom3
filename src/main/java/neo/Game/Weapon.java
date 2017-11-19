package neo.Game;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.AI.AI.idAI;
import neo.Game.Actor.idActor;

import static neo.Game.Actor.AI_AnimDone;
import static neo.Game.Actor.AI_GetBlendFrames;
import static neo.Game.Actor.AI_PlayAnim;
import static neo.Game.Actor.AI_PlayCycle;
import static neo.Game.Actor.AI_SetBlendFrames;
import static neo.Game.Animation.Anim.FRAME2MS;
import static neo.Game.Entity.EV_SetSkin;
import static neo.Game.Entity.EV_Touch;
import neo.Game.Entity.idAnimatedEntity;
import neo.Game.Entity.idEntity;
import static neo.Game.Entity.signalNum_t.SIG_TOUCH;
import neo.Game.Game.refSound_t;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t4;
import neo.Game.GameSys.Class.eventCallback_t5;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.g_debugWeapon;
import static neo.Game.GameSys.SysCvar.g_muzzleFlash;
import static neo.Game.GameSys.SysCvar.g_showBrass;
import static neo.Game.GameSys.SysCvar.g_showPlayerShadow;
import static neo.Game.GameSys.SysCvar.pm_thirdPerson;
import static neo.Game.Game_local.MASK_SHOT_RENDERMODEL;
import static neo.Game.Game_local.MAX_EVENT_PARAM_SIZE;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY2;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY3;
import static neo.Game.Game_local.gameState_t.GAMESTATE_SHUTDOWN;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Item.idMoveableItem;

import static neo.Game.Light.EV_Light_GetLightParm;
import static neo.Game.Light.EV_Light_SetLightParm;
import static neo.Game.Light.EV_Light_SetLightParms;
import static neo.Game.MultiplayerGame.gameType_t.GAME_TDM;
import static neo.Game.Player.ASYNC_PLAYER_INV_CLIP_BITS;
import static neo.Game.Player.BERSERK;
import static neo.Game.Player.INVISIBILITY;
import static neo.Game.Player.MELEE_DAMAGE;
import static neo.Game.Player.MELEE_DISTANCE;
import static neo.Game.Player.SPEED;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idDebris;
import neo.Game.Projectile.idProjectile;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Program.idScriptBool;
import neo.Game.Script.Script_Thread.idThread;
import neo.Game.Trigger.idTrigger;
import static neo.Game.Weapon.weaponStatus_t.WP_HOLSTERED;
import static neo.Game.Weapon.weaponStatus_t.WP_LOWERING;
import static neo.Game.Weapon.weaponStatus_t.WP_OUTOFAMMO;
import static neo.Game.Weapon.weaponStatus_t.WP_READY;
import static neo.Game.Weapon.weaponStatus_t.WP_RELOAD;
import static neo.Game.Weapon.weaponStatus_t.WP_RISING;
import static neo.Renderer.Material.CONTENTS_FLASHLIGHT_TRIGGER;
import static neo.Renderer.Material.CONTENTS_OPAQUE;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.surfTypes_t;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_NONE;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_DIVERSITY;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMESCALE;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.framework.DeclManager.declType_t.DECL_SKIN;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclSkin.idDeclSkin;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;
import static neo.ui.UserInterface.uiManager;

/**
 *
 */
public class Weapon {

    /*
     ===============================================================================

     Player Weapon
	
     ===============================================================================
     */
    public enum weaponStatus_t {

        WP_READY,
        WP_OUTOFAMMO,
        WP_RELOAD,
        WP_HOLSTERED,
        WP_RISING,
        WP_LOWERING
    };
    //    
    public static final int        AMMO_NUMTYPES               = 16;
    //
    public static final int        LIGHTID_WORLD_MUZZLE_FLASH  = 1;
    public static final int        LIGHTID_VIEW_MUZZLE_FLASH   = 100;
    //  
    //
    // event defs
    //
    public static final idEventDef EV_Weapon_Clear             = new idEventDef("<clear>");
    public static final idEventDef EV_Weapon_GetOwner          = new idEventDef("getOwner", null, 'e');
    public static final idEventDef EV_Weapon_Next              = new idEventDef("nextWeapon");
    public static final idEventDef EV_Weapon_State             = new idEventDef("weaponState", "sd");
    public static final idEventDef EV_Weapon_UseAmmo           = new idEventDef("useAmmo", "d");
    public static final idEventDef EV_Weapon_AddToClip         = new idEventDef("addToClip", "d");
    public static final idEventDef EV_Weapon_AmmoInClip        = new idEventDef("ammoInClip", null, 'f');
    public static final idEventDef EV_Weapon_AmmoAvailable     = new idEventDef("ammoAvailable", null, 'f');
    public static final idEventDef EV_Weapon_TotalAmmoCount    = new idEventDef("totalAmmoCount", null, 'f');
    public static final idEventDef EV_Weapon_ClipSize          = new idEventDef("clipSize", null, 'f');
    public static final idEventDef EV_Weapon_WeaponOutOfAmmo   = new idEventDef("weaponOutOfAmmo");
    public static final idEventDef EV_Weapon_WeaponReady       = new idEventDef("weaponReady");
    public static final idEventDef EV_Weapon_WeaponReloading   = new idEventDef("weaponReloading");
    public static final idEventDef EV_Weapon_WeaponHolstered   = new idEventDef("weaponHolstered");
    public static final idEventDef EV_Weapon_WeaponRising      = new idEventDef("weaponRising");
    public static final idEventDef EV_Weapon_WeaponLowering    = new idEventDef("weaponLowering");
    public static final idEventDef EV_Weapon_Flashlight        = new idEventDef("flashlight", "d");
    public static final idEventDef EV_Weapon_LaunchProjectiles = new idEventDef("launchProjectiles", "dffff");
    public static final idEventDef EV_Weapon_CreateProjectile  = new idEventDef("createProjectile", null, 'e');
    public static final idEventDef EV_Weapon_EjectBrass        = new idEventDef("ejectBrass");
    public static final idEventDef EV_Weapon_Melee             = new idEventDef("melee", null, 'd');
    public static final idEventDef EV_Weapon_GetWorldModel     = new idEventDef("getWorldModel", null, 'e');
    public static final idEventDef EV_Weapon_AllowDrop         = new idEventDef("allowDrop", "d");
    public static final idEventDef EV_Weapon_AutoReload        = new idEventDef("autoReload", null, 'f');
    public static final idEventDef EV_Weapon_NetReload         = new idEventDef("netReload");
    public static final idEventDef EV_Weapon_IsInvisible       = new idEventDef("isInvisible", null, 'f');
    public static final idEventDef EV_Weapon_NetEndReload      = new idEventDef("netEndReload");

    /* **********************************************************************

     idWeapon  
	
     ***********************************************************************/
    public static class idWeapon extends idAnimatedEntity {
        // CLASS_PROTOTYPE( idWeapon );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idAnimatedEntity.getEventCallBacks());
            eventCallbacks.put(EV_Weapon_Clear, (eventCallback_t0<idWeapon>) idWeapon::Event_Clear);
            eventCallbacks.put(EV_Weapon_GetOwner, (eventCallback_t0<idWeapon>) idWeapon::Event_GetOwner);
            eventCallbacks.put(EV_Weapon_State, (eventCallback_t2<idWeapon>) idWeapon::Event_WeaponState);
            eventCallbacks.put(EV_Weapon_WeaponReady, (eventCallback_t0<idWeapon>) idWeapon::Event_WeaponReady);
            eventCallbacks.put(EV_Weapon_WeaponOutOfAmmo, (eventCallback_t0<idWeapon>) idWeapon::Event_WeaponOutOfAmmo);
            eventCallbacks.put(EV_Weapon_WeaponReloading, (eventCallback_t0<idWeapon>) idWeapon::Event_WeaponReloading);
            eventCallbacks.put(EV_Weapon_WeaponHolstered, (eventCallback_t0<idWeapon>) idWeapon::Event_WeaponHolstered);
            eventCallbacks.put(EV_Weapon_WeaponRising, (eventCallback_t0<idWeapon>) idWeapon::Event_WeaponRising);
            eventCallbacks.put(EV_Weapon_WeaponLowering, (eventCallback_t0<idWeapon>) idWeapon::Event_WeaponLowering);
            eventCallbacks.put(EV_Weapon_UseAmmo, (eventCallback_t1<idWeapon>) idWeapon::Event_UseAmmo);
            eventCallbacks.put(EV_Weapon_AddToClip, (eventCallback_t1<idWeapon>) idWeapon::Event_AddToClip);
            eventCallbacks.put(EV_Weapon_AmmoInClip, (eventCallback_t0<idWeapon>) idWeapon::Event_AmmoInClip);
            eventCallbacks.put(EV_Weapon_AmmoAvailable, (eventCallback_t0<idWeapon>) idWeapon::Event_AmmoAvailable);
            eventCallbacks.put(EV_Weapon_TotalAmmoCount, (eventCallback_t0<idWeapon>) idWeapon::Event_TotalAmmoCount);
            eventCallbacks.put(EV_Weapon_ClipSize, (eventCallback_t0<idWeapon>) idWeapon::Event_ClipSize);
            eventCallbacks.put(AI_PlayAnim, (eventCallback_t2<idWeapon>) idWeapon::Event_PlayAnim);
            eventCallbacks.put(AI_PlayCycle, (eventCallback_t2<idWeapon>) idWeapon::Event_PlayCycle);
            eventCallbacks.put(AI_SetBlendFrames, (eventCallback_t2<idWeapon>) idWeapon::Event_SetBlendFrames);
            eventCallbacks.put(AI_GetBlendFrames, (eventCallback_t1<idWeapon>) idWeapon::Event_GetBlendFrames);
            eventCallbacks.put(AI_AnimDone, (eventCallback_t2<idWeapon>) idWeapon::Event_AnimDone);
            eventCallbacks.put(EV_Weapon_Next, (eventCallback_t0<idWeapon>) idWeapon::Event_Next);
            eventCallbacks.put(EV_SetSkin, (eventCallback_t1<idWeapon>) idWeapon::Event_SetSkin);
            eventCallbacks.put(EV_Weapon_Flashlight, (eventCallback_t1<idWeapon>) idWeapon::Event_Flashlight);
            eventCallbacks.put(EV_Light_GetLightParm, (eventCallback_t1<idWeapon>) idWeapon::Event_GetLightParm);
            eventCallbacks.put(EV_Light_SetLightParm, (eventCallback_t2<idWeapon>) idWeapon::Event_SetLightParm);
            eventCallbacks.put(EV_Light_SetLightParms, (eventCallback_t4<idWeapon>) idWeapon::Event_SetLightParms);
            eventCallbacks.put(EV_Weapon_LaunchProjectiles, (eventCallback_t5<idWeapon>) idWeapon::Event_LaunchProjectiles);
            eventCallbacks.put(EV_Weapon_CreateProjectile, (eventCallback_t0<idWeapon>) idWeapon::Event_CreateProjectile);
            eventCallbacks.put(EV_Weapon_EjectBrass, (eventCallback_t0<idWeapon>) idWeapon::Event_EjectBrass);
            eventCallbacks.put(EV_Weapon_Melee, (eventCallback_t0<idWeapon>) idWeapon::Event_Melee);
            eventCallbacks.put(EV_Weapon_GetWorldModel, (eventCallback_t0<idWeapon>) idWeapon::Event_GetWorldModel);
            eventCallbacks.put(EV_Weapon_AllowDrop, (eventCallback_t1<idWeapon>) idWeapon::Event_AllowDrop);
            eventCallbacks.put(EV_Weapon_AutoReload, (eventCallback_t0<idWeapon>) idWeapon::Event_AutoReload);
            eventCallbacks.put(EV_Weapon_NetReload, (eventCallback_t0<idWeapon>) idWeapon::Event_NetReload);
            eventCallbacks.put(EV_Weapon_IsInvisible, (eventCallback_t0<idWeapon>) idWeapon::Event_IsInvisible);
            eventCallbacks.put(EV_Weapon_NetEndReload, (eventCallback_t0<idWeapon>) idWeapon::Event_NetEndReload);
        }


        // script control
        private idScriptBool WEAPON_ATTACK       = new idScriptBool();
        private idScriptBool WEAPON_RELOAD       = new idScriptBool();
        private idScriptBool WEAPON_NETRELOAD    = new idScriptBool();
        private idScriptBool WEAPON_NETENDRELOAD = new idScriptBool();
        private idScriptBool WEAPON_NETFIRING    = new idScriptBool();
        private idScriptBool WEAPON_RAISEWEAPON  = new idScriptBool();
        private idScriptBool WEAPON_LOWERWEAPON  = new idScriptBool();
        private weaponStatus_t                status;
        private idThread                      thread;
        private idStr                         state;
        private idStr                         idealState;
        private int                           animBlendFrames;
        private int                           animDoneTime;
        private boolean                       isLinked;
        //
        // precreated projectile
        private idEntity                      projectileEnt;
        //
        private idPlayer                      owner;
        private idEntityPtr<idAnimatedEntity> worldModel;
        //
        // hiding (for GUIs and NPCs)
        private int                           hideTime;
        private float                         hideDistance;
        private int                           hideStartTime;
        private float                         hideStart;
        private float                         hideEnd;
        private float                         hideOffset;
        private boolean                       hide;
        private boolean                       disabled;
        //
        // berserk
        private int                           berserk;
        //
        // these are the player render view parms, which include bobbing
        private idVec3                        playerViewOrigin;
        private idMat3                        playerViewAxis;
        //
        // the view weapon render entity parms
        private idVec3                        viewWeaponOrigin;
        private idMat3                        viewWeaponAxis;
        //
        // the muzzle bone's position, used for launching projectiles and trailing smoke
        private idVec3                        muzzleOrigin;
        private idMat3                        muzzleAxis;
        //
        private idVec3                        pushVelocity;
        //
        // weapon definition
        // we maintain local copies of the projectile and brass dictionaries so they
        // do not have to be copied across the DLL boundary when entities are spawned
        private idDeclEntityDef               weaponDef;
        private idDeclEntityDef               meleeDef;
        private idDict                        projectileDict;
        private float                         meleeDistance;
        private idStr                         meleeDefName;
        private idDict                        brassDict;
        private int                           brassDelay;
        private idStr                         icon;
        //
        // view weapon gui light
        private renderLight_s                 guiLight;
        private int                           guiLightHandle;
        //
        // muzzle flash
        private renderLight_s                 muzzleFlash;          // positioned on view weapon bone
        private int                           muzzleFlashHandle;
        //
        private renderLight_s                 worldMuzzleFlash;     // positioned on world weapon bone
        private int                           worldMuzzleFlashHandle;
        //
        private idVec3                        flashColor;
        private int                           muzzleFlashEnd;
        private int                           flashTime;
        private boolean                       lightOn;
        private boolean                       silent_fire;
        private boolean                       allowDrop;
        //
        // effects
        private boolean                       hasBloodSplat;
        //
        // weapon kick
        private int                           kick_endtime;
        private int                           muzzle_kick_time;
        private int                           muzzle_kick_maxtime;
        private idAngles                      muzzle_kick_angles;
        private idVec3                        muzzle_kick_offset;
        //
        // ammo management
        private int /*ammo_t*/                ammoType;
        private int                           ammoRequired;         // amount of ammo to use each shot.  0 means weapon doesn't need ammo.
        private int                           clipSize;             // 0 means no reload
        private int                           ammoClip;
        private int                           lowAmmo;              // if ammo in clip hits this threshold, snd_
        private boolean                       powerAmmo;            // true if the clip reduction is a factor of the power setting when
        // a projectile is launched
        // mp client
        private boolean                       isFiring;
        //
        // zoom
        private int                           zoomFov;              // variable zoom fov per weapon
        //
        // joints from models
        private int /*jointHandle_t*/         barrelJointView;
        private int /*jointHandle_t*/         flashJointView;
        private int /*jointHandle_t*/         ejectJointView;
        private int /*jointHandle_t*/         guiLightJointView;
        private int /*jointHandle_t*/         ventLightJointView;
        //
        private int /*jointHandle_t*/         flashJointWorld;
        private int /*jointHandle_t*/         barrelJointWorld;
        private int /*jointHandle_t*/         ejectJointWorld;
        //
        // sound
        private idSoundShader                 sndHum;
        //
        // new style muzzle smokes
        private idDeclParticle                weaponSmoke;          // null if it doesn't smoke
        private int                           weaponSmokeStartTime; // set to gameLocal.time every weapon fire
        private boolean                       continuousSmoke;      // if smoke is continuous ( chainsaw )
        private idDeclParticle                strikeSmoke;          // striking something in melee
        private int                           strikeSmokeStartTime; // timing
        private idVec3                        strikePos;            // position of last melee strike
        private idMat3                        strikeAxis;           // axis of last melee strike
        private int                           nextStrikeFx;         // used for sound and decal ( may use for strike smoke too )
        //
        // nozzle effects
        private boolean                       nozzleFx;             // does this use nozzle effects ( parm5 at rest, parm6 firing )
        // this also assumes a nozzle light atm
        private int                           nozzleFxFade;         // time it takes to fade between the effects
        private int                           lastAttack;           // last time an attack occured
        private renderLight_s                 nozzleGlow;           // nozzle light
        private int                           nozzleGlowHandle;     // handle for nozzle light
        //
        private idVec3                        nozzleGlowColor;      // color of the nozzle glow
        private idMaterial                    nozzleGlowShader;     // shader for glow light
        private float                         nozzleGlowRadius;     // radius of glow light
        //
        // weighting for viewmodel angles
        private int                           weaponAngleOffsetAverages;
        private float                         weaponAngleOffsetScale;
        private float                         weaponAngleOffsetMax;
        private float                         weaponOffsetTime;
        private float                         weaponOffsetScale;
//
//

        /* **********************************************************************

         init

         ***********************************************************************/
        public idWeapon() {
            owner = null;
            worldModel = new idEntityPtr<>(null);
            weaponDef = null;
            thread = null;

            guiLight = new renderLight_s();//memset( &guiLight, 0, sizeof( guiLight ) );
            muzzleFlash = new renderLight_s();//memset( &muzzleFlash, 0, sizeof( muzzleFlash ) );
            worldMuzzleFlash = new renderLight_s();//memset( &worldMuzzleFlash, 0, sizeof( worldMuzzleFlash ) );
            nozzleGlow = new renderLight_s();//memset( &nozzleGlow, 0, sizeof( nozzleGlow ) );

            muzzleFlashEnd = 0;
            flashColor = getVec3_origin();
            muzzleFlashHandle = -1;
            worldMuzzleFlashHandle = -1;
            guiLightHandle = -1;
            nozzleGlowHandle = -1;
            modelDefHandle = -1;

            berserk = 2;
            brassDelay = 0;

            allowDrop = true;

            state = new idStr();
            idealState = new idStr();
            playerViewOrigin = new idVec3();
            playerViewAxis = new idMat3();
            viewWeaponOrigin = new idVec3();
            viewWeaponAxis = new idMat3();
            muzzleOrigin = new idVec3();
            muzzleAxis = new idMat3();
            pushVelocity = new idVec3();
            projectileDict = new idDict();
            meleeDefName = new idStr();
            brassDict = new idDict();
            icon = new idStr();
            muzzle_kick_angles = new idAngles();
            muzzle_kick_offset = new idVec3();
            strikePos = new idVec3();
            nozzleGlowColor = new idVec3();

            Clear();

            fl.networkSync = true;
        }
        // virtual					~idWeapon();

        // Init
        @Override
        public void Spawn() {
            super.Spawn();

            if (!gameLocal.isClient) {
                // setup the world model
                worldModel.oSet((idAnimatedEntity) gameLocal.SpawnEntityType(idAnimatedEntity.class, null));
                worldModel.GetEntity().fl.networkSync = true;
            }

            thread = new idThread();
            thread.ManualDelete();
            thread.ManualControl();
        }

        /*
         ================
         idWeapon::SetOwner

         Only called at player spawn time, not each weapon switch
         ================
         */
        public void SetOwner(idPlayer _owner) {
            assert (null == owner);
            owner = _owner;
            SetName(va("%s_weapon", owner.name));

            if (worldModel.GetEntity() != null) {
                worldModel.GetEntity().SetName(va("%s_weapon_worldmodel", owner.name));
            }
        }

        public idPlayer GetOwner() {
            return owner;
        }


        /*
         ================
         idWeapon::ShouldConstructScriptObjectAtSpawn

         Called during idEntity::Spawn to see if it should construct the script object or not.
         Overridden by subclasses that need to spawn the script object themselves.
         ================
         */
        public boolean ShouldfinalructScriptObjectAtSpawn() {
            return false;
        }

        public static void CacheWeapon(final String weaponName) {
            idDeclEntityDef weaponDef;
            String brassDefName;
            idStr clipModelName = new idStr();
            idTraceModel trm = new idTraceModel();
            String guiName;

            weaponDef = gameLocal.FindEntityDef(weaponName, false);
            if (null == weaponDef) {
                return;
            }

            // precache the brass collision model
            brassDefName = weaponDef.dict.GetString("def_ejectBrass");
            if (isNotNullOrEmpty(brassDefName)) {
                final idDeclEntityDef brassDef = gameLocal.FindEntityDef(brassDefName, false);
                if (brassDef != null) {
                    brassDef.dict.GetString("clipmodel", "", clipModelName);
                    if (!isNotNullOrEmpty(clipModelName)) {
                        clipModelName.oSet(brassDef.dict.GetString("model"));		// use the visual model
                    }
                    // load the trace model
                    CollisionModel_local.collisionModelManager.TrmFromModel(clipModelName, trm);
                }
            }

            guiName = weaponDef.dict.GetString("gui");
            if (isNotNullOrEmpty(guiName)) {
                uiManager.FindGui(guiName, true, false, true);
            }
        }

        // save games
        @Override
        public void Save(idSaveGame savefile) {					// archives object for save game file

            savefile.WriteInt(etoi(status));
            savefile.WriteObject(thread);
            savefile.WriteString(state);
            savefile.WriteString(idealState);
            savefile.WriteInt(animBlendFrames);
            savefile.WriteInt(animDoneTime);
            savefile.WriteBool(isLinked);

            savefile.WriteObject(owner);
            worldModel.Save(savefile);

            savefile.WriteInt(hideTime);
            savefile.WriteFloat(hideDistance);
            savefile.WriteInt(hideStartTime);
            savefile.WriteFloat(hideStart);
            savefile.WriteFloat(hideEnd);
            savefile.WriteFloat(hideOffset);
            savefile.WriteBool(hide);
            savefile.WriteBool(disabled);

            savefile.WriteInt(berserk);

            savefile.WriteVec3(playerViewOrigin);
            savefile.WriteMat3(playerViewAxis);

            savefile.WriteVec3(viewWeaponOrigin);
            savefile.WriteMat3(viewWeaponAxis);

            savefile.WriteVec3(muzzleOrigin);
            savefile.WriteMat3(muzzleAxis);

            savefile.WriteVec3(pushVelocity);

            savefile.WriteString(weaponDef.GetName());
            savefile.WriteFloat(meleeDistance);
            savefile.WriteString(meleeDefName);
            savefile.WriteInt(brassDelay);
            savefile.WriteString(icon);

            savefile.WriteInt(guiLightHandle);
            savefile.WriteRenderLight(guiLight);

            savefile.WriteInt(muzzleFlashHandle);
            savefile.WriteRenderLight(muzzleFlash);

            savefile.WriteInt(worldMuzzleFlashHandle);
            savefile.WriteRenderLight(worldMuzzleFlash);

            savefile.WriteVec3(flashColor);
            savefile.WriteInt(muzzleFlashEnd);
            savefile.WriteInt(flashTime);

            savefile.WriteBool(lightOn);
            savefile.WriteBool(silent_fire);

            savefile.WriteInt(kick_endtime);
            savefile.WriteInt(muzzle_kick_time);
            savefile.WriteInt(muzzle_kick_maxtime);
            savefile.WriteAngles(muzzle_kick_angles);
            savefile.WriteVec3(muzzle_kick_offset);

            savefile.WriteInt(ammoType);
            savefile.WriteInt(ammoRequired);
            savefile.WriteInt(clipSize);
            savefile.WriteInt(ammoClip);
            savefile.WriteInt(lowAmmo);
            savefile.WriteBool(powerAmmo);

            // savegames <= 17
            savefile.WriteInt(0);

            savefile.WriteInt(zoomFov);

            savefile.WriteJoint(barrelJointView);
            savefile.WriteJoint(flashJointView);
            savefile.WriteJoint(ejectJointView);
            savefile.WriteJoint(guiLightJointView);
            savefile.WriteJoint(ventLightJointView);

            savefile.WriteJoint(flashJointWorld);
            savefile.WriteJoint(barrelJointWorld);
            savefile.WriteJoint(ejectJointWorld);

            savefile.WriteBool(hasBloodSplat);

            savefile.WriteSoundShader(sndHum);

            savefile.WriteParticle(weaponSmoke);
            savefile.WriteInt(weaponSmokeStartTime);
            savefile.WriteBool(continuousSmoke);
            savefile.WriteParticle(strikeSmoke);
            savefile.WriteInt(strikeSmokeStartTime);
            savefile.WriteVec3(strikePos);
            savefile.WriteMat3(strikeAxis);
            savefile.WriteInt(nextStrikeFx);

            savefile.WriteBool(nozzleFx);
            savefile.WriteInt(nozzleFxFade);

            savefile.WriteInt(lastAttack);

            savefile.WriteInt(nozzleGlowHandle);
            savefile.WriteRenderLight(nozzleGlow);

            savefile.WriteVec3(nozzleGlowColor);
            savefile.WriteMaterial(nozzleGlowShader);
            savefile.WriteFloat(nozzleGlowRadius);

            savefile.WriteInt(weaponAngleOffsetAverages);
            savefile.WriteFloat(weaponAngleOffsetScale);
            savefile.WriteFloat(weaponAngleOffsetMax);
            savefile.WriteFloat(weaponOffsetTime);
            savefile.WriteFloat(weaponOffsetScale);

            savefile.WriteBool(allowDrop);
            savefile.WriteObject(projectileEnt);

        }

        @Override
        public void Restore(idRestoreGame savefile) {					// unarchives object from save game file

            status = weaponStatus_t.values()[savefile.ReadInt()];
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/thread);
            savefile.ReadString(state);
            savefile.ReadString(idealState);
            animBlendFrames = savefile.ReadInt();
            animDoneTime = savefile.ReadInt();
            isLinked = savefile.ReadBool();

            // Re-link script fields
            WEAPON_ATTACK.LinkTo(scriptObject, "WEAPON_ATTACK");
            WEAPON_RELOAD.LinkTo(scriptObject, "WEAPON_RELOAD");
            WEAPON_NETRELOAD.LinkTo(scriptObject, "WEAPON_NETRELOAD");
            WEAPON_NETENDRELOAD.LinkTo(scriptObject, "WEAPON_NETENDRELOAD");
            WEAPON_NETFIRING.LinkTo(scriptObject, "WEAPON_NETFIRING");
            WEAPON_RAISEWEAPON.LinkTo(scriptObject, "WEAPON_RAISEWEAPON");
            WEAPON_LOWERWEAPON.LinkTo(scriptObject, "WEAPON_LOWERWEAPON");

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/owner);
            worldModel.Restore(savefile);

            hideTime = savefile.ReadInt();
            hideDistance = savefile.ReadFloat();
            hideStartTime = savefile.ReadInt();
            hideStart = savefile.ReadFloat();
            hideEnd = savefile.ReadFloat();
            hideOffset = savefile.ReadFloat();
            hide = savefile.ReadBool();
            disabled = savefile.ReadBool();

            berserk = savefile.ReadInt();

            savefile.ReadVec3(playerViewOrigin);
            savefile.ReadMat3(playerViewAxis);

            savefile.ReadVec3(viewWeaponOrigin);
            savefile.ReadMat3(viewWeaponAxis);

            savefile.ReadVec3(muzzleOrigin);
            savefile.ReadMat3(muzzleAxis);

            savefile.ReadVec3(pushVelocity);

            idStr objectname = new idStr();
            savefile.ReadString(objectname);
            weaponDef = gameLocal.FindEntityDef(objectname.toString());
            meleeDef = gameLocal.FindEntityDef(weaponDef.dict.GetString("def_melee"), false);

            final idDeclEntityDef projectileDef = gameLocal.FindEntityDef(weaponDef.dict.GetString("def_projectile"), false);
            if (projectileDef != null) {
                projectileDict.oSet(projectileDef.dict);
            } else {
                projectileDict.Clear();
            }

            final idDeclEntityDef brassDef = gameLocal.FindEntityDef(weaponDef.dict.GetString("def_ejectBrass"), false);
            if (brassDef != null) {
                brassDict.oSet(brassDef.dict);
            } else {
                brassDict.Clear();
            }

            meleeDistance = savefile.ReadFloat();
            savefile.ReadString(meleeDefName);
            brassDelay = savefile.ReadInt();
            savefile.ReadString(icon);

            guiLightHandle = savefile.ReadInt();
            savefile.ReadRenderLight(guiLight);

            muzzleFlashHandle = savefile.ReadInt();
            savefile.ReadRenderLight(muzzleFlash);

            worldMuzzleFlashHandle = savefile.ReadInt();
            savefile.ReadRenderLight(worldMuzzleFlash);

            savefile.ReadVec3(flashColor);
            muzzleFlashEnd = savefile.ReadInt();
            flashTime = savefile.ReadInt();

            lightOn = savefile.ReadBool();
            silent_fire = savefile.ReadBool();

            kick_endtime = savefile.ReadInt();
            muzzle_kick_time = savefile.ReadInt();
            muzzle_kick_maxtime = savefile.ReadInt();
            savefile.ReadAngles(muzzle_kick_angles);
            savefile.ReadVec3(muzzle_kick_offset);

            ammoType = savefile.ReadInt();
            ammoRequired = savefile.ReadInt();
            clipSize = savefile.ReadInt();
            ammoClip = savefile.ReadInt();
            lowAmmo = savefile.ReadInt();
            powerAmmo = savefile.ReadBool();

            // savegame versions <= 17
            int foo;
            foo = savefile.ReadInt();

            zoomFov = savefile.ReadInt();

            barrelJointView = savefile.ReadJoint();
            flashJointView = savefile.ReadJoint();
            ejectJointView = savefile.ReadJoint();
            guiLightJointView = savefile.ReadJoint();
            ventLightJointView = savefile.ReadJoint();

            flashJointWorld = savefile.ReadJoint();
            barrelJointWorld = savefile.ReadJoint();
            ejectJointWorld = savefile.ReadJoint();

            hasBloodSplat = savefile.ReadBool();

            savefile.ReadSoundShader(sndHum);

            savefile.ReadParticle(weaponSmoke);
            weaponSmokeStartTime = savefile.ReadInt();
            continuousSmoke = savefile.ReadBool();
            savefile.ReadParticle(strikeSmoke);
            strikeSmokeStartTime = savefile.ReadInt();
            savefile.ReadVec3(strikePos);
            savefile.ReadMat3(strikeAxis);
            nextStrikeFx = savefile.ReadInt();

            nozzleFx = savefile.ReadBool();
            nozzleFxFade = savefile.ReadInt();

            lastAttack = savefile.ReadInt();

            nozzleGlowHandle = savefile.ReadInt();
            savefile.ReadRenderLight(nozzleGlow);

            savefile.ReadVec3(nozzleGlowColor);
            savefile.ReadMaterial(nozzleGlowShader);
            nozzleGlowRadius = savefile.ReadFloat();

            weaponAngleOffsetAverages = savefile.ReadInt();
            weaponAngleOffsetScale = savefile.ReadFloat();
            weaponAngleOffsetMax = savefile.ReadFloat();
            weaponOffsetTime = savefile.ReadFloat();
            weaponOffsetScale = savefile.ReadFloat();

            allowDrop = savefile.ReadBool();
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/projectileEnt);
        }

        /* **********************************************************************

         Weapon definition management

         ***********************************************************************/
        public void Clear() {
            CancelEvents(EV_Weapon_Clear);

            DeconstructScriptObject();
            scriptObject.Free();

            WEAPON_ATTACK.Unlink();
            WEAPON_RELOAD.Unlink();
            WEAPON_NETRELOAD.Unlink();
            WEAPON_NETENDRELOAD.Unlink();
            WEAPON_NETFIRING.Unlink();
            WEAPON_RAISEWEAPON.Unlink();
            WEAPON_LOWERWEAPON.Unlink();

            if (muzzleFlashHandle != -1) {
                gameRenderWorld.FreeLightDef(muzzleFlashHandle);
                muzzleFlashHandle = -1;
            }
            if (muzzleFlashHandle != -1) {
                gameRenderWorld.FreeLightDef(muzzleFlashHandle);
                muzzleFlashHandle = -1;
            }
            if (worldMuzzleFlashHandle != -1) {
                gameRenderWorld.FreeLightDef(worldMuzzleFlashHandle);
                worldMuzzleFlashHandle = -1;
            }
            if (guiLightHandle != -1) {
                gameRenderWorld.FreeLightDef(guiLightHandle);
                guiLightHandle = -1;
            }
            if (nozzleGlowHandle != -1) {
                gameRenderWorld.FreeLightDef(nozzleGlowHandle);
                nozzleGlowHandle = -1;
            }

//	memset( &renderEntity, 0, sizeof( renderEntity ) );
            renderEntity = new renderEntity_s();
            renderEntity.entityNum = entityNumber;

            renderEntity.noShadow = true;
            renderEntity.noSelfShadow = true;
            renderEntity.customSkin = null;

            // set default shader parms
            renderEntity.shaderParms[ SHADERPARM_RED] = 1.0f;
            renderEntity.shaderParms[ SHADERPARM_GREEN] = 1.0f;
            renderEntity.shaderParms[ SHADERPARM_BLUE] = 1.0f;
            renderEntity.shaderParms[3] = 1.0f;
            renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = 0.0f;
            renderEntity.shaderParms[5] = 0.0f;
            renderEntity.shaderParms[6] = 0.0f;
            renderEntity.shaderParms[7] = 0.0f;

            if (refSound.referenceSound != null) {
                refSound.referenceSound.Free(true);
            }
//	memset( &refSound, 0, sizeof( refSound_t ) );
            refSound = new refSound_t();

            // setting diversity to 0 results in no random sound.  -1 indicates random.
            refSound.diversity = -1.0f;

            if (owner != null) {
                // don't spatialize the weapon sounds
                refSound.listenerId = owner.GetListenerId();
            }

            // clear out the sounds from our spawnargs since we'll copy them from the weapon def
            idKeyValue kv = spawnArgs.MatchPrefix("snd_");
            while (kv != null) {
                spawnArgs.Delete(kv.GetKey());
                kv = spawnArgs.MatchPrefix("snd_");
            }

            hideTime = 300;
            hideDistance = -15.0f;
            hideStartTime = gameLocal.time - hideTime;
            hideStart = 0.0f;
            hideEnd = 0.0f;
            hideOffset = 0.0f;
            hide = false;
            disabled = false;

            weaponSmoke = null;
            weaponSmokeStartTime = 0;
            continuousSmoke = false;
            strikeSmoke = null;
            strikeSmokeStartTime = 0;
            strikePos.Zero();
            strikeAxis = getMat3_identity();
            nextStrikeFx = 0;

            icon.oSet("");

            playerViewAxis.Identity();
            playerViewOrigin.Zero();
            viewWeaponAxis.Identity();
            viewWeaponOrigin.Zero();
            muzzleAxis.Identity();
            muzzleOrigin.Zero();
            pushVelocity.Zero();

            status = WP_HOLSTERED;
            state.oSet("");
            idealState.oSet("");
            animBlendFrames = 0;
            animDoneTime = 0;

            projectileDict.Clear();
            meleeDef = null;
            meleeDefName.oSet("");
            meleeDistance = 0.0f;
            brassDict.Clear();

            flashTime = 250;
            lightOn = false;
            silent_fire = false;

            ammoType = 0;
            ammoRequired = 0;
            ammoClip = 0;
            clipSize = 0;
            lowAmmo = 0;
            powerAmmo = false;

            kick_endtime = 0;
            muzzle_kick_time = 0;
            muzzle_kick_maxtime = 0;
            muzzle_kick_angles.Zero();
            muzzle_kick_offset.Zero();

            zoomFov = 90;

            barrelJointView = INVALID_JOINT;
            flashJointView = INVALID_JOINT;
            ejectJointView = INVALID_JOINT;
            guiLightJointView = INVALID_JOINT;
            ventLightJointView = INVALID_JOINT;

            barrelJointWorld = INVALID_JOINT;
            flashJointWorld = INVALID_JOINT;
            ejectJointWorld = INVALID_JOINT;

            hasBloodSplat = false;
            nozzleFx = false;
            nozzleFxFade = 1500;
            lastAttack = 0;
            nozzleGlowHandle = -1;
            nozzleGlowShader = null;
            nozzleGlowRadius = 10;
            nozzleGlowColor.Zero();

            weaponAngleOffsetAverages = 0;
            weaponAngleOffsetScale = 0.0f;
            weaponAngleOffsetMax = 0.0f;
            weaponOffsetTime = 0.0f;
            weaponOffsetScale = 0.0f;

            allowDrop = true;

            animator.ClearAllAnims(gameLocal.time, 0);
            FreeModelDef();

            sndHum = null;

            isLinked = false;
            projectileEnt = null;

            isFiring = false;
        }

        public void GetWeaponDef(final String objectName, int ammoinclip) {
            String[] shader = {null};
            String[] objectType = {null};
            String vmodel;
            String guiName;
            String projectileName;
            String brassDefName;
            String smokeName;
            int ammoAvail;

            Clear();

            if (!isNotNullOrEmpty(objectName)) { //|| !objectname[ 0 ] ) {
                return;
            }

            assert (owner != null);

            weaponDef = gameLocal.FindEntityDef(objectName);

            ammoType = GetAmmoNumForName(weaponDef.dict.GetString("ammoType"));
            ammoRequired = weaponDef.dict.GetInt("ammoRequired");
            clipSize = weaponDef.dict.GetInt("clipSize");
            lowAmmo = weaponDef.dict.GetInt("lowAmmo");

            icon.oSet(weaponDef.dict.GetString("icon"));
            silent_fire = weaponDef.dict.GetBool("silent_fire");
            powerAmmo = weaponDef.dict.GetBool("powerAmmo");

            muzzle_kick_time = (int) SEC2MS(weaponDef.dict.GetFloat("muzzle_kick_time"));
            muzzle_kick_maxtime = (int) SEC2MS(weaponDef.dict.GetFloat("muzzle_kick_maxtime"));
            muzzle_kick_angles.oSet(weaponDef.dict.GetAngles("muzzle_kick_angles"));
            muzzle_kick_offset.oSet(weaponDef.dict.GetVector("muzzle_kick_offset"));

            hideTime = (int) SEC2MS(weaponDef.dict.GetFloat("hide_time", "0.3"));
            hideDistance = weaponDef.dict.GetFloat("hide_distance", "-15");

            // muzzle smoke
            smokeName = weaponDef.dict.GetString("smoke_muzzle");
            if (isNotNullOrEmpty(smokeName)) {
                weaponSmoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
            } else {
                weaponSmoke = null;
            }
            continuousSmoke = weaponDef.dict.GetBool("continuousSmoke");
            weaponSmokeStartTime = (continuousSmoke) ? gameLocal.time : 0;

            smokeName = weaponDef.dict.GetString("smoke_strike");
            if (isNotNullOrEmpty(smokeName)) {
                strikeSmoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
            } else {
                strikeSmoke = null;
            }
            strikeSmokeStartTime = 0;
            strikePos.Zero();
            strikeAxis = getMat3_identity();
            nextStrikeFx = 0;

            // setup gui light
            guiLight = new renderLight_s();//	memset( &guiLight, 0, sizeof( guiLight ) );
            final String guiLightShader = weaponDef.dict.GetString("mtr_guiLightShader");
            if (isNotNullOrEmpty(guiLightShader)) {
                guiLight.shader = declManager.FindMaterial(guiLightShader, false);
                guiLight.lightRadius.oSet(0, guiLight.lightRadius.oSet(1, guiLight.lightRadius.oSet(2, 3)));
                guiLight.pointLight = true;
            }

            // setup the view model
            vmodel = weaponDef.dict.GetString("model_view");
            SetModel(vmodel);

            // setup the world model
            InitWorldModel(weaponDef);

            // copy the sounds from the weapon view model def into out spawnargs
            idKeyValue kv = weaponDef.dict.MatchPrefix("snd_");
            while (kv != null) {
                spawnArgs.Set(kv.GetKey(), kv.GetValue());
                kv = weaponDef.dict.MatchPrefix("snd_", kv);
            }

            // find some joints in the model for locating effects
            barrelJointView = animator.GetJointHandle("barrel");
            flashJointView = animator.GetJointHandle("flash");
            ejectJointView = animator.GetJointHandle("eject");
            guiLightJointView = animator.GetJointHandle("guiLight");
            ventLightJointView = animator.GetJointHandle("ventLight");

            // get the projectile
            projectileDict.Clear();

            projectileName = weaponDef.dict.GetString("def_projectile");
            if (isNotNullOrEmpty(projectileName)) {
                final idDeclEntityDef projectileDef = gameLocal.FindEntityDef(projectileName, false);
                if (null == projectileDef) {
                    gameLocal.Warning("Unknown projectile '%s' in weapon '%s'", projectileName, objectName);
                } else {
                    final String spawnclass = projectileDef.dict.GetString("spawnclass");
                    if (!idProjectile.class.getSimpleName().equals(spawnclass)) {
                        gameLocal.Warning("Invalid spawnclass '%s' on projectile '%s' (used by weapon '%s')", spawnclass, projectileName, objectName);
                    } else {
                        projectileDict.oSet(projectileDef.dict);
                    }
                }
            }

            // set up muzzleflash render light
            idMaterial flashShader;
            idVec3 flashTarget;
            idVec3 flashUp;
            idVec3 flashRight;
            float flashRadius;
            boolean flashPointLight;

            weaponDef.dict.GetString("mtr_flashShader", "", shader);
            flashShader = declManager.FindMaterial(shader[0], false);
            flashPointLight = weaponDef.dict.GetBool("flashPointLight", "1");
            weaponDef.dict.GetVector("flashColor", "0 0 0", flashColor);
            flashRadius = (float) weaponDef.dict.GetInt("flashRadius");	// if 0, no light will spawn
            flashTime = (int) SEC2MS(weaponDef.dict.GetFloat("flashTime", "0.25"));
            flashTarget = weaponDef.dict.GetVector("flashTarget");
            flashUp = weaponDef.dict.GetVector("flashUp");
            flashRight = weaponDef.dict.GetVector("flashRight");

            muzzleFlash = new renderLight_s();//memset( & muzzleFlash, 0, sizeof(muzzleFlash));
            muzzleFlash.lightId = LIGHTID_VIEW_MUZZLE_FLASH + owner.entityNumber;
            muzzleFlash.allowLightInViewID = owner.entityNumber + 1;

            // the weapon lights will only be in first person
            guiLight.allowLightInViewID = owner.entityNumber + 1;
            nozzleGlow.allowLightInViewID = owner.entityNumber + 1;

            muzzleFlash.pointLight = flashPointLight;
            muzzleFlash.shader = flashShader;
            muzzleFlash.shaderParms[ SHADERPARM_RED] = flashColor.oGet(0);
            muzzleFlash.shaderParms[ SHADERPARM_GREEN] = flashColor.oGet(1);
            muzzleFlash.shaderParms[ SHADERPARM_BLUE] = flashColor.oGet(2);
            muzzleFlash.shaderParms[ SHADERPARM_TIMESCALE] = 1.0f;

            muzzleFlash.lightRadius.oSet(0, flashRadius);
            muzzleFlash.lightRadius.oSet(1, flashRadius);
            muzzleFlash.lightRadius.oSet(2, flashRadius);

            if (!flashPointLight) {
                muzzleFlash.target.oSet(flashTarget);
                muzzleFlash.up.oSet(flashUp);
                muzzleFlash.right.oSet(flashRight);
                muzzleFlash.end.oSet(flashTarget);
            }

            // the world muzzle flash is the same, just positioned differently
            worldMuzzleFlash = new renderLight_s(muzzleFlash);
            worldMuzzleFlash.suppressLightInViewID = owner.entityNumber + 1;
            worldMuzzleFlash.allowLightInViewID = 0;
            worldMuzzleFlash.lightId = LIGHTID_WORLD_MUZZLE_FLASH + owner.entityNumber;

            //-----------------------------------
            nozzleFx = weaponDef.dict.GetBool("nozzleFx");
            nozzleFxFade = weaponDef.dict.GetInt("nozzleFxFade", "1500");
            nozzleGlowColor.oSet(weaponDef.dict.GetVector("nozzleGlowColor", "1 1 1"));
            nozzleGlowRadius = weaponDef.dict.GetFloat("nozzleGlowRadius", "10");
            weaponDef.dict.GetString("mtr_nozzleGlowShader", "", shader);
            nozzleGlowShader = declManager.FindMaterial(shader[0], false);

            // get the melee damage def
            meleeDistance = weaponDef.dict.GetFloat("melee_distance");
            meleeDefName.oSet(weaponDef.dict.GetString("def_melee"));
            if (meleeDefName.Length() != 0) {
                meleeDef = gameLocal.FindEntityDef(meleeDefName.toString(), false);
                if (null == meleeDef) {
                    gameLocal.Error("Unknown melee '%s'", meleeDefName);
                }
            }

            // get the brass def
            brassDict.Clear();
            brassDelay = weaponDef.dict.GetInt("ejectBrassDelay", "0");
            brassDefName = weaponDef.dict.GetString("def_ejectBrass");

            if (isNotNullOrEmpty(brassDefName)) {
                final idDeclEntityDef brassDef = gameLocal.FindEntityDef(brassDefName, false);
                if (null == brassDef) {
                    gameLocal.Warning("Unknown brass '%s'", brassDefName);
                } else {
                    brassDict.oSet(brassDef.dict);
                }
            }

            if ((ammoType < 0) || (ammoType >= AMMO_NUMTYPES)) {
                gameLocal.Warning("Unknown ammotype in object '%s'", objectName);
            }

            ammoClip = ammoinclip;
            if ((ammoClip < 0) || (ammoClip > clipSize)) {
                // first time using this weapon so have it fully loaded to start
                ammoClip = clipSize;
                ammoAvail = owner.inventory.HasAmmo(ammoType, ammoRequired);
                if (ammoClip > ammoAvail) {
                    ammoClip = ammoAvail;
                }
            }

            renderEntity.gui[ 0] = null;
            guiName = weaponDef.dict.GetString("gui");
            if (isNotNullOrEmpty(guiName)) {
                renderEntity.gui[ 0] = uiManager.FindGui(guiName, true, false, true);
            }

            zoomFov = weaponDef.dict.GetInt("zoomFov", "70");
            berserk = weaponDef.dict.GetInt("berserk", "2");

            weaponAngleOffsetAverages = weaponDef.dict.GetInt("weaponAngleOffsetAverages", "10");
            weaponAngleOffsetScale = weaponDef.dict.GetFloat("weaponAngleOffsetScale", "0.25");
            weaponAngleOffsetMax = weaponDef.dict.GetFloat("weaponAngleOffsetMax", "10");

            weaponOffsetTime = weaponDef.dict.GetFloat("weaponOffsetTime", "400");
            weaponOffsetScale = weaponDef.dict.GetFloat("weaponOffsetScale", "0.005");

            if (!weaponDef.dict.GetString("weapon_scriptobject", null, objectType)) {
                gameLocal.Error("No 'weapon_scriptobject' set on '%s'.", objectName);
            }

            // setup script object
            if (!scriptObject.SetType(objectType[0])) {
                gameLocal.Error("Script object '%s' not found on weapon '%s'.", objectType[0], objectName);
            }

            WEAPON_ATTACK.LinkTo(scriptObject, "WEAPON_ATTACK");
            WEAPON_RELOAD.LinkTo(scriptObject, "WEAPON_RELOAD");
            WEAPON_NETRELOAD.LinkTo(scriptObject, "WEAPON_NETRELOAD");
            WEAPON_NETENDRELOAD.LinkTo(scriptObject, "WEAPON_NETENDRELOAD");
            WEAPON_NETFIRING.LinkTo(scriptObject, "WEAPON_NETFIRING");
            WEAPON_RAISEWEAPON.LinkTo(scriptObject, "WEAPON_RAISEWEAPON");
            WEAPON_LOWERWEAPON.LinkTo(scriptObject, "WEAPON_LOWERWEAPON");

            spawnArgs.oSet(weaponDef.dict);

            shader[0] = spawnArgs.GetString("snd_hum");
            if (isNotNullOrEmpty(shader[0])) {
                sndHum = declManager.FindSound(shader[0]);
                StartSoundShader(sndHum, SND_CHANNEL_BODY, 0, false, null);
            }

            isLinked = true;

            // call script object's constructor
            ConstructScriptObject();

            // make sure we have the correct skin
            UpdateSkin();
        }

        public boolean IsLinked() {
            return isLinked;
        }

        public boolean IsWorldModelReady() {
            return (worldModel.GetEntity() != null);
        }

        /* **********************************************************************

         GUIs

         ***********************************************************************/
        public String Icon() {
            return icon.toString();
        }

        public void UpdateGUI() {
            if (null == renderEntity.gui[ 0]) {
                return;
            }

            if (status == WP_HOLSTERED) {
                return;
            }

            if (owner.weaponGone) {
                // dropping weapons was implemented wierd, so we have to not update the gui when it happens or we'll get a negative ammo count
                return;
            }

            if (gameLocal.localClientNum != owner.entityNumber) {
                // if updating the hud for a followed client
                if (gameLocal.localClientNum >= 0 && gameLocal.entities[ gameLocal.localClientNum] != null && gameLocal.entities[ gameLocal.localClientNum].IsType(idPlayer.class)) {
                    idPlayer p = (idPlayer) gameLocal.entities[ gameLocal.localClientNum];
                    if (!p.spectating || p.spectator != owner.entityNumber) {
                        return;
                    }
                } else {
                    return;
                }
            }

            int inclip = AmmoInClip();
            int ammoamount = AmmoAvailable();

            if (ammoamount < 0) {
                // show infinite ammo
                renderEntity.gui[ 0].SetStateString("player_ammo", "");
            } else {
                // show remaining ammo
                renderEntity.gui[0].SetStateString("player_totalammo", va("%d", ammoamount - inclip));
                renderEntity.gui[0].SetStateString("player_ammo", ClipSize() != 0 ? va("%d", inclip) : "--");
                renderEntity.gui[0].SetStateString("player_clips", ClipSize() != 0 ? va("%d", ammoamount / ClipSize()) : "--");
                renderEntity.gui[0].SetStateString("player_allammo", va("%d/%d", inclip, ammoamount - inclip));
            }
            renderEntity.gui[0].SetStateBool("player_ammo_empty", (ammoamount == 0));
            renderEntity.gui[0].SetStateBool("player_clip_empty", (inclip == 0));
            renderEntity.gui[0].SetStateBool("player_clip_low", (inclip <= lowAmmo));
        }

        @Override
        public void SetModel(final String modelname) {
            assert (modelname != null);

            if (modelDefHandle >= 0) {
                gameRenderWorld.RemoveDecals(modelDefHandle);
            }

            renderEntity.hModel = animator.SetModel(modelname);
            if (renderEntity.hModel != null) {
                renderEntity.customSkin = animator.ModelDef().GetDefaultSkin();
                {
                    idJointMat[][] joints = {null};
                    renderEntity.numJoints = animator.GetJoints(joints);
                    renderEntity.joints = joints[0];
                }
            } else {
                renderEntity.customSkin = null;
                renderEntity.callback = null;
                renderEntity.numJoints = 0;
                renderEntity.joints = null;
            }

            // hide the model until an animation is played
            Hide();
        }

        /*
         ================
         idWeapon::GetGlobalJointTransform

         This returns the offset and axis of a weapon bone in world space, suitable for attaching models or lights
         ================
         */
        public boolean GetGlobalJointTransform(boolean viewModel, final int /*jointHandle_t*/ jointHandle, idVec3 offset, idMat3 axis) {
            if (viewModel) {
                // view model
                if (animator.GetJointTransform(jointHandle, gameLocal.time, offset, axis)) {
                    offset.oSet(offset.oMultiply(viewWeaponAxis).oPlus(viewWeaponOrigin));
                    axis.oSet(axis.oMultiply(viewWeaponAxis));
                    return true;
                }
            } else {
                // world model
                if (worldModel.GetEntity() != null && worldModel.GetEntity().GetAnimator().GetJointTransform(jointHandle, gameLocal.time, offset, axis)) {
                    offset.oSet(worldModel.GetEntity().GetPhysics().GetOrigin().oPlus(offset.oMultiply(worldModel.GetEntity().GetPhysics().GetAxis())));
                    axis.oSet(axis.oMultiply(worldModel.GetEntity().GetPhysics().GetAxis()));
                    return true;
                }
            }
            offset.oSet(viewWeaponOrigin);
            axis.oSet(viewWeaponAxis);
            return false;
        }

        public void SetPushVelocity(final idVec3 pushVelocity) {
            this.pushVelocity.oSet(pushVelocity);
        }

        public boolean UpdateSkin() {
            function_t func;

            if (!isLinked) {
                return false;
            }

            func = scriptObject.GetFunction("UpdateSkin");
            if (null == func) {
                common.Warning("Can't find function 'UpdateSkin' in object '%s'", scriptObject.GetTypeName());
                return false;
            }

            // use the frameCommandThread since it's safe to use outside of framecommands
            gameLocal.frameCommandThread.CallFunction(this, func, true);
            gameLocal.frameCommandThread.Execute();

            return true;
        }

        /* **********************************************************************

         State control/player interface

         ***********************************************************************/
        @Override
        public void Think() {
            // do nothing because the present is called from the player through PresentWeapon
        }

        public void Raise() {
            if (isLinked) {
                WEAPON_RAISEWEAPON._(true);
            }
        }

        public void PutAway() {
            hasBloodSplat = false;
            if (isLinked) {
                WEAPON_LOWERWEAPON._(true);
            }
        }

        /*
         ================
         idWeapon::Reload
         NOTE: this is only for impulse-triggered reload, auto reload is scripted
         ================
         */
        public void Reload() {
            if (isLinked) {
                WEAPON_RELOAD._(true);
            }
        }

        public void LowerWeapon() {
            if (!hide) {
                hideStart = 0.0f;
                hideEnd = hideDistance;
                if (gameLocal.time - hideStartTime < hideTime) {
                    hideStartTime = gameLocal.time - (hideTime - (gameLocal.time - hideStartTime));
                } else {
                    hideStartTime = gameLocal.time;
                }
                hide = true;
            }
        }

        public void RaiseWeapon() {
            Show();

            if (hide) {
                hideStart = hideDistance;
                hideEnd = 0.0f;
                if (gameLocal.time - hideStartTime < hideTime) {
                    hideStartTime = gameLocal.time - (hideTime - (gameLocal.time - hideStartTime));
                } else {
                    hideStartTime = gameLocal.time;
                }
                hide = false;
            }
        }

        public void HideWeapon() {
            Hide();
            if (worldModel.GetEntity() != null) {
                worldModel.GetEntity().Hide();
            }
            muzzleFlashEnd = 0;
        }

        public void ShowWeapon() {
            Show();
            if (worldModel.GetEntity() != null) {
                worldModel.GetEntity().Show();
            }
            if (lightOn) {
                MuzzleFlashLight();
            }
        }

        public void HideWorldModel() {
            if (worldModel.GetEntity() != null) {
                worldModel.GetEntity().Hide();
            }
        }

        public void ShowWorldModel() {
            if (worldModel.GetEntity() != null) {
                worldModel.GetEntity().Show();
            }
        }

        public void OwnerDied() {
            if (isLinked) {
                SetState("OwnerDied", 0);
                thread.Execute();
            }

            Hide();
            if (worldModel.GetEntity() != null) {
                worldModel.GetEntity().Hide();
            }

            // don't clear the weapon immediately since the owner might have killed himself by firing the weapon
            // within the current stack frame
            PostEventMS(EV_Weapon_Clear, 0);
        }

        public void BeginAttack() {
            if (status != WP_OUTOFAMMO) {
                lastAttack = gameLocal.time;
            }

            if (!isLinked) {
                return;
            }

            if (!WEAPON_ATTACK._()) {
                if (sndHum != null) {
                    StopSound(etoi(SND_CHANNEL_BODY), false);
                }
            }
            WEAPON_ATTACK._(true);
        }

        public void EndAttack() {
            if (!WEAPON_ATTACK.IsLinked()) {
                return;
            }
            if (WEAPON_ATTACK._()) {
                WEAPON_ATTACK._(false);
                if (sndHum != null) {
                    StartSoundShader(sndHum, SND_CHANNEL_BODY, 0, false, null);
                }
            }
        }

        public boolean IsReady() {
            return !hide && !IsHidden() && ((status == WP_RELOAD) || (status == WP_READY) || (status == WP_OUTOFAMMO));
        }

        public boolean IsReloading() {
            return (status == WP_RELOAD);
        }

        public boolean IsHolstered() {
            return (status == WP_HOLSTERED);
        }

        public boolean ShowCrosshair() {
            return !(state.equals(WP_RISING) || state.equals(WP_LOWERING) || state.equals(WP_HOLSTERED));
        }

        public idEntity DropItem(final idVec3 velocity, int activateDelay, int removeDelay, boolean died) {
            if (null == weaponDef || null == worldModel.GetEntity()) {
                return null;
            }
            if (!allowDrop) {
                return null;
            }
            final String classname = weaponDef.dict.GetString("def_dropItem");
            if (!isNotNullOrEmpty(classname)) {
                return null;
            }
            StopSound(etoi(SND_CHANNEL_BODY), true);
            StopSound(etoi(SND_CHANNEL_BODY3), true);

            return idMoveableItem.DropItem(classname, worldModel.GetEntity().GetPhysics().GetOrigin(), worldModel.GetEntity().GetPhysics().GetAxis(), velocity, activateDelay, removeDelay);
        }

        public boolean CanDrop() {
            if (null == weaponDef || null == worldModel.GetEntity()) {
                return false;
            }
            final String classname = weaponDef.dict.GetString("def_dropItem");
            if (!isNotNullOrEmpty(classname)) {
                return false;
            }
            return true;
        }

        public void WeaponStolen() {
            assert (!gameLocal.isClient);
            if (projectileEnt != null) {
                if (isLinked) {
                    SetState("WeaponStolen", 0);
                    thread.Execute();
                }
                projectileEnt = null;
            }

            // set to holstered so we can switch weapons right away
            status = WP_HOLSTERED;

            HideWeapon();
        }

        /* **********************************************************************

         Script state management

         ***********************************************************************/
        /*
         ================
         idWeapon::ConstructScriptObject

         Called during idEntity::Spawn.  Calls the constructor on the script object.
         Can be overridden by subclasses when a thread doesn't need to be allocated.
         ================
         */
        @Override
        public idThread ConstructScriptObject() {
            function_t constructor;

            thread.EndThread();

            // call script object's constructor
            constructor = scriptObject.GetConstructor();
            if (null == constructor) {
                gameLocal.Error("Missing constructor on '%s' for weapon", scriptObject.GetTypeName());
            }

            // init the script object's data
            scriptObject.ClearObject();
            thread.CallFunction(this, constructor, true);
            thread.Execute();

            return thread;
        }

        /*
         ================
         idWeapon::DeconstructScriptObject

         Called during idEntity::~idEntity.  Calls the destructor on the script object.
         Can be overridden by subclasses when a thread doesn't need to be allocated.
         Not called during idGameLocal::MapShutdown.
         ================
         */
        public void DefinalructScriptObject() {
            function_t destructor;

            if (NOT(thread)) {
                return;
            }

            // don't bother calling the script object's destructor on map shutdown
            if (gameLocal.GameState() == GAMESTATE_SHUTDOWN) {
                return;
            }

            thread.EndThread();

            // call script object's destructor
            destructor = scriptObject.GetDestructor();
            if (destructor != null) {
                // start a thread that will run immediately and end
                thread.CallFunction(this, destructor, true);
                thread.Execute();
                thread.EndThread();
            }

            // clear out the object's memory
            scriptObject.ClearObject();
        }

        public void SetState(final String statename, int blendFrames) {
            function_t func;

            if (!isLinked) {
                return;
            }

            func = scriptObject.GetFunction(statename);
            if (null == func) {
                assert (false);
                gameLocal.Error("Can't find function '%s' in object '%s'", statename, scriptObject.GetTypeName());
            }

            thread.CallFunction(this, func, true);
            state.oSet(statename);

            animBlendFrames = blendFrames;
            if (g_debugWeapon.GetBool()) {
                gameLocal.Printf("%d: weapon state : %s\n", gameLocal.time, statename);
            }

            idealState.oSet("");
        }

        public void UpdateScript() {
            int count;

            if (!isLinked) {
                return;
            }

            // only update the script on new frames
            if (!gameLocal.isNewFrame) {
                return;
            }

            if (idealState.Length() != 0) {
                SetState(idealState.toString(), animBlendFrames);
            }

            // update script state, which may call Event_LaunchProjectiles, among other things
            count = 10;
            while ((thread.Execute() || idealState.Length() != 0) && count-- != 0) {
                // happens for weapons with no clip (like grenades)
                if (idealState.Length() != 0) {
                    SetState(idealState.toString(), animBlendFrames);
                }
            }

            WEAPON_RELOAD._(false);
        }

        public void EnterCinematic() {
            StopSound(etoi(SND_CHANNEL_ANY), false);

            if (isLinked) {
                SetState("EnterCinematic", 0);
                thread.Execute();

                WEAPON_ATTACK._(false);
                WEAPON_RELOAD._(false);
                WEAPON_NETRELOAD._(false);
                WEAPON_NETENDRELOAD._(false);
                WEAPON_NETFIRING._(false);
                WEAPON_RAISEWEAPON._(false);
                WEAPON_LOWERWEAPON._(false);
            }

            disabled = true;

            LowerWeapon();
        }

        public void ExitCinematic() {
            disabled = false;

            if (isLinked) {
                SetState("ExitCinematic", 0);
                thread.Execute();
            }

            RaiseWeapon();
        }

        public void NetCatchup() {
            if (isLinked) {
                SetState("NetCatchup", 0);
                thread.Execute();
            }
        }

        /* **********************************************************************

         Visual presentation

         ***********************************************************************/
        public void PresentWeapon(boolean showViewModel) {
            playerViewOrigin.oSet(owner.firstPersonViewOrigin);
            playerViewAxis.oSet(owner.firstPersonViewAxis);

            // calculate weapon position based on player movement bobbing
            owner.CalculateViewWeaponPos(viewWeaponOrigin, viewWeaponAxis);

            // hide offset is for dropping the gun when approaching a GUI or NPC
            // This is simpler to manage than doing the weapon put-away animation
            if (gameLocal.time - hideStartTime < hideTime) {
                float frac = (float) (gameLocal.time - hideStartTime) / (float) hideTime;
                if (hideStart < hideEnd) {
                    frac = 1.0f - frac;
                    frac = 1.0f - frac * frac;
                } else {
                    frac = frac * frac;
                }
                hideOffset = hideStart + (hideEnd - hideStart) * frac;
            } else {
                hideOffset = hideEnd;
                if (hide && disabled) {
                    Hide();
                }
            }
            viewWeaponOrigin.oPluSet(viewWeaponAxis.oGet(2).oMultiply(hideOffset));

            // kick up based on repeat firing
            MuzzleRise(viewWeaponOrigin, viewWeaponAxis);

            // set the physics position and orientation
            GetPhysics().SetOrigin(viewWeaponOrigin);
            GetPhysics().SetAxis(viewWeaponAxis);
            UpdateVisuals();

            // update the weapon script
            UpdateScript();

            UpdateGUI();

            // update animation
            UpdateAnimation();

            // only show the surface in player view
            renderEntity.allowSurfaceInViewID = owner.entityNumber + 1;

            // crunch the depth range so it never pokes into walls this breaks the machine gun gui
            renderEntity.weaponDepthHack = true;

            // present the model
            if (showViewModel) {
                Present();
            } else {
                FreeModelDef();
            }

            if (worldModel.GetEntity() != null && worldModel.GetEntity().GetRenderEntity() != null) {
                // deal with the third-person visible world model
                // don't show shadows of the world model in first person
                if (gameLocal.isMultiplayer || g_showPlayerShadow.GetBool() || pm_thirdPerson.GetBool()) {
                    worldModel.GetEntity().GetRenderEntity().suppressShadowInViewID = 0;
                } else {
                    worldModel.GetEntity().GetRenderEntity().suppressShadowInViewID = owner.entityNumber + 1;
                    worldModel.GetEntity().GetRenderEntity().suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + owner.entityNumber;
                }
            }

            if (nozzleFx) {
                UpdateNozzleFx();
            }

            // muzzle smoke
            if (showViewModel && !disabled && weaponSmoke != null && (weaponSmokeStartTime != 0)) {
                // use the barrel joint if available
                if (barrelJointView != 0) {
                    GetGlobalJointTransform(true, barrelJointView, muzzleOrigin, muzzleAxis);
                } else {
                    // default to going straight out the view
                    muzzleOrigin.oSet(playerViewOrigin);
                    muzzleAxis.oSet(playerViewAxis);
                }
                // spit out a particle
                if (!gameLocal.smokeParticles.EmitSmoke(weaponSmoke, weaponSmokeStartTime, gameLocal.random.RandomFloat(), muzzleOrigin, muzzleAxis)) {
                    weaponSmokeStartTime = (continuousSmoke) ? gameLocal.time : 0;
                }
            }

            if (showViewModel && strikeSmoke != null && strikeSmokeStartTime != 0) {
                // spit out a particle
                if (!gameLocal.smokeParticles.EmitSmoke(strikeSmoke, strikeSmokeStartTime, gameLocal.random.RandomFloat(), strikePos, strikeAxis)) {
                    strikeSmokeStartTime = 0;
                }
            }

            // remove the muzzle flash light when it's done
            if ((!lightOn && (gameLocal.time >= muzzleFlashEnd)) || IsHidden()) {
                if (muzzleFlashHandle != -1) {
                    gameRenderWorld.FreeLightDef(muzzleFlashHandle);
                    muzzleFlashHandle = -1;
                }
                if (worldMuzzleFlashHandle != -1) {
                    gameRenderWorld.FreeLightDef(worldMuzzleFlashHandle);
                    worldMuzzleFlashHandle = -1;
                }
            }

            // update the muzzle flash light, so it moves with the gun
            if (muzzleFlashHandle != -1) {
                UpdateFlashPosition();
                gameRenderWorld.UpdateLightDef(muzzleFlashHandle, muzzleFlash);
                gameRenderWorld.UpdateLightDef(worldMuzzleFlashHandle, worldMuzzleFlash);

                // wake up monsters with the flashlight
                if (!gameLocal.isMultiplayer && lightOn && !owner.fl.notarget) {
                    AlertMonsters();
                }
            }

            // update the gui light
            if (guiLight.lightRadius.oGet(0) != 0 && guiLightJointView != INVALID_JOINT) {
                GetGlobalJointTransform(true, guiLightJointView, guiLight.origin, guiLight.axis);

                if ((guiLightHandle != -1)) {
                    gameRenderWorld.UpdateLightDef(guiLightHandle, guiLight);
                } else {
                    guiLightHandle = gameRenderWorld.AddLightDef(guiLight);
                }
            }

            if (status != WP_READY && sndHum != null) {
                StopSound(etoi(SND_CHANNEL_BODY), false);
            }

            UpdateSound();
        }

        public int GetZoomFov() {
            return zoomFov;
        }

        public void GetWeaponAngleOffsets(int[] average, float[] scale, float[] max) {
            average[0] = weaponAngleOffsetAverages;
            scale[0] = weaponAngleOffsetScale;
            max[0] = weaponAngleOffsetMax;
        }

        public void GetWeaponTimeOffsets(float[] time, float[] scale) {
            time[0] = weaponOffsetTime;
            scale[0] = weaponOffsetScale;
        }

        public boolean BloodSplat(float size) {
            float[] s = new float[1], c = new float[1];
            idMat3 localAxis = new idMat3(), axistemp = new idMat3();
            idVec3 localOrigin = new idVec3(), normal;

            if (hasBloodSplat) {
                return true;
            }

            hasBloodSplat = true;

            if (modelDefHandle < 0) {
                return false;
            }

            if (!GetGlobalJointTransform(true, ejectJointView, localOrigin, localAxis)) {
                return false;
            }

            localOrigin.oPluSet(0, gameLocal.random.RandomFloat() * -10.0f);
            localOrigin.oPluSet(1, gameLocal.random.RandomFloat() * 1.0f);
            localOrigin.oPluSet(2, gameLocal.random.RandomFloat() * -2.0f);

            normal = new idVec3(gameLocal.random.CRandomFloat(), -gameLocal.random.RandomFloat(), -1);
            normal.Normalize();

            idMath.SinCos16(gameLocal.random.RandomFloat() * idMath.TWO_PI, s, c);

            localAxis.oSet(2, normal.oNegative());
            localAxis.oGet(2).NormalVectors(axistemp.oGet(0), axistemp.oGet(1));
            localAxis.oSet(0, axistemp.oGet(0).oMultiply(c[0]).oPlus(axistemp.oGet(1).oMultiply(-s[0])));
            localAxis.oSet(1, axistemp.oGet(0).oMultiply(-s[0]).oPlus(axistemp.oGet(1).oMultiply(-c[0])));

            localAxis.oGet(0).oMulSet(1.0f / size);
            localAxis.oGet(1).oMulSet(1.0f / size);

            idPlane[] localPlane = new idPlane[2];

            localPlane[0].oSet(localAxis.oGet(0));//TODO:check what init value is you lazy arse.
            localPlane[0].oSet(3, -(localOrigin.oMultiply(localAxis.oGet(0))) + 0.5f);

            localPlane[1].oSet(localAxis.oGet(1));
            localPlane[1].oSet(3, -(localOrigin.oMultiply(localAxis.oGet(1))) + 0.5f);

            final idMaterial mtr = declManager.FindMaterial("textures/decals/duffysplatgun");

            gameRenderWorld.ProjectOverlay(modelDefHandle, localPlane, mtr);

            return true;
        }

        /* **********************************************************************

         Ammo

         ***********************************************************************/
        public static int /*ammo_t*/ GetAmmoNumForName(final String ammoname) {
            int[] num = new int[1];
            idDict ammoDict;

            assert (ammoname != null);

            ammoDict = gameLocal.FindEntityDefDict("ammo_types", false);
            if (null == ammoDict) {
                gameLocal.Error("Could not find entity definition for 'ammo_types'\n");
            }

            if (!isNotNullOrEmpty(ammoname)) {
                return 0;
            }

            if (!ammoDict.GetInt(ammoname, "-1", num)) {
                gameLocal.Error("Unknown ammo type '%s'", ammoname);
            }

            if ((num[0] < 0) || (num[0] >= AMMO_NUMTYPES)) {
                gameLocal.Error("Ammo type '%s' value out of range.  Maximum ammo types is %d.\n", ammoname, AMMO_NUMTYPES);
            }

            return num[0];
        }

        public static String GetAmmoNameForNum(int /*ammo_t*/ ammonum) {
            int i;
            int num;
            idDict ammoDict;
            idKeyValue kv;
//	char []text = new char[32 ];
            String text;

            ammoDict = gameLocal.FindEntityDefDict("ammo_types", false);
            if (null == ammoDict) {
                gameLocal.Error("Could not find entity definition for 'ammo_types'\n");
            }

            text = String.format("%d", ammonum);

            num = ammoDict.GetNumKeyVals();
            for (i = 0; i < num; i++) {
                kv = ammoDict.GetKeyVal(i);
                if (kv.GetValue().equals(text)) {
                    return kv.GetKey().toString();
                }
            }

            return null;
        }

        public static String GetAmmoPickupNameForNum(int /*ammo_t*/ ammonum) {
            int i;
            int num;
            idDict ammoDict;
            idKeyValue kv;

            ammoDict = gameLocal.FindEntityDefDict("ammo_names", false);
            if (null == ammoDict) {
                gameLocal.Error("Could not find entity definition for 'ammo_names'\n");
            }

            final String name = GetAmmoNameForNum(ammonum);

            if (isNotNullOrEmpty(name)) {
                num = ammoDict.GetNumKeyVals();
                for (i = 0; i < num; i++) {
                    kv = ammoDict.GetKeyVal(i);
                    if (idStr.Icmp(kv.GetKey().toString(), name) == 0) {
                        return kv.GetValue().toString();
                    }
                }
            }

            return "";
        }

        public int /*ammo_t*/ GetAmmoType() {
            return ammoType;
        }

        public int AmmoAvailable() {
            if (owner != null) {
                return owner.inventory.HasAmmo(ammoType, ammoRequired);
            } else {
                return 0;
            }
        }

        public int AmmoInClip() {
            return ammoClip;
        }

        public void ResetAmmoClip() {
            ammoClip = -1;
        }

        public int ClipSize() {
            return clipSize;
        }

        public int LowAmmo() {
            return lowAmmo;
        }

        public int AmmoRequired() {
            return ammoRequired;
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteBits(ammoClip, ASYNC_PLAYER_INV_CLIP_BITS);
            msg.WriteBits(worldModel.GetSpawnId(), 32);
            msg.WriteBits(btoi(lightOn), 1);
            msg.WriteBits(isFiring ? 1 : 0, 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            ammoClip = msg.ReadBits(ASYNC_PLAYER_INV_CLIP_BITS);
            worldModel.SetSpawnId(msg.ReadBits(32));
            boolean snapLight = msg.ReadBits(1) != 0;
            isFiring = msg.ReadBits(1) != 0;

            // WEAPON_NETFIRING is only turned on for other clients we're predicting. not for local client
            if (owner != null && gameLocal.localClientNum != owner.entityNumber && WEAPON_NETFIRING.IsLinked()) {

                // immediately go to the firing state so we don't skip fire animations
                if (!WEAPON_NETFIRING._() && isFiring) {
                    idealState.oSet("Fire");
                }

                // immediately switch back to idle
                if (WEAPON_NETFIRING._() && !isFiring) {
                    idealState.oSet("Idle");
                }

                WEAPON_NETFIRING._(isFiring);
            }

            if (snapLight != lightOn) {
                Reload();
            }
        }
        // enum {
        public static final int EVENT_RELOAD = idEntity.EVENT_MAXEVENTS;
        public static final int EVENT_ENDRELOAD = EVENT_RELOAD + 1;
        public static final int EVENT_CHANGESKIN = EVENT_RELOAD + 2;
        public static final int EVENT_MAXEVENTS = EVENT_RELOAD + 3;
        // };

        @Override
        public boolean ClientReceiveEvent(int event, int time, final idBitMsg msg) {

            switch (event) {
                case EVENT_RELOAD: {
                    if (gameLocal.time - time < 1000) {
                        if (WEAPON_NETRELOAD.IsLinked()) {
                            WEAPON_NETRELOAD._(true);
                            WEAPON_NETENDRELOAD._(false);
                        }
                    }
                    return true;
                }
                case EVENT_ENDRELOAD: {
                    if (WEAPON_NETENDRELOAD.IsLinked()) {
                        WEAPON_NETENDRELOAD._(true);
                    }
                    return true;
                }
                case EVENT_CHANGESKIN: {
                    int index = gameLocal.ClientRemapDecl(DECL_SKIN, msg.ReadLong());
                    renderEntity.customSkin = (index != -1) ? (idDeclSkin) declManager.DeclByIndex(DECL_SKIN, index) : null;
                    UpdateVisuals();
                    if (worldModel.GetEntity() != null) {
                        worldModel.GetEntity().SetSkin(renderEntity.customSkin);
                    }
                    return true;
                }
                default: {
                    return super.ClientReceiveEvent(event, time, msg);
                }
            }
//            return false;
        }

        @Override
        public void ClientPredictionThink() {
            UpdateAnimation();
        }

        // flashlight
        private void AlertMonsters() {
            trace_s[] tr = {null};
            idEntity ent;
            idVec3 end = muzzleFlash.origin.oPlus(muzzleFlash.axis.oMultiply(muzzleFlash.target));

            gameLocal.clip.TracePoint(tr, muzzleFlash.origin, end, CONTENTS_OPAQUE | MASK_SHOT_RENDERMODEL | CONTENTS_FLASHLIGHT_TRIGGER, owner);
            if (g_debugWeapon.GetBool()) {
                gameRenderWorld.DebugLine(colorYellow, muzzleFlash.origin, end, 0);
                gameRenderWorld.DebugArrow(colorGreen, muzzleFlash.origin, tr[0].endpos, 2, 0);
            }

            if (tr[0].fraction < 1.0f) {
                ent = gameLocal.GetTraceEntity(tr[0]);
                if (ent.IsType(idAI.class)) {
                    ((idAI) ent).TouchedByFlashlight(owner);
                } else if (ent.IsType(idTrigger.class)) {
                    ent.Signal(SIG_TOUCH);
                    ent.ProcessEvent(EV_Touch, owner, tr[0]);
                }
            }

            // jitter the trace to try to catch cases where a trace down the center doesn't hit the monster
            end.oPluSet(muzzleFlash.axis.oMultiply(muzzleFlash.right.oMultiply(idMath.Sin16(MS2SEC(gameLocal.time) * 31.34f))));
            end.oPluSet(muzzleFlash.axis.oMultiply(muzzleFlash.up.oMultiply(idMath.Sin16(MS2SEC(gameLocal.time) * 12.17f))));
            gameLocal.clip.TracePoint(tr, muzzleFlash.origin, end, CONTENTS_OPAQUE | MASK_SHOT_RENDERMODEL | CONTENTS_FLASHLIGHT_TRIGGER, owner);
            if (g_debugWeapon.GetBool()) {
                gameRenderWorld.DebugLine(colorYellow, muzzleFlash.origin, end, 0);
                gameRenderWorld.DebugArrow(colorGreen, muzzleFlash.origin, tr[0].endpos, 2, 0);
            }

            if (tr[0].fraction < 1.0f) {
                ent = gameLocal.GetTraceEntity(tr[0]);
                if (ent.IsType(idAI.class)) {
                    ((idAI) ent).TouchedByFlashlight(owner);
                } else if (ent.IsType(idTrigger.class)) {
                    ent.Signal(SIG_TOUCH);
                    ent.ProcessEvent(EV_Touch, owner, tr[0]);
                }
            }
        }

        // Visual presentation
        private void InitWorldModel(final idDeclEntityDef def) {
            idEntity ent;

            ent = worldModel.GetEntity();

            assert (ent != null);
            assert (def != null);

            final String model = def.dict.GetString("model_world");
            final String attach = def.dict.GetString("joint_attach");

            ent.SetSkin(null);
            if (isNotNullOrEmpty(model)) {
                ent.Show();
                ent.SetModel(model);
                if (ent.GetAnimator().ModelDef() != null) {
                    ent.SetSkin(ent.GetAnimator().ModelDef().GetDefaultSkin());
                }
                ent.GetPhysics().SetContents(0);
                ent.GetPhysics().SetClipModel(null, 1.0f);
                ent.BindToJoint(owner, attach, true);
                ent.GetPhysics().SetOrigin(getVec3_origin());
                ent.GetPhysics().SetAxis(getMat3_identity());

                // supress model in player views, but allow it in mirrors and remote views
                renderEntity_s worldModelRenderEntity = ent.GetRenderEntity();
                if (worldModelRenderEntity != null) {
                    worldModelRenderEntity.suppressSurfaceInViewID = owner.entityNumber + 1;
                    worldModelRenderEntity.suppressShadowInViewID = owner.entityNumber + 1;
                    worldModelRenderEntity.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + owner.entityNumber;
                }
            } else {
                ent.SetModel("");
                ent.Hide();
            }

            flashJointWorld = ent.GetAnimator().GetJointHandle("flash");
            barrelJointWorld = ent.GetAnimator().GetJointHandle("muzzle");
            ejectJointWorld = ent.GetAnimator().GetJointHandle("eject");
        }

        private void MuzzleFlashLight() {

            if (!lightOn && (!g_muzzleFlash.GetBool() || 0 == muzzleFlash.lightRadius.oGet(0))) {
                return;
            }

            if (flashJointView == INVALID_JOINT) {
                return;
            }

            UpdateFlashPosition();

            // these will be different each fire
            muzzleFlash.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            muzzleFlash.shaderParms[ SHADERPARM_DIVERSITY] = renderEntity.shaderParms[SHADERPARM_DIVERSITY];

            worldMuzzleFlash.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            worldMuzzleFlash.shaderParms[ SHADERPARM_DIVERSITY] = renderEntity.shaderParms[SHADERPARM_DIVERSITY];

            // the light will be removed at this time
            muzzleFlashEnd = gameLocal.time + flashTime;

            if (muzzleFlashHandle != -1) {
                gameRenderWorld.UpdateLightDef(muzzleFlashHandle, muzzleFlash);
                gameRenderWorld.UpdateLightDef(worldMuzzleFlashHandle, worldMuzzleFlash);
            } else {
                muzzleFlashHandle = gameRenderWorld.AddLightDef(muzzleFlash);
                worldMuzzleFlashHandle = gameRenderWorld.AddLightDef(worldMuzzleFlash);
            }
        }

        /*
         ================
         idWeapon::MuzzleRise

         The machinegun and chaingun will incrementally back up as they are being fired
         ================
         */
        private void MuzzleRise(idVec3 origin, idMat3 axis) {
            int time;
            float amount;
            idAngles ang;
            idVec3 offset;

            time = kick_endtime - gameLocal.time;
            if (time <= 0) {
                return;
            }

            if (muzzle_kick_maxtime <= 0) {
                return;
            }

            if (time > muzzle_kick_maxtime) {
                time = muzzle_kick_maxtime;
            }

            amount = (float) time / (float) muzzle_kick_maxtime;
            ang = muzzle_kick_angles.oMultiply(amount);
            offset = muzzle_kick_offset.oMultiply(amount);

            origin.oSet(origin.oMinus(axis.oMultiply(offset)));
            axis.oSet(ang.ToMat3().oMultiply(axis));
        }

        private void UpdateNozzleFx() {
            if (!nozzleFx) {
                return;
            }

            //
            // shader parms
            //
            int la = gameLocal.time - lastAttack + 1;
            float s = 1.0f;
            float l = 0.0f;
            if (la < nozzleFxFade) {
                s = ((float) la / nozzleFxFade);
                l = 1.0f - s;
            }
            renderEntity.shaderParms[5] = s;
            renderEntity.shaderParms[6] = l;

            if (ventLightJointView == INVALID_JOINT) {
                return;
            }

            //
            // vent light
            //
            if (nozzleGlowHandle == -1) {
//		memset(&nozzleGlow, 0, sizeof(nozzleGlow));
                nozzleGlow = new renderLight_s();
                if (owner != null) {
                    nozzleGlow.allowLightInViewID = owner.entityNumber + 1;
                }
                nozzleGlow.pointLight = true;
                nozzleGlow.noShadows = true;
                nozzleGlow.lightRadius.x = nozzleGlowRadius;
                nozzleGlow.lightRadius.y = nozzleGlowRadius;
                nozzleGlow.lightRadius.z = nozzleGlowRadius;
                nozzleGlow.shader = nozzleGlowShader;
                nozzleGlow.shaderParms[ SHADERPARM_TIMESCALE] = 1.0f;
                nozzleGlow.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                GetGlobalJointTransform(true, ventLightJointView, nozzleGlow.origin, nozzleGlow.axis);
                nozzleGlowHandle = gameRenderWorld.AddLightDef(nozzleGlow);
            }

            GetGlobalJointTransform(true, ventLightJointView, nozzleGlow.origin, nozzleGlow.axis);

            nozzleGlow.shaderParms[ SHADERPARM_RED] = nozzleGlowColor.x * s;
            nozzleGlow.shaderParms[ SHADERPARM_GREEN] = nozzleGlowColor.y * s;
            nozzleGlow.shaderParms[ SHADERPARM_BLUE] = nozzleGlowColor.z * s;
            gameRenderWorld.UpdateLightDef(nozzleGlowHandle, nozzleGlow);
        }

        private void UpdateFlashPosition() {
            // the flash has an explicit joint for locating it
            GetGlobalJointTransform(true, flashJointView, muzzleFlash.origin, muzzleFlash.axis);

            // if the desired point is inside or very close to a wall, back it up until it is clear
            idVec3 start = muzzleFlash.origin.oMinus(playerViewAxis.oGet(0).oMultiply(16));
            idVec3 end = muzzleFlash.origin.oPlus(playerViewAxis.oGet(0).oMultiply(8));
            trace_s[] tr = {null};
            gameLocal.clip.TracePoint(tr, start, end, MASK_SHOT_RENDERMODEL, owner);
            // be at least 8 units away from a solid
            muzzleFlash.origin = tr[0].endpos.oMinus(playerViewAxis.oGet(0).oMultiply(8));

            // put the world muzzle flash on the end of the joint, no matter what
            GetGlobalJointTransform(false, flashJointWorld, worldMuzzleFlash.origin, worldMuzzleFlash.axis);
        }

        /* **********************************************************************

         Script events

         ***********************************************************************/
        private void Event_Clear() {
            Clear();
        }

        private void Event_GetOwner() {
            idThread.ReturnEntity(owner);
        }

        private void Event_WeaponState(final idEventArg<String> _statename, idEventArg<Integer> blendFrames) {
            String statename = _statename.value;
            function_t func;

            func = scriptObject.GetFunction(statename);
            if (null == func) {
                assert (false);
                gameLocal.Error("Can't find function '%s' in object '%s'", statename, scriptObject.GetTypeName());
            }

            idealState.oSet(statename);

            if (0 == idealState.Icmp("Fire")) {
                isFiring = true;
            } else {
                isFiring = false;
            }

            animBlendFrames = blendFrames.value;
            thread.DoneProcessing();
        }
//
//        private void Event_SetWeaponStatus(float newStatus);
//

        private void Event_WeaponReady() {
            status = WP_READY;
            if (isLinked) {
                WEAPON_RAISEWEAPON._(false);
            }
            if (sndHum != null) {
                StartSoundShader(sndHum, SND_CHANNEL_BODY, 0, false, null);
            }

        }

        private void Event_WeaponOutOfAmmo() {
            status = WP_OUTOFAMMO;
            if (isLinked) {
                WEAPON_RAISEWEAPON._(false);
            }
        }

        private void Event_WeaponReloading() {
            status = WP_RELOAD;
        }

        private void Event_WeaponHolstered() {
            status = WP_HOLSTERED;
            if (isLinked) {
                WEAPON_LOWERWEAPON._(false);
            }
        }

        private void Event_WeaponRising() {
            status = WP_RISING;
            if (isLinked) {
                WEAPON_LOWERWEAPON._(false);
            }
            owner.WeaponRisingCallback();
        }

        private void Event_WeaponLowering() {
            status = WP_LOWERING;
            if (isLinked) {
                WEAPON_RAISEWEAPON._(false);
            }
            owner.WeaponLoweringCallback();
        }

        private void Event_UseAmmo(idEventArg<Integer> _amount) {
            int amount = _amount.value;
            if (gameLocal.isClient) {
                return;
            }

            owner.inventory.UseAmmo(ammoType, (powerAmmo) ? amount : (amount * ammoRequired));
            if (clipSize != 0 && ammoRequired != 0) {
                ammoClip -= powerAmmo ? amount : (amount * ammoRequired);
                if (ammoClip < 0) {
                    ammoClip = 0;
                }
            }
        }

        private void Event_AddToClip(idEventArg<Integer> amount) {
            int ammoAvail;

            if (gameLocal.isClient) {
                return;
            }

            ammoClip += amount.value;
            if (ammoClip > clipSize) {
                ammoClip = clipSize;
            }

            ammoAvail = owner.inventory.HasAmmo(ammoType, ammoRequired);
            if (ammoClip > ammoAvail) {
                ammoClip = ammoAvail;
            }
        }

        private void Event_AmmoInClip() {
            int ammo = AmmoInClip();
            idThread.ReturnFloat(ammo);
        }

        private void Event_AmmoAvailable() {
            int ammoAvail = owner.inventory.HasAmmo(ammoType, ammoRequired);
            idThread.ReturnFloat(ammoAvail);
        }

        private void Event_TotalAmmoCount() {
            int ammoAvail = owner.inventory.HasAmmo(ammoType, 1);
            idThread.ReturnFloat(ammoAvail);
        }

        private void Event_ClipSize() {
            idThread.ReturnFloat(clipSize);
        }

        private void Event_PlayAnim(idEventArg<Integer> _channel, final idEventArg<String> _animname) {
            int channel = _channel.value;
            String animname = _animname.value;
            int anim;

            anim = animator.GetAnim(animname);
            if (0 == anim) {
                gameLocal.Warning("missing '%s' animation on '%s' (%s)", animname, name, GetEntityDefName());
                animator.Clear(channel, gameLocal.time, FRAME2MS(animBlendFrames));
                animDoneTime = 0;
            } else {
                if (!(owner != null && owner.GetInfluenceLevel() != 0)) {
                    Show();
                }
                animator.PlayAnim(channel, anim, gameLocal.time, FRAME2MS(animBlendFrames));
                animDoneTime = animator.CurrentAnim(channel).GetEndTime();
                if (worldModel.GetEntity() != null) {
                    anim = worldModel.GetEntity().GetAnimator().GetAnim(animname);
                    if (anim != 0) {
                        worldModel.GetEntity().GetAnimator().PlayAnim(channel, anim, gameLocal.time, FRAME2MS(animBlendFrames));
                    }
                }
            }
            animBlendFrames = 0;
            idThread.ReturnInt(0);
        }

        private void Event_PlayCycle(idEventArg<Integer> _channel, final idEventArg<String> _animname) {
            int channel = _channel.value;
            String animname = _animname.value;
            int anim;

            anim = animator.GetAnim(animname);
            if (0 == anim) {
                gameLocal.Warning("missing '%s' animation on '%s' (%s)", animname, name, GetEntityDefName());
                animator.Clear(channel, gameLocal.time, FRAME2MS(animBlendFrames));
                animDoneTime = 0;
            } else {
                if (!(owner != null && owner.GetInfluenceLevel() != 0)) {
                    Show();
                }
                animator.CycleAnim(channel, anim, gameLocal.time, FRAME2MS(animBlendFrames));
                animDoneTime = animator.CurrentAnim(channel).GetEndTime();
                if (worldModel.GetEntity() != null) {
                    anim = worldModel.GetEntity().GetAnimator().GetAnim(animname);
                    worldModel.GetEntity().GetAnimator().CycleAnim(channel, anim, gameLocal.time, FRAME2MS(animBlendFrames));
                }
            }
            animBlendFrames = 0;
            idThread.ReturnInt(0);
        }

        private void Event_AnimDone(idEventArg<Integer> channel, idEventArg<Integer> blendFrames) {
            if (animDoneTime - FRAME2MS(blendFrames.value) <= gameLocal.time) {
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        private void Event_SetBlendFrames(idEventArg<Integer> channel, idEventArg<Integer> blendFrames) {
            animBlendFrames = blendFrames.value;
        }

        private void Event_GetBlendFrames(idEventArg<Integer> channel) {
            idThread.ReturnInt(animBlendFrames);
        }

        private void Event_Next() {
            // change to another weapon if possible
            owner.NextBestWeapon();
        }

        private void Event_SetSkin(final idEventArg<String> _skinname) {
            String skinname = _skinname.value;
            idDeclSkin skinDecl;

            if (!isNotNullOrEmpty(skinname)) {
                skinDecl = null;
            } else {
                skinDecl = declManager.FindSkin(skinname);
            }

            renderEntity.customSkin = skinDecl;
            UpdateVisuals();

            if (worldModel.GetEntity() != null) {
                worldModel.GetEntity().SetSkin(skinDecl);
            }

            if (gameLocal.isServer) {
                idBitMsg msg = new idBitMsg();
                ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.WriteLong((skinDecl != null) ? gameLocal.ServerRemapDecl(-1, DECL_SKIN, skinDecl.Index()) : -1);
                ServerSendEvent(EVENT_CHANGESKIN, msg, false, -1);
            }
        }

        private void Event_Flashlight(idEventArg<Integer> enable) {
            if (enable.value != 0) {
                lightOn = true;
                MuzzleFlashLight();
            } else {
                lightOn = false;
                muzzleFlashEnd = 0;
            }
        }

        private void Event_GetLightParm(idEventArg<Integer> _parmnum) {
            int parmnum = _parmnum.value;
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                gameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            idThread.ReturnFloat(muzzleFlash.shaderParms[ parmnum]);
        }

        private void Event_SetLightParm(idEventArg<Integer> _parmnum, idEventArg<Float> _value) {
            int parmnum = _parmnum.value;
            float value = _value.value;
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                gameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            muzzleFlash.shaderParms[ parmnum] = value;
            worldMuzzleFlash.shaderParms[ parmnum] = value;
            UpdateVisuals();
        }

        private void Event_SetLightParms(idEventArg<Float> parm0, idEventArg<Float> parm1, idEventArg<Float> parm2, idEventArg<Float> parm3) {
            muzzleFlash.shaderParms[SHADERPARM_RED] = parm0.value;
            muzzleFlash.shaderParms[SHADERPARM_GREEN] = parm1.value;
            muzzleFlash.shaderParms[SHADERPARM_BLUE] = parm2.value;
            muzzleFlash.shaderParms[SHADERPARM_ALPHA] = parm3.value;

            worldMuzzleFlash.shaderParms[SHADERPARM_RED] = parm0.value;
            worldMuzzleFlash.shaderParms[SHADERPARM_GREEN] = parm1.value;
            worldMuzzleFlash.shaderParms[SHADERPARM_BLUE] = parm2.value;
            worldMuzzleFlash.shaderParms[SHADERPARM_ALPHA] = parm3.value;

            UpdateVisuals();
        }

        private void Event_LaunchProjectiles(idEventArg<Integer> _num_projectiles, idEventArg<Float> _spread, idEventArg<Float> fuseOffset, idEventArg<Float> launchPower, idEventArg<Float> _dmgPower) {
            int num_projectiles = _num_projectiles.value;
            float spread = _spread.value;
            float dmgPower = _dmgPower.value;
            idProjectile proj;
            idEntity[] ent = {null};
            int i;
            idVec3 dir;
            float ang;
            float spin;
            float[] distance = {0};
            trace_s[] tr = {null};
            idVec3 start;
            idVec3 muzzle_pos = new idVec3();
            idBounds ownerBounds, projBounds;

            if (IsHidden()) {
                return;
            }

            if (0 == projectileDict.GetNumKeyVals()) {
                final String classname = weaponDef.dict.GetString("classname");
                gameLocal.Warning("No projectile defined on '%s'", classname);
                return;
            }

            // avoid all ammo considerations on an MP client
            if (!gameLocal.isClient) {

                // check if we're out of ammo or the clip is empty
                int ammoAvail = owner.inventory.HasAmmo(ammoType, ammoRequired);
                if (0 == ammoAvail || ((clipSize != 0) && (ammoClip <= 0))) {
                    return;
                }

                // if this is a power ammo weapon ( currently only the bfg ) then make sure 
                // we only fire as much power as available in each clip
                if (powerAmmo) {
                    // power comes in as a float from zero to max
                    // if we use this on more than the bfg will need to define the max
                    // in the .def as opposed to just in the script so proper calcs
                    // can be done here. 
                    dmgPower = (int) dmgPower + 1;
                    if (dmgPower > ammoClip) {
                        dmgPower = ammoClip;
                    }
                }

                owner.inventory.UseAmmo(ammoType, (int) ((powerAmmo) ? dmgPower : ammoRequired));
                if (clipSize != 0 && ammoRequired != 0) {
                    ammoClip -= powerAmmo ? dmgPower : 1;
                }

            }

            if (!silent_fire) {
                // wake up nearby monsters
                gameLocal.AlertAI(owner);
            }

            // set the shader parm to the time of last projectile firing,
            // which the gun material shaders can reference for single shot barrel glows, etc
            renderEntity.shaderParms[ SHADERPARM_DIVERSITY] = gameLocal.random.CRandomFloat();
            renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.realClientTime);

            if (worldModel.GetEntity() != null) {
                worldModel.GetEntity().SetShaderParm(SHADERPARM_DIVERSITY, renderEntity.shaderParms[ SHADERPARM_DIVERSITY]);
                worldModel.GetEntity().SetShaderParm(SHADERPARM_TIMEOFFSET, renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET]);
            }

            // calculate the muzzle position
            if (barrelJointView != INVALID_JOINT && projectileDict.GetBool("launchFromBarrel")) {
                // there is an explicit joint for the muzzle
                GetGlobalJointTransform(true, barrelJointView, muzzleOrigin, muzzleAxis);
            } else {
                // go straight out of the view
                muzzleOrigin.oSet(playerViewOrigin);
                muzzleAxis.oSet(playerViewAxis);
            }

            // add some to the kick time, incrementally moving repeat firing weapons back
            if (kick_endtime < gameLocal.realClientTime) {
                kick_endtime = gameLocal.realClientTime;
            }
            kick_endtime += muzzle_kick_time;
            if (kick_endtime > gameLocal.realClientTime + muzzle_kick_maxtime) {
                kick_endtime = gameLocal.realClientTime + muzzle_kick_maxtime;
            }

            if (gameLocal.isClient) {

                // predict instant hit projectiles
                if (projectileDict.GetBool("net_instanthit")) {
                    float spreadRad = DEG2RAD(spread);
                    muzzle_pos = muzzleOrigin.oPlus(playerViewAxis.oGet(0).oMultiply(2.0f));
                    for (i = 0; i < num_projectiles; i++) {
                        ang = idMath.Sin(spreadRad * gameLocal.random.RandomFloat());
                        spin = (float) DEG2RAD(360.0f) * gameLocal.random.RandomFloat();
                        dir = playerViewAxis.oGet(0).oPlus(playerViewAxis.oGet(2).oMultiply(ang * idMath.Sin(spin)).oMinus(playerViewAxis.oGet(1).oMultiply(ang * idMath.Cos(spin))));
                        dir.Normalize();
                        gameLocal.clip.Translation(tr, muzzle_pos, muzzle_pos.oPlus(dir.oMultiply(4096.0f)), null, getMat3_identity(), MASK_SHOT_RENDERMODEL, owner);
                        if (tr[0].fraction < 1.0f) {
                            idProjectile.ClientPredictionCollide(this, projectileDict, tr[0], getVec3_origin(), true);
                        }
                    }
                }

            } else {

                ownerBounds = owner.GetPhysics().GetAbsBounds();

                owner.AddProjectilesFired(num_projectiles);

                float spreadRad = (float) DEG2RAD(spread);
                for (i = 0; i < num_projectiles; i++) {
                    ang = idMath.Sin(spreadRad * gameLocal.random.RandomFloat());
                    spin = (float) DEG2RAD(360.0f) * gameLocal.random.RandomFloat();
                    dir = playerViewAxis.oGet(0).oPlus(playerViewAxis.oGet(2).oMultiply(ang * idMath.Sin(spin)).oMinus(playerViewAxis.oGet(1).oMultiply(ang * idMath.Cos(spin))));
                    dir.Normalize();

                    if (projectileEnt != null) {
                        ent[0] = projectileEnt;
                        ent[0].Show();
                        ent[0].Unbind();
                        projectileEnt = null;
                    } else {
                        gameLocal.SpawnEntityDef(projectileDict, ent, false);
                    }

                    if (null == ent[0] || !ent[0].IsType(idProjectile.class)) {
                        final String projectileName = weaponDef.dict.GetString("def_projectile");
                        gameLocal.Error("'%s' is not an idProjectile", projectileName);
                    }

                    if (projectileDict.GetBool("net_instanthit")) {
                        // don't synchronize this on top of the already predicted effect
                        ent[0].fl.networkSync = false;
                    }

                    proj = (idProjectile) ent[0];
                    proj.Create(owner, muzzleOrigin, dir);

                    projBounds = proj.GetPhysics().GetBounds().Rotate(proj.GetPhysics().GetAxis());

                    // make sure the projectile starts inside the bounding box of the owner
                    if (i == 0) {
                        muzzle_pos = muzzleOrigin.oPlus(playerViewAxis.oGet(0).oMultiply(2.0f));
                        if ((ownerBounds.oMinus(projBounds)).RayIntersection(muzzle_pos, playerViewAxis.oGet(0), distance)) {
                            start = muzzle_pos.oPlus(playerViewAxis.oGet(0).oMultiply(distance[0]));
                        } else {
                            start = ownerBounds.GetCenter();
                        }
                        gameLocal.clip.Translation(tr, start, muzzle_pos, proj.GetPhysics().GetClipModel(), proj.GetPhysics().GetClipModel().GetAxis(), MASK_SHOT_RENDERMODEL, owner
                        );
                        muzzle_pos = tr[0].endpos;
                    }

                    proj.Launch(muzzle_pos, dir, pushVelocity, fuseOffset.value, launchPower.value, dmgPower);
                }

                // toss the brass
                PostEventMS(EV_Weapon_EjectBrass, brassDelay);
            }

            // add the light for the muzzleflash
            if (!lightOn) {
                MuzzleFlashLight();
            }

            owner.WeaponFireFeedback(weaponDef.dict);

            // reset muzzle smoke
            weaponSmokeStartTime = gameLocal.realClientTime;
        }

        private void Event_CreateProjectile() {
            if (!gameLocal.isClient) {
                idEntity[] projectileEnt2 = {null};
                gameLocal.SpawnEntityDef(projectileDict, projectileEnt2, false);
                projectileEnt = projectileEnt2[0];
                if (projectileEnt != null) {
                    projectileEnt.SetOrigin(GetPhysics().GetOrigin());
                    projectileEnt.Bind(owner, false);
                    projectileEnt.Hide();
                }
                idThread.ReturnEntity(projectileEnt);
            } else {
                idThread.ReturnEntity(null);
            }
        }

        /*
         ================
         idWeapon::Event_EjectBrass

         Toss a shell model out from the breach if the bone is present
         ================
         */
        private void Event_EjectBrass() {
            if (!g_showBrass.GetBool() || !owner.CanShowWeaponViewmodel()) {
                return;
            }

            if (ejectJointView == INVALID_JOINT || 0 == brassDict.GetNumKeyVals()) {
                return;
            }

            if (gameLocal.isClient) {
                return;
            }

            idMat3 axis = new idMat3();
            idVec3 origin = new idVec3(), linear_velocity, angular_velocity = new idVec3();
            idEntity[] ent = {null};

            if (!GetGlobalJointTransform(true, ejectJointView, origin, axis)) {
                return;
            }

            gameLocal.SpawnEntityDef(brassDict, ent, false);
            if (NOT(ent[0]) || !ent[0].IsType(idDebris.class)) {
                gameLocal.Error("'%s' is not an idDebris", weaponDef != null ? weaponDef.dict.GetString("def_ejectBrass") : "def_ejectBrass");
            }
            idDebris debris = (idDebris) ent[0];
            debris.Create(owner, origin, axis);
            debris.Launch();

            linear_velocity = playerViewAxis.oGet(0).oPlus(playerViewAxis.oGet(1).oPlus(playerViewAxis.oGet(2))).oMultiply(40);
            angular_velocity.Set(10 * gameLocal.random.CRandomFloat(), 10 * gameLocal.random.CRandomFloat(), 10 * gameLocal.random.CRandomFloat());

            debris.GetPhysics().SetLinearVelocity(linear_velocity);
            debris.GetPhysics().SetAngularVelocity(angular_velocity);
        }

        private void Event_Melee() {
            idEntity ent;
            trace_s[] tr = {null};

            if (null == meleeDef) {
                gameLocal.Error("No meleeDef on '%s'", weaponDef.dict.GetString("classname"));
            }

            if (!gameLocal.isClient) {
                idVec3 start = playerViewOrigin;
                idVec3 end = start.oPlus(playerViewAxis.oGet(0).oMultiply(meleeDistance * owner.PowerUpModifier(MELEE_DISTANCE)));
                gameLocal.clip.TracePoint(tr, start, end, MASK_SHOT_RENDERMODEL, owner);
                if (tr[0].fraction < 1.0f) {
                    ent = gameLocal.GetTraceEntity(tr[0]);
                } else {
                    ent = null;
                }

                if (g_debugWeapon.GetBool()) {
                    gameRenderWorld.DebugLine(colorYellow, start, end, 100);
                    if (ent != null) {
                        gameRenderWorld.DebugBounds(colorRed, ent.GetPhysics().GetBounds(), ent.GetPhysics().GetOrigin(), 100);
                    }
                }

                boolean hit = false;
                String hitSound = meleeDef.dict.GetString("snd_miss");

                if (ent != null) {

                    float push = meleeDef.dict.GetFloat("push");
                    idVec3 impulse = tr[0].c.normal.oMultiply(-push * owner.PowerUpModifier(SPEED));

                    if (gameLocal.world.spawnArgs.GetBool("no_Weapons") && (ent.IsType(idActor.class) || ent.IsType(idAFAttachment.class))) {
                        idThread.ReturnInt(0);
                        return;
                    }

                    ent.ApplyImpulse(this, tr[0].c.id, tr[0].c.point, impulse);

                    // weapon stealing - do this before damaging so weapons are not dropped twice
                    if (gameLocal.isMultiplayer
                            && weaponDef != null && weaponDef.dict.GetBool("stealing")
                            && ent.IsType(idPlayer.class)
                            && !owner.PowerUpActive(BERSERK)
                            && (gameLocal.gameType != GAME_TDM || gameLocal.serverInfo.GetBool("si_teamDamage") || (owner.team != ((idPlayer) ent).team))) {
                        owner.StealWeapon((idPlayer) ent);
                    }

                    if (ent.fl.takedamage) {
                        idVec3 kickDir = new idVec3(), globalKickDir;
                        meleeDef.dict.GetVector("kickDir", "0 0 0", kickDir);
                        globalKickDir = muzzleAxis.oMultiply(kickDir);
                        ent.Damage(owner, owner, globalKickDir, meleeDefName.toString(), owner.PowerUpModifier(MELEE_DAMAGE), tr[0].c.id);
                        hit = true;
                    }

                    if (weaponDef.dict.GetBool("impact_damage_effect")) {

                        if (ent.spawnArgs.GetBool("bleed")) {

                            hitSound = meleeDef.dict.GetString(owner.PowerUpActive(BERSERK) ? "snd_hit_berserk" : "snd_hit");

                            ent.AddDamageEffect(tr[0], impulse, meleeDef.dict.GetString("classname"));

                        } else {

                            surfTypes_t type = tr[0].c.material.GetSurfaceType();
                            if (type == SURFTYPE_NONE) {
                                type = surfTypes_t.values()[GetDefaultSurfaceType()];
                            }

                            final String materialType = gameLocal.sufaceTypeNames[ type.ordinal()];

                            // start impact sound based on material type
                            hitSound = meleeDef.dict.GetString(va("snd_%s", materialType));
                            if (isNotNullOrEmpty(hitSound)) {
                                hitSound = meleeDef.dict.GetString("snd_metal");
                            }

                            if (gameLocal.time > nextStrikeFx) {
                                final String decal;
                                // project decal
                                decal = weaponDef.dict.GetString("mtr_strike");
                                if (isNotNullOrEmpty(decal)) {
                                    gameLocal.ProjectDecal(tr[0].c.point, tr[0].c.normal.oNegative(), 8.0f, true, 6.0f, decal);
                                }
                                nextStrikeFx = gameLocal.time + 200;
                            } else {
                                hitSound = "";
                            }

                            strikeSmokeStartTime = gameLocal.time;
                            strikePos.oSet(tr[0].c.point);
                            strikeAxis.oSet(tr[0].endAxis.oNegative());
                        }
                    }
                }

                if (isNotNullOrEmpty(hitSound)) {
                    final idSoundShader snd = declManager.FindSound(hitSound);
                    StartSoundShader(snd, SND_CHANNEL_BODY2, 0, true, null);
                }

                idThread.ReturnInt(hit);
                owner.WeaponFireFeedback(weaponDef.dict);
                return;
            }

            idThread.ReturnInt(0);
            owner.WeaponFireFeedback(weaponDef.dict);
        }

        private void Event_GetWorldModel() {
            idThread.ReturnEntity(worldModel.GetEntity());
        }

        private void Event_AllowDrop(idEventArg<Integer> allow) {
            allowDrop = (allow.value != 0);
        }

        private void Event_AutoReload() {
            assert (owner != null);
            if (gameLocal.isClient) {
                idThread.ReturnFloat(0.0f);
                return;
            }
            idThread.ReturnFloat(btoi(gameLocal.userInfo[owner.entityNumber].GetBool("ui_autoReload")));
        }

        private void Event_NetReload() {
            assert (owner != null);
            if (gameLocal.isServer) {
                ServerSendEvent(EVENT_RELOAD, null, false, -1);
            }
        }

        private void Event_IsInvisible() {
            if (null == owner) {
                idThread.ReturnFloat(0);
                return;
            }
            idThread.ReturnFloat(owner.PowerUpActive(INVISIBILITY) ? 1 : 0);
        }

        private void Event_NetEndReload() {
            assert (owner != null);
            if (gameLocal.isServer) {
                ServerSendEvent(EVENT_ENDRELOAD, null, false, -1);
            }
        }

        @Override
        public void oSet(idClass oGet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
