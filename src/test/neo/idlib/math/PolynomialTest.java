package neo.idlib.math;

public class PolynomialTest {

    public void Test() {
        int i, num;
        float roots[] = new float[4];
        float value;
        Complex.idComplex[] complexRoots = new Complex.idComplex[4];
        Complex.idComplex complexValue;
        Polynomial.idPolynomial p;

        p = new Polynomial.idPolynomial(-5.0f, 4.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            assert (Math_h.idMath.Fabs(value) < 1e-4f);
        }

        p = new Polynomial.idPolynomial(-5.0f, 4.0f, 3.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            assert (Math_h.idMath.Fabs(value) < 1e-4f);
        }

        p = new Polynomial.idPolynomial(1.0f, 4.0f, 3.0f, -2.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            assert (Math_h.idMath.Fabs(value) < 1e-4f);
        }

        p = new Polynomial.idPolynomial(5.0f, 4.0f, 3.0f, -2.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            assert (Math_h.idMath.Fabs(value) < 1e-4f);
        }

        p = new Polynomial.idPolynomial(-5.0f, 4.0f, 3.0f, 2.0f, 1.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            assert (Math_h.idMath.Fabs(value) < 1e-4f);
        }

        p = new Polynomial.idPolynomial(1.0f, 4.0f, 3.0f, -2.0f);
        num = p.GetRoots(complexRoots);
        for (i = 0; i < num; i++) {
            complexValue = p.GetValue(complexRoots[i]);
            assert (Math_h.idMath.Fabs(complexValue.r) < 1e-4f && Math_h.idMath.Fabs(complexValue.i) < 1e-4f);
        }

        p = new Polynomial.idPolynomial(5.0f, 4.0f, 3.0f, -2.0f);
        num = p.GetRoots(complexRoots);
        for (i = 0; i < num; i++) {
            complexValue = p.GetValue(complexRoots[i]);
            assert (Math_h.idMath.Fabs(complexValue.r) < 1e-4f && Math_h.idMath.Fabs(complexValue.i) < 1e-4f);
        }
    }
}
