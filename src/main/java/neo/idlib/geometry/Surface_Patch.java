package neo.idlib.geometry;

import neo.idlib.Dict_h.idDict;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.MapFile.idMapPrimitive;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Surface.idSurface;
import static neo.idlib.math.Math_h.Square;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Surface_Patch {

    /*
     ===============================================================================

     Bezier patch surface.

     ===============================================================================
     */
    public static class idSurface_Patch extends idSurface {

        protected int width;			// width of patch
        protected int height;			// height of patch
        protected int maxWidth;                 // maximum width allocated for
        protected int maxHeight;		// maximum height allocated for
        protected boolean expanded;		// true if vertices are spaced out
        //
        //

        public idSurface_Patch() {
            height = width = maxHeight = maxWidth = 0;
            expanded = false;
        }

        public idSurface_Patch(int maxPatchWidth, int maxPatchHeight) {
            width = height = 0;
            maxWidth = maxPatchWidth;
            maxHeight = maxPatchHeight;
            verts.SetNum(maxWidth * maxHeight);
            expanded = false;
        }

        public idSurface_Patch(final idSurface_Patch patch) {
            this.oSet(patch);
        }

        public idSurface_Patch(final idMapPrimitive patch) {
            this.oSet(patch);
        }
//public						~idSurface_Patch( void );
//

        public void SetSize(int patchWidth, int patchHeight) throws Exception {
            if (patchWidth < 1 || patchWidth > maxWidth) {
                idLib.common.FatalError("idSurface_Patch::SetSize: invalid patchWidth");
            }
            if (patchHeight < 1 || patchHeight > maxHeight) {
                idLib.common.FatalError("idSurface_Patch::SetSize: invalid patchHeight");
            }
            width = patchWidth;
            height = patchHeight;
            verts.SetNum(width * height, false);
        }

        public int GetWidth() {
            return width;
        }

        public int GetHeight() {
            return height;
        }

        public void Subdivide(float maxHorizontalError, float maxVerticalError, float maxLength) throws idException {
            this.Subdivide(maxHorizontalError, maxVerticalError, maxLength, false);
        }

        // subdivide the patch mesh based on error
        public void Subdivide(float maxHorizontalError, float maxVerticalError, float maxLength, boolean genNormals) throws idException {
            int i, j, k, l;
            idDrawVert prev = new idDrawVert(), next = new idDrawVert(), mid = new idDrawVert();
            idVec3 prevxyz = new idVec3(), nextxyz = new idVec3(), midxyz = new idVec3();
            idVec3 delta;
            float maxHorizontalErrorSqr, maxVerticalErrorSqr, maxLengthSqr;

            // generate normals for the control mesh
            if (genNormals) {
                GenerateNormals();
            }

            maxHorizontalErrorSqr = (float) Square(maxHorizontalError);
            maxVerticalErrorSqr = (float) Square(maxVerticalError);
            maxLengthSqr = (float) Square(maxLength);

            Expand();

            // horizontal subdivisions
            for (j = 0; j + 2 < width; j += 2) {
                // check subdivided midpoints against control points
                for (i = 0; i < height; i++) {
                    for (l = 0; l < 3; l++) {
                        prevxyz.oSet(1, verts.oGet(i * maxWidth + j + 1).xyz.oGet(l) - verts.oGet(i * maxWidth + j).xyz.oGet(l));
                        nextxyz.oSet(1, verts.oGet(i * maxWidth + j + 2).xyz.oGet(l) - verts.oGet(i * maxWidth + j + 1).xyz.oGet(l));
                        midxyz.oSet(1, (verts.oGet(i * maxWidth + j).xyz.oGet(l) + verts.oGet(i * maxWidth + j + 1).xyz.oGet(l) * 2.0f + verts.oGet(i * maxWidth + j + 2).xyz.oGet(l)) * 0.25f);
                    }

                    if (maxLength > 0.0f) {
                        // if the span length is too long, force a subdivision
                        if (prevxyz.LengthSqr() > maxLengthSqr || nextxyz.LengthSqr() > maxLengthSqr) {
                            break;
                        }
                    }
                    // see if this midpoint is off far enough to subdivide
                    delta = verts.oGet(i * maxWidth + j + 1).xyz.oMinus(midxyz);
                    if (delta.LengthSqr() > maxHorizontalErrorSqr) {
                        break;
                    }
                }

                if (i == height) {
                    continue;	// didn't need subdivision
                }

                if (width + 2 >= maxWidth) {
                    ResizeExpanded(maxHeight, maxWidth + 4);
                }

                // insert two columns and replace the peak
                width += 2;

                for (i = 0; i < height; i++) {
                    this.LerpVert(verts.oGet(i * maxWidth + j), verts.oGet(i * maxWidth + j + 1), prev);
                    this.LerpVert(verts.oGet(i * maxWidth + j + 1), verts.oGet(i * maxWidth + j + 2), next);
                    this.LerpVert(prev, next, mid);

                    for (k = width - 1; k > j + 3; k--) {
                        verts.oSet(i * maxWidth + k, verts.oGet(i * maxWidth + k - 2));
                    }
                    verts.oSet(i * maxWidth + j + 1, prev);
                    verts.oSet(i * maxWidth + j + 2, mid);
                    verts.oSet(i * maxWidth + j + 3, next);
                }

                // back up and recheck this set again, it may need more subdivision
                j -= 2;
            }

            // vertical subdivisions
            for (j = 0; j + 2 < height; j += 2) {
                // check subdivided midpoints against control points
                for (i = 0; i < width; i++) {
                    for (l = 0; l < 3; l++) {
                        prevxyz.oSet(1, verts.oGet((j + 1) * maxWidth + i).xyz.oGet(l) - verts.oGet(j * maxWidth + i).xyz.oGet(l));
                        nextxyz.oSet(1, verts.oGet((j + 2) * maxWidth + i).xyz.oGet(l) - verts.oGet((j + 1) * maxWidth + i).xyz.oGet(l));
                        midxyz.oSet(1, (verts.oGet(j * maxWidth + i).xyz.oGet(l) + verts.oGet((j + 1) * maxWidth + i).xyz.oGet(l) * 2.0f
                                + verts.oGet((j + 2) * maxWidth + i).xyz.oGet(l)) * 0.25f);
                    }

                    if (maxLength > 0.0f) {
                        // if the span length is too long, force a subdivision
                        if (prevxyz.LengthSqr() > maxLengthSqr || nextxyz.LengthSqr() > maxLengthSqr) {
                            break;
                        }
                    }
                    // see if this midpoint is off far enough to subdivide
                    delta = verts.oGet((j + 1) * maxWidth + i).xyz.oMinus(midxyz);
                    if (delta.LengthSqr() > maxVerticalErrorSqr) {
                        break;
                    }
                }

                if (i == width) {
                    continue;	// didn't need subdivision
                }

                if (height + 2 >= maxHeight) {
                    ResizeExpanded(maxHeight + 4, maxWidth);
                }

                // insert two columns and replace the peak
                height += 2;

                for (i = 0; i < width; i++) {
                    LerpVert(verts.oGet(j * maxWidth + i), verts.oGet((j + 1) * maxWidth + i), prev);
                    LerpVert(verts.oGet((j + 1) * maxWidth + i), verts.oGet((j + 2) * maxWidth + i), next);
                    LerpVert(prev, next, mid);

                    for (k = height - 1; k > j + 3; k--) {
                        verts.oSet(k * maxWidth + i, verts.oGet((k - 2) * maxWidth + i));
                    }
                    verts.oSet((j + 1) * maxWidth + i, prev);
                    verts.oSet((j + 2) * maxWidth + i, mid);
                    verts.oSet((j + 3) * maxWidth + i, next);
                }

                // back up and recheck this set again, it may need more subdivision
                j -= 2;
            }

            PutOnCurve();

            RemoveLinearColumnsRows();

            Collapse();

            // normalize all the lerped normals
            if (genNormals) {
                for (i = 0; i < width * height; i++) {
                    verts.oGet(i).normal.Normalize();
                }
            }

            GenerateIndexes();
        }

        public void SubdivideExplicit(int horzSubdivisions, int vertSubdivisions, boolean genNormals) throws idException {
            this.SubdivideExplicit(horzSubdivisions, vertSubdivisions, genNormals, false);
        }

        // subdivide the patch up to an explicit number of horizontal and vertical subdivisions
        public void SubdivideExplicit(int horzSubdivisions, int vertSubdivisions, boolean genNormals, boolean removeLinear) throws idException {
            int i, j, k, l;
            idDrawVert[][] sample = new idDrawVert[3][3];
            int outWidth = ((width - 1) / 2 * horzSubdivisions) + 1;
            int outHeight = ((height - 1) / 2 * vertSubdivisions) + 1;
            idDrawVert[] dv = new idDrawVert[outWidth * outHeight];

            // generate normals for the control mesh
            if (genNormals) {
                GenerateNormals();
            }

            int baseCol = 0;
            for (i = 0; i + 2 < width; i += 2) {
                int baseRow = 0;
                for (j = 0; j + 2 < height; j += 2) {
                    for (k = 0; k < 3; k++) {
                        for (l = 0; l < 3; l++) {
                            sample[k][l] = verts.oGet(((j + l) * width) + i + k);
                        }
                    }
                    SampleSinglePatch(sample, baseCol, baseRow, outWidth, horzSubdivisions, vertSubdivisions, dv);
                    baseRow += vertSubdivisions;
                }
                baseCol += horzSubdivisions;
            }
            verts.SetNum(outWidth * outHeight);
            for (i = 0; i < outWidth * outHeight; i++) {
                verts.oSet(i, dv[i]);
            }

//	delete[] dv;
            width = maxWidth = outWidth;
            height = maxHeight = outHeight;
            expanded = false;

            if (removeLinear) {
                Expand();
                RemoveLinearColumnsRows();
                Collapse();
            }

            // normalize all the lerped normals
            if (genNormals) {
                for (i = 0; i < width * height; i++) {
                    verts.oGet(i).normal.Normalize();
                }
            }

            GenerateIndexes();
        }

        /*
         =================
         idSurface_Patch::PutOnCurve

         Expects an expanded patch.
         =================
         */
        private void PutOnCurve() {// put the approximation points on the curve
            int i, j;
            idDrawVert prev = new idDrawVert(), next = new idDrawVert();

            assert (expanded == true);
            // put all the approximating points on the curve
            for (i = 0; i < width; i++) {
                for (j = 1; j < height; j += 2) {
                    LerpVert(verts.oGet(j * maxWidth + i), verts.oGet((j + 1) * maxWidth + i), prev);
                    LerpVert(verts.oGet(j * maxWidth + i), verts.oGet((j - 1) * maxWidth + i), next);
                    LerpVert(prev, next, verts.oGet(j * maxWidth + i));
                }
            }

            for (j = 0; j < height; j++) {
                for (i = 1; i < width; i += 2) {
                    LerpVert(verts.oGet(j * maxWidth + i), verts.oGet(j * maxWidth + i + 1), prev);
                    LerpVert(verts.oGet(j * maxWidth + i), verts.oGet(j * maxWidth + i - 1), next);
                    LerpVert(prev, next, verts.oGet(j * maxWidth + i));
                }
            }
        }
//						
//		

        /*
         ================
         idSurface_Patch::RemoveLinearColumnsRows

         Expects an expanded patch.
         ================
         */
        private void RemoveLinearColumnsRows() {// remove columns and rows with all points on one line{
            int i, j, k;
            float len, maxLength;
            idVec3 proj = new idVec3(), dir;

            assert (expanded == true);
            for (j = 1; j < width - 1; j++) {
                maxLength = 0;
                for (i = 0; i < height; i++) {
                    this.ProjectPointOntoVector(verts.oGet(i * maxWidth + j).xyz,
                            verts.oGet(i * maxWidth + j - 1).xyz, verts.oGet(i * maxWidth + j + 1).xyz, proj);
                    dir = verts.oGet(i * maxWidth + j).xyz.oMinus(proj);
                    len = dir.LengthSqr();
                    if (len > maxLength) {
                        maxLength = len;
                    }
                }
                if (maxLength < Square(0.2f)) {
                    width--;
                    for (i = 0; i < height; i++) {
                        for (k = j; k < width; k++) {
                            verts.oSet(i * maxWidth + k, verts.oGet(i * maxWidth + k + 1));
                        }
                    }
                    j--;
                }
            }
            for (j = 1; j < height - 1; j++) {
                maxLength = 0;
                for (i = 0; i < width; i++) {
                    this.ProjectPointOntoVector(verts.oGet(j * maxWidth + i).xyz,
                            verts.oGet((j - 1) * maxWidth + i).xyz, verts.oGet((j + 1) * maxWidth + i).xyz, proj);
                    dir = verts.oGet(j * maxWidth + i).xyz.oMinus(proj);
                    len = dir.LengthSqr();
                    if (len > maxLength) {
                        maxLength = len;
                    }
                }
                if (maxLength < Square(0.2f)) {
                    height--;
                    for (i = 0; i < width; i++) {
                        for (k = j; k < height; k++) {
                            verts.oSet(k * maxWidth + i, verts.oGet((k + 1) * maxWidth + i));
                        }
                    }
                    j--;
                }
            }
        }

        // resize verts buffer
        private void ResizeExpanded(int newHeight, int newWidth) {
            int i, j;

            assert (expanded == true);
            if (newHeight <= maxHeight && newWidth <= maxWidth) {
                return;
            }
            if (newHeight * newWidth > maxHeight * maxWidth) {
                verts.SetNum(newHeight * newWidth);
            }
            // space out verts for new height and width
            for (j = maxHeight - 1; j >= 0; j--) {
                for (i = maxWidth - 1; i >= 0; i--) {
                    verts.oSet(j * newWidth + i, verts.oGet(j * maxWidth + i));
                }
            }
            maxHeight = newHeight;
            maxWidth = newWidth;
        }

        // space points out over maxWidth * maxHeight buffer
        private void Expand() throws idException {
            int i, j;

            if (expanded) {
                idLib.common.FatalError("idSurface_Patch::Expand: patch alread expanded");
            }
            expanded = true;
            verts.SetNum(maxWidth * maxHeight, false);
            if (width != maxWidth) {
                for (j = height - 1; j >= 0; j--) {
                    for (i = width - 1; i >= 0; i--) {
                        verts.oSet(j * maxWidth + i, verts.oGet(j * width + i));
                    }
                }
            }
        }

        // move all points to the start of the verts buffer
        private void Collapse() throws idException {
            int i, j;

            if (!expanded) {
                idLib.common.FatalError("idSurface_Patch::Collapse: patch not expanded");
            }
            expanded = false;
            if (width != maxWidth) {
                for (j = 0; j < height; j++) {
                    for (i = 0; i < width; i++) {
                        verts.oSet(j * width + i, verts.oGet(j * maxWidth + i));
                    }
                }
            }
            verts.SetNum(width * height, false);
        }

        // project a point onto a vector to calculate maximum curve error
        private void ProjectPointOntoVector(final idVec3 point, final idVec3 vStart, final idVec3 vEnd, idVec3 vProj) {
            idVec3 pVec, vec;

            pVec = point.oMinus(vStart);
            vec = vEnd.oMinus(vStart);
            vec.Normalize();
            // project onto the directional vector for this segment
            vProj.oSet(vStart.oPlus(vec.oMultiply(pVec.oMultiply(vec))));
        }
//				
        static final float COPLANAR_EPSILON = 0.1f;

        /*
         =================
         idSurface_Patch::GenerateNormals

         Handles all the complicated wrapping and degenerate cases
         Expects a Not expanded patch.
         =================
         */
        private void GenerateNormals() {// generate normals
            int i, j, k, dist;
            idVec3 norm;
            idVec3 sum = new idVec3();
            int count;
            idVec3 base;
            idVec3 delta;
            int x, y;
            idVec3[] around = new idVec3[8];
            idVec3 temp;
            boolean[] good = new boolean[8];
            boolean wrapWidth, wrapHeight;
            final int[][] neighbors = {
                {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}
            };

            assert (expanded == false);

            //
            // if all points are coplanar, set all normals to that plane
            //
            idVec3[] extent = new idVec3[3];
            float offset;

            extent[0] = verts.oGet(width - 1).xyz.oMinus(verts.oGet(0).xyz);
            extent[1] = verts.oGet((height - 1) * width + width - 1).xyz.oMinus(verts.oGet(0).xyz);
            extent[2] = verts.oGet((height - 1) * width).xyz.oMinus(verts.oGet(0).xyz);

            norm = extent[0].Cross(extent[1]);
            if (norm.LengthSqr() == 0.0f) {
                norm = extent[0].Cross(extent[2]);
                if (norm.LengthSqr() == 0.0f) {
                    norm = extent[1].Cross(extent[2]);
                }
            }

            // wrapped patched may not get a valid normal here
            if (norm.Normalize() != 0.0f) {

                offset = verts.oGet(0).xyz.oMultiply(norm);
                for (i = 1; i < width * height; i++) {
                    float d = verts.oGet(i).xyz.oMultiply(norm);
                    if (idMath.Fabs(d - offset) > COPLANAR_EPSILON) {
                        break;
                    }
                }

                if (i == width * height) {
                    // all are coplanar
                    for (i = 0; i < width * height; i++) {
                        verts.oGet(i).normal = norm;
                    }
                    return;
                }
            }

            // check for wrapped edge cases, which should smooth across themselves
            wrapWidth = false;
            for (i = 0; i < height; i++) {
                delta = verts.oGet(i * width).xyz.oMinus(verts.oGet(i * width + width - 1).xyz);
                if (delta.LengthSqr() > Square(1.0f)) {
                    break;
                }
            }
            if (i == height) {
                wrapWidth = true;
            }

            wrapHeight = false;
            for (i = 0; i < width; i++) {
                delta = verts.oGet(i).xyz.oMinus(verts.oGet((height - 1) * width + i).xyz);
                if (delta.LengthSqr() > Square(1.0f)) {
                    break;
                }
            }
            if (i == width) {
                wrapHeight = true;
            }

            for (i = 0; i < width; i++) {
                for (j = 0; j < height; j++) {
                    count = 0;
                    base = verts.oGet(j * width + i).xyz;
                    for (k = 0; k < 8; k++) {
                        around[k].oSet(getVec3_origin());
                        good[k] = false;

                        for (dist = 1; dist <= 3; dist++) {
                            x = i + neighbors[k][0] * dist;
                            y = j + neighbors[k][1] * dist;
                            if (wrapWidth) {
                                if (x < 0) {
                                    x = width - 1 + x;
                                } else if (x >= width) {
                                    x = 1 + x - width;
                                }
                            }
                            if (wrapHeight) {
                                if (y < 0) {
                                    y = height - 1 + y;
                                } else if (y >= height) {
                                    y = 1 + y - height;
                                }
                            }

                            if (x < 0 || x >= width || y < 0 || y >= height) {
                                break;					// edge of patch
                            }
                            temp = verts.oGet(y * width + x).xyz.oMinus(base);
                            if (temp.Normalize() == 0.0f) {
                                continue;				// degenerate edge, get more dist
                            } else {
                                good[k] = true;
                                around[k] = temp;
                                break;					// good edge
                            }
                        }
                    }

                    sum.oSet(getVec3_origin());
                    for (k = 0; k < 8; k++) {
                        if (!good[k] || !good[(k + 1) & 7]) {
                            continue;	// didn't get two points
                        }
                        norm = around[(k + 1) & 7].Cross(around[k]);
                        if (norm.Normalize() == 0.0f) {
                            continue;
                        }
                        sum.oPluSet(norm);
                        count++;
                    }
                    if (count == 0) {
                        //idLib::common->Printf("bad normal\n");
                        count = 1;
                    }
                    verts.oGet(j * width + i).normal = sum;
                    verts.oGet(j * width + i).normal.Normalize();
                }
            }
        }

        // generate triangle indexes
        private void GenerateIndexes() {
            int i, j, v1, v2, v3, v4, index;

            indexes.SetNum((width - 1) * (height - 1) * 2 * 3, false);
            index = 0;
            for (i = 0; i < width - 1; i++) {
                for (j = 0; j < height - 1; j++) {
                    v1 = j * width + i;
                    v2 = v1 + 1;
                    v3 = v1 + width + 1;
                    v4 = v1 + width;
                    indexes.oSet(index++, v1);
                    indexes.oSet(index++, v3);
                    indexes.oSet(index++, v2);
                    indexes.oSet(index++, v1);
                    indexes.oSet(index++, v4);
                    indexes.oSet(index++, v3);
                }
            }

            GenerateEdgeIndexes();
        }

        // lerp point from two patch point
        private void LerpVert(final idDrawVert a, final idDrawVert b, idDrawVert out) {
            out.xyz.oSet(0, 0.5f * (a.xyz.oGet(0) + b.xyz.oGet(0)));
            out.xyz.oSet(1, 0.5f * (a.xyz.oGet(1) + b.xyz.oGet(1)));
            out.xyz.oSet(2, 0.5f * (a.xyz.oGet(2) + b.xyz.oGet(2)));
            out.normal.oSet(0, 0.5f * (a.normal.oGet(0) + b.normal.oGet(0)));
            out.normal.oSet(1, 0.5f * (a.normal.oGet(1) + b.normal.oGet(1)));
            out.normal.oSet(2, 0.5f * (a.normal.oGet(2) + b.normal.oGet(2)));
            out.st.oSet(0, 0.5f * (a.st.oGet(0) + b.st.oGet(0)));
            out.st.oSet(1, 0.5f * (a.st.oGet(1) + b.st.oGet(1)));
        }

        // sample a single 3x3 patch
        private void SampleSinglePatchPoint(final idDrawVert ctrl[][], float u, float v, idDrawVert out) {
            float[][] vCtrl = new float[3][8];
            int vPoint;
            int axis;

            // find the control points for the v coordinate
            for (vPoint = 0; vPoint < 3; vPoint++) {
                for (axis = 0; axis < 8; axis++) {
                    float a, b, c;
                    float qA, qB, qC;
                    if (axis < 3) {
                        a = ctrl[0][vPoint].xyz.oGet(axis);
                        b = ctrl[1][vPoint].xyz.oGet(axis);
                        c = ctrl[2][vPoint].xyz.oGet(axis);
                    } else if (axis < 6) {
                        a = ctrl[0][vPoint].normal.oGet(axis - 3);
                        b = ctrl[1][vPoint].normal.oGet(axis - 3);
                        c = ctrl[2][vPoint].normal.oGet(axis - 3);
                    } else {
                        a = ctrl[0][vPoint].st.oGet(axis - 6);
                        b = ctrl[1][vPoint].st.oGet(axis - 6);
                        c = ctrl[2][vPoint].st.oGet(axis - 6);
                    }
                    qA = a - 2.0f * b + c;
                    qB = 2.0f * b - 2.0f * a;
                    qC = a;
                    vCtrl[vPoint][axis] = qA * u * u + qB * u + qC;
                }
            }

            // interpolate the v value
            for (axis = 0; axis < 8; axis++) {
                float a, b, c;
                float qA, qB, qC;

                a = vCtrl[0][axis];
                b = vCtrl[1][axis];
                c = vCtrl[2][axis];
                qA = a - 2.0f * b + c;
                qB = 2.0f * b - 2.0f * a;
                qC = a;

                if (axis < 3) {
                    out.xyz.oSet(axis, qA * v * v + qB * v + qC);
                } else if (axis < 6) {
                    out.normal.oSet(axis - 3, qA * v * v + qB * v + qC);
                } else {
                    out.st.oSet(axis - 6, qA * v * v + qB * v + qC);
                }
            }
        }

        private void SampleSinglePatch(final idDrawVert ctrl[][], int baseCol, int baseRow, int width, int horzSub, int vertSub, idDrawVert[] outVerts) {
            int i, j;
            float u, v;

            horzSub++;
            vertSub++;
            for (i = 0; i < horzSub; i++) {
                for (j = 0; j < vertSub; j++) {
                    u = (float) i / (horzSub - 1);
                    v = (float) j / (vertSub - 1);
                    SampleSinglePatchPoint(ctrl, u, v, outVerts[((baseRow + j) * width) + i + baseCol]);
                }
            }
        }

        private void oSet(final idSurface_Patch patch) {
            this.width = patch.width;
            this.height = patch.height;
            this.maxWidth = patch.maxWidth;
            this.maxHeight = patch.maxHeight;
            this.verts.SetNum(maxWidth * maxHeight);
            this.expanded = patch.expanded;
        }
//        
//        
//        
//        
//        
//        
//        
//      
        public idDict epairs;
        protected int type;

        private void oSet(final idMapPrimitive patch) {
            this.type = patch.GetType();
            this.epairs = patch.epairs;
        }
    };
}
