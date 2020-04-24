package neo.Game;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static neo.Game.AI.AI.SE_BLOCKED;
import static neo.Game.AI.AI.SE_ENTER_LEDGE_AREA;
import static neo.Game.AI.AI.SE_ENTER_OBSTACLE;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Game.GAME_API_VERSION;
import static neo.Game.Game.SCRIPT_DEFAULT;
import static neo.Game.Game.SCRIPT_DEFAULTFUNC;
import static neo.Game.Game.allowReply_t.ALLOW_BADPASS;
import static neo.Game.Game.allowReply_t.ALLOW_NOTYET;
import static neo.Game.Game.allowReply_t.ALLOW_YES;
import static neo.Game.Game.escReply_t.ESC_GUI;
import static neo.Game.Game.escReply_t.ESC_IGNORE;
import static neo.Game.Game.escReply_t.ESC_MAIN;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCmds.D_DrawDebugLines;
import static neo.Game.GameSys.SysCvar.__DATE__;
import static neo.Game.GameSys.SysCvar.aas_test;
import static neo.Game.GameSys.SysCvar.ai_showCombatNodes;
import static neo.Game.GameSys.SysCvar.ai_showObstacleAvoidance;
import static neo.Game.GameSys.SysCvar.ai_showPaths;
import static neo.Game.GameSys.SysCvar.ai_testPredictPath;
import static neo.Game.GameSys.SysCvar.developer;
import static neo.Game.GameSys.SysCvar.g_bloodEffects;
import static neo.Game.GameSys.SysCvar.g_cinematic;
import static neo.Game.GameSys.SysCvar.g_cinematicMaxSkipTime;
import static neo.Game.GameSys.SysCvar.g_decals;
import static neo.Game.GameSys.SysCvar.g_editEntityMode;
import static neo.Game.GameSys.SysCvar.g_flushSave;
import static neo.Game.GameSys.SysCvar.g_frametime;
import static neo.Game.GameSys.SysCvar.g_gravity;
import static neo.Game.GameSys.SysCvar.g_mapCycle;
import static neo.Game.GameSys.SysCvar.g_maxShowDistance;
import static neo.Game.GameSys.SysCvar.g_showActiveEntities;
import static neo.Game.GameSys.SysCvar.g_showCollisionModels;
import static neo.Game.GameSys.SysCvar.g_showCollisionTraces;
import static neo.Game.GameSys.SysCvar.g_showCollisionWorld;
import static neo.Game.GameSys.SysCvar.g_showEntityInfo;
import static neo.Game.GameSys.SysCvar.g_showPVS;
import static neo.Game.GameSys.SysCvar.g_showTargets;
import static neo.Game.GameSys.SysCvar.g_skill;
import static neo.Game.GameSys.SysCvar.g_stopTime;
import static neo.Game.GameSys.SysCvar.g_timeentities;
import static neo.Game.GameSys.SysCvar.pm_thirdPerson;
import static neo.Game.GameSys.SysCvar.r_aspectRatio;
import static neo.Game.Game_local.gameState_t.GAMESTATE_ACTIVE;
import static neo.Game.Game_local.gameState_t.GAMESTATE_NOMAP;
import static neo.Game.Game_local.gameState_t.GAMESTATE_SHUTDOWN;
import static neo.Game.Game_local.gameState_t.GAMESTATE_STARTUP;
import static neo.Game.Game_local.gameState_t.GAMESTATE_UNINITIALIZED;
import static neo.Game.Game_network.ASYNC_WRITE_TAGS;
import static neo.Game.Game_network.net_clientMaxPrediction;
import static neo.Game.Game_network.net_clientShowSnapshot;
import static neo.Game.Game_network.net_clientShowSnapshotRadius;
import static neo.Game.Game_network.net_clientSmoothing;
import static neo.Game.Game_network.idEventQueue.outOfOrderBehaviour_t.OUTOFORDER_DROP;
import static neo.Game.Game_network.idEventQueue.outOfOrderBehaviour_t.OUTOFORDER_IGNORE;
import static neo.Game.MultiplayerGame.gameType_t.GAME_DM;
import static neo.Game.MultiplayerGame.gameType_t.GAME_LASTMAN;
import static neo.Game.MultiplayerGame.gameType_t.GAME_SP;
import static neo.Game.MultiplayerGame.gameType_t.GAME_TDM;
import static neo.Game.MultiplayerGame.gameType_t.GAME_TOURNEY;
import static neo.Game.MultiplayerGame.snd_evt_t.SND_COUNT;
import static neo.Game.Pvs.pvsType_t.PVS_ALL_PORTALS_OPEN;
import static neo.Game.Pvs.pvsType_t.PVS_CONNECTED_AREAS;
import static neo.Game.Pvs.pvsType_t.PVS_NORMAL;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_MONSTERCLIP;
import static neo.Renderer.Material.CONTENTS_OPAQUE;
import static neo.Renderer.Material.CONTENTS_PLAYERCLIP;
import static neo.Renderer.Material.CONTENTS_RENDERMODEL;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.Material.CONTENTS_WATER;
import static neo.Renderer.Material.MAX_SURFACE_TYPES;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_ALL;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_LOCATION;
import static neo.TempDump.NOT;
import static neo.TempDump.atoi;
import static neo.TempDump.ctos;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.Tools.Compilers.AAS.AASFileManager.AASFileManager;
import static neo.framework.Async.NetworkSystem.networkSystem;
import static neo.framework.BuildDefines.ID_DEMO_BUILD;
import static neo.framework.BuildDefines._DEBUG;
import static neo.framework.BuildVersion.BUILD_NUMBER;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_GAME;
import static neo.framework.CVarSystem.CVAR_SERVERINFO;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.CmdSystem.CMD_FL_CHEAT;
import static neo.framework.CmdSystem.CMD_FL_GAME;
import static neo.framework.CmdSystem.CMD_FL_SYSTEM;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.idDeclAllocator;
import static neo.framework.DeclManager.declState_t.DS_DEFAULTED;
import static neo.framework.DeclManager.declType_t.DECL_AF;
import static neo.framework.DeclManager.declType_t.DECL_AUDIO;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;
import static neo.framework.DeclManager.declType_t.DECL_FX;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import static neo.framework.DeclManager.declType_t.DECL_MAX_TYPES;
import static neo.framework.DeclManager.declType_t.DECL_MODELDEF;
import static neo.framework.DeclManager.declType_t.DECL_MODELEXPORT;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.framework.DeclManager.declType_t.DECL_PDA;
import static neo.framework.DeclManager.declType_t.DECL_SKIN;
import static neo.framework.DeclManager.declType_t.DECL_SOUND;
import static neo.framework.DeclManager.declType_t.DECL_VIDEO;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.idlib.Heap.Mem_EnableLeakTest;
import static neo.idlib.Lib.LittleRevBytes;
import static neo.idlib.Lib.MAX_STRING_CHARS;
import static neo.idlib.Lib.Max;
import static neo.idlib.Lib.Min;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorLtGrey;
import static neo.idlib.Lib.colorMdGrey;
import static neo.idlib.Lib.colorOrange;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.INTSIGNBITSET;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.sys.sys_public.sys;
import static neo.ui.UserInterface.uiManager;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import neo.TempDump.void_callback;
import neo.CM.CollisionModel.trace_s;
import neo.CM.CollisionModel_local;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.AFEntity.idAFEntity_Generic;
import neo.Game.AFEntity.idAFEntity_WithAttachedHead;
import neo.Game.Actor.idActor;
import neo.Game.BrittleFracture.idBrittleFracture;
import neo.Game.Camera.idCamera;
import neo.Game.Camera.idCameraAnim;
import neo.Game.Camera.idCameraView;
import neo.Game.Entity.idEntity;
import neo.Game.FX.idEntityFx;
import neo.Game.Game.allowReply_t;
import neo.Game.Game.escReply_t;
import neo.Game.Game.gameExport_t;
import neo.Game.Game.gameImport_t;
import neo.Game.Game.gameReturn_t;
import neo.Game.Game.idGame;
import neo.Game.GameEdit.idEditEntities;
import neo.Game.Game_network.idEventQueue;
import neo.Game.Item.idItem;
import neo.Game.Item.idItemPowerup;
import neo.Game.Item.idMoveableItem;
import neo.Game.Item.idMoveablePDAItem;
import neo.Game.Item.idObjective;
import neo.Game.Item.idObjectiveComplete;
import neo.Game.Item.idPDAItem;
import neo.Game.Item.idVideoCDItem;
import neo.Game.Light.idLight;
import neo.Game.Misc.idAnimated;
import neo.Game.Misc.idBeam;
import neo.Game.Misc.idEarthQuake;
import neo.Game.Misc.idExplodable;
import neo.Game.Misc.idForceField;
import neo.Game.Misc.idFuncAASObstacle;
import neo.Game.Misc.idFuncEmitter;
import neo.Game.Misc.idFuncPortal;
import neo.Game.Misc.idFuncRadioChatter;
import neo.Game.Misc.idFuncSmoke;
import neo.Game.Misc.idLocationEntity;
import neo.Game.Misc.idLocationSeparatorEntity;
import neo.Game.Misc.idPathCorner;
import neo.Game.Misc.idPhantomObjects;
import neo.Game.Misc.idPlayerStart;
import neo.Game.Misc.idShaking;
import neo.Game.Misc.idSpawnableEntity;
import neo.Game.Misc.idStaticEntity;
import neo.Game.Misc.idVacuumEntity;
import neo.Game.Moveable.idExplodingBarrel;
import neo.Game.Moveable.idMoveable;
import neo.Game.Mover.idDoor;
import neo.Game.Mover.idElevator;
import neo.Game.Mover.idMover;
import neo.Game.Mover.idPendulum;
import neo.Game.Mover.idRotater;
import neo.Game.Mover.idSplinePath;
import neo.Game.MultiplayerGame.gameType_t;
import neo.Game.MultiplayerGame.idMultiplayerGame;
import neo.Game.MultiplayerGame.snd_evt_t;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idBFGProjectile;
import neo.Game.Projectile.idDebris;
import neo.Game.Projectile.idGuidedProjectile;
import neo.Game.Projectile.idProjectile;
import neo.Game.Pvs.idPVS;
import neo.Game.Pvs.pvsHandle_t;
import neo.Game.Pvs.pvsType_t;
import neo.Game.SmokeParticles.idSmokeParticles;
import neo.Game.Sound.idSound;
import neo.Game.Target.idTarget;
import neo.Game.Target.idTarget_CallObjectFunction;
import neo.Game.Target.idTarget_EnableLevelWeapons;
import neo.Game.Target.idTarget_EndLevel;
import neo.Game.Target.idTarget_FadeEntity;
import neo.Game.Target.idTarget_GiveEmail;
import neo.Game.Target.idTarget_LightFadeIn;
import neo.Game.Target.idTarget_LightFadeOut;
import neo.Game.Target.idTarget_LockDoor;
import neo.Game.Target.idTarget_Remove;
import neo.Game.Target.idTarget_SetInfluence;
import neo.Game.Target.idTarget_SetPrimaryObjective;
import neo.Game.Target.idTarget_SetShaderParm;
import neo.Game.Target.idTarget_Show;
import neo.Game.Target.idTarget_Tip;
import neo.Game.Trigger.idTrigger_Count;
import neo.Game.Trigger.idTrigger_Fade;
import neo.Game.Trigger.idTrigger_Hurt;
import neo.Game.Trigger.idTrigger_Multi;
import neo.Game.Trigger.idTrigger_Timer;
import neo.Game.WorldSpawn.idWorldspawn;
import neo.Game.AI.AAS.idAAS;
import neo.Game.AI.AI.idAI;
import neo.Game.AI.AI.idCombatNode;
import neo.Game.AI.AI.obstaclePath_s;
import neo.Game.AI.AI.predictedPath_s;
import neo.Game.Animation.Anim.idAnimManager;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import neo.Game.Animation.Anim_Import.idModelExport;
import neo.Game.Animation.Anim_Testmodel.idTestModel;
import neo.Game.GameSys.Class.idAllocError;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idTypeInfo;
import neo.Game.GameSys.Event.idEvent;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.GameSys.SysCmds.ArgCompletion_DefFile;
import neo.Game.GameSys.SysCmds.Cmd_AASStats_f;
import neo.Game.GameSys.SysCmds.Cmd_ActiveEntityList_f;
import neo.Game.GameSys.SysCmds.Cmd_AddChatLine_f;
import neo.Game.GameSys.SysCmds.Cmd_AddDebugLine_f;
import neo.Game.GameSys.SysCmds.Cmd_BindRagdoll_f;
import neo.Game.GameSys.SysCmds.Cmd_BlinkDebugLine_f;
import neo.Game.GameSys.SysCmds.Cmd_CenterView_f;
import neo.Game.GameSys.SysCmds.Cmd_ClearLights_f;
import neo.Game.GameSys.SysCmds.Cmd_CloseViewNotes_f;
import neo.Game.GameSys.SysCmds.Cmd_CollisionModelInfo_f;
import neo.Game.GameSys.SysCmds.Cmd_Damage_f;
import neo.Game.GameSys.SysCmds.Cmd_DeleteSelected_f;
import neo.Game.GameSys.SysCmds.Cmd_DisasmScript_f;
import neo.Game.GameSys.SysCmds.Cmd_EntityList_f;
import neo.Game.GameSys.SysCmds.Cmd_ExportModels_f;
import neo.Game.GameSys.SysCmds.Cmd_GameError_f;
import neo.Game.GameSys.SysCmds.Cmd_GetViewpos_f;
import neo.Game.GameSys.SysCmds.Cmd_Give_f;
import neo.Game.GameSys.SysCmds.Cmd_God_f;
import neo.Game.GameSys.SysCmds.Cmd_Kick_f;
import neo.Game.GameSys.SysCmds.Cmd_KillMonsters_f;
import neo.Game.GameSys.SysCmds.Cmd_KillMovables_f;
import neo.Game.GameSys.SysCmds.Cmd_KillRagdolls_f;
import neo.Game.GameSys.SysCmds.Cmd_Kill_f;
import neo.Game.GameSys.SysCmds.Cmd_ListAnims_f;
import neo.Game.GameSys.SysCmds.Cmd_ListCollisionModels_f;
import neo.Game.GameSys.SysCmds.Cmd_ListDebugLines_f;
import neo.Game.GameSys.SysCmds.Cmd_ListSpawnArgs_f;
import neo.Game.GameSys.SysCmds.Cmd_NextGUI_f;
import neo.Game.GameSys.SysCmds.Cmd_Noclip_f;
import neo.Game.GameSys.SysCmds.Cmd_Notarget_f;
import neo.Game.GameSys.SysCmds.Cmd_PlayerModel_f;
import neo.Game.GameSys.SysCmds.Cmd_PopLight_f;
import neo.Game.GameSys.SysCmds.Cmd_RecordViewNotes_f;
import neo.Game.GameSys.SysCmds.Cmd_ReexportModels_f;
import neo.Game.GameSys.SysCmds.Cmd_ReloadAnims_f;
import neo.Game.GameSys.SysCmds.Cmd_ReloadScript_f;
import neo.Game.GameSys.SysCmds.Cmd_RemoveDebugLine_f;
import neo.Game.GameSys.SysCmds.Cmd_Remove_f;
import neo.Game.GameSys.SysCmds.Cmd_SaveLights_f;
import neo.Game.GameSys.SysCmds.Cmd_SaveMoveables_f;
import neo.Game.GameSys.SysCmds.Cmd_SaveParticles_f;
import neo.Game.GameSys.SysCmds.Cmd_SaveRagdolls_f;
import neo.Game.GameSys.SysCmds.Cmd_SaveSelected_f;
import neo.Game.GameSys.SysCmds.Cmd_SayTeam_f;
import neo.Game.GameSys.SysCmds.Cmd_Say_f;
import neo.Game.GameSys.SysCmds.Cmd_Script_f;
import neo.Game.GameSys.SysCmds.Cmd_SetViewpos_f;
import neo.Game.GameSys.SysCmds.Cmd_ShowViewNotes_f;
import neo.Game.GameSys.SysCmds.Cmd_Spawn_f;
import neo.Game.GameSys.SysCmds.Cmd_Teleport_f;
import neo.Game.GameSys.SysCmds.Cmd_TestBoneFx_f;
import neo.Game.GameSys.SysCmds.Cmd_TestDamage_f;
import neo.Game.GameSys.SysCmds.Cmd_TestDeath_f;
import neo.Game.GameSys.SysCmds.Cmd_TestFx_f;
import neo.Game.GameSys.SysCmds.Cmd_TestId_f;
import neo.Game.GameSys.SysCmds.Cmd_TestLight_f;
import neo.Game.GameSys.SysCmds.Cmd_TestPointLight_f;
import neo.Game.GameSys.SysCmds.Cmd_TestSave_f;
import neo.Game.GameSys.SysCmds.Cmd_Trigger_f;
import neo.Game.GameSys.SysCmds.Cmd_UnbindRagdoll_f;
import neo.Game.GameSys.SysCmds.Cmd_WeaponSplat_f;
import neo.Game.GameSys.TypeInfo.ListTypeInfo_f;
import neo.Game.GameSys.TypeInfo.TestSaveGame_f;
import neo.Game.GameSys.TypeInfo.WriteGameState_f;
import neo.Game.Physics.Clip.idClip;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Force.idForce;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Actor.idPhysics_Actor;
import neo.Game.Physics.Physics_Parametric.idPhysics_Parametric;
import neo.Game.Physics.Push.idPush;
import neo.Game.Script.Script_Program.function_t;
import neo.Game.Script.Script_Thread.idThread;
import neo.Game.Script.idProgram;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.ModelManager;
import neo.Renderer.RenderSystem;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Renderer.RenderWorld.modelTrace_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.snd_shader.soundShaderParms_t;
import neo.Sound.snd_system;
import neo.Sound.sound.idSoundWorld;
import neo.framework.CVarSystem;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem;
import neo.framework.CmdSystem.argCompletion_t;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.Common;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import neo.framework.DeclManager;
import neo.framework.DeclManager.declType_t;
import neo.framework.DeclManager.idDecl;
import neo.framework.DeclManager.idListDecls_f;
import neo.framework.DeclManager.idPrintDecls_f;
import neo.framework.FileSystem_h;
import neo.framework.File_h.idFile;
import neo.framework.UsercmdGen.usercmd_t;
import neo.framework.Async.NetworkSystem;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.Timer.idTimer;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.LinkList.idLinkList;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StaticList.idStaticList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.geometry.TraceModel.traceModelPoly_t;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Simd.idSIMD;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec5;
import neo.idlib.math.Matrix.idMat3;
import neo.open.Nio;
import neo.sys.sys_public;
import neo.ui.UserInterface;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class Game_local {

    static final boolean GAME_DLL = true;//TODO:find correct location

    // the rest of the engine will only reference the "game" variable, while all local aspects stay hidden
    public static final idGameLocal gameLocal = new idGameLocal();//TODO:these globals should either be collected to a single file, or always be set at the top.
    public static final idGame      game      = gameLocal;// statically pointed at an idGameLocal

    /*
     ===============================================================================

     Local implementation of the public game interface.

     ===============================================================================
     */
    public static final int    LAGO_IMG_WIDTH  = 64;
    public static final int    LAGO_IMG_HEIGHT = 64;
    public static final int    LAGO_WIDTH      = 64;
    public static final int    LAGO_HEIGHT     = 44;
    public static final String LAGO_MATERIAL   = "textures/sfx/lagometer";
    public static final String LAGO_IMAGE      = "textures/sfx/lagometer.tga";
//
// if set to 1 the server sends the client PVS with snapshots and the client compares against what it sees
//#ifndef ASYNC_WRITE_PVS
    public static final boolean ASYNC_WRITE_PVS = false;
//#endif
//    #ifdef ID_DEBUG_UNINITIALIZED_MEMORY
//// This is real evil but allows the code to inspect arbitrary class variables.//KEH:java reflection forever!
//#define private		public
//#define protected	public
//#endif
//
    public static idRenderWorld gameRenderWorld;
    public static idSoundWorld gameSoundWorld;
    //
    private static      gameExport_t gameExport             = new gameExport_t();
    //
// the "gameversion" client command will print this plus compile date
    public static final String       GAME_VERSION           = "baseDOOM-1";
    //
    public static final int          MAX_CLIENTS            = 32;
    public static final int          GENTITYNUM_BITS        = 12;
    //
    public static final int          MAX_GENTITIES          = 1 << GENTITYNUM_BITS;
    public static final int          ENTITYNUM_NONE         = MAX_GENTITIES - 1;
    public static final int          ENTITYNUM_WORLD        = MAX_GENTITIES - 2;
    public static final int          ENTITYNUM_MAX_NORMAL   = MAX_GENTITIES - 2;
    //============================================================================
    //============================================================================
    public static final int          MAX_GAME_MESSAGE_SIZE  = 8192;
    public static final int          MAX_ENTITY_STATE_SIZE  = 512;
    public static final int          ENTITY_PVS_SIZE        = ((MAX_GENTITIES + 31) >> 5);
    public static final int          NUM_RENDER_PORTAL_BITS = idMath.BitsForInteger(etoi(PS_BLOCK_ALL));
    //
    static final        idCVar       com_forceGenericSIMD   = new idCVar("com_forceGenericSIMD", "0", CVAR_BOOL | CVAR_SYSTEM, "force generic platform independent SIMD");

    /*
     ===============================================================================
     Public game interface with methods for in-game editing.
     ===============================================================================
     */
    public static class entityState_s {

        int      entityNumber;
        idBitMsg state;
        ByteBuffer stateBuf = ByteBuffer.allocate(MAX_ENTITY_STATE_SIZE);
        entityState_s next;
    }

    public static class snapshot_s {

        int           sequence;
        entityState_s firstEntityState;
        int[] pvs = new int[ENTITY_PVS_SIZE];
        snapshot_s next;
    }
    public static final int MAX_EVENT_PARAM_SIZE = 128;

    public static class entityNetEvent_s {

        int spawnId;
        int event;
        int time;
        int paramsSize;
        ByteBuffer paramsBuf = ByteBuffer.allocate(MAX_EVENT_PARAM_SIZE);
        entityNetEvent_s next;
        entityNetEvent_s prev;
    }
// enum {
    public static final int GAME_RELIABLE_MESSAGE_INIT_DECL_REMAP = 0;
    public static final int GAME_RELIABLE_MESSAGE_REMAP_DECL      = 1;
    public static final int GAME_RELIABLE_MESSAGE_SPAWN_PLAYER    = 2;
    public static final int GAME_RELIABLE_MESSAGE_DELETE_ENT      = 3;
    public static final int GAME_RELIABLE_MESSAGE_CHAT            = 4;
    public static final int GAME_RELIABLE_MESSAGE_TCHAT           = 5;
    public static final int GAME_RELIABLE_MESSAGE_SOUND_EVENT     = 6;
    public static final int GAME_RELIABLE_MESSAGE_SOUND_INDEX     = 7;
    public static final int GAME_RELIABLE_MESSAGE_DB              = 8;
    public static final int GAME_RELIABLE_MESSAGE_KILL            = 9;
    public static final int GAME_RELIABLE_MESSAGE_DROPWEAPON      = 10;
    public static final int GAME_RELIABLE_MESSAGE_RESTART         = 11;
    public static final int GAME_RELIABLE_MESSAGE_SERVERINFO      = 12;
    public static final int GAME_RELIABLE_MESSAGE_TOURNEYLINE     = 13;
    public static final int GAME_RELIABLE_MESSAGE_CALLVOTE        = 14;
    public static final int GAME_RELIABLE_MESSAGE_CASTVOTE        = 15;
    public static final int GAME_RELIABLE_MESSAGE_STARTVOTE       = 16;
    public static final int GAME_RELIABLE_MESSAGE_UPDATEVOTE      = 17;
    public static final int GAME_RELIABLE_MESSAGE_PORTALSTATES    = 18;
    public static final int GAME_RELIABLE_MESSAGE_PORTAL          = 19;
    public static final int GAME_RELIABLE_MESSAGE_VCHAT           = 20;
    public static final int GAME_RELIABLE_MESSAGE_STARTSTATE      = 21;
    public static final int GAME_RELIABLE_MESSAGE_MENU            = 22;
    public static final int GAME_RELIABLE_MESSAGE_WARMUPTIME      = 23;
    public static final int GAME_RELIABLE_MESSAGE_EVENT           = 24;
// };

    public enum gameState_t {

        GAMESTATE_UNINITIALIZED, // prior to Init being called
        GAMESTATE_NOMAP, // no map loaded
        GAMESTATE_STARTUP, // inside InitFromNewMap().  spawning map entities.
        GAMESTATE_ACTIVE, // normal gameplay
        GAMESTATE_SHUTDOWN				// inside MapShutdown().  clearing memory.
    }

    public static class spawnSpot_t {

        idEntity ent;
        int dist;
    }
//============================================================================

    public static class idEntityPtr<type extends idEntity> {

        private int spawnId;
        //
        //

        public idEntityPtr() {
            this.spawnId = 0;
        }
        
        public idEntityPtr(type ent) {
            this.oSet(ent);
        }
        // save games

        public void Save(idSaveGame savefile) {					// archives object for save game file
            savefile.WriteInt(this.spawnId);
        }

        public void Restore(idRestoreGame savefile) {					// unarchives object from save game file
            final int[] spawnId = {0};
            savefile.ReadInt(spawnId);
            this.spawnId = spawnId[0];
        }

        public idEntityPtr<type> oSet(type ent) {
            if (ent == null) {
                this.spawnId = 0;
            } else {
                final int entityNumber = ent.entityNumber;
                this.spawnId = (gameLocal.spawnIds[entityNumber] << GENTITYNUM_BITS) | entityNumber;
            }
            return this;
        }

        // synchronize entity pointers over the network
        public int GetSpawnId() {
            return this.spawnId;
        }

        public boolean SetSpawnId(int id) {
            // the reason for this first check is unclear:
            // the function returning false may mean the spawnId is already set right, or the entity is missing
            if (id == this.spawnId) {
                return false;
            }
            if ((id >> GENTITYNUM_BITS) == gameLocal.spawnIds[id & ((1 << GENTITYNUM_BITS) - 1)]) {
                this.spawnId = id;
                return true;
            }
            return false;
        }

//        public boolean UpdateSpawnId();
        public boolean IsValid() {
            return (gameLocal.spawnIds[this.spawnId & ((1 << GENTITYNUM_BITS) - 1)] == (this.spawnId >> GENTITYNUM_BITS));
        }

        public type GetEntity() {
            final int entityNum = this.spawnId & ((1 << GENTITYNUM_BITS) - 1);
            if ((gameLocal.spawnIds[entityNum] == (this.spawnId >> GENTITYNUM_BITS))) {
                return (type) gameLocal.entities[entityNum];
            }
            return null;
        }

        public int GetEntityNum() {
            return (this.spawnId & ((1 << GENTITYNUM_BITS) - 1));
        }
    }

    //============================================================================
    public static class idGameLocal extends neo.Game.Game.idGame {

        public idDict serverInfo = new idDict();                                // all the tunable parameters, like numclients, etc
        public int numClients;                                                  // pulled from serverInfo and verified
        public idDict[]    userInfo             = new idDict[MAX_CLIENTS];      // client specific settings
        public usercmd_t[] usercmds             = new usercmd_t[MAX_CLIENTS];   // client input commands
        public idDict[]    persistentPlayerInfo = new idDict[MAX_CLIENTS];
        public idEntity[]  entities             = new idEntity[MAX_GENTITIES];  // index to entities
        public int[]       spawnIds             = new int[MAX_GENTITIES];       // for use in idEntityPtr
        public int firstFreeIndex;                                              // first free index in the entities array
        public int num_entities;                                                // current number <= MAX_GENTITIES
        public idHashIndex entityHash = new idHashIndex();                      // hash table to quickly find entities by name
        public idWorldspawn world;                                              // world entity
        public idLinkList<idEntity> spawnedEntities = new idLinkList<>();       // all spawned entities
        public idLinkList<idEntity> activeEntities  = new idLinkList<>();       // all thinking entities (idEntity::thinkFlags != 0)
        public int     numEntitiesToDeactivate;                                 // number of entities that became inactive in current frame
        public boolean sortPushers;                                             // true if active lists needs to be reordered to place pushers at the front
        public boolean sortTeamMasters;                                         // true if active lists needs to be reordered to place physics team masters before their slaves
        public idDict    persistentLevelInfo = new idDict();                    // contains args that are kept around between levels
        //
        // can be used to automatically effect every material in the world that references globalParms
        public float[]   globalShaderParms   = new float[MAX_GLOBAL_SHADER_PARMS];
        //
        public idRandom  random              = new idRandom();         // random number generator used throughout the game
        //
        public idProgram program             = new idProgram();        // currently loaded script and data space
        public idThread frameCommandThread;
        //
        public idClip clip = new idClip();          // collision detection
        public idPush push = new idPush();          // geometric pushing
        public idPVS  pvs  = new idPVS();           // potential visible set
        //
        public idTestModel testmodel;               // for development testing of models
        public idEntityFx  testFx;                  // for development testing of fx
        //
        public idStr             sessionCommand = new idStr();              // a target_sessionCommand can set this to return something to the session
        //
        public idMultiplayerGame mpGame         = new idMultiplayerGame();  // handles rules for standard dm
        //
        public idSmokeParticles smokeParticles;         // global smoke trails
        public idEditEntities   editEntities;           // in game editing
        //
        public int              cinematicSkipTime;      // don't allow skipping cinemetics until this time has passed so player doesn't skip out accidently from a firefight
        public int              cinematicStopTime;      // cinematics have several camera changes, so keep track of when we stop them so that we don't reset cinematicSkipTime unnecessarily
        public int              cinematicMaxSkipTime;   // time to end cinematic when skipping.  there's a possibility of an infinite loop if the map isn't set up right.
        public boolean          inCinematic;            // game is playing cinematic (player controls frozen)
        public boolean          skipCinematic;
        //
        // are kept up to date with changes to serverInfo
        public int              framenum;
        public int              previousTime;           // time in msec of last frame
        public int              time;                   // in msec
        public static final int msec = USERCMD_MSEC;    // time since last update in milliseconds
        //
        public int                  vacuumAreaNum;      // -1 if level doesn't have any outside areas
        //
        public gameType_t           gameType;
        public boolean              isMultiplayer;      // set if the game is run in multiplayer mode
        public boolean              isServer;           // set if the game is run for a dedicated or listen server
        public boolean              isClient;           // set if the game is run for a client
        // discriminates between the RunFrame path and the ClientPrediction path
        // NOTE: on a listen server, isClient is false
        public int                  localClientNum;     // number of the local client. MP: -1 on a dedicated
        public idLinkList<idEntity> snapshotEntities;   // entities from the last snapshot
        public int                  realClientTime;     // real client time
        public boolean              isNewFrame;         // true if this is a new game frame, not a rerun due to prediction
        public float                clientSmoothing;    // smoothing of other clients in the view
        public int                  entityDefBits;      // bits required to store an entity def number
        //
        public String[] sufaceTypeNames = new String[MAX_SURFACE_TYPES];    // text names for surface types
        public idEntityPtr<idEntity> lastGUIEnt;        // last entity with a GUI, used by Cmd_NextGUI_f
        public int                   lastGUI;           // last GUI on the lastGUIEnt
        //
//
        private static final int   INITIAL_SPAWN_COUNT = 1;
        //
        private final              idStr mapFileName         = new idStr();    // name of the map, empty string if no map loaded
        private idMapFile          mapFile;            // will be NULL during the game unless in-game editing is used
        private boolean            mapCycleLoaded;
        //
        private int                spawnCount;
        private int                mapSpawnCount;      // it's handy to know which entities are part of the map
        //
        private idLocationEntity[] locationEntities;   // for location names, etc
        //
        private idCamera           camera;
        private idMaterial         globalMaterial;     // for overriding everything
        //
        private final idList<idAAS> aasList  = new idList<>(); // area system
        private final idStrList     aasNames = new idStrList();
        //
        private idEntityPtr<idActor> lastAIAlertEntity;
        private int                  lastAIAlertTime;
        //
        private final idDict      spawnArgs            = new idDict();        // spawn args used during entity spawning  FIXME: shouldn't be necessary anymore
        //
        private pvsHandle_t playerPVS            = new pvsHandle_t();// merged pvs of all players
        private pvsHandle_t playerConnectedAreas = new pvsHandle_t();// all areas connected to any player area
        //
        private final idVec3      gravity              = new idVec3();          // global gravity vector
        private gameState_t gamestate;            // keeps track of whether we're spawning, shutting down, or normal gameplay
        private boolean     influenceActive;        // true when a phantasm is happening
        private int         nextGibTime;
        //
        private final idList<Integer>[][]       clientDeclRemap    = new idList[MAX_CLIENTS][etoi(DECL_MAX_TYPES)];
        //
        private       entityState_s[][]         clientEntityStates = new entityState_s[MAX_CLIENTS][MAX_GENTITIES];
        private       int[][]                   clientPVS          = new int[MAX_CLIENTS][ENTITY_PVS_SIZE];
        private       snapshot_s[]              clientSnapshots    = new snapshot_s[MAX_CLIENTS];
        //        private final idBlockAlloc<entityState_s> entityStateAllocator = new idBlockAlloc<>(256);
//        private final idBlockAlloc<snapshot_s> snapshotAllocator = new idBlockAlloc<>(64);
//
        private final       idEventQueue              eventQueue         = new idEventQueue();
        private final       idEventQueue              savedEventQueue    = new idEventQueue();
        //
        private final idStaticList<spawnSpot_t> spawnSpots         = new idStaticList<>(MAX_GENTITIES);
        private final idStaticList<idEntity>    initialSpots       = new idStaticList<>(MAX_GENTITIES);
        private int currentInitialSpot;
        //
        private idDict newInfo = new idDict();
        //
        private idStrList shakeSounds;
        //
        private byte[][][] lagometer = new byte[LAGO_IMG_HEIGHT][LAGO_IMG_WIDTH][4];
//
//
//
        // ---------------------- Public idGame Interface -------------------

        public idGameLocal() {
            for (int u = 0; u < MAX_CLIENTS; u++) {
                this.userInfo[u] = new idDict();
                this.persistentPlayerInfo[u] = new idDict();
            }

            Clear();
        }

        /*
         ===========
         idGameLocal::Init

         initialize the game object, only happens once at startup, not each level load
         ============
         */
        @Override
        public void Init() {
            idDict dict;
            idAAS aas;

            if (GAME_DLL) {

                TestGameAPI();

            } else {

                // initialize idLib
                idLib.Init();

                // register static cvars declared in the game
                idCVar.RegisterStaticVars();

                // initialize processor specific SIMD
                idSIMD.InitProcessor("game", com_forceGenericSIMD.GetBool());

            }

            Printf("--------- Initializing Game ----------\n");
            Printf("gamename: %s\n", GAME_VERSION);
            Printf("gamedate: %s\n", __DATE__);

            // register game specific decl types
            declManager.RegisterDeclType("model", DECL_MODELDEF, idDeclAllocator(idDeclModelDef.class));
            declManager.RegisterDeclType("export", DECL_MODELEXPORT, idDeclAllocator(idDecl.class));

            // register game specific decl folders
            declManager.RegisterDeclFolder("def", ".def", DECL_ENTITYDEF);
            declManager.RegisterDeclFolder("fx", ".fx", DECL_FX);
            declManager.RegisterDeclFolder("particles", ".prt", DECL_PARTICLE);
            declManager.RegisterDeclFolder("af", ".af", DECL_AF);
            declManager.RegisterDeclFolder("newpdas", ".pda", DECL_PDA);

            cmdSystem.AddCommand("listModelDefs", new idListDecls_f(DECL_MODELDEF), CMD_FL_SYSTEM | CMD_FL_GAME, "lists model defs");
            cmdSystem.AddCommand("printModelDefs", new idPrintDecls_f(DECL_MODELDEF), CMD_FL_SYSTEM | CMD_FL_GAME, "prints a model def", new idCmdSystem.ArgCompletion_Decl(DECL_MODELDEF));

            Clear();

            idEvent.Init();
            idClass.INIT();

            InitConsoleCommands();

            // load default scripts
            this.program.Startup(SCRIPT_DEFAULT);

            this.smokeParticles = new idSmokeParticles();

            // set up the aas
            dict = FindEntityDefDict("aas_types");
            if (null == dict) {
                Error("Unable to find entityDef for 'aas_types'");
            }

            // allocate space for the aas
            idKeyValue kv = dict.MatchPrefix("type");
            while (kv != null) {
                aas = idAAS.Alloc();
                this.aasList.Append(aas);
                this.aasNames.Append(kv.GetValue());
                kv = dict.MatchPrefix("type", kv);
            }

            this.gamestate = GAMESTATE_NOMAP;

            Printf("...%d aas types\n", this.aasList.Num());
            Printf("game initialized.\n");
            Printf("--------------------------------------\n");
        }

        /*
         ===========
         idGameLocal::Shutdown

         shut down the entire game
         ============
         */
        @Override
        public void Shutdown() {

            if (NOT(common)) {
                return;
            }

            Printf("------------ Game Shutdown -----------\n");

            this.mpGame.Shutdown();

            MapShutdown();

            this.aasList.DeleteContents(true);
            this.aasNames.Clear();

            idAI.FreeObstacleAvoidanceNodes();

            // shutdown the model exporter
            idModelExport.Shutdown();

            idEvent.Shutdown();

//	delete[] locationEntities;
            this.locationEntities = null;

//	delete smokeParticles;
            this.smokeParticles = null;

            idClass.Shutdown();

            // clear list with forces
            idForce.ClearForceList();

            // free the program data
            this.program.FreeData();

            // delete the .map file
//	delete mapFile;
            this.mapFile = null;

            // free the collision map
            CollisionModel_local.collisionModelManager.FreeMap();

            ShutdownConsoleCommands();

            // free memory allocated by class objects
            Clear();

            // shut down the animation manager
            animationLib.Shutdown();

            Printf("--------------------------------------\n");

            if (GAME_DLL) {

                // remove auto-completion function pointers pointing into this DLL
                cvarSystem.RemoveFlaggedAutoCompletion(CVAR_GAME);

                // enable leak test
                Mem_EnableLeakTest("game");

                // shutdown idLib
                idLib.ShutDown();

            }
        }

        @Override
        public void SetLocalClient(int clientNum) {
            this.localClientNum = clientNum;
        }

        @Override
        public void ThrottleUserInfo() {
            this.mpGame.ThrottleUserInfo();
        }

        @Override
        public idDict SetUserInfo(int clientNum, final idDict userInfo, boolean isClient, boolean canModify) {
            int i;
            boolean modifiedInfo = false;

            this.isClient = isClient;

            if ((clientNum >= 0) && (clientNum < MAX_CLIENTS)) {
                this.userInfo[clientNum] = userInfo;

                // server sanity
                if (canModify) {

                    // don't let numeric nicknames, it can be exploited to go around kick and ban commands from the server
                    if (idStr.IsNumeric(this.userInfo[clientNum].GetString("ui_name"))) {
                        this.userInfo[clientNum].Set("ui_name", va("%s_", this.userInfo[clientNum].GetString("ui_name")));
                        modifiedInfo = true;
                    }

                    // don't allow dupe nicknames
                    for (i = 0; i < this.numClients; i++) {
                        if (i == clientNum) {
                            continue;
                        }
                        if ((this.entities[i] != null) && this.entities[i].IsType(idPlayer.class)) {
                            if (0 == idStr.Icmp(this.userInfo[clientNum].GetString("ui_name"), this.userInfo[i].GetString("ui_name"))) {
                                this.userInfo[clientNum].Set("ui_name", va("%s_", this.userInfo[clientNum].GetString("ui_name")));
                                modifiedInfo = true;
                                i = -1;	// rescan
                                continue;
                            }
                        }
                    }
                }

                if ((this.entities[clientNum] != null) && this.entities[clientNum].IsType(idPlayer.class)) {
                    modifiedInfo |= ((idPlayer) this.entities[clientNum]).UserInfoChanged(canModify);
                }

                if (!isClient) {
                    // now mark this client in game
                    this.mpGame.EnterGame(clientNum);
                }
            }

            if (modifiedInfo) {
                assert (canModify);
                this.newInfo = this.userInfo[clientNum];
                return this.newInfo;
            }
            return null;
        }

        @Override
        public idDict GetUserInfo(int clientNum) {
            if ((this.entities[ clientNum] != null) && this.entities[ clientNum].IsType(idPlayer.class)) {
                return this.userInfo[ clientNum];
            }
            return null;
        }

        @Override
        public void SetServerInfo(final idDict _serverInfo) {
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            this.serverInfo = _serverInfo;
            UpdateServerInfoFlags();

            if (!this.isClient) {
                // Let our clients know the server info changed
                outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_SERVERINFO);
                outMsg.WriteDeltaDict(gameLocal.serverInfo, null);
                networkSystem.ServerSendReliableMessage(-1, outMsg);
            }
        }

        @Override
        public idDict GetPersistentPlayerInfo(int clientNum) {
            idEntity ent;

            this.persistentPlayerInfo[ clientNum].Clear();
            ent = this.entities[ clientNum];
            if ((ent != null) && ent.IsType(idPlayer.class)) {
                ((idPlayer) ent).SavePersistantInfo();
            }

            return this.persistentPlayerInfo[ clientNum];
        }

        @Override
        public void SetPersistentPlayerInfo(int clientNum, final idDict playerInfo) {
            this.persistentPlayerInfo[ clientNum] = playerInfo;
        }

        @Override
        public void InitFromNewMap(final String mapName, idRenderWorld renderWorld, idSoundWorld soundWorld, boolean isServer, boolean isClient, int randSeed) {

            this.isServer = isServer;
            this.isClient = isClient;
            this.isMultiplayer = isServer || isClient;

            if (this.mapFileName.Length() != 0) {
                MapShutdown();
            }

            Printf("----------- Game Map Init ------------\n");

            this.gamestate = GAMESTATE_STARTUP;

            gameRenderWorld = renderWorld;
            gameSoundWorld = soundWorld;

            LoadMap(mapName, randSeed);

            InitScriptForMap();

            MapPopulate();

            this.mpGame.Reset();

            this.mpGame.Precache();

            // free up any unused animations
            animationLib.FlushUnusedAnims();

            this.gamestate = GAMESTATE_ACTIVE;

            Printf("--------------------------------------\n");
        }

        @Override
        public boolean InitFromSaveGame(final String mapName, idRenderWorld renderWorld, idSoundWorld soundWorld, idFile saveGameFile) {
            int i;
            int num;
            final idEntity ent = new idEntity();
            final idDict si = new idDict();

            if (this.mapFileName.Length() != 0) {
                MapShutdown();
            }

            Printf("------- Game Map Init SaveGame -------\n");

            this.gamestate = GAMESTATE_STARTUP;

            gameRenderWorld = renderWorld;
            gameSoundWorld = soundWorld;

            final idRestoreGame savegame = new idRestoreGame(saveGameFile);

            savegame.ReadBuildNumber();

            // Create the list of all objects in the game
            savegame.CreateObjects();

            // Load the idProgram, also checking to make sure scripting hasn't changed since the savegame
            if (this.program.Restore(savegame) == false) {

                // Abort the load process, and let the session know so that it can restart the level
                // with the player persistent data.
                savegame.DeleteObjects();
                this.program.Restart();

                return false;
            }

            // load the map needed for this savegame
            LoadMap(mapName, 0);

            i = savegame.ReadInt();
            g_skill.SetInteger(i);

            // precache the player
            FindEntityDef("player_doommarine", false);

            // precache any media specified in the map
            for (i = 0; i < this.mapFile.GetNumEntities(); i++) {
                final idMapEntity mapEnt = this.mapFile.GetEntity(i);

                if (!InhibitEntitySpawn(mapEnt.epairs)) {
                    CacheDictionaryMedia(mapEnt.epairs);
                    final String classname = mapEnt.epairs.GetString("classname");
//			if ( classname != '\0' ) {
                    if (!classname.isEmpty()) {
                        FindEntityDef(classname, false);
                    }
                }
            }

            savegame.ReadDict(si);
            SetServerInfo(si);

            this.numClients = savegame.ReadInt();
            for (i = 0; i < this.numClients; i++) {
                savegame.ReadDict(this.userInfo[ i]);
                savegame.ReadUsercmd(this.usercmds[ i]);
                savegame.ReadDict(this.persistentPlayerInfo[ i]);
            }

            for (i = 0; i < MAX_GENTITIES; i++) {
                savegame.ReadObject(/*reinterpret_cast<idClass *&>*/this.entities[i]);
                this.spawnIds[i] = savegame.ReadInt();

                // restore the entityNumber
                if (this.entities[ i] != null) {
                    this.entities[ i].entityNumber = i;
                }
            }

            this.firstFreeIndex = savegame.ReadInt();
            this.num_entities = savegame.ReadInt();

            // enityHash is restored by idEntity.Restore setting the entity name.
            savegame.ReadObject(this./*reinterpret_cast<idClass *&>*/world);

            num = savegame.ReadInt();
            for (i = 0; i < num; i++) {
                savegame.ReadObject(/*reinterpret_cast<idClass *&>*/ent);
                assert (!ent.isNULL());
                if (!ent.isNULL()) {
                    ent.spawnNode.AddToEnd(this.spawnedEntities);
                }
            }

            num = savegame.ReadInt();
            for (i = 0; i < num; i++) {
                savegame.ReadObject(/*reinterpret_cast<idClass *&>*/ent);
                assert (!ent.isNULL());
                if (!ent.isNULL()) {
                    ent.activeNode.AddToEnd(this.activeEntities);
                }
            }

            this.numEntitiesToDeactivate = savegame.ReadInt();
            this.sortPushers = savegame.ReadBool();
            this.sortTeamMasters = savegame.ReadBool();
            savegame.ReadDict(this.persistentLevelInfo);

            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                this.globalShaderParms[i] = savegame.ReadFloat();
            }

            i = savegame.ReadInt();
            this.random.SetSeed(i);

            savegame.ReadObject(this./*reinterpret_cast<idClass *&>*/frameCommandThread);

            // clip
            // push
            // pvs
            // testmodel = "<NULL>"
            // testFx = "<NULL>"
            savegame.ReadString(this.sessionCommand);

            // FIXME: save smoke particles
            this.cinematicSkipTime = savegame.ReadInt();
            this.cinematicStopTime = savegame.ReadInt();
            this.cinematicMaxSkipTime = savegame.ReadInt();
            this.inCinematic = savegame.ReadBool();
            this.skipCinematic = savegame.ReadBool();

            this.isMultiplayer = savegame.ReadBool();
            this.gameType = gameType_t.values()[savegame.ReadInt()];

            this.framenum = savegame.ReadInt();
            this.previousTime = savegame.ReadInt();
            this.time = savegame.ReadInt();

            this.vacuumAreaNum = savegame.ReadInt();

            this.entityDefBits = savegame.ReadInt();
            this.isServer = savegame.ReadBool();
            this.isClient = savegame.ReadBool();

            this.localClientNum = savegame.ReadInt();

            // snapshotEntities is used for multiplayer only
            this.realClientTime = savegame.ReadInt();
            this.isNewFrame = savegame.ReadBool();
            this.clientSmoothing = savegame.ReadFloat();

            this.mapCycleLoaded = savegame.ReadBool();
            this.spawnCount = savegame.ReadInt();

            num = savegame.ReadInt();
            if (num != 0) {
                if (num != gameRenderWorld.NumAreas()) {
                    savegame.Error("idGameLocal.InitFromSaveGame: number of areas in map differs from save game.");
                }

                this.locationEntities = new idLocationEntity[num];
                for (i = 0; i < num; i++) {
                    savegame.ReadObject(/*reinterpret_cast<idClass *&>*/this.locationEntities[i]);
                }
            }

            savegame.ReadObject(this./*reinterpret_cast<idClass *&>*/camera);

            savegame.ReadMaterial(this.globalMaterial);

            this.lastAIAlertEntity.Restore(savegame);
            this.lastAIAlertTime = savegame.ReadInt();

            savegame.ReadDict(this.spawnArgs);

            this.playerPVS.i = savegame.ReadInt();
            this.playerPVS.h = savegame.ReadInt(/*(int &)*/);
            this.playerConnectedAreas.i = savegame.ReadInt();
            this.playerConnectedAreas.h = savegame.ReadInt( /*(int &)*/);

            savegame.ReadVec3(this.gravity);

            // gamestate is restored after restoring everything else
            this.influenceActive = savegame.ReadBool();
            this.nextGibTime = savegame.ReadInt();

            // spawnSpots
            // initialSpots
            // currentInitialSpot
            // newInfo
            // makingBuild
            // shakeSounds
            // Read out pending events
            idEvent.Restore(savegame);

            savegame.RestoreObjects();

            this.mpGame.Reset();

            this.mpGame.Precache();

            // free up any unused animations
            animationLib.FlushUnusedAnims();

            this.gamestate = GAMESTATE_ACTIVE;

            Printf("--------------------------------------\n");

            return true;
        }

        /*
         ===========
         idGameLocal::SaveGame

         save the current player state, level name, and level state
         the session may have written some data to the file already
         ============
         */
        @Override
        public void SaveGame(idFile saveGameFile) {
            int i;
            idEntity ent;
            idEntity link;

            final idSaveGame savegame = new idSaveGame(saveGameFile);

            if (g_flushSave.GetBool() == true) {
                // force flushing with each write... for tracking down
                // save game bugs.
                saveGameFile.ForceFlush();
            }

            savegame.WriteBuildNumber(BUILD_NUMBER);

            // go through all entities and threads and add them to the object list
            for (i = 0; i < MAX_GENTITIES; i++) {
                ent = this.entities[i];

                if (ent != null) {
                    if ((ent.GetTeamMaster() != null) && !ent.GetTeamMaster().equals(ent)) {
                        continue;
                    }
                    for (link = ent; link != null; link = link.GetNextTeamEntity()) {
                        savegame.AddObject(link);
                    }
                }
            }

            idList<idThread> threads;
            threads = idThread.GetThreads();

            for (i = 0; i < threads.Num(); i++) {
                savegame.AddObject(threads.oGet(i));
            }

            // write out complete object list
            savegame.WriteObjectList();

            this.program.Save(savegame);

            savegame.WriteInt(g_skill.GetInteger());

            savegame.WriteDict(this.serverInfo);

            savegame.WriteInt(this.numClients);
            for (i = 0; i < this.numClients; i++) {
                savegame.WriteDict(this.userInfo[ i]);
                savegame.WriteUsercmd(this.usercmds[ i]);
                savegame.WriteDict(this.persistentPlayerInfo[ i]);
            }

            for (i = 0; i < MAX_GENTITIES; i++) {
                savegame.WriteObject(this.entities[i]);
                savegame.WriteInt(this.spawnIds[ i]);
            }

            savegame.WriteInt(this.firstFreeIndex);
            savegame.WriteInt(this.num_entities);

            // enityHash is restored by idEntity::Restore setting the entity name.
            savegame.WriteObject(this.world);

            savegame.WriteInt(this.spawnedEntities.Num());
            for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                savegame.WriteObject(ent);
            }

            savegame.WriteInt(this.activeEntities.Num());
            for (ent = this.activeEntities.Next(); ent != null; ent = ent.activeNode.Next()) {
                savegame.WriteObject(ent);
            }

            savegame.WriteInt(this.numEntitiesToDeactivate);
            savegame.WriteBool(this.sortPushers);
            savegame.WriteBool(this.sortTeamMasters);
            savegame.WriteDict(this.persistentLevelInfo);

            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                savegame.WriteFloat(this.globalShaderParms[ i]);
            }

            savegame.WriteInt(this.random.GetSeed());
            savegame.WriteObject(this.frameCommandThread);

            // clip
            // push
            // pvs
            this.testmodel = null;
            this.testFx = null;

            savegame.WriteString(this.sessionCommand);

            // FIXME: save smoke particles
            savegame.WriteInt(this.cinematicSkipTime);
            savegame.WriteInt(this.cinematicStopTime);
            savegame.WriteInt(this.cinematicMaxSkipTime);
            savegame.WriteBool(this.inCinematic);
            savegame.WriteBool(this.skipCinematic);

            savegame.WriteBool(this.isMultiplayer);
            savegame.WriteInt(etoi(this.gameType));

            savegame.WriteInt(this.framenum);
            savegame.WriteInt(this.previousTime);
            savegame.WriteInt(this.time);

            savegame.WriteInt(this.vacuumAreaNum);

            savegame.WriteInt(this.entityDefBits);
            savegame.WriteBool(this.isServer);
            savegame.WriteBool(this.isClient);

            savegame.WriteInt(this.localClientNum);

            // snapshotEntities is used for multiplayer only
            savegame.WriteInt(this.realClientTime);
            savegame.WriteBool(this.isNewFrame);
            savegame.WriteFloat(this.clientSmoothing);

            savegame.WriteBool(this.mapCycleLoaded);
            savegame.WriteInt(this.spawnCount);

            if (NOT((Object[])this.locationEntities)) {
                savegame.WriteInt(0);
            } else {
                savegame.WriteInt(gameRenderWorld.NumAreas());
                for (i = 0; i < gameRenderWorld.NumAreas(); i++) {
                    savegame.WriteObject(this.locationEntities[ i]);
                }
            }

            savegame.WriteObject(this.camera);

            savegame.WriteMaterial(this.globalMaterial);

            this.lastAIAlertEntity.Save(savegame);
            savegame.WriteInt(this.lastAIAlertTime);

            savegame.WriteDict(this.spawnArgs);

            savegame.WriteInt(this.playerPVS.i);
            savegame.WriteInt(this.playerPVS.h);
            savegame.WriteInt(this.playerConnectedAreas.i);
            savegame.WriteInt(this.playerConnectedAreas.h);

            savegame.WriteVec3(this.gravity);

            // gamestate
            savegame.WriteBool(this.influenceActive);
            savegame.WriteInt(this.nextGibTime);

            // spawnSpots
            // initialSpots
            // currentInitialSpot
            // newInfo
            // makingBuild
            // shakeSounds
            // write out pending events
            idEvent.Save(savegame);

            savegame.Close();
        }

        @Override
        public void MapShutdown() {
            Printf("--------- Game Map Shutdown ----------\n");

            this.gamestate = GAMESTATE_SHUTDOWN;

            if (gameRenderWorld != null) {
                // clear any debug lines, text, and polygons
                gameRenderWorld.DebugClearLines(0);
                gameRenderWorld.DebugClearPolygons(0);
            }

            // clear out camera if we're in a cinematic
            if (this.inCinematic) {
                this.camera = null;
                this.inCinematic = false;
            }

            MapClear(true);

            // reset the script to the state it was before the map was started
            this.program.Restart();

            if (this.smokeParticles != null) {
                this.smokeParticles.Shutdown();
            }

            this.pvs.Shutdown();

            this.clip.Shutdown();
            idClipModel.ClearTraceModelCache();

            ShutdownAsyncNetwork();

            this.mapFileName.Clear();

            gameRenderWorld = null;
            gameSoundWorld = null;

            this.gamestate = GAMESTATE_NOMAP;

            Printf("--------------------------------------\n");
        }

        /*
         ===================
         idGameLocal::CacheDictionaryMedia

         This is called after parsing an EntityDef and for each entity spawnArgs before
         merging the entitydef.  It could be done post-merge, but that would
         avoid the fast pre-cache check associated with each entityDef
         ===================
         */
        @Override
        public void CacheDictionaryMedia(final idDict dict) {
            idKeyValue kv;

            if (dict == null) {
                if (cvarSystem.GetCVarBool("com_makingBuild")) {
                    DumpOggSounds();
                }
                return;
            }

            if (cvarSystem.GetCVarBool("com_makingBuild")) {
                GetShakeSounds(dict);
            }

            kv = dict.MatchPrefix("model");
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.MediaPrint("Precaching model %s\n", kv.GetValue());
                    // precache model/animations
                    if (declManager.FindType(DECL_MODELDEF, kv.GetValue(), false) == null) {
                        // precache the render model
                        renderModelManager.FindModel(kv.GetValue());
                        // precache .cm files only
                        CollisionModel_local.collisionModelManager.LoadModel(kv.GetValue(), true);
                    }
                }
                kv = dict.MatchPrefix("model", kv);
            }

            kv = dict.FindKey("s_shader");
            if ((kv != null) && (kv.GetValue().Length() != 0)) {
                declManager.FindType(DECL_SOUND, kv.GetValue());
            }

            kv = dict.MatchPrefix("snd", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.FindType(DECL_SOUND, kv.GetValue());
                }
                kv = dict.MatchPrefix("snd", kv);
            }

            kv = dict.MatchPrefix("gui", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    if ((0 == idStr.Icmp(kv.GetKey(), "gui_noninteractive"))
                            || (0 == idStr.Icmpn(kv.GetKey(), "gui_parm", 8))
                            || (0 == idStr.Icmp(kv.GetKey(), "gui_inventory"))) {
                        // unfortunate flag names, they aren't actually a gui
                    } else {
                        declManager.MediaPrint("Precaching gui %s\n", kv.GetValue());
                        final idUserInterface gui = uiManager.Alloc();
                        if (gui != null) {
                            gui.InitFromFile(kv.GetValue().getData());
                            uiManager.DeAlloc(gui);
                        }
                    }
                }
                kv = dict.MatchPrefix("gui", kv);
            }

            kv = dict.FindKey("texture");
            if ((kv != null) && (kv.GetValue().Length() != 0)) {
                declManager.FindType(DECL_MATERIAL, kv.GetValue());
            }

            kv = dict.MatchPrefix("mtr", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.FindType(DECL_MATERIAL, kv.GetValue());
                }
                kv = dict.MatchPrefix("mtr", kv);
            }

            // handles hud icons
            kv = dict.MatchPrefix("inv_icon", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.FindType(DECL_MATERIAL, kv.GetValue());
                }
                kv = dict.MatchPrefix("inv_icon", kv);
            }

            // handles teleport fx.. this is not ideal but the actual decision on which fx to use
            // is handled by script code based on the teleport number
            kv = dict.MatchPrefix("teleport", null);
            if ((kv != null) && (kv.GetValue().Length() != 0)) {
                final int teleportType = atoi(kv.GetValue());
                final String p = (teleportType != 0) ? va("fx/teleporter%d.fx", teleportType) : "fx/teleporter.fx";
                declManager.FindType(DECL_FX, p);
            }

            kv = dict.MatchPrefix("fx", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.MediaPrint("Precaching fx %s\n", kv.GetValue());
                    declManager.FindType(DECL_FX, kv.GetValue());
                }
                kv = dict.MatchPrefix("fx", kv);
            }

            kv = dict.MatchPrefix("smoke", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    idStr prtName = kv.GetValue();
                    final int dash = prtName.Find('-');
                    if (dash > 0) {
                        prtName = prtName.Left(dash);
                    }
                    declManager.FindType(DECL_PARTICLE, prtName);
                }
                kv = dict.MatchPrefix("smoke", kv);
            }

            kv = dict.MatchPrefix("skin", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.MediaPrint("Precaching skin %s\n", kv.GetValue());
                    declManager.FindType(DECL_SKIN, kv.GetValue());
                }
                kv = dict.MatchPrefix("skin", kv);
            }

            kv = dict.MatchPrefix("def", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    FindEntityDef(kv.GetValue().getData(), false);
                }
                kv = dict.MatchPrefix("def", kv);
            }

            kv = dict.MatchPrefix("pda_name", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.FindType(DECL_PDA, kv.GetValue(), false);
                }
                kv = dict.MatchPrefix("pda_name", kv);
            }

            kv = dict.MatchPrefix("video", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.FindType(DECL_VIDEO, kv.GetValue(), false);
                }
                kv = dict.MatchPrefix("video", kv);
            }

            kv = dict.MatchPrefix("audio", null);
            while (kv != null) {
                if (kv.GetValue().Length() != 0) {
                    declManager.FindType(DECL_AUDIO, kv.GetValue(), false);
                }
                kv = dict.MatchPrefix("audio", kv);
            }
        }

        @Override
        public void SpawnPlayer(int clientNum) {
            final idEntity[] ent = {null};
            final idDict args = new idDict();

            // they can connect
            Printf("SpawnPlayer: %d\n", clientNum);

            args.SetInt("spawn_entnum", clientNum);
            args.Set("name", va("player%d", clientNum + 1));
            args.Set("classname", this.isMultiplayer ? "player_doommarine_mp" : "player_doommarine");
            if (!SpawnEntityDef(args, ent) || (null == this.entities[ clientNum])) {
                Error("Failed to spawn player as '%s'", args.GetString("classname"));
            }

            // make sure it's a compatible class
            if (!ent[0].IsType(idPlayer.class)) {
                Error("'%s' spawned the player as a '%s'.  Player spawnclass must be a subclass of idPlayer.", args.GetString("classname"), ent[0].GetClassname());
            }

            if (clientNum >= this.numClients) {
                this.numClients = clientNum + 1;
            }

            this.mpGame.SpawnPlayer(clientNum);
        }

        @Override
        public gameReturn_t RunFrame(final usercmd_t[] clientCmds) {
            idEntity ent;
            int num;
            float ms;
            final idTimer timer_think = new idTimer(), timer_events = new idTimer(), timer_singlethink = new idTimer();
            final gameReturn_t ret = new gameReturn_t();
            idPlayer player;
            renderView_s view;

            if (_DEBUG) {
                if (this.isMultiplayer) {
                    assert (!this.isClient);
                }
            }

            player = GetLocalPlayer();

            if (!this.isMultiplayer && g_stopTime.GetBool()) {
                // clear any debug lines from a previous frame
                gameRenderWorld.DebugClearLines(this.time + 1);

                // set the user commands for this frame
//                memcpy(usercmds, clientCmds, numClients * sizeof(usercmds[ 0]));
                System.arraycopy(clientCmds, 0, this.usercmds, 0, this.numClients);

                if (player != null) {
                    player.Think();
                }
            } else {
                do {
                    // update the game time
                    this.framenum++;
                    this.previousTime = this.time;
                    this.time += msec;
                    this.realClientTime = this.time;

                    if (GAME_DLL) {
                        // allow changing SIMD usage on the fly
                        if (com_forceGenericSIMD.IsModified()) {
                            idSIMD.InitProcessor("game", com_forceGenericSIMD.GetBool());
                        }
                    }

                    // make sure the random number counter is used each frame so random events
                    // are influenced by the player's actions
                    this.random.RandomInt();

                    if (player != null) {
                        // update the renderview so that any gui videos play from the right frame
                        view = player.GetRenderView();
                        if (view != null) {
                            gameRenderWorld.SetRenderView(view);
                        }
                    }

                    // clear any debug lines from a previous frame
                    gameRenderWorld.DebugClearLines(this.time);

                    // clear any debug polygons from a previous frame
                    gameRenderWorld.DebugClearPolygons(this.time);

                    // set the user commands for this frame
//                    memcpy(usercmds, clientCmds, numClients * sizeof(usercmds[ 0]));
                    System.arraycopy(clientCmds, 0, this.usercmds, 0, this.numClients);

                    // free old smoke particles
                    this.smokeParticles.FreeSmokes();

                    // process events on the server
                    ServerProcessEntityNetworkEventQueue();

                    // update our gravity vector if needed.
                    UpdateGravity();

                    // create a merged pvs for all players
                    SetupPlayerPVS();

                    // sort the active entity list
                    SortActiveEntityList();

                    timer_think.Clear();
                    timer_think.Start();

                    // let entities think
                    if (g_timeentities.GetFloat() != 0) {
                        num = 0;
                        for (ent = this.activeEntities.Next(); ent != null; ent = ent.activeNode.Next()) {
                            if (g_cinematic.GetBool() && this.inCinematic && !ent.cinematic) {
                                ent.GetPhysics().UpdateTime(this.time);
                                continue;
                            }
                            timer_singlethink.Clear();
                            timer_singlethink.Start();
                            ent.Think();
                            timer_singlethink.Stop();
                            ms = (float) timer_singlethink.Milliseconds();
                            if (ms >= g_timeentities.GetFloat()) {
                                Printf("%d: entity '%s': %.1f ms\n", this.time, ent.name, ms);
                            }
                            num++;
                        }
                    } else {
                        if (this.inCinematic) {
                            num = 0;
                            for (ent = this.activeEntities.Next(); ent != null; ent = ent.activeNode.Next()) {
                                if (g_cinematic.GetBool() && !ent.cinematic) {
                                    ent.GetPhysics().UpdateTime(this.time);
                                    continue;
                                }
                                ent.Think();
                                num++;
                            }
                        } else {
                            num = 0;
                            for (ent = this.activeEntities.Next(); ent != null; ent = ent.activeNode.Next()) {
                                ent.Think();
                                if(num==117){
                                    DBG_RunFrame++;
                                }
                                num++;
                            }
//                            System.out.println("~~" + num);
                        }
                    }

                    // remove any entities that have stopped thinking
                    if (this.numEntitiesToDeactivate != 0) {
                        idEntity next_ent;
                        int c = 0;
                        for (ent = this.activeEntities.Next(); ent != null; ent = next_ent) {
                            next_ent = ent.activeNode.Next();
                            if (0 == ent.thinkFlags) {
                                ent.activeNode.Remove();
                                c++;
                            }
                        }
                        //assert( numEntitiesToDeactivate == c );
                        this.numEntitiesToDeactivate = 0;
                    }

                    timer_think.Stop();
                    timer_events.Clear();
                    timer_events.Start();

                    // service any pending events
                    idEvent.ServiceEvents();

                    timer_events.Stop();

                    // free the player pvs
                    FreePlayerPVS();

                    // do multiplayer related stuff
                    if (this.isMultiplayer) {
                        this.mpGame.Run();
                    }

                    // display how long it took to calculate the current game frame
                    if (g_frametime.GetBool()) {
                        Printf("game %d: all:%.1f th:%.1f ev:%.1f %d ents \n",
                                this.time, timer_think.Milliseconds() + timer_events.Milliseconds(),
                                timer_think.Milliseconds(), timer_events.Milliseconds(), num);
                    }

                    // build the return value
                    ret.consistencyHash = 0;
                    ret.sessionCommand[0] = 0;

                    if (!this.isMultiplayer && (player != null)) {
                        ret.health = player.health;
                        ret.heartRate = player.heartRate;
                        ret.stamina = idMath.FtoiFast(player.stamina);
                        // combat is a 0-100 value based on lastHitTime and lastDmgTime
                        // each make up 50% of the time spread over 10 seconds
                        ret.combat = 0;
                        if ((player.lastDmgTime > 0) && (this.time < (player.lastDmgTime + 10000))) {
                            ret.combat += (50.0f * (this.time - player.lastDmgTime)) / 10000;
                        }
                        if ((player.lastHitTime > 0) && (this.time < (player.lastHitTime + 10000))) {
                            ret.combat += (50.0f * (this.time - player.lastHitTime)) / 10000;
                        }
                    }

                    // see if a target_sessionCommand has forced a changelevel
                    if (this.sessionCommand.Length() != 0) {
//                        strncpy(ret.sessionCommand, sessionCommand, sizeof(ret.sessionCommand));
                        ret.sessionCommand = this.sessionCommand.getData().toCharArray();
                        break;
                    }

                    // make sure we don't loop forever when skipping a cinematic
                    if (this.skipCinematic && (this.time > this.cinematicMaxSkipTime)) {
                        Warning("Exceeded maximum cinematic skip length.  Cinematic may be looping infinitely.");
                        this.skipCinematic = false;
                        break;
                    }
                } while ((this.inCinematic || (this.time < this.cinematicStopTime)) && this.skipCinematic);
            }

            ret.syncNextGameFrame = this.skipCinematic;
            if (this.skipCinematic) {
                snd_system.soundSystem.SetMute(false);
                this.skipCinematic = false;
            }

            // show any debug info for this frame
            RunDebugInfo();
            D_DrawDebugLines();

            return ret;
        }
        private static int DBG_RunFrame = 0;

        /*
         ================
         idGameLocal::Draw

         makes rendering and sound system calls
         ================
         */
        @Override
        public boolean Draw(int clientNum) {
            if (this.isMultiplayer) {
                return this.mpGame.Draw(clientNum);
            }

            final idPlayer player = (idPlayer) this.entities[ clientNum];

            if (null == player) {
                return false;
            }

            // render the scene
            player.playerView.RenderPlayerView(player.hud);

            return true;
        }

        @Override
        public escReply_t HandleESC(idUserInterface[] gui) {
            if (this.isMultiplayer) {
                gui[0] = StartMenu();
                // we may set the gui back to NULL to hide it
                return ESC_GUI;
            }
            final idPlayer player = GetLocalPlayer();
            if (player != null) {
                if (player.HandleESC()) {
                    return ESC_IGNORE;
                } else {
                    return ESC_MAIN;
                }
            }
            return ESC_MAIN;
        }

        @Override
        public idUserInterface StartMenu() {
            if (!this.isMultiplayer) {
                return null;
            }
            return this.mpGame.StartMenu();
        }

        @Override
        public String HandleGuiCommands(final String menuCommand) {
            if (!this.isMultiplayer) {
                return null;
            }
            return this.mpGame.HandleGuiCommands(menuCommand);
        }

        @Override
        public void HandleMainMenuCommands(final String menuCommand, idUserInterface gui) {
        }

        @Override
        public allowReply_t ServerAllowClient(int numClients, final String IP, final String guid, final String password, char[] reason/*[MAX_STRING_CHARS]*/) {
            reason[0] = '\0';

            if ((this.serverInfo.GetInt("si_pure") != 0) && !this.mpGame.IsPureReady()) {
                idStr.snPrintf(reason, MAX_STRING_CHARS, "#str_07139");
                return ALLOW_NOTYET;
            }

            if (0 == this.serverInfo.GetInt("si_maxPlayers")) {
                idStr.snPrintf(reason, MAX_STRING_CHARS, "#str_07140");
                return ALLOW_NOTYET;
            }

            if (numClients >= this.serverInfo.GetInt("si_maxPlayers")) {
                idStr.snPrintf(reason, MAX_STRING_CHARS, "#str_07141");
                return ALLOW_NOTYET;
            }

            if (!cvarSystem.GetCVarBool("si_usepass")) {
                return ALLOW_YES;
            }

            final String pass = cvarSystem.GetCVarString("g_password");
//	if ( pass[ 0 ] == '\0' ) {
            if (pass.isEmpty()) {
                common.Warning("si_usepass is set but g_password is empty");
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "say si_usepass is set but g_password is empty");
                // avoids silent misconfigured state
                idStr.snPrintf(reason, MAX_STRING_CHARS, "#str_07142");
                return ALLOW_NOTYET;
            }

            if (0 == idStr.Cmp(pass, password)) {
                return ALLOW_YES;
            }

            idStr.snPrintf(reason, MAX_STRING_CHARS, "#str_07143");
            Printf("Rejecting client %s from IP %s: invalid password\n", guid, IP);
            return ALLOW_BADPASS;
        }

        @Override
        public void ServerClientConnect(int clientNum, final String guid) {
            // make sure no parasite entity is left
            if (this.entities[ clientNum] != null) {
                common.DPrintf("ServerClientConnect: remove old player entity\n");
//		delete entities[ clientNum ];
                this.entities[clientNum] = null;
            }
            this.userInfo[clientNum].Clear();
            this.mpGame.ServerClientConnect(clientNum);
            Printf("client %d connected.\n", clientNum);
        }

        @Override
        public void ServerClientBegin(int clientNum) {
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            // initialize the decl remap
            InitClientDeclRemap(clientNum);

            // send message to initialize decl remap at the client (this is always the very first reliable game message)
            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_INIT_DECL_REMAP);
            networkSystem.ServerSendReliableMessage(clientNum, outMsg);

            // spawn the player
            SpawnPlayer(clientNum);
            if (clientNum == this.localClientNum) {
                this.mpGame.EnterGame(clientNum);
            }

            // send message to spawn the player at the clients
            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_SPAWN_PLAYER);
            outMsg.WriteByte(clientNum);
            outMsg.WriteLong(this.spawnIds[ clientNum]);
            networkSystem.ServerSendReliableMessage(-1, outMsg);
        }

        @Override
        public void ServerClientDisconnect(int clientNum) {
            int i;
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_DELETE_ENT);
            outMsg.WriteBits((this.spawnIds[ clientNum] << GENTITYNUM_BITS) | clientNum, 32); // see GetSpawnId
            networkSystem.ServerSendReliableMessage(-1, outMsg);

            // free snapshots stored for this client
            FreeSnapshotsOlderThanSequence(clientNum, 0x7FFFFFFF);

            // free entity states stored for this client
            for (i = 0; i < MAX_GENTITIES; i++) {
                if (this.clientEntityStates[ clientNum][ i] != null) {
//                    entityStateAllocator.Free(clientEntityStates[ clientNum][ i]);
                    this.clientEntityStates[ clientNum][ i] = null;
                }
            }

            // clear the client PVS
//	memset( clientPVS[ clientNum ], 0, sizeof( clientPVS[ clientNum ] ) );
            Arrays.fill(this.clientPVS[ clientNum], 0);

            // delete the player entity
//	delete entities[ clientNum ];
            this.entities[ clientNum] = null;

            this.mpGame.DisconnectClient(clientNum);

        }

        /*
         ================
         idGameLocal::ServerWriteInitialReliableMessages

         Send reliable messages to initialize the client game up to a certain initial state.
         ================
         */
        @Override
        public void ServerWriteInitialReliableMessages(int clientNum) {
            int i;
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);
            entityNetEvent_s event;

            // spawn players
            for (i = 0; i < MAX_CLIENTS; i++) {
                if ((this.entities[i] == null) || (i == clientNum)) {
                    continue;
                }
                outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
                outMsg.BeginWriting();
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_SPAWN_PLAYER);
                outMsg.WriteByte(i);
                outMsg.WriteLong(this.spawnIds[ i]);
                networkSystem.ServerSendReliableMessage(clientNum, outMsg);
            }

            // send all saved events
            for (event = this.savedEventQueue.Start(); event != null; event = event.next) {
                outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
                outMsg.BeginWriting();
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_EVENT);
                outMsg.WriteBits(event.spawnId, 32);
                outMsg.WriteByte(event.event);
                outMsg.WriteLong(event.time);
                outMsg.WriteBits(event.paramsSize, idMath.BitsForInteger(MAX_EVENT_PARAM_SIZE));
                if (event.paramsSize != 0) {
                    outMsg.WriteData(event.paramsBuf, event.paramsSize);
                }

                networkSystem.ServerSendReliableMessage(clientNum, outMsg);
            }

            // update portals for opened doors
            final int numPortals = gameRenderWorld.NumPortals();
            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_PORTALSTATES);
            outMsg.WriteLong(numPortals);
            for (i = 0; i < numPortals; i++) {
                outMsg.WriteBits(gameRenderWorld.GetPortalState(/*(qhandle_t)*/(i + 1)), NUM_RENDER_PORTAL_BITS);
            }
            networkSystem.ServerSendReliableMessage(clientNum, outMsg);

            this.mpGame.ServerWriteInitialReliableMessages(clientNum);
        }

        /*
         ================
         idGameLocal::ServerWriteSnapshot

         Write a snapshot of the current game state for the given client.
         ================
         */
        @Override
        public void ServerWriteSnapshot(int clientNum, int sequence, idBitMsg msg, byte[] clientInPVS, int numPVSClients) {
            int i;
            final int[] msgSize = {0}, msgWriteBit = {0};
            idPlayer player, spectated;
            idEntity ent;
            pvsHandle_t pvsHandle;
            final idBitMsgDelta deltaMsg = new idBitMsgDelta();
            snapshot_s snapshot;
            entityState_s base, newBase;
            int numSourceAreas;
            final int[] sourceAreas = new int[idEntity.MAX_PVS_AREAS];
            idRandom tagRandom;

            player = (idPlayer) this.entities[ clientNum];
            if (null == player) {
                return;
            }
            if (player.spectating && (player.spectator != clientNum) && (this.entities[ player.spectator] != null)) {
                spectated = (idPlayer) this.entities[ player.spectator];
            } else {
                spectated = player;
            }

            // free too old snapshots
            FreeSnapshotsOlderThanSequence(clientNum, sequence - 64);

            // allocate new snapshot
            snapshot = new snapshot_s();//snapshotAllocator.Alloc();
            snapshot.sequence = sequence;
            snapshot.firstEntityState = null;
            snapshot.next = this.clientSnapshots[clientNum];
            this.clientSnapshots[clientNum] = snapshot;
//            memset(snapshot.pvs, 0, sizeof(snapshot.pvs));
            Arrays.fill(snapshot.pvs, 0);

            // get PVS for this player
            // don't use PVSAreas for networking - PVSAreas depends on animations (and md5 bounds), which are not synchronized
            numSourceAreas = gameRenderWorld.BoundsInAreas(spectated.GetPlayerPhysics().GetAbsBounds(), sourceAreas, idEntity.MAX_PVS_AREAS);
            pvsHandle = gameLocal.pvs.SetupCurrentPVS(sourceAreas, numSourceAreas, PVS_NORMAL);

            if (ASYNC_WRITE_TAGS) {
                tagRandom = new idRandom();
                tagRandom.SetSeed(this.random.RandomInt());
                msg.WriteLong(tagRandom.GetSeed());
            }

            // create the snapshot
            for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {

                // if the entity is not in the player PVS
                if (!ent.PhysicsTeamInPVS(pvsHandle) && (ent.entityNumber != clientNum)) {
                    continue;
                }

                // add the entity to the snapshot pvs
                snapshot.pvs[ ent.entityNumber >> 5] |= 1 << (ent.entityNumber & 31);

                // if that entity is not marked for network synchronization
                if (!ent.fl.networkSync) {
                    continue;
                }

                // save the write state to which we can revert when the entity didn't change at all
                msg.SaveWriteState(msgSize, msgWriteBit);

                // write the entity to the snapshot
                msg.WriteBits(ent.entityNumber, GENTITYNUM_BITS);

                base = this.clientEntityStates[clientNum][ent.entityNumber];
                if (base != null) {
                    base.state.BeginReading();
                }
                newBase = new entityState_s();//entityStateAllocator.Alloc();
                newBase.entityNumber = ent.entityNumber;
                newBase.state.Init(newBase.stateBuf);
                newBase.state.BeginWriting();

                deltaMsg.Init(base != null ? base.state : null, newBase.state, msg);

                deltaMsg.WriteBits(this.spawnIds[ ent.entityNumber], 32 - GENTITYNUM_BITS);
//                deltaMsg.WriteBits(ent.GetType().typeNum, idClass.GetTypeNumBits());//TODO:fix this.
                deltaMsg.WriteBits(ServerRemapDecl(-1, DECL_ENTITYDEF, ent.entityDefNumber), this.entityDefBits);

                // write the class specific data to the snapshot
                ent.WriteToSnapshot(deltaMsg);

                if (!deltaMsg.HasChanged()) {
                    msg.RestoreWriteState(msgSize[0], msgWriteBit[0]);
//                    entityStateAllocator.Free(newBase);
                } else {
                    newBase.next = snapshot.firstEntityState;
                    snapshot.firstEntityState = newBase;

                    if (ASYNC_WRITE_TAGS) {
                        msg.WriteLong(tagRandom.RandomInt());
                    }
                }
            }

            msg.WriteBits(ENTITYNUM_NONE, GENTITYNUM_BITS);

            // write the PVS to the snapshot
            if (ASYNC_WRITE_PVS) {
                for (i = 0; i < idEntity.MAX_PVS_AREAS; i++) {
                    if (i < numSourceAreas) {
                        msg.WriteLong(sourceAreas[ i]);
                    } else {
                        msg.WriteLong(0);
                    }
                }
                gameLocal.pvs.WritePVS(pvsHandle, msg);
            }
            for (i = 0; i < ENTITY_PVS_SIZE; i++) {
                msg.WriteDeltaLong(this.clientPVS[clientNum][i], snapshot.pvs[i]);
            }

            // free the PVS
            this.pvs.FreeCurrentPVS(pvsHandle);

            // write the game and player state to the snapshot
            base = this.clientEntityStates[clientNum][ENTITYNUM_NONE];	// ENTITYNUM_NONE is used for the game and player state
            if (base != null) {
                base.state.BeginReading();
            }
            newBase = new entityState_s();//entityStateAllocator.Alloc();
            newBase.entityNumber = ENTITYNUM_NONE;
            newBase.next = snapshot.firstEntityState;
            snapshot.firstEntityState = newBase;
            newBase.state.Init(newBase.stateBuf);
            newBase.state.BeginWriting();
            deltaMsg.Init(base != null ? base.state : null, newBase.state, msg);
            if (player.spectating && (player.spectator != player.entityNumber) && (gameLocal.entities[ player.spectator] != null) && gameLocal.entities[ player.spectator].IsType(idPlayer.class)) {
                ((idPlayer) gameLocal.entities[ player.spectator]).WritePlayerStateToSnapshot(deltaMsg);
            } else {
                player.WritePlayerStateToSnapshot(deltaMsg);
            }
            WriteGameStateToSnapshot(deltaMsg);

            // copy the client PVS string
//	memcpy( clientInPVS, snapshot.pvs, ( numPVSClients + 7 ) >> 3 );
            System.arraycopy(snapshot.pvs, 0, clientInPVS, 0, (numPVSClients + 7) >> 3);
            LittleRevBytes(clientInPVS, clientInPVS.length);
        }

        @Override
        public boolean ServerApplySnapshot(int clientNum, int sequence) {
            return ApplySnapshot(clientNum, sequence);
        }

        @Override
        public void ServerProcessReliableMessage(int clientNum, final idBitMsg msg) {
            int id;

            id = msg.ReadByte();
            switch (id) {
                case GAME_RELIABLE_MESSAGE_CHAT:
                case GAME_RELIABLE_MESSAGE_TCHAT: {
                    final char[] name = new char[128];
                    final char[] text = new char[128];

                    msg.ReadString(name, 128);
                    msg.ReadString(text, 128);

                    this.mpGame.ProcessChatMessage(clientNum, id == GAME_RELIABLE_MESSAGE_TCHAT, ctos(name), ctos(text), null);

                    break;
                }
                case GAME_RELIABLE_MESSAGE_VCHAT: {
                    final int index = msg.ReadLong();
                    final boolean team = msg.ReadBits(1) != 0;
                    this.mpGame.ProcessVoiceChat(clientNum, team, index);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_KILL: {
                    this.mpGame.WantKilled(clientNum);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_DROPWEAPON: {
                    this.mpGame.DropWeapon(clientNum);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_CALLVOTE: {
                    this.mpGame.ServerCallVote(clientNum, msg);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_CASTVOTE: {
                    final boolean vote = (msg.ReadByte() != 0);
                    this.mpGame.CastVote(clientNum, vote);
                    break;
                }
//if (false){
//		// uncomment this if you want to track when players are in a menu
//		case GAME_RELIABLE_MESSAGE_MENU: {
//			boolean menuUp = ( msg.ReadBits( 1 ) != 0 );
//			break;
//		}
//}
                case GAME_RELIABLE_MESSAGE_EVENT: {
                    entityNetEvent_s event;

                    // allocate new event
                    event = this.eventQueue.Alloc();
                    this.eventQueue.Enqueue(event, OUTOFORDER_DROP);

                    event.spawnId = msg.ReadBits(32);
                    event.event = msg.ReadByte();
                    event.time = msg.ReadLong();

                    event.paramsSize = msg.ReadBits(idMath.BitsForInteger(MAX_EVENT_PARAM_SIZE));
                    if (event.paramsSize != 0) {
                        if (event.paramsSize > MAX_EVENT_PARAM_SIZE) {
                            NetworkEventWarning(event, "invalid param size");
                            return;
                        }
                        msg.ReadByteAlign();
                        msg.ReadData(event.paramsBuf, event.paramsSize);
                    }
                    break;
                }
                default: {
                    Warning("Unknown client.server reliable message: %d", id);
                    break;
                }
            }
        }

        @Override
        public void ClientReadSnapshot(int clientNum, int sequence, final int gameFrame, final int gameTime, final int dupeUsercmds, final int aheadOfServer, final idBitMsg msg) {
            int baseBits;
            idEntity ent;
            idPlayer player;
            idMat3 viewAxis;
            idBounds viewBounds;
            entityState_s base;

            if (0 == net_clientShowSnapshot.GetInteger()) {
                return;
            }

            player = (idPlayer) this.entities[clientNum];
            if (null == player) {
                return;
            }

            viewAxis = player.viewAngles.ToMat3();
            viewBounds = player.GetPhysics().GetAbsBounds().Expand(net_clientShowSnapshotRadius.GetFloat());

            for (ent = this.snapshotEntities.Next(); ent != null; ent = ent.snapshotNode.Next()) {

                if ((net_clientShowSnapshot.GetInteger() == 1) && (ent.snapshotBits == 0)) {
                    continue;
                }

                final idBounds entBounds = ent.GetPhysics().GetAbsBounds();

                if (!entBounds.IntersectsBounds(viewBounds)) {
                    continue;
                }

                base = this.clientEntityStates[clientNum][ent.entityNumber];
                if (base != null) {
                    baseBits = base.state.GetNumBitsWritten();
                } else {
                    baseBits = 0;
                }

                if ((net_clientShowSnapshot.GetInteger() == 2) && (baseBits == 0)) {
                    continue;
                }

                gameRenderWorld.DebugBounds(colorGreen, entBounds);
                gameRenderWorld.DrawText(va("%d: %s (%d,%d bytes of %d,%d)\n", ent.entityNumber,
                        ent.name, ent.snapshotBits >> 3, ent.snapshotBits & 7, baseBits >> 3, baseBits & 7),
                        entBounds.GetCenter(), 0.1f, colorWhite, viewAxis, 1);
            }
        }

        @Override
        public boolean ClientApplySnapshot(int clientNum, int sequence) {
            return ApplySnapshot(clientNum, sequence);
        }

        @Override
        public void ClientProcessReliableMessage(int clientNum, final idBitMsg msg) {
            int id, line;
            idPlayer p;
            final idDict backupSI;

            InitLocalClient(clientNum);

            id = msg.ReadByte();
            switch (id) {
                case GAME_RELIABLE_MESSAGE_INIT_DECL_REMAP: {
                    InitClientDeclRemap(clientNum);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_REMAP_DECL: {
                    int type, index;
                    final char[] name = new char[MAX_STRING_CHARS];

                    type = msg.ReadByte();
                    index = msg.ReadLong();
                    msg.ReadString(name, MAX_STRING_CHARS);

                    final idDecl decl = declManager.FindType(declType_t.values()[type], ctos(name), false);
                    if (decl != null) {
                        if (index >= this.clientDeclRemap[clientNum][type].Num()) {
                            this.clientDeclRemap[clientNum][type].AssureSize(index + 1, -1);
                        }
                        this.clientDeclRemap[clientNum][type].oSet(index, decl.Index());
                    }
                    break;
                }
                case GAME_RELIABLE_MESSAGE_SPAWN_PLAYER: {
                    final int client = msg.ReadByte();
                    final int spawnId = msg.ReadLong();
                    if (null == this.entities[ client]) {
                        SpawnPlayer(client);
                        this.entities[ client].FreeModelDef();
                    }
                    // fix up the spawnId to match what the server says
                    // otherwise there is going to be a bogus delete/new of the client entity in the first ClientReadFromSnapshot
                    this.spawnIds[ client] = spawnId;
                    break;
                }
                case GAME_RELIABLE_MESSAGE_DELETE_ENT: {
                    final int spawnId = msg.ReadBits(32);
                    final idEntityPtr<idEntity> entPtr = new idEntityPtr<>();
                    if (!entPtr.SetSpawnId(spawnId)) {
                        break;
                    }
//			delete entPtr.GetEntity();
                    break;
                }
                case GAME_RELIABLE_MESSAGE_CHAT:
                case GAME_RELIABLE_MESSAGE_TCHAT: { // (client should never get a TCHAT though)
                    final char[] name = new char[128];
                    final char[] text = new char[128];
                    msg.ReadString(name, 128);
                    msg.ReadString(text, 128);
                    this.mpGame.AddChatLine("%s^0: %s\n", ctos(name), ctos(text));
                    break;
                }
                case GAME_RELIABLE_MESSAGE_SOUND_EVENT: {
                    final snd_evt_t snd_evt = snd_evt_t.values()[msg.ReadByte()];
                    this.mpGame.PlayGlobalSound(-1, snd_evt);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_SOUND_INDEX: {
                    final int index = gameLocal.ClientRemapDecl(DECL_SOUND, msg.ReadLong());
                    if ((index >= 0) && (index < declManager.GetNumDecls(DECL_SOUND))) {
                        final idSoundShader shader = declManager.SoundByIndex(index);
                        this.mpGame.PlayGlobalSound(-1, SND_COUNT, shader.GetName());
                    }
                    break;
                }
                case GAME_RELIABLE_MESSAGE_DB: {
                    final idMultiplayerGame.msg_evt_t msg_evt = idMultiplayerGame.msg_evt_t.values()[msg.ReadByte()];
                    int parm1, parm2;
                    parm1 = msg.ReadByte();
                    parm2 = msg.ReadByte();
                    this.mpGame.PrintMessageEvent(-1, msg_evt, parm1, parm2);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_EVENT: {
                    entityNetEvent_s event;

                    // allocate new event
                    event = this.eventQueue.Alloc();
                    this.eventQueue.Enqueue(event, OUTOFORDER_IGNORE);

                    event.spawnId = msg.ReadBits(32);
                    event.event = msg.ReadByte();
                    event.time = msg.ReadLong();

                    event.paramsSize = msg.ReadBits(idMath.BitsForInteger(MAX_EVENT_PARAM_SIZE));
                    if (event.paramsSize != 0) {
                        if (event.paramsSize > MAX_EVENT_PARAM_SIZE) {
                            NetworkEventWarning(event, "invalid param size");
                            return;
                        }
                        msg.ReadByteAlign();
                        msg.ReadData(event.paramsBuf, event.paramsSize);
                    }
                    break;
                }
                case GAME_RELIABLE_MESSAGE_SERVERINFO: {
                    final idDict info = new idDict();
                    msg.ReadDeltaDict(info, null);
                    gameLocal.SetServerInfo(info);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_RESTART: {
                    MapRestart();
                    break;
                }
                case GAME_RELIABLE_MESSAGE_TOURNEYLINE: {
                    line = msg.ReadByte();
                    p = (idPlayer) this.entities[ clientNum];
                    if (null == p) {
                        break;
                    }
                    p.tourneyLine = line;
                    break;
                }
                case GAME_RELIABLE_MESSAGE_STARTVOTE: {
                    final char[] voteString = new char[MAX_STRING_CHARS];
                    final int clientNum2 = msg.ReadByte();
                    msg.ReadString(voteString, MAX_STRING_CHARS);
                    this.mpGame.ClientStartVote(clientNum2, ctos(voteString));
                    break;
                }
                case GAME_RELIABLE_MESSAGE_UPDATEVOTE: {
                    final int result = msg.ReadByte();
                    final int yesCount = msg.ReadByte();
                    final int noCount = msg.ReadByte();
                    this.mpGame.ClientUpdateVote(idMultiplayerGame.vote_result_t.values()[result], yesCount, noCount);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_PORTALSTATES: {
                    final int numPortals = msg.ReadLong();
                    assert (numPortals == gameRenderWorld.NumPortals());
                    for (int i = 0; i < numPortals; i++) {
                        gameRenderWorld.SetPortalState( /*(qhandle_t)*/(i + 1), msg.ReadBits(NUM_RENDER_PORTAL_BITS));
                    }
                    break;
                }
                case GAME_RELIABLE_MESSAGE_PORTAL: {
                    final int /*qhandle_t*/ portal = msg.ReadLong();
                    final int blockingBits = msg.ReadBits(NUM_RENDER_PORTAL_BITS);
                    assert ((portal > 0) && (portal <= gameRenderWorld.NumPortals()));
                    gameRenderWorld.SetPortalState(portal, blockingBits);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_STARTSTATE: {
                    this.mpGame.ClientReadStartState(msg);
                    break;
                }
                case GAME_RELIABLE_MESSAGE_WARMUPTIME: {
                    this.mpGame.ClientReadWarmupTime(msg);
                    break;
                }
                default: {
                    Error("Unknown server.client reliable message: %d", id);
                    break;
                }
            }
        }

        @Override
        public gameReturn_t ClientPrediction(int clientNum, final usercmd_t[] clientCmds, boolean lastPredictFrame) {
            idEntity ent;
            idPlayer player;
            final gameReturn_t ret = new gameReturn_t();

            ret.sessionCommand[ 0] = '\0';

            player = (idPlayer) this.entities[clientNum];
            if (null == player) {
                return ret;
            }

            // check for local client lag
            if (networkSystem.ClientGetTimeSinceLastPacket() >= net_clientMaxPrediction.GetInteger()) {
                player.isLagged = true;
            } else {
                player.isLagged = false;
            }

            InitLocalClient(clientNum);

            // update the game time
            this.framenum++;
            this.previousTime = this.time;
            this.time += msec;

            // update the real client time and the new frame flag
            if (this.time > this.realClientTime) {
                this.realClientTime = this.time;
                this.isNewFrame = true;
            } else {
                this.isNewFrame = false;
            }

            // set the user commands for this frame
//            memcpy(usercmds, clientCmds, numClients * sizeof(usercmds[ 0]));
            System.arraycopy(clientCmds, 0, this.usercmds, 0, this.numClients);

            // run prediction on all entities from the last snapshot
            for (ent = this.snapshotEntities.Next(); ent != null; ent = ent.snapshotNode.Next()) {
                ent.thinkFlags |= TH_PHYSICS;
                ent.ClientPredictionThink();
            }

            // service any pending events
            idEvent.ServiceEvents();

            // show any debug info for this frame
            if (this.isNewFrame) {
                RunDebugInfo();
                D_DrawDebugLines();
            }

            if (this.sessionCommand.Length() != 0) {
//                strncpy(ret.sessionCommand, sessionCommand, sizeof(ret.sessionCommand));
                ret.sessionCommand = this.sessionCommand.getData().toCharArray();
            }
            return ret;
        }

        @Override
        public void GetClientStats(int clientNum, String[] data, final int len) {
            this.mpGame.PlayerStats(clientNum, data, len);
        }

        @Override
        public void SwitchTeam(int clientNum, int team) {

            idPlayer player;
            player = clientNum >= 0 ? (idPlayer) gameLocal.entities[ clientNum] : null;

            if (null == player) {
                return;
            }

            final int oldTeam = player.team;

            // Put in spectator mode
            if (team == -1) {
                ((idPlayer) this.entities[ clientNum]).Spectate(true);
            } // Switch to a team
            else {
                this.mpGame.SwitchToTeam(clientNum, oldTeam, team);
            }
        }

        @Override
        public boolean DownloadRequest(final String IP, final String guid, final String paks, char[] urls/*[MAX_STRING_CHARS ]*/) {
            if (0 == cvarSystem.GetCVarInteger("net_serverDownload")) {
                return false;
            }
            if (cvarSystem.GetCVarInteger("net_serverDownload") == 1) {
                // 1: single URL redirect
                if (cvarSystem.GetCVarString("si_serverURL").isEmpty()) {
                    common.Warning("si_serverURL not set");
                    return false;
                }
                idStr.snPrintf(urls, MAX_STRING_CHARS, "1;%s", cvarSystem.GetCVarString("si_serverURL"));
                return true;
            } else {
                // 2: table of pak URLs
                // first token is the game pak if request, empty if not requested by the client
                // there may be empty tokens for paks the server couldn't pinpoint - the order matters
                String reply = "2;";
                final idStrList dlTable = new idStrList(), pakList = new idStrList();
                int i, j;

                Tokenize(dlTable, cvarSystem.GetCVarString("net_serverDlTable"));
                Tokenize(pakList, paks);

                for (i = 0; i < pakList.Num(); i++) {
                    if (i > 0) {
                        reply += ";";
                    }
                    if (pakList.oGet(i).IsEmpty()) {//[ i ][ 0 ] == '\0' ) {
                        if (i == 0) {
                            // pak 0 will always miss when client doesn't ask for game bin
                            common.DPrintf("no game pak request\n");
                        } else {
                            common.DPrintf("no pak %d\n", i);
                        }
                        continue;
                    }
                    for (j = 0; j < dlTable.Num(); j++) {
                        if (!fileSystem.FilenameCompare(pakList.oGet(i), dlTable.oGet(j))) {
                            break;
                        }
                    }
                    if (j == dlTable.Num()) {
                        common.Printf("download for %s: pak not matched: %s\n", IP, pakList.oGet(i));
                    } else {
                        final idStr url = new idStr(cvarSystem.GetCVarString("net_serverDlBaseURL"));
                        url.AppendPath(dlTable.oGet(j));
                        reply += url;
                        common.DPrintf("download for %s: %s\n", IP, url);
                    }
                }

                idStr.Copynz(urls, reply, MAX_STRING_CHARS);
                return true;
            }
//	return false;
        }

        // ---------------------- Public idGameLocal Interface -------------------//TODO:
//public		void					Printf( const char *fmt, ... ) const id_attribute((format(printf,2,3)));
//public		void					DPrintf( const char *fmt, ... ) const id_attribute((format(printf,2,3)));
//public		void					Warning( const char *fmt, ... ) const id_attribute((format(printf,2,3)));
//public		void					DWarning( const char *fmt, ... ) const id_attribute((format(printf,2,3)));
//public		void					Error( const char *fmt, ... ) const id_attribute((format(printf,2,3)));

        /*
         ===================
         idGameLocal::LoadMap

         Initializes all map variables common to both save games and spawned games.
         ===================
         */
        public void LoadMap(final String mapName, int randseed) {
            int i;
            final boolean sameMap = ((this.mapFile != null) && (idStr.Icmp(this.mapFileName, mapName) == 0));

            // clear the sound system
            gameSoundWorld.ClearAllSoundEmitters();

            InitAsyncNetwork();

            if (!sameMap || ((this.mapFile != null) && this.mapFile.NeedsReload())) {
                // load the .map file
//		if ( mapFile) {
//			delete mapFile;
//		}
                this.mapFile = new idMapFile();
                if (!this.mapFile.Parse(mapName + ".map")) {
//			delete mapFile;
                    this.mapFile = null;
                    Error("Couldn't load %s", mapName);
                }
            }
            this.mapFileName.oSet(this.mapFile.GetName());

            // load the collision map
            CollisionModel_local.collisionModelManager.LoadMap(this.mapFile);

            this.numClients = 0;

            // initialize all entities for this game
            this.entities = new idEntity[this.entities.length];//	memset( entities, 0, sizeof( entities ) );
            this.usercmds = Stream.generate(usercmd_t::new).limit(this.usercmds.length).toArray(usercmd_t[]::new);//memset( usercmds, 0, sizeof( usercmds ) );
            this.spawnIds = new int[this.spawnIds.length];//memset( spawnIds, -1, sizeof( spawnIds ) );
            this.spawnCount = INITIAL_SPAWN_COUNT;

            this.spawnedEntities.Clear();
            this.activeEntities.Clear();
            this.numEntitiesToDeactivate = 0;
            this.sortTeamMasters = false;
            this.sortPushers = false;
            this.lastGUIEnt.oSet(null);
            this.lastGUI = 0;

            this.globalMaterial = null;

//	memset( globalShaderParms, 0, sizeof( globalShaderParms ) );
            this.globalShaderParms = new float[this.globalShaderParms.length];

            // always leave room for the max number of clients,
            // even if they aren't all used, so numbers inside that
            // range are NEVER anything but clients
            this.num_entities = MAX_CLIENTS;
            this.firstFreeIndex = MAX_CLIENTS;

            // reset the random number generator.
            this.random.SetSeed(this.isMultiplayer ? randseed : 0);

            this.camera = null;
            this.world = null;
            this.testmodel = null;
            this.testFx = null;

            this.lastAIAlertEntity.oSet(null);
            this.lastAIAlertTime = 0;

            this.previousTime = 0;
            this.time = 0;
            this.framenum = 0;
            this.sessionCommand.oSet("");
            this.nextGibTime = 0;

            this.vacuumAreaNum = -1;		// if an info_vacuum is spawned, it will set this

            if (null == this.editEntities) {
                this.editEntities = new idEditEntities();
            }

            this.gravity.Set(0, 0, -g_gravity.GetFloat());

            this.spawnArgs.Clear();

            this.skipCinematic = false;
            this.inCinematic = false;
            this.cinematicSkipTime = 0;
            this.cinematicStopTime = 0;
            this.cinematicMaxSkipTime = 0;

            this.clip.Init();
            this.pvs.Init();
            this.playerPVS.i = -1;
            this.playerConnectedAreas.i = -1;

            // load navigation system for all the different monster sizes
            for (i = 0; i < this.aasNames.Num(); i++) {
                this.aasList.oGet(i).Init(new idStr(this.mapFileName).SetFileExtension(this.aasNames.oGet(i)), this.mapFile.GetGeometryCRC());
            }

            // clear the smoke particle free list
            this.smokeParticles.Init();

            // cache miscellanious media references
            FindEntityDef("preCacheExtras", false);

            if (!sameMap) {
                this.mapFile.RemovePrimitiveData();
            }
        }

        public void LocalMapRestart() {
            int i, latchSpawnCount;

            Printf("----------- Game Map Restart ------------\n");

            this.gamestate = GAMESTATE_SHUTDOWN;

            for (i = 0; i < MAX_CLIENTS; i++) {
                if ((this.entities[ i] != null) && this.entities[ i].IsType(idPlayer.class)) {
                    ((idPlayer) this.entities[ i]).PrepareForRestart();
                }
            }

            this.eventQueue.Shutdown();
            this.savedEventQueue.Shutdown();

            MapClear(false);

            // clear the smoke particle free list
            this.smokeParticles.Init();

            // clear the sound system
            if (gameSoundWorld != null) {
                gameSoundWorld.ClearAllSoundEmitters();
            }

            // the spawnCount is reset to zero temporarily to spawn the map entities with the same spawnId
            // if we don't do that, network clients are confused and don't show any map entities
            latchSpawnCount = this.spawnCount;
            this.spawnCount = INITIAL_SPAWN_COUNT;

            this.gamestate = GAMESTATE_STARTUP;

            this.program.Restart();

            InitScriptForMap();

            MapPopulate();

            // once the map is populated, set the spawnCount back to where it was so we don't risk any collision
            // (note that if there are no players in the game, we could just leave it at it's current value)
            this.spawnCount = latchSpawnCount;

            // setup the client entities again
            for (i = 0; i < MAX_CLIENTS; i++) {
                if ((this.entities[ i] != null) && this.entities[ i].IsType(idPlayer.class)) {
                    ((idPlayer) this.entities[ i]).Restart();
                }
            }

            this.gamestate = GAMESTATE_ACTIVE;

            Printf("--------------------------------------\n");
        }

        public void MapRestart() {
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);
            idDict newInfo;
            int i;
            idKeyValue keyval, keyval2;

            if (this.isClient) {
                LocalMapRestart();
            } else {
                newInfo = cvarSystem.MoveCVarsToDict(CVAR_SERVERINFO);
                for (i = 0; i < newInfo.GetNumKeyVals(); i++) {
                    keyval = newInfo.GetKeyVal(i);
                    keyval2 = this.serverInfo.FindKey(keyval.GetKey());
                    if (null == keyval2) {
                        break;
                    }
                    // a select set of si_ changes will cause a full restart of the server
                    if ((keyval.GetValue().Cmp(keyval2.GetValue()) != 0)
                            && (NOT(keyval.GetKey().Cmp("si_pure")) || NOT(keyval.GetKey().Cmp("si_map")))) {
                        break;
                    }
                }
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "rescanSI");
                if (i != newInfo.GetNumKeyVals()) {
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "nextMap");
                } else {
                    outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
                    outMsg.WriteByte(GAME_RELIABLE_MESSAGE_RESTART);
                    outMsg.WriteBits(1, 1);
                    outMsg.WriteDeltaDict(this.serverInfo, null);
                    networkSystem.ServerSendReliableMessage(-1, outMsg);

                    LocalMapRestart();
                    this.mpGame.MapRestart();
                }
            }
        }

        public void Printf(final String fmt, final Object... args) {
//	va_list		argptr;
//	char		text[MAX_STRING_CHARS];
//
//	va_start( argptr, fmt );
//	idStr::vsnPrintf( text, sizeof( text ), fmt, argptr );
//	va_end( argptr );

            common.Printf("%s", String.format(fmt, args));
        }

        public void Warning(final String fmt, final Object... args) {
//	va_list		argptr;
            final StringBuilder text = new StringBuilder(MAX_STRING_CHARS);
            idThread thread;
//
//	va_start( argptr, fmt );
//	idStr::vsnPrintf( text, sizeof( text ), fmt, argptr );
//	va_end( argptr );

            text.append(String.format(fmt, args));
            thread = idThread.CurrentThread();
            if (thread != null) {
                thread.Warning("%s", text);
            } else {
                common.Warning("%s", text);
            }
        }

        public static void Error(final String fmt, final Object... args) {
//	va_list		argptr;
            final StringBuilder text = new StringBuilder(MAX_STRING_CHARS);
            idThread thread;
//
//	va_start( argptr, fmt );
//	idStr::vsnPrintf( text, sizeof( text ), fmt, argptr );
//	va_end( argptr );

            text.append(String.format(fmt, args));
            thread = idThread.CurrentThread();
            if (thread != null) {
                thread.Error("%s", text);
            } else {
                common.Error("%s", text);
            }
        }

        /*
         ===============
         gameError
         ===============
         */
        public static void gameError(final String fmt, final Object... args) {
//	va_list		argptr;
//	char		text[MAX_STRING_CHARS];
//
//	va_start( argptr, fmt );
//	idStr::vsnPrintf( text, sizeof( text ), fmt, argptr );
//	va_end( argptr );

            idGameLocal.Error("%s", String.format(fmt, args));
        }

        public void DWarning(final String fmt, final Object... args) {
//	va_list		argptr;
            final StringBuilder text = new StringBuilder(MAX_STRING_CHARS);
            idThread thread;

            if (!developer.GetBool()) {
                return;
            }

            text.append(String.format(fmt, args));
//	va_start( argptr, fmt );
//	idStr::vsnPrintf( text, sizeof( text ), fmt, argptr );
//	va_end( argptr );

            thread = idThread.CurrentThread();
            if (thread != null) {
                thread.Warning("%s", text);
            } else {
                common.DWarning("%s", text);
            }
        }

        public void DPrintf(final String fmt, final Object... args) {
//	va_list		argptr;
//	char		text[MAX_STRING_CHARS];

            if (!developer.GetBool()) {
                return;
            }

//	va_start( argptr, fmt );
//	idStr::vsnPrintf( text, sizeof( text ), fmt, argptr );
//	va_end( argptr );
//            
            common.Printf("%s", String.format(fmt, args));
        }

        public static class MapRestart_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new MapRestart_f();

            private MapRestart_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (!gameLocal.isMultiplayer || gameLocal.isClient) {
                    common.Printf("server is not running - use spawnServer\n");
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "spawnServer\n");
                    return;
                }

                gameLocal.MapRestart();
            }
        }

        public boolean NextMap() {	// returns wether serverinfo settings have been modified
            function_t func;
            idThread thread;
            idDict newInfo;
            idKeyValue keyval, keyval2;
            int i;

            if (isNotNullOrEmpty(g_mapCycle.GetString())) {
                Printf(common.GetLanguageDict().GetString("#str_04294"));
                return false;
            }
            if (fileSystem.ReadFile(g_mapCycle.GetString(), null, null) < 0) {
                if (fileSystem.ReadFile(va("%s.scriptcfg", g_mapCycle.GetString()), null, null) < 0) {
                    Printf("map cycle script '%s': not found\n", g_mapCycle.GetString());
                    return false;
                } else {
                    g_mapCycle.SetString(va("%s.scriptcfg", g_mapCycle.GetString()));
                }
            }

            Printf("map cycle script: '%s'\n", g_mapCycle.GetString());
            func = this.program.FindFunction("mapcycle::cycle");
            if (NOT(func)) {
                this.program.CompileFile(g_mapCycle.GetString());
                func = this.program.FindFunction("mapcycle::cycle");
            }
            if (NOT(func)) {
                Printf("Couldn't find mapcycle::cycle\n");
                return false;
            }
            thread = new idThread(func);
            thread.Start();
//	delete thread;

            newInfo = cvarSystem.MoveCVarsToDict(CVAR_SERVERINFO);
            for (i = 0; i < newInfo.GetNumKeyVals(); i++) {
                keyval = newInfo.GetKeyVal(i);
                keyval2 = this.serverInfo.FindKey(keyval.GetKey());
                if ((null == keyval2) || (keyval.GetValue().Cmp(keyval2.GetValue()) != 0)) {
                    break;
                }
            }
            return (i != newInfo.GetNumKeyVals());
        }

        public static class NextMap_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new NextMap_f();

            private NextMap_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                if (!gameLocal.isMultiplayer || gameLocal.isClient) {
                    common.Printf("server is not running\n");
                    return;
                }

                gameLocal.NextMap();
                // next map was either voted for or triggered by a server command - always restart
                gameLocal.MapRestart();
            }
        }

        /*
         ================
         idGameLocal::GetLevelMap

         should only be used for in-game level editing
         ================
         */
        public idMapFile GetLevelMap() {
            if ((this.mapFile != null) && this.mapFile.HasPrimitiveData()) {
                return this.mapFile;
            }
            if (0 == this.mapFileName.Length()) {
                return null;
            }

//	if ( mapFile ) {
//		delete mapFile;
//	}
            this.mapFile = new idMapFile();
            if (!this.mapFile.Parse(this.mapFileName)) {
//		delete mapFile;
                this.mapFile = null;
            }

            return this.mapFile;
        }

        public String GetMapName() {
            return this.mapFileName.getData();
        }

        public int NumAAS() {
            return this.aasList.Num();
        }

        public idAAS GetAAS(int num) {
            if ((num >= 0) && (num < this.aasList.Num())) {
                if ((this.aasList.oGet(num) != null) && (this.aasList.oGet(num).GetSettings() != null)) {
                    return this.aasList.oGet(num);
                }
            }
            return null;
        }

        public idAAS GetAAS(final String name) {
            int i;

            for (i = 0; i < this.aasNames.Num(); i++) {
                if (this.aasNames.oGet(i).equals(name)) {
                    if (NOT(this.aasList.oGet(i).GetSettings())) {
                        return null;
                    } else {
                        return this.aasList.oGet(i);
                    }
                }
            }
            return null;
        }

        public void SetAASAreaState(final idBounds bounds, final int areaContents, boolean closed) {
            int i;

            for (i = 0; i < this.aasList.Num(); i++) {
                this.aasList.oGet(i).SetAreaState(bounds, areaContents, closed);
            }
        }

        public int/*aasHandle_t*/ AddAASObstacle(final idBounds bounds) {
            int i;
            int obstacle;
            int check;

            if (0 == this.aasList.Num()) {
                return -1;
            }

            obstacle = this.aasList.oGet(0).AddObstacle(bounds);
            for (i = 1; i < this.aasList.Num(); i++) {
                check = this.aasList.oGet(i).AddObstacle(bounds);
                assert (check == obstacle);
            }

            return obstacle;
        }

        public void RemoveAASObstacle(final int/*aasHandle_t*/ handle) {
            int i;

            for (i = 0; i < this.aasList.Num(); i++) {
                this.aasList.oGet(i).RemoveObstacle(handle);
            }
        }

        public void RemoveAllAASObstacles() {
            int i;

            for (i = 0; i < this.aasList.Num(); i++) {
                this.aasList.oGet(i).RemoveAllObstacles();
            }
        }

        public boolean CheatsOk(boolean requirePlayer /*= true*/) {
            idPlayer player;

            if (this.isMultiplayer && !cvarSystem.GetCVarBool("net_allowCheats")) {
                Printf("Not allowed in multiplayer.\n");
                return false;
            }

            if (developer.GetBool()) {
                return true;
            }

            player = GetLocalPlayer();
            if (!requirePlayer || ((player != null) && (player.health > 0))) {
                return true;
            }

            Printf("You must be alive to use this command.\n");

            return false;
        }

        public boolean CheatsOk() {
            return CheatsOk(true);
        }

        public void SetSkill(int value) {
            int skill_level;

            if (value < 0) {
                skill_level = 0;
            } else if (value > 3) {
                skill_level = 3;
            } else {
                skill_level = value;
            }

            g_skill.SetInteger(skill_level);
        }

        /*
         ==============
         idGameLocal::GameState

         Used to allow entities to know if they're being spawned during the initial spawn.
         ==============
         */
        public gameState_t GameState() {
            return this.gamestate;
        }

        public idEntity SpawnEntityType(final idTypeInfo classdef, final idDict args /*= NULL*/, boolean bIsClientReadSnapshot /*= false*/) {
            idClass obj;

            if (_DEBUG) {
                if (this.isClient) {
                    assert (bIsClientReadSnapshot);
                }
            }

            if (!classdef.IsType(idEntity.class)) {
                Error("Attempted to spawn non-entity class '%s'", classdef.classname);
            }

            try {
                if (args != null) {
                    this.spawnArgs.oSet(args);
                } else {
                    this.spawnArgs.Clear();
                }
                obj = (idClass) classdef.CreateInstance.run();
                obj.CallSpawn();
            } catch (final idAllocError ex) {
                obj = null;
            }
            this.spawnArgs.Clear();

            return (idEntity) obj;
        }

        @Deprecated
        public idEntity SpawnEntityType(final idTypeInfo classdef, final idDict args /*= NULL*/) {
            return SpawnEntityType(classdef, args, false);
        }

        public idEntity SpawnEntityType(final Class<?> classdef, final idDict args /*= NULL*/) {
            idEntity obj = null;

            if (!idEntity.class.isAssignableFrom(classdef)) {
                Error("Attempted to spawn non-entity class '%s'", classdef);
            }

            if (args != null) {
                this.spawnArgs.oSet(args);
            } else {
                this.spawnArgs.Clear();
            }

            try {
                obj = (idEntity) classdef.newInstance();
                obj.Spawn();
            } catch (InstantiationException | IllegalAccessException ex) {
            }

            return obj;
        }

        @Deprecated
        public idEntity SpawnEntityType(final idTypeInfo classdef) {
            return SpawnEntityType(classdef, null);
        }

        public idEntity SpawnEntityType(final Class<?> classdef) {
            return SpawnEntityType(classdef, null);
        }

        /*
         ===================
         idGameLocal::SpawnEntityDef

         Finds the spawn function for the entity and calls it,
         returning false if not found
         ===================
         */private static int DBG_SpawnEntityDef = 0;
        public boolean SpawnEntityDef(final idDict args, idEntity[] ent /*= NULL*/, boolean setDefaults /*= true*/) {
            final String[] classname = {null};DBG_SpawnEntityDef++;
            final String[] spawn = {null};
            final idTypeInfo cls;
//            idClass obj;
            String error = "";
            final String[] name = new String[1];

            if (ent != null) {
                ent[0] = null;
            }

            this.spawnArgs.oSet(args);

            if (this.spawnArgs.GetString("name", "", name)) {
                error = String.format(" on '%s'", name[0]);
            }

            this.spawnArgs.GetString("classname", null, classname);

            final idDeclEntityDef def = FindEntityDef(classname[0], false);

            if (null == def) {
                Warning("Unknown classname '%s'%s.", classname[0], error);
                return false;
            }

            this.spawnArgs.SetDefaults(def.dict);

            // check if we should spawn a class object
            this.spawnArgs.GetString("spawnclass", null, spawn);
            if (spawn[0] != null) {
                final idEntity obj;
                switch (spawn[0]) {//TODO:mayhaps implement some other cases
                    case "idWorldspawn":
                        obj = new idWorldspawn();
                        break;
                    case "idStaticEntity":
                        obj = new idStaticEntity();
                        break;
                    case "idPathCorner":
                        obj = new idPathCorner();
                        break;
                    case "idTrigger_Multi":
                        obj = new idTrigger_Multi();
                        break;
                    case "idTarget_Tip":
                        obj = new idTarget_Tip();
                        break;
                    case "idTarget_Remove":
                        obj = new idTarget_Remove();
                        break;
                    case "idMover":
                        obj = new idMover();
                        break;
                    case "idMoveable":
                        obj = new idMoveable();
                        break;
                    case "idLight":
                        obj = new idLight();
                        break;
                    case "idCameraAnim":
                        obj = new idCameraAnim();
                        break;
                    case "idAI":
                        obj = new idAI();
                        break;
                    case "idFuncEmitter":
                        obj = new idFuncEmitter();
                        break;
                    case "idAnimated":
                        obj = new idAnimated();
                        break;
                    case "idBFGProjectile":
                        obj = new idBFGProjectile();
                        break;
                    case "idTrigger_Hurt":
                        obj = new idTrigger_Hurt();
                        break;
                    case "idMoveablePDAItem":
                        obj = new idMoveablePDAItem();
                        break;
                    case "idLocationEntity":
                        obj = new idLocationEntity();
                        break;
                    case "idPlayerStart":
                        obj = new idPlayerStart();
                        break;
                    case "idSound":
                        obj = new idSound();
                        break;
                    case "idTarget_GiveEmail":
                        obj = new idTarget_GiveEmail();
                        break;
                    case "idTarget_SetPrimaryObjective":
                        obj = new idTarget_SetPrimaryObjective();
                        break;
                    case "idObjectiveComplete":
                        obj = new idObjectiveComplete();
                        break;
                    case "idTarget":
                        obj = new idTarget();
                        break;
                    case "idCameraView":
                        obj = new idCameraView();
                        break;
                    case "idObjective":
                        obj = new idObjective();
                        break;
                    case "idTarget_SetShaderParm":
                        obj = new idTarget_SetShaderParm();
                        break;
                    case "idTarget_FadeEntity":
                        obj = new idTarget_FadeEntity();
                        break;
                    case "idEntityFx":
                        obj = new idEntityFx();
                        break;
                    case "idItem":
                        obj = new idItem();
                        break;
                    case "idSplinePath":
                        obj = new idSplinePath();
                        break;
                    case "idAFEntity_Generic":
                        obj = new idAFEntity_Generic();
                        break;
                    case "idDoor":
                        obj = new idDoor();
                        break;
                    case "idProjectile":
                        obj = new idProjectile();
                        break;
                    case "idTrigger_Count":
                        obj = new idTrigger_Count();
                        break;
                    case "idTarget_EndLevel":
                        obj = new idTarget_EndLevel();
                        break;
                    case "idTarget_CallObjectFunction":
                        obj = new idTarget_CallObjectFunction();
                        break;
                    case "idTrigger_Fade":
                        obj = new idTrigger_Fade();
                        break;
                    case "idPDAItem":
                        obj = new idPDAItem();
                        break;
                    case "idVideoCDItem":
                        obj = new idVideoCDItem();
                        break;
                    case "idLocationSeparatorEntity":
                        obj = new idLocationSeparatorEntity();
                        break;
                    case "idPlayer":
                        obj = new idPlayer();
                        break;
                    case "idDebris":
                        obj = new idDebris();
                        break;
                    case "idSpawnableEntity":
                        obj = new idSpawnableEntity();
                        break;
                    case "idTarget_LightFadeIn":
                        obj = new idTarget_LightFadeIn();
                        break;
                    case "idTarget_LightFadeOut":
                        obj = new idTarget_LightFadeOut();
                        break;
                    case "idItemPowerup":
                        obj = new idItemPowerup();
                        break;
                    case "idForceField":
                        obj = new idForceField();
                        break;
                    case "idTarget_LockDoor":
                        obj = new idTarget_LockDoor();
                        break;
                    case "idTarget_SetInfluence":
                        obj = new idTarget_SetInfluence();
                        break;
                    case "idExplodingBarrel":
                        obj = new idExplodingBarrel();
                        break;
                    case "idTarget_EnableLevelWeapons":
                        obj = new idTarget_EnableLevelWeapons();
                        break;
                    case "idAFEntity_WithAttachedHead":
                        obj = new idAFEntity_WithAttachedHead();
                        break;
                    case "idCombatNode":
                        obj = new idCombatNode();
                        break;
                    case "idFuncAASObstacle":
                        obj = new idFuncAASObstacle();
                        break;
                    case "idVacuumEntity":
                        obj = new idVacuumEntity();
                        break;
                    case "idRotater":
                        obj = new idRotater();
                        break;
                    case "idElevator":
                        obj = new idElevator();
                        break;
                    case "idShaking":
                        obj = new idShaking();
                        break;
                    case "idFuncRadioChatter":
                        obj = new idFuncRadioChatter();
                        break;
                    case "idFuncPortal":
                        obj = new idFuncPortal();
                        break;
                    case "idMoveableItem":
                        obj = new idMoveableItem();
                        break;
                    case "idFuncSmoke":
                        obj = new idFuncSmoke();
                        break;
                    case "idPhantomObjects":
                        obj = new idPhantomObjects();
                        break;
                    case "idBeam":
                        obj = new idBeam();
                        break;
                    case "idExplodable":
                        obj = new idExplodable();
                        break;
                    case "idEarthQuake":
                        obj = new idEarthQuake();
                        break;
                    case "idGuidedProjectile":
                        obj = new idGuidedProjectile();
                        break;
                    case "idTarget_Show":
                        obj = new idTarget_Show();
                        break;
                    case "idBrittleFracture":
                        obj = new idBrittleFracture();
                        break;
                    case "idTrigger_Timer":
                        obj = new idTrigger_Timer();
                        break;
                    case "idPendulum":
                        obj = new idPendulum();
                        break;
                    default:
                        obj = null;
                }

                obj.Spawn();

//                obj = idClass.GetClass(spawn[0]);
//                if (NOT(cls)) {
//                    Warning("Could not spawn '%s'.  Class '%s' not found%s.", classname[0], spawn[0], error);
//                    return false;
//                }
//
//                obj = (idClass) cls.CreateInstance.run();
//                if (NOT(obj)) {
//                    Warning("Could not spawn '%s'. Instance could not be created%s.", classname[0], error);
//                    return false;
//                }
//
//                obj.CallSpawn();
                
                if (ent != null) {// && obj.IsType(idEntity.class)) {
                    ent[0] = obj;
                }

                return true;
            }

            // check if we should call a script function to spawn
            this.spawnArgs.GetString("spawnfunc", null, spawn);
            if (spawn[0] != null) {
                final function_t func = this.program.FindFunction(spawn[0]);
                if (null == func) {
                    Warning("Could not spawn '%s'.  Script function '%s' not found%s.", classname[0], spawn[0], error);
                    return false;
                }
                final idThread thread = new idThread(func);
                thread.DelayedStart(0);
                return true;
            }

            Warning("%s doesn't include a spawnfunc or spawnclass%s.", classname[0], error);
            return false;
        }

        public boolean SpawnEntityDef(final idDict args, idEntity[] ent /*= NULL*/) {
            return SpawnEntityDef(args, ent, true);
        }

        public boolean SpawnEntityDef(final idDict args) {
            return SpawnEntityDef(args, null);
        }

        public int GetSpawnId(final idEntity ent) {
            return (gameLocal.spawnIds[ ent.entityNumber] << GENTITYNUM_BITS) | ent.entityNumber;
        }

        public idDeclEntityDef FindEntityDef(final String name, boolean makeDefault /*= true*/) {
            idDecl decl = null;
            if (this.isMultiplayer) {
                decl = declManager.FindType(DECL_ENTITYDEF, va("%s_mp", name), false);
            }
            if (null == decl) {
                decl = declManager.FindType(DECL_ENTITYDEF, name, makeDefault);
            }
            return (idDeclEntityDef) decl;
        }

        public idDeclEntityDef FindEntityDef(final String name) {
            return FindEntityDef(name, true);
        }

        public idDict FindEntityDefDict(final String name, boolean makeDefault /*= true*/) {
            final idDeclEntityDef decl = FindEntityDef(name, makeDefault);
            return decl != null ? decl.dict : null;
        }

        public idDict FindEntityDefDict(final String name) {
            return FindEntityDefDict(name, true);
        }

        public idDict FindEntityDefDict(final idStr name) {
            return FindEntityDefDict(name.getData());
        }

        public void RegisterEntity(idEntity ent) {
            final int[] spawn_entnum = {0};

            if (this.spawnCount >= (1 << (32 - GENTITYNUM_BITS))) {
                Error("idGameLocal::RegisterEntity: spawn count overflow");
            }

            if (!this.spawnArgs.GetInt("spawn_entnum", "0", spawn_entnum)) {
                while ((this.entities[this.firstFreeIndex] != null) && (this.firstFreeIndex < ENTITYNUM_MAX_NORMAL)) {
                    this.firstFreeIndex++;
                }
                if (this.firstFreeIndex >= ENTITYNUM_MAX_NORMAL) {
                    Error("no free entities");
                }
                spawn_entnum[0] = this.firstFreeIndex++;
            }

            this.entities[ spawn_entnum[0]] = ent;
            this.spawnIds[ spawn_entnum[0]] = this.spawnCount++;
            ent.entityNumber = spawn_entnum[0];
            ent.spawnNode.AddToEnd(this.spawnedEntities);
            ent.spawnArgs.TransferKeyValues(this.spawnArgs);

            if (spawn_entnum[0] >= this.num_entities) {
                this.num_entities++;
            }
        }

        public void UnregisterEntity(idEntity ent) {
            assert (ent != null);

            if (this.editEntities != null) {
                this.editEntities.RemoveSelectedEntity(ent);
            }

            if ((ent.entityNumber != ENTITYNUM_NONE) && (this.entities[ ent.entityNumber] == ent)) {
                ent.spawnNode.Remove();
                this.entities[ ent.entityNumber] = null;
                this.spawnIds[ ent.entityNumber] = -1;
                if ((ent.entityNumber >= MAX_CLIENTS) && (ent.entityNumber < this.firstFreeIndex)) {
                    this.firstFreeIndex = ent.entityNumber;
                }
                ent.entityNumber = ENTITYNUM_NONE;
            }
        }

        public boolean RequirementMet(idEntity activator, final idStr requires, int removeItem) {
            if (requires.Length() != 0) {
                if (activator.IsType(idPlayer.class)) {
                    final idPlayer player = (idPlayer) activator;
                    final idDict item = player.FindInventoryItem(requires);
                    if (item != null) {
                        if (removeItem != 0) {
                            player.RemoveInventoryItem(item);
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            }

            return true;
        }

        public void AlertAI(idEntity ent) {
            if ((ent != null) && ent.IsType(idActor.class)) {
                // alert them for the next frame
                this.lastAIAlertTime = this.time + msec;
                this.lastAIAlertEntity.oSet((idActor) ent);
            }
        }

        public idActor GetAlertEntity() {
            if (this.lastAIAlertTime >= this.time) {
                return this.lastAIAlertEntity.GetEntity();
            }

            return null;
        }

        /*
         ================
         idGameLocal::InPlayerPVS

         should only be called during entity thinking and event handling
         ================
         */
        public boolean InPlayerPVS(idEntity ent) {
            if (this.playerPVS.i == -1) {
                return false;
            }
            return this.pvs.InCurrentPVS(this.playerPVS, ent.GetPVSAreas(), ent.GetNumPVSAreas());
        }

        /*
         ================
         idGameLocal::InPlayerConnectedArea

         should only be called during entity thinking and event handling
         ================
         */
        public boolean InPlayerConnectedArea(idEntity ent) {
            if (this.playerConnectedAreas.i == -1) {
                return false;
            }
            return this.pvs.InCurrentPVS(this.playerConnectedAreas, ent.GetPVSAreas(), ent.GetNumPVSAreas());
        }

        public void SetCamera(idCamera cam) {
            int i;
            idEntity ent;
            idAI ai;

            // this should fix going into a cinematic when dead.. rare but happens
            idPlayer client = GetLocalPlayer();
            if ((client.health <= 0) || client.AI_DEAD.operator()) {
                return;
            }

            this.camera = cam;
            if (this.camera != null) {
                this.inCinematic = true;

                if (this.skipCinematic && this.camera.spawnArgs.GetBool("disconnect")) {
                    this.camera.spawnArgs.SetBool("disconnect", false);
                    cvarSystem.SetCVarFloat("r_znear", 3.0f);
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "disconnect\n");
                    this.skipCinematic = false;
                    return;
                }

                if (this.time > this.cinematicStopTime) {
                    this.cinematicSkipTime = (int) (this.time + CINEMATIC_SKIP_DELAY);
                }

                // set r_znear so that transitioning into/out of the player's head doesn't clip through the view
                cvarSystem.SetCVarFloat("r_znear", 1.0f);

                // hide all the player models
                for (i = 0; i < this.numClients; i++) {
                    if (this.entities[ i] != null) {
                        client = (idPlayer) this.entities[ i];
                        client.EnterCinematic();
                    }
                }

                if (!cam.spawnArgs.GetBool("ignore_enemies")) {
                    // kill any active monsters that are enemies of the player
                    for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                        if (ent.cinematic || ent.fl.isDormant) {
                            // only kill entities that aren't needed for cinematics and aren't dormant
                            continue;
                        }

                        if (ent.IsType(idAI.class)) {
                            ai = (idAI) ent;
                            if (NOT(ai.GetEnemy()) || !ai.IsActive()) {
                                // no enemy, or inactive, so probably safe to ignore
                                continue;
                            }
                        } else if (ent.IsType(idProjectile.class)) {
                            // remove all projectiles
                        } else if (ent.spawnArgs.GetBool("cinematic_remove")) {
                            // remove anything marked to be removed during cinematics
                        } else {
                            // ignore everything else
                            continue;
                        }

                        // remove it
                        DPrintf("removing '%s' for cinematic\n", ent.GetName());
                        ent.PostEventMS(EV_Remove, 0);
                    }
                }

            } else {
                this.inCinematic = false;
                this.cinematicStopTime = this.time + msec;

                // restore r_znear
                cvarSystem.SetCVarFloat("r_znear", 3.0f);

                // show all the player models
                for (i = 0; i < this.numClients; i++) {
                    if (this.entities[ i] != null) {
                        final idPlayer client2 = (idPlayer) this.entities[i];
                        client2.ExitCinematic();
                    }
                }
            }
        }

        public idCamera GetCamera() {
            return this.camera;
        }

        public boolean SkipCinematic() {
            if (this.camera != null) {
                if (this.camera.spawnArgs.GetBool("disconnect")) {
                    this.camera.spawnArgs.SetBool("disconnect", false);
                    cvarSystem.SetCVarFloat("r_znear", 3.0f);
                    cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "disconnect\n");
                    this.skipCinematic = false;
                    return false;
                }

                if (this.camera.spawnArgs.GetBool("instantSkip")) {
                    this.camera.Stop();
                    return false;
                }
            }

            snd_system.soundSystem.SetMute(true);
            if (!this.skipCinematic) {
                this.skipCinematic = true;
                this.cinematicMaxSkipTime = (int) (gameLocal.time + SEC2MS(g_cinematicMaxSkipTime.GetFloat()));
            }

            return true;
        }

        /*
         ====================
         idGameLocal::CalcFov

         Calculates the horizontal and vertical field of view based on a horizontal field of view and custom aspect ratio
         ====================
         */
        public void CalcFov(float base_fov, float[] fov_x, float[] fov_y) {
            float x;
            float y;
            float ratio_x;
            float ratio_y;

//            if (!sys.FPU_StackIsEmpty()) {
//                Printf(sys.FPU_GetState());
//                Error("idGameLocal::CalcFov: FPU stack not empty");
//            }

            // first, calculate the vertical fov based on a 640x480 view
            x = (float) (640.0f / tan((base_fov / 360.0f) * idMath.PI));
            y = (float) atan2(480.0f, x);
            fov_y[0] = (y * 360.0f) / idMath.PI;

            // FIXME: somehow, this is happening occasionally
            assert (fov_y[0] > 0);
            if (fov_y[0] <= 0) {
                Printf(sys.FPU_GetState());
                Error("idGameLocal::CalcFov: bad result");
            }

            switch (r_aspectRatio.GetInteger()) {
                default:
                case 0:
                    // 4:3
                    fov_x[0] = base_fov;
                    return;
//                    break;

                case 1:
                    // 16:9
                    ratio_x = 16.0f;
                    ratio_y = 9.0f;
                    break;

                case 2:
                    // 16:10
                    ratio_x = 16.0f;
                    ratio_y = 10.0f;
                    break;
            }

            y = (float) (ratio_y / tan((fov_y[0] / 360.0f) * idMath.PI));
            fov_x[0] = (float) ((atan2(ratio_x, y) * 360.0f) / idMath.PI);

            if (fov_x[0] < base_fov) {
                fov_x[0] = base_fov;
                x = (float) (ratio_x / tan((fov_x[0] / 360.0f) * idMath.PI));
                fov_y[0] = (float) ((atan2(ratio_y, x) * 360.0f) / idMath.PI);
            }

            // FIXME: somehow, this is happening occasionally
            assert ((fov_x[0] > 0) && (fov_y[0] > 0));
            if ((fov_y[0] <= 0) || (fov_x[0] <= 0)) {
                Printf(sys.FPU_GetState());
                Error("idGameLocal::CalcFov: bad result");
            }
        }

        public void AddEntityToHash(final String name, idEntity ent) {
            if (FindEntity(name) != null) {
                Error("Multiple entities named '%s'", name);
            }
            this.entityHash.Add(this.entityHash.GenerateKey(name, true), ent.entityNumber);
        }

        public boolean RemoveEntityFromHash(final String name, idEntity ent) {
            int hash, i;

            hash = this.entityHash.GenerateKey(name, true);
            for (i = this.entityHash.First(hash); i != -1; i = this.entityHash.Next(i)) {
                if ((this.entities[i] != null) && this.entities[i].equals(ent) && (this.entities[i].name.Icmp(name) == 0)) {
                    this.entityHash.Remove(hash, i);
                    return true;
                }
            }
            return false;
        }

        public int GetTargets(final idDict args, idList<idEntityPtr<idEntity>> list, final String ref) {
            int i, num, refLength;
            idKeyValue arg;
            idEntity ent;

            list.Clear();

            refLength = ref.length();
            num = args.GetNumKeyVals();
            for (i = 0; i < num; i++) {

                arg = args.GetKeyVal(i);
                if (arg.GetKey().Icmpn(ref, refLength) == 0) {

                    ent = FindEntity(arg.GetValue());
                    if (ent != null) {
                        final idEntityPtr<idEntity> entityPtr = list.Alloc();
                        entityPtr.oSet(ent);
                    }
                }
            }

            return list.Num();
        }

        /*
         =============
         idGameLocal::GetTraceEntity

         returns the master entity of a trace.  for example, if the trace entity is the player's head, it will return the player.
         =============
         */
        public idEntity GetTraceEntity(final trace_s trace) {
            idEntity master;

            if (null == this.entities[ trace.c.entityNum]) {
                return null;
            }
            master = this.entities[ trace.c.entityNum].GetBindMaster();
            if (master != null) {
                return master;
            }
            return this.entities[ trace.c.entityNum];
        }

        /*
         =============
         idGameLocal::ArgCompletion_EntityName

         Argument completion for entity names
         =============
         */
        public static class ArgCompletion_EntityName extends argCompletion_t {

            private static final argCompletion_t instance = new ArgCompletion_EntityName();

            private ArgCompletion_EntityName() {
            }

            public static argCompletion_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args, void_callback<String> callback) {
                int i;

                for (i = 0; i < gameLocal.num_entities; i++) {
                    if (gameLocal.entities[ i] != null) {
                        callback.run(va("%s %s", args.Argv(0), gameLocal.entities[ i].name));
                    }
                }
            }
        }

        /*
         =============
         idGameLocal::FindTraceEntity

         Searches all active entities for the closest ( to start ) match that intersects
         the line start,end
         =============
         */
        public idEntity FindTraceEntity(idVec3 start, idVec3 end, final Class<?>/*idTypeInfo*/ c, final idEntity skip) {
            idEntity ent;
            idEntity bestEnt;
            final float[] scale = {0};
            float bestScale;
            idBounds b;

            bestEnt = null;
            bestScale = 1.0f;
            for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (ent.IsType(c) && (ent != skip)) {
                    b = ent.GetPhysics().GetAbsBounds().Expand(16);
                    if (b.RayIntersection(start, end.oMinus(start), scale)) {
                        if ((scale[0] >= 0.0f) && (scale[0] < bestScale)) {
                            bestEnt = ent;
                            bestScale = scale[0];
                        }
                    }
                }
            }

            return bestEnt;
        }

        /*
         =============
         idGameLocal::FindEntity

         Returns the entity whose name matches the specified string.
         =============
         */
        public idEntity FindEntity(final String name) {
            int hash, i;

            hash = this.entityHash.GenerateKey(name, true);
            for (i = this.entityHash.First(hash); i != -1; i = this.entityHash.Next(i)) {
                if ((this.entities[i] != null) && (this.entities[i].name.Icmp(name) == 0)) {
                    return this.entities[i];
                }
            }

            return null;
        }

        public idEntity FindEntity(final idStr name) {
            return FindEntity(name.getData());
        }

        /*
         =============
         idGameLocal::FindEntityUsingDef

         Searches all active entities for the next one using the specified entityDef.

         Searches beginning at the entity after from, or the beginning if NULL
         NULL will be returned if the end of the list is reached.
         =============
         */
        public idEntity FindEntityUsingDef(idEntity from, final String match) {
            idEntity ent;

            if (null == from) {
                ent = this.spawnedEntities.Next();
            } else {
                ent = from.spawnNode.Next();
            }

            for (; ent != null; ent = ent.spawnNode.Next()) {
                assert (ent != null);
                if (idStr.Icmp(ent.GetEntityDefName(), match) == 0) {
                    return ent;
                }
            }

            return null;
        }

        public int EntitiesWithinRadius(final idVec3 org, float radius, idEntity[] entityList, int maxCount) {
            idEntity ent;
            final idBounds bo = new idBounds(org);
            int entCount = 0;

            bo.ExpandSelf(radius);
            for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (ent.GetPhysics().GetAbsBounds().IntersectsBounds(bo)) {
                    entityList[entCount++] = ent;
                }
            }

            return entCount;
        }

        /*
         =================
         idGameLocal::KillBox

         Kills all entities that would touch the proposed new positioning of ent. The ent itself will not being killed.
         Checks if player entities are in the teleporter, and marks them to die at teleport exit instead of immediately.
         If catch_teleport, this only marks teleport players for death on exit
         =================
         */
        public void KillBox(idEntity ent, boolean catch_teleport /*= false*/) {
            int i;
            int num;
            idEntity hit;
            idClipModel cm;
            final idClipModel[] clipModels = new idClipModel[MAX_GENTITIES];
            idPhysics phys;

            phys = ent.GetPhysics();
            if (0 == phys.GetNumClipModels()) {
                return;
            }

            num = this.clip.ClipModelsTouchingBounds(phys.GetAbsBounds(), phys.GetClipMask(), clipModels, MAX_GENTITIES);
            for (i = 0; i < num; i++) {
                cm = clipModels[ i];

                // don't check render entities
                if (cm.IsRenderModel()) {
                    continue;
                }

                hit = cm.GetEntity();
                if ((hit == ent) || !hit.fl.takedamage) {
                    continue;
                }

                if (0 == phys.ClipContents(cm)) {
                    continue;
                }

                // nail it
                if (hit.IsType(idPlayer.class) && ((idPlayer) hit).IsInTeleport()) {
                    ((idPlayer) hit).TeleportDeath(ent.entityNumber);
                } else if (!catch_teleport) {
                    hit.Damage(ent, ent, getVec3_origin(), "damage_telefrag", 1.0f, INVALID_JOINT);
                }

                if (!gameLocal.isMultiplayer) {
                    // let the mapper know about it
                    Warning("'%s' telefragged '%s'", ent.name, hit.name);
                }
            }
        }

        public void KillBox(idEntity ent) {
            KillBox(ent, false);
        }

        public void RadiusDamage(final idVec3 origin, idEntity inflictor, idEntity attacker, idEntity ignoreDamage, idEntity ignorePush, final String damageDefName, float dmgPower /*= 1.0f*/) {
            float dist, damageScale;
            final float[] attackerDamageScale = {0}, attackerPushScale = {0};
            idEntity ent;
            final idEntity[] entityList = new idEntity[MAX_GENTITIES];
            int numListedEntities;
            idBounds bounds;
            final idVec3 v = new idVec3(), damagePoint = new idVec3();
			idVec3 dir;
            int i, e;
            final int[] damage = {0}, radius = {0}, push = {0};

            final idDict damageDef = FindEntityDefDict(damageDefName, false);
            if (null == damageDef) {
                Warning("Unknown damageDef '%s'", damageDefName);
                return;
            }

            damageDef.GetInt("damage", "20", damage);
            damageDef.GetInt("radius", "50", radius);
            damageDef.GetInt("push", va("%d", damage[0] * 100), push);
            damageDef.GetFloat("attackerDamageScale", "0.5", attackerDamageScale);
            damageDef.GetFloat("attackerPushScale", "0", attackerPushScale);

            if (radius[0] < 1) {
                radius[0] = 1;
            }

            bounds = new idBounds(origin).Expand(radius[0]);

            // get all entities touching the bounds
            numListedEntities = this.clip.EntitiesTouchingBounds(bounds, -1, entityList, MAX_GENTITIES);

            if ((inflictor != null) && inflictor.IsType(idAFAttachment.class)) {
                inflictor = ((idAFAttachment) inflictor).GetBody();
            }
            if ((attacker != null) && attacker.IsType(idAFAttachment.class)) {
                attacker = ((idAFAttachment) attacker).GetBody();
            }
            if ((ignoreDamage != null) && ignoreDamage.IsType(idAFAttachment.class)) {
                ignoreDamage = ((idAFAttachment) ignoreDamage).GetBody();
            }

            // apply damage to the entities
            for (e = 0; e < numListedEntities; e++) {
                ent = entityList[ e];
                assert (ent != null);

                if (!ent.fl.takedamage) {
                    continue;
                }

                if ((ent == inflictor) || (ent.IsType(idAFAttachment.class) && (((idAFAttachment) ent).GetBody() == inflictor))) {
                    continue;
                }

                if ((ent == ignoreDamage) || (ent.IsType(idAFAttachment.class) && (((idAFAttachment) ent).GetBody() == ignoreDamage))) {
                    continue;
                }

                // don't damage a dead player
                if (this.isMultiplayer && (ent.entityNumber < MAX_CLIENTS) && ent.IsType(idPlayer.class) && (((idPlayer) ent).health < 0)) {
                    continue;
                }

                // find the distance from the edge of the bounding box
                for (i = 0; i < 3; i++) {
                    if (origin.oGet(i) < ent.GetPhysics().GetAbsBounds().oGet(0, i)) {
                        v.oSet(i, ent.GetPhysics().GetAbsBounds().oGet(0, i) - origin.oGet(i));
                    } else if (origin.oGet(i) > ent.GetPhysics().GetAbsBounds().oGet(1, i)) {
                        v.oSet(i, origin.oGet(i) - ent.GetPhysics().GetAbsBounds().oGet(1, i));
                    } else {
                        v.oSet(i, 0);
                    }
                }

                dist = v.Length();
                if (dist >= radius[0]) {
                    continue;
                }

                if (ent.CanDamage(origin, damagePoint)) {
                    // push the center of mass higher than the origin so players
                    // get knocked into the air more
                    dir = ent.GetPhysics().GetOrigin().oMinus(origin);
                    dir.oPluSet(2, 24);

                    // get the damage scale
                    damageScale = dmgPower * (1.0f - (dist / radius[0]));
                    if ((ent == attacker) || (ent.IsType(idAFAttachment.class) && (((idAFAttachment) ent).GetBody() == attacker))) {
                        damageScale *= attackerDamageScale[0];
                    }

                    ent.Damage(inflictor, attacker, dir, damageDefName, damageScale, INVALID_JOINT);
                }
            }

            // push physics objects
            if (push[0] != 0) {
                RadiusPush(origin, radius[0], push[0] * dmgPower, attacker, ignorePush, attackerPushScale[0], false);
            }
        }

        public void RadiusDamage(final idVec3 origin, idEntity inflictor, idEntity attacker, idEntity ignoreDamage, idEntity ignorePush, final String damageDefName) {
            RadiusDamage(origin, inflictor, attacker, ignoreDamage, ignorePush, damageDefName, 0.0f);
        }

        public void RadiusPush(final idVec3 origin, final float radius, final float push, final idEntity inflictor, final idEntity ignore, float inflictorScale, final boolean quake) {
            int i, numListedClipModels;
            idClipModel clipModel;
            final idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            final idVec3 dir = new idVec3();
            idBounds bounds;
            final modelTrace_s result = new modelTrace_s();
            idEntity ent;
            float scale;

            dir.Set(0.0f, 0.0f, 1.0f);

            bounds = new idBounds(origin).Expand(radius);

            // get all clip models touching the bounds
            numListedClipModels = this.clip.ClipModelsTouchingBounds(bounds, -1, clipModelList, MAX_GENTITIES);

            if ((inflictor != null) && inflictor.IsType(idAFAttachment.class)) {
                inflictor.oSet(((idAFAttachment) inflictor).GetBody());
            }
            if ((ignore != null) && ignore.IsType(idAFAttachment.class)) {
                ignore.oSet(((idAFAttachment) ignore).GetBody());
            }

            // apply impact to all the clip models through their associated physics objects
            for (i = 0; i < numListedClipModels; i++) {

                clipModel = clipModelList[i];

                // never push render models
                if (clipModel.IsRenderModel()) {
                    continue;
                }

                ent = clipModel.GetEntity();

                // never push projectiles
                if (ent.IsType(idProjectile.class)) {
                    continue;
                }

                // players use "knockback" in idPlayer::Damage
                if (ent.IsType(idPlayer.class) && !quake) {
                    continue;
                }

                // don't push the ignore entity
                if ((ent == ignore) || (ent.IsType(idAFAttachment.class) && (((idAFAttachment) ent).GetBody() == ignore))) {
                    continue;
                }

                if (gameRenderWorld.FastWorldTrace(result, origin, clipModel.GetOrigin())) {
                    continue;
                }

                // scale the push for the inflictor
                if ((ent == inflictor) || (ent.IsType(idAFAttachment.class) && (((idAFAttachment) ent).GetBody() == inflictor))) {
                    scale = inflictorScale;
                } else {
                    scale = 1.0f;
                }

                if (quake) {
                    clipModel.GetEntity().ApplyImpulse(this.world, clipModel.GetId(), clipModel.GetOrigin(), dir.oMultiply(scale * push));
                } else {
                    RadiusPushClipModel(origin, scale * push, clipModel);
                }
            }
        }

        public void RadiusPushClipModel(final idVec3 origin, final float push, final idClipModel clipModel) {
            int i, j;
            float dot, dist, area;
            idTraceModel trm;
            traceModelPoly_t poly;
            final idFixedWinding w = new idFixedWinding();
            idVec3 v, localOrigin, center = new idVec3(), impulse;

            trm = clipModel.GetTraceModel();
            if (null == trm) {//|| 1 ) {//TODO:wtf?
                impulse = clipModel.GetAbsBounds().GetCenter().oMinus(origin);
                impulse.Normalize();
                impulse.z += 1.0f;
                clipModel.GetEntity().ApplyImpulse(this.world, clipModel.GetId(), clipModel.GetOrigin(), impulse.oMultiply(push));
                return;
            }

            localOrigin = (origin.oMinus(clipModel.GetOrigin())).oMultiply(clipModel.GetAxis().Transpose());
            for (i = 0; i < trm.numPolys; i++) {
                poly = trm.polys[i];

                center.Zero();
                for (j = 0; j < poly.numEdges; j++) {
                    v = trm.verts[ trm.edges[ abs(poly.edges[j])].v[ INTSIGNBITSET(poly.edges[j])]];
                    center.oPluSet(v);
                    v.oMinSet(localOrigin);
                    v.NormalizeFast();	// project point on a unit sphere
                    w.AddPoint(v);
                }
                center.oDivSet(poly.numEdges);
                v = center.oMinus(localOrigin);
                dist = v.NormalizeFast();
                dot = v.oMultiply(poly.normal);
                if (dot > 0.0f) {
                    continue;
                }
                area = w.GetArea();
                // impulse in polygon normal direction
                impulse = poly.normal.oMultiply(clipModel.GetAxis());
                // always push up for nicer effect
                impulse.z -= 1.0f;
                // scale impulse based on visible surface area and polygon angle
                impulse.oMulSet(push * (dot * area * (1.0f / (4.0f * idMath.PI))));
                // scale away distance for nicer effect
                impulse.oMulSet(dist * 2.0f);
                // impulse is applied to the center of the polygon
                center = clipModel.GetOrigin().oPlus(center.oMultiply(clipModel.GetAxis()));

                clipModel.GetEntity().ApplyImpulse(this.world, clipModel.GetId(), center, impulse);
            }
        }
        private static final idVec3[] decalWinding = {
            new idVec3(1.0f, 1.0f, 0.0f),
            new idVec3(-1.0f, 1.0f, 0.0f),
            new idVec3(-1.0f, -1.0f, 0.0f),
            new idVec3(1.0f, -1.0f, 0.0f)
        };

        public void ProjectDecal(final idVec3 origin, final idVec3 dir, float depth, boolean parallel, float size, final String material, float angle /*= 0*/) {
            final float[] s = {0}, c = {0};
            final idMat3 axis = new idMat3(), axistemp = new idMat3();
            final idFixedWinding winding = new idFixedWinding();
            idVec3 windingOrigin, projectionOrigin;

            if (!g_decals.GetBool()) {
                return;
            }

            // randomly rotate the decal winding
            idMath.SinCos16((angle != 0) ? angle : this.random.RandomFloat() * idMath.TWO_PI, s, c);

            // winding orientation
            axis.oSet(2, dir);
            axis.oGet(2).Normalize();
            axis.oGet(2).NormalVectors(axistemp.oGet(0), axistemp.oGet(1));
            axis.oSet(0, axistemp.oGet(0).oMultiply(c[0]).oPlus(axistemp.oGet(1).oMultiply(-s[0])));
            axis.oSet(1, axistemp.oGet(0).oMultiply(-s[0]).oPlus(axistemp.oGet(1).oMultiply(-c[0])));

            windingOrigin = origin.oPlus(axis.oGet(2).oMultiply(depth));
            if (parallel) {
                projectionOrigin = origin.oMinus(axis.oGet(2).oMultiply(depth));
            } else {
                projectionOrigin = origin;
            }

            size *= 0.5f;

            winding.Clear();
            winding.oPluSet(new idVec5(windingOrigin.oPlus((axis.oMultiply(decalWinding[0])).oMultiply(size)), new idVec2(1, 1)));
            winding.oPluSet(new idVec5(windingOrigin.oPlus((axis.oMultiply(decalWinding[1])).oMultiply(size)), new idVec2(0, 1)));
            winding.oPluSet(new idVec5(windingOrigin.oPlus((axis.oMultiply(decalWinding[2])).oMultiply(size)), new idVec2(0, 0)));
            winding.oPluSet(new idVec5(windingOrigin.oPlus((axis.oMultiply(decalWinding[3])).oMultiply(size)), new idVec2(1, 0)));
            gameRenderWorld.ProjectDecalOntoWorld(winding, projectionOrigin, parallel, depth * 0.5f, declManager.FindMaterial(material), this.time);
        }

        public void ProjectDecal(final idVec3 origin, final idVec3 dir, float depth, boolean parallel, float size, final String material) {
            ProjectDecal(origin, dir, depth, parallel, size, material, 0);
        }

        public void BloodSplat(final idVec3 origin, final idVec3 dir, float size, final String material) {
            final float halfSize = size * 0.5f;
            final idVec3[] verts = {new idVec3(0.0f, +halfSize, +halfSize),
                new idVec3(0.0f, +halfSize, -halfSize),
                new idVec3(0.0f, -halfSize, -halfSize),
                new idVec3(0.0f, -halfSize, +halfSize)};
            final idTraceModel trm = new idTraceModel();
            final idClipModel mdl = new idClipModel();
            final trace_s[] results = {null};

            // FIXME: get from damage def
            if (!g_bloodEffects.GetBool()) {
                return;
            }

            size = halfSize + (this.random.RandomFloat() * halfSize);
            trm.SetupPolygon(verts, 4);
            mdl.LoadModel(trm);
            this.clip.Translation(results, origin, origin.oPlus(dir.oMultiply(64.0f)), mdl, getMat3_identity(), CONTENTS_SOLID, null);
            ProjectDecal(results[0].endpos, dir, 2.0f * size, true, size, material);
        }

        public void CallFrameCommand(idEntity ent, final function_t frameCommand) {
            this.frameCommandThread.CallFunction(ent, frameCommand, true);
            this.frameCommandThread.Execute();
        }

        public void CallObjectFrameCommand(idEntity ent, final String frameCommand) {
            function_t func;

            func = ent.scriptObject.GetFunction(frameCommand);
            if (null == func) {
                if (!ent.IsType(idTestModel.class)) {
                    Error("Unknown function '%s' called for frame command on entity '%s'", frameCommand, ent.name);
                }
            } else {
                this.frameCommandThread.CallFunction(ent, func, true);
                this.frameCommandThread.Execute();
            }
        }

        public idVec3 GetGravity() {
            return this.gravity;
        }

        // added the following to assist licensees with merge issues
        public int GetFrameNum() {
            return this.framenum;
        }

        public int GetTime() {
            return this.time;
        }

        public int GetMSec() {
            return msec;
        }

        public int GetNextClientNum(int _current) {
            int i, current;

            current = 0;
            for (i = 0; i < this.numClients; i++) {
                current = (_current + i + 1) % this.numClients;
                if ((this.entities[ current] != null) && this.entities[ current].IsType(idPlayer.class)) {
                    return current;
                }
            }

            return current;
        }

        public idPlayer GetClientByNum(int current) {
            if ((current < 0) || (current >= this.numClients)) {
                current = 0;
            }
            if (this.entities[current] != null) {
                return ((idPlayer) this.entities[current]);
            }
            return null;
        }

        public idPlayer GetClientByName(final String name) {
            int i;
            idEntity ent;
            for (i = 0; i < this.numClients; i++) {
                ent = this.entities[ i];
                if ((ent != null) && ent.IsType(idPlayer.class)) {
                    if (idStr.IcmpNoColor(name, this.userInfo[ i].GetString("ui_name")) == 0) {
                        return (idPlayer) ent;
                    }
                }
            }
            return null;
        }

        public idPlayer GetClientByCmdArgs(final idCmdArgs args) {
            idPlayer player;
            final idStr client = new idStr(args.Argv(1));
            if (0 == client.Length()) {
                return null;
            }
            // we don't allow numeric ui_name so this can't go wrong
            if (client.IsNumeric()) {
                player = GetClientByNum(Integer.parseInt(client.getData()));
            } else {
                player = GetClientByName(client.getData());
            }
            if (null == player) {
                common.Printf("Player '%s' not found\n", client.getData());
            }
            return player;
        }

        /*
         ================
         idGameLocal::GetLocalPlayer

         Nothing in the game tic should EVER make a decision based on what the
         local client number is, it shouldn't even be aware that there is a
         draw phase even happening.  This just returns client 0, which will
         be correct for single player.
         ================
         */
        public idPlayer GetLocalPlayer() {
            if (this.localClientNum < 0) {
                return null;
            }

            if ((null == this.entities[ this.localClientNum]) || !this.entities[ this.localClientNum].IsType(idPlayer.class)) {
                // not fully in game yet
                return null;
            }
            return (idPlayer) this.entities[this.localClientNum];
        }

        /*
         ======================
         idGameLocal::SpreadLocations

         Now that everything has been spawned, associate areas with location entities
         ======================
         */private static int DBG_SpreadLocations = 0;
        public void SpreadLocations() {DBG_SpreadLocations++;
            idEntity ent;

            // allocate the area table
            final int numAreas = gameRenderWorld.NumAreas();
            this.locationEntities = new idLocationEntity[numAreas];
//	memset( locationEntities, 0, numAreas * sizeof( *locationEntities ) );

            // for each location entity, make pointers from every area it touches
            for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (!ent.IsType(idLocationEntity.class)) {
                    continue;
                }
                final idVec3 point = ent.spawnArgs.GetVector("origin");
                final int areaNum = gameRenderWorld.PointInArea(point);
                if (areaNum < 0) {
                    Printf("SpreadLocations: location '%s' is not in a valid area\n", ent.spawnArgs.GetString("name"));
                    continue;
                }
                if (areaNum >= numAreas) {
                    Error("idGameLocal::SpreadLocations: areaNum >= gameRenderWorld.NumAreas()");
                }
                if (this.locationEntities[areaNum] != null) {
                    Warning("location entity '%s' overlaps '%s'", ent.spawnArgs.GetString("name"),
                            this.locationEntities[areaNum].spawnArgs.GetString("name"));
                    continue;
                }
                this.locationEntities[areaNum] = (idLocationEntity) ent;

                // spread to all other connected areas
                for (int i = 0; i < numAreas; i++) {
                    if (i == areaNum) {
                        continue;
                    }
                    if (gameRenderWorld.AreasAreConnected(areaNum, i, PS_BLOCK_LOCATION)) {
                        this.locationEntities[i] = (idLocationEntity) ent;
                    }
                }
            }
        }

        /*
         ===================
         idGameLocal::LocationForPoint

         The player checks the location each frame to update the HUD text display
         May return NULL
         ===================
         */
        public idLocationEntity LocationForPoint(final idVec3 point) {
            if (null == this.locationEntities) {
                // before SpreadLocations() has been called
                return null;
            }

            final int areaNum = gameRenderWorld.PointInArea(point);
            if (areaNum < 0) {
                return null;
            }
            if (areaNum >= gameRenderWorld.NumAreas()) {
                Error("idGameLocal::LocationForPoint: areaNum >= gameRenderWorld.NumAreas()");
            }

            return this.locationEntities[ areaNum];
        }

        /*
         ===========
         idGameLocal::SelectInitialSpawnPoint
         spectators are spawned randomly anywhere
         in-game clients are spawned based on distance to active players (randomized on the first half)
         upon map restart, initial spawns are used (randomized ordered list of spawns flagged "initial")
         if there are more players than initial spots, overflow to regular spawning
         ============
         */
        public idEntity SelectInitialSpawnPoint(idPlayer player) {
            int i, j, which;
            spawnSpot_t spot = new spawnSpot_t();
            idVec3 pos;
            float dist;
            boolean alone;

            if (!this.isMultiplayer || NOT(this.spawnSpots.Num())) {
                spot.ent = FindEntityUsingDef(null, "info_player_start");
                if (null == spot.ent) {
                    Error("No info_player_start on map.\n");
                }
                return spot.ent;
            }
            if (player.spectating) {
                // plain random spot, don't bother
                return this.spawnSpots.oGet(this.random.RandomInt(this.spawnSpots.Num())).ent;
            } else if (player.useInitialSpawns && (this.currentInitialSpot < this.initialSpots.Num())) {
                return this.initialSpots.oGet(this.currentInitialSpot++);
            } else {
                // check if we are alone in map
                alone = true;
                for (j = 0; j < MAX_CLIENTS; j++) {
                    if ((this.entities[j] != null) && !this.entities[ j].equals(player)) {
                        alone = false;
                        break;
                    }
                }
                if (alone) {
                    // don't do distance-based
                    return this.spawnSpots.oGet(this.random.RandomInt(this.spawnSpots.Num())).ent;
                }

                // find the distance to the closest active player for each spawn spot
                for (i = 0; i < this.spawnSpots.Num(); i++) {
                    pos = this.spawnSpots.oGet(i).ent.GetPhysics().GetOrigin();
                    this.spawnSpots.oGet(i).dist = 0x7fffffff;
                    for (j = 0; j < MAX_CLIENTS; j++) {
                        if ((null == this.entities[ j]) || !this.entities[ j].IsType(idPlayer.class)
                                || this.entities[ j].equals(player)
                                || ((idPlayer) this.entities[ j]).spectating) {
                            continue;
                        }

                        dist = (pos.oMinus(this.entities[ j].GetPhysics().GetOrigin())).LengthSqr();
                        if (dist < this.spawnSpots.oGet(i).dist) {
                            this.spawnSpots.oGet(i).dist = (int) dist;
                        }
                    }
                }

                // sort the list
//                qsort( /*( void * )*/spawnSpots.Ptr(), spawnSpots.Num(), sizeof(spawnSpot_t), /*( int (*)(const void *, const void *) )*/ sortSpawnPoints);
                Arrays.sort(this.spawnSpots.Ptr(), 0, this.spawnSpots.Num(), new sortSpawnPoints());

                // choose a random one in the top half
                which = this.random.RandomInt(this.spawnSpots.Num() / 2);
                spot = this.spawnSpots.oGet(which);
            }
            return spot.ent;
        }

        public void SetPortalState(int/*qhandle_t*/ portal, int blockingBits) {
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            if (!gameLocal.isClient) {
                outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
                outMsg.WriteByte(GAME_RELIABLE_MESSAGE_PORTAL);
                outMsg.WriteLong(portal);
                outMsg.WriteBits(blockingBits, NUM_RENDER_PORTAL_BITS);
                networkSystem.ServerSendReliableMessage(-1, outMsg);
            }
            gameRenderWorld.SetPortalState(portal, blockingBits);
        }

        public void SaveEntityNetworkEvent(final idEntity ent, int eventId, final idBitMsg msg) {
            entityNetEvent_s event;

            event = this.savedEventQueue.Alloc();
            event.spawnId = GetSpawnId(ent);
            event.event = eventId;
            event.time = this.time;
            if (msg != null) {
                event.paramsSize = msg.GetSize();
//		memcpy( event.paramsBuf, msg.GetData(), msg.GetSize() );
                Nio.arraycopy(msg.GetData().array(), 0, event.paramsBuf.array(), 0, msg.GetSize());
            } else {
                event.paramsSize = 0;
            }

            this.savedEventQueue.Enqueue(event, OUTOFORDER_IGNORE);
        }

        public void ServerSendChatMessage(int to, final String name, final String text) {
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_CHAT);
            outMsg.WriteString(name);
            outMsg.WriteString(text, -1, false);
            networkSystem.ServerSendReliableMessage(to, outMsg);

            if ((to == -1) || (to == this.localClientNum)) {
                this.mpGame.AddChatLine("%s^0: %s\n", name, text);
            }
        }

        public int ServerRemapDecl(int clientNum, declType_t type, int index) {

            // only implicit materials and sound shaders decls are used
            if ((type != DECL_MATERIAL) && (type != DECL_SOUND)) {
                return index;
            }

            if (clientNum == -1) {
                for (int i = 0; i < MAX_CLIENTS; i++) {
                    ServerSendDeclRemapToClient(i, type, index);
                }
            } else {
                ServerSendDeclRemapToClient(clientNum, type, index);
            }
            return index;
        }

        public int ClientRemapDecl(declType_t type, int index) {

            // only implicit materials and sound shaders decls are used
            if ((type != DECL_MATERIAL) && (type != DECL_SOUND)) {
                return index;
            }

            // negative indexes are sometimes used for NULL decls
            if (index < 0) {
                return index;
            }

            // make sure the index is valid
            if (this.clientDeclRemap[this.localClientNum][ type.ordinal()].Num() == 0) {
                idGameLocal.Error("client received decl index %d before %s decl remap was initialized", index, declManager.GetDeclNameFromType(type));
                return -1;
            }
            if (index >= this.clientDeclRemap[this.localClientNum][ type.ordinal()].Num()) {
                idGameLocal.Error("client received unmapped %s decl index %d from server", declManager.GetDeclNameFromType(type), index);
                return -1;
            }
            if (this.clientDeclRemap[this.localClientNum][ type.ordinal()].oGet(index) == -1) {
                idGameLocal.Error("client received unmapped %s decl index %d from server", declManager.GetDeclNameFromType(type), index);
                return -1;
            }
            return this.clientDeclRemap[this.localClientNum][type.ordinal()].oGet(index);
        }

        public void SetGlobalMaterial(final idMaterial mat) {
            this.globalMaterial = mat;
        }

        public idMaterial GetGlobalMaterial() {
            return this.globalMaterial;
        }

        public void SetGibTime(int _time) {
            this.nextGibTime = _time;
        }

        public int GetGibTime() {
            return this.nextGibTime;
        }

        public boolean NeedRestart() {

            idDict newInfo;
            idKeyValue keyval, keyval2;

            newInfo = cvarSystem.MoveCVarsToDict(CVAR_SERVERINFO);

            for (int i = 0; i < newInfo.GetNumKeyVals(); i++) {
                keyval = newInfo.GetKeyVal(i);
                keyval2 = this.serverInfo.FindKey(keyval.GetKey());
                if (null == keyval2) {
                    return true;
                }
                // a select set of si_ changes will cause a full restart of the server
                if ((keyval.GetValue().Cmp(keyval2.GetValue().getData()) != 0) && ((0 == keyval.GetKey().Cmp("si_pure")) || (0 == keyval.GetKey().Cmp("si_map")))) {
                    return true;
                }
            }
            return false;
        }

        private void Clear() {
            int i;

            this.serverInfo.Clear();
            this.numClients = 0;
            for (i = 0; i < MAX_CLIENTS; i++) {
                this.userInfo[i].Clear();
                this.persistentPlayerInfo[i].Clear();
            }
//	memset( usercmds, 0, sizeof( usercmds ) );
            for (int u = 0; u < this.usercmds.length; u++) {
                this.usercmds[u] = new usercmd_t();
            }
//	memset( entities, 0, sizeof( entities ) );
            for (int e = 0; e < this.entities.length; e++) {
                this.entities[e] = new idEntity();
            }
            this.spawnIds = new int[this.spawnIds.length];
            Arrays.fill(this.spawnIds, -1);//	memset( spawnIds, -1, sizeof( spawnIds ) );
            this.firstFreeIndex = 0;
            this.num_entities = 0;
            this.spawnedEntities.Clear();
            this.activeEntities.Clear();
            this.numEntitiesToDeactivate = 0;
            this.sortPushers = false;
            this.sortTeamMasters = false;
            this.persistentLevelInfo.Clear();
            this.globalShaderParms = new float[this.globalShaderParms.length];//memset( globalShaderParms, 0, sizeof( globalShaderParms ) );
            this.random.SetSeed(0);
            this.world = null;
            this.frameCommandThread = null;
            this.testmodel = null;
            this.testFx = null;
            this.clip.Shutdown();
            this.pvs.Shutdown();
            this.sessionCommand.Clear();
            this.locationEntities = null;
            this.smokeParticles = null;
            this.editEntities = null;
            this.entityHash.Clear(1024, MAX_GENTITIES);
            this.inCinematic = false;
            this.cinematicSkipTime = 0;
            this.cinematicStopTime = 0;
            this.cinematicMaxSkipTime = 0;
            this.framenum = 0;
            this.previousTime = 0;
            this.time = 0;
            this.vacuumAreaNum = 0;
            this.mapFileName.Clear();
            this.mapFile = null;
            this.spawnCount = INITIAL_SPAWN_COUNT;
            this.mapSpawnCount = 0;
            this.camera = null;
            this.aasList.Clear();
            this.aasNames.Clear();
            this.lastAIAlertEntity = new idEntityPtr<>(null);
            this.lastAIAlertTime = 0;
            this.spawnArgs.Clear();
            this.gravity.Set(0, 0, -1);
            this.playerPVS.h = /*(unsigned int)*/ -1;
            this.playerConnectedAreas.h = /*(unsigned int)*/ -1;
            this.gamestate = GAMESTATE_UNINITIALIZED;
            this.skipCinematic = false;
            this.influenceActive = false;

            this.localClientNum = 0;
            this.isMultiplayer = false;
            this.isServer = false;
            this.isClient = false;
            this.realClientTime = 0;
            this.isNewFrame = true;
            this.clientSmoothing = 0.1f;
            this.entityDefBits = 0;

            this.nextGibTime = 0;
            this.globalMaterial = null;
            this.newInfo.Clear();
            this.lastGUIEnt = new idEntityPtr<>(null);
            this.lastGUI = 0;

//	memset( clientEntityStates, 0, sizeof( clientEntityStates ) );
            for (int a = 0; a < this.clientEntityStates.length; a++) {
                for (int b = 0; b < this.clientEntityStates[0].length; b++) {
                    this.clientEntityStates[a][b] = new entityState_s();
                }
            }
            this.clientPVS = new int[this.clientPVS.length][this.clientPVS[0].length];//memset( clientPVS, 0, sizeof( clientPVS ) );
//	memset( clientSnapshots, 0, sizeof( clientSnapshots ) );
            for (int c = 0; c < this.clientSnapshots.length; c++) {
                this.clientSnapshots[c] = new snapshot_s();
            }

            this.eventQueue.Init();
            this.savedEventQueue.Init();

            this.lagometer = new byte[this.lagometer.length][this.lagometer[0].length][this.lagometer[0][0].length];//memset(lagometer, 0, sizeof(lagometer));
        }

        // returns true if the entity shouldn't be spawned at all in this game type or difficulty level
        private boolean InhibitEntitySpawn(idDict spawnArgs) {

            final boolean[] result = {false};

            if (this.isMultiplayer) {
                spawnArgs.GetBool("not_multiplayer", "0", result);
            } else if (g_skill.GetInteger() == 0) {
                spawnArgs.GetBool("not_easy", "0", result);
            } else if (g_skill.GetInteger() == 1) {
                spawnArgs.GetBool("not_medium", "0", result);
            } else {
                spawnArgs.GetBool("not_hard", "0", result);
            }

            String name;
            if (!ID_DEMO_BUILD) {//#ifndef
                if (g_skill.GetInteger() == 3) {
                    name = spawnArgs.GetString("classname");
                    if ((idStr.Icmp(name, "item_medkit") == 0) || (idStr.Icmp(name, "item_medkit_small") == 0)) {
                        result[0] = true;
                    }
                }
            }

            if (gameLocal.isMultiplayer) {
                name = spawnArgs.GetString("classname");
                if ((idStr.Icmp(name, "weapon_bfg") == 0) || (idStr.Icmp(name, "weapon_soulcube") == 0)) {
                    result[0] = true;
                }
            }

            return result[0];
        }

        /*
         ==============
         idGameLocal::SpawnMapEntities

         Parses textual entity definitions out of an entstring and spawns gentities.
         ==============
         */
        // spawn entities from the map file
        private void SpawnMapEntities() {
            int i;
            int num;
            int inhibit;
            idMapEntity mapEnt;
            int numEntities;
            idDict args;

            Printf("Spawning entities\n");

            if (this.mapFile == null) {
                Printf("No mapfile present\n");
                return;
            }

            SetSkill(g_skill.GetInteger());

            numEntities = this.mapFile.GetNumEntities();
            if (numEntities == 0) {
                Error("...no entities");
            }

            // the worldspawn is a special that performs any global setup
            // needed by a level
            mapEnt = this.mapFile.GetEntity(0);
            args = mapEnt.epairs;
            args.SetInt("spawn_entnum", ENTITYNUM_WORLD);
            if (!SpawnEntityDef(args) || (null == this.entities[ ENTITYNUM_WORLD]) || !this.entities[ ENTITYNUM_WORLD].IsType(idWorldspawn.class)) {
                Error("Problem spawning world entity");
            }

            num = 1;
            inhibit = 0;

            for (i = 1; i < numEntities; i++) {
                mapEnt = this.mapFile.GetEntity(i);
                args = mapEnt.epairs;

                if (!InhibitEntitySpawn(args)) {
                    // precache any media specified in the map entity
                    CacheDictionaryMedia(args);

                    SpawnEntityDef(args);
                    num++;
                } else {
                    inhibit++;
                }
            }

            Printf("...%d entities spawned, %d inhibited\n\n", num, inhibit);
        }

        // commons used by init, shutdown, and restart
        private void MapPopulate() {

            if (this.isMultiplayer) {
                cvarSystem.SetCVarBool("r_skipSpecular", false);
            }
            // parse the key/value pairs and spawn entities
            SpawnMapEntities();

            // mark location entities in all connected areas
            SpreadLocations();

            // prepare the list of randomized initial spawn spots
            RandomizeInitialSpawns();

            // spawnCount - 1 is the number of entities spawned into the map, their indexes started at MAX_CLIENTS (included)
            // mapSpawnCount is used as the max index of map entities, it's the first index of non-map entities
            this.mapSpawnCount = (MAX_CLIENTS + this.spawnCount) - 1;

            // execute pending events before the very first game frame
            // this makes sure the map script main() function is called
            // before the physics are run so entities can bind correctly
            Printf("==== Processing events ====\n");
            idEvent.ServiceEvents();
        }

        private void MapClear(boolean clearClients) {
            int i;

            for (i = (clearClients ? 0 : MAX_CLIENTS); i < MAX_GENTITIES; i++) {
//		delete entities[ i ];
                // ~idEntity is in charge of setting the pointer to NULL
                // it will also clear pending events for this entity
                assert (null == this.entities[ i]);
                this.spawnIds[ i] = -1;
            }

            this.entityHash.Clear(1024, MAX_GENTITIES);

            if (!clearClients) {
                // add back the hashes of the clients
                for (i = 0; i < MAX_CLIENTS; i++) {
                    if (null == this.entities[ i]) {
                        continue;
                    }
                    this.entityHash.Add(this.entityHash.GenerateKey(this.entities[ i].name.getData(), true), i);
                }
            }

//	delete frameCommandThread;
            this.frameCommandThread = null;

            if (this.editEntities != null) {
//		delete editEntities;
                this.editEntities = null;
            }

//	delete[] locationEntities;
            this.locationEntities = null;
        }

        private pvsHandle_t GetClientPVS(idPlayer player, pvsType_t type) {
            if (player.GetPrivateCameraView() != null) {
                return this.pvs.SetupCurrentPVS(player.GetPrivateCameraView().GetPVSAreas(), player.GetPrivateCameraView().GetNumPVSAreas());
            } else if (this.camera != null) {
                return this.pvs.SetupCurrentPVS(this.camera.GetPVSAreas(), this.camera.GetNumPVSAreas());
            } else {
                return this.pvs.SetupCurrentPVS(player.GetPVSAreas(), player.GetNumPVSAreas());
            }
        }

        private void SetupPlayerPVS() {
            int i;
            idEntity ent;
            idPlayer player;
            pvsHandle_t otherPVS, newPVS;

            this.playerPVS.i = -1;
            for (i = 0; i < this.numClients; i++) {
                ent = this.entities[i];
                if ((null == ent) || !ent.IsType(idPlayer.class)) {
                    continue;
                }

                player = (idPlayer) ent;

                if (this.playerPVS.i == -1) {
                    this.playerPVS = GetClientPVS(player, PVS_NORMAL);
                } else {
                    otherPVS = GetClientPVS(player, PVS_NORMAL);
                    newPVS = this.pvs.MergeCurrentPVS(this.playerPVS, otherPVS);
                    this.pvs.FreeCurrentPVS(this.playerPVS);
                    this.pvs.FreeCurrentPVS(otherPVS);
                    this.playerPVS = newPVS;
                }

                if (this.playerConnectedAreas.i == -1) {
                    this.playerConnectedAreas = GetClientPVS(player, PVS_CONNECTED_AREAS);
                } else {
                    otherPVS = GetClientPVS(player, PVS_CONNECTED_AREAS);
                    newPVS = this.pvs.MergeCurrentPVS(this.playerConnectedAreas, otherPVS);
                    this.pvs.FreeCurrentPVS(this.playerConnectedAreas);
                    this.pvs.FreeCurrentPVS(otherPVS);
                    this.playerConnectedAreas = newPVS;
                }
            }
        }

        private void FreePlayerPVS() {
            if (this.playerPVS.i != -1) {
                this.pvs.FreeCurrentPVS(this.playerPVS);
                this.playerPVS.i = -1;
            }
            if (this.playerConnectedAreas.i != -1) {
                this.pvs.FreeCurrentPVS(this.playerConnectedAreas);
                this.playerConnectedAreas.i = -1;
            }
        }

        private void UpdateGravity() {
            idEntity ent;

            if (g_gravity.IsModified()) {
                if (g_gravity.GetFloat() == 0.0f) {
                    g_gravity.SetFloat(1.0f);
                }
                this.gravity.Set(0, 0, -g_gravity.GetFloat());

                // update all physics objects
                for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                    if (ent.IsType(idAFEntity_Generic.class)) {
                        final idPhysics phys = ent.GetPhysics();
                        if (phys != null) {
                            phys.SetGravity(this.gravity);
                        }
                    }
                }
                g_gravity.ClearModified();
            }
        }

        /*
         ================
         idGameLocal::SortActiveEntityList

         Sorts the active entity list such that pushing entities come first,
         actors come next and physics team slaves appear after their master.
         ================
         */
        private void SortActiveEntityList() {
            idEntity ent, next_ent, master, part;

            // if the active entity list needs to be reordered to place physics team masters at the front
            if (this.sortTeamMasters) {
                for (ent = this.activeEntities.Next(); ent != null; ent = next_ent) {
                    next_ent = ent.activeNode.Next();
                    master = ent.GetTeamMaster();
                    if ((master != null) && (master == ent)) {
                        ent.activeNode.Remove();
                        ent.activeNode.AddToFront(this.activeEntities);
                    }
                }
            }

            // if the active entity list needs to be reordered to place pushers at the front
            if (this.sortPushers) {

                for (ent = this.activeEntities.Next(); ent != null; ent = next_ent) {
                    next_ent = ent.activeNode.Next();
                    master = ent.GetTeamMaster();
                    if ((null == master) || (master == ent)) {
                        // check if there is an actor on the team
                        for (part = ent; part != null; part = part.GetNextTeamEntity()) {
                            if (part.GetPhysics().IsType(idPhysics_Actor.class)) {
                                break;
                            }
                        }
                        // if there is an actor on the team
                        if (part != null) {
                            ent.activeNode.Remove();
                            ent.activeNode.AddToFront(this.activeEntities);
                        }
                    }
                }

                for (ent = this.activeEntities.Next(); ent != null; ent = next_ent) {
                    next_ent = ent.activeNode.Next();
                    master = ent.GetTeamMaster();
                    if ((null == master) || (master == ent)) {
                        // check if there is an entity on the team using parametric physics
                        for (part = ent; part != null; part = part.GetNextTeamEntity()) {
                            if (part.GetPhysics().IsType(idPhysics_Parametric.class)) {
                                break;
                            }
                        }
                        // if there is an entity on the team using parametric physics
                        if (part != null) {
                            ent.activeNode.Remove();
                            ent.activeNode.AddToFront(this.activeEntities);
                        }
                    }
                }
            }

            this.sortTeamMasters = false;
            this.sortPushers = false;
        }

        private void ShowTargets() {
            final idMat3 axis = GetLocalPlayer().viewAngles.ToMat3();
            final idVec3 up = axis.oGet(2).oMultiply(5.0f);
            final idVec3 viewPos = GetLocalPlayer().GetPhysics().GetOrigin();
            final idBounds viewTextBounds = new idBounds(viewPos);
            final idBounds viewBounds = new idBounds(viewPos);
            final idBounds box = new idBounds(new idVec3(-4.0f, -4.0f, -4.0f), new idVec3(4.0f, 4.0f, 4.0f));
            idEntity ent;
            idEntity target;
            int i;
            idBounds totalBounds;

            viewTextBounds.ExpandSelf(128.0f);
            viewBounds.ExpandSelf(512.0f);
            for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                totalBounds = ent.GetPhysics().GetAbsBounds();
                for (i = 0; i < ent.targets.Num(); i++) {
                    target = ent.targets.oGet(i).GetEntity();
                    if (target != null) {
                        totalBounds.AddBounds(target.GetPhysics().GetAbsBounds());
                    }
                }

                if (!viewBounds.IntersectsBounds(totalBounds)) {
                    continue;
                }

                final float[] dist = {0};
                final idVec3 dir = totalBounds.GetCenter().oMinus(viewPos);
                dir.NormalizeFast();
                totalBounds.RayIntersection(viewPos, dir, dist);
                final float frac = (512.0f - dist[0]) / 512.0f;
                if (frac < 0.0f) {
                    continue;
                }

                gameRenderWorld.DebugBounds((ent.IsHidden() ? colorLtGrey : colorOrange).oMultiply(frac), ent.GetPhysics().GetAbsBounds());
                if (viewTextBounds.IntersectsBounds(ent.GetPhysics().GetAbsBounds())) {
                    final idVec3 center = ent.GetPhysics().GetAbsBounds().GetCenter();
                    gameRenderWorld.DrawText(ent.name.getData(), center.oMinus(up), 0.1f, (colorWhite).oMultiply(frac), axis, 1);
                    gameRenderWorld.DrawText(ent.GetEntityDefName(), center, 0.1f, (colorWhite).oMultiply(frac), axis, 1);
                    gameRenderWorld.DrawText(va("#%d", ent.entityNumber), center.oPlus(up), 0.1f, (colorWhite).oMultiply(frac), axis, 1);
                }

                for (i = 0; i < ent.targets.Num(); i++) {
                    target = ent.targets.oGet(i).GetEntity();
                    if (target != null) {
                        gameRenderWorld.DebugArrow((colorYellow).oMultiply(frac), ent.GetPhysics().GetAbsBounds().GetCenter(), target.GetPhysics().GetOrigin(), 10, 0);
                        gameRenderWorld.DebugBounds((colorGreen).oMultiply(frac), box, target.GetPhysics().GetOrigin());
                    }
                }
            }
        }

        private void RunDebugInfo() {
            idEntity ent;
            idPlayer player;

            player = GetLocalPlayer();
            if (null == player) {
                return;
            }

            final idVec3 origin = player.GetPhysics().GetOrigin();

            if (g_showEntityInfo.GetBool()) {
                final idMat3 axis = player.viewAngles.ToMat3();
                final idVec3 up = axis.oGet(2).oMultiply(5.0f);
                final idBounds viewTextBounds = new idBounds(origin);
                final idBounds viewBounds = new idBounds(origin);

                viewTextBounds.ExpandSelf(128.0f);
                viewBounds.ExpandSelf(512.0f);
                for (ent = this.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                    // don't draw the worldspawn
                    if (ent == this.world) {
                        continue;
                    }

                    // skip if the entity is very far away
                    if (!viewBounds.IntersectsBounds(ent.GetPhysics().GetAbsBounds())) {
                        continue;
                    }

                    final idBounds entBounds = ent.GetPhysics().GetAbsBounds();
                    final int contents = ent.GetPhysics().GetContents();
                    if ((contents & CONTENTS_BODY) != 0) {
                        gameRenderWorld.DebugBounds(colorCyan, entBounds);
                    } else if ((contents & CONTENTS_TRIGGER) != 0) {
                        gameRenderWorld.DebugBounds(colorOrange, entBounds);
                    } else if ((contents & CONTENTS_SOLID) != 0) {
                        gameRenderWorld.DebugBounds(colorGreen, entBounds);
                    } else {
                        if (0 == entBounds.GetVolume()) {
                            gameRenderWorld.DebugBounds(colorMdGrey, entBounds.Expand(8.0f));
                        } else {
                            gameRenderWorld.DebugBounds(colorMdGrey, entBounds);
                        }
                    }
                    if (viewTextBounds.IntersectsBounds(entBounds)) {
                        gameRenderWorld.DrawText(ent.name.getData(), entBounds.GetCenter(), 0.1f, colorWhite, axis, 1);
                        gameRenderWorld.DrawText(va("#%d", ent.entityNumber), entBounds.GetCenter().oPlus(up), 0.1f, colorWhite, axis, 1);
                    }
                }
            }

            // debug tool to draw bounding boxes around active entities
            if (g_showActiveEntities.GetBool()) {
                for (ent = this.activeEntities.Next(); ent != null; ent = ent.activeNode.Next()) {
                    final idBounds b = ent.GetPhysics().GetBounds();
                    if (b.GetVolume() <= 0) {
                        b.oSet(0, 0, b.oSet(0, 1, b.oSet(0, 2, -8)));
                        b.oSet(1, 0, b.oSet(1, 1, b.oSet(1, 2, 8)));
                    }
                    if (ent.fl.isDormant) {
                        gameRenderWorld.DebugBounds(colorYellow, b, ent.GetPhysics().GetOrigin());
                    } else {
                        gameRenderWorld.DebugBounds(colorGreen, b, ent.GetPhysics().GetOrigin());
                    }
                }
            }

            if (g_showTargets.GetBool()) {
                ShowTargets();
            }
//
//            if (g_showTriggers.GetBool()) {
//                idTrigger.DrawDebugInfo();//TODO:
//            }
//
            if (ai_showCombatNodes.GetBool()) {
                idCombatNode.DrawDebugInfo();
            }

            if (ai_showPaths.GetBool()) {
                idPathCorner.DrawDebugInfo();
            }

            if (g_editEntityMode.GetBool()) {
                this.editEntities.DisplayEntities();
            }

            if (g_showCollisionWorld.GetBool()) {
                CollisionModel_local.collisionModelManager.DrawModel(0, getVec3_origin(), getMat3_identity(), origin, 128.0f);
            }

            if (g_showCollisionModels.GetBool()) {
                this.clip.DrawClipModels(player.GetEyePosition(), g_maxShowDistance.GetFloat(), pm_thirdPerson.GetBool() ? null : player);
            }

            if (g_showCollisionTraces.GetBool()) {
                this.clip.PrintStatistics();
            }

            if (g_showPVS.GetInteger() != 0) {
                this.pvs.DrawPVS(origin, (g_showPVS.GetInteger() == 2) ? PVS_ALL_PORTALS_OPEN : PVS_NORMAL);
            }

            if (aas_test.GetInteger() >= 0) {
                final idAAS aas = GetAAS(aas_test.GetInteger());
                if (aas != null) {
                    aas.Test(origin);
                    if (ai_testPredictPath.GetBool()) {
                        final idVec3 velocity = new idVec3();
                        final predictedPath_s path = new predictedPath_s();

                        velocity.x = (float) (cos(DEG2RAD(player.viewAngles.yaw)) * 100.0f);
                        velocity.y = (float) (sin(DEG2RAD(player.viewAngles.yaw)) * 100.0f);
                        velocity.z = 0.0f;
                        idAI.PredictPath(player, aas, origin, velocity, 1000, 100, SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA, path);
                    }
                }
            }

            if (ai_showObstacleAvoidance.GetInteger() == 2) {
                final idAAS aas = GetAAS(0);
                if (aas != null) {
                    idVec3 seekPos;
                    final obstaclePath_s path = new obstaclePath_s();

                    seekPos = player.GetPhysics().GetOrigin().oPlus(player.viewAxis.oGet(0).oMultiply(200.0f));
                    idAI.FindPathAroundObstacles(player.GetPhysics(), aas, null, player.GetPhysics().GetOrigin(), seekPos, path);
                }
            }

            // collision map debug output
            CollisionModel_local.collisionModelManager.DebugOutput(player.GetEyePosition());
        }

        private void InitScriptForMap() {
            // create a thread to run frame commands on
            this.frameCommandThread = new idThread();
            this.frameCommandThread.ManualDelete();
            this.frameCommandThread.SetThreadName("frameCommands");

            // run the main game script function (not the level specific main)
            final function_t func = this.program.FindFunction(SCRIPT_DEFAULTFUNC);
            if (func != null) {
                final idThread thread = new idThread(func);
                if (thread.Start()) {
                    // thread has finished executing, so delete it
//			delete thread;
                }
            }
        }


        /*
         =================
         idGameLocal::InitConsoleCommands

         Let the system know about all of our commands
         so it can perform tab completion
         =================
         */
        private void InitConsoleCommands() {
            cmdSystem.AddCommand("listTypeInfo", ListTypeInfo_f.getInstance(), CMD_FL_GAME, "list type info");
            cmdSystem.AddCommand("writeGameState", WriteGameState_f.getInstance(), CMD_FL_GAME, "write game state");
            cmdSystem.AddCommand("testSaveGame", TestSaveGame_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "test a save game for a level");
            cmdSystem.AddCommand("game_memory", idClass.DisplayInfo_f.getInstance(), CMD_FL_GAME, "displays game class info");
            cmdSystem.AddCommand("listClasses", idClass.ListClasses_f.getInstance(), CMD_FL_GAME, "lists game classes");
            cmdSystem.AddCommand("listThreads", idThread.ListThreads_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "lists script threads");
            cmdSystem.AddCommand("listEntities", Cmd_EntityList_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "lists game entities");
            cmdSystem.AddCommand("listActiveEntities", Cmd_ActiveEntityList_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "lists active game entities");
            cmdSystem.AddCommand("listMonsters", idAI.List_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "lists monsters");
            cmdSystem.AddCommand("listSpawnArgs", Cmd_ListSpawnArgs_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "list the spawn args of an entity", idGameLocal.ArgCompletion_EntityName.getInstance());
            cmdSystem.AddCommand("say", Cmd_Say_f.getInstance(), CMD_FL_GAME, "text chat");
            cmdSystem.AddCommand("sayTeam", Cmd_SayTeam_f.getInstance(), CMD_FL_GAME, "team text chat");
            cmdSystem.AddCommand("addChatLine", Cmd_AddChatLine_f.getInstance(), CMD_FL_GAME, "internal use - core to game chat lines");
            cmdSystem.AddCommand("gameKick", Cmd_Kick_f.getInstance(), CMD_FL_GAME, "same as kick, but recognizes player names");
            cmdSystem.AddCommand("give", Cmd_Give_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "gives one or more items");
            cmdSystem.AddCommand("centerview", Cmd_CenterView_f.getInstance(), CMD_FL_GAME, "centers the view");
            cmdSystem.AddCommand("god", Cmd_God_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "enables god mode");
            cmdSystem.AddCommand("notarget", Cmd_Notarget_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "disables the player as a target");
            cmdSystem.AddCommand("noclip", Cmd_Noclip_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "disables collision detection for the player");
            cmdSystem.AddCommand("kill", Cmd_Kill_f.getInstance(), CMD_FL_GAME, "kills the player");
            cmdSystem.AddCommand("where", Cmd_GetViewpos_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "prints the current view position");
            cmdSystem.AddCommand("getviewpos", Cmd_GetViewpos_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "prints the current view position");
            cmdSystem.AddCommand("setviewpos", Cmd_SetViewpos_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "sets the current view position");
            cmdSystem.AddCommand("teleport", Cmd_Teleport_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "teleports the player to an entity location", idGameLocal.ArgCompletion_EntityName.getInstance());
            cmdSystem.AddCommand("trigger", Cmd_Trigger_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "triggers an entity", idGameLocal.ArgCompletion_EntityName.getInstance());
            cmdSystem.AddCommand("spawn", Cmd_Spawn_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "spawns a game entity", new idCmdSystem.ArgCompletion_Decl(DECL_ENTITYDEF));
            cmdSystem.AddCommand("damage", Cmd_Damage_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "apply damage to an entity", idGameLocal.ArgCompletion_EntityName.getInstance());
            cmdSystem.AddCommand("remove", Cmd_Remove_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "removes an entity", idGameLocal.ArgCompletion_EntityName.getInstance());
            cmdSystem.AddCommand("killMonsters", Cmd_KillMonsters_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "removes all monsters");
            cmdSystem.AddCommand("killMoveables", Cmd_KillMovables_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "removes all moveables");
            cmdSystem.AddCommand("killRagdolls", Cmd_KillRagdolls_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "removes all ragdolls");
            cmdSystem.AddCommand("addline", Cmd_AddDebugLine_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "adds a debug line");
            cmdSystem.AddCommand("addarrow", Cmd_AddDebugLine_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "adds a debug arrow");
            cmdSystem.AddCommand("removeline", Cmd_RemoveDebugLine_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "removes a debug line");
            cmdSystem.AddCommand("blinkline", Cmd_BlinkDebugLine_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "blinks a debug line");
            cmdSystem.AddCommand("listLines", Cmd_ListDebugLines_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "lists all debug lines");
            cmdSystem.AddCommand("playerModel", Cmd_PlayerModel_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "sets the given model on the player", new idCmdSystem.ArgCompletion_Decl(DECL_MODELDEF));
            cmdSystem.AddCommand("testFx", Cmd_TestFx_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests an FX system", new idCmdSystem.ArgCompletion_Decl(DECL_FX));
            cmdSystem.AddCommand("testBoneFx", Cmd_TestBoneFx_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests an FX system bound to a joint", new idCmdSystem.ArgCompletion_Decl(DECL_FX));
            cmdSystem.AddCommand("testLight", Cmd_TestLight_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests a light");
            cmdSystem.AddCommand("testPointLight", Cmd_TestPointLight_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests a point light");
            cmdSystem.AddCommand("popLight", Cmd_PopLight_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "removes the last created light");
            cmdSystem.AddCommand("testDeath", Cmd_TestDeath_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests death");
            cmdSystem.AddCommand("testSave", Cmd_TestSave_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "writes out a test savegame");
            cmdSystem.AddCommand("testModel", idTestModel.TestModel_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests a model", idTestModel.ArgCompletion_TestModel.getInstance());
            cmdSystem.AddCommand("testSkin", idTestModel.TestSkin_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests a skin on an existing testModel", new idCmdSystem.ArgCompletion_Decl(DECL_SKIN));
            cmdSystem.AddCommand("testShaderParm", idTestModel.TestShaderParm_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "sets a shaderParm on an existing testModel");
            cmdSystem.AddCommand("keepTestModel", idTestModel.KeepTestModel_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "keeps the last test model in the game");
            cmdSystem.AddCommand("testAnim", idTestModel.TestAnim_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests an animation", idTestModel.ArgCompletion_TestAnim.getInstance());
            cmdSystem.AddCommand("testParticleStopTime", idTestModel.TestParticleStopTime_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests particle stop time on a test model");
            cmdSystem.AddCommand("nextAnim", idTestModel.TestModelNextAnim_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "shows next animation on test model");
            cmdSystem.AddCommand("prevAnim", idTestModel.TestModelPrevAnim_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "shows previous animation on test model");
            cmdSystem.AddCommand("nextFrame", idTestModel.TestModelNextFrame_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "shows next animation frame on test model");
            cmdSystem.AddCommand("prevFrame", idTestModel.TestModelPrevFrame_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "shows previous animation frame on test model");
            cmdSystem.AddCommand("testBlend", idTestModel.TestBlend_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests animation blending");
            cmdSystem.AddCommand("reloadScript", Cmd_ReloadScript_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "reloads scripts");
            cmdSystem.AddCommand("script", Cmd_Script_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "executes a line of script");
            cmdSystem.AddCommand("listCollisionModels", Cmd_ListCollisionModels_f.getInstance(), CMD_FL_GAME, "lists collision models");
            cmdSystem.AddCommand("collisionModelInfo", Cmd_CollisionModelInfo_f.getInstance(), CMD_FL_GAME, "shows collision model info");
            cmdSystem.AddCommand("reexportmodels", Cmd_ReexportModels_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "reexports models", ArgCompletion_DefFile.getInstance());
            cmdSystem.AddCommand("reloadanims", Cmd_ReloadAnims_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "reloads animations");
            cmdSystem.AddCommand("listAnims", Cmd_ListAnims_f.getInstance(), CMD_FL_GAME, "lists all animations");
            cmdSystem.AddCommand("aasStats", Cmd_AASStats_f.getInstance(), CMD_FL_GAME, "shows AAS stats");
            cmdSystem.AddCommand("testDamage", Cmd_TestDamage_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "tests a damage def", new idCmdSystem.ArgCompletion_Decl(DECL_ENTITYDEF));
            cmdSystem.AddCommand("weaponSplat", Cmd_WeaponSplat_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "projects a blood splat on the player weapon");
            cmdSystem.AddCommand("saveSelected", Cmd_SaveSelected_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "saves the selected entity to the .map file");
            cmdSystem.AddCommand("deleteSelected", Cmd_DeleteSelected_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "deletes selected entity");
            cmdSystem.AddCommand("saveMoveables", Cmd_SaveMoveables_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "save all moveables to the .map file");
            cmdSystem.AddCommand("saveRagdolls", Cmd_SaveRagdolls_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "save all ragdoll poses to the .map file");
            cmdSystem.AddCommand("bindRagdoll", Cmd_BindRagdoll_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "binds ragdoll at the current drag position");
            cmdSystem.AddCommand("unbindRagdoll", Cmd_UnbindRagdoll_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "unbinds the selected ragdoll");
            cmdSystem.AddCommand("saveLights", Cmd_SaveLights_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "saves all lights to the .map file");
            cmdSystem.AddCommand("saveParticles", Cmd_SaveParticles_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "saves all lights to the .map file");
            cmdSystem.AddCommand("clearLights", Cmd_ClearLights_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "clears all lights");
            cmdSystem.AddCommand("gameError", Cmd_GameError_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "causes a game error");

            if (!ID_DEMO_BUILD) {//#ifndef
                cmdSystem.AddCommand("disasmScript", Cmd_DisasmScript_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "disassembles script");
                cmdSystem.AddCommand("recordViewNotes", Cmd_RecordViewNotes_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "record the current view position with notes");
                cmdSystem.AddCommand("showViewNotes", Cmd_ShowViewNotes_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "show any view notes for the current map, successive calls will cycle to the next note");
                cmdSystem.AddCommand("closeViewNotes", Cmd_CloseViewNotes_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "close the view showing any notes for this map");
                cmdSystem.AddCommand("exportmodels", Cmd_ExportModels_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "exports models", ArgCompletion_DefFile.getInstance());

                // multiplayer client commands ( replaces old impulses stuff )
                cmdSystem.AddCommand("clientDropWeapon", idMultiplayerGame.DropWeapon_f.getInstance(), CMD_FL_GAME, "drop current weapon");
                cmdSystem.AddCommand("clientMessageMode", idMultiplayerGame.MessageMode_f.getInstance(), CMD_FL_GAME, "ingame gui message mode");
                // FIXME: implement
//	cmdSystem.AddCommand( "clientVote",			idMultiplayerGame.Vote_f.getInstance(),	CMD_FL_GAME,				"cast your vote: clientVote yes | no" );
//	cmdSystem.AddCommand( "clientCallVote",		idMultiplayerGame.CallVote_f.getInstance(),	CMD_FL_GAME,			"call a vote: clientCallVote si_.. proposed_value" );
                cmdSystem.AddCommand("clientVoiceChat", idMultiplayerGame.VoiceChat_f.getInstance(), CMD_FL_GAME, "voice chats: clientVoiceChat <sound shader>");
                cmdSystem.AddCommand("clientVoiceChatTeam", idMultiplayerGame.VoiceChatTeam_f.getInstance(), CMD_FL_GAME, "team voice chats: clientVoiceChat <sound shader>");

                // multiplayer server commands
                cmdSystem.AddCommand("serverMapRestart", idGameLocal.MapRestart_f.getInstance(), CMD_FL_GAME, "restart the current game");
                cmdSystem.AddCommand("serverForceReady", idMultiplayerGame.ForceReady_f.getInstance(), CMD_FL_GAME, "force all players ready");
                cmdSystem.AddCommand("serverNextMap", idGameLocal.NextMap_f.getInstance(), CMD_FL_GAME, "change to the next map");
            }

            // localization help commands
            cmdSystem.AddCommand("nextGUI", Cmd_NextGUI_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "teleport the player to the next func_static with a gui");
            cmdSystem.AddCommand("testid", Cmd_TestId_f.getInstance(), CMD_FL_GAME | CMD_FL_CHEAT, "output the string for the specified id.");
        }

        private void ShutdownConsoleCommands() {
            cmdSystem.RemoveFlaggedCommands((int) CMD_FL_GAME);
        }

        private void InitAsyncNetwork() {
            int i, type;

            for (i = 0; i < MAX_CLIENTS; i++) {
                for (type = 0; type < declManager.GetNumDeclTypes(); type++) {
                    this.clientDeclRemap[i][type] = new idList<>();
                }
            }

//	memset( clientEntityStates, 0, sizeof( clientEntityStates ) );
            this.clientEntityStates = new entityState_s[this.clientEntityStates.length][this.clientEntityStates[0].length];
//	memset( clientPVS, 0, sizeof( clientPVS ) );
            this.clientPVS = new int[this.clientPVS.length][this.clientPVS[0].length];
//	memset( clientSnapshots, 0, sizeof( clientSnapshots ) );
            this.clientSnapshots = new snapshot_s[this.clientSnapshots.length];

            this.eventQueue.Init();
            this.savedEventQueue.Init();

            this.entityDefBits = -(idMath.BitsForInteger(declManager.GetNumDecls(DECL_ENTITYDEF)) + 1);
            this.localClientNum = 0; // on a listen server SetLocalUser will set this right
            this.realClientTime = 0;
            this.isNewFrame = true;
            this.clientSmoothing = net_clientSmoothing.GetFloat();
        }

        private void ShutdownAsyncNetwork() {
//            entityStateAllocator.Shutdown();
//            snapshotAllocator.Shutdown();
            this.eventQueue.Shutdown();
            this.savedEventQueue.Shutdown();
            //	memset( clientEntityStates, 0, sizeof( clientEntityStates ) );
            this.clientEntityStates = new entityState_s[this.clientEntityStates.length][this.clientEntityStates[0].length];
//	memset( clientPVS, 0, sizeof( clientPVS ) );
            this.clientPVS = new int[this.clientPVS.length][this.clientPVS[0].length];
//	memset( clientSnapshots, 0, sizeof( clientSnapshots ) );
            this.clientSnapshots = new snapshot_s[this.clientSnapshots.length];
        }

        private void InitLocalClient(int clientNum) {
            this.isServer = false;
            this.isClient = true;
            this.localClientNum = clientNum;
            this.clientSmoothing = net_clientSmoothing.GetFloat();
        }

        private void InitClientDeclRemap(int clientNum) {
            int type, i, num;

            for (type = 0; type < declManager.GetNumDeclTypes(); type++) {

                // only implicit materials and sound shaders decls are used
                if ((type != etoi(DECL_MATERIAL)) && (type != etoi(DECL_SOUND))) {
                    continue;
                }

                num = declManager.GetNumDecls(type);
                this.clientDeclRemap[clientNum][type].Clear();
                this.clientDeclRemap[clientNum][type].AssureSize(num, -1);

                // pre-initialize the remap with non-implicit decls, all non-implicit decls are always going
                // to be in order and in sync between server and client because of the decl manager checksum
                for (i = 0; i < num; i++) {
                    final idDecl decl = declManager.DeclByIndex(declType_t.values()[type], i, false);
                    if (decl.IsImplicit()) {
                        // once the first implicit decl is found all remaining decls are considered implicit as well
                        break;
                    }
                    this.clientDeclRemap[clientNum][type].oSet(i, i);
                }
            }
        }

        private void ServerSendDeclRemapToClient(int clientNum, declType_t type, int index) {
            final idBitMsg outMsg = new idBitMsg();
            final ByteBuffer msgBuf = ByteBuffer.allocate(MAX_GAME_MESSAGE_SIZE);

            // if no client connected for this spot
            if (this.entities[clientNum] == null) {
                return;
            }
            // increase size of list if required
            if (index >= this.clientDeclRemap[clientNum][type.ordinal()].Num()) {
                this.clientDeclRemap[clientNum][type.ordinal()].AssureSize(index + 1, -1);
            }
            // if already remapped
            if (this.clientDeclRemap[clientNum][type.ordinal()].oGet(index) != -1) {
                return;
            }

            final idDecl decl = declManager.DeclByIndex(type, index, false);
            if (decl == null) {
                idGameLocal.Error("server tried to remap bad %s decl index %d", declManager.GetDeclNameFromType(type), index);
                return;
            }

            // set the index at the server
            this.clientDeclRemap[clientNum][type.ordinal()].oSet(index, index);

            // write update to client
            outMsg.Init(msgBuf, MAX_GAME_MESSAGE_SIZE);
            outMsg.BeginWriting();
            outMsg.WriteByte(GAME_RELIABLE_MESSAGE_REMAP_DECL);
            outMsg.WriteByte(etoi(type));
            outMsg.WriteLong(index);
            outMsg.WriteString(decl.GetName());
            networkSystem.ServerSendReliableMessage(clientNum, outMsg);
        }

        private void FreeSnapshotsOlderThanSequence(int clientNum, int sequence) {
            snapshot_s snapshot, lastSnapshot, nextSnapshot;
            entityState_s state;

            for (lastSnapshot = null, snapshot = this.clientSnapshots[clientNum]; snapshot != null; snapshot = nextSnapshot) {
                nextSnapshot = snapshot.next;
                if (snapshot.sequence < sequence) {
                    for (state = snapshot.firstEntityState; state != null; state = snapshot.firstEntityState) {
                        snapshot.firstEntityState = snapshot.firstEntityState.next;
//                        entityStateAllocator.Free(state);
                    }
                    if (lastSnapshot != null) {
                        lastSnapshot.next = snapshot.next;
                    } else {
                        this.clientSnapshots[clientNum] = snapshot.next;
                    }
//                    snapshotAllocator.Free(snapshot);
                } else {
                    lastSnapshot = snapshot;
                }
            }
        }

        private boolean ApplySnapshot(int clientNum, int sequence) {
            snapshot_s snapshot, lastSnapshot, nextSnapshot;
            entityState_s state;

            FreeSnapshotsOlderThanSequence(clientNum, sequence);

            for (lastSnapshot = null, snapshot = this.clientSnapshots[clientNum]; snapshot != null; snapshot = nextSnapshot) {
                nextSnapshot = snapshot.next;
                if (snapshot.sequence == sequence) {
                    for (state = snapshot.firstEntityState; state != null; state = state.next) {
                        if (this.clientEntityStates[clientNum][state.entityNumber] != null) {
//                            entityStateAllocator.Free(clientEntityStates[clientNum][state.entityNumber]);
                        }
                        this.clientEntityStates[clientNum][state.entityNumber] = state;
                    }
//			memcpy( clientPVS[clientNum], snapshot.pvs, sizeof( snapshot.pvs ) );
                    Nio.arraycopy(snapshot.pvs, 0, this.clientPVS[clientNum], 0, snapshot.pvs.length);
                    if (lastSnapshot != null) {
                        lastSnapshot.next = nextSnapshot;
                    } else {
                        this.clientSnapshots[clientNum] = nextSnapshot;
                    }
//                    snapshotAllocator.Free(snapshot);
                    return true;
                } else {
                    lastSnapshot = snapshot;
                }
            }

            return false;
        }

        private void WriteGameStateToSnapshot(idBitMsgDelta msg) {
            int i;

            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                msg.WriteFloat(this.globalShaderParms[i]);
            }

            this.mpGame.WriteToSnapshot(msg);
        }

        private void ReadGameStateFromSnapshot(final idBitMsgDelta msg) {
            int i;

            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                this.globalShaderParms[i] = msg.ReadFloat();
            }

            this.mpGame.ReadFromSnapshot(msg);
        }

        private void NetworkEventWarning(final entityNetEvent_s event, final String... fmt) {//id_attribute((format(printf,3,4)));
//	char buf[1024];
//	int length = 0;
//	va_list argptr;
//
//	int entityNum	= event.spawnId & ( ( 1 << GENTITYNUM_BITS ) - 1 );
//	int id			= event.spawnId >> GENTITYNUM_BITS;
//
//	length += idStr.snPrintf( buf+length, sizeof(buf)-1-length, "event %d for entity %d %d: ", event.event, entityNum, id );
//	va_start( argptr, fmt );
//	length = idStr.vsnPrintf( buf+length, sizeof(buf)-1-length, fmt, argptr );
//	va_end( argptr );
//	idStr.Append( buf, sizeof(buf), "\n" );
//
//	common.DWarning( buf );//TODO:
        }

        private void ServerProcessEntityNetworkEventQueue() {
            idEntity ent;
            entityNetEvent_s event;
            final idBitMsg eventMsg = new idBitMsg();

            while (this.eventQueue.Start() != null) {
                event = this.eventQueue.Start();

                if (event.time > this.time) {
                    break;
                }

                final idEntityPtr< idEntity> entPtr = new idEntityPtr<>();

                if (!entPtr.SetSpawnId(event.spawnId)) {
                    NetworkEventWarning(event, "Entity does not exist any longer, or has not been spawned yet.");
                } else {
                    ent = entPtr.GetEntity();
                    assert (ent != null);

                    eventMsg.Init(event.paramsBuf, event.paramsBuf.capacity());
                    eventMsg.SetSize(event.paramsSize);
                    eventMsg.BeginReading();
                    if (!ent.ServerReceiveEvent(event.event, event.time, eventMsg)) {
                        NetworkEventWarning(event, "unknown event");
                    }
                }

                final entityNetEvent_s freedEvent = this.eventQueue.Dequeue();
                assert (freedEvent == event);
                this.eventQueue.Free(event);
            }
        }

        private void ClientProcessEntityNetworkEventQueue() {
            idEntity ent;
            entityNetEvent_s event;
            final idBitMsg eventMsg = new idBitMsg();

            while (this.eventQueue.Start() != null) {
                event = this.eventQueue.Start();

                // only process forward, in order
                if (event.time > this.time) {
                    break;
                }

                final idEntityPtr< idEntity> entPtr = new idEntityPtr<>();

                if (!entPtr.SetSpawnId(event.spawnId)) {
                    if (null == gameLocal.entities[ event.spawnId & ((1 << GENTITYNUM_BITS) - 1)]) {
                        // if new entity exists in this position, silently ignore
                        NetworkEventWarning(event, "Entity does not exist any longer, or has not been spawned yet.");
                    }
                } else {
                    ent = entPtr.GetEntity();
                    assert (ent != null);

                    eventMsg.Init(event.paramsBuf, event.paramsBuf.capacity());
                    eventMsg.SetSize(event.paramsSize);
                    eventMsg.BeginReading();
                    if (!ent.ClientReceiveEvent(event.event, event.time, eventMsg)) {
                        NetworkEventWarning(event, "unknown event");
                    }
                }

                final entityNetEvent_s freedEvent = this.eventQueue.Dequeue();
                assert (freedEvent.equals(event));
                this.eventQueue.Free(event);
            }
        }

        private void ClientShowSnapshot(int clientNum) {
            int baseBits;
            idEntity ent;
            idPlayer player;
            idMat3 viewAxis;
            idBounds viewBounds;
            entityState_s base;

            if (0 == net_clientShowSnapshot.GetInteger()) {
                return;
            }

            player = (idPlayer) this.entities[clientNum];
            if (null == player) {
                return;
            }

            viewAxis = player.viewAngles.ToMat3();
            viewBounds = player.GetPhysics().GetAbsBounds().Expand(net_clientShowSnapshotRadius.GetFloat());

            for (ent = this.snapshotEntities.Next(); ent != null; ent = ent.snapshotNode.Next()) {

                if ((net_clientShowSnapshot.GetInteger() == 1) && (ent.snapshotBits == 0)) {
                    continue;
                }

                final idBounds entBounds = ent.GetPhysics().GetAbsBounds();

                if (!entBounds.IntersectsBounds(viewBounds)) {
                    continue;
                }

                base = this.clientEntityStates[clientNum][ent.entityNumber];
                if (base != null) {
                    baseBits = base.state.GetNumBitsWritten();
                } else {
                    baseBits = 0;
                }

                if ((net_clientShowSnapshot.GetInteger() == 2) && (baseBits == 0)) {
                    continue;
                }

                gameRenderWorld.DebugBounds(colorGreen, entBounds);
                gameRenderWorld.DrawText(va("%d: %s (%d,%d bytes of %d,%d)\n", ent.entityNumber,
                        ent.name, ent.snapshotBits >> 3, ent.snapshotBits & 7, baseBits >> 3, baseBits & 7),
                        entBounds.GetCenter(), 0.1f, colorWhite, viewAxis, 1);
            }
        }

        // call after any change to serverInfo. Will update various quick-access flags
        private void UpdateServerInfoFlags() {
            this.gameType = GAME_SP;
            if ((idStr.Icmp(this.serverInfo.GetString("si_gameType"), "deathmatch") == 0)) {
                this.gameType = GAME_DM;
            } else if ((idStr.Icmp(this.serverInfo.GetString("si_gameType"), "Tourney") == 0)) {
                this.gameType = GAME_TOURNEY;
            } else if ((idStr.Icmp(this.serverInfo.GetString("si_gameType"), "Team DM") == 0)) {
                this.gameType = GAME_TDM;
            } else if ((idStr.Icmp(this.serverInfo.GetString("si_gameType"), "Last Man") == 0)) {
                this.gameType = GAME_LASTMAN;
            }
            if (this.gameType == GAME_LASTMAN) {
                if (0 == this.serverInfo.GetInt("si_warmup")) {
                    common.Warning("Last Man Standing - forcing warmup on");
                    this.serverInfo.SetInt("si_warmup", 1);
                }
                if (this.serverInfo.GetInt("si_fraglimit") <= 0) {
                    common.Warning("Last Man Standing - setting fraglimit 1");
                    this.serverInfo.SetInt("si_fraglimit", 1);
                }
            }
        }

        /*
         ===========
         idGameLocal::RandomizeInitialSpawns
         randomize the order of the initial spawns
         prepare for a sequence of initial player spawns
         ============
         */
        private void RandomizeInitialSpawns() {
            final spawnSpot_t spot = new spawnSpot_t();
            int i, j;
            idEntity ent;

            if (!this.isMultiplayer || this.isClient) {
                return;
            }
            this.spawnSpots.Clear();
            this.initialSpots.Clear();
            spot.dist = 0;
            spot.ent = FindEntityUsingDef(null, "info_player_deathmatch");
            while (spot.ent != null) {
                this.spawnSpots.Append(spot);
                if (spot.ent.spawnArgs.GetBool("initial")) {
                    this.initialSpots.Append(spot.ent);
                }
                spot.ent = FindEntityUsingDef(spot.ent, "info_player_deathmatch");
            }
            if (0 == this.spawnSpots.Num()) {
                common.Warning("no info_player_deathmatch in map");
                return;
            }
            common.Printf("%d spawns (%d initials)\n", this.spawnSpots.Num(), this.initialSpots.Num());
            // if there are no initial spots in the map, consider they can all be used as initial
            if (0 == this.initialSpots.Num()) {
                common.Warning("no info_player_deathmatch entities marked initial in map");
                for (i = 0; i < this.spawnSpots.Num(); i++) {
                    this.initialSpots.Append(this.spawnSpots.oGet(i).ent);
                }
            }
            for (i = 0; i < this.initialSpots.Num(); i++) {
                j = this.random.RandomInt(this.initialSpots.Num());
                ent = this.initialSpots.oGet(i);
                this.initialSpots.oSet(i, this.initialSpots.oGet(j));
                this.initialSpots.oSet(j, ent);
            }
            // reset the counter
            this.currentInitialSpot = 0;
        }

        private static class sortSpawnPoints implements cmp_t<spawnSpot_t> {

            @Override
            public int compare(spawnSpot_t s1, spawnSpot_t s2) {
                float diff;

                diff = s1.dist - s2.dist;
                if (diff < 0.0f) {
                    return 1;
                } else if (diff > 0.0f) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }

        private void DumpOggSounds() {
            int i, j, k, size, totalSize;
            idFile file;
            final idStrList oggSounds = new idStrList(), weaponSounds = new idStrList();
            idSoundShader soundShader;
            soundShaderParms_t parms;
            idStr soundName;

            for (i = 0; i < declManager.GetNumDecls(DECL_SOUND); i++) {
                soundShader = (idSoundShader) declManager.DeclByIndex(DECL_SOUND, i, false);
                parms = soundShader.GetParms();

                if (soundShader.EverReferenced() && (soundShader.GetState() != DS_DEFAULTED)) {

                    soundShader.EnsureNotPurged();

                    for (j = 0; j < soundShader.GetNumSounds(); j++) {
                        soundName = new idStr(soundShader.GetSound(j));
                        soundName.BackSlashesToSlashes();

                        // don't OGG sounds that cause a shake because that would
                        // cause continuous seeking on the OGG file which is expensive
                        if (parms.shakes != 0.0f) {
                            this.shakeSounds.AddUnique(soundName);
                            continue;
                        }

                        // if not voice over or combat chatter
                        if ((soundName.Find("/vo/", false) == -1)
                                && (soundName.Find("/combat_chatter/", false) == -1)
                                && (soundName.Find("/bfgcarnage/", false) == -1)
                                && (soundName.Find("/enpro/", false) == - 1)
                                && (soundName.Find("/soulcube/energize_01.wav", false) == -1)) {
                            // don't OGG weapon sounds
                            if ((soundName.Find("weapon", false) != -1)
                                    || (soundName.Find("gun", false) != -1)
                                    || (soundName.Find("bullet", false) != -1)
                                    || (soundName.Find("bfg", false) != -1)
                                    || (soundName.Find("plasma", false) != -1)) {
                                weaponSounds.AddUnique(soundName);
                                continue;
                            }
                        }

                        for (k = 0; k < this.shakeSounds.Num(); k++) {
                            if (this.shakeSounds.oGet(k).IcmpPath(soundName.getData()) == 0) {
                                break;
                            }
                        }
                        if (k < this.shakeSounds.Num()) {
                            continue;
                        }

                        oggSounds.AddUnique(soundName);
                    }
                }
            }

            file = fileSystem.OpenFileWrite("makeogg.bat", "fs_savepath");
            if (file == null) {
                common.Warning("Couldn't open makeogg.bat");
                return;
            }

            // list all the shake sounds
            totalSize = 0;
            for (i = 0; i < this.shakeSounds.Num(); i++) {
                size = fileSystem.ReadFile(this.shakeSounds.oGet(i), null, null);
                totalSize += size;
                this.shakeSounds.oGet(i).Replace("/", "\\");
                file.Printf("echo \"%s\" (%d kB)\n", this.shakeSounds.oGet(i), size >> 10);
            }
            file.Printf("echo %d kB in shake sounds\n\n\n", totalSize >> 10);

            // list all the weapon sounds
            totalSize = 0;
            for (i = 0; i < weaponSounds.Num(); i++) {
                size = fileSystem.ReadFile(weaponSounds.oGet(i), null, null);
                totalSize += size;
                weaponSounds.oGet(i).Replace("/", "\\");
                file.Printf("echo \"%s\" (%d kB)\n", weaponSounds.oGet(i), size >> 10);
            }
            file.Printf("echo %d kB in weapon sounds\n\n\n", totalSize >> 10);

            // list commands to convert all other sounds to ogg
            totalSize = 0;
            for (i = 0; i < oggSounds.Num(); i++) {
                size = fileSystem.ReadFile(oggSounds.oGet(i), null, null);
                totalSize += size;
                oggSounds.oGet(i).Replace("/", "\\");
                file.Printf("w:\\doom\\ogg\\oggenc -q 0 \"c:\\doom\\base\\%s\"\n", oggSounds.oGet(i));
                file.Printf("del \"c:\\doom\\base\\%s\"\n", oggSounds.oGet(i));
            }
            file.Printf("\n\necho %d kB in OGG sounds\n\n\n", totalSize >> 10);

            fileSystem.CloseFile(file);

            this.shakeSounds.Clear();
        }

        private void GetShakeSounds(final idDict dict) {
            idSoundShader soundShader;
            final String soundShaderName;
            final idStr soundName = new idStr();

            soundShaderName = dict.GetString("s_shader");
            if (!soundShaderName.isEmpty() && (dict.GetFloat("s_shakes") != 0.0f)) {
                soundShader = declManager.FindSound(soundShaderName);

                for (int i = 0; i < soundShader.GetNumSounds(); i++) {
                    soundName.oSet(soundShader.GetSound(i));
                    soundName.BackSlashesToSlashes();

                    this.shakeSounds.AddUnique(soundName);
                }
            }
        }

        @Override
        public void SelectTimeGroup(int timeGroup) {
        }

        @Override
        public int GetTimeGroupTime(int timeGroup) {
            return gameLocal.time;
        }

        @Override
        public void GetBestGameType(final String map, final String gametype, char[] buf/*[MAX_STRING_CHARS ]*/) {
//	strncpy( buf, gametype, MAX_STRING_CHARS );
            Nio.arraycopy(gametype.toCharArray(), 0, buf, 0, MAX_STRING_CHARS);
            buf[ MAX_STRING_CHARS - 1] = '\0';
        }

        private void Tokenize(idStrList out, final String in) {
//	char buf[ MAX_STRING_CHARS ];
//	char *token, *next;
//	
//	idStr::Copynz( buf, in, MAX_STRING_CHARS );
//	token = buf;
//	next = strchr( token, ';' );
//	while ( token ) {
//		if ( next ) {
//			*next = '\0';
//		}
//		idStr::ToLower( token );
//		out.Append( token );
//		if ( next ) {
//			token = next + 1;
//			next = strchr( token, ';' );
//		} else {
//			token = NULL;
//		}		
//	}
            final String[] tokens = in.split(";");
            for (final String token : tokens) {
                out.Append(token);
            }
        }
        private static final byte CCLV = (byte) 255;

        private void UpdateLagometer(int aheadOfServer, int dupeUsercmds) {
            int i, j, ahead;

            for (i = 0; i < LAGO_HEIGHT; i++) {
//                memmove( (byte *)lagometer + LAGO_WIDTH * 4 * i, (byte *)lagometer + LAGO_WIDTH * 4 * i + 4, ( LAGO_WIDTH - 1 ) * 4 );
                memmove(this.lagometer, LAGO_WIDTH * 4 * i, this.lagometer, (LAGO_WIDTH * 4 * i) + 4, (LAGO_WIDTH - 1) * 4);//TODO:flatten 3d array and copy
            }
            j = LAGO_WIDTH - 1;
            for (i = 0; i < LAGO_HEIGHT; i++) {
                this.lagometer[i][j][0] = this.lagometer[i][j][1] = this.lagometer[i][j][2] = this.lagometer[i][j][3] = 0;
            }
            ahead = (int) idMath.Rint(aheadOfServer / 16.0f);
            if (ahead >= 0) {
                for (i = 2 * Max(0, 5 - ahead); i < (2 * 5); i++) {
                    this.lagometer[i][j][1] = CCLV;
                    this.lagometer[i][j][3] = CCLV;
                }
            } else {
                for (i = 2 * 5; i < (2 * (5 + Min(10, -ahead))); i++) {
                    this.lagometer[i][j][0] = CCLV;
                    this.lagometer[i][j][1] = CCLV;
                    this.lagometer[i][j][3] = CCLV;
                }
            }
            for (i = LAGO_HEIGHT - (2 * Min(6, dupeUsercmds)); i < LAGO_HEIGHT; i++) {
                this.lagometer[i][j][0] = CCLV;
                if (dupeUsercmds <= 2) {
                    this.lagometer[i][j][1] = CCLV;
                }
                this.lagometer[i][j][3] = CCLV;
            }
        }

        @Override
        public void GetMapLoadingGUI(char[] gui/*[MAX_STRING_CHARS ]*/) {
        }
    }
    //============================================================================

    public static final idAnimManager animationLib = new idAnimManager();

    //============================================================================
    public static class idGameError extends idException {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public idGameError(final String text) {
            super(text);
        }
    }
    //============================================================================

//
// these defines work for all startsounds from all entity types
// make sure to change script/doom_defs.script if you add any channels, or change their order
//
    public enum gameSoundChannel_t {

        SND_CHANNEL_ANY,//= SCHANNEL_ANY,
        SND_CHANNEL_VOICE,// = SCHANNEL_ONE,
        SND_CHANNEL_VOICE2,
        SND_CHANNEL_BODY,
        SND_CHANNEL_BODY2,
        SND_CHANNEL_BODY3,
        SND_CHANNEL_WEAPON,
        SND_CHANNEL_ITEM,
        SND_CHANNEL_HEART,
        SND_CHANNEL_PDA,
        SND_CHANNEL_DEMONIC,
        SND_CHANNEL_RADIO,
        // internal use only.  not exposed to script or framecommands.
        SND_CHANNEL_AMBIENT,
        SND_CHANNEL_DAMAGE
    }
//    
// content masks
    public static final int MASK_ALL = (-1);
    public static final int MASK_SOLID = (CONTENTS_SOLID);
    public static final int MASK_MONSTERSOLID = (CONTENTS_SOLID | CONTENTS_MONSTERCLIP | CONTENTS_BODY);
    public static final int MASK_PLAYERSOLID = (CONTENTS_SOLID | CONTENTS_PLAYERCLIP | CONTENTS_BODY);
    public static final int MASK_DEADSOLID = (CONTENTS_SOLID | CONTENTS_PLAYERCLIP);
    public static final int MASK_WATER = (CONTENTS_WATER);
    public static final int MASK_OPAQUE = (CONTENTS_OPAQUE);
    public static final int MASK_SHOT_RENDERMODEL = (CONTENTS_SOLID | CONTENTS_RENDERMODEL);
    public static final int MASK_SHOT_BOUNDINGBOX = (CONTENTS_SOLID | CONTENTS_BODY);
//
    public static final float DEFAULT_GRAVITY = 1066.0f;
    public static final String DEFAULT_GRAVITY_STRING = "1066";
    public static final idVec3 DEFAULT_GRAVITY_VEC3 = new idVec3(0, 0, -DEFAULT_GRAVITY);
//
    public static final double CINEMATIC_SKIP_DELAY = SEC2MS(2.0f);
//============================================================================

    /*
     ===========
     GetGameAPI
     ============
     */
    public static gameExport_t GetGameAPI(gameImport_t gameImport) {

        if (gameImport.version == GAME_API_VERSION) {

            // set interface pointers used by the game
            sys_public.setSys(gameImport.sys);
            Common.setCommon(gameImport.common);
            CmdSystem.setCmdSystem(gameImport.cmdSystem);
            CVarSystem.setCvarSystem(gameImport.cvarSystem);
            FileSystem_h.setFileSystem(gameImport.fileSystem);//TODO:set both the fileSystem and the fileSystemLocal it's referencing.
            NetworkSystem.setNetworkSystem(gameImport.networkSystem);
            RenderSystem.setRenderSystem(gameImport.renderSystem);
            snd_system.setSoundSystem(gameImport.soundSystem);
            ModelManager.setRenderModelManager(gameImport.renderModelManager);
            UserInterface.setUiManager(gameImport.uiManager);
            DeclManager.setDeclManager(gameImport.declManager);
            neo.Tools.Compilers.AAS.AASFileManager.setAASFileManager(gameImport.AASFileManager);
            CollisionModel_local.setCollisionModelManager(gameImport.collisionModelManager);
        }

        // set interface pointers used by idLib
        idLib.sys = sys;
        idLib.common = common;
        idLib.cvarSystem = cvarSystem;
        idLib.fileSystem = fileSystem;

        // setup export interface
        gameExport.version = GAME_API_VERSION;
        gameExport.game = game;
        gameExport.gameEdit = GameEdit.gameEdit;

        return gameExport;
    }

    /*
     ===========
     TestGameAPI
     ============
     */
    static void TestGameAPI() {
        final gameImport_t testImport = new gameImport_t();
        gameExport_t testExport = new gameExport_t();

        testImport.sys = sys;
        testImport.common = common;
        testImport.cmdSystem = cmdSystem;
        testImport.cvarSystem = cvarSystem;
        testImport.fileSystem = fileSystem;
        testImport.networkSystem = networkSystem;
        testImport.renderSystem = renderSystem;
        testImport.soundSystem = snd_system.soundSystem;
        testImport.renderModelManager = renderModelManager;
        testImport.uiManager = uiManager;
        testImport.declManager = declManager;
        testImport.AASFileManager = AASFileManager;
        testImport.collisionModelManager = CollisionModel_local.collisionModelManager;

        testExport = GetGameAPI(testImport);
    }

    private static void memmove(byte[][][] dst, final int dstOffset, byte[][][] src, final int srcOffset, final int length) {
        int sa, sb, sc;
        int da, db, dc;

        sc = srcOffset % src.length;
        sb = (srcOffset - sc) / src.length;
        sa = (srcOffset - sc - (sb * src.length)) / src[0].length;

        dc = dstOffset % dst.length;
        db = (dstOffset - dc) / dst.length;
        da = (dstOffset - dc - (db * dst.length)) / dst[0].length;

        for (int count = 0; sa < src.length; sa++) {
            for (sb = 0; sb < src[0].length; sb++) {
                for (; (sc < src[0][0].length) && (count < length); sc++, count++) {
                    dst[da][db][dc++] = src[sa][sb][sc];

                    if (dc == dst[0][0].length) {
                        dc = 0;
                        if (++db == dst[0].length) {
                            db = 0;
                            da++;//if this overflows, then we're fucked!
                        }
                    }
                }
            }
        }
    }
}
