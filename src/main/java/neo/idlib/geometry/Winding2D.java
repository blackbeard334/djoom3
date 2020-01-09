package neo.idlib.geometry;

import static neo.idlib.Lib.MAX_WORLD_COORD;
import static neo.idlib.Lib.MIN_WORLD_COORD;
import static neo.idlib.containers.List.idSwap;
import static neo.idlib.math.Math_h.FLOATSIGNBITNOTSET;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Plane.SIDE_BACK;
import static neo.idlib.math.Plane.SIDE_CROSS;
import static neo.idlib.math.Plane.SIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_ON;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;

import java.util.stream.Stream;

/**
 *
 */
public class Winding2D {

    static boolean GetAxialBevel(final idVec3 plane1, final idVec3 plane2, final idVec2 point, idVec3 bevel) {
        if ((FLOATSIGNBITSET(plane1.x) ^ FLOATSIGNBITSET(plane2.x)) != 0) {
            if (idMath.Fabs(plane1.x) > 0.1f && idMath.Fabs(plane2.x) > 0.1f) {
                bevel.x = 0.0f;
                if (FLOATSIGNBITSET(plane1.y) != 0) {
                    bevel.y = -1.0f;
                } else {
                    bevel.y = 1.0f;
                }
                bevel.z = -(point.x * bevel.x + point.y * bevel.y);
                return true;
            }
        }
        if ((FLOATSIGNBITSET(plane1.y) ^ FLOATSIGNBITSET(plane2.y)) != 0) {
            if (idMath.Fabs(plane1.y) > 0.1f && idMath.Fabs(plane2.y) > 0.1f) {
                bevel.y = 0.0f;
                if (FLOATSIGNBITSET(plane1.x) != 0) {
                    bevel.x = -1.0f;
                } else {
                    bevel.x = 1.0f;
                }
                bevel.z = -(point.x * bevel.x + point.y * bevel.y);
                return true;
            }
        }
        return false;
    }

    /*
     ===============================================================================

     A 2D winding is an arbitrary convex 2D polygon defined by an array of points.

     ===============================================================================
     */
    static final int MAX_POINTS_ON_WINDING_2D = 16;

    public static class idWinding2D {

        private int numPoints;
        private idVec2[] p = Stream.generate(idVec2::new).limit(MAX_POINTS_ON_WINDING_2D).toArray(idVec2[]::new);
        //
        //

        public idWinding2D() {
            numPoints = 0;
        }

        public idWinding2D oSet(final idWinding2D winding) {
            int i;

            for (i = 0; i < winding.numPoints; i++) {
                p[i] = winding.p[i];
            }
            numPoints = winding.numPoints;
            return this;
        }

//public	final idVec2 	operator[]( final int index ) ;
        public idVec2 oGet(final int index) {
            return p[ index];
        }

        public idVec2 oSet(final int index, idVec2 value) {
            return p[index] = value;
        }

        public idVec2 oMinSet(final int index, idVec2 value) {
            return p[index].oMinSet(value);
        }

        public idVec2 oPluSet(final int index, idVec2 value) {
            return p[index].oPluSet(value);
        }

        public void Clear() {
            numPoints = 0;
        }

        public void AddPoint(final idVec2 point) {
            p[numPoints++] = point;
        }

        public int GetNumPoints() {
            return numPoints;
        }

        public void Expand(final float d) {
            int i;
            idVec2[] edgeNormals = new idVec2[MAX_POINTS_ON_WINDING_2D];

            for (i = 0; i < numPoints; i++) {
                idVec2 start = p[i];
                idVec2 end = p[(i + 1) % numPoints];
                edgeNormals[i].x = start.y - end.y;
                edgeNormals[i].y = end.x - start.x;
                edgeNormals[i].Normalize();
                edgeNormals[i].oMulSet(d);
            }

            for (i = 0; i < numPoints; i++) {
                p[i].oPluSet(edgeNormals[i].oPlus(edgeNormals[(i + numPoints - 1) % numPoints]));
            }
        }

        public void ExpandForAxialBox(final idVec2 bounds[]) {
            int i, j, numPlanes;
            idVec2 v = new idVec2();
            idVec3[] planes = new idVec3[MAX_POINTS_ON_WINDING_2D];
            idVec3 plane, bevel = new idVec3();

            // get planes for the edges and add bevels
            for (numPlanes = i = 0; i < numPoints; i++) {
                j = (i + 1) % numPoints;
                if ((p[j].oMinus(p[i])).LengthSqr() < 0.01f) {
                    continue;
                }
                plane = Plane2DFromPoints(p[i], p[j], true);
                if (i != 0) {
                    if (GetAxialBevel(planes[numPlanes - 1], plane, p[i], bevel)) {
                        planes[numPlanes++] = bevel;
                    }
                }
                assert (numPlanes < MAX_POINTS_ON_WINDING_2D);
                planes[numPlanes++] = plane;
            }
            if (GetAxialBevel(planes[numPlanes - 1], planes[0], p[0], bevel)) {
                planes[numPlanes++] = bevel;
            }

            // expand the planes
            for (i = 0; i < numPlanes; i++) {
                v.x = bounds[ FLOATSIGNBITSET(planes[i].x)].x;
                v.y = bounds[ FLOATSIGNBITSET(planes[i].y)].y;
                planes[i].z += v.x * planes[i].x + v.y * planes[i].y;
            }

            // get intersection points of the planes
            for (numPoints = i = 0; i < numPlanes; i++) {
                if (Plane2DIntersection(planes[(i + numPlanes - 1) % numPlanes], planes[i], p[numPoints])) {
                    numPoints++;
                }
            }
        }

        // splits the winding into a front and back winding, the winding itself stays unchanged
        // returns a SIDE_?
        public int Split(final idVec3 plane, final float epsilon, idWinding2D[][] front, idWinding2D[][] back) {
            float[] dists = new float[MAX_POINTS_ON_WINDING_2D];
            byte[] sides = new byte[MAX_POINTS_ON_WINDING_2D];
            int[] counts = new int[3];
            float dot;
            int i, j;
            idVec2 p1, p2;
            idVec2 mid = new idVec2();
            idWinding2D f;
            idWinding2D b;
            int maxpts;

            counts[0] = counts[1] = counts[2] = 0;

            // determine sides for each point
            for (i = 0; i < numPoints; i++) {
                dists[i] = dot = plane.x * p[i].x + plane.y * p[i].y + plane.z;
                if (dot > epsilon) {
                    sides[i] = SIDE_FRONT;
                } else if (dot < -epsilon) {
                    sides[i] = SIDE_BACK;
                } else {
                    sides[i] = SIDE_ON;
                }
                counts[sides[i]]++;
            }
            sides[i] = sides[0];
            dists[i] = dists[0];

            front[0] = back[0] = new idWinding2D[1];//TODO:check double pointers

            // if nothing at the front of the clipping plane
            if (0 == counts[SIDE_FRONT]) {
                back[0][0] = Copy();
                return SIDE_BACK;
            }
            // if nothing at the back of the clipping plane
            if (0 == counts[SIDE_BACK]) {
                front[0][0] = Copy();
                return SIDE_FRONT;
            }

            maxpts = numPoints + 4;	// cant use counts[0]+2 because of fp grouping errors

            front[0][0] = f = new idWinding2D();
            back[0][0] = b = new idWinding2D();

            for (i = 0; i < numPoints; i++) {
                p1 = p[i];

                if (sides[i] == SIDE_ON) {
                    f.p[f.numPoints] = p1;
                    f.numPoints++;
                    b.p[b.numPoints] = p1;
                    b.numPoints++;
                    continue;
                }

                if (sides[i] == SIDE_FRONT) {
                    f.p[f.numPoints] = p1;
                    f.numPoints++;
                }

                if (sides[i] == SIDE_BACK) {
                    b.p[b.numPoints] = p1;
                    b.numPoints++;
                }

                if (sides[i + 1] == SIDE_ON || sides[i + 1] == sides[i]) {
                    continue;
                }

                // generate a split point
                p2 = p[(i + 1) % numPoints];

                // always calculate the split going from the same side
                // or minor epsilon issues can happen
                if (sides[i] == SIDE_FRONT) {
                    dot = dists[i] / (dists[i] - dists[i + 1]);
                    for (j = 0; j < 2; j++) {
                        // avoid round off error when possible
                        if (plane.oGet(j) == 1.0f) {
                            mid.oSet(j, plane.z);
                        } else if (plane.oGet(j) == -1.0f) {
                            mid.oSet(j, -plane.z);
                        } else {
                            mid.oSet(j, p1.oGet(j) + dot * (p2.oGet(j) - p1.oGet(j)));
                        }
                    }
                } else {
                    dot = dists[i + 1] / (dists[i + 1] - dists[i]);
                    for (j = 0; j < 2; j++) {
                        // avoid round off error when possible
                        if (plane.oGet(j) == 1.0f) {
                            mid.oSet(j, plane.z);
                        } else if (plane.oGet(j) == -1.0f) {
                            mid.oSet(j, -plane.z);
                        } else {
                            mid.oSet(j, p2.oGet(j) + dot * (p1.oGet(j) - p2.oGet(j)));
                        }
                    }
                }

                f.p[f.numPoints] = mid;
                f.numPoints++;
                b.p[b.numPoints] = mid;
                b.numPoints++;
            }

            return SIDE_CROSS;
        }

        // cuts off the part at the back side of the plane, returns true if some part was at the front
		// if there is nothing at the front the number of points is set to zero
        public boolean ClipInPlace(final idVec3 plane, final float epsilon, final boolean keepOn) {
            int i, j, maxpts, newNumPoints;
            int[] sides = new int[MAX_POINTS_ON_WINDING_2D + 1], counts = new int[3];
            float dot;
            float[] dists = new float[MAX_POINTS_ON_WINDING_2D + 1];
            idVec2 p1, p2, mid = new idVec2();
            idVec2[] newPoints = new idVec2[MAX_POINTS_ON_WINDING_2D + 4];

            counts[SIDE_FRONT] = counts[SIDE_BACK] = counts[SIDE_ON] = 0;

            for (i = 0; i < numPoints; i++) {
                dists[i] = dot = plane.x * p[i].x + plane.y * p[i].y + plane.z;
                if (dot > epsilon) {
                    sides[i] = SIDE_FRONT;
                } else if (dot < -epsilon) {
                    sides[i] = SIDE_BACK;
                } else {
                    sides[i] = SIDE_ON;
                }
                counts[sides[i]]++;
            }
            sides[i] = sides[0];
            dists[i] = dists[0];

            // if the winding is on the plane and we should keep it
            if (keepOn && 0 == counts[SIDE_FRONT] && 0 == counts[SIDE_BACK]) {
                return true;
            }
            if (0 == counts[SIDE_FRONT]) {
                numPoints = 0;
                return false;
            }
            if (0 == counts[SIDE_BACK]) {
                return true;
            }

            maxpts = numPoints + 4;		// cant use counts[0]+2 because of fp grouping errors
            newNumPoints = 0;

            for (i = 0; i < numPoints; i++) {
                p1 = p[i];

                if (newNumPoints + 1 > maxpts) {
                    return true;		// can't split -- fall back to original
                }

                if (sides[i] == SIDE_ON) {
                    newPoints[newNumPoints] = p1;
                    newNumPoints++;
                    continue;
                }

                if (sides[i] == SIDE_FRONT) {
                    newPoints[newNumPoints] = p1;
                    newNumPoints++;
                }

                if (sides[i + 1] == SIDE_ON || sides[i + 1] == sides[i]) {
                    continue;
                }

                if (newNumPoints + 1 > maxpts) {
                    return true;		// can't split -- fall back to original
                }

                // generate a split point
                p2 = p[(i + 1) % numPoints];

                dot = dists[i] / (dists[i] - dists[i + 1]);
                for (j = 0; j < 2; j++) {
                    // avoid round off error when possible
                    if (plane.oGet(j) == 1.0f) {
                        mid.oSet(j, plane.z);
                    } else if (plane.oGet(j) == -1.0f) {
                        mid.oSet(j, -plane.z);
                    } else {
                        mid.oSet(j, p1.oGet(j) + dot * (p2.oGet(j) - p1.oGet(j)));
                    }
                }

                newPoints[newNumPoints] = mid;
                newNumPoints++;
            }

            if (newNumPoints >= MAX_POINTS_ON_WINDING_2D) {
                return true;
            }

            numPoints = newNumPoints;
//	memcpy( p, newPoints, newNumPoints * sizeof(idVec2) );
            System.arraycopy(newPoints, 0, p, 0, newNumPoints);

            return true;
        }

        public idWinding2D Copy() {
            idWinding2D w;

            w = new idWinding2D();
            w.numPoints = numPoints;
//	memcpy( w->p, p, numPoints * sizeof( p[0] ) );
            System.arraycopy(p, 0, w.p, 0, numPoints);
            return w;
        }

        public idWinding2D Reverse() {
            idWinding2D w;
            int i;

            w = new idWinding2D();
            w.numPoints = numPoints;
            for (i = 0; i < numPoints; i++) {
                w.p[ numPoints - i - 1] = p[i];
            }
            return w;
        }

        public float GetArea() {
            int i;
            idVec2 d1, d2;
            float total;

            total = 0.0f;
            for (i = 2; i < numPoints; i++) {
                d1 = p[i - 1].oMinus(p[0]);
                d2 = p[i].oMinus(p[0]);
                total += d1.x * d2.y - d1.y * d2.x;
            }
            return total * 0.5f;
        }

        public idVec2 GetCenter() {
            int i;
            idVec2 center = new idVec2();

            center.Zero();
            for (i = 0; i < numPoints; i++) {
                center.oPluSet(p[i]);
            }
            center.oMulSet((1.0f / numPoints));
            return center;
        }

        public float GetRadius(final idVec2 center) {
            int i;
            float radius, r;
            idVec2 dir;

            radius = 0.0f;
            for (i = 0; i < numPoints; i++) {
                dir = p[i].oMinus(center);
                r = dir.oMultiply(dir);
                if (r > radius) {
                    radius = r;
                }
            }
            return idMath.Sqrt(radius);
        }

        public void GetBounds(idVec2 bounds[]) {
            int i;

            if (0 == numPoints) {
                bounds[0].x = bounds[0].y = idMath.INFINITY;
                bounds[1].x = bounds[1].y = -idMath.INFINITY;
                return;
            }
            bounds[0] = bounds[1] = p[0];
            for (i = 1; i < numPoints; i++) {
                if (p[i].x < bounds[0].x) {
                    bounds[0].x = p[i].x;
                } else if (p[i].x > bounds[1].x) {
                    bounds[1].x = p[i].x;
                }
                if (p[i].y < bounds[0].y) {
                    bounds[0].y = p[i].y;
                } else if (p[i].y > bounds[1].y) {
                    bounds[1].y = p[i].y;
                }
            }
        }

        static final float EDGE_LENGTH = 0.2f;

        public boolean IsTiny() {
            int i;
            float len;
            idVec2 delta;
            int edges;

            edges = 0;
            for (i = 0; i < numPoints; i++) {
                delta = p[(i + 1) % numPoints].oMinus(p[i]);
                len = delta.Length();
                if (len > EDGE_LENGTH) {
                    if (++edges == 3) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean IsHuge() {// base winding for a plane is typically huge
            int i, j;

            for (i = 0; i < numPoints; i++) {
                for (j = 0; j < 2; j++) {
                    if (p[i].oGet(j) <= MIN_WORLD_COORD || p[i].oGet(j) >= MAX_WORLD_COORD) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void Print() {
            int i;

            for (i = 0; i < numPoints; i++) {
//		idLib::common->Printf( "(%5.1f, %5.1f)\n", p[i][0], p[i][1] );
            }
        }

        public float PlaneDistance(final idVec3 plane) {
            int i;
            float d, min, max;

            min = idMath.INFINITY;
            max = -min;
            for (i = 0; i < numPoints; i++) {
                d = plane.x * p[i].x + plane.y * p[i].y + plane.z;
                if (d < min) {
                    min = d;
                    if ((FLOATSIGNBITSET(min) & FLOATSIGNBITNOTSET(max)) != 0) {
                        return 0.0f;
                    }
                }
                if (d > max) {
                    max = d;
                    if ((FLOATSIGNBITSET(min) & FLOATSIGNBITNOTSET(max)) != 0) {
                        return 0.0f;
                    }
                }
            }
            if (FLOATSIGNBITNOTSET(min) != 0) {
                return min;
            }
            if (FLOATSIGNBITSET(max) != 0) {
                return max;
            }
            return 0.0f;
        }

        //public	int				PlaneSide( final idVec3 plane, final float epsilon = ON_EPSILON ) ;
        public int PlaneSide(final idVec3 plane, final float epsilon) {
            boolean front, back;
            int i;
            float d;

            front = false;
            back = false;
            for (i = 0; i < numPoints; i++) {
                d = plane.x * p[i].x + plane.y * p[i].y + plane.z;
                if (d < -epsilon) {
                    if (front) {
                        return SIDE_CROSS;
                    }
                    back = true;
//                    continue;
                } else if (d > epsilon) {
                    if (back) {
                        return SIDE_CROSS;
                    }
                    front = true;
//                    continue;
                }
            }

            if (back) {
                return SIDE_BACK;
            }
            if (front) {
                return SIDE_FRONT;
            }
            return SIDE_ON;
        }

        public boolean PointInside(final idVec2 point, final float epsilon) {
            int i;
            float d;
            idVec3 plane;

            for (i = 0; i < numPoints; i++) {
                plane = Plane2DFromPoints(p[i], p[(i + 1) % numPoints]);
                d = plane.x * point.x + plane.y * point.y + plane.z;
                if (d > epsilon) {
                    return false;
                }
            }
            return true;
        }

        public boolean LineIntersection(final idVec2 start, final idVec2 end) {
            int i, numEdges;
            int[] sides = new int[MAX_POINTS_ON_WINDING_2D + 1], counts = new int[3];
            float d1, d2, epsilon = 0.1f;
            idVec3 plane;
            idVec3[] edges = new idVec3[2];

            counts[SIDE_FRONT] = counts[SIDE_BACK] = counts[SIDE_ON] = 0;

            plane = Plane2DFromPoints(start, end);
            for (i = 0; i < numPoints; i++) {
                d1 = plane.x * p[i].x + plane.y * p[i].y + plane.z;
                if (d1 > epsilon) {
                    sides[i] = SIDE_FRONT;
                } else if (d1 < -epsilon) {
                    sides[i] = SIDE_BACK;
                } else {
                    sides[i] = SIDE_ON;
                }
                counts[sides[i]]++;
            }
            sides[i] = sides[0];

            if (0 == counts[SIDE_FRONT]) {
                return false;
            }
            if (0 == counts[SIDE_BACK]) {
                return false;
            }

            numEdges = 0;
            for (i = 0; i < numPoints; i++) {
                if (sides[i] != sides[i + 1] && sides[i + 1] != SIDE_ON) {
                    edges[numEdges++] = Plane2DFromPoints(p[i], p[(i + 1) % numPoints]);
                    if (numEdges >= 2) {
                        break;
                    }
                }
            }
            if (numEdges < 2) {
                return false;
            }

            d1 = edges[0].x * start.x + edges[0].y * start.y + edges[0].z;
            d2 = edges[0].x * end.x + edges[0].y * end.y + edges[0].z;
            if ((FLOATSIGNBITNOTSET(d1) & FLOATSIGNBITNOTSET(d2)) != 0) {
                return false;
            }
            d1 = edges[1].x * start.x + edges[1].y * start.y + edges[1].z;
            d2 = edges[1].x * end.x + edges[1].y * end.y + edges[1].z;
            if ((FLOATSIGNBITNOTSET(d1) & FLOATSIGNBITNOTSET(d2)) != 0) {
                return false;
            }
            return true;
        }

        //public	boolean			RayIntersection( final idVec2 start, final idVec2 dir, float scale1, float scale2) ;
        public boolean RayIntersection(final idVec2 start, final idVec2 dir, float[] scale1, float[] scale2, int[] edgeNums) {
            int i, numEdges;
            int[] localEdgeNums = new int[2];
            int[] sides = new int[MAX_POINTS_ON_WINDING_2D + 1], counts = new int[3];
            float d1, d2, epsilon = 0.1f;
            idVec3 plane;
            idVec3[] edges = new idVec3[2];

            scale1[0] = scale2[0] = 0.0f;
            counts[SIDE_FRONT] = counts[SIDE_BACK] = counts[SIDE_ON] = 0;

            plane = Plane2DFromVecs(start, dir);
            for (i = 0; i < numPoints; i++) {
                d1 = plane.x * p[i].x + plane.y * p[i].y + plane.z;
                if (d1 > epsilon) {
                    sides[i] = SIDE_FRONT;
                } else if (d1 < -epsilon) {
                    sides[i] = SIDE_BACK;
                } else {
                    sides[i] = SIDE_ON;
                }
                counts[sides[i]]++;
            }
            sides[i] = sides[0];

            if (0 == counts[SIDE_FRONT]) {
                return false;
            }
            if (0 == counts[SIDE_BACK]) {
                return false;
            }

            numEdges = 0;
            for (i = 0; i < numPoints; i++) {
                if (sides[i] != sides[i + 1] && sides[i + 1] != SIDE_ON) {
                    localEdgeNums[numEdges] = i;
                    edges[numEdges++] = Plane2DFromPoints(p[i], p[(i + 1) % numPoints]);
                    if (numEdges >= 2) {
                        break;
                    }
                }
            }
            if (numEdges < 2) {
                return false;
            }

            d1 = edges[0].x * start.x + edges[0].y * start.y + edges[0].z;
            d2 = -(edges[0].x * dir.x + edges[0].y * dir.y);
            if (d2 == 0.0f) {
                return false;
            }
            scale1[0] = d1 / d2;
            d1 = edges[1].x * start.x + edges[1].y * start.y + edges[1].z;
            d2 = -(edges[1].x * dir.x + edges[1].y * dir.y);
            if (d2 == 0.0f) {
                return false;
            }
            scale2[0] = d1 / d2;

            if (idMath.Fabs(scale1[0]) > idMath.Fabs(scale2[0])) {
                float scale3 = scale1[0];
                scale1[0] = scale2[0];
                scale2[0] = scale3;
                idSwap(localEdgeNums, localEdgeNums, 0, 1);
            }

            if (edgeNums != null) {
                edgeNums[0] = localEdgeNums[0];
                edgeNums[1] = localEdgeNums[1];
            }
            return true;
        }

        public static idVec3 Plane2DFromPoints(final idVec2 start, final idVec2 end) {
            return Plane2DFromPoints(start, end, false);
        }

        public static idVec3 Plane2DFromPoints(final idVec2 start, final idVec2 end, final boolean normalize) {
            idVec3 plane = new idVec3();
            plane.x = start.y - end.y;
            plane.y = end.x - start.x;
            if (normalize) {
                plane.ToVec2_Normalize();
            }
            plane.z = -(start.x * plane.x + start.y * plane.y);
            return plane;
        }

        public idVec3 Plane2DFromVecs(final idVec2 start, final idVec2 dir) {
            return Plane2DFromVecs(start, dir, false);
        }

        public idVec3 Plane2DFromVecs(final idVec2 start, final idVec2 dir, final boolean normalize) {
            idVec3 plane = new idVec3();
            plane.x = -dir.y;
            plane.y = dir.x;
            if (normalize) {
                plane.ToVec2_Normalize();
            }
            plane.z = -(start.x * plane.x + start.y * plane.y);
            return plane;
        }

        public boolean Plane2DIntersection(final idVec3 plane1, final idVec3 plane2, idVec2 point) {
            float n00, n01, n11, det, invDet, f0, f1;

            n00 = plane1.x * plane1.x + plane1.y * plane1.y;
            n01 = plane1.x * plane2.x + plane1.y * plane2.y;
            n11 = plane2.x * plane2.x + plane2.y * plane2.y;
            det = n00 * n11 - n01 * n01;

            if (idMath.Fabs(det) < 1e-6f) {
                return false;
            }

            invDet = 1.0f / det;
            f0 = (n01 * plane2.z - n11 * plane1.z) * invDet;
            f1 = (n01 * plane1.z - n00 * plane2.z) * invDet;
            point.x = f0 * plane1.x + f1 * plane2.x;
            point.y = f0 * plane1.y + f1 * plane2.y;
            return true;
        }
    };
}
