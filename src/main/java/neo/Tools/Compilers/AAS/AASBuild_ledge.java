package neo.Tools.Compilers.AAS;

import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Vector.getVec3_origin;

import neo.Tools.Compilers.AAS.BrushBSP.idBrushBSPNode;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

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
            this.start = v1;
            this.end = v2;
            this.node = n;
            this.numPlanes = 4;
            this.planes[0].SetNormal((v1.oMinus(v2)).Cross(gravityDir));
            this.planes[0].Normalize();
            this.planes[0].FitThroughPoint(v1);
            this.planes[1].SetNormal((v1.oMinus(v2)).Cross(this.planes[0].Normal()));
            this.planes[1].Normalize();
            this.planes[1].FitThroughPoint(v1);
            this.planes[2].SetNormal(v1.oMinus(v2));
            this.planes[2].Normalize();
            this.planes[2].FitThroughPoint(v1);
            this.planes[3].SetNormal(v2.oMinus(v1));
            this.planes[3].Normalize();
            this.planes[3].FitThroughPoint(v2);
        }

        public void AddPoint(final idVec3 v) {
            if (this.planes[2].Distance(v) > 0.0f) {
                this.start = v;
                this.planes[2].FitThroughPoint(this.start);
            }
            if (this.planes[3].Distance(v) > 0.0f) {
                this.end = v;
                this.planes[3].FitThroughPoint(this.end);
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
            final idBounds bounds = new idBounds();
            idVec3 size, normal;

            bounds.Clear();
            bounds.AddPoint(this.start);
            bounds.AddPoint(this.end);
            size = bounds.oGet(1).oMinus(bounds.oGet(0));

            // plane through ledge
            this.planes[0].SetNormal((this.start.oMinus(this.end)).Cross(gravityDir));
            this.planes[0].Normalize();
            this.planes[0].FitThroughPoint(this.start);
            // axial bevels at start and end point
            i = size.oGet(1) > size.oGet(0) ? 1 : 0;
            normal = getVec3_origin();
            normal.oSet(i, 1.0f);
            j = this.end.oGet(i) > this.start.oGet(i) ? 1 : 0;
            this.planes[1 + j].SetNormal(normal);
            this.planes[1 +/*!j*/ (1 ^ j)].SetNormal(normal.oNegative());
            this.planes[1].FitThroughPoint(this.start);
            this.planes[2].FitThroughPoint(this.end);
            this.numExpandedPlanes = 3;
            // if additional bevels are required
            if (idMath.Fabs(size.oGet(/*!i*/1 ^ i)) > 0.01f) {
                normal = getVec3_origin();
                normal.oSet(/*!i]*/1 ^ i, 1.0f);
                j = this.end.oGet(/*!i]*/1 ^ i) > this.start.oGet(/*!i]*/1 ^ i) ? 1 : 0;
                this.planes[3 + j].SetNormal(normal);
                this.planes[(3 +/*!j]*/ 1) ^ j].SetNormal(normal.oNegative());
                this.planes[3].FitThroughPoint(this.start);
                this.planes[4].FitThroughPoint(this.end);
                this.numExpandedPlanes = 5;
            }
            // opposite of first
            this.planes[this.numExpandedPlanes + 0] = this.planes[0].oNegative();
            // number of planes used for splitting
            this.numSplitPlanes = this.numExpandedPlanes + 1;
            // top plane
            this.planes[this.numSplitPlanes + 0].SetNormal((this.start.oMinus(this.end)).Cross(this.planes[0].Normal()));
            this.planes[this.numSplitPlanes + 0].Normalize();
            this.planes[this.numSplitPlanes + 0].FitThroughPoint(this.start);
            // bottom plane
            this.planes[this.numSplitPlanes + 1] = this.planes[this.numSplitPlanes + 0].oNegative();
            // total number of planes
            this.numPlanes = this.numSplitPlanes + 2;
        }

        public void Expand(final idBounds bounds, float maxStepHeight) {
            int i, j;
            final idVec3 v = new idVec3();

            for (i = 0; i < this.numExpandedPlanes; i++) {

                for (j = 0; j < 3; j++) {
                    if (this.planes[i].Normal().oGet(j) > 0.0f) {
                        v.oSet(j, bounds.oGet(0, j));
                    } else {
                        v.oSet(j, bounds.oGet(1, j));
                    }
                }

                this.planes[i].SetDist(this.planes[i].Dist() + v.oMultiply(this.planes[i].Normal().oNegative()));
            }

            this.planes[this.numSplitPlanes + 0].SetDist(this.planes[this.numSplitPlanes + 0].Dist() + maxStepHeight);
            this.planes[this.numSplitPlanes + 1].SetDist(this.planes[this.numSplitPlanes + 1].Dist() + 1.0f);
        }

        public idWinding ChopWinding(final idWinding winding) {
            int i;
            idWinding w;

            w = winding.Copy();
            for (i = 0; (i < this.numPlanes) && (w != null); i++) {
                w = w.Clip(this.planes[i].oNegative(), ON_EPSILON, true);
            }
            return w;
        }

        public boolean PointBetweenBounds(final idVec3 v) {
            return (this.planes[2].Distance(v) < LEDGE_EPSILON) && (this.planes[3].Distance(v) < LEDGE_EPSILON);
        }
    }
}
