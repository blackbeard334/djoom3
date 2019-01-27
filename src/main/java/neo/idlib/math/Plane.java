package neo.idlib.math;

import java.util.stream.Stream;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat2;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Plane {

    public static final float ON_EPSILON = 0.1f;
    public static final float DEGENERATE_DIST_EPSILON = 1e-4f;
    //                                                    
    public static final int SIDE_FRONT = 0;
    public static final int SIDE_BACK = 1;
    public static final int SIDE_ON = 2;
    public static final int SIDE_CROSS = 3;
    //                                                    
    // plane sides                                      
    public static final int PLANESIDE_FRONT = 0;
    public static final int PLANESIDE_BACK = 1;
    private static final int PLANESIDE_ON = 2;
    public static final int PLANESIDE_CROSS = 3;
    // plane types                                            
    private static final int PLANETYPE_X = 0;
    private static final int PLANETYPE_Y = 1;
    private static final int PLANETYPE_Z = 2;
    public static final int PLANETYPE_NEGX = 3;
    private static final int PLANETYPE_NEGY = 4;
    private static final int PLANETYPE_NEGZ = 5;
    public static final int PLANETYPE_TRUEAXIAL = 6;
    // all types < 6 are true axial planes
    private static final int PLANETYPE_ZEROX = 6;
    private static final int PLANETYPE_ZEROY = 7;
    private static final int PLANETYPE_ZEROZ = 8;
    private static final int PLANETYPE_NONAXIAL = 9;

    /*
     ===============================================================================

     3D plane with equation: a * x + b * y + c * z + d = 0

     ===============================================================================
     */
    public static class idPlane {
        public static final int BYTES = idVec3.BYTES + Float.BYTES;

        private final idVec3 abc = new idVec3();
        private float d;

        //
        //

        public idPlane() {
        }

        public idPlane(float a, float b, float c, float d) {
            abc.x = a;
            abc.y = b;
            abc.z = c;
            this.d = d;
        }

        public idPlane(final float[] array) {
            abc.x = array[0];
            abc.y = array[1];
            abc.z = array[2];
            this.d = array[4];
        }

        public idPlane(final idVec3 normal, final float dist) {
            abc.oSet(normal);
            this.d = -dist;
        }

        public idPlane(final idVec4 vec) {
            abc.x = vec.x;
            abc.y = vec.y;
            abc.z = vec.z;
            this.d = vec.w;
        }

        public idPlane(final idPlane plane) {
            abc.x = plane.abc.x;
            abc.y = plane.abc.y;
            abc.z = plane.abc.z;
            this.d = plane.d;
        }
//
//public	float			operator[]( int index ) const;
//public	float &			operator[]( int index );

        public float oGet(int index) {
            switch (index) {
                case 0:
                    return abc.x;
                case 1:
                    return abc.y;
                case 2:
                    return abc.z;
                default:
                    return d;
            }
        }

        public float oSet(int index, final float value) {
            switch (index) {
                case 0:
                    return abc.x = value;
                case 1:
                    return abc.y = value;
                case 2:
                    return abc.z = value;
                default:
                    return d = value;
            }
        }

        public float oPluSet(int index, final float value) {
            switch (index) {
                case 0:
                    return abc.x += value;
                case 1:
                    return abc.y += value;
                case 2:
                    return abc.z += value;
                default:
                    return d += value;
            }
        }

        public float oMinSet(int index, final float value) {
            switch (index) {
                case 0:
                    return abc.x -= value;
                case 1:
                    return abc.y -= value;
                case 2:
                    return abc.z -= value;
                default:
                    return d -= value;
            }
        }

        public float oDivSet(int index, final float value) {
            switch (index) {
                case 0:
                    return abc.x /= value;
                case 1:
                    return abc.y /= value;
                case 2:
                    return abc.z /= value;
                default:
                    return d /= value;
            }
        }
//public	idPlane			operator-() const;						// flips plane

        // flips plane
        public idPlane oNegative() {
            return new idPlane(-abc.x, -abc.y, -abc.z, -d);
        }
//public	idPlane &		operator=( const idVec3 &v );			// sets normal and sets idPlane::d to zero

        // sets normal and sets idPlane::d to zero
        public idPlane oSet(final idVec3 v) {
            abc.oSet(v);
            d = 0;
            return this;
        }
        
        public idPlane oSet(final idPlane p) {
            abc.oSet(p.abc);
            this.d = p.d;
            return this;
        }
//public	idPlane			operator+( const idPlane &p ) const;	// add plane equations

        // add plane equations
        public idPlane oPlus(final idPlane p) {
            return new idPlane(abc.x + p.abc.x, abc.y + p.abc.y, abc.z + p.abc.z, d + p.d);
        }
//public	idPlane			operator-( const idPlane &p ) const;	// subtract plane equations

        // subtract plane equations
        public idPlane oMinus(final idPlane p) {
            return new idPlane(abc.x - p.abc.x, abc.y - p.abc.y, abc.z - p.abc.z, d - p.d);
        }
//public	idPlane &		operator*=( const idMat3 &m );			// Normal() *= m

        // Normal() *= m
        public idPlane oMulSet(final idMat3 m) {
            Normal().oMulSet(m);
            return this;
        }

        // exact compare, no epsilon
        public boolean Compare(final idPlane p) {
            return (abc.x == p.abc.x && abc.y == p.abc.y && abc.z == p.abc.z && d == p.d);
        }

        // compare with epsilon
        public boolean Compare(final idPlane p, final float epsilon) {
            if (idMath.Fabs(abc.x - p.abc.x) > epsilon) {
                return false;
            }

            if (idMath.Fabs(abc.y - p.abc.y) > epsilon) {
                return false;
            }

            if (idMath.Fabs(abc.z - p.abc.z) > epsilon) {
                return false;
            }

            if (idMath.Fabs(d - p.d) > epsilon) {
                return false;
            }

            return true;
        }

        // compare with epsilon
        public boolean Compare(final idPlane p, final float normalEps, final float distEps) {
            if (idMath.Fabs(d - p.d) > distEps) {
                return false;
            }
            if (!Normal().Compare(p.Normal(), normalEps)) {
                return false;
            }
            return true;
        }
//public	boolean			operator==(	const idPlane &p ) const;					// exact compare, no epsilon
//public	boolean			operator!=(	const idPlane &p ) const;					// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Float.floatToIntBits(abc.x);
            hash = 23 * hash + Float.floatToIntBits(abc.y);
            hash = 23 * hash + Float.floatToIntBits(abc.z);
            hash = 23 * hash + Float.floatToIntBits(this.d);
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
            final idPlane other = (idPlane) obj;
            if (Float.floatToIntBits(this.abc.x) != Float.floatToIntBits(other.abc.x)) {
                return false;
            }
            if (Float.floatToIntBits(this.abc.y) != Float.floatToIntBits(other.abc.y)) {
                return false;
            }
            if (Float.floatToIntBits(this.abc.z) != Float.floatToIntBits(other.abc.z)) {
                return false;
            }
            if (Float.floatToIntBits(this.d) != Float.floatToIntBits(other.d)) {
                return false;
            }
            return true;
        }

        // zero plane
        public void Zero() {
            abc.x = abc.y = abc.z = d = 0.0f;
        }

        // sets the normal
        public void SetNormal(final idVec3 normal) {
            abc.oSet(normal);
        }

        // reference to const normal
        public idVec3 Normal() {
            return abc;
        }

        public float NormalX(final float value) {
            return abc.x = value;
        }

        public float NormalY(final float value) {
            return abc.y = value;
        }

        public float NormalZ(final float value) {
            return abc.z = value;
        }

        /**
         * sets the normal <b>ONLY</b>; a, b and c. d is ignored.
         */
        public idPlane oNorSet(final idVec3 v) {
            abc.oSet(v);
            return this;
        }
//public	idVec3 &		Normal( void );							// reference to normal

        // only normalizes the plane normal, does not adjust d
        public float Normalize() {
            return Normalize(true);
        }

        // only normalizes the plane normal, does not adjust d
        public float Normalize(boolean fixDegenerate) {
            idVec3 vec3 = new idVec3(abc.x, abc.y, abc.z);
            float length = vec3.Normalize();

            {
                final float oldD = d;//save old d
                this.oSet(vec3);     //set normalized values
                d = oldD;            //replace the zeroed d with its original value
            }

            if (fixDegenerate) {
                FixDegenerateNormal();
            }
            return length;
        }

        // fix degenerate normal
        public boolean FixDegenerateNormal() {
            idVec3 vec3 = new idVec3(abc.x, abc.y, abc.z);
            boolean fixedNormal = vec3.FixDegenerateNormal();
            {
                final float oldD = d;//save old d
                this.oSet(vec3);     //set new values
                d = oldD;            //replace the zeroed d with its original value
            }
            return fixedNormal;
        }

        // fix degenerate normal and dist
        public boolean FixDegeneracies(float distEpsilon) {
            boolean fixedNormal = FixDegenerateNormal();
            // only fix dist if the normal was degenerate
            if (fixedNormal) {
                if (idMath.Fabs(d - idMath.Rint(d)) < distEpsilon) {
                    d = idMath.Rint(d);
                }
            }
            return fixedNormal;
        }

        // returns: -d
        public float Dist() {
            return -d;
        }

        // sets: d = -dist
        public void SetDist(final float dist) {
            d = -dist;
        }

        // returns plane type
        public int Type() {
            if (Normal().oGet(0) == 0.0f) {
                if (Normal().oGet(1) == 0.0f) {
                    return Normal().oGet(2) > 0.0f ? PLANETYPE_Z : PLANETYPE_NEGZ;
                } else if (Normal().oGet(2) == 0.0f) {
                    return Normal().oGet(1) > 0.0f ? PLANETYPE_Y : PLANETYPE_NEGY;
                } else {
                    return PLANETYPE_ZEROX;
                }
            } else if (Normal().oGet(1) == 0.0f) {
                if (Normal().oGet(2) == 0.0f) {
                    return Normal().oGet(0) > 0.0f ? PLANETYPE_X : PLANETYPE_NEGX;
                } else {
                    return PLANETYPE_ZEROY;
                }
            } else if (Normal().oGet(2) == 0.0f) {
                return PLANETYPE_ZEROZ;
            } else {
                return PLANETYPE_NONAXIAL;
            }
        }

        public boolean FromPoints(final idVec3 p1, final idVec3 p2, final idVec3 p3) {
            return FromPoints(p1, p2, p3, true);
        }

        public boolean FromPoints(final idVec3 p1, final idVec3 p2, final idVec3 p3, boolean fixDegenerate) {
            idVec3 vec3 = Normal().oSet(p1.oMinus(p2)).Cross(p3.oMinus(p2));

            {                      //TODO: remove this pointless shit
                final float oldD = d;//save old d
                this.oSet(vec3);     //set new values
                d = oldD;            //replace the zeroed d with its original value
            }

            if (Normalize(fixDegenerate) == 0.0f) {
                return false;
            }
            d = -(Normal().oMultiply(p2));
            return true;
        }

        public boolean FromVecs(final idVec3 dir1, final idVec3 dir2, final idVec3 p) {
            return FromVecs(dir1, dir2, p, true);
        }

        public boolean FromVecs(final idVec3 dir1, final idVec3 dir2, final idVec3 p, boolean fixDegenerate) {
            idVec3 vec3 = Normal().oSet(dir1.Cross(dir2));

            {
                final float oldD = d;//save old d
                this.oSet(vec3);     //set new values
                d = oldD;            //replace the zeroed d with its original value
            }

            if (Normalize(fixDegenerate) == 0.0f) {
                return false;
            }
            d = -(Normal().oMultiply(p));
            return true;
        }

        // assumes normal is valid
        public void FitThroughPoint(final idVec3 p) {
            d = -(Normal().oMultiply(p));
        }

        public boolean HeightFit(final idVec3[] points, final int numPoints) {
            int i;
            float sumXX = 0.0f, sumXY = 0.0f, sumXZ = 0.0f;
            float sumYY = 0.0f, sumYZ = 0.0f;
            idVec3 sum = new idVec3(), average = new idVec3(), dir;

            if (numPoints == 1) {
                abc.x = 0.0f;
                abc.y = 0.0f;
                abc.z = 1.0f;
                d = -points[0].z;
                return true;
            }
            if (numPoints == 2) {
                dir = points[1].oMinus(points[0]);
//		Normal() = dir.Cross( idVec3( 0, 0, 1 ) ).Cross( dir );
                {
                    final float oldD = d;//save old d
                    this.oSet(dir.Cross(new idVec3(0, 0, 1)).Cross(dir));
                    d = oldD;            //replace the zeroed d with its original value
                }
                Normalize();
                d = -(Normal().oMultiply(points[0]));
                return true;
            }

            sum.Zero();
            for (i = 0; i < numPoints; i++) {
                sum.oPluSet(points[i]);
            }
            average.oSet(sum.oDivide(numPoints));

            for (i = 0; i < numPoints; i++) {
                dir = points[i].oMinus(average);
                sumXX += dir.x * dir.x;
                sumXY += dir.x * dir.y;
                sumXZ += dir.x * dir.z;
                sumYY += dir.y * dir.y;
                sumYZ += dir.y * dir.z;
            }

            idMat2 m = new idMat2(sumXX, sumXY, sumXY, sumYY);
            if (!m.InverseSelf()) {
                return false;
            }

            abc.x = -sumXZ * m.oGet(0).x - sumYZ * m.oGet(0).y;
            abc.y = -sumXZ * m.oGet(1).x - sumYZ * m.oGet(1).y;
            abc.z = 1.0f;
            Normalize();
            d = -(abc.x * average.x + abc.y * average.y + abc.z * average.z);
            return true;
        }

        public idPlane Translate(final idVec3 translation) {
            return new idPlane(abc.x, abc.y, abc.z, d - translation.oMultiply(Normal()));
        }

        public idPlane TranslateSelf(final idVec3 translation) {
            d -= translation.oMultiply(Normal());
            return this;
        }

        public idPlane Rotate(final idVec3 origin, final idMat3 axis) {
            idPlane p = new idPlane();
            p.oSet(axis.oMultiply(Normal()));
            p.d = d + origin.oMultiply(Normal()) - origin.oMultiply(p.Normal());
            return p;
        }

        public idPlane RotateSelf(final idVec3 origin, final idMat3 axis) {
            d += origin.oMultiply(Normal());

            {
                final float oldD = d;//save old d
                this.oSet(axis.oMultiply(Normal()));     //set new values
                d = oldD;            //replace the zeroed d with its original value
            }

            d -= origin.oMultiply(Normal());
            return this;
        }

        public float Distance(final idVec3 v) {
            return abc.x * v.x + abc.y * v.y + abc.z * v.z + d;
        }

        public int Side(final idVec3 v) {
            return Side(v, 0.0f);
        }

        public int Side(final idVec3 v, final float epsilon) {
            float dist = Distance(v);
            if (dist > epsilon) {
                return PLANESIDE_FRONT;
            } else if (dist < -epsilon) {
                return PLANESIDE_BACK;
            } else {
                return PLANESIDE_ON;
            }
        }

        public boolean LineIntersection(final idVec3 start, final idVec3 end) {
            float d1, d2, fraction;

            d1 = Normal().oMultiply(start.oPlus(d));
            d2 = Normal().oMultiply(end.oPlus(d));
            if (d1 == d2) {
                return false;
            }
            if (d1 > 0.0f && d2 > 0.0f) {
                return false;
            }
            if (d1 < 0.0f && d2 < 0.0f) {
                return false;
            }
            fraction = (d1 / (d1 - d2));
            return (fraction >= 0.0f && fraction <= 1.0f);
        }

        // intersection point is start + dir * scale
        public boolean RayIntersection(final idVec3 start, final idVec3 dir, float[] scale) {
            float d1, d2;

            d1 = Normal().oMultiply(start.oPlus(d));
            d2 = Normal().oMultiply(dir);
            if (d2 == 0.0f) {
                return false;
            }
            scale[0] = -(d1 / d2);
            return true;
        }

        public boolean PlaneIntersection(final idPlane plane, idVec3 start, idVec3 dir) {
            float n00, n01, n11, det, invDet, f0, f1;

            n00 = Normal().LengthSqr();
            n01 = Normal().oMultiply(plane.Normal());
            n11 = plane.Normal().LengthSqr();
            det = n00 * n11 - n01 * n01;

            if (idMath.Fabs(det) < 1e-6f) {
                return false;
            }

            invDet = 1.0f / det;
            f0 = (n01 * plane.d - n11 * d) * invDet;
            f1 = (n01 * d - n00 * plane.d) * invDet;

            dir.oSet(Normal().Cross(plane.Normal()));
//            start = f0 * Normal() + f1 * plane.Normal();
            start.oSet(Normal().oMultiply(f0).oPlus(plane.Normal().oMultiply(f1)));
            return true;
        }

        public int GetDimension() {
            return 4;
        }
//
//public	const idVec4 &	ToVec4( void ) const;

        public idVec4 ToVec4() {
            return new idVec4(abc.x, abc.y, abc.z, d);
        }

        public void ToVec4_oPluSet(final idVec4 v) {
            abc.x += v.x;
            abc.y += v.y;
            abc.z += v.z;
            d += v.w;
        }

        public void ToVec4_ToVec3_Cross(final idVec3 a, final idVec3 b) {
            abc.Cross(a, b);
        }

        public void ToVec4_ToVec3_Normalize() {
            abc.Normalize();
        }

        public float[] ToFloatPtr() {
            return new float[]{abc.x, abc.y, abc.z, d};
        }
//public	float *			ToFloatPtr( void );
        
        public static idPlane[] generateArray(final int length) {
            return Stream.
                    generate(idPlane::new).
                    limit(length).
                    toArray(idPlane[]::new);
        }

        @Override
        public String toString() {
            return "idPlane{"
                    + "a=" + abc.x
                    + ", b=" + abc.y
                    + ", c=" + abc.z
                    + ", d=" + d + "}";
        }
    };
}
