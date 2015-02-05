package neo.Tools.Compilers.AAS;

import neo.Tools.Compilers.AAS.BrushBSP.idBrushBSPNode;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Plane.ON_EPSILON;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import static neo.idlib.math.Vector.vec3_origin;

/**
 *
 */
public class AASBuild_ledge {

    static final float LEDGE_EPSILON = 0.1f;

    //===============================================================
    //
    //	idLedge
    //
    //===============================================================
    static class idLedge {

        public idVec3         start;
        public idVec3         end;
        public idBrushBSPNode node;
        public int            numExpandedPlanes;
        public int            numSplitPlanes;
        public int            numPlanes;
        public idPlane[] planes = new idPlane[8];
        //
        //

        public idLedge() {
        }

        public idLedge(final idVec3 v1, final idVec3 v2, final idVec3 gravityDir, idBrushBSPNode n) {
            start = v1;
            end = v2;
            node = n;
            numPlanes = 4;
            planes[0].SetNormal((v1.oMinus(v2)).Cross(gravityDir));
            planes[0].Normalize();
            planes[0].FitThroughPoint(v1);
            planes[1].SetNormal((v1.oMinus(v2)).Cross(planes[0].Normal()));
            planes[1].Normalize();
            planes[1].FitThroughPoint(v1);
            planes[2].SetNormal(v1.oMinus(v2));
            planes[2].Normalize();
            planes[2].FitThroughPoint(v1);
            planes[3].SetNormal(v2.oMinus(v1));
            planes[3].Normalize();
            planes[3].FitThroughPoint(v2);
        }

        public void AddPoint(final idVec3 v) {
            if (planes[2].Distance(v) > 0.0f) {
                start = v;
                planes[2].FitThroughPoint(start);
            }
            if (planes[3].Distance(v) > 0.0f) {
                end = v;
                planes[3].FitThroughPoint(end);
            }
        }

        /*
         ============
         idLedge::CreateBevels

         NOTE: this assumes the gravity is vertical
         ============
         */
        public void CreateBevels(final idVec3 gravityDir) {
            int i, j;
            idBounds bounds = new idBounds();
            idVec3 size, normal;

            bounds.Clear();
            bounds.AddPoint(start);
            bounds.AddPoint(end);
            size = bounds.oGet(1).oMinus(bounds.oGet(0));

            // plane through ledge
            planes[0].SetNormal((start.oMinus(end)).Cross(gravityDir));
            planes[0].Normalize();
            planes[0].FitThroughPoint(start);
            // axial bevels at start and end point
            i = size.oGet(1) > size.oGet(0) ? 1 : 0;
            normal = vec3_origin;
            normal.oSet(i, 1.0f);
            j = end.oGet(i) > start.oGet(i) ? 1 : 0;
            planes[1 + j].SetNormal(normal);
            planes[1 +/*!j*/ (1 ^ j)].SetNormal(normal.oNegative());
            planes[1].FitThroughPoint(start);
            planes[2].FitThroughPoint(end);
            numExpandedPlanes = 3;
            // if additional bevels are required
            if (idMath.Fabs(size.oGet(/*!i*/1 ^ i)) > 0.01f) {
                normal = vec3_origin;
                normal.oSet(/*!i]*/1 ^ i, 1.0f);
                j = end.oGet(/*!i]*/1 ^ i) > start.oGet(/*!i]*/1 ^ i) ? 1 : 0;
                planes[3 + j].SetNormal(normal);
                planes[3 +/*!j]*/ 1 ^ j].SetNormal(normal.oNegative());
                planes[3].FitThroughPoint(start);
                planes[4].FitThroughPoint(end);
                numExpandedPlanes = 5;
            }
            // opposite of first
            planes[numExpandedPlanes + 0] = planes[0].oNegative();
            // number of planes used for splitting
            numSplitPlanes = numExpandedPlanes + 1;
            // top plane
            planes[numSplitPlanes + 0].SetNormal((start.oMinus(end)).Cross(planes[0].Normal()));
            planes[numSplitPlanes + 0].Normalize();
            planes[numSplitPlanes + 0].FitThroughPoint(start);
            // bottom plane
            planes[numSplitPlanes + 1] = planes[numSplitPlanes + 0].oNegative();
            // total number of planes
            numPlanes = numSplitPlanes + 2;
        }

        public void Expand(final idBounds bounds, float maxStepHeight) {
            int i, j;
            idVec3 v = new idVec3();

            for (i = 0; i < numExpandedPlanes; i++) {

                for (j = 0; j < 3; j++) {
                    if (planes[i].Normal().oGet(j) > 0.0f) {
                        v.oSet(j, bounds.oGet(0, j));
                    } else {
                        v.oSet(j, bounds.oGet(1, j));
                    }
                }

                planes[i].SetDist(planes[i].Dist() + v.oMultiply(planes[i].Normal().oNegative()));
            }

            planes[numSplitPlanes + 0].SetDist(planes[numSplitPlanes + 0].Dist() + maxStepHeight);
            planes[numSplitPlanes + 1].SetDist(planes[numSplitPlanes + 1].Dist() + 1.0f);
        }

        public idWinding ChopWinding(final idWinding winding) {
            int i;
            idWinding w;

            w = winding.Copy();
            for (i = 0; i < numPlanes && w != null; i++) {
                w = w.Clip(planes[i].oNegative(), ON_EPSILON, true);
            }
            return w;
        }

        public boolean PointBetweenBounds(final idVec3 v) {
            return (planes[2].Distance(v) < LEDGE_EPSILON) && (planes[3].Distance(v) < LEDGE_EPSILON);
        }
    };
}
