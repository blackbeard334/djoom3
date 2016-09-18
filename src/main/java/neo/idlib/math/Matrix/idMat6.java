package neo.idlib.math.Matrix;

import java.util.Arrays;

import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Matrix.idMat0.MATRIX_EPSILON;
import static neo.idlib.math.Matrix.idMat0.MATRIX_INVERSE_EPSILON;

import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec6;

//===============================================================
//
//	idMat6 - 6x6 matrix
//
//===============================================================
public class idMat6 {
    private static final idMat6 mat6_zero = new idMat6(new idVec6(0, 0, 0, 0, 0, 0), new idVec6(0, 0, 0, 0, 0, 0), new idVec6(0, 0, 0, 0, 0, 0), new idVec6(0, 0, 0, 0, 0, 0), new idVec6(0, 0, 0, 0, 0, 0), new idVec6(0, 0, 0, 0, 0, 0));
    private static final idMat6 mat6_identity = new idMat6(new idVec6(1, 0, 0, 0, 0, 0), new idVec6(0, 1, 0, 0, 0, 0), new idVec6(0, 0, 1, 0, 0, 0), new idVec6(0, 0, 0, 1, 0, 0), new idVec6(0, 0, 0, 0, 1, 0), new idVec6(0, 0, 0, 0, 0, 1));

    public static idMat6 getMat6_zero() {
        return new idMat6(mat6_zero);
    }

    public static idMat6 getMat6_identity() {
        return new idMat6(mat6_identity);
    }
    
    private final idVec6[] mat = {new idVec6(), new idVec6(), new idVec6(), new idVec6(), new idVec6(), new idVec6()};

    public idMat6() {
    }

    public idMat6(final idVec6 v0, final idVec6 v1, final idVec6 v2, final idVec6 v3, final idVec6 v4, final idVec6 v5) {
        mat[0].oSet(v0);
        mat[1].oSet(v1);
        mat[2].oSet(v2);
        mat[3].oSet(v3);
        mat[4].oSet(v4);
        mat[5].oSet(v5);
    }

    public idMat6(final idMat3 m0, final idMat3 m1, final idMat3 m2, final idMat3 m3) {
        mat[0].oSet(new idVec6(m0.mat[0].x, m0.mat[0].y, m0.mat[0].z, m1.mat[0].x, m1.mat[0].y, m1.mat[0].z));
        mat[1].oSet(new idVec6(m0.mat[1].x, m0.mat[1].y, m0.mat[1].z, m1.mat[1].x, m1.mat[1].y, m1.mat[1].z));
        mat[2].oSet(new idVec6(m0.mat[2].x, m0.mat[2].y, m0.mat[2].z, m1.mat[2].x, m1.mat[2].y, m1.mat[2].z));
        mat[3].oSet(new idVec6(m2.mat[0].x, m2.mat[0].y, m2.mat[0].z, m3.mat[0].x, m3.mat[0].y, m3.mat[0].z));
        mat[4].oSet(new idVec6(m2.mat[1].x, m2.mat[1].y, m2.mat[1].z, m3.mat[1].x, m3.mat[1].y, m3.mat[1].z));
        mat[5].oSet(new idVec6(m2.mat[2].x, m2.mat[2].y, m2.mat[2].z, m3.mat[2].x, m3.mat[2].y, m3.mat[2].z));
    }

    public idMat6(final float src[][]) {
//	memcpy( mat, src, 6 * 6 * sizeof( float ) );
        mat[0].oSet(new idVec6(src[0][0], src[0][1], src[0][2], src[0][3], src[0][4], src[0][5]));
        mat[1].oSet(new idVec6(src[1][0], src[1][1], src[1][2], src[1][3], src[1][4], src[1][5]));
        mat[2].oSet(new idVec6(src[2][0], src[2][1], src[2][2], src[2][3], src[2][4], src[2][5]));
        mat[3].oSet(new idVec6(src[3][0], src[3][1], src[3][2], src[3][3], src[3][4], src[3][5]));
        mat[4].oSet(new idVec6(src[4][0], src[4][1], src[4][2], src[4][3], src[4][4], src[4][5]));
        mat[5].oSet(new idVec6(src[5][0], src[5][1], src[5][2], src[5][3], src[5][4], src[5][5]));
    }

    public idMat6(final idMat6 m) {
        this.oSet(m);
    }

//public	const idVec6 &	operator[]( int index ) const;
//public	idVec6 &		operator[]( int index );
//public	idMat6			operator*( const float a ) const;
    public idMat6 oMultiply(final float a) {
        return new idMat6(
                new idVec6(mat[0].p[0] * a, mat[0].p[1] * a, mat[0].p[2] * a, mat[0].p[3] * a, mat[0].p[4] * a, mat[0].p[5] * a),
                new idVec6(mat[1].p[0] * a, mat[1].p[1] * a, mat[1].p[2] * a, mat[1].p[3] * a, mat[1].p[4] * a, mat[1].p[5] * a),
                new idVec6(mat[2].p[0] * a, mat[2].p[1] * a, mat[2].p[2] * a, mat[2].p[3] * a, mat[2].p[4] * a, mat[2].p[5] * a),
                new idVec6(mat[3].p[0] * a, mat[3].p[1] * a, mat[3].p[2] * a, mat[3].p[3] * a, mat[3].p[4] * a, mat[3].p[5] * a),
                new idVec6(mat[4].p[0] * a, mat[4].p[1] * a, mat[4].p[2] * a, mat[4].p[3] * a, mat[4].p[4] * a, mat[4].p[5] * a),
                new idVec6(mat[5].p[0] * a, mat[5].p[1] * a, mat[5].p[2] * a, mat[5].p[3] * a, mat[5].p[4] * a, mat[5].p[5] * a));
    }
//public	idVec6			operator*( const idVec6 &vec ) const;

    public idVec6 oMultiply(final idVec6 vec) {
        return new idVec6(
                mat[0].p[0] * vec.p[0] + mat[0].p[1] * vec.p[1] + mat[0].p[2] * vec.p[2] + mat[0].p[3] * vec.p[3] + mat[0].p[4] * vec.p[4] + mat[0].p[5] * vec.p[5],
                mat[1].p[0] * vec.p[0] + mat[1].p[1] * vec.p[1] + mat[1].p[2] * vec.p[2] + mat[1].p[3] * vec.p[3] + mat[1].p[4] * vec.p[4] + mat[1].p[5] * vec.p[5],
                mat[2].p[0] * vec.p[0] + mat[2].p[1] * vec.p[1] + mat[2].p[2] * vec.p[2] + mat[2].p[3] * vec.p[3] + mat[2].p[4] * vec.p[4] + mat[2].p[5] * vec.p[5],
                mat[3].p[0] * vec.p[0] + mat[3].p[1] * vec.p[1] + mat[3].p[2] * vec.p[2] + mat[3].p[3] * vec.p[3] + mat[3].p[4] * vec.p[4] + mat[3].p[5] * vec.p[5],
                mat[4].p[0] * vec.p[0] + mat[4].p[1] * vec.p[1] + mat[4].p[2] * vec.p[2] + mat[4].p[3] * vec.p[3] + mat[4].p[4] * vec.p[4] + mat[4].p[5] * vec.p[5],
                mat[5].p[0] * vec.p[0] + mat[5].p[1] * vec.p[1] + mat[5].p[2] * vec.p[2] + mat[5].p[3] * vec.p[3] + mat[5].p[4] * vec.p[4] + mat[5].p[5] * vec.p[5]);
    }
//public	idMat6			operator*( const idMat6 &a ) const;

    public idMat6 oMultiply(final idMat6 a) {
        int i, j;
        final float[] m1Ptr, m2Ptr;
//	float *dstPtr;
        idMat6 dst = new idMat6();

        m1Ptr = this.reinterpret_cast();
        m2Ptr = a.reinterpret_cast();
//	dstPtr = reinterpret_cast<float *>(&dst);

        for (i = 0; i < 6; i++) {
            for (j = 0; j < 6; j++) {
                dst.mat[i].p[i] = m1Ptr[0] * m2Ptr[ 0 * 6 + j]
                        + m1Ptr[1] * m2Ptr[ 1 * 6 + j]
                        + m1Ptr[2] * m2Ptr[ 2 * 6 + j]
                        + m1Ptr[3] * m2Ptr[ 3 * 6 + j]
                        + m1Ptr[4] * m2Ptr[ 4 * 6 + j]
                        + m1Ptr[5] * m2Ptr[ 5 * 6 + j];
//			dstPtr++;
            }
//		m1Ptr += 6;
        }
        return dst;
    }
//public	idMat6			operator+( const idMat6 &a ) const;

    public idMat6 oPlus(final idMat6 a) {
        return new idMat6(
                new idVec6(mat[0].p[0] + a.mat[0].p[0], mat[0].p[1] + a.mat[0].p[1], mat[0].p[2] + a.mat[0].p[2], mat[0].p[3] + a.mat[0].p[3], mat[0].p[4] + a.mat[0].p[4], mat[0].p[5] + a.mat[0].p[5]),
                new idVec6(mat[1].p[0] + a.mat[1].p[0], mat[1].p[1] + a.mat[1].p[1], mat[1].p[2] + a.mat[1].p[2], mat[1].p[3] + a.mat[1].p[3], mat[1].p[4] + a.mat[1].p[4], mat[1].p[5] + a.mat[1].p[5]),
                new idVec6(mat[2].p[0] + a.mat[2].p[0], mat[2].p[1] + a.mat[2].p[1], mat[2].p[2] + a.mat[2].p[2], mat[2].p[3] + a.mat[2].p[3], mat[2].p[4] + a.mat[2].p[4], mat[2].p[5] + a.mat[2].p[5]),
                new idVec6(mat[3].p[0] + a.mat[3].p[0], mat[3].p[1] + a.mat[3].p[1], mat[3].p[2] + a.mat[3].p[2], mat[3].p[3] + a.mat[3].p[3], mat[3].p[4] + a.mat[3].p[4], mat[3].p[5] + a.mat[3].p[5]),
                new idVec6(mat[4].p[0] + a.mat[4].p[0], mat[4].p[1] + a.mat[4].p[1], mat[4].p[2] + a.mat[4].p[2], mat[4].p[3] + a.mat[4].p[3], mat[4].p[4] + a.mat[4].p[4], mat[4].p[5] + a.mat[4].p[5]),
                new idVec6(mat[5].p[0] + a.mat[5].p[0], mat[5].p[1] + a.mat[5].p[1], mat[5].p[2] + a.mat[5].p[2], mat[5].p[3] + a.mat[5].p[3], mat[5].p[4] + a.mat[5].p[4], mat[5].p[5] + a.mat[5].p[5]));
    }

//public	idMat6			operator-( const idMat6 &a ) const;
    public idMat6 oMinus(final idMat6 a) {
        return new idMat6(
                new idVec6(mat[0].p[0] - a.mat[0].p[0], mat[0].p[1] - a.mat[0].p[1], mat[0].p[2] - a.mat[0].p[2], mat[0].p[3] - a.mat[0].p[3], mat[0].p[4] - a.mat[0].p[4], mat[0].p[5] - a.mat[0].p[5]),
                new idVec6(mat[1].p[0] - a.mat[1].p[0], mat[1].p[1] - a.mat[1].p[1], mat[1].p[2] - a.mat[1].p[2], mat[1].p[3] - a.mat[1].p[3], mat[1].p[4] - a.mat[1].p[4], mat[1].p[5] - a.mat[1].p[5]),
                new idVec6(mat[2].p[0] - a.mat[2].p[0], mat[2].p[1] - a.mat[2].p[1], mat[2].p[2] - a.mat[2].p[2], mat[2].p[3] - a.mat[2].p[3], mat[2].p[4] - a.mat[2].p[4], mat[2].p[5] - a.mat[2].p[5]),
                new idVec6(mat[3].p[0] - a.mat[3].p[0], mat[3].p[1] - a.mat[3].p[1], mat[3].p[2] - a.mat[3].p[2], mat[3].p[3] - a.mat[3].p[3], mat[3].p[4] - a.mat[3].p[4], mat[3].p[5] - a.mat[3].p[5]),
                new idVec6(mat[4].p[0] - a.mat[4].p[0], mat[4].p[1] - a.mat[4].p[1], mat[4].p[2] - a.mat[4].p[2], mat[4].p[3] - a.mat[4].p[3], mat[4].p[4] - a.mat[4].p[4], mat[4].p[5] - a.mat[4].p[5]),
                new idVec6(mat[5].p[0] - a.mat[5].p[0], mat[5].p[1] - a.mat[5].p[1], mat[5].p[2] - a.mat[5].p[2], mat[5].p[3] - a.mat[5].p[3], mat[5].p[4] - a.mat[5].p[4], mat[5].p[5] - a.mat[5].p[5]));
    }
//public	idMat6 &		operator*=( const float a );

    public idMat6 oMulSet(final float a) {
        mat[0].p[0] *= a;
        mat[0].p[1] *= a;
        mat[0].p[2] *= a;
        mat[0].p[3] *= a;
        mat[0].p[4] *= a;
        mat[0].p[5] *= a;
        mat[1].p[0] *= a;
        mat[1].p[1] *= a;
        mat[1].p[2] *= a;
        mat[1].p[3] *= a;
        mat[1].p[4] *= a;
        mat[1].p[5] *= a;
        mat[2].p[0] *= a;
        mat[2].p[1] *= a;
        mat[2].p[2] *= a;
        mat[2].p[3] *= a;
        mat[2].p[4] *= a;
        mat[2].p[5] *= a;
        mat[3].p[0] *= a;
        mat[3].p[1] *= a;
        mat[3].p[2] *= a;
        mat[3].p[3] *= a;
        mat[3].p[4] *= a;
        mat[3].p[5] *= a;
        mat[4].p[0] *= a;
        mat[4].p[1] *= a;
        mat[4].p[2] *= a;
        mat[4].p[3] *= a;
        mat[4].p[4] *= a;
        mat[4].p[5] *= a;
        mat[5].p[0] *= a;
        mat[5].p[1] *= a;
        mat[5].p[2] *= a;
        mat[5].p[3] *= a;
        mat[5].p[4] *= a;
        mat[5].p[5] *= a;
        return this;
    }
//public	idMat6 &		operator*=( const idMat6 &a );

    public idMat6 oMulSet(final idMat6 a) {
        this.oSet(this.oMultiply(a));
        return this;
    }
//public	idMat6 &		operator+=( const idMat6 &a );

    public idMat6 oPluSet(final idMat6 a) {
        mat[0].p[0] += a.mat[0].p[0];
        mat[0].p[1] += a.mat[0].p[1];
        mat[0].p[2] += a.mat[0].p[2];
        mat[0].p[3] += a.mat[0].p[3];
        mat[0].p[4] += a.mat[0].p[4];
        mat[0].p[5] += a.mat[0].p[5];
        mat[1].p[0] += a.mat[1].p[0];
        mat[1].p[1] += a.mat[1].p[1];
        mat[1].p[2] += a.mat[1].p[2];
        mat[1].p[3] += a.mat[1].p[3];
        mat[1].p[4] += a.mat[1].p[4];
        mat[1].p[5] += a.mat[1].p[5];
        mat[2].p[0] += a.mat[2].p[0];
        mat[2].p[1] += a.mat[2].p[1];
        mat[2].p[2] += a.mat[2].p[2];
        mat[2].p[3] += a.mat[2].p[3];
        mat[2].p[4] += a.mat[2].p[4];
        mat[2].p[5] += a.mat[2].p[5];
        mat[3].p[0] += a.mat[3].p[0];
        mat[3].p[1] += a.mat[3].p[1];
        mat[3].p[2] += a.mat[3].p[2];
        mat[3].p[3] += a.mat[3].p[3];
        mat[3].p[4] += a.mat[3].p[4];
        mat[3].p[5] += a.mat[3].p[5];
        mat[4].p[0] += a.mat[4].p[0];
        mat[4].p[1] += a.mat[4].p[1];
        mat[4].p[2] += a.mat[4].p[2];
        mat[4].p[3] += a.mat[4].p[3];
        mat[4].p[4] += a.mat[4].p[4];
        mat[4].p[5] += a.mat[4].p[5];
        mat[5].p[0] += a.mat[5].p[0];
        mat[5].p[1] += a.mat[5].p[1];
        mat[5].p[2] += a.mat[5].p[2];
        mat[5].p[3] += a.mat[5].p[3];
        mat[5].p[4] += a.mat[5].p[4];
        mat[5].p[5] += a.mat[5].p[5];
        return this;
    }
//public	idMat6 &		operator-=( const idMat6 &a );

    public idMat6 oMinSet(final idMat6 a) {
        mat[0].p[0] -= a.mat[0].p[0];
        mat[0].p[1] -= a.mat[0].p[1];
        mat[0].p[2] -= a.mat[0].p[2];
        mat[0].p[3] -= a.mat[0].p[3];
        mat[0].p[4] -= a.mat[0].p[4];
        mat[0].p[5] -= a.mat[0].p[5];
        mat[1].p[0] -= a.mat[1].p[0];
        mat[1].p[1] -= a.mat[1].p[1];
        mat[1].p[2] -= a.mat[1].p[2];
        mat[1].p[3] -= a.mat[1].p[3];
        mat[1].p[4] -= a.mat[1].p[4];
        mat[1].p[5] -= a.mat[1].p[5];
        mat[2].p[0] -= a.mat[2].p[0];
        mat[2].p[1] -= a.mat[2].p[1];
        mat[2].p[2] -= a.mat[2].p[2];
        mat[2].p[3] -= a.mat[2].p[3];
        mat[2].p[4] -= a.mat[2].p[4];
        mat[2].p[5] -= a.mat[2].p[5];
        mat[3].p[0] -= a.mat[3].p[0];
        mat[3].p[1] -= a.mat[3].p[1];
        mat[3].p[2] -= a.mat[3].p[2];
        mat[3].p[3] -= a.mat[3].p[3];
        mat[3].p[4] -= a.mat[3].p[4];
        mat[3].p[5] -= a.mat[3].p[5];
        mat[4].p[0] -= a.mat[4].p[0];
        mat[4].p[1] -= a.mat[4].p[1];
        mat[4].p[2] -= a.mat[4].p[2];
        mat[4].p[3] -= a.mat[4].p[3];
        mat[4].p[4] -= a.mat[4].p[4];
        mat[4].p[5] -= a.mat[4].p[5];
        mat[5].p[0] -= a.mat[5].p[0];
        mat[5].p[1] -= a.mat[5].p[1];
        mat[5].p[2] -= a.mat[5].p[2];
        mat[5].p[3] -= a.mat[5].p[3];
        mat[5].p[4] -= a.mat[5].p[4];
        mat[5].p[5] -= a.mat[5].p[5];
        return this;
    }

//public	friend idMat6	operator*( const float a, const idMat6 &mat );
    public static idMat6 oMultiply(final float a, final idMat6 mat) {
        return mat.oMultiply(a);
    }
//public	friend idVec6	operator*( const idVec6 &vec, const idMat6 &mat );

    public static idVec6 oMultiply(final idVec6 vec, final idMat6 mat) {
        return mat.oMultiply(vec);
    }
//public	friend idVec6 &	operator*=( idVec6 &vec, const idMat6 &mat );

    public static idVec6 oMulSet(idVec6 vec, final idMat6 mat) {
        vec = mat.oMultiply(vec);
        return vec;
    }

    public boolean Compare(final idMat6 a) {// exact compare, no epsilon
        int i;
        final float[] ptr1, ptr2;

        ptr1 = this.reinterpret_cast();
        ptr2 = a.reinterpret_cast();
        for (i = 0; i < 6 * 6; i++) {
            if (ptr1[i] != ptr2[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean Compare(final idMat6 a, final float epsilon) {// compare with epsilon
        int i;
        final float[] ptr1, ptr2;

        ptr1 = this.reinterpret_cast();
        ptr2 = a.reinterpret_cast();
        for (i = 0; i < 6 * 6; i++) {
            if (idMath.Fabs(ptr1[i] - ptr2[i]) > epsilon) {
                return false;
            }
        }
        return true;
    }
//public	bool			operator==( const idMat6 &a ) const;					// exact compare, no epsilon
//public	bool			operator!=( const idMat6 &a ) const;					// exact compare, no epsilon

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + Arrays.deepHashCode(this.mat);
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
        final idMat6 other = (idMat6) obj;
        if (!Arrays.deepEquals(this.mat, other.mat)) {
            return false;
        }
        return true;
    }

    public void Zero() {
        this.oSet(getMat6_zero());
    }

    public void Identity() {
        this.oSet(getMat6_zero());
    }

    public boolean IsIdentity() {
        return IsIdentity((float) MATRIX_EPSILON);
    }

    public boolean IsIdentity(final float epsilon) {
        return Compare(getMat6_identity(), epsilon);
    }

    public boolean IsSymmetric() {
        return IsSymmetric((float) MATRIX_EPSILON);
    }

    public boolean IsSymmetric(final float epsilon) {
        for (int i = 1; i < 6; i++) {
            for (int j = 0; j < i; j++) {
                if (idMath.Fabs(mat[i].p[j] - mat[j].p[i]) > epsilon) {
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
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (i != j && idMath.Fabs(mat[i].p[j]) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }

    public idMat3 SubMat3(int n) {
        assert (n >= 0 && n < 4);
        int b0 = ((n & 2) >> 1) * 3;
        int b1 = (n & 1) * 3;
        return new idMat3(
                mat[b0 + 0].p[b1 + 0], mat[b0 + 0].p[b1 + 1], mat[b0 + 0].p[b1 + 2],
                mat[b0 + 1].p[b1 + 0], mat[b0 + 1].p[b1 + 1], mat[b0 + 1].p[b1 + 2],
                mat[b0 + 2].p[b1 + 0], mat[b0 + 2].p[b1 + 1], mat[b0 + 2].p[b1 + 2]);
    }

    public float Trace() {
        return (mat[0].p[0] + mat[1].p[1] + mat[2].p[2] + mat[3].p[3] + mat[4].p[4] + mat[5].p[5]);
    }

    public float Determinant() {
        // 2x2 sub-determinants required to calculate 6x6 determinant
        float det2_45_01 = mat[4].p[0] * mat[5].p[1] - mat[4].p[1] * mat[5].p[0];
        float det2_45_02 = mat[4].p[0] * mat[5].p[2] - mat[4].p[2] * mat[5].p[0];
        float det2_45_03 = mat[4].p[0] * mat[5].p[3] - mat[4].p[3] * mat[5].p[0];
        float det2_45_04 = mat[4].p[0] * mat[5].p[4] - mat[4].p[4] * mat[5].p[0];
        float det2_45_05 = mat[4].p[0] * mat[5].p[5] - mat[4].p[5] * mat[5].p[0];
        float det2_45_12 = mat[4].p[1] * mat[5].p[2] - mat[4].p[2] * mat[5].p[1];
        float det2_45_13 = mat[4].p[1] * mat[5].p[3] - mat[4].p[3] * mat[5].p[1];
        float det2_45_14 = mat[4].p[1] * mat[5].p[4] - mat[4].p[4] * mat[5].p[1];
        float det2_45_15 = mat[4].p[1] * mat[5].p[5] - mat[4].p[5] * mat[5].p[1];
        float det2_45_23 = mat[4].p[2] * mat[5].p[3] - mat[4].p[3] * mat[5].p[2];
        float det2_45_24 = mat[4].p[2] * mat[5].p[4] - mat[4].p[4] * mat[5].p[2];
        float det2_45_25 = mat[4].p[2] * mat[5].p[5] - mat[4].p[5] * mat[5].p[2];
        float det2_45_34 = mat[4].p[3] * mat[5].p[4] - mat[4].p[4] * mat[5].p[3];
        float det2_45_35 = mat[4].p[3] * mat[5].p[5] - mat[4].p[5] * mat[5].p[3];
        float det2_45_45 = mat[4].p[4] * mat[5].p[5] - mat[4].p[5] * mat[5].p[4];

        // 3x3 sub-determinants required to calculate 6x6 determinant
        float det3_345_012 = mat[3].p[0] * det2_45_12 - mat[3].p[1] * det2_45_02 + mat[3].p[2] * det2_45_01;
        float det3_345_013 = mat[3].p[0] * det2_45_13 - mat[3].p[1] * det2_45_03 + mat[3].p[3] * det2_45_01;
        float det3_345_014 = mat[3].p[0] * det2_45_14 - mat[3].p[1] * det2_45_04 + mat[3].p[4] * det2_45_01;
        float det3_345_015 = mat[3].p[0] * det2_45_15 - mat[3].p[1] * det2_45_05 + mat[3].p[5] * det2_45_01;
        float det3_345_023 = mat[3].p[0] * det2_45_23 - mat[3].p[2] * det2_45_03 + mat[3].p[3] * det2_45_02;
        float det3_345_024 = mat[3].p[0] * det2_45_24 - mat[3].p[2] * det2_45_04 + mat[3].p[4] * det2_45_02;
        float det3_345_025 = mat[3].p[0] * det2_45_25 - mat[3].p[2] * det2_45_05 + mat[3].p[5] * det2_45_02;
        float det3_345_034 = mat[3].p[0] * det2_45_34 - mat[3].p[3] * det2_45_04 + mat[3].p[4] * det2_45_03;
        float det3_345_035 = mat[3].p[0] * det2_45_35 - mat[3].p[3] * det2_45_05 + mat[3].p[5] * det2_45_03;
        float det3_345_045 = mat[3].p[0] * det2_45_45 - mat[3].p[4] * det2_45_05 + mat[3].p[5] * det2_45_04;
        float det3_345_123 = mat[3].p[1] * det2_45_23 - mat[3].p[2] * det2_45_13 + mat[3].p[3] * det2_45_12;
        float det3_345_124 = mat[3].p[1] * det2_45_24 - mat[3].p[2] * det2_45_14 + mat[3].p[4] * det2_45_12;
        float det3_345_125 = mat[3].p[1] * det2_45_25 - mat[3].p[2] * det2_45_15 + mat[3].p[5] * det2_45_12;
        float det3_345_134 = mat[3].p[1] * det2_45_34 - mat[3].p[3] * det2_45_14 + mat[3].p[4] * det2_45_13;
        float det3_345_135 = mat[3].p[1] * det2_45_35 - mat[3].p[3] * det2_45_15 + mat[3].p[5] * det2_45_13;
        float det3_345_145 = mat[3].p[1] * det2_45_45 - mat[3].p[4] * det2_45_15 + mat[3].p[5] * det2_45_14;
        float det3_345_234 = mat[3].p[2] * det2_45_34 - mat[3].p[3] * det2_45_24 + mat[3].p[4] * det2_45_23;
        float det3_345_235 = mat[3].p[2] * det2_45_35 - mat[3].p[3] * det2_45_25 + mat[3].p[5] * det2_45_23;
        float det3_345_245 = mat[3].p[2] * det2_45_45 - mat[3].p[4] * det2_45_25 + mat[3].p[5] * det2_45_24;
        float det3_345_345 = mat[3].p[3] * det2_45_45 - mat[3].p[4] * det2_45_35 + mat[3].p[5] * det2_45_34;

        // 4x4 sub-determinants required to calculate 6x6 determinant
        float det4_2345_0123 = mat[2].p[0] * det3_345_123 - mat[2].p[1] * det3_345_023 + mat[2].p[2] * det3_345_013 - mat[2].p[3] * det3_345_012;
        float det4_2345_0124 = mat[2].p[0] * det3_345_124 - mat[2].p[1] * det3_345_024 + mat[2].p[2] * det3_345_014 - mat[2].p[4] * det3_345_012;
        float det4_2345_0125 = mat[2].p[0] * det3_345_125 - mat[2].p[1] * det3_345_025 + mat[2].p[2] * det3_345_015 - mat[2].p[5] * det3_345_012;
        float det4_2345_0134 = mat[2].p[0] * det3_345_134 - mat[2].p[1] * det3_345_034 + mat[2].p[3] * det3_345_014 - mat[2].p[4] * det3_345_013;
        float det4_2345_0135 = mat[2].p[0] * det3_345_135 - mat[2].p[1] * det3_345_035 + mat[2].p[3] * det3_345_015 - mat[2].p[5] * det3_345_013;
        float det4_2345_0145 = mat[2].p[0] * det3_345_145 - mat[2].p[1] * det3_345_045 + mat[2].p[4] * det3_345_015 - mat[2].p[5] * det3_345_014;
        float det4_2345_0234 = mat[2].p[0] * det3_345_234 - mat[2].p[2] * det3_345_034 + mat[2].p[3] * det3_345_024 - mat[2].p[4] * det3_345_023;
        float det4_2345_0235 = mat[2].p[0] * det3_345_235 - mat[2].p[2] * det3_345_035 + mat[2].p[3] * det3_345_025 - mat[2].p[5] * det3_345_023;
        float det4_2345_0245 = mat[2].p[0] * det3_345_245 - mat[2].p[2] * det3_345_045 + mat[2].p[4] * det3_345_025 - mat[2].p[5] * det3_345_024;
        float det4_2345_0345 = mat[2].p[0] * det3_345_345 - mat[2].p[3] * det3_345_045 + mat[2].p[4] * det3_345_035 - mat[2].p[5] * det3_345_034;
        float det4_2345_1234 = mat[2].p[1] * det3_345_234 - mat[2].p[2] * det3_345_134 + mat[2].p[3] * det3_345_124 - mat[2].p[4] * det3_345_123;
        float det4_2345_1235 = mat[2].p[1] * det3_345_235 - mat[2].p[2] * det3_345_135 + mat[2].p[3] * det3_345_125 - mat[2].p[5] * det3_345_123;
        float det4_2345_1245 = mat[2].p[1] * det3_345_245 - mat[2].p[2] * det3_345_145 + mat[2].p[4] * det3_345_125 - mat[2].p[5] * det3_345_124;
        float det4_2345_1345 = mat[2].p[1] * det3_345_345 - mat[2].p[3] * det3_345_145 + mat[2].p[4] * det3_345_135 - mat[2].p[5] * det3_345_134;
        float det4_2345_2345 = mat[2].p[2] * det3_345_345 - mat[2].p[3] * det3_345_245 + mat[2].p[4] * det3_345_235 - mat[2].p[5] * det3_345_234;

        // 5x5 sub-determinants required to calculate 6x6 determinant
        float det5_12345_01234 = mat[1].p[0] * det4_2345_1234 - mat[1].p[1] * det4_2345_0234 + mat[1].p[2] * det4_2345_0134 - mat[1].p[3] * det4_2345_0124 + mat[1].p[4] * det4_2345_0123;
        float det5_12345_01235 = mat[1].p[0] * det4_2345_1235 - mat[1].p[1] * det4_2345_0235 + mat[1].p[2] * det4_2345_0135 - mat[1].p[3] * det4_2345_0125 + mat[1].p[5] * det4_2345_0123;
        float det5_12345_01245 = mat[1].p[0] * det4_2345_1245 - mat[1].p[1] * det4_2345_0245 + mat[1].p[2] * det4_2345_0145 - mat[1].p[4] * det4_2345_0125 + mat[1].p[5] * det4_2345_0124;
        float det5_12345_01345 = mat[1].p[0] * det4_2345_1345 - mat[1].p[1] * det4_2345_0345 + mat[1].p[3] * det4_2345_0145 - mat[1].p[4] * det4_2345_0135 + mat[1].p[5] * det4_2345_0134;
        float det5_12345_02345 = mat[1].p[0] * det4_2345_2345 - mat[1].p[2] * det4_2345_0345 + mat[1].p[3] * det4_2345_0245 - mat[1].p[4] * det4_2345_0235 + mat[1].p[5] * det4_2345_0234;
        float det5_12345_12345 = mat[1].p[1] * det4_2345_2345 - mat[1].p[2] * det4_2345_1345 + mat[1].p[3] * det4_2345_1245 - mat[1].p[4] * det4_2345_1235 + mat[1].p[5] * det4_2345_1234;

        // determinant of 6x6 matrix
        return mat[0].p[0] * det5_12345_12345 - mat[0].p[1] * det5_12345_02345 + mat[0].p[2] * det5_12345_01345
                - mat[0].p[3] * det5_12345_01245 + mat[0].p[4] * det5_12345_01235 - mat[0].p[5] * det5_12345_01234;
    }

    public idMat6 Transpose() {// returns transpose
        idMat6 transpose = new idMat6();
        int i, j;

        for (i = 0; i < 6; i++) {
            for (j = 0; j < 6; j++) {
                transpose.mat[ i].p[ j] = this.mat[ j].p[ i];
            }
        }
        return transpose;
    }

    public idMat6 TransposeSelf() {
        float temp;
        int i, j;

        for (i = 0; i < 6; i++) {
            for (j = i + 1; j < 6; j++) {
                temp = mat[ i].p[ j];
                mat[ i].p[ j] = mat[ j].p[ i];
                mat[ j].p[ i] = temp;
            }
        }
        return this;
    }

    public idMat6 Inverse() {// returns the inverse ( m * m.Inverse() = identity )
        idMat6 invMat;

        invMat = this;
        boolean r = invMat.InverseSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseSelf() {// returns false if determinant is zero
        // 810+6+36 = 852 multiplications
        //				1 division
        double det, invDet;

        // 2x2 sub-determinants required to calculate 6x6 determinant
        float det2_45_01 = mat[4].p[0] * mat[5].p[1] - mat[4].p[1] * mat[5].p[0];
        float det2_45_02 = mat[4].p[0] * mat[5].p[2] - mat[4].p[2] * mat[5].p[0];
        float det2_45_03 = mat[4].p[0] * mat[5].p[3] - mat[4].p[3] * mat[5].p[0];
        float det2_45_04 = mat[4].p[0] * mat[5].p[4] - mat[4].p[4] * mat[5].p[0];
        float det2_45_05 = mat[4].p[0] * mat[5].p[5] - mat[4].p[5] * mat[5].p[0];
        float det2_45_12 = mat[4].p[1] * mat[5].p[2] - mat[4].p[2] * mat[5].p[1];
        float det2_45_13 = mat[4].p[1] * mat[5].p[3] - mat[4].p[3] * mat[5].p[1];
        float det2_45_14 = mat[4].p[1] * mat[5].p[4] - mat[4].p[4] * mat[5].p[1];
        float det2_45_15 = mat[4].p[1] * mat[5].p[5] - mat[4].p[5] * mat[5].p[1];
        float det2_45_23 = mat[4].p[2] * mat[5].p[3] - mat[4].p[3] * mat[5].p[2];
        float det2_45_24 = mat[4].p[2] * mat[5].p[4] - mat[4].p[4] * mat[5].p[2];
        float det2_45_25 = mat[4].p[2] * mat[5].p[5] - mat[4].p[5] * mat[5].p[2];
        float det2_45_34 = mat[4].p[3] * mat[5].p[4] - mat[4].p[4] * mat[5].p[3];
        float det2_45_35 = mat[4].p[3] * mat[5].p[5] - mat[4].p[5] * mat[5].p[3];
        float det2_45_45 = mat[4].p[4] * mat[5].p[5] - mat[4].p[5] * mat[5].p[4];

        // 3x3 sub-determinants required to calculate 6x6 determinant
        float det3_345_012 = mat[3].p[0] * det2_45_12 - mat[3].p[1] * det2_45_02 + mat[3].p[2] * det2_45_01;
        float det3_345_013 = mat[3].p[0] * det2_45_13 - mat[3].p[1] * det2_45_03 + mat[3].p[3] * det2_45_01;
        float det3_345_014 = mat[3].p[0] * det2_45_14 - mat[3].p[1] * det2_45_04 + mat[3].p[4] * det2_45_01;
        float det3_345_015 = mat[3].p[0] * det2_45_15 - mat[3].p[1] * det2_45_05 + mat[3].p[5] * det2_45_01;
        float det3_345_023 = mat[3].p[0] * det2_45_23 - mat[3].p[2] * det2_45_03 + mat[3].p[3] * det2_45_02;
        float det3_345_024 = mat[3].p[0] * det2_45_24 - mat[3].p[2] * det2_45_04 + mat[3].p[4] * det2_45_02;
        float det3_345_025 = mat[3].p[0] * det2_45_25 - mat[3].p[2] * det2_45_05 + mat[3].p[5] * det2_45_02;
        float det3_345_034 = mat[3].p[0] * det2_45_34 - mat[3].p[3] * det2_45_04 + mat[3].p[4] * det2_45_03;
        float det3_345_035 = mat[3].p[0] * det2_45_35 - mat[3].p[3] * det2_45_05 + mat[3].p[5] * det2_45_03;
        float det3_345_045 = mat[3].p[0] * det2_45_45 - mat[3].p[4] * det2_45_05 + mat[3].p[5] * det2_45_04;
        float det3_345_123 = mat[3].p[1] * det2_45_23 - mat[3].p[2] * det2_45_13 + mat[3].p[3] * det2_45_12;
        float det3_345_124 = mat[3].p[1] * det2_45_24 - mat[3].p[2] * det2_45_14 + mat[3].p[4] * det2_45_12;
        float det3_345_125 = mat[3].p[1] * det2_45_25 - mat[3].p[2] * det2_45_15 + mat[3].p[5] * det2_45_12;
        float det3_345_134 = mat[3].p[1] * det2_45_34 - mat[3].p[3] * det2_45_14 + mat[3].p[4] * det2_45_13;
        float det3_345_135 = mat[3].p[1] * det2_45_35 - mat[3].p[3] * det2_45_15 + mat[3].p[5] * det2_45_13;
        float det3_345_145 = mat[3].p[1] * det2_45_45 - mat[3].p[4] * det2_45_15 + mat[3].p[5] * det2_45_14;
        float det3_345_234 = mat[3].p[2] * det2_45_34 - mat[3].p[3] * det2_45_24 + mat[3].p[4] * det2_45_23;
        float det3_345_235 = mat[3].p[2] * det2_45_35 - mat[3].p[3] * det2_45_25 + mat[3].p[5] * det2_45_23;
        float det3_345_245 = mat[3].p[2] * det2_45_45 - mat[3].p[4] * det2_45_25 + mat[3].p[5] * det2_45_24;
        float det3_345_345 = mat[3].p[3] * det2_45_45 - mat[3].p[4] * det2_45_35 + mat[3].p[5] * det2_45_34;

        // 4x4 sub-determinants required to calculate 6x6 determinant
        float det4_2345_0123 = mat[2].p[0] * det3_345_123 - mat[2].p[1] * det3_345_023 + mat[2].p[2] * det3_345_013 - mat[2].p[3] * det3_345_012;
        float det4_2345_0124 = mat[2].p[0] * det3_345_124 - mat[2].p[1] * det3_345_024 + mat[2].p[2] * det3_345_014 - mat[2].p[4] * det3_345_012;
        float det4_2345_0125 = mat[2].p[0] * det3_345_125 - mat[2].p[1] * det3_345_025 + mat[2].p[2] * det3_345_015 - mat[2].p[5] * det3_345_012;
        float det4_2345_0134 = mat[2].p[0] * det3_345_134 - mat[2].p[1] * det3_345_034 + mat[2].p[3] * det3_345_014 - mat[2].p[4] * det3_345_013;
        float det4_2345_0135 = mat[2].p[0] * det3_345_135 - mat[2].p[1] * det3_345_035 + mat[2].p[3] * det3_345_015 - mat[2].p[5] * det3_345_013;
        float det4_2345_0145 = mat[2].p[0] * det3_345_145 - mat[2].p[1] * det3_345_045 + mat[2].p[4] * det3_345_015 - mat[2].p[5] * det3_345_014;
        float det4_2345_0234 = mat[2].p[0] * det3_345_234 - mat[2].p[2] * det3_345_034 + mat[2].p[3] * det3_345_024 - mat[2].p[4] * det3_345_023;
        float det4_2345_0235 = mat[2].p[0] * det3_345_235 - mat[2].p[2] * det3_345_035 + mat[2].p[3] * det3_345_025 - mat[2].p[5] * det3_345_023;
        float det4_2345_0245 = mat[2].p[0] * det3_345_245 - mat[2].p[2] * det3_345_045 + mat[2].p[4] * det3_345_025 - mat[2].p[5] * det3_345_024;
        float det4_2345_0345 = mat[2].p[0] * det3_345_345 - mat[2].p[3] * det3_345_045 + mat[2].p[4] * det3_345_035 - mat[2].p[5] * det3_345_034;
        float det4_2345_1234 = mat[2].p[1] * det3_345_234 - mat[2].p[2] * det3_345_134 + mat[2].p[3] * det3_345_124 - mat[2].p[4] * det3_345_123;
        float det4_2345_1235 = mat[2].p[1] * det3_345_235 - mat[2].p[2] * det3_345_135 + mat[2].p[3] * det3_345_125 - mat[2].p[5] * det3_345_123;
        float det4_2345_1245 = mat[2].p[1] * det3_345_245 - mat[2].p[2] * det3_345_145 + mat[2].p[4] * det3_345_125 - mat[2].p[5] * det3_345_124;
        float det4_2345_1345 = mat[2].p[1] * det3_345_345 - mat[2].p[3] * det3_345_145 + mat[2].p[4] * det3_345_135 - mat[2].p[5] * det3_345_134;
        float det4_2345_2345 = mat[2].p[2] * det3_345_345 - mat[2].p[3] * det3_345_245 + mat[2].p[4] * det3_345_235 - mat[2].p[5] * det3_345_234;

        // 5x5 sub-determinants required to calculate 6x6 determinant
        float det5_12345_01234 = mat[1].p[0] * det4_2345_1234 - mat[1].p[1] * det4_2345_0234 + mat[1].p[2] * det4_2345_0134 - mat[1].p[3] * det4_2345_0124 + mat[1].p[4] * det4_2345_0123;
        float det5_12345_01235 = mat[1].p[0] * det4_2345_1235 - mat[1].p[1] * det4_2345_0235 + mat[1].p[2] * det4_2345_0135 - mat[1].p[3] * det4_2345_0125 + mat[1].p[5] * det4_2345_0123;
        float det5_12345_01245 = mat[1].p[0] * det4_2345_1245 - mat[1].p[1] * det4_2345_0245 + mat[1].p[2] * det4_2345_0145 - mat[1].p[4] * det4_2345_0125 + mat[1].p[5] * det4_2345_0124;
        float det5_12345_01345 = mat[1].p[0] * det4_2345_1345 - mat[1].p[1] * det4_2345_0345 + mat[1].p[3] * det4_2345_0145 - mat[1].p[4] * det4_2345_0135 + mat[1].p[5] * det4_2345_0134;
        float det5_12345_02345 = mat[1].p[0] * det4_2345_2345 - mat[1].p[2] * det4_2345_0345 + mat[1].p[3] * det4_2345_0245 - mat[1].p[4] * det4_2345_0235 + mat[1].p[5] * det4_2345_0234;
        float det5_12345_12345 = mat[1].p[1] * det4_2345_2345 - mat[1].p[2] * det4_2345_1345 + mat[1].p[3] * det4_2345_1245 - mat[1].p[4] * det4_2345_1235 + mat[1].p[5] * det4_2345_1234;

        // determinant of 6x6 matrix
        det = mat[0].p[0] * det5_12345_12345 - mat[0].p[1] * det5_12345_02345 + mat[0].p[2] * det5_12345_01345
                - mat[0].p[3] * det5_12345_01245 + mat[0].p[4] * det5_12345_01235 - mat[0].p[5] * det5_12345_01234;

        if (idMath.Fabs((float) det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        // remaining 2x2 sub-determinants
        float det2_34_01 = mat[3].p[0] * mat[4].p[1] - mat[3].p[1] * mat[4].p[0];
        float det2_34_02 = mat[3].p[0] * mat[4].p[2] - mat[3].p[2] * mat[4].p[0];
        float det2_34_03 = mat[3].p[0] * mat[4].p[3] - mat[3].p[3] * mat[4].p[0];
        float det2_34_04 = mat[3].p[0] * mat[4].p[4] - mat[3].p[4] * mat[4].p[0];
        float det2_34_05 = mat[3].p[0] * mat[4].p[5] - mat[3].p[5] * mat[4].p[0];
        float det2_34_12 = mat[3].p[1] * mat[4].p[2] - mat[3].p[2] * mat[4].p[1];
        float det2_34_13 = mat[3].p[1] * mat[4].p[3] - mat[3].p[3] * mat[4].p[1];
        float det2_34_14 = mat[3].p[1] * mat[4].p[4] - mat[3].p[4] * mat[4].p[1];
        float det2_34_15 = mat[3].p[1] * mat[4].p[5] - mat[3].p[5] * mat[4].p[1];
        float det2_34_23 = mat[3].p[2] * mat[4].p[3] - mat[3].p[3] * mat[4].p[2];
        float det2_34_24 = mat[3].p[2] * mat[4].p[4] - mat[3].p[4] * mat[4].p[2];
        float det2_34_25 = mat[3].p[2] * mat[4].p[5] - mat[3].p[5] * mat[4].p[2];
        float det2_34_34 = mat[3].p[3] * mat[4].p[4] - mat[3].p[4] * mat[4].p[3];
        float det2_34_35 = mat[3].p[3] * mat[4].p[5] - mat[3].p[5] * mat[4].p[3];
        float det2_34_45 = mat[3].p[4] * mat[4].p[5] - mat[3].p[5] * mat[4].p[4];
        float det2_35_01 = mat[3].p[0] * mat[5].p[1] - mat[3].p[1] * mat[5].p[0];
        float det2_35_02 = mat[3].p[0] * mat[5].p[2] - mat[3].p[2] * mat[5].p[0];
        float det2_35_03 = mat[3].p[0] * mat[5].p[3] - mat[3].p[3] * mat[5].p[0];
        float det2_35_04 = mat[3].p[0] * mat[5].p[4] - mat[3].p[4] * mat[5].p[0];
        float det2_35_05 = mat[3].p[0] * mat[5].p[5] - mat[3].p[5] * mat[5].p[0];
        float det2_35_12 = mat[3].p[1] * mat[5].p[2] - mat[3].p[2] * mat[5].p[1];
        float det2_35_13 = mat[3].p[1] * mat[5].p[3] - mat[3].p[3] * mat[5].p[1];
        float det2_35_14 = mat[3].p[1] * mat[5].p[4] - mat[3].p[4] * mat[5].p[1];
        float det2_35_15 = mat[3].p[1] * mat[5].p[5] - mat[3].p[5] * mat[5].p[1];
        float det2_35_23 = mat[3].p[2] * mat[5].p[3] - mat[3].p[3] * mat[5].p[2];
        float det2_35_24 = mat[3].p[2] * mat[5].p[4] - mat[3].p[4] * mat[5].p[2];
        float det2_35_25 = mat[3].p[2] * mat[5].p[5] - mat[3].p[5] * mat[5].p[2];
        float det2_35_34 = mat[3].p[3] * mat[5].p[4] - mat[3].p[4] * mat[5].p[3];
        float det2_35_35 = mat[3].p[3] * mat[5].p[5] - mat[3].p[5] * mat[5].p[3];
        float det2_35_45 = mat[3].p[4] * mat[5].p[5] - mat[3].p[5] * mat[5].p[4];

        // remaining 3x3 sub-determinants
        float det3_234_012 = mat[2].p[0] * det2_34_12 - mat[2].p[1] * det2_34_02 + mat[2].p[2] * det2_34_01;
        float det3_234_013 = mat[2].p[0] * det2_34_13 - mat[2].p[1] * det2_34_03 + mat[2].p[3] * det2_34_01;
        float det3_234_014 = mat[2].p[0] * det2_34_14 - mat[2].p[1] * det2_34_04 + mat[2].p[4] * det2_34_01;
        float det3_234_015 = mat[2].p[0] * det2_34_15 - mat[2].p[1] * det2_34_05 + mat[2].p[5] * det2_34_01;
        float det3_234_023 = mat[2].p[0] * det2_34_23 - mat[2].p[2] * det2_34_03 + mat[2].p[3] * det2_34_02;
        float det3_234_024 = mat[2].p[0] * det2_34_24 - mat[2].p[2] * det2_34_04 + mat[2].p[4] * det2_34_02;
        float det3_234_025 = mat[2].p[0] * det2_34_25 - mat[2].p[2] * det2_34_05 + mat[2].p[5] * det2_34_02;
        float det3_234_034 = mat[2].p[0] * det2_34_34 - mat[2].p[3] * det2_34_04 + mat[2].p[4] * det2_34_03;
        float det3_234_035 = mat[2].p[0] * det2_34_35 - mat[2].p[3] * det2_34_05 + mat[2].p[5] * det2_34_03;
        float det3_234_045 = mat[2].p[0] * det2_34_45 - mat[2].p[4] * det2_34_05 + mat[2].p[5] * det2_34_04;
        float det3_234_123 = mat[2].p[1] * det2_34_23 - mat[2].p[2] * det2_34_13 + mat[2].p[3] * det2_34_12;
        float det3_234_124 = mat[2].p[1] * det2_34_24 - mat[2].p[2] * det2_34_14 + mat[2].p[4] * det2_34_12;
        float det3_234_125 = mat[2].p[1] * det2_34_25 - mat[2].p[2] * det2_34_15 + mat[2].p[5] * det2_34_12;
        float det3_234_134 = mat[2].p[1] * det2_34_34 - mat[2].p[3] * det2_34_14 + mat[2].p[4] * det2_34_13;
        float det3_234_135 = mat[2].p[1] * det2_34_35 - mat[2].p[3] * det2_34_15 + mat[2].p[5] * det2_34_13;
        float det3_234_145 = mat[2].p[1] * det2_34_45 - mat[2].p[4] * det2_34_15 + mat[2].p[5] * det2_34_14;
        float det3_234_234 = mat[2].p[2] * det2_34_34 - mat[2].p[3] * det2_34_24 + mat[2].p[4] * det2_34_23;
        float det3_234_235 = mat[2].p[2] * det2_34_35 - mat[2].p[3] * det2_34_25 + mat[2].p[5] * det2_34_23;
        float det3_234_245 = mat[2].p[2] * det2_34_45 - mat[2].p[4] * det2_34_25 + mat[2].p[5] * det2_34_24;
        float det3_234_345 = mat[2].p[3] * det2_34_45 - mat[2].p[4] * det2_34_35 + mat[2].p[5] * det2_34_34;
        float det3_235_012 = mat[2].p[0] * det2_35_12 - mat[2].p[1] * det2_35_02 + mat[2].p[2] * det2_35_01;
        float det3_235_013 = mat[2].p[0] * det2_35_13 - mat[2].p[1] * det2_35_03 + mat[2].p[3] * det2_35_01;
        float det3_235_014 = mat[2].p[0] * det2_35_14 - mat[2].p[1] * det2_35_04 + mat[2].p[4] * det2_35_01;
        float det3_235_015 = mat[2].p[0] * det2_35_15 - mat[2].p[1] * det2_35_05 + mat[2].p[5] * det2_35_01;
        float det3_235_023 = mat[2].p[0] * det2_35_23 - mat[2].p[2] * det2_35_03 + mat[2].p[3] * det2_35_02;
        float det3_235_024 = mat[2].p[0] * det2_35_24 - mat[2].p[2] * det2_35_04 + mat[2].p[4] * det2_35_02;
        float det3_235_025 = mat[2].p[0] * det2_35_25 - mat[2].p[2] * det2_35_05 + mat[2].p[5] * det2_35_02;
        float det3_235_034 = mat[2].p[0] * det2_35_34 - mat[2].p[3] * det2_35_04 + mat[2].p[4] * det2_35_03;
        float det3_235_035 = mat[2].p[0] * det2_35_35 - mat[2].p[3] * det2_35_05 + mat[2].p[5] * det2_35_03;
        float det3_235_045 = mat[2].p[0] * det2_35_45 - mat[2].p[4] * det2_35_05 + mat[2].p[5] * det2_35_04;
        float det3_235_123 = mat[2].p[1] * det2_35_23 - mat[2].p[2] * det2_35_13 + mat[2].p[3] * det2_35_12;
        float det3_235_124 = mat[2].p[1] * det2_35_24 - mat[2].p[2] * det2_35_14 + mat[2].p[4] * det2_35_12;
        float det3_235_125 = mat[2].p[1] * det2_35_25 - mat[2].p[2] * det2_35_15 + mat[2].p[5] * det2_35_12;
        float det3_235_134 = mat[2].p[1] * det2_35_34 - mat[2].p[3] * det2_35_14 + mat[2].p[4] * det2_35_13;
        float det3_235_135 = mat[2].p[1] * det2_35_35 - mat[2].p[3] * det2_35_15 + mat[2].p[5] * det2_35_13;
        float det3_235_145 = mat[2].p[1] * det2_35_45 - mat[2].p[4] * det2_35_15 + mat[2].p[5] * det2_35_14;
        float det3_235_234 = mat[2].p[2] * det2_35_34 - mat[2].p[3] * det2_35_24 + mat[2].p[4] * det2_35_23;
        float det3_235_235 = mat[2].p[2] * det2_35_35 - mat[2].p[3] * det2_35_25 + mat[2].p[5] * det2_35_23;
        float det3_235_245 = mat[2].p[2] * det2_35_45 - mat[2].p[4] * det2_35_25 + mat[2].p[5] * det2_35_24;
        float det3_235_345 = mat[2].p[3] * det2_35_45 - mat[2].p[4] * det2_35_35 + mat[2].p[5] * det2_35_34;
        float det3_245_012 = mat[2].p[0] * det2_45_12 - mat[2].p[1] * det2_45_02 + mat[2].p[2] * det2_45_01;
        float det3_245_013 = mat[2].p[0] * det2_45_13 - mat[2].p[1] * det2_45_03 + mat[2].p[3] * det2_45_01;
        float det3_245_014 = mat[2].p[0] * det2_45_14 - mat[2].p[1] * det2_45_04 + mat[2].p[4] * det2_45_01;
        float det3_245_015 = mat[2].p[0] * det2_45_15 - mat[2].p[1] * det2_45_05 + mat[2].p[5] * det2_45_01;
        float det3_245_023 = mat[2].p[0] * det2_45_23 - mat[2].p[2] * det2_45_03 + mat[2].p[3] * det2_45_02;
        float det3_245_024 = mat[2].p[0] * det2_45_24 - mat[2].p[2] * det2_45_04 + mat[2].p[4] * det2_45_02;
        float det3_245_025 = mat[2].p[0] * det2_45_25 - mat[2].p[2] * det2_45_05 + mat[2].p[5] * det2_45_02;
        float det3_245_034 = mat[2].p[0] * det2_45_34 - mat[2].p[3] * det2_45_04 + mat[2].p[4] * det2_45_03;
        float det3_245_035 = mat[2].p[0] * det2_45_35 - mat[2].p[3] * det2_45_05 + mat[2].p[5] * det2_45_03;
        float det3_245_045 = mat[2].p[0] * det2_45_45 - mat[2].p[4] * det2_45_05 + mat[2].p[5] * det2_45_04;
        float det3_245_123 = mat[2].p[1] * det2_45_23 - mat[2].p[2] * det2_45_13 + mat[2].p[3] * det2_45_12;
        float det3_245_124 = mat[2].p[1] * det2_45_24 - mat[2].p[2] * det2_45_14 + mat[2].p[4] * det2_45_12;
        float det3_245_125 = mat[2].p[1] * det2_45_25 - mat[2].p[2] * det2_45_15 + mat[2].p[5] * det2_45_12;
        float det3_245_134 = mat[2].p[1] * det2_45_34 - mat[2].p[3] * det2_45_14 + mat[2].p[4] * det2_45_13;
        float det3_245_135 = mat[2].p[1] * det2_45_35 - mat[2].p[3] * det2_45_15 + mat[2].p[5] * det2_45_13;
        float det3_245_145 = mat[2].p[1] * det2_45_45 - mat[2].p[4] * det2_45_15 + mat[2].p[5] * det2_45_14;
        float det3_245_234 = mat[2].p[2] * det2_45_34 - mat[2].p[3] * det2_45_24 + mat[2].p[4] * det2_45_23;
        float det3_245_235 = mat[2].p[2] * det2_45_35 - mat[2].p[3] * det2_45_25 + mat[2].p[5] * det2_45_23;
        float det3_245_245 = mat[2].p[2] * det2_45_45 - mat[2].p[4] * det2_45_25 + mat[2].p[5] * det2_45_24;
        float det3_245_345 = mat[2].p[3] * det2_45_45 - mat[2].p[4] * det2_45_35 + mat[2].p[5] * det2_45_34;

        // remaining 4x4 sub-determinants
        float det4_1234_0123 = mat[1].p[0] * det3_234_123 - mat[1].p[1] * det3_234_023 + mat[1].p[2] * det3_234_013 - mat[1].p[3] * det3_234_012;
        float det4_1234_0124 = mat[1].p[0] * det3_234_124 - mat[1].p[1] * det3_234_024 + mat[1].p[2] * det3_234_014 - mat[1].p[4] * det3_234_012;
        float det4_1234_0125 = mat[1].p[0] * det3_234_125 - mat[1].p[1] * det3_234_025 + mat[1].p[2] * det3_234_015 - mat[1].p[5] * det3_234_012;
        float det4_1234_0134 = mat[1].p[0] * det3_234_134 - mat[1].p[1] * det3_234_034 + mat[1].p[3] * det3_234_014 - mat[1].p[4] * det3_234_013;
        float det4_1234_0135 = mat[1].p[0] * det3_234_135 - mat[1].p[1] * det3_234_035 + mat[1].p[3] * det3_234_015 - mat[1].p[5] * det3_234_013;
        float det4_1234_0145 = mat[1].p[0] * det3_234_145 - mat[1].p[1] * det3_234_045 + mat[1].p[4] * det3_234_015 - mat[1].p[5] * det3_234_014;
        float det4_1234_0234 = mat[1].p[0] * det3_234_234 - mat[1].p[2] * det3_234_034 + mat[1].p[3] * det3_234_024 - mat[1].p[4] * det3_234_023;
        float det4_1234_0235 = mat[1].p[0] * det3_234_235 - mat[1].p[2] * det3_234_035 + mat[1].p[3] * det3_234_025 - mat[1].p[5] * det3_234_023;
        float det4_1234_0245 = mat[1].p[0] * det3_234_245 - mat[1].p[2] * det3_234_045 + mat[1].p[4] * det3_234_025 - mat[1].p[5] * det3_234_024;
        float det4_1234_0345 = mat[1].p[0] * det3_234_345 - mat[1].p[3] * det3_234_045 + mat[1].p[4] * det3_234_035 - mat[1].p[5] * det3_234_034;
        float det4_1234_1234 = mat[1].p[1] * det3_234_234 - mat[1].p[2] * det3_234_134 + mat[1].p[3] * det3_234_124 - mat[1].p[4] * det3_234_123;
        float det4_1234_1235 = mat[1].p[1] * det3_234_235 - mat[1].p[2] * det3_234_135 + mat[1].p[3] * det3_234_125 - mat[1].p[5] * det3_234_123;
        float det4_1234_1245 = mat[1].p[1] * det3_234_245 - mat[1].p[2] * det3_234_145 + mat[1].p[4] * det3_234_125 - mat[1].p[5] * det3_234_124;
        float det4_1234_1345 = mat[1].p[1] * det3_234_345 - mat[1].p[3] * det3_234_145 + mat[1].p[4] * det3_234_135 - mat[1].p[5] * det3_234_134;
        float det4_1234_2345 = mat[1].p[2] * det3_234_345 - mat[1].p[3] * det3_234_245 + mat[1].p[4] * det3_234_235 - mat[1].p[5] * det3_234_234;
        float det4_1235_0123 = mat[1].p[0] * det3_235_123 - mat[1].p[1] * det3_235_023 + mat[1].p[2] * det3_235_013 - mat[1].p[3] * det3_235_012;
        float det4_1235_0124 = mat[1].p[0] * det3_235_124 - mat[1].p[1] * det3_235_024 + mat[1].p[2] * det3_235_014 - mat[1].p[4] * det3_235_012;
        float det4_1235_0125 = mat[1].p[0] * det3_235_125 - mat[1].p[1] * det3_235_025 + mat[1].p[2] * det3_235_015 - mat[1].p[5] * det3_235_012;
        float det4_1235_0134 = mat[1].p[0] * det3_235_134 - mat[1].p[1] * det3_235_034 + mat[1].p[3] * det3_235_014 - mat[1].p[4] * det3_235_013;
        float det4_1235_0135 = mat[1].p[0] * det3_235_135 - mat[1].p[1] * det3_235_035 + mat[1].p[3] * det3_235_015 - mat[1].p[5] * det3_235_013;
        float det4_1235_0145 = mat[1].p[0] * det3_235_145 - mat[1].p[1] * det3_235_045 + mat[1].p[4] * det3_235_015 - mat[1].p[5] * det3_235_014;
        float det4_1235_0234 = mat[1].p[0] * det3_235_234 - mat[1].p[2] * det3_235_034 + mat[1].p[3] * det3_235_024 - mat[1].p[4] * det3_235_023;
        float det4_1235_0235 = mat[1].p[0] * det3_235_235 - mat[1].p[2] * det3_235_035 + mat[1].p[3] * det3_235_025 - mat[1].p[5] * det3_235_023;
        float det4_1235_0245 = mat[1].p[0] * det3_235_245 - mat[1].p[2] * det3_235_045 + mat[1].p[4] * det3_235_025 - mat[1].p[5] * det3_235_024;
        float det4_1235_0345 = mat[1].p[0] * det3_235_345 - mat[1].p[3] * det3_235_045 + mat[1].p[4] * det3_235_035 - mat[1].p[5] * det3_235_034;
        float det4_1235_1234 = mat[1].p[1] * det3_235_234 - mat[1].p[2] * det3_235_134 + mat[1].p[3] * det3_235_124 - mat[1].p[4] * det3_235_123;
        float det4_1235_1235 = mat[1].p[1] * det3_235_235 - mat[1].p[2] * det3_235_135 + mat[1].p[3] * det3_235_125 - mat[1].p[5] * det3_235_123;
        float det4_1235_1245 = mat[1].p[1] * det3_235_245 - mat[1].p[2] * det3_235_145 + mat[1].p[4] * det3_235_125 - mat[1].p[5] * det3_235_124;
        float det4_1235_1345 = mat[1].p[1] * det3_235_345 - mat[1].p[3] * det3_235_145 + mat[1].p[4] * det3_235_135 - mat[1].p[5] * det3_235_134;
        float det4_1235_2345 = mat[1].p[2] * det3_235_345 - mat[1].p[3] * det3_235_245 + mat[1].p[4] * det3_235_235 - mat[1].p[5] * det3_235_234;
        float det4_1245_0123 = mat[1].p[0] * det3_245_123 - mat[1].p[1] * det3_245_023 + mat[1].p[2] * det3_245_013 - mat[1].p[3] * det3_245_012;
        float det4_1245_0124 = mat[1].p[0] * det3_245_124 - mat[1].p[1] * det3_245_024 + mat[1].p[2] * det3_245_014 - mat[1].p[4] * det3_245_012;
        float det4_1245_0125 = mat[1].p[0] * det3_245_125 - mat[1].p[1] * det3_245_025 + mat[1].p[2] * det3_245_015 - mat[1].p[5] * det3_245_012;
        float det4_1245_0134 = mat[1].p[0] * det3_245_134 - mat[1].p[1] * det3_245_034 + mat[1].p[3] * det3_245_014 - mat[1].p[4] * det3_245_013;
        float det4_1245_0135 = mat[1].p[0] * det3_245_135 - mat[1].p[1] * det3_245_035 + mat[1].p[3] * det3_245_015 - mat[1].p[5] * det3_245_013;
        float det4_1245_0145 = mat[1].p[0] * det3_245_145 - mat[1].p[1] * det3_245_045 + mat[1].p[4] * det3_245_015 - mat[1].p[5] * det3_245_014;
        float det4_1245_0234 = mat[1].p[0] * det3_245_234 - mat[1].p[2] * det3_245_034 + mat[1].p[3] * det3_245_024 - mat[1].p[4] * det3_245_023;
        float det4_1245_0235 = mat[1].p[0] * det3_245_235 - mat[1].p[2] * det3_245_035 + mat[1].p[3] * det3_245_025 - mat[1].p[5] * det3_245_023;
        float det4_1245_0245 = mat[1].p[0] * det3_245_245 - mat[1].p[2] * det3_245_045 + mat[1].p[4] * det3_245_025 - mat[1].p[5] * det3_245_024;
        float det4_1245_0345 = mat[1].p[0] * det3_245_345 - mat[1].p[3] * det3_245_045 + mat[1].p[4] * det3_245_035 - mat[1].p[5] * det3_245_034;
        float det4_1245_1234 = mat[1].p[1] * det3_245_234 - mat[1].p[2] * det3_245_134 + mat[1].p[3] * det3_245_124 - mat[1].p[4] * det3_245_123;
        float det4_1245_1235 = mat[1].p[1] * det3_245_235 - mat[1].p[2] * det3_245_135 + mat[1].p[3] * det3_245_125 - mat[1].p[5] * det3_245_123;
        float det4_1245_1245 = mat[1].p[1] * det3_245_245 - mat[1].p[2] * det3_245_145 + mat[1].p[4] * det3_245_125 - mat[1].p[5] * det3_245_124;
        float det4_1245_1345 = mat[1].p[1] * det3_245_345 - mat[1].p[3] * det3_245_145 + mat[1].p[4] * det3_245_135 - mat[1].p[5] * det3_245_134;
        float det4_1245_2345 = mat[1].p[2] * det3_245_345 - mat[1].p[3] * det3_245_245 + mat[1].p[4] * det3_245_235 - mat[1].p[5] * det3_245_234;
        float det4_1345_0123 = mat[1].p[0] * det3_345_123 - mat[1].p[1] * det3_345_023 + mat[1].p[2] * det3_345_013 - mat[1].p[3] * det3_345_012;
        float det4_1345_0124 = mat[1].p[0] * det3_345_124 - mat[1].p[1] * det3_345_024 + mat[1].p[2] * det3_345_014 - mat[1].p[4] * det3_345_012;
        float det4_1345_0125 = mat[1].p[0] * det3_345_125 - mat[1].p[1] * det3_345_025 + mat[1].p[2] * det3_345_015 - mat[1].p[5] * det3_345_012;
        float det4_1345_0134 = mat[1].p[0] * det3_345_134 - mat[1].p[1] * det3_345_034 + mat[1].p[3] * det3_345_014 - mat[1].p[4] * det3_345_013;
        float det4_1345_0135 = mat[1].p[0] * det3_345_135 - mat[1].p[1] * det3_345_035 + mat[1].p[3] * det3_345_015 - mat[1].p[5] * det3_345_013;
        float det4_1345_0145 = mat[1].p[0] * det3_345_145 - mat[1].p[1] * det3_345_045 + mat[1].p[4] * det3_345_015 - mat[1].p[5] * det3_345_014;
        float det4_1345_0234 = mat[1].p[0] * det3_345_234 - mat[1].p[2] * det3_345_034 + mat[1].p[3] * det3_345_024 - mat[1].p[4] * det3_345_023;
        float det4_1345_0235 = mat[1].p[0] * det3_345_235 - mat[1].p[2] * det3_345_035 + mat[1].p[3] * det3_345_025 - mat[1].p[5] * det3_345_023;
        float det4_1345_0245 = mat[1].p[0] * det3_345_245 - mat[1].p[2] * det3_345_045 + mat[1].p[4] * det3_345_025 - mat[1].p[5] * det3_345_024;
        float det4_1345_0345 = mat[1].p[0] * det3_345_345 - mat[1].p[3] * det3_345_045 + mat[1].p[4] * det3_345_035 - mat[1].p[5] * det3_345_034;
        float det4_1345_1234 = mat[1].p[1] * det3_345_234 - mat[1].p[2] * det3_345_134 + mat[1].p[3] * det3_345_124 - mat[1].p[4] * det3_345_123;
        float det4_1345_1235 = mat[1].p[1] * det3_345_235 - mat[1].p[2] * det3_345_135 + mat[1].p[3] * det3_345_125 - mat[1].p[5] * det3_345_123;
        float det4_1345_1245 = mat[1].p[1] * det3_345_245 - mat[1].p[2] * det3_345_145 + mat[1].p[4] * det3_345_125 - mat[1].p[5] * det3_345_124;
        float det4_1345_1345 = mat[1].p[1] * det3_345_345 - mat[1].p[3] * det3_345_145 + mat[1].p[4] * det3_345_135 - mat[1].p[5] * det3_345_134;
        float det4_1345_2345 = mat[1].p[2] * det3_345_345 - mat[1].p[3] * det3_345_245 + mat[1].p[4] * det3_345_235 - mat[1].p[5] * det3_345_234;

        // remaining 5x5 sub-determinants
        float det5_01234_01234 = mat[0].p[0] * det4_1234_1234 - mat[0].p[1] * det4_1234_0234 + mat[0].p[2] * det4_1234_0134 - mat[0].p[3] * det4_1234_0124 + mat[0].p[4] * det4_1234_0123;
        float det5_01234_01235 = mat[0].p[0] * det4_1234_1235 - mat[0].p[1] * det4_1234_0235 + mat[0].p[2] * det4_1234_0135 - mat[0].p[3] * det4_1234_0125 + mat[0].p[5] * det4_1234_0123;
        float det5_01234_01245 = mat[0].p[0] * det4_1234_1245 - mat[0].p[1] * det4_1234_0245 + mat[0].p[2] * det4_1234_0145 - mat[0].p[4] * det4_1234_0125 + mat[0].p[5] * det4_1234_0124;
        float det5_01234_01345 = mat[0].p[0] * det4_1234_1345 - mat[0].p[1] * det4_1234_0345 + mat[0].p[3] * det4_1234_0145 - mat[0].p[4] * det4_1234_0135 + mat[0].p[5] * det4_1234_0134;
        float det5_01234_02345 = mat[0].p[0] * det4_1234_2345 - mat[0].p[2] * det4_1234_0345 + mat[0].p[3] * det4_1234_0245 - mat[0].p[4] * det4_1234_0235 + mat[0].p[5] * det4_1234_0234;
        float det5_01234_12345 = mat[0].p[1] * det4_1234_2345 - mat[0].p[2] * det4_1234_1345 + mat[0].p[3] * det4_1234_1245 - mat[0].p[4] * det4_1234_1235 + mat[0].p[5] * det4_1234_1234;
        float det5_01235_01234 = mat[0].p[0] * det4_1235_1234 - mat[0].p[1] * det4_1235_0234 + mat[0].p[2] * det4_1235_0134 - mat[0].p[3] * det4_1235_0124 + mat[0].p[4] * det4_1235_0123;
        float det5_01235_01235 = mat[0].p[0] * det4_1235_1235 - mat[0].p[1] * det4_1235_0235 + mat[0].p[2] * det4_1235_0135 - mat[0].p[3] * det4_1235_0125 + mat[0].p[5] * det4_1235_0123;
        float det5_01235_01245 = mat[0].p[0] * det4_1235_1245 - mat[0].p[1] * det4_1235_0245 + mat[0].p[2] * det4_1235_0145 - mat[0].p[4] * det4_1235_0125 + mat[0].p[5] * det4_1235_0124;
        float det5_01235_01345 = mat[0].p[0] * det4_1235_1345 - mat[0].p[1] * det4_1235_0345 + mat[0].p[3] * det4_1235_0145 - mat[0].p[4] * det4_1235_0135 + mat[0].p[5] * det4_1235_0134;
        float det5_01235_02345 = mat[0].p[0] * det4_1235_2345 - mat[0].p[2] * det4_1235_0345 + mat[0].p[3] * det4_1235_0245 - mat[0].p[4] * det4_1235_0235 + mat[0].p[5] * det4_1235_0234;
        float det5_01235_12345 = mat[0].p[1] * det4_1235_2345 - mat[0].p[2] * det4_1235_1345 + mat[0].p[3] * det4_1235_1245 - mat[0].p[4] * det4_1235_1235 + mat[0].p[5] * det4_1235_1234;
        float det5_01245_01234 = mat[0].p[0] * det4_1245_1234 - mat[0].p[1] * det4_1245_0234 + mat[0].p[2] * det4_1245_0134 - mat[0].p[3] * det4_1245_0124 + mat[0].p[4] * det4_1245_0123;
        float det5_01245_01235 = mat[0].p[0] * det4_1245_1235 - mat[0].p[1] * det4_1245_0235 + mat[0].p[2] * det4_1245_0135 - mat[0].p[3] * det4_1245_0125 + mat[0].p[5] * det4_1245_0123;
        float det5_01245_01245 = mat[0].p[0] * det4_1245_1245 - mat[0].p[1] * det4_1245_0245 + mat[0].p[2] * det4_1245_0145 - mat[0].p[4] * det4_1245_0125 + mat[0].p[5] * det4_1245_0124;
        float det5_01245_01345 = mat[0].p[0] * det4_1245_1345 - mat[0].p[1] * det4_1245_0345 + mat[0].p[3] * det4_1245_0145 - mat[0].p[4] * det4_1245_0135 + mat[0].p[5] * det4_1245_0134;
        float det5_01245_02345 = mat[0].p[0] * det4_1245_2345 - mat[0].p[2] * det4_1245_0345 + mat[0].p[3] * det4_1245_0245 - mat[0].p[4] * det4_1245_0235 + mat[0].p[5] * det4_1245_0234;
        float det5_01245_12345 = mat[0].p[1] * det4_1245_2345 - mat[0].p[2] * det4_1245_1345 + mat[0].p[3] * det4_1245_1245 - mat[0].p[4] * det4_1245_1235 + mat[0].p[5] * det4_1245_1234;
        float det5_01345_01234 = mat[0].p[0] * det4_1345_1234 - mat[0].p[1] * det4_1345_0234 + mat[0].p[2] * det4_1345_0134 - mat[0].p[3] * det4_1345_0124 + mat[0].p[4] * det4_1345_0123;
        float det5_01345_01235 = mat[0].p[0] * det4_1345_1235 - mat[0].p[1] * det4_1345_0235 + mat[0].p[2] * det4_1345_0135 - mat[0].p[3] * det4_1345_0125 + mat[0].p[5] * det4_1345_0123;
        float det5_01345_01245 = mat[0].p[0] * det4_1345_1245 - mat[0].p[1] * det4_1345_0245 + mat[0].p[2] * det4_1345_0145 - mat[0].p[4] * det4_1345_0125 + mat[0].p[5] * det4_1345_0124;
        float det5_01345_01345 = mat[0].p[0] * det4_1345_1345 - mat[0].p[1] * det4_1345_0345 + mat[0].p[3] * det4_1345_0145 - mat[0].p[4] * det4_1345_0135 + mat[0].p[5] * det4_1345_0134;
        float det5_01345_02345 = mat[0].p[0] * det4_1345_2345 - mat[0].p[2] * det4_1345_0345 + mat[0].p[3] * det4_1345_0245 - mat[0].p[4] * det4_1345_0235 + mat[0].p[5] * det4_1345_0234;
        float det5_01345_12345 = mat[0].p[1] * det4_1345_2345 - mat[0].p[2] * det4_1345_1345 + mat[0].p[3] * det4_1345_1245 - mat[0].p[4] * det4_1345_1235 + mat[0].p[5] * det4_1345_1234;
        float det5_02345_01234 = mat[0].p[0] * det4_2345_1234 - mat[0].p[1] * det4_2345_0234 + mat[0].p[2] * det4_2345_0134 - mat[0].p[3] * det4_2345_0124 + mat[0].p[4] * det4_2345_0123;
        float det5_02345_01235 = mat[0].p[0] * det4_2345_1235 - mat[0].p[1] * det4_2345_0235 + mat[0].p[2] * det4_2345_0135 - mat[0].p[3] * det4_2345_0125 + mat[0].p[5] * det4_2345_0123;
        float det5_02345_01245 = mat[0].p[0] * det4_2345_1245 - mat[0].p[1] * det4_2345_0245 + mat[0].p[2] * det4_2345_0145 - mat[0].p[4] * det4_2345_0125 + mat[0].p[5] * det4_2345_0124;
        float det5_02345_01345 = mat[0].p[0] * det4_2345_1345 - mat[0].p[1] * det4_2345_0345 + mat[0].p[3] * det4_2345_0145 - mat[0].p[4] * det4_2345_0135 + mat[0].p[5] * det4_2345_0134;
        float det5_02345_02345 = mat[0].p[0] * det4_2345_2345 - mat[0].p[2] * det4_2345_0345 + mat[0].p[3] * det4_2345_0245 - mat[0].p[4] * det4_2345_0235 + mat[0].p[5] * det4_2345_0234;
        float det5_02345_12345 = mat[0].p[1] * det4_2345_2345 - mat[0].p[2] * det4_2345_1345 + mat[0].p[3] * det4_2345_1245 - mat[0].p[4] * det4_2345_1235 + mat[0].p[5] * det4_2345_1234;

        mat[0].p[0] = (float) (det5_12345_12345 * invDet);
        mat[0].p[1] = (float) (-det5_02345_12345 * invDet);
        mat[0].p[2] = (float) (det5_01345_12345 * invDet);
        mat[0].p[3] = (float) (-det5_01245_12345 * invDet);
        mat[0].p[4] = (float) (det5_01235_12345 * invDet);
        mat[0].p[5] = (float) (-det5_01234_12345 * invDet);

        mat[1].p[0] = (float) (-det5_12345_02345 * invDet);
        mat[1].p[1] = (float) (det5_02345_02345 * invDet);
        mat[1].p[2] = (float) (-det5_01345_02345 * invDet);
        mat[1].p[3] = (float) (det5_01245_02345 * invDet);
        mat[1].p[4] = (float) (-det5_01235_02345 * invDet);
        mat[1].p[5] = (float) (det5_01234_02345 * invDet);

        mat[2].p[0] = (float) (det5_12345_01345 * invDet);
        mat[2].p[1] = (float) (-det5_02345_01345 * invDet);
        mat[2].p[2] = (float) (det5_01345_01345 * invDet);
        mat[2].p[3] = (float) (-det5_01245_01345 * invDet);
        mat[2].p[4] = (float) (det5_01235_01345 * invDet);
        mat[2].p[5] = (float) (-det5_01234_01345 * invDet);

        mat[3].p[0] = (float) (-det5_12345_01245 * invDet);
        mat[3].p[1] = (float) (det5_02345_01245 * invDet);
        mat[3].p[2] = (float) (-det5_01345_01245 * invDet);
        mat[3].p[3] = (float) (det5_01245_01245 * invDet);
        mat[3].p[4] = (float) (-det5_01235_01245 * invDet);
        mat[3].p[5] = (float) (det5_01234_01245 * invDet);

        mat[4].p[0] = (float) (det5_12345_01235 * invDet);
        mat[4].p[1] = (float) (-det5_02345_01235 * invDet);
        mat[4].p[2] = (float) (det5_01345_01235 * invDet);
        mat[4].p[3] = (float) (-det5_01245_01235 * invDet);
        mat[4].p[4] = (float) (det5_01235_01235 * invDet);
        mat[4].p[5] = (float) (-det5_01234_01235 * invDet);

        mat[5].p[0] = (float) (-det5_12345_01234 * invDet);
        mat[5].p[1] = (float) (det5_02345_01234 * invDet);
        mat[5].p[2] = (float) (-det5_01345_01234 * invDet);
        mat[5].p[3] = (float) (det5_01245_01234 * invDet);
        mat[5].p[4] = (float) (-det5_01235_01234 * invDet);
        mat[5].p[5] = (float) (det5_01234_01234 * invDet);

        return true;
    }

    public idMat6 InverseFast() {// returns the inverse ( m * m.Inverse() = identity )
        idMat6 invMat;

        invMat = this;
        boolean r = invMat.InverseFastSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseFastSelf() {// returns false if determinant is zero
//    #else
        // 6*27+2*30 = 222 multiplications
        //		2*1  =	 2 divisions
        idVec3[] r0 = idMat0.genVec3Array(3), r1 = idMat0.genVec3Array(3), r2 = idMat0.genVec3Array(3), r3 = idMat0.genVec3Array(3);
        float c0, c1, c2, det, invDet;
        float[] mat = this.reinterpret_cast();

        // r0 = m0.Inverse();
        c0 = mat[1 * 6 + 1] * mat[2 * 6 + 2] - mat[1 * 6 + 2] * mat[2 * 6 + 1];
        c1 = mat[1 * 6 + 2] * mat[2 * 6 + 0] - mat[1 * 6 + 0] * mat[2 * 6 + 2];
        c2 = mat[1 * 6 + 0] * mat[2 * 6 + 1] - mat[1 * 6 + 1] * mat[2 * 6 + 0];

        det = mat[0 * 6 + 0] * c0 + mat[0 * 6 + 1] * c1 + mat[0 * 6 + 2] * c2;

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        r0[0].x = c0 * invDet;
        r0[0].y = (mat[0 * 6 + 2] * mat[2 * 6 + 1] - mat[0 * 6 + 1] * mat[2 * 6 + 2]) * invDet;
        r0[0].z = (mat[0 * 6 + 1] * mat[1 * 6 + 2] - mat[0 * 6 + 2] * mat[1 * 6 + 1]) * invDet;
        r0[1].x = c1 * invDet;
        r0[1].y = (mat[0 * 6 + 0] * mat[2 * 6 + 2] - mat[0 * 6 + 2] * mat[2 * 6 + 0]) * invDet;
        r0[1].z = (mat[0 * 6 + 2] * mat[1 * 6 + 0] - mat[0 * 6 + 0] * mat[1 * 6 + 2]) * invDet;
        r0[2].x = c2 * invDet;
        r0[2].y = (mat[0 * 6 + 1] * mat[2 * 6 + 0] - mat[0 * 6 + 0] * mat[2 * 6 + 1]) * invDet;
        r0[2].z = (mat[0 * 6 + 0] * mat[1 * 6 + 1] - mat[0 * 6 + 1] * mat[1 * 6 + 0]) * invDet;

        // r1 = r0 * m1;
        r1[0].x = r0[0].x * mat[0 * 6 + 3] + r0[0].y * mat[1 * 6 + 3] + r0[0].z * mat[2 * 6 + 3];
        r1[0].y = r0[0].x * mat[0 * 6 + 4] + r0[0].y * mat[1 * 6 + 4] + r0[0].z * mat[2 * 6 + 4];
        r1[0].z = r0[0].x * mat[0 * 6 + 5] + r0[0].y * mat[1 * 6 + 5] + r0[0].z * mat[2 * 6 + 5];
        r1[1].x = r0[1].x * mat[0 * 6 + 3] + r0[1].y * mat[1 * 6 + 3] + r0[1].z * mat[2 * 6 + 3];
        r1[1].y = r0[1].x * mat[0 * 6 + 4] + r0[1].y * mat[1 * 6 + 4] + r0[1].z * mat[2 * 6 + 4];
        r1[1].z = r0[1].x * mat[0 * 6 + 5] + r0[1].y * mat[1 * 6 + 5] + r0[1].z * mat[2 * 6 + 5];
        r1[2].x = r0[2].x * mat[0 * 6 + 3] + r0[2].y * mat[1 * 6 + 3] + r0[2].z * mat[2 * 6 + 3];
        r1[2].y = r0[2].x * mat[0 * 6 + 4] + r0[2].y * mat[1 * 6 + 4] + r0[2].z * mat[2 * 6 + 4];
        r1[2].z = r0[2].x * mat[0 * 6 + 5] + r0[2].y * mat[1 * 6 + 5] + r0[2].z * mat[2 * 6 + 5];

        // r2 = m2 * r1;
        r2[0].x = mat[3 * 6 + 0] * r1[0].x + mat[3 * 6 + 1] * r1[1].x + mat[3 * 6 + 2] * r1[2].x;
        r2[0].y = mat[3 * 6 + 0] * r1[0].y + mat[3 * 6 + 1] * r1[1].y + mat[3 * 6 + 2] * r1[2].y;
        r2[0].z = mat[3 * 6 + 0] * r1[0].z + mat[3 * 6 + 1] * r1[1].z + mat[3 * 6 + 2] * r1[2].z;
        r2[1].x = mat[4 * 6 + 0] * r1[0].x + mat[4 * 6 + 1] * r1[1].x + mat[4 * 6 + 2] * r1[2].x;
        r2[1].y = mat[4 * 6 + 0] * r1[0].y + mat[4 * 6 + 1] * r1[1].y + mat[4 * 6 + 2] * r1[2].y;
        r2[1].z = mat[4 * 6 + 0] * r1[0].z + mat[4 * 6 + 1] * r1[1].z + mat[4 * 6 + 2] * r1[2].z;
        r2[2].x = mat[5 * 6 + 0] * r1[0].x + mat[5 * 6 + 1] * r1[1].x + mat[5 * 6 + 2] * r1[2].x;
        r2[2].y = mat[5 * 6 + 0] * r1[0].y + mat[5 * 6 + 1] * r1[1].y + mat[5 * 6 + 2] * r1[2].y;
        r2[2].z = mat[5 * 6 + 0] * r1[0].z + mat[5 * 6 + 1] * r1[1].z + mat[5 * 6 + 2] * r1[2].z;

        // r3 = r2 - m3;
        r3[0].x = r2[0].x - mat[3 * 6 + 3];
        r3[0].y = r2[0].y - mat[3 * 6 + 4];
        r3[0].z = r2[0].z - mat[3 * 6 + 5];
        r3[1].x = r2[1].x - mat[4 * 6 + 3];
        r3[1].y = r2[1].y - mat[4 * 6 + 4];
        r3[1].z = r2[1].z - mat[4 * 6 + 5];
        r3[2].x = r2[2].x - mat[5 * 6 + 3];
        r3[2].y = r2[2].y - mat[5 * 6 + 4];
        r3[2].z = r2[2].z - mat[5 * 6 + 5];

        // r3.InverseSelf();
        r2[0].x = r3[1].y * r3[2].z - r3[1].z * r3[2].y;
        r2[1].x = r3[1].z * r3[2].x - r3[1].x * r3[2].z;
        r2[2].x = r3[1].x * r3[2].y - r3[1].y * r3[2].x;

        det = r3[0].x * r2[0].x + r3[0].y * r2[1].x + r3[0].z * r2[2].x;

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        r2[0].y = r3[0].z * r3[2].y - r3[0].y * r3[2].z;
        r2[0].z = r3[0].y * r3[1].z - r3[0].z * r3[1].y;
        r2[1].y = r3[0].x * r3[2].z - r3[0].z * r3[2].x;
        r2[1].z = r3[0].z * r3[1].x - r3[0].x * r3[1].z;
        r2[2].y = r3[0].y * r3[2].x - r3[0].x * r3[2].y;
        r2[2].z = r3[0].x * r3[1].y - r3[0].y * r3[1].x;

        r3[0].x = r2[0].x * invDet;
        r3[0].y = r2[0].y * invDet;
        r3[0].z = r2[0].z * invDet;
        r3[1].x = r2[1].x * invDet;
        r3[1].y = r2[1].y * invDet;
        r3[1].z = r2[1].z * invDet;
        r3[2].x = r2[2].x * invDet;
        r3[2].y = r2[2].y * invDet;
        r3[2].z = r2[2].z * invDet;

        // r2 = m2 * r0;
        r2[0].x = mat[3 * 6 + 0] * r0[0].x + mat[3 * 6 + 1] * r0[1].x + mat[3 * 6 + 2] * r0[2].x;
        r2[0].y = mat[3 * 6 + 0] * r0[0].y + mat[3 * 6 + 1] * r0[1].y + mat[3 * 6 + 2] * r0[2].y;
        r2[0].z = mat[3 * 6 + 0] * r0[0].z + mat[3 * 6 + 1] * r0[1].z + mat[3 * 6 + 2] * r0[2].z;
        r2[1].x = mat[4 * 6 + 0] * r0[0].x + mat[4 * 6 + 1] * r0[1].x + mat[4 * 6 + 2] * r0[2].x;
        r2[1].y = mat[4 * 6 + 0] * r0[0].y + mat[4 * 6 + 1] * r0[1].y + mat[4 * 6 + 2] * r0[2].y;
        r2[1].z = mat[4 * 6 + 0] * r0[0].z + mat[4 * 6 + 1] * r0[1].z + mat[4 * 6 + 2] * r0[2].z;
        r2[2].x = mat[5 * 6 + 0] * r0[0].x + mat[5 * 6 + 1] * r0[1].x + mat[5 * 6 + 2] * r0[2].x;
        r2[2].y = mat[5 * 6 + 0] * r0[0].y + mat[5 * 6 + 1] * r0[1].y + mat[5 * 6 + 2] * r0[2].y;
        r2[2].z = mat[5 * 6 + 0] * r0[0].z + mat[5 * 6 + 1] * r0[1].z + mat[5 * 6 + 2] * r0[2].z;

        // m2 = r3 * r2;
        this.mat[3].p[0] = r3[0].x * r2[0].x + r3[0].y * r2[1].x + r3[0].z * r2[2].x;
        this.mat[3].p[1] = r3[0].x * r2[0].y + r3[0].y * r2[1].y + r3[0].z * r2[2].y;
        this.mat[3].p[2] = r3[0].x * r2[0].z + r3[0].y * r2[1].z + r3[0].z * r2[2].z;
        this.mat[4].p[0] = r3[1].x * r2[0].x + r3[1].y * r2[1].x + r3[1].z * r2[2].x;
        this.mat[4].p[1] = r3[1].x * r2[0].y + r3[1].y * r2[1].y + r3[1].z * r2[2].y;
        this.mat[4].p[2] = r3[1].x * r2[0].z + r3[1].y * r2[1].z + r3[1].z * r2[2].z;
        this.mat[5].p[0] = r3[2].x * r2[0].x + r3[2].y * r2[1].x + r3[2].z * r2[2].x;
        this.mat[5].p[1] = r3[2].x * r2[0].y + r3[2].y * r2[1].y + r3[2].z * r2[2].y;
        this.mat[5].p[2] = r3[2].x * r2[0].z + r3[2].y * r2[1].z + r3[2].z * r2[2].z;

        // m0 = r0 - r1 * m2;
        this.mat[0].p[0] = r0[0].x - r1[0].x * this.mat[3].p[0] - r1[0].y * this.mat[4].p[0] - r1[0].z * this.mat[5].p[0];
        this.mat[0].p[1] = r0[0].y - r1[0].x * this.mat[3].p[1] - r1[0].y * this.mat[4].p[1] - r1[0].z * this.mat[5].p[1];
        this.mat[0].p[2] = r0[0].z - r1[0].x * this.mat[3].p[2] - r1[0].y * this.mat[4].p[2] - r1[0].z * this.mat[5].p[2];
        this.mat[1].p[0] = r0[1].x - r1[1].x * this.mat[3].p[0] - r1[1].y * this.mat[4].p[0] - r1[1].z * this.mat[5].p[0];
        this.mat[1].p[1] = r0[1].y - r1[1].x * this.mat[3].p[1] - r1[1].y * this.mat[4].p[1] - r1[1].z * this.mat[5].p[1];
        this.mat[1].p[2] = r0[1].z - r1[1].x * this.mat[3].p[2] - r1[1].y * this.mat[4].p[2] - r1[1].z * this.mat[5].p[2];
        this.mat[2].p[0] = r0[2].x - r1[2].x * this.mat[3].p[0] - r1[2].y * this.mat[4].p[0] - r1[2].z * this.mat[5].p[0];
        this.mat[2].p[1] = r0[2].y - r1[2].x * this.mat[3].p[1] - r1[2].y * this.mat[4].p[1] - r1[2].z * this.mat[5].p[1];
        this.mat[2].p[2] = r0[2].z - r1[2].x * this.mat[3].p[2] - r1[2].y * this.mat[4].p[2] - r1[2].z * this.mat[5].p[2];

        // m1 = r1 * r3;
        this.mat[0].p[3] = r1[0].x * r3[0].x + r1[0].y * r3[1].x + r1[0].z * r3[2].x;
        this.mat[0].p[4] = r1[0].x * r3[0].y + r1[0].y * r3[1].y + r1[0].z * r3[2].y;
        this.mat[0].p[5] = r1[0].x * r3[0].z + r1[0].y * r3[1].z + r1[0].z * r3[2].z;
        this.mat[1].p[3] = r1[1].x * r3[0].x + r1[1].y * r3[1].x + r1[1].z * r3[2].x;
        this.mat[1].p[4] = r1[1].x * r3[0].y + r1[1].y * r3[1].y + r1[1].z * r3[2].y;
        this.mat[1].p[5] = r1[1].x * r3[0].z + r1[1].y * r3[1].z + r1[1].z * r3[2].z;
        this.mat[2].p[3] = r1[2].x * r3[0].x + r1[2].y * r3[1].x + r1[2].z * r3[2].x;
        this.mat[2].p[4] = r1[2].x * r3[0].y + r1[2].y * r3[1].y + r1[2].z * r3[2].y;
        this.mat[2].p[5] = r1[2].x * r3[0].z + r1[2].y * r3[1].z + r1[2].z * r3[2].z;

        // m3 = -r3;
        this.mat[3].p[3] = -r3[0].x;
        this.mat[3].p[4] = -r3[0].y;
        this.mat[3].p[5] = -r3[0].z;
        this.mat[4].p[3] = -r3[1].x;
        this.mat[4].p[4] = -r3[1].y;
        this.mat[4].p[5] = -r3[1].z;
        this.mat[5].p[3] = -r3[2].x;
        this.mat[5].p[4] = -r3[2].y;
        this.mat[5].p[5] = -r3[2].z;

        return true;
//#endif
    }

    public final int GetDimension() {
        return 36;
    }
//public	const float *	ToFloatPtr( void ) const;
//public	float *			ToFloatPtr( void );
//public	const char *	ToString( int precision = 2 ) const;

    private void oSet(final idMat6 mat6) {
        this.mat[0].oSet(mat6.mat[0]);
        this.mat[1].oSet(mat6.mat[1]);
        this.mat[2].oSet(mat6.mat[2]);
        this.mat[3].oSet(mat6.mat[3]);
        this.mat[4].oSet(mat6.mat[4]);
        this.mat[5].oSet(mat6.mat[5]);
    }

    float[] reinterpret_cast() {
        final int size = 6;
        float[] temp = new float[size * size];

        for (int x = 0; x < size; x++) {
            System.arraycopy(this.mat[x].p, 0, temp, x * 6 + 0, size);
        }
        return temp;
    }

};
