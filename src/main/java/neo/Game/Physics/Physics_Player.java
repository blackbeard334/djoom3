package neo.Game.Physics;

import static java.lang.Math.abs;
import static neo.CM.CollisionModel.contactType_t.CONTACT_TRMVERTEX;
import static neo.Game.GameSys.SysCvar.pm_crouchheight;
import static neo.Game.GameSys.SysCvar.pm_deadheight;
import static neo.Game.GameSys.SysCvar.pm_normalheight;
import static neo.Game.GameSys.SysCvar.pm_usecylinder;
import static neo.Game.Game_local.ENTITYNUM_WORLD;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MASK_WATER;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_DEAD;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_FREEZE;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_NOCLIP;
import static neo.Game.Physics.Physics_Player.pmtype_t.PM_SPECTATOR;
import static neo.Game.Physics.Physics_Player.waterLevel_t.WATERLEVEL_FEET;
import static neo.Game.Physics.Physics_Player.waterLevel_t.WATERLEVEL_HEAD;
import static neo.Game.Physics.Physics_Player.waterLevel_t.WATERLEVEL_NONE;
import static neo.Game.Physics.Physics_Player.waterLevel_t.WATERLEVEL_WAIST;
import static neo.Game.Physics.Push.PUSHFL_APPLYIMPULSE;
import static neo.Game.Physics.Push.PUSHFL_CLIP;
import static neo.Game.Physics.Push.PUSHFL_NOGROUNDENTITIES;
import static neo.Game.Physics.Push.PUSHFL_ONLYMOVEABLE;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Material.SURF_LADDER;
import static neo.Renderer.Material.SURF_SLICK;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.Game_local.idEntityPtr;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_Actor.idPhysics_Actor;
import neo.Renderer.Material.idMaterial;
import neo.framework.UsercmdGen.usercmd_t;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Physics_Player {

    /*
     ===================================================================================

     Player physics

     Simulates the motion of a player through the environment. Input from the
     player is used to allow a certain degree of control over the motion.

     ===================================================================================
     */
    // movementType
    public enum pmtype_t {

        PM_NORMAL, // normal physics
        PM_DEAD, // no acceleration or turning, but free falling
        PM_SPECTATOR, // flying without gravity but with collision detection
        PM_FREEZE, // stuck in place without control
        PM_NOCLIP   // flying without collision detection nor gravity
    }

    public enum waterLevel_t {

        WATERLEVEL_NONE,
        WATERLEVEL_FEET,
        WATERLEVEL_WAIST,
        WATERLEVEL_HEAD
    }
//
    public static final int MAXTOUCH = 32;
//

    public static final class playerPState_s {

        idVec3 origin;
        idVec3 velocity;
        idVec3 localOrigin;
        idVec3 pushVelocity;
        float  stepUp;
        int    movementType;
        int    movementFlags;
        int    movementTime;

        public playerPState_s() {
            this.origin = new idVec3();
            this.velocity = new idVec3();
            this.localOrigin = new idVec3();
            this.pushVelocity = new idVec3();
        }
    }
    // movement parameters
    static final float PM_STOPSPEED       = 100.0f;
    static final float PM_SWIMSCALE       = 0.5f;
    static final float PM_LADDERSPEED     = 100.0f;
    static final float PM_STEPSCALE       = 1.0f;
    //
    static final float PM_ACCELERATE      = 10.0f;
    static final float PM_AIRACCELERATE   = 1.0f;
    static final float PM_WATERACCELERATE = 4.0f;
    static final float PM_FLYACCELERATE   = 8.0f;
    //
    static final float PM_FRICTION        = 6.0f;
    static final float PM_AIRFRICTION     = 0.0f;
    static final float PM_WATERFRICTION   = 1.0f;
    static final float PM_FLYFRICTION     = 3.0f;
    static final float PM_NOCLIPFRICTION  = 12.0f;
    //
    static final float MIN_WALK_NORMAL    = 0.7f;     // can't walk on very steep slopes
    static final float OVERCLIP           = 1.001f;
    //
// movementFlags
    static final int   PMF_DUCKED         = 1;        // set when ducking
    static final int   PMF_JUMPED         = 2;        // set when the player jumped this frame
    static final int   PMF_STEPPED_UP     = 4;        // set when the player stepped up this frame
    static final int   PMF_STEPPED_DOWN   = 8;        // set when the player stepped down this frame
    static final int   PMF_JUMP_HELD      = 16;       // set when jump button is held down
    static final int   PMF_TIME_LAND      = 32;       // movementTime is time before rejump
    static final int   PMF_TIME_KNOCKBACK = 64;       // movementTime is an air-accelerate only time
    static final int   PMF_TIME_WATERJUMP = 128;      // movementTime is waterjump
    static final int   PMF_ALL_TIMES      = (PMF_TIME_WATERJUMP | PMF_TIME_LAND | PMF_TIME_KNOCKBACK);
    //
    static       int   c_pmove            = 0;
//

    static final float PLAYER_VELOCITY_MAX           = 4000;
    static final int   PLAYER_VELOCITY_TOTAL_BITS    = 16;
    static final int   PLAYER_VELOCITY_EXPONENT_BITS = idMath.BitsForInteger(idMath.BitsForFloat(PLAYER_VELOCITY_MAX)) + 1;
    static final int   PLAYER_VELOCITY_MANTISSA_BITS = PLAYER_VELOCITY_TOTAL_BITS - 1 - PLAYER_VELOCITY_EXPONENT_BITS;
    static final int   PLAYER_MOVEMENT_TYPE_BITS     = 3;
    static final int   PLAYER_MOVEMENT_FLAGS_BITS    = 8;
//

    public static class idPhysics_Player extends idPhysics_Actor {
        // CLASS_PROTOTYPE( idPhysics_Player );

        // player physics state
        private playerPState_s current;
        private playerPState_s saved;
        //
        // properties
        private float          walkSpeed;
        private float          crouchSpeed;
        private float          maxStepHeight;
        private float          maxJumpHeight;
        private int            debugLevel;            // if set, diagnostic output will be printed
        //
        // player input
        private usercmd_t      command;
        private final idAngles       viewAngles;
        //
        // run-time variables
        private int            framemsec;
        private float          frametime;
        private float          playerSpeed;
        private final idVec3         viewForward;
        private final idVec3         viewRight;
        //
        // walk movement
        private boolean        walking;
        private boolean        groundPlane;
        private final trace_s        groundTrace;
        private idMaterial     groundMaterial;
        //
        // ladder movement
        private boolean        ladder;
        private final idVec3         ladderNormal;
        //
        // results of last evaluate
        waterLevel_t waterLevel;
        int          waterType;
        //
        //

        public idPhysics_Player() {
            this.debugLevel = 0;//false;
            this.clipModel = null;
            this.clipMask = 0;
            this.current = new playerPState_s();//memset( &current, 0, sizeof( current ) );
            this.saved = this.current;
            this.walkSpeed = 0;
            this.crouchSpeed = 0;
            this.maxStepHeight = 0;
            this.maxJumpHeight = 0;
            this.command = new usercmd_t();//memset( &command, 0, sizeof( command ) );
            this.viewAngles = new idAngles();
            this.framemsec = 0;
            this.frametime = 0;
            this.playerSpeed = 0;
            this.viewForward = new idVec3();
            this.viewRight = new idVec3();
            this.walking = false;
            this.groundPlane = false;
            this.groundTrace = new trace_s();//memset( &groundTrace, 0, sizeof( groundTrace ) );
            this.groundMaterial = null;
            this.ladder = false;
            this.ladderNormal = new idVec3();
            this.waterLevel = WATERLEVEL_NONE;
            this.waterType = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {

            idPhysics_Player_SavePState(savefile, this.current);
            idPhysics_Player_SavePState(savefile, this.saved);

            savefile.WriteFloat(this.walkSpeed);
            savefile.WriteFloat(this.crouchSpeed);
            savefile.WriteFloat(this.maxStepHeight);
            savefile.WriteFloat(this.maxJumpHeight);
            savefile.WriteInt(this.debugLevel);

            savefile.WriteUsercmd(this.command);
            savefile.WriteAngles(this.viewAngles);

            savefile.WriteInt(this.framemsec);
            savefile.WriteFloat(this.frametime);
            savefile.WriteFloat(this.playerSpeed);
            savefile.WriteVec3(this.viewForward);
            savefile.WriteVec3(this.viewRight);

            savefile.WriteBool(this.walking);
            savefile.WriteBool(this.groundPlane);
            savefile.WriteTrace(this.groundTrace);
            savefile.WriteMaterial(this.groundMaterial);

            savefile.WriteBool(this.ladder);
            savefile.WriteVec3(this.ladderNormal);

            savefile.WriteInt(etoi(this.waterLevel));
            savefile.WriteInt(this.waterType);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            idPhysics_Player_RestorePState(savefile, this.current);
            idPhysics_Player_RestorePState(savefile, this.saved);

            this.walkSpeed = savefile.ReadFloat();
            this.crouchSpeed = savefile.ReadFloat();
            this.maxStepHeight = savefile.ReadFloat();
            this.maxJumpHeight = savefile.ReadFloat();
            this.debugLevel = savefile.ReadInt();

            savefile.ReadUsercmd(this.command);
            savefile.ReadAngles(this.viewAngles);

            this.framemsec = savefile.ReadInt();
            this.frametime = savefile.ReadFloat();
            this.playerSpeed = savefile.ReadFloat();
            savefile.ReadVec3(this.viewForward);
            savefile.ReadVec3(this.viewRight);

            this.walking = savefile.ReadBool();
            this.groundPlane = savefile.ReadBool();
            savefile.ReadTrace(this.groundTrace);
            savefile.ReadMaterial(this.groundMaterial);

            this.ladder = savefile.ReadBool();
            savefile.ReadVec3(this.ladderNormal);

            this.waterLevel = waterLevel_t.values()[savefile.ReadInt()];
            this.waterType = savefile.ReadInt();
        }

        // initialisation
        public void SetSpeed(final float newWalkSpeed, final float newCrouchSpeed) {
            this.walkSpeed = newWalkSpeed;
            this.crouchSpeed = newCrouchSpeed;
        }

        public void SetMaxStepHeight(final float newMaxStepHeight) {
            this.maxStepHeight = newMaxStepHeight;
        }

        public float GetMaxStepHeight() {
            return this.maxStepHeight;
        }

        public void SetMaxJumpHeight(final float newMaxJumpHeight) {
            this.maxJumpHeight = newMaxJumpHeight;
        }

        public void SetMovementType(final pmtype_t type) {
            this.current.movementType = etoi(type);
        }

        public void SetPlayerInput(final usercmd_t cmd, final idAngles newViewAngles) {
            this.command = cmd;
            this.viewAngles.oSet(newViewAngles);// can't use cmd.angles cause of the delta_angles
        }

        public void SetKnockBack(final int knockBackTime) {
            if (this.current.movementTime != 0) {
                return;
            }
            this.current.movementFlags |= PMF_TIME_KNOCKBACK;
            this.current.movementTime = knockBackTime;
        }

        public void SetDebugLevel(boolean set) {
            this.debugLevel = btoi(set);
        }

        // feed back from last physics frame
        public waterLevel_t GetWaterLevel() {
            return this.waterLevel;
        }

        public int GetWaterType() {
            return this.waterType;
        }

        public boolean HasJumped() {
            return ((this.current.movementFlags & PMF_JUMPED) != 0);
        }

        public boolean HasSteppedUp() {
            return ((this.current.movementFlags & (PMF_STEPPED_UP | PMF_STEPPED_DOWN)) != 0);
        }

        public float GetStepUp() {
            return this.current.stepUp;
        }

        public boolean IsCrouching() {
            return ((this.current.movementFlags & PMF_DUCKED) != 0);
        }

        public boolean OnLadder() {
            return this.ladder;
        }

        // != GetOrigin
        public idVec3 PlayerGetOrigin() {
            return this.current.origin;
        }

        @Override
        // common physics interface
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            final idVec3 masterOrigin = new idVec3();
			idVec3 oldOrigin;
            final idMat3 masterAxis = new idMat3();

            this.waterLevel = WATERLEVEL_NONE;
            this.waterType = 0;
            oldOrigin = new idVec3(this.current.origin);

            this.clipModel.Unlink();

            // if bound to a master
            if (this.masterEntity != null) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.origin.oSet(masterOrigin.oPlus(this.current.localOrigin.oMultiply(masterAxis)));
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());
                this.current.velocity.oSet((this.current.origin.oMinus(oldOrigin)).oDivide(timeStepMSec * 0.001f));
                this.masterDeltaYaw = this.masterYaw;
                this.masterYaw = masterAxis.oGet(0).ToYaw();
                this.masterDeltaYaw = this.masterYaw - this.masterDeltaYaw;
                return true;
            }

            ActivateContactEntities();

            this.MovePlayer(timeStepMSec);

            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());

            if (IsOutsideWorld()) {
                gameLocal.Warning("clip model outside world bounds for entity '%s' at (%s)", this.self.name, this.current.origin.ToString(0));
            }

            return true; //( current.origin != oldOrigin );
        }

        @Override
        public void UpdateTime(int endTimeMSec) {
        }

        @Override
        public int GetTime() {
            return gameLocal.time;
        }

        @Override
        public impactInfo_s GetImpactInfo(final int id, final idVec3 point) {
            final impactInfo_s info = new impactInfo_s();
            info.invMass = this.invMass;
            info.invInertiaTensor.Zero();
            info.position.Zero();
            info.velocity.oSet(this.current.velocity);
            return info;
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
            if (this.current.movementType != etoi(PM_NOCLIP)) {
                this.current.velocity.oPluSet(impulse.oMultiply(this.invMass));
            }
        }

        @Override
        public boolean IsAtRest() {
            return false;
        }

        @Override
        public int GetRestStartTime() {
            return -1;
        }

        @Override
        public void SaveState() {
            this.saved = this.current;
        }

        @Override
        public void RestoreState() {
            this.current = this.saved;

            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());

            EvaluateContacts();
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.localOrigin.oSet(newOrigin);
            if (this.masterEntity != null) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.origin.oSet(masterOrigin.oPlus(newOrigin.oMultiply(masterAxis)));
            } else {
                this.current.origin.oSet(newOrigin);
            }

            this.clipModel.Link(gameLocal.clip, this.self, 0, newOrigin, this.clipModel.GetAxis());
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            this.clipModel.Link(gameLocal.clip, this.self, 0, this.clipModel.GetOrigin(), newAxis);
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {

            this.current.localOrigin.oPluSet(translation);
            this.current.origin.oPluSet(translation);

            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.origin.oMulSet(rotation);
            if (this.masterEntity != null) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.localOrigin.oSet((this.current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
            } else {
                this.current.localOrigin.oSet(this.current.origin);
            }

            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis().oMultiply(rotation.ToMat3()));
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            this.current.velocity.oSet(newLinearVelocity);
        }

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            return new idVec3(this.current.velocity);
        }

        @Override
        public void SetPushed(int deltaTime) {
            idVec3 velocity;
            float d;

            // velocity with which the player is pushed
            velocity = (this.current.origin.oMinus(this.saved.origin)).oDivide(deltaTime * idMath.M_MS2SEC);

            // remove any downward push velocity
            d = velocity.oMultiply(this.gravityNormal);
            if (d > 0.0f) {
                velocity.oMinSet(this.gravityNormal.oMultiply(d));
            }

            this.current.pushVelocity.oPluSet(velocity);
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return this.current.pushVelocity;
        }

        public idVec3 GetPushedLinearVelocity() {
            return GetPushedLinearVelocity(0);
        }

        public void ClearPushedVelocity() {
            this.current.pushVelocity.Zero();
        }

        /*
         ================
         idPhysics_Player::SetMaster

         the binding is never orientated
         ================
         */
        @Override
        public void SetMaster(idEntity master, final boolean orientated /*= true*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (null == this.masterEntity) {
                    // transform from world space to master space
                    this.self.GetMasterPosition(masterOrigin, masterAxis);
                    this.current.localOrigin.oSet((this.current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                    this.masterEntity = master;
                    this.masterYaw = masterAxis.oGet(0).ToYaw();
                }
                ClearContacts();
            } else {
                if (this.masterEntity != null) {
                    this.masterEntity = null;
                }
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteFloat(this.current.origin.oGet(0));
            msg.WriteFloat(this.current.origin.oGet(1));
            msg.WriteFloat(this.current.origin.oGet(2));
            msg.WriteFloat(this.current.velocity.oGet(0), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteFloat(this.current.velocity.oGet(1), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteFloat(this.current.velocity.oGet(2), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(this.current.origin.oGet(0), this.current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(this.current.origin.oGet(1), this.current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(this.current.origin.oGet(2), this.current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(0), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(1), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.pushVelocity.oGet(2), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, this.current.stepUp);
            msg.WriteBits(this.current.movementType, PLAYER_MOVEMENT_TYPE_BITS);
            msg.WriteBits(this.current.movementFlags, PLAYER_MOVEMENT_FLAGS_BITS);
            msg.WriteDeltaLong(0, this.current.movementTime);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            this.current.origin.oSet(0, msg.ReadFloat());
            this.current.origin.oSet(1, msg.ReadFloat());
            this.current.origin.oSet(2, msg.ReadFloat());
            this.current.velocity.oSet(0, msg.ReadFloat(PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            this.current.velocity.oSet(1, msg.ReadFloat(PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            this.current.velocity.oSet(2, msg.ReadFloat(PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            this.current.localOrigin.oSet(0, msg.ReadDeltaFloat(this.current.origin.oGet(0)));
            this.current.localOrigin.oSet(1, msg.ReadDeltaFloat(this.current.origin.oGet(1)));
            this.current.localOrigin.oSet(2, msg.ReadDeltaFloat(this.current.origin.oGet(2)));
            this.current.pushVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            this.current.pushVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            this.current.stepUp = msg.ReadDeltaFloat(0.0f);
            this.current.movementType = msg.ReadBits(PLAYER_MOVEMENT_TYPE_BITS);
            this.current.movementFlags = msg.ReadBits(PLAYER_MOVEMENT_FLAGS_BITS);
            this.current.movementTime = msg.ReadDeltaLong(0);

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.clipModel.GetAxis());
            }
        }

        /*
         ============
         idPhysics_Player::CmdScale

         Returns the scale factor to apply to cmd movements
         This allows the clients to use axial -127 to 127 values for all directions
         without getting a sqrt(2) distortion in speed.
         ============
         */
        private float CmdScale(final usercmd_t cmd) {
            int max;
            float total;
            float scale;
            int forwardmove;
            int rightmove;
            int upmove;

            forwardmove = cmd.forwardmove;
            rightmove = cmd.rightmove;

            // since the crouch key doubles as downward movement, ignore downward movement when we're on the ground
            // otherwise crouch speed will be lower than specified
            if (this.walking) {
                upmove = 0;
            } else {
                upmove = cmd.upmove;
            }

            max = abs(forwardmove);
            if (abs(rightmove) > max) {
                max = abs(rightmove);
            }
            if (abs(upmove) > max) {
                max = abs(upmove);
            }

            if (0 == max) {
                return 0.0f;
            }

            total = idMath.Sqrt(((float) forwardmove * forwardmove) + (rightmove * rightmove) + (upmove * upmove));
            scale = (this.playerSpeed * max) / (127.0f * total);

            return scale;
        }

        /*
         ==============
         idPhysics_Player::Accelerate

         Handles user intended acceleration
         ==============
         */
        private void Accelerate(final idVec3 wishdir, final float wishspeed, final float accel) {
            if (true) {
                // q2 style
                float addspeed, accelspeed, currentspeed;

                currentspeed = this.current.velocity.oMultiply(wishdir);
                addspeed = wishspeed - currentspeed;
                if (addspeed <= 0) {
                    return;
                }
                accelspeed = accel * this.frametime * wishspeed;
                if (accelspeed > addspeed) {
                    accelspeed = addspeed;
                }

                this.current.velocity.oPluSet(wishdir.oMultiply(accelspeed));
//}else{
//	// proper way (avoids strafe jump maxspeed bug), but feels bad
//	idVec3		wishVelocity;
//	idVec3		pushDir;
//	float		pushLen;
//	float		canPush;
//
//	wishVelocity = wishdir * wishspeed;
//	pushDir = wishVelocity - current.velocity;
//	pushLen = pushDir.Normalize();
//
//	canPush = accel * frametime * wishspeed;
//	if (canPush > pushLen) {
//		canPush = pushLen;
//	}
//
//	current.velocity += canPush * pushDir;
            }
        }

        /*
         ==================
         idPhysics_Player::SlideMove

         Returns true if the velocity was clipped in some way
         ==================
         */
        static final int MAX_CLIP_PLANES = 5;

        private boolean SlideMove(boolean gravity, boolean stepUp, boolean stepDown, boolean push) {
            int i, j, k, pushFlags;
            int bumpcount, numbumps, numplanes;
            float d, time_left, into, totalMass;
            idVec3 dir;
            final idVec3[] planes = new idVec3[MAX_CLIP_PLANES];
            idVec3 end, stepEnd;
			final idVec3 primal_velocity = new idVec3(), endVelocity = new idVec3();
			idVec3 endClipVelocity = new idVec3(), clipVelocity = new idVec3();
            final trace_s[] trace = {null}, stepTrace = {null}, downTrace = {null};
            boolean nearGround, stepped, pushed;

            numbumps = 4;

            primal_velocity.oSet(this.current.velocity);

            if (gravity) {
                endVelocity.oSet(this.current.velocity.oPlus(this.gravityVector.oMultiply(this.frametime)));
                this.current.velocity.oSet((this.current.velocity.oPlus(endVelocity)).oMultiply(0.5f));
                primal_velocity.oSet(endVelocity);
                if (this.groundPlane) {
                    // slide along the ground plane
                    this.current.velocity.ProjectOntoPlane(this.groundTrace.c.normal, OVERCLIP);
                }
            } else {
                endVelocity.oSet(this.current.velocity);
            }

            time_left = this.frametime;

            // never turn against the ground plane
            if (this.groundPlane) {
                numplanes = 1;
                planes[0] = this.groundTrace.c.normal;
            } else {
                numplanes = 0;
            }

            // never turn against original velocity
            planes[numplanes] = new idVec3(this.current.velocity);
            planes[numplanes].Normalize();
            numplanes++;

            for (bumpcount = 0; bumpcount < numbumps; bumpcount++) {

                // calculate position we are trying to move to
                end = this.current.origin.oPlus(this.current.velocity.oMultiply(time_left));

                // see if we can make it there
                gameLocal.clip.Translation(trace, this.current.origin, end, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);

                time_left -= time_left * trace[0].fraction;
                this.current.origin.oSet(trace[0].endpos);

                // if moved the entire distance
                if (trace[0].fraction >= 1.0f) {
                    break;
                }

                stepped = pushed = false;

                // if we are allowed to step up
                if (stepUp) {

                    nearGround = this.groundPlane | this.ladder;

                    if (!nearGround) {
                        // trace down to see if the player is near the ground
                        // step checking when near the ground allows the player to move up stairs smoothly while jumping
                        stepEnd = this.current.origin.oPlus(this.gravityNormal.oMultiply(this.maxStepHeight));
                        gameLocal.clip.Translation(downTrace, this.current.origin, stepEnd, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
                        nearGround = ((downTrace[0].fraction < 1.0f) && ((downTrace[0].c.normal.oMultiply(this.gravityNormal.oNegative())) > MIN_WALK_NORMAL));
                    }

                    // may only step up if near the ground or on a ladder
                    if (nearGround) {

                        // step up
                        stepEnd = this.current.origin.oMinus(this.gravityNormal.oMultiply(this.maxStepHeight));
                        gameLocal.clip.Translation(downTrace, this.current.origin, stepEnd, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);

                        // trace along velocity
                        stepEnd = downTrace[0].endpos.oPlus(this.current.velocity.oMultiply(time_left));
                        gameLocal.clip.Translation(stepTrace, downTrace[0].endpos, stepEnd, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);

                        // step down
                        stepEnd = stepTrace[0].endpos.oPlus(this.gravityNormal.oMultiply(this.maxStepHeight));
                        gameLocal.clip.Translation(downTrace, stepTrace[0].endpos, stepEnd, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);

                        if ((downTrace[0].fraction >= 1.0f) || ((downTrace[0].c.normal.oMultiply(this.gravityNormal.oNegative())) > MIN_WALK_NORMAL)) {

                            // if moved the entire distance
                            if (stepTrace[0].fraction >= 1.0f) {
//                                time_left = 0;
                                this.current.stepUp -= (downTrace[0].endpos.oMinus(this.current.origin)).oMultiply(this.gravityNormal);
                                this.current.origin.oSet(downTrace[0].endpos);
                                this.current.movementFlags |= PMF_STEPPED_UP;
                                this.current.velocity.oMulSet(PM_STEPSCALE);
                                break;
                            }

                            // if the move is further when stepping up
                            if (stepTrace[0].fraction > trace[0].fraction) {
                                time_left -= time_left * stepTrace[0].fraction;
                                this.current.stepUp -= (downTrace[0].endpos.oMinus(this.current.origin)).oMultiply(this.gravityNormal);
                                this.current.origin.oSet(downTrace[0].endpos);
                                this.current.movementFlags |= PMF_STEPPED_UP;
                                this.current.velocity.oMulSet(PM_STEPSCALE);
                                trace[0] = stepTrace[0];
                                stepped = true;
                            }
                        }
                    }
                }

                // if we can push other entities and not blocked by the world
                if (push && (trace[0].c.entityNum != ENTITYNUM_WORLD)) {

                    this.clipModel.SetPosition(this.current.origin, this.clipModel.GetAxis());

                    // clip movement, only push idMoveables, don't push entities the player is standing on
                    // apply impact to pushed objects
                    pushFlags = PUSHFL_CLIP | PUSHFL_ONLYMOVEABLE | PUSHFL_NOGROUNDENTITIES | PUSHFL_APPLYIMPULSE;

                    // clip & push
                    totalMass = gameLocal.push.ClipTranslationalPush(trace, this.self, pushFlags, end, end.oMinus(this.current.origin));

                    if (totalMass > 0.0f) {
                        // decrease velocity based on the total mass of the objects being pushed ?
                        this.current.velocity.oMulSet(1.0f - (idMath.ClampFloat(0.0f, 1000.0f, totalMass - 20.0f)
                                * (1.0f / 950.0f)));
                        pushed = true;
                    }

                    this.current.origin.oSet(trace[0].endpos);
                    time_left -= time_left * trace[0].fraction;

                    // if moved the entire distance
                    if (trace[0].fraction >= 1.0f) {
                        break;
                    }
                }

                if (!stepped) {
                    // let the entity know about the collision
                    this.self.Collide(trace[0], this.current.velocity);
                }

                if (numplanes >= MAX_CLIP_PLANES) {
                    // MrElusive: I think we have some relatively high poly LWO models with a lot of slanted tris
                    // where it may hit the max clip planes
                    this.current.velocity.oSet(getVec3_origin());
                    return true;
                }

                //
                // if this is the same plane we hit before, nudge velocity
                // out along it, which fixes some epsilon issues with
                // non-axial planes
                //
                for (i = 0; i < numplanes; i++) {
                    if ((trace[0].c.normal.oMultiply(planes[i])) > 0.999f) {
                        this.current.velocity.oPluSet(trace[0].c.normal);
                        break;
                    }
                }
                if (i < numplanes) {
                    continue;
                }
                planes[numplanes] = trace[0].c.normal;
                numplanes++;

                //
                // modify velocity so it parallels all of the clip planes
                //
                // find a plane that it enters
                for (i = 0; i < numplanes; i++) {
                    into = this.current.velocity.oMultiply(planes[i]);
                    if (into >= 0.1f) {
                        continue;		// move doesn't interact with the plane
                    }

                    // slide along the plane
                    clipVelocity.oSet(this.current.velocity);
                    clipVelocity.ProjectOntoPlane(planes[i], OVERCLIP);

                    // slide along the plane
                    endClipVelocity.oSet(endVelocity);
                    endClipVelocity.ProjectOntoPlane(planes[i], OVERCLIP);

                    // see if there is a second plane that the new move enters
                    for (j = 0; j < numplanes; j++) {
                        if (j == i) {
                            continue;
                        }
                        if ((clipVelocity.oMultiply(planes[j])) >= 0.1f) {
                            continue;		// move doesn't interact with the plane
                        }

                        // try clipping the move to the plane
                        clipVelocity.ProjectOntoPlane(planes[j], OVERCLIP);
                        endClipVelocity.ProjectOntoPlane(planes[j], OVERCLIP);

                        // see if it goes back into the first clip plane
                        if ((clipVelocity.oMultiply(planes[i])) >= 0) {
                            continue;
                        }

                        // slide the original velocity along the crease
                        dir = planes[i].Cross(planes[j]);
                        dir.Normalize();
                        d = dir.oMultiply(this.current.velocity);
                        clipVelocity.oSet(dir.oMultiply(d));

                        dir = planes[i].Cross(planes[j]);
                        dir.Normalize();
                        d = dir.oMultiply(endVelocity);
                        endClipVelocity.oSet(dir.oMultiply(d));

                        // see if there is a third plane the the new move enters
                        for (k = 0; k < numplanes; k++) {
                            if ((k == i) || (k == j)) {
                                continue;
                            }
                            if ((clipVelocity.oMultiply(planes[k])) >= 0.1f) {
                                continue;		// move doesn't interact with the plane
                            }

                            // stop dead at a tripple plane interaction
                            this.current.velocity.oSet(getVec3_origin());
                            return true;
                        }
                    }

                    // if we have fixed all interactions, try another move
                    this.current.velocity.oSet(clipVelocity);
                    endVelocity.oSet(endClipVelocity);
                    break;
                }
            }

            // step down
            if (stepDown && this.groundPlane) {
                stepEnd = this.current.origin.oPlus(this.gravityNormal.oMultiply(this.maxStepHeight));
                gameLocal.clip.Translation(downTrace, this.current.origin, stepEnd, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
                if ((downTrace[0].fraction > 1e-4f) && (downTrace[0].fraction < 1.0f)) {
                    this.current.stepUp -= (downTrace[0].endpos.oMinus(this.current.origin)).oMultiply(this.gravityNormal);
                    this.current.origin.oSet(downTrace[0].endpos);
                    this.current.movementFlags |= PMF_STEPPED_DOWN;
                    this.current.velocity.oMulSet(PM_STEPSCALE);
                }
            }

            if (gravity) {
                this.current.velocity.oSet(endVelocity);
            }

            // come to a dead stop when the velocity orthogonal to the gravity flipped
            clipVelocity = this.current.velocity.oMinus(this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal)));
            endClipVelocity = endVelocity.oMinus(this.gravityNormal.oMultiply(endVelocity.oMultiply(this.gravityNormal)));
            if (clipVelocity.oMultiply(endClipVelocity) < 0.0f) {
                this.current.velocity.oSet(this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal)));
            }

            return (bumpcount == 0);
        }

        /*
         ==================
         idPhysics_Player::Friction

         Handles both ground friction and water friction
         ==================
         */
        private void Friction() {
            idVec3 vel;
            float speed, newspeed, control;
            float drop;

            vel = this.current.velocity;
            if (this.walking) {
                // ignore slope movement, remove all velocity in gravity direction
                vel.oPluSet(this.gravityNormal.oMultiply(vel.oMultiply(this.gravityNormal)));
            }

            speed = vel.Length();
            if (speed < 1.0f) {
                // remove all movement orthogonal to gravity, allows for sinking underwater
                if (abs(this.current.velocity.oMultiply(this.gravityNormal)) < 1e-5f) {
                    this.current.velocity.Zero();
                } else {
                    this.current.velocity.oSet(this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal)));
                }
                // FIXME: still have z friction underwater?
                return;
            }

            drop = 0;

            // spectator friction
            if (this.current.movementType == etoi(PM_SPECTATOR)) {
                drop += speed * PM_FLYFRICTION * this.frametime;
            } // apply ground friction
            else if (this.walking && (etoi(this.waterLevel) <= etoi(WATERLEVEL_FEET))) {
                // no friction on slick surfaces
                if (!((this.groundMaterial != null) && ((this.groundMaterial.GetSurfaceFlags() & SURF_SLICK) != 0))) {
                    // if getting knocked back, no friction
                    if (0 == (this.current.movementFlags & PMF_TIME_KNOCKBACK)) {
                        control = speed < PM_STOPSPEED ? PM_STOPSPEED : speed;
                        drop += control * PM_FRICTION * this.frametime;
                    }
                }
            } // apply water friction even if just wading
            else if (this.waterLevel.ordinal() != 0) {//TODO:how can an enum be false?//FIXED:like that you dumbass.
                drop += speed * PM_WATERFRICTION * this.waterLevel.ordinal() * this.frametime;
            } // apply air friction
            else {
                drop += speed * PM_AIRFRICTION * this.frametime;
            }

            // scale the velocity
            newspeed = speed - drop;
            if (newspeed < 0) {
                newspeed = 0;
            }
            this.current.velocity.oMulSet(newspeed / speed);
        }

        /*
         ===================
         idPhysics_Player::WaterJumpMove

         Flying out of the water
         ===================
         */
        private void WaterJumpMove() {

            // waterjump has no control, but falls
            this.SlideMove(true, true, false, false);

            // add gravity
            this.current.velocity.oPluSet(this.gravityNormal.oMultiply(this.frametime));
            // if falling down
            if (this.current.velocity.oMultiply(this.gravityNormal) > 0.0f) {
                // cancel as soon as we are falling down again
                this.current.movementFlags &= ~PMF_ALL_TIMES;
                this.current.movementTime = 0;
            }
        }

        private void WaterMove() {
            idVec3 wishvel;
            float wishspeed;
            idVec3 wishdir;
            float scale;
            float vel;

            if (this.CheckWaterJump()) {
                this.WaterJumpMove();
                return;
            }

            this.Friction();

            scale = this.CmdScale(this.command);

            // user intentions
            if (0 == scale) {
                wishvel = this.gravityNormal.oMultiply(60); // sink towards bottom
            } else {
                wishvel = (this.viewForward.oMultiply(this.command.forwardmove).oPlus(this.viewRight.oMultiply(this.command.rightmove))).oMultiply(scale);
                wishvel.oMinSet(this.gravityNormal.oMultiply(this.command.upmove).oMultiply(scale));
            }

            wishdir = wishvel;
            wishspeed = wishdir.Normalize();

            if (wishspeed > (this.playerSpeed * PM_SWIMSCALE)) {
                wishspeed = this.playerSpeed * PM_SWIMSCALE;
            }

            this.Accelerate(wishdir, wishspeed, PM_WATERACCELERATE);

            // make sure we can go up slopes easily under water
            if (this.groundPlane && ((this.current.velocity.oMultiply(this.groundTrace.c.normal)) < 0.0f)) {
                vel = this.current.velocity.Length();
                // slide along the ground plane
                this.current.velocity.ProjectOntoPlane(this.groundTrace.c.normal, OVERCLIP);

                this.current.velocity.Normalize();
                this.current.velocity.oMulSet(vel);
            }

            this.SlideMove(false, true, false, false);
        }

        private void FlyMove() {
            idVec3 wishvel;
            float wishspeed;
            idVec3 wishdir;
            float scale;

            // normal slowdown
            this.Friction();

            scale = this.CmdScale(this.command);

            if (0 == scale) {
                wishvel = getVec3_origin();
            } else {
                wishvel = (this.viewForward.oMultiply(this.command.forwardmove).oPlus(this.viewRight.oMultiply(this.command.rightmove))).oMultiply(scale);
                wishvel.oMinSet(this.gravityNormal.oMultiply(this.command.upmove).oMultiply(scale));
            }

            wishdir = wishvel;
            wishspeed = wishdir.Normalize();

            this.Accelerate(wishdir, wishspeed, PM_FLYACCELERATE);

            this.SlideMove(false, false, false, false);
        }

        private void AirMove() {
            idVec3 wishvel;
            idVec3 wishdir;
            float wishspeed;
            float scale;

            this.Friction();

            scale = this.CmdScale(this.command);

            // project moves down to flat plane
            this.viewForward.oMinSet(this.gravityNormal.oMultiply(this.viewForward.oMultiply(this.gravityNormal)));
            this.viewRight.oMinSet(this.gravityNormal.oMultiply(this.viewRight.oMultiply(this.gravityNormal)));
            this.viewForward.Normalize();
            this.viewRight.Normalize();

            wishvel = (this.viewForward.oMultiply(this.command.forwardmove).oPlus(this.viewRight.oMultiply(this.command.rightmove))).oMultiply(scale);
            wishvel.oMinSet(this.gravityNormal.oMultiply(this.command.upmove).oMultiply(scale));
            wishdir = wishvel;
            wishspeed = wishdir.Normalize();
            wishspeed *= scale;

            // not on ground, so little effect on velocity
            this.Accelerate(wishdir, wishspeed, PM_AIRACCELERATE);

            // we may have a ground plane that is very steep, even
            // though we don't have a groundentity
            // slide along the steep plane
            if (this.groundPlane) {
                this.current.velocity.ProjectOntoPlane(this.groundTrace.c.normal, OVERCLIP);
            }

            this.SlideMove(true, false, false, false);
        }

        private void WalkMove() {
            idVec3 wishvel;
            idVec3 wishdir;
            float wishspeed;
            float scale;
            float accelerate;
            idVec3 oldVelocity, vel;
            float oldVel, newVel;

            if ((etoi(this.waterLevel) > etoi(WATERLEVEL_WAIST)) && ((this.viewForward.oMultiply(this.groundTrace.c.normal)) > 0.0f)) {
                // begin swimming
                this.WaterMove();
                return;
            }

            if (this.CheckJump()) {
                // jumped away
                if (etoi(this.waterLevel) > etoi(WATERLEVEL_FEET)) {
                    this.WaterMove();
                } else {
                    this.AirMove();
                }
                return;
            }

            this.Friction();

            scale = this.CmdScale(this.command);

            // project moves down to flat plane
            this.viewForward.oMinSet(this.gravityNormal.oMultiply(this.viewForward.oMultiply(this.gravityNormal)));
            this.viewRight.oMinSet(this.gravityNormal.oMultiply(this.viewRight.oMultiply(this.gravityNormal)));

            // project the forward and right directions onto the ground plane
            this.viewForward.ProjectOntoPlane(this.groundTrace.c.normal, OVERCLIP);
            this.viewRight.ProjectOntoPlane(this.groundTrace.c.normal, OVERCLIP);
            //
            this.viewForward.Normalize();
            this.viewRight.Normalize();

            wishvel = this.viewForward.oMultiply(this.command.forwardmove).oPlus(this.viewRight.oMultiply(this.command.rightmove));
            wishdir = wishvel;
            wishspeed = wishdir.Normalize();
            wishspeed *= scale;

            // clamp the speed lower if wading or walking on the bottom
            if (this.waterLevel != null) {
                float waterScale;

                waterScale = this.waterLevel.ordinal() / 3.0f;
                waterScale = 1.0f - ((1.0f - PM_SWIMSCALE) * waterScale);
                if (wishspeed > (this.playerSpeed * waterScale)) {
                    wishspeed = this.playerSpeed * waterScale;
                }
            }

            // when a player gets hit, they temporarily lose full control, which allows them to be moved a bit
            if (((this.groundMaterial != null) && ((this.groundMaterial.GetSurfaceFlags() & SURF_SLICK) != 0)) || ((this.current.movementFlags & PMF_TIME_KNOCKBACK) != 0)) {
                accelerate = PM_AIRACCELERATE;
            } else {
                accelerate = PM_ACCELERATE;
            }

            this.Accelerate(wishdir, wishspeed, accelerate);

            if (((this.groundMaterial != null) && ((this.groundMaterial.GetSurfaceFlags() & SURF_SLICK) != 0)) || ((this.current.movementFlags & PMF_TIME_KNOCKBACK) != 0)) {
                this.current.velocity.oPluSet(this.gravityVector.oMultiply(this.frametime));
            }

            oldVelocity = this.current.velocity;

            // slide along the ground plane
            this.current.velocity.ProjectOntoPlane(this.groundTrace.c.normal, OVERCLIP);

            // if not clipped into the opposite direction
            if (oldVelocity.oMultiply(this.current.velocity) > 0.0f) {
                newVel = this.current.velocity.LengthSqr();
                if (newVel > 1.0f) {
                    oldVel = oldVelocity.LengthSqr();
                    if (oldVel > 1.0f) {
                        // don't decrease velocity when going up or down a slope
                        this.current.velocity.oMulSet(idMath.Sqrt(oldVel / newVel));
                    }
                }
            }

            // don't do anything if standing still
            vel = this.current.velocity.oMinus(this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal)));
            if (0 == vel.LengthSqr()) {
                return;
            }

            gameLocal.push.InitSavingPushedEntityPositions();

            this.SlideMove(false, true, true, true);
        }

        private void DeadMove() {
            float forward;

            if (!this.walking) {
                return;
            }

            // extra friction
            forward = this.current.velocity.Length();
            forward -= 20;
            if (forward <= 0) {
                this.current.velocity.oSet(getVec3_origin());
            } else {
                this.current.velocity.Normalize();
                this.current.velocity.oMulSet(forward);
            }
        }

        private void NoclipMove() {
            float speed, drop, friction, newspeed, stopspeed;
            float scale, wishspeed;
            idVec3 wishdir;

            // friction
            speed = this.current.velocity.Length();
            if (speed < 20.0f) {
                this.current.velocity.oSet(getVec3_origin());
            } else {
                stopspeed = this.playerSpeed * 0.3f;
                if (speed < stopspeed) {
                    speed = stopspeed;
                }
                friction = PM_NOCLIPFRICTION;
                drop = speed * friction * this.frametime;

                // scale the velocity
                newspeed = speed - drop;
                if (newspeed < 0) {
                    newspeed = 0;
                }

                this.current.velocity.oMultiply(newspeed / speed);
            }

            // accelerate
            scale = this.CmdScale(this.command);

            wishdir = (this.viewForward.oMultiply(this.command.forwardmove).oPlus(this.viewRight.oMultiply(this.command.rightmove))).oMultiply(scale);
            wishdir.oMinSet(this.gravityNormal.oMultiply(this.command.upmove).oMultiply(scale));
            wishspeed = wishdir.Normalize();
            wishspeed *= scale;

            this.Accelerate(wishdir, wishspeed, PM_ACCELERATE);

            // move
            this.current.origin.oPluSet(this.current.velocity.oMultiply(this.frametime));
        }

        private void SpectatorMove() {
            idVec3 wishvel;
            float wishspeed;
            idVec3 wishdir;
            float scale;

            final trace_s trace;
            final idVec3 end;

            // fly movement
            this.Friction();

            scale = this.CmdScale(this.command);

            if (0 == scale) {
                wishvel = getVec3_origin();
            } else {
                wishvel = (this.viewForward.oMultiply(this.command.forwardmove).oPlus(this.viewRight.oMultiply(this.command.rightmove))).oMultiply(scale);
            }

            wishdir = wishvel;
            wishspeed = wishdir.Normalize();

            this.Accelerate(wishdir, wishspeed, PM_FLYACCELERATE);

            this.SlideMove(false, false, false, false);
        }

        private void LadderMove() {
            final idVec3 wishdir;
			idVec3 wishvel, right;
            float wishspeed, scale;
            float upscale;

            // stick to the ladder
            wishvel = this.ladderNormal.oMultiply(-100.0f);
            this.current.velocity.oSet((this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal))).oPlus(wishvel));

            upscale = (this.gravityNormal.oNegative().oMultiply(this.viewForward) + 0.5f) * 2.5f;
            if (upscale > 1.0f) {
                upscale = 1.0f;
            } else if (upscale < -1.0f) {
                upscale = -1.0f;
            }

            scale = this.CmdScale(this.command);
            wishvel = this.gravityNormal.oMultiply(upscale * scale * this.command.forwardmove * -0.9f);

            // strafe
            if (this.command.rightmove != 0) {
                // right vector orthogonal to gravity
                right = this.viewRight.oMinus(this.gravityNormal.oMultiply(this.viewRight.oMultiply(this.gravityNormal)));
                // project right vector into ladder plane
                right = right.oMinus(this.ladderNormal.oMultiply(right.oMultiply(this.ladderNormal)));
                right.Normalize();

                // if we are looking away from the ladder, reverse the right vector
                if (this.ladderNormal.oMultiply(this.viewForward) > 0.0f) {
                    right = right.oNegative();
                }
                wishvel.oPluSet(right.oMultiply(scale * this.command.rightmove * 2.0f));
            }

            // up down movement
            if (this.command.upmove != 0) {
                wishvel.oPluSet(this.gravityNormal.oMultiply(scale * this.command.upmove * -0.5f));
            }

            // do strafe friction
            this.Friction();

            // accelerate
            wishspeed = wishvel.Normalize();
            this.Accelerate(wishvel, wishspeed, PM_ACCELERATE);

            // cap the vertical velocity
            upscale = this.current.velocity.oMultiply(this.gravityNormal.oNegative());
            if (upscale < -PM_LADDERSPEED) {
                this.current.velocity.oPluSet(this.gravityNormal.oMultiply(upscale + PM_LADDERSPEED));
            } else if (upscale > PM_LADDERSPEED) {
                this.current.velocity.oPluSet(this.gravityNormal.oMultiply(upscale - PM_LADDERSPEED));
            }

            if ((wishvel.oMultiply(this.gravityNormal)) == 0.0f) {
                if (this.current.velocity.oMultiply(this.gravityNormal) < 0.0f) {
                    this.current.velocity.oPluSet(this.gravityVector.oMultiply(this.frametime));
                    if (this.current.velocity.oMultiply(this.gravityNormal) > 0.0f) {
                        this.current.velocity.oMinSet(this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal)));
                    }
                } else {
                    this.current.velocity.oMinSet(this.gravityVector.oMultiply(this.frametime));
                    if (this.current.velocity.oMultiply(this.gravityNormal) < 0.0f) {
                        this.current.velocity.oMinSet(this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal)));
                    }
                }
            }

            this.SlideMove(false, (this.command.forwardmove > 0), false, false);
        }

        private void CorrectAllSolid(trace_s trace, int contents) {
            if (this.debugLevel != 0) {
                gameLocal.Printf("%d:allsolid\n", c_pmove);
            }

            // FIXME: jitter around to find a free spot ?
            if (trace.fraction >= 1.0f) {
//		memset( &trace, 0, sizeof( trace ) );//TODO:init
                trace.endpos.oSet(this.current.origin);
                trace.endAxis.oSet(this.clipModelAxis);
                trace.fraction = 0.0f;
                trace.c.dist = this.current.origin.z;
                trace.c.normal.Set(0, 0, 1);
                trace.c.point.oSet(this.current.origin);
                trace.c.entityNum = ENTITYNUM_WORLD;
                trace.c.id = 0;
                trace.c.type = CONTACT_TRMVERTEX;
                trace.c.material = null;
                trace.c.contents = contents;
            }
        }

        private void CheckGround() {
            int i, contents;
            final idVec3 point;
            boolean hadGroundContacts;

            hadGroundContacts = HasGroundContacts();

            // set the clip model origin before getting the contacts
            this.clipModel.SetPosition(this.current.origin, this.clipModel.GetAxis());

            EvaluateContacts();

            // setup a ground trace from the contacts
            this.groundTrace.endpos.oSet(this.current.origin);
            this.groundTrace.endAxis.oSet(this.clipModel.GetAxis());
            if (this.contacts.Num() != 0) {
                this.groundTrace.fraction = 0.0f;
                this.groundTrace.c = this.contacts.oGet(0);
                for (i = 1; i < this.contacts.Num(); i++) {
                    this.groundTrace.c.normal.oPluSet(this.contacts.oGet(i).normal);
                }
                this.groundTrace.c.normal.Normalize();
            } else {
                this.groundTrace.fraction = 1.0f;
            }

            contents = gameLocal.clip.Contents(this.current.origin, this.clipModel, this.clipModel.GetAxis(), -1, this.self);
            if ((contents & MASK_SOLID) != 0) {
                // do something corrective if stuck in solid
                this.CorrectAllSolid(this.groundTrace, contents);
            }

            // if the trace didn't hit anything, we are in free fall
            if (this.groundTrace.fraction == 1.0f) {
                this.groundPlane = false;
                this.walking = false;
                this.groundEntityPtr = new idEntityPtr<>(null);
                return;
            }

            this.groundMaterial = this.groundTrace.c.material;
            this.groundEntityPtr.oSet(gameLocal.entities[this.groundTrace.c.entityNum]);

            // check if getting thrown off the ground
            if (((this.current.velocity.oMultiply(this.gravityNormal.oNegative())) > 0.0f) && ((this.current.velocity.oMultiply(this.groundTrace.c.normal)) > 10.0f)) {
                if (this.debugLevel != 0) {
                    gameLocal.Printf("%d:kickoff\n", c_pmove);
                }

                this.groundPlane = false;
                this.walking = false;
                return;
            }

            // slopes that are too steep will not be considered onground
            if ((this.groundTrace.c.normal.oMultiply(this.gravityNormal.oNegative())) < MIN_WALK_NORMAL) {
                if (this.debugLevel != 0) {
                    gameLocal.Printf("%d:steep\n", c_pmove);
                }

                // FIXME: if they can't slide down the slope, let them walk (sharp crevices)
                // make sure we don't die from sliding down a steep slope
                if (this.current.velocity.oMultiply(this.gravityNormal) > 150.0f) {
                    this.current.velocity.oMinSet(this.gravityNormal.oMultiply(this.current.velocity.oMultiply(this.gravityNormal) - 150.0f));
                }

                this.groundPlane = true;
                this.walking = false;
                return;
            }

            this.groundPlane = true;
            this.walking = true;

            // hitting solid ground will end a waterjump
            if ((this.current.movementFlags & PMF_TIME_WATERJUMP) != 0) {
                this.current.movementFlags &= ~(PMF_TIME_WATERJUMP | PMF_TIME_LAND);
                this.current.movementTime = 0;
            }

            // if the player didn't have ground contacts the previous frame
            if (!hadGroundContacts) {

                // don't do landing time if we were just going down a slope
                if ((this.current.velocity.oMultiply(this.gravityNormal.oNegative())) < -200.0f) {
                    // don't allow another jump for a little while
                    this.current.movementFlags |= PMF_TIME_LAND;
                    this.current.movementTime = 250;
                }
            }

            // let the entity know about the collision
            this.self.Collide(this.groundTrace, this.current.velocity);

            if (this.groundEntityPtr.GetEntity() != null) {
                final impactInfo_s info = this.groundEntityPtr.GetEntity().GetImpactInfo(this.self, this.groundTrace.c.id, this.groundTrace.c.point);
                if (info.invMass != 0.0f) {
                    this.groundEntityPtr.GetEntity().ApplyImpulse(this.self, this.groundTrace.c.id, this.groundTrace.c.point, this.current.velocity.oDivide(info.invMass * 10.0f));
                }
            }
        }

        /*
         ==============
         idPhysics_Player::CheckDuck

         Sets clip model size
         ==============
         */
        private void CheckDuck() {
            final trace_s[] trace = {null};
            idVec3 end;
            idBounds bounds;
            float maxZ;

            if (this.current.movementType == etoi(PM_DEAD)) {
                maxZ = pm_deadheight.GetFloat();
            } else {
                // stand up when up against a ladder
                if ((this.command.upmove < 0) && !this.ladder) {
                    // duck
                    this.current.movementFlags |= PMF_DUCKED;
                } else {
                    // stand up if possible
                    if ((this.current.movementFlags & PMF_DUCKED) != 0) {
                        // try to stand up
                        end = this.current.origin.oMinus(this.gravityNormal.oMultiply(pm_normalheight.GetFloat() - pm_crouchheight.GetFloat()));
                        gameLocal.clip.Translation(trace, this.current.origin, end, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
                        if (trace[0].fraction >= 1.0f) {
                            this.current.movementFlags &= ~PMF_DUCKED;
                        }
                    }
                }

                if ((this.current.movementFlags & PMF_DUCKED) != 0) {
                    this.playerSpeed = this.crouchSpeed;
                    maxZ = pm_crouchheight.GetFloat();
                } else {
                    maxZ = pm_normalheight.GetFloat();
                }
            }
            // if the clipModel height should change
            if (this.clipModel.GetBounds().oGet(1, 2) != maxZ) {

                bounds = this.clipModel.GetBounds();
                bounds.oSet(1, 2, maxZ);
                if (pm_usecylinder.GetBool()) {
                    this.clipModel.LoadModel(new idTraceModel(bounds, 8));
                } else {
                    this.clipModel.LoadModel(new idTraceModel(bounds));
                }
            }
        }

        private void CheckLadder() {
            idVec3 forward, start, end;
            final trace_s[] trace = {null};
            float tracedist;

            if (this.current.movementTime != 0) {
                return;
            }

            // if on the ground moving backwards
            if (this.walking && (this.command.forwardmove <= 0)) {
                return;
            }

            // forward vector orthogonal to gravity
            forward = this.viewForward.oMinus(this.gravityNormal.oMultiply(this.viewForward.oMultiply(this.gravityNormal)));
            forward.Normalize();

            if (this.walking) {
                // don't want to get sucked towards the ladder when still walking
                tracedist = 1.0f;
            } else {
                tracedist = 48.0f;
            }

            end = this.current.origin.oPlus(forward.oMultiply(tracedist));
            gameLocal.clip.Translation(trace, this.current.origin, end, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);

            // if near a surface
            if (trace[0].fraction < 1.0f) {

                // if a ladder surface
                if ((trace[0].c.material != null)
                        && ((trace[0].c.material.GetSurfaceFlags() & SURF_LADDER) != 0)) {

                    // check a step height higher
                    end = this.current.origin.oMinus(this.gravityNormal.oMultiply(this.maxStepHeight * 0.75f));
                    gameLocal.clip.Translation(trace, this.current.origin, end, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);
                    start = trace[0].endpos;
                    end = start.oPlus(forward.oMultiply(tracedist));
                    gameLocal.clip.Translation(trace, start, end, this.clipModel, this.clipModel.GetAxis(), this.clipMask, this.self);

                    // if also near a surface a step height higher
                    if (trace[0].fraction < 1.0f) {

                        // if it also is a ladder surface
                        if ((trace[0].c.material != null)
                                && ((trace[0].c.material.GetSurfaceFlags() & SURF_LADDER) != 0)) {
                            this.ladder = true;
                            this.ladderNormal.oSet(trace[0].c.normal);
                        }
                    }
                }
            }
        }

        private boolean CheckJump() {
            idVec3 addVelocity;

            if (this.command.upmove < 10) {
                // not holding jump
                return false;
            }

            // must wait for jump to be released
            if ((this.current.movementFlags & PMF_JUMP_HELD) != 0) {
                return false;
            }

            // don't jump if we can't stand up
            if ((this.current.movementFlags & PMF_DUCKED) != 0) {
                return false;
            }

            this.groundPlane = false;		// jumping away
            this.walking = false;
            this.current.movementFlags |= PMF_JUMP_HELD | PMF_JUMPED;

            addVelocity = this.gravityVector.oNegative().oMultiply(2.0f * this.maxJumpHeight);
            addVelocity.oMulSet(idMath.Sqrt(addVelocity.Normalize()));
            this.current.velocity.oPluSet(addVelocity);

            return true;
        }

        private boolean CheckWaterJump() {
            idVec3 spot;
            int cont;
            idVec3 flatforward;

            if (this.current.movementTime != 0) {
                return false;
            }

            // check for water jump
            if (this.waterLevel != WATERLEVEL_WAIST) {
                return false;
            }

            flatforward = this.viewForward.oMinus(this.gravityNormal.oMultiply(this.viewForward.oMultiply(this.gravityNormal)));
            flatforward.Normalize();

            spot = this.current.origin.oPlus(flatforward.oMultiply(30.0f));
            spot.oMinSet(this.gravityNormal.oMultiply(4.0f));
            cont = gameLocal.clip.Contents(spot, null, getMat3_identity(), -1, this.self);
            if (0 == (cont & CONTENTS_SOLID)) {
                return false;
            }

            spot.oMinSet(this.gravityNormal.oMultiply(16.0f));
            cont = gameLocal.clip.Contents(spot, null, getMat3_identity(), -1, this.self);
            if (cont != 0) {
                return false;
            }

            // jump out of water
            this.current.velocity.oSet(this.viewForward.oMultiply(200.0f).oMinus(this.gravityNormal.oMultiply(350.0f)));
            this.current.movementFlags |= PMF_TIME_WATERJUMP;
            this.current.movementTime = 2000;

            return true;
        }

        private void SetWaterLevel() {
            idVec3 point;
            idBounds bounds;
            int contents;

            //
            // get waterlevel, accounting for ducking
            //
            this.waterLevel = WATERLEVEL_NONE;
            this.waterType = 0;

            bounds = this.clipModel.GetBounds();

            // check at feet level
            point = this.current.origin.oMinus(this.gravityNormal.oMultiply(bounds.oGet(0, 2) + 1.0f));
            contents = gameLocal.clip.Contents(point, null, getMat3_identity(), -1, this.self);
            if ((contents & MASK_WATER) != 0) {

                this.waterType = contents;
                this.waterLevel = WATERLEVEL_FEET;

                // check at waist level
                point = this.current.origin.oMinus(this.gravityNormal.oMultiply((bounds.oGet(1, 2) - bounds.oGet(0, 2)) * 0.5f));
                contents = gameLocal.clip.Contents(point, null, getMat3_identity(), -1, this.self);
                if ((contents & MASK_WATER) != 0) {

                    this.waterLevel = WATERLEVEL_WAIST;

                    // check at head level
                    point = this.current.origin.oMinus(this.gravityNormal.oMultiply(bounds.oGet(1, 2) - 1.0f));
                    contents = gameLocal.clip.Contents(point, null, getMat3_identity(), -1, this.self);
                    if ((contents & MASK_WATER) != 0) {
                        this.waterLevel = WATERLEVEL_HEAD;
                    }
                }
            }
        }

        private void DropTimers() {
            // drop misc timing counter
            if (this.current.movementTime != 0) {
                if (this.framemsec >= this.current.movementTime) {
                    this.current.movementFlags &= ~PMF_ALL_TIMES;
                    this.current.movementTime = 0;
                } else {
                    this.current.movementTime -= this.framemsec;
                }
            }
        }

        private void MovePlayer(int msec) {

            // this counter lets us debug movement problems with a journal
            // by setting a conditional breakpoint for the previous frame
            c_pmove++;

            this.walking = false;
            this.groundPlane = false;
            this.ladder = false;

            // determine the time
            this.framemsec = msec;
            this.frametime = this.framemsec * 0.001f;

            // default speed
            this.playerSpeed = this.walkSpeed;

            // remove jumped and stepped up flag
            this.current.movementFlags &= ~(PMF_JUMPED | PMF_STEPPED_UP | PMF_STEPPED_DOWN);
            this.current.stepUp = 0.0f;

            if (this.command.upmove < 10) {
                // not holding jump
                this.current.movementFlags &= ~PMF_JUMP_HELD;
            }

            // if no movement at all
            if (this.current.movementType == etoi(PM_FREEZE)) {
                return;
            }

            // move the player velocity into the frame of a pusher
            this.current.velocity.oMinSet(this.current.pushVelocity);

            // view vectors
            this.viewAngles.ToVectors(this.viewForward, null, null);
            this.viewForward.oMulSet(this.clipModelAxis);
            this.viewRight.oSet(this.gravityNormal.Cross(this.viewForward));
            this.viewRight.Normalize();

            // fly in spectator mode
            if (this.current.movementType == etoi(PM_SPECTATOR)) {
                SpectatorMove();
                this.DropTimers();
                return;
            }

            // special no clip mode
            if (this.current.movementType == etoi(PM_NOCLIP)) {
                this.NoclipMove();
                this.DropTimers();
                return;
            }

            // no control when dead
            if (this.current.movementType == etoi(PM_DEAD)) {
                this.command.forwardmove = 0;
                this.command.rightmove = 0;
                this.command.upmove = 0;
            }

            // set watertype and waterlevel
            this.SetWaterLevel();

            // check for ground
            this.CheckGround();

            // check if up against a ladder
            this.CheckLadder();

            // set clip model size
            this.CheckDuck();

            // handle timers
            this.DropTimers();

            // move
            if (this.current.movementType == etoi(PM_DEAD)) {
                // dead
                this.DeadMove();
            } else if (this.ladder) {
                // going up or down a ladder
                this.LadderMove();
            } else if ((this.current.movementFlags & PMF_TIME_WATERJUMP) != 0) {
                // jumping out of water
                this.WaterJumpMove();
            } else if (etoi(this.waterLevel) > 1) {
                // swimming
                this.WaterMove();
            } else if (this.walking) {
                // walking on ground
                this.WalkMove();
            } else {
                // airborne
                this.AirMove();
            }

            // set watertype, waterlevel and groundentity
            this.SetWaterLevel();
            this.CheckGround();

            // move the player velocity back into the world frame
            this.current.velocity.oPluSet(this.current.pushVelocity);
            this.current.pushVelocity.Zero();
        }
    }

    /*
     ================
     idPhysics_Player_SavePState
     ================
     */
    static void idPhysics_Player_SavePState(idSaveGame savefile, final playerPState_s state) {
        savefile.WriteVec3(state.origin);
        savefile.WriteVec3(state.velocity);
        savefile.WriteVec3(state.localOrigin);
        savefile.WriteVec3(state.pushVelocity);
        savefile.WriteFloat(state.stepUp);
        savefile.WriteInt(state.movementType);
        savefile.WriteInt(state.movementFlags);
        savefile.WriteInt(state.movementTime);
    }

    /*
     ================
     idPhysics_Player_RestorePState
     ================
     */
    static void idPhysics_Player_RestorePState(idRestoreGame savefile, playerPState_s state) {
        savefile.ReadVec3(state.origin);
        savefile.ReadVec3(state.velocity);
        savefile.ReadVec3(state.localOrigin);
        savefile.ReadVec3(state.pushVelocity);
        state.stepUp = savefile.ReadFloat();
        state.movementType = savefile.ReadInt();
        state.movementFlags = savefile.ReadInt();
        state.movementTime = savefile.ReadInt();
    }
}
