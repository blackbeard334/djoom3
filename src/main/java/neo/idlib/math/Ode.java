package neo.idlib.math;

import neo.idlib.math.Math_h.idMath;

/*
 ===============================================================================

 Numerical solvers for ordinary differential equations.

 ===============================================================================
 */
public class Ode {

    public static abstract class deriveFunction_t {

        public abstract void run(final float t, final Object userData, final float[] state, float[] derivatives);//TODO:quadruple check the pointers
    }

    //===============================================================
    //
    //	idODE
    //
    //===============================================================
    public static abstract class idODE {

//public					~idODE( void ) {}
        protected int              dimension;   // dimension in floats allocated for
        protected deriveFunction_t derive;      // derive function
        protected Object           userData;    // client data

        public abstract float Evaluate(final float[] state, float[] newState, float t0, float t1);
    };

    //===============================================================
    //
    //	idODE_Euler
    //
    //===============================================================
    public static class idODE_Euler extends idODE {

        protected float[] derivatives;	// space to store derivatives
        //
        //

        public idODE_Euler(final int dim, final deriveFunction_t dr, final Object ud) {
            dimension = dim;
            derivatives = new float[dim];
            derive = dr;
            userData = ud;
        }
//	virtual				~idODE_Euler( void );

        @Override
        public float Evaluate(final float[] state, float[] newState, float t0, float t1) {//TODO:replace float[] input with rigidBodyIState_s.
            float delta;
            int i;

            derive.run(t0, userData, state, derivatives);
            delta = t1 - t0;
            for (i = 0; i < dimension; i++) {
                newState[i] = state[i] + delta * derivatives[i];
            }
            return delta;
        }
    };

//===============================================================
//
//	idODE_Midpoint
//
//===============================================================
    class idODE_Midpoint extends idODE {

        protected float[] tmpState;
        protected float[] derivatives;	// space to store derivatives
        //
        //

        public idODE_Midpoint(final int dim, final deriveFunction_t dr, final Object ud) {
            dimension = dim;
            tmpState = new float[dim];
            derivatives = new float[dim];
            derive = dr;
            userData = ud;
        }
//public	virtual				~idODE_Midpoint( void );

        @Override
        public float Evaluate(float[] state, float[] newState, float t0, float t1) {
            float delta, halfDelta;
            int i;

            delta = t1 - t0;
            halfDelta = delta * 0.5f;
            // first step
            derive.run(t0, userData, state, derivatives);
            for (i = 0; i < dimension; i++) {
                tmpState[i] = state[i] + halfDelta * derivatives[i];
            }
            // second step
            derive.run(t0 + halfDelta, userData, tmpState, derivatives);

            for (i = 0; i < dimension; i++) {
                newState[i] = state[i] + delta * derivatives[i];
            }
            return delta;
        }
    };

//===============================================================
//
//	idODE_RK4
//
//===============================================================
    class idODE_RK4 extends idODE {

        protected float[] tmpState;
        protected float[] d1;			// derivatives
        protected float[] d2;
        protected float[] d3;
        protected float[] d4;
        //
        //

        public idODE_RK4(final int dim, final deriveFunction_t dr, final Object ud) {
            dimension = dim;
            derive = dr;
            userData = ud;
            tmpState = new float[dim];
            d1 = new float[dim];
            d2 = new float[dim];
            d3 = new float[dim];
            d4 = new float[dim];
        }

//	virtual				~idODE_RK4( void );//TODO:experiment with overriding finalize
        @Override
        public float Evaluate(float[] state, float[] newState, float t0, float t1) {
            float delta, halfDelta, sixthDelta;
            int i;

            delta = t1 - t0;
            halfDelta = delta * 0.5f;
            // first step
            derive.run(t0, userData, state, d1);
            for (i = 0; i < dimension; i++) {
                tmpState[i] = state[i] + halfDelta * d1[i];
            }
            // second step
            derive.run(t0 + halfDelta, userData, tmpState, d2);
            for (i = 0; i < dimension; i++) {
                tmpState[i] = state[i] + halfDelta * d2[i];
            }
            // third step
            derive.run(t0 + halfDelta, userData, tmpState, d3);
            for (i = 0; i < dimension; i++) {
                tmpState[i] = state[i] + delta * d3[i];
            }
            // fourth step
            derive.run(t0 + delta, userData, tmpState, d4);

            sixthDelta = delta * (1.0f / 6.0f);
            for (i = 0; i < dimension; i++) {
                newState[i] = state[i] + sixthDelta * (d1[i] + 2.0f * (d2[i] + d3[i]) + d4[i]);
            }
            return delta;
        }
    };

//===============================================================
//
//	idODE_RK4Adaptive
//
//===============================================================
    class idODE_RK4Adaptive extends idODE {

        protected float maxError;		// maximum allowed error
        protected float[] tmpState;
        protected float[] d1;			// derivatives
        protected float[] d1half;
        protected float[] d2;
        protected float[] d3;
        protected float[] d4;
        //
        //

        public idODE_RK4Adaptive(final int dim, final deriveFunction_t dr, final Object ud) {
            dimension = dim;
            derive = dr;
            userData = ud;
            maxError = 0.01f;
            tmpState = new float[dim];
            d1 = new float[dim];
            d1half = new float[dim];
            d2 = new float[dim];
            d3 = new float[dim];
            d4 = new float[dim];
        }
//	virtual				~idODE_RK4Adaptive( void );

        @Override
        public float Evaluate(float[] state, float[] newState, float t0, float t1) {
            float delta, halfDelta, fourthDelta, sixthDelta;
            float error, max;
            int i, n;

            delta = t1 - t0;

            for (n = 0; n < 4; n++) {

                halfDelta = delta * 0.5f;
                fourthDelta = delta * 0.25f;

                // first step of first half delta
                derive.run(t0, userData, state, d1);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + fourthDelta * d1[i];
                }
                // second step of first half delta
                derive.run(t0 + fourthDelta, userData, tmpState, d2);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + fourthDelta * d2[i];
                }
                // third step of first half delta
                derive.run(t0 + fourthDelta, userData, tmpState, d3);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + halfDelta * d3[i];
                }
                // fourth step of first half delta
                derive.run(t0 + halfDelta, userData, tmpState, d4);

                sixthDelta = halfDelta * (1.0f / 6.0f);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + sixthDelta * (d1[i] + 2.0f * (d2[i] + d3[i]) + d4[i]);
                }

                // first step of second half delta
                derive.run(t0 + halfDelta, userData, tmpState, d1half);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + fourthDelta * d1half[i];
                }
                // second step of second half delta
                derive.run(t0 + halfDelta + fourthDelta, userData, tmpState, d2);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + fourthDelta * d2[i];
                }
                // third step of second half delta
                derive.run(t0 + halfDelta + fourthDelta, userData, tmpState, d3);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + halfDelta * d3[i];
                }
                // fourth step of second half delta
                derive.run(t0 + delta, userData, tmpState, d4);

                sixthDelta = halfDelta * (1.0f / 6.0f);
                for (i = 0; i < dimension; i++) {
                    newState[i] = state[i] + sixthDelta * (d1[i] + 2.0f * (d2[i] + d3[i]) + d4[i]);
                }

                // first step of full delta
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + halfDelta * d1[i];
                }
                // second step of full delta
                derive.run(t0 + halfDelta, userData, tmpState, d2);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + halfDelta * d2[i];
                }
                // third step of full delta
                derive.run(t0 + halfDelta, userData, tmpState, d3);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + delta * d3[i];
                }
                // fourth step of full delta
                derive.run(t0 + delta, userData, tmpState, d4);

                sixthDelta = delta * (1.0f / 6.0f);
                for (i = 0; i < dimension; i++) {
                    tmpState[i] = state[i] + sixthDelta * (d1[i] + 2.0f * (d2[i] + d3[i]) + d4[i]);
                }

                // get max estimated error
                max = 0.0f;
                for (i = 0; i < dimension; i++) {
                    error = idMath.Fabs((newState[i] - tmpState[i]) / (delta * d1[i] + 1e-10f));
                    if (error > max) {
                        max = error;
                    }
                }
                error = max / maxError;

                if (error <= 1.0f) {
                    return delta * 4.0f;
                }
                if (delta <= 1e-7) {
                    return delta;
                }
                delta *= 0.25;
            }
            return delta;
        }

        public void SetMaxError(final float err) {
            if (err > 0.0f) {
                maxError = err;
            }
        }
    };
}
