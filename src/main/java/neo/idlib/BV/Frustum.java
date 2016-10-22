package neo.idlib.BV;

import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Box.idBox;
import neo.idlib.BV.Sphere.idSphere;
import neo.idlib.Lib;
import static neo.idlib.Lib.Max;
import static neo.idlib.Lib.Min;
import static neo.idlib.containers.List.idSwap;
import neo.idlib.geometry.Winding.idWinding;
import static neo.idlib.math.Math_h.FLOATNOTZERO;
import static neo.idlib.math.Math_h.FLOATSIGNBITNOTSET;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_CROSS;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Frustum {

    /*
     bit 0 = min x
     bit 1 = max x
     bit 2 = min y
     bit 3 = max y
     bit 4 = min z
     bit 5 = max z
     */
    private static final int[] boxVertPlanes = {
        ((1 << 0) | (1 << 2) | (1 << 4)),
        ((1 << 1) | (1 << 2) | (1 << 4)),
        ((1 << 1) | (1 << 3) | (1 << 4)),
        ((1 << 0) | (1 << 3) | (1 << 4)),
        ((1 << 0) | (1 << 2) | (1 << 5)),
        ((1 << 1) | (1 << 2) | (1 << 5)),
        ((1 << 1) | (1 << 3) | (1 << 5)),
        ((1 << 0) | (1 << 3) | (1 << 5)),};

    /*
     ============
     BoxToPoints
     ============
     */
    private static void BoxToPoints(final idVec3 center, final idVec3 extents, final idMat3 axis, idVec3[] points) {
        idMat3 ax = new idMat3();
        idVec3[] temp = new idVec3[4];

        ax.oSet(0, axis.oGet(0).oMultiply(extents.oGet(0)));
        ax.oSet(1, axis.oGet(1).oMultiply(extents.oGet(1)));
        ax.oSet(2, axis.oGet(2).oMultiply(extents.oGet(2)));
        temp[0] = center.oMinus(ax.oGet(0));
        temp[1] = center.oPlus(ax.oGet(0));
        temp[2] = ax.oGet(1).oMinus(ax.oGet(2));
        temp[3] = ax.oGet(1).oPlus(ax.oGet(2));
        points[0] = temp[0].oMinus(temp[3]);
        points[1] = temp[1].oMinus(temp[3]);
        points[2] = temp[1].oPlus(temp[2]);
        points[3] = temp[0].oPlus(temp[2]);
        points[4] = temp[0].oMinus(temp[2]);
        points[5] = temp[1].oMinus(temp[2]);
        points[6] = temp[1].oPlus(temp[3]);
        points[7] = temp[0].oPlus(temp[3]);
    }

    /*
     ===============================================================================

     Orthogonal Frustum

     ===============================================================================
     */
    public static class idFrustum {

        private idVec3 origin;      // frustum origin
        private idMat3 axis;        // frustum orientation
        private float  dNear;       // distance of near plane, dNear >= 0.0f
        private float  dFar;        // distance of far plane, dFar > dNear
        private float  dLeft;       // half the width at the far plane
        private float  dUp;         // half the height at the far plane
        private float  invFar;      // 1.0f / dFar
        //
        //

        public idFrustum() {
            origin = new idVec3();
            axis = new idMat3();
            dNear = dFar = 0.0f;
        }

        public idFrustum(idFrustum f) {
            this.origin = new idVec3(f.origin);
            this.axis = new idMat3(f.axis);
            this.dNear = f.dNear;
            this.dFar = f.dFar;
            this.dLeft = f.dLeft;
            this.dUp = f.dUp;
            this.invFar = f.invFar;
        }

        public void SetOrigin(final idVec3 origin) {
            this.origin = new idVec3(origin);
        }

        public void SetAxis(final idMat3 axis) {
            this.axis = new idMat3(axis);
        }

        public void SetSize(float dNear, float dFar, float dLeft, float dUp) {
            assert (dNear >= 0.0f && dFar > dNear && dLeft > 0.0f && dUp > 0.0f);
            this.dNear = dNear;
            this.dFar = dFar;
            this.dLeft = dLeft;
            this.dUp = dUp;
            this.invFar = 1.0f / dFar;
        }

        public void SetPyramid(float dNear, float dFar) {
            assert (dNear >= 0.0f && dFar > dNear);
            this.dNear = dNear;
            this.dFar = dFar;
            this.dLeft = dFar;
            this.dUp = dFar;
            this.invFar = 1.0f / dFar;
        }

        public void MoveNearDistance(float dNear) {
            assert (dNear >= 0.0f);
            this.dNear = dNear;
        }

        public void MoveFarDistance(float dFar) {
            assert (dFar > this.dNear);
            float scale = dFar / this.dFar;
            this.dFar = dFar;
            this.dLeft *= scale;
            this.dUp *= scale;
            this.invFar = 1.0f / dFar;
        }
//

        public final idVec3 GetOrigin() {						// returns frustum origin
            return origin;
        }

        public final idMat3 GetAxis() {							// returns frustum orientation
            return axis;
        }

        public idVec3 GetCenter() {						// returns center of frustum
            return origin.oPlus(axis.oGet(0).oMultiply((dFar - dNear) * 0.5f));
        }
//

        public boolean IsValid() {							// returns true if the frustum is valid
            return (dFar > dNear);
        }

        public float GetNearDistance() {					// returns distance to near plane
            return dNear;
        }

        public float GetFarDistance() {					// returns distance to far plane
            return dFar;
        }

        public float GetLeft() {							// returns left vector length
            return dLeft;
        }

        public float GetUp() {							// returns up vector length
            return dUp;
        }
//

        public idFrustum Expand(final float d) {					// returns frustum expanded in all directions with the given value
            idFrustum f = new idFrustum(this);//TODO:oSET
            f.origin.oMinSet(f.axis.oGet(0).oMultiply(d));
            f.dFar += 2.0f * d;
            f.dLeft = f.dFar * dLeft * invFar;
            f.dUp = f.dFar * dUp * invFar;
            f.invFar = 1.0f / dFar;
            return f;
        }

        public idFrustum ExpandSelf(final float d) {					// expands frustum in all directions with the given value
            origin.oMinSet(axis.oGet(0).oMultiply(d));
            dFar += 2.0f * d;
            dLeft = dFar * dLeft * invFar;
            dUp = dFar * dUp * invFar;
            invFar = 1.0f / dFar;
            return this;
        }

        public idFrustum Translate(final idVec3 translation) {	// returns translated frustum
            idFrustum f = new idFrustum(this);
            f.origin.oPluSet(translation);
            return f;
        }

        public idFrustum TranslateSelf(final idVec3 translation) {		// translates frustum
            origin.oPluSet(translation);
            return this;
        }

        public idFrustum Rotate(final idMat3 rotation) {			// returns rotated frustum
            idFrustum f = new idFrustum(this);
            f.axis.oMulSet(rotation);
            return f;
        }

        public idFrustum RotateSelf(final idMat3 rotation) {			// rotates frustum
            axis.oMulSet(rotation);
            return this;
        }
//

        public float PlaneDistance(final idPlane plane) {
            float[] min = new float[1], max = new float[1];

            AxisProjection(plane.Normal(), min, max);
            if (min[0] + plane.oGet(3) > 0.0f) {
                return min[0] + plane.oGet(0);
            }
            if (max[0] + plane.oGet(3) < 0.0f) {
                return max[0] + plane.oGet(3);
            }
            return 0.0f;
        }

        public int PlaneSide(final idPlane plane) {
            return PlaneSide(plane, ON_EPSILON);
        }

        public int PlaneSide(final idPlane plane, final float epsilon) {
            float[] min = new float[1], max = new float[1];

            AxisProjection(plane.Normal(), min, max);
            if (min[0] + plane.oGet(3) > epsilon) {
                return PLANESIDE_FRONT;
            }
            if (max[0] + plane.oGet(3) < epsilon) {
                return PLANESIDE_BACK;
            }
            return PLANESIDE_CROSS;
        }
//

        // fast culling but might not cull everything outside the frustum
        public boolean CullPoint(final idVec3 point) {
            idVec3 p;
            float scale;

            // transform point to frustum space
            p = (point.oMinus(origin)).oMultiply(axis.Transpose());
            // test whether or not the point is within the frustum
            if (p.x < dNear || p.x > dFar) {
                return true;
            }
            scale = p.x * invFar;
            if (idMath.Fabs(p.y) > dLeft * scale) {
                return true;
            }
            if (idMath.Fabs(p.z) > dUp * scale) {
                return true;
            }
            return false;
        }

        /*
         ============
         idFrustum::CullBounds

         Tests if any of the planes of the frustum can be used as a separating plane.

         24 muls best case
         37 muls worst case
         ============
         */
        public boolean CullBounds(final idBounds bounds) {
            idVec3 localOrigin, center, extents;
            idMat3 localAxis;

            center = (bounds.oGet(0).oPlus(bounds.oGet(1))).oMultiply(0.5f);
            extents = bounds.oGet(1).oMinus(center);

            // transform the bounds into the space of this frustum
            localOrigin = (center.oMinus(origin)).oMultiply(axis.Transpose());
            localAxis = axis.Transpose();

            return CullLocalBox(localOrigin, extents, localAxis);
        }

        /*
         ============
         idFrustum::CullBox

         Tests if any of the planes of the frustum can be used as a separating plane.

         39 muls best case
         61 muls worst case
         ============
         */
        public boolean CullBox(final idBox box) {
            idVec3 localOrigin;
            idMat3 localAxis;

            // transform the box into the space of this frustum
            localOrigin = (box.GetCenter().oMinus(origin)).oMultiply(axis.Transpose());
            localAxis = box.GetAxis().oMultiply(axis.Transpose());

            return CullLocalBox(localOrigin, box.GetExtents(), localAxis);
        }

        /*
         ============
         idFrustum::CullSphere

         Tests if any of the planes of the frustum can be used as a separating plane.

         9 muls best case
         21 muls worst case
         ============
         */
        public boolean CullSphere(final idSphere sphere) {
            float d, r, rs, sFar;
            idVec3 center;

            center = (sphere.GetOrigin().oMinus(origin)).oMultiply(axis.Transpose());
            r = sphere.GetRadius();

            // test near plane
            if (dNear - center.x > r) {
                return true;
            }

            // test far plane
            if (center.x - dFar > r) {
                return true;
            }

            rs = r * r;
            sFar = dFar * dFar;

            // test left/right planes
            d = dFar * idMath.Fabs(center.y) - dLeft * center.x;
            if ((d * d) > rs * (sFar + dLeft * dLeft)) {
                return true;
            }

            // test up/down planes
            d = dFar * idMath.Fabs(center.z) - dUp * center.x;
            if ((d * d) > rs * (sFar + dUp * dUp)) {
                return true;
            }

            return false;
        }

        /*
         ============
         idFrustum::CullFrustum

         Tests if any of the planes of this frustum can be used as a separating plane.

         58 muls best case
         88 muls worst case
         ============
         */
        public boolean CullFrustum(final idFrustum frustum) {
            idFrustum localFrustum;
            idVec3[] indexPoints = new idVec3[8], cornerVecs = new idVec3[4];

            // transform the given frustum into the space of this frustum
            localFrustum = frustum;
            localFrustum.origin = (frustum.origin.oMinus(origin)).oMultiply(axis.Transpose());
            localFrustum.axis = frustum.axis.oMultiply(axis.Transpose());

            localFrustum.ToIndexPointsAndCornerVecs(indexPoints, cornerVecs);

            return CullLocalFrustum(localFrustum, indexPoints, cornerVecs);
        }

        public boolean CullWinding(final idWinding winding) {
            int i;
            int[] pointCull;
            idVec3[] localPoints;
            idMat3 transpose;

            localPoints = new idVec3[winding.GetNumPoints()];
            pointCull = new int[winding.GetNumPoints()];

            transpose = axis.Transpose();
            for (i = 0; i < winding.GetNumPoints(); i++) {
                localPoints[i] = (winding.oGet(i).ToVec3().oMinus(origin)).oMultiply(transpose);
            }

            return CullLocalWinding(localPoints, winding.GetNumPoints(), pointCull);
        }
//

        // exact intersection tests
        public boolean ContainsPoint(final idVec3 point) {
            return !CullPoint(point);
        }

        public boolean IntersectsBounds(final idBounds bounds) {
            idVec3 localOrigin, center, extents;
            idMat3 localAxis;

            center = (bounds.oGet(0).oPlus(bounds.oGet(1))).oMultiply(0.5f);
            extents = bounds.oGet(1).oMinus(center);

            localOrigin = (center.oMinus(origin)).oMultiply(axis.Transpose());
            localAxis = axis.Transpose();

            if (CullLocalBox(localOrigin, extents, localAxis)) {
                return false;
            }

            idVec3[] indexPoints = new idVec3[8], cornerVecs = new idVec3[4];

            ToIndexPointsAndCornerVecs(indexPoints, cornerVecs);

            if (BoundsCullLocalFrustum(bounds, this, indexPoints, cornerVecs)) {
                return false;
            }

            idSwap(indexPoints, indexPoints, 2, 3);
            idSwap(indexPoints, indexPoints, 6, 7);

            if (LocalFrustumIntersectsBounds(indexPoints, bounds)) {
                return true;
            }

            BoxToPoints(localOrigin, extents, localAxis, indexPoints);

            if (LocalFrustumIntersectsFrustum(indexPoints, true)) {
                return true;
            }

            return false;
        }

        public boolean IntersectsBox(final idBox box) {
            idVec3 localOrigin;
            idMat3 localAxis;

            localOrigin = (box.GetCenter().oMinus(origin)).oMultiply(axis.Transpose());
            localAxis = box.GetAxis().oMultiply(axis.Transpose());

            if (CullLocalBox(localOrigin, box.GetExtents(), localAxis)) {
                return false;
            }

            idVec3[] indexPoints = new idVec3[8], cornerVecs = new idVec3[4];
            idFrustum localFrustum;

            localFrustum = this;//TODO:SET
            localFrustum.origin = (origin.oMinus(box.GetCenter())).oMultiply(box.GetAxis().Transpose());
            localFrustum.axis = axis.oMultiply(box.GetAxis().Transpose());

            localFrustum.ToIndexPointsAndCornerVecs(indexPoints, cornerVecs);

            if (BoundsCullLocalFrustum(new idBounds(box.GetExtents().oNegative(), box.GetExtents()), localFrustum, indexPoints, cornerVecs)) {
                return false;
            }

            idSwap(indexPoints, indexPoints, 2, 3);
            idSwap(indexPoints, indexPoints, 6, 7);

            if (LocalFrustumIntersectsBounds(indexPoints, new idBounds(box.GetExtents().oNegative(), box.GetExtents()))) {
                return true;
            }

            BoxToPoints(localOrigin, box.GetExtents(), localAxis, indexPoints);

            if (LocalFrustumIntersectsFrustum(indexPoints, true)) {
                return true;
            }

            return false;
        }

        private int VORONOI_INDEX(int x, int y, int z) {
            return (x + y * 3 + z * 9);
        }
        private static final int VORONOI_INDEX_0_0_0 = (0 + 0 * 3 + 0 * 9), VORONOI_INDEX_1_0_0 = (1 + 0 * 3 + 0 * 9), VORONOI_INDEX_2_0_0 = (2 + 0 * 3 + 0 * 9),
                VORONOI_INDEX_0_1_0 = (0 + 1 * 3 + 0 * 9), VORONOI_INDEX_0_2_0 = (0 + 2 * 3 + 0 * 9), VORONOI_INDEX_0_0_1 = (0 + 0 * 3 + 1 * 9),
                VORONOI_INDEX_0_0_2 = (0 + 0 * 3 + 2 * 9), VORONOI_INDEX_1_1_1 = (1 + 1 * 3 + 1 * 9), VORONOI_INDEX_2_1_1 = (2 + 1 * 3 + 1 * 9),
                VORONOI_INDEX_1_2_1 = (1 + 2 * 3 + 1 * 9), VORONOI_INDEX_2_2_1 = (2 + 2 * 3 + 1 * 9), VORONOI_INDEX_1_1_2 = (1 + 1 * 3 + 2 * 9),
                VORONOI_INDEX_2_1_2 = (2 + 1 * 3 + 2 * 9), VORONOI_INDEX_1_2_2 = (1 + 2 * 3 + 2 * 9), VORONOI_INDEX_2_2_2 = (2 + 2 * 3 + 2 * 9),
                VORONOI_INDEX_1_1_0 = (1 + 1 * 3 + 0 * 9), VORONOI_INDEX_2_1_0 = (2 + 1 * 3 + 0 * 9), VORONOI_INDEX_1_2_0 = (1 + 2 * 3 + 0 * 9),
                VORONOI_INDEX_2_2_0 = (2 + 2 * 3 + 0 * 9), VORONOI_INDEX_1_0_1 = (1 + 0 * 3 + 1 * 9), VORONOI_INDEX_2_0_1 = (2 + 0 * 3 + 1 * 9),
                VORONOI_INDEX_0_1_1 = (0 + 1 * 3 + 1 * 9), VORONOI_INDEX_0_2_1 = (0 + 2 * 3 + 1 * 9), VORONOI_INDEX_1_0_2 = (1 + 0 * 3 + 2 * 9),
                VORONOI_INDEX_2_0_2 = (2 + 0 * 3 + 2 * 9), VORONOI_INDEX_0_1_2 = (0 + 1 * 3 + 2 * 9), VORONOI_INDEX_0_2_2 = (0 + 2 * 3 + 2 * 9);

        public boolean IntersectsSphere(final idSphere sphere) {
            int index, x, y, z;
            float scale, r, d;
            idVec3 p, dir = new idVec3();
            idVec3[] points = new idVec3[8];

            if (CullSphere(sphere)) {
                return false;
            }

            x = y = z = 0;
            dir.Zero();

            p = (sphere.GetOrigin().oMinus(origin)).oMultiply(axis.Transpose());

            if (p.x <= dNear) {
                scale = dNear * invFar;
                dir.y = idMath.Fabs(p.y) - dLeft * scale;
                dir.z = idMath.Fabs(p.z) - dUp * scale;
            } else if (p.x >= dFar) {
                dir.y = idMath.Fabs(p.y) - dLeft;
                dir.z = idMath.Fabs(p.z) - dUp;
            } else {
                scale = p.x * invFar;
                dir.y = idMath.Fabs(p.y) - dLeft * scale;
                dir.z = idMath.Fabs(p.z) - dUp * scale;
            }
            if (dir.y > 0.0f) {
                y = (1 + FLOATSIGNBITNOTSET(p.y));
            }
            if (dir.z > 0.0f) {
                z = (1 + FLOATSIGNBITNOTSET(p.z));
            }
            if (p.x < dNear) {
                scale = dLeft * dNear * invFar;
                if (p.x < dNear + (scale - p.y) * scale * invFar) {
                    scale = dUp * dNear * invFar;
                    if (p.x < dNear + (scale - p.z) * scale * invFar) {
                        x = 1;
                    }
                }
            } else {
                if (p.x > dFar) {
                    x = 2;
                } else if (p.x > dFar + (dLeft - p.y) * dLeft * invFar) {
                    x = 2;
                } else if (p.x > dFar + (dUp - p.z) * dUp * invFar) {
                    x = 2;
                }
            }

            r = sphere.GetRadius();
            index = VORONOI_INDEX(x, y, z);
            switch (index) {
                case VORONOI_INDEX_0_0_0:
                    return true;
                case VORONOI_INDEX_1_0_0:
                    return (dNear - p.x < r);
                case VORONOI_INDEX_2_0_0:
                    return (p.x - dFar < r);
                case VORONOI_INDEX_0_1_0:
                    d = dFar * p.y - dLeft * p.x;
                    return (d * d < r * r * (dFar * dFar + dLeft * dLeft));
                case VORONOI_INDEX_0_2_0:
                    d = -dFar * p.z - dLeft * p.x;
                    return (d * d < r * r * (dFar * dFar + dLeft * dLeft));
                case VORONOI_INDEX_0_0_1:
                    d = dFar * p.z - dUp * p.x;
                    return (d * d < r * r * (dFar * dFar + dUp * dUp));
                case VORONOI_INDEX_0_0_2:
                    d = -dFar * p.z - dUp * p.x;
                    return (d * d < r * r * (dFar * dFar + dUp * dUp));
                default: {
                    ToIndexPoints(points);
                    switch (index) {
                        case VORONOI_INDEX_1_1_1:
                            return sphere.ContainsPoint(points[0]);
                        case VORONOI_INDEX_2_1_1:
                            return sphere.ContainsPoint(points[4]);
                        case VORONOI_INDEX_1_2_1:
                            return sphere.ContainsPoint(points[1]);
                        case VORONOI_INDEX_2_2_1:
                            return sphere.ContainsPoint(points[5]);
                        case VORONOI_INDEX_1_1_2:
                            return sphere.ContainsPoint(points[2]);
                        case VORONOI_INDEX_2_1_2:
                            return sphere.ContainsPoint(points[6]);
                        case VORONOI_INDEX_1_2_2:
                            return sphere.ContainsPoint(points[3]);
                        case VORONOI_INDEX_2_2_2:
                            return sphere.ContainsPoint(points[7]);
                        case VORONOI_INDEX_1_1_0:
                            return sphere.LineIntersection(points[0], points[2]);
                        case VORONOI_INDEX_2_1_0:
                            return sphere.LineIntersection(points[4], points[6]);
                        case VORONOI_INDEX_1_2_0:
                            return sphere.LineIntersection(points[1], points[3]);
                        case VORONOI_INDEX_2_2_0:
                            return sphere.LineIntersection(points[5], points[7]);
                        case VORONOI_INDEX_1_0_1:
                            return sphere.LineIntersection(points[0], points[1]);
                        case VORONOI_INDEX_2_0_1:
                            return sphere.LineIntersection(points[4], points[5]);
                        case VORONOI_INDEX_0_1_1:
                            return sphere.LineIntersection(points[0], points[4]);
                        case VORONOI_INDEX_0_2_1:
                            return sphere.LineIntersection(points[1], points[5]);
                        case VORONOI_INDEX_1_0_2:
                            return sphere.LineIntersection(points[2], points[3]);
                        case VORONOI_INDEX_2_0_2:
                            return sphere.LineIntersection(points[6], points[7]);
                        case VORONOI_INDEX_0_1_2:
                            return sphere.LineIntersection(points[2], points[6]);
                        case VORONOI_INDEX_0_2_2:
                            return sphere.LineIntersection(points[3], points[7]);
                    }
                    break;
                }
            }
            return false;
        }

        public boolean IntersectsFrustum(final idFrustum frustum) {
            idVec3[] indexPoints2 = new idVec3[8], cornerVecs2 = new idVec3[4];
            idFrustum localFrustum2;

            localFrustum2 = new idFrustum(frustum);
            localFrustum2.origin = (frustum.origin.oMinus(origin)).oMultiply(axis.Transpose());
            localFrustum2.axis = frustum.axis.oMultiply(axis.Transpose());
            localFrustum2.ToIndexPointsAndCornerVecs(indexPoints2, cornerVecs2);

            if (CullLocalFrustum(localFrustum2, indexPoints2, cornerVecs2)) {
                return false;
            }

            idVec3[] indexPoints1 = new idVec3[8], cornerVecs1 = new idVec3[4];
            idFrustum localFrustum1;

            localFrustum1 = new idFrustum(this);//TODO:SET
            localFrustum1.origin = (origin.oMinus(frustum.origin)).oMultiply(frustum.axis.Transpose());
            localFrustum1.axis = axis.oMultiply(frustum.axis.Transpose());
            localFrustum1.ToIndexPointsAndCornerVecs(indexPoints1, cornerVecs1);

            if (frustum.CullLocalFrustum(localFrustum1, indexPoints1, cornerVecs1)) {
                return false;
            }

            idSwap(indexPoints2, indexPoints2, 2, 3);
            idSwap(indexPoints2, indexPoints2, 6, 7);

            if (LocalFrustumIntersectsFrustum(indexPoints2, (localFrustum2.dNear > 0.0f))) {
                return true;
            }

            idSwap(indexPoints1, indexPoints1, 2, 3);
            idSwap(indexPoints1, indexPoints1, 6, 7);

            if (frustum.LocalFrustumIntersectsFrustum(indexPoints1, (localFrustum1.dNear > 0.0f))) {
                return true;
            }

            return false;
        }

        public boolean IntersectsWinding(final idWinding winding) {
            int i, j;
            int[] pointCull;
            float[] min = new float[1], max = new float[1];
            idVec3[] localPoints, indexPoints = new idVec3[8], cornerVecs = new idVec3[4];
            idMat3 transpose;
            idPlane plane = new idPlane();

            localPoints = new idVec3[winding.GetNumPoints()];
            pointCull = new int[winding.GetNumPoints()];

            transpose = axis.Transpose();
            for (i = 0; i < winding.GetNumPoints(); i++) {
                localPoints[i] = (winding.oGet(i).ToVec3().oMinus(origin)).oMultiply(transpose);
            }

            // if the winding is culled
            if (CullLocalWinding(localPoints, winding.GetNumPoints(), pointCull)) {
                return false;
            }

            winding.GetPlane(plane);

            ToIndexPointsAndCornerVecs(indexPoints, cornerVecs);
            AxisProjection(indexPoints, cornerVecs, plane.Normal(), min, max);

            // if the frustum does not cross the winding plane
            if (min[0] + plane.oGet(3) > 0.0f || max[0] + plane.oGet(3) < 0.0f) {
                return false;
            }

            // test if any of the winding edges goes through the frustum
            for (i = 0; i < winding.GetNumPoints(); i++) {
                j = (i + 1) % winding.GetNumPoints();
                if (0 == (pointCull[i] & pointCull[j])) {
                    if (LocalLineIntersection(localPoints[i], localPoints[j])) {
                        return true;
                    }
                }
            }

            idSwap(indexPoints, indexPoints, 2, 3);
            idSwap(indexPoints, indexPoints, 6, 7);

            // test if any edges of the frustum intersect the winding
            for (i = 0; i < 4; i++) {
                if (winding.LineIntersection(plane, indexPoints[i], indexPoints[4 + i])) {
                    return true;
                }
            }
            if (dNear > 0.0f) {
                for (i = 0; i < 4; i++) {
                    if (winding.LineIntersection(plane, indexPoints[i], indexPoints[(i + 1) & 3])) {
                        return true;
                    }
                }
            }
            for (i = 0; i < 4; i++) {
                if (winding.LineIntersection(plane, indexPoints[4 + i], indexPoints[4 + ((i + 1) & 3)])) {
                    return true;
                }
            }

            return false;
        }

        /*
         ============
         idFrustum::LineIntersection

         Returns true if the line intersects the box between the start and end point.
         ============
         */
        public boolean LineIntersection(final idVec3 start, final idVec3 end) {
            return LocalLineIntersection((start.oMinus(origin)).oMultiply(axis.Transpose()), (end.oMinus(origin)).oMultiply(axis.Transpose()));
        }

        /*
         ============
         idFrustum::RayIntersection

         Returns true if the ray intersects the bounds.
         The ray can intersect the bounds in both directions from the start point.
         If start is inside the frustum then scale1 < 0 and scale2 > 0.
         ============
         */
        public boolean RayIntersection(final idVec3 start, final idVec3 dir, float[] scale1, float[] scale2) {
            if (LocalRayIntersection((start.oMinus(origin)).oMultiply(axis.Transpose()), dir.oMultiply(axis.Transpose()), scale1, scale2)) {//TODO:scale back ref??
                return true;
            }
            if (scale1[0] <= scale2[0]) {
                return true;
            }
            return false;
        }

        /*
         ============
         idFrustum::FromProjection

         Creates a frustum which contains the projection of the bounds.
         ============
         */
        // returns true if the projection origin is far enough away from the bounding volume to create a valid frustum
        public boolean FromProjection(final idBounds bounds, final idVec3 projectionOrigin, final float dFar) {
            return FromProjection(new idBox(bounds, getVec3_origin(), getMat3_identity()), projectionOrigin, dFar);
        }

        /*
         ============
         idFrustum::FromProjection

         Creates a frustum which contains the projection of the box.
         ============
         */
        public boolean FromProjection(final idBox box, final idVec3 projectionOrigin, final float dFar) {
            int i, bestAxis;
            float value, bestValue;
            idVec3 dir;

            assert (dFar > 0.0f);

            this.dNear = this.dFar = this.invFar = 0.0f;

            dir = box.GetCenter().oMinus(projectionOrigin);
            if (dir.Normalize() == 0.0f) {
                return false;
            }

            bestAxis = 0;
            bestValue = idMath.Fabs(box.GetAxis().oGet(0).oMultiply(dir));
            for (i = 1; i < 3; i++) {
                value = idMath.Fabs(box.GetAxis().oGet(i).oMultiply(dir));
                if (value * box.GetExtents().oGet(bestAxis) * box.GetExtents().oGet(bestAxis) < bestValue * box.GetExtents().oGet(i) * box.GetExtents().oGet(i)) {
                    bestValue = value;
                    bestAxis = i;
                }
            }

//#if 1
            int j, minX, minY, maxY, minZ, maxZ;
            idVec3[] points = new idVec3[8];

            minX = minY = maxY = minZ = maxZ = 0;

            for (j = 0; j < 2; j++) {

                axis.oSet(0, dir);
                axis.oSet(1, box.GetAxis().oGet(bestAxis).oMinus(axis.oGet(0).oMultiply(box.GetAxis().oGet(bestAxis).oMultiply(axis.oGet(0)))));
                axis.oGet(1).Normalize();
                axis.oGet(2).Cross(axis.oGet(0), axis.oGet(1));

                BoxToPoints(
                        (box.GetCenter().oMinus(projectionOrigin)).oMultiply(axis.Transpose()),
                        box.GetExtents(),
                        box.GetAxis().oMultiply(axis.Transpose()),
                        points);

                if (points[0].x <= 1.0f) {
                    return false;
                }

                minX = minY = maxY = minZ = maxZ = 0;
                for (i = 1; i < 8; i++) {
                    if (points[i].x <= 1.0f) {
                        return false;
                    }
                    if (points[i].x < points[minX].x) {
                        minX = i;
                    }
                    if (points[minY].x * points[i].y < points[i].x * points[minY].y) {
                        minY = i;
                    } else if (points[maxY].x * points[i].y > points[i].x * points[maxY].y) {
                        maxY = i;
                    }
                    if (points[minZ].x * points[i].z < points[i].x * points[minZ].z) {
                        minZ = i;
                    } else if (points[maxZ].x * points[i].z > points[i].x * points[maxZ].z) {
                        maxZ = i;
                    }
                }

                if (j == 0) {
                    dir.oPluSet(axis.oGet(1).oMultiply(idMath.Tan16(0.5f * (idMath.ATan16(points[minY].y, points[minY].x) + idMath.ATan16(points[maxY].y, points[maxY].x)))));
                    dir.oPluSet(axis.oGet(2).oMultiply(idMath.Tan16(0.5f * (idMath.ATan16(points[minZ].z, points[minZ].x) + idMath.ATan16(points[maxZ].z, points[maxZ].x)))));
                    dir.Normalize();
                }
            }

            this.origin = new idVec3(projectionOrigin);
            this.dNear = points[minX].x;
            this.dFar = dFar;
            this.dLeft = (float) (Lib.Max(idMath.Fabs(points[minY].y / points[minY].x), idMath.Fabs(points[maxY].y / points[maxY].x)) * dFar);
            this.dUp = (float) (Lib.Max(idMath.Fabs(points[minZ].z / points[minZ].x), idMath.Fabs(points[maxZ].z / points[maxZ].x)) * dFar);
            this.invFar = 1.0f / dFar;

//#elif 1
//
//	int j;
//	float f, x;
//	idBounds b;
//	idVec3 points[8];
//
//	for ( j = 0; j < 2; j++ ) {
//
//		axis[0] = dir;
//		axis[1] = box.GetAxis()[bestAxis] - ( box.GetAxis()[bestAxis] * axis[0] ) * axis[0];
//		axis[1].Normalize();
//		axis[2].Cross( axis[0], axis[1] );
//
//		BoxToPoints( ( box.GetCenter() - projectionOrigin ) * axis.Transpose(), box.GetExtents(), box.GetAxis() * axis.Transpose(), points );
//
//		b.Clear();
//		for ( i = 0; i < 8; i++ ) {
//			x = points[i].x;
//			if ( x <= 1.0f ) {
//				return false;
//			}
//			f = 1.0f / x;
//			points[i].y *= f;
//			points[i].z *= f;
//			b.AddPoint( points[i] );
//		}
//
//		if ( j == 0 ) {
//			dir += idMath::Tan16( 0.5f * ( idMath::ATan16( b[1][1] ) + idMath::ATan16( b[0][1] ) ) ) * axis[1];
//			dir += idMath::Tan16( 0.5f * ( idMath::ATan16( b[1][2] ) + idMath::ATan16( b[0][2] ) ) ) * axis[2];
//			dir.Normalize();
//		}
//	}
//
//	this->origin = projectionOrigin;
//	this->dNear = b[0][0];
//	this->dFar = dFar;
//	this->dLeft = Max( idMath::Fabs( b[0][1] ), idMath::Fabs( b[1][1] ) ) * dFar;
//	this->dUp = Max( idMath::Fabs( b[0][2] ), idMath::Fabs( b[1][2] ) ) * dFar;
//	this->invFar = 1.0f / dFar;
//
//#else
//
//	float dist;
//	idVec3 org;
//
//	axis[0] = dir;
//	axis[1] = box.GetAxis()[bestAxis] - ( box.GetAxis()[bestAxis] * axis[0] ) * axis[0];
//	axis[1].Normalize();
//	axis[2].Cross( axis[0], axis[1] );
//
//	for ( i = 0; i < 3; i++ ) {
//		dist[i] = idMath::Fabs( box.GetExtents()[0] * ( axis[i] * box.GetAxis()[0] ) ) +
//					idMath::Fabs( box.GetExtents()[1] * ( axis[i] * box.GetAxis()[1] ) ) +
//						idMath::Fabs( box.GetExtents()[2] * ( axis[i] * box.GetAxis()[2] ) );
//	}
//
//	dist[0] = axis[0] * ( box.GetCenter() - projectionOrigin ) - dist[0];
//	if ( dist[0] <= 1.0f ) {
//		return false;
//	}
//	float invDist = 1.0f / dist[0];
//
//	this->origin = projectionOrigin;
//	this->dNear = dist[0];
//	this->dFar = dFar;
//	this->dLeft = dist[1] * invDist * dFar;
//	this->dUp = dist[2] * invDist * dFar;
//	this->invFar = 1.0f / dFar;
//
//#endif
            return true;
        }

        /*
         ============
         idFrustum::FromProjection

         Creates a frustum which contains the projection of the sphere.
         ============
         */
        public boolean FromProjection(final idSphere sphere, final idVec3 projectionOrigin, final float dFar) {
            idVec3 dir;
            float d, r, s, x, y;

            assert (dFar > 0.0f);

            dir = sphere.GetOrigin().oMinus(projectionOrigin);
            d = dir.Normalize();
            r = sphere.GetRadius();

            if (d <= r + 1.0f) {
                this.dNear = this.dFar = this.invFar = 0.0f;
                return false;
            }

            origin = new idVec3(projectionOrigin);
            axis = dir.ToMat3();

            s = idMath.Sqrt(d * d - r * r);
            x = r / d * s;
            y = idMath.Sqrt(s * s - x * x);

            this.dNear = d - r;
            this.dFar = dFar;
            this.dLeft = x / y * dFar;
            this.dUp = dLeft;
            this.invFar = 1.0f / dFar;

            return true;
        }
        //

        /*
         ============
         idFrustum::ConstrainToBounds

         Returns false if no part of the bounds extends beyond the near plane.
         ============
         */
        // moves the far plane so it extends just beyond the bounding volume
        public boolean ConstrainToBounds(final idBounds bounds) {
            float[] min = new float[1], max = new float[1];
            float newdFar;

            bounds.AxisProjection(axis.oGet(0), min, max);
            newdFar = max[0] - origin.oMultiply(axis.oGet(0));
            if (newdFar <= dNear) {
                MoveFarDistance(dNear + 1.0f);
                return false;
            }
            MoveFarDistance(newdFar);
            return true;
        }

        /*
         ============
         idFrustum::ConstrainToBox

         Returns false if no part of the box extends beyond the near plane.
         ============
         */
        public boolean ConstrainToBox(final idBox box) {
            float[] min = new float[1], max = new float[1];
            float newdFar;

            box.AxisProjection(axis.oGet(0), min, max);
            newdFar = max[0] - origin.oMultiply(axis.oGet(0));
            if (newdFar <= dNear) {
                MoveFarDistance(dNear + 1.0f);
                return false;
            }
            MoveFarDistance(newdFar);
            return true;
        }

        /*
         ============
         idFrustum::ConstrainToSphere

         Returns false if no part of the sphere extends beyond the near plane.
         ============
         */
        public boolean ConstrainToSphere(final idSphere sphere) {
            float[] min = new float[1], max = new float[1];
            float newdFar;

            sphere.AxisProjection(axis.oGet(0), min, max);
            newdFar = max[0] - origin.oMultiply(axis.oGet(0));
            if (newdFar <= dNear) {
                MoveFarDistance(dNear + 1.0f);
                return false;
            }
            MoveFarDistance(newdFar);
            return true;
        }

        /*
         ============
         idFrustum::ConstrainToFrustum

         Returns false if no part of the frustum extends beyond the near plane.
         ============
         */
        public boolean ConstrainToFrustum(final idFrustum frustum) {
            float[] min = new float[1], max = new float[1];
            float newdFar;

            frustum.AxisProjection(axis.oGet(0), min, max);
            newdFar = max[0] - origin.oMultiply(axis.oGet(0));
            if (newdFar <= dNear) {
                MoveFarDistance(dNear + 1.0f);
                return false;
            }
            MoveFarDistance(newdFar);
            return true;
        }
//

        /*
         ============
         idFrustum::ToPlanes

         planes point outwards
         ============
         */
        public void ToPlanes(idPlane planes[]) {			// planes point outwards
            int i;
            idVec3[] scaled = new idVec3[2];
            idVec3[] points = new idVec3[4];

            planes[0].oNorSet(axis.oGet(0).oNegative());
            planes[0].SetDist(-dNear);
            planes[1].oNorSet(axis.oGet(0));
            planes[1].SetDist(dFar);

            scaled[0] = axis.oGet(1).oMultiply(dLeft);
            scaled[1] = axis.oGet(2).oMultiply(dUp);
            points[0] = scaled[0].oPlus(scaled[1]);
            points[1] = scaled[0].oPlus(scaled[1]).oNegative();
            points[2] = scaled[0].oMinus(scaled[1]).oNegative();
            points[3] = scaled[0].oMinus(scaled[1]);

            for (i = 0; i < 4; i++) {
                planes[i + 2].oNorSet(points[i].Cross(points[(i + 1) & 3].oMinus(points[i])));
                planes[i + 2].Normalize();
                planes[i + 2].FitThroughPoint(points[i]);
            }
        }

        public void ToPoints(idVec3 points[]) {				// 8 corners of the frustum
            idMat3 scaled = new idMat3();

            scaled.oSet(0, origin.oPlus(axis.oGet(0).oMultiply(dNear)));
            scaled.oSet(1, axis.oGet(1).oMultiply(dLeft * dNear * invFar));
            scaled.oSet(2, axis.oGet(2).oMultiply(dUp * dNear * invFar));

            points[0] = scaled.oGet(0).oPlus(scaled.oGet(1));
            points[1] = scaled.oGet(0).oMinus(scaled.oGet(1));
            points[2] = points[1].oMinus(scaled.oGet(2));
            points[3] = points[0].oMinus(scaled.oGet(2));
            points[0].oPluSet(scaled.oGet(2));
            points[1].oPluSet(scaled.oGet(2));

            scaled.oSet(0, origin.oPlus(axis.oGet(0).oMultiply(dFar)));
            scaled.oSet(1, axis.oGet(1).oMultiply(dLeft));
            scaled.oSet(2, axis.oGet(2).oMultiply(dUp));

            points[4] = scaled.oGet(0).oPlus(scaled.oGet(1));
            points[5] = scaled.oGet(0).oMinus(scaled.oGet(1));
            points[6] = points[5].oMinus(scaled.oGet(2));
            points[7] = points[4].oMinus(scaled.oGet(2));
            points[4].oPluSet(scaled.oGet(2));
            points[5].oPluSet(scaled.oGet(2));
        }
//

        /*
         ============
         idFrustum::AxisProjection

         40 muls
         ============
         */
        // calculates the projection of this frustum onto the given axis
        public void AxisProjection(final idVec3 dir, float[] min, float[] max) {
            idVec3[] indexPoints = new idVec3[8], cornerVecs = new idVec3[4];

            ToIndexPointsAndCornerVecs(indexPoints, cornerVecs);
            AxisProjection(indexPoints, cornerVecs, dir, min, max);
        }

        /*
         ============
         idFrustum::AxisProjection

         76 muls
         ============
         */
        public void AxisProjection(final idMat3 ax, idBounds bounds) {
            idVec3[] indexPoints = new idVec3[8], cornerVecs = new idVec3[4];
            final float[] b00 = {bounds.oGet(0).oGet(0)}, b01 = {bounds.oGet(0).oGet(1)}, b02 = {bounds.oGet(0).oGet(2)},
                    b10 = {bounds.oGet(1).oGet(0)}, b11 = {bounds.oGet(1).oGet(1)}, b12 = {bounds.oGet(1).oGet(2)};

            ToIndexPointsAndCornerVecs(indexPoints, cornerVecs);
            AxisProjection(indexPoints, cornerVecs, ax.oGet(0), b00, b11);
            AxisProjection(indexPoints, cornerVecs, ax.oGet(1), b01, b11);
            AxisProjection(indexPoints, cornerVecs, ax.oGet(2), b02, b12);

            bounds.oSet(0, 0, b00[0]);
            bounds.oSet(0, 1, b01[0]);
            bounds.oSet(0, 2, b02[0]);
            bounds.oSet(1, 0, b10[0]);
            bounds.oSet(1, 1, b11[0]);
            bounds.oSet(1, 2, b12[0]);

        }
//

        // calculates the bounds for the projection in this frustum
        public boolean ProjectionBounds(final idBounds bounds, idBounds projectionBounds) {
            return ProjectionBounds(new idBox(bounds, getVec3_origin(), getMat3_identity()), projectionBounds);
        }

        public boolean ProjectionBounds(final idBox box, idBounds projectionBounds) {
            int i, p1, p2, culled, outside;
            int[][] pointCull = new int[8][1];
            float[] scale1 = new float[1], scale2 = new float[1];
            idFrustum localFrustum;
            idVec3[] points = new idVec3[8];
            idVec3 localOrigin;
            idMat3 localAxis, localScaled;
            idBounds bounds = new idBounds(box.GetExtents().oNegative(), box.GetExtents());

            // if the frustum origin is inside the bounds
            if (bounds.ContainsPoint((origin.oMinus(box.GetCenter())).oMultiply(box.GetAxis().Transpose()))) {
                // bounds that cover the whole frustum
                float[] boxMin = new float[1], boxMax = new float[1];
                float base;

                base = origin.oMultiply(axis.oGet(0));
                box.AxisProjection(axis.oGet(0), boxMin, boxMax);

                projectionBounds.oSet(0, 0, boxMin[0] - base);
                projectionBounds.oSet(1, 0, boxMax[0] - base);
                projectionBounds.oSet(0, 1, -1.0f);
                projectionBounds.oSet(0, 2, -1.0f);
                projectionBounds.oSet(1, 1, 1.0f);
                projectionBounds.oSet(1, 2, 1.0f);

                return true;
            }

            projectionBounds.Clear();

            // transform the bounds into the space of this frustum
            localOrigin = (box.GetCenter().oMinus(origin)).oMultiply(axis.Transpose());
            localAxis = box.GetAxis().oMultiply(axis.Transpose());
            BoxToPoints(localOrigin, box.GetExtents(), localAxis, points);

            // test outer four edges of the bounds
            culled = -1;
            outside = 0;
            for (i = 0; i < 4; i++) {
                p1 = i;
                p2 = 4 + i;
                AddLocalLineToProjectionBoundsSetCull(points[p1], points[p2], pointCull[p1], pointCull[p2], projectionBounds);
                culled &= pointCull[p1][0] & pointCull[p2][0];
                outside |= pointCull[p1][0] | pointCull[p2][0];
            }

            // if the bounds are completely outside this frustum
            if (culled != 0) {
                return false;
            }

            // if the bounds are completely inside this frustum
            if (0 == outside) {
                return true;
            }

            // test the remaining edges of the bounds
            for (i = 0; i < 4; i++) {
                p1 = i;
                p2 = (i + 1) & 3;
                AddLocalLineToProjectionBoundsUseCull(points[p1], points[p2], pointCull[p1][0], pointCull[p2][0], projectionBounds);
            }

            for (i = 0; i < 4; i++) {
                p1 = 4 + i;
                p2 = 4 + ((i + 1) & 3);
                AddLocalLineToProjectionBoundsUseCull(points[p1], points[p2], pointCull[p1][0], pointCull[p2][0], projectionBounds);
            }

            // if the bounds extend beyond two or more boundaries of this frustum
            if (outside != 1 && outside != 2 && outside != 4 && outside != 8) {

                localOrigin = (origin.oMinus(box.GetCenter())).oMultiply(box.GetAxis().Transpose());
                localScaled = axis.oMultiply(box.GetAxis().Transpose());
                localScaled.oGet(0).oMulSet(dFar);
                localScaled.oGet(1).oMulSet(dLeft);
                localScaled.oGet(2).oMulSet(dUp);

                // test the outer edges of this frustum for intersection with the bounds
                if ((outside & 2) == 2 && (outside & 8) == 8) {
                    BoundsRayIntersection(bounds, localOrigin, localScaled.oGet(0).oMinus(localScaled.oGet(1).oMinus(localScaled.oGet(2))), scale1, scale2);
                    if (scale1[0] <= scale2[0] && scale1[0] >= 0.0f) {
                        projectionBounds.AddPoint(new idVec3(scale1[0] * dFar, -1.0f, -1.0f));
                        projectionBounds.AddPoint(new idVec3(scale2[0] * dFar, -1.0f, -1.0f));
                    }
                }
                if ((outside & 2) == 2 && (outside & 4) == 4) {
                    BoundsRayIntersection(bounds, localOrigin, localScaled.oGet(0).oMinus(localScaled.oGet(1).oPlus(localScaled.oGet(2))), scale1, scale2);
                    if (scale1[0] <= scale2[0] && scale1[0] >= 0.0f) {
                        projectionBounds.AddPoint(new idVec3(scale1[0] * dFar, -1.0f, 1.0f));
                        projectionBounds.AddPoint(new idVec3(scale2[0] * dFar, -1.0f, 1.0f));
                    }
                }
                if ((outside & 1) == 1 && (outside & 8) == 8) {
                    BoundsRayIntersection(bounds, localOrigin, localScaled.oGet(0).oPlus(localScaled.oGet(1).oMinus(localScaled.oGet(2))), scale1, scale2);
                    if (scale1[0] <= scale2[0] && scale1[0] >= 0.0f) {
                        projectionBounds.AddPoint(new idVec3(scale1[0] * dFar, 1.0f, -1.0f));
                        projectionBounds.AddPoint(new idVec3(scale2[0] * dFar, 1.0f, -1.0f));
                    }
                }
                if ((outside & 1) == 1 && (outside & 2) == 2) {
                    BoundsRayIntersection(bounds, localOrigin, localScaled.oGet(0).oPlus(localScaled.oGet(1).oPlus(localScaled.oGet(2))), scale1, scale2);
                    if (scale1[0] <= scale2[0] && scale1[0] >= 0.0f) {
                        projectionBounds.AddPoint(new idVec3(scale1[0] * dFar, 1.0f, 1.0f));
                        projectionBounds.AddPoint(new idVec3(scale2[0] * dFar, 1.0f, 1.0f));
                    }
                }
            }

            return true;
        }

        public boolean ProjectionBounds(final idSphere sphere, idBounds projectionBounds) {
            float d, r, rs, sFar;
            idVec3 center;

            projectionBounds.Clear();

            center = (sphere.GetOrigin().oMinus(origin)).oMultiply(axis.Transpose());
            r = sphere.GetRadius();
            rs = r * r;
            sFar = dFar * dFar;

            // test left/right planes
            d = dFar * idMath.Fabs(center.y) - dLeft * center.x;
            if ((d * d) > rs * (sFar + dLeft * dLeft)) {
                return false;
            }

            // test up/down planes
            d = dFar * idMath.Fabs(center.z) - dUp * center.x;
            if ((d * d) > rs * (sFar + dUp * dUp)) {
                return false;
            }

            // bounds that cover the whole frustum
            projectionBounds.oGet(0).x = 0.0f;
            projectionBounds.oGet(1).x = dFar;
            projectionBounds.oGet(0).y = projectionBounds.oGet(0).z = -1.0f;
            projectionBounds.oGet(1).y = projectionBounds.oGet(1).z = 1.0f;
            return true;
        }

        public boolean ProjectionBounds(final idFrustum frustum, idBounds projectionBounds) {
            int i, p1, p2, culled, outside;
            int[][] pointCull = new int[8][1];
            float[] scale1 = new float[1], scale2 = new float[1];
            idFrustum localFrustum;
            idVec3[] points = new idVec3[8];
            idVec3 localOrigin;
            idMat3 localScaled;

            // if the frustum origin is inside the other frustum
            if (frustum.ContainsPoint(origin)) {
                // bounds that cover the whole frustum
                float[] frustumMin = new float[1], frustumMax = new float[1];
                float base;

                base = origin.oMultiply(axis.oGet(0));
                frustum.AxisProjection(axis.oGet(0), frustumMin, frustumMax);

                projectionBounds.oGet(0).x = frustumMin[0] - base;
                projectionBounds.oGet(1).x = frustumMax[0] - base;
                projectionBounds.oGet(0).y = projectionBounds.oGet(0).z = -1.0f;
                projectionBounds.oGet(1).y = projectionBounds.oGet(1).z = 1.0f;
                return true;
            }

            projectionBounds.Clear();

            // transform the given frustum into the space of this frustum
            localFrustum = frustum;
            localFrustum.origin = (frustum.origin.oMinus(origin)).oMultiply(axis.Transpose());
            localFrustum.axis = frustum.axis.oMultiply(axis.Transpose());
            localFrustum.ToPoints(points);

            // test outer four edges of the other frustum
            culled = -1;
            outside = 0;
            for (i = 0; i < 4; i++) {
                p1 = i;
                p2 = 4 + i;
                AddLocalLineToProjectionBoundsSetCull(points[p1], points[p2], pointCull[p1], pointCull[p2], projectionBounds);
                culled &= pointCull[p1][0] & pointCull[p2][0];
                outside |= pointCull[p1][0] | pointCull[p2][0];
            }

            // if the other frustum is completely outside this frustum
            if (culled != 0) {
                return false;
            }

            // if the other frustum is completely inside this frustum
            if (0 == outside) {
                return true;
            }

            // test the remaining edges of the other frustum
            if (localFrustum.dNear > 0.0f) {
                for (i = 0; i < 4; i++) {
                    p1 = i;
                    p2 = (i + 1) & 3;
                    AddLocalLineToProjectionBoundsUseCull(points[p1], points[p2], pointCull[p1][0], pointCull[p2][0], projectionBounds);
                }
            }

            for (i = 0; i < 4; i++) {
                p1 = 4 + i;
                p2 = 4 + ((i + 1) & 3);
                AddLocalLineToProjectionBoundsUseCull(points[p1], points[p2], pointCull[p1][0], pointCull[p2][0], projectionBounds);
            }

            // if the other frustum extends beyond two or more boundaries of this frustum
            if (outside != 1 && outside != 2 && outside != 4 && outside != 8) {

                localOrigin = (origin.oMinus(frustum.origin)).oMultiply(frustum.axis.Transpose());
                localScaled = axis.oMultiply(frustum.axis.Transpose());
                localScaled.oGet(0).oMulSet(dFar);
                localScaled.oGet(1).oMulSet(dLeft);
                localScaled.oGet(2).oMulSet(dUp);

                // test the outer edges of this frustum for intersection with the other frustum
                if ((outside & 2) == 2 && (outside & 8) == 8) {
                    frustum.LocalRayIntersection(localOrigin, localScaled.oGet(0).oMinus(localScaled.oGet(1)).oMinus(localScaled.oGet(2)), scale1, scale2);
                    if (scale1[0] <= scale2[0] && scale1[0] >= 0.0f) {
                        projectionBounds.AddPoint(new idVec3(scale1[0] * dFar, -1.0f, -1.0f));
                        projectionBounds.AddPoint(new idVec3(scale2[0] * dFar, -1.0f, -1.0f));
                    }
                }
                if ((outside & 2) == 2 && (outside & 4) == 4) {
                    frustum.LocalRayIntersection(localOrigin, localScaled.oGet(0).oMinus(localScaled.oGet(1)).oPlus(localScaled.oGet(2)), scale1, scale2);
                    if (scale1[0] <= scale2[0] && scale1[0] >= 0.0f) {
                        projectionBounds.AddPoint(new idVec3(scale1[0] * dFar, -1.0f, 1.0f));
                        projectionBounds.AddPoint(new idVec3(scale2[0] * dFar, -1.0f, 1.0f));
                    }
                }
                if ((outside & 1) == 1 && (outside & 8) == 8) {
                    frustum.LocalRayIntersection(localOrigin, localScaled.oGet(0).oPlus(localScaled.oGet(1)).oMinus(localScaled.oGet(2)), scale1, scale2);
                    if (scale1[0] <= scale2[0] && scale1[0] >= 0.0f) {
                        projectionBounds.AddPoint(new idVec3(scale1[0] * dFar, 1.0f, -1.0f));
                        projectionBounds.AddPoint(new idVec3(scale2[0] * dFar, 1.0f, -1.0f));
                    }
                }
                if ((outside & 1) == 1 && (outside & 2) == 2) {
                    frustum.LocalRayIntersection(localOrigin, localScaled.oGet(0).oPlus(localScaled.oGet(1)).oPlus(localScaled.oGet(2)), scale1, scale2);
                    if (scale1[0] <= scale2[0] && scale1[0] >= 0.0f) {
                        projectionBounds.AddPoint(new idVec3(scale1[0] * dFar, 1.0f, 1.0f));
                        projectionBounds.AddPoint(new idVec3(scale2[0] * dFar, 1.0f, 1.0f));
                    }
                }
            }

            return true;
        }

        public boolean ProjectionBounds(final idWinding winding, idBounds projectionBounds) {
            int i, p1, p2, culled, outside;
            int[][] pointCull;
            float[] scale = new float[1];
            idVec3[] localPoints;
            idMat3 transpose, scaled = new idMat3();
            idPlane plane = new idPlane();

            projectionBounds.Clear();

            // transform the winding points into the space of this frustum
            localPoints = new idVec3[winding.GetNumPoints()];
            transpose = axis.Transpose();
            for (i = 0; i < winding.GetNumPoints(); i++) {
                localPoints[i] = (winding.oGet(0).ToVec3().oMinus(origin)).oMultiply(transpose);
            }

            // test the winding edges
            culled = -1;
            outside = 0;
            pointCull = new int[winding.GetNumPoints()][1];
            for (i = 0; i < winding.GetNumPoints(); i += 2) {
                p1 = i;
                p2 = (i + 1) % winding.GetNumPoints();
                AddLocalLineToProjectionBoundsSetCull(localPoints[p1], localPoints[p2], pointCull[p1], pointCull[p2], projectionBounds);
                culled &= pointCull[p1][0] & pointCull[p2][0];
                outside |= pointCull[p1][0] | pointCull[p2][0];
            }

            // if completely culled
            if (culled != 0) {
                return false;
            }

            // if completely inside
            if (0 == outside) {
                return true;
            }

            // test remaining winding edges
            for (i = 1; i < winding.GetNumPoints(); i += 2) {
                p1 = i;
                p2 = (i + 1) % winding.GetNumPoints();
                AddLocalLineToProjectionBoundsUseCull(localPoints[p1], localPoints[p2], pointCull[p1][0], pointCull[p2][0], projectionBounds);
            }

            // if the winding extends beyond two or more boundaries of this frustum
            if (outside != 1 && outside != 2 && outside != 4 && outside != 8) {

                winding.GetPlane(plane);
                scaled.oSet(0, axis.oGet(0).oMultiply(dFar));
                scaled.oSet(1, axis.oGet(1).oMultiply(dLeft));
                scaled.oSet(2, axis.oGet(2).oMultiply(dUp));

                // test the outer edges of this frustum for intersection with the winding
                if ((outside & 2) == 2 && (outside & 8) == 8) {
                    if (winding.RayIntersection(plane, origin, scaled.oGet(0).oMinus(scaled.oGet(1)).oMinus(scaled.oGet(2)), scale)) {
                        projectionBounds.AddPoint(new idVec3(scale[0] * dFar, -1.0f, -1.0f));
                    }
                }
                if ((outside & 2) == 2 && (outside & 4) == 4) {
                    if (winding.RayIntersection(plane, origin, scaled.oGet(0).oMinus(scaled.oGet(1)).oPlus(scaled.oGet(2)), scale)) {
                        projectionBounds.AddPoint(new idVec3(scale[0] * dFar, -1.0f, 1.0f));
                    }
                }
                if ((outside & 1) == 1 && (outside & 8) == 8) {
                    if (winding.RayIntersection(plane, origin, scaled.oGet(0).oPlus(scaled.oGet(1)).oMinus(scaled.oGet(2)), scale)) {
                        projectionBounds.AddPoint(new idVec3(scale[0] * dFar, 1.0f, -1.0f));
                    }
                }
                if ((outside & 1) == 1 && (outside & 2) == 2) {
                    if (winding.RayIntersection(plane, origin, scaled.oGet(0).oPlus(scaled.oGet(1)).oPlus(scaled.oGet(2)), scale)) {
                        projectionBounds.AddPoint(new idVec3(scale[0] * dFar, 1.0f, 1.0f));
                    }
                }
            }

            return true;
        }

        // calculates the bounds for the projection in this frustum of the given frustum clipped to the given box
        public boolean ClippedProjectionBounds(final idFrustum frustum, final idBox clipBox, idBounds projectionBounds) {
            int i, p1, p2, usedClipPlanes, nearCull, farCull, outside;
            int[] clipPointCull = new int[8], clipPlanes = new int[4];
            int[] pointCull = new int[2], startClip = {0}, endClip = {0}, boxPointCull = new int[8];
            float leftScale, upScale;
            float[] s1 = {0}, s2 = {0}, t1 = {0}, t2 = {0};
            float[] clipFractions = new float[4];
            idFrustum localFrustum;
            idVec3 localOrigin1, localOrigin2, start = new idVec3(), end = new idVec3();
            idVec3[] clipPoints = new idVec3[8], localPoints1 = new idVec3[8], localPoints2 = new idVec3[8];
            idMat3 localAxis1, localAxis2, transpose;
            idBounds clipBounds = new idBounds();

            // if the frustum origin is inside the other frustum
            if (frustum.ContainsPoint(origin)) {
                // bounds that cover the whole frustum
                float[] clipBoxMin = {0}, clipBoxMax = {0}, frustumMin = {0}, frustumMax = {0}, base = {0};

                base[0] = origin.oMultiply(axis.oGet(0));
                clipBox.AxisProjection(axis.oGet(0), clipBoxMin, clipBoxMax);
                frustum.AxisProjection(axis.oGet(0), frustumMin, frustumMax);

                projectionBounds.oGet(0).x = (float) (Max(clipBoxMin[0], frustumMin[0]) - base[0]);
                projectionBounds.oGet(1).x = (float) (Min(clipBoxMax[0], frustumMax[0]) - base[0]);
                projectionBounds.oGet(0).y = projectionBounds.oGet(0).z = -1.0f;
                projectionBounds.oGet(1).y = projectionBounds.oGet(1).z = 1.0f;
                return true;
            }

            projectionBounds.Clear();

            // clip the outer edges of the given frustum to the clip bounds
            frustum.ClipFrustumToBox(clipBox, clipFractions, clipPlanes);
            usedClipPlanes = clipPlanes[0] | clipPlanes[1] | clipPlanes[2] | clipPlanes[3];

            // transform the clipped frustum to the space of this frustum
            transpose = axis;
            transpose.TransposeSelf();
            localFrustum = frustum;
            localFrustum.origin = (frustum.origin.oMinus(origin)).oMultiply(transpose);
            localFrustum.axis = frustum.axis.oMultiply(transpose);
            localFrustum.ToClippedPoints(clipFractions, clipPoints);

            // test outer four edges of the clipped frustum
            for (i = 0; i < 4; i++) {
                p1 = i;
                p2 = 4 + i;
                int[] clipPointCull_p1 = {0}, clipPointCull_p2 = {0};
                AddLocalLineToProjectionBoundsSetCull(clipPoints[p1], clipPoints[p2], clipPointCull_p1, clipPointCull_p2, projectionBounds);
                clipPointCull[p1] = clipPointCull_p1[0];
                clipPointCull[p2] = clipPointCull_p2[0];
            }

            // get cull bits for the clipped frustum
            outside = clipPointCull[0] | clipPointCull[1] | clipPointCull[2] | clipPointCull[3]
                    | clipPointCull[4] | clipPointCull[5] | clipPointCull[6] | clipPointCull[7];
            nearCull = clipPointCull[0] & clipPointCull[1] & clipPointCull[2] & clipPointCull[3];
            farCull = clipPointCull[4] & clipPointCull[5] & clipPointCull[6] & clipPointCull[7];

            // if the clipped frustum is not completely inside this frustum
            if (outside != 0) {

                // test the remaining edges of the clipped frustum
                if (0 == nearCull && localFrustum.dNear > 0.0f) {
                    for (i = 0; i < 4; i++) {
                        p1 = i;
                        p2 = (i + 1) & 3;
                        AddLocalLineToProjectionBoundsUseCull(clipPoints[p1], clipPoints[p2], clipPointCull[p1], clipPointCull[p2], projectionBounds);
                    }
                }

                if (0 == farCull) {
                    for (i = 0; i < 4; i++) {
                        p1 = 4 + i;
                        p2 = 4 + ((i + 1) & 3);
                        AddLocalLineToProjectionBoundsUseCull(clipPoints[p1], clipPoints[p2], clipPointCull[p1], clipPointCull[p2], projectionBounds);
                    }
                }
            }

            // if the clipped frustum far end points are inside this frustum
            if (!(farCull != 0 && 0 == (nearCull & farCull))
                    && // if the clipped frustum is not clipped to a single plane of the clip bounds
                    (clipPlanes[0] != clipPlanes[1] || clipPlanes[1] != clipPlanes[2] || clipPlanes[2] != clipPlanes[3])) {

                // transform the clip box into the space of the other frustum
                transpose = frustum.axis;
                transpose.TransposeSelf();
                localOrigin1 = (clipBox.GetCenter().oMinus(frustum.origin)).oMultiply(transpose);
                localAxis1 = clipBox.GetAxis().oMultiply(transpose);
                BoxToPoints(localOrigin1, clipBox.GetExtents(), localAxis1, localPoints1);

                // cull the box corners with the other frustum
                leftScale = frustum.dLeft * frustum.invFar;
                upScale = frustum.dUp * frustum.invFar;
                for (i = 0; i < 8; i++) {
                    idVec3 p = localPoints1[i];
                    if (0 == (boxVertPlanes[i] & usedClipPlanes) || p.x <= 0.0f) {
                        boxPointCull[i] = 1 | 2 | 4 | 8;
                    } else {
                        boxPointCull[i] = 0;
                        if (idMath.Fabs(p.y) > p.x * leftScale) {
                            boxPointCull[i] |= 1 << FLOATSIGNBITSET(p.y);
                        }
                        if (idMath.Fabs(p.z) > p.x * upScale) {
                            boxPointCull[i] |= 4 << FLOATSIGNBITSET(p.z);
                        }
                    }
                }

                // transform the clip box into the space of this frustum
                transpose = axis;
                transpose.TransposeSelf();
                localOrigin2 = (clipBox.GetCenter().oMinus(origin)).oMultiply(transpose);
                localAxis2 = clipBox.GetAxis().oMultiply(transpose);
                BoxToPoints(localOrigin2, clipBox.GetExtents(), localAxis2, localPoints2);

                // clip the edges of the clip bounds to the other frustum and add the clipped edges to the projection bounds
                for (i = 0; i < 4; i++) {
                    p1 = i;
                    p2 = 4 + i;
                    if (0 == (boxPointCull[p1] & boxPointCull[p2])) {
                        if (frustum.ClipLine(localPoints1, localPoints2, p1, p2, start, end, startClip, endClip)) {
                            AddLocalLineToProjectionBoundsSetCull(start, end, pointCull, projectionBounds);
                            AddLocalCapsToProjectionBounds(clipPoints, 4, clipPointCull, 4, start, pointCull[0], startClip[0], projectionBounds);
                            AddLocalCapsToProjectionBounds(clipPoints, 4, clipPointCull, 4, end, pointCull[1], endClip[0], projectionBounds);
                            outside |= pointCull[0] | pointCull[1];
                        }
                    }
                }

                for (i = 0; i < 4; i++) {
                    p1 = i;
                    p2 = (i + 1) & 3;
                    if (0 == (boxPointCull[p1] & boxPointCull[p2])) {
                        if (frustum.ClipLine(localPoints1, localPoints2, p1, p2, start, end, startClip, endClip)) {
                            AddLocalLineToProjectionBoundsSetCull(start, end, pointCull, projectionBounds);
                            AddLocalCapsToProjectionBounds(clipPoints, 4, clipPointCull, 4, start, pointCull[0], startClip[0], projectionBounds);
                            AddLocalCapsToProjectionBounds(clipPoints, 4, clipPointCull, 4, end, pointCull[1], endClip[0], projectionBounds);
                            outside |= pointCull[0] | pointCull[1];
                        }
                    }
                }

                for (i = 0; i < 4; i++) {
                    p1 = 4 + i;
                    p2 = 4 + ((i + 1) & 3);
                    if (0 == (boxPointCull[p1] & boxPointCull[p2])) {
                        if (frustum.ClipLine(localPoints1, localPoints2, p1, p2, start, end, startClip, endClip)) {
                            AddLocalLineToProjectionBoundsSetCull(start, end, pointCull, projectionBounds);
                            AddLocalCapsToProjectionBounds(clipPoints, 4, clipPointCull, 4, start, pointCull[0], startClip[0], projectionBounds);
                            AddLocalCapsToProjectionBounds(clipPoints, 4, clipPointCull, 4, end, pointCull[1], endClip[0], projectionBounds);
                            outside |= pointCull[0] | pointCull[1];
                        }
                    }
                }
            }

            // if the clipped frustum extends beyond two or more boundaries of this frustum
            if (outside != 1 && outside != 2 && outside != 4 && outside != 8) {

                // transform this frustum into the space of the other frustum
                transpose = frustum.axis;
                transpose.TransposeSelf();
                localOrigin1 = (origin.oMinus(frustum.origin)).oMultiply(transpose);
                localAxis1 = axis.oMultiply(transpose);
                localAxis1.oGet(0).oMulSet(dFar);
                localAxis1.oGet(1).oMulSet(dLeft);
                localAxis1.oGet(2).oMulSet(dUp);

                // transform this frustum into the space of the clip bounds
                transpose = clipBox.GetAxis();
                transpose.TransposeSelf();
                localOrigin2 = (origin.oMinus(clipBox.GetCenter())).oMultiply(transpose);
                localAxis2 = axis.oMultiply(transpose);
                localAxis2.oGet(0).oMulSet(dFar);
                localAxis2.oGet(1).oMulSet(dLeft);
                localAxis2.oGet(2).oMulSet(dUp);

                clipBounds.oSet(0, clipBox.GetExtents().oNegative());
                clipBounds.oSet(1, clipBox.GetExtents());

                // test the outer edges of this frustum for intersection with both the other frustum and the clip bounds
                if ((outside & 2) != 0 && (outside & 8) != 0) {
                    frustum.LocalRayIntersection(localOrigin1, localAxis1.oGet(0).oMinus(localAxis1.oGet(1).oMinus(localAxis1.oGet(2))), s1, s2);
                    if (s1[0] <= s2[0] && s1[0] >= 0.0f) {
                        BoundsRayIntersection(clipBounds, localOrigin2, localAxis2.oGet(0).oMinus(localAxis2.oGet(1).oMinus(localAxis2.oGet(2))), t1, t2);
                        if (t1[0] <= t2[0] && t2[0] > s1[0] && t1[0] < s2[0]) {
                            projectionBounds.AddPoint(new idVec3(s1[0] * dFar, -1.0f, -1.0f));
                            projectionBounds.AddPoint(new idVec3(s2[0] * dFar, -1.0f, -1.0f));
                        }
                    }
                }
                if ((outside & 2) != 0 && (outside & 4) != 0) {
                    frustum.LocalRayIntersection(localOrigin1, localAxis1.oGet(0).oMinus(localAxis1.oGet(1).oPlus(localAxis1.oGet(2))), s1, s2);
                    if (s1[0] <= s2[0] && s1[0] >= 0.0f) {
                        BoundsRayIntersection(clipBounds, localOrigin2, localAxis2.oGet(0).oMinus(localAxis2.oGet(1).oPlus(localAxis2.oGet(2))), t1, t2);
                        if (t1[0] <= t2[0] && t2[0] > s1[0] && t1[0] < s2[0]) {
                            projectionBounds.AddPoint(new idVec3(s1[0] * dFar, -1.0f, 1.0f));
                            projectionBounds.AddPoint(new idVec3(s2[0] * dFar, -1.0f, 1.0f));
                        }
                    }
                }
                if ((outside & 1) != 0 && (outside & 8) != 0) {
                    frustum.LocalRayIntersection(localOrigin1, localAxis1.oGet(0).oPlus(localAxis1.oGet(1).oMinus(localAxis1.oGet(2))), s1, s2);
                    if (s1[0] <= s2[0] && s1[0] >= 0.0f) {
                        BoundsRayIntersection(clipBounds, localOrigin2, localAxis2.oGet(0).oPlus(localAxis2.oGet(1).oMinus(localAxis2.oGet(2))), t1, t2);
                        if (t1[0] <= t2[0] && t2[0] > s1[0] && t1[0] < s2[0]) {
                            projectionBounds.AddPoint(new idVec3(s1[0] * dFar, 1.0f, -1.0f));
                            projectionBounds.AddPoint(new idVec3(s2[0] * dFar, 1.0f, -1.0f));
                        }
                    }
                }
                if ((outside & 1) != 0 && (outside & 2) != 0) {
                    frustum.LocalRayIntersection(localOrigin1, localAxis1.oGet(0).oPlus(localAxis1.oGet(1).oPlus(localAxis1.oGet(2))), s1, s2);
                    if (s1[0] <= s2[0] && s1[0] >= 0.0f) {
                        BoundsRayIntersection(clipBounds, localOrigin2, localAxis2.oGet(0).oPlus(localAxis2.oGet(1).oPlus(localAxis2.oGet(2))), t1, t2);
                        if (t1[0] <= t2[0] && t2[0] > s1[0] && t1[0] < s2[0]) {
                            projectionBounds.AddPoint(new idVec3(s1[0] * dFar, 1.0f, 1.0f));
                            projectionBounds.AddPoint(new idVec3(s2[0] * dFar, 1.0f, 1.0f));
                        }
                    }
                }
            }

            return true;
        }

        /*
         ============
         idFrustum::CullLocalBox

         Tests if any of the planes of the frustum can be used as a separating plane.

         3 muls best case
         25 muls worst case
         ============
         */
        private boolean CullLocalBox(final idVec3 localOrigin, final idVec3 extents, final idMat3 localAxis) {
            float d1, d2;
            idVec3 testOrigin;
            idMat3 testAxis;

            // near plane
            d1 = dNear - localOrigin.x;
            d2 = idMath.Fabs(extents.oGet(0) * localAxis.oGet(0).oGet(0))
                    + idMath.Fabs(extents.oGet(1) * localAxis.oGet(1).oGet(0))
                    + idMath.Fabs(extents.oGet(2) * localAxis.oGet(2).oGet(0));
            if (d1 - d2 > 0.0f) {
                return true;
            }

            // far plane
            d1 = localOrigin.x - dFar;
            if (d1 - d2 > 0.0f) {
                return true;
            }

            testOrigin = localOrigin;
            testAxis = localAxis;

            if (testOrigin.y < 0.0f) {
                testOrigin.y = -testOrigin.y;
                testAxis.oGet(0).oSet(1, -testAxis.oGet(0).oGet(1));
                testAxis.oGet(1).oSet(1, -testAxis.oGet(1).oGet(1));
                testAxis.oGet(0).oSet(1, -testAxis.oGet(2).oGet(1));
            }

            // test left/right planes
            d1 = dFar * testOrigin.y - dLeft * testOrigin.x;
            d2 = idMath.Fabs(extents.oGet(0) * (dFar * testAxis.oGet(0).oGet(1) - dLeft * testAxis.oGet(0).oGet(0)))
                    + idMath.Fabs(extents.oGet(1) * (dFar * testAxis.oGet(1).oGet(1) - dLeft * testAxis.oGet(1).oGet(0)))
                    + idMath.Fabs(extents.oGet(2) * (dFar * testAxis.oGet(2).oGet(1) - dLeft * testAxis.oGet(2).oGet(0)));
            if (d1 - d2 > 0.0f) {
                return true;
            }

            if (testOrigin.z < 0.0f) {
                testOrigin.z = -testOrigin.z;
                testAxis.oGet(0).oSet(2, -testAxis.oGet(0).oGet(2));
                testAxis.oGet(1).oSet(2, -testAxis.oGet(1).oGet(2));
                testAxis.oGet(2).oSet(2, -testAxis.oGet(2).oGet(2));
            }

            // test up/down planes
            d1 = dFar * testOrigin.z - dUp * testOrigin.x;
            d2 = idMath.Fabs(extents.oGet(0) * (dFar * testAxis.oGet(0).oGet(2) - dUp * testAxis.oGet(0).oGet(0)))
                    + idMath.Fabs(extents.oGet(1) * (dFar * testAxis.oGet(1).oGet(2) - dUp * testAxis.oGet(1).oGet(0)))
                    + idMath.Fabs(extents.oGet(2) * (dFar * testAxis.oGet(2).oGet(2) - dUp * testAxis.oGet(2).oGet(0)));
            if (d1 - d2 > 0.0f) {
                return true;
            }

            return false;
        }

        /*
         ============
         idFrustum::CullLocalFrustum

         Tests if any of the planes of this frustum can be used as a separating plane.

         0 muls best case
         30 muls worst case
         ============
         */
        private boolean CullLocalFrustum(final idFrustum localFrustum, final idVec3[] indexPoints, final idVec3[] cornerVecs) {
            int index;
            float dx, dy, dz, leftScale, upScale;

            // test near plane
            dy = -localFrustum.axis.oGet(1).x;
            dz = -localFrustum.axis.oGet(2).x;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = -cornerVecs[index].x;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].x < dNear) {
                return true;
            }

            // test far plane
            dy = localFrustum.axis.oGet(1).x;
            dz = localFrustum.axis.oGet(2).x;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = cornerVecs[index].x;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].x > dFar) {
                return true;
            }

            leftScale = dLeft * invFar;

            // test left plane
            dy = dFar * localFrustum.axis.oGet(1).y - dLeft * localFrustum.axis.oGet(1).x;
            dz = dFar * localFrustum.axis.oGet(2).y - dLeft * localFrustum.axis.oGet(2).x;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = dFar * cornerVecs[index].y - dLeft * cornerVecs[index].x;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].y > indexPoints[index].x * leftScale) {
                return true;
            }

            // test right plane
            dy = -dFar * localFrustum.axis.oGet(1).y - dLeft * localFrustum.axis.oGet(1).x;
            dz = -dFar * localFrustum.axis.oGet(2).y - dLeft * localFrustum.axis.oGet(2).x;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = -dFar * cornerVecs[index].y - dLeft * cornerVecs[index].x;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].y < -indexPoints[index].x * leftScale) {
                return true;
            }

            upScale = dUp * invFar;

            // test up plane
            dy = dFar * localFrustum.axis.oGet(1).z - dUp * localFrustum.axis.oGet(1).x;
            dz = dFar * localFrustum.axis.oGet(2).z - dUp * localFrustum.axis.oGet(2).x;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = dFar * cornerVecs[index].z - dUp * cornerVecs[index].x;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].z > indexPoints[index].x * upScale) {
                return true;
            }

            // test down plane
            dy = -dFar * localFrustum.axis.oGet(1).z - dUp * localFrustum.axis.oGet(1).x;
            dz = -dFar * localFrustum.axis.oGet(2).z - dUp * localFrustum.axis.oGet(2).x;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = -dFar * cornerVecs[index].z - dUp * cornerVecs[index].x;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].z < -indexPoints[index].x * upScale) {
                return true;
            }

            return false;
        }

        private boolean CullLocalWinding(final idVec3[] points, final int numPoints, int[] pointCull) {
            int i, pCull, culled;
            float leftScale, upScale;

            leftScale = dLeft * invFar;
            upScale = dUp * invFar;

            culled = -1;
            for (i = 0; i < numPoints; i++) {
                final idVec3 p = points[i];
                pCull = 0;
                if (p.x < dNear) {
                    pCull = 1;
                } else if (p.x > dFar) {
                    pCull = 2;
                }
                if (idMath.Fabs(p.y) > p.x * leftScale) {
                    pCull |= 4 << FLOATSIGNBITSET(p.y);
                }
                if (idMath.Fabs(p.z) > p.x * upScale) {
                    pCull |= 16 << FLOATSIGNBITSET(p.z);
                }
                culled &= pCull;
                pointCull[i] = pCull;
            }

            return (culled != 0);
        }

        /*
         ============
         idFrustum::BoundsCullLocalFrustum

         Tests if any of the bounding box planes can be used as a separating plane.
         ============
         */
        private boolean BoundsCullLocalFrustum(final idBounds bounds, final idFrustum localFrustum, final idVec3[] indexPoints, final idVec3[] cornerVecs) {
            int index;
            float dx, dy, dz;

            dy = -localFrustum.axis.oGet(1).x;
            dz = -localFrustum.axis.oGet(2).x;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = -cornerVecs[index].x;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].x < bounds.oGet(0).x) {
                return true;
            }

            dy = localFrustum.axis.oGet(1).x;
            dz = localFrustum.axis.oGet(2).x;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = cornerVecs[index].x;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].x > bounds.oGet(1).x) {
                return true;
            }

            dy = -localFrustum.axis.oGet(1).y;
            dz = -localFrustum.axis.oGet(2).y;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = -cornerVecs[index].y;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].y < bounds.oGet(0).y) {
                return true;
            }

            dy = localFrustum.axis.oGet(1).y;
            dz = localFrustum.axis.oGet(2).y;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = cornerVecs[index].y;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].y > bounds.oGet(1).y) {
                return true;
            }

            dy = -localFrustum.axis.oGet(1).z;
            dz = -localFrustum.axis.oGet(2).z;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = -cornerVecs[index].z;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].z < bounds.oGet(0).z) {
                return true;
            }

            dy = localFrustum.axis.oGet(1).z;
            dz = localFrustum.axis.oGet(2).z;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = cornerVecs[index].z;
            index |= (FLOATSIGNBITSET(dx) << 2);

            if (indexPoints[index].z > bounds.oGet(1).z) {
                return true;
            }

            return false;
        }

        /*
         ============
         idFrustum::LocalLineIntersection

         7 divs
         30 muls
         ============
         */
        private boolean LocalLineIntersection(final idVec3 start, final idVec3 end) {
            idVec3 dir;
            float d1, d2, fstart, fend, lstart, lend, f, x;
            float leftScale, upScale;
            int startInside = 1;

            leftScale = dLeft * invFar;
            upScale = dUp * invFar;
            dir = end.oMinus(start);

            // test near plane
            if (dNear > 0.0f) {
                d1 = dNear - start.x;
                startInside &= FLOATSIGNBITSET(d1);
                if (FLOATNOTZERO(d1)) {
                    d2 = dNear - end.x;
                    if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                        f = d1 / (d1 - d2);
                        if (idMath.Fabs(start.y + f * dir.y) <= dNear * leftScale) {
                            if (idMath.Fabs(start.z + f * dir.z) <= dNear * upScale) {
                                return true;
                            }
                        }
                    }
                }
            }

            // test far plane
            d1 = start.x - dFar;
            startInside &= FLOATSIGNBITSET(d1);
            if (FLOATNOTZERO(d1)) {
                d2 = end.x - dFar;
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    if (idMath.Fabs(start.y + f * dir.y) <= dFar * leftScale) {
                        if (idMath.Fabs(start.z + f * dir.z) <= dFar * upScale) {
                            return true;
                        }
                    }
                }
            }

            fstart = dFar * start.y;
            fend = dFar * end.y;
            lstart = dLeft * start.x;
            lend = dLeft * end.x;

            // test left plane
            d1 = fstart - lstart;
            startInside &= FLOATSIGNBITSET(d1);
            if (FLOATNOTZERO(d1)) {
                d2 = fend - lend;
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    x = start.x + f * dir.x;
                    if (x >= dNear && x <= dFar) {
                        if (idMath.Fabs(start.z + f * dir.z) <= x * upScale) {
                            return true;
                        }
                    }
                }
            }

            // test right plane
            d1 = -fstart - lstart;
            startInside &= FLOATSIGNBITSET(d1);
            if (FLOATNOTZERO(d1)) {
                d2 = -fend - lend;
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    x = start.x + f * dir.x;
                    if (x >= dNear && x <= dFar) {
                        if (idMath.Fabs(start.z + f * dir.z) <= x * upScale) {
                            return true;
                        }
                    }
                }
            }

            fstart = dFar * start.z;
            fend = dFar * end.z;
            lstart = dUp * start.x;
            lend = dUp * end.x;

            // test up plane
            d1 = fstart - lstart;
            startInside &= FLOATSIGNBITSET(d1);
            if (FLOATNOTZERO(d1)) {
                d2 = fend - lend;
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    x = start.x + f * dir.x;
                    if (x >= dNear && x <= dFar) {
                        if (idMath.Fabs(start.y + f * dir.y) <= x * leftScale) {
                            return true;
                        }
                    }
                }
            }

            // test down plane
            d1 = -fstart - lstart;
            startInside &= FLOATSIGNBITSET(d1);
            if (FLOATNOTZERO(d1)) {
                d2 = -fend - lend;
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    x = start.x + f * dir.x;
                    if (x >= dNear && x <= dFar) {
                        if (idMath.Fabs(start.y + f * dir.y) <= x * leftScale) {
                            return true;
                        }
                    }
                }
            }

            return (startInside != 0);
        }

        /*
         ============
         idFrustum::LocalRayIntersection

         Returns true if the ray starts inside the frustum.
         If there was an intersection scale1 <= scale2
         ============
         */
        private boolean LocalRayIntersection(final idVec3 start, final idVec3 dir, float[] scale1, float[] scale2) {
            idVec3 end;
            float d1, d2, fstart, fend, lstart, lend, f, x;
            float leftScale, upScale;
            int startInside = 1;

            leftScale = dLeft * invFar;
            upScale = dUp * invFar;
            end = start.oPlus(dir);

            scale1[0] = idMath.INFINITY;
            scale2[0] = -idMath.INFINITY;

            // test near plane
            if (dNear > 0.0f) {
                d1 = dNear - start.x;
                startInside &= FLOATSIGNBITSET(d1);
                d2 = dNear - end.x;
                if (d1 != d2) {
                    f = d1 / (d1 - d2);
                    if (idMath.Fabs(start.y + f * dir.y) <= dNear * leftScale) {
                        if (idMath.Fabs(start.z + f * dir.z) <= dNear * upScale) {
                            if (f < scale1[0]) {
                                scale1[0] = f;
                            }
                            if (f > scale2[0]) {
                                scale2[0] = f;
                            }
                        }
                    }
                }
            }

            // test far plane
            d1 = start.x - dFar;
            startInside &= FLOATSIGNBITSET(d1);
            d2 = end.x - dFar;
            if (d1 != d2) {
                f = d1 / (d1 - d2);
                if (idMath.Fabs(start.y + f * dir.y) <= dFar * leftScale) {
                    if (idMath.Fabs(start.z + f * dir.z) <= dFar * upScale) {
                        if (f < scale1[0]) {
                            scale1[0] = f;
                        }
                        if (f > scale2[0]) {
                            scale2[0] = f;
                        }
                    }
                }
            }

            fstart = dFar * start.y;
            fend = dFar * end.y;
            lstart = dLeft * start.x;
            lend = dLeft * end.x;

            // test left plane
            d1 = fstart - lstart;
            startInside &= FLOATSIGNBITSET(d1);
            d2 = fend - lend;
            if (d1 != d2) {
                f = d1 / (d1 - d2);
                x = start.x + f * dir.x;
                if (x >= dNear && x <= dFar) {
                    if (idMath.Fabs(start.z + f * dir.z) <= x * upScale) {
                        if (f < scale1[0]) {
                            scale1[0] = f;
                        }
                        if (f > scale2[0]) {
                            scale2[0] = f;
                        }
                    }
                }
            }

            // test right plane
            d1 = -fstart - lstart;
            startInside &= FLOATSIGNBITSET(d1);
            d2 = -fend - lend;
            if (d1 != d2) {
                f = d1 / (d1 - d2);
                x = start.x + f * dir.x;
                if (x >= dNear && x <= dFar) {
                    if (idMath.Fabs(start.z + f * dir.z) <= x * upScale) {
                        if (f < scale1[0]) {
                            scale1[0] = f;
                        }
                        if (f > scale2[0]) {
                            scale2[0] = f;
                        }
                    }
                }
            }

            fstart = dFar * start.z;
            fend = dFar * end.z;
            lstart = dUp * start.x;
            lend = dUp * end.x;

            // test up plane
            d1 = fstart - lstart;
            startInside &= FLOATSIGNBITSET(d1);
            d2 = fend - lend;
            if (d1 != d2) {
                f = d1 / (d1 - d2);
                x = start.x + f * dir.x;
                if (x >= dNear && x <= dFar) {
                    if (idMath.Fabs(start.y + f * dir.y) <= x * leftScale) {
                        if (f < scale1[0]) {
                            scale1[0] = f;
                        }
                        if (f > scale2[0]) {
                            scale2[0] = f;
                        }
                    }
                }
            }

            // test down plane
            d1 = -fstart - lstart;
            startInside &= FLOATSIGNBITSET(d1);
            d2 = -fend - lend;
            if (d1 != d2) {
                f = d1 / (d1 - d2);
                x = start.x + f * dir.x;
                if (x >= dNear && x <= dFar) {
                    if (idMath.Fabs(start.y + f * dir.y) <= x * leftScale) {
                        if (f < scale1[0]) {
                            scale1[0] = f;
                        }
                        if (f > scale2[0]) {
                            scale2[0] = f;
                        }
                    }
                }
            }

            return (startInside != 0);
        }

        private boolean LocalFrustumIntersectsFrustum(final idVec3[] points, final boolean testFirstSide) {
            int i;

            // test if any edges of the other frustum intersect this frustum
            for (i = 0; i < 4; i++) {
                if (LocalLineIntersection(points[i], points[4 + i])) {
                    return true;
                }
            }
            if (testFirstSide) {
                for (i = 0; i < 4; i++) {
                    if (LocalLineIntersection(points[i], points[(i + 1) & 3])) {
                        return true;
                    }
                }
            }
            for (i = 0; i < 4; i++) {
                if (LocalLineIntersection(points[4 + i], points[4 + ((i + 1) & 3)])) {
                    return true;
                }
            }

            return false;
        }

        private boolean LocalFrustumIntersectsBounds(final idVec3[] points, final idBounds bounds) {
            int i;

            // test if any edges of the other frustum intersect this frustum
            for (i = 0; i < 4; i++) {
                if (bounds.LineIntersection(points[i], points[4 + i])) {
                    return true;
                }
            }
            if (dNear > 0.0f) {
                for (i = 0; i < 4; i++) {
                    if (bounds.LineIntersection(points[i], points[(i + 1) & 3])) {
                        return true;
                    }
                }
            }
            for (i = 0; i < 4; i++) {
                if (bounds.LineIntersection(points[4 + i], points[4 + ((i + 1) & 3)])) {
                    return true;
                }
            }

            return false;
        }

        private void ToClippedPoints(final float[] fractions, idVec3[] points) {
            idMat3 scaled = new idMat3();

            scaled.oSet(0, origin.oPlus(axis.oGet(0).oMultiply(dNear)));
            scaled.oSet(1, axis.oGet(1).oMultiply((dLeft * dNear * invFar)));
            scaled.oSet(2, axis.oGet(2).oMultiply((dUp * dNear * invFar)));

            points[0] = scaled.oGet(0).oPlus(scaled.oGet(1));
            points[1] = scaled.oGet(0).oMinus(scaled.oGet(1));
            points[2] = points[1].oMinus(scaled.oGet(2));
            points[3] = points[0].oMinus(scaled.oGet(2));
            points[0].oPluSet(scaled.oGet(2));
            points[1].oPluSet(scaled.oGet(2));

            scaled.oSet(0, axis.oGet(0).oMultiply(dFar));
            scaled.oSet(1, axis.oGet(1).oMultiply(dLeft));
            scaled.oSet(2, axis.oGet(2).oMulSet(dUp));

            points[4] = scaled.oGet(0).oPlus(scaled.oGet(1));
            points[5] = scaled.oGet(0).oMinus(scaled.oGet(1));
            points[6] = points[5].oMinus(scaled.oGet(2));
            points[7] = points[4].oMinus(scaled.oGet(2));
            points[4].oPluSet(scaled.oGet(2));
            points[5].oPluSet(scaled.oGet(2));

            points[4] = origin.oPlus(points[4].oMultiply(fractions[0]));
            points[5] = origin.oPlus(points[5].oMultiply(fractions[1]));
            points[6] = origin.oPlus(points[6].oMultiply(fractions[2]));
            points[7] = origin.oPlus(points[7].oMultiply(fractions[3]));
        }

        private void ToIndexPoints(idVec3[] indexPoints) {
            idMat3 scaled = new idMat3();

            scaled.oSet(0, origin.oPlus(axis.oGet(0).oMultiply(dNear)));
            scaled.oSet(1, axis.oGet(1).oMultiply((dLeft * dNear * invFar)));
            scaled.oSet(2, axis.oGet(2).oMultiply((dUp * dNear * invFar)));

            indexPoints[0] = scaled.oGet(0).oMinus(scaled.oGet(1));
            indexPoints[2] = scaled.oGet(0).oPlus(scaled.oGet(1));
            indexPoints[1] = indexPoints[0].oPlus(scaled.oGet(2));
            indexPoints[3] = indexPoints[2].oPlus(scaled.oGet(2));
            indexPoints[0].oMinSet(scaled.oGet(2));
            indexPoints[2].oMinSet(scaled.oGet(2));

            scaled.oSet(0, origin.oPlus(axis.oGet(0).oMultiply(dFar)));
            scaled.oSet(1, axis.oGet(1).oMultiply(dLeft));
            scaled.oSet(2, axis.oGet(2).oMulSet(dUp));

            indexPoints[4] = scaled.oGet(0).oMinus(scaled.oGet(1));
            indexPoints[6] = scaled.oGet(0).oPlus(scaled.oGet(1));
            indexPoints[5] = indexPoints[4].oPlus(scaled.oGet(2));
            indexPoints[7] = indexPoints[6].oPlus(scaled.oGet(2));
            indexPoints[4].oMinSet(scaled.oGet(2));
            indexPoints[6].oMinSet(scaled.oGet(2));
        }

        /*
         ============
         idFrustum::ToIndexPointsAndCornerVecs

         22 muls
         ============
         */
        private void ToIndexPointsAndCornerVecs(idVec3[] indexPoints, idVec3[] cornerVecs) {
            idMat3 scaled = new idMat3();

            scaled.oSet(0, origin.oPlus(axis.oGet(0).oMultiply(dNear)));
            scaled.oSet(1, axis.oGet(1).oMultiply((dLeft * dNear * invFar)));
            scaled.oSet(2, axis.oGet(2).oMultiply((dUp * dNear * invFar)));

            indexPoints[0] = scaled.oGet(0).oMinus(scaled.oGet(1));
            indexPoints[2] = scaled.oGet(0).oPlus(scaled.oGet(1));
            indexPoints[1] = indexPoints[0].oPlus(scaled.oGet(2));
            indexPoints[3] = indexPoints[2].oPlus(scaled.oGet(2));
            indexPoints[0].oMinSet(scaled.oGet(2));
            indexPoints[2].oMinSet(scaled.oGet(2));

            scaled.oSet(0, axis.oGet(0).oMultiply(dFar));
            scaled.oSet(1, axis.oGet(1).oMultiply(dLeft));
            scaled.oSet(2, axis.oGet(2).oMultiply(dUp));

            cornerVecs[0] = scaled.oGet(0).oMinus(scaled.oGet(1));
            cornerVecs[2] = scaled.oGet(0).oPlus(scaled.oGet(1));
            cornerVecs[1] = cornerVecs[0].oPlus(scaled.oGet(2));
            cornerVecs[3] = cornerVecs[2].oPlus(scaled.oGet(2));
            cornerVecs[0].oMinSet(scaled.oGet(2));
            cornerVecs[2].oMinSet(scaled.oGet(2));

            indexPoints[4] = cornerVecs[0].oPlus(origin);
            indexPoints[5] = cornerVecs[1].oPlus(origin);
            indexPoints[6] = cornerVecs[2].oPlus(origin);
            indexPoints[7] = cornerVecs[3].oPlus(origin);
        }

        /*
         ============
         idFrustum::AxisProjection

         18 muls
         ============
         */
        private void AxisProjection(final idVec3 indexPoints[], final idVec3 cornerVecs[], final idVec3 dir, float[] min, float[] max) {
            float dx, dy, dz;
            int index;

            dy = dir.x * axis.oGet(1).x + dir.y * axis.oGet(1).y + dir.z * axis.oGet(1).z;
            dz = dir.x * axis.oGet(2).x + dir.y * axis.oGet(2).y + dir.z * axis.oGet(2).z;
            index = (FLOATSIGNBITSET(dy) << 1) | FLOATSIGNBITSET(dz);
            dx = dir.x * cornerVecs[index].x + dir.y * cornerVecs[index].y + dir.z * cornerVecs[index].z;
            index |= (FLOATSIGNBITSET(dx) << 2);
            min[0] = indexPoints[index].oMultiply(dir);
            index = ~index & 3;
            dx = -dir.x * cornerVecs[index].x - dir.y * cornerVecs[index].y - dir.z * cornerVecs[index].z;
            index |= (FLOATSIGNBITSET(dx) << 2);
            max[0] = indexPoints[index].oMultiply(dir);
        }

        private void AddLocalLineToProjectionBoundsSetCull(final idVec3 start, final idVec3 end, int[] cull, idBounds bounds) {
            int[] cull2 = {0};
            AddLocalLineToProjectionBoundsSetCull(start, end, cull, cull2, bounds);
            cull[1] = cull2[0];
        }

        private void AddLocalLineToProjectionBoundsSetCull(final idVec3 start, final idVec3 end, int[] startCull, int[] endCull, idBounds bounds) {
            idVec3 dir, p = new idVec3();
            float d1, d2, fstart, fend, lstart, lend, f;
            float leftScale, upScale;
            int cull1, cull2;

//#ifdef FRUSTUM_DEBUG
//	static idCVar r_showInteractionScissors( "r_showInteractionScissors", "0", CVAR_RENDERER | CVAR_INTEGER, "", 0, 2, idCmdSystem::ArgCompletion_Integer<0,2> );
//	if ( r_showInteractionScissors.GetInteger() > 1 ) {
//		session->rw->DebugLine( colorGreen, origin + start * axis, origin + end * axis );
//	}
//#endif
            leftScale = dLeft * invFar;
            upScale = dUp * invFar;
            dir = end.oMinus(start);

            fstart = dFar * start.y;
            fend = dFar * end.y;
            lstart = dLeft * start.x;
            lend = dLeft * end.x;

            // test left plane
            d1 = -fstart + lstart;
            d2 = -fend + lend;
            cull1 = FLOATSIGNBITSET(d1);
            cull2 = FLOATSIGNBITSET(d2);
            if (FLOATNOTZERO(d1)) {
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    p.x = start.x + f * dir.x;
                    if (p.x > 0.0f) {
                        p.z = start.z + f * dir.z;
                        if (idMath.Fabs(p.z) <= p.x * upScale) {
                            p.y = 1.0f;
                            p.z = p.z * dFar / (p.x * dUp);
                            bounds.AddPoint(p);
                        }
                    }
                }
            }

            // test right plane
            d1 = fstart + lstart;
            d2 = fend + lend;
            cull1 |= FLOATSIGNBITSET(d1) << 1;
            cull2 |= FLOATSIGNBITSET(d2) << 1;
            if (FLOATNOTZERO(d1)) {
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    p.x = start.x + f * dir.x;
                    if (p.x > 0.0f) {
                        p.z = start.z + f * dir.z;
                        if (idMath.Fabs(p.z) <= p.x * upScale) {
                            p.y = -1.0f;
                            p.z = p.z * dFar / (p.x * dUp);
                            bounds.AddPoint(p);
                        }
                    }
                }
            }

            fstart = dFar * start.z;
            fend = dFar * end.z;
            lstart = dUp * start.x;
            lend = dUp * end.x;

            // test up plane
            d1 = -fstart + lstart;
            d2 = -fend + lend;
            cull1 |= FLOATSIGNBITSET(d1) << 2;
            cull2 |= FLOATSIGNBITSET(d2) << 2;
            if (FLOATNOTZERO(d1)) {
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    p.x = start.x + f * dir.x;
                    if (p.x > 0.0f) {
                        p.y = start.y + f * dir.y;
                        if (idMath.Fabs(p.y) <= p.x * leftScale) {
                            p.y = p.y * dFar / (p.x * dLeft);
                            p.z = 1.0f;
                            bounds.AddPoint(p);
                        }
                    }
                }
            }

            // test down plane
            d1 = fstart + lstart;
            d2 = fend + lend;
            cull1 |= FLOATSIGNBITSET(d1) << 3;
            cull2 |= FLOATSIGNBITSET(d2) << 3;
            if (FLOATNOTZERO(d1)) {
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    p.x = start.x + f * dir.x;
                    if (p.x > 0.0f) {
                        p.y = start.y + f * dir.y;
                        if (idMath.Fabs(p.y) <= p.x * leftScale) {
                            p.y = p.y * dFar / (p.x * dLeft);
                            p.z = -1.0f;
                            bounds.AddPoint(p);
                        }
                    }
                }
            }

            if (cull1 == 0 && start.x > 0.0f) {
                // add start point to projection bounds
                p.x = start.x;
                p.y = start.y * dFar / (start.x * dLeft);
                p.z = start.z * dFar / (start.x * dUp);
                bounds.AddPoint(p);
            }

            if (cull2 == 0 && end.x > 0.0f) {
                // add end point to projection bounds
                p.x = end.x;
                p.y = end.y * dFar / (end.x * dLeft);
                p.z = end.z * dFar / (end.x * dUp);
                bounds.AddPoint(p);
            }

            if (start.x < bounds.oGet(0).x) {
                bounds.oGet(0).x = start.x < 0.0f ? 0.0f : start.x;
            }
            if (end.x < bounds.oGet(0).x) {
                bounds.oGet(0).x = end.x < 0.0f ? 0.0f : end.x;
            }

            startCull[0] = cull1;
            endCull[0] = cull2;
        }

        private void AddLocalLineToProjectionBoundsUseCull(final idVec3 start, final idVec3 end, int startCull, int endCull, idBounds bounds) {
            idVec3 dir, p = new idVec3();
            float d1, d2, fstart, fend, lstart, lend, f;
            float leftScale, upScale;
            int clip;

            clip = startCull ^ endCull;
            if (0 == clip) {
                return;
            }

//#ifdef FRUSTUM_DEBUG
//	static idCVar r_showInteractionScissors( "r_showInteractionScissors", "0", CVAR_RENDERER | CVAR_INTEGER, "", 0, 2, idCmdSystem.ArgCompletion_Integer<0,2> );
//	if ( r_showInteractionScissors.GetInteger() > 1 ) {
//		session->rw->DebugLine( colorGreen, origin + start * axis, origin + end * axis );
//	}
//#endif
            leftScale = dLeft * invFar;
            upScale = dUp * invFar;
            dir = end.oMinus(start);

            if ((clip & (1 | 2)) != 0) {

                fstart = dFar * start.y;
                fend = dFar * end.y;
                lstart = dLeft * start.x;
                lend = dLeft * end.x;

                if ((clip & 1) != 0) {
                    // test left plane
                    d1 = -fstart + lstart;
                    d2 = -fend + lend;
                    if (FLOATNOTZERO(d1)) {
                        if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                            f = d1 / (d1 - d2);
                            p.x = start.x + f * dir.x;
                            if (p.x > 0.0f) {
                                p.z = start.z + f * dir.z;
                                if (idMath.Fabs(p.z) <= p.x * upScale) {
                                    p.y = 1.0f;
                                    p.z = p.z * dFar / (p.x * dUp);
                                    bounds.AddPoint(p);
                                }
                            }
                        }
                    }
                }

                if ((clip & 2) != 0) {
                    // test right plane
                    d1 = fstart + lstart;
                    d2 = fend + lend;
                    if (FLOATNOTZERO(d1)) {
                        if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                            f = d1 / (d1 - d2);
                            p.x = start.x + f * dir.x;
                            if (p.x > 0.0f) {
                                p.z = start.z + f * dir.z;
                                if (idMath.Fabs(p.z) <= p.x * upScale) {
                                    p.y = -1.0f;
                                    p.z = p.z * dFar / (p.x * dUp);
                                    bounds.AddPoint(p);
                                }
                            }
                        }
                    }
                }
            }

            if ((clip & (4 | 8)) != 0) {

                fstart = dFar * start.z;
                fend = dFar * end.z;
                lstart = dUp * start.x;
                lend = dUp * end.x;

                if ((clip & 4) != 0) {
                    // test up plane
                    d1 = -fstart + lstart;
                    d2 = -fend + lend;
                    if (FLOATNOTZERO(d1)) {
                        if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                            f = d1 / (d1 - d2);
                            p.x = start.x + f * dir.x;
                            if (p.x > 0.0f) {
                                p.y = start.y + f * dir.y;
                                if (idMath.Fabs(p.y) <= p.x * leftScale) {
                                    p.y = p.y * dFar / (p.x * dLeft);
                                    p.z = 1.0f;
                                    bounds.AddPoint(p);
                                }
                            }
                        }
                    }
                }

                if ((clip & 8) != 0) {
                    // test down plane
                    d1 = fstart + lstart;
                    d2 = fend + lend;
                    if (FLOATNOTZERO(d1)) {
                        if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                            f = d1 / (d1 - d2);
                            p.x = start.x + f * dir.x;
                            if (p.x > 0.0f) {
                                p.y = start.y + f * dir.y;
                                if (idMath.Fabs(p.y) <= p.x * leftScale) {
                                    p.y = p.y * dFar / (p.x * dLeft);
                                    p.z = -1.0f;
                                    bounds.AddPoint(p);
                                }
                            }
                        }
                    }
                }
            }
        }
        static final int[][] capPointIndex = {
            {0, 3},
            {1, 2},
            {0, 1},
            {2, 3}};

        private boolean AddLocalCapsToProjectionBounds(final idVec3[] endPoints, final int endPointsOffset, final int[] endPointCull, final int endPointCullOffset, final idVec3 point, int pointCull, int pointClip, idBounds projectionBounds) {
            int[] p;

            if (pointClip < 0) {
                return false;
            }
            p = capPointIndex[pointClip];
            AddLocalLineToProjectionBoundsUseCull(endPoints[endPointsOffset + p[0]], point, endPointCull[endPointCullOffset + p[0]], pointCull, projectionBounds);
            AddLocalLineToProjectionBoundsUseCull(endPoints[endPointsOffset + p[1]], point, endPointCull[endPointCullOffset + p[1]], pointCull, projectionBounds);
            return true;
        }

        private boolean AddLocalCapsToProjectionBounds(final idVec3[] endPoints, final int[] endPointCull, final idVec3 point, int pointCull, int pointClip, idBounds projectionBounds) {
            return AddLocalCapsToProjectionBounds(endPoints, 0, endPointCull, 0, point, pointCull, pointClip, projectionBounds);
        }

        /*
         ============
         idFrustum::BoundsRayIntersection

         Returns true if the ray starts inside the bounds.
         If there was an intersection scale1 <= scale2
         ============
         */
        private boolean BoundsRayIntersection(final idBounds bounds, final idVec3 start, final idVec3 dir, float[] scale1, float[] scale2) {
            idVec3 end, p = new idVec3();
            float d1, d2, f;
            int i, startInside = 1;

            scale1[0] = idMath.INFINITY;
            scale2[0] = -idMath.INFINITY;

            end = start.oPlus(dir);

            for (i = 0; i < 2; i++) {
                d1 = start.x - bounds.oGet(i).x;
                startInside &= FLOATSIGNBITSET(d1) ^ i;
                d2 = end.x - bounds.oGet(i).x;
                if (d1 != d2) {
                    f = d1 / (d1 - d2);
                    p.y = start.y + f * dir.y;
                    if (bounds.oGet(0).y <= p.y && p.y <= bounds.oGet(1).y) {
                        p.z = start.z + f * dir.z;
                        if (bounds.oGet(0).z <= p.z && p.z <= bounds.oGet(1).z) {
                            if (f < scale1[0]) {
                                scale1[0] = f;
                            }
                            if (f > scale2[0]) {
                                scale2[0] = f;
                            }
                        }
                    }
                }

                d1 = start.y - bounds.oGet(i).y;
                startInside &= FLOATSIGNBITSET(d1) ^ i;
                d2 = end.y - bounds.oGet(i).y;
                if (d1 != d2) {
                    f = d1 / (d1 - d2);
                    p.x = start.x + f * dir.x;
                    if (bounds.oGet(0).x <= p.x && p.x <= bounds.oGet(1).x) {
                        p.z = start.z + f * dir.z;
                        if (bounds.oGet(0).z <= p.z && p.z <= bounds.oGet(1).z) {
                            if (f < scale1[0]) {
                                scale1[0] = f;
                            }
                            if (f > scale2[0]) {
                                scale2[0] = f;
                            }
                        }
                    }
                }

                d1 = start.z - bounds.oGet(i).z;
                startInside &= FLOATSIGNBITSET(d1) ^ i;
                d2 = end.z - bounds.oGet(i).z;
                if (d1 != d2) {
                    f = d1 / (d1 - d2);
                    p.x = start.x + f * dir.x;
                    if (bounds.oGet(0).x <= p.x && p.x <= bounds.oGet(1).x) {
                        p.y = start.y + f * dir.y;
                        if (bounds.oGet(0).y <= p.y && p.y <= bounds.oGet(1).y) {
                            if (f < scale1[0]) {
                                scale1[0] = f;
                            }
                            if (f > scale2[0]) {
                                scale2[0] = f;
                            }
                        }
                    }
                }
            }

            return (startInside != 0);
        }

        /*
         ============
         idFrustum::ClipFrustumToBox

         Clips the frustum far extents to the box.
         ============
         */
        private void ClipFrustumToBox(final idBox box, float[] clipFractions, int[] clipPlanes) {
            int i, index;
            float f, minf;
            idMat3 scaled = new idMat3(), localAxis, transpose;
            idVec3 localOrigin;
            idVec3[] cornerVecs = new idVec3[4];
            idBounds bounds = new idBounds();

            transpose = box.GetAxis();
            transpose.TransposeSelf();
            localOrigin = (origin.oMinus(box.GetCenter())).oMultiply(transpose);
            localAxis = axis.oMultiply(transpose);

            scaled.oSet(0, localAxis.oGet(0).oMultiply(dFar));
            scaled.oSet(1, localAxis.oGet(1).oMultiply(dLeft));
            scaled.oSet(2, localAxis.oGet(2).oMultiply(dUp));
            cornerVecs[0] = scaled.oGet(0).oPlus(scaled.oGet(1));
            cornerVecs[1] = scaled.oGet(0).oMinus(scaled.oGet(1));
            cornerVecs[2] = cornerVecs[1].oMinus(scaled.oGet(2));
            cornerVecs[3] = cornerVecs[0].oMinus(scaled.oGet(2));
            cornerVecs[0].oPluSet(scaled.oGet(2));
            cornerVecs[1].oPluSet(scaled.oGet(2));

            bounds.oSet(0, box.GetExtents().oNegative());
            bounds.oSet(1, box.GetExtents());

            minf = (dNear + 1.0f) * invFar;

            for (i = 0; i < 4; i++) {

                index = FLOATSIGNBITNOTSET(cornerVecs[i].x);
                f = (bounds.oGet(index).x - localOrigin.x) / cornerVecs[i].x;
                clipFractions[i] = f;
                clipPlanes[i] = 1 << index;

                index = FLOATSIGNBITNOTSET(cornerVecs[i].y);
                f = (bounds.oGet(index).y - localOrigin.y) / cornerVecs[i].y;
                if (f < clipFractions[i]) {
                    clipFractions[i] = f;
                    clipPlanes[i] = 4 << index;
                }

                index = FLOATSIGNBITNOTSET(cornerVecs[i].z);
                f = (bounds.oGet(index).z - localOrigin.z) / cornerVecs[i].z;
                if (f < clipFractions[i]) {
                    clipFractions[i] = f;
                    clipPlanes[i] = 16 << index;
                }

                // make sure the frustum is not clipped between the frustum origin and the near plane
                if (clipFractions[i] < minf) {
                    clipFractions[i] = minf;
                }
            }
        }

        /*
         ============
         idFrustum::ClipLine

         Returns true if part of the line is inside the frustum.
         Does not clip to the near and far plane.
         ============
         */
        private boolean ClipLine(final idVec3[] localPoints, final idVec3[] points, int startIndex, int endIndex, idVec3 start, idVec3 end, int[] startClip, int[] endClip) {
            float d1, d2, fstart, fend, lstart, lend, f, x;
            float leftScale, upScale;
            float scale1, scale2;
            int startCull, endCull;
            idVec3 localStart, localEnd, localDir;

            leftScale = dLeft * invFar;
            upScale = dUp * invFar;

            localStart = localPoints[startIndex];
            localEnd = localPoints[endIndex];
            localDir = localEnd.oMinus(localStart);

            startClip[0] = endClip[0] = -1;
            scale1 = idMath.INFINITY;
            scale2 = -idMath.INFINITY;

            fstart = dFar * localStart.y;
            fend = dFar * localEnd.y;
            lstart = dLeft * localStart.x;
            lend = dLeft * localEnd.x;

            // test left plane
            d1 = -fstart + lstart;
            d2 = -fend + lend;
            startCull = FLOATSIGNBITSET(d1);
            endCull = FLOATSIGNBITSET(d2);
            if (FLOATNOTZERO(d1)) {
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    x = localStart.x + f * localDir.x;
                    if (x >= 0.0f) {
                        if (idMath.Fabs(localStart.z + f * localDir.z) <= x * upScale) {
                            if (f < scale1) {
                                scale1 = f;
                                startClip[0] = 0;
                            }
                            if (f > scale2) {
                                scale2 = f;
                                endClip[0] = 0;
                            }
                        }
                    }
                }
            }

            // test right plane
            d1 = fstart + lstart;
            d2 = fend + lend;
            startCull |= FLOATSIGNBITSET(d1) << 1;
            endCull |= FLOATSIGNBITSET(d2) << 1;
            if (FLOATNOTZERO(d1)) {
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    x = localStart.x + f * localDir.x;
                    if (x >= 0.0f) {
                        if (idMath.Fabs(localStart.z + f * localDir.z) <= x * upScale) {
                            if (f < scale1) {
                                scale1 = f;
                                startClip[0] = 1;
                            }
                            if (f > scale2) {
                                scale2 = f;
                                endClip[0] = 1;
                            }
                        }
                    }
                }
            }

            fstart = dFar * localStart.z;
            fend = dFar * localEnd.z;
            lstart = dUp * localStart.x;
            lend = dUp * localEnd.x;

            // test up plane
            d1 = -fstart + lstart;
            d2 = -fend + lend;
            startCull |= FLOATSIGNBITSET(d1) << 2;
            endCull |= FLOATSIGNBITSET(d2) << 2;
            if (FLOATNOTZERO(d1)) {
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    x = localStart.x + f * localDir.x;
                    if (x >= 0.0f) {
                        if (idMath.Fabs(localStart.y + f * localDir.y) <= x * leftScale) {
                            if (f < scale1) {
                                scale1 = f;
                                startClip[0] = 2;
                            }
                            if (f > scale2) {
                                scale2 = f;
                                endClip[0] = 2;
                            }
                        }
                    }
                }
            }

            // test down plane
            d1 = fstart + lstart;
            d2 = fend + lend;
            startCull |= FLOATSIGNBITSET(d1) << 3;
            endCull |= FLOATSIGNBITSET(d2) << 3;
            if (FLOATNOTZERO(d1)) {
                if ((FLOATSIGNBITSET(d1) ^ FLOATSIGNBITSET(d2)) != 0) {
                    f = d1 / (d1 - d2);
                    x = localStart.x + f * localDir.x;
                    if (x >= 0.0f) {
                        if (idMath.Fabs(localStart.y + f * localDir.y) <= x * leftScale) {
                            if (f < scale1) {
                                scale1 = f;
                                startClip[0] = 3;
                            }
                            if (f > scale2) {
                                scale2 = f;
                                endClip[0] = 3;
                            }
                        }
                    }
                }
            }

            // if completely inside
            if (0 == (startCull | endCull)) {
                start.oSet(points[startIndex]);
                end.oSet(points[endIndex]);
                return true;
            } else if (scale1 <= scale2) {
                if (0 == startCull) {
                    start.oSet(points[startIndex]);
                    startClip[0] = -1;
                } else {
                    start.oSet(points[startIndex].oPlus(points[endIndex].oMinus(points[startIndex])).oMultiply(scale1));
                }
                if (0 == endCull) {
                    end.oSet(points[endIndex]);
                    endClip[0] = -1;
                } else {
                    end.oSet(points[startIndex].oPlus(points[endIndex].oMinus(points[startIndex])).oMultiply(scale2));
                }
                return true;
            }
            return false;
        }

        private void oSet(idFrustum f) {
            this.origin = new idVec3(f.origin);
            this.axis = new idMat3(f.axis);
            this.dNear = f.dNear;
            this.dFar = f.dFar;
            this.dLeft = f.dLeft;
            this.dUp = f.dUp;
            this.invFar = f.invFar;
        }
    };
}
