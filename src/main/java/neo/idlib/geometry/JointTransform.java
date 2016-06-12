package neo.idlib.geometry;

import java.util.Arrays;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Quat.idQuat;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class JointTransform {

    /*
     ===============================================================================

     Joint Quaternion

     ===============================================================================
     */
    public static class idJointQuat {

        public idQuat q;
        public idVec3 t;

        public idJointQuat() {
            q = new idQuat();
            t = new idVec3();
        }
    };

    /*
     ===============================================================================

     Joint Matrix

     idMat3 m;
     idVec3 t;

     m[0][0], m[1][0], m[2][0], t[0]
     m[0][1], m[1][1], m[2][1], t[1]
     m[0][2], m[1][2], m[2][2], t[2]

     ===============================================================================
     */
    public static class idJointMat {
        public static final int BYTES = 12 * Float.BYTES;

        private final float[] mat = new float[3 * 4];
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        public idJointMat() {
            int a = 0;
        }

        public idJointMat(final float[] mat) {
            System.arraycopy(mat, 0, this.mat, 0, this.mat.length);
        }

        public void SetRotation(final idMat3 m) {
            // NOTE: idMat3 is transposed because it is column-major
            mat[0 * 4 + 0] = m.oGet(0).oGet(0);
            mat[0 * 4 + 1] = m.oGet(1).oGet(0);
            mat[0 * 4 + 2] = m.oGet(2).oGet(0);
            mat[1 * 4 + 0] = m.oGet(0).oGet(1);
            mat[1 * 4 + 1] = m.oGet(1).oGet(1);
            mat[1 * 4 + 2] = m.oGet(2).oGet(1);
            mat[2 * 4 + 0] = m.oGet(0).oGet(2);
            mat[2 * 4 + 1] = m.oGet(1).oGet(2);
            mat[2 * 4 + 2] = m.oGet(2).oGet(2);
        }

        public void SetTranslation(final idVec3 t) {
            mat[0 * 4 + 3] = t.oGet(0);
            mat[1 * 4 + 3] = t.oGet(1);
            mat[2 * 4 + 3] = t.oGet(2);
        }

        // only rotate
        public idVec3 oMultiply(final idVec3 v) {
            return new idVec3(
                    mat[0 * 4 + 0] * v.oGet(0) + mat[0 * 4 + 1] * v.oGet(1) + mat[0 * 4 + 2] * v.oGet(2),
                    mat[1 * 4 + 0] * v.oGet(0) + mat[1 * 4 + 1] * v.oGet(1) + mat[1 * 4 + 2] * v.oGet(2),
                    mat[2 * 4 + 0] * v.oGet(0) + mat[2 * 4 + 1] * v.oGet(1) + mat[2 * 4 + 2] * v.oGet(2));
        }

        // rotate and translate
        public idVec3 oMultiply(final idVec4 v) {
            return new idVec3(
                    mat[0 * 4 + 0] * v.oGet(0) + mat[0 * 4 + 1] * v.oGet(1) + mat[0 * 4 + 2] * v.oGet(2) + mat[0 * 4 + 3] * v.oGet(3),
                    mat[1 * 4 + 0] * v.oGet(0) + mat[1 * 4 + 1] * v.oGet(1) + mat[1 * 4 + 2] * v.oGet(2) + mat[1 * 4 + 3] * v.oGet(3),
                    mat[2 * 4 + 0] * v.oGet(0) + mat[2 * 4 + 1] * v.oGet(1) + mat[2 * 4 + 2] * v.oGet(2) + mat[2 * 4 + 3] * v.oGet(3));
        }

        // transform
        public idJointMat oMulSet(final idJointMat a) {
            float[] dst = new float[3];

            dst[0] = mat[0 * 4 + 0] * a.mat[0 * 4 + 0] + mat[1 * 4 + 0] * a.mat[0 * 4 + 1] + mat[2 * 4 + 0] * a.mat[0 * 4 + 2];
            dst[1] = mat[0 * 4 + 0] * a.mat[1 * 4 + 0] + mat[1 * 4 + 0] * a.mat[1 * 4 + 1] + mat[2 * 4 + 0] * a.mat[1 * 4 + 2];
            dst[2] = mat[0 * 4 + 0] * a.mat[2 * 4 + 0] + mat[1 * 4 + 0] * a.mat[2 * 4 + 1] + mat[2 * 4 + 0] * a.mat[2 * 4 + 2];
            mat[0 * 4 + 0] = dst[0];
            mat[1 * 4 + 0] = dst[1];
            mat[2 * 4 + 0] = dst[2];

            dst[0] = mat[0 * 4 + 1] * a.mat[0 * 4 + 0] + mat[1 * 4 + 1] * a.mat[0 * 4 + 1] + mat[2 * 4 + 1] * a.mat[0 * 4 + 2];
            dst[1] = mat[0 * 4 + 1] * a.mat[1 * 4 + 0] + mat[1 * 4 + 1] * a.mat[1 * 4 + 1] + mat[2 * 4 + 1] * a.mat[1 * 4 + 2];
            dst[2] = mat[0 * 4 + 1] * a.mat[2 * 4 + 0] + mat[1 * 4 + 1] * a.mat[2 * 4 + 1] + mat[2 * 4 + 1] * a.mat[2 * 4 + 2];
            mat[0 * 4 + 1] = dst[0];
            mat[1 * 4 + 1] = dst[1];
            mat[2 * 4 + 1] = dst[2];

            dst[0] = mat[0 * 4 + 2] * a.mat[0 * 4 + 0] + mat[1 * 4 + 2] * a.mat[0 * 4 + 1] + mat[2 * 4 + 2] * a.mat[0 * 4 + 2];
            dst[1] = mat[0 * 4 + 2] * a.mat[1 * 4 + 0] + mat[1 * 4 + 2] * a.mat[1 * 4 + 1] + mat[2 * 4 + 2] * a.mat[1 * 4 + 2];
            dst[2] = mat[0 * 4 + 2] * a.mat[2 * 4 + 0] + mat[1 * 4 + 2] * a.mat[2 * 4 + 1] + mat[2 * 4 + 2] * a.mat[2 * 4 + 2];
            mat[0 * 4 + 2] = dst[0];
            mat[1 * 4 + 2] = dst[1];
            mat[2 * 4 + 2] = dst[2];

            dst[0] = mat[0 * 4 + 3] * a.mat[0 * 4 + 0] + mat[1 * 4 + 3] * a.mat[0 * 4 + 1] + mat[2 * 4 + 3] * a.mat[0 * 4 + 2];
            dst[1] = mat[0 * 4 + 3] * a.mat[1 * 4 + 0] + mat[1 * 4 + 3] * a.mat[1 * 4 + 1] + mat[2 * 4 + 3] * a.mat[1 * 4 + 2];
            dst[2] = mat[0 * 4 + 3] * a.mat[2 * 4 + 0] + mat[1 * 4 + 3] * a.mat[2 * 4 + 1] + mat[2 * 4 + 3] * a.mat[2 * 4 + 2];
            mat[0 * 4 + 3] = dst[0];
            mat[1 * 4 + 3] = dst[1];
            mat[2 * 4 + 3] = dst[2];

            mat[0 * 4 + 3] += a.mat[0 * 4 + 3];
            mat[1 * 4 + 3] += a.mat[1 * 4 + 3];
            mat[2 * 4 + 3] += a.mat[2 * 4 + 3];

            return this;
        }

        // untransform
        public idJointMat oDivSet(final idJointMat a) {
            float[] dst = new float[3];

            mat[0 * 4 + 3] -= a.mat[0 * 4 + 3];
            mat[1 * 4 + 3] -= a.mat[1 * 4 + 3];
            mat[2 * 4 + 3] -= a.mat[2 * 4 + 3];

            dst[0] = mat[0 * 4 + 0] * a.mat[0 * 4 + 0] + mat[1 * 4 + 0] * a.mat[1 * 4 + 0] + mat[2 * 4 + 0] * a.mat[2 * 4 + 0];
            dst[1] = mat[0 * 4 + 0] * a.mat[0 * 4 + 1] + mat[1 * 4 + 0] * a.mat[1 * 4 + 1] + mat[2 * 4 + 0] * a.mat[2 * 4 + 1];
            dst[2] = mat[0 * 4 + 0] * a.mat[0 * 4 + 2] + mat[1 * 4 + 0] * a.mat[1 * 4 + 2] + mat[2 * 4 + 0] * a.mat[2 * 4 + 2];
            mat[0 * 4 + 0] = dst[0];
            mat[1 * 4 + 0] = dst[1];
            mat[2 * 4 + 0] = dst[2];

            dst[0] = mat[0 * 4 + 1] * a.mat[0 * 4 + 0] + mat[1 * 4 + 1] * a.mat[1 * 4 + 0] + mat[2 * 4 + 1] * a.mat[2 * 4 + 0];
            dst[1] = mat[0 * 4 + 1] * a.mat[0 * 4 + 1] + mat[1 * 4 + 1] * a.mat[1 * 4 + 1] + mat[2 * 4 + 1] * a.mat[2 * 4 + 1];
            dst[2] = mat[0 * 4 + 1] * a.mat[0 * 4 + 2] + mat[1 * 4 + 1] * a.mat[1 * 4 + 2] + mat[2 * 4 + 1] * a.mat[2 * 4 + 2];
            mat[0 * 4 + 1] = dst[0];
            mat[1 * 4 + 1] = dst[1];
            mat[2 * 4 + 1] = dst[2];

            dst[0] = mat[0 * 4 + 2] * a.mat[0 * 4 + 0] + mat[1 * 4 + 2] * a.mat[1 * 4 + 0] + mat[2 * 4 + 2] * a.mat[2 * 4 + 0];
            dst[1] = mat[0 * 4 + 2] * a.mat[0 * 4 + 1] + mat[1 * 4 + 2] * a.mat[1 * 4 + 1] + mat[2 * 4 + 2] * a.mat[2 * 4 + 1];
            dst[2] = mat[0 * 4 + 2] * a.mat[0 * 4 + 2] + mat[1 * 4 + 2] * a.mat[1 * 4 + 2] + mat[2 * 4 + 2] * a.mat[2 * 4 + 2];
            mat[0 * 4 + 2] = dst[0];
            mat[1 * 4 + 2] = dst[1];
            mat[2 * 4 + 2] = dst[2];

            dst[0] = mat[0 * 4 + 3] * a.mat[0 * 4 + 0] + mat[1 * 4 + 3] * a.mat[1 * 4 + 0] + mat[2 * 4 + 3] * a.mat[2 * 4 + 0];
            dst[1] = mat[0 * 4 + 3] * a.mat[0 * 4 + 1] + mat[1 * 4 + 3] * a.mat[1 * 4 + 1] + mat[2 * 4 + 3] * a.mat[2 * 4 + 1];
            dst[2] = mat[0 * 4 + 3] * a.mat[0 * 4 + 2] + mat[1 * 4 + 3] * a.mat[1 * 4 + 2] + mat[2 * 4 + 3] * a.mat[2 * 4 + 2];
            mat[0 * 4 + 3] = dst[0];
            mat[1 * 4 + 3] = dst[1];
            mat[2 * 4 + 3] = dst[2];

            return this;
        }

        // exact compare, no epsilon
        public boolean Compare(final idJointMat a) {
            int i;

            for (i = 0; i < 12; i++) {
                if (mat[i] != a.mat[i]) {
                    return false;
                }
            }
            return true;
        }

        // compare with epsilon
        public boolean Compare(final idJointMat a, final float epsilon) {
            int i;

            for (i = 0; i < 12; i++) {
                if (idMath.Fabs(mat[i] - a.mat[i]) > epsilon) {
                    return false;
                }
            }
            return true;
        }
//public	bool			operator==(	const idJointMat &a ) const;					// exact compare, no epsilon
//public	bool			operator!=(	const idJointMat &a ) const;					// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + Arrays.hashCode(this.mat);
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
            final idJointMat other = (idJointMat) obj;
            if (!Arrays.equals(this.mat, other.mat)) {
                return false;
            }
            return true;
        }

        public idMat3 ToMat3() {
            return new idMat3(
                    mat[0 * 4 + 0], mat[1 * 4 + 0], mat[2 * 4 + 0],
                    mat[0 * 4 + 1], mat[1 * 4 + 1], mat[2 * 4 + 1],
                    mat[0 * 4 + 2], mat[1 * 4 + 2], mat[2 * 4 + 2]);
        }

        public idVec3 ToVec3() {
            return new idVec3(
                    mat[0 * 4 + 3],
                    mat[1 * 4 + 3],
                    mat[2 * 4 + 3]);
        }

        public idJointQuat ToJointQuat() {
            idJointQuat jq = new idJointQuat();
            float trace;
            float s;
            float t;
            int i;
            int j;
            int k;

            int[] next = {1, 2, 0};

            trace = mat[0 * 4 + 0] + mat[1 * 4 + 1] + mat[2 * 4 + 2];

            if (trace > 0.0f) {

                t = trace + 1.0f;
                s = idMath.InvSqrt(t) * 0.5f;

                jq.q.oSet(3, s * t);
                jq.q.oSet(0, (mat[1 * 4 + 2] - mat[2 * 4 + 1]) * s);
                jq.q.oSet(1, (mat[2 * 4 + 0] - mat[0 * 4 + 2]) * s);
                jq.q.oSet(2, (mat[0 * 4 + 1] - mat[1 * 4 + 0]) * s);

            } else {

                i = 0;
                if (mat[1 * 4 + 1] > mat[0 * 4 + 0]) {
                    i = 1;
                }
                if (mat[2 * 4 + 2] > mat[i * 4 + i]) {
                    i = 2;
                }
                j = next[i];
                k = next[j];

                t = (mat[i * 4 + i] - (mat[j * 4 + j] + mat[k * 4 + k])) + 1.0f;
                s = idMath.InvSqrt(t) * 0.5f;

                jq.q.oSet(i, s * t);
                jq.q.oSet(3, (mat[j * 4 + k] - mat[k * 4 + j]) * s);
                jq.q.oSet(j, (mat[i * 4 + j] + mat[j * 4 + i]) * s);
                jq.q.oSet(k, (mat[i * 4 + k] + mat[k * 4 + i]) * s);
            }

            jq.t.oSet(0, mat[0 * 4 + 3]);
            jq.t.oSet(1, mat[1 * 4 + 3]);
            jq.t.oSet(2, mat[2 * 4 + 3]);

            return jq;
        }
//public	const float *	ToFloatPtr( void ) const;

        public float[] ToFloatPtr() {
            return mat;
        }
    };
}
