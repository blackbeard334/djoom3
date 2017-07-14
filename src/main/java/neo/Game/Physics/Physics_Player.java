package neo.Game.Physics;

import static java.lang.Math.abs;
import static neo.CM.CollisionModel.contactType_t.CONTACT_TRMVERTEX;
import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.GameSys.SysCvar.pm_crouchheight;
import static neo.Game.GameSys.SysCvar.pm_deadheight;
import static neo.Game.GameSys.SysCvar.pm_normalheight;
import static neo.Game.GameSys.SysCvar.pm_usecylinder;
import static neo.Game.Game_local.ENTITYNUM_WORLD;
import static neo.Game.Game_local.MASK_SOLID;
import static neo.Game.Game_local.MASK_WATER;
import static neo.Game.Game_local.gameLocal;

import neo.Game.Game_local.idEntityPtr;
import neo.Game.Physics.Physics.impactInfo_s;
import neo.Game.Physics.Physics_Actor.idPhysics_Actor;
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
import neo.Renderer.Material.idMaterial;
import static neo.TempDump.btoi;
import static neo.TempDump.etoi;
import neo.framework.UsercmdGen.usercmd_t;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.geometry.TraceModel.idTraceModel;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

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
    };

    public enum waterLevel_t {

        WATERLEVEL_NONE,
        WATERLEVEL_FEET,
        WATERLEVEL_WAIST,
        WATERLEVEL_HEAD
    };
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
            origin = new idVec3();
            velocity = new idVec3();
            localOrigin = new idVec3();
            pushVelocity = new idVec3();
        }
    };
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
        private idAngles       viewAngles;
        //
        // run-time variables
        private int            framemsec;
        private float          frametime;
        private float          playerSpeed;
        private idVec3         viewForward;
        private idVec3         viewRight;
        //
        // walk movement
        private boolean        walking;
        private boolean        groundPlane;
        private trace_s        groundTrace;
        private idMaterial     groundMaterial;
        //
        // ladder movement
        private boolean        ladder;
        private idVec3         ladderNormal;
        //
        // results of last evaluate
        waterLevel_t waterLevel;
        int          waterType;
        //
        //

        public idPhysics_Player() {
            debugLevel = 0;//false;
            clipModel = null;
            clipMask = 0;
            current = new playerPState_s();//memset( &current, 0, sizeof( current ) );
            saved = current;
            walkSpeed = 0;
            crouchSpeed = 0;
            maxStepHeight = 0;
            maxJumpHeight = 0;
            command = new usercmd_t();//memset( &command, 0, sizeof( command ) );
            viewAngles = new idAngles();
            framemsec = 0;
            frametime = 0;
            playerSpeed = 0;
            viewForward = new idVec3();
            viewRight = new idVec3();
            walking = false;
            groundPlane = false;
            groundTrace = new trace_s();//memset( &groundTrace, 0, sizeof( groundTrace ) );
            groundMaterial = null;
            ladder = false;
            ladderNormal = new idVec3();
            waterLevel = WATERLEVEL_NONE;
            waterType = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {

            idPhysics_Player_SavePState(savefile, current);
            idPhysics_Player_SavePState(savefile, saved);

            savefile.WriteFloat(walkSpeed);
            savefile.WriteFloat(crouchSpeed);
            savefile.WriteFloat(maxStepHeight);
            savefile.WriteFloat(maxJumpHeight);
            savefile.WriteInt(debugLevel);

            savefile.WriteUsercmd(command);
            savefile.WriteAngles(viewAngles);

            savefile.WriteInt(framemsec);
            savefile.WriteFloat(frametime);
            savefile.WriteFloat(playerSpeed);
            savefile.WriteVec3(viewForward);
            savefile.WriteVec3(viewRight);

            savefile.WriteBool(walking);
            savefile.WriteBool(groundPlane);
            savefile.WriteTrace(groundTrace);
            savefile.WriteMaterial(groundMaterial);

            savefile.WriteBool(ladder);
            savefile.WriteVec3(ladderNormal);

            savefile.WriteInt(etoi(waterLevel));
            savefile.WriteInt(waterType);
        }

        @Override
        public void Restore(idRestoreGame savefile) {

            idPhysics_Player_RestorePState(savefile, current);
            idPhysics_Player_RestorePState(savefile, saved);

            walkSpeed = savefile.ReadFloat();
            crouchSpeed = savefile.ReadFloat();
            maxStepHeight = savefile.ReadFloat();
            maxJumpHeight = savefile.ReadFloat();
            debugLevel = savefile.ReadInt();

            savefile.ReadUsercmd(command);
            savefile.ReadAngles(viewAngles);

            framemsec = savefile.ReadInt();
            frametime = savefile.ReadFloat();
            playerSpeed = savefile.ReadFloat();
            savefile.ReadVec3(viewForward);
            savefile.ReadVec3(viewRight);

            walking = savefile.ReadBool();
            groundPlane = savefile.ReadBool();
            savefile.ReadTrace(groundTrace);
            savefile.ReadMaterial(groundMaterial);

            ladder = savefile.ReadBool();
            savefile.ReadVec3(ladderNormal);

            waterLevel = waterLevel_t.values()[savefile.ReadInt()];
            waterType = savefile.ReadInt();
        }

        // initialisation
        public void SetSpeed(final float newWalkSpeed, final float newCrouchSpeed) {
            walkSpeed = newWalkSpeed;
            crouchSpeed = newCrouchSpeed;
        }

        public void SetMaxStepHeight(final float newMaxStepHeight) {
            maxStepHeight = newMaxStepHeight;
        }

        public float GetMaxStepHeight() {
            return maxStepHeight;
        }

        public void SetMaxJumpHeight(final float newMaxJumpHeight) {
            maxJumpHeight = newMaxJumpHeight;
        }

        public void SetMovementType(final pmtype_t type) {
            current.movementType = etoi(type);
        }

        public void SetPlayerInput(final usercmd_t cmd, final idAngles newViewAngles) {
            command = cmd;
            viewAngles.oSet(newViewAngles);// can't use cmd.angles cause of the delta_angles
        }

        public void SetKnockBack(final int knockBackTime) {
            if (current.movementTime != 0) {
                return;
            }
            current.movementFlags |= PMF_TIME_KNOCKBACK;
            current.movementTime = knockBackTime;
        }

        public void SetDebugLevel(boolean set) {
            debugLevel = btoi(set);
        }

        // feed back from last physics frame
        public waterLevel_t GetWaterLevel() {
            return waterLevel;
        }

        public int GetWaterType() {
            return waterType;
        }

        public boolean HasJumped() {
            return ((current.movementFlags & PMF_JUMPED) != 0);
        }

        public boolean HasSteppedUp() {
            return ((current.movementFlags & (PMF_STEPPED_UP | PMF_STEPPED_DOWN)) != 0);
        }

        public float GetStepUp() {
            return current.stepUp;
        }

        public boolean IsCrouching() {
            return ((current.movementFlags & PMF_DUCKED) != 0);
        }

        public boolean OnLadder() {
            return ladder;
        }

        // != GetOrigin
        public idVec3 PlayerGetOrigin() {
            return current.origin;
        }

        @Override
        // common physics interface
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            idVec3 masterOrigin = new idVec3(), oldOrigin;
            idMat3 masterAxis = new idMat3();

            waterLevel = WATERLEVEL_NONE;
            waterType = 0;
            oldOrigin = new idVec3(current.origin);

            clipModel.Unlink();

            // if bound to a master
            if (masterEntity != null) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.origin.oSet(masterOrigin.oPlus(current.localOrigin.oMultiply(masterAxis)));
                clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());
                current.velocity.oSet((current.origin.oMinus(oldOrigin)).oDivide(timeStepMSec * 0.001f));
                masterDeltaYaw = masterYaw;
                masterYaw = masterAxis.oGet(0).ToYaw();
                masterDeltaYaw = masterYaw - masterDeltaYaw;
                return true;
            }

            ActivateContactEntities();

            this.MovePlayer(timeStepMSec);

            clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());

            if (IsOutsideWorld()) {
                gameLocal.Warning("clip model outside world bounds for entity '%s' at (%s)", self.name, current.origin.ToString(0));
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
            impactInfo_s info = new impactInfo_s();
            info.invMass = invMass;
            info.invInertiaTensor.Zero();
            info.position.Zero();
            info.velocity.oSet(current.velocity);
            return info;
        }

        @Override
        public void ApplyImpulse(final int id, final idVec3 point, final idVec3 impulse) {
            if (current.movementType != etoi(PM_NOCLIP)) {
                current.velocity.oPluSet(impulse.oMultiply(invMass));
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
            saved = current;
        }

        @Override
        public void RestoreState() {
            current = saved;

            clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());

            EvaluateContacts();
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.localOrigin.oSet(newOrigin);
            if (masterEntity != null) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.origin.oSet(masterOrigin.oPlus(newOrigin.oMultiply(masterAxis)));
            } else {
                current.origin.oSet(newOrigin);
            }

            clipModel.Link(gameLocal.clip, self, 0, newOrigin, clipModel.GetAxis());
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            clipModel.Link(gameLocal.clip, self, 0, clipModel.GetOrigin(), newAxis);
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {

            current.localOrigin.oPluSet(translation);
            current.origin.oPluSet(translation);

            clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.origin.oMulSet(rotation);
            if (masterEntity != null) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.localOrigin.oSet((current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
            } else {
                current.localOrigin.oSet(current.origin);
            }

            clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis().oMultiply(rotation.ToMat3()));
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            current.velocity.oSet(newLinearVelocity);
        }

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            return new idVec3(current.velocity);
        }

        @Override
        public void SetPushed(int deltaTime) {
            idVec3 velocity;
            float d;

            // velocity with which the player is pushed
            velocity = (current.origin.oMinus(saved.origin)).oDivide(deltaTime * idMath.M_MS2SEC);

            // remove any downward push velocity
            d = velocity.oMultiply(gravityNormal);
            if (d > 0.0f) {
                velocity.oMinSet(gravityNormal.oMultiply(d));
            }

            current.pushVelocity.oPluSet(velocity);
        }

        @Override
        public idVec3 GetPushedLinearVelocity(final int id /*= 0*/) {
            return current.pushVelocity;
        }

        public idVec3 GetPushedLinearVelocity() {
            return GetPushedLinearVelocity(0);
        }

        public void ClearPushedVelocity() {
            current.pushVelocity.Zero();
        }

        /*
         ================
         idPhysics_Player::SetMaster

         the binding is never orientated
         ================
         */
        @Override
        public void SetMaster(idEntity master, final boolean orientated /*= true*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (null == masterEntity) {
                    // transform from world space to master space
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    current.localOrigin.oSet((current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose()));
                    masterEntity = master;
                    masterYaw = masterAxis.oGet(0).ToYaw();
                }
                ClearContacts();
            } else {
                if (masterEntity != null) {
                    masterEntity = null;
                }
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteFloat(current.origin.oGet(0));
            msg.WriteFloat(current.origin.oGet(1));
            msg.WriteFloat(current.origin.oGet(2));
            msg.WriteFloat(current.velocity.oGet(0), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteFloat(current.velocity.oGet(1), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteFloat(current.velocity.oGet(2), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(current.origin.oGet(0), current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(current.origin.oGet(1), current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(current.origin.oGet(2), current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(0), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(1), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.pushVelocity.oGet(2), PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS);
            msg.WriteDeltaFloat(0.0f, current.stepUp);
            msg.WriteBits(current.movementType, PLAYER_MOVEMENT_TYPE_BITS);
            msg.WriteBits(current.movementFlags, PLAYER_MOVEMENT_FLAGS_BITS);
            msg.WriteDeltaLong(0, current.movementTime);
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            current.origin.oSet(0, msg.ReadFloat());
            current.origin.oSet(1, msg.ReadFloat());
            current.origin.oSet(2, msg.ReadFloat());
            current.velocity.oSet(0, msg.ReadFloat(PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            current.velocity.oSet(1, msg.ReadFloat(PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            current.velocity.oSet(2, msg.ReadFloat(PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            current.localOrigin.oSet(0, msg.ReadDeltaFloat(current.origin.oGet(0)));
            current.localOrigin.oSet(1, msg.ReadDeltaFloat(current.origin.oGet(1)));
            current.localOrigin.oSet(2, msg.ReadDeltaFloat(current.origin.oGet(2)));
            current.pushVelocity.oSet(0, msg.ReadDeltaFloat(0.0f, PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(1, msg.ReadDeltaFloat(0.0f, PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            current.pushVelocity.oSet(2, msg.ReadDeltaFloat(0.0f, PLAYER_VELOCITY_EXPONENT_BITS, PLAYER_VELOCITY_MANTISSA_BITS));
            current.stepUp = msg.ReadDeltaFloat(0.0f);
            current.movementType = msg.ReadBits(PLAYER_MOVEMENT_TYPE_BITS);
            current.movementFlags = msg.ReadBits(PLAYER_MOVEMENT_FLAGS_BITS);
            current.movementTime = msg.ReadDeltaLong(0);

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, clipModel.GetAxis());
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
            if (walking) {
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

            total = idMath.Sqrt((float) forwardmove * forwardmove + rightmove * rightmove + upmove * upmove);
            scale = (float) playerSpeed * max / (127.0f * total);

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

                currentspeed = current.velocity.oMultiply(wishdir);
                addspeed = wishspeed - currentspeed;
                if (addspeed <= 0) {
                    return;
                }
                accelspeed = accel * frametime * wishspeed;
                if (accelspeed > addspeed) {
                    accelspeed = addspeed;
                }

                current.velocity.oPluSet(wishdir.oMultiply(accelspeed));
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
            idVec3[] planes = new idVec3[MAX_CLIP_PLANES];
            idVec3 end, stepEnd, primal_velocity = new idVec3(), endVelocity = new idVec3(), endClipVelocity = new idVec3(), clipVelocity = new idVec3();
            trace_s[] trace = {null}, stepTrace = {null}, downTrace = {null};
            boolean nearGround, stepped, pushed;

            numbumps = 4;

            primal_velocity.oSet(current.velocity);

            if (gravity) {
                endVelocity.oSet(current.velocity.oPlus(gravityVector.oMultiply(frametime)));
                current.velocity.oSet((current.velocity.oPlus(endVelocity)).oMultiply(0.5f));
                primal_velocity.oSet(endVelocity);
                if (groundPlane) {
                    // slide along the ground plane
                    current.velocity.ProjectOntoPlane(groundTrace.c.normal, OVERCLIP);
                }
            } else {
                endVelocity.oSet(current.velocity);
            }

            time_left = frametime;

            // never turn against the ground plane
            if (groundPlane) {
                numplanes = 1;
                planes[0] = groundTrace.c.normal;
            } else {
                numplanes = 0;
            }

            // never turn against original velocity
            planes[numplanes] = new idVec3(current.velocity);
            planes[numplanes].Normalize();
            numplanes++;

            for (bumpcount = 0; bumpcount < numbumps; bumpcount++) {

                // calculate position we are trying to move to
                end = current.origin.oPlus(current.velocity.oMultiply(time_left));

                // see if we can make it there
                gameLocal.clip.Translation(trace, current.origin, end, clipModel, clipModel.GetAxis(), clipMask, self);

                time_left -= time_left * trace[0].fraction;
                current.origin.oSet(trace[0].endpos);

                // if moved the entire distance
                if (trace[0].fraction >= 1.0f) {
                    break;
                }

                stepped = pushed = false;

                // if we are allowed to step up
                if (stepUp) {

                    nearGround = groundPlane | ladder;

                    if (!nearGround) {
                        // trace down to see if the player is near the ground
                        // step checking when near the ground allows the player to move up stairs smoothly while jumping
                        stepEnd = current.origin.oPlus(gravityNormal.oMultiply(maxStepHeight));
                        gameLocal.clip.Translation(downTrace, current.origin, stepEnd, clipModel, clipModel.GetAxis(), clipMask, self);
                        nearGround = (downTrace[0].fraction < 1.0f && (downTrace[0].c.normal.oMultiply(gravityNormal.oNegative())) > MIN_WALK_NORMAL);
                    }

                    // may only step up if near the ground or on a ladder
                    if (nearGround) {

                        // step up
                        stepEnd = current.origin.oMinus(gravityNormal.oMultiply(maxStepHeight));
                        gameLocal.clip.Translation(downTrace, current.origin, stepEnd, clipModel, clipModel.GetAxis(), clipMask, self);

                        // trace along velocity
                        stepEnd = downTrace[0].endpos.oPlus(current.velocity.oMultiply(time_left));
                        gameLocal.clip.Translation(stepTrace, downTrace[0].endpos, stepEnd, clipModel, clipModel.GetAxis(), clipMask, self);

                        // step down
                        stepEnd = stepTrace[0].endpos.oPlus(gravityNormal.oMultiply(maxStepHeight));
                        gameLocal.clip.Translation(downTrace, stepTrace[0].endpos, stepEnd, clipModel, clipModel.GetAxis(), clipMask, self);

                        if (downTrace[0].fraction >= 1.0f || (downTrace[0].c.normal.oMultiply(gravityNormal.oNegative())) > MIN_WALK_NORMAL) {

                            // if moved the entire distance
                            if (stepTrace[0].fraction >= 1.0f) {
//                                time_left = 0;
                                current.stepUp -= (downTrace[0].endpos.oMinus(current.origin)).oMultiply(gravityNormal);
                                current.origin.oSet(downTrace[0].endpos);
                                current.movementFlags |= PMF_STEPPED_UP;
                                current.velocity.oMulSet(PM_STEPSCALE);
                                break;
                            }

                            // if the move is further when stepping up
                            if (stepTrace[0].fraction > trace[0].fraction) {
                                time_left -= time_left * stepTrace[0].fraction;
                                current.stepUp -= (downTrace[0].endpos.oMinus(current.origin)).oMultiply(gravityNormal);
                                current.origin.oSet(downTrace[0].endpos);
                                current.movementFlags |= PMF_STEPPED_UP;
                                current.velocity.oMulSet(PM_STEPSCALE);
                                trace[0] = stepTrace[0];
                                stepped = true;
                            }
                        }
                    }
                }

                // if we can push other entities and not blocked by the world
                if (push && trace[0].c.entityNum != ENTITYNUM_WORLD) {

                    clipModel.SetPosition(current.origin, clipModel.GetAxis());

                    // clip movement, only push idMoveables, don't push entities the player is standing on
                    // apply impact to pushed objects
                    pushFlags = PUSHFL_CLIP | PUSHFL_ONLYMOVEABLE | PUSHFL_NOGROUNDENTITIES | PUSHFL_APPLYIMPULSE;

                    // clip & push
                    totalMass = gameLocal.push.ClipTranslationalPush(trace, self, pushFlags, end, end.oMinus(current.origin));

                    if (totalMass > 0.0f) {
                        // decrease velocity based on the total mass of the objects being pushed ?
                        current.velocity.oMulSet(1.0f - idMath.ClampFloat(0.0f, 1000.0f, totalMass - 20.0f)
                                * (1.0f / 950.0f));
                        pushed = true;
                    }

                    current.origin.oSet(trace[0].endpos);
                    time_left -= time_left * trace[0].fraction;

                    // if moved the entire distance
                    if (trace[0].fraction >= 1.0f) {
                        break;
                    }
                }

                if (!stepped) {
                    // let the entity know about the collision
                    self.Collide(trace[0], current.velocity);
                }

                if (numplanes >= MAX_CLIP_PLANES) {
                    // MrElusive: I think we have some relatively high poly LWO models with a lot of slanted tris
                    // where it may hit the max clip planes
                    current.velocity.oSet(getVec3_origin());
                    return true;
                }

                //
                // if this is the same plane we hit before, nudge velocity
                // out along it, which fixes some epsilon issues with
                // non-axial planes
                //
                for (i = 0; i < numplanes; i++) {
                    if ((trace[0].c.normal.oMultiply(planes[i])) > 0.999f) {
                        current.velocity.oPluSet(trace[0].c.normal);
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
                    into = current.velocity.oMultiply(planes[i]);
                    if (into >= 0.1f) {
                        continue;		// move doesn't interact with the plane
                    }

                    // slide along the plane
                    clipVelocity.oSet(current.velocity);
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
                        d = dir.oMultiply(current.velocity);
                        clipVelocity.oSet(dir.oMultiply(d));

                        dir = planes[i].Cross(planes[j]);
                        dir.Normalize();
                        d = dir.oMultiply(endVelocity);
                        endClipVelocity.oSet(dir.oMultiply(d));

                        // see if there is a third plane the the new move enters
                        for (k = 0; k < numplanes; k++) {
                            if (k == i || k == j) {
                                continue;
                            }
                            if ((clipVelocity.oMultiply(planes[k])) >= 0.1f) {
                                continue;		// move doesn't interact with the plane
                            }

                            // stop dead at a tripple plane interaction
                            current.velocity.oSet(getVec3_origin());
                            return true;
                        }
                    }

                    // if we have fixed all interactions, try another move
                    current.velocity.oSet(clipVelocity);
                    endVelocity.oSet(endClipVelocity);
                    break;
                }
            }

            // step down
            if (stepDown && groundPlane) {
                stepEnd = current.origin.oPlus(gravityNormal.oMultiply(maxStepHeight));
                gameLocal.clip.Translation(downTrace, current.origin, stepEnd, clipModel, clipModel.GetAxis(), clipMask, self);
                if (downTrace[0].fraction > 1e-4f && downTrace[0].fraction < 1.0f) {
                    current.stepUp -= (downTrace[0].endpos.oMinus(current.origin)).oMultiply(gravityNormal);
                    current.origin.oSet(downTrace[0].endpos);
                    current.movementFlags |= PMF_STEPPED_DOWN;
                    current.velocity.oMulSet(PM_STEPSCALE);
                }
            }

            if (gravity) {
                current.velocity.oSet(endVelocity);
            }

            // come to a dead stop when the velocity orthogonal to the gravity flipped
            clipVelocity = current.velocity.oMinus(gravityNormal.oMultiply(current.velocity.oMultiply(gravityNormal)));
            endClipVelocity = endVelocity.oMinus(gravityNormal.oMultiply(endVelocity.oMultiply(gravityNormal)));
            if (clipVelocity.oMultiply(endClipVelocity) < 0.0f) {
                current.velocity.oSet(gravityNormal.oMultiply(current.velocity.oMultiply(gravityNormal)));
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

            vel = current.velocity;
            if (walking) {
                // ignore slope movement, remove all velocity in gravity direction
                vel.oPluSet(gravityNormal.oMultiply(vel.oMultiply(gravityNormal)));
            }

            speed = vel.Length();
            if (speed < 1.0f) {
                // remove all movement orthogonal to gravity, allows for sinking underwater
                if (abs(current.velocity.oMultiply(gravityNormal)) < 1e-5f) {
                    current.velocity.Zero();
                } else {
                    current.velocity.oSet(gravityNormal.oMultiply(current.velocity.oMultiply(gravityNormal)));
                }
                // FIXME: still have z friction underwater?
                return;
            }

            drop = 0;

            // spectator friction
            if (current.movementType == etoi(PM_SPECTATOR)) {
                drop += speed * PM_FLYFRICTION * frametime;
            } // apply ground friction
            else if (walking && etoi(waterLevel) <= etoi(WATERLEVEL_FEET)) {
                // no friction on slick surfaces
                if (!(groundMaterial != null && ((groundMaterial.GetSurfaceFlags() & SURF_SLICK) != 0))) {
                    // if getting knocked back, no friction
                    if (0 == (current.movementFlags & PMF_TIME_KNOCKBACK)) {
                        control = speed < PM_STOPSPEED ? PM_STOPSPEED : speed;
                        drop += control * PM_FRICTION * frametime;
                    }
                }
            } // apply water friction even if just wading
            else if (waterLevel.ordinal() != 0) {//TODO:how can an enum be false?//FIXED:like that you dumbass.
                drop += speed * PM_WATERFRICTION * waterLevel.ordinal() * frametime;
            } // apply air friction
            else {
                drop += speed * PM_AIRFRICTION * frametime;
            }

            // scale the velocity
            newspeed = speed - drop;
            if (newspeed < 0) {
                newspeed = 0;
            }
            current.velocity.oMulSet(newspeed / speed);
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
            current.velocity.oPluSet(gravityNormal.oMultiply(frametime));
            // if falling down
            if (current.velocity.oMultiply(gravityNormal) > 0.0f) {
                // cancel as soon as we are falling down again
                current.movementFlags &= ~PMF_ALL_TIMES;
                current.movementTime = 0;
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

            scale = this.CmdScale(command);

            // user intentions
            if (0 == scale) {
                wishvel = gravityNormal.oMultiply(60); // sink towards bottom
            } else {
                wishvel = (viewForward.oMultiply(command.forwardmove).oPlus(viewRight.oMultiply(command.rightmove))).oMultiply(scale);
                wishvel.oMinSet(gravityNormal.oMultiply(command.upmove).oMultiply(scale));
            }

            wishdir = wishvel;
            wishspeed = wishdir.Normalize();

            if (wishspeed > playerSpeed * PM_SWIMSCALE) {
                wishspeed = playerSpeed * PM_SWIMSCALE;
            }

            this.Accelerate(wishdir, wishspeed, PM_WATERACCELERATE);

            // make sure we can go up slopes easily under water
            if (groundPlane && (current.velocity.oMultiply(groundTrace.c.normal)) < 0.0f) {
                vel = current.velocity.Length();
                // slide along the ground plane
                current.velocity.ProjectOntoPlane(groundTrace.c.normal, OVERCLIP);

                current.velocity.Normalize();
                current.velocity.oMulSet(vel);
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

            scale = this.CmdScale(command);

            if (0 == scale) {
                wishvel = getVec3_origin();
            } else {
                wishvel = (viewForward.oMultiply(command.forwardmove).oPlus(viewRight.oMultiply(command.rightmove))).oMultiply(scale);
                wishvel.oMinSet(gravityNormal.oMultiply(command.upmove).oMultiply(scale));
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

            scale = this.CmdScale(command);

            // project moves down to flat plane
            viewForward.oMinSet(gravityNormal.oMultiply(viewForward.oMultiply(gravityNormal)));
            viewRight.oMinSet(gravityNormal.oMultiply(viewRight.oMultiply(gravityNormal)));
            viewForward.Normalize();
            viewRight.Normalize();

            wishvel = (viewForward.oMultiply(command.forwardmove).oPlus(viewRight.oMultiply(command.rightmove))).oMultiply(scale);
            wishvel.oMinSet(gravityNormal.oMultiply(command.upmove).oMultiply(scale));
            wishdir = wishvel;
            wishspeed = wishdir.Normalize();
            wishspeed *= scale;

            // not on ground, so little effect on velocity
            this.Accelerate(wishdir, wishspeed, PM_AIRACCELERATE);

            // we may have a ground plane that is very steep, even
            // though we don't have a groundentity
            // slide along the steep plane
            if (groundPlane) {
                current.velocity.ProjectOntoPlane(groundTrace.c.normal, OVERCLIP);
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

            if (etoi(waterLevel) > etoi(WATERLEVEL_WAIST) && (viewForward.oMultiply(groundTrace.c.normal)) > 0.0f) {
                // begin swimming
                this.WaterMove();
                return;
            }

            if (this.CheckJump()) {
                // jumped away
                if (etoi(waterLevel) > etoi(WATERLEVEL_FEET)) {
                    this.WaterMove();
                } else {
                    this.AirMove();
                }
                return;
            }

            this.Friction();

            scale = this.CmdScale(command);

            // project moves down to flat plane
            viewForward.oMinSet(gravityNormal.oMultiply(viewForward.oMultiply(gravityNormal)));
            viewRight.oMinSet(gravityNormal.oMultiply(viewRight.oMultiply(gravityNormal)));

            // project the forward and right directions onto the ground plane
            viewForward.ProjectOntoPlane(groundTrace.c.normal, OVERCLIP);
            viewRight.ProjectOntoPlane(groundTrace.c.normal, OVERCLIP);
            //
            viewForward.Normalize();
            viewRight.Normalize();

            wishvel = viewForward.oMultiply(command.forwardmove).oPlus(viewRight.oMultiply(command.rightmove));
            wishdir = wishvel;
            wishspeed = wishdir.Normalize();
            wishspeed *= scale;

            // clamp the speed lower if wading or walking on the bottom
            if (waterLevel != null) {
                float waterScale;

                waterScale = waterLevel.ordinal() / 3.0f;
                waterScale = 1.0f - (1.0f - PM_SWIMSCALE) * waterScale;
                if (wishspeed > playerSpeed * waterScale) {
                    wishspeed = playerSpeed * waterScale;
                }
            }

            // when a player gets hit, they temporarily lose full control, which allows them to be moved a bit
            if ((groundMaterial != null && ((groundMaterial.GetSurfaceFlags() & SURF_SLICK) != 0)) || ((current.movementFlags & PMF_TIME_KNOCKBACK) != 0)) {
                accelerate = PM_AIRACCELERATE;
            } else {
                accelerate = PM_ACCELERATE;
            }

            this.Accelerate(wishdir, wishspeed, accelerate);

            if ((groundMaterial != null && ((groundMaterial.GetSurfaceFlags() & SURF_SLICK) != 0)) || ((current.movementFlags & PMF_TIME_KNOCKBACK) != 0)) {
                current.velocity.oPluSet(gravityVector.oMultiply(frametime));
            }

            oldVelocity = current.velocity;

            // slide along the ground plane
            current.velocity.ProjectOntoPlane(groundTrace.c.normal, OVERCLIP);

            // if not clipped into the opposite direction
            if (oldVelocity.oMultiply(current.velocity) > 0.0f) {
                newVel = current.velocity.LengthSqr();
                if (newVel > 1.0f) {
                    oldVel = oldVelocity.LengthSqr();
                    if (oldVel > 1.0f) {
                        // don't decrease velocity when going up or down a slope
                        current.velocity.oMulSet(idMath.Sqrt(oldVel / newVel));
                    }
                }
            }

            // don't do anything if standing still
            vel = current.velocity.oMinus(gravityNormal.oMultiply(current.velocity.oMultiply(gravityNormal)));
            if (0 == vel.LengthSqr()) {
                return;
            }

            gameLocal.push.InitSavingPushedEntityPositions();

            this.SlideMove(false, true, true, true);
        }

        private void DeadMove() {
            float forward;

            if (!walking) {
                return;
            }

            // extra friction
            forward = current.velocity.Length();
            forward -= 20;
            if (forward <= 0) {
                current.velocity.oSet(getVec3_origin());
            } else {
                current.velocity.Normalize();
                current.velocity.oMulSet(forward);
            }
        }

        private void NoclipMove() {
            float speed, drop, friction, newspeed, stopspeed;
            float scale, wishspeed;
            idVec3 wishdir;

            // friction
            speed = current.velocity.Length();
            if (speed < 20.0f) {
                current.velocity.oSet(getVec3_origin());
            } else {
                stopspeed = playerSpeed * 0.3f;
                if (speed < stopspeed) {
                    speed = stopspeed;
                }
                friction = PM_NOCLIPFRICTION;
                drop = speed * friction * frametime;

                // scale the velocity
                newspeed = speed - drop;
                if (newspeed < 0) {
                    newspeed = 0;
                }

                current.velocity.oMultiply(newspeed / speed);
            }

            // accelerate
            scale = this.CmdScale(command);

            wishdir = (viewForward.oMultiply(command.forwardmove).oPlus(viewRight.oMultiply(command.rightmove))).oMultiply(scale);
            wishdir.oMinSet(gravityNormal.oMultiply(command.upmove).oMultiply(scale));
            wishspeed = wishdir.Normalize();
            wishspeed *= scale;

            this.Accelerate(wishdir, wishspeed, PM_ACCELERATE);

            // move
            current.origin.oPluSet(current.velocity.oMultiply(frametime));
        }

        private void SpectatorMove() {
            idVec3 wishvel;
            float wishspeed;
            idVec3 wishdir;
            float scale;

            trace_s trace;
            idVec3 end;

            // fly movement
            this.Friction();

            scale = this.CmdScale(command);

            if (0 == scale) {
                wishvel = getVec3_origin();
            } else {
                wishvel = (viewForward.oMultiply(command.forwardmove).oPlus(viewRight.oMultiply(command.rightmove))).oMultiply(scale);
            }

            wishdir = wishvel;
            wishspeed = wishdir.Normalize();

            this.Accelerate(wishdir, wishspeed, PM_FLYACCELERATE);

            this.SlideMove(false, false, false, false);
        }

        private void LadderMove() {
            idVec3 wishdir, wishvel, right;
            float wishspeed, scale;
            float upscale;

            // stick to the ladder
            wishvel = ladderNormal.oMultiply(-100.0f);
            current.velocity.oSet((gravityNormal.oMultiply(current.velocity.oMultiply(gravityNormal))).oPlus(wishvel));

            upscale = (gravityNormal.oNegative().oMultiply(viewForward) + 0.5f) * 2.5f;
            if (upscale > 1.0f) {
                upscale = 1.0f;
            } else if (upscale < -1.0f) {
                upscale = -1.0f;
            }

            scale = this.CmdScale(command);
            wishvel = gravityNormal.oMultiply(upscale * scale * (float) command.forwardmove * -0.9f);

            // strafe
            if (command.rightmove != 0) {
                // right vector orthogonal to gravity
                right = viewRight.oMinus(gravityNormal.oMultiply(viewRight.oMultiply(gravityNormal)));
                // project right vector into ladder plane
                right = right.oMinus(ladderNormal.oMultiply(right.oMultiply(ladderNormal)));
                right.Normalize();

                // if we are looking away from the ladder, reverse the right vector
                if (ladderNormal.oMultiply(viewForward) > 0.0f) {
                    right = right.oNegative();
                }
                wishvel.oPluSet(right.oMultiply(scale * (float) command.rightmove * 2.0f));
            }

            // up down movement
            if (command.upmove != 0) {
                wishvel.oPluSet(gravityNormal.oMultiply(scale * (float) command.upmove * -0.5f));
            }

            // do strafe friction
            this.Friction();

            // accelerate
            wishspeed = wishvel.Normalize();
            this.Accelerate(wishvel, wishspeed, PM_ACCELERATE);

            // cap the vertical velocity
            upscale = current.velocity.oMultiply(gravityNormal.oNegative());
            if (upscale < -PM_LADDERSPEED) {
                current.velocity.oPluSet(gravityNormal.oMultiply(upscale + PM_LADDERSPEED));
            } else if (upscale > PM_LADDERSPEED) {
                current.velocity.oPluSet(gravityNormal.oMultiply(upscale - PM_LADDERSPEED));
            }

            if ((wishvel.oMultiply(gravityNormal)) == 0.0f) {
                if (current.velocity.oMultiply(gravityNormal) < 0.0f) {
                    current.velocity.oPluSet(gravityVector.oMultiply(frametime));
                    if (current.velocity.oMultiply(gravityNormal) > 0.0f) {
                        current.velocity.oMinSet(gravityNormal.oMultiply(current.velocity.oMultiply(gravityNormal)));
                    }
                } else {
                    current.velocity.oMinSet(gravityVector.oMultiply(frametime));
                    if (current.velocity.oMultiply(gravityNormal) < 0.0f) {
                        current.velocity.oMinSet(gravityNormal.oMultiply(current.velocity.oMultiply(gravityNormal)));
                    }
                }
            }

            this.SlideMove(false, (command.forwardmove > 0), false, false);
        }

        private void CorrectAllSolid(trace_s trace, int contents) {
            if (debugLevel != 0) {
                gameLocal.Printf("%d:allsolid\n", c_pmove);
            }

            // FIXME: jitter around to find a free spot ?
            if (trace.fraction >= 1.0f) {
//		memset( &trace, 0, sizeof( trace ) );//TODO:init
                trace.endpos.oSet(current.origin);
                trace.endAxis.oSet(clipModelAxis);
                trace.fraction = 0.0f;
                trace.c.dist = current.origin.z;
                trace.c.normal.Set(0, 0, 1);
                trace.c.point.oSet(current.origin);
                trace.c.entityNum = ENTITYNUM_WORLD;
                trace.c.id = 0;
                trace.c.type = CONTACT_TRMVERTEX;
                trace.c.material = null;
                trace.c.contents = contents;
            }
        }

        private void CheckGround() {
            int i, contents;
            idVec3 point;
            boolean hadGroundContacts;

            hadGroundContacts = HasGroundContacts();

            // set the clip model origin before getting the contacts
            clipModel.SetPosition(current.origin, clipModel.GetAxis());

            EvaluateContacts();

            // setup a ground trace from the contacts
            groundTrace.endpos.oSet(current.origin);
            groundTrace.endAxis.oSet(clipModel.GetAxis());
            if (contacts.Num() != 0) {
                groundTrace.fraction = 0.0f;
                groundTrace.c = contacts.oGet(0);
                for (i = 1; i < contacts.Num(); i++) {
                    groundTrace.c.normal.oPluSet(contacts.oGet(i).normal);
                }
                groundTrace.c.normal.Normalize();
            } else {
                groundTrace.fraction = 1.0f;
            }

            contents = gameLocal.clip.Contents(current.origin, clipModel, clipModel.GetAxis(), -1, self);
            if ((contents & MASK_SOLID) != 0) {
                // do something corrective if stuck in solid
                this.CorrectAllSolid(groundTrace, contents);
            }

            // if the trace didn't hit anything, we are in free fall
            if (groundTrace.fraction == 1.0f) {
                groundPlane = false;
                walking = false;
                groundEntityPtr = new idEntityPtr<>(null);
                return;
            }

            groundMaterial = groundTrace.c.material;
            groundEntityPtr.oSet(gameLocal.entities[groundTrace.c.entityNum]);

            // check if getting thrown off the ground
            if ((current.velocity.oMultiply(gravityNormal.oNegative())) > 0.0f && (current.velocity.oMultiply(groundTrace.c.normal)) > 10.0f) {
                if (debugLevel != 0) {
                    gameLocal.Printf("%d:kickoff\n", c_pmove);
                }

                groundPlane = false;
                walking = false;
                return;
            }

            // slopes that are too steep will not be considered onground
            if ((groundTrace.c.normal.oMultiply(gravityNormal.oNegative())) < MIN_WALK_NORMAL) {
                if (debugLevel != 0) {
                    gameLocal.Printf("%d:steep\n", c_pmove);
                }

                // FIXME: if they can't slide down the slope, let them walk (sharp crevices)
                // make sure we don't die from sliding down a steep slope
                if (current.velocity.oMultiply(gravityNormal) > 150.0f) {
                    current.velocity.oMinSet(gravityNormal.oMultiply(current.velocity.oMultiply(gravityNormal) - 150.0f));
                }

                groundPlane = true;
                walking = false;
                return;
            }

            groundPlane = true;
            walking = true;

            // hitting solid ground will end a waterjump
            if ((current.movementFlags & PMF_TIME_WATERJUMP) != 0) {
                current.movementFlags &= ~(PMF_TIME_WATERJUMP | PMF_TIME_LAND);
                current.movementTime = 0;
            }

            // if the player didn't have ground contacts the previous frame
            if (!hadGroundContacts) {

                // don't do landing time if we were just going down a slope
                if ((current.velocity.oMultiply(gravityNormal.oNegative())) < -200.0f) {
                    // don't allow another jump for a little while
                    current.movementFlags |= PMF_TIME_LAND;
                    current.movementTime = 250;
                }
            }

            // let the entity know about the collision
            self.Collide(groundTrace, current.velocity);

            if (groundEntityPtr.GetEntity() != null) {
                impactInfo_s info = groundEntityPtr.GetEntity().GetImpactInfo(self, groundTrace.c.id, groundTrace.c.point);
                if (info.invMass != 0.0f) {
                    groundEntityPtr.GetEntity().ApplyImpulse(self, groundTrace.c.id, groundTrace.c.point, current.velocity.oDivide(info.invMass * 10.0f));
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
            trace_s[] trace = {null};
            idVec3 end;
            idBounds bounds;
            float maxZ;

            if (current.movementType == etoi(PM_DEAD)) {
                maxZ = pm_deadheight.GetFloat();
            } else {
                // stand up when up against a ladder
                if (command.upmove < 0 && !ladder) {
                    // duck
                    current.movementFlags |= PMF_DUCKED;
                } else {
                    // stand up if possible
                    if ((current.movementFlags & PMF_DUCKED) != 0) {
                        // try to stand up
                        end = current.origin.oMinus(gravityNormal.oMultiply(pm_normalheight.GetFloat() - pm_crouchheight.GetFloat()));
                        gameLocal.clip.Translation(trace, current.origin, end, clipModel, clipModel.GetAxis(), clipMask, self);
                        if (trace[0].fraction >= 1.0f) {
                            current.movementFlags &= ~PMF_DUCKED;
                        }
                    }
                }

                if ((current.movementFlags & PMF_DUCKED) != 0) {
                    playerSpeed = crouchSpeed;
                    maxZ = pm_crouchheight.GetFloat();
                } else {
                    maxZ = pm_normalheight.GetFloat();
                }
            }
            // if the clipModel height should change
            if (clipModel.GetBounds().oGet(1, 2) != maxZ) {

                bounds = clipModel.GetBounds();
                bounds.oSet(1, 2, maxZ);
                if (pm_usecylinder.GetBool()) {
                    clipModel.LoadModel(new idTraceModel(bounds, 8));
                } else {
                    clipModel.LoadModel(new idTraceModel(bounds));
                }
            }
        }

        private void CheckLadder() {
            idVec3 forward, start, end;
            trace_s[] trace = {null};
            float tracedist;

            if (current.movementTime != 0) {
                return;
            }

            // if on the ground moving backwards
            if (walking && command.forwardmove <= 0) {
                return;
            }

            // forward vector orthogonal to gravity
            forward = viewForward.oMinus(gravityNormal.oMultiply(viewForward.oMultiply(gravityNormal)));
            forward.Normalize();

            if (walking) {
                // don't want to get sucked towards the ladder when still walking
                tracedist = 1.0f;
            } else {
                tracedist = 48.0f;
            }

            end = current.origin.oPlus(forward.oMultiply(tracedist));
            gameLocal.clip.Translation(trace, current.origin, end, clipModel, clipModel.GetAxis(), clipMask, self);

            // if near a surface
            if (trace[0].fraction < 1.0f) {

                // if a ladder surface
                if (trace[0].c.material != null
                        && ((trace[0].c.material.GetSurfaceFlags() & SURF_LADDER) != 0)) {

                    // check a step height higher
                    end = current.origin.oMinus(gravityNormal.oMultiply(maxStepHeight * 0.75f));
                    gameLocal.clip.Translation(trace, current.origin, end, clipModel, clipModel.GetAxis(), clipMask, self);
                    start = trace[0].endpos;
                    end = start.oPlus(forward.oMultiply(tracedist));
                    gameLocal.clip.Translation(trace, start, end, clipModel, clipModel.GetAxis(), clipMask, self);

                    // if also near a surface a step height higher
                    if (trace[0].fraction < 1.0f) {

                        // if it also is a ladder surface
                        if (trace[0].c.material != null
                                && ((trace[0].c.material.GetSurfaceFlags() & SURF_LADDER) != 0)) {
                            ladder = true;
                            ladderNormal.oSet(trace[0].c.normal);
                        }
                    }
                }
            }
        }

        private boolean CheckJump() {
            idVec3 addVelocity;

            if (command.upmove < 10) {
                // not holding jump
                return false;
            }

            // must wait for jump to be released
            if ((current.movementFlags & PMF_JUMP_HELD) != 0) {
                return false;
            }

            // don't jump if we can't stand up
            if ((current.movementFlags & PMF_DUCKED) != 0) {
                return false;
            }

            groundPlane = false;		// jumping away
            walking = false;
            current.movementFlags |= PMF_JUMP_HELD | PMF_JUMPED;

            addVelocity = gravityVector.oNegative().oMultiply(2.0f * maxJumpHeight);
            addVelocity.oMulSet(idMath.Sqrt(addVelocity.Normalize()));
            current.velocity.oPluSet(addVelocity);

            return true;
        }

        private boolean CheckWaterJump() {
            idVec3 spot;
            int cont;
            idVec3 flatforward;

            if (current.movementTime != 0) {
                return false;
            }

            // check for water jump
            if (waterLevel != WATERLEVEL_WAIST) {
                return false;
            }

            flatforward = viewForward.oMinus(gravityNormal.oMultiply(viewForward.oMultiply(gravityNormal)));
            flatforward.Normalize();

            spot = current.origin.oPlus(flatforward.oMultiply(30.0f));
            spot.oMinSet(gravityNormal.oMultiply(4.0f));
            cont = gameLocal.clip.Contents(spot, null, getMat3_identity(), -1, self);
            if (0 == (cont & CONTENTS_SOLID)) {
                return false;
            }

            spot.oMinSet(gravityNormal.oMultiply(16.0f));
            cont = gameLocal.clip.Contents(spot, null, getMat3_identity(), -1, self);
            if (cont != 0) {
                return false;
            }

            // jump out of water
            current.velocity.oSet(viewForward.oMultiply(200.0f).oMinus(gravityNormal.oMultiply(350.0f)));
            current.movementFlags |= PMF_TIME_WATERJUMP;
            current.movementTime = 2000;

            return true;
        }

        private void SetWaterLevel() {
            idVec3 point;
            idBounds bounds;
            int contents;

            //
            // get waterlevel, accounting for ducking
            //
            waterLevel = WATERLEVEL_NONE;
            waterType = 0;

            bounds = clipModel.GetBounds();

            // check at feet level
            point = current.origin.oMinus(gravityNormal.oMultiply(bounds.oGet(0, 2) + 1.0f));
            contents = gameLocal.clip.Contents(point, null, getMat3_identity(), -1, self);
            if ((contents & MASK_WATER) != 0) {

                waterType = contents;
                waterLevel = WATERLEVEL_FEET;

                // check at waist level
                point = current.origin.oMinus(gravityNormal.oMultiply((bounds.oGet(1, 2) - bounds.oGet(0, 2)) * 0.5f));
                contents = gameLocal.clip.Contents(point, null, getMat3_identity(), -1, self);
                if ((contents & MASK_WATER) != 0) {

                    waterLevel = WATERLEVEL_WAIST;

                    // check at head level
                    point = current.origin.oMinus(gravityNormal.oMultiply(bounds.oGet(1, 2) - 1.0f));
                    contents = gameLocal.clip.Contents(point, null, getMat3_identity(), -1, self);
                    if ((contents & MASK_WATER) != 0) {
                        waterLevel = WATERLEVEL_HEAD;
                    }
                }
            }
        }

        private void DropTimers() {
            // drop misc timing counter
            if (current.movementTime != 0) {
                if (framemsec >= current.movementTime) {
                    current.movementFlags &= ~PMF_ALL_TIMES;
                    current.movementTime = 0;
                } else {
                    current.movementTime -= framemsec;
                }
            }
        }

        private void MovePlayer(int msec) {

            // this counter lets us debug movement problems with a journal
            // by setting a conditional breakpoint for the previous frame
            c_pmove++;

            walking = false;
            groundPlane = false;
            ladder = false;

            // determine the time
            framemsec = msec;
            frametime = framemsec * 0.001f;

            // default speed
            playerSpeed = walkSpeed;

            // remove jumped and stepped up flag
            current.movementFlags &= ~(PMF_JUMPED | PMF_STEPPED_UP | PMF_STEPPED_DOWN);
            current.stepUp = 0.0f;

            if (command.upmove < 10) {
                // not holding jump
                current.movementFlags &= ~PMF_JUMP_HELD;
            }

            // if no movement at all
            if (current.movementType == etoi(PM_FREEZE)) {
                return;
            }

            // move the player velocity into the frame of a pusher
            current.velocity.oMinSet(current.pushVelocity);

            // view vectors
            viewAngles.ToVectors(viewForward, null, null);
            viewForward.oMulSet(clipModelAxis);
            viewRight.oSet(gravityNormal.Cross(viewForward));
            viewRight.Normalize();

            // fly in spectator mode
            if (current.movementType == etoi(PM_SPECTATOR)) {
                SpectatorMove();
                this.DropTimers();
                return;
            }

            // special no clip mode
            if (current.movementType == etoi(PM_NOCLIP)) {
                this.NoclipMove();
                this.DropTimers();
                return;
            }

            // no control when dead
            if (current.movementType == etoi(PM_DEAD)) {
                command.forwardmove = 0;
                command.rightmove = 0;
                command.upmove = 0;
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
            if (current.movementType == etoi(PM_DEAD)) {
                // dead
                this.DeadMove();
            } else if (ladder) {
                // going up or down a ladder
                this.LadderMove();
            } else if ((current.movementFlags & PMF_TIME_WATERJUMP) != 0) {
                // jumping out of water
                this.WaterJumpMove();
            } else if (etoi(waterLevel) > 1) {
                // swimming
                this.WaterMove();
            } else if (walking) {
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
            current.velocity.oPluSet(current.pushVelocity);
            current.pushVelocity.Zero();
        }
    };

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
