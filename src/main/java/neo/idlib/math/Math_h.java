package neo.idlib.math;

/**
 * ===============================================================================
 *
 * Math
 *
 * ===============================================================================
 */
public class Math_h {

    public static float DEG2RAD(float a) {
        return (a) * idMath.M_DEG2RAD;
    }

    public static float RAD2DEG(float a) {
        return (a) * idMath.M_RAD2DEG;
    }

    public static float SEC2MS(float t) {
        return idMath.FtoiFast((t * idMath.M_SEC2MS));
    }

    public static float MS2SEC(float t) {
        return t * idMath.M_MS2SEC;//TODO:doest anybody need the double returns?
    }

    public static float ANGLE2SHORT(float x) {
        return idMath.FtoiFast((x) * 65536.0f / 360.0f) & 65535;
    }

    public static float SHORT2ANGLE(float x) {
        return (x) * (360.0f / 65536.0f);
    }

    public static float ANGLE2BYTE(float x) {
        return idMath.FtoiFast((x) * 256.0f / 360.0f) & 255;
    }

    public static float BYTE2ANGLE(float x) {
        return (x) * (360.0f / 256.0f);
    }

    public static int FLOATSIGNBITSET(float f) {
        return (/**
                 * (const unsigned long *)&
                 */
                (Float.floatToIntBits(f) & 0x80000000) == 0) ? 0 : 1;
    }

    public static int FLOATSIGNBITNOTSET(float f) {
        return /*(~(*(const unsigned long *)&(f))) >> 31;}*/ (((~Float.floatToIntBits(f)) & 0x80000000) == 0) ? 0 : 1;
    }

    public static boolean FLOATNOTZERO(float f) /*{return (*(const unsigned long *)&(f)) & ~(1<<31) ;}*/ {
        return f != 0.0f;
    }

    public static int INTSIGNBITSET(int i) {
        return ((i)) >>> 31;
    }

    public static int INTSIGNBITNOTSET(int i) {
        return (~((i))) >>> 31;
    }

    public static boolean FLOAT_IS_NAN(float x) /*(((*(const unsigned long *)&x) & 0x7f800000) == 0x7f800000)*/ {
        return Float.isNaN(x);
    }

    static boolean FLOAT_IS_INF(float x) {
        return ((Float.floatToIntBits(x)) & 0x7fffffff) == 0x7f800000;
    }

    static boolean FLOAT_IS_IND(float x) {
        return x == 0xffc00000;
    }

    public static boolean FLOAT_IS_DENORMAL(float x) {
        return ((Float.floatToIntBits(x) & 0x7f800000) == 0x00000000) && ((Float.floatToIntBits(x) & 0x007fffff) != 0x00000000);
    }

    private static final int IEEE_FLT_MANTISSA_BITS  = 23;
    private static final int IEEE_FLT_EXPONENT_BITS  = 8;
    private static final int IEEE_FLT_EXPONENT_BIAS  = 127;
    private static final int IEEE_FLT_SIGN_BIT       = 31;
    private static final int IEEE_DBL_MANTISSA_BITS  = 52;
    private static final int IEEE_DBL_EXPONENT_BITS  = 11;
    private static final int IEEE_DBL_EXPONENT_BIAS  = 1023;
    private static final int IEEE_DBL_SIGN_BIT       = 63;
    private static final int IEEE_DBLE_MANTISSA_BITS = 63;
    private static final int IEEE_DBLE_EXPONENT_BITS = 15;
    private static final int IEEE_DBLE_EXPONENT_BIAS = 0;
    private static final int IEEE_DBLE_SIGN_BIT      = 79;

    public static int MaxIndex(float x, float y) {
        return (x > y) ? 0 : 1;
    }

    public static int MinIndex(float x, float y) {
        return (x < y) ? 0 : 1;
    }

    public static float Max3(float x, float y, float z) {
        return (x > y) ? ((x > z) ? x : z) : ((y > z) ? y : z);
    }

    public static float Min3(float x, float y, float z) {
        return (x < y) ? ((x < z) ? x : z) : ((y < z) ? y : z);
    }

    public static int Max3Index(float x, float y, float z) {
        return (x > y) ? ((x > z) ? 0 : 2) : ((y > z) ? 1 : 2);
    }

    public static int Min3Index(float x, float y, float z) {
        return (x < y) ? ((x < z) ? 0 : 2) : ((y < z) ? 1 : 2);
    }

    public static int Sign(float f) {
        return (f > 0) ? 1 : ((f < 0) ? -1 : 0);
    }

    public static float Square(float x) {//FUCKME: promoting float to double!
        return x * x;
    }

    public static float Cube(float x) {
        return x * x * x;
    }

    public static class idMath {

        public static void Init() {
            _flint fi, fo;

            for (int i = 0; i < SQRT_TABLE_SIZE; i++) {
                fi = new _flint(((EXP_BIAS - 1) << EXP_POS) | (i << LOOKUP_POS));
                fo = new _flint((float) (1.0 / Math.sqrt(fi.f)));
                iSqrt[i] = ((long) (((fo.i + (1 << (SEED_POS - 2))) >> SEED_POS) & 0xFF)) << SEED_POS;
            }

            iSqrt[SQRT_TABLE_SIZE / 2] = ((long) (0xFF)) << (SEED_POS);

            initialized = true;
        }

        public static float RSqrt(float x) {// reciprocal square root, returns huge number when x == 0.0

            long i;
            float y, r;

            y = x * 0.5f;
//	i = *reinterpret_cast<long *>( &x );
            i = Float.floatToIntBits(x);
            i = 0x5f3759df - (i >> 1);
            r = Float.intBitsToFloat((int) i);
            r = r * (1.5f - r * r * y);
            return r;
        }

        // inverse square root with 32 bits precision, returns huge number when x == 0.0
        public static float InvSqrt(float x) {

//	long  a = ((union _flint*)(&x))->i;
            _flint seed = new _flint(x);
            int a = seed.getI();

            assert (initialized);

            double y = x * 0.5f;
            seed.setI((int) (((((3 * EXP_BIAS - 1) - ((a >> EXP_POS) & 0xFF)) >> 1) << EXP_POS)
                    | iSqrt[(a >> (EXP_POS - LOOKUP_BITS)) & LOOKUP_MASK]));

            double r = seed.f;
            r = r * (1.5f - r * r * y);
            r = r * (1.5f - r * r * y);
            return (float) r;
        }

        static float InvSqrt16(float x) {// inverse square root with 16 bits precision, returns huge number when x == 0.0

            _flint seed = new _flint(x);
            int a = seed.getI();

            assert (initialized);

            double y = x * 0.5f;
            seed.setI((int) (((((3 * EXP_BIAS - 1) - ((a >> EXP_POS) & 0xFF)) >> 1) << EXP_POS) | iSqrt[(a >> (EXP_POS - LOOKUP_BITS)) & LOOKUP_MASK]));
            double r = seed.f;
            r = r * (1.5f - r * r * y);
            return (float) r;
        }

        static double InvSqrt64(float x) {// inverse square root with 64 bits precision, returns huge number when x == 0.0
            _flint seed = new _flint(x);
            int a = seed.getI();

            assert (initialized);

            double y = x * 0.5f;
            seed.setI((int) (((((3 * EXP_BIAS - 1) - ((a >> EXP_POS) & 0xFF)) >> 1) << EXP_POS) | iSqrt[(a >> (EXP_POS - LOOKUP_BITS)) & LOOKUP_MASK]));
            double r = seed.f;
            r = r * (1.5f - r * r * y);
            r = r * (1.5f - r * r * y);
            r = r * (1.5f - r * r * y);
            return r;
        }

        public static float Sqrt(float x) {// square root with 32 bits precision
            return x * InvSqrt(x);
        }

        public static float Sqrt16(float x) {// square root with 16 bits precision
            return x * InvSqrt16(x);
        }

        static double Sqrt64(float x) {// square root with 64 bits precision
            return x * InvSqrt64(x);
        }
//TODO:radians?

        public static float Sin(float a) {
            return (float) Math.sin(a);
        }			// sine with 32 bits precision

        public static float Sin16(float a) {// sine with 16 bits precision, maximum absolute error is 2.3082e-09
            float s;

            if ((a < 0.0f) || (a >= TWO_PI)) {
//		a -= floorf( a / TWO_PI ) * TWO_PI;
                a -= Math.floor(a / TWO_PI) * TWO_PI;
            }
//#if 1
            if (a < PI) {
                if (a > HALF_PI) {
                    a = PI - a;
                }
            } else {
                if (a > PI + HALF_PI) {
                    a = a - TWO_PI;
                } else {
                    a = PI - a;
                }
            }
//#else
//	a = PI - a;
//	if ( fabs( a ) >= HALF_PI ) {
//		a = ( ( a < 0.0f ) ? -PI : PI ) - a;
//	}
//#endif
            s = a * a;
            return (float) (a * (((((-2.39e-08f * s + 2.7526e-06f) * s - 1.98409e-04f) * s + 8.3333315e-03f) * s - 1.666666664e-01f) * s + 1.0f));
        }

        static double Sin64(float a) {
            return Math.sin(a);
        }			// sine with 64 bits precision

        public static float Cos(float a) {
            return (float) Math.cos(a);
        }			// cosine with 32 bits precision

        public static float Cos16(float a) {// cosine with 16 bits precision, maximum absolute error is 2.3082e-09
            float s, d;

            if ((a < 0.0f) || (a >= TWO_PI)) {
//		a -= floorf( a / TWO_PI ) * TWO_PI;
                a -= Math.floor(a / TWO_PI) * TWO_PI;
            }
//#if 1
            if (a < PI) {
                if (a > HALF_PI) {
                    a = PI - a;
                    d = -1.0f;
                } else {
                    d = 1.0f;
                }
            } else {
                if (a > PI + HALF_PI) {
                    a = a - TWO_PI;
                    d = 1.0f;
                } else {
                    a = PI - a;
                    d = -1.0f;
                }
            }
//#else
//	a = PI - a;
//	if ( fabs( a ) >= HALF_PI ) {
//		a = ( ( a < 0.0f ) ? -PI : PI ) - a;
//		d = 1.0f;
//	} else {
//		d = -1.0f;
//	}
//#endif
            s = a * a;
            return (float) (d * (((((-2.605e-07f * s + 2.47609e-05f) * s - 1.3888397e-03f) * s + 4.16666418e-02f) * s - 4.999999963e-01f) * s + 1.0f));
        }

        static double Cos64(float a) {
            return Math.cos(a);
        }		// cosine with 64 bits precision

        public static void SinCos(float a, float[] s, float[] c) {// sine and cosine with 32 bits precision
//#ifdef _WIN32//i wish.
//	_asm {
//		fld		a
//		fsincos
//		mov		ecx, c
//		mov		edx, s
//		fstp	dword ptr [ecx]
//		fstp	dword ptr [edx]
//	}
//#else
            s[0] = (float) Math.sin(a);
            c[0] = (float) Math.cos(a);
//#endif
        }

        public static void SinCos16(float a, float[] s, float[] c) {// sine and cosine with 16 bits precision
            float t, d;

            if ((a < 0.0f) || (a >= idMath.TWO_PI)) {
//		a -= floorf( a / idMath::TWO_PI ) * idMath::TWO_PI;
                a -= Math.floor(a / idMath.TWO_PI) * idMath.TWO_PI;
            }
//#if 1
            if (a < PI) {
                if (a > HALF_PI) {
                    a = PI - a;
                    d = -1.0f;
                } else {
                    d = 1.0f;
                }
            } else {
                if (a > PI + HALF_PI) {
                    a = a - TWO_PI;
                    d = 1.0f;
                } else {
                    a = PI - a;
                    d = -1.0f;
                }
            }
//#else
//	a = PI - a;
//	if ( fabs( a ) >= HALF_PI ) {
//		a = ( ( a < 0.0f ) ? -PI : PI ) - a;
//		d = 1.0f;
//	} else {
//		d = -1.0f;
//	}
//#endif
            t = a * a;
            s[0] = a * (((((-2.39e-08f * t + 2.7526e-06f) * t - 1.98409e-04f) * t + 8.3333315e-03f) * t - 1.666666664e-01f) * t + 1.0f);
            c[0] = d * (((((-2.605e-07f * t + 2.47609e-05f) * t - 1.3888397e-03f) * t + 4.16666418e-02f) * t - 4.999999963e-01f) * t + 1.0f);
        }

        static void SinCos64(float a, float[] s, float[] c) {// sine and cosine with 64 bits precision
//#ifdef _WIN32
//	_asm {
//		fld		a
//		fsincos
//		mov		ecx, c
//		mov		edx, s
//		fstp	qword ptr [ecx]
//		fstp	qword ptr [edx]
//	}
//#else
            s[0] = (float) Math.sin(a);
            c[0] = (float) Math.cos(a);
//#endif
        }

        public static float Tan(float a) {// tangent with 32 bits precision
            return (float) Math.tan(a);
        }

        public static float Tan16(float a) {// tangent with 16 bits precision, maximum absolute error is 1.8897e-08
            float s;
            boolean reciprocal;

            if ((a < 0.0f) || (a >= PI)) {
//		a -= floorf( a / PI ) * PI;
                a -= Math.floor(a / PI) * PI;
            }
//#if 1
            if (a < HALF_PI) {
                if (a > ONEFOURTH_PI) {
                    a = HALF_PI - a;
                    reciprocal = true;
                } else {
                    reciprocal = false;
                }
            } else {
                if (a > HALF_PI + ONEFOURTH_PI) {
                    a = a - PI;
                    reciprocal = false;
                } else {
                    a = HALF_PI - a;
                    reciprocal = true;
                }
            }
//#else
//	a = HALF_PI - a;
//	if ( fabs( a ) >= ONEFOURTH_PI ) {
//		a = ( ( a < 0.0f ) ? -HALF_PI : HALF_PI ) - a;
//		reciprocal = false;
//	} else {
//		reciprocal = true;
//	}
//#endif
            s = a * a;
            s = a * ((((((9.5168091e-03f * s + 2.900525e-03f) * s + 2.45650893e-02f) * s + 5.33740603e-02f) * s + 1.333923995e-01f) * s + 3.333314036e-01f) * s + 1.0f);
            if (reciprocal) {
                return 1.0f / s;
            } else {
                return s;
            }
        }

        static double Tan64(float a) {// tangent with 64 bits precision
            return Math.tan(a);
        }

        static float ASin(float a) {// arc sine with 32 bits precision, input is clamped to [-1, 1] to avoid a silent NaN
            if (a <= -1.0f) {
                return -HALF_PI;
            }
            if (a >= 1.0f) {
                return HALF_PI;
            }
            return (float) Math.asin(a);
        }

        static float ASin16(float a) {// arc sine with 16 bits precision, maximum absolute error is 6.7626e-05
            if (1 == FLOATSIGNBITSET(a)) {
                if (a <= -1.0f) {
                    return -HALF_PI;
                }
                a = Math.abs(a);
                return (float) ((((-0.0187293f * a + 0.0742610f) * a - 0.2121144f) * a + 1.5707288f) * Math.sqrt(1.0f - a) - HALF_PI);
            } else {
                if (a >= 1.0f) {
                    return HALF_PI;
                }
                return (float) (HALF_PI - (((-0.0187293f * a + 0.0742610f) * a - 0.2121144f) * a + 1.5707288f) * Math.sqrt(1.0f - a));
            }
        }

        static double ASin64(float a) {// arc sine with 64 bits precision
            if (a <= -1.0f) {
                return -HALF_PI;
            }
            if (a >= 1.0f) {
                return HALF_PI;
            }
            return Math.sin(a);
        }

        public static float ACos(float a) {// arc cosine with 32 bits precision, input is clamped to [-1, 1] to avoid a silent NaN
            if (a <= -1.0f) {
                return PI;
            }
            if (a >= 1.0f) {
                return 0.0f;
            }
            return (float) Math.acos(a);
        }

        public static float ACos16(float a) {// arc cosine with 16 bits precision, maximum absolute error is 6.7626e-05
            if (1 == FLOATSIGNBITSET(a)) {
                if (a <= -1.0f) {
                    return PI;
                }
                a = Math.abs(a);
                return (float) (PI - (((-0.0187293f * a + 0.0742610f) * a - 0.2121144f) * a + 1.5707288f) * Math.sqrt(1.0f - a));
            } else {
                if (a >= 1.0f) {
                    return 0.0f;
                }
                return (float) ((((-0.0187293f * a + 0.0742610f) * a - 0.2121144f) * a + 1.5707288f) * Math.sqrt(1.0f - a));
            }
        }

        static double ACos64(float a) {// arc cosine with 64 bits precision
            if (a <= -1.0f) {
                return PI;
            }
            if (a >= 1.0f) {
                return 0.0f;
            }
            return Math.acos(a);
        }

        static float ATan(float a) {// arc tangent with 32 bits precision
            return (float) Math.atan(a);
        }

        static float ATan16(float a) {// arc tangent with 16 bits precision, maximum absolute error is 1.3593e-08
            float s;

            if (Math.abs(a) > 1.0f) {
                a = 1.0f / a;
                s = a * a;
                s = -(((((((((0.0028662257f * s - 0.0161657367f) * s + 0.0429096138f) * s - 0.0752896400f)
                        * s + 0.1065626393f) * s - 0.1420889944f) * s + 0.1999355085f) * s - 0.3333314528f) * s) + 1.0f) * a;
                if (1 == FLOATSIGNBITSET(a)) {
                    return s - HALF_PI;
                } else {
                    return s + HALF_PI;
                }
            } else {
                s = a * a;
                return (((((((((0.0028662257f * s - 0.0161657367f) * s + 0.0429096138f) * s - 0.0752896400f)
                        * s + 0.1065626393f) * s - 0.1420889944f) * s + 0.1999355085f) * s - 0.3333314528f) * s) + 1.0f) * a;
            }
        }

        static double ATan64(float a) {// arc tangent with 64 bits precision
            return Math.atan(a);
        }

        static float ATan(float y, float x) {// arc tangent with 32 bits precision
            return (float) Math.atan2(y, x);
        }

        public static float ATan16(float y, float x) {// arc tangent with 16 bits precision, maximum absolute error is 1.3593e-08
            float a, s;

            if (Math.abs(y) > Math.abs(x)) {
                a = x / y;
                s = a * a;
                s = -(((((((((0.0028662257f * s - 0.0161657367f) * s + 0.0429096138f) * s - 0.0752896400f)
                        * s + 0.1065626393f) * s - 0.1420889944f) * s + 0.1999355085f) * s - 0.3333314528f) * s) + 1.0f) * a;
                if (1 == FLOATSIGNBITSET(a)) {
                    return s - HALF_PI;
                } else {
                    return s + HALF_PI;
                }
            } else {
                a = y / x;
                s = a * a;
                return (((((((((0.0028662257f * s - 0.0161657367f) * s + 0.0429096138f) * s - 0.0752896400f)
                        * s + 0.1065626393f) * s - 0.1420889944f) * s + 0.1999355085f) * s - 0.3333314528f) * s) + 1.0f) * a;
            }
        }

        static double ATan64(float y, float x) {// arc tangent with 64 bits precision
            return Math.atan2(y, x);
        }

        static float Pow(float x, float y) {// x raised to the power y with 32 bits precision
            return (float) Math.pow(x, y);
        }

        static float Pow16(float x, float y) {// x raised to the power y with 16 bits precision
            return Exp16(y * Log16(x));
        }

        static double Pow64(float x, float y) {// x raised to the power y with 64 bits precision
            return Math.pow(x, y);
        }

        static float Exp(float f) {// e raised to the power f with 32 bits precision
            return (float) Math.exp(f);
        }

        static float Exp16(float f) {// e raised to the power f with 16 bits precision
            int i, s, e, m, exponent;
            float x, x2, y, p, q;

            x = f * 1.44269504088896340f;		// multiply with ( 1 / log( 2 ) )
//#if 1
//	i = *reinterpret_cast<int *>(&x);
            i = Float.floatToIntBits(x);
            s = (i >> IEEE_FLT_SIGN_BIT);
            e = ((i >> IEEE_FLT_MANTISSA_BITS) & ((1 << IEEE_FLT_EXPONENT_BITS) - 1)) - IEEE_FLT_EXPONENT_BIAS;
            m = (i & ((1 << IEEE_FLT_MANTISSA_BITS) - 1)) | (1 << IEEE_FLT_MANTISSA_BITS);
            i = ((m >> (IEEE_FLT_MANTISSA_BITS - e)) & ~(e >> 31)) ^ s;
//#else
//	i = (int) x;
//	if ( x < 0.0f ) {
//		i--;
//	}
//#endif
            exponent = (i + IEEE_FLT_EXPONENT_BIAS) << IEEE_FLT_MANTISSA_BITS;
//	y = *reinterpret_cast<float *>(&exponent);
            y = Float.intBitsToFloat(exponent);
            x -= (float) i;
            if (x >= 0.5f) {
                x -= 0.5f;
                y *= 1.4142135623730950488f;	// multiply with sqrt( 2 )
            }
            x2 = x * x;
            p = x * (7.2152891511493f + x2 * 0.0576900723731f);
            q = 20.8189237930062f + x2;
            x = y * (q + p) / (q - p);
            return x;
        }

        static double Exp64(float f) {// e raised to the power f with 64 bits precision
            return Math.exp(f);
        }

        static float Log(float f) {// natural logarithm with 32 bits precision
            return (float) Math.log(f);
        }

        static float Log16(float f) {// natural logarithm with 16 bits precision
            int i, exponent;
            float y, y2;

//	i = *reinterpret_cast<int *>(&f);
            i = Float.floatToIntBits(f);
            exponent = ((i >> IEEE_FLT_MANTISSA_BITS) & ((1 << IEEE_FLT_EXPONENT_BITS) - 1)) - IEEE_FLT_EXPONENT_BIAS;
            i -= (exponent + 1) << IEEE_FLT_MANTISSA_BITS;	// get value in the range [.5, 1>
//	y = *reinterpret_cast<float *>(&i);
            y = Float.intBitsToFloat(i);
            y *= 1.4142135623730950488f;						// multiply with sqrt( 2 )
            y = (y - 1.0f) / (y + 1.0f);
            y2 = y * y;
            y = y * (2.000000000046727f + y2 * (0.666666635059382f + y2 * (0.4000059794795f + y2 * (0.28525381498f + y2 * 0.2376245609f))));
            y += 0.693147180559945f * ((float) exponent + 0.5f);
            return y;
        }

        static double Log64(float f) {// natural logarithm with 64 bits precision
            return Math.log(f);
        }

        public static int IPow(int x, int y) {// integral x raised to the power y
            int r;
            for (r = x; y > 1; y--) {
                r *= x;
            }
            return r;
        }

        static int ILog2(float f) {// integral base-2 logarithm of the floating point value
//	return ( ( (*reinterpret_cast<int *>(&f)) >> IEEE_FLT_MANTISSA_BITS ) & ( ( 1 << IEEE_FLT_EXPONENT_BITS ) - 1 ) ) - IEEE_FLT_EXPONENT_BIAS;
            return (((Float.floatToIntBits(f)) >> IEEE_FLT_MANTISSA_BITS) & ((1 << IEEE_FLT_EXPONENT_BITS) - 1)) - IEEE_FLT_EXPONENT_BIAS;
        }

        static int ILog2(int i) {// integral base-2 logarithm of the integer value
            return ILog2((float) i);
        }

        public static int BitsForFloat(float f) {// minumum number of bits required to represent ceil( f )
            return ILog2(f) + 1;
        }

        public static int BitsForInteger(int i) {// minumum number of bits required to represent i
            return ILog2((float) i) + 1;
        }

        static int MaskForFloatSign(float f) {// returns 0x00000000 if x >= 0.0f and returns 0xFFFFFFFF if x <= -0.0f
//	return ( (*reinterpret_cast<int *>(&f)) >> 31 );
            return ((Float.floatToIntBits(f)) >> 31);
        }

        static int MaskForIntegerSign(int i) {// returns 0x00000000 if x >= 0 and returns 0xFFFFFFFF if x < 0
            return (i >> 31);
        }

        static int FloorPowerOfTwo(int x) {// round x down to the nearest power of 2
            return CeilPowerOfTwo(x) >> 1;
        }

        static int CeilPowerOfTwo(int x) {// round x up to the nearest power of 2
            x--;
            x |= x >> 1;
            x |= x >> 2;
            x |= x >> 4;
            x |= x >> 8;
            x |= x >> 16;
            x++;
            return x;
        }

        public static boolean IsPowerOfTwo(int x) {// returns true if x is a power of 2
            return (x & (x - 1)) == 0 && x > 0;
        }

        static int BitCount(int x) {// returns the number of 1 bits in x
            x -= ((x >> 1) & 0x55555555);
            x = (((x >> 2) & 0x33333333) + (x & 0x33333333));
            x = (((x >> 4) + x) & 0x0f0f0f0f);
            x += (x >> 8);
            return ((x + (x >> 16)) & 0x0000003f);
        }

        static int BitReverse(int x) {// returns the bit reverse of x
            x = (((x >> 1) & 0x55555555) | ((x & 0x55555555) << 1));
            x = (((x >> 2) & 0x33333333) | ((x & 0x33333333) << 2));
            x = (((x >> 4) & 0x0f0f0f0f) | ((x & 0x0f0f0f0f) << 4));
            x = (((x >> 8) & 0x00ff00ff) | ((x & 0x00ff00ff) << 8));
            return ((x >> 16) | (x << 16));
        }

        public static int Abs(int x) {// returns the absolute value of the integer value (for reference only)
            int y = x >> 31;
            return ((x ^ y) - y);
        }

        // returns the absolute value of the floating point value
        public static float Fabs(float f) {
//	int tmp = *reinterpret_cast<int *>( &f );
            int tmp = Float.floatToIntBits(f);
            tmp &= 0x7FFFFFFF;
//	return *reinterpret_cast<float *>( &tmp );
            return Float.intBitsToFloat(tmp);
        }

        public static float Floor(float f) {// returns the largest integer that is less than or equal to the given value
            return (float) Math.floor(f);
        }

        public static float Ceil(float f) {// returns the smallest integer that is greater than or equal to the given value
            return (float) Math.ceil(f);
        }

        public static float Rint(float f) {// returns the nearest integer
            return (float) Math.floor(f + 0.5f);
        }

        public static int Ftoi(float f) {// float to int conversion
            return (int) f;
        }

        // fast float to int conversion but uses current FPU round mode (default round nearest)
        public static int FtoiFast(float f) {
//#ifdef _WIN32
//	int i;
//	__asm fld		f
//	__asm fistp		i		// use default rouding mode (round nearest)
//	return i;
//#elif 0						// round chop (C/C++ standard)
//            int i, s, e, m, shift;
//            i = Float.floatToIntBits(f);//*reinterpret_cast<int *>(&f);
//            s = i >> IEEE_FLT_SIGN_BIT;
//            e = ((i >> IEEE_FLT_MANTISSA_BITS) & ((1 << IEEE_FLT_EXPONENT_BITS) - 1)) - IEEE_FLT_EXPONENT_BIAS;
//            m = (i & ((1 << IEEE_FLT_MANTISSA_BITS) - 1)) | (1 << IEEE_FLT_MANTISSA_BITS);
//            shift = e - IEEE_FLT_MANTISSA_BITS;
//            return ((((m >> -shift) | (m << shift)) & ~(e >> 31)) ^ s) - s;
            return Math.round(f);//TODO:fix the C++ function.
//#elif defined( __i386__ )
//#elif 0
//	int i = 0;
//	__asm__ __volatile__ (
//						  "fld %1\n" \
//						  "fistp %0\n" \
//						  : "=m" (i) \
//						  : "m" (f) );
//	return i;
//#else
//	return (int) f;
//#endif
        }

        public static long Ftol(float f) {// float to long conversion
            return (long) f;
        }

        static long FtolFast(float f) {// fast float to long conversion but uses current FPU round mode (default round nearest)
//#ifdef _WIN32
//	// FIXME: this overflows on 31bits still .. same as FtoiFast
//	unsigned long i;
//	__asm fld		f
//	__asm fistp		i		// use default rouding mode (round nearest)
//	return i;
//#elif 0						// round chop (C/C++ standard)
            int i, s, e, m, shift;
//	i = *reinterpret_cast<int *>(&f);
            i = Float.floatToIntBits(f);
            s = i >> IEEE_FLT_SIGN_BIT;
            e = ((i >> IEEE_FLT_MANTISSA_BITS) & ((1 << IEEE_FLT_EXPONENT_BITS) - 1)) - IEEE_FLT_EXPONENT_BIAS;
            m = (i & ((1 << IEEE_FLT_MANTISSA_BITS) - 1)) | (1 << IEEE_FLT_MANTISSA_BITS);
            shift = e - IEEE_FLT_MANTISSA_BITS;
            return ((((m >> -shift) | (m << shift)) & ~(e >> 31)) ^ s) - s;
//#elif defined( __i386__ )
//#elif 0
//	// for some reason, on gcc I need to make sure i == 0 before performing a fistp
//	int i = 0;
//	__asm__ __volatile__ (
//						  "fld %1\n" \
//						  "fistp %0\n" \
//						  : "=m" (i) \
//						  : "m" (f) );
//	return i;
//#else
//	return (unsigned long) f;
//#endif
        }

        public static char ClampChar(int i) {
            if (i < -128) {//goddamn unsigned char!!
                return (char) -128;
            }
            if (i > 127) {
                return 127;
            }
            return (char) i;
        }

        public static short ClampShort(int i) {//TODO:signed
            if (i < -32768) {
                return -32768;
            }
            if (i > 32767) {
                return 32767;
            }
            return (short) i;
        }

        public static int ClampInt(int min, int max, int value) {
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        }

        public static float ClampFloat(float min, float max, float value) {
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        }

        public static float AngleNormalize360(float angle) {
            if ((angle >= 360.0f) || (angle < 0.0f)) {
                angle -= Math.floor(angle / 360.0f) * 360.0f;
            }
            return angle;
        }

        public static float AngleNormalize180(float angle) {
            angle = AngleNormalize360(angle);
            if (angle > 180.0f) {
                angle -= 360.0f;
            }
            return angle;
        }

        public static float AngleDelta(float angle1, float angle2) {
            return AngleNormalize180(angle1 - angle2);
        }

        public static int FloatToBits(float f, int exponentBits, int mantissaBits) {
            int i, sign, exponent, mantissa, value;

            assert (exponentBits >= 2 && exponentBits <= 8);
            assert (mantissaBits >= 2 && mantissaBits <= 23);

            int maxBits = (((1 << (exponentBits - 1)) - 1) << mantissaBits) | ((1 << mantissaBits) - 1);
            int minBits = (((1 << exponentBits) - 2) << mantissaBits) | 1;

            float max = BitsToFloat(maxBits, exponentBits, mantissaBits);
            float min = BitsToFloat(minBits, exponentBits, mantissaBits);

            if (f >= 0.0f) {
                if (f >= max) {
                    return maxBits;
                } else if (f <= min) {
                    return minBits;
                }
            } else {
                if (f <= -max) {
                    return (maxBits | (1 << (exponentBits + mantissaBits)));
                } else if (f >= -min) {
                    return (minBits | (1 << (exponentBits + mantissaBits)));
                }
            }

            exponentBits--;
//	i = *reinterpret_cast<int *>(&f);
            i = Float.floatToIntBits(f);
            sign = (i >> IEEE_FLT_SIGN_BIT) & 1;
            exponent = ((i >> IEEE_FLT_MANTISSA_BITS) & ((1 << IEEE_FLT_EXPONENT_BITS) - 1)) - IEEE_FLT_EXPONENT_BIAS;
            mantissa = i & ((1 << IEEE_FLT_MANTISSA_BITS) - 1);
            value = sign << (1 + exponentBits + mantissaBits);
            value |= ((INTSIGNBITSET(exponent) << exponentBits) | (Math.abs(exponent) & ((1 << exponentBits) - 1))) << mantissaBits;
            value |= mantissa >> (IEEE_FLT_MANTISSA_BITS - mantissaBits);
            return value;
        }

        public static float BitsToFloat(int i, int exponentBits, int mantissaBits) {
            int exponentSign[] = {1, -1};
            int sign, exponent, mantissa, value;

            assert (exponentBits >= 2 && exponentBits <= 8);
            assert (mantissaBits >= 2 && mantissaBits <= 23);

            exponentBits--;
            sign = i >> (1 + exponentBits + mantissaBits);
            exponent = ((i >> mantissaBits) & ((1 << exponentBits) - 1)) * exponentSign[(i >> (exponentBits + mantissaBits)) & 1];
            mantissa = (i & ((1 << mantissaBits) - 1)) << (IEEE_FLT_MANTISSA_BITS - mantissaBits);
            value = sign << IEEE_FLT_SIGN_BIT | (exponent + IEEE_FLT_EXPONENT_BIAS) << IEEE_FLT_MANTISSA_BITS | mantissa;
//	return *reinterpret_cast<float *>(&value);
            return Float.intBitsToFloat(value);
        }

        public static int FloatHash(final float[] array, final int numFloats) {
            int i, hash = 0;
//	const int *ptr;

//	ptr = reinterpret_cast<const int *>( array );
            for (i = 0; i < numFloats; i++) {
//		hash ^= ptr[i];
                hash ^= Float.floatToIntBits(array[i]);
            }
            return hash;
        }

        public static final float PI           = 3.14159265358979323846f;// pi
        public static final float TWO_PI       = 2.0f * PI;// pi * 2
        static final        float HALF_PI      = 0.5f * PI;// pi / 2
        static final        float ONEFOURTH_PI = 0.25f * PI;// pi / 4
        static final        float E            = 2.71828182845904523536f;// e
        static final        float SQRT_TWO     = 1.41421356237309504880f;// sqrt( 2 )
        static final        float SQRT_THREE   = 1.73205080756887729352f;// sqrt( 3 )
        public static final float SQRT_1OVER2  = 0.70710678118654752440f;// sqrt( 1 / 2 )
        static final        float SQRT_1OVER3  = 0.57735026918962576450f;// sqrt( 1 / 3 )
        public static final float M_DEG2RAD    = PI / 180.0f;// degrees to radians multiplier
        public static final float M_RAD2DEG    = 180.0f / PI;// radians to degrees multiplier
        static final        float M_SEC2MS     = 1000.0f;// seconds to milliseconds multiplier
        public static final float M_MS2SEC     = 0.001f;// milliseconds to seconds multiplier
        public static final float INFINITY     = 1e30f;// huge number which should be larger than any valid number used
        public static final float FLT_EPSILON  = 1.192092896e-07f;// smallest positive number such that 1.0+FLT_EPSILON != 1.0
        //	enum {
        public static final int LOOKUP_BITS     = 8;
        public static final int EXP_POS         = 23;
        public static final int EXP_BIAS        = 127;
        public static final int LOOKUP_POS      = (EXP_POS - LOOKUP_BITS);
        public static final int SEED_POS        = (EXP_POS - 8);
        public static final int SQRT_TABLE_SIZE = (2 << LOOKUP_BITS);
        public static final int LOOKUP_MASK     = (SQRT_TABLE_SIZE - 1);
        //	};

        static class _flint {

            private int   i;
            private float f;

            public _flint(int i) {
                setI(i);
            }

            public _flint(float f) {
                setF(f);
            }

            public int getI() {
                return i;
            }

            public void setI(int i) {
                this.i = i;
                this.f = Float.intBitsToFloat(i);
            }

            public float getF() {
                return f;
            }

            public void setF(float f) {
                this.f = f;
                this.i = Float.floatToIntBits(f);
            }
        };
        private static long iSqrt[] = new long[SQRT_TABLE_SIZE];
        private static boolean initialized;
    }
}
