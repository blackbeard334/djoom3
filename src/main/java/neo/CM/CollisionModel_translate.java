package neo.CM;

import neo.CM.CollisionModel_local.cm_edge_s;
import neo.CM.CollisionModel_local.cm_traceWork_s;
import neo.CM.CollisionModel_local.cm_vertex_s;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Pluecker.idPluecker;
import neo.idlib.math.Vector.idVec3;

import static neo.CM.CollisionModel.CM_CLIP_EPSILON;
import static neo.CM.CollisionModel.contactInfo_t;
import static neo.idlib.math.Math_h.FLOATSIGNBITNOTSET;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;

/**
 *
 */
public class CollisionModel_translate {
    /*
     ===============================================================================

     Trace model vs. polygonal model collision detection.

     ===============================================================================
     */
    /*
     ===============================================================================

     Collision detection for translational motion

     ===============================================================================
     */

    /*
     ================
     CM_AddContact
     ================
     */          private static int DBG_CM_AddContact = 0;
    static void CM_AddContact(cm_traceWork_s tw) {

        if (tw.numContacts >= tw.maxContacts) {
            return;
        }         DBG_CM_AddContact++;
        // copy contact information from trace_t
        tw.contacts[tw.numContacts] = new contactInfo_t(tw.trace.c);
        tw.numContacts++;
        // set fraction back to 1 to find all other contacts
        tw.trace.fraction = 1.0f;
    }

    /*
     ================
     CM_SetVertexSidedness

     stores for the given model vertex at which side of one of the trm edges it passes
     ================
     */
    static void CM_SetVertexSidedness(cm_vertex_s v, final idPluecker vpl, final idPluecker epl, final int bitNum) {
        if (0 == (v.sideSet & (1 << bitNum))) {
            float fl;
            fl = vpl.PermutedInnerProduct(epl);
            v.side = (v.side & ~(1 << bitNum)) | (FLOATSIGNBITSET(fl) << bitNum);
            v.sideSet |= (1 << bitNum);
        }
    }

    /*
     ================
     CM_SetEdgeSidedness

     stores for the given model edge at which side one of the trm vertices
     ================
     */
    static void CM_SetEdgeSidedness(cm_edge_s edge, final idPluecker vpl, final idPluecker epl, final int bitNum) {
        if (0 == (edge.sideSet & (1 << bitNum))) {
            float fl;
            fl = vpl.PermutedInnerProduct(epl);
            edge.side = (edge.side & ~(1 << bitNum)) | (FLOATSIGNBITSET(fl) << bitNum);
            edge.sideSet |= (1 << bitNum);
        }
    }

    /*
     ================
     CM_TranslationPlaneFraction
     ================
     */
    static float CM_TranslationPlaneFraction(idPlane plane, idVec3 start, idVec3 end) {
//        if (false) {
//            float d1, d2;
//
//            d2 = plane.Distance(end);
//            // if the end point is closer to the plane than an epsilon we still take it for a collision
//            if (d2 >= CM_CLIP_EPSILON) {
//                return 1.0f;
//            }
//            d1 = plane.Distance(start);
//
//            // if completely behind the polygon
//            if (d1 <= 0.0f) {
//                return 1.0f;
//            }
//            // leaves polygon
//            if (d1 <= d2) {
//                return 1.0f;
//            }
//            return (d1 - CM_CLIP_EPSILON) / (d1 - d2);
//
//        } else 
        {

            float d1, d2, d2eps;

            d2 = plane.Distance(end);
            // if the end point is closer to the plane than an epsilon we still take it for a collision
            // if ( d2 >= CM_CLIP_EPSILON ) {
            d2eps = d2 - CM_CLIP_EPSILON;
            if (FLOATSIGNBITNOTSET(d2eps) != 0) {
                return 1.0f;
            }
            d1 = plane.Distance(start);

            // if completely behind the polygon
            if (FLOATSIGNBITSET(d1) != 0) {
                return 1.0f;
            }
            // if going towards the front of the plane and
            // the start and end point are not at equal distance from the plane
            // if ( d1 > d2 )
            d2 = d1 - d2;
            if (d2 <= 0.0f) {
                return 1.0f;
            }
            return (d1 - CM_CLIP_EPSILON) / d2;
        }
    }
}
