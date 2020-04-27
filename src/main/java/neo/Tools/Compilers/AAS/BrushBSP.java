package neo.Tools.Compilers.AAS;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static neo.TempDump.NOT;
import static neo.TempDump.SNOT;
import static neo.Tools.Compilers.AAS.Brush.BFL_NO_VALID_SPLITTERS;
import static neo.Tools.Compilers.AAS.Brush.BRUSH_PLANESIDE_BACK;
import static neo.Tools.Compilers.AAS.Brush.BRUSH_PLANESIDE_BOTH;
import static neo.Tools.Compilers.AAS.Brush.BRUSH_PLANESIDE_FACING;
import static neo.Tools.Compilers.AAS.Brush.BRUSH_PLANESIDE_FRONT;
import static neo.Tools.Compilers.AAS.Brush.DisplayRealTimeString;
import static neo.Tools.Compilers.AAS.Brush.SFL_SPLIT;
import static neo.Tools.Compilers.AAS.Brush.SFL_USED_SPLITTER;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Lib.MAX_WORLD_COORD;
import static neo.idlib.Lib.MIN_WORLD_COORD;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_CROSS;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;
import static neo.idlib.math.Plane.PLANETYPE_TRUEAXIAL;
import static neo.idlib.math.Plane.SIDE_BACK;
import static neo.idlib.math.Plane.SIDE_CROSS;
import static neo.idlib.math.Plane.SIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_ON;
import static neo.idlib.math.Vector.getVec3_origin;

import java.util.Arrays;

import neo.Tools.Compilers.AAS.AASBuild.Allowance;
import neo.Tools.Compilers.AAS.Brush.idBrush;
import neo.Tools.Compilers.AAS.Brush.idBrushList;
import neo.Tools.Compilers.AAS.Brush.idBrushMap;
import neo.Tools.Compilers.AAS.Brush.idBrushSide;
import neo.framework.File_h.idFile;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.PlaneSet.idPlaneSet;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.containers.VectorSet.idVectorSet;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class BrushBSP {

    static final float   BSP_GRID_SIZE                 = 512.0f;
    static final float   SPLITTER_EPSILON              = 0.1f;
    static final float   VERTEX_MELT_EPSILON           = 0.1f;
    static final float   VERTEX_MELT_HASH_SIZE         = 32;
    //                                              
    static final float   PORTAL_PLANE_NORMAL_EPSILON   = 0.00001f;
    static final float   PORTAL_PLANE_DIST_EPSILON     = 0.01f;
    //
    static final boolean OUPUT_BSP_STATS_PER_GRID_CELL = false;
    //
    static final int     NODE_VISITED                  = BIT(30);
    static final int     NODE_DONE                     = BIT(31);
    //
    static final float   BASE_WINDING_EPSILON          = 0.001f;
    //
    static final float   SPLIT_WINDING_EPSILON         = 0.001f;

    //===============================================================
    //
    //	idBrushBSPPortal
    //
    //===============================================================
    static class idBrushBSPPortal {

        private idPlane   plane;                                    // portal plane
        private int       planeNum;                                 // number of plane this portal is on
        private idWinding winding;                                  // portal winding
        private idBrushBSPNode[]   nodes = new idBrushBSPNode[2];   // nodes this portal seperates
        private idBrushBSPPortal[] next  = new idBrushBSPPortal[2]; // next portal in list for both nodes
        private int flags;                                          // portal flags
        private int faceNum;                                        // number of the face created for this portal
        //
        //
        // friend class idBrushBSP;
        // friend class idBrushBSPNode;

        public idBrushBSPPortal() {
            planeNum = -1;
            winding = null;
            nodes[0] = nodes[1] = null;
            next[0] = next[1] = null;
            faceNum = 0;
            flags = 0;
        }

        // ~idBrushBSPPortal();
        public void AddToNodes(idBrushBSPNode front, idBrushBSPNode back) {
            if (nodes[0] != null || nodes[1] != null) {
                common.Error("AddToNode: allready included");
            }

            assert (front != null && back != null);

            nodes[0] = front;
            next[0] = front.portals;
            front.portals = this;

            nodes[1] = back;
            next[1] = back.portals;
            back.portals = this;
        }

        public void RemoveFromNode(idBrushBSPNode l) {
            idBrushBSPPortal pp, t;

            // remove reference to the current portal
            pp = l.portals;
            while (true) {
                t = pp;
                if (NOT(t)) {
                    common.Error("idBrushBSPPortal::RemoveFromNode: portal not in node");
                }

                if (t == this) {
                    break;
                }

                if (t.nodes[0] == l) {
                    pp.oSet(t.next[0]);
                } else if (t.nodes[1] == l) {
                    pp.oSet(t.next[1]);
                } else {
                    common.Error("idBrushBSPPortal::RemoveFromNode: portal not bounding node");
                }
            }

            if (nodes[0] == l) {
                pp.oSet(next[0]);
                nodes[0] = null;
            } else if (nodes[1] == l) {
                pp.oSet(next[1]);
                nodes[1] = null;
            } else {
                common.Error("idBrushBSPPortal::RemoveFromNode: mislinked portal");
            }
        }

        public void Flip() {
            idBrushBSPNode frontNode, backNode;

            frontNode = nodes[0];
            backNode = nodes[1];

            if (frontNode != null) {
                RemoveFromNode(frontNode);
            }
            if (backNode != null) {
                RemoveFromNode(backNode);
            }
            AddToNodes(frontNode, backNode);

            plane = plane.oNegative();
            planeNum ^= 1;
            winding.ReverseSelf();
        }

        public int Split(final idPlane splitPlane, idBrushBSPPortal front, idBrushBSPPortal back) {
            idWinding frontWinding = new idWinding(), backWinding = new idWinding();

//            front[0] = back[0] = null;
            winding.Split(splitPlane, 0.1f, frontWinding, backWinding);
            if (!frontWinding.isNULL()) {
                front.oSet(new idBrushBSPPortal());
                front.plane = plane;
                front.planeNum = planeNum;
                front.flags = flags;
                front.winding = frontWinding;
            }
            if (!backWinding.isNULL()) {
                back.oSet(new idBrushBSPPortal());
                back.plane = plane;
                back.planeNum = planeNum;
                back.flags = flags;
                back.winding = backWinding;
            }

            if (!frontWinding.isNULL() && !backWinding.isNULL()) {
                return PLANESIDE_CROSS;
            } else if (!frontWinding.isNULL()) {
                return PLANESIDE_FRONT;
            } else {
                return PLANESIDE_BACK;
            }
        }

        public idWinding GetWinding() {
            return winding;
        }

        public idPlane GetPlane() {
            return plane;
        }

        public void SetFaceNum(int num) {
            faceNum = num;
        }

        public int GetFaceNum() {
            return faceNum;
        }

        public int GetFlags() {
            return flags;
        }

        public void SetFlag(int flag) {
            flags |= flag;
        }

        public void RemoveFlag(int flag) {
            flags &= ~flag;
        }

        public idBrushBSPPortal Next(int side) {
            return next[side];
        }

        public idBrushBSPNode GetNode(int side) {
            return nodes[side];
        }

        private void oSet(idBrushBSPPortal idBrushBSPPortal) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    //===============================================================
    //
    //	idBrushBSPNode
    //
    //===============================================================
    static class idBrushBSPNode {

        // friend class idBrushBSP;
        // friend class idBrushBSPPortal;
        private idPlane        plane;                               // split plane if this is not a leaf node
        private idBrush        volume;                              // node volume
        private int            contents;                            // node contents
        private idBrushList    brushList;                           // list with brushes for this node
        private idBrushBSPNode parent;                              // parent of this node
        private idBrushBSPNode[] children = new idBrushBSPNode[2];  // both are NULL if this is a leaf node
        private idBrushBSPPortal portals;                           // portals of this node
        private int              flags;                             // node flags
        private int              areaNum;                           // number of the area created for this node
        private int              occupied;                          // true when portal is occupied
        //
        //

        public idBrushBSPNode() {
            brushList.Clear();
            contents = 0;
            flags = 0;
            volume = null;
            portals = null;
            children[0] = children[1] = null;
            areaNum = 0;
            occupied = 0;
        }
        // ~idBrushBSPNode();

        public void SetContentsFromBrushes() {
            idBrush brush;

            contents = 0;
            for (brush = brushList.Head(); brush != null; brush = brush.Next()) {
                contents |= brush.GetContents();
            }
        }

        public idBounds GetPortalBounds() {
            int s, i;
            idBrushBSPPortal p;
            idBounds bounds = new idBounds();

            bounds.Clear();
            for (p = portals; p != null; p = p.next[s]) {
                s = (p.nodes[1].equals(this)) ? 1 : 0;

                for (i = 0; i < p.winding.GetNumPoints(); i++) {
                    bounds.AddPoint(p.winding.oGet(i).ToVec3());
                }
            }
            return bounds;
        }

        public idBrushBSPNode GetChild(int index) {
            return children[index];
        }

        public idBrushBSPNode GetParent() {
            return parent;
        }

        public void SetContents(int contents) {
            this.contents = contents;
        }

        public int GetContents() {
            return contents;
        }

        public idPlane GetPlane() {
            return plane;
        }

        public idBrushBSPPortal GetPortals() {
            return portals;
        }

        public void SetAreaNum(int num) {
            areaNum = num;
        }

        public int GetAreaNum() {
            return areaNum;
        }

        public int GetFlags() {
            return flags;
        }

        public void SetFlag(int flag) {
            flags |= flag;
        }

        public void RemoveFlag(int flag) {
            flags &= ~flag;
        }

        public boolean TestLeafNode() {
            int s, n;
            float d;
            idBrushBSPPortal p;
            idVec3 center;
            idPlane plane;

            n = 0;
            center = getVec3_origin();
            for (p = portals; p != null; p = p.next[s]) {
                s = (p.nodes[1] == this) ? 1 : 0;
                center.oPluSet(p.winding.GetCenter());
                n++;
            }

            center.oDivSet(n);

            for (p = portals; p != null; p = p.next[s]) {
                s = (p.nodes[1].equals(this)) ? 1 : 0;
                if (s != 0) {
                    plane = p.GetPlane().oNegative();
                } else {
                    plane = p.GetPlane();
                }
                d = plane.Distance(center);
                if (d < 0.0f) {
                    return false;
                }
            }
            return true;
        }

        // remove the flag from nodes found by flooding through portals to nodes with the flag set
        public void RemoveFlagFlood(int flag) {
            int s;
            idBrushBSPPortal p;

            RemoveFlag(flag);

            for (p = GetPortals(); p != null; p = p.Next(s)) {
                s = (p.GetNode(1) == this) ? 1 : 0;

                if (0 == (p.GetNode( /*!s*/SNOT(s)).GetFlags() & flag)) {
                    continue;
                }

                p.GetNode( /*!s*/SNOT(s)).RemoveFlagFlood(flag);
            }
        }

        // recurse down the tree and remove the flag from all visited nodes
        public void RemoveFlagRecurse(int flag) {
            RemoveFlag(flag);
            if (children[0] != null) {
                children[0].RemoveFlagRecurse(flag);
            }
            if (children[1] != null) {
                children[1].RemoveFlagRecurse(flag);
            }
        }

        // first recurse down the tree and flood from there
        public void RemoveFlagRecurseFlood(int flag) {
            RemoveFlag(flag);
            if (NOT(children[0]) && NOT(children[1])) {
                RemoveFlagFlood(flag);
            } else {
                if (children[0] != null) {
                    children[0].RemoveFlagRecurseFlood(flag);
                }
                if (children[1] != null) {
                    children[1].RemoveFlagRecurseFlood(flag);
                }
            }
        }

        // returns side of the plane the node is on
        public int PlaneSide(final idPlane plane, float epsilon /*= ON_EPSILON*/) {
            int s, side;
            idBrushBSPPortal p;
            boolean front, back;

            front = back = false;
            for (p = portals; p != null; p = p.next[s]) {
                s = (p.nodes[1].equals(this)) ? 1 : 0;

                side = p.winding.PlaneSide(plane, epsilon);
                if (side == SIDE_CROSS || side == SIDE_ON) {
                    return side;
                }
                if (side == SIDE_FRONT) {
                    if (back) {
                        return SIDE_CROSS;
                    }
                    front = true;
                }
                if (side == SIDE_BACK) {
                    if (front) {
                        return SIDE_CROSS;
                    }
                    back = true;
                }
            }

            if (front) {
                return SIDE_FRONT;
            }
            return SIDE_BACK;
        }

        // split the leaf node with a plane
        public boolean Split(final idPlane splitPlane, int splitPlaneNum) {
            int s, i;
            idWinding mid;
            idBrushBSPPortal p, midPortal;
            idBrushBSPPortal[] newPortals = new idBrushBSPPortal[2];
            idBrushBSPNode[] newNodes = new idBrushBSPNode[2];

            mid = new idWinding(splitPlane.Normal(), splitPlane.Dist());

            for (p = portals; p != null && mid != null; p = p.next[s]) {
                s = (p.nodes[1].equals(this)) ? 1 : 0;
                if (s != 0) {
                    mid = mid.Clip(p.plane.oNegative(), 0.1f, false);
                } else {
                    mid = mid.Clip(p.plane, 0.1f, false);
                }
            }

            if (NOT(mid)) {
                return false;
            }

            // allocate two new nodes
            for (i = 0; i < 2; i++) {
                newNodes[i] = new idBrushBSPNode();
                newNodes[i].flags = flags;
                newNodes[i].contents = contents;
                newNodes[i].parent = this;
            }

            // split all portals of the node
            for (p = portals; p != null; p = portals) {
                s = (p.nodes[1].equals(this)) ? 1 : 0;
                p.Split(splitPlane, newPortals[0], newPortals[1]);
                for (i = 0; i < 2; i++) {
                    if (newPortals[i] != null) {
                        if (s != 0) {
                            newPortals[i].AddToNodes(p.nodes[0], newNodes[i]);
                        } else {
                            newPortals[i].AddToNodes(newNodes[i], p.nodes[1]);
                        }
                    }
                }
                p.RemoveFromNode(p.nodes[0]);
                p.RemoveFromNode(p.nodes[1]);
//		delete p;
            }

            // add seperating portal
            midPortal = new idBrushBSPPortal();
            midPortal.plane = splitPlane;
            midPortal.planeNum = splitPlaneNum;
            midPortal.winding = mid;
            midPortal.AddToNodes(newNodes[0], newNodes[1]);

            // set new child nodes
            children[0] = newNodes[0];
            children[1] = newNodes[1];
            plane = splitPlane;

            return true;
        }
    }

    ;

    //===============================================================
    //
    //	idBrushBSP
    //
    //===============================================================
    static class idBrushBSP {

        private idBrushBSPNode root;
        private idBrushBSPNode outside;
        private idBounds       treeBounds;
        private idPlaneSet     portalPlanes;
        private int            numGridCells;
        private int            numSplits;
        private int            numGridCellSplits;
        private int            numPrunedSplits;
        private int            numPortals;
        private int            solidLeafNodes;
        private int            outsideLeafNodes;
        private int            insideLeafNodes;
        private int            numMergedPortals;
        private int            numInsertedPoints;
        private idVec3         leakOrigin;
        private int            brushMapContents;
        private idBrushMap     brushMap;
        //
        private Allowance      BrushChopAllowed;
        private Allowance      BrushMergeAllowed;
        //
        //

        /*
         ============
         idBrushBSP::BrushSplitterStats
         ============
         */
        private class splitterStats_s {

            int numFront;	// number of brushes at the front of the splitter
            int numBack;	// number of brushes at the back of the splitter
            int numSplits;	// number of brush sides split by the splitter
            int numFacing;	// number of brushes facing this splitter
            int epsilonBrushes;	// number of tiny brushes this splitter would create
        };

        public idBrushBSP() {
            root = outside = null;
            numSplits = numPrunedSplits = 0;
            brushMapContents = 0;
            brushMap = null;
        }
        // ~idBrushBSP();

        // build a bsp tree from a set of brushes
        public void Build(idBrushList brushList, int skipContents,
                Allowance ChopAllowed/*boolean (*ChopAllowed)( idBrush *b1, idBrush *b2 )*/,
                Allowance MergeAllowed) /*boolean (*MergeAllowed)( idBrush *b1, idBrush *b2 ) )*/ {

            int i;
            idList<idBrushBSPNode> gridCells = new idList<>();

            common.Printf("[Brush BSP]\n");
            common.Printf("%6d brushes\n", brushList.Num());

            BrushChopAllowed = ChopAllowed;
            BrushMergeAllowed = MergeAllowed;

            numGridCells = 0;
            treeBounds = brushList.GetBounds();
            root = new idBrushBSPNode();
            root.brushList = brushList;
            root.volume = new idBrush();
            root.volume.FromBounds(treeBounds);
            root.parent = null;

            BuildGrid_r(gridCells, root);

            common.Printf("\r%6d grid cells\n", gridCells.Num());

            if (OUPUT_BSP_STATS_PER_GRID_CELL) {
                for (i = 0; i < gridCells.Num(); i++) {
                    ProcessGridCell(gridCells.oGet(i), skipContents);
                }
            } else {
                common.Printf("\r%6d %%", 0);
                for (i = 0; i < gridCells.Num(); i++) {
                    DisplayRealTimeString("\r%6d", i * 100 / gridCells.Num());
                    ProcessGridCell(gridCells.oGet(i), skipContents);
                }
                common.Printf("\r%6d %%\n", 100);
            }

            common.Printf("\r%6d splits\n", numSplits);

            if (brushMap != null) {
//		delete brushMap;
                brushMap = null;
            }
        }

        // remove splits in subspaces with the given contents
        public void PruneTree(int contents) {
            numPrunedSplits = 0;
            common.Printf("[Prune BSP]\n");
            PruneTree_r(root, contents);
            common.Printf("%6d splits pruned\n", numPrunedSplits);
        }

        // portalize the bsp tree
        public void Portalize() {
            common.Printf("[Portalize BSP]\n");
            common.Printf("%6d nodes\n", (numSplits - numPrunedSplits) * 2 + 1);
            numPortals = 0;
            MakeOutsidePortals();
            MakeTreePortals_r(root);
            common.Printf("\r%6d nodes portalized\n", numPortals);
        }

        // remove subspaces outside the map not reachable by entities
        public boolean RemoveOutside(final idMapFile mapFile, int contents, final idStrList classNames) {
            common.Printf("[Remove Outside]\n");

            solidLeafNodes = outsideLeafNodes = insideLeafNodes = 0;

            if (!FloodFromEntities(mapFile, contents, classNames)) {
                return false;
            }

            RemoveOutside_r(root, contents);

            common.Printf("%6d solid leaf nodes\n", solidLeafNodes);
            common.Printf("%6d outside leaf nodes\n", outsideLeafNodes);
            common.Printf("%6d inside leaf nodes\n", insideLeafNodes);

            //PruneTree( contents );
            return true;
        }

        /*
         =============
         LeakFile

         Finds the shortest possible chain of portals that
         leads from the outside leaf to a specific occupied leaf.
         // write file with a trace going through a leak
         =============
         */
        public void LeakFile(final idStr fileName) {
            int count, next, s;
            idVec3 mid;
            idFile lineFile;
            idBrushBSPNode node, nextNode = new idBrushBSPNode();
            idBrushBSPPortal p, nextPortal = new idBrushBSPPortal();
            idStr qpath, name;

            if (0 == outside.occupied) {
                return;
            }

            qpath = fileName;
            qpath.SetFileExtension("lin");

            common.Printf("writing %s...\n", qpath);

            lineFile = fileSystem.OpenFileWrite(qpath.toString(), "fs_devpath");
            if (null == lineFile) {
                common.Error("Couldn't open %s\n", qpath);
                return;
            }

            count = 0;
            node = outside;
            while (node.occupied > 1) {

                // find the best portal exit
                next = node.occupied;
                for (p = node.portals; p != null; p = p.next[/*!s*/SNOT(s)]) {
                    s = (p.nodes[0].equals(node)) ? 1 : 0;
                    if (p.nodes[s].occupied != 0 && p.nodes[s].occupied < next) {
                        nextPortal = p;
                        nextNode = p.nodes[s];
                        next = nextNode.occupied;
                    }
                }
                node = nextNode;
                mid = nextPortal.winding.GetCenter();
                lineFile.Printf("%f %f %f\n", mid.oGet(0), mid.oGet(1), mid.oGet(2));
                count++;
            }

            // add the origin of the entity from which the leak was found
            lineFile.Printf("%f %f %f\n", leakOrigin.oGet(0), leakOrigin.oGet(1), leakOrigin.oGet(2));

            fileSystem.CloseFile(lineFile);
        }

        // try to merge portals
        public void MergePortals(int skipContents) {
            numMergedPortals = 0;
            common.Printf("[Merge Portals]\n");
            SetPortalPlanes();
            MergePortals_r(root, skipContents);
            common.Printf("%6d portals merged\n", numMergedPortals);
        }

        /*
         ============
         idBrushBSP::TryMergeLeafNodes

         NOTE: multiple brances of the BSP tree might point to the same leaf node after merging
         // try to merge the two leaf nodes at either side of the portal
         ============
         */
        public boolean TryMergeLeafNodes(idBrushBSPPortal portal, int side) {
            int i, j, k, s1, s2, s;
            idBrushBSPNode node1, node2;
            idBrushBSPNode[] nodes = new idBrushBSPNode[2];
            idBrushBSPPortal p1, p2, p, nextp;
            idPlane plane;
            idWinding w;
            idBounds bounds = new idBounds(), b = new idBounds();

            nodes[0] = node1 = portal.nodes[side];
            nodes[1] = node2 = portal.nodes[/*!side*/ SNOT(side)];

            // check if the merged node would still be convex
            for (i = 0; i < 2; i++) {

                j = /*!i*/ 1 ^ i;

                for (p1 = nodes[i].portals; p1 != null; p1 = p1.next[s1]) {
                    s1 = (p1.nodes[1].equals(nodes[i])) ? 1 : 0;

                    if (p1.nodes[/*!s1*/SNOT(s1)].equals(nodes[j])) {
                        continue;
                    }

                    if (s1 != 0) {
                        plane = p1.plane.oNegative();
                    } else {
                        plane = p1.plane;
                    }

                    // all the non seperating portals of the other node should be at the front or on the plane
                    for (p2 = nodes[j].portals; p2 != null; p2 = p2.next[s2]) {
                        s2 = (p2.nodes[1].equals(nodes[j])) ? 1 : 0;

                        if (p2.nodes[/*!s2*/SNOT(s2)].equals(nodes[i])) {
                            continue;
                        }

                        w = p2.winding;
                        for (k = 0; k < w.GetNumPoints(); k++) {
                            if (plane.Distance(w.oGet(k).ToVec3()) < -0.1f) {
                                return false;
                            }
                        }
                    }
                }
            }

            // remove all portals that seperate the two nodes
            for (p = node1.portals; p != null; p = nextp) {
                s = (p.nodes[1].equals(node1)) ? 1 : 0;
                nextp = p.next[s];

                if (p.nodes[/*!s*/SNOT(s)].equals(node2)) {
                    p.RemoveFromNode(p.nodes[0]);
                    p.RemoveFromNode(p.nodes[1]);
//			delete p;
                }
            }

            // move all portals of node2 to node1
            for (p = node2.portals; p != null; p = node2.portals) {
                s = (p.nodes[1].equals(node2)) ? 1 : 0;

                nodes[s] = node1;
                nodes[/*!s*/SNOT(s)] = p.nodes[/*!s*/SNOT(s)];
                p.RemoveFromNode(p.nodes[0]);
                p.RemoveFromNode(p.nodes[1]);
                p.AddToNodes(nodes[0], nodes[1]);
            }

            // get bounds for the new node
            bounds.Clear();
            for (p = node1.portals; p != null; p = p.next[s]) {
                s = (p.nodes[1].equals(node1)) ? 1 : 0;
                p.GetWinding().GetBounds(b);
                bounds.oPluSet(b);
            }

            // replace every reference to node2 by a reference to node1
            UpdateTreeAfterMerge_r(root, bounds, node2, node1);

//	delete node2;
            return true;
        }

        public void PruneMergedTree_r(idBrushBSPNode node) {
            int i;
            idBrushBSPNode leafNode;

            if (NOT(node)) {
                return;
            }

            PruneMergedTree_r(node.children[0]);
            PruneMergedTree_r(node.children[1]);

            for (i = 0; i < 2; i++) {
                if (node.children[i] != null) {
                    leafNode = node.children[i].children[0];
                    if (leafNode != null && leafNode.equals(node.children[i].children[1])) {
                        if (leafNode.parent.equals(node.children[i])) {
                            leafNode.parent = node;
                        }
//				delete node.children[i];
                        node.children[i] = leafNode;
                    }
                }
            }
        }

        // melt portal windings
        public void MeltPortals(int skipContents) {
            idVectorSet<idVec3> vertexList = new idVectorSet(3);

            numInsertedPoints = 0;
            common.Printf("[Melt Portals]\n");
            RemoveColinearPoints_r(root, skipContents);
            MeltPortals_r(root, skipContents, vertexList);
            root.RemoveFlagRecurse(NODE_DONE);
            common.Printf("\r%6d points inserted\n", numInsertedPoints);
        }

        // write a map file with a brush for every leaf node that has the given contents
        public void WriteBrushMap(final idStr fileName, final idStr ext, int contents) {
            brushMap = new idBrushMap(fileName, ext);
            brushMapContents = contents;
        }

        // bounds for the whole tree
        public idBounds GetTreeBounds() {
            return treeBounds;
        }

        // root node of the tree
        public idBrushBSPNode GetRootNode() {
            return root;
        }

        private void RemoveMultipleLeafNodeReferences_r(idBrushBSPNode node) {
            if (NOT(node)) {
                return;
            }

            if (node.children[0] != null) {
                if (!node.children[0].parent.equals(node)) {
                    node.children[0] = null;
                } else {
                    RemoveMultipleLeafNodeReferences_r(node.children[0]);
                }
            }
            if (node.children[1] != null) {
                if (node.children[1].parent != node) {
                    node.children[1] = null;
                } else {
                    RemoveMultipleLeafNodeReferences_r(node.children[1]);
                }
            }
        }

        private void Free_r(idBrushBSPNode node) {
            if (NOT(node)) {
                return;
            }

            Free_r(node.children[0]);
            Free_r(node.children[1]);

//	delete node;
        }
//
//        private void IncreaseNumSplits();
//

        private boolean IsValidSplitter(final idBrushSide side) {
            return NOT(side.GetFlags() & (SFL_SPLIT | SFL_USED_SPLITTER));
        }

        private int BrushSplitterStats(final idBrush brush, int planeNum, final idPlaneSet planeList, boolean[] testedPlanes, splitterStats_s stats) {
            int i, j, num, s, lastNumSplits;
            idPlane plane;
            idWinding w;
            float d, d_front, d_back, brush_front, brush_back;

            plane = planeList.oGet(planeNum);

            // get the plane side for the brush bounds
            s = brush.GetBounds().PlaneSide(plane, SPLITTER_EPSILON);
            if (s == PLANESIDE_FRONT) {
                stats.numFront++;
                return BRUSH_PLANESIDE_FRONT;
            }
            if (s == PLANESIDE_BACK) {
                stats.numBack++;
                return BRUSH_PLANESIDE_BACK;
            }

            // if the brush actually uses the planenum, we can tell the side for sure
            for (i = 0; i < brush.GetNumSides(); i++) {
                num = brush.GetSide(i).GetPlaneNum();

                if (0 == ((num ^ planeNum) >> 1)) {
                    if (num == planeNum) {
                        stats.numBack++;
                        stats.numFacing++;
                        return (BRUSH_PLANESIDE_BACK | BRUSH_PLANESIDE_FACING);
                    }
                    if (num == (planeNum ^ 1)) {
                        stats.numFront++;
                        stats.numFacing++;
                        return (BRUSH_PLANESIDE_FRONT | BRUSH_PLANESIDE_FACING);
                    }
                }
            }

            lastNumSplits = stats.numSplits;
            brush_front = brush_back = 0.0f;
            for (i = 0; i < brush.GetNumSides(); i++) {

                if (!IsValidSplitter(brush.GetSide(i))) {
                    continue;
                }

                j = brush.GetSide(i).GetPlaneNum();
                if (testedPlanes[j] || testedPlanes[j ^ 1]) {
                    continue;
                }

                w = brush.GetSide(i).GetWinding();
                if (NOT(w)) {
                    continue;
                }
                d_front = d_back = 0.0f;
                for (j = 0; j < w.GetNumPoints(); j++) {
                    d = plane.Distance(w.oGet(j).ToVec3());
                    if (d > d_front) {
                        d_front = d;
                    } else if (d < d_back) {
                        d_back = d;
                    }
                }
                if (d_front > SPLITTER_EPSILON && d_back < -SPLITTER_EPSILON) {
                    stats.numSplits++;
                }
                if (d_front > brush_front) {
                    brush_front = d_front;
                } else if (d_back < brush_back) {
                    brush_back = d_back;
                }
            }

            // if brush sides are split and the brush only pokes one unit through the plane
            if (stats.numSplits > lastNumSplits && (brush_front < 1.0f || brush_back > -1.0f)) {
                stats.epsilonBrushes++;
            }

            return BRUSH_PLANESIDE_BOTH;
        }

        private int FindSplitter(idBrushBSPNode node, final idPlaneSet planeList, boolean[] testedPlanes, splitterStats_s[] bestStats) {
            int i, planeNum, bestSplitter, value, bestValue, f, numBrushSides;
            idBrush brush, b;
            splitterStats_s stats;

            Arrays.fill(testedPlanes, false);//	memset( testedPlanes, 0, planeList.Num() * sizeof( bool ) );

            bestSplitter = -1;
            bestValue = -99999999;
            for (brush = node.brushList.Head(); brush != null; brush = brush.Next()) {

                if ((brush.GetFlags() & BFL_NO_VALID_SPLITTERS) != 0) {
                    continue;
                }

                for (i = 0; i < brush.GetNumSides(); i++) {

                    if (!IsValidSplitter(brush.GetSide(i))) {
                        continue;
                    }

                    planeNum = brush.GetSide(i).GetPlaneNum();

                    if (testedPlanes[planeNum] || testedPlanes[planeNum ^ 1]) {
                        continue;
                    }

                    testedPlanes[planeNum] = testedPlanes[planeNum ^ 1] = true;

                    if (node.volume.Split(planeList.oGet(planeNum), planeNum, null, null) != PLANESIDE_CROSS) {
                        continue;
                    }

                    stats = new splitterStats_s();//memset( &stats, 0, sizeof( stats ) );

                    f = (brush.GetSide(i).GetPlane().Type() < PLANETYPE_TRUEAXIAL) ? 15 + 5 : 0;
                    numBrushSides = node.brushList.NumSides();

                    for (b = node.brushList.Head(); b != null; b = b.Next()) {

                        // if the brush has no valid splitters left
                        if ((b.GetFlags() & BFL_NO_VALID_SPLITTERS) != 0) {
                            b.SetPlaneSide(BRUSH_PLANESIDE_BOTH);
                        } else {
                            b.SetPlaneSide(BrushSplitterStats(b, planeNum, planeList, testedPlanes, stats));
                        }

                        numBrushSides -= b.GetNumSides();
                        // best value we can get using this plane as a splitter
                        value = f * (stats.numFacing + numBrushSides) - 10 * stats.numSplits - stats.epsilonBrushes * 1000;
                        // if the best value for this plane can't get any better than the best value we have
                        if (value < bestValue) {
                            break;
                        }
                    }

                    if (b != null) {
                        continue;
                    }

                    value = f * stats.numFacing - 10 * stats.numSplits - abs(stats.numFront - stats.numBack) - stats.epsilonBrushes * 1000;

                    if (value > bestValue) {
                        bestValue = value;
                        bestSplitter = planeNum;
                        bestStats[0]= stats;

                        for (b = node.brushList.Head(); b != null; b = b.Next()) {
                            b.SavePlaneSide();
                        }
                    }
                }
            }

            return bestSplitter;
        }

        private void SetSplitterUsed(idBrushBSPNode node, int planeNum) {
            int i, numValidBrushSplitters;
            idBrush brush;

            for (brush = node.brushList.Head(); brush != null; brush = brush.Next()) {
                if (0 == (brush.GetSavedPlaneSide() & BRUSH_PLANESIDE_FACING)) {
                    continue;
                }
                numValidBrushSplitters = 0;
                for (i = 0; i < brush.GetNumSides(); i++) {

                    if (0 == ((brush.GetSide(i).GetPlaneNum() ^ planeNum) >> 1)) {
                        brush.GetSide(i).SetFlag(SFL_USED_SPLITTER);
                    } else if (IsValidSplitter(brush.GetSide(i))) {
                        numValidBrushSplitters++;
                    }
                }
                if (numValidBrushSplitters == 0) {
                    brush.SetFlag(BFL_NO_VALID_SPLITTERS);
                }
            }
        }

        private idBrushBSPNode BuildBrushBSP_r(idBrushBSPNode node, final idPlaneSet planeList, boolean[] testedPlanes, int skipContents) {
            int planeNum;
            splitterStats_s[] bestStats = {null};

            planeNum = FindSplitter(node, planeList, testedPlanes, bestStats);

            // if no split plane found this is a leaf node
            if (planeNum == -1) {

                node.SetContentsFromBrushes();

                if (brushMap != null && (node.contents & brushMapContents) != 0) {
                    brushMap.WriteBrush(node.volume);
                }

                // free node memory
                node.brushList.Free();
                node.volume = null;//delete node.volume;

                node.children[0] = node.children[1] = null;
                return node;
            }

            numSplits++;
            numGridCellSplits++;

            // mark all brush sides on the split plane as used
            SetSplitterUsed(node, planeNum);

            // set node split plane
            node.plane = planeList.oGet(planeNum);

            // allocate children
            node.children[0] = new idBrushBSPNode();
            node.children[1] = new idBrushBSPNode();

            // split node volume and brush list for children
            node.volume.Split(node.plane, -1, node.children[0].volume, node.children[1].volume);
            node.brushList.Split(node.plane, -1, node.children[0].brushList, node.children[1].brushList, true);
            node.children[0].parent = node.children[1].parent = node;

            // free node memory
            node.brushList.Free();
            node.volume = null;//delete node.volume;

            // process children
            node.children[0] = BuildBrushBSP_r(node.children[0], planeList, testedPlanes, skipContents);
            node.children[1] = BuildBrushBSP_r(node.children[1], planeList, testedPlanes, skipContents);

            // if both children contain the skip contents
            if ((node.children[0].contents & node.children[1].contents & skipContents) != 0) {
                node.contents = node.children[0].contents | node.children[1].contents;
                node.children[0] = node.children[1] = null;//delete node.children[0];delete node.children[1];
                numSplits--;
                numGridCellSplits--;
            }

            return node;
        }

        private idBrushBSPNode ProcessGridCell(idBrushBSPNode node, int skipContents) {
            idPlaneSet planeList = new idPlaneSet();
            boolean[] testedPlanes;

            if (OUPUT_BSP_STATS_PER_GRID_CELL) {
                common.Printf("[Grid Cell %d]\n", ++numGridCells);
                common.Printf("%6d brushes\n", node.brushList.Num());
            }

            numGridCellSplits = 0;

            // chop away all brush overlap
            node.brushList.Chop(BrushChopAllowed);

            // merge brushes if possible
            //node->brushList.Merge( BrushMergeAllowed );
            // create a list with planes for this grid cell
            node.brushList.CreatePlaneList(planeList);

            if (OUPUT_BSP_STATS_PER_GRID_CELL) {
                common.Printf("[Grid Cell BSP]\n");
            }

            testedPlanes = new boolean[planeList.Num()];

            BuildBrushBSP_r(node, planeList, testedPlanes, skipContents);

//            testedPlanes = null;//delete testedPlanes;
            if (OUPUT_BSP_STATS_PER_GRID_CELL) {
                common.Printf("\r%6d splits\n", numGridCellSplits);
            }

            return node;
        }

        private void BuildGrid_r(idList<idBrushBSPNode> gridCells, idBrushBSPNode node) {
            int axis;
            float dist = 0;
            idBounds bounds;
            idVec3 normal, halfSize;

            if (0 == node.brushList.Num()) {
//		delete node.volume;
                node.volume = null;
                node.children[0] = node.children[1] = null;
                return;
            }

            bounds = node.volume.GetBounds();
            halfSize = (bounds.oGet(1).oMinus(bounds.oGet(0))).oMultiply(0.5f);
            for (axis = 0; axis < 3; axis++) {
                if (halfSize.oGet(axis) > BSP_GRID_SIZE) {
                    dist = (float) (BSP_GRID_SIZE * (floor((bounds.oGet(0, axis) + halfSize.oGet(axis)) / BSP_GRID_SIZE) + 1));
                } else {
                    dist = (float) (BSP_GRID_SIZE * (floor(bounds.oGet(0, axis) / BSP_GRID_SIZE) + 1));
                }
                if (dist > bounds.oGet(0, axis) + 1.0f && dist < bounds.oGet(1, axis) - 1.0f) {
                    break;
                }
            }
            if (axis >= 3) {
                gridCells.Append(node);
                return;
            }

            numSplits++;

            normal = getVec3_origin();
            normal.oSet(axis, 1.0f);
            node.plane.SetNormal(normal);
            node.plane.SetDist(dist);

            // allocate children
            node.children[0] = new idBrushBSPNode();
            node.children[1] = new idBrushBSPNode();

            // split volume and brush list for children
            node.volume.Split(node.plane, -1, node.children[0].volume, node.children[1].volume);
            node.brushList.Split(node.plane, -1, node.children[0].brushList, node.children[1].brushList);
            node.children[0].brushList.SetFlagOnFacingBrushSides(node.plane, SFL_USED_SPLITTER);
            node.children[1].brushList.SetFlagOnFacingBrushSides(node.plane, SFL_USED_SPLITTER);
            node.children[0].parent = node.children[1].parent = node;

            // free node memory
            node.brushList.Free();
//	delete node.volume;
            node.volume = null;

            // process children
            BuildGrid_r(gridCells, node.children[0]);
            BuildGrid_r(gridCells, node.children[1]);
        }

        private void PruneTree_r(idBrushBSPNode node, int contents) {
            int i, s;
            idBrushBSPNode[] nodes = new idBrushBSPNode[2];
            idBrushBSPPortal p, nextp;

            if (NOT(node.children[0]) || NOT(node.children[1])) {
                return;
            }

            PruneTree_r(node.children[0], contents);
            PruneTree_r(node.children[1], contents);

            if ((node.children[0].contents & node.children[1].contents & contents) != 0) {

                node.contents = node.children[0].contents | node.children[1].contents;
                // move all child portals to parent
                for (i = 0; i < 2; i++) {
                    for (p = node.children[i].portals; p != null; p = nextp) {
                        s = (p.nodes[1].equals(node.children[i])) ? 1 : 0;
                        nextp = p.next[s];
                        nodes[s] = node;
                        nodes[/*!s*/SNOT(s)] = p.nodes[/*!s*/SNOT(s)];
                        p.RemoveFromNode(p.nodes[0]);
                        p.RemoveFromNode(p.nodes[1]);
                        if (nodes[/*!s*/SNOT(s)].equals(node.children[/*!i*/SNOT(i)])) {
//					delete p;	// portal seperates both children
//                            p = null;
                        } else {
                            p.AddToNodes(nodes[0], nodes[1]);
                        }
                    }
                }

//		delete node.children[0];
//		delete node.children[1];
                node.children[0] = node.children[1] = null;

                numPrunedSplits++;
            }
        }

        private void MakeOutsidePortals() {
            int i, j, n;
            idBounds bounds;
            idBrushBSPPortal p;
            idBrushBSPPortal[] portals = new idBrushBSPPortal[6];
            idVec3 normal;
//            idPlane[] planes = new idPlane[6];

            // pad with some space so there will never be null volume leaves
            bounds = treeBounds.Expand(32);

            for (i = 0; i < 3; i++) {
                if (bounds.oGet(0, i) > bounds.oGet(1, i)) {
                    common.Error("empty BSP tree");
                }
            }

            outside = new idBrushBSPNode();
            outside.parent
                    = outside.children[0] = outside.children[1] = null;
            outside.brushList.Clear();
            outside.portals = null;
            outside.contents = 0;

            for (i = 0; i < 3; i++) {
                for (j = 0; j < 2; j++) {

                    p = new idBrushBSPPortal();
                    normal = getVec3_origin();
                    normal.oSet(i, j != 0 ? -1 : 1);
                    p.plane.SetNormal(normal);
                    p.plane.SetDist(j != 0 ? -bounds.oGet(j, i) : bounds.oGet(j, i));
                    p.winding = new idWinding(p.plane.Normal(), p.plane.Dist());
                    p.AddToNodes(root, outside);

                    n = j * 3 + i;
                    portals[n] = p;
                }
            }

            // clip the base windings with all the other planes
            for (i = 0; i < 6; i++) {
                for (j = 0; j < 6; j++) {
                    if (j == i) {
                        continue;
                    }
                    portals[i].winding = portals[i].winding.Clip(portals[j].plane, ON_EPSILON);
                }
            }
        }

        private idWinding BaseWindingForNode(idBrushBSPNode node) {
            idWinding w;
            idBrushBSPNode n;

            w = new idWinding(node.plane.Normal(), node.plane.Dist());

            // clip by all the parents
            for (n = node.parent; n != null && w != null; n = n.parent) {

                if (n.children[0].equals(node)) {
                    // take front
                    w = w.Clip(n.plane, BASE_WINDING_EPSILON);
                } else {
                    // take back
                    w = w.Clip(n.plane.oNegative(), BASE_WINDING_EPSILON);
                }
                node = n;
            }

            return w;
        }

        /*
         ============
         idBrushBSP::MakeNodePortal

         create the new portal by taking the full plane winding for the cutting
         plane and clipping it by all of parents of this node
         ============
         */
        private void MakeNodePortal(idBrushBSPNode node) {
            idBrushBSPPortal newPortal, p;
            idWinding w;
            int side = 0;

            w = BaseWindingForNode(node);

            // clip the portal by all the other portals in the node
            for (p = node.portals; p != null && w != null; p = p.next[side]) {
                if (p.nodes[0] == node) {
                    side = 0;
                    w = w.Clip(p.plane, 0.1f);
                } else if (p.nodes[1] == node) {
                    side = 1;
                    w = w.Clip(p.plane.oNegative(), 0.1f);
                } else {
                    common.Error("MakeNodePortal: mislinked portal");
                }
            }

            if (NOT(w)) {
                return;
            }

            if (w.IsTiny()) {
//		delete w;
                return;
            }

            newPortal = new idBrushBSPPortal();
            newPortal.plane = node.plane;
            newPortal.winding = w;
            newPortal.AddToNodes(node.children[0], node.children[1]);
        }

        /*
         ============
         idBrushBSP::SplitNodePortals

         Move or split the portals that bound the node so that the node's children have portals instead of node.
         ============
         */
        private void SplitNodePortals(idBrushBSPNode node) {
            int side = 0;
            idBrushBSPPortal p, nextPortal, newPortal;
            idBrushBSPNode f, b, otherNode;
            idPlane plane;
            idWinding frontWinding = new idWinding(), backWinding = new idWinding();

            plane = node.plane;
            f = node.children[0];
            b = node.children[1];

            for (p = node.portals; p != null; p = nextPortal) {
                if (p.nodes[0].equals(node)) {
                    side = 0;
                } else if (p.nodes[1].equals(node)) {
                    side = 1;
                } else {
                    common.Error("idBrushBSP::SplitNodePortals: mislinked portal");
                }
                nextPortal = p.next[side];

                otherNode = p.nodes[/*!side*/SNOT(side)];
                p.RemoveFromNode(p.nodes[0]);
                p.RemoveFromNode(p.nodes[1]);

                // cut the portal into two portals, one on each side of the cut plane
                p.winding.Split(plane, SPLIT_WINDING_EPSILON, frontWinding, backWinding);

                if (!frontWinding.isNULL() && frontWinding.IsTiny()) {
//			delete frontWinding;
                    frontWinding = null;
                    //tinyportals++;
                }

                if (!backWinding.isNULL() && backWinding.IsTiny()) {
//			delete backWinding;
                    backWinding = null;
                    //tinyportals++;
                }

                if (NOT(frontWinding) && NOT(backWinding)) {
                    // tiny windings on both sides
                    continue;
                }

                if (NOT(frontWinding)) {
//			delete backWinding;
                    if (side == 0) {
                        p.AddToNodes(b, otherNode);
                    } else {
                        p.AddToNodes(otherNode, b);
                    }
                    continue;
                }
                if (NOT(backWinding)) {
//			delete frontWinding;
                    if (side == 0) {
                        p.AddToNodes(f, otherNode);
                    } else {
                        p.AddToNodes(otherNode, f);
                    }
                    continue;
                }

                // the winding is split
//		newPortal = new idBrushBSPPortal();
                newPortal = p;
                newPortal.winding = backWinding;
//		delete p.winding;
                p.winding = frontWinding;

                if (side == 0) {
                    p.AddToNodes(f, otherNode);
                    newPortal.AddToNodes(b, otherNode);
                } else {
                    p.AddToNodes(otherNode, f);
                    newPortal.AddToNodes(otherNode, b);
                }
            }

            node.portals = null;
        }

        private void MakeTreePortals_r(idBrushBSPNode node) {
            int i;
            idBounds bounds;

            numPortals++;
            DisplayRealTimeString("\r%6d", numPortals);

            bounds = node.GetPortalBounds();

//	if ( bounds[0][0] >= bounds[1][0] ) {
//		//common.Warning( "node without volume" );
//	}
            for (i = 0; i < 3; i++) {
                if (bounds.oGet(0, i) < MIN_WORLD_COORD || bounds.oGet(1, i) > MAX_WORLD_COORD) {
                    common.Warning("node with unbounded volume");
                    break;
                }
            }

            if (NOT(node.children[0]) || NOT(node.children[1])) {
                return;
            }

            MakeNodePortal(node);
            SplitNodePortals(node);

            MakeTreePortals_r(node.children[0]);
            MakeTreePortals_r(node.children[1]);
        }

        private void FloodThroughPortals_r(idBrushBSPNode node, int contents, int depth) {
            idBrushBSPPortal p;
            int s;

            if (node.occupied != 0) {
                common.Error("FloodThroughPortals_r: node already occupied\n");
            }
            if (NOT(node)) {
                common.Error("FloodThroughPortals_r: NULL node\n");
            }

            node.occupied = depth;

            for (p = node.portals; p != null; p = p.next[s]) {
                s = (p.nodes[1].equals(node)) ? 1 : 0;

                // if the node at the other side of the portal is removed
                if (NOT(p.nodes[/*!s*/SNOT(s)])) {
                    continue;
                }

                // if the node at the other side of the portal is occupied already
                if (p.nodes[/*!s*/SNOT(s)].occupied != 0) {
                    continue;
                }

                // can't flood through the portal if it has the seperating contents at the other side
                if ((p.nodes[/*!s*/SNOT(s)].contents & contents) != 0) {
                    continue;
                }

                // flood recursively through the current portal
                FloodThroughPortals_r(p.nodes[/*!s*/SNOT(s)], contents, depth + 1);
            }
        }

        private boolean FloodFromOrigin(final idVec3 origin, int contents) {
            idBrushBSPNode node;

            //find the leaf to start in
            node = root;
            while (node.children[0] != null && node.children[1] != null) {

                if (node.plane.Side(origin) == PLANESIDE_BACK) {
                    node = node.children[1];
                } else {
                    node = node.children[0];
                }
            }

            if (NOT(node)) {
                return false;
            }

            // if inside the inside/outside seperating contents
            if ((node.contents & contents) != 0) {
                return false;
            }

            // if the node is already occupied
            if (node.occupied != 0) {
                return false;
            }

            FloodThroughPortals_r(node, contents, 1);

            return true;
        }

        /*
         ============
         idBrushBSP::FloodFromEntities

         Marks all nodes that can be reached by entites.
         ============
         */
        private boolean FloodFromEntities(final idMapFile mapFile, int contents, final idStrList classNames) {
            int i, j;
            boolean inside;
            idVec3 origin = new idVec3();
            idMapEntity mapEnt;
            idStr classname = new idStr();

            inside = false;
            outside.occupied = 0;

            // skip the first entity which is assumed to be the worldspawn
            for (i = 1; i < mapFile.GetNumEntities(); i++) {

                mapEnt = mapFile.GetEntity(i);

                if (!mapEnt.epairs.GetVector("origin", "", origin)) {
                    continue;
                }

                if (!mapEnt.epairs.GetString("classname", "", classname)) {
                    continue;
                }

                for (j = 0; j < classNames.Num(); j++) {
                    if (classname.Icmp(classNames.oGet(j)) == 0) {
                        break;
                    }
                }

                if (j >= classNames.Num()) {
                    continue;
                }

                origin.oPluSet(2, 1);

                // nudge around a little
                if (FloodFromOrigin(origin, contents)) {
                    inside = true;
                }

                if (outside.occupied != 0) {
                    leakOrigin = origin;
                    break;
                }
            }

            if (!inside) {
                common.Warning("no entities inside");
            } else if (outside.occupied != 0) {
                common.Warning("reached outside from entity %d (%s)", i, classname);
            }

            return (inside && 0 == outside.occupied);
        }

        private void RemoveOutside_r(idBrushBSPNode node, int contents) {

            if (NOT(node)) {
                return;
            }

            if (node.children[0] != null || node.children[1] != null) {
                RemoveOutside_r(node.children[0], contents);
                RemoveOutside_r(node.children[1], contents);
                return;
            }

            if (0 == node.occupied) {
                if (0 == (node.contents & contents)) {
                    outsideLeafNodes++;
                    node.contents |= contents;
                } else {
                    solidLeafNodes++;
                }
            } else {
                insideLeafNodes++;
            }
        }

        private void SetPortalPlanes_r(idBrushBSPNode node, idPlaneSet planeList) {
            int s;
            idBrushBSPPortal p;

            if (NOT(node)) {
                return;
            }

            for (p = node.portals; p != null; p = p.next[s]) {
                s = (p.nodes[1].equals(node)) ? 1 : 0;
                if (p.planeNum == -1) {
                    p.planeNum = planeList.FindPlane(p.plane, PORTAL_PLANE_NORMAL_EPSILON, PORTAL_PLANE_DIST_EPSILON);
                }
            }
            SetPortalPlanes_r(node.children[0], planeList);
            SetPortalPlanes_r(node.children[1], planeList);
        }

        /*
         ============
         idBrushBSP::SetPortalPlanes

         give all portals a plane number
         ============
         */
        private void SetPortalPlanes() {
            SetPortalPlanes_r(root, portalPlanes);
        }

        private void MergePortals_r(idBrushBSPNode node, int skipContents) {

            if (NOT(node)) {
                return;
            }

            if ((node.contents & skipContents) != 0) {
                return;
            }

            if (NOT(node.children[0]) && NOT(node.children[1])) {
                MergeLeafNodePortals(node, skipContents);
                return;
            }

            MergePortals_r(node.children[0], skipContents);
            MergePortals_r(node.children[1], skipContents);
        }

        private void MergeLeafNodePortals(idBrushBSPNode node, int skipContents) {
            int s1, s2;
            boolean foundPortal;
            idBrushBSPPortal p1, p2, nextp1, nextp2;
            idWinding newWinding, reverse;

            // pass 1: merge all portals that seperate the same leaf nodes
            for (p1 = node.GetPortals(); p1 != null; p1 = nextp1) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;
                nextp1 = p1.Next(s1);

                for (p2 = nextp1; p2 != null; p2 = nextp2) {
                    s2 = (p2.GetNode(1).equals(node)) ? 1 : 0;
                    nextp2 = p2.Next(s2);

                    // if both portals seperate the same leaf nodes
                    if (p1.nodes[/*!s1*/SNOT(s1)].equals(p2.nodes[/*!s2*/SNOT(s2)])) {

                        // add the winding of p2 to the winding of p1
                        p1.winding.AddToConvexHull(p2.winding, p1.plane.Normal());

                        // delete p2
                        p2.RemoveFromNode(p2.nodes[0]);
                        p2.RemoveFromNode(p2.nodes[1]);
//				delete p2;

                        numMergedPortals++;

                        nextp1 = node.GetPortals();
                        break;
                    }
                }
            }

            // pass 2: merge all portals in the same plane if they all have the skip contents at the other side
            for (p1 = node.GetPortals(); p1 != null; p1 = nextp1) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;
                nextp1 = p1.Next(s1);

                if (0 == (p1.nodes[/*!s1*/SNOT(s1)].contents & skipContents)) {
                    continue;
                }

                // test if all portals in this plane have the skip contents at the other side
                foundPortal = false;
                for (p2 = node.GetPortals(); p2 != null; p2 = nextp2) {
                    s2 = (p2.GetNode(1).equals(node)) ? 1 : 0;
                    nextp2 = p2.Next(s2);

                    if (p2 == p1 || (p2.planeNum & ~1) != (p1.planeNum & ~1)) {
                        continue;
                    }
                    foundPortal = true;
                    if (0 == (p2.nodes[/*!s2*/SNOT(s2)].contents & skipContents)) {
                        break;
                    }
                }

                // if all portals in this plane have the skip contents at the other side
                if (NOT(p2) && foundPortal) {
                    for (p2 = node.GetPortals(); p2 != null; p2 = nextp2) {
                        s2 = (p2.GetNode(1).equals(node)) ? 1 : 0;
                        nextp2 = p2.Next(s2);

                        if (p2 == p1 || (p2.planeNum & ~1) != (p1.planeNum & ~1)) {
                            continue;
                        }

                        // add the winding of p2 to the winding of p1
                        p1.winding.AddToConvexHull(p2.winding, p1.plane.Normal());

                        // delete p2
                        p2.RemoveFromNode(p2.nodes[0]);
                        p2.RemoveFromNode(p2.nodes[1]);
//				delete p2;

                        numMergedPortals++;
                    }
                    nextp1 = node.GetPortals();
                }
            }

            // pass 3: try to merge portals in the same plane that have the skip contents at the other side
            for (p1 = node.GetPortals(); p1 != null; p1 = nextp1) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;
                nextp1 = p1.Next(s1);

                if (0 == (p1.nodes[/*!s1*/SNOT(s1)].contents & skipContents)) {
                    continue;
                }

                for (p2 = nextp1; p2 != null; p2 = nextp2) {
                    s2 = (p2.GetNode(1).equals(node)) ? 1 : 0;
                    nextp2 = p2.Next(s2);

                    if (0 == (p2.nodes[/*!s2*/SNOT(s2)].contents & skipContents)) {
                        continue;
                    }

                    if ((p2.planeNum & ~1) != (p1.planeNum & ~1)) {
                        continue;
                    }

                    // try to merge the two portal windings
                    if (p2.planeNum == p1.planeNum) {
                        newWinding = p1.winding.TryMerge(p2.winding, p1.plane.Normal());
                    } else {
                        reverse = p2.winding.Reverse();
                        newWinding = p1.winding.TryMerge(reverse, p1.plane.Normal());
//				delete reverse;
                    }

                    // if successfully merged
                    if (newWinding != null) {

                        // replace the winding of the first portal
//				delete p1.winding;
                        p1.winding = newWinding;

                        // delete p2
                        p2.RemoveFromNode(p2.nodes[0]);
                        p2.RemoveFromNode(p2.nodes[1]);
//				delete p2;

                        numMergedPortals++;

                        nextp1 = node.GetPortals();
                        break;
                    }
                }
            }
        }

        private void UpdateTreeAfterMerge_r(idBrushBSPNode node, final idBounds bounds, idBrushBSPNode oldNode, idBrushBSPNode newNode) {

            if (NOT(node)) {
                return;
            }

            if (NOT(node.children[0]) && NOT(node.children[1])) {
                return;
            }

            if (node.children[0].equals(oldNode)) {
                node.children[0] = newNode;
            }
            if (node.children[1].equals(oldNode)) {
                node.children[1] = newNode;
            }

            switch (bounds.PlaneSide(node.plane, 2.0f)) {
                case PLANESIDE_FRONT:
                    UpdateTreeAfterMerge_r(node.children[0], bounds, oldNode, newNode);
                    break;
                case PLANESIDE_BACK:
                    UpdateTreeAfterMerge_r(node.children[1], bounds, oldNode, newNode);
                    break;
                default:
                    UpdateTreeAfterMerge_r(node.children[0], bounds, oldNode, newNode);
                    UpdateTreeAfterMerge_r(node.children[1], bounds, oldNode, newNode);
                    break;
            }
        }

        private void RemoveLeafNodeColinearPoints(idBrushBSPNode node) {
            int s1;
            idBrushBSPPortal p1;

            // remove colinear points
            for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;
                p1.winding.RemoveColinearPoints(p1.plane.Normal(), 0.1f);
            }
        }

        private void RemoveColinearPoints_r(idBrushBSPNode node, int skipContents) {
            if (NOT(node)) {
                return;
            }

            if ((node.contents & skipContents) != 0) {
                return;
            }

            if (NOT(node.children[0]) && NOT(node.children[1])) {
                RemoveLeafNodeColinearPoints(node);
                return;
            }

            RemoveColinearPoints_r(node.children[0], skipContents);
            RemoveColinearPoints_r(node.children[1], skipContents);
        }

        /*
         ============
         idBrushBSP::MeltFloor_r

         flood through portals touching the bounds to find all vertices that might be inside the bounds
         ============
         */
        private void MeltFlood_r(idBrushBSPNode node, int skipContents, idBounds bounds, idVectorSet<idVec3/*,3*/> vertexList) {
            int s1, i;
            idBrushBSPPortal p1;
            idBounds b = new idBounds();
            idWinding w;

            node.SetFlag(NODE_VISITED);

            for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;

                if ((p1.GetNode( /*!s1*/SNOT(s1)).GetFlags() & NODE_VISITED) != 0) {
                    continue;
                }

                w = p1.GetWinding();

                for (i = 0; i < w.GetNumPoints(); i++) {
                    if (bounds.ContainsPoint(w.oGet(i).ToVec3())) {
                        vertexList.FindVector(w.oGet(i).ToVec3(), VERTEX_MELT_EPSILON);
                    }
                }
            }

            for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;

                if ((p1.GetNode( /*!s1*/SNOT(s1)).GetFlags() & NODE_VISITED) != 0) {
                    continue;
                }

                if ((p1.GetNode( /*!s1*/SNOT(s1)).GetContents() & skipContents) != 0) {
                    continue;
                }

                w = p1.GetWinding();

                w.GetBounds(b);

                if (!bounds.IntersectsBounds(b)) {
                    continue;
                }

                MeltFlood_r(p1.GetNode( /*!s1*/SNOT(s1)), skipContents, bounds, vertexList
                );
            }
        }

        private void MeltLeafNodePortals(idBrushBSPNode node, int skipContents, idVectorSet<idVec3/*,3*/> vertexList) {
            int s1, i;
            idBrushBSPPortal p1;
            idBounds bounds = new idBounds();

            if ((node.GetFlags() & NODE_DONE) != 0) {
                return;
            }

            node.SetFlag(NODE_DONE);

            // melt things together
            for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;

                if ((p1.GetNode( /*!s1*/SNOT(s1)).GetFlags() & NODE_DONE) != 0) {
                    continue;
                }

                p1.winding.GetBounds(bounds);
                bounds.ExpandSelf(2 * VERTEX_MELT_HASH_SIZE * VERTEX_MELT_EPSILON);
                vertexList.Init(bounds.oGet(0), bounds.oGet(1), (int) VERTEX_MELT_HASH_SIZE, 128);

                // get all vertices to be considered
                MeltFlood_r(node, skipContents, bounds, vertexList);
                node.RemoveFlagFlood(NODE_VISITED);

                for (i = 0; i < vertexList.Num(); i++) {
                    if (p1.winding.InsertPointIfOnEdge(vertexList.oGet(i), p1.plane, 0.1f)) {
                        numInsertedPoints++;
                    }
                }
            }
            DisplayRealTimeString("\r%6d", numInsertedPoints);
        }

        private void MeltPortals_r(idBrushBSPNode node, int skipContents, idVectorSet<idVec3/*,3*/> vertexList) {
            if (NOT(node)) {
                return;
            }

            if ((node.contents & skipContents) != 0) {
                return;
            }

            if (NOT(node.children[0]) && NOT(node.children[1])) {
                MeltLeafNodePortals(node, skipContents, vertexList);
                return;
            }

            MeltPortals_r(node.children[0], skipContents, vertexList);
            MeltPortals_r(node.children[1], skipContents, vertexList);
        }
    };
}
