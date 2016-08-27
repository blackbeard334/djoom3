package neo.idlib.math.Matrix;

import java.util.Arrays;
import neo.idlib.Lib;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Matrix.idMat0.MATRIX_EPSILON;
import static neo.idlib.math.Matrix.idMat0.MATRIX_INVERSE_EPSILON;
import neo.idlib.math.Random;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec5;
import neo.idlib.math.Vector.idVec6;
import neo.idlib.math.Vector.idVecX;
import static neo.idlib.math.Vector.idVecX.VECX_ALLOCA;

public class idMatX {
//===============================================================
//
//	idMatX - arbitrary sized dense real matrix
//
//  The matrix lives on 16 byte aligned and 16 byte padded memory.
//
//	NOTE: due to the temporary memory pool idMatX cannot be used by multiple threads.
//
//===============================================================

    static final  int     MATX_MAX_TEMP       = 1024;
    //
    public static boolean DISABLE_RANDOM_TEST = false;
    //
    private int     numRows;                // number of rows
    private int     numColumns;             // number of columns
    private int     alloced;                // floats allocated, if -1 then mat points to data set with SetData
    private float[] mat;                    // memory the matrix is stored
    private static float[] temp = new float[MATX_MAX_TEMP + 4];    // used to store intermediate results
    private static int tempPtr;             // pointer to 16 byte aligned temporary memory
    private static int tempIndex;           // index into memory pool, wraps around
    //
    //

    static float[] MATX_QUAD(int x) {
        return new float[(((x) + 3) & ~3)];
    }

    void MATX_CLEAREND() {
        int s = numRows * numColumns;
        while (s < ((s + 3) & ~3)) {
            mat[s++] = 0.0f;
        }
    }

    public static float[] MATX_ALLOCA(int n) {
        return MATX_QUAD(n);
    }
//#define MATX_SIMD

    public idMatX() {
        numRows = numColumns = alloced = 0;
        mat = null;
    }

    public idMatX(int rows, int columns) {
        numRows = numColumns = alloced = 0;
        mat = null;
        SetSize(rows, columns);
    }

    public idMatX(int rows, int columns, float[] src) {
        numRows = numColumns = alloced = 0;
        mat = null;
        SetData(rows, columns, src);
    }

    public idMatX(idMatX matX) {
        this.oSet(matX);
    }
//public					~idMatX( void );

    public void Set(int rows, int columns, final float[] src) {
        SetSize(rows, columns);
//	memcpy( this->mat, src, rows * columns * sizeof( float ) );
        System.arraycopy(src, 0, mat, 0, src.length);
    }

    public void Set(final idMat3 m1, final idMat3 m2) {
        int i, j;

        SetSize(3, 6);
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                mat[(i + 0) * numColumns + (j + 0)] = m1.mat[i].oGet(j);
                mat[(i + 0) * numColumns + (j + 3)] = m2.mat[i].oGet(j);
            }
        }
    }

    public void Set(final idMat3 m1, final idMat3 m2, final idMat3 m3, final idMat3 m4) {
        int i, j;

        SetSize(6, 6);
        for (i = 0; i < 3; i++) {
            for (j = 0; j < 3; j++) {
                mat[(i + 0) * numColumns + (j + 0)] = m1.mat[i].oGet(j);
                mat[(i + 0) * numColumns + (j + 3)] = m2.mat[i].oGet(j);
                mat[(i + 3) * numColumns + (j + 0)] = m3.mat[i].oGet(j);
                mat[(i + 3) * numColumns + (j + 3)] = m4.mat[i].oGet(j);
            }
        }
    }

//public	const float *	operator[]( int index ) const;
//public	float *			operator[]( int index );
    @Deprecated
    public float[] oGet(int index) {////TODO:by sub array by reference
        return Arrays.copyOfRange(mat, index * numColumns, mat.length);
    }

//public	idMatX &		operator=( const idMatX &a );
    public idMatX oSet(final idMatX a) {
        SetSize(a.numRows, a.numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->Copy16( mat, a.mat, a.numRows * a.numColumns );
//#else
//	memcpy( mat, a.mat, a.numRows * a.numColumns * sizeof( float ) );
//#endif
        idMatX.tempIndex = 0;
        System.arraycopy(a.mat, 0, mat, 0, a.numRows * a.numColumns);
        return this;
    }
//public	idMatX			operator*( const float a ) const;

    public idMatX oMultiply(final float a) {
        idMatX m = new idMatX();

        m.SetTempSize(numRows, numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->Mul16( m.mat, mat, a, numRows * numColumns );
//#else
        int i, s;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            m.mat[i] = mat[i] * a;
        }
//#endif
        return m;
    }
//public	idVecX			operator*( const idVecX &vec ) const;

    public idVecX oMultiply(final idVecX vec) {
        idVecX dst = new idVecX();

        assert (numColumns == vec.GetSize());

        dst.SetTempSize(numRows);
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_MultiplyVecX( dst, *this, vec );
//#else
        Multiply(dst, vec);
//#endif
        return dst;
    }
//public	idMatX			operator*( const idMatX &a ) const;

    public idMatX oMultiply(final idMatX a) {
        idMatX dst = new idMatX();

        assert (numColumns == a.numRows);

        dst.SetTempSize(numRows, a.numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_MultiplyMatX( dst, *this, a );
//#else
        Multiply(dst, a);
//#endif
        return dst;
    }
//public	idMatX			operator+( const idMatX &a ) const;

    public idMatX oPlus(final idMatX a) {
        idMatX m = new idMatX();

        assert (numRows == a.numRows && numColumns == a.numColumns);
        m.SetTempSize(numRows, numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->Add16( m.mat, mat, a.mat, numRows * numColumns );
//#else
        int i, s;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            m.mat[i] = mat[i] + a.mat[i];
        }
//#endif
        return m;
    }
//public	idMatX			operator-( const idMatX &a ) const;

    public idMatX oMinus(final idMatX a) {
        idMatX m = new idMatX();

        assert (numRows == a.numRows && numColumns == a.numColumns);
        m.SetTempSize(numRows, numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->Sub16( m.mat, mat, a.mat, numRows * numColumns );
//#else
        int i, s;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            m.mat[i] = mat[i] - a.mat[i];
        }
//#endif
        return m;
    }
//public	idMatX &		operator*=( const float a );

    public idMatX oMulSet(final float a) {
//#ifdef MATX_SIMD
//	SIMDProcessor->MulAssign16( mat, a, numRows * numColumns );
//#else
        int i, s;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            mat[i] *= a;
        }
//#endif
        idMatX.tempIndex = 0;
        return this;
    }
//public	idMatX &		operator*=( const idMatX &a );

    public idMatX oMulSet(final idMatX a) {
        this.oSet(this.oMultiply(a));
        idMatX.tempIndex = 0;
        return this;
    }
//public	idMatX &		operator+=( const idMatX &a );

    public idMatX oPluSet(final idMatX a) {
        assert (numRows == a.numRows && numColumns == a.numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->AddAssign16( mat, a.mat, numRows * numColumns );
//#else
        int i, s;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            mat[i] += a.mat[i];
        }
//#endif
        idMatX.tempIndex = 0;
        return this;
    }

//public	idMatX &		operator-=( const idMatX &a );
    public idMatX oMinSet(final idMatX a) {
        assert (numRows == a.numRows && numColumns == a.numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->SubAssign16( mat, a.mat, numRows * numColumns );
//#else
        int i, s;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            mat[i] -= a.mat[i];
        }
//#endif
        idMatX.tempIndex = 0;
        return this;
    }

//public	friend idMatX	operator*( const float a, const idMatX &m );
    public static idMatX oMultiply(final float a, final idMatX m) {
        return m.oMultiply(a);
    }
//public	friend idVecX	operator*( const idVecX &vec, const idMatX &m );
//public	static idVecX	oMultiply( final idVecX vec, final idMatX m ){
//	return m.oMultiply(vec);
//}
//public	friend idVecX &	operator*=( idVecX &vec, const idMatX &m );

    public static idVecX oMultiply(idVecX vec, final idMatX m) {
        vec = m.oMultiply(vec);
        return vec;
    }

    // exact compare, no epsilon
    public boolean Compare(final idMatX a) {
        int i, s;

        assert (numRows == a.numRows && numColumns == a.numColumns);

        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            if (mat[i] != a.mat[i]) {
                return false;
            }
        }
        return true;
    }

    // compare with epsilon
    public boolean Compare(final idMatX a, final float epsilon) {
        int i, s;

        assert (numRows == a.numRows && numColumns == a.numColumns);

        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            if (idMath.Fabs(mat[i] - a.mat[i]) > epsilon) {
                return false;
            }
        }
        return true;
    }
//public	bool			operator==( const idMatX &a ) const;							// exact compare, no epsilon
//public	bool			operator!=( const idMatX &a ) const;							// exact compare, no epsilon

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Arrays.hashCode(this.mat);
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
        final idMatX other = (idMatX) obj;
        if (!Arrays.equals(this.mat, other.mat)) {
            return false;
        }
        return true;
    }

    // set the number of rows/columns
    public void SetSize(int rows, int columns) {
//            assert (mat < idMatX.tempPtr || mat > idMatX.tempPtr + MATX_MAX_TEMP);
        int alloc = (rows * columns + 3) & ~3;
        if (alloc > alloced && alloced != -1) {
            if (mat != null) {
//			Mem_Free16( mat );
                mat = null;//useless, but gives you a feeling of superiority.
            }
//		mat = (float *) Mem_Alloc16( alloc * sizeof( float ) );
            mat = new float[alloc];
            alloced = alloc;
        }
        numRows = rows;
        numColumns = columns;
//	MATX_CLEAREND();
    }

    public void ChangeSize(int rows, int columns) {
        ChangeSize(rows, columns, false);
    }

    // change the size keeping data intact where possible
    public void ChangeSize(int rows, int columns, boolean makeZero) {
        int alloc = (rows * columns + 3) & ~3;
        if (alloc > alloced && alloced != -1) {
            float[] oldMat = mat;
            mat = new float[alloc];
            if (makeZero) {
//			memset( mat, 0, alloc * sizeof( float ) );
                Arrays.fill(mat, 0, alloc, 0);
            }
            alloced = alloc;
            if (oldMat != null) {//TODO:wthfuck?
                int minRow = Lib.Min(numRows, rows);
                int minColumn = Lib.Min(numColumns, columns);
                for (int i = 0; i < minRow; i++) {
                    System.arraycopy(oldMat, i * numColumns + 0, mat, i * columns + 0, minColumn);
                }
//			Mem_Free16( oldMat );
            }
        } else {
            if (columns < numColumns) {
                int minRow = Lib.Min(numRows, rows);
                for (int i = 0; i < minRow; i++) {
                    System.arraycopy(mat, i * numColumns + 0, mat, i * columns + 0, columns);
                }
            } else if (columns > numColumns) {
                for (int i = Lib.Min(numRows, rows) - 1; i >= 0; i--) {
                    if (makeZero) {
                        for (int j = columns - 1; j >= numColumns; j--) {
                            mat[ i * columns + j] = 0.0f;
                        }
                    }
                    System.arraycopy(mat, i * numColumns + 0, mat, i * columns + 0, numColumns - 1 + 1);
                }
            }
            if (makeZero && rows > numRows) {
//			memset( mat + numRows * columns, 0, ( rows - numRows ) * columns * sizeof( float ) );
                int from = numRows * columns;
                int length = (rows - numRows) * columns;
                int to = from + length;
                Arrays.fill(mat, from, to, 0);
            }
        }
        numRows = rows;
        numColumns = columns;
//	MATX_CLEAREND();
    }

    public int GetNumRows() {
        return numRows;
    }					// get the number of rows

    public int GetNumColumns() {
        return numColumns;
    }				// get the number of columns

    public void SetData(int rows, int columns, float[] data) {// set float array pointer
//            assert (mat < idMatX.tempPtr || mat > idMatX.tempPtr + MATX_MAX_TEMP);
        if (mat != null && alloced != -1) {
//		Mem_Free16( mat );
        }
        assert ((data.length & 15) == 0); // data must be 16 byte aligned
        mat = data;
        alloced = -1;
        numRows = rows;
        numColumns = columns;
//	MATX_CLEAREND();
    }

    // clear matrix
    public void Zero() {
        Arrays.fill(mat, 0);
    }

    // set size and clear matrix
    public void Zero(int rows, int columns) {
        SetSize(rows, columns);
        Arrays.fill(mat, 0, rows * columns, 0);
    }

    // clear to identity matrix
    public void Identity() {
        assert (numRows == numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->Zero16( mat, numRows * numColumns );
//#else
//	memset( mat, 0, numRows * numColumns * sizeof( float ) );
        Arrays.fill(mat, 0, numRows * numColumns, 0);
//#endif
        for (int i = 0; i < numRows; i++) {
            mat[i * numColumns + i] = 1.0f;
        }
    }

    public void Identity(int rows, int columns) {// set size and clear to identity matrix     
        assert (rows == columns);
        SetSize(rows, columns);
        this.Identity();
    }

    // create diagonal matrix from vector
    public void Diag(final idVecX v) {
        Zero(v.GetSize(), v.GetSize());
        for ( int i = 0; i < v.GetSize(); i++ ) {
            mat[i * numColumns + i] = v.oGet(i);
        }
    }

    public void Random(int seed) {
        this.Random(seed, 0.0f);
    }

    public void Random(int seed, float l) {
        this.Random(seed, l, 1.0f);
    }

    public void Random(int seed, float l, float u) {// fill matrix with random values
        int i, s;
        float c;
        idRandom rnd = new Random.idRandom(seed);

        c = u - l;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            mat[i] = l + rnd.RandomFloat() * c;
        }
    }

    public void Random(int rows, int columns, int seed) {
        this.Random(rows, columns, seed, 0.0f);
    }

    public void Random(int rows, int columns, int seed, float l) {
        this.Random(rows, columns, seed, l, 1.0f);
    }

    public void Random(int rows, int columns, int seed, float l, float u) {
        int i, s;
        float c;
        idRandom rnd = new Random.idRandom(seed);

        SetSize(rows, columns);
        c = u - l;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            if (DISABLE_RANDOM_TEST) {//for testing.
                mat[i] = i;
            } else {
                mat[i] = l + rnd.RandomFloat() * c;
            }
        }
    }

    public void Negate() {// (*this) = - (*this)
//#ifdef MATX_SIMD
//	SIMDProcessor->Negate16( mat, numRows * numColumns );
//#else
        int i, s;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            mat[i] = -mat[i];
        }
//#endif
    }

    public void Clamp(float min, float max) {// clamp all values
        int i, s;
        s = numRows * numColumns;
        for (i = 0; i < s; i++) {
            if (mat[i] < min) {
                mat[i] = min;
            } else if (mat[i] > max) {
                mat[i] = max;
            }
        }
    }

    public idMatX SwapRows(int r1, int r2) {// swap rows
        float[] ptr = new float[numColumns];

//	ptr = (float *) _alloca16( numColumns * sizeof( float ) );
//	memcpy( ptr, mat + r1 * numColumns, numColumns * sizeof( float ) );
        System.arraycopy(mat, r1 * numColumns, ptr, 0, numColumns);
//	memcpy( mat + r1 * numColumns, mat + r2 * numColumns, numColumns * sizeof( float ) );
        System.arraycopy(mat, r2 * numColumns, mat, r1 * numColumns, numColumns);
//	memcpy( mat + r2 * numColumns, ptr, numColumns * sizeof( float ) );
        System.arraycopy(ptr, 0, mat, r2 * numColumns, numColumns);

        return this;
    }

    public idMatX SwapColumns(int r1, int r2) {// swap columns
        int i, ptr;
        float tmp;

        for (i = 0; i < numRows; i++) {
            ptr = i * numColumns;
            tmp = mat[ptr + r1];
            mat[ptr + r1] = mat[ptr + r2];
            mat[ptr + r2] = tmp;
        }

        return this;
    }

    public idMatX SwapRowsColumns(int r1, int r2) {// swap rows and columns
        SwapRows(r1, r2);
        SwapColumns(r1, r2);
        return this;
    }

    public idMatX RemoveRow(int r) {// remove a row
        int i;

        assert (r < numRows);

        numRows--;

//        this.SetSize(numRows, numColumns);
        for (i = r; i < numRows; i++) {//TODO:create new array to save memory?
//		memcpy( &mat[i * numColumns], &mat[( i + 1 ) * numColumns], numColumns * sizeof( float ) );
            System.arraycopy(mat, (i + 1) * numColumns, mat, i * numColumns, numColumns);
        }

        return this;
    }

    public idMatX RemoveColumn(int r) {// remove a column
        int i;

        assert (r < numColumns);

        numColumns--;

        for (i = 0; i < numRows - 1; i++) {
//		memmove( &mat[i * numColumns + r], &mat[i * ( numColumns + 1 ) + r + 1], numColumns * sizeof( float ) );
            System.arraycopy(mat, 1 + r + (1 + numColumns) * i, mat, r + numColumns * i, numColumns);
        }
//	memmove( &mat[i * numColumns + r], &mat[i * ( numColumns + 1 ) + r + 1], ( numColumns - r ) * sizeof( float ) );
        System.arraycopy(mat, 1 + r + (1 + numColumns) * i, mat, r + numColumns * i, numColumns - r);

        return this;
    }

    public idMatX RemoveRowColumn(int r) {// remove a row and column
//            int i;
//
//            assert (r < numRows && r < numColumns);
//
//            numRows--;
//            numColumns--;
//
//            if (r > 0) {
//                for (i = 0; i < r - 1; i++) {
////			memmove( &mat[i * numColumns + r], &mat[i * ( numColumns + 1 ) + r + 1], numColumns * sizeof( float ) );
//                    System.arraycopy(mat, i * (numColumns + 1) + r + 1, mat, i * numColumns + r, numColumns);
//                }
////		memmove( &mat[i * numColumns + r], &mat[i * ( numColumns + 1 ) + r + 1], ( numColumns - r ) * sizeof( float ) );
//                System.arraycopy(mat, i * (numColumns + 1) + r + 1, mat, i * numColumns + r, numColumns - r);
//            }
//
////	memcpy( &mat[r * numColumns], &mat[( r + 1 ) * ( numColumns + 1 )], r * sizeof( float ) );
//            System.arraycopy(mat, (r + 1) * (numColumns + 1), mat, r * numColumns, r);
//
//            for (i = r; i < numRows - 1; i++) {
////		memcpy( &mat[i * numColumns + r], &mat[( i + 1 ) * ( numColumns + 1 ) + r + 1], numColumns * sizeof( float ) );
//                System.arraycopy(mat, (i + 1) * (numColumns + 1) + r + 1, mat, i * numColumns + r, numColumns);
//            }
////	memcpy( &mat[i * numColumns + r], &mat[( i + 1 ) * ( numColumns + 1 ) + r + 1], ( numColumns - r ) * sizeof( float ) );
//            System.arraycopy(mat, (i + 1) * (numColumns + 1) + r + 1, mat, i * numColumns + r, numColumns - r);
        this.RemoveRow(r);
        this.RemoveColumn(r);

        return this;
    }

    // clear the upper triangle
    public void ClearUpperTriangle() {
        assert (numRows == numColumns);
        for (int i = numRows - 2; i >= 0; i--) {
//		memset( mat + i * numColumns + i + 1, 0, (numColumns - 1 - i) * sizeof(float) );
            int start = i * numColumns + i + 1;
            int end = start + (numColumns - 1 - i);
            Arrays.fill(mat, start, end, 0);
        }
    }

    public void ClearLowerTriangle() {// clear the lower triangle
        assert (numRows == numColumns);
        for (int i = 1; i < numRows; i++) {
//		memset( mat + i * numColumns, 0, i * sizeof(float) );
            int start = i * numColumns;
            int end = start + i;
            Arrays.fill(mat, start, end, 0);
        }
    }

    public void SquareSubMatrix(final idMatX m, int size) {// get square sub-matrix from 0,0 to size,size
        int i;
        assert (size <= m.numRows && size <= m.numColumns);
        SetSize(size, size);
        for (i = 0; i < size; i++) {
//		memcpy( mat + i * numColumns, m.mat + i * m.numColumns, size * sizeof( float ) );
            System.arraycopy(m.mat, i * m.numColumns, mat, i * numColumns, size);
        }
    }

    public float MaxDifference(final idMatX m) {// return maximum element difference between this and m
        int i, j;
        float diff, maxDiff;

        assert (numRows == m.numRows && numColumns == m.numColumns);

        maxDiff = -1.0f;
        for (i = 0; i < numRows; i++) {
            for (j = 0; j < numColumns; j++) {
                diff = idMath.Fabs(mat[ i * numColumns + j] - m.mat[i + j * m.numRows]);
                if (maxDiff < 0.0f || diff > maxDiff) {
                    maxDiff = diff;
                }
            }
        }
        return maxDiff;
    }

    public boolean IsSquare() {
        return (numRows == numColumns);
    }

    public boolean IsZero() {
        return this.IsZero((float) MATRIX_EPSILON);
    }

    public boolean IsZero(final float epsilon) {
        // returns true if (*this) == Zero
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if (idMath.Fabs(mat[i * numColumns + j]) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsIdentity() {
        return IsIdentity((float) MATRIX_EPSILON);
    }

    public boolean IsIdentity(final float epsilon) {
        // returns true if (*this) == Identity
        assert (numRows == numColumns);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if (idMath.Fabs(mat[i * numColumns + j]
                        - (i == j ? 1.0f : 0.0f)) > epsilon) {//TODO:i==j??
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
        // returns true if all elements are zero except for the elements on the diagonal
        assert (numRows == numColumns);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if (i != j && idMath.Fabs(oGet(i, j)) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsTriDiagonal() {
        return IsTriDiagonal((float) MATRIX_EPSILON);
    }

    public boolean IsTriDiagonal(final float epsilon) {
        // returns true if all elements are zero except for the elements on the diagonal plus or minus one column

        if (numRows != numColumns) {
            return false;
        }
        for (int i = 0; i < numRows - 2; i++) {
            for (int j = i + 2; j < numColumns; j++) {
                if (idMath.Fabs(oGet(i, j)) > epsilon) {
                    return false;
                }
                if (idMath.Fabs(oGet(j, i)) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean IsSymmetric() {
        return IsSymmetric((float) MATRIX_EPSILON);
    }

    public boolean IsSymmetric(final float epsilon) {
        // (*this)[i][j] == (*this)[j][i]
        if (numRows != numColumns) {
            return false;
        }
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if (idMath.Fabs(mat[ i * numColumns + j] - mat[ j * numColumns + i]) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * ============ idMatX::IsOrthogonal
     *
     * returns true if (*this) * this->Transpose() == Identity ============
     */
    public boolean IsOrthogonal() {
        return IsOrthogonal((float) MATRIX_EPSILON);
    }

    public boolean IsOrthogonal(final float epsilon) {
        int ptr1, ptr2;
        float sum;

        if (!IsSquare()) {
            return false;
        }

        ptr1 = 0;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                ptr2 = j;
                sum = mat[ptr1] * mat[ptr2] - (i == j ? 1 : 0);
                for (int n = 1; n < numColumns; n++) {
                    ptr2 += numColumns;
                    sum += mat[ptr1 + n] * mat[ptr2];
                }
                if (idMath.Fabs(sum) > epsilon) {
                    return false;
                }
            }
            ptr1 += numColumns;
        }
        return true;
    }
    /*
     ============
     idMatX::IsOrthonormal

     returns true if (*this) * this->Transpose() == Identity and the length of each column vector is 1
     ============
     */

    public boolean IsOrthonormal() {
        return IsOrthonormal((float) MATRIX_EPSILON);
    }

    public boolean IsOrthonormal(final float epsilon) {
        int ptr1, ptr2;
        float sum;

        if (!IsSquare()) {
            return false;
        }

        ptr1 = 0;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                ptr2 = j;
                sum = mat[ptr1] * mat[ptr2] - (i == j ? 1 : 0);
                for (int n = 1; n < numColumns; n++) {
                    ptr2 += numColumns;
                    sum += mat[ptr1 + n] * mat[ptr2];
                }
                if (idMath.Fabs(sum) > epsilon) {
                    return false;
                }
            }
            ptr1 += numColumns;

            ptr2 = i;
            sum = mat[ptr2] * mat[ptr2] - 1.0f;
            for (i = 1; i < numRows; i++) {
                ptr2 += numColumns;
                sum += mat[ptr2 + i] * mat[ptr2 + i];
            }
            if (idMath.Fabs(sum) > epsilon) {
                return false;
            }
        }
        return true;
    }

    /**
     * ============ idMatX::IsPMatrix
     *
     * returns true if the matrix is a P-matrix A square matrix is a P-matrix if
     * all its principal minors are positive. ============
     */
    public boolean IsPMatrix() {
        return IsPMatrix((float) MATRIX_EPSILON);
    }

    public boolean IsPMatrix(final float epsilon) {
        int i, j;
        float d;
        idMatX m = new idMatX();

        if (!IsSquare()) {
            return false;
        }

        if (numRows <= 0) {
            return true;
        }

        if (oGet(0, 0) <= epsilon) {
            return false;
        }

        if (numRows <= 1) {
            return true;
        }

//	m.SetData( numRows - 1, numColumns - 1, MATX_ALLOCA( ( numRows - 1 ) * ( numColumns - 1 ) ) );
        m.SetSize(numRows - 1, numColumns - 1);

        for (i = 1; i < numRows; i++) {
            for (j = 1; j < numColumns; j++) {
                m.oSet(i - 1, j - 1, oGet(i, j));
            }
        }

        if (!m.IsPMatrix(epsilon)) {
            return false;
        }

        for (i = 1; i < numRows; i++) {
            d = oGet(i, 0) / oGet(0, 0);
            for (j = 1; j < numColumns; j++) {
                m.oSet(i - 1, j - 1, oGet(i, j) - d * oGet(0, j));
            }
        }

        if (!m.IsPMatrix(epsilon)) {
            return false;
        }

        return true;
    }

    /**
     * ============ idMatX::IsZMatrix
     *
     * returns true if the matrix is a Z-matrix A square matrix M is a Z-matrix
     * if M[i][j] <= 0 for all i != j. ============
     */
    public boolean IsZMatrix() {
        return IsZMatrix((float) MATRIX_EPSILON);
    }

    public boolean IsZMatrix(final float epsilon) {
        int i, j;

        if (!IsSquare()) {
            return false;
        }

        for (i = 0; i < numRows; i++) {
            for (j = 0; j < numColumns; j++) {
                if (oGet(i, j) > epsilon && i != j) {
                    return false;
                }
            }
        }
        return true;
    }
    /*
     ============
     idMatX::IsPositiveDefinite

     returns true if the matrix is Positive Definite (PD)
     A square matrix M of order n is said to be PD if y'My > 0 for all vectors y of dimension n, y != 0.
     ============
     */

    public boolean IsPositiveDefinite() {
        return IsPositiveDefinite((float) MATRIX_EPSILON);
    }

    public boolean IsPositiveDefinite(final float epsilon) {
        int i, j, k;
        float d, s;
        idMatX m = new idMatX();

        // the matrix must be square
        if (!IsSquare()) {
            return false;
        }

        // copy matrix
//	m.SetData( numRows, numColumns, MATX_ALLOCA( numRows * numColumns ) );
//	m = *this;
        m.SetData(numRows, numColumns, m.mat);

        // add transpose
        for (i = 0; i < numRows; i++) {
            for (j = 0; j < numColumns; j++) {
                m.oPluSet(i, j, oGet(j, i));
            }
        }

        // test Positive Definiteness with Gaussian pivot steps
        for (i = 0; i < numRows; i++) {

            for (j = i; j < numColumns; j++) {
                if (oGet(j, j) <= epsilon) {
                    return false;
                }
            }

            d = 1.0f / m.oGet(i, i);
            for (j = i + 1; j < numColumns; j++) {
                s = d * m.oGet(j, i);
                m.oSet(i, j, 0.0f);
                for (k = i + 1; k < numRows; k++) {
                    m.oMinSet(j, k, s * m.oGet(i, k));
                }
            }
        }

        return true;
    }

    /**
     * ============ idMatX::IsSymmetricPositiveDefinite
     *
     * returns true if the matrix is Symmetric Positive Definite (PD)
     * ============
     */
    public boolean IsSymmetricPositiveDefinite() {
        return IsSymmetricPositiveDefinite((float) MATRIX_EPSILON);
    }

    public boolean IsSymmetricPositiveDefinite(final float epsilon) {
        idMatX m = new idMatX();

        // the matrix must be symmetric
        if (!IsSymmetric(epsilon)) {
            return false;
        }

        // copy matrix
//	m.SetData( numRows, numColumns, MATX_ALLOCA( numRows * numColumns ) );
//	m = *this;
        m.SetData(numRows, numColumns, this.mat);

        // being able to obtain Cholesky factors is both a necessary and sufficient condition for positive definiteness
        return m.Cholesky_Factor();
    }

    /**
     * ============ idMatX::IsPositiveSemiDefinite
     *
     * returns true if the matrix is Positive Semi Definite (PSD) A square
     * matrix M of order n is said to be PSD if y'My >= 0 for all vectors y of
     * dimension n, y != 0. ============
     */
    public boolean IsPositiveSemiDefinite() {
        return IsPositiveSemiDefinite((float) MATRIX_EPSILON);
    }

    public boolean IsPositiveSemiDefinite(final float epsilon) {
        int i, j, k;
        float d, s;
        idMatX m = new idMatX();

        // the matrix must be square
        if (!IsSquare()) {
            return false;
        }

        // copy original matrix
//	m.SetData( numRows, numColumns, MATX_ALLOCA( numRows * numColumns ) );
//	m = *this;
        m.SetData(numRows, numColumns, this.mat);

        // add transpose
        for (i = 0; i < numRows; i++) {
            for (j = 0; j < numColumns; j++) {
                m.oPluSet(i, j, this.oGet(j, i));
            }
        }

        // test Positive Semi Definiteness with Gaussian pivot steps
        for (i = 0; i < numRows; i++) {

            for (j = i; j < numColumns; j++) {
                if (m.oGet(j, j) < -epsilon) {
                    return false;
                }
                if (m.oGet(j, j) > epsilon) {
                    continue;
                }
                for (k = 0; k < numRows; k++) {
                    if (idMath.Fabs(m.oGet(k, j)) > epsilon) {
                        return false;
                    }
                    if (idMath.Fabs(m.oGet(j, k)) > epsilon) {
                        return false;
                    }
                }
            }

            if (m.oGet(i, i) <= epsilon) {
                continue;
            }

            d = 1.0f / m.oGet(i, i);
            for (j = i + 1; j < numColumns; j++) {
                s = d * m.oGet(j, i);
                m.oSet(j, i, 0.0f);
                for (k = i + 1; k < numRows; k++) {
                    m.oMinSet(j, k, s * m.oGet(i, k));
                }
            }
        }

        return true;
    }

    public boolean IsSymmetricPositiveSemiDefinite() {
        return IsSymmetricPositiveSemiDefinite((float) MATRIX_EPSILON);
    }

    public boolean IsSymmetricPositiveSemiDefinite(final float epsilon) {
        // the matrix must be symmetric
        if (!IsSymmetric(epsilon)) {
            return false;
        }

        return IsPositiveSemiDefinite(epsilon);
    }

    public float Trace() {// returns product of diagonal elements
        float trace = 0.0f;

        assert (numRows == numColumns);

        // sum of elements on the diagonal
        for (int i = 0; i < numRows; i++) {
            trace += mat[i * numRows + i];
        }
        return trace;
    }

    public float Determinant() {// returns determinant of matrix

        assert (numRows == numColumns);

        switch (numRows) {
            case 1:
                return mat[0];
            case 2:
//			return reinterpret_cast<const idMat2 *>(mat)->Determinant();
                return mat[0] + mat[3];
            case 3:
//			return reinterpret_cast<const idMat3 *>(mat)->Determinant();
                return mat[0] + mat[4] + mat[8];
            case 4:
//			return reinterpret_cast<const idMat4 *>(mat)->Determinant();
                return mat[0] + mat[5] + mat[10] + mat[15];
            case 5:
//			return reinterpret_cast<const idMat5 *>(mat)->Determinant();
                return mat[0] + mat[6] + mat[12] + mat[18] + mat[24];
            case 6:
//			return reinterpret_cast<const idMat6 *>(mat)->Determinant();
                return mat[0] + mat[7] + mat[14] + mat[21] + mat[28] + mat[35];
            default:
                return DeterminantGeneric();
        }
//            return 0.0f;
    }

    public idMatX Transpose() {// returns transpose
        idMatX transpose = new idMatX();
        int i, j;

        transpose.SetTempSize(numColumns, numRows);

        for (i = 0; i < numRows; i++) {
            for (j = 0; j < numColumns; j++) {
                transpose.mat[j * transpose.numColumns + i] = mat[i * numColumns + j];
            }
        }

        return transpose;
    }

    // transposes the matrix itself
    public idMatX TransposeSelf() {
        this.oSet(Transpose());
        return this;
    }

    public idMatX Inverse() {// returns the inverse ( m * m.Inverse() = identity )
        idMatX invMat = new idMatX();

//	invMat.SetTempSize( numRows, numColumns );
//	memcpy( invMat.mat, mat, numRows * numColumns * sizeof( float ) );
        invMat.SetData(numRows, numColumns, mat);
        boolean r = invMat.InverseSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseSelf() {// returns false if determinant is zero
        assert (numRows == numColumns);
        boolean result;
        switch (numRows) {
            case 1:
                if (idMath.Fabs(mat[0]) < MATRIX_INVERSE_EPSILON) {
                    return false;
                }
                mat[0] = 1.0f / mat[0];
                return true;
            case 2:
                idMat2 mat2 = new idMat2(
                        mat[0], mat[1],
                        mat[2], mat[3]);
                result = mat2.InverseSelf();
                this.mat = mat2.reinterpret_cast();
                return result;
            case 3:
                idMat3 mat3 = new idMat3(
                        mat[0], mat[1], mat[2],
                        mat[3], mat[4], mat[5],
                        mat[6], mat[7], mat[8]);
                result = mat3.InverseSelf();
                this.mat = mat3.reinterpret_cast();
                return result;
            case 4:
                idMat4 mat4 = new idMat4(
                        mat[0], mat[1], mat[2], mat[3],
                        mat[0], mat[1], mat[2], mat[3],
                        mat[0], mat[1], mat[2], mat[3],
                        mat[0], mat[1], mat[2], mat[3]);
                result = mat4.InverseSelf();
                this.mat = mat4.reinterpret_cast();
                return result;
            case 5:
                idMat5 mat5 = new idMat5(
                        new idVec5(mat[0], mat[1], mat[2], mat[3], mat[4]),
                        new idVec5(mat[5], mat[6], mat[2], mat[3], mat[4]),
                        new idVec5(mat[10], mat[11], mat[12], mat[13], mat[14]),
                        new idVec5(mat[15], mat[16], mat[17], mat[18], mat[19]),
                        new idVec5(mat[20], mat[21], mat[22], mat[23], mat[24]));
                result = mat5.InverseSelf();
                this.mat = mat5.reinterpret_cast();
                return result;
            case 6:
                idMat6 mat6 = new idMat6(
                        new idVec6(mat[0], mat[1], mat[2], mat[3], mat[4], mat[5]),
                        new idVec6(mat[6], mat[7], mat[8], mat[9], mat[10], mat[11]),
                        new idVec6(mat[12], mat[13], mat[14], mat[15], mat[16], mat[17]),
                        new idVec6(mat[18], mat[19], mat[20], mat[21], mat[22], mat[23]),
                        new idVec6(mat[24], mat[25], mat[26], mat[27], mat[28], mat[29]),
                        new idVec6(mat[30], mat[31], mat[32], mat[33], mat[34], mat[35]));
                result = mat6.InverseSelf();
                this.mat = mat6.reinterpret_cast();
                return result;
            default:
                return InverseSelfGeneric();
        }
    }

    public idMatX InverseFast() {// returns the inverse ( m * m.Inverse() = identity )
        idMatX invMat = new idMatX();

	    invMat.SetTempSize( numRows, numColumns );
        System.arraycopy(mat, 0, invMat.mat, 0, numRows * numColumns);
        boolean r = invMat.InverseFastSelf();
        assert (r);
        return invMat;
    }

    public boolean InverseFastSelf() {// returns false if determinant is zero
        assert (numRows == numColumns);

        boolean result;
        switch (numRows) {
            case 1:
                if (idMath.Fabs(mat[0]) < MATRIX_INVERSE_EPSILON) {
                    return false;
                }
                mat[0] = 1.0f / mat[0];
                return true;
            case 2:
                idMat2 mat2 = new idMat2(
                        mat[0], mat[1],
                        mat[2], mat[3]);
                result = mat2.InverseFastSelf();
                this.mat = mat2.reinterpret_cast();
                return result;
            case 3:
                idMat3 mat3 = new idMat3(
                        mat[0], mat[1], mat[2],
                        mat[3], mat[4], mat[5],
                        mat[6], mat[7], mat[8]);
                result = mat3.InverseFastSelf();
                this.mat = mat3.reinterpret_cast();
                return result;
            case 4:
                idMat4 mat4 = new idMat4(
                        mat[ 0], mat[ 1], mat[ 2], mat[ 3],
                        mat[ 4], mat[ 5], mat[ 6], mat[ 7],
                        mat[ 8], mat[ 9], mat[10], mat[11],
                        mat[12], mat[13], mat[14], mat[15]);
                result = mat4.InverseFastSelf();
                this.mat = mat4.reinterpret_cast();
                return result;
            case 5:
                idMat5 mat5 = new idMat5(
                        new idVec5(mat[ 0], mat[ 1], mat[ 2], mat[ 3], mat[ 4]),
                        new idVec5(mat[ 5], mat[ 6], mat[ 7], mat[ 8], mat[ 9]),
                        new idVec5(mat[10], mat[11], mat[12], mat[13], mat[14]),
                        new idVec5(mat[15], mat[16], mat[17], mat[18], mat[19]),
                        new idVec5(mat[20], mat[21], mat[22], mat[23], mat[24]));
                result = mat5.InverseFastSelf();
                this.mat = mat5.reinterpret_cast();
                return result;
            case 6:
                idMat6 mat6 = new idMat6(
                        new idVec6(mat[ 0], mat[ 1], mat[ 2], mat[ 3], mat[ 4], mat[ 5]),
                        new idVec6(mat[ 6], mat[ 7], mat[ 8], mat[ 9], mat[10], mat[11]),
                        new idVec6(mat[12], mat[13], mat[14], mat[15], mat[16], mat[17]),
                        new idVec6(mat[18], mat[19], mat[20], mat[21], mat[22], mat[23]),
                        new idVec6(mat[24], mat[25], mat[26], mat[27], mat[28], mat[29]),
                        new idVec6(mat[30], mat[31], mat[32], mat[33], mat[34], mat[35]));
                result = mat6.InverseFastSelf();//TODO: merge fast and slow
                this.mat = mat6.reinterpret_cast();
                return result;
            default:
                return InverseSelfGeneric();
        }
//            return false;
    }

    /**
     * ============ idMatX::LowerTriangularInverse
     *
     * in-place inversion of the lower triangular matrix ============
     */
    public boolean LowerTriangularInverse() {// in-place inversion, returns false if determinant is zero
        int i, j, k;
        float d, sum;

        for (i = 0; i < numRows; i++) {
            d = this.oGet(i, i);
//                System.out.println("1:" + d);
            if (d == 0.0f) {
                return false;
            }
            this.oSet(i, i, d = 1.0f / d);
//                System.out.println("2:" + d);

            for (j = 0; j < i; j++) {
                sum = 0.0f;
                for (k = j; k < i; k++) {
                    sum -= this.oGet(i, k) * this.oGet(k, j);
                }
                this.oSet(i, j, sum * d);
//                    System.out.println("3:" + sum * d);
            }
        }
        return true;
    }

    /**
     * ============ idMatX::UpperTriangularInverse
     *
     * in-place inversion of the upper triangular matrix ============
     */
    public boolean UpperTriangularInverse() {// in-place inversion, returns false if determinant is zero
        int i, j, k;
        double d, sum;

        for (i = numRows - 1; i >= 0; i--) {
            d = this.oGet(i, i);
            if (d == 0.0f) {
                return false;
            }
            this.oSet(i, i, (float) (d = 1.0f / d));

            for (j = numRows - 1; j > i; j--) {
                sum = 0.0f;
                for (k = j; k > i; k--) {
                    sum -= this.oGet(i, k) * this.oGet(k, j);
                }
                this.oSet(i, j, (float) (sum * d));
            }
        }
        return true;
    }

    public idVecX Multiply(final idVecX vec) {// (*this) * vec
        idVecX dst = new idVecX();

        assert (numColumns == vec.GetSize());

        dst.SetTempSize(numRows);
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_MultiplyVecX( dst, *this, vec );
//#else
        Multiply(dst, vec);
//#endif
        return dst;
    }

    public idVecX TransposeMultiply(final idVecX vec) {// this->Transpose() * vec
        idVecX dst = new idVecX();

        assert (numRows == vec.GetSize());

        dst.SetTempSize(numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_TransposeMultiplyVecX( dst, *this, vec );
//#else
        TransposeMultiply(dst, vec);
//#endif
        return dst;
    }

    public idMatX Multiply(final idMatX a) {// (*this) * a
        idMatX dst = new idMatX();

        assert (numColumns == a.numRows);

        dst.SetTempSize(numRows, a.numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_MultiplyMatX( dst, *this, a );
//#else
        Multiply(dst, a);
//#endif
        return dst;
    }

    public idMatX TransposeMultiply(final idMatX a) {// this->Transpose() * a
        idMatX dst = new idMatX();

        assert (numRows == a.numRows);

        dst.SetTempSize(numColumns, a.numColumns);
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_TransposeMultiplyMatX( dst, *this, a );
//#else
        TransposeMultiply(dst, a);
//#endif
        return dst;
    }

    public void Multiply(idVecX dst, final idVecX vec) {// dst = (*this) * vec
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_MultiplyVecX( dst, *this, vec );
//#else
        int i, j, m = 0;
        final float[] mPtr, vPtr, dstPtr;

        mPtr = mat;
        vPtr = vec.ToFloatPtr();
        dstPtr = dst.ToFloatPtr();
        for (i = 0; i < numRows; i++) {
            float sum = mPtr[m + 0] * vPtr[0];
            for (j = 1; j < numColumns; j++) {
                sum += mPtr[m + j] * vPtr[j];
            }
            dstPtr[i] = sum;
            m += numColumns;
        }
//#endif
    }

    public void MultiplyAdd(idVecX dst, final idVecX vec) {// dst += (*this) * vec
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_MultiplyAddVecX( dst, *this, vec );
//#else
        int i, j, m = 0;
        final float[] mPtr, vPtr, dstPtr;

        mPtr = mat;
        vPtr = vec.ToFloatPtr();
        dstPtr = dst.ToFloatPtr();
        for (i = 0; i < numRows; i++) {
            float sum = mPtr[0 + m] * vPtr[0];
            for (j = 1; j < numColumns; j++) {
                sum += mPtr[j + m] * vPtr[j];
            }
            dstPtr[i] += sum;
            m += numColumns;
        }
//#endif
    }

    public void MultiplySub(idVecX dst, final idVecX vec) {// dst -= (*this) * vec
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_MultiplySubVecX( dst, *this, vec );
//#else
        int i, j, m = 0;
        final float[] mPtr, vPtr, dstPtr;

        mPtr = mat;
        vPtr = vec.ToFloatPtr();
        dstPtr = dst.ToFloatPtr();
        for (i = 0; i < numRows; i++) {
            float sum = mPtr[0 + m] * vPtr[0];
            for (j = 1; j < numColumns; j++) {
                sum += mPtr[j + m] * vPtr[j];
            }
            dstPtr[i] -= sum;
            m += numColumns;
        }
//#endif
    }

    public void TransposeMultiply(idVecX dst, final idVecX vec) {// dst = this->Transpose() * vec
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_TransposeMultiplyVecX( dst, *this, vec );
//#else
        int i, j, mPtr;
        final float[] vPtr, dstPtr;

        vPtr = vec.ToFloatPtr();
        dstPtr = dst.ToFloatPtr();
        for (i = 0; i < numColumns; i++) {
            mPtr = i;
            float sum = mat[mPtr] * vPtr[0];
            for (j = 1; j < numRows; j++) {
                mPtr += numColumns;
                sum += mat[mPtr] * vPtr[j];
            }
            dstPtr[i] = sum;
        }
//#endif
    }

    public void TransposeMultiplyAdd(idVecX dst, final idVecX vec) {// dst += this->Transpose() * vec
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_TransposeMultiplyAddVecX( dst, *this, vec );
//#else
        int i, j, mPtr;
        final float[] vPtr, dstPtr;

        vPtr = vec.ToFloatPtr();
        dstPtr = dst.ToFloatPtr();
        for (i = 0; i < numColumns; i++) {
            mPtr = i;
            float sum = mat[mPtr] * vPtr[0];
            for (j = 1; j < numRows; j++) {
                mPtr += numColumns;
                sum += mat[mPtr] * vPtr[j];
            }
            dstPtr[i] += sum;
        }
//#endif
    }

    public void TransposeMultiplySub(idVecX dst, final idVecX vec) {// dst -= this->Transpose() * vec
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_TransposeMultiplySubVecX( dst, *this, vec );
//#else
        int i, j, mPtr;
        final float[] vPtr, dstPtr;

        vPtr = vec.ToFloatPtr();
        dstPtr = dst.ToFloatPtr();
        for (i = 0; i < numColumns; i++) {
            mPtr = i;
            float sum = mat[mPtr] * vPtr[0];
            for (j = 1; j < numRows; j++) {
                mPtr += numColumns;
                sum += mat[mPtr] * vPtr[j];
            }
            dstPtr[i] -= sum;
        }
//#endif
    }

    public void Multiply(idMatX dst, final idMatX a) {// dst = (*this) * a
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_MultiplyMatX( dst, *this, a );
//#else
        int i, j, k, l, n;
        float[] dstPtr;
        final float[] m1Ptr, m2Ptr;
        double sum;//double, the difference between life and death.
        int m1 = 0, m2 = 0, d0 = 0;//indices

        assert (numColumns == a.numRows);

        dstPtr = dst.ToFloatPtr();
        m1Ptr = ToFloatPtr();
        m2Ptr = a.ToFloatPtr();
        k = numRows;
        l = a.GetNumColumns();

        for (i = 0; i < k; i++) {
            for (j = 0; j < l; j++) {
                m2 = j;
                sum = m1Ptr[0 + m1] * m2Ptr[0 + m2];
                for (n = 1; n < numColumns; n++) {
                    m2 += l;
                    sum += m1Ptr[n + m1] * m2Ptr[0 + m2];
//                    System.out.printf("%f %f\n", m1Ptr[n + m1], m2Ptr[0 + m2]);
                }
                dstPtr[d0++] = (float) sum;
//                System.out.printf("%f\n", sum);
            }
            m1 += numColumns;
        }
//#endif
    }

    public void TransposeMultiply(idMatX dst, final idMatX a) {// dst = this->Transpose() * a
//#ifdef MATX_SIMD
//	SIMDProcessor->MatX_TransposeMultiplyMatX( dst, *this, a );
//#else
        int i, j, k, l, n;
        float[] dstPtr;
        final float[] m1Ptr, m2Ptr;
        double sum;
        int m1 = 0, m2 = 0, d0 = 0;//indices

        assert (numRows == a.numRows);//TODO:check if these pseudo indices work like the pointers

        dstPtr = dst.ToFloatPtr();
        m1Ptr = ToFloatPtr();
        m2Ptr = a.ToFloatPtr();
        k = numColumns;
        l = a.numColumns;

        for (i = 0; i < k; i++) {
            for (j = 0; j < l; j++) {
                m1 = i;
                m2 = j;
                sum = m1Ptr[0 + m1] * m2Ptr[0 + m2];
                for (n = 1; n < numRows; n++) {
                    m1 += numColumns;
                    m2 += a.numColumns;
                    sum += m1Ptr[0 + m1] * m2Ptr[0 + m2];
                }
                dstPtr[d0++] = (float) sum;
            }
        }
//#endif
    }

    public int GetDimension() {// returns total number of values in matrix
        return numRows * numColumns;
    }

    /** @deprecated returns readonly vector */
    @Deprecated
    public idVec6 SubVec6(int row) {// interpret beginning of row as a const idVec6
        assert (numColumns >= 6 && row >= 0 && row < numRows);
//	return *reinterpret_cast<const idVec6 *>(mat + row * numColumns);
        float[] temp = new float[6];
        System.arraycopy(mat, (row * numColumns), temp, 0, 6);
        return new idVec6(temp);
    }
//public	idVec6 &		SubVec6( int row );												// interpret beginning of row as an idVec6

    public idVecX SubVecX(int row) {// interpret complete row as a const idVecX
        idVecX v = new idVecX();
        assert (row >= 0 && row < numRows);
        float[] temp = new float[numColumns];
        System.arraycopy(mat, (row * numColumns), temp, 0, numColumns);
        v.SetData(numColumns, temp);
        return v;
    }
//public	idVecX			SubVecX( int row );												// interpret complete row as an idVecX

    public float[] ToFloatPtr() {// pointer to const matrix float array
        return mat;
    }

    public void FromFloatPtr(float[] mat) {
        this.mat = mat;
    }

//public	float *			ToFloatPtr( void );												// pointer to matrix float array
    public String ToString(int precision) {
        return idStr.FloatArrayToString(ToFloatPtr(), GetDimension(), precision);
    }

    /**
     * ============ idMatX::Update_RankOne
     *
     * Updates the matrix to obtain the matrix: A + alpha * v * w' ============
     */
    public void Update_RankOne(final idVecX v, final idVecX w, float alpha) {
        int i, j;
        float s;

        assert (v.GetSize() >= numRows);
        assert (w.GetSize() >= numColumns);

        for (i = 0; i < numRows; i++) {
            s = alpha * v.p[i];
            for (j = 0; j < numColumns; j++) {
                this.oPluSet(i, j, s * w.p[j]);
            }
        }
    }

    /*
     ============
     idMatX::Update_RankOneSymmetric

     Updates the matrix to obtain the matrix: A + alpha * v * v'
     ============
     */
    public void Update_RankOneSymmetric(final idVecX v, float alpha) {
        int i, j;
        float s;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);

        for (i = 0; i < numRows; i++) {
            s = alpha * v.p[i];
            for (j = 0; j < numColumns; j++) {
                this.oPluSet(i, j, s * v.p[j]);
            }
        }
    }

    /**
     * ============ idMatX::Update_RowColumn
     *
     * Updates the matrix to obtain the matrix:
     *
     * [ 0 a 0 ]
     * A + [ d b e ]
     * [ 0 c 0 ]
     *
     * where: a = v[0,r-1], b = v[r], c = v[r+1,numRows-1], d = w[0,r-1], w[r] =
     * 0.0f, e = w[r+1,numColumns-1] ============
     */
    public void Update_RowColumn(final idVecX v, final idVecX w, int r) {
        int i;

        assert (w.p[r] == 0.0f);
        assert (v.GetSize() >= numColumns);
        assert (w.GetSize() >= numRows);

        for (i = 0; i < numRows; i++) {
            this.oPluSet(i, r, v.p[i]);
        }
        for (i = 0; i < numColumns; i++) {
            this.oPluSet(r, i, w.p[i]);
        }
    }

    /**
     * ============ idMatX::Update_RowColumnSymmetric
     *
     * Updates the matrix to obtain the matrix:
     *
     * [ 0 a 0 ]
     * A + [ a b c ]
     * [ 0 c 0 ]
     *
     * where: a = v[0,r-1], b = v[r], c = v[r+1,numRows-1] ============
     */
    public void Update_RowColumnSymmetric(final idVecX v, int r) {
        int i;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);

        for (i = 0; i < r; i++) {
            this.oPluSet(i, r, v.p[i]);
            this.oPluSet(r, i, v.p[i]);
        }
        this.oSet(r, r, this.oGet(r, r) + v.p[r]);
        for (i = r + 1; i < numRows; i++) {
            this.oPluSet(i, r, v.p[i]);
            this.oPluSet(r, i, v.p[i]);
        }
    }

    /**
     * ============ idMatX::Update_Increment
     *
     * Updates the matrix to obtain the matrix:
     *
     * [ A a ]
     * [ c b ]
     *
     * where: a = v[0,numRows-1], b = v[numRows], c = w[0,numColumns-1]],
     * w[numColumns] = 0 ============
     */
    public void Update_Increment(final idVecX v, final idVecX w) {
        int i;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows + 1);
        assert (w.GetSize() >= numColumns + 1);

        ChangeSize(numRows + 1, numColumns + 1, false);

        for (i = 0; i < numRows; i++) {
            this.oSet(i, numColumns - 1, v.p[i]);
        }
        for (i = 0; i < numColumns - 1; i++) {
            this.oSet(numRows - 1, i, w.p[i]);
        }
    }

    /**
     * ============ idMatX::Update_IncrementSymmetric
     *
     * Updates the matrix to obtain the matrix:
     *
     * [ A a ]
     * [ a b ]
     *
     * where: a = v[0,numRows-1], b = v[numRows] ============
     */
    public void Update_IncrementSymmetric(final idVecX v) {
        int i;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows + 1);

        ChangeSize(numRows + 1, numColumns + 1, false);

        for (i = 0; i < numRows - 1; i++) {
            this.oSet(i, numColumns - 1, v.p[i]);
        }
        for (i = 0; i < numColumns; i++) {
            this.oSet(numRows - 1, i, v.p[i]);
        }
    }

    /*
     ============
     idMatX::Update_Decrement

     Updates the matrix to obtain a matrix with row r and column r removed.
     ============
     */
    public void Update_Decrement(int r) {
        RemoveRowColumn(r);
    }

    /*
     ============
     idMatX::Inverse_GaussJordan

     in-place inversion using Gauss-Jordan elimination
     ============
     */
    public boolean Inverse_GaussJordan() {// invert in-place with Gauss-Jordan elimination
        int i, j, k, r, c;
        float d, max;

        assert (numRows == numColumns);

        int[] columnIndex = new int[numRows];
        int[] rowIndex = new int[numRows];
        boolean[] pivot = new boolean[numRows];//memset( pivot, 0, numRows * sizeof( bool ) );

        // elimination with full pivoting
        for (i = 0; i < numRows; i++) {

            // search the whole matrix except for pivoted rows for the maximum absolute value
            max = 0.0f;
            r = c = 0;
            for (j = 0; j < numRows; j++) {
                if (!pivot[j]) {
                    for (k = 0; k < numRows; k++) {
                        if (!pivot[k]) {
                            d = idMath.Fabs(this.oGet(j, k));
                            if (d > max) {
                                max = d;
                                r = j;
                                c = k;
                            }
                        }
                    }
                }
            }

            if (max == 0.0f) {
                // matrix is not invertible
                return false;
            }

            pivot[c] = true;

            // swap rows such that entry (c,c) has the pivot entry
            if (r != c) {
                SwapRows(r, c);
            }

            // keep track of the row permutation
            rowIndex[i] = r;
            columnIndex[i] = c;

            // scale the row to make the pivot entry equal to 1
            d = 1.0f / this.oGet(c, c);
            this.oSet(c, c, 1.0f);
            for (k = 0; k < numRows; k++) {
                this.oMulSet(c, k, d);
            }

            // zero out the pivot column entries in the other rows
            for (j = 0; j < numRows; j++) {
                if (j != c) {
                    d = this.oGet(j, c);
                    this.oSet(j, c, 0.0f);
                    for (k = 0; k < numRows; k++) {
                        this.oMinSet(j, k, this.oGet(c, k) * d);
                    }
                }
            }
        }

        // reorder rows to store the inverse of the original matrix
        for (j = numRows - 1; j >= 0; j--) {
            if (rowIndex[j] != columnIndex[j]) {
                for (k = 0; k < numRows; k++) {
                    d = this.oGet(k, rowIndex[j]);
                    this.oSet(k, rowIndex[j], this.oGet(k, columnIndex[j]));
                    this.oSet(k, columnIndex[j], d);
                }
            }
        }

        return true;
    }
    /*
     ============
     idMatX::Inverse_UpdateRankOne

     Updates the in-place inverse using the Sherman-Morrison formula to obtain the inverse for the matrix: A + alpha * v * w'
     ============
     */

    public boolean Inverse_UpdateRankOne(final idVecX v, final idVecX w, float alpha) {
        int i, j;
        float beta, s;
        idVecX y = new idVecX(), z = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numColumns);
        assert (w.GetSize() >= numRows);

        y.SetData(numRows, VECX_ALLOCA(numRows));
        z.SetData(numRows, VECX_ALLOCA(numRows));

        Multiply(y, v);
        TransposeMultiply(z, w);
        beta = 1.0f + (w.oMultiply(y));

        if (beta == 0.0f) {
            return false;
        }

        alpha /= beta;

        for (i = 0; i < numRows; i++) {
            s = y.p[i] * alpha;
            for (j = 0; j < numColumns; j++) {
                this.oMinSet(i, j, s * z.p[j]);
            }
        }
        return true;
    }

    /**
     * ============ idMatX::Inverse_UpdateRowColumn
     *
     * Updates the in-place inverse to obtain the inverse for the matrix:
     *
     * [ 0 a 0 ]
     * A + [ d b e ]
     * [ 0 c 0 ]
     *
     * where: a = v[0,r-1], b = v[r], c = v[r+1,numRows-1], d = w[0,r-1], w[r] =
     * 0.0f, e = w[r+1,numColumns-1] ============
     */
    public boolean Inverse_UpdateRowColumn(final idVecX v, final idVecX w, int r) {
        idVecX s = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numColumns);
        assert (w.GetSize() >= numRows);
        assert (r >= 0 && r < numRows && r < numColumns);
        assert (w.p[r] == 0.0f);

        s.SetData(Lib.Max(numRows, numColumns), VECX_ALLOCA(Lib.Max(numRows, numColumns)));
        s.Zero();
        s.p[r] = 1.0f;

        if (!Inverse_UpdateRankOne(v, s, 1.0f)) {
            return false;
        }
        if (!Inverse_UpdateRankOne(s, w, 1.0f)) {
            return false;
        }
        return true;
    }

    /**
     * ============ idMatX::Inverse_UpdateIncrement
     *
     * Updates the in-place inverse to obtain the inverse for the matrix:
     *
     * [ A a ]
     * [ c b ]
     *
     * where: a = v[0,numRows-1], b = v[numRows], c = w[0,numColumns-1],
     * w[numColumns] = 0 ============
     */
    public boolean Inverse_UpdateIncrement(final idVecX v, final idVecX w) {
        idVecX v2 = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows + 1);
        assert (w.GetSize() >= numColumns + 1);

        ChangeSize(numRows + 1, numColumns + 1, true);
        this.oSet(numRows - 1, numRows - 1, 1.0f);

        v2.SetData(numRows, VECX_ALLOCA(numRows));
        v2 = v;
        v2.p[numRows - 1] -= 1.0f;

        return Inverse_UpdateRowColumn(v2, w, numRows - 1);
    }

    /**
     * ============ idMatX::Inverse_UpdateDecrement
     *
     * Updates the in-place inverse to obtain the inverse of the matrix with row
     * r and column r removed. v and w should store the column and row of the
     * original matrix respectively. ============
     */
    public boolean Inverse_UpdateDecrement(final idVecX v, final idVecX w, int r) {
        idVecX v1 = new idVecX(), w1 = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);
        assert (w.GetSize() >= numColumns);
        assert (r >= 0 && r < numRows && r < numColumns);

        v1.SetData(numRows, VECX_ALLOCA(numRows));
        w1.SetData(numRows, VECX_ALLOCA(numRows));

        // update the row and column to identity
        v1.oSet(v.oNegative());
        w1.oSet(w.oNegative());
        v1.p[r] += 1.0f;
        w1.p[r] = 0.0f;

        if (!Inverse_UpdateRowColumn(v1, w1, r)) {
            return false;
        }

        // physically remove the row and column
        Update_Decrement(r);

        return true;
    }

    /**
     * ============ idMatX::Inverse_Solve
     *
     * Solve Ax = b with A inverted ============
     */
    public void Inverse_Solve(idVecX x, final idVecX b) {
        Multiply(x, b);
    }

    /*
     ============
     idMatX::LU_Factor

     in-place factorization: LU
     L is a triangular matrix stored in the lower triangle.
     L has ones on the diagonal that are not stored.
     U is a triangular matrix stored in the upper triangle.
     If index != NULL partial pivoting is used for numerical stability.
     If index != NULL it must point to an array of numRow integers and is used to keep track of the row permutation.
     If det != NULL the determinant of the matrix is calculated and stored.
     ============
     */
    public boolean LU_Factor(int[] index) {
        return LU_Factor(index, null);
    }

    public boolean LU_Factor(int[] index, float[] det) {
        int i, j, k, newi, min;
        double s, t, d, w;

        // if partial pivoting should be used
        if (index != null) {
            for (i = 0; i < numRows; i++) {
                index[i] = i;
            }
        }

        w = 1.0f;
        min = Lib.Min(numRows, numColumns);
        for (i = 0; i < min; i++) {

            newi = i;
            s = idMath.Fabs(this.oGet(i, i));

            if (index != null) {
                // find the largest absolute pivot
                for (j = i + 1; j < numRows; j++) {
                    t = idMath.Fabs(this.oGet(j, i));
//                    System.out.println(t);
                    if (t > s) {
                        newi = j;
                        s = t;
                    }
                }
            }

            if (s == 0.0f) {
                return false;
            }

            if (newi != i) {

                w = -w;

                // swap index elements
                k = index[i];
                index[i] = index[newi];
                index[newi] = k;

                // swap rows
                for (j = 0; j < numColumns; j++) {
                    t = this.oGet(newi, j);
                    this.oSet(newi, j, this.oGet(i, j));
                    this.oSet(i, j, (float) t);
                }
            }

            if (i < numRows) {
                d = 1.0f / this.oGet(i, i);
                for (j = i + 1; j < numRows; j++) {
                    this.oMulSet(j, i, d);
                }
            }

            if (i < min - 1) {
                for (j = i + 1; j < numRows; j++) {
                    d = this.oGet(j, i);
                    for (k = i + 1; k < numColumns; k++) {
                        this.oMinSet(j, k, d * this.oGet(i, k));
                    }
                }
            }
        }

        if (det != null) {
            for (i = 0; i < numRows; i++) {
                w *= this.oGet(i, i);
            }
            det[0] = (float) w;//TODO:check back ref
        }

        return true;
    }

    /*
     ============
     idMatX::LU_UpdateRankOne

     Updates the in-place LU factorization to obtain the factors for the matrix: LU + alpha * v * w'
     ============
     */
    public boolean LU_UpdateRankOne(final idVecX v, final idVecX w, float alpha, int[] index) {
        int i, j, max;
        float[] y, z;
        double diag, beta, p0, p1, d;

        assert (v.GetSize() >= numColumns);
        assert (w.GetSize() >= numRows);

//	y = (float *) _alloca16( v.GetSize() * sizeof( float ) );
//	z = (float *) _alloca16( w.GetSize() * sizeof( float ) );
        y = new float[v.GetSize()];
        z = new float[w.GetSize()];

        if (index != null) {
            for (i = 0; i < numRows; i++) {
                y[i] = alpha * v.p[index[i]];
            }
        } else {
            for (i = 0; i < numRows; i++) {
                y[i] = alpha * v.p[i];
            }
        }

//	memcpy( z, w.ToFloatPtr(), w.GetSize() * sizeof( float ) );
        System.arraycopy(w.ToFloatPtr(), 0, z, 0, w.GetSize());

        max = Lib.Min(numRows, numColumns);
        for (i = 0; i < max; i++) {
            diag = this.oGet(i, i);

            p0 = y[i];
            p1 = z[i];
            diag += p0 * p1;

            if (diag == 0.0f) {
                return false;
            }

            beta = p1 / diag;

            this.oSet(i, i, (float) diag);

            for (j = i + 1; j < numColumns; j++) {

                d = this.oGet(i, j);

                d += p0 * z[j];
                z[j] -= beta * d;

                this.oSet(i, j, (float) d);
            }

            for (j = i + 1; j < numRows; j++) {

                d = this.oGet(j, i);

                y[j] -= p0 * d;
                d += beta * y[j];

                this.oSet(j, i, (float) d);
            }
        }
        return true;
    }

    /*
     ============
     idMatX::LU_UpdateRowColumn

     Updates the in-place LU factorization to obtain the factors for the matrix:

     [ 0  a  0 ]
     LU + [ d  b  e ]
     [ 0  c  0 ]

     where: a = v[0,r-1], b = v[r], c = v[r+1,numRows-1], d = w[0,r-1], w[r] = 0.0f, e = w[r+1,numColumns-1]
     ============
     */
    public boolean LU_UpdateRowColumn(final idVecX v, final idVecX w, int r, int[] index) {
//    #else
        int i, j, min, max, rp;
        float[] y0, y1, z0, z1;
        double diag, beta0, beta1, p0, p1, q0, q1, d;

        assert (v.GetSize() >= numColumns);
        assert (w.GetSize() >= numRows);
        assert (r >= 0 && r < numColumns && r < numRows);
        assert (w.p[r] == 0.0f);

        y0 = new float[v.GetSize()];
        z0 = new float[w.GetSize()];
        y1 = new float[v.GetSize()];
        z1 = new float[w.GetSize()];

        if (index != null) {
            for (i = 0; i < numRows; i++) {
                y0[i] = v.p[index[i]];
            }
            rp = r;
            for (i = 0; i < numRows; i++) {
                if (index[i] == r) {
                    rp = i;
                    break;
                }
            }
        } else {
            System.arraycopy(v.ToFloatPtr(), 0, y0, 0, v.GetSize());
            rp = r;
        }

//	memset( y1, 0, v.GetSize() * sizeof( float ) );
        y1[rp] = 1.0f;

//	memset( z0, 0, w.GetSize() * sizeof( float ) );
        z0[r] = 1.0f;

//	memcpy( z1, w.ToFloatPtr(), w.GetSize() * sizeof( float ) );
        System.arraycopy(w.ToFloatPtr(), 0, z1, 0, w.GetSize());

        // update the beginning of the to be updated row and column
        min = Lib.Min(r, rp);
        for (i = 0; i < min; i++) {
            p0 = y0[i];
            beta1 = z1[i] / this.oGet(i, i);

            this.oPluSet(i, r, p0);
            for (j = i + 1; j < numColumns; j++) {
                z1[j] -= beta1 * this.oGet(i, j);
            }
            for (j = i + 1; j < numRows; j++) {
                y0[j] -= p0 * this.oGet(j, i);
            }
            this.oPluSet(rp, i, beta1);
        }

        // update the lower right corner starting at r,r
        max = Lib.Min(numRows, numColumns);
        for (i = min; i < max; i++) {
            diag = this.oGet(i, i);

            p0 = y0[i];
            p1 = z0[i];
            diag += p0 * p1;

            if (diag == 0.0f) {
                return false;
            }

            beta0 = p1 / diag;

            q0 = y1[i];
            q1 = z1[i];
            diag += q0 * q1;

            if (diag == 0.0f) {
                return false;
            }

            beta1 = q1 / diag;

            this.oSet(i, i, (float) diag);

            for (j = i + 1; j < numColumns; j++) {

                d = this.oGet(i, j);

                d += p0 * z0[j];
                z0[j] -= beta0 * d;

                d += q0 * z1[j];
                z1[j] -= beta1 * d;

                this.oSet(i, j, (float) d);
            }

            for (j = i + 1; j < numRows; j++) {

                d = this.oGet(j, i);

                y0[j] -= p0 * d;
                d += beta0 * y0[j];

                y1[j] -= q0 * d;
                d += beta1 * y1[j];

                this.oSet(j, i, (float) d);
            }
        }
        return true;
//#endif
    }

    /*
     ============
     idMatX::LU_UpdateIncrement

     Updates the in-place LU factorization to obtain the factors for the matrix:

     [ A  a ]
     [ c  b ]

     where: a = v[0,numRows-1], b = v[numRows], c = w[0,numColumns-1], w[numColumns] = 0
     ============
     */
    public boolean LU_UpdateIncrement(final idVecX v, final idVecX w, int[] index) {
        int i, j;
        float sum;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows + 1);
        assert (w.GetSize() >= numColumns + 1);

        ChangeSize(numRows + 1, numColumns + 1, true);

        // add row to L
        for (i = 0; i < numRows - 1; i++) {
            sum = w.p[i];
            for (j = 0; j < i; j++) {
                sum -= this.oGet(numRows - 1, j) * this.oGet(j, i);
            }
            this.oSet(numRows - 1, i, sum / this.oGet(i, i));
        }

        // add row to the permutation index
        if (index != null) {
            index[numRows - 1] = numRows - 1;//TODO:check back reference, non final array
        }

        // add column to U
        for (i = 0; i < numRows; i++) {
            if (index != null) {
                sum = v.p[index[i]];
            } else {
                sum = v.p[i];
            }
            for (j = 0; j < i; j++) {
                sum -= this.oGet(i, j) * this.oGet(j, numRows - 1);
            }
            this.oSet(i, numRows - 1, sum);
        }

        return true;
    }

    /*
     ============
     idMatX::LU_UpdateDecrement

     Updates the in-place LU factorization to obtain the factors for the matrix with row r and column r removed.
     v and w should store the column and row of the original matrix respectively.
     If index != NULL then u should store row index[r] of the original matrix. If index == NULL then u = w.
     ============
     */
    public boolean LU_UpdateDecrement(final idVecX v, final idVecX w, final idVecX u, int r, int[] index) {
        int i, p;
        idVecX v1 = new idVecX(), w1 = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numColumns);
        assert (w.GetSize() >= numRows);
        assert (r >= 0 && r < numRows && r < numColumns);

        v1.SetData(numRows, VECX_ALLOCA(numRows));
        w1.SetData(numRows, VECX_ALLOCA(numRows));

        if (index != null) {

            // find the pivot row
            for (p = i = 0; i < numRows; i++) {
                if (index[i] == r) {
                    p = i;
                    break;
                }
            }

            // update the row and column to identity
            v1 = v.oNegative();
            w1 = u.oNegative();

            if (p != r) {
                List.idSwap(v1.p, v1.p, index[r], index[p]);
                List.idSwap(index, index, r, p);
            }

            v1.p[r] += 1.0f;
            w1.p[r] = 0.0f;

            if (!LU_UpdateRowColumn(v1, w1, r, index)) {
                return false;
            }

            if (p != r) {

                if (idMath.Fabs(u.p[p]) < 1e-4f) {
                    // NOTE: an additional row interchange is required for numerical stability
                }

                // move row index[r] of the original matrix to row index[p] of the original matrix
                v1.Zero();
                v1.p[index[p]] = 1.0f;
                w1 = u.oPlus(w.oNegative());

                if (!LU_UpdateRankOne(v1, w1, 1.0f, index)) {
                    return false;
                }
            }

            // remove the row from the permutation index
            for (i = r; i < numRows - 1; i++) {
                index[i] = index[i + 1];
            }
            for (i = 0; i < numRows - 1; i++) {
                if (index[i] > r) {
                    index[i]--;
                }
            }

        } else {

            v1 = v.oNegative();
            w1 = w.oNegative();
            v1.p[r] += 1.0f;
            w1.p[r] = 0.0f;

            if (!LU_UpdateRowColumn(v1, w1, r, index)) {
                return false;
            }
        }

        // physically remove the row and column
        Update_Decrement(r);

        return true;
    }

    /*
     ============
     idMatX::LU_Solve

     Solve Ax = b with A factored in-place as: LU
     ============
     */
    public void LU_Solve(idVecX x, final idVecX b, final int[] index) {
        int i, j;
        double sum;

        assert (x.GetSize() == numColumns && b.GetSize() == numRows);

        // solve L
        for (i = 0; i < numRows; i++) {
            if (index != null) {
                sum = b.p[index[i]];
            } else {
                sum = b.p[i];
            }
            for (j = 0; j < i; j++) {
                sum -= this.oGet(i, j) * x.p[j];
            }
            x.p[i] = (float) sum;
        }

        // solve U
        for (i = numRows - 1; i >= 0; i--) {
            sum = x.p[i];
            for (j = i + 1; j < numRows; j++) {
                sum -= this.oGet(i, j) * x.p[j];
            }
            x.p[i] = (float) (sum / this.oGet(i, i));
        }
    }

    /**
     * ============ idMatX::LU_Inverse
     *
     * Calculates the inverse of the matrix which is factored in-place as LU
     * ============
     */
    public void LU_Inverse(idMatX inv, final int[] index) {
        int i, j;
        idVecX x = new idVecX(), b = new idVecX();

        assert (numRows == numColumns);

        x.SetData(numRows, VECX_ALLOCA(numRows));
        b.SetData(numRows, VECX_ALLOCA(numRows));
        b.Zero();
        inv.SetSize(numRows, numColumns);

        for (i = 0; i < numRows; i++) {

            b.p[i] = 1.0f;
            LU_Solve(x, b, index);
            for (j = 0; j < numRows; j++) {
                inv.oSet(j, i, x.p[j]);
            }
            b.p[i] = 0.0f;
        }
    }

    /**
     * ============ idMatX::LU_UnpackFactors
     *
     * Unpacks the in-place LU factorization. ============
     */
    public void LU_UnpackFactors(idMatX L, idMatX U) {
        int i, j;

        L.Zero(numRows, numColumns);
        U.Zero(numRows, numColumns);
        for (i = 0; i < numRows; i++) {
            for (j = 0; j < i; j++) {
                L.oSet(i, j, this.oGet(i, j));
            }
            L.oSet(i, i, 1.0f);
            for (j = i; j < numColumns; j++) {
                U.oSet(i, j, this.oGet(i, j));
            }
        }
    }

    /**
     * ============ idMatX::LU_MultiplyFactors
     *
     * Multiplies the factors of the in-place LU factorization to form the
     * original matrix. ============
     */
    public void LU_MultiplyFactors(idMatX m, final int[] index) {
        int r, rp, i, j;
        double sum;

        m.SetSize(numRows, numColumns);

        for (r = 0; r < numRows; r++) {

            if (index != null) {
                rp = index[r];
            } else {
                rp = r;
            }

            // calculate row of matrix
            for (i = 0; i < numColumns; i++) {
                if (i >= r) {
                    sum = this.oGet(r, i);
                } else {
                    sum = 0.0f;
                }
                for (j = 0; j <= i && j < r; j++) {
                    sum += this.oGet(r, j) * this.oGet(j, i);
                }
                m.oSet(rp, i, (float) sum);
            }
        }
    }

    /**
     * ============ idMatX::QR_Factor
     *
     * in-place factorization: QR Q is an orthogonal matrix represented as a
     * product of Householder matrices stored in the lower triangle and c. R is
     * a triangular matrix stored in the upper triangle except for the diagonal
     * elements which are stored in d. The initial matrix has to be square.
     * ============
     */
    public boolean QR_Factor(idVecX c, idVecX d) {// factor in-place: Q * R
        int i, j, k;
        double scale, s, t, sum;
        boolean singular = false;

        assert (numRows == numColumns);
        assert (c.GetSize() >= numRows && d.GetSize() >= numRows);

        for (k = 0; k < numRows - 1; k++) {

            scale = 0.0f;
            for (i = k; i < numRows; i++) {
                s = idMath.Fabs(this.oGet(i, k));
                if (s > scale) {
                    scale = s;
                }
            }
            if (scale == 0.0f) {
                singular = true;
                c.p[k] = d.p[k] = 0.0f;
            } else {

                s = 1.0f / scale;
                for (i = k; i < numRows; i++) {
                    this.oMulSet(i, k, s);
                }

                sum = 0.0f;
                for (i = k; i < numRows; i++) {
                    s = this.oGet(i, k);
                    sum += s * s;
                }

                s = idMath.Sqrt((float) sum);
                if (this.oGet(k, k) < 0.0f) {
                    s = -s;
                }
                this.oPluSet(k, k, s);
                c.p[k] = (float) (s * this.oGet(k, k));
                d.p[k] = (float) (-scale * s);

                for (j = k + 1; j < numRows; j++) {

                    sum = 0.0f;
                    for (i = k; i < numRows; i++) {
                        sum += this.oGet(i, k) * this.oGet(i, j);
                    }
                    t = sum / c.p[k];
                    for (i = k; i < numRows; i++) {
                        this.oMinSet(i, j, t * this.oGet(i, k));
                    }
                }
            }
        }
        d.p[numRows - 1] = this.oGet(numRows - 1, numRows - 1);
        if (d.p[numRows - 1] == 0.0f) {
            singular = true;
        }

        return !singular;
    }

    /**
     * ============ idMatX::QR_Rotate
     *
     * Performs a Jacobi rotation on the rows i and i+1 of the unpacked QR
     * factors. ============
     */
    public boolean QR_UpdateRankOne(idMatX R, final idVecX v, final idVecX w, float alpha) {
        int i, k;
        float f;
        idVecX u = new idVecX();

        assert (v.GetSize() >= numColumns);
        assert (w.GetSize() >= numRows);

        u.SetData(v.GetSize(), VECX_ALLOCA(v.GetSize()));
        TransposeMultiply(u, v);
        u.oMulSet(alpha);

        for (k = v.GetSize() - 1; k > 0; k--) {
            if (u.p[k] != 0.0f) {
                break;
            }
        }
        for (i = k - 1; i >= 0; i--) {
            QR_Rotate(R, i, u.p[i], -u.p[i + 1]);
            if (u.p[i] == 0.0f) {
                u.p[i] = idMath.Fabs(u.p[i + 1]);
            } else if (idMath.Fabs(u.p[i]) > idMath.Fabs(u.p[i + 1])) {
                f = u.p[i + 1] / u.p[i];
                u.p[i] = idMath.Fabs(u.p[i]) * idMath.Sqrt(1.0f + f * f);
            } else {
                f = u.p[i] / u.p[i + 1];
                u.p[i] = idMath.Fabs(u.p[i + 1]) * idMath.Sqrt(1.0f + f * f);
            }
        }
        for (i = 0; i < v.GetSize(); i++) {
            R.oPluSet(0, i, u.p[0] * w.p[i]);
        }
        for (i = 0; i < k; i++) {
            QR_Rotate(R, i, -R.oGet(i, i), R.oGet(i + 1, i));
        }
        return true;
    }

    /**
     * ============ idMatX::QR_UpdateRowColumn
     *
     * Updates the unpacked QR factorization to obtain the factors for the
     * matrix:
     *
     * [ 0 a 0 ]
     * QR + [ d b e ] [ 0 c 0 ]
     *
     * where: a = v[0,r-1], b = v[r], c = v[r+1,numRows-1], d = w[0,r-1], w[r] =
     * 0.0f, e = w[r+1,numColumns-1] ============
     */
    public boolean QR_UpdateRowColumn(idMatX R, final idVecX v, final idVecX w, int r) {
        idVecX s = new idVecX();

        assert (v.GetSize() >= numColumns);
        assert (w.GetSize() >= numRows);
        assert (r >= 0 && r < numRows && r < numColumns);
        assert (w.p[r] == 0.0f);

        s.SetData(Lib.Max(numRows, numColumns), VECX_ALLOCA(Lib.Max(numRows, numColumns)));
        s.Zero();
        s.p[r] = 1.0f;

        if (!QR_UpdateRankOne(R, v, s, 1.0f)) {
            return false;
        }
        if (!QR_UpdateRankOne(R, s, w, 1.0f)) {
            return false;
        }
        return true;
    }

    /**
     * ============ idMatX::QR_UpdateIncrement
     *
     * Updates the unpacked QR factorization to obtain the factors for the
     * matrix:
     *
     * [ A a ]
     * [ c b ]
     *
     * where: a = v[0,numRows-1], b = v[numRows], c = w[0,numColumns-1],
     * w[numColumns] = 0 ============
     */
    public boolean QR_UpdateIncrement(idMatX R, final idVecX v, final idVecX w) {
        idVecX v2 = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows + 1);
        assert (w.GetSize() >= numColumns + 1);

        ChangeSize(numRows + 1, numColumns + 1, true);
        this.oSet(numRows - 1, numRows - 1, 1.0f);

        R.ChangeSize(R.numRows + 1, R.numColumns + 1, true);
        R.oSet(R.numRows - 1, R.numRows - 1, 1.0f);

        v2.SetData(numRows, VECX_ALLOCA(numRows));
        v2 = v;
        v2.p[numRows - 1] -= 1.0f;

        return QR_UpdateRowColumn(R, v2, w, numRows - 1);
    }

    /**
     * ============ idMatX::QR_UpdateDecrement
     *
     * Updates the unpacked QR factorization to obtain the factors for the
     * matrix with row r and column r removed. v and w should store the column
     * and row of the original matrix respectively. ============
     */
    public boolean QR_UpdateDecrement(idMatX R, final idVecX v, final idVecX w, int r) {
        idVecX v1 = new idVecX(), w1 = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);
        assert (w.GetSize() >= numColumns);
        assert (r >= 0 && r < numRows && r < numColumns);

        v1.SetData(numRows, VECX_ALLOCA(numRows));
        w1.SetData(numRows, VECX_ALLOCA(numRows));

        // update the row and column to identity
        v1 = v.oNegative();
        w1 = w.oNegative();
        v1.p[r] += 1.0f;
        w1.p[r] = 0.0f;

        if (!QR_UpdateRowColumn(R, v1, w1, r)) {
            return false;
        }

        // physically remove the row and column
        Update_Decrement(r);
        R.Update_Decrement(r);

        return true;
    }

    public void QR_Solve(idVecX x, final idVecX b, final idVecX c, final idVecX d) {
        int i, j;
        double sum, t;

        assert (numRows == numColumns);
        assert (x.GetSize() >= numRows && b.GetSize() >= numRows);
        assert (c.GetSize() >= numRows && d.GetSize() >= numRows);

        for (i = 0; i < numRows; i++) {
            x.p[i] = b.p[i];
        }

        // multiply b with transpose of Q
        for (i = 0; i < numRows - 1; i++) {

            sum = 0.0f;
            for (j = i; j < numRows; j++) {
                sum += this.oGet(j, i) * x.p[j];
            }
            t = sum / c.p[i];
            for (j = i; j < numRows; j++) {
                x.p[j] -= t * this.oGet(j, i);
            }
        }

        // backsubstitution with R
        for (i = numRows - 1; i >= 0; i--) {

            sum = x.p[i];
            for (j = i + 1; j < numRows; j++) {
                sum -= this.oGet(i, j) * x.p[j];
            }
            x.p[i] = (float) (sum / d.p[i]);
        }
    }

    /**
     * ============ idMatX::QR_Solve
     *
     * Solve Ax = b with A factored as: QR ============
     */
    public void QR_Solve(idVecX x, final idVecX b, final idMatX R) {
        int i, j;
        double sum;

        assert (numRows == numColumns);

        // multiply b with transpose of Q
        TransposeMultiply(x, b);

        // backsubstitution with R
        for (i = numRows - 1; i >= 0; i--) {

            sum = x.p[i];
            for (j = i + 1; j < numRows; j++) {
                sum -= R.oGet(i, j) * x.p[j];
            }
            x.p[i] = (float) (sum / R.oGet(i, i));
        }
    }

    /**
     * ============ idMatX::QR_Inverse
     *
     * Calculates the inverse of the matrix which is factored in-place as: QR
     * ============
     */
    public void QR_Inverse(idMatX inv, final idVecX c, final idVecX d) {
        int i, j;
        idVecX x = new idVecX(), b = new idVecX();

        assert (numRows == numColumns);

        x.SetData(numRows, VECX_ALLOCA(numRows));
        b.SetData(numRows, VECX_ALLOCA(numRows));
        b.Zero();
        inv.SetSize(numRows, numColumns);

        for (i = 0; i < numRows; i++) {

            b.p[i] = 1.0f;
            QR_Solve(x, b, c, d);
            for (j = 0; j < numRows; j++) {
                inv.oSet(j, i, x.p[j]);
            }
            b.p[i] = 0.0f;
        }
    }

    /**
     * ============ idMatX::QR_UnpackFactors
     *
     * Unpacks the in-place QR factorization. ============
     */
    public void QR_UnpackFactors(idMatX Q, idMatX R, final idVecX c, final idVecX d) {
        int i, j, k;
        double sum;

        Q.Identity(numRows, numColumns);
        for (i = 0; i < numColumns - 1; i++) {
            if (c.p[i] == 0.0f) {
                continue;
            }
            for (j = 0; j < numRows; j++) {
                sum = 0.0f;
                for (k = i; k < numColumns; k++) {
                    sum += this.oGet(k, i) * Q.oGet(j, k);
                }
                sum /= c.p[i];
                for (k = i; k < numColumns; k++) {
                    Q.oMinSet(j, k, sum * this.oGet(k, i));
                }
            }
        }

        R.Zero(numRows, numColumns);
        for (i = 0; i < numRows; i++) {
            R.oSet(i, i, d.p[i]);
            for (j = i + 1; j < numColumns; j++) {
                R.oSet(i, j, this.oGet(i, j));
            }
        }
    }

    /**
     * ============ idMatX::QR_MultiplyFactors
     *
     * Multiplies the factors of the in-place QR factorization to form the
     * original matrix. ============
     */
    public void QR_MultiplyFactors(idMatX m, final idVecX c, final idVecX d) {
        int i, j, k;
        double sum;
        idMatX Q = new idMatX();

        Q.Identity(numRows, numColumns);
        for (i = 0; i < numColumns - 1; i++) {
            if (c.p[i] == 0.0f) {
                continue;
            }
            for (j = 0; j < numRows; j++) {
                sum = 0.0f;
                for (k = i; k < numColumns; k++) {
                    sum += this.oGet(k, i) * Q.oGet(j, k);
                }
                sum /= c.p[i];
                for (k = i; k < numColumns; k++) {
                    Q.oMinSet(j, k, sum * this.oGet(k, i));
                }
            }
        }

        for (i = 0; i < numRows; i++) {
            for (j = 0; j < numColumns; j++) {
                sum = Q.oGet(i, j) * d.p[i];
                for (k = 0; k < i; k++) {
                    sum += Q.oGet(i, k) * this.oGet(j, k);
                }
                m.oSet(i, j, (float) sum);
            }
        }
    }

    /**
     * ============ idMatX::SVD_Factor
     *
     * in-place factorization: U * Diag(w) * V.Transpose() known as the Singular
     * Value Decomposition. U is a column-orthogonal matrix which overwrites the
     * original matrix. w is a diagonal matrix with all elements >= 0 which are
     * the singular values. V is the transpose of an orthogonal matrix.
     * ============
     */
    public boolean SVD_Factor(idVecX w, idMatX V)// factor in-place: U * Diag(w) * V.Transpose()
    {
        int flag, i, its, j, jj, k, l, nm;
        double c, f, h, s, x, y, z, r, g = 0.0f;
        float[] anorm = {0};
        idVecX rv1 = new idVecX();

        if (numRows < numColumns) {
            return false;
        }

        rv1.SetData(numColumns, VECX_ALLOCA(numColumns));
        rv1.Zero();
        w.Zero(numColumns);
        V.Zero(numColumns, numColumns);

        SVD_BiDiag(w, rv1, anorm);
        SVD_InitialWV(w, V, rv1);

        for (k = numColumns - 1; k >= 0; k--) {
            for (its = 1; its <= 30; its++) {
                flag = 1;
                nm = 0;
                for (l = k; l >= 0; l--) {
                    nm = l - 1;
                    if ((idMath.Fabs(rv1.p[l]) + anorm[0]) == anorm[0] /* idMath::Fabs( rv1.p[l] ) < idMath::FLT_EPSILON */) {
                        flag = 0;
                        break;
                    }
                    if ((idMath.Fabs(w.p[nm]) + anorm[0]) == anorm[0] /* idMath::Fabs( w[nm] ) < idMath::FLT_EPSILON */) {
                        break;
                    }
                }
                if (flag != 0) {
                    c = 0.0f;
                    s = 1.0f;
                    for (i = l; i <= k; i++) {
                        f = s * rv1.p[i];

                        if ((idMath.Fabs((float) f) + anorm[0]) != anorm[0] /* idMath::Fabs( f ) > idMath::FLT_EPSILON */) {
                            g = w.p[i];
                            h = Pythag((float) f, (float) g);
                            w.p[i] = (float) h;
                            h = 1.0f / h;
                            c = g * h;
                            s = -f * h;
                            for (j = 0; j < numRows; j++) {
                                y = this.oGet(j, nm);
                                z = this.oGet(j, i);
                                this.oSet(j, nm, (float) (y * c + z * s));
                                this.oSet(j, i, (float) (z * c - y * s));
                            }
                        }
                    }
                }
                z = w.p[k];
                if (l == k) {
                    if (z < 0.0f) {
                        w.p[k] = (float) -z;
                        for (j = 0; j < numColumns; j++) {
                            V.oNegative(j, k);
                        }
                    }
                    break;
                }
                if (its == 30) {
                    return false;		// no convergence
                }
                x = w.p[l];
                nm = k - 1;
                y = w.p[nm];
                g = rv1.p[nm];
                h = rv1.p[k];
                f = ((y - z) * (y + z) + (g - h) * (g + h)) / (2.0f * h * y);
                g = Pythag((float) f, 1.0f);
                r = (f >= 0.0f ? g : -g);
                f = ((x - z) * (x + z) + h * ((y / (f + r)) - h)) / x;
                c = s = 1.0f;
                for (j = l; j <= nm; j++) {
                    i = j + 1;
                    g = rv1.p[i];
                    y = w.p[i];
                    h = s * g;
                    g = c * g;
                    z = Pythag((float) f, (float) h);
                    rv1.p[j] = (float) z;
                    c = f / z;
                    s = h / z;
                    f = x * c + g * s;
                    g = g * c - x * s;
                    h = y * s;
                    y = y * c;
                    for (jj = 0; jj < numColumns; jj++) {
                        x = V.oGet(jj, j);
                        z = V.oGet(jj, i);
                        V.oSet(jj, j, (float) (x * c + z * s));
                        V.oSet(jj, i, (float) (z * c - x * s));
                    }
                    z = Pythag((float) f, (float) h);
                    w.p[j] = (float) z;
                    if (z != 0) {
                        z = 1.0f / z;
                        c = f * z;
                        s = h * z;
                    }
                    f = (c * g) + (s * y);
                    x = (c * y) - (s * g);
                    for (jj = 0; jj < numRows; jj++) {
                        y = this.oGet(jj, j);
                        z = this.oGet(jj, i);
                        this.oSet(jj, j, (float) (y * c + z * s));
                        this.oSet(jj, i, (float) (z * c - y * s));
                    }
                }
                rv1.p[l] = 0.0f;
                rv1.p[k] = (float) f;
                w.p[k] = (float) x;
            }
        }
        return true;
    }

    /**
     * ============ idMatX::SVD_Solve
     *
     * Solve Ax = b with A factored as: U * Diag(w) * V.Transpose() ============
     */
    public void SVD_Solve(idVecX x, final idVecX b, final idVecX w, final idMatX V) {
        int i, j;
        double sum;
        idVecX tmp = new idVecX();

        assert (x.GetSize() >= numColumns);
        assert (b.GetSize() >= numColumns);
        assert (w.GetSize() == numColumns);
        assert (V.GetNumRows() == numColumns && V.GetNumColumns() == numColumns);

        tmp.SetData(numColumns, VECX_ALLOCA(numColumns));

        for (i = 0; i < numColumns; i++) {
            sum = 0.0f;
            if (w.p[i] >= idMath.FLT_EPSILON) {
                for (j = 0; j < numRows; j++) {
                    sum += this.oGet(j, i) * b.p[j];
                }
                sum /= w.p[i];
            }
            tmp.p[i] = (float) sum;
        }
        for (i = 0; i < numColumns; i++) {
            sum = 0.0f;
            for (j = 0; j < numColumns; j++) {
                sum += V.oGet(i, j) * tmp.p[j];
            }
            x.p[i] = (float) sum;
        }
    }
    /*
     ============
     idMatX::SVD_Inverse

     Calculates the inverse of the matrix which is factored in-place as: U * Diag(w) * V.Transpose()
     ============
     */

    public void SVD_Inverse(idMatX inv, final idVecX w, final idMatX V) {
        int i, j, k;
        double wi, sum;
        idMatX V2;//= new idMatX();

        assert (numRows == numColumns);

        V2 = V;

        // V * [diag(1/w[i])]
        for (i = 0; i < numRows; i++) {
            wi = w.p[i];
            wi = (wi < idMath.FLT_EPSILON) ? 0.0f : 1.0f / wi;
            for (j = 0; j < numColumns; j++) {
                V2.oMulSet(j, i, wi);
            }
        }

        // V * [diag(1/w[i])] * Ut
        for (i = 0; i < numRows; i++) {
            for (j = 0; j < numColumns; j++) {
                sum = V2.oGet(i, 0) * this.oGet(j, 0);
                for (k = 1; k < numColumns; k++) {
                    sum += V2.oGet(i, k) * this.oGet(j, k);
                }
                inv.oSet(i, j, (float) sum);
            }
        }
    }

    /**
     * ============ idMatX::SVD_MultiplyFactors
     *
     * Multiplies the factors of the in-place SVD factorization to form the
     * original matrix. ============
     */
    public void SVD_MultiplyFactors(idMatX m, final idVecX w, final idMatX V) {
        int r, i, j;
        double sum;

        m.SetSize(numRows, V.GetNumRows());

        for (r = 0; r < numRows; r++) {
            // calculate row of matrix
            if (w.p[r] >= idMath.FLT_EPSILON) {
                for (i = 0; i < V.GetNumRows(); i++) {
                    sum = 0.0f;
                    for (j = 0; j < numColumns; j++) {
                        sum += this.oGet(r, j) * V.oGet(i, j);
                    }
                    m.oSet(r, i, (float) (sum * w.p[r]));
                }
            } else {
                for (i = 0; i < V.GetNumRows(); i++) {
                    m.oSet(r, i, 0.0f);
                }
            }
        }
    }

    /**
     * ============ idMatX::Cholesky_Factor
     *
     * in-place Cholesky factorization: LL' L is a triangular matrix stored in
     * the lower triangle. The upper triangle is not cleared. The initial matrix
     * has to be symmetric positive definite. ============
     */
    public boolean Cholesky_Factor() {// factor in-place: L * L.Transpose()
        int i, j, k;
        float[] invSqrt = new float[numRows];
        double sum;

        assert (numRows == numColumns);

//	invSqrt = (float *) _alloca16( numRows * sizeof( float ) );
        for (i = 0; i < numRows; i++) {

            for (j = 0; j < i; j++) {

                sum = this.oGet(i, j);
                for (k = 0; k < j; k++) {
                    sum -= this.oGet(i, k) * this.oGet(j, k);
                }
                this.oSet(i, j, (float) (sum * invSqrt[j]));
            }

            sum = this.oGet(i, i);
            for (k = 0; k < i; k++) {
                sum -= this.oGet(i, k) * this.oGet(i, k);
            }

            if (sum <= 0.0f) {
                return false;
            }

            invSqrt[i] = idMath.InvSqrt((float) sum);
            this.oSet(i, i, (float) (invSqrt[i] * sum));
        }
        return true;
    }

    /**
     * ============ idMatX::Cholesky_UpdateRankOne
     *
     * Updates the in-place Cholesky factorization to obtain the factors for the
     * matrix: LL' + alpha * v * v' If offset > 0 only the lower right corner
     * starting at (offset, offset) is updated. ============
     */
    public boolean Cholesky_UpdateRankOne(final idVecX v, float alpha) {
        return Cholesky_UpdateRankOne(v, alpha, 0);
    }

    public boolean Cholesky_UpdateRankOne(final idVecX v, float alpha, int offset) {
        int i, j;
        float[] y;
        double diag, invDiag, diagSqr, newDiag, newDiagSqr, beta, p, d;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);
        assert (offset >= 0 && offset < numRows);

//	y = (float *) _alloca16( v.GetSize() * sizeof( float ) );
//	memcpy( y, v.ToFloatPtr(), v.GetSize() * sizeof( float ) );
        y = v.ToFloatPtr();

        for (i = offset; i < numColumns; i++) {
            p = y[i];
            diag = this.oGet(i, i);
            invDiag = 1.0f / diag;
            diagSqr = diag * diag;
            newDiagSqr = diagSqr + alpha * p * p;

            if (newDiagSqr <= 0.0f) {
                return false;
            }

            this.oSet(i, i, (float) (newDiag = idMath.Sqrt((float) newDiagSqr)));

            alpha /= newDiagSqr;
            beta = p * alpha;
            alpha *= diagSqr;

            for (j = i + 1; j < numRows; j++) {

                d = this.oGet(j, i) * invDiag;

                y[j] -= p * d;
                d += beta * y[j];

                this.oSet(j, i, (float) (d * newDiag));
            }
        }
        return true;
    }

    /*
     ============
     idMatX::Cholesky_UpdateRowColumn

     Updates the in-place Cholesky factorization to obtain the factors for the matrix:

     [ 0  a  0 ]
     LL' + [ a  b  c ]
     [ 0  c  0 ]

     where: a = v[0,r-1], b = v[r], c = v[r+1,numRows-1]
     ============
     */
    public boolean Cholesky_UpdateRowColumn(final idVecX v, int r) {
        int i, j;
        double sum;
        float[] original, y;
        idVecX addSub = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);
        assert (r >= 0 && r < numRows);

//	addSub.SetData( numColumns, (float *) _alloca16( numColumns * sizeof( float ) ) );
        addSub.SetData(numColumns, new float[numColumns]);

        if (r == 0) {

            if (numColumns == 1) {
                float v0 = v.p[0];
                sum = this.oGet(0, 0);
                sum = sum * sum;
                sum = sum + v0;
                if (sum <= 0.0f) {
                    return false;
                }
                this.oSet(0, 0, idMath.Sqrt((float) sum));
                return true;
            }
            for (i = 0; i < numColumns; i++) {
                addSub.p[i] = v.p[i];
            }

        } else {

//		original = (float *) _alloca16( numColumns * sizeof( float ) );
//		y = (float *) _alloca16( numColumns * sizeof( float ) );
            original = new float[numColumns];
            y = new float[numColumns];

            // calculate original row/column of matrix
            for (i = 0; i < numRows; i++) {
                sum = 0.0f;
                for (j = 0; j <= i; j++) {
                    sum += this.oGet(r, j) * this.oGet(i, j);
                }
                original[i] = (float) sum;
            }

            // solve for y in L * y = original + v
            for (i = 0; i < r; i++) {
                sum = original[i] + v.p[i];
                for (j = 0; j < i; j++) {
                    sum -= this.oGet(r, j) * this.oGet(i, j);
                }
                this.oSet(r, i, (float) (sum / this.oGet(i, i)));
            }

            // if the last row/column of the matrix is updated
            if (r == numColumns - 1) {
                // only calculate new diagonal
                sum = original[r] + v.p[r];
                for (j = 0; j < r; j++) {
                    sum -= this.oGet(r, j) * this.oGet(r, j);
                }
                if (sum <= 0.0f) {
                    return false;
                }
                this.oSet(r, r, idMath.Sqrt((float) sum));
                return true;
            }

            // calculate the row/column to be added to the lower right sub matrix starting at (r, r)
            for (i = r; i < numColumns; i++) {
                sum = 0.0f;
                for (j = 0; j <= r; j++) {
                    sum += this.oGet(r, j) * this.oGet(i, j);
                }
                addSub.p[i] = (float) (v.p[i] - (sum - original[i]));
            }
        }

        // add row/column to the lower right sub matrix starting at (r, r)
//#else
        float[] v1, v2;
        double diag, invDiag, diagSqr, newDiag, newDiagSqr;
        double alpha1, alpha2, beta1, beta2, p1, p2, d;

//	v1 = (float *) _alloca16( numColumns * sizeof( float ) );
//	v2 = (float *) _alloca16( numColumns * sizeof( float ) );
        v1 = new float[numColumns];
        v2 = new float[numColumns];

        d = idMath.SQRT_1OVER2;
        v1[r] = (float) ((0.5f * addSub.p[r] + 1.0f) * d);
        v2[r] = (float) ((0.5f * addSub.p[r] - 1.0f) * d);
        for (i = r + 1; i < numColumns; i++) {
            v1[i] = v2[i] = (float) (addSub.p[i] * d);
        }

        alpha1 = 1.0f;
        alpha2 = -1.0f;

        // simultaneous update/downdate of the sub matrix starting at (r, r)
        for (i = r; i < numColumns; i++) {
            p1 = v1[i];
            diag = this.oGet(i, i);
            invDiag = 1.0f / diag;
            diagSqr = diag * diag;
            newDiagSqr = diagSqr + alpha1 * p1 * p1;

            if (newDiagSqr <= 0.0f) {
                return false;
            }

            alpha1 /= newDiagSqr;
            beta1 = p1 * alpha1;
            alpha1 *= diagSqr;

            p2 = v2[i];
            diagSqr = newDiagSqr;
            newDiagSqr = diagSqr + alpha2 * p2 * p2;

            if (newDiagSqr <= 0.0f) {
                return false;
            }

            this.oSet(i, i, (float) (newDiag = idMath.Sqrt((float) newDiagSqr)));

            alpha2 /= newDiagSqr;
            beta2 = p2 * alpha2;
            alpha2 *= diagSqr;

            for (j = i + 1; j < numRows; j++) {

                d = this.oGet(j, i) * invDiag;

                v1[j] -= p1 * d;
                d += beta1 * v1[j];

                v2[j] -= p2 * d;
                d += beta2 * v2[j];

                this.oSet(j, i, (float) (d * newDiag));
            }
        }

//#endif
        return true;
    }

    /**
     * ============ idMatX::Cholesky_UpdateIncrement
     *
     * Updates the in-place Cholesky factorization to obtain the factors for the
     * matrix:
     *
     * [ A a ]
     * [ a b ]
     *
     * where: a = v[0,numRows-1], b = v[numRows] ============
     */
    public boolean Cholesky_UpdateIncrement(final idVecX v) {
        int i, j;
        float[] x;
        double sum;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows + 1);

        ChangeSize(numRows + 1, numColumns + 1, false);

//	x = (float *) _alloca16( numRows * sizeof( float ) );
        x = new float[numRows];

        // solve for x in L * x = v
        for (i = 0; i < numRows - 1; i++) {
            sum = v.p[i];
            for (j = 0; j < i; j++) {
                sum -= this.oGet(i, j) * x[j];
            }
            x[i] = (float) (sum / this.oGet(i, i));
        }

        // calculate new row of L and calculate the square of the diagonal entry
        sum = v.p[numRows - 1];
        for (i = 0; i < numRows - 1; i++) {
            this.oSet(numRows - 1, i, x[i]);
            sum -= x[i] * x[i];
        }

        if (sum <= 0.0f) {
            return false;
        }

        // store the diagonal entry
        this.oSet(numRows - 1, numRows - 1, idMath.Sqrt((float) sum));

        return true;
    }

    /**
     * ============ idMatX::Cholesky_UpdateDecrement
     *
     * Updates the in-place Cholesky factorization to obtain the factors for the
     * matrix with row r and column r removed. v should store the row of the
     * original matrix. ============
     */
    public boolean Cholesky_UpdateDecrement(final idVecX v, int r) {
        idVecX v1 = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);
        assert (r >= 0 && r < numRows);

        v1.SetData(numRows, VECX_ALLOCA(numRows));

        // update the row and column to identity
        v1 = v.oNegative();
        v1.p[r] += 1.0f;

        // NOTE:	msvc compiler bug: the this pointer stored in edi is expected to stay
        //			untouched when calling Cholesky_UpdateRowColumn in the if statement
//#if 0
//	if ( !Cholesky_UpdateRowColumn( v1, r ) ) {
//#else
        boolean ret = Cholesky_UpdateRowColumn(v1, r);
        if (!ret) {
//#endif
            return false;
        }

        // physically remove the row and column
        Update_Decrement(r);

        return true;
    }

    /**
     * ============ idMatX::Cholesky_Solve
     *
     * Solve Ax = b with A factored in-place as: LL' ============
     */
    public void Cholesky_Solve(idVecX x, final idVecX b) {
        int i, j;
        double sum;

        assert (numRows == numColumns);
        assert (x.GetSize() >= numRows && b.GetSize() >= numRows);

        // solve L
        for (i = 0; i < numRows; i++) {
            sum = b.p[i];
            for (j = 0; j < i; j++) {
                sum -= this.oGet(i, j) * x.p[j];
            }
            x.p[i] = (float) (sum / this.oGet(i, i));
        }

        // solve Lt
        for (i = numRows - 1; i >= 0; i--) {
            sum = x.p[i];
            for (j = i + 1; j < numRows; j++) {
                sum -= this.oGet(j, i) * x.p[j];
            }
            x.p[i] = (float) (sum / this.oGet(i, i));
        }
    }

    /**
     * ============ idMatX::Cholesky_Inverse
     *
     * Calculates the inverse of the matrix which is factored in-place as: LL'
     * ============
     */
    public void Cholesky_Inverse(idMatX inv) {
        int i, j;
        idVecX x = new idVecX(), b = new idVecX();

        assert (numRows == numColumns);

        x.SetData(numRows, VECX_ALLOCA(numRows));
        b.SetData(numRows, VECX_ALLOCA(numRows));
        b.Zero();
        inv.SetSize(numRows, numColumns);

        for (i = 0; i < numRows; i++) {

            b.p[i] = 1.0f;
            Cholesky_Solve(x, b);
            for (j = 0; j < numRows; j++) {
                inv.oSet(j, i, x.p[j]);
            }
            b.p[i] = 0.0f;
        }
    }

    /**
     * ============ idMatX::Cholesky_MultiplyFactors
     *
     * Multiplies the factors of the in-place Cholesky factorization to form the
     * original matrix. ============
     */
    public void Cholesky_MultiplyFactors(idMatX m) {
        int r, i, j;
        double sum;

        m.SetSize(numRows, numColumns);

        for (r = 0; r < numRows; r++) {

            // calculate row of matrix
            for (i = 0; i < numRows; i++) {
                sum = 0.0f;
                for (j = 0; j <= i && j <= r; j++) {
                    sum += this.oGet(r, j) * this.oGet(i, j);
                }
                m.oSet(r, i, (float) sum);
            }
        }
    }

    /*
     ============
     idMatX::LDLT_Factor

     in-place factorization: LDL'
     L is a triangular matrix stored in the lower triangle.
     L has ones on the diagonal that are not stored.
     D is a diagonal matrix stored on the diagonal.
     The upper triangle is not cleared.
     The initial matrix has to be symmetric.
     ============
     */
    public boolean LDLT_Factor() {// factor in-place: L * D * L.Transpose()
        int i, j, k;
        float[] v;
        double d, sum;

        assert (numRows == numColumns);

//	v = (float *) _alloca16( numRows * sizeof( float ) );
        v = new float[numRows];

        for (i = 0; i < numRows; i++) {

            sum = this.oGet(i, i);
            for (j = 0; j < i; j++) {
                d = this.oGet(i, j);
                v[j] = (float) (this.oGet(j, j) * d);
                sum -= v[j] * d;
            }

            if (sum == 0.0f) {
                return false;
            }

            this.oSet(i, i, (float) sum);
            d = 1.0f / sum;

            for (j = i + 1; j < numRows; j++) {
                sum = this.oGet(j, i);
                for (k = 0; k < i; k++) {
                    sum -= this.oGet(j, k) * v[k];
                }
                this.oSet(j, i, (float) (sum * d));
            }
        }

        return true;
    }

    /*
     ============
     idMatX::LDLT_UpdateRankOne

     Updates the in-place LDL' factorization to obtain the factors for the matrix: LDL' + alpha * v * v'
     If offset > 0 only the lower right corner starting at (offset, offset) is updated.
     ============
     */
    public boolean LDLT_UpdateRankOne(final idVecX v, float alpha, int offset) {
        int i, j;
        float[] y;
        double diag, newDiag, beta, p, d;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);
        assert (offset >= 0 && offset < numRows);

//	y = (float *) _alloca16( v.GetSize() * sizeof( float ) );
//	memcpy( y, v.ToFloatPtr(), v.GetSize() * sizeof( float ) );
        y = v.ToFloatPtr();

        for (i = offset; i < numColumns; i++) {
            p = y[i];
            diag = this.oGet(i, i);
            this.oSet(i, i, (float) (newDiag = diag + alpha * p * p));

            if (newDiag == 0.0f) {
                return false;
            }

            alpha /= newDiag;
            beta = p * alpha;
            alpha *= diag;

            for (j = i + 1; j < numRows; j++) {

                d = this.oGet(j, i);

                y[j] -= p * d;
                d += beta * y[j];

                this.oSet(j, i, (float) d);
            }
        }

        return true;
    }

    /*
     ============
     idMatX::LDLT_UpdateRowColumn

     Updates the in-place LDL' factorization to obtain the factors for the matrix:

     [ 0  a  0 ]
     LDL' + [ a  b  c ]
     [ 0  c  0 ]

     where: a = v[0,r-1], b = v[r], c = v[r+1,numRows-1]
     ============
     */
    public boolean LDLT_UpdateRowColumn(final idVecX v, int r) {
        int i, j;
        double sum;
        float[] original, y;
        idVecX addSub = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);
        assert (r >= 0 && r < numRows);

        addSub.SetData(numColumns, new float[numColumns]);

        if (r == 0) {

            if (numColumns == 1) {
                this.oPluSet(0, 0, v.p[0]);
                return true;
            }
            for (i = 0; i < numColumns; i++) {
                addSub.p[i] = v.p[i];
            }

        } else {

            original = new float[numColumns];
            y = new float[numColumns];

            // calculate original row/column of matrix
            for (i = 0; i < r; i++) {
                y[i] = this.oGet(r, i) * this.oGet(i, i);
            }
            for (i = 0; i < numColumns; i++) {
                if (i < r) {
                    sum = this.oGet(i, i) * this.oGet(r, i);
                } else if (i == r) {
                    sum = this.oGet(r, r);
                } else {
                    sum = this.oGet(r, r) * this.oGet(i, r);
                }
                for (j = 0; j < i && j < r; j++) {
                    sum += this.oGet(i, j) * y[j];
                }
                original[i] = (float) sum;
            }

            // solve for y in L * y = original + v
            for (i = 0; i < r; i++) {
                sum = original[i] + v.p[i];
                for (j = 0; j < i; j++) {
                    sum -= this.oGet(i, j) * y[j];
                }
                y[i] = (float) sum;
            }

            // calculate new row of L
            for (i = 0; i < r; i++) {
                this.oSet(r, i, y[i] / this.oGet(i, i));
            }

            // if the last row/column of the matrix is updated
            if (r == numColumns - 1) {
                // only calculate new diagonal
                sum = original[r] + v.p[r];
                for (j = 0; j < r; j++) {
                    sum -= this.oGet(r, j) * y[j];
                }
                if (sum == 0.0f) {
                    return false;
                }
                this.oSet(r, r, (float) sum);
                return true;
            }

            // calculate the row/column to be added to the lower right sub matrix starting at (r, r)
            for (i = 0; i < r; i++) {
                y[i] = this.oGet(r, i) * this.oGet(i, i);
            }
            for (i = r; i < numColumns; i++) {
                if (i == r) {
                    sum = this.oGet(r, r);
                } else {
                    sum = this.oGet(r, r) * this.oGet(i, r);
                }
                for (j = 0; j < r; j++) {
                    sum += this.oGet(i, j) * y[j];
                }
                addSub.p[i] = (float) (v.p[i] - (sum - original[i]));
            }
        }

        // add row/column to the lower right sub matrix starting at (r, r)
//#else
        float[] v1, v2;
        double d, diag, newDiag, p1, p2, alpha1, alpha2, beta1, beta2;

        v1 = new float[numColumns];
        v2 = new float[numColumns];

        d = idMath.SQRT_1OVER2;
        v1[r] = (float) ((0.5f * addSub.p[r] + 1.0f) * d);
        v2[r] = (float) ((0.5f * addSub.p[r] - 1.0f) * d);
        for (i = r + 1; i < numColumns; i++) {
            v1[i] = v2[i] = (float) (addSub.p[i] * d);
        }

        alpha1 = 1.0f;
        alpha2 = -1.0f;

        // simultaneous update/downdate of the sub matrix starting at (r, r)
        for (i = r; i < numColumns; i++) {

            diag = this.oGet(i, i);
            p1 = v1[i];
            newDiag = diag + alpha1 * p1 * p1;

            if (newDiag == 0.0f) {
                return false;
            }

            alpha1 /= newDiag;
            beta1 = p1 * alpha1;
            alpha1 *= diag;

            diag = newDiag;
            p2 = v2[i];
            newDiag = diag + alpha2 * p2 * p2;

            if (newDiag == 0.0f) {
                return false;
            }

            alpha2 /= newDiag;
            beta2 = p2 * alpha2;
            alpha2 *= diag;

            this.oSet(i, i, (float) newDiag);

            for (j = i + 1; j < numRows; j++) {

                d = this.oGet(j, i);

                v1[j] -= p1 * d;
                d += beta1 * v1[j];

                v2[j] -= p2 * d;
                d += beta2 * v2[j];

                this.oSet(j, i, (float) d);
            }
        }

//#endif
        return true;
    }
    /*
     ============
     idMatX::LDLT_UpdateIncrement

     Updates the in-place LDL' factorization to obtain the factors for the matrix:

     [ A  a ]
     [ a  b ]

     where: a = v[0,numRows-1], b = v[numRows]
     ============
     */

    public boolean LDLT_UpdateIncrement(final idVecX v) {
        int i, j;
        float[] x;
        double sum, d;

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows + 1);

        ChangeSize(numRows + 1, numColumns + 1, false);

        x = new float[numRows];

        // solve for x in L * x = v
        for (i = 0; i < numRows - 1; i++) {
            sum = v.p[i];
            for (j = 0; j < i; j++) {
                sum -= this.oGet(i, j) * x[j];
            }
            x[i] = (float) sum;
        }

        // calculate new row of L and calculate the diagonal entry
        sum = v.p[numRows - 1];
        for (i = 0; i < numRows - 1; i++) {
            this.oSet(numRows - 1, i, (float) (d = x[i] / this.oGet(i, i)));
            sum -= d * x[i];
        }

        if (sum == 0.0f) {
            return false;
        }

        // store the diagonal entry
        this.oSet(numRows - 1, numRows - 1, (float) sum);

        return true;
    }

    /**
     * ============ idMatX::LDLT_UpdateDecrement
     *
     * Updates the in-place LDL' factorization to obtain the factors for the
     * matrix with row r and column r removed. v should store the row of the
     * original matrix. ============
     */
    public boolean LDLT_UpdateDecrement(final idVecX v, int r) {
        idVecX v1 = new idVecX();

        assert (numRows == numColumns);
        assert (v.GetSize() >= numRows);
        assert (r >= 0 && r < numRows);

        v1.SetData(numRows, VECX_ALLOCA(numRows));

        // update the row and column to identity
        v1 = v.oNegative();
        v1.p[r] += 1.0f;

        // NOTE:	msvc compiler bug: the this pointer stored in edi is expected to stay
        //			untouched when calling LDLT_UpdateRowColumn in the if statement
//#if 0
//	if ( !LDLT_UpdateRowColumn( v1, r ) ) {
//#else
        boolean ret = LDLT_UpdateRowColumn(v1, r);
        if (!ret) {
//#endif
            return false;
        }

        // physically remove the row and column
        Update_Decrement(r);

        return true;
    }

    /**
     * ============ idMatX::LDLT_Solve
     *
     * Solve Ax = b with A factored in-place as: LDL' ============
     */
    public void LDLT_Solve(idVecX x, final idVecX b) {
        int i, j;
        double sum;

        assert (numRows == numColumns);
        assert (x.GetSize() >= numRows && b.GetSize() >= numRows);

        // solve L
        for (i = 0; i < numRows; i++) {
            sum = b.p[i];
            for (j = 0; j < i; j++) {
                sum -= this.oGet(i, j) * x.p[j];
            }
            x.p[i] = (float) sum;
        }

        // solve D
        for (i = 0; i < numRows; i++) {
            x.p[i] /= this.oGet(i, i);
        }

        // solve Lt
        for (i = numRows - 2; i >= 0; i--) {
            sum = x.p[i];
            for (j = i + 1; j < numRows; j++) {
                sum -= this.oGet(j, i) * x.p[j];
            }
            x.p[i] = (float) sum;
        }
    }

    /**
     * ============ idMatX::LDLT_Inverse
     *
     * Calculates the inverse of the matrix which is factored in-place as: LDL'
     * ============
     */
    public void LDLT_Inverse(idMatX inv) {
        int i, j;
        idVecX x = new idVecX(), b = new idVecX();

        assert (numRows == numColumns);

        x.SetData(numRows, VECX_ALLOCA(numRows));
        b.SetData(numRows, VECX_ALLOCA(numRows));
        b.Zero();
        inv.SetSize(numRows, numColumns);

        for (i = 0; i < numRows; i++) {

            b.p[i] = 1.0f;
            LDLT_Solve(x, b);
            for (j = 0; j < numRows; j++) {
                inv.oSet(j, i, x.p[j]);
            }
            b.p[i] = 0.0f;
        }
    }

    /*
     ============
     idMatX::LDLT_UnpackFactors

     Unpacks the in-place LDL' factorization.
     ============
     */
    public void LDLT_UnpackFactors(idMatX L, idMatX D) {
        int i, j;

        L.Zero(numRows, numColumns);
        D.Zero(numRows, numColumns);
        for (i = 0; i < numRows; i++) {
            for (j = 0; j < i; j++) {
                L.oSet(i, j, this.oGet(i, j));
            }
            L.oSet(i, i, 1.0f);
            D.oSet(i, i, this.oGet(i, i));
        }
    }

    /*
     ============
     idMatX::LDLT_MultiplyFactors

     Multiplies the factors of the in-place LDL' factorization to form the original matrix.
     ============
     */
    public void LDLT_MultiplyFactors(idMatX m) {
        int r, i, j;
        float[] v;
        double sum;

        v = new float[numRows];
        m.SetSize(numRows, numColumns);

        for (r = 0; r < numRows; r++) {

            // calculate row of matrix
            for (i = 0; i < r; i++) {
                v[i] = this.oGet(r, i) * this.oGet(i, i);
            }
            for (i = 0; i < numColumns; i++) {
                if (i < r) {
                    sum = this.oGet(i, i) * this.oGet(r, i);
                } else if (i == r) {
                    sum = this.oGet(r, r);
                } else {
                    sum = this.oGet(r, r) * this.oGet(i, r);
                }
                for (j = 0; j < i && j < r; j++) {
                    sum += this.oGet(i, j) * v[j];
                }
                m.oSet(r, i, (float) sum);
            }
        }
    }

    public void TriDiagonal_ClearTriangles() {
        int i, j;

        assert (numRows == numColumns);
        for (i = 0; i < numRows - 2; i++) {
            for (j = i + 2; j < numColumns; j++) {
                this.oSet(i, j, 0.0f);
                this.oSet(j, i, 0.0f);
            }
        }
    }

    /**
     * ============ idMatX::TriDiagonal_Solve
     *
     * Solve Ax = b with A being tridiagonal. ============
     */
    public boolean TriDiagonal_Solve(idVecX x, final idVecX b) {
        int i;
        float d;
        idVecX tmp = new idVecX();

        assert (numRows == numColumns);
        assert (x.GetSize() >= numRows && b.GetSize() >= numRows);

        tmp.SetData(numRows, VECX_ALLOCA(numRows));

        d = this.oGet(0, 0);
        if (d == 0.0f) {
            return false;
        }
        d = 1.0f / d;
        x.p[0] = b.p[0] * d;
        for (i = 1; i < numRows; i++) {
            tmp.p[i] = this.oGet(i - 1, i) * d;
            d = this.oGet(i, i) - this.oGet(i, i - 1) * tmp.p[i];
            if (d == 0.0f) {
                return false;
            }
            d = 1.0f / d;
            x.p[i] = (b.p[i] - this.oGet(i, i - 1) * x.p[i - 1]) * d;
        }
        for (i = numRows - 2; i >= 0; i--) {
            x.p[i] -= tmp.p[i + 1] * x.p[i + 1];
        }
        return true;
    }

    /**
     * ============ idMatX::TriDiagonal_Inverse
     *
     * Calculates the inverse of a tri-diagonal matrix. ============
     */
    public void TriDiagonal_Inverse(idMatX inv) {
        int i, j;
        idVecX x = new idVecX(), b = new idVecX();

        assert (numRows == numColumns);

        x.SetData(numRows, VECX_ALLOCA(numRows));
        b.SetData(numRows, VECX_ALLOCA(numRows));
        b.Zero();
        inv.SetSize(numRows, numColumns);

        for (i = 0; i < numRows; i++) {

            b.p[i] = 1.0f;
            TriDiagonal_Solve(x, b);
            for (j = 0; j < numRows; j++) {
                inv.oSet(j, i, x.p[j]);
            }
            b.p[i] = 0.0f;
        }
    }

    /**
     * ============ idMatX::Eigen_SolveSymmetricTriDiagonal
     *
     * Determine eigen values and eigen vectors for a symmetric tri-diagonal
     * matrix. The eigen values are stored in 'eigenValues'. Column i of the
     * original matrix will store the eigen vector corresponding to the
     * eigenValues[i]. The initial matrix has to be symmetric tri-diagonal.
     * ============
     */
    public boolean Eigen_SolveSymmetricTriDiagonal(idVecX eigenValues) {
        int i;
        idVecX subd = new idVecX();

        assert (numRows == numColumns);

        subd.SetData(numRows, VECX_ALLOCA(numRows));
        eigenValues.SetSize(numRows);

        for (i = 0; i < numRows - 1; i++) {
            eigenValues.p[i] = this.oGet(i, i);
            subd.p[i] = this.oGet(i + 1, i);
        }
        eigenValues.p[numRows - 1] = this.oGet(numRows - 1, numRows - 1);

        Identity();

        return QL(eigenValues, subd);
    }
    /*
     ============
     idMatX::Eigen_SolveSymmetric

     Determine eigen values and eigen vectors for a symmetric matrix.
     The eigen values are stored in 'eigenValues'.
     Column i of the original matrix will store the eigen vector corresponding to the eigenValues[i].
     The initial matrix has to be symmetric.
     ============
     */

    public boolean Eigen_SolveSymmetric(idVecX eigenValues) {
        idVecX subd = new idVecX();

        assert (numRows == numColumns);

        subd.SetData(numRows, VECX_ALLOCA(numRows));
        eigenValues.SetSize(numRows);

        HouseholderReduction(eigenValues, subd);
        return QL(eigenValues, subd);
    }

    /**
     * ============ idMatX::Eigen_Solve
     *
     * Determine eigen values and eigen vectors for a square matrix. The eigen
     * values are stored in 'realEigenValues' and 'imaginaryEigenValues'. Column
     * i of the original matrix will store the eigen vector corresponding to the
     * realEigenValues[i] and imaginaryEigenValues[i]. ============
     */
    public boolean Eigen_Solve(idVecX realEigenValues, idVecX imaginaryEigenValues) {
        idMatX H = new idMatX();

        assert (numRows == numColumns);

        realEigenValues.SetSize(numRows);
        imaginaryEigenValues.SetSize(numRows);

        H.oSet(this);

        // reduce to Hessenberg form
        HessenbergReduction(H);

        // reduce Hessenberg to real Schur form
        return HessenbergToRealSchur(H, realEigenValues, imaginaryEigenValues);
    }

    public void Eigen_SortIncreasing(idVecX eigenValues) {
        int i, j, k;
        float min;

        for (i = j = 0; i <= numRows - 2; i++) {
            j = i;
            min = eigenValues.p[j];
            for (k = i + 1; k < numRows; k++) {
                if (eigenValues.p[k] < min) {
                    j = k;
                    min = eigenValues.p[j];
                }
            }
            if (j != i) {
                eigenValues.SwapElements(i, j);
                SwapColumns(i, j);
            }
        }
    }

    public void Eigen_SortDecreasing(idVecX eigenValues) {
        int i, j, k;
        float max;

        for (i = j = 0; i <= numRows - 2; i++) {
            j = i;
            max = eigenValues.p[j];
            for (k = i + 1; k < numRows; k++) {
                if (eigenValues.p[k] > max) {
                    j = k;
                    max = eigenValues.p[j];
                }
            }
            if (j != i) {
                eigenValues.SwapElements(i, j);
                SwapColumns(i, j);
            }
        }
    }

    public static void Test() {
        idMatX original = new idMatX(), m1 = new idMatX(), m2 = new idMatX(), m3 = new idMatX();
        idMatX q1 = new idMatX(), q2 = new idMatX(), r1 = new idMatX(), r2 = new idMatX();
        idVecX v = new idVecX(), w = new idVecX(), u = new idVecX(), c = new idVecX(), d = new idVecX();
        int offset, size;
        int[] index1, index2;

        size = 6;
        original.Random(size, size, 0);
        original = original.oMultiply(original.Transpose());

        index1 = new int[size + 1];
        index2 = new int[size + 1];

        /*
         idMatX::LowerTriangularInverse
         */
        m1.oSet(original);
        m1.ClearUpperTriangle();
        m2.oSet(m1);

        m2.InverseSelf();
        m1.LowerTriangularInverse();

        if (!m1.Compare(m2, 1.e-4f)) {
            idLib.common.Warning("idMatX::LowerTriangularInverse failed");
        }

        /*
         idMatX::UpperTriangularInverse
         */
        m1.oSet(original);
        m1.ClearLowerTriangle();
        m2.oSet(m1);

        m2.InverseSelf();
        m1.UpperTriangularInverse();

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::UpperTriangularInverse failed");
        }

        /*
         idMatX::Inverse_GaussJordan
         */
        m1.oSet(original);

        m1.Inverse_GaussJordan();
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            idLib.common.Warning("idMatX::Inverse_GaussJordan failed");
        }

        /*
         idMatX::Inverse_UpdateRankOne
         */
        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 1);
        v.Random(size, 2);

        // invert m1
        m1.Inverse_GaussJordan();

        // modify and invert m2 
        m2.Update_RankOne(v, w, 1.0f);
        if (!m2.Inverse_GaussJordan()) {
            assert (false);
        }

        // update inverse of m1
        m1.Inverse_UpdateRankOne(v, w, 1.0f);

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::Inverse_UpdateRankOne failed");
        }

        /*
         idMatX::Inverse_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            v.Random(size, 1);
            w.Random(size, 2);
            w.p[offset] = 0.0f;

            // invert m1
            m1.Inverse_GaussJordan();

            // modify and invert m2
            m2.Update_RowColumn(v, w, offset);
            if (!m2.Inverse_GaussJordan()) {
                assert (false);
            }

            // update inverse of m1
            m1.Inverse_UpdateRowColumn(v, w, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::Inverse_UpdateRowColumn failed");
            }
        }

        /*
         idMatX::Inverse_UpdateIncrement
         */
        m1.oSet(original);
        m2.oSet(original);

        v.Random(size + 1, 1);
        w.Random(size + 1, 2);
        w.p[size] = 0.0f;

        // invert m1
        m1.Inverse_GaussJordan();

        // modify and invert m2 
        m2.Update_Increment(v, w);
        if (!m2.Inverse_GaussJordan()) {
            assert (false);
        }

        // update inverse of m1
        m1.Inverse_UpdateIncrement(v, w);

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::Inverse_UpdateIncrement failed");
        }

        /*
         idMatX::Inverse_UpdateDecrement
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            w.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
                w.p[i] = original.oGet(offset, i);
            }

            // invert m1
            m1.Inverse_GaussJordan();

            // modify and invert m2
            m2.Update_Decrement(offset);
            if (!m2.Inverse_GaussJordan()) {
                assert (false);
            }

            // update inverse of m1
            m1.Inverse_UpdateDecrement(v, w, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::Inverse_UpdateDecrement failed");

            }
        }

        /*
         idMatX::LU_Factor
         */
        m1.oSet(original);

        m1.LU_Factor(null);	// no pivoting
        m1.LU_UnpackFactors(m2, m3);
        m1.oSet(m2.oMultiply(m3));

        if (!original.Compare(m1, 1e-4f)) {
            idLib.common.Warning("idMatX::LU_Factor failed");
        }

        /*
         idMatX::LU_UpdateRankOne
         */
        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 1);
        v.Random(size, 2);

        // factor m1
        m1.LU_Factor(index1);

        // modify and factor m2 
        m2.Update_RankOne(v, w, 1.0f);
        if (!m2.LU_Factor(index2)) {
            assert (false);
        }
        m2.LU_MultiplyFactors(m3, index2);
        m2.oSet(m3);

        // update factored m1
        m1.LU_UpdateRankOne(v, w, 1.0f, index1);
        m1.LU_MultiplyFactors(m3, index1);
        m1.oSet(m3);

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::LU_UpdateRankOne failed");

        }

        /*
         idMatX::LU_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            v.Random(size, 1);
            w.Random(size, 2);
            w.p[offset] = 0.0f;

            // factor m1
            m1.LU_Factor(index1);

            // modify and factor m2
            m2.Update_RowColumn(v, w, offset);
            if (!m2.LU_Factor(index2)) {
                assert (false);
            }
            m2.LU_MultiplyFactors(m3, index2);
            m2.oSet(m3);

            // update m1
            m1.LU_UpdateRowColumn(v, w, offset, index1);
            m1.LU_MultiplyFactors(m3, index1);
            m1.oSet(m3);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::LU_UpdateRowColumn failed");
            }
        }

        /*
         idMatX::LU_UpdateIncrement
         */
        m1.oSet(original);
        m2.oSet(original);

        v.Random(size + 1, 1);
        w.Random(size + 1, 2);
        w.p[size] = 0.0f;

        // factor m1
        m1.LU_Factor(index1);

        // modify and factor m2 
        m2.Update_Increment(v, w);
        if (!m2.LU_Factor(index2)) {
            assert (false);
        }
        m2.LU_MultiplyFactors(m3, index2);
        m2.oSet(m3);

        // update factored m1
        m1.LU_UpdateIncrement(v, w, index1);
        m1.LU_MultiplyFactors(m3, index1);
        m1.oSet(m3);

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::LU_UpdateIncrement failed");
        }

        /*
         idMatX::LU_UpdateDecrement
         */
        for (offset = 0; offset < size; offset++) {
            m1 = new idMatX();//TODO:check m1=m3, m2=m2 refs!!!
            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            w.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
                w.p[i] = original.oGet(offset, i);
            }

            // factor m1
            m1.LU_Factor(index1);

            // modify and factor m2
            m2.Update_Decrement(offset);
            if (!m2.LU_Factor(index2)) {
                assert (false);
            }
            m2.LU_MultiplyFactors(m3, index2);
            m2.oSet(m3);

            u.SetSize(6);
            for (int i = 0; i < size; i++) {
                u.p[i] = original.oGet(index1[offset], i);
            }

            // update factors of m1
            m1.LU_UpdateDecrement(v, w, u, offset, index1);
            m1.LU_MultiplyFactors(m3, index1);
            m1.oSet(m3);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::LU_UpdateDecrement failed");
            }
        }

        /*
         idMatX::LU_Inverse
         */
        m2.oSet(original);

        m2.LU_Factor(null);
        m2.LU_Inverse(m1, null);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            idLib.common.Warning("idMatX::LU_Inverse failed");
            //System.exit(9);
        }

        /*
         idMatX::QR_Factor
         */
        c.SetSize(size);
        d.SetSize(size);

        m1.oSet(original);

        m1.QR_Factor(c, d);
        m1.QR_UnpackFactors(q1, r1, c, d);
        m1.oSet(q1.oMultiply(r1));

        if (!original.Compare(m1, 1e-4f)) {
            idLib.common.Warning("idMatX::QR_Factor failed");
        }

        /*
         idMatX::QR_UpdateRankOne
         */
        c.SetSize(size);
        d.SetSize(size);

        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 0);
        v.oSet(w);

        // factor m1
        m1.QR_Factor(c, d);
        m1.QR_UnpackFactors(q1, r1, c, d);

        // modify and factor m2 
        m2.Update_RankOne(v, w, 1.0f);
        if (!m2.QR_Factor(c, d)) {
            assert (false);
        }
        m2.QR_UnpackFactors(q2, r2, c, d);
        m2 = q2.oMultiply(r2);

        // update factored m1
        q1.QR_UpdateRankOne(r1, v, w, 1.0f);
        m1 = q1.oMultiply(r1);

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::QR_UpdateRankOne failed");
        }

        /*
         idMatX::QR_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            c.SetSize(size);
            d.SetSize(size);

            m1.oSet(original);
            m2.oSet(original);

            v.Random(size, 1);
            w.Random(size, 2);
            w.p[offset] = 0.0f;

            // factor m1
            m1.QR_Factor(c, d);
            m1.QR_UnpackFactors(q1, r1, c, d);

            // modify and factor m2
            m2.Update_RowColumn(v, w, offset);
            if (!m2.QR_Factor(c, d)) {
                assert (false);
            }
            m2.QR_UnpackFactors(q2, r2, c, d);
            m2 = q2.oMultiply(r2);

            // update m1
            q1.QR_UpdateRowColumn(r1, v, w, offset);
            m1 = q1.oMultiply(r1);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::QR_UpdateRowColumn failed");

            }
        }

        /*
         idMatX::QR_UpdateIncrement
         */
        c.SetSize(size + 1);
        d.SetSize(size + 1);

        m1.oSet(original);
        m2.oSet(original);

        v.Random(size + 1, 1);
        w.Random(size + 1, 2);
        w.p[size] = 0.0f;

        // factor m1
        m1.QR_Factor(c, d);
        m1.QR_UnpackFactors(q1, r1, c, d);

        // modify and factor m2 
        m2.Update_Increment(v, w);
        if (!m2.QR_Factor(c, d)) {
            assert (false);
        }
        m2.QR_UnpackFactors(q2, r2, c, d);
        m2 = q2.oMultiply(r2);

        // update factored m1
        q1.QR_UpdateIncrement(r1, v, w);
        m1 = q1.oMultiply(r1);

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::QR_UpdateIncrement failed");
        }

        /*
         idMatX::QR_UpdateDecrement
         */
        for (offset = 0; offset < size; offset++) {
            c.SetSize(size + 1);
            d.SetSize(size + 1);

            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            w.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
                w.p[i] = original.oGet(offset, i);
            }

            // factor m1
            m1.QR_Factor(c, d);
            m1.QR_UnpackFactors(q1, r1, c, d);

            // modify and factor m2
            m2.Update_Decrement(offset);
            if (!m2.QR_Factor(c, d)) {
                assert (false);
            }
            m2.QR_UnpackFactors(q2, r2, c, d);
            m2 = q2.oMultiply(r2);

            // update factors of m1
            q1.QR_UpdateDecrement(r1, v, w, offset);
            m1.oSet(q1.oMultiply(r1));

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::QR_UpdateDecrement failed");
            }
        }

        /*
         idMatX::QR_Inverse
         */
        m2.oSet(original);

        m2.QR_Factor(c, d);
        m2.QR_Inverse(m1, c, d);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            idLib.common.Warning("idMatX::QR_Inverse failed");
        }

        /*
         idMatX::SVD_Factor
         */
        m1.oSet(original);
        m3.Zero(size, size);
        w.Zero(size);

        m1.SVD_Factor(w, m3);
        m2.Diag(w);
        m3.TransposeSelf();
        m1.oSet(m1.oMultiply(m2).oMultiply(m3));

        if (!original.Compare(m1, 1e-4f)) {
            idLib.common.Warning("idMatX::SVD_Factor failed");
        }

        /*
         idMatX::SVD_Inverse
         */
        m2.oSet(original);

        m2.SVD_Factor(w, m3);
        m2.SVD_Inverse(m1, w, m3);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            idLib.common.Warning("idMatX::SVD_Inverse failed");
        }

        /*
         idMatX::Cholesky_Factor
         */
        m1.oSet(original);

        m1.Cholesky_Factor();
        m1.Cholesky_MultiplyFactors(m2);

        if (!original.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::Cholesky_Factor failed");
        }

        /*
         idMatX::Cholesky_UpdateRankOne
         */
        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 0);

        // factor m1
        m1.Cholesky_Factor();
        m1.ClearUpperTriangle();

        // modify and factor m2 
        m2.Update_RankOneSymmetric(w, 1.0f);
        if (!m2.Cholesky_Factor()) {
            assert (false);
        }
        m2.ClearUpperTriangle();

        // update factored m1
        m1.Cholesky_UpdateRankOne(w, 1.0f, 0);

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::Cholesky_UpdateRankOne failed");
        }

        /*
         idMatX::Cholesky_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            // factor m1
            m1.Cholesky_Factor();
            m1.ClearUpperTriangle();

            int pdtable[] = {1, 0, 1, 0, 0, 0};
            w.Random(size, pdtable[offset]);
            w.oMulSet(0.1f);

            // modify and factor m2
            m2.Update_RowColumnSymmetric(w, offset);
            if (!m2.Cholesky_Factor()) {
                assert (false);
            }
            m2.ClearUpperTriangle();

            // update m1
            m1.Cholesky_UpdateRowColumn(w, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::Cholesky_UpdateRowColumn failed");
            }
        }

        /*
         idMatX::Cholesky_UpdateIncrement
         */
        m1.Random(size + 1, size + 1, 0);
        m3.oSet(m1.oMultiply(m1.Transpose()));

        m1.SquareSubMatrix(m3, size);
        m2.oSet(m1);

        w.SetSize(size + 1);
        for (int i = 0; i < size + 1; i++) {
            w.p[i] = m3.oGet(size, i);
        }

        // factor m1
        m1.Cholesky_Factor();

        // modify and factor m2 
        m2.Update_IncrementSymmetric(w);
        if (!m2.Cholesky_Factor()) {
            assert (false);
        }

        // update factored m1
        m1.Cholesky_UpdateIncrement(w);

        m1.ClearUpperTriangle();
        m2.ClearUpperTriangle();

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::Cholesky_UpdateIncrement failed");
        }

        /*
         idMatX::Cholesky_UpdateDecrement
         */
        for (offset = 0; offset < size; offset += size - 1) {
            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
            }

            // factor m1
            m1.Cholesky_Factor();

            // modify and factor m2
            m2.Update_Decrement(offset);
            if (!m2.Cholesky_Factor()) {
                assert (false);
            }

            // update factors of m1
            m1.Cholesky_UpdateDecrement(v, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::Cholesky_UpdateDecrement failed");
            }
        }

        /*
         idMatX::Cholesky_Inverse
         */
        m2.oSet(original);

        m2.Cholesky_Factor();
        m2.Cholesky_Inverse(m1);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            idLib.common.Warning("idMatX::Cholesky_Inverse failed");
        }

        /*
         idMatX::LDLT_Factor
         */
        m1.oSet(original);

        m1.LDLT_Factor();
        m1.LDLT_MultiplyFactors(m2);

        if (!original.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::LDLT_Factor failed");
        }

        m1.LDLT_UnpackFactors(m2, m3);
        m2 = m2.oMultiply(m3).oMultiply(m2.Transpose());

        if (!original.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::LDLT_Factor failed");
        }

        /*
         idMatX::LDLT_UpdateRankOne
         */
        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 0);

        // factor m1
        m1.LDLT_Factor();
        m1.ClearUpperTriangle();

        // modify and factor m2 
        m2.Update_RankOneSymmetric(w, 1.0f);
        if (!m2.LDLT_Factor()) {
            assert (false);
        }
        m2.ClearUpperTriangle();

        // update factored m1
        m1.LDLT_UpdateRankOne(w, 1.0f, 0);

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::LDLT_UpdateRankOne failed");
        }

        /*
         idMatX::LDLT_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            w.Random(size, 0);

            // factor m1
            m1.LDLT_Factor();
            m1.ClearUpperTriangle();

            // modify and factor m2
            m2.Update_RowColumnSymmetric(w, offset);
            if (!m2.LDLT_Factor()) {
                assert (false);
            }
            m2.ClearUpperTriangle();

            // update m1
            m1.LDLT_UpdateRowColumn(w, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::LDLT_UpdateRowColumn failed");
            }
        }

        /*
         idMatX::LDLT_UpdateIncrement
         */
        m1.Random(size + 1, size + 1, 0);
        m3 = m1.oMultiply(m1.Transpose());

        m1.SquareSubMatrix(m3, size);
        m2.oSet(m1);

        w.SetSize(size + 1);
        for (int i = 0; i < size + 1; i++) {
            w.p[i] = m3.oGet(size, i);
        }

        // factor m1
        m1.LDLT_Factor();

        // modify and factor m2 
        m2.Update_IncrementSymmetric(w);
        if (!m2.LDLT_Factor()) {
            assert (false);
        }

        // update factored m1
        m1.LDLT_UpdateIncrement(w);

        m1.ClearUpperTriangle();
        m2.ClearUpperTriangle();

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::LDLT_UpdateIncrement failed");
        }

        /*
         idMatX::LDLT_UpdateDecrement
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
            }

            // factor m1
            m1.LDLT_Factor();

            // modify and factor m2
            m2.Update_Decrement(offset);
            if (!m2.LDLT_Factor()) {
                assert (false);
            }

            // update factors of m1
            m1.LDLT_UpdateDecrement(v, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                idLib.common.Warning("idMatX::LDLT_UpdateDecrement failed");
            }
        }

        /*
         idMatX::LDLT_Inverse
         */
        m2.oSet(original);

        m2.LDLT_Factor();
        m2.LDLT_Inverse(m1);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            idLib.common.Warning("idMatX::LDLT_Inverse failed");
        }

        /*
         idMatX::Eigen_SolveSymmetricTriDiagonal
         */
        m3.oSet(original);
        m3.TriDiagonal_ClearTriangles();
        m1.oSet(m3);

        v.SetSize(size);

        m1.Eigen_SolveSymmetricTriDiagonal(v);

        m3.TransposeMultiply(m2, m1);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m1.oMulSet(i, j, v.p[j]);
            }
        }

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::Eigen_SolveSymmetricTriDiagonal failed");
        }

        /*
         idMatX::Eigen_SolveSymmetric
         */
        m3.oSet(original);
        m1.oSet(m3);

        v.SetSize(size);

        m1.Eigen_SolveSymmetric(v);

        m3.TransposeMultiply(m2, m1);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m1.oMulSet(i, j, v.p[j]);
            }
        }

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::Eigen_SolveSymmetric failed");
        }

        /*
         idMatX::Eigen_Solve
         */
        m3.oSet(original);
        m1.oSet(m3);

        v.SetSize(size);
        w.SetSize(size);

        m1.Eigen_Solve(v, w);

        m3.TransposeMultiply(m2, m1);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m1.oMulSet(i, j, v.p[j]);
            }
        }

        if (!m1.Compare(m2, 1e-4f)) {
            idLib.common.Warning("idMatX::Eigen_Solve failed");
        }
    }

    private void SetTempSize(int rows, int columns) {
        int newSize;

        newSize = (rows * columns + 3) & ~3;
        assert (newSize < MATX_MAX_TEMP);
        if (idMatX.tempIndex + newSize > MATX_MAX_TEMP) {
            idMatX.tempIndex = 0;
        }
//            mat = idMatX::tempPtr + idMatX::tempIndex;
        mat = new float[newSize];
        idMatX.tempIndex += newSize;
        alloced = newSize;
        numRows = rows;
        numColumns = columns;
        MATX_CLEAREND();
    }

    private float DeterminantGeneric() {
        int[] index;
        float[] det = new float[1];
        idMatX tmp = new idMatX();

        index = new int[numRows];
        tmp.SetData(numRows, numColumns, MATX_ALLOCA(numRows * numColumns));
        tmp = this;

        if (!tmp.LU_Factor(index, det)) {
            return 0.0f;
        }

        return det[0];
    }

    private boolean InverseSelfGeneric() {
        int i, j;
        int[] index;
        idMatX tmp = new idMatX();
        idVecX x = new idVecX(), b = new idVecX();

        index = new int[numRows];
        tmp.SetData(numRows, numColumns, MATX_ALLOCA(numRows * numColumns));
        tmp = this;

        if (!tmp.LU_Factor(index)) {
            return false;
        }

        x.SetData(numRows, VECX_ALLOCA(numRows));
        b.SetData(numRows, VECX_ALLOCA(numRows));
        b.Zero();

        for (i = 0; i < numRows; i++) {

            b.p[i] = 1.0f;
            tmp.LU_Solve(x, b, index);
            for (j = 0; j < numRows; j++) {
                this.oSet(j, i, x.p[j]);
            }
            b.p[i] = 0.0f;
        }
        return true;
    }

    /**
     * ============ idMatX::QR_Rotate
     *
     * Performs a Jacobi rotation on the rows i and i+1 of the unpacked QR
     * factors. ============
     */
    private void QR_Rotate(idMatX R, int i, float a, float b) {
        int j;
        float f, c, s, w, y;

        if (a == 0.0f) {
            c = 0.0f;
            s = (b >= 0.0f) ? 1.0f : -1.0f;
        } else if (idMath.Fabs(a) > idMath.Fabs(b)) {
            f = b / a;
            c = idMath.Fabs(1.0f / idMath.Sqrt(1.0f + f * f));
            if (a < 0.0f) {
                c = -c;
            }
            s = f * c;
        } else {
            f = a / b;
            s = idMath.Fabs(1.0f / idMath.Sqrt(1.0f + f * f));
            if (b < 0.0f) {
                s = -s;
            }
            c = f * s;
        }
        for (j = i; j < numRows; j++) {
            y = R.oGet(i, j);
            w = R.oGet(i + 1, j);
            R.oSet(i, j, c * y - s * w);
            R.oSet(i + 1, j, s * y + c * w);
        }
        for (j = 0; j < numRows; j++) {
            y = this.oGet(j, i);
            w = this.oGet(j, i + 1);
            this.oSet(j, i, c * y - s * w);
            this.oSet(j, i + 1, s * y + c * w);
        }
    }

    /**
     * ============ idMatX::Pythag
     *
     * Computes (a^2 + b^2)^1/2 without underflow or overflow. ============
     */
    private float Pythag(float a, float b) {
        float at, bt, ct;

        at = idMath.Fabs(a);
        bt = idMath.Fabs(b);
        if (at > bt) {
            ct = bt / at;
            return at * idMath.Sqrt(1.0f + ct * ct);
        } else {
            if (bt != 0) {
                ct = at / bt;
                return bt * idMath.Sqrt(1.0f + ct * ct);
            } else {
                return 0.0f;
            }
        }
    }

    private void SVD_BiDiag(idVecX w, idVecX rv1, float[] anorm) {
        int i, j, k, l;
        double f, h, r, g, s, scale;

        anorm[0] = 0.0f;
        g = s = scale = 0.0f;
        for (i = 0; i < numColumns; i++) {
            l = i + 1;
            rv1.p[i] = (float) (scale * g);
            g = s = scale = 0.0f;
            if (i < numRows) {
                for (k = i; k < numRows; k++) {
                    scale += idMath.Fabs(this.oGet(k, i));
                }
                if (scale != 0.0f) {
                    for (k = i; k < numRows; k++) {
                        this.oDivSet(k, i, scale);
                        s += this.oGet(k, i) * this.oGet(k, i);
                    }
                    f = this.oGet(i, i);
                    g = idMath.Sqrt((float) s);
                    if (f >= 0.0f) {
                        g = -g;
                    }
                    h = f * g - s;
                    this.oSet(i, i, (float) (f - g));
                    if (i != (numColumns - 1)) {
                        for (j = l; j < numColumns; j++) {
                            for (s = 0.0f, k = i; k < numRows; k++) {
                                s += this.oGet(k, i) * this.oGet(k, j);
                            }
                            f = s / h;
                            for (k = i; k < numRows; k++) {
                                this.oPluSet(k, j, f * this.oGet(k, i));
                            }
                        }
                    }
                    for (k = i; k < numRows; k++) {
                        this.oMulSet(k, i, scale);
                    }
                }
            }
            w.p[i] = (float) (scale * g);
            g = s = scale = 0.0f;
            if (i < numRows && i != (numColumns - 1)) {
                for (k = l; k < numColumns; k++) {
                    scale += idMath.Fabs(this.oGet(i, k));
                }
                if (scale != 0.0f) {
                    for (k = l; k < numColumns; k++) {
                        this.oDivSet(i, k, scale);//TODO:add oDivSit
                        s += this.oGet(i, k) * this.oGet(i, k);
                    }
                    f = this.oGet(i, l);
                    g = idMath.Sqrt((float) s);
                    if (f >= 0.0f) {
                        g = -g;
                    }
                    h = 1.0f / (f * g - s);
                    this.oSet(i, l, (float) (f - g));
                    for (k = l; k < numColumns; k++) {
                        rv1.p[k] = (float) (this.oGet(i, k) * h);
                    }
                    if (i != (numRows - 1)) {
                        for (j = l; j < numRows; j++) {
                            for (s = 0.0f, k = l; k < numColumns; k++) {
                                s += this.oGet(j, k) * this.oGet(i, k);
                            }
                            for (k = l; k < numColumns; k++) {
                                this.oPluSet(j, k, s * rv1.p[k]);
                            }
                        }
                    }
                    for (k = l; k < numColumns; k++) {
                        this.oMulSet(i, k, scale);
                    }
                }
            }
            r = idMath.Fabs(w.p[i]) + idMath.Fabs(rv1.p[i]);
            if (r > anorm[0]) {
                anorm[0] = (float) r;
            }
        }
    }

    private void SVD_InitialWV(idVecX w, idMatX V, idVecX rv1) {
        int i, j, k, l;
        double f, g, s;

        g = 0.0f;
        for (i = (numColumns - 1); i >= 0; i--) {
            l = i + 1;
            if (i < (numColumns - 1)) {
                if (g != 0) {
                    for (j = l; j < numColumns; j++) {
                        V.oSet(j, i, (float) ((this.oGet(i, j) / this.oGet(i, l)) / g));
                    }
                    // double division to reduce underflow
                    for (j = l; j < numColumns; j++) {
                        for (s = 0.0f, k = l; k < numColumns; k++) {
                            s += this.oGet(i, k) * V.oGet(k, j);
                        }
                        for (k = l; k < numColumns; k++) {
                            V.oPluSet(k, j, s * V.oGet(k, i));
                        }
                    }
                }
                for (j = l; j < numColumns; j++) {
                    V.oSet(j, i, V.oSet(i, j, 0.0f));
                }
            }
            V.oSet(i, i, 1.0f);
            g = rv1.p[i];
        }
        for (i = numColumns - 1; i >= 0; i--) {
            l = i + 1;
            g = w.p[i];
            if (i < (numColumns - 1)) {
                for (j = l; j < numColumns; j++) {
                    this.oSet(i, j, 0.0f);
                }
            }
            if (g != 0) {
                g = 1.0f / g;
                if (i != (numColumns - 1)) {
                    for (j = l; j < numColumns; j++) {
                        for (s = 0.0f, k = l; k < numRows; k++) {
                            s += this.oGet(k, i) * this.oGet(k, j);
                        }
                        f = (s / this.oGet(i, i) * g);
                        for (k = i; k < numRows; k++) {
                            this.oPluSet(k, j, f * this.oGet(k, i));
                        }
                    }
                }
                for (j = i; j < numRows; j++) {
                    this.oMulSet(j, i, g);
                }
            } else {
                for (j = i; j < numRows; j++) {
                    this.oSet(j, i, 0.0f);
                }
            }
            this.oPluSet(i, i, 1.0f);
        }
    }

    /**
     * ============ idMatX::HouseholderReduction
     *
     * Householder reduction to symmetric tri-diagonal form. The original matrix
     * is replaced by an orthogonal matrix effecting the accumulated householder
     * transformations. The diagonal elements of the diagonal matrix are stored
     * in diag. The off-diagonal elements of the diagonal matrix are stored in
     * subd. The initial matrix has to be symmetric. ============
     */
    private void HouseholderReduction(idVecX diag, idVecX subd) {
        int i0, i1, i2, i3;
        float h, f, g, invH, halfFdivH, scale, invScale, sum;

        assert (numRows == numColumns);

        diag.SetSize(numRows);
        subd.SetSize(numRows);

        for (i0 = numRows - 1, i3 = numRows - 2; i0 >= 1; i0--, i3--) {
            h = 0.0f;
            scale = 0.0f;

            if (i3 > 0) {
                for (i2 = 0; i2 <= i3; i2++) {
                    scale += idMath.Fabs(this.oGet(i0, i2));
                }
                if (scale == 0) {
                    subd.p[i0] = this.oGet(i0, i3);
                } else {
                    invScale = 1.0f / scale;
                    for (i2 = 0; i2 <= i3; i2++) {
                        this.oMulSet(i0, i2, invScale);
                        h += this.oGet(i0, i2) * this.oGet(i0, i2);
                    }
                    f = this.oGet(i0, i3);
                    g = idMath.Sqrt(h);
                    if (f > 0.0f) {
                        g = -g;
                    }
                    subd.p[i0] = scale * g;
                    h -= f * g;
                    this.oSet(i0, i3, f - g);
                    f = 0.0f;
                    invH = 1.0f / h;
                    for (i1 = 0; i1 <= i3; i1++) {
                        this.oSet(i1, i0, this.oGet(i0, i1) * invH);
                        g = 0.0f;
                        for (i2 = 0; i2 <= i1; i2++) {
                            g += this.oGet(i1, i2) * this.oGet(i0, i2);
                        }
                        for (i2 = i1 + 1; i2 <= i3; i2++) {
                            g += this.oGet(i2, i1) * this.oGet(i0, i2);
                        }
                        subd.p[i1] = g * invH;
                        f += subd.p[i1] * this.oGet(i0, i1);
                    }
                    halfFdivH = 0.5f * f * invH;
                    for (i1 = 0; i1 <= i3; i1++) {
                        f = this.oGet(i0, i1);
                        g = subd.p[i1] - halfFdivH * f;
                        subd.p[i1] = g;
                        for (i2 = 0; i2 <= i1; i2++) {
                            this.oMinSet(i1, i2, f * subd.p[i2] + g * this.oGet(i0, i2));
                        }
                    }
                }
            } else {
                subd.p[i0] = this.oGet(i0, i3);
            }

            diag.p[i0] = h;
        }

        diag.p[0] = 0.0f;
        subd.p[0] = 0.0f;
        for (i0 = 0, i3 = -1; i0 <= numRows - 1; i0++, i3++) {
            if (diag.p[i0] != 0) {
                for (i1 = 0; i1 <= i3; i1++) {
                    sum = 0.0f;
                    for (i2 = 0; i2 <= i3; i2++) {
                        sum += this.oGet(i0, i2) * this.oGet(i2, i1);
                    }
                    for (i2 = 0; i2 <= i3; i2++) {
                        this.oMinSet(i2, i1, sum * this.oGet(i2, i0));
                    }
                }
            }
            diag.p[i0] = this.oGet(i0, i0);
            this.oSet(i0, i0, 1.0f);
            for (i1 = 0; i1 <= i3; i1++) {
                this.oSet(i1, i0, 0.0f);
                this.oSet(i0, i1, 0.0f);
            }
        }

        // re-order
        for (i0 = 1, i3 = 0; i0 < numRows; i0++, i3++) {
            subd.p[i3] = subd.p[i0];
        }
        subd.p[numRows - 1] = 0.0f;
    }

    /**
     * ============ idMatX::QL
     *
     * QL algorithm with implicit shifts to determine the eigenvalues and
     * eigenvectors of a symmetric tri-diagonal matrix. diag contains the
     * diagonal elements of the symmetric tri-diagonal matrix on input and is
     * overwritten with the eigenvalues. subd contains the off-diagonal elements
     * of the symmetric tri-diagonal matrix and is destroyed. This matrix has to
     * be either the identity matrix to determine the eigenvectors for a
     * symmetric tri-diagonal matrix, or the matrix returned by the Householder
     * reduction to determine the eigenvalues for the original symmetric matrix.
     * ============
     */
    private boolean QL(idVecX diag, idVecX subd) {
        final int maxIter = 32;
        int i0, i1, i2, i3;
        float a, b, f, g, r, p, s, c;

        assert (numRows == numColumns);

        for (i0 = 0; i0 < numRows; i0++) {
            for (i1 = 0; i1 < maxIter; i1++) {
                for (i2 = i0; i2 <= numRows - 2; i2++) {
                    a = idMath.Fabs(diag.p[i2]) + idMath.Fabs(diag.p[i2 + 1]);
                    if (idMath.Fabs(subd.p[i2]) + a == a) {
                        break;
                    }
                }
                if (i2 == i0) {
                    break;
                }

                g = (diag.p[i0 + 1] - diag.p[i0]) / (2.0f * subd.p[i0]);
                r = idMath.Sqrt(g * g + 1.0f);
                if (g < 0.0f) {
                    g = diag.p[i2] - diag.p[i0] + subd.p[i0] / (g - r);
                } else {
                    g = diag.p[i2] - diag.p[i0] + subd.p[i0] / (g + r);
                }
                s = 1.0f;
                c = 1.0f;
                p = 0.0f;
                for (i3 = i2 - 1; i3 >= i0; i3--) {
                    f = s * subd.p[i3];
                    b = c * subd.p[i3];
                    if (idMath.Fabs(f) >= idMath.Fabs(g)) {
                        c = g / f;
                        r = idMath.Sqrt(c * c + 1.0f);
                        subd.p[i3 + 1] = f * r;
                        s = 1.0f / r;
                        c *= s;
                    } else {
                        s = f / g;
                        r = idMath.Sqrt(s * s + 1.0f);
                        subd.p[i3 + 1] = g * r;
                        c = 1.0f / r;
                        s *= c;
                    }
                    g = diag.p[i3 + 1] - p;
                    r = (diag.p[i3] - g) * s + 2.0f * b * c;
                    p = s * r;
                    diag.p[i3 + 1] = g + p;
                    g = c * r - b;

                    for (int i4 = 0; i4 < numRows; i4++) {
                        f = this.oGet(i4, i3 + 1);
                        this.oSet(i4, i3 + 1, s * this.oGet(i4, i3) + c * f);
                        this.oSet(i4, i3, c * this.oGet(i4, i3) - s * f);
                    }
                }
                diag.p[i0] -= p;
                subd.p[i0] = g;
                subd.p[i2] = 0.0f;
            }
            if (i1 == maxIter) {
                return false;
            }
        }
        return true;
    }

    /*
     ============
     idMatX::HessenbergReduction

     Reduction to Hessenberg form.
     ============
     */
    private void HessenbergReduction(idMatX H) {
        int i, j, m;
        int low = 0;
        int high = numRows - 1;
        float scale, f, g, h;
        idVecX v = new idVecX();

        v.SetData(numRows, VECX_ALLOCA(numRows));

        for (m = low + 1; m <= high - 1; m++) {

            scale = 0.0f;
            for (i = m; i <= high; i++) {
                scale = scale + idMath.Fabs(H.oGet(i, m - 1));
            }
            if (scale != 0.0f) {

                // compute Householder transformation.
                h = 0.0f;
                for (i = high; i >= m; i--) {
                    v.p[i] = H.oGet(i, m - 1) / scale;
                    h += v.p[i] * v.p[i];
                }
                g = idMath.Sqrt(h);
                if (v.p[m] > 0.0f) {
                    g = -g;
                }
                h = h - v.p[m] * g;
                v.p[m] = v.p[m] - g;

                // apply Householder similarity transformation
                // H = (I-u*u'/h)*H*(I-u*u')/h)
                for (j = m; j < numRows; j++) {
                    f = 0.0f;
                    for (i = high; i >= m; i--) {
                        f += v.p[i] * H.oGet(i, j);
                    }
                    f = f / h;
                    for (i = m; i <= high; i++) {
                        H.oMinSet(i, j, f * v.p[i]);
                    }
                }

                for (i = 0; i <= high; i++) {
                    f = 0.0f;
                    for (j = high; j >= m; j--) {
                        f += v.p[j] * H.oGet(i, j);
                    }
                    f = f / h;
                    for (j = m; j <= high; j++) {
                        H.oMinSet(i, j, f * v.p[j]);
                    }
                }
                v.p[m] = scale * v.p[m];
                H.oSet(m, m - 1, scale * g);
            }
        }

        // accumulate transformations
        Identity();
        for (m = high - 1; m >= low + 1; m--) {
            if (H.oGet(m, m - 1) != 0.0f) {
                for (i = m + 1; i <= high; i++) {
                    v.p[i] = H.oGet(i, m - 1);
                }
                for (j = m; j <= high; j++) {
                    g = 0.0f;
                    for (i = m; i <= high; i++) {
                        g += v.p[i] * this.oGet(i, j);
                    }
                    // float division to avoid possible underflow
                    g = (g / v.p[m]) / H.oGet(m, m - 1);
                    for (i = m; i <= high; i++) {
                        this.oPluSet(i, j, g * v.p[i]);
                    }
                }
            }
        }
    }

    /**
     * ============ idMatX::ComplexDivision
     *
     * Complex scalar division. ============
     */
    private void ComplexDivision(float xr, float xi, float yr, float yi, float[] cdivr, float[] cdivi) {
        float r, d;
        if (idMath.Fabs(yr) > idMath.Fabs(yi)) {
            r = yi / yr;
            d = yr + r * yi;
            cdivr[0] = (xr + r * xi) / d;
            cdivi[0] = (xi - r * xr) / d;
        } else {
            r = yr / yi;
            d = yi + r * yr;
            cdivr[0] = (r * xr + xi) / d;
            cdivi[0] = (r * xi - xr) / d;
        }
    }

    /**
     * ============ idMatX::HessenbergToRealSchur
     *
     * Reduction from Hessenberg to real Schur form. ============
     */
    private boolean HessenbergToRealSchur(idMatX H, idVecX realEigenValues, idVecX imaginaryEigenValues) {
        int i, j, k;
        int n = numRows - 1;
        int low = 0;
        int high = numRows - 1;
        float eps = 2e-16f, exshift = 0.0f;
        float p = 0.0f, q = 0.0f, r = 0.0f, s = 0.0f, z = 0.0f, t, w, x, y;

        // store roots isolated by balanc and compute matrix norm
        float norm = 0.0f;
        for (i = 0; i < numRows; i++) {
            if (i < low || i > high) {
                realEigenValues.p[i] = H.oGet(i, i);
                imaginaryEigenValues.p[i] = 0.0f;
            }
            for (j = Lib.Max(i - 1, 0); j < numRows; j++) {
                norm = norm + idMath.Fabs(H.oGet(i, j));
            }
        }

        int iter = 0;
        while (n >= low) {

            // look for single small sub-diagonal element
            int l = n;
            while (l > low) {
                s = idMath.Fabs(H.oGet(l - 1, l - 1)) + idMath.Fabs(H.oGet(l, l));
                if (s == 0.0f) {
                    s = norm;
                }
                if (idMath.Fabs(H.oGet(l, l - 1)) < eps * s) {
                    break;
                }
                l--;
            }

            // check for convergence
            if (l == n) {			// one root found
                H.oPluSet(n, n, exshift);
                realEigenValues.p[n] = H.oGet(n, n);
                imaginaryEigenValues.p[n] = 0.0f;
                n--;
                iter = 0;
            } else if (l == n - 1) {	// two roots found
                w = H.oGet(n, n - 1) * H.oGet(n - 1, n);
                p = (H.oGet(n - 1, n - 1) - H.oGet(n, n)) / 2.0f;
                q = p * p + w;
                z = idMath.Sqrt(idMath.Fabs(q));
                H.oPluSet(n, n, exshift);
                H.oPluSet(n - 1, n - 1, exshift);
                x = H.oGet(n, n);

                if (q >= 0.0f) {		// real pair
                    if (p >= 0.0f) {
                        z = p + z;
                    } else {
                        z = p - z;
                    }
                    realEigenValues.p[n - 1] = x + z;
                    realEigenValues.p[n] = realEigenValues.p[n - 1];
                    if (z != 0.0f) {
                        realEigenValues.p[n] = x - w / z;
                    }
                    imaginaryEigenValues.p[n - 1] = 0.0f;
                    imaginaryEigenValues.p[n] = 0.0f;
                    x = H.oGet(n, n - 1);
                    s = idMath.Fabs(x) + idMath.Fabs(z);
                    p = x / s;
                    q = z / s;
                    r = idMath.Sqrt(p * p + q * q);
                    p = p / r;
                    q = q / r;

                    // modify row
                    for (j = n - 1; j < numRows; j++) {
                        z = H.oGet(n - 1, j);
                        H.oSet(n - 1, j, q * z + p * H.oGet(n, j));
                        H.oSet(n, j, q * H.oGet(n, j) - p * z);
                    }

                    // modify column
                    for (i = 0; i <= n; i++) {
                        z = H.oGet(i, n - 1);
                        H.oSet(i, n - 1, q * z + p * H.oGet(i, n));
                        H.oSet(i, n, q * H.oGet(i, n) - p * z);
                    }

                    // accumulate transformations
                    for (i = low; i <= high; i++) {
                        z = this.oGet(i, n - 1);
                        this.oSet(i, n - 1, q * z + p * this.oGet(i, n));
                        this.oSet(i, n, q * this.oGet(i, n) - p * z);
                    }
                } else {		// complex pair
                    realEigenValues.p[n - 1] = x + p;
                    realEigenValues.p[n] = x + p;
                    imaginaryEigenValues.p[n - 1] = z;
                    imaginaryEigenValues.p[n] = -z;
                }
                n = n - 2;
                iter = 0;

            } else {	// no convergence yet

                // form shift
                x = H.oGet(n, n);
                y = 0.0f;
                w = 0.0f;
                if (l < n) {
                    y = H.oGet(n - 1, n - 1);
                    w = H.oGet(n, n - 1) * H.oGet(n - 1, n);
                }

                // Wilkinson's original ad hoc shift
                if (iter == 10) {
                    exshift += x;
                    for (i = low; i <= n; i++) {
                        H.oMinSet(i, i, x);
                    }
                    s = idMath.Fabs(H.oGet(n, n - 1)) + idMath.Fabs(H.oGet(n - 1, n - 2));
                    x = y = 0.75f * s;
                    w = -0.4375f * s * s;
                }

                // new ad hoc shift
                if (iter == 30) {
                    s = (y - x) / 2.0f;
                    s = s * s + w;
                    if (s > 0) {
                        s = idMath.Sqrt(s);
                        if (y < x) {
                            s = -s;
                        }
                        s = x - w / ((y - x) / 2.0f + s);
                        for (i = low; i <= n; i++) {
                            H.oPluSet(i, i, -s);
                        }
                        exshift += s;
                        x = y = w = 0.964f;
                    }
                }

                iter = iter + 1;

                // look for two consecutive small sub-diagonal elements
                int m;
                for (m = n - 2; m >= l; m--) {
                    z = H.oGet(m, m);
                    r = x - z;
                    s = y - z;
                    p = (r * s - w) / H.oGet(m + 1, m) + H.oGet(m, m + 1);
                    q = H.oGet(m + 1, m + 1) - z - r - s;
                    r = H.oGet(m + 2, m + 1);
                    s = idMath.Fabs(p) + idMath.Fabs(q) + idMath.Fabs(r);
                    p = p / s;
                    q = q / s;
                    r = r / s;
                    if (m == l) {
                        break;
                    }
                    if (idMath.Fabs(H.oGet(m, m - 1)) * (idMath.Fabs(q) + idMath.Fabs(r))
                            < eps * (idMath.Fabs(p) * (idMath.Fabs(H.oGet(m - 1, m - 1)) + idMath.Fabs(z) + idMath.Fabs(H.oGet(m + 1, m + 1))))) {
                        break;
                    }
                }

                for (i = m + 2; i <= n; i++) {
                    H.oSet(i, i - 2, 0.0f);
                    if (i > m + 2) {
                        H.oSet(i, i - 3, 0.0f);
                    }
                }

                // double QR step involving rows l:n and columns m:n
                for (k = m; k <= n - 1; k++) {
                    boolean notlast = (k != n - 1);
                    if (k != m) {
                        p = H.oGet(k, k - 1);
                        q = H.oGet(k + 1, k - 1);
                        r = (notlast ? H.oGet(k + 2, k - 1) : 0.0f);
                        x = idMath.Fabs(p) + idMath.Fabs(q) + idMath.Fabs(r);
                        if (x != 0.0f) {
                            p = p / x;
                            q = q / x;
                            r = r / x;
                        }
                    }
                    if (x == 0.0f) {
                        break;
                    }
                    s = idMath.Sqrt(p * p + q * q + r * r);
                    if (p < 0.0f) {
                        s = -s;
                    }
                    if (s != 0.0f) {
                        if (k != m) {
                            H.oSet(k, k - 1, -s * x);
                        } else if (l != m) {
                            H.oSet(k, k - 1, -H.oGet(k, k - 1));
                        }
                        p = p + s;
                        x = p / s;
                        y = q / s;
                        z = r / s;
                        q = q / p;
                        r = r / p;

                        // modify row
                        for (j = k; j < numRows; j++) {
                            p = H.oGet(k, j) + q * H.oGet(k + 1, j);
                            if (notlast) {
                                p = p + r * H.oGet(k + 2, j);
                                H.oMinSet(k + 2, j, p * z);
                            }
                            H.oPluSet(k, j, -p * x);
                            H.oMinSet(k + 1, j, p * y);
                        }

                        // modify column
                        for (i = 0; i <= Lib.Min(n, k + 3); i++) {
                            p = x * H.oGet(i, k) + y * H.oGet(i, k + 1);
                            if (notlast) {
                                p = p + z * H.oGet(i, k + 2);
                                H.oMinSet(i, k + 2, p * r);
                            }
                            H.oMinSet(i, k, p);
                            H.oMinSet(i, k + 1, p * q);
                        }

                        // accumulate transformations
                        for (i = low; i <= high; i++) {
                            p = x * this.oGet(i, k) + y * this.oGet(i, k + 1);
                            if (notlast) {
                                p = p + z * this.oGet(i, k + 2);
                                this.oMinSet(i, k + 2, p * r);
                            }
                            this.oMinSet(i, k, p);
                            this.oMinSet(i, k + 1, p * q);
                        }
                    }
                }
            }
        }

        // backsubstitute to find vectors of upper triangular form
        if (norm == 0.0f) {
            return false;
        }

        for (n = numRows - 1; n >= 0; n--) {
            p = realEigenValues.p[n];
            q = imaginaryEigenValues.p[n];

            if (q == 0.0f) {		// real vector
                int l = n;
                H.oSet(n, n, 1.0f);
                for (i = n - 1; i >= 0; i--) {
                    w = H.oGet(i, i) - p;
                    r = 0.0f;
                    for (j = l; j <= n; j++) {
                        r = r + H.oGet(i, j) * H.oGet(j, n);
                    }
                    if (imaginaryEigenValues.p[i] < 0.0f) {
                        z = w;
                        s = r;
                    } else {
                        l = i;
                        if (imaginaryEigenValues.p[i] == 0.0f) {
                            if (w != 0.0f) {
                                H.oSet(i, n, -r / w);
                            } else {
                                H.oSet(i, n, -r / (eps * norm));
                            }
                        } else {		// solve real equations
                            x = H.oGet(i, i + 1);
                            y = H.oGet(i + 1, i);
                            q = (realEigenValues.p[i] - p) * (realEigenValues.p[i] - p) + imaginaryEigenValues.p[i] * imaginaryEigenValues.p[i];
                            t = (x * s - z * r) / q;
                            H.oSet(i, n, t);
                            if (idMath.Fabs(x) > idMath.Fabs(z)) {
                                H.oSet(i + 1, n, (-r - w * t) / x);
                            } else {
                                H.oSet(i + 1, n, (-s - y * t) / z);
                            }
                        }

                        // overflow control
                        t = idMath.Fabs(H.oGet(i, n));
                        if ((eps * t) * t > 1) {
                            for (j = i; j <= n; j++) {
                                H.oSet(j, n, H.oGet(j, n) / t);
                            }
                        }
                    }
                }
            } else if (q < 0.0f) {	// complex vector
                int l = n - 1;
                float []cr = {0}, ci = {0};

                // last vector component imaginary so matrix is triangular
                if (idMath.Fabs(H.oGet(n, n - 1)) > idMath.Fabs(H.oGet(n - 1, n))) {
                    H.oSet(n - 1, n - 1, q / H.oGet(n, n - 1));
                    H.oSet(n - 1, n, -(H.oGet(n, n) - p) / H.oGet(n, n - 1));
                } else {
                    ComplexDivision(0.0f, -H.oGet(n - 1, n), H.oGet(n - 1, n - 1) - p, q, cr, ci);
                    H.oSet(n - 1, n - 1, cr[0]);
                    H.oSet(n - 1, n, ci[0]);
                }
                H.oSet(n, n - 1, 0.0f);
                H.oSet(n, n, 1.0f);
                for (i = n - 2; i >= 0; i--) {
                    float ra, sa, vr, vi;
                    ra = 0.0f;
                    sa = 0.0f;
                    for (j = l; j <= n; j++) {
                        ra = ra + H.oGet(i, j) * H.oGet(j, n - 1);
                        sa = sa + H.oGet(i, j) * H.oGet(j, n);
                    }
                    w = H.oGet(i, i) - p;

                    if (imaginaryEigenValues.p[i] < 0.0f) {
                        z = w;
                        r = ra;
                        s = sa;
                    } else {
                        l = i;
                        if (imaginaryEigenValues.p[i] == 0.0f) {
                            ComplexDivision(-ra, -sa, w, q, cr, ci);
                            H.oSet(i, n - 1, cr[0]);
                            H.oSet(i, n, ci[0]);
                        } else {
                            // solve complex equations
                            x = H.oGet(i, i + 1);
                            y = H.oGet(i + 1, i);
                            vr = (realEigenValues.p[i] - p) * (realEigenValues.p[i] - p) + imaginaryEigenValues.p[i] * imaginaryEigenValues.p[i] - q * q;
                            vi = (realEigenValues.p[i] - p) * 2.0f * q;
                            if (vr == 0.0f && vi == 0.0f) {
                                vr = eps * norm * (idMath.Fabs(w) + idMath.Fabs(q) + idMath.Fabs(x) + idMath.Fabs(y) + idMath.Fabs(z));
                            }
                            ComplexDivision(x * r - z * ra + q * sa, x * s - z * sa - q * ra, vr, vi, cr, ci);
                            H.oSet(i, n - 1, cr[0]);
                            H.oSet(i, n, ci[0]);
                            if (idMath.Fabs(x) > (idMath.Fabs(z) + idMath.Fabs(q))) {
                                H.oSet(i + 1, n - 1, (-ra - w * H.oGet(i, n - 1) + q * H.oGet(i, n)) / x);
                                H.oSet(i + 1, n, (-sa - w * H.oGet(i, n) - q * H.oGet(i, n - 1)) / x);
                            } else {
                                ComplexDivision(-r - y * H.oGet(i, n - 1), -s - y * H.oGet(i, n), z, q, cr, ci);
                                H.oSet(i + 1, n - 1, cr[0]);
                                H.oSet(i + 1, n, ci[0]);
                            }
                        }

                        // overflow control
                        t = (float) Lib.Max(idMath.Fabs(H.oGet(i, n - 1)), idMath.Fabs(H.oGet(i, n)));
                        if ((eps * t) * t > 1) {
                            for (j = i; j <= n; j++) {
                                H.oSet(j, n - 1, H.oGet(j, n - 1) / t);
                                H.oSet(j, n, H.oGet(j, n) / t);
                            }
                        }
                    }
                }
            }
        }

        // vectors of isolated roots
        for (i = 0; i < numRows; i++) {
            if (i < low || i > high) {
                for (j = i; j < numRows; j++) {
                    this.oSet(i, j, H.oGet(i, j));
                }
            }
        }

        // back transformation to get eigenvectors of original matrix
        for (j = numRows - 1; j >= low; j--) {
            for (i = low; i <= high; i++) {
                z = 0.0f;
                for (k = low; k <= Lib.Min(j, high); k++) {
                    z = z + this.oGet(i, k) * H.oGet(k, j);
                }
                this.oSet(i, j, z);
            }
        }

        return true;
    }

    float oGet(final int row, final int column) {
        return mat[column + (row * numColumns)];
    }

    public float oSet(final int row, final int column, final float value) {
        return mat[column + (row * numColumns)] = value;
    }

    public void oPluSet(final int row, final int column, final double value) {
        mat[column + (row * numColumns)] += value;
    }

    // DOUBLE: let the compiler cast it back to float AFTER the value has been subtracted.
    public void oMinSet(final int row, final int column, final double value) {
        mat[column + (row * numColumns)] -= value;
    }

    public void oMulSet(final int row, final int column, final double value) {
        mat[column + (row * numColumns)] *= value;
    }

    public void oDivSet(final int row, final int column, final double value) {
        mat[column + (row * numColumns)] /= value;
    }

    private void oNegative(final int row, final int column) {
        mat[column + (row * numColumns)] = (-mat[column + (row * numColumns)]);
    }

    public void arraycopy(final float[] src, final int srcPos, final int destPos, final int length) {
        System.arraycopy(src, srcPos, mat, destPos * numColumns, length);
    }

    public void arraycopy(final float[] src, final int destPos, final int length) {
        System.arraycopy(src, 0, mat, destPos * numColumns, length);
    }

    public void SubVec63_oSet(final int vec6, final int vec3, final idVec3 v) {
        assert (numColumns >= 6 && vec6 >= 0 && vec6 < numRows);

        final int offset = vec6 * 6 + vec3 * 3;
        mat[offset + 0] = v.x;
        mat[offset + 1] = v.y;
        mat[offset + 2] = v.z;
    }

    public void SubVec63_Zero(final int vec6, final int vec3) {
        assert (numColumns >= 6 && vec6 >= 0 && vec6 < numRows);

        final int offset = vec6 * 6 + vec3 * 3;
        mat[offset + 0] = mat[offset + 1] = mat[offset + 2] = 0;
    }
};
