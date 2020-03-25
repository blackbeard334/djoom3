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

        this.roots = new float[4];
        this.complexRoots = Stream.generate(idComplex::new).limit(4).toArray(idComplex[]::new);
    }

    @Test
    public void Test1() {

        this.p = new idPolynomial(-5.0f, 4.0f);
        this.num = this.p.GetRoots(this.roots);
        for (this.i = 0; this.i < this.num; this.i++) {
            this.value = this.p.GetValue(this.roots[this.i]);
            Assert.assertTrue(idMath.Fabs(this.value) < 1e-4f);
        }
    }

    @Test
    public void Test2() {
        this.p = new idPolynomial(-5.0f, 4.0f, 3.0f);
        this.num = this.p.GetRoots(this.roots);
        for (this.i = 0; this.i < this.num; this.i++) {
            this.value = this.p.GetValue(this.roots[this.i]);
            Assert.assertTrue(idMath.Fabs(this.value) < 1e-4f);
        }
    }

    @Test
    public void Test3() {
        this.p = new idPolynomial(1.0f, 4.0f, 3.0f, -2.0f);
        this.num = this.p.GetRoots(this.roots);
        for (this.i = 0; this.i < this.num; this.i++) {
            this.value = this.p.GetValue(this.roots[this.i]);
            Assert.assertTrue(idMath.Fabs(this.value) < 1e-4f);
        }
    }

    @Test
    public void Test4() {
        this.p = new idPolynomial(5.0f, 4.0f, 3.0f, -2.0f);
        this.num = this.p.GetRoots(this.roots);
        for (this.i = 0; this.i < this.num; this.i++) {
            this.value = this.p.GetValue(this.roots[this.i]);
            Assert.assertTrue(idMath.Fabs(this.value) < 1e-4f);
        }
    }

    @Test
    public void Test5() {
        this.p = new idPolynomial(-5.0f, 4.0f, 3.0f, 2.0f, 1.0f);
        this.num = this.p.GetRoots(this.roots);
        for (this.i = 0; this.i < this.num; this.i++) {
            this.value = this.p.GetValue(this.roots[this.i]);
            Assert.assertTrue(idMath.Fabs(this.value) < 1e-4f);
        }
    }

    @Test
    public void Test6() {
        this.p = new idPolynomial(1.0f, 4.0f, 3.0f, -2.0f);
        this.num = this.p.GetRoots(this.complexRoots);
        for (this.i = 0; this.i < this.num; this.i++) {
            this.complexValue = this.p.GetValue(this.complexRoots[this.i]);
            Assert.assertTrue((idMath.Fabs(this.complexValue.r) < 1e-4f) && (idMath.Fabs(this.complexValue.i) < 1e-4f));
        }
    }

    @Test
    public void Test7() {
        this.p = new idPolynomial(5.0f, 4.0f, 3.0f, -2.0f);
        this.num = this.p.GetRoots(this.complexRoots);
        for (this.i = 0; this.i < this.num; this.i++) {
            this.complexValue = this.p.GetValue(this.complexRoots[this.i]);
            Assert.assertTrue((idMath.Fabs(this.complexValue.r) < 1e-4f) && (idMath.Fabs(this.complexValue.i) < 1e-4f));
        }
    }
}
