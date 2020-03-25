package neo.Game.Physics;

import static neo.Game.Entity.TH_PHYSICS;
import static neo.Game.Game_local.gameLocal;
import static neo.idlib.math.Angles.getAng_zero;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_LINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NONE;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NOSTOP;
import static neo.idlib.math.Vector.RAD2DEG;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;

import neo.CM.CollisionModel.trace_s;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_Base.idPhysics_Base;
import neo.idlib.BitMsg.idBitMsgDelta;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Curve.idCurve_Spline;
import neo.idlib.math.Extrapolate.idExtrapolate;
import neo.idlib.math.Interpolate.idInterpolateAccelDecelLinear;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Physics_Parametric {

    /*
     ===================================================================================

     Parametric physics

     Used for predefined or scripted motion. The motion of an object is completely
     parametrized. By adjusting the parameters an object is forced to follow a
     predefined path. The parametric physics is typically used for doors, bridges,
     rotating fans etc.

     ===================================================================================
     */
    public static class parametricPState_s {

        int                                     time;                 // physics time
        int                                     atRest;               // set when simulation is suspended
        idVec3                                  origin;               // world origin
        idAngles                                angles;               // world angles
        idMat3                                  axis;                 // world axis
        idVec3                                  localOrigin;          // local origin
        idAngles                                localAngles;          // local angles
        idExtrapolate<idVec3>                   linearExtrapolation;  // extrapolation based description of the position over time
        idExtrapolate<idAngles>                 angularExtrapolation; // extrapolation based description of the orientation over time
        idInterpolateAccelDecelLinear<idVec3>   linearInterpolation;  // interpolation based description of the position over time
        idInterpolateAccelDecelLinear<idAngles> angularInterpolation; // interpolation based description of the orientation over time
        idCurve_Spline<idVec3>                  spline;               // spline based description of the position over time
        idInterpolateAccelDecelLinear<Float>    splineInterpolate;    // position along the spline over time
        boolean                                 useSplineAngles;      // set the orientation using the spline
    }

    public static class idPhysics_Parametric extends idPhysics_Base {
        // CLASS_PROTOTYPE( idPhysics_Parametric );

        // parametric physics state
        private parametricPState_s current;
        private parametricPState_s saved;
        //
        // pusher
        private boolean            isPusher;
        private idClipModel        clipModel;
        private int                pushFlags;
        //
        // results of last evaluate
        private trace_s            pushResults;
        private boolean            isBlocked;
        //
        // master
        private boolean            hasMaster;
        private boolean            isOrientated;
        //
        //

        public idPhysics_Parametric() {

            this.current = new parametricPState_s();
            this.current.time = gameLocal.time;
            this.current.atRest = -1;
            this.current.useSplineAngles = false;
            this.current.origin = new idVec3();
            this.current.angles = new idAngles();
            this.current.axis = idMat3.getMat3_identity();
            this.current.localOrigin = new idVec3();
            this.current.localAngles = new idAngles();
            this.current.linearExtrapolation = new idExtrapolate<>();
            this.current.linearExtrapolation.Init(0, 0, getVec3_zero(), getVec3_zero(), getVec3_zero(), EXTRAPOLATION_NONE);
            this.current.angularExtrapolation = new idExtrapolate<>();
            this.current.angularExtrapolation.Init(0, 0, getAng_zero(), getAng_zero(), getAng_zero(), EXTRAPOLATION_NONE);
            this.current.linearInterpolation = new idInterpolateAccelDecelLinear<>();
            this.current.linearInterpolation.Init(0, 0, 0, 0, getVec3_zero(), getVec3_zero());
            this.current.angularInterpolation = new idInterpolateAccelDecelLinear<>();
            this.current.angularInterpolation.Init(0, 0, 0, 0, getAng_zero(), getAng_zero());
            this.current.spline = null;
            this.current.splineInterpolate = new idInterpolateAccelDecelLinear<>();
            this.current.splineInterpolate.Init(0, 1, 1, 2, 0f, 0f);

            this.saved = this.current;

            this.isPusher = false;
            this.pushFlags = 0;
            this.clipModel = null;
            this.isBlocked = false;
            this.pushResults = new trace_s();//memset( &pushResults, 0, sizeof(pushResults));

            this.hasMaster = false;
            this.isOrientated = false;
        }

        // ~idPhysics_Parametric();
        @Override
        protected void _deconstructor(){
            if ( this.clipModel != null ) {
            idClipModel.delete(this.clipModel);
            }
            if ( this.current.spline != null ) {
//                delete current.spline;
                this.current.spline = null;
            }

            super._deconstructor();
        }

        @Override
        public void Save(idSaveGame savefile) {

            idPhysics_Parametric_SavePState(savefile, this.current);
            idPhysics_Parametric_SavePState(savefile, this.saved);

            savefile.WriteBool(this.isPusher);
            savefile.WriteClipModel(this.clipModel);
            savefile.WriteInt(this.pushFlags);

            savefile.WriteTrace(this.pushResults);
            savefile.WriteBool(this.isBlocked);

            savefile.WriteBool(this.hasMaster);
            savefile.WriteBool(this.isOrientated);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            final boolean[] isPusher = {false}, isBlocked = {false}, hasMaster = {false}, isOrientated = {false};
            final int[] pushFlags = {0};

            idPhysics_Parametric_RestorePState(savefile, this.current);
            idPhysics_Parametric_RestorePState(savefile, this.saved);

            savefile.ReadBool(isPusher);
            savefile.ReadClipModel(this.clipModel);
            savefile.ReadInt(pushFlags);

            savefile.ReadTrace(this.pushResults);
            savefile.ReadBool(isBlocked);

            savefile.ReadBool(hasMaster);
            savefile.ReadBool(isOrientated);

            this.isPusher = isPusher[0];
            this.isBlocked = isBlocked[0];
            this.hasMaster = hasMaster[0];
            this.isOrientated = isOrientated[0];
            this.pushFlags = pushFlags[0];
        }

        public void SetPusher(int flags) {
            assert (this.clipModel != null);
            this.isPusher = true;
            this.pushFlags = flags;
        }

        public boolean IsPusher() {
            return this.isPusher;
        }

        public void SetLinearExtrapolation(int/*extrapolation_t*/ type, int time, int duration, final idVec3 base, final idVec3 speed, final idVec3 baseSpeed) {
            this.current.time = gameLocal.time;
            this.current.linearExtrapolation.Init(time, duration, base, baseSpeed, speed, type);
            this.current.localOrigin.oSet(base);
            Activate();
        }

        public void SetAngularExtrapolation(int/*extrapolation_t*/ type, int time, int duration, final idAngles base, final idAngles speed, final idAngles baseSpeed) {
            this.current.time = gameLocal.time;
            this.current.angularExtrapolation.Init(time, duration, base, baseSpeed, speed, type);
            this.current.localAngles.oSet(base);
            Activate();
        }

        public int/*extrapolation_t*/ GetLinearExtrapolationType() {
            return this.current.linearExtrapolation.GetExtrapolationType();
        }

        public int/*extrapolation_t*/ GetAngularExtrapolationType() {
            return this.current.angularExtrapolation.GetExtrapolationType();
        }

        public void SetLinearInterpolation(int time, int accelTime, int decelTime, int duration, final idVec3 startPos, final idVec3 endPos) {
            this.current.time = gameLocal.time;
            this.current.linearInterpolation.Init(time, accelTime, decelTime, duration, startPos, endPos);
            this.current.localOrigin.oSet(startPos);
            Activate();
        }

        public void SetAngularInterpolation(int time, int accelTime, int decelTime, int duration, final idAngles startAng, final idAngles endAng) {
            this.current.time = gameLocal.time;
            this.current.angularInterpolation.Init(time, accelTime, decelTime, duration, startAng, endAng);
            this.current.localAngles.oSet(startAng);
            Activate();
        }

        public void SetSpline(idCurve_Spline<idVec3> spline, int accelTime, int decelTime, boolean useSplineAngles) {
            if (this.current.spline != null) {
//		delete current.spline;
                this.current.spline = null;
            }
            this.current.spline = spline;
            if (this.current.spline != null) {
                final float startTime = this.current.spline.GetTime(0);
                final float endTime = this.current.spline.GetTime(this.current.spline.GetNumValues() - 1);
                final float length = this.current.spline.GetLengthForTime(endTime);
                this.current.splineInterpolate.Init(startTime, accelTime, decelTime, endTime - startTime, 0.0f, length);
            }
            this.current.useSplineAngles = useSplineAngles;
            Activate();
        }

        public idCurve_Spline<idVec3> GetSpline() {
            return this.current.spline;
        }

        public int GetSplineAcceleration() {
            return (int) this.current.splineInterpolate.GetAcceleration();
        }

        public int GetSplineDeceleration() {
            return (int) this.current.splineInterpolate.GetDeceleration();
        }

        public boolean UsingSplineAngles() {
            return this.current.useSplineAngles;
        }

        public void GetLocalOrigin(idVec3 curOrigin) {
            curOrigin.oSet(this.current.localOrigin);
        }

        public void GetLocalAngles(idAngles curAngles) {
            curAngles.oSet(this.current.localAngles);
        }

        public void GetAngles(idAngles curAngles) {
            curAngles.oSet(this.current.angles);
        }

        // common physics interface
        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {

            assert (this.self != null);
            assert (model != null);

            if ((this.clipModel != null) && (this.clipModel != model) && freeOld) {
                idClipModel.delete(this.clipModel);
            }
            this.clipModel = model;
            this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            return this.clipModel;
        }

        @Override
        public int GetNumClipModels() {
            return (this.clipModel != null ? 1 : 0);
        }

        @Override
        public void SetMass(float mass, int id /*= -1*/) {
        }

        @Override
        public float GetMass(int id /*= -1*/) {
            return 0.0f;
        }

        @Override
        public void SetContents(int contents, int id /*= -1*/) {
            if (this.clipModel != null) {
                this.clipModel.SetContents(contents);
            }
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            if (this.clipModel != null) {
                return this.clipModel.GetContents();
            }
            return 0;
        }

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            if (this.clipModel != null) {
                return this.clipModel.GetBounds();
            }
            return super.GetBounds();
        }

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {
            if (this.clipModel != null) {
                return this.clipModel.GetAbsBounds();
            }
            return super.GetAbsBounds();
        }

        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            idVec3 oldLocalOrigin, oldOrigin;
			final idVec3 masterOrigin = new idVec3();
            idAngles oldLocalAngles, oldAngles;
            idMat3 oldAxis;
			final idMat3 masterAxis = new idMat3();

            this.isBlocked = false;
            oldLocalOrigin = new idVec3(this.current.localOrigin);
            oldOrigin = new idVec3(this.current.origin);
            oldLocalAngles = new idAngles(this.current.localAngles);
            oldAngles = new idAngles(this.current.angles);
            oldAxis = new idMat3(this.current.axis);

            this.current.localOrigin.Zero();
            this.current.localAngles.Zero();

            if (this.current.spline != null) {
                final float length = this.current.splineInterpolate.GetCurrentValue(endTimeMSec);
                final float t = this.current.spline.GetTimeForLength(length, 0.01f);
                this.current.localOrigin.oSet(this.current.spline.GetCurrentValue(t));
                if (this.current.useSplineAngles) {
                    this.current.localAngles = this.current.spline.GetCurrentFirstDerivative(t).ToAngles();
                }
            } else if (this.current.linearInterpolation.GetDuration() != 0) {
                this.current.localOrigin.oPluSet(this.current.linearInterpolation.GetCurrentValue(endTimeMSec));
            } else {
                this.current.localOrigin.oPluSet(this.current.linearExtrapolation.GetCurrentValue(endTimeMSec));
            }

            if (this.current.angularInterpolation.GetDuration() != 0) {
                this.current.localAngles.oPluSet(this.current.angularInterpolation.GetCurrentValue(endTimeMSec));
            } else {
                this.current.localAngles.oPluSet(this.current.angularExtrapolation.GetCurrentValue(endTimeMSec));
            }

            this.current.localAngles.Normalize360();
            this.current.origin.oSet(this.current.localOrigin);
            this.current.angles.oSet(this.current.localAngles);
            this.current.axis.oSet(this.current.localAngles.ToMat3());

            if (this.hasMaster) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                if (masterAxis.IsRotated()) {
                    this.current.origin = this.current.origin.oMultiply(masterAxis).oPlus(masterOrigin);
                    if (this.isOrientated) {
                        this.current.axis.oMulSet(masterAxis);
                        this.current.angles = this.current.axis.ToAngles();
                    }
                } else {
                    this.current.origin.oPluSet(masterOrigin);
                }
            }

            if (this.isPusher) {

                {
                    final trace_s[] pushResults = {this.pushResults};
                    gameLocal.push.ClipPush(pushResults, this.self, this.pushFlags, oldOrigin, oldAxis, this.current.origin, this.current.axis);
                    this.pushResults = pushResults[0];
                }
                if (this.pushResults.fraction < 1.0f) {
                    this.clipModel.Link(gameLocal.clip, this.self, 0, oldOrigin, oldAxis);
                    this.current.localOrigin = oldLocalOrigin;
                    this.current.origin = oldOrigin;
                    this.current.localAngles = oldLocalAngles;
                    this.current.angles = oldAngles;
                    this.current.axis = oldAxis;
                    this.isBlocked = true;
                    return false;
                }

                this.current.angles = this.current.axis.ToAngles();
            }

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }

            this.current.time = endTimeMSec;

            if (TestIfAtRest()) {
                Rest();
            }

            return (!this.current.origin.equals(oldOrigin) || !this.current.axis.equals(oldAxis));
        }

        @Override
        public void UpdateTime(int endTimeMSec) {
            final int timeLeap = endTimeMSec - this.current.time;

            this.current.time = endTimeMSec;
            // move the trajectory start times to sync the trajectory with the current endTime
            this.current.linearExtrapolation.SetStartTime(this.current.linearExtrapolation.GetStartTime() + timeLeap);
            this.current.angularExtrapolation.SetStartTime(this.current.angularExtrapolation.GetStartTime() + timeLeap);
            this.current.linearInterpolation.SetStartTime(this.current.linearInterpolation.GetStartTime() + timeLeap);
            this.current.angularInterpolation.SetStartTime(this.current.angularInterpolation.GetStartTime() + timeLeap);
            if (this.current.spline != null) {
                this.current.spline.ShiftTime(timeLeap);
                this.current.splineInterpolate.SetStartTime(this.current.splineInterpolate.GetStartTime() + timeLeap);
            }
        }

        @Override
        public int GetTime() {
            return this.current.time;
        }

        @Override
        public void Activate() {
            this.current.atRest = -1;
            this.self.BecomeActive(TH_PHYSICS);
        }

        @Override
        public boolean IsAtRest() {
            return this.current.atRest >= 0;
        }

        @Override
        public int GetRestStartTime() {
            return this.current.atRest;
        }

        @Override
        public boolean IsPushable() {
            return false;
        }

        @Override
        public void SaveState() {
            this.saved = this.current;
        }

        @Override
        public void RestoreState() {

            this.current = this.saved;

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.linearExtrapolation.SetStartValue(newOrigin);
            this.current.linearInterpolation.SetStartValue(newOrigin);

            this.current.localOrigin = this.current.linearExtrapolation.GetCurrentValue(this.current.time);
            if (this.hasMaster) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.origin = masterOrigin.oPlus(this.current.localOrigin.oMultiply(masterAxis));
            } else {
                this.current.origin.oSet(this.current.localOrigin);
            }
            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
            Activate();
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            this.current.localAngles = newAxis.ToAngles();

            this.current.angularExtrapolation.SetStartValue(this.current.localAngles);
            this.current.angularInterpolation.SetStartValue(this.current.localAngles);

            this.current.localAngles = this.current.angularExtrapolation.GetCurrentValue(this.current.time);
            if (this.hasMaster && this.isOrientated) {
                this.self.GetMasterPosition(masterOrigin, masterAxis);
                this.current.axis = this.current.localAngles.ToMat3().oMultiply(masterAxis);
                this.current.angles = this.current.axis.ToAngles();
            } else {
                this.current.axis = this.current.localAngles.ToMat3();
                this.current.angles.oSet(this.current.localAngles);
            }
            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
            Activate();
        }

        @Override
        public void Translate(final idVec3 translation, int id /*= -1*/) {
        }

        @Override
        public void Rotate(final idRotation rotation, int id /*= -1*/) {
        }

        @Override
        public idVec3 GetOrigin(int id /*= 0*/) {
            return new idVec3(this.current.origin);
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return new idMat3(this.current.axis);
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            SetLinearExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, this.current.origin, newLinearVelocity, getVec3_origin());
            this.current.linearInterpolation.Init(0, 0, 0, 0, getVec3_zero(), getVec3_zero());
            Activate();
        }

        @Override
        public void SetAngularVelocity(final idVec3 newAngularVelocity, int id /*= 0*/) {
            final idRotation rotation = new idRotation();
            idVec3 vec;
            float angle;

            vec = newAngularVelocity;
            angle = vec.Normalize();
            rotation.Set(getVec3_origin(), vec, RAD2DEG(angle));

            SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, this.current.angles, rotation.ToAngles(), getAng_zero());
            this.current.angularInterpolation.Init(0, 0, 0, 0, getAng_zero(), getAng_zero());
            Activate();
        }
        private static idVec3 curLinearVelocity;

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            curLinearVelocity = this.current.linearExtrapolation.GetCurrentSpeed(gameLocal.time);
            return curLinearVelocity;
        }
        private static idVec3 curAngularVelocity;

        @Override
        public idVec3 GetAngularVelocity(int id /*= 0*/) {
            idAngles angles;

            angles = this.current.angularExtrapolation.GetCurrentSpeed(gameLocal.time);
            curAngularVelocity = angles.ToAngularVelocity();
            return curAngularVelocity;
        }

        @Override
        public void DisableClip() {
            if (this.clipModel != null) {
                this.clipModel.Disable();
            }
        }

        @Override
        public void EnableClip() {
            if (this.clipModel != null) {
                this.clipModel.Enable();
            }
        }

        @Override
        public void UnlinkClip() {
            if (this.clipModel != null) {
                this.clipModel.Unlink();
            }
        }

        @Override
        public void LinkClip() {
            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        @Override
        public void SetMaster(idEntity master, final boolean orientated /*= true*/) {
            final idVec3 masterOrigin = new idVec3();
            final idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (!this.hasMaster) {

                    // transform from world space to master space
                    this.self.GetMasterPosition(masterOrigin, masterAxis);
                    this.current.localOrigin = (this.current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose());
                    if (orientated) {
                        this.current.localAngles = (this.current.axis.oMultiply(masterAxis.Transpose())).ToAngles();
                    } else {
                        this.current.localAngles = this.current.axis.ToAngles();
                    }

                    this.current.linearExtrapolation.SetStartValue(this.current.localOrigin);
                    this.current.angularExtrapolation.SetStartValue(this.current.localAngles);
                    this.hasMaster = true;
                    this.isOrientated = orientated;
                }
            } else {
                if (this.hasMaster) {
                    // transform from master space to world space
                    this.current.localOrigin.oSet(this.current.origin);
                    this.current.localAngles.oSet(this.current.angles);
                    SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.current.origin, getVec3_origin(), getVec3_origin());
                    SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, this.current.angles, getAng_zero(), getAng_zero());
                    this.hasMaster = false;
                }
            }
        }

        @Override
        public trace_s GetBlockingInfo() {
            return (this.isBlocked ? this.pushResults : null);
        }

        @Override
        public idEntity GetBlockingEntity() {
            if (this.isBlocked) {
                return gameLocal.entities[ this.pushResults.c.entityNum];
            }
            return null;
        }

        @Override
        public int GetLinearEndTime() {
            if (this.current.spline != null) {
                if (this.current.spline.GetBoundaryType() != idCurve_Spline.BT_CLOSED) {
                    return (int) this.current.spline.GetTime(this.current.spline.GetNumValues() - 1);
                } else {
                    return 0;
                }
            } else if (this.current.linearInterpolation.GetDuration() != 0) {
                return (int) this.current.linearInterpolation.GetEndTime();
            } else {
                return (int) this.current.linearExtrapolation.GetEndTime();
            }
        }

        @Override
        public int GetAngularEndTime() {
            if (this.current.angularInterpolation.GetDuration() != 0) {
                return (int) this.current.angularInterpolation.GetEndTime();
            } else {
                return (int) this.current.angularExtrapolation.GetEndTime();
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteLong(this.current.time);
            msg.WriteLong(this.current.atRest);
            msg.WriteFloat(this.current.origin.oGet(0));
            msg.WriteFloat(this.current.origin.oGet(1));
            msg.WriteFloat(this.current.origin.oGet(2));
            msg.WriteFloat(this.current.angles.oGet(0));
            msg.WriteFloat(this.current.angles.oGet(1));
            msg.WriteFloat(this.current.angles.oGet(2));
            msg.WriteDeltaFloat(this.current.origin.oGet(0), this.current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(this.current.origin.oGet(1), this.current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(this.current.origin.oGet(2), this.current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(this.current.angles.oGet(0), this.current.localAngles.oGet(0));
            msg.WriteDeltaFloat(this.current.angles.oGet(1), this.current.localAngles.oGet(1));
            msg.WriteDeltaFloat(this.current.angles.oGet(2), this.current.localAngles.oGet(2));

            msg.WriteBits(this.current.linearExtrapolation.GetExtrapolationType(), 8);
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetStartTime());
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetDuration());
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetStartValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetStartValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetStartValue().oGet(2));
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetSpeed().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetSpeed().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetSpeed().oGet(2));
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetBaseSpeed().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetBaseSpeed().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.linearExtrapolation.GetBaseSpeed().oGet(2));

            msg.WriteBits(this.current.angularExtrapolation.GetExtrapolationType(), 8);
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetStartTime());
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetDuration());
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetStartValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetStartValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetStartValue().oGet(2));
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetSpeed().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetSpeed().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetSpeed().oGet(2));
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetBaseSpeed().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetBaseSpeed().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.angularExtrapolation.GetBaseSpeed().oGet(2));

            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetStartTime());
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetAcceleration());
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetDeceleration());
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetDuration());
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetStartValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetStartValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetStartValue().oGet(2));
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetEndValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetEndValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.linearInterpolation.GetEndValue().oGet(2));

            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetStartTime());
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetAcceleration());
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetDeceleration());
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetDuration());
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetStartValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetStartValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetStartValue().oGet(2));
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetEndValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetEndValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, this.current.angularInterpolation.GetEndValue().oGet(2));
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            int/*extrapolation_t*/ linearType, angularType;
            float startTime, duration, accelTime, decelTime;
            final idVec3 linearStartValue = new idVec3(), linearSpeed = new idVec3(), linearBaseSpeed = new idVec3(), startPos = new idVec3(), endPos = new idVec3();
            final idAngles angularStartValue = new idAngles(), angularSpeed = new idAngles(), angularBaseSpeed = new idAngles(), startAng = new idAngles(), endAng = new idAngles();

            this.current.time = msg.ReadLong();
            this.current.atRest = msg.ReadLong();
            this.current.origin.oSet(0, msg.ReadFloat());
            this.current.origin.oSet(1, msg.ReadFloat());
            this.current.origin.oSet(2, msg.ReadFloat());
            this.current.angles.oSet(0, msg.ReadFloat());
            this.current.angles.oSet(1, msg.ReadFloat());
            this.current.angles.oSet(2, msg.ReadFloat());
            this.current.localOrigin.oSet(0, msg.ReadDeltaFloat(this.current.origin.oGet(0)));
            this.current.localOrigin.oSet(1, msg.ReadDeltaFloat(this.current.origin.oGet(1)));
            this.current.localOrigin.oSet(2, msg.ReadDeltaFloat(this.current.origin.oGet(2)));
            this.current.localAngles.oSet(0, msg.ReadDeltaFloat(this.current.angles.oGet(0)));
            this.current.localAngles.oSet(1, msg.ReadDeltaFloat(this.current.angles.oGet(1)));
            this.current.localAngles.oSet(2, msg.ReadDeltaFloat(this.current.angles.oGet(2)));

            linearType = /*(extrapolation_t)*/ msg.ReadBits(8);
            startTime = msg.ReadDeltaFloat(0.0f);
            duration = msg.ReadDeltaFloat(0.0f);
            linearStartValue.oSet(0, msg.ReadDeltaFloat(0.0f));
            linearStartValue.oSet(1, msg.ReadDeltaFloat(0.0f));
            linearStartValue.oSet(2, msg.ReadDeltaFloat(0.0f));
            linearSpeed.oSet(0, msg.ReadDeltaFloat(0.0f));
            linearSpeed.oSet(1, msg.ReadDeltaFloat(0.0f));
            linearSpeed.oSet(2, msg.ReadDeltaFloat(0.0f));
            linearBaseSpeed.oSet(0, msg.ReadDeltaFloat(0.0f));
            linearBaseSpeed.oSet(1, msg.ReadDeltaFloat(0.0f));
            linearBaseSpeed.oSet(2, msg.ReadDeltaFloat(0.0f));
            this.current.linearExtrapolation.Init(startTime, duration, linearStartValue, linearBaseSpeed, linearSpeed, linearType);

            angularType = msg.ReadBits(8);
            startTime = msg.ReadDeltaFloat(0.0f);
            duration = msg.ReadDeltaFloat(0.0f);
            angularStartValue.oSet(0, msg.ReadDeltaFloat(0.0f));
            angularStartValue.oSet(1, msg.ReadDeltaFloat(0.0f));
            angularStartValue.oSet(2, msg.ReadDeltaFloat(0.0f));
            angularSpeed.oSet(0, msg.ReadDeltaFloat(0.0f));
            angularSpeed.oSet(1, msg.ReadDeltaFloat(0.0f));
            angularSpeed.oSet(2, msg.ReadDeltaFloat(0.0f));
            angularBaseSpeed.oSet(0, msg.ReadDeltaFloat(0.0f));
            angularBaseSpeed.oSet(1, msg.ReadDeltaFloat(0.0f));
            angularBaseSpeed.oSet(2, msg.ReadDeltaFloat(0.0f));
            this.current.angularExtrapolation.Init(startTime, duration, angularStartValue, angularBaseSpeed, angularSpeed, angularType);

            startTime = msg.ReadDeltaFloat(0.0f);
            accelTime = msg.ReadDeltaFloat(0.0f);
            decelTime = msg.ReadDeltaFloat(0.0f);
            duration = msg.ReadDeltaFloat(0.0f);
            startPos.oSet(0, msg.ReadDeltaFloat(0.0f));
            startPos.oSet(1, msg.ReadDeltaFloat(0.0f));
            startPos.oSet(2, msg.ReadDeltaFloat(0.0f));
            endPos.oSet(0, msg.ReadDeltaFloat(0.0f));
            endPos.oSet(1, msg.ReadDeltaFloat(0.0f));
            endPos.oSet(2, msg.ReadDeltaFloat(0.0f));
            this.current.linearInterpolation.Init(startTime, accelTime, decelTime, duration, startPos, endPos);

            startTime = msg.ReadDeltaFloat(0.0f);
            accelTime = msg.ReadDeltaFloat(0.0f);
            decelTime = msg.ReadDeltaFloat(0.0f);
            duration = msg.ReadDeltaFloat(0.0f);
            startAng.oSet(0, msg.ReadDeltaFloat(0.0f));
            startAng.oSet(1, msg.ReadDeltaFloat(0.0f));
            startAng.oSet(2, msg.ReadDeltaFloat(0.0f));
            endAng.oSet(0, msg.ReadDeltaFloat(0.0f));
            endAng.oSet(1, msg.ReadDeltaFloat(0.0f));
            endAng.oSet(2, msg.ReadDeltaFloat(0.0f));
            this.current.angularInterpolation.Init(startTime, accelTime, decelTime, duration, startAng, endAng);

            this.current.axis = this.current.angles.ToMat3();

            if (this.clipModel != null) {
                this.clipModel.Link(gameLocal.clip, this.self, 0, this.current.origin, this.current.axis);
            }
        }

        private boolean TestIfAtRest() {

            if (((this.current.linearExtrapolation.GetExtrapolationType() & ~EXTRAPOLATION_NOSTOP) == EXTRAPOLATION_NONE)
                    && ((this.current.angularExtrapolation.GetExtrapolationType() & ~EXTRAPOLATION_NOSTOP) == EXTRAPOLATION_NONE)
                    && (this.current.linearInterpolation.GetDuration() == 0)
                    && (this.current.angularInterpolation.GetDuration() == 0)
                    && (this.current.spline == null)) {
                return true;
            }

            if (!this.current.linearExtrapolation.IsDone(this.current.time)) {
                return false;
            }

            if (!this.current.angularExtrapolation.IsDone(this.current.time)) {
                return false;
            }

            if (!this.current.linearInterpolation.IsDone(this.current.time)) {
                return false;
            }

            if (!this.current.angularInterpolation.IsDone(this.current.time)) {
                return false;
            }

            if ((this.current.spline != null) && !this.current.spline.IsDone(this.current.time)) {
                return false;
            }

            return true;
        }

        private void Rest() {
            this.current.atRest = gameLocal.time;
            this.self.BecomeInactive(TH_PHYSICS);
        }
    }

    /*
     ================
     idPhysics_Parametric_SavePState
     ================
     */
    static void idPhysics_Parametric_SavePState(idSaveGame savefile, final parametricPState_s state) {
        savefile.WriteInt(state.time);
        savefile.WriteInt(state.atRest);
        savefile.WriteBool(state.useSplineAngles);
        savefile.WriteVec3(state.origin);
        savefile.WriteAngles(state.angles);
        savefile.WriteMat3(state.axis);
        savefile.WriteVec3(state.localOrigin);
        savefile.WriteAngles(state.localAngles);

        savefile.WriteInt(state.linearExtrapolation.GetExtrapolationType());
        savefile.WriteFloat(state.linearExtrapolation.GetStartTime());
        savefile.WriteFloat(state.linearExtrapolation.GetDuration());
        savefile.WriteVec3(state.linearExtrapolation.GetStartValue());
        savefile.WriteVec3(state.linearExtrapolation.GetBaseSpeed());
        savefile.WriteVec3(state.linearExtrapolation.GetSpeed());

        savefile.WriteInt(state.angularExtrapolation.GetExtrapolationType());
        savefile.WriteFloat(state.angularExtrapolation.GetStartTime());
        savefile.WriteFloat(state.angularExtrapolation.GetDuration());
        savefile.WriteAngles(state.angularExtrapolation.GetStartValue());
        savefile.WriteAngles(state.angularExtrapolation.GetBaseSpeed());
        savefile.WriteAngles(state.angularExtrapolation.GetSpeed());

        savefile.WriteFloat(state.linearInterpolation.GetStartTime());
        savefile.WriteFloat(state.linearInterpolation.GetAcceleration());
        savefile.WriteFloat(state.linearInterpolation.GetDeceleration());
        savefile.WriteFloat(state.linearInterpolation.GetDuration());
        savefile.WriteVec3(state.linearInterpolation.GetStartValue());
        savefile.WriteVec3(state.linearInterpolation.GetEndValue());

        savefile.WriteFloat(state.angularInterpolation.GetStartTime());
        savefile.WriteFloat(state.angularInterpolation.GetAcceleration());
        savefile.WriteFloat(state.angularInterpolation.GetDeceleration());
        savefile.WriteFloat(state.angularInterpolation.GetDuration());
        savefile.WriteAngles(state.angularInterpolation.GetStartValue());
        savefile.WriteAngles(state.angularInterpolation.GetEndValue());

        // spline is handled by owner
        savefile.WriteFloat(state.splineInterpolate.GetStartTime());
        savefile.WriteFloat(state.splineInterpolate.GetAcceleration());
        savefile.WriteFloat(state.splineInterpolate.GetDuration());
        savefile.WriteFloat(state.splineInterpolate.GetDeceleration());
        savefile.WriteFloat(state.splineInterpolate.GetStartValue());
        savefile.WriteFloat(state.splineInterpolate.GetEndValue());
    }

    /*
     ================
     idPhysics_Parametric_RestorePState
     ================
     */
    static void idPhysics_Parametric_RestorePState(idRestoreGame savefile, parametricPState_s state) {
        final int[]/*extrapolation_t*/ etype = {0};
        final float[] startTime = {0}, duration = {0}, accelTime = {0}, decelTime = {0}, startValue = {0}, endValue = {0};
        final idVec3 linearStartValue = new idVec3(), linearBaseSpeed = new idVec3(), linearSpeed = new idVec3(), startPos = new idVec3(), endPos = new idVec3();
        final idAngles angularStartValue = new idAngles(), angularBaseSpeed = new idAngles(), angularSpeed = new idAngles(), startAng = new idAngles(), endAng = new idAngles();
        final int[] time = {0}, atRest = {0};
        final boolean[] useSplineAngles = {false};

        savefile.ReadInt(time);
        savefile.ReadInt(atRest);
        savefile.ReadBool(useSplineAngles);

        state.time = time[0];
        state.atRest = atRest[0];
        state.useSplineAngles = useSplineAngles[0];

        savefile.ReadVec3(state.origin);
        savefile.ReadAngles(state.angles);
        savefile.ReadMat3(state.axis);
        savefile.ReadVec3(state.localOrigin);
        savefile.ReadAngles(state.localAngles);

        savefile.ReadInt(etype);
        savefile.ReadFloat(startTime);
        savefile.ReadFloat(duration);
        savefile.ReadVec3(linearStartValue);
        savefile.ReadVec3(linearBaseSpeed);
        savefile.ReadVec3(linearSpeed);

        state.linearExtrapolation.Init(startTime[0], duration[0], linearStartValue, linearBaseSpeed, linearSpeed, etype[0]);

        savefile.ReadInt(etype);
        savefile.ReadFloat(startTime);
        savefile.ReadFloat(duration);
        savefile.ReadAngles(angularStartValue);
        savefile.ReadAngles(angularBaseSpeed);
        savefile.ReadAngles(angularSpeed);

        state.angularExtrapolation.Init(startTime[0], duration[0], angularStartValue, angularBaseSpeed, angularSpeed, etype[0]);

        savefile.ReadFloat(startTime);
        savefile.ReadFloat(accelTime);
        savefile.ReadFloat(decelTime);
        savefile.ReadFloat(duration);
        savefile.ReadVec3(startPos);
        savefile.ReadVec3(endPos);

        state.linearInterpolation.Init(startTime[0], accelTime[0], decelTime[0], duration[0], startPos, endPos);

        savefile.ReadFloat(startTime);
        savefile.ReadFloat(accelTime);
        savefile.ReadFloat(decelTime);
        savefile.ReadFloat(duration);
        savefile.ReadAngles(startAng);
        savefile.ReadAngles(endAng);

        state.angularInterpolation.Init(startTime[0], accelTime[0], decelTime[0], duration[0], startAng, endAng);

        // spline is handled by owner
        savefile.ReadFloat(startTime);
        savefile.ReadFloat(accelTime);
        savefile.ReadFloat(duration);
        savefile.ReadFloat(decelTime);
        savefile.ReadFloat(startValue);
        savefile.ReadFloat(endValue);

        state.splineInterpolate.Init(startTime[0], accelTime[0], decelTime[0], duration[0], startValue[0], endValue[0]);
    }

}
