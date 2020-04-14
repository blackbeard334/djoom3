package neo.Renderer;

import static neo.Renderer.RenderSystem_init.r_skipDeforms;
import static neo.Renderer.RenderWorld.SHADERPARM_DIVERSITY;
import static neo.Renderer.RenderWorld.SHADERPARM_PARTICLE_STOPTIME;
import static neo.Renderer.RenderWorld.SHADERPARM_TIMEOFFSET;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.tr_light.R_AddDrawSurf;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_main.R_GlobalVectorToLocal;
import static neo.Renderer.tr_trisurf.R_DeriveTangents;
import static neo.framework.Common.common;
import static neo.idlib.containers.BinSearch.idBinSearch_LessEqual;
import static neo.idlib.math.Matrix.idMat3.getMat3_identity;
import static neo.idlib.math.Vector.getVec3_origin;

import java.util.Arrays;
import java.util.stream.Stream;

import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.viewDef_s;
import neo.framework.DeclParticle.idDeclParticle;
import neo.framework.DeclParticle.idParticleStage;
import neo.framework.DeclParticle.particleGen_t;
import neo.framework.DeclTable.idDeclTable;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec3;
import neo.open.ColorUtil;

/**
 *
 */
public class tr_deform {

    /*
     =================
     R_FinishDeform

     The ambientCache is on the stack, so we don't want to leave a reference
     to it that would try to be freed later.  Create the ambientCache immediately.
     =================
     */
    public static void R_FinishDeform(drawSurf_s drawSurf, srfTriangles_s newTri, idDrawVert[] ac) {
        if (null == newTri) {
            return;
        }

        // generate current normals, tangents, and bitangents
        // We might want to support the possibility of deform functions generating
        // explicit normals, and we might also want to allow the cached deformInfo
        // optimization for these.
        // FIXME: this doesn't work, because the deformed surface is just the
        // ambient one, and there isn't an opportunity to generate light interactions
        if (drawSurf.material.ReceivesLighting()) {
            newTri.verts = ac;
            R_DeriveTangents(newTri, false);
            newTri.verts = null;
        }

        newTri.ambientCache = vertexCache.AllocFrameTemp(ac, newTri.numVerts * idDrawVert.BYTES);
        // if we are out of vertex cache, leave it the way it is
        if (newTri.ambientCache != null) {
            drawSurf.geo = newTri;
        }
    }

    /*
     =====================
     R_AutospriteDeform

     Assuming all the triangles for this shader are independant
     quads, rebuild them as forward facing sprites
     =====================
     */
    public static void R_AutospriteDeform(drawSurf_s surf) {
        int i;
        idDrawVert v;
        final idVec3 mid = new idVec3();
		idVec3 delta;
        float radius;
        idVec3 left, up;
        idVec3 leftDir = new idVec3();
		final idVec3 upDir = new idVec3();
        final srfTriangles_s tri;
        srfTriangles_s newTri;

        tri = surf.geo;

        if ((tri.numVerts & 3) != 0) {
            common.Warning("R_AutospriteDeform: shader had odd vertex count");
            return;
        }
        if (tri.getNumIndexes() != ((tri.numVerts >> 2) * 6)) {
            common.Warning("R_AutospriteDeform: autosprite had odd index count");
            return;
        }

        R_GlobalVectorToLocal(surf.space.modelMatrix, tr.viewDef.renderView.viewaxis.oGet(1), leftDir);
        R_GlobalVectorToLocal(surf.space.modelMatrix, tr.viewDef.renderView.viewaxis.oGet(2), upDir);

        if (tr.viewDef.isMirror) {
            leftDir = getVec3_origin().oMinus(leftDir);
        }

        // this srfTriangles_t and all its indexes and caches are in frame
        // memory, and will be automatically disposed of
        newTri = new srfTriangles_s();// R_ClearedFrameAlloc(sizeof(newTri));
        newTri.numVerts = tri.numVerts;
        newTri.setNumIndexes(tri.getNumIndexes());
        newTri.setIndexes(new int[newTri.getNumIndexes()]);// R_FrameAlloc(newTri.numIndexes);

        final idDrawVert[] ac = Stream.generate(idDrawVert::new).limit(newTri.numVerts).toArray(idDrawVert[]::new);

        for (i = 0; i < tri.numVerts; i += 4) {
            // find the midpoint
            v = tri.verts[i];
            final idDrawVert v1 = tri.verts[i + 1];
            final idDrawVert v2 = tri.verts[i + 2];
            final idDrawVert v3 = tri.verts[i + 3];

            mid.oSet(0, 0.25f * (v.xyz.oGet(0) + v1.xyz.oGet(0) + v2.xyz.oGet(0) + v3.xyz.oGet(0)));
            mid.oSet(1, 0.25f * (v.xyz.oGet(1) + v1.xyz.oGet(1) + v2.xyz.oGet(1) + v3.xyz.oGet(1)));
            mid.oSet(2, 0.25f * (v.xyz.oGet(2) + v1.xyz.oGet(2) + v2.xyz.oGet(2) + v3.xyz.oGet(2)));

            delta = v.xyz.oMinus(mid);
            radius = delta.Length() * 0.707f;		// / sqrt(2)

            left = leftDir.oMultiply(radius);
            up = upDir.oMultiply(radius);

            ac[i + 0].xyz = mid.oPlus(left.oPlus(up));
            ac[i + 0].st.oSet(0, 0);
            ac[i + 0].st.oSet(1, 0);
            ac[i + 1].xyz = mid.oMinus(left.oPlus(up));
            ac[i + 1].st.oSet(0, 1);
            ac[i + 1].st.oSet(1, 0);
            ac[i + 2].xyz = mid.oMinus(left.oMinus(up));
            ac[i + 2].st.oSet(0, 1);
            ac[i + 2].st.oSet(1, 1);
            ac[i + 3].xyz = mid.oPlus(left.oMinus(up));
            ac[i + 3].st.oSet(0, 0);
            ac[i + 3].st.oSet(1, 1);

            newTri.getIndexes()[(6 * (i >> 2)) + 0] = i;
            newTri.getIndexes()[(6 * (i >> 2)) + 1] = i + 1;
            newTri.getIndexes()[(6 * (i >> 2)) + 2] = i + 2;

            newTri.getIndexes()[(6 * (i >> 2)) + 3] = i;
            newTri.getIndexes()[(6 * (i >> 2)) + 4] = i + 2;
            newTri.getIndexes()[(6 * (i >> 2)) + 5] = i + 3;
        }

        R_FinishDeform(surf, newTri, ac);
    }

    /*
     =====================
     R_TubeDeform

     will pivot a rectangular quad along the center of its long axis

     Note that a geometric tube with even quite a few sides tube will almost certainly render much faster
     than this, so this should only be for faked volumetric tubes.
     Make sure this is used with twosided translucent shaders, because the exact side
     order may not be correct.
     =====================
     */
    private final static int[][] edgeVerts/*[6][2]*/ = {
                {0, 1},
                {1, 2},
                {2, 0},
                {3, 4},
                {4, 5},
                {5, 3}
            };

    public static void R_TubeDeform(drawSurf_s surf) {
        int i, j;
        int indexes;
        final srfTriangles_s tri;

        tri = surf.geo;

        if ((tri.numVerts & 3) != 0) {
            common.Error("R_AutospriteDeform: shader had odd vertex count");
        }
        if (tri.getNumIndexes() != ((tri.numVerts >> 2) * 6)) {
            common.Error("R_AutospriteDeform: autosprite had odd index count");
        }

        // we need the view direction to project the minor axis of the tube
        // as the view changes
        final idVec3 localView = new idVec3();
        R_GlobalPointToLocal(surf.space.modelMatrix, tr.viewDef.renderView.vieworg, localView);

        // this srfTriangles_t and all its indexes and caches are in frame
        // memory, and will be automatically disposed of
        final srfTriangles_s newTri = new srfTriangles_s();// R_ClearedFrameAlloc(sizeof(newTri));
        newTri.numVerts = tri.numVerts;
        newTri.setNumIndexes(tri.getNumIndexes());
        newTri.setIndexes(new int[newTri.getNumIndexes()]);// R_FrameAlloc(newTri.numIndexes);
        System.arraycopy(tri.getIndexes(), 0, newTri.getIndexes(), 0, newTri.getNumIndexes());//memcpy( newTri.indexes, tri.indexes, newTri.numIndexes * sizeof( newTri.indexes[0] ) );

        final idDrawVert[] ac = Stream.generate(idDrawVert::new).limit(newTri.numVerts).toArray(idDrawVert[]::new);//memset( ac, 0, sizeof( idDrawVert ) * newTri.numVerts );

        // this is a lot of work for two triangles...
        // we could precalculate a lot if it is an issue, but it would mess up
        // the shader abstraction
        for (i = 0, indexes = 0; i < tri.numVerts; i += 4, indexes += 6) {
            final float[] lengths = new float[2];
            final int[] nums = new int[2];
            final idVec3[] mid = new idVec3[2];
            idVec3 major;
			final idVec3 minor = new idVec3();
            idDrawVert v1, v2;

            // identify the two shortest edges out of the six defined by the indexes
            nums[0] = nums[1] = 0;
            lengths[0] = lengths[1] = 999999;

            for (j = 0; j < 6; j++) {
                float l;

                v1 = tri.verts[tri.getIndexes()[i + edgeVerts[j][0]]];
                v2 = tri.verts[tri.getIndexes()[i + edgeVerts[j][1]]];

                l = (v1.xyz.oMinus(v2.xyz)).Length();
                if (l < lengths[0]) {
                    nums[1] = nums[0];
                    lengths[1] = lengths[0];
                    nums[0] = j;
                    lengths[0] = l;
                } else if (l < lengths[1]) {
                    nums[1] = j;
                    lengths[1] = l;
                }
            }

            // find the midpoints of the two short edges, which
            // will give us the major axis in object coordinates
            for (j = 0; j < 2; j++) {
                v1 = tri.verts[tri.getIndexes()[i + edgeVerts[nums[j]][0]]];
                v2 = tri.verts[tri.getIndexes()[i + edgeVerts[nums[j]][1]]];

                mid[j] = new idVec3(
                        0.5f * (v1.xyz.oGet(0) + v2.xyz.oGet(0)),
                        0.5f * (v1.xyz.oGet(1) + v2.xyz.oGet(1)),
                        0.5f * (v1.xyz.oGet(2) + v2.xyz.oGet(2)));
            }

            // find the vector of the major axis
            major = mid[1].oMinus(mid[0]);

            // re-project the points
            for (j = 0; j < 2; j++) {
                float l;
                final int i1 = tri.getIndexes()[i + edgeVerts[nums[j]][0]];
                final int i2 = tri.getIndexes()[i + edgeVerts[nums[j]][1]];

                final idDrawVert av1 = ac[i1] = tri.verts[i1];
                final idDrawVert av2 = ac[i2] = tri.verts[i2];
//                av1 = tri.verts[i1];
//                av2 = tri.verts[i2];

                l = 0.5f * lengths[j];

                // cross this with the view direction to get minor axis
                final idVec3 dir = mid[j].oMinus(localView);
                minor.Cross(major, dir);
                minor.Normalize();

                if (j != 0) {
                    av1.xyz = mid[j].oMinus(minor.oMultiply(l));
                    av2.xyz = mid[j].oPlus(minor.oMultiply(l));
                } else {
                    av1.xyz = mid[j].oPlus(minor.oMultiply(l));
                    av2.xyz = mid[j].oMinus(minor.oMultiply(l));
                }
            }
        }

        R_FinishDeform(surf, newTri, ac);
    }

    /*
     =====================
     R_WindingFromTriangles

     =====================
     */
    static final int MAX_TRI_WINDING_INDEXES = 16;

    public static int R_WindingFromTriangles(final srfTriangles_s tri, int[]/*glIndex_t*/ indexes/*[MAX_TRI_WINDING_INDEXES]*/) {
        int i, j, k, l;

        indexes[0] = tri.getIndexes()[0];
        int numIndexes = 1;
        final int numTris = tri.getNumIndexes() / 3;

        do {
            // find an edge that goes from the current index to another
            // index that isn't already used, and isn't an internal edge
            for (i = 0; i < numTris; i++) {
                for (j = 0; j < 3; j++) {
                    if (tri.getIndexes()[(i * 3) + j] != indexes[numIndexes - 1]) {
                        continue;
                    }
                    final int next = tri.getIndexes()[(i * 3) + ((j + 1) % 3)];

                    // make sure it isn't already used
                    if (numIndexes == 1) {
                        if (next == indexes[0]) {
                            continue;
                        }
                    } else {
                        for (k = 1; k < numIndexes; k++) {
                            if (indexes[k] == next) {
                                break;
                            }
                        }
                        if (k != numIndexes) {
                            continue;
                        }
                    }

                    // make sure it isn't an interior edge
                    for (k = 0; k < numTris; k++) {
                        if (k == i) {
                            continue;
                        }
                        for (l = 0; l < 3; l++) {
                            int a, b;

                            a = tri.getIndexes()[(k * 3) + l];
                            if (a != next) {
                                continue;
                            }
                            b = tri.getIndexes()[(k * 3) + ((l + 1) % 3)];
                            if (b != indexes[numIndexes - 1]) {
                                continue;
                            }

                            // this is an interior edge
                            break;
                        }
                        if (l != 3) {
                            break;
                        }
                    }
                    if (k != numTris) {
                        continue;
                    }

                    // add this to the list
                    indexes[numIndexes] = next;
                    numIndexes++;
                    break;
                }
                if (j != 3) {
                    break;
                }
            }
            if (numIndexes == tri.numVerts) {
                break;
            }
        } while (i != numTris);

        return numIndexes;
    }

    /*
     =====================
     R_FlareDeform

     =====================
     */
    /*
     static void R_FlareDeform( drawSurf_t *surf ) {
     const srfTriangles_t *tri;
     srfTriangles_t		*newTri;
     idPlane	plane;
     float	dot;
     idVec3	localViewer;
     int		j;

     tri = surf.geo;

     if ( tri.numVerts != 4 || tri.numIndexes != 6 ) {
     //FIXME: temp hack for flares on tripleted models
     common.Warning( "R_FlareDeform: not a single quad" );
     return;
     }

     // this srfTriangles_t and all its indexes and caches are in frame
     // memory, and will be automatically disposed of
     newTri = (srfTriangles_t *)R_ClearedFrameAlloc( sizeof( *newTri ) );
     newTri.numVerts = 4;
     newTri.numIndexes = 2*3;
     newTri.indexes = (glIndex_t *)R_FrameAlloc( newTri.numIndexes * sizeof( newTri.indexes[0] ) );
	
     idDrawVert *ac = (idDrawVert *)_alloca16( newTri.numVerts * sizeof( idDrawVert ) );

     // find the plane
     plane.FromPoints( tri.verts[tri.indexes[0]].xyz, tri.verts[tri.indexes[1]].xyz, tri.verts[tri.indexes[2]].xyz );

     // if viewer is behind the plane, draw nothing
     R_GlobalPointToLocal( surf.space.modelMatrix, tr.viewDef.renderView.vieworg, localViewer );
     float distFromPlane = localViewer * plane.Normal() + plane[3];
     if ( distFromPlane <= 0 ) {
     newTri.numIndexes = 0;
     surf.geo = newTri;
     return;
     }

     idVec3	center;
     center = tri.verts[0].xyz;
     for ( j = 1 ; j < tri.numVerts ; j++ ) {
     center += tri.verts[j].xyz;
     }
     center *= 1.0/tri.numVerts;

     idVec3	dir = localViewer - center;
     dir.Normalize();

     dot = dir * plane.Normal();

     // set vertex colors based on plane angle
     int	color = (int)(dot * 8 * 256);
     if ( color > 255 ) {
     color = 255;
     }
     for ( j = 0 ; j < newTri.numVerts ; j++ ) {
     ac[j].color[0] =
     ac[j].color[1] =
     ac[j].color[2] = color;
     ac[j].color[3] = 255;
     }

     float	spread = surf.shaderRegisters[ surf.material.GetDeformRegister(0) ] * r_flareSize.GetFloat();
     idVec3	edgeDir[4][3];
     glIndex_t		indexes[MAX_TRI_WINDING_INDEXES];
     int		numIndexes = R_WindingFromTriangles( tri, indexes );

     surf.material = declManager.FindMaterial( "textures/smf/anamorphicFlare" );

     // only deal with quads
     if ( numIndexes != 4 ) {
     return;
     }

     // compute centroid
     idVec3 centroid, toeye, forward, up, left;
     centroid.Set( 0, 0, 0 );
     for ( int i = 0; i < 4; i++ ) {
     centroid += tri.verts[ indexes[i] ].xyz;
     }
     centroid /= 4;

     // compute basis vectors
     up.Set( 0, 0, 1 );

     toeye = centroid - localViewer;
     toeye.Normalize();
     left = toeye.Cross( up );
     up = left.Cross( toeye );

     left = left * 40 * 6;
     up = up * 40;

     // compute flares
     struct flare_t {
     float	angle;
     float	length;
     };

     static flare_t flares[] = {
     { 0, 100 },
     { 90, 100 }
     };

     for ( int i = 0; i < 4; i++ ) {
     memset( ac + i, 0, sizeof( ac[i] ) );
     }

     ac[0].xyz = centroid - left;
     ac[0].st[0] = 0; ac[0].st[1] = 0;

     ac[1].xyz = centroid + up;
     ac[1].st[0] = 1; ac[1].st[1] = 0;

     ac[2].xyz = centroid + left;
     ac[2].st[0] = 1; ac[2].st[1] = 1;

     ac[3].xyz = centroid - up;
     ac[3].st[0] = 0; ac[3].st[1] = 1;

     // setup colors
     for ( j = 0 ; j < newTri.numVerts ; j++ ) {
     ac[j].color[0] =
     ac[j].color[1] =
     ac[j].color[2] = 255;
     ac[j].color[3] = 255;
     }

     // setup indexes
     static glIndex_t	triIndexes[2*3] = {
     0,1,2,  0,2,3
     };

     memcpy( newTri.indexes, triIndexes, sizeof( triIndexes ) );

     R_FinishDeform( surf, newTri, ac );
     }
     */
    static final int[]/*glIndex_t	*/ triIndexes/*[18*3]*/ = {
                0, 4, 5,
                0, 5, 6,
                0, 6, 7,
                0, 7, 1,
                1, 7, 8,
                1, 8, 9,
                15, 4, 0,
                15, 0, 3,
                3, 0, 1,
                3, 1, 2,
                2, 1, 9,
                2, 9, 10,
                14, 15, 3,
                14, 3, 13,
                13, 3, 2,
                13, 2, 12,
                12, 2, 11,
                11, 2, 10
            };

    public static void R_FlareDeform(drawSurf_s surf) {
        final srfTriangles_s tri;
        srfTriangles_s newTri;
        final idPlane plane = new idPlane();
        float dot;
        final idVec3 localViewer = new idVec3();
        int j;

        tri = surf.geo;

        if ((tri.numVerts != 4) || (tri.getNumIndexes() != 6)) {
            //FIXME: temp hack for flares on tripleted models
            common.Warning("R_FlareDeform: not a single quad");
            return;
        }

        // this srfTriangles_t and all its indexes and caches are in frame
        // memory, and will be automatically disposed of
        newTri = new srfTriangles_s();// R_ClearedFrameAlloc(sizeof(newTri));
        newTri.numVerts = 16;
        newTri.setNumIndexes(18 * 3);
        newTri.setIndexes(new int[newTri.getNumIndexes()]);

        final idDrawVert[] ac = new idDrawVert[newTri.numVerts];

        // find the plane
        plane.FromPoints(tri.verts[tri.getIndexes()[0]].xyz, tri.verts[tri.getIndexes()[1]].xyz, tri.verts[tri.getIndexes()[2]].xyz);

        // if viewer is behind the plane, draw nothing
        R_GlobalPointToLocal(surf.space.modelMatrix, tr.viewDef.renderView.vieworg, localViewer);
        final float distFromPlane = localViewer.oMultiply(plane.Normal()) + plane.oGet(3);
        if (distFromPlane <= 0) {
            newTri.setNumIndexes(0);
            surf.geo = newTri;
            return;
        }

        idVec3 center;
        center = tri.verts[0].xyz;
        for (j = 1; j < tri.numVerts; j++) {
            center.oPluSet(tri.verts[j].xyz);
        }
        center.oMulSet(1.0f / tri.numVerts);

        idVec3 dir = localViewer.oMinus(center);
        dir.Normalize();

        dot = dir.oMultiply(plane.Normal());

        // set vertex colors based on plane angle
        int color = (int) (dot * 8 * 256);
        if (color > 255) {
            color = 255;
        }
        for (j = 0; j < newTri.numVerts; j++) {
            ac[j] = new idDrawVert();
            ColorUtil.setElements(ac[j].getColor(), (byte) 255);
        }

        final float spread = surf.shaderRegisters[ surf.material.GetDeformRegister(0)] * RenderSystem_init.r_flareSize.GetFloat();
        final idVec3[][] edgeDir = new idVec3[4][3];
        final int[]/*glIndex_t*/ indexes = new int[MAX_TRI_WINDING_INDEXES];
        final int numIndexes = R_WindingFromTriangles(tri, indexes);

        // only deal with quads
        if (numIndexes != 4) {
            return;
        }
        int i;
        // calculate vector directions
        for (i = 0; i < 4; i++) {
            ac[i].xyz = tri.verts[ indexes[i]].xyz;
            ac[i].st.oSet(0, ac[i].st.oSet(1, 0.5f));

            final idVec3 toEye = tri.verts[indexes[i]].xyz.oMinus(localViewer);
            toEye.Normalize();

            final idVec3 d1 = tri.verts[indexes[(i + 1) % 4]].xyz.oMinus(localViewer);
            d1.Normalize();
            edgeDir[i][1] = new idVec3();
            edgeDir[i][1].Cross(toEye, d1);
            edgeDir[i][1].Normalize();
            edgeDir[i][1] = getVec3_origin().oMinus(edgeDir[i][1]);

            final idVec3 d2 = tri.verts[indexes[(i + 3) % 4]].xyz.oMinus(localViewer);
            d2.Normalize();
            edgeDir[i][0] = new idVec3();
            edgeDir[i][0].Cross(toEye, d2);
            edgeDir[i][0].Normalize();

            edgeDir[i][2] = new idVec3();
            edgeDir[i][2] = edgeDir[i][0].oPlus(edgeDir[i][1]);
            edgeDir[i][2].Normalize();
        }

        // build all the points
        ac[ 4].xyz = tri.verts[indexes[0]].xyz.oPlus(edgeDir[0][0].oMultiply(spread));
        ac[ 4].st.oSet(0, 0);
        ac[ 4].st.oSet(1, 0.5f);

        ac[ 5].xyz = tri.verts[indexes[0]].xyz.oPlus(edgeDir[0][2].oMultiply(spread));
        ac[ 5].st.oSet(0, 0);
        ac[ 5].st.oSet(1, 0);

        ac[ 6].xyz = tri.verts[indexes[0]].xyz.oPlus(edgeDir[0][1].oMultiply(spread));
        ac[ 6].st.oSet(0, 0.5f);
        ac[ 6].st.oSet(1, 0);

        ac[ 7].xyz = tri.verts[indexes[1]].xyz.oPlus(edgeDir[1][0].oMultiply(spread));
        ac[ 7].st.oSet(0, 0.5f);
        ac[ 7].st.oSet(1, 0);

        ac[ 8].xyz = tri.verts[indexes[1]].xyz.oPlus(edgeDir[1][2].oMultiply(spread));
        ac[ 8].st.oSet(0, 1);
        ac[ 8].st.oSet(1, 0);

        ac[ 9].xyz = tri.verts[indexes[1]].xyz.oPlus(edgeDir[1][1].oMultiply(spread));
        ac[ 9].st.oSet(0, 1);
        ac[ 9].st.oSet(1, 0.5f);

        ac[10].xyz = tri.verts[indexes[2]].xyz.oPlus(edgeDir[2][0].oMultiply(spread));
        ac[10].st.oSet(0, 1);
        ac[10].st.oSet(1, 0.5f);

        ac[11].xyz = tri.verts[indexes[2]].xyz.oPlus(edgeDir[2][2].oMultiply(spread));
        ac[11].st.oSet(0, 1);
        ac[11].st.oSet(1, 1);

        ac[12].xyz = tri.verts[indexes[2]].xyz.oPlus(edgeDir[2][1].oMultiply(spread));
        ac[12].st.oSet(0, 0.5f);
        ac[12].st.oSet(1, 1);

        ac[13].xyz = tri.verts[indexes[3]].xyz.oPlus(edgeDir[3][0].oMultiply(spread));
        ac[13].st.oSet(0, 0.5f);
        ac[13].st.oSet(1, 1);

        ac[14].xyz = tri.verts[indexes[3]].xyz.oPlus(edgeDir[3][2].oMultiply(spread));
        ac[14].st.oSet(0, 0);
        ac[14].st.oSet(1, 1);

        ac[15].xyz = tri.verts[indexes[3]].xyz.oPlus(edgeDir[3][1].oMultiply(spread));
        ac[15].st.oSet(0, 0);
        ac[15].st.oSet(1, 0.5f);

        for (i = 4; i < 16; i++) {
            dir = ac[i].xyz.oMinus(localViewer);
            final float len = dir.Normalize();

            final float ang = dir.oMultiply(plane.Normal());

//		ac[i].xyz -= dir * spread * 2;
            final float newLen = -(distFromPlane / ang);

            if ((newLen > 0) && (newLen < len)) {
                ac[i].xyz = localViewer.oPlus(dir.oMultiply(newLen));
            }

            ac[i].st.oSet(0, 0);
            ac[i].st.oSet(1, 0.5f);
        }

//if (true){
//	static glIndex_t	triIndexes[18*3] = {
//		0,4,5,  0,5,6, 0,6,7, 0,7,1, 1,7,8, 1,8,9, 
//		15,4,0, 15,0,3, 3,0,1, 3,1,2, 2,1,9, 2,9,10,
//		14,15,3, 14,3,13, 13,3,2, 13,2,12, 12,2,11, 11,2,10
//	};
//}else{
//	newTri.numIndexes = 12;
//	static glIndex_t triIndexes[4*3] = {
//		0,1,2, 0,2,3, 0,4,5,0,5,6
//	};
//}
//        memcpy(newTri.indexes, triIndexes, sizeof(triIndexes));
        System.arraycopy(triIndexes, 0, newTri.getIndexes(), 0, triIndexes.length);

        R_FinishDeform(surf, newTri, ac);
    }

    /*
     =====================
     R_ExpandDeform

     Expands the surface along it's normals by a shader amount
     =====================
     */
    public static void R_ExpandDeform(drawSurf_s surf) {
        int i;
        final srfTriangles_s tri;
        srfTriangles_s newTri;

        tri = surf.geo;

        // this srfTriangles_t and all its indexes and caches are in frame
        // memory, and will be automatically disposed of
        newTri = new srfTriangles_s();// R_ClearedFrameAlloc(sizeof(newTri));
        newTri.numVerts = tri.numVerts;
        newTri.setNumIndexes(tri.getNumIndexes());
        newTri.setIndexes(tri.getIndexes());

        final idDrawVert[] ac = new idDrawVert[newTri.numVerts];

        final float dist = surf.shaderRegisters[ surf.material.GetDeformRegister(0)];
        for (i = 0; i < tri.numVerts; i++) {
            ac[i] = tri.verts[i];
            ac[i].xyz = tri.verts[i].xyz.oPlus(tri.verts[i].normal.oMultiply(dist));
        }

        R_FinishDeform(surf, newTri, ac);
    }

    /*
     =====================
     R_MoveDeform

     Moves the surface along the X axis, mostly just for demoing the deforms
     =====================
     */
    public static void R_MoveDeform(drawSurf_s surf) {
        int i;
        final srfTriangles_s tri;
        srfTriangles_s newTri;

        tri = surf.geo;

        // this srfTriangles_t and all its indexes and caches are in frame
        // memory, and will be automatically disposed of
        newTri = new srfTriangles_s();// R_ClearedFrameAlloc(sizeof(newTri));
        newTri.numVerts = tri.numVerts;
        newTri.setNumIndexes(tri.getNumIndexes());
        newTri.setIndexes(tri.getIndexes());

        final idDrawVert[] ac = new idDrawVert[newTri.numVerts];

        final float dist = surf.shaderRegisters[ surf.material.GetDeformRegister(0)];
        for (i = 0; i < tri.numVerts; i++) {
            ac[i] = tri.verts[i];
            ac[i].xyz.oPluSet(0, dist);
        }

        R_FinishDeform(surf, newTri, ac);
    }

//=====================================================================================

    /*
     =====================
     R_TurbulentDeform

     Turbulently deforms the XYZ, S, and T values
     =====================
     */
    public static void R_TurbulentDeform(drawSurf_s surf) {
        int i;
        final srfTriangles_s tri;
        srfTriangles_s newTri;

        tri = surf.geo;

        // this srfTriangles_t and all its indexes and caches are in frame
        // memory, and will be automatically disposed of
        newTri = new srfTriangles_s();// R_ClearedFrameAlloc(sizeof(newTri));
        newTri.numVerts = tri.numVerts;
        newTri.setNumIndexes(tri.getNumIndexes());
        newTri.setIndexes(tri.getIndexes());

        final idDrawVert[] ac = new idDrawVert[newTri.numVerts];

        final idDeclTable table = (idDeclTable) surf.material.GetDeformDecl();
        final float range = surf.shaderRegisters[ surf.material.GetDeformRegister(0)];
        final float timeOfs = surf.shaderRegisters[ surf.material.GetDeformRegister(1)];
        final float domain = surf.shaderRegisters[ surf.material.GetDeformRegister(2)];
        final float tOfs = 0.5f;

        for (i = 0; i < tri.numVerts; i++) {
            float f = (tri.verts[i].xyz.oGet(0) * 0.003f)
                    + (tri.verts[i].xyz.oGet(1) * 0.007f)
                    + (tri.verts[i].xyz.oGet(2) * 0.011f);

            f = timeOfs + (domain * f);
            f += timeOfs;

            ac[i] = tri.verts[i];

            ac[i].st.oPluSet(0, range * table.TableLookup(f));
            ac[i].st.oPluSet(1, range * table.TableLookup(f + tOfs));
        }

        R_FinishDeform(surf, newTri, ac);
    }
//=====================================================================================

    /*
     =====================
     AddTriangleToIsland_r

     =====================
     */
    static final int MAX_EYEBALL_TRIS = 10;
    static final int MAX_EYEBALL_ISLANDS = 6;

    static class eyeIsland_t {

        int[]    tris = new int[MAX_EYEBALL_TRIS];
        int      numTris;
        idBounds bounds;
        idVec3   mid;

        public eyeIsland_t() {
            this.bounds = new idBounds();
            this.mid = new idVec3();
        }
    }

    public static void AddTriangleToIsland_r(final srfTriangles_s tri, int triangleNum, boolean[] usedList, eyeIsland_t island) {
        int a, b, c;

        usedList[triangleNum] = true;

        // add to the current island
        if (island.numTris == MAX_EYEBALL_TRIS) {
            common.Error("MAX_EYEBALL_TRIS");
        }
        island.tris[island.numTris] = triangleNum;
        island.numTris++;

        // recurse into all neighbors
        a = tri.getIndexes()[triangleNum * 3];
        b = tri.getIndexes()[(triangleNum * 3) + 1];
        c = tri.getIndexes()[(triangleNum * 3) + 2];

        island.bounds.AddPoint(tri.verts[a].xyz);
        island.bounds.AddPoint(tri.verts[b].xyz);
        island.bounds.AddPoint(tri.verts[c].xyz);

        final int numTri = tri.getNumIndexes() / 3;
        for (int i = 0; i < numTri; i++) {
            if (usedList[i]) {
                continue;
            }
            if ((tri.getIndexes()[(i * 3) + 0] == a)
                    || (tri.getIndexes()[(i * 3) + 1] == a)
                    || (tri.getIndexes()[(i * 3) + 2] == a)
                    || (tri.getIndexes()[(i * 3) + 0] == b)
                    || (tri.getIndexes()[(i * 3) + 1] == b)
                    || (tri.getIndexes()[(i * 3) + 2] == b)
                    || (tri.getIndexes()[(i * 3) + 0] == c)
                    || (tri.getIndexes()[(i * 3) + 1] == c)
                    || (tri.getIndexes()[(i * 3) + 2] == c)) {
                AddTriangleToIsland_r(tri, i, usedList, island);
            }
        }
    }

    /*
     =====================
     R_EyeballDeform

     Each eyeball surface should have an separate upright triangle behind it, long end
     pointing out the eye, and another single triangle in front of the eye for the focus point.
     =====================
     */
    public static void R_EyeballDeform(drawSurf_s surf) {
        int i, j, k;
        final srfTriangles_s tri;
        srfTriangles_s newTri;
        final eyeIsland_t[] islands = new eyeIsland_t[MAX_EYEBALL_ISLANDS];
        int numIslands;
        final boolean[] triUsed = new boolean[MAX_EYEBALL_ISLANDS * MAX_EYEBALL_TRIS];

        tri = surf.geo;

        // separate all the triangles into islands
        final int numTri = tri.getNumIndexes() / 3;
        if (numTri > (MAX_EYEBALL_ISLANDS * MAX_EYEBALL_TRIS)) {
            common.Printf("R_EyeballDeform: too many triangles in surface");
            return;
        }
//	memset( triUsed, 0, sizeof( triUsed ) );

        for (numIslands = 0; numIslands < MAX_EYEBALL_ISLANDS; numIslands++) {
            islands[numIslands] = new eyeIsland_t();
            islands[numIslands].numTris = 0;
            islands[numIslands].bounds.Clear();
            for (i = 0; i < numTri; i++) {
                if (!triUsed[i]) {
                    AddTriangleToIsland_r(tri, i, triUsed, islands[numIslands]);
                    break;
                }
            }
            if (i == numTri) {
                break;
            }
        }

        // assume we always have two eyes, two origins, and two targets
        if (numIslands != 3) {
            common.Printf("R_EyeballDeform: %d triangle islands\n", numIslands);
            return;
        }

        // this srfTriangles_t and all its indexes and caches are in frame
        // memory, and will be automatically disposed of
        // the surface cannot have more indexes or verts than the original
        newTri = new srfTriangles_s();// R_ClearedFrameAlloc(sizeof(newTri));
        newTri.numVerts = tri.numVerts;
        newTri.setNumIndexes(tri.getNumIndexes());
        newTri.setIndexes(new int[tri.getNumIndexes()]);
        final idDrawVert[] ac = Stream.generate(idDrawVert::new).limit(tri.numVerts).toArray(idDrawVert[]::new);

        newTri.setNumIndexes(0);

        // decide which islands are the eyes and points
        for (i = 0; i < numIslands; i++) {
            islands[i].mid.oSet(islands[i].bounds.GetCenter());
        }

        for (i = 0; i < numIslands; i++) {
            final eyeIsland_t island = islands[i];

            if (island.numTris == 1) {
                continue;
            }

            // the closest single triangle point will be the eye origin
            // and the next-to-farthest will be the focal point
            idVec3 origin, focus;
            int originIsland = 0;
            final float[] dist = new float[MAX_EYEBALL_ISLANDS];
            final int[] sortOrder = new int[MAX_EYEBALL_ISLANDS];

            for (j = 0; j < numIslands; j++) {
                final idVec3 dir = islands[j].mid.oMinus(island.mid);
                dist[j] = dir.Length();
                sortOrder[j] = j;
                for (k = j - 1; k >= 0; k--) {
                    if (dist[k] > dist[k + 1]) {
                        final int temp = sortOrder[k];
                        sortOrder[k] = sortOrder[k + 1];
                        sortOrder[k + 1] = temp;
                        final float ftemp = dist[k];
                        dist[k] = dist[k + 1];
                        dist[k + 1] = ftemp;
                    }
                }
            }

            originIsland = sortOrder[1];
            origin = islands[originIsland].mid;

            focus = islands[sortOrder[2]].mid;

            // determine the projection directions based on the origin island triangle
            final idVec3 dir = focus.oMinus(origin);
            dir.Normalize();

            final idVec3 p1 = tri.verts[tri.getIndexes()[islands[originIsland].tris[0] + 0]].xyz;
            final idVec3 p2 = tri.verts[tri.getIndexes()[islands[originIsland].tris[0] + 1]].xyz;
            final idVec3 p3 = tri.verts[tri.getIndexes()[islands[originIsland].tris[0] + 2]].xyz;

            final idVec3 v1 = p2.oMinus(p1);
            v1.Normalize();
            final idVec3 v2 = p3.oMinus(p1);
            v2.Normalize();

            // texVec[0] will be the normal to the origin triangle
            final idVec3[] texVec = {new idVec3(), new idVec3()};

            texVec[0].Cross(v1, v2);

            texVec[1].Cross(texVec[0], dir);

            for (j = 0; j < 2; j++) {
                texVec[j].oMinSet(dir.oMultiply(texVec[j].oMultiply(dir)));
                texVec[j].Normalize();
            }

            // emit these triangles, generating the projected texcoords
            for (j = 0; j < islands[i].numTris; j++) {
                for (k = 0; k < 3; k++) {
                    int index = islands[i].tris[j] * 3;

                    index = tri.getIndexes()[index + k];
                    newTri.getIndexes()[newTri.incNumIndexes()] = index;

                    ac[index].xyz.oSet(tri.verts[index].xyz);

                    final idVec3 local = tri.verts[index].xyz.oMinus(origin);

                    ac[index].st.oSet(0, 0.5f + local.oMultiply(texVec[0]));
                    ac[index].st.oSet(1, 0.5f + local.oMultiply(texVec[1]));
                }
            }
        }

        R_FinishDeform(surf, newTri, ac);
    }

//==========================================================================================
    /*
     =====================
     R_ParticleDeform

     Emit particles from the surface instead of drawing it
     =====================
     */
    public static void R_ParticleDeform(drawSurf_s surf, boolean useArea) {
        final renderEntity_s renderEntity = surf.space.entityDef.parms;
        final viewDef_s viewDef = tr.viewDef;
        final idDeclParticle particleSystem = (idDeclParticle) surf.material.GetDeformDecl();

        if (RenderSystem_init.r_skipParticles.GetBool()) {
            return;
        }

//    if (false) {
//        if (renderEntity.shaderParms[SHADERPARM_PARTICLE_STOPTIME] != 0f
//                && viewDef.renderView.time * 0.001 >= renderEntity.shaderParms[SHADERPARM_PARTICLE_STOPTIME]) {
//            // the entire system has faded out
//            return null;
//        }
//    }
        //
        // calculate the area of all the triangles
        //
        final int numSourceTris = surf.geo.getNumIndexes() / 3;
        float totalArea = 0;
        Float[] sourceTriAreas = null;
        final srfTriangles_s srcTri = surf.geo;

        if (useArea) {
            sourceTriAreas = new Float[numSourceTris];
            int triNum = 0;
            for (int i = 0; i < srcTri.getNumIndexes(); i += 3, triNum++) {
                float area;
                area = idWinding.TriangleArea(srcTri.verts[srcTri.getIndexes()[i]].xyz, srcTri.verts[srcTri.getIndexes()[i + 1]].xyz, srcTri.verts[srcTri.getIndexes()[i + 2]].xyz);
                sourceTriAreas[triNum] = totalArea;
                totalArea += area;
            }
        }

        //
        // create the particles almost exactly the way idRenderModelPrt does
        //
        final particleGen_t g = new particleGen_t();

        g.renderEnt = renderEntity;
        g.renderView = viewDef.renderView;
        g.origin.Zero();
        g.axis.oSet(getMat3_identity());

        for (int currentTri = 0; currentTri < ((useArea) ? 1 : numSourceTris); currentTri++) {

            for (int stageNum = 0; stageNum < particleSystem.stages.Num(); stageNum++) {
                final idParticleStage stage = particleSystem.stages.oGet(stageNum);

                if (null == stage.material) {
                    continue;
                }
                if (0 == stage.cycleMsec) {
                    continue;
                }
                if (stage.hidden) {		// just for gui particle editor use
                    continue;
                }

                // we interpret stage.totalParticles as "particles per map square area"
                // so the systems look the same on different size surfaces
                final int totalParticles = (int) ((useArea) ? (stage.totalParticles * totalArea) / 4096.0 : (stage.totalParticles));

                final int count = totalParticles * stage.NumQuadsPerParticle();

                // allocate a srfTriangles in temp memory that can hold all the particles
                srfTriangles_s tri;

                tri = new srfTriangles_s();// R_ClearedFrameAlloc(sizeof(tri));
                tri.numVerts = 4 * count;
                tri.setNumIndexes(6 * count);
                tri.verts = new idDrawVert[tri.numVerts];// R_FrameAlloc(tri.numVerts);
                tri.setIndexes(new int[tri.getNumIndexes()]);// R_FrameAlloc(tri.numIndexes);

                // just always draw the particles
                tri.bounds.oSet(stage.bounds);

                tri.numVerts = 0;

                final idRandom steppingRandom = new idRandom(), steppingRandom2 = new idRandom();

                final int stageAge = (int) ((g.renderView.time + (renderEntity.shaderParms[SHADERPARM_TIMEOFFSET] * 1000)) - (stage.timeOffset * 1000));
                final int stageCycle = stageAge / stage.cycleMsec;
                int inCycleTime = stageAge - (stageCycle * stage.cycleMsec);

                // some particles will be in this cycle, some will be in the previous cycle
                steppingRandom.SetSeed(((stageCycle << 10) & idRandom.MAX_RAND) ^ (int) (renderEntity.shaderParms[SHADERPARM_DIVERSITY] * idRandom.MAX_RAND));
                steppingRandom2.SetSeed((((stageCycle - 1) << 10) & idRandom.MAX_RAND) ^ (int) (renderEntity.shaderParms[SHADERPARM_DIVERSITY] * idRandom.MAX_RAND));

                for (int index = 0; index < totalParticles; index++) {
                    g.index = index;

                    // bump the random
                    steppingRandom.RandomInt();
                    steppingRandom2.RandomInt();

                    // calculate local age for this index 
                    final int bunchOffset = (int) ((stage.particleLife * 1000 * stage.spawnBunching * index) / totalParticles);

                    final int particleAge = stageAge - bunchOffset;
                    final int particleCycle = particleAge / stage.cycleMsec;
                    if (particleCycle < 0) {
                        // before the particleSystem spawned
                        continue;
                    }
                    if ((stage.cycles != 0) && (particleCycle >= stage.cycles)) {
                        // cycled systems will only run cycle times
                        continue;
                    }

                    if (particleCycle == stageCycle) {
                        g.random = new idRandom(steppingRandom);
                    } else {
                        g.random = new idRandom(steppingRandom2);
                    }

                    inCycleTime = particleAge - (particleCycle * stage.cycleMsec);

                    if ((renderEntity.shaderParms[SHADERPARM_PARTICLE_STOPTIME] != 0)
                            && ((g.renderView.time - inCycleTime) >= (renderEntity.shaderParms[SHADERPARM_PARTICLE_STOPTIME] * 1000))) {
                        // don't fire any more particles
                        continue;
                    }

                    // supress particles before or after the age clamp
                    g.frac = inCycleTime / (stage.particleLife * 1000);
                    if (g.frac < 0) {
                        // yet to be spawned
                        continue;
                    }
                    if (g.frac > 1.0) {
                        // this particle is in the deadTime band
                        continue;
                    }

                    //---------------
                    // locate the particle origin and axis somewhere on the surface
                    //---------------
                    int pointTri = currentTri;

                    if (useArea) {
                        // select a triangle based on an even area distribution
                        pointTri = idBinSearch_LessEqual(sourceTriAreas, numSourceTris, g.random.RandomFloat() * totalArea);
                    }

                    // now pick a random point inside pointTri
                    final idDrawVert v1 = srcTri.verts[srcTri.getIndexes()[(pointTri * 3) + 0]];
                    final idDrawVert v2 = srcTri.verts[srcTri.getIndexes()[(pointTri * 3) + 1]];
                    final idDrawVert v3 = srcTri.verts[srcTri.getIndexes()[(pointTri * 3) + 2]];

                    float f1 = g.random.RandomFloat();
                    float f2 = g.random.RandomFloat();
                    float f3 = g.random.RandomFloat();

                    final float ft = 1.0f / (f1 + f2 + f3 + 0.0001f);

                    f1 *= ft;
                    f2 *= ft;
                    f3 *= ft;

                    g.origin.oSet(v1.xyz.oMultiply(f1).oPlus(v2.xyz.oMultiply(f2).oPlus(v3.xyz.oMultiply(f3))));
                    g.axis.oSet(0, v1.tangents[0].oMultiply(f1).oPlus(v2.tangents[0].oMultiply(f2).oPlus(v3.tangents[0].oMultiply(f3))));
                    g.axis.oSet(1, v1.tangents[1].oMultiply(f1).oPlus(v2.tangents[1].oMultiply(f2).oPlus(v3.tangents[1].oMultiply(f3))));
                    g.axis.oSet(2, v1.normal.oMultiply(f1).oPlus(v2.normal.oMultiply(f2).oPlus(v3.normal.oMultiply(f3))));

                    //-----------------------
                    // this is needed so aimed particles can calculate origins at different times
                    g.originalRandom = new idRandom(g.random);

                    g.age = g.frac * stage.particleLife;

                    // if the particle doesn't get drawn because it is faded out or beyond a kill region,
                    // don't increment the verts
                    tri.numVerts += stage.CreateParticle(g, Arrays.copyOfRange(tri.verts, tri.numVerts, tri.verts.length));
                }

                if (tri.numVerts > 0) {
                    // build the index list
                    int indexes = 0;
                    for (int i = 0; i < tri.numVerts; i += 4) {
                        tri.getIndexes()[indexes + 0] = i;
                        tri.getIndexes()[indexes + 1] = i + 2;
                        tri.getIndexes()[indexes + 2] = i + 3;
                        tri.getIndexes()[indexes + 3] = i;
                        tri.getIndexes()[indexes + 4] = i + 3;
                        tri.getIndexes()[indexes + 5] = i + 1;
                        indexes += 6;
                    }
                    tri.setNumIndexes(indexes);
                    tri.ambientCache = vertexCache.AllocFrameTemp(tri.verts, tri.numVerts * idDrawVert.BYTES);
                    if (tri.ambientCache != null) {
                        // add the drawsurf
                        R_AddDrawSurf(tri, surf.space, renderEntity, stage.material, surf.scissorRect);
                    }
                }
            }
        }
    }

//========================================================================================

    /*
     =================
     R_DeformDrawSurf
     =================
     */
    public static void R_DeformDrawSurf(drawSurf_s drawSurf) {
        if (null == drawSurf.material) {
            return;
        }

        if (r_skipDeforms.GetBool()) {
            return;
        }
        switch (drawSurf.material.Deform()) {
            case DFRM_NONE:
                return;
            case DFRM_SPRITE:
                R_AutospriteDeform(drawSurf);
                break;
            case DFRM_TUBE:
                R_TubeDeform(drawSurf);
                break;
            case DFRM_FLARE:
                R_FlareDeform(drawSurf);
                break;
            case DFRM_EXPAND:
                R_ExpandDeform(drawSurf);
                break;
            case DFRM_MOVE:
                R_MoveDeform(drawSurf);
                break;
            case DFRM_TURB:
                R_TurbulentDeform(drawSurf);
                break;
            case DFRM_EYEBALL:
                R_EyeballDeform(drawSurf);
                break;
            case DFRM_PARTICLE:
                R_ParticleDeform(drawSurf, true);
                break;
            case DFRM_PARTICLE2:
                R_ParticleDeform(drawSurf, false);
                break;
        }
    }
}
