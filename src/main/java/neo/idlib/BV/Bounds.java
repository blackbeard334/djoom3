package neo.idlib.BV;

import java.nio.ByteBuffer;
import java.util.Arrays;
import neo.TempDump.SERiAL;
import neo.idlib.BV.Sphere.idSphere;
import neo.idlib.Lib;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_CROSS;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Rotation.idRotation;
import neo.idlib.math.Simd;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Bounds {

    /*
     ===============================================================================

     Axis Aligned Bounding Box

     ===============================================================================
     */
    public static class idBounds implements SERiAL {
        public static final int BYTES = idVec3.BYTES * 2;

        private idVec3[] b = {new idVec3(), new idVec3()};
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count   = DBG_counter++;

        public idBounds() {
        }

        public idBounds(final idVec3 mins, final idVec3 maxs) {
            b[0] = mins;
            b[1] = maxs;
        }

        public idBounds(final idBounds bounds) {
            this.oSet(bounds);
        }

        public idBounds(final idVec3 point) {
            b[0].oSet(point);
            b[1].oSet(point);
        }

        public void set(final float v0, final float v1, final float v2, final float v3, final float v4, final float v5) {
            b[0] = new idVec3(v0, v1, v2);
            b[1] = new idVec3(v3, v4, v5);
        }

        public final void oSet(final idBounds bounds) {
            this.b[0].oSet(bounds.b[0]);
            this.b[1].oSet(bounds.b[1]);
        }
//
//public	final idVec3 	operator[]( final int index ) ;

        public idVec3 oGet(final int index) {
            return b[index];
        }

        public float oGet(final int index1, final int index2) {
            return b[index1].oGet(index2);
        }

        public idVec3 oSet(final int index, final idVec3 t) {
            return b[index].oSet(t);
        }

        public float oSet(final int x, final int y, final float value) {
            return b[x].oSet(y, value);
        }

        // returns translated bounds
        public idBounds oPlus(final idVec3 t) {
            return new idBounds(b[0].oPlus(t), b[1].oPlus(t));
        }

        // translate the bounds
        public idBounds oPluSet(final idVec3 t) {
            b[0].oPluSet(t);
            b[1].oPluSet(t);
            return this;
        }

        public idVec3 oPluSet(final int index, final idVec3 t) {
            return b[index].oPluSet(t);
        }

        // returns rotated bounds
        public idBounds oMultiply(final idMat3 r) {
            idBounds bounds = new idBounds();
            bounds.FromTransformedBounds(this, getVec3_origin(), r);
            return bounds;
        }

        // rotate the bounds
        public idBounds oMulSet(final idMat3 r) {
            this.FromTransformedBounds(this, getVec3_origin(), r);
            return this;
        }

        public idBounds oPlus(final idBounds a) {
            idBounds newBounds;
            newBounds = this;//TODO:oSet
            newBounds.AddBounds(a);
            return newBounds;
        }

        public idBounds oPluSet(final idBounds a) {
            this.AddBounds(a);
            return this;
        }

        public idBounds oMinus(final idBounds a) {
            assert (b[1].oGet(0) - b[0].oGet(0) > a.b[1].oGet(0) - a.b[0].oGet(0)
                    && b[1].oGet(1) - b[0].oGet(1) > a.b[1].oGet(1) - a.b[0].oGet(1)
                    && b[1].oGet(2) - b[0].oGet(2) > a.b[1].oGet(2) - a.b[0].oGet(2));
            return new idBounds(
                    new idVec3(b[0].oGet(0) + a.b[1].oGet(0), b[0].oGet(1) + a.b[1].oGet(1), b[0].oGet(2) + a.b[1].oGet(2)),
                    new idVec3(b[1].oGet(0) + a.b[0].oGet(0), b[1].oGet(1) + a.b[0].oGet(1), b[1].oGet(2) + a.b[0].oGet(2)));
        }

        public idBounds oMinSet(final idBounds a) {
            assert (b[1].oGet(0) - b[0].oGet(0) > a.b[1].oGet(0) - a.b[0].oGet(0)
                    && b[1].oGet(1) - b[0].oGet(1) > a.b[1].oGet(1) - a.b[0].oGet(1)
                    && b[1].oGet(2) - b[0].oGet(2) > a.b[1].oGet(2) - a.b[0].oGet(2));
            b[0].oPluSet(a.b[1]);
            b[1].oPluSet(a.b[0]);
            return this;
        }

        public idBounds oMinSet(final idVec3 t) {
            b[0].oMinSet(t);
            b[1].oMinSet(t);
            return this;
        }

        public idVec3 oMinSet(final int index, final idVec3 t) {
            return b[index].oMinSet(t);
        }

        public boolean Compare(final idBounds a) {							// exact compare, no epsilon
            return (b[0].Compare(a.b[0]) && b[1].Compare(a.b[1]));
        }

        public boolean Compare(final idBounds a, final float epsilon) {	// compare with epsilon
            return (b[0].Compare(a.b[0], epsilon) && b[1].Compare(a.b[1], epsilon));
        }
//public	boolean			operator==(	final idBounds a ) ;						// exact compare, no epsilon
//public	boolean			operator!=(	final idBounds a ) ;						// exact compare, no epsilon

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 11 * hash + Arrays.deepHashCode(this.b);
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
            final idBounds other = (idBounds) obj;
            if (!Arrays.deepEquals(this.b, other.b)) {
                return false;
            }
            return true;
        }

        // inside out bounds
        public void Clear() {
            b[0] = new idVec3(idMath.INFINITY, idMath.INFINITY, idMath.INFINITY);
            b[1] = new idVec3(-idMath.INFINITY, -idMath.INFINITY, -idMath.INFINITY);//TODO:set faster than new objects?
        }

        // single point at origin
        public void Zero() {
            b[0].x = b[0].y = b[0].z = b[1].x = b[1].y = b[1].z = 0;
        }

        // returns center of bounds
        public idVec3 GetCenter() {
            return new idVec3((b[1].oGet(0) + b[0].oGet(0)) * 0.5f, (b[1].oGet(1) + b[0].oGet(1)) * 0.5f, (b[1].oGet(2) + b[0].oGet(2)) * 0.5f);
        }

        // returns the radius relative to the bounds origin
        public float GetRadius() {
            int i;
            float total, b0, b1;

            total = 0.0f;
            for (i = 0; i < 3; i++) {
                b0 = idMath.Fabs(b[0].oGet(i));
                b1 = idMath.Fabs(b[1].oGet(i));
                if (b0 > b1) {
                    total += b0 * b0;
                } else {
                    total += b1 * b1;
                }
            }
            return idMath.Sqrt(total);
        }

        // returns the radius relative to the given center
        public float GetRadius(final idVec3 center) {
            int i;
            float total, b0, b1;

            total = 0.0f;
            for (i = 0; i < 3; i++) {
                b0 = (float) idMath.Fabs(center.oGet(i) - b[0].oGet(i));
                b1 = (float) idMath.Fabs(b[1].oGet(i) - center.oGet(i));
                if (b0 > b1) {
                    total += b0 * b0;
                } else {
                    total += b1 * b1;
                }
            }
            return idMath.Sqrt(total);
        }

        // returns the volume of the bounds
        public float GetVolume() {
            if (b[0].oGet(0) >= b[1].oGet(0) || b[0].oGet(1) >= b[1].oGet(1) || b[0].oGet(2) >= b[1].oGet(2)) {
                return 0.0f;
            }
            return ((b[1].oGet(0) - b[0].oGet(0)) * (b[1].oGet(1) - b[0].oGet(1)) * (b[1].oGet(2) - b[0].oGet(2)));
        }

        // returns true if bounds are inside out
        public boolean IsCleared() {
            return b[0].oGet(0) > b[1].oGet(0);
        }

        // add the point, returns true if the bounds expanded
        public boolean AddPoint(final idVec3 v) {
            boolean expanded = false;
            if (v.oGet(0) < b[0].oGet(0)) {
                b[0].oSet(0, v.oGet(0));
                expanded = true;
            }
            if (v.oGet(0) > b[1].oGet(0)) {
                b[1].oSet(0, v.oGet(0));
                expanded = true;
            }
            if (v.oGet(1) < b[0].oGet(1)) {
                b[0].oSet(1, v.oGet(1));
                expanded = true;
            }
            if (v.oGet(1) > b[1].oGet(1)) {
                b[1].oSet(1, v.oGet(1));
                expanded = true;
            }
            if (v.oGet(2) < b[0].oGet(2)) {
                b[0].oSet(2, v.oGet(2));
                expanded = true;
            }
            if (v.oGet(2) > b[1].oGet(2)) {
                b[1].oSet(2, v.oGet(2));
                expanded = true;
            }
            return expanded;
        }

        // add the bounds, returns true if the bounds expanded
        public boolean AddBounds(final idBounds a) {
            boolean expanded = false;
            if (a.b[0].oGet(0) < b[0].oGet(0)) {
                b[0].oSet(0, a.b[0].oGet(0));
                expanded = true;
            }
            if (a.b[0].oGet(1) < b[0].oGet(1)) {
                b[0].oSet(1, a.b[0].oGet(1));
                expanded = true;
            }
            if (a.b[0].oGet(2) < b[0].oGet(2)) {
                b[0].oSet(2, a.b[0].oGet(2));
                expanded = true;
            }
            if (a.b[1].oGet(0) > b[1].oGet(0)) {
                b[1].oSet(0, a.b[1].oGet(0));
                expanded = true;
            }
            if (a.b[1].oGet(1) > b[1].oGet(1)) {
                b[1].oSet(1, a.b[1].oGet(1));
                expanded = true;
            }
            if (a.b[1].oGet(2) > b[1].oGet(2)) {
                b[1].oSet(2, a.b[1].oGet(2));
                expanded = true;
            }
            return expanded;
        }

        // return intersection of this bounds with the given bounds
        public idBounds Intersect(final idBounds a) {
            idBounds n = new idBounds();
            n.b[0].oSet(0, (a.b[0].oGet(0) > b[0].oGet(0)) ? a.b[0].oGet(0) : b[0].oGet(0));
            n.b[0].oSet(1, (a.b[0].oGet(1) > b[0].oGet(1)) ? a.b[0].oGet(1) : b[0].oGet(1));
            n.b[0].oSet(2, (a.b[0].oGet(2) > b[0].oGet(2)) ? a.b[0].oGet(2) : b[0].oGet(2));
            n.b[1].oSet(0, (a.b[1].oGet(0) < b[1].oGet(0)) ? a.b[1].oGet(0) : b[1].oGet(0));
            n.b[1].oSet(1, (a.b[1].oGet(1) < b[1].oGet(1)) ? a.b[1].oGet(1) : b[1].oGet(1));
            n.b[1].oSet(2, (a.b[1].oGet(2) < b[1].oGet(2)) ? a.b[1].oGet(2) : b[1].oGet(2));
            return n;
        }

        // intersect this bounds with the given bounds
        public idBounds IntersectSelf(final idBounds a) {
            if (a.b[0].oGet(0) > b[0].oGet(0)) {
                b[0].oSet(0, a.b[0].oGet(0));
            }
            if (a.b[0].oGet(1) > b[0].oGet(1)) {
                b[0].oSet(1, a.b[0].oGet(1));
            }
            if (a.b[0].oGet(2) > b[0].oGet(2)) {
                b[0].oSet(2, a.b[0].oGet(2));
            }
            if (a.b[1].oGet(0) < b[1].oGet(0)) {
                b[1].oSet(0, a.b[1].oGet(0));
            }
            if (a.b[1].oGet(1) < b[1].oGet(1)) {
                b[1].oSet(1, a.b[1].oGet(1));
            }
            if (a.b[1].oGet(2) < b[1].oGet(2)) {
                b[1].oSet(2, a.b[1].oGet(2));
            }
            return this;
        }

        /**
         * @return bounds expanded in all directions with the given value
         */
        public idBounds Expand(final float d) {
            return new idBounds(
                    new idVec3(b[0].oGet(0) - d, b[0].oGet(1) - d, b[0].oGet(2) - d),
                    new idVec3(b[1].oGet(0) + d, b[1].oGet(1) + d, b[1].oGet(2) + d));
        }

        /**
         * expand bounds in all directions with the given value
         */
        public idBounds ExpandSelf(final float d) {
            b[0].oMinSet(new idVec3(d, d, d));
            b[1].x += d;
            b[1].y += d;
            b[1].z += d;
            return this;
        }

        public idBounds Translate(final idVec3 translation) {// return translated bounds
            return new idBounds(b[0].oPlus(translation), b[1].oPlus(translation));
        }

        // translate this bounds
        public idBounds TranslateSelf(final idVec3 translation) {
            b[0].oPluSet(translation);
            b[1].oPluSet(translation);
            return this;
        }

        // return rotated bounds
        public idBounds Rotate(final idMat3 rotation) {
            idBounds bounds = new idBounds();
            bounds.FromTransformedBounds(this, getVec3_origin(), rotation);
            return bounds;
        }

        // rotate this bounds
        public idBounds RotateSelf(final idMat3 rotation) {
            FromTransformedBounds(this, getVec3_origin(), rotation);
            return this;
        }

        public float PlaneDistance(final idPlane plane) {
            idVec3 center;
            float d1, d2;

            center = (b[0].oPlus(b[1])).oMultiply(0.5f);

            d1 = plane.Distance(center);
            d2 = idMath.Fabs((b[1].oGet(0) - center.oGet(0)) * plane.Normal().oGet(0))
                    + idMath.Fabs((b[1].oGet(1) - center.oGet(1)) * plane.Normal().oGet(1))
                    + idMath.Fabs((b[1].oGet(2) - center.oGet(2)) * plane.Normal().oGet(2));

            if (d1 - d2 > 0.0f) {
                return d1 - d2;
            }
            if (d1 + d2 < 0.0f) {
                return d1 + d2;
            }
            return 0.0f;
        }

        public int PlaneSide(final idPlane plane) {
            return PlaneSide(plane, ON_EPSILON);
        }

        public int PlaneSide(final idPlane plane, final float epsilon) {
            idVec3 center;
            float d1, d2;

            center = (b[0].oPlus(b[1])).oMultiply(0.5f);

            d1 = plane.Distance(center);
            d2 = idMath.Fabs((b[1].oGet(0) - center.oGet(0)) * plane.Normal().oGet(0))
                    + idMath.Fabs((b[1].oGet(1) - center.oGet(1)) * plane.Normal().oGet(1))
                    + idMath.Fabs((b[1].oGet(2) - center.oGet(2)) * plane.Normal().oGet(2));

            if (d1 - d2 > epsilon) {
                return PLANESIDE_FRONT;
            }
            if (d1 + d2 < -epsilon) {
                return PLANESIDE_BACK;
            }
            return PLANESIDE_CROSS;
        }

        // includes touching
        public boolean ContainsPoint(final idVec3 p) {
            if (p.oGet(0) < b[0].oGet(0) || p.oGet(1) < b[0].oGet(1) || p.oGet(2) < b[0].oGet(2)
                    || p.oGet(0) > b[1].oGet(0) || p.oGet(1) > b[1].oGet(1) || p.oGet(2) > b[1].oGet(2)) {
                return false;
            }
            return true;
        }

        // includes touching
        public boolean IntersectsBounds(final idBounds a) {
            if (a.b[1].oGet(0) < b[0].oGet(0) || a.b[1].oGet(1) < b[0].oGet(1) || a.b[1].oGet(2) < b[0].oGet(2)
                    || a.b[0].oGet(0) > b[1].oGet(0) || a.b[0].oGet(1) > b[1].oGet(1) || a.b[0].oGet(2) > b[1].oGet(2)) {
                return false;
            }
            return true;
        }

        /*
         ============
         idBounds::LineIntersection

         Returns true if the line intersects the bounds between the start and end point.
         ============
         */
        public boolean LineIntersection(final idVec3 start, final idVec3 end) {
            float[] ld = new float[3];
            idVec3 center = (b[0].oPlus(b[1])).oMultiply(0.5f);
            idVec3 extents = b[1].oMinus(center);
            idVec3 lineDir = (end.oMinus(start)).oMultiply(0.5f);
            idVec3 lineCenter = start.oPlus(lineDir);
            idVec3 dir = lineCenter.oMinus(center);

            ld[0] = idMath.Fabs(lineDir.oGet(0));
            if (idMath.Fabs(dir.oGet(0)) > extents.oGet(0) + ld[0]) {
                return false;
            }

            ld[1] = idMath.Fabs(lineDir.oGet(1));
            if (idMath.Fabs(dir.oGet(1)) > extents.oGet(1) + ld[1]) {
                return false;
            }

            ld[2] = idMath.Fabs(lineDir.oGet(2));
            if (idMath.Fabs(dir.oGet(2)) > extents.oGet(2) + ld[2]) {
                return false;
            }

            idVec3 cross = lineDir.Cross(dir);

            if (idMath.Fabs(cross.oGet(0)) > extents.oGet(1) * ld[2] + extents.oGet(2) * ld[1]) {
                return false;
            }

            if (idMath.Fabs(cross.oGet(1)) > extents.oGet(0) * ld[2] + extents.oGet(2) * ld[0]) {
                return false;
            }

            if (idMath.Fabs(cross.oGet(2)) > extents.oGet(0) * ld[1] + extents.oGet(1) * ld[0]) {
                return false;
            }

            return true;
        }

        /*
         ============
         idBounds::RayIntersection

         Returns true if the ray intersects the bounds.
         The ray can intersect the bounds in both directions from the start point.
         If start is inside the bounds it is considered an intersection with scale = 0
         ============
         */
        public boolean RayIntersection(final idVec3 start, final idVec3 dir, float scale[]) {// intersection point is start + dir * scale
            int i, ax0, ax1, ax2, side, inside;
            float f;
            idVec3 hit = new idVec3();

            ax0 = -1;
            inside = 0;
            for (i = 0; i < 3; i++) {
                if (start.oGet(i) < b[0].oGet(i)) {
                    side = 0;
                } else if (start.oGet(i) > b[1].oGet(i)) {
                    side = 1;
                } else {
                    inside++;
                    continue;
                }
                if (dir.oGet(i) == 0.0f) {
                    continue;
                }
                f = (start.oGet(i) - b[side].oGet(i));
                if (ax0 < 0 || idMath.Fabs(f) > idMath.Fabs(scale[0] * dir.oGet(i))) {
                    scale[0] = -(f / dir.oGet(i));
                    ax0 = i;
                }
            }

            if (ax0 < 0) {
                scale[0] = 0.0f;//TODO:should scale have a backreference?
                // return true if the start point is inside the bounds
                return (inside == 3);
            }

            ax1 = (ax0 + 1) % 3;
            ax2 = (ax0 + 2) % 3;
            hit.oSet(ax1, start.oGet(ax1) + scale[0] * dir.oGet(ax1));
            hit.oSet(ax2, start.oGet(ax2) + scale[0] * dir.oGet(ax2));

            return (hit.oGet(ax1) >= b[0].oGet(ax1) && hit.oGet(ax1) <= b[1].oGet(ax1)
                    && hit.oGet(ax2) >= b[0].oGet(ax2) && hit.oGet(ax2) <= b[1].oGet(ax2));
        }

        // most tight bounds for the given transformed bounds
        public void FromTransformedBounds(final idBounds bounds, final idVec3 origin, final idMat3 axis) {
            int i;
            idVec3 center, extents, rotatedExtents = new idVec3();

            center = bounds.oGet(0).oPlus(bounds.oGet(1)).oMultiply(0.5f);
            extents = bounds.oGet(1).oMinus(center);

            for (i = 0; i < 3; i++) {
                rotatedExtents.oSet(i, idMath.Fabs(extents.oGet(0) * axis.oGet(0).oGet(i))
                        + idMath.Fabs(extents.oGet(1) * axis.oGet(1).oGet(i))
                        + idMath.Fabs(extents.oGet(2) * axis.oGet(2).oGet(i)));
            }

            center = origin.oPlus(axis.oMultiply(center));
            b[0] = center.oMinus(rotatedExtents);
            b[1] = center.oPlus(rotatedExtents);
        }

        /*
         ============
         idBounds::FromPoints

         Most tight bounds for a point set.
         ============
         */
        public void FromPoints(final idVec3[] points, final int numPoints) {// most tight bounds for a point set
            Simd.SIMDProcessor.MinMax(b[0], b[1], points, numPoints);
        }

        /*
         ============
         idBounds::FromPointTranslation

         Most tight bounds for the translational movement of the given point.
         ============
         */
        public void FromPointTranslation(final idVec3 point, final idVec3 translation) {// most tight bounds for a translation
            int i;

            for (i = 0; i < 3; i++) {
                if (translation.oGet(i) < 0.0f) {
                    b[0].oSet(i, point.oGet(i) + translation.oGet(i));
                    b[1].oSet(i, point.oGet(i));
                } else {
                    b[0].oSet(i, point.oGet(i));
                    b[1].oSet(i, point.oGet(i) + translation.oGet(i));
                }
            }
        }

        /*
         ============
         idBounds::FromBoundsTranslation

         Most tight bounds for the translational movement of the given bounds.
         ============
         */
        public void FromBoundsTranslation(final idBounds bounds, final idVec3 origin, final idMat3 axis, final idVec3 translation) {
            int i;

            if (axis.IsRotated()) {
                FromTransformedBounds(bounds, origin, axis);
            } else {
                b[0] = bounds.oGet(0).oPlus(origin);
                b[1] = bounds.oGet(1).oPlus(origin);
            }
            for (i = 0; i < 3; i++) {
                if (translation.oGet(i) < 0.0f) {
                    b[0].oPluSet(i, translation.oGet(i));
                } else {
                    b[1].oPluSet(i, translation.oGet(i));
                }
            }
        }

        /*
         ============
         idBounds::FromPointRotation

         Most tight bounds for the rotational movement of the given point.
         ============
         */
        public void FromPointRotation(final idVec3 point, final idRotation rotation) {// most tight bounds for a rotation
            float radius;

            if (idMath.Fabs(rotation.GetAngle()) < 180.0f) {
                BoundsForPointRotation(point, rotation);
            } else {

                radius = (point.oMinus(rotation.GetOrigin())).Length();

                // FIXME: these bounds are usually way larger
                b[0].Set(-radius, -radius, -radius);
                b[1].Set(radius, radius, radius);
            }
        }

        /*
         ============
         idBounds::FromBoundsRotation

         Most tight bounds for the rotational movement of the given bounds.
         ============
         */
        public void FromBoundsRotation(final idBounds bounds, final idVec3 origin, final idMat3 axis, final idRotation rotation) {
            int i;
            float radius;
            idVec3 point = new idVec3();
            idBounds rBounds;

            if (idMath.Fabs(rotation.GetAngle()) < 180.0f) {

                this.b = BoundsForPointRotation(axis.oMultiply(bounds.oGet(0)).oPlus(origin), rotation).b;//TODO:check if function output is gargbage collected
                for (i = 1; i < 8; i++) {
                    point.oSet(0, bounds.oGet((i ^ (i >> 1)) & 1).oGet(0));
                    point.oSet(0, bounds.oGet((i >> 1) & 1).oGet(1));
                    point.oSet(0, bounds.oGet((i >> 2) & 1).oGet(2));
                    this.b = BoundsForPointRotation(axis.oMultiply(point).oPlus(origin), rotation).b;
                }
            } else {

                point = (bounds.oGet(1).oMinus(bounds.oGet(0))).oMultiply(0.5f);
                radius = (bounds.oGet(1).oMinus(point)).Length() + (point.oMinus(rotation.GetOrigin())).Length();

                // FIXME: these bounds are usually way larger
                b[0].Set(-radius, -radius, -radius);
                b[1].Set(radius, radius, radius);
            }
        }

        public void ToPoints(idVec3 points[]) {
            for (int i = 0; i < 8; i++) {
                points[i].oSet(0, b[(i ^ (i >> 1)) & 1].oGet(0));
                points[i].oSet(1, b[(i >> 1) & 1].oGet(1));
                points[i].oSet(2, b[(i >> 2) & 1].oGet(2));
            }
        }

        public idSphere ToSphere() {
            idSphere sphere = new idSphere();
            sphere.SetOrigin((b[0].oPlus(b[1])).oMultiply(0.5f));
            sphere.SetRadius((b[1].oMinus(sphere.GetOrigin())).Length());
            return sphere;
        }

        public void AxisProjection(final idVec3 dir, float[] min, float[] max) {
            float d1, d2;
            idVec3 center, extents;

            center = (b[0].oPlus(b[1])).oMultiply(0.5f);
            extents = b[1].oMinus(center);

            d1 = dir.oMultiply(center);
            d2 = idMath.Fabs(extents.oGet(0) * dir.oGet(0))
                    + idMath.Fabs(extents.oGet(1) * dir.oGet(1))
                    + idMath.Fabs(extents.oGet(2) * dir.oGet(2));

            min[0] = d1 - d2;
            max[0] = d1 + d2;
        }

        public void AxisProjection(final idVec3 origin, final idMat3 axis, final idVec3 dir, float[] min, float[] max) {
            float d1, d2;
            idVec3 center, extents;

            center = (b[0].oPlus(b[1])).oMultiply(0.5f);
            extents = b[1].oMinus(center);
            center = origin.oPlus(axis.oMultiply(center));

            d1 = dir.oMultiply(center);
            d2 = idMath.Fabs(extents.oGet(0) * (dir.oMultiply(axis.oGet(0))))
                    + idMath.Fabs(extents.oGet(1) * (dir.oMultiply(axis.oGet(1))))
                    + idMath.Fabs(extents.oGet(2) * (dir.oMultiply(axis.oGet(2))));

            min[0] = d1 - d2;
            max[0] = d1 + d2;
        }

        @Override
        public String toString() {
            return Arrays.toString(b);
        }

        @Override
        public ByteBuffer AllocBuffer() {
            return ByteBuffer.allocate(idBounds.BYTES);
        }

        @Override
        public void Read(ByteBuffer buffer) {
            b[0].Read(buffer);
            b[1].Read(buffer);
        }

        @Override
        public ByteBuffer Write() {
            ByteBuffer buffer = AllocBuffer();
            buffer.put(b[0].Write()).put(b[1].Write()).flip();

            return buffer;
        }
    };

    /*
     ================
     BoundsForPointRotation

     only for rotations < 180 degrees
     ================
     */
    static idBounds BoundsForPointRotation(final idVec3 start, final idRotation rotation) {
        int i;
        float radiusSqr;
        idVec3 v1, v2;
        idVec3 origin, axis, end;
        idBounds bounds = new idBounds();

        end = rotation.oMultiply(start);
        axis = rotation.GetVec();
        origin = rotation.GetOrigin().oPlus(axis.oMultiply(axis.oMultiply((start.oMinus(rotation.GetOrigin())))));
        radiusSqr = (start.oPlus(origin)).LengthSqr();
        v1 = (start.oMinus(origin)).Cross(axis);
        v2 = (end.oMinus(origin)).Cross(axis);

        for (i = 0; i < 3; i++) {
            // if the derivative changes sign along this axis during the rotation from start to end
            if ((v1.oGet(i) > 0.0f && v2.oGet(i) < 0.0f) || (v1.oGet(i) < 0.0f && v2.oGet(i) > 0.0f)) {
                if ((0.5f * (start.oGet(i) + end.oGet(i)) - origin.oGet(i)) > 0.0f) {
                    bounds.oSet(0, i, (float) Lib.Min(start.oGet(i), end.oGet(i)));
                    bounds.oSet(1, i, origin.oGet(i) + idMath.Sqrt(radiusSqr * (1.0f - axis.oGet(i) * axis.oGet(i))));
                } else {
                    bounds.oSet(0, i, origin.oGet(i) - idMath.Sqrt(radiusSqr * (1.0f - axis.oGet(i) * axis.oGet(i))));
                    bounds.oSet(1, i, (float) Lib.Max(start.oGet(i), end.oGet(i)));
                }
            } else if (start.oGet(i) > end.oGet(i)) {
                bounds.oSet(0, i, end.oGet(i));
                bounds.oSet(1, i, start.oGet(i));
            } else {
                bounds.oSet(0, i, start.oGet(i));
                bounds.oSet(1, i, end.oGet(i));
            }
        }

        return bounds;
    }
    public static idBounds bounds_zero;
}
