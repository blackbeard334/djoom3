package neo.Game;

import static neo.Game.Actor.AI_AnimDone;
import static neo.Game.Actor.AI_GetBlendFrames;
import static neo.Game.Actor.AI_PlayAnim;
import static neo.Game.Actor.AI_PlayCycle;
import static neo.Game.Actor.AI_SetBlendFrames;
import static neo.Game.Animation.Anim.FRAME2MS;
import static neo.Game.Entity.EV_SetSkin;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.signalNum_t.SIG_TOUCH;
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
import static neo.Game.Weapon.weaponStatus_t.WP_HOLSTERED;
import static neo.Game.Weapon.weaponStatus_t.WP_LOWERING;
import static neo.Game.Weapon.weaponStatus_t.WP_OUTOFAMMO;
import static neo.Game.Weapon.weaponStatus_t.WP_READY;
import static neo.Game.Weapon.weaponStatus_t.WP_RELOAD;
import static neo.Game.Weapon.weaponStatus_t.WP_RISING;
import static neo.Renderer.Material.CONTENTS_FLASHLIGHT_TRIGGER;
import static neo.Renderer.Material.CONTENTS_OPAQUE;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_NONE;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_DIVERSITY;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMESCALE;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.BuildDefines.ID_DEMO_BUILD;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.framework.DeclManager.declType_t.DECL_SKIN;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.ui.UserInterface.uiManager;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.Actor.idActor;
import neo.Game.Entity.idAnimatedEntity;
import neo.Game.Entity.idEntity;
import neo.Game.Game.refSound_t;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Item.idMoveableItem;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idDebris;
import neo.Game.Projectile.idProjectile;
import neo.Game.Trigger.idTrigger;
import neo.Game.AI.AI.idAI;
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
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Program.idScriptBool;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.surfTypes_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclSkin.idDeclSkin;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

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
    }
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
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
        private final idScriptBool WEAPON_ATTACK       = new idScriptBool();
        private final idScriptBool WEAPON_RELOAD       = new idScriptBool();
        private final idScriptBool WEAPON_NETRELOAD    = new idScriptBool();
        private final idScriptBool WEAPON_NETENDRELOAD = new idScriptBool();
        private final idScriptBool WEAPON_NETFIRING    = new idScriptBool();
        private final idScriptBool WEAPON_RAISEWEAPON  = new idScriptBool();
        private final idScriptBool WEAPON_LOWERWEAPON  = new idScriptBool();
        private weaponStatus_t                status;
        private idThread                      thread;
        private final idStr                         state;
        private final idStr                         idealState;
        private int                           animBlendFrames;
        private int                           animDoneTime;
        private boolean                       isLinked;
        //
        // precreated projectile
        private idEntity                      projectileEnt;
        //
        private idPlayer                      owner;
        private final idEntityPtr<idAnimatedEntity> worldModel;
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
        private final idVec3                        playerViewOrigin;
        private final idMat3                        playerViewAxis;
        //
        // the view weapon render entity parms
        private final idVec3                        viewWeaponOrigin;
        private final idMat3                        viewWeaponAxis;
        //
        // the muzzle bone's position, used for launching projectiles and trailing smoke
        private final idVec3                        muzzleOrigin;
        private final idMat3                        muzzleAxis;
        //
        private final idVec3                        pushVelocity;
        //
        // weapon definition
        // we maintain local copies of the projectile and brass dictionaries so they
        // do not have to be copied across the DLL boundary when entities are spawned
        private idDeclEntityDef               weaponDef;
        private idDeclEntityDef               meleeDef;
        private final idDict                        projectileDict;
        private float                         meleeDistance;
        private final idStr                         meleeDefName;
        private final idDict                        brassDict;
        private int                           brassDelay;
        private final idStr                         icon;
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
        private final idVec3                        flashColor;
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
        private final idAngles                      muzzle_kick_angles;
        private final idVec3                        muzzle_kick_offset;
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
        private final idVec3                        strikePos;            // position of last melee strike
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
        private final idVec3                        nozzleGlowColor;      // color of the nozzle glow
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
            this.owner = null;
            this.worldModel = new idEntityPtr<>(null);
            this.weaponDef = null;
            this.thread = null;

            this.guiLight = new renderLight_s();//memset( &guiLight, 0, sizeof( guiLight ) );
            this.muzzleFlash = new renderLight_s();//memset( &muzzleFlash, 0, sizeof( muzzleFlash ) );
            this.worldMuzzleFlash = new renderLight_s();//memset( &worldMuzzleFlash, 0, sizeof( worldMuzzleFlash ) );
            this.nozzleGlow = new renderLight_s();//memset( &nozzleGlow, 0, sizeof( nozzleGlow ) );

            this.muzzleFlashEnd = 0;
            this.flashColor = getVec3_origin();
            this.muzzleFlashHandle = -1;
            this.worldMuzzleFlashHandle = -1;
            this.guiLightHandle = -1;
            this.nozzleGlowHandle = -1;
            this.modelDefHandle = -1;

            this.berserk = 2;
            this.brassDelay = 0;

            this.allowDrop = true;

            this.state = new idStr();
            this.idealState = new idStr();
            this.playerViewOrigin = new idVec3();
            this.playerViewAxis = new idMat3();
            this.viewWeaponOrigin = new idVec3();
            this.viewWeaponAxis = new idMat3();
            this.muzzleOrigin = new idVec3();
            this.muzzleAxis = new idMat3();
            this.pushVelocity = new idVec3();
            this.projectileDict = new idDict();
            this.meleeDefName = new idStr();
            this.brassDict = new idDict();
            this.icon = new idStr();
            this.muzzle_kick_angles = new idAngles();
            this.muzzle_kick_offset = new idVec3();
            this.strikePos = new idVec3();
            this.nozzleGlowColor = new idVec3();

            Clear();

            this.fl.networkSync = true;
        }
        // virtual					~idWeapon();

        // Init
        @Override
        public void Spawn() {
            super.Spawn();

            if (!gameLocal.isClient) {
                // setup the world model
                this.worldModel.oSet((idAnimatedEntity) gameLocal.SpawnEntityType(idAnimatedEntity.class, null));
                this.worldModel.GetEntity().fl.networkSync = true;
            }

            this.thread = new idThread();
            this.thread.ManualDelete();
            this.thread.ManualControl();
        }

        /*
         ================
         idWeapon::SetOwner

         Only called at player spawn time, not each weapon switch
         ================
         */
        public void SetOwner(idPlayer _owner) {
            assert (null == this.owner);
            this.owner = _owner;
            SetName(va("%s_weapon", this.owner.name));

            if (this.worldModel.GetEntity() != null) {
                this.worldModel.GetEntity().SetName(va("%s_weapon_worldmodel", this.owner.name));
            }
        }

        public idPlayer GetOwner() {
            return this.owner;
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
            final idStr clipModelName = new idStr();
            final idTraceModel trm = new idTraceModel();
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

            savefile.WriteInt(etoi(this.status));
            savefile.WriteObject(this.thread);
            savefile.WriteString(this.state);
            savefile.WriteString(this.idealState);
            savefile.WriteInt(this.animBlendFrames);
            savefile.WriteInt(this.animDoneTime);
            savefile.WriteBool(this.isLinked);

            savefile.WriteObject(this.owner);
            this.worldModel.Save(savefile);

            savefile.WriteInt(this.hideTime);
            savefile.WriteFloat(this.hideDistance);
            savefile.WriteInt(this.hideStartTime);
            savefile.WriteFloat(this.hideStart);
            savefile.WriteFloat(this.hideEnd);
            savefile.WriteFloat(this.hideOffset);
            savefile.WriteBool(this.hide);
            savefile.WriteBool(this.disabled);

            savefile.WriteInt(this.berserk);

            savefile.WriteVec3(this.playerViewOrigin);
            savefile.WriteMat3(this.playerViewAxis);

            savefile.WriteVec3(this.viewWeaponOrigin);
            savefile.WriteMat3(this.viewWeaponAxis);

            savefile.WriteVec3(this.muzzleOrigin);
            savefile.WriteMat3(this.muzzleAxis);

            savefile.WriteVec3(this.pushVelocity);

            savefile.WriteString(this.weaponDef.GetName());
            savefile.WriteFloat(this.meleeDistance);
            savefile.WriteString(this.meleeDefName);
            savefile.WriteInt(this.brassDelay);
            savefile.WriteString(this.icon);

            savefile.WriteInt(this.guiLightHandle);
            savefile.WriteRenderLight(this.guiLight);

            savefile.WriteInt(this.muzzleFlashHandle);
            savefile.WriteRenderLight(this.muzzleFlash);

            savefile.WriteInt(this.worldMuzzleFlashHandle);
            savefile.WriteRenderLight(this.worldMuzzleFlash);

            savefile.WriteVec3(this.flashColor);
            savefile.WriteInt(this.muzzleFlashEnd);
            savefile.WriteInt(this.flashTime);

            savefile.WriteBool(this.lightOn);
            savefile.WriteBool(this.silent_fire);

            savefile.WriteInt(this.kick_endtime);
            savefile.WriteInt(this.muzzle_kick_time);
            savefile.WriteInt(this.muzzle_kick_maxtime);
            savefile.WriteAngles(this.muzzle_kick_angles);
            savefile.WriteVec3(this.muzzle_kick_offset);

            savefile.WriteInt(this.ammoType);
            savefile.WriteInt(this.ammoRequired);
            savefile.WriteInt(this.clipSize);
            savefile.WriteInt(this.ammoClip);
            savefile.WriteInt(this.lowAmmo);
            savefile.WriteBool(this.powerAmmo);

            // savegames <= 17
            savefile.WriteInt(0);

            savefile.WriteInt(this.zoomFov);

            savefile.WriteJoint(this.barrelJointView);
            savefile.WriteJoint(this.flashJointView);
            savefile.WriteJoint(this.ejectJointView);
            savefile.WriteJoint(this.guiLightJointView);
            savefile.WriteJoint(this.ventLightJointView);

            savefile.WriteJoint(this.flashJointWorld);
            savefile.WriteJoint(this.barrelJointWorld);
            savefile.WriteJoint(this.ejectJointWorld);

            savefile.WriteBool(this.hasBloodSplat);

            savefile.WriteSoundShader(this.sndHum);

            savefile.WriteParticle(this.weaponSmoke);
            savefile.WriteInt(this.weaponSmokeStartTime);
            savefile.WriteBool(this.continuousSmoke);
            savefile.WriteParticle(this.strikeSmoke);
            savefile.WriteInt(this.strikeSmokeStartTime);
            savefile.WriteVec3(this.strikePos);
            savefile.WriteMat3(this.strikeAxis);
            savefile.WriteInt(this.nextStrikeFx);

            savefile.WriteBool(this.nozzleFx);
            savefile.WriteInt(this.nozzleFxFade);

            savefile.WriteInt(this.lastAttack);

            savefile.WriteInt(this.nozzleGlowHandle);
            savefile.WriteRenderLight(this.nozzleGlow);

            savefile.WriteVec3(this.nozzleGlowColor);
            savefile.WriteMaterial(this.nozzleGlowShader);
            savefile.WriteFloat(this.nozzleGlowRadius);

            savefile.WriteInt(this.weaponAngleOffsetAverages);
            savefile.WriteFloat(this.weaponAngleOffsetScale);
            savefile.WriteFloat(this.weaponAngleOffsetMax);
            savefile.WriteFloat(this.weaponOffsetTime);
            savefile.WriteFloat(this.weaponOffsetScale);

            savefile.WriteBool(this.allowDrop);
            savefile.WriteObject(this.projectileEnt);

        }

        @Override
        public void Restore(idRestoreGame savefile) {					// unarchives object from save game file

            this.status = weaponStatus_t.values()[savefile.ReadInt()];
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/thread);
            savefile.ReadString(this.state);
            savefile.ReadString(this.idealState);
            this.animBlendFrames = savefile.ReadInt();
            this.animDoneTime = savefile.ReadInt();
            this.isLinked = savefile.ReadBool();

            // Re-link script fields
            this.WEAPON_ATTACK.LinkTo(this.scriptObject, "WEAPON_ATTACK");
            this.WEAPON_RELOAD.LinkTo(this.scriptObject, "WEAPON_RELOAD");
            this.WEAPON_NETRELOAD.LinkTo(this.scriptObject, "WEAPON_NETRELOAD");
            this.WEAPON_NETENDRELOAD.LinkTo(this.scriptObject, "WEAPON_NETENDRELOAD");
            this.WEAPON_NETFIRING.LinkTo(this.scriptObject, "WEAPON_NETFIRING");
            this.WEAPON_RAISEWEAPON.LinkTo(this.scriptObject, "WEAPON_RAISEWEAPON");
            this.WEAPON_LOWERWEAPON.LinkTo(this.scriptObject, "WEAPON_LOWERWEAPON");

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/owner);
            this.worldModel.Restore(savefile);

            this.hideTime = savefile.ReadInt();
            this.hideDistance = savefile.ReadFloat();
            this.hideStartTime = savefile.ReadInt();
            this.hideStart = savefile.ReadFloat();
            this.hideEnd = savefile.ReadFloat();
            this.hideOffset = savefile.ReadFloat();
            this.hide = savefile.ReadBool();
            this.disabled = savefile.ReadBool();

            this.berserk = savefile.ReadInt();

            savefile.ReadVec3(this.playerViewOrigin);
            savefile.ReadMat3(this.playerViewAxis);

            savefile.ReadVec3(this.viewWeaponOrigin);
            savefile.ReadMat3(this.viewWeaponAxis);

            savefile.ReadVec3(this.muzzleOrigin);
            savefile.ReadMat3(this.muzzleAxis);

            savefile.ReadVec3(this.pushVelocity);

            final idStr objectname = new idStr();
            savefile.ReadString(objectname);
            this.weaponDef = gameLocal.FindEntityDef(objectname.toString());
            this.meleeDef = gameLocal.FindEntityDef(this.weaponDef.dict.GetString("def_melee"), false);

            final idDeclEntityDef projectileDef = gameLocal.FindEntityDef(this.weaponDef.dict.GetString("def_projectile"), false);
            if (projectileDef != null) {
                this.projectileDict.oSet(projectileDef.dict);
            } else {
                this.projectileDict.Clear();
            }

            final idDeclEntityDef brassDef = gameLocal.FindEntityDef(this.weaponDef.dict.GetString("def_ejectBrass"), false);
            if (brassDef != null) {
                this.brassDict.oSet(brassDef.dict);
            } else {
                this.brassDict.Clear();
            }

            this.meleeDistance = savefile.ReadFloat();
            savefile.ReadString(this.meleeDefName);
            this.brassDelay = savefile.ReadInt();
            savefile.ReadString(this.icon);

            this.guiLightHandle = savefile.ReadInt();
            savefile.ReadRenderLight(this.guiLight);

            this.muzzleFlashHandle = savefile.ReadInt();
            savefile.ReadRenderLight(this.muzzleFlash);

            this.worldMuzzleFlashHandle = savefile.ReadInt();
            savefile.ReadRenderLight(this.worldMuzzleFlash);

            savefile.ReadVec3(this.flashColor);
            this.muzzleFlashEnd = savefile.ReadInt();
            this.flashTime = savefile.ReadInt();

            this.lightOn = savefile.ReadBool();
            this.silent_fire = savefile.ReadBool();

            this.kick_endtime = savefile.ReadInt();
            this.muzzle_kick_time = savefile.ReadInt();
            this.muzzle_kick_maxtime = savefile.ReadInt();
            savefile.ReadAngles(this.muzzle_kick_angles);
            savefile.ReadVec3(this.muzzle_kick_offset);

            this.ammoType = savefile.ReadInt();
            this.ammoRequired = savefile.ReadInt();
            this.clipSize = savefile.ReadInt();
            this.ammoClip = savefile.ReadInt();
            this.lowAmmo = savefile.ReadInt();
            this.powerAmmo = savefile.ReadBool();

            // savegame versions <= 17
            int foo;
            foo = savefile.ReadInt();

            this.zoomFov = savefile.ReadInt();

            this.barrelJointView = savefile.ReadJoint();
            this.flashJointView = savefile.ReadJoint();
            this.ejectJointView = savefile.ReadJoint();
            this.guiLightJointView = savefile.ReadJoint();
            this.ventLightJointView = savefile.ReadJoint();

            this.flashJointWorld = savefile.ReadJoint();
            this.barrelJointWorld = savefile.ReadJoint();
            this.ejectJointWorld = savefile.ReadJoint();

            this.hasBloodSplat = savefile.ReadBool();

            savefile.ReadSoundShader(this.sndHum);

            savefile.ReadParticle(this.weaponSmoke);
            this.weaponSmokeStartTime = savefile.ReadInt();
            this.continuousSmoke = savefile.ReadBool();
            savefile.ReadParticle(this.strikeSmoke);
            this.strikeSmokeStartTime = savefile.ReadInt();
            savefile.ReadVec3(this.strikePos);
            savefile.ReadMat3(this.strikeAxis);
            this.nextStrikeFx = savefile.ReadInt();

            this.nozzleFx = savefile.ReadBool();
            this.nozzleFxFade = savefile.ReadInt();

            this.lastAttack = savefile.ReadInt();

            this.nozzleGlowHandle = savefile.ReadInt();
            savefile.ReadRenderLight(this.nozzleGlow);

            savefile.ReadVec3(this.nozzleGlowColor);
            savefile.ReadMaterial(this.nozzleGlowShader);
            this.nozzleGlowRadius = savefile.ReadFloat();

            this.weaponAngleOffsetAverages = savefile.ReadInt();
            this.weaponAngleOffsetScale = savefile.ReadFloat();
            this.weaponAngleOffsetMax = savefile.ReadFloat();
            this.weaponOffsetTime = savefile.ReadFloat();
            this.weaponOffsetScale = savefile.ReadFloat();

            this.allowDrop = savefile.ReadBool();
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/projectileEnt);
        }

        /* **********************************************************************

         Weapon definition management

         ***********************************************************************/
        public void Clear() {
            CancelEvents(EV_Weapon_Clear);

            DeconstructScriptObject();
            this.scriptObject.Free();

            this.WEAPON_ATTACK.Unlink();
            this.WEAPON_RELOAD.Unlink();
            this.WEAPON_NETRELOAD.Unlink();
            this.WEAPON_NETENDRELOAD.Unlink();
            this.WEAPON_NETFIRING.Unlink();
            this.WEAPON_RAISEWEAPON.Unlink();
            this.WEAPON_LOWERWEAPON.Unlink();

            if (this.muzzleFlashHandle != -1) {
                gameRenderWorld.FreeLightDef(this.muzzleFlashHandle);
                this.muzzleFlashHandle = -1;
            }
            if (this.muzzleFlashHandle != -1) {
                gameRenderWorld.FreeLightDef(this.muzzleFlashHandle);
                this.muzzleFlashHandle = -1;
            }
            if (this.worldMuzzleFlashHandle != -1) {
                gameRenderWorld.FreeLightDef(this.worldMuzzleFlashHandle);
                this.worldMuzzleFlashHandle = -1;
            }
            if (this.guiLightHandle != -1) {
                gameRenderWorld.FreeLightDef(this.guiLightHandle);
                this.guiLightHandle = -1;
            }
            if (this.nozzleGlowHandle != -1) {
                gameRenderWorld.FreeLightDef(this.nozzleGlowHandle);
                this.nozzleGlowHandle = -1;
            }

//	memset( &renderEntity, 0, sizeof( renderEntity ) );
            this.renderEntity = new renderEntity_s();
            this.renderEntity.entityNum = this.entityNumber;

            this.renderEntity.noShadow = true;
            this.renderEntity.noSelfShadow = true;
            this.renderEntity.customSkin = null;

            // set default shader parms
            this.renderEntity.shaderParms[ SHADERPARM_RED] = 1.0f;
            this.renderEntity.shaderParms[ SHADERPARM_GREEN] = 1.0f;
            this.renderEntity.shaderParms[ SHADERPARM_BLUE] = 1.0f;
            this.renderEntity.shaderParms[3] = 1.0f;
            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = 0.0f;
            this.renderEntity.shaderParms[5] = 0.0f;
            this.renderEntity.shaderParms[6] = 0.0f;
            this.renderEntity.shaderParms[7] = 0.0f;

            if (this.refSound.referenceSound != null) {
                this.refSound.referenceSound.Free(true);
            }
//	memset( &refSound, 0, sizeof( refSound_t ) );
            this.refSound = new refSound_t();

            // setting diversity to 0 results in no random sound.  -1 indicates random.
            this.refSound.diversity = -1.0f;

            if (this.owner != null) {
                // don't spatialize the weapon sounds
                this.refSound.listenerId = this.owner.GetListenerId();
            }

            // clear out the sounds from our spawnargs since we'll copy them from the weapon def
            idKeyValue kv = this.spawnArgs.MatchPrefix("snd_");
            while (kv != null) {
                this.spawnArgs.Delete(kv.GetKey());
                kv = this.spawnArgs.MatchPrefix("snd_");
            }

            this.hideTime = 300;
            this.hideDistance = -15.0f;
            this.hideStartTime = gameLocal.time - this.hideTime;
            this.hideStart = 0.0f;
            this.hideEnd = 0.0f;
            this.hideOffset = 0.0f;
            this.hide = false;
            this.disabled = false;

            this.weaponSmoke = null;
            this.weaponSmokeStartTime = 0;
            this.continuousSmoke = false;
            this.strikeSmoke = null;
            this.strikeSmokeStartTime = 0;
            this.strikePos.Zero();
            this.strikeAxis = getMat3_identity();
            this.nextStrikeFx = 0;

            this.icon.oSet("");

            this.playerViewAxis.Identity();
            this.playerViewOrigin.Zero();
            this.viewWeaponAxis.Identity();
            this.viewWeaponOrigin.Zero();
            this.muzzleAxis.Identity();
            this.muzzleOrigin.Zero();
            this.pushVelocity.Zero();

            this.status = WP_HOLSTERED;
            this.state.oSet("");
            this.idealState.oSet("");
            this.animBlendFrames = 0;
            this.animDoneTime = 0;

            this.projectileDict.Clear();
            this.meleeDef = null;
            this.meleeDefName.oSet("");
            this.meleeDistance = 0.0f;
            this.brassDict.Clear();

            this.flashTime = 250;
            this.lightOn = false;
            this.silent_fire = false;

            this.ammoType = 0;
            this.ammoRequired = 0;
            this.ammoClip = 0;
            this.clipSize = 0;
            this.lowAmmo = 0;
            this.powerAmmo = false;

            this.kick_endtime = 0;
            this.muzzle_kick_time = 0;
            this.muzzle_kick_maxtime = 0;
            this.muzzle_kick_angles.Zero();
            this.muzzle_kick_offset.Zero();

            this.zoomFov = 90;

            this.barrelJointView = INVALID_JOINT;
            this.flashJointView = INVALID_JOINT;
            this.ejectJointView = INVALID_JOINT;
            this.guiLightJointView = INVALID_JOINT;
            this.ventLightJointView = INVALID_JOINT;

            this.barrelJointWorld = INVALID_JOINT;
            this.flashJointWorld = INVALID_JOINT;
            this.ejectJointWorld = INVALID_JOINT;

            this.hasBloodSplat = false;
            this.nozzleFx = false;
            this.nozzleFxFade = 1500;
            this.lastAttack = 0;
            this.nozzleGlowHandle = -1;
            this.nozzleGlowShader = null;
            this.nozzleGlowRadius = 10;
            this.nozzleGlowColor.Zero();

            this.weaponAngleOffsetAverages = 0;
            this.weaponAngleOffsetScale = 0.0f;
            this.weaponAngleOffsetMax = 0.0f;
            this.weaponOffsetTime = 0.0f;
            this.weaponOffsetScale = 0.0f;

            this.allowDrop = true;

            this.animator.ClearAllAnims(gameLocal.time, 0);
            FreeModelDef();

            this.sndHum = null;

            this.isLinked = false;
            this.projectileEnt = null;

            this.isFiring = false;
        }

        public void GetWeaponDef(final String objectName, int ammoinclip) {
            final String[] shader = {null};
            final String[] objectType = {null};
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

            assert (this.owner != null);

            this.weaponDef = gameLocal.FindEntityDef(objectName);

            this.ammoType = GetAmmoNumForName(this.weaponDef.dict.GetString("ammoType"));
            this.ammoRequired = this.weaponDef.dict.GetInt("ammoRequired");
            this.clipSize = this.weaponDef.dict.GetInt("clipSize");
            this.lowAmmo = this.weaponDef.dict.GetInt("lowAmmo");

            this.icon.oSet(this.weaponDef.dict.GetString("icon"));
            this.silent_fire = this.weaponDef.dict.GetBool("silent_fire");
            this.powerAmmo = this.weaponDef.dict.GetBool("powerAmmo");

            this.muzzle_kick_time = (int) SEC2MS(this.weaponDef.dict.GetFloat("muzzle_kick_time"));
            this.muzzle_kick_maxtime = (int) SEC2MS(this.weaponDef.dict.GetFloat("muzzle_kick_maxtime"));
            this.muzzle_kick_angles.oSet(this.weaponDef.dict.GetAngles("muzzle_kick_angles"));
            this.muzzle_kick_offset.oSet(this.weaponDef.dict.GetVector("muzzle_kick_offset"));

            this.hideTime = (int) SEC2MS(this.weaponDef.dict.GetFloat("hide_time", "0.3"));
            this.hideDistance = this.weaponDef.dict.GetFloat("hide_distance", "-15");

            // muzzle smoke
            smokeName = this.weaponDef.dict.GetString("smoke_muzzle");
            if (isNotNullOrEmpty(smokeName)) {
                this.weaponSmoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
            } else {
                this.weaponSmoke = null;
            }
            this.continuousSmoke = this.weaponDef.dict.GetBool("continuousSmoke");
            this.weaponSmokeStartTime = (this.continuousSmoke) ? gameLocal.time : 0;

            smokeName = this.weaponDef.dict.GetString("smoke_strike");
            if (isNotNullOrEmpty(smokeName)) {
                this.strikeSmoke = (idDeclParticle) declManager.FindType(DECL_PARTICLE, smokeName);
            } else {
                this.strikeSmoke = null;
            }
            this.strikeSmokeStartTime = 0;
            this.strikePos.Zero();
            this.strikeAxis = getMat3_identity();
            this.nextStrikeFx = 0;

            // setup gui light
            this.guiLight = new renderLight_s();//	memset( &guiLight, 0, sizeof( guiLight ) );
            final String guiLightShader = this.weaponDef.dict.GetString("mtr_guiLightShader");
            if (isNotNullOrEmpty(guiLightShader)) {
                this.guiLight.shader = declManager.FindMaterial(guiLightShader, false);
                this.guiLight.lightRadius.oSet(0, this.guiLight.lightRadius.oSet(1, this.guiLight.lightRadius.oSet(2, 3)));
                this.guiLight.pointLight = true;
            }

            // setup the view model
            vmodel = this.weaponDef.dict.GetString("model_view");
            SetModel(vmodel);

            // setup the world model
            InitWorldModel(this.weaponDef);

            // copy the sounds from the weapon view model def into out spawnargs
            idKeyValue kv = this.weaponDef.dict.MatchPrefix("snd_");
            while (kv != null) {
                this.spawnArgs.Set(kv.GetKey(), kv.GetValue());
                kv = this.weaponDef.dict.MatchPrefix("snd_", kv);
            }

            // find some joints in the model for locating effects
            this.barrelJointView = this.animator.GetJointHandle("barrel");
            this.flashJointView = this.animator.GetJointHandle("flash");
            this.ejectJointView = this.animator.GetJointHandle("eject");
            this.guiLightJointView = this.animator.GetJointHandle("guiLight");
            this.ventLightJointView = this.animator.GetJointHandle("ventLight");

            // get the projectile
            this.projectileDict.Clear();

            projectileName = this.weaponDef.dict.GetString("def_projectile");
            if (isNotNullOrEmpty(projectileName)) {
                final idDeclEntityDef projectileDef = gameLocal.FindEntityDef(projectileName, false);
                if (null == projectileDef) {
                    gameLocal.Warning("Unknown projectile '%s' in weapon '%s'", projectileName, objectName);
                } else {
                    final String spawnclass = projectileDef.dict.GetString("spawnclass");
                    if (!idProjectile.class.getSimpleName().equals(spawnclass)) {
                        gameLocal.Warning("Invalid spawnclass '%s' on projectile '%s' (used by weapon '%s')", spawnclass, projectileName, objectName);
                    } else {
                        this.projectileDict.oSet(projectileDef.dict);
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

            this.weaponDef.dict.GetString("mtr_flashShader", "", shader);
            flashShader = declManager.FindMaterial(shader[0], false);
            flashPointLight = this.weaponDef.dict.GetBool("flashPointLight", "1");
            this.weaponDef.dict.GetVector("flashColor", "0 0 0", this.flashColor);
            flashRadius = this.weaponDef.dict.GetInt("flashRadius");	// if 0, no light will spawn
            this.flashTime = (int) SEC2MS(this.weaponDef.dict.GetFloat("flashTime", "0.25"));
            flashTarget = this.weaponDef.dict.GetVector("flashTarget");
            flashUp = this.weaponDef.dict.GetVector("flashUp");
            flashRight = this.weaponDef.dict.GetVector("flashRight");

            this.muzzleFlash = new renderLight_s();//memset( & muzzleFlash, 0, sizeof(muzzleFlash));
            this.muzzleFlash.lightId = LIGHTID_VIEW_MUZZLE_FLASH + this.owner.entityNumber;
            this.muzzleFlash.allowLightInViewID = this.owner.entityNumber + 1;

            // the weapon lights will only be in first person
            this.guiLight.allowLightInViewID = this.owner.entityNumber + 1;
            this.nozzleGlow.allowLightInViewID = this.owner.entityNumber + 1;

            this.muzzleFlash.pointLight = flashPointLight;
            this.muzzleFlash.shader = flashShader;
            this.muzzleFlash.shaderParms[ SHADERPARM_RED] = this.flashColor.oGet(0);
            this.muzzleFlash.shaderParms[ SHADERPARM_GREEN] = this.flashColor.oGet(1);
            this.muzzleFlash.shaderParms[ SHADERPARM_BLUE] = this.flashColor.oGet(2);
            this.muzzleFlash.shaderParms[ SHADERPARM_TIMESCALE] = 1.0f;

            this.muzzleFlash.lightRadius.oSet(0, flashRadius);
            this.muzzleFlash.lightRadius.oSet(1, flashRadius);
            this.muzzleFlash.lightRadius.oSet(2, flashRadius);

            if (!flashPointLight) {
                this.muzzleFlash.target.oSet(flashTarget);
                this.muzzleFlash.up.oSet(flashUp);
                this.muzzleFlash.right.oSet(flashRight);
                this.muzzleFlash.end.oSet(flashTarget);
            }

            // the world muzzle flash is the same, just positioned differently
            this.worldMuzzleFlash = new renderLight_s(this.muzzleFlash);
            this.worldMuzzleFlash.suppressLightInViewID = this.owner.entityNumber + 1;
            this.worldMuzzleFlash.allowLightInViewID = 0;
            this.worldMuzzleFlash.lightId = LIGHTID_WORLD_MUZZLE_FLASH + this.owner.entityNumber;

            //-----------------------------------
            this.nozzleFx = this.weaponDef.dict.GetBool("nozzleFx");
            this.nozzleFxFade = this.weaponDef.dict.GetInt("nozzleFxFade", "1500");
            this.nozzleGlowColor.oSet(this.weaponDef.dict.GetVector("nozzleGlowColor", "1 1 1"));
            this.nozzleGlowRadius = this.weaponDef.dict.GetFloat("nozzleGlowRadius", "10");
            this.weaponDef.dict.GetString("mtr_nozzleGlowShader", "", shader);
            this.nozzleGlowShader = declManager.FindMaterial(shader[0], false);

            // get the melee damage def
            this.meleeDistance = this.weaponDef.dict.GetFloat("melee_distance");
            this.meleeDefName.oSet(this.weaponDef.dict.GetString("def_melee"));
            if (this.meleeDefName.Length() != 0) {
                this.meleeDef = gameLocal.FindEntityDef(this.meleeDefName.toString(), false);
                if (null == this.meleeDef) {
                    gameLocal.Error("Unknown melee '%s'", this.meleeDefName);
                }
            }

            // get the brass def
            this.brassDict.Clear();
            this.brassDelay = this.weaponDef.dict.GetInt("ejectBrassDelay", "0");
            brassDefName = this.weaponDef.dict.GetString("def_ejectBrass");

            if (isNotNullOrEmpty(brassDefName)) {
                final idDeclEntityDef brassDef = gameLocal.FindEntityDef(brassDefName, false);
                if (null == brassDef) {
                    gameLocal.Warning("Unknown brass '%s'", brassDefName);
                } else {
                    this.brassDict.oSet(brassDef.dict);
                }
            }

            if ((this.ammoType < 0) || (this.ammoType >= AMMO_NUMTYPES)) {
                gameLocal.Warning("Unknown ammotype in object '%s'", objectName);
            }

            this.ammoClip = ammoinclip;
            if ((this.ammoClip < 0) || (this.ammoClip > this.clipSize)) {
                // first time using this weapon so have it fully loaded to start
                this.ammoClip = this.clipSize;
                ammoAvail = this.owner.inventory.HasAmmo(this.ammoType, this.ammoRequired);
                if (this.ammoClip > ammoAvail) {
                    this.ammoClip = ammoAvail;
                }
            }

            this.renderEntity.gui[ 0] = null;
            guiName = this.weaponDef.dict.GetString("gui");
            if (isNotNullOrEmpty(guiName)) {
                this.renderEntity.gui[ 0] = uiManager.FindGui(guiName, true, false, true);
            }

            this.zoomFov = this.weaponDef.dict.GetInt("zoomFov", "70");
            this.berserk = this.weaponDef.dict.GetInt("berserk", "2");

            this.weaponAngleOffsetAverages = this.weaponDef.dict.GetInt("weaponAngleOffsetAverages", "10");
            this.weaponAngleOffsetScale = this.weaponDef.dict.GetFloat("weaponAngleOffsetScale", "0.25");
            this.weaponAngleOffsetMax = this.weaponDef.dict.GetFloat("weaponAngleOffsetMax", "10");

            this.weaponOffsetTime = this.weaponDef.dict.GetFloat("weaponOffsetTime", "400");
            this.weaponOffsetScale = this.weaponDef.dict.GetFloat("weaponOffsetScale", "0.005");

            if (!this.weaponDef.dict.GetString("weapon_scriptobject", null, objectType)) {
                gameLocal.Error("No 'weapon_scriptobject' set on '%s'.", objectName);
            }

            // setup script object
            if (!this.scriptObject.SetType(objectType[0])) {
                gameLocal.Error("Script object '%s' not found on weapon '%s'.", objectType[0], objectName);
            }

            this.WEAPON_ATTACK.LinkTo(this.scriptObject, "WEAPON_ATTACK");
            this.WEAPON_RELOAD.LinkTo(this.scriptObject, "WEAPON_RELOAD");
            this.WEAPON_NETRELOAD.LinkTo(this.scriptObject, "WEAPON_NETRELOAD");
            this.WEAPON_NETENDRELOAD.LinkTo(this.scriptObject, "WEAPON_NETENDRELOAD");
            if (!ID_DEMO_BUILD) {
				this.WEAPON_NETFIRING.LinkTo(this.scriptObject, "WEAPON_NETFIRING");
			}
            this.WEAPON_RAISEWEAPON.LinkTo(this.scriptObject, "WEAPON_RAISEWEAPON");
            this.WEAPON_LOWERWEAPON.LinkTo(this.scriptObject, "WEAPON_LOWERWEAPON");

            this.spawnArgs.oSet(this.weaponDef.dict);

            shader[0] = this.spawnArgs.GetString("snd_hum");
            if (isNotNullOrEmpty(shader[0])) {
                this.sndHum = declManager.FindSound(shader[0]);
                StartSoundShader(this.sndHum, SND_CHANNEL_BODY, 0, false, null);
            }

            this.isLinked = true;

            // call script object's constructor
            ConstructScriptObject();

            // make sure we have the correct skin
            UpdateSkin();
        }

        public boolean IsLinked() {
            return this.isLinked;
        }

        public boolean IsWorldModelReady() {
            return (this.worldModel.GetEntity() != null);
        }

        /* **********************************************************************

         GUIs

         ***********************************************************************/
        public String Icon() {
            return this.icon.toString();
        }

        public void UpdateGUI() {
            if (null == this.renderEntity.gui[ 0]) {
                return;
            }

            if (this.status == WP_HOLSTERED) {
                return;
            }

            if (this.owner.weaponGone) {
                // dropping weapons was implemented wierd, so we have to not update the gui when it happens or we'll get a negative ammo count
                return;
            }

            if (gameLocal.localClientNum != this.owner.entityNumber) {
                // if updating the hud for a followed client
                if ((gameLocal.localClientNum >= 0) && (gameLocal.entities[ gameLocal.localClientNum] != null) && gameLocal.entities[ gameLocal.localClientNum].IsType(idPlayer.class)) {
                    final idPlayer p = (idPlayer) gameLocal.entities[ gameLocal.localClientNum];
                    if (!p.spectating || (p.spectator != this.owner.entityNumber)) {
                        return;
                    }
                } else {
                    return;
                }
            }

            final int inclip = AmmoInClip();
            final int ammoamount = AmmoAvailable();

            if (ammoamount < 0) {
                // show infinite ammo
                this.renderEntity.gui[ 0].SetStateString("player_ammo", "");
            } else {
                // show remaining ammo
                this.renderEntity.gui[0].SetStateString("player_totalammo", va("%d", ammoamount - inclip));
                this.renderEntity.gui[0].SetStateString("player_ammo", ClipSize() != 0 ? va("%d", inclip) : "--");
                this.renderEntity.gui[0].SetStateString("player_clips", ClipSize() != 0 ? va("%d", ammoamount / ClipSize()) : "--");
                this.renderEntity.gui[0].SetStateString("player_allammo", va("%d/%d", inclip, ammoamount - inclip));
            }
            this.renderEntity.gui[0].SetStateBool("player_ammo_empty", (ammoamount == 0));
            this.renderEntity.gui[0].SetStateBool("player_clip_empty", (inclip == 0));
            this.renderEntity.gui[0].SetStateBool("player_clip_low", (inclip <= this.lowAmmo));
        }

        @Override
        public void SetModel(final String modelname) {
            assert (modelname != null);

            if (this.modelDefHandle >= 0) {
                gameRenderWorld.RemoveDecals(this.modelDefHandle);
            }

            this.renderEntity.hModel = this.animator.SetModel(modelname);
            if (this.renderEntity.hModel != null) {
                this.renderEntity.customSkin = this.animator.ModelDef().GetDefaultSkin();
                {
                    final idJointMat[][] joints = {null};
                    this.renderEntity.numJoints = this.animator.GetJoints(joints);
                    this.renderEntity.joints = joints[0];
                }
            } else {
                this.renderEntity.customSkin = null;
                this.renderEntity.callback = null;
                this.renderEntity.numJoints = 0;
                this.renderEntity.joints = null;
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
                if (this.animator.GetJointTransform(jointHandle, gameLocal.time, offset, axis)) {
                    offset.oSet(offset.oMultiply(this.viewWeaponAxis).oPlus(this.viewWeaponOrigin));
                    axis.oSet(axis.oMultiply(this.viewWeaponAxis));
                    return true;
                }
            } else {
                // world model
                if ((this.worldModel.GetEntity() != null) && this.worldModel.GetEntity().GetAnimator().GetJointTransform(jointHandle, gameLocal.time, offset, axis)) {
                    offset.oSet(this.worldModel.GetEntity().GetPhysics().GetOrigin().oPlus(offset.oMultiply(this.worldModel.GetEntity().GetPhysics().GetAxis())));
                    axis.oSet(axis.oMultiply(this.worldModel.GetEntity().GetPhysics().GetAxis()));
                    return true;
                }
            }
            offset.oSet(this.viewWeaponOrigin);
            axis.oSet(this.viewWeaponAxis);
            return false;
        }

        public void SetPushVelocity(final idVec3 pushVelocity) {
            this.pushVelocity.oSet(pushVelocity);
        }

        public boolean UpdateSkin() {
            function_t func;

            if (!this.isLinked) {
                return false;
            }

            func = this.scriptObject.GetFunction("UpdateSkin");
            if (null == func) {
                common.Warning("Can't find function 'UpdateSkin' in object '%s'", this.scriptObject.GetTypeName());
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
            if (this.isLinked) {
                this.WEAPON_RAISEWEAPON.operator(true);
            }
        }

        public void PutAway() {
            this.hasBloodSplat = false;
            if (this.isLinked) {
                this.WEAPON_LOWERWEAPON.operator(true);
            }
        }

        /*
         ================
         idWeapon::Reload
         NOTE: this is only for impulse-triggered reload, auto reload is scripted
         ================
         */
        public void Reload() {
            if (this.isLinked) {
                this.WEAPON_RELOAD.operator(true);
            }
        }

        public void LowerWeapon() {
            if (!this.hide) {
                this.hideStart = 0.0f;
                this.hideEnd = this.hideDistance;
                if ((gameLocal.time - this.hideStartTime) < this.hideTime) {
                    this.hideStartTime = gameLocal.time - (this.hideTime - (gameLocal.time - this.hideStartTime));
                } else {
                    this.hideStartTime = gameLocal.time;
                }
                this.hide = true;
            }
        }

        public void RaiseWeapon() {
            Show();

            if (this.hide) {
                this.hideStart = this.hideDistance;
                this.hideEnd = 0.0f;
                if ((gameLocal.time - this.hideStartTime) < this.hideTime) {
                    this.hideStartTime = gameLocal.time - (this.hideTime - (gameLocal.time - this.hideStartTime));
                } else {
                    this.hideStartTime = gameLocal.time;
                }
                this.hide = false;
            }
        }

        public void HideWeapon() {
            Hide();
            if (this.worldModel.GetEntity() != null) {
                this.worldModel.GetEntity().Hide();
            }
            this.muzzleFlashEnd = 0;
        }

        public void ShowWeapon() {
            Show();
            if (this.worldModel.GetEntity() != null) {
                this.worldModel.GetEntity().Show();
            }
            if (this.lightOn) {
                MuzzleFlashLight();
            }
        }

        public void HideWorldModel() {
            if (this.worldModel.GetEntity() != null) {
                this.worldModel.GetEntity().Hide();
            }
        }

        public void ShowWorldModel() {
            if (this.worldModel.GetEntity() != null) {
                this.worldModel.GetEntity().Show();
            }
        }

        public void OwnerDied() {
            if (this.isLinked) {
                SetState("OwnerDied", 0);
                this.thread.Execute();
            }

            Hide();
            if (this.worldModel.GetEntity() != null) {
                this.worldModel.GetEntity().Hide();
            }

            // don't clear the weapon immediately since the owner might have killed himself by firing the weapon
            // within the current stack frame
            PostEventMS(EV_Weapon_Clear, 0);
        }

        public void BeginAttack() {
            if (this.status != WP_OUTOFAMMO) {
                this.lastAttack = gameLocal.time;
            }

            if (!this.isLinked) {
                return;
            }

            if (!this.WEAPON_ATTACK.operator()) {
                if (this.sndHum != null) {
                    StopSound(etoi(SND_CHANNEL_BODY), false);
                }
            }
            this.WEAPON_ATTACK.operator(true);
        }

        public void EndAttack() {
            if (!this.WEAPON_ATTACK.IsLinked()) {
                return;
            }
            if (this.WEAPON_ATTACK.operator()) {
                this.WEAPON_ATTACK.operator(false);
                if (this.sndHum != null) {
                    StartSoundShader(this.sndHum, SND_CHANNEL_BODY, 0, false, null);
                }
            }
        }

        public boolean IsReady() {
            return !this.hide && !IsHidden() && ((this.status == WP_RELOAD) || (this.status == WP_READY) || (this.status == WP_OUTOFAMMO));
        }

        public boolean IsReloading() {
            return (this.status == WP_RELOAD);
        }

        public boolean IsHolstered() {
            return (this.status == WP_HOLSTERED);
        }

        public boolean ShowCrosshair() {
            return !(this.state.equals(WP_RISING) || this.state.equals(WP_LOWERING) || this.state.equals(WP_HOLSTERED));
        }

        public idEntity DropItem(final idVec3 velocity, int activateDelay, int removeDelay, boolean died) {
            if ((null == this.weaponDef) || (null == this.worldModel.GetEntity())) {
                return null;
            }
            if (!this.allowDrop) {
                return null;
            }
            final String classname = this.weaponDef.dict.GetString("def_dropItem");
            if (!isNotNullOrEmpty(classname)) {
                return null;
            }
            StopSound(etoi(SND_CHANNEL_BODY), true);
            StopSound(etoi(SND_CHANNEL_BODY3), true);

            return idMoveableItem.DropItem(classname, this.worldModel.GetEntity().GetPhysics().GetOrigin(), this.worldModel.GetEntity().GetPhysics().GetAxis(), velocity, activateDelay, removeDelay);
        }

        public boolean CanDrop() {
            if ((null == this.weaponDef) || (null == this.worldModel.GetEntity())) {
                return false;
            }
            final String classname = this.weaponDef.dict.GetString("def_dropItem");
            if (!isNotNullOrEmpty(classname)) {
                return false;
            }
            return true;
        }

        public void WeaponStolen() {
            assert (!gameLocal.isClient);
            if (this.projectileEnt != null) {
                if (this.isLinked) {
                    SetState("WeaponStolen", 0);
                    this.thread.Execute();
                }
                this.projectileEnt = null;
            }

            // set to holstered so we can switch weapons right away
            this.status = WP_HOLSTERED;

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

            this.thread.EndThread();

            // call script object's constructor
            constructor = this.scriptObject.GetConstructor();
            if (null == constructor) {
                gameLocal.Error("Missing constructor on '%s' for weapon", this.scriptObject.GetTypeName());
            }

            // init the script object's data
            this.scriptObject.ClearObject();
            this.thread.CallFunction(this, constructor, true);
            this.thread.Execute();

            return this.thread;
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

            if (NOT(this.thread)) {
                return;
            }

            // don't bother calling the script object's destructor on map shutdown
            if (gameLocal.GameState() == GAMESTATE_SHUTDOWN) {
                return;
            }

            this.thread.EndThread();

            // call script object's destructor
            destructor = this.scriptObject.GetDestructor();
            if (destructor != null) {
                // start a thread that will run immediately and end
                this.thread.CallFunction(this, destructor, true);
                this.thread.Execute();
                this.thread.EndThread();
            }

            // clear out the object's memory
            this.scriptObject.ClearObject();
        }

        public void SetState(final String statename, int blendFrames) {
            function_t func;

            if (!this.isLinked) {
                return;
            }

            func = this.scriptObject.GetFunction(statename);
            if (null == func) {
                assert (false);
                gameLocal.Error("Can't find function '%s' in object '%s'", statename, this.scriptObject.GetTypeName());
            }

            this.thread.CallFunction(this, func, true);
            this.state.oSet(statename);

            this.animBlendFrames = blendFrames;
            if (g_debugWeapon.GetBool()) {
                gameLocal.Printf("%d: weapon state : %s\n", gameLocal.time, statename);
            }

            this.idealState.oSet("");
        }

        public void UpdateScript() {
            int count;

            if (!this.isLinked) {
                return;
            }

            // only update the script on new frames
            if (!gameLocal.isNewFrame) {
                return;
            }

            if (this.idealState.Length() != 0) {
                SetState(this.idealState.toString(), this.animBlendFrames);
            }

            // update script state, which may call Event_LaunchProjectiles, among other things
            count = 10;
            while ((this.thread.Execute() || (this.idealState.Length() != 0)) && (count-- != 0)) {
                // happens for weapons with no clip (like grenades)
                if (this.idealState.Length() != 0) {
                    SetState(this.idealState.toString(), this.animBlendFrames);
                }
            }

            this.WEAPON_RELOAD.operator(false);
        }

        public void EnterCinematic() {
            StopSound(etoi(SND_CHANNEL_ANY), false);

            if (this.isLinked) {
                SetState("EnterCinematic", 0);
                this.thread.Execute();

                this.WEAPON_ATTACK.operator(false);
                this.WEAPON_RELOAD.operator(false);
                this.WEAPON_NETRELOAD.operator(false);
                this.WEAPON_NETENDRELOAD.operator(false);
                this.WEAPON_NETFIRING.operator(false);
                this.WEAPON_RAISEWEAPON.operator(false);
                this.WEAPON_LOWERWEAPON.operator(false);
            }

            this.disabled = true;

            LowerWeapon();
        }

        public void ExitCinematic() {
            this.disabled = false;

            if (this.isLinked) {
                SetState("ExitCinematic", 0);
                this.thread.Execute();
            }

            RaiseWeapon();
        }

        public void NetCatchup() {
            if (this.isLinked) {
                SetState("NetCatchup", 0);
                this.thread.Execute();
            }
        }

        /* **********************************************************************

         Visual presentation

         ***********************************************************************/
        public void PresentWeapon(boolean showViewModel) {
            this.playerViewOrigin.oSet(this.owner.firstPersonViewOrigin);
            this.playerViewAxis.oSet(this.owner.firstPersonViewAxis);

            // calculate weapon position based on player movement bobbing
            this.owner.CalculateViewWeaponPos(this.viewWeaponOrigin, this.viewWeaponAxis);

            // hide offset is for dropping the gun when approaching a GUI or NPC
            // This is simpler to manage than doing the weapon put-away animation
            if ((gameLocal.time - this.hideStartTime) < this.hideTime) {
                float frac = (float) (gameLocal.time - this.hideStartTime) / (float) this.hideTime;
                if (this.hideStart < this.hideEnd) {
                    frac = 1.0f - frac;
                    frac = 1.0f - (frac * frac);
                } else {
                    frac = frac * frac;
                }
                this.hideOffset = this.hideStart + ((this.hideEnd - this.hideStart) * frac);
            } else {
                this.hideOffset = this.hideEnd;
                if (this.hide && this.disabled) {
                    Hide();
                }
            }
            this.viewWeaponOrigin.oPluSet(this.viewWeaponAxis.oGet(2).oMultiply(this.hideOffset));

            // kick up based on repeat firing
            MuzzleRise(this.viewWeaponOrigin, this.viewWeaponAxis);

            // set the physics position and orientation
            GetPhysics().SetOrigin(this.viewWeaponOrigin);
            GetPhysics().SetAxis(this.viewWeaponAxis);
            UpdateVisuals();

            // update the weapon script
            UpdateScript();

            UpdateGUI();

            // update animation
            UpdateAnimation();

            // only show the surface in player view
            this.renderEntity.allowSurfaceInViewID = this.owner.entityNumber + 1;

            // crunch the depth range so it never pokes into walls this breaks the machine gun gui
            this.renderEntity.weaponDepthHack = true;

            // present the model
            if (showViewModel) {
                Present();
            } else {
                FreeModelDef();
            }

            if ((this.worldModel.GetEntity() != null) && (this.worldModel.GetEntity().GetRenderEntity() != null)) {
                // deal with the third-person visible world model
                // don't show shadows of the world model in first person
                if (gameLocal.isMultiplayer || g_showPlayerShadow.GetBool() || pm_thirdPerson.GetBool()) {
                    this.worldModel.GetEntity().GetRenderEntity().suppressShadowInViewID = 0;
                } else {
                    this.worldModel.GetEntity().GetRenderEntity().suppressShadowInViewID = this.owner.entityNumber + 1;
                    this.worldModel.GetEntity().GetRenderEntity().suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + this.owner.entityNumber;
                }
            }

            if (this.nozzleFx) {
                UpdateNozzleFx();
            }

            // muzzle smoke
            if (showViewModel && !this.disabled && (this.weaponSmoke != null) && (this.weaponSmokeStartTime != 0)) {
                // use the barrel joint if available
                if (this.barrelJointView != 0) {
                    GetGlobalJointTransform(true, this.barrelJointView, this.muzzleOrigin, this.muzzleAxis);
                } else {
                    // default to going straight out the view
                    this.muzzleOrigin.oSet(this.playerViewOrigin);
                    this.muzzleAxis.oSet(this.playerViewAxis);
                }
                // spit out a particle
                if (!gameLocal.smokeParticles.EmitSmoke(this.weaponSmoke, this.weaponSmokeStartTime, gameLocal.random.RandomFloat(), this.muzzleOrigin, this.muzzleAxis)) {
                    this.weaponSmokeStartTime = (this.continuousSmoke) ? gameLocal.time : 0;
                }
            }

            if (showViewModel && (this.strikeSmoke != null) && (this.strikeSmokeStartTime != 0)) {
                // spit out a particle
                if (!gameLocal.smokeParticles.EmitSmoke(this.strikeSmoke, this.strikeSmokeStartTime, gameLocal.random.RandomFloat(), this.strikePos, this.strikeAxis)) {
                    this.strikeSmokeStartTime = 0;
                }
            }

            // remove the muzzle flash light when it's done
            if ((!this.lightOn && (gameLocal.time >= this.muzzleFlashEnd)) || IsHidden()) {
                if (this.muzzleFlashHandle != -1) {
                    gameRenderWorld.FreeLightDef(this.muzzleFlashHandle);
                    this.muzzleFlashHandle = -1;
                }
                if (this.worldMuzzleFlashHandle != -1) {
                    gameRenderWorld.FreeLightDef(this.worldMuzzleFlashHandle);
                    this.worldMuzzleFlashHandle = -1;
                }
            }

            // update the muzzle flash light, so it moves with the gun
            if (this.muzzleFlashHandle != -1) {
                UpdateFlashPosition();
                gameRenderWorld.UpdateLightDef(this.muzzleFlashHandle, this.muzzleFlash);
                gameRenderWorld.UpdateLightDef(this.worldMuzzleFlashHandle, this.worldMuzzleFlash);

                // wake up monsters with the flashlight
                if (!gameLocal.isMultiplayer && this.lightOn && !this.owner.fl.notarget) {
                    AlertMonsters();
                }
            }

            // update the gui light
            if ((this.guiLight.lightRadius.oGet(0) != 0) && (this.guiLightJointView != INVALID_JOINT)) {
                GetGlobalJointTransform(true, this.guiLightJointView, this.guiLight.origin, this.guiLight.axis);

                if ((this.guiLightHandle != -1)) {
                    gameRenderWorld.UpdateLightDef(this.guiLightHandle, this.guiLight);
                } else {
                    this.guiLightHandle = gameRenderWorld.AddLightDef(this.guiLight);
                }
            }

            if ((this.status != WP_READY) && (this.sndHum != null)) {
                StopSound(etoi(SND_CHANNEL_BODY), false);
            }

            UpdateSound();
        }

        public int GetZoomFov() {
            return this.zoomFov;
        }

        public void GetWeaponAngleOffsets(int[] average, float[] scale, float[] max) {
            average[0] = this.weaponAngleOffsetAverages;
            scale[0] = this.weaponAngleOffsetScale;
            max[0] = this.weaponAngleOffsetMax;
        }

        public void GetWeaponTimeOffsets(float[] time, float[] scale) {
            time[0] = this.weaponOffsetTime;
            scale[0] = this.weaponOffsetScale;
        }

        public boolean BloodSplat(float size) {
            final float[] s = new float[1], c = new float[1];
            final idMat3 localAxis = new idMat3(), axistemp = new idMat3();
            final idVec3 localOrigin = new idVec3();
			idVec3 normal;

            if (this.hasBloodSplat) {
                return true;
            }

            this.hasBloodSplat = true;

            if (this.modelDefHandle < 0) {
                return false;
            }

            if (!GetGlobalJointTransform(true, this.ejectJointView, localOrigin, localAxis)) {
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

            final idPlane[] localPlane = new idPlane[2];

            localPlane[0].oSet(localAxis.oGet(0));//TODO:check what init value is you lazy arse.
            localPlane[0].oSet(3, -(localOrigin.oMultiply(localAxis.oGet(0))) + 0.5f);

            localPlane[1].oSet(localAxis.oGet(1));
            localPlane[1].oSet(3, -(localOrigin.oMultiply(localAxis.oGet(1))) + 0.5f);

            final idMaterial mtr = declManager.FindMaterial("textures/decals/duffysplatgun");

            gameRenderWorld.ProjectOverlay(this.modelDefHandle, localPlane, mtr);

            return true;
        }

        /* **********************************************************************

         Ammo

         ***********************************************************************/
        public static int /*ammo_t*/ GetAmmoNumForName(final String ammoname) {
            final int[] num = new int[1];
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
            return this.ammoType;
        }

        public int AmmoAvailable() {
            if (this.owner != null) {
                return this.owner.inventory.HasAmmo(this.ammoType, this.ammoRequired);
            } else {
                return 0;
            }
        }

        public int AmmoInClip() {
            return this.ammoClip;
        }

        public void ResetAmmoClip() {
            this.ammoClip = -1;
        }

        public int ClipSize() {
            return this.clipSize;
        }

        public int LowAmmo() {
            return this.lowAmmo;
        }

        public int AmmoRequired() {
            return this.ammoRequired;
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteBits(this.ammoClip, ASYNC_PLAYER_INV_CLIP_BITS);
            msg.WriteBits(this.worldModel.GetSpawnId(), 32);
            msg.WriteBits(btoi(this.lightOn), 1);
            msg.WriteBits(this.isFiring ? 1 : 0, 1);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            this.ammoClip = msg.ReadBits(ASYNC_PLAYER_INV_CLIP_BITS);
            this.worldModel.SetSpawnId(msg.ReadBits(32));
            final boolean snapLight = msg.ReadBits(1) != 0;
            this.isFiring = msg.ReadBits(1) != 0;

            // WEAPON_NETFIRING is only turned on for other clients we're predicting. not for local client
            if ((this.owner != null) && (gameLocal.localClientNum != this.owner.entityNumber) && this.WEAPON_NETFIRING.IsLinked()) {

                // immediately go to the firing state so we don't skip fire animations
                if (!this.WEAPON_NETFIRING.operator() && this.isFiring) {
                    this.idealState.oSet("Fire");
                }

                // immediately switch back to idle
                if (this.WEAPON_NETFIRING.operator() && !this.isFiring) {
                    this.idealState.oSet("Idle");
                }

                this.WEAPON_NETFIRING.operator(this.isFiring);
            }

            if (snapLight != this.lightOn) {
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
                    if ((gameLocal.time - time) < 1000) {
                        if (this.WEAPON_NETRELOAD.IsLinked()) {
                            this.WEAPON_NETRELOAD.operator(true);
                            this.WEAPON_NETENDRELOAD.operator(false);
                        }
                    }
                    return true;
                }
                case EVENT_ENDRELOAD: {
                    if (this.WEAPON_NETENDRELOAD.IsLinked()) {
                        this.WEAPON_NETENDRELOAD.operator(true);
                    }
                    return true;
                }
                case EVENT_CHANGESKIN: {
                    final int index = gameLocal.ClientRemapDecl(DECL_SKIN, msg.ReadLong());
                    this.renderEntity.customSkin = (index != -1) ? (idDeclSkin) declManager.DeclByIndex(DECL_SKIN, index) : null;
                    UpdateVisuals();
                    if (this.worldModel.GetEntity() != null) {
                        this.worldModel.GetEntity().SetSkin(this.renderEntity.customSkin);
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
            final trace_s[] tr = {null};
            idEntity ent;
            final idVec3 end = this.muzzleFlash.origin.oPlus(this.muzzleFlash.axis.oMultiply(this.muzzleFlash.target));

            gameLocal.clip.TracePoint(tr, this.muzzleFlash.origin, end, CONTENTS_OPAQUE | MASK_SHOT_RENDERMODEL | CONTENTS_FLASHLIGHT_TRIGGER, this.owner);
            if (g_debugWeapon.GetBool()) {
                gameRenderWorld.DebugLine(colorYellow, this.muzzleFlash.origin, end, 0);
                gameRenderWorld.DebugArrow(colorGreen, this.muzzleFlash.origin, tr[0].endpos, 2, 0);
            }

            if (tr[0].fraction < 1.0f) {
                ent = gameLocal.GetTraceEntity(tr[0]);
                if (ent.IsType(idAI.class)) {
                    ((idAI) ent).TouchedByFlashlight(this.owner);
                } else if (ent.IsType(idTrigger.class)) {
                    ent.Signal(SIG_TOUCH);
                    ent.ProcessEvent(EV_Touch, this.owner, tr[0]);
                }
            }

            // jitter the trace to try to catch cases where a trace down the center doesn't hit the monster
            end.oPluSet(this.muzzleFlash.axis.oMultiply(this.muzzleFlash.right.oMultiply(idMath.Sin16(MS2SEC(gameLocal.time) * 31.34f))));
            end.oPluSet(this.muzzleFlash.axis.oMultiply(this.muzzleFlash.up.oMultiply(idMath.Sin16(MS2SEC(gameLocal.time) * 12.17f))));
            gameLocal.clip.TracePoint(tr, this.muzzleFlash.origin, end, CONTENTS_OPAQUE | MASK_SHOT_RENDERMODEL | CONTENTS_FLASHLIGHT_TRIGGER, this.owner);
            if (g_debugWeapon.GetBool()) {
                gameRenderWorld.DebugLine(colorYellow, this.muzzleFlash.origin, end, 0);
                gameRenderWorld.DebugArrow(colorGreen, this.muzzleFlash.origin, tr[0].endpos, 2, 0);
            }

            if (tr[0].fraction < 1.0f) {
                ent = gameLocal.GetTraceEntity(tr[0]);
                if (ent.IsType(idAI.class)) {
                    ((idAI) ent).TouchedByFlashlight(this.owner);
                } else if (ent.IsType(idTrigger.class)) {
                    ent.Signal(SIG_TOUCH);
                    ent.ProcessEvent(EV_Touch, this.owner, tr[0]);
                }
            }
        }

        // Visual presentation
        private void InitWorldModel(final idDeclEntityDef def) {
            idEntity ent;

            ent = this.worldModel.GetEntity();

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
                ent.BindToJoint(this.owner, attach, true);
                ent.GetPhysics().SetOrigin(getVec3_origin());
                ent.GetPhysics().SetAxis(getMat3_identity());

                // supress model in player views, but allow it in mirrors and remote views
                final renderEntity_s worldModelRenderEntity = ent.GetRenderEntity();
                if (worldModelRenderEntity != null) {
                    worldModelRenderEntity.suppressSurfaceInViewID = this.owner.entityNumber + 1;
                    worldModelRenderEntity.suppressShadowInViewID = this.owner.entityNumber + 1;
                    worldModelRenderEntity.suppressShadowInLightID = LIGHTID_VIEW_MUZZLE_FLASH + this.owner.entityNumber;
                }
            } else {
                ent.SetModel("");
                ent.Hide();
            }

            this.flashJointWorld = ent.GetAnimator().GetJointHandle("flash");
            this.barrelJointWorld = ent.GetAnimator().GetJointHandle("muzzle");
            this.ejectJointWorld = ent.GetAnimator().GetJointHandle("eject");
        }

        private void MuzzleFlashLight() {

            if (!this.lightOn && (!g_muzzleFlash.GetBool() || (0 == this.muzzleFlash.lightRadius.oGet(0)))) {
                return;
            }

            if (this.flashJointView == INVALID_JOINT) {
                return;
            }

            UpdateFlashPosition();

            // these will be different each fire
            this.muzzleFlash.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            this.muzzleFlash.shaderParms[ SHADERPARM_DIVERSITY] = this.renderEntity.shaderParms[SHADERPARM_DIVERSITY];

            this.worldMuzzleFlash.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            this.worldMuzzleFlash.shaderParms[ SHADERPARM_DIVERSITY] = this.renderEntity.shaderParms[SHADERPARM_DIVERSITY];

            // the light will be removed at this time
            this.muzzleFlashEnd = gameLocal.time + this.flashTime;

            if (this.muzzleFlashHandle != -1) {
                gameRenderWorld.UpdateLightDef(this.muzzleFlashHandle, this.muzzleFlash);
                gameRenderWorld.UpdateLightDef(this.worldMuzzleFlashHandle, this.worldMuzzleFlash);
            } else {
                this.muzzleFlashHandle = gameRenderWorld.AddLightDef(this.muzzleFlash);
                this.worldMuzzleFlashHandle = gameRenderWorld.AddLightDef(this.worldMuzzleFlash);
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

            time = this.kick_endtime - gameLocal.time;
            if (time <= 0) {
                return;
            }

            if (this.muzzle_kick_maxtime <= 0) {
                return;
            }

            if (time > this.muzzle_kick_maxtime) {
                time = this.muzzle_kick_maxtime;
            }

            amount = (float) time / (float) this.muzzle_kick_maxtime;
            ang = this.muzzle_kick_angles.oMultiply(amount);
            offset = this.muzzle_kick_offset.oMultiply(amount);

            origin.oSet(origin.oMinus(axis.oMultiply(offset)));
            axis.oSet(ang.ToMat3().oMultiply(axis));
        }

        private void UpdateNozzleFx() {
            if (!this.nozzleFx) {
                return;
            }

            //
            // shader parms
            //
            final int la = (gameLocal.time - this.lastAttack) + 1;
            float s = 1.0f;
            float l = 0.0f;
            if (la < this.nozzleFxFade) {
                s = ((float) la / this.nozzleFxFade);
                l = 1.0f - s;
            }
            this.renderEntity.shaderParms[5] = s;
            this.renderEntity.shaderParms[6] = l;

            if (this.ventLightJointView == INVALID_JOINT) {
                return;
            }

            //
            // vent light
            //
            if (this.nozzleGlowHandle == -1) {
//		memset(&nozzleGlow, 0, sizeof(nozzleGlow));
                this.nozzleGlow = new renderLight_s();
                if (this.owner != null) {
                    this.nozzleGlow.allowLightInViewID = this.owner.entityNumber + 1;
                }
                this.nozzleGlow.pointLight = true;
                this.nozzleGlow.noShadows = true;
                this.nozzleGlow.lightRadius.x = this.nozzleGlowRadius;
                this.nozzleGlow.lightRadius.y = this.nozzleGlowRadius;
                this.nozzleGlow.lightRadius.z = this.nozzleGlowRadius;
                this.nozzleGlow.shader = this.nozzleGlowShader;
                this.nozzleGlow.shaderParms[ SHADERPARM_TIMESCALE] = 1.0f;
                this.nozzleGlow.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                GetGlobalJointTransform(true, this.ventLightJointView, this.nozzleGlow.origin, this.nozzleGlow.axis);
                this.nozzleGlowHandle = gameRenderWorld.AddLightDef(this.nozzleGlow);
            }

            GetGlobalJointTransform(true, this.ventLightJointView, this.nozzleGlow.origin, this.nozzleGlow.axis);

            this.nozzleGlow.shaderParms[ SHADERPARM_RED] = this.nozzleGlowColor.x * s;
            this.nozzleGlow.shaderParms[ SHADERPARM_GREEN] = this.nozzleGlowColor.y * s;
            this.nozzleGlow.shaderParms[ SHADERPARM_BLUE] = this.nozzleGlowColor.z * s;
            gameRenderWorld.UpdateLightDef(this.nozzleGlowHandle, this.nozzleGlow);
        }

        private void UpdateFlashPosition() {
            // the flash has an explicit joint for locating it
            GetGlobalJointTransform(true, this.flashJointView, this.muzzleFlash.origin, this.muzzleFlash.axis);

            // if the desired point is inside or very close to a wall, back it up until it is clear
            final idVec3 start = this.muzzleFlash.origin.oMinus(this.playerViewAxis.oGet(0).oMultiply(16));
            final idVec3 end = this.muzzleFlash.origin.oPlus(this.playerViewAxis.oGet(0).oMultiply(8));
            final trace_s[] tr = {null};
            gameLocal.clip.TracePoint(tr, start, end, MASK_SHOT_RENDERMODEL, this.owner);
            // be at least 8 units away from a solid
            this.muzzleFlash.origin = tr[0].endpos.oMinus(this.playerViewAxis.oGet(0).oMultiply(8));

            // put the world muzzle flash on the end of the joint, no matter what
            GetGlobalJointTransform(false, this.flashJointWorld, this.worldMuzzleFlash.origin, this.worldMuzzleFlash.axis);
        }

        /* **********************************************************************

         Script events

         ***********************************************************************/
        private void Event_Clear() {
            Clear();
        }

        private void Event_GetOwner() {
            idThread.ReturnEntity(this.owner);
        }

        private void Event_WeaponState(final idEventArg<String> _statename, idEventArg<Integer> blendFrames) {
            final String statename = _statename.value;
            function_t func;

            func = this.scriptObject.GetFunction(statename);
            if (null == func) {
                assert (false);
                gameLocal.Error("Can't find function '%s' in object '%s'", statename, this.scriptObject.GetTypeName());
            }

            this.idealState.oSet(statename);

            if (0 == this.idealState.Icmp("Fire")) {
                this.isFiring = true;
            } else {
                this.isFiring = false;
            }

            this.animBlendFrames = blendFrames.value;
            this.thread.DoneProcessing();
        }
//
//        private void Event_SetWeaponStatus(float newStatus);
//

        private void Event_WeaponReady() {
            this.status = WP_READY;
            if (this.isLinked) {
                this.WEAPON_RAISEWEAPON.operator(false);
            }
            if (this.sndHum != null) {
                StartSoundShader(this.sndHum, SND_CHANNEL_BODY, 0, false, null);
            }

        }

        private void Event_WeaponOutOfAmmo() {
            this.status = WP_OUTOFAMMO;
            if (this.isLinked) {
                this.WEAPON_RAISEWEAPON.operator(false);
            }
        }

        private void Event_WeaponReloading() {
            this.status = WP_RELOAD;
        }

        private void Event_WeaponHolstered() {
            this.status = WP_HOLSTERED;
            if (this.isLinked) {
                this.WEAPON_LOWERWEAPON.operator(false);
            }
        }

        private void Event_WeaponRising() {
            this.status = WP_RISING;
            if (this.isLinked) {
                this.WEAPON_LOWERWEAPON.operator(false);
            }
            this.owner.WeaponRisingCallback();
        }

        private void Event_WeaponLowering() {
            this.status = WP_LOWERING;
            if (this.isLinked) {
                this.WEAPON_RAISEWEAPON.operator(false);
            }
            this.owner.WeaponLoweringCallback();
        }

        private void Event_UseAmmo(idEventArg<Integer> _amount) {
            final int amount = _amount.value;
            if (gameLocal.isClient) {
                return;
            }

            this.owner.inventory.UseAmmo(this.ammoType, (this.powerAmmo) ? amount : (amount * this.ammoRequired));
            if ((this.clipSize != 0) && (this.ammoRequired != 0)) {
                this.ammoClip -= this.powerAmmo ? amount : (amount * this.ammoRequired);
                if (this.ammoClip < 0) {
                    this.ammoClip = 0;
                }
            }
        }

        private void Event_AddToClip(idEventArg<Integer> amount) {
            int ammoAvail;

            if (gameLocal.isClient) {
                return;
            }

            this.ammoClip += amount.value;
            if (this.ammoClip > this.clipSize) {
                this.ammoClip = this.clipSize;
            }

            ammoAvail = this.owner.inventory.HasAmmo(this.ammoType, this.ammoRequired);
            if (this.ammoClip > ammoAvail) {
                this.ammoClip = ammoAvail;
            }
        }

        private void Event_AmmoInClip() {
            final int ammo = AmmoInClip();
            idThread.ReturnFloat(ammo);
        }

        private void Event_AmmoAvailable() {
            final int ammoAvail = this.owner.inventory.HasAmmo(this.ammoType, this.ammoRequired);
            idThread.ReturnFloat(ammoAvail);
        }

        private void Event_TotalAmmoCount() {
            final int ammoAvail = this.owner.inventory.HasAmmo(this.ammoType, 1);
            idThread.ReturnFloat(ammoAvail);
        }

        private void Event_ClipSize() {
            idThread.ReturnFloat(this.clipSize);
        }

        private void Event_PlayAnim(idEventArg<Integer> _channel, final idEventArg<String> _animname) {
            final int channel = _channel.value;
            final String animname = _animname.value;
            int anim;

            anim = this.animator.GetAnim(animname);
            if (0 == anim) {
                gameLocal.Warning("missing '%s' animation on '%s' (%s)", animname, this.name, GetEntityDefName());
                this.animator.Clear(channel, gameLocal.time, FRAME2MS(this.animBlendFrames));
                this.animDoneTime = 0;
            } else {
                if (!((this.owner != null) && (this.owner.GetInfluenceLevel() != 0))) {
                    Show();
                }
                this.animator.PlayAnim(channel, anim, gameLocal.time, FRAME2MS(this.animBlendFrames));
                this.animDoneTime = this.animator.CurrentAnim(channel).GetEndTime();
                if (this.worldModel.GetEntity() != null) {
                    anim = this.worldModel.GetEntity().GetAnimator().GetAnim(animname);
                    if (anim != 0) {
                        this.worldModel.GetEntity().GetAnimator().PlayAnim(channel, anim, gameLocal.time, FRAME2MS(this.animBlendFrames));
                    }
                }
            }
            this.animBlendFrames = 0;
            idThread.ReturnInt(0);
        }

        private void Event_PlayCycle(idEventArg<Integer> _channel, final idEventArg<String> _animname) {
            final int channel = _channel.value;
            final String animname = _animname.value;
            int anim;

            anim = this.animator.GetAnim(animname);
            if (0 == anim) {
                gameLocal.Warning("missing '%s' animation on '%s' (%s)", animname, this.name, GetEntityDefName());
                this.animator.Clear(channel, gameLocal.time, FRAME2MS(this.animBlendFrames));
                this.animDoneTime = 0;
            } else {
                if (!((this.owner != null) && (this.owner.GetInfluenceLevel() != 0))) {
                    Show();
                }
                this.animator.CycleAnim(channel, anim, gameLocal.time, FRAME2MS(this.animBlendFrames));
                this.animDoneTime = this.animator.CurrentAnim(channel).GetEndTime();
                if (this.worldModel.GetEntity() != null) {
                    anim = this.worldModel.GetEntity().GetAnimator().GetAnim(animname);
                    this.worldModel.GetEntity().GetAnimator().CycleAnim(channel, anim, gameLocal.time, FRAME2MS(this.animBlendFrames));
                }
            }
            this.animBlendFrames = 0;
            idThread.ReturnInt(0);
        }

        private void Event_AnimDone(idEventArg<Integer> channel, idEventArg<Integer> blendFrames) {
            if ((this.animDoneTime - FRAME2MS(blendFrames.value)) <= gameLocal.time) {
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        private void Event_SetBlendFrames(idEventArg<Integer> channel, idEventArg<Integer> blendFrames) {
            this.animBlendFrames = blendFrames.value;
        }

        private void Event_GetBlendFrames(idEventArg<Integer> channel) {
            idThread.ReturnInt(this.animBlendFrames);
        }

        private void Event_Next() {
            // change to another weapon if possible
            this.owner.NextBestWeapon();
        }

        private void Event_SetSkin(final idEventArg<String> _skinname) {
            final String skinname = _skinname.value;
            idDeclSkin skinDecl;

            if (!isNotNullOrEmpty(skinname)) {
                skinDecl = null;
            } else {
                skinDecl = declManager.FindSkin(skinname);
            }

            this.renderEntity.customSkin = skinDecl;
            UpdateVisuals();

            if (this.worldModel.GetEntity() != null) {
                this.worldModel.GetEntity().SetSkin(skinDecl);
            }

            if (gameLocal.isServer) {
                final idBitMsg msg = new idBitMsg();
                final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);

                msg.Init(msgBuf, MAX_EVENT_PARAM_SIZE);
                msg.WriteLong((skinDecl != null) ? gameLocal.ServerRemapDecl(-1, DECL_SKIN, skinDecl.Index()) : -1);
                ServerSendEvent(EVENT_CHANGESKIN, msg, false, -1);
            }
        }

        private void Event_Flashlight(idEventArg<Integer> enable) {
            if (enable.value != 0) {
                this.lightOn = true;
                MuzzleFlashLight();
            } else {
                this.lightOn = false;
                this.muzzleFlashEnd = 0;
            }
        }

        private void Event_GetLightParm(idEventArg<Integer> _parmnum) {
            final int parmnum = _parmnum.value;
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                gameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            idThread.ReturnFloat(this.muzzleFlash.shaderParms[ parmnum]);
        }

        private void Event_SetLightParm(idEventArg<Integer> _parmnum, idEventArg<Float> _value) {
            final int parmnum = _parmnum.value;
            final float value = _value.value;
            if ((parmnum < 0) || (parmnum >= MAX_ENTITY_SHADER_PARMS)) {
                gameLocal.Error("shader parm index (%d) out of range", parmnum);
            }

            this.muzzleFlash.shaderParms[ parmnum] = value;
            this.worldMuzzleFlash.shaderParms[ parmnum] = value;
            UpdateVisuals();
        }

        private void Event_SetLightParms(idEventArg<Float> parm0, idEventArg<Float> parm1, idEventArg<Float> parm2, idEventArg<Float> parm3) {
            this.muzzleFlash.shaderParms[SHADERPARM_RED] = parm0.value;
            this.muzzleFlash.shaderParms[SHADERPARM_GREEN] = parm1.value;
            this.muzzleFlash.shaderParms[SHADERPARM_BLUE] = parm2.value;
            this.muzzleFlash.shaderParms[SHADERPARM_ALPHA] = parm3.value;

            this.worldMuzzleFlash.shaderParms[SHADERPARM_RED] = parm0.value;
            this.worldMuzzleFlash.shaderParms[SHADERPARM_GREEN] = parm1.value;
            this.worldMuzzleFlash.shaderParms[SHADERPARM_BLUE] = parm2.value;
            this.worldMuzzleFlash.shaderParms[SHADERPARM_ALPHA] = parm3.value;

            UpdateVisuals();
        }

        private void Event_LaunchProjectiles(idEventArg<Integer> _num_projectiles, idEventArg<Float> _spread, idEventArg<Float> fuseOffset, idEventArg<Float> launchPower, idEventArg<Float> _dmgPower) {
            final int num_projectiles = _num_projectiles.value;
            final float spread = _spread.value;
            float dmgPower = _dmgPower.value;
            idProjectile proj;
            final idEntity[] ent = {null};
            int i;
            idVec3 dir;
            float ang;
            float spin;
            final float[] distance = {0};
            final trace_s[] tr = {null};
            idVec3 start;
            idVec3 muzzle_pos = new idVec3();
            idBounds ownerBounds, projBounds;

            if (IsHidden()) {
                return;
            }

            if (0 == this.projectileDict.GetNumKeyVals()) {
                final String classname = this.weaponDef.dict.GetString("classname");
                gameLocal.Warning("No projectile defined on '%s'", classname);
                return;
            }

            // avoid all ammo considerations on an MP client
            if (!gameLocal.isClient) {

                // check if we're out of ammo or the clip is empty
                final int ammoAvail = this.owner.inventory.HasAmmo(this.ammoType, this.ammoRequired);
                if ((0 == ammoAvail) || ((this.clipSize != 0) && (this.ammoClip <= 0))) {
                    return;
                }

                // if this is a power ammo weapon ( currently only the bfg ) then make sure 
                // we only fire as much power as available in each clip
                if (this.powerAmmo) {
                    // power comes in as a float from zero to max
                    // if we use this on more than the bfg will need to define the max
                    // in the .def as opposed to just in the script so proper calcs
                    // can be done here. 
                    dmgPower = (int) dmgPower + 1;
                    if (dmgPower > this.ammoClip) {
                        dmgPower = this.ammoClip;
                    }
                }

                this.owner.inventory.UseAmmo(this.ammoType, (int) ((this.powerAmmo) ? dmgPower : this.ammoRequired));
                if ((this.clipSize != 0) && (this.ammoRequired != 0)) {
                    this.ammoClip -= this.powerAmmo ? dmgPower : 1;
                }

            }

            if (!this.silent_fire) {
                // wake up nearby monsters
                gameLocal.AlertAI(this.owner);
            }

            // set the shader parm to the time of last projectile firing,
            // which the gun material shaders can reference for single shot barrel glows, etc
            this.renderEntity.shaderParms[ SHADERPARM_DIVERSITY] = gameLocal.random.CRandomFloat();
            this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.realClientTime);

            if (this.worldModel.GetEntity() != null) {
                this.worldModel.GetEntity().SetShaderParm(SHADERPARM_DIVERSITY, this.renderEntity.shaderParms[ SHADERPARM_DIVERSITY]);
                this.worldModel.GetEntity().SetShaderParm(SHADERPARM_TIMEOFFSET, this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET]);
            }

            // calculate the muzzle position
            if ((this.barrelJointView != INVALID_JOINT) && this.projectileDict.GetBool("launchFromBarrel")) {
                // there is an explicit joint for the muzzle
                GetGlobalJointTransform(true, this.barrelJointView, this.muzzleOrigin, this.muzzleAxis);
            } else {
                // go straight out of the view
                this.muzzleOrigin.oSet(this.playerViewOrigin);
                this.muzzleAxis.oSet(this.playerViewAxis);
            }

            // add some to the kick time, incrementally moving repeat firing weapons back
            if (this.kick_endtime < gameLocal.realClientTime) {
                this.kick_endtime = gameLocal.realClientTime;
            }
            this.kick_endtime += this.muzzle_kick_time;
            if (this.kick_endtime > (gameLocal.realClientTime + this.muzzle_kick_maxtime)) {
                this.kick_endtime = gameLocal.realClientTime + this.muzzle_kick_maxtime;
            }

            if (gameLocal.isClient) {

                // predict instant hit projectiles
                if (this.projectileDict.GetBool("net_instanthit")) {
                    final float spreadRad = DEG2RAD(spread);
                    muzzle_pos = this.muzzleOrigin.oPlus(this.playerViewAxis.oGet(0).oMultiply(2.0f));
                    for (i = 0; i < num_projectiles; i++) {
                        ang = idMath.Sin(spreadRad * gameLocal.random.RandomFloat());
                        spin = DEG2RAD(360.0f) * gameLocal.random.RandomFloat();
                        dir = this.playerViewAxis.oGet(0).oPlus(this.playerViewAxis.oGet(2).oMultiply(ang * idMath.Sin(spin)).oMinus(this.playerViewAxis.oGet(1).oMultiply(ang * idMath.Cos(spin))));
                        dir.Normalize();
                        gameLocal.clip.Translation(tr, muzzle_pos, muzzle_pos.oPlus(dir.oMultiply(4096.0f)), null, getMat3_identity(), MASK_SHOT_RENDERMODEL, this.owner);
                        if (tr[0].fraction < 1.0f) {
                            idProjectile.ClientPredictionCollide(this, this.projectileDict, tr[0], getVec3_origin(), true);
                        }
                    }
                }

            } else {

                ownerBounds = this.owner.GetPhysics().GetAbsBounds();

                this.owner.AddProjectilesFired(num_projectiles);

                final float spreadRad = DEG2RAD(spread);
                for (i = 0; i < num_projectiles; i++) {
                    ang = idMath.Sin(spreadRad * gameLocal.random.RandomFloat());
                    spin = DEG2RAD(360.0f) * gameLocal.random.RandomFloat();
                    dir = this.playerViewAxis.oGet(0).oPlus(this.playerViewAxis.oGet(2).oMultiply(ang * idMath.Sin(spin)).oMinus(this.playerViewAxis.oGet(1).oMultiply(ang * idMath.Cos(spin))));
                    dir.Normalize();

                    if (this.projectileEnt != null) {
                        ent[0] = this.projectileEnt;
                        ent[0].Show();
                        ent[0].Unbind();
                        this.projectileEnt = null;
                    } else {
                        gameLocal.SpawnEntityDef(this.projectileDict, ent, false);
                    }

                    if ((null == ent[0]) || !ent[0].IsType(idProjectile.class)) {
                        final String projectileName = this.weaponDef.dict.GetString("def_projectile");
                        gameLocal.Error("'%s' is not an idProjectile", projectileName);
                    }

                    if (this.projectileDict.GetBool("net_instanthit")) {
                        // don't synchronize this on top of the already predicted effect
                        ent[0].fl.networkSync = false;
                    }

                    proj = (idProjectile) ent[0];
                    proj.Create(this.owner, this.muzzleOrigin, dir);

                    projBounds = proj.GetPhysics().GetBounds().Rotate(proj.GetPhysics().GetAxis());

                    // make sure the projectile starts inside the bounding box of the owner
                    if (i == 0) {
                        muzzle_pos = this.muzzleOrigin.oPlus(this.playerViewAxis.oGet(0).oMultiply(2.0f));
                        if ((ownerBounds.oMinus(projBounds)).RayIntersection(muzzle_pos, this.playerViewAxis.oGet(0), distance)) {
                            start = muzzle_pos.oPlus(this.playerViewAxis.oGet(0).oMultiply(distance[0]));
                        } else {
                            start = ownerBounds.GetCenter();
                        }
                        gameLocal.clip.Translation(tr, start, muzzle_pos, proj.GetPhysics().GetClipModel(), proj.GetPhysics().GetClipModel().GetAxis(), MASK_SHOT_RENDERMODEL, this.owner
                        );
                        muzzle_pos = tr[0].endpos;
                    }

                    proj.Launch(muzzle_pos, dir, this.pushVelocity, fuseOffset.value, launchPower.value, dmgPower);
                }

                // toss the brass
                PostEventMS(EV_Weapon_EjectBrass, this.brassDelay);
            }

            // add the light for the muzzleflash
            if (!this.lightOn) {
                MuzzleFlashLight();
            }

            this.owner.WeaponFireFeedback(this.weaponDef.dict);

            // reset muzzle smoke
            this.weaponSmokeStartTime = gameLocal.realClientTime;
        }

        private void Event_CreateProjectile() {
            if (!gameLocal.isClient) {
                final idEntity[] projectileEnt2 = {null};
                gameLocal.SpawnEntityDef(this.projectileDict, projectileEnt2, false);
                this.projectileEnt = projectileEnt2[0];
                if (this.projectileEnt != null) {
                    this.projectileEnt.SetOrigin(GetPhysics().GetOrigin());
                    this.projectileEnt.Bind(this.owner, false);
                    this.projectileEnt.Hide();
                }
                idThread.ReturnEntity(this.projectileEnt);
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
            if (!g_showBrass.GetBool() || !this.owner.CanShowWeaponViewmodel()) {
                return;
            }

            if ((this.ejectJointView == INVALID_JOINT) || (0 == this.brassDict.GetNumKeyVals())) {
                return;
            }

            if (gameLocal.isClient) {
                return;
            }

            final idMat3 axis = new idMat3();
            final idVec3 origin = new idVec3();
			idVec3 linear_velocity;
			final idVec3 angular_velocity = new idVec3();
            final idEntity[] ent = {null};

            if (!GetGlobalJointTransform(true, this.ejectJointView, origin, axis)) {
                return;
            }

            gameLocal.SpawnEntityDef(this.brassDict, ent, false);
            if (NOT(ent[0]) || !ent[0].IsType(idDebris.class)) {
                gameLocal.Error("'%s' is not an idDebris", this.weaponDef != null ? this.weaponDef.dict.GetString("def_ejectBrass") : "def_ejectBrass");
            }
            final idDebris debris = (idDebris) ent[0];
            debris.Create(this.owner, origin, axis);
            debris.Launch();

            linear_velocity = this.playerViewAxis.oGet(0).oPlus(this.playerViewAxis.oGet(1).oPlus(this.playerViewAxis.oGet(2))).oMultiply(40);
            angular_velocity.Set(10 * gameLocal.random.CRandomFloat(), 10 * gameLocal.random.CRandomFloat(), 10 * gameLocal.random.CRandomFloat());

            debris.GetPhysics().SetLinearVelocity(linear_velocity);
            debris.GetPhysics().SetAngularVelocity(angular_velocity);
        }

        private void Event_Melee() {
            idEntity ent;
            final trace_s[] tr = {null};

            if (null == this.meleeDef) {
                gameLocal.Error("No meleeDef on '%s'", this.weaponDef.dict.GetString("classname"));
            }

            if (!gameLocal.isClient) {
                final idVec3 start = this.playerViewOrigin;
                final idVec3 end = start.oPlus(this.playerViewAxis.oGet(0).oMultiply(this.meleeDistance * this.owner.PowerUpModifier(MELEE_DISTANCE)));
                gameLocal.clip.TracePoint(tr, start, end, MASK_SHOT_RENDERMODEL, this.owner);
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
                String hitSound = this.meleeDef.dict.GetString("snd_miss");

                if (ent != null) {

                    final float push = this.meleeDef.dict.GetFloat("push");
                    final idVec3 impulse = tr[0].c.normal.oMultiply(-push * this.owner.PowerUpModifier(SPEED));

                    if (gameLocal.world.spawnArgs.GetBool("no_Weapons") && (ent.IsType(idActor.class) || ent.IsType(idAFAttachment.class))) {
                        idThread.ReturnInt(0);
                        return;
                    }

                    ent.ApplyImpulse(this, tr[0].c.id, tr[0].c.point, impulse);

                    // weapon stealing - do this before damaging so weapons are not dropped twice
                    if (gameLocal.isMultiplayer
                            && (this.weaponDef != null) && this.weaponDef.dict.GetBool("stealing")
                            && ent.IsType(idPlayer.class)
                            && !this.owner.PowerUpActive(BERSERK)
                            && ((gameLocal.gameType != GAME_TDM) || gameLocal.serverInfo.GetBool("si_teamDamage") || (this.owner.team != ((idPlayer) ent).team))) {
                        this.owner.StealWeapon((idPlayer) ent);
                    }

                    if (ent.fl.takedamage) {
                        final idVec3 kickDir = new idVec3();
						idVec3 globalKickDir;
                        this.meleeDef.dict.GetVector("kickDir", "0 0 0", kickDir);
                        globalKickDir = this.muzzleAxis.oMultiply(kickDir);
                        ent.Damage(this.owner, this.owner, globalKickDir, this.meleeDefName.toString(), this.owner.PowerUpModifier(MELEE_DAMAGE), tr[0].c.id);
                        hit = true;
                    }

                    if (this.weaponDef.dict.GetBool("impact_damage_effect")) {

                        if (ent.spawnArgs.GetBool("bleed")) {

                            hitSound = this.meleeDef.dict.GetString(this.owner.PowerUpActive(BERSERK) ? "snd_hit_berserk" : "snd_hit");

                            ent.AddDamageEffect(tr[0], impulse, this.meleeDef.dict.GetString("classname"));

                        } else {

                            surfTypes_t type = tr[0].c.material.GetSurfaceType();
                            if (type == SURFTYPE_NONE) {
                                type = surfTypes_t.values()[GetDefaultSurfaceType()];
                            }

                            final String materialType = gameLocal.sufaceTypeNames[ type.ordinal()];

                            // start impact sound based on material type
                            hitSound = this.meleeDef.dict.GetString(va("snd_%s", materialType));
                            if (isNotNullOrEmpty(hitSound)) {
                                hitSound = this.meleeDef.dict.GetString("snd_metal");
                            }

                            if (gameLocal.time > this.nextStrikeFx) {
                                final String decal;
                                // project decal
                                decal = this.weaponDef.dict.GetString("mtr_strike");
                                if (isNotNullOrEmpty(decal)) {
                                    gameLocal.ProjectDecal(tr[0].c.point, tr[0].c.normal.oNegative(), 8.0f, true, 6.0f, decal);
                                }
                                this.nextStrikeFx = gameLocal.time + 200;
                            } else {
                                hitSound = "";
                            }

                            this.strikeSmokeStartTime = gameLocal.time;
                            this.strikePos.oSet(tr[0].c.point);
                            this.strikeAxis.oSet(tr[0].endAxis.oNegative());
                        }
                    }
                }

                if (isNotNullOrEmpty(hitSound)) {
                    final idSoundShader snd = declManager.FindSound(hitSound);
                    StartSoundShader(snd, SND_CHANNEL_BODY2, 0, true, null);
                }

                idThread.ReturnInt(hit);
                this.owner.WeaponFireFeedback(this.weaponDef.dict);
                return;
            }

            idThread.ReturnInt(0);
            this.owner.WeaponFireFeedback(this.weaponDef.dict);
        }

        private void Event_GetWorldModel() {
            idThread.ReturnEntity(this.worldModel.GetEntity());
        }

        private void Event_AllowDrop(idEventArg<Integer> allow) {
            this.allowDrop = (allow.value != 0);
        }

        private void Event_AutoReload() {
            assert (this.owner != null);
            if (gameLocal.isClient) {
                idThread.ReturnFloat(0.0f);
                return;
            }
            idThread.ReturnFloat(btoi(gameLocal.userInfo[this.owner.entityNumber].GetBool("ui_autoReload")));
        }

        private void Event_NetReload() {
            assert (this.owner != null);
            if (gameLocal.isServer) {
                ServerSendEvent(EVENT_RELOAD, null, false, -1);
            }
        }

        private void Event_IsInvisible() {
            if (null == this.owner) {
                idThread.ReturnFloat(0);
                return;
            }
            idThread.ReturnFloat(this.owner.PowerUpActive(INVISIBILITY) ? 1 : 0);
        }

        private void Event_NetEndReload() {
            assert (this.owner != null);
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

    }
}
