package neo.idlib.math;

import java.nio.ByteBuffer;

import neo.TempDump;
import neo.TempDump.SERiAL;
import neo.idlib.math.Extrapolate.idExtrapolate;
import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class Interpolate {

    /*
     ==============================================================================================

     Linear interpolation.

     ==============================================================================================
     */
    public static class idInterpolate<type> {

        private float startTime;
        private float duration;
        private type startValue;
        private type endValue;
        private float currentTime;
        private type currentValue;
        //
        //

        public idInterpolate() {
            this.currentTime = this.startTime = this.duration = 0;
//            memset( & currentValue, 0, sizeof(currentValue));
//            startValue = endValue = currentValue;
        }

        public void Init(final float startTime, final float duration, final type startValue, final type endValue) {
            this.startTime = startTime;
            this.duration = duration;
            this.startValue = TempDump.clone(startValue);
            this.endValue = TempDump.clone(endValue);
            this.currentTime = startTime - 1;
            this.currentValue = TempDump.clone(startValue);
        }

        public void SetStartTime(float time) {
            this.startTime = time;
        }

        public void SetDuration(float duration) {
            this.duration = duration;
        }

        public void SetStartValue(final type startValue) {
            this.startValue = TempDump.clone(startValue);
        }

        public void SetEndValue(final type endValue) {
            this.endValue = TempDump.clone(endValue);
        }

        public type GetCurrentValue(float time) {
            float deltaTime;

            deltaTime = time - this.startTime;
            if (time != this.currentTime) {
                this.currentTime = time;
                if (deltaTime <= 0) {
                    this.currentValue = TempDump.clone(this.startValue);
                } else if (deltaTime >= this.duration) {
                    this.currentValue = TempDump.clone(this.endValue);
                } else {
                    if (this.currentValue instanceof Integer) {
                        final int e = (Integer) this.endValue;
                        final int s = (Integer) this.startValue;
                        this.currentValue = (type) (Integer) (int) (s + ((e - s) * (deltaTime / this.duration)));
                    }
                    if (this.currentValue instanceof Float) {
                        final float e = (Float) this.endValue;
                        final float s = (Float) this.startValue;
                        this.currentValue = (type) (Float) (s + ((e - s) * (deltaTime / this.duration)));
                    }
                }
            }
            return this.currentValue;
        }

        public boolean IsDone(float time) {
            return (time >= (this.startTime + this.duration));
        }

        public float GetStartTime() {
            return this.startTime;
        }

        public float GetEndTime() {
            return this.startTime + this.duration;
        }

        public float GetDuration() {
            return this.duration;
        }

        public type GetStartValue() {
            return this.startValue;
        }

        public type GetEndValue() {
            return this.endValue;
        }
    }

    /*
     ==============================================================================================

     Continuous interpolation with linear acceleration and deceleration phase.
     The velocity is continuous but the acceleration is not.

     ==============================================================================================
     */
    public static class idInterpolateAccelDecelLinear<type> implements SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private float startTime;
        private float accelTime;
        private float linearTime;
        private float decelTime;
        private type startValue;
        private type endValue;
        private final idExtrapolate<type> extrapolate;
        //
        //

        public idInterpolateAccelDecelLinear() {
            this.startTime = this.accelTime = this.linearTime = this.decelTime = 0;
//	memset( &startValue, 0, sizeof( startValue ) );
            this.endValue = this.startValue;
            this.extrapolate = new idExtrapolate<>();
        }

        public void Init(final float startTime, final float accelTime, final float decelTime, final float duration, final type startValue, final type endValue) {
            type speed;

            this.startTime = startTime;
            this.accelTime = accelTime;
            this.decelTime = decelTime;
            this.startValue = startValue;
            this.endValue = endValue;

            if (duration <= 0.0f) {
                return;
            }

            if ((this.accelTime + this.decelTime) > duration) {
                this.accelTime = (this.accelTime * duration) / (this.accelTime + this.decelTime);
                this.decelTime = duration - this.accelTime;
            }
            this.linearTime = duration - this.accelTime - this.decelTime;
            speed = _Multiply(_Minus(endValue, startValue), (1000.0f / (this.linearTime + ((this.accelTime + this.decelTime) * 0.5f))));

            if (0.0f != this.accelTime) {
                this.extrapolate.Init(startTime, this.accelTime, startValue, _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_ACCELLINEAR);
            } else if (0.0f != this.linearTime) {
                this.extrapolate.Init(startTime, this.linearTime, startValue, _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_LINEAR);
            } else {
                this.extrapolate.Init(startTime, this.decelTime, startValue, _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_DECELLINEAR);
            }
        }

        public void SetStartTime(float time) {
            this.startTime = time;
            Invalidate();
        }

        public void SetStartValue(final type startValue) {
            this.startValue = startValue;
            Invalidate();
        }

        public void SetEndValue(final type endValue) {
            this.endValue = endValue;
            Invalidate();
        }

        public type GetCurrentValue(float time) {
            SetPhase(time);
            return this.extrapolate.GetCurrentValue(time);
        }

        public type GetCurrentSpeed(float time) {
            SetPhase(time);
            return this.extrapolate.GetCurrentSpeed(time);
        }

        public boolean IsDone(float time) {
            return (time >= (this.startTime + this.accelTime + this.linearTime + this.decelTime));
        }

        public float GetStartTime() {
            return this.startTime;
        }

        public float GetEndTime() {
            return this.startTime + this.accelTime + this.linearTime + this.decelTime;
        }

        public float GetDuration() {
            return this.accelTime + this.linearTime + this.decelTime;
        }

        public float GetAcceleration() {
            return this.accelTime;
        }

        public float GetDeceleration() {
            return this.decelTime;
        }

        public type GetStartValue() {
            return this.startValue;
        }

        public type GetEndValue() {
            return this.endValue;
        }

        private void Invalidate() {
            this.extrapolate.Init(0, 0, this.extrapolate.GetStartValue(), this.extrapolate.GetBaseSpeed(), this.extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_NONE);
        }

        private void SetPhase(float time) {
            float deltaTime;

            deltaTime = time - this.startTime;
            if (deltaTime < this.accelTime) {
                if (this.extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_ACCELLINEAR) {
                    this.extrapolate.Init(this.startTime, this.accelTime, this.startValue, this.extrapolate.GetBaseSpeed(), this.extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_ACCELLINEAR);
                }
            } else if (deltaTime < (this.accelTime + this.linearTime)) {
                if (this.extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_LINEAR) {
                    this.extrapolate.Init(this.startTime + this.accelTime, this.linearTime, _Plus(this.startValue, _Multiply(this.extrapolate.GetSpeed(), (this.accelTime * 0.001f * 0.5f))), this.extrapolate.GetBaseSpeed(), this.extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_LINEAR);
                }
            } else {
                if (this.extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_DECELLINEAR) {
                    this.extrapolate.Init(this.startTime + this.accelTime + this.linearTime, this.decelTime, _Minus(this.endValue, _Multiply(this.extrapolate.GetSpeed(), (this.decelTime * 0.001f * 0.5f))), this.extrapolate.GetBaseSpeed(), this.extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_DECELLINEAR);
                }
            }
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private type _Multiply(final type t, final float f) {
            if (t instanceof Vector.idVec3) {
                return (type) ((Vector.idVec3) t).oMultiply(f);
            } else if (t instanceof Vector.idVec4) {
                return (type) ((Vector.idVec4) t).oMultiply(f);
            } else if (t instanceof Angles.idAngles) {
                return (type) ((Angles.idAngles) t).oMultiply(f);
            } else if (t instanceof Double) {
                return (type) Double.valueOf(f * ((Double) t));
            }

            return (type) Float.valueOf(f * ((Float) t));
        }

        private type _Plus(final type t1, final type t2) {
            if (t1 instanceof Vector.idVec3) {
                return (type) ((Vector.idVec3) t1).oPlus((Vector.idVec3) t2);
            } else if (t1 instanceof Vector.idVec4) {
                return (type) ((Vector.idVec4) t1).oPlus((Vector.idVec4) t2);
            } else if (t1 instanceof Angles.idAngles) {
                return (type) ((Angles.idAngles) t1).oPlus((Angles.idAngles) t2);
            } else if (t1 instanceof Double) {
                return (type) Double.valueOf((Double) t1 + (Double) t2);
            }

            return (type) Float.valueOf((Float) t1 + (Float) t2);
        }

        private type _Minus(final type t1, final type t2) {
            if (t1 instanceof Vector.idVec3) {
                return (type) ((Vector.idVec3) t1).oMinus((Vector.idVec3) t2);
            } else if (t1 instanceof Vector.idVec4) {
                return (type) ((Vector.idVec4) t1).oMinus((Vector.idVec4) t2);
            } else if (t1 instanceof Angles.idAngles) {
                return (type) ((Angles.idAngles) t1).oMinus((Angles.idAngles) t2);
            }

            return (type) Float.valueOf((Float) t1 - (Float) t2);
        }
    }

    /*
     ==============================================================================================

     Continuous interpolation with sinusoidal acceleration and deceleration phase.
     Both the velocity and acceleration are continuous.

     ==============================================================================================
     */
    class idInterpolateAccelDecelSine<type> {

        private float startTime;
        private float accelTime;
        private float linearTime;
        private float decelTime;
        private type startValue;
        private type endValue;
        private idExtrapolate<type> extrapolate;
        //
        //

        public idInterpolateAccelDecelSine() {
            this.startTime = this.accelTime = this.linearTime = this.decelTime = 0;
//	memset( &startValue, 0, sizeof( startValue ) );
            this.endValue = this.startValue;
        }

        public void Init(final float startTime, final float accelTime, final float decelTime, final float duration, final type startValue, final type endValue) {
            type speed;

            this.startTime = startTime;
            this.accelTime = accelTime;
            this.decelTime = decelTime;
            this.startValue = startValue;
            this.endValue = endValue;

            if (duration <= 0.0f) {
                return;
            }

            if ((this.accelTime + this.decelTime) > duration) {
                this.accelTime = (this.accelTime * duration) / (this.accelTime + this.decelTime);
                this.decelTime = duration - this.accelTime;
            }
            this.linearTime = duration - this.accelTime - this.decelTime;
            speed = _Multiply(_Minus(endValue, startValue), (1000.0f / (this.linearTime + ((this.accelTime + this.decelTime) * idMath.SQRT_1OVER2))));

            if (0 != this.accelTime) {
                this.extrapolate.Init(startTime, this.accelTime, startValue, _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_ACCELSINE);
            } else if (0 != this.linearTime) {
                this.extrapolate.Init(startTime, this.linearTime, startValue, _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_LINEAR);
            } else {
                this.extrapolate.Init(startTime, this.decelTime, startValue, _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_DECELSINE);
            }
        }

        public void SetStartTime(float time) {
            this.startTime = time;
            Invalidate();
        }

        public void SetStartValue(final type startValue) {
            this.startValue = startValue;
            Invalidate();
        }

        public void SetEndValue(final type endValue) {
            this.endValue = endValue;
            Invalidate();
        }

        public type GetCurrentValue(float time) {
            SetPhase(time);
            return this.extrapolate.GetCurrentValue(time);
        }

        public type GetCurrentSpeed(float time) {
            SetPhase(time);
            return this.extrapolate.GetCurrentSpeed(time);
        }

        public boolean IsDone(float time) {
            return (time >= (this.startTime + this.accelTime + this.linearTime + this.decelTime));
        }

        public float GetStartTime() {
            return this.startTime;
        }

        public float GetEndTime() {
            return this.startTime + this.accelTime + this.linearTime + this.decelTime;
        }

        public float GetDuration() {
            return this.accelTime + this.linearTime + this.decelTime;
        }

        public float GetAcceleration() {
            return this.accelTime;
        }

        public float GetDeceleration() {
            return this.decelTime;
        }

        public type GetStartValue() {
            return this.startValue;
        }

        public type GetEndValue() {
            return this.endValue;
        }

        private void Invalidate() {
            this.extrapolate.Init(0, 0, this.extrapolate.GetStartValue(), this.extrapolate.GetBaseSpeed(), this.extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_NONE);
        }

        private void SetPhase(float time) {
            float deltaTime;

            deltaTime = time - this.startTime;
            if (deltaTime < this.accelTime) {
                if (this.extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_ACCELSINE) {
                    this.extrapolate.Init(this.startTime, this.accelTime, this.startValue, this.extrapolate.GetBaseSpeed(), this.extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_ACCELSINE);
                }
            } else if (deltaTime < (this.accelTime + this.linearTime)) {
                if (this.extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_LINEAR) {
                    this.extrapolate.Init(this.startTime + this.accelTime, this.linearTime, _Plus(this.startValue, _Plus(this.extrapolate.GetSpeed(), (this.accelTime * 0.001f * idMath.SQRT_1OVER2))), this.extrapolate.GetBaseSpeed(), this.extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_LINEAR);

                }
            } else {
                if (this.extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_DECELSINE) {
                    this.extrapolate.Init(this.startTime + this.accelTime + this.linearTime, this.decelTime, _Plus(this.endValue, _Minus(this.extrapolate.GetSpeed(), (this.decelTime * 0.001f * idMath.SQRT_1OVER2))), this.extrapolate.GetBaseSpeed(), this.extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_DECELSINE);
                }
            }
        }

        private type _Multiply(final type t, final float f) {
            if (t instanceof Vector.idVec3) {
                return (type) ((Vector.idVec3) t).oMultiply(f);
            } else if (t instanceof Vector.idVec4) {
                return (type) ((Vector.idVec4) t).oMultiply(f);
            } else if (t instanceof Angles.idAngles) {
                return (type) ((Angles.idAngles) t).oMultiply(f);
            } else if (t instanceof Double) {
                return (type) Double.valueOf(f * ((Double) t));
            }

            return (type) Float.valueOf(f * ((Float) t));
        }

        private type _Plus(final type t1, final Object t2) {
            if (t1 instanceof Vector.idVec3) {
                return (type) ((Vector.idVec3) t1).oPlus((Vector.idVec3) t2);
            } else if (t1 instanceof Vector.idVec4) {
                return (type) ((Vector.idVec4) t1).oPlus((Vector.idVec4) t2);
            } else if (t1 instanceof Angles.idAngles) {
                return (type) ((Angles.idAngles) t1).oPlus((Angles.idAngles) t2);
            } else if (t1 instanceof Double) {
                return (type) Double.valueOf((Double) t1 + (Double) t2);
            }

            return (type) Float.valueOf((Float) t1 + (Float) t2);
        }

        private type _Minus(final type t1, final Object t2) {
            if (t1 instanceof Vector.idVec3) {
                return (type) ((Vector.idVec3) t1).oMinus((Vector.idVec3) t2);
            } else if (t1 instanceof Vector.idVec4) {
                return (type) ((Vector.idVec4) t1).oMinus((Vector.idVec4) t2);
            } else if (t1 instanceof Angles.idAngles) {
                return (type) ((Angles.idAngles) t1).oMinus((Angles.idAngles) t2);
            }

            return (type) Float.valueOf((Float) t1 - (Float) t2);
        }
    }
}
