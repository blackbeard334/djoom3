package neo.idlib.math;

import static neo.TempDump.reflects._Minus;
import static neo.TempDump.reflects._Multiply;
import static neo.TempDump.reflects._Plus;
import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class Extrapolate {

    public static final int EXTRAPOLATION_NONE = 0x01;	// no extrapolation, covered distance = duration * 0.001 * ( baseSpeed )
    public static final int EXTRAPOLATION_LINEAR = 0x02;	// linear extrapolation, covered distance = duration * 0.001 * ( baseSpeed + speed )
    public static final int EXTRAPOLATION_ACCELLINEAR = 0x04;	// linear acceleration, covered distance = duration * 0.001 * ( baseSpeed + 0.5 * speed )
    public static final int EXTRAPOLATION_DECELLINEAR = 0x08;	// linear deceleration, covered distance = duration * 0.001 * ( baseSpeed + 0.5 * speed )
    public static final int EXTRAPOLATION_ACCELSINE = 0x10;	// sinusoidal acceleration, covered distance = duration * 0.001 * ( baseSpeed + sqrt( 0.5 ) * speed )
    public static final int EXTRAPOLATION_DECELSINE = 0x20;	// sinusoidal deceleration, covered distance = duration * 0.001 * ( baseSpeed + sqrt( 0.5 ) * speed )
    public static final int EXTRAPOLATION_NOSTOP = 0x40;	// do not stop at startTime + duration

    /*
     ==============================================================================================

     Extrapolate

     ==============================================================================================
     */
    public static class idExtrapolate<type> {

        public idExtrapolate() {
            extrapolationType = EXTRAPOLATION_NONE;
            startTime = duration = 0.0f;
//	memset( &startValue, 0, sizeof( startValue ) );
//	memset( &baseSpeed, 0, sizeof( baseSpeed ) );
//	memset( &speed, 0, sizeof( speed ) );
            currentTime = -1;
            currentValue = startValue;
        }

        public void Init(final float startTime, final float duration, final type startValue, final type baseSpeed, final type speed, final /*extrapolation_t*/ int extrapolationType) {
            this.extrapolationType = extrapolationType;
            this.startTime = startTime;
            this.duration = duration;
            this.startValue = startValue;
            this.baseSpeed = baseSpeed;
            this.speed = speed;
            currentTime = -1;
            currentValue = startValue;
        }

        public type GetCurrentValue(float time) {
            float deltaTime, s;

            if (time == currentTime) {
                return currentValue;
            }

            currentTime = time;

            if (time < startTime) {
                return startValue;
            }

            if (0 == (extrapolationType & EXTRAPOLATION_NOSTOP) && (time > startTime + duration)) {
                time = startTime + duration;
            }

            switch (extrapolationType & ~EXTRAPOLATION_NOSTOP) {
                case EXTRAPOLATION_NONE: {
                    deltaTime = (time - startTime) * 0.001f;
                    currentValue = (type) _Plus(startValue, _Multiply(deltaTime, baseSpeed));
                    break;
                }
                case EXTRAPOLATION_LINEAR: {
                    deltaTime = (time - startTime) * 0.001f;
                    currentValue = (type) _Plus(startValue, _Multiply(deltaTime, _Plus(baseSpeed, speed)));
                    break;
                }
                case EXTRAPOLATION_ACCELLINEAR: {
                    if (0 == duration) {
                        currentValue = startValue;
                    } else {
                        deltaTime = (time - startTime) / duration;
                        s = (0.5f * deltaTime * deltaTime) * (duration * 0.001f);
                        currentValue = (type) _Plus(startValue, _Plus(_Multiply(deltaTime, baseSpeed), _Multiply(s, speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_DECELLINEAR: {
                    if (0 == duration) {
                        currentValue = startValue;
                    } else {
                        deltaTime = (time - startTime) / duration;
                        s = (deltaTime - (0.5f * deltaTime * deltaTime)) * (duration * 0.001f);
                        currentValue = (type) _Plus(startValue, _Plus(_Multiply(deltaTime, baseSpeed), _Multiply(s, speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_ACCELSINE: {
                    if (0 == duration) {
                        currentValue = startValue;
                    } else {
                        deltaTime = (time - startTime) / duration;
                        s = (1.0f - idMath.Cos(deltaTime * idMath.HALF_PI)) * duration * 0.001f * idMath.SQRT_1OVER2;
                        currentValue = (type) _Plus(startValue, _Plus(_Multiply(deltaTime, baseSpeed), _Multiply(s, speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_DECELSINE: {
                    if (0 == duration) {
                        currentValue = startValue;
                    } else {
                        deltaTime = (time - startTime) / duration;
                        s = idMath.Sin(deltaTime * idMath.HALF_PI) * duration * 0.001f * idMath.SQRT_1OVER2;
                        currentValue = (type) _Plus(startValue, _Plus(_Multiply(deltaTime, baseSpeed), _Multiply(s, speed)));
                    }
                    break;
                }
            }
            return currentValue;
        }

        public type GetCurrentSpeed(float time) {
            float deltaTime, s;

            if (time < startTime || 0 == duration) {
                return (type) _Minus(startValue, startValue);
            }

            if (0 == (extrapolationType & EXTRAPOLATION_NOSTOP) && (time > startTime + duration)) {
                return (type) _Minus(startValue, startValue);
            }

            switch (extrapolationType & ~EXTRAPOLATION_NOSTOP) {
                case EXTRAPOLATION_NONE: {
                    return baseSpeed;
                }
                case EXTRAPOLATION_LINEAR: {
                    return (type) _Plus(baseSpeed, speed);
                }
                case EXTRAPOLATION_ACCELLINEAR: {
                    deltaTime = (time - startTime) / duration;
                    s = deltaTime;
                    return (type) _Plus(baseSpeed, _Multiply(s, speed));
                }
                case EXTRAPOLATION_DECELLINEAR: {
                    deltaTime = (time - startTime) / duration;
                    s = 1.0f - deltaTime;
                    return (type) _Plus(baseSpeed, _Multiply(s, speed));
                }
                case EXTRAPOLATION_ACCELSINE: {
                    deltaTime = (time - startTime) / duration;
                    s = idMath.Sin(deltaTime * idMath.HALF_PI);
                    return (type) _Plus(baseSpeed, _Multiply(s, speed));
                }
                case EXTRAPOLATION_DECELSINE: {
                    deltaTime = (time - startTime) / duration;
                    s = idMath.Cos(deltaTime * idMath.HALF_PI);
                    return (type) _Plus(baseSpeed, _Multiply(s, speed));
                }
                default: {
                    return baseSpeed;
                }
            }
        }

        public boolean IsDone(float time) {
            return (0 == (extrapolationType & EXTRAPOLATION_NOSTOP) && time >= startTime + duration);
        }

        public void SetStartTime(float time) {
            startTime = time;
            currentTime = -1;
        }

        public float GetStartTime() {
            return startTime;
        }

        public float GetEndTime() {
            return (0 == (extrapolationType & EXTRAPOLATION_NOSTOP) && duration > 0) ? startTime + duration : 0;
        }

        public float GetDuration() {
            return duration;
        }

        public void SetStartValue(final type value) {
            startValue = value;
            currentTime = -1;
        }

        public final type GetStartValue() {
            return startValue;
        }

        public final type GetBaseSpeed() {
            return baseSpeed;
        }

        public final type GetSpeed() {
            return speed;
        }

        public /*extrapolation_t*/ int GetExtrapolationType() {
            return extrapolationType;
        }
//
        private /*extrapolation_t*/ int extrapolationType;
        private float startTime;
        private float duration;
        private type startValue;
        private type baseSpeed;
        private type speed;
        private float currentTime;
        private type currentValue;
    };
}
