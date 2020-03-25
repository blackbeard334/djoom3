package neo.idlib.math;

import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class Complex {

    /*
     ===============================================================================

     Complex number

     ===============================================================================
     */
    static class idComplex {

        public float r;		// real part
        public float i;		// imaginary part

        public idComplex() {
        }

        public idComplex(final float r, final float i) {
            this.r = r;
            this.i = i;
        }

        public void Set(final float r, final float i) {
            this.r = r;
            this.i = i;
        }

        public void Zero() {
            this.r = this.i = 0.0f;
        }

//public	float				operator[]( int index ) final;
        public float oGet(int index) {
            assert ((index >= 0) && (index < 2));
            if (0 == index) {
                return this.r;
            } else {
                return this.i;
            }
        }

        public void oSet(int index, float value) {
            assert ((index >= 0) && (index < 2));
            if (0 == index) {
                this.r = value;
            } else {
                this.i = value;
            }
        }
//public		float &				operator[]( int index );
//
//public		idComplex			operator-() final;

        public idComplex oNegative() {
            return new idComplex(-this.r, -this.i);
        }
//public		idComplex &			operator=( final idComplex &a );

        public idComplex oSet(final idComplex a) {
            this.r = a.r;
            this.i = a.i;
            return this;
        }
//
//public		idComplex			operator*( final idComplex &a ) final;

        public idComplex oMultiply(final idComplex a) {
            return new idComplex((this.r * a.r) - (this.i * a.i), (this.i * a.r) + (this.r * a.i));
        }
//public		idComplex			operator/( final idComplex &a ) final;

        public idComplex oDivide(final idComplex a) {
            float s, t;
            if (idMath.Fabs(a.r) >= idMath.Fabs(a.i)) {
                s = a.i / a.r;
                t = 1.0f / (a.r + (s * a.i));
                return new idComplex((this.r + (s * this.i)) * t, (this.i - (s * this.r)) * t);
            } else {
                s = a.r / a.i;
                t = 1.0f / ((s * a.r) + a.i);
                return new idComplex(((this.r * s) + this.i) * t, ((this.i * s) - this.r) * t);
            }
        }
//public		idComplex			operator+( final idComplex &a ) final;

        public idComplex oPlus(final idComplex a) {
            return new idComplex(this.r + a.r, this.i + a.i);
        }
//public		idComplex			operator-( final idComplex &a ) final;

        public idComplex oMinus(final idComplex a) {
            return new idComplex(this.r - a.r, this.i - a.i);
        }
//
//public		idComplex &			operator*=( final idComplex &a );

        public idComplex oMulSet(final idComplex a) {
            this.Set((this.r * a.r) - (this.i * a.i), (this.i * a.r) + (this.r * a.i));
            return this;
        }
//public		idComplex &			operator/=( final idComplex &a );

        public idComplex oDivSet(final idComplex a) {
            float s, t;
            if (idMath.Fabs(a.r) >= idMath.Fabs(a.i)) {
                s = a.i / a.r;
                t = 1.0f / (a.r + (s * a.i));
                this.Set((this.r + (s * this.i)) * t, (this.i - (s * this.r)) * t);
            } else {
                s = a.r / a.i;
                t = 1.0f / ((s * a.r) + a.i);
                this.Set(((this.r * s) + this.i) * t, ((this.i * s) - this.r) * t);
            }
            return this;
        }
//public		idComplex &			operator+=( final idComplex &a );

        public idComplex oPluSet(final idComplex a) {
            this.r += a.r;
            this.i += a.i;
            return this;
        }
//public		idComplex &			operator-=( final idComplex &a );

        public idComplex oMinSet(final idComplex a) {
            this.r -= a.r;
            this.i -= a.i;
            return this;
        }
//
//public		idComplex			operator*( final float a ) final;

        public idComplex oMultiply(final float a) {
            return new idComplex(this.r * a, this.i * a);
        }
//public		idComplex			operator/( final float a ) final;

        public idComplex oDivide(final float a) {
            final float s = 1.0f / a;
            return new idComplex(this.r * s, this.i * s);
        }
//public		idComplex			operator+( final float a ) final;

        public idComplex oPlus(final float a) {
            return new idComplex(this.r + a, this.i);
        }
//public		idComplex			operator-( final float a ) final;

        public idComplex oMinus(final float a) {
            return new idComplex(this.r - a, this.i);
        }
//
//public		idComplex &			operator*=( final float a );

        public idComplex oMulSet(final float a) {
            this.r *= a;
            this.i *= a;
            return this;
        }
//public		idComplex &			operator/=( final float a );

        public idComplex oDivSet(final float a) {
            final float s = 1.0f / a;
            this.r *= s;
            this.i *= s;
            return this;
        }
//public		idComplex &			operator+=( final float a );

        public idComplex oPluSet(final float a) {
            this.r += a;
            return this;
        }
//public		idComplex &			operator-=( final float a );

        public idComplex oMinSet(final float a) {
            this.r -= a;
            return this;
        }
//
//public		friend idComplex	operator*( final float a, final idComplex &b );

        public static idComplex oMultiply(final float a, final idComplex b) {
            return new idComplex(a * b.r, a * b.i);
        }
//public		friend idComplex	operator/( final float a, final idComplex &b );

        public static idComplex oDivide(final float a, final idComplex b) {
            float s, t;
            if (idMath.Fabs(b.r) >= idMath.Fabs(b.i)) {
                s = b.i / b.r;
                t = a / (b.r + (s * b.i));
                return new idComplex(t, -s * t);
            } else {
                s = b.r / b.i;
                t = a / ((s * b.r) + b.i);
                return new idComplex(s * t, -t);
            }
        }
//public		friend idComplex	operator+( final float a, final idComplex &b );

        public static idComplex oPlus(final float a, final idComplex b) {
            return new idComplex(a + b.r, b.i);
        }
//public		friend idComplex	operator-( final float a, final idComplex &b );

        public static idComplex oMinus(final float a, final idComplex b) {
            return new idComplex(a - b.r, -b.i);
        }
//

        public boolean Compare(final idComplex a) {// exact compare, no epsilon
            return ((this.r == a.r) && (this.i == a.i));
        }

        public boolean Compare(final idComplex a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(this.r - a.r) > epsilon) {
                return false;
            }
            if (idMath.Fabs(this.i - a.i) > epsilon) {
                return false;
            }
            return true;
        }
//public		boolean				operator==(	final idComplex &a ) final;						// exact compare, no epsilon
//public		boolean				operator!=(	final idComplex &a ) final;						// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 7;
            hash = (29 * hash) + Float.floatToIntBits(this.r);
            hash = (29 * hash) + Float.floatToIntBits(this.i);
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
            final idComplex other = (idComplex) obj;
            if (Float.floatToIntBits(this.r) != Float.floatToIntBits(other.r)) {
                return false;
            }
            if (Float.floatToIntBits(this.i) != Float.floatToIntBits(other.i)) {
                return false;
            }
            return true;
        }

        public idComplex Reciprocal() {
            float s, t;
            if (idMath.Fabs(this.r) >= idMath.Fabs(this.i)) {
                s = this.i / this.r;
                t = 1.0f / (this.r + (s * this.i));
                return new idComplex(t, -s * t);
            } else {
                s = this.r / this.i;
                t = 1.0f / ((s * this.r) + this.i);
                return new idComplex(s * t, -t);
            }
        }

        public idComplex Sqrt() {
            float x, y, w;

            if ((this.r == 0.0f) && (this.i == 0.0f)) {
                return new idComplex(0.0f, 0.0f);
            }
            x = idMath.Fabs(this.r);
            y = idMath.Fabs(this.i);
            if (x >= y) {
                w = y / x;
                w = idMath.Sqrt(x) * idMath.Sqrt(0.5f * (1.0f + idMath.Sqrt(1.0f + (w * w))));
            } else {
                w = x / y;
                w = idMath.Sqrt(y) * idMath.Sqrt(0.5f * (w + idMath.Sqrt(1.0f + (w * w))));
            }
            if (w == 0.0f) {
                return new idComplex(0.0f, 0.0f);
            }
            if (this.r >= 0.0f) {
                return new idComplex(w, (0.5f * this.i) / w);
            } else {
                return new idComplex((0.5f * y) / w, (this.i >= 0.0f) ? w : -w);
            }
        }

        public float Abs() {
            float x, y, t;
            x = idMath.Fabs(this.r);
            y = idMath.Fabs(this.i);
            if (x == 0.0f) {
                return y;
            } else if (y == 0.0f) {
                return x;
            } else if (x > y) {
                t = y / x;
                return x * idMath.Sqrt(1.0f + (t * t));
            } else {
                t = x / y;
                return y * idMath.Sqrt(1.0f + (t * t));
            }
        }

        public int GetDimension() {
            return 2;
        }
//
//public		final float *		ToFloatPtr( void ) final;
//public		float *				ToFloatPtr( void );
//public		final char *		ToString( int precision = 2 ) final;
    }
}
