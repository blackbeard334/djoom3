package neo.idlib.math.Matrix;

import neo.idlib.Lib;
import neo.idlib.math.Vector;

public class idMatXTest {

    public void Test() {
        idMatX original = new idMatX(), m1 = new idMatX(), m2 = new idMatX(), m3 = new idMatX();
        idMatX q1 = new idMatX(), q2 = new idMatX(), r1 = new idMatX(), r2 = new idMatX();
        Vector.idVecX v = new Vector.idVecX(), w = new Vector.idVecX(), u = new Vector.idVecX(), c = new Vector.idVecX(), d = new Vector.idVecX();
        int offset, size;
        int[] index1, index2;

        size = 6;
        original.Random(size, size, 0);
        original = original.oMultiply(original.Transpose());

        index1 = new int[size + 1];
        index2 = new int[size + 1];

        /*
         idMatX::LowerTriangularInverse
         */
        m1.oSet(original);
        m1.ClearUpperTriangle();
        m2.oSet(m1);

        m2.InverseSelf();
        m1.LowerTriangularInverse();

        if (!m1.Compare(m2, 1.e-4f)) {
            Lib.idLib.common.Warning("idMatX::LowerTriangularInverse failed");
        }

        /*
         idMatX::UpperTriangularInverse
         */
        m1.oSet(original);
        m1.ClearLowerTriangle();
        m2.oSet(m1);

        m2.InverseSelf();
        m1.UpperTriangularInverse();

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::UpperTriangularInverse failed");
        }

        /*
         idMatX::Inverse_GaussJordan
         */
        m1.oSet(original);

        m1.Inverse_GaussJordan();
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Inverse_GaussJordan failed");
        }

        /*
         idMatX::Inverse_UpdateRankOne
         */
        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 1);
        v.Random(size, 2);

        // invert m1
        m1.Inverse_GaussJordan();

        // modify and invert m2 
        m2.Update_RankOne(v, w, 1.0f);
        if (!m2.Inverse_GaussJordan()) {
            assert (false);
        }

        // update inverse of m1
        m1.Inverse_UpdateRankOne(v, w, 1.0f);

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Inverse_UpdateRankOne failed");
        }

        /*
         idMatX::Inverse_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            v.Random(size, 1);
            w.Random(size, 2);
            w.p[offset] = 0.0f;

            // invert m1
            m1.Inverse_GaussJordan();

            // modify and invert m2
            m2.Update_RowColumn(v, w, offset);
            if (!m2.Inverse_GaussJordan()) {
                assert (false);
            }

            // update inverse of m1
            m1.Inverse_UpdateRowColumn(v, w, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::Inverse_UpdateRowColumn failed");
            }
        }

        /*
         idMatX::Inverse_UpdateIncrement
         */
        m1.oSet(original);
        m2.oSet(original);

        v.Random(size + 1, 1);
        w.Random(size + 1, 2);
        w.p[size] = 0.0f;

        // invert m1
        m1.Inverse_GaussJordan();

        // modify and invert m2 
        m2.Update_Increment(v, w);
        if (!m2.Inverse_GaussJordan()) {
            assert (false);
        }

        // update inverse of m1
        m1.Inverse_UpdateIncrement(v, w);

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Inverse_UpdateIncrement failed");
        }

        /*
         idMatX::Inverse_UpdateDecrement
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            w.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
                w.p[i] = original.oGet(offset, i);
            }

            // invert m1
            m1.Inverse_GaussJordan();

            // modify and invert m2
            m2.Update_Decrement(offset);
            if (!m2.Inverse_GaussJordan()) {
                assert (false);
            }

            // update inverse of m1
            m1.Inverse_UpdateDecrement(v, w, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::Inverse_UpdateDecrement failed");

            }
        }

        /*
         idMatX::LU_Factor
         */
        m1.oSet(original);

        m1.LU_Factor(null);	// no pivoting
        m1.LU_UnpackFactors(m2, m3);
        m1.oSet(m2.oMultiply(m3));

        if (!original.Compare(m1, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LU_Factor failed");
        }

        /*
         idMatX::LU_UpdateRankOne
         */
        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 1);
        v.Random(size, 2);

        // factor m1
        m1.LU_Factor(index1);

        // modify and factor m2 
        m2.Update_RankOne(v, w, 1.0f);
        if (!m2.LU_Factor(index2)) {
            assert (false);
        }
        m2.LU_MultiplyFactors(m3, index2);
        m2.oSet(m3);

        // update factored m1
        m1.LU_UpdateRankOne(v, w, 1.0f, index1);
        m1.LU_MultiplyFactors(m3, index1);
        m1.oSet(m3);

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LU_UpdateRankOne failed");

        }

        /*
         idMatX::LU_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            v.Random(size, 1);
            w.Random(size, 2);
            w.p[offset] = 0.0f;

            // factor m1
            m1.LU_Factor(index1);

            // modify and factor m2
            m2.Update_RowColumn(v, w, offset);
            if (!m2.LU_Factor(index2)) {
                assert (false);
            }
            m2.LU_MultiplyFactors(m3, index2);
            m2.oSet(m3);

            // update m1
            m1.LU_UpdateRowColumn(v, w, offset, index1);
            m1.LU_MultiplyFactors(m3, index1);
            m1.oSet(m3);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::LU_UpdateRowColumn failed");
            }
        }

        /*
         idMatX::LU_UpdateIncrement
         */
        m1.oSet(original);
        m2.oSet(original);

        v.Random(size + 1, 1);
        w.Random(size + 1, 2);
        w.p[size] = 0.0f;

        // factor m1
        m1.LU_Factor(index1);

        // modify and factor m2 
        m2.Update_Increment(v, w);
        if (!m2.LU_Factor(index2)) {
            assert (false);
        }
        m2.LU_MultiplyFactors(m3, index2);
        m2.oSet(m3);

        // update factored m1
        m1.LU_UpdateIncrement(v, w, index1);
        m1.LU_MultiplyFactors(m3, index1);
        m1.oSet(m3);

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LU_UpdateIncrement failed");
        }

        /*
         idMatX::LU_UpdateDecrement
         */
        for (offset = 0; offset < size; offset++) {
            m1 = new idMatX();//TODO:check m1=m3, m2=m2 refs!!!
            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            w.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
                w.p[i] = original.oGet(offset, i);
            }

            // factor m1
            m1.LU_Factor(index1);

            // modify and factor m2
            m2.Update_Decrement(offset);
            if (!m2.LU_Factor(index2)) {
                assert (false);
            }
            m2.LU_MultiplyFactors(m3, index2);
            m2.oSet(m3);

            u.SetSize(6);
            for (int i = 0; i < size; i++) {
                u.p[i] = original.oGet(index1[offset], i);
            }

            // update factors of m1
            m1.LU_UpdateDecrement(v, w, u, offset, index1);
            m1.LU_MultiplyFactors(m3, index1);
            m1.oSet(m3);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::LU_UpdateDecrement failed");
            }
        }

        /*
         idMatX::LU_Inverse
         */
        m2.oSet(original);

        m2.LU_Factor(null);
        m2.LU_Inverse(m1, null);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LU_Inverse failed");
            //System.exit(9);
        }

        /*
         idMatX::QR_Factor
         */
        c.SetSize(size);
        d.SetSize(size);

        m1.oSet(original);

        m1.QR_Factor(c, d);
        m1.QR_UnpackFactors(q1, r1, c, d);
        m1.oSet(q1.oMultiply(r1));

        if (!original.Compare(m1, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::QR_Factor failed");
        }

        /*
         idMatX::QR_UpdateRankOne
         */
        c.SetSize(size);
        d.SetSize(size);

        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 0);
        v.oSet(w);

        // factor m1
        m1.QR_Factor(c, d);
        m1.QR_UnpackFactors(q1, r1, c, d);

        // modify and factor m2 
        m2.Update_RankOne(v, w, 1.0f);
        if (!m2.QR_Factor(c, d)) {
            assert (false);
        }
        m2.QR_UnpackFactors(q2, r2, c, d);
        m2 = q2.oMultiply(r2);

        // update factored m1
        q1.QR_UpdateRankOne(r1, v, w, 1.0f);
        m1 = q1.oMultiply(r1);

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::QR_UpdateRankOne failed");
        }

        /*
         idMatX::QR_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            c.SetSize(size);
            d.SetSize(size);

            m1.oSet(original);
            m2.oSet(original);

            v.Random(size, 1);
            w.Random(size, 2);
            w.p[offset] = 0.0f;

            // factor m1
            m1.QR_Factor(c, d);
            m1.QR_UnpackFactors(q1, r1, c, d);

            // modify and factor m2
            m2.Update_RowColumn(v, w, offset);
            if (!m2.QR_Factor(c, d)) {
                assert (false);
            }
            m2.QR_UnpackFactors(q2, r2, c, d);
            m2 = q2.oMultiply(r2);

            // update m1
            q1.QR_UpdateRowColumn(r1, v, w, offset);
            m1 = q1.oMultiply(r1);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::QR_UpdateRowColumn failed");

            }
        }

        /*
         idMatX::QR_UpdateIncrement
         */
        c.SetSize(size + 1);
        d.SetSize(size + 1);

        m1.oSet(original);
        m2.oSet(original);

        v.Random(size + 1, 1);
        w.Random(size + 1, 2);
        w.p[size] = 0.0f;

        // factor m1
        m1.QR_Factor(c, d);
        m1.QR_UnpackFactors(q1, r1, c, d);

        // modify and factor m2 
        m2.Update_Increment(v, w);
        if (!m2.QR_Factor(c, d)) {
            assert (false);
        }
        m2.QR_UnpackFactors(q2, r2, c, d);
        m2 = q2.oMultiply(r2);

        // update factored m1
        q1.QR_UpdateIncrement(r1, v, w);
        m1 = q1.oMultiply(r1);

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::QR_UpdateIncrement failed");
        }

        /*
         idMatX::QR_UpdateDecrement
         */
        for (offset = 0; offset < size; offset++) {
            c.SetSize(size + 1);
            d.SetSize(size + 1);

            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            w.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
                w.p[i] = original.oGet(offset, i);
            }

            // factor m1
            m1.QR_Factor(c, d);
            m1.QR_UnpackFactors(q1, r1, c, d);

            // modify and factor m2
            m2.Update_Decrement(offset);
            if (!m2.QR_Factor(c, d)) {
                assert (false);
            }
            m2.QR_UnpackFactors(q2, r2, c, d);
            m2 = q2.oMultiply(r2);

            // update factors of m1
            q1.QR_UpdateDecrement(r1, v, w, offset);
            m1.oSet(q1.oMultiply(r1));

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::QR_UpdateDecrement failed");
            }
        }

        /*
         idMatX::QR_Inverse
         */
        m2.oSet(original);

        m2.QR_Factor(c, d);
        m2.QR_Inverse(m1, c, d);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            Lib.idLib.common.Warning("idMatX::QR_Inverse failed");
        }

        /*
         idMatX::SVD_Factor
         */
        m1.oSet(original);
        m3.Zero(size, size);
        w.Zero(size);

        m1.SVD_Factor(w, m3);
        m2.Diag(w);
        m3.TransposeSelf();
        m1.oSet(m1.oMultiply(m2).oMultiply(m3));

        if (!original.Compare(m1, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::SVD_Factor failed");
        }

        /*
         idMatX::SVD_Inverse
         */
        m2.oSet(original);

        m2.SVD_Factor(w, m3);
        m2.SVD_Inverse(m1, w, m3);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            Lib.idLib.common.Warning("idMatX::SVD_Inverse failed");
        }

        /*
         idMatX::Cholesky_Factor
         */
        m1.oSet(original);

        m1.Cholesky_Factor();
        m1.Cholesky_MultiplyFactors(m2);

        if (!original.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Cholesky_Factor failed");
        }

        /*
         idMatX::Cholesky_UpdateRankOne
         */
        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 0);

        // factor m1
        m1.Cholesky_Factor();
        m1.ClearUpperTriangle();

        // modify and factor m2 
        m2.Update_RankOneSymmetric(w, 1.0f);
        if (!m2.Cholesky_Factor()) {
            assert (false);
        }
        m2.ClearUpperTriangle();

        // update factored m1
        m1.Cholesky_UpdateRankOne(w, 1.0f, 0);

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Cholesky_UpdateRankOne failed");
        }

        /*
         idMatX::Cholesky_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            // factor m1
            m1.Cholesky_Factor();
            m1.ClearUpperTriangle();

            int pdtable[] = {1, 0, 1, 0, 0, 0};
            w.Random(size, pdtable[offset]);
            w.oMulSet(0.1f);

            // modify and factor m2
            m2.Update_RowColumnSymmetric(w, offset);
            if (!m2.Cholesky_Factor()) {
                assert (false);
            }
            m2.ClearUpperTriangle();

            // update m1
            m1.Cholesky_UpdateRowColumn(w, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::Cholesky_UpdateRowColumn failed");
            }
        }

        /*
         idMatX::Cholesky_UpdateIncrement
         */
        m1.Random(size + 1, size + 1, 0);
        m3.oSet(m1.oMultiply(m1.Transpose()));

        m1.SquareSubMatrix(m3, size);
        m2.oSet(m1);

        w.SetSize(size + 1);
        for (int i = 0; i < size + 1; i++) {
            w.p[i] = m3.oGet(size, i);
        }

        // factor m1
        m1.Cholesky_Factor();

        // modify and factor m2 
        m2.Update_IncrementSymmetric(w);
        if (!m2.Cholesky_Factor()) {
            assert (false);
        }

        // update factored m1
        m1.Cholesky_UpdateIncrement(w);

        m1.ClearUpperTriangle();
        m2.ClearUpperTriangle();

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Cholesky_UpdateIncrement failed");
        }

        /*
         idMatX::Cholesky_UpdateDecrement
         */
        for (offset = 0; offset < size; offset += size - 1) {
            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
            }

            // factor m1
            m1.Cholesky_Factor();

            // modify and factor m2
            m2.Update_Decrement(offset);
            if (!m2.Cholesky_Factor()) {
                assert (false);
            }

            // update factors of m1
            m1.Cholesky_UpdateDecrement(v, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::Cholesky_UpdateDecrement failed");
            }
        }

        /*
         idMatX::Cholesky_Inverse
         */
        m2.oSet(original);

        m2.Cholesky_Factor();
        m2.Cholesky_Inverse(m1);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Cholesky_Inverse failed");
        }

        /*
         idMatX::LDLT_Factor
         */
        m1.oSet(original);

        m1.LDLT_Factor();
        m1.LDLT_MultiplyFactors(m2);

        if (!original.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LDLT_Factor failed");
        }

        m1.LDLT_UnpackFactors(m2, m3);
        m2 = m2.oMultiply(m3).oMultiply(m2.Transpose());

        if (!original.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LDLT_Factor failed");
        }

        /*
         idMatX::LDLT_UpdateRankOne
         */
        m1.oSet(original);
        m2.oSet(original);

        w.Random(size, 0);

        // factor m1
        m1.LDLT_Factor();
        m1.ClearUpperTriangle();

        // modify and factor m2 
        m2.Update_RankOneSymmetric(w, 1.0f);
        if (!m2.LDLT_Factor()) {
            assert (false);
        }
        m2.ClearUpperTriangle();

        // update factored m1
        m1.LDLT_UpdateRankOne(w, 1.0f, 0);

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LDLT_UpdateRankOne failed");
        }

        /*
         idMatX::LDLT_UpdateRowColumn
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            w.Random(size, 0);

            // factor m1
            m1.LDLT_Factor();
            m1.ClearUpperTriangle();

            // modify and factor m2
            m2.Update_RowColumnSymmetric(w, offset);
            if (!m2.LDLT_Factor()) {
                assert (false);
            }
            m2.ClearUpperTriangle();

            // update m1
            m1.LDLT_UpdateRowColumn(w, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::LDLT_UpdateRowColumn failed");
            }
        }

        /*
         idMatX::LDLT_UpdateIncrement
         */
        m1.Random(size + 1, size + 1, 0);
        m3 = m1.oMultiply(m1.Transpose());

        m1.SquareSubMatrix(m3, size);
        m2.oSet(m1);

        w.SetSize(size + 1);
        for (int i = 0; i < size + 1; i++) {
            w.p[i] = m3.oGet(size, i);
        }

        // factor m1
        m1.LDLT_Factor();

        // modify and factor m2 
        m2.Update_IncrementSymmetric(w);
        if (!m2.LDLT_Factor()) {
            assert (false);
        }

        // update factored m1
        m1.LDLT_UpdateIncrement(w);

        m1.ClearUpperTriangle();
        m2.ClearUpperTriangle();

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LDLT_UpdateIncrement failed");
        }

        /*
         idMatX::LDLT_UpdateDecrement
         */
        for (offset = 0; offset < size; offset++) {
            m1.oSet(original);
            m2.oSet(original);

            v.SetSize(6);
            for (int i = 0; i < size; i++) {
                v.p[i] = original.oGet(i, offset);
            }

            // factor m1
            m1.LDLT_Factor();

            // modify and factor m2
            m2.Update_Decrement(offset);
            if (!m2.LDLT_Factor()) {
                assert (false);
            }

            // update factors of m1
            m1.LDLT_UpdateDecrement(v, offset);

            if (!m1.Compare(m2, 1e-3f)) {
                Lib.idLib.common.Warning("idMatX::LDLT_UpdateDecrement failed");
            }
        }

        /*
         idMatX::LDLT_Inverse
         */
        m2.oSet(original);

        m2.LDLT_Factor();
        m2.LDLT_Inverse(m1);
        m1.oMulSet(original);

        if (!m1.IsIdentity(1e-4f)) {
            Lib.idLib.common.Warning("idMatX::LDLT_Inverse failed");
        }

        /*
         idMatX::Eigen_SolveSymmetricTriDiagonal
         */
        m3.oSet(original);
        m3.TriDiagonal_ClearTriangles();
        m1.oSet(m3);

        v.SetSize(size);

        m1.Eigen_SolveSymmetricTriDiagonal(v);

        m3.TransposeMultiply(m2, m1);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m1.oMulSet(i, j, v.p[j]);
            }
        }

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Eigen_SolveSymmetricTriDiagonal failed");
        }

        /*
         idMatX::Eigen_SolveSymmetric
         */
        m3.oSet(original);
        m1.oSet(m3);

        v.SetSize(size);

        m1.Eigen_SolveSymmetric(v);

        m3.TransposeMultiply(m2, m1);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m1.oMulSet(i, j, v.p[j]);
            }
        }

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Eigen_SolveSymmetric failed");
        }

        /*
         idMatX::Eigen_Solve
         */
        m3.oSet(original);
        m1.oSet(m3);

        v.SetSize(size);
        w.SetSize(size);

        m1.Eigen_Solve(v, w);

        m3.TransposeMultiply(m2, m1);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                m1.oMulSet(i, j, v.p[j]);
            }
        }

        if (!m1.Compare(m2, 1e-4f)) {
            Lib.idLib.common.Warning("idMatX::Eigen_Solve failed");
        }
    }

}
