package neo.idlib.geometry;

import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_BONE;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_BOX;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_CONE;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_CYLINDER;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_DODECAHEDRON;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_INVALID;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_OCTAHEDRON;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_POLYGON;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_POLYGONVOLUME;
import static neo.idlib.math.Math_h.Cube;
import static neo.idlib.math.Math_h.INTSIGNBITNOTSET;
import static neo.idlib.math.Math_h.INTSIGNBITSET;
import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Vector.getVec3_origin;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import neo.idlib.Lib.idLib;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;
import neo.open.Nio;

/**
 *
 */
public class TraceModel {

    /*
     ===============================================================================

     A trace model is an arbitrary polygonal model which is used by the
     collision detection system to find collisions, contacts or the contents
     of a volume. For collision detection speed reasons the number of vertices
     and edges are limited. The trace model can have any shape. However convex
     models are usually preferred.

     ===============================================================================
     */
// trace model type
    public enum traceModel_t {

        TRM_INVALID, // invalid trm
        TRM_BOX, // box
        TRM_OCTAHEDRON, // octahedron
        TRM_DODECAHEDRON, // dodecahedron
        TRM_CYLINDER, // cylinder approximation
        TRM_CONE, // cone approximation
        TRM_BONE, // two tetrahedrons attached to each other
        TRM_POLYGON, // arbitrary convex polygon
        TRM_POLYGONVOLUME, // volume for arbitrary convex polygon
        TRM_CUSTOM			// loaded from map model or ASE/LWO
    }
// these are bit cache limits
    public static final int MAX_TRACEMODEL_VERTS = 32;
    public static final int MAX_TRACEMODEL_EDGES = 32;
    public static final int MAX_TRACEMODEL_POLYS = 16;
    public static final int MAX_TRACEMODEL_POLYEDGES = 16;

    public static class traceModelVert_t extends idVec3 {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
    }

    public static class traceModelEdge_t {

        public int[] v;
        public idVec3 normal;

        public traceModelEdge_t() {
            this.v = new int[2];
            this.normal = new idVec3();
        }

        public void oSet(final traceModelEdge_t t){
            this.v[0] = t.v[0];
            this.v[1] = t.v[1];
            this.normal.oSet(t.normal);
        }
    }

    public static class traceModelPoly_t {

        public idVec3   normal;
        public float    dist;
        public idBounds bounds;
        public int      numEdges;
        public int[]    edges;

        public traceModelPoly_t() {
            this.normal = new idVec3();
            this.bounds = new idBounds();
            this.edges = new int[MAX_TRACEMODEL_POLYEDGES];
        }

        private void oSet(traceModelPoly_t t) {
            this.normal.oSet(t.normal);
            this.dist = t.dist;
            this.bounds.oSet(t.bounds);
            this.numEdges = t.numEdges;
            Nio.arraycopy(t.edges, 0, this.edges, 0, MAX_TRACEMODEL_POLYEDGES);
        }
    }

    public static class idTraceModel {
        public traceModel_t       type;
        public int                numVerts;
        public traceModelVert_t[] verts = Stream.generate(traceModelVert_t::new).limit(MAX_TRACEMODEL_VERTS).toArray(traceModelVert_t[]::new);
        public int                numEdges;
        public traceModelEdge_t[] edges = Stream.generate(traceModelEdge_t::new).limit(MAX_TRACEMODEL_EDGES + 1).toArray(traceModelEdge_t[]::new);
        public int                numPolys;
        public traceModelPoly_t[] polys = Stream.generate(traceModelPoly_t::new).limit(MAX_TRACEMODEL_POLYS).toArray(traceModelPoly_t[]::new);
        public idVec3             offset;            // offset to center of model
        public idBounds           bounds;            // bounds of model
        public boolean            isConvex;          // true when model is convex
//

        public idTraceModel() {
            this.type = TRM_INVALID;
            this.numVerts = this.numEdges = this.numPolys = 0;
            this.offset = new idVec3();
            this.bounds = new idBounds();
        }

        // axial bounding box
        public idTraceModel(final idBounds boxBounds) {
            this();
            InitBox();
            SetupBox(boxBounds);
        }

        // cylinder approximation
        public idTraceModel(final idBounds cylBounds, final int numSides) {
            this();
            SetupCylinder(cylBounds, numSides);
        }

        // bone
        public idTraceModel(final float length, final float width) {
            this();
            InitBone();
            SetupBone(length, width);
        }

        // axial box
        public void SetupBox(final idBounds boxBounds) {
            int i;

            if (this.type != TRM_BOX) {
                InitBox();
            }
            // offset to center
            this.offset = (boxBounds.oGet(0).oPlus(boxBounds.oGet(1))).oMultiply(0.5f);
            // set box vertices
            for (i = 0; i < 8; i++) {
                this.verts[i].oSet(0, boxBounds.oGet((i ^ (i >> 1)) & 1).oGet(0));
                this.verts[i].oSet(1, boxBounds.oGet((i >> 1) & 1).oGet(1));
                this.verts[i].oSet(2, boxBounds.oGet((i >> 2) & 1).oGet(2));
            }
            // set polygon plane distances
            this.polys[0].dist = -boxBounds.oGet(0).oGet(2);
            this.polys[1].dist = boxBounds.oGet(1).oGet(2);
            this.polys[2].dist = -boxBounds.oGet(0).oGet(1);
            this.polys[3].dist = boxBounds.oGet(1).oGet(0);
            this.polys[4].dist = boxBounds.oGet(1).oGet(1);
            this.polys[5].dist = -boxBounds.oGet(0).oGet(0);
            // set polygon bounds
            for (i = 0; i < 6; i++) {
                this.polys[i].bounds.oSet(boxBounds);
            }
            this.polys[0].bounds.oSet(1, 2, boxBounds.oGet(0).oGet(2));
            this.polys[1].bounds.oSet(0, 2, boxBounds.oGet(1).oGet(2));
            this.polys[2].bounds.oSet(1, 1, boxBounds.oGet(0).oGet(1));
            this.polys[3].bounds.oSet(0, 0, boxBounds.oGet(1).oGet(0));
            this.polys[4].bounds.oSet(0, 1, boxBounds.oGet(1).oGet(1));
            this.polys[5].bounds.oSet(1, 0, boxBounds.oGet(0).oGet(0));

            this.bounds.oSet(boxBounds);
        }

        /*
         ============
         idTraceModel::SetupBox

         The origin is placed at the center of the cube.
         ============
         */
        public void SetupBox(final float size) {
            final idBounds boxBounds = new idBounds();
            float halfSize;

            halfSize = size * 0.5f;
            boxBounds.oGet(0).Set(-halfSize, -halfSize, -halfSize);
            boxBounds.oGet(1).Set(halfSize, halfSize, halfSize);
            SetupBox(boxBounds);
        }

        // octahedron
        public void SetupOctahedron(final idBounds octBounds) {
            int i, e0, e1, v0, v1, v2;
            final idVec3 v = new idVec3();

            if (this.type != TRM_OCTAHEDRON) {
                InitOctahedron();
            }

            this.offset = octBounds.oGet(0).oPlus(octBounds.oGet(1)).oMultiply(0.5f);
            v.oSet(0, octBounds.oGet(1).oGet(0) - this.offset.oGet(0));
            v.oSet(1, octBounds.oGet(1).oGet(1) - this.offset.oGet(1));
            v.oSet(2, octBounds.oGet(1).oGet(2) - this.offset.oGet(2));

            // set vertices
            this.verts[0].Set(this.offset.x + v.oGet(0), this.offset.y, this.offset.z);
            this.verts[1].Set(this.offset.x - v.oGet(0), this.offset.y, this.offset.z);
            this.verts[2].Set(this.offset.x, this.offset.y + v.oGet(1), this.offset.z);
            this.verts[3].Set(this.offset.x, this.offset.y - v.oGet(1), this.offset.z);
            this.verts[4].Set(this.offset.x, this.offset.y, this.offset.z + v.oGet(2));
            this.verts[5].Set(this.offset.x, this.offset.y, this.offset.z - v.oGet(2));

            // set polygons
            for (i = 0; i < this.numPolys; i++) {
                e0 = this.polys[i].edges[0];
                e1 = this.polys[i].edges[1];
                v0 = this.edges[Math.abs(e0)].v[INTSIGNBITSET(e0)];
                v1 = this.edges[Math.abs(e0)].v[INTSIGNBITNOTSET(e0)];
                v2 = this.edges[Math.abs(e1)].v[INTSIGNBITNOTSET(e1)];
                // polygon plane
                this.polys[i].normal.oSet((this.verts[v1].oMinus(this.verts[v0])).Cross(this.verts[v2].oMinus(this.verts[v0])));
                this.polys[i].normal.Normalize();
                this.polys[i].dist = this.polys[i].normal.oMultiply(this.verts[v0]);
                // polygon bounds
                this.polys[i].bounds.oSet(0, this.polys[i].bounds.oSet(0, this.verts[v0]));
                this.polys[i].bounds.AddPoint(this.verts[v1]);
                this.polys[i].bounds.AddPoint(this.verts[v2]);
            }

            // trm bounds
            this.bounds = octBounds;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::SetupOctahedron

         The origin is placed at the center of the octahedron.
         ============
         */
        public void SetupOctahedron(final float size) {
            final idBounds octBounds = new idBounds();
            float halfSize;

            halfSize = size * 0.5f;
            octBounds.oGet(0).Set(-halfSize, -halfSize, -halfSize);
            octBounds.oGet(1).Set(halfSize, halfSize, halfSize);
            SetupOctahedron(octBounds);
        }

        // dodecahedron
        public void SetupDodecahedron(final idBounds dodBounds) {
            int i, e0, e1, e2, e3, v0, v1, v2, v3, v4;
            float s, d;
            final idVec3 a = new idVec3(), b = new idVec3(), c = new idVec3();

            if (this.type != TRM_DODECAHEDRON) {
                InitDodecahedron();
            }

            a.x = a.y = a.z = 0.5773502691896257f; // 1.0f / ( 3.0f ) ^ 0.5f;
            b.x = b.y = b.z = 0.3568220897730899f; // ( ( 3.0f - ( 5.0f ) ^ 0.5f ) / 6.0f ) ^ 0.5f;
            c.x = c.y = c.z = 0.9341723589627156f; // ( ( 3.0f + ( 5.0f ) ^ 0.5f ) / 6.0f ) ^ 0.5f;
            d = 0.5f / c.oGet(0);
            s = (dodBounds.oGet(1).oGet(0) - dodBounds.oGet(0).oGet(0)) * d;
            a.x *= s;
            b.x *= s;
            c.x *= s;
            s = (dodBounds.oGet(1).oGet(1) - dodBounds.oGet(0).oGet(1)) * d;
            a.y *= s;
            b.y *= s;
            c.y *= s;
            s = (dodBounds.oGet(1).oGet(2) - dodBounds.oGet(0).oGet(2)) * d;
            a.z *= s;
            b.z *= s;
            c.z *= s;

            this.offset = dodBounds.oGet(0).oPlus(dodBounds.oGet(1)).oMultiply(0.5f);

            // set vertices
            this.verts[ 0].Set(this.offset.x + a.oGet(0), this.offset.y + a.oGet(1), this.offset.z + a.oGet(2));
            this.verts[ 1].Set(this.offset.x + a.oGet(0), this.offset.y + a.oGet(1), this.offset.z - a.oGet(2));
            this.verts[ 2].Set(this.offset.x + a.oGet(0), this.offset.y - a.oGet(1), this.offset.z + a.oGet(2));
            this.verts[ 3].Set(this.offset.x + a.oGet(0), this.offset.y - a.oGet(1), this.offset.z - a.oGet(2));
            this.verts[ 4].Set(this.offset.x - a.oGet(0), this.offset.y + a.oGet(1), this.offset.z + a.oGet(2));
            this.verts[ 5].Set(this.offset.x - a.oGet(0), this.offset.y + a.oGet(1), this.offset.z - a.oGet(2));
            this.verts[ 6].Set(this.offset.x - a.oGet(0), this.offset.y - a.oGet(1), this.offset.z + a.oGet(2));
            this.verts[ 7].Set(this.offset.x - a.oGet(0), this.offset.y - a.oGet(1), this.offset.z - a.oGet(2));
            this.verts[ 8].Set(this.offset.x + b.oGet(0), this.offset.y + c.oGet(1), this.offset.z/*        */);
            this.verts[ 9].Set(this.offset.x - b.oGet(0), this.offset.y + c.oGet(1), this.offset.z/*        */);
            this.verts[10].Set(this.offset.x + b.oGet(0), this.offset.y - c.oGet(1), this.offset.z/*        */);
            this.verts[11].Set(this.offset.x - b.oGet(0), this.offset.y - c.oGet(1), this.offset.z/*        */);
            this.verts[12].Set(this.offset.x + c.oGet(0), this.offset.y/*        */, this.offset.z + b.oGet(2));
            this.verts[13].Set(this.offset.x + c.oGet(0), this.offset.y/*        */, this.offset.z - b.oGet(2));
            this.verts[14].Set(this.offset.x - c.oGet(0), this.offset.y/*        */, this.offset.z + b.oGet(2));
            this.verts[15].Set(this.offset.x - c.oGet(0), this.offset.y/*        */, this.offset.z - b.oGet(2));
            this.verts[16].Set(this.offset.x/*        */, this.offset.y + b.oGet(1), this.offset.z + c.oGet(2));
            this.verts[17].Set(this.offset.x/*        */, this.offset.y - b.oGet(1), this.offset.z + c.oGet(2));
            this.verts[18].Set(this.offset.x/*        */, this.offset.y + b.oGet(1), this.offset.z - c.oGet(2));
            this.verts[19].Set(this.offset.x/*        */, this.offset.y - b.oGet(1), this.offset.z - c.oGet(2));

            // set polygons
            for (i = 0; i < this.numPolys; i++) {
                e0 = this.polys[i].edges[0];
                e1 = this.polys[i].edges[1];
                e2 = this.polys[i].edges[2];
                e3 = this.polys[i].edges[3];
                v0 = this.edges[Math.abs(e0)].v[INTSIGNBITSET(e0)];
                v1 = this.edges[Math.abs(e0)].v[INTSIGNBITNOTSET(e0)];
                v2 = this.edges[Math.abs(e1)].v[INTSIGNBITNOTSET(e1)];
                v3 = this.edges[Math.abs(e2)].v[INTSIGNBITNOTSET(e2)];
                v4 = this.edges[Math.abs(e3)].v[INTSIGNBITNOTSET(e3)];
                // polygon plane
                this.polys[i].normal.oSet((this.verts[v1].oMinus(this.verts[v0])).Cross(this.verts[v2].oMinus(this.verts[v0])));
                this.polys[i].normal.Normalize();
                this.polys[i].dist = this.polys[i].normal.oMultiply(this.verts[v0]);
                // polygon bounds
                this.polys[i].bounds.oSet(0, this.polys[i].bounds.oSet(1, this.verts[v0]));
                this.polys[i].bounds.AddPoint(this.verts[v1]);
                this.polys[i].bounds.AddPoint(this.verts[v2]);
                this.polys[i].bounds.AddPoint(this.verts[v3]);
                this.polys[i].bounds.AddPoint(this.verts[v4]);
            }

            // trm bounds
            this.bounds = dodBounds;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::SetupDodecahedron

         The origin is placed at the center of the octahedron.
         ============
         */
        public void SetupDodecahedron(final float size) {
            final idBounds dodBounds = new idBounds();
            float halfSize;

            halfSize = size * 0.5f;
            dodBounds.oGet(0).Set(-halfSize, -halfSize, -halfSize);
            dodBounds.oGet(1).Set(halfSize, halfSize, halfSize);
            SetupDodecahedron(dodBounds);
        }

        // cylinder approximation
        public void SetupCylinder(final idBounds cylBounds, final int numSides) {
            int i, n, ii, n2;
            float angle;
            idVec3 halfSize;

            n = numSides;
            if (n < 3) {
                n = 3;
            }
            if ((n * 2) > MAX_TRACEMODEL_VERTS) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCylinder: too many vertices\n");
                n = MAX_TRACEMODEL_VERTS / 2;
            }
            if ((n * 3) > MAX_TRACEMODEL_EDGES) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCylinder: too many sides\n");
                n = MAX_TRACEMODEL_EDGES / 3;
            }
            if ((n + 2) > MAX_TRACEMODEL_POLYS) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCylinder: too many polygons\n");
                n = MAX_TRACEMODEL_POLYS - 2;
            }

            this.type = TRM_CYLINDER;
            this.numVerts = n * 2;
            this.numEdges = n * 3;
            this.numPolys = n + 2;
            this.offset = cylBounds.oGet(0).oPlus(cylBounds.oGet(1)).oMultiply(0.5f);
            halfSize = cylBounds.oGet(1).oMinus(this.offset);
            for (i = 0; i < n; i++) {
                // verts
                angle = (idMath.TWO_PI * i) / n;
                this.verts[i].x = (float) ((Math.cos(angle) * halfSize.x) + this.offset.x);
                this.verts[i].y = (float) ((Math.sin(angle) * halfSize.y) + this.offset.y);
                this.verts[i].z = -halfSize.z + this.offset.z;
                this.verts[n + i].x = this.verts[i].x;
                this.verts[n + i].y = this.verts[i].y;
                this.verts[n + i].z = halfSize.z + this.offset.z;
                // edges
                ii = i + 1;
                n2 = n << 1;
                this.edges[ii].v[0] = i;
                this.edges[ii].v[1] = ii % n;
                this.edges[n + ii].v[0] = this.edges[ii].v[0] + n;
                this.edges[n + ii].v[1] = this.edges[ii].v[1] + n;
                this.edges[n2 + ii].v[0] = i;
                this.edges[n2 + ii].v[1] = n + i;
                // vertical polygon edges
                this.polys[i].numEdges = 4;
                this.polys[i].edges[0] = ii;
                this.polys[i].edges[1] = n2 + (ii % n) + 1;
                this.polys[i].edges[2] = -(n + ii);
                this.polys[i].edges[3] = -(n2 + ii);
                // bottom and top polygon edges
                this.polys[n].edges[i] = -(n - i);
                this.polys[n + 1].edges[i] = n + ii;
            }
            // bottom and top polygon numEdges
            this.polys[n].numEdges = n;
            this.polys[n + 1].numEdges = n;
            // polygons
            for (i = 0; i < n; i++) {
                // vertical polygon plane
                this.polys[i].normal.oSet((this.verts[(i + 1) % n].oMinus(this.verts[i])).Cross(this.verts[n + i].oMinus(this.verts[i])));
                this.polys[i].normal.Normalize();
                this.polys[i].dist = this.polys[i].normal.oMultiply(this.verts[i]);
                // vertical polygon bounds
                this.polys[i].bounds.Clear();
                this.polys[i].bounds.AddPoint(this.verts[i]);
                this.polys[i].bounds.AddPoint(this.verts[(i + 1) % n]);
                this.polys[i].bounds.oSet(0, 2, -halfSize.z + this.offset.z);
                this.polys[i].bounds.oSet(1, 2, halfSize.z + this.offset.z);
            }
            // bottom and top polygon plane
            this.polys[n].normal.Set(0.0f, 0.0f, -1.0f);
            this.polys[n].dist = -cylBounds.oGet(0).oGet(2);
            this.polys[n + 1].normal.Set(0.0f, 0.0f, 1.0f);
            this.polys[n + 1].dist = cylBounds.oGet(1).oGet(2);
            // trm bounds
            this.bounds = cylBounds;
            // bottom and top polygon bounds
            this.polys[n].bounds.oSet(this.bounds);
            this.polys[n].bounds.oSet(1, 2, this.bounds.oGet(0).oGet(2));
            this.polys[n + 1].bounds.oSet(this.bounds);
            this.polys[n + 1].bounds.oSet(0, 2, this.bounds.oGet(1).oGet(2));
            // convex model
            this.isConvex = true;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::SetupCylinder

         The origin is placed at the center of the cylinder.
         ============
         */
        public void SetupCylinder(final float height, final float width, final int numSides) {
            final idBounds cylBounds = new idBounds();
            float halfHeight, halfWidth;

            halfHeight = height * 0.5f;
            halfWidth = width * 0.5f;
            cylBounds.oGet(0).Set(-halfWidth, -halfWidth, -halfHeight);
            cylBounds.oGet(1).Set(halfWidth, halfWidth, halfHeight);
            SetupCylinder(cylBounds, numSides);
        }

        // cone approximation
        public void SetupCone(final idBounds coneBounds, final int numSides) {
            int i, n, ii;
            float angle;
            idVec3 halfSize;

            n = numSides;
            if (n < 2) {
                n = 3;
            }
            if ((n + 1) > MAX_TRACEMODEL_VERTS) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCone: too many vertices\n");
                n = MAX_TRACEMODEL_VERTS - 1;
            }
            if ((n * 2) > MAX_TRACEMODEL_EDGES) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCone: too many edges\n");
                n = MAX_TRACEMODEL_EDGES / 2;
            }
            if ((n + 1) > MAX_TRACEMODEL_POLYS) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCone: too many polygons\n");
                n = MAX_TRACEMODEL_POLYS - 1;
            }

            this.type = TRM_CONE;
            this.numVerts = n + 1;
            this.numEdges = n * 2;
            this.numPolys = n + 1;
            this.offset = coneBounds.oGet(0).oPlus(coneBounds.oGet(1)).oMultiply(0.5f);
            halfSize = coneBounds.oGet(1).oMinus(this.offset);
            this.verts[n].Set(0.0f, 0.0f, halfSize.z + this.offset.z);
            for (i = 0; i < n; i++) {
                // verts
                angle = (idMath.TWO_PI * i) / n;
                this.verts[i].x = (float) ((Math.cos(angle) * halfSize.x) + this.offset.x);
                this.verts[i].y = (float) ((Math.sin(angle) * halfSize.y) + this.offset.y);
                this.verts[i].z = -halfSize.z + this.offset.z;
                // edges
                ii = i + 1;
                this.edges[ii].v[0] = i;
                this.edges[ii].v[1] = ii % n;
                this.edges[n + ii].v[0] = i;
                this.edges[n + ii].v[1] = n;
                // vertical polygon edges
                this.polys[i].numEdges = 3;
                this.polys[i].edges[0] = ii;
                this.polys[i].edges[1] = n + (ii % n) + 1;
                this.polys[i].edges[2] = -(n + ii);
                // bottom polygon edges
                this.polys[n].edges[i] = -(n - i);
            }
            // bottom polygon numEdges
            this.polys[n].numEdges = n;

            // polygons
            for (i = 0; i < n; i++) {
                // polygon plane
                this.polys[i].normal.oSet((this.verts[(i + 1) % n].oMinus(this.verts[i])).Cross(this.verts[n].oMinus(this.verts[i])));
                this.polys[i].normal.Normalize();
                this.polys[i].dist = this.polys[i].normal.oMultiply(this.verts[i]);
                // polygon bounds
                this.polys[i].bounds.Clear();
                this.polys[i].bounds.AddPoint(this.verts[i]);
                this.polys[i].bounds.AddPoint(this.verts[(i + 1) % n]);
                this.polys[i].bounds.AddPoint(this.verts[n]);
            }
            // bottom polygon plane
            this.polys[n].normal.Set(0.0f, 0.0f, -1.0f);
            this.polys[n].dist = -coneBounds.oGet(0).oGet(2);
            // trm bounds
            this.bounds = coneBounds;
            // bottom polygon bounds
            this.polys[n].bounds.oSet(this.bounds);
            this.polys[n].bounds.oSet(1, 2, this.bounds.oGet(0).oGet(2));
            // convex model
            this.isConvex = true;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::SetupCone

         The origin is placed at the apex of the cone.
         ============
         */
        public void SetupCone(final float height, final float width, final int numSides) {
            final idBounds coneBounds = new idBounds();
            float halfWidth;

            halfWidth = width * 0.5f;
            coneBounds.oGet(0).Set(-halfWidth, -halfWidth, -height);
            coneBounds.oGet(1).Set(halfWidth, halfWidth, 0.0f);
            SetupCone(coneBounds, numSides);
        }

        /*
         ============
         idTraceModel::SetupBone

         The origin is placed at the center of the bone.
         ============
         */
        public void SetupBone(final float length, final float width) {// two tetrahedrons attached to each other
            int i, j, edgeNum;
            final float halfLength = (float) (length * 0.5);

            if (this.type != TRM_BONE) {
                InitBone();
            }
            // offset to center
            this.offset.Set(0.0f, 0.0f, 0.0f);
            // set vertices
            this.verts[0].Set(0.0f, 0.0f, -halfLength);
            this.verts[1].Set(0.0f, width * -0.5f, 0.0f);
            this.verts[2].Set(width * 0.5f, width * 0.25f, 0.0f);
            this.verts[3].Set(width * -0.5f, width * 0.25f, 0.0f);
            this.verts[4].Set(0.0f, 0.0f, halfLength);
            // set bounds
            this.bounds.oGet(0).Set(width * -0.5f, width * -0.5f, -halfLength);
            this.bounds.oGet(1).Set(width * 0.5f, width * 0.25f, halfLength);
            // poly plane normals
            this.polys[0].normal.oSet((this.verts[2].oMinus(this.verts[0])).Cross(this.verts[1].oMinus(this.verts[0])));
            this.polys[0].normal.Normalize();
            this.polys[2].normal.Set(-this.polys[0].normal.oGet(0), this.polys[0].normal.oGet(1), this.polys[0].normal.oGet(2));
            this.polys[3].normal.Set(this.polys[0].normal.oGet(0), this.polys[0].normal.oGet(1), -this.polys[0].normal.oGet(2));
            this.polys[5].normal.Set(-this.polys[0].normal.oGet(0), this.polys[0].normal.oGet(1), -this.polys[0].normal.oGet(2));
            this.polys[1].normal.oSet((this.verts[3].oMinus(this.verts[0])).Cross(this.verts[2].oMinus(this.verts[0])));
            this.polys[1].normal.Normalize();
            this.polys[4].normal.Set(this.polys[1].normal.oGet(0), this.polys[1].normal.oGet(1), -this.polys[1].normal.oGet(2));
            // poly plane distances
            for (i = 0; i < 6; i++) {
                this.polys[i].dist = this.polys[i].normal.oMultiply(this.verts[ this.edges[ Math.abs(this.polys[i].edges[0])].v[0]]);
                this.polys[i].bounds.Clear();
                for (j = 0; j < 3; j++) {
                    edgeNum = this.polys[i].edges[ j];
                    this.polys[i].bounds.AddPoint(this.verts[ this.edges[Math.abs(edgeNum)].v[edgeNum < 0 ? 1 : 0]]);
                }
            }

            GenerateEdgeNormals();
        }

        // arbitrary convex polygon
        public void SetupPolygon(final idVec3[] v, final int count) {
            int i, j;
            idVec3 mid;

            this.type = TRM_POLYGON;
            this.numVerts = count;
            // times three because we need to be able to turn the polygon into a volume
            if ((this.numVerts * 3) > MAX_TRACEMODEL_EDGES) {
                idLib.common.Printf("WARNING: idTraceModel::SetupPolygon: too many vertices\n");
                this.numVerts = MAX_TRACEMODEL_EDGES / 3;
            }

            this.numEdges = this.numVerts;
            this.numPolys = 2;
            // set polygon planes
            this.polys[0].numEdges = this.numEdges;
            this.polys[0].normal.oSet((v[1].oMinus(v[0])).Cross(v[2].oMinus(v[0])));
            this.polys[0].normal.Normalize();
            this.polys[0].dist = this.polys[0].normal.oMultiply(v[0]);
            this.polys[1].numEdges = this.numEdges;
            this.polys[1].normal.oSet(this.polys[0].normal.oNegative());
            this.polys[1].dist = -this.polys[0].dist;
            // setup verts, edges and polygons
            this.polys[0].bounds.Clear();
            mid = getVec3_origin();
            for (i = 0, j = 1; i < this.numVerts; i++, j++) {
                if (j >= this.numVerts) {
                    j = 0;
                }
                this.verts[i].oSet(v[i]);
                this.edges[i + 1].v[0] = i;
                this.edges[i + 1].v[1] = j;
                this.edges[i + 1].normal = this.polys[0].normal.Cross(v[i].oMinus(v[j]));
                this.edges[i + 1].normal.Normalize();
                this.polys[0].edges[i] = i + 1;
                this.polys[1].edges[i] = -(this.numVerts - i);
                this.polys[0].bounds.AddPoint(this.verts[i]);
                mid.oPluSet(v[i]);
            }
            this.polys[1].bounds.oSet(this.polys[0].bounds);
            // offset to center
            this.offset = mid.oMultiply((1.0f / this.numVerts));
            // total bounds
            this.bounds = this.polys[0].bounds;
            // considered non convex because the model has no volume
            this.isConvex = false;
        }

        public void SetupPolygon(final idWinding w) {
            int i;
            idVec3[] verts;

            verts = new idVec3[w.GetNumPoints()];
            for (i = 0; i < w.GetNumPoints(); i++) {
                verts[i] = w.oGet(i).ToVec3();
            }
            SetupPolygon(verts, w.GetNumPoints());
        }
        static final float SHARP_EDGE_DOT = -0.7f;

        // generate edge normals
        public int GenerateEdgeNormals() {
            int i, j, edgeNum, numSharpEdges;
            float dot;
            idVec3 dir;
            traceModelPoly_t poly;
            traceModelEdge_t edge;

            for (i = 0; i <= this.numEdges; i++) {
                this.edges[i].normal.Zero();
            }

            numSharpEdges = 0;
            for (i = 0; i < this.numPolys; i++) {
                poly = this.polys[i];
                for (j = 0; j < poly.numEdges; j++) {
                    edgeNum = poly.edges[j];
                    edge = this.edges[Math.abs(edgeNum)];
                    if ((edge.normal.oGet(0) == 0.0f) && (edge.normal.oGet(1) == 0.0f) && (edge.normal.oGet(2) == 0.0f)) {
                        edge.normal = poly.normal;
                    } else {
                        dot = edge.normal.oMultiply(poly.normal);
                        // if the two planes make a very sharp edge
                        if (dot < SHARP_EDGE_DOT) {
                            // max length normal pointing outside both polygons
                            dir = this.verts[ edge.v[edgeNum > 0 ? 1 : 0]].oMinus(this.verts[ edge.v[edgeNum < 0 ? 1 : 0]]);
                            edge.normal = edge.normal.Cross(dir).oPlus(poly.normal.Cross(dir.oNegative()));
                            edge.normal.oMulSet((0.5f / (0.5f + (0.5f * SHARP_EDGE_DOT))) / edge.normal.Length());
                            numSharpEdges++;
                        } else {
                            edge.normal = (edge.normal.oPlus(poly.normal)).oMultiply(0.5f / (0.5f + (0.5f * dot)));
                        }
                    }
                }
            }
            return numSharpEdges;
        }

        // translate the trm
        public void Translate(final idVec3 translation) {
            int i;

            for (i = 0; i < this.numVerts; i++) {
                this.verts[i].oPluSet(translation);
            }
            for (i = 0; i < this.numPolys; i++) {
                this.polys[i].dist += this.polys[i].normal.oMultiply(translation);
                this.polys[i].bounds.oPluSet(translation);
            }
            this.offset.oPluSet(translation);
            this.bounds.oPluSet(translation);
        }

        // rotate the trm
        public void Rotate(final idMat3 rotation) {
            int i, j, edgeNum;

            for (i = 0; i < this.numVerts; i++) {
                this.verts[i].oSet(rotation.oMultiply(this.verts[i]));
            }

            this.bounds.Clear();
            for (i = 0; i < this.numPolys; i++) {
                this.polys[i].normal.oSet(rotation.oMultiply(this.polys[i].normal));
                this.polys[i].bounds.Clear();
                edgeNum = 0;
                for (j = 0; j < this.polys[i].numEdges; j++) {
                    edgeNum = this.polys[i].edges[j];
                    this.polys[i].bounds.AddPoint(this.verts[this.edges[Math.abs(edgeNum)].v[INTSIGNBITSET(edgeNum)]]);
                }
                this.polys[i].dist = this.polys[i].normal.oMultiply(this.verts[this.edges[Math.abs(edgeNum)].v[INTSIGNBITSET(edgeNum)]]);
                this.bounds.oPluSet(this.polys[i].bounds);
            }

            GenerateEdgeNormals();
        }

        // shrink the model m units on all sides
        public void Shrink(final float m) {
            int i, j, edgeNum;
            traceModelEdge_t edge;
            idVec3 dir;

            if (this.type == TRM_POLYGON) {
                for (i = 0; i < this.numEdges; i++) {
                    edgeNum = this.polys[0].edges[i];
                    edge = this.edges[Math.abs(edgeNum)];
                    dir = this.verts[ edge.v[ INTSIGNBITSET(edgeNum)]].oMinus(this.verts[ edge.v[ INTSIGNBITNOTSET(edgeNum)]]);
                    if (dir.Normalize() < (2.0f * m)) {
                        continue;
                    }
                    dir.oMulSet(m);
                    this.verts[ edge.v[ 0]].oMinSet(dir);
                    this.verts[ edge.v[ 1]].oPluSet(dir);
                }
                return;
            }

            for (i = 0; i < this.numPolys; i++) {
                this.polys[i].dist -= m;

                for (j = 0; j < this.polys[i].numEdges; j++) {
                    edgeNum = this.polys[i].edges[j];
                    edge = this.edges[Math.abs(edgeNum)];
                    this.verts[ edge.v[ INTSIGNBITSET(edgeNum)]].oMinSet(this.polys[i].normal.oMultiply(m));
                }
            }
        }

        // compare
        public boolean Compare(final idTraceModel trm) {
            int i;

            if ((this.type != trm.type) || (this.numVerts != trm.numVerts)
                    || (this.numEdges != trm.numEdges) || (this.numPolys != trm.numPolys)) {
                return false;
            }
            if ((this.bounds != trm.bounds) || (this.offset != trm.offset)) {
                return false;
            }

            switch (this.type) {
                case TRM_INVALID:
                case TRM_BOX:
                case TRM_OCTAHEDRON:
                case TRM_DODECAHEDRON:
                case TRM_CYLINDER:
                case TRM_CONE:
                    break;
                case TRM_BONE:
                case TRM_POLYGON:
                case TRM_POLYGONVOLUME:
                case TRM_CUSTOM:
                    for (i = 0; i < trm.numVerts; i++) {
                        if (this.verts[i] != trm.verts[i]) {
                            return false;
                        }
                    }
                    break;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = (19 * hash) + (this.type != null ? this.type.hashCode() : 0);
            hash = (19 * hash) + this.numVerts;
            hash = (19 * hash) + Arrays.deepHashCode(this.verts);
            hash = (19 * hash) + this.numEdges;
            hash = (19 * hash) + this.numPolys;
            hash = (19 * hash) + Objects.hashCode(this.offset);
            hash = (19 * hash) + Objects.hashCode(this.bounds);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final idTraceModel other = (idTraceModel) obj;
            if (this.type != other.type) {
                return false;
            }
            if (this.numVerts != other.numVerts) {
                return false;
            }
            if (!Arrays.deepEquals(this.verts, other.verts)) {
                return false;
            }
            if (this.numEdges != other.numEdges) {
                return false;
            }
            if (this.numPolys != other.numPolys) {
                return false;
            }
            if (!Objects.equals(this.offset, other.offset)) {
                return false;
            }
            if (!Objects.equals(this.bounds, other.bounds)) {
                return false;
            }
            return true;
        }

        //public	bool				operator==(	const idTraceModel &trm ) const;
        //public	bool				operator!=(	const idTraceModel &trm ) const;
        //
        // get the area of one of the polygons
        public float GetPolygonArea(int polyNum) {
            int i;
            idVec3 base, v1, v2, cross;
            float total;
            final traceModelPoly_t poly;

            if ((polyNum < 0) || (polyNum >= this.numPolys)) {
                return 0.0f;
            }
            poly = this.polys[polyNum];
            total = 0.0f;
            base = this.verts[ this.edges[ Math.abs(poly.edges[0])].v[ INTSIGNBITSET(poly.edges[0])]];
            for (i = 0; i < poly.numEdges; i++) {
                v1 = this.verts[ this.edges[ Math.abs(poly.edges[i])].v[ INTSIGNBITSET(poly.edges[i])]].oMinus(base);
                v2 = this.verts[ this.edges[ Math.abs(poly.edges[i])].v[ INTSIGNBITNOTSET(poly.edges[i])]].oMinus(base);
                cross = v1.Cross(v2);
                total += cross.Length();
            }
            return total * 0.5f;
        }

        // get the silhouette edges
        public int GetProjectionSilhouetteEdges(final idVec3 projectionOrigin, int silEdges[]) {
            int i, j, edgeNum;
            final int[] edgeIsSilEdge = new int[MAX_TRACEMODEL_EDGES + 1];
            traceModelPoly_t poly;
            idVec3 dir;

//	memset( edgeIsSilEdge, 0, sizeof( edgeIsSilEdge ) );

            for (i = 0; i < this.numPolys; i++) {
                poly = this.polys[i];
                edgeNum = poly.edges[0];
                dir = this.verts[ this.edges[Math.abs(edgeNum)].v[ INTSIGNBITSET(edgeNum)]].oMinus(projectionOrigin);
                if (dir.oMultiply(poly.normal) < 0.0f) {
                    for (j = 0; j < poly.numEdges; j++) {
                        edgeNum = poly.edges[j];
                        edgeIsSilEdge[Math.abs(edgeNum)] ^= 1;
                    }
                }
            }

            return GetOrderedSilhouetteEdges(edgeIsSilEdge, silEdges);
        }

        public int GetParallelProjectionSilhouetteEdges(final idVec3 projectionDir, int silEdges[]) {
            int i, j, edgeNum;
            final int[] edgeIsSilEdge = new int[MAX_TRACEMODEL_EDGES + 1];
            traceModelPoly_t poly;

//	memset( edgeIsSilEdge, 0, sizeof( edgeIsSilEdge ) );

            for (i = 0; i < this.numPolys; i++) {
                poly = this.polys[i];
                if (projectionDir.oMultiply(poly.normal) < 0.0f) {
                    for (j = 0; j < poly.numEdges; j++) {
                        edgeNum = poly.edges[j];
                        edgeIsSilEdge[Math.abs(edgeNum)] ^= 1;
                    }
                }
            }

            return GetOrderedSilhouetteEdges(edgeIsSilEdge, silEdges);
        }

        private static int DBG_GetMassProperties = 0;
        // calculate mass properties assuming an uniform density
        public void GetMassProperties(final float density, float[] mass, idVec3 centerOfMass, idMat3 inertiaTensor) {
            final volumeIntegrals_t integrals = new volumeIntegrals_t();DBG_GetMassProperties++;

            // if polygon trace model
            if (this.type == TRM_POLYGON) {
                final idTraceModel trm = new idTraceModel();

                VolumeFromPolygon(trm, 1.0f);
                trm.GetMassProperties(density, mass, centerOfMass, inertiaTensor);
                return;
            }

            VolumeIntegrals(integrals);

            // if no volume
            if (integrals.T0 == 0.0f) {
                mass[0] = 1.0f;
                centerOfMass.Zero();
                inertiaTensor.Identity();
                return;
            }

            // mass of model
            mass[0] = density * integrals.T0;
            // center of mass
            centerOfMass.oSet(integrals.T1.oDivide(integrals.T0));
            // compute inertia tensor
            inertiaTensor.oSet(0, 0, density * (integrals.T2.oGet(1) + integrals.T2.oGet(2)));
            inertiaTensor.oSet(1, 1, density * (integrals.T2.oGet(2) + integrals.T2.oGet(0)));
            inertiaTensor.oSet(2, 2, density * (integrals.T2.oGet(0) + integrals.T2.oGet(1)));
            inertiaTensor.oSet(0, 1, -density * integrals.TP.oGet(0));
            inertiaTensor.oSet(1, 0, -density * integrals.TP.oGet(0));
            inertiaTensor.oSet(1, 2, -density * integrals.TP.oGet(1));
            inertiaTensor.oSet(2, 1, -density * integrals.TP.oGet(1));
            inertiaTensor.oSet(2, 0, -density * integrals.TP.oGet(2));
            inertiaTensor.oSet(0, 2, -density * integrals.TP.oGet(2));
            // translate inertia tensor to center of mass
            inertiaTensor.oMinSet(0, 0, mass[0] * ((centerOfMass.oGet(1) * centerOfMass.oGet(1)) + (centerOfMass.oGet(2) * centerOfMass.oGet(2))));
            inertiaTensor.oMinSet(1, 1, mass[0] * ((centerOfMass.oGet(2) * centerOfMass.oGet(2)) + (centerOfMass.oGet(0) * centerOfMass.oGet(0))));
            inertiaTensor.oMinSet(2, 2, mass[0] * ((centerOfMass.oGet(0) * centerOfMass.oGet(0)) + (centerOfMass.oGet(1) * centerOfMass.oGet(1))));
            inertiaTensor.oPluSet(0, 1, mass[0] * centerOfMass.oGet(0) * centerOfMass.oGet(1));
            inertiaTensor.oPluSet(1, 0, mass[0] * centerOfMass.oGet(0) * centerOfMass.oGet(1));
            inertiaTensor.oPluSet(1, 2, mass[0] * centerOfMass.oGet(1) * centerOfMass.oGet(2));
            inertiaTensor.oPluSet(2, 1, mass[0] * centerOfMass.oGet(1) * centerOfMass.oGet(2));
            inertiaTensor.oPluSet(2, 0, mass[0] * centerOfMass.oGet(2) * centerOfMass.oGet(0));
            inertiaTensor.oPluSet(0, 2, mass[0] * centerOfMass.oGet(2) * centerOfMass.oGet(0));
        }

        //
        /*
         ============
         idTraceModel::InitBox

         Initialize size independent box.
         ============
         */
        private void InitBox() {
            int i;

            this.type = TRM_BOX;
            this.numVerts = 8;
            this.numEdges = 12;
            this.numPolys = 6;

            // set box edges
            for (i = 0; i < 4; i++) {
                this.edges[i + 1].v[0] = i;
                this.edges[i + 1].v[1] = (i + 1) & 3;
                this.edges[i + 5].v[0] = 4 + i;
                this.edges[i + 5].v[1] = 4 + ((i + 1) & 3);
                this.edges[i + 9].v[0] = i;
                this.edges[i + 9].v[1] = 4 + i;
            }

            // all edges of a polygon go counter clockwise
            this.polys[0].numEdges = 4;
            this.polys[0].edges[0] = -4;
            this.polys[0].edges[1] = -3;
            this.polys[0].edges[2] = -2;
            this.polys[0].edges[3] = -1;
            this.polys[0].normal.Set(0.0f, 0.0f, -1.0f);

            this.polys[1].numEdges = 4;
            this.polys[1].edges[0] = 5;
            this.polys[1].edges[1] = 6;
            this.polys[1].edges[2] = 7;
            this.polys[1].edges[3] = 8;
            this.polys[1].normal.Set(0.0f, 0.0f, 1.0f);

            this.polys[2].numEdges = 4;
            this.polys[2].edges[0] = 1;
            this.polys[2].edges[1] = 10;
            this.polys[2].edges[2] = -5;
            this.polys[2].edges[3] = -9;
            this.polys[2].normal.Set(0.0f, -1.0f, 0.0f);

            this.polys[3].numEdges = 4;
            this.polys[3].edges[0] = 2;
            this.polys[3].edges[1] = 11;
            this.polys[3].edges[2] = -6;
            this.polys[3].edges[3] = -10;
            this.polys[3].normal.Set(1.0f, 0.0f, 0.0f);

            this.polys[4].numEdges = 4;
            this.polys[4].edges[0] = 3;
            this.polys[4].edges[1] = 12;
            this.polys[4].edges[2] = -7;
            this.polys[4].edges[3] = -11;
            this.polys[4].normal.Set(0.0f, 1.0f, 0.0f);

            this.polys[5].numEdges = 4;
            this.polys[5].edges[0] = 4;
            this.polys[5].edges[1] = 9;
            this.polys[5].edges[2] = -8;
            this.polys[5].edges[3] = -12;
            this.polys[5].normal.Set(-1.0f, 0.0f, 0.0f);

            // convex model
            this.isConvex = true;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::InitOctahedron

         Initialize size independent octahedron.
         ============
         */
        private void InitOctahedron() {

            this.type = TRM_OCTAHEDRON;
            this.numVerts = 6;
            this.numEdges = 12;
            this.numPolys = 8;

            // set edges
            this.edges[ 1].v[0] = 4;
            this.edges[ 1].v[1] = 0;
            this.edges[ 2].v[0] = 0;
            this.edges[ 2].v[1] = 2;
            this.edges[ 3].v[0] = 2;
            this.edges[ 3].v[1] = 4;
            this.edges[ 4].v[0] = 2;
            this.edges[ 4].v[1] = 1;
            this.edges[ 5].v[0] = 1;
            this.edges[ 5].v[1] = 4;
            this.edges[ 6].v[0] = 1;
            this.edges[ 6].v[1] = 3;
            this.edges[ 7].v[0] = 3;
            this.edges[ 7].v[1] = 4;
            this.edges[ 8].v[0] = 3;
            this.edges[ 8].v[1] = 0;
            this.edges[ 9].v[0] = 5;
            this.edges[ 9].v[1] = 2;
            this.edges[10].v[0] = 0;
            this.edges[10].v[1] = 5;
            this.edges[11].v[0] = 5;
            this.edges[11].v[1] = 1;
            this.edges[12].v[0] = 5;
            this.edges[12].v[1] = 3;

            // all edges of a polygon go counter clockwise
            this.polys[0].numEdges = 3;
            this.polys[0].edges[0] = 1;
            this.polys[0].edges[1] = 2;
            this.polys[0].edges[2] = 3;

            this.polys[1].numEdges = 3;
            this.polys[1].edges[0] = -3;
            this.polys[1].edges[1] = 4;
            this.polys[1].edges[2] = 5;

            this.polys[2].numEdges = 3;
            this.polys[2].edges[0] = -5;
            this.polys[2].edges[1] = 6;
            this.polys[2].edges[2] = 7;

            this.polys[3].numEdges = 3;
            this.polys[3].edges[0] = -7;
            this.polys[3].edges[1] = 8;
            this.polys[3].edges[2] = -1;

            this.polys[4].numEdges = 3;
            this.polys[4].edges[0] = 9;
            this.polys[4].edges[1] = -2;
            this.polys[4].edges[2] = 10;

            this.polys[5].numEdges = 3;
            this.polys[5].edges[0] = 11;
            this.polys[5].edges[1] = -4;
            this.polys[5].edges[2] = -9;

            this.polys[6].numEdges = 3;
            this.polys[6].edges[0] = 12;
            this.polys[6].edges[1] = -6;
            this.polys[6].edges[2] = -11;

            this.polys[7].numEdges = 3;
            this.polys[7].edges[0] = -10;
            this.polys[7].edges[1] = -8;
            this.polys[7].edges[2] = -12;

            // convex model
            this.isConvex = true;
        }

        /*
         ============
         idTraceModel::InitDodecahedron

         Initialize size independent dodecahedron.
         ============
         */
        private void InitDodecahedron() {

            this.type = TRM_DODECAHEDRON;
            this.numVerts = 20;
            this.numEdges = 30;
            this.numPolys = 12;

            // set edges
            this.edges[ 1].v[0] = 0;
            this.edges[ 1].v[1] = 8;
            this.edges[ 2].v[0] = 8;
            this.edges[ 2].v[1] = 9;
            this.edges[ 3].v[0] = 9;
            this.edges[ 3].v[1] = 4;
            this.edges[ 4].v[0] = 4;
            this.edges[ 4].v[1] = 16;
            this.edges[ 5].v[0] = 16;
            this.edges[ 5].v[1] = 0;
            this.edges[ 6].v[0] = 16;
            this.edges[ 6].v[1] = 17;
            this.edges[ 7].v[0] = 17;
            this.edges[ 7].v[1] = 2;
            this.edges[ 8].v[0] = 2;
            this.edges[ 8].v[1] = 12;
            this.edges[ 9].v[0] = 12;
            this.edges[ 9].v[1] = 0;
            this.edges[10].v[0] = 2;
            this.edges[10].v[1] = 10;
            this.edges[11].v[0] = 10;
            this.edges[11].v[1] = 3;
            this.edges[12].v[0] = 3;
            this.edges[12].v[1] = 13;
            this.edges[13].v[0] = 13;
            this.edges[13].v[1] = 12;
            this.edges[14].v[0] = 9;
            this.edges[14].v[1] = 5;
            this.edges[15].v[0] = 5;
            this.edges[15].v[1] = 15;
            this.edges[16].v[0] = 15;
            this.edges[16].v[1] = 14;
            this.edges[17].v[0] = 14;
            this.edges[17].v[1] = 4;
            this.edges[18].v[0] = 3;
            this.edges[18].v[1] = 19;
            this.edges[19].v[0] = 19;
            this.edges[19].v[1] = 18;
            this.edges[20].v[0] = 18;
            this.edges[20].v[1] = 1;
            this.edges[21].v[0] = 1;
            this.edges[21].v[1] = 13;
            this.edges[22].v[0] = 7;
            this.edges[22].v[1] = 11;
            this.edges[23].v[0] = 11;
            this.edges[23].v[1] = 6;
            this.edges[24].v[0] = 6;
            this.edges[24].v[1] = 14;
            this.edges[25].v[0] = 15;
            this.edges[25].v[1] = 7;
            this.edges[26].v[0] = 1;
            this.edges[26].v[1] = 8;
            this.edges[27].v[0] = 18;
            this.edges[27].v[1] = 5;
            this.edges[28].v[0] = 6;
            this.edges[28].v[1] = 17;
            this.edges[29].v[0] = 11;
            this.edges[29].v[1] = 10;
            this.edges[30].v[0] = 19;
            this.edges[30].v[1] = 7;

            // all edges of a polygon go counter clockwise
            this.polys[0].numEdges = 5;
            this.polys[0].edges[0] = 1;
            this.polys[0].edges[1] = 2;
            this.polys[0].edges[2] = 3;
            this.polys[0].edges[3] = 4;
            this.polys[0].edges[4] = 5;

            this.polys[1].numEdges = 5;
            this.polys[1].edges[0] = -5;
            this.polys[1].edges[1] = 6;
            this.polys[1].edges[2] = 7;
            this.polys[1].edges[3] = 8;
            this.polys[1].edges[4] = 9;

            this.polys[2].numEdges = 5;
            this.polys[2].edges[0] = -8;
            this.polys[2].edges[1] = 10;
            this.polys[2].edges[2] = 11;
            this.polys[2].edges[3] = 12;
            this.polys[2].edges[4] = 13;

            this.polys[3].numEdges = 5;
            this.polys[3].edges[0] = 14;
            this.polys[3].edges[1] = 15;
            this.polys[3].edges[2] = 16;
            this.polys[3].edges[3] = 17;
            this.polys[3].edges[4] = -3;

            this.polys[4].numEdges = 5;
            this.polys[4].edges[0] = 18;
            this.polys[4].edges[1] = 19;
            this.polys[4].edges[2] = 20;
            this.polys[4].edges[3] = 21;
            this.polys[4].edges[4] = -12;

            this.polys[5].numEdges = 5;
            this.polys[5].edges[0] = 22;
            this.polys[5].edges[1] = 23;
            this.polys[5].edges[2] = 24;
            this.polys[5].edges[3] = -16;
            this.polys[5].edges[4] = 25;

            this.polys[6].numEdges = 5;
            this.polys[6].edges[0] = -9;
            this.polys[6].edges[1] = -13;
            this.polys[6].edges[2] = -21;
            this.polys[6].edges[3] = 26;
            this.polys[6].edges[4] = -1;

            this.polys[7].numEdges = 5;
            this.polys[7].edges[0] = -26;
            this.polys[7].edges[1] = -20;
            this.polys[7].edges[2] = 27;
            this.polys[7].edges[3] = -14;
            this.polys[7].edges[4] = -2;

            this.polys[8].numEdges = 5;
            this.polys[8].edges[0] = -4;
            this.polys[8].edges[1] = -17;
            this.polys[8].edges[2] = -24;
            this.polys[8].edges[3] = 28;
            this.polys[8].edges[4] = -6;

            this.polys[9].numEdges = 5;
            this.polys[9].edges[0] = -23;
            this.polys[9].edges[1] = 29;
            this.polys[9].edges[2] = -10;
            this.polys[9].edges[3] = -7;
            this.polys[9].edges[4] = -28;

            this.polys[10].numEdges = 5;
            this.polys[10].edges[0] = -25;
            this.polys[10].edges[1] = -15;
            this.polys[10].edges[2] = -27;
            this.polys[10].edges[3] = -19;
            this.polys[10].edges[4] = 30;

            this.polys[11].numEdges = 5;
            this.polys[11].edges[0] = -30;
            this.polys[11].edges[1] = -18;
            this.polys[11].edges[2] = -11;
            this.polys[11].edges[3] = -29;
            this.polys[11].edges[4] = -22;

            // convex model
            this.isConvex = true;
        }

        /*
         ============
         idTraceModel::InitBone

         Initialize size independent bone.
         ============
         */
        private void InitBone() {
            int i;

            this.type = TRM_BONE;
            this.numVerts = 5;
            this.numEdges = 9;
            this.numPolys = 6;

            // set bone edges
            for (i = 0; i < 3; i++) {
                this.edges[ i + 1].v[0] = 0;
                this.edges[ i + 1].v[1] = i + 1;
                this.edges[ i + 4].v[0] = 1 + i;
                this.edges[ i + 4].v[1] = 1 + ((i + 1) % 3);
                this.edges[ i + 7].v[0] = i + 1;
                this.edges[ i + 7].v[1] = 4;
            }

            // all edges of a polygon go counter clockwise
            this.polys[0].numEdges = 3;
            this.polys[0].edges[0] = 2;
            this.polys[0].edges[1] = -4;
            this.polys[0].edges[2] = -1;

            this.polys[1].numEdges = 3;
            this.polys[1].edges[0] = 3;
            this.polys[1].edges[1] = -5;
            this.polys[1].edges[2] = -2;

            this.polys[2].numEdges = 3;
            this.polys[2].edges[0] = 1;
            this.polys[2].edges[1] = -6;
            this.polys[2].edges[2] = -3;

            this.polys[3].numEdges = 3;
            this.polys[3].edges[0] = 4;
            this.polys[3].edges[1] = 8;
            this.polys[3].edges[2] = -7;

            this.polys[4].numEdges = 3;
            this.polys[4].edges[0] = 5;
            this.polys[4].edges[1] = 9;
            this.polys[4].edges[2] = -8;

            this.polys[5].numEdges = 3;
            this.polys[5].edges[0] = 6;
            this.polys[5].edges[1] = 7;
            this.polys[5].edges[2] = -9;

            // convex model
            this.isConvex = true;
        }

        private void oSet(idTraceModel trm) {
            this.type = trm.type;
            this.numVerts = trm.numVerts;
            for (int i = 0; i < this.numVerts; i++) {
                this.verts[i].oSet(trm.verts[i]);
            }
            this.numEdges = trm.numEdges;
            for (int i = 0; i < this.numEdges; i++) {
                this.edges[i].oSet(trm.edges[i]);
            }
            this.numPolys = trm.numPolys;
            for (int i = 0; i < this.numPolys; i++) {
                this.polys[i].oSet(trm.polys[i]);
            }
            this.offset = trm.offset;
            this.bounds.oSet(trm.bounds);
            this.isConvex = trm.isConvex;
        }

        class projectionIntegrals_t {

            float P1;
            float Pa, Pb;
            float Paa, Pab, Pbb;
            float Paaa, Paab, Pabb, Pbbb;
        }

        private void ProjectionIntegrals(int polyNum, int a, int b, projectionIntegrals_t integrals) {
            traceModelPoly_t poly;
            int i, edgeNum;
            idVec3 v1, v2;
            float a0, a1, da;
            float b0, b1, db;
            float a0_2, a0_3, a0_4, b0_2, b0_3, b0_4;
            float a1_2, a1_3, b1_2, b1_3;
            float C1, Ca, Caa, Caaa, Cb, Cbb, Cbbb;
            float Cab, Kab, Caab, Kaab, Cabb, Kabb;

//	memset(&integrals, 0, sizeof(projectionIntegrals_t));
            poly = this.polys[polyNum];
            for (i = 0; i < poly.numEdges; i++) {
                edgeNum = poly.edges[i];
                v1 = this.verts[ this.edges[ Math.abs(edgeNum)].v[ edgeNum < 0 ? 1 : 0]];
                v2 = this.verts[ this.edges[ Math.abs(edgeNum)].v[ edgeNum > 0 ? 1 : 0]];
                a0 = v1.oGet(a);
                b0 = v1.oGet(b);
                a1 = v2.oGet(a);
                b1 = v2.oGet(b);
                da = a1 - a0;
                db = b1 - b0;
                a0_2 = a0 * a0;
                a0_3 = a0_2 * a0;
                a0_4 = a0_3 * a0;
                b0_2 = b0 * b0;
                b0_3 = b0_2 * b0;
                b0_4 = b0_3 * b0;
                a1_2 = a1 * a1;
                a1_3 = a1_2 * a1;
                b1_2 = b1 * b1;
                b1_3 = b1_2 * b1;

                C1 = a1 + a0;
                Ca = (a1 * C1) + a0_2;
                Caa = (a1 * Ca) + a0_3;
                Caaa = (a1 * Caa) + a0_4;
                Cb = (b1 * (b1 + b0)) + b0_2;
                Cbb = (b1 * Cb) + b0_3;
                Cbbb = (b1 * Cbb) + b0_4;
                Cab = (3 * a1_2) + (2 * a1 * a0) + a0_2;
                Kab = a1_2 + (2 * a1 * a0) + (3 * a0_2);
                Caab = (a0 * Cab) + (4 * a1_3);
                Kaab = (a1 * Kab) + (4 * a0_3);
                Cabb = (4 * b1_3) + (3 * b1_2 * b0) + (2 * b1 * b0_2) + b0_3;
                Kabb = b1_3 + (2 * b1_2 * b0) + (3 * b1 * b0_2) + (4 * b0_3);

                integrals.P1 += db * C1;
                integrals.Pa += db * Ca;
                integrals.Paa += db * Caa;
                integrals.Paaa += db * Caaa;
                integrals.Pb += da * Cb;
                integrals.Pbb += da * Cbb;
                integrals.Pbbb += da * Cbbb;
                integrals.Pab += db * ((b1 * Cab) + (b0 * Kab));
                integrals.Paab += db * ((b1 * Caab) + (b0 * Kaab));
                integrals.Pabb += da * ((a1 * Cabb) + (a0 * Kabb));
            }

            integrals.P1 *= (1.0f / 2.0f);
            integrals.Pa *= (1.0f / 6.0f);
            integrals.Paa *= (1.0f / 12.0f);
            integrals.Paaa *= (1.0f / 20.0f);
            integrals.Pb *= (1.0f / -6.0f);
            integrals.Pbb *= (1.0f / -12.0f);
            integrals.Pbbb *= (1.0f / -20.0f);
            integrals.Pab *= (1.0f / 24.0f);
            integrals.Paab *= (1.0f / 60.0f);
            integrals.Pabb *= (1.0f / -60.0f);
        }

        class polygonIntegrals_t {

            float Fa,   Fb,   Fc;
            float Faa,  Fbb,  Fcc;
            float Faaa, Fbbb, Fccc;
            float Faab, Fbbc, Fcca;
        }

        private void PolygonIntegrals(int polyNum, int a, int b, int c, polygonIntegrals_t integrals) {
            final projectionIntegrals_t pi = new projectionIntegrals_t();
            idVec3 n;
            float w;
            float k1, k2, k3, k4;

            ProjectionIntegrals(polyNum, a, b, pi);

            n = this.polys[polyNum].normal;
            w = -this.polys[polyNum].dist;
            k1 = 1 / n.oGet(c);
            k2 = k1 * k1;
            k3 = k2 * k1;
            k4 = k3 * k1;

            integrals.Fa = k1 * pi.Pa;
            integrals.Fb = k1 * pi.Pb;
            integrals.Fc = -k2 * ((n.oGet(a) * pi.Pa) + (n.oGet(b) * pi.Pb) + (w * pi.P1));

            integrals.Faa = k1 * pi.Paa;
            integrals.Fbb = k1 * pi.Pbb;
            integrals.Fcc = k3 * ((Square(n.oGet(a)) * pi.Paa) + (2 * n.oGet(a) * n.oGet(b) * pi.Pab) + (Square(n.oGet(b)) * pi.Pbb)
                    + (w * ((2 * ((n.oGet(a) * pi.Pa) + (n.oGet(b) * pi.Pb))) + (w * pi.P1))));

            integrals.Faaa = k1 * pi.Paaa;
            integrals.Fbbb = k1 * pi.Pbbb;
            integrals.Fccc = -k4 * ((Cube(n.oGet(a)) * pi.Paaa) + (3 * Square(n.oGet(a)) * n.oGet(b) * pi.Paab)
                    + (3 * n.oGet(a) * Square(n.oGet(b)) * pi.Pabb) + (Cube(n.oGet(b)) * pi.Pbbb)
                    + (3 * w * ((Square(n.oGet(a)) * pi.Paa) + (2 * n.oGet(a) * n.oGet(b) * pi.Pab) + (Square(n.oGet(b)) * pi.Pbb)))
                    + (w * w * ((3 * ((n.oGet(a) * pi.Pa) + (n.oGet(b) * pi.Pb))) + (w * pi.P1))));

            integrals.Faab = k1 * pi.Paab;
            integrals.Fbbc = -k2 * ((n.oGet(a) * pi.Pabb) + (n.oGet(b) * pi.Pbbb) + (w * pi.Pbb));
            integrals.Fcca = k3 * ((Square(n.oGet(a)) * pi.Paaa) + (2 * n.oGet(a) * n.oGet(b) * pi.Paab) + (Square(n.oGet(b)) * pi.Pabb)
                    + (w * ((2 * ((n.oGet(a) * pi.Paa) + (n.oGet(b) * pi.Pab))) + (w * pi.Pa))));
        }

        class volumeIntegrals_t {

            float T0;
            idVec3 T1;
            idVec3 T2;
            idVec3 TP;

            volumeIntegrals_t() {
                this.T1 = new idVec3();
                this.T2 = new idVec3();
                this.TP = new idVec3();
            }
        }

        private void VolumeIntegrals(volumeIntegrals_t integrals) {
            traceModelPoly_t poly;
            final polygonIntegrals_t pi = new polygonIntegrals_t();
            int i, a, b, c;
            float nx, ny, nz;
            float T0 = 0;
            final float[] T1 = new float[3];
            final float[] T2 = new float[3];
            final float[] TP = new float[3];

//	memset( &integrals, 0, sizeof(volumeIntegrals_t) );
            for (i = 0; i < this.numPolys; i++) {
                poly = this.polys[i];

                nx = idMath.Fabs(poly.normal.oGet(0));
                ny = idMath.Fabs(poly.normal.oGet(1));
                nz = idMath.Fabs(poly.normal.oGet(2));
                if ((nx > ny) && (nx > nz)) {
                    c = 0;
                } else {
                    c = (ny > nz) ? 1 : 2;
                }
                a = (c + 1) % 3;
                b = (a + 1) % 3;

                PolygonIntegrals(i, a, b, c, pi);

                T0 += poly.normal.oGet(0) * ((a == 0) ? pi.Fa : ((b == 0) ? pi.Fb : pi.Fc));

                T1[a] += poly.normal.oGet(a) * pi.Faa;
                T1[b] += poly.normal.oGet(b) * pi.Fbb;
                T1[c] += poly.normal.oGet(c) * pi.Fcc;
                T2[a] += poly.normal.oGet(a) * pi.Faaa;
                T2[b] += poly.normal.oGet(b) * pi.Fbbb;
                T2[c] += poly.normal.oGet(c) * pi.Fccc;
                TP[a] += poly.normal.oGet(a) * pi.Faab;
                TP[b] += poly.normal.oGet(b) * pi.Fbbc;
                TP[c] += poly.normal.oGet(c) * pi.Fcca;
            }

            integrals.T0 = T0;
            integrals.T1.oSet(0, T1[0] * 0.5f);
            integrals.T1.oSet(1, T1[1] * 0.5f);
            integrals.T1.oSet(2, T1[2] * 0.5f);
            integrals.T2.oSet(0, T2[0] * (1.0f / 3.0f));
            integrals.T2.oSet(1, T2[1] * (1.0f / 3.0f));
            integrals.T2.oSet(2, T2[2] * (1.0f / 3.0f));
            integrals.TP.oSet(0, TP[0] * 0.5f);
            integrals.TP.oSet(1, TP[1] * 0.5f);
            integrals.TP.oSet(2, TP[2] * 0.5f);
        }

        private void VolumeFromPolygon(idTraceModel trm, float thickness) {
            int i;

            trm.oSet(this);
            trm.type = TRM_POLYGONVOLUME;
            trm.numVerts = this.numVerts * 2;
            trm.numEdges = this.numEdges * 3;
            trm.numPolys = this.numEdges + 2;
            for (i = 0; i < this.numEdges; i++) {
                trm.verts[this.numVerts + i].oSet(this.verts[i].oMinus(this.polys[0].normal.oMultiply(thickness)));
                trm.edges[ this.numEdges + i + 1].v[0] = this.numVerts + i;
                trm.edges[ this.numEdges + i + 1].v[1] = this.numVerts + ((i + 1) % this.numVerts);
                trm.edges[ (this.numEdges * 2) + i + 1].v[0] = i;
                trm.edges[ (this.numEdges * 2) + i + 1].v[1] = this.numVerts + i;
                trm.polys[1].edges[i] = -(this.numEdges + i + 1);
                trm.polys[2 + i].numEdges = 4;
                trm.polys[2 + i].edges[0] = -(i + 1);
                trm.polys[2 + i].edges[1] = (this.numEdges * 2) + i + 1;
                trm.polys[2 + i].edges[2] = this.numEdges + i + 1;
                trm.polys[2 + i].edges[3] = -((this.numEdges * 2) + ((i + 1) % this.numEdges) + 1);
                trm.polys[2 + i].normal.oSet((this.verts[(i + 1) % this.numVerts].oMinus(this.verts[i])).Cross(this.polys[0].normal));
                trm.polys[2 + i].normal.Normalize();
                trm.polys[2 + i].dist = trm.polys[2 + i].normal.oMultiply(this.verts[i]);
            }
            trm.polys[1].dist = trm.polys[1].normal.oMultiply(trm.verts[ this.numEdges]);

            trm.GenerateEdgeNormals();
        }

        private int GetOrderedSilhouetteEdges(final int edgeIsSilEdge[], int silEdges[]) {
            int i, j, edgeNum, numSilEdges, nextSilVert;
            final int[] unsortedSilEdges = new int[MAX_TRACEMODEL_EDGES];

            numSilEdges = 0;
            for (i = 1; i <= this.numEdges; i++) {
                if (edgeIsSilEdge[i] != 0) {
                    unsortedSilEdges[numSilEdges++] = i;
                }
            }

            silEdges[0] = unsortedSilEdges[0];
            unsortedSilEdges[0] = -1;
            nextSilVert = this.edges[silEdges[0]].v[0];
            for (i = 1; i < numSilEdges; i++) {
                for (j = 1; j < numSilEdges; j++) {
                    edgeNum = unsortedSilEdges[j];
                    if (edgeNum >= 0) {
                        if (this.edges[edgeNum].v[0] == nextSilVert) {
                            nextSilVert = this.edges[edgeNum].v[1];
                            silEdges[i] = edgeNum;
                            break;
                        }
                        if (this.edges[edgeNum].v[1] == nextSilVert) {
                            nextSilVert = this.edges[edgeNum].v[0];
                            silEdges[i] = -edgeNum;
                            break;
                        }
                    }
                }
                if (j >= numSilEdges) {
                    silEdges[i] = 1;	// shouldn't happen
                }
                unsortedSilEdges[j] = -1;
            }
            return numSilEdges;
        }
    }
}
