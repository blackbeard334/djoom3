package neo.idlib.math;

import static neo.idlib.math.Simd.SIMDProcessor;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import neo.TempDump;
import neo.TempDump.SERiAL;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Matrix.idMat4;
import neo.idlib.math.Matrix.idMatX;
import neo.open.Nio;

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
        return ((float) a) * idMath.M_RAD2DEG;
    }

    public static float RAD2DEG(float a) {
        return a * idMath.M_RAD2DEG;
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

    public interface idVec<type extends Vector.idVec<?>> {
        //reflection was too slow. 
        //never thought I would say this, but thank God for type erasure.

        default float oGet(final int index) {
            throw new TempDump.TODO_Exception();
        }

        default type oSet(final type a) {
            throw new TempDump.TODO_Exception();
        }

        default float oSet(final int index, final float value) {
            throw new TempDump.TODO_Exception();
        }

        default type oPlus(final type a) {
            throw new TempDump.TODO_Exception();
        }

        default type oMinus(final type a) {
            throw new TempDump.TODO_Exception();
        }

        default float oMultiply(final type a) {
            throw new TempDump.TODO_Exception();
        }

        default type oMultiply(final float a) {
            throw new TempDump.TODO_Exception();
        }

        default type oDivide(final float a) {
            throw new TempDump.TODO_Exception();
        }

        default type oPluSet(final type a) {
            throw new TempDump.TODO_Exception();
        }

        default int GetDimension() {
            throw new TempDump.TODO_Exception();
        }

        default void Zero() {
            throw new TempDump.TODO_Exception();
        }

    }

    //===============================================================
    //
    //	idVec2 - 2D vector
    //
    //===============================================================
    public static class idVec2 implements idVec<idVec2>, SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static final transient int SIZE  = 2 * Float.SIZE;
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

        @Override
        public void Zero() {
            this.x = this.y = 0.0f;
        }

        //public	float			operator[]( int index ) const;
        @Override
        public float oSet(final int index, final float value) {
            if (index == 1) {
                return this.y = value;
            } else {
                return this.x = value;
            }
        }

        public float oPluSet(final int index, final float value) {
            if (index == 1) {
                return this.y += value;
            } else {
                return this.x += value;
            }
        }
//public	float &			operator[]( int index );

        @Override
        public float oGet(final int index) {//TODO:rename you lazy sod
            if (index == 1) {
                return this.y;
            }
            return this.x;
        }
//public	idVec2			operator-() const;

        //public	float			operator*( const idVec2 &a ) const;
        @Override
        public float oMultiply(final idVec2 a) {
            return (this.x * a.x) + (this.y * a.y);
        }

        //public	idVec2			operator*( const float a ) const;
        @Override
        public idVec2 oMultiply(final float a) {
            return new idVec2(this.x * a, this.y * a);
        }
//public	idVec2			operator/( const float a ) const;

        @Override
        public idVec2 oDivide(final float a) {
            final float inva = 1.0f / a;
            return new idVec2(this.x * inva, this.y * inva);
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
        @Override
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
        @Override
        public idVec2 oSet(final idVec2 a) {
            this.x = a.x;
            this.y = a.y;
            return this;
        }

        public boolean Compare(final idVec2 a) {// exact compare, no epsilon
            return ((this.x == a.x) && (this.y == a.y));
        }

        public boolean Compare(final idVec2 a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(this.x - a.x) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.y - a.y) > epsilon) {
                return false;
            }

            return true;
        }
//public	bool			operator==(	const idVec2 &a ) const;						// exact compare, no epsilon
//public	bool			operator!=(	const idVec2 &a ) const;						// exact compare, no epsilon

        public float Length() {
            return idMath.Sqrt((this.x * this.x) + (this.y * this.y));
        }

        public float LengthFast() {
            float sqrLength;

            sqrLength = (this.x * this.x) + (this.y * this.y);
            return sqrLength * idMath.RSqrt(sqrLength);
        }

        public float LengthSqr() {
            return ((this.x * this.x) + (this.y * this.y));
        }

        public float Normalize() {// returns length
            float sqrLength, invLength;

            sqrLength = (this.x * this.x) + (this.y * this.y);
            invLength = idMath.InvSqrt(sqrLength);
            this.x *= invLength;
            this.y *= invLength;
            return invLength * sqrLength;
        }

        public float NormalizeFast() {// returns length
            float lengthSqr, invLength;

            lengthSqr = (this.x * this.x) + (this.y * this.y);
            invLength = idMath.RSqrt(lengthSqr);
            this.x *= invLength;
            this.y *= invLength;
            return invLength * lengthSqr;
        }

        public idVec2 Truncate(float length) {// cap length
            float length2;
            float ilength;

            if (length == 0) {
                Zero();
            } else {
                length2 = LengthSqr();
                if (length2 > (length * length)) {
                    ilength = length * idMath.InvSqrt(length2);
                    this.x *= ilength;
                    this.y *= ilength;
                }
            }

            return this;
        }

        public void Clamp(final idVec2 min, final idVec2 max) {
            if (this.x < min.x) {
                this.x = min.x;
            } else if (this.x > max.x) {
                this.x = max.x;
            }
            if (this.y < min.y) {
                this.y = min.y;
            } else if (this.y > max.y) {
                this.y = max.y;
            }
        }

        public void Snap() {// snap to closest integer value
//            x = floor(x + 0.5f);
            this.x = (float) Math.floor(this.x + 0.5f);
            this.y = (float) Math.floor(this.y + 0.5f);
        }

        public void SnapInt() {// snap towards integer (floor)
            this.x = (int) this.x;
            this.y = (int) this.y;
        }

        @Override
        public int GetDimension() {
            return 2;
        }

        public FloatBuffer toFloatBuffer() {
            return (FloatBuffer) Nio.newFloatBuffer(2).put(this.x)
                	.put(this.y).flip();
        }

       public float[] ToFloatPtr() {
            return new float[]{this.x, this.y};
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
            return this.x + " " + this.y;
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
                    generate(idVec2::new).
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

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static final transient int SIZE  = 3 * Float.SIZE;
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

        @Override
        public void Zero() {
            this.x = this.y = this.z = 0.0f;
        }

        //public	float			operator[]( final  int index ) final ;
//public	float &			operator[]( final  int index );
//public	idVec3			operator-() final ;
        public idVec3 oNegative() {
            return new idVec3(-this.x, -this.y, -this.z);
        }

        //public	idVec3 &		operator=( final  idVec3 &a );		// required because of a msvc 6 & 7 bug
        @Override
        public idVec3 oSet(final idVec3 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
            return this;
        }

        public idVec3 oSet(final idVec2 a) {
            this.x = a.x;
            this.y = a.y;
            return this;
        }

        //public	float			operator*( final  idVec3 &a ) final ;
        @Override
        public float oMultiply(final idVec3 a) {
            return (a.x * this.x) + (a.y * this.y) + (a.z * this.z);
        }

        //public	idVec3			operator*( final  float a ) final ;
        @Override
        public idVec3 oMultiply(final float a) {
            return new idVec3(this.x * a, this.y * a, this.z * a);
        }

        public idVec3 oMultiply(final idMat3 a) {
            return new idVec3(
                    (a.getRow(0).oGet(0) * this.x) + (a.getRow(1).oGet(0) * this.y) + (a.getRow(2).oGet(0) * this.z),
                    (a.getRow(0).oGet(1) * this.x) + (a.getRow(1).oGet(1) * this.y) + (a.getRow(2).oGet(1) * this.z),
                    (a.getRow(0).oGet(2) * this.x) + (a.getRow(1).oGet(2) * this.y) + (a.getRow(2).oGet(2) * this.z));
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
            final float inva = 1.0f / a;
            return new idVec3(this.x * inva, this.y * inva, this.z * inva);
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
        @Override
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
            return ((this.x == a.x) && (this.y == a.y) && (this.z == a.z));
        }

        public boolean Compare(final idVec3 a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(this.x - a.x) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.y - a.y) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.z - a.z) > epsilon) {
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
            if (this.x == 0.0f) {
                if (this.y == 0.0f) {
                    if (this.z > 0.0f) {
                        if (this.z != 1.0f) {
                            this.z = 1.0f;
                            return true;
                        }
                    } else {
                        if (this.z != -1.0f) {
                            this.z = -1.0f;
                            return true;
                        }
                    }
                    return false;
                } else if (this.z == 0.0f) {
                    if (this.y > 0.0f) {
                        if (this.y != 1.0f) {
                            this.y = 1.0f;
                            return true;
                        }
                    } else {
                        if (this.y != -1.0f) {
                            this.y = -1.0f;
                            return true;
                        }
                    }
                    return false;
                }
            } else if (this.y == 0.0f) {
                if (this.z == 0.0f) {
                    if (this.x > 0.0f) {
                        if (this.x != 1.0f) {
                            this.x = 1.0f;
                            return true;
                        }
                    } else {
                        if (this.x != -1.0f) {
                            this.x = -1.0f;
                            return true;
                        }
                    }
                    return false;
                }
            }
            if (idMath.Fabs(this.x) == 1.0f) {
                if ((this.y != 0.0f) || (this.z != 0.0f)) {
                    this.y = this.z = 0.0f;
                    return true;
                }
                return false;
            } else if (idMath.Fabs(this.y) == 1.0f) {
                if ((this.x != 0.0f) || (this.z != 0.0f)) {
                    this.x = this.z = 0.0f;
                    return true;
                }
                return false;
            } else if (idMath.Fabs(this.z) == 1.0f) {
                if ((this.x != 0.0f) || (this.y != 0.0f)) {
                    this.x = this.y = 0.0f;
                    return true;
                }
                return false;
            }
            return false;
        }

        public boolean FixDenormals() {// change tiny numbers to zero
            boolean denormal = false;
            if (Math.abs(this.x) < 1e-30f) {
                this.x = 0.0f;
                denormal = true;
            }
            if (Math.abs(this.y) < 1e-30f) {
                this.y = 0.0f;
                denormal = true;
            }
            if (Math.abs(this.z) < 1e-30f) {
                this.z = 0.0f;
                denormal = true;
            }
            return denormal;
        }

        public idVec3 Cross(final idVec3 a) {
            return new idVec3((this.y * a.z) - (this.z * a.y), (this.z * a.x) - (this.x * a.z), (this.x * a.y) - (this.y * a.x));
        }

        public idVec3 Cross(final idVec3 a, final idVec3 b) {
            this.x = (a.y * b.z) - (a.z * b.y);
            this.y = (a.z * b.x) - (a.x * b.z);
            this.z = (a.x * b.y) - (a.y * b.x);

            return this;
        }

        public float Length() {
            return idMath.Sqrt((this.x * this.x) + (this.y * this.y) + (this.z * this.z));
        }

        public float LengthSqr() {
            return ((this.x * this.x) + (this.y * this.y) + (this.z * this.z));
        }

        public float LengthFast() {
            float sqrLength;

            sqrLength = (this.x * this.x) + (this.y * this.y) + (this.z * this.z);
            return sqrLength * idMath.RSqrt(sqrLength);
        }

        public float Normalize() {// returns length
            float sqrLength, invLength;

            sqrLength = (this.x * this.x) + (this.y * this.y) + (this.z * this.z);
            invLength = idMath.InvSqrt(sqrLength);
            this.x *= invLength;
            this.y *= invLength;
            this.z *= invLength;
            return invLength * sqrLength;
        }

        public float NormalizeFast() {// returns length
            float sqrLength, invLength;

            sqrLength = (this.x * this.x) + (this.y * this.y) + (this.z * this.z);
            invLength = idMath.RSqrt(sqrLength);
            this.x *= invLength;
            this.y *= invLength;
            this.z *= invLength;
            return invLength * sqrLength;
        }

        public idVec3 Truncate(float length) {// cap length
            float length2;
            float ilength;

            if (length != 0.0f) {
                Zero();
            } else {
                length2 = LengthSqr();
                if (length2 > (length * length)) {
                    ilength = length * idMath.InvSqrt(length2);
                    this.x *= ilength;
                    this.y *= ilength;
                    this.z *= ilength;
                }
            }

            return this;
        }

        public void Clamp(final idVec3 min, final idVec3 max) {
            if (this.x < min.x) {
                this.x = min.x;
            } else if (this.x > max.x) {
                this.x = max.x;
            }
            if (this.y < min.y) {
                this.y = min.y;
            } else if (this.y > max.y) {
                this.y = max.y;
            }
            if (this.z < min.z) {
                this.z = min.z;
            } else if (this.z > max.z) {
                this.z = max.z;
            }
        }

        public void Snap() {// snap to closest integer value
            this.x = (float) Math.floor(this.x + 0.5f);
            this.y = (float) Math.floor(this.y + 0.5f);
            this.z = (float) Math.floor(this.z + 0.5f);
        }

        public void SnapInt() {// snap towards integer (floor)
            this.x = (int) this.x;
            this.y = (int) this.y;
            this.z = (int) this.z;
        }

        @Override
        public int GetDimension() {
            return 3;
        }

        public float ToYaw() {
            float yaw;

            if ((this.y == 0.0f) && (this.x == 0.0f)) {
                yaw = 0.0f;
            } else {
                yaw = RAD2DEG(Math.atan2(this.y, this.x));
                if (yaw < 0.0f) {
                    yaw += 360.0f;
                }
            }

            return yaw;
        }

        public float ToPitch() {
            float forward;
            float pitch;

            if ((this.x == 0.0f) && (this.y == 0.0f)) {
                if (this.z > 0.0f) {
                    pitch = 90.0f;
                } else {
                    pitch = 270.0f;
                }
            } else {
                forward = idMath.Sqrt((this.x * this.x) + (this.y * this.y));
                pitch = RAD2DEG(Math.atan2(this.z, forward));
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

            if ((this.x == 0.0f) && (this.y == 0.0f)) {
                yaw = 0.0f;
                if (this.z > 0.0f) {
                    pitch = 90.0f;
                } else {
                    pitch = 270.0f;
                }
            } else {
                yaw = RAD2DEG(Math.atan2(this.y, this.x));
                if (yaw < 0.0f) {
                    yaw += 360.0f;
                }

                forward = idMath.Sqrt((this.x * this.x) + (this.y * this.y));
                pitch = RAD2DEG(Math.atan2(this.z, forward));
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

            if ((this.x == 0.0f) && (this.y == 0.0f)) {
                yaw = 0.0f;
                if (this.z > 0.0f) {
                    pitch = 90.0f;
                } else {
                    pitch = 270.0f;
                }
            } else {
                yaw = RAD2DEG(Math.atan2(this.y, this.x));
                if (yaw < 0.0f) {
                    yaw += 360.0f;
                }

                forward = idMath.Sqrt((this.x * this.x) + (this.y * this.y));
                pitch = RAD2DEG(Math.atan2(this.z, forward));
                if (pitch < 0.0f) {
                    pitch += 360.0f;
                }
            }
            return new idPolar3(idMath.Sqrt((this.x * this.x) + (this.y * this.y) + (this.z * this.z)), yaw, -pitch);
        }

        // vector should be normalized
        public idMat3 ToMat3() {
            final idMat3 mat = new idMat3();
            float d;

            mat.setRow(0, this.x, this.y, this.z);
            d = (this.x * this.x) + (this.y * this.y);
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
                mat.setRow(1, -this.y * d, this.x * d, 0.0f);
            }
//        mat[2] = Cross( mat[1] );
            mat.setRow(2, Cross(mat.getRow(1)));

            return mat;
        }

        public final idVec2 ToVec2() {
//	return *reinterpret_cast<const idVec2 *>(this);
            return new idVec2(this.x, this.y);
        }
//public	idVec2 &		ToVec2( void );

        public FloatBuffer toFloatBuffer() {
            return (FloatBuffer) Nio.newFloatBuffer(3).put(this.x)
                	.put(this.y)
                	.put(this.z).flip();
        }

        public float[] ToFloatPtr() {
            return new float[]{this.x, this.y, this.z};
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
            return this.x + " " + this.y + " " + this.z;
        }

        // vector should be normalized
        public void NormalVectors(idVec3 left, idVec3 down) {
            float d;

            d = (this.x * this.x) + (this.y * this.y);
            if (d == 0) {
                left.x = 1;
                left.y = 0;
                left.z = 0;
            } else {
                d = idMath.InvSqrt(d);
                left.x = -this.y * d;
                left.y = this.x * d;
                left.z = 0;
            }
            down.oSet(left.Cross(this));
        }

        public void OrthogonalBasis(idVec3 left, idVec3 up) {
            float l, s;

            if (idMath.Fabs(this.z) > 0.7f) {
                l = (this.y * this.y) + (this.z * this.z);
                s = idMath.InvSqrt(l);
                up.x = 0;
                up.y = this.z * s;
                up.z = -this.y * s;
                left.x = l * s;
                left.y = -this.x * up.z;
                left.z = this.x * up.y;
            } else {
                l = (this.x * this.x) + (this.y * this.y);
                s = idMath.InvSqrt(l);
                left.x = -this.y * s;
                left.y = this.x * s;
                left.z = 0;
                up.x = -this.z * left.y;
                up.y = this.z * left.x;
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
            cross.oMulSet((overBounce * normal.oMultiply(this)) / len);//	cross *= overBounce * ( normal * (*this) ) / len;
            this.oMinSet(cross);//(*this) -= cross;
            return true;
        }
        /*
         =============
         ProjectSelfOntoSphere

         Projects the z component onto a sphere.
         =============
         */

        public void ProjectSelfOntoSphere(final float radius) {
            final float rsqr = radius * radius;
            final float len = Length();
            if (len < (rsqr * 0.5f)) {
                this.z = (float) Math.sqrt(rsqr - len);
            } else {
                this.z = (float) (rsqr / (2.0f * Math.sqrt(len)));
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
                return this.y;
            } else if (i == 2) {
                return this.z;
            }
            return this.x;
        }

        @Override
        public float oSet(final int i, final float value) {
            if (i == 1) {
                this.y = value;
            } else if (i == 2) {
                this.z = value;
            } else {
                this.x = value;
            }
            return value;
        }

        public void oPluSet(final int i, final float value) {
            if (i == 1) {
                this.y += value;
            } else if (i == 2) {
                this.z += value;
            } else {
                this.x += value;
            }
        }

        public void oMinSet(final int i, final float value) {
            if (i == 1) {
                this.y -= value;
            } else if (i == 2) {
                this.z -= value;
            } else {
                this.x -= value;
            }
        }

        public void oMulSet(final int i, final float value) {
            if (i == 1) {
                this.y *= value;
            } else if (i == 2) {
                this.z *= value;
            } else {
                this.x *= value;
            }
        }

        @Override
        public ByteBuffer AllocBuffer() {
            return ByteBuffer.allocate(idVec3.BYTES);
        }

        @Override
        public void Read(ByteBuffer buffer) {
            this.x = buffer.getFloat();
            this.y = buffer.getFloat();
            this.z = buffer.getFloat();
        }

        @Override
        public ByteBuffer Write() {
            final ByteBuffer buffer = ByteBuffer.allocate(BYTES);

            buffer.putFloat(this.x).putFloat(this.y).putFloat(this.z).flip();

            return buffer;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = (int) ((53 * hash) + this.x);
            hash = (int) ((53 * hash) + this.y);
            hash = (int) ((53 * hash) + this.z);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if ((obj == null)
                    || (getClass() != obj.getClass())) {
                return false;
            }

            final idVec3 other = (idVec3) obj;

            return (this.x == other.x) && (this.y == other.y) && (this.z == other.z);
        }

        public static idVec3[] generateArray(final int length) {
            return Stream.
                    generate(idVec3::new).
                    limit(length).
                    toArray(idVec3[]::new);
        }

        public void ToVec2_oPluSet(idVec2 v) {
            this.x += v.x;
            this.y += v.y;
        }

        public void ToVec2_oMinSet(idVec2 v) {
            this.x -= v.x;
            this.y -= v.y;
        }

        public void ToVec2_oMulSet(float a) {
            this.x *= a;
            this.y *= a;
        }

        public void ToVec2_Normalize() {
            final idVec2 v = ToVec2();
            v.Normalize();
            this.oSet(v);
        }

        public void ToVec2_NormalizeFast() {
            final idVec2 v = ToVec2();
            v.NormalizeFast();
            this.oSet(v);
        }

        public static ByteBuffer toByteBuffer(idVec3[] vecs) {
            final ByteBuffer data = Nio.newByteBuffer(idVec3.BYTES * vecs.length);

            for (final idVec3 vec : vecs) {
                data.put((ByteBuffer) vec.Write().rewind());
            }

            return (ByteBuffer) data.flip();
        }
    }

    //===============================================================
    //
    //	idVec4 - 4D vector
    //
    //===============================================================
    public static class idVec4 implements idVec<idVec4>, SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static final transient int SIZE  = 4 * Float.SIZE;
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public float x;
        public float y;
        public float z;
        public float w;

        private static int DBG_counter = 0;
        private final  int DBG_count   = DBG_counter++;

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

        @Override
        public void Zero() {
            this.x = this.y = this.z = this.w = 0.0f;
        }

        //public	float			operator[]( final  int index ) final ;
//public	float &			operator[]( final  int index );
//public	idVec4			operator-() final ;
        @Override
        public float oMultiply(final idVec4 a) {
            return (this.x * a.x) + (this.y * a.y) + (this.z * a.z) + (this.w * a.w);
        }

        @Override
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
                    this.x -= value;
                    break;
                case 1:
                    this.y -= value;
                    break;
                case 2:
                    this.z -= value;
                    break;
                case 3:
                    this.w -= value;
                    break;
            }
        }

        public void oMulSet(final int i, final float value) {//TODO:rename you lazy ass          
            switch (i) {
                default:
                    this.x *= value;
                    break;
                case 1:
                    this.y *= value;
                    break;
                case 2:
                    this.z *= value;
                    break;
                case 3:
                    this.w *= value;
                    break;
            }
        }
//public	idVec4 &		operator+=( final  idVec4 &a );

        @Override
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
            return ((this.x == a.x) && (this.y == a.y) && (this.z == a.z) && (this.w == a.w));
        }

        public boolean Compare(final idVec4 a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(this.x - a.x) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.y - a.y) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.z - a.z) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.w - a.w) > epsilon) {
                return false;
            }

            return true;
        }
//public	bool			operator==(	final  idVec4 &a ) final ;						// exact compare, no epsilon
//public	bool			operator!=(	final  idVec4 &a ) final ;						// exact compare, no epsilon

        public float Length() {
            return idMath.Sqrt((this.x * this.x) + (this.y * this.y) + (this.z * this.z) + (this.w * this.w));
        }

        public float LengthSqr() {
            return ((this.x * this.x) + (this.y * this.y) + (this.z * this.z) + (this.w * this.w));
        }

        public float Normalize() {// returns length
            float sqrLength, invLength;

            sqrLength = (this.x * this.x) + (this.y * this.y) + (this.z * this.z) + (this.w * this.w);
            invLength = idMath.InvSqrt(sqrLength);
            this.x *= invLength;
            this.y *= invLength;
            this.z *= invLength;
            this.w *= invLength;
            return invLength * sqrLength;
        }

        public float NormalizeFast() {// returns length
            float sqrLength, invLength;

            sqrLength = (this.x * this.x) + (this.y * this.y) + (this.z * this.z) + (this.w * this.w);
            invLength = idMath.RSqrt(sqrLength);
            this.x *= invLength;
            this.y *= invLength;
            this.z *= invLength;
            this.w *= invLength;
            return invLength * sqrLength;
        }

        @Override
        public final int GetDimension() {
            return 4;
        }

        @Deprecated
        public final idVec2 ToVec2() {
//	return *reinterpret_cast<const idVec2 *>(this);
            return new idVec2(this.x, this.y);
        }
//public	idVec2 &		ToVec2( void );

        @Deprecated
        public final idVec3 ToVec3() {
//	return *reinterpret_cast<const idVec3 *>(this);
            return new idVec3(this.x, this.y, this.z);
        }
//public	idVec3 &		ToVec3( void );

        public FloatBuffer toFloatBuffer() {
            return (FloatBuffer) Nio.newFloatBuffer(4).put(this.x)
                	.put(this.y)
                	.put(this.z)
                	.put(this.w).flip();
        }

       public final float[] ToFloatPtr() {
            return new float[]{this.x, this.y, this.z, this.w};//TODO:put shit in array si we can referef it
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
            return this.x + " " + this.y + " " + this.z + " " + this.w;
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
                this.x = v1.x;
                this.y = v1.y;
                this.z = v1.z;
                this.w = v1.w;
            } else if (l >= 1.0f) {
//		(*this) = v2;
                this.x = v2.x;
                this.y = v2.y;
                this.z = v2.z;
                this.w = v2.w;
            } else {
//		(*this) = v1 + l * ( v2 - v1 );
                this.w = v1.w + (l * (v2.w - v1.w));
                this.x = v1.x + (l * (v2.x - v1.x));
                this.y = v1.y + (l * (v2.y - v1.y));
                this.z = v1.z + (l * (v2.z - v1.z));
            }
        }

        @Override
        public idVec4 oSet(final idVec4 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
            this.w = a.w;
            return this;
        }

        public idVec4 oSet(final idVec3 a) {
            this.x = a.x;
            this.y = a.y;
            this.z = a.z;
            return this;
        }

        @Override
        public float oGet(final int i) {//TODO:rename you lazy ass          
            switch (i) {
                default:
                    return this.x;
                case 1:
                    return this.y;
                case 2:
                    return this.z;
                case 3:
                    return this.w;
            }
        }

        @Override
        public float oSet(final int i, final float value) {//TODO:rename you lazy ass          
            switch (i) {
                default:
                    return this.x = value;
                case 1:
                    return this.y = value;
                case 2:
                    return this.z = value;
                case 3:
                    return this.w = value;
            }
        }

        public float oPluSet(final int i, final float value) {
            switch (i) {
                default:
                    return this.x += value;
                case 1:
                    return this.y += value;
                case 2:
                    return this.z += value;
                case 3:
                    return this.w += value;
            }
        }

        @Override
        public ByteBuffer AllocBuffer() {
            return ByteBuffer.allocate(idVec4.BYTES);
        }

        @Override
        public void Read(ByteBuffer buffer) {
            this.x = buffer.getFloat();
            this.y = buffer.getFloat();
            this.z = buffer.getFloat();
            this.w = buffer.getFloat();
        }

        @Override
        public ByteBuffer Write() {
            final ByteBuffer buffer = AllocBuffer();

            buffer.putFloat(this.x).putFloat(this.y).putFloat(this.z).putFloat(this.w).flip();

            return buffer;
        }

        @Override
        public idVec4 oDivide(float a) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public static idVec4[] generateArray(final int length) {
            return Stream.
                    generate(idVec4::new).
                    limit(length).
                    toArray(idVec4[]::new);
        }

        public static ByteBuffer toByteBuffer(idVec4[] vecs) {
            final ByteBuffer data = Nio.newByteBuffer(idVec4.BYTES * vecs.length);

            for (final idVec4 vec : vecs) {
                data.put((ByteBuffer) vec.Write().rewind());
            }

            return (ByteBuffer) data.flip();
        }
    }

    //===============================================================
    //
    //	idVec5 - 5D vector
    //
    //===============================================================
    public static class idVec5 implements idVec<idVec5>, SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static final transient int SIZE  = 5 * Float.SIZE;
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public float x;
        public float y;
        public float z;
        public float s;
        public float t;

        public idVec5() {
        }

        public idVec5(final idVec3 xyz, final idVec2 st) {
            this.x = xyz.x;
            this.y = xyz.y;
            this.z = xyz.z;
//	s = st[0];
            this.s = st.x;
//	t = st[1];
            this.t = st.y;
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
                    return this.x;
                case 1:
                    return this.y;
                case 2:
                    return this.z;
                case 3:
                    return this.s;
                case 4:
                    return this.t;
            }
        }

        @Override
        public float oSet(final int i, final float value) {
            switch (i) {
                default:
                    return this.x = value;
                case 1:
                    return this.y = value;
                case 2:
                    return this.z = value;
                case 3:
                    return this.s = value;
                case 4:
                    return this.t = value;
            }
        }

        @Override
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

        @Override
        public final int GetDimension() {
            return 5;
        }

        public final idVec3 ToVec3() {
//	return *reinterpret_cast<const idVec3 *>(this);
            return new idVec3(this.x, this.y, this.z);
        }

        public FloatBuffer toFloatBuffer() {
            return (FloatBuffer) Nio.newFloatBuffer(3).put(this.x)
                	.put(this.y)
                	.put(this.z).flip();
        }

        //public	idVec3 &		ToVec3( void );
        public final float[] ToFloatPtr() {
            return new float[]{this.x, this.y, this.z};//TODO:array!?
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
                this.x = v1.x + (l * (v2.x - v1.x));
                this.y = v1.y + (l * (v2.y - v1.y));
                this.z = v1.z + (l * (v2.z - v1.z));
                this.s = v1.s + (l * (v2.s - v1.s));
                this.t = v1.t + (l * (v2.t - v1.t));
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
                    generate(idVec5::new).
                    limit(length).
                    toArray(idVec5[]::new);
        }

        public void ToVec3_oMulSet(final idMat3 axis) {
            this.oSet(ToVec3().oMulSet(axis));
        }

        public void ToVec3_oPluSet(final idVec3 origin) {
            this.oSet(ToVec3().oPluSet(origin));
        }
    }

    //===============================================================
    //
    //	idVec6 - 6D vector
    //
    //===============================================================
    public static class idVec6 implements idVec<idVec6>, SERiAL {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static final transient int SIZE  = 6 * Float.SIZE;
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public float p[] = new float[6];

        private static int DBG_counter = 0;
        private final  int DBG_count   = DBG_counter++;
        //
        //

        private static int DBG_idVec6 = 0;

        public idVec6() {
            DBG_idVec6++;
            final int a = 0;
        }

        public idVec6(final float[] a) {
//	memcpy( p, a, 6 * sizeof( float ) );
            System.arraycopy(a, 0, this.p, 0, 6);
        }

        public idVec6(final idVec6 v) {
            System.arraycopy(v.p, 0, this.p, 0, 6);
        }

        public idVec6(final float a1, final float a2, final float a3, final float a4, final float a5, final float a6) {
            this.p[0] = a1;
            this.p[1] = a2;
            this.p[2] = a3;
            this.p[3] = a4;
            this.p[4] = a5;
            this.p[5] = a6;
        }

        public void Set(final float a1, final float a2, final float a3, final float a4, final float a5, final float a6) {
            this.p[0] = a1;
            this.p[1] = a2;
            this.p[2] = a3;
            this.p[3] = a4;
            this.p[4] = a5;
            this.p[5] = a6;
        }

        @Override
        public void Zero() {
            this.p[0] = this.p[1] = this.p[2] = this.p[3] = this.p[4] = this.p[5] = 0.0f;
        }

        //public 	float			operator[]( final  int index ) final ;
//public 	float &			operator[]( final  int index );
        public idVec6 oNegative() {
            return new idVec6(-this.p[0], -this.p[1], -this.p[2], -this.p[3], -this.p[4], -this.p[5]);
        }

        @Override
        public idVec6 oMultiply(final float a) {
            return new idVec6(this.p[0] * a, this.p[1] * a, this.p[2] * a, this.p[3] * a, this.p[4] * a, this.p[5] * a);
        }

        //public 	idVec6			operator/( final  float a ) final ;
        @Override
        public float oMultiply(final idVec6 a) {
            return (this.p[0] * a.p[0]) + (this.p[1] * a.p[1]) + (this.p[2] * a.p[2]) + (this.p[3] * a.p[3]) + (this.p[4] * a.p[4]) + (this.p[5] * a.p[5]);
        }
//public 	idVec6			operator-( final  idVec6 &a ) final ;

        @Override
        public idVec6 oPlus(final idVec6 a) {
            return new idVec6(this.p[0] + a.p[0], this.p[1] + a.p[1], this.p[2] + a.p[2], this.p[3] + a.p[3], this.p[4] + a.p[4], this.p[5] + a.p[5]);
        }
//public 	idVec6 &		operator*=( final  float a );
//public 	idVec6 &		operator/=( final  float a );

        @Override
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
            return ((this.p[0] == a.p[0]) && (this.p[1] == a.p[1]) && (this.p[2] == a.p[2])
                    && (this.p[3] == a.p[3]) && (this.p[4] == a.p[4]) && (this.p[5] == a.p[5]));
        }

        public boolean Compare(final idVec6 a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(this.p[0] - a.p[0]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[1] - a.p[1]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[2] - a.p[2]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[3] - a.p[3]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[4] - a.p[4]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[5] - a.p[5]) > epsilon) {
                return false;
            }

            return true;
        }
//public 	bool			operator==(	final  idVec6 &a ) final ;						// exact compare, no epsilon
//public 	bool			operator!=(	final  idVec6 &a ) final ;						// exact compare, no epsilon

        public float Length() {
            return idMath.Sqrt((this.p[0] * this.p[0]) + (this.p[1] * this.p[1]) + (this.p[2] * this.p[2]) + (this.p[3] * this.p[3]) + (this.p[4] * this.p[4]) + (this.p[5] * this.p[5]));
        }

        public float LengthSqr() {
            return ((this.p[0] * this.p[0]) + (this.p[1] * this.p[1]) + (this.p[2] * this.p[2]) + (this.p[3] * this.p[3]) + (this.p[4] * this.p[4]) + (this.p[5] * this.p[5]));
        }

        public float Normalize() {// returns length
            float sqrLength, invLength;

            sqrLength = (this.p[0] * this.p[0]) + (this.p[1] * this.p[1]) + (this.p[2] * this.p[2]) + (this.p[3] * this.p[3]) + (this.p[4] * this.p[4]) + (this.p[5] * this.p[5]);
            invLength = idMath.InvSqrt(sqrLength);
            this.p[0] *= invLength;
            this.p[1] *= invLength;
            this.p[2] *= invLength;
            this.p[3] *= invLength;
            this.p[4] *= invLength;
            this.p[5] *= invLength;
            return invLength * sqrLength;
        }

        public float NormalizeFast() {// returns length
            float sqrLength, invLength;

            sqrLength = (this.p[0] * this.p[0]) + (this.p[1] * this.p[1]) + (this.p[2] * this.p[2]) + (this.p[3] * this.p[3]) + (this.p[4] * this.p[4]) + (this.p[5] * this.p[5]);
            invLength = idMath.RSqrt(sqrLength);
            this.p[0] *= invLength;
            this.p[1] *= invLength;
            this.p[2] *= invLength;
            this.p[3] *= invLength;
            this.p[4] *= invLength;
            this.p[5] *= invLength;
            return invLength * sqrLength;
        }

        @Override
        public final int GetDimension() {
            return 6;
        }

        /** @deprecated returns readonly vector */
        @Deprecated
        public final idVec3 SubVec3(int index) {
//	return *reinterpret_cast<const idVec3 *>(p + index * 3);
            return new idVec3(this.p[index *= 3], this.p[index + 1], this.p[index + 2]);
        }
//public 	idVec3 &		SubVec3( int index );

        public FloatBuffer toFloatBuffer() {
            return Nio.wrap(this.p);
        }

       public final float[] ToFloatPtr() {
            return this.p;
        }
//public 	float *			ToFloatPtr( void );

        public final String ToString() {
            return ToString(2);
        }

        public final String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

        @Override
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
            return this.p[index];
        }

        @Override
        public float oSet(final int index, final float value) {
            return this.p[index] = value;
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

        public void SubVec3_oSet(final int i, final idVec3 v) {
            System.arraycopy(v.ToFloatPtr(), 0, this.p, i * 3, 3);
        }

        public idVec3 SubVec3_oPluSet(final int i, final idVec3 v) {
            final int off = i * 3;
            this.p[off + 0] += v.x;
            this.p[off + 1] += v.y;
            this.p[off + 2] += v.z;

            return new idVec3(this.p, off);
        }

        public idVec3 SubVec3_oMinSet(final int i, final idVec3 v) {
            return SubVec3_oPluSet(i, v.oNegative());
        }

        public void SubVec3_oMulSet(final int i, final float v) {
            final int off = i * 3;
            this.p[off + 0] *= v;
            this.p[off + 1] *= v;
            this.p[off + 2] *= v;
        }

        public float SubVec3_Normalize(final int i) {
            final idVec3 v = this.SubVec3(i);
            final float normalize = v.Normalize();

            this.SubVec3_oSet(i, v);

            return normalize;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.p);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
				return true;
			}
            if ((o == null) || (getClass() != o.getClass())) {
				return false;
			}

            final idVec6 idVec6 = (idVec6) o;

            return Arrays.equals(this.p, idVec6.p);
        }

        @Override
        public String toString() {
            return "idVec6{" +
                    "p=" + Arrays.toString(this.p) +
                    '}';
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
        private static int     tempIndex;                                     // index into memory pool, wraps around
        //
        //

        static int VECX_QUAD(int x) {
            return ((x + 3) & ~3);
        }

        @Deprecated
        void VECX_CLEAREND() {//TODO:is this function need for Java?
            int s = this.size;
////            while (s < ((s + 3) & ~3)) {
            while (s < this.p.length) {
                this.p[s++] = 0.0f;
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
            this.size = this.alloced = 0;
            this.p = null;
        }

        public idVecX(int length) {
            this.size = this.alloced = 0;
            this.p = null;
            SetSize(length);
        }

        public idVecX(int length, float[] data) {
            this.size = this.alloced = 0;
            this.p = null;
            SetData(length, data);
        }
//public					~idVecX( void );

        //public	float			operator[]( const int index ) const;
        public float oGet(final int index) {
            return this.p[index];
        }

        public float oSet(final int index, final float value) {
            return this.p[index] = value;
        }

        //public	float &			operator[]( const int index );
//public	idVecX			operator-() const;
        public idVecX oNegative() {
            int i;
            final idVecX m = new idVecX();

            m.SetTempSize(this.size);
            for (i = 0; i < this.size; i++) {
                m.p[i] = -this.p[i];
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
            final idVecX m = new idVecX();

            m.SetTempSize(this.size);
            if (this.VECX_SIMD) {
                SIMDProcessor.Mul16(m.p, this.p, a, this.size);
            } else {
                int i;
                for (i = 0; i < this.size; i++) {
                    m.p[i] = this.p[i] * a;
                }
            }
            return m;
        }
//public	idVecX			operator/( const float a ) const;
//public	float			operator*( const idVecX &a ) const;

        public float oMultiply(final idVecX a) {
            int i;
            float sum = 0.0f;

            assert (this.size == a.size);
            for (i = 0; i < this.size; i++) {
                sum += this.p[i] * a.p[i];
            }
            return sum;
        }
//public	idVecX			operator-( const idVecX &a ) const;
//public	idVecX			operator+( const idVecX &a ) const;

        public idVecX oPlus(final idVecX a) {
            final idVecX m = new idVecX();

            assert (this.size == a.size);
            m.SetTempSize(this.size);
//#ifdef VECX_SIMD
//	SIMDProcessor->Add16( m.p, p, a.p, size );
//#else
            int i;
            for (i = 0; i < this.size; i++) {
                m.p[i] = this.p[i] + a.p[i];
            }
//#endif
            return m;
        }
//public	idVecX &		operator*=( const float a );

        public idVecX oMulSet(final float a) {
//#ifdef VECX_SIMD
//	SIMDProcessor->MulAssign16( p, a, size );
//#else
            int i;
            for (i = 0; i < this.size; i++) {
                this.p[i] *= a;
            }
//#endif
            return this;
        }
//public	idVecX &		operator/=( const float a );
//public	idVecX &		operator+=( const idVecX &a );
//public	idVecX &		operator-=( const idVecX &a );
//public	friend idVecX	operator*( const float a, const idVecX b );

        public boolean Compare(final idVecX a) {// exact compare, no epsilon
            int i;

            assert (this.size == a.size);
            for (i = 0; i < this.size; i++) {
                if (this.p[i] != a.p[i]) {
                    return false;
                }
            }
            return true;
        }

        public boolean Compare(final idVecX a, final float epsilon) {// compare with epsilon
            int i;

            assert (this.size == a.size);
            for (i = 0; i < this.size; i++) {
                if (idMath.Fabs(this.p[i] - a.p[i]) > epsilon) {
                    return false;
                }
            }
            return true;
        }
//public	bool			operator==(	const idVecX &a ) const;						// exact compare, no epsilon
//public	bool			operator!=(	const idVecX &a ) const;						// exact compare, no epsilon

        public void SetSize(int newSize) {
            final int alloc = (newSize + 3) & ~3;
            if ((alloc > this.alloced) && (this.alloced != -1)) {
                if (this.p != null) {
//			Mem_Free16( p );
                    this.p = null;
                }
//		p = (float *) Mem_Alloc16( alloc * sizeof( float ) );
                this.p = new float[alloc];
                this.alloced = alloc;
            }
            this.size = newSize;
            VECX_CLEAREND();
        }

        public void ChangeSize(int newSize) {
            ChangeSize(newSize, false);
        }

        public void ChangeSize(int newSize, boolean makeZero) {
            final int alloc = (newSize + 3) & ~3;
            if ((alloc > this.alloced) && (this.alloced != -1)) {
                final float[] oldVec = this.p;
//		p = (float *) Mem_Alloc16( alloc * sizeof( float ) );
                this.p = new float[alloc];
                this.alloced = alloc;
                if (oldVec != null) {
                    System.arraycopy(oldVec, 0, this.p, 0, this.size);
//			Mem_Free16( oldVec );//garbage collect me!
                }//TODO:ifelse
                if (makeZero) {
                    // zero any new elements
                    for (int i = this.size; i < newSize; i++) {
                        this.p[i] = 0.0f;
                    }
                }
            }
            this.size = newSize;
            VECX_CLEAREND();
        }

        public int GetSize() {
            return this.size;
        }

        public void SetData(int length, float[] data) {
            if ((this.p != null) && ((this.p[0] < idVecX.tempPtr[0]) || (this.p[0] >= (idVecX.tempPtr[0] + VECX_MAX_TEMP))) && (this.alloced != -1)) {
//		Mem_Free16( p );
                this.p = null;
            }
//	assert( ( ( (int) data ) & 15 ) == 0 ); // data must be 16 byte aligned
            this.p = data;
            this.size = length;
            this.alloced = -1;
            VECX_CLEAREND();
        }

        public void Zero() {
//#ifdef VECX_SIMD
//	SIMDProcessor.Zero16( p, size );
//#else
//	memset( p, 0, size * sizeof( float ) );
//#endif
            Arrays.fill(this.p, 0, this.size, 0);
        }

        public void Zero(int length) {
            SetSize(length);
//#ifdef VECX_SIMD
//	SIMDProcessor.Zero16( p, length );
//#else
//	memset( p, 0, size * sizeof( float ) );
//#endif
            Arrays.fill(this.p, 0, this.size, 0);
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
            final idRandom rnd = new idRandom(seed);

            c = u - l;
            for (i = 0; i < this.size; i++) {
                this.p[i] = l + (rnd.RandomFloat() * c);
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
            final idRandom rnd = new idRandom(seed);

            SetSize(length);
            c = u - l;
            for (i = 0; i < this.size; i++) {
                if (idMatX.DISABLE_RANDOM_TEST) {//for testing.
                    this.p[i] = i;
                } else {
                    this.p[i] = l + (rnd.RandomFloat() * c);
                }
            }
        }

        public void Negate() {
//#ifdef VECX_SIMD
//	SIMDProcessor.Negate16( p, size );
//#else
            int oGet;
            for (oGet = 0; oGet < this.size; oGet++) {
                this.p[oGet] = -this.p[oGet];
            }
//#endif
        }

        public void Clamp(float min, float max) {
            int i;
            for (i = 0; i < this.size; i++) {
                if (this.p[i] < min) {
                    this.p[i] = min;
                } else if (this.p[i] > max) {
                    this.p[i] = max;
                }
            }
        }

        public idVecX SwapElements(int e1, int e2) {
            float tmp;
            tmp = this.p[e1];
            this.p[e1] = this.p[e2];
            this.p[e2] = tmp;
            return this;
        }

        public float Length() {
            int i;
            float sum = 0.0f;

            for (i = 0; i < this.size; i++) {
                sum += this.p[i] * this.p[i];
            }
            return idMath.Sqrt(sum);
        }

        public float LengthSqr() {
            int i;
            float sum = 0.0f;

            for (i = 0; i < this.size; i++) {
                sum += this.p[i] * this.p[i];
            }
            return sum;
        }

        public idVecX Normalize() {
            int i;
            final idVecX m = new idVecX();
            float invSqrt, sum = 0.0f;

            m.SetTempSize(this.size);
            for (i = 0; i < this.size; i++) {
                sum += this.p[i] * this.p[i];
            }
            invSqrt = idMath.InvSqrt(sum);
            for (i = 0; i < this.size; i++) {
                m.p[i] = this.p[i] * invSqrt;
            }
            return m;
        }

        public float NormalizeSelf() {
            float invSqrt, sum = 0.0f;
            int i;
            for (i = 0; i < this.size; i++) {
                sum += this.p[i] * this.p[i];
            }
            invSqrt = idMath.InvSqrt(sum);
            for (i = 0; i < this.size; i++) {
                this.p[i] *= invSqrt;
            }
            return invSqrt * sum;
        }

        public int GetDimension() {
            return this.size;
        }

        /** @deprecated readonly */
        @Deprecated
        public idVec3 SubVec3(int index) {
            assert ((index >= 0) && (((index * 3) + 3) <= this.size));
//	return *reinterpret_cast<idVec3 *>(p + index * 3);
            return new idVec3(this.p[index *= 3], this.p[index + 1], this.p[index + 2]);
        }
//public	idVec3 &		SubVec3( int index );

        /** @deprecated readonly */
        @Deprecated
        public idVec6 SubVec6(int index) {
            assert ((index >= 0) && (((index * 6) + 6) <= this.size));
//	return *reinterpret_cast<idVec6 *>(p + index * 6);
            return new idVec6(this.p[index *= 6], this.p[index + 1], this.p[index + 2], this.p[index + 3], this.p[index + 4], this.p[index + 5]);
        }
//public	idVec6 &		SubVec6( int index );

        public FloatBuffer toFloatBuffer() {
            return Nio.wrap(this.p);
        }

       public float[] ToFloatPtr() {
            return this.p;
        }
//public	float *			ToFloatPtr( void );

        public String ToString() {
            return ToString(2);
        }

        public String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

        public void SetTempSize(int newSize) {

            this.size = newSize;
            this.alloced = (newSize + 3) & ~3;
            assert (this.alloced < VECX_MAX_TEMP);
            if ((idVecX.tempIndex + this.alloced) > VECX_MAX_TEMP) {
                idVecX.tempIndex = 0;
            }
//            p = idVecX.tempPtr + idVecX.tempIndex;
//            for (int a = 0; a < idVecX.tempIndex; a++) {//TODO:trippple check
//                p[a] = idVecX.tempPtr[a + idVecX.tempIndex];
//            }
            this.p = new float[this.alloced];
            idVecX.tempIndex += this.alloced;
            VECX_CLEAREND();
        }

        public void SubVec3_Normalize(int i) {
            final idVec3 vec3 = new idVec3(this.p, i * 3);
            vec3.Normalize();
            this.SubVec3_oSet(i, vec3);
        }

        public void SubVec3_oSet(int i, idVec3 v) {
            this.p[(i * 3) + 0] = v.oGet(0);
            this.p[(i * 3) + 1] = v.oGet(1);
            this.p[(i * 3) + 2] = v.oGet(2);
        }

        public void SubVec6_oSet(int i, idVec6 v) {
            this.p[(i * 6) + 0] = v.oGet(0);
            this.p[(i * 6) + 1] = v.oGet(1);
            this.p[(i * 6) + 2] = v.oGet(2);
            this.p[(i * 6) + 3] = v.oGet(3);
            this.p[(i * 6) + 4] = v.oGet(4);
            this.p[(i * 6) + 5] = v.oGet(5);
        }

        public void SubVec6_oPluSet(int i, idVec6 v) {
            this.p[(i * 6) + 0] += v.oGet(0);
            this.p[(i * 6) + 1] += v.oGet(1);
            this.p[(i * 6) + 2] += v.oGet(2);
            this.p[(i * 6) + 3] += v.oGet(3);
            this.p[(i * 6) + 4] += v.oGet(4);
            this.p[(i * 6) + 5] += v.oGet(5);
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
            final float[] sp = new float[1], cp = new float[1], st = new float[1], ct = new float[1];
//            sp = cp = st = ct = 0.0f;
            idMath.SinCos(this.phi, sp, cp);
            idMath.SinCos(this.theta, st, ct);
            return new idVec3(cp[0] * this.radius * ct[0], cp[0] * this.radius * st[0], this.radius * sp[0]);
        }
    }

    /*
     ===============================================================================

     Old 3D vector macros, should no longer be used.

     ===============================================================================
     */
    public static double DotProduct(double[] a, double[] b) {
        return ((a[0] * b[0]) + (a[1] * b[1]) + (a[2] * b[2]));
    }

    public static float DotProduct(float[] a, float[] b) {
        return ((a[0] * b[0]) + (a[1] * b[1]) + (a[2] * b[2]));
    }

    public static float DotProduct(idVec3 a, idVec3 b) {
        return ((a.oGet(0) * b.oGet(0))
                + (a.oGet(1) * b.oGet(1))
                + (a.oGet(2) * b.oGet(2)));
    }

    public static float DotProduct(idVec3 a, idVec4 b) {
        return DotProduct(a, b.ToVec3());
    }

    public static float DotProduct(idVec3 a, idVec5 b) {
        return DotProduct(a, b.ToVec3());
    }

    public static float DotProduct(idPlane a, idPlane b) {
        return ((a.oGet(0) * b.oGet(0))
                + (a.oGet(1) * b.oGet(1))
                + (a.oGet(2) * b.oGet(2)));
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
        o[0] = v[0] + (b[0] * s);
        o[1] = v[1] + (b[1] * s);
        o[2] = v[2] + (b[2] * s);
    }

    public static void VectorMA(final idVec3 v, final float s, final idVec3 b, idVec3 o) {
        o.oSet(0, v.oGet(0) + (b.oGet(0) * s));
        o.oSet(1, v.oGet(1) + (b.oGet(1) * s));
        o.oSet(2, v.oGet(2) + (b.oGet(2) * s));
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
