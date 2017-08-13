package neo.idlib.math;

import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Matrix.idMat4;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Quat {

    /**
     * ===============================================================================
     *
     * Quaternion
     *
     * ===============================================================================
     */
    public static class idQuat {
//        public:

        public float x;//TODO:prime candidate to turn into an array.
        public float y;
        public float z;
        public float w;

        public idQuat() {
        }

        public idQuat(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        public idQuat(final idQuat quat) {
            this.x = quat.x;
            this.y = quat.y;
            this.z = quat.z;
            this.w = quat.w;
        }

        public void Set(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
//
//	float			operator[]( int index ) const;

        public float oGet(final int index) {
            switch (index) {
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
//	float &			operator[]( int index );

        public void oSet(final int index, final float value) {
            switch (index) {
                default:
                    x = value;
                    break;
                case 1:
                    y = value;
                    break;
                case 2:
                    z = value;
                    break;
                case 3:
                    w = value;
                    break;
            }
        }

        public idQuat oNegative() {
            return new idQuat(-x, -y, -z, -w);
        }

        public idQuat oSet(final idQuat a) {
            x = a.x;
            y = a.y;
            z = a.z;
            w = a.w;

            return this;
        }

        public idQuat oPlus(final idQuat a) {
            return new idQuat(x + a.x, y + a.y, z + a.z, w + a.w);
        }

        public idQuat oPluSet(final idQuat a) {
            x += a.x;
            y += a.y;
            z += a.z;
            w += a.w;

            return this;
        }

        public idQuat oMinus(final idQuat a) {
            return new idQuat(x - a.x, y - a.y, z - a.z, w - a.w);
        }

        public idQuat oMinSet(final idQuat a) {
            x -= a.x;
            y -= a.y;
            z -= a.z;
            w -= a.w;

            return this;
        }

        public idQuat oMultiply(final idQuat a) {
            return new idQuat(
                    w * a.x + x * a.w + y * a.z - z * a.y,
                    w * a.y + y * a.w + z * a.x - x * a.z,
                    w * a.z + z * a.w + x * a.y - y * a.x,
                    w * a.w - x * a.x - y * a.y - z * a.z);
        }

        public idVec3 oMultiply(final idVec3 a) {
//#if 0
            // it's faster to do the conversion to a 3x3 matrix and multiply the vector by this 3x3 matrix
//            return (ToMat3() * a);
//#else
            // result = this->Inverse() * idQuat( a.x, a.y, a.z, 0.0f ) * (*this)
            float xxzz = x * x - z * z;
            float wwyy = w * w - y * y;

            float xw2 = x * w * 2.0f;
            float xy2 = x * y * 2.0f;
            float xz2 = x * z * 2.0f;
            float yw2 = y * w * 2.0f;
            float yz2 = y * z * 2.0f;
            float zw2 = z * w * 2.0f;

            return new idVec3(
                    (xxzz + wwyy) * a.x + (xy2 + zw2) * a.y + (xz2 - yw2) * a.z,
                    (xy2 - zw2) * a.x + (y * y + w * w - x * x - z * z) * a.y + (yz2 + xw2) * a.z,
                    (xz2 + yw2) * a.x + (yz2 - xw2) * a.y + (wwyy - xxzz) * a.z);
//#endif
        }

        public idQuat oMultiply(float a) {
            return new idQuat(x * a, y * a, z * a, w * a);
        }

        public idQuat oMulSet(final idQuat a) {
            this.oSet(this.oMultiply(a));

            return this;
        }

        public idQuat oMulSet(float a) {
            x *= a;
            y *= a;
            z *= a;
            w *= a;

            return this;
        }
//

        public static idQuat oMultiply(final float a, final idQuat b) {
            return b.oMultiply(a);
        }

        public static idVec3 oMultiply(final idVec3 a, final idQuat b) {
            return b.oMultiply(a);
        }
//

        public boolean Compare(final idQuat a) {// exact compare, no epsilon
            return ((x == a.x) && (y == a.y) && (z == a.z) && (w == a.w));
        }

        public boolean Compare(final idQuat a, final float epsilon) {// compare with epsilon
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

//public 	bool			operator==(	const idQuat &a ) const;					// exact compare, no epsilon
//public 	bool			operator!=(	const idQuat &a ) const;					// exact compare, no epsilon
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 31 * hash + Float.floatToIntBits(this.x);
            hash = 31 * hash + Float.floatToIntBits(this.y);
            hash = 31 * hash + Float.floatToIntBits(this.z);
            hash = 31 * hash + Float.floatToIntBits(this.w);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final idQuat other = (idQuat) obj;
            if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x)) {
                return false;
            }
            if (Float.floatToIntBits(this.y) != Float.floatToIntBits(other.y)) {
                return false;
            }
            if (Float.floatToIntBits(this.z) != Float.floatToIntBits(other.z)) {
                return false;
            }
            if (Float.floatToIntBits(this.w) != Float.floatToIntBits(other.w)) {
                return false;
            }
            return true;
        }
//

        public idQuat Inverse() {
            return new idQuat(-x, -y, -z, w);
        }

        public float Length() {
            float len;

            len = x * x + y * y + z * z + w * w;
            return idMath.Sqrt(len);
        }

        public idQuat Normalize() {
            float len;
            float ilength;

            len = this.Length();
            if (len != 0) {
                ilength = 1 / len;
                x *= ilength;
                y *= ilength;
                z *= ilength;
                w *= ilength;
            }
            return this;
        }
//

        public float CalcW() {
            // take the absolute value because floating point rounding may cause the dot of x,y,z to be larger than 1
            return (float) Math.sqrt(Math.abs(1.0f - (x * x + y * y + z * z)));
        }

        public int GetDimension() {
            return 4;
        }
//

        public idAngles ToAngles() {
            return ToMat3().ToAngles();
        }

        public idRotation ToRotation() {
            idVec3 vec = new idVec3();
            float angle;

            vec.x = x;
            vec.y = y;
            vec.z = z;
            angle = idMath.ACos(w);
            if (angle == 0.0f) {
                vec.Set(0.0f, 0.0f, 1.0f);
            } else {
                //vec *= (1.0f / sin( angle ));
                vec.Normalize();
                vec.FixDegenerateNormal();
                angle *= 2.0f * idMath.M_RAD2DEG;
            }
            return new idRotation(Vector.getVec3_origin(), vec, angle);
        }

        public idMat3 ToMat3() {
            idMat3 mat = new idMat3();
            float wx, wy, wz;
            float xx, yy, yz;
            float xy, xz, zz;
            float x2, y2, z2;

            x2 = x + x;
            y2 = y + y;
            z2 = z + z;

            xx = x * x2;
            xy = x * y2;
            xz = x * z2;

            yy = y * y2;
            yz = y * z2;
            zz = z * z2;

            wx = w * x2;
            wy = w * y2;
            wz = w * z2;

            mat.oSet(0, 0, 1.0f - ( yy + zz ));
            mat.oSet(0, 1, xy - wz);
            mat.oSet(0, 2, xz + wy);

            mat.oSet(1, 0, xy + wz);
            mat.oSet(1, 1, 1.0f - ( xx + zz ));
            mat.oSet(1, 2, yz - wx);

            mat.oSet(2, 0, xz - wy);
            mat.oSet(2, 1, yz + wx);
            mat.oSet(2, 2, 1.0f - ( xx + yy ));

            return mat;
        }

        public idMat4 ToMat4() {
            return ToMat3().ToMat4();
        }

        public idCQuat ToCQuat() {
            if (w < 0.0f) {
                return new idCQuat(-x, -y, -z);
            }
            return new idCQuat(x, y, z);
        }

        public idVec3 ToAngularVelocity() {
            idVec3 vec = new idVec3();

            vec.x = x;
            vec.y = y;
            vec.z = z;
            vec.Normalize();
            return vec.oMultiply(idMath.ACos(w));
        }

//public 	const float *	ToFloatPtr( void ) const;
        @Deprecated
        public float[] ToFloatPtr() {
            return new float[]{x, y, z, w};//TODO:array!?
        }

        public String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }

//
        /**
         * ===================== idQuat::Slerp
         *
         * Spherical linear interpolation between two quaternions.
         * =====================
         */
        public idQuat Slerp(final idQuat from, final idQuat to, float t) {
            idQuat temp = new idQuat();
            float omega, cosom, sinom, scale0, scale1;

            if (t <= 0.0f) {
                this.oSet(from);
                return this;
            }

            if (t >= 1.0f) {
                this.oSet(to);
                return this;
            }

            if (from == to) {
                this.oSet(to);
                return this;
            }

            cosom = from.x * to.x + from.y * to.y + from.z * to.z + from.w * to.w;
            if (cosom < 0.0f) {
                this.oSet(to.oNegative());
                cosom = -cosom;
            } else {
                temp = to;
            }

            if ((1.0f - cosom) > 1e-6f) {
//#if 0
//		omega = acos( cosom );
//		sinom = 1.0f / sin( omega );
//		scale0 = sin( ( 1.0f - t ) * omega ) * sinom;
//		scale1 = sin( t * omega ) * sinom;
//#else
                scale0 = 1.0f - cosom * cosom;
                sinom = idMath.InvSqrt(scale0);
                omega = idMath.ATan16(scale0 * sinom, cosom);
                scale0 = idMath.Sin16((1.0f - t) * omega) * sinom;
                scale1 = idMath.Sin16(t * omega) * sinom;
//#endif
            } else {
                scale0 = 1.0f - t;
                scale1 = t;
            }

            this.oSet(from.oMultiply(scale0).oPlus(temp.oMultiply(scale1)));
            return this;
        }
    };

    /**
     * ===============================================================================
     *
     * Compressed quaternion
     *
     * ===============================================================================
     */
    public static class idCQuat {
//        public:

        public float x;
        public float y;
        public float z;
//

        public idCQuat() {
        }

        public idCQuat(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
//

        void Set(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
//

//	float			operator[]( int index ) const;
        public float oGet(final int index) {
            switch (index) {
                default:
                    return x;
                case 1:
                    return y;
                case 2:
                    return z;
            }
        }

//	float &			operator[]( int index );
        public void oSet(final int index, final float value) {
            switch (index) {
                default:
                    x = value;
                    break;
                case 1:
                    y = value;
                    break;
                case 2:
                    z = value;
                    break;
            }
        }
//

        public boolean Compare(final idCQuat a) {// exact compare, no epsilon
            return ((x == a.x) && (y == a.y) && (z == a.z));
        }

        public boolean Compare(final idCQuat a, final float epsilon) {// compare with epsilon
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

//	bool			operator==(	const idCQuat &a ) const;					// exact compare, no epsilon
//	bool			operator!=(	const idCQuat &a ) const;					// exact compare, no epsilon
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Float.floatToIntBits(this.x);
            hash = 37 * hash + Float.floatToIntBits(this.y);
            hash = 37 * hash + Float.floatToIntBits(this.z);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final idCQuat other = (idCQuat) obj;
            if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x)) {
                return false;
            }
            if (Float.floatToIntBits(this.y) != Float.floatToIntBits(other.y)) {
                return false;
            }
            if (Float.floatToIntBits(this.z) != Float.floatToIntBits(other.z)) {
                return false;
            }
            return true;
        }
//

        public int GetDimension() {
            return 3;
        }
//

        public idAngles ToAngles() {
            return ToQuat().ToAngles();
        }

        public idRotation ToRotation() {
            return ToQuat().ToRotation();
        }

        public idMat3 ToMat3() {
            return ToQuat().ToMat3();
        }

        public idMat4 ToMat4() {
            return ToQuat().ToMat4();
        }

        public idQuat ToQuat() {
            // take the absolute value because floating point rounding may cause the dot of x,y,z to be larger than 1
            return new idQuat(x, y, z, (float) Math.sqrt(Math.abs(1.0f - (x * x + y * y + z * z))));
        }
//	const float *	ToFloatPtr( void ) const;

        public float[] ToFloatPtr() {
            return new float[]{x, y, z};//TODO:back redf
        }

        public String ToString(int precision) {
            return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
        }
    };
}
