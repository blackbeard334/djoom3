package neo.idlib.geometry;

import neo.idlib.BV.Bounds.idBounds;
import static neo.idlib.containers.List.idSwap;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Surface.idSurface;
import static neo.idlib.geometry.Winding.*;
import neo.idlib.geometry.Winding.idFixedWinding;
import static neo.idlib.math.Math_h.*;
import static neo.idlib.math.Plane.*;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Surface_Polytope {

    static final float POLYTOPE_VERTEX_EPSILON = 0.1f;
    /*
     ===============================================================================

     Polytope surface.

     NOTE: vertexes are not duplicated for texture coordinates.

     ===============================================================================
     */

    class idSurface_Polytope extends idSurface {

        public idSurface_Polytope() {
        }

        public void FromPlanes(final idPlane[] planes, final int numPlanes) {
            int i, j, k;
            int[] windingVerts;
            idFixedWinding w = new idFixedWinding();
            idDrawVert newVert = new idDrawVert();

            windingVerts = new int[MAX_POINTS_ON_WINDING];
//	memset( &newVert, 0, sizeof( newVert ) );

            for (i = 0; i < numPlanes; i++) {

                w.BaseForPlane(planes[i]);

                for (j = 0; j < numPlanes; j++) {
                    if (j == i) {
                        continue;
                    }
                    if (!w.ClipInPlace(planes[j].oNegative(), ON_EPSILON, true)) {
                        break;
                    }
                }
                if (0 == w.GetNumPoints()) {
                    continue;
                }

                for (j = 0; j < w.GetNumPoints(); j++) {
                    for (k = 0; k < verts.Num(); j++) {
                        if (verts.oGet(k).xyz.Compare(w.oGet(j).ToVec3(), POLYTOPE_VERTEX_EPSILON)) {
                            break;
                        }
                    }
                    if (k >= verts.Num()) {
                        newVert.xyz = w.oGet(j).ToVec3();
                        k = verts.Append(newVert);
                    }
                    windingVerts[j] = k;
                }

                for (j = 2; j < w.GetNumPoints(); j++) {
                    indexes.Append(windingVerts[0]);
                    indexes.Append(windingVerts[j - 1]);
                    indexes.Append(windingVerts[j]);
                }
            }

            GenerateEdgeIndexes();
        }

        public void SetupTetrahedron(final idBounds bounds) {
            idVec3 center, scale;
            float c1, c2, c3;

            c1 = 0.4714045207f;
            c2 = 0.8164965809f;
            c3 = -0.3333333333f;

            center = bounds.GetCenter();
            scale = bounds.oGet(1).oMinus(center);

            verts.SetNum(4);
            verts.oGet(0).xyz = center.oPlus(new idVec3(0.0f, 0.0f, scale.z));
            verts.oGet(1).xyz = center.oPlus(new idVec3(2.0f * c1 * scale.x, 0.0f, c3 * scale.z));
            verts.oGet(2).xyz = center.oPlus(new idVec3(-c1 * scale.x, c2 * scale.y, c3 * scale.z));
            verts.oGet(3).xyz = center.oPlus(new idVec3(-c1 * scale.x, -c2 * scale.y, c3 * scale.z));

            indexes.SetNum(4 * 3);
            indexes.oSet(0 * 3 + 0, 0);
            indexes.oSet(0 * 3 + 1, 1);
            indexes.oSet(0 * 3 + 2, 2);
            indexes.oSet(1 * 3 + 0, 0);
            indexes.oSet(1 * 3 + 1, 2);
            indexes.oSet(1 * 3 + 2, 3);
            indexes.oSet(2 * 3 + 0, 0);
            indexes.oSet(2 * 3 + 1, 3);
            indexes.oSet(2 * 3 + 2, 1);
            indexes.oSet(3 * 3 + 0, 1);
            indexes.oSet(3 * 3 + 1, 3);
            indexes.oSet(3 * 3 + 2, 2);

            GenerateEdgeIndexes();
        }

        public void SetupHexahedron(final idBounds bounds) {
            idVec3 center, scale;

            center = bounds.GetCenter();
            scale = bounds.oGet(1).oMinus(center);

            verts.SetNum(8);
            verts.oGet(0).xyz = center.oPlus(new idVec3(-scale.x, -scale.y, -scale.z));
            verts.oGet(1).xyz = center.oPlus(new idVec3(scale.x, -scale.y, -scale.z));
            verts.oGet(2).xyz = center.oPlus(new idVec3(scale.x, scale.y, -scale.z));
            verts.oGet(3).xyz = center.oPlus(new idVec3(-scale.x, scale.y, -scale.z));
            verts.oGet(4).xyz = center.oPlus(new idVec3(-scale.x, -scale.y, scale.z));
            verts.oGet(5).xyz = center.oPlus(new idVec3(scale.x, -scale.y, scale.z));
            verts.oGet(6).xyz = center.oPlus(new idVec3(scale.x, scale.y, scale.z));
            verts.oGet(7).xyz = center.oPlus(new idVec3(-scale.x, scale.y, scale.z));

            indexes.SetNum(12 * 3);
            indexes.oSet(0 * 3 + 0, 0);
            indexes.oSet(0 * 3 + 1, 3);
            indexes.oSet(0 * 3 + 2, 2);
            indexes.oSet(1 * 3 + 0, 0);
            indexes.oSet(1 * 3 + 1, 2);
            indexes.oSet(1 * 3 + 2, 1);
            indexes.oSet(2 * 3 + 0, 0);
            indexes.oSet(2 * 3 + 1, 1);
            indexes.oSet(2 * 3 + 2, 5);
            indexes.oSet(3 * 3 + 0, 0);
            indexes.oSet(3 * 3 + 1, 5);
            indexes.oSet(3 * 3 + 2, 4);
            indexes.oSet(4 * 3 + 0, 0);
            indexes.oSet(4 * 3 + 1, 4);
            indexes.oSet(4 * 3 + 2, 7);
            indexes.oSet(5 * 3 + 0, 0);
            indexes.oSet(5 * 3 + 1, 7);
            indexes.oSet(5 * 3 + 2, 3);
            indexes.oSet(6 * 3 + 0, 6);
            indexes.oSet(6 * 3 + 1, 5);
            indexes.oSet(6 * 3 + 2, 1);
            indexes.oSet(7 * 3 + 0, 6);
            indexes.oSet(7 * 3 + 1, 1);
            indexes.oSet(7 * 3 + 2, 2);
            indexes.oSet(8 * 3 + 0, 6);
            indexes.oSet(8 * 3 + 1, 2);
            indexes.oSet(8 * 3 + 2, 3);
            indexes.oSet(9 * 3 + 0, 6);
            indexes.oSet(9 * 3 + 1, 3);
            indexes.oSet(9 * 3 + 2, 7);
            indexes.oSet(10 * 3 + 0, 6);
            indexes.oSet(10 * 3 + 1, 7);
            indexes.oSet(10 * 3 + 2, 4);
            indexes.oSet(11 * 3 + 0, 6);
            indexes.oSet(11 * 3 + 1, 4);
            indexes.oSet(11 * 3 + 2, 5);

            GenerateEdgeIndexes();
        }

        public void SetupOctahedron(final idBounds bounds) {
            idVec3 center, scale;

            center = bounds.GetCenter();
            scale = bounds.oGet(1).oMinus(center);

            verts.SetNum(6);
            verts.oGet(0).xyz = center.oPlus(new idVec3(scale.x, 0.0f, 0.0f));
            verts.oGet(1).xyz = center.oPlus(new idVec3(-scale.x, 0.0f, 0.0f));
            verts.oGet(2).xyz = center.oPlus(new idVec3(0.0f, scale.y, 0.0f));
            verts.oGet(3).xyz = center.oPlus(new idVec3(0.0f, -scale.y, 0.0f));
            verts.oGet(4).xyz = center.oPlus(new idVec3(0.0f, 0.0f, scale.z));
            verts.oGet(5).xyz = center.oPlus(new idVec3(0.0f, 0.0f, -scale.z));

            indexes.SetNum(8 * 3);
            indexes.oSet(0 * 3 + 0, 4);
            indexes.oSet(0 * 3 + 1, 0);
            indexes.oSet(0 * 3 + 2, 2);
            indexes.oSet(1 * 3 + 0, 4);
            indexes.oSet(1 * 3 + 1, 2);
            indexes.oSet(1 * 3 + 2, 1);
            indexes.oSet(2 * 3 + 0, 4);
            indexes.oSet(2 * 3 + 1, 1);
            indexes.oSet(2 * 3 + 2, 3);
            indexes.oSet(3 * 3 + 0, 4);
            indexes.oSet(3 * 3 + 1, 3);
            indexes.oSet(3 * 3 + 2, 0);
            indexes.oSet(4 * 3 + 0, 5);
            indexes.oSet(4 * 3 + 1, 2);
            indexes.oSet(4 * 3 + 2, 0);
            indexes.oSet(5 * 3 + 0, 5);
            indexes.oSet(5 * 3 + 1, 1);
            indexes.oSet(5 * 3 + 2, 0);
            indexes.oSet(6 * 3 + 0, 5);
            indexes.oSet(6 * 3 + 1, 3);
            indexes.oSet(6 * 3 + 2, 1);
            indexes.oSet(7 * 3 + 0, 5);
            indexes.oSet(7 * 3 + 1, 0);
            indexes.oSet(7 * 3 + 2, 3);

            GenerateEdgeIndexes();
        }
//public	void				SetupDodecahedron( const idBounds &bounds );
//public	void				SetupIcosahedron( const idBounds &bounds );
//public	void				SetupCylinder( const idBounds &bounds, const int numSides );
//public	void				SetupCone( const idBounds &bounds, const int numSides );
//

        public int SplitPolytope(final idPlane plane, final float epsilon, idSurface_Polytope[] front, idSurface_Polytope[] back) {
            int side, i, j, s, v0, v1, v2, edgeNum;
            idSurface[][][] surface = new idSurface[2][1][1];
            idSurface_Polytope[] polytopeSurfaces = new idSurface_Polytope[2];
            idSurface_Polytope surf;
            int[][] onPlaneEdges = new int[2][];

            onPlaneEdges[0] = new int[indexes.Num() / 3];
            onPlaneEdges[1] = new int[indexes.Num() / 3];

            side = Split(plane, epsilon, surface[0], surface[1], onPlaneEdges[0], onPlaneEdges[1]);

            front[0] = polytopeSurfaces[0] = new idSurface_Polytope();
            back[0] = polytopeSurfaces[1] = new idSurface_Polytope();

            for (s = 0; s < 2; s++) {
                if (surface[s][1][1] != null) {
                    polytopeSurfaces[s] = new idSurface_Polytope();
                    polytopeSurfaces[s].SwapTriangles(surface[s][1][1]);
//                    delete surface[s];
                    surface[s][1][1] = null;
                }
            }

            front[0] = polytopeSurfaces[0];
            back[0] = polytopeSurfaces[1];

            if (side != SIDE_CROSS) {
                return side;
            }

            // add triangles to close off the front and back polytope
            for (s = 0; s < 2; s++) {

                surf = polytopeSurfaces[s];

                edgeNum = surf.edgeIndexes.oGet(onPlaneEdges[s][0]);
                v0 = surf.edges.oGet(Math.abs(edgeNum)).verts[INTSIGNBITSET(edgeNum)];
                v1 = surf.edges.oGet(Math.abs(edgeNum)).verts[INTSIGNBITNOTSET(edgeNum)];

                for (i = 1; onPlaneEdges[s][i] >= 0; i++) {
                    for (j = i + 1; onPlaneEdges[s][j] >= 0; j++) {
                        edgeNum = surf.edgeIndexes.oGet(onPlaneEdges[s][j]);
                        if (v1 == surf.edges.oGet(Math.abs(edgeNum)).verts[INTSIGNBITSET(edgeNum)]) {
                            v1 = surf.edges.oGet(Math.abs(edgeNum)).verts[INTSIGNBITNOTSET(edgeNum)];
                            idSwap(onPlaneEdges, s, i, onPlaneEdges, s, j);
                            break;
                        }
                    }
                }

                for (i = 2; onPlaneEdges[s][i] >= 0; i++) {
                    edgeNum = surf.edgeIndexes.oGet(onPlaneEdges[s][i]);
                    v1 = surf.edges.oGet(Math.abs(edgeNum)).verts[INTSIGNBITNOTSET(edgeNum)];
                    v2 = surf.edges.oGet(Math.abs(edgeNum)).verts[INTSIGNBITSET(edgeNum)];
                    surf.indexes.Append(v0);
                    surf.indexes.Append(v1);
                    surf.indexes.Append(v2);
                }

                surf.GenerateEdgeIndexes();
            }

            return side;
        }
    };
}
