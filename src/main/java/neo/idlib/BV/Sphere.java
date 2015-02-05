package neo.idlib.BV;

import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Vector.idVec3;

import java.util.Objects;

import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_CROSS;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;
import static neo.idlib.math.Simd.SIMDProcessor;

/**
 *
 */
public class Sphere {
    /*
     ===============================================================================

     Sphere

     ===============================================================================
     */

    public static class idSphere {

        private idVec3 origin;
        private float  radius;
        //
        //

        public idSphere() {
        }

        public idSphere(final idVec3 point) {
            origin = new idVec3(point);
            radius = 0.0f;
        }

        public idSphere(final idVec3 point, final float r) {
            origin = new idVec3(point);
            radius = r;
        }
//

        public float oGet(final int index) {
            return origin.oGet(index);
        }

        public float oSet(final int index, final float value) {
            return origin.oSet(index, value);
        }

        public idSphere oPlus(final idVec3 t) {                // returns tranlated sphere
            return new idSphere(origin.oPlus(t), radius);
        }

        public idSphere oPluSet(final idVec3 t) {					// translate the sphere
            origin.oPluSet(t);
            return this;
        }
//public	idSphere		operator+( final idSphere &s );
//public	idSphere &		operator+=( final idSphere &s );
//

        public boolean Compare(final idSphere a) {							// exact compare, no epsilon
            return (origin.Compare(a.origin) && radius == a.radius);
        }

        public boolean Compare(final idSphere a, final float epsilon) {	// compare with epsilon
            return (origin.Compare(a.origin, epsilon) && idMath.Fabs(radius - a.radius) <= epsilon);
        }
//public	boolean			operator==(	final idSphere &a );						// exact compare, no epsilon
//public	boolean			operator!=(	final idSphere &a );						// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.origin);
            hash = 97 * hash + Float.floatToIntBits(this.radius);
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
            final idSphere other = (idSphere) obj;
            if (!Objects.equals(this.origin, other.origin)) {
                return false;
            }
            if (Float.floatToIntBits(this.radius) != Float.floatToIntBits(other.radius)) {
                return false;
            }
            return true;
        }

        public void Clear() {									// inside out sphere
            origin.Zero();
            radius = -1.0f;
        }

        public void Zero() {									// single point at origin
            origin.Zero();
            radius = 0.0f;
        }

        public void SetOrigin(final idVec3 o) {					// set origin of sphere
            origin = o;
        }

        public void SetRadius(final float r) {						// set square radius
            radius = r;
        }

        public final idVec3 GetOrigin() {						// returns origin of sphere
            return origin;
        }

        public float GetRadius() {						// returns sphere radius
            return radius;
        }

        public boolean IsCleared() {					// returns true if sphere is inside out
            return (radius < 0.0f);
        }

        public boolean AddPoint(final idVec3 p) {					// add the point, returns true if the sphere expanded 
            if (radius < 0.0f) {
                origin = p;
                radius = 0.0f;
                return true;
            } else {
                float r = (p.oMinus(origin)).LengthSqr();
                if (r > radius * radius) {
                    r = idMath.Sqrt(r);
                    origin.oPluSet((p.oMinus(origin)).oMultiply(0.5f).oMultiply(1.0f - radius / r));
                    radius += 0.5f * (r - radius);
                    return true;
                }
                return false;
            }
        }

        public boolean AddSphere(final idSphere s) {					// add the sphere, returns true if the sphere expanded
            if (radius < 0.0f) {
                origin = s.origin;
                radius = s.radius;
                return true;
            } else {
                float r = (s.origin.oMinus(origin)).LengthSqr();
                if (r > (radius + s.radius) * (radius + s.radius)) {
                    r = idMath.Sqrt(r);
                    origin.oPluSet((s.origin.oPlus(origin)).oMultiply(0.5f).oMultiply(1.0f - radius / (r + s.radius)));
                    radius += 0.5f * ((r + s.radius) - radius);
                    return true;
                }
                return false;
            }
        }

        public idSphere Expand(final float d) {					// return bounds expanded in all directions with the given value
            return new idSphere(origin, radius + d);
        }

        public idSphere ExpandSelf(final float d) {					// expand bounds in all directions with the given value
            radius += d;
            return this;
        }

        public idSphere Translate(final idVec3 translation) {
            return new idSphere(origin.oPlus(translation), radius);
        }

        public idSphere TranslateSelf(final idVec3 translation) {
            origin.oPluSet(translation);
            return this;
        }

        public float PlaneDistance(final idPlane plane) {
            float d;

            d = plane.Distance(origin);
            if (d > radius) {
                return d - radius;
            }
            if (d < -radius) {
                return d + radius;
            }
            return 0.0f;
        }

        public int PlaneSide(final idPlane plane) {
            return PlaneSide(plane, ON_EPSILON);
        }

        public int PlaneSide(final idPlane plane, final float epsilon) {
            float d;

            d = plane.Distance(origin);
            if (d > radius + epsilon) {
                return PLANESIDE_FRONT;
            }
            if (d < -radius - epsilon) {
                return PLANESIDE_BACK;
            }
            return PLANESIDE_CROSS;
        }

        public boolean ContainsPoint(final idVec3 p) {			// includes touching
            if ((p.oMinus(origin)).LengthSqr() > radius * radius) {
                return false;
            }
            return true;
        }

        public boolean IntersectsSphere(final idSphere s) {	// includes touching
            float r = s.radius + radius;
            if ((s.origin.oMinus(origin)).LengthSqr() > r * r) {
                return false;
            }
            return true;
        }

        /*
         ============
         idSphere::LineIntersection

         Returns true if the line intersects the sphere between the start and end point.
         ============
         */
        public boolean LineIntersection(final idVec3 start, final idVec3 end) {
            idVec3 r, s, e;
            float a;

            s = start.oMinus(origin);
            e = end.oMinus(origin);
            r = e.oMinus(s);
            a = s.oNegative().oMultiply(r);
            if (a <= 0) {
                return (s.oMultiply(s) < radius * radius);
            } else if (a >= r.oMultiply(r)) {
                return (e.oMultiply(e) < radius * radius);
            } else {
                r = s.oPlus(r.oMultiply(a / (r.oMultiply(r))));
                return (r.oMultiply(r) < radius * radius);
            }
        }

        /*
         ============
         idSphere::RayIntersection

         Returns true if the ray intersects the sphere.
         The ray can intersect the sphere in both directions from the start point.
         If start is inside the sphere then scale1 < 0 and scale2 > 0.
         ============
         */
        // intersection points are (start + dir * scale1) and (start + dir * scale2)
        public boolean RayIntersection(final idVec3 start, final idVec3 dir, float[] scale1, float[] scale2) {
            float a, b, c, d, sqrtd;
            idVec3 p;

            p = start.oMinus(origin);
            a = dir.oMultiply(dir);
            b = dir.oMultiply(p);
            c = p.oMultiply(p) - radius * radius;
            d = b * b - c * a;

            if (d < 0.0f) {
                return false;
            }

            sqrtd = idMath.Sqrt(d);
            a = 1.0f / a;

            scale1[0] = (-b + sqrtd) * a;
            scale2[0] = (-b - sqrtd) * a;

            return true;
        }

        /*
         ============
         idSphere::FromPoints

         Tight sphere for a point set.
         ============
         */
        // Tight sphere for a point set.
        public void FromPoints(final idVec3[] points, final int numPoints) {
            int i;
            float radiusSqr, dist;
            idVec3 mins = new idVec3(), maxs = new idVec3();

            SIMDProcessor.MinMax(mins, maxs, points, numPoints);

            origin = (mins.oPlus(maxs)).oMultiply(0.5f);

            radiusSqr = 0.0f;
            for (i = 0; i < numPoints; i++) {
                dist = (points[i].oMinus(origin)).LengthSqr();
                if (dist > radiusSqr) {
                    radiusSqr = dist;
                }
            }
            radius = idMath.Sqrt(radiusSqr);
        }

        // Most tight sphere for a translation.
        public void FromPointTranslation(final idVec3 point, final idVec3 translation) {
            origin = point.oPlus(translation.oMultiply(0.5f));
            radius = idMath.Sqrt(0.5f * translation.LengthSqr());
        }

        public void FromSphereTranslation(final idSphere sphere, final idVec3 start, final idVec3 translation) {
            origin = start.oPlus(sphere.origin).oPlus(translation.oMultiply(0.5f));
            radius = idMath.Sqrt(0.5f * translation.LengthSqr()) + sphere.radius;
        }

        // Most tight sphere for a rotation.
        public void FromPointRotation(final idVec3 point, final idRotation rotation) {
            idVec3 end = rotation.oMultiply(point);
            origin = (point.oPlus(end)).oMultiply(0.5f);
            radius = idMath.Sqrt(0.5f * (end.oMinus(point)).LengthSqr());
        }

        public void FromSphereRotation(final idSphere sphere, final idVec3 start, final idRotation rotation) {
            idVec3 end = rotation.oMultiply(sphere.origin);
            origin = start.oPlus(sphere.origin.oPlus(end)).oMultiply(0.5f);
            radius = idMath.Sqrt(0.5f * (end.oMinus(sphere.origin)).LengthSqr()) + sphere.radius;
        }

        public void AxisProjection(final idVec3 dir, float[] min, float[] max) {
            float d;
            d = dir.oMultiply(origin);
            min[0] = d - radius;
            max[0] = d + radius;
        }
    };
}
