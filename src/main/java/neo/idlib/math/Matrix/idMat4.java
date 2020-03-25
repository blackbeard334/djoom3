package neo.idlib.math.Matrix;

import static neo.idlib.math.Matrix.idMat0.MATRIX_EPSILON;
import static neo.idlib.math.Matrix.idMat0.MATRIX_INVERSE_EPSILON;

import java.util.Arrays;

import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

//===============================================================
//
//	idMat4 - 4x4 matrix
//
//===============================================================
public class idMat4 {
    private static final idMat4 mat4_zero = new idMat4(new idVec4(0, 0, 0, 0), new idVec4(0, 0, 0, 0), new idVec4(0, 0, 0, 0), new idVec4(0, 0, 0, 0));
    private static final idMat4 mat4_identity = new idMat4(new idVec4(1, 0, 0, 0), new idVec4(0, 1, 0, 0), new idVec4(0, 0, 1, 0), new idVec4(0, 0, 0, 1));

    public static idMat4 getMat4_zero() {
        return new idMat4(mat4_zero);
    }

    public static idMat4 getMat4_identity() {
        return new idMat4(mat4_identity);
    }
    
    

    private final idVec4[] mat = {new idVec4(), new idVec4(), new idVec4(), new idVec4()};

    public idMat4() {
    }

    public idMat4(final idVec4 x, final idVec4 y, final idVec4 z, final idVec4 w) {
        this.mat[0].oSet(x);
        this.mat[1].oSet(y);
        this.mat[2].oSet(z);
        this.mat[3].oSet(w);
    }

    public idMat4(final float xx, final float xy, final float xz, final float xw,
            final float yx, final float yy, final float yz, final float yw,
            final float zx, final float zy, final float zz, final float zw,
            final float wx, final float wy, final float wz, final float ww) {
        this.mat[0].x = xx;
        this.mat[0].y = xy;
        this.mat[0].z = xz;
        this.mat[0].w = xw;
        //
        this.mat[1].x = yx;
        this.mat[1].y = yy;
        this.mat[1].z = yz;
        this.mat[1].w = yw;
        //
        this.mat[2].x = zx;
        this.mat[2].y = zy;
        this.mat[2].z = zz;
        this.mat[2].w = zw;
        //
        this.mat[3].x = wx;
        this.mat[3].y = wy;
        this.mat[3].z = wz;
        this.mat[3].w = ww;
    }

    public idMat4(final idMat3 rotation, final idVec3 translation) {
        // NOTE: idMat3 is transposed because it is column-major
        this.mat[0].x = rotation.mat[0].x;
        this.mat[0].y = rotation.mat[1].x;
        this.mat[0].z = rotation.mat[2].x;
        this.mat[0].w = translation.x;
        this.mat[1].x = rotation.mat[0].y;
        this.mat[1].y = rotation.mat[1].y;
        this.mat[1].z = rotation.mat[2].y;
        this.mat[1].w = translation.y;
        this.mat[2].x = rotation.mat[0].z;
        this.mat[2].y = rotation.mat[1].z;
        this.mat[2].z = rotation.mat[2].z;
        this.mat[2].w = translation.z;
        this.mat[3].x = 0.0f;
        this.mat[3].y = 0.0f;
        this.mat[3].z = 0.0f;
        this.mat[3].w = 1.0f;
    }

    public idMat4(final float src[][]) {
//	memcpy( mat, src, 4 * 4 * sizeof( float ) );
        this.mat[0].x = src[0][0];
        this.mat[0].y = src[0][1];
        this.mat[0].z = src[0][2];
        this.mat[0].w = src[0][3];
        //
        this.mat[1].x = src[1][0];
        this.mat[1].y = src[1][1];
        this.mat[1].z = src[1][2];
        this.mat[1].w = src[1][3];
        //
        this.mat[2].x = src[2][0];
        this.mat[2].y = src[2][1];
        this.mat[2].z = src[2][2];
        this.mat[2].w = src[2][3];
        //
        this.mat[3].x = src[3][0];
        this.mat[3].y = src[3][1];
        this.mat[3].z = src[3][2];
        this.mat[3].w = src[3][3];
    }

    public idMat4(final idMat4 m) {
        this.oSet(m);
    }

//public	const idVec4 &	operator[]( int index ) const;
    public idVec4 oGet(final int index) {
        return this.mat[index];
    }

    public idVec4 oSet(final int index, final idVec4 value) {
        return this.mat[index] = value;
    }

    public float oSet(final int index1, final int index2, final float value) {
        return this.mat[index1].oSet(index2, value);
    }
//public	idVec4 &		operator[]( int index );
//public	idMat4			operator*( const float a ) const;

    public idMat4 oMultiply(final float a) {
        return new idMat4(
                this.mat[0].x * a, this.mat[0].y * a, this.mat[0].z * a, this.mat[0].w * a,
                this.mat[1].x * a, this.mat[1].y * a, this.mat[1].z * a, this.mat[1].w * a,
                this.mat[2].x * a, this.mat[2].y * a, this.mat[2].z * a, this.mat[2].w * a,
                this.mat[3].x * a, this.mat[3].y * a, this.mat[3].z * a, this.mat[3].w * a);
    }
//public	idVec4			operator*( const idVec4 &vec ) const;

    public idVec4 oMultiply(final idVec4 vec) {
        return new idVec4(
                (this.mat[0].x * vec.x) + (this.mat[0].y * vec.y) + (this.mat[0].z * vec.z) + (this.mat[0].w * vec.w),
                (this.mat[1].x * vec.x) + (this.mat[1].y * vec.y) + (this.mat[1].z * vec.z) + (this.mat[1].w * vec.w),
                (this.mat[2].x * vec.x) + (this.mat[2].y * vec.y) + (this.mat[2].z * vec.z) + (this.mat[2].w * vec.w),
                (this.mat[3].x * vec.x) + (this.mat[3].y * vec.y) + (this.mat[3].z * vec.z) + (this.mat[3].w * vec.w));
    }
//public	idVec3			operator*( const idVec3 &vec ) const;

    public idVec3 oMultiply(final idVec3 vec) {
        final float s = (this.mat[3].x * vec.x) + (this.mat[3].y * vec.y) + (this.mat[3].z * vec.z) + this.mat[3].w;
        if (s == 0.0f) {
            return new idVec3(0.0f, 0.0f, 0.0f);
        }
        if (s == 1.0f) {
            return new idVec3(
                    (this.mat[0].x * vec.x) + (this.mat[0].y * vec.y) + (this.mat[0].z * vec.z) + this.mat[0].w,
                    (this.mat[1].x * vec.x) + (this.mat[1].y * vec.y) + (this.mat[1].z * vec.z) + this.mat[1].w,
                    (this.mat[2].x * vec.x) + (this.mat[2].y * vec.y) + (this.mat[2].z * vec.z) + this.mat[2].w);
        } else {
            final float invS = 1.0f / s;
            return new idVec3(
                    ((this.mat[0].x * vec.x) + (this.mat[0].y * vec.y) + (this.mat[0].z * vec.z) + this.mat[0].w) * invS,
                    ((this.mat[1].x * vec.x) + (this.mat[1].y * vec.y) + (this.mat[1].z * vec.z) + this.mat[1].w) * invS,
                    ((this.mat[2].x * vec.x) + (this.mat[2].y * vec.y) + (this.mat[2].z * vec.z) + this.mat[2].w) * invS);
        }
    }
//public	idMat4			operator*( const idMat4 &a ) const;

    public idMat4 oMultiply(final idMat4 a) {
        int i, j;
        final idMat4 dst = new idMat4();

        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                final float value = this.mat[0].oMultiply(a.mat[(0 * 4) + j])
                        + this.mat[1].oMultiply(a.mat[(1 * 4) + j])
                        + this.mat[2].oMultiply(a.mat[(2 * 4) + j])
                        + this.mat[2].oMultiply(a.mat[(3 * 4) + j]);
                dst.setCell(i, j, value);
            }
        }
        return dst;
    }
//public	idMat4			operator+( const idMat4 &a ) const;

    public idMat4 oPlus(final idMat4 a) {
        return new idMat4(
                this.mat[0].x + a.mat[0].x, this.mat[0].y + a.mat[0].y, this.mat[0].z + a.mat[0].z, this.mat[0].w + a.mat[0].w,
                this.mat[1].x + a.mat[1].x, this.mat[1].y + a.mat[1].y, this.mat[1].z + a.mat[1].z, this.mat[1].w + a.mat[1].w,
                this.mat[2].x + a.mat[2].x, this.mat[2].y + a.mat[2].y, this.mat[2].z + a.mat[2].z, this.mat[2].w + a.mat[2].w,
                this.mat[3].x + a.mat[3].x, this.mat[3].y + a.mat[3].y, this.mat[3].z + a.mat[3].z, this.mat[3].w + a.mat[3].w);
    }
//public	idMat4			operator-( const idMat4 &a ) const;

    public idMat4 oMinus(final idMat4 a) {
        return new idMat4(
                this.mat[0].x - a.mat[0].x, this.mat[0].y - a.mat[0].y, this.mat[0].z - a.mat[0].z, this.mat[0].w - a.mat[0].w,
                this.mat[1].x - a.mat[1].x, this.mat[1].y - a.mat[1].y, this.mat[1].z - a.mat[1].z, this.mat[1].w - a.mat[1].w,
                this.mat[2].x - a.mat[2].x, this.mat[2].y - a.mat[2].y, this.mat[2].z - a.mat[2].z, this.mat[2].w - a.mat[2].w,
                this.mat[3].x - a.mat[3].x, this.mat[3].y - a.mat[3].y, this.mat[3].z - a.mat[3].z, this.mat[3].w - a.mat[3].w);
    }
//public	idMat4 &		operator*=( const float a );

    public idMat4 oMulSet(final float a) {
        this.mat[0].x *= a;
        this.mat[0].y *= a;
        this.mat[0].z *= a;
        this.mat[0].w *= a;
        //
        this.mat[1].x *= a;
        this.mat[1].y *= a;
        this.mat[1].z *= a;
        this.mat[1].w *= a;
        //
        this.mat[2].x *= a;
        this.mat[2].y *= a;
        this.mat[2].z *= a;
        this.mat[2].w *= a;
        //
        this.mat[3].x *= a;
        this.mat[3].y *= a;
        this.mat[3].z *= a;
        this.mat[3].w *= a;
        return this;
    }
//public	idMat4 &		operator*=( const idMat4 &a );

    public idMat4 oMultSet(final idMat4 a) {
        int i, j;
        final idMat4 dst = new idMat4();

        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                final float value = this.mat[0].oMultiply(a.mat[(0 * 4) + j])
                        + this.mat[1].oMultiply(a.mat[(1 * 4) + j])
                        + this.mat[2].oMultiply(a.mat[(2 * 4) + j])
                        + this.mat[2].oMultiply(a.mat[(3 * 4) + j]);
                dst.setCell(i, j, value);
            }
        }
        return this;
    }
//public	idMat4 &		operator+=( const idMat4 &a );

    public idMat4 oPluSet(final idMat4 a) {
        this.mat[0].x += a.mat[0].x;
        this.mat[0].y += a.mat[0].y;
        this.mat[0].z += a.mat[0].z;
        this.mat[0].w += a.mat[0].w;
        //
        this.mat[1].x += a.mat[1].x;
        this.mat[1].y += a.mat[1].y;
        this.mat[1].z += a.mat[1].z;
        this.mat[1].w += a.mat[1].w;
        //
        this.mat[2].x += a.mat[2].x;
        this.mat[2].y += a.mat[2].y;
        this.mat[2].z += a.mat[2].z;
        this.mat[2].w += a.mat[2].w;
        //
        this.mat[3].x += a.mat[3].x;
        this.mat[3].y += a.mat[3].y;
        this.mat[3].z += a.mat[3].z;
        this.mat[3].w += a.mat[3].w;
        return this;
    }

//public	idMat4 &		operator-=( const idMat4 &a );
    public idMat4 oMinSet(final idMat4 a) {
        this.mat[0].x -= a.mat[0].x;
        this.mat[0].y -= a.mat[0].y;
        this.mat[0].z -= a.mat[0].z;
        this.mat[0].w -= a.mat[0].w;
        //
        this.mat[1].x -= a.mat[1].x;
        this.mat[1].y -= a.mat[1].y;
        this.mat[1].z -= a.mat[1].z;
        this.mat[1].w -= a.mat[1].w;
        //
        this.mat[2].x -= a.mat[2].x;
        this.mat[2].y -= a.mat[2].y;
        this.mat[2].z -= a.mat[2].z;
        this.mat[2].w -= a.mat[2].w;
        //
        this.mat[3].x -= a.mat[3].x;
        this.mat[3].y -= a.mat[3].y;
        this.mat[3].z -= a.mat[3].z;
        this.mat[3].w -= a.mat[3].w;
        return this;
    }

//public	friend idMat4	operator*( const float a, const idMat4 &mat );
    public static idMat4 oMultiply(final float a, final idMat4 mat) {
        return mat.oMultiply(a);
    }

//public	friend idVec4	operator*( const idVec4 &vec, const idMat4 &mat );
    public static idVec4 oMultiply(final idVec4 vec, final idMat4 mat) {
        return mat.oMultiply(vec);
    }
//public	friend idVec3	operator*( const idVec3 &vec, const idMat4 &mat );

    public static idVec3 oMultiply(final idVec3 vec, final idMat4 mat) {
        return mat.oMultiply(vec);
    }
//public	friend idVec4 &	operator*=( idVec4 &vec, const idMat4 &mat );

    public static idVec4 oMulSet(idVec4 vec, final idMat4 mat) {
        vec.oSet(mat.oMultiply(vec));
        return vec;
    }
//public	friend idVec3 &	operator*=( idVec3 &vec, const idMat4 &mat );

    public static idVec3 oMulSet(idVec3 vec, final idMat4 mat) {
        vec.oSet(mat.oMultiply(vec));
        return vec;
    }

    public boolean Compare(final idMat4 a) {// exact compare, no epsilon
        int i, j;
        final idVec4[] ptr1 = this.mat, ptr2 = a.mat;

        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                if (ptr1[i].oGet(j) != ptr2[i].oGet(j)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean Compare(final idMat4 a, final float epsilon) // compare with epsilon
    {
        int i, j;
        final idVec4[] ptr1 = this.mat, ptr2 = a.mat;

        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                if (idMath.Fabs(ptr1[i].oGet(j) - ptr2[i].oGet(j)) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }
//public	bool			operator==( const idMat4 &a ) const;					// exact compare, no epsilon
//public	bool			operator!=( const idMat4 &a ) const;					// exact compare, no epsilon

    @Override
    public int hashCode() {
        int hash = 3;
        hash = (89 * hash) + Arrays.deepHashCode(this.mat);
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
        final idMat4 other = (idMat4) obj;
        if (!Arrays.deepEquals(this.mat, other.mat)) {
            return false;
        }
        return true;
    }

    public void Zero() {
        this.oSet(getMat4_zero());
    }

    public void Identity() {
        this.oSet(getMat4_identity());
    }

    public boolean IsIdentity() {
        return IsIdentity((float) MATRIX_EPSILON);
    }

    public boolean IsIdentity(final float epsilon) {
        return Compare(getMat4_identity(), epsilon);
    }

    public boolean IsSymmetric() {
        return IsSymmetric((float) MATRIX_EPSILON);
    }

    public boolean IsSymmetric(final float epsilon) {
        for (int i = 1; i < 4; i++) {
            for (int j = 0; j < i; j++) {
                if (idMath.Fabs(this.mat[i].oGet(j) - this.mat[j].oGet(i)) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsDiagonal() {
        return IsDiagonal((float) MATRIX_EPSILON);
    }

    public boolean IsDiagonal(final float epsilon) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if ((i != j) && (idMath.Fabs(this.mat[i].oGet(j)) > epsilon)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsRotated() {
        if (0
                == (this.mat[0].oGet(1) + this.mat[0].oGet(2)
                + this.mat[1].oGet(0) + this.mat[1].oGet(2)
                + this.mat[2].oGet(0) + this.mat[2].oGet(1))) {
            return false;
        }
        return true;
    }

    public void ProjectVector(final idVec4 src, idVec4 dst) {
        dst.x = src.oMultiply(this.mat[0]);
        dst.y = src.oMultiply(this.mat[1]);
        dst.z = src.oMultiply(this.mat[2]);
        dst.w = src.oMultiply(this.mat[3]);
    }

    public void UnprojectVector(final idVec4 src, idVec4 dst) {
//	dst = mat[ 0 ] * src.x + mat[ 1 ] * src.y + mat[ 2 ] * src.z + mat[ 3 ] * src.w;
        dst.oSet(this.mat[0].oMultiply(src.x).oPlus(
                this.mat[1].oMultiply(src.y).oPlus(
                        this.mat[2].oMultiply(src.z).oPlus(
                                this.mat[3].oMultiply(src.w)))));
    }

    public float Trace() {
        return (this.mat[0].oGet(0) + this.mat[1].oGet(1) + this.mat[2].oGet(2) + this.mat[3].oGet(3));
    }

    public float Determinant() {

        // 2x2 sub-determinants
        final float det2_01_01 = (this.mat[0].oGet(0) * this.mat[1].oGet(1)) - (this.mat[0].oGet(1) * this.mat[1].oGet(0));
        final float det2_01_02 = (this.mat[0].oGet(0) * this.mat[1].oGet(2)) - (this.mat[0].oGet(2) * this.mat[1].oGet(0));
        final float det2_01_03 = (this.mat[0].oGet(0) * this.mat[1].oGet(3)) - (this.mat[0].oGet(3) * this.mat[1].oGet(0));
        final float det2_01_12 = (this.mat[0].oGet(1) * this.mat[1].oGet(2)) - (this.mat[0].oGet(2) * this.mat[1].oGet(1));
        final float det2_01_13 = (this.mat[0].oGet(1) * this.mat[1].oGet(3)) - (this.mat[0].oGet(3) * this.mat[1].oGet(1));
        final float det2_01_23 = (this.mat[0].oGet(2) * this.mat[1].oGet(3)) - (this.mat[0].oGet(3) * this.mat[1].oGet(2));

        // 3x3 sub-determinants
        final float det3_201_012 = ((this.mat[2].oGet(0) * det2_01_12) - (this.mat[2].oGet(1) * det2_01_02)) + (this.mat[2].oGet(2) * det2_01_01);
        final float det3_201_013 = ((this.mat[2].oGet(0) * det2_01_13) - (this.mat[2].oGet(1) * det2_01_03)) + (this.mat[2].oGet(3) * det2_01_01);
        final float det3_201_023 = ((this.mat[2].oGet(0) * det2_01_23) - (this.mat[2].oGet(2) * det2_01_03)) + (this.mat[2].oGet(3) * det2_01_02);
        final float det3_201_123 = ((this.mat[2].oGet(1) * det2_01_23) - (this.mat[2].oGet(2) * det2_01_13)) + (this.mat[2].oGet(3) * det2_01_12);

        return ((((-det3_201_123 * this.mat[3].oGet(0)) + (det3_201_023 * this.mat[3].oGet(1))) - (det3_201_013 * this.mat[3].oGet(2))) + (det3_201_012 * this.mat[3].oGet(3)));
    }

    public idMat4 Transpose() {// returns transpose
        final idMat4 transpose = new idMat4();
        int i, j;

        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                transpose.mat[i].oSet(j, this.mat[j].oGet(i));
            }
        }
        return transpose;
    }

    public idMat4 TransposeSelf() {
        float temp;
        int i, j;

        for (i = 0; i < 4; i++) {
            for (j = i + 1; j < 4; j++) {
                temp = this.mat[i].oGet(j);
                this.mat[i].oSet(j, this.mat[j].oGet(i));
                this.mat[j].oSet(i, temp);
            }
        }
        return this;
    }

    public idMat4 Inverse() {// returns the inverse ( m * m.Inverse() = identity )
        idMat4 invMat;

        invMat = this;
        final boolean r = invMat.InverseSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseSelf()// returns false if determinant is zero
    {
        // 84+4+16 = 104 multiplications
        //			   1 division
        float det, invDet;

        // 2x2 sub-determinants required to calculate 4x4 determinant
        final float det2_01_01 = (this.mat[0].x * this.mat[1].y) - (this.mat[0].y * this.mat[1].x);
        final float det2_01_02 = (this.mat[0].x * this.mat[1].z) - (this.mat[0].z * this.mat[1].x);
        final float det2_01_03 = (this.mat[0].x * this.mat[1].w) - (this.mat[0].w * this.mat[1].x);
        final float det2_01_12 = (this.mat[0].y * this.mat[1].z) - (this.mat[0].z * this.mat[1].y);
        final float det2_01_13 = (this.mat[0].y * this.mat[1].w) - (this.mat[0].w * this.mat[1].y);
        final float det2_01_23 = (this.mat[0].z * this.mat[1].w) - (this.mat[0].w * this.mat[1].z);

        // 3x3 sub-determinants required to calculate 4x4 determinant
        final float det3_201_012 = ((this.mat[2].x * det2_01_12) - (this.mat[2].y * det2_01_02)) + (this.mat[2].z * det2_01_01);
        final float det3_201_013 = ((this.mat[2].x * det2_01_13) - (this.mat[2].y * det2_01_03)) + (this.mat[2].w * det2_01_01);
        final float det3_201_023 = ((this.mat[2].x * det2_01_23) - (this.mat[2].z * det2_01_03)) + (this.mat[2].w * det2_01_02);
        final float det3_201_123 = ((this.mat[2].y * det2_01_23) - (this.mat[2].z * det2_01_13)) + (this.mat[2].w * det2_01_12);

        det = ((((-det3_201_123 * this.mat[3].x) + (det3_201_023 * this.mat[3].y)) - (det3_201_013 * this.mat[3].z)) + (det3_201_012 * this.mat[3].w));

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        // remaining 2x2 sub-determinants
        final float det2_03_01 = (this.mat[0].x * this.mat[3].y) - (this.mat[0].y * this.mat[3].x);
        final float det2_03_02 = (this.mat[0].x * this.mat[3].z) - (this.mat[0].z * this.mat[3].x);
        final float det2_03_03 = (this.mat[0].x * this.mat[3].w) - (this.mat[0].w * this.mat[3].x);
        final float det2_03_12 = (this.mat[0].y * this.mat[3].z) - (this.mat[0].z * this.mat[3].y);
        final float det2_03_13 = (this.mat[0].y * this.mat[3].w) - (this.mat[0].w * this.mat[3].y);
        final float det2_03_23 = (this.mat[0].z * this.mat[3].w) - (this.mat[0].w * this.mat[3].z);

        final float det2_13_01 = (this.mat[1].x * this.mat[3].y) - (this.mat[1].y * this.mat[3].x);
        final float det2_13_02 = (this.mat[1].x * this.mat[3].z) - (this.mat[1].z * this.mat[3].x);
        final float det2_13_03 = (this.mat[1].x * this.mat[3].w) - (this.mat[1].w * this.mat[3].x);
        final float det2_13_12 = (this.mat[1].y * this.mat[3].z) - (this.mat[1].z * this.mat[3].y);
        final float det2_13_13 = (this.mat[1].y * this.mat[3].w) - (this.mat[1].w * this.mat[3].y);
        final float det2_13_23 = (this.mat[1].z * this.mat[3].w) - (this.mat[1].w * this.mat[3].z);

        // remaining 3x3 sub-determinants
        final float det3_203_012 = ((this.mat[2].x * det2_03_12) - (this.mat[2].y * det2_03_02)) + (this.mat[2].z * det2_03_01);
        final float det3_203_013 = ((this.mat[2].x * det2_03_13) - (this.mat[2].y * det2_03_03)) + (this.mat[2].w * det2_03_01);
        final float det3_203_023 = ((this.mat[2].x * det2_03_23) - (this.mat[2].z * det2_03_03)) + (this.mat[2].w * det2_03_02);
        final float det3_203_123 = ((this.mat[2].y * det2_03_23) - (this.mat[2].z * det2_03_13)) + (this.mat[2].w * det2_03_12);

        final float det3_213_012 = ((this.mat[2].x * det2_13_12) - (this.mat[2].y * det2_13_02)) + (this.mat[2].z * det2_13_01);
        final float det3_213_013 = ((this.mat[2].x * det2_13_13) - (this.mat[2].y * det2_13_03)) + (this.mat[2].w * det2_13_01);
        final float det3_213_023 = ((this.mat[2].x * det2_13_23) - (this.mat[2].z * det2_13_03)) + (this.mat[2].w * det2_13_02);
        final float det3_213_123 = ((this.mat[2].y * det2_13_23) - (this.mat[2].z * det2_13_13)) + (this.mat[2].w * det2_13_12);

        final float det3_301_012 = ((this.mat[3].x * det2_01_12) - (this.mat[3].y * det2_01_02)) + (this.mat[3].z * det2_01_01);
        final float det3_301_013 = ((this.mat[3].x * det2_01_13) - (this.mat[3].y * det2_01_03)) + (this.mat[3].w * det2_01_01);
        final float det3_301_023 = ((this.mat[3].x * det2_01_23) - (this.mat[3].z * det2_01_03)) + (this.mat[3].w * det2_01_02);
        final float det3_301_123 = ((this.mat[3].y * det2_01_23) - (this.mat[3].z * det2_01_13)) + (this.mat[3].w * det2_01_12);

        this.mat[0].x = -det3_213_123 * invDet;
        this.mat[1].x = +det3_213_023 * invDet;
        this.mat[2].x = -det3_213_013 * invDet;
        this.mat[3].x = +det3_213_012 * invDet;

        this.mat[0].y = +det3_203_123 * invDet;
        this.mat[1].y = -det3_203_023 * invDet;
        this.mat[2].y = +det3_203_013 * invDet;
        this.mat[3].y = -det3_203_012 * invDet;

        this.mat[0].z = +det3_301_123 * invDet;
        this.mat[1].z = -det3_301_023 * invDet;
        this.mat[2].z = +det3_301_013 * invDet;
        this.mat[3].z = -det3_301_012 * invDet;

        this.mat[0].w = -det3_201_123 * invDet;
        this.mat[1].w = +det3_201_023 * invDet;
        this.mat[2].w = -det3_201_013 * invDet;
        this.mat[3].w = +det3_201_012 * invDet;

        return true;
    }

    public idMat4 InverseFast() {// returns the inverse ( m * m.Inverse() = identity )
        idMat4 invMat;

        invMat = this;
        final boolean r = invMat.InverseFastSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseFastSelf() // returns false if determinant is zero
    {
//    #else
        //	6*8+2*6 = 60 multiplications
        //		2*1 =  2 divisions
        final float[][] r0 = new float[2][2], r1 = new float[2][2], r2 = new float[2][2], r3 = new float[2][2];
        float a, det, invDet;

        // r0 = m0.Inverse();
        det = (this.mat[0].x * this.mat[1].y) - (this.mat[0].y * this.mat[1].x);

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        r0[0][0] = this.mat[1].y * invDet;
        r0[0][1] = -this.mat[0].y * invDet;
        r0[1][0] = -this.mat[1].x * invDet;
        r0[1][1] = this.mat[0].x * invDet;

        // r1 = r0 * m1;
        r1[0][0] = (r0[0][0] * this.mat[0].z) + (r0[0][1] * this.mat[1].z);
        r1[0][1] = (r0[0][0] * this.mat[0].w) + (r0[0][1] * this.mat[1].w);
        r1[1][0] = (r0[1][0] * this.mat[0].z) + (r0[1][1] * this.mat[1].z);
        r1[1][1] = (r0[1][0] * this.mat[0].w) + (r0[1][1] * this.mat[1].w);

        // r2 = m2 * r1;
        r2[0][0] = (this.mat[2].x * r1[0][0]) + (this.mat[2].y * r1[1][0]);
        r2[0][1] = (this.mat[2].x * r1[0][1]) + (this.mat[2].y * r1[1][1]);
        r2[1][0] = (this.mat[3].x * r1[0][0]) + (this.mat[3].y * r1[1][0]);
        r2[1][1] = (this.mat[3].x * r1[0][1]) + (this.mat[3].y * r1[1][1]);

        // r3 = r2 - m3;
        r3[0][0] = r2[0][0] - this.mat[2].z;
        r3[0][1] = r2[0][1] - this.mat[2].w;
        r3[1][0] = r2[1][0] - this.mat[3].z;
        r3[1][1] = r2[1][1] - this.mat[3].w;

        // r3.InverseSelf();
        det = (r3[0][0] * r3[1][1]) - (r3[0][1] * r3[1][0]);

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        a = r3[0][0];
        r3[0][0] = r3[1][1] * invDet;
        r3[0][1] = -r3[0][1] * invDet;
        r3[1][0] = -r3[1][0] * invDet;
        r3[1][1] = a * invDet;

        // r2 = m2 * r0;
        r2[0][0] = (this.mat[2].x * r0[0][0]) + (this.mat[2].y * r0[1][0]);
        r2[0][1] = (this.mat[2].x * r0[0][1]) + (this.mat[2].y * r0[1][1]);
        r2[1][0] = (this.mat[3].x * r0[0][0]) + (this.mat[3].y * r0[1][0]);
        r2[1][1] = (this.mat[3].x * r0[0][1]) + (this.mat[3].y * r0[1][1]);

        // m2 = r3 * r2;
        this.mat[2].x = (r3[0][0] * r2[0][0]) + (r3[0][1] * r2[1][0]);
        this.mat[2].y = (r3[0][0] * r2[0][1]) + (r3[0][1] * r2[1][1]);
        this.mat[3].x = (r3[1][0] * r2[0][0]) + (r3[1][1] * r2[1][0]);
        this.mat[3].y = (r3[1][0] * r2[0][1]) + (r3[1][1] * r2[1][1]);

        // m0 = r0 - r1 * m2;
        this.mat[0].x = r0[0][0] - (r1[0][0] * this.mat[2].x) - (r1[0][1] * this.mat[3].x);
        this.mat[0].y = r0[0][1] - (r1[0][0] * this.mat[2].y) - (r1[0][1] * this.mat[3].y);
        this.mat[1].x = r0[1][0] - (r1[1][0] * this.mat[2].x) - (r1[1][1] * this.mat[3].x);
        this.mat[1].y = r0[1][1] - (r1[1][0] * this.mat[2].y) - (r1[1][1] * this.mat[3].y);

        // m1 = r1 * r3;
        this.mat[0].z = (r1[0][0] * r3[0][0]) + (r1[0][1] * r3[1][0]);
        this.mat[0].w = (r1[0][0] * r3[0][1]) + (r1[0][1] * r3[1][1]);
        this.mat[1].z = (r1[1][0] * r3[0][0]) + (r1[1][1] * r3[1][0]);
        this.mat[1].w = (r1[1][0] * r3[0][1]) + (r1[1][1] * r3[1][1]);

        // m3 = -r3;
        this.mat[2].z = -r3[0][0];
        this.mat[2].w = -r3[0][1];
        this.mat[3].z = -r3[1][0];
        this.mat[3].w = -r3[1][1];

        return true;
    }
//public	idMat4			TransposeMultiply( const idMat4 &b ) const;

    public final int GetDimension() {
        return 16;
    }
//public	const float *	ToFloatPtr( void ) const;
//public	float *			ToFloatPtr( void );
//public	const char *	ToString( int precision = 2 ) const;

    private void setCell(final int x, final int y, final float value) {
        switch (y) {
            case 0:
                this.mat[x].x = value;
                break;
            case 1:
                this.mat[x].y = value;
                break;
            case 2:
                this.mat[x].z = value;
                break;
            case 3:
                this.mat[x].w = value;
                break;
        }
    }

    private void oSet(final idMat4 mat4) {
        this.mat[0].oSet(mat4.mat[0]);
        this.mat[1].oSet(mat4.mat[1]);
        this.mat[2].oSet(mat4.mat[2]);
        this.mat[3].oSet(mat4.mat[3]);
    }

    float[] reinterpret_cast() {
        final int size = 4;

        final float[] temp = new float[size * size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                temp[(x * size) + y] = this.mat[x].oGet(y);
            }
        }
        return temp;
    }
}
