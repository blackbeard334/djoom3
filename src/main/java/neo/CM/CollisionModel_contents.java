package neo.CM;

import neo.CM.CollisionModel_local.cm_edge_s;
import neo.CM.CollisionModel_local.cm_vertex_s;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Pluecker.idPluecker;

/**
 *
 */
public class CollisionModel_contents {

    /*
     ===============================================================================

     Contents test

     ===============================================================================
     */
    /*
     ================
     CM_SetTrmEdgeSidedness
     ================
     */
    static void CM_SetTrmEdgeSidedness(cm_edge_s edge, final idPluecker bpl, final idPluecker epl, final int bitNum) {
        if (0 == (edge.sideSet & (1 << bitNum))) {
            float fl;
            fl = bpl.PermutedInnerProduct(epl);
            edge.side = (edge.side & ~(1 << bitNum)) | (FLOATSIGNBITSET(fl) << bitNum);
            edge.sideSet |= (1 << bitNum);
        }
    }

    /*
     ================
     CM_SetTrmPolygonSidedness
     ================
     */
    static void CM_SetTrmPolygonSidedness(cm_vertex_s v, final idPlane plane, final int bitNum) {
        if (0 == (v.sideSet & (1 << bitNum))) {
            float fl;
            fl = plane.Distance(v.p);
            /* cannot use float sign bit because it is undetermined when fl == 0.0f */
            if (fl < 0.0f) {
                v.side |= (1 << bitNum);
            } else {
                v.side &= ~(1 << bitNum);
            }
            v.sideSet |= (1 << bitNum);
        }
    }
}
