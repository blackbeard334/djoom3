package neo.idlib.math.Matrix;

import static neo.idlib.math.Matrix.idMat0.MATRIX_EPSILON;
import static neo.idlib.math.Matrix.idMat0.MATRIX_INVERSE_EPSILON;

import java.util.Arrays;

import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec6;
import neo.open.Nio;

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
        this.mat[0].oSet(v0);
        this.mat[1].oSet(v1);
        this.mat[2].oSet(v2);
        this.mat[3].oSet(v3);
        this.mat[4].oSet(v4);
        this.mat[5].oSet(v5);
    }

    public idMat6(final idMat3 m0, final idMat3 m1, final idMat3 m2, final idMat3 m3) {
        this.mat[0].oSet(new idVec6(m0.mat[0].x, m0.mat[0].y, m0.mat[0].z, m1.mat[0].x, m1.mat[0].y, m1.mat[0].z));
        this.mat[1].oSet(new idVec6(m0.mat[1].x, m0.mat[1].y, m0.mat[1].z, m1.mat[1].x, m1.mat[1].y, m1.mat[1].z));
        this.mat[2].oSet(new idVec6(m0.mat[2].x, m0.mat[2].y, m0.mat[2].z, m1.mat[2].x, m1.mat[2].y, m1.mat[2].z));
        this.mat[3].oSet(new idVec6(m2.mat[0].x, m2.mat[0].y, m2.mat[0].z, m3.mat[0].x, m3.mat[0].y, m3.mat[0].z));
        this.mat[4].oSet(new idVec6(m2.mat[1].x, m2.mat[1].y, m2.mat[1].z, m3.mat[1].x, m3.mat[1].y, m3.mat[1].z));
        this.mat[5].oSet(new idVec6(m2.mat[2].x, m2.mat[2].y, m2.mat[2].z, m3.mat[2].x, m3.mat[2].y, m3.mat[2].z));
    }

    public idMat6(final float src[][]) {
//	memcpy( mat, src, 6 * 6 * sizeof( float ) );
        this.mat[0].oSet(new idVec6(src[0][0], src[0][1], src[0][2], src[0][3], src[0][4], src[0][5]));
        this.mat[1].oSet(new idVec6(src[1][0], src[1][1], src[1][2], src[1][3], src[1][4], src[1][5]));
        this.mat[2].oSet(new idVec6(src[2][0], src[2][1], src[2][2], src[2][3], src[2][4], src[2][5]));
        this.mat[3].oSet(new idVec6(src[3][0], src[3][1], src[3][2], src[3][3], src[3][4], src[3][5]));
        this.mat[4].oSet(new idVec6(src[4][0], src[4][1], src[4][2], src[4][3], src[4][4], src[4][5]));
        this.mat[5].oSet(new idVec6(src[5][0], src[5][1], src[5][2], src[5][3], src[5][4], src[5][5]));
    }

    public idMat6(final idMat6 m) {
        this.oSet(m);
    }

//public	const idVec6 &	operator[]( int index ) const;
//public	idVec6 &		operator[]( int index );
//public	idMat6			operator*( const float a ) const;
    public idMat6 oMultiply(final float a) {
        return new idMat6(
                new idVec6(this.mat[0].p[0] * a, this.mat[0].p[1] * a, this.mat[0].p[2] * a, this.mat[0].p[3] * a, this.mat[0].p[4] * a, this.mat[0].p[5] * a),
                new idVec6(this.mat[1].p[0] * a, this.mat[1].p[1] * a, this.mat[1].p[2] * a, this.mat[1].p[3] * a, this.mat[1].p[4] * a, this.mat[1].p[5] * a),
                new idVec6(this.mat[2].p[0] * a, this.mat[2].p[1] * a, this.mat[2].p[2] * a, this.mat[2].p[3] * a, this.mat[2].p[4] * a, this.mat[2].p[5] * a),
                new idVec6(this.mat[3].p[0] * a, this.mat[3].p[1] * a, this.mat[3].p[2] * a, this.mat[3].p[3] * a, this.mat[3].p[4] * a, this.mat[3].p[5] * a),
                new idVec6(this.mat[4].p[0] * a, this.mat[4].p[1] * a, this.mat[4].p[2] * a, this.mat[4].p[3] * a, this.mat[4].p[4] * a, this.mat[4].p[5] * a),
                new idVec6(this.mat[5].p[0] * a, this.mat[5].p[1] * a, this.mat[5].p[2] * a, this.mat[5].p[3] * a, this.mat[5].p[4] * a, this.mat[5].p[5] * a));
    }
//public	idVec6			operator*( const idVec6 &vec ) const;

    public idVec6 oMultiply(final idVec6 vec) {
        return new idVec6(
                (this.mat[0].p[0] * vec.p[0]) + (this.mat[0].p[1] * vec.p[1]) + (this.mat[0].p[2] * vec.p[2]) + (this.mat[0].p[3] * vec.p[3]) + (this.mat[0].p[4] * vec.p[4]) + (this.mat[0].p[5] * vec.p[5]),
                (this.mat[1].p[0] * vec.p[0]) + (this.mat[1].p[1] * vec.p[1]) + (this.mat[1].p[2] * vec.p[2]) + (this.mat[1].p[3] * vec.p[3]) + (this.mat[1].p[4] * vec.p[4]) + (this.mat[1].p[5] * vec.p[5]),
                (this.mat[2].p[0] * vec.p[0]) + (this.mat[2].p[1] * vec.p[1]) + (this.mat[2].p[2] * vec.p[2]) + (this.mat[2].p[3] * vec.p[3]) + (this.mat[2].p[4] * vec.p[4]) + (this.mat[2].p[5] * vec.p[5]),
                (this.mat[3].p[0] * vec.p[0]) + (this.mat[3].p[1] * vec.p[1]) + (this.mat[3].p[2] * vec.p[2]) + (this.mat[3].p[3] * vec.p[3]) + (this.mat[3].p[4] * vec.p[4]) + (this.mat[3].p[5] * vec.p[5]),
                (this.mat[4].p[0] * vec.p[0]) + (this.mat[4].p[1] * vec.p[1]) + (this.mat[4].p[2] * vec.p[2]) + (this.mat[4].p[3] * vec.p[3]) + (this.mat[4].p[4] * vec.p[4]) + (this.mat[4].p[5] * vec.p[5]),
                (this.mat[5].p[0] * vec.p[0]) + (this.mat[5].p[1] * vec.p[1]) + (this.mat[5].p[2] * vec.p[2]) + (this.mat[5].p[3] * vec.p[3]) + (this.mat[5].p[4] * vec.p[4]) + (this.mat[5].p[5] * vec.p[5]));
    }
//public	idMat6			operator*( const idMat6 &a ) const;

    public idMat6 oMultiply(final idMat6 a) {
        int i, j;
        final float[] m1Ptr, m2Ptr;
//	float *dstPtr;
        final idMat6 dst = new idMat6();

        m1Ptr = this.reinterpret_cast();
        m2Ptr = a.reinterpret_cast();
//	dstPtr = reinterpret_cast<float *>(&dst);

        for (i = 0; i < 6; i++) {
            for (j = 0; j < 6; j++) {
                dst.mat[i].p[i] = (m1Ptr[0] * m2Ptr[ (0 * 6) + j])
                        + (m1Ptr[1] * m2Ptr[ (1 * 6) + j])
                        + (m1Ptr[2] * m2Ptr[ (2 * 6) + j])
                        + (m1Ptr[3] * m2Ptr[ (3 * 6) + j])
                        + (m1Ptr[4] * m2Ptr[ (4 * 6) + j])
                        + (m1Ptr[5] * m2Ptr[ (5 * 6) + j]);
//			dstPtr++;
            }
//		m1Ptr += 6;
        }
        return dst;
    }
//public	idMat6			operator+( const idMat6 &a ) const;

    public idMat6 oPlus(final idMat6 a) {
        return new idMat6(
                new idVec6(this.mat[0].p[0] + a.mat[0].p[0], this.mat[0].p[1] + a.mat[0].p[1], this.mat[0].p[2] + a.mat[0].p[2], this.mat[0].p[3] + a.mat[0].p[3], this.mat[0].p[4] + a.mat[0].p[4], this.mat[0].p[5] + a.mat[0].p[5]),
                new idVec6(this.mat[1].p[0] + a.mat[1].p[0], this.mat[1].p[1] + a.mat[1].p[1], this.mat[1].p[2] + a.mat[1].p[2], this.mat[1].p[3] + a.mat[1].p[3], this.mat[1].p[4] + a.mat[1].p[4], this.mat[1].p[5] + a.mat[1].p[5]),
                new idVec6(this.mat[2].p[0] + a.mat[2].p[0], this.mat[2].p[1] + a.mat[2].p[1], this.mat[2].p[2] + a.mat[2].p[2], this.mat[2].p[3] + a.mat[2].p[3], this.mat[2].p[4] + a.mat[2].p[4], this.mat[2].p[5] + a.mat[2].p[5]),
                new idVec6(this.mat[3].p[0] + a.mat[3].p[0], this.mat[3].p[1] + a.mat[3].p[1], this.mat[3].p[2] + a.mat[3].p[2], this.mat[3].p[3] + a.mat[3].p[3], this.mat[3].p[4] + a.mat[3].p[4], this.mat[3].p[5] + a.mat[3].p[5]),
                new idVec6(this.mat[4].p[0] + a.mat[4].p[0], this.mat[4].p[1] + a.mat[4].p[1], this.mat[4].p[2] + a.mat[4].p[2], this.mat[4].p[3] + a.mat[4].p[3], this.mat[4].p[4] + a.mat[4].p[4], this.mat[4].p[5] + a.mat[4].p[5]),
                new idVec6(this.mat[5].p[0] + a.mat[5].p[0], this.mat[5].p[1] + a.mat[5].p[1], this.mat[5].p[2] + a.mat[5].p[2], this.mat[5].p[3] + a.mat[5].p[3], this.mat[5].p[4] + a.mat[5].p[4], this.mat[5].p[5] + a.mat[5].p[5]));
    }

//public	idMat6			operator-( const idMat6 &a ) const;
    public idMat6 oMinus(final idMat6 a) {
        return new idMat6(
                new idVec6(this.mat[0].p[0] - a.mat[0].p[0], this.mat[0].p[1] - a.mat[0].p[1], this.mat[0].p[2] - a.mat[0].p[2], this.mat[0].p[3] - a.mat[0].p[3], this.mat[0].p[4] - a.mat[0].p[4], this.mat[0].p[5] - a.mat[0].p[5]),
                new idVec6(this.mat[1].p[0] - a.mat[1].p[0], this.mat[1].p[1] - a.mat[1].p[1], this.mat[1].p[2] - a.mat[1].p[2], this.mat[1].p[3] - a.mat[1].p[3], this.mat[1].p[4] - a.mat[1].p[4], this.mat[1].p[5] - a.mat[1].p[5]),
                new idVec6(this.mat[2].p[0] - a.mat[2].p[0], this.mat[2].p[1] - a.mat[2].p[1], this.mat[2].p[2] - a.mat[2].p[2], this.mat[2].p[3] - a.mat[2].p[3], this.mat[2].p[4] - a.mat[2].p[4], this.mat[2].p[5] - a.mat[2].p[5]),
                new idVec6(this.mat[3].p[0] - a.mat[3].p[0], this.mat[3].p[1] - a.mat[3].p[1], this.mat[3].p[2] - a.mat[3].p[2], this.mat[3].p[3] - a.mat[3].p[3], this.mat[3].p[4] - a.mat[3].p[4], this.mat[3].p[5] - a.mat[3].p[5]),
                new idVec6(this.mat[4].p[0] - a.mat[4].p[0], this.mat[4].p[1] - a.mat[4].p[1], this.mat[4].p[2] - a.mat[4].p[2], this.mat[4].p[3] - a.mat[4].p[3], this.mat[4].p[4] - a.mat[4].p[4], this.mat[4].p[5] - a.mat[4].p[5]),
                new idVec6(this.mat[5].p[0] - a.mat[5].p[0], this.mat[5].p[1] - a.mat[5].p[1], this.mat[5].p[2] - a.mat[5].p[2], this.mat[5].p[3] - a.mat[5].p[3], this.mat[5].p[4] - a.mat[5].p[4], this.mat[5].p[5] - a.mat[5].p[5]));
    }
//public	idMat6 &		operator*=( const float a );

    public idMat6 oMulSet(final float a) {
        this.mat[0].p[0] *= a;
        this.mat[0].p[1] *= a;
        this.mat[0].p[2] *= a;
        this.mat[0].p[3] *= a;
        this.mat[0].p[4] *= a;
        this.mat[0].p[5] *= a;
        this.mat[1].p[0] *= a;
        this.mat[1].p[1] *= a;
        this.mat[1].p[2] *= a;
        this.mat[1].p[3] *= a;
        this.mat[1].p[4] *= a;
        this.mat[1].p[5] *= a;
        this.mat[2].p[0] *= a;
        this.mat[2].p[1] *= a;
        this.mat[2].p[2] *= a;
        this.mat[2].p[3] *= a;
        this.mat[2].p[4] *= a;
        this.mat[2].p[5] *= a;
        this.mat[3].p[0] *= a;
        this.mat[3].p[1] *= a;
        this.mat[3].p[2] *= a;
        this.mat[3].p[3] *= a;
        this.mat[3].p[4] *= a;
        this.mat[3].p[5] *= a;
        this.mat[4].p[0] *= a;
        this.mat[4].p[1] *= a;
        this.mat[4].p[2] *= a;
        this.mat[4].p[3] *= a;
        this.mat[4].p[4] *= a;
        this.mat[4].p[5] *= a;
        this.mat[5].p[0] *= a;
        this.mat[5].p[1] *= a;
        this.mat[5].p[2] *= a;
        this.mat[5].p[3] *= a;
        this.mat[5].p[4] *= a;
        this.mat[5].p[5] *= a;
        return this;
    }
//public	idMat6 &		operator*=( const idMat6 &a );

    public idMat6 oMulSet(final idMat6 a) {
        this.oSet(this.oMultiply(a));
        return this;
    }
//public	idMat6 &		operator+=( const idMat6 &a );

    public idMat6 oPluSet(final idMat6 a) {
        this.mat[0].p[0] += a.mat[0].p[0];
        this.mat[0].p[1] += a.mat[0].p[1];
        this.mat[0].p[2] += a.mat[0].p[2];
        this.mat[0].p[3] += a.mat[0].p[3];
        this.mat[0].p[4] += a.mat[0].p[4];
        this.mat[0].p[5] += a.mat[0].p[5];
        this.mat[1].p[0] += a.mat[1].p[0];
        this.mat[1].p[1] += a.mat[1].p[1];
        this.mat[1].p[2] += a.mat[1].p[2];
        this.mat[1].p[3] += a.mat[1].p[3];
        this.mat[1].p[4] += a.mat[1].p[4];
        this.mat[1].p[5] += a.mat[1].p[5];
        this.mat[2].p[0] += a.mat[2].p[0];
        this.mat[2].p[1] += a.mat[2].p[1];
        this.mat[2].p[2] += a.mat[2].p[2];
        this.mat[2].p[3] += a.mat[2].p[3];
        this.mat[2].p[4] += a.mat[2].p[4];
        this.mat[2].p[5] += a.mat[2].p[5];
        this.mat[3].p[0] += a.mat[3].p[0];
        this.mat[3].p[1] += a.mat[3].p[1];
        this.mat[3].p[2] += a.mat[3].p[2];
        this.mat[3].p[3] += a.mat[3].p[3];
        this.mat[3].p[4] += a.mat[3].p[4];
        this.mat[3].p[5] += a.mat[3].p[5];
        this.mat[4].p[0] += a.mat[4].p[0];
        this.mat[4].p[1] += a.mat[4].p[1];
        this.mat[4].p[2] += a.mat[4].p[2];
        this.mat[4].p[3] += a.mat[4].p[3];
        this.mat[4].p[4] += a.mat[4].p[4];
        this.mat[4].p[5] += a.mat[4].p[5];
        this.mat[5].p[0] += a.mat[5].p[0];
        this.mat[5].p[1] += a.mat[5].p[1];
        this.mat[5].p[2] += a.mat[5].p[2];
        this.mat[5].p[3] += a.mat[5].p[3];
        this.mat[5].p[4] += a.mat[5].p[4];
        this.mat[5].p[5] += a.mat[5].p[5];
        return this;
    }
//public	idMat6 &		operator-=( const idMat6 &a );

    public idMat6 oMinSet(final idMat6 a) {
        this.mat[0].p[0] -= a.mat[0].p[0];
        this.mat[0].p[1] -= a.mat[0].p[1];
        this.mat[0].p[2] -= a.mat[0].p[2];
        this.mat[0].p[3] -= a.mat[0].p[3];
        this.mat[0].p[4] -= a.mat[0].p[4];
        this.mat[0].p[5] -= a.mat[0].p[5];
        this.mat[1].p[0] -= a.mat[1].p[0];
        this.mat[1].p[1] -= a.mat[1].p[1];
        this.mat[1].p[2] -= a.mat[1].p[2];
        this.mat[1].p[3] -= a.mat[1].p[3];
        this.mat[1].p[4] -= a.mat[1].p[4];
        this.mat[1].p[5] -= a.mat[1].p[5];
        this.mat[2].p[0] -= a.mat[2].p[0];
        this.mat[2].p[1] -= a.mat[2].p[1];
        this.mat[2].p[2] -= a.mat[2].p[2];
        this.mat[2].p[3] -= a.mat[2].p[3];
        this.mat[2].p[4] -= a.mat[2].p[4];
        this.mat[2].p[5] -= a.mat[2].p[5];
        this.mat[3].p[0] -= a.mat[3].p[0];
        this.mat[3].p[1] -= a.mat[3].p[1];
        this.mat[3].p[2] -= a.mat[3].p[2];
        this.mat[3].p[3] -= a.mat[3].p[3];
        this.mat[3].p[4] -= a.mat[3].p[4];
        this.mat[3].p[5] -= a.mat[3].p[5];
        this.mat[4].p[0] -= a.mat[4].p[0];
        this.mat[4].p[1] -= a.mat[4].p[1];
        this.mat[4].p[2] -= a.mat[4].p[2];
        this.mat[4].p[3] -= a.mat[4].p[3];
        this.mat[4].p[4] -= a.mat[4].p[4];
        this.mat[4].p[5] -= a.mat[4].p[5];
        this.mat[5].p[0] -= a.mat[5].p[0];
        this.mat[5].p[1] -= a.mat[5].p[1];
        this.mat[5].p[2] -= a.mat[5].p[2];
        this.mat[5].p[3] -= a.mat[5].p[3];
        this.mat[5].p[4] -= a.mat[5].p[4];
        this.mat[5].p[5] -= a.mat[5].p[5];
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
        for (i = 0; i < (6 * 6); i++) {
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
        for (i = 0; i < (6 * 6); i++) {
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
        hash = (13 * hash) + Arrays.deepHashCode(this.mat);
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
                if (idMath.Fabs(this.mat[i].p[j] - this.mat[j].p[i]) > epsilon) {
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
                if ((i != j) && (idMath.Fabs(this.mat[i].p[j]) > epsilon)) {
                    return false;
                }
            }
        }
        return true;
    }

    public idMat3 SubMat3(int n) {
        assert ((n >= 0) && (n < 4));
        final int b0 = ((n & 2) >> 1) * 3;
        final int b1 = (n & 1) * 3;
        return new idMat3(
                this.mat[b0 + 0].p[b1 + 0], this.mat[b0 + 0].p[b1 + 1], this.mat[b0 + 0].p[b1 + 2],
                this.mat[b0 + 1].p[b1 + 0], this.mat[b0 + 1].p[b1 + 1], this.mat[b0 + 1].p[b1 + 2],
                this.mat[b0 + 2].p[b1 + 0], this.mat[b0 + 2].p[b1 + 1], this.mat[b0 + 2].p[b1 + 2]);
    }

    public float Trace() {
        return (this.mat[0].p[0] + this.mat[1].p[1] + this.mat[2].p[2] + this.mat[3].p[3] + this.mat[4].p[4] + this.mat[5].p[5]);
    }

    public float Determinant() {
        // 2x2 sub-determinants required to calculate 6x6 determinant
        final float det2_45_01 = (this.mat[4].p[0] * this.mat[5].p[1]) - (this.mat[4].p[1] * this.mat[5].p[0]);
        final float det2_45_02 = (this.mat[4].p[0] * this.mat[5].p[2]) - (this.mat[4].p[2] * this.mat[5].p[0]);
        final float det2_45_03 = (this.mat[4].p[0] * this.mat[5].p[3]) - (this.mat[4].p[3] * this.mat[5].p[0]);
        final float det2_45_04 = (this.mat[4].p[0] * this.mat[5].p[4]) - (this.mat[4].p[4] * this.mat[5].p[0]);
        final float det2_45_05 = (this.mat[4].p[0] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[0]);
        final float det2_45_12 = (this.mat[4].p[1] * this.mat[5].p[2]) - (this.mat[4].p[2] * this.mat[5].p[1]);
        final float det2_45_13 = (this.mat[4].p[1] * this.mat[5].p[3]) - (this.mat[4].p[3] * this.mat[5].p[1]);
        final float det2_45_14 = (this.mat[4].p[1] * this.mat[5].p[4]) - (this.mat[4].p[4] * this.mat[5].p[1]);
        final float det2_45_15 = (this.mat[4].p[1] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[1]);
        final float det2_45_23 = (this.mat[4].p[2] * this.mat[5].p[3]) - (this.mat[4].p[3] * this.mat[5].p[2]);
        final float det2_45_24 = (this.mat[4].p[2] * this.mat[5].p[4]) - (this.mat[4].p[4] * this.mat[5].p[2]);
        final float det2_45_25 = (this.mat[4].p[2] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[2]);
        final float det2_45_34 = (this.mat[4].p[3] * this.mat[5].p[4]) - (this.mat[4].p[4] * this.mat[5].p[3]);
        final float det2_45_35 = (this.mat[4].p[3] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[3]);
        final float det2_45_45 = (this.mat[4].p[4] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[4]);

        // 3x3 sub-determinants required to calculate 6x6 determinant
        final float det3_345_012 = ((this.mat[3].p[0] * det2_45_12) - (this.mat[3].p[1] * det2_45_02)) + (this.mat[3].p[2] * det2_45_01);
        final float det3_345_013 = ((this.mat[3].p[0] * det2_45_13) - (this.mat[3].p[1] * det2_45_03)) + (this.mat[3].p[3] * det2_45_01);
        final float det3_345_014 = ((this.mat[3].p[0] * det2_45_14) - (this.mat[3].p[1] * det2_45_04)) + (this.mat[3].p[4] * det2_45_01);
        final float det3_345_015 = ((this.mat[3].p[0] * det2_45_15) - (this.mat[3].p[1] * det2_45_05)) + (this.mat[3].p[5] * det2_45_01);
        final float det3_345_023 = ((this.mat[3].p[0] * det2_45_23) - (this.mat[3].p[2] * det2_45_03)) + (this.mat[3].p[3] * det2_45_02);
        final float det3_345_024 = ((this.mat[3].p[0] * det2_45_24) - (this.mat[3].p[2] * det2_45_04)) + (this.mat[3].p[4] * det2_45_02);
        final float det3_345_025 = ((this.mat[3].p[0] * det2_45_25) - (this.mat[3].p[2] * det2_45_05)) + (this.mat[3].p[5] * det2_45_02);
        final float det3_345_034 = ((this.mat[3].p[0] * det2_45_34) - (this.mat[3].p[3] * det2_45_04)) + (this.mat[3].p[4] * det2_45_03);
        final float det3_345_035 = ((this.mat[3].p[0] * det2_45_35) - (this.mat[3].p[3] * det2_45_05)) + (this.mat[3].p[5] * det2_45_03);
        final float det3_345_045 = ((this.mat[3].p[0] * det2_45_45) - (this.mat[3].p[4] * det2_45_05)) + (this.mat[3].p[5] * det2_45_04);
        final float det3_345_123 = ((this.mat[3].p[1] * det2_45_23) - (this.mat[3].p[2] * det2_45_13)) + (this.mat[3].p[3] * det2_45_12);
        final float det3_345_124 = ((this.mat[3].p[1] * det2_45_24) - (this.mat[3].p[2] * det2_45_14)) + (this.mat[3].p[4] * det2_45_12);
        final float det3_345_125 = ((this.mat[3].p[1] * det2_45_25) - (this.mat[3].p[2] * det2_45_15)) + (this.mat[3].p[5] * det2_45_12);
        final float det3_345_134 = ((this.mat[3].p[1] * det2_45_34) - (this.mat[3].p[3] * det2_45_14)) + (this.mat[3].p[4] * det2_45_13);
        final float det3_345_135 = ((this.mat[3].p[1] * det2_45_35) - (this.mat[3].p[3] * det2_45_15)) + (this.mat[3].p[5] * det2_45_13);
        final float det3_345_145 = ((this.mat[3].p[1] * det2_45_45) - (this.mat[3].p[4] * det2_45_15)) + (this.mat[3].p[5] * det2_45_14);
        final float det3_345_234 = ((this.mat[3].p[2] * det2_45_34) - (this.mat[3].p[3] * det2_45_24)) + (this.mat[3].p[4] * det2_45_23);
        final float det3_345_235 = ((this.mat[3].p[2] * det2_45_35) - (this.mat[3].p[3] * det2_45_25)) + (this.mat[3].p[5] * det2_45_23);
        final float det3_345_245 = ((this.mat[3].p[2] * det2_45_45) - (this.mat[3].p[4] * det2_45_25)) + (this.mat[3].p[5] * det2_45_24);
        final float det3_345_345 = ((this.mat[3].p[3] * det2_45_45) - (this.mat[3].p[4] * det2_45_35)) + (this.mat[3].p[5] * det2_45_34);

        // 4x4 sub-determinants required to calculate 6x6 determinant
        final float det4_2345_0123 = (((this.mat[2].p[0] * det3_345_123) - (this.mat[2].p[1] * det3_345_023)) + (this.mat[2].p[2] * det3_345_013)) - (this.mat[2].p[3] * det3_345_012);
        final float det4_2345_0124 = (((this.mat[2].p[0] * det3_345_124) - (this.mat[2].p[1] * det3_345_024)) + (this.mat[2].p[2] * det3_345_014)) - (this.mat[2].p[4] * det3_345_012);
        final float det4_2345_0125 = (((this.mat[2].p[0] * det3_345_125) - (this.mat[2].p[1] * det3_345_025)) + (this.mat[2].p[2] * det3_345_015)) - (this.mat[2].p[5] * det3_345_012);
        final float det4_2345_0134 = (((this.mat[2].p[0] * det3_345_134) - (this.mat[2].p[1] * det3_345_034)) + (this.mat[2].p[3] * det3_345_014)) - (this.mat[2].p[4] * det3_345_013);
        final float det4_2345_0135 = (((this.mat[2].p[0] * det3_345_135) - (this.mat[2].p[1] * det3_345_035)) + (this.mat[2].p[3] * det3_345_015)) - (this.mat[2].p[5] * det3_345_013);
        final float det4_2345_0145 = (((this.mat[2].p[0] * det3_345_145) - (this.mat[2].p[1] * det3_345_045)) + (this.mat[2].p[4] * det3_345_015)) - (this.mat[2].p[5] * det3_345_014);
        final float det4_2345_0234 = (((this.mat[2].p[0] * det3_345_234) - (this.mat[2].p[2] * det3_345_034)) + (this.mat[2].p[3] * det3_345_024)) - (this.mat[2].p[4] * det3_345_023);
        final float det4_2345_0235 = (((this.mat[2].p[0] * det3_345_235) - (this.mat[2].p[2] * det3_345_035)) + (this.mat[2].p[3] * det3_345_025)) - (this.mat[2].p[5] * det3_345_023);
        final float det4_2345_0245 = (((this.mat[2].p[0] * det3_345_245) - (this.mat[2].p[2] * det3_345_045)) + (this.mat[2].p[4] * det3_345_025)) - (this.mat[2].p[5] * det3_345_024);
        final float det4_2345_0345 = (((this.mat[2].p[0] * det3_345_345) - (this.mat[2].p[3] * det3_345_045)) + (this.mat[2].p[4] * det3_345_035)) - (this.mat[2].p[5] * det3_345_034);
        final float det4_2345_1234 = (((this.mat[2].p[1] * det3_345_234) - (this.mat[2].p[2] * det3_345_134)) + (this.mat[2].p[3] * det3_345_124)) - (this.mat[2].p[4] * det3_345_123);
        final float det4_2345_1235 = (((this.mat[2].p[1] * det3_345_235) - (this.mat[2].p[2] * det3_345_135)) + (this.mat[2].p[3] * det3_345_125)) - (this.mat[2].p[5] * det3_345_123);
        final float det4_2345_1245 = (((this.mat[2].p[1] * det3_345_245) - (this.mat[2].p[2] * det3_345_145)) + (this.mat[2].p[4] * det3_345_125)) - (this.mat[2].p[5] * det3_345_124);
        final float det4_2345_1345 = (((this.mat[2].p[1] * det3_345_345) - (this.mat[2].p[3] * det3_345_145)) + (this.mat[2].p[4] * det3_345_135)) - (this.mat[2].p[5] * det3_345_134);
        final float det4_2345_2345 = (((this.mat[2].p[2] * det3_345_345) - (this.mat[2].p[3] * det3_345_245)) + (this.mat[2].p[4] * det3_345_235)) - (this.mat[2].p[5] * det3_345_234);

        // 5x5 sub-determinants required to calculate 6x6 determinant
        final float det5_12345_01234 = ((((this.mat[1].p[0] * det4_2345_1234) - (this.mat[1].p[1] * det4_2345_0234)) + (this.mat[1].p[2] * det4_2345_0134)) - (this.mat[1].p[3] * det4_2345_0124)) + (this.mat[1].p[4] * det4_2345_0123);
        final float det5_12345_01235 = ((((this.mat[1].p[0] * det4_2345_1235) - (this.mat[1].p[1] * det4_2345_0235)) + (this.mat[1].p[2] * det4_2345_0135)) - (this.mat[1].p[3] * det4_2345_0125)) + (this.mat[1].p[5] * det4_2345_0123);
        final float det5_12345_01245 = ((((this.mat[1].p[0] * det4_2345_1245) - (this.mat[1].p[1] * det4_2345_0245)) + (this.mat[1].p[2] * det4_2345_0145)) - (this.mat[1].p[4] * det4_2345_0125)) + (this.mat[1].p[5] * det4_2345_0124);
        final float det5_12345_01345 = ((((this.mat[1].p[0] * det4_2345_1345) - (this.mat[1].p[1] * det4_2345_0345)) + (this.mat[1].p[3] * det4_2345_0145)) - (this.mat[1].p[4] * det4_2345_0135)) + (this.mat[1].p[5] * det4_2345_0134);
        final float det5_12345_02345 = ((((this.mat[1].p[0] * det4_2345_2345) - (this.mat[1].p[2] * det4_2345_0345)) + (this.mat[1].p[3] * det4_2345_0245)) - (this.mat[1].p[4] * det4_2345_0235)) + (this.mat[1].p[5] * det4_2345_0234);
        final float det5_12345_12345 = ((((this.mat[1].p[1] * det4_2345_2345) - (this.mat[1].p[2] * det4_2345_1345)) + (this.mat[1].p[3] * det4_2345_1245)) - (this.mat[1].p[4] * det4_2345_1235)) + (this.mat[1].p[5] * det4_2345_1234);

        // determinant of 6x6 matrix
        return (((((this.mat[0].p[0] * det5_12345_12345) - (this.mat[0].p[1] * det5_12345_02345)) + (this.mat[0].p[2] * det5_12345_01345))
                - (this.mat[0].p[3] * det5_12345_01245)) + (this.mat[0].p[4] * det5_12345_01235)) - (this.mat[0].p[5] * det5_12345_01234);
    }

    public idMat6 Transpose() {// returns transpose
        final idMat6 transpose = new idMat6();
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
                temp = this.mat[ i].p[ j];
                this.mat[ i].p[ j] = this.mat[ j].p[ i];
                this.mat[ j].p[ i] = temp;
            }
        }
        return this;
    }

    public idMat6 Inverse() {// returns the inverse ( m * m.Inverse() = identity )
        idMat6 invMat;

        invMat = this;
        final boolean r = invMat.InverseSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseSelf() {// returns false if determinant is zero
        // 810+6+36 = 852 multiplications
        //				1 division
        double det, invDet;

        // 2x2 sub-determinants required to calculate 6x6 determinant
        final float det2_45_01 = (this.mat[4].p[0] * this.mat[5].p[1]) - (this.mat[4].p[1] * this.mat[5].p[0]);
        final float det2_45_02 = (this.mat[4].p[0] * this.mat[5].p[2]) - (this.mat[4].p[2] * this.mat[5].p[0]);
        final float det2_45_03 = (this.mat[4].p[0] * this.mat[5].p[3]) - (this.mat[4].p[3] * this.mat[5].p[0]);
        final float det2_45_04 = (this.mat[4].p[0] * this.mat[5].p[4]) - (this.mat[4].p[4] * this.mat[5].p[0]);
        final float det2_45_05 = (this.mat[4].p[0] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[0]);
        final float det2_45_12 = (this.mat[4].p[1] * this.mat[5].p[2]) - (this.mat[4].p[2] * this.mat[5].p[1]);
        final float det2_45_13 = (this.mat[4].p[1] * this.mat[5].p[3]) - (this.mat[4].p[3] * this.mat[5].p[1]);
        final float det2_45_14 = (this.mat[4].p[1] * this.mat[5].p[4]) - (this.mat[4].p[4] * this.mat[5].p[1]);
        final float det2_45_15 = (this.mat[4].p[1] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[1]);
        final float det2_45_23 = (this.mat[4].p[2] * this.mat[5].p[3]) - (this.mat[4].p[3] * this.mat[5].p[2]);
        final float det2_45_24 = (this.mat[4].p[2] * this.mat[5].p[4]) - (this.mat[4].p[4] * this.mat[5].p[2]);
        final float det2_45_25 = (this.mat[4].p[2] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[2]);
        final float det2_45_34 = (this.mat[4].p[3] * this.mat[5].p[4]) - (this.mat[4].p[4] * this.mat[5].p[3]);
        final float det2_45_35 = (this.mat[4].p[3] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[3]);
        final float det2_45_45 = (this.mat[4].p[4] * this.mat[5].p[5]) - (this.mat[4].p[5] * this.mat[5].p[4]);

        // 3x3 sub-determinants required to calculate 6x6 determinant
        final float det3_345_012 = ((this.mat[3].p[0] * det2_45_12) - (this.mat[3].p[1] * det2_45_02)) + (this.mat[3].p[2] * det2_45_01);
        final float det3_345_013 = ((this.mat[3].p[0] * det2_45_13) - (this.mat[3].p[1] * det2_45_03)) + (this.mat[3].p[3] * det2_45_01);
        final float det3_345_014 = ((this.mat[3].p[0] * det2_45_14) - (this.mat[3].p[1] * det2_45_04)) + (this.mat[3].p[4] * det2_45_01);
        final float det3_345_015 = ((this.mat[3].p[0] * det2_45_15) - (this.mat[3].p[1] * det2_45_05)) + (this.mat[3].p[5] * det2_45_01);
        final float det3_345_023 = ((this.mat[3].p[0] * det2_45_23) - (this.mat[3].p[2] * det2_45_03)) + (this.mat[3].p[3] * det2_45_02);
        final float det3_345_024 = ((this.mat[3].p[0] * det2_45_24) - (this.mat[3].p[2] * det2_45_04)) + (this.mat[3].p[4] * det2_45_02);
        final float det3_345_025 = ((this.mat[3].p[0] * det2_45_25) - (this.mat[3].p[2] * det2_45_05)) + (this.mat[3].p[5] * det2_45_02);
        final float det3_345_034 = ((this.mat[3].p[0] * det2_45_34) - (this.mat[3].p[3] * det2_45_04)) + (this.mat[3].p[4] * det2_45_03);
        final float det3_345_035 = ((this.mat[3].p[0] * det2_45_35) - (this.mat[3].p[3] * det2_45_05)) + (this.mat[3].p[5] * det2_45_03);
        final float det3_345_045 = ((this.mat[3].p[0] * det2_45_45) - (this.mat[3].p[4] * det2_45_05)) + (this.mat[3].p[5] * det2_45_04);
        final float det3_345_123 = ((this.mat[3].p[1] * det2_45_23) - (this.mat[3].p[2] * det2_45_13)) + (this.mat[3].p[3] * det2_45_12);
        final float det3_345_124 = ((this.mat[3].p[1] * det2_45_24) - (this.mat[3].p[2] * det2_45_14)) + (this.mat[3].p[4] * det2_45_12);
        final float det3_345_125 = ((this.mat[3].p[1] * det2_45_25) - (this.mat[3].p[2] * det2_45_15)) + (this.mat[3].p[5] * det2_45_12);
        final float det3_345_134 = ((this.mat[3].p[1] * det2_45_34) - (this.mat[3].p[3] * det2_45_14)) + (this.mat[3].p[4] * det2_45_13);
        final float det3_345_135 = ((this.mat[3].p[1] * det2_45_35) - (this.mat[3].p[3] * det2_45_15)) + (this.mat[3].p[5] * det2_45_13);
        final float det3_345_145 = ((this.mat[3].p[1] * det2_45_45) - (this.mat[3].p[4] * det2_45_15)) + (this.mat[3].p[5] * det2_45_14);
        final float det3_345_234 = ((this.mat[3].p[2] * det2_45_34) - (this.mat[3].p[3] * det2_45_24)) + (this.mat[3].p[4] * det2_45_23);
        final float det3_345_235 = ((this.mat[3].p[2] * det2_45_35) - (this.mat[3].p[3] * det2_45_25)) + (this.mat[3].p[5] * det2_45_23);
        final float det3_345_245 = ((this.mat[3].p[2] * det2_45_45) - (this.mat[3].p[4] * det2_45_25)) + (this.mat[3].p[5] * det2_45_24);
        final float det3_345_345 = ((this.mat[3].p[3] * det2_45_45) - (this.mat[3].p[4] * det2_45_35)) + (this.mat[3].p[5] * det2_45_34);

        // 4x4 sub-determinants required to calculate 6x6 determinant
        final float det4_2345_0123 = (((this.mat[2].p[0] * det3_345_123) - (this.mat[2].p[1] * det3_345_023)) + (this.mat[2].p[2] * det3_345_013)) - (this.mat[2].p[3] * det3_345_012);
        final float det4_2345_0124 = (((this.mat[2].p[0] * det3_345_124) - (this.mat[2].p[1] * det3_345_024)) + (this.mat[2].p[2] * det3_345_014)) - (this.mat[2].p[4] * det3_345_012);
        final float det4_2345_0125 = (((this.mat[2].p[0] * det3_345_125) - (this.mat[2].p[1] * det3_345_025)) + (this.mat[2].p[2] * det3_345_015)) - (this.mat[2].p[5] * det3_345_012);
        final float det4_2345_0134 = (((this.mat[2].p[0] * det3_345_134) - (this.mat[2].p[1] * det3_345_034)) + (this.mat[2].p[3] * det3_345_014)) - (this.mat[2].p[4] * det3_345_013);
        final float det4_2345_0135 = (((this.mat[2].p[0] * det3_345_135) - (this.mat[2].p[1] * det3_345_035)) + (this.mat[2].p[3] * det3_345_015)) - (this.mat[2].p[5] * det3_345_013);
        final float det4_2345_0145 = (((this.mat[2].p[0] * det3_345_145) - (this.mat[2].p[1] * det3_345_045)) + (this.mat[2].p[4] * det3_345_015)) - (this.mat[2].p[5] * det3_345_014);
        final float det4_2345_0234 = (((this.mat[2].p[0] * det3_345_234) - (this.mat[2].p[2] * det3_345_034)) + (this.mat[2].p[3] * det3_345_024)) - (this.mat[2].p[4] * det3_345_023);
        final float det4_2345_0235 = (((this.mat[2].p[0] * det3_345_235) - (this.mat[2].p[2] * det3_345_035)) + (this.mat[2].p[3] * det3_345_025)) - (this.mat[2].p[5] * det3_345_023);
        final float det4_2345_0245 = (((this.mat[2].p[0] * det3_345_245) - (this.mat[2].p[2] * det3_345_045)) + (this.mat[2].p[4] * det3_345_025)) - (this.mat[2].p[5] * det3_345_024);
        final float det4_2345_0345 = (((this.mat[2].p[0] * det3_345_345) - (this.mat[2].p[3] * det3_345_045)) + (this.mat[2].p[4] * det3_345_035)) - (this.mat[2].p[5] * det3_345_034);
        final float det4_2345_1234 = (((this.mat[2].p[1] * det3_345_234) - (this.mat[2].p[2] * det3_345_134)) + (this.mat[2].p[3] * det3_345_124)) - (this.mat[2].p[4] * det3_345_123);
        final float det4_2345_1235 = (((this.mat[2].p[1] * det3_345_235) - (this.mat[2].p[2] * det3_345_135)) + (this.mat[2].p[3] * det3_345_125)) - (this.mat[2].p[5] * det3_345_123);
        final float det4_2345_1245 = (((this.mat[2].p[1] * det3_345_245) - (this.mat[2].p[2] * det3_345_145)) + (this.mat[2].p[4] * det3_345_125)) - (this.mat[2].p[5] * det3_345_124);
        final float det4_2345_1345 = (((this.mat[2].p[1] * det3_345_345) - (this.mat[2].p[3] * det3_345_145)) + (this.mat[2].p[4] * det3_345_135)) - (this.mat[2].p[5] * det3_345_134);
        final float det4_2345_2345 = (((this.mat[2].p[2] * det3_345_345) - (this.mat[2].p[3] * det3_345_245)) + (this.mat[2].p[4] * det3_345_235)) - (this.mat[2].p[5] * det3_345_234);

        // 5x5 sub-determinants required to calculate 6x6 determinant
        final float det5_12345_01234 = ((((this.mat[1].p[0] * det4_2345_1234) - (this.mat[1].p[1] * det4_2345_0234)) + (this.mat[1].p[2] * det4_2345_0134)) - (this.mat[1].p[3] * det4_2345_0124)) + (this.mat[1].p[4] * det4_2345_0123);
        final float det5_12345_01235 = ((((this.mat[1].p[0] * det4_2345_1235) - (this.mat[1].p[1] * det4_2345_0235)) + (this.mat[1].p[2] * det4_2345_0135)) - (this.mat[1].p[3] * det4_2345_0125)) + (this.mat[1].p[5] * det4_2345_0123);
        final float det5_12345_01245 = ((((this.mat[1].p[0] * det4_2345_1245) - (this.mat[1].p[1] * det4_2345_0245)) + (this.mat[1].p[2] * det4_2345_0145)) - (this.mat[1].p[4] * det4_2345_0125)) + (this.mat[1].p[5] * det4_2345_0124);
        final float det5_12345_01345 = ((((this.mat[1].p[0] * det4_2345_1345) - (this.mat[1].p[1] * det4_2345_0345)) + (this.mat[1].p[3] * det4_2345_0145)) - (this.mat[1].p[4] * det4_2345_0135)) + (this.mat[1].p[5] * det4_2345_0134);
        final float det5_12345_02345 = ((((this.mat[1].p[0] * det4_2345_2345) - (this.mat[1].p[2] * det4_2345_0345)) + (this.mat[1].p[3] * det4_2345_0245)) - (this.mat[1].p[4] * det4_2345_0235)) + (this.mat[1].p[5] * det4_2345_0234);
        final float det5_12345_12345 = ((((this.mat[1].p[1] * det4_2345_2345) - (this.mat[1].p[2] * det4_2345_1345)) + (this.mat[1].p[3] * det4_2345_1245)) - (this.mat[1].p[4] * det4_2345_1235)) + (this.mat[1].p[5] * det4_2345_1234);

        // determinant of 6x6 matrix
        det = (((((this.mat[0].p[0] * det5_12345_12345) - (this.mat[0].p[1] * det5_12345_02345)) + (this.mat[0].p[2] * det5_12345_01345))
                - (this.mat[0].p[3] * det5_12345_01245)) + (this.mat[0].p[4] * det5_12345_01235)) - (this.mat[0].p[5] * det5_12345_01234);

        if (idMath.Fabs((float) det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        // remaining 2x2 sub-determinants
        final float det2_34_01 = (this.mat[3].p[0] * this.mat[4].p[1]) - (this.mat[3].p[1] * this.mat[4].p[0]);
        final float det2_34_02 = (this.mat[3].p[0] * this.mat[4].p[2]) - (this.mat[3].p[2] * this.mat[4].p[0]);
        final float det2_34_03 = (this.mat[3].p[0] * this.mat[4].p[3]) - (this.mat[3].p[3] * this.mat[4].p[0]);
        final float det2_34_04 = (this.mat[3].p[0] * this.mat[4].p[4]) - (this.mat[3].p[4] * this.mat[4].p[0]);
        final float det2_34_05 = (this.mat[3].p[0] * this.mat[4].p[5]) - (this.mat[3].p[5] * this.mat[4].p[0]);
        final float det2_34_12 = (this.mat[3].p[1] * this.mat[4].p[2]) - (this.mat[3].p[2] * this.mat[4].p[1]);
        final float det2_34_13 = (this.mat[3].p[1] * this.mat[4].p[3]) - (this.mat[3].p[3] * this.mat[4].p[1]);
        final float det2_34_14 = (this.mat[3].p[1] * this.mat[4].p[4]) - (this.mat[3].p[4] * this.mat[4].p[1]);
        final float det2_34_15 = (this.mat[3].p[1] * this.mat[4].p[5]) - (this.mat[3].p[5] * this.mat[4].p[1]);
        final float det2_34_23 = (this.mat[3].p[2] * this.mat[4].p[3]) - (this.mat[3].p[3] * this.mat[4].p[2]);
        final float det2_34_24 = (this.mat[3].p[2] * this.mat[4].p[4]) - (this.mat[3].p[4] * this.mat[4].p[2]);
        final float det2_34_25 = (this.mat[3].p[2] * this.mat[4].p[5]) - (this.mat[3].p[5] * this.mat[4].p[2]);
        final float det2_34_34 = (this.mat[3].p[3] * this.mat[4].p[4]) - (this.mat[3].p[4] * this.mat[4].p[3]);
        final float det2_34_35 = (this.mat[3].p[3] * this.mat[4].p[5]) - (this.mat[3].p[5] * this.mat[4].p[3]);
        final float det2_34_45 = (this.mat[3].p[4] * this.mat[4].p[5]) - (this.mat[3].p[5] * this.mat[4].p[4]);
        final float det2_35_01 = (this.mat[3].p[0] * this.mat[5].p[1]) - (this.mat[3].p[1] * this.mat[5].p[0]);
        final float det2_35_02 = (this.mat[3].p[0] * this.mat[5].p[2]) - (this.mat[3].p[2] * this.mat[5].p[0]);
        final float det2_35_03 = (this.mat[3].p[0] * this.mat[5].p[3]) - (this.mat[3].p[3] * this.mat[5].p[0]);
        final float det2_35_04 = (this.mat[3].p[0] * this.mat[5].p[4]) - (this.mat[3].p[4] * this.mat[5].p[0]);
        final float det2_35_05 = (this.mat[3].p[0] * this.mat[5].p[5]) - (this.mat[3].p[5] * this.mat[5].p[0]);
        final float det2_35_12 = (this.mat[3].p[1] * this.mat[5].p[2]) - (this.mat[3].p[2] * this.mat[5].p[1]);
        final float det2_35_13 = (this.mat[3].p[1] * this.mat[5].p[3]) - (this.mat[3].p[3] * this.mat[5].p[1]);
        final float det2_35_14 = (this.mat[3].p[1] * this.mat[5].p[4]) - (this.mat[3].p[4] * this.mat[5].p[1]);
        final float det2_35_15 = (this.mat[3].p[1] * this.mat[5].p[5]) - (this.mat[3].p[5] * this.mat[5].p[1]);
        final float det2_35_23 = (this.mat[3].p[2] * this.mat[5].p[3]) - (this.mat[3].p[3] * this.mat[5].p[2]);
        final float det2_35_24 = (this.mat[3].p[2] * this.mat[5].p[4]) - (this.mat[3].p[4] * this.mat[5].p[2]);
        final float det2_35_25 = (this.mat[3].p[2] * this.mat[5].p[5]) - (this.mat[3].p[5] * this.mat[5].p[2]);
        final float det2_35_34 = (this.mat[3].p[3] * this.mat[5].p[4]) - (this.mat[3].p[4] * this.mat[5].p[3]);
        final float det2_35_35 = (this.mat[3].p[3] * this.mat[5].p[5]) - (this.mat[3].p[5] * this.mat[5].p[3]);
        final float det2_35_45 = (this.mat[3].p[4] * this.mat[5].p[5]) - (this.mat[3].p[5] * this.mat[5].p[4]);

        // remaining 3x3 sub-determinants
        final float det3_234_012 = ((this.mat[2].p[0] * det2_34_12) - (this.mat[2].p[1] * det2_34_02)) + (this.mat[2].p[2] * det2_34_01);
        final float det3_234_013 = ((this.mat[2].p[0] * det2_34_13) - (this.mat[2].p[1] * det2_34_03)) + (this.mat[2].p[3] * det2_34_01);
        final float det3_234_014 = ((this.mat[2].p[0] * det2_34_14) - (this.mat[2].p[1] * det2_34_04)) + (this.mat[2].p[4] * det2_34_01);
        final float det3_234_015 = ((this.mat[2].p[0] * det2_34_15) - (this.mat[2].p[1] * det2_34_05)) + (this.mat[2].p[5] * det2_34_01);
        final float det3_234_023 = ((this.mat[2].p[0] * det2_34_23) - (this.mat[2].p[2] * det2_34_03)) + (this.mat[2].p[3] * det2_34_02);
        final float det3_234_024 = ((this.mat[2].p[0] * det2_34_24) - (this.mat[2].p[2] * det2_34_04)) + (this.mat[2].p[4] * det2_34_02);
        final float det3_234_025 = ((this.mat[2].p[0] * det2_34_25) - (this.mat[2].p[2] * det2_34_05)) + (this.mat[2].p[5] * det2_34_02);
        final float det3_234_034 = ((this.mat[2].p[0] * det2_34_34) - (this.mat[2].p[3] * det2_34_04)) + (this.mat[2].p[4] * det2_34_03);
        final float det3_234_035 = ((this.mat[2].p[0] * det2_34_35) - (this.mat[2].p[3] * det2_34_05)) + (this.mat[2].p[5] * det2_34_03);
        final float det3_234_045 = ((this.mat[2].p[0] * det2_34_45) - (this.mat[2].p[4] * det2_34_05)) + (this.mat[2].p[5] * det2_34_04);
        final float det3_234_123 = ((this.mat[2].p[1] * det2_34_23) - (this.mat[2].p[2] * det2_34_13)) + (this.mat[2].p[3] * det2_34_12);
        final float det3_234_124 = ((this.mat[2].p[1] * det2_34_24) - (this.mat[2].p[2] * det2_34_14)) + (this.mat[2].p[4] * det2_34_12);
        final float det3_234_125 = ((this.mat[2].p[1] * det2_34_25) - (this.mat[2].p[2] * det2_34_15)) + (this.mat[2].p[5] * det2_34_12);
        final float det3_234_134 = ((this.mat[2].p[1] * det2_34_34) - (this.mat[2].p[3] * det2_34_14)) + (this.mat[2].p[4] * det2_34_13);
        final float det3_234_135 = ((this.mat[2].p[1] * det2_34_35) - (this.mat[2].p[3] * det2_34_15)) + (this.mat[2].p[5] * det2_34_13);
        final float det3_234_145 = ((this.mat[2].p[1] * det2_34_45) - (this.mat[2].p[4] * det2_34_15)) + (this.mat[2].p[5] * det2_34_14);
        final float det3_234_234 = ((this.mat[2].p[2] * det2_34_34) - (this.mat[2].p[3] * det2_34_24)) + (this.mat[2].p[4] * det2_34_23);
        final float det3_234_235 = ((this.mat[2].p[2] * det2_34_35) - (this.mat[2].p[3] * det2_34_25)) + (this.mat[2].p[5] * det2_34_23);
        final float det3_234_245 = ((this.mat[2].p[2] * det2_34_45) - (this.mat[2].p[4] * det2_34_25)) + (this.mat[2].p[5] * det2_34_24);
        final float det3_234_345 = ((this.mat[2].p[3] * det2_34_45) - (this.mat[2].p[4] * det2_34_35)) + (this.mat[2].p[5] * det2_34_34);
        final float det3_235_012 = ((this.mat[2].p[0] * det2_35_12) - (this.mat[2].p[1] * det2_35_02)) + (this.mat[2].p[2] * det2_35_01);
        final float det3_235_013 = ((this.mat[2].p[0] * det2_35_13) - (this.mat[2].p[1] * det2_35_03)) + (this.mat[2].p[3] * det2_35_01);
        final float det3_235_014 = ((this.mat[2].p[0] * det2_35_14) - (this.mat[2].p[1] * det2_35_04)) + (this.mat[2].p[4] * det2_35_01);
        final float det3_235_015 = ((this.mat[2].p[0] * det2_35_15) - (this.mat[2].p[1] * det2_35_05)) + (this.mat[2].p[5] * det2_35_01);
        final float det3_235_023 = ((this.mat[2].p[0] * det2_35_23) - (this.mat[2].p[2] * det2_35_03)) + (this.mat[2].p[3] * det2_35_02);
        final float det3_235_024 = ((this.mat[2].p[0] * det2_35_24) - (this.mat[2].p[2] * det2_35_04)) + (this.mat[2].p[4] * det2_35_02);
        final float det3_235_025 = ((this.mat[2].p[0] * det2_35_25) - (this.mat[2].p[2] * det2_35_05)) + (this.mat[2].p[5] * det2_35_02);
        final float det3_235_034 = ((this.mat[2].p[0] * det2_35_34) - (this.mat[2].p[3] * det2_35_04)) + (this.mat[2].p[4] * det2_35_03);
        final float det3_235_035 = ((this.mat[2].p[0] * det2_35_35) - (this.mat[2].p[3] * det2_35_05)) + (this.mat[2].p[5] * det2_35_03);
        final float det3_235_045 = ((this.mat[2].p[0] * det2_35_45) - (this.mat[2].p[4] * det2_35_05)) + (this.mat[2].p[5] * det2_35_04);
        final float det3_235_123 = ((this.mat[2].p[1] * det2_35_23) - (this.mat[2].p[2] * det2_35_13)) + (this.mat[2].p[3] * det2_35_12);
        final float det3_235_124 = ((this.mat[2].p[1] * det2_35_24) - (this.mat[2].p[2] * det2_35_14)) + (this.mat[2].p[4] * det2_35_12);
        final float det3_235_125 = ((this.mat[2].p[1] * det2_35_25) - (this.mat[2].p[2] * det2_35_15)) + (this.mat[2].p[5] * det2_35_12);
        final float det3_235_134 = ((this.mat[2].p[1] * det2_35_34) - (this.mat[2].p[3] * det2_35_14)) + (this.mat[2].p[4] * det2_35_13);
        final float det3_235_135 = ((this.mat[2].p[1] * det2_35_35) - (this.mat[2].p[3] * det2_35_15)) + (this.mat[2].p[5] * det2_35_13);
        final float det3_235_145 = ((this.mat[2].p[1] * det2_35_45) - (this.mat[2].p[4] * det2_35_15)) + (this.mat[2].p[5] * det2_35_14);
        final float det3_235_234 = ((this.mat[2].p[2] * det2_35_34) - (this.mat[2].p[3] * det2_35_24)) + (this.mat[2].p[4] * det2_35_23);
        final float det3_235_235 = ((this.mat[2].p[2] * det2_35_35) - (this.mat[2].p[3] * det2_35_25)) + (this.mat[2].p[5] * det2_35_23);
        final float det3_235_245 = ((this.mat[2].p[2] * det2_35_45) - (this.mat[2].p[4] * det2_35_25)) + (this.mat[2].p[5] * det2_35_24);
        final float det3_235_345 = ((this.mat[2].p[3] * det2_35_45) - (this.mat[2].p[4] * det2_35_35)) + (this.mat[2].p[5] * det2_35_34);
        final float det3_245_012 = ((this.mat[2].p[0] * det2_45_12) - (this.mat[2].p[1] * det2_45_02)) + (this.mat[2].p[2] * det2_45_01);
        final float det3_245_013 = ((this.mat[2].p[0] * det2_45_13) - (this.mat[2].p[1] * det2_45_03)) + (this.mat[2].p[3] * det2_45_01);
        final float det3_245_014 = ((this.mat[2].p[0] * det2_45_14) - (this.mat[2].p[1] * det2_45_04)) + (this.mat[2].p[4] * det2_45_01);
        final float det3_245_015 = ((this.mat[2].p[0] * det2_45_15) - (this.mat[2].p[1] * det2_45_05)) + (this.mat[2].p[5] * det2_45_01);
        final float det3_245_023 = ((this.mat[2].p[0] * det2_45_23) - (this.mat[2].p[2] * det2_45_03)) + (this.mat[2].p[3] * det2_45_02);
        final float det3_245_024 = ((this.mat[2].p[0] * det2_45_24) - (this.mat[2].p[2] * det2_45_04)) + (this.mat[2].p[4] * det2_45_02);
        final float det3_245_025 = ((this.mat[2].p[0] * det2_45_25) - (this.mat[2].p[2] * det2_45_05)) + (this.mat[2].p[5] * det2_45_02);
        final float det3_245_034 = ((this.mat[2].p[0] * det2_45_34) - (this.mat[2].p[3] * det2_45_04)) + (this.mat[2].p[4] * det2_45_03);
        final float det3_245_035 = ((this.mat[2].p[0] * det2_45_35) - (this.mat[2].p[3] * det2_45_05)) + (this.mat[2].p[5] * det2_45_03);
        final float det3_245_045 = ((this.mat[2].p[0] * det2_45_45) - (this.mat[2].p[4] * det2_45_05)) + (this.mat[2].p[5] * det2_45_04);
        final float det3_245_123 = ((this.mat[2].p[1] * det2_45_23) - (this.mat[2].p[2] * det2_45_13)) + (this.mat[2].p[3] * det2_45_12);
        final float det3_245_124 = ((this.mat[2].p[1] * det2_45_24) - (this.mat[2].p[2] * det2_45_14)) + (this.mat[2].p[4] * det2_45_12);
        final float det3_245_125 = ((this.mat[2].p[1] * det2_45_25) - (this.mat[2].p[2] * det2_45_15)) + (this.mat[2].p[5] * det2_45_12);
        final float det3_245_134 = ((this.mat[2].p[1] * det2_45_34) - (this.mat[2].p[3] * det2_45_14)) + (this.mat[2].p[4] * det2_45_13);
        final float det3_245_135 = ((this.mat[2].p[1] * det2_45_35) - (this.mat[2].p[3] * det2_45_15)) + (this.mat[2].p[5] * det2_45_13);
        final float det3_245_145 = ((this.mat[2].p[1] * det2_45_45) - (this.mat[2].p[4] * det2_45_15)) + (this.mat[2].p[5] * det2_45_14);
        final float det3_245_234 = ((this.mat[2].p[2] * det2_45_34) - (this.mat[2].p[3] * det2_45_24)) + (this.mat[2].p[4] * det2_45_23);
        final float det3_245_235 = ((this.mat[2].p[2] * det2_45_35) - (this.mat[2].p[3] * det2_45_25)) + (this.mat[2].p[5] * det2_45_23);
        final float det3_245_245 = ((this.mat[2].p[2] * det2_45_45) - (this.mat[2].p[4] * det2_45_25)) + (this.mat[2].p[5] * det2_45_24);
        final float det3_245_345 = ((this.mat[2].p[3] * det2_45_45) - (this.mat[2].p[4] * det2_45_35)) + (this.mat[2].p[5] * det2_45_34);

        // remaining 4x4 sub-determinants
        final float det4_1234_0123 = (((this.mat[1].p[0] * det3_234_123) - (this.mat[1].p[1] * det3_234_023)) + (this.mat[1].p[2] * det3_234_013)) - (this.mat[1].p[3] * det3_234_012);
        final float det4_1234_0124 = (((this.mat[1].p[0] * det3_234_124) - (this.mat[1].p[1] * det3_234_024)) + (this.mat[1].p[2] * det3_234_014)) - (this.mat[1].p[4] * det3_234_012);
        final float det4_1234_0125 = (((this.mat[1].p[0] * det3_234_125) - (this.mat[1].p[1] * det3_234_025)) + (this.mat[1].p[2] * det3_234_015)) - (this.mat[1].p[5] * det3_234_012);
        final float det4_1234_0134 = (((this.mat[1].p[0] * det3_234_134) - (this.mat[1].p[1] * det3_234_034)) + (this.mat[1].p[3] * det3_234_014)) - (this.mat[1].p[4] * det3_234_013);
        final float det4_1234_0135 = (((this.mat[1].p[0] * det3_234_135) - (this.mat[1].p[1] * det3_234_035)) + (this.mat[1].p[3] * det3_234_015)) - (this.mat[1].p[5] * det3_234_013);
        final float det4_1234_0145 = (((this.mat[1].p[0] * det3_234_145) - (this.mat[1].p[1] * det3_234_045)) + (this.mat[1].p[4] * det3_234_015)) - (this.mat[1].p[5] * det3_234_014);
        final float det4_1234_0234 = (((this.mat[1].p[0] * det3_234_234) - (this.mat[1].p[2] * det3_234_034)) + (this.mat[1].p[3] * det3_234_024)) - (this.mat[1].p[4] * det3_234_023);
        final float det4_1234_0235 = (((this.mat[1].p[0] * det3_234_235) - (this.mat[1].p[2] * det3_234_035)) + (this.mat[1].p[3] * det3_234_025)) - (this.mat[1].p[5] * det3_234_023);
        final float det4_1234_0245 = (((this.mat[1].p[0] * det3_234_245) - (this.mat[1].p[2] * det3_234_045)) + (this.mat[1].p[4] * det3_234_025)) - (this.mat[1].p[5] * det3_234_024);
        final float det4_1234_0345 = (((this.mat[1].p[0] * det3_234_345) - (this.mat[1].p[3] * det3_234_045)) + (this.mat[1].p[4] * det3_234_035)) - (this.mat[1].p[5] * det3_234_034);
        final float det4_1234_1234 = (((this.mat[1].p[1] * det3_234_234) - (this.mat[1].p[2] * det3_234_134)) + (this.mat[1].p[3] * det3_234_124)) - (this.mat[1].p[4] * det3_234_123);
        final float det4_1234_1235 = (((this.mat[1].p[1] * det3_234_235) - (this.mat[1].p[2] * det3_234_135)) + (this.mat[1].p[3] * det3_234_125)) - (this.mat[1].p[5] * det3_234_123);
        final float det4_1234_1245 = (((this.mat[1].p[1] * det3_234_245) - (this.mat[1].p[2] * det3_234_145)) + (this.mat[1].p[4] * det3_234_125)) - (this.mat[1].p[5] * det3_234_124);
        final float det4_1234_1345 = (((this.mat[1].p[1] * det3_234_345) - (this.mat[1].p[3] * det3_234_145)) + (this.mat[1].p[4] * det3_234_135)) - (this.mat[1].p[5] * det3_234_134);
        final float det4_1234_2345 = (((this.mat[1].p[2] * det3_234_345) - (this.mat[1].p[3] * det3_234_245)) + (this.mat[1].p[4] * det3_234_235)) - (this.mat[1].p[5] * det3_234_234);
        final float det4_1235_0123 = (((this.mat[1].p[0] * det3_235_123) - (this.mat[1].p[1] * det3_235_023)) + (this.mat[1].p[2] * det3_235_013)) - (this.mat[1].p[3] * det3_235_012);
        final float det4_1235_0124 = (((this.mat[1].p[0] * det3_235_124) - (this.mat[1].p[1] * det3_235_024)) + (this.mat[1].p[2] * det3_235_014)) - (this.mat[1].p[4] * det3_235_012);
        final float det4_1235_0125 = (((this.mat[1].p[0] * det3_235_125) - (this.mat[1].p[1] * det3_235_025)) + (this.mat[1].p[2] * det3_235_015)) - (this.mat[1].p[5] * det3_235_012);
        final float det4_1235_0134 = (((this.mat[1].p[0] * det3_235_134) - (this.mat[1].p[1] * det3_235_034)) + (this.mat[1].p[3] * det3_235_014)) - (this.mat[1].p[4] * det3_235_013);
        final float det4_1235_0135 = (((this.mat[1].p[0] * det3_235_135) - (this.mat[1].p[1] * det3_235_035)) + (this.mat[1].p[3] * det3_235_015)) - (this.mat[1].p[5] * det3_235_013);
        final float det4_1235_0145 = (((this.mat[1].p[0] * det3_235_145) - (this.mat[1].p[1] * det3_235_045)) + (this.mat[1].p[4] * det3_235_015)) - (this.mat[1].p[5] * det3_235_014);
        final float det4_1235_0234 = (((this.mat[1].p[0] * det3_235_234) - (this.mat[1].p[2] * det3_235_034)) + (this.mat[1].p[3] * det3_235_024)) - (this.mat[1].p[4] * det3_235_023);
        final float det4_1235_0235 = (((this.mat[1].p[0] * det3_235_235) - (this.mat[1].p[2] * det3_235_035)) + (this.mat[1].p[3] * det3_235_025)) - (this.mat[1].p[5] * det3_235_023);
        final float det4_1235_0245 = (((this.mat[1].p[0] * det3_235_245) - (this.mat[1].p[2] * det3_235_045)) + (this.mat[1].p[4] * det3_235_025)) - (this.mat[1].p[5] * det3_235_024);
        final float det4_1235_0345 = (((this.mat[1].p[0] * det3_235_345) - (this.mat[1].p[3] * det3_235_045)) + (this.mat[1].p[4] * det3_235_035)) - (this.mat[1].p[5] * det3_235_034);
        final float det4_1235_1234 = (((this.mat[1].p[1] * det3_235_234) - (this.mat[1].p[2] * det3_235_134)) + (this.mat[1].p[3] * det3_235_124)) - (this.mat[1].p[4] * det3_235_123);
        final float det4_1235_1235 = (((this.mat[1].p[1] * det3_235_235) - (this.mat[1].p[2] * det3_235_135)) + (this.mat[1].p[3] * det3_235_125)) - (this.mat[1].p[5] * det3_235_123);
        final float det4_1235_1245 = (((this.mat[1].p[1] * det3_235_245) - (this.mat[1].p[2] * det3_235_145)) + (this.mat[1].p[4] * det3_235_125)) - (this.mat[1].p[5] * det3_235_124);
        final float det4_1235_1345 = (((this.mat[1].p[1] * det3_235_345) - (this.mat[1].p[3] * det3_235_145)) + (this.mat[1].p[4] * det3_235_135)) - (this.mat[1].p[5] * det3_235_134);
        final float det4_1235_2345 = (((this.mat[1].p[2] * det3_235_345) - (this.mat[1].p[3] * det3_235_245)) + (this.mat[1].p[4] * det3_235_235)) - (this.mat[1].p[5] * det3_235_234);
        final float det4_1245_0123 = (((this.mat[1].p[0] * det3_245_123) - (this.mat[1].p[1] * det3_245_023)) + (this.mat[1].p[2] * det3_245_013)) - (this.mat[1].p[3] * det3_245_012);
        final float det4_1245_0124 = (((this.mat[1].p[0] * det3_245_124) - (this.mat[1].p[1] * det3_245_024)) + (this.mat[1].p[2] * det3_245_014)) - (this.mat[1].p[4] * det3_245_012);
        final float det4_1245_0125 = (((this.mat[1].p[0] * det3_245_125) - (this.mat[1].p[1] * det3_245_025)) + (this.mat[1].p[2] * det3_245_015)) - (this.mat[1].p[5] * det3_245_012);
        final float det4_1245_0134 = (((this.mat[1].p[0] * det3_245_134) - (this.mat[1].p[1] * det3_245_034)) + (this.mat[1].p[3] * det3_245_014)) - (this.mat[1].p[4] * det3_245_013);
        final float det4_1245_0135 = (((this.mat[1].p[0] * det3_245_135) - (this.mat[1].p[1] * det3_245_035)) + (this.mat[1].p[3] * det3_245_015)) - (this.mat[1].p[5] * det3_245_013);
        final float det4_1245_0145 = (((this.mat[1].p[0] * det3_245_145) - (this.mat[1].p[1] * det3_245_045)) + (this.mat[1].p[4] * det3_245_015)) - (this.mat[1].p[5] * det3_245_014);
        final float det4_1245_0234 = (((this.mat[1].p[0] * det3_245_234) - (this.mat[1].p[2] * det3_245_034)) + (this.mat[1].p[3] * det3_245_024)) - (this.mat[1].p[4] * det3_245_023);
        final float det4_1245_0235 = (((this.mat[1].p[0] * det3_245_235) - (this.mat[1].p[2] * det3_245_035)) + (this.mat[1].p[3] * det3_245_025)) - (this.mat[1].p[5] * det3_245_023);
        final float det4_1245_0245 = (((this.mat[1].p[0] * det3_245_245) - (this.mat[1].p[2] * det3_245_045)) + (this.mat[1].p[4] * det3_245_025)) - (this.mat[1].p[5] * det3_245_024);
        final float det4_1245_0345 = (((this.mat[1].p[0] * det3_245_345) - (this.mat[1].p[3] * det3_245_045)) + (this.mat[1].p[4] * det3_245_035)) - (this.mat[1].p[5] * det3_245_034);
        final float det4_1245_1234 = (((this.mat[1].p[1] * det3_245_234) - (this.mat[1].p[2] * det3_245_134)) + (this.mat[1].p[3] * det3_245_124)) - (this.mat[1].p[4] * det3_245_123);
        final float det4_1245_1235 = (((this.mat[1].p[1] * det3_245_235) - (this.mat[1].p[2] * det3_245_135)) + (this.mat[1].p[3] * det3_245_125)) - (this.mat[1].p[5] * det3_245_123);
        final float det4_1245_1245 = (((this.mat[1].p[1] * det3_245_245) - (this.mat[1].p[2] * det3_245_145)) + (this.mat[1].p[4] * det3_245_125)) - (this.mat[1].p[5] * det3_245_124);
        final float det4_1245_1345 = (((this.mat[1].p[1] * det3_245_345) - (this.mat[1].p[3] * det3_245_145)) + (this.mat[1].p[4] * det3_245_135)) - (this.mat[1].p[5] * det3_245_134);
        final float det4_1245_2345 = (((this.mat[1].p[2] * det3_245_345) - (this.mat[1].p[3] * det3_245_245)) + (this.mat[1].p[4] * det3_245_235)) - (this.mat[1].p[5] * det3_245_234);
        final float det4_1345_0123 = (((this.mat[1].p[0] * det3_345_123) - (this.mat[1].p[1] * det3_345_023)) + (this.mat[1].p[2] * det3_345_013)) - (this.mat[1].p[3] * det3_345_012);
        final float det4_1345_0124 = (((this.mat[1].p[0] * det3_345_124) - (this.mat[1].p[1] * det3_345_024)) + (this.mat[1].p[2] * det3_345_014)) - (this.mat[1].p[4] * det3_345_012);
        final float det4_1345_0125 = (((this.mat[1].p[0] * det3_345_125) - (this.mat[1].p[1] * det3_345_025)) + (this.mat[1].p[2] * det3_345_015)) - (this.mat[1].p[5] * det3_345_012);
        final float det4_1345_0134 = (((this.mat[1].p[0] * det3_345_134) - (this.mat[1].p[1] * det3_345_034)) + (this.mat[1].p[3] * det3_345_014)) - (this.mat[1].p[4] * det3_345_013);
        final float det4_1345_0135 = (((this.mat[1].p[0] * det3_345_135) - (this.mat[1].p[1] * det3_345_035)) + (this.mat[1].p[3] * det3_345_015)) - (this.mat[1].p[5] * det3_345_013);
        final float det4_1345_0145 = (((this.mat[1].p[0] * det3_345_145) - (this.mat[1].p[1] * det3_345_045)) + (this.mat[1].p[4] * det3_345_015)) - (this.mat[1].p[5] * det3_345_014);
        final float det4_1345_0234 = (((this.mat[1].p[0] * det3_345_234) - (this.mat[1].p[2] * det3_345_034)) + (this.mat[1].p[3] * det3_345_024)) - (this.mat[1].p[4] * det3_345_023);
        final float det4_1345_0235 = (((this.mat[1].p[0] * det3_345_235) - (this.mat[1].p[2] * det3_345_035)) + (this.mat[1].p[3] * det3_345_025)) - (this.mat[1].p[5] * det3_345_023);
        final float det4_1345_0245 = (((this.mat[1].p[0] * det3_345_245) - (this.mat[1].p[2] * det3_345_045)) + (this.mat[1].p[4] * det3_345_025)) - (this.mat[1].p[5] * det3_345_024);
        final float det4_1345_0345 = (((this.mat[1].p[0] * det3_345_345) - (this.mat[1].p[3] * det3_345_045)) + (this.mat[1].p[4] * det3_345_035)) - (this.mat[1].p[5] * det3_345_034);
        final float det4_1345_1234 = (((this.mat[1].p[1] * det3_345_234) - (this.mat[1].p[2] * det3_345_134)) + (this.mat[1].p[3] * det3_345_124)) - (this.mat[1].p[4] * det3_345_123);
        final float det4_1345_1235 = (((this.mat[1].p[1] * det3_345_235) - (this.mat[1].p[2] * det3_345_135)) + (this.mat[1].p[3] * det3_345_125)) - (this.mat[1].p[5] * det3_345_123);
        final float det4_1345_1245 = (((this.mat[1].p[1] * det3_345_245) - (this.mat[1].p[2] * det3_345_145)) + (this.mat[1].p[4] * det3_345_125)) - (this.mat[1].p[5] * det3_345_124);
        final float det4_1345_1345 = (((this.mat[1].p[1] * det3_345_345) - (this.mat[1].p[3] * det3_345_145)) + (this.mat[1].p[4] * det3_345_135)) - (this.mat[1].p[5] * det3_345_134);
        final float det4_1345_2345 = (((this.mat[1].p[2] * det3_345_345) - (this.mat[1].p[3] * det3_345_245)) + (this.mat[1].p[4] * det3_345_235)) - (this.mat[1].p[5] * det3_345_234);

        // remaining 5x5 sub-determinants
        final float det5_01234_01234 = ((((this.mat[0].p[0] * det4_1234_1234) - (this.mat[0].p[1] * det4_1234_0234)) + (this.mat[0].p[2] * det4_1234_0134)) - (this.mat[0].p[3] * det4_1234_0124)) + (this.mat[0].p[4] * det4_1234_0123);
        final float det5_01234_01235 = ((((this.mat[0].p[0] * det4_1234_1235) - (this.mat[0].p[1] * det4_1234_0235)) + (this.mat[0].p[2] * det4_1234_0135)) - (this.mat[0].p[3] * det4_1234_0125)) + (this.mat[0].p[5] * det4_1234_0123);
        final float det5_01234_01245 = ((((this.mat[0].p[0] * det4_1234_1245) - (this.mat[0].p[1] * det4_1234_0245)) + (this.mat[0].p[2] * det4_1234_0145)) - (this.mat[0].p[4] * det4_1234_0125)) + (this.mat[0].p[5] * det4_1234_0124);
        final float det5_01234_01345 = ((((this.mat[0].p[0] * det4_1234_1345) - (this.mat[0].p[1] * det4_1234_0345)) + (this.mat[0].p[3] * det4_1234_0145)) - (this.mat[0].p[4] * det4_1234_0135)) + (this.mat[0].p[5] * det4_1234_0134);
        final float det5_01234_02345 = ((((this.mat[0].p[0] * det4_1234_2345) - (this.mat[0].p[2] * det4_1234_0345)) + (this.mat[0].p[3] * det4_1234_0245)) - (this.mat[0].p[4] * det4_1234_0235)) + (this.mat[0].p[5] * det4_1234_0234);
        final float det5_01234_12345 = ((((this.mat[0].p[1] * det4_1234_2345) - (this.mat[0].p[2] * det4_1234_1345)) + (this.mat[0].p[3] * det4_1234_1245)) - (this.mat[0].p[4] * det4_1234_1235)) + (this.mat[0].p[5] * det4_1234_1234);
        final float det5_01235_01234 = ((((this.mat[0].p[0] * det4_1235_1234) - (this.mat[0].p[1] * det4_1235_0234)) + (this.mat[0].p[2] * det4_1235_0134)) - (this.mat[0].p[3] * det4_1235_0124)) + (this.mat[0].p[4] * det4_1235_0123);
        final float det5_01235_01235 = ((((this.mat[0].p[0] * det4_1235_1235) - (this.mat[0].p[1] * det4_1235_0235)) + (this.mat[0].p[2] * det4_1235_0135)) - (this.mat[0].p[3] * det4_1235_0125)) + (this.mat[0].p[5] * det4_1235_0123);
        final float det5_01235_01245 = ((((this.mat[0].p[0] * det4_1235_1245) - (this.mat[0].p[1] * det4_1235_0245)) + (this.mat[0].p[2] * det4_1235_0145)) - (this.mat[0].p[4] * det4_1235_0125)) + (this.mat[0].p[5] * det4_1235_0124);
        final float det5_01235_01345 = ((((this.mat[0].p[0] * det4_1235_1345) - (this.mat[0].p[1] * det4_1235_0345)) + (this.mat[0].p[3] * det4_1235_0145)) - (this.mat[0].p[4] * det4_1235_0135)) + (this.mat[0].p[5] * det4_1235_0134);
        final float det5_01235_02345 = ((((this.mat[0].p[0] * det4_1235_2345) - (this.mat[0].p[2] * det4_1235_0345)) + (this.mat[0].p[3] * det4_1235_0245)) - (this.mat[0].p[4] * det4_1235_0235)) + (this.mat[0].p[5] * det4_1235_0234);
        final float det5_01235_12345 = ((((this.mat[0].p[1] * det4_1235_2345) - (this.mat[0].p[2] * det4_1235_1345)) + (this.mat[0].p[3] * det4_1235_1245)) - (this.mat[0].p[4] * det4_1235_1235)) + (this.mat[0].p[5] * det4_1235_1234);
        final float det5_01245_01234 = ((((this.mat[0].p[0] * det4_1245_1234) - (this.mat[0].p[1] * det4_1245_0234)) + (this.mat[0].p[2] * det4_1245_0134)) - (this.mat[0].p[3] * det4_1245_0124)) + (this.mat[0].p[4] * det4_1245_0123);
        final float det5_01245_01235 = ((((this.mat[0].p[0] * det4_1245_1235) - (this.mat[0].p[1] * det4_1245_0235)) + (this.mat[0].p[2] * det4_1245_0135)) - (this.mat[0].p[3] * det4_1245_0125)) + (this.mat[0].p[5] * det4_1245_0123);
        final float det5_01245_01245 = ((((this.mat[0].p[0] * det4_1245_1245) - (this.mat[0].p[1] * det4_1245_0245)) + (this.mat[0].p[2] * det4_1245_0145)) - (this.mat[0].p[4] * det4_1245_0125)) + (this.mat[0].p[5] * det4_1245_0124);
        final float det5_01245_01345 = ((((this.mat[0].p[0] * det4_1245_1345) - (this.mat[0].p[1] * det4_1245_0345)) + (this.mat[0].p[3] * det4_1245_0145)) - (this.mat[0].p[4] * det4_1245_0135)) + (this.mat[0].p[5] * det4_1245_0134);
        final float det5_01245_02345 = ((((this.mat[0].p[0] * det4_1245_2345) - (this.mat[0].p[2] * det4_1245_0345)) + (this.mat[0].p[3] * det4_1245_0245)) - (this.mat[0].p[4] * det4_1245_0235)) + (this.mat[0].p[5] * det4_1245_0234);
        final float det5_01245_12345 = ((((this.mat[0].p[1] * det4_1245_2345) - (this.mat[0].p[2] * det4_1245_1345)) + (this.mat[0].p[3] * det4_1245_1245)) - (this.mat[0].p[4] * det4_1245_1235)) + (this.mat[0].p[5] * det4_1245_1234);
        final float det5_01345_01234 = ((((this.mat[0].p[0] * det4_1345_1234) - (this.mat[0].p[1] * det4_1345_0234)) + (this.mat[0].p[2] * det4_1345_0134)) - (this.mat[0].p[3] * det4_1345_0124)) + (this.mat[0].p[4] * det4_1345_0123);
        final float det5_01345_01235 = ((((this.mat[0].p[0] * det4_1345_1235) - (this.mat[0].p[1] * det4_1345_0235)) + (this.mat[0].p[2] * det4_1345_0135)) - (this.mat[0].p[3] * det4_1345_0125)) + (this.mat[0].p[5] * det4_1345_0123);
        final float det5_01345_01245 = ((((this.mat[0].p[0] * det4_1345_1245) - (this.mat[0].p[1] * det4_1345_0245)) + (this.mat[0].p[2] * det4_1345_0145)) - (this.mat[0].p[4] * det4_1345_0125)) + (this.mat[0].p[5] * det4_1345_0124);
        final float det5_01345_01345 = ((((this.mat[0].p[0] * det4_1345_1345) - (this.mat[0].p[1] * det4_1345_0345)) + (this.mat[0].p[3] * det4_1345_0145)) - (this.mat[0].p[4] * det4_1345_0135)) + (this.mat[0].p[5] * det4_1345_0134);
        final float det5_01345_02345 = ((((this.mat[0].p[0] * det4_1345_2345) - (this.mat[0].p[2] * det4_1345_0345)) + (this.mat[0].p[3] * det4_1345_0245)) - (this.mat[0].p[4] * det4_1345_0235)) + (this.mat[0].p[5] * det4_1345_0234);
        final float det5_01345_12345 = ((((this.mat[0].p[1] * det4_1345_2345) - (this.mat[0].p[2] * det4_1345_1345)) + (this.mat[0].p[3] * det4_1345_1245)) - (this.mat[0].p[4] * det4_1345_1235)) + (this.mat[0].p[5] * det4_1345_1234);
        final float det5_02345_01234 = ((((this.mat[0].p[0] * det4_2345_1234) - (this.mat[0].p[1] * det4_2345_0234)) + (this.mat[0].p[2] * det4_2345_0134)) - (this.mat[0].p[3] * det4_2345_0124)) + (this.mat[0].p[4] * det4_2345_0123);
        final float det5_02345_01235 = ((((this.mat[0].p[0] * det4_2345_1235) - (this.mat[0].p[1] * det4_2345_0235)) + (this.mat[0].p[2] * det4_2345_0135)) - (this.mat[0].p[3] * det4_2345_0125)) + (this.mat[0].p[5] * det4_2345_0123);
        final float det5_02345_01245 = ((((this.mat[0].p[0] * det4_2345_1245) - (this.mat[0].p[1] * det4_2345_0245)) + (this.mat[0].p[2] * det4_2345_0145)) - (this.mat[0].p[4] * det4_2345_0125)) + (this.mat[0].p[5] * det4_2345_0124);
        final float det5_02345_01345 = ((((this.mat[0].p[0] * det4_2345_1345) - (this.mat[0].p[1] * det4_2345_0345)) + (this.mat[0].p[3] * det4_2345_0145)) - (this.mat[0].p[4] * det4_2345_0135)) + (this.mat[0].p[5] * det4_2345_0134);
        final float det5_02345_02345 = ((((this.mat[0].p[0] * det4_2345_2345) - (this.mat[0].p[2] * det4_2345_0345)) + (this.mat[0].p[3] * det4_2345_0245)) - (this.mat[0].p[4] * det4_2345_0235)) + (this.mat[0].p[5] * det4_2345_0234);
        final float det5_02345_12345 = ((((this.mat[0].p[1] * det4_2345_2345) - (this.mat[0].p[2] * det4_2345_1345)) + (this.mat[0].p[3] * det4_2345_1245)) - (this.mat[0].p[4] * det4_2345_1235)) + (this.mat[0].p[5] * det4_2345_1234);

        this.mat[0].p[0] = (float) (det5_12345_12345 * invDet);
        this.mat[0].p[1] = (float) (-det5_02345_12345 * invDet);
        this.mat[0].p[2] = (float) (det5_01345_12345 * invDet);
        this.mat[0].p[3] = (float) (-det5_01245_12345 * invDet);
        this.mat[0].p[4] = (float) (det5_01235_12345 * invDet);
        this.mat[0].p[5] = (float) (-det5_01234_12345 * invDet);

        this.mat[1].p[0] = (float) (-det5_12345_02345 * invDet);
        this.mat[1].p[1] = (float) (det5_02345_02345 * invDet);
        this.mat[1].p[2] = (float) (-det5_01345_02345 * invDet);
        this.mat[1].p[3] = (float) (det5_01245_02345 * invDet);
        this.mat[1].p[4] = (float) (-det5_01235_02345 * invDet);
        this.mat[1].p[5] = (float) (det5_01234_02345 * invDet);

        this.mat[2].p[0] = (float) (det5_12345_01345 * invDet);
        this.mat[2].p[1] = (float) (-det5_02345_01345 * invDet);
        this.mat[2].p[2] = (float) (det5_01345_01345 * invDet);
        this.mat[2].p[3] = (float) (-det5_01245_01345 * invDet);
        this.mat[2].p[4] = (float) (det5_01235_01345 * invDet);
        this.mat[2].p[5] = (float) (-det5_01234_01345 * invDet);

        this.mat[3].p[0] = (float) (-det5_12345_01245 * invDet);
        this.mat[3].p[1] = (float) (det5_02345_01245 * invDet);
        this.mat[3].p[2] = (float) (-det5_01345_01245 * invDet);
        this.mat[3].p[3] = (float) (det5_01245_01245 * invDet);
        this.mat[3].p[4] = (float) (-det5_01235_01245 * invDet);
        this.mat[3].p[5] = (float) (det5_01234_01245 * invDet);

        this.mat[4].p[0] = (float) (det5_12345_01235 * invDet);
        this.mat[4].p[1] = (float) (-det5_02345_01235 * invDet);
        this.mat[4].p[2] = (float) (det5_01345_01235 * invDet);
        this.mat[4].p[3] = (float) (-det5_01245_01235 * invDet);
        this.mat[4].p[4] = (float) (det5_01235_01235 * invDet);
        this.mat[4].p[5] = (float) (-det5_01234_01235 * invDet);

        this.mat[5].p[0] = (float) (-det5_12345_01234 * invDet);
        this.mat[5].p[1] = (float) (det5_02345_01234 * invDet);
        this.mat[5].p[2] = (float) (-det5_01345_01234 * invDet);
        this.mat[5].p[3] = (float) (det5_01245_01234 * invDet);
        this.mat[5].p[4] = (float) (-det5_01235_01234 * invDet);
        this.mat[5].p[5] = (float) (det5_01234_01234 * invDet);

        return true;
    }

    public idMat6 InverseFast() {// returns the inverse ( m * m.Inverse() = identity )
        idMat6 invMat;

        invMat = this;
        final boolean r = invMat.InverseFastSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseFastSelf() {// returns false if determinant is zero
//    #else
        // 6*27+2*30 = 222 multiplications
        //		2*1  =	 2 divisions
        final idVec3[] r0 = idMat0.genVec3Array(3), r1 = idMat0.genVec3Array(3), r2 = idMat0.genVec3Array(3), r3 = idMat0.genVec3Array(3);
        float c0, c1, c2, det, invDet;
        final float[] mat = this.reinterpret_cast();

        // r0 = m0.Inverse();
        c0 = (mat[(1 * 6) + 1] * mat[(2 * 6) + 2]) - (mat[(1 * 6) + 2] * mat[(2 * 6) + 1]);
        c1 = (mat[(1 * 6) + 2] * mat[(2 * 6) + 0]) - (mat[(1 * 6) + 0] * mat[(2 * 6) + 2]);
        c2 = (mat[(1 * 6) + 0] * mat[(2 * 6) + 1]) - (mat[(1 * 6) + 1] * mat[(2 * 6) + 0]);

        det = (mat[(0 * 6) + 0] * c0) + (mat[(0 * 6) + 1] * c1) + (mat[(0 * 6) + 2] * c2);

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        r0[0].x = c0 * invDet;
        r0[0].y = ((mat[(0 * 6) + 2] * mat[(2 * 6) + 1]) - (mat[(0 * 6) + 1] * mat[(2 * 6) + 2])) * invDet;
        r0[0].z = ((mat[(0 * 6) + 1] * mat[(1 * 6) + 2]) - (mat[(0 * 6) + 2] * mat[(1 * 6) + 1])) * invDet;
        r0[1].x = c1 * invDet;
        r0[1].y = ((mat[(0 * 6) + 0] * mat[(2 * 6) + 2]) - (mat[(0 * 6) + 2] * mat[(2 * 6) + 0])) * invDet;
        r0[1].z = ((mat[(0 * 6) + 2] * mat[(1 * 6) + 0]) - (mat[(0 * 6) + 0] * mat[(1 * 6) + 2])) * invDet;
        r0[2].x = c2 * invDet;
        r0[2].y = ((mat[(0 * 6) + 1] * mat[(2 * 6) + 0]) - (mat[(0 * 6) + 0] * mat[(2 * 6) + 1])) * invDet;
        r0[2].z = ((mat[(0 * 6) + 0] * mat[(1 * 6) + 1]) - (mat[(0 * 6) + 1] * mat[(1 * 6) + 0])) * invDet;

        // r1 = r0 * m1;
        r1[0].x = (r0[0].x * mat[(0 * 6) + 3]) + (r0[0].y * mat[(1 * 6) + 3]) + (r0[0].z * mat[(2 * 6) + 3]);
        r1[0].y = (r0[0].x * mat[(0 * 6) + 4]) + (r0[0].y * mat[(1 * 6) + 4]) + (r0[0].z * mat[(2 * 6) + 4]);
        r1[0].z = (r0[0].x * mat[(0 * 6) + 5]) + (r0[0].y * mat[(1 * 6) + 5]) + (r0[0].z * mat[(2 * 6) + 5]);
        r1[1].x = (r0[1].x * mat[(0 * 6) + 3]) + (r0[1].y * mat[(1 * 6) + 3]) + (r0[1].z * mat[(2 * 6) + 3]);
        r1[1].y = (r0[1].x * mat[(0 * 6) + 4]) + (r0[1].y * mat[(1 * 6) + 4]) + (r0[1].z * mat[(2 * 6) + 4]);
        r1[1].z = (r0[1].x * mat[(0 * 6) + 5]) + (r0[1].y * mat[(1 * 6) + 5]) + (r0[1].z * mat[(2 * 6) + 5]);
        r1[2].x = (r0[2].x * mat[(0 * 6) + 3]) + (r0[2].y * mat[(1 * 6) + 3]) + (r0[2].z * mat[(2 * 6) + 3]);
        r1[2].y = (r0[2].x * mat[(0 * 6) + 4]) + (r0[2].y * mat[(1 * 6) + 4]) + (r0[2].z * mat[(2 * 6) + 4]);
        r1[2].z = (r0[2].x * mat[(0 * 6) + 5]) + (r0[2].y * mat[(1 * 6) + 5]) + (r0[2].z * mat[(2 * 6) + 5]);

        // r2 = m2 * r1;
        r2[0].x = (mat[(3 * 6) + 0] * r1[0].x) + (mat[(3 * 6) + 1] * r1[1].x) + (mat[(3 * 6) + 2] * r1[2].x);
        r2[0].y = (mat[(3 * 6) + 0] * r1[0].y) + (mat[(3 * 6) + 1] * r1[1].y) + (mat[(3 * 6) + 2] * r1[2].y);
        r2[0].z = (mat[(3 * 6) + 0] * r1[0].z) + (mat[(3 * 6) + 1] * r1[1].z) + (mat[(3 * 6) + 2] * r1[2].z);
        r2[1].x = (mat[(4 * 6) + 0] * r1[0].x) + (mat[(4 * 6) + 1] * r1[1].x) + (mat[(4 * 6) + 2] * r1[2].x);
        r2[1].y = (mat[(4 * 6) + 0] * r1[0].y) + (mat[(4 * 6) + 1] * r1[1].y) + (mat[(4 * 6) + 2] * r1[2].y);
        r2[1].z = (mat[(4 * 6) + 0] * r1[0].z) + (mat[(4 * 6) + 1] * r1[1].z) + (mat[(4 * 6) + 2] * r1[2].z);
        r2[2].x = (mat[(5 * 6) + 0] * r1[0].x) + (mat[(5 * 6) + 1] * r1[1].x) + (mat[(5 * 6) + 2] * r1[2].x);
        r2[2].y = (mat[(5 * 6) + 0] * r1[0].y) + (mat[(5 * 6) + 1] * r1[1].y) + (mat[(5 * 6) + 2] * r1[2].y);
        r2[2].z = (mat[(5 * 6) + 0] * r1[0].z) + (mat[(5 * 6) + 1] * r1[1].z) + (mat[(5 * 6) + 2] * r1[2].z);

        // r3 = r2 - m3;
        r3[0].x = r2[0].x - mat[(3 * 6) + 3];
        r3[0].y = r2[0].y - mat[(3 * 6) + 4];
        r3[0].z = r2[0].z - mat[(3 * 6) + 5];
        r3[1].x = r2[1].x - mat[(4 * 6) + 3];
        r3[1].y = r2[1].y - mat[(4 * 6) + 4];
        r3[1].z = r2[1].z - mat[(4 * 6) + 5];
        r3[2].x = r2[2].x - mat[(5 * 6) + 3];
        r3[2].y = r2[2].y - mat[(5 * 6) + 4];
        r3[2].z = r2[2].z - mat[(5 * 6) + 5];

        // r3.InverseSelf();
        r2[0].x = (r3[1].y * r3[2].z) - (r3[1].z * r3[2].y);
        r2[1].x = (r3[1].z * r3[2].x) - (r3[1].x * r3[2].z);
        r2[2].x = (r3[1].x * r3[2].y) - (r3[1].y * r3[2].x);

        det = (r3[0].x * r2[0].x) + (r3[0].y * r2[1].x) + (r3[0].z * r2[2].x);

        if (idMath.Fabs(det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        r2[0].y = (r3[0].z * r3[2].y) - (r3[0].y * r3[2].z);
        r2[0].z = (r3[0].y * r3[1].z) - (r3[0].z * r3[1].y);
        r2[1].y = (r3[0].x * r3[2].z) - (r3[0].z * r3[2].x);
        r2[1].z = (r3[0].z * r3[1].x) - (r3[0].x * r3[1].z);
        r2[2].y = (r3[0].y * r3[2].x) - (r3[0].x * r3[2].y);
        r2[2].z = (r3[0].x * r3[1].y) - (r3[0].y * r3[1].x);

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
        r2[0].x = (mat[(3 * 6) + 0] * r0[0].x) + (mat[(3 * 6) + 1] * r0[1].x) + (mat[(3 * 6) + 2] * r0[2].x);
        r2[0].y = (mat[(3 * 6) + 0] * r0[0].y) + (mat[(3 * 6) + 1] * r0[1].y) + (mat[(3 * 6) + 2] * r0[2].y);
        r2[0].z = (mat[(3 * 6) + 0] * r0[0].z) + (mat[(3 * 6) + 1] * r0[1].z) + (mat[(3 * 6) + 2] * r0[2].z);
        r2[1].x = (mat[(4 * 6) + 0] * r0[0].x) + (mat[(4 * 6) + 1] * r0[1].x) + (mat[(4 * 6) + 2] * r0[2].x);
        r2[1].y = (mat[(4 * 6) + 0] * r0[0].y) + (mat[(4 * 6) + 1] * r0[1].y) + (mat[(4 * 6) + 2] * r0[2].y);
        r2[1].z = (mat[(4 * 6) + 0] * r0[0].z) + (mat[(4 * 6) + 1] * r0[1].z) + (mat[(4 * 6) + 2] * r0[2].z);
        r2[2].x = (mat[(5 * 6) + 0] * r0[0].x) + (mat[(5 * 6) + 1] * r0[1].x) + (mat[(5 * 6) + 2] * r0[2].x);
        r2[2].y = (mat[(5 * 6) + 0] * r0[0].y) + (mat[(5 * 6) + 1] * r0[1].y) + (mat[(5 * 6) + 2] * r0[2].y);
        r2[2].z = (mat[(5 * 6) + 0] * r0[0].z) + (mat[(5 * 6) + 1] * r0[1].z) + (mat[(5 * 6) + 2] * r0[2].z);

        // m2 = r3 * r2;
        this.mat[3].p[0] = (r3[0].x * r2[0].x) + (r3[0].y * r2[1].x) + (r3[0].z * r2[2].x);
        this.mat[3].p[1] = (r3[0].x * r2[0].y) + (r3[0].y * r2[1].y) + (r3[0].z * r2[2].y);
        this.mat[3].p[2] = (r3[0].x * r2[0].z) + (r3[0].y * r2[1].z) + (r3[0].z * r2[2].z);
        this.mat[4].p[0] = (r3[1].x * r2[0].x) + (r3[1].y * r2[1].x) + (r3[1].z * r2[2].x);
        this.mat[4].p[1] = (r3[1].x * r2[0].y) + (r3[1].y * r2[1].y) + (r3[1].z * r2[2].y);
        this.mat[4].p[2] = (r3[1].x * r2[0].z) + (r3[1].y * r2[1].z) + (r3[1].z * r2[2].z);
        this.mat[5].p[0] = (r3[2].x * r2[0].x) + (r3[2].y * r2[1].x) + (r3[2].z * r2[2].x);
        this.mat[5].p[1] = (r3[2].x * r2[0].y) + (r3[2].y * r2[1].y) + (r3[2].z * r2[2].y);
        this.mat[5].p[2] = (r3[2].x * r2[0].z) + (r3[2].y * r2[1].z) + (r3[2].z * r2[2].z);

        // m0 = r0 - r1 * m2;
        this.mat[0].p[0] = r0[0].x - (r1[0].x * this.mat[3].p[0]) - (r1[0].y * this.mat[4].p[0]) - (r1[0].z * this.mat[5].p[0]);
        this.mat[0].p[1] = r0[0].y - (r1[0].x * this.mat[3].p[1]) - (r1[0].y * this.mat[4].p[1]) - (r1[0].z * this.mat[5].p[1]);
        this.mat[0].p[2] = r0[0].z - (r1[0].x * this.mat[3].p[2]) - (r1[0].y * this.mat[4].p[2]) - (r1[0].z * this.mat[5].p[2]);
        this.mat[1].p[0] = r0[1].x - (r1[1].x * this.mat[3].p[0]) - (r1[1].y * this.mat[4].p[0]) - (r1[1].z * this.mat[5].p[0]);
        this.mat[1].p[1] = r0[1].y - (r1[1].x * this.mat[3].p[1]) - (r1[1].y * this.mat[4].p[1]) - (r1[1].z * this.mat[5].p[1]);
        this.mat[1].p[2] = r0[1].z - (r1[1].x * this.mat[3].p[2]) - (r1[1].y * this.mat[4].p[2]) - (r1[1].z * this.mat[5].p[2]);
        this.mat[2].p[0] = r0[2].x - (r1[2].x * this.mat[3].p[0]) - (r1[2].y * this.mat[4].p[0]) - (r1[2].z * this.mat[5].p[0]);
        this.mat[2].p[1] = r0[2].y - (r1[2].x * this.mat[3].p[1]) - (r1[2].y * this.mat[4].p[1]) - (r1[2].z * this.mat[5].p[1]);
        this.mat[2].p[2] = r0[2].z - (r1[2].x * this.mat[3].p[2]) - (r1[2].y * this.mat[4].p[2]) - (r1[2].z * this.mat[5].p[2]);

        // m1 = r1 * r3;
        this.mat[0].p[3] = (r1[0].x * r3[0].x) + (r1[0].y * r3[1].x) + (r1[0].z * r3[2].x);
        this.mat[0].p[4] = (r1[0].x * r3[0].y) + (r1[0].y * r3[1].y) + (r1[0].z * r3[2].y);
        this.mat[0].p[5] = (r1[0].x * r3[0].z) + (r1[0].y * r3[1].z) + (r1[0].z * r3[2].z);
        this.mat[1].p[3] = (r1[1].x * r3[0].x) + (r1[1].y * r3[1].x) + (r1[1].z * r3[2].x);
        this.mat[1].p[4] = (r1[1].x * r3[0].y) + (r1[1].y * r3[1].y) + (r1[1].z * r3[2].y);
        this.mat[1].p[5] = (r1[1].x * r3[0].z) + (r1[1].y * r3[1].z) + (r1[1].z * r3[2].z);
        this.mat[2].p[3] = (r1[2].x * r3[0].x) + (r1[2].y * r3[1].x) + (r1[2].z * r3[2].x);
        this.mat[2].p[4] = (r1[2].x * r3[0].y) + (r1[2].y * r3[1].y) + (r1[2].z * r3[2].y);
        this.mat[2].p[5] = (r1[2].x * r3[0].z) + (r1[2].y * r3[1].z) + (r1[2].z * r3[2].z);

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
        final float[] temp = new float[size * size];

        for (int x = 0; x < size; x++) {
            Nio.arraycopy(this.mat[x].p, 0, temp, (x * 6) + 0, size);
        }
        return temp;
    }

}
