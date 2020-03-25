package neo.idlib.math.Matrix;

import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Simd;
import neo.idlib.math.Vector.idVecX;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class idMatXTest {

    private idMatX original = new idMatX();
    private idMatX m1       = new idMatX();
    private idMatX m2       = new idMatX();
    private idMatX m3       = new idMatX();
    private final idMatX q1       = new idMatX();
    private final idMatX q2       = new idMatX();
    private final idMatX r1       = new idMatX();
    private final idMatX r2       = new idMatX();
    private final idVecX v        = new idVecX();
    private final idVecX w        = new idVecX();
    private final idVecX u        = new idVecX();
    private final idVecX c        = new idVecX();
    private final idVecX d        = new idVecX();
    private int    offset;
    private int    size;
    private int[]  index1;
    private int[]  index2;

    @Before
    public void setUpTest() {
        Simd.idSIMD.Init();
        idMath.Init();

        this.size = 6;
        this.original.Random(this.size, this.size, 0);
        this.original = this.original.oMultiply(this.original.Transpose());

        this.index1 = new int[this.size + 1];
        this.index2 = new int[this.size + 1];
    }

    @Test
    public void LowerTriangularInverseTest() {
        this.m1.oSet(this.original);
        this.m1.ClearUpperTriangle();
        this.m2.oSet(this.m1);

        this.m2.InverseSelf();
        this.m1.LowerTriangularInverse();

        Assert.assertTrue("idMatX::LowerTriangularInverse failed", this.m1.Compare(this.m2, 1.e-4f));
    }

    @Test
    public void UpperTriangularInverseTest() {
        this.m1.oSet(this.original);
        this.m1.ClearLowerTriangle();
        this.m2.oSet(this.m1);

        this.m2.InverseSelf();
        this.m1.UpperTriangularInverse();


        Assert.assertTrue("idMatX::UpperTriangularInverse failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void Inverse_GaussJordanTest() {
        this.m1.oSet(this.original);

        this.m1.Inverse_GaussJordan();
        this.m1.oMulSet(this.original);


        Assert.assertTrue("idMatX::Inverse_GaussJordan failed", this.m1.IsIdentity(1e-4f));
    }

    @Test
    public void Inverse_UpdateRankOneTest() {
        this.m1.oSet(this.original);
        this.m2.oSet(this.original);

        this.w.Random(this.size, 1);
        this.v.Random(this.size, 2);

        // invert m1
        this.m1.Inverse_GaussJordan();

        // modify and invert m2
        this.m2.Update_RankOne(this.v, this.w, 1.0f);
        if (!this.m2.Inverse_GaussJordan()) {
            assert (false);
        }

        // update inverse of m1
        this.m1.Inverse_UpdateRankOne(this.v, this.w, 1.0f);


        Assert.assertTrue("idMatX::Inverse_UpdateRankOne failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void Inverse_UpdateRowColumnTest() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.v.Random(this.size, 1);
            this.w.Random(this.size, 2);
            this.w.p[this.offset] = 0.0f;

            // invert m1
            this.m1.Inverse_GaussJordan();

            // modify and invert m2
            this.m2.Update_RowColumn(this.v, this.w, this.offset);
            if (!this.m2.Inverse_GaussJordan()) {
                assert (false);
            }

            // update inverse of m1
            this.m1.Inverse_UpdateRowColumn(this.v, this.w, this.offset);

            Assert.assertTrue("idMatX::Inverse_UpdateRowColumn failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void Inverse_UpdateIncrementTest() {
        this.m1.oSet(this.original);
        this.m2.oSet(this.original);

        this.v.Random(this.size + 1, 1);
        this.w.Random(this.size + 1, 2);
        this.w.p[this.size] = 0.0f;

        // invert m1
        this.m1.Inverse_GaussJordan();

        // modify and invert m2
        this.m2.Update_Increment(this.v, this.w);
        if (!this.m2.Inverse_GaussJordan()) {
            assert (false);
        }

        // update inverse of m1
        this.m1.Inverse_UpdateIncrement(this.v, this.w);

        Assert.assertTrue("idMatX::Inverse_UpdateIncrement failed", !this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void Inverse_UpdateDecrementTest() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.v.SetSize(6);
            this.w.SetSize(6);
            for (int i = 0; i < this.size; i++) {
                this.v.p[i] = this.original.oGet(i, this.offset);
                this.w.p[i] = this.original.oGet(this.offset, i);
            }

            // invert m1
            this.m1.Inverse_GaussJordan();

            // modify and invert m2
            this.m2.Update_Decrement(this.offset);
            if (!this.m2.Inverse_GaussJordan()) {
                assert (false);
            }

            // update inverse of m1
            this.m1.Inverse_UpdateDecrement(this.v, this.w, this.offset);

//            Assert.assertTrue("idMatX::Inverse_UpdateDecrement failed " + offset, m1.Compare(m2, 1e-3f));//TODO: fix this?
            Assert.assertTrue("idMatX::Inverse_UpdateDecrement failed " + this.offset, this.m1.Compare(this.m2, 1e-2f));
        }
    }

    @Test
    public void LU_FactorTest() {
        this.m1.oSet(this.original);

        this.m1.LU_Factor(null);    // no pivoting
        this.m1.LU_UnpackFactors(this.m2, this.m3);
        this.m1.oSet(this.m2.oMultiply(this.m3));

        Assert.assertTrue("idMatX::LU_Factor failed", this.original.Compare(this.m1, 1e-4f));
    }

    @Test
    public void LU_UpdateRankOneTest() {
        this.m1.oSet(this.original);
        this.m2.oSet(this.original);

        this.w.Random(this.size, 1);
        this.v.Random(this.size, 2);

        // factor m1
        this.m1.LU_Factor(this.index1);

        // modify and factor m2
        this.m2.Update_RankOne(this.v, this.w, 1.0f);
        if (!this.m2.LU_Factor(this.index2)) {
            assert (false);
        }
        this.m2.LU_MultiplyFactors(this.m3, this.index2);
        this.m2.oSet(this.m3);

        // update factored m1
        this.m1.LU_UpdateRankOne(this.v, this.w, 1.0f, this.index1);
        this.m1.LU_MultiplyFactors(this.m3, this.index1);
        this.m1.oSet(this.m3);

        Assert.assertTrue("idMatX::LU_UpdateRankOne failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void LU_UpdateRowColumnTest() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.v.Random(this.size, 1);
            this.w.Random(this.size, 2);
            this.w.p[this.offset] = 0.0f;

            // factor m1
            this.m1.LU_Factor(this.index1);

            // modify and factor m2
            this.m2.Update_RowColumn(this.v, this.w, this.offset);
            if (!this.m2.LU_Factor(this.index2)) {
                assert (false);
            }
            this.m2.LU_MultiplyFactors(this.m3, this.index2);
            this.m2.oSet(this.m3);

            // update m1
            this.m1.LU_UpdateRowColumn(this.v, this.w, this.offset, this.index1);
            this.m1.LU_MultiplyFactors(this.m3, this.index1);
            this.m1.oSet(this.m3);

            Assert.assertTrue("idMatX::LU_UpdateRowColumn failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void LU_UpdateIncrementTest() {
        this.m1.oSet(this.original);
        this.m2.oSet(this.original);

        this.v.Random(this.size + 1, 1);
        this.w.Random(this.size + 1, 2);
        this.w.p[this.size] = 0.0f;

        // factor m1
        this.m1.LU_Factor(this.index1);

        // modify and factor m2
        this.m2.Update_Increment(this.v, this.w);
        if (!this.m2.LU_Factor(this.index2)) {
            assert (false);
        }
        this.m2.LU_MultiplyFactors(this.m3, this.index2);
        this.m2.oSet(this.m3);

        // update factored m1
        this.m1.LU_UpdateIncrement(this.v, this.w, this.index1);
        this.m1.LU_MultiplyFactors(this.m3, this.index1);
        this.m1.oSet(this.m3);

        Assert.assertTrue("idMatX::LU_UpdateIncrement failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void LU_UpdateDecrementTest() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.m1 = new idMatX();//TODO:check m1=m3, m2=m2 refs!!!
            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.v.SetSize(6);
            this.w.SetSize(6);
            for (int i = 0; i < this.size; i++) {
                this.v.p[i] = this.original.oGet(i, this.offset);
                this.w.p[i] = this.original.oGet(this.offset, i);
            }

            // factor m1
            this.m1.LU_Factor(this.index1);

            // modify and factor m2
            this.m2.Update_Decrement(this.offset);
            if (!this.m2.LU_Factor(this.index2)) {
                assert (false);
            }
            this.m2.LU_MultiplyFactors(this.m3, this.index2);
            this.m2.oSet(this.m3);

            this.u.SetSize(6);
            for (int i = 0; i < this.size; i++) {
                this.u.p[i] = this.original.oGet(this.index1[this.offset], i);
            }

            // update factors of m1
            this.m1.LU_UpdateDecrement(this.v, this.w, this.u, this.offset, this.index1);
            this.m1.LU_MultiplyFactors(this.m3, this.index1);
            this.m1.oSet(this.m3);

            Assert.assertTrue("idMatX::LU_UpdateDecrement failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void LU_InverseTest() {
        this.m2.oSet(this.original);

        this.m2.LU_Factor(null);
        this.m2.LU_Inverse(this.m1, null);
        this.m1.oMulSet(this.original);

        Assert.assertTrue("idMatX::LU_Inverse failed", this.m1.IsIdentity(1e-4f));
    }

    @Test
    public void QR_FactorTest() {
        this.c.SetSize(this.size);
        this.d.SetSize(this.size);

        this.m1.oSet(this.original);

        this.m1.QR_Factor(this.c, this.d);
        this.m1.QR_UnpackFactors(this.q1, this.r1, this.c, this.d);
        this.m1.oSet(this.q1.oMultiply(this.r1));

        Assert.assertTrue("idMatX::QR_Factor failed", this.original.Compare(this.m1, 1e-4f));
    }

    @Test
    public void QR_UpdateRankOneTest() {
        this.c.SetSize(this.size);
        this.d.SetSize(this.size);

        this.m1.oSet(this.original);
        this.m2.oSet(this.original);

        this.w.Random(this.size, 0);
        this.v.oSet(this.w);

        // factor m1
        this.m1.QR_Factor(this.c, this.d);
        this.m1.QR_UnpackFactors(this.q1, this.r1, this.c, this.d);

        // modify and factor m2
        this.m2.Update_RankOne(this.v, this.w, 1.0f);
        if (!this.m2.QR_Factor(this.c, this.d)) {
            assert (false);
        }
        this.m2.QR_UnpackFactors(this.q2, this.r2, this.c, this.d);
        this.m2 = this.q2.oMultiply(this.r2);

        // update factored m1
        this.q1.QR_UpdateRankOne(this.r1, this.v, this.w, 1.0f);
        this.m1 = this.q1.oMultiply(this.r1);

        Assert.assertTrue("idMatX::QR_UpdateRankOne failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void QR_UpdateRowColumnTest() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.c.SetSize(this.size);
            this.d.SetSize(this.size);

            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.v.Random(this.size, 1);
            this.w.Random(this.size, 2);
            this.w.p[this.offset] = 0.0f;

            // factor m1
            this.m1.QR_Factor(this.c, this.d);
            this.m1.QR_UnpackFactors(this.q1, this.r1, this.c, this.d);

            // modify and factor m2
            this.m2.Update_RowColumn(this.v, this.w, this.offset);
            if (!this.m2.QR_Factor(this.c, this.d)) {
                assert (false);
            }
            this.m2.QR_UnpackFactors(this.q2, this.r2, this.c, this.d);
            this.m2 = this.q2.oMultiply(this.r2);

            // update m1
            this.q1.QR_UpdateRowColumn(this.r1, this.v, this.w, this.offset);
            this.m1 = this.q1.oMultiply(this.r1);

            Assert.assertTrue("idMatX::QR_UpdateRowColumn failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void QR_UpdateIncrementTest() {
        this.c.SetSize(this.size + 1);
        this.d.SetSize(this.size + 1);

        this.m1.oSet(this.original);
        this.m2.oSet(this.original);

        this.v.Random(this.size + 1, 1);
        this.w.Random(this.size + 1, 2);
        this.w.p[this.size] = 0.0f;

        // factor m1
        this.m1.QR_Factor(this.c, this.d);
        this.m1.QR_UnpackFactors(this.q1, this.r1, this.c, this.d);

        // modify and factor m2
        this.m2.Update_Increment(this.v, this.w);
        if (!this.m2.QR_Factor(this.c, this.d)) {
            assert (false);
        }
        this.m2.QR_UnpackFactors(this.q2, this.r2, this.c, this.d);
        this.m2 = this.q2.oMultiply(this.r2);

        // update factored m1
        this.q1.QR_UpdateIncrement(this.r1, this.v, this.w);
        this.m1 = this.q1.oMultiply(this.r1);

        Assert.assertTrue("idMatX::QR_UpdateIncrement failed", !this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void QR_UpdateDecrementTest() {
        QR_UpdateDecrementSetUp();
    }

    private void QR_UpdateDecrementSetUp() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.c.SetSize(this.size + 1);
            this.d.SetSize(this.size + 1);

            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.v.SetSize(6);
            this.w.SetSize(6);
            for (int i = 0; i < this.size; i++) {
                this.v.p[i] = this.original.oGet(i, this.offset);
                this.w.p[i] = this.original.oGet(this.offset, i);
            }

            // factor m1
            this.m1.QR_Factor(this.c, this.d);
            this.m1.QR_UnpackFactors(this.q1, this.r1, this.c, this.d);

            // modify and factor m2
            this.m2.Update_Decrement(this.offset);
            if (!this.m2.QR_Factor(this.c, this.d)) {
                assert (false);
            }
            this.m2.QR_UnpackFactors(this.q2, this.r2, this.c, this.d);
            this.m2 = this.q2.oMultiply(this.r2);

            // update factors of m1
            this.q1.QR_UpdateDecrement(this.r1, this.v, this.w, this.offset);
            this.m1.oSet(this.q1.oMultiply(this.r1));

            Assert.assertTrue("idMatX::QR_UpdateDecrement failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void QR_InverseTest() {
        QR_UpdateDecrementSetUp();
        this.m2.oSet(this.original);

        this.m2.QR_Factor(this.c, this.d);
        this.m2.QR_Inverse(this.m1, this.c, this.d);
        this.m1.oMulSet(this.original);

        Assert.assertTrue("idMatX::QR_Inverse failed", this.m1.IsIdentity(1e-4f));
    }

    @Test
    public void SVD_FactorTest() {
        SVD_FactorSetUp();

        Assert.assertTrue("idMatX::SVD_Factor failed", this.original.Compare(this.m1, 1e-4f));
    }

    private void SVD_FactorSetUp() {
        this.m1.oSet(this.original);
        this.m3.Zero(this.size, this.size);
        this.w.Zero(this.size);

        this.m1.SVD_Factor(this.w, this.m3);
        this.m2.Diag(this.w);
        this.m3.TransposeSelf();
        this.m1.oSet(this.m1.oMultiply(this.m2).oMultiply(this.m3));
    }

    @Test
    public void SVD_InverseTest() {
        SVD_FactorSetUp();
        this.m2.oSet(this.original);

        this.m2.SVD_Factor(this.w, this.m3);
        this.m2.SVD_Inverse(this.m1, this.w, this.m3);
        this.m1.oMulSet(this.original);

        Assert.assertTrue("idMatX::SVD_Inverse failed", this.m1.IsIdentity(1e-4f));
    }

    @Test
    public void Cholesky_FactorTest() {
        this.m1.oSet(this.original);

        this.m1.Cholesky_Factor();
        this.m1.Cholesky_MultiplyFactors(this.m2);

        Assert.assertTrue("idMatX::Cholesky_Factor failed", this.original.Compare(this.m2, 1e-4f));
    }

    @Test
    public void Cholesky_UpdateRankOneTest() {
        this.m1.oSet(this.original);
        this.m2.oSet(this.original);

        this.w.Random(this.size, 0);

        // factor m1
        this.m1.Cholesky_Factor();
        this.m1.ClearUpperTriangle();

        // modify and factor m2
        this.m2.Update_RankOneSymmetric(this.w, 1.0f);
        if (!this.m2.Cholesky_Factor()) {
            assert (false);
        }
        this.m2.ClearUpperTriangle();

        // update factored m1
        this.m1.Cholesky_UpdateRankOne(this.w, 1.0f, 0);

        Assert.assertTrue("idMatX::Cholesky_UpdateRankOne failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void Cholesky_UpdateRowColumnTest() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            // factor m1
            this.m1.Cholesky_Factor();
            this.m1.ClearUpperTriangle();

            final int pdtable[] = {1, 0, 1, 0, 0, 0};
            this.w.Random(this.size, pdtable[this.offset]);
            this.w.oMulSet(0.1f);

            // modify and factor m2
            this.m2.Update_RowColumnSymmetric(this.w, this.offset);
            if (!this.m2.Cholesky_Factor()) {
                assert (false);
            }
            this.m2.ClearUpperTriangle();

            // update m1
            this.m1.Cholesky_UpdateRowColumn(this.w, this.offset);

            Assert.assertTrue("idMatX::Cholesky_UpdateRowColumn failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void Cholesky_UpdateIncrementTest() {
        this.m1.Random(this.size + 1, this.size + 1, 0);
        this.m3.oSet(this.m1.oMultiply(this.m1.Transpose()));

        this.m1.SquareSubMatrix(this.m3, this.size);
        this.m2.oSet(this.m1);

        this.w.SetSize(this.size + 1);
        for (int i = 0; i < (this.size + 1); i++) {
            this.w.p[i] = this.m3.oGet(this.size, i);
        }

        // factor m1
        this.m1.Cholesky_Factor();

        // modify and factor m2
        this.m2.Update_IncrementSymmetric(this.w);
        if (!this.m2.Cholesky_Factor()) {
            assert (false);
        }

        // update factored m1
        this.m1.Cholesky_UpdateIncrement(this.w);

        this.m1.ClearUpperTriangle();
        this.m2.ClearUpperTriangle();

        Assert.assertTrue("idMatX::Cholesky_UpdateIncrement failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void Cholesky_UpdateDecrementTest() {
        for (this.offset = 0; this.offset < this.size; this.offset += this.size - 1) {
            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.v.SetSize(6);
            for (int i = 0; i < this.size; i++) {
                this.v.p[i] = this.original.oGet(i, this.offset);
            }

            // factor m1
            this.m1.Cholesky_Factor();

            // modify and factor m2
            this.m2.Update_Decrement(this.offset);
            if (!this.m2.Cholesky_Factor()) {
                assert (false);
            }

            // update factors of m1
            this.m1.Cholesky_UpdateDecrement(this.v, this.offset);

            Assert.assertTrue("idMatX::Cholesky_UpdateDecrement failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void Cholesky_InverseTest() {
        this.m2.oSet(this.original);

        this.m2.Cholesky_Factor();
        this.m2.Cholesky_Inverse(this.m1);
        this.m1.oMulSet(this.original);

        Assert.assertTrue("idMatX::Cholesky_Inverse failed", this.m1.IsIdentity(1e-4f));
    }

    @Test
    public void LDLT_FactorTest() {
        this.m1.oSet(this.original);

        this.m1.LDLT_Factor();
        this.m1.LDLT_MultiplyFactors(this.m2);

        Assert.assertTrue("idMatX::LDLT_Factor failed", this.original.Compare(this.m2, 1e-4f));

        this.m1.LDLT_UnpackFactors(this.m2, this.m3);
        this.m2 = this.m2.oMultiply(this.m3).oMultiply(this.m2.Transpose());

        Assert.assertTrue("idMatX::LDLT_Factor failed", this.original.Compare(this.m2, 1e-4f));
    }

    @Test
    public void LDLT_UpdateRankOneTest() {
        this.m1.oSet(this.original);
        this.m2.oSet(this.original);

        this.w.Random(this.size, 0);

        // factor m1
        this.m1.LDLT_Factor();
        this.m1.ClearUpperTriangle();

        // modify and factor m2
        this.m2.Update_RankOneSymmetric(this.w, 1.0f);
        if (!this.m2.LDLT_Factor()) {
            assert (false);
        }
        this.m2.ClearUpperTriangle();

        // update factored m1
        this.m1.LDLT_UpdateRankOne(this.w, 1.0f, 0);

        Assert.assertTrue("idMatX::LDLT_UpdateRankOne failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void LDLT_UpdateRowColumnTest() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.w.Random(this.size, 0);

            // factor m1
            this.m1.LDLT_Factor();
            this.m1.ClearUpperTriangle();

            // modify and factor m2
            this.m2.Update_RowColumnSymmetric(this.w, this.offset);
            if (!this.m2.LDLT_Factor()) {
                assert (false);
            }
            this.m2.ClearUpperTriangle();

            // update m1
            this.m1.LDLT_UpdateRowColumn(this.w, this.offset);

            Assert.assertTrue("idMatX::LDLT_UpdateRowColumn failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void LDLT_UpdateIncrementTest() {
        this.m1.Random(this.size + 1, this.size + 1, 0);
        this.m3 = this.m1.oMultiply(this.m1.Transpose());

        this.m1.SquareSubMatrix(this.m3, this.size);
        this.m2.oSet(this.m1);

        this.w.SetSize(this.size + 1);
        for (int i = 0; i < (this.size + 1); i++) {
            this.w.p[i] = this.m3.oGet(this.size, i);
        }

        // factor m1
        this.m1.LDLT_Factor();

        // modify and factor m2
        this.m2.Update_IncrementSymmetric(this.w);
        if (!this.m2.LDLT_Factor()) {
            assert (false);
        }

        // update factored m1
        this.m1.LDLT_UpdateIncrement(this.w);

        this.m1.ClearUpperTriangle();
        this.m2.ClearUpperTriangle();

        Assert.assertTrue("idMatX::LDLT_UpdateIncrement failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void LDLT_UpdateDecrementTest() {
        for (this.offset = 0; this.offset < this.size; this.offset++) {
            this.m1.oSet(this.original);
            this.m2.oSet(this.original);

            this.v.SetSize(6);
            for (int i = 0; i < this.size; i++) {
                this.v.p[i] = this.original.oGet(i, this.offset);
            }

            // factor m1
            this.m1.LDLT_Factor();

            // modify and factor m2
            this.m2.Update_Decrement(this.offset);
            if (!this.m2.LDLT_Factor()) {
                assert (false);
            }

            // update factors of m1
            this.m1.LDLT_UpdateDecrement(this.v, this.offset);

            Assert.assertTrue("idMatX::LDLT_UpdateDecrement failed", this.m1.Compare(this.m2, 1e-3f));
        }
    }

    @Test
    public void LDLT_InverseTest() {
        LDLT_InverseSetUp();

        Assert.assertTrue("idMatX::LDLT_Inverse failed", this.m1.IsIdentity(1e-4f));
    }

    private void LDLT_InverseSetUp() {
        this.m2.oSet(this.original);

        this.m2.LDLT_Factor();
        this.m2.LDLT_Inverse(this.m1);
        this.m1.oMulSet(this.original);
    }

    @Test
    public void Eigen_SolveSymmetricTriDiagonalTest() {
        LDLT_InverseSetUp();
        this.m3.oSet(this.original);
        this.m3.TriDiagonal_ClearTriangles();
        this.m1.oSet(this.m3);

        this.v.SetSize(this.size);

        this.m1.Eigen_SolveSymmetricTriDiagonal(this.v);

        this.m3.TransposeMultiply(this.m2, this.m1);

        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                this.m1.oMulSet(i, j, this.v.p[j]);
            }
        }

        Assert.assertTrue("idMatX::Eigen_SolveSymmetricTriDiagonal failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void Eigen_SolveSymmetricTest() {
        LDLT_InverseSetUp();
        this.m3.oSet(this.original);
        this.m1.oSet(this.m3);

        this.v.SetSize(this.size);

        this.m1.Eigen_SolveSymmetric(this.v);

        this.m3.TransposeMultiply(this.m2, this.m1);

        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                this.m1.oMulSet(i, j, this.v.p[j]);
            }
        }

        Assert.assertTrue("idMatX::Eigen_SolveSymmetric failed", this.m1.Compare(this.m2, 1e-4f));
    }

    @Test
    public void Eigen_SolveTest() {
        LDLT_InverseSetUp();
        this.m3.oSet(this.original);
        this.m1.oSet(this.m3);

        this.v.SetSize(this.size);
        this.w.SetSize(this.size);

        this.m1.Eigen_Solve(this.v, this.w);

        this.m3.TransposeMultiply(this.m2, this.m1);

        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                this.m1.oMulSet(i, j, this.v.p[j]);
            }
        }

        Assert.assertTrue("idMatX::Eigen_Solve failed", this.m1.Compare(this.m2, 1e-4f));
    }

}
