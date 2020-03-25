package neo.idlib.math.Matrix;

import static neo.idlib.math.Matrix.idMat0.MATRIX_EPSILON;
import static neo.idlib.math.Matrix.idMat0.MATRIX_INVERSE_EPSILON;

import java.util.Arrays;

import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec2;

/**
 * 1x1 is too complex, so we'll skip to 2x2.
 */
//===============================================================
//
//	idMat2 - 2x2 matrix
//
//===============================================================
public class idMat2 {

    private static final idMat2 mat2_zero = new idMat2(new idVec2(0, 0), new idVec2(0, 0));
    private static final idMat2 mat2_identity = new idMat2(new idVec2(1, 0), new idVec2(0, 1));

    public static idMat2 getMat2_zero() {
        return new idMat2(mat2_zero);
    }

    public static idMat2 getMat2_identity() {
        return new idMat2(mat2_identity);
    }   
    

    private final idVec2[] mat = {new idVec2(), new idVec2()};

    public idMat2() {
    }

    public idMat2(final idVec2 x, final idVec2 y) {
        this.mat[0].x = x.x;
        this.mat[0].y = x.y;
        this.mat[1].x = y.x;
        this.mat[1].y = y.y;
    }

    public idMat2(final float xx, final float xy, final float yx, final float yy) {
        this.mat[0].x = xx;
        this.mat[0].y = xy;
        this.mat[1].x = yx;
        this.mat[1].y = yy;
    }

    public idMat2(final float src[][]) {
//	memcpy( mat, src, 2 * 2 * sizeof( float ) );
        this.mat[0] = new idVec2(src[0][0], src[0][1]);
        this.mat[1] = new idVec2(src[1][0], src[1][1]);
    }

    public idMat2(final idMat2 m){
        this(m.mat[0], m.mat[1]);
    }

//public	const idVec2 &	operator[]( int index ) const;
//public	idVec2 &		operator[]( int index );
    public idVec2 oGet(int index) {
        return this.mat[index];
    }

//public	idMat2			operator-() const;
    public idMat2 oNegative() {
        return new idMat2(-this.mat[0].x, -this.mat[0].y,
                -this.mat[1].x, -this.mat[1].y);
    }

//public	idMat2			operator*( const float a ) const;
    public idMat2 oMultiply(final float a) {
        return new idMat2(
                this.mat[0].x * a, this.mat[0].y * a,
                this.mat[1].x * a, this.mat[1].y * a);
    }
//public	idVec2			operator*( const idVec2 &vec ) const;

    public idVec2 oMultiply(final idVec2 vec) {
        return new idVec2(
                (this.mat[0].x * vec.x) + (this.mat[0].y * vec.y),
                (this.mat[1].x * vec.x) + (this.mat[1].y * vec.y));
    }
//public	idMat2			operator*( const idMat2 &a ) const;

    public idMat2 oMultiply(final idMat2 a) {
        return new idMat2(
                (this.mat[0].x * a.mat[0].x) + (this.mat[0].y * a.mat[1].x),
                (this.mat[0].x * a.mat[0].y) + (this.mat[0].y * a.mat[1].y),
                (this.mat[1].x * a.mat[0].x) + (this.mat[1].y * a.mat[1].x),
                (this.mat[1].x * a.mat[0].y) + (this.mat[1].y * a.mat[1].y));
    }
//public	idMat2			operator+( const idMat2 &a ) const;

    public idMat2 oPlus(final idMat2 a) {
        return new idMat2(
                this.mat[0].x + a.mat[0].x, this.mat[0].y + a.mat[0].y,
                this.mat[1].x + a.mat[1].x, this.mat[1].y + a.mat[1].y);
    }

    public idMat2 oMinus(final idMat2 a) {
        return new idMat2(
                this.mat[0].x - a.mat[0].x, this.mat[0].y - a.mat[0].y,
                this.mat[1].x - a.mat[1].x, this.mat[1].y - a.mat[1].y);
    }
//public	idMat2 &		operator*=( const float a );

    public idMat2 oMulSet(final float a) {
        this.mat[0].x *= a;
        this.mat[0].y *= a;
        this.mat[1].x *= a;
        this.mat[1].y *= a;

        return this;
    }
//public	idMat2 &		operator*=( const idMat2 &a );

    public idMat2 oMulSet(final idMat2 a) {
        float x, y;
        x = this.mat[0].x;
        y = this.mat[0].y;
        this.mat[0].x = (x * a.mat[0].x) + (y * a.mat[1].x);
        this.mat[0].y = (x * a.mat[0].y) + (y * a.mat[1].y);
        x = this.mat[1].x;
        y = this.mat[1].y;
        this.mat[1].x = (x * a.mat[0].x) + (y * a.mat[1].x);
        this.mat[1].y = (x * a.mat[0].y) + (y * a.mat[1].y);
        return this;
    }
//public	idMat2 &		operator+=( const idMat2 &a );

    public idMat2 oPluSet(final idMat2 a) {
        this.mat[0].x += a.mat[0].x;
        this.mat[0].y += a.mat[0].y;
        this.mat[1].x += a.mat[1].x;
        this.mat[1].y += a.mat[1].y;

        return this;
    }
//public	idMat2 &		operator-=( const idMat2 &a );

    public idMat2 oMinSet(final idMat2 a) {
        this.mat[0].x -= a.mat[0].x;
        this.mat[0].y -= a.mat[0].y;
        this.mat[1].x -= a.mat[1].x;
        this.mat[1].y -= a.mat[1].y;

        return this;
    }

//public	friend idMat2	operator*( const float a, const idMat2 &mat );
//public	friend idVec2	operator*( const idVec2 &vec, const idMat2 &mat );
//public	friend idVec2 &	operator*=( idVec2 &vec, const idMat2 &mat );
//public	bool			Compare( const idMat2 &a ) const;						// exact compare, no epsilon
    public boolean Compare(final idMat2 a) {// exact compare, no epsilon
        if (this.mat[0].Compare(a.mat[0])
                && this.mat[1].Compare(a.mat[1])) {
            return true;
        }
        return false;
    }
//public	bool			Compare( const idMat2 &a, const float epsilon ) const;	// compare with epsilon

    public boolean Compare(final idMat2 a, final float epsilon) {// compare with epsilon
        if (this.mat[0].Compare(a.mat[0], epsilon)
                && this.mat[1].Compare(a.mat[1], epsilon)) {
            return true;
        }
        return false;
    }
//public	bool			operator==( const idMat2 &a ) const;					// exact compare, no epsilon
//public	bool			operator!=( const idMat2 &a ) const;					// exact compare, no epsilon

    @Override
    public int hashCode() {
        int hash = 7;
        hash = (83 * hash) + Arrays.deepHashCode(this.mat);
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
        final idMat2 other = (idMat2) obj;
        if (!Arrays.deepEquals(this.mat, other.mat)) {
            return false;
        }
        return true;
    }

    public void Zero() {
        this.mat[0].Zero();
        this.mat[1].Zero();
    }

    public void Identity() {
        this.mat[0] = getMat2_identity().mat[0];
        this.mat[1] = getMat2_identity().mat[1];
    }

    public boolean IsIdentity() {
        return IsIdentity((float) MATRIX_EPSILON);
    }

    public boolean IsIdentity(final float epsilon) {
        return Compare(getMat2_identity(), epsilon);
    }

    public boolean IsSymmetric() {
        return IsSymmetric((float) MATRIX_EPSILON);
    }

    public boolean IsSymmetric(final float epsilon) {
        return (idMath.Fabs(this.mat[0].y - this.mat[1].x) < epsilon);
    }

    public boolean IsDiagonal() {
        return IsDiagonal((float) MATRIX_EPSILON);
    }

    public boolean IsDiagonal(final float epsilon) {
        if ((idMath.Fabs(this.mat[0].y) > epsilon)
                || (idMath.Fabs(this.mat[1].x) > epsilon)) {
            return false;
        }
        return true;
    }

    public float Trace() {
        return (this.mat[0].x + this.mat[1].y);
    }

    public float Determinant() {
        return (this.mat[0].x * this.mat[1].y) - (this.mat[0].y * this.mat[1].x);
    }

    public idMat2 Transpose() {// returns transpose
        return new idMat2(this.mat[0].x, this.mat[1].x,
                this.mat[0].y, this.mat[1].y);
    }

    public idMat2 TransposeSelf() {
        float tmp;

        tmp = this.mat[0].x;
        this.mat[0].y = this.mat[1].x;
        this.mat[1].x = tmp;

        return this;
    }

    public idMat2 Inverse() {// returns the inverse ( m * m.Inverse() = identity )
        idMat2 invMat;

        invMat = this;
        final boolean r = invMat.InverseSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseSelf() {// returns false if determinant is zero
        // 2+4 = 6 multiplications
        //		 1 division
//	double det, invDet, a;
        double det, invDet, a;

        det = this.Determinant();//	det = mat[0][0] * mat[1][1] - mat[0][1] * mat[1][0];

        if (idMath.Fabs((float) det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        a = this.mat[0].x;
        this.mat[0].x = (float) (this.mat[1].y * invDet);
        this.mat[0].y = (float) (-this.mat[0].y * invDet);
        this.mat[1].x = (float) (-this.mat[1].x * invDet);
        this.mat[1].y = (float) (a * invDet);

        return true;
    }

    public idMat2 InverseFast() {// returns the inverse ( m * m.Inverse() = identity )
        idMat2 invMat;

        invMat = this;
        final boolean r = invMat.InverseFastSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseFastSelf() {// returns false if determinant is zero
//#if 1
        // 2+4 = 6 multiplications
        //		 1 division
        double det, invDet, a;

        det = this.Determinant();//	det = mat[0][0] * mat[1][1] - mat[0][1] * mat[1][0];

        if (idMath.Fabs((float) det) < MATRIX_INVERSE_EPSILON) {
            return false;
        }

        invDet = 1.0f / det;

        a = this.mat[0].x;
        this.mat[0].x = (float) (this.mat[1].y * invDet);
        this.mat[0].y = (float) (-this.mat[0].y * invDet);
        this.mat[1].x = (float) (-this.mat[1].x * invDet);
        this.mat[1].y = (float) (a * invDet);

        return true;
    }

    public final int GetDimension() {
        return 4;
    }

    @Deprecated
    public float[] ToFloatPtr() {
        return this.mat[0].ToFloatPtr();
    }
//public	float *			ToFloatPtr( void );

    public String ToString() {
        return ToString(2);
    }

    public String ToString(int precision) {
        return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
    }

    float[] reinterpret_cast() {
        final int size = 2;

        final float[] temp = new float[size * size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                temp[(x * size) + y] = this.mat[x].oGet(y);
            }
        }
        return temp;
    }
}
