package neo.Tools.Compilers.DMap;

import java.util.Arrays;
import static neo.Renderer.Interaction.R_FreeInteractionCullInfo;
import neo.Renderer.Interaction.srfCullInfo_t;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.tr_local.idRenderEntityLocal;
import neo.Renderer.tr_local.optimizedShadow_t;
import static neo.Renderer.tr_stencilshadow.R_CreateShadowVolume;
import static neo.Renderer.tr_stencilshadow.R_LightProjectionMatrix;
import static neo.Renderer.tr_stencilshadow.shadowGen_t.SG_OFFLINE;
import static neo.Renderer.tr_stencilshadow.shadowGen_t.SG_STATIC;
import static neo.Renderer.tr_trisurf.R_CleanupTriangles;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurf;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import neo.Tools.Compilers.DMap.dmap.mapLight_t;
import neo.Tools.Compilers.DMap.dmap.mapTri_s;
import neo.Tools.Compilers.DMap.dmap.optimizeGroup_s;
import static neo.Tools.Compilers.DMap.dmap.shadowOptLevel_t.SO_CLIP_SILS;
import static neo.Tools.Compilers.DMap.dmap.shadowOptLevel_t.SO_CULL_OCCLUDED;
import static neo.Tools.Compilers.DMap.dmap.shadowOptLevel_t.SO_MERGE_SURFACES;
import static neo.Tools.Compilers.DMap.dmap.shadowOptLevel_t.SO_SIL_OPTIMIZE;
import static neo.Tools.Compilers.DMap.map.FindFloatPlane;
import static neo.Tools.Compilers.DMap.map.FreeOptimizeGroupList;
import static neo.Tools.Compilers.DMap.optimize.OptimizeGroupList;
import static neo.Tools.Compilers.DMap.output.ShareMapTriVerts;
import neo.Tools.Compilers.DMap.shadowopt3.shadowOptEdge_s;
import neo.Tools.Compilers.DMap.shadowopt3.shadowTri_t;
import neo.Tools.Compilers.DMap.shadowopt3.silPlane_t;
import neo.Tools.Compilers.DMap.shadowopt3.silQuad_s;
import static neo.Tools.Compilers.DMap.tritools.CopyTriList;
import static neo.Tools.Compilers.DMap.tritools.FreeTriList;
import static neo.Tools.Compilers.DMap.tritools.MergeTriLists;
import static neo.framework.Common.common;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.geometry.Winding.idWinding;
import static neo.idlib.math.Plane.ON_EPSILON;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class shadowopt3 {

    /*

     given a set of faces that are clipped to the required frustum

     make 2D projection for each vertex

     for each edge
     add edge, generating new points at each edge intersection

     ?add all additional edges to make a full triangulation

     make full triangulation

     for each triangle
     find midpoint
     find original triangle with midpoint closest to view
     annotate triangle with that data
     project all vertexes to that plane
     output the triangle as a front cap

     snap all vertexes
     make a back plane projection for all vertexes

     for each edge
     if one side doesn't have a triangle
     make a sil edge to back plane projection
     continue
     if triangles on both sides have two verts in common
     continue
     make a sil edge from one triangle to the other
		



     classify triangles on common planes, so they can be optimized 

     what about interpenetrating triangles???

     a perfect shadow volume will have every edge exactly matched with
     an opposite, and no two triangles covering the same area on either
     the back projection or a silhouette edge.

     Optimizing the triangles on the projected plane can give a significant
     improvement, but the quadratic time nature of the optimization process
     probably makes it untenable.

     There exists some small room for further triangle count optimizations of the volumes
     by collapsing internal surface geometry in some cases, or allowing original triangles
     to extend outside the exactly light frustum without being clipped, but it probably
     isn't worth it.

     Triangle count optimizations at the expense of a slight fill rate cost
     may be apropriate in some cases.


     Perform the complete clipping on all triangles
     for each vertex
     project onto the apropriate plane and mark plane bit as in use
     for each triangle
     if points project onto different planes, clip 
     */
    static final int           MAX_SHADOW_TRIS = 32768;
    //
    static       shadowTri_t[] outputTris      = new shadowTri_t[MAX_SHADOW_TRIS];
    static int numOutputTris;
    //
    static final int               MAX_SIL_EDGES = MAX_SHADOW_TRIS * 3;
    static       shadowOptEdge_s[] silEdges      = new shadowOptEdge_s[MAX_SIL_EDGES];
    static int numSilEdges;
    //
    static final int         MAX_SIL_QUADS = MAX_SHADOW_TRIS * 3;
    static       silQuad_s[] silQuads      = new silQuad_s[MAX_SIL_QUADS];
    static int numSilQuads;
    //
    static float EDGE_PLANE_EPSILON = 0.1f;
    static float UNIQUE_EPSILON     = 0.1f;
    //
    static int               numSilPlanes;
    static silPlane_t[]      silPlanes;
    //
// the uniqued verts are still in projection centered space, not global space
    static int               numUniqued;
    static int               numUniquedBeforeProjection;
    static int               maxUniqued;
    static idVec3[]          uniqued;
    //
    static optimizedShadow_t ret;
    static int               maxRetIndexes;
    //
    static final float EDGE_EPSILON = 0.1f;

    static class shadowTri_t {

        idVec3[]           v     = new idVec3[3];
        idVec3[]           edge  = new idVec3[3];   // positive side is inside the triangle
        int/*glIndex_t*/[] index = new int[3];
        idPlane plane;                              // positive side is forward for the triangle, which is away from the light
        int     planeNum;                           // from original triangle, not calculated from the clipped verts
    };

    static class shadowOptEdge_s {

        int/*glIndex_t*/[] index = new int[2];
        shadowOptEdge_s nextEdge;
    };

    static class silQuad_s {

        int[] nearV = new int[2];
        int[] farV  = new int[2];        // will always be a projection of near[]
        silQuad_s nextQuad;
    };

    static class silPlane_t {

        idVec3          normal;    // all sil planes go through the projection origin
        shadowOptEdge_s edges;
        silQuad_s       fragmentedQuads;
    };

//static int FindUniqueVert( idVec3 v );
//=====================================================================================

    /*
     =================
     CreateEdgesForTri
     =================
     */
    static void CreateEdgesForTri(shadowTri_t tri) {
        for (int j = 0; j < 3; j++) {
            idVec3 v1 = tri.v[j];
            idVec3 v2 = tri.v[(j + 1) % 3];

            tri.edge[j].Cross(v2, v1);
            tri.edge[j].Normalize();
        }
    }

    static boolean TriOutsideTri(final shadowTri_t a, final shadowTri_t b) {
//#if 0
//	if ( a.v[0] * b.edge[0] <= EDGE_EPSILON
//		&& a.v[1] * b.edge[0] <= EDGE_EPSILON
//		&& a.v[2] * b.edge[0] <= EDGE_EPSILON ) {
//			return true;
//		}
//	if ( a.v[0] * b.edge[1] <= EDGE_EPSILON
//		&& a.v[1] * b.edge[1] <= EDGE_EPSILON
//		&& a.v[2] * b.edge[1] <= EDGE_EPSILON ) {
//			return true;
//		}
//	if ( a.v[0] * b.edge[2] <= EDGE_EPSILON
//		&& a.v[1] * b.edge[2] <= EDGE_EPSILON
//		&& a.v[2] * b.edge[2] <= EDGE_EPSILON ) {
//			return true;
//		}
//#else
        for (int i = 0; i < 3; i++) {
            int j;
            for (j = 0; j < 3; j++) {
                float d = a.v[j].oMultiply(b.edge[i]);
                if (d > EDGE_EPSILON) {
                    break;
                }
            }
            if (j == 3) {
                return true;
            }
        }
//#endif
        return false;
    }

    static boolean TriBehindTri(final shadowTri_t a, final shadowTri_t b) {
        float d;

        d = b.plane.Distance(a.v[0]);
        if (d > 0) {
            return true;
        }
        d = b.plane.Distance(a.v[1]);
        if (d > 0) {
            return true;
        }
        d = b.plane.Distance(a.v[2]);
        if (d > 0) {
            return true;
        }

        return false;
    }

    /*
     ===================
     ClipTriangle_r
     ===================
     */
    static int c_removedFragments;

    static void ClipTriangle_r(final shadowTri_t tri, int startTri, int skipTri, int numTris, final shadowTri_t[] tris) {
        // create edge planes for this triangle

        // compare against all the other triangles
        for (int i = startTri; i < numTris; i++) {
            if (i == skipTri) {
                continue;
            }
            final shadowTri_t other = tris[i];

            if (TriOutsideTri(tri, other)) {
                continue;
            }
            if (TriOutsideTri(other, tri)) {
                continue;
            }
            // they overlap to some degree

            // if other is behind tri, it doesn't clip it
            if (!TriBehindTri(tri, other)) {
                continue;
            }

            // clip it
            idWinding w = new idWinding(tri.v, 3);

            for (int j = 0; j < 4 && !w.isNULL(); j++) {
                idWinding front = new idWinding(), back = new idWinding();

                // keep any portion in front of other's plane
                if (j == 0) {
                    w.Split(other.plane, ON_EPSILON, front, back);
                } else {
                    w.Split(new idPlane(other.edge[j - 1], 0.0f), ON_EPSILON, front, back);
                }
                if (!back.isNULL()) {
                    // recursively clip these triangles to all subsequent triangles
                    for (int k = 2; k < back.GetNumPoints(); k++) {
                        shadowTri_t fragment = tri;

                        fragment.v[0] = back.oGet(0).ToVec3();
                        fragment.v[1] = back.oGet(k - 1).ToVec3();
                        fragment.v[2] = back.oGet(k).ToVec3();
                        CreateEdgesForTri(fragment);
                        ClipTriangle_r(fragment, i + 1, skipTri, numTris, tris);
                    }
//				delete back;
                }

//			delete w;
                w = front;
            }
//		if ( w ) {
//			delete w;
//		}

            c_removedFragments++;
            // any fragments will have been added recursively
            return;
        }

        // this fragment is frontmost, so add it to the output list
        if (numOutputTris == MAX_SHADOW_TRIS) {
            common.Error("numOutputTris == MAX_SHADOW_TRIS");
        }

        outputTris[numOutputTris] = tri;
        numOutputTris++;
    }


    /*
     ====================
     ClipOccluders

     Generates outputTris by clipping all the triangles against each other,
     retaining only those closest to the projectionOrigin
     ====================
     */
    static void ClipOccluders(idVec4[] verts, int/*glIndex_t*/[] indexes, int numIndexes, idVec3 projectionOrigin) {
        int numTris = numIndexes / 3;
        int i;
        shadowTri_t[] tris = new shadowTri_t[numTris];
        shadowTri_t tri;

        common.Printf("ClipOccluders: %d triangles\n", numTris);

        for (i = 0; i < numTris; i++) {
            tri = tris[i];

            // the indexes are in reversed order from tr_stencilshadow
            tri.v[0] = verts[indexes[i * 3 + 2]].ToVec3().oMinus(projectionOrigin);
            tri.v[1] = verts[indexes[i * 3 + 1]].ToVec3().oMinus(projectionOrigin);
            tri.v[2] = verts[indexes[i * 3 + 0]].ToVec3().oMinus(projectionOrigin);

            idVec3 d1 = tri.v[1].oMinus(tri.v[0]);
            idVec3 d2 = tri.v[2].oMinus(tri.v[0]);

            tri.plane.ToVec4().ToVec3().Cross(d2, d1);
            tri.plane.ToVec4().ToVec3().Normalize();
            tri.plane.oSet(3, tri.v[0].oMultiply(tri.plane.ToVec4().ToVec3()));

            // get the plane number before any clipping
            // we should avoid polluting the regular dmap planes with these
            // that are offset from the light origin...
            tri.planeNum = FindFloatPlane(tri.plane);

            CreateEdgesForTri(tri);
        }

        // clear our output buffer
        numOutputTris = 0;

        // for each triangle, clip against all other triangles
        int numRemoved = 0;
        int numComplete = 0;
        int numFragmented = 0;

        for (i = 0; i < numTris; i++) {
            int oldOutput = numOutputTris;
            c_removedFragments = 0;
            ClipTriangle_r(tris[i], 0, i, numTris, tris);
            if (numOutputTris == oldOutput) {
                numRemoved++;		// completely unused
            } else if (c_removedFragments == 0) {
                // the entire triangle is visible
                numComplete++;
                shadowTri_t out = outputTris[oldOutput] = tris[i];
                numOutputTris = oldOutput + 1;
            } else {
                numFragmented++;
                // we made at least one fragment

                // if we are at the low optimization level, just use a single
                // triangle if it produced any fragments
                if (dmapGlobals.shadowOptLevel == SO_CULL_OCCLUDED) {
                    shadowTri_t out = outputTris[oldOutput] = tris[i];//TODO:useless
                    numOutputTris = oldOutput + 1;
                }
            }
        }
        common.Printf("%d triangles completely invisible\n", numRemoved);
        common.Printf("%d triangles completely visible\n", numComplete);
        common.Printf("%d triangles fragmented\n", numFragmented);
        common.Printf("%d shadowing fragments before optimization\n", numOutputTris);
    }

//=====================================================================================

    /*
     ================
     OptimizeOutputTris
     ================
     */
    static void OptimizeOutputTris() {
        int i;

        // optimize the clipped surfaces
        optimizeGroup_s optGroups = null;
        optimizeGroup_s checkGroup;

        for (i = 0; i < numOutputTris; i++) {
            shadowTri_t tri = outputTris[i];

            int planeNum = tri.planeNum;

            // add it to an optimize group
            for (checkGroup = optGroups; checkGroup != null; checkGroup = checkGroup.nextGroup) {
                if (checkGroup.planeNum == planeNum) {
                    break;
                }
            }
            if (NOT(checkGroup)) {
                // create a new optGroup
                checkGroup = new optimizeGroup_s();// Mem_ClearedAlloc(sizeof(checkGroup));
                checkGroup.planeNum = planeNum;
                checkGroup.nextGroup = optGroups;
                optGroups = checkGroup;
            }

            // create a mapTri for the optGroup
            mapTri_s mtri = new mapTri_s();// Mem_ClearedAlloc(sizeof(mtri));
            mtri.v[0].xyz = tri.v[0];
            mtri.v[1].xyz = tri.v[1];
            mtri.v[2].xyz = tri.v[2];
            mtri.next = checkGroup.triList;
            checkGroup.triList = mtri;
        }

        OptimizeGroupList(optGroups);

        numOutputTris = 0;
        for (checkGroup = optGroups; checkGroup != null; checkGroup = checkGroup.nextGroup) {
            for (mapTri_s mtri = checkGroup.triList; mtri != null; mtri = mtri.next) {
                shadowTri_t tri = outputTris[numOutputTris];
                numOutputTris++;
                tri.v[0] = mtri.v[0].xyz;
                tri.v[1] = mtri.v[1].xyz;
                tri.v[2] = mtri.v[2].xyz;
            }
        }
        FreeOptimizeGroupList(optGroups);
    }

//==================================================================================
    @Deprecated
    static class EdgeSort implements cmp_t<Long> {

        @Override
        public int compare(Long a, Long b) {
//	if ( *(unsigned *)a < *(unsigned *)b ) {
//		return -1;
//	}
//	if ( *(unsigned *)a > *(unsigned *)b ) {
//		return 1;
//	}
            if (a < b) {
                return -1;
            }
            if (a > b) {
                return 1;
            }

            return 0;
        }
    }

    /*
     =====================
     GenerateSilEdges

     Output tris must be tjunction fixed and vertex uniqued
     A edge that is not exactly matched is a silhouette edge
     We could skip this and rely completely on the matched quad removal
     for all sil edges, but this will avoid the bulk of the checks.
     =====================
     */
    static void GenerateSilEdges() {
        int i, j;

//	unsigned	*edges = (unsigned *)_alloca( (numOutputTris*3+1)*sizeof(*edges) );
        long[] edges = new long[numOutputTris * 3 + 1];
        int numEdges = 0;

        numSilEdges = 0;

        for (i = 0; i < numOutputTris; i++) {
            int a = outputTris[i].index[0];
            int b = outputTris[i].index[1];
            int c = outputTris[i].index[2];
            if (a == b || a == c || b == c) {
                continue;		// degenerate
            }

            for (j = 0; j < 3; j++) {
                int v1, v2;

                v1 = outputTris[i].index[j];
                v2 = outputTris[i].index[(j + 1) % 3];
                if (v1 == v2) {
                    continue;		// degenerate
                }
                if (v1 > v2) {
                    edges[numEdges] = (v1 << 16) | (v2 << 1);
                } else {
                    edges[numEdges] = (v2 << 16) | (v1 << 1) | 1;
                }
                numEdges++;
            }
        }

//        qsort(edges, numEdges, sizeof(edges[0]), EdgeSort);
        Arrays.sort(edges, 0, numEdges);//, new EdgeSort());//TODO:check whether the default sort is enough
        edges[numEdges] = -1;	// force the last to make an edge if no matched to previous

        for (i = 0; i < numEdges; i++) {
            if ((edges[i] ^ edges[i + 1]) == 1) {
                // skip the next one, because we matched and
                // removed both
                i++;
                continue;
            }
            // this is an unmatched edge, so we need to generate a sil plane
            int v1, v2;
            if ((edges[i] & 1) != 0) {
                v2 = (int) (edges[i] >> 16);
                v1 = (int) ((edges[i] >> 1) & 0x7fff);
            } else {
                v1 = (int) (edges[i] >> 16);
                v2 = (int) ((edges[i] >> 1) & 0x7fff);
            }

            if (numSilEdges == MAX_SIL_EDGES) {
                common.Error("numSilEdges == MAX_SIL_EDGES");
            }
            silEdges[numSilEdges].index[0] = v1;
            silEdges[numSilEdges].index[1] = v2;
            numSilEdges++;
        }
    }

//==================================================================================

    /*
     =====================
     GenerateSilPlanes

     Groups the silEdges into common planes
     =====================
     */
    static void GenerateSilPlanes() {
        numSilPlanes = 0;
        silPlanes = new silPlane_t[numSilEdges];// Mem_Alloc(numSilEdges);

        // identify the silPlanes
        numSilPlanes = 0;
        for (int i = 0; i < numSilEdges; i++) {
            if (silEdges[i].index[0] == silEdges[i].index[1]) {
                continue;	// degenerate
            }

            idVec3 v1 = uniqued[silEdges[i].index[0]];
            idVec3 v2 = uniqued[silEdges[i].index[1]];

            // search for an existing plane
            int j;
            for (j = 0; j < numSilPlanes; j++) {
                float d = v1.oMultiply(silPlanes[j].normal);
                float d2 = v2.oMultiply(silPlanes[j].normal);

                if (Math.abs(d) < EDGE_PLANE_EPSILON
                        && Math.abs(d2) < EDGE_PLANE_EPSILON) {
                    silEdges[i].nextEdge = silPlanes[j].edges;
                    silPlanes[j].edges = silEdges[i];
                    break;
                }
            }

            if (j == numSilPlanes) {
                // create a new silPlane
                silPlanes[j].normal.Cross(v2, v1);
                silPlanes[j].normal.Normalize();
                silEdges[i].nextEdge = null;
                silPlanes[j].edges = silEdges[i];
                silPlanes[j].fragmentedQuads = null;
                numSilPlanes++;
            }
        }
    }

//==================================================================================

    /*
     =============
     SaveQuad
     =============
     */
    static void SaveQuad(silPlane_t silPlane, silQuad_s quad) {
        // this fragment is a final fragment
        if (numSilQuads == MAX_SIL_QUADS) {
            common.Error("numSilQuads == MAX_SIL_QUADS");
        }
        silQuads[numSilQuads] = quad;
        silQuads[numSilQuads].nextQuad = silPlane.fragmentedQuads;
        silPlane.fragmentedQuads = silQuads[numSilQuads];
        numSilQuads++;
    }


    /*
     ===================
     FragmentSilQuad

     Clip quads, or reconstruct?
     Generate them T-junction free, or require another pass of fix-tjunc?
     Call optimizer on a per-sil-plane basis?
     will this ever introduce tjunctions with the front faces?
     removal of planes can allow the rear projection to be farther optimized

     For quad clipping
     PlaneThroughEdge

     quad clipping introduces new vertexes

     Cannot just fragment edges, must emit full indexes

     what is the bounds on max indexes?
     the worst case is that all edges but one carve an existing edge in the middle,
     giving twice the input number of indexes (I think)

     can we avoid knowing about projected positions and still optimize?

     Fragment all edges first
     Introduces T-junctions
     create additional silEdges, linked to silPlanes

     In theory, we should never have more than one edge clipping a given
     fragment, but it is more robust if we check them all
     ===================
     */
    static void FragmentSilQuad(silQuad_s quad, silPlane_t silPlane,
            shadowOptEdge_s startEdge, shadowOptEdge_s skipEdge) {
        if (quad.nearV[0] == quad.nearV[1]) {
            return;
        }

        for (shadowOptEdge_s check = startEdge; check != null; check = check.nextEdge) {
            if (check.equals(skipEdge)) {
                // don't clip against self
                continue;
            }

            if (check.index[0] == check.index[1]) {
                continue;
            }

            // make planes through both points of check
            for (int i = 0; i < 2; i++) {
                idVec3 plane = new idVec3();

                plane.Cross(uniqued[check.index[i]], silPlane.normal);
                plane.Normalize();

                if (plane.Length() < 0.9) {
                    continue;
                }

                // if the other point on check isn't on the negative side of the plane,
                // flip the plane
                if (uniqued[check.index[/*!i*/1 ^ i]].oMultiply(plane) > 0) {
                    plane = plane.oNegative();
                }

                float d1 = uniqued[quad.nearV[0]].oMultiply(plane);
                float d2 = uniqued[quad.nearV[1]].oMultiply(plane);

                float d3 = uniqued[quad.farV[0]].oMultiply(plane);
                float d4 = uniqued[quad.farV[1]].oMultiply(plane);

                // it is better to conservatively NOT split the quad, which, at worst,
                // will leave some extra overdraw
                // if the plane divides the incoming edge, split it and recurse
                // with the outside fraction before continuing with the inside fraction
                if ((d1 > EDGE_PLANE_EPSILON && d3 > EDGE_PLANE_EPSILON && d2 < -EDGE_PLANE_EPSILON && d4 < -EDGE_PLANE_EPSILON)
                        || (d2 > EDGE_PLANE_EPSILON && d4 > EDGE_PLANE_EPSILON && d1 < -EDGE_PLANE_EPSILON && d3 < -EDGE_PLANE_EPSILON)) {
                    float f = d1 / (d1 - d2);
                    float f2 = d3 / (d3 - d4);
                    f = f2;
                    if (f <= 0.0001 || f >= 0.9999) {
                        common.Error("Bad silQuad fraction");
                    }

                    // finding uniques may be causing problems here
                    idVec3 nearMid = uniqued[quad.nearV[0]].oMultiply(1 - f).oPlus(uniqued[quad.nearV[1]].oMultiply(f));
                    int nearMidIndex = FindUniqueVert(nearMid);
                    idVec3 farMid = uniqued[quad.farV[0]].oMultiply(1 - f).oPlus(uniqued[quad.farV[1]].oMultiply(f));
                    int farMidIndex = FindUniqueVert(farMid);

                    silQuad_s clipped = quad;

                    if (d1 > EDGE_PLANE_EPSILON) {
                        clipped.nearV[1] = nearMidIndex;
                        clipped.farV[1] = farMidIndex;
                        FragmentSilQuad(clipped, silPlane, check.nextEdge, skipEdge);
                        quad.nearV[0] = nearMidIndex;
                        quad.farV[0] = farMidIndex;
                    } else {
                        clipped.nearV[0] = nearMidIndex;
                        clipped.farV[0] = farMidIndex;
                        FragmentSilQuad(clipped, silPlane, check.nextEdge, skipEdge);
                        quad.nearV[1] = nearMidIndex;
                        quad.farV[1] = farMidIndex;
                    }
                }
            }

            // make a plane through the line of check
            idPlane separate = new idPlane();

            idVec3 dir = uniqued[check.index[1]].oMinus(uniqued[check.index[0]]);
            separate.Normal().Cross(dir, silPlane.normal);
            separate.Normal().Normalize();
            separate.ToVec4().oSet(3, -uniqued[check.index[1]].oMultiply(separate.Normal()));

            // this may miss a needed separation when the quad would be
            // clipped into a triangle and a quad
            float d1 = separate.Distance(uniqued[quad.nearV[0]]);
            float d2 = separate.Distance(uniqued[quad.farV[0]]);

            if ((d1 < EDGE_PLANE_EPSILON && d2 < EDGE_PLANE_EPSILON)
                    || (d1 > -EDGE_PLANE_EPSILON && d2 > -EDGE_PLANE_EPSILON)) {
                continue;
            }

            // split the quad at this plane
            float f = d1 / (d1 - d2);
            idVec3 mid0 = uniqued[quad.nearV[0]].oMultiply(1 - f).oPlus(uniqued[quad.farV[0]].oMultiply(f));
            int mid0Index = FindUniqueVert(mid0);

            d1 = separate.Distance(uniqued[quad.nearV[1]]);
            d2 = separate.Distance(uniqued[quad.farV[1]]);
            f = d1 / (d1 - d2);
            if (f < 0 || f > 1) {
                continue;
            }

            idVec3 mid1 = uniqued[quad.nearV[1]].oMultiply(1 - f).oPlus(uniqued[quad.farV[1]].oMultiply(f));
            int mid1Index = FindUniqueVert(mid1);

            silQuad_s clipped = quad;

            clipped.nearV[0] = mid0Index;
            clipped.nearV[1] = mid1Index;
            FragmentSilQuad(clipped, silPlane, check.nextEdge, skipEdge);
            quad.farV[0] = mid0Index;
            quad.farV[1] = mid1Index;
        }

        SaveQuad(silPlane, quad);
    }


    /*
     ===============
     FragmentSilQuads
     ===============
     */
    static void FragmentSilQuads() {
        // group the edges into common planes
        GenerateSilPlanes();

        numSilQuads = 0;

        // fragment overlapping edges
        for (int i = 0; i < numSilPlanes; i++) {
            silPlane_t sil = silPlanes[i];

            for (shadowOptEdge_s e1 = sil.edges; e1 != null; e1 = e1.nextEdge) {
                silQuad_s quad = new silQuad_s();

                quad.nearV[0] = e1.index[0];
                quad.nearV[1] = e1.index[1];
                if (e1.index[0] == e1.index[1]) {
                    common.Error("FragmentSilQuads: degenerate edge");
                }
                quad.farV[0] = e1.index[0] + numUniquedBeforeProjection;
                quad.farV[1] = e1.index[1] + numUniquedBeforeProjection;
                FragmentSilQuad(quad, sil, sil.edges, e1);
            }
        }
    }

//=======================================================================

    /*
     =====================
     EmitFragmentedSilQuads

     =====================
     */
    static void EmitFragmentedSilQuads() {
        int i, j, k;
        mapTri_s mtri;

        for (i = 0; i < numSilPlanes; i++) {
            silPlane_t sil = silPlanes[i];

            // prepare for optimizing the sil quads on each side of the sil plane
            optimizeGroup_s[] groups = new optimizeGroup_s[2];
//		memset( &groups, 0, sizeof( groups ) );
            idPlane[] planes = new idPlane[2];
            planes[0].SetNormal(sil.normal);//TODO:reinterpret cast
            planes[0].oSet(3, 0);
            planes[1] = planes[0].oNegative();
            groups[0].planeNum = FindFloatPlane(planes[0]);
            groups[1].planeNum = FindFloatPlane(planes[1]);

            // emit the quads that aren't matched
            for (silQuad_s f1 = sil.fragmentedQuads; f1 != null; f1 = f1.nextQuad) {
                silQuad_s f2;
                for (f2 = sil.fragmentedQuads; f2 != null; f2 = f2.nextQuad) {
                    if (f2.equals(f1)) {
                        continue;
                    }
                    // in theory, this is sufficient, but we might
                    // have some cases of tripple+ matching, or unclipped rear projections
                    if (f1.nearV[0] == f2.nearV[1] && f1.nearV[1] == f2.nearV[0]) {
                        break;
                    }
                }
                // if we went through all the quads without finding a match, emit the quad
                if (NOT(f2)) {
                    optimizeGroup_s gr;
                    idVec3 v1, v2, normal = new idVec3();

                    mtri = new mapTri_s();// Mem_ClearedAlloc(sizeof(mtri));
                    mtri.v[0].xyz = uniqued[f1.nearV[0]];
                    mtri.v[1].xyz = uniqued[f1.nearV[1]];
                    mtri.v[2].xyz = uniqued[f1.farV[1]];

                    v1 = mtri.v[1].xyz.oMinus(mtri.v[0].xyz);
                    v2 = mtri.v[2].xyz.oMinus(mtri.v[0].xyz);
                    normal.Cross(v2, v1);

                    if (normal.oMultiply(planes[0].Normal()) > 0) {
                        gr = groups[0];
                    } else {
                        gr = groups[1];
                    }

                    mtri.next = gr.triList;
                    gr.triList = mtri;

                    mtri = new mapTri_s();// Mem_ClearedAlloc(sizeof(mtri));
                    mtri.v[0].xyz = uniqued[f1.farV[0]];
                    mtri.v[1].xyz = uniqued[f1.nearV[0]];
                    mtri.v[2].xyz = uniqued[f1.farV[1]];

                    mtri.next = gr.triList;
                    gr.triList = mtri;

//#if 0
//				// emit a sil quad all the way to the projection plane
//				int index = ret.totalIndexes;
//				if ( index + 6 > maxRetIndexes ) {
//					common.Error( "maxRetIndexes exceeded" );
//				}
//				ret.indexes[index+0] = f1.nearV[0];
//				ret.indexes[index+1] = f1.nearV[1];
//				ret.indexes[index+2] = f1.farV[1];
//				ret.indexes[index+3] = f1.farV[0];
//				ret.indexes[index+4] = f1.nearV[0];
//				ret.indexes[index+5] = f1.farV[1];
//				ret.totalIndexes += 6;
//#endif
                }
            }

            // optimize
            for (j = 0; j < 2; j++) {
                if (NOT(groups[j].triList)) {
                    continue;
                }
                if (dmapGlobals.shadowOptLevel == SO_SIL_OPTIMIZE) {
                    OptimizeGroupList(groups[j]);
                }
                // add as indexes
                for (mtri = groups[j].triList; mtri != null; mtri = mtri.next) {
                    for (k = 0; k < 3; k++) {
                        if (ret.totalIndexes == maxRetIndexes) {
                            common.Error("maxRetIndexes exceeded");
                        }
                        ret.indexes[ret.totalIndexes] = FindUniqueVert(mtri.v[k].xyz);
                        ret.totalIndexes++;
                    }
                }
                FreeTriList(groups[j].triList);
            }
        }

        // we don't need the silPlane grouping anymore
        silPlanes = null;//Mem_Free(silPlanes);
    }

    /*
     =================
     EmitUnoptimizedSilEdges
     =================
     */
    static void EmitUnoptimizedSilEdges() {
        int i;

        for (i = 0; i < numSilEdges; i++) {
            int v1 = silEdges[i].index[0];
            int v2 = silEdges[i].index[1];
            int index = ret.totalIndexes;
            ret.indexes[index + 0] = v1;
            ret.indexes[index + 1] = v2;
            ret.indexes[index + 2] = v2 + numUniquedBeforeProjection;
            ret.indexes[index + 3] = v1 + numUniquedBeforeProjection;
            ret.indexes[index + 4] = v1;
            ret.indexes[index + 5] = v2 + numUniquedBeforeProjection;
            ret.totalIndexes += 6;
        }
    }

//==================================================================================

    /*
     ================
     FindUniqueVert
     ================
     */
    static int FindUniqueVert(idVec3 v) {
        int k;

        for (k = 0; k < numUniqued; k++) {
            idVec3 check = uniqued[k];
            if (Math.abs(v.oGet(0) - check.oGet(0)) < UNIQUE_EPSILON
                    && Math.abs(v.oGet(1) - check.oGet(1)) < UNIQUE_EPSILON
                    && Math.abs(v.oGet(2) - check.oGet(2)) < UNIQUE_EPSILON) {
                return k;
            }
        }
        if (numUniqued == maxUniqued) {
            common.Error("FindUniqueVert: numUniqued == maxUniqued");
        }
        uniqued[numUniqued] = v;
        numUniqued++;

        return k;
    }

    /*
     ===================
     UniqueVerts

     Snaps all triangle verts together, setting tri.index[]
     and generating numUniqued and uniqued.
     These are still in projection-centered space, not global space
     ===================
     */
    static void UniqueVerts() {
        int i, j;

        // we may add to uniqued later when splitting sil edges, so leave
        // some extra room
        maxUniqued = 100000; // numOutputTris * 10 + 1000;
        uniqued = new idVec3[maxUniqued];// Mem_Alloc(maxUniqued);
        numUniqued = 0;

        for (i = 0; i < numOutputTris; i++) {
            for (j = 0; j < 3; j++) {
                outputTris[i].index[j] = FindUniqueVert(outputTris[i].v[j]);
            }
        }
    }

    /*
     ======================
     ProjectUniqued
     ======================
     */
    static void ProjectUniqued(idVec3 projectionOrigin, idPlane projectionPlane) {
        // calculate the projection 
        idVec4[] mat = new idVec4[4];

        R_LightProjectionMatrix(projectionOrigin, projectionPlane, mat);

        if (numUniqued * 2 > maxUniqued) {
            common.Error("ProjectUniqued: numUniqued * 2 > maxUniqued");
        }

        // this is goofy going back and forth between the spaces,
        // but I don't want to change R_LightProjectionMatrix righ tnow...
        for (int i = 0; i < numUniqued; i++) {
            // put the vert back in global space, instead of light centered space
            idVec3 in = uniqued[i].oPlus(projectionOrigin);

            // project to far plane
            float w, oow;
            idVec3 out = new idVec3();

            w = in.oMultiply(mat[3].ToVec3()) + mat[3].oGet(3);

            oow = 1.0f / w;
            out.x = (in.oMultiply(mat[0].ToVec3()) + mat[0].oGet(3)) * oow;
            out.y = (in.oMultiply(mat[1].ToVec3()) + mat[1].oGet(3)) * oow;
            out.z = (in.oMultiply(mat[2].ToVec3()) + mat[2].oGet(3)) * oow;

            uniqued[numUniqued + i] = out.oMinus(projectionOrigin);
        }
        numUniqued *= 2;
    }

    /*
     ====================
     SuperOptimizeOccluders

     This is the callback from the renderer shadow generation routine, after
     verts have been culled against individual frustums of point lights

     ====================
     */
    public static optimizedShadow_t SuperOptimizeOccluders(idVec4[] verts, final int/*glIndex_t*/[] indexes, int numIndexes, idPlane projectionPlane, idVec3 projectionOrigin) {
//	memset( &ret, 0, sizeof( ret ) );
        ret = new optimizedShadow_t();

        // generate outputTris, removing fragments that are occluded by closer fragments
        ClipOccluders(verts, indexes, numIndexes, projectionOrigin);

        if (etoi(dmapGlobals.shadowOptLevel) >= etoi(SO_CULL_OCCLUDED)) {
            OptimizeOutputTris();
        }

        // match up common verts
        UniqueVerts();

        // now that we have uniqued the vertexes, we can find unmatched
        // edges, which are silhouette planes
        GenerateSilEdges();

        // generate the projected verts
        numUniquedBeforeProjection = numUniqued;
        ProjectUniqued(projectionOrigin, projectionPlane);

        // fragment the sil edges where the overlap,
        // possibly generating some additional unique verts
        if (etoi(dmapGlobals.shadowOptLevel) >= etoi(SO_CLIP_SILS)) {
            FragmentSilQuads();
        }

        // indexes for face and projection caps
        ret.numFrontCapIndexes = numOutputTris * 3;
        ret.numRearCapIndexes = numOutputTris * 3;
        if (etoi(dmapGlobals.shadowOptLevel) >= etoi(SO_CLIP_SILS)) {
            ret.numSilPlaneIndexes = numSilQuads * 12;	// this is the worst case with clipping
        } else {
            ret.numSilPlaneIndexes = numSilEdges * 6;	// this is the worst case with clipping
        }

        ret.totalIndexes = 0;

        maxRetIndexes = ret.numFrontCapIndexes + ret.numRearCapIndexes + ret.numSilPlaneIndexes;

        ret.indexes = new int/*glIndex_t*/[maxRetIndexes];// Mem_Alloc(maxRetIndexes);
        for (int i = 0; i < numOutputTris; i++) {
            // flip the indexes so the surface triangle faces outside the shadow volume
            ret.indexes[i * 3 + 0] = outputTris[i].index[2];
            ret.indexes[i * 3 + 1] = outputTris[i].index[1];
            ret.indexes[i * 3 + 2] = outputTris[i].index[0];

            ret.indexes[(numOutputTris + i) * 3 + 0] = numUniquedBeforeProjection + outputTris[i].index[0];
            ret.indexes[(numOutputTris + i) * 3 + 1] = numUniquedBeforeProjection + outputTris[i].index[1];
            ret.indexes[(numOutputTris + i) * 3 + 2] = numUniquedBeforeProjection + outputTris[i].index[2];
        }
        // emit the sil planes
        ret.totalIndexes = ret.numFrontCapIndexes + ret.numRearCapIndexes;

        if (etoi(dmapGlobals.shadowOptLevel) >= etoi(SO_CLIP_SILS)) {
            // re-optimize the sil planes, cutting 
            EmitFragmentedSilQuads();
        } else {
            // indexes for silhouette edges
            EmitUnoptimizedSilEdges();
        }

        // we have all the verts now
        // create twice the uniqued verts
        ret.numVerts = numUniqued;
        ret.verts = new idVec3[ret.numVerts];// Mem_Alloc(ret.numVerts);
        for (int i = 0; i < numUniqued; i++) {
            // put the vert back in global space, instead of light centered space
            ret.verts[i] = uniqued[i].oPlus(projectionOrigin);
        }

        // set the final index count
        ret.numSilPlaneIndexes = ret.totalIndexes - (ret.numFrontCapIndexes + ret.numRearCapIndexes);

        // free out local data
        uniqued = null;//Mem_Free(uniqued);

        return ret;
    }

    /*
     =================
     RemoveDegenerateTriangles
     =================
     */
    static void RemoveDegenerateTriangles(srfTriangles_s tri) {
        int c_removed;
        int i;
        int a, b, c;

        // check for completely degenerate triangles
        c_removed = 0;
        for (i = 0; i < tri.numIndexes; i += 3) {
            a = tri.indexes[i];
            b = tri.indexes[i + 1];
            c = tri.indexes[i + 2];
            if (a == b || a == c || b == c) {
                c_removed++;
//			memmove( tri.indexes + i, tri.indexes + i + 3, ( tri.numIndexes - i - 3 ) * sizeof( tri.indexes[0] ) );
                System.arraycopy(tri.indexes, i + 3, tri.indexes, i, tri.numIndexes - i - 3);
                tri.numIndexes -= 3;
                if (i < tri.numShadowIndexesNoCaps) {
                    tri.numShadowIndexesNoCaps -= 3;
                }
                if (i < tri.numShadowIndexesNoFrontCaps) {
                    tri.numShadowIndexesNoFrontCaps -= 3;
                }
                i -= 3;
            }
        }

        // this doesn't free the memory used by the unused verts
        if (c_removed != 0) {
            common.Printf("removed %d degenerate triangles from shadow\n", c_removed);
        }
    }

    /*
     ====================
     CleanupOptimizedShadowTris

     Uniques all verts across the frustums
     removes matched sil quads at frustum seams
     removes degenerate tris
     ====================
     */
    public static void CleanupOptimizedShadowTris(srfTriangles_s tri) {
        int i;

        // unique all the verts
        maxUniqued = tri.numVerts;
        uniqued = new idVec3[maxUniqued];
        numUniqued = 0;

        int/*glIndex_t*/[] remap = new int[tri.numVerts];

        for (i = 0; i < tri.numIndexes; i++) {
            if (tri.indexes[i] > tri.numVerts || tri.indexes[i] < 0) {
                common.Error("CleanupOptimizedShadowTris: index out of range");
            }
        }

        for (i = 0; i < tri.numVerts; i++) {
            remap[i] = FindUniqueVert(tri.shadowVertexes[i].xyz.ToVec3());
        }
        tri.numVerts = numUniqued;
        for (i = 0; i < tri.numVerts; i++) {
            tri.shadowVertexes[i].xyz.ToVec3().oSet(uniqued[i]);
            tri.shadowVertexes[i].xyz.oSet(3, 1);
        }

        for (i = 0; i < tri.numIndexes; i++) {
            tri.indexes[i] = remap[tri.indexes[i]];
        }

        // remove matched quads
        int numSilIndexes = tri.numShadowIndexesNoCaps;
        for (int i2 = 0; i2 < numSilIndexes; i2 += 6) {
            int j;
            for (j = i2 + 6; j < numSilIndexes; j += 6) {
                // if there is a reversed quad match, we can throw both of them out
                // this is not a robust check, it relies on the exact ordering of
                // quad indexes
                if (tri.indexes[i2 + 0] == tri.indexes[j + 1]
                        && tri.indexes[i2 + 1] == tri.indexes[j + 0]
                        && tri.indexes[i2 + 2] == tri.indexes[j + 3]
                        && tri.indexes[i2 + 3] == tri.indexes[j + 5]
                        && tri.indexes[i2 + 4] == tri.indexes[j + 1]
                        && tri.indexes[i2 + 5] == tri.indexes[j + 3]) {
                    break;
                }
            }
            if (j == numSilIndexes) {
                continue;
            }
            int k;
            // remove first quad
            for (k = i2 + 6; k < j; k++) {
                tri.indexes[k - 6] = tri.indexes[k];
            }
            // remove second quad
            for (k = j + 6; k < tri.numIndexes; k++) {
                tri.indexes[k - 12] = tri.indexes[k];
            }
            numSilIndexes -= 12;
            i2 -= 6;
        }

        int removed = tri.numShadowIndexesNoCaps - numSilIndexes;

        tri.numIndexes -= removed;
        tri.numShadowIndexesNoCaps -= removed;
        tri.numShadowIndexesNoFrontCaps -= removed;

        // remove degenerates after we have removed quads, so the double
        // triangle pairing isn't disturbed
        RemoveDegenerateTriangles(tri);
    }

    /*
     ========================
     CreateLightShadow

     This is called from dmap in util/surface.cpp
     shadowerGroups should be exactly clipped to the light frustum before calling.
     shadowerGroups is optimized by this function, but the contents can be freed, because the returned
     lightShadow_t list is a further culling and optimization of the data.
     ========================
     */
    static srfTriangles_s CreateLightShadow(optimizeGroup_s shadowerGroups, final mapLight_t light) {;

        common.Printf("----- CreateLightShadow %p -----\n", light);

        // optimize all the groups
        OptimizeGroupList(shadowerGroups);

        // combine all the triangles into one list
        mapTri_s combined;

        combined = null;
        for (optimizeGroup_s group = shadowerGroups; group != null; group = group.nextGroup) {
            combined = MergeTriLists(combined, CopyTriList(group.triList));
        }

        if (NOT(combined)) {
            return null;
        }

        // find uniqued vertexes
        srfTriangles_s occluders = ShareMapTriVerts(combined);

        FreeTriList(combined);

        // find silhouette information for the triSurf
        R_CleanupTriangles(occluders, false, true, false);

        // let the renderer build the shadow volume normally
        idRenderEntityLocal space = new idRenderEntityLocal();

        space.modelMatrix[0] = 1;
        space.modelMatrix[5] = 1;
        space.modelMatrix[10] = 1;
        space.modelMatrix[15] = 1;

        srfCullInfo_t cullInfo = new srfCullInfo_t();
//	memset( &cullInfo, 0, sizeof( cullInfo ) );

        // call the normal shadow creation, but with the superOptimize flag set, which will
        // call back to SuperOptimizeOccluders after clipping the triangles to each frustum
        srfTriangles_s shadowTris;
        if (dmapGlobals.shadowOptLevel == SO_MERGE_SURFACES) {
            shadowTris = R_CreateShadowVolume(space, occluders, light.def, SG_STATIC, cullInfo);
        } else {
            shadowTris = R_CreateShadowVolume(space, occluders, light.def, SG_OFFLINE, cullInfo);
        }
        R_FreeStaticTriSurf(occluders);

        R_FreeInteractionCullInfo(cullInfo);

        if (shadowTris != null) {
            dmapGlobals.totalShadowTriangles += shadowTris.numIndexes / 3;
            dmapGlobals.totalShadowVerts += shadowTris.numVerts / 3;
        }

        return shadowTris;
    }
}
