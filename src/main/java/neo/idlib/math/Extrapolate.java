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
            this.extrapolationType = EXTRAPOLATION_NONE;
            this.startTime = this.duration = 0.0f;
//	memset( &startValue, 0, sizeof( startValue ) );
//	memset( &baseSpeed, 0, sizeof( baseSpeed ) );
//	memset( &speed, 0, sizeof( speed ) );
            this.currentTime = -1;
//            currentValue = startValue;
        }

        public void Init(final float startTime, final float duration, final type startValue, final type baseSpeed, final type speed, final /*extrapolation_t*/ int extrapolationType) {
            this.extrapolationType = extrapolationType;
            this.startTime = startTime;
            this.duration = duration;
            this.startValue = TempDump.clone(startValue);
            this.baseSpeed = TempDump.clone(baseSpeed);
            this.speed = TempDump.clone(speed);
            this.currentTime = -1;
            this.currentValue = TempDump.clone(startValue);
        }

        public type GetCurrentValue(float time) {
            float deltaTime, s;

            if (time == this.currentTime) {
                return this.currentValue;
            }

            this.currentTime = time;

            if (time < this.startTime) {
                return this.startValue;
            }

            if ((0 == (this.extrapolationType & EXTRAPOLATION_NOSTOP)) && (time > (this.startTime + this.duration))) {
                time = this.startTime + this.duration;
            }

            switch (this.extrapolationType & ~EXTRAPOLATION_NOSTOP) {
                case EXTRAPOLATION_NONE: {
                    deltaTime = (time - this.startTime) * 0.001f;
                    this.currentValue = _Plus(this.startValue, _Multiply(deltaTime, this.baseSpeed));
                    break;
                }
                case EXTRAPOLATION_LINEAR: {
                    deltaTime = (time - this.startTime) * 0.001f;
                    this.currentValue = _Plus(this.startValue, _Multiply(deltaTime, _Plus(this.baseSpeed, this.speed)));
                    break;
                }
                case EXTRAPOLATION_ACCELLINEAR: {
                    if (0 == this.duration) {
                        this.currentValue =TempDump.clone(this.startValue);
                    } else {
                        deltaTime = (time - this.startTime) / this.duration;
                        s = (0.5f * deltaTime * deltaTime) * (this.duration * 0.001f);
                        this.currentValue = _Plus(this.startValue, _Plus(_Multiply(deltaTime, this.baseSpeed), _Multiply(s, this.speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_DECELLINEAR: {
                    if (0 == this.duration) {
                        this.currentValue = TempDump.clone(this.startValue);
                    } else {
                        deltaTime = (time - this.startTime) / this.duration;
                        s = (deltaTime - (0.5f * deltaTime * deltaTime)) * (this.duration * 0.001f);
                        this.currentValue = _Plus(this.startValue, _Plus(_Multiply(deltaTime, this.baseSpeed), _Multiply(s, this.speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_ACCELSINE: {
                    if (0 == this.duration) {
                        this.currentValue = TempDump.clone(this.startValue);
                    } else {
                        deltaTime = (time - this.startTime) / this.duration;
                        s = (1.0f - idMath.Cos(deltaTime * idMath.HALF_PI)) * this.duration * 0.001f * idMath.SQRT_1OVER2;
                        this.currentValue = _Plus(this.startValue, _Plus(_Multiply(deltaTime, this.baseSpeed), _Multiply(s, this.speed)));
                    }
                    break;
                }
                case EXTRAPOLATION_DECELSINE: {
                    if (0 == this.duration) {
                        this.currentValue = TempDump.clone(this.startValue);
                    } else {
                        deltaTime = (time - this.startTime) / this.duration;
                        s = idMath.Sin(deltaTime * idMath.HALF_PI) * this.duration * 0.001f * idMath.SQRT_1OVER2;
                        this.currentValue = _Plus(this.startValue, _Plus(_Multiply(deltaTime, this.baseSpeed), _Multiply(s, this.speed)));
                    }
                    break;
                }
            }
            return this.currentValue;
        }

        public type GetCurrentSpeed(float time) {
            float deltaTime, s;

            if ((time < this.startTime) || (0 == this.duration)) {
                return _Minus(this.startValue, this.startValue);
            }

            if ((0 == (this.extrapolationType & EXTRAPOLATION_NOSTOP)) && (time > (this.startTime + this.duration))) {
                return _Minus(this.startValue, this.startValue);
            }

            switch (this.extrapolationType & ~EXTRAPOLATION_NOSTOP) {
                case EXTRAPOLATION_NONE: {
                    return this.baseSpeed;
                }
                case EXTRAPOLATION_LINEAR: {
                    return _Plus(this.baseSpeed, this.speed);
                }
                case EXTRAPOLATION_ACCELLINEAR: {
                    deltaTime = (time - this.startTime) / this.duration;
                    s = deltaTime;
                    return _Plus(this.baseSpeed, _Multiply(s, this.speed));
                }
                case EXTRAPOLATION_DECELLINEAR: {
                    deltaTime = (time - this.startTime) / this.duration;
                    s = 1.0f - deltaTime;
                    return _Plus(this.baseSpeed, _Multiply(s, this.speed));
                }
                case EXTRAPOLATION_ACCELSINE: {
                    deltaTime = (time - this.startTime) / this.duration;
                    s = idMath.Sin(deltaTime * idMath.HALF_PI);
                    return _Plus(this.baseSpeed, _Multiply(s, this.speed));
                }
                case EXTRAPOLATION_DECELSINE: {
                    deltaTime = (time - this.startTime) / this.duration;
                    s = idMath.Cos(deltaTime * idMath.HALF_PI);
                    return _Plus(this.baseSpeed, _Multiply(s, this.speed));
                }
                default: {
                    return this.baseSpeed;
                }
            }
        }

        public boolean IsDone(float time) {
            return ((0 == (this.extrapolationType & EXTRAPOLATION_NOSTOP)) && (time >= (this.startTime + this.duration)));
        }

        public void SetStartTime(float time) {
            this.startTime = time;
            this.currentTime = -1;
        }

        public float GetStartTime() {
            return this.startTime;
        }

        public float GetEndTime() {
            return ((0 == (this.extrapolationType & EXTRAPOLATION_NOSTOP)) && (this.duration > 0)) ? this.startTime + this.duration : 0;
        }

        public float GetDuration() {
            return this.duration;
        }

        public void SetStartValue(final type value) {
            this.startValue = TempDump.clone(value);
            this.currentTime = -1;
        }

        public final type GetStartValue() {
            return this.startValue;
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
            return this.extrapolationType;
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
    }
}
