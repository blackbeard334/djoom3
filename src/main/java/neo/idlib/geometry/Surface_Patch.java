package neo.idlib.geometry;

import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Vector.getVec3_origin;

import neo.idlib.Dict_h.idDict;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.MapFile.idMapPrimitive;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Surface.idSurface;
import neo.idlib.math.Math_h.idMath;
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
            this.height = this.width = this.maxHeight = this.maxWidth = 0;
            this.expanded = false;
        }

        public idSurface_Patch(int maxPatchWidth, int maxPatchHeight) {
            this.width = this.height = 0;
            this.maxWidth = maxPatchWidth;
            this.maxHeight = maxPatchHeight;
            this.verts.SetNum(this.maxWidth * this.maxHeight);
            this.expanded = false;
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
            if ((patchWidth < 1) || (patchWidth > this.maxWidth)) {
                idLib.common.FatalError("idSurface_Patch::SetSize: invalid patchWidth");
            }
            if ((patchHeight < 1) || (patchHeight > this.maxHeight)) {
                idLib.common.FatalError("idSurface_Patch::SetSize: invalid patchHeight");
            }
            this.width = patchWidth;
            this.height = patchHeight;
            this.verts.SetNum(this.width * this.height, false);
        }

        public int GetWidth() {
            return this.width;
        }

        public int GetHeight() {
            return this.height;
        }

        public void Subdivide(float maxHorizontalError, float maxVerticalError, float maxLength) throws idException {
            this.Subdivide(maxHorizontalError, maxVerticalError, maxLength, false);
        }

        // subdivide the patch mesh based on error
        public void Subdivide(float maxHorizontalError, float maxVerticalError, float maxLength, boolean genNormals) throws idException {
            int i, j, k, l;
            final idDrawVert prev = new idDrawVert(), next = new idDrawVert(), mid = new idDrawVert();
            final idVec3 prevxyz = new idVec3(), nextxyz = new idVec3(), midxyz = new idVec3();
            idVec3 delta;
            float maxHorizontalErrorSqr, maxVerticalErrorSqr, maxLengthSqr;

            // generate normals for the control mesh
            if (genNormals) {
                GenerateNormals();
            }

            maxHorizontalErrorSqr = Square(maxHorizontalError);
            maxVerticalErrorSqr = Square(maxVerticalError);
            maxLengthSqr = Square(maxLength);

            Expand();

            // horizontal subdivisions
            for (j = 0; (j + 2) < this.width; j += 2) {
                // check subdivided midpoints against control points
                for (i = 0; i < this.height; i++) {
                    for (l = 0; l < 3; l++) {
                        prevxyz.oSet(1, this.verts.oGet((i * this.maxWidth) + j + 1).xyz.oGet(l) - this.verts.oGet((i * this.maxWidth) + j).xyz.oGet(l));
                        nextxyz.oSet(1, this.verts.oGet((i * this.maxWidth) + j + 2).xyz.oGet(l) - this.verts.oGet((i * this.maxWidth) + j + 1).xyz.oGet(l));
                        midxyz.oSet(1, (this.verts.oGet((i * this.maxWidth) + j).xyz.oGet(l) + (this.verts.oGet((i * this.maxWidth) + j + 1).xyz.oGet(l) * 2.0f) + this.verts.oGet((i * this.maxWidth) + j + 2).xyz.oGet(l)) * 0.25f);
                    }

                    if (maxLength > 0.0f) {
                        // if the span length is too long, force a subdivision
                        if ((prevxyz.LengthSqr() > maxLengthSqr) || (nextxyz.LengthSqr() > maxLengthSqr)) {
                            break;
                        }
                    }
                    // see if this midpoint is off far enough to subdivide
                    delta = this.verts.oGet((i * this.maxWidth) + j + 1).xyz.oMinus(midxyz);
                    if (delta.LengthSqr() > maxHorizontalErrorSqr) {
                        break;
                    }
                }

                if (i == this.height) {
                    continue;	// didn't need subdivision
                }

                if ((this.width + 2) >= this.maxWidth) {
                    ResizeExpanded(this.maxHeight, this.maxWidth + 4);
                }

                // insert two columns and replace the peak
                this.width += 2;

                for (i = 0; i < this.height; i++) {
                    this.LerpVert(this.verts.oGet((i * this.maxWidth) + j), this.verts.oGet((i * this.maxWidth) + j + 1), prev);
                    this.LerpVert(this.verts.oGet((i * this.maxWidth) + j + 1), this.verts.oGet((i * this.maxWidth) + j + 2), next);
                    this.LerpVert(prev, next, mid);

                    for (k = this.width - 1; k > (j + 3); k--) {
                        this.verts.oSet((i * this.maxWidth) + k, this.verts.oGet(((i * this.maxWidth) + k) - 2));
                    }
                    this.verts.oSet((i * this.maxWidth) + j + 1, prev);
                    this.verts.oSet((i * this.maxWidth) + j + 2, mid);
                    this.verts.oSet((i * this.maxWidth) + j + 3, next);
                }

                // back up and recheck this set again, it may need more subdivision
                j -= 2;
            }

            // vertical subdivisions
            for (j = 0; (j + 2) < this.height; j += 2) {
                // check subdivided midpoints against control points
                for (i = 0; i < this.width; i++) {
                    for (l = 0; l < 3; l++) {
                        prevxyz.oSet(1, this.verts.oGet(((j + 1) * this.maxWidth) + i).xyz.oGet(l) - this.verts.oGet((j * this.maxWidth) + i).xyz.oGet(l));
                        nextxyz.oSet(1, this.verts.oGet(((j + 2) * this.maxWidth) + i).xyz.oGet(l) - this.verts.oGet(((j + 1) * this.maxWidth) + i).xyz.oGet(l));
                        midxyz.oSet(1, (this.verts.oGet((j * this.maxWidth) + i).xyz.oGet(l) + (this.verts.oGet(((j + 1) * this.maxWidth) + i).xyz.oGet(l) * 2.0f)
                                + this.verts.oGet(((j + 2) * this.maxWidth) + i).xyz.oGet(l)) * 0.25f);
                    }

                    if (maxLength > 0.0f) {
                        // if the span length is too long, force a subdivision
                        if ((prevxyz.LengthSqr() > maxLengthSqr) || (nextxyz.LengthSqr() > maxLengthSqr)) {
                            break;
                        }
                    }
                    // see if this midpoint is off far enough to subdivide
                    delta = this.verts.oGet(((j + 1) * this.maxWidth) + i).xyz.oMinus(midxyz);
                    if (delta.LengthSqr() > maxVerticalErrorSqr) {
                        break;
                    }
                }

                if (i == this.width) {
                    continue;	// didn't need subdivision
                }

                if ((this.height + 2) >= this.maxHeight) {
                    ResizeExpanded(this.maxHeight + 4, this.maxWidth);
                }

                // insert two columns and replace the peak
                this.height += 2;

                for (i = 0; i < this.width; i++) {
                    LerpVert(this.verts.oGet((j * this.maxWidth) + i), this.verts.oGet(((j + 1) * this.maxWidth) + i), prev);
                    LerpVert(this.verts.oGet(((j + 1) * this.maxWidth) + i), this.verts.oGet(((j + 2) * this.maxWidth) + i), next);
                    LerpVert(prev, next, mid);

                    for (k = this.height - 1; k > (j + 3); k--) {
                        this.verts.oSet((k * this.maxWidth) + i, this.verts.oGet(((k - 2) * this.maxWidth) + i));
                    }
                    this.verts.oSet(((j + 1) * this.maxWidth) + i, prev);
                    this.verts.oSet(((j + 2) * this.maxWidth) + i, mid);
                    this.verts.oSet(((j + 3) * this.maxWidth) + i, next);
                }

                // back up and recheck this set again, it may need more subdivision
                j -= 2;
            }

            PutOnCurve();

            RemoveLinearColumnsRows();

            Collapse();

            // normalize all the lerped normals
            if (genNormals) {
                for (i = 0; i < (this.width * this.height); i++) {
                    this.verts.oGet(i).normal.Normalize();
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
            final idDrawVert[][] sample = new idDrawVert[3][3];
            final int outWidth = (((this.width - 1) / 2) * horzSubdivisions) + 1;
            final int outHeight = (((this.height - 1) / 2) * vertSubdivisions) + 1;
            final idDrawVert[] dv = new idDrawVert[outWidth * outHeight];

            // generate normals for the control mesh
            if (genNormals) {
                GenerateNormals();
            }

            int baseCol = 0;
            for (i = 0; (i + 2) < this.width; i += 2) {
                int baseRow = 0;
                for (j = 0; (j + 2) < this.height; j += 2) {
                    for (k = 0; k < 3; k++) {
                        for (l = 0; l < 3; l++) {
                            sample[k][l] = this.verts.oGet(((j + l) * this.width) + i + k);
                        }
                    }
                    SampleSinglePatch(sample, baseCol, baseRow, outWidth, horzSubdivisions, vertSubdivisions, dv);
                    baseRow += vertSubdivisions;
                }
                baseCol += horzSubdivisions;
            }
            this.verts.SetNum(outWidth * outHeight);
            for (i = 0; i < (outWidth * outHeight); i++) {
                this.verts.oSet(i, dv[i]);
            }

//	delete[] dv;
            this.width = this.maxWidth = outWidth;
            this.height = this.maxHeight = outHeight;
            this.expanded = false;

            if (removeLinear) {
                Expand();
                RemoveLinearColumnsRows();
                Collapse();
            }

            // normalize all the lerped normals
            if (genNormals) {
                for (i = 0; i < (this.width * this.height); i++) {
                    this.verts.oGet(i).normal.Normalize();
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
            final idDrawVert prev = new idDrawVert(), next = new idDrawVert();

            assert (this.expanded == true);
            // put all the approximating points on the curve
            for (i = 0; i < this.width; i++) {
                for (j = 1; j < this.height; j += 2) {
                    LerpVert(this.verts.oGet((j * this.maxWidth) + i), this.verts.oGet(((j + 1) * this.maxWidth) + i), prev);
                    LerpVert(this.verts.oGet((j * this.maxWidth) + i), this.verts.oGet(((j - 1) * this.maxWidth) + i), next);
                    LerpVert(prev, next, this.verts.oGet((j * this.maxWidth) + i));
                }
            }

            for (j = 0; j < this.height; j++) {
                for (i = 1; i < this.width; i += 2) {
                    LerpVert(this.verts.oGet((j * this.maxWidth) + i), this.verts.oGet((j * this.maxWidth) + i + 1), prev);
                    LerpVert(this.verts.oGet((j * this.maxWidth) + i), this.verts.oGet(((j * this.maxWidth) + i) - 1), next);
                    LerpVert(prev, next, this.verts.oGet((j * this.maxWidth) + i));
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
            final idVec3 proj = new idVec3();
			idVec3 dir;

            assert (this.expanded == true);
            for (j = 1; j < (this.width - 1); j++) {
                maxLength = 0;
                for (i = 0; i < this.height; i++) {
                    this.ProjectPointOntoVector(this.verts.oGet((i * this.maxWidth) + j).xyz,
                            this.verts.oGet(((i * this.maxWidth) + j) - 1).xyz, this.verts.oGet((i * this.maxWidth) + j + 1).xyz, proj);
                    dir = this.verts.oGet((i * this.maxWidth) + j).xyz.oMinus(proj);
                    len = dir.LengthSqr();
                    if (len > maxLength) {
                        maxLength = len;
                    }
                }
                if (maxLength < Square(0.2f)) {
                    this.width--;
                    for (i = 0; i < this.height; i++) {
                        for (k = j; k < this.width; k++) {
                            this.verts.oSet((i * this.maxWidth) + k, this.verts.oGet((i * this.maxWidth) + k + 1));
                        }
                    }
                    j--;
                }
            }
            for (j = 1; j < (this.height - 1); j++) {
                maxLength = 0;
                for (i = 0; i < this.width; i++) {
                    this.ProjectPointOntoVector(this.verts.oGet((j * this.maxWidth) + i).xyz,
                            this.verts.oGet(((j - 1) * this.maxWidth) + i).xyz, this.verts.oGet(((j + 1) * this.maxWidth) + i).xyz, proj);
                    dir = this.verts.oGet((j * this.maxWidth) + i).xyz.oMinus(proj);
                    len = dir.LengthSqr();
                    if (len > maxLength) {
                        maxLength = len;
                    }
                }
                if (maxLength < Square(0.2f)) {
                    this.height--;
                    for (i = 0; i < this.width; i++) {
                        for (k = j; k < this.height; k++) {
                            this.verts.oSet((k * this.maxWidth) + i, this.verts.oGet(((k + 1) * this.maxWidth) + i));
                        }
                    }
                    j--;
                }
            }
        }

        // resize verts buffer
        private void ResizeExpanded(int newHeight, int newWidth) {
            int i, j;

            assert (this.expanded == true);
            if ((newHeight <= this.maxHeight) && (newWidth <= this.maxWidth)) {
                return;
            }
            if ((newHeight * newWidth) > (this.maxHeight * this.maxWidth)) {
                this.verts.SetNum(newHeight * newWidth);
            }
            // space out verts for new height and width
            for (j = this.maxHeight - 1; j >= 0; j--) {
                for (i = this.maxWidth - 1; i >= 0; i--) {
                    this.verts.oSet((j * newWidth) + i, this.verts.oGet((j * this.maxWidth) + i));
                }
            }
            this.maxHeight = newHeight;
            this.maxWidth = newWidth;
        }

        // space points out over maxWidth * maxHeight buffer
        private void Expand() throws idException {
            int i, j;

            if (this.expanded) {
                idLib.common.FatalError("idSurface_Patch::Expand: patch alread expanded");
            }
            this.expanded = true;
            this.verts.SetNum(this.maxWidth * this.maxHeight, false);
            if (this.width != this.maxWidth) {
                for (j = this.height - 1; j >= 0; j--) {
                    for (i = this.width - 1; i >= 0; i--) {
                        this.verts.oSet((j * this.maxWidth) + i, this.verts.oGet((j * this.width) + i));
                    }
                }
            }
        }

        // move all points to the start of the verts buffer
        private void Collapse() throws idException {
            int i, j;

            if (!this.expanded) {
                idLib.common.FatalError("idSurface_Patch::Collapse: patch not expanded");
            }
            this.expanded = false;
            if (this.width != this.maxWidth) {
                for (j = 0; j < this.height; j++) {
                    for (i = 0; i < this.width; i++) {
                        this.verts.oSet((j * this.width) + i, this.verts.oGet((j * this.maxWidth) + i));
                    }
                }
            }
            this.verts.SetNum(this.width * this.height, false);
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
            final idVec3 sum = new idVec3();
            int count;
            idVec3 base;
            idVec3 delta;
            int x, y;
            final idVec3[] around = new idVec3[8];
            idVec3 temp;
            final boolean[] good = new boolean[8];
            boolean wrapWidth, wrapHeight;
            final int[][] neighbors = {
                {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}
            };

            assert (this.expanded == false);

            //
            // if all points are coplanar, set all normals to that plane
            //
            final idVec3[] extent = new idVec3[3];
            float offset;

            extent[0] = this.verts.oGet(this.width - 1).xyz.oMinus(this.verts.oGet(0).xyz);
            extent[1] = this.verts.oGet((((this.height - 1) * this.width) + this.width) - 1).xyz.oMinus(this.verts.oGet(0).xyz);
            extent[2] = this.verts.oGet((this.height - 1) * this.width).xyz.oMinus(this.verts.oGet(0).xyz);

            norm = extent[0].Cross(extent[1]);
            if (norm.LengthSqr() == 0.0f) {
                norm = extent[0].Cross(extent[2]);
                if (norm.LengthSqr() == 0.0f) {
                    norm = extent[1].Cross(extent[2]);
                }
            }

            // wrapped patched may not get a valid normal here
            if (norm.Normalize() != 0.0f) {

                offset = this.verts.oGet(0).xyz.oMultiply(norm);
                for (i = 1; i < (this.width * this.height); i++) {
                    final float d = this.verts.oGet(i).xyz.oMultiply(norm);
                    if (idMath.Fabs(d - offset) > COPLANAR_EPSILON) {
                        break;
                    }
                }

                if (i == (this.width * this.height)) {
                    // all are coplanar
                    for (i = 0; i < (this.width * this.height); i++) {
                        this.verts.oGet(i).normal = norm;
                    }
                    return;
                }
            }

            // check for wrapped edge cases, which should smooth across themselves
            wrapWidth = false;
            for (i = 0; i < this.height; i++) {
                delta = this.verts.oGet(i * this.width).xyz.oMinus(this.verts.oGet(((i * this.width) + this.width) - 1).xyz);
                if (delta.LengthSqr() > Square(1.0f)) {
                    break;
                }
            }
            if (i == this.height) {
                wrapWidth = true;
            }

            wrapHeight = false;
            for (i = 0; i < this.width; i++) {
                delta = this.verts.oGet(i).xyz.oMinus(this.verts.oGet(((this.height - 1) * this.width) + i).xyz);
                if (delta.LengthSqr() > Square(1.0f)) {
                    break;
                }
            }
            if (i == this.width) {
                wrapHeight = true;
            }

            for (i = 0; i < this.width; i++) {
                for (j = 0; j < this.height; j++) {
                    count = 0;
                    base = this.verts.oGet((j * this.width) + i).xyz;
                    for (k = 0; k < 8; k++) {
                        around[k].oSet(getVec3_origin());
                        good[k] = false;

                        for (dist = 1; dist <= 3; dist++) {
                            x = i + (neighbors[k][0] * dist);
                            y = j + (neighbors[k][1] * dist);
                            if (wrapWidth) {
                                if (x < 0) {
                                    x = (this.width - 1) + x;
                                } else if (x >= this.width) {
                                    x = (1 + x) - this.width;
                                }
                            }
                            if (wrapHeight) {
                                if (y < 0) {
                                    y = (this.height - 1) + y;
                                } else if (y >= this.height) {
                                    y = (1 + y) - this.height;
                                }
                            }

                            if ((x < 0) || (x >= this.width) || (y < 0) || (y >= this.height)) {
                                break;					// edge of patch
                            }
                            temp = this.verts.oGet((y * this.width) + x).xyz.oMinus(base);
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
                    this.verts.oGet((j * this.width) + i).normal = sum;
                    this.verts.oGet((j * this.width) + i).normal.Normalize();
                }
            }
        }

        // generate triangle indexes
        private void GenerateIndexes() {
            int i, j, v1, v2, v3, v4, index;

            this.indexes.SetNum((this.width - 1) * (this.height - 1) * 2 * 3, false);
            index = 0;
            for (i = 0; i < (this.width - 1); i++) {
                for (j = 0; j < (this.height - 1); j++) {
                    v1 = (j * this.width) + i;
                    v2 = v1 + 1;
                    v3 = v1 + this.width + 1;
                    v4 = v1 + this.width;
                    this.indexes.oSet(index++, v1);
                    this.indexes.oSet(index++, v3);
                    this.indexes.oSet(index++, v2);
                    this.indexes.oSet(index++, v1);
                    this.indexes.oSet(index++, v4);
                    this.indexes.oSet(index++, v3);
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
            final float[][] vCtrl = new float[3][8];
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
                    qA = (a - (2.0f * b)) + c;
                    qB = (2.0f * b) - (2.0f * a);
                    qC = a;
                    vCtrl[vPoint][axis] = (qA * u * u) + (qB * u) + qC;
                }
            }

            // interpolate the v value
            for (axis = 0; axis < 8; axis++) {
                float a, b, c;
                float qA, qB, qC;

                a = vCtrl[0][axis];
                b = vCtrl[1][axis];
                c = vCtrl[2][axis];
                qA = (a - (2.0f * b)) + c;
                qB = (2.0f * b) - (2.0f * a);
                qC = a;

                if (axis < 3) {
                    out.xyz.oSet(axis, (qA * v * v) + (qB * v) + qC);
                } else if (axis < 6) {
                    out.normal.oSet(axis - 3, (qA * v * v) + (qB * v) + qC);
                } else {
                    out.st.oSet(axis - 6, (qA * v * v) + (qB * v) + qC);
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
            this.verts.SetNum(this.maxWidth * this.maxHeight);
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
    }
}
