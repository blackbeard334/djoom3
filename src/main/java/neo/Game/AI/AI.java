package neo.Game.AI;

import neo.CM.CollisionModel.trace_s;
import neo.Game.AF.afTouch_s;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.AI.AAS.aasGoal_s;
import neo.Game.AI.AAS.aasObstacle_s;
import neo.Game.AI.AAS.aasPath_s;
import neo.Game.AI.AAS.idAAS;
import neo.Game.AI.AAS.idAASCallback;
import neo.Game.AI.AI_pathing.ballistics_s;
import neo.Game.AI.AI_pathing.obstacle_s;
import neo.Game.AI.AI_pathing.pathNode_s;
import neo.Game.AI.AI_pathing.pathTrace_s;
import neo.Game.Actor.idActor;
import neo.Game.Animation.Anim.animFlags_t;
import neo.Game.Animation.Anim.frameCommand_t;
import neo.Game.Animation.Anim_Blend.idAnim;
import neo.Game.Animation.Anim_Blend.idAnimator;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t3;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Misc.idPathCorner;
import neo.Game.Moveable.idMoveable;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Monster.idPhysics_Monster;
import neo.Game.Physics.Physics_Monster.monsterMoveResult_t;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idProjectile;
import neo.Game.Projectile.idSoulCubeMissile;
import neo.Game.Pvs.pvsHandle_t;
import neo.Game.Script.Script_Program.idScriptBool;
import neo.Game.Script.Script_Program.idScriptFloat;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.Tools.Compilers.AAS.AASFile.idAASSettings;
import neo.Tools.Compilers.AAS.AASFile.idReachability;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.DeclParticle.idDeclParticle;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;
import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import static neo.Game.AI.AI.moveCommand_t.MOVE_FACE_ENEMY;
import static neo.Game.AI.AI.moveCommand_t.MOVE_FACE_ENTITY;
import static neo.Game.AI.AI.moveCommand_t.MOVE_NONE;
import static neo.Game.AI.AI.moveCommand_t.MOVE_OUT_OF_RANGE;
import static neo.Game.AI.AI.moveCommand_t.MOVE_SLIDE_TO_POSITION;
import static neo.Game.AI.AI.moveCommand_t.MOVE_TO_ATTACK_POSITION;
import static neo.Game.AI.AI.moveCommand_t.MOVE_TO_COVER;
import static neo.Game.AI.AI.moveCommand_t.MOVE_TO_ENEMY;
import static neo.Game.AI.AI.moveCommand_t.MOVE_TO_ENEMYHEIGHT;
import static neo.Game.AI.AI.moveCommand_t.MOVE_TO_ENTITY;
import static neo.Game.AI.AI.moveCommand_t.MOVE_TO_POSITION;
import static neo.Game.AI.AI.moveCommand_t.MOVE_TO_POSITION_DIRECT;
import static neo.Game.AI.AI.moveCommand_t.MOVE_WANDER;
import static neo.Game.AI.AI.moveCommand_t.NUM_NONMOVING_COMMANDS;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_BLOCKED_BY_ENEMY;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_BLOCKED_BY_MONSTER;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_BLOCKED_BY_OBJECT;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_BLOCKED_BY_WALL;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_DEST_NOT_FOUND;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_DEST_UNREACHABLE;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_DONE;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_MOVING;
import static neo.Game.AI.AI.moveStatus_t.MOVE_STATUS_WAITING;
import static neo.Game.AI.AI.moveType_t.MOVETYPE_ANIM;
import static neo.Game.AI.AI.moveType_t.MOVETYPE_DEAD;
import static neo.Game.AI.AI.moveType_t.MOVETYPE_FLY;
import static neo.Game.AI.AI.moveType_t.MOVETYPE_SLIDE;
import static neo.Game.AI.AI.moveType_t.MOVETYPE_STATIC;
import static neo.Game.AI.AI.moveType_t.NUM_MOVETYPES;
import static neo.Game.AI.AI.talkState_t.NUM_TALK_STATES;
import static neo.Game.AI.AI.talkState_t.TALK_BUSY;
import static neo.Game.AI.AI.talkState_t.TALK_DEAD;
import static neo.Game.AI.AI.talkState_t.TALK_NEVER;
import static neo.Game.AI.AI.talkState_t.TALK_OK;
import static neo.Game.AI.AI_Events.AI_AllowDamage;
import static neo.Game.AI.AI_Events.AI_AllowHiddenMovement;
import static neo.Game.AI.AI_Events.AI_AllowMovement;
import static neo.Game.AI.AI_Events.AI_AnimTurn;
import static neo.Game.AI.AI_Events.AI_AttackMelee;
import static neo.Game.AI.AI_Events.AI_AttackMissile;
import static neo.Game.AI.AI_Events.AI_BecomeRagdoll;
import static neo.Game.AI.AI_Events.AI_BecomeSolid;
import static neo.Game.AI.AI_Events.AI_BeginAttack;
import static neo.Game.AI.AI_Events.AI_Burn;
import static neo.Game.AI.AI_Events.AI_CanBecomeSolid;
import static neo.Game.AI.AI_Events.AI_CanHitEnemy;
import static neo.Game.AI.AI_Events.AI_CanHitEnemyFromAnim;
import static neo.Game.AI.AI_Events.AI_CanHitEnemyFromJoint;
import static neo.Game.AI.AI_Events.AI_CanReachEnemy;
import static neo.Game.AI.AI_Events.AI_CanReachEntity;
import static neo.Game.AI.AI_Events.AI_CanReachPosition;
import static neo.Game.AI.AI_Events.AI_CanSeeEntity;
import static neo.Game.AI.AI_Events.AI_ChargeAttack;
import static neo.Game.AI.AI_Events.AI_ClearBurn;
import static neo.Game.AI.AI_Events.AI_ClearEnemy;
import static neo.Game.AI.AI_Events.AI_ClearFlyOffset;
import static neo.Game.AI.AI_Events.AI_ClosestReachableEnemyOfEntity;
import static neo.Game.AI.AI_Events.AI_CreateMissile;
import static neo.Game.AI.AI_Events.AI_DirectDamage;
import static neo.Game.AI.AI_Events.AI_DisableAFPush;
import static neo.Game.AI.AI_Events.AI_DisableClip;
import static neo.Game.AI.AI_Events.AI_DisableGravity;
import static neo.Game.AI.AI_Events.AI_EnableAFPush;
import static neo.Game.AI.AI_Events.AI_EnableClip;
import static neo.Game.AI.AI_Events.AI_EnableGravity;
import static neo.Game.AI.AI_Events.AI_EndAttack;
import static neo.Game.AI.AI_Events.AI_EnemyInCombatCone;
import static neo.Game.AI.AI_Events.AI_EnemyPositionValid;
import static neo.Game.AI.AI_Events.AI_EnemyRange;
import static neo.Game.AI.AI_Events.AI_EnemyRange2D;
import static neo.Game.AI.AI_Events.AI_EntityInAttackCone;
import static neo.Game.AI.AI_Events.AI_FaceEnemy;
import static neo.Game.AI.AI_Events.AI_FaceEntity;
import static neo.Game.AI.AI_Events.AI_FacingIdeal;
import static neo.Game.AI.AI_Events.AI_FindActorsInBounds;
import static neo.Game.AI.AI_Events.AI_FindEnemy;
import static neo.Game.AI.AI_Events.AI_FindEnemyAI;
import static neo.Game.AI.AI_Events.AI_FindEnemyInCombatNodes;
import static neo.Game.AI.AI_Events.AI_FireMissileAtTarget;
import static neo.Game.AI.AI_Events.AI_GetClosestHiddenTarget;
import static neo.Game.AI.AI_Events.AI_GetCombatNode;
import static neo.Game.AI.AI_Events.AI_GetCurrentYaw;
import static neo.Game.AI.AI_Events.AI_GetEnemy;
import static neo.Game.AI.AI_Events.AI_GetEnemyEyePos;
import static neo.Game.AI.AI_Events.AI_GetEnemyPos;
import static neo.Game.AI.AI_Events.AI_GetHealth;
import static neo.Game.AI.AI_Events.AI_GetJumpVelocity;
import static neo.Game.AI.AI_Events.AI_GetMoveType;
import static neo.Game.AI.AI_Events.AI_GetObstacle;
import static neo.Game.AI.AI_Events.AI_GetRandomTarget;
import static neo.Game.AI.AI_Events.AI_GetReachableEntityPosition;
import static neo.Game.AI.AI_Events.AI_GetTalkTarget;
import static neo.Game.AI.AI_Events.AI_GetTurnDelta;
import static neo.Game.AI.AI_Events.AI_GetTurnRate;
import static neo.Game.AI.AI_Events.AI_HeardSound;
import static neo.Game.AI.AI_Events.AI_IgnoreDamage;
import static neo.Game.AI.AI_Events.AI_JumpFrame;
import static neo.Game.AI.AI_Events.AI_KickObstacles;
import static neo.Game.AI.AI_Events.AI_Kill;
import static neo.Game.AI.AI_Events.AI_LaunchMissile;
import static neo.Game.AI.AI_Events.AI_LocateEnemy;
import static neo.Game.AI.AI_Events.AI_LookAtEnemy;
import static neo.Game.AI.AI_Events.AI_LookAtEntity;
import static neo.Game.AI.AI_Events.AI_MeleeAttackToJoint;
import static neo.Game.AI.AI_Events.AI_MoveOutOfRange;
import static neo.Game.AI.AI_Events.AI_MoveStatus;
import static neo.Game.AI.AI_Events.AI_MoveToAttackPosition;
import static neo.Game.AI.AI_Events.AI_MoveToCover;
import static neo.Game.AI.AI_Events.AI_MoveToEnemy;
import static neo.Game.AI.AI_Events.AI_MoveToEnemyHeight;
import static neo.Game.AI.AI_Events.AI_MoveToEntity;
import static neo.Game.AI.AI_Events.AI_MoveToPosition;
import static neo.Game.AI.AI_Events.AI_MuzzleFlash;
import static neo.Game.AI.AI_Events.AI_NumSmokeEmitters;
import static neo.Game.AI.AI_Events.AI_PreBurn;
import static neo.Game.AI.AI_Events.AI_PredictEnemyPos;
import static neo.Game.AI.AI_Events.AI_PushPointIntoAAS;
import static neo.Game.AI.AI_Events.AI_RadiusDamageFromJoint;
import static neo.Game.AI.AI_Events.AI_RandomPath;
import static neo.Game.AI.AI_Events.AI_RealKill;
import static neo.Game.AI.AI_Events.AI_RestoreMove;
import static neo.Game.AI.AI_Events.AI_SaveMove;
import static neo.Game.AI.AI_Events.AI_SetEnemy;
import static neo.Game.AI.AI_Events.AI_SetFlyOffset;
import static neo.Game.AI.AI_Events.AI_SetFlySpeed;
import static neo.Game.AI.AI_Events.AI_SetHealth;
import static neo.Game.AI.AI_Events.AI_SetJointMod;
import static neo.Game.AI.AI_Events.AI_SetMoveType;
import static neo.Game.AI.AI_Events.AI_SetSmokeVisibility;
import static neo.Game.AI.AI_Events.AI_SetTalkState;
import static neo.Game.AI.AI_Events.AI_SetTalkTarget;
import static neo.Game.AI.AI_Events.AI_SetTurnRate;
import static neo.Game.AI.AI_Events.AI_Shrivel;
import static neo.Game.AI.AI_Events.AI_SlideTo;
import static neo.Game.AI.AI_Events.AI_StopMove;
import static neo.Game.AI.AI_Events.AI_StopRagdoll;
import static neo.Game.AI.AI_Events.AI_StopThinking;
import static neo.Game.AI.AI_Events.AI_TestAnimAttack;
import static neo.Game.AI.AI_Events.AI_TestAnimMove;
import static neo.Game.AI.AI_Events.AI_TestAnimMoveTowardEnemy;
import static neo.Game.AI.AI_Events.AI_TestChargeAttack;
import static neo.Game.AI.AI_Events.AI_TestMeleeAttack;
import static neo.Game.AI.AI_Events.AI_TestMoveToPosition;
import static neo.Game.AI.AI_Events.AI_ThrowAF;
import static neo.Game.AI.AI_Events.AI_ThrowMoveable;
import static neo.Game.AI.AI_Events.AI_TravelDistanceBetweenEntities;
import static neo.Game.AI.AI_Events.AI_TravelDistanceBetweenPoints;
import static neo.Game.AI.AI_Events.AI_TravelDistanceToEntity;
import static neo.Game.AI.AI_Events.AI_TravelDistanceToPoint;
import static neo.Game.AI.AI_Events.AI_TriggerParticles;
import static neo.Game.AI.AI_Events.AI_TurnTo;
import static neo.Game.AI.AI_Events.AI_TurnToEntity;
import static neo.Game.AI.AI_Events.AI_TurnToPos;
import static neo.Game.AI.AI_Events.AI_WaitAction;
import static neo.Game.AI.AI_Events.AI_WaitMove;
import static neo.Game.AI.AI_Events.AI_WakeOnFlashlight;
import static neo.Game.AI.AI_Events.AI_Wander;
import static neo.Game.AI.AI_pathing.Ballistics;
import static neo.Game.AI.AI_pathing.BuildPathTree;
import static neo.Game.AI.AI_pathing.DrawPathTree;
import static neo.Game.AI.AI_pathing.FindOptimalPath;
import static neo.Game.AI.AI_pathing.FreePathTree_r;
import static neo.Game.AI.AI_pathing.GetObstacles;
import static neo.Game.AI.AI_pathing.GetPointOutsideObstacles;
import static neo.Game.AI.AI_pathing.MAX_FRAME_SLIDE;
import static neo.Game.AI.AI_pathing.MAX_OBSTACLES;
import static neo.Game.AI.AI_pathing.OVERCLIP;
import static neo.Game.AI.AI_pathing.PathTrace;
import static neo.Game.AI.AI_pathing.PrunePathTree;
import static neo.Game.AI.AI_pathing.pathNodeAllocator;
import static neo.Game.Actor.AI_PlayAnim;
import static neo.Game.Animation.Anim.ANIMCHANNEL_LEGS;
import static neo.Game.Animation.Anim.ANIMCHANNEL_TORSO;
import static neo.Game.Animation.Anim.FRAME2MS;
import static neo.Game.Animation.Anim.frameCommandType_t.FC_LAUNCHMISSILE;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD;
import static neo.Game.Animation.Anim.jointModTransform_t.JOINTMOD_WORLD_OVERRIDE;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_GetAngles;
import static neo.Game.Entity.EV_SetAngles;
import static neo.Game.Entity.EV_SetOwner;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEPARTICLES;
import static neo.Game.GameSys.Class.EV_Remove;
import static neo.Game.GameSys.SysCvar.ai_blockedFailSafe;
import static neo.Game.GameSys.SysCvar.ai_debugMove;
import static neo.Game.GameSys.SysCvar.ai_debugTrajectory;
import static neo.Game.GameSys.SysCvar.ai_showObstacleAvoidance;
import static neo.Game.GameSys.SysCvar.g_debugCinematic;
import static neo.Game.GameSys.SysCvar.g_debugDamage;
import static neo.Game.GameSys.SysCvar.g_gravity;
import static neo.Game.GameSys.SysCvar.g_monsters;
import static neo.Game.GameSys.SysCvar.g_muzzleFlash;
import static neo.Game.GameSys.SysCvar.g_skill;
import static neo.Game.Game_local.DEFAULT_GRAVITY_VEC3;
import static neo.Game.Game_local.MASK_MONSTERSOLID;
import static neo.Game.Game_local.MASK_OPAQUE;
import static neo.Game.Game_local.MASK_SHOT_BOUNDINGBOX;
import static neo.Game.Game_local.MASK_SHOT_RENDERMODEL;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_AMBIENT;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_DAMAGE;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_VOICE;
import static neo.Game.Moveable.EV_BecomeNonSolid;
import static neo.Game.Physics.Physics_Monster.monsterMoveResult_t.MM_BLOCKED;
import static neo.Game.Player.SAVING_THROW_TIME;
import static neo.Renderer.Material.CONTENTS_BODY;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_DIVERSITY;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_MD5_SKINSCALE;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMESCALE;
import static neo.Renderer.RenderWorld.SHADERPARM_TIME_OF_DEATH;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itob;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_FLY;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_WALK;
import static neo.Tools.Compilers.AAS.AASFile.TFL_AIR;
import static neo.Tools.Compilers.AAS.AASFile.TFL_FLY;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WALK;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Lib.MAX_WORLD_SIZE;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorLtGrey;
import static neo.idlib.Lib.colorMagenta;
import static neo.idlib.Lib.colorMdGrey;
import static neo.idlib.Lib.colorOrange;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Angles.getAng_zero;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.MS2SEC;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;

/**
 *
 */
public class AI {

    static final idEventDef EV_CombatNode_MarkUsed = new idEventDef("markUsed");

    /*
     ===============================================================================

     idAI

     ===============================================================================
     */
    static final float SQUARE_ROOT_OF_2   = 1.414213562f;
    static final float AI_TURN_PREDICTION = 0.2f;
    static final float AI_TURN_SCALE      = 60.0f;
    static final float AI_SEEK_PREDICTION = 0.3f;
    static final float AI_FLY_DAMPENING   = 0.15f;
    static final float AI_HEARING_RANGE   = 2048.0f;
    static final int   DEFAULT_FLY_OFFSET = 68;
    //
    static final int   ATTACK_IGNORE      = 0;
    static final int   ATTACK_ON_DAMAGE   = 1;
    static final int   ATTACK_ON_ACTIVATE = 2;
    static final int   ATTACK_ON_SIGHT    = 4;
    
    //
    // defined in script/ai_base.script.  please keep them up to date.
    public enum moveType_t {

        MOVETYPE_DEAD,
        MOVETYPE_ANIM,
        MOVETYPE_SLIDE,
        MOVETYPE_FLY,
        MOVETYPE_STATIC,
        NUM_MOVETYPES
    };

    public enum moveCommand_t {

        MOVE_NONE,
        MOVE_FACE_ENEMY,
        MOVE_FACE_ENTITY,
        //
        // commands < NUM_NONMOVING_COMMANDS don't cause a change in position
        NUM_NONMOVING_COMMANDS,
        //
        MOVE_TO_ENEMY,// = NUM_NONMOVING_COMMANDS,
        MOVE_TO_ENEMYHEIGHT,
        MOVE_TO_ENTITY,
        MOVE_OUT_OF_RANGE,
        MOVE_TO_ATTACK_POSITION,
        MOVE_TO_COVER,
        MOVE_TO_POSITION,
        MOVE_TO_POSITION_DIRECT,
        MOVE_SLIDE_TO_POSITION,
        MOVE_WANDER,
        NUM_MOVE_COMMANDS
    };

    public enum talkState_t {

        TALK_NEVER,
        TALK_DEAD,
        TALK_OK,
        TALK_BUSY,
        NUM_TALK_STATES
    };

    //
    // status results from move commands
    // make sure to change script/doom_defs.script if you add any, or change their order
    //
    public enum moveStatus_t {

        MOVE_STATUS_DONE,
        MOVE_STATUS_MOVING,
        MOVE_STATUS_WAITING,
        MOVE_STATUS_DEST_NOT_FOUND,
        MOVE_STATUS_DEST_UNREACHABLE,
        MOVE_STATUS_BLOCKED_BY_WALL,
        MOVE_STATUS_BLOCKED_BY_OBJECT,
        MOVE_STATUS_BLOCKED_BY_ENEMY,
        MOVE_STATUS_BLOCKED_BY_MONSTER
    };

    static final int DI_NODIR = -1;

    // obstacle avoidance
    public static class obstaclePath_s {

        idVec3   seekPos;                   // seek position avoiding obstacles
        idEntity firstObstacle;             // if != NULL the first obstacle along the path
        idVec3   startPosOutsideObstacles;  // start position outside obstacles
        idEntity startPosObstacle;          // if != NULL the obstacle containing the start position
        idVec3   seekPosOutsideObstacles;   // seek position outside obstacles
        idEntity seekPosObstacle;           // if != NULL the obstacle containing the seek position
    };

    // path prediction
// typedef enum {
    public static final int SE_BLOCKED          = BIT(0);
    public static final int SE_ENTER_LEDGE_AREA = BIT(1);
    public static final int SE_ENTER_OBSTACLE   = BIT(2);
    public static final int SE_FALL             = BIT(3);
    public static final int SE_LAND             = BIT(4);
// } stopEvent_t;

    public static class predictedPath_s {

        idVec3 endPos;                      // final position
        idVec3 endVelocity;                 // velocity at end position
        idVec3 endNormal;                   // normal of blocking surface
        int    endTime;                     // time predicted
        int    endEvent;                    // event that stopped the prediction
        static idEntity blockingEntity;     // entity that blocks the movement
    };


    static class particleEmitter_s {

        particleEmitter_s() {
            particle = null;
            time = 0;
            joint = INVALID_JOINT;
        }

        idDeclParticle particle;
        int            time;
        int/*jointHandle_t*/ joint;
    };

    static final String[] moveCommandString/*[ NUM_MOVE_COMMANDS ]*/ = {
            "MOVE_NONE",
            "MOVE_FACE_ENEMY",
            "MOVE_FACE_ENTITY",
            "MOVE_TO_ENEMY",
            "MOVE_TO_ENEMYHEIGHT",
            "MOVE_TO_ENTITY",
            "MOVE_OUT_OF_RANGE",
            "MOVE_TO_ATTACK_POSITION",
            "MOVE_TO_COVER",
            "MOVE_TO_POSITION",
            "MOVE_TO_POSITION_DIRECT",
            "MOVE_SLIDE_TO_POSITION",
            "MOVE_WANDER"
    };

    public static class idMoveState {

        public moveType_t            moveType;
        public moveCommand_t         moveCommand;
        public moveStatus_t          moveStatus;
        public idVec3                moveDest;
        public idVec3                moveDir;           // used for wandering and slide moves
        public idEntityPtr<idEntity> goalEntity;
        public idVec3                goalEntityOrigin;  // move to entity uses this to avoid checking the floor position every frame
        public int                   toAreaNum;
        public int                   startTime;
        public int                   duration;
        public float                 speed;             // only used by flying creatures
        public float                 range;
        public float                 wanderYaw;
        public int                   nextWanderTime;
        public int                   blockTime;
        public idEntityPtr<idEntity> obstacle;
        public idVec3                lastMoveOrigin;
        public int                   lastMoveTime;
        public int                   anim;
        //
        //

        public idMoveState() {
            moveType = MOVETYPE_ANIM;
            moveCommand = MOVE_NONE;
            moveStatus = MOVE_STATUS_DONE;
            moveDest = new idVec3();
            moveDir = new idVec3(1.0f, 0.0f, 0.0f);
            goalEntity = null;
            goalEntityOrigin = new idVec3();
            toAreaNum = 0;
            startTime = 0;
            duration = 0;
            speed = 0.0f;
            range = 0.0f;
            wanderYaw = 0;
            nextWanderTime = 0;
            blockTime = 0;
            obstacle = null;
            lastMoveOrigin = getVec3_origin();
            lastMoveTime = 0;
            anim = 0;
        }

        public void Save(idSaveGame savefile) {
            savefile.WriteInt(moveType.ordinal());
            savefile.WriteInt(moveCommand.ordinal());
            savefile.WriteInt(moveStatus.ordinal());
            savefile.WriteVec3(moveDest);
            savefile.WriteVec3(moveDir);
            goalEntity.Save(savefile);
            savefile.WriteVec3(goalEntityOrigin);
            savefile.WriteInt(toAreaNum);
            savefile.WriteInt(startTime);
            savefile.WriteInt(duration);
            savefile.WriteFloat(speed);
            savefile.WriteFloat(range);
            savefile.WriteFloat(wanderYaw);
            savefile.WriteInt(nextWanderTime);
            savefile.WriteInt(blockTime);
            obstacle.Save(savefile);
            savefile.WriteVec3(lastMoveOrigin);
            savefile.WriteInt(lastMoveTime);
            savefile.WriteInt(anim);
        }

        public void Restore(idRestoreGame savefile) {
            moveType = moveType_t.values()[savefile.ReadInt()];
            moveCommand = moveCommand_t.values()[savefile.ReadInt()];
            moveStatus = moveStatus_t.values()[savefile.ReadInt()];
            savefile.ReadVec3(moveDest);
            savefile.ReadVec3(moveDir);
            goalEntity.Restore(savefile);
            savefile.ReadVec3(goalEntityOrigin);
            toAreaNum = savefile.ReadInt();
            startTime = savefile.ReadInt();
            duration = savefile.ReadInt();
            speed = savefile.ReadFloat();
            range = savefile.ReadFloat();
            wanderYaw = savefile.ReadFloat();
            nextWanderTime = savefile.ReadInt();
            blockTime = savefile.ReadInt();
            obstacle.Restore(savefile);
            savefile.ReadVec3(lastMoveOrigin);
            lastMoveTime = savefile.ReadInt();
            anim = savefile.ReadInt();
        }
    };

    public static class idAASFindCover extends idAASCallback {

        private pvsHandle_t hidePVS;
        private int[] PVSAreas = new int[idEntity.MAX_PVS_AREAS];
        //
        //

        public idAASFindCover(final idVec3 hideFromPos) {
            int numPVSAreas;
            idBounds bounds = new idBounds(hideFromPos.oMinus(new idVec3(16, 16, 0)), hideFromPos.oPlus(new idVec3(16, 16, 64)));

            // setup PVS
            numPVSAreas = gameLocal.pvs.GetPVSAreas(bounds, PVSAreas, idEntity.MAX_PVS_AREAS);
            hidePVS = gameLocal.pvs.SetupCurrentPVS(PVSAreas, numPVSAreas);
        }

        // ~idAASFindCover();
        @Override
        public boolean TestArea(final idAAS aas, int areaNum) {
            idVec3 areaCenter;
            int numPVSAreas;
            int[] PVSAreas = new int[idEntity.MAX_PVS_AREAS];

            areaCenter = aas.AreaCenter(areaNum);
            areaCenter.oPluSet(2, 1.0f);

            numPVSAreas = gameLocal.pvs.GetPVSAreas(new idBounds(areaCenter).Expand(16.0f), PVSAreas, idEntity.MAX_PVS_AREAS);
            if (!gameLocal.pvs.InCurrentPVS(hidePVS, PVSAreas, numPVSAreas)) {
                return true;
            }

            return false;
        }
    };

    public static class idAASFindAreaOutOfRange extends idAASCallback {

        private idVec3 targetPos;
        private float  maxDistSqr;
        //
        //

        public idAASFindAreaOutOfRange(final idVec3 targetPos, float maxDist) {
            this.targetPos = targetPos;
            this.maxDistSqr = maxDist * maxDist;
        }

        @Override
        public boolean TestArea(final idAAS aas, int areaNum) {
            final idVec3 areaCenter = aas.AreaCenter(areaNum);
            trace_s[] trace = {null};
            float dist;

            dist = (targetPos.ToVec2().oMinus(areaCenter.ToVec2())).LengthSqr();

            if ((maxDistSqr > 0.0f) && (dist < maxDistSqr)) {
                return false;
            }

            gameLocal.clip.TracePoint(trace, targetPos, areaCenter.oPlus(new idVec3(0.0f, 0.0f, 1.0f)), MASK_OPAQUE, null);
            if (trace[0].fraction < 1.0f) {
                return false;
            }

            return true;
        }
    };

    public static class idAASFindAttackPosition extends idAASCallback {

        private idAI        self;
        private idEntity    target;
        private idBounds    excludeBounds;
        private idVec3      targetPos;
        private idVec3      fireOffset;
        private idMat3      gravityAxis;
        private pvsHandle_t targetPVS;
        private int[] PVSAreas = new int[idEntity.MAX_PVS_AREAS];
        //
        //

        public idAASFindAttackPosition(final idAI self, final idMat3 gravityAxis, idEntity target, final idVec3 targetPos, final idVec3 fireOffset) {
            int numPVSAreas;

            this.target = target;
            this.targetPos = targetPos;
            this.fireOffset = fireOffset;
            this.self = self;
            this.gravityAxis = gravityAxis;

            excludeBounds = new idBounds(new idVec3(-64.0f, -64.0f, -8.0f), new idVec3(64.0f, 64.0f, 64.0f));
            excludeBounds.TranslateSelf(self.GetPhysics().GetOrigin());

            // setup PVS
            idBounds bounds = new idBounds(targetPos.oMinus(new idVec3(16, 16, 0)), targetPos.oPlus(new idVec3(16, 16, 64)));
            numPVSAreas = gameLocal.pvs.GetPVSAreas(bounds, PVSAreas, idEntity.MAX_PVS_AREAS);
            targetPVS = gameLocal.pvs.SetupCurrentPVS(PVSAreas, numPVSAreas);
        }
        // ~idAASFindAttackPosition();

        @Override
        public boolean TestArea(final idAAS aas, int areaNum) {
            idVec3 dir;
            idVec3 local_dir = new idVec3();
            idVec3 fromPos;
            idMat3 axis;
            idVec3 areaCenter;
            int numPVSAreas;
            int[] PVSAreas = new int[idEntity.MAX_PVS_AREAS];

            areaCenter = aas.AreaCenter(areaNum);
            areaCenter.oPluSet(2, 1.0f);

            if (excludeBounds.ContainsPoint(areaCenter)) {
                // too close to where we already are
                return false;
            }

            numPVSAreas = gameLocal.pvs.GetPVSAreas(new idBounds(areaCenter).Expand(16.0f), PVSAreas, idEntity.MAX_PVS_AREAS);
            if (!gameLocal.pvs.InCurrentPVS(targetPVS, PVSAreas, numPVSAreas)) {
                return false;
            }

            // calculate the world transform of the launch position
            dir = targetPos.oMinus(areaCenter);
            gravityAxis.ProjectVector(dir, local_dir);
            local_dir.z = 0.0f;
            local_dir.ToVec2().Normalize();
            axis = local_dir.ToMat3();
            fromPos = areaCenter.oPlus(fireOffset.oMultiply(axis));

            return self.GetAimDir(fromPos, target, self, dir);
        }
    };

    public static class idAI extends idActor {
        // CLASS_PROTOTYPE( idAI );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idActor.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idAI>) idAI::Event_Activate);
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idAI>) idAI::Event_Touch);
            eventCallbacks.put(AI_FindEnemy, (eventCallback_t1<idAI>) idAI::Event_FindEnemy);
            eventCallbacks.put(AI_FindEnemyAI, (eventCallback_t1<idAI>) idAI::Event_FindEnemyAI);
            eventCallbacks.put(AI_FindEnemyInCombatNodes, (eventCallback_t0<idAI>) idAI::Event_FindEnemyInCombatNodes);
            eventCallbacks.put(AI_ClosestReachableEnemyOfEntity, (eventCallback_t1<idAI>) idAI::Event_ClosestReachableEnemyOfEntity);
            eventCallbacks.put(AI_HeardSound, (eventCallback_t1<idAI>) idAI::Event_HeardSound);
            eventCallbacks.put(AI_SetEnemy, (eventCallback_t1<idAI>) idAI::Event_SetEnemy);
            eventCallbacks.put(AI_ClearEnemy, (eventCallback_t0<idAI>) idAI::Event_ClearEnemy);
            eventCallbacks.put(AI_MuzzleFlash, (eventCallback_t1<idAI>) idAI::Event_MuzzleFlash);
            eventCallbacks.put(AI_CreateMissile, (eventCallback_t1<idAI>) idAI::Event_CreateMissile);
            eventCallbacks.put(AI_AttackMissile, (eventCallback_t1<idAI>) idAI::Event_AttackMissile);
            eventCallbacks.put(AI_FireMissileAtTarget, (eventCallback_t2<idAI>) idAI::Event_FireMissileAtTarget);
            eventCallbacks.put(AI_LaunchMissile, (eventCallback_t2<idAI>) idAI::Event_LaunchMissile);
            eventCallbacks.put(AI_AttackMelee, (eventCallback_t1<idAI>) idAI::Event_AttackMelee);
            eventCallbacks.put(AI_DirectDamage, (eventCallback_t2<idAI>) idAI::Event_DirectDamage);
            eventCallbacks.put(AI_RadiusDamageFromJoint, (eventCallback_t2<idAI>) idAI::Event_RadiusDamageFromJoint);
            eventCallbacks.put(AI_BeginAttack, (eventCallback_t1<idAI>) idAI::Event_BeginAttack);
            eventCallbacks.put(AI_EndAttack, (eventCallback_t0<idAI>) idAI::Event_EndAttack);
            eventCallbacks.put(AI_MeleeAttackToJoint, (eventCallback_t2<idAI>) idAI::Event_MeleeAttackToJoint);
            eventCallbacks.put(AI_RandomPath, (eventCallback_t0<idAI>) idAI::Event_RandomPath);
            eventCallbacks.put(AI_CanBecomeSolid, (eventCallback_t0<idAI>) idAI::Event_CanBecomeSolid);
            eventCallbacks.put(AI_BecomeSolid, (eventCallback_t0<idAI>) idAI::Event_BecomeSolid);
            eventCallbacks.put(EV_BecomeNonSolid, (eventCallback_t0<idAI>) idAI::Event_BecomeNonSolid);
            eventCallbacks.put(AI_BecomeRagdoll, (eventCallback_t0<idAI>) idAI::Event_BecomeRagdoll);
            eventCallbacks.put(AI_StopRagdoll, (eventCallback_t0<idAI>) idAI::Event_StopRagdoll);
            eventCallbacks.put(AI_SetHealth, (eventCallback_t1<idAI>) idAI::Event_SetHealth);
            eventCallbacks.put(AI_GetHealth, (eventCallback_t0<idAI>) idAI::Event_GetHealth);
            eventCallbacks.put(AI_AllowDamage, (eventCallback_t0<idAI>) idAI::Event_AllowDamage);
            eventCallbacks.put(AI_IgnoreDamage, (eventCallback_t0<idAI>) idAI::Event_IgnoreDamage);
            eventCallbacks.put(AI_GetCurrentYaw, (eventCallback_t0<idAI>) idAI::Event_GetCurrentYaw);
            eventCallbacks.put(AI_TurnTo, (eventCallback_t1<idAI>) idAI::Event_TurnTo);
            eventCallbacks.put(AI_TurnToPos, (eventCallback_t1<idAI>) idAI::Event_TurnToPos);
            eventCallbacks.put(AI_TurnToEntity, (eventCallback_t1<idAI>) idAI::Event_TurnToEntity);
            eventCallbacks.put(AI_MoveStatus, (eventCallback_t0<idAI>) idAI::Event_MoveStatus);
            eventCallbacks.put(AI_StopMove, (eventCallback_t0<idAI>) idAI::Event_StopMove);
            eventCallbacks.put(AI_MoveToCover, (eventCallback_t0<idAI>) idAI::Event_MoveToCover);
            eventCallbacks.put(AI_MoveToEnemy, (eventCallback_t0<idAI>) idAI::Event_MoveToEnemy);
            eventCallbacks.put(AI_MoveToEnemyHeight, (eventCallback_t0<idAI>) idAI::Event_MoveToEnemyHeight);
            eventCallbacks.put(AI_MoveOutOfRange, (eventCallback_t2<idAI>) idAI::Event_MoveOutOfRange);
            eventCallbacks.put(AI_MoveToAttackPosition, (eventCallback_t2<idAI>) idAI::Event_MoveToAttackPosition);
            eventCallbacks.put(AI_Wander, (eventCallback_t0<idAI>) idAI::Event_Wander);
            eventCallbacks.put(AI_MoveToEntity, (eventCallback_t1<idAI>) idAI::Event_MoveToEntity);
            eventCallbacks.put(AI_MoveToPosition, (eventCallback_t1<idAI>) idAI::Event_MoveToPosition);
            eventCallbacks.put(AI_SlideTo, (eventCallback_t2<idAI>) idAI::Event_SlideTo);
            eventCallbacks.put(AI_FacingIdeal, (eventCallback_t0<idAI>) idAI::Event_FacingIdeal);
            eventCallbacks.put(AI_FaceEnemy, (eventCallback_t0<idAI>) idAI::Event_FaceEnemy);
            eventCallbacks.put(AI_FaceEntity, (eventCallback_t1<idAI>) idAI::Event_FaceEntity);
            eventCallbacks.put(AI_WaitAction, (eventCallback_t1<idAI>) idAI::Event_WaitAction);
            eventCallbacks.put(AI_GetCombatNode, (eventCallback_t0<idAI>) idAI::Event_GetCombatNode);
            eventCallbacks.put(AI_EnemyInCombatCone, (eventCallback_t2<idAI>) idAI::Event_EnemyInCombatCone);
            eventCallbacks.put(AI_WaitMove, (eventCallback_t0<idAI>) idAI::Event_WaitMove);
            eventCallbacks.put(AI_GetJumpVelocity, (eventCallback_t3<idAI>) idAI::Event_GetJumpVelocity);
            eventCallbacks.put(AI_EntityInAttackCone, (eventCallback_t1<idAI>) idAI::Event_EntityInAttackCone);
            eventCallbacks.put(AI_CanSeeEntity, (eventCallback_t1<idAI>) idAI::Event_CanSeeEntity);
            eventCallbacks.put(AI_SetTalkTarget, (eventCallback_t1<idAI>) idAI::Event_SetTalkTarget);
            eventCallbacks.put(AI_GetTalkTarget, (eventCallback_t0<idAI>) idAI::Event_GetTalkTarget);
            eventCallbacks.put(AI_SetTalkState, (eventCallback_t1<idAI>) idAI::Event_SetTalkState);
            eventCallbacks.put(AI_EnemyRange, (eventCallback_t0<idAI>) idAI::Event_EnemyRange);
            eventCallbacks.put(AI_EnemyRange2D, (eventCallback_t0<idAI>) idAI::Event_EnemyRange2D);
            eventCallbacks.put(AI_GetEnemy, (eventCallback_t0<idAI>) idAI::Event_GetEnemy);
            eventCallbacks.put(AI_GetEnemyPos, (eventCallback_t0<idAI>) idAI::Event_GetEnemyPos);
            eventCallbacks.put(AI_GetEnemyEyePos, (eventCallback_t0<idAI>) idAI::Event_GetEnemyEyePos);
            eventCallbacks.put(AI_PredictEnemyPos, (eventCallback_t1<idAI>) idAI::Event_PredictEnemyPos);
            eventCallbacks.put(AI_CanHitEnemy, (eventCallback_t0<idAI>) idAI::Event_CanHitEnemy);
            eventCallbacks.put(AI_CanHitEnemyFromAnim, (eventCallback_t1<idAI>) idAI::Event_CanHitEnemyFromAnim);
            eventCallbacks.put(AI_CanHitEnemyFromJoint, (eventCallback_t1<idAI>) idAI::Event_CanHitEnemyFromJoint);
            eventCallbacks.put(AI_EnemyPositionValid, (eventCallback_t0<idAI>) idAI::Event_EnemyPositionValid);
            eventCallbacks.put(AI_ChargeAttack, (eventCallback_t1<idAI>) idAI::Event_ChargeAttack);
            eventCallbacks.put(AI_TestChargeAttack, (eventCallback_t0<idAI>) idAI::Event_TestChargeAttack);
            eventCallbacks.put(AI_TestAnimMoveTowardEnemy, (eventCallback_t1<idAI>) idAI::Event_TestAnimMoveTowardEnemy);
            eventCallbacks.put(AI_TestAnimMove, (eventCallback_t1<idAI>) idAI::Event_TestAnimMove);
            eventCallbacks.put(AI_TestMoveToPosition, (eventCallback_t1<idAI>) idAI::Event_TestMoveToPosition);
            eventCallbacks.put(AI_TestMeleeAttack, (eventCallback_t0<idAI>) idAI::Event_TestMeleeAttack);
            eventCallbacks.put(AI_TestAnimAttack, (eventCallback_t1<idAI>) idAI::Event_TestAnimAttack);
            eventCallbacks.put(AI_Shrivel, (eventCallback_t1<idAI>) idAI::Event_Shrivel);
            eventCallbacks.put(AI_Burn, (eventCallback_t0<idAI>) idAI::Event_Burn);
            eventCallbacks.put(AI_PreBurn, (eventCallback_t0<idAI>) idAI::Event_PreBurn);
            eventCallbacks.put(AI_SetSmokeVisibility, (eventCallback_t2<idAI>) idAI::Event_SetSmokeVisibility);
            eventCallbacks.put(AI_NumSmokeEmitters, (eventCallback_t0<idAI>) idAI::Event_NumSmokeEmitters);
            eventCallbacks.put(AI_ClearBurn, (eventCallback_t0<idAI>) idAI::Event_ClearBurn);
            eventCallbacks.put(AI_StopThinking, (eventCallback_t0<idAI>) idAI::Event_StopThinking);
            eventCallbacks.put(AI_GetTurnDelta, (eventCallback_t0<idAI>) idAI::Event_GetTurnDelta);
            eventCallbacks.put(AI_GetMoveType, (eventCallback_t0<idAI>) idAI::Event_GetMoveType);
            eventCallbacks.put(AI_SetMoveType, (eventCallback_t1<idAI>) idAI::Event_SetMoveType);
            eventCallbacks.put(AI_SaveMove, (eventCallback_t0<idAI>) idAI::Event_SaveMove);
            eventCallbacks.put(AI_RestoreMove, (eventCallback_t0<idAI>) idAI::Event_RestoreMove);
            eventCallbacks.put(AI_AllowMovement, (eventCallback_t1<idAI>) idAI::Event_AllowMovement);
            eventCallbacks.put(AI_JumpFrame, (eventCallback_t0<idAI>) idAI::Event_JumpFrame);
            eventCallbacks.put(AI_EnableClip, (eventCallback_t0<idAI>) idAI::Event_EnableClip);
            eventCallbacks.put(AI_DisableClip, (eventCallback_t0<idAI>) idAI::Event_DisableClip);
            eventCallbacks.put(AI_EnableGravity, (eventCallback_t0<idAI>) idAI::Event_EnableGravity);
            eventCallbacks.put(AI_DisableGravity, (eventCallback_t0<idAI>) idAI::Event_DisableGravity);
            eventCallbacks.put(AI_EnableAFPush, (eventCallback_t0<idAI>) idAI::Event_EnableAFPush);
            eventCallbacks.put(AI_DisableAFPush, (eventCallback_t0<idAI>) idAI::Event_DisableAFPush);
            eventCallbacks.put(AI_SetFlySpeed, (eventCallback_t1<idAI>) idAI::Event_SetFlySpeed);
            eventCallbacks.put(AI_SetFlyOffset, (eventCallback_t1<idAI>) idAI::Event_SetFlyOffset);
            eventCallbacks.put(AI_ClearFlyOffset, (eventCallback_t0<idAI>) idAI::Event_ClearFlyOffset);
            eventCallbacks.put(AI_GetClosestHiddenTarget, (eventCallback_t1<idAI>) idAI::Event_GetClosestHiddenTarget);
            eventCallbacks.put(AI_GetRandomTarget, (eventCallback_t1<idAI>) idAI::Event_GetRandomTarget);
            eventCallbacks.put(AI_TravelDistanceToPoint, (eventCallback_t1<idAI>) idAI::Event_TravelDistanceToPoint);
            eventCallbacks.put(AI_TravelDistanceToEntity, (eventCallback_t1<idAI>) idAI::Event_TravelDistanceToEntity);
            eventCallbacks.put(AI_TravelDistanceBetweenPoints, (eventCallback_t2<idAI>) idAI::Event_TravelDistanceBetweenPoints);
            eventCallbacks.put(AI_TravelDistanceBetweenEntities, (eventCallback_t2<idAI>) idAI::Event_TravelDistanceBetweenEntities);
            eventCallbacks.put(AI_LookAtEntity, (eventCallback_t2<idAI>) idAI::Event_LookAtEntity);
            eventCallbacks.put(AI_LookAtEnemy, (eventCallback_t1<idAI>) idAI::Event_LookAtEnemy);
            eventCallbacks.put(AI_SetJointMod, (eventCallback_t1<idAI>) idAI::Event_SetJointMod);
            eventCallbacks.put(AI_ThrowMoveable, (eventCallback_t0<idAI>) idAI::Event_ThrowMoveable);
            eventCallbacks.put(AI_ThrowAF, (eventCallback_t0<idAI>) idAI::Event_ThrowAF);
            eventCallbacks.put(EV_GetAngles, (eventCallback_t0<idAI>) idAI::Event_GetAngles);
            eventCallbacks.put(EV_SetAngles, (eventCallback_t1<idAI>) idAI::Event_SetAngles);
            eventCallbacks.put(AI_RealKill, (eventCallback_t0<idAI>) idAI::Event_RealKill);
            eventCallbacks.put(AI_Kill, (eventCallback_t0<idAI>) idAI::Event_Kill);
            eventCallbacks.put(AI_WakeOnFlashlight, (eventCallback_t1<idAI>) idAI::Event_WakeOnFlashlight);
            eventCallbacks.put(AI_LocateEnemy, (eventCallback_t0<idAI>) idAI::Event_LocateEnemy);
            eventCallbacks.put(AI_KickObstacles, (eventCallback_t2<idAI>) idAI::Event_KickObstacles);
            eventCallbacks.put(AI_GetObstacle, (eventCallback_t0<idAI>) idAI::Event_GetObstacle);
            eventCallbacks.put(AI_PushPointIntoAAS, (eventCallback_t1<idAI>) idAI::Event_PushPointIntoAAS);
            eventCallbacks.put(AI_GetTurnRate, (eventCallback_t0<idAI>) idAI::Event_GetTurnRate);
            eventCallbacks.put(AI_SetTurnRate, (eventCallback_t1<idAI>) idAI::Event_SetTurnRate);
            eventCallbacks.put(AI_AnimTurn, (eventCallback_t1<idAI>) idAI::Event_AnimTurn);
            eventCallbacks.put(AI_AllowHiddenMovement, (eventCallback_t1<idAI>) idAI::Event_AllowHiddenMovement);
            eventCallbacks.put(AI_TriggerParticles, (eventCallback_t1<idAI>) idAI::Event_TriggerParticles);
            eventCallbacks.put(AI_FindActorsInBounds, (eventCallback_t2<idAI>) idAI::Event_FindActorsInBounds);
            eventCallbacks.put(AI_CanReachPosition, (eventCallback_t1<idAI>) idAI::Event_CanReachPosition);
            eventCallbacks.put(AI_CanReachEntity, (eventCallback_t1<idAI>) idAI::Event_CanReachEntity);
            eventCallbacks.put(AI_CanReachEnemy, (eventCallback_t0<idAI>) idAI::Event_CanReachEnemy);
            eventCallbacks.put(AI_GetReachableEntityPosition, (eventCallback_t1<idAI>) idAI::Event_GetReachableEntityPosition);
        }

        // navigation
        protected       idAAS                            aas;
        protected       int                              travelFlags;
        //
        protected       idMoveState                      move;
        protected       idMoveState                      savedMove;
        //
        protected       float                            kickForce;
        protected       boolean                          ignore_obstacles;
        protected       float                            blockedRadius;
        protected       int                              blockedMoveTime;
        protected       int                              blockedAttackTime;
        //
        // turning
        protected       float                            ideal_yaw;
        protected       float                            current_yaw;
        protected       float                            turnRate;
        protected       float                            turnVel;
        protected       float                            anim_turn_yaw;
        protected       float                            anim_turn_amount;
        protected       float                            anim_turn_angles;
        //
        // physics
        protected       idPhysics_Monster                physicsObj;
        //
        // flying
        protected       int/*jointHandle_t*/             flyTiltJoint;
        protected       float                            fly_speed;
        protected       float                            fly_bob_strength;
        protected       float                            fly_bob_vert;
        protected       float                            fly_bob_horz;
        protected       int                              fly_offset;               // prefered offset from player's view
        protected       float                            fly_seek_scale;
        protected       float                            fly_roll_scale;
        protected       float                            fly_roll_max;
        protected       float                            fly_roll;
        protected       float                            fly_pitch_scale;
        protected       float                            fly_pitch_max;
        protected       float                            fly_pitch;
        //
        protected       boolean                          allowMove;                // disables any animation movement
        protected       boolean                          allowHiddenMovement;      // allows character to still move around while hidden
        protected       boolean                          disableGravity;           // disables gravity and allows vertical movement by the animation
        protected       boolean                          af_push_moveables;        // allow the articulated figure to push moveable objects
        //
        // weapon/attack vars
        protected       boolean                          lastHitCheckResult;
        protected       int                              lastHitCheckTime;
        protected       int                              lastAttackTime;
        protected       float                            melee_range;
        protected       float                            projectile_height_to_distance_ratio;    // calculates the maximum height a projectile can be thrown
        protected       idList<idVec3>                   missileLaunchOffset;
        //
        protected       idDict                           projectileDef;
        protected       idClipModel                      projectileClipModel;
        protected       float                            projectileRadius;
        protected       float                            projectileSpeed;
        protected       idVec3                           projectileVelocity;
        protected       idVec3                           projectileGravity;
        protected       idEntityPtr<idProjectile>        projectile;
        protected       idStr                            attack;
        //
        // chatter/talking
        protected       idSoundShader                    chat_snd;
        protected       int                              chat_min;
        protected       int                              chat_max;
        protected       int                              chat_time;
        protected       talkState_t                      talk_state;
        protected       idEntityPtr<idActor>             talkTarget;
        //
        // cinematics
        protected       int                              num_cinematics;
        protected       int                              current_cinematic;
        //
        protected       boolean                          allowJointMod;
        protected       idEntityPtr<idEntity>            focusEntity;
        protected       idVec3                           currentFocusPos;
        protected       int                              focusTime;
        protected       int                              alignHeadTime;
        protected       int                              forceAlignHeadTime;
        protected       idAngles                         eyeAng;
        protected       idAngles                         lookAng;
        protected       idAngles                         destLookAng;
        protected       idAngles                         lookMin;
        protected       idAngles                         lookMax;
        protected       idList<Integer/*jointHandle_t*/> lookJoints;
        protected       idList<idAngles>                 lookJointAngles;
        protected       float                            eyeVerticalOffset;
        protected       float                            eyeHorizontalOffset;
        protected       float                            eyeFocusRate;
        protected       float                            headFocusRate;
        protected       int                              focusAlignTime;
        //
        // special fx
        protected       float                            shrivel_rate;
        protected       int                              shrivel_start;
        //
        protected       boolean                          restartParticles;         // should smoke emissions restart
        protected       boolean                          useBoneAxis;              // use the bone vs the model axis
        protected       idList<particleEmitter_s>        particles;                // particle data
        //
        protected       renderLight_s                    worldMuzzleFlash;         // positioned on world weapon bone
        protected       int                              worldMuzzleFlashHandle;
        protected       int/*jointHandle_t*/             flashJointWorld;
        protected       int                              muzzleFlashEnd;
        protected       int                              flashTime;
        //
        // joint controllers
        protected       idAngles                         eyeMin;
        protected       idAngles                         eyeMax;
        protected       int/*jointHandle_t*/             focusJoint;
        protected       int/*jointHandle_t*/             orientationJoint;
        //
        // enemy variables
        protected       idEntityPtr<idActor>             enemy;
        protected       idVec3                           lastVisibleEnemyPos;
        protected       idVec3                           lastVisibleEnemyEyeOffset;
        protected       idVec3                           lastVisibleReachableEnemyPos;
        protected       idVec3                           lastReachableEnemyPos;
        protected       boolean                          wakeOnFlashlight;
        //
        // script variables
        protected final idScriptBool                     AI_TALK;
        protected final idScriptBool                     AI_DAMAGE;
        protected final idScriptBool                     AI_PAIN;
        protected final idScriptFloat                    AI_SPECIAL_DAMAGE;
        protected final idScriptBool                     AI_DEAD;
        protected final idScriptBool                     AI_ENEMY_VISIBLE;
        protected final idScriptBool                     AI_ENEMY_IN_FOV;
        protected final idScriptBool                     AI_ENEMY_DEAD;
        protected final idScriptBool                     AI_MOVE_DONE;
        protected final idScriptBool                     AI_ONGROUND;
        protected final idScriptBool                     AI_ACTIVATED;
        protected final idScriptBool                     AI_FORWARD;
        protected final idScriptBool                     AI_JUMP;
        protected final idScriptBool                     AI_ENEMY_REACHABLE;
        protected final idScriptBool                     AI_BLOCKED;
        protected final idScriptBool                     AI_OBSTACLE_IN_PATH;
        protected final idScriptBool                     AI_DEST_UNREACHABLE;
        protected final idScriptBool                     AI_HIT_ENEMY;
        protected final idScriptBool                     AI_PUSHED;
        //
        //

        public idAI() {
            aas = null;
            travelFlags = TFL_WALK | TFL_AIR;
            move = new idMoveState();
            kickForce = 2048.0f;
            ignore_obstacles = false;
            blockedRadius = 0.0f;
            blockedMoveTime = 750;
            blockedAttackTime = 750;
            turnRate = 360.0f;
            turnVel = 0.0f;
            anim_turn_yaw = 0.0f;
            anim_turn_amount = 0.0f;
            anim_turn_angles = 0.0f;
            physicsObj = new idPhysics_Monster();
            fly_offset = 0;
            fly_seek_scale = 1.0f;
            fly_roll_scale = 0.0f;
            fly_roll_max = 0.0f;
            fly_roll = 0.0f;
            fly_pitch_scale = 0.0f;
            fly_pitch_max = 0.0f;
            fly_pitch = 0.0f;
            allowMove = false;
            allowHiddenMovement = false;
            fly_speed = 0.0f;
            fly_bob_strength = 0.0f;
            fly_bob_vert = 0.0f;
            fly_bob_horz = 0.0f;
            lastHitCheckResult = false;
            lastHitCheckTime = 0;
            lastAttackTime = 0;
            melee_range = 0.0f;
            projectile_height_to_distance_ratio = 1.0f;
            missileLaunchOffset = new idList<>();
            projectileDef = null;
            projectile = new idEntityPtr<>(null);
            attack = new idStr();
            projectileClipModel = null;
            projectileRadius = 0.0f;
            projectileVelocity = getVec3_origin();
            projectileGravity = getVec3_origin();
            projectileSpeed = 0.0f;
            chat_snd = null;
            chat_min = 0;
            chat_max = 0;
            chat_time = 0;
            talk_state = TALK_NEVER;
            talkTarget = new idEntityPtr<>(null);

            particles = new idList<>();
            restartParticles = true;
            useBoneAxis = false;

            wakeOnFlashlight = false;
            worldMuzzleFlash = new renderLight_s();//memset( &worldMuzzleFlash, 0, sizeof ( worldMuzzleFlash ) );
            worldMuzzleFlashHandle = -1;

            enemy = new idEntityPtr<>(null);
            lastVisibleEnemyPos = new idVec3();
            lastVisibleEnemyEyeOffset = new idVec3();
            lastVisibleReachableEnemyPos = new idVec3();
            lastReachableEnemyPos = new idVec3();
            shrivel_rate = 0.0f;
            shrivel_start = 0;
            fl.neverDormant = false;        // AI's can go dormant
            current_yaw = 0.0f;
            ideal_yaw = 0.0f;

            num_cinematics = 0;
            current_cinematic = 0;

            allowEyeFocus = true;
            allowPain = true;
            allowJointMod = true;
            focusEntity = new idEntityPtr<>(null);
            focusTime = 0;
            alignHeadTime = 0;
            forceAlignHeadTime = 0;

            currentFocusPos = new idVec3();
            eyeAng = new idAngles();
            lookAng = new idAngles();
            destLookAng = new idAngles();
            lookMin = new idAngles();
            lookMax = new idAngles();
            
            lookJoints = new idList<>();
            lookJointAngles = new idList<>();

            eyeMin = new idAngles();
            eyeMax = new idAngles();
            muzzleFlashEnd = 0;
            flashTime = 0;
            flashJointWorld = INVALID_JOINT;

            focusJoint = INVALID_JOINT;
            orientationJoint = INVALID_JOINT;
            flyTiltJoint = INVALID_JOINT;

            eyeVerticalOffset = 0.0f;
            eyeHorizontalOffset = 0.0f;
            eyeFocusRate = 0.0f;
            headFocusRate = 0.0f;
            focusAlignTime = 0;

            AI_TALK = new idScriptBool();
            AI_DAMAGE = new idScriptBool();
            AI_PAIN = new idScriptBool();
            AI_SPECIAL_DAMAGE = new idScriptFloat();
            AI_DEAD = new idScriptBool();
            AI_ENEMY_VISIBLE = new idScriptBool();
            AI_ENEMY_IN_FOV = new idScriptBool();
            AI_ENEMY_DEAD = new idScriptBool();
            AI_MOVE_DONE = new idScriptBool();
            AI_ONGROUND = new idScriptBool();
            AI_ACTIVATED = new idScriptBool();
            AI_FORWARD = new idScriptBool();
            AI_JUMP = new idScriptBool();
            AI_ENEMY_REACHABLE = new idScriptBool();
            AI_BLOCKED = new idScriptBool();
            AI_OBSTACLE_IN_PATH = new idScriptBool();
            AI_DEST_UNREACHABLE = new idScriptBool();
            AI_HIT_ENEMY = new idScriptBool();
            AI_PUSHED = new idScriptBool();
        }
        // ~idAI();

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(travelFlags);
            move.Save(savefile);
            savedMove.Save(savefile);
            savefile.WriteFloat(kickForce);
            savefile.WriteBool(ignore_obstacles);
            savefile.WriteFloat(blockedRadius);
            savefile.WriteInt(blockedMoveTime);
            savefile.WriteInt(blockedAttackTime);

            savefile.WriteFloat(ideal_yaw);
            savefile.WriteFloat(current_yaw);
            savefile.WriteFloat(turnRate);
            savefile.WriteFloat(turnVel);
            savefile.WriteFloat(anim_turn_yaw);
            savefile.WriteFloat(anim_turn_amount);
            savefile.WriteFloat(anim_turn_angles);

            savefile.WriteStaticObject(physicsObj);

            savefile.WriteFloat(fly_speed);
            savefile.WriteFloat(fly_bob_strength);
            savefile.WriteFloat(fly_bob_vert);
            savefile.WriteFloat(fly_bob_horz);
            savefile.WriteInt(fly_offset);
            savefile.WriteFloat(fly_seek_scale);
            savefile.WriteFloat(fly_roll_scale);
            savefile.WriteFloat(fly_roll_max);
            savefile.WriteFloat(fly_roll);
            savefile.WriteFloat(fly_pitch_scale);
            savefile.WriteFloat(fly_pitch_max);
            savefile.WriteFloat(fly_pitch);

            savefile.WriteBool(allowMove);
            savefile.WriteBool(allowHiddenMovement);
            savefile.WriteBool(disableGravity);
            savefile.WriteBool(af_push_moveables);

            savefile.WriteBool(lastHitCheckResult);
            savefile.WriteInt(lastHitCheckTime);
            savefile.WriteInt(lastAttackTime);
            savefile.WriteFloat(melee_range);
            savefile.WriteFloat(projectile_height_to_distance_ratio);

            savefile.WriteInt(missileLaunchOffset.Num());
            for (i = 0; i < missileLaunchOffset.Num(); i++) {
                savefile.WriteVec3(missileLaunchOffset.oGet(i));
            }

            idStr projectileName = new idStr();
            spawnArgs.GetString("def_projectile", "", projectileName);
            savefile.WriteString(projectileName);
            savefile.WriteFloat(projectileRadius);
            savefile.WriteFloat(projectileSpeed);
            savefile.WriteVec3(projectileVelocity);
            savefile.WriteVec3(projectileGravity);
            projectile.Save(savefile);
            savefile.WriteString(attack);

            savefile.WriteSoundShader(chat_snd);
            savefile.WriteInt(chat_min);
            savefile.WriteInt(chat_max);
            savefile.WriteInt(chat_time);
            savefile.WriteInt(etoi(talk_state));
            talkTarget.Save(savefile);

            savefile.WriteInt(num_cinematics);
            savefile.WriteInt(current_cinematic);

            savefile.WriteBool(allowJointMod);
            focusEntity.Save(savefile);
            savefile.WriteVec3(currentFocusPos);
            savefile.WriteInt(focusTime);
            savefile.WriteInt(alignHeadTime);
            savefile.WriteInt(forceAlignHeadTime);
            savefile.WriteAngles(eyeAng);
            savefile.WriteAngles(lookAng);
            savefile.WriteAngles(destLookAng);
            savefile.WriteAngles(lookMin);
            savefile.WriteAngles(lookMax);

            savefile.WriteInt(lookJoints.Num());
            for (i = 0; i < lookJoints.Num(); i++) {
                savefile.WriteJoint(lookJoints.oGet(i));
                savefile.WriteAngles(lookJointAngles.oGet(i));
            }

            savefile.WriteFloat(shrivel_rate);
            savefile.WriteInt(shrivel_start);

            savefile.WriteInt(particles.Num());
            for (i = 0; i < particles.Num(); i++) {
                savefile.WriteParticle(particles.oGet(i).particle);
                savefile.WriteInt(particles.oGet(i).time);
                savefile.WriteJoint(particles.oGet(i).joint);
            }
            savefile.WriteBool(restartParticles);
            savefile.WriteBool(useBoneAxis);

            enemy.Save(savefile);
            savefile.WriteVec3(lastVisibleEnemyPos);
            savefile.WriteVec3(lastVisibleEnemyEyeOffset);
            savefile.WriteVec3(lastVisibleReachableEnemyPos);
            savefile.WriteVec3(lastReachableEnemyPos);
            savefile.WriteBool(wakeOnFlashlight);

            savefile.WriteAngles(eyeMin);
            savefile.WriteAngles(eyeMax);

            savefile.WriteFloat(eyeVerticalOffset);
            savefile.WriteFloat(eyeHorizontalOffset);
            savefile.WriteFloat(eyeFocusRate);
            savefile.WriteFloat(headFocusRate);
            savefile.WriteInt(focusAlignTime);

            savefile.WriteJoint(flashJointWorld);
            savefile.WriteInt(muzzleFlashEnd);

            savefile.WriteJoint(focusJoint);
            savefile.WriteJoint(orientationJoint);
            savefile.WriteJoint(flyTiltJoint);

            savefile.WriteBool(GetPhysics().equals(physicsObj));
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            boolean[] restorePhysics = {false};
            int i;
            int num;
            idBounds bounds;

            travelFlags = savefile.ReadInt();
            move.Restore(savefile);
            savedMove.Restore(savefile);
            kickForce = savefile.ReadFloat();
            ignore_obstacles = savefile.ReadBool();
            blockedRadius = savefile.ReadFloat();
            blockedMoveTime = savefile.ReadInt();
            blockedAttackTime = savefile.ReadInt();

            ideal_yaw = savefile.ReadFloat();
            current_yaw = savefile.ReadFloat();
            turnRate = savefile.ReadFloat();
            turnVel = savefile.ReadFloat();
            anim_turn_yaw = savefile.ReadFloat();
            anim_turn_amount = savefile.ReadFloat();
            anim_turn_angles = savefile.ReadFloat();

            savefile.ReadStaticObject(physicsObj);

            fly_speed = savefile.ReadFloat();
            fly_bob_strength = savefile.ReadFloat();
            fly_bob_vert = savefile.ReadFloat();
            fly_bob_horz = savefile.ReadFloat();
            fly_offset = savefile.ReadInt();
            fly_seek_scale = savefile.ReadFloat();
            fly_roll_scale = savefile.ReadFloat();
            fly_roll_max = savefile.ReadFloat();
            fly_roll = savefile.ReadFloat();
            fly_pitch_scale = savefile.ReadFloat();
            fly_pitch_max = savefile.ReadFloat();
            fly_pitch = savefile.ReadFloat();

            allowMove = savefile.ReadBool();
            allowHiddenMovement = savefile.ReadBool();
            disableGravity = savefile.ReadBool();
            af_push_moveables = savefile.ReadBool();

            lastHitCheckResult = savefile.ReadBool();
            lastHitCheckTime = savefile.ReadInt();
            lastAttackTime = savefile.ReadInt();
            melee_range = savefile.ReadFloat();
            projectile_height_to_distance_ratio = savefile.ReadFloat();

            num = savefile.ReadInt();
            missileLaunchOffset.SetGranularity(1);
            missileLaunchOffset.SetNum(num);
            for (i = 0; i < num; i++) {
                savefile.ReadVec3(missileLaunchOffset.oGet(i));
            }

            idStr projectileName = new idStr();
            savefile.ReadString(projectileName);
            if (projectileName.Length() != 0) {
                projectileDef = gameLocal.FindEntityDefDict(projectileName.toString());
            } else {
                projectileDef = null;
            }
            projectileRadius = savefile.ReadFloat();
            projectileSpeed = savefile.ReadFloat();
            savefile.ReadVec3(projectileVelocity);
            savefile.ReadVec3(projectileGravity);
            projectile.Restore(savefile);
            savefile.ReadString(attack);

            savefile.ReadSoundShader(chat_snd);
            chat_min = savefile.ReadInt();
            chat_max = savefile.ReadInt();
            chat_time = savefile.ReadInt();
            i = savefile.ReadInt();
            talk_state = talkState_t.values()[i];
            talkTarget.Restore(savefile);

            num_cinematics = savefile.ReadInt();
            current_cinematic = savefile.ReadInt();

            allowJointMod = savefile.ReadBool();
            focusEntity.Restore(savefile);
            savefile.ReadVec3(currentFocusPos);
            focusTime = savefile.ReadInt();
            alignHeadTime = savefile.ReadInt();
            forceAlignHeadTime = savefile.ReadInt();
            savefile.ReadAngles(eyeAng);
            savefile.ReadAngles(lookAng);
            savefile.ReadAngles(destLookAng);
            savefile.ReadAngles(lookMin);
            savefile.ReadAngles(lookMax);

            num = savefile.ReadInt();
            lookJoints.SetGranularity(1);
            lookJoints.SetNum(num);
            lookJointAngles.SetGranularity(1);
            lookJointAngles.SetNum(num);
            for (i = 0; i < num; i++) {
                lookJoints.oSet(i, savefile.ReadJoint());
                savefile.ReadAngles(lookJointAngles.oGet(i));
            }

            shrivel_rate = savefile.ReadFloat();
            shrivel_start = savefile.ReadInt();

            num = savefile.ReadInt();
            particles.SetNum(num);
            for (i = 0; i < particles.Num(); i++) {
                savefile.ReadParticle(particles.oGet(i).particle);
                particles.oGet(i).time = savefile.ReadInt();
                particles.oGet(i).joint = savefile.ReadJoint();
            }
            restartParticles = savefile.ReadBool();
            useBoneAxis = savefile.ReadBool();

            enemy.Restore(savefile);
            savefile.ReadVec3(lastVisibleEnemyPos);
            savefile.ReadVec3(lastVisibleEnemyEyeOffset);
            savefile.ReadVec3(lastVisibleReachableEnemyPos);
            savefile.ReadVec3(lastReachableEnemyPos);

            wakeOnFlashlight = savefile.ReadBool();

            savefile.ReadAngles(eyeMin);
            savefile.ReadAngles(eyeMax);

            eyeVerticalOffset = savefile.ReadFloat();
            eyeHorizontalOffset = savefile.ReadFloat();
            eyeFocusRate = savefile.ReadFloat();
            headFocusRate = savefile.ReadFloat();
            focusAlignTime = savefile.ReadInt();

            flashJointWorld = savefile.ReadJoint();
            muzzleFlashEnd = savefile.ReadInt();

            focusJoint = savefile.ReadJoint();
            orientationJoint = savefile.ReadJoint();
            flyTiltJoint = savefile.ReadJoint();

            savefile.ReadBool(restorePhysics);

            // Set the AAS if the character has the correct gravity vector
            idVec3 gravity = spawnArgs.GetVector("gravityDir", "0 0 -1");
            gravity.oMulSet(g_gravity.GetFloat());
            if (gravity == gameLocal.GetGravity()) {
                SetAAS();
            }

            SetCombatModel();
            LinkCombat();

            InitMuzzleFlash();

            // Link the script variables back to the scriptobject
            LinkScriptVariables();

            if (restorePhysics[0]) {
                RestorePhysics(physicsObj);
            }
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            idKeyValue kv;
            idStr jointName = new idStr();
            idAngles jointScale;
            int/*jointHandle_t*/ joint;
            idVec3 local_dir = new idVec3();
            boolean[] talks = {false};

            if (!g_monsters.GetBool()) {
                PostEventMS(EV_Remove, 0);
                return;
            }

            team = spawnArgs.GetInt("team", "1");
            rank = spawnArgs.GetInt("rank", "0");
            fly_offset = spawnArgs.GetInt("fly_offset", "0");
            fly_speed = spawnArgs.GetFloat("fly_speed", "100");
            fly_bob_strength = spawnArgs.GetFloat("fly_bob_strength", "50");
            fly_bob_horz = spawnArgs.GetFloat("fly_bob_vert", "2");
            fly_bob_vert = spawnArgs.GetFloat("fly_bob_horz", "2.7");
            fly_seek_scale = spawnArgs.GetFloat("fly_seek_scale", "4");
            fly_roll_scale = spawnArgs.GetFloat("fly_roll_scale", "90");
            fly_roll_max = spawnArgs.GetFloat("fly_roll_max", "60");
            fly_pitch_scale = spawnArgs.GetFloat("fly_pitch_scale", "45");
            fly_pitch_max = spawnArgs.GetFloat("fly_pitch_max", "30");

            melee_range = spawnArgs.GetFloat("melee_range", "64");
            projectile_height_to_distance_ratio = spawnArgs.GetFloat("projectile_height_to_distance_ratio", "1");

            turnRate = spawnArgs.GetFloat("turn_rate", "360");

            spawnArgs.GetBool("talks", "0", talks);
            if (spawnArgs.GetString("npc_name", null) != null) {
                if (talks[0]) {
                    talk_state = TALK_OK;
                } else {
                    talk_state = TALK_BUSY;
                }
            } else {
                talk_state = TALK_NEVER;
            }

            disableGravity = spawnArgs.GetBool("animate_z", "0");
            af_push_moveables = spawnArgs.GetBool("af_push_moveables", "0");
            kickForce = spawnArgs.GetFloat("kick_force", "4096");
            ignore_obstacles = spawnArgs.GetBool("ignore_obstacles", "0");
            blockedRadius = spawnArgs.GetFloat("blockedRadius", "-1");
            blockedMoveTime = spawnArgs.GetInt("blockedMoveTime", "750");
            blockedAttackTime = spawnArgs.GetInt("blockedAttackTime", "750");

            num_cinematics = spawnArgs.GetInt("num_cinematics", "0");
            current_cinematic = 0;

            LinkScriptVariables();

            fl.takedamage = !spawnArgs.GetBool("noDamage");
            enemy.oSet(null);
            allowMove = true;
            allowHiddenMovement = false;

            animator.RemoveOriginOffset(true);

            // create combat collision hull for exact collision detection
            SetCombatModel();

            lookMin = spawnArgs.GetAngles("look_min", "-80 -75 0");
            lookMax = spawnArgs.GetAngles("look_max", "80 75 0");

            lookJoints.SetGranularity(1);
            lookJointAngles.SetGranularity(1);
            kv = spawnArgs.MatchPrefix("look_joint", null);
            while (kv != null) {
                jointName = kv.GetKey();
                jointName.StripLeadingOnce("look_joint ");
                joint = animator.GetJointHandle(jointName);
                if (joint == INVALID_JOINT) {
                    gameLocal.Warning("Unknown look_joint '%s' on entity %s", jointName, name);
                } else {
                    jointScale = spawnArgs.GetAngles(kv.GetKey().toString(), "0 0 0");
                    jointScale.roll = 0.0f;

                    // if no scale on any component, then don't bother adding it.  this may be done to
                    // zero out rotation from an inherited entitydef.
                    if (jointScale != getAng_zero()) {
                        lookJoints.Append(joint);
                        lookJointAngles.Append(jointScale);
                    }
                }
                kv = spawnArgs.MatchPrefix("look_joint", kv);
            }

            // calculate joint positions on attack frames so we can do proper "can hit" tests
            CalculateAttackOffsets();

            eyeMin = spawnArgs.GetAngles("eye_turn_min", "-10 -30 0");
            eyeMax = spawnArgs.GetAngles("eye_turn_max", "10 30 0");
            eyeVerticalOffset = spawnArgs.GetFloat("eye_verticle_offset", "5");
            eyeHorizontalOffset = spawnArgs.GetFloat("eye_horizontal_offset", "-8");
            eyeFocusRate = spawnArgs.GetFloat("eye_focus_rate", "0.5");
            headFocusRate = spawnArgs.GetFloat("head_focus_rate", "0.1");
            focusAlignTime = (int) SEC2MS(spawnArgs.GetFloat("focus_align_time", "1"));

            flashJointWorld = animator.GetJointHandle("flash");

            if (head.GetEntity() != null) {
                idAnimator headAnimator = head.GetEntity().GetAnimator();

                jointName.oSet(spawnArgs.GetString("bone_focus"));
                if (isNotNullOrEmpty(jointName)) {
                    focusJoint = headAnimator.GetJointHandle(jointName);
                    if (focusJoint == INVALID_JOINT) {
                        gameLocal.Warning("Joint '%s' not found on head on '%s'", jointName, name);
                    }
                }
            } else {
                jointName.oSet(spawnArgs.GetString("bone_focus"));
                if (isNotNullOrEmpty(jointName)) {
                    focusJoint = animator.GetJointHandle(jointName);
                    if (focusJoint == INVALID_JOINT) {
                        gameLocal.Warning("Joint '%s' not found on '%s'", jointName, name);
                    }
                }
            }

            jointName.oSet(spawnArgs.GetString("bone_orientation"));
            if (isNotNullOrEmpty(jointName)) {
                orientationJoint = animator.GetJointHandle(jointName);
                if (orientationJoint == INVALID_JOINT) {
                    gameLocal.Warning("Joint '%s' not found on '%s'", jointName, name);
                }
            }

            jointName.oSet(spawnArgs.GetString("bone_flytilt"));
            if (isNotNullOrEmpty(jointName)) {
                flyTiltJoint = animator.GetJointHandle(jointName);
                if (flyTiltJoint == INVALID_JOINT) {
                    gameLocal.Warning("Joint '%s' not found on '%s'", jointName, name);
                }
            }

            InitMuzzleFlash();

            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            physicsObj.SetMass(spawnArgs.GetFloat("mass", "100"));

            if (spawnArgs.GetBool("big_monster")) {
                physicsObj.SetContents(0);
                physicsObj.SetClipMask(MASK_MONSTERSOLID & ~CONTENTS_BODY);
            } else {
                if (use_combat_bbox) {
                    physicsObj.SetContents(CONTENTS_BODY | CONTENTS_SOLID);
                } else {
                    physicsObj.SetContents(CONTENTS_BODY);
                }
                physicsObj.SetClipMask(MASK_MONSTERSOLID);
            }

            // move up to make sure the monster is at least an epsilon above the floor
            physicsObj.SetOrigin(GetPhysics().GetOrigin().oPlus(new idVec3(0, 0, CM_CLIP_EPSILON)));

            if (num_cinematics != 0) {
                physicsObj.SetGravity(getVec3_origin());
            } else {
                idVec3 gravity = spawnArgs.GetVector("gravityDir", "0 0 -1");
                gravity.oMulSet(g_gravity.GetFloat());
                physicsObj.SetGravity(gravity);
            }

            SetPhysics(physicsObj);

            physicsObj.GetGravityAxis().ProjectVector(viewAxis.oGet(0), local_dir);
            current_yaw = local_dir.ToYaw();
            ideal_yaw = idMath.AngleNormalize180(current_yaw);

            move.blockTime = 0;

            SetAAS();

            projectile.oSet(null);
            projectileDef = null;
            projectileClipModel = null;
            idStr projectileName = new idStr();
            if (spawnArgs.GetString("def_projectile", "", projectileName) && projectileName.Length() != 0) {
                projectileDef = gameLocal.FindEntityDefDict(projectileName);
                CreateProjectile(getVec3_origin(), viewAxis.oGet(0));
                projectileRadius = projectile.GetEntity().GetPhysics().GetClipModel().GetBounds().GetRadius();
                projectileVelocity = idProjectile.GetVelocity(projectileDef);
                projectileGravity = idProjectile.GetGravity(projectileDef);
                projectileSpeed = projectileVelocity.Length();
		        idEntity.delete(projectile.GetEntity());
                projectile.oSet(null);
            }

            particles.Clear();
            restartParticles = true;
            useBoneAxis = spawnArgs.GetBool("useBoneAxis");
            SpawnParticles("smokeParticleSystem");

            if (num_cinematics != 0 || spawnArgs.GetBool("hide") || spawnArgs.GetBool("teleport") || spawnArgs.GetBool("trigger_anim")) {
                fl.takedamage = false;
                physicsObj.SetContents(0);
                physicsObj.GetClipModel().Unlink();
                Hide();
            } else {
                // play a looping ambient sound if we have one
                StartSound("snd_ambient", SND_CHANNEL_AMBIENT, 0, false, null);
            }

            if (health <= 0) {
                gameLocal.Warning("entity '%s' doesn't have health set", name);
                health = 1;
            }

            // set up monster chatter
            SetChatSound();

            BecomeActive(TH_THINK);

            if (af_push_moveables) {
                af.SetupPose(this, gameLocal.time);
                af.GetPhysics().EnableClip();
            }

            // init the move variables
            StopMove(MOVE_STATUS_DONE);
        }
//
//        public void HeardSound(idEntity ent, final String action);
//

        public idActor GetEnemy() {
            return enemy.GetEntity();
        }

        public void TalkTo(idActor actor) {
            if (talk_state != TALK_OK) {
                return;
            }

            talkTarget.oSet(actor);
            AI_TALK._(actor != null);
        }

        public talkState_t GetTalkState() {
            if ((talk_state != TALK_NEVER) && AI_DEAD._()) {
                return TALK_DEAD;
            }
            if (IsHidden()) {
                return TALK_NEVER;
            }
            return talk_state;
        }

        public boolean GetAimDir(final idVec3 firePos, idEntity aimAtEnt, final idEntity ignore, idVec3 aimDir) {
            idVec3 targetPos1 = new idVec3();
            idVec3 targetPos2 = new idVec3();
            idVec3 delta;
            float max_height;
            boolean result;

            // if no aimAtEnt or projectile set
            if (null == aimAtEnt || null == projectileDef) {
                aimDir.oSet(viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis()));
                return false;
            }

            if (projectileClipModel == null) {
                CreateProjectileClipModel();
            }

            if (aimAtEnt.equals(enemy.GetEntity())) {
                ((idActor) aimAtEnt).GetAIAimTargets(lastVisibleEnemyPos, targetPos1, targetPos2);
            } else if (aimAtEnt.IsType(idActor.class)) {
                ((idActor) aimAtEnt).GetAIAimTargets(aimAtEnt.GetPhysics().GetOrigin(), targetPos1, targetPos2);
            } else {
                targetPos1 = aimAtEnt.GetPhysics().GetAbsBounds().GetCenter();
                targetPos2 = targetPos1;
            }

            // try aiming for chest
            delta = firePos.oMinus(targetPos1);
            max_height = delta.LengthFast() * projectile_height_to_distance_ratio;
            result = PredictTrajectory(firePos, targetPos1, projectileSpeed, projectileGravity, projectileClipModel, MASK_SHOT_RENDERMODEL, max_height, ignore, aimAtEnt, ai_debugTrajectory.GetBool() ? 1000 : 0, aimDir);
            if (result || !aimAtEnt.IsType(idActor.class)) {
                return result;
            }

            // try aiming for head
            delta = firePos.oMinus(targetPos2);
            max_height = delta.LengthFast() * projectile_height_to_distance_ratio;
            result = PredictTrajectory(firePos, targetPos2, projectileSpeed, projectileGravity, projectileClipModel, MASK_SHOT_RENDERMODEL, max_height, ignore, aimAtEnt, ai_debugTrajectory.GetBool() ? 1000 : 0, aimDir);

            return result;
        }

        public void TouchedByFlashlight(idActor flashlight_owner) {
            if (wakeOnFlashlight) {
                Activate(flashlight_owner);
            }
        }

        // Outputs a list of all monsters to the console.
        public static class List_f extends cmdFunction_t {

            private static final cmdFunction_t instance = new List_f();

            private List_f() {
            }

            public static cmdFunction_t getInstance() {
                return instance;
            }

            @Override
            public void run(idCmdArgs args) {
                int e;
                idAI check;
                int count;
                String statename;

                count = 0;

                gameLocal.Printf("%-4s  %-20s %s\n", " Num", "EntityDef", "Name");
                gameLocal.Printf("------------------------------------------------\n");
                for (e = 0; e < MAX_GENTITIES; e++) {
                    check = ((idAI) gameLocal.entities[e]);
                    if (NOT(check) || !check.IsType(idAI.class)) {
                        continue;
                    }

                    if (check.state != null) {
                        statename = check.state.Name();
                    } else {
                        statename = "NULL state";
                    }

                    gameLocal.Printf("%4d: %-20s %-20s %s  move: %d\n", e, check.GetEntityDefName(), check.name, statename, check.allowMove);
                    count++;
                }

                gameLocal.Printf("...%d monsters\n", count);
            }
        };

        /*
         ============
         idAI::FindPathAroundObstacles

         Finds a path around dynamic obstacles using a path tree with clockwise and counter clockwise edge walks.
         ============
         */
        public static boolean FindPathAroundObstacles(final idPhysics physics, final idAAS aas, final idEntity ignore, final idVec3 startPos, final idVec3 seekPos, obstaclePath_s path) {
            int numObstacles, areaNum;
            int[] insideObstacle = {0};
            obstacle_s[] obstacles = new obstacle_s[MAX_OBSTACLES];
            idBounds clipBounds = new idBounds();
            idBounds bounds = new idBounds();
            pathNode_s root;
            boolean pathToGoalExists;

            path.seekPos = seekPos;
            path.firstObstacle = null;
            path.startPosOutsideObstacles = startPos;
            path.startPosObstacle = null;
            path.seekPosOutsideObstacles = seekPos;
            path.seekPosObstacle = null;

            if (NOT(aas)) {
                return true;
            }

            bounds.oSet(1, aas.GetSettings().boundingBoxes[0].oGet(1));
            bounds.oSet(0, bounds.oGet(1).oNegative());
            bounds.oGet(1).z = 32.0f;

            // get the AAS area number and a valid point inside that area
            areaNum = aas.PointReachableAreaNum(path.startPosOutsideObstacles, bounds, (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY));
            aas.PushPointIntoAreaNum(areaNum, path.startPosOutsideObstacles);

            // get all the nearby obstacles
            numObstacles = GetObstacles(physics, aas, ignore, areaNum, path.startPosOutsideObstacles, path.seekPosOutsideObstacles, obstacles, MAX_OBSTACLES, clipBounds);

            // get a source position outside the obstacles
            GetPointOutsideObstacles(obstacles, numObstacles, path.startPosOutsideObstacles.ToVec2(), insideObstacle, null);
            if (insideObstacle[0] != -1) {
                path.startPosObstacle = obstacles[insideObstacle[0]].entity;
            }

            // get a goal position outside the obstacles
            GetPointOutsideObstacles(obstacles, numObstacles, path.seekPosOutsideObstacles.ToVec2(), insideObstacle, null);
            if (insideObstacle[0] != -1) {
                path.seekPosObstacle = obstacles[insideObstacle[0]].entity;
            }

            // if start and destination are pushed to the same point, we don't have a path around the obstacle
            if ((path.seekPosOutsideObstacles.ToVec2().oMinus(path.startPosOutsideObstacles.ToVec2())).LengthSqr() < Square(1.0f)) {
                if ((seekPos.ToVec2().oMinus(startPos.ToVec2())).LengthSqr() > Square(2.0f)) {
                    return false;
                }
            }

            // build a path tree
            root = BuildPathTree(obstacles, numObstacles, clipBounds, path.startPosOutsideObstacles.ToVec2(), path.seekPosOutsideObstacles.ToVec2(), path);

            // draw the path tree
            if (ai_showObstacleAvoidance.GetBool()) {
                DrawPathTree(root, physics.GetOrigin().z);
            }

            // prune the tree
            PrunePathTree(root, path.seekPosOutsideObstacles.ToVec2());

            // find the optimal path
            pathToGoalExists = FindOptimalPath(root, obstacles, numObstacles, physics.GetOrigin().z, physics.GetLinearVelocity(), path.seekPos);

            // free the tree
            FreePathTree_r(root);

            return pathToGoalExists;
        }

        // Frees any nodes used for the dynamic obstacle avoidance.
        public static void FreeObstacleAvoidanceNodes() {
            pathNodeAllocator = 0;//Shutdown();//TODO:do other shutdowning actions.
        }

        /*
         ============
         idAI::PredictPath

         Can also be used when there is no AAS file available however ledges are not detected.
         // Predicts movement, returns true if a stop event was triggered.
         ============
         */
        public static boolean PredictPath(final idEntity ent, final idAAS aas, final idVec3 start, final idVec3 velocity, int totalTime, int frameTime, int stopEvent, predictedPath_s path) {
            int i, j, step, numFrames, curFrameTime;
            idVec3 delta, curStart, curEnd, curVelocity, lastEnd = new idVec3(), stepUp = new idVec3(), tmpStart;
            idVec3 gravity, gravityDir, invGravityDir;
            float maxStepHeight, minFloorCos;
            pathTrace_s trace = new pathTrace_s();

            if (aas != null && aas.GetSettings() != null) {
                gravity = aas.GetSettings().gravity;
                gravityDir = aas.GetSettings().gravityDir;
                invGravityDir = aas.GetSettings().invGravityDir;
                maxStepHeight = aas.GetSettings().maxStepHeight[0];
                minFloorCos = aas.GetSettings().minFloorCos[0];
            } else {
                gravity = DEFAULT_GRAVITY_VEC3;
                gravityDir = new idVec3(0, 0, -1);
                invGravityDir = new idVec3(0, 0, 1);
                maxStepHeight = 14.0f;
                minFloorCos = 0.7f;
            }

            path.endPos = start;
            path.endVelocity = velocity;
            path.endNormal.Zero();
            path.endEvent = 0;
            path.endTime = 0;
            path.blockingEntity = null;

            curStart = start;
            curVelocity = velocity;

            numFrames = (totalTime + frameTime - 1) / frameTime;
            curFrameTime = frameTime;
            for (i = 0; i < numFrames; i++) {

                if (i == numFrames - 1) {
                    curFrameTime = totalTime - i * curFrameTime;
                }

                delta = curVelocity.oMultiply(curFrameTime).oMultiply(0.001f);

                path.endVelocity = curVelocity;
                path.endTime = i * frameTime;

                // allow sliding along a few surfaces per frame
                for (j = 0; j < MAX_FRAME_SLIDE; j++) {

                    idVec3 lineStart = curStart;

                    // allow stepping up three times per frame
                    for (step = 0; step < 3; step++) {

                        curEnd = curStart.oPlus(delta);
                        if (PathTrace(ent, aas, curStart, curEnd, stopEvent, trace, path)) {
                            return true;
                        }

                        if (step != 0) {

                            // step down at end point
                            tmpStart = trace.endPos;
                            curEnd = tmpStart.oMinus(stepUp);
                            if (PathTrace(ent, aas, tmpStart, curEnd, stopEvent, trace, path)) {
                                return true;
                            }

                            // if not moved any further than without stepping up, or if not on a floor surface
                            if ((lastEnd.oMinus(start)).LengthSqr() > (trace.endPos.oMinus(start)).LengthSqr() - 0.1f
                                    || (trace.normal.oMultiply(invGravityDir)) < minFloorCos) {
                                if ((stopEvent & SE_BLOCKED) != 0) {
                                    path.endPos = lastEnd;
                                    path.endEvent = SE_BLOCKED;

                                    if (ai_debugMove.GetBool()) {
                                        gameRenderWorld.DebugLine(colorRed, lineStart, lastEnd);
                                    }

                                    return true;
                                }

                                curStart = lastEnd;
                                break;
                            }
                        }

                        path.endNormal = trace.normal;
                        path.blockingEntity = trace.blockingEntity;

                        // if the trace is not blocked or blocked by a floor surface
                        if (trace.fraction >= 1.0f || (trace.normal.oMultiply(invGravityDir)) > minFloorCos) {
                            curStart = trace.endPos;
                            break;
                        }

                        // save last result
                        lastEnd = trace.endPos;

                        // step up
                        stepUp = invGravityDir.oMultiply(maxStepHeight);
                        if (PathTrace(ent, aas, curStart, curStart.oPlus(stepUp), stopEvent, trace, path)) {
                            return true;
                        }
                        stepUp.oMulSet(trace.fraction);
                        curStart = trace.endPos;
                    }

                    if (ai_debugMove.GetBool()) {
                        gameRenderWorld.DebugLine(colorRed, lineStart, curStart);
                    }

                    if (trace.fraction >= 1.0f) {
                        break;
                    }

                    delta.ProjectOntoPlane(trace.normal, OVERCLIP);
                    curVelocity.ProjectOntoPlane(trace.normal, OVERCLIP);

                    if ((stopEvent & SE_BLOCKED) != 0) {
                        // if going backwards
                        if ((curVelocity.oMinus(gravityDir.oMultiply(curVelocity.oMultiply(gravityDir))))
                                .oMultiply(velocity.oMinus(gravityDir.oMultiply(velocity.oMultiply(gravityDir)))) < 0.0f) {
                            path.endPos = curStart;
                            path.endEvent = SE_BLOCKED;

                            return true;
                        }
                    }
                }

                if (j >= MAX_FRAME_SLIDE) {
                    if ((stopEvent & SE_BLOCKED) != 0) {
                        path.endPos = curStart;
                        path.endEvent = SE_BLOCKED;
                        return true;
                    }
                }

                // add gravity
                curVelocity.oPluSet(gravity.oMultiply(frameTime).oMultiply(0.001f));
            }

            path.endTime = totalTime;
            path.endVelocity = curVelocity;
            path.endPos = curStart;
            path.endEvent = 0;

            return false;
        }

        /*
         ===============================================================================

         Trajectory Prediction

         Finds the best collision free trajectory for a clip model based on an
         initial position, target position and speed.

         ===============================================================================
         */
        // Return true if the trajectory of the clip model is collision free.
        public static boolean TestTrajectory(final idVec3 start, final idVec3 end, float zVel, float gravity, float time, float max_height, final idClipModel clip, int clipmask, final idEntity ignore, final idEntity targetEntity, int drawtime) {
            int i, numSegments;
            float maxHeight, t, t2;
            idVec3[] points = new idVec3[5];
            trace_s[] trace = {null};
            boolean result;

            t = zVel / gravity;
            // maximum height of projectile
            maxHeight = start.z - 0.5f * gravity * (t * t);
            // time it takes to fall from the top to the end height
            t = idMath.Sqrt((maxHeight - end.z) / (0.5f * -gravity));

            // start of parabolic
            points[0] = start;

            if (t < time) {
                numSegments = 4;
                // point in the middle between top and start
                t2 = (time - t) * 0.5f;
                points[1].ToVec2().oSet(start.ToVec2().oPlus((end.ToVec2().oMinus(start.ToVec2())).oMultiply(t2 / time)));
                points[1].z = start.z + t2 * zVel + 0.5f * gravity * t2 * t2;
                // top of parabolic
                t2 = time - t;
                points[2].ToVec2().oSet(start.ToVec2().oPlus((end.ToVec2().oMinus(start.ToVec2())).oMultiply(t2 / time)));
                points[2].z = start.z + t2 * zVel + 0.5f * gravity * t2 * t2;
                // point in the middel between top and end
                t2 = time - t * 0.5f;
                points[3].ToVec2().oSet(start.ToVec2().oPlus((end.ToVec2().oMinus(start.ToVec2())).oMultiply(t2 / time)));
                points[3].z = start.z + t2 * zVel + 0.5f * gravity * t2 * t2;
            } else {
                numSegments = 2;
                // point halfway through
                t2 = time * 0.5f;
                points[1].ToVec2().oSet(start.ToVec2().oPlus((end.ToVec2().oMinus(start.ToVec2())).oMultiply(0.5f)));
                points[1].z = start.z + t2 * zVel + 0.5f * gravity * t2 * t2;
            }

            // end of parabolic
            points[numSegments] = end;

            if (drawtime != 0) {
                for (i = 0; i < numSegments; i++) {
                    gameRenderWorld.DebugLine(colorRed, points[i], points[i + 1], drawtime);
                }
            }

            // make sure projectile doesn't go higher than we want it to go
            for (i = 0; i < numSegments; i++) {
                if (points[i].z > max_height) {
                    // goes higher than we want to allow
                    return false;
                }
            }

            result = true;
            for (i = 0; i < numSegments; i++) {
                gameLocal.clip.Translation(trace, points[i], points[i + 1], clip, getMat3_identity(), clipmask, ignore);
                if (trace[0].fraction < 1.0f) {
                    result = gameLocal.GetTraceEntity(trace[0]).equals(targetEntity);
                    break;
                }
            }

            if (drawtime != 0) {
                if (clip != null) {
                    gameRenderWorld.DebugBounds(result ? colorGreen : colorYellow, clip.GetBounds().Expand(1.0f), trace[0].endpos, drawtime);
                } else {
                    idBounds bnds = new idBounds(trace[0].endpos);
                    bnds.ExpandSelf(1.0f);
                    gameRenderWorld.DebugBounds(result ? colorGreen : colorYellow, bnds, getVec3_zero(), drawtime);
                }
            }

            return result;
        }

        /*
         =====================
         idAI::PredictTrajectory

         returns true if there is a collision free trajectory for the clip model
         aimDir is set to the ideal aim direction in order to hit the target
         // Finds the best collision free trajectory for a clip model.
         =====================
         */
        public static boolean PredictTrajectory(final idVec3 firePos, final idVec3 target, float projectileSpeed, final idVec3 projGravity, final idClipModel clip, int clipmask, float max_height, final idEntity ignore, final idEntity targetEntity, int drawtime, idVec3 aimDir) {
            int n, i, j;
            float zVel, a, t, pitch;
            float[] s = {0}, c = {0};
            trace_s[] trace = {null};
            ballistics_s[] ballistics = new ballistics_s[2];
            idVec3[] dir = new idVec3[2];
            idVec3 velocity;
            idVec3 lastPos, pos;

            assert (targetEntity != null);

            // check if the projectile starts inside the target
            if (targetEntity.GetPhysics().GetAbsBounds().IntersectsBounds(clip.GetBounds().Translate(firePos))) {
                aimDir.oSet(target.oMinus(firePos));
                aimDir.Normalize();
                return true;
            }

            // if no velocity or the projectile is not affected by gravity
            if (projectileSpeed <= 0.0f || projGravity == getVec3_origin()) {

                aimDir.oSet(target.oMinus(firePos));
                aimDir.Normalize();

                gameLocal.clip.Translation(trace, firePos, target, clip, getMat3_identity(), clipmask, ignore);

                if (drawtime != 0) {
                    gameRenderWorld.DebugLine(colorRed, firePos, target, drawtime);
                    idBounds bnds = new idBounds(trace[0].endpos);
                    bnds.ExpandSelf(1.0f);
                    gameRenderWorld.DebugBounds((trace[0].fraction >= 1.0f || (gameLocal.GetTraceEntity(trace[0]) == targetEntity)) ? colorGreen : colorYellow, bnds, getVec3_zero(), drawtime);
                }

                return (trace[0].fraction >= 1.0f || (gameLocal.GetTraceEntity(trace[0]) == targetEntity));
            }

            n = Ballistics(firePos, target, projectileSpeed, projGravity.oGet(2), ballistics);
            if (n == 0) {
                // there is no valid trajectory
                aimDir.oSet(target.oMinus(firePos));
                aimDir.Normalize();
                return false;
            }

            // make sure the first angle is the smallest
            if (n == 2) {
                if (ballistics[1].angle < ballistics[0].angle) {
                    a = ballistics[0].angle;
                    ballistics[0].angle = ballistics[1].angle;
                    ballistics[1].angle = a;
                    t = ballistics[0].time;
                    ballistics[0].time = ballistics[1].time;
                    ballistics[1].time = t;
                }
            }

            // test if there is a collision free trajectory
            for (i = 0; i < n; i++) {
                pitch = (float) DEG2RAD(ballistics[i].angle);
                idMath.SinCos(pitch, s, c);
                dir[i] = target.oMinus(firePos);
                dir[i].z = 0.0f;
                dir[i].oMulSet(c[0] * idMath.InvSqrt(dir[i].LengthSqr()));
                dir[i].z = s[0];

                zVel = projectileSpeed * dir[i].z;

                if (ai_debugTrajectory.GetBool()) {
                    t = ballistics[i].time / 100.0f;
                    velocity = dir[i].oMultiply(projectileSpeed);
                    lastPos = firePos;
                    pos = firePos;
                    for (j = 1; j < 100; j++) {
                        pos.oPluSet(velocity.oMultiply(t));
                        velocity.oPluSet(projGravity.oMultiply(t));
                        gameRenderWorld.DebugLine(colorCyan, lastPos, pos);
                        lastPos = pos;
                    }
                }

                if (TestTrajectory(firePos, target, zVel, projGravity.oGet(2), ballistics[i].time, firePos.z + max_height, clip, clipmask, ignore, targetEntity, drawtime)) {
                    aimDir.oSet(dir[i]);
                    return true;
                }
            }

            aimDir.oSet(dir[0]);

            // there is no collision free trajectory
            return false;
        }

        //
        // ai/ai.cpp
        //
        protected void SetAAS() {
            idStr use_aas = new idStr();

            spawnArgs.GetString("use_aas", null, use_aas);
            aas = gameLocal.GetAAS(use_aas.toString());
            if (aas != null) {
                final idAASSettings settings = aas.GetSettings();
                if (settings != null) {
                    if (!ValidForBounds(settings, physicsObj.GetBounds())) {
                        gameLocal.Error("%s cannot use use_aas %s\n", name, use_aas);
                    }
                    float height = settings.maxStepHeight[0];
                    physicsObj.SetMaxStepHeight(height);
                    return;
                } else {
                    aas = null;
                }
            }
            gameLocal.Printf("WARNING: %s has no AAS file\n", name);
        }

        /*
         ================
         idAI::DormantBegin

         called when entity becomes dormant
         ================
         */
        @Override
        public void DormantBegin() {
            // since dormant happens on a timer, we wont get to update particles to
            // hidden through the think loop, but we need to hide them though.
            if (particles.Num() != 0) {
                for (int i = 0; i < particles.Num(); i++) {
                    particles.oGet(i).time = 0;
                }
            }

            if (enemyNode.InList()) {
                // remove ourselves from the enemy's enemylist
                enemyNode.Remove();
            }
            super.DormantBegin();
        }

        /*
         ================
         idAI::DormantEnd

         called when entity wakes from being dormant
         ================
         */
        @Override
        public void DormantEnd() {
            if (enemy.GetEntity() != null && !enemyNode.InList()) {
                // let our enemy know we're back on the trail
                enemyNode.AddToEnd(enemy.GetEntity().enemyList);
            }

            if (particles.Num() != 0) {
                for (int i = 0; i < particles.Num(); i++) {
                    particles.oGet(i).time = gameLocal.time;
                }
            }

            super.DormantEnd();
        }

        @Override
        public void Think() {
            // if we are completely closed off from the player, don't do anything at all
            if (CheckDormant()) {
                return;
            }

            if ((thinkFlags & TH_THINK) != 0) {
                // clear out the enemy when he dies or is hidden
                idActor enemyEnt = enemy.GetEntity();
                if (enemyEnt != null) {
                    if (enemyEnt.health <= 0) {
                        EnemyDead();
                    }
                }

                current_yaw += deltaViewAngles.yaw;
                ideal_yaw = idMath.AngleNormalize180(ideal_yaw + deltaViewAngles.yaw);
                deltaViewAngles.Zero();
                viewAxis = new idAngles(0, current_yaw, 0).ToMat3();

                if (num_cinematics != 0) {
                    if (!IsHidden() && torsoAnim.AnimDone(0)) {
                        PlayCinematic();
                    }
                    RunPhysics();
                } else if (!allowHiddenMovement && IsHidden()) {
                    // hidden monsters
                    UpdateAIScript();
                } else {
                    // clear the ik before we do anything else so the skeleton doesn't get updated twice
                    walkIK.ClearJointMods();

                    switch (move.moveType) {
                        case MOVETYPE_DEAD:
                            // dead monsters
                            UpdateAIScript();
                            DeadMove();
                            break;

                        case MOVETYPE_FLY:
                            // flying monsters
                            UpdateEnemyPosition();
                            UpdateAIScript();
                            FlyMove();
                            PlayChatter();
                            CheckBlink();
                            break;

                        case MOVETYPE_STATIC:
                            // static monsters
                            UpdateEnemyPosition();
                            UpdateAIScript();
                            StaticMove();
                            PlayChatter();
                            CheckBlink();
                            break;

                        case MOVETYPE_ANIM:
                            // animation based movement
                            UpdateEnemyPosition();
                            UpdateAIScript();
                            AnimMove();
                            PlayChatter();
                            CheckBlink();
                            break;

                        case MOVETYPE_SLIDE:
                            // velocity based movement
                            UpdateEnemyPosition();
                            UpdateAIScript();
                            SlideMove();
                            PlayChatter();
                            CheckBlink();
                            break;
                    }
                }

                // clear pain flag so that we recieve any damage between now and the next time we run the script
                AI_PAIN._(false);
                AI_SPECIAL_DAMAGE._(0f);
                AI_PUSHED._(false);
            } else if ((thinkFlags & TH_PHYSICS) != 0) {
                RunPhysics();
            }

            if (af_push_moveables) {
                PushWithAF();
            }

            if (fl.hidden && allowHiddenMovement) {
                // UpdateAnimation won't call frame commands when hidden, so call them here when we allow hidden movement
                animator.ServiceAnims(gameLocal.previousTime, gameLocal.time);
            }
            /*	this still draws in retail builds.. not sure why.. don't care at this point.
             if ( !aas && developer.GetBool() && !fl.hidden && !num_cinematics ) {
             gameRenderWorld->DrawText( "No AAS", physicsObj.GetAbsBounds().GetCenter(), 0.1f, colorWhite, gameLocal.GetLocalPlayer()->viewAngles.ToMat3(), 1, gameLocal.msec );
             }
             */

            UpdateMuzzleFlash();
            UpdateAnimation();
            UpdateParticles();
            Present();
            UpdateDamageEffects();
            LinkCombat();
        }

        /*
         =====================
         idAI::Activate

         Notifies the script that a monster has been activated by a trigger or flashlight
         =====================
         */
        protected void Activate(idEntity activator) {
            idPlayer player;

            if (AI_DEAD._()) {
                // ignore it when they're dead
                return;
            }

            // make sure he's not dormant
            dormantStart = 0;

            if (num_cinematics != 0) {
                PlayCinematic();
            } else {
                AI_ACTIVATED._(true);
                if (NOT(activator) || !activator.IsType(idPlayer.class)) {
                    player = gameLocal.GetLocalPlayer();
                } else {
                    player = ((idPlayer) activator);
                }

                if ((ReactionTo(player) & ATTACK_ON_ACTIVATE) != 0) {
                    SetEnemy(player);
                }

                // update the script in cinematics so that entities don't start anims or show themselves a frame late.
                if (cinematic) {
                    UpdateAIScript();

                    // make sure our model gets updated
                    animator.ForceUpdate();

                    // update the anim bounds
                    UpdateAnimation();
                    UpdateVisuals();
                    Present();

                    if (head.GetEntity() != null) {
                        // since the body anim was updated, we need to run physics to update the position of the head
                        RunPhysics();

                        // make sure our model gets updated
                        head.GetEntity().GetAnimator().ForceUpdate();

                        // update the anim bounds
                        head.GetEntity().UpdateAnimation();
                        head.GetEntity().UpdateVisuals();
                        head.GetEntity().Present();
                    }
                }
            }
        }

        protected int ReactionTo(final idEntity ent) {

            if (ent.fl.hidden) {
                // ignore hidden entities
                return ATTACK_IGNORE;
            }

            if (!ent.IsType(idActor.class)) {
                return ATTACK_IGNORE;
            }

            final idActor actor = ((idActor) ent);
            if (actor.IsType(idPlayer.class) && ((idPlayer) actor).noclip) {
                // ignore players in noclip mode
                return ATTACK_IGNORE;
            }

            // actors on different teams will always fight each other
            if (actor.team != team) {
                if (actor.fl.notarget) {
                    // don't attack on sight when attacker is notargeted
                    return ATTACK_ON_DAMAGE | ATTACK_ON_ACTIVATE;
                }
                return ATTACK_ON_SIGHT | ATTACK_ON_DAMAGE | ATTACK_ON_ACTIVATE;
            }

            // monsters will fight when attacked by lower ranked monsters.  rank 0 never fights back.
            if (rank != 0 && (actor.rank < rank)) {
                return ATTACK_ON_DAMAGE;
            }

            // don't fight back
            return ATTACK_IGNORE;
        }
//
//        protected boolean CheckForEnemy();
//

        protected void EnemyDead() {
            ClearEnemy();
            AI_ENEMY_DEAD._(true);
        }

        /*
         ================
         idAI::CanPlayChatterSounds

         Used for playing chatter sounds on monsters.
         ================
         */
        @Override
        public boolean CanPlayChatterSounds() {
            if (AI_DEAD._()) {
                return false;
            }

            if (IsHidden()) {
                return false;
            }

            if (enemy.GetEntity() != null) {
                return true;
            }

            return !spawnArgs.GetBool("no_idle_chatter");
        }

        protected void SetChatSound() {
            final String snd;

            if (IsHidden()) {
                snd = null;
            } else if (enemy.GetEntity() != null) {
                snd = spawnArgs.GetString("snd_chatter_combat", null);
                chat_min = (int) SEC2MS(spawnArgs.GetFloat("chatter_combat_min", "5"));
                chat_max = (int) SEC2MS(spawnArgs.GetFloat("chatter_combat_max", "10"));
            } else if (!spawnArgs.GetBool("no_idle_chatter")) {
                snd = spawnArgs.GetString("snd_chatter", null);
                chat_min = (int) SEC2MS(spawnArgs.GetFloat("chatter_min", "5"));
                chat_max = (int) SEC2MS(spawnArgs.GetFloat("chatter_max", "10"));
            } else {
                snd = null;
            }

            if (isNotNullOrEmpty(snd)) {
                chat_snd = declManager.FindSound(snd);

                // set the next chat time
                chat_time = (int) (gameLocal.time + chat_min + gameLocal.random.RandomFloat() * (chat_max - chat_min));
            } else {
                chat_snd = null;
            }
        }

        protected void PlayChatter() {
            // check if it's time to play a chat sound
            if (AI_DEAD._() || NOT(chat_snd) || (chat_time > gameLocal.time)) {
                return;
            }

            StartSoundShader(chat_snd, SND_CHANNEL_VOICE, 0, false, null);

            // set the next chat time
            chat_time = (int) (gameLocal.time + chat_min + gameLocal.random.RandomFloat() * (chat_max - chat_min));
        }

        @Override
        public void Hide() {
            super.Hide();//TODO:expose multilayer inherited functions
            fl.takedamage = false;
            physicsObj.SetContents(0);
            physicsObj.GetClipModel().Unlink();
            StopSound(etoi(SND_CHANNEL_AMBIENT), false);
            SetChatSound();

            AI_ENEMY_IN_FOV._(false);
            AI_ENEMY_VISIBLE._(false);
            StopMove(MOVE_STATUS_DONE);
        }

        @Override
        public void Show() {
            super.Show();
            if (spawnArgs.GetBool("big_monster")) {
                physicsObj.SetContents(0);
            } else if (use_combat_bbox) {
                physicsObj.SetContents(CONTENTS_BODY | CONTENTS_SOLID);
            } else {
                physicsObj.SetContents(CONTENTS_BODY);
            }
            physicsObj.GetClipModel().Link(gameLocal.clip);
            fl.takedamage = !spawnArgs.GetBool("noDamage");
            SetChatSound();
            StartSound("snd_ambient", SND_CHANNEL_AMBIENT, 0, false, null);
        }

        protected idVec3 FirstVisiblePointOnPath(final idVec3 origin, final idVec3 target, int travelFlags) {
            int i, areaNum, targetAreaNum, curAreaNum;
            int[] travelTime = {0};
            idVec3 curOrigin;
            idReachability[] reach = {null};

            if (NOT(aas)) {
                return origin;
            }

            areaNum = PointReachableAreaNum(origin);
            targetAreaNum = PointReachableAreaNum(target);

            if (0 == areaNum || 0 == targetAreaNum) {
                return origin;
            }

            if ((areaNum == targetAreaNum) || PointVisible(origin)) {
                return origin;
            }

            curAreaNum = areaNum;
            curOrigin = origin;

            for (i = 0; i < 10; i++) {

                if (!aas.RouteToGoalArea(curAreaNum, curOrigin, targetAreaNum, travelFlags, travelTime, reach)) {
                    break;
                }

                if (NOT(reach[0])) {
                    return target;
                }

                curAreaNum = reach[0].toAreaNum;
                curOrigin = reach[0].end;

                if (PointVisible(curOrigin)) {
                    return curOrigin;
                }
            }

            return origin;
        }

        /*
         ===================
         idAI::CalculateAttackOffsets

         calculate joint positions on attack frames so we can do proper "can hit" tests
         ===================
         */
        protected void CalculateAttackOffsets() {
            idDeclModelDef modelDef;
            int num;
            int i;
            int frame;
            frameCommand_t[] command = {null};
            idMat3 axis = new idMat3();
            idAnim anim;
            int/*jointHandle_t*/ joint;

            modelDef = animator.ModelDef();
            if (null == modelDef) {
                return;
            }
            num = modelDef.NumAnims();

            // needs to be off while getting the offsets so that we account for the distance the monster moves in the attack anim
            animator.RemoveOriginOffset(false);

            // anim number 0 is reserved for non-existant anims.  to avoid off by one issues, just allocate an extra spot for
            // launch offsets so that anim number can be used without subtracting 1.
            missileLaunchOffset.SetGranularity(1);
            missileLaunchOffset.SetNum(num + 1);
            missileLaunchOffset.oSet(0, new idVec3());

            for (i = 1; i <= num; i++) {
                missileLaunchOffset.oSet(i, new idVec3());
                anim = modelDef.GetAnim(i);
                if (anim != null) {
                    frame = anim.FindFrameForFrameCommand(FC_LAUNCHMISSILE, command);
                    if (frame >= 0) {
                        joint = animator.GetJointHandle(command[0].string.toString());
                        if (joint == INVALID_JOINT) {
                            gameLocal.Error("Invalid joint '%s' on 'launch_missile' frame command on frame %d of model '%s'", command[0].string.toString(), frame, modelDef.GetName());
                        }
                        GetJointTransformForAnim(joint, i, FRAME2MS(frame), missileLaunchOffset.oGet(i), axis);
                    }
                }
            }

            animator.RemoveOriginOffset(true);
        }

        protected void PlayCinematic() {
            String[] animName = {null};

            if (current_cinematic >= num_cinematics) {
                if (g_debugCinematic.GetBool()) {
                    gameLocal.Printf("%d: '%s' stop\n", gameLocal.framenum, GetName());
                }
                if (!spawnArgs.GetBool("cinematic_no_hide")) {
                    Hide();
                }
                current_cinematic = 0;
                ActivateTargets(gameLocal.GetLocalPlayer());
                fl.neverDormant = false;
                return;
            }

            Show();
            current_cinematic++;

            allowJointMod = false;
            allowEyeFocus = false;

            spawnArgs.GetString(va("anim%d", current_cinematic), null, animName);
            if (null == animName) {
                gameLocal.Warning("missing 'anim%d' key on %s", current_cinematic, name);
                return;
            }

            if (g_debugCinematic.GetBool()) {
                gameLocal.Printf("%d: '%s' start '%s'\n", gameLocal.framenum, GetName(), animName);
            }

            headAnim.animBlendFrames = 0;
            headAnim.lastAnimBlendFrames = 0;
            headAnim.BecomeIdle();

            legsAnim.animBlendFrames = 0;
            legsAnim.lastAnimBlendFrames = 0;
            legsAnim.BecomeIdle();

            torsoAnim.animBlendFrames = 0;
            torsoAnim.lastAnimBlendFrames = 0;
            ProcessEvent(AI_PlayAnim, ANIMCHANNEL_TORSO, animName);

            // make sure our model gets updated
            animator.ForceUpdate();

            // update the anim bounds
            UpdateAnimation();
            UpdateVisuals();
            Present();

            if (head.GetEntity() != null) {
                // since the body anim was updated, we need to run physics to update the position of the head
                RunPhysics();

                // make sure our model gets updated
                head.GetEntity().GetAnimator().ForceUpdate();

                // update the anim bounds
                head.GetEntity().UpdateAnimation();
                head.GetEntity().UpdateVisuals();
                head.GetEntity().Present();
            }

            fl.neverDormant = true;
        }

        // movement
        @Override
        public void ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {
            // FIXME: Jim take a look at this and see if this is a reasonable thing to do
            // instead of a spawnArg flag.. Sabaoth is the only slide monster ( and should be the only one for D3 )
            // and we don't want him taking physics impulses as it can knock him off the path
            if (move.moveType != MOVETYPE_STATIC && move.moveType != MOVETYPE_SLIDE) {
                super.ApplyImpulse(ent, id, point, impulse);
            }
        }

        protected void GetMoveDelta(final idMat3 oldaxis, final idMat3 axis, idVec3 delta) {
            idVec3 oldModelOrigin;
            idVec3 modelOrigin;

            animator.GetDelta(gameLocal.time - gameLocal.msec, gameLocal.time, delta);
            delta.oSet(axis.oMultiply(delta));

            if (modelOffset != getVec3_zero()) {
                // the pivot of the monster's model is around its origin, and not around the bounding
                // box's origin, so we have to compensate for this when the model is offset so that
                // the monster still appears to rotate around it's origin.
                oldModelOrigin = modelOffset.oMultiply(oldaxis);
                modelOrigin = modelOffset.oMultiply(axis);
                delta.oPluSet(oldModelOrigin.oMinus(modelOrigin));
            }

            delta.oMulSet(physicsObj.GetGravityAxis());
        }

        protected void CheckObstacleAvoidance(final idVec3 goalPos, idVec3 newPos) {
            idEntity obstacle;
            obstaclePath_s path = new obstaclePath_s();
            idVec3 dir;
            float dist;
            boolean foundPath;

            if (ignore_obstacles) {
                newPos.oSet(goalPos);
                move.obstacle = null;
                return;
            }

            final idVec3 origin = physicsObj.GetOrigin();

            obstacle = null;
            AI_OBSTACLE_IN_PATH._(false);
            foundPath = FindPathAroundObstacles(physicsObj, aas, enemy.GetEntity(), origin, goalPos, path);
            if (ai_showObstacleAvoidance.GetBool()) {
                gameRenderWorld.DebugLine(colorBlue, goalPos.oPlus(new idVec3(1.0f, 1.0f, 0.0f)), goalPos.oPlus(new idVec3(1.0f, 1.0f, 64.0f)), gameLocal.msec);
                gameRenderWorld.DebugLine(foundPath ? colorYellow : colorRed, path.seekPos, path.seekPos.oPlus(new idVec3(0.0f, 0.0f, 64.0f)), gameLocal.msec);
            }

            if (!foundPath) {
                // couldn't get around obstacles
                if (path.firstObstacle != null) {
                    AI_OBSTACLE_IN_PATH._(true);
                    if (physicsObj.GetAbsBounds().Expand(2.0f).IntersectsBounds(path.firstObstacle.GetPhysics().GetAbsBounds())) {
                        obstacle = path.firstObstacle;
                    }
                } else if (path.startPosObstacle != null) {
                    AI_OBSTACLE_IN_PATH._(true);
                    if (physicsObj.GetAbsBounds().Expand(2.0f).IntersectsBounds(path.startPosObstacle.GetPhysics().GetAbsBounds())) {
                        obstacle = path.startPosObstacle;
                    }
                } else {
                    // Blocked by wall
                    move.moveStatus = MOVE_STATUS_BLOCKED_BY_WALL;
                }
//#if 0
//	} else if ( path.startPosObstacle ) {
//		// check if we're past where the our origin was pushed out of the obstacle
//		dir = goalPos - origin;
//		dir.Normalize();
//		dist = ( path.seekPos - origin ) * dir;
//		if ( dist < 1.0f ) {
//			AI_OBSTACLE_IN_PATH = true;
//			obstacle = path.startPosObstacle;
//		}
//#endif
            } else if (path.seekPosObstacle != null) {
                // if the AI is very close to the path.seekPos already and path.seekPosObstacle != NULL
                // then we want to push the path.seekPosObstacle entity out of the way
                AI_OBSTACLE_IN_PATH._(true);

                // check if we're past where the goalPos was pushed out of the obstacle
                dir = goalPos.oMinus(origin);
                dir.Normalize();
                dist = (path.seekPos.oMinus(origin)).oMultiply(dir);
                if (dist < 1.0f) {
                    obstacle = path.seekPosObstacle;
                }
            }

            // if we had an obstacle, set our move status based on the type, and kick it out of the way if it's a moveable
            if (obstacle != null) {
                if (obstacle.IsType(idActor.class)) {
                    // monsters aren't kickable
                    if (obstacle == enemy.GetEntity()) {
                        move.moveStatus = MOVE_STATUS_BLOCKED_BY_ENEMY;
                    } else {
                        move.moveStatus = MOVE_STATUS_BLOCKED_BY_MONSTER;
                    }
                } else {
                    // try kicking the object out of the way
                    move.moveStatus = MOVE_STATUS_BLOCKED_BY_OBJECT;
                }
                newPos.oSet(obstacle.GetPhysics().GetOrigin());
                //newPos = path.seekPos;
                move.obstacle.oSet(obstacle);
            } else {
                newPos.oSet(path.seekPos);
                move.obstacle = null;
            }
        }

        protected void DeadMove() {
            idVec3 delta = new idVec3();
            monsterMoveResult_t moveResult;

            idVec3 org = physicsObj.GetOrigin();

            GetMoveDelta(viewAxis, viewAxis, delta);
            physicsObj.SetDelta(delta);

            RunPhysics();

            moveResult = physicsObj.GetMoveResult();
            AI_ONGROUND._(physicsObj.OnGround());
        }

        protected void AnimMove() {
            idVec3 goalPos = new idVec3();
            idVec3 delta = new idVec3();
            idVec3 goalDelta;
            float goalDist;
            monsterMoveResult_t moveResult;
            idVec3 newDest = new idVec3();

            idVec3 oldOrigin = physicsObj.GetOrigin();
            idMat3 oldAxis = viewAxis;

            AI_BLOCKED._(false);

            if (etoi(move.moveCommand) < etoi(NUM_NONMOVING_COMMANDS)) {
                move.lastMoveOrigin.Zero();
                move.lastMoveTime = gameLocal.time;
            }

            move.obstacle = null;
            if ((move.moveCommand == MOVE_FACE_ENEMY) && enemy.GetEntity() != null) {
                TurnToward(lastVisibleEnemyPos);
                goalPos = oldOrigin;
            } else if ((move.moveCommand == MOVE_FACE_ENTITY) && move.goalEntity.GetEntity() != null) {
                TurnToward(move.goalEntity.GetEntity().GetPhysics().GetOrigin());
                goalPos = oldOrigin;
            } else if (GetMovePos(goalPos)) {
                if (move.moveCommand != MOVE_WANDER) {
                    CheckObstacleAvoidance(goalPos, newDest);
                    TurnToward(newDest);
                } else {
                    TurnToward(goalPos);
                }
            }

            Turn();

            if (move.moveCommand == MOVE_SLIDE_TO_POSITION) {
                if (gameLocal.time < move.startTime + move.duration) {
                    goalPos = move.moveDest.oMinus(move.moveDir.oMultiply(MS2SEC(move.startTime + move.duration - gameLocal.time)));
                    delta = goalPos.oMinus(oldOrigin);
                    delta.z = 0.0f;
                } else {
                    delta = move.moveDest.oMinus(oldOrigin);
                    delta.z = 0.0f;
                    StopMove(MOVE_STATUS_DONE);
                }
            } else if (allowMove) {
                GetMoveDelta(oldAxis, viewAxis, delta);
            } else {
                delta.Zero();
            }

            if (move.moveCommand == MOVE_TO_POSITION) {
                goalDelta = move.moveDest.oMinus(oldOrigin);
                goalDist = goalDelta.LengthFast();
                if (goalDist < delta.LengthFast()) {
                    delta = goalDelta;
                }
            }

            physicsObj.SetDelta(delta);
            physicsObj.ForceDeltaMove(disableGravity);

            RunPhysics();

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorCyan, oldOrigin, physicsObj.GetOrigin(), 5000);
            }

            moveResult = physicsObj.GetMoveResult();
            if (!af_push_moveables && attack.Length() != 0 && TestMelee()) {
                DirectDamage(attack, enemy.GetEntity());
            } else {
                idEntity blockEnt = physicsObj.GetSlideMoveEntity();
                if (blockEnt != null && blockEnt.IsType(idMoveable.class) && blockEnt.GetPhysics().IsPushable()) {
                    KickObstacles(viewAxis.oGet(0), kickForce, blockEnt);
                }
            }

            BlockedFailSafe();

            AI_ONGROUND._(physicsObj.OnGround());

            idVec3 org = physicsObj.GetOrigin();
            if (oldOrigin != org) {
                TouchTriggers();
            }

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugBounds(colorMagenta, physicsObj.GetBounds(), org, gameLocal.msec);
                gameRenderWorld.DebugBounds(colorMagenta, physicsObj.GetBounds(), move.moveDest, gameLocal.msec);
                gameRenderWorld.DebugLine(colorYellow, org.oPlus(EyeOffset()), org.oPlus(EyeOffset().oPlus(viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis().oMultiply(16.0f)))), gameLocal.msec, true);
                DrawRoute();
            }
        }

        protected void SlideMove() {
            idVec3 goalPos = new idVec3();
            idVec3 delta = new idVec3();
            idVec3 goalDelta;
            float goalDist;
            monsterMoveResult_t moveResult;
            idVec3 newDest = new idVec3();

            idVec3 oldOrigin = physicsObj.GetOrigin();
            idMat3 oldAxis = viewAxis;

            AI_BLOCKED._(false);

            if (etoi(move.moveCommand) < etoi(NUM_NONMOVING_COMMANDS)) {
                move.lastMoveOrigin.Zero();
                move.lastMoveTime = gameLocal.time;
            }

            move.obstacle = null;
            if ((move.moveCommand == MOVE_FACE_ENEMY) && enemy.GetEntity() != null) {
                TurnToward(lastVisibleEnemyPos);
                goalPos = move.moveDest;
            } else if ((move.moveCommand == MOVE_FACE_ENTITY) && move.goalEntity.GetEntity() != null) {
                TurnToward(move.goalEntity.GetEntity().GetPhysics().GetOrigin());
                goalPos = move.moveDest;
            } else if (GetMovePos(goalPos)) {
                CheckObstacleAvoidance(goalPos, newDest);
                TurnToward(newDest);
                goalPos = newDest;
            }

            if (move.moveCommand == MOVE_SLIDE_TO_POSITION) {
                if (gameLocal.time < move.startTime + move.duration) {
                    goalPos = move.moveDest.oMinus(move.moveDir.oMultiply(MS2SEC(move.startTime + move.duration - gameLocal.time)));
                } else {
                    goalPos = move.moveDest;
                    StopMove(MOVE_STATUS_DONE);
                }
            }

            if (move.moveCommand == MOVE_TO_POSITION) {
                goalDelta = move.moveDest.oMinus(oldOrigin);
                goalDist = goalDelta.LengthFast();
                if (goalDist < delta.LengthFast()) {
                    delta = goalDelta;
                }
            }

            idVec3 vel = physicsObj.GetLinearVelocity();
            float z = vel.z;
            idVec3 predictedPos = oldOrigin.oPlus(vel.oMultiply(AI_SEEK_PREDICTION));

            // seek the goal position
            goalDelta = goalPos.oMinus(predictedPos);
            vel.oMinSet(vel.oMultiply(AI_FLY_DAMPENING * MS2SEC(gameLocal.msec)));
            vel.oPluSet(goalDelta.oMultiply(MS2SEC(gameLocal.msec)));

            // cap our speed
            vel.Truncate(fly_speed);
            vel.z = z;
            physicsObj.SetLinearVelocity(vel);
            physicsObj.UseVelocityMove(true);
            RunPhysics();

            if ((move.moveCommand == MOVE_FACE_ENEMY) && enemy.GetEntity() != null) {
                TurnToward(lastVisibleEnemyPos);
            } else if ((move.moveCommand == MOVE_FACE_ENTITY) && move.goalEntity.GetEntity() != null) {
                TurnToward(move.goalEntity.GetEntity().GetPhysics().GetOrigin());
            } else if (move.moveCommand != MOVE_NONE) {
                if (vel.ToVec2().LengthSqr() > 0.1f) {
                    TurnToward(vel.ToYaw());
                }
            }
            Turn();

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorCyan, oldOrigin, physicsObj.GetOrigin(), 5000);
            }

            moveResult = physicsObj.GetMoveResult();
            if (!af_push_moveables && attack.Length() != 0 && TestMelee()) {
                DirectDamage(attack, enemy.GetEntity());
            } else {
                idEntity blockEnt = physicsObj.GetSlideMoveEntity();
                if (blockEnt != null && blockEnt.IsType(idMoveable.class) && blockEnt.GetPhysics().IsPushable()) {
                    KickObstacles(viewAxis.oGet(0), kickForce, blockEnt);
                }
            }

            BlockedFailSafe();

            AI_ONGROUND._(physicsObj.OnGround());

            idVec3 org = physicsObj.GetOrigin();
            if (oldOrigin != org) {
                TouchTriggers();
            }

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugBounds(colorMagenta, physicsObj.GetBounds(), org, gameLocal.msec);
                gameRenderWorld.DebugBounds(colorMagenta, physicsObj.GetBounds(), move.moveDest, gameLocal.msec);
                gameRenderWorld.DebugLine(colorYellow, org.oPlus(EyeOffset()), org.oPlus(EyeOffset().oPlus(viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis().oMultiply(16.0f)))), gameLocal.msec, true);
                DrawRoute();
            }
        }

        protected void AdjustFlyingAngles() {
            idVec3 vel;
            float speed;
            float roll;
            float pitch;

            vel = physicsObj.GetLinearVelocity();

            speed = vel.Length();
            if (speed < 5.0f) {
                roll = 0.0f;
                pitch = 0.0f;
            } else {
                roll = vel.oMultiply(viewAxis.oGet(1).oMultiply(-fly_roll_scale / fly_speed));
                if (roll > fly_roll_max) {
                    roll = fly_roll_max;
                } else if (roll < -fly_roll_max) {
                    roll = -fly_roll_max;
                }

                pitch = vel.oMultiply(viewAxis.oGet(2).oMultiply(-fly_pitch_scale / fly_speed));
                if (pitch > fly_pitch_max) {
                    pitch = fly_pitch_max;
                } else if (pitch < -fly_pitch_max) {
                    pitch = -fly_pitch_max;
                }
            }

            fly_roll = fly_roll * 0.95f + roll * 0.05f;
            fly_pitch = fly_pitch * 0.95f + pitch * 0.05f;

            if (flyTiltJoint != INVALID_JOINT) {
                animator.SetJointAxis(flyTiltJoint, JOINTMOD_WORLD, new idAngles(fly_pitch, 0.0f, fly_roll).ToMat3());
            } else {
                viewAxis = new idAngles(fly_pitch, current_yaw, fly_roll).ToMat3();
            }
        }

        protected void AddFlyBob(idVec3 vel) {
            idVec3 fly_bob_add;
            float t;

            if (fly_bob_strength != 0) {
                t = MS2SEC(gameLocal.time + entityNumber * 497);
                fly_bob_add = (viewAxis.oGet(1).oMultiply(idMath.Sin16(t * fly_bob_horz)).oPlus(viewAxis.oGet(2).oMultiply(idMath.Sin16(t * fly_bob_vert)))).oMultiply(fly_bob_strength);
                vel.oPluSet(fly_bob_add.oMultiply(MS2SEC(gameLocal.msec)));
                if (ai_debugMove.GetBool()) {
                    final idVec3 origin = physicsObj.GetOrigin();
                    gameRenderWorld.DebugArrow(colorOrange, origin, origin.oPlus(fly_bob_add), 0);
                }
            }
        }

        protected void AdjustFlyHeight(idVec3 vel, final idVec3 goalPos) {
            final idVec3 origin = physicsObj.GetOrigin();
            predictedPath_s path = new predictedPath_s();
            idVec3 end;
            idVec3 dest;
            trace_s[] trace = {null};
            idActor enemyEnt;
            boolean goLower;

            // make sure we're not flying too high to get through doors
            goLower = false;
            if (origin.z > goalPos.z) {
                dest = goalPos;
                dest.z = origin.z + 128.0f;
                idAI.PredictPath(this, aas, goalPos, dest.oMinus(origin), 1000, 1000, SE_BLOCKED, path);
                if (path.endPos.z < origin.z) {
                    idVec3 addVel = Seek(vel, origin, path.endPos, AI_SEEK_PREDICTION);
                    vel.z += addVel.z;
                    goLower = true;
                }

                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugBounds(goLower ? colorRed : colorGreen, physicsObj.GetBounds(), path.endPos, gameLocal.msec);
                }
            }

            if (!goLower) {
                // make sure we don't fly too low
                end = origin;

                enemyEnt = enemy.GetEntity();
                if (enemyEnt != null) {
                    end.z = lastVisibleEnemyPos.z + lastVisibleEnemyEyeOffset.z + fly_offset;
                } else {
                    // just use the default eye height for the player
                    end.z = goalPos.z + DEFAULT_FLY_OFFSET + fly_offset;
                }

                gameLocal.clip.Translation(trace, origin, end, physicsObj.GetClipModel(), getMat3_identity(), MASK_MONSTERSOLID, this);
                vel.oPluSet(Seek(vel, origin, trace[0].endpos, AI_SEEK_PREDICTION));
            }
        }

        protected void FlySeekGoal(idVec3 vel, idVec3 goalPos) {
            idVec3 seekVel;

            // seek the goal position
            seekVel = Seek(vel, physicsObj.GetOrigin(), goalPos, AI_SEEK_PREDICTION);
            seekVel.oMulSet(fly_seek_scale);
            vel.oPluSet(seekVel);
        }

        protected void AdjustFlySpeed(idVec3 vel) {
            float speed;

            // apply dampening
            vel.oMinSet(vel.oMultiply(AI_FLY_DAMPENING * MS2SEC(gameLocal.msec)));

            // gradually speed up/slow down to desired speed
            speed = vel.Normalize();
            speed += (move.speed - speed) * MS2SEC(gameLocal.msec);
            if (speed < 0.0f) {
                speed = 0.0f;
            } else if (move.speed != 0 && (speed > move.speed)) {
                speed = move.speed;
            }

            vel.oMulSet(speed);
        }

        protected void FlyTurn() {
            if (move.moveCommand == MOVE_FACE_ENEMY) {
                TurnToward(lastVisibleEnemyPos);
            } else if ((move.moveCommand == MOVE_FACE_ENTITY) && move.goalEntity.GetEntity() != null) {
                TurnToward(move.goalEntity.GetEntity().GetPhysics().GetOrigin());
            } else if (move.speed > 0.0f) {
                final idVec3 vel = physicsObj.GetLinearVelocity();
                if (vel.ToVec2().LengthSqr() > 0.1f) {
                    TurnToward(vel.ToYaw());
                }
            }
            Turn();
        }

        protected void FlyMove() {
            idVec3 goalPos = new idVec3();
            idVec3 oldorigin;
            idVec3 newDest = new idVec3();

            AI_BLOCKED._(false);
            if ((move.moveCommand != MOVE_NONE) && ReachedPos(move.moveDest, move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
            }

            if (ai_debugMove.GetBool()) {
                gameLocal.Printf("%d: %s: %s, vel = %.2f, sp = %.2f, maxsp = %.2f\n", gameLocal.time, name, moveCommandString[etoi(move.moveCommand)], physicsObj.GetLinearVelocity().Length(), move.speed, fly_speed);
            }

            if (move.moveCommand != MOVE_TO_POSITION_DIRECT) {
                idVec3 vel = physicsObj.GetLinearVelocity();

                if (GetMovePos(goalPos)) {
                    CheckObstacleAvoidance(goalPos, newDest);
                    goalPos = newDest;
                }

                if (move.speed != 0) {
                    FlySeekGoal(vel, goalPos);
                }

                // add in bobbing
                AddFlyBob(vel);

                if (enemy.GetEntity() != null && (move.moveCommand != MOVE_TO_POSITION)) {
                    AdjustFlyHeight(vel, goalPos);
                }

                AdjustFlySpeed(vel);

                physicsObj.SetLinearVelocity(vel);
            }

            // turn
            FlyTurn();

            // run the physics for this frame
            oldorigin = physicsObj.GetOrigin();
            physicsObj.UseFlyMove(true);
            physicsObj.UseVelocityMove(false);
            physicsObj.SetDelta(getVec3_zero());
            physicsObj.ForceDeltaMove(disableGravity);
            RunPhysics();

            monsterMoveResult_t moveResult = physicsObj.GetMoveResult();
            if (!af_push_moveables && attack.Length() != 0 && TestMelee()) {
                DirectDamage(attack, enemy.GetEntity());
            } else {
                idEntity blockEnt = physicsObj.GetSlideMoveEntity();
                if (blockEnt != null && blockEnt.IsType(idMoveable.class) && blockEnt.GetPhysics().IsPushable()) {
                    KickObstacles(viewAxis.oGet(0), kickForce, blockEnt);
                } else if (moveResult == MM_BLOCKED) {
                    move.blockTime = gameLocal.time + 500;
                    AI_BLOCKED._(true);
                }
            }

            idVec3 org = physicsObj.GetOrigin();
            if (oldorigin != org) {
                TouchTriggers();
            }

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorCyan, oldorigin, physicsObj.GetOrigin(), 4000);
                gameRenderWorld.DebugBounds(colorOrange, physicsObj.GetBounds(), org, gameLocal.msec);
                gameRenderWorld.DebugBounds(colorMagenta, physicsObj.GetBounds(), move.moveDest, gameLocal.msec);
                gameRenderWorld.DebugLine(colorRed, org, org.oPlus(physicsObj.GetLinearVelocity()), gameLocal.msec, true);
                gameRenderWorld.DebugLine(colorBlue, org, goalPos, gameLocal.msec, true);
                gameRenderWorld.DebugLine(colorYellow, org.oPlus(EyeOffset()), org.oPlus(EyeOffset().oPlus(viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis().oMultiply(16.0f)))), gameLocal.msec, true);
                DrawRoute();
            }
        }

        protected void StaticMove() {
            idActor enemyEnt = enemy.GetEntity();

            if (AI_DEAD._()) {
                return;
            }

            if ((move.moveCommand == MOVE_FACE_ENEMY) && enemyEnt != null) {
                TurnToward(lastVisibleEnemyPos);
            } else if ((move.moveCommand == MOVE_FACE_ENTITY) && move.goalEntity.GetEntity() != null) {
                TurnToward(move.goalEntity.GetEntity().GetPhysics().GetOrigin());
            } else if (move.moveCommand != MOVE_NONE) {
                TurnToward(move.moveDest);
            }
            Turn();

            physicsObj.ForceDeltaMove(true); // disable gravity
            RunPhysics();

            AI_ONGROUND._(false);

            if (!af_push_moveables && attack.Length() != 0 && TestMelee()) {
                DirectDamage(attack, enemyEnt);
            }

            if (ai_debugMove.GetBool()) {
                final idVec3 org = physicsObj.GetOrigin();
                gameRenderWorld.DebugBounds(colorMagenta, physicsObj.GetBounds(), org, gameLocal.msec);
                gameRenderWorld.DebugLine(colorBlue, org, move.moveDest, gameLocal.msec, true);
                gameRenderWorld.DebugLine(colorYellow, org.oPlus(EyeOffset()), org.oPlus(EyeOffset().oPlus(viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis().oMultiply(16.0f)))), gameLocal.msec, true);
            }
        }

        // damage
        @Override
        public boolean Pain(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            idActor actor;

            AI_PAIN._(super.Pain(inflictor, attacker, damage, dir, location));
            AI_DAMAGE._(true);

            // force a blink
            blink_time = 0;

            // ignore damage from self
            if (attacker != this) {
                if (inflictor != null) {
                    AI_SPECIAL_DAMAGE._(inflictor.spawnArgs.GetInt("special_damage") * 1f);
                } else {
                    AI_SPECIAL_DAMAGE._(0f);
                }

                if (enemy.GetEntity() != attacker && attacker.IsType(idActor.class)) {
                    actor = (idActor) attacker;
                    if ((ReactionTo(actor) & ATTACK_ON_DAMAGE) != 0) {
                        gameLocal.AlertAI(actor);
                        SetEnemy(actor);
                    }
                }
            }

            return (AI_PAIN._() /*!= 0*/);
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            idAngles ang;
            String[] modelDeath = {null};

            // make sure the monster is activated
            EndAttack();

            if (g_debugDamage.GetBool()) {
                gameLocal.Printf("Damage: joint: '%s', zone '%s'\n", animator.GetJointName(location),
                        GetDamageGroup(location));
            }

            if (inflictor != null) {
                AI_SPECIAL_DAMAGE._(inflictor.spawnArgs.GetInt("special_damage") * 1f);
            } else {
                AI_SPECIAL_DAMAGE._(0f);
            }

            if (AI_DEAD._()) {
                AI_PAIN._(true);
                AI_DAMAGE._(true);
                return;
            }

            // stop all voice sounds
            StopSound(etoi(SND_CHANNEL_VOICE), false);
            if (head.GetEntity() != null) {
                head.GetEntity().StopSound(etoi(SND_CHANNEL_VOICE), false);
                head.GetEntity().GetAnimator().ClearAllAnims(gameLocal.time, 100);
            }

            disableGravity = false;
            move.moveType = MOVETYPE_DEAD;
            af_push_moveables = false;

            physicsObj.UseFlyMove(false);
            physicsObj.ForceDeltaMove(false);

            // end our looping ambient sound
            StopSound(etoi(SND_CHANNEL_AMBIENT), false);

            if (attacker != null && attacker.IsType(idActor.class)) {
                gameLocal.AlertAI((idActor) attacker);
            }

            // activate targets
            ActivateTargets(attacker);

            RemoveAttachments();
            RemoveProjectile();
            StopMove(MOVE_STATUS_DONE);

            ClearEnemy();
            AI_DEAD._(true);

            // make monster nonsolid
            physicsObj.SetContents(0);
            physicsObj.GetClipModel().Unlink();

            Unbind();

            if (StartRagdoll()) {
                StartSound("snd_death", SND_CHANNEL_VOICE, 0, false, null);
            }

            if (spawnArgs.GetString("model_death", "", modelDeath)) {
                // lost soul is only case that does not use a ragdoll and has a model_death so get the death sound in here
                StartSound("snd_death", SND_CHANNEL_VOICE, 0, false, null);
                renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                SetModel(modelDeath[0]);
                physicsObj.SetLinearVelocity(getVec3_zero());
                physicsObj.PutToRest();
                physicsObj.DisableImpact();
            }

            restartParticles = false;

            state = GetScriptFunction("state_Killed");
            SetState(state);
            SetWaitState("");

            idKeyValue kv = spawnArgs.MatchPrefix("def_drops", null);
            while (kv != null) {
                idDict args = new idDict();

                args.Set("classname", kv.GetValue());
                args.Set("origin", physicsObj.GetOrigin().ToString());
                gameLocal.SpawnEntityDef(args);
                kv = spawnArgs.MatchPrefix("def_drops", kv);
            }

            if ((attacker != null && attacker.IsType(idPlayer.class)) && (inflictor != null && !inflictor.IsType(idSoulCubeMissile.class))) {
                ((idPlayer) attacker).AddAIKill();
            }
        }

        // navigation
        protected void KickObstacles(final idVec3 dir, float force, idEntity alwaysKick) {
            int i, numListedClipModels;
            idBounds clipBounds;
            idEntity obEnt;
            idClipModel clipModel;
            idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            int clipmask;
            idVec3 org;
            idVec3 forceVec;
            idVec3 delta;
            idVec2 perpendicular = new idVec2();

            org = physicsObj.GetOrigin();

            // find all possible obstacles
            clipBounds = physicsObj.GetAbsBounds();
            clipBounds.TranslateSelf(dir.oMultiply(32.0f));
            clipBounds.ExpandSelf(8.0f);
            clipBounds.AddPoint(org);
            clipmask = physicsObj.GetClipMask();
            numListedClipModels = gameLocal.clip.ClipModelsTouchingBounds(clipBounds, clipmask, clipModelList, MAX_GENTITIES);
            for (i = 0; i < numListedClipModels; i++) {
                clipModel = clipModelList[i];
                obEnt = clipModel.GetEntity();
                if (obEnt == alwaysKick) {
                    // we'll kick this one outside the loop
                    continue;
                }

                if (!clipModel.IsTraceModel()) {
                    continue;
                }

                if (obEnt.IsType(idMoveable.class) && obEnt.GetPhysics().IsPushable()) {
                    delta = obEnt.GetPhysics().GetOrigin().oMinus(org);
                    delta.NormalizeFast();
                    perpendicular.x = -delta.y;
                    perpendicular.y = delta.x;
                    delta.z += 0.5f;
                    delta.ToVec2().oPluSet(perpendicular.oMultiply(gameLocal.random.CRandomFloat() * 0.5f));
                    forceVec = delta.oMultiply(force * obEnt.GetPhysics().GetMass());
                    obEnt.ApplyImpulse(this, 0, obEnt.GetPhysics().GetOrigin(), forceVec);
                }
            }

            if (alwaysKick != null) {
                delta = alwaysKick.GetPhysics().GetOrigin().oMinus(org);
                delta.NormalizeFast();
                perpendicular.x = -delta.y;
                perpendicular.y = delta.x;
                delta.z += 0.5f;
                delta.ToVec2().oPluSet(perpendicular.oMultiply(gameLocal.random.CRandomFloat() * 0.5f));
                forceVec = delta.oMultiply(force * alwaysKick.GetPhysics().GetMass());
                alwaysKick.ApplyImpulse(this, 0, alwaysKick.GetPhysics().GetOrigin(), forceVec);
            }
        }

        protected boolean ReachedPos(final idVec3 pos, final moveCommand_t moveCommand) {
            if (move.moveType == MOVETYPE_SLIDE) {
                idBounds bnds = new idBounds(new idVec3(-4, -4.0f, -8.0f), new idVec3(4.0f, 4.0f, 64.0f));
                bnds.TranslateSelf(physicsObj.GetOrigin());
                if (bnds.ContainsPoint(pos)) {
                    return true;
                }
            } else {
                if ((moveCommand == MOVE_TO_ENEMY) || (moveCommand == MOVE_TO_ENTITY)) {
                    if (physicsObj.GetAbsBounds().IntersectsBounds(new idBounds(pos).Expand(8.0f))) {
                        return true;
                    }
                } else {
                    idBounds bnds = new idBounds(new idVec3(-16.0f, -16.0f, -8.0f), new idVec3(16.0f, 16.0f, 64.0f));
                    bnds.TranslateSelf(physicsObj.GetOrigin());
                    if (bnds.ContainsPoint(pos)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /*
         =====================
         idAI::TravelDistance

         Returns the approximate travel distance from one position to the goal, or if no AAS, the straight line distance.

         This is feakin' slow, so it's not good to do it too many times per frame.  It also is slower the further you
         are from the goal, so try to break the goals up into shorter distances.
         =====================
         */
        protected float TravelDistance(final idVec3 start, final idVec3 end) {
            int fromArea;
            int toArea;
            float dist;
            idVec2 delta;
//            aasPath_s path;

            if (NOT(aas)) {
                // no aas, so just take the straight line distance
                delta = end.ToVec2().oMinus(start.ToVec2());
                dist = delta.LengthFast();

                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugLine(colorBlue, start, end, gameLocal.msec, false);
                    gameRenderWorld.DrawText(va("%d", (int) dist), (start.oPlus(end)).oMultiply(0.5f), 0.1f, colorWhite, gameLocal.GetLocalPlayer().viewAngles.ToMat3());
                }

                return dist;
            }

            fromArea = PointReachableAreaNum(start);
            toArea = PointReachableAreaNum(end);

            if (0 == fromArea || 0 == toArea) {
                // can't seem to get there
                return -1;
            }

            if (fromArea == toArea) {
                // same area, so just take the straight line distance
                delta = end.ToVec2().oMinus(start.ToVec2());
                dist = delta.LengthFast();

                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugLine(colorBlue, start, end, gameLocal.msec, false);
                    gameRenderWorld.DrawText(va("%d", (int) dist), (start.oPlus(end)).oMultiply(0.5f), 0.1f, colorWhite, gameLocal.GetLocalPlayer().viewAngles.ToMat3());
                }

                return dist;
            }

            idReachability[] reach = {null};
            int[] travelTime = {0};
            if (!aas.RouteToGoalArea(fromArea, start, toArea, travelFlags, travelTime, reach)) {
                return -1;
            }

            if (ai_debugMove.GetBool()) {
                if (move.moveType == MOVETYPE_FLY) {
                    aas.ShowFlyPath(start, toArea, end);
                } else {
                    aas.ShowWalkPath(start, toArea, end);
                }
            }

            return travelTime[0];
        }

        protected int PointReachableAreaNum(final idVec3 pos, final float boundsScale /*= 2.0f*/) {
            int areaNum;
            idVec3 size;
            idBounds bounds = new idBounds();

            if (NOT(aas)) {
                return 0;
            }

            size = aas.GetSettings().boundingBoxes[0].oGet(1).oMultiply(boundsScale);
            bounds.oSet(0, size.oNegative());
            size.z = 32.0f;
            bounds.oSet(1, size);

            if (move.moveType == MOVETYPE_FLY) {
                areaNum = aas.PointReachableAreaNum(pos, bounds, AREA_REACHABLE_WALK | AREA_REACHABLE_FLY);
            } else {
                areaNum = aas.PointReachableAreaNum(pos, bounds, AREA_REACHABLE_WALK);
            }

            return areaNum;
        }

        protected int PointReachableAreaNum(final idVec3 pos) {
            return PointReachableAreaNum(pos, 2.0f);
        }

        protected boolean PathToGoal(aasPath_s path, int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin) {
            idVec3 org;
            idVec3 goal;

            if (NOT(aas)) {
                return false;
            }

            org = origin;
            aas.PushPointIntoAreaNum(areaNum, org);
            if (0 == areaNum) {
                return false;
            }

            goal = goalOrigin;
            aas.PushPointIntoAreaNum(goalAreaNum, goal);
            if (0 == goalAreaNum) {
                return false;
            }

            if (move.moveType == MOVETYPE_FLY) {
                return aas.FlyPathToGoal(path, areaNum, org, goalAreaNum, goal, travelFlags);
            } else {
                return aas.WalkPathToGoal(path, areaNum, org, goalAreaNum, goal, travelFlags);
            }
        }

        protected void DrawRoute() {
            if (aas != null && move.toAreaNum != 0 && move.moveCommand != MOVE_NONE && move.moveCommand != MOVE_WANDER && move.moveCommand != MOVE_FACE_ENEMY
                    && move.moveCommand != MOVE_FACE_ENTITY && move.moveCommand != MOVE_TO_POSITION_DIRECT) {
                if (move.moveType == MOVETYPE_FLY) {
                    aas.ShowFlyPath(physicsObj.GetOrigin(), move.toAreaNum, move.moveDest);
                } else {
                    aas.ShowWalkPath(physicsObj.GetOrigin(), move.toAreaNum, move.moveDest);
                }
            }
        }

        protected boolean GetMovePos(idVec3 seekPos) {
            int areaNum;
            aasPath_s path = new aasPath_s();
            boolean result;
            idVec3 org;

            org = physicsObj.GetOrigin();
            seekPos.oSet(org);

            switch (move.moveCommand) {
                case MOVE_NONE:
                    seekPos.oSet(move.moveDest);
                    return false;

                case MOVE_FACE_ENEMY:
                case MOVE_FACE_ENTITY:
                    seekPos.oSet(move.moveDest);
                    return false;

                case MOVE_TO_POSITION_DIRECT:
                    seekPos.oSet(move.moveDest);
                    if (ReachedPos(move.moveDest, move.moveCommand)) {
                        StopMove(MOVE_STATUS_DONE);
                    }
                    return false;

                case MOVE_SLIDE_TO_POSITION:
                    seekPos.oSet(org);
                    return false;
            }

            if (move.moveCommand == MOVE_TO_ENTITY) {
                MoveToEntity(move.goalEntity.GetEntity());
            }

            move.moveStatus = MOVE_STATUS_MOVING;
            result = false;
            if (gameLocal.time > move.blockTime) {
                if (move.moveCommand == MOVE_WANDER) {
                    move.moveDest = org.oPlus(viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis().oMultiply(256.0f)));
                } else {
                    if (ReachedPos(move.moveDest, move.moveCommand)) {
                        StopMove(MOVE_STATUS_DONE);
                        seekPos.oSet(org);
                        return false;
                    }
                }

                if (aas != null && move.toAreaNum != 0) {
                    areaNum = PointReachableAreaNum(org);
                    if (PathToGoal(path, areaNum, org, move.toAreaNum, move.moveDest)) {
                        seekPos.oSet(path.moveGoal);
                        result = true;
                        move.nextWanderTime = 0;
                    } else {
                        AI_DEST_UNREACHABLE._(true);
                    }
                }
            }

            if (!result) {
                // wander around
                if ((gameLocal.time > move.nextWanderTime) || !StepDirection(move.wanderYaw)) {
                    result = NewWanderDir(move.moveDest);
                    if (!result) {
                        StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                        AI_DEST_UNREACHABLE._(true);
                        seekPos.oSet(org);
                        return false;
                    }
                } else {
                    result = true;
                }

                seekPos.oSet(org.oPlus(move.moveDir.oMultiply(2048.0f)));
                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugLine(colorYellow, org, seekPos, gameLocal.msec, true);
                }
            } else {
                AI_DEST_UNREACHABLE._(false);
            }

            if (result && (ai_debugMove.GetBool())) {
                gameRenderWorld.DebugLine(colorCyan, physicsObj.GetOrigin(), seekPos);
            }

            return result;
        }

        protected boolean MoveDone() {
            return (move.moveCommand == MOVE_NONE);
        }

        protected boolean EntityCanSeePos(idActor actor, final idVec3 actorOrigin, final idVec3 pos) {
            idVec3 eye, point;
            trace_s[] results = {null};
            pvsHandle_t handle;

            handle = gameLocal.pvs.SetupCurrentPVS(actor.GetPVSAreas(), actor.GetNumPVSAreas());

            if (!gameLocal.pvs.InCurrentPVS(handle, GetPVSAreas(), GetNumPVSAreas())) {
                gameLocal.pvs.FreeCurrentPVS(handle);
                return false;
            }

            gameLocal.pvs.FreeCurrentPVS(handle);

            eye = actorOrigin.oPlus(actor.EyeOffset());

            point = pos;
            point.oPluSet(2, 1.0f);

            physicsObj.DisableClip();

            gameLocal.clip.TracePoint(results, eye, point, MASK_SOLID, actor);
            if (results[0].fraction >= 1.0f || (gameLocal.GetTraceEntity(results[0]) == this)) {
                physicsObj.EnableClip();
                return true;
            }

            final idBounds bounds = physicsObj.GetBounds();
            point.oPluSet(2, bounds.oGet(1, 2) - bounds.oGet(0, 2));

            gameLocal.clip.TracePoint(results, eye, point, MASK_SOLID, actor);
            physicsObj.EnableClip();

            return results[0].fraction >= 1.0f || (gameLocal.GetTraceEntity(results[0]) == this);
        }

        protected void BlockedFailSafe() {
            if (!ai_blockedFailSafe.GetBool() || blockedRadius < 0.0f) {
                return;
            }
            if (!physicsObj.OnGround() || enemy.GetEntity() == null
                    || (physicsObj.GetOrigin().oMinus(move.lastMoveOrigin)).LengthSqr() > Square(blockedRadius)) {
                move.lastMoveOrigin = physicsObj.GetOrigin();
                move.lastMoveTime = gameLocal.time;
            }
            if (move.lastMoveTime < gameLocal.time - blockedMoveTime) {
                if (lastAttackTime < gameLocal.time - blockedAttackTime) {
                    AI_BLOCKED._(true);
                    move.lastMoveTime = gameLocal.time;
                }
            }
        }

        // movement control
        protected void StopMove(moveStatus_t status) {
            AI_MOVE_DONE._(true);
            AI_FORWARD._(false);
            move.moveCommand = MOVE_NONE;
            move.moveStatus = status;
            move.toAreaNum = 0;
            move.goalEntity = null;
            move.moveDest = physicsObj.GetOrigin();
            AI_DEST_UNREACHABLE._(false);
            AI_OBSTACLE_IN_PATH._(false);
            AI_BLOCKED._(false);
            move.startTime = gameLocal.time;
            move.duration = 0;
            move.range = 0.0f;
            move.speed = 0.0f;
            move.anim = 0;
            move.moveDir.Zero();
            move.lastMoveOrigin.Zero();
            move.lastMoveTime = gameLocal.time;
        }

        /*
         =====================
         idAI::FaceEnemy

         Continually face the enemy's last known position.  MoveDone is always true in this case.
         =====================
         */
        protected boolean FaceEnemy() {
            idActor enemyEnt = enemy.GetEntity();
            if (null == enemyEnt) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            TurnToward(lastVisibleEnemyPos);
            move.goalEntity.oSet(enemyEnt);
            move.moveDest = physicsObj.GetOrigin();
            move.moveCommand = MOVE_FACE_ENEMY;
            move.moveStatus = MOVE_STATUS_WAITING;
            move.startTime = gameLocal.time;
            move.speed = 0.0f;
            AI_MOVE_DONE._(true);
            AI_FORWARD._(false);
            AI_DEST_UNREACHABLE._(false);

            return true;
        }

        /*
         =====================
         idAI::FaceEntity

         Continually face the entity position.  MoveDone is always true in this case.
         =====================
         */
        protected boolean FaceEntity(idEntity ent) {
            if (null == ent) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            idVec3 entityOrg = ent.GetPhysics().GetOrigin();
            TurnToward(entityOrg);
            move.goalEntity.oSet(ent);
            move.moveDest = physicsObj.GetOrigin();
            move.moveCommand = MOVE_FACE_ENTITY;
            move.moveStatus = MOVE_STATUS_WAITING;
            move.startTime = gameLocal.time;
            move.speed = 0.0f;
            AI_MOVE_DONE._(true);
            AI_FORWARD._(false);
            AI_DEST_UNREACHABLE._(false);

            return true;
        }

        protected boolean DirectMoveToPosition(final idVec3 pos) {
            if (ReachedPos(pos, move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            move.moveDest = pos;
            move.goalEntity = null;
            move.moveCommand = MOVE_TO_POSITION_DIRECT;
            move.moveStatus = MOVE_STATUS_MOVING;
            move.startTime = gameLocal.time;
            move.speed = fly_speed;
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(true);

            if (move.moveType == MOVETYPE_FLY) {
                idVec3 dir = pos.oMinus(physicsObj.GetOrigin());
                dir.Normalize();
                dir.oMulSet(fly_speed);
                physicsObj.SetLinearVelocity(dir);
            }

            return true;
        }

        protected boolean MoveToEnemyHeight() {
            idActor enemyEnt = enemy.GetEntity();

            if (null == enemyEnt || (move.moveType != MOVETYPE_FLY)) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            move.moveDest.z = lastVisibleEnemyPos.z + enemyEnt.EyeOffset().z + fly_offset;
            move.goalEntity.oSet(enemyEnt);
            move.moveCommand = MOVE_TO_ENEMYHEIGHT;
            move.moveStatus = MOVE_STATUS_MOVING;
            move.startTime = gameLocal.time;
            move.speed = 0.0f;
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(false);

            return true;
        }

        protected boolean MoveOutOfRange(idEntity ent, float range) {
            int areaNum;
            aasObstacle_s[] obstacle = new aasObstacle_s[1];
            aasGoal_s goal = new aasGoal_s();
//            idBounds bounds;
            idVec3 pos;

            if (null == aas || null == ent) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                AI_DEST_UNREACHABLE._(true);
                return false;
            }

            final idVec3 org = physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);

            // consider the entity the monster is getting close to as an obstacle
            obstacle[0].absBounds = ent.GetPhysics().GetAbsBounds();

            if (ent == enemy.GetEntity()) {
                pos = lastVisibleEnemyPos;
            } else {
                pos = ent.GetPhysics().GetOrigin();
            }

            idAASFindAreaOutOfRange findGoal = new idAASFindAreaOutOfRange(pos, range);
            if (!aas.FindNearestGoal(goal, areaNum, org, pos, travelFlags, obstacle, 1, findGoal)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                AI_DEST_UNREACHABLE._(true);
                return false;
            }

            if (ReachedPos(goal.origin, move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            move.moveDest = goal.origin;
            move.toAreaNum = goal.areaNum;
            move.goalEntity.oSet(ent);
            move.moveCommand = MOVE_OUT_OF_RANGE;
            move.moveStatus = MOVE_STATUS_MOVING;
            move.range = range;
            move.speed = fly_speed;
            move.startTime = gameLocal.time;
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(true);

            return true;
        }

        protected boolean MoveToAttackPosition(idEntity ent, int attack_anim) {
            int areaNum;
            aasObstacle_s[] obstacle = new aasObstacle_s[1];
            aasGoal_s goal = new aasGoal_s();
            idBounds bounds;
            idVec3 pos;

            if (null == aas || null == ent) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                AI_DEST_UNREACHABLE._(true);
                return false;
            }

            final idVec3 org = physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);

            // consider the entity the monster is getting close to as an obstacle
            obstacle[0].absBounds = ent.GetPhysics().GetAbsBounds();

            if (ent == enemy.GetEntity()) {
                pos = lastVisibleEnemyPos;
            } else {
                pos = ent.GetPhysics().GetOrigin();
            }

            idAASFindAttackPosition findGoal = new idAASFindAttackPosition(this, physicsObj.GetGravityAxis(), ent, pos, missileLaunchOffset.oGet(attack_anim));
            if (!aas.FindNearestGoal(goal, areaNum, org, pos, travelFlags, obstacle, 1, findGoal)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                AI_DEST_UNREACHABLE._(true);
                return false;
            }

            move.moveDest = goal.origin;
            move.toAreaNum = goal.areaNum;
            move.goalEntity.oSet(ent);
            move.moveCommand = MOVE_TO_ATTACK_POSITION;
            move.moveStatus = MOVE_STATUS_MOVING;
            move.speed = fly_speed;
            move.startTime = gameLocal.time;
            move.anim = attack_anim;
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(true);

            return true;
        }

        protected boolean MoveToEnemy() {
            int areaNum;
            aasPath_s path = new aasPath_s();
            idActor enemyEnt = enemy.GetEntity();

            if (null == enemyEnt) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            if (ReachedPos(lastVisibleReachableEnemyPos, MOVE_TO_ENEMY)) {
                if (!ReachedPos(lastVisibleEnemyPos, MOVE_TO_ENEMY) || !AI_ENEMY_VISIBLE._()) {
                    StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                    AI_DEST_UNREACHABLE._(true);
                    return false;
                }
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            idVec3 pos = lastVisibleReachableEnemyPos;

            move.toAreaNum = 0;
            if (aas != null) {
                move.toAreaNum = PointReachableAreaNum(pos);
                aas.PushPointIntoAreaNum(move.toAreaNum, pos);

                areaNum = PointReachableAreaNum(physicsObj.GetOrigin());
                if (!PathToGoal(path, areaNum, physicsObj.GetOrigin(), move.toAreaNum, pos)) {
                    AI_DEST_UNREACHABLE._(true);
                    return false;
                }
            }

            if (0 == move.toAreaNum) {
                // if only trying to update the enemy position
                if (move.moveCommand == MOVE_TO_ENEMY) {
                    if (NOT(aas)) {
                        // keep the move destination up to date for wandering
                        move.moveDest = pos;
                    }
                    return false;
                }

                if (!NewWanderDir(pos)) {
                    StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                    AI_DEST_UNREACHABLE._(true);
                    return false;
                }
            }

            if (move.moveCommand != MOVE_TO_ENEMY) {
                move.moveCommand = MOVE_TO_ENEMY;
                move.startTime = gameLocal.time;
            }

            move.moveDest = pos;
            move.goalEntity.oSet(enemyEnt);
            move.speed = fly_speed;
            move.moveStatus = MOVE_STATUS_MOVING;
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(true);

            return true;
        }

        protected boolean MoveToEntity(idEntity ent) {
            int areaNum;
            aasPath_s path = new aasPath_s();
            idVec3 pos;

            if (null == ent) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            pos = ent.GetPhysics().GetOrigin();
            if ((move.moveType != MOVETYPE_FLY) && ((move.moveCommand != MOVE_TO_ENTITY) || (move.goalEntityOrigin != pos))) {
                ent.GetFloorPos(64.0f, pos);
            }

            if (ReachedPos(pos, MOVE_TO_ENTITY)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            move.toAreaNum = 0;
            if (aas != null) {
                move.toAreaNum = PointReachableAreaNum(pos);
                aas.PushPointIntoAreaNum(move.toAreaNum, pos);

                areaNum = PointReachableAreaNum(physicsObj.GetOrigin());
                if (!PathToGoal(path, areaNum, physicsObj.GetOrigin(), move.toAreaNum, pos)) {
                    AI_DEST_UNREACHABLE._(true);
                    return false;
                }
            }

            if (0 == move.toAreaNum) {
                // if only trying to update the entity position
                if (move.moveCommand == MOVE_TO_ENTITY) {
                    if (NOT(aas)) {
                        // keep the move destination up to date for wandering
                        move.moveDest = pos;
                    }
                    return false;
                }

                if (!NewWanderDir(pos)) {
                    StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                    AI_DEST_UNREACHABLE._(true);
                    return false;
                }
            }

            if ((move.moveCommand != MOVE_TO_ENTITY) || (!move.goalEntity.GetEntity().equals(ent))) {
                move.startTime = gameLocal.time;
                move.goalEntity.oSet(ent);
                move.moveCommand = MOVE_TO_ENTITY;
            }

            move.moveDest = pos;
            move.goalEntityOrigin = ent.GetPhysics().GetOrigin();
            move.moveStatus = MOVE_STATUS_MOVING;
            move.speed = fly_speed;
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(true);

            return true;
        }

        protected boolean MoveToPosition(final idVec3 pos) {
            idVec3 org;
            int areaNum;
            aasPath_s path = new aasPath_s();

            if (ReachedPos(pos, move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            org = pos;
            move.toAreaNum = 0;
            if (aas != null) {
                move.toAreaNum = PointReachableAreaNum(org);
                aas.PushPointIntoAreaNum(move.toAreaNum, org);

                areaNum = PointReachableAreaNum(physicsObj.GetOrigin());
                if (!PathToGoal(path, areaNum, physicsObj.GetOrigin(), move.toAreaNum, org)) {
                    StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                    AI_DEST_UNREACHABLE._(true);
                    return false;
                }
            }

            if (0 == move.toAreaNum && !NewWanderDir(org)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                AI_DEST_UNREACHABLE._(true);
                return false;
            }

            move.moveDest = org;
            move.goalEntity = null;
            move.moveCommand = MOVE_TO_POSITION;
            move.moveStatus = MOVE_STATUS_MOVING;
            move.startTime = gameLocal.time;
            move.speed = fly_speed;
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(true);

            return true;
        }

        protected boolean MoveToCover(idEntity entity, final idVec3 hideFromPos) {
            int areaNum;
            aasObstacle_s[] obstacle = {new aasObstacle_s()};
            aasGoal_s hideGoal = new aasGoal_s();
//            idBounds bounds;

            if (null == aas || null == entity) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                AI_DEST_UNREACHABLE._(true);
                return false;
            }

            final idVec3 org = physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);

            // consider the entity the monster tries to hide from as an obstacle
            obstacle[0].absBounds = entity.GetPhysics().GetAbsBounds();

            idAASFindCover findCover = new idAASFindCover(hideFromPos);
            if (!aas.FindNearestGoal(hideGoal, areaNum, org, hideFromPos, travelFlags, obstacle, 1, findCover)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                AI_DEST_UNREACHABLE._(true);
                return false;
            }

            if (ReachedPos(hideGoal.origin, move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            move.moveDest = hideGoal.origin;
            move.toAreaNum = hideGoal.areaNum;
            move.goalEntity.oSet(entity);
            move.moveCommand = MOVE_TO_COVER;
            move.moveStatus = MOVE_STATUS_MOVING;
            move.startTime = gameLocal.time;
            move.speed = fly_speed;
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(true);

            return true;
        }

        protected boolean SlideToPosition(final idVec3 pos, float time) {
            StopMove(MOVE_STATUS_DONE);

            move.moveDest = pos;
            move.goalEntity = null;
            move.moveCommand = MOVE_SLIDE_TO_POSITION;
            move.moveStatus = MOVE_STATUS_MOVING;
            move.startTime = gameLocal.time;
            move.duration = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(time));
            AI_MOVE_DONE._(false);
            AI_DEST_UNREACHABLE._(false);
            AI_FORWARD._(false);

            if (move.duration > 0) {
                move.moveDir = (pos.oMinus(physicsObj.GetOrigin())).oDivide(MS2SEC(move.duration));
                if (move.moveType != MOVETYPE_FLY) {
                    move.moveDir.z = 0.0f;
                }
                move.speed = move.moveDir.LengthFast();
            }

            return true;
        }

        protected boolean WanderAround() {
            StopMove(MOVE_STATUS_DONE);

            move.moveDest = physicsObj.GetOrigin().oPlus(viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis().oMultiply(256.0f)));
            if (!NewWanderDir(move.moveDest)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                AI_DEST_UNREACHABLE._(true);
                return false;
            }

            move.moveCommand = MOVE_WANDER;
            move.moveStatus = MOVE_STATUS_MOVING;
            move.startTime = gameLocal.time;
            move.speed = fly_speed;
            AI_MOVE_DONE._(false);
            AI_FORWARD._(true);

            return true;
        }

        protected boolean StepDirection(float dir) {
            predictedPath_s path = new predictedPath_s();
            idVec3 org;

            move.wanderYaw = dir;
            move.moveDir = new idAngles(0, move.wanderYaw, 0).ToForward();

            org = physicsObj.GetOrigin();

            idAI.PredictPath(this, aas, org, move.moveDir.oMultiply(48.0f), 1000, 1000, (move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (path.blockingEntity != null && ((move.moveCommand == MOVE_TO_ENEMY) || (move.moveCommand == MOVE_TO_ENTITY)) && (path.blockingEntity == move.goalEntity.GetEntity())) {
                // don't report being blocked if we ran into our goal entity
                return true;
            }

            if ((move.moveType == MOVETYPE_FLY) && (path.endEvent == SE_BLOCKED)) {
                float z;

                move.moveDir = path.endVelocity.oMultiply(1.0f / 48.0f);

                // trace down to the floor and see if we can go forward
                idAI.PredictPath(this, aas, org, new idVec3(0.0f, 0.0f, -1024.0f), 1000, 1000, SE_BLOCKED, path);

                idVec3 floorPos = path.endPos;
                idAI.PredictPath(this, aas, floorPos, move.moveDir.oMultiply(48.0f), 1000, 1000, SE_BLOCKED, path);
                if (0 == path.endEvent) {
                    move.moveDir.z = -1.0f;
                    return true;
                }

                // trace up to see if we can go over something and go forward
                idAI.PredictPath(this, aas, org, new idVec3(0.0f, 0.0f, 256.0f), 1000, 1000, SE_BLOCKED, path);

                idVec3 ceilingPos = path.endPos;

                for (z = org.z; z <= ceilingPos.z + 64.0f; z += 64.0f) {
                    idVec3 start = new idVec3();
                    if (z <= ceilingPos.z) {
                        start.x = org.x;
                        start.y = org.y;
                        start.z = z;
                    } else {
                        start = ceilingPos;
                    }
                    idAI.PredictPath(this, aas, start, move.moveDir.oMultiply(48.0f), 1000, 1000, SE_BLOCKED, path);
                    if (0 == path.endEvent) {
                        move.moveDir.z = 1.0f;
                        return true;
                    }
                }
                return false;
            }

            return (path.endEvent == 0);
        }

        protected boolean NewWanderDir(final idVec3 dest) {
            float deltax, deltay;
            float[] d = new float[3];
            float tdir, olddir, turnaround;

            move.nextWanderTime = (int) (gameLocal.time + (gameLocal.random.RandomFloat() * 500 + 500));

            olddir = idMath.AngleNormalize360((int) (current_yaw / 45) * 45);
            turnaround = idMath.AngleNormalize360(olddir - 180);

            idVec3 org = physicsObj.GetOrigin();
            deltax = dest.x - org.x;
            deltay = dest.y - org.y;
            if (deltax > 10) {
                d[ 1] = 0;
            } else if (deltax < -10) {
                d[ 1] = 180;
            } else {
                d[ 1] = DI_NODIR;
            }

            if (deltay < -10) {
                d[ 2] = 270;
            } else if (deltay > 10) {
                d[ 2] = 90;
            } else {
                d[ 2] = DI_NODIR;
            }

            // try direct route
            if (d[ 1] != DI_NODIR && d[ 2] != DI_NODIR) {
                if (d[ 1] == 0) {
                    tdir = d[ 2] == 90 ? 45 : 315;
                } else {
                    tdir = d[ 2] == 90 ? 135 : 215;
                }

                if (tdir != turnaround && StepDirection(tdir)) {
                    return true;
                }
            }

            // try other directions
            if (((gameLocal.random.RandomInt() & 1) != 0) || abs(deltay) > abs(deltax)) {
                tdir = d[ 1];
                d[ 1] = d[ 2];
                d[ 2] = tdir;
            }

            if (d[ 1] != DI_NODIR && d[ 1] != turnaround && StepDirection(d[1])) {
                return true;
            }

            if (d[ 2] != DI_NODIR && d[ 2] != turnaround && StepDirection(d[ 2])) {
                return true;
            }

            // there is no direct path to the player, so pick another direction
            if (olddir != DI_NODIR && StepDirection(olddir)) {
                return true;
            }

            // randomly determine direction of search
            if ((gameLocal.random.RandomInt() & 1) == 1) {
                for (tdir = 0; tdir <= 315; tdir += 45) {
                    if (tdir != turnaround && StepDirection(tdir)) {
                        return true;
                    }
                }
            } else {
                for (tdir = 315; tdir >= 0; tdir -= 45) {
                    if (tdir != turnaround && StepDirection(tdir)) {
                        return true;
                    }
                }
            }

            if (turnaround != DI_NODIR && StepDirection(turnaround)) {
                return true;
            }

            // can't move
            StopMove(MOVE_STATUS_DEST_UNREACHABLE);
            return false;
        }

        // effects
        protected idDeclParticle SpawnParticlesOnJoint(particleEmitter_s pe, final idStr particleName, final String jointName) {
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            if (!isNotNullOrEmpty(particleName)) {
//		memset( &pe, 0, sizeof( pe ) );//TODO:
                return pe.particle;
            }

            pe.joint = animator.GetJointHandle(jointName);
            if (pe.joint == INVALID_JOINT) {
                gameLocal.Warning("Unknown particleJoint '%s' on '%s'", jointName, name);
                pe.time = 0;
                pe.particle = null;
            } else {
                animator.GetJointTransform(pe.joint, gameLocal.time, origin, axis);
                origin = renderEntity.origin.oPlus(origin.oMultiply(renderEntity.axis));

                BecomeActive(TH_UPDATEPARTICLES);
                if (0 == gameLocal.time) {
                    // particles with time of 0 don't show, so set the time differently on the first frame
                    pe.time = 1;
                } else {
                    pe.time = gameLocal.time;
                }
                pe.particle = ((idDeclParticle) declManager.FindType(DECL_PARTICLE, particleName));
                gameLocal.smokeParticles.EmitSmoke(pe.particle, pe.time, gameLocal.random.CRandomFloat(), origin, axis);
            }

            return pe.particle;
        }

        protected void SpawnParticles(final String keyName) {
            idKeyValue kv = spawnArgs.MatchPrefix(keyName, null);
            while (kv != null) {
                particleEmitter_s pe = new particleEmitter_s();

                idStr particleName = kv.GetValue();

                if (particleName.Length() != 0) {

                    idStr jointName = kv.GetValue();
                    int dash = jointName.Find('-');
                    if (dash > 0) {
                        particleName = particleName.Left(dash);
                        jointName = jointName.Right(jointName.Length() - dash - 1);
                    }

                    SpawnParticlesOnJoint(pe, particleName, jointName.toString());
                    particles.Append(pe);
                }

                kv = spawnArgs.MatchPrefix(keyName, kv);
            }
        }
//
//        protected boolean ParticlesActive();
//

        // turning
        protected boolean FacingIdeal() {
            float diff;

            if (0 == turnRate) {
                return true;
            }

            diff = idMath.AngleNormalize180(current_yaw - ideal_yaw);
            if (idMath.Fabs(diff) < 0.01f) {
                // force it to be exact
                current_yaw = ideal_yaw;
                return true;
            }

            return false;
        }

        protected void Turn() {
            float diff;
            float diff2;
            float turnAmount;
            animFlags_t animflags;

            if (0 == turnRate) {
                return;
            }

            // check if the animator has marker this anim as non-turning
            if (!legsAnim.Disabled() && !legsAnim.AnimDone(0)) {
                animflags = legsAnim.GetAnimFlags();
            } else {
                animflags = torsoAnim.GetAnimFlags();
            }
            if (animflags.ai_no_turn) {
                return;
            }

            if (anim_turn_angles != 0 && animflags.anim_turn) {
                idMat3 rotateAxis = new idMat3();

                // set the blend between no turn and full turn
                float frac = anim_turn_amount / anim_turn_angles;
                animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(0, 1.0f - frac);
                animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(1, frac);
                animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(0, 1.0f - frac);
                animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(1, frac);

                // get the total rotation from the start of the anim
                animator.GetDeltaRotation(0, gameLocal.time, rotateAxis);
                current_yaw = idMath.AngleNormalize180(anim_turn_yaw + rotateAxis.oGet(0).ToYaw());
            } else {
                diff = idMath.AngleNormalize180(ideal_yaw - current_yaw);
                turnVel += AI_TURN_SCALE * diff * MS2SEC(gameLocal.msec);
                if (turnVel > turnRate) {
                    turnVel = turnRate;
                } else if (turnVel < -turnRate) {
                    turnVel = -turnRate;
                }
                turnAmount = turnVel * MS2SEC(gameLocal.msec);
                if ((diff >= 0.0f) && (turnAmount >= diff)) {
                    turnVel = diff / MS2SEC(gameLocal.msec);
                    turnAmount = diff;
                } else if ((diff <= 0.0f) && (turnAmount <= diff)) {
                    turnVel = diff / MS2SEC(gameLocal.msec);
                    turnAmount = diff;
                }
                current_yaw += turnAmount;
                current_yaw = idMath.AngleNormalize180(current_yaw);
                diff2 = idMath.AngleNormalize180(ideal_yaw - current_yaw);
                if (idMath.Fabs(diff2) < 0.1f) {
                    current_yaw = ideal_yaw;
                }
            }

            viewAxis = new idAngles(0, current_yaw, 0).ToMat3();

            if (ai_debugMove.GetBool()) {
                final idVec3 org = physicsObj.GetOrigin();
                gameRenderWorld.DebugLine(colorRed, org, org.oPlus(new idAngles(0, ideal_yaw, 0).ToForward().oMultiply(64)), gameLocal.msec);
                gameRenderWorld.DebugLine(colorGreen, org, org.oPlus(new idAngles(0, current_yaw, 0).ToForward().oMultiply(48)), gameLocal.msec);
                gameRenderWorld.DebugLine(colorYellow, org, org.oPlus(new idAngles(0, current_yaw + turnVel, 0).ToForward().oMultiply(32)), gameLocal.msec);
            }
        }

        protected boolean TurnToward(float yaw) {
            ideal_yaw = idMath.AngleNormalize180(yaw);
            boolean result = FacingIdeal();
            return result;
        }

        protected boolean TurnToward(final idVec3 pos) {
            idVec3 dir;
            idVec3 local_dir = new idVec3();
            float lengthSqr;

            dir = pos.oMinus(physicsObj.GetOrigin());
            physicsObj.GetGravityAxis().ProjectVector(dir, local_dir);
            local_dir.z = 0.0f;
            lengthSqr = local_dir.LengthSqr();
            if (lengthSqr > Square(2.0f) || (lengthSqr > Square(0.1f) && enemy.GetEntity() == null)) {
                ideal_yaw = idMath.AngleNormalize180(local_dir.ToYaw());
            }

            boolean result = FacingIdeal();
            return result;
        }

        // enemy management
        protected void ClearEnemy() {
            if (move.moveCommand == MOVE_TO_ENEMY) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            }

            enemyNode.Remove();
            enemy.oSet(null);
            AI_ENEMY_IN_FOV._(false);
            AI_ENEMY_VISIBLE._(false);
            AI_ENEMY_DEAD._(true);

            SetChatSound();
        }

        protected boolean EnemyPositionValid() {
            trace_s[] tr = {null};
            idVec3 muzzle;
            idMat3 axis;

            if (null == enemy.GetEntity()) {
                return false;
            }

            if (AI_ENEMY_VISIBLE._()) {
                return true;
            }

            gameLocal.clip.TracePoint(tr, GetEyePosition(), lastVisibleEnemyPos.oPlus(lastVisibleEnemyEyeOffset), MASK_OPAQUE, this);
            if (tr[0].fraction < 1.0f) {
                // can't see the area yet, so don't know if he's there or not
                return true;
            }

            return false;
        }

        protected void SetEnemyPosition() {
            idActor enemyEnt = enemy.GetEntity();
            int enemyAreaNum;
            int areaNum;
            int lastVisibleReachableEnemyAreaNum = 0;
            aasPath_s path = new aasPath_s();
            idVec3 pos = new idVec3();
            boolean onGround;

            if (null == enemyEnt) {
                return;
            }

            lastVisibleReachableEnemyPos = lastReachableEnemyPos;
            lastVisibleEnemyEyeOffset = enemyEnt.EyeOffset();
            lastVisibleEnemyPos = enemyEnt.GetPhysics().GetOrigin();
            if (move.moveType == MOVETYPE_FLY) {
                pos = lastVisibleEnemyPos;
                onGround = true;
            } else {
                onGround = enemyEnt.GetFloorPos(64.0f, pos);
                if (enemyEnt.OnLadder()) {
                    onGround = false;
                }
            }

            if (!onGround) {
                if (move.moveCommand == MOVE_TO_ENEMY) {
                    AI_DEST_UNREACHABLE._(true);
                }
                return;
            }

            // when we don't have an AAS, we can't tell if an enemy is reachable or not,
            // so just assume that he is.
            if (NOT(aas)) {
                lastVisibleReachableEnemyPos = lastVisibleEnemyPos;
                if (move.moveCommand == MOVE_TO_ENEMY) {
                    AI_DEST_UNREACHABLE._(false);
                }
                enemyAreaNum = 0;
//                areaNum = 0;
            } else {
                lastVisibleReachableEnemyAreaNum = move.toAreaNum;
                enemyAreaNum = PointReachableAreaNum(lastVisibleEnemyPos, 1.0f);
                if (0 == enemyAreaNum) {
                    enemyAreaNum = PointReachableAreaNum(lastReachableEnemyPos, 1.0f);
                    pos = lastReachableEnemyPos;
                }
                if (0 == enemyAreaNum) {
                    if (move.moveCommand == MOVE_TO_ENEMY) {
                        AI_DEST_UNREACHABLE._(true);
                    }
//                    areaNum = 0;
                } else {
                    final idVec3 org = physicsObj.GetOrigin();
                    areaNum = PointReachableAreaNum(org);
                    if (PathToGoal(path, areaNum, org, enemyAreaNum, pos)) {
                        lastVisibleReachableEnemyPos = pos;
                        lastVisibleReachableEnemyAreaNum = enemyAreaNum;
                        if (move.moveCommand == MOVE_TO_ENEMY) {
                            AI_DEST_UNREACHABLE._(false);
                        }
                    } else if (move.moveCommand == MOVE_TO_ENEMY) {
                        AI_DEST_UNREACHABLE._(true);
                    }
                }
            }

            if (move.moveCommand == MOVE_TO_ENEMY) {
                if (NOT(aas)) {
                    // keep the move destination up to date for wandering
                    move.moveDest = lastVisibleReachableEnemyPos;
                } else if (enemyAreaNum != 0) {
                    move.toAreaNum = lastVisibleReachableEnemyAreaNum;
                    move.moveDest = lastVisibleReachableEnemyPos;
                }

                if (move.moveType == MOVETYPE_FLY) {
                    predictedPath_s path2 = new predictedPath_s();
                    idVec3 end = move.moveDest;
                    end.z += enemyEnt.EyeOffset().z + fly_offset;
                    idAI.PredictPath(this, aas, move.moveDest, end.oMinus(move.moveDest), 1000, 1000, SE_BLOCKED, path2);
                    move.moveDest = path2.endPos;
                    move.toAreaNum = PointReachableAreaNum(move.moveDest, 1.0f);
                }
            }
        }

        protected void UpdateEnemyPosition() {
            idActor enemyEnt = enemy.GetEntity();
            int enemyAreaNum;
            int areaNum;
            aasPath_s path = new aasPath_s();
            predictedPath_s predictedPath;
            idVec3 enemyPos = new idVec3();
            boolean onGround;

            if (null == enemyEnt) {
                return;
            }

            final idVec3 org = physicsObj.GetOrigin();

            if (move.moveType == MOVETYPE_FLY) {
                enemyPos = enemyEnt.GetPhysics().GetOrigin();
                onGround = true;
            } else {
                onGround = enemyEnt.GetFloorPos(64.0f, enemyPos);
                if (enemyEnt.OnLadder()) {
                    onGround = false;
                }
            }

            if (onGround) {
                // when we don't have an AAS, we can't tell if an enemy is reachable or not,
                // so just assume that he is.
                if (NOT(aas)) {
//                    enemyAreaNum = 0;
                    lastReachableEnemyPos = enemyPos;
                } else {
                    enemyAreaNum = PointReachableAreaNum(enemyPos, 1.0f);
                    if (enemyAreaNum != 0) {
                        areaNum = PointReachableAreaNum(org);
                        if (PathToGoal(path, areaNum, org, enemyAreaNum, enemyPos)) {
                            lastReachableEnemyPos = enemyPos;
                        }
                    }
                }
            }

            AI_ENEMY_IN_FOV._(false);
            AI_ENEMY_VISIBLE._(false);

            if (CanSee(enemyEnt, false)) {
                AI_ENEMY_VISIBLE._(true);
                if (CheckFOV(enemyEnt.GetPhysics().GetOrigin())) {
                    AI_ENEMY_IN_FOV._(true);
                }

                SetEnemyPosition();
            } else {
                // check if we heard any sounds in the last frame
                if (enemyEnt == gameLocal.GetAlertEntity()) {
                    float dist = (enemyEnt.GetPhysics().GetOrigin().oMinus(org)).LengthSqr();
                    if (dist < Square(AI_HEARING_RANGE)) {
                        SetEnemyPosition();
                    }
                }
            }

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugBounds(colorLtGrey, enemyEnt.GetPhysics().GetBounds(), lastReachableEnemyPos, gameLocal.msec);
                gameRenderWorld.DebugBounds(colorWhite, enemyEnt.GetPhysics().GetBounds(), lastVisibleReachableEnemyPos, gameLocal.msec);
            }
        }

        protected void SetEnemy(idActor newEnemy) {
            int[] enemyAreaNum = {0};

            if (AI_DEAD._()) {
                ClearEnemy();
                return;
            }

            AI_ENEMY_DEAD._(false);
            if (null == newEnemy) {
                ClearEnemy();
            } else if (enemy.GetEntity() != newEnemy) {
                enemy.oSet(newEnemy);
                enemyNode.AddToEnd(newEnemy.enemyList);
                if (newEnemy.health <= 0) {
                    EnemyDead();
                    return;
                }
                // let the monster know where the enemy is
                newEnemy.GetAASLocation(aas, lastReachableEnemyPos, enemyAreaNum);
                SetEnemyPosition();
                SetChatSound();

                lastReachableEnemyPos = lastVisibleEnemyPos;
                lastVisibleReachableEnemyPos = lastReachableEnemyPos;
                enemyAreaNum[0] = PointReachableAreaNum(lastReachableEnemyPos, 1.0f);
                if (aas != null && enemyAreaNum[0] != 0) {
                    aas.PushPointIntoAreaNum(enemyAreaNum[0], lastReachableEnemyPos);
                    lastVisibleReachableEnemyPos = lastReachableEnemyPos;
                }
            }
        }

        // attacks
        protected void CreateProjectileClipModel() {
            if (projectileClipModel == null) {
                idBounds projectileBounds = new idBounds(getVec3_origin());
                projectileBounds.ExpandSelf(projectileRadius);
                projectileClipModel = new idClipModel(new idTraceModel(projectileBounds));
            }
        }

        protected idProjectile CreateProjectile(final idVec3 pos, final idVec3 dir) {
            idEntity[] ent = {null};
            String clsname;

            if (null == projectile.GetEntity()) {
                gameLocal.SpawnEntityDef(projectileDef, ent, false);
                if (null == ent[0]) {
                    clsname = projectileDef.GetString("classname");
                    gameLocal.Error("Could not spawn entityDef '%s'", clsname);
                }

                if (!ent[0].IsType(idProjectile.class)) {
                    clsname = ent[0].GetClassname();
                    gameLocal.Error("'%s' is not an idProjectile", clsname);
                }
                projectile.oSet((idProjectile) ent[0]);
            }

            projectile.GetEntity().Create(this, pos, dir);

            return projectile.GetEntity();
        }

        protected void RemoveProjectile() {
            if (projectile.GetEntity() != null) {
                projectile.GetEntity().PostEventMS(EV_Remove, 0);
                projectile.oSet(null);
            }
        }

        protected idProjectile LaunchProjectile(final String jointname, idEntity target, boolean clampToAttackCone) {
            idVec3 muzzle = new idVec3();
            idVec3 dir = new idVec3();
            idVec3 start;
            trace_s[] tr = {null};
            idBounds projBounds;
            float[] distance = {0};
            idClipModel projClip;
            float attack_accuracy;
            float attack_cone;
            float projectile_spread;
            float diff;
            float angle;
            float spin;
            idAngles ang;
            int num_projectiles;
            int i;
            idMat3 axis = new idMat3();
            idVec3 tmp;
            idProjectile lastProjectile;

            if (null == projectileDef) {
                gameLocal.Warning("%s (%s) doesn't have a projectile specified", name, GetEntityDefName());
                return null;
            }

            attack_accuracy = spawnArgs.GetFloat("attack_accuracy", "7");
            attack_cone = spawnArgs.GetFloat("attack_cone", "70");
            projectile_spread = spawnArgs.GetFloat("projectile_spread", "0");
            num_projectiles = spawnArgs.GetInt("num_projectiles", "1");

            GetMuzzle(jointname, muzzle, axis);

            if (null == projectile.GetEntity()) {
                CreateProjectile(muzzle, axis.oGet(0));
            }

            lastProjectile = projectile.GetEntity();

            if (target != null) {
                tmp = target.GetPhysics().GetAbsBounds().GetCenter().oMinus(muzzle);
                tmp.Normalize();
                axis = tmp.ToMat3();
            } else {
                axis = viewAxis;
            }

            // rotate it because the cone points up by default
            tmp = axis.oGet(2);
            axis.oSet(2, axis.oGet(0));
            axis.oSet(0, tmp.oNegative());

            // make sure the projectile starts inside the monster bounding box
            final idBounds ownerBounds = physicsObj.GetAbsBounds();
            projClip = lastProjectile.GetPhysics().GetClipModel();
            projBounds = projClip.GetBounds().Rotate(axis);

            // check if the owner bounds is bigger than the projectile bounds
            if (((ownerBounds.oGet(1, 0) - ownerBounds.oGet(0, 0)) > (projBounds.oGet(1, 0) - projBounds.oGet(0, 0)))
                    && ((ownerBounds.oGet(1, 1) - ownerBounds.oGet(0, 1)) > (projBounds.oGet(1, 1) - projBounds.oGet(0, 1)))
                    && ((ownerBounds.oGet(1, 2) - ownerBounds.oGet(0, 2)) > (projBounds.oGet(1, 2) - projBounds.oGet(0, 2)))) {
                if ((ownerBounds.oMinus(projBounds)).RayIntersection(muzzle, viewAxis.oGet(0), distance)) {
                    start = muzzle.oPlus(viewAxis.oGet(0).oMultiply(distance[0]));
                } else {
                    start = ownerBounds.GetCenter();
                }
            } else {
                // projectile bounds bigger than the owner bounds, so just start it from the center
                start = ownerBounds.GetCenter();
            }

            gameLocal.clip.Translation(tr, start, muzzle, projClip, axis, MASK_SHOT_RENDERMODEL, this);
            muzzle = tr[0].endpos;

            // set aiming direction
            GetAimDir(muzzle, target, this, dir);
            ang = dir.ToAngles();

            // adjust his aim so it's not perfect.  uses sine based movement so the tracers appear less random in their spread.
            float t = MS2SEC(gameLocal.time + entityNumber * 497);
            ang.pitch += idMath.Sin16(t * 5.1) * attack_accuracy;
            ang.yaw += idMath.Sin16(t * 6.7) * attack_accuracy;

            if (clampToAttackCone) {
                // clamp the attack direction to be within monster's attack cone so he doesn't do
                // things like throw the missile backwards if you're behind him
                diff = idMath.AngleDelta(ang.yaw, current_yaw);
                if (diff > attack_cone) {
                    ang.yaw = current_yaw + attack_cone;
                } else if (diff < -attack_cone) {
                    ang.yaw = current_yaw - attack_cone;
                }
            }

            axis = ang.ToMat3();

            float spreadRad = (float) DEG2RAD(projectile_spread);
            for (i = 0; i < num_projectiles; i++) {
                // spread the projectiles out
                angle = idMath.Sin(spreadRad * gameLocal.random.RandomFloat());
                spin = (float) DEG2RAD(360.0f) * gameLocal.random.RandomFloat();
                dir = axis.oGet(0).oPlus(axis.oGet(2).oMultiply(angle * idMath.Sin(spin)).oMinus(axis.oGet(1).oMultiply(angle * idMath.Cos(spin))));
                dir.Normalize();

                // launch the projectile
                if (null == projectile.GetEntity()) {
                    CreateProjectile(muzzle, dir);
                }
                lastProjectile = projectile.GetEntity();
                lastProjectile.Launch(muzzle, dir, getVec3_origin());
                projectile.oSet(null);
            }

            TriggerWeaponEffects(muzzle);

            lastAttackTime = gameLocal.time;

            return lastProjectile;
        }

        /*
         ================
         idAI::DamageFeedback

         callback function for when another entity received damage from this entity.  damage can be adjusted and returned to the caller.

         FIXME: This gets called when we call idPlayer::CalcDamagePoints from idAI::AttackMelee, which then checks for a saving throw,
         possibly forcing a miss.  This is harmless behavior ATM, but is not intuitive.
         ================
         */
        @Override
        public void DamageFeedback(idEntity victim, idEntity inflictor, int[] damage) {
            if ((victim.equals(this)) && inflictor.IsType(idProjectile.class)) {
                // monsters only get half damage from their own projectiles
                damage[0] = (damage[0] + 1) / 2;  // round up so we don't do 0 damage

            } else if (victim.equals(enemy.GetEntity())) {
                AI_HIT_ENEMY._(true);
            }
        }

        /*
         =====================
         idAI::DirectDamage

         Causes direct damage to an entity

         kickDir is specified in the monster's coordinate system, and gives the direction
         that the view kick and knockback should go
         =====================
         */
        protected void DirectDamage(final String meleeDefName, idEntity ent) {
            final idDict meleeDef;
            final String p;
            idSoundShader shader;

            meleeDef = gameLocal.FindEntityDefDict(meleeDefName, false);
            if (null == meleeDef) {
                gameLocal.Error("Unknown damage def '%s' on '%s'", meleeDefName, name);
            }

            if (!ent.fl.takedamage) {
                final idSoundShader shader2 = declManager.FindSound(meleeDef.GetString("snd_miss"));
                StartSoundShader(shader2, SND_CHANNEL_DAMAGE, 0, false, null);
                return;
            }

            //
            // do the damage
            //
            p = meleeDef.GetString("snd_hit");
            if (isNotNullOrEmpty(p)) {
                shader = declManager.FindSound(p);
                StartSoundShader(shader, SND_CHANNEL_DAMAGE, 0, false, null);
            }

            idVec3 kickDir = new idVec3();
            meleeDef.GetVector("kickDir", "0 0 0", kickDir);

            idVec3 globalKickDir;
            globalKickDir = (viewAxis.oMultiply(physicsObj.GetGravityAxis())).oMultiply(kickDir);

            ent.Damage(this, this, globalKickDir, meleeDefName, 1.0f, INVALID_JOINT);

            // end the attack if we're a multiframe attack
            EndAttack();
        }

        protected void DirectDamage(final idStr meleeDefName, idEntity ent) {
            DirectDamage(meleeDefName.toString(), ent);
        }

        protected boolean TestMelee() {
            trace_s[] trace = {null};
            idActor enemyEnt = enemy.GetEntity();

            if (null == enemyEnt || 0 == melee_range) {
                return false;
            }

            //FIXME: make work with gravity vector
            idVec3 org = physicsObj.GetOrigin();
            final idBounds myBounds = physicsObj.GetBounds();
            idBounds bounds = new idBounds();

            // expand the bounds out by our melee range
            bounds.oSet(0, 0, -melee_range);
            bounds.oSet(0, 1, -melee_range);
            bounds.oSet(0, 2, myBounds.oGet(0, 2) - 4.0f);
            bounds.oSet(1, 0, -melee_range);
            bounds.oSet(1, 1, -melee_range);
            bounds.oSet(1, 2, myBounds.oGet(1, 2) - 4.0f);
            bounds.TranslateSelf(org);

            idVec3 enemyOrg = enemyEnt.GetPhysics().GetOrigin();
            idBounds enemyBounds = enemyEnt.GetPhysics().GetBounds();
            enemyBounds.TranslateSelf(enemyOrg);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugBounds(colorYellow, bounds, getVec3_zero(), gameLocal.msec);
            }

            if (!bounds.IntersectsBounds(enemyBounds)) {
                return false;
            }

            idVec3 start = GetEyePosition();
            idVec3 end = enemyEnt.GetEyePosition();

            gameLocal.clip.TracePoint(trace, start, end, MASK_SHOT_BOUNDINGBOX, this);

            return (trace[0].fraction == 1.0f) || (gameLocal.GetTraceEntity(trace[0]).equals(enemyEnt));
        }

        /*
         =====================
         idAI::AttackMelee

         jointname allows the endpoint to be exactly specified in the model,
         as for the commando tentacle.  If not specified, it will be set to
         the facing direction + melee_range.

         kickDir is specified in the monster's coordinate system, and gives the direction
         that the view kick and knockback should go
         =====================
         */
        protected boolean AttackMelee(final String meleeDefName) {
            idDict meleeDef;
            idActor enemyEnt = enemy.GetEntity();
            String p;
            idSoundShader shader;

            meleeDef = gameLocal.FindEntityDefDict(meleeDefName, false);
            if (null == meleeDef) {
                gameLocal.Error("Unknown melee '%s'", meleeDefName);
            }

            if (null == enemyEnt) {
                p = meleeDef.GetString("snd_miss");
                if (isNotNullOrEmpty(p)) {
                    shader = declManager.FindSound(p);
                    StartSoundShader(shader, SND_CHANNEL_DAMAGE, 0, false, null);
                }
                return false;
            }

            // check for the "saving throw" automatic melee miss on lethal blow
            // stupid place for this.
            boolean forceMiss = false;
            if (enemyEnt.IsType(idPlayer.class) && g_skill.GetInteger() < 2) {
                int[] damage = {0}, armor = {0};
                idPlayer player = (idPlayer) enemyEnt;
                player.CalcDamagePoints(this, this, meleeDef, 1.0f, INVALID_JOINT, damage, armor);

                if (enemyEnt.health <= damage[0]) {
                    int t = gameLocal.time - player.lastSavingThrowTime;
                    if (t > SAVING_THROW_TIME) {
                        player.lastSavingThrowTime = gameLocal.time;
                        t = 0;
                    }
                    if (t < 1000) {
                        gameLocal.Printf("Saving throw.\n");
                        forceMiss = true;
                    }
                }
            }

            // make sure the trace can actually hit the enemy
            if (forceMiss || !TestMelee()) {
                // missed
                p = meleeDef.GetString("snd_miss");
                if (isNotNullOrEmpty(p)) {
                    shader = declManager.FindSound(p);
                    StartSoundShader(shader, SND_CHANNEL_DAMAGE, 0, false, null);
                }
                return false;
            }

            //
            // do the damage
            //
            p = meleeDef.GetString("snd_hit");
            if (isNotNullOrEmpty(p)) {
                shader = declManager.FindSound(p);
                StartSoundShader(shader, SND_CHANNEL_DAMAGE, 0, false, null);
            }

            idVec3 kickDir = new idVec3();
            meleeDef.GetVector("kickDir", "0 0 0", kickDir);

            idVec3 globalKickDir;
            globalKickDir = (viewAxis.oMultiply(physicsObj.GetGravityAxis())).oMultiply(kickDir);

            enemyEnt.Damage(this, this, globalKickDir, meleeDefName, 1.0f, INVALID_JOINT);

            lastAttackTime = gameLocal.time;

            return true;
        }

        protected void BeginAttack(final String name) {
            attack.oSet(name);
            lastAttackTime = gameLocal.time;
        }

        protected void EndAttack() {
            attack.oSet("");
        }

        protected void PushWithAF() {
            int i, j;
            afTouch_s[] touchList = new afTouch_s[MAX_GENTITIES];
            idEntity[] pushed_ents = new idEntity[MAX_GENTITIES];
            idEntity ent;
            idVec3 vel;
            int num_pushed;

            num_pushed = 0;
            af.ChangePose(this, gameLocal.time);
            int num = af.EntitiesTouchingAF(touchList);
            for (i = 0; i < num; i++) {
                if (touchList[ i].touchedEnt.IsType(idProjectile.class)) {
                    // skip projectiles
                    continue;
                }

                // make sure we havent pushed this entity already.  this avoids causing double damage
                for (j = 0; j < num_pushed; j++) {
                    if (pushed_ents[ j] == touchList[ i].touchedEnt) {
                        break;
                    }
                }
                if (j >= num_pushed) {
                    ent = touchList[ i].touchedEnt;
                    pushed_ents[num_pushed++] = ent;
                    vel = ent.GetPhysics().GetAbsBounds().GetCenter().oMinus(touchList[i].touchedByBody.GetWorldOrigin());
                    vel.Normalize();
                    if (attack.Length() != 0 && ent.IsType(idActor.class)) {
                        ent.Damage(this, this, vel, attack.toString(), 1.0f, INVALID_JOINT);
                    } else {
                        ent.GetPhysics().SetLinearVelocity(vel.oMultiply(100.0f), touchList[ i].touchedClipModel.GetId());
                    }
                }
            }
        }

        // special effects
        protected void GetMuzzle(final String jointname, idVec3 muzzle, idMat3 axis) {
            int /*jointHandle_t*/ joint;

            if (!isNotNullOrEmpty(jointname)) {
                muzzle.oSet(physicsObj.GetOrigin().oPlus(viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis().oMultiply(14))));
                muzzle.oMinSet(physicsObj.GetGravityNormal().oMultiply(physicsObj.GetBounds().oGet(1).z * 0.5f));
            } else {
                joint = animator.GetJointHandle(jointname);
                if (joint == INVALID_JOINT) {
                    gameLocal.Error("Unknown joint '%s' on %s", jointname, GetEntityDefName());
                }
                GetJointWorldTransform(joint, gameLocal.time, muzzle, axis);
            }
        }

        protected void InitMuzzleFlash() {
            idStr shader = new idStr();
            idVec3 flashColor = new idVec3();

            spawnArgs.GetString("mtr_flashShader", "muzzleflash", shader);
            spawnArgs.GetVector("flashColor", "0 0 0", flashColor);
            float flashRadius = spawnArgs.GetFloat("flashRadius");
            flashTime = (int) SEC2MS(spawnArgs.GetFloat("flashTime", "0.25"));

//	memset( &worldMuzzleFlash, 0, sizeof ( worldMuzzleFlash ) );
            worldMuzzleFlash = new renderLight_s();

            worldMuzzleFlash.pointLight = true;
            worldMuzzleFlash.shader = declManager.FindMaterial(shader, false);
            worldMuzzleFlash.shaderParms[SHADERPARM_RED] = flashColor.oGet(0);
            worldMuzzleFlash.shaderParms[SHADERPARM_GREEN] = flashColor.oGet(1);
            worldMuzzleFlash.shaderParms[SHADERPARM_BLUE] = flashColor.oGet(2);
            worldMuzzleFlash.shaderParms[SHADERPARM_ALPHA] = 1.0f;
            worldMuzzleFlash.shaderParms[SHADERPARM_TIMESCALE] = 1.0f;
            worldMuzzleFlash.lightRadius.oSet(0, flashRadius);
            worldMuzzleFlash.lightRadius.oSet(1, flashRadius);
            worldMuzzleFlash.lightRadius.oSet(2, flashRadius);

            worldMuzzleFlashHandle = -1;
        }

        protected void TriggerWeaponEffects(final idVec3 muzzle) {
            idVec3 org = new idVec3();
            idMat3 axis = new idMat3();

            if (!g_muzzleFlash.GetBool()) {
                return;
            }

            // muzzle flash
            // offset the shader parms so muzzle flashes show up
            renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            renderEntity.shaderParms[ SHADERPARM_DIVERSITY] = gameLocal.random.CRandomFloat();

            if (flashJointWorld != INVALID_JOINT) {
                GetJointWorldTransform(flashJointWorld, gameLocal.time, org, axis);

                if (worldMuzzleFlash.lightRadius.x > 0.0f) {
                    worldMuzzleFlash.axis = axis;
                    worldMuzzleFlash.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                    if (worldMuzzleFlashHandle != - 1) {
                        gameRenderWorld.UpdateLightDef(worldMuzzleFlashHandle, worldMuzzleFlash);
                    } else {
                        worldMuzzleFlashHandle = gameRenderWorld.AddLightDef(worldMuzzleFlash);
                    }
                    muzzleFlashEnd = gameLocal.time + flashTime;
                    UpdateVisuals();
                }
            }
        }

        protected void UpdateMuzzleFlash() {
            if (worldMuzzleFlashHandle != -1) {
                if (gameLocal.time >= muzzleFlashEnd) {
                    gameRenderWorld.FreeLightDef(worldMuzzleFlashHandle);
                    worldMuzzleFlashHandle = -1;
                } else {
                    idVec3 muzzle = new idVec3();
                    animator.GetJointTransform(flashJointWorld, gameLocal.time, muzzle, worldMuzzleFlash.axis);
                    animator.GetJointTransform(flashJointWorld, gameLocal.time, muzzle, worldMuzzleFlash.axis);
                    muzzle = physicsObj.GetOrigin().oPlus((muzzle.oPlus(modelOffset)).oMultiply(viewAxis.oMultiply(physicsObj.GetGravityAxis())));
                    worldMuzzleFlash.origin = muzzle;
                    gameRenderWorld.UpdateLightDef(worldMuzzleFlashHandle, worldMuzzleFlash);
                }
            }
        }

        @Override
        public boolean UpdateAnimationControllers() {
            idVec3 local;
            idVec3 focusPos;
            idQuat jawQuat;
            idVec3 left;
            idVec3 dir;
            idVec3 orientationJointPos = new idVec3();
            idVec3 localDir = new idVec3();
            idAngles newLookAng = new idAngles();
            idAngles diff;
            idMat3 mat;
            idMat3 axis = new idMat3();
            idMat3 orientationJointAxis = new idMat3();
            idAFAttachment headEnt = head.GetEntity();
            idVec3 eyepos = new idVec3();
            idVec3 pos;
            int i;
            idAngles jointAng = new idAngles();
            float orientationJointYaw;

            if (AI_DEAD._()) {
                return super.UpdateAnimationControllers();
            }

            if (orientationJoint == INVALID_JOINT) {
                orientationJointAxis = viewAxis;
                orientationJointPos = physicsObj.GetOrigin();
                orientationJointYaw = current_yaw;
            } else {
                GetJointWorldTransform(orientationJoint, gameLocal.time, orientationJointPos, orientationJointAxis);
                orientationJointYaw = orientationJointAxis.oGet(2).ToYaw();
                orientationJointAxis = new idAngles(0.0f, orientationJointYaw, 0.0f).ToMat3();
            }

            if (focusJoint != INVALID_JOINT) {
                if (headEnt != null) {
                    headEnt.GetJointWorldTransform(focusJoint, gameLocal.time, eyepos, axis);
                } else {
                    GetJointWorldTransform(focusJoint, gameLocal.time, eyepos, axis);
                }
                eyeOffset.z = eyepos.z - physicsObj.GetOrigin().z;
                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugLine(colorRed, eyepos, eyepos.oPlus(orientationJointAxis.oGet(0).oMultiply(32.0f)), gameLocal.msec);
                }
            } else {
                eyepos = GetEyePosition();
            }

            if (headEnt != null) {
                CopyJointsFromBodyToHead();
            }

            // Update the IK after we've gotten all the joint positions we need, but before we set any joint positions.
            // Getting the joint positions causes the joints to be updated.  The IK gets joint positions itself (which
            // are already up to date because of getting the joints in this function) and then sets their positions, which
            // forces the heirarchy to be updated again next time we get a joint or present the model.  If IK is enabled,
            // or if we have a seperate head, we end up transforming the joints twice per frame.  Characters with no
            // head entity and no ik will only transform their joints once.  Set g_debuganim to the current entity number
            // in order to see how many times an entity transforms the joints per frame.
            super.UpdateAnimationControllers();

            idEntity focusEnt = focusEntity.GetEntity();
            if (!allowJointMod || !allowEyeFocus || (gameLocal.time >= focusTime)) {
                focusPos = GetEyePosition().oPlus(orientationJointAxis.oGet(0).oMultiply(512.0f));
            } else if (focusEnt == null) {
                // keep looking at last position until focusTime is up
                focusPos = currentFocusPos;
            } else if (focusEnt.equals(enemy.GetEntity())) {
                focusPos = lastVisibleEnemyPos.oPlus(lastVisibleEnemyEyeOffset).oMinus(enemy.GetEntity().GetPhysics().GetGravityNormal().oMultiply(eyeVerticalOffset));
            } else if (focusEnt.IsType(idActor.class)) {
                focusPos = ((idActor) focusEnt).GetEyePosition().oMinus(focusEnt.GetPhysics().GetGravityNormal().oMultiply(eyeVerticalOffset));
            } else {
                focusPos = focusEnt.GetPhysics().GetOrigin();
            }
            currentFocusPos = currentFocusPos.oPlus(focusPos.oMinus(currentFocusPos)).oMultiply(eyeFocusRate);
            // determine yaw from origin instead of from focus joint since joint may be offset, which can cause us to bounce between two angles
            dir = focusPos.oMinus(orientationJointPos);
            newLookAng.yaw = idMath.AngleNormalize180(dir.ToYaw() - orientationJointYaw);
            newLookAng.roll = 0.0f;
            newLookAng.pitch = 0.0f;
// #if 0
            // gameRenderWorld.DebugLine( colorRed, orientationJointPos, focusPos, gameLocal.msec );
            // gameRenderWorld.DebugLine( colorYellow, orientationJointPos, orientationJointPos + orientationJointAxis[ 0 ] * 32.0f, gameLocal.msec );
            // gameRenderWorld.DebugLine( colorGreen, orientationJointPos, orientationJointPos + newLookAng.ToForward() * 48.0f, gameLocal.msec );
// #endif
            // determine pitch from joint position
            dir = focusPos.oMinus(eyepos);

            dir.NormalizeFast();

            orientationJointAxis.ProjectVector(dir, localDir);
            newLookAng.pitch = -idMath.AngleNormalize180(localDir.ToPitch());
            newLookAng.roll = 0.0f;
            diff = newLookAng.oMinus(lookAng);
            if (!eyeAng.equals(diff)) {
                eyeAng = diff;
                eyeAng.Clamp(eyeMin, eyeMax);
                idAngles angDelta = diff.oMinus(eyeAng);
                if (!angDelta.Compare(getAng_zero(), 0.1f)) {
                    alignHeadTime = gameLocal.time;
                } else {
                    alignHeadTime = (int) (gameLocal.time + (0.5f + 0.5f * gameLocal.random.RandomFloat()) * focusAlignTime);
                }
            }
            if (idMath.Fabs(newLookAng.yaw)
                    < 0.1f) {
                alignHeadTime = gameLocal.time;
            }
            if ((gameLocal.time >= alignHeadTime) || (gameLocal.time < forceAlignHeadTime)) {
                alignHeadTime = (int) (gameLocal.time + (0.5f + 0.5f * gameLocal.random.RandomFloat()) * focusAlignTime);
                destLookAng = newLookAng;
                destLookAng.Clamp(lookMin, lookMax);
            }
            diff = destLookAng.oMinus(lookAng);
            if ((lookMin.pitch == -180.0f) && (lookMax.pitch
                    == 180.0f)) {
                if ((diff.pitch > 180.0f) || (diff.pitch <= -180.0f)) {
                    diff.pitch = 360.0f - diff.pitch;
                }
            }
            if ((lookMin.yaw == -180.0f) && (lookMax.yaw
                    == 180.0f)) {
                if (diff.yaw > 180.0f) {
                    diff.yaw -= 360.0f;
                } else if (diff.yaw <= -180.0f) {
                    diff.yaw += 360.0f;
                }
            }
            lookAng = lookAng.oPlus(diff.oMultiply(headFocusRate));

            lookAng.Normalize180();
            jointAng.roll = 0.0f;
            for (i = 0;
                    i < lookJoints.Num();
                    i++) {
                jointAng.pitch = lookAng.pitch * lookJointAngles.oGet(i).pitch;
                jointAng.yaw = lookAng.yaw * lookJointAngles.oGet(i).yaw;
                animator.SetJointAxis(lookJoints.oGet(i), JOINTMOD_WORLD, jointAng.ToMat3());
            }
            if (move.moveType == MOVETYPE_FLY) {
                // lean into turns
                AdjustFlyingAngles();
            }
            if (headEnt != null) {
                idAnimator headAnimator = headEnt.GetAnimator();

                if (allowEyeFocus) {
                    idMat3 eyeAxis = (lookAng.oPlus(eyeAng)).ToMat3();
                    idMat3 headTranspose = headEnt.GetPhysics().GetAxis().Transpose();
                    axis = eyeAxis.oMultiply(orientationJointAxis);
                    left = axis.oGet(1).oMultiply(eyeHorizontalOffset);
                    eyepos.oMinSet(headEnt.GetPhysics().GetOrigin());
                    headAnimator.SetJointPos(leftEyeJoint, JOINTMOD_WORLD_OVERRIDE, eyepos.oPlus((axis.oGet(0).oMultiply(64.0f).oPlus(left)).oMultiply(headTranspose)));
                    headAnimator.SetJointPos(rightEyeJoint, JOINTMOD_WORLD_OVERRIDE, eyepos.oPlus((axis.oGet(0).oMultiply(64.0f).oMinus(left)).oMultiply(headTranspose)));
                } else {
                    headAnimator.ClearJoint(leftEyeJoint);
                    headAnimator.ClearJoint(rightEyeJoint);
                }
            } else {
                if (allowEyeFocus) {
                    idMat3 eyeAxis = (lookAng.oPlus(eyeAng)).ToMat3();
                    axis = eyeAxis.oMultiply(orientationJointAxis);
                    left = axis.oGet(1).oMultiply(eyeHorizontalOffset);
                    eyepos.oPluSet(axis.oGet(0).oMultiply(64.0f).oMinus(physicsObj.GetOrigin()));
                    animator.SetJointPos(leftEyeJoint, JOINTMOD_WORLD_OVERRIDE, eyepos.oPlus(left));
                    animator.SetJointPos(rightEyeJoint, JOINTMOD_WORLD_OVERRIDE, eyepos.oMinus(left));
                } else {
                    animator.ClearJoint(leftEyeJoint);
                    animator.ClearJoint(rightEyeJoint);
                }
            }

            return true;
        }

        protected void UpdateParticles() {
            if ((thinkFlags & TH_UPDATEPARTICLES) != 0 && !IsHidden()) {
                idVec3 realVector = new idVec3();
                idMat3 realAxis = new idMat3();

                int particlesAlive = 0;
                for (int i = 0; i < particles.Num(); i++) {
                    if (particles.oGet(i).particle != null && particles.oGet(i).time != 0) {
                        particlesAlive++;
                        if (af.IsActive()) {
                            realAxis = getMat3_identity();
                            realVector = GetPhysics().GetOrigin();
                        } else {
                            animator.GetJointTransform(particles.oGet(i).joint, gameLocal.time, realVector, realAxis);
                            realAxis.oMulSet(renderEntity.axis);
                            realVector = physicsObj.GetOrigin().oPlus((realVector.oPlus(modelOffset)).oMultiply(viewAxis.oMultiply(physicsObj.GetGravityAxis())));
                        }

                        if (!gameLocal.smokeParticles.EmitSmoke(particles.oGet(i).particle, particles.oGet(i).time, gameLocal.random.CRandomFloat(), realVector, realAxis)) {
                            if (restartParticles) {
                                particles.oGet(i).time = gameLocal.time;
                            } else {
                                particles.oGet(i).time = 0;
                                particlesAlive--;
                            }
                        }
                    }
                }
                if (particlesAlive == 0) {
                    BecomeInactive(TH_UPDATEPARTICLES);
                }
            }
        }

        protected void TriggerParticles(final String jointName) {
            int/*jointHandle_t*/ jointNum;

            jointNum = animator.GetJointHandle(jointName);
            for (int i = 0; i < particles.Num(); i++) {
                if (particles.oGet(i).joint == jointNum) {
                    particles.oGet(i).time = gameLocal.time;
                    BecomeActive(TH_UPDATEPARTICLES);
                }
            }
        }

        // AI script state management
        protected void LinkScriptVariables() {
            AI_TALK.LinkTo(scriptObject, "AI_TALK");
            AI_DAMAGE.LinkTo(scriptObject, "AI_DAMAGE");
            AI_PAIN.LinkTo(scriptObject, "AI_PAIN");
            AI_SPECIAL_DAMAGE.LinkTo(scriptObject, "AI_SPECIAL_DAMAGE");
            AI_DEAD.LinkTo(scriptObject, "AI_DEAD");
            AI_ENEMY_VISIBLE.LinkTo(scriptObject, "AI_ENEMY_VISIBLE");
            AI_ENEMY_IN_FOV.LinkTo(scriptObject, "AI_ENEMY_IN_FOV");
            AI_ENEMY_DEAD.LinkTo(scriptObject, "AI_ENEMY_DEAD");
            AI_MOVE_DONE.LinkTo(scriptObject, "AI_MOVE_DONE");
            AI_ONGROUND.LinkTo(scriptObject, "AI_ONGROUND");
            AI_ACTIVATED.LinkTo(scriptObject, "AI_ACTIVATED");
            AI_FORWARD.LinkTo(scriptObject, "AI_FORWARD");
            AI_JUMP.LinkTo(scriptObject, "AI_JUMP");
            AI_BLOCKED.LinkTo(scriptObject, "AI_BLOCKED");
            AI_DEST_UNREACHABLE.LinkTo(scriptObject, "AI_DEST_UNREACHABLE");
            AI_HIT_ENEMY.LinkTo(scriptObject, "AI_HIT_ENEMY");
            AI_OBSTACLE_IN_PATH.LinkTo(scriptObject, "AI_OBSTACLE_IN_PATH");
            AI_PUSHED.LinkTo(scriptObject, "AI_PUSHED");
        }

        protected void UpdateAIScript() {
            UpdateScript();

            // clear the hit enemy flag so we catch the next time we hit someone
            AI_HIT_ENEMY._(false);

            if (allowHiddenMovement || !IsHidden()) {
                // update the animstate if we're not hidden
                UpdateAnimState();
            }
        }

        //
        // ai/ai_events.cpp
        //
        protected void Event_Activate(idEventArg<idEntity> activator) {
            Activate(activator.value);
        }

        protected void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            idEntity other = _other.value;
            if (null == enemy.GetEntity() && !other.fl.notarget && (ReactionTo(other) & ATTACK_ON_ACTIVATE) != 0) {
                Activate(other);
            }
            AI_PUSHED._(true);
        }

        protected void Event_FindEnemy(idEventArg<Integer> useFOV) {
            int i;
            idEntity ent;
            idActor actor;

            if (gameLocal.InPlayerPVS(this)) {
                for (i = 0; i < gameLocal.numClients; i++) {
                    ent = gameLocal.entities[ i];

                    if (null == ent || !ent.IsType(idActor.class)) {
                        continue;
                    }

                    actor = (idActor) ent;
                    if ((actor.health <= 0) || NOT(ReactionTo(actor) & ATTACK_ON_SIGHT)) {
                        continue;
                    }

                    if (CanSee(actor, useFOV.value != 0)) {
                        idThread.ReturnEntity(actor);
                        return;
                    }
                }
            }

            idThread.ReturnEntity(null);
        }

        protected void Event_FindEnemyAI(idEventArg<Integer> useFOV) {
            idEntity ent;
            idActor actor;
            idActor bestEnemy;
            float bestDist;
            float dist;
            idVec3 delta;
            pvsHandle_t pvs;

            pvs = gameLocal.pvs.SetupCurrentPVS(GetPVSAreas(), GetNumPVSAreas());

            bestDist = idMath.INFINITY;
            bestEnemy = null;
            for (ent = gameLocal.activeEntities.Next(); ent != null; ent = ent.activeNode.Next()) {
                if (ent.fl.hidden || ent.fl.isDormant || !ent.IsType(idActor.class)) {
                    continue;
                }

                actor = (idActor) ent;
                if ((actor.health <= 0) || 0 == (ReactionTo(actor) & ATTACK_ON_SIGHT)) {
                    continue;
                }

                if (!gameLocal.pvs.InCurrentPVS(pvs, actor.GetPVSAreas(), actor.GetNumPVSAreas())) {
                    continue;
                }

                delta = physicsObj.GetOrigin().oMinus(actor.GetPhysics().GetOrigin());
                dist = delta.LengthSqr();
                if ((dist < bestDist) && CanSee(actor, useFOV.value != 0)) {
                    bestDist = dist;
                    bestEnemy = actor;
                }
            }

            gameLocal.pvs.FreeCurrentPVS(pvs);
            idThread.ReturnEntity(bestEnemy);
        }

        protected void Event_FindEnemyInCombatNodes() {
            int i, j;
            idCombatNode node;
            idEntity ent;
            idEntity targetEnt;
            idActor actor;

            if (!gameLocal.InPlayerPVS(this)) {
                // don't locate the player when we're not in his PVS
                idThread.ReturnEntity(null);
                return;
            }

            for (i = 0; i < gameLocal.numClients; i++) {
                ent = gameLocal.entities[ i];

                if (null == ent || !ent.IsType(idActor.class)) {
                    continue;
                }

                actor = (idActor) ent;
                if ((actor.health <= 0) || NOT(ReactionTo(actor) & ATTACK_ON_SIGHT)) {
                    continue;
                }

                for (j = 0; j < targets.Num(); j++) {
                    targetEnt = targets.oGet(j).GetEntity();
                    if (null == targetEnt || !targetEnt.IsType(idCombatNode.class)) {
                        continue;
                    }

                    node = (idCombatNode) targetEnt;
                    if (!node.IsDisabled() && node.EntityInView(actor, actor.GetPhysics().GetOrigin())) {
                        idThread.ReturnEntity(actor);
                        return;
                    }
                }
            }

            idThread.ReturnEntity(null);
        }

        protected void Event_ClosestReachableEnemyOfEntity(idEventArg<idEntity> _team_mate) {
            idEntity team_mate = _team_mate.value;
            idActor actor;
            idActor ent;
            idActor bestEnt;
            float bestDistSquared;
            float distSquared;
            idVec3 delta;
            int areaNum;
            int enemyAreaNum;
            aasPath_s path = new aasPath_s();

            if (!team_mate.IsType(idActor.class)) {
                gameLocal.Error("Entity '%s' is not an AI character or player", team_mate.GetName());
            }

            actor = (idActor) team_mate;

            final idVec3 origin = physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(origin);

            bestDistSquared = idMath.INFINITY;
            bestEnt = null;
            for (ent = actor.enemyList.Next(); ent != null; ent = ent.enemyNode.Next()) {
                if (ent.fl.hidden) {
                    continue;
                }
                delta = ent.GetPhysics().GetOrigin().oMinus(origin);
                distSquared = delta.LengthSqr();
                if (distSquared < bestDistSquared) {
                    final idVec3 enemyPos = ent.GetPhysics().GetOrigin();
                    enemyAreaNum = PointReachableAreaNum(enemyPos);
                    if ((areaNum != 0) && PathToGoal(path, areaNum, origin, enemyAreaNum, enemyPos)) {
                        bestEnt = ent;
                        bestDistSquared = distSquared;
                    }
                }
            }

            idThread.ReturnEntity(bestEnt);
        }

        protected void Event_HeardSound(idEventArg<Integer> ignore_team) {
            // check if we heard any sounds in the last frame
            idActor actor = gameLocal.GetAlertEntity();
            if (actor != null && (0 == ignore_team.value || (ReactionTo(actor) & ATTACK_ON_SIGHT) != 0) && gameLocal.InPlayerPVS(this)) {
                idVec3 pos = actor.GetPhysics().GetOrigin();
                idVec3 org = physicsObj.GetOrigin();
                float dist = (pos.oMinus(org)).LengthSqr();
                if (dist < Square(AI_HEARING_RANGE)) {
                    idThread.ReturnEntity(actor);
                    return;
                }
            }

            idThread.ReturnEntity(null);
        }

        protected void Event_SetEnemy(idEventArg<idEntity> _ent) {
            idEntity ent = _ent.value;
            if (null == ent) {
                ClearEnemy();
            } else if (!ent.IsType(idActor.class)) {
                gameLocal.Error("'%s' is not an idActor (player or ai controlled character)", ent.name);
            } else {
                SetEnemy((idActor) ent);
            }
        }

        protected void Event_ClearEnemy() {
            ClearEnemy();
        }

        protected void Event_MuzzleFlash(final idEventArg<String> jointname) {
            idVec3 muzzle = new idVec3();
            idMat3 axis = new idMat3();

            GetMuzzle(jointname.value, muzzle, axis);
            TriggerWeaponEffects(muzzle);
        }

        protected void Event_CreateMissile(final idEventArg<String> _jointname) {
            String jointname = _jointname.value;
            idVec3 muzzle = new idVec3();
            idMat3 axis = new idMat3();

            if (null == projectileDef) {
                gameLocal.Warning("%s (%s) doesn't have a projectile specified", name, GetEntityDefName());
                idThread.ReturnEntity(null);
            }

            GetMuzzle(jointname, muzzle, axis);
            CreateProjectile(muzzle, viewAxis.oGet(0).oMultiply(physicsObj.GetGravityAxis()));
            if (projectile.GetEntity() != null) {
                if (!isNotNullOrEmpty(jointname)) {
                    projectile.GetEntity().Bind(this, true);
                } else {
                    projectile.GetEntity().BindToJoint(this, jointname, true);
                }
            }
            idThread.ReturnEntity(projectile.GetEntity());
        }

        protected void Event_AttackMissile(final idEventArg<String> jointname) {
            idProjectile proj;

            proj = LaunchProjectile(jointname.value, enemy.GetEntity(), true);
            idThread.ReturnEntity(proj);
        }

        protected void Event_FireMissileAtTarget(final idEventArg<String> jointname, final idEventArg<String> targetname) {
            idEntity aent;
            idProjectile proj;

            aent = gameLocal.FindEntity(targetname.value);
            if (null == aent) {
                gameLocal.Warning("Entity '%s' not found for 'fireMissileAtTarget'", targetname.value);
            }

            proj = LaunchProjectile(jointname.value, aent, false);
            idThread.ReturnEntity(proj);
        }

        protected void Event_LaunchMissile(final idEventArg<idVec3> _muzzle, final idEventArg<idAngles> _ang) {
            final idVec3 muzzle = _muzzle.value;
            final idAngles ang = _ang.value;
            idVec3 start;
            trace_s[] tr = {null};
            idBounds projBounds;
            idClipModel projClip;
            idMat3 axis;
            float[] distance = {0};

            if (null == projectileDef) {
                gameLocal.Warning("%s (%s) doesn't have a projectile specified", name, GetEntityDefName());
                idThread.ReturnEntity(null);
                return;
            }

            axis = ang.ToMat3();
            if (null == projectile.GetEntity()) {
                CreateProjectile(muzzle, axis.oGet(0));
            }

            // make sure the projectile starts inside the monster bounding box
            final idBounds ownerBounds = physicsObj.GetAbsBounds();
            projClip = projectile.GetEntity().GetPhysics().GetClipModel();
            projBounds = projClip.GetBounds().Rotate(projClip.GetAxis());

            // check if the owner bounds is bigger than the projectile bounds
            if (((ownerBounds.oGet(1, 0) - ownerBounds.oGet(0, 0)) > (projBounds.oGet(1, 0) - projBounds.oGet(0, 0)))
                    && ((ownerBounds.oGet(1, 1) - ownerBounds.oGet(0, 1)) > (projBounds.oGet(1, 1) - projBounds.oGet(0, 1)))
                    && ((ownerBounds.oGet(1, 2) - ownerBounds.oGet(0, 2)) > (projBounds.oGet(1, 2) - projBounds.oGet(0, 2)))) {
                if ((ownerBounds.oMinus(projBounds)).RayIntersection(muzzle, viewAxis.oGet(0), distance)) {
                    start = muzzle.oPlus(viewAxis.oGet(0).oMultiply(distance[0]));
                } else {
                    start = ownerBounds.GetCenter();
                }
            } else {
                // projectile bounds bigger than the owner bounds, so just start it from the center
                start = ownerBounds.GetCenter();
            }

            gameLocal.clip.Translation(tr, start, muzzle, projClip, projClip.GetAxis(), MASK_SHOT_RENDERMODEL, this);

            // launch the projectile
            idThread.ReturnEntity(projectile.GetEntity());
            projectile.GetEntity().Launch(tr[0].endpos, axis.oGet(0), getVec3_origin());
            projectile.oSet(null);

            TriggerWeaponEffects(tr[0].endpos);

            lastAttackTime = gameLocal.time;
        }

        protected void Event_AttackMelee(final idEventArg<String> meleeDefName) {
            int hit;

            hit = AttackMelee(meleeDefName.value) ? 1 : 0;
            idThread.ReturnInt(hit);
        }

        protected void Event_DirectDamage(idEventArg<idEntity> damageTarget, final idEventArg<String> damageDefName) {
            DirectDamage(damageDefName.value, damageTarget.value);
        }

        protected void Event_RadiusDamageFromJoint(final idEventArg<String> jointname, final idEventArg<String> damageDefName) {
            int/*jointHandle_t*/ joint;
            idVec3 org = new idVec3();
            idMat3 axis = new idMat3();

            if (!isNotNullOrEmpty(jointname.value)) {
                org = physicsObj.GetOrigin();
            } else {
                joint = animator.GetJointHandle(jointname.value);
                if (joint == INVALID_JOINT) {
                    gameLocal.Error("Unknown joint '%s' on %s", jointname.value, GetEntityDefName());
                }
                GetJointWorldTransform(joint, gameLocal.time, org, axis);
            }

            gameLocal.RadiusDamage(org, this, this, this, this, damageDefName.value);
        }

        protected void Event_BeginAttack(final idEventArg<String> name) {
            BeginAttack(name.value);
        }

        protected void Event_EndAttack() {
            EndAttack();
        }

        protected void Event_MeleeAttackToJoint(final idEventArg<String> jointname, final idEventArg<String> meleeDefName) {
            int/*jointHandle_t*/ joint;
            idVec3 start;
            idVec3 end = new idVec3();
            idMat3 axis = new idMat3();
            trace_s trace = new trace_s();
            idEntity hitEnt;

            joint = animator.GetJointHandle(jointname.value);
            if (joint == INVALID_JOINT) {
                gameLocal.Error("Unknown joint '%s' on %s", jointname.value, GetEntityDefName());
            }
            animator.GetJointTransform(joint, gameLocal.time, end, axis);
            end = physicsObj.GetOrigin().oPlus((end.oPlus(modelOffset)).oMultiply(viewAxis).oMultiply(physicsObj.GetGravityAxis()));
            start = GetEyePosition();

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorYellow, start, end, gameLocal.msec);
            }

            gameLocal.clip.TranslationEntities(trace, start, end, null, getMat3_identity(), MASK_SHOT_BOUNDINGBOX, this);
            if (trace.fraction < 1.0f) {
                hitEnt = gameLocal.GetTraceEntity(trace);
                if (hitEnt != null && hitEnt.IsType(idActor.class)) {
                    DirectDamage(meleeDefName.value, hitEnt);
                    idThread.ReturnInt(true);
                    return;
                }
            }

            idThread.ReturnInt(false);
        }

        protected void Event_RandomPath() {
            idPathCorner path;

            path = idPathCorner.RandomPath(this, null);
            idThread.ReturnEntity(path);
        }

        protected void Event_CanBecomeSolid() {
            int i;
            int num;
            idEntity hit;
            idClipModel cm;
            idClipModel[] clipModels = new idClipModel[MAX_GENTITIES];

            num = gameLocal.clip.ClipModelsTouchingBounds(physicsObj.GetAbsBounds(), MASK_MONSTERSOLID, clipModels, MAX_GENTITIES);
            for (i = 0; i < num; i++) {
                cm = clipModels[ i];

                // don't check render entities
                if (cm.IsRenderModel()) {
                    continue;
                }

                hit = cm.GetEntity();
                if (hit.equals(this) || !hit.fl.takedamage) {
                    continue;
                }

                if (physicsObj.ClipContents(cm) != 0) {
                    idThread.ReturnFloat(0);//(false);
                    return;
                }
            }

            idThread.ReturnFloat(1);//(true);
        }

        protected void Event_BecomeSolid() {
            physicsObj.EnableClip();
            if (spawnArgs.GetBool("big_monster")) {
                physicsObj.SetContents(0);
            } else if (use_combat_bbox) {
                physicsObj.SetContents(CONTENTS_BODY | CONTENTS_SOLID);
            } else {
                physicsObj.SetContents(CONTENTS_BODY);
            }
            physicsObj.GetClipModel().Link(gameLocal.clip);
            fl.takedamage = !spawnArgs.GetBool("noDamage");
        }

        protected void Event_BecomeNonSolid() {
            fl.takedamage = false;
            physicsObj.SetContents(0);
            physicsObj.GetClipModel().Unlink();
        }

        protected void Event_BecomeRagdoll() {
            int result;

            result = StartRagdoll() ? 1 : 0;
            idThread.ReturnInt(result);
        }

        protected void Event_StopRagdoll() {
            StopRagdoll();

            // set back the monster physics
            SetPhysics(physicsObj);
        }

        protected void Event_SetHealth(idEventArg<Float> newHealth) {
            health = newHealth.value.intValue();
            fl.takedamage = true;
            if (health > 0) {
                AI_DEAD._(false);
            } else {
                AI_DEAD._(true);
            }
        }

        protected void Event_GetHealth() {
            idThread.ReturnFloat(health);
        }

        protected void Event_AllowDamage() {
            fl.takedamage = true;
        }

        protected void Event_IgnoreDamage() {
            fl.takedamage = false;
        }

        protected void Event_GetCurrentYaw() {
            idThread.ReturnFloat(current_yaw);
        }

        protected void Event_TurnTo(idEventArg<Float> angle) {
            TurnToward(angle.value);
        }

        protected void Event_TurnToPos(final idEventArg<idVec3> pos) {
            TurnToward(pos.value);
        }

        protected void Event_TurnToEntity(idEventArg<idEntity> ent) {
            if (ent.value != null) {
                TurnToward(ent.value.GetPhysics().GetOrigin());
            }
        }

        protected void Event_MoveStatus() {
            idThread.ReturnInt(etoi(move.moveStatus));
        }

        protected void Event_StopMove() {
            StopMove(MOVE_STATUS_DONE);
        }

        protected void Event_MoveToCover() {
            idActor enemyEnt = enemy.GetEntity();

            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            if (null == enemyEnt || !MoveToCover(enemyEnt, lastVisibleEnemyPos)) {
                return;
            }
        }

        protected void Event_MoveToEnemy() {
            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            if (null == enemy.GetEntity() || !MoveToEnemy()) {
                return;
            }
        }

        protected void Event_MoveToEnemyHeight() {
            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            MoveToEnemyHeight();
        }

        protected void Event_MoveOutOfRange(idEventArg<idEntity> entity, idEventArg<Float> range) {
            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            MoveOutOfRange(entity.value, range.value);
        }

        protected void Event_MoveToAttackPosition(idEventArg<idEntity> entity, final idEventArg<String> attack_anim) {
            int anim;

            StopMove(MOVE_STATUS_DEST_NOT_FOUND);

            anim = GetAnim(ANIMCHANNEL_LEGS, attack_anim.value);
            if (0 == anim) {
                gameLocal.Error("Unknown anim '%s'", attack_anim.value);
            }

            MoveToAttackPosition(entity.value, anim);
        }

        protected void Event_MoveToEntity(idEventArg<idEntity> ent) {
            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            if (ent.value != null) {
                MoveToEntity(ent.value);
            }
        }

        protected void Event_MoveToPosition(final idEventArg<idVec3> pos) {
            StopMove(MOVE_STATUS_DONE);
            MoveToPosition(pos.value);
        }

        protected void Event_SlideTo(final idEventArg<idVec3> pos, idEventArg<Float> time) {
            SlideToPosition(pos.value, time.value);
        }

        protected void Event_Wander() {
            WanderAround();
        }

        protected void Event_FacingIdeal() {
            boolean facing = FacingIdeal();
            idThread.ReturnInt(facing);
        }

        protected void Event_FaceEnemy() {
            FaceEnemy();
        }

        protected void Event_FaceEntity(idEventArg<idEntity> ent) {
            FaceEntity(ent.value);
        }

        protected void Event_WaitAction(final idEventArg<String> waitForState) {
            if (idThread.BeginMultiFrameEvent(this, AI_WaitAction)) {
                SetWaitState(waitForState.value);
            }

            if (null == WaitState()) {
                idThread.EndMultiFrameEvent(this, AI_WaitAction);
            }
        }

        protected void Event_GetCombatNode() {
            int i;
            float dist;
            idEntity targetEnt;
            idCombatNode node;
            float bestDist;
            idCombatNode bestNode;
            idActor enemyEnt = enemy.GetEntity();

            if (0 == targets.Num()) {
                // no combat nodes
                idThread.ReturnEntity(null);
                return;
            }

            if (null == enemyEnt || !EnemyPositionValid()) {
                // don't return a combat node if we don't have an enemy or
                // if we can see he's not in the last place we saw him
                idThread.ReturnEntity(null);
                return;
            }

            // find the closest attack node that can see our enemy and is closer than our enemy
            bestNode = null;
            final idVec3 myPos = physicsObj.GetOrigin();
            bestDist = (myPos.oMinus(lastVisibleEnemyPos)).LengthSqr();
            for (i = 0; i < targets.Num(); i++) {
                targetEnt = targets.oGet(i).GetEntity();
                if (null == targetEnt || !targetEnt.IsType(idCombatNode.class)) {
                    continue;
                }

                node = (idCombatNode) targetEnt;
                if (!node.IsDisabled() && node.EntityInView(enemyEnt, lastVisibleEnemyPos)) {
                    idVec3 org = node.GetPhysics().GetOrigin();
                    dist = (myPos.oMinus(org)).LengthSqr();
                    if (dist < bestDist) {
                        bestNode = node;
                        bestDist = dist;
                    }
                }
            }

            idThread.ReturnEntity(bestNode);
        }

        protected void Event_EnemyInCombatCone(idEventArg<idEntity> _ent, idEventArg<Integer> use_current_enemy_location) {
            idEntity ent = _ent.value;
            idCombatNode node;
            boolean result;
            idActor enemyEnt = enemy.GetEntity();

            if (0 == targets.Num()) {
                // no combat nodes
                idThread.ReturnInt(false);
                return;
            }

            if (null == enemyEnt) {
                // have to have an enemy
                idThread.ReturnInt(false);
                return;
            }

            if (null == ent || !ent.IsType(idCombatNode.class)) {
                // not a combat node
                idThread.ReturnInt(false);
                return;
            }

            node = (idCombatNode) ent;
            if (use_current_enemy_location.value != 0) {
                final idVec3 pos = enemyEnt.GetPhysics().GetOrigin();
                result = node.EntityInView(enemyEnt, pos);
            } else {
                result = node.EntityInView(enemyEnt, lastVisibleEnemyPos);
            }

            idThread.ReturnInt(result);
        }

        protected void Event_WaitMove() {
            idThread.BeginMultiFrameEvent(this, AI_WaitMove);

            if (MoveDone()) {
                idThread.EndMultiFrameEvent(this, AI_WaitMove);
            }
        }

        protected void Event_GetJumpVelocity(final idEventArg<idVec3> _pos, idEventArg<Float> _speed, idEventArg<Float> _max_height) {
            idVec3 pos = _pos.value;
            float speed = _speed.value;
            float max_height = _max_height.value;
            idVec3 start;
            idVec3 end;
            idVec3 dir;
            float dist;
            boolean result;
            idEntity enemyEnt = enemy.GetEntity();

            if (null == enemyEnt) {
                idThread.ReturnVector(getVec3_zero());
                return;
            }

            if (speed <= 0.0f) {
                gameLocal.Error("Invalid speed.  speed must be > 0.");
            }

            start = physicsObj.GetOrigin();
            end = pos;
            dir = end.oMinus(start);
            dist = dir.Normalize();
            if (dist > 16.0f) {
                dist -= 16.0f;
                end.oMinus(dir.oMultiply(16.0f));
            }

            result = PredictTrajectory(start, end, speed, physicsObj.GetGravity(), physicsObj.GetClipModel(), MASK_MONSTERSOLID, max_height, this, enemyEnt, ai_debugMove.GetBool() ? 4000 : 0, dir);
            if (result) {
                idThread.ReturnVector(dir.oMultiply(speed));
            } else {
                idThread.ReturnVector(getVec3_zero());
            }
        }

        protected void Event_EntityInAttackCone(idEventArg<idEntity> ent) {
            float attack_cone;
            idVec3 delta;
            float yaw;
            float relYaw;

            if (null == ent.value) {
                idThread.ReturnInt(false);
                return;
            }

            delta = ent.value.GetPhysics().GetOrigin().oMinus(GetEyePosition());

            // get our gravity normal
            final idVec3 gravityDir = GetPhysics().GetGravityNormal();

            // infinite vertical vision, so project it onto our orientation plane
            delta.oMinSet(gravityDir.oMultiply(gravityDir.oMultiply(delta)));

            delta.Normalize();
            yaw = delta.ToYaw();

            attack_cone = spawnArgs.GetFloat("attack_cone", "70");
            relYaw = idMath.AngleNormalize180(ideal_yaw - yaw);
            if (idMath.Fabs(relYaw) < (attack_cone * 0.5f)) {
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        protected void Event_CanSeeEntity(idEventArg<idEntity> ent) {
            if (null == ent.value) {
                idThread.ReturnInt(false);
                return;
            }

            boolean cansee = CanSee(ent.value, false);
            idThread.ReturnInt(cansee);
        }

        protected void Event_SetTalkTarget(idEventArg<idEntity> _target) {
            idEntity target = _target.value;
            if (target != null && !target.IsType(idActor.class)) {
                gameLocal.Error("Cannot set talk target to '%s'.  Not a character or player.", target.GetName());
            }
            talkTarget.oSet((idActor) target);
            if (target != null) {
                AI_TALK._(true);
            } else {
                AI_TALK._(false);
            }
        }

        protected void Event_GetTalkTarget() {
            idThread.ReturnEntity(talkTarget.GetEntity());
        }

        protected void Event_SetTalkState(idEventArg<Integer> _state) {
            int state = _state.value;
            if ((state < 0) || (state >= etoi(NUM_TALK_STATES))) {
                gameLocal.Error("Invalid talk state (%d)", state);
            }

            talk_state = talkState_t.values()[state];
        }

        protected void Event_EnemyRange() {
            float dist;
            idActor enemyEnt = enemy.GetEntity();

            if (enemyEnt != null) {
                dist = (enemyEnt.GetPhysics().GetOrigin().oMinus(GetPhysics().GetOrigin()).Length());
            } else {
                // Just some really high number
                dist = idMath.INFINITY;
            }

            idThread.ReturnFloat(dist);
        }

        protected void Event_EnemyRange2D() {
            float dist;
            idActor enemyEnt = enemy.GetEntity();

            if (enemyEnt != null) {
                dist = (enemyEnt.GetPhysics().GetOrigin().ToVec2().oMinus(GetPhysics().GetOrigin().ToVec2())).Length();
            } else {
                // Just some really high number
                dist = idMath.INFINITY;
            }

            idThread.ReturnFloat(dist);
        }

        protected void Event_GetEnemy() {
            idThread.ReturnEntity(enemy.GetEntity());
        }

        protected void Event_GetEnemyPos() {
            idThread.ReturnVector(lastVisibleEnemyPos);
        }

        protected void Event_GetEnemyEyePos() {
            idThread.ReturnVector(lastVisibleEnemyPos.oPlus(lastVisibleEnemyEyeOffset));
        }

        protected void Event_PredictEnemyPos(idEventArg<Float> time) {
            predictedPath_s path = new predictedPath_s();
            idActor enemyEnt = enemy.GetEntity();

            // if no enemy set
            if (null == enemyEnt) {
                idThread.ReturnVector(physicsObj.GetOrigin());
                return;
            }

            // predict the enemy movement
            idAI.PredictPath(enemyEnt, aas, lastVisibleEnemyPos, enemyEnt.GetPhysics().GetLinearVelocity(),
                    (int) SEC2MS(time.value), (int) SEC2MS(time.value),
                    (move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            idThread.ReturnVector(path.endPos);
        }

        protected void Event_CanHitEnemy() {
            trace_s[] tr = {null};
            idEntity hit;

            idActor enemyEnt = enemy.GetEntity();
            if (!AI_ENEMY_VISIBLE._() || NOT(enemyEnt)) {
                idThread.ReturnInt(false);
                return;
            }

            // don't check twice per frame
            if (gameLocal.time == lastHitCheckTime) {
                idThread.ReturnInt(lastHitCheckResult);
                return;
            }

            lastHitCheckTime = gameLocal.time;

            idVec3 toPos = enemyEnt.GetEyePosition();
            idVec3 eye = GetEyePosition();
            idVec3 dir;

            // expand the ray out as far as possible so we can detect anything behind the enemy
            dir = toPos.oMinus(eye);
            dir.Normalize();
            toPos = eye.oPlus(dir.oMultiply(MAX_WORLD_SIZE));
            gameLocal.clip.TracePoint(tr, eye, toPos, MASK_SHOT_BOUNDINGBOX, this);
            hit = gameLocal.GetTraceEntity(tr[0]);
            if (tr[0].fraction >= 1.0f || (hit.equals(enemyEnt))) {
                lastHitCheckResult = true;
            } else if ((tr[0].fraction < 1.0f) && (hit.IsType(idAI.class))
                    && (((idAI) hit).team != team)) {
                lastHitCheckResult = true;
            } else {
                lastHitCheckResult = false;
            }

            idThread.ReturnInt(lastHitCheckResult);
        }

        protected void Event_CanHitEnemyFromAnim(final idEventArg<String> animname) {
            int anim;
            idVec3 dir;
            idVec3 local_dir = new idVec3();
            idVec3 fromPos;
            idMat3 axis;
            idVec3 start;
            trace_s[] tr = {null};
            float[] distance = {0};

            idActor enemyEnt = enemy.GetEntity();
            if (!AI_ENEMY_VISIBLE._() || NOT(enemyEnt)) {
                idThread.ReturnInt(false);
                return;
            }

            anim = GetAnim(ANIMCHANNEL_LEGS, animname.value);
            if (0 == anim) {
                idThread.ReturnInt(false);
                return;
            }

            // just do a ray test if close enough
            if (enemyEnt.GetPhysics().GetAbsBounds().IntersectsBounds(physicsObj.GetAbsBounds().Expand(16.0f))) {
                Event_CanHitEnemy();
                return;
            }

            // calculate the world transform of the launch position
            final idVec3 org = physicsObj.GetOrigin();
            dir = lastVisibleEnemyPos.oMinus(org);
            physicsObj.GetGravityAxis().ProjectVector(dir, local_dir);
            local_dir.z = 0.0f;
            local_dir.ToVec2().Normalize();
            axis = local_dir.ToMat3();
            fromPos = physicsObj.GetOrigin().oPlus(missileLaunchOffset.oGet(anim).oMultiply(axis));

            if (projectileClipModel == null) {
                CreateProjectileClipModel();
            }

            // check if the owner bounds is bigger than the projectile bounds
            final idBounds ownerBounds = physicsObj.GetAbsBounds();
            final idBounds projBounds = projectileClipModel.GetBounds();
            if (((ownerBounds.oGet(1, 0) - ownerBounds.oGet(0, 0)) > (projBounds.oGet(1, 0) - projBounds.oGet(0, 0)))
                    && ((ownerBounds.oGet(1, 1) - ownerBounds.oGet(0, 1)) > (projBounds.oGet(1, 1) - projBounds.oGet(0, 1)))
                    && ((ownerBounds.oGet(1, 2) - ownerBounds.oGet(0, 2)) > (projBounds.oGet(1, 2) - projBounds.oGet(0, 2)))) {
                if ((ownerBounds.oMinus(projBounds)).RayIntersection(org, viewAxis.oGet(0), distance)) {
                    start = org.oPlus(viewAxis.oGet(0).oMultiply(distance[0]));
                } else {
                    start = ownerBounds.GetCenter();
                }
            } else {
                // projectile bounds bigger than the owner bounds, so just start it from the center
                start = ownerBounds.GetCenter();
            }

            gameLocal.clip.Translation(tr, start, fromPos, projectileClipModel, getMat3_identity(), MASK_SHOT_RENDERMODEL, this);
            fromPos = tr[0].endpos;

            if (GetAimDir(fromPos, enemy.GetEntity(), this, dir)) {
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        protected void Event_CanHitEnemyFromJoint(final idEventArg<String> jointname) {
            trace_s[] tr = {null};
            idVec3 muzzle = new idVec3();
            idMat3 axis = new idMat3();
            idVec3 start;
            float[] distance = {0};

            idActor enemyEnt = enemy.GetEntity();
            if (!AI_ENEMY_VISIBLE._() || null == enemyEnt) {
                idThread.ReturnInt(false);
                return;
            }

            // don't check twice per frame
            if (gameLocal.time == lastHitCheckTime) {
                idThread.ReturnInt(lastHitCheckResult);
                return;
            }

            lastHitCheckTime = gameLocal.time;

            final idVec3 org = physicsObj.GetOrigin();
            idVec3 toPos = enemyEnt.GetEyePosition();
            int/*jointHandle_t*/ joint = animator.GetJointHandle(jointname.value);
            if (joint == INVALID_JOINT) {
                gameLocal.Error("Unknown joint '%s' on %s", jointname.value, GetEntityDefName());
            }
            animator.GetJointTransform(joint, gameLocal.time, muzzle, axis);
            muzzle = org.oPlus((muzzle.oPlus(modelOffset)).oMultiply(viewAxis).oMultiply(physicsObj.GetGravityAxis()));

            if (projectileClipModel == null) {
                CreateProjectileClipModel();
            }

            // check if the owner bounds is bigger than the projectile bounds
            final idBounds ownerBounds = physicsObj.GetAbsBounds();
            final idBounds projBounds = projectileClipModel.GetBounds();
            if (((ownerBounds.oGet(1, 0) - ownerBounds.oGet(0, 0)) > (projBounds.oGet(1, 0) - projBounds.oGet(0, 0)))
                    && ((ownerBounds.oGet(1, 1) - ownerBounds.oGet(0, 1)) > (projBounds.oGet(1, 1) - projBounds.oGet(0, 1)))
                    && ((ownerBounds.oGet(1, 2) - ownerBounds.oGet(0, 2)) > (projBounds.oGet(1, 2) - projBounds.oGet(0, 2)))) {
                if ((ownerBounds.oMinus(projBounds)).RayIntersection(org, viewAxis.oGet(0), distance)) {
                    start = org.oPlus(viewAxis.oGet(0).oMultiply(distance[0]));
                } else {
                    start = ownerBounds.GetCenter();
                }
            } else {
                // projectile bounds bigger than the owner bounds, so just start it from the center
                start = ownerBounds.GetCenter();
            }

            gameLocal.clip.Translation(tr, start, muzzle, projectileClipModel, getMat3_identity(), MASK_SHOT_BOUNDINGBOX, this);
            muzzle = tr[0].endpos;

            gameLocal.clip.Translation(tr, muzzle, toPos, projectileClipModel, getMat3_identity(), MASK_SHOT_BOUNDINGBOX, this);
            if (tr[0].fraction >= 1.0f || (gameLocal.GetTraceEntity(tr[0]).equals(enemyEnt))) {
                lastHitCheckResult = true;
            } else {
                lastHitCheckResult = false;
            }

            idThread.ReturnInt(lastHitCheckResult);
        }

        protected void Event_EnemyPositionValid() {
            int result;

            result = EnemyPositionValid() ? 1 : 0;
            idThread.ReturnInt(result);
        }

        protected void Event_ChargeAttack(final idEventArg<String> damageDef) {
            idActor enemyEnt = enemy.GetEntity();

            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            if (enemyEnt != null) {
                idVec3 enemyOrg;

                if (move.moveType == MOVETYPE_FLY) {
                    // position destination so that we're in the enemy's view
                    enemyOrg = enemyEnt.GetEyePosition();
                    enemyOrg.oMinSet(enemyEnt.GetPhysics().GetGravityNormal().oMultiply(fly_offset));
                } else {
                    enemyOrg = enemyEnt.GetPhysics().GetOrigin();
                }

                BeginAttack(damageDef.value);
                DirectMoveToPosition(enemyOrg);
                TurnToward(enemyOrg);
            }
        }

        protected void Event_TestChargeAttack() {
            trace_s trace = new trace_s();
            idActor enemyEnt = enemy.GetEntity();
            predictedPath_s path = new predictedPath_s();
            idVec3 end;

            if (null == enemyEnt) {
                idThread.ReturnFloat(0.0f);
                return;
            }

            if (move.moveType == MOVETYPE_FLY) {
                // position destination so that we're in the enemy's view
                end = enemyEnt.GetEyePosition();
                end.oMinSet(enemyEnt.GetPhysics().GetGravityNormal().oMultiply(fly_offset));
            } else {
                end = enemyEnt.GetPhysics().GetOrigin();
            }

            idAI.PredictPath(this, aas, physicsObj.GetOrigin(), end.oMinus(physicsObj.GetOrigin()), 1000, 1000, (move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorGreen, physicsObj.GetOrigin(), end, gameLocal.msec);
                gameRenderWorld.DebugBounds(path.endEvent == 0 ? colorYellow : colorRed, physicsObj.GetBounds(), end, gameLocal.msec);
            }

            if ((path.endEvent == 0) || (path.blockingEntity.equals(enemyEnt))) {
                idVec3 delta = end.oMinus(physicsObj.GetOrigin());
                float time = delta.LengthFast();
                idThread.ReturnFloat(time);
            } else {
                idThread.ReturnFloat(0.0f);
            }
        }

        protected void Event_TestAnimMoveTowardEnemy(final idEventArg<String> animname) {
            int anim;
            predictedPath_s path = new predictedPath_s();
            idVec3 moveVec;
            float yaw;
            idVec3 delta;
            idActor enemyEnt;

            enemyEnt = enemy.GetEntity();
            if (null == enemyEnt) {
                idThread.ReturnInt(false);
                return;
            }

            anim = GetAnim(ANIMCHANNEL_LEGS, animname.value);
            if (0 == anim) {
                gameLocal.DWarning("missing '%s' animation on '%s' (%s)", animname.value, name, GetEntityDefName());
                idThread.ReturnInt(false);
                return;
            }

            delta = enemyEnt.GetPhysics().GetOrigin().oMinus(physicsObj.GetOrigin());
            yaw = delta.ToYaw();

            moveVec = animator.TotalMovementDelta(anim).oMultiply(new idAngles(0.0f, yaw, 0.0f).ToMat3().oMultiply(physicsObj.GetGravityAxis()));
            idAI.PredictPath(this, aas, physicsObj.GetOrigin(), moveVec, 1000, 1000, (move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorGreen, physicsObj.GetOrigin(), physicsObj.GetOrigin().oPlus(moveVec), gameLocal.msec);
                gameRenderWorld.DebugBounds(path.endEvent == 0 ? colorYellow : colorRed, physicsObj.GetBounds(), physicsObj.GetOrigin().oPlus(moveVec), gameLocal.msec);
            }

            idThread.ReturnInt(path.endEvent == 0);
        }

        protected void Event_TestAnimMove(final idEventArg<String> animname) {
            int anim;
            predictedPath_s path = new predictedPath_s();
            idVec3 moveVec;

            anim = GetAnim(ANIMCHANNEL_LEGS, animname.value);
            if (0 == anim) {
                gameLocal.DWarning("missing '%s' animation on '%s' (%s)", animname.value, name, GetEntityDefName());
                idThread.ReturnInt(false);
                return;
            }

            moveVec = animator.TotalMovementDelta(anim).oMultiply(new idAngles(0.0f, ideal_yaw, 0.0f).ToMat3().oMultiply(physicsObj.GetGravityAxis()));
            idAI.PredictPath(this, aas, physicsObj.GetOrigin(), moveVec, 1000, 1000, (move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorGreen, physicsObj.GetOrigin(), physicsObj.GetOrigin().oPlus(moveVec), gameLocal.msec);
                gameRenderWorld.DebugBounds(path.endEvent == 0 ? colorYellow : colorRed, physicsObj.GetBounds(), physicsObj.GetOrigin().oPlus(moveVec), gameLocal.msec);
            }

            idThread.ReturnInt(path.endEvent == 0);
        }

        protected void Event_TestMoveToPosition(final idEventArg<idVec3> _position) {
            idVec3 position = _position.value;
            predictedPath_s path = new predictedPath_s();

            idAI.PredictPath(this, aas, physicsObj.GetOrigin(), position.oMinus(physicsObj.GetOrigin()), 1000, 1000, (move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorGreen, physicsObj.GetOrigin(), position, gameLocal.msec);
                gameRenderWorld.DebugBounds(colorYellow, physicsObj.GetBounds(), position, gameLocal.msec);
                if (path.endEvent != 0) {
                    gameRenderWorld.DebugBounds(colorRed, physicsObj.GetBounds(), path.endPos, gameLocal.msec);
                }
            }

            idThread.ReturnInt(path.endEvent == 0);
        }

        protected void Event_TestMeleeAttack() {
            boolean result = TestMelee();
            idThread.ReturnInt(result);
        }

        protected void Event_TestAnimAttack(final idEventArg<String> animname) {
            int anim;
            predictedPath_s path = new predictedPath_s();

            anim = GetAnim(ANIMCHANNEL_LEGS, animname.value);
            if (0 == anim) {
                gameLocal.DWarning("missing '%s' animation on '%s' (%s)", animname.value, name, GetEntityDefName());
                idThread.ReturnInt(false);
                return;
            }

            idAI.PredictPath(this, aas, physicsObj.GetOrigin(), animator.TotalMovementDelta(anim), 1000, 1000, (move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            idThread.ReturnInt(path.blockingEntity != null && (path.blockingEntity.equals(enemy.GetEntity())));
        }

        protected void Event_Shrivel(idEventArg<Float> shrivel_time) {
            float t;

            if (idThread.BeginMultiFrameEvent(this, AI_Shrivel)) {
                if (shrivel_time.value <= 0.0f) {
                    idThread.EndMultiFrameEvent(this, AI_Shrivel);
                    return;
                }

                shrivel_rate = 0.001f / shrivel_time.value;
                shrivel_start = gameLocal.time;
            }

            t = (gameLocal.time - shrivel_start) * shrivel_rate;
            if (t > 0.25f) {
                renderEntity.noShadow = true;
            }
            if (t > 1.0f) {
                t = 1.0f;
                idThread.EndMultiFrameEvent(this, AI_Shrivel);
            }

            renderEntity.shaderParms[SHADERPARM_MD5_SKINSCALE] = 1.0f - t * 0.5f;
            UpdateVisuals();
        }

        protected void Event_Burn() {
            renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = gameLocal.time * 0.001f;
            SpawnParticles("smoke_burnParticleSystem");
            UpdateVisuals();
        }

        protected void Event_PreBurn() {
            // for now this just turns shadows off
            renderEntity.noShadow = true;
        }

        protected void Event_ClearBurn() {
            renderEntity.noShadow = spawnArgs.GetBool("noshadows");
            renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = 0.0f;
            UpdateVisuals();
        }

        protected void Event_SetSmokeVisibility(idEventArg<Integer> _num, idEventArg<Integer> on) {
            int num = _num.value;
            int i;
            int time;

            if (num >= particles.Num()) {
                gameLocal.Warning("Particle #%d out of range (%d particles) on entity '%s'", num, particles.Num(), name);
                return;
            }

            if (on.value != 0) {
                time = gameLocal.time;
                BecomeActive(TH_UPDATEPARTICLES);
            } else {
                time = 0;
            }

            if (num >= 0) {
                particles.oGet(num).time = time;
            } else {
                for (i = 0; i < particles.Num(); i++) {
                    particles.oGet(i).time = time;
                }
            }

            UpdateVisuals();
        }

        protected void Event_NumSmokeEmitters() {
            idThread.ReturnInt(particles.Num());
        }

        protected void Event_StopThinking() {
            BecomeInactive(TH_THINK);
            idThread thread = idThread.CurrentThread();
            if (thread != null) {
                thread.DoneProcessing();
            }
        }

        protected void Event_GetTurnDelta() {
            float amount;

            if (turnRate != 0) {
                amount = idMath.AngleNormalize180(ideal_yaw - current_yaw);
                idThread.ReturnFloat(amount);
            } else {
                idThread.ReturnFloat(0.0f);
            }
        }

        protected void Event_GetMoveType() {
            idThread.ReturnInt(etoi(move.moveType));
        }

        protected void Event_SetMoveType(idEventArg<Integer> _moveType) {
            int moveType = _moveType.value;
            if ((moveType < 0) || (moveType >= etoi(NUM_MOVETYPES))) {
                gameLocal.Error("Invalid movetype %d", moveType);
            }

            move.moveType = moveType_t.values()[moveType];
            if (move.moveType == MOVETYPE_FLY) {
                travelFlags = TFL_WALK | TFL_AIR | TFL_FLY;
            } else {
                travelFlags = TFL_WALK | TFL_AIR;
            }
        }

        protected void Event_SaveMove() {
            savedMove = move;
        }

        protected void Event_RestoreMove() {
            idVec3 goalPos = new idVec3();
            idVec3 dest = new idVec3();

            switch (savedMove.moveCommand) {
                case MOVE_NONE:
                    StopMove(savedMove.moveStatus);
                    break;

                case MOVE_FACE_ENEMY:
                    FaceEnemy();
                    break;

                case MOVE_FACE_ENTITY:
                    FaceEntity(savedMove.goalEntity.GetEntity());
                    break;

                case MOVE_TO_ENEMY:
                    MoveToEnemy();
                    break;

                case MOVE_TO_ENEMYHEIGHT:
                    MoveToEnemyHeight();
                    break;

                case MOVE_TO_ENTITY:
                    MoveToEntity(savedMove.goalEntity.GetEntity());
                    break;

                case MOVE_OUT_OF_RANGE:
                    MoveOutOfRange(savedMove.goalEntity.GetEntity(), savedMove.range);
                    break;

                case MOVE_TO_ATTACK_POSITION:
                    MoveToAttackPosition(savedMove.goalEntity.GetEntity(), savedMove.anim);
                    break;

                case MOVE_TO_COVER:
                    MoveToCover(savedMove.goalEntity.GetEntity(), lastVisibleEnemyPos);
                    break;

                case MOVE_TO_POSITION:
                    MoveToPosition(savedMove.moveDest);
                    break;

                case MOVE_TO_POSITION_DIRECT:
                    DirectMoveToPosition(savedMove.moveDest);
                    break;

                case MOVE_SLIDE_TO_POSITION:
                    SlideToPosition(savedMove.moveDest, savedMove.duration);
                    break;

                case MOVE_WANDER:
                    WanderAround();
                    break;
            }

            if (GetMovePos(goalPos)) {
                CheckObstacleAvoidance(goalPos, dest);
            }
        }

        protected void Event_AllowMovement(idEventArg<Float> flag) {
            allowMove = (flag.value != 0.0f);
        }

        protected void Event_JumpFrame() {
            AI_JUMP._(true);
        }

        protected void Event_EnableClip() {
            physicsObj.SetClipMask(MASK_MONSTERSOLID);
            disableGravity = false;
        }

        protected void Event_DisableClip() {
            physicsObj.SetClipMask(0);
            disableGravity = true;
        }

        protected void Event_EnableGravity() {
            disableGravity = false;
        }

        protected void Event_DisableGravity() {
            disableGravity = true;
        }

        protected void Event_EnableAFPush() {
            af_push_moveables = true;
        }

        protected void Event_DisableAFPush() {
            af_push_moveables = false;
        }

        protected void Event_SetFlySpeed(idEventArg<Float> speed) {
            if (move.speed == fly_speed) {
                move.speed = speed.value;
            }
            fly_speed = speed.value;
        }

        protected void Event_SetFlyOffset(idEventArg<Integer> offset) {
            fly_offset = offset.value;
        }

        protected void Event_ClearFlyOffset() {
            fly_offset = spawnArgs.GetInt("fly_offset", "0");
        }

        protected void Event_GetClosestHiddenTarget(final idEventArg<String> type) {
            int i;
            idEntity ent;
            idEntity bestEnt;
            float time;
            float bestTime;
            final idVec3 org = physicsObj.GetOrigin();
            idActor enemyEnt = enemy.GetEntity();

            if (null == enemyEnt) {
                // no enemy to hide from
                idThread.ReturnEntity(null);
                return;
            }

            if (targets.Num() == 1) {
                ent = targets.oGet(0).GetEntity();
                if (ent != null && idStr.Cmp(ent.GetEntityDefName(), type.value) == 0) {
                    if (!EntityCanSeePos(enemyEnt, lastVisibleEnemyPos, ent.GetPhysics().GetOrigin())) {
                        idThread.ReturnEntity(ent);
                        return;
                    }
                }
                idThread.ReturnEntity(null);
                return;
            }

            bestEnt = null;
            bestTime = idMath.INFINITY;
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null && idStr.Cmp(ent.GetEntityDefName(), type.value) == 0) {
                    final idVec3 destOrg = ent.GetPhysics().GetOrigin();
                    time = TravelDistance(org, destOrg);
                    if ((time >= 0.0f) && (time < bestTime)) {
                        if (!EntityCanSeePos(enemyEnt, lastVisibleEnemyPos, destOrg)) {
                            bestEnt = ent;
                            bestTime = time;
                        }
                    }
                }
            }
            idThread.ReturnEntity(bestEnt);
        }

        protected void Event_GetRandomTarget(final idEventArg<String> type) {
            int i;
            int num;
            int which;
            idEntity ent;
            idEntity[] ents = new idEntity[MAX_GENTITIES];

            num = 0;
            for (i = 0; i < targets.Num(); i++) {
                ent = targets.oGet(i).GetEntity();
                if (ent != null && idStr.Cmp(ent.GetEntityDefName(), type.value) == 0) {
                    ents[ num++] = ent;
                    if (num >= MAX_GENTITIES) {
                        break;
                    }
                }
            }

            if (0 == num) {
                idThread.ReturnEntity(null);
                return;
            }

            which = gameLocal.random.RandomInt(num);
            idThread.ReturnEntity(ents[ which]);
        }

        protected void Event_TravelDistanceToPoint(final idEventArg<idVec3> pos) {
            float time;

            time = TravelDistance(physicsObj.GetOrigin(), pos.value);
            idThread.ReturnFloat(time);
        }

        protected void Event_TravelDistanceToEntity(idEventArg<idEntity> ent) {
            float time;

            time = TravelDistance(physicsObj.GetOrigin(), ent.value.GetPhysics().GetOrigin());
            idThread.ReturnFloat(time);
        }

        protected void Event_TravelDistanceBetweenPoints(final idEventArg<idVec3> source, final idEventArg<idVec3> dest) {
            float time;

            time = TravelDistance(source.value, dest.value);
            idThread.ReturnFloat(time);
        }

        protected void Event_TravelDistanceBetweenEntities(idEventArg<idEntity> source, idEventArg<idEntity> dest) {
            float time;

            assert (source.value != null);
            assert (dest.value != null);
            time = TravelDistance(source.value.GetPhysics().GetOrigin(), dest.value.GetPhysics().GetOrigin());
            idThread.ReturnFloat(time);
        }

        protected void Event_LookAtEntity(idEventArg<idEntity> _ent, idEventArg<Float> duration) {
            idEntity ent = _ent.value;
            if (ent.equals(this)) {
                ent = null;
            } else if ((!ent.equals(focusEntity.GetEntity())) || (focusTime < gameLocal.time)) {
                focusEntity.oSet(ent);
                alignHeadTime = gameLocal.time;
                forceAlignHeadTime = (int) (gameLocal.time + SEC2MS(1));
                blink_time = 0;
            }

            focusTime = (int) (gameLocal.time + SEC2MS(duration.value));
        }

        protected void Event_LookAtEnemy(idEventArg<Float> duration) {
            idActor enemyEnt;

            enemyEnt = enemy.GetEntity();
            if ((!enemyEnt.equals(focusEntity.GetEntity())) || (focusTime < gameLocal.time)) {
                focusEntity.oSet(enemyEnt);
                alignHeadTime = gameLocal.time;
                forceAlignHeadTime = (int) (gameLocal.time + SEC2MS(1));
                blink_time = 0;
            }

            focusTime = (int) (gameLocal.time + SEC2MS(duration.value));
        }

        protected void Event_SetJointMod(idEventArg<Integer> allow) {
            allowJointMod = itob(allow.value);
        }

        protected void Event_ThrowMoveable() {
            idEntity ent;
            idEntity moveable = null;

            for (ent = GetNextTeamEntity(); ent != null; ent = ent.GetNextTeamEntity()) {
                if (ent.GetBindMaster().equals(this) && ent.IsType(idMoveable.class)) {
                    moveable = ent;
                    break;
                }
            }
            if (moveable != null) {
                moveable.Unbind();
                moveable.PostEventMS(EV_SetOwner, 200, null);
            }
        }

        protected void Event_ThrowAF() {
            idEntity ent;
            idEntity af = null;

            for (ent = GetNextTeamEntity(); ent != null; ent = ent.GetNextTeamEntity()) {
                if (ent.GetBindMaster().equals(this) && ent.IsType(idAFEntity_Base.class)) {
                    af = ent;
                    break;
                }
            }
            if (af != null) {
                af.Unbind();
                af.PostEventMS(EV_SetOwner, 200, null);
            }
        }

        protected void Event_SetAngles(final idEventArg<idAngles> ang) {
            current_yaw = ang.value.yaw;
            viewAxis = new idAngles(0, current_yaw, 0).ToMat3();
        }

        protected void Event_GetAngles() {
            idThread.ReturnVector(new idVec3(0.0f, current_yaw, 0.0f));
        }

        protected void Event_RealKill() {
            health = 0;

            if (af.IsLoaded()) {
                // clear impacts
                af.Rest();

                // physics is turned off by calling af.Rest()
                BecomeActive(TH_PHYSICS);
            }

            Killed(this, this, 0, getVec3_zero(), INVALID_JOINT);
        }

        protected void Event_Kill() {
            PostEventMS(AI_RealKill, 0);
        }

        protected void Event_WakeOnFlashlight(idEventArg<Integer> enable) {
            wakeOnFlashlight = (enable.value != 0);
        }

        protected void Event_LocateEnemy() {
            idActor enemyEnt;
            int[] areaNum = {0};

            enemyEnt = enemy.GetEntity();
            if (null == enemyEnt) {
                return;
            }

            enemyEnt.GetAASLocation(aas, lastReachableEnemyPos, areaNum);
            SetEnemyPosition();
            UpdateEnemyPosition();
        }

        protected void Event_KickObstacles(idEventArg<idEntity> kickEnt, idEventArg<Float> force) {
            idVec3 dir;
            idEntity obEnt;

            if (kickEnt.value != null) {
                obEnt = kickEnt.value;
            } else {
                obEnt = move.obstacle.GetEntity();
            }

            if (obEnt != null) {
                dir = obEnt.GetPhysics().GetOrigin().oMinus(physicsObj.GetOrigin());
                dir.Normalize();
            } else {
                dir = viewAxis.oGet(0);
            }
            KickObstacles(dir, force.value, obEnt);
        }

        protected void Event_GetObstacle() {
            idThread.ReturnEntity(move.obstacle.GetEntity());
        }

        protected void Event_PushPointIntoAAS(final idEventArg<idVec3> _pos) {
            idVec3 pos = _pos.value;
            int areaNum;
            idVec3 newPos;

            areaNum = PointReachableAreaNum(pos);
            if (areaNum != 0) {
                newPos = pos;
                aas.PushPointIntoAreaNum(areaNum, newPos);
                idThread.ReturnVector(newPos);
            } else {
                idThread.ReturnVector(pos);
            }
        }

        protected void Event_GetTurnRate() {
            idThread.ReturnFloat(turnRate);
        }

        protected void Event_SetTurnRate(idEventArg<Float> rate) {
            turnRate = rate.value;
        }

        protected void Event_AnimTurn(idEventArg<Float> angles) {
            turnVel = 0.0f;
            anim_turn_angles = angles.value;
            if (angles.value != 0) {
                anim_turn_yaw = current_yaw;
                anim_turn_amount = idMath.Fabs(idMath.AngleNormalize180(current_yaw - ideal_yaw));
                if (anim_turn_amount > anim_turn_angles) {
                    anim_turn_amount = anim_turn_angles;
                }
            } else {
                anim_turn_amount = 0.0f;
                animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(0, 1.0f);
                animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(1, 0.0f);
                animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(0, 1.0f);
                animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(1, 0.0f);
            }
        }

        protected void Event_AllowHiddenMovement(idEventArg<Integer> enable) {
            allowHiddenMovement = (enable.value != 0);
        }

        protected void Event_TriggerParticles(final idEventArg<String> jointName) {
            TriggerParticles(jointName.value);
        }

        protected void Event_FindActorsInBounds(final idEventArg<idVec3> mins, final idEventArg<idVec3> maxs) {
            idEntity ent;
            idEntity[] entityList = new idEntity[MAX_GENTITIES];
            int numListedEntities;
            int i;

            numListedEntities = gameLocal.clip.EntitiesTouchingBounds(new idBounds(mins.value, maxs.value), CONTENTS_BODY, entityList, MAX_GENTITIES);
            for (i = 0; i < numListedEntities; i++) {
                ent = entityList[ i];
                if (!ent.equals(this) && !ent.IsHidden() && (ent.health > 0) && ent.IsType(idActor.class)) {
                    idThread.ReturnEntity(ent);
                    return;
                }
            }

            idThread.ReturnEntity(null);
        }

        protected void Event_CanReachPosition(final idEventArg<idVec3> pos) {
            aasPath_s path = new aasPath_s();
            int toAreaNum;
            int areaNum;

            toAreaNum = PointReachableAreaNum(pos.value);
            areaNum = PointReachableAreaNum(physicsObj.GetOrigin());
            if (0 == toAreaNum || !PathToGoal(path, areaNum, physicsObj.GetOrigin(), toAreaNum, pos.value)) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
            }
        }

        protected void Event_CanReachEntity(idEventArg<idEntity> _ent) {
            idEntity ent = _ent.value;
            aasPath_s path = new aasPath_s();
            int toAreaNum;
            int areaNum;
            idVec3 pos = new idVec3();

            if (null == ent) {
                idThread.ReturnInt(false);
                return;
            }

            if (move.moveType != MOVETYPE_FLY) {
                if (!ent.GetFloorPos(64.0f, pos)) {
                    idThread.ReturnInt(false);
                    return;
                }
                if (ent.IsType(idActor.class) && ((idActor) ent).OnLadder()) {
                    idThread.ReturnInt(false);
                    return;
                }
            } else {
                pos = ent.GetPhysics().GetOrigin();
            }

            toAreaNum = PointReachableAreaNum(pos);
            if (0 == toAreaNum) {
                idThread.ReturnInt(false);
                return;
            }

            final idVec3 org = physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);
            if (0 == toAreaNum || !PathToGoal(path, areaNum, org, toAreaNum, pos)) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
            }
        }

        protected void Event_CanReachEnemy() {
            aasPath_s path = new aasPath_s();
            int[] toAreaNum = {0};
            int areaNum;
            idVec3 pos = new idVec3();
            idActor enemyEnt;

            enemyEnt = enemy.GetEntity();
            if (null == enemyEnt) {
                idThread.ReturnInt(false);
                return;
            }

            if (move.moveType != MOVETYPE_FLY) {
                if (enemyEnt.OnLadder()) {
                    idThread.ReturnInt(false);
                    return;
                }
                enemyEnt.GetAASLocation(aas, pos, toAreaNum);
            } else {
                pos = enemyEnt.GetPhysics().GetOrigin();
                toAreaNum[0] = PointReachableAreaNum(pos);
            }

            if (0 == toAreaNum[0]) {
                idThread.ReturnInt(false);
                return;
            }

            final idVec3 org = physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);
            if (!PathToGoal(path, areaNum, org, toAreaNum[0], pos)) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
            }
        }

        protected void Event_GetReachableEntityPosition(idEventArg<idEntity> _ent) {
            idEntity ent = _ent.value;
            int toAreaNum;
            idVec3 pos = new idVec3();

            if (move.moveType != MOVETYPE_FLY) {
                if (!ent.GetFloorPos(64.0f, pos)) {

                    // NOTE: not a good way to return 'false'
                    /*return*/ idThread.ReturnVector(getVec3_zero());
                }
                if (ent.IsType(idActor.class) && ((idActor) ent).OnLadder()) {
                    /*return*/// NOTE: not a good way to return 'false'
                    /*return*/ idThread.ReturnVector(getVec3_zero());
                }
            } else {
                pos = ent.GetPhysics().GetOrigin();
            }

            if (aas != null) {
                toAreaNum = PointReachableAreaNum(pos);
                aas.PushPointIntoAreaNum(toAreaNum, pos);
            }

            idThread.ReturnVector(pos);
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

    public static class idCombatNode extends idEntity {
        // CLASS_PROTOTYPE( idCombatNode );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_CombatNode_MarkUsed, (eventCallback_t0<idCombatNode>) idCombatNode::Event_MarkUsed );
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idCombatNode>) idCombatNode::Event_Activate );
        }

        private float min_dist;
        private float max_dist;
        private float cone_dist;
        private float min_height;
        private float max_height;
        private idVec3 cone_left;
        private idVec3 cone_right;
        private idVec3 offset;
        private boolean disabled;
        //
        //

        public idCombatNode() {
            min_dist = 0.0f;
            max_dist = 0.0f;
            cone_dist = 0.0f;
            min_height = 0.0f;
            max_height = 0.0f;
            cone_left = new idVec3();
            cone_right = new idVec3();
            offset = new idVec3();
            disabled = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(min_dist);
            savefile.WriteFloat(max_dist);
            savefile.WriteFloat(cone_dist);
            savefile.WriteFloat(min_height);
            savefile.WriteFloat(max_height);
            savefile.WriteVec3(cone_left);
            savefile.WriteVec3(cone_right);
            savefile.WriteVec3(offset);
            savefile.WriteBool(disabled);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            min_dist = savefile.ReadFloat();
            max_dist = savefile.ReadFloat();
            cone_dist = savefile.ReadFloat();
            min_height = savefile.ReadFloat();
            max_height = savefile.ReadFloat();
            savefile.ReadVec3(cone_left);
            savefile.ReadVec3(cone_right);
            savefile.ReadVec3(offset);
            disabled = savefile.ReadBool();
        }

        @Override
        public void Spawn() {
            float fov;
            float yaw;
            float height;

            min_dist = spawnArgs.GetFloat("min");
            max_dist = spawnArgs.GetFloat("max");
            height = spawnArgs.GetFloat("height");
            fov = spawnArgs.GetFloat("fov", "60");
            offset = spawnArgs.GetVector("offset");

            final idVec3 org = GetPhysics().GetOrigin().oPlus(offset);
            min_height = org.z - height * 0.5f;
            max_height = min_height + height;

            final idMat3 axis = GetPhysics().GetAxis();
            yaw = axis.oGet(0).ToYaw();

            idAngles leftang = new idAngles(0.0f, yaw + fov * 0.5f - 90.0f, 0.0f);
            cone_left = leftang.ToForward();

            idAngles rightang = new idAngles(0.0f, yaw - fov * 0.5f + 90.0f, 0.0f);
            cone_right = rightang.ToForward();

            disabled = spawnArgs.GetBool("start_off");
        }

        public boolean IsDisabled() {
            return disabled;
        }

        public boolean EntityInView(idActor actor, final idVec3 pos) {
            if (null == actor || (actor.health <= 0)) {
                return false;
            }

            final idBounds bounds = actor.GetPhysics().GetBounds();
            if ((pos.z + bounds.oGet(1).z < min_height) || (pos.z + bounds.oGet(0).z >= max_height)) {
                return false;
            }

            final idVec3 org = GetPhysics().GetOrigin().oPlus(offset);
            final idMat3 axis = GetPhysics().GetAxis();
            idVec3 dir = pos.oMinus(org);
            float dist = dir.oMultiply(axis.oGet(0));

            if ((dist < min_dist) || (dist > max_dist)) {
                return false;
            }

            float left_dot = dir.oMultiply(cone_left);
            if (left_dot < 0.0f) {
                return false;
            }

            float right_dot = dir.oMultiply(cone_right);
            if (right_dot < 0.0f) {
                return false;
            }

            return true;
        }

        public static void DrawDebugInfo() {
            idEntity ent;
            idCombatNode node;
            idPlayer player = gameLocal.GetLocalPlayer();
            idVec4 color;
            idBounds bounds = new idBounds(new idVec3(-16, -16, 0), new idVec3(16, 16, 0));

            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (!ent.IsType(idCombatNode.class)) {
                    continue;
                }

                node = (idCombatNode) ent;
                if (node.disabled) {
                    color = colorMdGrey;
                } else if (player != null && node.EntityInView(player, player.GetPhysics().GetOrigin())) {
                    color = colorYellow;
                } else {
                    color = colorRed;
                }

                idVec3 leftDir = new idVec3(-node.cone_left.y, node.cone_left.x, 0.0f);
                idVec3 rightDir = new idVec3(node.cone_right.y, -node.cone_right.x, 0.0f);
                idVec3 org = node.GetPhysics().GetOrigin().oPlus(node.offset);

                bounds.oGet(1).z = node.max_height;

                leftDir.NormalizeFast();
                rightDir.NormalizeFast();

                final idMat3 axis = node.GetPhysics().GetAxis();
                float cone_dot = node.cone_right.oMultiply(axis.oGet(1));
                if (idMath.Fabs(cone_dot) > 0.1) {
                    float cone_dist = node.max_dist / cone_dot;
                    idVec3 pos1 = org.oPlus(leftDir.oMultiply(node.min_dist));
                    idVec3 pos2 = org.oPlus(leftDir.oMultiply(cone_dist));
                    idVec3 pos3 = org.oPlus(rightDir.oMultiply(node.min_dist));
                    idVec3 pos4 = org.oPlus(rightDir.oMultiply(cone_dist));

                    gameRenderWorld.DebugLine(color, node.GetPhysics().GetOrigin(), (pos1.oPlus(pos3)).oMultiply(0.5f), gameLocal.msec);
                    gameRenderWorld.DebugLine(color, pos1, pos2, gameLocal.msec);
                    gameRenderWorld.DebugLine(color, pos1, pos3, gameLocal.msec);
                    gameRenderWorld.DebugLine(color, pos3, pos4, gameLocal.msec);
                    gameRenderWorld.DebugLine(color, pos2, pos4, gameLocal.msec);
                    gameRenderWorld.DebugBounds(color, bounds, org, gameLocal.msec);
                }
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            disabled = !disabled;
        }

        private void Event_MarkUsed() {
            if (spawnArgs.GetBool("use_once")) {
                disabled = true;
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

    /*
     ============
     ValidForBounds
     ============
     */
    static boolean ValidForBounds(final idAASSettings settings, final idBounds bounds) {
        int i;

        for (i = 0; i < 3; i++) {
            if (bounds.oGet(0, i) < settings.boundingBoxes[0].oGet(0, i)) {
                return false;
            }
            if (bounds.oGet(1, i) > settings.boundingBoxes[0].oGet(1, i)) {
                return false;
            }
        }
        return true;
    }

    /*
     =====================
     Seek
     =====================
     */
    static idVec3 Seek(idVec3 vel, final idVec3 org, final idVec3 goal, float prediction) {
        idVec3 predictedPos;
        idVec3 goalDelta;
        idVec3 seekVel;

        // predict our position
        predictedPos = org.oPlus(vel.oMultiply(prediction));
        goalDelta = goal.oMinus(predictedPos);
        seekVel = goalDelta.oMultiply(MS2SEC(gameLocal.msec));

        return seekVel;
    }
}
