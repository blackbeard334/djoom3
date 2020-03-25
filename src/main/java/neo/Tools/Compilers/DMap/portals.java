package neo.Tools.Compilers.DMap;

import static neo.Renderer.Material.CONTENTS_AREAPORTAL;
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.Tools.Compilers.DMap.dmap.PLANENUM_LEAF;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import static neo.Tools.Compilers.DMap.ubrush.CLIP_EPSILON;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Lib.MAX_WORLD_COORD;
import static neo.idlib.Lib.MIN_WORLD_COORD;
import static neo.idlib.math.Plane.ON_EPSILON;

import neo.Renderer.Material.idMaterial;
import neo.Tools.Compilers.DMap.dmap.node_s;
import neo.Tools.Compilers.DMap.dmap.side_s;
import neo.Tools.Compilers.DMap.dmap.tree_s;
import neo.Tools.Compilers.DMap.dmap.uBrush_t;
import neo.Tools.Compilers.DMap.dmap.uEntity_t;
import neo.Tools.Compilers.DMap.dmap.uPortal_s;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class portals {

    static final int   MAX_INTER_AREA_PORTALS = 1024;
    //
    static final int   SIDESPACE              = 8;
    //
    static final float BASE_WINDING_EPSILON   = 0.001f;
    static final float SPLIT_WINDING_EPSILON  = 0.001f;
    //
    static int c_tinyportals;
    //
    static int c_floodedleafs;
    //
    static int c_areas;
    static int c_areaFloods;
    //
    static int c_outside;
    static int c_inside;
    static int c_solid;

    static class interAreaPortal_t {

        int area0, area1;
        side_s side;
    }
    //
    public static final interAreaPortal_t[] interAreaPortals = new interAreaPortal_t[MAX_INTER_AREA_PORTALS];
    public static int numInterAreaPortals;
    //
    static        int c_active_portals;
    static        int c_peak_portals;

    /*
     ===========
     AllocPortal
     ===========
     */
    static uPortal_s AllocPortal() {
        uPortal_s p;

        c_active_portals++;
        if (c_active_portals > c_peak_portals) {
            c_peak_portals = c_active_portals;
        }

        p = new uPortal_s();// Mem_Alloc(sizeof(uPortal_s));
//	memset (p, 0, sizeof(uPortal_s ));

        return p;
    }

    static void FreePortal(uPortal_s p) {
        if (p.winding != null) //		delete p.winding;
        {
            p.winding = null;
        }
        c_active_portals--;
        p.clear();//Mem_Free(p);
    }

//==============================================================

    /*
     =============
     Portal_Passable

     Returns true if the portal has non-opaque leafs on both sides
     =============
     */
    static boolean Portal_Passable(uPortal_s p) {
        if (NOT(p.onnode)) {
            return false;    // to global outsideleaf
        }

        if ((p.nodes[0].planenum != PLANENUM_LEAF)
                || (p.nodes[1].planenum != PLANENUM_LEAF)) {
            common.Error("Portal_EntityFlood: not a leaf");
        }

        if (!p.nodes[0].opaque && !p.nodes[1].opaque) {
            return true;
        }

        return false;
    }

//=============================================================================
    /*
     =============
     AddPortalToNodes
     =============
     */
    static void AddPortalToNodes(uPortal_s p, node_s front, node_s back) {
        if ((p.nodes[0] != null) || (p.nodes[1] != null)) {
            common.Error("AddPortalToNode: allready included");
        }

        p.nodes[0] = front;
        p.next[0] = front.portals;
        front.portals = p;

        p.nodes[1] = back;
        p.next[1] = back.portals;
        back.portals = p;
    }


    /*
     =============
     RemovePortalFromNode
     =============
     */
    static void RemovePortalFromNode(uPortal_s portal, node_s l) {
        uPortal_s pp;
        uPortal_s t;

// remove reference to the current portal
        pp = l.portals;
        while (true) {
            t = pp;
            if (NOT(t)) {
                common.Error("RemovePortalFromNode: portal not in leaf");
            }

            if (t.equals(portal)) {
                break;
            }

            if (t.nodes[0].equals(l)) {
                l.portals = pp = t.next[0];//TODO:check this pointer to a pointer assignment.
            } else if (t.nodes[1] == l) {
                l.portals = pp = t.next[1];
            } else {
                common.Error("RemovePortalFromNode: portal not bounding leaf");
            }
        }

        if (portal.nodes[0] == l) {
            l.portals = pp = portal.next[0];
            portal.nodes[0] = null;
        } else if (portal.nodes[1] == l) {
            l.portals = pp = portal.next[1];
            portal.nodes[1] = null;
        } else {
            common.Error("RemovePortalFromNode: mislinked");
        }
    }

//============================================================================
    static void PrintPortal(uPortal_s p) {
        int i;
        idWinding w;

        w = p.winding;
        for (i = 0; i < w.GetNumPoints(); i++) {
            common.Printf("(%5.0f,%5.0f,%5.0f)\n", w.oGet(i, 0), w.oGet(i, 1), w.oGet(i, 2));
        }
    }

    /*
     ================
     MakeHeadnodePortals

     The created portals will face the global outside_node
     ================
     */
    static void MakeHeadnodePortals(tree_s tree) {
        final idBounds bounds = new idBounds();
        int i, j, n;
        uPortal_s p;
        final uPortal_s[] portals = new uPortal_s[6];
        final idPlane[] bplanes = new idPlane[6];
        idPlane pl;
        node_s node;

        node = tree.headnode;

        tree.outside_node.planenum = PLANENUM_LEAF;
        tree.outside_node.brushlist = null;
        tree.outside_node.portals = null;
        tree.outside_node.opaque = false;

        // if no nodes, don't go any farther
        if (node.planenum == PLANENUM_LEAF) {
            return;
        }

        // pad with some space so there will never be null volume leafs
        for (i = 0; i < 3; i++) {
            bounds.oSet(0, i, tree.bounds.oGet(0, i) - SIDESPACE);
            bounds.oSet(1, i, tree.bounds.oGet(1, i) - SIDESPACE);
            if (bounds.oGet(0, i) >= bounds.oGet(1, i)) {
                common.Error("Backwards tree volume");
            }
        }

        for (i = 0; i < 3; i++) {
            for (j = 0; j < 2; j++) {
                n = (j * 3) + i;

                p = AllocPortal();
                portals[n] = p;

                pl = bplanes[n] = new idPlane();//TODO:is this a pointer?
//			memset (pl, 0, sizeof(*pl));
                if (j != 0) {
                    pl.oSet(i, -1);
                    pl.oSet(3, bounds.oGet(j, i));
                } else {
                    pl.oSet(i, 1);
                    pl.oSet(3, -bounds.oGet(j, i));
                }
                p.plane = pl;
                p.winding = new idWinding(pl);
                AddPortalToNodes(p, node, tree.outside_node);
            }
        }

        // clip the basewindings by all the other planes
        for (i = 0; i < 6; i++) {
            for (j = 0; j < 6; j++) {
                if (j == i) {
                    continue;
                }
                portals[i].winding = portals[i].winding.Clip(bplanes[j], ON_EPSILON);
            }
        }
    }

//===================================================
    /*
     ================
     BaseWindingForNode
     ================
     */
    static idWinding BaseWindingForNode(node_s node) {
        idWinding w;
        node_s n;

        w = new idWinding(dmapGlobals.mapPlanes.oGet(node.planenum));

        // clip by all the parents
        for (n = node.parent; (n != null) && (w != null);) {
            final idPlane plane = dmapGlobals.mapPlanes.oGet(n.planenum);

            if (n.children[0].equals(node)) {
                // take front
                w = w.Clip(plane, BASE_WINDING_EPSILON);
            } else {
                // take back
                final idPlane back = plane.oNegative();
                w = w.Clip(back, BASE_WINDING_EPSILON);
            }
            node = n;
            n = n.parent;
        }

        return w;
    }

//============================================================

    /*
     ==================
     MakeNodePortal

     create the new portal by taking the full plane winding for the cutting plane
     and clipping it by all of parents of this node
     ==================
     */
    static void MakeNodePortal(node_s node) {
        uPortal_s new_portal, p;
        idWinding w;
//        idVec3 normal;
        int side;

        w = BaseWindingForNode(node);

        // clip the portal by all the other portals in the node
        for (p = node.portals; (p != null) && (w != null); p = p.next[side]) {
            idPlane plane = new idPlane();

            if (p.nodes[0].equals(node)) {
                side = 0;
                plane = p.plane;
            } else if (p.nodes[1] == node) {
                side = 1;
                plane = p.plane.oNegative();
            } else {
                common.Error("CutNodePortals_r: mislinked portal");
                side = 0;	// quiet a compiler warning
            }

            w = w.Clip(plane, CLIP_EPSILON);
        }

        if (NOT(w)) {
            return;
        }

        if (w.IsTiny()) {
            c_tinyportals++;
//		delete w;
            return;
        }

        new_portal = AllocPortal();
        new_portal.plane = dmapGlobals.mapPlanes.oGet(node.planenum);
        new_portal.onnode = node;
        new_portal.winding = w;
        AddPortalToNodes(new_portal, node.children[0], node.children[1]);
    }


    /*
     ==============
     SplitNodePortals

     Move or split the portals that bound node so that the node's
     children have portals instead of node.
     ==============
     */
    static void SplitNodePortals(node_s node) {
        uPortal_s p, next_portal, new_portal;
        node_s f, b, other_node;
        int side;
        idPlane plane;
        idWinding frontwinding = new idWinding(), backwinding = new idWinding();

        plane = dmapGlobals.mapPlanes.oGet(node.planenum);
        f = node.children[0];
        b = node.children[1];

        for (p = node.portals; p != null; p = next_portal) {
            if (p.nodes[0] == node) {
                side = 0;
            } else if (p.nodes[1] == node) {
                side = 1;
            } else {
                common.Error("SplitNodePortals: mislinked portal");
                side = 0;	// quiet a compiler warning
            }
            next_portal = p.next[side];

            other_node = p.nodes[/*!side*/1 ^ side];
            RemovePortalFromNode(p, p.nodes[0]);
            RemovePortalFromNode(p, p.nodes[1]);

            //
            // cut the portal into two portals, one on each side of the cut plane
            //
            p.winding.Split(plane, SPLIT_WINDING_EPSILON, frontwinding, backwinding);

            if ((frontwinding != null) && frontwinding.IsTiny()) {
//			delete frontwinding;
                frontwinding = null;
                c_tinyportals++;
            }

            if ((backwinding != null) && backwinding.IsTiny()) {
//			delete backwinding;
                backwinding = null;
                c_tinyportals++;
            }

            if (NOT(frontwinding) && NOT(backwinding)) {	// tiny windings on both sides
                continue;
            }

            if (NOT(frontwinding)) {
//			delete backwinding;
                if (side == 0) {
                    AddPortalToNodes(p, b, other_node);
                } else {
                    AddPortalToNodes(p, other_node, b);
                }
                continue;
            }
            if (NOT(backwinding)) {
//			delete frontwinding;
                if (side == 0) {
                    AddPortalToNodes(p, f, other_node);
                } else {
                    AddPortalToNodes(p, other_node, f);
                }
                continue;
            }

            // the winding is split
            new_portal = AllocPortal();
            new_portal = p;
            new_portal.winding = backwinding;
//		delete p.winding;
            p.winding = frontwinding;

            if (side == 0) {
                AddPortalToNodes(p, f, other_node);
                AddPortalToNodes(new_portal, b, other_node);
            } else {
                AddPortalToNodes(p, other_node, f);
                AddPortalToNodes(new_portal, other_node, b);
            }
        }

        node.portals = null;
    }


    /*
     ================
     CalcNodeBounds
     ================
     */
    static void CalcNodeBounds(node_s node) {
        uPortal_s p;
        int s;
        int i;

        // calc mins/maxs for both leafs and nodes
        node.bounds.Clear();
        for (p = node.portals; p != null; p = p.next[s]) {
            s = (p.nodes[1].equals(node)) ? 1 : 0;
            for (i = 0; i < p.winding.GetNumPoints(); i++) {
                node.bounds.AddPoint(p.winding.oGet(i).ToVec3());
            }
        }
    }


    /*
     ==================
     MakeTreePortals_r
     ==================
     */
    static void MakeTreePortals_r(node_s node) {
        int i;

        CalcNodeBounds(node);

        if (node.bounds.oGet(0, 0) >= node.bounds.oGet(1, 0)) {
            common.Warning("node without a volume");
        }

        for (i = 0; i < 3; i++) {
            if ((node.bounds.oGet(0, i) < MIN_WORLD_COORD) || (node.bounds.oGet(1, i) > MAX_WORLD_COORD)) {
                common.Warning("node with unbounded volume");
                break;
            }
        }
        if (node.planenum == PLANENUM_LEAF) {
            return;
        }

        MakeNodePortal(node);
        SplitNodePortals(node);

        MakeTreePortals_r(node.children[0]);
        MakeTreePortals_r(node.children[1]);
    }

    /*
     ==================
     MakeTreePortals
     ==================
     */
    static void MakeTreePortals(tree_s tree) {
        common.Printf("----- MakeTreePortals -----\n");
        MakeHeadnodePortals(tree);
        MakeTreePortals_r(tree.headnode);
    }

    /*
     =========================================================

     FLOOD ENTITIES

     =========================================================
     */
    /*
     =============
     FloodPortals_r
     =============
     */
    static void FloodPortals_r(node_s node, int dist) {
        uPortal_s p;
        int s;

        if (node.occupied != 0) {
            return;
        }

        if (node.opaque) {
            return;
        }

        c_floodedleafs++;
        node.occupied = dist;

        for (p = node.portals; p != null; p = p.next[s]) {
            s = (p.nodes[1].equals(node)) ? 1 : 0;
            FloodPortals_r(p.nodes[/*!s*/1 ^ s], dist + 1);
        }
    }

    /*
     =============
     PlaceOccupant
     =============
     */
    static boolean PlaceOccupant(node_s headnode, idVec3 origin, uEntity_t occupant) {
        node_s node;
        float d;
        idPlane plane;

        // find the leaf to start in
        node = headnode;
        while (node.planenum != PLANENUM_LEAF) {
            plane = dmapGlobals.mapPlanes.oGet(node.planenum);
            d = plane.Distance(origin);
            if (d >= 0.0f) {
                node = node.children[0];
            } else {
                node = node.children[1];
            }
        }

        if (node.opaque) {
            return false;
        }
        node.occupant = occupant;

        FloodPortals_r(node, 1);

        return true;
    }

    /*
     =============
     FloodEntities

     Marks all nodes that can be reached by entites
     =============
     */
    static boolean FloodEntities(tree_s tree) {
        int i;
        final idVec3 origin = new idVec3();
        final String[] cl = {null};
        boolean inside;
        node_s headnode;

        headnode = tree.headnode;
        common.Printf("--- FloodEntities ---\n");
        inside = false;
        tree.outside_node.occupied = 0;

        c_floodedleafs = 0;
        boolean errorShown = false;
        for (i = 1; i < dmapGlobals.num_entities; i++) {
            idMapEntity mapEnt;

            mapEnt = dmapGlobals.uEntities[i].mapEntity;
            if (!mapEnt.epairs.GetVector("origin", "", origin)) {
                continue;
            }

            // any entity can have "noFlood" set to skip it
            if (mapEnt.epairs.GetString("noFlood", "", cl)) {
                continue;
            }

            mapEnt.epairs.GetString("classname", "", cl);

            if (cl[0].equals("light")) {
                final String[] v = {null};

                // don't place lights that have a light_start field, because they can still
                // be valid if their origin is outside the world
                mapEnt.epairs.GetString("light_start", "", v);
                if (isNotNullOrEmpty(v[0])) {
                    continue;
                }

                // don't place fog lights, because they often
                // have origins outside the light
                mapEnt.epairs.GetString("texture", "", v);
                if (isNotNullOrEmpty(v[0])) {
                    final idMaterial mat = declManager.FindMaterial(v[0]);
                    if (mat.IsFogLight()) {
                        continue;
                    }
                }
            }

            if (PlaceOccupant(headnode, origin, dmapGlobals.uEntities[i])) {
                inside = true;
            }

            if ((tree.outside_node.occupied != 0) && !errorShown) {
                errorShown = true;
                common.Printf("Leak on entity # %d\n", i);
                final String[] p = {null};

                mapEnt.epairs.GetString("classname", "", p);
                common.Printf("Entity classname was: %s\n", p[0]);
                mapEnt.epairs.GetString("name", "", p);
                common.Printf("Entity name was: %s\n", p[0]);
                final idVec3 origin2 = new idVec3();
                if (mapEnt.epairs.GetVector("origin", "", origin2)) {
                    common.Printf("Entity origin is: %f %f %f\n\n\n", origin2.x, origin2.y, origin2.z);
                }
            }
        }

        common.Printf("%5d flooded leafs\n", c_floodedleafs);

        if (!inside) {
            common.Printf("no entities in open -- no filling\n");
        } else if (tree.outside_node.occupied != 0) {
            common.Printf("entity reached from outside -- no filling\n");
        }

        return (inside && (0 == tree.outside_node.occupied));
    }

    /*
     =========================================================

     FLOOD AREAS

     =========================================================
     */
    /*
     =================
     FindSideForPortal
     =================
     */
    static side_s FindSideForPortal(uPortal_s p) {
        int i, j, k;
        node_s node;
        uBrush_t b, orig;
        side_s s, s2;

        // scan both bordering nodes brush lists for a portal brush
        // that shares the plane
        for (i = 0; i < 2; i++) {
            node = p.nodes[i];
            for (b = node.brushlist; b != null; b = (uBrush_t) b.next) {
                if (0 == (b.contents & CONTENTS_AREAPORTAL)) {
                    continue;
                }
                orig = (uBrush_t) b.original;
                for (j = 0; j < orig.numsides; j++) {
                    s = orig.sides[j];
                    if (NOT(s.visibleHull)) {
                        continue;
                    }
                    if (0 == (s.material.GetContentFlags() & CONTENTS_AREAPORTAL)) {
                        continue;
                    }
                    if ((s.planenum & ~1) != (p.onnode.planenum & ~1)) {
                        continue;
                    }
                    // remove the visible hull from any other portal sides of this portal brush
                    for (k = 0; k < orig.numsides; k++) {
                        if (k == j) {
                            continue;
                        }
                        s2 = orig.sides[k];
                        if (null == s2.visibleHull) {
                            continue;
                        }
                        if (0 == (s2.material.GetContentFlags() & CONTENTS_AREAPORTAL)) {
                            continue;
                        }
                        common.Warning("brush has multiple area portal sides at %s", s2.visibleHull.GetCenter().ToString());
//					delete s2.visibleHull;
                        s2.visibleHull = null;
                    }
                    return s;
                }
            }
        }
        return null;
    }

    /*
     =============
     FloodAreas_r
     =============
     */
    static void FloodAreas_r(node_s node) {
        uPortal_s p;
        int s;

        if (node.area != -1) {
            return;		// allready got it
        }
        if (node.opaque) {
            return;
        }

        c_areaFloods++;
        node.area = c_areas;

        for (p = node.portals; p != null; p = p.next[s]) {
            node_s other;

            s = (p.nodes[1].equals(node)) ? 1 : 0;
            other = p.nodes[/*!s*/1 ^ s];

            if (!Portal_Passable(p)) {
                continue;
            }

            // can't flood through an area portal
            if (FindSideForPortal(p) != null) {
                continue;
            }

            FloodAreas_r(other);
        }
    }

    /*
     =============
     FindAreas_r

     Just decend the tree, and for each node that hasn't had an
     area set, flood fill out from there
     =============
     */
    static void FindAreas_r(node_s node) {
        if (node.planenum != PLANENUM_LEAF) {
            FindAreas_r(node.children[0]);
            FindAreas_r(node.children[1]);
            return;
        }

        if (node.opaque) {
            return;
        }

        if (node.area != -1) {
            return;		// allready got it
        }

        c_areaFloods = 0;
        FloodAreas_r(node);
        common.Printf("area %d has %d leafs\n", c_areas, c_areaFloods);
        c_areas++;
    }

    /*
     ============
     CheckAreas_r
     ============
     */
    static void CheckAreas_r(node_s node) {
        if (node.planenum != PLANENUM_LEAF) {
            CheckAreas_r(node.children[0]);
            CheckAreas_r(node.children[1]);
            return;
        }
        if (!node.opaque && (node.area < 0)) {
            common.Error("CheckAreas_r: area = %d", node.area);
        }
    }

    /*
     ============
     ClearAreas_r

     Set all the areas to -1 before filling
     ============
     */
    static void ClearAreas_r(node_s node) {
        if (node.planenum != PLANENUM_LEAF) {
            ClearAreas_r(node.children[0]);
            ClearAreas_r(node.children[1]);
            return;
        }
        node.area = -1;
    }

//=============================================================
    /*
     =================
     FindInterAreaPortals_r

     =================
     */
    static void FindInterAreaPortals_r(node_s node) {
        uPortal_s p;
        int s;
        int i;
        idWinding w;
        interAreaPortal_t iap;
        side_s side;

        if (node.planenum != PLANENUM_LEAF) {
            FindInterAreaPortals_r(node.children[0]);
            FindInterAreaPortals_r(node.children[1]);
            return;
        }

        if (node.opaque) {
            return;
        }

        for (p = node.portals; p != null; p = p.next[s]) {
            node_s other;

            s = (p.nodes[1].equals(node)) ? 1 : 0;
            other = p.nodes[/*!s*/1 ^ s];

            if (other.opaque) {
                continue;
            }

            // only report areas going from lower number to higher number
            // so we don't report the portal twice
            if (other.area <= node.area) {
                continue;
            }

            side = FindSideForPortal(p);
//		w = p.winding;
            if (NOT(side)) {
                common.Warning("FindSideForPortal failed at %s", p.winding.GetCenter().ToString());
                continue;
            }
            w = side.visibleHull;
            if (NOT(w)) {
                continue;
            }

            // see if we have created this portal before
            for (i = 0; i < numInterAreaPortals; i++) {
                iap = interAreaPortals[i];

                if ((side == iap.side)
                        && (((p.nodes[0].area == iap.area0) && (p.nodes[1].area == iap.area1))
                        || ((p.nodes[1].area == iap.area0) && (p.nodes[0].area == iap.area1)))) {
                    break;
                }
            }

            if (i != numInterAreaPortals) {
                continue;	// already emited
            }

            iap = interAreaPortals[numInterAreaPortals];
            numInterAreaPortals++;
            if (side.planenum == p.onnode.planenum) {
                iap.area0 = p.nodes[0].area;
                iap.area1 = p.nodes[1].area;
            } else {
                iap.area0 = p.nodes[1].area;
                iap.area1 = p.nodes[0].area;
            }
            iap.side = side;

        }
    }

    /*
     =============
     FloodAreas

     Mark each leaf with an area, bounded by CONTENTS_AREAPORTAL
     Sets e.areas.numAreas
     =============
     */
    static void FloodAreas(uEntity_t e) {
        common.Printf("--- FloodAreas ---\n");

        // set all areas to -1
        ClearAreas_r(e.tree.headnode);

        // flood fill from non-opaque areas
        c_areas = 0;
        FindAreas_r(e.tree.headnode);

        common.Printf("%5d areas\n", c_areas);
        e.numAreas = c_areas;

        // make sure we got all of them
        CheckAreas_r(e.tree.headnode);

        // identify all portals between areas if this is the world
        if (e.equals(dmapGlobals.uEntities[0])) {
            numInterAreaPortals = 0;
            FindInterAreaPortals_r(e.tree.headnode);
        }
    }

    /*
     ======================================================

     FILL OUTSIDE

     ======================================================
     */
    static void FillOutside_r(node_s node) {
        if (node.planenum != PLANENUM_LEAF) {
            FillOutside_r(node.children[0]);
            FillOutside_r(node.children[1]);
            return;
        }

        // anything not reachable by an entity
        // can be filled away
        if (NOT(node.occupied)) {
            if (!node.opaque) {
                c_outside++;
                node.opaque = true;
            } else {
                c_solid++;
            }
        } else {
            c_inside++;
        }

    }

    /*
     =============
     FillOutside

     Fill (set node.opaque = true) all nodes that can't be reached by entities
     =============
     */
    static void FillOutside(uEntity_t e) {
        c_outside = 0;
        c_inside = 0;
        c_solid = 0;
        common.Printf("--- FillOutside ---\n");
        FillOutside_r(e.tree.headnode);
        common.Printf("%5d solid leafs\n", c_solid);
        common.Printf("%5d leafs filled\n", c_outside);
        common.Printf("%5d inside leafs\n", c_inside);
    }
}
