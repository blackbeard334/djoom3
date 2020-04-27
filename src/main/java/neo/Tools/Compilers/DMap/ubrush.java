package neo.Tools.Compilers.DMap;

import static neo.TempDump.NOT;
import static neo.Tools.Compilers.DMap.dmap.PLANENUM_LEAF;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import static neo.Tools.Compilers.DMap.gldraw.GLS_BeginScene;
import static neo.Tools.Compilers.DMap.gldraw.GLS_EndScene;
import static neo.Tools.Compilers.DMap.gldraw.GLS_Winding;
import static neo.Tools.Compilers.DMap.map.FindFloatPlane;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.MAX_WORLD_COORD;
import static neo.idlib.Lib.MIN_WORLD_COORD;
import static neo.idlib.math.Vector.VectorCopy;

import neo.Tools.Compilers.DMap.dmap.node_s;
import neo.Tools.Compilers.DMap.dmap.primitive_s;
import neo.Tools.Compilers.DMap.dmap.side_s;
import neo.Tools.Compilers.DMap.dmap.tree_s;
import neo.Tools.Compilers.DMap.dmap.uBrush_t;
import neo.Tools.Compilers.DMap.dmap.uEntity_t;
import neo.framework.File_h.idFile;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class ubrush {

    static final float CLIP_EPSILON = 0.1f;
    //
    static final int   PSIDE_FRONT  = 1;
    static final int   PSIDE_BACK   = 2;
    static final int   PSIDE_BOTH   = (PSIDE_FRONT | PSIDE_BACK);
    static final int   PSIDE_FACING = 4;
    //
    static int c_active_brushes;
    //
    static int c_nodes;
    //
    // if a brush just barely pokes onto the other side,
    // let it slide by without chopping
    static final double PLANESIDE_EPSILON = 0.001;
    //0.1

    /*
     ================
     CountBrushList
     ================
     */
    static int CountBrushList(uBrush_t brushes) {
        int c;

        c = 0;
        for (; brushes != null; brushes = (uBrush_t) brushes.next) {
            c++;
        }
        return c;
    }

    @Deprecated
    static int BrushSizeForSides(int numsides) {
        throw new UnsupportedOperationException();
//        int c;
//
//        // allocate a structure with a variable number of sides at the end
//        //	c = (int)&(((uBrush_t *)0).sides[numsides]);	// bounds checker complains about this
//        c = sizeof(uBrush_t) + sizeof(side_t) * (numsides - 6);
//
//        return c;
    }

    /*
     ================
     AllocBrush
     ================
     */
    @Deprecated
    static uBrush_t[] AllocBrush(int numsides) {
        throw new UnsupportedOperationException();
//        uBrush_t[] bb;
//        int c;
//
//        c = BrushSizeForSides(numsides);
//
//        bb = new uBrush_t[c];// Mem_Alloc(c);
//        //	memset (bb, 0, c);
//        c_active_brushes++;
//        return bb;
    }

    /*
     ================
     FreeBrush
     ================
     */
    @Deprecated
    static void FreeBrush(uBrush_t brushes) {
        int i;

        for (i = 0; i < brushes.numsides; i++) {
            if (brushes.sides[i].winding != null) {
                //			delete brushes.sides[i].winding;
                brushes.sides[i].winding = null;
            }
            if (brushes.sides[i].visibleHull != null) {
                //			delete brushes.sides[i].visibleHull;
                brushes.sides[i].visibleHull = null;
            }
        }
        brushes.clear();//Mem_Free(brushes);
        c_active_brushes--;
    }


    /*
     ================
     FreeBrushList
     ================
     */
    static void FreeBrushList(uBrush_t brushes) {
        uBrush_t next;

        for (; brushes != null; brushes = next) {
            next = (uBrush_t) brushes.next;

            FreeBrush(brushes);
        }
    }

    /*
     ==================
     CopyBrush

     Duplicates the brush, the sides, and the windings
     ==================
     */
    static uBrush_t CopyBrush(uBrush_t brush) {
        uBrush_t newBrush;
        int size;
        int i;
//
//        size = BrushSizeForSides(brush.numsides);
//
//        newbrush = AllocBrush(brush.numsides);
//        memcpy(newbrush, brush, size);

        newBrush = new uBrush_t(brush);
        c_active_brushes++;

        for (i = 0; i < brush.numsides; i++) {
            if (brush.sides[i].winding != null) {
                newBrush.sides[i].winding = brush.sides[i].winding.Copy();
            }
        }

        return newBrush;
    }


    /*
     ================
     DrawBrushList
     ================
     */
    static void DrawBrushList(uBrush_t brush) {
        int i;
        side_s s;

        GLS_BeginScene();
        for (; brush != null; brush = (uBrush_t) brush.next) {
            for (i = 0; i < brush.numsides; i++) {
                s = brush.sides[i];
                if (NOT(s.winding)) {
                    continue;
                }
                GLS_Winding(s.winding, 0);
            }
        }
        GLS_EndScene();
    }


    /*
     =============
     PrintBrush
     =============
     */
    static void PrintBrush(uBrush_t brush) {
        int i;

        common.Printf("brush: %p\n", brush);
        for (i = 0; i < brush.numsides; i++) {
            brush.sides[i].winding.Print();
            common.Printf("\n");
        }
    }

    /*
     ==================
     BoundBrush

     Sets the mins/maxs based on the windings
     returns false if the brush doesn't enclose a valid volume
     ==================
     */
    static boolean BoundBrush(uBrush_t brush) {
        int i, j;
        idWinding w;

        brush.bounds.Clear();
        for (i = 0; i < brush.numsides; i++) {
            w = brush.sides[i].winding;
            if (NOT(w)) {
                continue;
            }
            for (j = 0; j < w.GetNumPoints(); j++) {
                brush.bounds.AddPoint(w.oGet(j).ToVec3());
            }
        }

        for (i = 0; i < 3; i++) {
            if (brush.bounds.oGet(0, i) < MIN_WORLD_COORD || brush.bounds.oGet(1, i) > MAX_WORLD_COORD
                    || brush.bounds.oGet(0, i) >= brush.bounds.oGet(1, i)) {
                return false;
            }
        }

        return true;
    }

    /*
     ==================
     CreateBrushWindings

     makes basewindigs for sides and mins / maxs for the brush
     returns false if the brush doesn't enclose a valid volume
     ==================
     */
    static boolean CreateBrushWindings(uBrush_t brush) {
        int i, j;
        idWinding w;
        idPlane plane;
        side_s side;

        for (i = 0; i < brush.numsides; i++) {
            side = brush.sides[i];
            plane = dmapGlobals.mapPlanes.oGet(side.planenum);
            w = new idWinding(plane);
            for (j = 0; j < brush.numsides && w != null; j++) {
                if (i == j) {
                    continue;
                }
                if (brush.sides[j].planenum == (brush.sides[i].planenum ^ 1)) {
                    continue;		// back side clipaway
                }
                plane = dmapGlobals.mapPlanes.oGet(brush.sides[j].planenum ^ 1);
                w = w.Clip(plane, 0);//CLIP_EPSILON);
            }
            if (side.winding != null) {
                //			delete side.winding;
                side.winding = null;
            }
            side.winding = w;
        }

        return BoundBrush(brush);
    }

    /*
     ==================
     BrushFromBounds

     Creates a new axial brush
     ==================
     */
    static uBrush_t BrushFromBounds(final idBounds bounds) {
        uBrush_t b;
        int i;
        idPlane plane = new idPlane();

        b = new uBrush_t();//AllocBrush(6);
        c_active_brushes++;
        b.numsides = 6;
        for (i = 0; i < 3; i++) {
            plane.oSet(0, plane.oSet(1, plane.oSet(2, 0)));
            plane.oSet(i, 1);
            plane.oSet(3, -bounds.oGet(1, i));
            b.sides[i].planenum = FindFloatPlane(plane);

            plane.oSet(i, -1);
            plane.oSet(3, bounds.oGet(0, i));
            b.sides[3 + i].planenum = FindFloatPlane(plane);
        }

        CreateBrushWindings(b);

        return b;
    }

    /*
     ==================
     BrushVolume

     ==================
     */
    static float BrushVolume(uBrush_t brush) {
        int i;
        idWinding w;
        idVec3 corner = new idVec3();
        float d, area, volume;
        idPlane plane;

        if (NOT(brush)) {
            return 0;
        }

        // grab the first valid point as the corner
        w = null;
        for (i = 0; i < brush.numsides; i++) {
            w = brush.sides[i].winding;
            if (w != null) {
                break;
            }
        }
        if (NOT(w)) {
            return 0;
        }
        VectorCopy(w.oGet(0), corner);

        // make tetrahedrons to all other faces
        volume = 0;
        for (; i < brush.numsides; i++) {
            w = brush.sides[i].winding;
            if (NOT(w)) {
                continue;
            }
            plane = dmapGlobals.mapPlanes.oGet(brush.sides[i].planenum);
            d = -plane.Distance(corner);
            area = w.GetArea();
            volume += d * area;
        }

        volume /= 3;
        return volume;
    }


    /*
     ==================
     WriteBspBrushMap

     FIXME: use new brush format
     ==================
     */
    static void WriteBspBrushMap(final String name, uBrush_t list) {
        idFile f;
        side_s s;
        int i;
        idWinding w;

        common.Printf("writing %s\n", name);
        f = fileSystem.OpenFileWrite(name);

        if (NOT(f)) {
            common.Error("Can't write %s\b", name);
        }

        f.Printf("{\n\"classname\" \"worldspawn\"\n");

        for (; list != null; list = (uBrush_t) list.next) {
            f.Printf("{\n");
            for (s = list.sides[i = 0]; i < list.numsides; s = list.sides[++i]) {
                w = new idWinding(dmapGlobals.mapPlanes.oGet(s.planenum));

                f.Printf("( %d %d %d ) ", (int) w.oGet(0).oGet(0), (int) w.oGet(0).oGet(1), (int) w.oGet(0).oGet(2));
                f.Printf("( %d %d %d ) ", (int) w.oGet(1).oGet(0), (int) w.oGet(1).oGet(1), (int) w.oGet(1).oGet(2));
                f.Printf("( %d %d %d ) ", (int) w.oGet(2).oGet(0), (int) w.oGet(2).oGet(1), (int) w.oGet(2).oGet(2));

                f.Printf("notexture 0 0 0 1 1\n");
                //			delete w;
            }
            f.Printf("}\n");
        }
        f.Printf("}\n");

        fileSystem.CloseFile(f);

    }

    //=====================================================================================

    /*
     ====================
     FilterBrushIntoTree_r

     ====================
     */
    static int FilterBrushIntoTree_r(uBrush_t b, node_s node) {
        uBrush_t front = new uBrush_t(), back = new uBrush_t();
        int c;

        if (NOT(b)) {
            return 0;
        }

        // add it to the leaf list
        if (node.planenum == PLANENUM_LEAF) {
            b.next = node.brushlist;
            node.brushlist = b;

            // classify the leaf by the structural brush
            if (b.opaque) {
                node.opaque = true;
            }

            return 1;
        }

        // split it by the node plane
        SplitBrush(b, node.planenum, front, back);
        FreeBrush(b);

        c = 0;
        c += FilterBrushIntoTree_r(front, node.children[0]);
        c += FilterBrushIntoTree_r(back, node.children[1]);

        return c;
    }


    /*
     =====================
     FilterBrushesIntoTree

     Mark the leafs as opaque and areaportals and put brush
     fragments in each leaf so portal surfaces can be matched
     to materials
     =====================
     */
    static void FilterBrushesIntoTree(uEntity_t e) {
        primitive_s prim;
        uBrush_t b, newb;
        int r;
        int c_unique, c_clusters;

        common.Printf("----- FilterBrushesIntoTree -----\n");

        c_unique = 0;
        c_clusters = 0;
        for (prim = e.primitives; prim != null; prim = prim.next) {
            b = (uBrush_t) prim.brush;
            if (NOT(b)) {
                continue;
            }
            c_unique++;
            newb = CopyBrush(b);
            r = FilterBrushIntoTree_r(newb, e.tree.headnode);
            c_clusters += r;
        }

        common.Printf("%5d total brushes\n", c_unique);
        common.Printf("%5d cluster references\n", c_clusters);
    }

    /*
     ================
     AllocTree
     ================
     */
    static tree_s AllocTree() {
        tree_s tree;

        tree = new tree_s();// Mem_Alloc(sizeof(tree));
        //	memset (tree, 0, sizeof(*tree));
        tree.bounds.Clear();

        return tree;
    }

    /*
     ================
     AllocNode
     ================
     */
    static node_s AllocNode() {
        node_s node;

        node = new node_s();// Mem_Alloc(sizeof(node));
        //	memset (node, 0, sizeof(*node));

        return node;
    }

    //============================================================

    /*
     ==================
     BrushMostlyOnSide

     ==================
     */
    static int BrushMostlyOnSide(uBrush_t brush, idPlane plane) {
        int i, j;
        idWinding w;
        float d, max;
        int side;

        max = 0;
        side = PSIDE_FRONT;
        for (i = 0; i < brush.numsides; i++) {
            w = brush.sides[i].winding;
            if (NOT(w)) {
                continue;
            }
            for (j = 0; j < w.GetNumPoints(); j++) {
                d = plane.Distance(w.oGet(j).ToVec3());
                if (d > max) {
                    max = d;
                    side = PSIDE_FRONT;
                }
                if (-d > max) {
                    max = -d;
                    side = PSIDE_BACK;
                }
            }
        }
        return side;
    }

    /*
     ================
     SplitBrush

     Generates two new brushes, leaving the original
     unchanged
     ================
     */
    static void SplitBrush(uBrush_t brush, int planenum, uBrush_t front, uBrush_t back) {
        uBrush_t[] b = new uBrush_t[2];
        int i, j;
        idWinding w, midwinding;
        idWinding[] cw = new idWinding[2];
        side_s s, cs;
        float d, d_front, d_back;

//        front[0] = back[0] = null;
        idPlane plane = dmapGlobals.mapPlanes.oGet(planenum);

        // check all points
        d_front = d_back = 0;
        for (i = 0; i < brush.numsides; i++) {
            w = brush.sides[i].winding;
            if (NOT(w)) {
                continue;
            }
            for (j = 0; j < w.GetNumPoints(); j++) {
                d = plane.Distance(w.oGet(j).ToVec3());
                if (d > 0 && d > d_front) {
                    d_front = d;
                }
                if (d < 0 && d < d_back) {
                    d_back = d;
                }
            }
        }
        if (d_front < 0.1) // PLANESIDE_EPSILON)
        {	// only on back
            back.oSet(CopyBrush(brush));
            return;
        }
        if (d_back > -0.1) // PLANESIDE_EPSILON)
        {	// only on front
            front.oSet(CopyBrush(brush));
            return;
        }

        // create a new winding from the split plane
        w = new idWinding(plane);
        for (i = 0; i < brush.numsides && w != null; i++) {
            idPlane plane2 = dmapGlobals.mapPlanes.oGet(brush.sides[i].planenum ^ 1);
            w = w.Clip(plane2, 0); // PLANESIDE_EPSILON);
        }

        if (NOT(w) || w.IsTiny()) {
            // the brush isn't really split
            int side;

            side = BrushMostlyOnSide(brush, plane);
            if (side == PSIDE_FRONT) {
                front.oSet(CopyBrush(brush));
            }
            if (side == PSIDE_BACK) {
                back.oSet(CopyBrush(brush));
            }
            return;
        }

        if (w.IsHuge()) {
            common.Printf("WARNING: huge winding\n");
        }

        midwinding = w;

        // split it for real
        for (i = 0; i < 2; i++) {
            b[i] = new uBrush_t(brush);//AllocBrush(brush.numsides + 1);
//            memcpy(b[i], brush, sizeof(uBrush_t) - sizeof(brush.sides));
            c_active_brushes++;
            b[i].numsides = 0;
            b[i].next = null;
            b[i].original = brush.original;
        }

        // split all the current windings
        for (i = 0; i < brush.numsides; i++) {
            s = brush.sides[i];
            w = s.winding;
            if (NOT(w)) {
                continue;
            }
            w.Split(plane, 0 /*PLANESIDE_EPSILON*/, cw[0], cw[1]);
            for (j = 0; j < 2; j++) {
                if (NOT(cw[j])) {
                    continue;
                }
                /*
                 if ( cw[j].IsTiny() )
                 {
                 delete cw[j];
                 continue;
                 }
                 */
                cs = b[j].sides[b[j].numsides] = s;
                b[j].numsides++;
//                cs = s;
                cs.winding = cw[j];
            }
        }

        // see if we have valid polygons on both sides
        for (i = 0; i < 2; i++) {
            if (!BoundBrush(b[i])) {
                break;
            }

            if (b[i].numsides < 3) {
                FreeBrush(b[i]);
                b[i] = null;
            }
        }

        if (!(b[0] != null && b[1] != null)) {
            if (NOT(b[0]) && NOT(b[1])) {
                common.Printf("split removed brush\n");
            } else {
                common.Printf("split not on both sides\n");
            }
            if (b[0] != null) {
                FreeBrush(b[0]);
                front.oSet(CopyBrush(brush));
            }
            if (b[1] != null) {
                FreeBrush(b[1]);
                back.oSet(CopyBrush(brush));
            }
            return;
        }

        // add the midwinding to both sides
        for (i = 0; i < 2; i++) {
            cs = b[i].sides[b[i].numsides];
            b[i].numsides++;

            cs.planenum = planenum ^ i ^ 1;
            cs.material = null;
            if (i == 0) {
                cs.winding = midwinding.Copy();
            } else {
                cs.winding = midwinding;
            }
        }

        {
            float v1;
            int i2;

            for (i2 = 0; i2 < 2; i2++) {
                v1 = BrushVolume(b[i2]);
                if (v1 < 1.0) {
                    FreeBrush(b[i2]);
                    b[i2] = null;
                    //			common.Printf ("tiny volume after clip\n");
                }
            }
        }

        front.oSet(b[0]);
        back.oSet(b[1]);
    }
}
