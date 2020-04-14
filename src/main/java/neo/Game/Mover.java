package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.EV_Touch;
import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.signalNum_t.SIG_MOVER_1TO2;
import static neo.Game.Entity.signalNum_t.SIG_MOVER_2TO1;
import static neo.Game.Entity.signalNum_t.SIG_MOVER_POS1;
import static neo.Game.Entity.signalNum_t.SIG_MOVER_POS2;
import static neo.Game.GameSys.SysCvar.g_debugMover;
import static neo.Game.GameSys.SysCvar.g_gravity;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_BODY2;
import static neo.Game.Game_local.gameState_t.GAMESTATE_STARTUP;
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
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Angles.getAng_zero;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_ACCELLINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_DECELLINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_DECELSINE;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_LINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NONE;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NOSTOP;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;

import java.util.HashMap;
import java.util.Map;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.Game_local.idGameLocal;
import neo.Game.Player.idPlayer;
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
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import neo.Game.Physics.Physics_Parametric.idPhysics_Parametric;
import neo.Game.Script.Script_Thread.idThread;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Curve.idCurve_Spline;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

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
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
        private final rotationState_t rot;
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
		private final idList<idEntityPtr<idEntity>> guiTargets;

		{
	        @SuppressWarnings("unchecked")
			final
	        idList<idEntityPtr<idEntity>> guiTargets1 = new idList<idEntityPtr<idEntity>>((Class<idEntityPtr<idEntity>>) new idEntityPtr<>().getClass());
	        this.guiTargets = guiTargets1;
		}
        
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
        }

        protected enum moverCommand_t {

            MOVER_NONE,
            MOVER_ROTATING,
            MOVER_MOVING,
            MOVER_SPLINE
        }
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
        }

        protected static class rotationState_t {

            moveStage_t stage;
            int acceleration;
            int movetime;
            int deceleration;
            idAngles rot;
        }
        //
        protected idPhysics_Parametric physicsObj;
        //
        //

        public idMover() {
//	memset( &move, 0, sizeof( move ) );
            this.move = new moveState_t();
//	memset( &rot, 0, sizeof( rot ) );
            this.rot = new rotationState_t();
            this.move_thread = 0;
            this.rotate_thread = 0;
            this.dest_angles = new idAngles();
            this.angle_delta = new idAngles();
            this.dest_position = new idVec3();
            this.move_delta = new idVec3();
            this.move_speed = 0.0f;
            this.move_time = 0;
            this.deceltime = 0;
            this.acceltime = 0;
            this.stopRotation = false;
            this.useSplineAngles = true;
            this.lastCommand = MOVER_NONE;
            this.damage = 0.0f;
            this.areaPortal = 0;
            this.fl.networkSync = true;
            this.physicsObj = new idPhysics_Parametric();
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            final float[] damage = {0};

            this.move_thread = 0;
            this.rotate_thread = 0;
            this.stopRotation = false;
            this.lastCommand = MOVER_NONE;

            this.acceltime = (int) (1000 * this.spawnArgs.GetFloat("accel_time", "0"));
            this.deceltime = (int) (1000 * this.spawnArgs.GetFloat("decel_time", "0"));
            this.move_time = (int) (1000 * this.spawnArgs.GetFloat("move_time", "1"));	// safe default value
            this.move_speed = this.spawnArgs.GetFloat("move_speed", "0");

            this.spawnArgs.GetFloat("damage", "0", damage);
            this.damage = damage[0];

            this.dest_position = GetPhysics().GetOrigin();
            this.dest_angles = GetPhysics().GetAxis().ToAngles();

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetClipMask(MASK_SOLID);
            if (!this.spawnArgs.GetBool("solid", "1")) {
                this.physicsObj.SetContents(0);
            }
            if ((null == this.renderEntity.hModel) || !this.spawnArgs.GetBool("nopush")) {
                this.physicsObj.SetPusher(0);
            }
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.dest_position, getVec3_origin(), getVec3_origin());
            this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.dest_angles, getAng_zero(), getAng_zero());
            SetPhysics(this.physicsObj);

            // see if we are on an areaportal
            this.areaPortal = gameRenderWorld.FindPortal(GetPhysics().GetAbsBounds());

            if (this.spawnArgs.MatchPrefix("guiTarget") != null) {
                if (gameLocal.GameState() == GAMESTATE_STARTUP) {
                    PostEventMS(EV_FindGuiTargets, 0);
                } else {
                    // not during spawn, so it's ok to get the targets
                    FindGuiTargets();
                }
            }

            this.health = this.spawnArgs.GetInt("health");
            if (this.health != 0) {
                this.fl.takedamage = true;
            }

        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteStaticObject(this.physicsObj);

            savefile.WriteInt(etoi(this.move.stage));
            savefile.WriteInt(this.move.acceleration);
            savefile.WriteInt(this.move.movetime);
            savefile.WriteInt(this.move.deceleration);
            savefile.WriteVec3(this.move.dir);

            savefile.WriteInt(etoi(this.rot.stage));
            savefile.WriteInt(this.rot.acceleration);
            savefile.WriteInt(this.rot.movetime);
            savefile.WriteInt(this.rot.deceleration);
            savefile.WriteFloat(this.rot.rot.pitch);
            savefile.WriteFloat(this.rot.rot.yaw);
            savefile.WriteFloat(this.rot.rot.roll);

            savefile.WriteInt(this.move_thread);
            savefile.WriteInt(this.rotate_thread);

            savefile.WriteAngles(this.dest_angles);
            savefile.WriteAngles(this.angle_delta);
            savefile.WriteVec3(this.dest_position);
            savefile.WriteVec3(this.move_delta);

            savefile.WriteFloat(this.move_speed);
            savefile.WriteInt(this.move_time);
            savefile.WriteInt(this.deceltime);
            savefile.WriteInt(this.acceltime);
            savefile.WriteBool(this.stopRotation);
            savefile.WriteBool(this.useSplineAngles);
            savefile.WriteInt(etoi(this.lastCommand));
            savefile.WriteFloat(this.damage);

            savefile.WriteInt(this.areaPortal);
            if (this.areaPortal > 0) {
                savefile.WriteInt(gameRenderWorld.GetPortalState(this.areaPortal));
            }

            savefile.WriteInt(this.guiTargets.Num());
            for (i = 0; i < this.guiTargets.Num(); i++) {
                this.guiTargets.oGet(i).Save(savefile);
            }

            if ((this.splineEnt.GetEntity() != null) && (this.splineEnt.GetEntity().GetSpline() != null)) {
                final idCurve_Spline<idVec3> spline = this.physicsObj.GetSpline();

                savefile.WriteBool(true);
                this.splineEnt.Save(savefile);
                savefile.WriteInt((int) spline.GetTime(0));
                savefile.WriteInt((int) (spline.GetTime(spline.GetNumValues() - 1) - spline.GetTime(0)));
                savefile.WriteInt(this.physicsObj.GetSplineAcceleration());
                savefile.WriteInt(this.physicsObj.GetSplineDeceleration());
                savefile.WriteInt(btoi(this.physicsObj.UsingSplineAngles()));

            } else {
                savefile.WriteBool(false);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            final int[] num = {0};
            final boolean[] hasSpline = {false};

            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);

            this.move.stage = moveStage_t.values()[savefile.ReadInt()];
            this.move.acceleration = savefile.ReadInt();
            this.move.movetime = savefile.ReadInt();
            this.move.deceleration = savefile.ReadInt();
            savefile.ReadVec3(this.move.dir);

            this.rot.stage = moveStage_t.values()[savefile.ReadInt()];
            this.rot.acceleration = savefile.ReadInt();
            this.rot.movetime = savefile.ReadInt();
            this.rot.deceleration = savefile.ReadInt();
            this.rot.rot.pitch = savefile.ReadFloat();
            this.rot.rot.yaw = savefile.ReadFloat();
            this.rot.rot.roll = savefile.ReadFloat();

            this.move_thread = savefile.ReadInt();
            this.rotate_thread = savefile.ReadInt();

            savefile.ReadAngles(this.dest_angles);
            savefile.ReadAngles(this.angle_delta);
            savefile.ReadVec3(this.dest_position);
            savefile.ReadVec3(this.move_delta);

            this.move_speed = savefile.ReadFloat();
            this.move_time = savefile.ReadInt();
            this.deceltime = savefile.ReadInt();
            this.acceltime = savefile.ReadInt();
            this.stopRotation = savefile.ReadBool();
            this.useSplineAngles = savefile.ReadBool();
            this.lastCommand = moverCommand_t.values()[savefile.ReadInt()];
            this.damage = savefile.ReadFloat();

            this.areaPortal = savefile.ReadInt();
            if (this.areaPortal > 0) {
                final int[] portalState = {0};
                savefile.ReadInt(portalState);
                gameLocal.SetPortalState(this.areaPortal, portalState[0]);
            }

            this.guiTargets.Clear();
            savefile.ReadInt(num);
            this.guiTargets.SetNum(num[0]);
            for (i = 0; i < num[0]; i++) {
                this.guiTargets.oGet(i).Restore(savefile);
            }

            savefile.ReadBool(hasSpline);
            if (hasSpline[0]) {
                final int[] starttime = {0};
                final int[] totaltime = {0};
                final int[] accel = {0};
                final int[] decel = {0};
                final int[] useAngles = {0};

                this.splineEnt.Restore(savefile);
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
            this.fl.takedamage = false;
            ActivateTargets(this);
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            this.physicsObj.WriteToSnapshot(msg);
            msg.WriteBits(etoi(this.move.stage), 3);
            msg.WriteBits(etoi(this.rot.stage), 3);
            WriteBindToSnapshot(msg);
            WriteGUIToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            final moveStage_t oldMoveStage = this.move.stage;
            final moveStage_t oldRotStage = this.rot.stage;

            this.physicsObj.ReadFromSnapshot(msg);
            this.move.stage = moveStage_t.values()[msg.ReadBits(3)];
            this.rot.stage = moveStage_t.values()[msg.ReadBits(3)];
            ReadBindFromSnapshot(msg);
            ReadGUIFromSnapshot(msg);

            if (msg.HasChanged()) {
                if (this.move.stage != oldMoveStage) {
                    UpdateMoveSound(oldMoveStage);
                }
                if (this.rot.stage != oldRotStage) {
                    UpdateRotationSound(oldRotStage);
                }
                UpdateVisuals();
            }
        }

        @Override
        public void Hide() {
            super.Hide();
            this.physicsObj.SetContents(0);
        }

        @Override
        public void Show() {
            super.Show();
            if (this.spawnArgs.GetBool("solid", "1")) {
                this.physicsObj.SetContents(CONTENTS_SOLID);
            }
            SetPhysics(this.physicsObj);
        }

        public void SetPortalState(boolean open) {
            assert (this.areaPortal != 0);
            gameLocal.SetPortalState(this.areaPortal, (open ? PS_BLOCK_NONE : PS_BLOCK_ALL).ordinal());
        }


        /*
         ================
         idMover::Event_OpenPortal

         Sets the portal associtated with this mover to be open
         ================
         */
        protected void Event_OpenPortal() {
            if (this.areaPortal != 0) {
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
            if (this.areaPortal != 0) {
                SetPortalState(false);
            }
        }

        protected void Event_PartBlocked(idEventArg<idEntity> blockingEntity) {
            if (this.damage > 0.0f) {
                blockingEntity.value.Damage(this, this, getVec3_origin(), "damage_moverCrush", this.damage, INVALID_JOINT);
            }
            if (g_debugMover.GetBool()) {
                gameLocal.Printf("%d: '%s' blocked by '%s'\n", gameLocal.time, this.name, blockingEntity.value.name);
            }
        }

        protected void MoveToPos(final idVec3 pos) {
            this.dest_position = GetLocalCoordinates(pos);
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
            if (this.guiTargets.Num() != 0) {
                SetGuiState("movestate", state);
            }
            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                if (this.renderEntity.gui[i] != null) {
                    this.renderEntity.gui[i].SetStateString("movestate", state);
                    this.renderEntity.gui[i].StateChanged(gameLocal.time, true);
                }
            }
        }

        protected void FindGuiTargets() {
            gameLocal.GetTargets(this.spawnArgs, this.guiTargets, "guiTarget");
        }

        /*
         ==============================
         idMover::SetGuiState

         key/val will be set to any renderEntity->gui's on the list
         ==============================
         */
        protected void SetGuiState(final String key, final String val) {
            gameLocal.Printf("Setting %s to %s\n", key, val);
            for (int i = 0; i < this.guiTargets.Num(); i++) {
                final idEntity ent = this.guiTargets.oGet(i).GetEntity();
                if (ent != null) {
                    for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                        if ((ent.GetRenderEntity() != null) && (ent.GetRenderEntity().gui[ j] != null)) {
                            ent.GetRenderEntity().gui[ j].SetStateString(key, val);
                            ent.GetRenderEntity().gui[ j].StateChanged(gameLocal.time, true);
                        }
                    }
                    ent.UpdateVisuals();
                }
            }
        }

        protected void DoneMoving() {

            if (this.lastCommand != MOVER_SPLINE) {
                // set our final position so that we get rid of any numerical inaccuracy
                this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.dest_position, getVec3_origin(), getVec3_origin());
            }

            this.lastCommand = MOVER_NONE;
            idThread.ObjectMoveDone(this.move_thread, this);
            this.move_thread = 0;

            StopSound(etoi(SND_CHANNEL_BODY), false);
        }

        protected void DoneRotating() {
            this.lastCommand = MOVER_NONE;
            idThread.ObjectMoveDone(this.rotate_thread, this);
            this.rotate_thread = 0;

            StopSound(etoi(SND_CHANNEL_BODY), false);
        }

        protected void BeginMove(idThread thread) {
            moveStage_t stage;
            final idVec3 org = new idVec3();
            float dist;
            float acceldist;
            int totalacceltime;
            int at;
            int dt;

            this.lastCommand = MOVER_MOVING;
            this.move_thread = 0;

            this.physicsObj.GetLocalOrigin(org);

            this.move_delta = this.dest_position.oMinus(org);
            if (this.move_delta.Compare(getVec3_zero())) {
                DoneMoving();
                return;
            }

            // scale times up to whole physics frames
            at = idPhysics.SnapTimeToPhysicsFrame(this.acceltime);
            this.move_time += at - this.acceltime;
            this.acceltime = at;
            dt = idPhysics.SnapTimeToPhysicsFrame(this.deceltime);
            this.move_time += dt - this.deceltime;
            this.deceltime = dt;

            // if we're moving at a specific speed, we need to calculate the move time
            if (this.move_speed != 0) {
                dist = this.move_delta.Length();

                totalacceltime = this.acceltime + this.deceltime;

                // calculate the distance we'll move during acceleration and deceleration
                acceldist = totalacceltime * 0.5f * 0.001f * this.move_speed;
                if (acceldist >= dist) {
                    // going too slow for this distance to move at a constant speed
                    this.move_time = totalacceltime;
                } else {
                    // calculate move time taking acceleration into account
                    this.move_time = (int) (totalacceltime + ((1000.0f * (dist - acceldist)) / this.move_speed));
                }
            }

            // scale time up to a whole physics frames
            this.move_time = idPhysics.SnapTimeToPhysicsFrame(this.move_time);

            if (this.acceltime != 0) {
                stage = ACCELERATION_STAGE;
            } else if (this.move_time <= this.deceltime) {
                stage = DECELERATION_STAGE;
            } else {
                stage = LINEAR_STAGE;
            }

            at = this.acceltime;
            dt = this.deceltime;

            if ((at + dt) > this.move_time) {
                // there's no real correct way to handle this, so we just scale
                // the times to fit into the move time in the same proportions
                at = idPhysics.SnapTimeToPhysicsFrame((at * this.move_time) / (at + dt));
                dt = this.move_time - at;
            }

            this.move_delta = this.move_delta.oMultiply(1000.0f / (this.move_time - ((at + dt) * 0.5f)));

            this.move.stage = stage;
            this.move.acceleration = at;
            this.move.movetime = this.move_time - at - dt;
            this.move.deceleration = dt;
            this.move.dir = new idVec3(this.move_delta);

            ProcessEvent(EV_ReachedPos);
        }

        protected void BeginRotation(idThread thread, boolean stopwhendone) {
            moveStage_t stage;
            final idAngles ang = new idAngles();
            int at;
            int dt;

            this.lastCommand = MOVER_ROTATING;
            this.rotate_thread = 0;

            // rotation always uses move_time so that if a move was started before the rotation,
            // the rotation will take the same amount of time as the move.  If no move has been
            // started and no time is set, the rotation takes 1 second.
            if (0 == this.move_time) {
                this.move_time = 1;
            }

            this.physicsObj.GetLocalAngles(ang);
            this.angle_delta = this.dest_angles.oMinus(ang);
            if (this.angle_delta.equals(getAng_zero())) {
                // set our final angles so that we get rid of any numerical inaccuracy
                this.dest_angles.Normalize360();
                this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.dest_angles, getAng_zero(), getAng_zero());
                this.stopRotation = false;
                DoneRotating();
                return;
            }

            // scale times up to whole physics frames
            at = idPhysics.SnapTimeToPhysicsFrame(this.acceltime);
            this.move_time += at - this.acceltime;
            this.acceltime = at;
            dt = idPhysics.SnapTimeToPhysicsFrame(this.deceltime);
            this.move_time += dt - this.deceltime;
            this.deceltime = dt;
            this.move_time = idPhysics.SnapTimeToPhysicsFrame(this.move_time);

            if (this.acceltime != 0) {
                stage = ACCELERATION_STAGE;
            } else if (this.move_time <= this.deceltime) {
                stage = DECELERATION_STAGE;
            } else {
                stage = LINEAR_STAGE;
            }

            at = this.acceltime;
            dt = this.deceltime;

            if ((at + dt) > this.move_time) {
                // there's no real correct way to handle this, so we just scale
                // the times to fit into the move time in the same proportions
                at = idPhysics.SnapTimeToPhysicsFrame((at * this.move_time) / (at + dt));
                dt = this.move_time - at;
            }

            this.angle_delta = this.angle_delta.oMultiply(1000.0f / (this.move_time - ((at + dt) * 0.5f)));

            this.stopRotation = stopwhendone || (dt != 0);

            this.rot.stage = stage;
            this.rot.acceleration = at;
            this.rot.movetime = this.move_time - at - dt;
            this.rot.deceleration = dt;
            this.rot.rot = this.angle_delta;

            ProcessEvent(EV_ReachedAng);
        }

        private void VectorForDir(float dir, idVec3 vec) {
            final idAngles ang = new idAngles();

            switch ((int) dir) {
                case DIR_UP:
                    vec.Set(0, 0, 1);
                    break;

                case DIR_DOWN:
                    vec.Set(0, 0, -1);
                    break;

                case DIR_LEFT:
                    this.physicsObj.GetLocalAngles(ang);
                    ang.pitch = 0;
                    ang.roll = 0;
                    ang.yaw += 90;
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_RIGHT:
                    this.physicsObj.GetLocalAngles(ang);
                    ang.pitch = 0;
                    ang.roll = 0;
                    ang.yaw -= 90;
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_FORWARD:
                    this.physicsObj.GetLocalAngles(ang);
                    ang.pitch = 0;
                    ang.roll = 0;
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_BACK:
                    this.physicsObj.GetLocalAngles(ang);
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
                    this.physicsObj.GetLocalAngles(ang);
                    ang.ToVectors(null, vec);
                    vec.oMulSet(-1);
                    break;

                case DIR_REL_RIGHT:
                    this.physicsObj.GetLocalAngles(ang);
                    ang.ToVectors(null, vec);
                    break;

                case DIR_REL_FORWARD:
                    this.physicsObj.GetLocalAngles(ang);
                    vec.oSet(ang.ToForward());
                    break;

                case DIR_REL_BACK:
                    this.physicsObj.GetLocalAngles(ang);
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
            if ((this.lastCommand.equals(MOVER_ROTATING)) && (0 == this.rotate_thread)) {
                this.lastCommand = MOVER_NONE;
                this.rotate_thread = idThread.CurrentThreadNum();
                idThread.ReturnInt(true);
            } else if ((this.lastCommand.equals(MOVER_MOVING) || this.lastCommand.equals(MOVER_SPLINE)) && (0 == this.move_thread)) {
                this.lastCommand = MOVER_NONE;
                this.move_thread = idThread.CurrentThreadNum();
                idThread.ReturnInt(true);
            } else {
                idThread.ReturnInt(false);
            }
        }

        private void Event_TeamBlocked(idEventArg<idEntity> blockedPart, idEventArg<idEntity> blockingEntity) {
            if (g_debugMover.GetBool()) {
                gameLocal.Printf("%d: '%s' stopped due to team member '%s' blocked by '%s'\n", gameLocal.time, this.name, blockedPart.value.name, blockingEntity.value.name);
            }
        }

        private void Event_StopMoving() {
            this.physicsObj.GetLocalOrigin(this.dest_position);
            DoneMoving();
        }

        private void Event_StopRotating() {
            this.physicsObj.GetLocalAngles(this.dest_angles);
            this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.dest_angles, getAng_zero(), getAng_zero());
            DoneRotating();
        }

        private void Event_UpdateMove() {
            final idVec3 org = new idVec3();

            this.physicsObj.GetLocalOrigin(org);

            UpdateMoveSound(this.move.stage);

            switch (this.move.stage) {
                case ACCELERATION_STAGE: {
                    this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_ACCELLINEAR, gameLocal.time, this.move.acceleration, org, this.move.dir, getVec3_origin());
                    if (this.move.movetime > 0) {
                        this.move.stage = LINEAR_STAGE;
                    } else if (this.move.deceleration > 0) {
                        this.move.stage = DECELERATION_STAGE;
                    } else {
                        this.move.stage = FINISHED_STAGE;
                    }
                    break;
                }
                case LINEAR_STAGE: {
                    this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_LINEAR, gameLocal.time, this.move.movetime, org, this.move.dir, getVec3_origin());
                    if (this.move.deceleration != 0) {
                        this.move.stage = DECELERATION_STAGE;
                    } else {
                        this.move.stage = FINISHED_STAGE;
                    }
                    break;
                }
                case DECELERATION_STAGE: {
                    this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_DECELLINEAR, gameLocal.time, this.move.deceleration, org, this.move.dir, getVec3_origin());
                    this.move.stage = FINISHED_STAGE;
                    break;
                }
                case FINISHED_STAGE: {
                    if (g_debugMover.GetBool()) {
                        gameLocal.Printf("%d: '%s' move done\n", gameLocal.time, this.name);
                    }
                    DoneMoving();
                    break;
                }
            }
        }

        private void Event_UpdateRotation() {
            final idAngles ang = new idAngles();

            this.physicsObj.GetLocalAngles(ang);

            UpdateRotationSound(this.rot.stage);

            switch (this.rot.stage) {
                case ACCELERATION_STAGE: {
                    this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_ACCELLINEAR, gameLocal.time, this.rot.acceleration, ang, this.rot.rot, getAng_zero());
                    if (this.rot.movetime > 0) {
                        this.rot.stage = LINEAR_STAGE;
                    } else if (this.rot.deceleration > 0) {
                        this.rot.stage = DECELERATION_STAGE;
                    } else {
                        this.rot.stage = FINISHED_STAGE;
                    }
                    break;
                }
                case LINEAR_STAGE: {
                    if (!this.stopRotation && (0 == this.rot.deceleration)) {
                        this.physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, this.rot.movetime, ang, this.rot.rot, getAng_zero());
                    } else {
                        this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_LINEAR, gameLocal.time, this.rot.movetime, ang, this.rot.rot, getAng_zero());
                    }

                    if (this.rot.deceleration != 0) {
                        this.rot.stage = DECELERATION_STAGE;
                    } else {
                        this.rot.stage = FINISHED_STAGE;
                    }
                    break;
                }
                case DECELERATION_STAGE: {
                    this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_DECELLINEAR, gameLocal.time, this.rot.deceleration, ang, this.rot.rot, getAng_zero());
                    this.rot.stage = FINISHED_STAGE;
                    break;
                }
                case FINISHED_STAGE: {
                    this.lastCommand = MOVER_NONE;
                    if (this.stopRotation) {
                        // set our final angles so that we get rid of any numerical inaccuracy
                        this.dest_angles.Normalize360();
                        this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.dest_angles, getAng_zero(), getAng_zero());
                        this.stopRotation = false;
                    } else if (this.physicsObj.GetAngularExtrapolationType() == EXTRAPOLATION_ACCELLINEAR) {
                        // keep our angular velocity constant
                        this.physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, ang, this.rot.rot, getAng_zero());
                    }

                    if (g_debugMover.GetBool()) {
                        gameLocal.Printf("%d: '%s' rotation done\n", gameLocal.time, this.name);
                    }

                    DoneRotating();
                    break;
                }
            }
        }

        private void Event_SetMoveSpeed(idEventArg<Float> speed) {
            if (speed.value <= 0) {
                idGameLocal.Error("Cannot set speed less than or equal to 0.");
            }

            this.move_speed = speed.value;
            this.move_time = 0;			// move_time is calculated for each move when move_speed is non-0
        }

        private void Event_SetMoveTime(idEventArg<Float> time) {
            if (time.value <= 0) {
                idGameLocal.Error("Cannot set time less than or equal to 0.");
            }

            this.move_speed = 0;
            this.move_time = (int) SEC2MS(time.value);
        }

        private void Event_SetDecelerationTime(idEventArg<Float> time) {
            if (time.value < 0) {
                idGameLocal.Error("Cannot set deceleration time less than 0.");
            }

            this.deceltime = (int) SEC2MS(time.value);
        }

        private void Event_SetAccellerationTime(idEventArg<Float> time) {
            if (time.value < 0) {
                idGameLocal.Error("Cannot set acceleration time less than 0.");
            }

            this.acceltime = (int) SEC2MS(time.value);
        }

        private void Event_MoveTo(idEventArg<idEntity> ent) {
            if (null == ent.value) {
                gameLocal.Warning("Entity not found");
            }

            this.dest_position = GetLocalCoordinates(ent.value.GetPhysics().GetOrigin());
            BeginMove(idThread.CurrentThread());
        }

        private void Event_MoveToPos(idEventArg<idVec3> pos) {
            this.dest_position = GetLocalCoordinates(pos.value);
            BeginMove(null);
        }

        private void Event_MoveDir(idEventArg<Float> angle, idEventArg<Float> distance) {
            final idVec3 dir = new idVec3();
            final idVec3 org = new idVec3();

            this.physicsObj.GetLocalOrigin(org);
            VectorForDir(angle.value, dir);
            this.dest_position = org.oPlus(dir.oMultiply(distance.value));

            BeginMove(idThread.CurrentThread());
        }

        private void Event_MoveAccelerateTo(idEventArg<Float> speed, idEventArg<Float> time) {
            float v;
            final idVec3 org = new idVec3();
			idVec3 dir;
            int at;

            if (time.value < 0) {
                idGameLocal.Error("idMover::Event_MoveAccelerateTo: cannot set acceleration time less than 0.");
            }

            dir = this.physicsObj.GetLinearVelocity();
            v = dir.Normalize();

            // if not moving already
            if (v == 0.0f) {
                idGameLocal.Error("idMover::Event_MoveAccelerateTo: not moving.");
            }

            // if already moving faster than the desired speed
            if (v >= speed.value) {
                return;
            }

            at = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(time.value));

            this.lastCommand = MOVER_MOVING;

            this.physicsObj.GetLocalOrigin(org);

            this.move.stage = ACCELERATION_STAGE;
            this.move.acceleration = at;
            this.move.movetime = 0;
            this.move.deceleration = 0;

            StartSound("snd_accel", SND_CHANNEL_BODY2, 0, false, null);
            StartSound("snd_move", SND_CHANNEL_BODY, 0, false, null);
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_ACCELLINEAR, gameLocal.time, this.move.acceleration, org, dir.oMultiply(speed.value - v), dir.oMultiply(v));
        }

        private void Event_MoveDecelerateTo(idEventArg<Float> speed, idEventArg<Float> time) {
            float v;
            final idVec3 org = new idVec3();
			idVec3 dir;
            int dt;

            if (time.value < 0) {
                idGameLocal.Error("idMover::Event_MoveDecelerateTo: cannot set deceleration time less than 0.");
            }

            dir = this.physicsObj.GetLinearVelocity();
            v = dir.Normalize();

            // if not moving already
            if (v == 0.0f) {
                idGameLocal.Error("idMover::Event_MoveDecelerateTo: not moving.");
            }

            // if already moving slower than the desired speed
            if (v <= speed.value) {
                return;
            }

            dt = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(time.value));

            this.lastCommand = MOVER_MOVING;

            this.physicsObj.GetLocalOrigin(org);

            this.move.stage = DECELERATION_STAGE;
            this.move.acceleration = 0;
            this.move.movetime = 0;
            this.move.deceleration = dt;

            StartSound("snd_decel", SND_CHANNEL_BODY2, 0, false, null);
            StartSound("snd_move", SND_CHANNEL_BODY, 0, false, null);
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_DECELLINEAR, gameLocal.time, this.move.deceleration, org, dir.oMultiply(v - speed.value), dir.oMultiply(speed.value));
        }

        private void Event_RotateDownTo(idEventArg<Integer> _axis, idEventArg<Float> angle) {
            final int axis = _axis.value;
            final idAngles ang = new idAngles();

            if ((axis < 0) || (axis > 2)) {
                idGameLocal.Error("Invalid axis");
            }

            this.physicsObj.GetLocalAngles(ang);

            this.dest_angles.oSet(axis, angle.value);
            if (this.dest_angles.oGet(axis) > ang.oGet(axis)) {
                this.dest_angles.oMinSet(axis, 360);
            }

            BeginRotation(idThread.CurrentThread(), true);
        }

        private void Event_RotateUpTo(idEventArg<Integer> _axis, idEventArg<Float> angle) {
            final int axis = _axis.value;
            final idAngles ang = new idAngles();

            if ((axis < 0) || (axis > 2)) {
                idGameLocal.Error("Invalid axis");
            }

            this.physicsObj.GetLocalAngles(ang);

            this.dest_angles.oSet(axis, angle.value);
            if (this.dest_angles.oGet(axis) < ang.oGet(axis)) {
                this.dest_angles.oPluSet(axis, 360);
            }

            BeginRotation(idThread.CurrentThread(), true);
        }

        private void Event_RotateTo(idEventArg<idAngles> angles) {
            this.dest_angles.oSet(angles.value);
            BeginRotation(idThread.CurrentThread(), true);
        }

        private void Event_Rotate(idEventArg<idVec3> angles) {
            final idAngles ang = new idAngles();

            if (this.rotate_thread != 0) {
                DoneRotating();
            }

            this.physicsObj.GetLocalAngles(ang);
            this.dest_angles = ang.oPlus(angles.value.oMultiply(this.move_time - ((this.acceltime + this.deceltime) / 2)).oMultiply(0.001f));

            BeginRotation(idThread.CurrentThread(), false);
        }

        private void Event_RotateOnce(idEventArg<idVec3> angles) {
            final idAngles ang = new idAngles();

            if (this.rotate_thread != 0) {
                DoneRotating();
            }

            this.physicsObj.GetLocalAngles(ang);
            this.dest_angles = ang.oPlus(angles.value);

            BeginRotation(idThread.CurrentThread(), true);
        }

        private void Event_Bob(idEventArg<Float> speed, idEventArg<Float> phase, idEventArg<idVec3> depth) {
            final idVec3 org = new idVec3();

            this.physicsObj.GetLocalOrigin(org);
            this.physicsObj.SetLinearExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), (int) (speed.value * 1000 * phase.value),
                    (int) (speed.value * 500), org, depth.value.oMultiply(2.0f), getVec3_origin());
        }

        private void Event_Sway(idEventArg<Float> speed, idEventArg<Float> phase, idEventArg<idVec3> _depth) {
            final idAngles depth = new idAngles(_depth.value);
            final idAngles ang = new idAngles();
			idAngles angSpeed;
            float duration;

            this.physicsObj.GetLocalAngles(ang);
            assert (speed.value > 0.0f);
            duration = idMath.Sqrt((depth.oGet(0) * depth.oGet(0)) + (depth.oGet(1) * depth.oGet(1)) + (depth.oGet(2) * depth.oGet(2))) / speed.value;
            angSpeed = depth.oDivide(duration * idMath.SQRT_1OVER2);
            this.physicsObj.SetAngularExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), (int) (duration * 1000.0f * phase.value), (int) (duration * 1000.0f), ang, angSpeed, getAng_zero());
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
            this.useSplineAngles = true;
        }

        private void Event_DisableSplineAngles() {
            this.useSplineAngles = false;
        }

        private void Event_RemoveInitialSplineAngles() {
            idCurve_Spline<idVec3> spline;
            idAngles ang;

            spline = this.physicsObj.GetSpline();
            if (null == spline) {
                return;
            }
            ang = spline.GetCurrentFirstDerivative(0).ToAngles();
            this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, ang.oNegative(), getAng_zero(), getAng_zero());
        }

        private void Event_StartSpline(idEventArg<idEntity> _splineEntity) {
            final idEntity splineEntity = _splineEntity.value;
            idCurve_Spline<idVec3> spline;

            if (null == splineEntity) {
                return;
            }

            // Needed for savegames
            this.splineEnt = new idEntityPtr<>(splineEntity);

            spline = splineEntity.GetSpline();
            if (null == spline) {
                return;
            }

            this.lastCommand = MOVER_SPLINE;
            this.move_thread = 0;

            if ((this.acceltime + this.deceltime) > this.move_time) {
                this.acceltime = this.move_time / 2;
                this.deceltime = this.move_time - this.acceltime;
            }
            this.move.stage = FINISHED_STAGE;
            this.move.acceleration = this.acceltime;
            this.move.movetime = this.move_time;
            this.move.deceleration = this.deceltime;

            spline.MakeUniform(this.move_time);
            spline.ShiftTime(gameLocal.time - spline.GetTime(0));

            this.physicsObj.SetSpline(spline, this.move.acceleration, this.move.deceleration, this.useSplineAngles);
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.dest_position, getVec3_origin(), getVec3_origin());
        }

        private void Event_StopSpline() {
            this.physicsObj.SetSpline(null, 0, 0, this.useSplineAngles);
            this.splineEnt = null;
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            Show();
            Event_StartSpline(idEventArg.toArg(this));
        }

        private void Event_PostRestore(idEventArg<Integer> start, idEventArg<Integer> total, idEventArg<Integer> accel,
                                       idEventArg<Integer> decel, idEventArg<Integer> useSplineAng) {
            idCurve_Spline<idVec3> spline;

            final idEntity splineEntity = this.splineEnt.GetEntity();
            if (null == splineEntity) {
                // We should never get this event if splineEnt is invalid
                common.Warning("Invalid spline entity during restore\n");
                return;
            }

            spline = splineEntity.GetSpline();

            spline.MakeUniform(total.value);
            spline.ShiftTime(start.value - spline.GetTime(0));

            this.physicsObj.SetSpline(spline, accel.value, decel.value, (useSplineAng.value != 0));
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.dest_position, getVec3_origin(), getVec3_origin());
        }

        private void Event_IsMoving() {
            if (this.physicsObj.GetLinearExtrapolationType() == EXTRAPOLATION_NONE) {
                idThread.ReturnInt(false);
            } else {
                idThread.ReturnInt(true);
            }
        }

        private void Event_IsRotating() {
            if (this.physicsObj.GetAngularExtrapolationType() == EXTRAPOLATION_NONE) {
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

    }

    /*
     ===============================================================================

     idSplinePath, holds a spline path to be used by an idMover

     ===============================================================================
     */
    public static class idSplinePath extends idEntity {
//	CLASS_PROTOTYPE( idSplinePath );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

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
    }

    public static class floorInfo_s {

        idVec3 pos;
        idStr door;
        int floor;
    }

    /*
     ===============================================================================

     idElevator

     ===============================================================================
     */
    public static class idElevator extends idMover {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
        }
        //
        private elevatorState_t state;
        private final idList<floorInfo_s> floorInfo;
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
            this.state = INIT;
            this.floorInfo = new idList<>();
            this.currentFloor = 0;
            this.pendingFloor = 0;
            this.lastFloor = 0;
            this.controlsDisabled = false;
            this.lastTouchTime = 0;
            this.returnFloor = 0;
            this.returnTime = 0;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            idStr str;
            int len1;

            this.lastFloor = 0;
            this.currentFloor = 0;
            this.pendingFloor = this.spawnArgs.GetInt("floor", "1");
            SetGuiStates((this.pendingFloor == 1) ? guiBinaryMoverStates[0] : guiBinaryMoverStates[1]);

            this.returnTime = this.spawnArgs.GetFloat("returnTime");
            this.returnFloor = this.spawnArgs.GetInt("returnFloor");

            len1 = "floorPos_".length();
            idKeyValue kv = this.spawnArgs.MatchPrefix("floorPos_", null);
            while (kv != null) {
                str = kv.GetKey().Right(kv.GetKey().Length() - len1);
                final floorInfo_s fi = new floorInfo_s();
                fi.floor = Integer.parseInt(str.getData());
                fi.door = new idStr(this.spawnArgs.GetString(va("floorDoor_%d", fi.floor)));
                fi.pos = this.spawnArgs.GetVector(kv.GetKey().getData());
                this.floorInfo.Append(fi);
                kv = this.spawnArgs.MatchPrefix("floorPos_", kv);
            }
            this.lastTouchTime = 0;
            this.state = INIT;
            BecomeActive(TH_THINK | TH_PHYSICS);
            PostEventMS(EV_Mover_InitGuiTargets, 0);
            this.controlsDisabled = false;
        }

        @Override
        public void Save(idSaveGame savefile) {
            int i;

            savefile.WriteInt(etoi(this.state));

            savefile.WriteInt(this.floorInfo.Num());
            for (i = 0; i < this.floorInfo.Num(); i++) {
                savefile.WriteVec3(this.floorInfo.oGet(i).pos);
                savefile.WriteString(this.floorInfo.oGet(i).door.getData());
                savefile.WriteInt(this.floorInfo.oGet(i).floor);
            }

            savefile.WriteInt(this.currentFloor);
            savefile.WriteInt(this.pendingFloor);
            savefile.WriteInt(this.lastFloor);
            savefile.WriteBool(this.controlsDisabled);
            savefile.WriteFloat(this.returnTime);
            savefile.WriteInt(this.returnFloor);
            savefile.WriteInt(this.lastTouchTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int num;

            this.state = elevatorState_t.values()[savefile.ReadInt()];

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                final floorInfo_s floor = new floorInfo_s();

                savefile.ReadVec3(floor.pos);
                savefile.ReadString(floor.door);
                floor.floor = savefile.ReadInt();

                this.floorInfo.Append(floor);
            }

            this.currentFloor = savefile.ReadInt();
            this.pendingFloor = savefile.ReadInt();
            this.lastFloor = savefile.ReadInt();
            this.controlsDisabled = savefile.ReadBool();
            this.returnTime = savefile.ReadFloat();
            this.returnFloor = savefile.ReadInt();
            this.lastTouchTime = savefile.ReadInt();
        }

        @Override
        public boolean HandleSingleGuiCommand(idEntity entityGui, idLexer src) {
            final idToken token = new idToken();

            if (this.controlsDisabled) {
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
                    final int newFloor = Integer.parseInt(token.getData());
                    if (newFloor == this.currentFloor) {
                        // open currentFloor and interior doors
                        OpenInnerDoor();
                        OpenFloorDoor(this.currentFloor);
                    } else {
                        final idDoor door = GetDoor(this.spawnArgs.GetString("innerdoor"));
                        if ((door != null) && door.IsOpen()) {
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
            final floorInfo_s fi = GetFloorInfo(floor.value);
            if (fi != null) {
                final idDoor door = GetDoor(this.spawnArgs.GetString("innerdoor"));
                if (door != null) {
                    if (door.IsBlocked() || door.IsOpen()) {
                        PostEventSec(EV_GotoFloor, 0.5f, floor);
                        return;
                    }
                }
                DisableAllDoors();
                CloseAllDoors();
                this.state = WAITING_ON_DOORS;
                this.pendingFloor = floor.value;
            }
        }

        public floorInfo_s GetFloorInfo(int floor) {
            for (int i = 0; i < this.floorInfo.Num(); i++) {
                if (this.floorInfo.oGet(i).floor == floor) {
                    return this.floorInfo.oGet(i);
                }
            }
            return null;
        }

        @Override
        protected void DoneMoving() {
            super.DoneMoving();
            EnableProperDoors();
            idKeyValue kv = this.spawnArgs.MatchPrefix("statusGui");
            while (kv != null) {
                final idEntity ent = gameLocal.FindEntity(kv.GetValue().getData());
                if (ent != null) {
                    for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                        if ((ent.GetRenderEntity() != null) && (ent.GetRenderEntity().gui[ j] != null)) {
                            ent.GetRenderEntity().gui[ j].SetStateString("floor", va("%d", this.currentFloor));
                            ent.GetRenderEntity().gui[ j].StateChanged(gameLocal.time, true);
                        }
                    }
                    ent.UpdateVisuals();
                }
                kv = this.spawnArgs.MatchPrefix("statusGui", kv);
            }
            if (this.spawnArgs.GetInt("pauseOnFloor", "-1") == this.currentFloor) {
                PostEventSec(EV_PostArrival, this.spawnArgs.GetFloat("pauseTime"));
            } else {
                Event_PostFloorArrival();
            }
        }

        @Override
        protected void BeginMove(idThread thread /*= NULL*/) {
            this.controlsDisabled = true;
            CloseAllDoors();
            DisableAllDoors();
            idKeyValue kv = this.spawnArgs.MatchPrefix("statusGui");
            while (kv != null) {
                final idEntity ent = gameLocal.FindEntity(kv.GetValue().getData());
                if (ent != null) {
                    for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                        if ((ent.GetRenderEntity() != null) && (ent.GetRenderEntity().gui[ j] != null)) {
                            ent.GetRenderEntity().gui[ j].SetStateString("floor", "");
                            ent.GetRenderEntity().gui[ j].StateChanged(gameLocal.time, true);
                        }
                    }
                    ent.UpdateVisuals();
                }
                kv = this.spawnArgs.MatchPrefix("statusGui", kv);
            }
            SetGuiStates((this.pendingFloor == 1) ? guiBinaryMoverStates[3] : guiBinaryMoverStates[2]);
            super.BeginMove(thread);
        }
//
//        protected void SpawnTrigger(final idVec3 pos);
//
//        protected void GetLocalTriggerPosition();
//

        protected void Event_Touch(idEventArg<idEntity> other, idEventArg<trace_s> trace) {

            if (gameLocal.time < (this.lastTouchTime + 2000)) {
                return;
            }

            if (!other.value.IsType(idPlayer.class)) {
                return;
            }

            this.lastTouchTime = gameLocal.time;

            if ((this.thinkFlags & TH_PHYSICS) != 0) {
                return;
            }

            final int triggerFloor = this.spawnArgs.GetInt("triggerFloor");
            if (this.spawnArgs.GetBool("trigger") && (triggerFloor != this.currentFloor)) {
                PostEventSec(EV_GotoFloor, 0.25f, triggerFloor);
            }
        }

        private idDoor GetDoor(final String name) {
            idEntity ent;
            idEntity master;
            idDoor doorEnt;

            doorEnt = null;
            if ((name != null) && !name.isEmpty()) {
                ent = gameLocal.FindEntity(name);
                if ((ent != null) && ent.IsType(idDoor.class)) {
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
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();
            final idDoor doorEnt = GetDoor(this.spawnArgs.GetString("innerdoor"));
            if (this.state == INIT) {
                this.state = IDLE;
                if (doorEnt != null) {
                    doorEnt.BindTeam(this);
                    doorEnt.spawnArgs.Set("snd_open", "");
                    doorEnt.spawnArgs.Set("snd_close", "");
                    doorEnt.spawnArgs.Set("snd_opened", "");
                }
                for (int i = 0; i < this.floorInfo.Num(); i++) {
                    final idDoor door = GetDoor(this.floorInfo.oGet(i).door.getData());
                    if (door != null) {
                        door.SetCompanion(doorEnt);
                    }
                }

                Event_GotoFloor(idEventArg.toArg(this.pendingFloor));
                DisableAllDoors();
                SetGuiStates((this.pendingFloor == 1) ? guiBinaryMoverStates[0] : guiBinaryMoverStates[1]);
            } else if (this.state == WAITING_ON_DOORS) {
                if (doorEnt != null) {
                    this.state = doorEnt.IsOpen() ? WAITING_ON_DOORS : IDLE;
                } else {
                    this.state = IDLE;
                }
                if (this.state == IDLE) {
                    this.lastFloor = this.currentFloor;
                    this.currentFloor = this.pendingFloor;
                    final floorInfo_s fi = GetFloorInfo(this.currentFloor);
                    if (fi != null) {
                        MoveToPos(fi.pos);
                    }
                }
            }
            RunPhysics();
            Present();
        }

        private void OpenInnerDoor() {
            final idDoor door = GetDoor(this.spawnArgs.GetString("innerdoor"));
            if (door != null) {
                door.Open();
            }
        }

        private void OpenFloorDoor(int floor) {
            final floorInfo_s fi = GetFloorInfo(floor);
            if (fi != null) {
                final idDoor door = GetDoor(fi.door.getData());
                if (door != null) {
                    door.Open();
                }
            }
        }

        private void CloseAllDoors() {
            idDoor door = GetDoor(this.spawnArgs.GetString("innerdoor"));
            if (door != null) {
                door.Close();
            }
            for (int i = 0; i < this.floorInfo.Num(); i++) {
                door = GetDoor(this.floorInfo.oGet(i).door.getData());
                if (door != null) {
                    door.Close();
                }
            }
        }

        private void DisableAllDoors() {
            idDoor door = GetDoor(this.spawnArgs.GetString("innerdoor"));
            if (door != null) {
                door.Enable(false);
            }
            for (int i = 0; i < this.floorInfo.Num(); i++) {
                door = GetDoor(this.floorInfo.oGet(i).door.getData());
                if (door != null) {
                    door.Enable(false);
                }
            }
        }

        private void EnableProperDoors() {
            idDoor door = GetDoor(this.spawnArgs.GetString("innerdoor"));
            if (door != null) {
                door.Enable(true);
            }
            for (int i = 0; i < this.floorInfo.Num(); i++) {
                if (this.floorInfo.oGet(i).floor == this.currentFloor) {
                    door = GetDoor(this.floorInfo.oGet(i).door.getData());
                    if (door != null) {
                        door.Enable(true);
                        break;
                    }
                }
            }
        }

        private void Event_TeamBlocked(idEventArg<idEntity> blockedEntity, idEventArg<idEntity> blockingEntity) {
            if (blockedEntity.value == this) {
                Event_GotoFloor(idEventArg.toArg(this.lastFloor));
            } else if ((blockedEntity != null) && blockedEntity.value.IsType(idDoor.class)) {
                // open the inner doors if one is blocked
                final idDoor blocked = (idDoor) blockedEntity.value;
                final idDoor door = GetDoor(this.spawnArgs.GetString("innerdoor"));
                if ((door != null) && (blocked.GetMoveMaster() == door.GetMoveMaster())) {//TODO:equalds
                    door.SetBlocked(true);
                    OpenInnerDoor();
                    OpenFloorDoor(this.currentFloor);
                }
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            final int triggerFloor = this.spawnArgs.GetInt("triggerFloor");
            if (this.spawnArgs.GetBool("trigger") && (triggerFloor != this.currentFloor)) {
                Event_GotoFloor(idEventArg.toArg(triggerFloor));
            }
        }

        private void Event_PostFloorArrival() {
            OpenFloorDoor(this.currentFloor);
            OpenInnerDoor();
            SetGuiStates((this.currentFloor == 1) ? guiBinaryMoverStates[0] : guiBinaryMoverStates[1]);
            this.controlsDisabled = false;
            if ((this.returnTime > 0.0f) && (this.returnFloor != this.currentFloor)) {
                PostEventSec(EV_GotoFloor, this.returnTime, this.returnFloor);
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

     Binary movers.

     ===============================================================================
     */
    public enum moverState_t {

        MOVER_POS1,
        MOVER_POS2,
        MOVER_1TO2,
        MOVER_2TO1
    }


    /*
     ===============================================================================

     idMover_Binary

     Doors, plats, and buttons are all binary (two position) movers
     Pos1 is "at rest", pos2 is "activated"

     ===============================================================================
     */
    public static class idMover_Binary extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
            this.pos1 = new idVec3();
            this.pos2 = new idVec3();
            this.moverState = MOVER_POS1;
            this.moveMaster = null;
            this.activateChain = null;
            this.soundPos1 = 0;
            this.sound1to2 = 0;
            this.sound2to1 = 0;
            this.soundPos2 = 0;
            this.soundLoop = 0;
            this.wait = 0.0f;
            this.damage = 0.0f;
            this.duration = 0;
            this.accelTime = 0;
            this.decelTime = 0;
            this.activatedBy = new idEntityPtr<>(this);
            this.stateStartTime = 0;
            this.team = new idStr();
            this.enabled = false;
            this.move_thread = 0;
            this.updateStatus = 0;
            this.buddies = new idStrList();
            this.physicsObj = new idPhysics_Parametric();
            this.areaPortal = 0;
            this.blocked = false;
            this.fl.networkSync = true;
	        @SuppressWarnings("unchecked")
			final
	        idList<idEntityPtr<idEntity>> guiTargets1 = new idList<idEntityPtr<idEntity>>((Class<idEntityPtr<idEntity>>) new idEntityPtr<>().getClass());
	        this.guiTargets = guiTargets1;
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
            final String[] temp = {null};

            this.move_thread = 0;
            this.enabled = true;
            this.areaPortal = 0;

            this.activateChain = null;

            this.wait = this.spawnArgs.GetFloat("wait", "0");

            this.updateStatus = this.spawnArgs.GetInt("updateStatus", "0");

            idKeyValue kv = this.spawnArgs.MatchPrefix("buddy", null);
            while (kv != null) {
                this.buddies.Append(kv.GetValue());
                kv = this.spawnArgs.MatchPrefix("buddy", kv);
            }

            this.spawnArgs.GetString("team", "", temp);
            this.team = new idStr(temp[0]);

            if (0 == this.team.Length()) {
                ent = this;
            } else {
                // find the first entity spawned on this team (which could be us)
                for (ent = gameLocal.spawnedEntities.Next(); ent != null; ent = ent.spawnNode.Next()) {
                    if (ent.IsType(idMover_Binary.class) && NOT(idStr.Icmp(((idMover_Binary) ent).team.getData(), temp[0]))) {
                        break;
                    }
                }
                if (null == ent) {
                    ent = this;
                }
            }
            this.moveMaster = (idMover_Binary) ent;

            // create a physics team for the binary mover parts
            if (ent != this) {
                JoinTeam(ent);
            }

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetClipMask(MASK_SOLID);
            if (!this.spawnArgs.GetBool("solid", "1")) {
                this.physicsObj.SetContents(0);
            }
            if (!this.spawnArgs.GetBool("nopush")) {
                this.physicsObj.SetPusher(0);
            }
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, GetPhysics().GetOrigin(), getVec3_origin(), getVec3_origin());
            this.physicsObj.SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, GetPhysics().GetAxis().ToAngles(), getAng_zero(), getAng_zero());
            SetPhysics(this.physicsObj);

            if (this.moveMaster != this) {
                JoinActivateTeam(this.moveMaster);
            }

            final idBounds soundOrigin = new idBounds();
            idMover_Binary slave;

            soundOrigin.Clear();
            for (slave = this.moveMaster; slave != null; slave = slave.activateChain) {
                soundOrigin.oPluSet(slave.GetPhysics().GetAbsBounds());
            }
            this.moveMaster.refSound.origin = soundOrigin.GetCenter();

            if (this.spawnArgs.MatchPrefix("guiTarget") != null) {
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

            savefile.WriteVec3(this.pos1);
            savefile.WriteVec3(this.pos2);
            savefile.WriteInt(etoi(this.moverState));

            savefile.WriteObject(this.moveMaster);
            savefile.WriteObject(this.activateChain);

            savefile.WriteInt(this.soundPos1);
            savefile.WriteInt(this.sound1to2);
            savefile.WriteInt(this.sound2to1);
            savefile.WriteInt(this.soundPos2);
            savefile.WriteInt(this.soundLoop);

            savefile.WriteFloat(this.wait);
            savefile.WriteFloat(this.damage);

            savefile.WriteInt(this.duration);
            savefile.WriteInt(this.accelTime);
            savefile.WriteInt(this.decelTime);

            this.activatedBy.Save(savefile);

            savefile.WriteInt(this.stateStartTime);
            savefile.WriteString(this.team);
            savefile.WriteBool(this.enabled);

            savefile.WriteInt(this.move_thread);
            savefile.WriteInt(this.updateStatus);

            savefile.WriteInt(this.buddies.Num());
            for (i = 0; i < this.buddies.Num(); i++) {
                savefile.WriteString(this.buddies.oGet(i));
            }

            savefile.WriteStaticObject(this.physicsObj);

            savefile.WriteInt(this.areaPortal);
            if (this.areaPortal != 0) {
                savefile.WriteInt(gameRenderWorld.GetPortalState(this.areaPortal));
            }
            savefile.WriteBool(this.blocked);

            savefile.WriteInt(this.guiTargets.Num());
            for (i = 0; i < this.guiTargets.Num(); i++) {
                this.guiTargets.oGet(i).Save(savefile);
            }
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            int i;
            int num, portalState;
            final idStr temp = new idStr();

            savefile.ReadVec3(this.pos1);
            savefile.ReadVec3(this.pos2);
            this.moverState = moverState_t.values()[savefile.ReadInt()];

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/moveMaster);
            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/activateChain);

            this.soundPos1 = savefile.ReadInt();
            this.sound1to2 = savefile.ReadInt();
            this.sound2to1 = savefile.ReadInt();
            this.soundPos2 = savefile.ReadInt();
            this.soundLoop = savefile.ReadInt();

            this.wait = savefile.ReadFloat();
            this.damage = savefile.ReadFloat();

            this.duration = savefile.ReadInt();
            this.accelTime = savefile.ReadInt();
            this.decelTime = savefile.ReadInt();

            this.activatedBy.Restore(savefile);

            this.stateStartTime = savefile.ReadInt();

            savefile.ReadString(this.team);
            this.enabled = savefile.ReadBool();

            this.move_thread = savefile.ReadInt();
            this.updateStatus = savefile.ReadInt();

            num = savefile.ReadInt();
            for (i = 0; i < num; i++) {
                savefile.ReadString(temp);
                this.buddies.Append(temp);
            }

            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);

            this.areaPortal = savefile.ReadInt();
            if (this.areaPortal != 0) {
                portalState = savefile.ReadInt();
                gameLocal.SetPortalState(this.areaPortal, portalState);
            }
            this.blocked = savefile.ReadBool();

            this.guiTargets.Clear();
            num = savefile.ReadInt();
            this.guiTargets.SetNum(num);
            for (i = 0; i < num; i++) {
                this.guiTargets.oGet(i).Restore(savefile);
            }
        }

        @Override
        public void PreBind() {
            this.pos1 = GetWorldCoordinates(this.pos1);
            this.pos2 = GetWorldCoordinates(this.pos2);
        }

        @Override
        public void PostBind() {
            this.pos1 = GetLocalCoordinates(this.pos1);
            this.pos2 = GetLocalCoordinates(this.pos2);
        }

        public void Enable(boolean b) {
            this.enabled = b;
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

            this.pos1 = mpos1;
            this.pos2 = mpos2;

            this.accelTime = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(maccelTime));
            this.decelTime = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(mdecelTime));

            speed = mspeed != 0 ? mspeed : 100;

            // calculate time to reach second position from speed
            move = this.pos2.oMinus(this.pos1);
            distance = move.Length();
            this.duration = idPhysics.SnapTimeToPhysicsFrame((int) ((distance * 1000) / speed));
            if (this.duration <= 0) {
                this.duration = 1;
            }

            this.moverState = MOVER_POS1;

            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.pos1, getVec3_origin(), getVec3_origin());
            this.physicsObj.SetLinearInterpolation(0, 0, 0, 0, getVec3_origin(), getVec3_origin());
            SetOrigin(this.pos1);

            PostEventMS(EV_Mover_InitGuiTargets, 0);
        }

        /*
         ================
         idMover_Binary::InitTime

         pos1, pos2, and time are passed in so the movement delta can be calculated
         ================
         */
        public void InitTime(idVec3 mpos1, idVec3 mpos2, float mtime, float maccelTime, float mdecelTime) {

            this.pos1 = mpos1;
            this.pos2 = mpos2;

            this.accelTime = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(maccelTime));
            this.decelTime = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(mdecelTime));

            this.duration = idPhysics.SnapTimeToPhysicsFrame((int) SEC2MS(mtime));
            if (this.duration <= 0) {
                this.duration = 1;
            }

            this.moverState = MOVER_POS1;

            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.pos1, getVec3_origin(), getVec3_origin());
            this.physicsObj.SetLinearInterpolation(0, 0, 0, 0, getVec3_origin(), getVec3_origin());
            SetOrigin(this.pos1);

            PostEventMS(EV_Mover_InitGuiTargets, 0);
        }

        public void GotoPosition1() {
            idMover_Binary slave;
            int partial;

            // only the master should control this
            if (this.moveMaster != this) {
                this.moveMaster.GotoPosition1();
                return;
            }

            SetGuiStates(guiBinaryMoverStates[etoi(MOVER_2TO1)]);

            if ((this.moverState == MOVER_POS1) || (this.moverState == MOVER_2TO1)) {
                // already there, or on the way
                return;
            }

            if (this.moverState == MOVER_POS2) {
                for (slave = this; slave != null; slave = slave.activateChain) {
                    slave.CancelEvents(EV_Mover_ReturnToPos1);
                }
                if (!this.spawnArgs.GetBool("toggle")) {
                    ProcessEvent(EV_Mover_ReturnToPos1);
                }
                return;
            }

            // only partway up before reversing
            if (this.moverState == MOVER_1TO2) {
                // use the physics times because this might be executed during the physics simulation
                partial = this.physicsObj.GetLinearEndTime() - this.physicsObj.GetTime();
                assert (partial >= 0);
                if (partial < 0) {
                    partial = 0;
                }
                MatchActivateTeam(MOVER_2TO1, this.physicsObj.GetTime() - partial);
                // if already at at position 1 (partial == duration) execute the reached event
                if (partial >= this.duration) {
                    Event_Reached_BinaryMover();
                }
            }
        }

        public void GotoPosition2() {
            int partial;

            // only the master should control this
            if (this.moveMaster != this) {
                this.moveMaster.GotoPosition2();
                return;
            }

            SetGuiStates(guiBinaryMoverStates[etoi(MOVER_1TO2)]);

            if ((this.moverState == MOVER_POS2) || (this.moverState == MOVER_1TO2)) {
                // already there, or on the way
                return;
            }

            if (this.moverState == MOVER_POS1) {
                MatchActivateTeam(MOVER_1TO2, gameLocal.time);

                // open areaportal
                ProcessEvent(EV_Mover_OpenPortal);
                return;
            }

            // only partway up before reversing
            if (this.moverState == MOVER_2TO1) {
                // use the physics times because this might be executed during the physics simulation
                partial = this.physicsObj.GetLinearEndTime() - this.physicsObj.GetTime();
                assert (partial >= 0);
                if (partial < 0) {
                    partial = 0;
                }
                MatchActivateTeam(MOVER_1TO2, this.physicsObj.GetTime() - partial);
                // if already at at position 2 (partial == duration) execute the reached event
                if (partial >= this.duration) {
                    Event_Reached_BinaryMover();
                }
            }
        }

        public void Use_BinaryMover(idEntity activator) {
            // only the master should be used
            if (this.moveMaster != this) {
                this.moveMaster.Use_BinaryMover(activator);
                return;
            }

            if (!this.enabled) {
                return;
            }

            this.activatedBy.oSet(activator);

            if (this.moverState == MOVER_POS1) {
                // FIXME: start moving USERCMD_MSEC later, because if this was player
                // triggered, gameLocal.time hasn't been advanced yet
                MatchActivateTeam(MOVER_1TO2, gameLocal.time + USERCMD_MSEC);

                SetGuiStates(guiBinaryMoverStates[etoi(MOVER_1TO2)]);
                // open areaportal
                ProcessEvent(EV_Mover_OpenPortal);
                return;
            }

            // if all the way up, just delay before coming down
            if (this.moverState == MOVER_POS2) {
                idMover_Binary slave;

                if (this.wait == -1) {
                    return;
                }

                SetGuiStates(guiBinaryMoverStates[etoi(MOVER_2TO1)]);

                for (slave = this; slave != null; slave = slave.activateChain) {
                    slave.CancelEvents(EV_Mover_ReturnToPos1);
                    slave.PostEventSec(EV_Mover_ReturnToPos1, this.spawnArgs.GetBool("toggle") ? 0 : this.wait);
                }
                return;
            }

            // only partway down before reversing
            if (this.moverState == MOVER_2TO1) {
                GotoPosition2();
                return;
            }

            // only partway up before reversing
            if (this.moverState == MOVER_1TO2) {
                GotoPosition1();
                return;
            }
        }

        public void SetGuiStates(final String state) {
            if (this.guiTargets.Num() != 0) {
                SetGuiState("movestate", state);
            }

            idMover_Binary mb = this.activateChain;
            while (mb != null) {
                if (mb.guiTargets.Num() != 0) {
                    mb.SetGuiState("movestate", state);
                }
                mb = mb.activateChain;
            }
        }

        public void UpdateBuddies(int val) {
            int i, c;

            if (this.updateStatus == 2) {
                c = this.buddies.Num();
                for (i = 0; i < c; i++) {
                    final idEntity buddy = gameLocal.FindEntity(this.buddies.oGet(i));
                    if (buddy != null) {
                        buddy.SetShaderParm(SHADERPARM_MODE, val);
                        buddy.UpdateVisuals();
                    }
                }
            }
        }

        public idMover_Binary GetActivateChain() {
            return this.activateChain;
        }

        public idMover_Binary GetMoveMaster() {
            return this.moveMaster;
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
            for (idMover_Binary slave = this.moveMaster; slave != null; slave = slave.activateChain) {
                slave.blocked = b;
                if (b) {
                    idKeyValue kv = slave.spawnArgs.MatchPrefix("triggerBlocked");
                    while (kv != null) {
                        final idEntity ent = gameLocal.FindEntity(kv.GetValue().getData());
                        if (ent != null) {
                            ent.PostEventMS(EV_Activate, 0, this.moveMaster.GetActivator());
                        }
                        kv = slave.spawnArgs.MatchPrefix("triggerBlocked", kv);
                    }
                }
            }
        }

        public boolean IsBlocked() {
            return this.blocked;
        }

        public idEntity GetActivator() {
            return this.activatedBy.GetEntity();
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            this.physicsObj.WriteToSnapshot(msg);
            msg.WriteBits(etoi(this.moverState), 3);
            WriteBindToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            final moverState_t oldMoverState = this.moverState;

            this.physicsObj.ReadFromSnapshot(msg);
            this.moverState = moverState_t.values()[msg.ReadBits(3)];
            ReadBindFromSnapshot(msg);

            if (msg.HasChanged()) {
                if (this.moverState != oldMoverState) {
                    UpdateMoverSound(this.moverState);
                }
                UpdateVisuals();
            }
        }

        public void SetPortalState(boolean open) {
            assert (this.areaPortal != 0);
            gameLocal.SetPortalState(this.areaPortal, (open ? PS_BLOCK_NONE : PS_BLOCK_ALL).ordinal());
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
            if (this.moveMaster == this) {
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
            final idVec3 delta;

            this.moverState = newstate;
            this.move_thread = 0;

            UpdateMoverSound(newstate);

            this.stateStartTime = time;
            switch (this.moverState) {
                case MOVER_POS1: {
                    Signal(SIG_MOVER_POS1);
                    this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, time, 0, this.pos1, getVec3_origin(), getVec3_origin());
                    break;
                }
                case MOVER_POS2: {
                    Signal(SIG_MOVER_POS2);
                    this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, time, 0, this.pos2, getVec3_origin(), getVec3_origin());
                    break;
                }
                case MOVER_1TO2: {
                    Signal(SIG_MOVER_1TO2);
                    this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_LINEAR, time, this.duration, this.pos1, (this.pos2.oMinus(this.pos1)).oMultiply(1000.0f).oDivide(this.duration), getVec3_origin());
                    if ((this.accelTime != 0) || (this.decelTime != 0)) {
                        this.physicsObj.SetLinearInterpolation(time, this.accelTime, this.decelTime, this.duration, this.pos1, this.pos2);
                    } else {
                        this.physicsObj.SetLinearInterpolation(0, 0, 0, 0, this.pos1, this.pos2);
                    }
                    break;
                }
                case MOVER_2TO1: {
                    Signal(SIG_MOVER_2TO1);
                    this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_LINEAR, time, this.duration, this.pos2, (this.pos1.oMinus(this.pos2)).oMultiply(1000.0f).oDivide(this.duration), getVec3_origin());
                    if ((this.accelTime != 0) || (this.decelTime != 0)) {
                        this.physicsObj.SetLinearInterpolation(time, this.accelTime, this.decelTime, this.duration, this.pos2, this.pos1);
                    } else {
                        this.physicsObj.SetLinearInterpolation(0, 0, 0, 0, this.pos1, this.pos2);
                    }
                    break;
                }
            }
        }

        protected moverState_t GetMoverState() {
            return this.moverState;
        }

        protected void FindGuiTargets() {
            gameLocal.GetTargets(this.spawnArgs, this.guiTargets, "guiTarget");
        }

        /*
         ==============================
         idMover_Binary::SetGuiState

         key/val will be set to any renderEntity->gui's on the list
         ==============================
         */
        protected void SetGuiState(final String key, final String val) {
            int i;

            for (i = 0; i < this.guiTargets.Num(); i++) {
                final idEntity ent = this.guiTargets.oGet(i).GetEntity();
                if (ent != null) {
                    for (int j = 0; j < MAX_RENDERENTITY_GUI; j++) {
                        if ((ent.GetRenderEntity() != null) && (ent.GetRenderEntity().gui[ j] != null)) {
                            ent.GetRenderEntity().gui[ j].SetStateString(key, val);
                            ent.GetRenderEntity().gui[ j].StateChanged(gameLocal.time, true);
                        }
                    }
                    ent.UpdateVisuals();
                }
            }
        }

        protected void Event_SetCallback() {
            if ((this.moverState == MOVER_1TO2) || (this.moverState == MOVER_2TO1)) {
                this.move_thread = idThread.CurrentThreadNum();
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

            if (this.moverState == MOVER_1TO2) {
                // reached pos2
                idThread.ObjectMoveDone(this.move_thread, this);
                this.move_thread = 0;

                if (this.moveMaster == this) {
                    StartSound("snd_opened", SND_CHANNEL_ANY, 0, false, null);
                }

                SetMoverState(MOVER_POS2, gameLocal.time);

                SetGuiStates(guiBinaryMoverStates[MOVER_POS2.ordinal()]);

                UpdateBuddies(1);

                if (this.enabled && (this.wait >= 0) && !this.spawnArgs.GetBool("toggle")) {
                    // return to pos1 after a delay
                    PostEventSec(EV_Mover_ReturnToPos1, this.wait);
                }

                // fire targets
                ActivateTargets(this.moveMaster.GetActivator());

                SetBlocked(false);
            } else if (this.moverState == MOVER_2TO1) {
                // reached pos1
                idThread.ObjectMoveDone(this.move_thread, this);
                this.move_thread = 0;

                SetMoverState(MOVER_POS1, gameLocal.time);

                SetGuiStates(guiBinaryMoverStates[MOVER_POS1.ordinal()]);

                UpdateBuddies(0);

                // close areaportals
                if (this.moveMaster == this) {
                    ProcessEvent(EV_Mover_ClosePortal);
                }

                if (this.enabled && (this.wait >= 0) && this.spawnArgs.GetBool("continuous")) {
                    PostEventSec(EV_Activate, this.wait, this);
                }

                SetBlocked(false);
            } else {
                idGameLocal.Error("Event_Reached_BinaryMover: bad moverState");
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

            for (slave = this.moveMaster; slave != null; slave = slave.activateChain) {
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

            for (slave = this.moveMaster; slave != null; slave = slave.activateChain) {
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

            for (slave = this.moveMaster; slave != null; slave = slave.activateChain) {
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

            for (slave = this.moveMaster; slave != null; slave = slave.activateChain) {
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
            if (this.guiTargets.Num() != 0) {
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

    }

    /*
     ===============================================================================

     idDoor

     A use can be triggered either by a touch function, by being shot, or by being
     targeted by another entity.

     ===============================================================================
     */
    public static class idDoor extends idMover_Binary {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
        private final idStr       buddyStr;
        private idClipModel trigger;
        private idClipModel sndTrigger;
        private int         nextSndTriggerTime;
        private idVec3      localTriggerOrigin;
        private idMat3      localTriggerAxis;
        private final idStr       requires;
        private int         removeItem;
        private final idStr       syncLock;
        private int         normalAxisIndex;        // door faces X or Y for spectator teleports
        private idDoor      companionDoor;
        //
        //

// public:
        // CLASS_PROTOTYPE( idDoor );
        public idDoor() {
            this.triggersize = 1.0f;
            this.crusher = false;
            this.noTouch = false;
            this.aas_area_closed = false;
            this.buddyStr = new idStr();
            this.trigger = null;
            this.sndTrigger = null;
            this.nextSndTriggerTime = 0;
            this.localTriggerOrigin = new idVec3();
            this.localTriggerAxis = idMat3.getMat3_identity();
            this.requires = new idStr();
            this.removeItem = 0;
            this.syncLock = new idStr();
            this.companionDoor = null;
            this.normalAxisIndex = 0;
        }
        // ~idDoor( void );

        @Override
        public void Spawn() {
            super.Spawn();
            
            final idVec3 abs_movedir = new idVec3();
            float distance;
            idVec3 size;
            final idVec3 moveDir = new idVec3();
            final float[] dir = {0};
            final float[] lip = {0};
            final boolean[] start_open = {false};
            final float[] time = {0};
            final float[] speed = {0};

            // get the direction to move
            if (!this.spawnArgs.GetFloat("movedir", "0", dir)) {
                // no movedir, so angle defines movement direction and not orientation,
                // a la oldschool Quake
                SetAngles(getAng_zero());
                this.spawnArgs.GetFloat("angle", "0", dir);
            }
            GetMovedir(dir[0], moveDir);

            // default speed of 400
            this.spawnArgs.GetFloat("speed", "400", speed);

            // default wait of 2 seconds
            this.wait = this.spawnArgs.GetFloat("wait", "3");

            // default lip of 8 units
            this.spawnArgs.GetFloat("lip", "8", lip);

            // by default no damage
            this.damage = this.spawnArgs.GetFloat("damage", "0");

            // trigger size
            this.triggersize = this.spawnArgs.GetFloat("triggersize", "120");

            this.crusher = this.spawnArgs.GetBool("crusher", "0");
            this.spawnArgs.GetBool("start_open", "0", start_open);
            this.noTouch = this.spawnArgs.GetBool("no_touch", "0");

            // expects syncLock to be a door that must be closed before this door will open
            this.spawnArgs.GetString("syncLock", "", this.syncLock);

            this.spawnArgs.GetString("buddy", "", this.buddyStr);

            this.spawnArgs.GetString("requires", "", this.requires);
            this.removeItem = this.spawnArgs.GetInt("removeItem", "0");

            // ever separate piece of a door is considered solid when other team mates push entities
            this.fl.solidForTeam = true;

            // first position at start
            this.pos1 = GetPhysics().GetOrigin();

            // calculate second position
            abs_movedir.oSet(0, idMath.Fabs(moveDir.oGet(0)));
            abs_movedir.oSet(1, idMath.Fabs(moveDir.oGet(1)));
            abs_movedir.oSet(2, idMath.Fabs(moveDir.oGet(2)));
            size = GetPhysics().GetAbsBounds().oGet(1).oMinus(GetPhysics().GetAbsBounds().oGet(0));
            distance = (abs_movedir.oMultiply(size)) - lip[0];
            this.pos2 = this.pos1.oPlus(moveDir.oMultiply(distance));

            // if "start_open", reverse position 1 and 2
            if (start_open[0]) {
                // post it after EV_SpawnBind
                PostEventMS(EV_Door_StartOpen, 1);
            }

            if (this.spawnArgs.GetFloat("time", "1", time)) {
                InitTime(this.pos1, this.pos2, time[0], 0, 0);
            } else {
                InitSpeed(this.pos1, this.pos2, speed[0], 0, 0);
            }

            if (this.moveMaster == this) {
                if (this.health != 0) {
                    this.fl.takedamage = true;
                }
                if (this.noTouch || (this.health != 0)) {
                    // non touch/shoot doors
                    PostEventMS(EV_Mover_MatchTeam, 0, this.moverState, gameLocal.time);

                    final String sndtemp = this.spawnArgs.GetString("snd_locked");
                    if ((this.spawnArgs.GetInt("locked") != 0) && (sndtemp != null) && !sndtemp.isEmpty()) {
                        PostEventMS(EV_Door_SpawnSoundTrigger, 0);
                    }
                } else {
                    // spawn trigger
                    PostEventMS(EV_Door_SpawnDoorTrigger, 0);
                }
            }

            // see if we are on an areaportal
            this.areaPortal = gameRenderWorld.FindPortal(GetPhysics().GetAbsBounds());
            if (!start_open[0]) {
                // start closed
                ProcessEvent(EV_Mover_ClosePortal);
            }

            final int locked = this.spawnArgs.GetInt("locked");
            if (locked != 0) {
                // make sure all members of the team get locked
                PostEventMS(EV_Door_Lock, 0, locked);
            }

            if (this.spawnArgs.GetBool("continuous")) {
                PostEventSec(EV_Activate, this.spawnArgs.GetFloat("delay"), this);
            }

            // sounds have a habit of stuttering when portals close, so make them unoccluded
            this.refSound.parms.soundShaderFlags |= SSF_NO_OCCLUSION;

            this.companionDoor = null;

            this.enabled = true;
            this.blocked = false;
        }

        @Override
        public void Save(idSaveGame savefile) {

            savefile.WriteFloat(this.triggersize);
            savefile.WriteBool(this.crusher);
            savefile.WriteBool(this.noTouch);
            savefile.WriteBool(this.aas_area_closed);
            savefile.WriteString(this.buddyStr);
            savefile.WriteInt(this.nextSndTriggerTime);

            savefile.WriteVec3(this.localTriggerOrigin);
            savefile.WriteMat3(this.localTriggerAxis);

            savefile.WriteString(this.requires);
            savefile.WriteInt(this.removeItem);
            savefile.WriteString(this.syncLock);
            savefile.WriteInt(this.normalAxisIndex);

            savefile.WriteClipModel(this.trigger);
            savefile.WriteClipModel(this.sndTrigger);

            savefile.WriteObject(this.companionDoor);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            this.triggersize = savefile.ReadFloat();
            this.crusher = savefile.ReadBool();
            this.noTouch = savefile.ReadBool();
            this.aas_area_closed = savefile.ReadBool();
            SetAASAreaState(this.aas_area_closed);
            savefile.ReadString(this.buddyStr);
            this.nextSndTriggerTime = savefile.ReadInt();

            savefile.ReadVec3(this.localTriggerOrigin);
            savefile.ReadMat3(this.localTriggerAxis);

            savefile.ReadString(this.requires);
            this.removeItem = savefile.ReadInt();
            savefile.ReadString(this.syncLock);
            this.normalAxisIndex = savefile.ReadInt();

            savefile.ReadClipModel(this.trigger);
            savefile.ReadClipModel(this.sndTrigger);

            savefile.ReadObject(this./*reinterpret_cast<idClass *&>*/companionDoor);
        }

        @Override
        public void Think() {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            super.Think();

            if ((this.thinkFlags & TH_PHYSICS) != 0) {
                // update trigger position
                if (GetMasterPosition(masterOrigin, masterAxis)) {
                    if (this.trigger != null) {
                        this.trigger.Link(gameLocal.clip, this, 0, masterOrigin.oPlus(this.localTriggerOrigin.oMultiply(masterAxis)), this.localTriggerAxis.oMultiply(masterAxis));
                    }
                    if (this.sndTrigger != null) {
                        this.sndTrigger.Link(gameLocal.clip, this, 0, masterOrigin.oPlus(this.localTriggerOrigin.oMultiply(masterAxis)), this.localTriggerAxis.oMultiply(masterAxis));
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
            GetLocalTriggerPosition(this.trigger != null ? this.trigger : this.sndTrigger);
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
                        if ((companion != null) && (!companion.equals(master)) && (!companion.GetMoveMaster().equals(master))) {
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
                        if ((companion != null) && (!companion.equals(master)) && (!companion.GetMoveMaster().equals(master))) {
                            companion.Show();
                        }
                        if (slaveDoor.trigger != null) {
                            slaveDoor.trigger.Enable();
                        }
                        if (slaveDoor.sndTrigger != null) {
                            slaveDoor.sndTrigger.Enable();
                        }
                        if ((slaveDoor.areaPortal != 0) && (slaveDoor.moverState == MOVER_POS1)) {
                            slaveDoor.SetPortalState(false);
                        }
                        slaveDoor.SetAASAreaState((IsLocked() != 0) || IsNoTouch());
                    }
                    slave.GetPhysics().GetClipModel().Enable();
                    slave.Show();
                }
            }
        }

        public boolean IsOpen() {
            return (this.moverState != MOVER_POS1);
        }

        public boolean IsNoTouch() {
            return this.noTouch;
        }

        public int IsLocked() {
            return this.spawnArgs.GetInt("locked");
        }

        public void Lock(int f) {
            idMover_Binary other;

            // lock all the doors on the team
            for (other = this.moveMaster; other != null; other = other.GetActivateChain()) {
                if (other.IsType(idDoor.class)) {
                    final idDoor door = (idDoor) other;
                    if (other.equals(this.moveMaster)) {
                        if (door.sndTrigger == null) {
                            // in this case the sound trigger never got spawned
                            final String sndtemp = door.spawnArgs.GetString("snd_locked");
                            if ((sndtemp != null) && !sndtemp.isEmpty()) {
                                door.PostEventMS(EV_Door_SpawnSoundTrigger, 0);
                            }
                        }
                        if ((0 == f) && (door.spawnArgs.GetInt("locked") != 0)) {
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
            if (gameLocal.RequirementMet(activator, this.requires, this.removeItem)) {
                if (this.syncLock.Length() != 0) {
                    final idEntity sync = gameLocal.FindEntity(this.syncLock);
                    if ((sync != null) && sync.IsType(idDoor.class)) {
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
            this.companionDoor = door;
        }

        private void SetAASAreaState(boolean closed) {
            this.aas_area_closed = closed;
            gameLocal.SetAASAreaState(this.physicsObj.GetAbsBounds(), AREACONTENTS_CLUSTERPORTAL | AREACONTENTS_OBSTACLE, closed);
        }

        private void GetLocalTriggerPosition(final idClipModel trigger) {
            final idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();

            if (NOT(trigger)) {
                return;
            }

            GetMasterPosition(origin, axis);
            this.localTriggerOrigin = (trigger.GetOrigin().oMinus(origin)).oMultiply(axis.Transpose());
            this.localTriggerAxis = trigger.GetAxis().oMultiply(axis.Transpose());
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

            this.fl.takedamage = true;
            for (other = this.activateChain; other != null; other = other.GetActivateChain()) {
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
                if ((bounds.oGet(1, i) - bounds.oGet(0, i)) < (bounds.oGet(1, best) - bounds.oGet(0, best))) {
                    best = i;
                }
            }
            this.normalAxisIndex = best;
            bounds.oGet(0).oMinSet(best, size);
            bounds.oGet(1).oPluSet(best, size);
            bounds.oMinSet(GetPhysics().GetOrigin());
        }

        @Override
        protected void Event_Reached_BinaryMover() {
            if (this.moverState == MOVER_2TO1) {
                SetBlocked(false);
                idKeyValue kv = this.spawnArgs.MatchPrefix("triggerClosed");
                while (kv != null) {
                    final idEntity ent = gameLocal.FindEntity(kv.GetValue().getData());
                    if (ent != null) {
                        ent.PostEventMS(EV_Activate, 0, this.moveMaster.GetActivator());
                    }
                    kv = this.spawnArgs.MatchPrefix("triggerClosed", kv);
                }
            } else if (this.moverState == MOVER_1TO2) {
                idKeyValue kv = this.spawnArgs.MatchPrefix("triggerOpened");
                while (kv != null) {
                    final idEntity ent = gameLocal.FindEntity(kv.GetValue().getData());
                    if (ent != null) {
                        ent.PostEventMS(EV_Activate, 0, this.moveMaster.GetActivator());
                    }
                    kv = this.spawnArgs.MatchPrefix("triggerOpened", kv);
                }
            }
            super.Event_Reached_BinaryMover();
        }

        private void Event_TeamBlocked(idEventArg<idEntity> blockedEntity, idEventArg<idEntity> blockingEntity) {
            SetBlocked(true);

            if (this.crusher) {
                return;		// crushers don't reverse
            }

            // reverse direction
            Use_BinaryMover(this.moveMaster.GetActivator());

            if (this.companionDoor != null) {
                this.companionDoor.ProcessEvent(EV_TeamBlocked, blockedEntity.value, blockingEntity.value);
            }
        }

        private void Event_PartBlocked(idEventArg<idEntity> blockingEntity) {
            if (this.damage > 0.0f) {
                blockingEntity.value.Damage(this, this, getVec3_origin(), "damage_moverCrush", this.damage, INVALID_JOINT);
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> _trace) {
            final idEntity other = _other.value;
            final trace_s trace = _trace.value;
//            idVec3 contact, translate;
//            idVec3 planeaxis1, planeaxis2, normal;
//            idBounds bounds;

            if (!this.enabled) {
                return;
            }

            if ((this.trigger != null) && (trace.c.id == this.trigger.GetId())) {
                if (!IsNoTouch() && (0 == IsLocked()) && (GetMoverState() != MOVER_1TO2)) {
                    Use(this, other);
                }
            } else if ((this.sndTrigger != null) && (trace.c.id == this.sndTrigger.GetId())) {
                if ((other != null) && other.IsType(idPlayer.class) && (IsLocked() != 0) && (gameLocal.time > this.nextSndTriggerTime)) {
                    StartSound("snd_locked", SND_CHANNEL_ANY, 0, false, null);
                    this.nextSndTriggerTime = gameLocal.time + 10000;
                }
            }
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            int old_lock;

            if (this.spawnArgs.GetInt("locked") != 0) {
                if (NOT(this.trigger)) {
                    PostEventMS(EV_Door_SpawnDoorTrigger, 0);
                }
                if (this.buddyStr.Length() != 0) {
                    final idEntity buddy = gameLocal.FindEntity(this.buddyStr);
                    if (buddy != null) {
                        buddy.SetShaderParm(SHADERPARM_MODE, 1);
                        buddy.UpdateVisuals();
                    }
                }

                old_lock = this.spawnArgs.GetInt("locked");
                Lock(0);
                if (old_lock == 2) {
                    return;
                }
            }

            if (this.syncLock.Length() != 0) {
                final idEntity sync = gameLocal.FindEntity(this.syncLock);
                if ((sync != null) && sync.IsType(idDoor.class)) {
                    if (((idDoor) sync).IsOpen()) {
                        return;
                    }
                }
            }

            ActivateTargets(activator.value);

            this.renderEntity.shaderParms[ SHADERPARM_MODE] = 1;
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
            final float[] time = {0};
            final float[] speed = {0};

            // if "start_open", reverse position 1 and 2
            this.pos1 = this.pos2;
            this.pos2 = GetPhysics().GetOrigin();

            this.spawnArgs.GetFloat("speed", "400", speed);

            if (this.spawnArgs.GetFloat("time", "1", time)) {
                InitTime(this.pos1, this.pos2, time[0], 0, 0);
            } else {
                InitSpeed(this.pos1, this.pos2, speed[0], 0, 0);
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
            final idBounds bounds = new idBounds();
            idMover_Binary other;
            boolean toggle;

            if (this.trigger != null) {
                // already have a trigger, so don't spawn a new one.
                return;
            }

            // check if any of the doors are marked as toggled
            toggle = false;
            for (other = this.moveMaster; other != null; other = other.GetActivateChain()) {
                if (other.IsType(idDoor.class) && other.spawnArgs.GetBool("toggle")) {
                    toggle = true;
                    break;
                }
            }

            if (toggle) {
                // mark them all as toggled
                for (other = this.moveMaster; other != null; other = other.GetActivateChain()) {
                    if (other.IsType(idDoor.class)) {
                        other.spawnArgs.Set("toggle", "1");
                    }
                }
                // don't spawn trigger
                return;
            }

            final String sndtemp = this.spawnArgs.GetString("snd_locked");
            if ((this.spawnArgs.GetInt("locked") != 0) && (sndtemp != null) && !sndtemp.isEmpty()) {
                PostEventMS(EV_Door_SpawnSoundTrigger, 0);
            }

            CalcTriggerBounds(this.triggersize, bounds);

            // create a trigger clip model
            this.trigger = new idClipModel(new idTraceModel(bounds));
            this.trigger.Link(gameLocal.clip, this, 255, GetPhysics().GetOrigin(), getMat3_identity());
            this.trigger.SetContents(CONTENTS_TRIGGER);

            GetLocalTriggerPosition(this.trigger);

            MatchActivateTeam(this.moverState, gameLocal.time);
        }

        /*
         ======================
         idDoor::Event_SpawnSoundTrigger

         Spawn a sound trigger to activate locked sound if it exists.
         ======================
         */
        private void Event_SpawnSoundTrigger() {
            final idBounds bounds = new idBounds();

            if (this.sndTrigger != null) {
                return;
            }

            CalcTriggerBounds(this.triggersize * 0.5f, bounds);

            // create a trigger clip model
            this.sndTrigger = new idClipModel(new idTraceModel(bounds));
            this.sndTrigger.Link(gameLocal.clip, this, 254, GetPhysics().GetOrigin(), getMat3_identity());
            this.sndTrigger.SetContents(CONTENTS_TRIGGER);

            GetLocalTriggerPosition(this.sndTrigger);
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
            idThread.ReturnFloat(this.spawnArgs.GetInt("locked"));
        }

        private void Event_SpectatorTouch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            final idEntity other = _other.value;
            idVec3 contact, translate;
			final idVec3 normal = new idVec3();
            idBounds bounds;
            idPlayer p;

            assert ((other != null) && other.IsType(idPlayer.class) && ((idPlayer) other).spectating);

            p = (idPlayer) other;
            // avoid flicker when stopping right at clip box boundaries
            if (p.lastSpectateTeleport > (gameLocal.time - 1000)) {
                return;
            }
            if ((this.trigger != null) && !IsOpen()) {
                // teleport to the other side, center to the middle of the trigger brush
                bounds = this.trigger.GetAbsBounds();
                contact = trace.value.endpos.oMinus(bounds.GetCenter());
                translate = bounds.GetCenter();
                normal.Zero();
                normal.oSet(this.normalAxisIndex, 1.0f);
                if (normal.oMultiply(contact) > 0) {
                    translate.oPluSet(this.normalAxisIndex, (bounds.oGet(0, this.normalAxisIndex) - translate.oGet(this.normalAxisIndex)) * 0.5f);
                } else {
                    translate.oPluSet(this.normalAxisIndex, (bounds.oGet(1, this.normalAxisIndex) - translate.oGet(this.normalAxisIndex)) * 0.5f);
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
                        slaveDoor.SetAASAreaState((IsLocked() != 0) || IsNoTouch());
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

     idPlat

     ===============================================================================
     */
    public static class idPlat extends idMover_Binary {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
            this.trigger = null;
            this.localTriggerOrigin.Zero();
            this.localTriggerAxis.Identity();
        }
        // ~idPlat( void );

        @Override
        public void Spawn() {
            final float[] lip = {0};
            final float[] height = {0};
            final float[] time = {0};
            final float[] speed = {0};
            final float[] accel = {0};
            final float[] decel = {0};
            final boolean[] noTouch = {false};

            this.spawnArgs.GetFloat("speed", "100", speed);
            this.damage = this.spawnArgs.GetFloat("damage", "0");
            this.wait = this.spawnArgs.GetFloat("wait", "1");
            this.spawnArgs.GetFloat("lip", "8", lip);
            this.spawnArgs.GetFloat("accel_time", "0.25", accel);
            this.spawnArgs.GetFloat("decel_time", "0.25", decel);

            // create second position
            if (!this.spawnArgs.GetFloat("height", "0", height)) {
                height[0] = (GetPhysics().GetBounds().oGet(1, 2) - GetPhysics().GetBounds().oGet(0, 2)) - lip[0];
            }

            this.spawnArgs.GetBool("no_touch", "0", noTouch);

            // pos1 is the rest (bottom) position, pos2 is the top
            this.pos2 = GetPhysics().GetOrigin();
            this.pos1 = this.pos2;
            this.pos1.oMinSet(2, height[0]);

            if (this.spawnArgs.GetFloat("time", "1", time)) {
                InitTime(this.pos1, this.pos2, time[0], accel[0], decel[0]);
            } else {
                InitSpeed(this.pos1, this.pos2, speed[0], accel[0], decel[0]);
            }

            SetMoverState(MOVER_POS1, gameLocal.time);
            UpdateVisuals();

            // spawn the trigger if one hasn't been custom made
            if (!noTouch[0]) {
                // spawn trigger
                SpawnPlatTrigger(this.pos1);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteClipModel(this.trigger);
            savefile.WriteVec3(this.localTriggerOrigin);
            savefile.WriteMat3(this.localTriggerAxis);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadClipModel(this.trigger);
            savefile.ReadVec3(this.localTriggerOrigin);
            savefile.ReadMat3(this.localTriggerAxis);
        }

        @Override
        public void Think() {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            super.Think();

            if ((this.thinkFlags & TH_PHYSICS) != 0) {
                // update trigger position
                if (GetMasterPosition(masterOrigin, masterAxis)) {
                    if (this.trigger != null) {
                        this.trigger.Link(gameLocal.clip, this, 0, masterOrigin.oPlus(this.localTriggerOrigin.oMultiply(masterAxis)), this.localTriggerAxis.oMultiply(masterAxis));
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
            GetLocalTriggerPosition(this.trigger);
        }

        private void GetLocalTriggerPosition(final idClipModel trigger) {
            final idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();

            if (NOT(trigger)) {
                return;
            }

            GetMasterPosition(origin, axis);
            this.localTriggerOrigin = (trigger.GetOrigin().oMinus(origin)).oMultiply(axis.Transpose());
            this.localTriggerAxis = trigger.GetAxis().oMultiply(axis.Transpose());
        }

        private void SpawnPlatTrigger(idVec3 pos) {
            idBounds bounds;
            final idVec3 tmin = new idVec3();
            final idVec3 tmax = new idVec3();

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

            this.trigger = new idClipModel(new idTraceModel(new idBounds(tmin, tmax)));
            this.trigger.Link(gameLocal.clip, this, 255, GetPhysics().GetOrigin(), getMat3_identity());
            this.trigger.SetContents(CONTENTS_TRIGGER);
        }

        private void Event_TeamBlocked(idEventArg<idEntity> blockedEntity, idEventArg<idEntity> blockingEntity) {
            // reverse direction
            Use_BinaryMover(this.activatedBy.GetEntity());
        }

        private void Event_PartBlocked(idEventArg<idEntity> blockingEntity) {
            if (this.damage > 0) {
                blockingEntity.value.Damage(this, this, getVec3_origin(), "damage_moverCrush", this.damage, INVALID_JOINT);
            }
        }

        private void Event_Touch(idEventArg<idEntity> _other, idEventArg<trace_s> trace) {
            final idEntity other = _other.value;
            if (!other.IsType(idPlayer.class)) {
                return;
            }

            if ((GetMoverState() == MOVER_POS1) && (this.trigger != null) && (trace.value.c.id == this.trigger.GetId()) && (other.health > 0)) {
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

    }

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
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
            this.damage[0] = 0;
            this.physicsObj = new idPhysics_Parametric();
            this.fl.neverDormant = false;
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.spawnArgs.GetFloat("damage", "0", this.damage);
            if (!this.spawnArgs.GetBool("solid", "1")) {
                GetPhysics().SetContents(0);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.damage[0]);
            savefile.WriteStaticObject(this.physicsObj);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            savefile.ReadFloat(this.damage);
            savefile.ReadStaticObject(this.physicsObj);
            RestorePhysics(this.physicsObj);
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
            this.physicsObj.WriteToSnapshot(msg);
            WriteBindToSnapshot(msg);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            this.physicsObj.ReadFromSnapshot(msg);
            ReadBindFromSnapshot(msg);

            if (msg.HasChanged()) {
                UpdateVisuals();
            }
        }

        protected void Event_TeamBlocked(idEventArg<idEntity> blockedEntity, idEventArg<idEntity> blockingEntity) {
        }

        protected void Event_PartBlocked(idEventArg<idEntity> blockingEntity) {
            if (this.damage[0] > 0) {
                blockingEntity.value.Damage(this, this, getVec3_origin(), "damage_moverCrush", this.damage[0], INVALID_JOINT);
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

    }

    /*
     ===============================================================================

     idRotater

     ===============================================================================
     */
    public static class idRotater extends idMover_Periodic {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idRotater );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idMover_Periodic.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idRotater>) idRotater::Event_Activate);
        }

        private final idEntityPtr<idEntity> activatedBy;
        //
        //

        public idRotater() {
            this.activatedBy = new idEntityPtr<>().oSet(this);
        }

        @Override
        public void Spawn() {
            super.Spawn();

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetClipMask(MASK_SOLID);
            if (!this.spawnArgs.GetBool("nopush")) {
                this.physicsObj.SetPusher(0);
            }
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, gameLocal.time, 0, GetPhysics().GetOrigin(), getVec3_origin(), getVec3_origin());
            this.physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, GetPhysics().GetAxis().ToAngles(), getAng_zero(), getAng_zero());
            SetPhysics(this.physicsObj);

            if (this.spawnArgs.GetBool("start_on")) {
                ProcessEvent(EV_Activate, this);
            }
        }

        @Override
        public void Save(idSaveGame savefile) {
            this.activatedBy.Save(savefile);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.activatedBy.Restore(savefile);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {
            final float[] speed = {0};
            final boolean[] x_axis = {false};
            final boolean[] y_axis = {false};
            final idAngles delta = new idAngles();

            this.activatedBy.oSet(activator.value);

            delta.Zero();

            if (!this.spawnArgs.GetBool("rotate")) {
                this.spawnArgs.Set("rotate", "1");
                this.spawnArgs.GetFloat("speed", "100", speed);
                this.spawnArgs.GetBool("x_axis", "0", x_axis);
                this.spawnArgs.GetBool("y_axis", "0", y_axis);

                // set the axis of rotation
                if (x_axis[0]) {
                    delta.oSet(2, speed[0]);
                } else if (y_axis[0]) {
                    delta.oSet(0, speed[0]);
                } else {
                    delta.oSet(1, speed[0]);
                }
            } else {
                this.spawnArgs.Set("rotate", "0");
            }

            this.physicsObj.SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, this.physicsObj.GetAxis().ToAngles(), delta, getAng_zero());
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

     idBobber

     ===============================================================================
     */
    public static class idBobber extends idMover_Periodic {
        // CLASS_PROTOTYPE( idBobber );

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public idBobber() {
        }

        @Override
        public void Spawn() {
            final float[] speed = {0};
            final float[] height = {0};
            final float[] phase = {0};
            final boolean[] x_axis = {false};
            final boolean[] y_axis = {false};
            idVec3 delta;

            this.spawnArgs.GetFloat("speed", "4", speed);
            this.spawnArgs.GetFloat("height", "32", height);
            this.spawnArgs.GetFloat("phase", "0", phase);
            this.spawnArgs.GetBool("x_axis", "0", x_axis);
            this.spawnArgs.GetBool("y_axis", "0", y_axis);

            // set the axis of bobbing
            delta = getVec3_origin();
            if (x_axis[0]) {
                delta.oSet(0, height[0]);
            } else if (y_axis[0]) {
                delta.oSet(1, height[0]);
            } else {
                delta.oSet(2, height[0]);
            }

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetClipMask(MASK_SOLID);
            if (!this.spawnArgs.GetBool("nopush")) {
                this.physicsObj.SetPusher(0);
            }
            this.physicsObj.SetLinearExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), (int) (phase[0] * 1000), (int) (speed[0] * 500), GetPhysics().GetOrigin(), delta.oMultiply(2.0f), getVec3_origin());
            SetPhysics(this.physicsObj);
        }
    }

    /*
     ===============================================================================

     idPendulum

     ===============================================================================
     */
    public static class idPendulum extends idMover_Periodic {
        // CLASS_PROTOTYPE( idPendulum );

/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		//        public idPendulum() {//TODO:remove default constructor override
//        }
        @Override
        public void Spawn() {
            super.Spawn();

            final float[] speed = {0};
            final float[] freq = {0};
            final float[] length = {0};
            final float[] phase = {0};

            this.spawnArgs.GetFloat("speed", "30", speed);
            this.spawnArgs.GetFloat("phase", "0", phase);

            if (this.spawnArgs.GetFloat("freq", "", freq)) {
                if (freq[0] <= 0.0f) {
                    idGameLocal.Error("Invalid frequency on entity '%s'", GetName());
                }
            } else {
                // find pendulum length
                length[0] = idMath.Fabs(GetPhysics().GetBounds().oGet(0, 2));
                if (length[0] < 8) {
                    length[0] = 8;
                }

                freq[0] = (1 / (idMath.TWO_PI)) * idMath.Sqrt(g_gravity.GetFloat() / (3 * length[0]));
            }

            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());
            this.physicsObj.SetClipMask(MASK_SOLID);
            if (!this.spawnArgs.GetBool("nopush")) {
                this.physicsObj.SetPusher(0);
            }
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, GetPhysics().GetOrigin(), getVec3_origin(), getVec3_origin());
            this.physicsObj.SetAngularExtrapolation((EXTRAPOLATION_DECELSINE | EXTRAPOLATION_NOSTOP), (int) (phase[0] * 1000), (int) (500 / freq[0]), GetPhysics().GetAxis().ToAngles(), new idAngles(0, 0, speed[0] * 2.0f), getAng_zero());
            SetPhysics(this.physicsObj);
        }
    }


    /*
     ===============================================================================

     idRiser

     ===============================================================================
     */
    public static class idRiser extends idMover_Periodic {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		// CLASS_PROTOTYPE( idRiser );
        private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idMover_Periodic.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idRiser>) idRiser::Event_Activate);
        }

//public	idRiser( ){}
        @Override
        public void Spawn() {
            this.physicsObj.SetSelf(this);
            this.physicsObj.SetClipModel(new idClipModel(GetPhysics().GetClipModel()), 1.0f);
            this.physicsObj.SetOrigin(GetPhysics().GetOrigin());
            this.physicsObj.SetAxis(GetPhysics().GetAxis());

            this.physicsObj.SetClipMask(MASK_SOLID);
            if (!this.spawnArgs.GetBool("solid", "1")) {
                this.physicsObj.SetContents(0);
            }
            if (!this.spawnArgs.GetBool("nopush")) {
                this.physicsObj.SetPusher(0);
            }
            this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, GetPhysics().GetOrigin(), getVec3_origin(), getVec3_origin());
            SetPhysics(this.physicsObj);
        }

        private void Event_Activate(idEventArg<idEntity> activator) {

            if (!IsHidden() && this.spawnArgs.GetBool("hide")) {
                Hide();
            } else {
                Show();
                final float[] time = {0};
                final float[] height = {0};
                idVec3 delta;

                this.spawnArgs.GetFloat("time", "4", time);
                this.spawnArgs.GetFloat("height", "32", height);

                delta = getVec3_origin();
                delta.oSet(2, height[0]);

                this.physicsObj.SetLinearExtrapolation(EXTRAPOLATION_LINEAR, gameLocal.time, (int) (time[0] * 1000), this.physicsObj.GetOrigin(), delta, getVec3_origin());
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
