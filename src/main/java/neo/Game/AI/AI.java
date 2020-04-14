package neo.Game.AI;

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
import static neo.Game.AI.AI_Events.*;
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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import neo.CM.CollisionModel.trace_s;
import neo.Game.AF.afTouch_s;
import neo.Game.AFEntity.idAFAttachment;
import neo.Game.AFEntity.idAFEntity_Base;
import neo.Game.Actor.idActor;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Game_local.idGameLocal;
import neo.Game.Misc.idPathCorner;
import neo.Game.Moveable.idMoveable;
import neo.Game.Player.idPlayer;
import neo.Game.Projectile.idProjectile;
import neo.Game.Projectile.idSoulCubeMissile;
import neo.Game.Pvs.pvsHandle_t;
import neo.Game.AI.AAS.aasGoal_s;
import neo.Game.AI.AAS.aasObstacle_s;
import neo.Game.AI.AAS.aasPath_s;
import neo.Game.AI.AAS.idAAS;
import neo.Game.AI.AAS.idAASCallback;
import neo.Game.AI.AI_pathing.ballistics_s;
import neo.Game.AI.AI_pathing.obstacle_s;
import neo.Game.AI.AI_pathing.pathNode_s;
import neo.Game.AI.AI_pathing.pathTrace_s;
import neo.Game.Animation.Anim.animFlags_t;
import neo.Game.Animation.Anim.frameCommand_t;
import neo.Game.Animation.Anim_Blend.idAnim;
import neo.Game.Animation.Anim_Blend.idAnimator;
import neo.Game.Animation.Anim_Blend.idDeclModelDef;
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
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Monster.idPhysics_Monster;
import neo.Game.Physics.Physics_Monster.monsterMoveResult_t;
import neo.Game.Script.Script_Program.idScriptBool;
import neo.Game.Script.Script_Program.idScriptFloat;
import neo.Game.Script.Script_Thread.idThread;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Sound.snd_shader.idSoundShader;
import neo.Tools.Compilers.AAS.AASFile.idAASSettings;
import neo.Tools.Compilers.AAS.AASFile.idReachability;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.DeclParticle.idDeclParticle;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

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
    }

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
    }

    public enum talkState_t {

        TALK_NEVER,
        TALK_DEAD,
        TALK_OK,
        TALK_BUSY,
        NUM_TALK_STATES
    }

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
    }

    static final int DI_NODIR = -1;

    // obstacle avoidance
    public static class obstaclePath_s {

        idVec3   seekPos;                   // seek position avoiding obstacles
        idEntity firstObstacle;             // if != NULL the first obstacle along the path
        idVec3   startPosOutsideObstacles;  // start position outside obstacles
        idEntity startPosObstacle;          // if != NULL the obstacle containing the start position
        idVec3   seekPosOutsideObstacles;   // seek position outside obstacles
        idEntity seekPosObstacle;           // if != NULL the obstacle containing the seek position

        public obstaclePath_s() {
            this.seekPos = new idVec3();
            this.startPosOutsideObstacles = new idVec3();
            this.seekPosOutsideObstacles = new idVec3();
        }
    }

    // path prediction
// typedef enum {
    public static final int SE_BLOCKED          = BIT(0);
    public static final int SE_ENTER_LEDGE_AREA = BIT(1);
    public static final int SE_ENTER_OBSTACLE   = BIT(2);
    public static final int SE_FALL             = BIT(3);
    public static final int SE_LAND             = BIT(4);
// } stopEvent_t;

    public static class predictedPath_s {

        idVec3   endPos      = new idVec3();  // final position
        idVec3   endVelocity = new idVec3();  // velocity at end position
        idVec3   endNormal   = new idVec3();  // normal of blocking surface
        int      endTime;                     // time predicted
        int      endEvent;                    // event that stopped the prediction
        idEntity blockingEntity;              // entity that blocks the movement
    }


    static class particleEmitter_s {

        particleEmitter_s() {
            this.particle = null;
            this.time = 0;
            this.joint = INVALID_JOINT;
        }

        idDeclParticle particle;
        int            time;
        int/*jointHandle_t*/ joint;
    }

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
            this.moveType = MOVETYPE_ANIM;
            this.moveCommand = MOVE_NONE;
            this.moveStatus = MOVE_STATUS_DONE;
            this.moveDest = new idVec3();
            this.moveDir = new idVec3(1.0f, 0.0f, 0.0f);
            this.goalEntity = new idEntityPtr<>(null);
            this.goalEntityOrigin = new idVec3();
            this.toAreaNum = 0;
            this.startTime = 0;
            this.duration = 0;
            this.speed = 0.0f;
            this.range = 0.0f;
            this.wanderYaw = 0;
            this.nextWanderTime = 0;
            this.blockTime = 0;
            this.obstacle = new idEntityPtr<>(null);
            this.lastMoveOrigin = getVec3_origin();
            this.lastMoveTime = 0;
            this.anim = 0;
        }

        public void Save(idSaveGame savefile) {
            savefile.WriteInt(this.moveType.ordinal());
            savefile.WriteInt(this.moveCommand.ordinal());
            savefile.WriteInt(this.moveStatus.ordinal());
            savefile.WriteVec3(this.moveDest);
            savefile.WriteVec3(this.moveDir);
            this.goalEntity.Save(savefile);
            savefile.WriteVec3(this.goalEntityOrigin);
            savefile.WriteInt(this.toAreaNum);
            savefile.WriteInt(this.startTime);
            savefile.WriteInt(this.duration);
            savefile.WriteFloat(this.speed);
            savefile.WriteFloat(this.range);
            savefile.WriteFloat(this.wanderYaw);
            savefile.WriteInt(this.nextWanderTime);
            savefile.WriteInt(this.blockTime);
            this.obstacle.Save(savefile);
            savefile.WriteVec3(this.lastMoveOrigin);
            savefile.WriteInt(this.lastMoveTime);
            savefile.WriteInt(this.anim);
        }

        public void Restore(idRestoreGame savefile) {
            this.moveType = moveType_t.values()[savefile.ReadInt()];
            this.moveCommand = moveCommand_t.values()[savefile.ReadInt()];
            this.moveStatus = moveStatus_t.values()[savefile.ReadInt()];
            savefile.ReadVec3(this.moveDest);
            savefile.ReadVec3(this.moveDir);
            this.goalEntity.Restore(savefile);
            savefile.ReadVec3(this.goalEntityOrigin);
            this.toAreaNum = savefile.ReadInt();
            this.startTime = savefile.ReadInt();
            this.duration = savefile.ReadInt();
            this.speed = savefile.ReadFloat();
            this.range = savefile.ReadFloat();
            this.wanderYaw = savefile.ReadFloat();
            this.nextWanderTime = savefile.ReadInt();
            this.blockTime = savefile.ReadInt();
            this.obstacle.Restore(savefile);
            savefile.ReadVec3(this.lastMoveOrigin);
            this.lastMoveTime = savefile.ReadInt();
            this.anim = savefile.ReadInt();
        }
    }

    public static class idAASFindCover extends idAASCallback {

        private final pvsHandle_t hidePVS;
        private final int[] PVSAreas = new int[idEntity.MAX_PVS_AREAS];
        //
        //

        public idAASFindCover(final idVec3 hideFromPos) {
            int numPVSAreas;
            final idBounds bounds = new idBounds(hideFromPos.oMinus(new idVec3(16, 16, 0)), hideFromPos.oPlus(new idVec3(16, 16, 64)));

            // setup PVS
            numPVSAreas = gameLocal.pvs.GetPVSAreas(bounds, this.PVSAreas, idEntity.MAX_PVS_AREAS);
            this.hidePVS = gameLocal.pvs.SetupCurrentPVS(this.PVSAreas, numPVSAreas);
        }

        // ~idAASFindCover();
        @Override
        public boolean TestArea(final idAAS aas, int areaNum) {
            idVec3 areaCenter;
            int numPVSAreas;
            final int[] PVSAreas = new int[idEntity.MAX_PVS_AREAS];

            areaCenter = aas.AreaCenter(areaNum);
            areaCenter.oPluSet(2, 1.0f);

            numPVSAreas = gameLocal.pvs.GetPVSAreas(new idBounds(areaCenter).Expand(16.0f), PVSAreas, idEntity.MAX_PVS_AREAS);
            if (!gameLocal.pvs.InCurrentPVS(this.hidePVS, PVSAreas, numPVSAreas)) {
                return true;
            }

            return false;
        }
    }

    public static class idAASFindAreaOutOfRange extends idAASCallback {

        private final idVec3 targetPos;
        private final float  maxDistSqr;
        //
        //

        public idAASFindAreaOutOfRange(final idVec3 targetPos, float maxDist) {
            this.targetPos = targetPos;
            this.maxDistSqr = maxDist * maxDist;
        }

        @Override
        public boolean TestArea(final idAAS aas, int areaNum) {
            final idVec3 areaCenter = aas.AreaCenter(areaNum);
            final trace_s[] trace = {null};
            float dist;

            dist = (this.targetPos.ToVec2().oMinus(areaCenter.ToVec2())).LengthSqr();

            if ((this.maxDistSqr > 0.0f) && (dist < this.maxDistSqr)) {
                return false;
            }

            gameLocal.clip.TracePoint(trace, this.targetPos, areaCenter.oPlus(new idVec3(0.0f, 0.0f, 1.0f)), MASK_OPAQUE, null);
            if (trace[0].fraction < 1.0f) {
                return false;
            }

            return true;
        }
    }

    public static class idAASFindAttackPosition extends idAASCallback {

        private final idAI        self;
        private final idEntity    target;
        private final idBounds    excludeBounds;
        private final idVec3      targetPos;
        private final idVec3      fireOffset;
        private final idMat3      gravityAxis;
        private final pvsHandle_t targetPVS;
        private final int[] PVSAreas = new int[idEntity.MAX_PVS_AREAS];
        //
        //

        public idAASFindAttackPosition(final idAI self, final idMat3 gravityAxis, idEntity target, final idVec3 targetPos, final idVec3 fireOffset) {
            int numPVSAreas;

            this.target = target;
            this.targetPos = targetPos;
            this.fireOffset = fireOffset;
            this.self = self;
            this.gravityAxis = gravityAxis;

            this.excludeBounds = new idBounds(new idVec3(-64.0f, -64.0f, -8.0f), new idVec3(64.0f, 64.0f, 64.0f));
            this.excludeBounds.TranslateSelf(self.GetPhysics().GetOrigin());

            // setup PVS
            final idBounds bounds = new idBounds(targetPos.oMinus(new idVec3(16, 16, 0)), targetPos.oPlus(new idVec3(16, 16, 64)));
            numPVSAreas = gameLocal.pvs.GetPVSAreas(bounds, this.PVSAreas, idEntity.MAX_PVS_AREAS);
            this.targetPVS = gameLocal.pvs.SetupCurrentPVS(this.PVSAreas, numPVSAreas);
        }
        // ~idAASFindAttackPosition();

        @Override
        public boolean TestArea(final idAAS aas, int areaNum) {
            idVec3 dir;
            final idVec3 local_dir = new idVec3();
            idVec3 fromPos;
            idMat3 axis;
            idVec3 areaCenter;
            int numPVSAreas;
            final int[] PVSAreas = new int[idEntity.MAX_PVS_AREAS];

            areaCenter = aas.AreaCenter(areaNum);
            areaCenter.oPluSet(2, 1.0f);

            if (this.excludeBounds.ContainsPoint(areaCenter)) {
                // too close to where we already are
                return false;
            }

            numPVSAreas = gameLocal.pvs.GetPVSAreas(new idBounds(areaCenter).Expand(16.0f), PVSAreas, idEntity.MAX_PVS_AREAS);
            if (!gameLocal.pvs.InCurrentPVS(this.targetPVS, PVSAreas, numPVSAreas)) {
                return false;
            }

            // calculate the world transform of the launch position
            dir = this.targetPos.oMinus(areaCenter);
            this.gravityAxis.ProjectVector(dir, local_dir);
            local_dir.z = 0.0f;
            local_dir.ToVec2_Normalize();
            axis = local_dir.ToMat3();
            fromPos = areaCenter.oPlus(this.fireOffset.oMultiply(axis));

            return this.self.GetAimDir(fromPos, this.target, this.self, dir);
        }
    }

    public static class idAI extends idActor {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
            this.aas = null;
            this.travelFlags = TFL_WALK | TFL_AIR;
            this.move = new idMoveState();
            this.kickForce = 2048.0f;
            this.ignore_obstacles = false;
            this.blockedRadius = 0.0f;
            this.blockedMoveTime = 750;
            this.blockedAttackTime = 750;
            this.turnRate = 360.0f;
            this.turnVel = 0.0f;
            this.anim_turn_yaw = 0.0f;
            this.anim_turn_amount = 0.0f;
            this.anim_turn_angles = 0.0f;
            this.physicsObj = new idPhysics_Monster();
            this.fly_offset = 0;
            this.fly_seek_scale = 1.0f;
            this.fly_roll_scale = 0.0f;
            this.fly_roll_max = 0.0f;
            this.fly_roll = 0.0f;
            this.fly_pitch_scale = 0.0f;
            this.fly_pitch_max = 0.0f;
            this.fly_pitch = 0.0f;
            this.allowMove = false;
            this.allowHiddenMovement = false;
            this.fly_speed = 0.0f;
            this.fly_bob_strength = 0.0f;
            this.fly_bob_vert = 0.0f;
            this.fly_bob_horz = 0.0f;
            this.lastHitCheckResult = false;
            this.lastHitCheckTime = 0;
            this.lastAttackTime = 0;
            this.melee_range = 0.0f;
            this.projectile_height_to_distance_ratio = 1.0f;
            this.missileLaunchOffset = new idList<>();
            this.projectileDef = null;
            this.projectile = new idEntityPtr<>(null);
            this.attack = new idStr();
            this.projectileClipModel = null;
            this.projectileRadius = 0.0f;
            this.projectileVelocity = getVec3_origin();
            this.projectileGravity = getVec3_origin();
            this.projectileSpeed = 0.0f;
            this.chat_snd = null;
            this.chat_min = 0;
            this.chat_max = 0;
            this.chat_time = 0;
            this.talk_state = TALK_NEVER;
            this.talkTarget = new idEntityPtr<>(null);

            this.particles = new idList<>();
            this.restartParticles = true;
            this.useBoneAxis = false;

            this.wakeOnFlashlight = false;
            this.worldMuzzleFlash = new renderLight_s();//memset( &worldMuzzleFlash, 0, sizeof ( worldMuzzleFlash ) );
            this.worldMuzzleFlashHandle = -1;

            this.enemy = new idEntityPtr<>(null);
            this.lastVisibleEnemyPos = new idVec3();
            this.lastVisibleEnemyEyeOffset = new idVec3();
            this.lastVisibleReachableEnemyPos = new idVec3();
            this.lastReachableEnemyPos = new idVec3();
            this.shrivel_rate = 0.0f;
            this.shrivel_start = 0;
            this.fl.neverDormant = false;        // AI's can go dormant
            this.current_yaw = 0.0f;
            this.ideal_yaw = 0.0f;

            this.num_cinematics = 0;
            this.current_cinematic = 0;

            this.allowEyeFocus = true;
            this.allowPain = true;
            this.allowJointMod = true;
            this.focusEntity = new idEntityPtr<>(null);
            this.focusTime = 0;
            this.alignHeadTime = 0;
            this.forceAlignHeadTime = 0;

            this.currentFocusPos = new idVec3();
            this.eyeAng = new idAngles();
            this.lookAng = new idAngles();
            this.destLookAng = new idAngles();
            this.lookMin = new idAngles();
            this.lookMax = new idAngles();
            
            this.lookJoints = new idList<>();
            this.lookJointAngles = new idList<>();

            this.eyeMin = new idAngles();
            this.eyeMax = new idAngles();
            this.muzzleFlashEnd = 0;
            this.flashTime = 0;
            this.flashJointWorld = INVALID_JOINT;

            this.focusJoint = INVALID_JOINT;
            this.orientationJoint = INVALID_JOINT;
            this.flyTiltJoint = INVALID_JOINT;

            this.eyeVerticalOffset = 0.0f;
            this.eyeHorizontalOffset = 0.0f;
            this.eyeFocusRate = 0.0f;
            this.headFocusRate = 0.0f;
            this.focusAlignTime = 0;

            this.AI_TALK = new idScriptBool();
            this.AI_DAMAGE = new idScriptBool();
            this.AI_PAIN = new idScriptBool();
            this.AI_SPECIAL_DAMAGE = new idScriptFloat();
            this.AI_DEAD = new idScriptBool();
            this.AI_ENEMY_VISIBLE = new idScriptBool();
            this.AI_ENEMY_IN_FOV = new idScriptBool();
            this.AI_ENEMY_DEAD = new idScriptBool();
            this.AI_MOVE_DONE = new idScriptBool();
            this.AI_ONGROUND = new idScriptBool();
            this.AI_ACTIVATED = new idScriptBool();
            this.AI_FORWARD = new idScriptBool();
            this.AI_JUMP = new idScriptBool();
            this.AI_ENEMY_REACHABLE = new idScriptBool();
            this.AI_BLOCKED = new idScriptBool();
            this.AI_OBSTACLE_IN_PATH = new idScriptBool();
            this.AI_DEST_UNREACHABLE = new idScriptBool();
            this.AI_HIT_ENEMY = new idScriptBool();
            this.AI_PUSHED = new idScriptBool();
        }
        // ~idAI();

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(this.travelFlags);
            this.move.Save(savefile);
            this.savedMove.Save(savefile);
            savefile.WriteFloat(this.kickForce);
            savefile.WriteBool(this.ignore_obstacles);
            savefile.WriteFloat(this.blockedRadius);
            savefile.WriteInt(this.blockedMoveTime);
            savefile.WriteInt(this.blockedAttackTime);

            savefile.WriteFloat(this.ideal_yaw);
            savefile.WriteFloat(this.current_yaw);
            savefile.WriteFloat(this.turnRate);
            savefile.WriteFloat(this.turnVel);
            savefile.WriteFloat(this.anim_turn_yaw);
            savefile.WriteFloat(this.anim_turn_amount);
            savefile.WriteFloat(this.anim_turn_angles);

            savefile.WriteStaticObject(this.physicsObj);

            savefile.WriteFloat(this.fly_speed);
            savefile.WriteFloat(this.fly_bob_strength);
            savefile.WriteFloat(this.fly_bob_vert);
            savefile.WriteFloat(this.fly_bob_horz);
            savefile.WriteInt(this.fly_offset);
            savefile.WriteFloat(this.fly_seek_scale);
            savefile.WriteFloat(this.fly_roll_scale);
            savefile.WriteFloat(this.fly_roll_max);
            savefile.WriteFloat(this.fly_roll);
            savefile.WriteFloat(this.fly_pitch_scale);
            savefile.WriteFloat(this.fly_pitch_max);
            savefile.WriteFloat(this.fly_pitch);

            savefile.WriteBool(this.allowMove);
            savefile.WriteBool(this.allowHiddenMovement);
            savefile.WriteBool(this.disableGravity);
            savefile.WriteBool(this.af_push_moveables);

            savefile.WriteBool(this.lastHitCheckResult);
            savefile.WriteInt(this.lastHitCheckTime);
            savefile.WriteInt(this.lastAttackTime);
            savefile.WriteFloat(this.melee_range);
            savefile.WriteFloat(this.projectile_height_to_distance_ratio);

            savefile.WriteInt(this.missileLaunchOffset.Num());
            for (i = 0; i < this.missileLaunchOffset.Num(); i++) {
                savefile.WriteVec3(this.missileLaunchOffset.oGet(i));
            }

            final idStr projectileName = new idStr();
            this.spawnArgs.GetString("def_projectile", "", projectileName);
            savefile.WriteString(projectileName);
            savefile.WriteFloat(this.projectileRadius);
            savefile.WriteFloat(this.projectileSpeed);
            savefile.WriteVec3(this.projectileVelocity);
            savefile.WriteVec3(this.projectileGravity);
            this.projectile.Save(savefile);
            savefile.WriteString(this.attack);

            savefile.WriteSoundShader(this.chat_snd);
            savefile.WriteInt(this.chat_min);
            savefile.WriteInt(this.chat_max);
            savefile.WriteInt(this.chat_time);
            savefile.WriteInt(etoi(this.talk_state));
            this.talkTarget.Save(savefile);

            savefile.WriteInt(this.num_cinematics);
            savefile.WriteInt(this.current_cinematic);

            savefile.WriteBool(this.allowJointMod);
            this.focusEntity.Save(savefile);
            savefile.WriteVec3(this.currentFocusPos);
            savefile.WriteInt(this.focusTime);
            savefile.WriteInt(this.alignHeadTime);
            savefile.WriteInt(this.forceAlignHeadTime);
            savefile.WriteAngles(this.eyeAng);
            savefile.WriteAngles(this.lookAng);
            savefile.WriteAngles(this.destLookAng);
            savefile.WriteAngles(this.lookMin);
            savefile.WriteAngles(this.lookMax);

            savefile.WriteInt(this.lookJoints.Num());
            for (i = 0; i < this.lookJoints.Num(); i++) {
                savefile.WriteJoint(this.lookJoints.oGet(i));
                savefile.WriteAngles(this.lookJointAngles.oGet(i));
            }

            savefile.WriteFloat(this.shrivel_rate);
            savefile.WriteInt(this.shrivel_start);

            savefile.WriteInt(this.particles.Num());
            for (i = 0; i < this.particles.Num(); i++) {
                savefile.WriteParticle(this.particles.oGet(i).particle);
                savefile.WriteInt(this.particles.oGet(i).time);
                savefile.WriteJoint(this.particles.oGet(i).joint);
            }
            savefile.WriteBool(this.restartParticles);
            savefile.WriteBool(this.useBoneAxis);

            this.enemy.Save(savefile);
            savefile.WriteVec3(this.lastVisibleEnemyPos);
            savefile.WriteVec3(this.lastVisibleEnemyEyeOffset);
            savefile.WriteVec3(this.lastVisibleReachableEnemyPos);
            savefile.WriteVec3(this.lastReachableEnemyPos);
            savefile.WriteBool(this.wakeOnFlashlight);

            savefile.WriteAngles(this.eyeMin);
            savefile.WriteAngles(this.eyeMax);

            savefile.WriteFloat(this.eyeVerticalOffset);
            savefile.WriteFloat(this.eyeHorizontalOffset);
            savefile.WriteFloat(this.eyeFocusRate);
            savefile.WriteFloat(this.headFocusRate);
            savefile.WriteInt(this.focusAlignTime);

            savefile.WriteJoint(this.flashJointWorld);
            savefile.WriteInt(this.muzzleFlashEnd);

            savefile.WriteJoint(this.focusJoint);
            savefile.WriteJoint(this.orientationJoint);
            savefile.WriteJoint(this.flyTiltJoint);

            savefile.WriteBool(GetPhysics().equals(this.physicsObj));
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final boolean[] restorePhysics = {false};
            int i;
            int num;
            final idBounds bounds;

            this.travelFlags = savefile.ReadInt();
            this.move.Restore(savefile);
            this.savedMove.Restore(savefile);
            this.kickForce = savefile.ReadFloat();
            this.ignore_obstacles = savefile.ReadBool();
            this.blockedRadius = savefile.ReadFloat();
            this.blockedMoveTime = savefile.ReadInt();
            this.blockedAttackTime = savefile.ReadInt();

            this.ideal_yaw = savefile.ReadFloat();
            this.current_yaw = savefile.ReadFloat();
            this.turnRate = savefile.ReadFloat();
            this.turnVel = savefile.ReadFloat();
            this.anim_turn_yaw = savefile.ReadFloat();
            this.anim_turn_amount = savefile.ReadFloat();
            this.anim_turn_angles = savefile.ReadFloat();

            savefile.ReadStaticObject(this.physicsObj);

            this.fly_speed = savefile.ReadFloat();
            this.fly_bob_strength = savefile.ReadFloat();
            this.fly_bob_vert = savefile.ReadFloat();
            this.fly_bob_horz = savefile.ReadFloat();
            this.fly_offset = savefile.ReadInt();
            this.fly_seek_scale = savefile.ReadFloat();
            this.fly_roll_scale = savefile.ReadFloat();
            this.fly_roll_max = savefile.ReadFloat();
            this.fly_roll = savefile.ReadFloat();
            this.fly_pitch_scale = savefile.ReadFloat();
            this.fly_pitch_max = savefile.ReadFloat();
            this.fly_pitch = savefile.ReadFloat();

            this.allowMove = savefile.ReadBool();
            this.allowHiddenMovement = savefile.ReadBool();
            this.disableGravity = savefile.ReadBool();
            this.af_push_moveables = savefile.ReadBool();

            this.lastHitCheckResult = savefile.ReadBool();
            this.lastHitCheckTime = savefile.ReadInt();
            this.lastAttackTime = savefile.ReadInt();
            this.melee_range = savefile.ReadFloat();
            this.projectile_height_to_distance_ratio = savefile.ReadFloat();

            num = savefile.ReadInt();
            this.missileLaunchOffset.SetGranularity(1);
            this.missileLaunchOffset.SetNum(num);
            for (i = 0; i < num; i++) {
                savefile.ReadVec3(this.missileLaunchOffset.oGet(i));
            }

            final idStr projectileName = new idStr();
            savefile.ReadString(projectileName);
            if (projectileName.Length() != 0) {
                this.projectileDef = gameLocal.FindEntityDefDict(projectileName.getData());
            } else {
                this.projectileDef = null;
            }
            this.projectileRadius = savefile.ReadFloat();
            this.projectileSpeed = savefile.ReadFloat();
            savefile.ReadVec3(this.projectileVelocity);
            savefile.ReadVec3(this.projectileGravity);
            this.projectile.Restore(savefile);
            savefile.ReadString(this.attack);

            savefile.ReadSoundShader(this.chat_snd);
            this.chat_min = savefile.ReadInt();
            this.chat_max = savefile.ReadInt();
            this.chat_time = savefile.ReadInt();
            i = savefile.ReadInt();
            this.talk_state = talkState_t.values()[i];
            this.talkTarget.Restore(savefile);

            this.num_cinematics = savefile.ReadInt();
            this.current_cinematic = savefile.ReadInt();

            this.allowJointMod = savefile.ReadBool();
            this.focusEntity.Restore(savefile);
            savefile.ReadVec3(this.currentFocusPos);
            this.focusTime = savefile.ReadInt();
            this.alignHeadTime = savefile.ReadInt();
            this.forceAlignHeadTime = savefile.ReadInt();
            savefile.ReadAngles(this.eyeAng);
            savefile.ReadAngles(this.lookAng);
            savefile.ReadAngles(this.destLookAng);
            savefile.ReadAngles(this.lookMin);
            savefile.ReadAngles(this.lookMax);

            num = savefile.ReadInt();
            this.lookJoints.SetGranularity(1);
            this.lookJoints.SetNum(num);
            this.lookJointAngles.SetGranularity(1);
            this.lookJointAngles.SetNum(num);
            for (i = 0; i < num; i++) {
                this.lookJoints.oSet(i, savefile.ReadJoint());
                savefile.ReadAngles(this.lookJointAngles.oGet(i));
            }

            this.shrivel_rate = savefile.ReadFloat();
            this.shrivel_start = savefile.ReadInt();

            num = savefile.ReadInt();
            this.particles.SetNum(num);
            for (i = 0; i < this.particles.Num(); i++) {
                savefile.ReadParticle(this.particles.oGet(i).particle);
                this.particles.oGet(i).time = savefile.ReadInt();
                this.particles.oGet(i).joint = savefile.ReadJoint();
            }
            this.restartParticles = savefile.ReadBool();
            this.useBoneAxis = savefile.ReadBool();

            this.enemy.Restore(savefile);
            savefile.ReadVec3(this.lastVisibleEnemyPos);
            savefile.ReadVec3(this.lastVisibleEnemyEyeOffset);
            savefile.ReadVec3(this.lastVisibleReachableEnemyPos);
            savefile.ReadVec3(this.lastReachableEnemyPos);

            this.wakeOnFlashlight = savefile.ReadBool();

            savefile.ReadAngles(this.eyeMin);
            savefile.ReadAngles(this.eyeMax);

            this.eyeVerticalOffset = savefile.ReadFloat();
            this.eyeHorizontalOffset = savefile.ReadFloat();
            this.eyeFocusRate = savefile.ReadFloat();
            this.headFocusRate = savefile.ReadFloat();
            this.focusAlignTime = savefile.ReadInt();

            this.flashJointWorld = savefile.ReadJoint();
            this.muzzleFlashEnd = savefile.ReadInt();

            this.focusJoint = savefile.ReadJoint();
            this.orientationJoint = savefile.ReadJoint();
            this.flyTiltJoint = savefile.ReadJoint();

            savefile.ReadBool(restorePhysics);

            // Set the AAS if the character has the correct gravity vector
            final idVec3 gravity = this.spawnArgs.GetVector("gravityDir", "0 0 -1");
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
                RestorePhysics(this.physicsObj);
            }
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            idKeyValue kv;
            final idStr jointName = new idStr();
            idAngles jointScale;
            int/*jointHandle_t*/ joint;
            final idVec3 local_dir = new idVec3();
            final boolean[] talks = {false};

            if (!g_monsters.GetBool()) {
                PostEventMS(EV_Remove, 0);
                return;
            }

            this.team = this.spawnArgs.GetInt("team", "1");
            this.rank = this.spawnArgs.GetInt("rank", "0");
            this.fly_offset = this.spawnArgs.GetInt("fly_offset", "0");
            this.fly_speed = this.spawnArgs.GetFloat("fly_speed", "100");
            this.fly_bob_strength = this.spawnArgs.GetFloat("fly_bob_strength", "50");
            this.fly_bob_horz = this.spawnArgs.GetFloat("fly_bob_vert", "2");
            this.fly_bob_vert = this.spawnArgs.GetFloat("fly_bob_horz", "2.7");
            this.fly_seek_scale = this.spawnArgs.GetFloat("fly_seek_scale", "4");
            this.fly_roll_scale = this.spawnArgs.GetFloat("fly_roll_scale", "90");
            this.fly_roll_max = this.spawnArgs.GetFloat("fly_roll_max", "60");
            this.fly_pitch_scale = this.spawnArgs.GetFloat("fly_pitch_scale", "45");
            this.fly_pitch_max = this.spawnArgs.GetFloat("fly_pitch_max", "30");

            this.melee_range = this.spawnArgs.GetFloat("melee_range", "64");
            this.projectile_height_to_distance_ratio = this.spawnArgs.GetFloat("projectile_height_to_distance_ratio", "1");

            this.turnRate = this.spawnArgs.GetFloat("turn_rate", "360");

            this.spawnArgs.GetBool("talks", "0", talks);
            if (this.spawnArgs.GetString("npc_name", null) != null) {
                if (talks[0]) {
                    this.talk_state = TALK_OK;
                } else {
                    this.talk_state = TALK_BUSY;
                }
            } else {
                this.talk_state = TALK_NEVER;
            }

            this.disableGravity = this.spawnArgs.GetBool("animate_z", "0");
            this.af_push_moveables = this.spawnArgs.GetBool("af_push_moveables", "0");
            this.kickForce = this.spawnArgs.GetFloat("kick_force", "4096");
            this.ignore_obstacles = this.spawnArgs.GetBool("ignore_obstacles", "0");
            this.blockedRadius = this.spawnArgs.GetFloat("blockedRadius", "-1");
            this.blockedMoveTime = this.spawnArgs.GetInt("blockedMoveTime", "750");
            this.blockedAttackTime = this.spawnArgs.GetInt("blockedAttackTime", "750");

            this.num_cinematics = this.spawnArgs.GetInt("num_cinematics", "0");
            this.current_cinematic = 0;

            LinkScriptVariables();

            this.fl.takedamage = !this.spawnArgs.GetBool("noDamage");
            this.enemy.oSet(null);
            this.allowMove = true;
            this.allowHiddenMovement = false;

            this.animator.RemoveOriginOffset(true);

            // create combat collision hull for exact collision detection
            SetCombatModel();

            this.lookMin = this.spawnArgs.GetAngles("look_min", "-80 -75 0");
            this.lookMax = this.spawnArgs.GetAngles("look_max", "80 75 0");

            this.lookJoints.SetGranularity(1);
            this.lookJointAngles.SetGranularity(1);
            kv = this.spawnArgs.MatchPrefix("look_joint", null);
            while (kv != null) {
                jointName.oSet(kv.GetKey());
                jointName.StripLeadingOnce("look_joint ");
                joint = this.animator.GetJointHandle(jointName);
                if (joint == INVALID_JOINT) {
                    gameLocal.Warning("Unknown look_joint '%s' on entity %s", jointName, this.name);
                } else {
                    jointScale = this.spawnArgs.GetAngles(kv.GetKey().getData(), "0 0 0");
                    jointScale.roll = 0.0f;

                    // if no scale on any component, then don't bother adding it.  this may be done to
                    // zero out rotation from an inherited entitydef.
                    if (!jointScale.equals(getAng_zero())) {
                        this.lookJoints.Append(joint);
                        this.lookJointAngles.Append(jointScale);
                    }
                }
                kv = this.spawnArgs.MatchPrefix("look_joint", kv);
            }

            // calculate joint positions on attack frames so we can do proper "can hit" tests
            CalculateAttackOffsets();

            this.eyeMin = this.spawnArgs.GetAngles("eye_turn_min", "-10 -30 0");
            this.eyeMax = this.spawnArgs.GetAngles("eye_turn_max", "10 30 0");
            this.eyeVerticalOffset = this.spawnArgs.GetFloat("eye_verticle_offset", "5");
            this.eyeHorizontalOffset = this.spawnArgs.GetFloat("eye_horizontal_offset", "-8");
            this.eyeFocusRate = this.spawnArgs.GetFloat("eye_focus_rate", "0.5");
            this.headFocusRate = this.spawnArgs.GetFloat("head_focus_rate", "0.1");
            this.focusAlignTime = (int) SEC2MS(this.spawnArgs.GetFloat("focus_align_time", "1"));

            this.flashJointWorld = this.animator.GetJointHandle("flash");

            if (this.head.GetEntity() != null) {
                final idAnimator headAnimator = this.head.GetEntity().GetAnimator();

                jointName.oSet(this.spawnArgs.GetString("bone_focus"));
                if (isNotNullOrEmpty(jointName)) {
                    this.focusJoint = headAnimator.GetJointHandle(jointName);
                    if (this.focusJoint == INVALID_JOINT) {
                        gameLocal.Warning("Joint '%s' not found on head on '%s'", jointName, this.name);
                    }
                }
            } else {
                jointName.oSet(this.spawnArgs.GetString("bone_focus"));
                if (isNotNullOrEmpty(jointName)) {
                    this.focusJoint = this.animator.GetJointHandle(jointName);
                    if (this.focusJoint == INVALID_JOINT) {
                        gameLocal.Warning("Joint '%s' not found on '%s'", jointName, this.name);
                    }
                }
            }

            jointName.oSet(this.spawnArgs.GetString("bone_orientation"));
            if (isNotNullOrEmpty(jointName)) {
                this.orientationJoint = this.animator.GetJointHandle(jointName);
                if (this.orientationJoint == INVALID_JOINT) {
                    gameLocal.Warning("Joint '%s' not found on '%s'", jointName, this.name);
                }
            }

            jointName.oSet(this.spawnArgs.GetString("bone_flytilt"));
            if (isNotNullOrEmpty(jointName)) {
                this.flyTiltJoint = this.animator.GetJointHandle(jointName);
                if (this.flyTiltJoint == INVALID_JOINT) {
                    gameLocal.Warning("Joint '%s' not found on '%s'", jointName, this.name);
                }
            }

            InitMuzzleFlash();

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetMass(this.spawnArgs.GetFloat("mass", "100"));

            if (this.spawnArgs.GetBool("big_monster")) {
                this.physicsObj.SetContents(0);
                this.physicsObj.SetClipMask(MASK_MONSTERSOLID & ~CONTENTS_BODY);
            } else {
                if (this.use_combat_bbox) {
                    this.physicsObj.SetContents(CONTENTS_BODY | CONTENTS_SOLID);
                } else {
                    this.physicsObj.SetContents(CONTENTS_BODY);
                }
                this.physicsObj.SetClipMask(MASK_MONSTERSOLID);
            }

            // move up to make sure the monster is at least an epsilon above the floor
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin().oPlus(new idVec3(0, 0, CM_CLIP_EPSILON)));

            if (this.num_cinematics != 0) {
                this.physicsObj.SetGravity(getVec3_origin());
            } else {
                final idVec3 gravity = this.spawnArgs.GetVector("gravityDir", "0 0 -1");
                gravity.oMulSet(g_gravity.GetFloat());
                this.physicsObj.SetGravity(gravity);
            }

            SetPhysics(this.physicsObj);

            this.physicsObj.GetGravityAxis().ProjectVector(this.viewAxis.oGet(0), local_dir);
            this.current_yaw = local_dir.ToYaw();
            this.ideal_yaw = idMath.AngleNormalize180(this.current_yaw);

            this.move.blockTime = 0;

            SetAAS();

            this.projectile.oSet(null);
            this.projectileDef = null;
            this.projectileClipModel = null;
            final idStr projectileName = new idStr();
            if (this.spawnArgs.GetString("def_projectile", "", projectileName) && (projectileName.Length() != 0)) {
                this.projectileDef = gameLocal.FindEntityDefDict(projectileName);
                CreateProjectile(getVec3_origin(), this.viewAxis.oGet(0));
                this.projectileRadius = this.projectile.GetEntity().GetPhysics().GetClipModel().GetBounds().GetRadius();
                this.projectileVelocity = idProjectile.GetVelocity(this.projectileDef);
                this.projectileGravity = idProjectile.GetGravity(this.projectileDef);
                this.projectileSpeed = this.projectileVelocity.Length();
		        idEntity.delete(this.projectile.GetEntity());
                this.projectile.oSet(null);
            }

            this.particles.Clear();
            this.restartParticles = true;
            this.useBoneAxis = this.spawnArgs.GetBool("useBoneAxis");
            SpawnParticles("smokeParticleSystem");

            if ((this.num_cinematics != 0) || this.spawnArgs.GetBool("hide") || this.spawnArgs.GetBool("teleport") || this.spawnArgs.GetBool("trigger_anim")) {
                this.fl.takedamage = false;
                this.physicsObj.SetContents(0);
                this.physicsObj.GetClipModel().Unlink();
                Hide();
            } else {
                // play a looping ambient sound if we have one
                StartSound("snd_ambient", SND_CHANNEL_AMBIENT, 0, false, null);
            }

            if (this.health <= 0) {
                gameLocal.Warning("entity '%s' doesn't have health set", this.name);
                this.health = 1;
            }

            // set up monster chatter
            SetChatSound();

            BecomeActive(TH_THINK);

            if (this.af_push_moveables) {
                this.af.SetupPose(this, gameLocal.time);
                this.af.GetPhysics().EnableClip();
            }

            // init the move variables
            StopMove(MOVE_STATUS_DONE);
        }
//
//        public void HeardSound(idEntity ent, final String action);
//

        public idActor GetEnemy() {
            return this.enemy.GetEntity();
        }

        public void TalkTo(idActor actor) {
            if (this.talk_state != TALK_OK) {
                return;
            }

            this.talkTarget.oSet(actor);
            this.AI_TALK.operator(actor != null);
        }

        public talkState_t GetTalkState() {
            if ((this.talk_state != TALK_NEVER) && this.AI_DEAD.operator()) {
                return TALK_DEAD;
            }
            if (IsHidden()) {
                return TALK_NEVER;
            }
            return this.talk_state;
        }

        public boolean GetAimDir(final idVec3 firePos, idEntity aimAtEnt, final idEntity ignore, idVec3 aimDir) {
            idVec3 targetPos1 = new idVec3();
            idVec3 targetPos2 = new idVec3();
            idVec3 delta;
            float max_height;
            boolean result;

            // if no aimAtEnt or projectile set
            if ((null == aimAtEnt) || (null == this.projectileDef)) {
                aimDir.oSet(this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis()));
                return false;
            }

            if (this.projectileClipModel == null) {
                CreateProjectileClipModel();
            }

            if (aimAtEnt.equals(this.enemy.GetEntity())) {
                ((idActor) aimAtEnt).GetAIAimTargets(this.lastVisibleEnemyPos, targetPos1, targetPos2);
            } else if (aimAtEnt.IsType(idActor.class)) {
                ((idActor) aimAtEnt).GetAIAimTargets(aimAtEnt.GetPhysics().GetOrigin(), targetPos1, targetPos2);
            } else {
                targetPos1 = aimAtEnt.GetPhysics().GetAbsBounds().GetCenter();
                targetPos2 = targetPos1;
            }

            // try aiming for chest
            delta = firePos.oMinus(targetPos1);
            max_height = delta.LengthFast() * this.projectile_height_to_distance_ratio;
            result = PredictTrajectory(firePos, targetPos1, this.projectileSpeed, this.projectileGravity, this.projectileClipModel, MASK_SHOT_RENDERMODEL, max_height, ignore, aimAtEnt, ai_debugTrajectory.GetBool() ? 1000 : 0, aimDir);
            if (result || !aimAtEnt.IsType(idActor.class)) {
                return result;
            }

            // try aiming for head
            delta = firePos.oMinus(targetPos2);
            max_height = delta.LengthFast() * this.projectile_height_to_distance_ratio;
            result = PredictTrajectory(firePos, targetPos2, this.projectileSpeed, this.projectileGravity, this.projectileClipModel, MASK_SHOT_RENDERMODEL, max_height, ignore, aimAtEnt, ai_debugTrajectory.GetBool() ? 1000 : 0, aimDir);

            return result;
        }

        public void TouchedByFlashlight(idActor flashlight_owner) {
            if (this.wakeOnFlashlight) {
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
        }

        /*
         ============
         idAI::FindPathAroundObstacles

         Finds a path around dynamic obstacles using a path tree with clockwise and counter clockwise edge walks.
         ============
         */
        public static boolean FindPathAroundObstacles(final idPhysics physics, final idAAS aas, final idEntity ignore, final idVec3 startPos, final idVec3 seekPos, obstaclePath_s path) {
            int numObstacles, areaNum;
            final int[] insideObstacle = {0};
            final obstacle_s[] obstacles = Stream.generate(obstacle_s::new).limit(MAX_OBSTACLES).toArray(obstacle_s[]::new);
            final idBounds clipBounds = new idBounds();
            final idBounds bounds = new idBounds();
            pathNode_s root;
            boolean pathToGoalExists;

            path.seekPos.oSet(seekPos);
            path.firstObstacle = null;
            path.startPosOutsideObstacles.oSet(startPos);
            path.startPosObstacle = null;
            path.seekPosOutsideObstacles.oSet(seekPos);
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
            final pathTrace_s trace = new pathTrace_s();

            if ((aas != null) && (aas.GetSettings() != null)) {
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

            path.endPos.oSet(start);
            path.endVelocity.oSet(velocity);
            path.endNormal.Zero();
            path.endEvent = 0;
            path.endTime = 0;
            path.blockingEntity = null;

            curStart = start;
            curVelocity = velocity;

            numFrames = ((totalTime + frameTime) - 1) / frameTime;
            curFrameTime = frameTime;
            for (i = 0; i < numFrames; i++) {

                if (i == (numFrames - 1)) {
                    curFrameTime = totalTime - (i * curFrameTime);
                }

                delta = curVelocity.oMultiply(curFrameTime).oMultiply(0.001f);

                path.endVelocity.oSet(curVelocity);
                path.endTime = i * frameTime;

                // allow sliding along a few surfaces per frame
                for (j = 0; j < MAX_FRAME_SLIDE; j++) {

                    final idVec3 lineStart = curStart;

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
                            if (((lastEnd.oMinus(start)).LengthSqr() > ((trace.endPos.oMinus(start)).LengthSqr() - 0.1f))
                                    || ((trace.normal.oMultiply(invGravityDir)) < minFloorCos)) {
                                if ((stopEvent & SE_BLOCKED) != 0) {
                                    path.endPos.oSet(lastEnd);
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

                        path.endNormal.oSet(trace.normal);
                        path.blockingEntity = trace.blockingEntity;

                        // if the trace is not blocked or blocked by a floor surface
                        if ((trace.fraction >= 1.0f) || ((trace.normal.oMultiply(invGravityDir)) > minFloorCos)) {
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
                            path.endPos.oSet(curStart);
                            path.endEvent = SE_BLOCKED;

                            return true;
                        }
                    }
                }

                if (j >= MAX_FRAME_SLIDE) {
                    if ((stopEvent & SE_BLOCKED) != 0) {
                        path.endPos.oSet(curStart);
                        path.endEvent = SE_BLOCKED;
                        return true;
                    }
                }

                // add gravity
                curVelocity.oPluSet(gravity.oMultiply(frameTime).oMultiply(0.001f));
            }

            path.endTime = totalTime;
            path.endVelocity.oSet(curVelocity);
            path.endPos.oSet(curStart);
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
            final idVec3[] points = new idVec3[5];
            final trace_s[] trace = {null};
            boolean result;

            t = zVel / gravity;
            // maximum height of projectile
            maxHeight = start.z - (0.5f * gravity * (t * t));
            // time it takes to fall from the top to the end height
            t = idMath.Sqrt((maxHeight - end.z) / (0.5f * -gravity));

            // start of parabolic
            points[0] = start;

            if (t < time) {
                numSegments = 4;
                // point in the middle between top and start
                t2 = (time - t) * 0.5f;
                points[1].oSet(start.ToVec2().oPlus((end.ToVec2().oMinus(start.ToVec2())).oMultiply(t2 / time)));
                points[1].z = start.z + (t2 * zVel) + (0.5f * gravity * t2 * t2);
                // top of parabolic
                t2 = time - t;
                points[2].oSet(start.ToVec2().oPlus((end.ToVec2().oMinus(start.ToVec2())).oMultiply(t2 / time)));
                points[2].z = start.z + (t2 * zVel) + (0.5f * gravity * t2 * t2);
                // point in the middel between top and end
                t2 = time - (t * 0.5f);
                points[3].oSet(start.ToVec2().oPlus((end.ToVec2().oMinus(start.ToVec2())).oMultiply(t2 / time)));
                points[3].z = start.z + (t2 * zVel) + (0.5f * gravity * t2 * t2);
            } else {
                numSegments = 2;
                // point halfway through
                t2 = time * 0.5f;
                points[1].oSet(start.ToVec2().oPlus((end.ToVec2().oMinus(start.ToVec2())).oMultiply(0.5f)));
                points[1].z = start.z + (t2 * zVel) + (0.5f * gravity * t2 * t2);
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
                    final idBounds bnds = new idBounds(trace[0].endpos);
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
            final float[] s = {0}, c = {0};
            final trace_s[] trace = {null};
            final ballistics_s[] ballistics = new ballistics_s[2];
            final idVec3[] dir = new idVec3[2];
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
            if ((projectileSpeed <= 0.0f) || projGravity.equals(getVec3_origin())) {

                aimDir.oSet(target.oMinus(firePos));
                aimDir.Normalize();

                gameLocal.clip.Translation(trace, firePos, target, clip, getMat3_identity(), clipmask, ignore);

                if (drawtime != 0) {
                    gameRenderWorld.DebugLine(colorRed, firePos, target, drawtime);
                    final idBounds bnds = new idBounds(trace[0].endpos);
                    bnds.ExpandSelf(1.0f);
                    gameRenderWorld.DebugBounds(((trace[0].fraction >= 1.0f) || (gameLocal.GetTraceEntity(trace[0]) == targetEntity)) ? colorGreen : colorYellow, bnds, getVec3_zero(), drawtime);
                }

                return ((trace[0].fraction >= 1.0f) || (gameLocal.GetTraceEntity(trace[0]) == targetEntity));
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
                pitch = DEG2RAD(ballistics[i].angle);
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
            final idStr use_aas = new idStr();

            this.spawnArgs.GetString("use_aas", null, use_aas);
            this.aas = gameLocal.GetAAS(use_aas.getData());
            if (this.aas != null) {
                final idAASSettings settings = this.aas.GetSettings();
                if (settings != null) {
                    if (!ValidForBounds(settings, this.physicsObj.GetBounds())) {
                        idGameLocal.Error("%s cannot use use_aas %s\n", this.name, use_aas);
                    }
                    final float height = settings.maxStepHeight[0];
                    this.physicsObj.SetMaxStepHeight(height);
                    return;
                } else {
                    this.aas = null;
                }
            }
            gameLocal.Printf("WARNING: %s has no AAS file\n", this.name);
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
            if (this.particles.Num() != 0) {
                for (int i = 0; i < this.particles.Num(); i++) {
                    this.particles.oGet(i).time = 0;
                }
            }

            if (this.enemyNode.InList()) {
                // remove ourselves from the enemy's enemylist
                this.enemyNode.Remove();
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
            if ((this.enemy.GetEntity() != null) && !this.enemyNode.InList()) {
                // let our enemy know we're back on the trail
                this.enemyNode.AddToEnd(this.enemy.GetEntity().enemyList);
            }

            if (this.particles.Num() != 0) {
                for (int i = 0; i < this.particles.Num(); i++) {
                    this.particles.oGet(i).time = gameLocal.time;
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

            if ((this.thinkFlags & TH_THINK) != 0) {
                // clear out the enemy when he dies or is hidden
                final idActor enemyEnt = this.enemy.GetEntity();
                if (enemyEnt != null) {
                    if (enemyEnt.health <= 0) {
                        EnemyDead();
                    }
                }

                this.current_yaw += this.deltaViewAngles.yaw;
                this.ideal_yaw = idMath.AngleNormalize180(this.ideal_yaw + this.deltaViewAngles.yaw);
                this.deltaViewAngles.Zero();
                this.viewAxis = new idAngles(0, this.current_yaw, 0).ToMat3();

                if (this.num_cinematics != 0) {
                    if (!IsHidden() && this.torsoAnim.AnimDone(0)) {
                        PlayCinematic();
                    }
                    RunPhysics();
                } else if (!this.allowHiddenMovement && IsHidden()) {
                    // hidden monsters
                    UpdateAIScript();
                } else {
                    // clear the ik before we do anything else so the skeleton doesn't get updated twice
                    this.walkIK.ClearJointMods();

                    switch (this.move.moveType) {
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
					default:
						// TODO check unused Enum case labels
						break;
                    }
                }

                // clear pain flag so that we recieve any damage between now and the next time we run the script
                this.AI_PAIN.operator(false);
                this.AI_SPECIAL_DAMAGE.operator(0f);
                this.AI_PUSHED.operator(false);
            } else if ((this.thinkFlags & TH_PHYSICS) != 0) {
                RunPhysics();
            }

            if (this.af_push_moveables) {
                PushWithAF();
            }

            if (this.fl.hidden && this.allowHiddenMovement) {
                // UpdateAnimation won't call frame commands when hidden, so call them here when we allow hidden movement
                this.animator.ServiceAnims(gameLocal.previousTime, gameLocal.time);
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

            if (this.AI_DEAD.operator()) {
                // ignore it when they're dead
                return;
            }

            // make sure he's not dormant
            this.dormantStart = 0;

            if (this.num_cinematics != 0) {
                PlayCinematic();
            } else {
                this.AI_ACTIVATED.operator(true);
                if (NOT(activator) || !activator.IsType(idPlayer.class)) {
                    player = gameLocal.GetLocalPlayer();
                } else {
                    player = ((idPlayer) activator);
                }

                if ((ReactionTo(player) & ATTACK_ON_ACTIVATE) != 0) {
                    SetEnemy(player);
                }

                // update the script in cinematics so that entities don't start anims or show themselves a frame late.
                if (this.cinematic) {
                    UpdateAIScript();

                    // make sure our model gets updated
                    this.animator.ForceUpdate();

                    // update the anim bounds
                    UpdateAnimation();
                    UpdateVisuals();
                    Present();

                    if (this.head.GetEntity() != null) {
                        // since the body anim was updated, we need to run physics to update the position of the head
                        RunPhysics();

                        // make sure our model gets updated
                        this.head.GetEntity().GetAnimator().ForceUpdate();

                        // update the anim bounds
                        this.head.GetEntity().UpdateAnimation();
                        this.head.GetEntity().UpdateVisuals();
                        this.head.GetEntity().Present();
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
            if (actor.team != this.team) {
                if (actor.fl.notarget) {
                    // don't attack on sight when attacker is notargeted
                    return ATTACK_ON_DAMAGE | ATTACK_ON_ACTIVATE;
                }
                return ATTACK_ON_SIGHT | ATTACK_ON_DAMAGE | ATTACK_ON_ACTIVATE;
            }

            // monsters will fight when attacked by lower ranked monsters.  rank 0 never fights back.
            if ((this.rank != 0) && (actor.rank < this.rank)) {
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
            this.AI_ENEMY_DEAD.operator(true);
        }

        /*
         ================
         idAI::CanPlayChatterSounds

         Used for playing chatter sounds on monsters.
         ================
         */
        @Override
        public boolean CanPlayChatterSounds() {
            if (this.AI_DEAD.operator()) {
                return false;
            }

            if (IsHidden()) {
                return false;
            }

            if (this.enemy.GetEntity() != null) {
                return true;
            }

            return !this.spawnArgs.GetBool("no_idle_chatter");
        }

        protected void SetChatSound() {
            final String snd;

            if (IsHidden()) {
                snd = null;
            } else if (this.enemy.GetEntity() != null) {
                snd = this.spawnArgs.GetString("snd_chatter_combat", null);
                this.chat_min = (int) SEC2MS(this.spawnArgs.GetFloat("chatter_combat_min", "5"));
                this.chat_max = (int) SEC2MS(this.spawnArgs.GetFloat("chatter_combat_max", "10"));
            } else if (!this.spawnArgs.GetBool("no_idle_chatter")) {
                snd = this.spawnArgs.GetString("snd_chatter", null);
                this.chat_min = (int) SEC2MS(this.spawnArgs.GetFloat("chatter_min", "5"));
                this.chat_max = (int) SEC2MS(this.spawnArgs.GetFloat("chatter_max", "10"));
            } else {
                snd = null;
            }

            if (isNotNullOrEmpty(snd)) {
                this.chat_snd = declManager.FindSound(snd);

                // set the next chat time
                this.chat_time = (int) (gameLocal.time + this.chat_min + (gameLocal.random.RandomFloat() * (this.chat_max - this.chat_min)));
            } else {
                this.chat_snd = null;
            }
        }

        protected void PlayChatter() {
            // check if it's time to play a chat sound
            if (this.AI_DEAD.operator() || NOT(this.chat_snd) || (this.chat_time > gameLocal.time)) {
                return;
            }

            StartSoundShader(this.chat_snd, SND_CHANNEL_VOICE, 0, false, null);

            // set the next chat time
            this.chat_time = (int) (gameLocal.time + this.chat_min + (gameLocal.random.RandomFloat() * (this.chat_max - this.chat_min)));
        }

        @Override
        public void Hide() {
            super.Hide();//TODO:expose multilayer inherited functions
            this.fl.takedamage = false;
            this.physicsObj.SetContents(0);
            this.physicsObj.GetClipModel().Unlink();
            StopSound(etoi(SND_CHANNEL_AMBIENT), false);
            SetChatSound();

            this.AI_ENEMY_IN_FOV.operator(false);
            this.AI_ENEMY_VISIBLE.operator(false);
            StopMove(MOVE_STATUS_DONE);
        }

        @Override
        public void Show() {
            super.Show();
            if (this.spawnArgs.GetBool("big_monster")) {
                this.physicsObj.SetContents(0);
            } else if (this.use_combat_bbox) {
                this.physicsObj.SetContents(CONTENTS_BODY | CONTENTS_SOLID);
            } else {
                this.physicsObj.SetContents(CONTENTS_BODY);
            }
            this.physicsObj.GetClipModel().Link(gameLocal.clip);
            this.fl.takedamage = !this.spawnArgs.GetBool("noDamage");
            SetChatSound();
            StartSound("snd_ambient", SND_CHANNEL_AMBIENT, 0, false, null);
        }

        protected idVec3 FirstVisiblePointOnPath(final idVec3 origin, final idVec3 target, int travelFlags) {
            int i, areaNum, targetAreaNum, curAreaNum;
            final int[] travelTime = {0};
            idVec3 curOrigin;
            final idReachability[] reach = {null};

            if (NOT(this.aas)) {
                return origin;
            }

            areaNum = PointReachableAreaNum(origin);
            targetAreaNum = PointReachableAreaNum(target);

            if ((0 == areaNum) || (0 == targetAreaNum)) {
                return origin;
            }

            if ((areaNum == targetAreaNum) || PointVisible(origin)) {
                return origin;
            }

            curAreaNum = areaNum;
            curOrigin = origin;

            for (i = 0; i < 10; i++) {

                if (!this.aas.RouteToGoalArea(curAreaNum, curOrigin, targetAreaNum, travelFlags, travelTime, reach)) {
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
            final frameCommand_t[] command = {null};
            final idMat3 axis = new idMat3();
            idAnim anim;
            int/*jointHandle_t*/ joint;

            modelDef = this.animator.ModelDef();
            if (null == modelDef) {
                return;
            }
            num = modelDef.NumAnims();

            // needs to be off while getting the offsets so that we account for the distance the monster moves in the attack anim
            this.animator.RemoveOriginOffset(false);

            // anim number 0 is reserved for non-existant anims.  to avoid off by one issues, just allocate an extra spot for
            // launch offsets so that anim number can be used without subtracting 1.
            this.missileLaunchOffset.SetGranularity(1);
            this.missileLaunchOffset.SetNum(num + 1);
            this.missileLaunchOffset.oSet(0, new idVec3());

            for (i = 1; i <= num; i++) {
                this.missileLaunchOffset.oSet(i, new idVec3());
                anim = modelDef.GetAnim(i);
                if (anim != null) {
                    frame = anim.FindFrameForFrameCommand(FC_LAUNCHMISSILE, command);
                    if (frame >= 0) {
                        joint = this.animator.GetJointHandle(command[0].string.getData());
                        if (joint == INVALID_JOINT) {
                            idGameLocal.Error("Invalid joint '%s' on 'launch_missile' frame command on frame %d of model '%s'", command[0].string.getData(), frame, modelDef.GetName());
                        }
                        GetJointTransformForAnim(joint, i, FRAME2MS(frame), this.missileLaunchOffset.oGet(i), axis);
                    }
                }
            }

            this.animator.RemoveOriginOffset(true);
        }

        protected void PlayCinematic() {
            final String[] animName = {null};

            if (this.current_cinematic >= this.num_cinematics) {
                if (g_debugCinematic.GetBool()) {
                    gameLocal.Printf("%d: '%s' stop\n", gameLocal.framenum, GetName());
                }
                if (!this.spawnArgs.GetBool("cinematic_no_hide")) {
                    Hide();
                }
                this.current_cinematic = 0;
                ActivateTargets(gameLocal.GetLocalPlayer());
                this.fl.neverDormant = false;
                return;
            }

            Show();
            this.current_cinematic++;

            this.allowJointMod = false;
            this.allowEyeFocus = false;

            this.spawnArgs.GetString(va("anim%d", this.current_cinematic), null, animName);
            if (null == animName[0]) {
                gameLocal.Warning("missing 'anim%d' key on %s", this.current_cinematic, this.name);
                return;
            }

            if (g_debugCinematic.GetBool()) {
                gameLocal.Printf("%d: '%s' start '%s'\n", gameLocal.framenum, GetName(), animName[0]);
            }

            this.headAnim.animBlendFrames = 0;
            this.headAnim.lastAnimBlendFrames = 0;
            this.headAnim.BecomeIdle();

            this.legsAnim.animBlendFrames = 0;
            this.legsAnim.lastAnimBlendFrames = 0;
            this.legsAnim.BecomeIdle();

            this.torsoAnim.animBlendFrames = 0;
            this.torsoAnim.lastAnimBlendFrames = 0;
            ProcessEvent(AI_PlayAnim, ANIMCHANNEL_TORSO, animName[0]);

            // make sure our model gets updated
            this.animator.ForceUpdate();

            // update the anim bounds
            UpdateAnimation();
            UpdateVisuals();
            Present();

            if (this.head.GetEntity() != null) {
                // since the body anim was updated, we need to run physics to update the position of the head
                RunPhysics();

                // make sure our model gets updated
                this.head.GetEntity().GetAnimator().ForceUpdate();

                // update the anim bounds
                this.head.GetEntity().UpdateAnimation();
                this.head.GetEntity().UpdateVisuals();
                this.head.GetEntity().Present();
            }

            this.fl.neverDormant = true;
        }

        // movement
        @Override
        public void ApplyImpulse(idEntity ent, int id, final idVec3 point, final idVec3 impulse) {
            // FIXME: Jim take a look at this and see if this is a reasonable thing to do
            // instead of a spawnArg flag.. Sabaoth is the only slide monster ( and should be the only one for D3 )
            // and we don't want him taking physics impulses as it can knock him off the path
            if ((this.move.moveType != MOVETYPE_STATIC) && (this.move.moveType != MOVETYPE_SLIDE)) {
                super.ApplyImpulse(ent, id, point, impulse);
            }
        }

        protected void GetMoveDelta(final idMat3 oldaxis, final idMat3 axis, idVec3 delta) {
            idVec3 oldModelOrigin;
            idVec3 modelOrigin;

            this.animator.GetDelta(gameLocal.time - idGameLocal.msec, gameLocal.time, delta);
            delta.oSet(axis.oMultiply(delta));

            if (!this.modelOffset.equals(getVec3_zero())) {
                // the pivot of the monster's model is around its origin, and not around the bounding
                // box's origin, so we have to compensate for this when the model is offset so that
                // the monster still appears to rotate around it's origin.
                oldModelOrigin = this.modelOffset.oMultiply(oldaxis);
                modelOrigin = this.modelOffset.oMultiply(axis);
                delta.oPluSet(oldModelOrigin.oMinus(modelOrigin));
            }

            delta.oMulSet(this.physicsObj.GetGravityAxis());
        }

        protected void CheckObstacleAvoidance(final idVec3 goalPos, idVec3 newPos) {
            idEntity obstacle;
            final obstaclePath_s path = new obstaclePath_s();
            idVec3 dir;
            float dist;
            boolean foundPath;

            if (this.ignore_obstacles) {
                newPos.oSet(goalPos);
                this.move.obstacle.oSet(null);
                return;
            }

            final idVec3 origin = this.physicsObj.GetOrigin();

            obstacle = null;
            this.AI_OBSTACLE_IN_PATH.operator(false);
            foundPath = FindPathAroundObstacles(this.physicsObj, this.aas, this.enemy.GetEntity(), origin, goalPos, path);
            if (ai_showObstacleAvoidance.GetBool()) {
                gameRenderWorld.DebugLine(colorBlue, goalPos.oPlus(new idVec3(1.0f, 1.0f, 0.0f)), goalPos.oPlus(new idVec3(1.0f, 1.0f, 64.0f)), idGameLocal.msec);
                gameRenderWorld.DebugLine(foundPath ? colorYellow : colorRed, path.seekPos, path.seekPos.oPlus(new idVec3(0.0f, 0.0f, 64.0f)), idGameLocal.msec);
            }

            if (!foundPath) {
                // couldn't get around obstacles
                if (path.firstObstacle != null) {
                    this.AI_OBSTACLE_IN_PATH.operator(true);
                    if (this.physicsObj.GetAbsBounds().Expand(2.0f).IntersectsBounds(path.firstObstacle.GetPhysics().GetAbsBounds())) {
                        obstacle = path.firstObstacle;
                    }
                } else if (path.startPosObstacle != null) {
                    this.AI_OBSTACLE_IN_PATH.operator(true);
                    if (this.physicsObj.GetAbsBounds().Expand(2.0f).IntersectsBounds(path.startPosObstacle.GetPhysics().GetAbsBounds())) {
                        obstacle = path.startPosObstacle;
                    }
                } else {
                    // Blocked by wall
                    this.move.moveStatus = MOVE_STATUS_BLOCKED_BY_WALL;
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
                this.AI_OBSTACLE_IN_PATH.operator(true);

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
                    if (obstacle == this.enemy.GetEntity()) {
                        this.move.moveStatus = MOVE_STATUS_BLOCKED_BY_ENEMY;
                    } else {
                        this.move.moveStatus = MOVE_STATUS_BLOCKED_BY_MONSTER;
                    }
                } else {
                    // try kicking the object out of the way
                    this.move.moveStatus = MOVE_STATUS_BLOCKED_BY_OBJECT;
                }
                newPos.oSet(obstacle.GetPhysics().GetOrigin());
                //newPos = path.seekPos;
                this.move.obstacle.oSet(obstacle);
            } else {
                newPos.oSet(path.seekPos);
                this.move.obstacle.oSet(null);
            }
        }

        protected void DeadMove() {
            final idVec3 delta = new idVec3();
            monsterMoveResult_t moveResult;

            final idVec3 org = this.physicsObj.GetOrigin();

            GetMoveDelta(this.viewAxis, this.viewAxis, delta);
            this.physicsObj.SetDelta(delta);

            RunPhysics();

            moveResult = this.physicsObj.GetMoveResult();
            this.AI_ONGROUND.operator(this.physicsObj.OnGround());
        }

        protected void AnimMove() {
            idVec3 goalPos = new idVec3();
            idVec3 delta = new idVec3();
            idVec3 goalDelta;
            float goalDist;
            monsterMoveResult_t moveResult;
            final idVec3 newDest = new idVec3();

            final idVec3 oldOrigin = this.physicsObj.GetOrigin();
            final idMat3 oldAxis = this.viewAxis;

            this.AI_BLOCKED.operator(false);

            if (etoi(this.move.moveCommand) < etoi(NUM_NONMOVING_COMMANDS)) {
                this.move.lastMoveOrigin.Zero();
                this.move.lastMoveTime = gameLocal.time;
            }

            this.move.obstacle.oSet(null);
            if ((this.move.moveCommand == MOVE_FACE_ENEMY) && (this.enemy.GetEntity() != null)) {
                TurnToward(this.lastVisibleEnemyPos);
                goalPos = oldOrigin;
            } else if ((this.move.moveCommand == MOVE_FACE_ENTITY) && (this.move.goalEntity.GetEntity() != null)) {
                TurnToward(this.move.goalEntity.GetEntity().GetPhysics().GetOrigin());
                goalPos = oldOrigin;
            } else if (GetMovePos(goalPos)) {
                if (this.move.moveCommand != MOVE_WANDER) {
                    CheckObstacleAvoidance(goalPos, newDest);
                    TurnToward(newDest);
                } else {
                    TurnToward(goalPos);
                }
            }

            Turn();

            if (this.move.moveCommand == MOVE_SLIDE_TO_POSITION) {
                if (gameLocal.time < (this.move.startTime + this.move.duration)) {
                    goalPos = this.move.moveDest.oMinus(this.move.moveDir.oMultiply(MS2SEC((this.move.startTime + this.move.duration) - gameLocal.time)));
                    delta = goalPos.oMinus(oldOrigin);
                    delta.z = 0.0f;
                } else {
                    delta = this.move.moveDest.oMinus(oldOrigin);
                    delta.z = 0.0f;
                    StopMove(MOVE_STATUS_DONE);
                }
            } else if (this.allowMove) {
                GetMoveDelta(oldAxis, this.viewAxis, delta);
            } else {
                delta.Zero();
            }

            if (this.move.moveCommand == MOVE_TO_POSITION) {
                goalDelta = this.move.moveDest.oMinus(oldOrigin);
                goalDist = goalDelta.LengthFast();
                if (goalDist < delta.LengthFast()) {
                    delta = goalDelta;
                }
            }

            this.physicsObj.SetDelta(delta);
            this.physicsObj.ForceDeltaMove(this.disableGravity);

            RunPhysics();

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorCyan, oldOrigin, this.physicsObj.GetOrigin(), 5000);
            }

            moveResult = this.physicsObj.GetMoveResult();
            if (!this.af_push_moveables && (this.attack.Length() != 0) && TestMelee()) {
                DirectDamage(this.attack, this.enemy.GetEntity());
            } else {
                final idEntity blockEnt = this.physicsObj.GetSlideMoveEntity();
                if ((blockEnt != null) && blockEnt.IsType(idMoveable.class) && blockEnt.GetPhysics().IsPushable()) {
                    KickObstacles(this.viewAxis.oGet(0), this.kickForce, blockEnt);
                }
            }

            BlockedFailSafe();

            this.AI_ONGROUND.operator(this.physicsObj.OnGround());

            final idVec3 org = this.physicsObj.GetOrigin();
            if (!oldOrigin.equals(org)) {//FIXME: so this checks value instead of refs which COULD go wrong!
                TouchTriggers();
            }

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugBounds(colorMagenta, this.physicsObj.GetBounds(), org, idGameLocal.msec);
                gameRenderWorld.DebugBounds(colorMagenta, this.physicsObj.GetBounds(), this.move.moveDest, idGameLocal.msec);
                gameRenderWorld.DebugLine(colorYellow, org.oPlus(EyeOffset()), org.oPlus(EyeOffset().oPlus(this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis().oMultiply(16.0f)))), idGameLocal.msec, true);
                DrawRoute();
            }
        }

        protected void SlideMove() {
            idVec3 goalPos = new idVec3();
            idVec3 delta = new idVec3();
            idVec3 goalDelta;
            float goalDist;
            monsterMoveResult_t moveResult;
            final idVec3 newDest = new idVec3();

            final idVec3 oldOrigin = this.physicsObj.GetOrigin();
            final idMat3 oldAxis = this.viewAxis;

            this.AI_BLOCKED.operator(false);

            if (etoi(this.move.moveCommand) < etoi(NUM_NONMOVING_COMMANDS)) {
                this.move.lastMoveOrigin.Zero();
                this.move.lastMoveTime = gameLocal.time;
            }

            this.move.obstacle.oSet(null);
            if ((this.move.moveCommand == MOVE_FACE_ENEMY) && (this.enemy.GetEntity() != null)) {
                TurnToward(this.lastVisibleEnemyPos);
                goalPos = this.move.moveDest;
            } else if ((this.move.moveCommand == MOVE_FACE_ENTITY) && (this.move.goalEntity.GetEntity() != null)) {
                TurnToward(this.move.goalEntity.GetEntity().GetPhysics().GetOrigin());
                goalPos = this.move.moveDest;
            } else if (GetMovePos(goalPos)) {
                CheckObstacleAvoidance(goalPos, newDest);
                TurnToward(newDest);
                goalPos = newDest;
            }

            if (this.move.moveCommand == MOVE_SLIDE_TO_POSITION) {
                if (gameLocal.time < (this.move.startTime + this.move.duration)) {
                    goalPos = this.move.moveDest.oMinus(this.move.moveDir.oMultiply(MS2SEC((this.move.startTime + this.move.duration) - gameLocal.time)));
                } else {
                    goalPos = this.move.moveDest;
                    StopMove(MOVE_STATUS_DONE);
                }
            }

            if (this.move.moveCommand == MOVE_TO_POSITION) {
                goalDelta = this.move.moveDest.oMinus(oldOrigin);
                goalDist = goalDelta.LengthFast();
                if (goalDist < delta.LengthFast()) {
                    delta = goalDelta;
                }
            }

            final idVec3 vel = this.physicsObj.GetLinearVelocity();
            final float z = vel.z;
            final idVec3 predictedPos = oldOrigin.oPlus(vel.oMultiply(AI_SEEK_PREDICTION));

            // seek the goal position
            goalDelta = goalPos.oMinus(predictedPos);
            vel.oMinSet(vel.oMultiply(AI_FLY_DAMPENING * MS2SEC(idGameLocal.msec)));
            vel.oPluSet(goalDelta.oMultiply(MS2SEC(idGameLocal.msec)));

            // cap our speed
            vel.Truncate(this.fly_speed);
            vel.z = z;
            this.physicsObj.SetLinearVelocity(vel);
            this.physicsObj.UseVelocityMove(true);
            RunPhysics();

            if ((this.move.moveCommand == MOVE_FACE_ENEMY) && (this.enemy.GetEntity() != null)) {
                TurnToward(this.lastVisibleEnemyPos);
            } else if ((this.move.moveCommand == MOVE_FACE_ENTITY) && (this.move.goalEntity.GetEntity() != null)) {
                TurnToward(this.move.goalEntity.GetEntity().GetPhysics().GetOrigin());
            } else if (this.move.moveCommand != MOVE_NONE) {
                if (vel.ToVec2().LengthSqr() > 0.1f) {
                    TurnToward(vel.ToYaw());
                }
            }
            Turn();

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorCyan, oldOrigin, this.physicsObj.GetOrigin(), 5000);
            }

            moveResult = this.physicsObj.GetMoveResult();
            if (!this.af_push_moveables && (this.attack.Length() != 0) && TestMelee()) {
                DirectDamage(this.attack, this.enemy.GetEntity());
            } else {
                final idEntity blockEnt = this.physicsObj.GetSlideMoveEntity();
                if ((blockEnt != null) && blockEnt.IsType(idMoveable.class) && blockEnt.GetPhysics().IsPushable()) {
                    KickObstacles(this.viewAxis.oGet(0), this.kickForce, blockEnt);
                }
            }

            BlockedFailSafe();

            this.AI_ONGROUND.operator(this.physicsObj.OnGround());

            final idVec3 org = this.physicsObj.GetOrigin();
            if (oldOrigin != org) {
                TouchTriggers();
            }

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugBounds(colorMagenta, this.physicsObj.GetBounds(), org, idGameLocal.msec);
                gameRenderWorld.DebugBounds(colorMagenta, this.physicsObj.GetBounds(), this.move.moveDest, idGameLocal.msec);
                gameRenderWorld.DebugLine(colorYellow, org.oPlus(EyeOffset()), org.oPlus(EyeOffset().oPlus(this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis().oMultiply(16.0f)))), idGameLocal.msec, true);
                DrawRoute();
            }
        }

        protected void AdjustFlyingAngles() {
            idVec3 vel;
            float speed;
            float roll;
            float pitch;

            vel = this.physicsObj.GetLinearVelocity();

            speed = vel.Length();
            if (speed < 5.0f) {
                roll = 0.0f;
                pitch = 0.0f;
            } else {
                roll = vel.oMultiply(this.viewAxis.oGet(1).oMultiply(-this.fly_roll_scale / this.fly_speed));
                if (roll > this.fly_roll_max) {
                    roll = this.fly_roll_max;
                } else if (roll < -this.fly_roll_max) {
                    roll = -this.fly_roll_max;
                }

                pitch = vel.oMultiply(this.viewAxis.oGet(2).oMultiply(-this.fly_pitch_scale / this.fly_speed));
                if (pitch > this.fly_pitch_max) {
                    pitch = this.fly_pitch_max;
                } else if (pitch < -this.fly_pitch_max) {
                    pitch = -this.fly_pitch_max;
                }
            }

            this.fly_roll = (this.fly_roll * 0.95f) + (roll * 0.05f);
            this.fly_pitch = (this.fly_pitch * 0.95f) + (pitch * 0.05f);

            if (this.flyTiltJoint != INVALID_JOINT) {
                this.animator.SetJointAxis(this.flyTiltJoint, JOINTMOD_WORLD, new idAngles(this.fly_pitch, 0.0f, this.fly_roll).ToMat3());
            } else {
                this.viewAxis = new idAngles(this.fly_pitch, this.current_yaw, this.fly_roll).ToMat3();
            }
        }

        protected void AddFlyBob(idVec3 vel) {
            idVec3 fly_bob_add;
            float t;

            if (this.fly_bob_strength != 0) {
                t = MS2SEC(gameLocal.time + (this.entityNumber * 497));
                fly_bob_add = (this.viewAxis.oGet(1).oMultiply(idMath.Sin16(t * this.fly_bob_horz)).oPlus(this.viewAxis.oGet(2).oMultiply(idMath.Sin16(t * this.fly_bob_vert)))).oMultiply(this.fly_bob_strength);
                vel.oPluSet(fly_bob_add.oMultiply(MS2SEC(idGameLocal.msec)));
                if (ai_debugMove.GetBool()) {
                    final idVec3 origin = this.physicsObj.GetOrigin();
                    gameRenderWorld.DebugArrow(colorOrange, origin, origin.oPlus(fly_bob_add), 0);
                }
            }
        }

        protected void AdjustFlyHeight(idVec3 vel, final idVec3 goalPos) {
            final idVec3 origin = this.physicsObj.GetOrigin();
            final predictedPath_s path = new predictedPath_s();
            idVec3 end;
            idVec3 dest;
            final trace_s[] trace = {null};
            idActor enemyEnt;
            boolean goLower;

            // make sure we're not flying too high to get through doors
            goLower = false;
            if (origin.z > goalPos.z) {
                dest = goalPos;
                dest.z = origin.z + 128.0f;
                idAI.PredictPath(this, this.aas, goalPos, dest.oMinus(origin), 1000, 1000, SE_BLOCKED, path);
                if (path.endPos.z < origin.z) {
                    final idVec3 addVel = Seek(vel, origin, path.endPos, AI_SEEK_PREDICTION);
                    vel.z += addVel.z;
                    goLower = true;
                }

                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugBounds(goLower ? colorRed : colorGreen, this.physicsObj.GetBounds(), path.endPos, idGameLocal.msec);
                }
            }

            if (!goLower) {
                // make sure we don't fly too low
                end = origin;

                enemyEnt = this.enemy.GetEntity();
                if (enemyEnt != null) {
                    end.z = this.lastVisibleEnemyPos.z + this.lastVisibleEnemyEyeOffset.z + this.fly_offset;
                } else {
                    // just use the default eye height for the player
                    end.z = goalPos.z + DEFAULT_FLY_OFFSET + this.fly_offset;
                }

                gameLocal.clip.Translation(trace, origin, end, this.physicsObj.GetClipModel(), getMat3_identity(), MASK_MONSTERSOLID, this);
                vel.oPluSet(Seek(vel, origin, trace[0].endpos, AI_SEEK_PREDICTION));
            }
        }

        protected void FlySeekGoal(idVec3 vel, idVec3 goalPos) {
            idVec3 seekVel;

            // seek the goal position
            seekVel = Seek(vel, this.physicsObj.GetOrigin(), goalPos, AI_SEEK_PREDICTION);
            seekVel.oMulSet(this.fly_seek_scale);
            vel.oPluSet(seekVel);
        }

        protected void AdjustFlySpeed(idVec3 vel) {
            float speed;

            // apply dampening
            vel.oMinSet(vel.oMultiply(AI_FLY_DAMPENING * MS2SEC(idGameLocal.msec)));

            // gradually speed up/slow down to desired speed
            speed = vel.Normalize();
            speed += (this.move.speed - speed) * MS2SEC(idGameLocal.msec);
            if (speed < 0.0f) {
                speed = 0.0f;
            } else if ((this.move.speed != 0) && (speed > this.move.speed)) {
                speed = this.move.speed;
            }

            vel.oMulSet(speed);
        }

        protected void FlyTurn() {
            if (this.move.moveCommand == MOVE_FACE_ENEMY) {
                TurnToward(this.lastVisibleEnemyPos);
            } else if ((this.move.moveCommand == MOVE_FACE_ENTITY) && (this.move.goalEntity.GetEntity() != null)) {
                TurnToward(this.move.goalEntity.GetEntity().GetPhysics().GetOrigin());
            } else if (this.move.speed > 0.0f) {
                final idVec3 vel = this.physicsObj.GetLinearVelocity();
                if (vel.ToVec2().LengthSqr() > 0.1f) {
                    TurnToward(vel.ToYaw());
                }
            }
            Turn();
        }

        protected void FlyMove() {
            idVec3 goalPos = new idVec3();
            idVec3 oldorigin;
            final idVec3 newDest = new idVec3();

            this.AI_BLOCKED.operator(false);
            if ((this.move.moveCommand != MOVE_NONE) && ReachedPos(this.move.moveDest, this.move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
            }

            if (ai_debugMove.GetBool()) {
                gameLocal.Printf("%d: %s: %s, vel = %.2f, sp = %.2f, maxsp = %.2f\n", gameLocal.time, this.name, moveCommandString[etoi(this.move.moveCommand)], this.physicsObj.GetLinearVelocity().Length(), this.move.speed, this.fly_speed);
            }

            if (this.move.moveCommand != MOVE_TO_POSITION_DIRECT) {
                final idVec3 vel = this.physicsObj.GetLinearVelocity();

                if (GetMovePos(goalPos)) {
                    CheckObstacleAvoidance(goalPos, newDest);
                    goalPos = newDest;
                }

                if (this.move.speed != 0) {
                    FlySeekGoal(vel, goalPos);
                }

                // add in bobbing
                AddFlyBob(vel);

                if ((this.enemy.GetEntity() != null) && (this.move.moveCommand != MOVE_TO_POSITION)) {
                    AdjustFlyHeight(vel, goalPos);
                }

                AdjustFlySpeed(vel);

                this.physicsObj.SetLinearVelocity(vel);
            }

            // turn
            FlyTurn();

            // run the physics for this frame
            oldorigin = this.physicsObj.GetOrigin();
            this.physicsObj.UseFlyMove(true);
            this.physicsObj.UseVelocityMove(false);
            this.physicsObj.SetDelta(getVec3_zero());
            this.physicsObj.ForceDeltaMove(this.disableGravity);
            RunPhysics();

            final monsterMoveResult_t moveResult = this.physicsObj.GetMoveResult();
            if (!this.af_push_moveables && (this.attack.Length() != 0) && TestMelee()) {
                DirectDamage(this.attack, this.enemy.GetEntity());
            } else {
                final idEntity blockEnt = this.physicsObj.GetSlideMoveEntity();
                if ((blockEnt != null) && blockEnt.IsType(idMoveable.class) && blockEnt.GetPhysics().IsPushable()) {
                    KickObstacles(this.viewAxis.oGet(0), this.kickForce, blockEnt);
                } else if (moveResult == MM_BLOCKED) {
                    this.move.blockTime = gameLocal.time + 500;
                    this.AI_BLOCKED.operator(true);
                }
            }

            final idVec3 org = this.physicsObj.GetOrigin();
            if (oldorigin != org) {
                TouchTriggers();
            }

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorCyan, oldorigin, this.physicsObj.GetOrigin(), 4000);
                gameRenderWorld.DebugBounds(colorOrange, this.physicsObj.GetBounds(), org, idGameLocal.msec);
                gameRenderWorld.DebugBounds(colorMagenta, this.physicsObj.GetBounds(), this.move.moveDest, idGameLocal.msec);
                gameRenderWorld.DebugLine(colorRed, org, org.oPlus(this.physicsObj.GetLinearVelocity()), idGameLocal.msec, true);
                gameRenderWorld.DebugLine(colorBlue, org, goalPos, idGameLocal.msec, true);
                gameRenderWorld.DebugLine(colorYellow, org.oPlus(EyeOffset()), org.oPlus(EyeOffset().oPlus(this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis().oMultiply(16.0f)))), idGameLocal.msec, true);
                DrawRoute();
            }
        }

        protected void StaticMove() {
            final idActor enemyEnt = this.enemy.GetEntity();

            if (this.AI_DEAD.operator()) {
                return;
            }

            if ((this.move.moveCommand == MOVE_FACE_ENEMY) && (enemyEnt != null)) {
                TurnToward(this.lastVisibleEnemyPos);
            } else if ((this.move.moveCommand == MOVE_FACE_ENTITY) && (this.move.goalEntity.GetEntity() != null)) {
                TurnToward(this.move.goalEntity.GetEntity().GetPhysics().GetOrigin());
            } else if (this.move.moveCommand != MOVE_NONE) {
                TurnToward(this.move.moveDest);
            }
            Turn();

            this.physicsObj.ForceDeltaMove(true); // disable gravity
            RunPhysics();

            this.AI_ONGROUND.operator(false);

            if (!this.af_push_moveables && (this.attack.Length() != 0) && TestMelee()) {
                DirectDamage(this.attack, enemyEnt);
            }

            if (ai_debugMove.GetBool()) {
                final idVec3 org = this.physicsObj.GetOrigin();
                gameRenderWorld.DebugBounds(colorMagenta, this.physicsObj.GetBounds(), org, idGameLocal.msec);
                gameRenderWorld.DebugLine(colorBlue, org, this.move.moveDest, idGameLocal.msec, true);
                gameRenderWorld.DebugLine(colorYellow, org.oPlus(EyeOffset()), org.oPlus(EyeOffset().oPlus(this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis().oMultiply(16.0f)))), idGameLocal.msec, true);
            }
        }

        // damage
        @Override
        public boolean Pain(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            idActor actor;

            this.AI_PAIN.operator(super.Pain(inflictor, attacker, damage, dir, location));
            this.AI_DAMAGE.operator(true);

            // force a blink
            this.blink_time = 0;

            // ignore damage from self
            if (attacker != this) {
                if (inflictor != null) {
                    this.AI_SPECIAL_DAMAGE.operator(inflictor.spawnArgs.GetInt("special_damage") * 1f);
                } else {
                    this.AI_SPECIAL_DAMAGE.operator(0f);
                }

                if ((this.enemy.GetEntity() != attacker) && attacker.IsType(idActor.class)) {
                    actor = (idActor) attacker;
                    if ((ReactionTo(actor) & ATTACK_ON_DAMAGE) != 0) {
                        gameLocal.AlertAI(actor);
                        SetEnemy(actor);
                    }
                }
            }

            return (this.AI_PAIN.operator() /*!= 0*/);
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            final idAngles ang;
            final String[] modelDeath = {null};

            // make sure the monster is activated
            EndAttack();

            if (g_debugDamage.GetBool()) {
                gameLocal.Printf("Damage: joint: '%s', zone '%s'\n", this.animator.GetJointName(location),
                        GetDamageGroup(location));
            }

            if (inflictor != null) {
                this.AI_SPECIAL_DAMAGE.operator(inflictor.spawnArgs.GetInt("special_damage") * 1f);
            } else {
                this.AI_SPECIAL_DAMAGE.operator(0f);
            }

            if (this.AI_DEAD.operator()) {
                this.AI_PAIN.operator(true);
                this.AI_DAMAGE.operator(true);
                return;
            }

            // stop all voice sounds
            StopSound(etoi(SND_CHANNEL_VOICE), false);
            if (this.head.GetEntity() != null) {
                this.head.GetEntity().StopSound(etoi(SND_CHANNEL_VOICE), false);
                this.head.GetEntity().GetAnimator().ClearAllAnims(gameLocal.time, 100);
            }

            this.disableGravity = false;
            this.move.moveType = MOVETYPE_DEAD;
            this.af_push_moveables = false;

            this.physicsObj.UseFlyMove(false);
            this.physicsObj.ForceDeltaMove(false);

            // end our looping ambient sound
            StopSound(etoi(SND_CHANNEL_AMBIENT), false);

            if ((attacker != null) && attacker.IsType(idActor.class)) {
                gameLocal.AlertAI(attacker);
            }

            // activate targets
            ActivateTargets(attacker);

            RemoveAttachments();
            RemoveProjectile();
            StopMove(MOVE_STATUS_DONE);

            ClearEnemy();
            this.AI_DEAD.operator(true);

            // make monster nonsolid
            this.physicsObj.SetContents(0);
            this.physicsObj.GetClipModel().Unlink();

            Unbind();

            if (StartRagdoll()) {
                StartSound("snd_death", SND_CHANNEL_VOICE, 0, false, null);
            }

            if (this.spawnArgs.GetString("model_death", "", modelDeath)) {
                // lost soul is only case that does not use a ragdoll and has a model_death so get the death sound in here
                StartSound("snd_death", SND_CHANNEL_VOICE, 0, false, null);
                this.renderEntity.shaderParms[ SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                SetModel(modelDeath[0]);
                this.physicsObj.SetLinearVelocity(getVec3_zero());
                this.physicsObj.PutToRest();
                this.physicsObj.DisableImpact();
            }

            this.restartParticles = false;

            this.state = GetScriptFunction("state_Killed");
            SetState(this.state);
            SetWaitState("");

            idKeyValue kv = this.spawnArgs.MatchPrefix("def_drops", null);
            while (kv != null) {
                final idDict args = new idDict();

                args.Set("classname", kv.GetValue());
                args.Set("origin", this.physicsObj.GetOrigin().ToString());
                gameLocal.SpawnEntityDef(args);
                kv = this.spawnArgs.MatchPrefix("def_drops", kv);
            }

            if (((attacker != null) && attacker.IsType(idPlayer.class)) && ((inflictor != null) && !inflictor.IsType(idSoulCubeMissile.class))) {
                ((idPlayer) attacker).AddAIKill();
            }
        }

        // navigation
        protected void KickObstacles(final idVec3 dir, float force, idEntity alwaysKick) {
            int i, numListedClipModels;
            idBounds clipBounds;
            idEntity obEnt;
            idClipModel clipModel;
            final idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];
            int clipmask;
            idVec3 org;
            idVec3 forceVec;
            idVec3 delta;
            final idVec2 perpendicular = new idVec2();

            org = this.physicsObj.GetOrigin();

            // find all possible obstacles
            clipBounds = this.physicsObj.GetAbsBounds();
            clipBounds.TranslateSelf(dir.oMultiply(32.0f));
            clipBounds.ExpandSelf(8.0f);
            clipBounds.AddPoint(org);
            clipmask = this.physicsObj.GetClipMask();
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
                    delta.ToVec2_oPluSet(perpendicular.oMultiply(gameLocal.random.CRandomFloat() * 0.5f));
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
                delta.ToVec2_oPluSet(perpendicular.oMultiply(gameLocal.random.CRandomFloat() * 0.5f));
                forceVec = delta.oMultiply(force * alwaysKick.GetPhysics().GetMass());
                alwaysKick.ApplyImpulse(this, 0, alwaysKick.GetPhysics().GetOrigin(), forceVec);
            }
        }

        protected boolean ReachedPos(final idVec3 pos, final moveCommand_t moveCommand) {
            if (this.move.moveType == MOVETYPE_SLIDE) {
                final idBounds bnds = new idBounds(new idVec3(-4, -4.0f, -8.0f), new idVec3(4.0f, 4.0f, 64.0f));
                bnds.TranslateSelf(this.physicsObj.GetOrigin());
                if (bnds.ContainsPoint(pos)) {
                    return true;
                }
            } else {
                if ((moveCommand == MOVE_TO_ENEMY) || (moveCommand == MOVE_TO_ENTITY)) {
                    if (this.physicsObj.GetAbsBounds().IntersectsBounds(new idBounds(pos).Expand(8.0f))) {
                        return true;
                    }
                } else {
                    final idBounds bnds = new idBounds(new idVec3(-16.0f, -16.0f, -8.0f), new idVec3(16.0f, 16.0f, 64.0f));
                    bnds.TranslateSelf(this.physicsObj.GetOrigin());
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

            if (NOT(this.aas)) {
                // no aas, so just take the straight line distance
                delta = end.ToVec2().oMinus(start.ToVec2());
                dist = delta.LengthFast();

                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugLine(colorBlue, start, end, idGameLocal.msec, false);
                    gameRenderWorld.DrawText(va("%d", (int) dist), (start.oPlus(end)).oMultiply(0.5f), 0.1f, colorWhite, gameLocal.GetLocalPlayer().viewAngles.ToMat3());
                }

                return dist;
            }

            fromArea = PointReachableAreaNum(start);
            toArea = PointReachableAreaNum(end);

            if ((0 == fromArea) || (0 == toArea)) {
                // can't seem to get there
                return -1;
            }

            if (fromArea == toArea) {
                // same area, so just take the straight line distance
                delta = end.ToVec2().oMinus(start.ToVec2());
                dist = delta.LengthFast();

                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugLine(colorBlue, start, end, idGameLocal.msec, false);
                    gameRenderWorld.DrawText(va("%d", (int) dist), (start.oPlus(end)).oMultiply(0.5f), 0.1f, colorWhite, gameLocal.GetLocalPlayer().viewAngles.ToMat3());
                }

                return dist;
            }

            final idReachability[] reach = {null};
            final int[] travelTime = {0};
            if (!this.aas.RouteToGoalArea(fromArea, start, toArea, this.travelFlags, travelTime, reach)) {
                return -1;
            }

            if (ai_debugMove.GetBool()) {
                if (this.move.moveType == MOVETYPE_FLY) {
                    this.aas.ShowFlyPath(start, toArea, end);
                } else {
                    this.aas.ShowWalkPath(start, toArea, end);
                }
            }

            return travelTime[0];
        }

        protected int PointReachableAreaNum(final idVec3 pos, final float boundsScale /*= 2.0f*/) {
            int areaNum;
            idVec3 size;
            final idBounds bounds = new idBounds();

            if (NOT(this.aas)) {
                return 0;
            }

            size = this.aas.GetSettings().boundingBoxes[0].oGet(1).oMultiply(boundsScale);
            bounds.oSet(0, size.oNegative());
            size.z = 32.0f;
            bounds.oSet(1, size);

            if (this.move.moveType == MOVETYPE_FLY) {
                areaNum = this.aas.PointReachableAreaNum(pos, bounds, AREA_REACHABLE_WALK | AREA_REACHABLE_FLY);
            } else {
                areaNum = this.aas.PointReachableAreaNum(pos, bounds, AREA_REACHABLE_WALK);
            }

            return areaNum;
        }

        protected int PointReachableAreaNum(final idVec3 pos) {
            return PointReachableAreaNum(pos, 2.0f);
        }

        protected boolean PathToGoal(aasPath_s path, int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin) {
            idVec3 org;
            idVec3 goal;

            if (NOT(this.aas)) {
                return false;
            }

            org = origin;
            this.aas.PushPointIntoAreaNum(areaNum, org);
            if (0 == areaNum) {
                return false;
            }

            goal = goalOrigin;
            this.aas.PushPointIntoAreaNum(goalAreaNum, goal);
            if (0 == goalAreaNum) {
                return false;
            }

            if (this.move.moveType == MOVETYPE_FLY) {
                return this.aas.FlyPathToGoal(path, areaNum, org, goalAreaNum, goal, this.travelFlags);
            } else {
                return this.aas.WalkPathToGoal(path, areaNum, org, goalAreaNum, goal, this.travelFlags);
            }
        }

        protected void DrawRoute() {
            if ((this.aas != null) && (this.move.toAreaNum != 0) && (this.move.moveCommand != MOVE_NONE) && (this.move.moveCommand != MOVE_WANDER) && (this.move.moveCommand != MOVE_FACE_ENEMY)
                    && (this.move.moveCommand != MOVE_FACE_ENTITY) && (this.move.moveCommand != MOVE_TO_POSITION_DIRECT)) {
                if (this.move.moveType == MOVETYPE_FLY) {
                    this.aas.ShowFlyPath(this.physicsObj.GetOrigin(), this.move.toAreaNum, this.move.moveDest);
                } else {
                    this.aas.ShowWalkPath(this.physicsObj.GetOrigin(), this.move.toAreaNum, this.move.moveDest);
                }
            }
        }

        protected boolean GetMovePos(idVec3 seekPos) {
            int areaNum;
            final aasPath_s path = new aasPath_s();
            boolean result;
            idVec3 org;

            org = this.physicsObj.GetOrigin();
            seekPos.oSet(org);

            switch (this.move.moveCommand) {
                case MOVE_NONE:
                    seekPos.oSet(this.move.moveDest);
                    return false;

                case MOVE_FACE_ENEMY:
                case MOVE_FACE_ENTITY:
                    seekPos.oSet(this.move.moveDest);
                    return false;

                case MOVE_TO_POSITION_DIRECT:
                    seekPos.oSet(this.move.moveDest);
                    if (ReachedPos(this.move.moveDest, this.move.moveCommand)) {
                        StopMove(MOVE_STATUS_DONE);
                    }
                    return false;

                case MOVE_SLIDE_TO_POSITION:
                    seekPos.oSet(org);
                    return false;
			default:
				// TODO check unused Enum case labels
				break;
            }

            if (this.move.moveCommand == MOVE_TO_ENTITY) {
                MoveToEntity(this.move.goalEntity.GetEntity());
            }

            this.move.moveStatus = MOVE_STATUS_MOVING;
            result = false;
            if (gameLocal.time > this.move.blockTime) {
                if (this.move.moveCommand == MOVE_WANDER) {
                    this.move.moveDest = org.oPlus(this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis().oMultiply(256.0f)));
                } else {
                    if (ReachedPos(this.move.moveDest, this.move.moveCommand)) {
                        StopMove(MOVE_STATUS_DONE);
                        seekPos.oSet(org);
                        return false;
                    }
                }

                if ((this.aas != null) && (this.move.toAreaNum != 0)) {
                    areaNum = PointReachableAreaNum(org);
                    if (PathToGoal(path, areaNum, org, this.move.toAreaNum, this.move.moveDest)) {
                        seekPos.oSet(path.moveGoal);
                        result = true;
                        this.move.nextWanderTime = 0;
                    } else {
                        this.AI_DEST_UNREACHABLE.operator(true);
                    }
                }
            }

            if (!result) {
                // wander around
                if ((gameLocal.time > this.move.nextWanderTime) || !StepDirection(this.move.wanderYaw)) {
                    result = NewWanderDir(this.move.moveDest);
                    if (!result) {
                        StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                        this.AI_DEST_UNREACHABLE.operator(true);
                        seekPos.oSet(org);
                        return false;
                    }
                } else {
                    result = true;
                }

                seekPos.oSet(org.oPlus(this.move.moveDir.oMultiply(2048.0f)));
                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugLine(colorYellow, org, seekPos, idGameLocal.msec, true);
                }
            } else {
                this.AI_DEST_UNREACHABLE.operator(false);
            }

            if (result && (ai_debugMove.GetBool())) {
                gameRenderWorld.DebugLine(colorCyan, this.physicsObj.GetOrigin(), seekPos);
            }

            return result;
        }

        protected boolean MoveDone() {
            return (this.move.moveCommand == MOVE_NONE);
        }

        protected boolean EntityCanSeePos(idActor actor, final idVec3 actorOrigin, final idVec3 pos) {
            idVec3 eye, point;
            final trace_s[] results = {null};
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

            this.physicsObj.DisableClip();

            gameLocal.clip.TracePoint(results, eye, point, MASK_SOLID, actor);
            if ((results[0].fraction >= 1.0f) || (gameLocal.GetTraceEntity(results[0]) == this)) {
                this.physicsObj.EnableClip();
                return true;
            }

            final idBounds bounds = this.physicsObj.GetBounds();
            point.oPluSet(2, bounds.oGet(1, 2) - bounds.oGet(0, 2));

            gameLocal.clip.TracePoint(results, eye, point, MASK_SOLID, actor);
            this.physicsObj.EnableClip();

            return (results[0].fraction >= 1.0f) || (gameLocal.GetTraceEntity(results[0]) == this);
        }

        protected void BlockedFailSafe() {
            if (!ai_blockedFailSafe.GetBool() || (this.blockedRadius < 0.0f)) {
                return;
            }
            if (!this.physicsObj.OnGround() || (this.enemy.GetEntity() == null)
                    || ((this.physicsObj.GetOrigin().oMinus(this.move.lastMoveOrigin)).LengthSqr() > Square(this.blockedRadius))) {
                this.move.lastMoveOrigin = this.physicsObj.GetOrigin();
                this.move.lastMoveTime = gameLocal.time;
            }
            if (this.move.lastMoveTime < (gameLocal.time - this.blockedMoveTime)) {
                if (this.lastAttackTime < (gameLocal.time - this.blockedAttackTime)) {
                    this.AI_BLOCKED.operator(true);
                    this.move.lastMoveTime = gameLocal.time;
                }
            }
        }

        // movement control
        protected void StopMove(moveStatus_t status) {
            this.AI_MOVE_DONE.operator(true);
            this.AI_FORWARD.operator(false);
            this.move.moveCommand = MOVE_NONE;
            this.move.moveStatus = status;
            this.move.toAreaNum = 0;
            this.move.goalEntity.oSet(null);
            this.move.moveDest = this.physicsObj.GetOrigin();
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_OBSTACLE_IN_PATH.operator(false);
            this.AI_BLOCKED.operator(false);
            this.move.startTime = gameLocal.time;
            this.move.duration = 0;
            this.move.range = 0.0f;
            this.move.speed = 0.0f;
            this.move.anim = 0;
            this.move.moveDir.Zero();
            this.move.lastMoveOrigin.Zero();
            this.move.lastMoveTime = gameLocal.time;
        }

        /*
         =====================
         idAI::FaceEnemy

         Continually face the enemy's last known position.  MoveDone is always true in this case.
         =====================
         */
        protected boolean FaceEnemy() {
            final idActor enemyEnt = this.enemy.GetEntity();
            if (null == enemyEnt) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            TurnToward(this.lastVisibleEnemyPos);
            this.move.goalEntity.oSet(enemyEnt);
            this.move.moveDest = this.physicsObj.GetOrigin();
            this.move.moveCommand = MOVE_FACE_ENEMY;
            this.move.moveStatus = MOVE_STATUS_WAITING;
            this.move.startTime = gameLocal.time;
            this.move.speed = 0.0f;
            this.AI_MOVE_DONE.operator(true);
            this.AI_FORWARD.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);

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

            final idVec3 entityOrg = ent.GetPhysics().GetOrigin();
            TurnToward(entityOrg);
            this.move.goalEntity.oSet(ent);
            this.move.moveDest = this.physicsObj.GetOrigin();
            this.move.moveCommand = MOVE_FACE_ENTITY;
            this.move.moveStatus = MOVE_STATUS_WAITING;
            this.move.startTime = gameLocal.time;
            this.move.speed = 0.0f;
            this.AI_MOVE_DONE.operator(true);
            this.AI_FORWARD.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);

            return true;
        }

        protected boolean DirectMoveToPosition(final idVec3 pos) {
            if (ReachedPos(pos, this.move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            this.move.moveDest.oSet(pos);
            this.move.goalEntity.oSet(null);
            this.move.moveCommand = MOVE_TO_POSITION_DIRECT;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.startTime = gameLocal.time;
            this.move.speed = this.fly_speed;
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(true);

            if (this.move.moveType == MOVETYPE_FLY) {
                final idVec3 dir = pos.oMinus(this.physicsObj.GetOrigin());
                dir.Normalize();
                dir.oMulSet(this.fly_speed);
                this.physicsObj.SetLinearVelocity(dir);
            }

            return true;
        }

        protected boolean MoveToEnemyHeight() {
            final idActor enemyEnt = this.enemy.GetEntity();

            if ((null == enemyEnt) || (this.move.moveType != MOVETYPE_FLY)) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            this.move.moveDest.z = this.lastVisibleEnemyPos.z + enemyEnt.EyeOffset().z + this.fly_offset;
            this.move.goalEntity.oSet(enemyEnt);
            this.move.moveCommand = MOVE_TO_ENEMYHEIGHT;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.startTime = gameLocal.time;
            this.move.speed = 0.0f;
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(false);

            return true;
        }

        protected boolean MoveOutOfRange(idEntity ent, float range) {
            int areaNum;
            final aasObstacle_s[] obstacle = new aasObstacle_s[1];
            final aasGoal_s goal = new aasGoal_s();
//            idBounds bounds;
            idVec3 pos;

            if ((null == this.aas) || (null == ent)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                this.AI_DEST_UNREACHABLE.operator(true);
                return false;
            }

            final idVec3 org = this.physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);

            // consider the entity the monster is getting close to as an obstacle
            obstacle[0].absBounds = ent.GetPhysics().GetAbsBounds();

            if (ent == this.enemy.GetEntity()) {
                pos = this.lastVisibleEnemyPos;
            } else {
                pos = ent.GetPhysics().GetOrigin();
            }

            final idAASFindAreaOutOfRange findGoal = new idAASFindAreaOutOfRange(pos, range);
            if (!this.aas.FindNearestGoal(goal, areaNum, org, pos, this.travelFlags, obstacle, 1, findGoal)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                this.AI_DEST_UNREACHABLE.operator(true);
                return false;
            }

            if (ReachedPos(goal.origin, this.move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            this.move.moveDest.oSet(goal.origin);
            this.move.toAreaNum = goal.areaNum;
            this.move.goalEntity.oSet(ent);
            this.move.moveCommand = MOVE_OUT_OF_RANGE;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.range = range;
            this.move.speed = this.fly_speed;
            this.move.startTime = gameLocal.time;
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(true);

            return true;
        }

        protected boolean MoveToAttackPosition(idEntity ent, int attack_anim) {
            int areaNum;
            final aasObstacle_s[] obstacle = new aasObstacle_s[1];
            final aasGoal_s goal = new aasGoal_s();
            final idBounds bounds;
            idVec3 pos;

            if ((null == this.aas) || (null == ent)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                this.AI_DEST_UNREACHABLE.operator(true);
                return false;
            }

            final idVec3 org = this.physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);

            // consider the entity the monster is getting close to as an obstacle
            obstacle[0].absBounds = ent.GetPhysics().GetAbsBounds();

            if (ent == this.enemy.GetEntity()) {
                pos = this.lastVisibleEnemyPos;
            } else {
                pos = ent.GetPhysics().GetOrigin();
            }

            final idAASFindAttackPosition findGoal = new idAASFindAttackPosition(this, this.physicsObj.GetGravityAxis(), ent, pos, this.missileLaunchOffset.oGet(attack_anim));
            if (!this.aas.FindNearestGoal(goal, areaNum, org, pos, this.travelFlags, obstacle, 1, findGoal)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                this.AI_DEST_UNREACHABLE.operator(true);
                return false;
            }

            this.move.moveDest.oSet(goal.origin);
            this.move.toAreaNum = goal.areaNum;
            this.move.goalEntity.oSet(ent);
            this.move.moveCommand = MOVE_TO_ATTACK_POSITION;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.speed = this.fly_speed;
            this.move.startTime = gameLocal.time;
            this.move.anim = attack_anim;
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(true);

            return true;
        }

        protected boolean MoveToEnemy() {
            int areaNum;
            final aasPath_s path = new aasPath_s();
            final idActor enemyEnt = this.enemy.GetEntity();

            if (null == enemyEnt) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            if (ReachedPos(this.lastVisibleReachableEnemyPos, MOVE_TO_ENEMY)) {
                if (!ReachedPos(this.lastVisibleEnemyPos, MOVE_TO_ENEMY) || !this.AI_ENEMY_VISIBLE.operator()) {
                    StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                    this.AI_DEST_UNREACHABLE.operator(true);
                    return false;
                }
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            final idVec3 pos = this.lastVisibleReachableEnemyPos;

            this.move.toAreaNum = 0;
            if (this.aas != null) {
                this.move.toAreaNum = PointReachableAreaNum(pos);
                this.aas.PushPointIntoAreaNum(this.move.toAreaNum, pos);

                areaNum = PointReachableAreaNum(this.physicsObj.GetOrigin());
                if (!PathToGoal(path, areaNum, this.physicsObj.GetOrigin(), this.move.toAreaNum, pos)) {
                    this.AI_DEST_UNREACHABLE.operator(true);
                    return false;
                }
            }

            if (0 == this.move.toAreaNum) {
                // if only trying to update the enemy position
                if (this.move.moveCommand == MOVE_TO_ENEMY) {
                    if (NOT(this.aas)) {
                        // keep the move destination up to date for wandering
                        this.move.moveDest.oSet(pos);
                    }
                    return false;
                }

                if (!NewWanderDir(pos)) {
                    StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                    this.AI_DEST_UNREACHABLE.operator(true);
                    return false;
                }
            }

            if (this.move.moveCommand != MOVE_TO_ENEMY) {
                this.move.moveCommand = MOVE_TO_ENEMY;
                this.move.startTime = gameLocal.time;
            }

            this.move.moveDest.oSet(pos);
            this.move.goalEntity.oSet(enemyEnt);
            this.move.speed = this.fly_speed;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(true);

            return true;
        }

        protected boolean MoveToEntity(idEntity ent) {
            int areaNum;
            final aasPath_s path = new aasPath_s();
            idVec3 pos;

            if (null == ent) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
                return false;
            }

            pos = ent.GetPhysics().GetOrigin();
            if ((this.move.moveType != MOVETYPE_FLY) && ((this.move.moveCommand != MOVE_TO_ENTITY) || (this.move.goalEntityOrigin != pos))) {
                ent.GetFloorPos(64.0f, pos);
            }

            if (ReachedPos(pos, MOVE_TO_ENTITY)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            this.move.toAreaNum = 0;
            if (this.aas != null) {
                this.move.toAreaNum = PointReachableAreaNum(pos);
                this.aas.PushPointIntoAreaNum(this.move.toAreaNum, pos);

                areaNum = PointReachableAreaNum(this.physicsObj.GetOrigin());
                if (!PathToGoal(path, areaNum, this.physicsObj.GetOrigin(), this.move.toAreaNum, pos)) {
                    this.AI_DEST_UNREACHABLE.operator(true);
                    return false;
                }
            }

            if (0 == this.move.toAreaNum) {
                // if only trying to update the entity position
                if (this.move.moveCommand == MOVE_TO_ENTITY) {
                    if (NOT(this.aas)) {
                        // keep the move destination up to date for wandering
                        this.move.moveDest.oSet(pos);
                    }
                    return false;
                }

                if (!NewWanderDir(pos)) {
                    StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                    this.AI_DEST_UNREACHABLE.operator(true);
                    return false;
                }
            }

            if ((this.move.moveCommand != MOVE_TO_ENTITY) || (!this.move.goalEntity.GetEntity().equals(ent))) {
                this.move.startTime = gameLocal.time;
                this.move.goalEntity.oSet(ent);
                this.move.moveCommand = MOVE_TO_ENTITY;
            }

            this.move.moveDest.oSet(pos);
            this.move.goalEntityOrigin = ent.GetPhysics().GetOrigin();
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.speed = this.fly_speed;
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(true);

            return true;
        }

        protected boolean MoveToPosition(final idVec3 pos) {
            idVec3 org;
            int areaNum;
            final aasPath_s path = new aasPath_s();

            if (ReachedPos(pos, this.move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            org = pos;
            this.move.toAreaNum = 0;
            if (this.aas != null) {
                this.move.toAreaNum = PointReachableAreaNum(org);
                this.aas.PushPointIntoAreaNum(this.move.toAreaNum, org);

                areaNum = PointReachableAreaNum(this.physicsObj.GetOrigin());
                if (!PathToGoal(path, areaNum, this.physicsObj.GetOrigin(), this.move.toAreaNum, org)) {
                    StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                    this.AI_DEST_UNREACHABLE.operator(true);
                    return false;
                }
            }

            if ((0 == this.move.toAreaNum) && !NewWanderDir(org)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                this.AI_DEST_UNREACHABLE.operator(true);
                return false;
            }

            this.move.moveDest.oSet(org);
            this.move.goalEntity.oSet(null);
            this.move.moveCommand = MOVE_TO_POSITION;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.startTime = gameLocal.time;
            this.move.speed = this.fly_speed;
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(true);

            return true;
        }

        protected boolean MoveToCover(idEntity entity, final idVec3 hideFromPos) {
            int areaNum;
            final aasObstacle_s[] obstacle = {new aasObstacle_s()};
            final aasGoal_s hideGoal = new aasGoal_s();
//            idBounds bounds;

            if ((null == this.aas) || (null == entity)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                this.AI_DEST_UNREACHABLE.operator(true);
                return false;
            }

            final idVec3 org = this.physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);

            // consider the entity the monster tries to hide from as an obstacle
            obstacle[0].absBounds = entity.GetPhysics().GetAbsBounds();

            final idAASFindCover findCover = new idAASFindCover(hideFromPos);
            if (!this.aas.FindNearestGoal(hideGoal, areaNum, org, hideFromPos, this.travelFlags, obstacle, 1, findCover)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                this.AI_DEST_UNREACHABLE.operator(true);
                return false;
            }

            if (ReachedPos(hideGoal.origin, this.move.moveCommand)) {
                StopMove(MOVE_STATUS_DONE);
                return true;
            }

            this.move.moveDest.oSet(hideGoal.origin);
            this.move.toAreaNum = hideGoal.areaNum;
            this.move.goalEntity.oSet(entity);
            this.move.moveCommand = MOVE_TO_COVER;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.startTime = gameLocal.time;
            this.move.speed = this.fly_speed;
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(true);

            return true;
        }

        protected boolean SlideToPosition(final idVec3 pos, float time) {
            StopMove(MOVE_STATUS_DONE);

            this.move.moveDest.oSet(pos);
            this.move.goalEntity.oSet(null);
            this.move.moveCommand = MOVE_SLIDE_TO_POSITION;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.startTime = gameLocal.time;
            this.move.duration = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(time));
            this.AI_MOVE_DONE.operator(false);
            this.AI_DEST_UNREACHABLE.operator(false);
            this.AI_FORWARD.operator(false);

            if (this.move.duration > 0) {
                this.move.moveDir = (pos.oMinus(this.physicsObj.GetOrigin())).oDivide(MS2SEC(this.move.duration));
                if (this.move.moveType != MOVETYPE_FLY) {
                    this.move.moveDir.z = 0.0f;
                }
                this.move.speed = this.move.moveDir.LengthFast();
            }

            return true;
        }

        protected boolean WanderAround() {
            StopMove(MOVE_STATUS_DONE);

            this.move.moveDest = this.physicsObj.GetOrigin().oPlus(this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis().oMultiply(256.0f)));
            if (!NewWanderDir(this.move.moveDest)) {
                StopMove(MOVE_STATUS_DEST_UNREACHABLE);
                this.AI_DEST_UNREACHABLE.operator(true);
                return false;
            }

            this.move.moveCommand = MOVE_WANDER;
            this.move.moveStatus = MOVE_STATUS_MOVING;
            this.move.startTime = gameLocal.time;
            this.move.speed = this.fly_speed;
            this.AI_MOVE_DONE.operator(false);
            this.AI_FORWARD.operator(true);

            return true;
        }

        protected boolean StepDirection(float dir) {
            final predictedPath_s path = new predictedPath_s();
            idVec3 org;

            this.move.wanderYaw = dir;
            this.move.moveDir = new idAngles(0, this.move.wanderYaw, 0).ToForward();

            org = this.physicsObj.GetOrigin();

            idAI.PredictPath(this, this.aas, org, this.move.moveDir.oMultiply(48.0f), 1000, 1000, (this.move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if ((path.blockingEntity != null) && ((this.move.moveCommand == MOVE_TO_ENEMY) || (this.move.moveCommand == MOVE_TO_ENTITY)) && (path.blockingEntity == this.move.goalEntity.GetEntity())) {
                // don't report being blocked if we ran into our goal entity
                return true;
            }

            if ((this.move.moveType == MOVETYPE_FLY) && (path.endEvent == SE_BLOCKED)) {
                float z;

                this.move.moveDir = path.endVelocity.oMultiply(1.0f / 48.0f);

                // trace down to the floor and see if we can go forward
                idAI.PredictPath(this, this.aas, org, new idVec3(0.0f, 0.0f, -1024.0f), 1000, 1000, SE_BLOCKED, path);

                final idVec3 floorPos = path.endPos;
                idAI.PredictPath(this, this.aas, floorPos, this.move.moveDir.oMultiply(48.0f), 1000, 1000, SE_BLOCKED, path);
                if (0 == path.endEvent) {
                    this.move.moveDir.z = -1.0f;
                    return true;
                }

                // trace up to see if we can go over something and go forward
                idAI.PredictPath(this, this.aas, org, new idVec3(0.0f, 0.0f, 256.0f), 1000, 1000, SE_BLOCKED, path);

                final idVec3 ceilingPos = path.endPos;

                for (z = org.z; z <= (ceilingPos.z + 64.0f); z += 64.0f) {
                    idVec3 start = new idVec3();
                    if (z <= ceilingPos.z) {
                        start.x = org.x;
                        start.y = org.y;
                        start.z = z;
                    } else {
                        start = ceilingPos;
                    }
                    idAI.PredictPath(this, this.aas, start, this.move.moveDir.oMultiply(48.0f), 1000, 1000, SE_BLOCKED, path);
                    if (0 == path.endEvent) {
                        this.move.moveDir.z = 1.0f;
                        return true;
                    }
                }
                return false;
            }

            return (path.endEvent == 0);
        }

        protected boolean NewWanderDir(final idVec3 dest) {
            float deltax, deltay;
            final float[] d = new float[3];
            float tdir, olddir, turnaround;

            this.move.nextWanderTime = (int) (gameLocal.time + ((gameLocal.random.RandomFloat() * 500) + 500));

            olddir = idMath.AngleNormalize360((int) (this.current_yaw / 45) * 45);
            turnaround = idMath.AngleNormalize360(olddir - 180);

            final idVec3 org = this.physicsObj.GetOrigin();
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
            if ((d[ 1] != DI_NODIR) && (d[ 2] != DI_NODIR)) {
                if (d[ 1] == 0) {
                    tdir = d[ 2] == 90 ? 45 : 315;
                } else {
                    tdir = d[ 2] == 90 ? 135 : 215;
                }

                if ((tdir != turnaround) && StepDirection(tdir)) {
                    return true;
                }
            }

            // try other directions
            if (((gameLocal.random.RandomInt() & 1) != 0) || (abs(deltay) > abs(deltax))) {
                tdir = d[ 1];
                d[ 1] = d[ 2];
                d[ 2] = tdir;
            }

            if ((d[ 1] != DI_NODIR) && (d[ 1] != turnaround) && StepDirection(d[1])) {
                return true;
            }

            if ((d[ 2] != DI_NODIR) && (d[ 2] != turnaround) && StepDirection(d[ 2])) {
                return true;
            }

            // there is no direct path to the player, so pick another direction
            if ((olddir != DI_NODIR) && StepDirection(olddir)) {
                return true;
            }

            // randomly determine direction of search
            if ((gameLocal.random.RandomInt() & 1) == 1) {
                for (tdir = 0; tdir <= 315; tdir += 45) {
                    if ((tdir != turnaround) && StepDirection(tdir)) {
                        return true;
                    }
                }
            } else {
                for (tdir = 315; tdir >= 0; tdir -= 45) {
                    if ((tdir != turnaround) && StepDirection(tdir)) {
                        return true;
                    }
                }
            }

            if ((turnaround != DI_NODIR) && StepDirection(turnaround)) {
                return true;
            }

            // can't move
            StopMove(MOVE_STATUS_DEST_UNREACHABLE);
            return false;
        }

        // effects
        protected idDeclParticle SpawnParticlesOnJoint(particleEmitter_s pe, final idStr particleName, final String jointName) {
            idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();

            if (!isNotNullOrEmpty(particleName)) {
//		memset( &pe, 0, sizeof( pe ) );//TODO:
                return pe.particle;
            }

            pe.joint = this.animator.GetJointHandle(jointName);
            if (pe.joint == INVALID_JOINT) {
                gameLocal.Warning("Unknown particleJoint '%s' on '%s'", jointName, this.name);
                pe.time = 0;
                pe.particle = null;
            } else {
                this.animator.GetJointTransform(pe.joint, gameLocal.time, origin, axis);
                origin = this.renderEntity.origin.oPlus(origin.oMultiply(this.renderEntity.axis));

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
            idKeyValue kv = this.spawnArgs.MatchPrefix(keyName, null);
            while (kv != null) {
                final particleEmitter_s pe = new particleEmitter_s();

                idStr particleName = kv.GetValue();

                if (particleName.Length() != 0) {

                    idStr jointName = kv.GetValue();
                    final int dash = jointName.Find('-');
                    if (dash > 0) {
                        particleName = particleName.Left(dash);
                        jointName = jointName.Right(jointName.Length() - dash - 1);
                    }

                    SpawnParticlesOnJoint(pe, particleName, jointName.getData());
                    this.particles.Append(pe);
                }

                kv = this.spawnArgs.MatchPrefix(keyName, kv);
            }
        }
//
//        protected boolean ParticlesActive();
//

        // turning
        protected boolean FacingIdeal() {
            float diff;

            if (0 == this.turnRate) {
                return true;
            }

            diff = idMath.AngleNormalize180(this.current_yaw - this.ideal_yaw);
            if (idMath.Fabs(diff) < 0.01f) {
                // force it to be exact
                this.current_yaw = this.ideal_yaw;
                return true;
            }

            return false;
        }

        protected void Turn() {
            float diff;
            float diff2;
            float turnAmount;
            animFlags_t animflags;

            if (0 == this.turnRate) {
                return;
            }

            // check if the animator has marker this anim as non-turning
            if (!this.legsAnim.Disabled() && !this.legsAnim.AnimDone(0)) {
                animflags = this.legsAnim.GetAnimFlags();
            } else {
                animflags = this.torsoAnim.GetAnimFlags();
            }
            if (animflags.ai_no_turn) {
                return;
            }

            if ((this.anim_turn_angles != 0) && animflags.anim_turn) {
                final idMat3 rotateAxis = new idMat3();

                // set the blend between no turn and full turn
                final float frac = this.anim_turn_amount / this.anim_turn_angles;
                this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(0, 1.0f - frac);
                this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(1, frac);
                this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(0, 1.0f - frac);
                this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(1, frac);

                // get the total rotation from the start of the anim
                this.animator.GetDeltaRotation(0, gameLocal.time, rotateAxis);
                this.current_yaw = idMath.AngleNormalize180(this.anim_turn_yaw + rotateAxis.oGet(0).ToYaw());
            } else {
                diff = idMath.AngleNormalize180(this.ideal_yaw - this.current_yaw);
                this.turnVel += AI_TURN_SCALE * diff * MS2SEC(idGameLocal.msec);
                if (this.turnVel > this.turnRate) {
                    this.turnVel = this.turnRate;
                } else if (this.turnVel < -this.turnRate) {
                    this.turnVel = -this.turnRate;
                }
                turnAmount = this.turnVel * MS2SEC(idGameLocal.msec);
                if ((diff >= 0.0f) && (turnAmount >= diff)) {
                    this.turnVel = diff / MS2SEC(idGameLocal.msec);
                    turnAmount = diff;
                } else if ((diff <= 0.0f) && (turnAmount <= diff)) {
                    this.turnVel = diff / MS2SEC(idGameLocal.msec);
                    turnAmount = diff;
                }
                this.current_yaw += turnAmount;
                this.current_yaw = idMath.AngleNormalize180(this.current_yaw);
                diff2 = idMath.AngleNormalize180(this.ideal_yaw - this.current_yaw);
                if (idMath.Fabs(diff2) < 0.1f) {
                    this.current_yaw = this.ideal_yaw;
                }
            }

            this.viewAxis = new idAngles(0, this.current_yaw, 0).ToMat3();

            if (ai_debugMove.GetBool()) {
                final idVec3 org = this.physicsObj.GetOrigin();
                gameRenderWorld.DebugLine(colorRed, org, org.oPlus(new idAngles(0, this.ideal_yaw, 0).ToForward().oMultiply(64)), idGameLocal.msec);
                gameRenderWorld.DebugLine(colorGreen, org, org.oPlus(new idAngles(0, this.current_yaw, 0).ToForward().oMultiply(48)), idGameLocal.msec);
                gameRenderWorld.DebugLine(colorYellow, org, org.oPlus(new idAngles(0, this.current_yaw + this.turnVel, 0).ToForward().oMultiply(32)), idGameLocal.msec);
            }
        }

        protected boolean TurnToward(float yaw) {
            this.ideal_yaw = idMath.AngleNormalize180(yaw);
            final boolean result = FacingIdeal();
            return result;
        }

        protected boolean TurnToward(final idVec3 pos) {
            idVec3 dir;
            final idVec3 local_dir = new idVec3();
            float lengthSqr;

            dir = pos.oMinus(this.physicsObj.GetOrigin());
            this.physicsObj.GetGravityAxis().ProjectVector(dir, local_dir);
            local_dir.z = 0.0f;
            lengthSqr = local_dir.LengthSqr();
            if ((lengthSqr > Square(2.0f)) || ((lengthSqr > Square(0.1f)) && (this.enemy.GetEntity() == null))) {
                this.ideal_yaw = idMath.AngleNormalize180(local_dir.ToYaw());
            }

            final boolean result = FacingIdeal();
            return result;
        }

        // enemy management
        protected void ClearEnemy() {
            if (this.move.moveCommand == MOVE_TO_ENEMY) {
                StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            }

            this.enemyNode.Remove();
            this.enemy.oSet(null);
            this.AI_ENEMY_IN_FOV.operator(false);
            this.AI_ENEMY_VISIBLE.operator(false);
            this.AI_ENEMY_DEAD.operator(true);

            SetChatSound();
        }

        protected boolean EnemyPositionValid() {
            final trace_s[] tr = {null};
            final idVec3 muzzle;
            final idMat3 axis;

            if (null == this.enemy.GetEntity()) {
                return false;
            }

            if (this.AI_ENEMY_VISIBLE.operator()) {
                return true;
            }

            gameLocal.clip.TracePoint(tr, GetEyePosition(), this.lastVisibleEnemyPos.oPlus(this.lastVisibleEnemyEyeOffset), MASK_OPAQUE, this);
            if (tr[0].fraction < 1.0f) {
                // can't see the area yet, so don't know if he's there or not
                return true;
            }

            return false;
        }

        protected void SetEnemyPosition() {
            final idActor enemyEnt = this.enemy.GetEntity();
            int enemyAreaNum;
            int areaNum;
            int lastVisibleReachableEnemyAreaNum = 0;
            final aasPath_s path = new aasPath_s();
            idVec3 pos = new idVec3();
            boolean onGround;

            if (null == enemyEnt) {
                return;
            }

            this.lastVisibleReachableEnemyPos = this.lastReachableEnemyPos;
            this.lastVisibleEnemyEyeOffset = enemyEnt.EyeOffset();
            this.lastVisibleEnemyPos = enemyEnt.GetPhysics().GetOrigin();
            if (this.move.moveType == MOVETYPE_FLY) {
                pos = this.lastVisibleEnemyPos;
                onGround = true;
            } else {
                onGround = enemyEnt.GetFloorPos(64.0f, pos);
                if (enemyEnt.OnLadder()) {
                    onGround = false;
                }
            }

            if (!onGround) {
                if (this.move.moveCommand == MOVE_TO_ENEMY) {
                    this.AI_DEST_UNREACHABLE.operator(true);
                }
                return;
            }

            // when we don't have an AAS, we can't tell if an enemy is reachable or not,
            // so just assume that he is.
            if (NOT(this.aas)) {
                this.lastVisibleReachableEnemyPos = this.lastVisibleEnemyPos;
                if (this.move.moveCommand == MOVE_TO_ENEMY) {
                    this.AI_DEST_UNREACHABLE.operator(false);
                }
                enemyAreaNum = 0;
//                areaNum = 0;
            } else {
                lastVisibleReachableEnemyAreaNum = this.move.toAreaNum;
                enemyAreaNum = PointReachableAreaNum(this.lastVisibleEnemyPos, 1.0f);
                if (0 == enemyAreaNum) {
                    enemyAreaNum = PointReachableAreaNum(this.lastReachableEnemyPos, 1.0f);
                    pos = this.lastReachableEnemyPos;
                }
                if (0 == enemyAreaNum) {
                    if (this.move.moveCommand == MOVE_TO_ENEMY) {
                        this.AI_DEST_UNREACHABLE.operator(true);
                    }
//                    areaNum = 0;
                } else {
                    final idVec3 org = this.physicsObj.GetOrigin();
                    areaNum = PointReachableAreaNum(org);
                    if (PathToGoal(path, areaNum, org, enemyAreaNum, pos)) {
                        this.lastVisibleReachableEnemyPos = pos;
                        lastVisibleReachableEnemyAreaNum = enemyAreaNum;
                        if (this.move.moveCommand == MOVE_TO_ENEMY) {
                            this.AI_DEST_UNREACHABLE.operator(false);
                        }
                    } else if (this.move.moveCommand == MOVE_TO_ENEMY) {
                        this.AI_DEST_UNREACHABLE.operator(true);
                    }
                }
            }

            if (this.move.moveCommand == MOVE_TO_ENEMY) {
                if (NOT(this.aas)) {
                    // keep the move destination up to date for wandering
                    this.move.moveDest.oSet(this.lastVisibleReachableEnemyPos);
                } else if (enemyAreaNum != 0) {
                    this.move.toAreaNum = lastVisibleReachableEnemyAreaNum;
                    this.move.moveDest.oSet(this.lastVisibleReachableEnemyPos);
                }

                if (this.move.moveType == MOVETYPE_FLY) {
                    final predictedPath_s path2 = new predictedPath_s();
                    final idVec3 end = this.move.moveDest;
                    end.z += enemyEnt.EyeOffset().z + this.fly_offset;
                    idAI.PredictPath(this, this.aas, this.move.moveDest, end.oMinus(this.move.moveDest), 1000, 1000, SE_BLOCKED, path2);
                    this.move.moveDest.oSet(path2.endPos);
                    this.move.toAreaNum = PointReachableAreaNum(this.move.moveDest, 1.0f);
                }
            }
        }

        protected void UpdateEnemyPosition() {
            final idActor enemyEnt = this.enemy.GetEntity();
            int enemyAreaNum;
            int areaNum;
            final aasPath_s path = new aasPath_s();
            final predictedPath_s predictedPath;
            idVec3 enemyPos = new idVec3();
            boolean onGround;

            if (null == enemyEnt) {
                return;
            }

            final idVec3 org = this.physicsObj.GetOrigin();

            if (this.move.moveType == MOVETYPE_FLY) {
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
                if (NOT(this.aas)) {
//                    enemyAreaNum = 0;
                    this.lastReachableEnemyPos = enemyPos;
                } else {
                    enemyAreaNum = PointReachableAreaNum(enemyPos, 1.0f);
                    if (enemyAreaNum != 0) {
                        areaNum = PointReachableAreaNum(org);
                        if (PathToGoal(path, areaNum, org, enemyAreaNum, enemyPos)) {
                            this.lastReachableEnemyPos = enemyPos;
                        }
                    }
                }
            }

            this.AI_ENEMY_IN_FOV.operator(false);
            this.AI_ENEMY_VISIBLE.operator(false);

            if (CanSee(enemyEnt, false)) {
                this.AI_ENEMY_VISIBLE.operator(true);
                if (CheckFOV(enemyEnt.GetPhysics().GetOrigin())) {
                    this.AI_ENEMY_IN_FOV.operator(true);
                }

                SetEnemyPosition();
            } else {
                // check if we heard any sounds in the last frame
                if (enemyEnt == gameLocal.GetAlertEntity()) {
                    final float dist = (enemyEnt.GetPhysics().GetOrigin().oMinus(org)).LengthSqr();
                    if (dist < Square(AI_HEARING_RANGE)) {
                        SetEnemyPosition();
                    }
                }
            }

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugBounds(colorLtGrey, enemyEnt.GetPhysics().GetBounds(), this.lastReachableEnemyPos, idGameLocal.msec);
                gameRenderWorld.DebugBounds(colorWhite, enemyEnt.GetPhysics().GetBounds(), this.lastVisibleReachableEnemyPos, idGameLocal.msec);
            }
        }

        protected void SetEnemy(idActor newEnemy) {
            final int[] enemyAreaNum = {0};

            if (this.AI_DEAD.operator()) {
                ClearEnemy();
                return;
            }

            this.AI_ENEMY_DEAD.operator(false);
            if (null == newEnemy) {
                ClearEnemy();
            } else if (this.enemy.GetEntity() != newEnemy) {
                this.enemy.oSet(newEnemy);
                this.enemyNode.AddToEnd(newEnemy.enemyList);
                if (newEnemy.health <= 0) {
                    EnemyDead();
                    return;
                }
                // let the monster know where the enemy is
                newEnemy.GetAASLocation(this.aas, this.lastReachableEnemyPos, enemyAreaNum);
                SetEnemyPosition();
                SetChatSound();

                this.lastReachableEnemyPos = this.lastVisibleEnemyPos;
                this.lastVisibleReachableEnemyPos = this.lastReachableEnemyPos;
                enemyAreaNum[0] = PointReachableAreaNum(this.lastReachableEnemyPos, 1.0f);
                if ((this.aas != null) && (enemyAreaNum[0] != 0)) {
                    this.aas.PushPointIntoAreaNum(enemyAreaNum[0], this.lastReachableEnemyPos);
                    this.lastVisibleReachableEnemyPos = this.lastReachableEnemyPos;
                }
            }
        }

        // attacks
        protected void CreateProjectileClipModel() {
            if (this.projectileClipModel == null) {
                final idBounds projectileBounds = new idBounds(getVec3_origin());
                projectileBounds.ExpandSelf(this.projectileRadius);
                this.projectileClipModel = new idClipModel(new idTraceModel(projectileBounds));
            }
        }

        protected idProjectile CreateProjectile(final idVec3 pos, final idVec3 dir) {
            final idEntity[] ent = {null};
            String clsname;

            if (null == this.projectile.GetEntity()) {
                gameLocal.SpawnEntityDef(this.projectileDef, ent, false);
                if (null == ent[0]) {
                    clsname = this.projectileDef.GetString("classname");
                    idGameLocal.Error("Could not spawn entityDef '%s'", clsname);
                }

                if (!ent[0].IsType(idProjectile.class)) {
                    clsname = ent[0].GetClassname();
                    idGameLocal.Error("'%s' is not an idProjectile", clsname);
                }
                this.projectile.oSet((idProjectile) ent[0]);
            }

            this.projectile.GetEntity().Create(this, pos, dir);

            return this.projectile.GetEntity();
        }

        protected void RemoveProjectile() {
            if (this.projectile.GetEntity() != null) {
                this.projectile.GetEntity().PostEventMS(EV_Remove, 0);
                this.projectile.oSet(null);
            }
        }

        protected idProjectile LaunchProjectile(final String jointname, idEntity target, boolean clampToAttackCone) {
            idVec3 muzzle = new idVec3();
            idVec3 dir = new idVec3();
            idVec3 start;
            final trace_s[] tr = {null};
            idBounds projBounds;
            final float[] distance = {0};
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

            if (null == this.projectileDef) {
                gameLocal.Warning("%s (%s) doesn't have a projectile specified", this.name, GetEntityDefName());
                return null;
            }

            attack_accuracy = this.spawnArgs.GetFloat("attack_accuracy", "7");
            attack_cone = this.spawnArgs.GetFloat("attack_cone", "70");
            projectile_spread = this.spawnArgs.GetFloat("projectile_spread", "0");
            num_projectiles = this.spawnArgs.GetInt("num_projectiles", "1");

            GetMuzzle(jointname, muzzle, axis);

            if (null == this.projectile.GetEntity()) {
                CreateProjectile(muzzle, axis.oGet(0));
            }

            lastProjectile = this.projectile.GetEntity();

            if (target != null) {
                tmp = target.GetPhysics().GetAbsBounds().GetCenter().oMinus(muzzle);
                tmp.Normalize();
                axis = tmp.ToMat3();
            } else {
                axis = this.viewAxis;
            }

            // rotate it because the cone points up by default
            tmp = axis.oGet(2);
            axis.oSet(2, axis.oGet(0));
            axis.oSet(0, tmp.oNegative());

            // make sure the projectile starts inside the monster bounding box
            final idBounds ownerBounds = this.physicsObj.GetAbsBounds();
            projClip = lastProjectile.GetPhysics().GetClipModel();
            projBounds = projClip.GetBounds().Rotate(axis);

            // check if the owner bounds is bigger than the projectile bounds
            if (((ownerBounds.oGet(1, 0) - ownerBounds.oGet(0, 0)) > (projBounds.oGet(1, 0) - projBounds.oGet(0, 0)))
                    && ((ownerBounds.oGet(1, 1) - ownerBounds.oGet(0, 1)) > (projBounds.oGet(1, 1) - projBounds.oGet(0, 1)))
                    && ((ownerBounds.oGet(1, 2) - ownerBounds.oGet(0, 2)) > (projBounds.oGet(1, 2) - projBounds.oGet(0, 2)))) {
                if ((ownerBounds.oMinus(projBounds)).RayIntersection(muzzle, this.viewAxis.oGet(0), distance)) {
                    start = muzzle.oPlus(this.viewAxis.oGet(0).oMultiply(distance[0]));
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
            final float t = MS2SEC(gameLocal.time + (this.entityNumber * 497));
            ang.pitch += idMath.Sin16(t * 5.1f) * attack_accuracy;
            ang.yaw += idMath.Sin16(t * 6.7f) * attack_accuracy;

            if (clampToAttackCone) {
                // clamp the attack direction to be within monster's attack cone so he doesn't do
                // things like throw the missile backwards if you're behind him
                diff = idMath.AngleDelta(ang.yaw, this.current_yaw);
                if (diff > attack_cone) {
                    ang.yaw = this.current_yaw + attack_cone;
                } else if (diff < -attack_cone) {
                    ang.yaw = this.current_yaw - attack_cone;
                }
            }

            axis = ang.ToMat3();

            final float spreadRad = DEG2RAD(projectile_spread);
            for (i = 0; i < num_projectiles; i++) {
                // spread the projectiles out
                angle = idMath.Sin(spreadRad * gameLocal.random.RandomFloat());
                spin = DEG2RAD(360.0f) * gameLocal.random.RandomFloat();
                dir = axis.oGet(0).oPlus(axis.oGet(2).oMultiply(angle * idMath.Sin(spin)).oMinus(axis.oGet(1).oMultiply(angle * idMath.Cos(spin))));
                dir.Normalize();

                // launch the projectile
                if (null == this.projectile.GetEntity()) {
                    CreateProjectile(muzzle, dir);
                }
                lastProjectile = this.projectile.GetEntity();
                lastProjectile.Launch(muzzle, dir, getVec3_origin());
                this.projectile.oSet(null);
            }

            TriggerWeaponEffects(muzzle);

            this.lastAttackTime = gameLocal.time;

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

            } else if (victim.equals(this.enemy.GetEntity())) {
                this.AI_HIT_ENEMY.operator(true);
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
                idGameLocal.Error("Unknown damage def '%s' on '%s'", meleeDefName, this.name);
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

            final idVec3 kickDir = new idVec3();
            meleeDef.GetVector("kickDir", "0 0 0", kickDir);

            idVec3 globalKickDir;
            globalKickDir = (this.viewAxis.oMultiply(this.physicsObj.GetGravityAxis())).oMultiply(kickDir);

            ent.Damage(this, this, globalKickDir, meleeDefName, 1.0f, INVALID_JOINT);

            // end the attack if we're a multiframe attack
            EndAttack();
        }

        protected void DirectDamage(final idStr meleeDefName, idEntity ent) {
            DirectDamage(meleeDefName.getData(), ent);
        }

        protected boolean TestMelee() {
            final trace_s[] trace = {null};
            final idActor enemyEnt = this.enemy.GetEntity();

            if ((null == enemyEnt) || (0 == this.melee_range)) {
                return false;
            }

            //FIXME: make work with gravity vector
            final idVec3 org = this.physicsObj.GetOrigin();
            final idBounds myBounds = this.physicsObj.GetBounds();
            final idBounds bounds = new idBounds();

            // expand the bounds out by our melee range
            bounds.oSet(0, 0, -this.melee_range);
            bounds.oSet(0, 1, -this.melee_range);
            bounds.oSet(0, 2, myBounds.oGet(0, 2) - 4.0f);
            bounds.oSet(1, 0, -this.melee_range);
            bounds.oSet(1, 1, -this.melee_range);
            bounds.oSet(1, 2, myBounds.oGet(1, 2) - 4.0f);
            bounds.TranslateSelf(org);

            final idVec3 enemyOrg = enemyEnt.GetPhysics().GetOrigin();
            final idBounds enemyBounds = enemyEnt.GetPhysics().GetBounds();
            enemyBounds.TranslateSelf(enemyOrg);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugBounds(colorYellow, bounds, getVec3_zero(), idGameLocal.msec);
            }

            if (!bounds.IntersectsBounds(enemyBounds)) {
                return false;
            }

            final idVec3 start = GetEyePosition();
            final idVec3 end = enemyEnt.GetEyePosition();

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
            final idActor enemyEnt = this.enemy.GetEntity();
            String p;
            idSoundShader shader;

            meleeDef = gameLocal.FindEntityDefDict(meleeDefName, false);
            if (null == meleeDef) {
                idGameLocal.Error("Unknown melee '%s'", meleeDefName);
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
            if (enemyEnt.IsType(idPlayer.class) && (g_skill.GetInteger() < 2)) {
                final int[] damage = {0}, armor = {0};
                final idPlayer player = (idPlayer) enemyEnt;
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

            final idVec3 kickDir = new idVec3();
            meleeDef.GetVector("kickDir", "0 0 0", kickDir);

            idVec3 globalKickDir;
            globalKickDir = (this.viewAxis.oMultiply(this.physicsObj.GetGravityAxis())).oMultiply(kickDir);

            enemyEnt.Damage(this, this, globalKickDir, meleeDefName, 1.0f, INVALID_JOINT);

            this.lastAttackTime = gameLocal.time;

            return true;
        }

        protected void BeginAttack(final String name) {
            this.attack.oSet(name);
            this.lastAttackTime = gameLocal.time;
        }

        protected void EndAttack() {
            this.attack.oSet("");
        }

        protected void PushWithAF() {
            int i, j;
            final afTouch_s[] touchList = new afTouch_s[MAX_GENTITIES];
            final idEntity[] pushed_ents = new idEntity[MAX_GENTITIES];
            idEntity ent;
            idVec3 vel;
            int num_pushed;

            num_pushed = 0;
            this.af.ChangePose(this, gameLocal.time);
            final int num = this.af.EntitiesTouchingAF(touchList);
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
                    if ((this.attack.Length() != 0) && ent.IsType(idActor.class)) {
                        ent.Damage(this, this, vel, this.attack.getData(), 1.0f, INVALID_JOINT);
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
                muzzle.oSet(this.physicsObj.GetOrigin().oPlus(this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis().oMultiply(14))));
                muzzle.oMinSet(this.physicsObj.GetGravityNormal().oMultiply(this.physicsObj.GetBounds().oGet(1).z * 0.5f));
            } else {
                joint = this.animator.GetJointHandle(jointname);
                if (joint == INVALID_JOINT) {
                    idGameLocal.Error("Unknown joint '%s' on %s", jointname, GetEntityDefName());
                }
                GetJointWorldTransform(joint, gameLocal.time, muzzle, axis);
            }
        }

        protected void InitMuzzleFlash() {
            final idStr shader = new idStr();
            final idVec3 flashColor = new idVec3();

            this.spawnArgs.GetString("mtr_flashShader", "muzzleflash", shader);
            this.spawnArgs.GetVector("flashColor", "0 0 0", flashColor);
            final float flashRadius = this.spawnArgs.GetFloat("flashRadius");
            this.flashTime = (int) SEC2MS(this.spawnArgs.GetFloat("flashTime", "0.25"));

//	memset( &worldMuzzleFlash, 0, sizeof ( worldMuzzleFlash ) );
            this.worldMuzzleFlash = new renderLight_s();

            this.worldMuzzleFlash.pointLight = true;
            this.worldMuzzleFlash.shader = declManager.FindMaterial(shader, false);
            this.worldMuzzleFlash.shaderParms[SHADERPARM_RED] = flashColor.oGet(0);
            this.worldMuzzleFlash.shaderParms[SHADERPARM_GREEN] = flashColor.oGet(1);
            this.worldMuzzleFlash.shaderParms[SHADERPARM_BLUE] = flashColor.oGet(2);
            this.worldMuzzleFlash.shaderParms[SHADERPARM_ALPHA] = 1.0f;
            this.worldMuzzleFlash.shaderParms[SHADERPARM_TIMESCALE] = 1.0f;
            this.worldMuzzleFlash.lightRadius.oSet(0, flashRadius);
            this.worldMuzzleFlash.lightRadius.oSet(1, flashRadius);
            this.worldMuzzleFlash.lightRadius.oSet(2, flashRadius);

            this.worldMuzzleFlashHandle = -1;
        }

        protected void TriggerWeaponEffects(final idVec3 muzzle) {
            final idVec3 org = new idVec3();
            final idMat3 axis = new idMat3();

            if (!g_muzzleFlash.GetBool()) {
                return;
            }

            // muzzle flash
            // offset the shader parms so muzzle flashes show up
            this.renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
            this.renderEntity.shaderParms[ SHADERPARM_DIVERSITY] = gameLocal.random.CRandomFloat();

            if (this.flashJointWorld != INVALID_JOINT) {
                GetJointWorldTransform(this.flashJointWorld, gameLocal.time, org, axis);

                if (this.worldMuzzleFlash.lightRadius.x > 0.0f) {
                    this.worldMuzzleFlash.axis = axis;
                    this.worldMuzzleFlash.shaderParms[SHADERPARM_TIMEOFFSET] = -MS2SEC(gameLocal.time);
                    if (this.worldMuzzleFlashHandle != - 1) {
                        gameRenderWorld.UpdateLightDef(this.worldMuzzleFlashHandle, this.worldMuzzleFlash);
                    } else {
                        this.worldMuzzleFlashHandle = gameRenderWorld.AddLightDef(this.worldMuzzleFlash);
                    }
                    this.muzzleFlashEnd = gameLocal.time + this.flashTime;
                    UpdateVisuals();
                }
            }
        }

        protected void UpdateMuzzleFlash() {
            if (this.worldMuzzleFlashHandle != -1) {
                if (gameLocal.time >= this.muzzleFlashEnd) {
                    gameRenderWorld.FreeLightDef(this.worldMuzzleFlashHandle);
                    this.worldMuzzleFlashHandle = -1;
                } else {
                    idVec3 muzzle = new idVec3();
                    this.animator.GetJointTransform(this.flashJointWorld, gameLocal.time, muzzle, this.worldMuzzleFlash.axis);
                    this.animator.GetJointTransform(this.flashJointWorld, gameLocal.time, muzzle, this.worldMuzzleFlash.axis);
                    muzzle = this.physicsObj.GetOrigin().oPlus((muzzle.oPlus(this.modelOffset)).oMultiply(this.viewAxis.oMultiply(this.physicsObj.GetGravityAxis())));
                    this.worldMuzzleFlash.origin = muzzle;
                    gameRenderWorld.UpdateLightDef(this.worldMuzzleFlashHandle, this.worldMuzzleFlash);
                }
            }
        }

        @Override
        public boolean UpdateAnimationControllers() {
            final idVec3 local;
            idVec3 focusPos;
            final idQuat jawQuat;
            idVec3 left;
            idVec3 dir;
            idVec3 orientationJointPos = new idVec3();
            final idVec3 localDir = new idVec3();
            final idAngles newLookAng = new idAngles();
            idAngles diff;
            final idMat3 mat;
            idMat3 axis = new idMat3();
            idMat3 orientationJointAxis = new idMat3();
            final idAFAttachment headEnt = this.head.GetEntity();
            idVec3 eyepos = new idVec3();
            final idVec3 pos;
            int i;
            final idAngles jointAng = new idAngles();
            float orientationJointYaw;

            if (this.AI_DEAD.operator()) {
                return super.UpdateAnimationControllers();
            }

            if (this.orientationJoint == INVALID_JOINT) {
                orientationJointAxis = this.viewAxis;
                orientationJointPos = this.physicsObj.GetOrigin();
                orientationJointYaw = this.current_yaw;
            } else {
                GetJointWorldTransform(this.orientationJoint, gameLocal.time, orientationJointPos, orientationJointAxis);
                orientationJointYaw = orientationJointAxis.oGet(2).ToYaw();
                orientationJointAxis = new idAngles(0.0f, orientationJointYaw, 0.0f).ToMat3();
            }

            if (this.focusJoint != INVALID_JOINT) {
                if (headEnt != null) {
                    headEnt.GetJointWorldTransform(this.focusJoint, gameLocal.time, eyepos, axis);
                } else {
                    GetJointWorldTransform(this.focusJoint, gameLocal.time, eyepos, axis);
                }
                this.eyeOffset.z = eyepos.z - this.physicsObj.GetOrigin().z;
                if (ai_debugMove.GetBool()) {
                    gameRenderWorld.DebugLine(colorRed, eyepos, eyepos.oPlus(orientationJointAxis.oGet(0).oMultiply(32.0f)), idGameLocal.msec);
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

            final idEntity focusEnt = this.focusEntity.GetEntity();
            if (!this.allowJointMod || !this.allowEyeFocus || (gameLocal.time >= this.focusTime)) {
                focusPos = GetEyePosition().oPlus(orientationJointAxis.oGet(0).oMultiply(512.0f));
            } else if (focusEnt == null) {
                // keep looking at last position until focusTime is up
                focusPos = this.currentFocusPos;
            } else if (focusEnt.equals(this.enemy.GetEntity())) {
                focusPos = this.lastVisibleEnemyPos.oPlus(this.lastVisibleEnemyEyeOffset).oMinus(this.enemy.GetEntity().GetPhysics().GetGravityNormal().oMultiply(this.eyeVerticalOffset));
            } else if (focusEnt.IsType(idActor.class)) {
                focusPos = ((idActor) focusEnt).GetEyePosition().oMinus(focusEnt.GetPhysics().GetGravityNormal().oMultiply(this.eyeVerticalOffset));
            } else {
                focusPos = focusEnt.GetPhysics().GetOrigin();
            }
            this.currentFocusPos = this.currentFocusPos.oPlus(focusPos.oMinus(this.currentFocusPos)).oMultiply(this.eyeFocusRate);
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
            diff = newLookAng.oMinus(this.lookAng);
            if (!this.eyeAng.equals(diff)) {
                this.eyeAng = diff;
                this.eyeAng.Clamp(this.eyeMin, this.eyeMax);
                final idAngles angDelta = diff.oMinus(this.eyeAng);
                if (!angDelta.Compare(getAng_zero(), 0.1f)) {
                    this.alignHeadTime = gameLocal.time;
                } else {
                    this.alignHeadTime = (int) (gameLocal.time + ((0.5f + (0.5f * gameLocal.random.RandomFloat())) * this.focusAlignTime));
                }
            }
            if (idMath.Fabs(newLookAng.yaw) < 0.1f) {
                this.alignHeadTime = gameLocal.time;
            }
            if ((gameLocal.time >= this.alignHeadTime) || (gameLocal.time < this.forceAlignHeadTime)) {
                this.alignHeadTime = (int) (gameLocal.time + ((0.5f + (0.5f * gameLocal.random.RandomFloat())) * this.focusAlignTime));
                this.destLookAng = newLookAng;
                this.destLookAng.Clamp(this.lookMin, this.lookMax);
            }
            diff = this.destLookAng.oMinus(this.lookAng);
            if ((this.lookMin.pitch == -180.0f) && (this.lookMax.pitch == 180.0f)) {
                if ((diff.pitch > 180.0f) || (diff.pitch <= -180.0f)) {
                    diff.pitch = 360.0f - diff.pitch;
                }
            }
            if ((this.lookMin.yaw == -180.0f) && (this.lookMax.yaw == 180.0f)) {
                if (diff.yaw > 180.0f) {
                    diff.yaw -= 360.0f;
                } else if (diff.yaw <= -180.0f) {
                    diff.yaw += 360.0f;
                }
            }
            this.lookAng = this.lookAng.oPlus(diff.oMultiply(this.headFocusRate));

            this.lookAng.Normalize180();
            jointAng.roll = 0.0f;
            for (i = 0; i < this.lookJoints.Num(); i++) {
                jointAng.pitch = this.lookAng.pitch * this.lookJointAngles.oGet(i).pitch;
                jointAng.yaw = this.lookAng.yaw * this.lookJointAngles.oGet(i).yaw;
                this.animator.SetJointAxis(this.lookJoints.oGet(i), JOINTMOD_WORLD, jointAng.ToMat3());
            }
            if (this.move.moveType == MOVETYPE_FLY) {
                // lean into turns
                AdjustFlyingAngles();
            }
            if (headEnt != null) {
                final idAnimator headAnimator = headEnt.GetAnimator();

                if (this.allowEyeFocus) {
                    final idMat3 eyeAxis = (this.lookAng.oPlus(this.eyeAng)).ToMat3();
                    final idMat3 headTranspose = headEnt.GetPhysics().GetAxis().Transpose();
                    axis = eyeAxis.oMultiply(orientationJointAxis);
                    left = axis.oGet(1).oMultiply(this.eyeHorizontalOffset);
                    eyepos.oMinSet(headEnt.GetPhysics().GetOrigin());
                    headAnimator.SetJointPos(this.leftEyeJoint, JOINTMOD_WORLD_OVERRIDE, eyepos.oPlus((axis.oGet(0).oMultiply(64.0f).oPlus(left)).oMultiply(headTranspose)));
                    headAnimator.SetJointPos(this.rightEyeJoint, JOINTMOD_WORLD_OVERRIDE, eyepos.oPlus((axis.oGet(0).oMultiply(64.0f).oMinus(left)).oMultiply(headTranspose)));
                } else {
                    headAnimator.ClearJoint(this.leftEyeJoint);
                    headAnimator.ClearJoint(this.rightEyeJoint);
                }
            } else {
                if (this.allowEyeFocus) {
                    final idMat3 eyeAxis = (this.lookAng.oPlus(this.eyeAng)).ToMat3();
                    axis = eyeAxis.oMultiply(orientationJointAxis);
                    left = axis.oGet(1).oMultiply(this.eyeHorizontalOffset);
                    eyepos.oPluSet(axis.oGet(0).oMultiply(64.0f).oMinus(this.physicsObj.GetOrigin()));
                    this.animator.SetJointPos(this.leftEyeJoint, JOINTMOD_WORLD_OVERRIDE, eyepos.oPlus(left));
                    this.animator.SetJointPos(this.rightEyeJoint, JOINTMOD_WORLD_OVERRIDE, eyepos.oMinus(left));
                } else {
                    this.animator.ClearJoint(this.leftEyeJoint);
                    this.animator.ClearJoint(this.rightEyeJoint);
                }
            }

            return true;
        }

        protected void UpdateParticles() {
            if (((this.thinkFlags & TH_UPDATEPARTICLES) != 0) && !IsHidden()) {
                idVec3 realVector = new idVec3();
                idMat3 realAxis = new idMat3();

                int particlesAlive = 0;
                for (int i = 0; i < this.particles.Num(); i++) {
                    if ((this.particles.oGet(i).particle != null) && (this.particles.oGet(i).time != 0)) {
                        particlesAlive++;
                        if (this.af.IsActive()) {
                            realAxis = getMat3_identity();
                            realVector = GetPhysics().GetOrigin();
                        } else {
                            this.animator.GetJointTransform(this.particles.oGet(i).joint, gameLocal.time, realVector, realAxis);
                            realAxis.oMulSet(this.renderEntity.axis);
                            realVector = this.physicsObj.GetOrigin().oPlus((realVector.oPlus(this.modelOffset)).oMultiply(this.viewAxis.oMultiply(this.physicsObj.GetGravityAxis())));
                        }

                        if (!gameLocal.smokeParticles.EmitSmoke(this.particles.oGet(i).particle, this.particles.oGet(i).time, gameLocal.random.CRandomFloat(), realVector, realAxis)) {
                            if (this.restartParticles) {
                                this.particles.oGet(i).time = gameLocal.time;
                            } else {
                                this.particles.oGet(i).time = 0;
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

            jointNum = this.animator.GetJointHandle(jointName);
            for (int i = 0; i < this.particles.Num(); i++) {
                if (this.particles.oGet(i).joint == jointNum) {
                    this.particles.oGet(i).time = gameLocal.time;
                    BecomeActive(TH_UPDATEPARTICLES);
                }
            }
        }

        // AI script state management
        protected void LinkScriptVariables() {
            this.AI_TALK.LinkTo(this.scriptObject, "AI_TALK");
            this.AI_DAMAGE.LinkTo(this.scriptObject, "AI_DAMAGE");
            this.AI_PAIN.LinkTo(this.scriptObject, "AI_PAIN");
            this.AI_SPECIAL_DAMAGE.LinkTo(this.scriptObject, "AI_SPECIAL_DAMAGE");
            this.AI_DEAD.LinkTo(this.scriptObject, "AI_DEAD");
            this.AI_ENEMY_VISIBLE.LinkTo(this.scriptObject, "AI_ENEMY_VISIBLE");
            this.AI_ENEMY_IN_FOV.LinkTo(this.scriptObject, "AI_ENEMY_IN_FOV");
            this.AI_ENEMY_DEAD.LinkTo(this.scriptObject, "AI_ENEMY_DEAD");
            this.AI_MOVE_DONE.LinkTo(this.scriptObject, "AI_MOVE_DONE");
            this.AI_ONGROUND.LinkTo(this.scriptObject, "AI_ONGROUND");
            this.AI_ACTIVATED.LinkTo(this.scriptObject, "AI_ACTIVATED");
            this.AI_FORWARD.LinkTo(this.scriptObject, "AI_FORWARD");
            this.AI_JUMP.LinkTo(this.scriptObject, "AI_JUMP");
            this.AI_BLOCKED.LinkTo(this.scriptObject, "AI_BLOCKED");
            this.AI_DEST_UNREACHABLE.LinkTo(this.scriptObject, "AI_DEST_UNREACHABLE");
            this.AI_HIT_ENEMY.LinkTo(this.scriptObject, "AI_HIT_ENEMY");
            this.AI_OBSTACLE_IN_PATH.LinkTo(this.scriptObject, "AI_OBSTACLE_IN_PATH");
            this.AI_PUSHED.LinkTo(this.scriptObject, "AI_PUSHED");
        }

        protected void UpdateAIScript() {
            UpdateScript();

            // clear the hit enemy flag so we catch the next time we hit someone
            this.AI_HIT_ENEMY.operator(false);

            if (this.allowHiddenMovement || !IsHidden()) {
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
            final idEntity other = _other.value;
            if ((null == this.enemy.GetEntity()) && !other.fl.notarget && ((ReactionTo(other) & ATTACK_ON_ACTIVATE) != 0)) {
                Activate(other);
            }
            this.AI_PUSHED.operator(true);
        }

        protected void Event_FindEnemy(idEventArg<Integer> useFOV) {
            int i;
            idEntity ent;
            idActor actor;

            if (gameLocal.InPlayerPVS(this)) {
                for (i = 0; i < gameLocal.numClients; i++) {
                    ent = gameLocal.entities[ i];

                    if ((null == ent) || !ent.IsType(idActor.class)) {
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
                if ((actor.health <= 0) || (0 == (ReactionTo(actor) & ATTACK_ON_SIGHT))) {
                    continue;
                }

                if (!gameLocal.pvs.InCurrentPVS(pvs, actor.GetPVSAreas(), actor.GetNumPVSAreas())) {
                    continue;
                }

                delta = this.physicsObj.GetOrigin().oMinus(actor.GetPhysics().GetOrigin());
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

                if ((null == ent) || !ent.IsType(idActor.class)) {
                    continue;
                }

                actor = (idActor) ent;
                if ((actor.health <= 0) || NOT(ReactionTo(actor) & ATTACK_ON_SIGHT)) {
                    continue;
                }

                for (j = 0; j < this.targets.Num(); j++) {
                    targetEnt = this.targets.oGet(j).GetEntity();
                    if ((null == targetEnt) || !targetEnt.IsType(idCombatNode.class)) {
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
            final idEntity team_mate = _team_mate.value;
            idActor actor;
            idActor ent;
            idActor bestEnt;
            float bestDistSquared;
            float distSquared;
            idVec3 delta;
            int areaNum;
            int enemyAreaNum;
            final aasPath_s path = new aasPath_s();

            if (!team_mate.IsType(idActor.class)) {
                idGameLocal.Error("Entity '%s' is not an AI character or player", team_mate.GetName());
            }

            actor = (idActor) team_mate;

            final idVec3 origin = this.physicsObj.GetOrigin();
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
            final idActor actor = gameLocal.GetAlertEntity();
            if ((actor != null) && ((0 == ignore_team.value) || ((ReactionTo(actor) & ATTACK_ON_SIGHT) != 0)) && gameLocal.InPlayerPVS(this)) {
                final idVec3 pos = actor.GetPhysics().GetOrigin();
                final idVec3 org = this.physicsObj.GetOrigin();
                final float dist = (pos.oMinus(org)).LengthSqr();
                if (dist < Square(AI_HEARING_RANGE)) {
                    idThread.ReturnEntity(actor);
                    return;
                }
            }

            idThread.ReturnEntity(null);
        }

        protected void Event_SetEnemy(idEventArg<idEntity> _ent) {
            final idEntity ent = _ent.value;
            if (null == ent) {
                ClearEnemy();
            } else if (!ent.IsType(idActor.class)) {
                idGameLocal.Error("'%s' is not an idActor (player or ai controlled character)", ent.name);
            } else {
                SetEnemy((idActor) ent);
            }
        }

        protected void Event_ClearEnemy() {
            ClearEnemy();
        }

        protected void Event_MuzzleFlash(final idEventArg<String> jointname) {
            final idVec3 muzzle = new idVec3();
            final idMat3 axis = new idMat3();

            GetMuzzle(jointname.value, muzzle, axis);
            TriggerWeaponEffects(muzzle);
        }

        protected void Event_CreateMissile(final idEventArg<String> _jointname) {
            final String jointname = _jointname.value;
            final idVec3 muzzle = new idVec3();
            final idMat3 axis = new idMat3();

            if (null == this.projectileDef) {
                gameLocal.Warning("%s (%s) doesn't have a projectile specified", this.name, GetEntityDefName());
                idThread.ReturnEntity(null);
            }

            GetMuzzle(jointname, muzzle, axis);
            CreateProjectile(muzzle, this.viewAxis.oGet(0).oMultiply(this.physicsObj.GetGravityAxis()));
            if (this.projectile.GetEntity() != null) {
                if (!isNotNullOrEmpty(jointname)) {
                    this.projectile.GetEntity().Bind(this, true);
                } else {
                    this.projectile.GetEntity().BindToJoint(this, jointname, true);
                }
            }
            idThread.ReturnEntity(this.projectile.GetEntity());
        }

        protected void Event_AttackMissile(final idEventArg<String> jointname) {
            idProjectile proj;

            proj = LaunchProjectile(jointname.value, this.enemy.GetEntity(), true);
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
            final trace_s[] tr = {null};
            idBounds projBounds;
            idClipModel projClip;
            idMat3 axis;
            final float[] distance = {0};

            if (null == this.projectileDef) {
                gameLocal.Warning("%s (%s) doesn't have a projectile specified", this.name, GetEntityDefName());
                idThread.ReturnEntity(null);
                return;
            }

            axis = ang.ToMat3();
            if (null == this.projectile.GetEntity()) {
                CreateProjectile(muzzle, axis.oGet(0));
            }

            // make sure the projectile starts inside the monster bounding box
            final idBounds ownerBounds = this.physicsObj.GetAbsBounds();
            projClip = this.projectile.GetEntity().GetPhysics().GetClipModel();
            projBounds = projClip.GetBounds().Rotate(projClip.GetAxis());

            // check if the owner bounds is bigger than the projectile bounds
            if (((ownerBounds.oGet(1, 0) - ownerBounds.oGet(0, 0)) > (projBounds.oGet(1, 0) - projBounds.oGet(0, 0)))
                    && ((ownerBounds.oGet(1, 1) - ownerBounds.oGet(0, 1)) > (projBounds.oGet(1, 1) - projBounds.oGet(0, 1)))
                    && ((ownerBounds.oGet(1, 2) - ownerBounds.oGet(0, 2)) > (projBounds.oGet(1, 2) - projBounds.oGet(0, 2)))) {
                if ((ownerBounds.oMinus(projBounds)).RayIntersection(muzzle, this.viewAxis.oGet(0), distance)) {
                    start = muzzle.oPlus(this.viewAxis.oGet(0).oMultiply(distance[0]));
                } else {
                    start = ownerBounds.GetCenter();
                }
            } else {
                // projectile bounds bigger than the owner bounds, so just start it from the center
                start = ownerBounds.GetCenter();
            }

            gameLocal.clip.Translation(tr, start, muzzle, projClip, projClip.GetAxis(), MASK_SHOT_RENDERMODEL, this);

            // launch the projectile
            idThread.ReturnEntity(this.projectile.GetEntity());
            this.projectile.GetEntity().Launch(tr[0].endpos, axis.oGet(0), getVec3_origin());
            this.projectile.oSet(null);

            TriggerWeaponEffects(tr[0].endpos);

            this.lastAttackTime = gameLocal.time;
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
            final idMat3 axis = new idMat3();

            if (!isNotNullOrEmpty(jointname.value)) {
                org = this.physicsObj.GetOrigin();
            } else {
                joint = this.animator.GetJointHandle(jointname.value);
                if (joint == INVALID_JOINT) {
                    idGameLocal.Error("Unknown joint '%s' on %s", jointname.value, GetEntityDefName());
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
            final idMat3 axis = new idMat3();
            final trace_s trace = new trace_s();
            idEntity hitEnt;

            joint = this.animator.GetJointHandle(jointname.value);
            if (joint == INVALID_JOINT) {
                idGameLocal.Error("Unknown joint '%s' on %s", jointname.value, GetEntityDefName());
            }
            this.animator.GetJointTransform(joint, gameLocal.time, end, axis);
            end = this.physicsObj.GetOrigin().oPlus((end.oPlus(this.modelOffset)).oMultiply(this.viewAxis).oMultiply(this.physicsObj.GetGravityAxis()));
            start = GetEyePosition();

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorYellow, start, end, idGameLocal.msec);
            }

            gameLocal.clip.TranslationEntities(trace, start, end, null, getMat3_identity(), MASK_SHOT_BOUNDINGBOX, this);
            if (trace.fraction < 1.0f) {
                hitEnt = gameLocal.GetTraceEntity(trace);
                if ((hitEnt != null) && hitEnt.IsType(idActor.class)) {
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
            final idClipModel[] clipModels = new idClipModel[MAX_GENTITIES];

            num = gameLocal.clip.ClipModelsTouchingBounds(this.physicsObj.GetAbsBounds(), MASK_MONSTERSOLID, clipModels, MAX_GENTITIES);
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

                if (this.physicsObj.ClipContents(cm) != 0) {
                    idThread.ReturnFloat(0);//(false);
                    return;
                }
            }

            idThread.ReturnFloat(1);//(true);
        }

        protected void Event_BecomeSolid() {
            this.physicsObj.EnableClip();
            if (this.spawnArgs.GetBool("big_monster")) {
                this.physicsObj.SetContents(0);
            } else if (this.use_combat_bbox) {
                this.physicsObj.SetContents(CONTENTS_BODY | CONTENTS_SOLID);
            } else {
                this.physicsObj.SetContents(CONTENTS_BODY);
            }
            this.physicsObj.GetClipModel().Link(gameLocal.clip);
            this.fl.takedamage = !this.spawnArgs.GetBool("noDamage");
        }

        protected void Event_BecomeNonSolid() {
            this.fl.takedamage = false;
            this.physicsObj.SetContents(0);
            this.physicsObj.GetClipModel().Unlink();
        }

        protected void Event_BecomeRagdoll() {
            int result;

            result = StartRagdoll() ? 1 : 0;
            idThread.ReturnInt(result);
        }

        protected void Event_StopRagdoll() {
            StopRagdoll();

            // set back the monster physics
            SetPhysics(this.physicsObj);
        }

        protected void Event_SetHealth(idEventArg<Float> newHealth) {
            this.health = newHealth.value.intValue();
            this.fl.takedamage = true;
            if (this.health > 0) {
                this.AI_DEAD.operator(false);
            } else {
                this.AI_DEAD.operator(true);
            }
        }

        protected void Event_GetHealth() {
            idThread.ReturnFloat(this.health);
        }

        protected void Event_AllowDamage() {
            this.fl.takedamage = true;
        }

        protected void Event_IgnoreDamage() {
            this.fl.takedamage = false;
        }

        protected void Event_GetCurrentYaw() {
            idThread.ReturnFloat(this.current_yaw);
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
            idThread.ReturnInt(etoi(this.move.moveStatus));
        }

        protected void Event_StopMove() {
            StopMove(MOVE_STATUS_DONE);
        }

        protected void Event_MoveToCover() {
            final idActor enemyEnt = this.enemy.GetEntity();

            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            if ((null == enemyEnt) || !MoveToCover(enemyEnt, this.lastVisibleEnemyPos)) {
                return;
            }
        }

        protected void Event_MoveToEnemy() {
            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            if ((null == this.enemy.GetEntity()) || !MoveToEnemy()) {
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
                idGameLocal.Error("Unknown anim '%s'", attack_anim.value);
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
            final boolean facing = FacingIdeal();
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
            final idActor enemyEnt = this.enemy.GetEntity();

            if (0 == this.targets.Num()) {
                // no combat nodes
                idThread.ReturnEntity(null);
                return;
            }

            if ((null == enemyEnt) || !EnemyPositionValid()) {
                // don't return a combat node if we don't have an enemy or
                // if we can see he's not in the last place we saw him
                idThread.ReturnEntity(null);
                return;
            }

            // find the closest attack node that can see our enemy and is closer than our enemy
            bestNode = null;
            final idVec3 myPos = this.physicsObj.GetOrigin();
            bestDist = (myPos.oMinus(this.lastVisibleEnemyPos)).LengthSqr();
            for (i = 0; i < this.targets.Num(); i++) {
                targetEnt = this.targets.oGet(i).GetEntity();
                if ((null == targetEnt) || !targetEnt.IsType(idCombatNode.class)) {
                    continue;
                }

                node = (idCombatNode) targetEnt;
                if (!node.IsDisabled() && node.EntityInView(enemyEnt, this.lastVisibleEnemyPos)) {
                    final idVec3 org = node.GetPhysics().GetOrigin();
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
            final idEntity ent = _ent.value;
            idCombatNode node;
            boolean result;
            final idActor enemyEnt = this.enemy.GetEntity();

            if (0 == this.targets.Num()) {
                // no combat nodes
                idThread.ReturnInt(false);
                return;
            }

            if (null == enemyEnt) {
                // have to have an enemy
                idThread.ReturnInt(false);
                return;
            }

            if ((null == ent) || !ent.IsType(idCombatNode.class)) {
                // not a combat node
                idThread.ReturnInt(false);
                return;
            }

            node = (idCombatNode) ent;
            if (use_current_enemy_location.value != 0) {
                final idVec3 pos = enemyEnt.GetPhysics().GetOrigin();
                result = node.EntityInView(enemyEnt, pos);
            } else {
                result = node.EntityInView(enemyEnt, this.lastVisibleEnemyPos);
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
            final idVec3 pos = _pos.value;
            final float speed = _speed.value;
            final float max_height = _max_height.value;
            idVec3 start;
            idVec3 end;
            idVec3 dir;
            float dist;
            boolean result;
            final idEntity enemyEnt = this.enemy.GetEntity();

            if (null == enemyEnt) {
                idThread.ReturnVector(getVec3_zero());
                return;
            }

            if (speed <= 0.0f) {
                idGameLocal.Error("Invalid speed.  speed must be > 0.");
            }

            start = this.physicsObj.GetOrigin();
            end = pos;
            dir = end.oMinus(start);
            dist = dir.Normalize();
            if (dist > 16.0f) {
                dist -= 16.0f;
                end.oMinus(dir.oMultiply(16.0f));
            }

            result = PredictTrajectory(start, end, speed, this.physicsObj.GetGravity(), this.physicsObj.GetClipModel(), MASK_MONSTERSOLID, max_height, this, enemyEnt, ai_debugMove.GetBool() ? 4000 : 0, dir);
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

            attack_cone = this.spawnArgs.GetFloat("attack_cone", "70");
            relYaw = idMath.AngleNormalize180(this.ideal_yaw - yaw);
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

            final boolean cansee = CanSee(ent.value, false);
            idThread.ReturnInt(cansee);
        }

        protected void Event_SetTalkTarget(idEventArg<idEntity> _target) {
            final idEntity target = _target.value;
            if ((target != null) && !target.IsType(idActor.class)) {
                idGameLocal.Error("Cannot set talk target to '%s'.  Not a character or player.", target.GetName());
            }
            this.talkTarget.oSet((idActor) target);
            if (target != null) {
                this.AI_TALK.operator(true);
            } else {
                this.AI_TALK.operator(false);
            }
        }

        protected void Event_GetTalkTarget() {
            idThread.ReturnEntity(this.talkTarget.GetEntity());
        }

        protected void Event_SetTalkState(idEventArg<Integer> _state) {
            final int state = _state.value;
            if ((state < 0) || (state >= etoi(NUM_TALK_STATES))) {
                idGameLocal.Error("Invalid talk state (%d)", state);
            }

            this.talk_state = talkState_t.values()[state];
        }

        protected void Event_EnemyRange() {
            float dist;
            final idActor enemyEnt = this.enemy.GetEntity();

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
            final idActor enemyEnt = this.enemy.GetEntity();

            if (enemyEnt != null) {
                dist = (enemyEnt.GetPhysics().GetOrigin().ToVec2().oMinus(GetPhysics().GetOrigin().ToVec2())).Length();
            } else {
                // Just some really high number
                dist = idMath.INFINITY;
            }

            idThread.ReturnFloat(dist);
        }

        protected void Event_GetEnemy() {
            idThread.ReturnEntity(this.enemy.GetEntity());
        }

        protected void Event_GetEnemyPos() {
            idThread.ReturnVector(this.lastVisibleEnemyPos);
        }

        protected void Event_GetEnemyEyePos() {
            idThread.ReturnVector(this.lastVisibleEnemyPos.oPlus(this.lastVisibleEnemyEyeOffset));
        }

        protected void Event_PredictEnemyPos(idEventArg<Float> time) {
            final predictedPath_s path = new predictedPath_s();
            final idActor enemyEnt = this.enemy.GetEntity();

            // if no enemy set
            if (null == enemyEnt) {
                idThread.ReturnVector(this.physicsObj.GetOrigin());
                return;
            }

            // predict the enemy movement
            idAI.PredictPath(enemyEnt, this.aas, this.lastVisibleEnemyPos, enemyEnt.GetPhysics().GetLinearVelocity(),
                    (int) SEC2MS(time.value), (int) SEC2MS(time.value),
                    (this.move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            idThread.ReturnVector(path.endPos);
        }

        protected void Event_CanHitEnemy() {
            final trace_s[] tr = {null};
            idEntity hit;

            final idActor enemyEnt = this.enemy.GetEntity();
            if (!this.AI_ENEMY_VISIBLE.operator() || NOT(enemyEnt)) {
                idThread.ReturnInt(false);
                return;
            }

            // don't check twice per frame
            if (gameLocal.time == this.lastHitCheckTime) {
                idThread.ReturnInt(this.lastHitCheckResult);
                return;
            }

            this.lastHitCheckTime = gameLocal.time;

            idVec3 toPos = enemyEnt.GetEyePosition();
            final idVec3 eye = GetEyePosition();
            idVec3 dir;

            // expand the ray out as far as possible so we can detect anything behind the enemy
            dir = toPos.oMinus(eye);
            dir.Normalize();
            toPos = eye.oPlus(dir.oMultiply(MAX_WORLD_SIZE));
            gameLocal.clip.TracePoint(tr, eye, toPos, MASK_SHOT_BOUNDINGBOX, this);
            hit = gameLocal.GetTraceEntity(tr[0]);
            if ((tr[0].fraction >= 1.0f) || (hit.equals(enemyEnt))) {
                this.lastHitCheckResult = true;
            } else if ((tr[0].fraction < 1.0f) && (hit.IsType(idAI.class))
                    && (((idAI) hit).team != this.team)) {
                this.lastHitCheckResult = true;
            } else {
                this.lastHitCheckResult = false;
            }

            idThread.ReturnInt(this.lastHitCheckResult);
        }

        protected void Event_CanHitEnemyFromAnim(final idEventArg<String> animname) {
            int anim;
            idVec3 dir;
            final idVec3 local_dir = new idVec3();
            idVec3 fromPos;
            idMat3 axis;
            idVec3 start;
            final trace_s[] tr = {null};
            final float[] distance = {0};

            final idActor enemyEnt = this.enemy.GetEntity();
            if (!this.AI_ENEMY_VISIBLE.operator() || NOT(enemyEnt)) {
                idThread.ReturnInt(false);
                return;
            }

            anim = GetAnim(ANIMCHANNEL_LEGS, animname.value);
            if (0 == anim) {
                idThread.ReturnInt(false);
                return;
            }

            // just do a ray test if close enough
            if (enemyEnt.GetPhysics().GetAbsBounds().IntersectsBounds(this.physicsObj.GetAbsBounds().Expand(16.0f))) {
                Event_CanHitEnemy();
                return;
            }

            // calculate the world transform of the launch position
            final idVec3 org = this.physicsObj.GetOrigin();
            dir = this.lastVisibleEnemyPos.oMinus(org);
            this.physicsObj.GetGravityAxis().ProjectVector(dir, local_dir);
            local_dir.z = 0.0f;
            local_dir.ToVec2_Normalize();
            axis = local_dir.ToMat3();
            fromPos = this.physicsObj.GetOrigin().oPlus(this.missileLaunchOffset.oGet(anim).oMultiply(axis));

            if (this.projectileClipModel == null) {
                CreateProjectileClipModel();
            }

            // check if the owner bounds is bigger than the projectile bounds
            final idBounds ownerBounds = this.physicsObj.GetAbsBounds();
            final idBounds projBounds = this.projectileClipModel.GetBounds();
            if (((ownerBounds.oGet(1, 0) - ownerBounds.oGet(0, 0)) > (projBounds.oGet(1, 0) - projBounds.oGet(0, 0)))
                    && ((ownerBounds.oGet(1, 1) - ownerBounds.oGet(0, 1)) > (projBounds.oGet(1, 1) - projBounds.oGet(0, 1)))
                    && ((ownerBounds.oGet(1, 2) - ownerBounds.oGet(0, 2)) > (projBounds.oGet(1, 2) - projBounds.oGet(0, 2)))) {
                if ((ownerBounds.oMinus(projBounds)).RayIntersection(org, this.viewAxis.oGet(0), distance)) {
                    start = org.oPlus(this.viewAxis.oGet(0).oMultiply(distance[0]));
                } else {
                    start = ownerBounds.GetCenter();
                }
            } else {
                // projectile bounds bigger than the owner bounds, so just start it from the center
                start = ownerBounds.GetCenter();
            }

            gameLocal.clip.Translation(tr, start, fromPos, this.projectileClipModel, getMat3_identity(), MASK_SHOT_RENDERMODEL, this);
            fromPos = tr[0].endpos;

            if (GetAimDir(fromPos, this.enemy.GetEntity(), this, dir)) {
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        protected void Event_CanHitEnemyFromJoint(final idEventArg<String> jointname) {
            final trace_s[] tr = {null};
            idVec3 muzzle = new idVec3();
            final idMat3 axis = new idMat3();
            idVec3 start;
            final float[] distance = {0};

            final idActor enemyEnt = this.enemy.GetEntity();
            if (!this.AI_ENEMY_VISIBLE.operator() || (null == enemyEnt)) {
                idThread.ReturnInt(false);
                return;
            }

            // don't check twice per frame
            if (gameLocal.time == this.lastHitCheckTime) {
                idThread.ReturnInt(this.lastHitCheckResult);
                return;
            }

            this.lastHitCheckTime = gameLocal.time;

            final idVec3 org = this.physicsObj.GetOrigin();
            final idVec3 toPos = enemyEnt.GetEyePosition();
            final int/*jointHandle_t*/ joint = this.animator.GetJointHandle(jointname.value);
            if (joint == INVALID_JOINT) {
                idGameLocal.Error("Unknown joint '%s' on %s", jointname.value, GetEntityDefName());
            }
            this.animator.GetJointTransform(joint, gameLocal.time, muzzle, axis);
            muzzle = org.oPlus((muzzle.oPlus(this.modelOffset)).oMultiply(this.viewAxis).oMultiply(this.physicsObj.GetGravityAxis()));

            if (this.projectileClipModel == null) {
                CreateProjectileClipModel();
            }

            // check if the owner bounds is bigger than the projectile bounds
            final idBounds ownerBounds = this.physicsObj.GetAbsBounds();
            final idBounds projBounds = this.projectileClipModel.GetBounds();
            if (((ownerBounds.oGet(1, 0) - ownerBounds.oGet(0, 0)) > (projBounds.oGet(1, 0) - projBounds.oGet(0, 0)))
                    && ((ownerBounds.oGet(1, 1) - ownerBounds.oGet(0, 1)) > (projBounds.oGet(1, 1) - projBounds.oGet(0, 1)))
                    && ((ownerBounds.oGet(1, 2) - ownerBounds.oGet(0, 2)) > (projBounds.oGet(1, 2) - projBounds.oGet(0, 2)))) {
                if ((ownerBounds.oMinus(projBounds)).RayIntersection(org, this.viewAxis.oGet(0), distance)) {
                    start = org.oPlus(this.viewAxis.oGet(0).oMultiply(distance[0]));
                } else {
                    start = ownerBounds.GetCenter();
                }
            } else {
                // projectile bounds bigger than the owner bounds, so just start it from the center
                start = ownerBounds.GetCenter();
            }

            gameLocal.clip.Translation(tr, start, muzzle, this.projectileClipModel, getMat3_identity(), MASK_SHOT_BOUNDINGBOX, this);
            muzzle = tr[0].endpos;

            gameLocal.clip.Translation(tr, muzzle, toPos, this.projectileClipModel, getMat3_identity(), MASK_SHOT_BOUNDINGBOX, this);
            if ((tr[0].fraction >= 1.0f) || (gameLocal.GetTraceEntity(tr[0]).equals(enemyEnt))) {
                this.lastHitCheckResult = true;
            } else {
                this.lastHitCheckResult = false;
            }

            idThread.ReturnInt(this.lastHitCheckResult);
        }

        protected void Event_EnemyPositionValid() {
            int result;

            result = EnemyPositionValid() ? 1 : 0;
            idThread.ReturnInt(result);
        }

        protected void Event_ChargeAttack(final idEventArg<String> damageDef) {
            final idActor enemyEnt = this.enemy.GetEntity();

            StopMove(MOVE_STATUS_DEST_NOT_FOUND);
            if (enemyEnt != null) {
                idVec3 enemyOrg;

                if (this.move.moveType == MOVETYPE_FLY) {
                    // position destination so that we're in the enemy's view
                    enemyOrg = enemyEnt.GetEyePosition();
                    enemyOrg.oMinSet(enemyEnt.GetPhysics().GetGravityNormal().oMultiply(this.fly_offset));
                } else {
                    enemyOrg = enemyEnt.GetPhysics().GetOrigin();
                }

                BeginAttack(damageDef.value);
                DirectMoveToPosition(enemyOrg);
                TurnToward(enemyOrg);
            }
        }

        protected void Event_TestChargeAttack() {
            final trace_s trace = new trace_s();
            final idActor enemyEnt = this.enemy.GetEntity();
            final predictedPath_s path = new predictedPath_s();
            idVec3 end;

            if (null == enemyEnt) {
                idThread.ReturnFloat(0.0f);
                return;
            }

            if (this.move.moveType == MOVETYPE_FLY) {
                // position destination so that we're in the enemy's view
                end = enemyEnt.GetEyePosition();
                end.oMinSet(enemyEnt.GetPhysics().GetGravityNormal().oMultiply(this.fly_offset));
            } else {
                end = enemyEnt.GetPhysics().GetOrigin();
            }

            idAI.PredictPath(this, this.aas, this.physicsObj.GetOrigin(), end.oMinus(this.physicsObj.GetOrigin()), 1000, 1000, (this.move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorGreen, this.physicsObj.GetOrigin(), end, idGameLocal.msec);
                gameRenderWorld.DebugBounds(path.endEvent == 0 ? colorYellow : colorRed, this.physicsObj.GetBounds(), end, idGameLocal.msec);
            }

            if ((path.endEvent == 0) || (path.blockingEntity.equals(enemyEnt))) {
                final idVec3 delta = end.oMinus(this.physicsObj.GetOrigin());
                final float time = delta.LengthFast();
                idThread.ReturnFloat(time);
            } else {
                idThread.ReturnFloat(0.0f);
            }
        }

        protected void Event_TestAnimMoveTowardEnemy(final idEventArg<String> animname) {
            int anim;
            final predictedPath_s path = new predictedPath_s();
            idVec3 moveVec;
            float yaw;
            idVec3 delta;
            idActor enemyEnt;

            enemyEnt = this.enemy.GetEntity();
            if (null == enemyEnt) {
                idThread.ReturnInt(false);
                return;
            }

            anim = GetAnim(ANIMCHANNEL_LEGS, animname.value);
            if (0 == anim) {
                gameLocal.DWarning("missing '%s' animation on '%s' (%s)", animname.value, this.name, GetEntityDefName());
                idThread.ReturnInt(false);
                return;
            }

            delta = enemyEnt.GetPhysics().GetOrigin().oMinus(this.physicsObj.GetOrigin());
            yaw = delta.ToYaw();

            moveVec = this.animator.TotalMovementDelta(anim).oMultiply(new idAngles(0.0f, yaw, 0.0f).ToMat3().oMultiply(this.physicsObj.GetGravityAxis()));
            idAI.PredictPath(this, this.aas, this.physicsObj.GetOrigin(), moveVec, 1000, 1000, (this.move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorGreen, this.physicsObj.GetOrigin(), this.physicsObj.GetOrigin().oPlus(moveVec), idGameLocal.msec);
                gameRenderWorld.DebugBounds(path.endEvent == 0 ? colorYellow : colorRed, this.physicsObj.GetBounds(), this.physicsObj.GetOrigin().oPlus(moveVec), idGameLocal.msec);
            }

            idThread.ReturnInt(path.endEvent == 0);
        }

        protected void Event_TestAnimMove(final idEventArg<String> animname) {
            int anim;
            final predictedPath_s path = new predictedPath_s();
            idVec3 moveVec;

            anim = GetAnim(ANIMCHANNEL_LEGS, animname.value);
            if (0 == anim) {
                gameLocal.DWarning("missing '%s' animation on '%s' (%s)", animname.value, this.name, GetEntityDefName());
                idThread.ReturnInt(false);
                return;
            }

            moveVec = this.animator.TotalMovementDelta(anim).oMultiply(new idAngles(0.0f, this.ideal_yaw, 0.0f).ToMat3().oMultiply(this.physicsObj.GetGravityAxis()));
            idAI.PredictPath(this, this.aas, this.physicsObj.GetOrigin(), moveVec, 1000, 1000, (this.move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorGreen, this.physicsObj.GetOrigin(), this.physicsObj.GetOrigin().oPlus(moveVec), idGameLocal.msec);
                gameRenderWorld.DebugBounds(path.endEvent == 0 ? colorYellow : colorRed, this.physicsObj.GetBounds(), this.physicsObj.GetOrigin().oPlus(moveVec), idGameLocal.msec);
            }

            idThread.ReturnInt(path.endEvent == 0);
        }

        protected void Event_TestMoveToPosition(final idEventArg<idVec3> _position) {
            final idVec3 position = _position.value;
            final predictedPath_s path = new predictedPath_s();

            idAI.PredictPath(this, this.aas, this.physicsObj.GetOrigin(), position.oMinus(this.physicsObj.GetOrigin()), 1000, 1000, (this.move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            if (ai_debugMove.GetBool()) {
                gameRenderWorld.DebugLine(colorGreen, this.physicsObj.GetOrigin(), position, idGameLocal.msec);
                gameRenderWorld.DebugBounds(colorYellow, this.physicsObj.GetBounds(), position, idGameLocal.msec);
                if (path.endEvent != 0) {
                    gameRenderWorld.DebugBounds(colorRed, this.physicsObj.GetBounds(), path.endPos, idGameLocal.msec);
                }
            }

            idThread.ReturnInt(path.endEvent == 0);
        }

        protected void Event_TestMeleeAttack() {
            final boolean result = TestMelee();
            idThread.ReturnInt(result);
        }

        protected void Event_TestAnimAttack(final idEventArg<String> animname) {
            int anim;
            final predictedPath_s path = new predictedPath_s();

            anim = GetAnim(ANIMCHANNEL_LEGS, animname.value);
            if (0 == anim) {
                gameLocal.DWarning("missing '%s' animation on '%s' (%s)", animname.value, this.name, GetEntityDefName());
                idThread.ReturnInt(false);
                return;
            }

            idAI.PredictPath(this, this.aas, this.physicsObj.GetOrigin(), this.animator.TotalMovementDelta(anim), 1000, 1000, (this.move.moveType == MOVETYPE_FLY) ? SE_BLOCKED : (SE_ENTER_OBSTACLE | SE_BLOCKED | SE_ENTER_LEDGE_AREA), path);

            idThread.ReturnInt((path.blockingEntity != null) && (path.blockingEntity.equals(this.enemy.GetEntity())));
        }

        protected void Event_Shrivel(idEventArg<Float> shrivel_time) {
            float t;

            if (idThread.BeginMultiFrameEvent(this, AI_Shrivel)) {
                if (shrivel_time.value <= 0.0f) {
                    idThread.EndMultiFrameEvent(this, AI_Shrivel);
                    return;
                }

                this.shrivel_rate = 0.001f / shrivel_time.value;
                this.shrivel_start = gameLocal.time;
            }

            t = (gameLocal.time - this.shrivel_start) * this.shrivel_rate;
            if (t > 0.25f) {
                this.renderEntity.noShadow = true;
            }
            if (t > 1.0f) {
                t = 1.0f;
                idThread.EndMultiFrameEvent(this, AI_Shrivel);
            }

            this.renderEntity.shaderParms[SHADERPARM_MD5_SKINSCALE] = 1.0f - (t * 0.5f);
            UpdateVisuals();
        }

        protected void Event_Burn() {
            this.renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = gameLocal.time * 0.001f;
            SpawnParticles("smoke_burnParticleSystem");
            UpdateVisuals();
        }

        protected void Event_PreBurn() {
            // for now this just turns shadows off
            this.renderEntity.noShadow = true;
        }

        protected void Event_ClearBurn() {
            this.renderEntity.noShadow = this.spawnArgs.GetBool("noshadows");
            this.renderEntity.shaderParms[ SHADERPARM_TIME_OF_DEATH] = 0.0f;
            UpdateVisuals();
        }

        protected void Event_SetSmokeVisibility(idEventArg<Integer> _num, idEventArg<Integer> on) {
            final int num = _num.value;
            int i;
            int time;

            if (num >= this.particles.Num()) {
                gameLocal.Warning("Particle #%d out of range (%d particles) on entity '%s'", num, this.particles.Num(), this.name);
                return;
            }

            if (on.value != 0) {
                time = gameLocal.time;
                BecomeActive(TH_UPDATEPARTICLES);
            } else {
                time = 0;
            }

            if (num >= 0) {
                this.particles.oGet(num).time = time;
            } else {
                for (i = 0; i < this.particles.Num(); i++) {
                    this.particles.oGet(i).time = time;
                }
            }

            UpdateVisuals();
        }

        protected void Event_NumSmokeEmitters() {
            idThread.ReturnInt(this.particles.Num());
        }

        protected void Event_StopThinking() {
            BecomeInactive(TH_THINK);
            final idThread thread = idThread.CurrentThread();
            if (thread != null) {
                thread.DoneProcessing();
            }
        }

        protected void Event_GetTurnDelta() {
            float amount;

            if (this.turnRate != 0) {
                amount = idMath.AngleNormalize180(this.ideal_yaw - this.current_yaw);
                idThread.ReturnFloat(amount);
            } else {
                idThread.ReturnFloat(0.0f);
            }
        }

        protected void Event_GetMoveType() {
            idThread.ReturnInt(etoi(this.move.moveType));
        }

        protected void Event_SetMoveType(idEventArg<Integer> _moveType) {
            final int moveType = _moveType.value;
            if ((moveType < 0) || (moveType >= etoi(NUM_MOVETYPES))) {
                idGameLocal.Error("Invalid movetype %d", moveType);
            }

            this.move.moveType = moveType_t.values()[moveType];
            if (this.move.moveType == MOVETYPE_FLY) {
                this.travelFlags = TFL_WALK | TFL_AIR | TFL_FLY;
            } else {
                this.travelFlags = TFL_WALK | TFL_AIR;
            }
        }

        protected void Event_SaveMove() {
            this.savedMove = this.move;
        }

        protected void Event_RestoreMove() {
            final idVec3 goalPos = new idVec3();
            final idVec3 dest = new idVec3();

            switch (this.savedMove.moveCommand) {
                case MOVE_NONE:
                    StopMove(this.savedMove.moveStatus);
                    break;

                case MOVE_FACE_ENEMY:
                    FaceEnemy();
                    break;

                case MOVE_FACE_ENTITY:
                    FaceEntity(this.savedMove.goalEntity.GetEntity());
                    break;

                case MOVE_TO_ENEMY:
                    MoveToEnemy();
                    break;

                case MOVE_TO_ENEMYHEIGHT:
                    MoveToEnemyHeight();
                    break;

                case MOVE_TO_ENTITY:
                    MoveToEntity(this.savedMove.goalEntity.GetEntity());
                    break;

                case MOVE_OUT_OF_RANGE:
                    MoveOutOfRange(this.savedMove.goalEntity.GetEntity(), this.savedMove.range);
                    break;

                case MOVE_TO_ATTACK_POSITION:
                    MoveToAttackPosition(this.savedMove.goalEntity.GetEntity(), this.savedMove.anim);
                    break;

                case MOVE_TO_COVER:
                    MoveToCover(this.savedMove.goalEntity.GetEntity(), this.lastVisibleEnemyPos);
                    break;

                case MOVE_TO_POSITION:
                    MoveToPosition(this.savedMove.moveDest);
                    break;

                case MOVE_TO_POSITION_DIRECT:
                    DirectMoveToPosition(this.savedMove.moveDest);
                    break;

                case MOVE_SLIDE_TO_POSITION:
                    SlideToPosition(this.savedMove.moveDest, this.savedMove.duration);
                    break;

                case MOVE_WANDER:
                    WanderAround();
                    break;
			default:
				// TODO check unused Enum case labels
				break;
            }

            if (GetMovePos(goalPos)) {
                CheckObstacleAvoidance(goalPos, dest);
            }
        }

        protected void Event_AllowMovement(idEventArg<Float> flag) {
            this.allowMove = (flag.value != 0.0f);
        }

        protected void Event_JumpFrame() {
            this.AI_JUMP.operator(true);
        }

        protected void Event_EnableClip() {
            this.physicsObj.SetClipMask(MASK_MONSTERSOLID);
            this.disableGravity = false;
        }

        protected void Event_DisableClip() {
            this.physicsObj.SetClipMask(0);
            this.disableGravity = true;
        }

        protected void Event_EnableGravity() {
            this.disableGravity = false;
        }

        protected void Event_DisableGravity() {
            this.disableGravity = true;
        }

        protected void Event_EnableAFPush() {
            this.af_push_moveables = true;
        }

        protected void Event_DisableAFPush() {
            this.af_push_moveables = false;
        }

        protected void Event_SetFlySpeed(idEventArg<Float> speed) {
            if (this.move.speed == this.fly_speed) {
                this.move.speed = speed.value;
            }
            this.fly_speed = speed.value;
        }

        protected void Event_SetFlyOffset(idEventArg<Integer> offset) {
            this.fly_offset = offset.value;
        }

        protected void Event_ClearFlyOffset() {
            this.fly_offset = this.spawnArgs.GetInt("fly_offset", "0");
        }

        protected void Event_GetClosestHiddenTarget(final idEventArg<String> type) {
            int i;
            idEntity ent;
            idEntity bestEnt;
            float time;
            float bestTime;
            final idVec3 org = this.physicsObj.GetOrigin();
            final idActor enemyEnt = this.enemy.GetEntity();

            if (null == enemyEnt) {
                // no enemy to hide from
                idThread.ReturnEntity(null);
                return;
            }

            if (this.targets.Num() == 1) {
                ent = this.targets.oGet(0).GetEntity();
                if ((ent != null) && (idStr.Cmp(ent.GetEntityDefName(), type.value) == 0)) {
                    if (!EntityCanSeePos(enemyEnt, this.lastVisibleEnemyPos, ent.GetPhysics().GetOrigin())) {
                        idThread.ReturnEntity(ent);
                        return;
                    }
                }
                idThread.ReturnEntity(null);
                return;
            }

            bestEnt = null;
            bestTime = idMath.INFINITY;
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if ((ent != null) && (idStr.Cmp(ent.GetEntityDefName(), type.value) == 0)) {
                    final idVec3 destOrg = ent.GetPhysics().GetOrigin();
                    time = TravelDistance(org, destOrg);
                    if ((time >= 0.0f) && (time < bestTime)) {
                        if (!EntityCanSeePos(enemyEnt, this.lastVisibleEnemyPos, destOrg)) {
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
            final idEntity[] ents = new idEntity[MAX_GENTITIES];

            num = 0;
            for (i = 0; i < this.targets.Num(); i++) {
                ent = this.targets.oGet(i).GetEntity();
                if ((ent != null) && (idStr.Cmp(ent.GetEntityDefName(), type.value) == 0)) {
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

            time = TravelDistance(this.physicsObj.GetOrigin(), pos.value);
            idThread.ReturnFloat(time);
        }

        protected void Event_TravelDistanceToEntity(idEventArg<idEntity> ent) {
            float time;

            time = TravelDistance(this.physicsObj.GetOrigin(), ent.value.GetPhysics().GetOrigin());
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
            if (ent == this) {
                ent = null;
            } else if ((ent != this.focusEntity.GetEntity()) || (this.focusTime < gameLocal.time)) {
                this.focusEntity.oSet(ent);
                this.alignHeadTime = gameLocal.time;
                this.forceAlignHeadTime = (int) (gameLocal.time + SEC2MS(1));
                this.blink_time = 0;
            }

            this.focusTime = (int) (gameLocal.time + SEC2MS(duration.value));
        }

        protected void Event_LookAtEnemy(idEventArg<Float> duration) {
            idActor enemyEnt;

            enemyEnt = this.enemy.GetEntity();
            if ((!enemyEnt.equals(this.focusEntity.GetEntity())) || (this.focusTime < gameLocal.time)) {
                this.focusEntity.oSet(enemyEnt);
                this.alignHeadTime = gameLocal.time;
                this.forceAlignHeadTime = (int) (gameLocal.time + SEC2MS(1));
                this.blink_time = 0;
            }

            this.focusTime = (int) (gameLocal.time + SEC2MS(duration.value));
        }

        protected void Event_SetJointMod(idEventArg<Integer> allow) {
            this.allowJointMod = itob(allow.value);
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
            this.current_yaw = ang.value.yaw;
            this.viewAxis = new idAngles(0, this.current_yaw, 0).ToMat3();
        }

        protected void Event_GetAngles() {
            idThread.ReturnVector(new idVec3(0.0f, this.current_yaw, 0.0f));
        }

        protected void Event_RealKill() {
            this.health = 0;

            if (this.af.IsLoaded()) {
                // clear impacts
                this.af.Rest();

                // physics is turned off by calling af.Rest()
                BecomeActive(TH_PHYSICS);
            }

            Killed(this, this, 0, getVec3_zero(), INVALID_JOINT);
        }

        protected void Event_Kill() {
            PostEventMS(AI_RealKill, 0);
        }

        protected void Event_WakeOnFlashlight(idEventArg<Integer> enable) {
            this.wakeOnFlashlight = (enable.value != 0);
        }

        protected void Event_LocateEnemy() {
            idActor enemyEnt;
            final int[] areaNum = {0};

            enemyEnt = this.enemy.GetEntity();
            if (null == enemyEnt) {
                return;
            }

            enemyEnt.GetAASLocation(this.aas, this.lastReachableEnemyPos, areaNum);
            SetEnemyPosition();
            UpdateEnemyPosition();
        }

        protected void Event_KickObstacles(idEventArg<idEntity> kickEnt, idEventArg<Float> force) {
            idVec3 dir;
            idEntity obEnt;

            if (kickEnt.value != null) {
                obEnt = kickEnt.value;
            } else {
                obEnt = this.move.obstacle.GetEntity();
            }

            if (obEnt != null) {
                dir = obEnt.GetPhysics().GetOrigin().oMinus(this.physicsObj.GetOrigin());
                dir.Normalize();
            } else {
                dir = this.viewAxis.oGet(0);
            }
            KickObstacles(dir, force.value, obEnt);
        }

        protected void Event_GetObstacle() {
            idThread.ReturnEntity(this.move.obstacle.GetEntity());
        }

        protected void Event_PushPointIntoAAS(final idEventArg<idVec3> _pos) {
            final idVec3 pos = _pos.value;
            int areaNum;
            idVec3 newPos;

            areaNum = PointReachableAreaNum(pos);
            if (areaNum != 0) {
                newPos = pos;
                this.aas.PushPointIntoAreaNum(areaNum, newPos);
                idThread.ReturnVector(newPos);
            } else {
                idThread.ReturnVector(pos);
            }
        }

        protected void Event_GetTurnRate() {
            idThread.ReturnFloat(this.turnRate);
        }

        protected void Event_SetTurnRate(idEventArg<Float> rate) {
            this.turnRate = rate.value;
        }

        protected void Event_AnimTurn(idEventArg<Float> angles) {
            this.turnVel = 0.0f;
            this.anim_turn_angles = angles.value;
            if (angles.value != 0) {
                this.anim_turn_yaw = this.current_yaw;
                this.anim_turn_amount = idMath.Fabs(idMath.AngleNormalize180(this.current_yaw - this.ideal_yaw));
                if (this.anim_turn_amount > this.anim_turn_angles) {
                    this.anim_turn_amount = this.anim_turn_angles;
                }
            } else {
                this.anim_turn_amount = 0.0f;
                this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(0, 1.0f);
                this.animator.CurrentAnim(ANIMCHANNEL_LEGS).SetSyncedAnimWeight(1, 0.0f);
                this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(0, 1.0f);
                this.animator.CurrentAnim(ANIMCHANNEL_TORSO).SetSyncedAnimWeight(1, 0.0f);
            }
        }

        protected void Event_AllowHiddenMovement(idEventArg<Integer> enable) {
            this.allowHiddenMovement = (enable.value != 0);
        }

        protected void Event_TriggerParticles(final idEventArg<String> jointName) {
            TriggerParticles(jointName.value);
        }

        protected void Event_FindActorsInBounds(final idEventArg<idVec3> mins, final idEventArg<idVec3> maxs) {
            idEntity ent;
            final idEntity[] entityList = new idEntity[MAX_GENTITIES];
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
            final aasPath_s path = new aasPath_s();
            int toAreaNum;
            int areaNum;

            toAreaNum = PointReachableAreaNum(pos.value);
            areaNum = PointReachableAreaNum(this.physicsObj.GetOrigin());
            if ((0 == toAreaNum) || !PathToGoal(path, areaNum, this.physicsObj.GetOrigin(), toAreaNum, pos.value)) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
            }
        }

        protected void Event_CanReachEntity(idEventArg<idEntity> _ent) {
            final idEntity ent = _ent.value;
            final aasPath_s path = new aasPath_s();
            int toAreaNum;
            int areaNum;
            idVec3 pos = new idVec3();

            if (null == ent) {
                idThread.ReturnInt(false);
                return;
            }

            if (this.move.moveType != MOVETYPE_FLY) {
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

            final idVec3 org = this.physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);
            if ((0 == toAreaNum) || !PathToGoal(path, areaNum, org, toAreaNum, pos)) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
            }
        }

        protected void Event_CanReachEnemy() {
            final aasPath_s path = new aasPath_s();
            final int[] toAreaNum = {0};
            int areaNum;
            idVec3 pos = new idVec3();
            idActor enemyEnt;

            enemyEnt = this.enemy.GetEntity();
            if (null == enemyEnt) {
                idThread.ReturnInt(false);
                return;
            }

            if (this.move.moveType != MOVETYPE_FLY) {
                if (enemyEnt.OnLadder()) {
                    idThread.ReturnInt(false);
                    return;
                }
                enemyEnt.GetAASLocation(this.aas, pos, toAreaNum);
            } else {
                pos = enemyEnt.GetPhysics().GetOrigin();
                toAreaNum[0] = PointReachableAreaNum(pos);
            }

            if (0 == toAreaNum[0]) {
                idThread.ReturnInt(false);
                return;
            }

            final idVec3 org = this.physicsObj.GetOrigin();
            areaNum = PointReachableAreaNum(org);
            if (!PathToGoal(path, areaNum, org, toAreaNum[0], pos)) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
            }
        }

        protected void Event_GetReachableEntityPosition(idEventArg<idEntity> _ent) {
            final idEntity ent = _ent.value;
            int toAreaNum;
            idVec3 pos = new idVec3();

            if (this.move.moveType != MOVETYPE_FLY) {
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

            if (this.aas != null) {
                toAreaNum = PointReachableAreaNum(pos);
                this.aas.PushPointIntoAreaNum(toAreaNum, pos);
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

        @Override
        protected void _deconstructor() {
            if (this.projectileClipModel != null) {
				idClipModel.delete(this.projectileClipModel);
			}

            DeconstructScriptObject();
            this.scriptObject.Free();
            if (this.worldMuzzleFlashHandle != -1) {
                gameRenderWorld.FreeLightDef(this.worldMuzzleFlashHandle);
                this.worldMuzzleFlashHandle = -1;
            }

            super._deconstructor();
        }
    }

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
            this.min_dist = 0.0f;
            this.max_dist = 0.0f;
            this.cone_dist = 0.0f;
            this.min_height = 0.0f;
            this.max_height = 0.0f;
            this.cone_left = new idVec3();
            this.cone_right = new idVec3();
            this.offset = new idVec3();
            this.disabled = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.min_dist);
            savefile.WriteFloat(this.max_dist);
            savefile.WriteFloat(this.cone_dist);
            savefile.WriteFloat(this.min_height);
            savefile.WriteFloat(this.max_height);
            savefile.WriteVec3(this.cone_left);
            savefile.WriteVec3(this.cone_right);
            savefile.WriteVec3(this.offset);
            savefile.WriteBool(this.disabled);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.min_dist = savefile.ReadFloat();
            this.max_dist = savefile.ReadFloat();
            this.cone_dist = savefile.ReadFloat();
            this.min_height = savefile.ReadFloat();
            this.max_height = savefile.ReadFloat();
            savefile.ReadVec3(this.cone_left);
            savefile.ReadVec3(this.cone_right);
            savefile.ReadVec3(this.offset);
            this.disabled = savefile.ReadBool();
        }

        @Override
        public void Spawn() {
            super.Spawn();

            float fov;
            float yaw;
            float height;

            this.min_dist = this.spawnArgs.GetFloat("min");
            this.max_dist = this.spawnArgs.GetFloat("max");
            height = this.spawnArgs.GetFloat("height");
            fov = this.spawnArgs.GetFloat("fov", "60");
            this.offset = this.spawnArgs.GetVector("offset");

            final idVec3 org = GetPhysics().GetOrigin().oPlus(this.offset);
            this.min_height = org.z - (height * 0.5f);
            this.max_height = this.min_height + height;

            final idMat3 axis = GetPhysics().GetAxis();
            yaw = axis.oGet(0).ToYaw();

            final idAngles leftang = new idAngles(0.0f, (yaw + (fov * 0.5f)) - 90.0f, 0.0f);
            this.cone_left = leftang.ToForward();

            final idAngles rightang = new idAngles(0.0f, (yaw - (fov * 0.5f)) + 90.0f, 0.0f);
            this.cone_right = rightang.ToForward();

            this.disabled = this.spawnArgs.GetBool("start_off");
        }

        public boolean IsDisabled() {
            return this.disabled;
        }

        public boolean EntityInView(idActor actor, final idVec3 pos) {
            if ((null == actor) || (actor.health <= 0)) {
                return false;
            }

            final idBounds bounds = actor.GetPhysics().GetBounds();
            if (((pos.z + bounds.oGet(1).z) < this.min_height) || ((pos.z + bounds.oGet(0).z) >= this.max_height)) {
                return false;
            }

            final idVec3 org = GetPhysics().GetOrigin().oPlus(this.offset);
            final idMat3 axis = GetPhysics().GetAxis();
            final idVec3 dir = pos.oMinus(org);
            final float dist = dir.oMultiply(axis.oGet(0));

            if ((dist < this.min_dist) || (dist > this.max_dist)) {
                return false;
            }

            final float left_dot = dir.oMultiply(this.cone_left);
            if (left_dot < 0.0f) {
                return false;
            }

            final float right_dot = dir.oMultiply(this.cone_right);
            if (right_dot < 0.0f) {
                return false;
            }

            return true;
        }

        public static void DrawDebugInfo() {
            idEntity ent;
            idCombatNode node;
            final idPlayer player = gameLocal.GetLocalPlayer();
            idVec4 color;
            final idBounds bounds = new idBounds(new idVec3(-16, -16, 0), new idVec3(16, 16, 0));

            for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                if (!ent.IsType(idCombatNode.class)) {
                    continue;
                }

                node = (idCombatNode) ent;
                if (node.disabled) {
                    color = colorMdGrey;
                } else if ((player != null) && node.EntityInView(player, player.GetPhysics().GetOrigin())) {
                    color = colorYellow;
                } else {
                    color = colorRed;
                }

                final idVec3 leftDir = new idVec3(-node.cone_left.y, node.cone_left.x, 0.0f);
                final idVec3 rightDir = new idVec3(node.cone_right.y, -node.cone_right.x, 0.0f);
                final idVec3 org = node.GetPhysics().GetOrigin().oPlus(node.offset);

                bounds.oGet(1).z = node.max_height;

                leftDir.NormalizeFast();
                rightDir.NormalizeFast();

                final idMat3 axis = node.GetPhysics().GetAxis();
                final float cone_dot = node.cone_right.oMultiply(axis.oGet(1));
                if (idMath.Fabs(cone_dot) > 0.1) {
                    final float cone_dist = node.max_dist / cone_dot;
                    final idVec3 pos1 = org.oPlus(leftDir.oMultiply(node.min_dist));
                    final idVec3 pos2 = org.oPlus(leftDir.oMultiply(cone_dist));
                    final idVec3 pos3 = org.oPlus(rightDir.oMultiply(node.min_dist));
                    final idVec3 pos4 = org.oPlus(rightDir.oMultiply(cone_dist));

                    gameRenderWorld.DebugLine(color, node.GetPhysics().GetOrigin(), (pos1.oPlus(pos3)).oMultiply(0.5f), idGameLocal.msec);
                    gameRenderWorld.DebugLine(color, pos1, pos2, idGameLocal.msec);
                    gameRenderWorld.DebugLine(color, pos1, pos3, idGameLocal.msec);
                    gameRenderWorld.DebugLine(color, pos3, pos4, idGameLocal.msec);
                    gameRenderWorld.DebugLine(color, pos2, pos4, idGameLocal.msec);
                    gameRenderWorld.DebugBounds(color, bounds, org, idGameLocal.msec);
                }
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            this.disabled = !this.disabled;
        }

        private void Event_MarkUsed() {
            if (this.spawnArgs.GetBool("use_once")) {
                this.disabled = true;
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
        seekVel = goalDelta.oMultiply(MS2SEC(idGameLocal.msec));

        return seekVel;
    }
}
