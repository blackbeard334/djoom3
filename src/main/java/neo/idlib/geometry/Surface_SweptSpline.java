package neo.idlib.geometry;

import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Surface.idSurface;
import neo.idlib.math.Curve.idCurve_NURBS;
import neo.idlib.math.Curve.idCurve_Spline;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Surface_SweptSpline {

    /*
     ===============================================================================

     Swept Spline surface.

     ===============================================================================
     */
    class idSurface_SweptSpline extends idSurface {

        public idSurface_SweptSpline() {
            spline = null;
            sweptSpline = null;
        }
//	public						~idSurface_SweptSpline( void );
//

        public void SetSpline(idCurve_Spline<idVec4> spline) {
//            if (null != this.spline) {
////		delete this->spline;
//                this.spline = null;
//            }
            this.spline = spline;
        }

        public void SetSweptSpline(idCurve_Spline<idVec4> sweptSpline) {
//            if (null != this.sweptSpline) {
////		delete this->sweptSpline;
//                this.sweptSpline = null;
//            }
            this.sweptSpline = sweptSpline;
        }

        /*
         ====================
         idSurface_SweptSpline::SetSweptCircle

         Sets the swept spline to a NURBS circle.
         ====================
         */
        public void SetSweptCircle(final float radius) {
            idCurve_NURBS<idVec4> nurbs = new idCurve_NURBS<>();
            nurbs.Clear();
            nurbs.AddValue(0.0f, new idVec4(radius, radius, 0.0f, 0.00f));
            nurbs.AddValue(100.0f, new idVec4(-radius, radius, 0.0f, 0.25f));
            nurbs.AddValue(200.0f, new idVec4(-radius, -radius, 0.0f, 0.50f));
            nurbs.AddValue(300.0f, new idVec4(radius, -radius, 0.0f, 0.75f));
            nurbs.SetBoundaryType(idCurve_NURBS.BT_CLOSED);
            nurbs.SetCloseTime(100.0f);
//            if (null != sweptSpline) {
////		delete sweptSpline;
//                sweptSpline = null;
//            }
            sweptSpline = nurbs;
        }
//

        /*
         ====================
         idSurface_SweptSpline::Tessellate

         tesselate the surface
         ====================
         */
        public void Tessellate(final int splineSubdivisions, final int sweptSplineSubdivisions) {
            int i, j, offset, baseOffset, splineDiv, sweptSplineDiv;
            int i0, i1, j0, j1;
            float totalTime, t;
            idVec4 splinePos, splineD1;
            idMat3 splineMat = new idMat3();

            if (null == spline || null == sweptSpline) {
                super.Clear();
                return;
            }

            verts.SetNum(splineSubdivisions * sweptSplineSubdivisions, false);

            // calculate the points and first derivatives for the swept spline
            totalTime = sweptSpline.GetTime(sweptSpline.GetNumValues() - 1) - sweptSpline.GetTime(0) + sweptSpline.GetCloseTime();
            sweptSplineDiv = sweptSpline.GetBoundaryType() == idCurve_Spline.BT_CLOSED ? sweptSplineSubdivisions : sweptSplineSubdivisions - 1;
            baseOffset = (splineSubdivisions - 1) * sweptSplineSubdivisions;
            for (i = 0; i < sweptSplineSubdivisions; i++) {
                t = totalTime * i / sweptSplineDiv;
                splinePos = sweptSpline.GetCurrentValue(t);
                splineD1 = sweptSpline.GetCurrentFirstDerivative(t);
                verts.oGet(baseOffset + i).xyz = splinePos.ToVec3();
                verts.oGet(baseOffset + i).st.oSet(0, splinePos.w);
                verts.oGet(baseOffset + i).tangents[0] = splineD1.ToVec3();
            }

            // sweep the spline
            totalTime = spline.GetTime(spline.GetNumValues() - 1) - spline.GetTime(0) + spline.GetCloseTime();
            splineDiv = spline.GetBoundaryType() == idCurve_Spline.BT_CLOSED ? splineSubdivisions : splineSubdivisions - 1;
            splineMat.Identity();
            for (i = 0; i < splineSubdivisions; i++) {
                t = totalTime * i / splineDiv;

                splinePos = spline.GetCurrentValue(t);
                splineD1 = spline.GetCurrentFirstDerivative(t);

                GetFrame(splineMat, splineD1.ToVec3(), splineMat);

                offset = i * sweptSplineSubdivisions;
                for (j = 0; j < sweptSplineSubdivisions; j++) {
                    idDrawVert v = verts.oGet(offset + j);
                    v.xyz = splinePos.ToVec3().oPlus(verts.oGet(baseOffset + j).xyz.oMultiply(splineMat));
                    v.st.oSet(0, verts.oGet(baseOffset + j).st.oGet(0));
                    v.st.oSet(1, splinePos.w);
                    v.tangents[0] = verts.oGet(baseOffset + j).tangents[0].oMultiply(splineMat);
                    v.tangents[1] = splineD1.ToVec3();
                    v.normal = v.tangents[1].Cross(v.tangents[0]);
                    v.normal.Normalize();
                    v.color[0] = v.color[1] = v.color[2] = v.color[3] = 0;
                }
            }

            indexes.SetNum(splineDiv * sweptSplineDiv * 2 * 3, false);

            // create indexes for the triangles
            for (offset = i = 0; i < splineDiv; i++) {

                i0 = (i + 0) * sweptSplineSubdivisions;
                i1 = (i + 1) % splineSubdivisions * sweptSplineSubdivisions;

                for (j = 0; j < sweptSplineDiv; j++) {

                    j0 = (j + 0);
                    j1 = (j + 1) % sweptSplineSubdivisions;

                    indexes.oSet(offset++, i0 + j0);
                    indexes.oSet(offset++, i0 + j1);
                    indexes.oSet(offset++, i1 + j1);

                    indexes.oSet(offset++, i1 + j1);
                    indexes.oSet(offset++, i1 + j0);
                    indexes.oSet(offset++, i0 + j0);
                }
            }

            GenerateEdgeIndexes();
        }

//
        @Override
        public void Clear() {
            super.Clear();
//	delete spline;
            spline = null;
            spline = null;
//	delete sweptSpline;
            sweptSpline = null;
            sweptSpline = null;
        }
//
        protected idCurve_Spline<idVec4> spline;
        protected idCurve_Spline<idVec4> sweptSpline;
//

        protected void GetFrame(final idMat3 previousFrame, final idVec3 dir, idMat3 newFrame) {
            float wx, wy, wz;
            float xx, yy, yz;
            float xy, xz, zz;
            float x2, y2, z2;
            float a, c, s, x, y, z;
            idVec3 d, v = new idVec3();
            idMat3 axis = new idMat3();

            d = dir;
            d.Normalize();
            v.oSet(d.Cross(previousFrame.oGet(2)));
            v.Normalize();

            a = idMath.ACos(previousFrame.oGet(2).oMultiply(d)) * 0.5f;
            c = idMath.Cos(a);
            s = idMath.Sqrt(1.0f - c * c);

            x = v.oGet(0) * s;
            y = v.oGet(1) * s;
            z = v.oGet(2) * s;

            x2 = x + x;
            y2 = y + y;
            z2 = z + z;
            xx = x * x2;
            xy = x * y2;
            xz = x * z2;
            yy = y * y2;
            yz = y * z2;
            zz = z * z2;
            wx = c * x2;
            wy = c * y2;
            wz = c * z2;

            axis.oSet(0, 0, 1.0f - (yy + zz));
            axis.oSet(0, 1, xy - wz);
            axis.oSet(0, 2, xz + wy);
            axis.oSet(1, 0, xy + wz);
            axis.oSet(1, 1, 1.0f - (xx + zz));
            axis.oSet(1, 2, yz - wx);
            axis.oSet(2, 0, xz - wy);
            axis.oSet(2, 1, yz + wx);
            axis.oSet(2, 2, 1.0f - (xx + yy));

            newFrame.oSet(previousFrame.oMultiply(axis));

            newFrame.setRow(2, dir);
            newFrame.oGet(2).Normalize();//TODO:check if this normalizes back ref
            newFrame.setRow(1, newFrame.oGet(1).Cross(newFrame.oGet(2), newFrame.oGet(0)));
            newFrame.oGet(1).Normalize();
            newFrame.setRow(0, newFrame.oGet(0).Cross(newFrame.oGet(1), newFrame.oGet(2)));
            newFrame.oGet(0).Normalize();
        }
    };
}
