package neo.idlib.math.Matrix;

import java.util.Arrays;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Matrix.idMat0.MATRIX_EPSILON;
import static neo.idlib.math.Matrix.idMat0.MATRIX_INVERSE_EPSILON;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec5;

public class idMat5 {
//===============================================================
//
//	idMat5 - 5x5 matrix
//
//===============================================================

    public static final idMat5 mat5_zero = new idMat5(new idVec5(0, 0, 0, 0, 0), new idVec5(0, 0, 0, 0, 0), new idVec5(0, 0, 0, 0, 0), new idVec5(0, 0, 0, 0, 0), new idVec5(0, 0, 0, 0, 0));
    public static final idMat5 mat5_identity = new idMat5(new idVec5(1, 0, 0, 0, 0), new idVec5(0, 1, 0, 0, 0), new idVec5(0, 0, 1, 0, 0), new idVec5(0, 0, 0, 1, 0), new idVec5(0, 0, 0, 0, 1));

    private final idVec5[] mat = {new idVec5(), new idVec5(), new idVec5(), new idVec5(), new idVec5()};

    public idMat5() {//TODO:remove empty default constructs?
    }

    public idMat5(final idVec5 v0, final idVec5 v1, final idVec5 v2, final idVec5 v3, final idVec5 v4) {
        mat[0] = v0;
        mat[1] = v1;
        mat[2] = v2;
        mat[3] = v3;
        mat[4] = v4;
    }

    public idMat5(final float src[][]) {
        mat[0] = new idVec5(src[0][0], src[0][1], src[0][2], src[0][3], src[0][4]);
        mat[1] = new idVec5(src[1][0], src[1][1], src[1][2], src[1][3], src[1][4]);
        mat[2] = new idVec5(src[2][0], src[2][1], src[2][2], src[2][3], src[2][4]);
        mat[3] = new idVec5(src[3][0], src[3][1], src[3][2], src[3][3], src[3][4]);
        mat[4] = new idVec5(src[4][0], src[4][1], src[4][2], src[4][3], src[4][4]);
    }

//	public const idVec5 &	operator[]( int index ) const;
//public	idVec5 &		operator[]( int index );
//public	idMat5			operator*( const float a ) const;
    public idMat5 oMultiply(final float a) {
        return new idMat5(
                new idVec5(mat[0].x * a, mat[0].y * a, mat[0].z * a, mat[0].s * a, mat[0].t * a),
                new idVec5(mat[1].x * a, mat[1].y * a, mat[1].z * a, mat[1].s * a, mat[1].t * a),
                new idVec5(mat[2].x * a, mat[2].y * a, mat[2].z * a, mat[2].s * a, mat[2].t * a),
                new idVec5(mat[3].x * a, mat[3].y * a, mat[3].z * a, mat[3].s * a, mat[3].t * a),
                new idVec5(mat[4].x * a, mat[4].y * a, mat[4].z * a, mat[4].s * a, mat[4].t * a));
    }
//public	idVec5			operator*( const idVec5 &vec ) const;

    public idVec5 oMultiply(final idVec5 vec) {
        return new idVec5(
                mat[0].x * vec.x + mat[0].y * vec.y + mat[0].z * vec.z + mat[0].s * vec.s + mat[0].t * vec.t,
                mat[1].x * vec.x + mat[1].y * vec.y + mat[1].z * vec.z + mat[1].s * vec.s + mat[1].t * vec.t,
                mat[2].x * vec.x + mat[2].y * vec.y + mat[2].z * vec.z + mat[2].s * vec.s + mat[2].t * vec.t,
                mat[3].x * vec.x + mat[3].y * vec.y + mat[3].z * vec.z + mat[3].s * vec.s + mat[3].t * vec.t,
                mat[4].x * vec.x + mat[4].y * vec.y + mat[4].z * vec.z + mat[4].s * vec.s + mat[4].t * vec.t);
    }
//public	idMat5			operator*( const idMat5 &a ) const;

    public idMat5 oMultiply(final idMat5 a) {
        int i, j;
        final float[] m1Ptr = this.reinterpret_cast(), m2Ptr = a.reinterpret_cast();
//	float *dstPtr;
        float[][] dst = new float[5][5];

//	m1Ptr = reinterpret_cast<const float *>(this);
//	m2Ptr = reinterpret_cast<const float *>(&a);
//	dstPtr = reinterpret_cast<float *>(&dst);
        for (i = 0; i < 5; i++) {
            for (j = 0; j < 5; j++) {
                dst[i][j] = m1Ptr[0] * m2Ptr[0 * 5 + j]
                        + m1Ptr[1] * m2Ptr[1 * 5 + j]
                        + m1Ptr[2] * m2Ptr[2 * 5 + j]
                        + m1Ptr[3] * m2Ptr[3 * 5 + j]
                        + m1Ptr[4] * m2Ptr[4 * 5 + j];
//			dstPtr++;
            }
//		m1Ptr += 5;
        }
        return new idMat5(dst);
    }
//public	idMat5			operator+( const idMat5 &a ) const;

    public idMat5 oPlus(final idMat5 a) {
        return new idMat5(
                new idVec5(mat[0].x + a.mat[0].x, mat[0].y + a.mat[0].y, mat[0].z + a.mat[0].z, mat[0].s + a.mat[0].s, mat[0].t + a.mat[0].t),
                new idVec5(mat[1].x + a.mat[1].x, mat[1].y + a.mat[1].y, mat[1].z + a.mat[1].z, mat[1].s + a.mat[1].s, mat[1].t + a.mat[1].t),
                new idVec5(mat[2].x + a.mat[2].x, mat[2].y + a.mat[2].y, mat[2].z + a.mat[2].z, mat[2].s + a.mat[2].s, mat[2].t + a.mat[2].t),
                new idVec5(mat[3].x + a.mat[3].x, mat[3].y + a.mat[3].y, mat[3].z + a.mat[3].z, mat[3].s + a.mat[3].s, mat[3].t + a.mat[3].t),
                new idVec5(mat[4].x + a.mat[4].x, mat[4].y + a.mat[4].y, mat[4].z + a.mat[4].z, mat[4].s + a.mat[4].s, mat[4].t + a.mat[4].t));
    }
//public	idMat5			operator-( const idMat5 &a ) const;

    public idMat5 oMinus(final idMat5 a) {
        return new idMat5(
                new idVec5(mat[0].x - a.mat[0].x, mat[0].y - a.mat[0].y, mat[0].z - a.mat[0].z, mat[0].s - a.mat[0].s, mat[0].t - a.mat[0].t),
                new idVec5(mat[1].x - a.mat[1].x, mat[1].y - a.mat[1].y, mat[1].z - a.mat[1].z, mat[1].s - a.mat[1].s, mat[1].t - a.mat[1].t),
                new idVec5(mat[2].x - a.mat[2].x, mat[2].y - a.mat[2].y, mat[2].z - a.mat[2].z, mat[2].s - a.mat[2].s, mat[2].t - a.mat[2].t),
                new idVec5(mat[3].x - a.mat[3].x, mat[3].y - a.mat[3].y, mat[3].z - a.mat[3].z, mat[3].s - a.mat[3].s, mat[3].t - a.mat[3].t),
                new idVec5(mat[4].x - a.mat[4].x, mat[4].y - a.mat[4].y, mat[4].z - a.mat[4].z, mat[4].s - a.mat[4].s, mat[4].t - a.mat[4].t));
    }
//public	idMat5 &		operator*=( const float a );

    public idMat5 oMulSet(final float a) {
        mat[0].x *= a;
        mat[0].y *= a;
        mat[0].z *= a;
        mat[0].s *= a;
        mat[0].t *= a;
        mat[1].x *= a;
        mat[1].y *= a;
        mat[1].z *= a;
        mat[1].s *= a;
        mat[1].t *= a;
        mat[2].x *= a;
        mat[2].y *= a;
        mat[2].z *= a;
        mat[2].s *= a;
        mat[2].t *= a;
        mat[3].x *= a;
        mat[3].y *= a;
        mat[3].z *= a;
        mat[3].s *= a;
        mat[3].t *= a;
        mat[4].x *= a;
        mat[4].y *= a;
        mat[4].z *= a;
        mat[4].s *= a;
        mat[4].t *= a;
        return this;
    }
//public	idMat5 &		operator*=( const idMat5 &a );

    public idMat5 oMulSet(final idMat5 a) {
        this.oSet(this.oMultiply(a));
        return this;
    }
//public	idMat5 &		operator+=( const idMat5 &a );

    public idMat5 oPluSet(final idMat5 a) {
        this.mat[0].x += a.mat[0].x;
        this.mat[0].y += a.mat[0].y;
        this.mat[0].z += a.mat[0].z;
        this.mat[0].s += a.mat[0].s;
        this.mat[0].t += a.mat[0].t;
        //
        this.mat[1].x += a.mat[1].x;
        this.mat[1].y += a.mat[1].y;
        this.mat[1].z += a.mat[1].z;
        this.mat[1].s += a.mat[1].s;
        this.mat[1].t += a.mat[1].t;
        //
        this.mat[2].x += a.mat[2].x;
        this.mat[2].y += a.mat[2].y;
        this.mat[2].z += a.mat[2].z;
        this.mat[2].s += a.mat[2].s;
        this.mat[2].t += a.mat[2].t;
        //
        this.mat[3].x += a.mat[3].x;
        this.mat[3].y += a.mat[3].y;
        this.mat[3].z += a.mat[3].z;
        this.mat[3].s += a.mat[3].s;
        this.mat[3].t += a.mat[3].t;
        //
        this.mat[4].x += a.mat[4].x;
        this.mat[4].y += a.mat[4].y;
        this.mat[4].z += a.mat[4].z;
        this.mat[4].s += a.mat[4].s;
        this.mat[4].t += a.mat[4].t;
        return this;
    }
//public	idMat5 &		operator-=( const idMat5 &a );

    public idMat5 oMinSet(final idMat5 a) {
        this.mat[0].x -= a.mat[0].x;
        this.mat[0].y -= a.mat[0].y;
        this.mat[0].z -= a.mat[0].z;
        this.mat[0].s -= a.mat[0].s;
        this.mat[0].t -= a.mat[0].t;
        //
        this.mat[1].x -= a.mat[1].x;
        this.mat[1].y -= a.mat[1].y;
        this.mat[1].z -= a.mat[1].z;
        this.mat[1].s -= a.mat[1].s;
        this.mat[1].t -= a.mat[1].t;
        //
        this.mat[2].x -= a.mat[2].x;
        this.mat[2].y -= a.mat[2].y;
        this.mat[2].z -= a.mat[2].z;
        this.mat[2].s -= a.mat[2].s;
        this.mat[2].t -= a.mat[2].t;
        //
        this.mat[3].x -= a.mat[3].x;
        this.mat[3].y -= a.mat[3].y;
        this.mat[3].z -= a.mat[3].z;
        this.mat[3].s -= a.mat[3].s;
        this.mat[3].t -= a.mat[3].t;
        //
        this.mat[4].x -= a.mat[4].x;
        this.mat[4].y -= a.mat[4].y;
        this.mat[4].z -= a.mat[4].z;
        this.mat[4].s -= a.mat[4].s;
        this.mat[4].t -= a.mat[4].t;
        return this;
    }

//public	friend idMat5	operator*( const float a, const idMat5 &mat );
    public static idMat5 oMultiply(final float a, final idMat5 mat) {
        return mat.oMultiply(a);
    }
//public	friend idVec5	operator*( const idVec5 &vec, const idMat5 &mat );

    public static idVec5 oMultiply(final idVec5 vec, final idMat5 mat) {
        return mat.oMultiply(vec);
    }

    public static idVec5 oMulSet(idVec5 vec, final idMat5 mat) {
        vec = mat.oMultiply(vec);
        return vec;
    }

    public boolean Compare(final idMat5 a) {// exact compare, no epsilon
        int i;
        float[] ptr1, ptr2;

        ptr1 = this.reinterpret_cast();
        ptr2 = a.reinterpret_cast();
        for (i = 0; i < 5 * 5; i++) {
            if (ptr1[i] != ptr2[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean Compare(final idMat5 a, final float epsilon)// compare with epsilon
    {
        int i;
        float[] ptr1, ptr2;

        ptr1 = this.reinterpret_cast();
        ptr2 = a.reinterpret_cast();
        for (i = 0; i < 5 * 5; i++) {
            if (idMath.Fabs(ptr1[i] - ptr2[i]) > epsilon) {
                return false;
            }
        }
        return true;
    }
//public	bool			operator==( const idMat5 &a ) const;					// exact compare, no epsilon
//public	bool			operator!=( const idMat5 &a ) const;					// exact compare, no epsilon

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Arrays.deepHashCode(this.mat);
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
        final idMat5 other = (idMat5) obj;
        if (!Arrays.deepEquals(this.mat, other.mat)) {
            return false;
        }
        return true;
    }

    public void Zero() {
        this.oSet(mat5_zero);
    }

    public void Identity() {
        this.oSet(mat5_identity);
    }

    public boolean IsIdentity() {
        return IsIdentity((float) MATRIX_EPSILON);
    }

    public boolean IsIdentity(final float epsilon) {
        return Compare(mat5_identity, epsilon);
    }

    public boolean IsSymmetric() {
        return IsSymmetric((float) MATRIX_EPSILON);
    }

    public boolean IsSymmetric(final float epsilon) {
        for (int i = 1; i < 5; i++) {
            for (int j = 0; j < i; j++) {
                if (idMath.Fabs(mat[i].oGet(j) - mat[j].oGet(i)) > epsilon) {
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
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (i != j && idMath.Fabs(mat[i].oGet(j)) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }

    public float Trace() {
        return (mat[0].x + mat[1].y + mat[2].z + mat[3].s + mat[4].t);
    }

    public float Determinant() {

        // 2x2 sub-determinants required to calculate 5x5 determinant
        float det2_34_01 = mat[3].x * mat[4].y - mat[3].y * mat[4].x;
        float det2_34_02 = mat[3].x * mat[4].z - mat[3].z * mat[4].x;
        float det2_34_03 = mat[3].x * mat[4].s - mat[3].s * mat[4].x;
        float det2_34_04 = mat[3].x * mat[4].t - mat[3].t * mat[4].x;
        float det2_34_12 = mat[3].y * mat[4].z - mat[3].z * mat[4].y;
        float det2_34_13 = mat[3].y * mat[4].s - mat[3].s * mat[4].y;
        float det2_34_14 = mat[3].y * mat[4].t - mat[3].t * mat[4].y;
        float det2_34_23 = mat[3].z * mat[4].s - mat[3].s * mat[4].z;
        float det2_34_24 = mat[3].z * mat[4].t - mat[3].t * mat[4].z;
        float det2_34_34 = mat[3].s * mat[4].t - mat[3].t * mat[4].s;

        // 3x3 sub-determinants required to calculate 5x5 determinant
        float det3_234_012 = mat[2].x * det2_34_12 - mat[2].y * det2_34_02 + mat[2].z * det2_34_01;
        float det3_234_013 = mat[2].x * det2_34_13 - mat[2].y * det2_34_03 + mat[2].s * det2_34_01;
        float det3_234_014 = mat[2].x * det2_34_14 - mat[2].y * det2_34_04 + mat[2].t * det2_34_01;
        float det3_234_023 = mat[2].x * det2_34_23 - mat[2].z * det2_34_03 + mat[2].s * det2_34_02;
        float det3_234_024 = mat[2].x * det2_34_24 - mat[2].z * det2_34_04 + mat[2].t * det2_34_02;
        float det3_234_034 = mat[2].x * det2_34_34 - mat[2].s * det2_34_04 + mat[2].t * det2_34_03;
        float det3_234_123 = mat[2].y * det2_34_23 - mat[2].z * det2_34_13 + mat[2].s * det2_34_12;
        float det3_234_124 = mat[2].y * det2_34_24 - mat[2].z * det2_34_14 + mat[2].t * det2_34_12;
        float det3_234_134 = mat[2].y * det2_34_34 - mat[2].s * det2_34_14 + mat[2].t * det2_34_13;
        float det3_234_234 = mat[2].z * det2_34_34 - mat[2].s * det2_34_24 + mat[2].t * det2_34_23;

        // 4x4 sub-determinants required to calculate 5x5 determinant
        float det4_1234_0123 = mat[1].x * det3_234_123 - mat[1].y * det3_234_023 + mat[1].z * det3_234_013 - mat[1].s * det3_234_012;
        float det4_1234_0124 = mat[1].x * det3_234_124 - mat[1].y * det3_234_024 + mat[1].z * det3_234_014 - mat[1].t * det3_234_012;
        float det4_1234_0134 = mat[1].x * det3_234_134 - mat[1].y * det3_234_034 + mat[1].s * det3_234_014 - mat[1].t * det3_234_013;
        float det4_1234_0234 = mat[1].x * det3_234_234 - mat[1].z * det3_234_034 + mat[1].s * det3_234_024 - mat[1].t * det3_234_023;
        float det4_1234_1234 = mat[1].y * det3_234_234 - mat[1].z * det3_234_134 + mat[1].s * det3_234_124 - mat[1].t * det3_234_123;

        // determinant of 5x5 matrix
        return mat[0].x * det4_1234_1234 - mat[0].y * det4_1234_0234 + mat[0].z * det4_1234_0134 - mat[0].s * det4_1234_0124 + mat[0].t * det4_1234_0123;
    }

    public idMat5 Transpose() {// returns transpose
        idMat5 transpose = new idMat5();
        int i, j;

        for (i = 0; i < 5; i++) {
            for (j = 0; j < 5; j++) {
                transpose.mat[i].oSet(j, mat[j].oGet(i));
            }
        }
        return transpose;
    }

    public idMat5 TransposeSelf() {
        float temp;
        int i, j;

        for (i = 0; i < 5; i++) {
            for (j = i + 1; j < 5; j++) {
                temp = mat[i].oGet(j);
                mat[i].oSet(j, mat[j].oGet(i));
                mat[j].oSet(i, temp);
            }
        }
        return this;
    }

    public idMat5 Inverse() {// returns the inverse ( m * m.Inverse() = identity )
        idMat5 invMat;

        invMat = this;
        boolean r = invMat.InverseSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseSelf() {// returns false if determinant is zero
        // 280+5+25 = 310 multiplications
        //				1 division
        float det, invDet;

        // 2x2 sub-determinants required to calculate 5x5 determinant
        float det2_34_01 = mat[3].x * mat[4].y - mat[3].y * mat[4].x;
        float det2_34_02 = mat[3].x * mat[4].z - mat[3].z * mat[4].x;
        float det2_34_03 = mat[3].x * mat[4].s - mat[3].s * mat[4].x;
        float det2_34_04 = mat[3].x * mat[4].t - mat[3].t * mat[4].x;
        float det2_34_12 = mat[3].y * mat[4].z - mat[3].z * mat[4].y;
        float det2_34_13 = mat[3].y * mat[4].s - mat[3].s * mat[4].y;
        float det2_34_14 = mat[3].y * mat[4].t - mat[3].t * mat[4].y;
        float det2_34_23 = mat[3].z * mat[4].s - mat[3].s * mat[4].z;
        float det2_34_24 = mat[3].z * mat[4].t - mat[3].t * mat[4].z;
        float det2_34_34 = mat[3].s * mat[4].t - mat[3].t * mat[4].s;

        // 3x3 sub-determinants required to calculate 5x5 determinant
        float det3_234_012 = mat[2].x * det2_34_12 - mat[2].y * det2_34_02 + mat[2].z * det2_34_01;
        float det3_234_013 = mat[2].x * det2_34_13 - mat[2].y * det2_34_03 + mat[2].s * det2_34_01;
        float det3_234_014 = mat[2].x * det2_34_14 - mat[2].y * det2_34_04 + mat[2].t * det2_34_01;
        float det3_234_023 = mat[2].x * det2_34_23 - mat[2].z * det2_34_03 + mat[2].s * det2_34_02;
        float det3_234_024 = mat[2].x * det2_34_24 - mat[2].z * det2_34_04 + mat[2].t * det2_34_02;
        float det3_234_034 = mat[2].x * det2_34_34 - mat[2].s * det2_34_04 + mat[2].t * det2_34_03;
        float det3_234_123 = mat[2].y * det2_34_23 - mat[2].z * det2_34_13 + mat[2].s * det2_34_12;
        float det3_234_124 = mat[2].y * det2_34_24 - mat[2].z * det2_34_14 + mat[2].t * det2_34_12;
        float det3_234_134 = mat[2].y * det2_34_34 - mat[2].s * det2_34_14 + mat[2].t * det2_34_13;
        float det3_234_234 = mat[2].z * det2_34_34 - mat[2].s * det2_34_24 + mat[2].t * det2_34_23;

        // 4x4 sub-determinants required to calculate 5x5 determinant
        float det4_1234_0123 = mat[1].x * det3_234_123 - mat[1].y * det3_234_023 + mat[1].z * det3_234_013 - mat[1].s * det3_234_012;
        float det4_1234_0124 = mat[1].x * det3_234_124 - mat[1].y * det3_234_024 + mat[1].z * det3_234_014 - mat[1].t * det3_234_012;
        float det4_1234_0134 = mat[1].x * det3_234_134 - mat[1].y * det3_234_034 + mat[1].s * det3_234_014 - mat[1].t * det3_234_013;
        float det4_1234_0234 = mat[1].x * det3_234_234 - mat[1].z * det3_234_034 + mat[1].s * det3_234_024 - mat[1].t * det3_234_023;
        float det4_1234_1234 = mat[1].y * det3_234_234 - mat[1].z * det3_234_134 + mat[1].s * det3_234_124 - mat[1].t * det3_234_123;

        // determinant of 5x5 matrix
        det = mat[0].x * det4_1234_1234 - mat[0].y * det4_1234_0234 + mat[0].z * det4_1234_0134 - mat[0].s * det4_1234_0124 + mat[0].t * det4_1234_0123;

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        // remaining 2x2 sub-determinants
        float det2_23_01 = mat[2].x * mat[3].y - mat[2].y * mat[3].x;
        float det2_23_02 = mat[2].x * mat[3].z - mat[2].z * mat[3].x;
        float det2_23_03 = mat[2].x * mat[3].s - mat[2].s * mat[3].x;
        float det2_23_04 = mat[2].x * mat[3].t - mat[2].t * mat[3].x;
        float det2_23_12 = mat[2].y * mat[3].z - mat[2].z * mat[3].y;
        float det2_23_13 = mat[2].y * mat[3].s - mat[2].s * mat[3].y;
        float det2_23_14 = mat[2].y * mat[3].t - mat[2].t * mat[3].y;
        float det2_23_23 = mat[2].z * mat[3].s - mat[2].s * mat[3].z;
        float det2_23_24 = mat[2].z * mat[3].t - mat[2].t * mat[3].z;
        float det2_23_34 = mat[2].s * mat[3].t - mat[2].t * mat[3].s;
        float det2_24_01 = mat[2].x * mat[4].y - mat[2].y * mat[4].x;
        float det2_24_02 = mat[2].x * mat[4].z - mat[2].z * mat[4].x;
        float det2_24_03 = mat[2].x * mat[4].s - mat[2].s * mat[4].x;
        float det2_24_04 = mat[2].x * mat[4].t - mat[2].t * mat[4].x;
        float det2_24_12 = mat[2].y * mat[4].z - mat[2].z * mat[4].y;
        float det2_24_13 = mat[2].y * mat[4].s - mat[2].s * mat[4].y;
        float det2_24_14 = mat[2].y * mat[4].t - mat[2].t * mat[4].y;
        float det2_24_23 = mat[2].z * mat[4].s - mat[2].s * mat[4].z;
        float det2_24_24 = mat[2].z * mat[4].t - mat[2].t * mat[4].z;
        float det2_24_34 = mat[2].s * mat[4].t - mat[2].t * mat[4].s;

        // remaining 3x3 sub-determinants
        float det3_123_012 = mat[1].x * det2_23_12 - mat[1].y * det2_23_02 + mat[1].z * det2_23_01;
        float det3_123_013 = mat[1].x * det2_23_13 - mat[1].y * det2_23_03 + mat[1].s * det2_23_01;
        float det3_123_014 = mat[1].x * det2_23_14 - mat[1].y * det2_23_04 + mat[1].t * det2_23_01;
        float det3_123_023 = mat[1].x * det2_23_23 - mat[1].z * det2_23_03 + mat[1].s * det2_23_02;
        float det3_123_024 = mat[1].x * det2_23_24 - mat[1].z * det2_23_04 + mat[1].t * det2_23_02;
        float det3_123_034 = mat[1].x * det2_23_34 - mat[1].s * det2_23_04 + mat[1].t * det2_23_03;
        float det3_123_123 = mat[1].y * det2_23_23 - mat[1].z * det2_23_13 + mat[1].s * det2_23_12;
        float det3_123_124 = mat[1].y * det2_23_24 - mat[1].z * det2_23_14 + mat[1].t * det2_23_12;
        float det3_123_134 = mat[1].y * det2_23_34 - mat[1].s * det2_23_14 + mat[1].t * det2_23_13;
        float det3_123_234 = mat[1].z * det2_23_34 - mat[1].s * det2_23_24 + mat[1].t * det2_23_23;
        float det3_124_012 = mat[1].x * det2_24_12 - mat[1].y * det2_24_02 + mat[1].z * det2_24_01;
        float det3_124_013 = mat[1].x * det2_24_13 - mat[1].y * det2_24_03 + mat[1].s * det2_24_01;
        float det3_124_014 = mat[1].x * det2_24_14 - mat[1].y * det2_24_04 + mat[1].t * det2_24_01;
        float det3_124_023 = mat[1].x * det2_24_23 - mat[1].z * det2_24_03 + mat[1].s * det2_24_02;
        float det3_124_024 = mat[1].x * det2_24_24 - mat[1].z * det2_24_04 + mat[1].t * det2_24_02;
        float det3_124_034 = mat[1].x * det2_24_34 - mat[1].s * det2_24_04 + mat[1].t * det2_24_03;
        float det3_124_123 = mat[1].y * det2_24_23 - mat[1].z * det2_24_13 + mat[1].s * det2_24_12;
        float det3_124_124 = mat[1].y * det2_24_24 - mat[1].z * det2_24_14 + mat[1].t * det2_24_12;
        float det3_124_134 = mat[1].y * det2_24_34 - mat[1].s * det2_24_14 + mat[1].t * det2_24_13;
        float det3_124_234 = mat[1].z * det2_24_34 - mat[1].s * det2_24_24 + mat[1].t * det2_24_23;
        float det3_134_012 = mat[1].x * det2_34_12 - mat[1].y * det2_34_02 + mat[1].z * det2_34_01;
        float det3_134_013 = mat[1].x * det2_34_13 - mat[1].y * det2_34_03 + mat[1].s * det2_34_01;
        float det3_134_014 = mat[1].x * det2_34_14 - mat[1].y * det2_34_04 + mat[1].t * det2_34_01;
        float det3_134_023 = mat[1].x * det2_34_23 - mat[1].z * det2_34_03 + mat[1].s * det2_34_02;
        float det3_134_024 = mat[1].x * det2_34_24 - mat[1].z * det2_34_04 + mat[1].t * det2_34_02;
        float det3_134_034 = mat[1].x * det2_34_34 - mat[1].s * det2_34_04 + mat[1].t * det2_34_03;
        float det3_134_123 = mat[1].y * det2_34_23 - mat[1].z * det2_34_13 + mat[1].s * det2_34_12;
        float det3_134_124 = mat[1].y * det2_34_24 - mat[1].z * det2_34_14 + mat[1].t * det2_34_12;
        float det3_134_134 = mat[1].y * det2_34_34 - mat[1].s * det2_34_14 + mat[1].t * det2_34_13;
        float det3_134_234 = mat[1].z * det2_34_34 - mat[1].s * det2_34_24 + mat[1].t * det2_34_23;

        // remaining 4x4 sub-determinants
        float det4_0123_0123 = mat[0].x * det3_123_123 - mat[0].y * det3_123_023 + mat[0].z * det3_123_013 - mat[0].s * det3_123_012;
        float det4_0123_0124 = mat[0].x * det3_123_124 - mat[0].y * det3_123_024 + mat[0].z * det3_123_014 - mat[0].t * det3_123_012;
        float det4_0123_0134 = mat[0].x * det3_123_134 - mat[0].y * det3_123_034 + mat[0].s * det3_123_014 - mat[0].t * det3_123_013;
        float det4_0123_0234 = mat[0].x * det3_123_234 - mat[0].z * det3_123_034 + mat[0].s * det3_123_024 - mat[0].t * det3_123_023;
        float det4_0123_1234 = mat[0].y * det3_123_234 - mat[0].z * det3_123_134 + mat[0].s * det3_123_124 - mat[0].t * det3_123_123;
        float det4_0124_0123 = mat[0].x * det3_124_123 - mat[0].y * det3_124_023 + mat[0].z * det3_124_013 - mat[0].s * det3_124_012;
        float det4_0124_0124 = mat[0].x * det3_124_124 - mat[0].y * det3_124_024 + mat[0].z * det3_124_014 - mat[0].t * det3_124_012;
        float det4_0124_0134 = mat[0].x * det3_124_134 - mat[0].y * det3_124_034 + mat[0].s * det3_124_014 - mat[0].t * det3_124_013;
        float det4_0124_0234 = mat[0].x * det3_124_234 - mat[0].z * det3_124_034 + mat[0].s * det3_124_024 - mat[0].t * det3_124_023;
        float det4_0124_1234 = mat[0].y * det3_124_234 - mat[0].z * det3_124_134 + mat[0].s * det3_124_124 - mat[0].t * det3_124_123;
        float det4_0134_0123 = mat[0].x * det3_134_123 - mat[0].y * det3_134_023 + mat[0].z * det3_134_013 - mat[0].s * det3_134_012;
        float det4_0134_0124 = mat[0].x * det3_134_124 - mat[0].y * det3_134_024 + mat[0].z * det3_134_014 - mat[0].t * det3_134_012;
        float det4_0134_0134 = mat[0].x * det3_134_134 - mat[0].y * det3_134_034 + mat[0].s * det3_134_014 - mat[0].t * det3_134_013;
        float det4_0134_0234 = mat[0].x * det3_134_234 - mat[0].z * det3_134_034 + mat[0].s * det3_134_024 - mat[0].t * det3_134_023;
        float det4_0134_1234 = mat[0].y * det3_134_234 - mat[0].z * det3_134_134 + mat[0].s * det3_134_124 - mat[0].t * det3_134_123;
        float det4_0234_0123 = mat[0].x * det3_234_123 - mat[0].y * det3_234_023 + mat[0].z * det3_234_013 - mat[0].s * det3_234_012;
        float det4_0234_0124 = mat[0].x * det3_234_124 - mat[0].y * det3_234_024 + mat[0].z * det3_234_014 - mat[0].t * det3_234_012;
        float det4_0234_0134 = mat[0].x * det3_234_134 - mat[0].y * det3_234_034 + mat[0].s * det3_234_014 - mat[0].t * det3_234_013;
        float det4_0234_0234 = mat[0].x * det3_234_234 - mat[0].z * det3_234_034 + mat[0].s * det3_234_024 - mat[0].t * det3_234_023;
        float det4_0234_1234 = mat[0].y * det3_234_234 - mat[0].z * det3_234_134 + mat[0].s * det3_234_124 - mat[0].t * det3_234_123;

        mat[0].x = det4_1234_1234 * invDet;
        mat[0].y = -det4_0234_1234 * invDet;
        mat[0].z = det4_0134_1234 * invDet;
        mat[0].s = -det4_0124_1234 * invDet;
        mat[0].t = det4_0123_1234 * invDet;

        mat[1].x = -det4_1234_0234 * invDet;
        mat[1].y = det4_0234_0234 * invDet;
        mat[1].z = -det4_0134_0234 * invDet;
        mat[1].s = det4_0124_0234 * invDet;
        mat[1].t = -det4_0123_0234 * invDet;

        mat[2].x = det4_1234_0134 * invDet;
        mat[2].y = -det4_0234_0134 * invDet;
        mat[2].z = det4_0134_0134 * invDet;
        mat[2].s = -det4_0124_0134 * invDet;
        mat[2].t = det4_0123_0134 * invDet;

        mat[3].x = -det4_1234_0124 * invDet;
        mat[3].y = det4_0234_0124 * invDet;
        mat[3].z = -det4_0134_0124 * invDet;
        mat[3].s = det4_0124_0124 * invDet;
        mat[3].t = -det4_0123_0124 * invDet;

        mat[4].x = det4_1234_0123 * invDet;
        mat[4].y = -det4_0234_0123 * invDet;
        mat[4].z = det4_0134_0123 * invDet;
        mat[4].s = -det4_0124_0123 * invDet;
        mat[4].t = det4_0123_0123 * invDet;

        return true;
    }

    public idMat5 InverseFast() {// returns the inverse ( m * m.Inverse() = identity )
        idMat5 invMat;

        invMat = this;
        boolean r = invMat.InverseFastSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseFastSelf() {// returns false if determinant is zero
//    #else
        // 86+30+6 = 122 multiplications
        //	  2*1  =   2 divisions
        idVec3[] r0 = new idVec3[3], r1 = new idVec3[3], r2 = new idVec3[3], r3 = new idVec3[3];
        float c0, c1, c2, det, invDet;
        float[] matt = reinterpret_cast();

        // r0 = m0.Inverse();	// 3x3
        c0 = matt[1 * 5 + 1] * matt[2 * 5 + 2] - matt[1 * 5 + 2] * matt[2 * 5 + 1];
        c1 = matt[1 * 5 + 2] * matt[2 * 5 + 0] - matt[1 * 5 + 0] * matt[2 * 5 + 2];
        c2 = matt[1 * 5 + 0] * matt[2 * 5 + 1] - matt[1 * 5 + 1] * matt[2 * 5 + 0];

        det = matt[0 * 5 + 0] * c0 + matt[0 * 5 + 1] * c1 + matt[0 * 5 + 2] * c2;

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        r0[0].x = c0 * invDet;
        r0[0].y = (matt[0 * 5 + 2] * matt[2 * 5 + 1] - matt[0 * 5 + 1] * matt[2 * 5 + 2]) * invDet;
        r0[0].z = (matt[0 * 5 + 1] * matt[1 * 5 + 2] - matt[0 * 5 + 2] * matt[1 * 5 + 1]) * invDet;
        r0[1].x = c1 * invDet;
        r0[1].y = (matt[0 * 5 + 0] * matt[2 * 5 + 2] - matt[0 * 5 + 2] * matt[2 * 5 + 0]) * invDet;
        r0[1].z = (matt[0 * 5 + 2] * matt[1 * 5 + 0] - matt[0 * 5 + 0] * matt[1 * 5 + 2]) * invDet;
        r0[2].x = c2 * invDet;
        r0[2].y = (matt[0 * 5 + 1] * matt[2 * 5 + 0] - matt[0 * 5 + 0] * matt[2 * 5 + 1]) * invDet;
        r0[2].z = (matt[0 * 5 + 0] * matt[1 * 5 + 1] - matt[0 * 5 + 1] * matt[1 * 5 + 0]) * invDet;

        // r1 = r0 * m1;		// 3x2 = 3x3 * 3x2
        r1[0].x = r0[0].x * matt[0 * 5 + 3] + r0[0].y * matt[1 * 5 + 3] + r0[0].z * matt[2 * 5 + 3];
        r1[0].y = r0[0].x * matt[0 * 5 + 4] + r0[0].y * matt[1 * 5 + 4] + r0[0].z * matt[2 * 5 + 4];
        r1[1].x = r0[1].x * matt[0 * 5 + 3] + r0[1].y * matt[1 * 5 + 3] + r0[1].z * matt[2 * 5 + 3];
        r1[1].y = r0[1].x * matt[0 * 5 + 4] + r0[1].y * matt[1 * 5 + 4] + r0[1].z * matt[2 * 5 + 4];
        r1[2].x = r0[2].x * matt[0 * 5 + 3] + r0[2].y * matt[1 * 5 + 3] + r0[2].z * matt[2 * 5 + 3];
        r1[2].y = r0[2].x * matt[0 * 5 + 4] + r0[2].y * matt[1 * 5 + 4] + r0[2].z * matt[2 * 5 + 4];

        // r2 = m2 * r1;		// 2x2 = 2x3 * 3x2
        r2[0].x = matt[3 * 5 + 0] * r1[0].x + matt[3 * 5 + 1] * r1[1].x + matt[3 * 5 + 2] * r1[2].x;
        r2[0].y = matt[3 * 5 + 0] * r1[0].y + matt[3 * 5 + 1] * r1[1].y + matt[3 * 5 + 2] * r1[2].y;
        r2[1].x = matt[4 * 5 + 0] * r1[0].x + matt[4 * 5 + 1] * r1[1].x + matt[4 * 5 + 2] * r1[2].x;
        r2[1].y = matt[4 * 5 + 0] * r1[0].y + matt[4 * 5 + 1] * r1[1].y + matt[4 * 5 + 2] * r1[2].y;

        // r3 = r2 - m3;		// 2x2 = 2x2 - 2x2
        r3[0].x = r2[0].x - matt[3 * 5 + 3];
        r3[0].y = r2[0].y - matt[3 * 5 + 4];
        r3[1].x = r2[1].x - matt[4 * 5 + 3];
        r3[1].y = r2[1].y - matt[4 * 5 + 4];

        // r3.InverseSelf();	// 2x2
        det = r3[0].x * r3[1].y - r3[0].y * r3[1].x;

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        c0 = r3[0].x;
        r3[0].x = r3[1].y * invDet;
        r3[0].y = -r3[0].y * invDet;
        r3[1].x = -r3[1].x * invDet;
        r3[1].y = c0 * invDet;

        // r2 = m2 * r0;		// 2x3 = 2x3 * 3x3
        r2[0].x = matt[3 * 5 + 0] * r0[0].x + matt[3 * 5 + 1] * r0[1].x + matt[3 * 5 + 2] * r0[2].x;
        r2[0].y = matt[3 * 5 + 0] * r0[0].y + matt[3 * 5 + 1] * r0[1].y + matt[3 * 5 + 2] * r0[2].y;
        r2[0].z = matt[3 * 5 + 0] * r0[0].z + matt[3 * 5 + 1] * r0[1].z + matt[3 * 5 + 2] * r0[2].z;
        r2[1].x = matt[4 * 5 + 0] * r0[0].x + matt[4 * 5 + 1] * r0[1].x + matt[4 * 5 + 2] * r0[2].x;
        r2[1].y = matt[4 * 5 + 0] * r0[0].y + matt[4 * 5 + 1] * r0[1].y + matt[4 * 5 + 2] * r0[2].y;
        r2[1].z = matt[4 * 5 + 0] * r0[0].z + matt[4 * 5 + 1] * r0[1].z + matt[4 * 5 + 2] * r0[2].z;

        // m2 = r3 * r2;		// 2x3 = 2x2 * 2x3
        matt[3 * 5 + 0] = r3[0].x * r2[0].x + r3[0].y * r2[1].x;
        matt[3 * 5 + 1] = r3[0].x * r2[0].y + r3[0].y * r2[1].y;
        matt[3 * 5 + 2] = r3[0].x * r2[0].z + r3[0].y * r2[1].z;
        matt[4 * 5 + 0] = r3[1].x * r2[0].x + r3[1].y * r2[1].x;
        matt[4 * 5 + 1] = r3[1].x * r2[0].y + r3[1].y * r2[1].y;
        matt[4 * 5 + 2] = r3[1].x * r2[0].z + r3[1].y * r2[1].z;

        // m0 = r0 - r1 * m2;	// 3x3 = 3x3 - 3x2 * 2x3
        matt[0 * 5 + 0] = r0[0].x - r1[0].x * matt[3 * 5 + 0] - r1[0].y * matt[4 * 5 + 0];
        matt[0 * 5 + 1] = r0[0].y - r1[0].x * matt[3 * 5 + 1] - r1[0].y * matt[4 * 5 + 1];
        matt[0 * 5 + 2] = r0[0].z - r1[0].x * matt[3 * 5 + 2] - r1[0].y * matt[4 * 5 + 2];
        matt[1 * 5 + 0] = r0[1].x - r1[1].x * matt[3 * 5 + 0] - r1[1].y * matt[4 * 5 + 0];
        matt[1 * 5 + 1] = r0[1].y - r1[1].x * matt[3 * 5 + 1] - r1[1].y * matt[4 * 5 + 1];
        matt[1 * 5 + 2] = r0[1].z - r1[1].x * matt[3 * 5 + 2] - r1[1].y * matt[4 * 5 + 2];
        matt[2 * 5 + 0] = r0[2].x - r1[2].x * matt[3 * 5 + 0] - r1[2].y * matt[4 * 5 + 0];
        matt[2 * 5 + 1] = r0[2].y - r1[2].x * matt[3 * 5 + 1] - r1[2].y * matt[4 * 5 + 1];
        matt[2 * 5 + 2] = r0[2].z - r1[2].x * matt[3 * 5 + 2] - r1[2].y * matt[4 * 5 + 2];

        // m1 = r1 * r3;		// 3x2 = 3x2 * 2x2
        matt[0 * 5 + 3] = r1[0].x * r3[0].x + r1[0].y * r3[1].x;
        matt[0 * 5 + 4] = r1[0].x * r3[0].y + r1[0].y * r3[1].y;
        matt[1 * 5 + 3] = r1[1].x * r3[0].x + r1[1].y * r3[1].x;
        matt[1 * 5 + 4] = r1[1].x * r3[0].y + r1[1].y * r3[1].y;
        matt[2 * 5 + 3] = r1[2].x * r3[0].x + r1[2].y * r3[1].x;
        matt[2 * 5 + 4] = r1[2].x * r3[0].y + r1[2].y * r3[1].y;

        // m3 = -r3;			// 2x2 = - 2x2
        matt[3 * 5 + 3] = -r3[0].x;
        matt[3 * 5 + 4] = -r3[0].y;
        matt[4 * 5 + 3] = -r3[1].x;
        matt[4 * 5 + 4] = -r3[1].y;

        return true;
//#endif
    }

    public final int GetDimension() {
        return 25;
    }
//public	const float *	ToFloatPtr( void ) const;
//public	float *			ToFloatPtr( void );
//public	const char *	ToString( int precision = 2 ) const;

    private void oSet(final idMat5 mat5) {
        this.mat[0].oSet(mat5.mat[0]);
        this.mat[1].oSet(mat5.mat[1]);
        this.mat[2].oSet(mat5.mat[2]);
        this.mat[3].oSet(mat5.mat[3]);
        this.mat[4].oSet(mat5.mat[4]);
    }

    float[] reinterpret_cast() {
        final int size = 5;

        float[] temp = new float[size * size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                temp[x * size + y] = this.mat[x].oGet(y);
            }
        }
        return temp;
    }
};
