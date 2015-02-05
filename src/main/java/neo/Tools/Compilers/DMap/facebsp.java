package neo.Tools.Compilers.DMap;

import static java.lang.Math.floor;
import static neo.Renderer.Material.CONTENTS_AREAPORTAL;
import static neo.TempDump.NOT;
import static neo.Tools.Compilers.DMap.dmap.PLANENUM_LEAF;
import neo.Tools.Compilers.DMap.dmap.bspface_s;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import neo.Tools.Compilers.DMap.dmap.node_s;
import neo.Tools.Compilers.DMap.dmap.primitive_s;
import neo.Tools.Compilers.DMap.dmap.side_s;
import neo.Tools.Compilers.DMap.dmap.tree_s;
import neo.Tools.Compilers.DMap.dmap.uBrush_t;
import neo.Tools.Compilers.DMap.dmap.uPortal_s;
import static neo.Tools.Compilers.DMap.map.FindFloatPlane;
import static neo.Tools.Compilers.DMap.portals.FreePortal;
import static neo.Tools.Compilers.DMap.portals.RemovePortalFromNode;
import static neo.Tools.Compilers.DMap.ubrush.AllocNode;
import static neo.Tools.Compilers.DMap.ubrush.AllocTree;
import static neo.Tools.Compilers.DMap.ubrush.CLIP_EPSILON;
import static neo.Tools.Compilers.DMap.ubrush.FreeBrushList;
import static neo.framework.Common.common;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Plane.PLANETYPE_TRUEAXIAL;
import static neo.idlib.math.Plane.SIDE_BACK;
import static neo.idlib.math.Plane.SIDE_CROSS;
import static neo.idlib.math.Plane.SIDE_FRONT;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import static neo.sys.win_shared.Sys_Milliseconds;

/**
 *
 */
public class facebsp {

    static int c_faceLeafs;
    //
    //
    public static int c_nodes;
    //
    static final int BLOCK_SIZE = 1024;

    //void RemovePortalFromNode( uPortal_s *portal, node_s *l );
    static node_s NodeForPoint(node_s node, idVec3 origin) {
        float d;

        while (node.planenum != PLANENUM_LEAF) {
            idPlane plane = dmapGlobals.mapPlanes.oGet(node.planenum);
            d = plane.Distance(origin);
            if (d >= 0) {
                node = node.children[0];
            } else {
                node = node.children[1];
            }
        }

        return node;
    }

    /*
     =============
     FreeTreePortals_r
     =============
     */
    static void FreeTreePortals_r(node_s node) {
        uPortal_s p, nextp;
        int s;

        // free children
        if (node.planenum != PLANENUM_LEAF) {
            FreeTreePortals_r(node.children[0]);
            FreeTreePortals_r(node.children[1]);
        }

        // free portals
        for (p = node.portals; p != null; p = nextp) {
            s = (p.nodes[1].equals(node)) ? 1 : 0;
            nextp = p.next[s];

            RemovePortalFromNode(p, p.nodes[/*!s*/1 ^ s]);
            FreePortal(p);
        }
        node.portals = null;
    }

    /*
     =============
     FreeTree_r
     =============
     */
    static void FreeTree_r(node_s node) {
        // free children
        if (node.planenum != PLANENUM_LEAF) {
            FreeTree_r(node.children[0]);
            FreeTree_r(node.children[1]);
        }

        // free brushes
        FreeBrushList(node.brushlist);

        // free the node
        c_nodes--;
        node.clear();//Mem_Free(node);
    }


    /*
     =============
     FreeTree
     =============
     */
    static void FreeTree(tree_s tree) {
        if (NOT(tree)) {
            return;
        }
        FreeTreePortals_r(tree.headnode);
        FreeTree_r(tree.headnode);
        tree.clear();//Mem_Free(tree);
    }

    //===============================================================
    static void PrintTree_r(node_s node, int depth) {
        int i;
        uBrush_t bb;

        for (i = 0; i < depth; i++) {
            common.Printf("  ");
        }
        if (node.planenum == PLANENUM_LEAF) {
            if (NOT(node.brushlist)) {
                common.Printf("NULL\n");
            } else {
                for (bb = node.brushlist; bb != null; bb = (uBrush_t) bb.next) {
                    common.Printf("%d ", bb.original.brushnum);
                }
                common.Printf("\n");
            }
            return;
        }

        idPlane plane = dmapGlobals.mapPlanes.oGet(node.planenum);
        common.Printf("#%d (%5.2f %5.2f %5.2f %5.2f)\n", node.planenum,
                plane.oGet(0), plane.oGet(1), plane.oGet(2), plane.oGet(3));
        PrintTree_r(node.children[0], depth + 1);
        PrintTree_r(node.children[1], depth + 1);
    }

    /*
     ================
     AllocBspFace
     ================
     */
    static bspface_s AllocBspFace() {
        bspface_s f;

        f = new bspface_s();// Mem_Alloc(sizeof(f));
        //	memset( f, 0, sizeof(*f) );

        return f;
    }

    /*
     ================
     FreeBspFace
     ================
     */
    static void FreeBspFace(bspface_s f) {
        if (f.w != null) {
            //		delete f.w;
            f.w = null;
        }
        f.clear();//Mem_Free(f);
    }


    /*
     ================
     SelectSplitPlaneNum
     ================
     */
    static int SelectSplitPlaneNum(node_s node, bspface_s list) {
        bspface_s split;
        bspface_s check;
        bspface_s bestSplit;
        int splits, facing, front, back;
        int side;
        idPlane mapPlane;
        int value, bestValue;
        idPlane plane = new idPlane();
        int planenum;
        boolean havePortals;
        float dist;
        idVec3 halfSize;

        // if it is crossing a 1k block boundary, force a split
        // this prevents epsilon problems from extending an
        // arbitrary distance across the map
        halfSize = (node.bounds.oGet(1).oMinus(node.bounds.oGet(0))).oMultiply(0.5f);
        for (int axis = 0; axis < 3; axis++) {
            if (halfSize.oGet(axis) > BLOCK_SIZE) {
                dist = (float) (BLOCK_SIZE * (floor((node.bounds.oGet(0, axis) + halfSize.oGet(axis)) / BLOCK_SIZE) + 1.0f));
            } else {
                dist = (float) (BLOCK_SIZE * (floor(node.bounds.oGet(0, axis) / BLOCK_SIZE) + 1.0f));
            }
            if (dist > node.bounds.oGet(0, axis) + 1.0f && dist < node.bounds.oGet(1, axis) - 1.0f) {
                plane.oSet(0, plane.oSet(1, plane.oSet(2, 0.0f)));
                plane.oSet(axis, 1.0f);
                plane.oSet(3, -dist);
                planenum = FindFloatPlane(plane);
                return planenum;
            }
        }

        // pick one of the face planes
        // if we have any portal faces at all, only
        // select from them, otherwise select from
        // all faces
        bestValue = -999999;
        bestSplit = list;

        havePortals = false;
        for (split = list; split != null; split = split.next) {
            split.checked = false;
            if (split.portal) {
                havePortals = true;
            }
        }

        for (split = list; split != null; split = split.next) {
            if (split.checked) {
                continue;
            }
            if (havePortals != split.portal) {
                continue;
            }
            mapPlane = dmapGlobals.mapPlanes.oGet(split.planenum);
            splits = 0;
            facing = 0;
            front = 0;
            back = 0;
            for (check = list; check != null; check = check.next) {
                if (check.planenum == split.planenum) {
                    facing++;
                    check.checked = true;	// won't need to test this plane again
                    continue;
                }
                side = check.w.PlaneSide(mapPlane);
                if (side == SIDE_CROSS) {
                    splits++;
                } else if (side == SIDE_FRONT) {
                    front++;
                } else if (side == SIDE_BACK) {
                    back++;
                }
            }
            value = 5 * facing - 5 * splits; // - abs(front-back);
            if (mapPlane.Type() < PLANETYPE_TRUEAXIAL) {
                value += 5;		// axial is better
            }

            if (value > bestValue) {
                bestValue = value;
                bestSplit = split;
            }
        }

        if (bestValue == -999999) {
            return -1;
        }

        return bestSplit.planenum;
    }

    /*
     ================
     BuildFaceTree_r
     ================
     */
    static void BuildFaceTree_r(node_s node, bspface_s list) {
        bspface_s split;
        bspface_s next;
        int side;
        bspface_s newFace;
        bspface_s[] childLists = new bspface_s[2];
        idWinding frontWinding = new idWinding(), backWinding = new idWinding();
        int i;
        int splitPlaneNum;

        splitPlaneNum = SelectSplitPlaneNum(node, list);
        // if we don't have any more faces, this is a node
        if (splitPlaneNum == -1) {
            node.planenum = PLANENUM_LEAF;
            c_faceLeafs++;
            return;
        }

        // partition the list
        node.planenum = splitPlaneNum;
        idPlane plane = dmapGlobals.mapPlanes.oGet(splitPlaneNum);
        childLists[0] = null;
        childLists[1] = null;
        for (split = list; split != null; split = next) {
            next = split.next;

            if (split.planenum == node.planenum) {
                FreeBspFace(split);
                continue;
            }

            side = split.w.PlaneSide(plane);

            if (side == SIDE_CROSS) {
                split.w.Split(plane, CLIP_EPSILON * 2, frontWinding, backWinding);
                if (!frontWinding.isNULL()) {
                    newFace = AllocBspFace();
                    newFace.w = frontWinding;
                    newFace.next = childLists[0];
                    newFace.planenum = split.planenum;
                    childLists[0] = newFace;
                }
                if (!backWinding.isNULL()) {
                    newFace = AllocBspFace();
                    newFace.w = backWinding;
                    newFace.next = childLists[1];
                    newFace.planenum = split.planenum;
                    childLists[1] = newFace;
                }
                FreeBspFace(split);
            } else if (side == SIDE_FRONT) {
                split.next = childLists[0];
                childLists[0] = split;
            } else if (side == SIDE_BACK) {
                split.next = childLists[1];
                childLists[1] = split;
            }
        }

        // recursively process children
        for (i = 0; i < 2; i++) {
            node.children[i] = AllocNode();
            node.children[i].parent = node;
            node.children[i].bounds = node.bounds;
        }

        // split the bounds if we have a nice axial plane
        for (i = 0; i < 3; i++) {
            if (idMath.Fabs(plane.oGet(i) - 1.0f) < 0.001) {
                node.children[0].bounds.oSet(0, i, plane.Dist());
                node.children[1].bounds.oSet(1, i, plane.Dist());
                break;
            }
        }

        for (i = 0; i < 2; i++) {
            BuildFaceTree_r(node.children[i], childLists[i]);
        }
    }


    /*
     ================
     FaceBSP

     List will be freed before returning
     ================
     */
    static tree_s FaceBSP(bspface_s list) {
        tree_s tree;
        bspface_s face;
        int i;
        int count;
        int start, end;

        start = Sys_Milliseconds();

        common.Printf("--- FaceBSP ---\n");

        tree = AllocTree();

        count = 0;
        tree.bounds.Clear();
        for (face = list; face != null; face = face.next) {
            count++;
            for (i = 0; i < face.w.GetNumPoints(); i++) {
                tree.bounds.AddPoint(face.w.oGet(i).ToVec3());
            }
        }
        common.Printf("%5i faces\n", count);

        tree.headnode = AllocNode();
        tree.headnode.bounds = tree.bounds;
        c_faceLeafs = 0;

        BuildFaceTree_r(tree.headnode, list);

        common.Printf("%5i leafs\n", c_faceLeafs);

        end = Sys_Milliseconds();

        common.Printf("%5.1f seconds faceBsp\n", (end - start) / 1000.0);

        return tree;
    }

    //==========================================================================

    /*
     =================
     MakeStructuralBspFaceList
     =================
     */
    static bspface_s MakeStructuralBspFaceList(primitive_s list) {
        uBrush_t b;
        int i;
        side_s s;
        idWinding w;
        bspface_s f, flist;

        flist = null;
        for (; list != null; list = list.next) {
            b = (uBrush_t) list.brush;
            if (NOT(b)) {
                continue;
            }
            if (!b.opaque && 0 == (b.contents & CONTENTS_AREAPORTAL)) {
                continue;
            }
            for (i = 0; i < b.numsides; i++) {
                s = b.sides[i];
                w = s.winding;
                if (NOT(w)) {
                    continue;
                }
                if (((b.contents & CONTENTS_AREAPORTAL) != 0) && (0 == (s.material.GetContentFlags() & CONTENTS_AREAPORTAL))) {
                    continue;
                }
                f = AllocBspFace();
                if ((s.material.GetContentFlags() & CONTENTS_AREAPORTAL) != 0) {
                    f.portal = true;
                }
                f.w = w.Copy();
                f.planenum = s.planenum & ~1;
                f.next = flist;
                flist = f;
            }
        }

        return flist;
    }

    /*
     =================
     MakeVisibleBspFaceList
     =================
     */
    static bspface_s MakeVisibleBspFaceList(primitive_s list) {
        uBrush_t b;
        int i;
        side_s s;
        idWinding w;
        bspface_s f, flist;

        flist = null;
        for (; list != null; list = list.next) {
            b = (uBrush_t) list.brush;
            if (NOT(b)) {
                continue;
            }
            if (!b.opaque && (0 == (b.contents & CONTENTS_AREAPORTAL))) {
                continue;
            }
            for (i = 0; i < b.numsides; i++) {
                s = b.sides[i];
                w = s.visibleHull;
                if (NOT(w)) {
                    continue;
                }
                f = AllocBspFace();
                if ((s.material.GetContentFlags() & CONTENTS_AREAPORTAL) != 0) {
                    f.portal = true;
                }
                f.w = w.Copy();
                f.planenum = s.planenum & ~1;
                f.next = flist;
                flist = f;
            }
        }

        return flist;
    }
}
