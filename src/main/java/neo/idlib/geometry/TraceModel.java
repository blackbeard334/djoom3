package neo.idlib.geometry;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Lib.idLib;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_BONE;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_BOX;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_CONE;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_CUSTOM;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_CYLINDER;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_DODECAHEDRON;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_INVALID;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_OCTAHEDRON;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_POLYGON;
import static neo.idlib.geometry.TraceModel.traceModel_t.TRM_POLYGONVOLUME;
import neo.idlib.geometry.Winding.idWinding;
import static neo.idlib.math.Math_h.Cube;
import static neo.idlib.math.Math_h.INTSIGNBITNOTSET;
import static neo.idlib.math.Math_h.INTSIGNBITSET;
import static neo.idlib.math.Math_h.Square;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Matrix.idMat3;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

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
    };
// these are bit cache limits
    public static final int MAX_TRACEMODEL_VERTS = 32;
    public static final int MAX_TRACEMODEL_EDGES = 32;
    public static final int MAX_TRACEMODEL_POLYS = 16;
    public static final int MAX_TRACEMODEL_POLYEDGES = 16;

    public static class traceModelVert_t extends idVec3 {
    }

    public static class traceModelEdge_t {

        public int[] v;
        public idVec3 normal;

        public traceModelEdge_t() {
            this.v = new int[2];
            normal = new idVec3();
        }
        
        public void oSet(final traceModelEdge_t t){
            this.v[0] = t.v[0];
            this.v[1] = t.v[1];
            this.normal.oSet(t.normal);
        }
    };

    public static class traceModelPoly_t {

        public idVec3 normal;
        public float dist;
        public idBounds bounds;
        public int numEdges;
        public int[] edges;

        public traceModelPoly_t() {    
            normal = new idVec3();
            bounds = new idBounds();
            this.edges = new int[MAX_TRACEMODEL_POLYEDGES];
        }        

        private void oSet(traceModelPoly_t t) {
            this.normal.oSet(t.normal);
            this.dist = t.dist;
            this.bounds.oSet(t.bounds);
            this.numEdges = t.numEdges;
            System.arraycopy(t.edges, 0, this.edges, 0, MAX_TRACEMODEL_POLYEDGES);
        }
    };

    public static class idTraceModel {
        public traceModel_t       type;
        public int                numVerts;
        public traceModelVert_t[] verts = Stream.generate(() -> new traceModelVert_t()).limit(MAX_TRACEMODEL_VERTS).toArray(traceModelVert_t[]::new);
        public int                numEdges;
        public traceModelEdge_t[] edges = Stream.generate(() -> new traceModelEdge_t()).limit(MAX_TRACEMODEL_EDGES + 1).toArray(traceModelEdge_t[]::new);
        public int                numPolys;
        public traceModelPoly_t[] polys = Stream.generate(() -> new traceModelPoly_t()).limit(MAX_TRACEMODEL_POLYS).toArray(traceModelPoly_t[]::new);
        public idVec3             offset;            // offset to center of model
        public idBounds           bounds;            // bounds of model
        public boolean            isConvex;          // true when model is convex
//

        public idTraceModel() {
            type = TRM_INVALID;
            numVerts = numEdges = numPolys = 0;
            offset = new idVec3();
            bounds = new idBounds();
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

            if (type != TRM_BOX) {
                InitBox();
            }
            // offset to center
            offset = (boxBounds.oGet(0).oPlus(boxBounds.oGet(1))).oMultiply(0.5f);
            // set box vertices
            for (i = 0; i < 8; i++) {
                verts[i].oSet(0, boxBounds.oGet((i ^ (i >> 1)) & 1).oGet(0));
                verts[i].oSet(1, boxBounds.oGet((i >> 1) & 1).oGet(1));
                verts[i].oSet(2, boxBounds.oGet((i >> 2) & 1).oGet(2));
            }
            // set polygon plane distances
            polys[0].dist = -boxBounds.oGet(0).oGet(2);
            polys[1].dist = boxBounds.oGet(1).oGet(2);
            polys[2].dist = -boxBounds.oGet(0).oGet(1);
            polys[3].dist = boxBounds.oGet(1).oGet(0);
            polys[4].dist = boxBounds.oGet(1).oGet(1);
            polys[5].dist = -boxBounds.oGet(0).oGet(0);
            // set polygon bounds
            for (i = 0; i < 6; i++) {
                polys[i].bounds.oSet(boxBounds);
            }
            polys[0].bounds.oSet(1, 2, boxBounds.oGet(0).oGet(2));
            polys[1].bounds.oSet(0, 2, boxBounds.oGet(1).oGet(2));
            polys[2].bounds.oSet(1, 1, boxBounds.oGet(0).oGet(1));
            polys[3].bounds.oSet(0, 0, boxBounds.oGet(1).oGet(0));
            polys[4].bounds.oSet(0, 1, boxBounds.oGet(1).oGet(1));
            polys[5].bounds.oSet(1, 0, boxBounds.oGet(0).oGet(0));

            bounds.oSet(boxBounds);
        }

        /*
         ============
         idTraceModel::SetupBox

         The origin is placed at the center of the cube.
         ============
         */
        public void SetupBox(final float size) {
            idBounds boxBounds = new idBounds();
            float halfSize;

            halfSize = size * 0.5f;
            boxBounds.oGet(0).Set(-halfSize, -halfSize, -halfSize);
            boxBounds.oGet(1).Set(halfSize, halfSize, halfSize);
            SetupBox(boxBounds);
        }

        // octahedron
        public void SetupOctahedron(final idBounds octBounds) {
            int i, e0, e1, v0, v1, v2;
            idVec3 v = new idVec3();

            if (type != TRM_OCTAHEDRON) {
                InitOctahedron();
            }

            offset = octBounds.oGet(0).oPlus(octBounds.oGet(1)).oMultiply(0.5f);
            v.oSet(0, octBounds.oGet(1).oGet(0) - offset.oGet(0));
            v.oSet(1, octBounds.oGet(1).oGet(1) - offset.oGet(1));
            v.oSet(2, octBounds.oGet(1).oGet(2) - offset.oGet(2));

            // set vertices
            verts[0].Set(offset.x + v.oGet(0), offset.y, offset.z);
            verts[1].Set(offset.x - v.oGet(0), offset.y, offset.z);
            verts[2].Set(offset.x, offset.y + v.oGet(1), offset.z);
            verts[3].Set(offset.x, offset.y - v.oGet(1), offset.z);
            verts[4].Set(offset.x, offset.y, offset.z + v.oGet(2));
            verts[5].Set(offset.x, offset.y, offset.z - v.oGet(2));

            // set polygons
            for (i = 0; i < numPolys; i++) {
                e0 = polys[i].edges[0];
                e1 = polys[i].edges[1];
                v0 = edges[Math.abs(e0)].v[INTSIGNBITSET(e0)];
                v1 = edges[Math.abs(e0)].v[INTSIGNBITNOTSET(e0)];
                v2 = edges[Math.abs(e1)].v[INTSIGNBITNOTSET(e1)];
                // polygon plane
                polys[i].normal = (verts[v1].oMinus(verts[v0])).Cross(verts[v2].oMinus(verts[v0]));
                polys[i].normal.Normalize();
                polys[i].dist = polys[i].normal.oMultiply(verts[v0]);
                // polygon bounds
                polys[i].bounds.oSet(0, polys[i].bounds.oSet(0, verts[v0]));
                polys[i].bounds.AddPoint(verts[v1]);
                polys[i].bounds.AddPoint(verts[v2]);
            }

            // trm bounds
            bounds = octBounds;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::SetupOctahedron

         The origin is placed at the center of the octahedron.
         ============
         */
        public void SetupOctahedron(final float size) {
            idBounds octBounds = new idBounds();
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
            idVec3 a = new idVec3(), b = new idVec3(), c = new idVec3();

            if (type != TRM_DODECAHEDRON) {
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

            offset = dodBounds.oGet(0).oPlus(dodBounds.oGet(1)).oMultiply(0.5f);

            // set vertices
            verts[ 0].Set(offset.x + a.oGet(0), offset.y + a.oGet(1), offset.z + a.oGet(2));
            verts[ 1].Set(offset.x + a.oGet(0), offset.y + a.oGet(1), offset.z - a.oGet(2));
            verts[ 2].Set(offset.x + a.oGet(0), offset.y - a.oGet(1), offset.z + a.oGet(2));
            verts[ 3].Set(offset.x + a.oGet(0), offset.y - a.oGet(1), offset.z - a.oGet(2));
            verts[ 4].Set(offset.x - a.oGet(0), offset.y + a.oGet(1), offset.z + a.oGet(2));
            verts[ 5].Set(offset.x - a.oGet(0), offset.y + a.oGet(1), offset.z - a.oGet(2));
            verts[ 6].Set(offset.x - a.oGet(0), offset.y - a.oGet(1), offset.z + a.oGet(2));
            verts[ 7].Set(offset.x - a.oGet(0), offset.y - a.oGet(1), offset.z - a.oGet(2));
            verts[ 8].Set(offset.x + b.oGet(0), offset.y + c.oGet(1), offset.z/*        */);
            verts[ 9].Set(offset.x - b.oGet(0), offset.y + c.oGet(1), offset.z/*        */);
            verts[10].Set(offset.x + b.oGet(0), offset.y - c.oGet(1), offset.z/*        */);
            verts[11].Set(offset.x - b.oGet(0), offset.y - c.oGet(1), offset.z/*        */);
            verts[12].Set(offset.x + c.oGet(0), offset.y/*        */, offset.z + b.oGet(2));
            verts[13].Set(offset.x + c.oGet(0), offset.y/*        */, offset.z - b.oGet(2));
            verts[14].Set(offset.x - c.oGet(0), offset.y/*        */, offset.z + b.oGet(2));
            verts[15].Set(offset.x - c.oGet(0), offset.y/*        */, offset.z - b.oGet(2));
            verts[16].Set(offset.x/*        */, offset.y + b.oGet(1), offset.z + c.oGet(2));
            verts[17].Set(offset.x/*        */, offset.y - b.oGet(1), offset.z + c.oGet(2));
            verts[18].Set(offset.x/*        */, offset.y + b.oGet(1), offset.z - c.oGet(2));
            verts[19].Set(offset.x/*        */, offset.y - b.oGet(1), offset.z - c.oGet(2));

            // set polygons
            for (i = 0; i < numPolys; i++) {
                e0 = polys[i].edges[0];
                e1 = polys[i].edges[1];
                e2 = polys[i].edges[2];
                e3 = polys[i].edges[3];
                v0 = edges[Math.abs(e0)].v[INTSIGNBITSET(e0)];
                v1 = edges[Math.abs(e0)].v[INTSIGNBITNOTSET(e0)];
                v2 = edges[Math.abs(e1)].v[INTSIGNBITNOTSET(e1)];
                v3 = edges[Math.abs(e2)].v[INTSIGNBITNOTSET(e2)];
                v4 = edges[Math.abs(e3)].v[INTSIGNBITNOTSET(e3)];
                // polygon plane
                polys[i].normal = (verts[v1].oMinus(verts[v0])).Cross(verts[v2].oMinus(verts[v0]));
                polys[i].normal.Normalize();
                polys[i].dist = polys[i].normal.oMultiply(verts[v0]);
                // polygon bounds
                polys[i].bounds.oSet(0, polys[i].bounds.oSet(1, verts[v0]));
                polys[i].bounds.AddPoint(verts[v1]);
                polys[i].bounds.AddPoint(verts[v2]);
                polys[i].bounds.AddPoint(verts[v3]);
                polys[i].bounds.AddPoint(verts[v4]);
            }

            // trm bounds
            bounds = dodBounds;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::SetupDodecahedron

         The origin is placed at the center of the octahedron.
         ============
         */
        public void SetupDodecahedron(final float size) {
            idBounds dodBounds = new idBounds();
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
            if (n * 2 > MAX_TRACEMODEL_VERTS) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCylinder: too many vertices\n");
                n = MAX_TRACEMODEL_VERTS / 2;
            }
            if (n * 3 > MAX_TRACEMODEL_EDGES) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCylinder: too many sides\n");
                n = MAX_TRACEMODEL_EDGES / 3;
            }
            if (n + 2 > MAX_TRACEMODEL_POLYS) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCylinder: too many polygons\n");
                n = MAX_TRACEMODEL_POLYS - 2;
            }

            type = TRM_CYLINDER;
            numVerts = n * 2;
            numEdges = n * 3;
            numPolys = n + 2;
            offset = cylBounds.oGet(0).oPlus(cylBounds.oGet(1)).oMultiply(0.5f);
            halfSize = cylBounds.oGet(1).oMinus(offset);
            for (i = 0; i < n; i++) {
                // verts
                angle = idMath.TWO_PI * i / n;
                verts[i].x = (float) (Math.cos(angle) * halfSize.x + offset.x);
                verts[i].y = (float) (Math.sin(angle) * halfSize.y + offset.y);
                verts[i].z = -halfSize.z + offset.z;
                verts[n + i].x = verts[i].x;
                verts[n + i].y = verts[i].y;
                verts[n + i].z = halfSize.z + offset.z;
                // edges
                ii = i + 1;
                n2 = n << 1;
                edges[ii].v[0] = i;
                edges[ii].v[1] = ii % n;
                edges[n + ii].v[0] = edges[ii].v[0] + n;
                edges[n + ii].v[1] = edges[ii].v[1] + n;
                edges[n2 + ii].v[0] = i;
                edges[n2 + ii].v[1] = n + i;
                // vertical polygon edges
                polys[i].numEdges = 4;
                polys[i].edges[0] = ii;
                polys[i].edges[1] = n2 + (ii % n) + 1;
                polys[i].edges[2] = -(n + ii);
                polys[i].edges[3] = -(n2 + ii);
                // bottom and top polygon edges
                polys[n].edges[i] = -(n - i);
                polys[n + 1].edges[i] = n + ii;
            }
            // bottom and top polygon numEdges
            polys[n].numEdges = n;
            polys[n + 1].numEdges = n;
            // polygons
            for (i = 0; i < n; i++) {
                // vertical polygon plane
                polys[i].normal = (verts[(i + 1) % n].oMinus(verts[i])).Cross(verts[n + i].oMinus(verts[i]));
                polys[i].normal.Normalize();
                polys[i].dist = polys[i].normal.oMultiply(verts[i]);
                // vertical polygon bounds
                polys[i].bounds.Clear();
                polys[i].bounds.AddPoint(verts[i]);
                polys[i].bounds.AddPoint(verts[(i + 1) % n]);
                polys[i].bounds.oSet(0, 2, -halfSize.z + offset.z);
                polys[i].bounds.oSet(1, 2, halfSize.z + offset.z);
            }
            // bottom and top polygon plane
            polys[n].normal.Set(0.0f, 0.0f, -1.0f);
            polys[n].dist = -cylBounds.oGet(0).oGet(2);
            polys[n + 1].normal.Set(0.0f, 0.0f, 1.0f);
            polys[n + 1].dist = cylBounds.oGet(1).oGet(2);
            // trm bounds
            bounds = cylBounds;
            // bottom and top polygon bounds
            polys[n].bounds = bounds;
            polys[n].bounds.oSet(1, 2, bounds.oGet(0).oGet(2));
            polys[n + 1].bounds = bounds;
            polys[n + 1].bounds.oSet(0, 2, bounds.oGet(1).oGet(2));
            // convex model
            isConvex = true;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::SetupCylinder

         The origin is placed at the center of the cylinder.
         ============
         */
        public void SetupCylinder(final float height, final float width, final int numSides) {
            idBounds cylBounds = new idBounds();
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
            if (n + 1 > MAX_TRACEMODEL_VERTS) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCone: too many vertices\n");
                n = MAX_TRACEMODEL_VERTS - 1;
            }
            if (n * 2 > MAX_TRACEMODEL_EDGES) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCone: too many edges\n");
                n = MAX_TRACEMODEL_EDGES / 2;
            }
            if (n + 1 > MAX_TRACEMODEL_POLYS) {
                idLib.common.Printf("WARNING: idTraceModel::SetupCone: too many polygons\n");
                n = MAX_TRACEMODEL_POLYS - 1;
            }

            type = TRM_CONE;
            numVerts = n + 1;
            numEdges = n * 2;
            numPolys = n + 1;
            offset = coneBounds.oGet(0).oPlus(coneBounds.oGet(1)).oMultiply(0.5f);
            halfSize = coneBounds.oGet(1).oMinus(offset);
            verts[n].Set(0.0f, 0.0f, halfSize.z + offset.z);
            for (i = 0; i < n; i++) {
                // verts
                angle = idMath.TWO_PI * i / n;
                verts[i].x = (float) (Math.cos(angle) * halfSize.x + offset.x);
                verts[i].y = (float) (Math.sin(angle) * halfSize.y + offset.y);
                verts[i].z = -halfSize.z + offset.z;
                // edges
                ii = i + 1;
                edges[ii].v[0] = i;
                edges[ii].v[1] = ii % n;
                edges[n + ii].v[0] = i;
                edges[n + ii].v[1] = n;
                // vertical polygon edges
                polys[i].numEdges = 3;
                polys[i].edges[0] = ii;
                polys[i].edges[1] = n + (ii % n) + 1;
                polys[i].edges[2] = -(n + ii);
                // bottom polygon edges
                polys[n].edges[i] = -(n - i);
            }
            // bottom polygon numEdges
            polys[n].numEdges = n;

            // polygons
            for (i = 0; i < n; i++) {
                // polygon plane
                polys[i].normal = (verts[(i + 1) % n].oMinus(verts[i])).Cross(verts[n].oMinus(verts[i]));
                polys[i].normal.Normalize();
                polys[i].dist = polys[i].normal.oMultiply(verts[i]);
                // polygon bounds
                polys[i].bounds.Clear();
                polys[i].bounds.AddPoint(verts[i]);
                polys[i].bounds.AddPoint(verts[(i + 1) % n]);
                polys[i].bounds.AddPoint(verts[n]);
            }
            // bottom polygon plane
            polys[n].normal.Set(0.0f, 0.0f, -1.0f);
            polys[n].dist = -coneBounds.oGet(0).oGet(2);
            // trm bounds
            bounds = coneBounds;
            // bottom polygon bounds
            polys[n].bounds = bounds;
            polys[n].bounds.oSet(1, 2, bounds.oGet(0).oGet(2));
            // convex model
            isConvex = true;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::SetupCone

         The origin is placed at the apex of the cone.
         ============
         */
        public void SetupCone(final float height, final float width, final int numSides) {
            idBounds coneBounds = new idBounds();
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
            float halfLength = length * 0.5f;

            if (type != TRM_BONE) {
                InitBone();
            }
            // offset to center
            offset.Set(0.0f, 0.0f, 0.0f);
            // set vertices
            verts[0].Set(0.0f, 0.0f, -halfLength);
            verts[1].Set(0.0f, width * -0.5f, 0.0f);
            verts[2].Set(width * 0.5f, width * 0.25f, 0.0f);
            verts[3].Set(width * -0.5f, width * 0.25f, 0.0f);
            verts[4].Set(0.0f, 0.0f, halfLength);
            // set bounds
            bounds.oGet(0).Set(width * -0.5f, width * -0.5f, -halfLength);
            bounds.oGet(1).Set(width * 0.5f, width * 0.25f, halfLength);
            // poly plane normals
            polys[0].normal = (verts[2].oMinus(verts[0])).Cross(verts[1].oMinus(verts[0]));
            polys[0].normal.Normalize();
            polys[2].normal.Set(-polys[0].normal.oGet(0), polys[0].normal.oGet(1), polys[0].normal.oGet(2));
            polys[3].normal.Set(polys[0].normal.oGet(0), polys[0].normal.oGet(1), -polys[0].normal.oGet(2));
            polys[5].normal.Set(-polys[0].normal.oGet(0), polys[0].normal.oGet(1), -polys[0].normal.oGet(2));
            polys[1].normal = (verts[3].oMinus(verts[0])).Cross(verts[2].oMinus(verts[0]));
            polys[1].normal.Normalize();
            polys[4].normal.Set(polys[1].normal.oGet(0), polys[1].normal.oGet(1), -polys[1].normal.oGet(2));
            // poly plane distances
            for (i = 0; i < 6; i++) {
                polys[i].dist = polys[i].normal.oMultiply(verts[ edges[ Math.abs(polys[i].edges[0])].v[0]]);
                polys[i].bounds.Clear();
                for (j = 0; j < 3; j++) {
                    edgeNum = polys[i].edges[ j];
                    polys[i].bounds.AddPoint(verts[ edges[Math.abs(edgeNum)].v[edgeNum < 0 ? 1 : 0]]);
                }
            }

            GenerateEdgeNormals();
        }

        // arbitrary convex polygon
        public void SetupPolygon(final idVec3[] v, final int count) {
            int i, j;
            idVec3 mid;

            type = TRM_POLYGON;
            numVerts = count;
            // times three because we need to be able to turn the polygon into a volume
            if (numVerts * 3 > MAX_TRACEMODEL_EDGES) {
                idLib.common.Printf("WARNING: idTraceModel::SetupPolygon: too many vertices\n");
                numVerts = MAX_TRACEMODEL_EDGES / 3;
            }

            numEdges = numVerts;
            numPolys = 2;
            // set polygon planes
            polys[0].numEdges = numEdges;
            polys[0].normal = (v[1].oMinus(v[0])).Cross(v[2].oMinus(v[0]));
            polys[0].normal.Normalize();
            polys[0].dist = polys[0].normal.oMultiply(v[0]);
            polys[1].numEdges = numEdges;
            polys[1].normal.oSet(polys[0].normal.oNegative());
            polys[1].dist = -polys[0].dist;
            // setup verts, edges and polygons
            polys[0].bounds.Clear();
            mid = getVec3_origin();
            for (i = 0, j = 1; i < numVerts; i++, j++) {
                if (j >= numVerts) {
                    j = 0;
                }
                verts[i].oSet(v[i]);
                edges[i + 1].v[0] = i;
                edges[i + 1].v[1] = j;
                edges[i + 1].normal = polys[0].normal.Cross(v[i].oMinus(v[j]));
                edges[i + 1].normal.Normalize();
                polys[0].edges[i] = i + 1;
                polys[1].edges[i] = -(numVerts - i);
                polys[0].bounds.AddPoint(verts[i]);
                mid.oPluSet(v[i]);
            }
            polys[1].bounds = polys[0].bounds;
            // offset to center
            offset = mid.oMultiply((1.0f / numVerts));
            // total bounds
            bounds = polys[0].bounds;
            // considered non convex because the model has no volume
            isConvex = false;
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

            for (i = 0; i <= numEdges; i++) {
                edges[i].normal.Zero();
            }

            numSharpEdges = 0;
            for (i = 0; i < numPolys; i++) {
                poly = polys[i];
                for (j = 0; j < poly.numEdges; j++) {
                    edgeNum = poly.edges[j];
                    edge = edges[Math.abs(edgeNum)];
                    if (edge.normal.oGet(0) == 0.0f && edge.normal.oGet(1) == 0.0f && edge.normal.oGet(2) == 0.0f) {
                        edge.normal = poly.normal;
                    } else {
                        dot = edge.normal.oMultiply(poly.normal);
                        // if the two planes make a very sharp edge
                        if (dot < SHARP_EDGE_DOT) {
                            // max length normal pointing outside both polygons
                            dir = verts[ edge.v[edgeNum > 0 ? 1 : 0]].oMinus(verts[ edge.v[edgeNum < 0 ? 1 : 0]]);
                            edge.normal = edge.normal.Cross(dir).oPlus(poly.normal.Cross(dir.oNegative()));
                            edge.normal.oMulSet((0.5f / (0.5f + 0.5f * SHARP_EDGE_DOT)) / edge.normal.Length());
                            numSharpEdges++;
                        } else {
                            edge.normal = (edge.normal.oPlus(poly.normal)).oMultiply(0.5f / (0.5f + 0.5f * dot));
                        }
                    }
                }
            }
            return numSharpEdges;
        }

        // translate the trm
        public void Translate(final idVec3 translation) {
            int i;

            for (i = 0; i < numVerts; i++) {
                verts[i].oPluSet(translation);
            }
            for (i = 0; i < numPolys; i++) {
                polys[i].dist += polys[i].normal.oMultiply(translation);
                polys[i].bounds.oPluSet(translation);
            }
            offset.oPluSet(translation);
            bounds.oPluSet(translation);
        }

        // rotate the trm
        public void Rotate(final idMat3 rotation) {
            int i, j, edgeNum;

            for (i = 0; i < numVerts; i++) {
                verts[i].oSet(rotation.oMultiply(verts[i]));
            }

            bounds.Clear();
            for (i = 0; i < numPolys; i++) {
                polys[i].normal.oSet(rotation.oMultiply(polys[i].normal));
                polys[i].bounds.Clear();
                edgeNum = 0;
                for (j = 0; j < polys[i].numEdges; j++) {
                    edgeNum = polys[i].edges[j];
                    polys[i].bounds.AddPoint(verts[edges[Math.abs(edgeNum)].v[INTSIGNBITSET(edgeNum)]]);
                }
                polys[i].dist = polys[i].normal.oMultiply(verts[edges[Math.abs(edgeNum)].v[INTSIGNBITSET(edgeNum)]]);
                bounds.oPluSet(polys[i].bounds);
            }

            GenerateEdgeNormals();
        }

        // shrink the model m units on all sides
        public void Shrink(final float m) {
            int i, j, edgeNum;
            traceModelEdge_t edge;
            idVec3 dir;

            if (type == TRM_POLYGON) {
                for (i = 0; i < numEdges; i++) {
                    edgeNum = polys[0].edges[i];
                    edge = edges[Math.abs(edgeNum)];
                    dir = verts[ edge.v[ INTSIGNBITSET(edgeNum)]].oMinus(verts[ edge.v[ INTSIGNBITNOTSET(edgeNum)]]);
                    if (dir.Normalize() < 2.0f * m) {
                        continue;
                    }
                    dir.oMulSet(m);
                    verts[ edge.v[ 0]].oMinSet(dir);
                    verts[ edge.v[ 1]].oPluSet(dir);
                }
                return;
            }

            for (i = 0; i < numPolys; i++) {
                polys[i].dist -= m;

                for (j = 0; j < polys[i].numEdges; j++) {
                    edgeNum = polys[i].edges[j];
                    edge = edges[Math.abs(edgeNum)];
                    verts[ edge.v[ INTSIGNBITSET(edgeNum)]].oMinSet(polys[i].normal.oMultiply(m));
                }
            }
        }

        // compare
        public boolean Compare(final idTraceModel trm) {
            int i;

            if (type != trm.type || numVerts != trm.numVerts
                    || numEdges != trm.numEdges || numPolys != trm.numPolys) {
                return false;
            }
            if (bounds != trm.bounds || offset != trm.offset) {
                return false;
            }

            switch (type) {
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
                        if (verts[i] != trm.verts[i]) {
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
            hash = 19 * hash + (this.type != null ? this.type.hashCode() : 0);
            hash = 19 * hash + this.numVerts;
            hash = 19 * hash + Arrays.deepHashCode(this.verts);
            hash = 19 * hash + this.numEdges;
            hash = 19 * hash + this.numPolys;
            hash = 19 * hash + Objects.hashCode(this.offset);
            hash = 19 * hash + Objects.hashCode(this.bounds);
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

            if (polyNum < 0 || polyNum >= numPolys) {
                return 0.0f;
            }
            poly = polys[polyNum];
            total = 0.0f;
            base = verts[ edges[ Math.abs(poly.edges[0])].v[ INTSIGNBITSET(poly.edges[0])]];
            for (i = 0; i < poly.numEdges; i++) {
                v1 = verts[ edges[ Math.abs(poly.edges[i])].v[ INTSIGNBITSET(poly.edges[i])]].oMinus(base);
                v2 = verts[ edges[ Math.abs(poly.edges[i])].v[ INTSIGNBITNOTSET(poly.edges[i])]].oMinus(base);
                cross = v1.Cross(v2);
                total += cross.Length();
            }
            return total * 0.5f;
        }

        // get the silhouette edges
        public int GetProjectionSilhouetteEdges(final idVec3 projectionOrigin, int silEdges[]) {
            int i, j, edgeNum;
            int[] edgeIsSilEdge = new int[MAX_TRACEMODEL_EDGES + 1];
            traceModelPoly_t poly;
            idVec3 dir;

//	memset( edgeIsSilEdge, 0, sizeof( edgeIsSilEdge ) );

            for (i = 0; i < numPolys; i++) {
                poly = polys[i];
                edgeNum = poly.edges[0];
                dir = verts[ edges[Math.abs(edgeNum)].v[ INTSIGNBITSET(edgeNum)]].oMinus(projectionOrigin);
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
            int[] edgeIsSilEdge = new int[MAX_TRACEMODEL_EDGES + 1];
            traceModelPoly_t poly;

//	memset( edgeIsSilEdge, 0, sizeof( edgeIsSilEdge ) );

            for (i = 0; i < numPolys; i++) {
                poly = polys[i];
                if (projectionDir.oMultiply(poly.normal) < 0.0f) {
                    for (j = 0; j < poly.numEdges; j++) {
                        edgeNum = poly.edges[j];
                        edgeIsSilEdge[Math.abs(edgeNum)] ^= 1;
                    }
                }
            }

            return GetOrderedSilhouetteEdges(edgeIsSilEdge, silEdges);
        }

        // calculate mass properties assuming an uniform density
        public void GetMassProperties(final float density, float[] mass, idVec3 centerOfMass, idMat3 inertiaTensor) {
            volumeIntegrals_t integrals = new volumeIntegrals_t();

            // if polygon trace model
            if (type == TRM_POLYGON) {
                idTraceModel trm = new idTraceModel();

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
            inertiaTensor.oMinSet(0, 0, mass[0] * (centerOfMass.oGet(1) * centerOfMass.oGet(1) + centerOfMass.oGet(2) * centerOfMass.oGet(2)));
            inertiaTensor.oMinSet(1, 1, mass[0] * (centerOfMass.oGet(2) * centerOfMass.oGet(2) + centerOfMass.oGet(0) * centerOfMass.oGet(0)));
            inertiaTensor.oMinSet(2, 2, mass[0] * (centerOfMass.oGet(0) * centerOfMass.oGet(0) + centerOfMass.oGet(1) * centerOfMass.oGet(1)));
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

            type = TRM_BOX;
            numVerts = 8;
            numEdges = 12;
            numPolys = 6;

            // set box edges
            for (i = 0; i < 4; i++) {
                edges[i + 1].v[0] = i;
                edges[i + 1].v[1] = (i + 1) & 3;
                edges[i + 5].v[0] = 4 + i;
                edges[i + 5].v[1] = 4 + ((i + 1) & 3);
                edges[i + 9].v[0] = i;
                edges[i + 9].v[1] = 4 + i;
            }

            // all edges of a polygon go counter clockwise
            polys[0].numEdges = 4;
            polys[0].edges[0] = -4;
            polys[0].edges[1] = -3;
            polys[0].edges[2] = -2;
            polys[0].edges[3] = -1;
            polys[0].normal.Set(0.0f, 0.0f, -1.0f);

            polys[1].numEdges = 4;
            polys[1].edges[0] = 5;
            polys[1].edges[1] = 6;
            polys[1].edges[2] = 7;
            polys[1].edges[3] = 8;
            polys[1].normal.Set(0.0f, 0.0f, 1.0f);

            polys[2].numEdges = 4;
            polys[2].edges[0] = 1;
            polys[2].edges[1] = 10;
            polys[2].edges[2] = -5;
            polys[2].edges[3] = -9;
            polys[2].normal.Set(0.0f, -1.0f, 0.0f);

            polys[3].numEdges = 4;
            polys[3].edges[0] = 2;
            polys[3].edges[1] = 11;
            polys[3].edges[2] = -6;
            polys[3].edges[3] = -10;
            polys[3].normal.Set(1.0f, 0.0f, 0.0f);

            polys[4].numEdges = 4;
            polys[4].edges[0] = 3;
            polys[4].edges[1] = 12;
            polys[4].edges[2] = -7;
            polys[4].edges[3] = -11;
            polys[4].normal.Set(0.0f, 1.0f, 0.0f);

            polys[5].numEdges = 4;
            polys[5].edges[0] = 4;
            polys[5].edges[1] = 9;
            polys[5].edges[2] = -8;
            polys[5].edges[3] = -12;
            polys[5].normal.Set(-1.0f, 0.0f, 0.0f);

            // convex model
            isConvex = true;

            GenerateEdgeNormals();
        }

        /*
         ============
         idTraceModel::InitOctahedron

         Initialize size independent octahedron.
         ============
         */
        private void InitOctahedron() {

            type = TRM_OCTAHEDRON;
            numVerts = 6;
            numEdges = 12;
            numPolys = 8;

            // set edges
            edges[ 1].v[0] = 4;
            edges[ 1].v[1] = 0;
            edges[ 2].v[0] = 0;
            edges[ 2].v[1] = 2;
            edges[ 3].v[0] = 2;
            edges[ 3].v[1] = 4;
            edges[ 4].v[0] = 2;
            edges[ 4].v[1] = 1;
            edges[ 5].v[0] = 1;
            edges[ 5].v[1] = 4;
            edges[ 6].v[0] = 1;
            edges[ 6].v[1] = 3;
            edges[ 7].v[0] = 3;
            edges[ 7].v[1] = 4;
            edges[ 8].v[0] = 3;
            edges[ 8].v[1] = 0;
            edges[ 9].v[0] = 5;
            edges[ 9].v[1] = 2;
            edges[10].v[0] = 0;
            edges[10].v[1] = 5;
            edges[11].v[0] = 5;
            edges[11].v[1] = 1;
            edges[12].v[0] = 5;
            edges[12].v[1] = 3;

            // all edges of a polygon go counter clockwise
            polys[0].numEdges = 3;
            polys[0].edges[0] = 1;
            polys[0].edges[1] = 2;
            polys[0].edges[2] = 3;

            polys[1].numEdges = 3;
            polys[1].edges[0] = -3;
            polys[1].edges[1] = 4;
            polys[1].edges[2] = 5;

            polys[2].numEdges = 3;
            polys[2].edges[0] = -5;
            polys[2].edges[1] = 6;
            polys[2].edges[2] = 7;

            polys[3].numEdges = 3;
            polys[3].edges[0] = -7;
            polys[3].edges[1] = 8;
            polys[3].edges[2] = -1;

            polys[4].numEdges = 3;
            polys[4].edges[0] = 9;
            polys[4].edges[1] = -2;
            polys[4].edges[2] = 10;

            polys[5].numEdges = 3;
            polys[5].edges[0] = 11;
            polys[5].edges[1] = -4;
            polys[5].edges[2] = -9;

            polys[6].numEdges = 3;
            polys[6].edges[0] = 12;
            polys[6].edges[1] = -6;
            polys[6].edges[2] = -11;

            polys[7].numEdges = 3;
            polys[7].edges[0] = -10;
            polys[7].edges[1] = -8;
            polys[7].edges[2] = -12;

            // convex model
            isConvex = true;
        }

        /*
         ============
         idTraceModel::InitDodecahedron

         Initialize size independent dodecahedron.
         ============
         */
        private void InitDodecahedron() {

            type = TRM_DODECAHEDRON;
            numVerts = 20;
            numEdges = 30;
            numPolys = 12;

            // set edges
            edges[ 1].v[0] = 0;
            edges[ 1].v[1] = 8;
            edges[ 2].v[0] = 8;
            edges[ 2].v[1] = 9;
            edges[ 3].v[0] = 9;
            edges[ 3].v[1] = 4;
            edges[ 4].v[0] = 4;
            edges[ 4].v[1] = 16;
            edges[ 5].v[0] = 16;
            edges[ 5].v[1] = 0;
            edges[ 6].v[0] = 16;
            edges[ 6].v[1] = 17;
            edges[ 7].v[0] = 17;
            edges[ 7].v[1] = 2;
            edges[ 8].v[0] = 2;
            edges[ 8].v[1] = 12;
            edges[ 9].v[0] = 12;
            edges[ 9].v[1] = 0;
            edges[10].v[0] = 2;
            edges[10].v[1] = 10;
            edges[11].v[0] = 10;
            edges[11].v[1] = 3;
            edges[12].v[0] = 3;
            edges[12].v[1] = 13;
            edges[13].v[0] = 13;
            edges[13].v[1] = 12;
            edges[14].v[0] = 9;
            edges[14].v[1] = 5;
            edges[15].v[0] = 5;
            edges[15].v[1] = 15;
            edges[16].v[0] = 15;
            edges[16].v[1] = 14;
            edges[17].v[0] = 14;
            edges[17].v[1] = 4;
            edges[18].v[0] = 3;
            edges[18].v[1] = 19;
            edges[19].v[0] = 19;
            edges[19].v[1] = 18;
            edges[20].v[0] = 18;
            edges[20].v[1] = 1;
            edges[21].v[0] = 1;
            edges[21].v[1] = 13;
            edges[22].v[0] = 7;
            edges[22].v[1] = 11;
            edges[23].v[0] = 11;
            edges[23].v[1] = 6;
            edges[24].v[0] = 6;
            edges[24].v[1] = 14;
            edges[25].v[0] = 15;
            edges[25].v[1] = 7;
            edges[26].v[0] = 1;
            edges[26].v[1] = 8;
            edges[27].v[0] = 18;
            edges[27].v[1] = 5;
            edges[28].v[0] = 6;
            edges[28].v[1] = 17;
            edges[29].v[0] = 11;
            edges[29].v[1] = 10;
            edges[30].v[0] = 19;
            edges[30].v[1] = 7;

            // all edges of a polygon go counter clockwise
            polys[0].numEdges = 5;
            polys[0].edges[0] = 1;
            polys[0].edges[1] = 2;
            polys[0].edges[2] = 3;
            polys[0].edges[3] = 4;
            polys[0].edges[4] = 5;

            polys[1].numEdges = 5;
            polys[1].edges[0] = -5;
            polys[1].edges[1] = 6;
            polys[1].edges[2] = 7;
            polys[1].edges[3] = 8;
            polys[1].edges[4] = 9;

            polys[2].numEdges = 5;
            polys[2].edges[0] = -8;
            polys[2].edges[1] = 10;
            polys[2].edges[2] = 11;
            polys[2].edges[3] = 12;
            polys[2].edges[4] = 13;

            polys[3].numEdges = 5;
            polys[3].edges[0] = 14;
            polys[3].edges[1] = 15;
            polys[3].edges[2] = 16;
            polys[3].edges[3] = 17;
            polys[3].edges[4] = -3;

            polys[4].numEdges = 5;
            polys[4].edges[0] = 18;
            polys[4].edges[1] = 19;
            polys[4].edges[2] = 20;
            polys[4].edges[3] = 21;
            polys[4].edges[4] = -12;

            polys[5].numEdges = 5;
            polys[5].edges[0] = 22;
            polys[5].edges[1] = 23;
            polys[5].edges[2] = 24;
            polys[5].edges[3] = -16;
            polys[5].edges[4] = 25;

            polys[6].numEdges = 5;
            polys[6].edges[0] = -9;
            polys[6].edges[1] = -13;
            polys[6].edges[2] = -21;
            polys[6].edges[3] = 26;
            polys[6].edges[4] = -1;

            polys[7].numEdges = 5;
            polys[7].edges[0] = -26;
            polys[7].edges[1] = -20;
            polys[7].edges[2] = 27;
            polys[7].edges[3] = -14;
            polys[7].edges[4] = -2;

            polys[8].numEdges = 5;
            polys[8].edges[0] = -4;
            polys[8].edges[1] = -17;
            polys[8].edges[2] = -24;
            polys[8].edges[3] = 28;
            polys[8].edges[4] = -6;

            polys[9].numEdges = 5;
            polys[9].edges[0] = -23;
            polys[9].edges[1] = 29;
            polys[9].edges[2] = -10;
            polys[9].edges[3] = -7;
            polys[9].edges[4] = -28;

            polys[10].numEdges = 5;
            polys[10].edges[0] = -25;
            polys[10].edges[1] = -15;
            polys[10].edges[2] = -27;
            polys[10].edges[3] = -19;
            polys[10].edges[4] = 30;

            polys[11].numEdges = 5;
            polys[11].edges[0] = -30;
            polys[11].edges[1] = -18;
            polys[11].edges[2] = -11;
            polys[11].edges[3] = -29;
            polys[11].edges[4] = -22;

            // convex model
            isConvex = true;
        }

        /*
         ============
         idTraceModel::InitBone

         Initialize size independent bone.
         ============
         */
        private void InitBone() {
            int i;

            type = TRM_BONE;
            numVerts = 5;
            numEdges = 9;
            numPolys = 6;

            // set bone edges
            for (i = 0; i < 3; i++) {
                edges[ i + 1].v[0] = 0;
                edges[ i + 1].v[1] = i + 1;
                edges[ i + 4].v[0] = 1 + i;
                edges[ i + 4].v[1] = 1 + ((i + 1) % 3);
                edges[ i + 7].v[0] = i + 1;
                edges[ i + 7].v[1] = 4;
            }

            // all edges of a polygon go counter clockwise
            polys[0].numEdges = 3;
            polys[0].edges[0] = 2;
            polys[0].edges[1] = -4;
            polys[0].edges[2] = -1;

            polys[1].numEdges = 3;
            polys[1].edges[0] = 3;
            polys[1].edges[1] = -5;
            polys[1].edges[2] = -2;

            polys[2].numEdges = 3;
            polys[2].edges[0] = 1;
            polys[2].edges[1] = -6;
            polys[2].edges[2] = -3;

            polys[3].numEdges = 3;
            polys[3].edges[0] = 4;
            polys[3].edges[1] = 8;
            polys[3].edges[2] = -7;

            polys[4].numEdges = 3;
            polys[4].edges[0] = 5;
            polys[4].edges[1] = 9;
            polys[4].edges[2] = -8;

            polys[5].numEdges = 3;
            polys[5].edges[0] = 6;
            polys[5].edges[1] = 7;
            polys[5].edges[2] = -9;

            // convex model
            isConvex = true;
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
        };

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
            poly = polys[polyNum];
            for (i = 0; i < poly.numEdges; i++) {
                edgeNum = poly.edges[i];
                v1 = verts[ edges[ Math.abs(edgeNum)].v[ edgeNum < 0 ? 1 : 0]];
                v2 = verts[ edges[ Math.abs(edgeNum)].v[ edgeNum > 0 ? 1 : 0]];
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
                Ca = a1 * C1 + a0_2;
                Caa = a1 * Ca + a0_3;
                Caaa = a1 * Caa + a0_4;
                Cb = b1 * (b1 + b0) + b0_2;
                Cbb = b1 * Cb + b0_3;
                Cbbb = b1 * Cbb + b0_4;
                Cab = 3 * a1_2 + 2 * a1 * a0 + a0_2;
                Kab = a1_2 + 2 * a1 * a0 + 3 * a0_2;
                Caab = a0 * Cab + 4 * a1_3;
                Kaab = a1 * Kab + 4 * a0_3;
                Cabb = 4 * b1_3 + 3 * b1_2 * b0 + 2 * b1 * b0_2 + b0_3;
                Kabb = b1_3 + 2 * b1_2 * b0 + 3 * b1 * b0_2 + 4 * b0_3;

                integrals.P1 += db * C1;
                integrals.Pa += db * Ca;
                integrals.Paa += db * Caa;
                integrals.Paaa += db * Caaa;
                integrals.Pb += da * Cb;
                integrals.Pbb += da * Cbb;
                integrals.Pbbb += da * Cbbb;
                integrals.Pab += db * (b1 * Cab + b0 * Kab);
                integrals.Paab += db * (b1 * Caab + b0 * Kaab);
                integrals.Pabb += da * (a1 * Cabb + a0 * Kabb);
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

            float Fa, Fb, Fc;
            float Faa, Fbb, Fcc;
            float Faaa, Fbbb, Fccc;
            float Faab, Fbbc, Fcca;
        };

        private void PolygonIntegrals(int polyNum, int a, int b, int c, polygonIntegrals_t integrals) {
            projectionIntegrals_t pi = new projectionIntegrals_t();
            idVec3 n;
            float w;
            float k1, k2, k3, k4;

            ProjectionIntegrals(polyNum, a, b, pi);

            n = polys[polyNum].normal;
            w = -polys[polyNum].dist;
            k1 = 1 / n.oGet(c);
            k2 = k1 * k1;
            k3 = k2 * k1;
            k4 = k3 * k1;

            integrals.Fa = k1 * pi.Pa;
            integrals.Fb = k1 * pi.Pb;
            integrals.Fc = -k2 * (n.oGet(a) * pi.Pa + n.oGet(b) * pi.Pb + w * pi.P1);

            integrals.Faa = k1 * pi.Paa;
            integrals.Fbb = k1 * pi.Pbb;
            integrals.Fcc = (float) (k3 * (Square(n.oGet(a)) * pi.Paa + 2 * n.oGet(a) * n.oGet(b) * pi.Pab + Square(n.oGet(b)) * pi.Pbb
                    + w * (2 * (n.oGet(a) * pi.Pa + n.oGet(b) * pi.Pb) + w * pi.P1)));

            integrals.Faaa = k1 * pi.Paaa;
            integrals.Fbbb = k1 * pi.Pbbb;
            integrals.Fccc = (float) (-k4 * (Cube(n.oGet(a)) * pi.Paaa + 3 * Square(n.oGet(a)) * n.oGet(b) * pi.Paab
                    + 3 * n.oGet(a) * Square(n.oGet(b)) * pi.Pabb + Cube(n.oGet(b)) * pi.Pbbb
                    + 3 * w * (Square(n.oGet(a)) * pi.Paa + 2 * n.oGet(a) * n.oGet(b) * pi.Pab + Square(n.oGet(b)) * pi.Pbb)
                    + w * w * (3 * (n.oGet(a) * pi.Pa + n.oGet(b) * pi.Pb) + w * pi.P1)));

            integrals.Faab = k1 * pi.Paab;
            integrals.Fbbc = -k2 * (n.oGet(a) * pi.Pabb + n.oGet(b) * pi.Pbbb + w * pi.Pbb);
            integrals.Fcca = (float) (k3 * (Square(n.oGet(a)) * pi.Paaa + 2 * n.oGet(a) * n.oGet(b) * pi.Paab + Square(n.oGet(b)) * pi.Pabb
                    + w * (2 * (n.oGet(a) * pi.Paa + n.oGet(b) * pi.Pab) + w * pi.Pa)));
        }

        class volumeIntegrals_t {

            float T0;
            idVec3 T1;
            idVec3 T2;
            idVec3 TP;

            volumeIntegrals_t() {
                T1 = new idVec3();
                T2 = new idVec3();
                TP = new idVec3();
            }
        };

        private void VolumeIntegrals(volumeIntegrals_t integrals) {
            traceModelPoly_t poly;
            polygonIntegrals_t pi = new polygonIntegrals_t();
            int i, a, b, c;
            float nx, ny, nz;

//	memset( &integrals, 0, sizeof(volumeIntegrals_t) );
            for (i = 0; i < numPolys; i++) {
                poly = polys[i];

                nx = idMath.Fabs(poly.normal.oGet(0));
                ny = idMath.Fabs(poly.normal.oGet(1));
                nz = idMath.Fabs(poly.normal.oGet(2));
                if (nx > ny && nx > nz) {
                    c = 0;
                } else {
                    c = (ny > nz) ? 1 : 2;
                }
                a = (c + 1) % 3;
                b = (a + 1) % 3;

                PolygonIntegrals(i, a, b, c, pi);

                integrals.T0 += poly.normal.oGet(0) * ((a == 0) ? pi.Fa : ((b == 0) ? pi.Fb : pi.Fc));

                integrals.T1.oPluSet(a, poly.normal.oGet(a) * pi.Faa);
                integrals.T1.oPluSet(b, poly.normal.oGet(b) * pi.Fbb);
                integrals.T1.oPluSet(c, poly.normal.oGet(c) * pi.Fcc);
                integrals.T2.oPluSet(a, poly.normal.oGet(a) * pi.Faaa);
                integrals.T2.oPluSet(b, poly.normal.oGet(b) * pi.Fbbb);
                integrals.T2.oPluSet(c, poly.normal.oGet(c) * pi.Fccc);
                integrals.TP.oPluSet(a, poly.normal.oGet(a) * pi.Faab);
                integrals.TP.oPluSet(b, poly.normal.oGet(b) * pi.Fbbc);
                integrals.TP.oPluSet(c, poly.normal.oGet(c) * pi.Fcca);
            }

            integrals.T1.oMulSet(0.5f);
            integrals.T2.oMulSet(1.0f / 3.0f);
            integrals.TP.oMulSet(0.5f);
        }

        private void VolumeFromPolygon(idTraceModel trm, float thickness) {
            int i;

            trm.oSet(this);
            trm.type = TRM_POLYGONVOLUME;
            trm.numVerts = numVerts * 2;
            trm.numEdges = numEdges * 3;
            trm.numPolys = numEdges + 2;
            for (i = 0; i < numEdges; i++) {
                trm.verts[numVerts + i].oSet(verts[i].oMinus(polys[0].normal.oMultiply(thickness)));
                trm.edges[ numEdges + i + 1].v[0] = numVerts + i;
                trm.edges[ numEdges + i + 1].v[1] = numVerts + (i + 1) % numVerts;
                trm.edges[ numEdges * 2 + i + 1].v[0] = i;
                trm.edges[ numEdges * 2 + i + 1].v[1] = numVerts + i;
                trm.polys[1].edges[i] = -(numEdges + i + 1);
                trm.polys[2 + i].numEdges = 4;
                trm.polys[2 + i].edges[0] = -(i + 1);
                trm.polys[2 + i].edges[1] = numEdges * 2 + i + 1;
                trm.polys[2 + i].edges[2] = numEdges + i + 1;
                trm.polys[2 + i].edges[3] = -(numEdges * 2 + (i + 1) % numEdges + 1);
                trm.polys[2 + i].normal = (verts[(i + 1) % numVerts].oMinus(verts[i])).Cross(polys[0].normal);
                trm.polys[2 + i].normal.Normalize();
                trm.polys[2 + i].dist = trm.polys[2 + i].normal.oMultiply(verts[i]);
            }
            trm.polys[1].dist = trm.polys[1].normal.oMultiply(trm.verts[ numEdges]);

            trm.GenerateEdgeNormals();
        }

        private int GetOrderedSilhouetteEdges(final int edgeIsSilEdge[], int silEdges[]) {
            int i, j, edgeNum, numSilEdges, nextSilVert;
            int[] unsortedSilEdges = new int[MAX_TRACEMODEL_EDGES];

            numSilEdges = 0;
            for (i = 1; i <= numEdges; i++) {
                if (edgeIsSilEdge[i] != 0) {
                    unsortedSilEdges[numSilEdges++] = i;
                }
            }

            silEdges[0] = unsortedSilEdges[0];
            unsortedSilEdges[0] = -1;
            nextSilVert = edges[silEdges[0]].v[0];
            for (i = 1; i < numSilEdges; i++) {
                for (j = 1; j < numSilEdges; j++) {
                    edgeNum = unsortedSilEdges[j];
                    if (edgeNum >= 0) {
                        if (edges[edgeNum].v[0] == nextSilVert) {
                            nextSilVert = edges[edgeNum].v[1];
                            silEdges[i] = edgeNum;
                            break;
                        }
                        if (edges[edgeNum].v[1] == nextSilVert) {
                            nextSilVert = edges[edgeNum].v[0];
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
    };
}
