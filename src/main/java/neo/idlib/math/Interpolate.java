package neo.idlib.math;

import java.nio.ByteBuffer;
import neo.TempDump.SERiAL;
import static neo.TempDump.reflects._Minus;
import static neo.TempDump.reflects._Multiply;
import static neo.TempDump.reflects._Plus;
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
            currentTime = startTime = duration = 0;
//            memset( & currentValue, 0, sizeof(currentValue));
            startValue = endValue = currentValue;
        }

        public void Init(final float startTime, final float duration, final type startValue, final type endValue) {
            this.startTime = startTime;
            this.duration = duration;
            this.startValue = startValue;
            this.endValue = endValue;
            this.currentTime = startTime - 1;
            this.currentValue = startValue;
        }

        public void SetStartTime(float time) {
            this.startTime = time;
        }

        public void SetDuration(float duration) {
            this.duration = duration;
        }

        public void SetStartValue(final type startValue) {
            this.startValue = startValue;
        }

        public void SetEndValue(final type endValue) {
            this.endValue = endValue;
        }

        public type GetCurrentValue(float time) {
            float deltaTime;

            deltaTime = time - startTime;
            if (time != currentTime) {
                currentTime = time;
                if (deltaTime <= 0) {
                    currentValue = startValue;
                } else if (deltaTime >= duration) {
                    currentValue = endValue;
                } else {
                    if (currentValue instanceof Integer) {
                        final int e = (Integer) this.endValue;
                        final int s = (Integer) this.startValue;
                        currentValue = (type) (Integer) (int) (s + (e - s) * ((float) deltaTime / duration));
                    }
                    if (currentValue instanceof Float) {
                        final float e = (Float) this.endValue;
                        final float s = (Float) this.startValue;
                        currentValue = (type) (Float) (s + (e - s) * ((float) deltaTime / duration));
                    }
                }
            }
            return currentValue;
        }

        public boolean IsDone(float time) {
            return (time >= startTime + duration);
        }

        public float GetStartTime() {
            return startTime;
        }

        public float GetEndTime() {
            return startTime + duration;
        }

        public float GetDuration() {
            return duration;
        }

        public type GetStartValue() {
            return startValue;
        }

        public type GetEndValue() {
            return endValue;
        }
    };

    /*
     ==============================================================================================

     Continuous interpolation with linear acceleration and deceleration phase.
     The velocity is continuous but the acceleration is not.

     ==============================================================================================
     */
    public static class idInterpolateAccelDecelLinear<type> implements SERiAL {

        private float startTime;
        private float accelTime;
        private float linearTime;
        private float decelTime;
        private type startValue;
        private type endValue;
        private idExtrapolate<type> extrapolate;
        //
        //

        public idInterpolateAccelDecelLinear() {
            startTime = accelTime = linearTime = decelTime = 0;
//	memset( &startValue, 0, sizeof( startValue ) );
            endValue = startValue;
            this.extrapolate = new idExtrapolate<type>();
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

            if (this.accelTime + this.decelTime > duration) {
                this.accelTime = this.accelTime * duration / (this.accelTime + this.decelTime);
                this.decelTime = duration - this.accelTime;
            }
            this.linearTime = duration - this.accelTime - this.decelTime;
            speed = (type) _Multiply(_Minus(endValue, startValue), (1000.0f / (this.linearTime + (this.accelTime + this.decelTime) * 0.5f)));

            if (0.0f != this.accelTime) {
                extrapolate.Init(startTime, this.accelTime, startValue, (type) _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_ACCELLINEAR);
            } else if (0.0f != this.linearTime) {
                extrapolate.Init(startTime, this.linearTime, startValue, (type) _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_LINEAR);
            } else {
                extrapolate.Init(startTime, this.decelTime, startValue, (type) _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_DECELLINEAR);
            }
        }

        public void SetStartTime(float time) {
            startTime = time;
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
            return extrapolate.GetCurrentValue(time);
        }

        public type GetCurrentSpeed(float time) {
            SetPhase(time);
            return extrapolate.GetCurrentSpeed(time);
        }

        public boolean IsDone(float time) {
            return (time >= startTime + accelTime + linearTime + decelTime);
        }

        public float GetStartTime() {
            return startTime;
        }

        public float GetEndTime() {
            return startTime + accelTime + linearTime + decelTime;
        }

        public float GetDuration() {
            return accelTime + linearTime + decelTime;
        }

        public float GetAcceleration() {
            return accelTime;
        }

        public float GetDeceleration() {
            return decelTime;
        }

        public type GetStartValue() {
            return startValue;
        }

        public type GetEndValue() {
            return endValue;
        }

        private void Invalidate() {
            extrapolate.Init(0, 0, extrapolate.GetStartValue(), extrapolate.GetBaseSpeed(), extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_NONE);
        }

        private void SetPhase(float time) {
            float deltaTime;

            deltaTime = time - startTime;
            if (deltaTime < accelTime) {
                if (extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_ACCELLINEAR) {
                    extrapolate.Init(startTime, accelTime, startValue, extrapolate.GetBaseSpeed(), extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_ACCELLINEAR);
                }
            } else if (deltaTime < accelTime + linearTime) {
                if (extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_LINEAR) {
                    extrapolate.Init(startTime + accelTime, linearTime, (type) _Plus(startValue, _Multiply(extrapolate.GetSpeed(), (accelTime * 0.001f * 0.5f))), extrapolate.GetBaseSpeed(), extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_LINEAR);
                }
            } else {
                if (extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_DECELLINEAR) {
                    extrapolate.Init(startTime + accelTime + linearTime, decelTime, (type) _Minus(endValue, _Multiply(extrapolate.GetSpeed(), (decelTime * 0.001f * 0.5f))), extrapolate.GetBaseSpeed(), extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_DECELLINEAR);
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
    };

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
            startTime = accelTime = linearTime = decelTime = 0;
//	memset( &startValue, 0, sizeof( startValue ) );
            endValue = startValue;
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

            if (this.accelTime + this.decelTime > duration) {
                this.accelTime = this.accelTime * duration / (this.accelTime + this.decelTime);
                this.decelTime = duration - this.accelTime;
            }
            this.linearTime = duration - this.accelTime - this.decelTime;
            speed = (type) _Multiply(_Minus(endValue, startValue), (1000.0f / (this.linearTime + (this.accelTime + this.decelTime) * idMath.SQRT_1OVER2)));

            if (0 != this.accelTime) {
                extrapolate.Init(startTime, this.accelTime, startValue, (type) _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_ACCELSINE);
            } else if (0 != this.linearTime) {
                extrapolate.Init(startTime, this.linearTime, startValue, (type) _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_LINEAR);
            } else {
                extrapolate.Init(startTime, this.decelTime, startValue, (type) _Minus(startValue, startValue), speed, Extrapolate.EXTRAPOLATION_DECELSINE);
            }
        }

        public void SetStartTime(float time) {
            startTime = time;
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
            return extrapolate.GetCurrentValue(time);
        }

        public type GetCurrentSpeed(float time) {
            SetPhase(time);
            return extrapolate.GetCurrentSpeed(time);
        }

        public boolean IsDone(float time) {
            return (time >= startTime + accelTime + linearTime + decelTime);
        }

        public float GetStartTime() {
            return startTime;
        }

        public float GetEndTime() {
            return startTime + accelTime + linearTime + decelTime;
        }

        public float GetDuration() {
            return accelTime + linearTime + decelTime;
        }

        public float GetAcceleration() {
            return accelTime;
        }

        public float GetDeceleration() {
            return decelTime;
        }

        public type GetStartValue() {
            return startValue;
        }

        public type GetEndValue() {
            return endValue;
        }

        private void Invalidate() {
            extrapolate.Init(0, 0, extrapolate.GetStartValue(), extrapolate.GetBaseSpeed(), extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_NONE);
        }

        private void SetPhase(float time) {
            float deltaTime;

            deltaTime = time - startTime;
            if (deltaTime < accelTime) {
                if (extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_ACCELSINE) {
                    extrapolate.Init(startTime, accelTime, startValue, extrapolate.GetBaseSpeed(), extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_ACCELSINE);
                }
            } else if (deltaTime < accelTime + linearTime) {
                if (extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_LINEAR) {
                    extrapolate.Init(startTime + accelTime, linearTime, (type) _Plus(startValue, _Plus(extrapolate.GetSpeed(), (accelTime * 0.001f * idMath.SQRT_1OVER2))), extrapolate.GetBaseSpeed(), extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_LINEAR);

                }
            } else {
                if (extrapolate.GetExtrapolationType() != Extrapolate.EXTRAPOLATION_DECELSINE) {
                    extrapolate.Init(startTime + accelTime + linearTime, decelTime, (type) _Plus(endValue, _Minus(extrapolate.GetSpeed(), (decelTime * 0.001f * idMath.SQRT_1OVER2))), extrapolate.GetBaseSpeed(), extrapolate.GetSpeed(), Extrapolate.EXTRAPOLATION_DECELSINE);
                }
            }
        }
    };
}
