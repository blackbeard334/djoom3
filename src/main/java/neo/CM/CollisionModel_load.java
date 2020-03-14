package neo.CM;

import static neo.CM.CollisionModel_local.MAX_NODE_POLYGONS;
import static neo.CM.CollisionModel_local.MIN_NODE_SIZE;

import neo.CM.CollisionModel_local.cm_brushRef_s;
import neo.CM.CollisionModel_local.cm_node_s;
import neo.CM.CollisionModel_local.cm_polygonRef_s;
import neo.idlib.MapFile.idMapBrush;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapPatch;
import neo.idlib.MapFile.idMapPrimitive;
import neo.idlib.BV.Bounds.idBounds;

/**
 *
 */
public class CollisionModel_load {

    /*
     ===============================================================================

     Spatial subdivision

     ===============================================================================
     */

    /*
     ================
     CM_FindSplitter
     ================
     */
    static boolean CM_FindSplitter(final cm_node_s node, final idBounds bounds, int[] planeType, float[] planeDist) {
        int i, j, type, polyCount;
        int[] axis = new int[3];
        float dist, t, bestt;
        float[] size = new float[3];
        cm_brushRef_s bref;
        cm_polygonRef_s pref;
        cm_node_s n;
        boolean forceSplit = false;

        for (i = 0; i < 3; i++) {
            size[i] = bounds.oGet(1, i) - bounds.oGet(0, i);
            axis[i] = i;
        }
        // sort on largest axis
        for (i = 0; i < 2; i++) {
            if (size[i] < size[i + 1]) {
                t = size[i];
                size[i] = size[i + 1];
                size[i + 1] = t;
                j = axis[i];
                axis[i] = axis[i + 1];
                axis[i + 1] = j;
                i = -1;
            }
        }
        // if the node is too small for further splits
        if (size[0] < MIN_NODE_SIZE) {
            polyCount = 0;
            for (pref = node.polygons; pref != null; pref = pref.next) {
                polyCount++;
            }
            if (polyCount > MAX_NODE_POLYGONS) {
                forceSplit = true;
            }
        }
        // find an axial aligned splitter
        for (i = 0; i < 3; i++) {
            // start with the largest axis first
            type = axis[i];
            bestt = size[i];
            // if the node is small anough in this axis direction
            if (!forceSplit && bestt < MIN_NODE_SIZE) {
                break;
            }
            // find an axial splitter from the brush bounding boxes
            // also try brushes from parent nodes
            for (n = node; n != null; n = n.parent) {
                for (bref = n.brushes; bref != null; bref = bref.next) {
                    for (j = 0; j < 2; j++) {
                        dist = bref.b.bounds.oGet(j, type);
                        // if the splitter is already used or outside node bounds
                        if (dist >= bounds.oGet(1, type) || dist <= bounds.oGet(0, type)) {
                            continue;
                        }
                        // find the most centered splitter
                        t = Math.abs((bounds.oGet(1, type) - dist) - (dist - bounds.oGet(0, type)));
                        if (t < bestt) {
                            bestt = t;
                            planeType[0] = type;
                            planeDist[0] = dist;
                        }
                    }
                }
            }
            // find an axial splitter from the polygon bounding boxes
            // also try brushes from parent nodes
            for (n = node; n != null; n = n.parent) {
                for (pref = n.polygons; pref != null; pref = pref.next) {
                    for (j = 0; j < 2; j++) {
                        dist = pref.p.bounds.oGet(j, type);
                        // if the splitter is already used or outside node bounds
                        if (dist >= bounds.oGet(1, type) || dist <= bounds.oGet(0, type)) {
                            continue;
                        }
                        // find the most centered splitter
                        t = Math.abs((bounds.oGet(1, type) - dist) - (dist - bounds.oGet(0, type)));
                        if (t < bestt) {
                            bestt = t;
                            planeType[0] = type;
                            planeDist[0] = dist;
                        }
                    }
                }
            }
            // if we found a splitter on the largest axis
            if (bestt < size[i]) {
                // if forced split due to lots of polygons
                if (forceSplit) {
                    return true;
                }
                // don't create splitters real close to the bounds
                if (bounds.oGet(1, type) - planeDist[0] > (MIN_NODE_SIZE * 0.5f)
                        && planeDist[0] - bounds.oGet(0, type) > (MIN_NODE_SIZE * 0.5f)) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     ================
     CM_R_InsideAllChildren
     ================
     */
    static boolean CM_R_InsideAllChildren(cm_node_s node, final idBounds bounds) {
        assert (node != null);
        if (node.planeType != -1) {
            if (bounds.oGet(0, node.planeType) >= node.planeDist) {
                return false;
            }
            if (bounds.oGet(1, node.planeType) <= node.planeDist) {
                return false;
            }
            if (!CM_R_InsideAllChildren(node.children[0], bounds)) {
                return false;
            }
            if (!CM_R_InsideAllChildren(node.children[1], bounds)) {
                return false;
            }
        }
        return true;
    }

    /*
     ===============================================================================

     Raw polygon and brush data

     ===============================================================================
     */
    /*
     =================
     CM_EstimateVertsAndEdges
     =================
     */
    static void CM_EstimateVertsAndEdges(final idMapEntity mapEnt, int[] numVerts, int[] numEdges) {
        int j, width, height;

        numVerts[0] = numEdges[0] = 0;
        for (j = 0; j < mapEnt.GetNumPrimitives(); j++) {
            final idMapPrimitive mapPrim;
            mapPrim = mapEnt.GetPrimitive(j);
            if (mapPrim.GetType() == idMapPrimitive.TYPE_PATCH) {
                // assume maximum tesselation without adding verts
                width = ((idMapPatch) mapPrim).GetWidth();
                height = ((idMapPatch) mapPrim).GetHeight();
                numVerts[0] += width * height;
                numEdges[0] += (width - 1) * height + width * (height - 1) + (width - 1) * (height - 1);
                continue;
            }
            if (mapPrim.GetType() == idMapPrimitive.TYPE_BRUSH) {
                // assume cylinder with a polygon with (numSides - 2) edges ontop and on the bottom
                numVerts[0] += (((idMapBrush) mapPrim).GetNumSides() - 2) * 2;
                numEdges[0] += (((idMapBrush) mapPrim).GetNumSides() - 2) * 3;
//                continue;
            }
        }
    }

    /*
     ================
     CM_CountNodeBrushes
     ================
     */
    static int CM_CountNodeBrushes(final cm_node_s node) {
        int count;
        cm_brushRef_s bref;

        count = 0;
        for (bref = node.brushes; bref != null; bref = bref.next) {
            count++;
        }
        return count;
    }

    /*
     ================
     CM_R_GetModelBounds
     ================
     */
    static void CM_R_GetNodeBounds(idBounds bounds, cm_node_s node) {
        cm_polygonRef_s pref;
        cm_brushRef_s bref;

        while (true) {
            for (pref = node.polygons; pref != null; pref = pref.next) {
                bounds.AddPoint(pref.p.bounds.oGet(0));
                bounds.AddPoint(pref.p.bounds.oGet(1));
            }
            for (bref = node.brushes; bref != null; bref = bref.next) {
                bounds.AddPoint(bref.b.bounds.oGet(0));
                bounds.AddPoint(bref.b.bounds.oGet(1));
            }
            if (node.planeType == -1) {
                break;
            }
            CM_R_GetNodeBounds(bounds, node.children[1]);
            node = node.children[0];
        }
    }

    /*
     ================
     CM_GetNodeBounds
     ================
     */
    static void CM_GetNodeBounds(idBounds bounds, cm_node_s node) {
        bounds.Clear();
        CM_R_GetNodeBounds(bounds, node);
        if (bounds.IsCleared()) {
            bounds.Zero();
        }
    }

    /*
     ================
     CM_GetNodeContents
     ================
     */
    static int CM_GetNodeContents(cm_node_s node) {
        int contents;
        cm_polygonRef_s pref;
        cm_brushRef_s bref;

        contents = 0;
        while (true) {
            for (pref = node.polygons; pref != null; pref = pref.next) {
                contents |= pref.p.contents;
            }
            for (bref = node.brushes; bref != null; bref = bref.next) {
                contents |= bref.b.contents;
            }
            if (node.planeType == -1) {
                break;
            }
            contents |= CM_GetNodeContents(node.children[1]);
            node = node.children[0];
        }
        return contents;
    }
}
