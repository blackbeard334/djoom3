package neo.Game;

import neo.CM.CollisionModel.trace_s;
import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import neo.Game.Entity.idEntity;
import static neo.Game.Entity.signalNum_t.SIG_MOVER_1TO2;
import static neo.Game.Entity.signalNum_t.SIG_MOVER_2TO1;
import static neo.Game.Entity.signalNum_t.SIG_MOVER_POS1;
import static neo.Game.Entity.signalNum_t.SIG_MOVER_POS2;

import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.eventCallback_t2;
import neo.Game.GameSys.Class.eventCallback_t3;
import neo.Game.GameSys.Class.eventCallback_t5;
import neo.Game.GameSys.Class.idClass;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.g_debugMover;
import static neo.Game.GameSys.SysCvar.g_gravity;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY2;
import static neo.Game.Game_local.gameState_t.GAMESTATE_STARTUP;
import neo.Game.Game_local.idEntityPtr;
import static neo.Game.Mover.idElevator.elevatorState_t.IDLE;
import static neo.Game.Mover.idElevator.elevatorState_t.INIT;
import static neo.Game.Mover.idElevator.elevatorState_t.WAITING_ON_DOORS;
import static neo.Game.Mover.idMover.moveStage_t.ACCELERATION_STAGE;
import static neo.Game.Mover.idMover.moveStage_t.DECELERATION_STAGE;
import static neo.Game.Mover.idMover.moveStage_t.FINISHED_STAGE;
import static neo.Game.Mover.idMover.moveStage_t.LINEAR_STAGE;
import static neo.Game.Mover.idMover.moverCommand_t.MOVER_MOVING;
import static neo.Game.Mover.idMover.moverCommand_t.MOVER_NONE;
import static neo.Game.Mover.idMover.moverCommand_t.MOVER_ROTATING;
import static neo.Game.Mover.idMover.moverCommand_t.MOVER_SPLINE;
import static neo.Game.Mover.moverState_t.MOVER_1TO2;
import static neo.Game.Mover.moverState_t.MOVER_2TO1;
import static neo.Game.Mover.moverState_t.MOVER_POS1;
import static neo.Game.Mover.moverState_t.MOVER_POS2;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Parametric.idPhysics_Parametric;
import neo.Game.Player.idPlayer;
import neo.Game.Script.Script_Thread.idThread;

import static neo.Game.Player.EV_SpectatorTouch;
import static neo.Game.Script.Script_Thread.EV_Thread_SetCallback;
import static neo.Game.Sound.SSF_NO_OCCLUSION;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Material.CONTENTS_TRIGGER;
import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.Renderer.RenderWorld.SHADERPARM_MODE;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_ALL;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_NONE;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_CLUSTERPORTAL;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_OBSTACLE;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idKeyValue;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import static neo.idlib.math.Angles.getAng_zero;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Curve.idCurve_Spline;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_ACCELLINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_DECELLINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_DECELSINE;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_LINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NONE;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NOSTOP;
import static neo.idlib.math.Math_h.SEC2MS;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import neo.idlib.math.Vector.idVec3;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Mover {

    /*
     ===============================================================================

     General movers.

     ===============================================================================
     */
// a mover will update any gui entities in it's target list with 
// a key/val pair of "mover" "state" from below.. guis can represent
// realtime info like this
// binary only
    public static final String[] guiBinaryMoverStates = {
        "1", // pos 1
        "2", // pos 2
        "3", // moving 1 to 2
        "4" // moving 2 to 1
    };
    public static final idEventDef EV_FindGuiTargets            = new idEventDef("<FindGuiTargets>", null);
    public static final idEventDef EV_TeamBlocked               = new idEventDef("<teamblocked>", "ee");
    public static final idEventDef EV_PartBlocked               = new idEventDef("<partblocked>", "e");
    public static final idEventDef EV_ReachedPos                = new idEventDef("<reachedpos>", null);
    public static final idEventDef EV_ReachedAng                = new idEventDef("<reachedang>", null);
    public static final idEventDef EV_PostRestore               = new idEventDef("<postrestore>", "ddddd");
    public static final idEventDef EV_StopMoving                = new idEventDef("stopMoving", null);
    public static final idEventDef EV_StopRotating              = new idEventDef("stopRotating", null);
    public static final idEventDef EV_Speed                     = new idEventDef("speed", "f");
    public static final idEventDef EV_Time                      = new idEventDef("time", "f");
    public static final idEventDef EV_AccelTime                 = new idEventDef("accelTime", "f");
    public static final idEventDef EV_DecelTime                 = new idEventDef("decelTime", "f");
    public static final idEventDef EV_MoveTo                    = new idEventDef("moveTo", "e");
    public static final idEventDef EV_MoveToPos                 = new idEventDef("moveToPos", "v");
    public static final idEventDef EV_Move                      = new idEventDef("move", "ff");
    public static final idEventDef EV_MoveAccelerateTo          = new idEventDef("accelTo", "ff");
    public static final idEventDef EV_MoveDecelerateTo          = new idEventDef("decelTo", "ff");
    public static final idEventDef EV_RotateDownTo              = new idEventDef("rotateDownTo", "df");
    public static final idEventDef EV_RotateUpTo                = new idEventDef("rotateUpTo", "df");
    public static final idEventDef EV_RotateTo                  = new idEventDef("rotateTo", "v");
    public static final idEventDef EV_Rotate                    = new idEventDef("rotate", "v");
    public static final idEventDef EV_RotateOnce                = new idEventDef("rotateOnce", "v");
    public static final idEventDef EV_Bob                       = new idEventDef("bob", "ffv");
    public static final idEventDef EV_Sway                      = new idEventDef("sway", "ffv");
    public static final idEventDef EV_Mover_OpenPortal          = new idEventDef("openPortal");
    public static final idEventDef EV_Mover_ClosePortal         = new idEventDef("closePortal");
    public static final idEventDef EV_AccelSound                = new idEventDef("accelSound", "s");
    public static final idEventDef EV_DecelSound                = new idEventDef("decelSound", "s");
    public static final idEventDef EV_MoveSound                 = new idEventDef("moveSound", "s");
    public static final idEventDef EV_Mover_InitGuiTargets      = new idEventDef("<initguitargets>", null);
    public static final idEventDef EV_EnableSplineAngles        = new idEventDef("enableSplineAngles", null);
    public static final idEventDef EV_DisableSplineAngles       = new idEventDef("disableSplineAngles", null);
    public static final idEventDef EV_RemoveInitialSplineAngles = new idEventDef("removeInitialSplineAngles", null);
    public static final idEventDef EV_StartSpline               = new idEventDef("startSpline", "e");
    public static final idEventDef EV_StopSpline                = new idEventDef("stopSpline", null);
    public static final idEventDef EV_IsMoving                  = new idEventDef("isMoving", null, 'd');
    public static final idEventDef EV_IsRotating                = new idEventDef("isRotating", null, 'd');
    //
    public static final idEventDef EV_PostArrival               = new idEventDef("postArrival", null);
    public static final idEventDef EV_GotoFloor                 = new idEventDef("gotoFloor", "d");
    //
    public static final idEventDef EV_Mover_ReturnToPos1        = new idEventDef("<returntopos1>", null);
    public static final idEventDef EV_Mover_MatchTeam           = new idEventDef("<matchteam>", "dd");
    public static final idEventDef EV_Mover_Enable              = new idEventDef("enable", null);
    public static final idEventDef EV_Mover_Disable             = new idEventDef("disable", null);
    //    
    public static final idEventDef EV_Door_StartOpen            = new idEventDef("<startOpen>", null);
    public static final idEventDef EV_Door_SpawnDoorTrigger     = new idEventDef("<spawnDoorTrigger>", null);
    public static final idEventDef EV_Door_SpawnSoundTrigger    = new idEventDef("<spawnSoundTrigger>", null);
    public static final idEventDef EV_Door_Open                 = new idEventDef("open", null);
    public static final idEventDef EV_Door_Close                = new idEventDef("close", null);
    public static final idEventDef EV_Door_Lock                 = new idEventDef("lock", "d");
    public static final idEventDef EV_Door_IsOpen               = new idEventDef("isOpen", null, 'f');
    public static final idEventDef EV_Door_IsLocked             = new idEventDef("isLocked", null, 'f');

    /*
     ===============================================================================

     idMover

     ===============================================================================
     */
    public static class idMover extends idEntity {
        // CLASS_PROTOTYPE( idMover );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static{
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_FindGuiTargets, (eventCallback_t0<idMover>) idMover::Event_FindGuiTargets);
            eventCallbacks.put(EV_Thread_SetCallback, (eventCallback_t0<idMover>) idMover::Event_SetCallback);
            eventCallbacks.put(EV_TeamBlocked, (eventCallback_t2<idMover>) idMover::Event_TeamBlocked);
            eventCallbacks.put(EV_PartBlocked, (eventCallback_t1<idMover>) idMover::Event_PartBlocked);
            eventCallbacks.put(EV_ReachedPos, (eventCallback_t0<idMover>) idMover::Event_UpdateMove);
            eventCallbacks.put(EV_ReachedAng, (eventCallback_t0<idMover>) idMover::Event_UpdateRotation);
            eventCallbacks.put(EV_PostRestore, (eventCallback_t5<idMover>) idMover::Event_PostRestore);
            eventCallbacks.put(EV_StopMoving, (eventCallback_t0<idMover>) idMover::Event_StopMoving);
            eventCallbacks.put(EV_StopRotating, (eventCallback_t0<idMover>) idMover::Event_StopRotating);
            eventCallbacks.put(EV_Speed, (eventCallback_t1<idMover>) idMover::Event_SetMoveSpeed);
            eventCallbacks.put(EV_Time, (eventCallback_t1<idMover>) idMover::Event_SetMoveTime);
            eventCallbacks.put(EV_AccelTime, (eventCallback_t1<idMover>) idMover::Event_SetAccellerationTime);
            eventCallbacks.put(EV_DecelTime, (eventCallback_t1<idMover>) idMover::Event_SetDecelerationTime);
            eventCallbacks.put(EV_MoveTo, (eventCallback_t1<idMover>) idMover::Event_MoveTo);
            eventCallbacks.put(EV_MoveToPos, (eventCallback_t1<idMover>) idMover::Event_MoveToPos);
            eventCallbacks.put(EV_Move, (eventCallback_t2<idMover>) idMover::Event_MoveDir);
            eventCallbacks.put(EV_MoveAccelerateTo, (eventCallback_t2<idMover>) idMover::Event_MoveAccelerateTo);
            eventCallbacks.put(EV_MoveDecelerateTo, (eventCallback_t2<idMover>) idMover::Event_MoveDecelerateTo);
            eventCallbacks.put(EV_RotateDownTo, (eventCallback_t2<idMover>) idMover::Event_RotateDownTo);
            eventCallbacks.put(EV_RotateUpTo, (eventCallback_t2<idMover>) idMover::Event_RotateUpTo);
            eventCallbacks.put(EV_RotateTo, (eventCallback_t1<idMover>) idMover::Event_RotateTo);
            eventCallbacks.put(EV_Rotate, (eventCallback_t1<idMover>) idMover::Event_Rotate);
            eventCallbacks.put(EV_RotateOnce, (eventCallback_t1<idMover>) idMover::Event_RotateOnce);
            eventCallbacks.put(EV_Bob, (eventCallback_t3<idMover>) idMover::Event_Bob);
            eventCallbacks.put(EV_Sway, (eventCallback_t3<idMover>) idMover::Event_Sway);
            eventCallbacks.put(EV_Mover_OpenPortal, (eventCallback_t0<idMover>) idMover::Event_OpenPortal);
            eventCallbacks.put(EV_Mover_ClosePortal, (eventCallback_t0<idMover>) idMover::Event_ClosePortal);
            eventCallbacks.put(EV_AccelSound, (eventCallback_t1<idMover>) idMover::Event_SetAccelSound);
            eventCallbacks.put(EV_DecelSound, (eventCallback_t1<idMover>) idMover::Event_SetDecelSound);
            eventCallbacks.put(EV_MoveSound, (eventCallback_t1<idMover>) idMover::Event_SetMoveSound);
            eventCallbacks.put(EV_Mover_InitGuiTargets, (eventCallback_t0<idMover>) idMover::Event_InitGuiTargets);
            eventCallbacks.put(EV_EnableSplineAngles, (eventCallback_t0<idMover>) idMover::Event_EnableSplineAngles);
            eventCallbacks.put(EV_DisableSplineAngles, (eventCallback_t0<idMover>) idMover::Event_DisableSplineAngles);
            eventCallbacks.put(EV_RemoveInitialSplineAngles, (eventCallback_t0<idMover>) idMover::Event_RemoveInitialSplineAngles);
            eventCallbacks.put(EV_StartSpline, (eventCallback_t1<idMover>) idMover::Event_StartSpline);
            eventCallbacks.put(EV_StopSpline, (eventCallback_t0<idMover>) idMover::Event_StopSpline);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idMover>) idMover::Event_Activate);
            eventCallbacks.put(EV_IsMoving, (eventCallback_t0<idMover>) idMover::Event_IsMoving);
            eventCallbacks.put(EV_IsRotating, (eventCallback_t0<idMover>) idMover::Event_IsRotating);
        }


        protected moveState_t move;
        //
        private rotationState_t rot;
        //
        private int move_thread;
        private int rotate_thread;
        private idAngles dest_angles;
        private idAngles angle_delta;
        private idVec3 dest_position;
        private idVec3 move_delta;
        private float move_speed;
        private int move_time;
        private int deceltime;
        private int acceltime;
        private boolean stopRotation;
        private boolean useSplineAngles;
        private idEntityPtr<idEntity> splineEnt;
        private moverCommand_t lastCommand;
        private float damage;
        //
        private int/*qhandle_t*/ areaPortal;		// 0 = no portal
        //
        private final idList<idEntityPtr<idEntity>> guiTargets = (idList<idEntityPtr<idEntity>>) new idList<>(new idEntityPtr<>().getClass());

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        //
        //
        protected enum moveStage_t {

            ACCELERATION_STAGE,
            LINEAR_STAGE,
            DECELERATION_STAGE,
            FINISHED_STAGE
        };

        protected enum moverCommand_t {

            MOVER_NONE,
            MOVER_ROTATING,
            MOVER_MOVING,
            MOVER_SPLINE
        };
        //
        // mover directions.  make sure to change script/doom_defs.script if you add any, or change their order
        //
        // typedef enum {
        protected static final int DIR_UP = -1;
        protected static final int DIR_DOWN = -2;
        protected static final int DIR_LEFT = -3;
        protected static final int DIR_RIGHT = -4;
        protected static final int DIR_FORWARD = -5;
        protected static final int DIR_BACK = -6;
        protected static final int DIR_REL_UP = -7;
        protected static final int DIR_REL_DOWN = -8;
        protected static final int DIR_REL_LEFT = -9;
        protected static final int DIR_REL_RIGHT = -10;
        protected static final int DIR_REL_FORWARD = -11;
        protected static final int DIR_REL_BACK = -12;
        // } moverDir_t;

        protected static class moveState_t {

            moveStage_t stage;
            int acceleration;
            int movetime;
            int deceleration;
            idVec3 dir;
        };

        protected static class rotationState_t {

            moveStage_t stage;
            int acceleration;
            int movetime;
            int deceleration;
            idAngles rot;
        };
        //
        protected idPhysics_Parametric physicsObj;
        //
        //

        public idMover() {
//	memset( &move, 0, sizeof( move ) );
            move = new moveState_t();
//	memset( &rot, 0, sizeof( rot ) );
            rot = new rotationState_t();
            move_thread = 0;
            rotate_thread = 0;
            dest_angles = new idAngles();
            angle_delta = new idAngles();
            dest_position = new idVec3();
            move_delta = new idVec3();
            move_speed = 0.0f;
            move_time = 0;
            deceltime = 0;
            acceltime = 0;
            stopRotation = false;
            useSplineAngles = true;
            lastCommand = MOVER_NONE;
            damage = 0.0f;
            areaPortal = 0;
            fl.networkSync = true;
            physicsObj = new idPhysics_Parametric();
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            float[] damage = {0};

            move_thread = 0;
            rotate_thread = 0;
            stopRotation = false;
            lastCommand = MOVER_NONE;

            acceltime = (int) (1000 * spawnArgs.GetFloat("accel_time", "0"));
            deceltime = (int) (1000 * spawnArgs.GetFloat("decel_time", "0"));
            move_time = (int) (1000 * spawnArgs.GetFloat("move_time", "1"));	// safe default value
            move_speed = spawnArgs.GetFloat("move_speed", "0");

            spawnArgs.GetFloat("damage", "0", damage);
            this.damage = damage[0];

            dest_position = GetPhysics().GetOrigin();
            dest_angles = GetPhysics().GetAxis().ToAngles();

            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(GetPhysics().GetAxis());
            physicsObj.SetClipMask(MASK_SOLID);
            if (!spawnArgs.GetBool("solid", "1")) {
                physicsObj.SetContents(0);
            }
            if (null == renderEntity.hModel || !spawnArgs.GetBool("nopush")) {
                physicsObj.SetPusher(0);
            }
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, dest_position, getVec3_origin(), getVec3_origin());
            physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, dest_angles, getAng_zero(), getAng_zero());
            SetPhysics(physicsObj);

            // see if we are on an areaportal
            areaPortal = gameRenderWorld.FindPortal(GetPhysics().GetAbsBounds());

            if (spawnArgs.MatchPrefix("guiTarget") != null) {
                if (gameLocal.GameState() == GAMESTATE_STARTUP) {
                    PostEventMS(EV_FindGuiTargets, 0);
                } else {
                    // not during spawn, so it's ok to get the targets
                    FindGuiTargets();
                }
            }

            health = spawnArgs.GetInt("health");
            if (health != 0) {
                fl.takedamage = true;
            }

        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteStaticObject(physicsObj);

            savefile.WriteInt(etoi(move.stage));
            savefile.WriteInt(move.acceleration);
            savefile.WriteInt(move.movetime);
            savefile.WriteInt(move.deceleration);
            savefile.WriteVec3(move.dir);

            savefile.WriteInt(etoi(rot.stage));
            savefile.WriteInt(rot.acceleration);
            savefile.WriteInt(rot.movetime);
            savefile.WriteInt(rot.deceleration);
            savefile.WriteFloat(rot.rot.pitch);
            savefile.WriteFloat(rot.rot.yaw);
            savefile.WriteFloat(rot.rot.roll);

            savefile.WriteInt(move_thread);
            savefile.WriteInt(rotate_thread);

            savefile.WriteAngles(dest_angles);
            savefile.WriteAngles(angle_delta);
            savefile.WriteVec3(dest_position);
            savefile.WriteVec3(move_delta);

            savefile.WriteFloat(move_speed);
            savefile.WriteInt(move_time);
            savefile.WriteInt(deceltime);
            savefile.WriteInt(acceltime);
            savefile.WriteBool(stopRotation);
            savefile.WriteBool(useSplineAngles);
            savefile.WriteInt(etoi(lastCommand));
            savefile.WriteFloat(damage);

            savefile.WriteInt(areaPortal);
            if (areaPortal > 0) {
                savefile.WriteInt(gameRenderWorld.GetPortalState(areaPortal));
            }

            savefile.WriteInt(guiTargets.Num());
            for (i = 0; i < guiTargets.Num(); i++) {
                guiTargets.oGet(i).Save(savefile);
            }

            if (splineEnt.GetEntity() != null && splineEnt.GetEntity().GetSpline() != null) {
                idCurve_Spline<idVec3> spline = physicsObj.GetSpline();

                savefile.WriteBool(true);
                splineEnt.Save(savefile);
                savefile.WriteInt((int) spline.GetTime(0));
                savefile.WriteInt((int) (spline.GetTime(spline.GetNumValues() - 1) - spline.GetTime(0)));
                savefile.WriteInt(physicsObj.GetSplineAcceleration());
                savefile.WriteInt(physicsObj.GetSplineDeceleration());
                savefile.WriteInt(btoi(physicsObj.UsingSplineAngles()));

            } else {
                savefile.WriteBool(false);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int[] num = {0};
            boolean[] hasSpline = {false};

            savefile.ReadStaticObject(physicsObj);
            RestorePhysics(physicsObj);

            move.stage = moveStage_t.values()[savefile.ReadInt()];
            move.acceleration = savefile.ReadInt();
            move.movetime = savefile.ReadInt();
            move.deceleration = savefile.ReadInt();
            savefile.ReadVec3(move.dir);

            rot.stage = moveStage_t.values()[savefile.ReadInt()];
            rot.acceleration = savefile.ReadInt();
            rot.movetime = savefile.ReadInt();
            rot.deceleration = savefile.ReadInt();
            rot.rot.pitch = savefile.ReadFloat();
            rot.rot.yaw = savefile.ReadFloat();
            rot.rot.roll = savefile.ReadFloat();

            move_thread = savefile.ReadInt();
            rotate_thread = savefile.ReadInt();

            savefile.ReadAngles(dest_angles);
            savefile.ReadAngles(angle_delta);
            savefile.ReadVec3(dest_position);
            savefile.ReadVec3(move_delta);

            move_speed = savefile.ReadFloat();
            move_time = savefile.ReadInt();
            deceltime = savefile.ReadInt();
            acceltime = savefile.ReadInt();
            stopRotation = savefile.ReadBool();
            useSplineAngles = savefile.ReadBool();
            lastCommand = moverCommand_t.values()[savefile.ReadInt()];
            damage = savefile.ReadFloat();

            areaPortal = savefile.ReadInt();
            if (areaPortal > 0) {
                int[] portalState = {0};
                savefile.ReadInt(portalState);
                gameLocal.SetPortalState(areaPortal, portalState[0]);
            }

            guiTargets.Clear();
            savefile.ReadInt(num);
            guiTargets.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                guiTargets.oGet(i).Restore(savefile);
            }

            savefile.ReadBool(hasSpline);
            if (hasSpline[0]) {
                int[] starttime = {0};
                int[] totaltime = {0};
                int[] accel = {0};
                int[] decel = {0};
                int[] useAngles = {0};

                splineEnt.Restore(savefile);
                savefile.ReadInt(starttime);
                savefile.ReadInt(totaltime);
                savefile.ReadInt(accel);
                savefile.ReadInt(decel);
                savefile.ReadInt(useAngles);

                PostEventMS(EV_PostRestore, 0, starttime, totaltime, accel, decel, useAngles);
            }
        }

        @Override
        public void Killed(idEntity inflictor, idEntity attacker, int damage, final idVec3 dir, int location) {
            fl.takedamage = false;
            ActivateTargets(this);
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            physicsObj.WriteToSnapshot(msg);
            msg.WriteBits(etoi(move.stage), 3);
            msg.WriteBits(etoi(rot.stage), 3);
            WriteBindToSnapshot(msg);
            WriteGUIToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            moveStage_t oldMoveStage = move.stage;
            moveStage_t oldRotStage = rot.stage;

            physicsObj.ReadFromSnapshot(msg);
            move.stage = moveStage_t.values()[msg.ReadBits(3)];
            rot.stage = moveStage_t.values()[msg.ReadBits(3)];
            ReadBindFromSnapshot(msg);
            ReadGUIFromSnapshot(msg);

            if (msg.HasChanged()) {
                if (move.stage != oldMoveStage) {
                    UpdateMoveSound(oldMoveStage);
                }
                if (rot.stage != oldRotStage) {
                    UpdateRotationSound(oldRotStage);
                }
                UpdateVisuals();
            }
        }

        @Override
        public void Hide() {
            super.Hide();
            physicsObj.SetContents(0);
        }

        @Override
        public void Show() {
            super.Show();
            if (spawnArgs.GetBool("solid", "1")) {
                physicsObj.SetContents(CONTENTS_SOLID);
            }
            SetPhysics(physicsObj);
        }

        public void SetPortalState(boolean open) {
            assert (areaPortal != 0);
            gameLocal.SetPortalState(areaPortal, (open ? PS_BLOCK_NONE : PS_BLOCK_ALL).ordinal());
        }


        /*
         ================
         idMover::Event_OpenPortal

         Sets the portal associtated with this mover to be open
         ================
         */
        protected void Event_OpenPortal() {
            if (areaPortal != 0) {
                SetPortalState(true);
            }
        }

        /*
         ================
         idMover::Event_ClosePortal

         Sets the portal associtated with this mover to be closed
         ================
         */
        protected void Event_ClosePortal() {
            if (areaPortal != 0) {
                SetPortalState(false);
            }
        }

        protected void Event_PartBlocked(idEventArg<idEntity> blockingEntity) {
            if (damage > 0.0f) {
                blockingEntity.value.Damage(this, this, getVec3_origin(), "damage_moverCrush", damage, INVALID_JOINT);
            }
            if (g_debugMover.GetBool()) {
                gameLocal.Printf("%d: '%s' blocked by '%s'\n", gameLocal.time, name, blockingEntity.value.name);
            }
        }

        protected void MoveToPos(final idVec3 pos) {
            dest_position = GetLocalCoordinates(pos);
            BeginMove(null);
        }

        protected void UpdateMoveSound(moveStage_t stage) {
            switch (stage) {
                case ACCELERATION_STAGE: {
                    StartSound("snd_accel", SND_CHANNEL_BODY2, 0, false, null);
                    StartSound("snd_move", SND_CHANNEL_BODY, 0, false, null);
                    break;
                }
                case LINEAR_STAGE: {
                    StartSound("snd_move", SND_CHANNEL_BODY, 0, false, null);
                    break;
                }
                case DECELERATION_STAGE: {
                    StopSound(etoi(SND_CHANNEL_BODY), false);
                    StartSound("snd_decel", SND_CHANNEL_BODY2, 0, false, null);
                    break;
                }
                case FINISHED_STAGE: {
                    StopSound(etoi(SND_CHANNEL_BODY), false);
                    break;
                }
            }
        }

        protected void UpdateRotationSound(moveStage_t stage) {
            switch (stage) {
                case ACCELERATION_STAGE: {
                    StartSound("snd_accel", SND_CHANNEL_BODY2, 0, false, null);
                    StartSound("snd_move", SND_CHANNEL_BODY, 0, false, null);
                    break;
                }
                case LINEAR_STAGE: {
                    StartSound("snd_move", SND_CHANNEL_BODY, 0, false, null);
                    break;
                }
                case DECELERATION_STAGE: {
                    StopSound(etoi(SND_CHANNEL_BODY), false);
                    StartSound("snd_decel", SND_CHANNEL_BODY2, 0, false, null);
                    break;
                }
                case FINISHED_STAGE: {
                    StopSound(etoi(SND_CHANNEL_BODY), false);
                    break;
                }
            }
        }

        protected void SetGuiStates(final String state) {
            int i;
            if (guiTargets.Num() != 0) {
                SetGuiState("movestate", state);
            }
            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                if (renderEntity.gui[i] != null) {
                    renderEntity.gui[i].SetStateString("movestate", state);
                    renderEntity.gui[i].StateChanged(gameLocal.time, true);
                }
            }
        }

        protected void FindGuiTargets() {
            gameLocal.GetTargets(spawnArgs, guiTargets, "guiTarget");
        }

        /*
         ==============================
         idMover::SetGuiState

         key/val will be set to any renderEntity->gui's on the list
         ==============================
         */
        protected void SetGuiState(final String key, final String val) {
            gameLocal.Printf("Setting %s to %s\n", key, val);
            for (int i = 0; i < guiTargets.Num(); i++) {
                idEntity ent = guiTargets.oGet(i).GetEntity();
                if (ent != null) {
                    for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                        if (ent.GetRenderEntity() != null && ent.GetRenderEntity().gui[ j] != null) {
                            ent.GetRenderEntity().gui[ j].SetStateString(key, val);
                            ent.GetRenderEntity().gui[ j].StateChanged(gameLocal.time, true);
                        }
                    }
                    ent.UpdateVisuals();
                }
            }
        }

        protected void DoneMoving() {

            if (lastCommand != MOVER_SPLINE) {
                // set our final position so that we get rid of any numerical inaccuracy
                physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, dest_position, getVec3_origin(), getVec3_origin());
            }

            lastCommand = MOVER_NONE;
            idThread.ObjectMoveDone(move_thread, this);
            move_thread = 0;

            StopSound(etoi(SND_CHANNEL_BODY), false);
        }

        protected void DoneRotating() {
            lastCommand = MOVER_NONE;
            idThread.ObjectMoveDone(rotate_thread, this);
            rotate_thread = 0;

            StopSound(etoi(SND_CHANNEL_BODY), false);
        }

        protected void BeginMove(idThread thread) {
            moveStage_t stage;
            idVec3 org = new idVec3();
            float dist;
            float acceldist;
            int totalacceltime;
            int at;
            int dt;

            lastCommand = MOVER_MOVING;
            move_thread = 0;

            physicsObj.GetLocalOrigin(org);

            move_delta = dest_position.oMinus(org);
            if (move_delta.Compare(getVec3_zero())) {
                DoneMoving();
                return;
            }

            // scale times up to whole physics frames
            at = idPhysics.SnapTimeToPhysicsFrame(acceltime);
            move_time += at - acceltime;
            acceltime = at;
            dt = idPhysics.SnapTimeToPhysicsFrame(deceltime);
            move_time += dt - deceltime;
            deceltime = dt;

            // if we're moving at a specific speed, we need to calculate the move time
            if (move_speed != 0) {
                dist = move_delta.Length();

                totalacceltime = acceltime + deceltime;

                // calculate the distance we'll move during acceleration and deceleration
                acceldist = totalacceltime * 0.5f * 0.001f * move_speed;
                if (acceldist >= dist) {
                    // going too slow for this distance to move at a constant speed
                    move_time = totalacceltime;
                } else {
                    // calculate move time taking acceleration into account
                    move_time = (int) (totalacceltime + 1000.0f * (dist - acceldist) / move_speed);
                }
            }

            // scale time up to a whole physics frames
            move_time = idPhysics.SnapTimeToPhysicsFrame(move_time);

            if (acceltime != 0) {
                stage = ACCELERATION_STAGE;
            } else if (move_time <= deceltime) {
                stage = DECELERATION_STAGE;
            } else {
                stage = LINEAR_STAGE;
            }

            at = acceltime;
            dt = deceltime;

            if (at + dt > move_time) {
                // there's no real correct way to handle this, so we just scale
                // the times to fit into the move time in the same proportions
                at = idPhysics.SnapTimeToPhysicsFrame(at * move_time / (at + dt));
                dt = move_time - at;
            }

            move_delta = move_delta.oMultiply(1000.0f / ((float) move_time - (at + dt) * 0.5f));

            move.stage = stage;
            move.acceleration = at;
            move.movetime = move_time - at - dt;
            move.deceleration = dt;
            move.dir = new idVec3(move_delta);

            ProcessEvent(EV_ReachedPos);
        }

        protected void BeginRotation(idThread thread, boolean stopwhendone) {
            moveStage_t stage;
            idAngles ang = new idAngles();
            int at;
            int dt;

            lastCommand = MOVER_ROTATING;
            rotate_thread = 0;

            // rotation always uses move_time so that if a move was started before the rotation,
            // the rotation will take the same amount of time as the move.  If no move has been
            // started and no time is set, the rotation takes 1 second.
            if (0 == move_time) {
                move_time = 1;
            }

            physicsObj.GetLocalAngles(ang);
            angle_delta = dest_angles.oMinus(ang);
            if (angle_delta.equals(getAng_zero())) {
                // set our final angles so that we get rid of any numerical inaccuracy
                dest_angles.Normalize360();
                physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, dest_angles, getAng_zero(), getAng_zero());
                stopRotation = false;
                DoneRotating();
                return;
            }

            // scale times up to whole physics frames
            at = idPhysics.SnapTimeToPhysicsFrame(acceltime);
            move_time += at - acceltime;
            acceltime = at;
            dt = idPhysics.SnapTimeToPhysicsFrame(deceltime);
            move_time += dt - deceltime;
            deceltime = dt;
            move_time = idPhysics.SnapTimeToPhysicsFrame(move_time);

            if (acceltime != 0) {
                stage = ACCELERATION_STAGE;
            } else if (move_time <= deceltime) {
                stage = DECELERATION_STAGE;
            } else {
                stage = LINEAR_STAGE;
            }

            at = acceltime;
            dt = deceltime;

            if (at + dt > move_time) {
                // there's no real correct way to handle this, so we just scale
                // the times to fit into the move time in the same proportions
                at = idPhysics.SnapTimeToPhysicsFrame(at * move_time / (at + dt));
                dt = move_time - at;
            }

            angle_delta = angle_delta.oMultiply(1000.0f / ((float) move_time - (at + dt) * 0.5f));

            stopRotation = stopwhendone || (dt != 0);

            rot.stage = stage;
            rot.acceleration = at;
            rot.movetime = move_time - at - dt;
            rot.deceleration = dt;
            rot.rot = angle_delta;

            ProcessEvent(EV_ReachedAng);
        }

        private void VectorForDir(float dir, idVec3 vec) {
            idAngles ang = new idAngles();

            switch ((int) dir) {
                case DIR_UP:
                    vec.Set(0, 0, 1);
                    break;

                case DIR_DOWN:
                    vec.Set(0, 0, -1);
                    break;

                case DIR_LEFT:
                    physicsObj.GetLocalAngles(ang);
                    ang.pitch = 0;
                    ang.roll = 0;
                    ang.yaw += 90;
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_RIGHT:
                    physicsObj.GetLocalAngles(ang);
                    ang.pitch = 0;
                    ang.roll = 0;
                    ang.yaw -= 90;
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_FORWARD:
                    physicsObj.GetLocalAngles(ang);
                    ang.pitch = 0;
                    ang.roll = 0;
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_BACK:
                    physicsObj.GetLocalAngles(ang);
                    ang.pitch = 0;
                    ang.roll = 0;
                    ang.yaw += 180;
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_REL_UP:
                    vec.Set(0, 0, 1);
                    break;

                case DIR_REL_DOWN:
                    vec.Set(0, 0, -1);
                    break;

                case DIR_REL_LEFT:
                    physicsObj.GetLocalAngles(ang);
                    ang.ToVectors(null, vec);
                    vec.oMulSet(-1);
                    break;

                case DIR_REL_RIGHT:
                    physicsObj.GetLocalAngles(ang);
                    ang.ToVectors(null, vec);
                    break;

                case DIR_REL_FORWARD:
                    physicsObj.GetLocalAngles(ang);
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_REL_BACK:
                    physicsObj.GetLocalAngles(ang);
                    vec.oSet(ang.ToForward().oMultiply(-1));
                    break;

                default:
                    ang.Set(0, dir, 0);
                    vec.oSet(GetWorldVector(ang.ToForward()));
                    break;
            }
        }

//        private idCurve_Spline<idVec3> GetSpline(idEntity splineEntity);
        private void Event_SetCallback() {
            if ((lastCommand.equals(MOVER_ROTATING)) && 0 == rotate_thread) {
                lastCommand = MOVER_NONE;
                rotate_thread = idThread.CurrentThreadNum();
                idThread.ReturnInt(true);
            } else if ((lastCommand.equals(MOVER_MOVING) || lastCommand.equals(MOVER_SPLINE)) && 0 == move_thread) {
                lastCommand = MOVER_NONE;
                move_thread = idThread.CurrentThreadNum();
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        private void Event_TeamBlocked(idEventArg<idEntity> blockedPart, idEventArg<idEntity> blockingEntity) {
            if (g_debugMover.GetBool()) {
                gameLocal.Printf("%d: '%s' stopped due to team member '%s' blocked by '%s'\n", gameLocal.time, name, blockedPart.value.name, blockingEntity.value.name);
            }
        }

        private void Event_StopMoving() {
            physicsObj.GetLocalOrigin(dest_position);
            DoneMoving();
        }

        private void Event_StopRotating() {
            physicsObj.GetLocalAngles(dest_angles);
            physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, dest_angles, getAng_zero(), getAng_zero());
            DoneRotating();
        }

        private void Event_UpdateMove() {
            idVec3 org = new idVec3();

            physicsObj.GetLocalOrigin(org);

            UpdateMoveSound(move.stage);

            switch (move.stage) {
                case ACCELERATION_STAGE: {
                    physicsObj.SetLinearExtrapolation(EXTRAPOLATION_ACCELLINEAR, gameLocal.time, move.acceleration, org, move.dir, getVec3_origin());
                    if (move.movetime > 0) {
                        move.stage = LINEAR_STAGE;
                    } else if (move.deceleration > 0) {
                        move.stage = DECELERATION_STAGE;
                    } else {
                        move.stage = FINISHED_STAGE;
                    }
                    break;
                }
                case LINEAR_STAGE: {
                    physicsObj.SetLinearExtrapolation(EXTRAPOLATION_LINEAR, gameLocal.time, move.movetime, org, move.dir, getVec3_origin());
                    if (move.deceleration != 0) {
                        move.stage = DECELERATION_STAGE;
                    } else {
                        move.stage = FINISHED_STAGE;
                    }
                    break;
                }
                case DECELERATION_STAGE: {
                    physicsObj.SetLinearExtrapolation(EXTRAPOLATION_DECELLINEAR, gameLocal.time, move.deceleration, org, move.dir, getVec3_origin());
                    move.stage = FINISHED_STAGE;
                    break;
                }
                case FINISHED_STAGE: {
                    if (g_debugMover.GetBool()) {
                        gameLocal.Printf("%d: '%s' move done\n", gameLocal.time, name);
                    }
                    DoneMoving();
                    break;
                }
            }
        }

        private void Event_UpdateRotation() {
            idAngles ang = new idAngles();

            physicsObj.GetLocalAngles(ang);

            UpdateRotationSound(rot.stage);

            switch (rot.stage) {
                case ACCELERATION_STAGE: {
                    physicsObj.SetAngularExtrapolation(EXTRAPOLATION_ACCELLINEAR, gameLocal.time, rot.acceleration, ang, rot.rot, getAng_zero());
                    if (rot.movetime > 0) {
                        rot.stage = LINEAR_STAGE;
                    } else if (rot.deceleration > 0) {
                        rot.stage = DECELERATION_STAGE;
                    } else {
                        rot.stage = FINISHED_STAGE;
                    }
                    break;
                }
                case LINEAR_STAGE: {
                    if (!stopRotation && 0 == rot.deceleration) {
                        physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, rot.movetime, ang, rot.rot, getAng_zero());
                    } else {
                        physicsObj.SetAngularExtrapolation(EXTRAPOLATION_LINEAR, gameLocal.time, rot.movetime, ang, rot.rot, getAng_zero());
                    }

                    if (rot.deceleration != 0) {
                        rot.stage = DECELERATION_STAGE;
                    } else {
                        rot.stage = FINISHED_STAGE;
                    }
                    break;
                }
                case DECELERATION_STAGE: {
                    physicsObj.SetAngularExtrapolation(EXTRAPOLATION_DECELLINEAR, gameLocal.time, rot.deceleration, ang, rot.rot, getAng_zero());
                    rot.stage = FINISHED_STAGE;
                    break;
                }
                case FINISHED_STAGE: {
                    lastCommand = MOVER_NONE;
                    if (stopRotation) {
                        // set our final angles so that we get rid of any numerical inaccuracy
                        dest_angles.Normalize360();
                        physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, dest_angles, getAng_zero(), getAng_zero());
                        stopRotation = false;
                    } else if (physicsObj.GetAngularExtrapolationType() == EXTRAPOLATION_ACCELLINEAR) {
                        // keep our angular velocity constant
                        physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, ang, rot.rot, getAng_zero());
                    }

                    if (g_debugMover.GetBool()) {
                        gameLocal.Printf("%d: '%s' rotation done\n", gameLocal.time, name);
                    }

                    DoneRotating();
                    break;
                }
            }
        }

        private void Event_SetMoveSpeed(idEventArg<Float> speed) {
            if (speed.value <= 0) {
                gameLocal.Error("Cannot set speed less than or equal to 0.");
            }

            move_speed = speed.value;
            move_time = 0;			// move_time is calculated for each move when move_speed is non-0
        }

        private void Event_SetMoveTime(idEventArg<Float> time) {
            if (time.value <= 0) {
                gameLocal.Error("Cannot set time less than or equal to 0.");
            }

            move_speed = 0;
            move_time = (int) SEC2MS(time.value);
        }

        private void Event_SetDecelerationTime(idEventArg<Float> time) {
            if (time.value < 0) {
                gameLocal.Error("Cannot set deceleration time less than 0.");
            }

            deceltime = (int) SEC2MS(time.value);
        }

        private void Event_SetAccellerationTime(idEventArg<Float> time) {
            if (time.value < 0) {
                gameLocal.Error("Cannot set acceleration time less than 0.");
            }

            acceltime = (int) SEC2MS(time.value);
        }

        private void Event_MoveTo(idEventArg<idEntity> ent) {
            if (null == ent.value) {
                gameLocal.Warning("Entity not found");
            }

            dest_position = GetLocalCoordinates(ent.value.GetPhysics().GetOrigin());
            BeginMove(idThread.CurrentThread());
        }

        private void Event_MoveToPos(idEventArg<idVec3> pos) {
            dest_position = GetLocalCoordinates(pos.value);
            BeginMove(null);
        }

        private void Event_MoveDir(idEventArg<Float> angle, idEventArg<Float> distance) {
            idVec3 dir = new idVec3();
            idVec3 org = new idVec3();

            physicsObj.GetLocalOrigin(org);
            VectorForDir(angle.value, dir);
            dest_position = org.oPlus(dir.oMultiply(distance.value));

            BeginMove(idThread.CurrentThread());
        }

        private void Event_MoveAccelerateTo(idEventArg<Float> speed, idEventArg<Float> time) {
            float v;
            idVec3 org = new idVec3(), dir;
            int at;

            if (time.value < 0) {
                gameLocal.Error("idMover::Event_MoveAccelerateTo: cannot set acceleration time less than 0.");
            }

            dir = physicsObj.GetLinearVelocity();
            v = dir.Normalize();

            // if not moving already
            if (v == 0.0f) {
                gameLocal.Error("idMover::Event_MoveAccelerateTo: not moving.");
            }

            // if already moving faster than the desired speed
            if (v >= speed.value) {
                return;
            }

            at = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(time.value));

            lastCommand = MOVER_MOVING;

            physicsObj.GetLocalOrigin(org);

            move.stage = ACCELERATION_STAGE;
            move.acceleration = at;
            move.movetime = 0;
            move.deceleration = 0;

            StartSound("snd_accel", SND_CHANNEL_BODY2, 0, false, null);
            StartSound("snd_move", SND_CHANNEL_BODY, 0, false, null);
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_ACCELLINEAR, gameLocal.time, move.acceleration, org, dir.oMultiply(speed.value - v), dir.oMultiply(v));
        }

        private void Event_MoveDecelerateTo(idEventArg<Float> speed, idEventArg<Float> time) {
            float v;
            idVec3 org = new idVec3(), dir;
            int dt;

            if (time.value < 0) {
                gameLocal.Error("idMover::Event_MoveDecelerateTo: cannot set deceleration time less than 0.");
            }

            dir = physicsObj.GetLinearVelocity();
            v = dir.Normalize();

            // if not moving already
            if (v == 0.0f) {
                gameLocal.Error("idMover::Event_MoveDecelerateTo: not moving.");
            }

            // if already moving slower than the desired speed
            if (v <= speed.value) {
                return;
            }

            dt = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(time.value));

            lastCommand = MOVER_MOVING;

            physicsObj.GetLocalOrigin(org);

            move.stage = DECELERATION_STAGE;
            move.acceleration = 0;
            move.movetime = 0;
            move.deceleration = dt;

            StartSound("snd_decel", SND_CHANNEL_BODY2, 0, false, null);
            StartSound("snd_move", SND_CHANNEL_BODY, 0, false, null);
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_DECELLINEAR, gameLocal.time, move.deceleration, org, dir.oMultiply(v - speed.value), dir.oMultiply(speed.value));
        }

        private void Event_RotateDownTo(idEventArg<Integer> _axis, idEventArg<Float> angle) {
            int axis = _axis.value;
            idAngles ang = new idAngles();

            if ((axis < 0) || (axis > 2)) {
                gameLocal.Error("Invalid axis");
            }

            physicsObj.GetLocalAngles(ang);

            dest_angles.oSet(axis, angle.value);
            if (dest_angles.oGet(axis) > ang.oGet(axis)) {
                dest_angles.oMinSet(axis, 360);
            }

            BeginRotation(idThread.CurrentThread(), true);
        }

        private void Event_RotateUpTo(idEventArg<Integer> _axis, idEventArg<Float> angle) {
            int axis = _axis.value;
            idAngles ang = new idAngles();

            if ((axis < 0) || (axis > 2)) {
                gameLocal.Error("Invalid axis");
            }

            physicsObj.GetLocalAngles(ang);

            dest_angles.oSet(axis, angle.value);
            if (dest_angles.oGet(axis) < ang.oGet(axis)) {
                dest_angles.oPluSet(axis, 360);
            }

            BeginRotation(idThread.CurrentThread(), true);
        }

        private void Event_RotateTo(idEventArg<idAngles> angles) {
            dest_angles.oSet(angles.value);
            BeginRotation(idThread.CurrentThread(), true);
        }

        private void Event_Rotate(idEventArg<idVec3> angles) {
            idAngles ang = new idAngles();

            if (rotate_thread != 0) {
                DoneRotating();
            }

            physicsObj.GetLocalAngles(ang);
            dest_angles = ang.oPlus(angles.value.oMultiply(move_time - (acceltime + deceltime) / 2).oMultiply(0.001f));

            BeginRotation(idThread.CurrentThread(), false);
        }

        private void Event_RotateOnce(idEventArg<idVec3> angles) {
            idAngles ang = new idAngles();

            if (rotate_thread != 0) {
                DoneRotating();
            }

            physicsObj.GetLocalAngles(ang);
            dest_angles = ang.oPlus(angles.value);

            BeginRotation(idThread.CurrentThread(), true);
        }

        private void Event_Bob(idEventArg<Float> speed, idEventArg<Float> phase, idEventArg<idVec3> depth) {
            idVec3 org = new idVec3();

            physicsObj.GetLocalOrigin(org);
            physicsObj.SetLinearExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), (int) (speed.value * 1000 * phase.value),
                    (int) (speed.value * 500), org, depth.value.oMultiply(2.0f), getVec3_origin());
        }

        private void Event_Sway(idEventArg<Float> speed, idEventArg<Float> phase, idEventArg<idVec3> _depth) {
            idAngles depth = new idAngles(_depth.value);
            idAngles ang = new idAngles(), angSpeed;
            float duration;

            physicsObj.GetLocalAngles(ang);
            assert (speed.value > 0.0f);
            duration = idMath.Sqrt(depth.oGet(0) * depth.oGet(0) + depth.oGet(1) * depth.oGet(1) + depth.oGet(2) * depth.oGet(2)) / speed.value;
            angSpeed = depth.oDivide(duration * idMath.SQRT_1OVER2);
            physicsObj.SetAngularExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), (int) (duration * 1000.0f * phase.value), (int) (duration * 1000.0f), ang, angSpeed, getAng_zero());
        }

        private void Event_SetAccelSound(final idEventArg<String> sound) {
//	refSound.SetSound( "accel", sound );
        }

        private void Event_SetDecelSound(final idEventArg<String> sound) {
//	refSound.SetSound( "decel", sound );
        }

        private void Event_SetMoveSound(final idEventArg<String> sound) {
//	refSound.SetSound( "move", sound );
        }

        private void Event_FindGuiTargets() {
            FindGuiTargets();
        }

        private void Event_InitGuiTargets() {
            SetGuiStates(guiBinaryMoverStates[etoi(MOVER_POS1)]);
        }

        private void Event_EnableSplineAngles() {
            useSplineAngles = true;
        }

        private void Event_DisableSplineAngles() {
            useSplineAngles = false;
        }

        private void Event_RemoveInitialSplineAngles() {
            idCurve_Spline<idVec3> spline;
            idAngles ang;

            spline = physicsObj.GetSpline();
            if (null == spline) {
                return;
            }
            ang = spline.GetCurrentFirstDerivative(0).ToAngles();
            physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, ang.oNegative(), getAng_zero(), getAng_zero());
        }

        private void Event_StartSpline(idEventArg<idEntity> _splineEntity) {
            idEntity splineEntity = _splineEntity.value;
            idCurve_Spline<idVec3> spline;

            if (null == splineEntity) {
                return;
            }

            // Needed for savegames
            splineEnt = new idEntityPtr<>(splineEntity);

            spline = splineEntity.GetSpline();
            if (null == spline) {
                return;
            }

            lastCommand = MOVER_SPLINE;
            move_thread = 0;

            if (acceltime + deceltime > move_time) {
                acceltime = move_time / 2;
                deceltime = move_time - acceltime;
            }
            move.stage = FINISHED_STAGE;
            move.acceleration = acceltime;
            move.movetime = move_time;
            move.deceleration = deceltime;

            spline.MakeUniform(move_time);
            spline.ShiftTime(gameLocal.time - spline.GetTime(0));

            physicsObj.SetSpline(spline, move.acceleration, move.deceleration, useSplineAngles);
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, dest_position, getVec3_origin(), getVec3_origin());
        }

        private void Event_StopSpline() {
            physicsObj.SetSpline(null, 0, 0, useSplineAngles);
            splineEnt = null;
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            Show();
            Event_StartSpline(idEventArg.toArg(this));
        }

        private void Event_PostRestore(idEventArg<Integer> start, idEventArg<Integer> total, idEventArg<Integer> accel,
                                       idEventArg<Integer> decel, idEventArg<Integer> useSplineAng) {
            idCurve_Spline<idVec3> spline;

            idEntity splineEntity = splineEnt.GetEntity();
            if (null == splineEntity) {
                // We should never get this event if splineEnt is invalid
                common.Warning("Invalid spline entity during restore\n");
                return;
            }

            spline = splineEntity.GetSpline();

            spline.MakeUniform(total.value);
            spline.ShiftTime(start.value - spline.GetTime(0));

            physicsObj.SetSpline(spline, accel.value, decel.value, (useSplineAng.value != 0));
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, dest_position, getVec3_origin(), getVec3_origin());
        }

        private void Event_IsMoving() {
            if (physicsObj.GetLinearExtrapolationType() == EXTRAPOLATION_NONE) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
            }
        }

        private void Event_IsRotating() {
            if (physicsObj.GetAngularExtrapolationType() == EXTRAPOLATION_NONE) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
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

    /*
     ===============================================================================

     idSplinePath, holds a spline path to be used by an idMover

     ===============================================================================
     */
    public static class idSplinePath extends idEntity {
//	CLASS_PROTOTYPE( idSplinePath );

        public idSplinePath() {//TODO:delete this class?
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    public static class floorInfo_s {

        idVec3 pos;
        idStr door;
        int floor;
    };

    /*
     ===============================================================================

     idElevator

     ===============================================================================
     */
    public static class idElevator extends idMover {
        // CLASS_PROTOTYPE( idElevator );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idMover.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idElevator>) idElevator::Event_Activate);
            eventCallbacks.put(EV_TeamBlocked, (eventCallback_t2<idElevator>) idElevator::Event_TeamBlocked);
            eventCallbacks.put(EV_PartBlocked, (eventCallback_t1<idElevator>) idElevator::Event_PartBlocked);
            eventCallbacks.put(EV_PostArrival, (eventCallback_t0<idElevator>) idElevator::Event_PostFloorArrival);
            eventCallbacks.put(EV_GotoFloor, (eventCallback_t1<idElevator>) idElevator::Event_GotoFloor);
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idElevator>) idElevator::Event_Touch);
        }

        protected enum elevatorState_t {

            INIT,
            IDLE,
            WAITING_ON_DOORS
        };
        //
        private elevatorState_t state;
        private idList<floorInfo_s> floorInfo;
        private int currentFloor;
        private int pendingFloor;
        private int lastFloor;
        private boolean controlsDisabled;
        private float returnTime;
        private int returnFloor;
        private int lastTouchTime;
        //
        //

        public idElevator() {
            state = INIT;
            floorInfo = new idList<>();
            currentFloor = 0;
            pendingFloor = 0;
            lastFloor = 0;
            controlsDisabled = false;
            lastTouchTime = 0;
            returnFloor = 0;
            returnTime = 0;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            idStr str;
            int len1;

            lastFloor = 0;
            currentFloor = 0;
            pendingFloor = spawnArgs.GetInt("floor", "1");
            SetGuiStates((pendingFloor == 1) ? guiBinaryMoverStates[0] : guiBinaryMoverStates[1]);

            returnTime = spawnArgs.GetFloat("returnTime");
            returnFloor = spawnArgs.GetInt("returnFloor");

            len1 = "floorPos_".length();
            idKeyValue kv = spawnArgs.MatchPrefix("floorPos_", null);
            while (kv != null) {
                str = kv.GetKey().Right(kv.GetKey().Length() - len1);
                floorInfo_s fi = new floorInfo_s();
                fi.floor = Integer.parseInt(str.toString());
                fi.door = new idStr(spawnArgs.GetString(va("floorDoor_%d", fi.floor)));
                fi.pos = spawnArgs.GetVector(kv.GetKey().toString());
                floorInfo.Append(fi);
                kv = spawnArgs.MatchPrefix("floorPos_", kv);
            }
            lastTouchTime = 0;
            state = INIT;
            BecomeActive(TH_THINK | TH_PHYSICS);
            PostEventMS(EV_Mover_InitGuiTargets, 0);
            controlsDisabled = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(etoi(state));

            savefile.WriteInt(floorInfo.Num());
            for (i = 0; i < floorInfo.Num(); i++) {
                savefile.WriteVec3(floorInfo.oGet(i).pos);
                savefile.WriteString(floorInfo.oGet(i).door.toString());
                savefile.WriteInt(floorInfo.oGet(i).floor);
            }

            savefile.WriteInt(currentFloor);
            savefile.WriteInt(pendingFloor);
            savefile.WriteInt(lastFloor);
            savefile.WriteBool(controlsDisabled);
            savefile.WriteFloat(returnTime);
            savefile.WriteInt(returnFloor);
            savefile.WriteInt(lastTouchTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int num;

            state = elevatorState_t.values()[savefile.ReadInt()];

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                floorInfo_s floor = new floorInfo_s();

                savefile.ReadVec3(floor.pos);
                savefile.ReadString(floor.door);
                floor.floor = savefile.ReadInt();

                floorInfo.Append(floor);
            }

            currentFloor = savefile.ReadInt();
            pendingFloor = savefile.ReadInt();
            lastFloor = savefile.ReadInt();
            controlsDisabled = savefile.ReadBool();
            returnTime = savefile.ReadFloat();
            returnFloor = savefile.ReadInt();
            lastTouchTime = savefile.ReadInt();
        }

        @Override
        public boolean HandleSingleGuiCommand(idEntity entityGui, idLexer src) {
            idToken token = new idToken();

            if (controlsDisabled) {
                return false;
            }

            if (!src.ReadToken(token)) {
                return false;
            }

            if (token.equals(";")) {
                return false;
            }

            if (token.Icmp("changefloor") == 0) {
                if (src.ReadToken(token)) {
                    int newFloor = Integer.parseInt(token.toString());
                    if (newFloor == currentFloor) {
                        // open currentFloor and interior doors
                        OpenInnerDoor();
                        OpenFloorDoor(currentFloor);
                    } else {
                        idDoor door = GetDoor(spawnArgs.GetString("innerdoor"));
                        if (door != null && door.IsOpen()) {
                            PostEventSec(EV_GotoFloor, 0.5f, newFloor);
                        } else {
                            ProcessEvent(EV_GotoFloor, newFloor);
                        }
                    }
                    return true;
                }
            }

            src.UnreadToken(token);
            return false;
        }

        public void Event_GotoFloor(idEventArg<Integer> floor) {
            floorInfo_s fi = GetFloorInfo(floor.value);
            if (fi != null) {
                idDoor door = GetDoor(spawnArgs.GetString("innerdoor"));
                if (door != null) {
                    if (door.IsBlocked() || door.IsOpen()) {
                        PostEventSec(EV_GotoFloor, 0.5f, floor);
                        return;
                    }
                }
                DisableAllDoors();
                CloseAllDoors();
                state = WAITING_ON_DOORS;
                pendingFloor = floor.value;
            }
        }

        public floorInfo_s GetFloorInfo(int floor) {
            for (int i = 0; i < floorInfo.Num(); i++) {
                if (floorInfo.oGet(i).floor == floor) {
                    return floorInfo.oGet(i);
                }
            }
            return null;
        }

        @Override
        protected void DoneMoving() {
            super.DoneMoving();
            EnableProperDoors();
            idKeyValue kv = spawnArgs.MatchPrefix("statusGui");
            while (kv != null) {
                idEntity ent = gameLocal.FindEntity(kv.GetValue().toString());
                if (ent != null) {
                    for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                        if (ent.GetRenderEntity() != null && ent.GetRenderEntity().gui[ j] != null) {
                            ent.GetRenderEntity().gui[ j].SetStateString("floor", va("%d", currentFloor));
                            ent.GetRenderEntity().gui[ j].StateChanged(gameLocal.time, true);
                        }
                    }
                    ent.UpdateVisuals();
                }
                kv = spawnArgs.MatchPrefix("statusGui", kv);
            }
            if (spawnArgs.GetInt("pauseOnFloor", "-1") == currentFloor) {
                PostEventSec(EV_PostArrival, spawnArgs.GetFloat("pauseTime"));
            } else {
                Event_PostFloorArrival();
            }
        }

        @Override
        protected void BeginMove(idThread thread /*= NULL*/) {
            controlsDisabled = true;
            CloseAllDoors();
            DisableAllDoors();
            idKeyValue kv = spawnArgs.MatchPrefix("statusGui");
            while (kv != null) {
                idEntity ent = gameLocal.FindEntity(kv.GetValue().toString());
                if (ent != null) {
                    for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                        if (ent.GetRenderEntity() != null && ent.GetRenderEntity().gui[ j] != null) {
                            ent.GetRenderEntity().gui[ j].SetStateString("floor", "");
                            ent.GetRenderEntity().gui[ j].StateChanged(gameLocal.time, true);
                        }
                    }
                    ent.UpdateVisuals();
                }
                kv = spawnArgs.MatchPrefix("statusGui", kv);
            }
            SetGuiStates((pendingFloor == 1) ? guiBinaryMoverStates[3] : guiBinaryMoverStates[2]);
            super.BeginMove(thread);
        }
//
//        protected void SpawnTrigger(final idVec3 pos);
//
//        protected void GetLocalTriggerPosition();
//

        protected void Event_Touch(idEventArg<idEntity> other, idEventArg<trace_s> trace) {

            if (gameLocal.time < lastTouchTime + 2000) {
                return;
            }

            if (!other.value.IsType(idPlayer.class)) {
                return;
            }

            lastTouchTime = gameLocal.time;

            if ((thinkFlags & TH_PHYSICS) != 0) {
                return;
            }

            int triggerFloor = spawnArgs.GetInt("triggerFloor");
            if (spawnArgs.GetBool("trigger") && triggerFloor != currentFloor) {
                PostEventSec(EV_GotoFloor, 0.25f, triggerFloor);
            }
        }

        private idDoor GetDoor(final String name) {
            idEntity ent;
            idEntity master;
            idDoor doorEnt;

            doorEnt = null;
            if (name != null && !name.isEmpty()) {
                ent = gameLocal.FindEntity(name);
                if (ent != null && ent.IsType(idDoor.class)) {
                    doorEnt = (idDoor) ent;
                    master = doorEnt.GetMoveMaster();
                    if (master != doorEnt) {
                        if (master.IsType(idDoor.class)) {
                            doorEnt = (idDoor) master;
                        } else {
                            doorEnt = null;
                        }
                    }
                }
            }

            return doorEnt;
        }

        @Override
        public void Think() {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();
            idDoor doorEnt = GetDoor(spawnArgs.GetString("innerdoor"));
            if (state == INIT) {
                state = IDLE;
                if (doorEnt != null) {
                    doorEnt.BindTeam(this);
                    doorEnt.spawnArgs.Set("snd_open", "");
                    doorEnt.spawnArgs.Set("snd_close", "");
                    doorEnt.spawnArgs.Set("snd_opened", "");
                }
                for (int i = 0; i < floorInfo.Num(); i++) {
                    idDoor door = GetDoor(floorInfo.oGet(i).door.toString());
                    if (door != null) {
                        door.SetCompanion(doorEnt);
                    }
                }

                Event_GotoFloor(idEventArg.toArg(pendingFloor));
                DisableAllDoors();
                SetGuiStates((pendingFloor == 1) ? guiBinaryMoverStates[0] : guiBinaryMoverStates[1]);
            } else if (state == WAITING_ON_DOORS) {
                if (doorEnt != null) {
                    state = doorEnt.IsOpen() ? WAITING_ON_DOORS : IDLE;
                } else {
                    state = IDLE;
                }
                if (state == IDLE) {
                    lastFloor = currentFloor;
                    currentFloor = pendingFloor;
                    floorInfo_s fi = GetFloorInfo(currentFloor);
                    if (fi != null) {
                        MoveToPos(fi.pos);
                    }
                }
            }
            RunPhysics();
            Present();
        }

        private void OpenInnerDoor() {
            idDoor door = GetDoor(spawnArgs.GetString("innerdoor"));
            if (door != null) {
                door.Open();
            }
        }

        private void OpenFloorDoor(int floor) {
            floorInfo_s fi = GetFloorInfo(floor);
            if (fi != null) {
                idDoor door = GetDoor(fi.door.toString());
                if (door != null) {
                    door.Open();
                }
            }
        }

        private void CloseAllDoors() {
            idDoor door = GetDoor(spawnArgs.GetString("innerdoor"));
            if (door != null) {
                door.Close();
            }
            for (int i = 0; i < floorInfo.Num(); i++) {
                door = GetDoor(floorInfo.oGet(i).door.toString());
                if (door != null) {
                    door.Close();
                }
            }
        }

        private void DisableAllDoors() {
            idDoor door = GetDoor(spawnArgs.GetString("innerdoor"));
            if (door != null) {
                door.Enable(false);
            }
            for (int i = 0; i < floorInfo.Num(); i++) {
                door = GetDoor(floorInfo.oGet(i).door.toString());
                if (door != null) {
                    door.Enable(false);
                }
            }
        }

        private void EnableProperDoors() {
            idDoor door = GetDoor(spawnArgs.GetString("innerdoor"));
            if (door != null) {
                door.Enable(true);
            }
            for (int i = 0; i < floorInfo.Num(); i++) {
                if (floorInfo.oGet(i).floor == currentFloor) {
                    door = GetDoor(floorInfo.oGet(i).door.toString());
                    if (door != null) {
                        door.Enable(true);
                        break;
                    }
                }
            }
        }

        private void Event_TeamBlocked(idEventArg<idEntity> blockedEntity, idEventArg<idEntity> blockingEntity) {
            if (blockedEntity.value == this) {
                Event_GotoFloor(idEventArg.toArg(lastFloor));
            } else if (blockedEntity != null && blockedEntity.value.IsType(idDoor.class)) {
                // open the inner doors if one is blocked
                idDoor blocked = (idDoor) blockedEntity.value;
                idDoor door = GetDoor(spawnArgs.GetString("innerdoor"));
                if (door != null && blocked.GetMoveMaster() == door.GetMoveMaster()) {//TODO:equalds
                    door.SetBlocked(true);
                    OpenInnerDoor();
                    OpenFloorDoor(currentFloor);
                }
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            int triggerFloor = spawnArgs.GetInt("triggerFloor");
            if (spawnArgs.GetBool("trigger") && triggerFloor != currentFloor) {
                Event_GotoFloor(idEventArg.toArg(triggerFloor));
            }
        }

        private void Event_PostFloorArrival() {
            OpenFloorDoor(currentFloor);
            OpenInnerDoor();
            SetGuiStates((currentFloor == 1) ? guiBinaryMoverStates[0] : guiBinaryMoverStates[1]);
            controlsDisabled = false;
            if (returnTime > 0.0f && returnFloor != currentFloor) {
                PostEventSec(EV_GotoFloor, returnTime, returnFloor);
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

    /*
     ===============================================================================

     Binary movers.

     ===============================================================================
     */
    public enum moverState_t {

        MOVER_POS1,
        MOVER_POS2,
        MOVER_1TO2,
        MOVER_2TO1
    };


    /*
     ===============================================================================

     idMover_Binary

     Doors, plats, and buttons are all binary (two position) movers
     Pos1 is "at rest", pos2 is "activated"

     ===============================================================================
     */
    public static class idMover_Binary extends idEntity {
        // CLASS_PROTOTYPE( idMover_Binary );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_FindGuiTargets, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_FindGuiTargets);
            eventCallbacks.put(EV_Thread_SetCallback, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_SetCallback);
            eventCallbacks.put(EV_Mover_ReturnToPos1, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_ReturnToPos1);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idMover_Binary>) idMover_Binary::Event_Use_BinaryMover);
            eventCallbacks.put(EV_ReachedPos, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_Reached_BinaryMover);
            eventCallbacks.put(EV_Mover_MatchTeam, (eventCallback_t2<idMover_Binary>) idMover_Binary::Event_MatchActivateTeam);
            eventCallbacks.put(EV_Mover_Enable, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_Enable);
            eventCallbacks.put(EV_Mover_Disable, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_Disable);
            eventCallbacks.put(EV_Mover_OpenPortal, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_OpenPortal);
            eventCallbacks.put(EV_Mover_ClosePortal, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_ClosePortal);
            eventCallbacks.put(EV_Mover_InitGuiTargets, (eventCallback_t0<idMover_Binary>) idMover_Binary::Event_InitGuiTargets);
        }

        protected idVec3                        pos1;
        protected idVec3                        pos2;
        protected moverState_t                  moverState;
        protected idMover_Binary                moveMaster;
        protected idMover_Binary                activateChain;
        protected int                           soundPos1;
        protected int                           sound1to2;
        protected int                           sound2to1;
        protected int                           soundPos2;
        protected int                           soundLoop;
        protected float                         wait;
        protected float                         damage;
        protected int                           duration;
        protected int                           accelTime;
        protected int                           decelTime;
        protected idEntityPtr<idEntity>         activatedBy;
        protected int                           stateStartTime;
        protected idStr                         team;
        protected boolean                       enabled;
        protected int                           move_thread;
        protected int                           updateStatus;        // 1 = lock behaviour, 2 = open close status
        protected idStrList                     buddies;
        protected idPhysics_Parametric          physicsObj;
        protected int/*qhandle_t*/              areaPortal;          // 0 = no portal
        protected boolean                       blocked;
        protected idList<idEntityPtr<idEntity>> guiTargets;
        //
        //

        public idMover_Binary() {
            pos1 = new idVec3();
            pos2 = new idVec3();
            moverState = MOVER_POS1;
            moveMaster = null;
            activateChain = null;
            soundPos1 = 0;
            sound1to2 = 0;
            sound2to1 = 0;
            soundPos2 = 0;
            soundLoop = 0;
            wait = 0.0f;
            damage = 0.0f;
            duration = 0;
            accelTime = 0;
            decelTime = 0;
            activatedBy = new idEntityPtr<>(this);
            stateStartTime = 0;
            team = new idStr();
            enabled = false;
            move_thread = 0;
            updateStatus = 0;
            buddies = new idStrList();
            physicsObj = new idPhysics_Parametric();
            areaPortal = 0;
            blocked = false;
            fl.networkSync = true;
            guiTargets = new idList(idEntityPtr.class);
        }

        // ~idMover_Binary();
        /*
         ================
         idMover_Binary::Spawn

         Base class for all movers.

         "wait"		wait before returning (3 default, -1 = never return)
         "speed"		movement speed
         ================
         */
        @Override
        public void Spawn() {
            super.Spawn();
            
            idEntity ent;
            String[] temp = {null};

            move_thread = 0;
            enabled = true;
            areaPortal = 0;

            activateChain = null;

            wait = spawnArgs.GetFloat("wait", "0");

            updateStatus = spawnArgs.GetInt("updateStatus", "0");

            idKeyValue kv = spawnArgs.MatchPrefix("buddy", null);
            while (kv != null) {
                buddies.Append(kv.GetValue());
                kv = spawnArgs.MatchPrefix("buddy", kv);
            }

            spawnArgs.GetString("team", "", temp);
            team = new idStr(temp[0]);

            if (0 == team.Length()) {
                ent = this;
            } else {
                // find the first entity spawned on this team (which could be us)
                for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                    if (ent.IsType(idMover_Binary.class) && NOT(idStr.Icmp(((idMover_Binary) ent).team.toString(), temp[0]))) {
                        break;
                    }
                }
                if (null == ent) {
                    ent = this;
                }
            }
            moveMaster = (idMover_Binary) ent;

            // create a physics team for the binary mover parts
            if (ent != this) {
                JoinTeam(ent);
            }

            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(GetPhysics().GetAxis());
            physicsObj.SetClipMask(MASK_SOLID);
            if (!spawnArgs.GetBool("solid", "1")) {
                physicsObj.SetContents(0);
            }
            if (!spawnArgs.GetBool("nopush")) {
                physicsObj.SetPusher(0);
            }
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, GetPhysics().GetOrigin(), getVec3_origin(), getVec3_origin());
            physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, GetPhysics().GetAxis().ToAngles(), getAng_zero(), getAng_zero());
            SetPhysics(physicsObj);

            if (moveMaster != this) {
                JoinActivateTeam(moveMaster);
            }

            idBounds soundOrigin = new idBounds();
            idMover_Binary slave;

            soundOrigin.Clear();
            for (slave = moveMaster; slave != null; slave = slave.activateChain) {
                soundOrigin.oPluSet(slave.GetPhysics().GetAbsBounds());
            }
            moveMaster.refSound.origin = soundOrigin.GetCenter();

            if (spawnArgs.MatchPrefix("guiTarget") != null) {
                if (gameLocal.GameState() == GAMESTATE_STARTUP) {
                    PostEventMS(EV_FindGuiTargets, 0);
                } else {
                    // not during spawn, so it's ok to get the targets
                    FindGuiTargets();
                }
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteVec3(pos1);
            savefile.WriteVec3(pos2);
            savefile.WriteInt(etoi(moverState));

            savefile.WriteObject(moveMaster);
            savefile.WriteObject(activateChain);

            savefile.WriteInt(soundPos1);
            savefile.WriteInt(sound1to2);
            savefile.WriteInt(sound2to1);
            savefile.WriteInt(soundPos2);
            savefile.WriteInt(soundLoop);

            savefile.WriteFloat(wait);
            savefile.WriteFloat(damage);

            savefile.WriteInt(duration);
            savefile.WriteInt(accelTime);
            savefile.WriteInt(decelTime);

            activatedBy.Save(savefile);

            savefile.WriteInt(stateStartTime);
            savefile.WriteString(team);
            savefile.WriteBool(enabled);

            savefile.WriteInt(move_thread);
            savefile.WriteInt(updateStatus);

            savefile.WriteInt(buddies.Num());
            for (i = 0; i < buddies.Num(); i++) {
                savefile.WriteString(buddies.oGet(i));
            }

            savefile.WriteStaticObject(physicsObj);

            savefile.WriteInt(areaPortal);
            if (areaPortal != 0) {
                savefile.WriteInt(gameRenderWorld.GetPortalState(areaPortal));
            }
            savefile.WriteBool(blocked);

            savefile.WriteInt(guiTargets.Num());
            for (i = 0; i < guiTargets.Num(); i++) {
                guiTargets.oGet(i).Save(savefile);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int num, portalState;
            idStr temp = new idStr();

            savefile.ReadVec3(pos1);
            savefile.ReadVec3(pos2);
            moverState = moverState_t.values()[savefile.ReadInt()];

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/moveMaster);
            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/activateChain);

            soundPos1 = savefile.ReadInt();
            sound1to2 = savefile.ReadInt();
            sound2to1 = savefile.ReadInt();
            soundPos2 = savefile.ReadInt();
            soundLoop = savefile.ReadInt();

            wait = savefile.ReadFloat();
            damage = savefile.ReadFloat();

            duration = savefile.ReadInt();
            accelTime = savefile.ReadInt();
            decelTime = savefile.ReadInt();

            activatedBy.Restore(savefile);

            stateStartTime = savefile.ReadInt();

            savefile.ReadString(team);
            enabled = savefile.ReadBool();

            move_thread = savefile.ReadInt();
            updateStatus = savefile.ReadInt();

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                savefile.ReadString(temp);
                buddies.Append(temp);
            }

            savefile.ReadStaticObject(physicsObj);
            RestorePhysics(physicsObj);

            areaPortal = savefile.ReadInt();
            if (areaPortal != 0) {
                portalState = savefile.ReadInt();
                gameLocal.SetPortalState(areaPortal, portalState);
            }
            blocked = savefile.ReadBool();

            guiTargets.Clear();
            num = savefile.ReadInt();
            guiTargets.SetNum(num);
            for (i = 0; i < num; i++) {
                guiTargets.oGet(i).Restore(savefile);
            }
        }

        @Override
        public void PreBind() {
            pos1 = GetWorldCoordinates(pos1);
            pos2 = GetWorldCoordinates(pos2);
        }

        @Override
        public void PostBind() {
            pos1 = GetLocalCoordinates(pos1);
            pos2 = GetLocalCoordinates(pos2);
        }

        public void Enable(boolean b) {
            enabled = b;
        }

        /*
         ================
         idMover_Binary::InitSpeed

         pos1, pos2, and speed are passed in so the movement delta can be calculated
         ================
         */
        public void InitSpeed(idVec3 mpos1, idVec3 mpos2, float mspeed, float maccelTime, float mdecelTime) {
            idVec3 move;
            float distance;
            float speed;

            pos1 = mpos1;
            pos2 = mpos2;

            accelTime = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(maccelTime));
            decelTime = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(mdecelTime));

            speed = mspeed != 0 ? mspeed : 100;

            // calculate time to reach second position from speed
            move = pos2.oMinus(pos1);
            distance = move.Length();
            duration = idPhysics.SnapTimeToPhysicsFrame((int) (distance * 1000 / speed));
            if (duration <= 0) {
                duration = 1;
            }

            moverState = MOVER_POS1;

            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, pos1, getVec3_origin(), getVec3_origin());
            physicsObj.SetLinearInterpolation(0, 0, 0, 0, getVec3_origin(), getVec3_origin());
            SetOrigin(pos1);

            PostEventMS(EV_Mover_InitGuiTargets, 0);
        }

        /*
         ================
         idMover_Binary::InitTime

         pos1, pos2, and time are passed in so the movement delta can be calculated
         ================
         */
        public void InitTime(idVec3 mpos1, idVec3 mpos2, float mtime, float maccelTime, float mdecelTime) {

            pos1 = mpos1;
            pos2 = mpos2;

            accelTime = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(maccelTime));
            decelTime = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(mdecelTime));

            duration = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(mtime));
            if (duration <= 0) {
                duration = 1;
            }

            moverState = MOVER_POS1;

            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, pos1, getVec3_origin(), getVec3_origin());
            physicsObj.SetLinearInterpolation(0, 0, 0, 0, getVec3_origin(), getVec3_origin());
            SetOrigin(pos1);

            PostEventMS(EV_Mover_InitGuiTargets, 0);
        }

        public void GotoPosition1() {
            idMover_Binary slave;
            int partial;

            // only the master should control this
            if (moveMaster != this) {
                moveMaster.GotoPosition1();
                return;
            }

            SetGuiStates(guiBinaryMoverStates[etoi(MOVER_2TO1)]);

            if ((moverState == MOVER_POS1) || (moverState == MOVER_2TO1)) {
                // already there, or on the way
                return;
            }

            if (moverState == MOVER_POS2) {
                for (slave = this; slave != null; slave = slave.activateChain) {
                    slave.CancelEvents(EV_Mover_ReturnToPos1);
                }
                if (!spawnArgs.GetBool("toggle")) {
                    ProcessEvent(EV_Mover_ReturnToPos1);
                }
                return;
            }

            // only partway up before reversing
            if (moverState == MOVER_1TO2) {
                // use the physics times because this might be executed during the physics simulation
                partial = physicsObj.GetLinearEndTime() - physicsObj.GetTime();
                assert (partial >= 0);
                if (partial < 0) {
                    partial = 0;
                }
                MatchActivateTeam(MOVER_2TO1, physicsObj.GetTime() - partial);
                // if already at at position 1 (partial == duration) execute the reached event
                if (partial >= duration) {
                    Event_Reached_BinaryMover();
                }
            }
        }

        public void GotoPosition2() {
            int partial;

            // only the master should control this
            if (moveMaster != this) {
                moveMaster.GotoPosition2();
                return;
            }

            SetGuiStates(guiBinaryMoverStates[etoi(MOVER_1TO2)]);

            if ((moverState == MOVER_POS2) || (moverState == MOVER_1TO2)) {
                // already there, or on the way
                return;
            }

            if (moverState == MOVER_POS1) {
                MatchActivateTeam(MOVER_1TO2, gameLocal.time);

                // open areaportal
                ProcessEvent(EV_Mover_OpenPortal);
                return;
            }

            // only partway up before reversing
            if (moverState == MOVER_2TO1) {
                // use the physics times because this might be executed during the physics simulation
                partial = physicsObj.GetLinearEndTime() - physicsObj.GetTime();
                assert (partial >= 0);
                if (partial < 0) {
                    partial = 0;
                }
                MatchActivateTeam(MOVER_1TO2, physicsObj.GetTime() - partial);
                // if already at at position 2 (partial == duration) execute the reached event
                if (partial >= duration) {
                    Event_Reached_BinaryMover();
                }
            }
        }

        public void Use_BinaryMover(idEntity activator) {
            // only the master should be used
            if (moveMaster != this) {
                moveMaster.Use_BinaryMover(activator);
                return;
            }

            if (!enabled) {
                return;
            }

            activatedBy.oSet(activator);

            if (moverState == MOVER_POS1) {
                // FIXME: start moving USERCMD_MSEC later, because if this was player
                // triggered, gameLocal.time hasn't been advanced yet
                MatchActivateTeam(MOVER_1TO2, gameLocal.time + USERCMD_MSEC);

                SetGuiStates(guiBinaryMoverStates[etoi(MOVER_1TO2)]);
                // open areaportal
                ProcessEvent(EV_Mover_OpenPortal);
                return;
            }

            // if all the way up, just delay before coming down
            if (moverState == MOVER_POS2) {
                idMover_Binary slave;

                if (wait == -1) {
                    return;
                }

                SetGuiStates(guiBinaryMoverStates[etoi(MOVER_2TO1)]);

                for (slave = this; slave != null; slave = slave.activateChain) {
                    slave.CancelEvents(EV_Mover_ReturnToPos1);
                    slave.PostEventSec(EV_Mover_ReturnToPos1, spawnArgs.GetBool("toggle") ? 0 : wait);
                }
                return;
            }

            // only partway down before reversing
            if (moverState == MOVER_2TO1) {
                GotoPosition2();
                return;
            }

            // only partway up before reversing
            if (moverState == MOVER_1TO2) {
                GotoPosition1();
                return;
            }
        }

        public void SetGuiStates(final String state) {
            if (guiTargets.Num() != 0) {
                SetGuiState("movestate", state);
            }

            idMover_Binary mb = activateChain;
            while (mb != null) {
                if (mb.guiTargets.Num() != 0) {
                    mb.SetGuiState("movestate", state);
                }
                mb = mb.activateChain;
            }
        }

        public void UpdateBuddies(int val) {
            int i, c;

            if (updateStatus == 2) {
                c = buddies.Num();
                for (i = 0; i < c; i++) {
                    idEntity buddy = gameLocal.FindEntity(buddies.oGet(i));
                    if (buddy != null) {
                        buddy.SetShaderParm(SHADERPARM_MODE, val);
                        buddy.UpdateVisuals();
                    }
                }
            }
        }

        public idMover_Binary GetActivateChain() {
            return activateChain;
        }

        public idMover_Binary GetMoveMaster() {
            return moveMaster;
        }

        /*
         ================
         idMover_Binary::BindTeam

         All entities in a mover team will be bound 
         ================
         */
        public void BindTeam(idEntity bindTo) {
            idMover_Binary slave;

            for (slave = this; slave != null; slave = slave.activateChain) {
                slave.Bind(bindTo, true);
            }
        }

        public void SetBlocked(boolean b) {
            for (idMover_Binary slave = moveMaster; slave != null; slave = slave.activateChain) {
                slave.blocked = b;
                if (b) {
                    idKeyValue kv = slave.spawnArgs.MatchPrefix("triggerBlocked");
                    while (kv != null) {
                        idEntity ent = gameLocal.FindEntity(kv.GetValue().toString());
                        if (ent != null) {
                            ent.PostEventMS(EV_Activate, 0, moveMaster.GetActivator());
                        }
                        kv = slave.spawnArgs.MatchPrefix("triggerBlocked", kv);
                    }
                }
            }
        }

        public boolean IsBlocked() {
            return blocked;
        }

        public idEntity GetActivator() {
            return activatedBy.GetEntity();
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            physicsObj.WriteToSnapshot(msg);
            msg.WriteBits(etoi(moverState), 3);
            WriteBindToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            moverState_t oldMoverState = moverState;

            physicsObj.ReadFromSnapshot(msg);
            moverState = moverState_t.values()[msg.ReadBits(3)];
            ReadBindFromSnapshot(msg);

            if (msg.HasChanged()) {
                if (moverState != oldMoverState) {
                    UpdateMoverSound(moverState);
                }
                UpdateVisuals();
            }
        }

        public void SetPortalState(boolean open) {
            assert (areaPortal != 0);
            gameLocal.SetPortalState(areaPortal, (open ? PS_BLOCK_NONE : PS_BLOCK_ALL).ordinal());
        }


        /*
         ================
         idMover_Binary::MatchActivateTeam

         All entities in a mover team will move from pos1 to pos2
         in the same amount of time
         ================
         */
        protected void MatchActivateTeam(moverState_t newstate, int time) {
            idMover_Binary slave;

            for (slave = this; slave != null; slave = slave.activateChain) {
                slave.SetMoverState(newstate, time);
            }
        }

        /*
         ================
         idMover_Binary::JoinActivateTeam

         Set all entities in a mover team to be enabled
         ================
         */
        protected void JoinActivateTeam(idMover_Binary master) {
            this.activateChain = master.activateChain;
            master.activateChain = this;
        }

        protected void UpdateMoverSound(moverState_t state) {
            if (moveMaster == this) {
                switch (state) {
                    case MOVER_POS1:
                        break;
                    case MOVER_POS2:
                        break;
                    case MOVER_1TO2:
                        StartSound("snd_open", SND_CHANNEL_ANY, 0, false, null);
                        break;
                    case MOVER_2TO1:
                        StartSound("snd_close", SND_CHANNEL_ANY, 0, false, null);
                        break;
                }
            }
        }

        protected void SetMoverState(moverState_t newstate, int time) {
            idVec3 delta;

            moverState = newstate;
            move_thread = 0;

            UpdateMoverSound(newstate);

            stateStartTime = time;
            switch (moverState) {
                case MOVER_POS1: {
                    Signal(SIG_MOVER_POS1);
                    physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, time, 0, pos1, getVec3_origin(), getVec3_origin());
                    break;
                }
                case MOVER_POS2: {
                    Signal(SIG_MOVER_POS2);
                    physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, time, 0, pos2, getVec3_origin(), getVec3_origin());
                    break;
                }
                case MOVER_1TO2: {
                    Signal(SIG_MOVER_1TO2);
                    physicsObj.SetLinearExtrapolation(EXTRAPOLATION_LINEAR, time, duration, pos1, (pos2.oMinus(pos1)).oMultiply(1000.0f).oDivide(duration), getVec3_origin());
                    if (accelTime != 0 || decelTime != 0) {
                        physicsObj.SetLinearInterpolation(time, accelTime, decelTime, duration, pos1, pos2);
                    } else {
                        physicsObj.SetLinearInterpolation(0, 0, 0, 0, pos1, pos2);
                    }
                    break;
                }
                case MOVER_2TO1: {
                    Signal(SIG_MOVER_2TO1);
                    physicsObj.SetLinearExtrapolation(EXTRAPOLATION_LINEAR, time, duration, pos2, (pos1.oMinus(pos2)).oMultiply(1000.0f).oDivide(duration), getVec3_origin());
                    if (accelTime != 0 || decelTime != 0) {
                        physicsObj.SetLinearInterpolation(time, accelTime, decelTime, duration, pos2, pos1);
                    } else {
                        physicsObj.SetLinearInterpolation(0, 0, 0, 0, pos1, pos2);
                    }
                    break;
                }
            }
        }

        protected moverState_t GetMoverState() {
            return moverState;
        }

        protected void FindGuiTargets() {
            gameLocal.GetTargets(spawnArgs, guiTargets, "guiTarget");
        }

        /*
         ==============================
         idMover_Binary::SetGuiState

         key/val will be set to any renderEntity->gui's on the list
         ==============================
         */
        protected void SetGuiState(final String key, final String val) {
            int i;

            for (i = 0; i < guiTargets.Num(); i++) {
                idEntity ent = guiTargets.oGet(i).GetEntity();
                if (ent != null) {
                    for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                        if (ent.GetRenderEntity() != null && ent.GetRenderEntity().gui[ j] != null) {
                            ent.GetRenderEntity().gui[ j].SetStateString(key, val);
                            ent.GetRenderEntity().gui[ j].StateChanged(gameLocal.time, true);
                        }
                    }
                    ent.UpdateVisuals();
                }
            }
        }

        protected void Event_SetCallback() {
            if ((moverState == MOVER_1TO2) || (moverState == MOVER_2TO1)) {
                move_thread = idThread.CurrentThreadNum();
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        protected void Event_ReturnToPos1() {
            MatchActivateTeam(MOVER_2TO1, gameLocal.time);
        }

        protected void Event_Use_BinaryMover(idEventArg<idEntity> activator) {
            Use_BinaryMover(activator.value);
        }

        protected void Event_Reached_BinaryMover() {

            if (moverState == MOVER_1TO2) {
                // reached pos2
                idThread.ObjectMoveDone(move_thread, this);
                move_thread = 0;

                if (moveMaster == this) {
                    StartSound("snd_opened", SND_CHANNEL_ANY, 0, false, null);
                }

                SetMoverState(MOVER_POS2, gameLocal.time);

                SetGuiStates(guiBinaryMoverStates[MOVER_POS2.ordinal()]);

                UpdateBuddies(1);

                if (enabled && wait >= 0 && !spawnArgs.GetBool("toggle")) {
                    // return to pos1 after a delay
                    PostEventSec(EV_Mover_ReturnToPos1, wait);
                }

                // fire targets
                ActivateTargets(moveMaster.GetActivator());

                SetBlocked(false);
            } else if (moverState == MOVER_2TO1) {
                // reached pos1
                idThread.ObjectMoveDone(move_thread, this);
                move_thread = 0;

                SetMoverState(MOVER_POS1, gameLocal.time);

                SetGuiStates(guiBinaryMoverStates[MOVER_POS1.ordinal()]);

                UpdateBuddies(0);

                // close areaportals
                if (moveMaster == this) {
                    ProcessEvent(EV_Mover_ClosePortal);
                }

                if (enabled && wait >= 0 && spawnArgs.GetBool("continuous")) {
                    PostEventSec(EV_Activate, wait, this);
                }

                SetBlocked(false);
            } else {
                gameLocal.Error("Event_Reached_BinaryMover: bad moverState");
            }
        }

        protected void Event_MatchActivateTeam(idEventArg<moverState_t> newstate, idEventArg<Integer> time) {
            MatchActivateTeam(newstate.value, time.value);
        }

        /*
         ================
         idMover_Binary::Event_Enable

         Set all entities in a mover team to be enabled
         ================
         */
        protected void Event_Enable() {
            idMover_Binary slave;

            for (slave = moveMaster; slave != null; slave = slave.activateChain) {
                slave.Enable(true);//TODO: this is false in the original code.
            }
        }

        /*
         ================
         idMover_Binary::Event_Disable

         Set all entities in a mover team to be disabled
         ================
         */
        protected void Event_Disable() {
            idMover_Binary slave;

            for (slave = moveMaster; slave != null; slave = slave.activateChain) {
                slave.Enable(false);
            }
        }

        /*
         ================
         idMover_Binary::Event_OpenPortal

         Sets the portal associtated with this mover to be open
         ================
         */
        protected void Event_OpenPortal() {
            idMover_Binary slave;

            for (slave = moveMaster; slave != null; slave = slave.activateChain) {
                if (slave.areaPortal != 0) {
                    slave.SetPortalState(true);
                }
            }
        }


        /*
         ================
         idMover_Binary::Event_ClosePortal

         Sets the portal associtated with this mover to be closed
         ================
         */
        protected void Event_ClosePortal() {
            idMover_Binary slave;

            for (slave = moveMaster; slave != null; slave = slave.activateChain) {
                if (!slave.IsHidden()) {
                    if (slave.areaPortal != 0) {
                        slave.SetPortalState(false);
                    }
                }
            }
        }

        protected void Event_FindGuiTargets() {
            FindGuiTargets();
        }

        protected void Event_InitGuiTargets() {
            if (guiTargets.Num() != 0) {
                SetGuiState("movestate", guiBinaryMoverStates[MOVER_POS1.ordinal()]);
            }
        }

        /*
         ===============
         idMover_Binary::GetMovedir

         The editor only specifies a single value for angles (yaw),
         but we have special constants to generate an up or down direction.
         Angles will be cleared, because it is being used to represent a direction
         instead of an orientation.
         ===============
         */
        protected static void GetMovedir(float dir, idVec3 movedir) {
            if (dir == -1) {
                movedir.Set(0, 0, 1);
            } else if (dir == -2) {
                movedir.Set(0, 0, -1);
            } else {
                movedir.oSet(new idAngles(0, dir, 0).ToForward());
            }
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
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
     ===============================================================================

     idDoor

     A use can be triggered either by a touch function, by being shot, or by being
     targeted by another entity.

     ===============================================================================
     */
    public static class idDoor extends idMover_Binary {
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idMover_Binary.getEventCallBacks());
            eventCallbacks.put(EV_TeamBlocked, (eventCallback_t2<idDoor>) idDoor::Event_TeamBlocked);
            eventCallbacks.put(EV_PartBlocked, (eventCallback_t1<idDoor>) idDoor::Event_PartBlocked);
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idDoor>) idDoor::Event_Touch);
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idDoor>) idDoor::Event_Activate);
            eventCallbacks.put(EV_Door_StartOpen, (eventCallback_t0<idDoor>) idDoor::Event_StartOpen);
            eventCallbacks.put(EV_Door_SpawnDoorTrigger, (eventCallback_t0<idDoor>) idDoor::Event_SpawnDoorTrigger);
            eventCallbacks.put(EV_Door_SpawnSoundTrigger, (eventCallback_t0<idDoor>) idDoor::Event_SpawnSoundTrigger);
            eventCallbacks.put(EV_Door_Open, (eventCallback_t0<idDoor>) idDoor::Event_Open);
            eventCallbacks.put(EV_Door_Close, (eventCallback_t0<idDoor>) idDoor::Event_Close);
            eventCallbacks.put(EV_Door_Lock, (eventCallback_t1<idDoor>) idDoor::Event_Lock);
            eventCallbacks.put(EV_Door_IsOpen, (eventCallback_t0<idDoor>) idDoor::Event_IsOpen);
            eventCallbacks.put(EV_Door_IsLocked, (eventCallback_t0<idDoor>) idDoor::Event_Locked);
            eventCallbacks.put(EV_ReachedPos, (eventCallback_t0<idDoor>) idDoor::Event_Reached_BinaryMover);
            eventCallbacks.put(EV_SpectatorTouch, (eventCallback_t2<idDoor>) idDoor::Event_SpectatorTouch);
            eventCallbacks.put(EV_Mover_OpenPortal, (eventCallback_t0<idDoor>) idDoor::Event_OpenPortal);
            eventCallbacks.put(EV_Mover_ClosePortal, (eventCallback_t0<idDoor>) idDoor::Event_ClosePortal);
        }

        private float       triggersize;
        private boolean     crusher;
        private boolean     noTouch;
        private boolean     aas_area_closed;
        private idStr       buddyStr;
        private idClipModel trigger;
        private idClipModel sndTrigger;
        private int         nextSndTriggerTime;
        private idVec3      localTriggerOrigin;
        private idMat3      localTriggerAxis;
        private idStr       requires;
        private int         removeItem;
        private idStr       syncLock;
        private int         normalAxisIndex;        // door faces X or Y for spectator teleports
        private idDoor      companionDoor;
        //
        //

// public:
        // CLASS_PROTOTYPE( idDoor );
        public idDoor() {
            triggersize = 1.0f;
            crusher = false;
            noTouch = false;
            aas_area_closed = false;
            buddyStr = new idStr();
            trigger = null;
            sndTrigger = null;
            nextSndTriggerTime = 0;
            localTriggerOrigin = new idVec3();
            localTriggerAxis = idMat3.getMat3_identity();
            requires = new idStr();
            removeItem = 0;
            syncLock = new idStr();
            companionDoor = null;
            normalAxisIndex = 0;
        }
        // ~idDoor( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            idVec3 abs_movedir = new idVec3();
            float distance;
            idVec3 size;
            idVec3 moveDir = new idVec3();
            float[] dir = {0};
            float[] lip = {0};
            boolean[] start_open = {false};
            float[] time = {0};
            float[] speed = {0};

            // get the direction to move
            if (!spawnArgs.GetFloat("movedir", "0", dir)) {
                // no movedir, so angle defines movement direction and not orientation,
                // a la oldschool Quake
                SetAngles(getAng_zero());
                spawnArgs.GetFloat("angle", "0", dir);
            }
            GetMovedir(dir[0], moveDir);

            // default speed of 400
            spawnArgs.GetFloat("speed", "400", speed);

            // default wait of 2 seconds
            wait = spawnArgs.GetFloat("wait", "3");

            // default lip of 8 units
            spawnArgs.GetFloat("lip", "8", lip);

            // by default no damage
            damage = spawnArgs.GetFloat("damage", "0");

            // trigger size
            triggersize = spawnArgs.GetFloat("triggersize", "120");

            crusher = spawnArgs.GetBool("crusher", "0");
            spawnArgs.GetBool("start_open", "0", start_open);
            noTouch = spawnArgs.GetBool("no_touch", "0");

            // expects syncLock to be a door that must be closed before this door will open
            spawnArgs.GetString("syncLock", "", syncLock);

            spawnArgs.GetString("buddy", "", buddyStr);

            spawnArgs.GetString("requires", "", requires);
            removeItem = spawnArgs.GetInt("removeItem", "0");

            // ever separate piece of a door is considered solid when other team mates push entities
            fl.solidForTeam = true;

            // first position at start
            pos1 = GetPhysics().GetOrigin();

            // calculate second position
            abs_movedir.oSet(0, idMath.Fabs(moveDir.oGet(0)));
            abs_movedir.oSet(1, idMath.Fabs(moveDir.oGet(1)));
            abs_movedir.oSet(2, idMath.Fabs(moveDir.oGet(2)));
            size = GetPhysics().GetAbsBounds().oGet(1).oMinus(GetPhysics().GetAbsBounds().oGet(0));
            distance = (abs_movedir.oMultiply(size)) - lip[0];
            pos2 = pos1.oPlus(moveDir.oMultiply(distance));

            // if "start_open", reverse position 1 and 2
            if (start_open[0]) {
                // post it after EV_SpawnBind
                PostEventMS(EV_Door_StartOpen, 1);
            }

            if (spawnArgs.GetFloat("time", "1", time)) {
                InitTime(pos1, pos2, time[0], 0, 0);
            } else {
                InitSpeed(pos1, pos2, speed[0], 0, 0);
            }

            if (moveMaster == this) {
                if (health != 0) {
                    fl.takedamage = true;
                }
                if (noTouch || health != 0) {
                    // non touch/shoot doors
                    PostEventMS(EV_Mover_MatchTeam, 0, moverState, gameLocal.time);

                    final String sndtemp = spawnArgs.GetString("snd_locked");
                    if (spawnArgs.GetInt("locked") != 0 && sndtemp != null && !sndtemp.isEmpty()) {
                        PostEventMS(EV_Door_SpawnSoundTrigger, 0);
                    }
                } else {
                    // spawn trigger
                    PostEventMS(EV_Door_SpawnDoorTrigger, 0);
                }
            }

            // see if we are on an areaportal
            areaPortal = gameRenderWorld.FindPortal(GetPhysics().GetAbsBounds());
            if (!start_open[0]) {
                // start closed
                ProcessEvent(EV_Mover_ClosePortal);
            }

            int locked = spawnArgs.GetInt("locked");
            if (locked != 0) {
                // make sure all members of the team get locked
                PostEventMS(EV_Door_Lock, 0, locked);
            }

            if (spawnArgs.GetBool("continuous")) {
                PostEventSec(EV_Activate, spawnArgs.GetFloat("delay"), this);
            }

            // sounds have a habit of stuttering when portals close, so make them unoccluded
            refSound.parms.soundShaderFlags |= SSF_NO_OCCLUSION;

            companionDoor = null;

            enabled = true;
            blocked = false;
        }

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteFloat(triggersize);
            savefile.WriteBool(crusher);
            savefile.WriteBool(noTouch);
            savefile.WriteBool(aas_area_closed);
            savefile.WriteString(buddyStr);
            savefile.WriteInt(nextSndTriggerTime);

            savefile.WriteVec3(localTriggerOrigin);
            savefile.WriteMat3(localTriggerAxis);

            savefile.WriteString(requires);
            savefile.WriteInt(removeItem);
            savefile.WriteString(syncLock);
            savefile.WriteInt(normalAxisIndex);

            savefile.WriteClipModel(trigger);
            savefile.WriteClipModel(sndTrigger);

            savefile.WriteObject(companionDoor);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            triggersize = savefile.ReadFloat();
            crusher = savefile.ReadBool();
            noTouch = savefile.ReadBool();
            aas_area_closed = savefile.ReadBool();
            SetAASAreaState(aas_area_closed);
            savefile.ReadString(buddyStr);
            nextSndTriggerTime = savefile.ReadInt();

            savefile.ReadVec3(localTriggerOrigin);
            savefile.ReadMat3(localTriggerAxis);

            savefile.ReadString(requires);
            removeItem = savefile.ReadInt();
            savefile.ReadString(syncLock);
            normalAxisIndex = savefile.ReadInt();

            savefile.ReadClipModel(trigger);
            savefile.ReadClipModel(sndTrigger);

            savefile.ReadObject(/*reinterpret_cast<idClass *&>*/companionDoor);
        }

        @Override
        public void Think() {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            super.Think();

            if ((thinkFlags & TH_PHYSICS) != 0) {
                // update trigger position
                if (GetMasterPosition(masterOrigin, masterAxis)) {
                    if (trigger != null) {
                        trigger.Link(gameLocal.clip, this, 0, masterOrigin.oPlus(localTriggerOrigin.oMultiply(masterAxis)), localTriggerAxis.oMultiply(masterAxis));
                    }
                    if (sndTrigger != null) {
                        sndTrigger.Link(gameLocal.clip, this, 0, masterOrigin.oPlus(localTriggerOrigin.oMultiply(masterAxis)), localTriggerAxis.oMultiply(masterAxis));
                    }
                }
            }
        }

        @Override
        public void PreBind() {
            super.PreBind();
        }

        @Override
        public void PostBind() {
            super.PostBind();
            GetLocalTriggerPosition(trigger != null ? trigger : sndTrigger);
        }

        @Override
        public void Hide() {
            idMover_Binary slave;
            idMover_Binary master;
            idDoor slaveDoor;
            idDoor companion;

            master = GetMoveMaster();
            if (!this.equals(master)) {
                master.Hide();
            } else {
                for (slave = this; slave != null; slave = slave.GetActivateChain()) {
                    if (slave.IsType(idDoor.class)) {
                        slaveDoor = (idDoor) slave;
                        companion = slaveDoor.companionDoor;
                        if (companion != null && (!companion.equals(master)) && (!companion.GetMoveMaster().equals(master))) {
                            companion.Hide();
                        }
                        if (slaveDoor.trigger != null) {
                            slaveDoor.trigger.Disable();
                        }
                        if (slaveDoor.sndTrigger != null) {
                            slaveDoor.sndTrigger.Disable();
                        }
                        if (slaveDoor.areaPortal != 0) {
                            slaveDoor.SetPortalState(true);
                        }
                        slaveDoor.SetAASAreaState(false);
                    }
                    slave.GetPhysics().GetClipModel().Disable();
                    slave.Hide();
                }
            }
        }

        @Override
        public void Show() {
            idMover_Binary slave;
            idMover_Binary master;
            idDoor slaveDoor;
            idDoor companion;

            master = GetMoveMaster();
            if (!this.equals(master)) {
                master.Show();
            } else {
                for (slave = this; slave != null; slave = slave.GetActivateChain()) {
                    if (slave.IsType(idDoor.class)) {
                        slaveDoor = (idDoor) slave;
                        companion = slaveDoor.companionDoor;
                        if (companion != null && (!companion.equals(master)) && (!companion.GetMoveMaster().equals(master))) {
                            companion.Show();
                        }
                        if (slaveDoor.trigger != null) {
                            slaveDoor.trigger.Enable();
                        }
                        if (slaveDoor.sndTrigger != null) {
                            slaveDoor.sndTrigger.Enable();
                        }
                        if (slaveDoor.areaPortal != 0 && (slaveDoor.moverState == MOVER_POS1)) {
                            slaveDoor.SetPortalState(false);
                        }
                        slaveDoor.SetAASAreaState(IsLocked() != 0 || IsNoTouch());
                    }
                    slave.GetPhysics().GetClipModel().Enable();
                    slave.Show();
                }
            }
        }

        public boolean IsOpen() {
            return (moverState != MOVER_POS1);
        }

        public boolean IsNoTouch() {
            return noTouch;
        }

        public int IsLocked() {
            return spawnArgs.GetInt("locked");
        }

        public void Lock(int f) {
            idMover_Binary other;

            // lock all the doors on the team
            for (other = moveMaster; other != null; other = other.GetActivateChain()) {
                if (other.IsType(idDoor.class)) {
                    idDoor door = (idDoor) other;
                    if (other.equals(moveMaster)) {
                        if (door.sndTrigger == null) {
                            // in this case the sound trigger never got spawned
                            final String sndtemp = door.spawnArgs.GetString("snd_locked");
                            if (sndtemp != null && !sndtemp.isEmpty()) {
                                door.PostEventMS(EV_Door_SpawnSoundTrigger, 0);
                            }
                        }
                        if (0 == f && (door.spawnArgs.GetInt("locked") != 0)) {
                            door.StartSound("snd_unlocked", SND_CHANNEL_ANY, 0, false, null);
                        }
                    }
                    door.spawnArgs.SetInt("locked", f);
                    if ((f == 0) || (!IsHidden() && (door.moverState == MOVER_POS1))) {
                        door.SetAASAreaState(f != 0);
                    }
                }
            }

            if (f != 0) {
                Close();
            }
        }

        public void Use(idEntity other, idEntity activator) {
            if (gameLocal.RequirementMet(activator, requires, removeItem)) {
                if (syncLock.Length() != 0) {
                    idEntity sync = gameLocal.FindEntity(syncLock);
                    if (sync != null && sync.IsType(idDoor.class)) {
                        if (((idDoor) sync).IsOpen()) {
                            return;
                        }
                    }
                }
                ActivateTargets(activator);
                Use_BinaryMover(activator);
            }
        }

        public void Close() {
            GotoPosition1();
        }

        public void Open() {
            GotoPosition2();
        }

        public void SetCompanion(idDoor door) {
            companionDoor = door;
        }

        private void SetAASAreaState(boolean closed) {
            aas_area_closed = closed;
            gameLocal.SetAASAreaState(physicsObj.GetAbsBounds(), AREACONTENTS_CLUSTERPORTAL | AREACONTENTS_OBSTACLE, closed);
        }

        private void GetLocalTriggerPosition(final idClipModel trigger) {
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            if (NOT(trigger)) {
                return;
            }

            GetMasterPosition(origin, axis);
            localTriggerOrigin = (trigger.GetOrigin().oMinus(origin)).oMultiply(axis.Transpose());
            localTriggerAxis = trigger.GetAxis().oMultiply(axis.Transpose());
        }

        /*
         ======================
         idDoor::CalcTriggerBounds

         Calcs bounds for a trigger.
         ======================
         */
        private void CalcTriggerBounds(float size, idBounds bounds) {
            idMover_Binary other;
            int i;
            int best;

            // find the bounds of everything on the team
            bounds.oSet(GetPhysics().GetAbsBounds());

            fl.takedamage = true;
            for (other = activateChain; other != null; other = other.GetActivateChain()) {
                if (other.IsType(idDoor.class)) {
                    // find the bounds of everything on the team
                    bounds.AddBounds(other.GetPhysics().GetAbsBounds());

                    // set all of the slaves as shootable
                    other.fl.takedamage = true;
                }
            }

            // find the thinnest axis, which will be the one we expand
            best = 0;
            for (i = 1; i < 3; i++) {
                if (bounds.oGet(1, i) - bounds.oGet(0, i) < bounds.oGet(1, best) - bounds.oGet(0, best)) {
                    best = i;
                }
            }
            normalAxisIndex = best;
            bounds.oGet(0).oMinSet(best, size);;
            bounds.oGet(1).oPluSet(best, size);;
            bounds.oMinSet(GetPhysics().GetOrigin());
        }

        @Override
        protected void Event_Reached_BinaryMover() {
            if (moverState == MOVER_2TO1) {
                SetBlocked(false);
                idKeyValue kv = spawnArgs.MatchPrefix("triggerClosed");
                while (kv != null) {
                    idEntity ent = gameLocal.FindEntity(kv.GetValue().toString());
                    if (ent != null) {
                        ent.PostEventMS(EV_Activate, 0, moveMaster.GetActivator());
                    }
                    kv = spawnArgs.MatchPrefix("triggerClosed", kv);
                }
            } else if (moverState == MOVER_1TO2) {
                idKeyValue kv = spawnArgs.MatchPrefix("triggerOpened");
                while (kv != null) {
                    idEntity ent = gameLocal.FindEntity(kv.GetValue().toString());
                    if (ent != null) {
                        ent.PostEventMS(EV_Activate, 0, moveMaster.GetActivator());
                    }
                    kv = spawnArgs.MatchPrefix("triggerOpened", kv);
                }
            }
            super.Event_Reached_BinaryMover();
        }

        private void Event_TeamBlocked(idEventArg<idEntity> blockedEntity, idEventArg<idEntity> blockingEntity) {
            SetBlocked(true);

            if (crusher) {
                return;		// crushers don't reverse
            }

            // reverse direction
            Use_BinaryMover(moveMaster.GetActivator());

            if (companionDoor != null) {
                companionDoor.ProcessEvent(EV_TeamBlocked, blockedEntity.value, blockingEntity.value);
            }
        }

        private void Event_PartBlocked(idEventArg<idEntity> blockingEntity) {
            if (damage > 0.0f) {
                blockingEntity.value.Damage(this, this, getVec3_origin(), "damage_moverCrush", damage, INVALID_JOINT);
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> _trace) {
            idEntity other = _other.value;
            trace_s trace = _trace.value;
//            idVec3 contact, translate;
//            idVec3 planeaxis1, planeaxis2, normal;
//            idBounds bounds;

            if (!enabled) {
                return;
            }

            if (trigger != null && trace.c.id == trigger.GetId()) {
                if (!IsNoTouch() && 0 == IsLocked() && GetMoverState() != MOVER_1TO2) {
                    Use(this, other);
                }
            } else if (sndTrigger != null && trace.c.id == sndTrigger.GetId()) {
                if (other != null && other.IsType(idPlayer.class) && IsLocked() != 0 && gameLocal.time > nextSndTriggerTime) {
                    StartSound("snd_locked", SND_CHANNEL_ANY, 0, false, null);
                    nextSndTriggerTime = gameLocal.time + 10000;
                }
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            int old_lock;

            if (spawnArgs.GetInt("locked") != 0) {
                if (NOT(trigger)) {
                    PostEventMS(EV_Door_SpawnDoorTrigger, 0);
                }
                if (buddyStr.Length() != 0) {
                    idEntity buddy = gameLocal.FindEntity(buddyStr);
                    if (buddy != null) {
                        buddy.SetShaderParm(SHADERPARM_MODE, 1);
                        buddy.UpdateVisuals();
                    }
                }

                old_lock = spawnArgs.GetInt("locked");
                Lock(0);
                if (old_lock == 2) {
                    return;
                }
            }

            if (syncLock.Length() != 0) {
                idEntity sync = gameLocal.FindEntity(syncLock);
                if (sync != null && sync.IsType(idDoor.class)) {
                    if (((idDoor) sync).IsOpen()) {
                        return;
                    }
                }
            }

            ActivateTargets(activator.value);

            renderEntity.shaderParms[ SHADERPARM_MODE] = 1;
            UpdateVisuals();

            Use_BinaryMover(activator.value);
        }

        /*
         ======================
         idDoor::Event_StartOpen

         if "start_open", reverse position 1 and 2
         ======================
         */
        private void Event_StartOpen() {
            float[] time = {0};
            float[] speed = {0};

            // if "start_open", reverse position 1 and 2
            pos1 = pos2;
            pos2 = GetPhysics().GetOrigin();

            spawnArgs.GetFloat("speed", "400", speed);

            if (spawnArgs.GetFloat("time", "1", time)) {
                InitTime(pos1, pos2, time[0], 0, 0);
            } else {
                InitSpeed(pos1, pos2, speed[0], 0, 0);
            }
        }

        /*
         ======================
         idDoor::Event_SpawnDoorTrigger

         All of the parts of a door have been spawned, so create
         a trigger that encloses all of them.
         ======================
         */
        private void Event_SpawnDoorTrigger() {
            idBounds bounds = new idBounds();
            idMover_Binary other;
            boolean toggle;

            if (trigger != null) {
                // already have a trigger, so don't spawn a new one.
                return;
            }

            // check if any of the doors are marked as toggled
            toggle = false;
            for (other = moveMaster; other != null; other = other.GetActivateChain()) {
                if (other.IsType(idDoor.class) && other.spawnArgs.GetBool("toggle")) {
                    toggle = true;
                    break;
                }
            }

            if (toggle) {
                // mark them all as toggled
                for (other = moveMaster; other != null; other = other.GetActivateChain()) {
                    if (other.IsType(idDoor.class)) {
                        other.spawnArgs.Set("toggle", "1");
                    }
                }
                // don't spawn trigger
                return;
            }

            final String sndtemp = spawnArgs.GetString("snd_locked");
            if (spawnArgs.GetInt("locked") != 0 && sndtemp != null && !sndtemp.isEmpty()) {
                PostEventMS(EV_Door_SpawnSoundTrigger, 0);
            }

            CalcTriggerBounds(triggersize, bounds);

            // create a trigger clip model
            trigger = new idClipModel(new idTraceModel(bounds));
            trigger.Link(gameLocal.clip, this, 255, GetPhysics().GetOrigin(), getMat3_identity());
            trigger.SetContents(CONTENTS_TRIGGER);

            GetLocalTriggerPosition(trigger);

            MatchActivateTeam(moverState, gameLocal.time);
        }

        /*
         ======================
         idDoor::Event_SpawnSoundTrigger

         Spawn a sound trigger to activate locked sound if it exists.
         ======================
         */
        private void Event_SpawnSoundTrigger() {
            idBounds bounds = new idBounds();

            if (sndTrigger != null) {
                return;
            }

            CalcTriggerBounds(triggersize * 0.5f, bounds);

            // create a trigger clip model
            sndTrigger = new idClipModel(new idTraceModel(bounds));
            sndTrigger.Link(gameLocal.clip, this, 254, GetPhysics().GetOrigin(), getMat3_identity());
            sndTrigger.SetContents(CONTENTS_TRIGGER);

            GetLocalTriggerPosition(sndTrigger);
        }

        private void Event_Close() {
            Close();
        }

        private void Event_Open() {
            Open();
        }

        private void Event_Lock(idEventArg<Integer> f) {
            Lock(f.value);
        }

        private void Event_IsOpen() {
            int state;

            state = IsOpen() ? 1 : 0;
            idThread.ReturnFloat(state);
        }

        private void Event_Locked() {
            idThread.ReturnFloat(spawnArgs.GetInt("locked"));
        }

        private void Event_SpectatorTouch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            idEntity other = _other.value;
            idVec3 contact, translate, normal = new idVec3();
            idBounds bounds;
            idPlayer p;

            assert (other != null && other.IsType(idPlayer.class) && ((idPlayer) other).spectating);

            p = (idPlayer) other;
            // avoid flicker when stopping right at clip box boundaries
            if (p.lastSpectateTeleport > gameLocal.time - 1000) {
                return;
            }
            if (trigger != null && !IsOpen()) {
                // teleport to the other side, center to the middle of the trigger brush
                bounds = trigger.GetAbsBounds();
                contact = trace.value.endpos.oMinus(bounds.GetCenter());
                translate = bounds.GetCenter();
                normal.Zero();
                normal.oSet(normalAxisIndex, 1.0f);
                if (normal.oMultiply(contact) > 0) {
                    translate.oPluSet(normalAxisIndex, (bounds.oGet(0, normalAxisIndex) - translate.oGet(normalAxisIndex)) * 0.5f);
                } else {
                    translate.oPluSet(normalAxisIndex, (bounds.oGet(1, normalAxisIndex) - translate.oGet(normalAxisIndex)) * 0.5f);
                }
                p.SetOrigin(translate);
                p.lastSpectateTeleport = gameLocal.time;
            }
        }

        /*
         ================
         idDoor::Event_OpenPortal

         Sets the portal associtated with this door to be open
         ================
         */
        @Override
        protected void Event_OpenPortal() {
            idMover_Binary slave;
            idDoor slaveDoor;

            for (slave = this; slave != null; slave = slave.GetActivateChain()) {
                if (slave.IsType(idDoor.class)) {
                    slaveDoor = (idDoor) slave;
                    if (slaveDoor.areaPortal != 0) {
                        slaveDoor.SetPortalState(true);
                    }
                    slaveDoor.SetAASAreaState(false);
                }
            }
        }

        /*
         ================
         idDoor::Event_ClosePortal

         Sets the portal associtated with this door to be closed
         ================
         */
        @Override
        protected void Event_ClosePortal() {
            idMover_Binary slave;
            idDoor slaveDoor;

            for (slave = this; slave != null; slave = slave.GetActivateChain()) {
                if (!slave.IsHidden()) {
                    if (slave.IsType(idDoor.class)) {
                        slaveDoor = (idDoor) slave;
                        if (slaveDoor.areaPortal != 0) {
                            slaveDoor.SetPortalState(false);
                        }
                        slaveDoor.SetAASAreaState(IsLocked() != 0 || IsNoTouch());
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

    };

    /*
     ===============================================================================

     idPlat

     ===============================================================================
     */
    public static class idPlat extends idMover_Binary {
        // CLASS_PROTOTYPE( idPlat );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idMover_Binary.getEventCallBacks());
            eventCallbacks.put(EV_Touch, (eventCallback_t2<idPlat>) idPlat::Event_Touch);
            eventCallbacks.put(EV_TeamBlocked, (eventCallback_t2<idPlat>) idPlat::Event_TeamBlocked);
            eventCallbacks.put(EV_PartBlocked, (eventCallback_t1<idPlat>) idPlat::Event_PartBlocked);
        }

        private idClipModel trigger;
        private idVec3 localTriggerOrigin;
        private idMat3 localTriggerAxis;
        //
        //

        public idPlat() {
            trigger = null;
            localTriggerOrigin.Zero();
            localTriggerAxis.Identity();
        }
        // ~idPlat( void );

        @Override
        public void Spawn() {
            float[] lip = {0};
            float[] height = {0};
            float[] time = {0};
            float[] speed = {0};
            float[] accel = {0};
            float[] decel = {0};
            boolean[] noTouch = {false};

            spawnArgs.GetFloat("speed", "100", speed);
            damage = spawnArgs.GetFloat("damage", "0");
            wait = spawnArgs.GetFloat("wait", "1");
            spawnArgs.GetFloat("lip", "8", lip);
            spawnArgs.GetFloat("accel_time", "0.25", accel);
            spawnArgs.GetFloat("decel_time", "0.25", decel);

            // create second position
            if (!spawnArgs.GetFloat("height", "0", height)) {
                height[0] = (GetPhysics().GetBounds().oGet(1, 2) - GetPhysics().GetBounds().oGet(0, 2)) - lip[0];
            }

            spawnArgs.GetBool("no_touch", "0", noTouch);

            // pos1 is the rest (bottom) position, pos2 is the top
            pos2 = GetPhysics().GetOrigin();
            pos1 = pos2;
            pos1.oMinSet(2, height[0]);

            if (spawnArgs.GetFloat("time", "1", time)) {
                InitTime(pos1, pos2, time[0], accel[0], decel[0]);
            } else {
                InitSpeed(pos1, pos2, speed[0], accel[0], decel[0]);
            }

            SetMoverState(MOVER_POS1, gameLocal.time);
            UpdateVisuals();

            // spawn the trigger if one hasn't been custom made
            if (!noTouch[0]) {
                // spawn trigger
                SpawnPlatTrigger(pos1);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteClipModel(trigger);
            savefile.WriteVec3(localTriggerOrigin);
            savefile.WriteMat3(localTriggerAxis);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadClipModel(trigger);
            savefile.ReadVec3(localTriggerOrigin);
            savefile.ReadMat3(localTriggerAxis);
        }

        @Override
        public void Think() {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            super.Think();

            if ((thinkFlags & TH_PHYSICS) != 0) {
                // update trigger position
                if (GetMasterPosition(masterOrigin, masterAxis)) {
                    if (trigger != null) {
                        trigger.Link(gameLocal.clip, this, 0, masterOrigin.oPlus(localTriggerOrigin.oMultiply(masterAxis)), localTriggerAxis.oMultiply(masterAxis));
                    }
                }
            }
        }

        @Override
        public void PreBind() {
            super.PreBind();
        }

        @Override
        public void PostBind() {
            super.PostBind();
            GetLocalTriggerPosition(trigger);
        }

        private void GetLocalTriggerPosition(final idClipModel trigger) {
            idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            if (NOT(trigger)) {
                return;
            }

            GetMasterPosition(origin, axis);
            localTriggerOrigin = (trigger.GetOrigin().oMinus(origin)).oMultiply(axis.Transpose());
            localTriggerAxis = trigger.GetAxis().oMultiply(axis.Transpose());
        }

        private void SpawnPlatTrigger(idVec3 pos) {
            idBounds bounds;
            idVec3 tmin = new idVec3();
            idVec3 tmax = new idVec3();

            // the middle trigger will be a thin trigger just
            // above the starting position
            bounds = GetPhysics().GetBounds();

            tmin.oSet(0, bounds.oGet(0, 0) + 33);
            tmin.oSet(1, bounds.oGet(0, 1) + 33);
            tmin.oSet(2, bounds.oGet(0, 2));

            tmax.oSet(0, bounds.oGet(1, 0) - 33);
            tmax.oSet(1, bounds.oGet(1, 1) - 33);
            tmax.oSet(2, bounds.oGet(1, 2) + 8);

            if (tmax.oGet(0) <= tmin.oGet(0)) {
                tmin.oSet(0, (bounds.oGet(0, 0) + bounds.oGet(1, 0)) * 0.5f);
                tmax.oSet(0, tmin.oGet(0) + 1);
            }
            if (tmax.oGet(1) <= tmin.oGet(1)) {
                tmin.oSet(0, (bounds.oGet(0, 1) + bounds.oGet(1, 1)) * 0.5f);
                tmax.oSet(0, tmin.oGet(1) + 1);
            }

            trigger = new idClipModel(new idTraceModel(new idBounds(tmin, tmax)));
            trigger.Link(gameLocal.clip, this, 255, GetPhysics().GetOrigin(), getMat3_identity());
            trigger.SetContents(CONTENTS_TRIGGER);
        }

        private void Event_TeamBlocked(idEventArg<idEntity> blockedEntity, idEventArg<idEntity> blockingEntity) {
            // reverse direction
            Use_BinaryMover(activatedBy.GetEntity());
        }

        private void Event_PartBlocked(idEventArg<idEntity> blockingEntity) {
            if (damage > 0) {
                blockingEntity.value.Damage(this, this, getVec3_origin(), "damage_moverCrush", damage, INVALID_JOINT);
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            idEntity other = _other.value;
            if (!other.IsType(idPlayer.class)) {
                return;
            }

            if ((GetMoverState() == MOVER_POS1) && trigger != null && (trace.value.c.id == trigger.GetId()) && (other.health > 0)) {
                Use_BinaryMover(other);
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

    /*
     ===============================================================================

     Special periodic movers.

     ===============================================================================
     */
    /*
     ===============================================================================

     idMover_Periodic

     ===============================================================================
     */
    public static class idMover_Periodic extends idEntity {
        // CLASS_PROTOTYPE( idMover_Periodic );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_TeamBlocked, (eventCallback_t2<idMover_Periodic>) idMover_Periodic::Event_TeamBlocked);
            eventCallbacks.put(EV_PartBlocked, (eventCallback_t1<idMover_Periodic>) idMover_Periodic::Event_PartBlocked);
        }

        protected idPhysics_Parametric physicsObj;
        protected float[] damage = {0};
        //
        //

        public idMover_Periodic() {
            damage[0] = 0;
            physicsObj = new idPhysics_Parametric();
            fl.neverDormant = false;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            spawnArgs.GetFloat("damage", "0", damage);
            if (!spawnArgs.GetBool("solid", "1")) {
                GetPhysics().SetContents(0);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(damage[0]);
            savefile.WriteStaticObject(physicsObj);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadFloat(damage);
            savefile.ReadStaticObject(physicsObj);
            RestorePhysics(physicsObj);
        }

        @Override
        public void Think() {
            // if we are completely closed off from the player, don't do anything at all
            if (CheckDormant()) {
                return;
            }

            RunPhysics();
            Present();
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            physicsObj.WriteToSnapshot(msg);
            WriteBindToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            physicsObj.ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);

            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        protected void Event_TeamBlocked(idEventArg<idEntity> blockedEntity, idEventArg<idEntity> blockingEntity) {
        }

        protected void Event_PartBlocked(idEventArg<idEntity> blockingEntity) {
            if (damage[0] > 0) {
                blockingEntity.value.Damage(this, this, getVec3_origin(), "damage_moverCrush", damage[0], INVALID_JOINT);
            }
        }

        @Override
        public idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
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
     ===============================================================================

     idRotater

     ===============================================================================
     */
    public static class idRotater extends idMover_Periodic {
        // CLASS_PROTOTYPE( idRotater );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idMover_Periodic.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idRotater>) idRotater::Event_Activate);
        }

        private idEntityPtr<idEntity> activatedBy;
        //
        //

        public idRotater() {
            this.activatedBy = new idEntityPtr<>().oSet(this);
        }

        @Override
        public void Spawn() {
            super.Spawn();

            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(GetPhysics().GetAxis());
            physicsObj.SetClipMask(MASK_SOLID);
            if (!spawnArgs.GetBool("nopush")) {
                physicsObj.SetPusher(0);
            }
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, gameLocal.time, 0, GetPhysics().GetOrigin(), getVec3_origin(), getVec3_origin());
            physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, GetPhysics().GetAxis().ToAngles(), getAng_zero(), getAng_zero());
            SetPhysics(physicsObj);

            if (spawnArgs.GetBool("start_on")) {
                ProcessEvent(EV_Activate, this);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            activatedBy.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            activatedBy.Restore(savefile);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            float[] speed = {0};
            boolean[] x_axis = {false};
            boolean[] y_axis = {false};
            idAngles delta = new idAngles();

            activatedBy.oSet(activator.value);

            delta.Zero();

            if (!spawnArgs.GetBool("rotate")) {
                spawnArgs.Set("rotate", "1");
                spawnArgs.GetFloat("speed", "100", speed);
                spawnArgs.GetBool("x_axis", "0", x_axis);
                spawnArgs.GetBool("y_axis", "0", y_axis);

                // set the axis of rotation
                if (x_axis[0]) {
                    delta.oSet(2, speed[0]);
                } else if (y_axis[0]) {
                    delta.oSet(0, speed[0]);
                } else {
                    delta.oSet(1, speed[0]);
                }
            } else {
                spawnArgs.Set("rotate", "0");
            }

            physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, physicsObj.GetAxis().ToAngles(), delta, getAng_zero());
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
     ===============================================================================

     idBobber

     ===============================================================================
     */
    public static class idBobber extends idMover_Periodic {
        // CLASS_PROTOTYPE( idBobber );

        public idBobber() {
        }

        @Override
        public void Spawn() {
            float[] speed = {0};
            float[] height = {0};
            float[] phase = {0};
            boolean[] x_axis = {false};
            boolean[] y_axis = {false};
            idVec3 delta;

            spawnArgs.GetFloat("speed", "4", speed);
            spawnArgs.GetFloat("height", "32", height);
            spawnArgs.GetFloat("phase", "0", phase);
            spawnArgs.GetBool("x_axis", "0", x_axis);
            spawnArgs.GetBool("y_axis", "0", y_axis);

            // set the axis of bobbing
            delta = getVec3_origin();
            if (x_axis[0]) {
                delta.oSet(0, height[0]);
            } else if (y_axis[0]) {
                delta.oSet(1, height[0]);
            } else {
                delta.oSet(2, height[0]);
            }

            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(GetPhysics().GetAxis());
            physicsObj.SetClipMask(MASK_SOLID);
            if (!spawnArgs.GetBool("nopush")) {
                physicsObj.SetPusher(0);
            }
            physicsObj.SetLinearExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), (int) (phase[0] * 1000), (int) (speed[0] * 500), GetPhysics().GetOrigin(), delta.oMultiply(2.0f), getVec3_origin());
            SetPhysics(physicsObj);
        }
    };

    /*
     ===============================================================================

     idPendulum

     ===============================================================================
     */
    public static class idPendulum extends idMover_Periodic {
        // CLASS_PROTOTYPE( idPendulum );

//        public idPendulum() {//TODO:remove default constructor override
//        }
        @Override
        public void Spawn() {
            super.Spawn();

            float[] speed = {0};
            float[] freq = {0};
            float[] length = {0};
            float[] phase = {0};

            spawnArgs.GetFloat("speed", "30", speed);
            spawnArgs.GetFloat("phase", "0", phase);

            if (spawnArgs.GetFloat("freq", "", freq)) {
                if (freq[0] <= 0.0f) {
                    gameLocal.Error("Invalid frequency on entity '%s'", GetName());
                }
            } else {
                // find pendulum length
                length[0] = idMath.Fabs(GetPhysics().GetBounds().oGet(0, 2));
                if (length[0] < 8) {
                    length[0] = 8;
                }

                freq[0] = 1 / (idMath.TWO_PI) * idMath.Sqrt(g_gravity.GetFloat() / (3 * length[0]));
            }

            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(GetPhysics().GetAxis());
            physicsObj.SetClipMask(MASK_SOLID);
            if (!spawnArgs.GetBool("nopush")) {
                physicsObj.SetPusher(0);
            }
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, GetPhysics().GetOrigin(), getVec3_origin(), getVec3_origin());
            physicsObj.SetAngularExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), (int) (phase[0] * 1000), (int) (500 / freq[0]), GetPhysics().GetAxis().ToAngles(), new idAngles(0, 0, speed[0] * 2.0f), getAng_zero());
            SetPhysics(physicsObj);
        }
    };


    /*
     ===============================================================================

     idRiser

     ===============================================================================
     */
    public static class idRiser extends idMover_Periodic {
        // CLASS_PROTOTYPE( idRiser );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idMover_Periodic.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idRiser>) idRiser::Event_Activate);
        }

//public	idRiser( ){}
        @Override
        public void Spawn() {
            physicsObj.SetSelf(this);
            physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            physicsObj.SetOrigin(GetPhysics().GetOrigin());
            physicsObj.SetAxis(GetPhysics().GetAxis());

            physicsObj.SetClipMask(MASK_SOLID);
            if (!spawnArgs.GetBool("solid", "1")) {
                physicsObj.SetContents(0);
            }
            if (!spawnArgs.GetBool("nopush")) {
                physicsObj.SetPusher(0);
            }
            physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, GetPhysics().GetOrigin(), getVec3_origin(), getVec3_origin());
            SetPhysics(physicsObj);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {

            if (!IsHidden() && spawnArgs.GetBool("hide")) {
                Hide();
            } else {
                Show();
                float[] time = {0};
                float[] height = {0};
                idVec3 delta;

                spawnArgs.GetFloat("time", "4", time);
                spawnArgs.GetFloat("height", "32", height);

                delta = getVec3_origin();
                delta.oSet(2, height[0]);

                physicsObj.SetLinearExtrapolation(EXTRAPOLATION_LINEAR, gameLocal.time, (int) (time[0] * 1000), physicsObj.GetOrigin(), delta, getVec3_origin());
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
