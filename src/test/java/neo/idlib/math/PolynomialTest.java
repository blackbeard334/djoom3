package neo.idlib.math;

import neo.idlib.math.Complex.idComplex;
import neo.idlib.math.Math_h.idMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

import static neo.idlib.math.Polynomial.idPolynomial;

public class PolynomialTest {
    private int          i;
    private int          num;
    private float[]      roots;
    private float        value;
    private idComplex[]  complexRoots;
    private idComplex    complexValue;
    private idPolynomial p;

    @Before
    public void setUp() throws Exception {
        idMath.Init();

        roots = new float[4];
        complexRoots = Stream.generate(idComplex::new).limit(4).toArray(idComplex[]::new);
    }

    @Test
    public void Test1() {

        p = new idPolynomial(-5.0f, 4.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            Assert.assertTrue(idMath.Fabs(value) < 1e-4f);
        }
    }

    @Test
    public void Test2() {
        p = new idPolynomial(-5.0f, 4.0f, 3.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            Assert.assertTrue(idMath.Fabs(value) < 1e-4f);
        }
    }

    @Test
    public void Test3() {
        p = new idPolynomial(1.0f, 4.0f, 3.0f, -2.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            Assert.assertTrue(idMath.Fabs(value) < 1e-4f);
        }
    }

    @Test
    public void Test4() {
        p = new idPolynomial(5.0f, 4.0f, 3.0f, -2.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            Assert.assertTrue(idMath.Fabs(value) < 1e-4f);
        }
    }

    @Test
    public void Test5() {
        p = new idPolynomial(-5.0f, 4.0f, 3.0f, 2.0f, 1.0f);
        num = p.GetRoots(roots);
        for (i = 0; i < num; i++) {
            value = p.GetValue(roots[i]);
            Assert.assertTrue(idMath.Fabs(value) < 1e-4f);
        }
    }

    @Test
    public void Test6() {
        p = new idPolynomial(1.0f, 4.0f, 3.0f, -2.0f);
        num = p.GetRoots(complexRoots);
        for (i = 0; i < num; i++) {
            complexValue = p.GetValue(complexRoots[i]);
            Assert.assertTrue(idMath.Fabs(complexValue.r) < 1e-4f && idMath.Fabs(complexValue.i) < 1e-4f);
        }
    }

    @Test
    public void Test7() {
        p = new idPolynomial(5.0f, 4.0f, 3.0f, -2.0f);
        num = p.GetRoots(complexRoots);
        for (i = 0; i < num; i++) {
            complexValue = p.GetValue(complexRoots[i]);
            Assert.assertTrue(idMath.Fabs(complexValue.r) < 1e-4f && idMath.Fabs(complexValue.i) < 1e-4f);
        }
    }
}
