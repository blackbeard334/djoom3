package neo.idlib.math;

import java.util.Arrays;
import java.util.stream.Stream;

import neo.idlib.Lib;
import neo.idlib.math.Complex.idComplex;
import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class Polynomial {

    static final float EPSILON = 1e-6f;

    /*
     ===============================================================================

     Polynomial of arbitrary degree with real coefficients.

     ===============================================================================
     */
    public static class idPolynomial {

        private int degree;
        private int allocated;
        private float[] coefficient;
        //
        //

        public idPolynomial() {
            degree = -1;
            allocated = 0;
            coefficient = null;
        }

        public idPolynomial(int d) {
            degree = -1;
            allocated = 0;
            coefficient = null;
            Resize(d, false);
        }

        public idPolynomial(float a, float b) {
            degree = -1;
            allocated = 0;
            coefficient = null;
            Resize(1, false);
            coefficient[0] = b;
            coefficient[1] = a;
        }

        public idPolynomial(float a, float b, float c) {
            degree = -1;
            allocated = 0;
            coefficient = null;
            Resize(2, false);
            coefficient[0] = c;
            coefficient[1] = b;
            coefficient[2] = a;
        }

        public idPolynomial(float a, float b, float c, float d) {
            degree = -1;
            allocated = 0;
            coefficient = null;
            Resize(3, false);
            coefficient[0] = d;
            coefficient[1] = c;
            coefficient[2] = b;
            coefficient[3] = a;
        }

        public idPolynomial(float a, float b, float c, float d, float e) {
            degree = -1;
            allocated = 0;
            coefficient = null;
            Resize(4, false);
            coefficient[0] = e;
            coefficient[1] = d;
            coefficient[2] = c;
            coefficient[3] = b;
            coefficient[4] = a;
        }

        public idPolynomial(idPolynomial p) {
            this.allocated = p.allocated;
            System.arraycopy(p.coefficient, 0, this.coefficient, 0, p.coefficient.length);
            this.degree = p.degree;
        }
//
//public	float			operator[]( int index ) const;

        public float oGet(int index) {
            assert (index >= 0 && index <= degree);
            return coefficient[ index];
        }

        public idPolynomial oNegative() {
            int i;
            idPolynomial n = new idPolynomial();

//            n = new idPolynomial(this);
            n.oSet(this);
            for (i = 0; i <= degree; i++) {
                n.coefficient[i] = -n.coefficient[i];
            }
            return n;
        }

        public idPolynomial oSet(final idPolynomial p) {
            Resize(p.degree, false);
            System.arraycopy(p.coefficient, 0, coefficient, 0, degree + 1);
            return this;
        }

        public idPolynomial oPlus(final idPolynomial p) {
            int i;
            idPolynomial n = new idPolynomial();

            if (degree > p.degree) {
                n.Resize(degree, false);
                for (i = 0; i <= p.degree; i++) {
                    n.coefficient[i] = coefficient[i] + p.coefficient[i];
                }
                for (; i <= degree; i++) {
                    n.coefficient[i] = coefficient[i];
                }
                n.degree = degree;
            } else if (p.degree > degree) {
                n.Resize(p.degree, false);
                for (i = 0; i <= degree; i++) {
                    n.coefficient[i] = coefficient[i] + p.coefficient[i];
                }
                for (; i <= p.degree; i++) {
                    n.coefficient[i] = p.coefficient[i];
                }
                n.degree = p.degree;
            } else {
                n.Resize(degree, false);
                n.degree = 0;
                for (i = 0; i <= degree; i++) {
                    n.coefficient[i] = coefficient[i] + p.coefficient[i];
                    if (n.coefficient[i] != 0.0f) {
                        n.degree = i;
                    }
                }
            }
            return n;
        }

        public idPolynomial oMinus(final idPolynomial p) {
            int i;
            idPolynomial n = new idPolynomial();

            if (degree > p.degree) {
                n.Resize(degree, false);
                for (i = 0; i <= p.degree; i++) {
                    n.coefficient[i] = coefficient[i] - p.coefficient[i];
                }
                for (; i <= degree; i++) {
                    n.coefficient[i] = coefficient[i];
                }
                n.degree = degree;
            } else if (p.degree >= degree) {
                n.Resize(p.degree, false);
                for (i = 0; i <= degree; i++) {
                    n.coefficient[i] = coefficient[i] - p.coefficient[i];
                }
                for (; i <= p.degree; i++) {
                    n.coefficient[i] = -p.coefficient[i];
                }
                n.degree = p.degree;
            } else {
                n.Resize(degree, false);
                n.degree = 0;
                for (i = 0; i <= degree; i++) {
                    n.coefficient[i] = coefficient[i] - p.coefficient[i];
                    if (n.coefficient[i] != 0.0f) {
                        n.degree = i;
                    }
                }
            }
            return n;
        }

        public idPolynomial oMultiply(final float s) {
            idPolynomial n = new idPolynomial();

            if (s == 0.0f) {
                n.degree = 0;
            } else {
                n.Resize(degree, false);
                for (int i = 0; i <= degree; i++) {
                    n.coefficient[i] = coefficient[i] * s;
                }
            }
            return n;
        }

        public idPolynomial oDivide(final float s) {
            float invs;
            idPolynomial n = new idPolynomial();

            assert (s != 0.0f);
            n.Resize(degree, false);
            invs = 1.0f / s;
            for (int i = 0; i <= degree; i++) {
                n.coefficient[i] = coefficient[i] * invs;
            }
            return n;
        }

        public idPolynomial oPluSet(final idPolynomial p) {
            int i;

            if (degree > p.degree) {
                for (i = 0; i <= p.degree; i++) {
                    coefficient[i] += p.coefficient[i];
                }
            } else if (p.degree > degree) {
                Resize(p.degree, true);
                for (i = 0; i <= degree; i++) {
                    coefficient[i] += p.coefficient[i];
                }
                for (; i <= p.degree; i++) {
                    coefficient[i] = p.coefficient[i];
                }
            } else {
                for (i = 0; i <= degree; i++) {
                    coefficient[i] += p.coefficient[i];
                    if (coefficient[i] != 0.0f) {
                        degree = i;
                    }
                }
            }
            return this;
        }

        public idPolynomial oMinSet(final idPolynomial p) {
            int i;

            if (degree > p.degree) {
                for (i = 0; i <= p.degree; i++) {
                    coefficient[i] -= p.coefficient[i];
                }
            } else if (p.degree > degree) {
                Resize(p.degree, true);
                for (i = 0; i <= degree; i++) {
                    coefficient[i] -= p.coefficient[i];
                }
                for (; i <= p.degree; i++) {
                    coefficient[i] = -p.coefficient[i];
                }
            } else {
                for (i = 0; i <= degree; i++) {
                    coefficient[i] -= p.coefficient[i];
                    if (coefficient[i] != 0.0f) {
                        degree = i;
                    }
                }
            }
            return this;
        }

        public idPolynomial oMulSet(final float s) {
            if (s == 0.0f) {
                degree = 0;
            } else {
                for (int i = 0; i <= degree; i++) {
                    coefficient[i] *= s;
                }
            }
            return this;
        }

        public idPolynomial oDivSet(final float s) {
            float invs;

            assert (s != 0.0f);
            invs = 1.0f / s;
            for (int i = 0; i <= degree; i++) {
                coefficient[i] = invs;
            }
            return this;
        }

        public boolean Compare(final idPolynomial p) {// exact compare, no epsilon
            if (degree != p.degree) {
                return false;
            }
            for (int i = 0; i <= degree; i++) {
                if (coefficient[i] != p.coefficient[i]) {
                    return false;
                }
            }
            return true;
        }

        public boolean Compare(final idPolynomial p, final float epsilon) {// compare with epsilon
            if (degree != p.degree) {
                return false;
            }
            for (int i = 0; i <= degree; i++) {
                if (idMath.Fabs(coefficient[i] - p.coefficient[i]) > epsilon) {
                    return false;
                }
            }
            return true;
        }
//public	boolean			operator==(	const idPolynomial &p ) const;					// exact compare, no epsilon
//public	boolean			operator!=(	const idPolynomial &p ) const;					// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + this.degree;
            hash = 43 * hash + Arrays.hashCode(this.coefficient);
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
            final idPolynomial other = (idPolynomial) obj;
            if (this.degree != other.degree) {
                return false;
            }
            if (!Arrays.equals(this.coefficient, other.coefficient)) {
                return false;
            }
            return true;
        }

        public void Zero() {
            degree = 0;
        }

        public void Zero(int d) {
            Resize(d, false);
            for (int i = 0; i <= degree; i++) {
                coefficient[i] = 0.0f;
            }
        }

        public int GetDimension() {// get the degree of the polynomial
            return degree;
        }

        public int GetDegree() {// get the degree of the polynomial
            return degree;
        }

        public float GetValue(final float x) {// evaluate the polynomial with the given real value
            float y, z;
            y = coefficient[0];
            z = x;
            for (int i = 1; i <= degree; i++) {
                y += coefficient[i] * z;
                z *= x;
            }
            return y;
        }

        public idComplex GetValue(final idComplex x) {// evaluate the polynomial with the given complex value
            idComplex y = new idComplex(), z = new idComplex();
            y.Set(coefficient[0], 0.0f);
            z.oSet(x);
            for (int i = 1; i <= degree; i++) {
                y.oPluSet(z.oMultiply(coefficient[i]));
                z.oMulSet(x);
            }
            return y;
        }

        public idPolynomial GetDerivative() {// get the first derivative of the polynomial
            idPolynomial n = new idPolynomial();

            if (degree == 0) {
                return n;
            }
            n.Resize(degree - 1, false);
            for (int i = 1; i <= degree; i++) {
                n.coefficient[i - 1] = i * coefficient[i];
            }
            return n;
        }

        public idPolynomial GetAntiDerivative() {// get the anti derivative of the polynomial
            idPolynomial n = new idPolynomial();

            if (degree == 0) {
                return n;
            }
            n.Resize(degree + 1, false);
            n.coefficient[0] = 0.0f;
            for (int i = 0; i <= degree; i++) {
                n.coefficient[i + 1] = coefficient[i] / (i + 1);
            }
            return n;
        }

        public int GetRoots(idComplex[] roots) {// get all roots
            int i, j;
            idComplex x = new idComplex(), b = new idComplex(), c = new idComplex();
            idComplex[] coef;

            coef = new idComplex[degree + 1];//	coef = (idComplex *) _alloca16( ( degree + 1 ) * sizeof( idComplex ) );
            for (i = 0; i <= degree; i++) {
                coef[i] = new idComplex(coefficient[i], 0.0f);
            }

            for (i = degree - 1; i >= 0; i--) {
                x.Zero();
                Laguer(coef, i + 1, x);
                if (idMath.Fabs(x.i) < 2.0f * EPSILON * idMath.Fabs(x.r)) {
                    x.i = 0.0f;
                }
                roots[i].oSet(x);
                b.oSet(coef[i + 1]);
                for (j = i; j >= 0; j--) {
                    c.oSet(coef[j]);
                    coef[j].oSet(b);
                    b.oSet(x.oMultiply(b).oPlus(c));
                }
            }

            for (i = 0; i <= degree; i++) {
                coef[i].Set(coefficient[i], 0.0f);
            }
            for (i = 0; i < degree; i++) {
                Laguer(coef, degree, roots[i]);
            }

            for (i = 1; i < degree; i++) {
                x.oSet(roots[i]);
                for (j = i - 1; j >= 0; j--) {
                    if (roots[j].r <= x.r) {
                        break;
                    }
                    roots[j + 1].oSet(roots[j]);
                }
                roots[j + 1].oSet(x);
            }

            return degree;
        }

        public int GetRoots(float[] roots) {// get the real roots
            int i, num;
            idComplex[] complexRoots;

            switch (degree) {
                case 0:
                    return 0;
                case 1:
                    return GetRoots1(coefficient[1], coefficient[0], roots);
                case 2:
                    return GetRoots2(coefficient[2], coefficient[1], coefficient[0], roots);
                case 3:
                    return GetRoots3(coefficient[3], coefficient[2], coefficient[1], coefficient[0], roots);
                case 4:
                    return GetRoots4(coefficient[4], coefficient[3], coefficient[2], coefficient[1], coefficient[0], roots);
            }

            // The Abel-Ruffini theorem states that there is no general solution
            // in radicals to polynomial equations of degree five or higher.
            // A polynomial equation can be solved by radicals if and only if
            // its Galois group is a solvable group.
//	complexRoots = (idComplex *) _alloca16( degree * sizeof( idComplex ) );
            complexRoots = new idComplex[degree];

            GetRoots(complexRoots);

            for (num = i = 0; i < degree; i++) {
                if (complexRoots[i].i == 0.0f) {
                    roots[i] = complexRoots[i].r;
                    num++;
                }
            }
            return num;
        }

        public static int GetRoots1(float a, float b, float[] roots) {
            assert (a != 0.0f);
            roots[0] = -b / a;
            return 1;
        }

        public static int GetRoots2(float a, float b, float c, float[] roots) {
            float inva, ds;

            if (a != 1.0f) {
                assert (a != 0.0f);
                inva = 1.0f / a;
                c *= inva;
                b *= inva;
            }
            ds = b * b - 4.0f * c;
            if (ds < 0.0f) {
                return 0;
            } else if (ds > 0.0f) {
                ds = idMath.Sqrt(ds);
                roots[0] = 0.5f * (-b - ds);
                roots[1] = 0.5f * (-b + ds);
                return 2;
            } else {
                roots[0] = 0.5f * -b;
                return 1;
            }
        }

        public static int GetRoots3(float a, float b, float c, float d, float[] roots) {
            float inva, f, g, halfg, ofs, ds, dist, angle, cs, ss, t;

            if (a != 1.0f) {
                assert (a != 0.0f);
                inva = 1.0f / a;
                d *= inva;
                c *= inva;
                b *= inva;
            }

            f = (1.0f / 3.0f) * (3.0f * c - b * b);
            g = (1.0f / 27.0f) * (2.0f * b * b * b - 9.0f * c * b + 27.0f * d);
            halfg = 0.5f * g;
            ofs = (1.0f / 3.0f) * b;
            ds = 0.25f * g * g + (1.0f / 27.0f) * f * f * f;

            if (ds < 0.0f) {
                dist = idMath.Sqrt((-1.0f / 3.0f) * f);
                angle = (1.0f / 3.0f) * idMath.ATan(idMath.Sqrt(-ds), -halfg);
                cs = idMath.Cos(angle);
                ss = idMath.Sin(angle);
                roots[0] = 2.0f * dist * cs - ofs;
                roots[1] = -dist * (cs + idMath.SQRT_THREE * ss) - ofs;
                roots[2] = -dist * (cs - idMath.SQRT_THREE * ss) - ofs;
                return 3;
            } else if (ds > 0.0f) {
                ds = idMath.Sqrt(ds);
                t = -halfg + ds;
                if (t >= 0.0f) {
                    roots[0] = idMath.Pow(t, (1.0f / 3.0f));
                } else {
                    roots[0] = -idMath.Pow(-t, (1.0f / 3.0f));
                }
                t = -halfg - ds;
                if (t >= 0.0f) {
                    roots[0] += idMath.Pow(t, (1.0f / 3.0f));
                } else {
                    roots[0] -= idMath.Pow(-t, (1.0f / 3.0f));
                }
                roots[0] -= ofs;
                return 1;
            } else {
                if (halfg >= 0.0f) {
                    t = -idMath.Pow(halfg, (1.0f / 3.0f));
                } else {
                    t = idMath.Pow(-halfg, (1.0f / 3.0f));
                }
                roots[0] = 2.0f * t - ofs;
                roots[1] = -t - ofs;
                roots[2] = roots[1];
                return 3;
            }
        }

        public static int GetRoots4(float a, float b, float c, float d, float e, float[] roots) {
            int count;
            float inva, y, ds, r, s1, s2, t1, t2, tp, tm;
            float roots3[] = new float[3];

            if (a != 1.0f) {
                assert (a != 0.0f);
                inva = 1.0f / a;
                e *= inva;
                d *= inva;
                c *= inva;
                b *= inva;
            }

            count = 0;

            GetRoots3(1.0f, -c, b * d - 4.0f * e, -b * b * e + 4.0f * c * e - d * d, roots3);
            y = roots3[0];
            ds = 0.25f * b * b - c + y;

            if (ds < 0.0f) {
                return 0;
            } else if (ds > 0.0f) {
                r = idMath.Sqrt(ds);
                t1 = 0.75f * b * b - r * r - 2.0f * c;
                t2 = (4.0f * b * c - 8.0f * d - b * b * b) / (4.0f * r);
                tp = t1 + t2;
                tm = t1 - t2;

                if (tp >= 0.0f) {
                    s1 = idMath.Sqrt(tp);
                    roots[count++] = -0.25f * b + 0.5f * (r + s1);
                    roots[count++] = -0.25f * b + 0.5f * (r - s1);
                }
                if (tm >= 0.0f) {
                    s2 = idMath.Sqrt(tm);
                    roots[count++] = -0.25f * b + 0.5f * (s2 - r);
                    roots[count++] = -0.25f * b - 0.5f * (s2 + r);
                }
                return count;
            } else {
                t2 = y * y - 4.0f * e;
                if (t2 >= 0.0f) {
                    t2 = 2.0f * idMath.Sqrt(t2);
                    t1 = 0.75f * b * b - 2.0f * c;
                    if (t1 + t2 >= 0.0f) {
                        s1 = idMath.Sqrt(t1 + t2);
                        roots[count++] = -0.25f * b + 0.5f * s1;
                        roots[count++] = -0.25f * b - 0.5f * s1;
                    }
                    if (t1 - t2 >= 0.0f) {
                        s2 = idMath.Sqrt(t1 - t2);
                        roots[count++] = -0.25f * b + 0.5f * s2;
                        roots[count++] = -0.25f * b - 0.5f * s2;
                    }
                }
                return count;
            }
        }
//
//public	const float *	ToFloatPtr( void ) const;
//public	float *			ToFloatPtr( void );
//public	const char *	ToString( int precision = 2 ) const;
//
//public	static void		Test( void );
//

        private void Resize(int d, boolean keep) {
            int alloc = (d + 1 + 3) & ~3;
            if (alloc > allocated) {
                float[] ptr = new float[alloc];//float *ptr = (float *) Mem_Alloc16( alloc * sizeof( float ) );
                if (coefficient != null) {
                    if (keep) {
                        System.arraycopy(coefficient, 0, ptr, 0, degree + 1);
                    }
//			Mem_Free16( coefficient );
                }
                allocated = alloc;
                coefficient = ptr;
            }
            degree = d;
        }

        private int Laguer(final idComplex[] coef, final int degree, idComplex x) {
            final int MT = 10, MAX_ITERATIONS = MT * 8;
            final float frac[] = {0.0f, 0.5f, 0.25f, 0.75f, 0.13f, 0.38f, 0.62f, 0.88f, 1.0f};
            int i, j;
            float abx, abp, abm, err;
            idComplex dx, cx, b, d = new idComplex(), f = new idComplex(), g, s, gps, gms, g2;

            for (i = 1; i <= MAX_ITERATIONS; i++) {
                b = coef[degree];
                err = b.Abs();
                d.Zero();
                f.Zero();
                abx = x.Abs();
                for (j = degree - 1; j >= 0; j--) {
                    f = x.oMultiply(f).oPlus(d);
                    d = x.oMultiply(d).oPlus(b);
                    b = x.oMultiply(b).oPlus(coef[j]);
                    err = b.Abs() + abx * err;
                }
                if (b.Abs() < err * EPSILON) {
                    return i;
                }
                g = d.oDivide(b);
                g2 = g.oMultiply(g);
                s = (((g2.oMinus(f.oDivide(b).oMultiply(2.0f))).oMultiply(degree).oMinus(g2)).oMultiply(degree - 1)).Sqrt();
                gps = g.oPlus(s);
                gms = g.oMinus(s);
                abp = gps.Abs();
                abm = gms.Abs();
                if (abp < abm) {
                    gps = gms;
                }
                if (Lib.Max(abp, abm) > 0.0f) {
                    dx = idComplex.oDivide(degree, gps);
                } else {
                    dx = new idComplex(idMath.Cos(i), idMath.Sin(i)).oMultiply(idMath.Exp(idMath.Log(1.0f + abx)));
                }
                cx = x.oMinus(dx);
                if (x == cx) {
                    return i;
                }
                if (i % MT == 0) {
                    x.oSet(cx);
                } else {
                    x.oMinSet(dx.oMultiply(frac[i / MT]));
                }
            }
            return i;
        }

        public static void Test() {
            int i, num;
            float roots[] = new float[4];
            float value;
            idComplex[] complexRoots = Stream.generate(idComplex::new).limit(4).toArray(idComplex[]::new);
            idComplex complexValue;
            idPolynomial p;

            p = new idPolynomial(-5.0f, 4.0f);
            num = p.GetRoots(roots);
            for (i = 0; i < num; i++) {
                value = p.GetValue(roots[i]);
                assert (idMath.Fabs(value) < 1e-4f);
            }

            p = new idPolynomial(-5.0f, 4.0f, 3.0f);
            num = p.GetRoots(roots);
            for (i = 0; i < num; i++) {
                value = p.GetValue(roots[i]);
                assert (idMath.Fabs(value) < 1e-4f);
            }

            p = new idPolynomial(1.0f, 4.0f, 3.0f, -2.0f);
            num = p.GetRoots(roots);
            for (i = 0; i < num; i++) {
                value = p.GetValue(roots[i]);
                assert (idMath.Fabs(value) < 1e-4f);
            }

            p = new idPolynomial(5.0f, 4.0f, 3.0f, -2.0f);
            num = p.GetRoots(roots);
            for (i = 0; i < num; i++) {
                value = p.GetValue(roots[i]);
                assert (idMath.Fabs(value) < 1e-4f);
            }

            p = new idPolynomial(-5.0f, 4.0f, 3.0f, 2.0f, 1.0f);
            num = p.GetRoots(roots);
            for (i = 0; i < num; i++) {
                value = p.GetValue(roots[i]);
                assert (idMath.Fabs(value) < 1e-4f);
            }

            p = new idPolynomial(1.0f, 4.0f, 3.0f, -2.0f);
            num = p.GetRoots(complexRoots);
            for (i = 0; i < num; i++) {
                complexValue = p.GetValue(complexRoots[i]);
                assert (idMath.Fabs(complexValue.r) < 1e-4f && idMath.Fabs(complexValue.i) < 1e-4f);
            }

            p = new idPolynomial(5.0f, 4.0f, 3.0f, -2.0f);
            num = p.GetRoots(complexRoots);
            for (i = 0; i < num; i++) {
                complexValue = p.GetValue(complexRoots[i]);
                assert (idMath.Fabs(complexValue.r) < 1e-4f && idMath.Fabs(complexValue.i) < 1e-4f);
            }
        }
    };
}
