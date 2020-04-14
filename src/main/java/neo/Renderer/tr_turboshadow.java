package neo.Renderer;

import static neo.Renderer.Interaction.LIGHT_CULL_ALL_FRONT;
import static neo.Renderer.Interaction.R_CalcInteractionCullBits;
import static neo.Renderer.Interaction.R_CalcInteractionFacing;
import static neo.Renderer.Model.SHADOW_CAP_INFINITE;
import static neo.Renderer.tr_local.USE_TRI_DATA_ALLOCATOR;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfShadowVerts;
import static neo.Renderer.tr_trisurf.R_ResizeStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_ResizeStaticTriSurfShadowVerts;
import static neo.idlib.math.Simd.SIMDProcessor;

import neo.Renderer.Interaction.srfCullInfo_t;
import neo.Renderer.Model.shadowCache_s;
import neo.Renderer.Model.silEdge_t;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.tr_local.idRenderEntityLocal;
import neo.Renderer.tr_local.idRenderLightLocal;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class tr_turboshadow {

    /*
     ============================================================

     TR_TURBOSHADOW

     Fast, non-clipped overshoot shadow volumes

     "facing" should have one more element than tri->numIndexes / 3, which should be set to 1
     calling this function may modify "facing" based on culling

     ============================================================
     */
    static int c_turboUsedVerts;
    static int c_turboUnusedVerts;


    /*
     =====================
     R_CreateVertexProgramTurboShadowVolume

     are dangling edges that are outside the light frustum still making planes?
     =====================
     */
    public static srfTriangles_s R_CreateVertexProgramTurboShadowVolume(final idRenderEntityLocal ent, final srfTriangles_s tri, final idRenderLightLocal light, srfCullInfo_t cullInfo) {
        int i, j;
        srfTriangles_s newTri;
        int sil;
        int/* glIndex_t */[] indexes;
        byte[] facing;

        R_CalcInteractionFacing(ent, tri, light, cullInfo);
        if (RenderSystem_init.r_useShadowProjectedCull.GetBool()) {
            R_CalcInteractionCullBits(ent, tri, light, cullInfo);
        }

        final int numFaces = tri.getIndexes().getNumValues() / 3;
        int numShadowingFaces = 0;
        facing = cullInfo.facing;

        // if all the triangles are inside the light frustum
        if ((cullInfo.cullBits == LIGHT_CULL_ALL_FRONT) || !RenderSystem_init.r_useShadowProjectedCull.GetBool()) {

            // count the number of shadowing faces
            for (i = 0; i < numFaces; i++) {
                numShadowingFaces += facing[i];
            }
            numShadowingFaces = numFaces - numShadowingFaces;

        } else {

            // make all triangles that are outside the light frustum "facing", so they won't cast shadows
            indexes = tri.getIndexes().getValues();
            final byte[] modifyFacing = cullInfo.facing;
            final byte[] cullBits = cullInfo.cullBits;
            for (j = i = 0; i < tri.getIndexes().getNumValues(); i += 3, j++) {
                if (0 == modifyFacing[j]) {
                    final int i1 = indexes[i + 0];
                    final int i2 = indexes[i + 1];
                    final int i3 = indexes[i + 2];
                    if ((cullBits[i1] & cullBits[i2] & cullBits[i3]) != 0) {
                        modifyFacing[j] = 1;
                    } else {
                        numShadowingFaces++;
                    }
                }
            }
        }

        if (0 == numShadowingFaces) {
            // no faces are inside the light frustum and still facing the right way
            return null;
        }

        // shadowVerts will be NULL on these surfaces, so the shadowVerts will be taken from the ambient surface
        newTri = R_AllocStaticTriSurf();

        newTri.numVerts = tri.numVerts * 2;

        // alloc the max possible size
        int/*glIndex_t */[] tempIndexes;
        int/*glIndex_t */[] shadowIndexes;
        if (USE_TRI_DATA_ALLOCATOR) {
            R_AllocStaticTriSurfIndexes(newTri, (numShadowingFaces + tri.numSilEdges) * 6);
            tempIndexes = newTri.getIndexes().getValues();
            shadowIndexes = newTri.getIndexes().getValues();
        } else {
            tempIndexes = new int[tri.numSilEdges * 6];
            shadowIndexes = tempIndexes;
        }

        int shadowIndex = 0;
        // create new triangles along sil planes
        for (sil = 0, i = tri.numSilEdges; i > 0; i--, sil++) {

            final int f1 = facing[tri.silEdges[sil].p1];
            final int f2 = facing[tri.silEdges[sil].p2];

            if (0 == (f1 ^ f2)) {
                continue;
            }

            final int v1 = tri.silEdges[sil].v1 << 1;
            final int v2 = tri.silEdges[sil].v2 << 1;

            // set the two triangle winding orders based on facing
            // without using a poorly-predictable branch
            shadowIndexes[shadowIndex + 0] = v1;
            shadowIndexes[shadowIndex + 1] = v2 ^ f1;
            shadowIndexes[shadowIndex + 2] = v2 ^ f2;
            shadowIndexes[shadowIndex + 3] = v1 ^ f2;
            shadowIndexes[shadowIndex + 4] = v1 ^ f1;
            shadowIndexes[shadowIndex + 5] = v2 ^ 1;

            shadowIndex += 6;
        }

        final int numShadowIndexes = shadowIndex;//shadowIndexes - tempIndexes;

        // we aren't bothering to separate front and back caps on these
        newTri.getIndexes().setNumValues(newTri.numShadowIndexesNoFrontCaps = numShadowIndexes + (numShadowingFaces * 6));
        newTri.numShadowIndexesNoCaps = numShadowIndexes;
        newTri.shadowCapPlaneBits = SHADOW_CAP_INFINITE;

        if (USE_TRI_DATA_ALLOCATOR) {
            // decrease the size of the memory block to only store the used indexes
            R_ResizeStaticTriSurfIndexes(newTri, newTri.getIndexes().getNumValues());
        } else {
            // allocate memory for the indexes
            R_AllocStaticTriSurfIndexes(newTri, newTri.getIndexes().getNumValues());
            // copy the indexes we created for the sil planes
            SIMDProcessor.Memcpy(newTri.getIndexes().getValues(), tempIndexes, numShadowIndexes /* sizeof( tempIndexes[0] )*/);
        }

        // these have no effect, because they extend to infinity
        newTri.bounds.Clear();

        // put some faces on the model and some on the distant projection
        indexes = tri.getIndexes().getValues();
        shadowIndex = numShadowIndexes;
        shadowIndexes = newTri.getIndexes().getValues();
        for (i = 0, j = 0; i < tri.getIndexes().getNumValues(); i += 3, j++) {
            if (facing[j] != 0) {
                continue;
            }

            final int i0 = indexes[i + 0] << 1;
            shadowIndexes[shadowIndex + 2] = i0;
            shadowIndexes[shadowIndex + 3] = i0 ^ 1;
            final int i1 = indexes[i + 1] << 1;
            shadowIndexes[shadowIndex + 1] = i1;
            shadowIndexes[shadowIndex + 4] = i1 ^ 1;
            final int i2 = indexes[i + 2] << 1;
            shadowIndexes[shadowIndex + 0] = i2;
            shadowIndexes[shadowIndex + 5] = i2 ^ 1;

            shadowIndex += 6;
        }

        return newTri;
    }

    /*
     =====================
     R_CreateTurboShadowVolume
     =====================
     */
    public static srfTriangles_s R_CreateTurboShadowVolume(final idRenderEntityLocal ent, final srfTriangles_s tri, final idRenderLightLocal light, srfCullInfo_t cullInfo) {
        int i, j;
        final idVec3 localLightOrigin = new idVec3();
        srfTriangles_s newTri;
        silEdge_t sil;
        int /*glIndex_t */[] indexes;
        final byte[] facing;

        R_CalcInteractionFacing(ent, tri, light, cullInfo);
        if (RenderSystem_init.r_useShadowProjectedCull.GetBool()) {
            R_CalcInteractionCullBits(ent, tri, light, cullInfo);
        }

        final int numFaces = tri.getIndexes().getNumValues() / 3;
        int numShadowingFaces = 0;
        facing = cullInfo.facing;

        // if all the triangles are inside the light frustum
        if ((cullInfo.cullBits == LIGHT_CULL_ALL_FRONT) || !RenderSystem_init.r_useShadowProjectedCull.GetBool()) {

            // count the number of shadowing faces
            for (i = 0; i < numFaces; i++) {
                numShadowingFaces += facing[i];
            }
            numShadowingFaces = numFaces - numShadowingFaces;

        } else {

            // make all triangles that are outside the light frustum "facing", so they won't cast shadows
            indexes = tri.getIndexes().getValues();
            final byte[] modifyFacing = cullInfo.facing;
            final byte[] cullBits = cullInfo.cullBits;
            for (j = i = 0; i < tri.getIndexes().getNumValues(); i += 3, j++) {
                if (0 == modifyFacing[j]) {
                    final int i1 = indexes[i + 0];
                    final int i2 = indexes[i + 1];
                    final int i3 = indexes[i + 2];
                    if ((cullBits[i1] & cullBits[i2] & cullBits[i3]) != 0) {
                        modifyFacing[j] = 1;
                    } else {
                        numShadowingFaces++;
                    }
                }
            }
        }

        if (0 == numShadowingFaces) {
            // no faces are inside the light frustum and still facing the right way
            return null;
        }

        newTri = R_AllocStaticTriSurf();

        shadowCache_s[] shadowVerts;
        if (USE_TRI_DATA_ALLOCATOR) {
            R_AllocStaticTriSurfShadowVerts(newTri, tri.numVerts * 2);
            shadowVerts = newTri.shadowVertexes;
        } else {
            shadowVerts = new shadowCache_s[tri.numVerts * 2];
        }

        R_GlobalPointToLocal(ent.modelMatrix, light.globalLightOrigin, localLightOrigin);

        final int[] vertRemap = new int[tri.numVerts];

        SIMDProcessor.Memset(vertRemap, -1, tri.numVerts /* sizeof(vertRemap[0])*/);

        for (i = 0, j = 0; i < tri.getIndexes().getNumValues(); i += 3, j++) {
            if (facing[j] != 0) {
                continue;
            }
            // this may pull in some vertexes that are outside
            // the frustum, because they connect to vertexes inside
            vertRemap[tri.silIndexes[i + 0]] = 0;
            vertRemap[tri.silIndexes[i + 1]] = 0;
            vertRemap[tri.silIndexes[i + 2]] = 0;
        }

        {
            final idVec4[] shadows = new idVec4[shadowVerts.length];

            for (int a = 0; a < shadows.length; a++) {
                shadows[a] = shadowVerts[a].xyz;
            }
            newTri.numVerts = SIMDProcessor.CreateShadowCache(shadows, vertRemap, localLightOrigin, tri.verts, tri.numVerts);
        }

        c_turboUsedVerts += newTri.numVerts;
        c_turboUnusedVerts += (tri.numVerts * 2) - newTri.numVerts;

        if (USE_TRI_DATA_ALLOCATOR) {
            R_ResizeStaticTriSurfShadowVerts(newTri, newTri.numVerts);
        } else {
            R_AllocStaticTriSurfShadowVerts(newTri, newTri.numVerts);
            SIMDProcessor.Memcpy(newTri.shadowVertexes, shadowVerts, newTri.numVerts /* sizeof( shadowVerts[0] ) */);
        }

        // alloc the max possible size
        int/*glIndex_t */[] tempIndexes;
        int/*glIndex_t */[] shadowIndexes;
        if (USE_TRI_DATA_ALLOCATOR) {
            R_AllocStaticTriSurfIndexes(newTri, (numShadowingFaces + tri.numSilEdges) * 6);
            tempIndexes = newTri.getIndexes().getValues();
            shadowIndexes = newTri.getIndexes().getValues();
        } else {
            tempIndexes = new int[tri.numSilEdges * 6];
            shadowIndexes = tempIndexes;
        }

        int sil_index = 0;
        int shadowIndex = 0;
        // create new triangles along sil planes
        for (sil = tri.silEdges[sil_index], i = tri.numSilEdges; i > 0; i--, sil = tri.silEdges[++sil_index]) {

            final int f1 = facing[sil.p1];
            final int f2 = facing[sil.p2];

            if (0 == (f1 ^ f2)) {
                continue;
            }

            final int v1 = vertRemap[sil.v1];
            final int v2 = vertRemap[sil.v2];

            // set the two triangle winding orders based on facing
            // without using a poorly-predictable branch
            shadowIndexes[shadowIndex + 0] = v1;
            shadowIndexes[shadowIndex + 1] = v2 ^ f1;
            shadowIndexes[shadowIndex + 2] = v2 ^ f2;
            shadowIndexes[shadowIndex + 3] = v1 ^ f2;
            shadowIndexes[shadowIndex + 4] = v1 ^ f1;
            shadowIndexes[shadowIndex + 5] = v2 ^ 1;

            shadowIndex += 6;
        }

        final int numShadowIndexes = shadowIndex;

        // we aren't bothering to separate front and back caps on these
        newTri.getIndexes().setNumValues(newTri.numShadowIndexesNoFrontCaps = numShadowIndexes + (numShadowingFaces * 6));
        newTri.numShadowIndexesNoCaps = numShadowIndexes;
        newTri.shadowCapPlaneBits = SHADOW_CAP_INFINITE;

        if (USE_TRI_DATA_ALLOCATOR) {
            // decrease the size of the memory block to only store the used indexes
            R_ResizeStaticTriSurfIndexes(newTri, newTri.getIndexes().getNumValues());
        } else {
            // allocate memory for the indexes
            R_AllocStaticTriSurfIndexes(newTri, newTri.getIndexes().getNumValues());
            // copy the indexes we created for the sil planes
            SIMDProcessor.Memcpy(newTri.getIndexes().getValues(), tempIndexes, numShadowIndexes /* sizeof( tempIndexes[0] )*/);
        }

        // these have no effect, because they extend to infinity
        newTri.bounds.Clear();

        // put some faces on the model and some on the distant projection
        indexes = tri.silIndexes;
        shadowIndex = numShadowIndexes;
        shadowIndexes = newTri.getIndexes().getValues();
        for (i = 0, j = 0; i < tri.getIndexes().getNumValues(); i += 3, j++) {
            if (facing[j] != 0) {
                continue;
            }

            final int i0 = vertRemap[indexes[i + 0]];
            shadowIndexes[shadowIndex + 2] = i0;
            shadowIndexes[shadowIndex + 3] = i0 ^ 1;
            final int i1 = vertRemap[indexes[i + 1]];
            shadowIndexes[shadowIndex + 1] = i1;
            shadowIndexes[shadowIndex + 4] = i1 ^ 1;
            final int i2 = vertRemap[indexes[i + 2]];
            shadowIndexes[shadowIndex + 0] = i2;
            shadowIndexes[shadowIndex + 5] = i2 ^ 1;

            shadowIndex += 6;
        }

        return newTri;
    }
}
