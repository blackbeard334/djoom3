package neo.idlib.geometry;

import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Surface.idSurface;
import neo.idlib.math.Curve.idCurve_NURBS;
import neo.idlib.math.Curve.idCurve_Spline;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.open.ColorUtil;
import neo.idlib.math.Matrix.idMat3;

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
            this.spline = null;
            this.sweptSpline = null;
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
            final idCurve_NURBS<idVec4> nurbs = new idCurve_NURBS<>(idVec4.class);
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
            this.sweptSpline = nurbs;
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
            final idMat3 splineMat = new idMat3();

            if ((null == this.spline) || (null == this.sweptSpline)) {
                super.Clear();
                return;
            }

            this.verts.SetNum(splineSubdivisions * sweptSplineSubdivisions, false);

            // calculate the points and first derivatives for the swept spline
            totalTime = (this.sweptSpline.GetTime(this.sweptSpline.GetNumValues() - 1) - this.sweptSpline.GetTime(0)) + this.sweptSpline.GetCloseTime();
            sweptSplineDiv = this.sweptSpline.GetBoundaryType() == idCurve_Spline.BT_CLOSED ? sweptSplineSubdivisions : sweptSplineSubdivisions - 1;
            baseOffset = (splineSubdivisions - 1) * sweptSplineSubdivisions;
            for (i = 0; i < sweptSplineSubdivisions; i++) {
                t = (totalTime * i) / sweptSplineDiv;
                splinePos = this.sweptSpline.GetCurrentValue(t);
                splineD1 = this.sweptSpline.GetCurrentFirstDerivative(t);
                this.verts.oGet(baseOffset + i).xyz = splinePos.ToVec3();
                this.verts.oGet(baseOffset + i).st.oSet(0, splinePos.w);
                this.verts.oGet(baseOffset + i).tangents[0] = splineD1.ToVec3();
            }

            // sweep the spline
            totalTime = (this.spline.GetTime(this.spline.GetNumValues() - 1) - this.spline.GetTime(0)) + this.spline.GetCloseTime();
            splineDiv = this.spline.GetBoundaryType() == idCurve_Spline.BT_CLOSED ? splineSubdivisions : splineSubdivisions - 1;
            splineMat.Identity();
            for (i = 0; i < splineSubdivisions; i++) {
                t = (totalTime * i) / splineDiv;

                splinePos = this.spline.GetCurrentValue(t);
                splineD1 = this.spline.GetCurrentFirstDerivative(t);

                GetFrame(splineMat, splineD1.ToVec3(), splineMat);

                offset = i * sweptSplineSubdivisions;
                for (j = 0; j < sweptSplineSubdivisions; j++) {
                    final idDrawVert v = this.verts.oGet(offset + j);
                    v.xyz = splinePos.ToVec3().oPlus(this.verts.oGet(baseOffset + j).xyz.oMultiply(splineMat));
                    v.st.oSet(0, this.verts.oGet(baseOffset + j).st.oGet(0));
                    v.st.oSet(1, splinePos.w);
                    v.tangents[0] = this.verts.oGet(baseOffset + j).tangents[0].oMultiply(splineMat);
                    v.tangents[1] = splineD1.ToVec3();
                    v.normal = v.tangents[1].Cross(v.tangents[0]);
                    v.normal.Normalize();
                    ColorUtil.setElementsWith(v.getColor(), (byte) 0);
                }
            }

            this.indexes.SetNum(splineDiv * sweptSplineDiv * 2 * 3, false);

            // create indexes for the triangles
            for (offset = i = 0; i < splineDiv; i++) {

                i0 = (i + 0) * sweptSplineSubdivisions;
                i1 = ((i + 1) % splineSubdivisions) * sweptSplineSubdivisions;

                for (j = 0; j < sweptSplineDiv; j++) {

                    j0 = (j + 0);
                    j1 = (j + 1) % sweptSplineSubdivisions;

                    this.indexes.oSet(offset++, i0 + j0);
                    this.indexes.oSet(offset++, i0 + j1);
                    this.indexes.oSet(offset++, i1 + j1);

                    this.indexes.oSet(offset++, i1 + j1);
                    this.indexes.oSet(offset++, i1 + j0);
                    this.indexes.oSet(offset++, i0 + j0);
                }
            }

            GenerateEdgeIndexes();
        }

        //
        @Override
        public void Clear() {
            super.Clear();
//	delete spline;
            this.spline = null;
            this.spline = null;
//	delete sweptSpline;
            this.sweptSpline = null;
            this.sweptSpline = null;
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
            idVec3 d;
			final idVec3 v = new idVec3();
            final idMat3 axis = new idMat3();

            d = dir;
            d.Normalize();
            v.oSet(d.Cross(previousFrame.oGet(2)));
            v.Normalize();

            a = idMath.ACos(previousFrame.oGet(2).oMultiply(d)) * 0.5f;
            c = idMath.Cos(a);
            s = idMath.Sqrt(1.0f - (c * c));

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
    }
}
