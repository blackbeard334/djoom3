package neo.idlib.math;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;
import neo.TempDump.SERiAL;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Matrix.idMat4;
import neo.idlib.math.Matrix.idMatX;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Simd.SIMDProcessor;

/**
 *
 */
public class Vector {

    private static final idVec2 vec2_origin   = new idVec2(0.0f, 0.0f);
    private static final idVec3 vec3_origin   = new idVec3(0.0f, 0.0f, 0.0f);
    private static final idVec3 vec3_zero     = vec3_origin;
    private static final idVec4 vec4_origin   = new idVec4(0.0f, 0.0f, 0.0f, 0.0f);
    private static final idVec4 vec4_zero     = vec4_origin;
    private static final idVec5 vec5_origin   = new idVec5(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    private static final idVec6 vec6_origin   = new idVec6(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    private static final idVec6 vec6_zero     = vec6_origin;
    private static final idVec6 vec6_infinity = new idVec6(idMath.INFINITY, idMath.INFINITY, idMath.INFINITY, idMath.INFINITY, idMath.INFINITY, idMath.INFINITY);

    @Deprecated
    public static float RAD2DEG(double a) {
        return (float) (a * idMath.M_RAD2DEG);
    }

    public static idVec2 getVec2_origin() {
        return new idVec2(vec2_origin);
    }

    public static idVec3 getVec3_origin() {
        return new idVec3(vec3_origin);
    }

    public static idVec3 getVec3_zero() {
        return new idVec3(vec3_zero);
    }

    public static idVec4 getVec4_origin() {
        return new idVec4(vec4_origin);
    }

    public static idVec4 getVec4_zero() {
        return new idVec4(vec4_zero);
    }

    public static idVec5 getVec5_origin() {
        return new idVec5(vec5_origin);
    }

    public static idVec6 getVec6_origin() {
        return new idVec6(vec6_origin.p);
    }

    public static idVec6 getVec6_zero() {
        return new idVec6(vec6_zero.p);
    }

    public static idVec6 getVec6_infinity() {
        return new idVec6(vec6_infinity.p);
    }

    public interface idVec<type> {
        //reflection was too slow. 
        //never thought I would say this, but thank God for type erasure.

        public float oGet(final int index);

        public float oSet(final int index, final float value);

        public type oPlus(final type a);

        public type oMinus(final type a);

        public float oMultiply(final type a);

        public type oDivide(final float a);

    }

    //===============================================================
    //
    //	idVec2 - 2D vector
    //
    //===============================================================
    public static class idVec2 implements idVec<idVec2>, SERiAL {

        public static final transient int SIZE = 2 * Float.SIZE;
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public float x;
        public float y;

        public idVec2() {
        }

        public idVec2(final float x, final float y) {
            this.x = x;
            this.y = y;
        }

        public idVec2(final idVec2 v) {
            this.x = v.x;
            this.y = v.y;
        }

        public void Set(final float x, final float y) {
            this.x = x;
            this.y = y;
        }

        public void Zero() {
            x = y = 0.0f;
        }

//public	float			operator[]( int index ) const;
        @Override
        public float oSet(final int index, final float value) {
            if (index == 1) {
                return y = value;
            } else {
                return x = value;
            }
        }

        public float oPluSet(final int index, final float value) {
            if (index == 1) {
                return y += value;
            } else {
                return x += value;
            }
        }
//public	float &			operator[]( int index );

        @Override
        public float oGet(final int index) {//TODO:rename you lazy sod
            if (index == 1) {
                return y;
            }
            return x;
        }
//public	idVec2			operator-() const;

//public	float			operator*( const idVec2 &a ) const;
        @Override
        public float oMultiply(final idVec2 a) {
            return this.x * a.x + this.y * a.y;
        }

//public	idVec2			operator*( const float a ) const;
        public idVec2 oMultiply(final float a) {
            return new idVec2(this.x * a, this.y * a);
        }
//public	idVec2			operator/( const float a ) const;

        @Override
        public idVec2 oDivide(final float a) {
            float inva = 1.0f / a;
            return new idVec2(x * inva, y * inva);
        }

//public	idVec2			operator+( const idVec2 &a ) const;
        @Override
        public idVec2 oPlus(final idVec2 a) {
            return new idVec2(this.x + a.x, this.y + a.y);
        }

//public	idVec2			operator-( const idVec2 &a ) const;
        @Override
        public idVec2 oMinus(final idVec2 a) {
            return new idVec2(this.x - a.x, this.y - a.y);
        }

//public	idVec2 &		operator+=( const idVec2 &a );
        public idVec2 oPluSet(final idVec2 a) {
            this.x += a.x;
            this.y += a.y;
            return this;
        }

//public	idVec2 &		operator-=( const idVec2 &a );
        public idVec2 oMinSet(final idVec2 a) {
            this.x -= a.x;
            this.y -= a.y;
            return this;
        }
//public	idVec2 &		operator/=( const idVec2 &a );
//public	idVec2 &		operator/=( const float a );
//public	idVec2 &		operator*=( const float a );

        public idVec2 oMulSet(final float a) {
            this.x *= a;
            this.y *= a;
            return this;
        }

//public	friend idVec2	operator*( const float a, const idVec2 b );
        public idVec2 oSet(final idVec2 a) {
            this.x = a.x;
            this.y = a.y;
            return this;
        }

        public boolean Compare(final idVec2 a) {// exact compare, no epsilon
            return ((x == a.x) && (y == a.y));
        }

        public boolean Compare(final idVec2 a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(x - a.x) > epsilon) {
                return false;
            }

            if (idMath.Fabs(y - a.y) > epsilon) {
                return false;
            }

            return true;
        }
//public	bool			operator==(	const idVec2 &a ) const;						// exact compare, no epsilon
//public	bool			operator!=(	const idVec2 &a ) const;						// exact compare, no epsilon

        public float Length() {
            return (float) idMath.Sqrt(x * x + y * y);
        }

        public float LengthFast() {
            float sqrLength;

            sqrLength = x * x + y * y;
            return sqrLength * idMath.RSqrt(sqrLength);
        }

        public float LengthSqr() {
            return (x * x + y * y);
        }

        public float Normalize() {// returns length
            float sqrLength, invLength;

            sqrLength = x * x + y * y;
            invLength = idMath.InvSqrt(sqrLength);
            x *= invLength;
            y *= invLength;
            return invLength * sqrLength;
        }

        public float NormalizeFast() {// returns length
            float lengthSqr, invLength;

            lengthSqr = x * x + y * y;
            invLength = idMath.RSqrt(lengthSqr);
            x *= invLength;
            y *= invLength;
            return invLength * lengthSqr;
        }

        public idVec2 Truncate(float length) {// cap length
            float length2;
            float ilength;

            if (length == 0) {
                Zero();
            } else {
                length2 = LengthSqr();
                if (length2 > length * length) {
                    ilength = length * idMath.InvSqrt(length2);
                    x *= ilength;
                    y *= ilength;
                }
            }

            return this;
        }

        public void Clamp(final idVec2 min, final idVec2 max) {
            if (x < min.x) {
                x = min.x;
            } else if (x > max.x) {
                x = max.x;
            }
            if (y < min.y) {
                y = min.y;
            } else if (y > max.y) {
                y = max.y;
            }
        }

        public void Snap() {// snap to closest integer value
//            x = floor(x + 0.5f);
            x = (float) Math.floor(x + 0.5f);
            y = (float) Math.floor(y + 0.5f);
        }

        public void SnapInt() {// snap towards integer (floor)
            x = (float) (int) x;
            y = (float) (int) y;
        }

        public int GetDimension() {
            return 2;
        }

        public float[] ToFloatPtr() {
            return new float[]{x, y};
        }
//public	float *			ToFloatPtr( void );

        public String ToString() {
            return ToString(2);
        }

        public String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

        @Override
        public String toString() {
            return x + " " + y;
        }

        /*
         =============
         Lerp

         Linearly inperpolates one vector to another.
         =============
         */
        public void Lerp(final idVec2 v1, final idVec2 v2, final float l) {
            if (l <= 0.0f) {
                this.oSet(v1);//( * this) = v1;
            } else if (l >= 1.0f) {
                this.oSet(v2);//( * this) = v2;
            } else {
                this.oSet((v2.oMinus(v1)).oMultiply(l).oPlus(v1));//( * this) = v1 + l * (v2 - v1);
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

        public static idVec2[] generateArray(final int length) {
            return Stream.
                    generate(() -> new idVec2()).
                    limit(length).
                    toArray(idVec2[]::new);
        }
    }

    //===============================================================
    //
    //	idVec3 - 3D vector
    //
    //===============================================================
    public static class idVec3 implements idVec<idVec3>, SERiAL {

        public static final transient int SIZE = 3 * Float.SIZE;
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public float x;
        public float y;
        public float z;

        public idVec3() {
        }

        public idVec3(final float x, final float y, final float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public idVec3(idVec3 v) {
            this.x = v.x;
            this.y = v.y;
            this.z = v.z;
        }

        public idVec3(final float[] xyz, final int offset) {
            this.x = xyz[offset + 0];
            this.y = xyz[offset + 1];
            this.z = xyz[offset + 2];
        }

        public idVec3(final float[] xyz) {
            this(xyz, 0);
        }

        public void Set(final float x, final float y, final float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void Zero() {
            x = y = z = 0.0f;
        }

//public	float			operator[]( final  int index ) final ;
//public	float &			operator[]( final  int index );
//public	idVec3			operator-() final ;
        public idVec3 oNegative() {
            return new idVec3(-x, -y, -z);
        }

//public	idVec3 &		operator=( final  idVec3 &a );		// required because of a msvc 6 & 7 bug
        public idVec3 oSet(final idVec3 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
            return this;
        }

//public	float			operator*( final  idVec3 &a ) final ;
        @Override
        public float oMultiply(final idVec3 a) {
            return a.x * this.x + a.y * this.y + a.z * this.z;
        }

//public	idVec3			operator*( final  float a ) final ;
        public idVec3 oMultiply(final float a) {
            return new idVec3(this.x * a, this.y * a, this.z * a);
        }

        public idVec3 oMultiply(final idMat3 a) {
            return new idVec3(
                    a.getRow(0).oGet(0) * x + a.getRow(1).oGet(0) * y + a.getRow(2).oGet(0) * z,
                    a.getRow(0).oGet(1) * x + a.getRow(1).oGet(1) * y + a.getRow(2).oGet(1) * z,
                    a.getRow(0).oGet(2) * x + a.getRow(1).oGet(2) * y + a.getRow(2).oGet(2) * z);
        }

        public idVec3 oMultiply(final idRotation a) {
            return a.oMultiply(this);
        }

        public idVec3 oMultiply(final idMat4 a) {
            return a.oMultiply(this);
        }

//public	idVec3			operator/( final  float a ) final ;
        @Override
        public idVec3 oDivide(final float a) {
            float inva = 1.0f / a;
            return new idVec3(x * inva, y * inva, z * inva);
        }

//public	idVec3			operator+( final  idVec3 &a ) final ;F
        @Override
        public idVec3 oPlus(final idVec3 a) {
            return new idVec3(this.x + a.x, this.y + a.y, this.z + a.z);
        }

//public	idVec3			operator-( final  idVec3 &a ) final ;
        @Override
        public idVec3 oMinus(final idVec3 a) {
            return new idVec3(this.x - a.x, this.y - a.y, this.z - a.z);
        }

//public	idVec3 &		operator+=( final  idVec3 &a );
        public idVec3 oPluSet(final idVec3 a) {
            this.x += a.x;
            this.y += a.y;
            this.z += a.z;
            return this;
        }

//public	idVec3 &		operator-=( final  idVec3 &a );
        public idVec3 oMinSet(final idVec3 a) {
            this.x -= a.x;
            this.y -= a.y;
            this.z -= a.z;
            return this;
        }

//public	idVec3 &		operator/=( final  idVec3 &a );
        public idVec3 oDivSet(final float a) {
            this.x /= a;
            this.y /= a;
            this.z /= a;
            return this;
        }

//public	idVec3 &		operator*=( final  float a );
        public idVec3 oMulSet(final float a) {
            this.x *= a;
            this.y *= a;
            this.z *= a;
            return this;
        }

        public idVec3 oMulSet(final idMat3 mat) {
            this.oSet(idMat3.oMulSet(this, mat));
            return this;
        }

        public idVec3 oMulSet(final idRotation rotation) {
            this.oSet(rotation.oMultiply(this));
            return this;
        }

//public	friend idVec3	operator*( final  float a, final  idVec3 b );
        public static idVec3 oMultiply(final float a, final idVec3 b) {
            return new idVec3(b.x * a, b.y * a, b.z * a);
        }

        public boolean Compare(final idVec3 a) {// exact compare, no epsilon
            return ((x == a.x) && (y == a.y) && (z == a.z));
        }

        public boolean Compare(final idVec3 a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(x - a.x) > epsilon) {
                return false;
            }

            if (idMath.Fabs(y - a.y) > epsilon) {
                return false;
            }

            if (idMath.Fabs(z - a.z) > epsilon) {
                return false;
            }

            return true;
        }
//public	boolean			operator==(	final  idVec3 &a ) final ;						// exact compare, no epsilon
//public	boolean			operator!=(	final  idVec3 &a ) final ;						// exact compare, no epsilon

//private idVec3  multiply(float a){
//    return new idVec3( this.x * a, this.y * a, this.z * a );
//}
        public idVec3 oPlus(final float a) {
            this.x += a;
            this.y += a;
            this.z += a;
            return this;
        }

        private idVec3 oDivide(final idVec3 a, final float b) {
            final float invB = 1.0f / b;
            return new idVec3(a.x * b, a.y * b, a.z * b);
        }

        public boolean FixDegenerateNormal() {// fix degenerate axial cases
            if (x == 0.0f) {
                if (y == 0.0f) {
                    if (z > 0.0f) {
                        if (z != 1.0f) {
                            z = 1.0f;
                            return true;
                        }
                    } else {
                        if (z != -1.0f) {
                            z = -1.0f;
                            return true;
                        }
                    }
                    return false;
                } else if (z == 0.0f) {
                    if (y > 0.0f) {
                        if (y != 1.0f) {
                            y = 1.0f;
                            return true;
                        }
                    } else {
                        if (y != -1.0f) {
                            y = -1.0f;
                            return true;
                        }
                    }
                    return false;
                }
            } else if (y == 0.0f) {
                if (z == 0.0f) {
                    if (x > 0.0f) {
                        if (x != 1.0f) {
                            x = 1.0f;
                            return true;
                        }
                    } else {
                        if (x != -1.0f) {
                            x = -1.0f;
                            return true;
                        }
                    }
                    return false;
                }
            }
            if (idMath.Fabs(x) == 1.0f) {
                if (y != 0.0f || z != 0.0f) {
                    y = z = 0.0f;
                    return true;
                }
                return false;
            } else if (idMath.Fabs(y) == 1.0f) {
                if (x != 0.0f || z != 0.0f) {
                    x = z = 0.0f;
                    return true;
                }
                return false;
            } else if (idMath.Fabs(z) == 1.0f) {
                if (x != 0.0f || y != 0.0f) {
                    x = y = 0.0f;
                    return true;
                }
                return false;
            }
            return false;
        }

        public boolean FixDenormals() {// change tiny numbers to zero
            boolean denormal = false;
            if (Math.abs(x) < 1e-30f) {
                x = 0.0f;
                denormal = true;
            }
            if (Math.abs(y) < 1e-30f) {
                y = 0.0f;
                denormal = true;
            }
            if (Math.abs(z) < 1e-30f) {
                z = 0.0f;
                denormal = true;
            }
            return denormal;
        }

        public idVec3 Cross(final idVec3 a) {
            return new idVec3(y * a.z - z * a.y, z * a.x - x * a.z, x * a.y - y * a.x);
        }

        public idVec3 Cross(final idVec3 a, final idVec3 b) {
            x = a.y * b.z - a.z * b.y;
            y = a.z * b.x - a.x * b.z;
            z = a.x * b.y - a.y * b.x;

            return this;
        }

        public float Length() {
            return idMath.Sqrt(x * x + y * y + z * z);
        }

        public float LengthSqr() {
            return (x * x + y * y + z * z);
        }

        public float LengthFast() {
            float sqrLength;

            sqrLength = x * x + y * y + z * z;
            return sqrLength * idMath.RSqrt(sqrLength);
        }

        public float Normalize() {// returns length
            float sqrLength, invLength;

            sqrLength = x * x + y * y + z * z;
            invLength = idMath.InvSqrt(sqrLength);
            x *= invLength;
            y *= invLength;
            z *= invLength;
            return invLength * sqrLength;
        }

        public float NormalizeFast() {// returns length
            float sqrLength, invLength;

            sqrLength = x * x + y * y + z * z;
            invLength = idMath.RSqrt(sqrLength);
            x *= invLength;
            y *= invLength;
            z *= invLength;
            return invLength * sqrLength;
        }

        public idVec3 Truncate(float length) {// cap length
            float length2;
            float ilength;

            if (length != 0.0f) {
                Zero();
            } else {
                length2 = LengthSqr();
                if (length2 > length * length) {
                    ilength = length * idMath.InvSqrt(length2);
                    x *= ilength;
                    y *= ilength;
                    z *= ilength;
                }
            }

            return this;
        }

        public void Clamp(final idVec3 min, final idVec3 max) {
            if (x < min.x) {
                x = min.x;
            } else if (x > max.x) {
                x = max.x;
            }
            if (y < min.y) {
                y = min.y;
            } else if (y > max.y) {
                y = max.y;
            }
            if (z < min.z) {
                z = min.z;
            } else if (z > max.z) {
                z = max.z;
            }
        }

        public void Snap() {// snap to closest integer value
            x = (float) Math.floor(x + 0.5f);
            y = (float) Math.floor(y + 0.5f);
            z = (float) Math.floor(z + 0.5f);
        }

        public void SnapInt() {// snap towards integer (floor)
            x = (int) x;
            y = (int) y;
            z = (int) z;
        }

        public int GetDimension() {
            return 3;
        }

        public float ToYaw() {
            float yaw;

            if ((y == 0.0f) && (x == 0.0f)) {
                yaw = 0.0f;
            } else {
                yaw = RAD2DEG(Math.atan2(y, x));
                if (yaw < 0.0f) {
                    yaw += 360.0f;
                }
            }

            return yaw;
        }

        public float ToPitch() {
            float forward;
            float pitch;

            if ((x == 0.0f) && (y == 0.0f)) {
                if (z > 0.0f) {
                    pitch = 90.0f;
                } else {
                    pitch = 270.0f;
                }
            } else {
                forward = (float) idMath.Sqrt(x * x + y * y);
                pitch = RAD2DEG(Math.atan2(z, forward));
                if (pitch < 0.0f) {
                    pitch += 360.0f;
                }
            }

            return pitch;
        }

        public idAngles ToAngles() {
            float forward;
            float yaw;
            float pitch;

            if ((x == 0.0f) && (y == 0.0f)) {
                yaw = 0.0f;
                if (z > 0.0f) {
                    pitch = 90.0f;
                } else {
                    pitch = 270.0f;
                }
            } else {
                yaw = RAD2DEG(Math.atan2(y, x));
                if (yaw < 0.0f) {
                    yaw += 360.0f;
                }

                forward = idMath.Sqrt(x * x + y * y);
                pitch = RAD2DEG(Math.atan2(z, forward));
                if (pitch < 0.0f) {
                    pitch += 360.0f;
                }
            }

            return new idAngles(-pitch, yaw, 0.0f);
        }

        public idPolar3 ToPolar() {
            float forward;
            float yaw;
            float pitch;

            if ((x == 0.0f) && (y == 0.0f)) {
                yaw = 0.0f;
                if (z > 0.0f) {
                    pitch = 90.0f;
                } else {
                    pitch = 270.0f;
                }
            } else {
                yaw = RAD2DEG(Math.atan2(y, x));
                if (yaw < 0.0f) {
                    yaw += 360.0f;
                }

                forward = idMath.Sqrt(x * x + y * y);
                pitch = RAD2DEG(Math.atan2(z, forward));
                if (pitch < 0.0f) {
                    pitch += 360.0f;
                }
            }
            return new idPolar3(idMath.Sqrt(x * x + y * y + z * z), yaw, -pitch);
        }

        // vector should be normalized
        public idMat3 ToMat3() {
            idMat3 mat = new idMat3();
            float d;

            mat.setRow(0, x, y, z);
            d = x * x + y * y;
            if (d == 0) {
//		mat[1][0] = 1.0f;
//		mat[1][1] = 0.0f;
//		mat[1][2] = 0.0f;
                mat.setRow(1, 1.0f, 0.0f, 0.0f);//TODO:test, and rename, column??
            } else {
                d = idMath.InvSqrt(d);
//		mat[1][0] = -y * d;
//		mat[1][1] = x * d;
//		mat[1][2] = 0.0f;
                mat.setRow(1, -y * d, x * d, 0.0f);
            }
//        mat[2] = Cross( mat[1] );
            mat.setRow(2, Cross(mat.getRow(1)));

            return mat;
        }

        public final idVec2 ToVec2() {
//	return *reinterpret_cast<const idVec2 *>(this);
            return new idVec2(x, y);
        }
//public	idVec2 &		ToVec2( void );

        public float[] ToFloatPtr() {
            return new float[]{x, y, z};
        }
//public	float *			ToFloatPtr( void );

        public final String ToString() {
            return ToString(2);
        }

        public final String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

        @Override
        public String toString() {
            return x + " " + y + " " + z;
        }

        // vector should be normalized
        public void NormalVectors(idVec3 left, idVec3 down) {
            float d;

            d = x * x + y * y;
            if (d == 0) {
                left.x = 1;
                left.y = 0;
                left.z = 0;
            } else {
                d = idMath.InvSqrt(d);
                left.x = -y * d;
                left.y = x * d;
                left.z = 0;
            }
            down.oSet(left.Cross(this));
        }

        public void OrthogonalBasis(idVec3 left, idVec3 up) {
            float l, s;

            if (idMath.Fabs(z) > 0.7f) {
                l = y * y + z * z;
                s = idMath.InvSqrt(l);
                up.x = 0;
                up.y = z * s;
                up.z = -y * s;
                left.x = l * s;
                left.y = -x * up.z;
                left.z = x * up.y;
            } else {
                l = x * x + y * y;
                s = idMath.InvSqrt(l);
                left.x = -y * s;
                left.y = x * s;
                left.z = 0;
                up.x = -z * left.y;
                up.y = z * left.x;
                up.z = l * s;
            }
        }

        public void ProjectOntoPlane(final idVec3 normal) {
            ProjectOntoPlane(normal, 1.0f);
        }

        public void ProjectOntoPlane(final idVec3 normal, final float overBounce) {
            float backoff;
            // x * a.x + y * a.y + z * a.z;
            backoff = this.oMultiply(normal);//	backoff = this.x * normal.x;//TODO:normal.x???

            if (overBounce != 1.0) {
                if (backoff < 0) {
                    backoff *= overBounce;
                } else {
                    backoff /= overBounce;
                }
            }

            this.oMinSet(oMultiply(backoff, normal));//	*this -= backoff * normal;
        }

        public boolean ProjectAlongPlane(final idVec3 normal, final float epsilon) {
            return ProjectAlongPlane(normal, epsilon, 1.0f);
        }

        public boolean ProjectAlongPlane(final idVec3 normal, final float epsilon, final float overBounce) {
            idVec3 cross;
            float len;

            cross = this.Cross(normal).Cross(this);
            // normalize so a fixed epsilon can be used
            cross.Normalize();
            len = normal.oMultiply(cross);
            if (idMath.Fabs(len) < epsilon) {
                return false;
            }
            cross.oMulSet(overBounce * normal.oMultiply(this) / len);//	cross *= overBounce * ( normal * (*this) ) / len;
            this.oMinus(cross);//(*this) -= cross;
            return true;
        }
        /*
         =============
         ProjectSelfOntoSphere

         Projects the z component onto a sphere.
         =============
         */

        public void ProjectSelfOntoSphere(final float radius) {
            float rsqr = radius * radius;
            float len = Length();
            if (len < rsqr * 0.5f) {
                z = (float) Math.sqrt(rsqr - len);
            } else {
                z = (float) (rsqr / (2.0f * Math.sqrt(len)));
            }
        }

        /*
         =============
         Lerp

         Linearly inperpolates one vector to another.
         =============
         */
        public void Lerp(final idVec3 v1, final idVec3 v2, final float l) {
            if (l <= 0.0f) {
                this.oSet(v1);//(*this) = v1;
            } else if (l >= 1.0f) {
                this.oSet(v2);//(*this) = v2;
            } else {
                this.oSet((v2.oMinus(v1)).oMultiply(l).oPlus(v1));//(*this) = v1 + l * ( v2 - v1 );
            }
        }

        /*
         =============
         SLerp

         Spherical linear interpolation from v1 to v2.
         Vectors are expected to be normalized.
         =============
         */
        private static final double LERP_DELTA = 1e-6;

        public void SLerp(final idVec3 v1, final idVec3 v2, final float t) {
            float omega, cosom, sinom, scale0, scale1;

            if (t <= 0.0f) {
//		(*this) = v1;
                oSet(v1);
                return;
            } else if (t >= 1.0f) {
//		(*this) = v2;
                oSet(v2);
                return;
            }

            cosom = v1.oMultiply(v2);
            if ((1.0f - cosom) > LERP_DELTA) {
                omega = (float) Math.acos(cosom);
                sinom = (float) Math.sin(omega);
                scale0 = (float) (Math.sin((1.0f - t) * omega) / sinom);
                scale1 = (float) (Math.sin(t * omega) / sinom);
            } else {
                scale0 = 1.0f - t;
                scale1 = t;
            }

//	(*this) = ( v1 * scale0 + v2 * scale1 );
            oSet(v1.oMultiply(scale0).oPlus(v2.oMultiply(scale1)));
        }

        @Override
        public float oGet(final int i) {//TODO:rename you lazy ass
            if (i == 1) {
                return y;
            } else if (i == 2) {
                return z;
            }
            return x;
        }

        @Override
        public float oSet(final int i, final float value) {
            if (i == 1) {
                y = value;
            } else if (i == 2) {
                z = value;
            } else {
                x = value;
            }
            return value;
        }

        public void oPluSet(final int i, final float value) {
            if (i == 1) {
                y += value;
            } else if (i == 2) {
                z += value;
            } else {
                x += value;
            }
        }

        public void oMinSet(final int i, final float value) {
            if (i == 1) {
                y -= value;
            } else if (i == 2) {
                z -= value;
            } else {
                x -= value;
            }
        }

        public void oMulSet(final int i, final float value) {
            if (i == 1) {
                y *= value;
            } else if (i == 2) {
                z *= value;
            } else {
                x *= value;
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
            ByteBuffer buffer = ByteBuffer.allocate(BYTES);

            buffer.putFloat(x).putFloat(y).putFloat(z).flip();

            return buffer;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = (int) (53 * hash + this.x);
            hash = (int) (53 * hash + this.y);
            hash = (int) (53 * hash + this.z);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null
                    || getClass() != obj.getClass()) {
                return false;
            }

            final idVec3 other = (idVec3) obj;

            return (this.x == other.x) && (this.y == other.y) && (this.z == other.z);
        }

        public static idVec3[] generateArray(final int length) {
            return Stream.
                    generate(() -> new idVec3()).
                    limit(length).
                    toArray(idVec3[]::new);
        }
    }

    //===============================================================
    //
    //	idVec4 - 4D vector
    //
    //===============================================================
    public static class idVec4 implements idVec<idVec4>, SERiAL {

        public static final transient int SIZE = 4 * Float.SIZE;
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public float x;
        public float y;
        public float z;
        public float w;

        public idVec4() {
        }

        public idVec4(final idVec4 v) {
            this.x = v.x;
            this.y = v.y;
            this.z = v.z;
            this.w = v.w;
        }

        public idVec4(final float x, final float y, final float z, final float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public void Set(final float x, final float y, final float z, final float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public void Zero() {
            x = y = z = w = 0.0f;
        }

//public	float			operator[]( final  int index ) final ;
//public	float &			operator[]( final  int index );
//public	idVec4			operator-() final ;
        @Override
        public float oMultiply(final idVec4 a) {
            return x * a.x + y * a.y + z * a.z + w * a.w;
        }

        public idVec4 oMultiply(final float a) {
            return new idVec4(this.x * a, this.y * a, this.z * a, this.w * a);
        }

        public idVec4 oMultiply(final Float a) {//for our reflection method
            return oMultiply(a.floatValue());
        }
//public	idVec4			operator/( final  float a ) final ;

        @Override
        public idVec4 oPlus(final idVec4 a) {
            return new idVec4(this.x + a.x, this.y + a.y, this.z + a.z, this.w + a.w);
        }

        @Override
        public idVec4 oMinus(final idVec4 a) {
            return new idVec4(this.x - a.x, this.y - a.y, this.z - a.z, this.w - a.w);
        }

        public idVec4 oNegative() {
            return new idVec4(-this.x, -this.y, -this.z, -this.w);
        }

//public	idVec4			operator-( final  idVec4 &a ) final ;
        public void oMinSet(final int i, final float value) {//TODO:rename you lazy ass          
            switch (i) {
                default:
                    x -= value;
                    break;
                case 1:
                    y -= value;
                    break;
                case 2:
                    z -= value;
                    break;
                case 3:
                    w -= value;
                    break;
            }
        }

        public void oMulSet(final int i, final float value) {//TODO:rename you lazy ass          
            switch (i) {
                default:
                    x *= value;
                    break;
                case 1:
                    y *= value;
                    break;
                case 2:
                    z *= value;
                    break;
                case 3:
                    w *= value;
                    break;
            }
        }
//public	idVec4 &		operator+=( final  idVec4 &a );

        public idVec4 oPluSet(final idVec4 a) {
            this.x += a.x;
            this.y += a.y;
            this.z += a.z;
            this.w += a.w;
            return this;
        }

//public	idVec4 &		operator-=( final  idVec4 &a );
//public	idVec4 &		operator/=( final  idVec4 &a );
//public	idVec4 &		operator/=( final  float a );
//public	idVec4 &		operator*=( final  float a );
//
//public	friend idVec4	operator*( final  float a, final  idVec4 b );
        public boolean Compare(final idVec4 a) {// exact compare, no epsilon
            return ((x == a.x) && (y == a.y) && (z == a.z) && w == a.w);
        }

        public boolean Compare(final idVec4 a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(x - a.x) > epsilon) {
                return false;
            }

            if (idMath.Fabs(y - a.y) > epsilon) {
                return false;
            }

            if (idMath.Fabs(z - a.z) > epsilon) {
                return false;
            }

            if (idMath.Fabs(w - a.w) > epsilon) {
                return false;
            }

            return true;
        }
//public	bool			operator==(	final  idVec4 &a ) final ;						// exact compare, no epsilon
//public	bool			operator!=(	final  idVec4 &a ) final ;						// exact compare, no epsilon

        public float Length() {
            return idMath.Sqrt(x * x + y * y + z * z + w * w);
        }

        public float LengthSqr() {
            return (x * x + y * y + z * z + w * w);
        }

        public float Normalize() {// returns length
            float sqrLength, invLength;

            sqrLength = x * x + y * y + z * z + w * w;
            invLength = idMath.InvSqrt(sqrLength);
            x *= invLength;
            y *= invLength;
            z *= invLength;
            w *= invLength;
            return invLength * sqrLength;
        }

        public float NormalizeFast() {// returns length
            float sqrLength, invLength;

            sqrLength = x * x + y * y + z * z + w * w;
            invLength = idMath.RSqrt(sqrLength);
            x *= invLength;
            y *= invLength;
            z *= invLength;
            w *= invLength;
            return invLength * sqrLength;
        }

        public final int GetDimension() {
            return 4;
        }

        @Deprecated
        public final idVec2 ToVec2() {
//	return *reinterpret_cast<const idVec2 *>(this);
            return new idVec2(x, y);
        }
//public	idVec2 &		ToVec2( void );

        @Deprecated
        public final idVec3 ToVec3() {
//	return *reinterpret_cast<const idVec3 *>(this);
            return new idVec3(x, y, z);
        }
//public	idVec3 &		ToVec3( void );

        public final float[] ToFloatPtr() {
            return new float[]{x, y, z, w};//TODO:put shit in array si we can referef it
        }
//public	float *			ToFloatPtr( void );

        public final String ToString() {
            return ToString(2);
        }

        public final String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

        @Override
        public String toString() {
            return x + " " + y + " " + z + " " + w;
        }

        /*
         =============
         Lerp

         Linearly inperpolates one vector to another.
         =============
         */
        public void Lerp(final idVec4 v1, final idVec4 v2, final float l) {
            if (l <= 0.0f) {
//		(*this) = v1;
                x = v1.x;
                y = v1.y;
                z = v1.z;
                w = v1.w;
            } else if (l >= 1.0f) {
//		(*this) = v2;
                x = v2.x;
                y = v2.y;
                z = v2.z;
                w = v2.w;
            } else {
//		(*this) = v1 + l * ( v2 - v1 );
                w = v1.w + l * (v2.w - v1.w);
                x = v1.x + l * (v2.x - v1.x);
                y = v1.y + l * (v2.y - v1.y);
                z = v1.z + l * (v2.z - v1.z);
            }
        }

        public idVec4 oSet(final idVec4 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
            this.w = a.w;
            return this;
        }

        @Override
        public float oGet(final int i) {//TODO:rename you lazy ass          
            switch (i) {
                default:
                    return x;
                case 1:
                    return y;
                case 2:
                    return z;
                case 3:
                    return w;
            }
        }

        @Override
        public float oSet(final int i, final float value) {//TODO:rename you lazy ass          
            switch (i) {
                default:
                    return x = value;
                case 1:
                    return y = value;
                case 2:
                    return z = value;
                case 3:
                    return w = value;
            }
        }

        public float oPluSet(final int i, final float value) {
            switch (i) {
                default:
                    return x += value;
                case 1:
                    return y += value;
                case 2:
                    return z += value;
                case 3:
                    return w += value;
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

        @Override
        public idVec4 oDivide(float a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public static idVec4[] generateArray(final int length) {
            return Stream.
                    generate(() -> new idVec4()).
                    limit(length).
                    toArray(idVec4[]::new);
        }
    }

    //===============================================================
    //
    //	idVec5 - 5D vector
    //
    //===============================================================
    public static class idVec5 implements idVec<idVec5>, SERiAL {

        public static final transient int SIZE = 5 * Float.SIZE;
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public float x;
        public float y;
        public float z;
        public float s;
        public float t;

        public idVec5() {
        }

        public idVec5(final idVec3 xyz, final idVec2 st) {
            x = xyz.x;
            y = xyz.y;
            z = xyz.z;
//	s = st[0];
            s = st.x;
//	t = st[1];
            t = st.y;
        }

        public idVec5(final float x, final float y, final float z, final float s, final float t) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.s = s;
            this.t = t;
        }

        public idVec5(final idVec3 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
        }

        //copy constructor
        public idVec5(final idVec5 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
            this.s = a.s;
            this.t = a.t;
        }

//public	float			operator[]( int index ) final ;
        @Override
        public float oGet(final int i) {//TODO:rename you lazy sod          
            switch (i) {
                default:
                    return x;
                case 1:
                    return y;
                case 2:
                    return z;
                case 3:
                    return s;
                case 4:
                    return t;
            }
        }

        @Override
        public float oSet(final int i, final float value) {
            switch (i) {
                default:
                    return x = value;
                case 1:
                    return y = value;
                case 2:
                    return z = value;
                case 3:
                    return s = value;
                case 4:
                    return t = value;
            }
        }

        public idVec5 oSet(final idVec5 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
            this.s = a.s;
            this.t = a.t;
            return this;
        }

        public idVec5 oSet(final idVec3 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
            return this;
        }
//public	float &			operator[]( int index );
//public	idVec5 &		operator=( final  idVec3 &a );

        public final int GetDimension() {
            return 5;
        }

        public final idVec3 ToVec3() {
//	return *reinterpret_cast<const idVec3 *>(this);
            return new idVec3(x, y, z);
        }

//public	idVec3 &		ToVec3( void );
        public final float[] ToFloatPtr() {
            return new float[]{x, y, z};//TODO:array!?
        }
//public	float *			ToFloatPtr( void );

        public final String ToString() {
            return ToString(2);
        }

        public final String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

        public void Lerp(final idVec5 v1, final idVec5 v2, final float l) {
            if (l <= 0.0f) {
                this.oSet(v1);//(*this) = v1;
            } else if (l >= 1.0f) {
                this.oSet(v2);//(*this) = v2;
            } else {
                x = v1.x + l * (v2.x - v1.x);
                y = v1.y + l * (v2.y - v1.y);
                z = v1.z + l * (v2.z - v1.z);
                s = v1.s + l * (v2.s - v1.s);
                t = v1.t + l * (v2.t - v1.t);
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

        @Override
        public idVec5 oPlus(idVec5 a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public idVec5 oMinus(idVec5 a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public float oMultiply(idVec5 a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public idVec5 oDivide(float a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public static idVec5[] generateArray(final int length) {
            return Stream.
                    generate(() -> new idVec5()).
                    limit(length).
                    toArray(idVec5[]::new);
        }
    }

    //===============================================================
    //
    //	idVec6 - 6D vector
    //
    //===============================================================
    public static class idVec6 implements idVec<idVec6>, SERiAL {

        public static final transient int SIZE = 6 * Float.SIZE;
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public float p[] = new float[6];
        //
        //

        public idVec6() {
        }

        public idVec6(final float[] a) {
//	memcpy( p, a, 6 * sizeof( float ) );
            System.arraycopy(a, 0, p, 0, 6);
        }

        public idVec6(final float a1, final float a2, final float a3, final float a4, final float a5, final float a6) {
            p[0] = a1;
            p[1] = a2;
            p[2] = a3;
            p[3] = a4;
            p[4] = a5;
            p[5] = a6;
        }

        public void Set(final float a1, final float a2, final float a3, final float a4, final float a5, final float a6) {
            p[0] = a1;
            p[1] = a2;
            p[2] = a3;
            p[3] = a4;
            p[4] = a5;
            p[5] = a6;
        }

        public void Zero() {
            p[0] = p[1] = p[2] = p[3] = p[4] = p[5] = 0.0f;
        }

//public 	float			operator[]( final  int index ) final ;
//public 	float &			operator[]( final  int index );
        public idVec6 oNegative() {
            return new idVec6(-p[0], -p[1], -p[2], -p[3], -p[4], -p[5]);
        }

        public idVec6 oMultiply(final float a) {
            return new idVec6(p[0] * a, p[1] * a, p[2] * a, p[3] * a, p[4] * a, p[5] * a);
        }

//public 	idVec6			operator/( final  float a ) final ;
        @Override
        public float oMultiply(final idVec6 a) {
            return p[0] * a.p[0] + p[1] * a.p[1] + p[2] * a.p[2] + p[3] * a.p[3] + p[4] * a.p[4] + p[5] * a.p[5];
        }
//public 	idVec6			operator-( final  idVec6 &a ) final ;

        @Override
        public idVec6 oPlus(final idVec6 a) {
            return new idVec6(p[0] + a.p[0], p[1] + a.p[1], p[2] + a.p[2], p[3] + a.p[3], p[4] + a.p[4], p[5] + a.p[5]);
        }
//public 	idVec6 &		operator*=( final  float a );
//public 	idVec6 &		operator/=( final  float a );

        public idVec6 oPluSet(final idVec6 a) {
            this.p[0] += a.p[0];
            this.p[1] += a.p[1];
            this.p[2] += a.p[2];
            this.p[3] += a.p[3];
            this.p[4] += a.p[4];
            this.p[5] += a.p[5];
            return this;
        }
//public 	idVec6 &		operator-=( final  idVec6 &a );
// 
//public 	friend idVec6	operator*( final  float a, final  idVec6 b );

        public boolean Compare(final idVec6 a) {// exact compare, no epsilon
            return ((p[0] == a.p[0]) && (p[1] == a.p[1]) && (p[2] == a.p[2])
                    && (p[3] == a.p[3]) && (p[4] == a.p[4]) && (p[5] == a.p[5]));
        }

        public boolean Compare(final idVec6 a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(p[0] - a.p[0]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[1] - a.p[1]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[2] - a.p[2]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[3] - a.p[3]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[4] - a.p[4]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[5] - a.p[5]) > epsilon) {
                return false;
            }

            return true;
        }
//public 	bool			operator==(	final  idVec6 &a ) final ;						// exact compare, no epsilon
//public 	bool			operator!=(	final  idVec6 &a ) final ;						// exact compare, no epsilon

        public float Length() {
            return idMath.Sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2] + p[3] * p[3] + p[4] * p[4] + p[5] * p[5]);
        }

        public float LengthSqr() {
            return (p[0] * p[0] + p[1] * p[1] + p[2] * p[2] + p[3] * p[3] + p[4] * p[4] + p[5] * p[5]);
        }

        public float Normalize() {// returns length
            float sqrLength, invLength;

            sqrLength = p[0] * p[0] + p[1] * p[1] + p[2] * p[2] + p[3] * p[3] + p[4] * p[4] + p[5] * p[5];
            invLength = idMath.InvSqrt(sqrLength);
            p[0] *= invLength;
            p[1] *= invLength;
            p[2] *= invLength;
            p[3] *= invLength;
            p[4] *= invLength;
            p[5] *= invLength;
            return invLength * sqrLength;
        }

        public float NormalizeFast() {// returns length
            float sqrLength, invLength;

            sqrLength = p[0] * p[0] + p[1] * p[1] + p[2] * p[2] + p[3] * p[3] + p[4] * p[4] + p[5] * p[5];
            invLength = idMath.RSqrt(sqrLength);
            p[0] *= invLength;
            p[1] *= invLength;
            p[2] *= invLength;
            p[3] *= invLength;
            p[4] *= invLength;
            p[5] *= invLength;
            return invLength * sqrLength;
        }

        public final int GetDimension() {
            return 6;
        }

        public final idVec3 SubVec3(int index) {
//	return *reinterpret_cast<const idVec3 *>(p + index * 3);
            return new idVec3(p[index *= 3], p[index + 1], p[index + 2]);
        }
//public 	idVec3 &		SubVec3( int index );

        public final float[] ToFloatPtr() {
            return p;
        }
//public 	float *			ToFloatPtr( void );

        public final String ToString() {
            return ToString(2);
        }

        public final String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

        public idVec6 oSet(final idVec6 a) {
            this.p[0] = a.p[0];
            this.p[1] = a.p[1];
            this.p[2] = a.p[2];
            this.p[3] = a.p[3];
            this.p[4] = a.p[4];
            this.p[5] = a.p[5];
            return this;
        }

        @Override
        public float oGet(final int index) {
            return p[index];
        }

        @Override
        public float oSet(final int index, final float value) {
            return p[index] = value;
        }
//
//        public void setP(final int index, final float value) {
//            p[index] = value;
//        }

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

        @Override
        public idVec6 oMinus(idVec6 a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public idVec6 oDivide(float a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    //===============================================================
    //
    //	idVecX - arbitrary sized vector
    //
    //  The vector lives on 16 byte aligned and 16 byte padded memory.
    //
    //	NOTE: due to the temporary memory pool idVecX cannot be used by multiple threads
    //
    //===============================================================
    public static class idVecX {
        // friend class idMatX;

        static final int VECX_MAX_TEMP = 1024;

        private int     size;                    // size of the vector
        private int     alloced;                 // if -1 p points to data set with SetData
        public  float[] p;                       // memory the vector is stored

        private static float[] temp    = new float[VECX_MAX_TEMP + 4];    // used to store intermediate results
        private static float[] tempPtr = temp;                            // pointer to 16 byte aligned temporary memory
        private static int tempIndex;                                     // index into memory pool, wraps around
        //
        //

        static int VECX_QUAD(int x) {
            return ((x + 3) & ~3);
        }

        @Deprecated
        void VECX_CLEAREND() {//TODO:is this function need for Java?
            int s = size;
////            while (s < ((s + 3) & ~3)) {
            while (s < p.length) {
                p[s++] = 0.0f;
            }
        }

        @Deprecated
        public static float[] VECX_ALLOCA(int n) {
//    ( (float *) _alloca16( VECX_QUAD( n ) ) )
//            float[] temp = new float[VECX_QUAD(n)];
//            Arrays.fill(temp, -107374176);
//
//            return temp;

            return new float[VECX_QUAD(n)];
        }

        boolean VECX_SIMD;

        public idVecX() {
            size = alloced = 0;
            p = null;
        }

        public idVecX(int length) {
            size = alloced = 0;
            p = null;
            SetSize(length);
        }

        public idVecX(int length, float[] data) {
            size = alloced = 0;
            p = null;
            SetData(length, data);
        }
//public					~idVecX( void );

//public	float			operator[]( const int index ) const;
        public float oGet(final int index) {
            return p[index];
        }

        public float oSet(final int index, final float value) {
            return p[index] = value;
        }

//public	float &			operator[]( const int index );
//public	idVecX			operator-() const;
        public idVecX oNegative() {
            int i;
            idVecX m = new idVecX();

            m.SetTempSize(size);
            for (i = 0; i < size; i++) {
                m.p[i] = -p[i];
            }
            return m;
        }

//public	idVecX &		operator=( const idVecX &a );
        public idVecX oSet(final idVecX a) {
            SetSize(a.size);
//#ifdef VECX_SIMD
//	SIMDProcessor->Copy16( p, a.p, a.size );
//#else
//	memcpy( p, a.p, a.size * sizeof( float ) );
            System.arraycopy(a.p, 0, this.p, 0, a.p.length);//TODO:use a.size??
//#endif
            idVecX.tempIndex = 0;
            return this;
        }

        public idVecX oMultiply(final float a) {
            idVecX m = new idVecX();

            m.SetTempSize(size);
            if (VECX_SIMD) {
                SIMDProcessor.Mul16(m.p, p, a, size);
            } else {
                int i;
                for (i = 0; i < size; i++) {
                    m.p[i] = p[i] * a;
                }
            }
            return m;
        }
//public	idVecX			operator/( const float a ) const;
//public	float			operator*( const idVecX &a ) const;

        public float oMultiply(final idVecX a) {
            int i;
            float sum = 0.0f;

            assert (size == a.size);
            for (i = 0; i < size; i++) {
                sum += p[i] * a.p[i];
            }
            return sum;
        }
//public	idVecX			operator-( const idVecX &a ) const;
//public	idVecX			operator+( const idVecX &a ) const;

        public idVecX oPlus(final idVecX a) {
            idVecX m = new idVecX();

            assert (size == a.size);
            m.SetTempSize(size);
//#ifdef VECX_SIMD
//	SIMDProcessor->Add16( m.p, p, a.p, size );
//#else
            int i;
            for (i = 0; i < size; i++) {
                m.p[i] = p[i] + a.p[i];
            }
//#endif
            return m;
        }
//public	idVecX &		operator*=( const float a );

        public idVecX oMulSet(final float a) {
            idVecX m = new idVecX();

            m.SetTempSize(size);
//#ifdef VECX_SIMD
//	SIMDProcessor->Mul16( m.p, p, a, size );
//#else
            int i;
            for (i = 0; i < size; i++) {
                m.p[i] = p[i] * a;
            }
//#endif
            return m;
        }
//public	idVecX &		operator/=( const float a );
//public	idVecX &		operator+=( const idVecX &a );
//public	idVecX &		operator-=( const idVecX &a );
//public	friend idVecX	operator*( const float a, const idVecX b );

        public boolean Compare(final idVecX a) {// exact compare, no epsilon
            int i;

            assert (size == a.size);
            for (i = 0; i < size; i++) {
                if (p[i] != a.p[i]) {
                    return false;
                }
            }
            return true;
        }

        public boolean Compare(final idVecX a, final float epsilon) {// compare with epsilon
            int i;

            assert (size == a.size);
            for (i = 0; i < size; i++) {
                if (idMath.Fabs(p[i] - a.p[i]) > epsilon) {
                    return false;
                }
            }
            return true;
        }
//public	bool			operator==(	const idVecX &a ) const;						// exact compare, no epsilon
//public	bool			operator!=(	const idVecX &a ) const;						// exact compare, no epsilon

        public void SetSize(int newSize) {
            int alloc = (newSize + 3) & ~3;
            if (alloc > alloced && alloced != -1) {
                if (p != null) {
//			Mem_Free16( p );
                    p = null;
                }
//		p = (float *) Mem_Alloc16( alloc * sizeof( float ) );
                p = new float[alloc];
                alloced = alloc;
            }
            size = newSize;
            VECX_CLEAREND();
        }

        public void ChangeSize(int newSize) {
            ChangeSize(newSize, false);
        }

        public void ChangeSize(int newSize, boolean makeZero) {
            int alloc = (newSize + 3) & ~3;
            if (alloc > alloced && alloced != -1) {
                float[] oldVec = p;
//		p = (float *) Mem_Alloc16( alloc * sizeof( float ) );
                p = new float[alloc];
                alloced = alloc;
                if (oldVec != null) {
                    for (int i = 0; i < size; i++) {
                        p[i] = oldVec[i];
                    }
//			Mem_Free16( oldVec );//garbage collect me!
                }//TODO:ifelse
                if (makeZero) {
                    // zero any new elements
                    for (int i = size; i < newSize; i++) {
                        p[i] = 0.0f;
                    }
                }
            }
            size = newSize;
            VECX_CLEAREND();
        }

        public int GetSize() {
            return size;
        }

        public void SetData(int length, float[] data) {
            if (p != null && (p[0] < idVecX.tempPtr[0] || p[0] >= idVecX.tempPtr[0] + VECX_MAX_TEMP) && alloced != -1) {
//		Mem_Free16( p );
                p = null;
            }
//	assert( ( ( (int) data ) & 15 ) == 0 ); // data must be 16 byte aligned
            assert (data.length == 16);//TODO:??
            p = data;
            size = length;
            alloced = -1;
            VECX_CLEAREND();
        }

        public void Zero() {
//#ifdef VECX_SIMD
//	SIMDProcessor.Zero16( p, size );
//#else
//	memset( p, 0, size * sizeof( float ) );
//#endif
            Arrays.fill(p, 0, size, 0);
        }

        public void Zero(int length) {
            SetSize(length);
//#ifdef VECX_SIMD
//	SIMDProcessor.Zero16( p, length );
//#else
//	memset( p, 0, size * sizeof( float ) );
//#endif
            Arrays.fill(p, 0, size, 0);
        }

        public void Random(int seed) {
            Random(seed, 0.0f, 1.0f);
        }

        public void Random(int seed, float l) {
            Random(seed, l, 1.0f);

        }

        public void Random(int seed, float l, float u) {
            int i;
            float c;
            idRandom rnd = new idRandom(seed);

            c = u - l;
            for (i = 0; i < size; i++) {
                p[i] = l + rnd.RandomFloat() * c;
            }
        }

        public void Random(int length, int seed) {
            Random(length, seed, 0.0f, 1.0f);
        }

        public void Random(int length, int seed, float l) {
            Random(length, seed, l, 1.0f);
        }

        public void Random(int length, int seed, float l, float u) {
            int i;
            float c;
            idRandom rnd = new idRandom(seed);

            SetSize(length);
            c = u - l;
            for (i = 0; i < size; i++) {
                if (idMatX.DISABLE_RANDOM_TEST) {//for testing.
                    p[i] = i;
                } else {
                    p[i] = l + rnd.RandomFloat() * c;
                }
            }
        }

        public void Negate() {
//#ifdef VECX_SIMD
//	SIMDProcessor.Negate16( p, size );
//#else
            int oGet;
            for (oGet = 0; oGet < size; oGet++) {
                p[oGet] = -p[oGet];
            }
//#endif
        }

        public void Clamp(float min, float max) {
            int i;
            for (i = 0; i < size; i++) {
                if (p[i] < min) {
                    p[i] = min;
                } else if (p[i] > max) {
                    p[i] = max;
                }
            }
        }

        public idVecX SwapElements(int e1, int e2) {
            float tmp;
            tmp = p[e1];
            p[e1] = p[e2];
            p[e2] = tmp;
            return this;
        }

        public float Length() {
            int i;
            float sum = 0.0f;

            for (i = 0; i < size; i++) {
                sum += p[i] * p[i];
            }
            return idMath.Sqrt(sum);
        }

        public float LengthSqr() {
            int i;
            float sum = 0.0f;

            for (i = 0; i < size; i++) {
                sum += p[i] * p[i];
            }
            return sum;
        }

        public idVecX Normalize() {
            int i;
            idVecX m = new idVecX();
            float invSqrt, sum = 0.0f;

            m.SetTempSize(size);
            for (i = 0; i < size; i++) {
                sum += p[i] * p[i];
            }
            invSqrt = idMath.InvSqrt(sum);
            for (i = 0; i < size; i++) {
                m.p[i] = p[i] * invSqrt;
            }
            return m;
        }

        public float NormalizeSelf() {
            float invSqrt, sum = 0.0f;
            int i;
            for (i = 0; i < size; i++) {
                sum += p[i] * p[i];
            }
            invSqrt = idMath.InvSqrt(sum);
            for (i = 0; i < size; i++) {
                p[i] *= invSqrt;
            }
            return invSqrt * sum;
        }

        public int GetDimension() {
            return size;
        }

        public idVec3 SubVec3(int index) {
            assert (index >= 0 && index * 3 + 3 <= size);
//	return *reinterpret_cast<idVec3 *>(p + index * 3);
            return new idVec3(p[index *= 3], p[index + 1], p[index + 2]);
        }
//public	idVec3 &		SubVec3( int index );

        public idVec6 SubVec6(int index) {
            assert (index >= 0 && index * 6 + 6 <= size);
//	return *reinterpret_cast<idVec6 *>(p + index * 6);
            return new idVec6(p[index *= 6], p[index + 1], p[index + 2], p[index + 3], p[index + 4], p[index + 5]);
        }
//public	idVec6 &		SubVec6( int index );

        public float[] ToFloatPtr() {
            return p;
        }
//public	float *			ToFloatPtr( void );

        public String ToString() {
            return ToString(2);
        }

        public String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

        public void SetTempSize(int newSize) {

            size = newSize;
            alloced = (newSize + 3) & ~3;
            assert (alloced < VECX_MAX_TEMP);
            if (idVecX.tempIndex + alloced > VECX_MAX_TEMP) {
                idVecX.tempIndex = 0;
            }
//            p = idVecX.tempPtr + idVecX.tempIndex;
//            for (int a = 0; a < idVecX.tempIndex; a++) {//TODO:trippple check
//                p[a] = idVecX.tempPtr[a + idVecX.tempIndex];
//            }
            p = new float[alloced];
            idVecX.tempIndex += alloced;
            VECX_CLEAREND();
        }
    }

    //===============================================================
    //
    //	idPolar3
    //
    //===============================================================
    static class idPolar3 {

        public float radius, theta, phi;

        public idPolar3() {
        }

        public idPolar3(final float radius, final float theta, final float phi) {
            assert (radius > 0);
            this.radius = radius;
            this.theta = theta;
            this.phi = phi;
        }

        public void Set(final float radius, final float theta, final float phi) {
            assert (radius > 0);
            this.radius = radius;
            this.theta = theta;
            this.phi = phi;
        }

//public	float			operator[]( const int index ) const;
//public	float &			operator[]( const int index );
//public	idPolar3		operator-() const;
//public	idPolar3 &		operator=( const idPolar3 &a );
        public idVec3 ToVec3() {
            float[] sp = new float[1], cp = new float[1], st = new float[1], ct = new float[1];
//            sp = cp = st = ct = 0.0f;
            idMath.SinCos(phi, sp, cp);
            idMath.SinCos(theta, st, ct);
            return new idVec3(cp[0] * radius * ct[0], cp[0] * radius * st[0], radius * sp[0]);
        }
    }

    /*
     ===============================================================================

     Old 3D vector macros, should no longer be used.

     ===============================================================================
     */
    public static double DotProduct(double[] a, double[] b) {
        return (a[0] * b[0] + a[1] * b[1] + a[2] * b[2]);
    }

    public static float DotProduct(float[] a, float[] b) {
        return (a[0] * b[0] + a[1] * b[1] + a[2] * b[2]);
    }

    public static float DotProduct(idVec3 a, idVec3 b) {
        return (a.oGet(0) * b.oGet(0)
                + a.oGet(1) * b.oGet(1)
                + a.oGet(2) * b.oGet(2));
    }

    public static float DotProduct(idVec3 a, idVec4 b) {
        return DotProduct(a, b.ToVec3());
    }

    public static float DotProduct(idVec3 a, idVec5 b) {
        return DotProduct(a, b.ToVec3());
    }

    public static float DotProduct(idPlane a, idPlane b) {
        return (a.oGet(0) * b.oGet(0)
                + a.oGet(1) * b.oGet(1)
                + a.oGet(2) * b.oGet(2));
    }

    public static double[] VectorSubtract(double[] a, double[] b, double[] c) {
        c[0] = a[0] - b[0];
        c[1] = a[1] - b[1];
        c[2] = a[2] - b[2];
        return c;
    }

    public static float[] VectorSubtract(float[] a, float[] b, float[] c) {
        c[0] = a[0] - b[0];
        c[1] = a[1] - b[1];
        c[2] = a[2] - b[2];
        return c;
    }

    public static float[] VectorSubtract(final idVec3 a, final idVec3 b, float[] c) {
        c[0] = a.oGet(0) - b.oGet(0);
        c[1] = a.oGet(1) - b.oGet(1);
        c[2] = a.oGet(2) - b.oGet(2);
        return c;
    }

    public static idVec3 VectorSubtract(final idVec3 a, final idVec3 b, idVec3 c) {
        c.oSet(0, a.oGet(0) - b.oGet(0));
        c.oSet(1, a.oGet(1) - b.oGet(1));
        c.oSet(2, a.oGet(2) - b.oGet(2));
        return c;
    }

    static void VectorAdd(double[] a, double[] b, Double[] c) {
        c[0] = a[0] + b[0];
        c[1] = a[1] + b[1];
        c[2] = a[2] + b[2];
    }

    static void VectorScale(double[] v, double s, Double[] o) {
        o[0] = v[0] * s;
        o[1] = v[1] * s;
        o[2] = v[2] * s;
    }

    public static void VectorMA(double[] v, double s, double[] b, Double[] o) {
        o[0] = v[0] + b[0] * s;
        o[1] = v[1] + b[1] * s;
        o[2] = v[2] + b[2] * s;
    }

    public static void VectorMA(final idVec3 v, final float s, final idVec3 b, idVec3 o) {
        o.oSet(0, v.oGet(0) + b.oGet(0) * s);
        o.oSet(1, v.oGet(1) + b.oGet(1) * s);
        o.oSet(2, v.oGet(2) + b.oGet(2) * s);
    }

    static void VectorCopy(double[] a, Double[] b) {
        b[0] = a[0];
        b[1] = a[1];
        b[2] = a[2];
    }

    public static void VectorCopy(idVec3 a, idVec3 b) {
        b.oSet(a);
    }

    public static void VectorCopy(idVec3 a, idVec5 b) {
        b.oSet(a);
    }

    public static void VectorCopy(idVec5 a, idVec3 b) {
        b.oSet(a.ToVec3());
    }
}
