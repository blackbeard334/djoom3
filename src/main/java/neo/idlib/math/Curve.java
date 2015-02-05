package neo.idlib.math;

import java.util.Arrays;
import static neo.TempDump.reflects.GetDimension;
import static neo.TempDump.reflects.Zero;
import static neo.TempDump.reflects._Get;
import static neo.TempDump.reflects._GetGet;
import static neo.TempDump.reflects._GetSet;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Curve.idCurve_Spline;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMatX;
import neo.idlib.math.Vector.idVecX;

/**
 *
 */
public class Curve {

    /**
     * ===============================================================================
     *
     * Curve base template.
     *
     * ===============================================================================
     */
    static class idCurve<type> {

        public idCurve() {
            currentIndex = -1;
            changed = false;
        }
//public	virtual				~idCurve( void );


        /*
         ====================
         idCurve::AddValue

         add a timed/value pair to the spline
         returns the index to the inserted pair
         ====================
         */
        public int AddValue(final float time, final type value) {
            int i;

            i = IndexForTime(time);
            times.Insert(time, i);
            values.Insert((Double) value, i);
            changed = true;
            return i;
        }

        public void RemoveIndex(final int index) {
            values.RemoveIndex(index);
            times.RemoveIndex(index);
            changed = true;
        }

        public void Clear() {
            values.Clear();
            times.Clear();
            currentIndex = -1;
            changed = true;
        }

        /*
         ====================
         idCurve::GetCurrentValue

         get the value for the given time
         ====================
         */
        public type GetCurrentValue(final float time) {
            int i;

            i = IndexForTime(time);
            if (i >= values.Num()) {
                return (type) values.oGet(values.Num() - 1);
            } else {
                return (type) values.oGet(i);
            }
        }

        /*
         ====================
         idCurve::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        public type GetCurrentFirstDerivative(final float time) {
            return (type) (Object) (values.oGet(0) - values.oGet(0));
        }

        /*
         ====================
         idCurve::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        public type GetCurrentSecondDerivative(final float time) {
            return (type) (Object) (values.oGet(0) - values.oGet(0));
        }

        public boolean IsDone(final float time) {
            return (time >= times.oGet(times.Num() - 1));
        }

        public int GetNumValues() {
            return values.Num();
        }

        public void SetValue(final int index, final type value) {
            values.oSet(index, (Double) value);
            changed = true;
        }

        public type GetValue(final int index) {
            return (type) values.oGet(index);
        }

        public type GetValueAddress(final int index) {//TODO:pointer
            return (type) values.oGet(index);
        }

        public float GetTime(final int index) {
            return times.oGet(index);
        }

        public float GetLengthForTime(final float time) {
            float length = 0.0f;
            int index = IndexForTime(time);
            for (int i = 0; i < index; i++) {
                length += RombergIntegral(times.oGet(i), times.oGet(i + 1), 5);
            }
            length += RombergIntegral(times.oGet(index), time, 5);
            return length;
        }

        public float GetTimeForLength(final float length) {
            return GetTimeForLength(length, 1.0f);
        }

        public float GetTimeForLength(final float length, final float epsilon) {
            int i, index;
            float[] accumLength;
            float totalLength, len0, len1, t, diff;

            if (length <= 0.0f) {
                return times.oGet(0);
            }

//	accumLength = (float *) _alloca16( values.Num() * sizeof( float ) );
            accumLength = new float[values.Num()];
            totalLength = 0.0f;
            for (index = 0; index < values.Num() - 1; index++) {
                totalLength += GetLengthBetweenKnots(index, index + 1);
                accumLength[index] = totalLength;
                if (length < accumLength[index]) {
                    break;
                }
            }

            if (index >= values.Num() - 1) {
                return times.oGet(times.Num() - 1);
            }

            if (index == 0) {
                len0 = length;
                len1 = accumLength[0];
            } else {
                len0 = length - accumLength[index - 1];
                len1 = accumLength[index] - accumLength[index - 1];
            }

            // invert the arc length integral using Newton's method
            t = (times.oGet(index + 1) - times.oGet(index)) * len0 / len1;
            for (i = 0; i < 32; i++) {
                diff = RombergIntegral(times.oGet(index), times.oGet(index) + t, 5) - len0;
                if (idMath.Fabs(diff) <= epsilon) {
                    return times.oGet(index) + t;
                }
                t -= diff / GetSpeed(times.oGet(index) + t);
            }
            return times.oGet(index) + t;
        }

        public float GetLengthBetweenKnots(final int i0, final int i1) {
            float length = 0.0f;
            for (int i = i0; i < i1; i++) {
                length += RombergIntegral(times.oGet(i), times.oGet(i + 1), 5);
            }
            return length;
        }

        public void MakeUniform(final float totalTime) {
            int i, n;

            n = times.Num() - 1;
            for (i = 0; i <= n; i++) {
                times.oSet(i, i * totalTime / n);
            }
            changed = true;
        }

        public void SetConstantSpeed(final float totalTime) {
            int i;
            float[] length;
            float totalLength, scale, t;

//	length = (float *) _alloca16( values.Num() * sizeof( float ) );
            length = new float[values.Num()];
            totalLength = 0.0f;
            for (i = 0; i < values.Num() - 1; i++) {
                length[i] = GetLengthBetweenKnots(i, i + 1);
                totalLength += length[i];
            }
            scale = totalTime / totalLength;
            for (t = 0.0f, i = 0; i < times.Num() - 1; i++) {
                times.oSet(i, t);
                t += scale * length[i];
            }
            times.oSet(times.Num() - 1, totalTime);
            changed = true;
        }

        public void ShiftTime(final float deltaTime) {
            for (int i = 0; i < times.Num(); i++) {
                times.oSet(i, times.oGet(i) + deltaTime);
            }
            changed = true;
        }

        public void Translate(final type translation) {
            for (int i = 0; i < values.Num(); i++) {
                values.oSet(i, values.oGet(i) + (Double) translation);
            }
            changed = true;
        }
        protected idList<Float> times;			// knots
        protected idList<Double> values;                                  // knot values
        protected int currentIndex;                                     // cached index for fast lookup
        protected boolean changed;                                      // set whenever the curve changes

        /*
         ====================
         idCurve::IndexForTime

         find the index for the first time greater than or equal to the given time
         ====================
         */
        protected int IndexForTime(final float time) {
            int len, mid, offset, res;

            if (currentIndex >= 0 && currentIndex <= times.Num()) {
                // use the cached index if it is still valid
                if (currentIndex == 0) {
                    if (time <= times.oGet(currentIndex)) {
                        return currentIndex;
                    }
                } else if (currentIndex == times.Num()) {
                    if (time > times.oGet(currentIndex - 1)) {

                        return currentIndex;
                    }
                } else if (time > times.oGet(currentIndex - 1) && time <= times.oGet(currentIndex)) {
                    return currentIndex;
                } else if (time > times.oGet(currentIndex) && (currentIndex + 1 == times.Num() || time <= times.oGet(currentIndex + 1))) {
                    // use the next index
                    currentIndex++;
                    return currentIndex;
                }
            }
            // use binary search to find the index for the given time
            len = times.Num();
            mid = len;
            offset = 0;
            res = 0;
            while (mid
                    > 0) {
                mid = len >> 1;
                if (time == times.oGet(offset + mid)) {
                    return offset + mid;
                } else if (time > times.oGet(offset + mid)) {
                    offset += mid;
                    len -= mid;
                    res = 1;
                } else {
                    len -= mid;
                    res = 0;
                }
            }
            currentIndex = offset + res;
            return currentIndex;
        }

        /*
         ====================
         idCurve::TimeForIndex

         get the value for the given time
         ====================
         */
        protected float TimeForIndex(final int index) {
            int n = times.Num() - 1;

            if (index < 0) {
                return times.oGet(0)
                        + index * (times.oGet(1) - times.oGet(0));
            } else if (index > n) {
                return times.oGet(n) + (index - n) * (times.oGet(n) - times.oGet(n - 1));
            }
            return times.oGet(index);
        }

        /*
         ====================
         idCurve::ValueForIndex

         get the value for the given time
         ====================
         */
        protected type ValueForIndex(final int index) {
            int n = values.Num() - 1;

            if (index < 0) {
                return (type) (Object) (values.oGet(0) + index * (values.oGet(1) - values.oGet(0)));
            } else if (index > n) {
                return (type) (Object) (values.oGet(n) + (index - n) * (values.oGet(n) - values.oGet(n - 1)));
            }
            return (type) values.oGet(index);
        }

        protected float GetSpeed(final float time) {
            int i;
            float speed;
            type value;

            value = GetCurrentFirstDerivative(time);
            for (speed = 0.0f, i = 0; i < GetDimension(value); i++) {
                speed += _Get(value, i) * _Get(value, i);
            }
            return idMath.Sqrt(speed);
        }

        protected float RombergIntegral(final float t0, final float t1, final int order) {
            int i, j, k, m, n;
            float sum, delta;
            float[][] temp = new float[2][];

//	temp[0] = (float *) _alloca16( order * sizeof( float ) );
            temp[0] = new float[order];
//	temp[1] = (float *) _alloca16( order * sizeof( float ) );
            temp[1] = new float[order];

            delta = t1 - t0;
            temp[0][0] = 0.5f * delta * (GetSpeed(t0) + GetSpeed(t1));

            for (i = 2, m = 1; i <= order; i++, m *= 2, delta *= 0.5f) {

                // approximate using the trapezoid rule
                sum = 0.0f;
                for (j = 1; j <= m; j++) {
                    sum += GetSpeed(t0 + delta * (j - 0.5f));
                }

                // Richardson extrapolation
                temp[1][0] = 0.5f * (temp[0][0] + delta * sum);
                for (k = 1, n = 4; k < i; k++, n *= 4) {
                    temp[1][k] = (n * temp[1][k - 1] - temp[0][k - 1]) / (n - 1);
                }

                for (j = 0; j < i; j++) {
                    temp[0][j] = temp[1][j];
                }
            }
            return temp[0][order - 1];
        }
    };

    /**
     * ===============================================================================
     *
     * Bezier Curve template. The degree of the polynomial equals the number of
     * knots minus one.
     *
     * ===============================================================================
     */
    class idCurve_Bezier<type> extends idCurve<type> {

        public idCurve_Bezier() {
        }

        /*
         ====================
         idCurve_Bezier::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            {
                int i;
                float[] bvals;
                Double v;

//	bvals = (float *) _alloca16( this->values.Num() * sizeof( float ) );
                bvals = new float[this.values.Num()];

                Basis(this.values.Num(), time, bvals);
                v = bvals[0] * this.values.oGet(0);
                for (i = 1; i < this.values.Num(); i++) {
                    v += bvals[i] * this.values.oGet(i);
                }
                return (type) v;
            }
        }

        /*
         ====================
         idCurve_Bezier::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            int i;
            float[] bvals;
            float d;
            Double v;

//	bvals = (float *) _alloca16( this->values.Num() * sizeof( float ) );
            bvals = new float[this.values.Num()];

            BasisFirstDerivative(this.values.Num(), time, bvals);
            v = bvals[0] * this.values.oGet(0);
            for (i = 1; i < this.values.Num(); i++) {
                v += bvals[i] * this.values.oGet(i);
            }
            d = (this.times.oGet(this.times.Num() - 1) - this.times.oGet(0));
            return (type) (Object) (((this.values.Num() - 1) / d) * v);
        }

        /*
         ====================
         idCurve_Bezier::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            int i;
            float[] bvals;
            float d;
            Double v;

//	bvals = (float *) _alloca16( this->values.Num() * sizeof( float ) );
            bvals = new float[this.values.Num()];

            BasisSecondDerivative(this.values.Num(), time, bvals);
            v = bvals[0] * this.values.oGet(0);
            for (i = 1; i < this.values.Num(); i++) {
                v += bvals[i] * this.values.oGet(i);
            }
            d = (this.times.oGet(this.times.Num() - 1) - this.times.oGet(0));
            return (type) (Object) (((this.values.Num() - 2) * (this.values.Num() - 1) / (d * d)) * v);
        }

        /*
         ====================
         idCurve_Bezier::Basis

         bezier basis functions
         ====================
         */
        protected void Basis(final int order, final float t, float[] bvals) {
            int i, j, d;
            float[] c;
            float c1, c2, s, o, ps, po;

            bvals[0] = 1.0f;
            d = order - 1;
            if (d <= 0) {
                return;
            }

//	c = (float *) _alloca16( (d+1) * sizeof( float ) );
            c = new float[d + 1];
            s = (t - this.times.oGet(0)) / (this.times.oGet(this.times.Num() - 1) - this.times.oGet(0));
            o = 1.0f - s;
            ps = s;
            po = o;

            for (i = 1; i < d; i++) {
                c[i] = 1.0f;
            }
            for (i = 1; i < d; i++) {
                c[i - 1] = 0.0f;
                c1 = c[i];
                c[i] = 1.0f;
                for (j = i + 1; j <= d; j++) {
                    c2 = c[j];
                    c[j] = c1 + c[j - 1];
                    c1 = c2;
                }
                bvals[i] = c[d] * ps;
                ps *= s;
            }
            for (i = d - 1; i >= 0; i--) {
                bvals[i] *= po;
                po *= o;
            }
            bvals[d] = ps;
        }

        /*
         ====================
         idCurve_Bezier::BasisFirstDerivative

         first derivative of bezier basis functions
         ====================
         */
        protected void BasisFirstDerivative(final int order, final float t, float[] bvals) {
            int i;

            float[] bvals_1 = Arrays.copyOfRange(bvals, 1, bvals.length - 1);//TODO:check copy back mechanism
            Basis(order - 1, t, bvals_1);
            System.arraycopy(bvals_1, 0, bvals, 1, bvals_1.length);

            bvals[0] = 0.0f;
            for (i = 0; i < order - 1; i++) {
                bvals[i] -= bvals[i + 1];
            }
        }

        /*
         ====================
         idCurve_Bezier::BasisSecondDerivative

         second derivative of bezier basis functions
         ====================
         */
        protected void BasisSecondDerivative(final int order, final float t, float[] bvals) {
            int i;

            float[] bvals_1 = Arrays.copyOfRange(bvals, 1, bvals.length - 1);//TODO:check copy back mechanism
            BasisFirstDerivative(order - 1, t, bvals_1);
            System.arraycopy(bvals_1, 0, bvals, 1, bvals_1.length);

            bvals[0] = 0.0f;
            for (i = 0; i < order - 1; i++) {
                bvals[i] -= bvals[i + 1];
            }
        }
    };

    /*
     ===============================================================================

     Quadratic Bezier Curve template.
     Should always have exactly three knots.

     ===============================================================================
     */
    class idCurve_QuadraticBezier<type> extends idCurve<type> {

        public idCurve_QuadraticBezier() {
        }

        /*
         ====================
         idCurve_QuadraticBezier::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            float[] bvals = new float[3];
            assert (this.values.Num() == 3);
            Basis(time, bvals);
            return (type) (Object) ((bvals[0] * this.values.oGet(0) + bvals[1] * this.values.oGet(1) + bvals[2] * this.values.oGet(2)));
        }

        /*
         ====================
         idCurve_QuadraticBezier::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            float[] bvals = new float[3];
            float d;
            assert (this.values.Num() == 3);
            BasisFirstDerivative(time, bvals);
            d = (this.times.oGet(2) - this.times.oGet(0));
            return (type) (Object) ((bvals[0] * this.values.oGet(0) + bvals[1] * this.values.oGet(1) + bvals[2] * this.values.oGet(2)) / d);
        }

        /*
         ====================
         idCurve_QuadraticBezier::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            float[] bvals = new float[3];
            float d;
            assert (this.values.Num() == 3);
            BasisSecondDerivative(time, bvals);
            d = (this.times.oGet(2) - this.times.oGet(0));
            return (type) (Object) ((bvals[0] * this.values.oGet(0) + bvals[1] * this.values.oGet(1) + bvals[2] * this.values.oGet(2)) / (d * d));
        }

        /*
         ====================
         idCurve_QuadraticBezier::Basis

         quadratic bezier basis functions
         ====================
         */
        protected void Basis(final float t, float[] bvals) {
            float s1 = (float) (t - this.times.oGet(0)) / (this.times.oGet(2) - this.times.oGet(0));
            float s2 = s1 * s1;
            bvals[0] = s2 - 2.0f * s1 + 1.0f;
            bvals[1] = -2.0f * s2 + 2.0f * s1;
            bvals[2] = s2;
        }

        /*
         ====================
         idCurve_QuadraticBezier::BasisFirstDerivative

         first derivative of quadratic bezier basis functions
         ====================
         */
        protected void BasisFirstDerivative(final float t, float[] bvals) {
            float s1 = (float) (t - this.times.oGet(0)) / (this.times.oGet(2) - this.times.oGet(0));
            bvals[0] = 2.0f * s1 - 2.0f;
            bvals[1] = -4.0f * s1 + 2.0f;
            bvals[2] = 2.0f * s1;
        }

        /*
         ====================
         idCurve_QuadraticBezier::BasisSecondDerivative

         second derivative of quadratic bezier basis functions
         ====================
         */
        protected void BasisSecondDerivative(final float t, float[] bvals) {
//	float s1 = (float) ( t - this->times.oGet(0] ) / ( this->times.oGet(2] - this->times.oGet(0] );
            bvals[0] = 2.0f;
            bvals[1] = -4.0f;
            bvals[2] = 2.0f;
        }
    };

    /**
     * ===============================================================================
     *
     * Cubic Bezier Curve template. Should always have exactly four knots.
     *
     * ===============================================================================
     */
    class idCurve_CubicBezier<type> extends idCurve<type> {

        public idCurve_CubicBezier() {
        }

        /*
         ====================
         idCurve_CubicBezier::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            float[] bvals = new float[4];
            assert (this.values.Num() == 4);
            Basis(time, bvals);
            return (type) (Object) ((bvals[0] * this.values.oGet(0) + bvals[1] * this.values.oGet(1) + bvals[2] * this.values.oGet(2) + bvals[3] * this.values.oGet(3)));
        }

        /*
         ====================
         idCurve_CubicBezier::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            float[] bvals = new float[4];
            float d;
            assert (this.values.Num() == 4);
            BasisFirstDerivative(time, bvals);
            d = (this.times.oGet(3) - this.times.oGet(0));
            return (type) (Object) ((bvals[0] * this.values.oGet(0) + bvals[1] * this.values.oGet(1) + bvals[2] * this.values.oGet(2) + bvals[3] * this.values.oGet(3)) / d);
        }

        /*
         ====================
         idCurve_CubicBezier::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            float[] bvals = new float[4];
            float d;
            assert (this.values.Num() == 4);
            BasisSecondDerivative(time, bvals);
            d = (this.times.oGet(3) - this.times.oGet(0));
            return (type) (Object) ((bvals[0] * this.values.oGet(0) + bvals[1] * this.values.oGet(1) + bvals[2] * this.values.oGet(2) + bvals[3] * this.values.oGet(3)) / (d * d));
        }


        /*
         ====================
         idCurve_CubicBezier::Basis

         cubic bezier basis functions
         ====================
         */
        protected void Basis(final float t, float[] bvals) {
            float s1 = (t - this.times.oGet(0)) / (this.times.oGet(3) - this.times.oGet(0));
            float s2 = s1 * s1;
            float s3 = s2 * s1;
            bvals[0] = -s3 + 3.0f * s2 - 3.0f * s1 + 1.0f;
            bvals[1] = 3.0f * s3 - 6.0f * s2 + 3.0f * s1;
            bvals[2] = -3.0f * s3 + 3.0f * s2;
            bvals[3] = s3;
        }

        /*
         ====================
         idCurve_CubicBezier::BasisFirstDerivative

         first derivative of cubic bezier basis functions
         ====================
         */
        protected void BasisFirstDerivative(final float t, float[] bvals) {
            float s1 = (t - this.times.oGet(0)) / (this.times.oGet(3) - this.times.oGet(0));
            float s2 = s1 * s1;
            bvals[0] = -3.0f * s2 + 6.0f * s1 - 3.0f;
            bvals[1] = 9.0f * s2 - 12.0f * s1 + 3.0f;
            bvals[2] = -9.0f * s2 + 6.0f * s1;
            bvals[3] = 3.0f * s2;
        }

        /*
         ====================
         idCurve_CubicBezier::BasisSecondDerivative

         second derivative of cubic bezier basis functions
         ====================
         */
        protected void BasisSecondDerivative(final float t, float[] bvals) {
            float s1 = (t - this.times.oGet(0)) / (this.times.oGet(3) - this.times.oGet(0));
            bvals[0] = -6.0f * s1 + 6.0f;
            bvals[1] = 18.0f * s1 - 12.0f;
            bvals[2] = -18.0f * s1 + 6.0f;
            bvals[3] = 6.0f * s1;
        }
    };

    /**
     * ===============================================================================
     *
     * Spline base template.
     *
     * ===============================================================================
     */
    public static class idCurve_Spline<type> extends idCurve<type> {

        /**
         * enum	boundary_t { BT_FREE, BT_CLAMPED, BT_CLOSED };
         */
        public static final int BT_FREE = 0, BT_CLAMPED = 1, BT_CLOSED = 2;

        public idCurve_Spline() {
            boundaryType = BT_FREE;
            closeTime = 0.0f;
        }

        @Override
        public boolean IsDone(float time) {
            return (boundaryType != BT_CLOSED && time >= this.times.oGet(this.times.Num() - 1));
        }

        public void SetBoundaryType(final int boundary_t) {
            boundaryType = boundary_t;
            this.changed = true;
        }

        public int GetBoundaryType() {
            return boundaryType;
        }

        public void SetCloseTime(final float t) {
            closeTime = t;
            this.changed = true;
        }

        public float GetCloseTime() {
            return boundaryType == BT_CLOSED ? closeTime : 0.0f;
        }
        protected int boundaryType;
        protected float closeTime;

        /*
         ====================
         idCurve_Spline::ValueForIndex

         get the value for the given time
         ====================
         */
        @Override
        protected type ValueForIndex(final int index) {
            int n = this.values.Num() - 1;

            if (index < 0) {
                if (boundaryType == BT_CLOSED) {
                    return (type) this.values.oGet(this.values.Num() + index % this.values.Num());
                } else {
                    return (type) (Object) (this.values.oGet(0) + index * (this.values.oGet(1) - this.values.oGet(0)));
                }
            } else if (index > n) {
                if (boundaryType == BT_CLOSED) {
                    return (type) this.values.oGet(index % this.values.Num());
                } else {
                    return (type) (Object) (this.values.oGet(n) + (index - n) * (this.values.oGet(n) - this.values.oGet(n - 1)));
                }
            }
            return (type) this.values.oGet(index);
        }

        /*
         ====================
         idCurve_Spline::TimeForIndex

         get the value for the given time
         ====================
         */
        @Override
        protected float TimeForIndex(int index) {
            int n = this.times.Num() - 1;

            if (index < 0) {
                if (boundaryType == BT_CLOSED) {
                    return (index / this.times.Num()) * (this.times.oGet(n) + closeTime) - (this.times.oGet(n) + closeTime - this.times.oGet(this.times.Num() + index % this.times.Num()));
                } else {
                    return this.times.oGet(0) + index * (this.times.oGet(1) - this.times.oGet(0));
                }
            } else if (index > n) {
                if (boundaryType == BT_CLOSED) {
                    return (index / this.times.Num()) * (this.times.oGet(n) + closeTime) + this.times.oGet(index % this.times.Num());
                } else {
                    return this.times.oGet(n) + (index - n) * (this.times.oGet(n) - this.times.oGet(n - 1));
                }
            }
            return this.times.oGet(index);
        }

        /*
         ====================
         idCurve_Spline::ClampedTime

         return the clamped time based on the boundary type
         ====================
         */
        protected float ClampedTime(final float t) {
            if (boundaryType == BT_CLAMPED) {
                if (t < this.times.oGet(0)) {
                    return this.times.oGet(0);
                } else if (t >= this.times.oGet(this.times.Num() - 1)) {
                    return this.times.oGet(this.times.Num() - 1);
                }
            }
            return t;
        }
    };

    /**
     * ===============================================================================
     *
     * Cubic Interpolating Spline template. The curve goes through all the
     * knots.
     *
     * ===============================================================================
     */
    class idCurve_NaturalCubicSpline<type> extends idCurve_Spline<type> {

        public idCurve_NaturalCubicSpline() {
        }

        @Override
        public void Clear() {
            this.Clear();
            this.values.Clear();
            b.Clear();
            c.Clear();
            d.Clear();
        }

        /*
         ====================
         idCurve_NaturalCubicSpline::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            float clampedTime = this.ClampedTime(time);
            int i = this.IndexForTime(clampedTime);
            float s = time - this.TimeForIndex(i);
            Setup();
            return (type) (Object) (this.values.oGet(i) + s * (_Get(b, i) + s * (_Get(c, i) + s * _Get(d, i))));
        }

        /*
         ====================
         idCurve_NaturalCubicSpline::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            float clampedTime = this.ClampedTime(time);
            int i = this.IndexForTime(clampedTime);
            float s = time - this.TimeForIndex(i);
            Setup();
            return (type) (Object) ((_Get(b, i) + s * (2.0f * _Get(c, i) + 3.0f * s * _Get(d, i))));
        }

        /*
         ====================
         idCurve_NaturalCubicSpline::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            float clampedTime = this.ClampedTime(time);
            int i = this.IndexForTime(clampedTime);
            float s = time - this.TimeForIndex(i);
            Setup();
            return (type) (Object) ((2.0f * _Get(c, i) + 6.0f * s * _Get(d, i)));
        }
        protected idList<type> b;
        protected idList<type> c;
        protected idList<type> d;

        protected void Setup() {
            if (this.changed) {
                switch (this.boundaryType) {
                    case idCurve_Spline.BT_FREE:
                        SetupFree();
                        break;
                    case idCurve_Spline.BT_CLAMPED:
                        SetupClamped();
                        break;
                    case idCurve_Spline.BT_CLOSED:
                        SetupClosed();
                        break;
                }
                this.changed = false;
            }
        }

        protected void SetupFree() {
            int i;
            float inv;
            float[] d0, d1, beta, gamma;
            double[] alpha, delta;

            d0 = new float[(this.values.Num() - 1)];
            d1 = new float[(this.values.Num() - 1)];
            alpha = new double[(this.values.Num() - 1)];
            beta = new float[(this.values.Num())];
            gamma = new float[(this.values.Num() - 1)];
            delta = new double[(this.values.Num())];

            for (i = 0; i < this.values.Num() - 1; i++) {
                d0[i] = this.times.oGet(i + 1) - this.times.oGet(i);
            }

            for (i = 1; i < this.values.Num() - 1; i++) {
                d1[i] = this.times.oGet(i + 1) - this.times.oGet(i - 1);
            }

            for (i = 1; i < this.values.Num() - 1; i++) {
                float sum = (float) (3.0f * (d0[i - 1] * this.values.oGet(i + 1) - d1[i] * this.values.oGet(i) + d0[i] * this.values.oGet(i - 1)));
                inv = 1.0f / (d0[i - 1] * d0[i]);
                alpha[i] = inv * sum;
            }

            beta[0] = 1.0f;
            gamma[0] = 0.0f;
            delta[0] = this.values.oGet(0) - this.values.oGet(0);

            for (i = 1; i < this.values.Num() - 1; i++) {
                beta[i] = 2.0f * d1[i] - d0[i - 1] * gamma[i - 1];
                inv = 1.0f / beta[i];
                gamma[i] = inv * d0[i];
                delta[i] = inv * (alpha[i] - d0[i - 1] * delta[i - 1]);
            }
            beta[this.values.Num() - 1] = 1.0f;
            delta[this.values.Num() - 1] = this.values.oGet(0) - this.values.oGet(0);

            b.AssureSize(this.values.Num());
            c.AssureSize(this.values.Num());
            d.AssureSize(this.values.Num());

            c.oSet(this.values.Num() - 1, this.values.oGet(0) - this.values.oGet(0));

            for (i = this.values.Num() - 2; i >= 0; i--) {
                c.oSet(i, delta[i] - gamma[i] * _Get(c, i + 1));
                inv = 1.0f / d0[i];
                b.oSet(i, inv * (this.values.oGet(i + 1) - this.values.oGet(i)) - (1.0f / 3.0f) * d0[i] * (_Get(c, i + 1) + 2.0f * _Get(c, i)));
                d.oSet(i, (1.0f / 3.0f) * inv * (_Get(c, i + 1) - _Get(c, i)));
            }
        }

        protected void SetupClamped() {
            int i;
            float inv;
            float[] d0, d1, beta, gamma;
            double[] alpha, delta;

            d0 = new float[(this.values.Num() - 1)];
            d1 = new float[(this.values.Num() - 1)];
            alpha = new double[(this.values.Num() - 1)];
            beta = new float[(this.values.Num())];
            gamma = new float[(this.values.Num() - 1)];
            delta = new double[(this.values.Num())];

            for (i = 0; i < this.values.Num() - 1; i++) {
                d0[i] = this.times.oGet(i + 1) - this.times.oGet(i);
            }

            for (i = 1; i < this.values.Num() - 1; i++) {
                d1[i] = this.times.oGet(i + 1) - this.times.oGet(i - 1);
            }

            inv = 1.0f / d0[0];
            alpha[0] = 3.0f * (inv - 1.0f) * (this.values.oGet(1) - this.values.oGet(0));
            inv = 1.0f / d0[this.values.Num() - 2];
            alpha[this.values.Num() - 1] = 3.0f * (1.0f - inv) * (this.values.oGet(this.values.Num() - 1) - this.values.oGet(this.values.Num() - 2));

            for (i = 1; i < this.values.Num() - 1; i++) {
                float sum = (float) (3.0f * (d0[i - 1] * this.values.oGet(i + 1) - d1[i] * this.values.oGet(i) + d0[i] * this.values.oGet(i - 1)));
                inv = 1.0f / (d0[i - 1] * d0[i]);
                alpha[i] = inv * sum;
            }

            beta[0] = 2.0f * d0[0];
            gamma[0] = 0.5f;
            inv = 1.0f / beta[0];
            delta[0] = inv * alpha[0];

            for (i = 1; i < this.values.Num() - 1; i++) {
                beta[i] = 2.0f * d1[i] - d0[i - 1] * gamma[i - 1];
                inv = 1.0f / beta[i];
                gamma[i] = inv * d0[i];
                delta[i] = inv * (alpha[i] - d0[i - 1] * delta[i - 1]);
            }

            beta[this.values.Num() - 1] = d0[this.values.Num() - 2] * (2.0f - gamma[this.values.Num() - 2]);
            inv = 1.0f / beta[this.values.Num() - 1];
            delta[this.values.Num() - 1] = inv * (alpha[this.values.Num() - 1] - d0[this.values.Num() - 2] * delta[this.values.Num() - 2]);

            b.AssureSize(this.values.Num());
            c.AssureSize(this.values.Num());
            d.AssureSize(this.values.Num());

            c.oSet(this.values.Num() - 1, delta[this.values.Num() - 1]);

            for (i = this.values.Num() - 2; i >= 0; i--) {
                c.oSet(i, delta[i] - gamma[i] * _Get(c, i + 1));
                inv = 1.0f / d0[i];
                b.oSet(i, inv * (this.values.oGet(i + 1) - this.values.oGet(i)) - (1.0f / 3.0f) * d0[i] * (_Get(c, i + 1) + 2.0f * _Get(c, i)));
                d.oSet(i, (1.0f / 3.0f) * inv * (_Get(c, i + 1) - _Get(c, i)));
            }
        }

        protected void SetupClosed() {
            int i, j;
            float c0, c1;
            float[] d0;
            idMatX mat = new idMatX();
            idVecX x = new idVecX();

            d0 = new float[(this.values.Num() - 1)];
            x.SetData(this.values.Num(), Vector.idVecX.VECX_ALLOCA(this.values.Num()));
            mat.SetData(this.values.Num(), this.values.Num(), idMatX.MATX_ALLOCA(this.values.Num() * this.values.Num()));

            b.AssureSize(this.values.Num());
            c.AssureSize(this.values.Num());
            d.AssureSize(this.values.Num());

            for (i = 0; i < this.values.Num() - 1; i++) {
                d0[i] = this.times.oGet(i + 1) - this.times.oGet(i);
            }

            // matrix of system
            mat.oSet(0, 0, 1.0f);
            mat.oSet(0, this.values.Num() - 1, -1.0f);
            for (i = 1; i <= this.values.Num() - 2; i++) {
                mat.oSet(i, i - 1, d0[i - 1]);
                mat.oSet(i, i, 2.0f * (d0[i - 1] + d0[i]));
                mat.oSet(i, i + 1, d0[i]);
            }
            mat.oSet(this.values.Num() - 1, this.values.Num() - 2, d0[this.values.Num() - 2]);
            mat.oSet(this.values.Num() - 1, 0, 2.0f * (d0[this.values.Num() - 2] + d0[0]));
            mat.oSet(this.values.Num() - 1, 1, d0[0]);

            // right-hand side
            Zero(c.oGet(0));
            for (i = 1; i <= this.values.Num() - 2; i++) {
                c0 = 1.0f / d0[i];
                c1 = 1.0f / d0[i - 1];
                c.oSet(i, 3.0f * (c0 * (this.values.oGet(i + 1) - this.values.oGet(i)) - c1 * (this.values.oGet(i) - this.values.oGet(i - 1))));
            }
            c0 = 1.0f / d0[0];
            c1 = 1.0f / d0[this.values.Num() - 2];
            c.oSet(this.values.Num() - 1, 3.0f * (c0 * (this.values.oGet(1) - this.values.oGet(0)) - c1 * (this.values.oGet(0) - this.values.oGet(this.values.Num() - 2))));

            // solve system for each dimension
            mat.LU_Factor(null);
            for (i = 0; i < GetDimension(this.values.oGet(0));
                    i++) {
                for (j = 0; j < this.values.Num(); j++) {
                    x.p[j] = _GetGet(c, j, i);
                }
                mat.LU_Solve(x, x, null);
                for (j = 0; j < this.values.Num(); j++) {
                    _GetSet(c, j, i, x.oGet(j));
                }
            }

            for (i = 0; i < this.values.Num() - 1; i++) {
                c0 = 1.0f / d0[i];
                b.oSet(i, c0 * (this.values.oGet(i + 1) - this.values.oGet(i)) - (1.0f / 3.0f) * (_Get(c, i + 1) + 2.0f * _Get(c, i)) * d0[i]);
                d.oSet(i, (1.0f / 3.0f) * c0 * (_Get(c, i + 1) - _Get(c, i)));
            }
        }
    };

    /**
     * ===============================================================================
     *
     * Uniform Cubic Interpolating Spline template. The curve goes through all
     * the knots.
     *
     * ===============================================================================
     */
    public static class idCurve_CatmullRomSpline<type> extends idCurve_Spline<type> {

        public idCurve_CatmullRomSpline() {
        }

        /*
         ====================
         idCurve_CatmullRomSpline::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            int i, j, k;
            float[] bvals = new float[4];
            float clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            Basis(i - 1, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < 4; j++) {
                k = i + j - 2;
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            return (type) v;
        }

        /*
         ====================
         idCurve_CatmullRomSpline::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            int i, j, k;
            float[] bvals = new float[4];
            float d, clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) (Object) (this.values.oGet(0) - this.values.oGet(0));
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            BasisFirstDerivative(i - 1, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < 4; j++) {
                k = i + j - 2;
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            d = (this.TimeForIndex(i) - this.TimeForIndex(i - 1));
            return (type) (Object) (v / d);
        }

        /*
         ====================
         idCurve_CatmullRomSpline::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            int i, j, k;
            float[] bvals = new float[4];
            float d, clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) (Object) (this.values.oGet(0) - this.values.oGet(0));
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            BasisSecondDerivative(i - 1, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < 4; j++) {
                k = i + j - 2;
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            d = (this.TimeForIndex(i) - this.TimeForIndex(i - 1));
            return (type) (Object) (v / (d * d));
        }


        /*
         ====================
         idCurve_CatmullRomSpline::Basis

         spline basis functions
         ====================
         */
        protected void Basis(final int index, final float t, float[] bvals) {
            float s = (float) (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = ((-s + 2.0f) * s - 1.0f) * s * 0.5f;				// -0.5f s * s * s + s * s - 0.5f * s
            bvals[1] = (((3.0f * s - 5.0f) * s) * s + 2.0f) * 0.5f;	// 1.5f * s * s * s - 2.5f * s * s + 1.0f
            bvals[2] = ((-3.0f * s + 4.0f) * s + 1.0f) * s * 0.5f;		// -1.5f * s * s * s - 2.0f * s * s + 0.5f s
            bvals[3] = ((s - 1.0f) * s * s) * 0.5f;						// 0.5f * s * s * s - 0.5f * s * s
        }

        /*
         ====================
         idCurve_CatmullRomSpline::BasisFirstDerivative

         first derivative of spline basis functions
         ====================
         */
        protected void BasisFirstDerivative(final int index, final float t, float[] bvals) {
            float s = (float) (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = (-1.5f * s + 2.0f) * s - 0.5f;						// -1.5f * s * s + 2.0f * s - 0.5f
            bvals[1] = (4.5f * s - 5.0f) * s;								// 4.5f * s * s - 5.0f * s
            bvals[2] = (-4.5f * s + 4.0f) * s + 0.5f;						// -4.5 * s * s + 4.0f * s + 0.5f
            bvals[3] = 1.5f * s * s - s;									// 1.5f * s * s - s
        }

        /*
         ====================
         idCurve_CatmullRomSpline::BasisSecondDerivative

         second derivative of spline basis functions
         ====================
         */
        protected void BasisSecondDerivative(final int index, final float t, float[] bvals) {
            float s = (float) (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = -3.0f * s + 2.0f;
            bvals[1] = 9.0f * s - 5.0f;
            bvals[2] = -9.0f * s + 4.0f;
            bvals[3] = 3.0f * s - 1.0f;
        }
    };

    /**
     * ===============================================================================
     *
     * Cubic Interpolating Spline template. The curve goes through all the
     * knots. The curve becomes the Catmull-Rom spline if the tension,
     * continuity and bias are all set to zero.
     *
     * ===============================================================================
     */
    class idCurve_KochanekBartelsSpline<type> extends idCurve_Spline<type> {

        public idCurve_KochanekBartelsSpline() {
        }

        /*
         ====================
         idCurve_KochanekBartelsSpline::AddValue

         add a timed/value pair to the spline
         returns the index to the inserted pair
         ====================
         */
        @Override
        public int AddValue(final float time, final type value) {
            int i;

            i = this.IndexForTime(time);
            this.times.Insert(time, i);
            this.values.Insert((Double) value, i);
            tension.Insert(0.0f, i);
            continuity.Insert(0.0f, i);
            bias.Insert(0.0f, i);
            return i;
        }

        /*
         ====================
         idCurve_KochanekBartelsSpline::AddValue

         add a timed/value pair to the spline
         returns the index to the inserted pair
         ====================
         */
        public int AddValue(final float time, final type value, final float tension, final float continuity, final float bias) {
            int i;

            i = this.IndexForTime(time);
            this.times.Insert(time, i);
            this.values.Insert((Double) value, i);
            this.tension.Insert(tension, i);
            this.continuity.Insert(continuity, i);
            this.bias.Insert(bias, i);
            return i;
        }

        @Override
        public void RemoveIndex(final int index) {
            this.values.RemoveIndex(index);
            this.times.RemoveIndex(index);
            tension.RemoveIndex(index);
            continuity.RemoveIndex(index);
            bias.RemoveIndex(index);
        }

        @Override
        public void Clear() {
            this.values.Clear();
            this.times.Clear();
            tension.Clear();
            continuity.Clear();
            bias.Clear();
            this.currentIndex = -1;
        }

        /*
         ====================
         idCurve_KochanekBartelsSpline::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(final float time) {
            int i;
            float[] bvals = new float[4];
            float clampedTime;
            type[] t0 = (type[]) new Object[1], t1 = (type[]) new Object[1];
            Double v;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            TangentsForIndex(i - 1, t0, t1);
            Basis(i - 1, clampedTime, bvals);
            v = bvals[0] * (Double) this.ValueForIndex(i - 1);
            v += bvals[1] * (Double) this.ValueForIndex(i);
            v += bvals[2] * (Double) t0[0];
            v += bvals[3] * (Double) t1[0];
            return (type) v;
        }

        /*
         ====================
         idCurve_KochanekBartelsSpline::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            int i;
            float[] bvals = new float[4];
            float d, clampedTime;
            type[] t0 = (type[]) new Object[1], t1 = (type[]) new Object[1];
            Double v;

            if (this.times.Num() == 1) {
                return (type) (Object) (this.values.oGet(0) - this.values.oGet(0));
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            TangentsForIndex(i - 1, t0, t1);
            Basis(i - 1, clampedTime, bvals);
            v = bvals[0] * (Double) this.ValueForIndex(i - 1);
            v += bvals[1] * (Double) this.ValueForIndex(i);
            v += bvals[2] * (Double) t0[0];
            v += bvals[3] * (Double) t1[0];
            d = (this.TimeForIndex(i) - this.TimeForIndex(i - 1));
            return (type) (Object) (v / d);
        }

        /*
         ====================
         idCurve_KochanekBartelsSpline::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            int i;
            float[] bvals = new float[4];
            float d, clampedTime;
            type[] t0 = (type[]) new Object[1], t1 = (type[]) new Object[1];
            Double v;

            if (this.times.Num() == 1) {
                return (type) (Object) (this.values.oGet(0) - this.values.oGet(0));
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            TangentsForIndex(i - 1, t0, t1);
            Basis(i - 1, clampedTime, bvals);
            v = bvals[0] * (Double) this.ValueForIndex(i - 1);
            v += bvals[1] * (Double) this.ValueForIndex(i);
            v += bvals[2] * (Double) t0[0];
            v += bvals[3] * (Double) t1[0];
            d = (this.TimeForIndex(i) - this.TimeForIndex(i - 1));
            return (type) (Object) (v / (d * d));
        }
        protected idList<Float> tension;
        protected idList<Float> continuity;
        protected idList<Float> bias;

        protected void TangentsForIndex(final int index, type[] t0, type[] t1) {
            float dt, omt, omc, opc, omb, opb, adj, s0, s1;
            double delta;

            delta = (Double) this.ValueForIndex(index + 1) - (Double) this.ValueForIndex(index);
            dt = this.TimeForIndex(index + 1) - this.TimeForIndex(index);

            omt = 1.0f - tension.oGet(index);
            omc = 1.0f - continuity.oGet(index);
            opc = 1.0f + continuity.oGet(index);
            omb = 1.0f - bias.oGet(index);
            opb = 1.0f + bias.oGet(index);
            adj = 2.0f * dt / (this.TimeForIndex(index + 1) - this.TimeForIndex(index - 1));
            s0 = 0.5f * adj * omt * opc * opb;
            s1 = 0.5f * adj * omt * omc * omb;

            // outgoing tangent at first point
            t0[0] = (type) (Object) (s1 * delta + s0 * ((Double) this.ValueForIndex(index) - (Double) this.ValueForIndex(index - 1)));

            omt = 1.0f - tension.oGet(index + 1);
            omc = 1.0f - continuity.oGet(index + 1);
            opc = 1.0f + continuity.oGet(index + 1);
            omb = 1.0f - bias.oGet(index + 1);
            opb = 1.0f + bias.oGet(index + 1);
            adj = 2.0f * dt / (this.TimeForIndex(index + 2) - this.TimeForIndex(index));
            s0 = 0.5f * adj * omt * omc * opb;
            s1 = 0.5f * adj * omt * opc * omb;

            // incoming tangent at second point
            t1[0] = (type) (Object) (s1 * ((Double) this.ValueForIndex(index + 2) - (Double) this.ValueForIndex(index + 1)) + s0 * delta);
        }

        /*
         ====================
         idCurve_KochanekBartelsSpline::Basis

         spline basis functions
         ====================
         */
        protected void Basis(final int index, final float t, float[] bvals) {
            float s = (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = ((2.0f * s - 3.0f) * s) * s + 1.0f;				// 2.0f * s * s * s - 3.0f * s * s + 1.0f
            bvals[1] = ((-2.0f * s + 3.0f) * s) * s;					// -2.0f * s * s * s + 3.0f * s * s
            bvals[2] = ((s - 2.0f) * s) * s + s;						// s * s * s - 2.0f * s * s + s
            bvals[3] = ((s - 1.0f) * s) * s;							// s * s * s - s * s
        }

        /*
         ====================
         idCurve_KochanekBartelsSpline::BasisFirstDerivative

         first derivative of spline basis functions
         ====================
         */
        protected void BasisFirstDerivative(final int index, final float t, float[] bvals) {
            float s = (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = (6.0f * s - 6.0f) * s;								// 6.0f * s * s - 6.0f * s
            bvals[1] = (-6.0f * s + 6.0f) * s;							// -6.0f * s * s + 6.0f * s
            bvals[2] = (3.0f * s - 4.0f) * s + 1.0f;						// 3.0f * s * s - 4.0f * s + 1.0f
            bvals[3] = (3.0f * s - 2.0f) * s;								// 3.0f * s * s - 2.0f * s
        }

        /*
         ====================
         idCurve_KochanekBartelsSpline::BasisSecondDerivative

         second derivative of spline basis functions
         ====================
         */
        protected void BasisSecondDerivative(final int index, final float t, float[] bvals) {
            float s = (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = 12.0f * s - 6.0f;
            bvals[1] = -12.0f * s + 6.0f;
            bvals[2] = 6.0f * s - 4.0f;
            bvals[3] = 6.0f * s - 2.0f;
        }
    };

    /**
     * ===============================================================================
     *
     * B-Spline base template. Uses recursive definition and is slow. Use
     * idCurve_UniformCubicBSpline or idCurve_NonUniformBSpline instead.
     *
     * ===============================================================================
     */
    public static class idCurve_BSpline<type> extends idCurve_Spline<type> {

        public idCurve_BSpline() {
            order = 4;	// default to cubic
        }

        public int GetOrder() {
            return order;
        }

        public void SetOrder(final int i) {
            assert (i > 0 && i < 10);
            order = i;
        }

        /*
         ====================
         idCurve_BSpline::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            int i, j, k;
            float clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < order; j++) {
                k = i + j - (order >> 1);
                v += Basis(k - 2, order, clampedTime) * (Double) this.ValueForIndex(k);
            }
            return (type) v;
        }

        /*
         ====================
         idCurve_BSpline::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            int i, j, k;
            float clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < order; j++) {
                k = i + j - (order >> 1);
                v += BasisFirstDerivative(k - 2, order, clampedTime) * (Double) this.ValueForIndex(k);
            }
            return (type) v;
        }

        /*
         ====================
         idCurve_BSpline::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            int i, j, k;
            float clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < order; j++) {
                k = i + j - (order >> 1);
                v += BasisSecondDerivative(k - 2, order, clampedTime) * (Double) this.ValueForIndex(k);
            }
            return (type) v;
        }
        protected int order;

        /*
         ====================
         idCurve_BSpline::Basis

         spline basis function
         ====================
         */
        protected float Basis(final int index, final int order, final float t) {
            if (order <= 1) {
                if (this.TimeForIndex(index) < t && t <= this.TimeForIndex(index + 1)) {
                    return 1.0f;
                } else {
                    return 0.0f;
                }
            } else {
                float sum = 0.0f;
                float d1 = this.TimeForIndex(index + order - 1) - this.TimeForIndex(index);
                if (d1 != 0.0f) {
                    sum += (float) (t - this.TimeForIndex(index)) * Basis(index, order - 1, t) / d1;
                }

                float d2 = this.TimeForIndex(index + order) - this.TimeForIndex(index + 1);
                if (d2 != 0.0f) {
                    sum += (float) (this.TimeForIndex(index + order) - t) * Basis(index + 1, order - 1, t) / d2;
                }
                return sum;
            }
        }

        /*
         ====================
         idCurve_BSpline::BasisFirstDerivative

         first derivative of spline basis function
         ====================
         */
        protected float BasisFirstDerivative(final int index, final int order, final float t) {
            return (Basis(index, order - 1, t) - Basis(index + 1, order - 1, t))
                    * (float) (order - 1) / (this.TimeForIndex(index + (order - 1) - 2) - this.TimeForIndex(index - 2));
        }

        /*
         ====================
         idCurve_BSpline::BasisSecondDerivative

         second derivative of spline basis function
         ====================
         */
        protected float BasisSecondDerivative(final int index, final int order, final float t) {
            return (BasisFirstDerivative(index, order - 1, t) - BasisFirstDerivative(index + 1, order - 1, t))
                    * (float) (order - 1) / (this.TimeForIndex(index + (order - 1) - 2) - this.TimeForIndex(index - 2));
        }
    };

    /**
     * ===============================================================================
     *
     * Uniform Non-Rational Cubic B-Spline template.
     *
     * ===============================================================================
     */
    class idCurve_UniformCubicBSpline<type> extends idCurve_BSpline<type> {

        public idCurve_UniformCubicBSpline() {
            this.order = 4;	// always cubic
        }

        /*
         ====================
         idCurve_UniformCubicBSpline::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            int i, j, k;
            float[] bvals = new float[4];
            float clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            Basis(i - 1, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < 4; j++) {
                k = i + j - 2;
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            return (type) v;
        }

        /*
         ====================
         idCurve_UniformCubicBSpline::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            int i, j, k;
            float[] bvals = new float[4];
            float d, clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) (Object) (this.values.oGet(0) - this.values.oGet(0));
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            BasisFirstDerivative(i - 1, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < 4; j++) {
                k = i + j - 2;
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            d = (this.TimeForIndex(i) - this.TimeForIndex(i - 1));
            return (type) (Object) (v / d);
        }

        /*
         ====================
         idCurve_UniformCubicBSpline::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            int i, j, k;
            float[] bvals = new float[4];
            float d, clampedTime;
            Double v;

            if (this.times.Num() == 1) {
                return (type) (Object) (this.values.oGet(0) - this.values.oGet(0));
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            BasisSecondDerivative(i - 1, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < 4; j++) {
                k = i + j - 2;
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            d = (this.TimeForIndex(i) - this.TimeForIndex(i - 1));
            return (type) (Object) (v / (d * d));
        }


        /*
         ====================
         idCurve_UniformCubicBSpline::Basis

         spline basis functions
         ====================
         */
        protected void Basis(final int index, final float t, float[] bvals) {
            float s = (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = (((-s + 3.0f) * s - 3.0f) * s + 1.0f) * (1.0f / 6.0f);
            bvals[1] = (((3.0f * s - 6.0f) * s) * s + 4.0f) * (1.0f / 6.0f);
            bvals[2] = (((-3.0f * s + 3.0f) * s + 3.0f) * s + 1.0f) * (1.0f / 6.0f);
            bvals[3] = (s * s * s) * (1.0f / 6.0f);
        }

        /*
         ====================
         idCurve_UniformCubicBSpline::BasisFirstDerivative

         first derivative of spline basis functions
         ====================
         */
        protected void BasisFirstDerivative(final int index, final float t, float[] bvals) {
            float s = (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = -0.5f * s * s + s - 0.5f;
            bvals[1] = 1.5f * s * s - 2.0f * s;
            bvals[2] = -1.5f * s * s + s + 0.5f;
            bvals[3] = 0.5f * s * s;
        }

        /*
         ====================
         idCurve_UniformCubicBSpline::BasisSecondDerivative

         second derivative of spline basis functions
         ====================
         */
        protected void BasisSecondDerivative(final int index, final float t, float[] bvals) {
            float s = (t - this.TimeForIndex(index)) / (this.TimeForIndex(index + 1) - this.TimeForIndex(index));
            bvals[0] = -s + 1.0f;
            bvals[1] = 3.0f * s - 2.0f;
            bvals[2] = -3.0f * s + 1.0f;
            bvals[3] = s;
        }
    };

    /**
     * ===============================================================================
     *
     * Non-Uniform Non-Rational B-Spline (NUBS) template.
     *
     * ===============================================================================
     */
    public static class idCurve_NonUniformBSpline<type> extends idCurve_BSpline<type> {

        public idCurve_NonUniformBSpline() {
        }


        /*
         ====================
         idCurve_NonUniformBSpline::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            int i, j, k;
            float clampedTime;
            Double v;
//	float *bvals = (float *) _alloca16( this.order * sizeof(float) );
            float[] bvals = new float[this.order];

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            Basis(i - 1, this.order, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < this.order; j++) {
                k = i + j - (this.order >> 1);
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            return (type) v;
        }

        /*
         ====================
         idCurve_NonUniformBSpline::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            int i, j, k;
            float clampedTime;
            Double v;
//	float *bvals = (float *) _alloca16( this.order * sizeof(float) );
            float[] bvals = new float[this.order];

            if (this.times.Num() == 1) {
                return (type) (Object) (this.values.oGet(0) - this.values.oGet(0));
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            BasisFirstDerivative(i - 1, this.order, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < this.order; j++) {
                k = i + j - (this.order >> 1);
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            return (type) v;
        }

        /*
         ====================
         idCurve_NonUniformBSpline::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            int i, j, k;
            float clampedTime;
            Double v;
//	float *bvals = (float *) _alloca16( this.order * sizeof(float) );
            float[] bvals = new float[this.order];

            if (this.times.Num() == 1) {
                return (type) (Object) (this.values.oGet(0) - this.values.oGet(0));
            }

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            BasisSecondDerivative(i - 1, this.order, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            for (j = 0; j < this.order; j++) {
                k = i + j - (this.order >> 1);
                v += bvals[j] * (Double) this.ValueForIndex(k);
            }
            return (type) v;
        }


        /*
         ====================
         idCurve_NonUniformBSpline::Basis

         spline basis functions
         ====================
         */
        protected void Basis(final int index, final int order, final float t, float[] bvals) {
            int r, s, i;
            float omega;

            bvals[order - 1] = 1.0f;
            for (r = 2; r <= order; r++) {
                i = index - r + 1;
                bvals[order - r] = 0.0f;
                for (s = order - r + 1; s < order; s++) {
                    i++;
                    omega = (t - this.TimeForIndex(i)) / (this.TimeForIndex(i + r - 1) - this.TimeForIndex(i));
                    bvals[s - 1] += (1.0f - omega) * bvals[s];
                    bvals[s] *= omega;
                }
            }
        }

        /*
         ====================
         idCurve_NonUniformBSpline::BasisFirstDerivative

         first derivative of spline basis functions
         ====================
         */
        protected void BasisFirstDerivative(final int index, final int order, final float t, float[] bvals) {
            int i;

            float[] bvals_1 = Arrays.copyOfRange(bvals, 1, bvals.length - 1);//TODO:check copy back mechanism
            Basis(index, order - 1, t, bvals_1);
            System.arraycopy(bvals_1, 0, bvals, 1, bvals_1.length);

            bvals[0] = 0.0f;
            for (i = 0; i < order - 1; i++) {
                bvals[i] -= bvals[i + 1];
                bvals[i] *= (order - 1) / (this.TimeForIndex(index + i + (order - 1) - 2) - this.TimeForIndex(index + i - 2));
            }
            bvals[i] *= (order - 1) / (this.TimeForIndex(index + i + (order - 1) - 2) - this.TimeForIndex(index + i - 2));
        }

        /*
         ====================
         idCurve_NonUniformBSpline::BasisSecondDerivative

         second derivative of spline basis functions
         ====================
         */
        protected void BasisSecondDerivative(final int index, final int order, final float t, float[] bvals) {
            int i;

            float[] bvals_1 = Arrays.copyOfRange(bvals, 1, bvals.length - 1);//TODO:check copy back mechanism
            BasisFirstDerivative(index, order - 1, t, bvals_1);
            System.arraycopy(bvals_1, 0, bvals, 1, bvals_1.length);

            bvals[0] = 0.0f;
            for (i = 0; i < order - 1; i++) {
                bvals[i] -= bvals[i + 1];
                bvals[i] *= (order - 1) / (this.TimeForIndex(index + i + (order - 1) - 2) - this.TimeForIndex(index + i - 2));
            }
            bvals[i] *= (order - 1) / (this.TimeForIndex(index + i + (order - 1) - 2) - this.TimeForIndex(index + i - 2));
        }
    };

    /*
     ===============================================================================

     Non-Uniform Rational B-Spline (NURBS) template.

     ===============================================================================
     */
    public static class idCurve_NURBS<type> extends idCurve_NonUniformBSpline<type> {

        public idCurve_NURBS() {
        }

        /*
         ====================
         idCurve_NURBS::AddValue

         add a timed/value pair to the spline
         returns the index to the inserted pair
         ====================
         */
        @Override
        public int AddValue(final float time, final type value) {
            int i;

            i = this.IndexForTime(time);
            this.times.Insert(time, i);
            this.values.Insert((Double) value, i);
            weights.Insert(1.0f, i);
            return i;
        }

        /*
         ====================
         idCurve_NURBS::AddValue

         add a timed/value pair to the spline
         returns the index to the inserted pair
         ====================
         */
        public int AddValue(final float time, final type value, final float weight) {
            int i;

            i = this.IndexForTime(time);
            this.times.Insert(time, i);
            this.values.Insert((Double) value, i);
            weights.Insert(weight, i);
            return i;
        }

        @Override
        public void RemoveIndex(final int index) {
            this.values.RemoveIndex(index);
            this.times.RemoveIndex(index);
            weights.RemoveIndex(index);
        }

        @Override
        public void Clear() {
            this.values.Clear();
            this.times.Clear();
            weights.Clear();
            this.currentIndex = -1;
        }

        /*
         ====================
         idCurve_NURBS::GetCurrentValue

         get the value for the given time
         ====================
         */
        @Override
        public type GetCurrentValue(float time) {
            int i, j, k;
            float w, b, clampedTime;
            float[] bvals;
            Double v;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            bvals = new float[this.order];

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            this.Basis(i - 1, this.order, clampedTime, bvals);
            v = this.values.oGet(0) - this.values.oGet(0);
            w = 0.0f;
            for (j = 0; j < this.order; j++) {
                k = i + j - (this.order >> 1);
                b = bvals[j] * WeightForIndex(k);
                w += b;
                v += b * (Double) this.ValueForIndex(k);
            }
            return (type) (Object) (v / w);
        }

        /*
         ====================
         idCurve_NURBS::GetCurrentFirstDerivative

         get the first derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentFirstDerivative(float time) {
            int i, j, k;
            float w, wb, wd1, b, d1, clampedTime;
            float[] bvals, d1vals;
            Double v, vb, vd1;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

//	bvals = (float *) _alloca16( this.order * sizeof(float) );
//	d1vals = (float *) _alloca16( this.order * sizeof(float) );
            bvals = new float[this.order];
            d1vals = new float[this.order];

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            this.Basis(i - 1, this.order, clampedTime, bvals);
            this.BasisFirstDerivative(i - 1, this.order, clampedTime, d1vals);
            vb = vd1 = this.values.oGet(0) - this.values.oGet(0);
            wb = wd1 = 0.0f;
            for (j = 0; j < this.order; j++) {
                k = i + j - (this.order >> 1);
                w = WeightForIndex(k);
                b = bvals[j] * w;
                d1 = d1vals[j] * w;
                wb += b;
                wd1 += d1;
                v = (Double) this.ValueForIndex(k);
                vb += b * v;
                vd1 += d1 * v;
            }
            return (type) (Object) ((wb * vd1 - vb * wd1) / (wb * wb));
        }

        /*
         ====================
         idCurve_NURBS::GetCurrentSecondDerivative

         get the second derivative for the given time
         ====================
         */
        @Override
        public type GetCurrentSecondDerivative(float time) {
            int i, j, k;
            float w, wb, wd1, wd2, b, d1, d2, clampedTime;
            float[] bvals, d1vals, d2vals;
            double v, vb, vd1, vd2;

            if (this.times.Num() == 1) {
                return (type) this.values.oGet(0);
            }

            bvals = new float[this.order];
            d1vals = new float[this.order];
            d2vals = new float[this.order];

            clampedTime = this.ClampedTime(time);
            i = this.IndexForTime(clampedTime);
            this.Basis(i - 1, this.order, clampedTime, bvals);
            this.BasisFirstDerivative(i - 1, this.order, clampedTime, d1vals);
            this.BasisSecondDerivative(i - 1, this.order, clampedTime, d2vals);
            vb = vd1 = vd2 = this.values.oGet(0) - this.values.oGet(0);
            wb = wd1 = wd2 = 0.0f;
            for (j = 0; j < this.order; j++) {
                k = i + j - (this.order >> 1);
                w = WeightForIndex(k);
                b = bvals[j] * w;
                d1 = d1vals[j] * w;
                d2 = d2vals[j] * w;
                wb += b;
                wd1 += d1;
                wd2 += d2;
                v = (Double) this.ValueForIndex(k);
                vb += b * v;
                vd1 += d1 * v;
                vd2 += d2 * v;
            }
            return (type) (Object) (((wb * wb) * (wb * vd2 - vb * wd2) - (wb * vd1 - vb * wd1) * 2.0f * wb * wd1) / (wb * wb * wb * wb));
        }
        protected idList<Float> weights;

        protected float WeightForIndex(final int index) {
            int n = weights.Num() - 1;

            if (index < 0) {
                if (this.boundaryType == idCurve_Spline.BT_CLOSED) {
                    return weights.oGet(weights.Num() + index % weights.Num());
                } else {
                    return weights.oGet(0) + index * (weights.oGet(1) - weights.oGet(0));
                }
            } else if (index > n) {
                if (this.boundaryType == idCurve_Spline.BT_CLOSED) {
                    return weights.oGet(index % weights.Num());
                } else {
                    return weights.oGet(n) + (index - n) * (weights.oGet(n) - weights.oGet(n - 1));
                }
            }
            return weights.oGet(index);
        }
    };
}
