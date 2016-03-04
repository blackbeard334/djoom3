package neo.idlib.math.Matrix;

import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Angles.idAngles;
import static neo.idlib.math.Math_h.DEG2RAD;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Matrix.idMat0.MATRIX_EPSILON;
import static neo.idlib.math.Matrix.idMat0.MATRIX_INVERSE_EPSILON;
import neo.idlib.math.Quat.idCQuat;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Rotation.idRotation;
import static neo.idlib.math.Vector.RAD2DEG;
import neo.idlib.math.Vector.idVec3;

//===============================================================
//
//	idMat3 - 3x3 matrix
//
//	NOTE:	matrix is column-major
//
//===============================================================
public class idMat3 {
    public static final int    BYTES         = idVec3.BYTES * 3;

    private static final idMat3 mat3_zero     = new idMat3(new idVec3(0, 0, 0), new idVec3(0, 0, 0), new idVec3(0, 0, 0));
    private static final idMat3 mat3_identity = new idMat3(new idVec3(1, 0, 0), new idVec3(0, 1, 0), new idVec3(0, 0, 1));
    private static final idMat3 mat3_default  = mat3_identity;

    public static idMat3 getMat3_zero() {
        return new idMat3(mat3_zero);
    }

    public static idMat3 getMat3_identity() {
        return new idMat3(mat3_identity);
    }

    public static idMat3 getMat3_default() {
        return new idMat3(mat3_default);
    }

    final idVec3[] mat = {new idVec3(), new idVec3(), new idVec3()};

    private static int DBG_counter = 0;
    private final  int DBG_count = DBG_counter++;

    public idMat3() {
    }

    public idMat3(final idVec3 x, final idVec3 y, final idVec3 z) {
        mat[0].x = x.x;
        mat[0].y = x.y;
        mat[0].z = x.z;
        //
        mat[1].x = y.x;
        mat[1].y = y.y;
        mat[1].z = y.z;
        //
        mat[2].x = z.x;
        mat[2].y = z.y;
        mat[2].z = z.z;
    }

    public idMat3(final float xx, final float xy, final float xz, final float yx, final float yy, final float yz, final float zx, final float zy, final float zz) {
        mat[0].x = xx;
        mat[0].y = xy;
        mat[0].z = xz;
        //
        mat[1].x = yx;
        mat[1].y = yy;
        mat[1].z = yz;
        //
        mat[2].x = zx;
        mat[2].y = zy;
        mat[2].z = zz;
    }

    public idMat3(final idMat3 m) {
        mat[0].x = m.mat[0].x;
        mat[0].y = m.mat[0].y;
        mat[0].z = m.mat[0].z;
        //
        mat[1].x = m.mat[1].x;
        mat[1].y = m.mat[1].y;
        mat[1].z = m.mat[1].z;
        //
        mat[2].x = m.mat[2].x;
        mat[2].y = m.mat[2].y;
        mat[2].z = m.mat[2].z;
    }

    public idMat3(final float src[][]) {
//	memcpy( mat, src, 3 * 3 * sizeof( float ) );
        mat[0] = new idVec3(src[0][0], src[0][1], src[0][2]);
        mat[1] = new idVec3(src[1][0], src[1][1], src[1][2]);
        mat[2] = new idVec3(src[2][0], src[2][1], src[2][2]);
    }
//
//public	const idVec3 &	operator[]( int index ) const;
//public	idVec3 &		operator[]( int index );

    public idVec3 oGet(int index) {
        return mat[index];
    }

    public float oGet(final int index1, final int index2) {
        return mat[index1].oGet(index2);
    }

    public void oSet(final int index, final idVec3 vec3) {
        mat[index].oSet(vec3);
    }

//public	idMat3			operator-() const;
    public idMat3 oNegative() {
        return new idMat3(
                -mat[0].x, -mat[0].y, -mat[0].z,
                -mat[1].x, -mat[1].y, -mat[1].z,
                -mat[2].x, -mat[2].y, -mat[2].z);
    }
//public	idMat3			operator*( const float a ) const;

    public idMat3 oMultiply(final float a) {
        return new idMat3(
                mat[0].x * a, mat[0].y * a, mat[0].z * a,
                mat[1].x * a, mat[1].y * a, mat[1].z * a,
                mat[2].x * a, mat[2].y * a, mat[2].z * a);
    }
//public	idVec3			operator*( const idVec3 &vec ) const;

    public idVec3 oMultiply(final idVec3 vec) {
        return new idVec3(
                mat[0].x * vec.x + mat[1].x * vec.y + mat[2].x * vec.z,
                mat[0].y * vec.x + mat[1].y * vec.y + mat[2].y * vec.z,
                mat[0].z * vec.x + mat[1].z * vec.y + mat[2].z * vec.z);
    }
//public	idMat3			operator*( const idMat3 &a ) const;

    public idMat3 oMultiply(final idMat3 a) {
        int i, j;
        float[] m1Ptr, m2Ptr;
//            float dstPtr;
        idMat3 dst = new idMat3();

        m1Ptr = this.ToFloatPtr();//reinterpret_cast<const float *>(this);
        m2Ptr = a.ToFloatPtr();//reinterpret_cast<const float *>(&a);
//	dstPtr = reinterpret_cast<float *>(&dst);
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                float value = m1Ptr[i * 3 + 0] * m2Ptr[0 * 3 + j]
                        + m1Ptr[i * 3 + 1] * m2Ptr[1 * 3 + j]
                        + m1Ptr[i * 3 + 2] * m2Ptr[2 * 3 + j];
                dst.oSet(i, j, value);
//			dstPtr++;
            }
//                m1Ptr += 3;
        }
        return dst;
    }
//public	idMat3			operator+( const idMat3 &a ) const;

    public idMat3 oPlus(final idMat3 a) {
        return new idMat3(
                this.mat[0].x + a.mat[0].x, this.mat[0].y + a.mat[0].y, this.mat[0].z + a.mat[0].z,
                this.mat[1].x + a.mat[1].x, this.mat[1].y + a.mat[1].y, this.mat[1].z + a.mat[1].z,
                this.mat[2].x + a.mat[2].x, this.mat[2].y + a.mat[2].y, this.mat[2].z + a.mat[2].z);
    }
//public	idMat3			operator-( const idMat3 &a ) const;

    public idMat3 oMinus(final idMat3 a) {
        return new idMat3(
                this.mat[0].x - a.mat[0].x, this.mat[0].y - a.mat[0].y, this.mat[0].z - a.mat[0].z,
                this.mat[1].x - a.mat[1].x, this.mat[1].y - a.mat[1].y, this.mat[1].z - a.mat[1].z,
                this.mat[2].x - a.mat[2].x, this.mat[2].y - a.mat[2].y, this.mat[2].z - a.mat[2].z);
    }
//public	idMat3 &		operator*=( const float a );

    public idMat3 oMulSet(final float a) {
        this.mat[0].x *= a;
        this.mat[0].y *= a;
        this.mat[0].z *= a;
        //
        this.mat[1].x *= a;
        this.mat[1].y *= a;
        this.mat[1].z *= a;
        //
        this.mat[2].x *= a;
        this.mat[2].y *= a;
        this.mat[2].z *= a;
        return this;
    }
//public	idMat3 &		operator*=( const idMat3 &a );

    public idMat3 oMulSet(final idMat3 a) {
        int i, j;

        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                float value 
                        = mat[i].x * a.mat[0].oGet(j)
                        + mat[i].y * a.mat[1].oGet(j)
                        + mat[i].z * a.mat[2].oGet(j);
                this.oSet(i, j, value);
            }
        }
        return this;
    }
//public	idMat3 &		operator+=( const idMat3 &a );

    public idMat3 oPluSet(final float a) {
        this.mat[0].x += a;
        this.mat[0].y += a;
        this.mat[0].z += a;
        //
        this.mat[1].x += a;
        this.mat[1].y += a;
        this.mat[1].z += a;
        //
        this.mat[2].x += a;
        this.mat[2].y += a;
        this.mat[2].z += a;
        return this;
    }
//public	idMat3 &		operator-=( const idMat3 &a );

    public idMat3 oMinSet(final float a) {
        this.mat[0].x -= a;
        this.mat[0].y -= a;
        this.mat[0].z -= a;
        //
        this.mat[1].x -= a;
        this.mat[1].y -= a;
        this.mat[1].z -= a;
        //
        this.mat[2].x -= a;
        this.mat[2].y -= a;
        this.mat[2].z -= a;
        return this;
    }
//
//public	friend idMat3	operator*( const float a, const idMat3 &mat );

    public static idMat3 oMultiply(final float a, final idMat3 mat) {
        return mat.oMultiply(a);
    }
//public	friend idVec3	operator*( const idVec3 &vec, const idMat3 &mat );

    public static idVec3 oMultiply(final idVec3 vec, final idMat3 mat) {
        return mat.oMultiply(vec);
    }
//public	friend idVec3 &	operator*=( idVec3 &vec, const idMat3 &mat );

    public static idVec3 oMulSet(idVec3 vec, final idMat3 mat) {
        float x = mat.mat[0].x * vec.x + mat.mat[1].x * vec.y + mat.mat[2].x * vec.z;
        float y = mat.getRow(0).y * vec.x + mat.mat[1].y * vec.y + mat.mat[2].y * vec.z;
        vec.z = mat.mat[0].z * vec.x + mat.mat[1].z * vec.y + mat.mat[2].z * vec.z;
        vec.x = x;
        vec.y = y;
        return vec;
    }
//

    public boolean Compare(final idMat3 a) {// exact compare, no epsilon
        if (mat[0].Compare(a.mat[0])
                && mat[1].Compare(a.mat[1])
                && mat[2].Compare(a.mat[2])) {
            return true;
        }
        return false;
    }

    public boolean Compare(final idMat3 a, final float epsilon) {// compare with epsilon
        if (mat[0].Compare(a.mat[0], epsilon)
                && mat[1].Compare(a.mat[1], epsilon)
                && mat[2].Compare(a.mat[2], epsilon)) {
            return true;
        }
        return false;
    }
//public	bool			operator==( const idMat3 &a ) const;					// exact compare, no epsilon
//public	bool			operator!=( const idMat3 &a ) const;					// exact compare, no epsilon

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.mat[0].hashCode();
        hash = 37 * hash + this.mat[1].hashCode();
        hash = 37 * hash + this.mat[2].hashCode();
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
        final idMat3 other = (idMat3) obj;

        return this.mat[0].equals(other.mat[0])
                && this.mat[1].equals(other.mat[1])
                && this.mat[2].equals(other.mat[2]);
    }

    public void Zero() {
        mat[0].Zero();
        mat[1].Zero();
        mat[2].Zero();
    }

    public void Identity() {
        this.oSet(getMat3_identity());
    }

    public boolean IsIdentity() {
        return IsIdentity((float) MATRIX_EPSILON);
    }

    public boolean IsIdentity(final float epsilon) {
        return Compare(getMat3_identity(), epsilon);
    }

    public boolean IsSymmetric() {
        return IsSymmetric((float) MATRIX_EPSILON);
    }

    public boolean IsSymmetric(final float epsilon) {
        if (idMath.Fabs(mat[0].y - mat[1].x) > epsilon) {
            return false;
        }
        if (idMath.Fabs(mat[0].z - mat[2].x) > epsilon) {
            return false;
        }
        if (idMath.Fabs(mat[1].z - mat[2].y) > epsilon) {
            return false;
        }
        return true;
    }

    public boolean IsDiagonal() {
        return IsDiagonal((float) MATRIX_EPSILON);
    }

    public boolean IsDiagonal(final float epsilon) {
        if (idMath.Fabs(mat[0].y) > epsilon
                || idMath.Fabs(mat[0].z) > epsilon
                || idMath.Fabs(mat[1].x) > epsilon
                || idMath.Fabs(mat[1].z) > epsilon
                || idMath.Fabs(mat[2].x) > epsilon
                || idMath.Fabs(mat[2].y) > epsilon) {
            return false;
        }
        return true;
    }

    public boolean IsRotated() {
        return !Compare(mat3_identity);
    }

    public void ProjectVector(final idVec3 src, idVec3 dst) {
        dst.x = mat[0].oMultiply(src);
        dst.y = mat[1].oMultiply(src);
        dst.z = mat[2].oMultiply(src);
    }

    public void UnprojectVector(final idVec3 src, idVec3 dst) {
        dst.oSet(mat[0].oMultiply(src.x).oPlus(
                mat[1].oMultiply(src.y).oPlus(
                        mat[2].oMultiply(src.z))));
    }

    public boolean FixDegeneracies() {// fix degenerate axial cases
        boolean r = mat[0].FixDegenerateNormal();
        r |= mat[1].FixDegenerateNormal();
        r |= mat[2].FixDegenerateNormal();
        return r;
    }

    public boolean FixDenormals() {// change tiny numbers to zero
        boolean r = mat[0].FixDenormals();
        r |= mat[1].FixDenormals();
        r |= mat[2].FixDenormals();
        return r;
    }

    public float Trace() {
        return (mat[0].x + mat[1].y + mat[2].z);
    }

    public float Determinant() {

        float det2_12_01 = mat[1].x * mat[2].y - mat[1].y * mat[2].x;
        float det2_12_02 = mat[1].x * mat[2].z - mat[1].z * mat[2].x;
        float det2_12_12 = mat[1].y * mat[2].z - mat[1].z * mat[2].y;

        return mat[0].x * det2_12_12 - mat[0].y * det2_12_02 + mat[0].z * det2_12_01;
    }

    public idMat3 OrthoNormalize() {
        idMat3 ortho;

        ortho = this;
        ortho.mat[0].Normalize();
        ortho.mat[2].Cross(mat[0], mat[1]);
        ortho.mat[2].Normalize();
        ortho.mat[1].Cross(mat[2], mat[0]);
        ortho.mat[1].Normalize();
        return ortho;
    }

    public idMat3 OrthoNormalizeSelf() {
        mat[0].Normalize();
        mat[2].Cross(mat[0], mat[1]);
        mat[2].Normalize();
        mat[1].Cross(mat[2], mat[0]);
        mat[1].Normalize();
        return this;
    }

    public idMat3 Transpose() {// returns transpose
        return new idMat3(
                mat[0].x, mat[1].x, mat[2].x,
                mat[0].y, mat[1].y, mat[2].y,
                mat[0].z, mat[1].z, mat[2].z);
    }

    public idMat3 TransposeSelf() {
        float tmp0, tmp1, tmp2;

        tmp0 = mat[0].x;
        mat[0].y = mat[1].x;
        mat[1].x = tmp0;
        tmp1 = mat[0].z;
        mat[0].z = mat[2].x;
        mat[2].x = tmp1;
        tmp2 = mat[1].z;
        mat[1].z = mat[2].y;
        mat[2].y = tmp2;

        return this;
    }

    public idMat3 Inverse() {// returns the inverse ( m * m.Inverse() = identity )
        idMat3 invMat;

        invMat = this;
        boolean r = invMat.InverseSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseSelf() {// returns false if determinant is zero
        // 18+3+9 = 30 multiplications
        //			 1 division
        idMat3 inverse = new idMat3();
        float det, invDet;

        inverse.mat[0].x = mat[1].y * mat[2].z - mat[1].z * mat[2].y;
        inverse.mat[1].x = mat[1].z * mat[2].x - mat[1].x * mat[2].z;
        inverse.mat[2].x = mat[1].x * mat[2].y - mat[1].y * mat[2].x;

        det = mat[0].x * inverse.mat[0].x + mat[0].y * inverse.mat[1].x + mat[0].z * inverse.mat[2].x;

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        inverse.mat[0].y = mat[0].z * mat[2].y - mat[0].y * mat[2].z;
        inverse.mat[0].z = mat[0].y * mat[1].z - mat[0].z * mat[1].y;
        inverse.mat[1].y = mat[0].x * mat[2].z - mat[0].z * mat[2].x;
        inverse.mat[1].z = mat[0].z * mat[1].x - mat[0].x * mat[1].z;
        inverse.mat[2].y = mat[0].y * mat[2].x - mat[0].x * mat[2].y;
        inverse.mat[2].z = mat[0].x * mat[1].y - mat[0].y * mat[1].x;

        mat[0].x = inverse.mat[0].x * invDet;
        mat[0].y = inverse.mat[0].y * invDet;
        mat[0].z = inverse.mat[0].z * invDet;

        mat[1].x = inverse.mat[1].x * invDet;
        mat[1].y = inverse.mat[1].y * invDet;
        mat[1].z = inverse.mat[1].z * invDet;

        mat[2].x = inverse.mat[2].x * invDet;
        mat[2].y = inverse.mat[2].y * invDet;
        mat[2].z = inverse.mat[2].z * invDet;

        return true;
    }

    public idMat3 InverseFast() {// returns the inverse ( m * m.Inverse() = identity )
        idMat3 invMat;

        invMat = this;
        boolean r = invMat.InverseFastSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseFastSelf()// returns false if determinant is zero
    {//TODO://#if 1
        return InverseSelf();
    }

    public idMat3 TransposeMultiply(final idMat3 b) {
        return new idMat3(
                mat[0].x * b.mat[0].x + mat[1].x * b.mat[1].x + mat[2].x * b.mat[2].x,
                mat[0].x * b.mat[0].y + mat[1].x * b.mat[1].y + mat[2].x * b.mat[2].y,
                mat[0].x * b.mat[0].z + mat[1].x * b.mat[1].z + mat[2].x * b.mat[2].z,
                mat[0].y * b.mat[0].x + mat[1].y * b.mat[1].x + mat[2].y * b.mat[2].x,
                mat[0].y * b.mat[0].y + mat[1].y * b.mat[1].y + mat[2].y * b.mat[2].y,
                mat[0].y * b.mat[0].z + mat[1].y * b.mat[1].z + mat[2].y * b.mat[2].z,
                mat[0].z * b.mat[0].x + mat[1].z * b.mat[1].x + mat[2].z * b.mat[2].x,
                mat[0].z * b.mat[0].y + mat[1].z * b.mat[1].y + mat[2].z * b.mat[2].y,
                mat[0].z * b.mat[0].z + mat[1].z * b.mat[1].z + mat[2].z * b.mat[2].z);
    }
//

    public idMat3 InertiaTranslate(final float mass, final idVec3 centerOfMass, final idVec3 translation) {
        idMat3 m = new idMat3();
        idVec3 newCenter;

        newCenter = centerOfMass.oPlus(translation);

        m.mat[0].x = mass * ((centerOfMass.y * centerOfMass.y + centerOfMass.z * centerOfMass.z)
                - (newCenter.y * newCenter.y + newCenter.z * newCenter.z));
        m.mat[1].y = mass * ((centerOfMass.x * centerOfMass.x + centerOfMass.z * centerOfMass.z)
                - (newCenter.x * newCenter.x + newCenter.z * newCenter.z));
        m.mat[2].z = mass * ((centerOfMass.x * centerOfMass.x + centerOfMass.y * centerOfMass.y)
                - (newCenter.x * newCenter.x + newCenter.y * newCenter.y));

        m.mat[0].y = m.mat[1].x = mass * (newCenter.x * newCenter.y - centerOfMass.x * centerOfMass.y);
        m.mat[1].z = m.mat[2].y = mass * (newCenter.y * newCenter.z - centerOfMass.y * centerOfMass.z);
        m.mat[0].z = m.mat[2].x = mass * (newCenter.x * newCenter.z - centerOfMass.x * centerOfMass.z);

        return this.oPlus(m);
    }

    public idMat3 InertiaTranslateSelf(final float mass, final idVec3 centerOfMass, final idVec3 translation) {
        idMat3 m = new idMat3();
        idVec3 newCenter;

        newCenter = centerOfMass.oPlus(translation);

        m.mat[0].x = mass * ((centerOfMass.y * centerOfMass.y + centerOfMass.z * centerOfMass.z)
                - (newCenter.y * newCenter.y + newCenter.z * newCenter.z));
        m.mat[1].y = mass * ((centerOfMass.x * centerOfMass.x + centerOfMass.z * centerOfMass.z)
                - (newCenter.x * newCenter.x + newCenter.z * newCenter.z));
        m.mat[2].z = mass * ((centerOfMass.x * centerOfMass.x + centerOfMass.y * centerOfMass.y)
                - (newCenter.x * newCenter.x + newCenter.y * newCenter.y));

        m.mat[0].y = m.mat[1].x = mass * (newCenter.x * newCenter.y - centerOfMass.x * centerOfMass.y);
        m.mat[1].z = m.mat[2].y = mass * (newCenter.y * newCenter.z - centerOfMass.y * centerOfMass.z);
        m.mat[0].z = m.mat[2].x = mass * (newCenter.x * newCenter.z - centerOfMass.x * centerOfMass.z);

        return this.oPluSet(m);
    }

    public idMat3 InertiaRotate(final idMat3 rotation) {
        // NOTE: the rotation matrix is stored column-major
//            return rotation.Transpose() * (*this) * rotation;
        return rotation.Transpose().oMultiply(this).oMultiply(rotation);
    }

    public idMat3 InertiaRotateSelf(final idMat3 rotation) {
        // NOTE: the rotation matrix is stored column-major
//	*this = rotation.Transpose() * (*this) * rotation;
        this.oSet(rotation.Transpose().oMultiply(this).oMultiply(rotation));
        return this;
    }

    public final int GetDimension() {
        return 9;
    }

    public idAngles ToAngles() {
        idAngles angles = new idAngles();
        double theta;
        double cp;
        float sp;

        sp = mat[0].z;

        // cap off our sin value so that we don't get any NANs
        if (sp > 1.0f) {
            sp = 1.0f;
        } else if (sp < -1.0f) {
            sp = -1.0f;
        }

        theta = -Math.asin(sp);
        cp = Math.cos(theta);

        if (cp > 8192.0f * idMath.FLT_EPSILON) {
            angles.pitch = (float) RAD2DEG(theta);
            angles.yaw = (float) RAD2DEG(Math.atan2(mat[0].y, mat[0].x));
            angles.roll = (float) RAD2DEG(Math.atan2(mat[1].z, mat[2].z));
        } else {
            angles.pitch = (float) RAD2DEG(theta);
            angles.yaw = (float) RAD2DEG(-Math.atan2(mat[1].x, mat[1].y));
            angles.roll = 0;
        }
        return angles;
    }

    public idQuat ToQuat() {
        idQuat q = new idQuat();
        float trace;
        float s;
        float t;
        int i;
        int j;
        int k;

        int next[] = {1, 2, 0};

//	trace = mat[0 ][0 ] + mat[1 ][1 ] + mat[2 ][2 ];
        trace = this.Trace();

        if (trace > 0.0f) {

            t = trace + 1.0f;
            s = idMath.InvSqrt(t) * 0.5f;

            q.oSet(3, s * t);
            q.oSet(0, (mat[2].y - mat[1].z) * s);
            q.oSet(1, (mat[0].z - mat[2].x) * s);
            q.oSet(2, (mat[1].x - mat[0].y) * s);

        } else {

            i = 0;
            if (mat[1].y > mat[0].x) {
                i = 1;
            }
            if (mat[2].z > mat[i].oGet(i)) {
                i = 2;
            }
            j = next[i];
            k = next[j];

            t = (mat[i].oGet(i) - (mat[j].oGet(j) + mat[k].oGet(k))) + 1.0f;
            s = idMath.InvSqrt(t) * 0.5f;

            q.oSet(i, s * t);
            q.oSet(3, (mat[k].oGet(j) - mat[j].oGet(k)) * s);
            q.oSet(j, (mat[j].oGet(i) + mat[i].oGet(j)) * s);
            q.oSet(k, (mat[k].oGet(i) + mat[i].oGet(k)) * s);
        }
        return q;
    }

    public idCQuat ToCQuat() {
        idQuat q = ToQuat();
        if (q.w < 0.0f) {
            return new idCQuat(-q.x, -q.y, -q.z);
        }
        return new idCQuat(q.x, q.y, q.z);
    }

    public idRotation ToRotation() {
        idRotation r = new idRotation();
        float trace;
        float s;
        float t;
        int i;
        int j;
        int k;
        final int[] next = {1, 2, 0};

        trace = mat[0].x + mat[1].y + mat[2].z;
        if (trace > 0.0f) {

            t = trace + 1.0f;
            s = idMath.InvSqrt(t) * 0.5f;

            r.angle = s * t;
            r.vec.oSet(0, (mat[2].y - mat[1].z) * s);
            r.vec.oSet(1, (mat[0].z - mat[2].x) * s);
            r.vec.oSet(2, (mat[1].x - mat[0].y) * s);

        } else {

            i = 0;
            if (mat[1].y > mat[0].x) {
                i = 1;
            }
            if (mat[2].z > mat[i].oGet(i)) {
                i = 2;
            }
            j = next[i];
            k = next[j];

            t = (mat[i].oGet(i) - (mat[j].oGet(j) + mat[k].oGet(k))) + 1.0f;
            s = idMath.InvSqrt(t) * 0.5f;

            r.vec.oSet(i, s * t);
            r.angle = (mat[k].oGet(j) - mat[j].oGet(k)) * s;
            r.vec.oSet(j, (mat[j].oGet(i) + mat[i].oGet(j)) * s);
            r.vec.oSet(k, (mat[k].oGet(i) + mat[i].oGet(k)) * s);
        }
        r.angle = idMath.ACos(r.angle);
        if (idMath.Fabs(r.angle) < 1e-10f) {
            r.vec.Set(0.0f, 0.0f, 1.0f);
            r.angle = 0.0f;
        } else {
            //vec *= (1.0f / sin( angle ));
            r.vec.Normalize();
            r.vec.FixDegenerateNormal();
            r.angle *= 2.0f * idMath.M_RAD2DEG;
        }

        r.origin.Zero();
        r.axis = this;
        r.axisValid = true;
        return r;
    }

    public idMat4 ToMat4() {
        // NOTE: idMat3 is transposed because it is column-major
        return new idMat4(mat[0].x, mat[1].x, mat[2].x, 0.0f,
                mat[0].y, mat[1].y, mat[2].y, 0.0f,
                mat[0].z, mat[1].z, mat[2].z, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f);
    }

    public idVec3 ToAngularVelocity() {
        idRotation rotation = ToRotation();
        return rotation.GetVec().oMultiply((float) DEG2RAD(rotation.GetAngle()));
    }

    /**
     * Read-only array.
     */
    public float[] ToFloatPtr() {
        return new float[]{
                mat[0].x, mat[0].y, mat[0].z,
                mat[1].x, mat[1].y, mat[1].z,
                mat[2].x, mat[2].y, mat[2].z
        };
    }
    //	public	float *			ToFloatPtr( void );

    public String ToString() {
        return ToString(2);
    }

    public String ToString(int precision) {
        return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
    }
//

    public static void TransposeMultiply(final idMat3 transpose, final idMat3 b, idMat3 dst) {
        dst.mat[0].x = transpose.mat[0].x * b.mat[0].x + transpose.mat[1].x * b.mat[1].x + transpose.mat[2].x * b.mat[2].x;
        dst.mat[0].y = transpose.mat[0].x * b.mat[0].y + transpose.mat[1].x * b.mat[1].y + transpose.mat[2].x * b.mat[2].y;
        dst.mat[0].z = transpose.mat[0].x * b.mat[0].z + transpose.mat[1].x * b.mat[1].z + transpose.mat[2].x * b.mat[2].z;
        dst.mat[1].x = transpose.mat[0].y * b.mat[0].x + transpose.mat[1].y * b.mat[1].x + transpose.mat[2].y * b.mat[2].x;
        dst.mat[1].y = transpose.mat[0].y * b.mat[0].y + transpose.mat[1].y * b.mat[1].y + transpose.mat[2].y * b.mat[2].y;
        dst.mat[1].z = transpose.mat[0].y * b.mat[0].z + transpose.mat[1].y * b.mat[1].z + transpose.mat[2].y * b.mat[2].z;
        dst.mat[2].x = transpose.mat[0].z * b.mat[0].x + transpose.mat[1].z * b.mat[1].x + transpose.mat[2].z * b.mat[2].x;
        dst.mat[2].y = transpose.mat[0].z * b.mat[0].y + transpose.mat[1].z * b.mat[1].y + transpose.mat[2].z * b.mat[2].y;
        dst.mat[2].z = transpose.mat[0].z * b.mat[0].z + transpose.mat[1].z * b.mat[1].z + transpose.mat[2].z * b.mat[2].z;
    }

    public static idMat3 SkewSymmetric(final idVec3 src) {
        return new idMat3(0.0f, -src.z, src.y, src.z, 0.0f, -src.x, -src.y, src.x, 0.0f);
    }

    public idVec3 getRow(final int row) {
        return mat[row];
    }

    @Deprecated
    public void setRow(final int rowNumber, final idVec3 row) {
        mat[rowNumber] = row;
    }

    @Deprecated
    public void setRow(final int rowNumber, final float x, final float y, final float z) {
        mat[rowNumber] = new idVec3(x, y, z);
    }

    public float oSet(final int x, final int y, final float value) {
        switch (y) {
//                case 0:
            default:
                return mat[x].x = value;
            case 1:
                return mat[x].y = value;
            case 2:
                return mat[x].z = value;
        }
    }

    public idMat3 oSet(final idMat3 m) {
        this.mat[0].oSet(m.mat[0]);
        this.mat[1].oSet(m.mat[1]);
        this.mat[2].oSet(m.mat[2]);
        return this;
    }

    public void oMinSet(final int x, final int y, final float value) {
        switch (y) {
            case 0:
                mat[x].x -= value;
                break;
            case 1:
                mat[x].y -= value;
                break;
            case 2:
                mat[x].z -= value;
                break;
        }
    }

    public void oPluSet(final int x, final int y, final float value) {
        switch (y) {
            case 0:
                mat[x].x -= value;
                break;
            case 1:
                mat[x].y -= value;
                break;
            case 2:
                mat[x].z -= value;
                break;
        }
    }

    public idMat3 oPluSet(final idMat3 a) {
        this.mat[0].x += a.mat[0].x;
        this.mat[0].y += a.mat[0].y;
        this.mat[0].z += a.mat[0].z;
        //
        this.mat[1].x += a.mat[1].x;
        this.mat[1].y += a.mat[1].y;
        this.mat[1].z += a.mat[1].z;
        //
        this.mat[2].x += a.mat[2].x;
        this.mat[2].y += a.mat[2].y;
        this.mat[2].z += a.mat[2].z;
        return this;
    }

    float[] reinterpret_cast() {
        final int size = 3;

        float[] temp = new float[size * size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                temp[x * size + y] = this.mat[x].oGet(y);
            }
        }
        return temp;
    }

    @Override
    public String toString() {
        return "\n"
                + mat[0] + ",\n"
                + mat[1] + ",\n"
                + mat[2];
    }
};
