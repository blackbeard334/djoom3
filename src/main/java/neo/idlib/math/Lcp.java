package neo.idlib.math;

import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.idlib.containers.List.idSwap;
import static neo.idlib.math.Matrix.idMatX.MATX_ALLOCA;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.idlib.math.Vector.idVecX.VECX_ALLOCA;

import java.nio.FloatBuffer;

import neo.TempDump;
import neo.framework.CVarSystem.idCVar;
import neo.idlib.Lib.idLib;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVecX;
import neo.idlib.math.Matrix.idMatX;
import neo.open.Nio;

/**
 *
 */
public class Lcp {

    static final idCVar lcp_showFailures = new idCVar("lcp_showFailures", "0", CVAR_SYSTEM | CVAR_BOOL, "show LCP solver failures");

    static final float LCP_BOUND_EPSILON       = 1e-5f;
    static final float LCP_ACCEL_EPSILON       = 1e-5f;
    static final float LCP_DELTA_ACCEL_EPSILON = 1e-9f;
    static final float LCP_DELTA_FORCE_EPSILON = 1e-9f;

    /*
     ===============================================================================

     Box Constrained Mixed Linear Complementarity Problem solver

     A is a matrix of dimension n*n and x, b, lo, hi are vectors of dimension n

     Solve: Ax = b + t, where t is a vector of dimension n, with
     complementarity condition: (x[i] - lo[i]) * (x[i] - hi[i]) * t[i] = 0
     such that for each 0 <= i < n one of the following holds:

     1. lo[i] < x[i] < hi[i], t[i] == 0
     2. x[i] == lo[i], t[i] >= 0
     3. x[i] == hi[i], t[i] <= 0

     Partly bounded or unbounded variables can have lo[i] and/or hi[i]
     set to negative/positive idMath::INFITITY respectively.

     If boxIndex != NULL and boxIndex[i] != -1 then

     lo[i] = - fabs( lo[i] * x[boxIndex[i]] )
     hi[i] = fabs( hi[i] * x[boxIndex[i]] )
     boxIndex[boxIndex[i]] must be -1
  
     Before calculating any of the bounded x[i] with boxIndex[i] != -1 the
     solver calculates all unbounded x[i] and all x[i] with boxIndex[i] == -1.

     ===============================================================================
     */
    public static abstract class idLCP {

        protected int maxIterations;

        // A must be a square matrix
        public idLCP AllocSquare() {
            final idLCP lcp = new idLCP_Square();
            lcp.SetMaxIterations(32);
            return lcp;
        }

        // A must be a symmetric matrix
        public static idLCP AllocSymmetric() {
            final idLCP lcp = new idLCP_Symmetric();
            lcp.SetMaxIterations(32);
            return lcp;
        }

//public	virtual			~idLCP( void );
        public boolean Solve(final idMatX A, idVecX x, final idVecX b, final idVecX lo, final idVecX hi) {
            return Solve(A, x, b, lo, hi, null);
        }

        public abstract boolean Solve(final idMatX A, idVecX x, final idVecX b, final idVecX lo, final idVecX hi, final int[] boxIndex);

        public void SetMaxIterations(int max) {
            this.maxIterations = max;
        }

        public int GetMaxIterations() {
            return this.maxIterations;
        }
    }

    //===============================================================
    //                                                        M
    //  idLCP_Square                                         MrE
    //                                                        E
    //===============================================================
    static class idLCP_Square extends idLCP {

        private idMatX m;		// original matrix
        idVecX b;			    // right hand side
        idVecX lo, hi;			// low and high bounds
        idVecX f, a;			// force and acceleration
        idVecX delta_f, delta_a;// delta force and delta acceleration
        idMatX clamped;			// LU factored sub matrix for clamped variables
        idVecX diagonal;		// reciprocal of diagonal of U of the LU factored sub matrix for clamped variables
        int numUnbounded;		// number of unbounded variables
        int numClamped;			// number of clamped variables
        private FloatBuffer[] rowPtrs;	// pointers to the rows of m
        private int[] boxIndex;		// box index
        private int[] side;		    // tells if a variable is at the low boundary = -1, high boundary = 1 or inbetween = 0
        private int[] permuted;		// index to keep track of the permutation
        private boolean padded;		// set to true if the rows of the initial matrix are 16 byte padded
        //
        //

        @Override
        public boolean Solve(final idMatX o_m, idVecX o_x, final idVecX o_b, final idVecX o_lo, final idVecX o_hi, final int[] o_boxIndex) {
            int i, j, n, boxStartIndex;
            final int[] limit = new int[1], limitSide = new int[1];
            float dir, s;
            final float[] dot = new float[1];
            final float[] maxStep = new float[1];
            char[] failed;

            // true when the matrix rows are 16 byte padded
            this.padded = ((o_m.GetNumRows() + 3) & ~3) == o_m.GetNumColumns();

            assert (this.padded || (o_m.GetNumRows() == o_m.GetNumColumns()));
            assert (o_x.GetSize() == o_m.GetNumRows());
            assert (o_b.GetSize() == o_m.GetNumRows());
            assert (o_lo.GetSize() == o_m.GetNumRows());
            assert (o_hi.GetSize() == o_m.GetNumRows());

            // allocate memory for permuted input
            this.f.SetData(o_m.GetNumRows(), VECX_ALLOCA(o_m.GetNumRows()));
            this.a.SetData(o_b.GetSize(), VECX_ALLOCA(o_b.GetSize()));
            this.b.SetData(o_b.GetSize(), VECX_ALLOCA(o_b.GetSize()));
            this.lo.SetData(o_lo.GetSize(), VECX_ALLOCA(o_lo.GetSize()));
            this.hi.SetData(o_hi.GetSize(), VECX_ALLOCA(o_hi.GetSize()));
            if (o_boxIndex != null) {
//		boxIndex = (int *)_alloca16( o_x.GetSize() * sizeof( int ) );
//		memcpy( boxIndex, o_boxIndex, o_x.GetSize() * sizeof( int ) );
                this.boxIndex = new int[o_x.GetSize()];
                Nio.arraycopy(o_boxIndex, 0, this.boxIndex, 0, o_x.GetSize());
            } else {
                this.boxIndex = null;
            }

            // we override the const on o_m here but on exit the matrix is unchanged
            final float[] const_cast = o_m.ToFloatPtr();
            this.m.SetData(o_m.GetNumRows(), o_m.GetNumColumns(), const_cast);
            o_m.FromFloatPtr(const_cast);

            this.f.Zero();
            this.a.Zero();
            this.b = o_b;
            this.lo = o_lo;
            this.hi = o_hi;

            // pointers to the rows of m
            this.rowPtrs = new FloatBuffer[this.m.GetNumRows()];//rowPtrs = (float **) _alloca16( m.GetNumRows() * sizeof( float * ) );
            for (i = 0; i < this.m.GetNumRows(); i++) {
                this.rowPtrs[i] = this.m.GetRowPtr(i);
            }

            // tells if a variable is at the low boundary, high boundary or inbetween
//	side = (int *) _alloca16( m.GetNumRows() * sizeof( int ) );
            this.side = new int[this.m.GetNumRows()];

            // index to keep track of the permutation
//	permuted = (int *) _alloca16( m.GetNumRows() * sizeof( int ) );
            this.permuted = new int[this.m.GetNumRows()];
            for (i = 0; i < this.m.GetNumRows(); i++) {
                this.permuted[i] = i;
            }

            // permute input so all unbounded variables come first
            this.numUnbounded = 0;
            for (i = 0; i < this.m.GetNumRows(); i++) {
                if ((this.lo.p[i] == -idMath.INFINITY) && (this.hi.p[i] == idMath.INFINITY)) {
                    if (this.numUnbounded != i) {
                        Swap(this.numUnbounded, i);
                    }
                    this.numUnbounded++;
                }
            }

            // permute input so all variables using the boxIndex come last
            boxStartIndex = this.m.GetNumRows();
            if (this.boxIndex != null) {
                for (i = this.m.GetNumRows() - 1; i >= this.numUnbounded; i--) {
                    if ((this.boxIndex[i] >= 0) && ((this.lo.p[i] != -idMath.INFINITY) || (this.hi.p[i] != idMath.INFINITY))) {
                        boxStartIndex--;
                        if (boxStartIndex != i) {
                            Swap(boxStartIndex, i);
                        }
                    }
                }
            }

            // sub matrix for factorization 
            this.clamped.SetData(this.m.GetNumRows(), this.m.GetNumColumns(), MATX_ALLOCA(this.m.GetNumRows() * this.m.GetNumColumns()));
            this.diagonal.SetData(this.m.GetNumRows(), VECX_ALLOCA(this.m.GetNumRows()));

            // all unbounded variables are clamped
            this.numClamped = this.numUnbounded;

            // if there are unbounded variables
            if (this.numUnbounded != 0) {

                // factor and solve for unbounded variables
                if (!FactorClamped()) {
			        idLib.common.Printf( "idLCP_Square::Solve: unbounded factorization failed\n" );
                    return false;
                }
                SolveClamped(this.f, this.b.ToFloatPtr());

                // if there are no bounded variables we are done
                if (this.numUnbounded == this.m.GetNumRows()) {
                    o_x.oSet(this.f);	// the vector is not permuted
                    return true;
                }
            }

//#ifdef IGNORE_UNSATISFIABLE_VARIABLES
            int numIgnored = 0;
//#endif

            // allocate for delta force and delta acceleration
            this.delta_f.SetData(this.m.GetNumRows(), VECX_ALLOCA(this.m.GetNumRows()));
            this.delta_a.SetData(this.m.GetNumRows(), VECX_ALLOCA(this.m.GetNumRows()));

            // solve for bounded variables
            failed = null;
            for (i = this.numUnbounded; i < this.m.GetNumRows(); i++) {

                // once we hit the box start index we can initialize the low and high boundaries of the variables using the box index
                if (i == boxStartIndex) {
                    for (j = 0; j < boxStartIndex; j++) {
                        o_x.p[this.permuted[j]] = this.f.p[j];
                    }
                    for (j = boxStartIndex; j < this.m.GetNumRows(); j++) {
                        s = o_x.p[this.boxIndex[j]];
                        if (this.lo.p[j] != -idMath.INFINITY) {
                            this.lo.p[j] = -idMath.Fabs(this.lo.p[j] * s);
                        }
                        if (this.hi.p[j] != idMath.INFINITY) {
                            this.hi.p[j] = idMath.Fabs(this.hi.p[j] * s);
                        }
                    }
                }

                // calculate acceleration for current variable
                SIMDProcessor.Dot(dot, this.rowPtrs[i], this.f.ToFloatPtr(), i);
                this.a.p[i] = dot[0] - this.b.p[i];

                // if already at the low boundary
                if ((this.lo.p[i] >= -LCP_BOUND_EPSILON) && (this.a.p[i] >= -LCP_ACCEL_EPSILON)) {
                    this.side[i] = -1;
                    continue;
                }

                // if already at the high boundary
                if ((this.hi.p[i] <= LCP_BOUND_EPSILON) && (this.a.p[i] <= LCP_ACCEL_EPSILON)) {
                    this.side[i] = 1;
                    continue;
                }

                // if inside the clamped region
                if (idMath.Fabs(this.a.p[i]) <= LCP_ACCEL_EPSILON) {
                    this.side[i] = 0;
                    AddClamped(i);
                    continue;
                }

                // drive the current variable into a valid region
                for (n = 0; n < this.maxIterations; n++) {

                    // direction to move
                    if (this.a.p[i] <= 0.0f) {
                        dir = 1.0f;
                    } else {
                        dir = -1.0f;
                    }

                    // calculate force delta
                    CalcForceDelta(i, dir);

                    // calculate acceleration delta: delta_a = m * delta_f;
                    CalcAccelDelta(i);

                    // maximum step we can take
                    GetMaxStep(i, dir, maxStep, limit, limitSide);

                    if (maxStep[0] <= 0.0f) {
//#ifdef IGNORE_UNSATISFIABLE_VARIABLES
                        // ignore the current variable completely
                        this.lo.p[i] = this.hi.p[i] = 0.0f;
                        this.f.p[i] = 0.0f;
                        this.side[i] = -1;
                        numIgnored++;
//#else
//				failed = va( "invalid step size %.4f", maxStep );
//#endif
                        break;
                    }

                    // change force
                    ChangeForce(i, maxStep[0]);

                    // change acceleration
                    ChangeAccel(i, maxStep[0]);

                    // clamp/unclamp the variable that limited this step
                    this.side[limit[0]] = limitSide[0];
                    switch (limitSide[0]) {
                        case 0: {
                            this.a.p[limit[0]] = 0.0f;
                            AddClamped(limit[0]);
                            break;
                        }
                        case -1: {
                            this.f.p[limit[0]] = this.lo.p[limit[0]];
                            if (limit[0] != i) {
                                RemoveClamped(limit[0]);
                            }
                            break;
                        }
                        case 1: {
                            this.f.p[limit[0]] = this.hi.p[limit[0]];
                            if (limit[0] != i) {
                                RemoveClamped(limit[0]);
                            }
                            break;
                        }
                    }

                    // if the current variable limited the step we can continue with the next variable
                    if (limit[0] == i) {
                        break;
                    }
                }

//		if ( n >= maxIterations ) {
//			failed = va( "max iterations %d", maxIterations );
//			break;
//		}
//
//		if ( failed ) {
//			break;
//		}
            }

//#ifdef IGNORE_UNSATISFIABLE_VARIABLES
            if (numIgnored != 0) {
                if (lcp_showFailures.GetBool()) {
			        idLib.common.Printf( "idLCP_Symmetric::Solve: %d of %d bounded variables ignored\n", numIgnored, this.m.GetNumRows() - this.numUnbounded );
                }
            }
//#endif

            // if failed clear remaining forces
            if (!TempDump.isDeadCodeFalse() /*failed != null*/) { /* failed is never assigned a value not null before this => It must be null!*/
                if (lcp_showFailures.GetBool()) {
			        idLib.common.Printf( "idLCP_Square::Solve: %s (%d of %d bounded variables ignored)\n", failed, this.m.GetNumRows() - i, this.m.GetNumRows() - this.numUnbounded );
                }
                for (j = i; j < this.m.GetNumRows(); j++) {
                    this.f.p[j] = 0.0f;
                }
            }

//#if defined(_DEBUG) && 0
//	if ( !failed ) {
//		// test whether or not the solution satisfies the complementarity conditions
//		for ( i = 0; i < m.GetNumRows(); i++ ) {
//			a[i] = -b[i];
//			for ( j = 0; j < m.GetNumRows(); j++ ) {
//				a[i] += rowPtrs[i][j] * f[j];
//			}
//
//			if ( f[i] == lo[i] ) {
//				if ( lo[i] != hi[i] && a[i] < -LCP_ACCEL_EPSILON ) {
//					int bah1 = 1;
//				}
//			} else if ( f[i] == hi[i] ) {
//				if ( lo[i] != hi[i] && a[i] > LCP_ACCEL_EPSILON ) {
//					int bah2 = 1;
//				}
//			} else if ( f[i] < lo[i] || f[i] > hi[i] || idMath.Fabs( a[i] ) > 1.0f ) {
//				int bah3 = 1;
//			}
//		}
//	}
//#endif
            // unpermute result
            for (i = 0; i < this.f.GetSize(); i++) {
                o_x.p[this.permuted[i]] = this.f.p[i];
            }

            // unpermute original matrix
            for (i = 0; i < this.m.GetNumRows(); i++) {
                for (j = 0; j < this.m.GetNumRows(); j++) {
                    if (this.permuted[j] == i) {
                        break;
                    }
                }
                if (i != j) {
                    this.m.SwapColumns(i, j);
                    idSwap(this.permuted, this.permuted, i, j);
                }
            }

            return true;
        }

        private boolean FactorClamped() {
            int i, j, k;
            float s, d;

            for (i = 0; i < this.numClamped; i++) {
//		memcpy( clamped[i], rowPtrs[i], numClamped * sizeof( float ) );
                this.clamped.arraycopy(this.rowPtrs[i], i, this.numClamped);//TODO:check two dimensional array
            }

            for (i = 0; i < this.numClamped; i++) {

                s = idMath.Fabs(this.clamped.oGet(i)[i]);

                if (s == 0.0f) {
                    return false;
                }

                this.diagonal.p[i] = d = 1.0f / this.clamped.oGet(i)[i];
                for (j = i + 1; j < this.numClamped; j++) {
//			clamped[j][i] *= d;
                    this.clamped.oMulSet(j, i, d);
                }

                for (j = i + 1; j < this.numClamped; j++) {
                    d = this.clamped.oGet(j)[i];
                    for (k = i + 1; k < this.numClamped; k++) {
                        this.clamped.oMinSet(j, k, d * this.clamped.oGet(i)[k]);
                    }
                }
            }

            return true;
        }

        void SolveClamped(idVecX x, final float[] b) {
            int i, j;
            float sum;

            // solve L
            for (i = 0; i < this.numClamped; i++) {
                sum = b[i];
                for (j = 0; j < i; j++) {
                    sum -= this.clamped.oGet(i)[j] * x.p[j];
                }
                x.p[i] = sum;
            }

            // solve U
            for (i = this.numClamped - 1; i >= 0; i--) {
                sum = x.p[i];
                for (j = i + 1; j < this.numClamped; j++) {
                    sum -= this.clamped.oGet(i)[j] * x.p[j];
                }
                x.p[i] = sum * this.diagonal.p[i];
            }
        }

        void Swap(int i, int j) {

            if (i == j) {
                return;
            }

            idSwap(this.rowPtrs, this.rowPtrs, i, j);
            this.m.SwapColumns(i, j);
            this.b.SwapElements(i, j);
            this.lo.SwapElements(i, j);
            this.hi.SwapElements(i, j);
            this.a.SwapElements(i, j);
            this.f.SwapElements(i, j);
            if (null != this.boxIndex) {
                idSwap(this.boxIndex, this.boxIndex, i, j);
            }
            idSwap(this.side, this.side, i, j);
            idSwap(this.permuted, this.permuted, i, j);
        }

        void AddClamped(int r) {
            int i, j;
            float sum;

            assert (r >= this.numClamped);

            // add a row at the bottom and a column at the right of the factored
            // matrix for the clamped variables
            Swap(this.numClamped, r);

            // add row to L
            for (i = 0; i < this.numClamped; i++) {
                sum = this.rowPtrs[this.numClamped].get(i);
                for (j = 0; j < i; j++) {
                    sum -= this.clamped.oGet(this.numClamped)[j] * this.clamped.oGet(j)[i];
                }
                this.clamped.oSet(this.numClamped, i, sum * this.diagonal.p[i]);
            }

            // add column to U
            for (i = 0; i <= this.numClamped; i++) {
                sum = this.rowPtrs[i].get(this.numClamped);
                for (j = 0; j < i; j++) {
                    sum -= this.clamped.oGet(i)[j] * this.clamped.oGet(j)[this.numClamped];
                }
                this.clamped.oSet(i, this.numClamped, sum);
            }

            this.diagonal.p[this.numClamped] = 1.0f / this.clamped.oGet(this.numClamped)[this.numClamped];

            this.numClamped++;
        }

        void RemoveClamped(int r) {
            int i, j;
            float[] y0, y1, z0, z1;
            double diag, beta0, beta1, p0, p1, q0, q1, d;

            assert (r < this.numClamped);

            this.numClamped--;

            // no need to swap and update the factored matrix when the last row and column are removed
            if (r == this.numClamped) {
                return;
            }

//	y0 = (float *) _alloca16( numClamped * sizeof( float ) );
//	z0 = (float *) _alloca16( numClamped * sizeof( float ) );
//	y1 = (float *) _alloca16( numClamped * sizeof( float ) );
//	z1 = (float *) _alloca16( numClamped * sizeof( float ) );
            y0 = new float[this.numClamped];
            z0 = new float[this.numClamped];
            y1 = new float[this.numClamped];
            z1 = new float[this.numClamped];

            // the row/column need to be subtracted from the factorization
            for (i = 0; i < this.numClamped; i++) {
                y0[i] = -this.rowPtrs[i].get(r);
            }

//	memset( y1, 0, numClamped * sizeof( float ) );
            y1[r] = 1.0f;

//	memset( z0, 0, numClamped * sizeof( float ) );
            z0[r] = 1.0f;

            for (i = 0; i < this.numClamped; i++) {
                z1[i] = -this.rowPtrs[r].get(i);
            }

            // swap the to be removed row/column with the last row/column
            Swap(r, this.numClamped);

            // the swapped last row/column need to be added to the factorization
            for (i = 0; i < this.numClamped; i++) {
                y0[i] += this.rowPtrs[i].get(r);
            }

            for (i = 0; i < this.numClamped; i++) {
                z1[i] += this.rowPtrs[r].get(i);
            }
            z1[r] = 0.0f;

            // update the beginning of the to be updated row and column
            for (i = 0; i < r; i++) {
                p0 = y0[i];
                beta1 = z1[i] * this.diagonal.p[i];

                this.clamped.oPluSet(i, r, p0);
                for (j = i + 1; j < this.numClamped; j++) {
                    z1[j] -= beta1 * this.clamped.oGet(i)[j];
                }
                for (j = i + 1; j < this.numClamped; j++) {
                    y0[j] -= p0 * this.clamped.oGet(j)[i];
                }
                this.clamped.oPluSet(r, i, beta1);
            }

            // update the lower right corner starting at r,r
            for (i = r; i < this.numClamped; i++) {
                diag = this.clamped.oGet(i)[i];

                p0 = y0[i];
                p1 = z0[i];
                diag += p0 * p1;

                if (diag == 0.0f) {
			        idLib.common.Printf( "idLCP_Square::RemoveClamped: updating factorization failed\n" );
                    return;
                }

                beta0 = p1 / diag;

                q0 = y1[i];
                q1 = z1[i];
                diag += q0 * q1;

                if (diag == 0.0f) {
			        idLib.common.Printf( "idLCP_Square::RemoveClamped: updating factorization failed\n" );
                    return;
                }

                d = 1.0f / diag;
                beta1 = q1 * d;

                this.clamped.oSet(i, i, (float) diag);
                this.diagonal.p[i] = (float) d;

                for (j = i + 1; j < this.numClamped; j++) {

                    d = this.clamped.oGet(i)[j];

                    d += p0 * z0[j];
                    z0[j] -= beta0 * d;

                    d += q0 * z1[j];
                    z1[j] -= beta1 * d;

                    this.clamped.oSet(i, j, (float) d);
                }

                for (j = i + 1; j < this.numClamped; j++) {

                    d = this.clamped.oGet(j)[i];

                    y0[j] -= p0 * d;
                    d += beta0 * y0[j];

                    y1[j] -= q0 * d;
                    d += beta1 * y1[j];

                    this.clamped.oSet(j, i, (float) d);
                }
            }
            return;
        }

        /*
         ============
         idLCP_Square::CalcForceDelta

         modifies this->delta_f
         ============
         */
        private void CalcForceDelta(int d, float dir) {
            int i;
            float[] ptr;

            this.delta_f.p[d] = dir;

            if (this.numClamped == 0) {
                return;
            }

            // get column d of matrix
//	ptr = (float *) _alloca16( numClamped * sizeof( float ) );
            ptr = new float[this.numClamped];
            for (i = 0; i < this.numClamped; i++) {
                ptr[i] = this.rowPtrs[i].get(d);
            }

            // solve force delta
            SolveClamped(this.delta_f, ptr);

            // flip force delta based on direction
            if (dir > 0.0f) {
                ptr = this.delta_f.ToFloatPtr();
                for (i = 0; i < this.numClamped; i++) {
                    ptr[i] = -ptr[i];
                }
            }
        }

        /*
         ============
         idLCP_Square::CalcAccelDelta

         modifies this->delta_a and uses this->delta_f
         ============
         */
        private void CalcAccelDelta(int d) {
            int j;
            final float[] dot = new float[1];

            // only the not clamped variables, including the current variable, can have a change in acceleration
            for (j = this.numClamped; j <= d; j++) {
                // only the clamped variables and the current variable have a force delta unequal zero
                SIMDProcessor.Dot(dot, this.rowPtrs[j], this.delta_f.ToFloatPtr(), this.numClamped);
                this.delta_a.p[j] = dot[0] + (this.rowPtrs[j].get(d) * this.delta_f.p[d]);
            }
        }

        /*
         ============
         idLCP_Square::ChangeForce

         modifies this->f and uses this->delta_f
         ============
         */
        private void ChangeForce(int d, float step) {
            // only the clamped variables and current variable have a force delta unequal zero
            SIMDProcessor.MulAdd(this.f.ToFloatPtr(), step, this.delta_f.ToFloatPtr(), this.numClamped);
            this.f.p[d] += step * this.delta_f.p[d];
        }

        /*
         ============
         idLCP_Square::ChangeAccel

         modifies this->a and uses this->delta_a
         ============
         */
        private void ChangeAccel(int d, float step) {
            final float[] clampedA = clam(this.a, this.numClamped);
            final float[] clampedDeltaA = clam(this.delta_a, this.numClamped);

            // only the not clamped variables, including the current variable, can have an acceleration unequal zero
            SIMDProcessor.MulAdd(clampedA, step, clampedDeltaA, (d - this.numClamped) + 1);

            unClam(this.a, clampedA);
            unClam(this.delta_a, clampedDeltaA);
        }

        private void GetMaxStep(int d, float dir, float[] maxStep, int[] limit, int[] limitSide) {
            int i;
            float s;

            // default to a full step for the current variable
            if (idMath.Fabs(this.delta_a.p[d]) > LCP_DELTA_ACCEL_EPSILON) {
                maxStep[0] = -this.a.p[d] / this.delta_a.p[d];
            } else {
                maxStep[0] = 0.0f;
            }
            limit[0] = d;
            limitSide[0] = 0;

            // test the current variable
            if (dir < 0.0f) {
                if (this.lo.p[d] != -idMath.INFINITY) {
                    s = (this.lo.p[d] - this.f.p[d]) / dir;
                    if (s < maxStep[0]) {
                        maxStep[0] = s;
                        limitSide[0] = -1;
                    }
                }
            } else {
                if (this.hi.p[d] != idMath.INFINITY) {
                    s = (this.hi.p[d] - this.f.p[d]) / dir;
                    if (s < maxStep[0]) {
                        maxStep[0] = s;
                        limitSide[0] = 1;
                    }
                }
            }

            // test the clamped bounded variables
            for (i = this.numUnbounded; i < this.numClamped; i++) {
                if (this.delta_f.p[i] < -LCP_DELTA_FORCE_EPSILON) {
                    // if there is a low boundary
                    if (this.lo.p[i] != -idMath.INFINITY) {
                        s = (this.lo.p[i] - this.f.p[i]) / this.delta_f.p[i];
                        if (s < maxStep[0]) {
                            maxStep[0] = s;
                            limit[0] = i;
                            limitSide[0] = -1;
                        }
                    }
                } else if (this.delta_f.p[i] > LCP_DELTA_FORCE_EPSILON) {
                    // if there is a high boundary
                    if (this.hi.p[i] != idMath.INFINITY) {
                        s = (this.hi.p[i] - this.f.p[i]) / this.delta_f.p[i];
                        if (s < maxStep[0]) {
                            maxStep[0] = s;
                            limit[0] = i;
                            limitSide[0] = 1;
                        }
                    }
                }
            }

            // test the not clamped bounded variables
            for (i = this.numClamped; i < d; i++) {
                if (this.side[i] == -1) {
                    if (this.delta_a.p[i] >= -LCP_DELTA_ACCEL_EPSILON) {
                        continue;
                    }
                } else if (this.side[i] == 1) {
                    if (this.delta_a.p[i] <= LCP_DELTA_ACCEL_EPSILON) {
                        continue;
                    }
                } else {
                    continue;
                }
                // ignore variables for which the force is not allowed to take any substantial value
                if ((this.lo.p[i] >= -LCP_BOUND_EPSILON) && (this.hi.p[i] <= LCP_BOUND_EPSILON)) {
                    continue;
                }
                s = -this.a.p[i] / this.delta_a.p[i];
                if (s < maxStep[0]) {
                    maxStep[0] = s;
                    limit[0] = i;
                    limitSide[0] = 0;
                }
            }
        }
    }

    //===============================================================
    //                                                        M
    //  idLCP_Symmetric                                      MrE
    //                                                        E
    //===============================================================
    static class idLCP_Symmetric extends idLCP {

        private final idMatX        m;                    // original matrix
        private final idVecX        b;                    // right hand side
        private final idVecX        lo, hi;               // low and high bounds
        private final idVecX        f, a;                 // force and acceleration
        private final idVecX        delta_f, delta_a;     // delta force and delta acceleration
        private final idMatX        clamped;              // LDLt factored sub matrix for clamped variables
        private final idVecX        diagonal;             // reciprocal of diagonal of LDLt factored sub matrix for clamped variables
        private final idVecX        solveCache1;          // intermediate result cached in SolveClamped
        private final idVecX        solveCache2;          // "
        private int           numUnbounded;         // number of unbounded variables
        private int           numClamped;           // number of clamped variables
        private int           clampedChangeStart;   // lowest row/column changed in the clamped matrix during an iteration
        private FloatBuffer[] rowPtrs;              // pointers to the rows of m
        private int[]         boxIndex;             // box index
        private int[]         side;                 // tells if a variable is at the low boundary = -1, high boundary = 1 or inbetween = 0
        private int[]         permuted;             // index to keep track of the permutation
        private boolean       padded;               // set to true if the rows of the initial matrix are 16 byte padded
        //
        //

        public idLCP_Symmetric() {
            this.m = new idMatX();
            this.b = new idVecX();
            this.lo = new idVecX();
            this.hi = new idVecX();
            this.f = new idVecX();
            this.a = new idVecX();
            this.delta_f = new idVecX();
            this.delta_a = new idVecX();
            this.clamped = new idMatX();
            this.diagonal = new idVecX();
            this.solveCache1 = new idVecX();
            this.solveCache2 = new idVecX();
        }

        @Override
        public boolean Solve(final idMatX o_m, idVecX o_x, final idVecX o_b, final idVecX o_lo, final idVecX o_hi, final int[] o_boxIndex) {
            int i, j, n, boxStartIndex;
            final int[] limit = new int[1], limitSide = new int[1];
            float dir, s;
            final float[] dot = new float[1];
            final float[] maxStep = new float[1];
            char[] failed;

            // true when the matrix rows are 16 byte padded
            this.padded = ((o_m.GetNumRows() + 3) & ~3) == o_m.GetNumColumns();

            assert (this.padded || (o_m.GetNumRows() == o_m.GetNumColumns()));
            assert (o_x.GetSize() == o_m.GetNumRows());
            assert (o_b.GetSize() == o_m.GetNumRows());
            assert (o_lo.GetSize() == o_m.GetNumRows());
            assert (o_hi.GetSize() == o_m.GetNumRows());

            // allocate memory for permuted input
            this.f.SetData(o_m.GetNumRows(), VECX_ALLOCA(o_m.GetNumRows()));
            this.a.SetData(o_b.GetSize(), VECX_ALLOCA(o_b.GetSize()));
            this.b.SetData(o_b.GetSize(), VECX_ALLOCA(o_b.GetSize()));
            this.lo.SetData(o_lo.GetSize(), VECX_ALLOCA(o_lo.GetSize()));
            this.hi.SetData(o_hi.GetSize(), VECX_ALLOCA(o_hi.GetSize()));
            if (null != o_boxIndex) {
//		boxIndex = (int *)_alloca16( o_x.GetSize() * sizeof( int ) );
                this.boxIndex = new int[o_x.GetSize()];
//		memcpy( boxIndex, o_boxIndex, o_x.GetSize() * sizeof( int ) );
                Nio.arraycopy(o_boxIndex, 0, this.boxIndex, 0, o_x.GetSize());
            } else {
                this.boxIndex = null;
            }

            // we override the const on o_m here but on exit the matrix is unchanged
            this.m.SetData(o_m.GetNumRows(), o_m.GetNumColumns(), o_m.oGet(0));
            this.f.Zero();
            this.a.Zero();
            this.b.oSet(o_b);
            this.lo.oSet(o_lo);
            this.hi.oSet(o_hi);

            // pointers to the rows of m
            this.rowPtrs = new FloatBuffer[this.m.GetNumRows()];//rowPtrs = (float **) _alloca16( m.GetNumRows() * sizeof( float * ) );
            for (i = 0; i < this.m.GetNumRows(); i++) {
                this.rowPtrs[i] = this.m.GetRowPtr(i);
            }

            // tells if a variable is at the low boundary, high boundary or inbetween
//	side = (int *) _alloca16( m.GetNumRows() * sizeof( int ) );
            this.side = new int[this.m.GetNumRows()];

            // index to keep track of the permutation
//	permuted = (int *) _alloca16( m.GetNumRows() * sizeof( int ) );
            this.permuted = new int[this.m.GetNumRows()];
            for (i = 0; i < this.m.GetNumRows(); i++) {
                this.permuted[i] = i;
            }

            // permute input so all unbounded variables come first
            this.numUnbounded = 0;
            for (i = 0; i < this.m.GetNumRows(); i++) {
                if ((this.lo.p[i] == -idMath.INFINITY) && (this.hi.p[i] == idMath.INFINITY)) {
                    if (this.numUnbounded != i) {
                        Swap(this.numUnbounded, i);
                    }
                    this.numUnbounded++;
                }
            }

            // permute input so all variables using the boxIndex come last
            boxStartIndex = this.m.GetNumRows();
            if (null != this.boxIndex) {
                for (i = this.m.GetNumRows() - 1; i >= this.numUnbounded; i--) {
                    if ((this.boxIndex[i] >= 0) && ((this.lo.p[i] != -idMath.INFINITY) || (this.hi.p[i] != idMath.INFINITY))) {
                        boxStartIndex--;
                        if (boxStartIndex != i) {
                            Swap(boxStartIndex, i);
                        }
                    }
                }
            }

            // sub matrix for factorization 
            this.clamped.SetData(this.m.GetNumRows(), this.m.GetNumColumns(), MATX_ALLOCA(this.m.GetNumRows() * this.m.GetNumColumns()));
            this.diagonal.SetData(this.m.GetNumRows(), VECX_ALLOCA(this.m.GetNumRows()));
            this.solveCache1.SetData(this.m.GetNumRows(), VECX_ALLOCA(this.m.GetNumRows()));
            this.solveCache2.SetData(this.m.GetNumRows(), VECX_ALLOCA(this.m.GetNumRows()));

            // all unbounded variables are clamped
            this.numClamped = this.numUnbounded;

            // if there are unbounded variables
            if (0 != this.numUnbounded) {

                // factor and solve for unbounded variables
                if (!FactorClamped()) {
			        idLib.common.Printf( "idLCP_Symmetric::Solve: unbounded factorization failed\n" );
                    return false;
                }
                SolveClamped(this.f, this.b.ToFloatPtr());

                // if there are no bounded variables we are done
                if (this.numUnbounded == this.m.GetNumRows()) {
                    o_x.oSet(this.f);	// the vector is not permuted
                    return true;
                }
            }

//#ifdef IGNORE_UNSATISFIABLE_VARIABLES
            int numIgnored = 0;
//#endif

            // allocate for delta force and delta acceleration
            this.delta_f.SetData(this.m.GetNumRows(), VECX_ALLOCA(this.m.GetNumRows()));
            this.delta_a.SetData(this.m.GetNumRows(), VECX_ALLOCA(this.m.GetNumRows()));

            // solve for bounded variables
            failed = null;
            for (i = this.numUnbounded; i < this.m.GetNumRows(); i++) {

                this.clampedChangeStart = 0;

                // once we hit the box start index we can initialize the low and high boundaries of the variables using the box index
                if (i == boxStartIndex) {
                    for (j = 0; j < boxStartIndex; j++) {
                        o_x.p[this.permuted[j]] = this.f.p[j];
                    }
                    for (j = boxStartIndex; j < this.m.GetNumRows(); j++) {
                        s = o_x.p[this.boxIndex[j]];
                        if (this.lo.p[j] != -idMath.INFINITY) {
                            this.lo.p[j] = -idMath.Fabs(this.lo.p[j] * s);
                        }
                        if (this.hi.p[j] != idMath.INFINITY) {
                            this.hi.p[j] = idMath.Fabs(this.hi.p[j] * s);
                        }
                    }
                }

                // calculate acceleration for current variable
                SIMDProcessor.Dot(dot, this.rowPtrs[i], this.f.ToFloatPtr(), i);
                this.a.p[i] = dot[0] - this.b.p[i];

                // if already at the low boundary
                if ((this.lo.p[i] >= -LCP_BOUND_EPSILON) && (this.a.p[i] >= -LCP_ACCEL_EPSILON)) {
                    this.side[i] = -1;
                    continue;
                }

                // if already at the high boundary
                if ((this.hi.p[i] <= LCP_BOUND_EPSILON) && (this.a.p[i] <= LCP_ACCEL_EPSILON)) {
                    this.side[i] = 1;
                    continue;
                }

                // if inside the clamped region
                if (idMath.Fabs(this.a.p[i]) <= LCP_ACCEL_EPSILON) {
                    this.side[i] = 0;
                    AddClamped(i, false);
                    continue;
                }

                // drive the current variable into a valid region
                for (n = 0; n < this.maxIterations; n++) {

                    // direction to move
                    if (this.a.p[i] <= 0.0f) {
                        dir = 1.0f;
                    } else {
                        dir = -1.0f;
                    }

                    // calculate force delta
                    CalcForceDelta(i, dir);

                    // calculate acceleration delta: delta_a = m * delta_f;
                    CalcAccelDelta(i);

                    // maximum step we can take
                    GetMaxStep(i, dir, maxStep, limit, limitSide);

                    if (maxStep[0] <= 0.0f) {
//#ifdef IGNORE_UNSATISFIABLE_VARIABLES
                        // ignore the current variable completely
                        this.lo.p[i] = this.hi.p[i] = 0.0f;
                        this.f.p[i] = 0.0f;
                        this.side[i] = -1;
                        numIgnored++;
//#else
//				failed = va( "invalid step size %.4f", maxStep );
//#endif
                        break;
                    }

                    // change force
                    ChangeForce(i, maxStep[0]);

                    // change acceleration
                    ChangeAccel(i, maxStep[0]);

                    // clamp/unclamp the variable that limited this step
                    this.side[limit[0]] = limitSide[0];
                    switch (limitSide[0]) {
                        case 0: {
                            this.a.p[limit[0]] = 0.0f;
                            AddClamped(limit[0], (limit[0] == i));
                            break;
                        }
                        case -1: {
                            this.f.p[limit[0]] = this.lo.p[limit[0]];
                            if (limit[0] != i) {
                                RemoveClamped(limit[0]);
                            }
                            break;
                        }
                        case 1: {
                            this.f.p[limit[0]] = this.hi.p[limit[0]];
                            if (limit[0] != i) {
                                RemoveClamped(limit[0]);
                            }
                            break;
                        }
                    }

                    // if the current variable limited the step we can continue with the next variable
                    if (limit[0] == i) {
                        break;
                    }
                }

                if (n >= this.maxIterations) {
//			failed = va( "max iterations %d", maxIterations );
                    break;
                }

                if (null != failed) {
                    break;
                }
            }

//#ifdef IGNORE_UNSATISFIABLE_VARIABLES
            if (0 != numIgnored) {
                if (lcp_showFailures.GetBool()) {
			        idLib.common.Printf( "idLCP_Symmetric::Solve: %d of %d bounded variables ignored\n", numIgnored, this.m.GetNumRows() - this.numUnbounded );
                }
            }
//#endif

            // if failed clear remaining forces
            if (!TempDump.isDeadCodeFalse() /*null != failed*/) { /* failed is never assigned a value not null before this => It must be null!*/
                if (lcp_showFailures.GetBool()) {
			        idLib.common.Printf( "idLCP_Symmetric::Solve: %s (%d of %d bounded variables ignored)\n", failed, this.m.GetNumRows() - i, this.m.GetNumRows() - this.numUnbounded );
                }
                for (j = i; j < this.m.GetNumRows(); j++) {
                    this.f.p[j] = 0.0f;
                }
            }

//#if defined(_DEBUG) && 0
//	if ( !failed ) {
//		// test whether or not the solution satisfies the complementarity conditions
//		for ( i = 0; i < m.GetNumRows(); i++ ) {
//			a[i] = -b[i];
//			for ( j = 0; j < m.GetNumRows(); j++ ) {
//				a[i] += rowPtrs[i][j] * f[j];
//			}
//
//			if ( f[i] == lo[i] ) {
//				if ( lo[i] != hi[i] && a[i] < -LCP_ACCEL_EPSILON ) {
//					int bah1 = 1;
//				}
//			} else if ( f[i] == hi[i] ) {
//				if ( lo[i] != hi[i] && a[i] > LCP_ACCEL_EPSILON ) {
//					int bah2 = 1;
//				}
//			} else if ( f[i] < lo[i] || f[i] > hi[i] || idMath::Fabs( a[i] ) > 1.0f ) {
//				int bah3 = 1;
//			}
//		}
//	}
//#endif
            // unpermute result
            for (i = 0; i < this.f.GetSize(); i++) {
                o_x.p[this.permuted[i]] = this.f.p[i];
            }

            // unpermute original matrix
            for (i = 0; i < this.m.GetNumRows(); i++) {
                for (j = 0; j < this.m.GetNumRows(); j++) {
                    if (this.permuted[j] == i) {
                        break;
                    }
                }
                if (i != j) {
                    this.m.SwapColumns(i, j);
                    idSwap(this.permuted, i, j);
                }
            }

            return true;
        }

        private boolean FactorClamped() {

            this.clampedChangeStart = 0;

            for (int i = 0; i < this.numClamped; i++) {
//		memcpy( clamped[i], rowPtrs[i], numClamped * sizeof( float ) );
                this.clamped.arraycopy(this.rowPtrs[i], i, this.numClamped);
                final int a = 0;
            }
            final boolean b = SIMDProcessor.MatX_LDLTFactor(this.clamped, this.diagonal, this.numClamped);
            final int a = 0;
            return b;
        }

        private void SolveClamped(idVecX x, final float[] b) {

            // solve L
            SIMDProcessor.MatX_LowerTriangularSolve(this.clamped, this.solveCache1.ToFloatPtr(), b, this.numClamped, this.clampedChangeStart);

            // solve D
            SIMDProcessor.Mul(this.solveCache2.ToFloatPtr(), this.solveCache1.ToFloatPtr(), this.diagonal.ToFloatPtr(), this.numClamped);

            // solve Lt
            SIMDProcessor.MatX_LowerTriangularSolveTranspose(this.clamped, x.ToFloatPtr(), this.solveCache2.ToFloatPtr(), this.numClamped);

            this.clampedChangeStart = this.numClamped;
        }

        private void SolveClamped(idVecX x, final FloatBuffer b) {
            SolveClamped(x, TempDump.fbtofa(b));
        }

        private void Swap(int i, int j) {

            if (i == j) {
                return;
            }

            idSwap(this.rowPtrs, this.rowPtrs, i, j);
            this.m.SwapColumns(i, j);
            this.b.SwapElements(i, j);
            this.lo.SwapElements(i, j);
            this.hi.SwapElements(i, j);
            this.a.SwapElements(i, j);
            this.f.SwapElements(i, j);
            if (null != this.boxIndex) {
                idSwap(this.boxIndex, this.boxIndex, i, j);
            }
            idSwap(this.side, this.side, i, j);
            idSwap(this.permuted, this.permuted, i, j);
        }

        private void AddClamped(int r, boolean useSolveCache) {
            float d;
            final float[] dot = new float[1];

            assert (r >= this.numClamped);

            if (this.numClamped < this.clampedChangeStart) {
                this.clampedChangeStart = this.numClamped;
            }

            // add a row at the bottom and a column at the right of the factored
            // matrix for the clamped variables
            Swap(this.numClamped, r);

            // solve for v in L * v = rowPtr[numClamped]
            if (useSolveCache) {

                // the lower triangular solve was cached in SolveClamped called by CalcForceDelta
                this.clamped.arraycopy(this.solveCache2.ToFloatPtr(), this.numClamped, this.numClamped);//memcpy(clamped[numClamped], solveCache2.ToFloatPtr(), numClamped * sizeof(float));
                final int a = 0;
                // calculate row dot product
                SIMDProcessor.Dot(dot, this.solveCache2.ToFloatPtr(), this.solveCache1.ToFloatPtr(), this.numClamped);

            } else {
                final float[] v = new float[this.numClamped];//(float *) _alloca16(numClamped * sizeof(float));
                final float[] clampedArray = clam(this.clamped, this.numClamped);

                SIMDProcessor.MatX_LowerTriangularSolve(this.clamped, v, this.rowPtrs[this.numClamped], this.numClamped);
                // add bottom row to L
                SIMDProcessor.Mul(clampedArray, v, this.diagonal.ToFloatPtr(), this.numClamped);
                // calculate row dot product
                SIMDProcessor.Dot(dot, clampedArray, v, this.numClamped);

                unClam(this.clamped, clampedArray);
            }

            // update diagonal[numClamped]
            d = this.rowPtrs[this.numClamped].get(this.numClamped) - dot[0];

            if (d == 0.0f) {
                idLib.common.Printf("idLCP_Symmetric::AddClamped: updating factorization failed\n");
                this.numClamped++;
                return;
            }

            this.clamped.oSet(this.numClamped, this.numClamped, d);
            this.diagonal.p[this.numClamped] = 1.0f / d;

            this.numClamped++;
        }

        private void RemoveClamped(int r) {
            int i, j, n;
            float[] addSub, v, v1, v2;
			final float[] dot = new float[1];
            double sum, diag, newDiag, invNewDiag, p1, p2, alpha1, alpha2, beta1, beta2;
            FloatBuffer original, ptr;

            assert (r < this.numClamped);

            if (r < this.clampedChangeStart) {
                this.clampedChangeStart = r;
            }

            this.numClamped--;

            // no need to swap and update the factored matrix when the last row and column are removed
            if (r == this.numClamped) {
                return;
            }

            // swap the to be removed row/column with the last row/column
            Swap(r, this.numClamped);

            // update the factored matrix
            addSub = new float[this.numClamped];//	addSub = (float *) _alloca16( numClamped * sizeof( float ) );

            if (r == 0) {

                if (this.numClamped == 1) {
                    diag = this.rowPtrs[0].get(0);
                    if (diag == 0.0f) {
                        idLib.common.Printf("idLCP_Symmetric::RemoveClamped: updating factorization failed\n");
                        return;
                    }
                    this.clamped.oSet(0, 0, (float) diag);
                    this.diagonal.p[0] = (float) (1.0f / diag);
                    return;
                }

                // calculate the row/column to be added to the lower right sub matrix starting at (r, r)
                original = this.rowPtrs[this.numClamped];
                ptr = this.rowPtrs[r];
                addSub[0] = ptr.get(0) - original.get(this.numClamped);
                for (i = 1; i < this.numClamped; i++) {
                    addSub[i] = ptr.get(i) - original.get(i);
                }

            } else {

                v = new float[this.numClamped];//= (float *) _alloca16( numClamped * sizeof( float ) );
                final float[] clampedArray = clam(this.clamped, r);

                // solve for v in L * v = rowPtr[r]
                SIMDProcessor.MatX_LowerTriangularSolve(this.clamped, v, this.rowPtrs[r], r);

                // update removed row
                SIMDProcessor.Mul(clampedArray, v, this.diagonal.ToFloatPtr(), r);

                // if the last row/column of the matrix is updated
                if (r == (this.numClamped - 1)) {
                    // only calculate new diagonal
                    SIMDProcessor.Dot(dot, clampedArray, v, r);
                    unClam(this.clamped, clampedArray);
                    diag = this.rowPtrs[r].get(r) - dot[0];
                    if (diag == 0.0f) {
				        idLib.common.Printf( "idLCP_Symmetric::RemoveClamped: updating factorization failed\n" );
                        return;
                    }
                    this.clamped.oSet(r, r, (float) diag);
                    this.diagonal.p[r] = (float) (1.0f / diag);
                    return;
                }
                unClam(this.clamped, clampedArray);

                // calculate the row/column to be added to the lower right sub matrix starting at (r, r)
                for (i = 0; i < r; i++) {
                    v[i] = this.clamped.oGet(r)[i] * this.clamped.oGet(i)[i];
                }
                for (i = r; i < this.numClamped; i++) {
                    if (i == r) {
                        sum = this.clamped.oGet(r)[r];
                    } else {
                        sum = this.clamped.oGet(r)[r] * this.clamped.oGet(i)[r];
                    }
                    ptr = this.clamped.GetRowPtr(i);
                    for (j = 0; j < r; j++) {
                        sum += ptr.get(j) * v[j];
                    }
                    addSub[i] = (float) (this.rowPtrs[r].get(i) - sum);
                }
            }

            // add row/column to the lower right sub matrix starting at (r, r)
            v1 = new float[this.numClamped];//	v1 = (float *) _alloca16( numClamped * sizeof( float ) );
            v2 = new float[this.numClamped];//	v2 = (float *) _alloca16( numClamped * sizeof( float ) );

            diag = idMath.SQRT_1OVER2;
            v1[r] = (float) (((0.5f * addSub[r]) + 1.0f) * diag);
            v2[r] = (float) (((0.5f * addSub[r]) - 1.0f) * diag);
            for (i = r + 1; i < this.numClamped; i++) {
                v1[i] = v2[i] = (float) (addSub[i] * diag);
            }

            alpha1 = 1.0f;
            alpha2 = -1.0f;

            // simultaneous update/downdate of the sub matrix starting at (r, r)
            n = this.clamped.GetNumColumns();
            for (i = r; i < this.numClamped; i++) {

                diag = this.clamped.oGet(i)[i];
                p1 = v1[i];
                newDiag = diag + (alpha1 * p1 * p1);

                if (newDiag == 0.0f) {
			        idLib.common.Printf( "idLCP_Symmetric::RemoveClamped: updating factorization failed\n" );
                    return;
                }

                alpha1 /= newDiag;
                beta1 = p1 * alpha1;
                alpha1 *= diag;

                diag = newDiag;
                p2 = v2[i];
                newDiag = diag + (alpha2 * p2 * p2);

                if (newDiag == 0.0f) {
			        idLib.common.Printf( "idLCP_Symmetric::RemoveClamped: updating factorization failed\n" );
                    return;
                }

                this.clamped.oSet(i, i, (float) newDiag);
                this.diagonal.p[i] = (float) (invNewDiag = 1.0f / newDiag);

                alpha2 *= invNewDiag;
                beta2 = p2 * alpha2;
                alpha2 *= diag;

                // update column below diagonal (i,i)
                ptr = this.clamped.ToFloatBufferPtr(i);

                for (j = i + 1; j < (this.numClamped - 1); j += 2) {

                    float sum0 = ptr.get((j + 0) * n);
                    float sum1 = ptr.get((j + 1) * n);

                    v1[j + 0] -= p1 * sum0;
                    v1[j + 1] -= p1 * sum1;

                    sum0 += beta1 * v1[j + 0];
                    sum1 += beta1 * v1[j + 1];

                    v2[j + 0] -= p2 * sum0;
                    v2[j + 1] -= p2 * sum1;

                    sum0 += beta2 * v2[j + 0];
                    sum1 += beta2 * v2[j + 1];

                    ptr.put((j + 0) * n, sum0);
                    ptr.put((j + 1) * n, sum1);
                    final int a = 0;
                }

                for (; j < this.numClamped; j++) {

                    sum = ptr.get(j * n);

                    v1[j] -= p1 * sum;
                    sum += beta1 * v1[j];

                    v2[j] -= p2 * sum;
                    sum += beta2 * v2[j];

                    ptr.put(j * n, (float) sum);
                }
            }
        }

        /*
         ============
         idLCP_Symmetric::CalcForceDelta

         modifies this->delta_f
         ============
         */
        private void CalcForceDelta(int d, float dir) {
            int i;
            float[] ptr;

            this.delta_f.p[d] = dir;

            if (this.numClamped == 0) {
                return;
            }

            // solve force delta
            final float[] clone = this.delta_f.p.clone();
            SolveClamped(this.delta_f, this.rowPtrs[d]);

            // flip force delta based on direction
            if (dir > 0.0f) {
                ptr = this.delta_f.ToFloatPtr();
                for (i = 0; i < this.numClamped; i++) {
                    ptr[i] = -ptr[i];
                    final int a = 0;
                }
            }
        }

        /*
         ============
         idLCP_Symmetric::CalcAccelDelta

         modifies this->delta_a and uses this->delta_f
         ============
         */
        private void CalcAccelDelta(int d) {
            int j;
            final float[] dot = new float[1];

            // only the not clamped variables, including the current variable, can have a change in acceleration
            for (j = this.numClamped; j <= d; j++) {
                // only the clamped variables and the current variable have a force delta unequal zero
                SIMDProcessor.Dot(dot, this.rowPtrs[j], this.delta_f.ToFloatPtr(), this.numClamped);
                this.delta_a.p[j] = dot[0] + (this.rowPtrs[j].get(d) * this.delta_f.p[d]);
                final int a = 0;
            }
        }

        /*
         ============
         idLCP_Symmetric::ChangeForce

         modifies this->f and uses this->delta_f
         ============
         */
        private void ChangeForce(int d, float step) {
            // only the clamped variables and current variable have a force delta unequal zero
            SIMDProcessor.MulAdd(this.f.ToFloatPtr(), step, this.delta_f.ToFloatPtr(), this.numClamped);
            this.f.p[d] += step * this.delta_f.p[d];
            final int a = 0;
        }

        /*
         ============
         idLCP_Symmetric::ChangeAccel

         modifies this->a and uses this->delta_a
         ============
         */
        private void ChangeAccel(int d, float step) {
            final float[] clampedA = clam(this.a, this.numClamped);
            final float[] clampedDeltaA = clam(this.delta_a, this.numClamped);

            // only the not clamped variables, including the current variable, can have an acceleration unequal zero
            SIMDProcessor.MulAdd(clampedA, step, clampedDeltaA, (d - this.numClamped) + 1);

            unClam(this.a, clampedA);
            final int a = 0;
        }

        private void GetMaxStep(int d, float dir, float[] maxStep, int[] limit, int[] limitSide) {
            int i;
            float s;

            // default to a full step for the current variable
            if (idMath.Fabs(this.delta_a.p[d]) > LCP_DELTA_ACCEL_EPSILON) {
                maxStep[0] = -this.a.p[d] / this.delta_a.p[d];
            } else {
                maxStep[0] = 0.0f;
            }
            limit[0] = d;
            limitSide[0] = 0;

            // test the current variable
            if (dir < 0.0f) {
                if (this.lo.p[d] != -idMath.INFINITY) {
                    s = (this.lo.p[d] - this.f.p[d]) / dir;
                    if (s < maxStep[0]) {
                        maxStep[0] = s;
                        limitSide[0] = -1;
                    }
                }
            } else {
                if (this.hi.p[d] != idMath.INFINITY) {
                    s = (this.hi.p[d] - this.f.p[d]) / dir;
                    if (s < maxStep[0]) {
                        maxStep[0] = s;
                        limitSide[0] = 1;
                    }
                }
            }

            // test the clamped bounded variables
            for (i = this.numUnbounded; i < this.numClamped; i++) {
                if (this.delta_f.p[i] < -LCP_DELTA_FORCE_EPSILON) {
                    // if there is a low boundary
                    if (this.lo.p[i] != -idMath.INFINITY) {
                        s = (this.lo.p[i] - this.f.p[i]) / this.delta_f.p[i];
                        if (s < maxStep[0]) {
                            maxStep[0] = s;
                            limit[0] = i;
                            limitSide[0] = -1;
                        }
                    }
                } else if (this.delta_f.p[i] > LCP_DELTA_FORCE_EPSILON) {
                    // if there is a high boundary
                    if (this.hi.p[i] != idMath.INFINITY) {
                        s = (this.hi.p[i] - this.f.p[i]) / this.delta_f.p[i];
                        if (s < maxStep[0]) {
                            maxStep[0] = s;
                            limit[0] = i;
                            limitSide[0] = 1;
                        }
                    }
                }
            }

            // test the not clamped bounded variables
            for (i = this.numClamped; i < d; i++) {
                if (this.side[i] == -1) {
                    if (this.delta_a.p[i] >= -LCP_DELTA_ACCEL_EPSILON) {
                        continue;
                    }
                } else if (this.side[i] == 1) {
                    if (this.delta_a.p[i] <= LCP_DELTA_ACCEL_EPSILON) {
                        continue;
                    }
                } else {
                    continue;
                }
                // ignore variables for which the force is not allowed to take any substantial value
                if ((this.lo.p[i] >= -LCP_BOUND_EPSILON) && (this.hi.p[i] <= LCP_BOUND_EPSILON)) {
                    continue;
                }
                s = -this.a.p[i] / this.delta_a.p[i];
                if (s < maxStep[0]) {
                    maxStep[0] = s;
                    limit[0] = i;
                    limitSide[0] = 0;
                }
            }
        }
    }

    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */
    public static float[] clam(final idMatX src, final int numClamped) {
       return clam(src.ToFloatPtr(), numClamped * src.GetNumColumns());
    }

    public static float[] clam(final idVecX src, final int numClamped) {
       return clam(src.ToFloatPtr(), numClamped);
    }

    public static float[] clam(final float[] src, final int numClamped) {
        final float[] clamped = new float[src.length - numClamped];

        Nio.arraycopy(src, numClamped, clamped, 0, clamped.length);

        return clamped;
    }

    public static float[] unClam(idMatX dst, final float[] clamArray) {
        return unClam(dst.ToFloatPtr(), clamArray);
    }

    public static float[] unClam(idVecX dst, final float[] clamArray) {
        return unClam(dst.ToFloatPtr(), clamArray);
    }

    public static float[] unClam(float[] dst, final float[] clamArray) {
        Nio.arraycopy(clamArray, 0, dst, dst.length - clamArray.length, clamArray.length);
        return dst;
    }

    public static char[] clam(final char[] src, int numClamped) {
        final char[] clamped = new char[src.length - numClamped];

        Nio.arraycopy(src, numClamped, clamped, 0, clamped.length);

        return clamped;
    }

    public static char[] unClam(char[] dst, char[] clamArray) {
        Nio.arraycopy(clamArray, 0, dst, dst.length - clamArray.length, clamArray.length);
        return dst;
    }
}
