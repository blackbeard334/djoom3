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
            r = i = 0.0f;
        }

//public	float				operator[]( int index ) final;
        public float oGet(int index) {
            assert (index >= 0 && index < 2);
            if (0 == index) {
                return r;
            } else {
                return i;
            }
        }

        public void oSet(int index, float value) {
            assert (index >= 0 && index < 2);
            if (0 == index) {
                r = value;
            } else {
                i = value;
            }
        }
//public		float &				operator[]( int index );
//
//public		idComplex			operator-() final;

        public idComplex oNegative() {
            return new idComplex(-r, -i);
        }
//public		idComplex &			operator=( final idComplex &a );

        public idComplex oSet(final idComplex a) {
            r = a.r;
            i = a.i;
            return this;
        }
//
//public		idComplex			operator*( final idComplex &a ) final;

        public idComplex oMultiply(final idComplex a) {
            return new idComplex(r * a.r - i * a.i, i * a.r + r * a.i);
        }
//public		idComplex			operator/( final idComplex &a ) final;

        public idComplex oDivide(final idComplex a) {
            float s, t;
            if (idMath.Fabs(a.r) >= idMath.Fabs(a.i)) {
                s = a.i / a.r;
                t = 1.0f / (a.r + s * a.i);
                return new idComplex((r + s * i) * t, (i - s * r) * t);
            } else {
                s = a.r / a.i;
                t = 1.0f / (s * a.r + a.i);
                return new idComplex((r * s + i) * t, (i * s - r) * t);
            }
        }
//public		idComplex			operator+( final idComplex &a ) final;

        public idComplex oPlus(final idComplex a) {
            return new idComplex(r + a.r, i + a.i);
        }
//public		idComplex			operator-( final idComplex &a ) final;

        public idComplex oMinus(final idComplex a) {
            return new idComplex(r - a.r, i - a.i);
        }
//
//public		idComplex &			operator*=( final idComplex &a );

        public idComplex oMulSet(final idComplex a) {
            this.Set(r * a.r - i * a.i, i * a.r + r * a.i);
            return this;
        }
//public		idComplex &			operator/=( final idComplex &a );

        public idComplex oDivSet(final idComplex a) {
            float s, t;
            if (idMath.Fabs(a.r) >= idMath.Fabs(a.i)) {
                s = a.i / a.r;
                t = 1.0f / (a.r + s * a.i);
                this.Set((r + s * i) * t, (i - s * r) * t);
            } else {
                s = a.r / a.i;
                t = 1.0f / (s * a.r + a.i);
                this.Set((r * s + i) * t, (i * s - r) * t);
            }
            return this;
        }
//public		idComplex &			operator+=( final idComplex &a );

        public idComplex oPluSet(final idComplex a) {
            r += a.r;
            i += a.i;
            return this;
        }
//public		idComplex &			operator-=( final idComplex &a );

        public idComplex oMinSet(final idComplex a) {
            r -= a.r;
            i -= a.i;
            return this;
        }
//
//public		idComplex			operator*( final float a ) final;

        public idComplex oMultiply(final float a) {
            return new idComplex(r * a, i * a);
        }
//public		idComplex			operator/( final float a ) final;

        public idComplex oDivide(final float a) {
            float s = 1.0f / a;
            return new idComplex(r * s, i * s);
        }
//public		idComplex			operator+( final float a ) final;

        public idComplex oPlus(final float a) {
            return new idComplex(r + a, i);
        }
//public		idComplex			operator-( final float a ) final;

        public idComplex oMinus(final float a) {
            return new idComplex(r - a, i);
        }
//
//public		idComplex &			operator*=( final float a );

        public idComplex oMulSet(final float a) {
            r *= a;
            i *= a;
            return this;
        }
//public		idComplex &			operator/=( final float a );

        public idComplex oDivSet(final float a) {
            float s = 1.0f / a;
            r *= s;
            i *= s;
            return this;
        }
//public		idComplex &			operator+=( final float a );

        public idComplex oPluSet(final float a) {
            r += a;
            return this;
        }
//public		idComplex &			operator-=( final float a );

        public idComplex oMinSet(final float a) {
            r -= a;
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
                t = a / (b.r + s * b.i);
                return new idComplex(t, -s * t);
            } else {
                s = b.r / b.i;
                t = a / (s * b.r + b.i);
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
            return ((r == a.r) && (i == a.i));
        }

        public boolean Compare(final idComplex a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(r - a.r) > epsilon) {
                return false;
            }
            if (idMath.Fabs(i - a.i) > epsilon) {
                return false;
            }
            return true;
        }
//public		boolean				operator==(	final idComplex &a ) final;						// exact compare, no epsilon
//public		boolean				operator!=(	final idComplex &a ) final;						// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Float.floatToIntBits(this.r);
            hash = 29 * hash + Float.floatToIntBits(this.i);
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
            if (idMath.Fabs(r) >= idMath.Fabs(i)) {
                s = i / r;
                t = 1.0f / (r + s * i);
                return new idComplex(t, -s * t);
            } else {
                s = r / i;
                t = 1.0f / (s * r + i);
                return new idComplex(s * t, -t);
            }
        }

        public idComplex Sqrt() {
            float x, y, w;

            if (r == 0.0f && i == 0.0f) {
                return new idComplex(0.0f, 0.0f);
            }
            x = idMath.Fabs(r);
            y = idMath.Fabs(i);
            if (x >= y) {
                w = y / x;
                w = idMath.Sqrt(x) * idMath.Sqrt(0.5f * (1.0f + idMath.Sqrt(1.0f + w * w)));
            } else {
                w = x / y;
                w = idMath.Sqrt(y) * idMath.Sqrt(0.5f * (w + idMath.Sqrt(1.0f + w * w)));
            }
            if (w == 0.0f) {
                return new idComplex(0.0f, 0.0f);
            }
            if (r >= 0.0f) {
                return new idComplex(w, 0.5f * i / w);
            } else {
                return new idComplex(0.5f * y / w, (i >= 0.0f) ? w : -w);
            }
        }

        public float Abs() {
            float x, y, t;
            x = idMath.Fabs(r);
            y = idMath.Fabs(i);
            if (x == 0.0f) {
                return y;
            } else if (y == 0.0f) {
                return x;
            } else if (x > y) {
                t = y / x;
                return x * idMath.Sqrt(1.0f + t * t);
            } else {
                t = x / y;
                return y * idMath.Sqrt(1.0f + t * t);
            }
        }

        public int GetDimension() {
            return 2;
        }
//
//public		final float *		ToFloatPtr( void ) final;
//public		float *				ToFloatPtr( void );
//public		final char *		ToString( int precision = 2 ) final;
    };
}
