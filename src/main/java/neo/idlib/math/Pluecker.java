package neo.idlib.math;

import java.util.Arrays;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Pluecker {

    /*
     ===============================================================================

     Pluecker coordinate

     ===============================================================================
     */
    public static class idPluecker {

        public idPluecker() {
        }

        public idPluecker(final float[] a) {
//    memcpy( p, a, 6 * sizeof( float ) );
            System.arraycopy(a, 0, p, 0, 6);
        }

        public idPluecker(final idVec3 start, final idVec3 end) {
            this.FromLine(start, end);
        }

        public idPluecker(final float a1, final float a2, final float a3, final float a4, final float a5, final float a6) {
            p[0] = a1;
            p[1] = a2;
            p[2] = a3;
            p[3] = a4;
            p[4] = a5;
            p[5] = a6;
        }
//
//public	float			operator[]( final int index ) final;

        public float oGet(final int index) {
            return p[index];
        }

        public idPluecker oNegative() {// flips the direction
            return new idPluecker(-p[0], -p[1], -p[2], -p[3], -p[4], -p[5]);
        }

        public idPluecker oMultiply(final float a) {
            return new idPluecker(p[0] * a, p[1] * a, p[2] * a, p[3] * a, p[4] * a, p[5] * a);
        }

        public idPluecker oDivide(final float a) {
            float inva;

            assert (a != 0.0f);
            inva = 1.0f / a;
            return new idPluecker(p[0] * inva, p[1] * inva, p[2] * inva, p[3] * inva, p[4] * inva, p[5] * inva);
        }

        public float oMultiply(final idPluecker a) {// permuted inner product
            return p[0] * a.p[4] + p[1] * a.p[5] + p[2] * a.p[3] + p[4] * a.p[0] + p[5] * a.p[1] + p[3] * a.p[2];
        }

        public idPluecker oMinus(final idPluecker a) {
            return new idPluecker(p[0] - a.oGet(0), p[1] - a.oGet(1), p[2] - a.oGet(2), p[3] - a.oGet(3), p[4] - a.oGet(4), p[5] - a.oGet(5));
        }

        public idPluecker oPlus(final idPluecker a) {
            return new idPluecker(p[0] + a.oGet(0), p[1] + a.oGet(1), p[2] + a.oGet(2), p[3] + a.oGet(3), p[4] + a.oGet(4), p[5] + a.oGet(5));
        }

        public idPluecker oMulSet(final float a) {
            p[0] *= a;
            p[1] *= a;
            p[2] *= a;
            p[3] *= a;
            p[4] *= a;
            p[5] *= a;
            return this;
        }

        public idPluecker oDivSet(final float a) {
            float inva;

            assert (a != 0.0f);
            inva = 1.0f / a;
            p[0] *= inva;
            p[1] *= inva;
            p[2] *= inva;
            p[3] *= inva;
            p[4] *= inva;
            p[5] *= inva;
            return this;
        }

        public idPluecker oPluSet(final idPluecker a) {
            p[0] += a.oGet(0);
            p[1] += a.oGet(1);
            p[2] += a.oGet(2);
            p[3] += a.oGet(3);
            p[4] += a.oGet(4);
            p[5] += a.oGet(5);
            return this;
        }

        public idPluecker oMinSet(final idPluecker a) {
            p[0] -= a.oGet(0);
            p[1] -= a.oGet(1);
            p[2] -= a.oGet(2);
            p[3] -= a.oGet(3);
            p[4] -= a.oGet(4);
            p[5] -= a.oGet(5);
            return this;
        }
//

        public boolean Compare(final idPluecker a) {// exact compare, no epsilon
            return ((p[0] == a.p[0]) && (p[1] == a.p[1]) && (p[2] == a.p[2])
                    && (p[3] == a.p[3]) && (p[4] == a.p[4]) && (p[5] == a.p[5]));
        }

        public boolean Compare(final idPluecker a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(p[0] - a.p[0]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[1] - a.p[1]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[2] - a.p[2]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[3] - a.p[3]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[4] - a.p[4]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(p[5] - a.p[5]) > epsilon) {
                return false;
            }

            return true;
        }
//public	boolean			operator==(	final idPluecker &a ) final;					// exact compare, no epsilon
//public	boolean			operator!=(	final idPluecker &a ) final;					// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 83 * hash + Arrays.hashCode(this.p);
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
            final idPluecker other = (idPluecker) obj;
            if (!Arrays.equals(this.p, other.p)) {
                return false;
            }
            return true;
        }
//

        public void Set(final float a1, final float a2, final float a3, final float a4, final float a5, final float a6) {
            p[0] = a1;
            p[1] = a2;
            p[2] = a3;
            p[3] = a4;
            p[4] = a5;
            p[5] = a6;
        }

        public void Zero() {
            p[0] = p[1] = p[2] = p[3] = p[4] = p[5] = 0.0f;
        }
//

        public void FromLine(final idVec3 start, final idVec3 end) {// pluecker from line{
            p[0] = start.oGet(0) * end.oGet(1) - end.oGet(0) * start.oGet(1);
            p[1] = start.oGet(0) * end.oGet(2) - end.oGet(0) * start.oGet(2);
            p[2] = start.oGet(0) - end.oGet(0);
            p[3] = start.oGet(1) * end.oGet(2) - end.oGet(1) * start.oGet(2);
            p[4] = start.oGet(2) - end.oGet(2);
            p[5] = end.oGet(1) - start.oGet(1);
        }

        public void FromRay(final idVec3 start, final idVec3 dir) {// pluecker from ray
            p[0] = start.oGet(0) * dir.oGet(1) - dir.oGet(0) * start.oGet(1);
            p[1] = start.oGet(0) * dir.oGet(2) - dir.oGet(0) * start.oGet(2);
            p[2] = -dir.oGet(0);
            p[3] = start.oGet(1) * dir.oGet(2) - dir.oGet(1) * start.oGet(2);
            p[4] = -dir.oGet(2);
            p[5] = dir.oGet(1);
        }

        /*
         ================
         idPluecker::FromPlanes

         pluecker coordinate for the intersection of two planes
         ================
         */
        public boolean FromPlanes(final idPlane p1, final idPlane p2) {// pluecker from intersection of planes

            p[0] = -(p1.oGet(2) * -p2.oGet(3) - p2.oGet(2) * -p1.oGet(3));
            p[1] = -(p2.oGet(1) * -p1.oGet(3) - p1.oGet(1) * -p2.oGet(3));
            p[2] = p1.oGet(1) * p2.oGet(2) - p2.oGet(1) * p1.oGet(2);

            p[3] = -(p1.oGet(0) * -p2.oGet(3) - p2.oGet(0) * -p1.oGet(3));
            p[4] = p1.oGet(0) * p2.oGet(1) - p2.oGet(0) * p1.oGet(1);
            p[5] = p1.oGet(0) * p2.oGet(2) - p2.oGet(0) * p1.oGet(2);

            return (p[2] != 0.0f || p[5] != 0.0f || p[4] != 0.0f);
        }

        // pluecker to line
        public boolean ToLine(idVec3 start, idVec3 end) {
            idVec3 dir1 = new idVec3(), dir2 = new idVec3();
            float d;

            dir1.oSet(0, p[3]);
            dir1.oSet(1, -p[1]);
            dir1.oSet(2, p[0]);

            dir2.oSet(0, -p[2]);
            dir2.oSet(1, p[5]);
            dir2.oSet(2, -p[4]);

            d = dir2.oMultiply(dir2);
            if (d == 0.0f) {
                return false; // pluecker coordinate does not represent a line
            }

            start.oSet(dir2.Cross(dir1).oMultiply((1.0f / d)));
            end.oSet(start.oPlus(dir2));
            return true;
        }

        // pluecker to ray
        public boolean ToRay(idVec3 start, idVec3 dir) {
            idVec3 dir1 = new idVec3();
            float d;

            dir1.oSet(0, p[3]);
            dir1.oSet(1, -p[1]);
            dir1.oSet(2, p[0]);

            dir.oSet(0, -p[2]);
            dir.oSet(1, p[5]);
            dir.oSet(2, -p[4]);

            d = dir.oMultiply(dir);
            if (d == 0.0f) {
                return false; // pluecker coordinate does not represent a line
            }

            start.oSet(dir.Cross(dir1).oMultiply((1.0f / d)));
            return true;
        }

        public void ToDir(idVec3 dir) {// pluecker to direction{
            dir.oSet(0, -p[2]);
            dir.oSet(1, p[5]);
            dir.oSet(2, -p[4]);
        }

        public float PermutedInnerProduct(final idPluecker a) {// pluecker permuted inner product
            return p[0] * a.p[4] + p[1] * a.p[5] + p[2] * a.p[3] + p[4] * a.p[0] + p[5] * a.p[1] + p[3] * a.p[2];
        }

        /*
         ================
         idPluecker::Distance3DSqr

         calculates square of shortest distance between the two
         3D lines represented by their pluecker coordinates
         ================
         */
        public float Distance3DSqr(final idPluecker a) {// pluecker line distance{
            float d, s;
            idVec3 dir = new idVec3();

            dir.oSet(0, -a.p[5] * p[4] - a.p[4] * -p[5]);
            dir.oSet(1, a.p[4] * p[2] - a.p[2] * p[4]);
            dir.oSet(2, a.p[2] * -p[5] - -a.p[5] * p[2]);
            if (dir.oGet(0) == 0.0f && dir.oGet(1) == 0.0f && dir.oGet(2) == 0.0f) {
                return -1.0f;	// FIXME: implement for parallel lines
            }
            d = a.p[4] * (p[2] * dir.oGet(1) - -p[5] * dir.oGet(0))
                    + a.p[5] * (p[2] * dir.oGet(2) - p[4] * dir.oGet(0))
                    + a.p[2] * (-p[5] * dir.oGet(2) - p[4] * dir.oGet(1));
            s = PermutedInnerProduct(a) / d;
            return (dir.oMultiply(dir)) * (s * s);
        }
//

        public float Length() {// pluecker length
            return idMath.Sqrt(p[5] * p[5] + p[4] * p[4] + p[2] * p[2]);
        }

        public float LengthSqr() {// pluecker squared length
            return (p[5] * p[5] + p[4] * p[4] + p[2] * p[2]);
        }

        public idPluecker Normalize() {// pluecker normalize
            float d;

            d = LengthSqr();
            if (d == 0.0f) {
                return this; // pluecker coordinate does not represent a line
            }
            d = idMath.InvSqrt(d);
            return new idPluecker(p[0] * d, p[1] * d, p[2] * d, p[3] * d, p[4] * d, p[5] * d);
        }

        public float NormalizeSelf() {// pluecker normalize 
            float l, d;

            l = LengthSqr();
            if (l == 0.0f) {
                return l; // pluecker coordinate does not represent a line
            }
            d = idMath.InvSqrt(l);
            p[0] *= d;
            p[1] *= d;
            p[2] *= d;
            p[3] *= d;
            p[4] *= d;
            p[5] *= d;
            return d * l;
        }
//

        public int GetDimension() {
            return 6;
        }
//
//public	final float *	ToFloatPtr( void ) final;
//public	float *			ToFloatPtr( void );
//public	final char *	ToString( int precision = 2 ) final;
        private float[] p = new float[6];
    };
}
