package neo.Tools.Compilers.DMap;

import static neo.TempDump.NOT;
import neo.Tools.Compilers.DMap.dmap.mapTri_s;
import static neo.Tools.Compilers.DMap.gldraw.DrawWinding;
import neo.Tools.Compilers.DMap.optimize.optVertex_s;
import neo.Tools.Compilers.DMap.tritjunction.hashVert_s;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Vector.VectorCopy;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class tritools {

    /*

     All triangle list functions should behave reasonably with NULL lists.

     */

    /*
     ===============
     AllocTri
     ===============
     */
    static mapTri_s AllocTri() {
        mapTri_s tri;

        tri = new mapTri_s();// Mem_Alloc(sizeof(tri));
//	memset( tri, 0, sizeof( *tri ) );
        return tri;
    }

    /*
     ===============
     FreeTri
     ===============
     */
    static void FreeTri(mapTri_s tri) {
        tri.clear();//Mem_Free(tri);
    }


    /*
     ===============
     MergeTriLists

     This does not copy any tris, it just relinks them
     ===============
     */
    static mapTri_s MergeTriLists(mapTri_s a, mapTri_s b) {
        mapTri_s prev;

        prev = a;
        while (prev != null && prev.next != null) {
            prev = prev.next;
        }

        prev.next = b;

        return a;
    }


    /*
     ===============
     FreeTriList
     ===============
     */
    static void FreeTriList(mapTri_s a) {
        mapTri_s next;

        for (; a != null; a = next) {
            next = a.next;
            a.clear();//Mem_Free(a);
        }
    }

    /*
     ===============
     CopyTriList
     ===============
     */
    static mapTri_s CopyTriList(final mapTri_s a) {
        mapTri_s testList;
        mapTri_s tri;

        testList = null;
        for (tri = a; tri != null; tri = tri.next) {
            mapTri_s copy;

            copy = CopyMapTri(tri);
            copy.next = testList;
            testList = copy;
        }

        return testList;
    }


    /*
     =============
     CountTriList
     =============
     */
    static int CountTriList( /*final*/mapTri_s tri) {
        int c;

        c = 0;
        while (tri != null) {
            c++;
            tri = tri.next;
        }

        return c;
    }


    /*
     ===============
     CopyMapTri
     ===============
     */
    static mapTri_s CopyMapTri(final mapTri_s tri) {
        mapTri_s t;

//        t = (mapTri_s) Mem_Alloc(sizeof(t));
        t = tri;

        return t;
    }

    /*
     ===============
     MapTriArea
     ===============
     */
    static float MapTriArea(final mapTri_s tri) {
        return idWinding.TriangleArea(tri.v[0].xyz, tri.v[1].xyz, tri.v[2].xyz);
    }

    /*
     ===============
     RemoveBadTris

     Return a new list with any zero or negative area triangles removed
     ===============
     */
    static mapTri_s RemoveBadTris(final mapTri_s list) {
        mapTri_s newList;
        mapTri_s copy;
        mapTri_s tri;

        newList = null;

        for (tri = list; tri != null; tri = tri.next) {
            if (MapTriArea(tri) > 0) {
                copy = CopyMapTri(tri);
                copy.next = newList;
                newList = copy;
            }
        }

        return newList;
    }

    /*
     ================
     BoundTriList
     ================
     */
    static void BoundTriList( /*final*/mapTri_s list, idBounds b) {
        b.Clear();
        for (; list != null; list = list.next) {
            b.AddPoint(list.v[0].xyz);
            b.AddPoint(list.v[1].xyz);
            b.AddPoint(list.v[2].xyz);
        }
    }

    /*
     ================
     DrawTri
     ================
     */
    static void DrawTri(final mapTri_s tri) {
        idWinding w = new idWinding();

        w.SetNumPoints(3);
        VectorCopy(tri.v[0].xyz, w.oGet(0));
        VectorCopy(tri.v[1].xyz, w.oGet(1));
        VectorCopy(tri.v[2].xyz, w.oGet(2));
        DrawWinding(w);
    }


    /*
     ================
     FlipTriList

     Swaps the vertex order
     ================
     */
    static void FlipTriList(mapTri_s tris) {
        mapTri_s tri;

        for (tri = tris; tri != null; tri = tri.next) {
            idDrawVert v;
            hashVert_s hv;
            optVertex_s ov;

            v = tri.v[0];
            tri.v[0] = tri.v[2];
            tri.v[2] = v;

            hv = tri.hashVert[0];
            tri.hashVert[0] = tri.hashVert[2];
            tri.hashVert[2] = hv;

            ov = tri.optVert[0];
            tri.optVert[0] = tri.optVert[2];
            tri.optVert[2] = ov;
        }
    }

    /*
     ================
     WindingForTri
     ================
     */
    static idWinding WindingForTri(final mapTri_s tri) {
        idWinding w;

        w = new idWinding(3);
        w.SetNumPoints(3);
        VectorCopy(tri.v[0].xyz, w.oGet(0));
        VectorCopy(tri.v[1].xyz, w.oGet(1));
        VectorCopy(tri.v[2].xyz, w.oGet(2));

        return w;
    }

    /*
     ================
     TriVertsFromOriginal

     Regenerate the texcoords and colors on a fragmented tri from the plane equations
     ================
     */
    static void TriVertsFromOriginal(mapTri_s tri, final mapTri_s original) {
        int i, j;
        float denom;

        denom = idWinding.TriangleArea(original.v[0].xyz, original.v[1].xyz, original.v[2].xyz);
        if (denom == 0) {
            return;		// original was degenerate, so it doesn't matter
        }

        for (i = 0; i < 3; i++) {
            float a, b, c;

            // find the barycentric coordinates
            a = idWinding.TriangleArea(tri.v[i].xyz, original.v[1].xyz, original.v[2].xyz) / denom;
            b = idWinding.TriangleArea(tri.v[i].xyz, original.v[2].xyz, original.v[0].xyz) / denom;
            c = idWinding.TriangleArea(tri.v[i].xyz, original.v[0].xyz, original.v[1].xyz) / denom;

            // regenerate the interpolated values
            tri.v[i].st.oSet(0, a * original.v[0].st.oGet(0)
                    + b * original.v[1].st.oGet(0) + c * original.v[2].st.oGet(0));
            tri.v[i].st.oSet(1, a * original.v[0].st.oGet(1)
                    + b * original.v[1].st.oGet(1) + c * original.v[2].st.oGet(1));

            for (j = 0; j < 3; j++) {
                tri.v[i].normal.oSet(j, a * original.v[0].normal.oGet(j)
                        + b * original.v[1].normal.oGet(j) + c * original.v[2].normal.oGet(j));
            }
            tri.v[i].normal.Normalize();
        }
    }

    /*
     ================
     WindingToTriList

     Generates a new list of triangles with proper texcoords from a winding
     created by clipping the originalTri

     OriginalTri can be NULL if you don't care about texCoords
     ================
     */
    static mapTri_s WindingToTriList(final idWinding w, final mapTri_s originalTri) {
        mapTri_s tri;
        mapTri_s triList;
        int i, j;
        idVec3 vec;

        if (NOT(w)) {
            return null;
        }

        triList = null;
        for (i = 2; i < w.GetNumPoints(); i++) {
            tri = AllocTri();
            if (NOT(originalTri)) {
//			memset( tri, 0, sizeof( *tri ) );
                tri = new mapTri_s();
            } else {
                tri = originalTri;
            }
            tri.next = triList;//TODO:what happens here?
            triList = tri;

            for (j = 0; j < 3; j++) {
                if (j == 0) {
                    vec = w.oGet(0).ToVec3();
                } else if (j == 1) {
                    vec = w.oGet(i - 1).ToVec3();
                } else {
                    vec = w.oGet(i).ToVec3();
                }

                VectorCopy(vec, tri.v[j].xyz);
            }
            if (originalTri != null) {
                TriVertsFromOriginal(tri, originalTri);
            }
        }

        return triList;
    }


    /*
     ==================
     ClipTriList
     ==================
     */
    static void ClipTriList(final mapTri_s list, final idPlane plane, float epsilon,
            mapTri_s front, mapTri_s back) {
        mapTri_s tri;
        mapTri_s newList;
        idWinding w, frontW = new idWinding(), backW = new idWinding();

//        front[0] = null;
//        back[0] = null;
        for (tri = list; tri != null; tri = tri.next) {
            w = WindingForTri(tri);
            w.Split(plane, epsilon, frontW, backW);

            newList = WindingToTriList(frontW, tri);
            front = MergeTriLists(front, newList);

            newList = WindingToTriList(backW, tri);
            back = MergeTriLists(back, newList);

//		delete w;
        }

    }

    /*
     ==================
     PlaneForTri
     ==================
     */
    static void PlaneForTri(final mapTri_s tri, idPlane plane) {
        plane.FromPoints(tri.v[0].xyz, tri.v[1].xyz, tri.v[2].xyz);
    }
}
