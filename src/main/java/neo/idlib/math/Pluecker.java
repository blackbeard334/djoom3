package neo.idlib.math;

import java.util.Arrays;

import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.open.FloatOGet;

/**
 *  Plücker
 */
public class Pluecker {

    /*
     ===============================================================================

     Pluecker coordinate

     ===============================================================================
     */
    public static class idPluecker implements FloatOGet {
        private final float[] p = new float[6];
        

        public idPluecker() {
        }

        public idPluecker(final float[] a) {
            System.arraycopy(a, 0, this.p, 0, 6);//memcpy( p, a, 6 * sizeof( float ) );
        }

        public idPluecker(final idVec3 start, final idVec3 end) {
            this.FromLine(start, end);
        }

        public idPluecker(final float a1, final float a2, final float a3, final float a4, final float a5, final float a6) {
            this.p[0] = a1;
            this.p[1] = a2;
            this.p[2] = a3;
            this.p[3] = a4;
            this.p[4] = a5;
            this.p[5] = a6;
        }

        //public	float			operator[]( final int index ) final;
        public float oGet(final int index) {
            return this.p[index];
        }

        public idPluecker oNegative() {// flips the direction
            return new idPluecker(-this.p[0], -this.p[1], -this.p[2], -this.p[3], -this.p[4], -this.p[5]);
        }

        public idPluecker oMultiply(final float a) {
            return new idPluecker(this.p[0] * a, this.p[1] * a, this.p[2] * a, this.p[3] * a, this.p[4] * a, this.p[5] * a);
        }

        public idPluecker oDivide(final float a) {
            float inva;

            assert (a != 0.0f);
            inva = 1.0f / a;
            return new idPluecker(this.p[0] * inva, this.p[1] * inva, this.p[2] * inva, this.p[3] * inva, this.p[4] * inva, this.p[5] * inva);
        }

        public float oMultiply(final idPluecker a) {// permuted inner product
            return (this.p[0] * a.p[4]) + (this.p[1] * a.p[5]) + (this.p[2] * a.p[3]) + (this.p[4] * a.p[0]) + (this.p[5] * a.p[1]) + (this.p[3] * a.p[2]);
        }

        public idPluecker oMinus(final idPluecker a) {
            return new idPluecker(this.p[0] - a.oGet(0), this.p[1] - a.oGet(1), this.p[2] - a.oGet(2), this.p[3] - a.oGet(3), this.p[4] - a.oGet(4), this.p[5] - a.oGet(5));
        }

        public idPluecker oPlus(final idPluecker a) {
            return new idPluecker(this.p[0] + a.oGet(0), this.p[1] + a.oGet(1), this.p[2] + a.oGet(2), this.p[3] + a.oGet(3), this.p[4] + a.oGet(4), this.p[5] + a.oGet(5));
        }

        public idPluecker oMulSet(final float a) {
            this.p[0] *= a;
            this.p[1] *= a;
            this.p[2] *= a;
            this.p[3] *= a;
            this.p[4] *= a;
            this.p[5] *= a;
            return this;
        }

        public idPluecker oDivSet(final float a) {
            float inva;

            assert (a != 0.0f);
            inva = 1.0f / a;
            this.p[0] *= inva;
            this.p[1] *= inva;
            this.p[2] *= inva;
            this.p[3] *= inva;
            this.p[4] *= inva;
            this.p[5] *= inva;
            return this;
        }

        public idPluecker oPluSet(final idPluecker a) {
            this.p[0] += a.oGet(0);
            this.p[1] += a.oGet(1);
            this.p[2] += a.oGet(2);
            this.p[3] += a.oGet(3);
            this.p[4] += a.oGet(4);
            this.p[5] += a.oGet(5);
            return this;
        }

        public idPluecker oMinSet(final idPluecker a) {
            this.p[0] -= a.oGet(0);
            this.p[1] -= a.oGet(1);
            this.p[2] -= a.oGet(2);
            this.p[3] -= a.oGet(3);
            this.p[4] -= a.oGet(4);
            this.p[5] -= a.oGet(5);
            return this;
        }

        public boolean Compare(final idPluecker a) {// exact compare, no epsilon
            return ((this.p[0] == a.p[0]) && (this.p[1] == a.p[1]) && (this.p[2] == a.p[2])
                    && (this.p[3] == a.p[3]) && (this.p[4] == a.p[4]) && (this.p[5] == a.p[5]));
        }

        public boolean Compare(final idPluecker a, final float epsilon) {// compare with epsilon
            if (idMath.Fabs(this.p[0] - a.p[0]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[1] - a.p[1]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[2] - a.p[2]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[3] - a.p[3]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[4] - a.p[4]) > epsilon) {
                return false;
            }

            if (idMath.Fabs(this.p[5] - a.p[5]) > epsilon) {
                return false;
            }

            return true;
        }
//public	boolean			operator==(	final idPluecker &a ) final;					// exact compare, no epsilon
//public	boolean			operator!=(	final idPluecker &a ) final;					// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 5;
            hash = (83 * hash) + Arrays.hashCode(this.p);
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

        public void Set(final idPluecker a) {
            this.p[0] = a.p[0];
            this.p[1] = a.p[1];
            this.p[2] = a.p[2];
            this.p[3] = a.p[3];
            this.p[4] = a.p[4];
            this.p[5] = a.p[5];
        }

        public void Set(final float a1, final float a2, final float a3, final float a4, final float a5, final float a6) {
            this.p[0] = a1;
            this.p[1] = a2;
            this.p[2] = a3;
            this.p[3] = a4;
            this.p[4] = a5;
            this.p[5] = a6;
        }

        public void Zero() {
            this.p[0] = this.p[1] = this.p[2] = this.p[3] = this.p[4] = this.p[5] = 0.0f;
        }

        public void FromLine(final idVec3 start, final idVec3 end) {// pluecker from line{
            this.p[0] = (start.oGet(0) * end.oGet(1)) - (end.oGet(0) * start.oGet(1));
            this.p[1] = (start.oGet(0) * end.oGet(2)) - (end.oGet(0) * start.oGet(2));
            this.p[2] = start.oGet(0) - end.oGet(0);
            this.p[3] = (start.oGet(1) * end.oGet(2)) - (end.oGet(1) * start.oGet(2));
            this.p[4] = start.oGet(2) - end.oGet(2);
            this.p[5] = end.oGet(1) - start.oGet(1);
        }

        public void FromRay(final idVec3 start, final idVec3 dir) {// pluecker from ray
            this.p[0] = (start.oGet(0) * dir.oGet(1)) - (dir.oGet(0) * start.oGet(1));
            this.p[1] = (start.oGet(0) * dir.oGet(2)) - (dir.oGet(0) * start.oGet(2));
            this.p[2] = -dir.oGet(0);
            this.p[3] = (start.oGet(1) * dir.oGet(2)) - (dir.oGet(1) * start.oGet(2));
            this.p[4] = -dir.oGet(2);
            this.p[5] = dir.oGet(1);
        }

        /*
         ================
         idPluecker::FromPlanes

         pluecker coordinate for the intersection of two planes
         ================
         */
        public boolean FromPlanes(final idPlane p1, final idPlane p2) {// pluecker from intersection of planes

            this.p[0] = -((p1.oGet(2) * -p2.oGet(3)) - (p2.oGet(2) * -p1.oGet(3)));
            this.p[1] = -((p2.oGet(1) * -p1.oGet(3)) - (p1.oGet(1) * -p2.oGet(3)));
            this.p[2] = (p1.oGet(1) * p2.oGet(2)) - (p2.oGet(1) * p1.oGet(2));

            this.p[3] = -((p1.oGet(0) * -p2.oGet(3)) - (p2.oGet(0) * -p1.oGet(3)));
            this.p[4] = (p1.oGet(0) * p2.oGet(1)) - (p2.oGet(0) * p1.oGet(1));
            this.p[5] = (p1.oGet(0) * p2.oGet(2)) - (p2.oGet(0) * p1.oGet(2));

            return ((this.p[2] != 0.0f) || (this.p[5] != 0.0f) || (this.p[4] != 0.0f));
        }

        // pluecker to line
        public boolean ToLine(idVec3 start, idVec3 end) {
            final idVec3 dir1 = new idVec3(), dir2 = new idVec3();
            float d;

            dir1.oSet(0, this.p[3]);
            dir1.oSet(1, -this.p[1]);
            dir1.oSet(2, this.p[0]);

            dir2.oSet(0, -this.p[2]);
            dir2.oSet(1, this.p[5]);
            dir2.oSet(2, -this.p[4]);

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
            final idVec3 dir1 = new idVec3();
            float d;

            dir1.oSet(0, this.p[3]);
            dir1.oSet(1, -this.p[1]);
            dir1.oSet(2, this.p[0]);

            dir.oSet(0, -this.p[2]);
            dir.oSet(1, this.p[5]);
            dir.oSet(2, -this.p[4]);

            d = dir.oMultiply(dir);
            if (d == 0.0f) {
                return false; // pluecker coordinate does not represent a line
            }

            start.oSet(dir.Cross(dir1).oMultiply((1.0f / d)));
            return true;
        }

        public void ToDir(idVec3 dir) {// pluecker to direction{
            dir.oSet(0, -this.p[2]);
            dir.oSet(1, this.p[5]);
            dir.oSet(2, -this.p[4]);
        }

        public float PermutedInnerProduct(final idPluecker a) {// pluecker permuted inner product
            return (this.p[0] * a.p[4]) + (this.p[1] * a.p[5]) + (this.p[2] * a.p[3]) + (this.p[4] * a.p[0]) + (this.p[5] * a.p[1]) + (this.p[3] * a.p[2]);
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
            final idVec3 dir = new idVec3();

            dir.oSet(0, (-a.p[5] * this.p[4]) - (a.p[4] * -this.p[5]));
            dir.oSet(1, (a.p[4] * this.p[2]) - (a.p[2] * this.p[4]));
            dir.oSet(2, (a.p[2] * -this.p[5]) - (-a.p[5] * this.p[2]));
            if ((dir.oGet(0) == 0.0f) && (dir.oGet(1) == 0.0f) && (dir.oGet(2) == 0.0f)) {
                return -1.0f;    // FIXME: implement for parallel lines
            }
            d = (a.p[4] * ((this.p[2] * dir.oGet(1)) - (-this.p[5] * dir.oGet(0))))
                    + (a.p[5] * ((this.p[2] * dir.oGet(2)) - (this.p[4] * dir.oGet(0))))
                    + (a.p[2] * ((-this.p[5] * dir.oGet(2)) - (this.p[4] * dir.oGet(1))));
            s = PermutedInnerProduct(a) / d;
            return (dir.oMultiply(dir)) * (s * s);
        }

        public float Length() {// pluecker length
            return idMath.Sqrt((this.p[5] * this.p[5]) + (this.p[4] * this.p[4]) + (this.p[2] * this.p[2]));
        }

        public float LengthSqr() {// pluecker squared length
            return ((this.p[5] * this.p[5]) + (this.p[4] * this.p[4]) + (this.p[2] * this.p[2]));
        }

        public idPluecker Normalize() {// pluecker normalize
            float d;

            d = LengthSqr();
            if (d == 0.0f) {
                return this; // pluecker coordinate does not represent a line
            }
            d = idMath.InvSqrt(d);
            return new idPluecker(this.p[0] * d, this.p[1] * d, this.p[2] * d, this.p[3] * d, this.p[4] * d, this.p[5] * d);
        }

        public float NormalizeSelf() {// pluecker normalize 
            float l, d;

            l = LengthSqr();
            if (l == 0.0f) {
                return l; // pluecker coordinate does not represent a line
            }
            d = idMath.InvSqrt(l);
            this.p[0] *= d;
            this.p[1] *= d;
            this.p[2] *= d;
            this.p[3] *= d;
            this.p[4] *= d;
            this.p[5] *= d;
            return d * l;
        }

        public int GetDimension() {
            return 6;
        }
//
//public	final float *	ToFloatPtr( void ) final;
//public	float *			ToFloatPtr( void );
//public	final char *	ToString( int precision = 2 ) final;
    }
}
