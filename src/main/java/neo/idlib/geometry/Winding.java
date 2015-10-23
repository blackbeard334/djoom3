package neo.idlib.geometry;

import neo.Renderer.tr_main;
import neo.TempDump;
import neo.TempDump.NiLLABLE;
import neo.idlib.BV.Bounds.idBounds;
import static neo.idlib.Lib.MAX_WORLD_COORD;
import static neo.idlib.Lib.MAX_WORLD_SIZE;
import static neo.idlib.Lib.MIN_WORLD_COORD;
import neo.idlib.math.Math_h;
import static neo.idlib.math.Math_h.FLOATSIGNBITNOTSET;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.SIDE_BACK;
import static neo.idlib.math.Plane.SIDE_CROSS;
import static neo.idlib.math.Plane.SIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_ON;
import static neo.idlib.Lib.idLib;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Pluecker.idPluecker;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec5;
import neo.sys.win_main;

/**
 *
 */
public class Winding {

    /*
     ===============================================================================

     A winding is an arbitrary convex polygon defined by an array of points.

     ===============================================================================
     */
    public static class idWinding implements NiLLABLE<idWinding> {

        protected int      numPoints;    // number of points
        protected idVec5[] p;            // pointer to point data
        protected int      allocedSize;
        private boolean    NULL = true;  // used to identify whether any value was assigned. used in combination with idWinding.Split(....);
        //
        //

        public idWinding() {
            numPoints = allocedSize = 0;
            p = null;
        }

        // allocate for n points
        public idWinding(final int n) {
            numPoints = allocedSize = 0;
            p = null;
            EnsureAlloced(n);
        }

        // winding from points
        public idWinding(final idVec3[] verts, final int n) {
            int i;

            numPoints = allocedSize = 0;
            p = null;
            if (!EnsureAlloced(n)) {
                numPoints = 0;
                return;
            }
            for (i = 0; i < n; i++) {
                p[i].oSet(verts[i]);
                p[i].s = p[i].t = 0.0f;
            }
            numPoints = n;

        }

        // base winding for plane
        public idWinding(final idVec3 normal, final float dist) {
            numPoints = allocedSize = 0;
            p = null;
            BaseForPlane(normal, dist);
        }

        // base winding for plane
        public idWinding(final idPlane plane) {
            numPoints = allocedSize = 0;
            p = null;
            BaseForPlane(plane);
        }

        public idWinding(final idWinding winding) {
            int i;
            if (!EnsureAlloced(winding.GetNumPoints())) {
                numPoints = 0;
                return;
            }
            for (i = 0; i < winding.GetNumPoints(); i++) {
                p[i] = new idVec5(winding.oGet(i));
            }
            numPoints = winding.GetNumPoints();
        }

//public				~idWinding();
//
        @Override
        public idWinding oSet(final idWinding winding) {
            int i;
            this.NULL = false;

            if (!EnsureAlloced(winding.numPoints)) {
                numPoints = 0;
                return this;
            }
            for (i = 0; i < winding.numPoints; i++) {
                p[i] = new idVec5(winding.p[i]);
            }
            numPoints = winding.numPoints;
            return this;
        }

//public	final idVec5 	operator[]( final int index ) ;
        public idVec5 oGet(final int index) {
            return p[ index];
        }

        public float oGet(final int index, final int index2) {
            return p[index].oGet(index2);
        }

        public idVec5 oSet(final int index, final idVec5 value) {
            return p[index] = value;
        }

        public idVec5 oSet(final int index, final idVec3 value) {
            if (null == p[index]) {
                p[index] = new idVec5();//lazy init.
            }
            return p[index].oSet(value);
        }

        // add a point to the end of the winding point array
        public idWinding oPluSet(final idVec3 v) {
            AddPoint(v);
            return this;
        }

        public idWinding oPluSet(final idVec5 v) {
            AddPoint(v);
            return this;
        }

        public void AddPoint(final idVec3 v) {
            if (!EnsureAlloced(numPoints + 1, true)) {
                return;
            }
            p[numPoints] = new idVec5(v);
            numPoints++;
        }

        public void AddPoint(final idVec5 v) {
            if (!EnsureAlloced(numPoints + 1, true)) {
                return;
            }
            p[numPoints] = v;
            numPoints++;
        }

        // number of points on winding
        public int GetNumPoints() {
            return numPoints;
        }

        public void SetNumPoints(int n) {
            if (!EnsureAlloced(n, true)) {
                return;
            }
            numPoints = n;
        }

        public void Clear() {
            numPoints = 0;
//	delete[] p;
            p = null;
        }

        // huge winding for plane, the points go counter clockwise when facing the front of the plane
        public void BaseForPlane(final idVec3 normal, final float dist) {
            idVec3 org, vRight = new idVec3(), vUp = new idVec3();

            org = normal.oMultiply(dist);

            normal.NormalVectors(vUp, vRight);
            vUp.oMulSet(MAX_WORLD_SIZE);
            vRight.oMulSet(MAX_WORLD_SIZE);

            EnsureAlloced(4);
            numPoints = 4;
            p[0] = new idVec5(org.oMinus(vRight).oPlus(vUp));
            p[0].s = p[0].t = 0.0f;
            p[1] = new idVec5(org.oPlus(vRight).oPlus(vUp));
            p[1].s = p[1].t = 0.0f;
            p[2] = new idVec5(org.oPlus(vRight).oMinus(vUp));
            p[2].s = p[2].t = 0.0f;
            p[3] = new idVec5(org.oMinus(vRight).oMinus(vUp));
            p[3].s = p[3].t = 0.0f;
        }

        public void BaseForPlane(final idPlane plane) {
            BaseForPlane(plane.Normal(), plane.Dist());
        }

        // splits the winding into a front and back winding, the winding itself stays unchanged
        // returns a SIDE_?
        public int Split(final idPlane plane, final float epsilon, idWinding front, idWinding back) {
            float[] dists;
            byte[] sides;
            int[] counts = new int[3];
            float dot;
            int i, j;
            idVec5 p1, p2;
            idVec5 mid = new idVec5();
            idWinding f, b;
            int maxpts;

//	assert( this );
            dists = new float[numPoints + 4];
            sides = new byte[numPoints + 4];

            counts[0] = counts[1] = counts[2] = 0;

            // determine sides for each point
            for (i = 0; i < numPoints; i++) {
                dists[i] = dot = plane.Distance(p[i].ToVec3());
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

//            front[0] = back[0] = null;//TODO:check the double pointers!!!
            //
            // if coplanar, put on the front side if the normals match
            if (0 == counts[SIDE_FRONT] && 0 == counts[SIDE_BACK]) {
                idPlane windingPlane = new idPlane();

                GetPlane(windingPlane);
                if (windingPlane.Normal().oMultiply(plane.Normal()) > 0.0f) {
                    front.oSet(Copy());
                    return SIDE_FRONT;
                } else {
                    back.oSet(Copy());
                    return SIDE_BACK;
                }
            }
            // if nothing at the front of the clipping plane
            if (0 == counts[SIDE_FRONT]) {
                back.oSet(Copy());
                return SIDE_BACK;
            }
            // if nothing at the back of the clipping plane
            if (0 == counts[SIDE_BACK]) {
                front.oSet(Copy());
                return SIDE_FRONT;
            }

            maxpts = numPoints + 4;	// cant use counts[0]+2 because of fp grouping errors

            front.oSet(f = new idWinding(maxpts));
            back.oSet(b = new idWinding(maxpts));

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
                    for (j = 0; j < 3; j++) {
                        // avoid round off error when possible
                        if (plane.Normal().oGet(j) == 1.0f) {
                            mid.oSet(j, plane.Dist());
                        } else if (plane.Normal().oGet(j) == -1.0f) {
                            mid.oSet(j, -plane.Dist());
                        } else {
                            mid.oSet(j, p1.oGet(j) + dot * (p2.oGet(j) - p1.oGet(j)));
                        }
                    }
                    mid.s = p1.s + dot * (p2.s - p1.s);
                    mid.t = p1.t + dot * (p2.t - p1.t);
                } else {
                    dot = dists[i + 1] / (dists[i + 1] - dists[i]);
                    for (j = 0; j < 3; j++) {
                        // avoid round off error when possible
                        if (plane.Normal().oGet(j) == 1.0f) {
                            mid.oSet(j, plane.Dist());
                        } else if (plane.Normal().oGet(j) == -1.0f) {
                            mid.oSet(j, -plane.Dist());
                        } else {
                            mid.oSet(j, p2.oGet(j) + dot * (p1.oGet(j) - p2.oGet(j)));
                        }
                    }
                    mid.s = p2.s + dot * (p1.s - p2.s);
                    mid.t = p2.t + dot * (p1.t - p2.t);
                }

                f.p[f.numPoints] = mid;
                f.numPoints++;
                b.p[b.numPoints] = mid;
                b.numPoints++;
            }

            if (f.numPoints > maxpts || b.numPoints > maxpts) {
		idLib.common.FatalError( "idWinding::Split: points exceeded estimate." );
            }

            return SIDE_CROSS;
        }

        public idWinding Clip(final idPlane plane) {
            return Clip(plane, ON_EPSILON, false);
        }

        public idWinding Clip(final idPlane plane, final float epsilon) {
            return Clip(plane, epsilon, false);
        }

        // returns the winding fragment at the front of the clipping plane,
        // if there is nothing at the front the winding itself is destroyed and NULL is returned
        public idWinding Clip(final idPlane plane, final float epsilon, final boolean keepOn) {
            float[] dists;
            byte[] sides;
            idVec5[] newPoints;
            int newNumPoints;
            int[] counts = new int[3];
            float dot;
            int i, j;
            idVec5 p1, p2;
            idVec5 mid = new idVec5();
            int maxpts;

//	assert( this );
            dists = new float[numPoints + 4];
            sides = new byte[numPoints + 4];

            counts[SIDE_FRONT] = counts[SIDE_BACK] = counts[SIDE_ON] = 0;

            // determine sides for each point
            for (i = 0; i < numPoints; i++) {
                dists[i] = dot = plane.Distance(p[i].ToVec3());
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
                return this;
            }
            // if nothing at the front of the clipping plane
            if (0 == counts[SIDE_FRONT]) {
//		delete this;
                return null;
            }
            // if nothing at the back of the clipping plane
            if (0 == counts[SIDE_BACK]) {
                return this;
            }

            maxpts = numPoints + 4;		// cant use counts[0]+2 because of fp grouping errors

            newPoints = new idVec5[maxpts];
            newNumPoints = 0;

            for (i = 0; i < numPoints; i++) {
                p1 = p[i];

                if (newNumPoints + 1 > maxpts) {
                    return this;		// can't split -- fall back to original
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
                    return this;		// can't split -- fall back to original
                }

                // generate a split point
                p2 = p[(i + 1) % numPoints];

                dot = dists[i] / (dists[i] - dists[i + 1]);
                for (j = 0; j < 3; j++) {
                    // avoid round off error when possible
                    if (plane.Normal().oGet(j) == 1.0f) {
                        mid.oSet(j, plane.Dist());
                    } else if (plane.Normal().oGet(j) == -1.0f) {
                        mid.oSet(j, -plane.Dist());
                    } else {
                        mid.oSet(j, p1.oGet(j) + dot * (p2.oGet(j) - p1.oGet(j)));
                    }
                }
                mid.s = p1.s + dot * (p2.s - p1.s);
                mid.t = p1.t + dot * (p2.t - p1.t);

                newPoints[newNumPoints] = mid;
                newNumPoints++;
            }

            if (!EnsureAlloced(newNumPoints, false)) {
                return this;
            }

            numPoints = newNumPoints;
            System.arraycopy(newPoints, 0, p, 0, newNumPoints);//memcpy( p, newPoints, newNumPoints * sizeof(idVec5) );

            return this;
        }

        public boolean ClipInPlace(final idPlane plane) {
            return ClipInPlace(plane, ON_EPSILON);
        }

        public boolean ClipInPlace(final idPlane plane, final float epsilon) {
            return ClipInPlace(plane, epsilon, false);
        }

        // cuts off the part at the back side of the plane, returns true if some part was at the front
        // if there is nothing at the front the number of points is set to zero
        public boolean ClipInPlace(final idPlane plane, final float epsilon, final boolean keepOn) {
            float[] dists;
            byte[] sides;
            idVec5[] newPoints;
            int newNumPoints;
            int[] counts = new int[3];
            float dot;
            int i, j;
            idVec5 p1, p2;
            idVec5 mid = new idVec5();
            int maxpts;

//	assert( this );
            dists = new float[numPoints + 4];
            sides = new byte[numPoints + 4];

            counts[SIDE_FRONT] = counts[SIDE_BACK] = counts[SIDE_ON] = 0;

            // determine sides for each point
            for (i = 0; i < numPoints; i++) {
                dists[i] = dot = plane.Distance(p[i].ToVec3());
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
            // if nothing at the front of the clipping plane
            if (0 == counts[SIDE_FRONT]) {
                numPoints = 0;
                return false;
            }
            // if nothing at the back of the clipping plane
            if (0 == counts[SIDE_BACK]) {
                return true;
            }

            maxpts = numPoints + 4;		// cant use counts[0]+2 because of fp grouping errors

            newPoints = tr_main.R_ClearedStaticAlloc(maxpts, idVec5.class);
            newNumPoints = 0;

            for (i = 0; i < numPoints; i++) {
                p1 = p[i];

                if (newNumPoints + 1 > maxpts) {
                    return true;		// can't split -- fall back to original
                }

                if (sides[i] == SIDE_ON) {
                    newPoints[newNumPoints].oSet(p1);
                    newNumPoints++;
                    continue;
                }

                if (sides[i] == SIDE_FRONT) {
                    newPoints[newNumPoints].oSet(p1);
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
                for (j = 0; j < 3; j++) {
                    // avoid round off error when possible
                    if (plane.Normal().oGet(j) == 1.0f) {
                        mid.oSet(j, plane.Dist());
                    } else if (plane.Normal().oGet(j) == -1.0f) {
                        mid.oSet(j, -plane.Dist());
                    } else {
                        mid.oSet(j, p1.oGet(j) + dot * (p2.oGet(j) - p1.oGet(j)));
                    }
                }
                mid.s = p1.s + dot * (p2.s - p1.s);
                mid.t = p1.t + dot * (p2.t - p1.t);

                newPoints[newNumPoints].oSet(mid);
                newNumPoints++;
            }

            if (!EnsureAlloced(newNumPoints, false)) {
                return true;
            }

            numPoints = newNumPoints;
            System.arraycopy(newPoints, 0, p, 0, newNumPoints);//memcpy( p, newPoints, newNumPoints * sizeof(idVec5) );

            return true;
        }

//
        // returns a copy of the winding
        public idWinding Copy() {
            idWinding w;

            w = new idWinding(numPoints);
            w.numPoints = numPoints;
            System.arraycopy(p, 0, w.p, 0, numPoints);//memcpy( w->p, p, numPoints * sizeof(p[0]) );
            return w;
        }

        public idWinding Reverse() {
            idWinding w;
            int i;

            w = new idWinding(numPoints);
            w.numPoints = numPoints;
            for (i = 0; i < numPoints; i++) {
                w.p[ numPoints - i - 1] = p[i];
            }
            return w;
        }

        public void ReverseSelf() {
            idVec5 v;
            int i;

            for (i = 0; i < (numPoints >> 1); i++) {
                v = p[i];
                p[i] = p[numPoints - i - 1];
                p[numPoints - i - 1] = v;
            }
        }

        public void RemoveEqualPoints() {
            RemoveEqualPoints(ON_EPSILON);
        }

        public void RemoveEqualPoints(final float epsilon) {
            int i, j;

            for (i = 0; i < numPoints; i++) {
                if ((p[i].ToVec3().oMinus(p[(i + numPoints - 1) % numPoints].ToVec3())).LengthSqr() >= Math_h.Square(epsilon)) {
                    continue;
                }
                numPoints--;
                for (j = i; j < numPoints; j++) {
                    p[j] = p[j + 1];
                }
                i--;
            }
        }

        public void RemoveColinearPoints(final idVec3 normal) {
            RemoveColinearPoints(normal, ON_EPSILON);
        }

        public void RemoveColinearPoints(final idVec3 normal, final float epsilon) {
            int i, j;
            idVec3 edgeNormal;
            float dist;

            if (numPoints <= 3) {
                return;
            }

            for (i = 0; i < numPoints; i++) {

                // create plane through edge orthogonal to winding plane
                edgeNormal = (p[i].ToVec3().oMinus(p[(i + numPoints - 1) % numPoints].ToVec3()).Cross(normal));
                edgeNormal.Normalize();
                dist = edgeNormal.oMultiply(p[i].ToVec3());

                if (idMath.Fabs(edgeNormal.oMultiply(p[(i + 1) % numPoints].ToVec3()) - dist) > epsilon) {
                    continue;
                }

                numPoints--;
                for (j = i; j < numPoints; j++) {
                    p[j] = p[j + 1];
                }
                i--;
            }
        }

        public void RemovePoint(int point) {
            if (point < 0 || point >= numPoints) {
                idLib.common.FatalError("idWinding::removePoint: point out of range");
            }
            if (point < numPoints - 1) {
//		memmove(&p[point], &p[point+1], (numPoints - point - 1) * sizeof(p[0]) );
                p[point] = new idVec5().oSet(p[point + 1]);
            }
            numPoints--;
        }

        public void InsertPoint(final idVec3 point, int spot) {
            int i;

            if (spot > numPoints) {
                idLib.common.FatalError("idWinding::insertPoint: spot > numPoints");
            }

            if (spot < 0) {
                idLib.common.FatalError("idWinding::insertPoint: spot < 0");
            }

            EnsureAlloced(numPoints + 1, true);
            for (i = numPoints; i > spot; i--) {
                p[i] = p[i - 1];
            }
            p[spot].oSet(point);
            numPoints++;
        }

        public boolean InsertPointIfOnEdge(final idVec3 point, final idPlane plane) {
            return InsertPointIfOnEdge(point, plane, ON_EPSILON);
        }

        public boolean InsertPointIfOnEdge(final idVec3 point, final idPlane plane, final float epsilon) {
            int i;
            float dist, dot;
            idVec3 normal;

            // point may not be too far from the winding plane
            if (idMath.Fabs(plane.Distance(point)) > epsilon) {
                return false;
            }

            for (i = 0; i < numPoints; i++) {

                // create plane through edge orthogonal to winding plane
                normal = (p[(i + 1) % numPoints].ToVec3().oMinus(p[i].ToVec3())).Cross(plane.Normal());
                normal.Normalize();
                dist = normal.oMultiply(p[i].ToVec3());

                if (idMath.Fabs(normal.oMultiply(point) - dist) > epsilon) {
                    continue;
                }

                normal = plane.Normal().Cross(normal);
                dot = normal.oMultiply(point);

                dist = dot - normal.oMultiply(p[i].ToVec3());

                if (dist < epsilon) {
                    // if the winding already has the point
                    if (dist > -epsilon) {
                        return false;
                    }
                    continue;
                }

                dist = dot - normal.oMultiply(p[(i + 1) % numPoints].ToVec3());

                if (dist > -epsilon) {
                    // if the winding already has the point
                    if (dist < epsilon) {
                        return false;
                    }
                    continue;
                }

                InsertPoint(point, i + 1);
                return true;
            }
            return false;
        }

        // add a winding to the convex hull
        public void AddToConvexHull(final idWinding winding, final idVec3 normal) {
            AddToConvexHull(winding, normal, ON_EPSILON);
        }

        /*
         =============
         idWinding::AddToConvexHull

         Adds the given winding to the convex hull.
         Assumes the current winding already is a convex hull with three or more points.
         =============
         */
        public void AddToConvexHull(final idWinding winding, final idVec3 normal, final float epsilon) {// add a winding to the convex hull
            int i, j, k;
            idVec3 dir;
            float d;
            int maxPts;
            idVec3[] hullDirs;
            boolean[] hullSide;
            boolean outside;
            int numNewHullPoints;
            idVec5[] newHullPoints;

            if (null == winding) {
                return;
            }

            maxPts = this.numPoints + winding.numPoints;

            if (!this.EnsureAlloced(maxPts, true)) {
                return;
            }

            newHullPoints = new idVec5[maxPts];
            hullDirs = new idVec3[maxPts];
            hullSide = new boolean[maxPts];

            for (i = 0; i < winding.numPoints; i++) {
                final idVec5 p1 = winding.p[i];

                // calculate hull edge vectors
                for (j = 0; j < this.numPoints; j++) {
                    dir = this.p[ (j + 1) % this.numPoints].ToVec3().oMinus(this.p[ j].ToVec3());
                    dir.Normalize();
                    hullDirs[j] = normal.Cross(dir);
                }

                // calculate side for each hull edge
                outside = false;
                for (j = 0; j < this.numPoints; j++) {
                    dir = p1.ToVec3().oMinus(this.p[j].ToVec3());
                    d = dir.oMultiply(hullDirs[j]);
                    if (d >= epsilon) {
                        outside = true;
                    }
                    if (d >= -epsilon) {
                        hullSide[j] = true;
                    } else {
                        hullSide[j] = false;
                    }
                }

                // if the point is effectively inside, do nothing
                if (!outside) {
                    continue;
                }

                // find the back side to front side transition
                for (j = 0; j < this.numPoints; j++) {
                    if (!hullSide[ j] && hullSide[ (j + 1) % this.numPoints]) {
                        break;
                    }
                }
                if (j >= this.numPoints) {
                    continue;
                }

                // insert the point here
                newHullPoints[0] = p1;
                numNewHullPoints = 1;

                // copy over all points that aren't double fronts
                j = (j + 1) % this.numPoints;
                for (k = 0; k < this.numPoints; k++) {
                    if (hullSide[ (j + k) % this.numPoints] && hullSide[ (j + k + 1) % this.numPoints]) {
                        continue;
                    }
                    newHullPoints[numNewHullPoints] = this.p[ (j + k + 1) % this.numPoints];
                    numNewHullPoints++;
                }

                this.numPoints = numNewHullPoints;
                System.arraycopy(newHullPoints, 0, this.p, 0, numNewHullPoints);//memcpy( this.p, newHullPoints, numNewHullPoints * sizeof(idVec5) );
            }
        }

        // add a point to the convex hull
        public void AddToConvexHull(final idVec3 point, final idVec3 normal) {
            AddToConvexHull(point, normal, ON_EPSILON);
        }

        /*
         =============
         idWinding::AddToConvexHull

         Add a point to the convex hull.
         The current winding must be convex but may be degenerate and can have less than three points.
         =============
         */
        public void AddToConvexHull(final idVec3 point, final idVec3 normal, final float epsilon) {// add a point to the convex hull
            int j, k, numHullPoints;
            idVec3 dir;
            float d;
            idVec3[] hullDirs;
            boolean[] hullSide;
            idVec5[] hullPoints;
            boolean outside;

            switch (numPoints) {
                case 0: {
                    p[0] = new idVec5(point);
                    numPoints++;
                    return;
                }
                case 1: {
                    // don't add the same point second
                    if (p[0].ToVec3().Compare(point, epsilon)) {
                        return;
                    }
                    p[1] = new idVec5(point);
                    numPoints++;
                    return;
                }
                case 2: {
                    // don't add a point if it already exists
                    if (p[0].ToVec3().Compare(point, epsilon) || p[1].ToVec3().Compare(point, epsilon)) {
                        return;
                    }
                    // if only two points make sure we have the right ordering according to the normal
                    dir = point.oMinus(p[0].ToVec3());
                    dir = dir.Cross(p[1].ToVec3().oMinus(p[0].ToVec3()));
                    if (dir.oGet(0) == 0.0f && dir.oGet(1) == 0.0f && dir.oGet(2) == 0.0f) {
                        // points don't make a plane
                        return;
                    }
                    if (dir.oMultiply(normal) > 0.0f) {
                        p[2] = new idVec5(point);
                    } else {
                        p[2] = p[1];
                        p[1] = new idVec5(point);
                    }
                    numPoints++;
                    return;
                }
            }

            hullDirs = new idVec3[numPoints];
            hullSide = new boolean[numPoints];

            // calculate hull edge vectors
            for (j = 0; j < numPoints; j++) {
                dir = p[(j + 1) % numPoints].ToVec3().oMinus(p[j].ToVec3());
                hullDirs[j] = normal.Cross(dir);
            }

            // calculate side for each hull edge
            outside = false;
            for (j = 0; j < numPoints; j++) {
                dir = point.oMinus(p[j].ToVec3());
                d = dir.oMultiply(hullDirs[j]);
                if (d >= epsilon) {
                    outside = true;
                }
                if (d >= -epsilon) {
                    hullSide[j] = true;
                } else {
                    hullSide[j] = false;
                }
            }

            // if the point is effectively inside, do nothing
            if (!outside) {
                return;
            }

            // find the back side to front side transition
            for (j = 0; j < numPoints; j++) {
                if (!hullSide[ j] && hullSide[ (j + 1) % numPoints]) {
                    break;
                }
            }
            if (j >= numPoints) {
                return;
            }

            hullPoints = new idVec5[numPoints + 1];

            // insert the point here
            hullPoints[0] = new idVec5(point);
            numHullPoints = 1;

            // copy over all points that aren't double fronts
            j = (j + 1) % numPoints;
            for (k = 0; k < numPoints; k++) {
                if (hullSide[ (j + k) % numPoints] && hullSide[ (j + k + 1) % numPoints]) {
                    continue;
                }
                hullPoints[numHullPoints] = p[ (j + k + 1) % numPoints];
                numHullPoints++;
            }

            if (!EnsureAlloced(numHullPoints, false)) {
                return;
            }
            numPoints = numHullPoints;
//	memcpy( p, hullPoints, numHullPoints * sizeof(idVec5) );
            System.arraycopy(hullPoints, 0, this.p, 0, numHullPoints);
        }
        static final float CONTINUOUS_EPSILON = 0.005f;

        public idWinding TryMerge(final idWinding w, final idVec3 planenormal) {
            return TryMerge(w, planenormal, 0);
        }

        // tries to merge 'this' with the given winding, returns NULL if merge fails, both 'this' and 'w' stay intact
        // 'keep' tells if the contacting points should stay even if they create colinear edges
        public idWinding TryMerge(final idWinding w, final idVec3 planenormal, int keep) {
            idVec3 p1, p2, p3, p4, back;
            idWinding newf;
            final idWinding f1, f2;
            int i, j, k, l;
            idVec3 normal, delta;
            float dot;
            boolean keep1, keep2;

            f1 = this;
            f2 = new idWinding(w);
            //
            // find a idLib::common edge
            //	
            p1 = p2 = null;	// stop compiler warning
            j = 0;

            for (i = 0; i < f1.numPoints; i++) {
                p1 = f1.p[i].ToVec3();
                p2 = f1.p[(i + 1) % f1.numPoints].ToVec3();
                for (j = 0; j < f2.numPoints; j++) {
                    p3 = f2.p[j].ToVec3();
                    p4 = f2.p[(j + 1) % f2.numPoints].ToVec3();
                    for (k = 0; k < 3; k++) {
                        if (idMath.Fabs(p1.oGet(k) - p4.oGet(k)) > 0.1f) {
                            break;
                        }
                        if (idMath.Fabs(p2.oGet(k) - p3.oGet(k)) > 0.1f) {
                            break;
                        }
                    }
                    if (k == 3) {
                        break;
                    }
                }
                if (j < f2.numPoints) {
                    break;
                }
            }

            if (i == f1.numPoints) {
                return null;			// no matching edges
            }

            //
            // check slope of connected lines
            // if the slopes are colinear, the point can be removed
            //
            back = f1.p[(i + f1.numPoints - 1) % f1.numPoints].ToVec3();
            delta = p1.oMinus(back);
            normal = planenormal.Cross(delta);
            normal.Normalize();

            back = f2.p[(j + 2) % f2.numPoints].ToVec3();
            delta = back.oMinus(p1);
            dot = delta.oMultiply(normal);
            if (dot > CONTINUOUS_EPSILON) {
                return null;			// not a convex polygon
            }

            keep1 = (dot < -CONTINUOUS_EPSILON);

            back = f1.p[(i + 2) % f1.numPoints].ToVec3();
            delta = back.oMinus(p2);
            normal = planenormal.Cross(delta);
            normal.Normalize();

            back = f2.p[(j + f2.numPoints - 1) % f2.numPoints].ToVec3();
            delta = back.oMinus(p2);
            dot = delta.oMultiply(normal);
            if (dot > CONTINUOUS_EPSILON) {
                return null;			// not a convex polygon
            }

            keep2 = (dot < -CONTINUOUS_EPSILON);

            //
            // build the new polygon
            //
            newf = new idWinding(f1.numPoints + f2.numPoints);

            // copy first polygon
            for (k = (i + 1) % f1.numPoints; k != i; k = (k + 1) % f1.numPoints) {
                if (0 == keep && k == (i + 1) % f1.numPoints && !keep2) {
                    continue;
                }

                newf.p[newf.numPoints] = f1.p[k];
                newf.numPoints++;
            }

            // copy second polygon
            for (l = (j + 1) % f2.numPoints; l != j; l = (l + 1) % f2.numPoints) {
                if (0 == keep && l == (j + 1) % f2.numPoints && !keep1) {
                    continue;
                }
                newf.p[newf.numPoints] = f2.p[l];
                newf.numPoints++;
            }

            return newf;
        }

        public boolean Check() {
            return Check(true);
        }

        // check whether the winding is valid or not
        public boolean Check(boolean print) {
            int i, j;
            float d, edgedist;
            idVec3 dir, edgenormal;
            float area;
            idPlane plane = new idPlane();

            if (numPoints < 3) {
                if (print) {
                    idLib.common.Printf("idWinding::Check: only %i points.", numPoints);
                }
                return false;
            }

            area = GetArea();
            if (area < 1.0f) {
                if (print) {
                    idLib.common.Printf("idWinding::Check: tiny area: %f", area);
                }
                return false;
            }

            GetPlane(plane);

            for (i = 0; i < numPoints; i++) {
                final idVec3 p1 = p[i].ToVec3();

                // check if the winding is huge
                for (j = 0; j < 3; j++) {
                    if (p1.oGet(j) >= MAX_WORLD_COORD || p1.oGet(j) <= MIN_WORLD_COORD) {
                        if (print) {
                            idLib.common.Printf("idWinding::Check: point %d outside world %c-axis: %f", i, 'X' + j, p1.oGet(j));
                        }
                        return false;
                    }
                }

                j = i + 1 == numPoints ? 0 : i + 1;

                // check if the point is on the face plane
                d = p1.oMultiply(plane.Normal()) + plane.oGet(3);
                if (d < -ON_EPSILON || d > ON_EPSILON) {
                    if (print) {
                        idLib.common.Printf("idWinding::Check: point %d off plane.", i);
                    }
                    return false;
                }

                // check if the edge isn't degenerate
                final idVec3 p2 = p[j].ToVec3();
                dir = p2.oMinus(p1);

                if (dir.Length() < ON_EPSILON) {
                    if (print) {
				        idLib.common.Printf("idWinding::Check: edge %d is degenerate.", i);
                    }
                    return false;
                }

                // check if the winding is convex
                edgenormal = plane.Normal().Cross(dir);
                edgenormal.Normalize();
                edgedist = p1.oMultiply(edgenormal);
                edgedist += ON_EPSILON;

                // all other points must be on front side
                for (j = 0; j < numPoints; j++) {
                    if (j == i) {
                        continue;
                    }
                    d = p[j].ToVec3().oMultiply(edgenormal);
                    if (d > edgedist) {
                        if (print) {
                            idLib.common.Printf("idWinding::Check: non-convex.");
                        }
                        return false;
                    }
                }
            }
            return true;
        }

        public float GetArea() {
            int i;
            idVec3 d1, d2, cross;
            float total;

            total = 0.0f;
            for (i = 2; i < numPoints; i++) {
                d1 = p[i - 1].ToVec3().oMinus(p[0].ToVec3());
                d2 = p[i].ToVec3().oMinus(p[0].ToVec3());
                cross = d1.Cross(d2);
                total += cross.Length();
            }
            return total * 0.5f;
        }

        public idVec3 GetCenter() {
            int i;
            idVec3 center = new idVec3();

            center.Zero();
            for (i = 0; i < numPoints; i++) {
                center.oPluSet(p[i].ToVec3());
            }
            center.oMulSet(1.0f / numPoints);
            return center;
        }

        public float GetRadius(final idVec3 center) {
            int i;
            float radius, r;
            idVec3 dir;

            radius = 0.0f;
            for (i = 0; i < numPoints; i++) {
                dir = p[i].ToVec3().oMinus(center);
                r = dir.oMultiply(dir);
                if (r > radius) {
                    radius = r;
                }
            }
            return idMath.Sqrt(radius);
        }

        public void GetPlane(idVec3 normal, float[]dist) {
            idVec3 v1, v2, center;

            if (numPoints < 3) {
                normal.Zero();
                dist[0] = 0.0f;
                return;
            }

            center = GetCenter();
            v1 = p[0].ToVec3().oMinus(center);
            v2 = p[1].ToVec3().oMinus(center);
            normal = v2.Cross(v1);
            normal.Normalize();
            dist[0] = p[0].ToVec3().oMultiply(normal);
        }

        public void GetPlane(idPlane plane) {
            idVec3 v1, v2;
            idVec3 center;

            if (numPoints < 3) {
                plane.Zero();
                return;
            }

            center = GetCenter();
            v1 = p[0].ToVec3().oMinus(center);
            v2 = p[1].ToVec3().oMinus(center);
            plane.SetNormal(v2.Cross(v1));
            plane.Normalize();
            plane.FitThroughPoint(p[0].ToVec3());
        }

        public void GetBounds(idBounds bounds) {
            int i;

            if (0 == numPoints) {
                bounds.Clear();
                return;
            }

            bounds.oSet(0, bounds.oSet(1, p[0].ToVec3()));
            for (i = 1; i < numPoints; i++) {
                if (p[i].x < bounds.oGet(0).x) {
                    bounds.oGet(0).x = p[i].x;
                } else if (p[i].x > bounds.oGet(1).x) {
                    bounds.oGet(1).x = p[i].x;
                }
                if (p[i].y < bounds.oGet(0).y) {
                    bounds.oGet(0).y = p[i].y;
                } else if (p[i].y > bounds.oGet(1).y) {
                    bounds.oGet(1).y = p[i].y;
                }
                if (p[i].z < bounds.oGet(0).z) {
                    bounds.oGet(0).z = p[i].z;
                } else if (p[i].z > bounds.oGet(1).z) {
                    bounds.oGet(1).z = p[i].z;
                }
            }
        }
//
        private static final float EDGE_LENGTH = 0.2f;

        public boolean IsTiny() {
            int i;
            float len;
            idVec3 delta;
            int edges;

            edges = 0;
            for (i = 0; i < numPoints; i++) {
                delta = p[(i + 1) % numPoints].ToVec3().oMinus(p[i].ToVec3());
                len = delta.Length();
                if (len > EDGE_LENGTH) {
                    if (++edges == 3) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean IsHuge() {	// base winding for a plane is typically huge
            int i, j;

            for (i = 0; i < numPoints; i++) {
                for (j = 0; j < 3; j++) {
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
                idLib.common.Printf("(%5.1f, %5.1f, %5.1f)\n", p[i].oGet(0), p[i].oGet(1), p[i].oGet(2));
            }
        }

        public float PlaneDistance(final idPlane plane) {
            int i;
            float d, min, max;

            min = idMath.INFINITY;
            max = -min;
            for (i = 0; i < numPoints; i++) {
                d = plane.Distance(p[i].ToVec3());
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

        public int PlaneSide(final idPlane plane) {
            return PlaneSide(plane, ON_EPSILON);
        }

        public int PlaneSide(final idPlane plane, final float epsilon) {
            boolean front, back;
            int i;
            float d;

            front = false;
            back = false;
            for (i = 0; i < numPoints; i++) {
                d = plane.Distance(p[i].ToVec3());
                if (d < -epsilon) {
                    if (front) {
                        return SIDE_CROSS;
                    }
                    back = true;
                    continue;
                } else if (d > epsilon) {
                    if (back) {
                        return SIDE_CROSS;
                    }
                    front = true;
                    continue;
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
//
        private static final float WCONVEX_EPSILON = 0.2f;

        public boolean PlanesConcave(final idWinding w2, final idVec3 normal1, final idVec3 normal2, float dist1, float dist2) {
            int i;

            // check if one of the points of winding 1 is at the back of the plane of winding 2
            for (i = 0; i < numPoints; i++) {
                if (normal2.oMultiply(p[i].ToVec3()) - dist2 > WCONVEX_EPSILON) {
                    return true;
                }
            }
            // check if one of the points of winding 2 is at the back of the plane of winding 1
            for (i = 0; i < w2.numPoints; i++) {
                if (normal1.oMultiply(w2.p[i].ToVec3()) - dist1 > WCONVEX_EPSILON) {
                    return true;
                }
            }

            return false;
        }

        public boolean PointInside(final idVec3 normal, final idVec3 point, final float epsilon) {
            int i;
            idVec3 dir, n, pointvec;

            for (i = 0; i < numPoints; i++) {
                dir = p[(i + 1) % numPoints].ToVec3().oMinus(p[i].ToVec3());
                pointvec = point.oMinus(p[i].ToVec3());

                n = dir.Cross(normal);

                if (pointvec.oMultiply(n) < -epsilon) {
                    return false;
                }
            }
            return true;
        }

        public boolean LineIntersection(final idPlane windingPlane, final idVec3 start, final idVec3 end) {
            return LineIntersection(windingPlane, start, end, false);
        }

        // returns true if the line or ray intersects the winding
        public boolean LineIntersection(final idPlane windingPlane, final idVec3 start, final idVec3 end, boolean backFaceCull) {
            float front, back, frac;
            idVec3 mid = new idVec3();

            front = windingPlane.Distance(start);
            back = windingPlane.Distance(end);

            // if both points at the same side of the plane
            if (front < 0.0f && back < 0.0f) {
                return false;
            }

            if (front > 0.0f && back > 0.0f) {
                return false;
            }

            // if back face culled
            if (backFaceCull && front < 0.0f) {
                return false;
            }

            // get point of intersection with winding plane
            if (idMath.Fabs(front - back) < 0.0001f) {
                mid = end;
            } else {
                frac = front / (front - back);
                mid.oSet(0, start.oGet(0) + (end.oGet(0) - start.oGet(0)) * frac);
                mid.oSet(1, start.oGet(1) + (end.oGet(1) - start.oGet(1)) * frac);
                mid.oSet(2, start.oGet(2) + (end.oGet(2) - start.oGet(2)) * frac);
            }

            return PointInside(windingPlane.Normal(), mid, 0.0f);
        }

        public boolean RayIntersection(final idPlane windingPlane, final idVec3 start, final idVec3 dir, float scale[]) {
            return RayIntersection(windingPlane, start, dir, scale, false);
        }

        // intersection point is start + dir * scale
        public boolean RayIntersection(final idPlane windingPlane, final idVec3 start, final idVec3 dir, float[] scale, boolean backFaceCull) {
            int i;
            boolean side, lastside = false;
            idPluecker pl1 = new idPluecker(), pl2 = new idPluecker();

            scale[0] = 0.0f;
            pl1.FromRay(start, dir);
            for (i = 0; i < numPoints; i++) {
                pl2.FromLine(p[i].ToVec3(), p[(i + 1) % numPoints].ToVec3());
                side = pl1.PermutedInnerProduct(pl2) > 0.0f;
                if (i != 0 && side != lastside) {
                    return false;
                }
                lastside = side;
            }
            if (!backFaceCull || lastside) {
                windingPlane.RayIntersection(start, dir, scale);
                return true;
            }
            return false;
        }

        public static float TriangleArea(final idVec3 a, final idVec3 b, final idVec3 c) {
            idVec3 v1, v2;
            idVec3 cross;

            v1 = b.oMinus(a);
            v2 = c.oMinus(a);
            cross = v1.Cross(v2);
            return 0.5f * cross.Length();
        }

        protected boolean EnsureAlloced(int n) {
            return EnsureAlloced(n, false);
        }

        protected boolean EnsureAlloced(int n, boolean keep) {
            if (n > allocedSize) {
                return ReAllocate(n, keep);
            }
            return true;
        }

        protected boolean ReAllocate(int n) {
            return ReAllocate(n, false);
        }

        protected boolean ReAllocate(int n, boolean keep) {
            idVec5[] oldP;

            oldP = p;
            n = (n + 3) & ~3;	// align up to multiple of four
            p = TempDump.allocArray(idVec5.class, n);
            if (oldP != null && keep) {
//			memcpy( p, oldP, numPoints * sizeof(p[0]) );
                System.arraycopy(oldP, 0, p, 0, numPoints);
            }
            allocedSize = n;

            return true;
        }

        @Override
        public boolean isNULL() {
            return NULL;
        }
    };
    /*
     ===============================================================================

     idFixedWinding is a fixed buffer size winding not using
     memory allocations.

     When an operation would overflow the fixed buffer a warning
     is printed and the operation is safely cancelled.

     ===============================================================================
     */
    public static final int MAX_POINTS_ON_WINDING = 64;

    public static class idFixedWinding extends idWinding {

        public idFixedWinding() {
            numPoints = 0;
            p = data;
            allocedSize = MAX_POINTS_ON_WINDING;
        }

        public idFixedWinding(final int n) {
            numPoints = 0;
            p = data;
            allocedSize = MAX_POINTS_ON_WINDING;
        }

        public idFixedWinding(final idVec3[] verts, final int n) {
            int i;

            numPoints = 0;
            p = data;
            allocedSize = MAX_POINTS_ON_WINDING;
            if (!EnsureAlloced(n)) {
                numPoints = 0;
                return;
            }
            for (i = 0; i < n; i++) {
                p[i].oSet(verts[i]);
                p[i].s = p[i].t = 0;
            }
            numPoints = n;
        }

        public idFixedWinding(final idVec3 normal, final float dist) {
            numPoints = 0;
            p = data;
            allocedSize = MAX_POINTS_ON_WINDING;
            BaseForPlane(normal, dist);
        }

        public idFixedWinding(final idPlane plane) {
            numPoints = 0;
            p = data;
            allocedSize = MAX_POINTS_ON_WINDING;
            BaseForPlane(plane);
        }

        public idFixedWinding(final idWinding winding) {
            int i;

            p = data;
            allocedSize = MAX_POINTS_ON_WINDING;
            if (!EnsureAlloced(winding.GetNumPoints())) {
                numPoints = 0;
                return;
            }
            for (i = 0; i < winding.GetNumPoints(); i++) {
                p[i] = new idVec5(winding.oGet(i));
            }
            numPoints = winding.GetNumPoints();
        }

        public idFixedWinding(final idFixedWinding winding) {
            int i;

            p = data;
            allocedSize = MAX_POINTS_ON_WINDING;
            if (!EnsureAlloced(winding.GetNumPoints())) {
                numPoints = 0;
                return;
            }
            for (i = 0; i < winding.GetNumPoints(); i++) {
                p[i] = new idVec5(winding.oGet(i));
            }
            numPoints = winding.GetNumPoints();
        }
//public	virtual			~idFixedWinding( void );
//

        @Override
        public idFixedWinding oSet(final idWinding winding) {
            int i;

            if (!EnsureAlloced(winding.GetNumPoints())) {
                numPoints = 0;
                return this;
            }
            for (i = 0; i < winding.GetNumPoints(); i++) {
                p[i] = new idVec5(winding.oGet(i));
            }
            numPoints = winding.GetNumPoints();
            return this;
        }

        @Override
        public void Clear() {
            numPoints = 0;
        }

        public int Split(idFixedWinding back, final idPlane plane) {
            return Split(back, plane, ON_EPSILON);
        }

        // splits the winding in a back and front part, 'this' becomes the front part
        // returns a SIDE_?
        public int Split(idFixedWinding back, final idPlane plane, final float epsilon) {
            int[] counts = new int[3];
            float[] dists = new float[MAX_POINTS_ON_WINDING + 4];
            byte[] sides = new byte[MAX_POINTS_ON_WINDING + 4];
            float dot;
            int i, j;
            idVec5 p1, p2;
            idVec5 mid = new idVec5();
            idFixedWinding out = new idFixedWinding();

            counts[SIDE_FRONT] = counts[SIDE_BACK] = counts[SIDE_ON] = 0;

            // determine sides for each point
            for (i = 0; i < numPoints; i++) {
                dists[i] = dot = plane.Distance(p[i].ToVec3());
                if (dot > epsilon) {
                    sides[i] = SIDE_FRONT;
                } else if (dot < -epsilon) {
                    sides[i] = SIDE_BACK;
                } else {
                    sides[i] = SIDE_ON;
                }
                counts[sides[i]]++;
            }

            if (0 == counts[SIDE_BACK]) {
                if (0 == counts[SIDE_FRONT]) {
                    return SIDE_ON;
                } else {
                    return SIDE_FRONT;
                }
            }

            if (0 == counts[SIDE_FRONT]) {
                return SIDE_BACK;
            }

            sides[i] = sides[0];
            dists[i] = dists[0];

            out.numPoints = 0;
            back.numPoints = 0;

            for (i = 0; i < numPoints; i++) {
                p1 = p[i];

                if (!out.EnsureAlloced(out.numPoints + 1, true)) {
                    return SIDE_FRONT;		// can't split -- fall back to original
                }
                if (!back.EnsureAlloced(back.numPoints + 1, true)) {
                    return SIDE_FRONT;		// can't split -- fall back to original
                }

                if (sides[i] == SIDE_ON) {
                    out.p[out.numPoints] = p1;
                    out.numPoints++;
                    back.p[back.numPoints] = p1;
                    back.numPoints++;
                    continue;
                }

                if (sides[i] == SIDE_FRONT) {
                    out.p[out.numPoints] = p1;
                    out.numPoints++;
                }
                if (sides[i] == SIDE_BACK) {
                    back.p[back.numPoints] = p1;
                    back.numPoints++;
                }

                if (sides[i + 1] == SIDE_ON || sides[i + 1] == sides[i]) {
                    continue;
                }

                if (!out.EnsureAlloced(out.numPoints + 1, true)) {
                    return SIDE_FRONT;		// can't split -- fall back to original
                }

                if (!back.EnsureAlloced(back.numPoints + 1, true)) {
                    return SIDE_FRONT;		// can't split -- fall back to original
                }

                // generate a split point
                j = i + 1;
                if (j >= numPoints) {
                    p2 = p[0];
                } else {
                    p2 = p[j];
                }

                dot = dists[i] / (dists[i] - dists[i + 1]);
                for (j = 0; j < 3; j++) {
                    // avoid round off error when possible
                    if (plane.Normal().oGet(j) == 1.0f) {
                        mid.oSet(j, plane.Dist());
                    } else if (plane.Normal().oGet(j) == -1.0f) {
                        mid.oSet(j, -plane.Dist());
                    } else {
                        mid.oSet(j, p1.oGet(j) + dot * (p2.oGet(j) - p1.oGet(j)));
                    }
                }
                mid.s = p1.s + dot * (p2.s - p1.s);
                mid.t = p1.t + dot * (p2.t - p1.t);

                out.p[out.numPoints] = mid;
                out.numPoints++;
                back.p[back.numPoints] = mid;
                back.numPoints++;
            }
            for (i = 0; i < out.numPoints; i++) {
                p[i] = out.p[i];
            }
            numPoints = out.numPoints;

            return SIDE_CROSS;
        }
        protected idVec5[] data = new idVec5[MAX_POINTS_ON_WINDING];	// point data

        @Override
        protected boolean ReAllocate(int n) {
            return ReAllocate(n, false);
        }

        @Override
        protected boolean ReAllocate(int n, boolean keep) {

            assert (n <= MAX_POINTS_ON_WINDING);

            if (n > MAX_POINTS_ON_WINDING) {
                idLib.common.Printf("WARNING: idFixedWinding -> MAX_POINTS_ON_WINDING overflowed\n");
                return false;
            }
            return true;
        }
    };
}
