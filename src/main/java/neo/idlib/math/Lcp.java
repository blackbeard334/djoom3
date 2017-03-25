package neo.idlib.math;

import neo.TempDump;
import neo.framework.CVarSystem.idCVar;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMatX;
import neo.idlib.math.Vector.idVecX;

import java.nio.FloatBuffer;

import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.idlib.Lib.idLib;
import static neo.idlib.containers.List.idSwap;
import static neo.idlib.math.Matrix.idMatX.MATX_ALLOCA;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.idlib.math.Vector.idVecX.VECX_ALLOCA;

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
            idLCP lcp = new idLCP_Square();
            lcp.SetMaxIterations(32);
            return lcp;
        }

        // A must be a symmetric matrix
        public static idLCP AllocSymmetric() {
            idLCP lcp = new idLCP_Symmetric();
            lcp.SetMaxIterations(32);
            return lcp;
        }

//public	virtual			~idLCP( void );
        public boolean Solve(final idMatX A, idVecX x, final idVecX b, final idVecX lo, final idVecX hi) {
            return Solve(A, x, b, lo, hi, null);
        }

        public abstract boolean Solve(final idMatX A, idVecX x, final idVecX b, final idVecX lo, final idVecX hi, final int[] boxIndex);

        public void SetMaxIterations(int max) {
            maxIterations = max;
        }

        public int GetMaxIterations() {
            return maxIterations;
        }
    };

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
            int[] limit = new int[1], limitSide = new int[1];
            float dir, s;
            float[] dot = new float[1];
            float[] maxStep = new float[1];
            char[] failed;

            // true when the matrix rows are 16 byte padded
            padded = ((o_m.GetNumRows() + 3) & ~3) == o_m.GetNumColumns();

            assert (padded || o_m.GetNumRows() == o_m.GetNumColumns());
            assert (o_x.GetSize() == o_m.GetNumRows());
            assert (o_b.GetSize() == o_m.GetNumRows());
            assert (o_lo.GetSize() == o_m.GetNumRows());
            assert (o_hi.GetSize() == o_m.GetNumRows());

            // allocate memory for permuted input
            f.SetData(o_m.GetNumRows(), VECX_ALLOCA(o_m.GetNumRows()));
            a.SetData(o_b.GetSize(), VECX_ALLOCA(o_b.GetSize()));
            b.SetData(o_b.GetSize(), VECX_ALLOCA(o_b.GetSize()));
            lo.SetData(o_lo.GetSize(), VECX_ALLOCA(o_lo.GetSize()));
            hi.SetData(o_hi.GetSize(), VECX_ALLOCA(o_hi.GetSize()));
            if (o_boxIndex != null) {
//		boxIndex = (int *)_alloca16( o_x.GetSize() * sizeof( int ) );
//		memcpy( boxIndex, o_boxIndex, o_x.GetSize() * sizeof( int ) );
                boxIndex = new int[o_x.GetSize()];
                System.arraycopy(o_boxIndex, 0, boxIndex, 0, o_x.GetSize());
            } else {
                boxIndex = null;
            }

            // we override the const on o_m here but on exit the matrix is unchanged
            float[] const_cast = o_m.ToFloatPtr();
            m.SetData(o_m.GetNumRows(), o_m.GetNumColumns(), const_cast);
            o_m.FromFloatPtr(const_cast);

            f.Zero();
            a.Zero();
            b = o_b;
            lo = o_lo;
            hi = o_hi;

            // pointers to the rows of m
            rowPtrs = new FloatBuffer[m.GetNumRows()];//rowPtrs = (float **) _alloca16( m.GetNumRows() * sizeof( float * ) );
            for (i = 0; i < m.GetNumRows(); i++) {
                rowPtrs[i] = m.GetRowPtr(i);
            }

            // tells if a variable is at the low boundary, high boundary or inbetween
//	side = (int *) _alloca16( m.GetNumRows() * sizeof( int ) );
            side = new int[m.GetNumRows()];

            // index to keep track of the permutation
//	permuted = (int *) _alloca16( m.GetNumRows() * sizeof( int ) );
            permuted = new int[m.GetNumRows()];
            for (i = 0; i < m.GetNumRows(); i++) {
                permuted[i] = i;
            }

            // permute input so all unbounded variables come first
            numUnbounded = 0;
            for (i = 0; i < m.GetNumRows(); i++) {
                if (lo.p[i] == -idMath.INFINITY && hi.p[i] == idMath.INFINITY) {
                    if (numUnbounded != i) {
                        Swap(numUnbounded, i);
                    }
                    numUnbounded++;
                }
            }

            // permute input so all variables using the boxIndex come last
            boxStartIndex = m.GetNumRows();
            if (boxIndex != null) {
                for (i = m.GetNumRows() - 1; i >= numUnbounded; i--) {
                    if (boxIndex[i] >= 0 && (lo.p[i] != -idMath.INFINITY || hi.p[i] != idMath.INFINITY)) {
                        boxStartIndex--;
                        if (boxStartIndex != i) {
                            Swap(boxStartIndex, i);
                        }
                    }
                }
            }

            // sub matrix for factorization 
            clamped.SetData(m.GetNumRows(), m.GetNumColumns(), MATX_ALLOCA(m.GetNumRows() * m.GetNumColumns()));
            diagonal.SetData(m.GetNumRows(), VECX_ALLOCA(m.GetNumRows()));

            // all unbounded variables are clamped
            numClamped = numUnbounded;

            // if there are unbounded variables
            if (numUnbounded != 0) {

                // factor and solve for unbounded variables
                if (!FactorClamped()) {
			        idLib.common.Printf( "idLCP_Square::Solve: unbounded factorization failed\n" );
                    return false;
                }
                SolveClamped(f, b.ToFloatPtr());

                // if there are no bounded variables we are done
                if (numUnbounded == m.GetNumRows()) {
                    o_x.oSet(f);	// the vector is not permuted
                    return true;
                }
            }

//#ifdef IGNORE_UNSATISFIABLE_VARIABLES
            int numIgnored = 0;
//#endif

            // allocate for delta force and delta acceleration
            delta_f.SetData(m.GetNumRows(), VECX_ALLOCA(m.GetNumRows()));
            delta_a.SetData(m.GetNumRows(), VECX_ALLOCA(m.GetNumRows()));

            // solve for bounded variables
            failed = null;
            for (i = numUnbounded; i < m.GetNumRows(); i++) {

                // once we hit the box start index we can initialize the low and high boundaries of the variables using the box index
                if (i == boxStartIndex) {
                    for (j = 0; j < boxStartIndex; j++) {
                        o_x.p[permuted[j]] = f.p[j];
                    }
                    for (j = boxStartIndex; j < m.GetNumRows(); j++) {
                        s = o_x.p[boxIndex[j]];
                        if (lo.p[j] != -idMath.INFINITY) {
                            lo.p[j] = -idMath.Fabs(lo.p[j] * s);
                        }
                        if (hi.p[j] != idMath.INFINITY) {
                            hi.p[j] = idMath.Fabs(hi.p[j] * s);
                        }
                    }
                }

                // calculate acceleration for current variable
                SIMDProcessor.Dot(dot, rowPtrs[i], f.ToFloatPtr(), i);
                a.p[i] = dot[0] - b.p[i];

                // if already at the low boundary
                if (lo.p[i] >= -LCP_BOUND_EPSILON && a.p[i] >= -LCP_ACCEL_EPSILON) {
                    side[i] = -1;
                    continue;
                }

                // if already at the high boundary
                if (hi.p[i] <= LCP_BOUND_EPSILON && a.p[i] <= LCP_ACCEL_EPSILON) {
                    side[i] = 1;
                    continue;
                }

                // if inside the clamped region
                if (idMath.Fabs(a.p[i]) <= LCP_ACCEL_EPSILON) {
                    side[i] = 0;
                    AddClamped(i);
                    continue;
                }

                // drive the current variable into a valid region
                for (n = 0; n < maxIterations; n++) {

                    // direction to move
                    if (a.p[i] <= 0.0f) {
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
                        lo.p[i] = hi.p[i] = 0.0f;
                        f.p[i] = 0.0f;
                        side[i] = -1;
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
                    side[limit[0]] = limitSide[0];
                    switch (limitSide[0]) {
                        case 0: {
                            a.p[limit[0]] = 0.0f;
                            AddClamped(limit[0]);
                            break;
                        }
                        case -1: {
                            f.p[limit[0]] = lo.p[limit[0]];
                            if (limit[0] != i) {
                                RemoveClamped(limit[0]);
                            }
                            break;
                        }
                        case 1: {
                            f.p[limit[0]] = hi.p[limit[0]];
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
			        idLib.common.Printf( "idLCP_Symmetric::Solve: %d of %d bounded variables ignored\n", numIgnored, m.GetNumRows() - numUnbounded );
                }
            }
//#endif

            // if failed clear remaining forces
            if (failed != null) {
                if (lcp_showFailures.GetBool()) {
			        idLib.common.Printf( "idLCP_Square::Solve: %s (%d of %d bounded variables ignored)\n", failed, m.GetNumRows() - i, m.GetNumRows() - numUnbounded );
                }
                for (j = i; j < m.GetNumRows(); j++) {
                    f.p[j] = 0.0f;
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
            for (i = 0; i < f.GetSize(); i++) {
                o_x.p[permuted[i]] = f.p[i];
            }

            // unpermute original matrix
            for (i = 0; i < m.GetNumRows(); i++) {
                for (j = 0; j < m.GetNumRows(); j++) {
                    if (permuted[j] == i) {
                        break;
                    }
                }
                if (i != j) {
                    m.SwapColumns(i, j);
                    idSwap(permuted, permuted, i, j);
                }
            }

            return true;
        }

        private boolean FactorClamped() {
            int i, j, k;
            float s, d;

            for (i = 0; i < numClamped; i++) {
//		memcpy( clamped[i], rowPtrs[i], numClamped * sizeof( float ) );
                clamped.arraycopy(rowPtrs[i], i, numClamped);//TODO:check two dimensional array
            }

            for (i = 0; i < numClamped; i++) {

                s = idMath.Fabs(clamped.oGet(i)[i]);

                if (s == 0.0f) {
                    return false;
                }

                diagonal.p[i] = d = 1.0f / clamped.oGet(i)[i];
                for (j = i + 1; j < numClamped; j++) {
//			clamped[j][i] *= d;
                    clamped.oMulSet(j, i, d);
                }

                for (j = i + 1; j < numClamped; j++) {
                    d = clamped.oGet(j)[i];
                    for (k = i + 1; k < numClamped; k++) {
                        clamped.oMinSet(j, k, d * clamped.oGet(i)[k]);
                    }
                }
            }

            return true;
        }

        void SolveClamped(idVecX x, final float[] b) {
            int i, j;
            float sum;

            // solve L
            for (i = 0; i < numClamped; i++) {
                sum = b[i];
                for (j = 0; j < i; j++) {
                    sum -= clamped.oGet(i)[j] * x.p[j];
                }
                x.p[i] = sum;
            }

            // solve U
            for (i = numClamped - 1; i >= 0; i--) {
                sum = x.p[i];
                for (j = i + 1; j < numClamped; j++) {
                    sum -= clamped.oGet(i)[j] * x.p[j];
                }
                x.p[i] = sum * diagonal.p[i];
            }
        }

        void Swap(int i, int j) {

            if (i == j) {
                return;
            }

            idSwap(rowPtrs, rowPtrs, i, j);
            m.SwapColumns(i, j);
            b.SwapElements(i, j);
            lo.SwapElements(i, j);
            hi.SwapElements(i, j);
            a.SwapElements(i, j);
            f.SwapElements(i, j);
            if (null != boxIndex) {
                idSwap(boxIndex, boxIndex, i, j);
            }
            idSwap(side, side, i, j);
            idSwap(permuted, permuted, i, j);
        }

        void AddClamped(int r) {
            int i, j;
            float sum;

            assert (r >= numClamped);

            // add a row at the bottom and a column at the right of the factored
            // matrix for the clamped variables
            Swap(numClamped, r);

            // add row to L
            for (i = 0; i < numClamped; i++) {
                sum = rowPtrs[numClamped].get(i);
                for (j = 0; j < i; j++) {
                    sum -= clamped.oGet(numClamped)[j] * clamped.oGet(j)[i];
                }
                clamped.oSet(numClamped, i, sum * diagonal.p[i]);
            }

            // add column to U
            for (i = 0; i <= numClamped; i++) {
                sum = rowPtrs[i].get(numClamped);
                for (j = 0; j < i; j++) {
                    sum -= clamped.oGet(i)[j] * clamped.oGet(j)[numClamped];
                }
                clamped.oSet(i, numClamped, sum);
            }

            diagonal.p[numClamped] = 1.0f / clamped.oGet(numClamped)[numClamped];

            numClamped++;
        }

        void RemoveClamped(int r) {
            int i, j;
            float[] y0, y1, z0, z1;
            float diag, beta0, beta1, p0, p1, q0, q1, d;

            assert (r < numClamped);

            numClamped--;

            // no need to swap and update the factored matrix when the last row and column are removed
            if (r == numClamped) {
                return;
            }

//	y0 = (float *) _alloca16( numClamped * sizeof( float ) );
//	z0 = (float *) _alloca16( numClamped * sizeof( float ) );
//	y1 = (float *) _alloca16( numClamped * sizeof( float ) );
//	z1 = (float *) _alloca16( numClamped * sizeof( float ) );
            y0 = new float[numClamped];
            z0 = new float[numClamped];
            y1 = new float[numClamped];
            z1 = new float[numClamped];

            // the row/column need to be subtracted from the factorization
            for (i = 0; i < numClamped; i++) {
                y0[i] = -rowPtrs[i].get(r);
            }

//	memset( y1, 0, numClamped * sizeof( float ) );
            y1[r] = 1.0f;

//	memset( z0, 0, numClamped * sizeof( float ) );
            z0[r] = 1.0f;

            for (i = 0; i < numClamped; i++) {
                z1[i] = -rowPtrs[r].get(i);
            }

            // swap the to be removed row/column with the last row/column
            Swap(r, numClamped);

            // the swapped last row/column need to be added to the factorization
            for (i = 0; i < numClamped; i++) {
                y0[i] += rowPtrs[i].get(r);
            }

            for (i = 0; i < numClamped; i++) {
                z1[i] += rowPtrs[r].get(i);
            }
            z1[r] = 0.0f;

            // update the beginning of the to be updated row and column
            for (i = 0; i < r; i++) {
                p0 = y0[i];
                beta1 = z1[i] * diagonal.p[i];

                clamped.oPluSet(i, r, p0);
                for (j = i + 1; j < numClamped; j++) {
                    z1[j] -= beta1 * clamped.oGet(i)[j];
                }
                for (j = i + 1; j < numClamped; j++) {
                    y0[j] -= p0 * clamped.oGet(j)[i];
                }
                clamped.oPluSet(r, i, beta1);
            }

            // update the lower right corner starting at r,r
            for (i = r; i < numClamped; i++) {
                diag = clamped.oGet(i)[i];

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

                clamped.oSet(i, i, diag);
                diagonal.p[i] = d;

                for (j = i + 1; j < numClamped; j++) {

                    d = clamped.oGet(i)[j];

                    d += p0 * z0[j];
                    z0[j] -= beta0 * d;

                    d += q0 * z1[j];
                    z1[j] -= beta1 * d;

                    clamped.oSet(i, j, d);
                }

                for (j = i + 1; j < numClamped; j++) {

                    d = clamped.oGet(j)[i];

                    y0[j] -= p0 * d;
                    d += beta0 * y0[j];

                    y1[j] -= q0 * d;
                    d += beta1 * y1[j];

                    clamped.oSet(j, i, d);
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

            delta_f.p[d] = dir;

            if (numClamped == 0) {
                return;
            }

            // get column d of matrix
//	ptr = (float *) _alloca16( numClamped * sizeof( float ) );
            ptr = new float[numClamped];
            for (i = 0; i < numClamped; i++) {
                ptr[i] = rowPtrs[i].get(d);
            }

            // solve force delta
            SolveClamped(delta_f, ptr);

            // flip force delta based on direction
            if (dir > 0.0f) {
                ptr = delta_f.ToFloatPtr();
                for (i = 0; i < numClamped; i++) {
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
            float[] dot = new float[1];

            // only the not clamped variables, including the current variable, can have a change in acceleration
            for (j = numClamped; j <= d; j++) {
                // only the clamped variables and the current variable have a force delta unequal zero
                SIMDProcessor.Dot(dot, rowPtrs[j], delta_f.ToFloatPtr(), numClamped);
                delta_a.p[j] = dot[0] + rowPtrs[j].get(d) * delta_f.p[d];
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
            SIMDProcessor.MulAdd(f.ToFloatPtr(), step, delta_f.ToFloatPtr(), numClamped);
            f.p[d] += step * delta_f.p[d];
        }

        /*
         ============
         idLCP_Square::ChangeAccel

         modifies this->a and uses this->delta_a
         ============
         */
        private void ChangeAccel(int d, float step) {
            float[] clampedA = clam(a, numClamped);
            float[] clampedDeltaA = clam(delta_a, numClamped);

            // only the not clamped variables, including the current variable, can have an acceleration unequal zero
            SIMDProcessor.MulAdd(clampedA, step, clampedDeltaA, d - numClamped + 1);

            unClam(a, clampedA);
            unClam(delta_a, clampedDeltaA);
        }

        private void GetMaxStep(int d, float dir, float[] maxStep, int[] limit, int[] limitSide) {
            int i;
            float s;

            // default to a full step for the current variable
            if (idMath.Fabs(delta_a.p[d]) > LCP_DELTA_ACCEL_EPSILON) {
                maxStep[0] = -a.p[d] / delta_a.p[d];
            } else {
                maxStep[0] = 0.0f;
            }
            limit[0] = d;
            limitSide[0] = 0;

            // test the current variable
            if (dir < 0.0f) {
                if (lo.p[d] != -idMath.INFINITY) {
                    s = (lo.p[d] - f.p[d]) / dir;
                    if (s < maxStep[0]) {
                        maxStep[0] = s;
                        limitSide[0] = -1;
                    }
                }
            } else {
                if (hi.p[d] != idMath.INFINITY) {
                    s = (hi.p[d] - f.p[d]) / dir;
                    if (s < maxStep[0]) {
                        maxStep[0] = s;
                        limitSide[0] = 1;
                    }
                }
            }

            // test the clamped bounded variables
            for (i = numUnbounded; i < numClamped; i++) {
                if (delta_f.p[i] < -LCP_DELTA_FORCE_EPSILON) {
                    // if there is a low boundary
                    if (lo.p[i] != -idMath.INFINITY) {
                        s = (lo.p[i] - f.p[i]) / delta_f.p[i];
                        if (s < maxStep[0]) {
                            maxStep[0] = s;
                            limit[0] = i;
                            limitSide[0] = -1;
                        }
                    }
                } else if (delta_f.p[i] > LCP_DELTA_FORCE_EPSILON) {
                    // if there is a high boundary
                    if (hi.p[i] != idMath.INFINITY) {
                        s = (hi.p[i] - f.p[i]) / delta_f.p[i];
                        if (s < maxStep[0]) {
                            maxStep[0] = s;
                            limit[0] = i;
                            limitSide[0] = 1;
                        }
                    }
                }
            }

            // test the not clamped bounded variables
            for (i = numClamped; i < d; i++) {
                if (side[i] == -1) {
                    if (delta_a.p[i] >= -LCP_DELTA_ACCEL_EPSILON) {
                        continue;
                    }
                } else if (side[i] == 1) {
                    if (delta_a.p[i] <= LCP_DELTA_ACCEL_EPSILON) {
                        continue;
                    }
                } else {
                    continue;
                }
                // ignore variables for which the force is not allowed to take any substantial value
                if (lo.p[i] >= -LCP_BOUND_EPSILON && hi.p[i] <= LCP_BOUND_EPSILON) {
                    continue;
                }
                s = -a.p[i] / delta_a.p[i];
                if (s < maxStep[0]) {
                    maxStep[0] = s;
                    limit[0] = i;
                    limitSide[0] = 0;
                }
            }
        }
    };

    //===============================================================
    //                                                        M
    //  idLCP_Symmetric                                      MrE
    //                                                        E
    //===============================================================
    static class idLCP_Symmetric extends idLCP {

        private idMatX        m;                    // original matrix
        private idVecX        b;                    // right hand side
        private idVecX        lo, hi;               // low and high bounds
        private idVecX        f, a;                 // force and acceleration
        private idVecX        delta_f, delta_a;     // delta force and delta acceleration
        private idMatX        clamped;              // LDLt factored sub matrix for clamped variables
        private idVecX        diagonal;             // reciprocal of diagonal of LDLt factored sub matrix for clamped variables
        private idVecX        solveCache1;          // intermediate result cached in SolveClamped
        private idVecX        solveCache2;          // "
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
            m = new idMatX();
            b = new idVecX();
            lo = new idVecX();
            hi = new idVecX();
            f = new idVecX();
            a = new idVecX();
            delta_f = new idVecX();
            delta_a = new idVecX();
            clamped = new idMatX();
            diagonal = new idVecX();
            solveCache1 = new idVecX();
            solveCache2 = new idVecX();
        }

        @Override
        public boolean Solve(idMatX o_m, idVecX o_x, idVecX o_b, idVecX o_lo, idVecX o_hi, int[] o_boxIndex) {
            int i, j, n, boxStartIndex;
            int[] limit = new int[1], limitSide = new int[1];
            float dir, s;
            float[] dot = new float[1];
            float[] maxStep = new float[1];
            char[] failed;

            // true when the matrix rows are 16 byte padded
            padded = ((o_m.GetNumRows() + 3) & ~3) == o_m.GetNumColumns();

            assert (padded || o_m.GetNumRows() == o_m.GetNumColumns());
            assert (o_x.GetSize() == o_m.GetNumRows());
            assert (o_b.GetSize() == o_m.GetNumRows());
            assert (o_lo.GetSize() == o_m.GetNumRows());
            assert (o_hi.GetSize() == o_m.GetNumRows());

            // allocate memory for permuted input
            f.SetData(o_m.GetNumRows(), VECX_ALLOCA(o_m.GetNumRows()));
            a.SetData(o_b.GetSize(), VECX_ALLOCA(o_b.GetSize()));
            b.SetData(o_b.GetSize(), VECX_ALLOCA(o_b.GetSize()));
            lo.SetData(o_lo.GetSize(), VECX_ALLOCA(o_lo.GetSize()));
            hi.SetData(o_hi.GetSize(), VECX_ALLOCA(o_hi.GetSize()));
            if (null != o_boxIndex) {
//		boxIndex = (int *)_alloca16( o_x.GetSize() * sizeof( int ) );
                boxIndex = new int[o_x.GetSize()];
//		memcpy( boxIndex, o_boxIndex, o_x.GetSize() * sizeof( int ) );
                System.arraycopy(o_boxIndex, 0, boxIndex, 0, o_x.GetSize());
            } else {
                boxIndex = null;
            }

            // we override the const on o_m here but on exit the matrix is unchanged
            m.SetData(o_m.GetNumRows(), o_m.GetNumColumns(), o_m.oGet(0));
            f.Zero();
            a.Zero();
            b.oSet(o_b);
            lo.oSet(o_lo);
            hi.oSet(o_hi);

            // pointers to the rows of m
            rowPtrs = new FloatBuffer[m.GetNumRows()];//rowPtrs = (float **) _alloca16( m.GetNumRows() * sizeof( float * ) );
            for (i = 0; i < m.GetNumRows(); i++) {
                rowPtrs[i] = m.GetRowPtr(i);
            }

            // tells if a variable is at the low boundary, high boundary or inbetween
//	side = (int *) _alloca16( m.GetNumRows() * sizeof( int ) );
            side = new int[m.GetNumRows()];

            // index to keep track of the permutation
//	permuted = (int *) _alloca16( m.GetNumRows() * sizeof( int ) );
            permuted = new int[m.GetNumRows()];
            for (i = 0; i < m.GetNumRows(); i++) {
                permuted[i] = i;
            }

            // permute input so all unbounded variables come first
            numUnbounded = 0;
            for (i = 0; i < m.GetNumRows(); i++) {
                if (lo.p[i] == -idMath.INFINITY && hi.p[i] == idMath.INFINITY) {
                    if (numUnbounded != i) {
                        Swap(numUnbounded, i);
                    }
                    numUnbounded++;
                }
            }

            // permute input so all variables using the boxIndex come last
            boxStartIndex = m.GetNumRows();
            if (null != boxIndex) {
                for (i = m.GetNumRows() - 1; i >= numUnbounded; i--) {
                    if (boxIndex[i] >= 0 && (lo.p[i] != -idMath.INFINITY || hi.p[i] != idMath.INFINITY)) {
                        boxStartIndex--;
                        if (boxStartIndex != i) {
                            Swap(boxStartIndex, i);
                        }
                    }
                }
            }

            // sub matrix for factorization 
            clamped.SetData(m.GetNumRows(), m.GetNumColumns(), MATX_ALLOCA(m.GetNumRows() * m.GetNumColumns()));
            diagonal.SetData(m.GetNumRows(), VECX_ALLOCA(m.GetNumRows()));
            solveCache1.SetData(m.GetNumRows(), VECX_ALLOCA(m.GetNumRows()));
            solveCache2.SetData(m.GetNumRows(), VECX_ALLOCA(m.GetNumRows()));

            // all unbounded variables are clamped
            numClamped = numUnbounded;

            // if there are unbounded variables
            if (0 != numUnbounded) {

                // factor and solve for unbounded variables
                if (!FactorClamped()) {
			        idLib.common.Printf( "idLCP_Symmetric::Solve: unbounded factorization failed\n" );
                    return false;
                }
                SolveClamped(f, b.ToFloatPtr());

                // if there are no bounded variables we are done
                if (numUnbounded == m.GetNumRows()) {
                    o_x.oSet(f);	// the vector is not permuted
                    return true;
                }
            }

//#ifdef IGNORE_UNSATISFIABLE_VARIABLES
            int numIgnored = 0;
//#endif

            // allocate for delta force and delta acceleration
            delta_f.SetData(m.GetNumRows(), VECX_ALLOCA(m.GetNumRows()));
            delta_a.SetData(m.GetNumRows(), VECX_ALLOCA(m.GetNumRows()));

            // solve for bounded variables
            failed = null;
            for (i = numUnbounded; i < m.GetNumRows(); i++) {

                clampedChangeStart = 0;

                // once we hit the box start index we can initialize the low and high boundaries of the variables using the box index
                if (i == boxStartIndex) {
                    for (j = 0; j < boxStartIndex; j++) {
                        o_x.p[permuted[j]] = f.p[j];
                    }
                    for (j = boxStartIndex; j < m.GetNumRows(); j++) {
                        s = o_x.p[boxIndex[j]];
                        if (lo.p[j] != -idMath.INFINITY) {
                            lo.p[j] = -idMath.Fabs(lo.p[j] * s);
                        }
                        if (hi.p[j] != idMath.INFINITY) {
                            hi.p[j] = idMath.Fabs(hi.p[j] * s);
                        }
                    }
                }

                // calculate acceleration for current variable
                SIMDProcessor.Dot(dot, rowPtrs[i], f.ToFloatPtr(), i);
                a.p[i] = dot[0] - b.p[i];

                // if already at the low boundary
                if (lo.p[i] >= -LCP_BOUND_EPSILON && a.p[i] >= -LCP_ACCEL_EPSILON) {
                    side[i] = -1;
                    continue;
                }

                // if already at the high boundary
                if (hi.p[i] <= LCP_BOUND_EPSILON && a.p[i] <= LCP_ACCEL_EPSILON) {
                    side[i] = 1;
                    continue;
                }

                // if inside the clamped region
                if (idMath.Fabs(a.p[i]) <= LCP_ACCEL_EPSILON) {
                    side[i] = 0;
                    AddClamped(i, false);
                    continue;
                }

                // drive the current variable into a valid region
                for (n = 0; n < maxIterations; n++) {

                    // direction to move
                    if (a.p[i] <= 0.0f) {
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
                        lo.p[i] = hi.p[i] = 0.0f;
                        f.p[i] = 0.0f;
                        side[i] = -1;
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
                    side[limit[0]] = limitSide[0];
                    switch (limitSide[0]) {
                        case 0: {
                            a.p[limit[0]] = 0.0f;
                            AddClamped(limit[0], (limit[0] == i));
                            break;
                        }
                        case -1: {
                            f.p[limit[0]] = lo.p[limit[0]];
                            if (limit[0] != i) {
                                RemoveClamped(limit[0]);
                            }
                            break;
                        }
                        case 1: {
                            f.p[limit[0]] = hi.p[limit[0]];
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

                if (n >= maxIterations) {
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
			        idLib.common.Printf( "idLCP_Symmetric::Solve: %d of %d bounded variables ignored\n", numIgnored, m.GetNumRows() - numUnbounded );
                }
            }
//#endif

            // if failed clear remaining forces
            if (null != failed) {
                if (lcp_showFailures.GetBool()) {
			        idLib.common.Printf( "idLCP_Symmetric::Solve: %s (%d of %d bounded variables ignored)\n", failed, m.GetNumRows() - i, m.GetNumRows() - numUnbounded );
                }
                for (j = i; j < m.GetNumRows(); j++) {
                    f.p[j] = 0.0f;
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
            for (i = 0; i < f.GetSize(); i++) {
                o_x.p[permuted[i]] = f.p[i];
            }

            // unpermute original matrix
            for (i = 0; i < m.GetNumRows(); i++) {
                for (j = 0; j < m.GetNumRows(); j++) {
                    if (permuted[j] == i) {
                        break;
                    }
                }
                if (i != j) {
                    m.SwapColumns(i, j);
                    idSwap(permuted, i, j);
                }
            }

            return true;
        }

        private boolean FactorClamped() {

            clampedChangeStart = 0;

            for (int i = 0; i < numClamped; i++) {
//		memcpy( clamped[i], rowPtrs[i], numClamped * sizeof( float ) );
                clamped.arraycopy(rowPtrs[i], i, numClamped);
                int a = 0;
            }
            boolean b = SIMDProcessor.MatX_LDLTFactor(clamped, diagonal, numClamped);
            int a = 0;
            return b;
        }

        private void SolveClamped(idVecX x, final float[] b) {

            // solve L
            SIMDProcessor.MatX_LowerTriangularSolve(clamped, solveCache1.ToFloatPtr(), b, numClamped, clampedChangeStart);

            // solve D
            SIMDProcessor.Mul(solveCache2.ToFloatPtr(), solveCache1.ToFloatPtr(), diagonal.ToFloatPtr(), numClamped);

            // solve Lt
            SIMDProcessor.MatX_LowerTriangularSolveTranspose(clamped, x.ToFloatPtr(), solveCache2.ToFloatPtr(), numClamped);

            clampedChangeStart = numClamped;
        }

        private void SolveClamped(idVecX x, final FloatBuffer b) {
            SolveClamped(x, TempDump.fbtofa(b));
        }

        private void Swap(int i, int j) {

            if (i == j) {
                return;
            }

            idSwap(rowPtrs, rowPtrs, i, j);
            m.SwapColumns(i, j);
            b.SwapElements(i, j);
            lo.SwapElements(i, j);
            hi.SwapElements(i, j);
            a.SwapElements(i, j);
            f.SwapElements(i, j);
            if (null != boxIndex) {
                idSwap(boxIndex, boxIndex, i, j);
            }
            idSwap(side, side, i, j);
            idSwap(permuted, permuted, i, j);
        }

        private void AddClamped(int r, boolean useSolveCache) {
            float d;
            float[] dot = new float[1];

            assert (r >= numClamped);

            if (numClamped < clampedChangeStart) {
                clampedChangeStart = numClamped;
            }

            // add a row at the bottom and a column at the right of the factored
            // matrix for the clamped variables
            Swap(numClamped, r);

            // solve for v in L * v = rowPtr[numClamped]
            if (useSolveCache) {

                // the lower triangular solve was cached in SolveClamped called by CalcForceDelta
//                memcpy(clamped[numClamped], solveCache2.ToFloatPtr(), numClamped * sizeof(float));
                clamped.arraycopy(solveCache2.ToFloatPtr(), numClamped, numClamped);
                int a = 0;
                // calculate row dot product
                SIMDProcessor.Dot(dot, solveCache2.ToFloatPtr(), solveCache1.ToFloatPtr(), numClamped);

            } else {
                float[] v = new float[numClamped];//(float *) _alloca16(numClamped * sizeof(float));
                float[] clampedArray = clam(clamped, numClamped);

                SIMDProcessor.MatX_LowerTriangularSolve(clamped, v, rowPtrs[numClamped], numClamped);
                // add bottom row to L
                SIMDProcessor.Mul(clampedArray, v, diagonal.ToFloatPtr(), numClamped);
                // calculate row dot product
                SIMDProcessor.Dot(dot, clampedArray, v, numClamped);

                unClam(clamped, clampedArray);
            }

            // update diagonal[numClamped]
            d = rowPtrs[numClamped].get(numClamped) - dot[0];

            if (d == 0.0f) {
                idLib.common.Printf("idLCP_Symmetric::AddClamped: updating factorization failed\n");
                numClamped++;
                return;
            }

            clamped.oSet(numClamped, numClamped, d);
            diagonal.p[numClamped] = 1.0f / d;

            numClamped++;
        }

        private void RemoveClamped(int r) {
            int i, j, n;
            float[] addSub, v, v1, v2;
            float[] dot = new float[1];
            float sum, diag, newDiag, invNewDiag, p1, p2, alpha1, alpha2, beta1, beta2;
            FloatBuffer original, ptr;

            assert (r < numClamped);

            if (r < clampedChangeStart) {
                clampedChangeStart = r;
            }

            numClamped--;

            // no need to swap and update the factored matrix when the last row and column are removed
            if (r == numClamped) {
                return;
            }

            // swap the to be removed row/column with the last row/column
            Swap(r, numClamped);

            // update the factored matrix
//	addSub = (float *) _alloca16( numClamped * sizeof( float ) );
            addSub = new float[numClamped];

            if (r == 0) {

                if (numClamped == 1) {
                    diag = rowPtrs[0].get(0);
                    if (diag == 0.0f) {
                        idLib.common.Printf("idLCP_Symmetric::RemoveClamped: updating factorization failed\n");
                        return;
                    }
                    clamped.oSet(0, 0, diag);
                    diagonal.p[0] = 1.0f / diag;
                    return;
                }

                // calculate the row/column to be added to the lower right sub matrix starting at (r, r)
                original = rowPtrs[numClamped];
                ptr = rowPtrs[r];
                addSub[0] = ptr.get(0) - original.get(numClamped);
                for (i = 1; i < numClamped; i++) {
                    addSub[i] = ptr.get(i) - original.get(i);
                }

            } else {

                v = new float[numClamped];//= (float *) _alloca16( numClamped * sizeof( float ) );
                float[] clampedArray = clam(clamped, r);

                // solve for v in L * v = rowPtr[r]
                SIMDProcessor.MatX_LowerTriangularSolve(clamped, v, rowPtrs[r], r);

                // update removed row
                SIMDProcessor.Mul(clampedArray, v, diagonal.ToFloatPtr(), r);

                // if the last row/column of the matrix is updated
                if (r == numClamped - 1) {
                    // only calculate new diagonal
                    SIMDProcessor.Dot(dot, clampedArray, v, r);
                    diag = rowPtrs[r].get(r) - dot[0];
                    if (diag == 0.0f) {
				        idLib.common.Printf( "idLCP_Symmetric::RemoveClamped: updating factorization failed\n" );
                        return;
                    }
                    unClam(clamped, clampedArray);
                    clamped.oSet(r, r, diag);
                    diagonal.p[r] = 1.0f / diag;
                    return;
                }

                // calculate the row/column to be added to the lower right sub matrix starting at (r, r)
                for (i = 0; i < r; i++) {
                    v[i] = clamped.oGet(r)[i] * clamped.oGet(i)[i];
                }
                for (i = r; i < numClamped; i++) {
                    if (i == r) {
                        sum = clamped.oGet(r)[r];
                    } else {
                        sum = clamped.oGet(r)[r] * clamped.oGet(i)[r];
                    }
                    ptr = clamped.GetRowPtr(i);
                    for (j = 0; j < r; j++) {
                        sum += ptr.get(j) * v[j];
                    }
                    addSub[i] = rowPtrs[r].get(i) - sum;
                }
            }

            // add row/column to the lower right sub matrix starting at (r, r)
//	v1 = (float *) _alloca16( numClamped * sizeof( float ) );
//	v2 = (float *) _alloca16( numClamped * sizeof( float ) );
            v1 = new float[numClamped];
            v2 = new float[numClamped];

            diag = idMath.SQRT_1OVER2;
            v1[r] = (0.5f * addSub[r] + 1.0f) * diag;
            v2[r] = (0.5f * addSub[r] - 1.0f) * diag;
            for (i = r + 1; i < numClamped; i++) {
                v1[i] = v2[i] = addSub[i] * diag;
            }

            alpha1 = 1.0f;
            alpha2 = -1.0f;

            // simultaneous update/downdate of the sub matrix starting at (r, r)
            n = clamped.GetNumColumns();
            for (i = r; i < numClamped; i++) {

                diag = clamped.oGet(i)[i];
                p1 = v1[i];
                newDiag = diag + alpha1 * p1 * p1;

                if (newDiag == 0.0f) {
			        idLib.common.Printf( "idLCP_Symmetric::RemoveClamped: updating factorization failed\n" );
                    return;
                }

                alpha1 /= newDiag;
                beta1 = p1 * alpha1;
                alpha1 *= diag;

                diag = newDiag;
                p2 = v2[i];
                newDiag = diag + alpha2 * p2 * p2;

                if (newDiag == 0.0f) {
			        idLib.common.Printf( "idLCP_Symmetric::RemoveClamped: updating factorization failed\n" );
                    return;
                }

                clamped.oSet(i, i, newDiag);
                diagonal.p[i] = invNewDiag = 1.0f / newDiag;

                alpha2 *= invNewDiag;
                beta2 = p2 * alpha2;
                alpha2 *= diag;

                // update column below diagonal (i,i)
                ptr = clamped.GetRowPtr(i);

                for (j = i + 1; j < numClamped - 1; j += 2) {

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
                    int a = 0;
                }

                for (; j < numClamped; j++) {

                    sum = ptr.get(j * n);

                    v1[j] -= p1 * sum;
                    sum += beta1 * v1[j];

                    v2[j] -= p2 * sum;
                    sum += beta2 * v2[j];

                    ptr.put(j * n, sum);
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

            delta_f.p[d] = dir;

            if (numClamped == 0) {
                return;
            }

            // solve force delta
            float[] clone = delta_f.p.clone();
            SolveClamped(delta_f, rowPtrs[d]);

            // flip force delta based on direction
            if (dir > 0.0f) {
                ptr = delta_f.ToFloatPtr();
                for (i = 0; i < numClamped; i++) {
                    ptr[i] = -ptr[i];
                    int a = 0;
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
            float[] dot = new float[1];

            // only the not clamped variables, including the current variable, can have a change in acceleration
            for (j = numClamped; j <= d; j++) {
                // only the clamped variables and the current variable have a force delta unequal zero
                SIMDProcessor.Dot(dot, rowPtrs[j], delta_f.ToFloatPtr(), numClamped);
                delta_a.p[j] = dot[0] + rowPtrs[j].get(d) * delta_f.p[d];
                int a = 0;
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
            SIMDProcessor.MulAdd(f.ToFloatPtr(), step, delta_f.ToFloatPtr(), numClamped);
            f.p[d] += step * delta_f.p[d];
            int a = 0;
        }

        /*
         ============
         idLCP_Symmetric::ChangeAccel

         modifies this->a and uses this->delta_a
         ============
         */
        private void ChangeAccel(int d, float step) {
            float[] clampedA = clam(a, numClamped);
            float[] clampedDeltaA = clam(delta_a, numClamped);

            // only the not clamped variables, including the current variable, can have an acceleration unequal zero
            SIMDProcessor.MulAdd(clampedA, step, clampedDeltaA, d - numClamped + 1);

            unClam(a, clampedA);
            unClam(delta_a, clampedDeltaA);
            int a = 0;
        }

        private void GetMaxStep(int d, float dir, float[] maxStep, int[] limit, int[] limitSide) {
            int i;
            float s;

            // default to a full step for the current variable
            if (idMath.Fabs(delta_a.p[d]) > LCP_DELTA_ACCEL_EPSILON) {
                maxStep[0] = -a.p[d] / delta_a.p[d];
            } else {
                maxStep[0] = 0.0f;
            }
            limit[0] = d;
            limitSide[0] = 0;

            // test the current variable
            if (dir < 0.0f) {
                if (lo.p[d] != -idMath.INFINITY) {
                    s = (lo.p[d] - f.p[d]) / dir;
                    if (s < maxStep[0]) {
                        maxStep[0] = s;
                        limitSide[0] = -1;
                    }
                }
            } else {
                if (hi.p[d] != idMath.INFINITY) {
                    s = (hi.p[d] - f.p[d]) / dir;
                    if (s < maxStep[0]) {
                        maxStep[0] = s;
                        limitSide[0] = 1;
                    }
                }
            }

            // test the clamped bounded variables
            for (i = numUnbounded; i < numClamped; i++) {
                if (delta_f.p[i] < -LCP_DELTA_FORCE_EPSILON) {
                    // if there is a low boundary
                    if (lo.p[i] != -idMath.INFINITY) {
                        s = (lo.p[i] - f.p[i]) / delta_f.p[i];
                        if (s < maxStep[0]) {
                            maxStep[0] = s;
                            limit[0] = i;
                            limitSide[0] = -1;
                        }
                    }
                } else if (delta_f.p[i] > LCP_DELTA_FORCE_EPSILON) {
                    // if there is a high boundary
                    if (hi.p[i] != idMath.INFINITY) {
                        s = (hi.p[i] - f.p[i]) / delta_f.p[i];
                        if (s < maxStep[0]) {
                            maxStep[0] = s;
                            limit[0] = i;
                            limitSide[0] = 1;
                        }
                    }
                }
            }

            // test the not clamped bounded variables
            for (i = numClamped; i < d; i++) {
                if (side[i] == -1) {
                    if (delta_a.p[i] >= -LCP_DELTA_ACCEL_EPSILON) {
                        continue;
                    }
                } else if (side[i] == 1) {
                    if (delta_a.p[i] <= LCP_DELTA_ACCEL_EPSILON) {
                        continue;
                    }
                } else {
                    continue;
                }
                // ignore variables for which the force is not allowed to take any substantial value
                if (lo.p[i] >= -LCP_BOUND_EPSILON && hi.p[i] <= LCP_BOUND_EPSILON) {
                    continue;
                }
                s = -a.p[i] / delta_a.p[i];
                if (s < maxStep[0]) {
                    maxStep[0] = s;
                    limit[0] = i;
                    limitSide[0] = 0;
                }
            }
        }
    };

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
        float[] clamped = new float[src.length - numClamped];

        System.arraycopy(src, numClamped, clamped, 0, clamped.length);

        return clamped;
    }

    public static float[] unClam(idMatX dst, final float[] clamArray) {
        return unClam(dst.ToFloatPtr(), clamArray);
    }

    public static float[] unClam(idVecX dst, final float[] clamArray) {
        return unClam(dst.ToFloatPtr(), clamArray);
    }

    public static float[] unClam(float[] dst, final float[] clamArray) {
        System.arraycopy(clamArray, 0, dst, dst.length - clamArray.length, clamArray.length);
        return dst;
    }

    public static char[] clam(final char[] src, int numClamped) {
        char[] clamped = new char[src.length - numClamped];

        System.arraycopy(src, numClamped, clamped, 0, clamped.length);

        return clamped;
    }

    public static char[] unClam(char[] dst, char[] clamArray) {
        System.arraycopy(clamArray, 0, dst, dst.length - clamArray.length, clamArray.length);
        return dst;
    }
}
