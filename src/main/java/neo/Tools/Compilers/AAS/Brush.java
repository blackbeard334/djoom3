package neo.Tools.Compilers.AAS;

import static neo.TempDump.NOT;
import static neo.TempDump.SNOT;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.MAX_WORLD_COORD;
import static neo.idlib.Lib.MIN_WORLD_COORD;
import static neo.idlib.MapFile.CURRENT_MAP_VERSION;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_CROSS;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_ON;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.sys.win_shared.Sys_Milliseconds;

import java.util.Arrays;

import neo.Tools.Compilers.AAS.AASBuild.Allowance;
import neo.framework.File_h.idFile;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.PlaneSet.idPlaneSet;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Brush {

    /*
     ===============================================================================

     Brushes

     ===============================================================================
     */
    static final int     BRUSH_PLANESIDE_FRONT      = 1;
    static final int     BRUSH_PLANESIDE_BACK       = 2;
    static final int     BRUSH_PLANESIDE_BOTH       = (BRUSH_PLANESIDE_FRONT | BRUSH_PLANESIDE_BACK);
    static final int     BRUSH_PLANESIDE_FACING     = 4;
    //
    static final int     SFL_SPLIT                  = 0x0001;
    static final int     SFL_BEVEL                  = 0x0002;
    static final int     SFL_USED_SPLITTER          = 0x0004;
    static final int     SFL_TESTED_SPLITTER        = 0x0008;
    //
    static final int     BFL_NO_VALID_SPLITTERS     = 0x0001;
    //
    static final float   BRUSH_EPSILON              = 0.1f;
    static final float   BRUSH_PLANE_NORMAL_EPSILON = 0.00001f;
    static final float   BRUSH_PLANE_DIST_EPSILON   = 0.01f;
    //                                                
    static final int     OUTPUT_UPDATE_TIME         = 500;// update every 500 msec
    //
    static final float   BRUSH_BEVEL_EPSILON        = 0.1f;
    //
    static final boolean OUTPUT_CHOP_STATS          = false;

    //===============================================================
    //
    //	idBrushSide
    //
    //===============================================================
    static class idBrushSide {

        private int       flags;
        private int       planeNum;
        private idPlane   plane;
        private idWinding winding;

        // friend class idBrush;
        public idBrushSide() {
            this.flags = 0;
            this.planeNum = -1;
            this.winding = null;
        }

        public idBrushSide(final idPlane plane, int planeNum) {
            this.flags = 0;
            this.plane = plane;
            this.planeNum = planeNum;
            this.winding = null;
        }
        // ~idBrushSide();

        public int GetFlags() {
            return this.flags;
        }

        public void SetFlag(int flag) {
            this.flags |= flag;
        }

        public void RemoveFlag(int flag) {
            this.flags &= ~flag;
        }

        public idPlane GetPlane() {
            return this.plane;
        }

        public void SetPlaneNum(int num) {
            this.planeNum = num;
        }

        public int GetPlaneNum() {
            return this.planeNum;
        }

        public idWinding GetWinding() {
            return this.winding;
        }

        public idBrushSide Copy() {
            idBrushSide side;

            side = new idBrushSide(this.plane, this.planeNum);
            side.flags = this.flags;
            if (this.winding != null) {
                side.winding = this.winding.Copy();
            } else {
                side.winding = null;
            }
            return side;
        }

        public int Split(final idPlane splitPlane, idBrushSide[] front, idBrushSide[] back) {
            final idWinding frontWinding = new idWinding(), backWinding = new idWinding();

            assert (this.winding != null);

            front[0] = back[0] = null;

            this.winding.Split(splitPlane, 0.0f, frontWinding, backWinding);

            if (!frontWinding.isNULL()) {
                front[0] = new idBrushSide(this.plane, this.planeNum);
                front[0].winding = frontWinding;
                front[0].flags = this.flags;
            }

            if (!backWinding.isNULL()) {
                back[0] = new idBrushSide(this.plane, this.planeNum);
                back[0].winding = backWinding;
                back[0].flags = this.flags;
            }

            if (!frontWinding.isNULL() && !backWinding.isNULL()) {
                return PLANESIDE_CROSS;
            } else if (!frontWinding.isNULL()) {
                return PLANESIDE_FRONT;
            } else {
                return PLANESIDE_BACK;
            }
        }
    }

    //===============================================================
    //
    //	idBrush
    //
    //===============================================================
    static class idBrush {

        private idBrush             next;          // next brush in list
        private int                 entityNum;     // entity number in editor
        private int                 primitiveNum;  // primitive number in editor
        private int                 flags;         // brush flags
        private boolean             windingsValid; // set when side windings are valid
        private int                 contents;      // contents of brush
        private int                 planeSide;     // side of a plane this brush is on
        private int                 savedPlaneSide;// saved plane side
        private idBounds            bounds;        // brush bounds
        private idList<idBrushSide> sides;         // list with sides
        private boolean NULL = true;               // used with the Split(....), see idWinding for more info.
        //
        //
        // friend class idBrushList;

        public idBrush() {
            this.contents = this.flags = 0;
            this.bounds.Clear();
            this.sides.Clear();
            this.windingsValid = false;
        }
        // ~idBrush();

        public int GetFlags() {
            return this.flags;
        }

        public void SetFlag(int flag) {
            this.flags |= flag;
        }

        public void RemoveFlag(int flag) {
            this.flags &= ~flag;
        }

        public void SetEntityNum(int num) {
            this.entityNum = num;
        }

        public void SetPrimitiveNum(int num) {
            this.primitiveNum = num;
        }

        public void SetContents(int contents) {
            this.contents = contents;
        }

        public int GetContents() {
            return this.contents;
        }

        public idBounds GetBounds() {
            return this.bounds;
        }

        public float GetVolume() {
            int i;
            idWinding w;
            idVec3 corner;
            float d, area, volume;

            // grab the first valid point as a corner
            w = null;
            for (i = 0; i < this.sides.Num(); i++) {
                w = this.sides.oGet(i).winding;
                if (w != null) {
                    break;
                }
            }
            if (NOT(w)) {
                return 0.0f;
            }
            corner = w.oGet(0).ToVec3();

            // create tetrahedrons to all other sides
            volume = 0.0f;
            for (; i < this.sides.Num(); i++) {
                w = this.sides.oGet(i).winding;
                if (NOT(w)) {
                    continue;
                }
                d = -(corner.oMultiply(this.sides.oGet(i).plane.Normal()) - this.sides.oGet(i).plane.Dist());
                area = w.GetArea();
                volume += d * area;
            }

            return (volume * (1.0f / 3.0f));
        }

        public int GetNumSides() {
            return this.sides.Num();
        }

        public idBrushSide GetSide(int i) {
            return this.sides.oGet(i);
        }

        public void SetPlaneSide(int s) {
            this.planeSide = s;
        }

        public void SavePlaneSide() {
            this.savedPlaneSide = this.planeSide;
        }

        public int GetSavedPlaneSide() {
            return this.savedPlaneSide;
        }

        public boolean FromSides(idList<idBrushSide> sideList) {
            int i;

            for (i = 0; i < sideList.Num(); i++) {
                this.sides.Append(sideList.oGet(i));
            }

            sideList.Clear();

            return CreateWindings();
        }

        public boolean FromWinding(final idWinding w, final idPlane windingPlane) {
            int i, j, bestAxis;
            final idPlane plane = new idPlane();
            idVec3 normal, axialNormal;

            this.sides.Append(new idBrushSide(windingPlane, -1));
            this.sides.Append(new idBrushSide(windingPlane.oNegative(), -1));

            bestAxis = 0;
            for (i = 1; i < 3; i++) {
                if (idMath.Fabs(windingPlane.Normal().oGet(i)) > idMath.Fabs(windingPlane.Normal().oGet(bestAxis))) {
                    bestAxis = i;
                }
            }
            axialNormal = getVec3_origin();
            if (windingPlane.Normal().oGet(bestAxis) > 0.0f) {
                axialNormal.oSet(bestAxis, 1.0f);
            } else {
                axialNormal.oSet(bestAxis, -1.0f);
            }

            for (i = 0; i < w.GetNumPoints(); i++) {
                j = (i + 1) % w.GetNumPoints();
                normal = (w.oGet(j).ToVec3().oMinus(w.oGet(i).ToVec3())).Cross(axialNormal);
                if (normal.Normalize() < 0.5f) {
                    continue;
                }
                plane.SetNormal(normal);
                plane.FitThroughPoint(w.oGet(j).ToVec3());
                this.sides.Append(new idBrushSide(plane, -1));
            }

            if (this.sides.Num() < 4) {
                for (i = 0; i < this.sides.Num(); i++) {
//			delete sides[i];
                    this.sides.oSet(i, null);
                }
                this.sides.Clear();
                return false;
            }

            this.sides.oGet(0).winding = w.Copy();
            this.windingsValid = true;
            BoundBrush(null);

            return true;
        }

        public boolean FromBounds(final idBounds bounds) {
            int axis, dir;
            idVec3 normal;
            final idPlane plane = new idPlane();

            for (axis = 0; axis < 3; axis++) {
                for (dir = -1; dir <= 1; dir += 2) {
                    normal = getVec3_origin();
                    normal.oSet(axis, dir);
                    plane.SetNormal(normal);
                    plane.SetDist(dir * bounds.oGet(dir == 1 ? 1 : 0, axis));
                    this.sides.Append(new idBrushSide(plane, -1));
                }
            }

            return CreateWindings();
        }

        public void Transform(final idVec3 origin, final idMat3 axis) {
            int i;
            boolean transformed = false;

            if (axis.IsRotated()) {
                for (i = 0; i < this.sides.Num(); i++) {
                    this.sides.oGet(i).plane.RotateSelf(getVec3_origin(), axis);
                }
                transformed = true;
            }
            if (!origin.equals(getVec3_origin())) {
                for (i = 0; i < this.sides.Num(); i++) {
                    this.sides.oGet(i).plane.TranslateSelf(origin);
                }
                transformed = true;
            }
            if (transformed) {
                CreateWindings();
            }
        }

        public idBrush Copy() {
            int i;
            idBrush b;

            b = new idBrush();
            b.entityNum = this.entityNum;
            b.primitiveNum = this.primitiveNum;
            b.contents = this.contents;
            b.windingsValid = this.windingsValid;
            b.bounds = this.bounds;
            for (i = 0; i < this.sides.Num(); i++) {
                b.sides.Append(this.sides.oGet(i).Copy());
            }
            return b;
        }

        public boolean TryMerge(final idBrush brush, final idPlaneSet planeList) {
            int i, j, k, l, m, seperatingPlane;
            final idBrush[] brushes = new idBrush[2];
            idWinding w;
            idPlane plane;

            // brush bounds should overlap
            for (i = 0; i < 3; i++) {
                if (this.bounds.oGet(0, i) > (brush.bounds.oGet(1, i) + 0.1f)) {
                    return false;
                }
                if (this.bounds.oGet(1, i) < (brush.bounds.oGet(0, i) - 0.1f)) {
                    return false;
                }
            }

            // the brushes should share an opposite plane
            seperatingPlane = -1;
            for (i = 0; i < GetNumSides(); i++) {
                for (j = 0; j < brush.GetNumSides(); j++) {
                    if (GetSide(i).GetPlaneNum() == (brush.GetSide(j).GetPlaneNum() ^ 1)) {
                        // may only have one seperating plane
                        if (seperatingPlane != -1) {
                            return false;
                        }
                        seperatingPlane = GetSide(i).GetPlaneNum();
                        break;
                    }
                }
            }
            if (seperatingPlane == -1) {
                return false;
            }

            brushes[0] = this;
            brushes[1] = brush;

            for (i = 0; i < 2; i++) {

                j = SNOT(i);

                for (k = 0; k < brushes[i].GetNumSides(); k++) {

                    // if the brush side plane is the seprating plane
                    if (0 == ((brushes[i].GetSide(k).GetPlaneNum() ^ seperatingPlane) >> 1)) {
                        continue;
                    }

                    plane = brushes[i].GetSide(k).GetPlane();

                    // all the non seperating brush sides of the other brush should be at the back or on the plane
                    for (l = 0; l < brushes[j].GetNumSides(); l++) {

                        w = brushes[j].GetSide(l).GetWinding();
                        if (NOT(w)) {
                            continue;
                        }

                        if (0 == ((brushes[j].GetSide(l).GetPlaneNum() ^ seperatingPlane) >> 1)) {
                            continue;
                        }

                        for (m = 0; m < w.GetNumPoints(); m++) {
                            if (plane.Distance(w.oGet(m).ToVec3()) > 0.1f) {
                                return false;
                            }
                        }
                    }
                }
            }

            // add any sides from the other brush to this brush
            for (i = 0; i < brush.GetNumSides(); i++) {
                for (j = 0; j < GetNumSides(); j++) {
                    if (0 == ((brush.GetSide(i).GetPlaneNum() ^ GetSide(j).GetPlaneNum()) >> 1)) {
                        break;
                    }
                }
                if (j < GetNumSides()) {
                    this.sides.oGet(j).flags &= brush.GetSide(i).GetFlags();
                    continue;
                }
                this.sides.Append(brush.GetSide(i).Copy());
            }

            // remove any side from this brush that is the opposite of a side of the other brush
            for (i = 0; i < GetNumSides(); i++) {
                for (j = 0; j < brush.GetNumSides(); j++) {
                    if (GetSide(i).GetPlaneNum() == (brush.GetSide(j).GetPlaneNum() ^ 1)) {
                        break;
                    }
                }
                if (j < brush.GetNumSides()) {
//			delete sides[i];
                    this.sides.RemoveIndex(i);
                    i--;
//                    continue;
                }
            }

            this.contents |= brush.contents;

            CreateWindings();
            BoundBrush();

            return true;
        }

        // returns true if the brushes did intersect
        public boolean Subtract(final idBrush b, idBrushList list) {
            int i;
            final idBrush front = new idBrush(), back = new idBrush();
            idBrush in;

            list.Clear();
            in = this;
            for (i = 0; (i < b.sides.Num()) && (in != null); i++) {

                in.Split(b.sides.oGet(i).plane, b.sides.oGet(i).planeNum, front, back);

//                if (!in.equals(this)) {
//			delete in;
//                    in = null;
//                }
                if (!front.isNULL()) {
                    list.AddToTail(front);
                }
                in = back;
            }
            // if didn't really intersect
            if (!NOT(in)) {
                list.Free();
                return false;
            }

//	delete in;
            return true;
        }

        // split the brush into a front and back brush
        public int Split(final idPlane plane, int planeNum, idBrush front, idBrush back) {//TODO:generic function pointer class.
            int res, i, j;
            idBrushSide side;
            final idBrushSide[] frontSide = {null}, backSide = {null};
            float dist, maxBack, maxFront;
            float[] maxBackWinding, maxFrontWinding;
            idWinding w, mid;

            assert (this.windingsValid);

//            if (front != null) {
//                front[0] = null;
//            }
//            if (back != null) {
//                back[0] = null;
//            }
//            
            res = this.bounds.PlaneSide(plane, -BRUSH_EPSILON);
            if (res == PLANESIDE_FRONT) {
                if (front != null) {
                    front.oSet(Copy());
                }
                return res;
            }
            if (res == PLANESIDE_BACK) {
                if (back != null) {
                    back.oSet(Copy());
                }
                return res;
            }

            maxBackWinding = new float[this.sides.Num()];
            maxFrontWinding = new float[this.sides.Num()];

            maxFront = maxBack = 0.0f;
            for (i = 0; i < this.sides.Num(); i++) {
                side = this.sides.oGet(i);

                w = side.winding;

                if (NOT(w)) {
                    continue;
                }

                maxBackWinding[i] = 10.0f;
                maxFrontWinding[i] = -10.0f;

                for (j = 0; j < w.GetNumPoints(); j++) {

                    dist = plane.Distance(w.oGet(j).ToVec3());
                    if (dist > maxFrontWinding[i]) {
                        maxFrontWinding[i] = dist;
                    }
                    if (dist < maxBackWinding[i]) {
                        maxBackWinding[i] = dist;
                    }
                }

                if (maxFrontWinding[i] > maxFront) {
                    maxFront = maxFrontWinding[i];
                }
                if (maxBackWinding[i] < maxBack) {
                    maxBack = maxBackWinding[i];
                }
            }

            if (maxFront < BRUSH_EPSILON) {
                if (back != null) {
                    back.oSet(Copy());
                }
                return PLANESIDE_BACK;
            }

            if (maxBack > -BRUSH_EPSILON) {
                if (front != null) {
                    front.oSet(Copy());
                }
                return PLANESIDE_FRONT;
            }

            mid = new idWinding(plane.Normal(), plane.Dist());

            for (i = 0; (i < this.sides.Num()) && (mid != null); i++) {
                mid = mid.Clip(this.sides.oGet(i).plane.oNegative(), BRUSH_EPSILON, false);
            }

            if (mid != null) {
                if (mid.IsTiny()) {
//			delete mid;
                    mid = null;
                } else if (mid.IsHuge()) {
                    // if the winding is huge then the brush is unbounded
                    common.Warning("brush %d on entity %d is unbounded"
                            + "( %1.2f %1.2f %1.2f )-( %1.2f %1.2f %1.2f )-( %1.2f %1.2f %1.2f )", this.primitiveNum, this.entityNum,
                            this.bounds.oGet(0, 0), this.bounds.oGet(0, 1), this.bounds.oGet(0, 2),
                            this.bounds.oGet(1, 0), this.bounds.oGet(1, 1), this.bounds.oGet(1, 2),
                            this.bounds.oGet(1, 0) - this.bounds.oGet(0, 0), this.bounds.oGet(1, 1) - this.bounds.oGet(0, 1), this.bounds.oGet(1, 2) - this.bounds.oGet(0, 2));
//			delete mid;
                    mid = null;
                }
            }

            if (NOT(mid)) {
                if (maxFront > -maxBack) {
                    if (front != null) {
                        front.oSet(Copy());
                    }
                    return PLANESIDE_FRONT;
                } else {
                    if (back != null) {
                        back.oSet(Copy());
                    }
                    return PLANESIDE_BACK;
                }
            }

            if (NOT(front) && NOT(back)) {
//		delete mid;
                return PLANESIDE_CROSS;
            }

            front.oSet(new idBrush());
            front.SetContents(this.contents);
            front.SetEntityNum(this.entityNum);
            front.SetPrimitiveNum(this.primitiveNum);
            back.oSet(new idBrush());
            back.SetContents(this.contents);
            back.SetEntityNum(this.entityNum);
            back.SetPrimitiveNum(this.primitiveNum);

            for (i = 0; i < this.sides.Num(); i++) {
                side = this.sides.oGet(i);

                if (NOT(side.winding)) {
                    continue;
                }

                // if completely at the front
                if (maxBackWinding[i] >= BRUSH_EPSILON) {
                    front.sides.Append(side.Copy());
                } // if completely at the back
                else if (maxFrontWinding[i] <= -BRUSH_EPSILON) {
                    back.sides.Append(side.Copy());
                } else {
                    // split the side
                    side.Split(plane, frontSide, backSide);
                    if (frontSide[0] != null) {
                        front.sides.Append(frontSide[0]);
                    } else if (maxFrontWinding[i] > -BRUSH_EPSILON) {
                        // favor an overconstrained brush
                        side = side.Copy();
                        side.winding = side.winding.Clip(new idPlane(plane.Normal(), (plane.Dist() - (BRUSH_EPSILON + 0.02f))), 0.01f, true);
                        assert (side.winding != null);
                        front.sides.Append(side);
                    }
                    if (backSide[0] != null) {
                        back.sides.Append(backSide[0]);
                    } else if (maxBackWinding[i] < BRUSH_EPSILON) {
                        // favor an overconstrained brush
                        side = side.Copy();
                        side.winding = side.winding.Clip(new idPlane(plane.Normal().oNegative(), -(plane.Dist() + (BRUSH_EPSILON + 0.02f))), 0.01f, true);
                        assert (side.winding != null);
                        back.sides.Append(side);
                    }
                }
            }

            side = new idBrushSide(plane.oNegative(), planeNum ^ 1);
            side.winding = mid.Reverse();
            side.flags |= SFL_SPLIT;
            front.sides.Append(side);
            front.windingsValid = true;
            front.BoundBrush(this);

            side = new idBrushSide(plane, planeNum);
            side.winding = mid;
            side.flags |= SFL_SPLIT;
            back.sides.Append(side);
            back.windingsValid = true;
            back.BoundBrush(this);

            return PLANESIDE_CROSS;
        }

        // expand the brush for an axial bounding box
        public void ExpandForAxialBox(final idBounds bounds) {
            int i, j;
            idBrushSide side;
            final idVec3 v = new idVec3();

            AddBevelsForAxialBox();

            for (i = 0; i < this.sides.Num(); i++) {
                side = this.sides.oGet(i);

                for (j = 0; j < 3; j++) {
                    if (side.plane.Normal().oGet(j) > 0.0f) {
                        v.oSet(j, bounds.oGet(0, j));
                    } else {
                        v.oSet(j, bounds.oGet(1, j));
                    }
                }

                side.plane.SetDist(side.plane.Dist() + v.oMultiply(side.plane.Normal().oNegative()));
            }

            if (!CreateWindings()) {
                common.Error("idBrush::ExpandForAxialBox: brush %d on entity %d imploded", this.primitiveNum, this.entityNum);
            }

            /*
             // after expansion at least all non bevel sides should have a winding
             for ( i = 0; i < sides.Num(); i++ ) {
             side = sides[i];
             if ( !side->winding ) {
             if ( !( side->flags & SFL_BEVEL ) ) {
             int shit = 1;
             }
             }
             }
             */
        }

        // next brush in list
        public idBrush Next() {
            return this.next;
        }

        private boolean CreateWindings() {
            int i, j;
            idBrushSide side;

            this.bounds.Clear();
            for (i = 0; i < this.sides.Num(); i++) {
                side = this.sides.oGet(i);

//		if ( side.winding!=null ) {
//			delete side.winding;
//		}
                side.winding = new idWinding(side.plane.Normal(), side.plane.Dist());

                for (j = 0; (j < this.sides.Num()) && (side.winding != null); j++) {
                    if (i == j) {
                        continue;
                    }
                    // keep the winding if on the clip plane
                    side.winding = side.winding.Clip(this.sides.oGet(j).plane.oNegative(), BRUSH_EPSILON, true);
                }

                if (side.winding != null) {
                    for (j = 0; j < side.winding.GetNumPoints(); j++) {
                        this.bounds.AddPoint(side.winding.oGet(j).ToVec3());
                    }
                }
            }

            if (this.bounds.oGet(0, 0) > this.bounds.oGet(1, 0)) {
                return false;
            }
            for (i = 0; i < 3; i++) {
                if ((this.bounds.oGet(0, i) < MIN_WORLD_COORD) || (this.bounds.oGet(1, i) > MAX_WORLD_COORD)) {
                    return false;
                }
            }

            this.windingsValid = true;

            return true;
        }

        private void BoundBrush(final idBrush original /*= NULL*/) {
            int i, j;
            idBrushSide side;
            idWinding w;

            assert (this.windingsValid);

            this.bounds.Clear();
            for (i = 0; i < this.sides.Num(); i++) {
                side = this.sides.oGet(i);

                w = side.winding;

                if (NOT(w)) {
                    continue;
                }

                for (j = 0; j < w.GetNumPoints(); j++) {
                    this.bounds.AddPoint(w.oGet(j).ToVec3());
                }
            }

            if (this.bounds.oGet(0, 0) > this.bounds.oGet(1, 0)) {
                if (original != null) {
                    final idBrushMap bm = new idBrushMap("error_brush", "_original");
                    bm.WriteBrush(original);
//			delete bm;
                }
                common.Error("idBrush::BoundBrush: brush %d on entity %d without windings", this.primitiveNum, this.entityNum);
            }

            for (i = 0; i < 3; i++) {
                if ((this.bounds.oGet(0, i) < MIN_WORLD_COORD) || (this.bounds.oGet(1, i) > MAX_WORLD_COORD)) {
                    if (original != null) {
                        final idBrushMap bm = new idBrushMap("error_brush", "_original");
                        bm.WriteBrush(original);
//				delete bm;
                    }
                    common.Error("idBrush::BoundBrush: brush %d on entity %d is unbounded", this.primitiveNum, this.entityNum);
                }
            }
        }

        private void BoundBrush() {
            BoundBrush(null);
        }

        private void AddBevelsForAxialBox() {
            int axis, dir, i, j, k, l, order;
            idBrushSide side, newSide;
            final idPlane plane = new idPlane();
            idVec3 normal, vec;
            idWinding w, w2;
            float d, minBack;

            assert (this.windingsValid);

            // add the axial planes
            order = 0;
            for (axis = 0; axis < 3; axis++) {

                for (dir = -1; dir <= 1; dir += 2, order++) {

                    // see if the plane is already present
                    for (i = 0; i < this.sides.Num(); i++) {
                        if (dir > 0) {
                            if (this.sides.oGet(i).plane.Normal().oGet(axis) >= 0.9999f) {
                                break;
                            }
                        } else {
                            if (this.sides.oGet(i).plane.Normal().oGet(axis) <= -0.9999f) {
                                break;
                            }
                        }
                    }

                    if (i >= this.sides.Num()) {
                        normal = getVec3_origin();
                        normal.oSet(axis, dir);
                        plane.SetNormal(normal);
                        plane.SetDist(dir * this.bounds.oGet(((dir == 1) ? 1 : 0), axis));
                        newSide = new idBrushSide(plane, -1);
                        newSide.SetFlag(SFL_BEVEL);
                        this.sides.Append(newSide);
                    }
                }
            }

            // if the brush is pure axial we're done
            if (this.sides.Num() == 6) {
                return;
            }

            // test the non-axial plane edges
            for (i = 0; i < this.sides.Num(); i++) {
                side = this.sides.oGet(i);
                w = side.winding;
                if (NOT(w)) {
                    continue;
                }

                for (j = 0; j < w.GetNumPoints(); j++) {
                    k = (j + 1) % w.GetNumPoints();
                    vec = w.oGet(j).ToVec3().oMinus(w.oGet(k).ToVec3());
                    if (vec.Normalize() < 0.5f) {
                        continue;
                    }
                    for (k = 0; k < 3; k++) {
                        if ((vec.oGet(k) == 1.0f) || (vec.oGet(k) == -1.0f) || ((vec.oGet(k) == 0.0f) && (vec.oGet((k + 1) % 3) == 0.0f))) {
                            break;	// axial
                        }
                    }
                    if (k < 3) {
                        continue;	// only test non-axial edges
                    }

                    // try the six possible slanted axials from this edge
                    for (axis = 0; axis < 3; axis++) {

                        for (dir = -1; dir <= 1; dir += 2) {

                            // construct a plane
                            normal = getVec3_origin();
                            normal.oSet(axis, dir);
                            normal = vec.Cross(normal);
                            if (normal.Normalize() < 0.5f) {
                                continue;
                            }
                            plane.SetNormal(normal);
                            plane.FitThroughPoint(w.oGet(j).ToVec3());

                            // if all the points on all the sides are
                            // behind this plane, it is a proper edge bevel
                            for (k = 0; k < this.sides.Num(); k++) {

                                // if this plane has allready been used, skip it
                                if (plane.Compare(this.sides.oGet(k).plane, 0.001f, 0.1f)) {
                                    break;
                                }

                                w2 = this.sides.oGet(k).winding;
                                if (NOT(w2)) {
                                    continue;
                                }
                                minBack = 0.0f;
                                for (l = 0; l < w2.GetNumPoints(); l++) {
                                    d = plane.Distance(w2.oGet(l).ToVec3());
                                    if (d > BRUSH_BEVEL_EPSILON) {
                                        break;	// point at the front
                                    }
                                    if (d < minBack) {
                                        minBack = d;
                                    }
                                }
                                // if some point was at the front
                                if (l < w2.GetNumPoints()) {
                                    break;
                                }
                                // if no points at the back then the winding is on the bevel plane
                                if (minBack > -BRUSH_BEVEL_EPSILON) {
                                    break;
                                }
                            }

                            if (k < this.sides.Num()) {
                                continue;	// wasn't part of the outer hull
                            }

                            // add this plane
                            newSide = new idBrushSide(plane, -1);
                            newSide.SetFlag(SFL_BEVEL);
                            this.sides.Append(newSide);
                        }
                    }
                }
            }
        }

        private boolean RemoveSidesWithoutWinding() {
            int i;

            for (i = 0; i < this.sides.Num(); i++) {

                if (this.sides.oGet(i).winding != null) {
                    continue;
                }

                this.sides.RemoveIndex(i);
                i--;
            }

            return (this.sides.Num() >= 4);
        }

        public boolean isNULL() {
            return this.NULL;
        }

        private void oSet(idBrush Copy) {
            this.NULL = false;
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    //===============================================================
    //
    //	idBrushList
    //
    //===============================================================
    static class idBrushList {

        private idBrush head;
        private idBrush tail;
        private int numBrushes;
        private int numBrushSides;
        //
        //

        public idBrushList() {
            this.numBrushes = this.numBrushSides = 0;
            this.head = this.tail = null;
        }

        private void oSet(idBrushList keep) {
            this.head = keep.head;
            this.tail = keep.tail;
            this.numBrushes = keep.numBrushes;
            this.numBrushSides = keep.numBrushSides;
        }
        // ~idBrushList();

        public int Num() {
            return this.numBrushes;
        }

        public int NumSides() {
            return this.numBrushSides;
        }

        public idBrush Head() {
            return this.head;
        }

        public idBrush Tail() {
            return this.tail;
        }

        public void Clear() {
            this.head = this.tail = null;
            this.numBrushes = 0;
        }

        public boolean IsEmpty() {
            return (this.numBrushes == 0);
        }

        public idBounds GetBounds() {
            final idBounds bounds = new idBounds();
            idBrush b;

            bounds.Clear();
            for (b = Head(); b != null; b = b.Next()) {
                bounds.oPluSet(b.GetBounds());
            }
            return bounds;
        }

        // add brush to the tail of the list
        public void AddToTail(idBrush brush) {
            brush.next = null;
            if (this.tail != null) {
                this.tail.next = brush;
            }
            this.tail = brush;
            if (NOT(this.head)) {
                this.head = brush;
            }
            this.numBrushes++;
            this.numBrushSides += brush.sides.Num();
        }

        // add list to the tail of the list
        public void AddToTail(idBrushList list) {
            idBrush brush, next;

            for (brush = list.head; brush != null; brush = next) {
                next = brush.next;
                brush.next = null;
                if (this.tail != null) {
                    this.tail.next = brush;
                }
                this.tail = brush;
                if (NOT(this.head)) {
                    this.head = brush;
                }
                this.numBrushes++;
                this.numBrushSides += brush.sides.Num();
            }
            list.head = list.tail = null;
            list.numBrushes = 0;
        }

        // add brush to the front of the list
        public void AddToFront(idBrush brush) {
            brush.next = this.head;
            this.head = brush;
            if (NOT(this.tail)) {
                this.tail = brush;
            }
            this.numBrushes++;
            this.numBrushSides += brush.sides.Num();
        }

        // add list to the front of the list
        public void AddToFront(idBrushList list) {
            idBrush brush, next;

            for (brush = list.head; brush != null; brush = next) {
                next = brush.next;
                brush.next = this.head;
                this.head = brush;
                if (NOT(this.tail)) {
                    this.tail = brush;
                }
                this.numBrushes++;
                this.numBrushSides += brush.sides.Num();
            }
            list.head = list.tail = null;
            list.numBrushes = 0;
        }

        // remove the brush from the list
        public void Remove(idBrush brush) {
            idBrush b, last;

            last = null;
            for (b = this.head; b != null; b = b.next) {
                if (b.equals(brush)) {
                    if (last != null) {
                        last.next = b.next;
                    } else {
                        this.head = b.next;
                    }
                    if (b.equals(this.tail)) {
                        this.tail = last;
                    }
                    this.numBrushes--;
                    this.numBrushSides -= brush.sides.Num();
                    return;
                }
                last = b;
            }
        }

        // remove the brush from the list and delete the brush
        public void Delete(idBrush brush) {
            idBrush b, last;

            last = null;
            for (b = this.head; b != null; b = b.next) {
                if (b.equals(brush)) {
                    if (last != null) {
                        last.next = b.next;
                    } else {
                        this.head = b.next;
                    }
                    if (b.equals(this.tail)) {
                        this.tail = last;
                    }
                    this.numBrushes--;
                    this.numBrushSides -= b.sides.Num();
//			delete b;
                    return;
                }
                last = b;
            }
        }

        // returns a copy of the brush list
        public idBrushList Copy() {
            idBrush brush;
            idBrushList list;

            list = new idBrushList();

            for (brush = this.head; brush != null; brush = brush.next) {
                list.AddToTail(brush.Copy());
            }
            return list;
        }

        // delete all brushes in the list
        public void Free() {
            idBrush brush, next;

            for (brush = this.head; brush != null; brush = next) {
                next = brush.next;
//		delete brush;
            }
            this.head = this.tail = null;
            this.numBrushes = this.numBrushSides = 0;
        }

        // split the brushes in the list into two lists
        public void Split(final idPlane plane, int planeNum, idBrushList frontList, idBrushList backList, boolean useBrushSavedPlaneSide /*= false*/) {
            idBrush b;
            final idBrush front = new idBrush(), back = new idBrush();

            frontList.Clear();
            backList.Clear();

            if (!useBrushSavedPlaneSide) {
                for (b = this.head; b != null; b = b.next) {
                    b.Split(plane, planeNum, front, back);
                    if (!front.isNULL()) {
                        frontList.AddToTail(front);
                    }
                    if (!back.isNULL()) {
                        backList.AddToTail(back);
                    }
                }
                return;
            }

            for (b = this.head; b != null; b = b.next) {
                if ((b.savedPlaneSide & BRUSH_PLANESIDE_BOTH) != 0) {
                    b.Split(plane, planeNum, front, back);
                    if (!front.isNULL()) {
                        frontList.AddToTail(front);
                    }
                    if (!back.isNULL()) {
                        backList.AddToTail(back);
                    }
                } else if ((b.savedPlaneSide & BRUSH_PLANESIDE_FRONT) != 0) {
                    frontList.AddToTail(b.Copy());
                } else {
                    backList.AddToTail(b.Copy());
                }
            }
        }

        public void Split(final idPlane plane, int planeNum, idBrushList frontList, idBrushList backList) {
            Split(plane, planeNum, frontList, backList, false);
        }

        // chop away all brush overlap
        public void Chop(Allowance chopAllowed) {
            idBrush b1, b2, next;
            final idBrushList sub1 = new idBrushList(), sub2 = new idBrushList(), keep = new idBrushList();
            int i, j, c1, c2;
            final idPlaneSet planeList = new idPlaneSet();

            if (OUTPUT_CHOP_STATS) {
                common.Printf("[Brush CSG]\n");
                common.Printf("%6d original brushes\n", this.Num());
            }

            CreatePlaneList(planeList);

            for (b1 = this.Head(); b1 != null; b1 = this.Head()) {

                for (b2 = b1.next; b2 != null; b2 = next) {

                    next = b2.next;

                    for (i = 0; i < 3; i++) {
                        if (b1.bounds.oGet(0, i) >= b2.bounds.oGet(1, i)) {
                            break;
                        }
                        if (b1.bounds.oGet(1, i) <= b2.bounds.oGet(0, i)) {
                            break;
                        }
                    }
                    if (i < 3) {
                        continue;
                    }

                    for (i = 0; i < b1.GetNumSides(); i++) {
                        for (j = 0; j < b2.GetNumSides(); j++) {
                            if (b1.GetSide(i).GetPlaneNum() == (b2.GetSide(j).GetPlaneNum() ^ 1)) {
                                // opposite planes, so not touching
                                break;
                            }
                        }
                        if (j < b2.GetNumSides()) {
                            break;
                        }
                    }
                    if (i < b1.GetNumSides()) {
                        continue;
                    }

                    sub1.Clear();
                    sub2.Clear();

                    c1 = 999999;
                    c2 = 999999;

                    // if b2 may chop up b1
                    if (NOT(chopAllowed) || chopAllowed.run(b2, b1)) {
                        if (!b1.Subtract(b2, sub1)) {
                            // didn't really intersect
                            continue;
                        }
                        if (sub1.IsEmpty()) {
                            // b1 is swallowed by b2
                            this.Delete(b1);
                            break;
                        }
                        c1 = sub1.Num();
                    }

                    // if b1 may chop up b2
                    if (NOT(chopAllowed) || chopAllowed.run(b1, b2)) {
                        if (!b2.Subtract(b1, sub2)) {
                            // didn't really intersect
                            continue;
                        }
                        if (sub2.IsEmpty()) {
                            // b2 is swallowed by b1
                            sub1.Free();
                            this.Delete(b2);
                            continue;
                        }
                        c2 = sub2.Num();
                    }

                    if (sub1.IsEmpty() && sub2.IsEmpty()) {
                        continue;
                    }

                    // don't allow too much fragmentation
                    if ((c1 > 2) && (c2 > 2)) {
                        sub1.Free();
                        sub2.Free();
                        continue;
                    }

                    if (c1 < c2) {
                        sub2.Free();
                        this.AddToTail(sub1);
                        this.Delete(b1);
                        break;
                    } else {
                        sub1.Free();
                        this.AddToTail(sub2);
                        this.Delete(b2);
                        continue;
                    }
                }

                if (NOT(2)) {
                    // b1 is no longer intersecting anything, so keep it
                    this.Remove(b1);
                    keep.AddToTail(b1);
                    if (OUTPUT_CHOP_STATS) {
                        DisplayRealTimeString("\r%6d", keep.numBrushes);
                    }
                }
            }

            this.oSet(keep);

            if (OUTPUT_CHOP_STATS) {
                common.Printf("\r%6d output brushes\n", Num());
            }
        }

        // merge brushes
        public void Merge(Allowance mergeAllowed) {
            final idPlaneSet planeList = new idPlaneSet();
            idBrush b1, b2, nextb2;
            int numMerges;

            common.Printf("[Brush Merge]\n");
            common.Printf("%6d original brushes\n", Num());

            CreatePlaneList(planeList);

            numMerges = 0;
            for (b1 = Head(); b1 != null; b1 = b1.next) {

                for (b2 = Head(); b2 != null; b2 = nextb2) {
                    nextb2 = b2.Next();

                    if (b2 == b1) {
                        continue;
                    }

                    if ((mergeAllowed != null) && !mergeAllowed.run(b1, b2)) {
                        continue;
                    }

                    if (b1.TryMerge(b2, planeList)) {
                        Delete(b2);
                        DisplayRealTimeString("\r%6d" + (++numMerges));
                        nextb2 = Head();
                    }
                }
            }

            common.Printf("\r%6d brushes merged\n", numMerges);
        }

        // set the given flag on all brush sides facing the plane
        public void SetFlagOnFacingBrushSides(final idPlane plane, int flag) {
            int i;
            idBrush b;
            idWinding w;

            for (b = this.head; b != null; b = b.next) {
                if (idMath.Fabs(b.GetBounds().PlaneDistance(plane)) > 0.1f) {
                    continue;
                }
                for (i = 0; i < b.GetNumSides(); i++) {
                    w = b.GetSide(i).GetWinding();
                    if (NOT(w)) {
                        if (b.GetSide(i).GetPlane().Compare(plane, BRUSH_PLANE_NORMAL_EPSILON, BRUSH_PLANE_DIST_EPSILON)) {
                            b.GetSide(i).SetFlag(flag);
                        }
                        continue;
                    }
                    if (w.PlaneSide(plane) == SIDE_ON) {
                        b.GetSide(i).SetFlag(flag);
                    }
                }
            }
        }

        // get a list with planes for all brushes in the list
        public void CreatePlaneList(idPlaneSet planeList) {
            int i;
            idBrush b;
            idBrushSide side;

            planeList.Resize(512, 128);
            for (b = Head(); b != null; b = b.Next()) {
                for (i = 0; i < b.GetNumSides(); i++) {
                    side = b.GetSide(i);
                    side.SetPlaneNum(planeList.FindPlane(side.GetPlane(), BRUSH_PLANE_NORMAL_EPSILON, BRUSH_PLANE_DIST_EPSILON));
                }
            }
        }

        // write a brush map with the brushes in the list
        public void WriteBrushMap(final idStr fileName, final idStr ext) {
            idBrushMap map;

            map = new idBrushMap(fileName, ext);
            map.WriteBrushList(this);
//	delete map;
        }
    }

    //===============================================================
    //
    //	idBrushMap
    //
    //===============================================================
    static class idBrushMap {

        private idFile fp;
        private idStr texture;
        private int brushCount;
        //
        //

        idBrushMap(final idStr fileName, final idStr ext) {
            this(fileName.toString(), ext.toString());
        }

        public idBrushMap(final String fileName, final String ext) {
            idStr qpath;

            qpath = new idStr(fileName);
            qpath.StripFileExtension();
            qpath.oPluSet(ext);
            qpath.SetFileExtension("map");

            common.Printf("writing %s...\n", qpath);

            this.fp = fileSystem.OpenFileWrite(qpath.toString(), "fs_devpath");
            if (NOT(this.fp)) {
                common.Error("Couldn't open %s\n", qpath);
                return;
            }

            this.texture.oSet("textures/washroom/btile01");

            this.fp.WriteFloatString("Version %1.2f\n", (float) CURRENT_MAP_VERSION);
            this.fp.WriteFloatString("{\n");
            this.fp.WriteFloatString("\"classname\" \"worldspawn\"\n");

            this.brushCount = 0;
        }

        // ~idBrushMap( void );
        public void SetTexture(final idStr textureName) {
            this.texture = textureName;
        }

        public void SetTexture(final String textureName) {
            this.SetTexture(new idStr(textureName));
        }

        public void WriteBrush(final idBrush brush) {
            int i;
            idBrushSide side;

            if (NOT(this.fp)) {
                return;
            }

            this.fp.WriteFloatString("// primitive %d\n{\nbrushDef3\n{\n", this.brushCount++);

            for (i = 0; i < brush.GetNumSides(); i++) {
                side = brush.GetSide(i);
                this.fp.WriteFloatString(" ( %f %f %f %f ) ", side.GetPlane().oGet(0), side.GetPlane().oGet(1), side.GetPlane().oGet(2), -side.GetPlane().Dist());
                this.fp.WriteFloatString("( ( 0.031250 0 0 ) ( 0 0.031250 0 ) ) %s 0 0 0\n", this.texture);

            }
            this.fp.WriteFloatString("}\n}\n");
        }

        public void WriteBrushList(final idBrushList brushList) {
            idBrush b;

            if (NOT(this.fp)) {
                return;
            }

            for (b = brushList.Head(); b != null; b = b.Next()) {
                WriteBrush(b);
            }
        }
    }

    /*
     ============
     DisplayRealTimeString
     ============
     */
    static void DisplayRealTimeString(String format, Object... args) {
//        va_list argPtr;
        String buf;//= new char[MAX_STRING_CHARS];
        int time;

        time = Sys_Milliseconds();
        if (time > (lastUpdateTime + OUTPUT_UPDATE_TIME)) {
//            va_start(argPtr, string);
//            vsprintf(buf, string, argPtr);
//            va_end(argPtr);
            buf = String.format(format, Arrays.toString(args));
            common.Printf(buf);
            lastUpdateTime = time;
        }
    }
    private static int lastUpdateTime;
}
