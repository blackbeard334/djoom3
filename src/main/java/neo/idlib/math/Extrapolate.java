package neo.idlib.math;

import neo.TempDump;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Extrapolate {

    public static final int EXTRAPOLATION_NONE        = 0x01;    // no extrapolation, covered distance = duration * 0.001 * ( baseSpeed )
    public static final int EXTRAPOLATION_LINEAR      = 0x02;    // linear extrapolation, covered distance = duration * 0.001 * ( baseSpeed + speed )
    public static final int EXTRAPOLATION_ACCELLINEAR = 0x04;    // linear acceleration, covered distance = duration * 0.001 * ( baseSpeed + 0.5 * speed )
    public static final int EXTRAPOLATION_DECELLINEAR = 0x08;    // linear deceleration, covered distance = duration * 0.001 * ( baseSpeed + 0.5 * speed )
    public static final int EXTRAPOLATION_ACCELSINE   = 0x10;    // sinusoidal acceleration, covered distance = duration * 0.001 * ( baseSpeed + sqrt( 0.5 ) * speed )
    public static final int EXTRAPOLATION_DECELSINE   = 0x20;    // sinusoidal deceleration, covered distance = duration * 0.001 * ( baseSpeed + sqrt( 0.5 ) * speed )
    public static final int EXTRAPOLATION_NOSTOP      = 0x40;    // do not stop at startTime + duration

    /*
     ==============================================================================================

     Extrapolate

     ==============================================================================================
     */
    public static class idExtrapolate<type> {

        private /*extrapolation_t*/ int   extrapolationType;
        private                     float startTime;
        private                     float duration;
        private                     type  startValue;
        private                     type  baseSpeed;
        private                     type  speed;
        private                     float currentTime;
        private                     type  currentValue;
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        public idExtrapolate() {
            extrapolationType = EXTRAPOLATION_NONE;
            startTime = duration = 0.0f;
//	memset( &startValue, 0, sizeof( startValue ) );
//	memset( &baseSpeed, 0, sizeof( baseSpeed ) );
//	memset( &speed, 0, sizeof( speed ) );
            currentTime = -1;
//            currentValue = startValue;
        }

        public void Init(final float startTime, final float duration, final type startValue, final type baseSpeed, final type speed, final /*extrapolation_t*/ int extrapolationType) {
            this.extrapolationType = extrapolationType;
            this.startTime = startTime;
            this.duration = duration;
            this.startValue = TempDump.clone(startValue);
            this.baseSpeed = TempDump.clone(baseSpeed);
            this.speed = TempDump.clone(speed);
            currentTime = -1;
            currentValue = TempDump.clone(startValue);
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
                    currentValue = _Plus(startValue, _Multiply(deltaTime, baseSpeed));
                    break;
                }
                case EXTRAPOLATION_LINEAR: {
                    deltaTime = (time - startTime) * 0.001f;
                    currentValue = _Plus(startValue, _Multiply(deltaTime, _Plus(baseSpeed, speed)));
                    break;
                }
                case EXTRAPOLATION_ACCELLINEAR: {
                    if (0 == duration) {
                        currentValue =TempDump.clone(startValue);
                    } else {
                        deltaTime = (time - startTime) / duration;
                        s = (0.5f * deltaTime * deltaTime) * (duration * 0.001f);
                        currentValue = _Plus(startValue, _Plus(_Multiply(deltaTime, baseSpeed), _Multiply(s, speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_DECELLINEAR: {
                    if (0 == duration) {
                        currentValue = TempDump.clone(startValue);
                    } else {
                        deltaTime = (time - startTime) / duration;
                        s = (deltaTime - (0.5f * deltaTime * deltaTime)) * (duration * 0.001f);
                        currentValue = _Plus(startValue, _Plus(_Multiply(deltaTime, baseSpeed), _Multiply(s, speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_ACCELSINE: {
                    if (0 == duration) {
                        currentValue = TempDump.clone(startValue);
                    } else {
                        deltaTime = (time - startTime) / duration;
                        s = (1.0f - idMath.Cos(deltaTime * idMath.HALF_PI)) * duration * 0.001f * idMath.SQRT_1OVER2;
                        currentValue = _Plus(startValue, _Plus(_Multiply(deltaTime, baseSpeed), _Multiply(s, speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_DECELSINE: {
                    if (0 == duration) {
                        currentValue = TempDump.clone(startValue);
                    } else {
                        deltaTime = (time - startTime) / duration;
                        s = idMath.Sin(deltaTime * idMath.HALF_PI) * duration * 0.001f * idMath.SQRT_1OVER2;
                        currentValue = _Plus(startValue, _Plus(_Multiply(deltaTime, baseSpeed), _Multiply(s, speed)));
                    }
                    break;
                }
            }
            return currentValue;
        }

        public type GetCurrentSpeed(float time) {
            float deltaTime, s;

            if (time < startTime || 0 == duration) {
                return _Minus(startValue, startValue);
            }

            if (0 == (extrapolationType & EXTRAPOLATION_NOSTOP) && (time > startTime + duration)) {
                return _Minus(startValue, startValue);
            }

            switch (extrapolationType & ~EXTRAPOLATION_NOSTOP) {
                case EXTRAPOLATION_NONE: {
                    return baseSpeed;
                }
                case EXTRAPOLATION_LINEAR: {
                    return _Plus(baseSpeed, speed);
                }
                case EXTRAPOLATION_ACCELLINEAR: {
                    deltaTime = (time - startTime) / duration;
                    s = deltaTime;
                    return _Plus(baseSpeed, _Multiply(s, speed));
                }
                case EXTRAPOLATION_DECELLINEAR: {
                    deltaTime = (time - startTime) / duration;
                    s = 1.0f - deltaTime;
                    return _Plus(baseSpeed, _Multiply(s, speed));
                }
                case EXTRAPOLATION_ACCELSINE: {
                    deltaTime = (time - startTime) / duration;
                    s = idMath.Sin(deltaTime * idMath.HALF_PI);
                    return _Plus(baseSpeed, _Multiply(s, speed));
                }
                case EXTRAPOLATION_DECELSINE: {
                    deltaTime = (time - startTime) / duration;
                    s = idMath.Cos(deltaTime * idMath.HALF_PI);
                    return _Plus(baseSpeed, _Multiply(s, speed));
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
            startValue = TempDump.clone(value);
            currentTime = -1;
        }

        public final type GetStartValue() {
            return startValue;
        }

        public final type GetBaseSpeed() {
            final type b = this.baseSpeed;
//            if (b instanceof idVec3) {
//                return (type) (new idVec3((idVec3) b));
//            } else if (b instanceof idVec4) {
//                return (type) (new idVec4((idVec4) b));
//            } else if (b instanceof idAngles) {
//                return (type) (new idAngles((idAngles) b));
//            }

            return b;
        }

        public final type GetSpeed() {
            final type s = this.speed;
//            if (s instanceof idVec3) {
//                return (type) (new idVec3((idVec3) s));
//            } else if (s instanceof idVec4) {
//                return (type) (new idVec4((idVec4) s));
//            } else if (s instanceof idAngles) {
//                return (type) (new idAngles((idAngles) s));
//            }

            return s;
        }

        public /*extrapolation_t*/ int GetExtrapolationType() {
            return extrapolationType;
        }

        private type _Multiply(final float f, final type t) {
            if (t instanceof idVec3) {
                return (type) ((idVec3) t).oMultiply(f);
            } else if (t instanceof idVec4) {
                return (type) ((idVec4) t).oMultiply(f);
            } else if (t instanceof idAngles) {
                return (type) ((idAngles) t).oMultiply(f);
            } else if (t instanceof Double) {
                return (type) Double.valueOf(f * ((Double) t));
            }

            return (type) Float.valueOf(f * ((Float) t));
        }

        private type _Plus(final type t1, final type t2) {
            if (t1 instanceof idVec3) {
                return (type) ((idVec3) t1).oPlus((idVec3) t2);
            } else if (t1 instanceof idVec4) {
                return (type) ((idVec4) t1).oPlus((idVec4) t2);
            } else if (t1 instanceof idAngles) {
                return (type) ((idAngles) t1).oPlus((idAngles) t2);
            } else if (t1 instanceof Double) {
                return (type) Double.valueOf((Double) t1 + (Double) t2);
            }

            return (type) Float.valueOf((Float) t1 + (Float) t2);
        }

        private type _Minus(final type t1, final type t2) {
            if (t1 instanceof idVec3) {
                return (type) ((idVec3) t1).oMinus((idVec3) t2);
            } else if (t1 instanceof idVec4) {
                return (type) ((idVec4) t1).oMinus((idVec4) t2);
            } else if (t1 instanceof idAngles) {
                return (type) ((idAngles) t1).oMinus((idAngles) t2);
            }

            return (type) Float.valueOf((Float) t1 - (Float) t2);
        }
    };
}
