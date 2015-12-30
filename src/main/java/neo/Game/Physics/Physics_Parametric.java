package neo.Game.Physics;

import neo.CM.CollisionModel.trace_s;
import static neo.Game.Entity.TH_PHYSICS;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.gameLocal;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics_Base.idPhysics_Base;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BitMsg.idBitMsgDelta;
import static neo.idlib.math.Angles.getAng_zero;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Curve.idCurve_Spline;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_LINEAR;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NONE;
import static neo.idlib.math.Extrapolate.EXTRAPOLATION_NOSTOP;
import neo.idlib.math.Extrapolate.idExtrapolate;
import neo.idlib.math.Interpolate.idInterpolateAccelDecelLinear;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.RAD2DEG;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.idlib.math.Vector.getVec3_zero;
import neo.idlib.math.Vector.idVec3;

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
    };

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

            current = new parametricPState_s();
            current.time = gameLocal.time;
            current.atRest = -1;
            current.useSplineAngles = false;
            current.origin = new idVec3();
            current.angles = new idAngles();
            current.axis = idMat3.getMat3_identity();
            current.localOrigin = new idVec3();
            current.localAngles = new idAngles();
            current.linearExtrapolation = new idExtrapolate<>();
            current.linearExtrapolation.Init(0, 0, getVec3_zero(), getVec3_zero(), getVec3_zero(), EXTRAPOLATION_NONE);
            current.angularExtrapolation = new idExtrapolate<>();
            current.angularExtrapolation.Init(0, 0, getAng_zero(), getAng_zero(), getAng_zero(), EXTRAPOLATION_NONE);
            current.linearInterpolation = new idInterpolateAccelDecelLinear<>();
            current.linearInterpolation.Init(0, 0, 0, 0, getVec3_zero(), getVec3_zero());
            current.angularInterpolation = new idInterpolateAccelDecelLinear<>();
            current.angularInterpolation.Init(0, 0, 0, 0, getAng_zero(), getAng_zero());
            current.spline = null;
            current.splineInterpolate = new idInterpolateAccelDecelLinear<>();
            current.splineInterpolate.Init(0, 1, 1, 2, 0f, 0f);

            saved = current;

            isPusher = false;
            pushFlags = 0;
            clipModel = null;
            isBlocked = false;
            pushResults = new trace_s();//memset( &pushResults, 0, sizeof(pushResults));

            hasMaster = false;
            isOrientated = false;
        }
        // ~idPhysics_Parametric();

        @Override
        public void Save(idSaveGame savefile) {

            idPhysics_Parametric_SavePState(savefile, current);
            idPhysics_Parametric_SavePState(savefile, saved);

            savefile.WriteBool(isPusher);
            savefile.WriteClipModel(clipModel);
            savefile.WriteInt(pushFlags);

            savefile.WriteTrace(pushResults);
            savefile.WriteBool(isBlocked);

            savefile.WriteBool(hasMaster);
            savefile.WriteBool(isOrientated);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            boolean[] isPusher = {false}, isBlocked = {false}, hasMaster = {false}, isOrientated = {false};
            int[] pushFlags = {0};

            idPhysics_Parametric_RestorePState(savefile, current);
            idPhysics_Parametric_RestorePState(savefile, saved);

            savefile.ReadBool(isPusher);
            savefile.ReadClipModel(clipModel);
            savefile.ReadInt(pushFlags);

            savefile.ReadTrace(pushResults);
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
            assert (clipModel != null);
            isPusher = true;
            pushFlags = flags;
        }

        public boolean IsPusher() {
            return isPusher;
        }

        public void SetLinearExtrapolation(int/*extrapolation_t*/ type, int time, int duration, final idVec3 base, final idVec3 speed, final idVec3 baseSpeed) {
            current.time = gameLocal.time;
            current.linearExtrapolation.Init(time, duration, base, baseSpeed, speed, type);
            current.localOrigin = base;
            Activate();
        }

        public void SetAngularExtrapolation(int/*extrapolation_t*/ type, int time, int duration, final idAngles base, final idAngles speed, final idAngles baseSpeed) {
            current.time = gameLocal.time;
            current.angularExtrapolation.Init(time, duration, base, baseSpeed, speed, type);
            current.localAngles = base;
            Activate();
        }

        public int/*extrapolation_t*/ GetLinearExtrapolationType() {
            return current.linearExtrapolation.GetExtrapolationType();
        }

        public int/*extrapolation_t*/ GetAngularExtrapolationType() {
            return current.angularExtrapolation.GetExtrapolationType();
        }

        public void SetLinearInterpolation(int time, int accelTime, int decelTime, int duration, final idVec3 startPos, final idVec3 endPos) {
            current.time = gameLocal.time;
            current.linearInterpolation.Init(time, accelTime, decelTime, duration, startPos, endPos);
            current.localOrigin = startPos;
            Activate();
        }

        public void SetAngularInterpolation(int time, int accelTime, int decelTime, int duration, final idAngles startAng, final idAngles endAng) {
            current.time = gameLocal.time;
            current.angularInterpolation.Init(time, accelTime, decelTime, duration, startAng, endAng);
            current.localAngles = startAng;
            Activate();
        }

        public void SetSpline(idCurve_Spline<idVec3> spline, int accelTime, int decelTime, boolean useSplineAngles) {
            if (current.spline != null) {
//		delete current.spline;
                current.spline = null;
            }
            current.spline = spline;
            if (current.spline != null) {
                float startTime = current.spline.GetTime(0);
                float endTime = current.spline.GetTime(current.spline.GetNumValues() - 1);
                float length = current.spline.GetLengthForTime(endTime);
                current.splineInterpolate.Init(startTime, accelTime, decelTime, endTime - startTime, 0.0f, length);
            }
            current.useSplineAngles = useSplineAngles;
            Activate();
        }

        public idCurve_Spline<idVec3> GetSpline() {
            return current.spline;
        }

        public int GetSplineAcceleration() {
            return (int) current.splineInterpolate.GetAcceleration();
        }

        public int GetSplineDeceleration() {
            return (int) current.splineInterpolate.GetDeceleration();
        }

        public boolean UsingSplineAngles() {
            return current.useSplineAngles;
        }

        public void GetLocalOrigin(idVec3 curOrigin) {
            curOrigin.oSet(current.localOrigin);
        }

        public void GetLocalAngles(idAngles curAngles) {
            curAngles.oSet(current.localAngles);
        }

        public void GetAngles(idAngles curAngles) {
            curAngles.oSet(current.angles);
        }

        // common physics interface
        @Override
        public void SetClipModel(idClipModel model, float density, int id /*= 0*/, boolean freeOld /*= true*/) {

            assert (self != null);
            assert (model != null);

//	if ( clipModel && clipModel != model && freeOld ) {
//		delete clipModel;
//	}
            clipModel = model;
            clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
        }

        @Override
        public idClipModel GetClipModel(int id /*= 0*/) {
            return clipModel;
        }

        @Override
        public int GetNumClipModels() {
            return (clipModel != null ? 1 : 0);
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
            if (clipModel != null) {
                clipModel.SetContents(contents);
            }
        }

        @Override
        public int GetContents(int id /*= -1*/) {
            if (clipModel != null) {
                return clipModel.GetContents();
            }
            return 0;
        }

        @Override
        public idBounds GetBounds(int id /*= -1*/) {
            if (clipModel != null) {
                return clipModel.GetBounds();
            }
            return super.GetBounds();
        }

        @Override
        public idBounds GetAbsBounds(int id /*= -1*/) {
            if (clipModel != null) {
                return clipModel.GetAbsBounds();
            }
            return super.GetAbsBounds();
        }

        @Override
        public boolean Evaluate(int timeStepMSec, int endTimeMSec) {
            idVec3 oldLocalOrigin, oldOrigin, masterOrigin = new idVec3();
            idAngles oldLocalAngles, oldAngles;
            idMat3 oldAxis, masterAxis = new idMat3();

            isBlocked = false;
            oldLocalOrigin = current.localOrigin;
            oldOrigin = current.origin;
            oldLocalAngles = current.localAngles;
            oldAngles = current.angles;
            oldAxis = current.axis;

            current.localOrigin.Zero();
            current.localAngles.Zero();

            if (current.spline != null) {
                float length = current.splineInterpolate.GetCurrentValue(endTimeMSec);
                float t = current.spline.GetTimeForLength(length, 0.01f);
                current.localOrigin = current.spline.GetCurrentValue(t);
                if (current.useSplineAngles) {
                    current.localAngles = current.spline.GetCurrentFirstDerivative(t).ToAngles();
                }
            } else if (current.linearInterpolation.GetDuration() != 0) {
                current.localOrigin.oPluSet(current.linearInterpolation.GetCurrentValue(endTimeMSec));
            } else {
                current.localOrigin.oPluSet(current.linearExtrapolation.GetCurrentValue(endTimeMSec));
            }

            if (current.angularInterpolation.GetDuration() != 0) {
                current.localAngles.oPluSet(current.angularInterpolation.GetCurrentValue(endTimeMSec));
            } else {
                current.localAngles.oPluSet(current.angularExtrapolation.GetCurrentValue(endTimeMSec));
            }

            current.localAngles.Normalize360();
            current.origin = current.localOrigin;
            current.angles = current.localAngles;
            current.axis = current.localAngles.ToMat3();

            if (hasMaster) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                if (masterAxis.IsRotated()) {
                    current.origin = current.origin.oMultiply(masterAxis).oPlus(masterOrigin);
                    if (isOrientated) {
                        current.axis.oMulSet(masterAxis);
                        current.angles = current.axis.ToAngles();
                    }
                } else {
                    current.origin.oPluSet(masterOrigin);
                }
            }

            if (isPusher) {

                {
                    trace_s[] pushResults = {this.pushResults};
                    gameLocal.push.ClipPush(pushResults, self, pushFlags, oldOrigin, oldAxis, current.origin, current.axis);
                    this.pushResults = pushResults[0];
                }
                if (pushResults.fraction < 1.0f) {
                    clipModel.Link(gameLocal.clip, self, 0, oldOrigin, oldAxis);
                    current.localOrigin = oldLocalOrigin;
                    current.origin = oldOrigin;
                    current.localAngles = oldLocalAngles;
                    current.angles = oldAngles;
                    current.axis = oldAxis;
                    isBlocked = true;
                    return false;
                }

                current.angles = current.axis.ToAngles();
            }

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }

            current.time = endTimeMSec;

            if (TestIfAtRest()) {
                Rest();
            }

            return (current.origin != oldOrigin || current.axis != oldAxis);
        }

        @Override
        public void UpdateTime(int endTimeMSec) {
            int timeLeap = endTimeMSec - current.time;

            current.time = endTimeMSec;
            // move the trajectory start times to sync the trajectory with the current endTime
            current.linearExtrapolation.SetStartTime(current.linearExtrapolation.GetStartTime() + timeLeap);
            current.angularExtrapolation.SetStartTime(current.angularExtrapolation.GetStartTime() + timeLeap);
            current.linearInterpolation.SetStartTime(current.linearInterpolation.GetStartTime() + timeLeap);
            current.angularInterpolation.SetStartTime(current.angularInterpolation.GetStartTime() + timeLeap);
            if (current.spline != null) {
                current.spline.ShiftTime(timeLeap);
                current.splineInterpolate.SetStartTime(current.splineInterpolate.GetStartTime() + timeLeap);
            }
        }

        @Override
        public int GetTime() {
            return current.time;
        }

        @Override
        public void Activate() {
            current.atRest = -1;
            self.BecomeActive(TH_PHYSICS);
        }

        @Override
        public boolean IsAtRest() {
            return current.atRest >= 0;
        }

        @Override
        public int GetRestStartTime() {
            return current.atRest;
        }

        @Override
        public boolean IsPushable() {
            return false;
        }

        @Override
        public void SaveState() {
            saved = current;
        }

        @Override
        public void RestoreState() {

            current = saved;

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
        }

        @Override
        public void SetOrigin(final idVec3 newOrigin, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.linearExtrapolation.SetStartValue(newOrigin);
            current.linearInterpolation.SetStartValue(newOrigin);

            current.localOrigin = current.linearExtrapolation.GetCurrentValue(current.time);
            if (hasMaster) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.origin = masterOrigin.oPlus(current.localOrigin.oMultiply(masterAxis));
            } else {
                current.origin = current.localOrigin;
            }
            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
            Activate();
        }

        @Override
        public void SetAxis(final idMat3 newAxis, int id /*= -1*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            current.localAngles = newAxis.ToAngles();

            current.angularExtrapolation.SetStartValue(current.localAngles);
            current.angularInterpolation.SetStartValue(current.localAngles);

            current.localAngles = current.angularExtrapolation.GetCurrentValue(current.time);
            if (hasMaster && isOrientated) {
                self.GetMasterPosition(masterOrigin, masterAxis);
                current.axis = current.localAngles.ToMat3().oMultiply(masterAxis);
                current.angles = current.axis.ToAngles();
            } else {
                current.axis = current.localAngles.ToMat3();
                current.angles = current.localAngles;
            }
            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
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
            return current.origin;
        }

        @Override
        public idMat3 GetAxis(int id /*= 0*/) {
            return current.axis;
        }

        @Override
        public void SetLinearVelocity(final idVec3 newLinearVelocity, int id /*= 0*/) {
            SetLinearExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, current.origin, newLinearVelocity, getVec3_origin());
            current.linearInterpolation.Init(0, 0, 0, 0, getVec3_zero(), getVec3_zero());
            Activate();
        }

        @Override
        public void SetAngularVelocity(final idVec3 newAngularVelocity, int id /*= 0*/) {
            idRotation rotation = new idRotation();
            idVec3 vec;
            float angle;

            vec = newAngularVelocity;
            angle = vec.Normalize();
            rotation.Set(getVec3_origin(), vec, (float) RAD2DEG(angle));

            SetAngularExtrapolation((EXTRAPOLATION_LINEAR | EXTRAPOLATION_NOSTOP), gameLocal.time, 0, current.angles, rotation.ToAngles(), getAng_zero());
            current.angularInterpolation.Init(0, 0, 0, 0, getAng_zero(), getAng_zero());
            Activate();
        }
        private static idVec3 curLinearVelocity;

        @Override
        public idVec3 GetLinearVelocity(int id /*= 0*/) {
            curLinearVelocity = current.linearExtrapolation.GetCurrentSpeed(gameLocal.time);
            return curLinearVelocity;
        }
        private static idVec3 curAngularVelocity;

        @Override
        public idVec3 GetAngularVelocity(int id /*= 0*/) {
            idAngles angles;

            angles = current.angularExtrapolation.GetCurrentSpeed(gameLocal.time);
            curAngularVelocity = angles.ToAngularVelocity();
            return curAngularVelocity;
        }

        @Override
        public void DisableClip() {
            if (clipModel != null) {
                clipModel.Disable();
            }
        }

        @Override
        public void EnableClip() {
            if (clipModel != null) {
                clipModel.Enable();
            }
        }

        @Override
        public void UnlinkClip() {
            if (clipModel != null) {
                clipModel.Unlink();
            }
        }

        @Override
        public void LinkClip() {
            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
        }

        @Override
        public void SetMaster(idEntity master, final boolean orientated /*= true*/) {
            idVec3 masterOrigin = new idVec3();
            idMat3 masterAxis = new idMat3();

            if (master != null) {
                if (!hasMaster) {

                    // transform from world space to master space
                    self.GetMasterPosition(masterOrigin, masterAxis);
                    current.localOrigin = (current.origin.oMinus(masterOrigin)).oMultiply(masterAxis.Transpose());
                    if (orientated) {
                        current.localAngles = (current.axis.oMultiply(masterAxis.Transpose())).ToAngles();
                    } else {
                        current.localAngles = current.axis.ToAngles();
                    }

                    current.linearExtrapolation.SetStartValue(current.localOrigin);
                    current.angularExtrapolation.SetStartValue(current.localAngles);
                    hasMaster = true;
                    isOrientated = orientated;
                }
            } else {
                if (hasMaster) {
                    // transform from master space to world space
                    current.localOrigin = current.origin;
                    current.localAngles = current.angles;
                    SetLinearExtrapolation(EXTRAPOLATION_NONE, 0, 0, current.origin, getVec3_origin(), getVec3_origin());
                    SetAngularExtrapolation(EXTRAPOLATION_NONE, 0, 0, current.angles, getAng_zero(), getAng_zero());
                    hasMaster = false;
                }
            }
        }

        @Override
        public trace_s GetBlockingInfo() {
            return (isBlocked ? pushResults : null);
        }

        @Override
        public idEntity GetBlockingEntity() {
            if (isBlocked) {
                return gameLocal.entities[ pushResults.c.entityNum];
            }
            return null;
        }

        @Override
        public int GetLinearEndTime() {
            if (current.spline != null) {
                if (current.spline.GetBoundaryType() != idCurve_Spline.BT_CLOSED) {
                    return (int) current.spline.GetTime(current.spline.GetNumValues() - 1);
                } else {
                    return 0;
                }
            } else if (current.linearInterpolation.GetDuration() != 0) {
                return (int) current.linearInterpolation.GetEndTime();
            } else {
                return (int) current.linearExtrapolation.GetEndTime();
            }
        }

        @Override
        public int GetAngularEndTime() {
            if (current.angularInterpolation.GetDuration() != 0) {
                return (int) current.angularInterpolation.GetEndTime();
            } else {
                return (int) current.angularExtrapolation.GetEndTime();
            }
        }

        @Override
        public void WriteToSnapshot(idBitMsgDelta msg) {
            msg.WriteLong(current.time);
            msg.WriteLong(current.atRest);
            msg.WriteFloat(current.origin.oGet(0));
            msg.WriteFloat(current.origin.oGet(1));
            msg.WriteFloat(current.origin.oGet(2));
            msg.WriteFloat(current.angles.oGet(0));
            msg.WriteFloat(current.angles.oGet(1));
            msg.WriteFloat(current.angles.oGet(2));
            msg.WriteDeltaFloat(current.origin.oGet(0), current.localOrigin.oGet(0));
            msg.WriteDeltaFloat(current.origin.oGet(1), current.localOrigin.oGet(1));
            msg.WriteDeltaFloat(current.origin.oGet(2), current.localOrigin.oGet(2));
            msg.WriteDeltaFloat(current.angles.oGet(0), current.localAngles.oGet(0));
            msg.WriteDeltaFloat(current.angles.oGet(1), current.localAngles.oGet(1));
            msg.WriteDeltaFloat(current.angles.oGet(2), current.localAngles.oGet(2));

            msg.WriteBits(current.linearExtrapolation.GetExtrapolationType(), 8);
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetStartTime());
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetDuration());
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetStartValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetStartValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetStartValue().oGet(2));
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetSpeed().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetSpeed().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetSpeed().oGet(2));
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetBaseSpeed().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetBaseSpeed().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.linearExtrapolation.GetBaseSpeed().oGet(2));

            msg.WriteBits(current.angularExtrapolation.GetExtrapolationType(), 8);
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetStartTime());
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetDuration());
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetStartValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetStartValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetStartValue().oGet(2));
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetSpeed().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetSpeed().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetSpeed().oGet(2));
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetBaseSpeed().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetBaseSpeed().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.angularExtrapolation.GetBaseSpeed().oGet(2));

            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetStartTime());
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetAcceleration());
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetDeceleration());
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetDuration());
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetStartValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetStartValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetStartValue().oGet(2));
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetEndValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetEndValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.linearInterpolation.GetEndValue().oGet(2));

            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetStartTime());
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetAcceleration());
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetDeceleration());
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetDuration());
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetStartValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetStartValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetStartValue().oGet(2));
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetEndValue().oGet(0));
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetEndValue().oGet(1));
            msg.WriteDeltaFloat(0.0f, current.angularInterpolation.GetEndValue().oGet(2));
        }

        @Override
        public void ReadFromSnapshot(final idBitMsgDelta msg) {
            int/*extrapolation_t*/ linearType, angularType;
            float startTime, duration, accelTime, decelTime;
            idVec3 linearStartValue = new idVec3(), linearSpeed = new idVec3(), linearBaseSpeed = new idVec3(), startPos = new idVec3(), endPos = new idVec3();
            idAngles angularStartValue = new idAngles(), angularSpeed = new idAngles(), angularBaseSpeed = new idAngles(), startAng = new idAngles(), endAng = new idAngles();

            current.time = msg.ReadLong();
            current.atRest = msg.ReadLong();
            current.origin.oSet(0, msg.ReadFloat());
            current.origin.oSet(1, msg.ReadFloat());
            current.origin.oSet(2, msg.ReadFloat());
            current.angles.oSet(0, msg.ReadFloat());
            current.angles.oSet(1, msg.ReadFloat());
            current.angles.oSet(2, msg.ReadFloat());
            current.localOrigin.oSet(0, msg.ReadDeltaFloat(current.origin.oGet(0)));
            current.localOrigin.oSet(1, msg.ReadDeltaFloat(current.origin.oGet(1)));
            current.localOrigin.oSet(2, msg.ReadDeltaFloat(current.origin.oGet(2)));
            current.localAngles.oSet(0, msg.ReadDeltaFloat(current.angles.oGet(0)));
            current.localAngles.oSet(1, msg.ReadDeltaFloat(current.angles.oGet(1)));
            current.localAngles.oSet(2, msg.ReadDeltaFloat(current.angles.oGet(2)));

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
            current.linearExtrapolation.Init(startTime, duration, linearStartValue, linearBaseSpeed, linearSpeed, linearType);

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
            current.angularExtrapolation.Init(startTime, duration, angularStartValue, angularBaseSpeed, angularSpeed, angularType);

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
            current.linearInterpolation.Init(startTime, accelTime, decelTime, duration, startPos, endPos);

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
            current.angularInterpolation.Init(startTime, accelTime, decelTime, duration, startAng, endAng);

            current.axis = current.angles.ToMat3();

            if (clipModel != null) {
                clipModel.Link(gameLocal.clip, self, 0, current.origin, current.axis);
            }
        }

        private boolean TestIfAtRest() {

            if ((current.linearExtrapolation.GetExtrapolationType() & ~EXTRAPOLATION_NOSTOP) == EXTRAPOLATION_NONE
                    && (current.angularExtrapolation.GetExtrapolationType() & ~EXTRAPOLATION_NOSTOP) == EXTRAPOLATION_NONE
                    && current.linearInterpolation.GetDuration() == 0
                    && current.angularInterpolation.GetDuration() == 0
                    && current.spline == null) {
                return true;
            }

            if (!current.linearExtrapolation.IsDone(current.time)) {
                return false;
            }

            if (!current.angularExtrapolation.IsDone(current.time)) {
                return false;
            }

            if (!current.linearInterpolation.IsDone(current.time)) {
                return false;
            }

            if (!current.angularInterpolation.IsDone(current.time)) {
                return false;
            }

            if (current.spline != null && !current.spline.IsDone(current.time)) {
                return false;
            }

            return true;
        }

        private void Rest() {
            current.atRest = gameLocal.time;
            self.BecomeInactive(TH_PHYSICS);
        }
    };

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

        savefile.WriteInt((int) state.linearExtrapolation.GetExtrapolationType());
        savefile.WriteFloat(state.linearExtrapolation.GetStartTime());
        savefile.WriteFloat(state.linearExtrapolation.GetDuration());
        savefile.WriteVec3(state.linearExtrapolation.GetStartValue());
        savefile.WriteVec3(state.linearExtrapolation.GetBaseSpeed());
        savefile.WriteVec3(state.linearExtrapolation.GetSpeed());

        savefile.WriteInt((int) state.angularExtrapolation.GetExtrapolationType());
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
        int[]/*extrapolation_t*/ etype = {0};
        float[] startTime = {0}, duration = {0}, accelTime = {0}, decelTime = {0}, startValue = {0}, endValue = {0};
        idVec3 linearStartValue = new idVec3(), linearBaseSpeed = new idVec3(), linearSpeed = new idVec3(), startPos = new idVec3(), endPos = new idVec3();
        idAngles angularStartValue = new idAngles(), angularBaseSpeed = new idAngles(), angularSpeed = new idAngles(), startAng = new idAngles(), endAng = new idAngles();
        int[] time = {0}, atRest = {0};
        boolean[] useSplineAngles = {false};

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
