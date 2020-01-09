package neo.Renderer;

import java.util.Arrays;
import java.util.stream.Stream;
import static neo.Renderer.Interaction.LIGHT_TRIS_DEFERRED;
import neo.Renderer.Model.dominantTri_s;
import neo.Renderer.Model.shadowCache_s;
import neo.Renderer.Model.silEdge_t;
import neo.Renderer.Model.srfTriangles_s;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.tr_local.USE_TRI_DATA_ALLOCATOR;
import neo.Renderer.tr_local.deformInfo_s;
import static neo.Renderer.tr_local.frameData;
import neo.Renderer.tr_local.frameData_t;
import static neo.Renderer.tr_local.tr;
import neo.Renderer.tr_trisurf.faceTangents_t;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.sizeof;
import static neo.framework.BuildDefines._DEBUG;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.Common.common;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class tr_trisurf {

    /*
     ==============================================================================

     TRIANGLE MESH PROCESSING

     The functions in this file have no vertex / index count limits.

     Truly identical vertexes that match in position, normal, and texcoord can
     be merged away.

     Vertexes that match in position and texcoord, but have distinct normals will
     remain distinct for all purposes.  This is usually a poor choice for models,
     as adding a bevel face will not add any more vertexes, and will tend to
     look better.

     Match in position and normal, but differ in texcoords are referenced together
     for calculating tangent vectors for bump mapping.
     Artists should take care to have identical texels in all maps (bump/diffuse/specular)
     in this case

     Vertexes that only match in position are merged for shadow edge finding.

     Degenerate triangles.

     Overlapped triangles, even if normals or texcoords differ, must be removed.
     for the silhoette based stencil shadow algorithm to function properly.
     Is this true???
     Is the overlapped triangle problem just an example of the trippled edge problem?

     Interpenetrating triangles are not currently clipped to surfaces.
     Do they effect the shadows?

     if vertexes are intended to deform apart, make sure that no vertexes
     are on top of each other in the base frame, or the sil edges may be
     calculated incorrectly.

     We might be able to identify this from topology.

     Dangling edges are acceptable, but three way edges are not.

     Are any combinations of two way edges unacceptable, like one facing
     the backside of the other?


     Topology is determined by a collection of triangle indexes.

     The edge list can be built up from this, and stays valid even under
     deformations.

     Somewhat non-intuitively, concave edges cannot be optimized away, or the
     stencil shadow algorithm miscounts.

     Face normals are needed for generating shadow volumes and for calculating
     the silhouette, but they will change with any deformation.

     Vertex normals and vertex tangents will change with each deformation,
     but they may be able to be transformed instead of recalculated.

     bounding volume, both box and sphere will change with deformation.

     silhouette indexes
     shade indexes
     texture indexes

     shade indexes will only be > silhouette indexes if there is facet shading present

     lookups from texture to sil and texture to shade?

     The normal and tangent vector smoothing is simple averaging, no attempt is
     made to better handle the cases where the distribution around the shared vertex
     is highly uneven.


     we may get degenerate triangles even with the uniquing and removal
     if the vertexes have different texcoords.

     ==============================================================================
     */
    // this shouldn't change anything, but previously renderbumped models seem to need it
    static final boolean USE_INVA                    = true;
    //
    // instead of using the texture T vector, cross the normal and S vector for an orthogonal axis
    static final boolean DERIVE_UNSMOOTHED_BITANGENT = true;
    //
    static final int     MAX_SIL_EDGES               = 0x10000;
    static final int     SILEDGE_HASH_SIZE           = 1024;
    //
    static int         numSilEdges;
    static silEdge_t[] silEdges;
    static idHashIndex silEdgeHash = new idHashIndex(SILEDGE_HASH_SIZE, MAX_SIL_EDGES);
    static int numPlanes;
    //
    private static final boolean ID_DEBUG_MEMORY = false;

//
//    static final idBlockAlloc<srfTriangles_s> srfTrianglesAllocator = new idBlockAlloc<>(1 << 8);
//    
//    static final idDynamicBlockAlloc<idDrawVert> triVertexAllocator;
//    static final idDynamicBlockAlloc</*glIndex_t*/Integer> triIndexAllocator;
//    static final idDynamicBlockAlloc<shadowCache_s> triShadowVertexAllocator;
//    static final idDynamicBlockAlloc<idPlane> triPlaneAllocator;
//    static final idDynamicBlockAlloc</*glIndex_t*/Integer> triSilIndexAllocator;
//    static final idDynamicBlockAlloc<silEdge_t> triSilEdgeAllocator;
//    static final idDynamicBlockAlloc<dominantTri_s> triDominantTrisAllocator;
//    static final idDynamicBlockAlloc<Integer> triMirroredVertAllocator;
//    static final idDynamicBlockAlloc<Integer> triDupVertAllocator;
//
//    static {
//        if (USE_TRI_DATA_ALLOCATOR) {
//            triVertexAllocator = new idDynamicBlockAlloc(1 << 20, 1 << 10);
//            triIndexAllocator = new idDynamicBlockAlloc(1 << 18, 1 << 10);
//            triShadowVertexAllocator = new idDynamicBlockAlloc(1 << 18, 1 << 10);
//            triPlaneAllocator = new idDynamicBlockAlloc(1 << 17, 1 << 10);
//            triSilIndexAllocator = new idDynamicBlockAlloc(1 << 17, 1 << 10);
//            triSilEdgeAllocator = new idDynamicBlockAlloc(1 << 17, 1 << 10);
//            triDominantTrisAllocator = new idDynamicBlockAlloc(1 << 16, 1 << 10);
//            triMirroredVertAllocator = new idDynamicBlockAlloc(1 << 16, 1 << 10);
//            triDupVertAllocator = new idDynamicBlockAlloc(1 << 16, 1 << 10);
////        } else {
////            triVertexAllocator = new idDynamicAlloc(1 << 20, 1 << 10);
////            triIndexAllocator = new idDynamicAlloc(1 << 18, 1 << 10);
////            triShadowVertexAllocator = new idDynamicAlloc(1 << 18, 1 << 10);
////            triPlaneAllocator = new idDynamicAlloc(1 << 17, 1 << 10);
////            triSilIndexAllocator = new idDynamicAlloc(1 << 17, 1 << 10);
////            triSilEdgeAllocator = new idDynamicAlloc(1 << 17, 1 << 10);
////            triDominantTrisAllocator = new idDynamicAlloc(1 << 16, 1 << 10);
////            triMirroredVertAllocator = new idDynamicAlloc(1 << 16, 1 << 10);
////            triDupVertAllocator = new idDynamicAlloc(1 << 16, 1 << 10);
//        }
//    }

    /*
     ===============
     R_InitTriSurfData
     ===============
     */
    public static void R_InitTriSurfData() {
        silEdges = silEdge_t.generateArray(MAX_SIL_EDGES);

//
//        // initialize allocators for triangle surfaces
//        triVertexAllocator.Init();
//        triIndexAllocator.Init();
//        triShadowVertexAllocator.Init();
//        triPlaneAllocator.Init();
//        triSilIndexAllocator.Init();
//        triSilEdgeAllocator.Init();
//        triDominantTrisAllocator.Init();
//        triMirroredVertAllocator.Init();
//        triDupVertAllocator.Init();
//
//        // never swap out triangle surfaces
//        triVertexAllocator.SetLockMemory(true);
//        triIndexAllocator.SetLockMemory(true);
//        triShadowVertexAllocator.SetLockMemory(true);
//        triPlaneAllocator.SetLockMemory(true);
//        triSilIndexAllocator.SetLockMemory(true);
//        triSilEdgeAllocator.SetLockMemory(true);
//        triDominantTrisAllocator.SetLockMemory(true);
//        triMirroredVertAllocator.SetLockMemory(true);
//        triDupVertAllocator.SetLockMemory(true);
    }

    /*
     ===============
     R_ShutdownTriSurfData
     ===============
     */
    public static void R_ShutdownTriSurfData() {
        silEdges = null;//R_StaticFree(silEdges);
        silEdgeHash.Free();
//        srfTrianglesAllocator.Shutdown();
//        triVertexAllocator.Shutdown();
//        triIndexAllocator.Shutdown();
//        triShadowVertexAllocator.Shutdown();
//        triPlaneAllocator.Shutdown();
//        triSilIndexAllocator.Shutdown();
//        triSilEdgeAllocator.Shutdown();
//        triDominantTrisAllocator.Shutdown();
//        triMirroredVertAllocator.Shutdown();
//        triDupVertAllocator.Shutdown();
    }

    /*
     ===============
     R_PurgeTriSurfData
     ===============
     */
    public static void R_PurgeTriSurfData(frameData_t frame) {
        // free deferred triangle surfaces
        R_FreeDeferredTriSurfs(frame);

        // free empty base blocks
//        triVertexAllocator.FreeEmptyBaseBlocks();
//        triIndexAllocator.FreeEmptyBaseBlocks();
//        triShadowVertexAllocator.FreeEmptyBaseBlocks();
//        triPlaneAllocator.FreeEmptyBaseBlocks();
//        triSilIndexAllocator.FreeEmptyBaseBlocks();
//        triSilEdgeAllocator.FreeEmptyBaseBlocks();
//        triDominantTrisAllocator.FreeEmptyBaseBlocks();
//        triMirroredVertAllocator.FreeEmptyBaseBlocks();
//        triDupVertAllocator.FreeEmptyBaseBlocks();
    }

    /*
     ===============
     R_ShowTriMemory_f
     ===============
     */
    @Deprecated
    public static class R_ShowTriSurfMemory_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ShowTriSurfMemory_f();

        private R_ShowTriSurfMemory_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
//            common.Printf("%6d kB in %d triangle surfaces\n",
//                    (srfTrianglesAllocator.GetAllocCount() /* sizeof( srfTriangles_t )*/) >> 10,
//                    srfTrianglesAllocator.GetAllocCount());
//
//            common.Printf("%6d kB vertex memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triVertexAllocator.GetBaseBlockMemory() >> 10, triVertexAllocator.GetFreeBlockMemory() >> 10,
//                    triVertexAllocator.GetNumFreeBlocks(), triVertexAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB index memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triIndexAllocator.GetBaseBlockMemory() >> 10, triIndexAllocator.GetFreeBlockMemory() >> 10,
//                    triIndexAllocator.GetNumFreeBlocks(), triIndexAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB shadow vert memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triShadowVertexAllocator.GetBaseBlockMemory() >> 10, triShadowVertexAllocator.GetFreeBlockMemory() >> 10,
//                    triShadowVertexAllocator.GetNumFreeBlocks(), triShadowVertexAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB tri plane memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triPlaneAllocator.GetBaseBlockMemory() >> 10, triPlaneAllocator.GetFreeBlockMemory() >> 10,
//                    triPlaneAllocator.GetNumFreeBlocks(), triPlaneAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB sil index memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triSilIndexAllocator.GetBaseBlockMemory() >> 10, triSilIndexAllocator.GetFreeBlockMemory() >> 10,
//                    triSilIndexAllocator.GetNumFreeBlocks(), triSilIndexAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB sil edge memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triSilEdgeAllocator.GetBaseBlockMemory() >> 10, triSilEdgeAllocator.GetFreeBlockMemory() >> 10,
//                    triSilEdgeAllocator.GetNumFreeBlocks(), triSilEdgeAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB dominant tri memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triDominantTrisAllocator.GetBaseBlockMemory() >> 10, triDominantTrisAllocator.GetFreeBlockMemory() >> 10,
//                    triDominantTrisAllocator.GetNumFreeBlocks(), triDominantTrisAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB mirror vert memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triMirroredVertAllocator.GetBaseBlockMemory() >> 10, triMirroredVertAllocator.GetFreeBlockMemory() >> 10,
//                    triMirroredVertAllocator.GetNumFreeBlocks(), triMirroredVertAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB dup vert memory (%d kB free in %d blocks, %d empty base blocks)\n",
//                    triDupVertAllocator.GetBaseBlockMemory() >> 10, triDupVertAllocator.GetFreeBlockMemory() >> 10,
//                    triDupVertAllocator.GetNumFreeBlocks(), triDupVertAllocator.GetNumEmptyBaseBlocks());
//
//            common.Printf("%6d kB total triangle memory\n",
//                    (srfTrianglesAllocator.GetAllocCount() /* sizeof( srfTriangles_t )*/ + triVertexAllocator.GetBaseBlockMemory()
//                    + triIndexAllocator.GetBaseBlockMemory()
//                    + triShadowVertexAllocator.GetBaseBlockMemory()
//                    + triPlaneAllocator.GetBaseBlockMemory()
//                    + triSilIndexAllocator.GetBaseBlockMemory()
//                    + triSilEdgeAllocator.GetBaseBlockMemory()
//                    + triDominantTrisAllocator.GetBaseBlockMemory()
//                    + triMirroredVertAllocator.GetBaseBlockMemory()
//                    + triDupVertAllocator.GetBaseBlockMemory()) >> 10);
        }
    };

    /*
     =================
     R_TriSurfMemory

     For memory profiling
     =================
     */
    public static int R_TriSurfMemory(final srfTriangles_s tri) {
        int total = 0;

        if (null == tri) {
            return total;
        }

        // used as a flag in interactions
        if (tri == LIGHT_TRIS_DEFERRED) {
            return total;
        }

        if (tri.shadowVertexes != null) {
            total += tri.numVerts;//* sizeof( tri.shadowVertexes[0] );
        } else if (tri.verts != null) {
            if (tri.ambientSurface == null || tri.verts != tri.ambientSurface.verts) {
                total += tri.numVerts;// * sizeof( tri.verts[0] );
            }
        }
        if (tri.facePlanes != null) {
            total += tri.numIndexes / 3;//* sizeof( tri.facePlanes[0] );
        }
        if (tri.indexes != null) {
            if (tri.ambientSurface == null || tri.indexes != tri.ambientSurface.indexes) {
                total += tri.numIndexes;// * sizeof( tri.indexes[0] );
            }
        }
        if (tri.silIndexes != null) {
            total += tri.numIndexes;//* sizeof( tri.silIndexes[0] );
        }
        if (tri.silEdges != null) {
            total += tri.numSilEdges * sizeof(tri.silEdges[0]);
        }
        if (tri.dominantTris != null) {
            total += tri.numVerts;//* sizeof( tri.dominantTris[0] );
        }
        if (tri.mirroredVerts != null) {
            total += tri.numMirroredVerts;//* sizeof( tri.mirroredVerts[0] );
        }
        if (tri.dupVerts != null) {
            total += tri.numDupVerts;// * sizeof( tri.dupVerts[0] );
        }

        total += sizeof(tri);

        return total;
    }

    public static int R_TriSurfMemory(final srfTriangles_s[] tri) {
        throw new UnsupportedOperationException();
    }

    /*
     ==============
     R_FreeStaticTriSurfVertexCaches
     ==============
     */
    public static void R_FreeStaticTriSurfVertexCaches(srfTriangles_s tri) {
        if (tri.ambientSurface == null) {
            // this is a real model surface
            vertexCache.Free(tri.ambientCache);
            tri.ambientCache = null;
        } else {
            // this is a light interaction surface that references
            // a different ambient model surface
            vertexCache.Free(tri.lightingCache);
            tri.lightingCache = null;
        }
        if (tri.indexCache != null) {
            vertexCache.Free(tri.indexCache);
            tri.indexCache = null;
        }
        if ((tri.shadowCache != null) && (tri.shadowVertexes != null || tri.verts != null)) {
            // if we don't have tri.shadowVertexes, these are a reference to a
            // shadowCache on the original surface, which a vertex program
            // will take care of making unique for each light
            vertexCache.Free(tri.shadowCache);
            tri.shadowCache = null;
        }
    }

    /*
     ==============
     R_ReallyFreeStaticTriSurf

     This does the actual free
     ==============
     */
    public static void R_ReallyFreeStaticTriSurf(srfTriangles_s tri) {
        if (null == tri) {
            return;
        }

        R_FreeStaticTriSurfVertexCaches(tri);
//
//        if (tri.verts != null) {
//            // R_CreateLightTris points tri.verts at the verts of the ambient surface
//            if (tri.ambientSurface == null || tri.verts != tri.ambientSurface.verts) {
//                triVertexAllocator.Free(tri.verts);
//            }
//        }
//
//        if (!tri.deformedSurface) {
//            if (tri.indexes != null) {
//                // if a surface is completely inside a light volume R_CreateLightTris points tri.indexes at the indexes of the ambient surface
//                if (tri.ambientSurface == null || tri.indexes != tri.ambientSurface.indexes) {
//                    triIndexAllocator.Free(tri.indexes);
//                }
//            }
//            if (tri.silIndexes != null) {
//                triSilIndexAllocator.Free(tri.silIndexes);
//            }
//            if (tri.silEdges != null) {
//                triSilEdgeAllocator.Free(tri.silEdges);
//            }
//            if (tri.dominantTris != null) {
//                triDominantTrisAllocator.Free(tri.dominantTris);
//            }
//            if (tri.mirroredVerts != null) {
//                triMirroredVertAllocator.Free(tri.mirroredVerts);
//            }
//            if (tri.dupVerts != null) {
//                triDupVertAllocator.Free(tri.dupVerts);
//            }
//        }
//
//        if (tri.facePlanes != null) {
//            triPlaneAllocator.Free(tri.facePlanes);
//        }
//
//        if (tri.shadowVertexes != null) {
//            triShadowVertexAllocator.Free(tri.shadowVertexes);
//        }

        if (_DEBUG) {
//            memset(tri, 0, sizeof(srfTriangles_t));
            tri = new srfTriangles_s();
        }
//
//        srfTrianglesAllocator.Free(tri);
    }

    /*
     ==============
     R_CheckStaticTriSurfMemory
     ==============
     */
    public static void R_CheckStaticTriSurfMemory(final srfTriangles_s tri) {
        if (null == tri) {
            return;
        }
//
//        if (tri.verts != null) {
//            // R_CreateLightTris points tri.verts at the verts of the ambient surface
//            if (tri.ambientSurface == null || tri.verts != tri.ambientSurface.verts) {
//                final String error = triVertexAllocator.CheckMemory(tri.verts);
//                assert (error == null);
//            }
//        }
//
//        if (!tri.deformedSurface) {
//            if (tri.indexes != null) {
//                // if a surface is completely inside a light volume R_CreateLightTris points tri.indexes at the indexes of the ambient surface
//                if (tri.ambientSurface == null || tri.indexes != tri.ambientSurface.indexes) {
//                    final String error = triIndexAllocator.CheckMemory(tri.indexes);
//                    assert (error == null);
//                }
//            }
//        }
//
//        if (tri.shadowVertexes != null) {
//            final String error = triShadowVertexAllocator.CheckMemory(tri.shadowVertexes);
//            assert (error == null);
//        }
    }

    /*
     ==================
     R_FreeDeferredTriSurfs
     ==================
     */
    public static void R_FreeDeferredTriSurfs(frameData_t frame) {
        srfTriangles_s tri, next;

        if (null == frame) {
            return;
        }

        for (tri = frame.firstDeferredFreeTriSurf; tri != null; tri = next) {
            next = tri.nextDeferredFree;
            R_ReallyFreeStaticTriSurf(tri);
        }

        frame.firstDeferredFreeTriSurf = null;
        frame.lastDeferredFreeTriSurf = null;
    }

    /*
     ==============
     R_FreeStaticTriSurf

     This will defer the free until the current frame has run through the back end.
     ==============
     */
    public static void R_FreeStaticTriSurf(srfTriangles_s tri) {
        frameData_t frame;

        if (null == tri) {
            return;
        }

        if (tri.nextDeferredFree != null) {
            common.Error("R_FreeStaticTriSurf: freed a freed triangle");
        }
        frame = frameData;

        if (NOT(frame)) {
            // command line utility, or rendering in editor preview mode ( force )
            R_ReallyFreeStaticTriSurf(tri);
        } else {
            if (ID_DEBUG_MEMORY) {
                R_CheckStaticTriSurfMemory(tri);
            }
            tri.nextDeferredFree = null;
            if (frame.lastDeferredFreeTriSurf != null) {
                frame.lastDeferredFreeTriSurf.nextDeferredFree = tri;
            } else {
                frame.firstDeferredFreeTriSurf = tri;
            }
            frame.lastDeferredFreeTriSurf = tri;
        }
    }

    public static void R_FreeStaticTriSurf(srfTriangles_s[] tri) {
        throw new UnsupportedOperationException();
    }

    /*
     ==============
     R_AllocStaticTriSurf
     ==============
     */private static int DBG_R_AllocStaticTriSurf = 0;
    @Deprecated
    public static srfTriangles_s R_AllocStaticTriSurf() {DBG_R_AllocStaticTriSurf++;
//        srfTriangles_s tris = srfTrianglesAllocator.Alloc();
//        memset(tris, 0, sizeof(srfTriangles_t));
        return new srfTriangles_s();
    }

    /*
     =================
     R_CopyStaticTriSurf

     This only duplicates the indexes and verts, not any of the derived data.
     =================
     */
    public static srfTriangles_s R_CopyStaticTriSurf(final srfTriangles_s tri) {
        srfTriangles_s newTri;

        newTri = R_AllocStaticTriSurf();
        R_AllocStaticTriSurfVerts(newTri, tri.numVerts);
        R_AllocStaticTriSurfIndexes(newTri, tri.numIndexes);
        newTri.numVerts = tri.numVerts;
        newTri.numIndexes = tri.numIndexes;
//	memcpy( newTri.verts, tri.verts, tri.numVerts * sizeof( newTri.verts[0] ) );
        System.arraycopy(tri.verts, 0, newTri.verts, 0, tri.numVerts);
//	memcpy( newTri.indexes, tri.indexes, tri.numIndexes * sizeof( newTri.indexes[0] ) );
        System.arraycopy(tri.indexes, 0, newTri.indexes, 0, tri.numIndexes);

        return newTri;
    }

    /*
     =================
     R_AllocStaticTriSurfVerts
     =================
     */
    public static void R_AllocStaticTriSurfVerts(srfTriangles_s tri, int numVerts) {
        assert (tri.verts == null);
        tri.verts = new idDrawVert[numVerts];//triVertexAllocator.Alloc(numVerts);
        for (int a = 0; a < tri.verts.length; a++) {
            tri.verts[a] = new idDrawVert();
        }
    }

    /*
     =================
     R_AllocStaticTriSurfIndexes
     =================
     */
    public static void R_AllocStaticTriSurfIndexes(srfTriangles_s tri, int numIndexes) {
        assert (tri.indexes == null);
        tri.indexes = new int[numIndexes];// triIndexAllocator.Alloc(numIndexes);
    }

    /*
     =================
     R_AllocStaticTriSurfShadowVerts
     =================
     */
    public static void R_AllocStaticTriSurfShadowVerts(srfTriangles_s tri, int numVerts) {
        assert (tri.shadowVertexes == null);
        tri.shadowVertexes = new shadowCache_s[numVerts];//triShadowVertexAllocator.Alloc(numVerts);
        for (int a = 0; a < tri.shadowVertexes.length; a++) {
            tri.shadowVertexes[a] = new shadowCache_s();
        }
    }

    /*
     =================
     R_AllocStaticTriSurfPlanes
     =================
     */
    public static void R_AllocStaticTriSurfPlanes(srfTriangles_s tri, int numIndexes) {
//        if (tri.facePlanes != null) {
//            triPlaneAllocator.Free(tri.facePlanes);
//        }
        tri.facePlanes = new idPlane[numIndexes / 3];//triPlaneAllocator.Alloc(numIndexes / 3);
        for (int a = 0; a < tri.facePlanes.length; a++) {
            tri.facePlanes[a] = new idPlane();
        }
    }

    /*
     =================
     R_ResizeStaticTriSurfVerts
     =================
     */
    public static void R_ResizeStaticTriSurfVerts(srfTriangles_s tri, int numVerts) {
        if (USE_TRI_DATA_ALLOCATOR) {
            tri.verts = /*triVertexAllocator.*/ Resize(tri.verts, numVerts);
        } else {
            assert (false);
        }
    }

    /*
     =================
     R_ResizeStaticTriSurfIndexes
     =================
     */
    public static void R_ResizeStaticTriSurfIndexes(srfTriangles_s tri, int numIndexes) {
        if (USE_TRI_DATA_ALLOCATOR) {
            tri.indexes = /*triIndexAllocator.*/ Resize(tri.indexes, numIndexes);
        } else {
            assert (false);
        }
    }

    /*
     =================
     R_ResizeStaticTriSurfShadowVerts
     =================
     */
    public static void R_ResizeStaticTriSurfShadowVerts(srfTriangles_s tri, int numVerts) {
        if (USE_TRI_DATA_ALLOCATOR) {
            tri.shadowVertexes = /*triShadowVertexAllocator.*/ Resize(tri.shadowVertexes, numVerts);
        } else {
            assert (false);
        }
    }

    /*
     =================
     R_ReferenceStaticTriSurfVerts
     =================
     */
    public static void R_ReferenceStaticTriSurfVerts(srfTriangles_s tri, final srfTriangles_s reference) {
        tri.verts = reference.verts;
    }

    /*
     =================
     R_ReferenceStaticTriSurfIndexes
     =================
     */
    public static void R_ReferenceStaticTriSurfIndexes(srfTriangles_s tri, final srfTriangles_s reference) {
        tri.indexes = reference.indexes;
    }

    /*
     =================
     R_FreeStaticTriSurfSilIndexes
     =================
     */
    public static void R_FreeStaticTriSurfSilIndexes(srfTriangles_s tri) {
//        triSilIndexAllocator.Free(tri.silIndexes);
        tri.silIndexes = null;
    }

    /*
     ===============
     R_RangeCheckIndexes

     Check for syntactically incorrect indexes, like out of range values.
     Does not check for semantics, like degenerate triangles.

     No vertexes is acceptable if no indexes.
     No indexes is acceptable.
     More vertexes than are referenced by indexes are acceptable.
     ===============
     */
    public static void R_RangeCheckIndexes(final srfTriangles_s tri) {
        int i;

        if (tri.numIndexes < 0) {
            common.Error("R_RangeCheckIndexes: numIndexes < 0");
        }
        if (tri.numVerts < 0) {
            common.Error("R_RangeCheckIndexes: numVerts < 0");
        }

        // must specify an integral number of triangles
        if (tri.numIndexes % 3 != 0) {
            common.Error("R_RangeCheckIndexes: numIndexes %% 3");
        }

        for (i = 0; i < tri.numIndexes; i++) {
            if (tri.indexes[i] < 0 || tri.indexes[i] >= tri.numVerts) {
                common.Error("R_RangeCheckIndexes: index out of range");
            }
        }

        // this should not be possible unless there are unused verts
        if (tri.numVerts > tri.numIndexes) {
            // FIXME: find the causes of these
            // common.Printf( "R_RangeCheckIndexes: tri.numVerts > tri.numIndexes\n" );
        }
    }

    /*
     =================
     R_BoundTriSurf
     =================
     */
    public static void R_BoundTriSurf(srfTriangles_s tri) {
        SIMDProcessor.MinMax(tri.bounds.oGet(0), tri.bounds.oGet(1), tri.verts, tri.numVerts);
    }

    /*
     =================
     R_CreateSilRemap
     =================
     */
    public static int[] R_CreateSilRemap(final srfTriangles_s tri) {
        int c_removed, c_unique;
        int[] remap;
        int i, j, hashKey;
        idDrawVert v1, v2;

        remap = new int[tri.numVerts];// R_ClearedStaticAlloc(tri.numVerts);

        if (!RenderSystem_init.r_useSilRemap.GetBool()) {
            for (i = 0; i < tri.numVerts; i++) {
                remap[i] = i;
            }
            return remap;
        }

        idHashIndex hash = new idHashIndex(1024, tri.numVerts);

        c_removed = 0;
        c_unique = 0;
        for (i = 0; i < tri.numVerts; i++) {
            v1 = tri.verts[i];

            // see if there is an earlier vert that it can map to
            hashKey = hash.GenerateKey(v1.xyz);
            for (j = hash.First(hashKey); j >= 0; j = hash.Next(j)) {
                v2 = tri.verts[j];
                if (v2.xyz.oGet(0) == v1.xyz.oGet(0)
                        && v2.xyz.oGet(1) == v1.xyz.oGet(1)
                        && v2.xyz.oGet(2) == v1.xyz.oGet(2)) {
                    c_removed++;
                    remap[i] = j;
                    break;
                }
            }
            if (j < 0) {
                c_unique++;
                remap[i] = i;
                hash.Add(hashKey, i);
            }
        }

        return remap;
    }

    /*
     =================
     R_CreateSilIndexes

     Uniquing vertexes only on xyz before creating sil edges reduces
     the edge count by about 20% on Q3 models
     =================
     */
    public static void R_CreateSilIndexes(srfTriangles_s tri) {
        int i;
        int[] remap;

        if (tri.silIndexes != null) {
//            triSilIndexAllocator.Free(tri.silIndexes);
            tri.silIndexes = null;
        }

        remap = R_CreateSilRemap(tri);

        // remap indexes to the first one
        tri.silIndexes = new int[tri.numIndexes];//triSilIndexAllocator.Alloc(tri.numIndexes);
        for (i = 0; i < tri.numIndexes; i++) {
            tri.silIndexes[i] = remap[tri.indexes[i]];
        }

//        R_StaticFree(remap);
    }

    /*
     =====================
     R_CreateDupVerts
     =====================
     */
    public static void R_CreateDupVerts(srfTriangles_s tri) {
        int i;

        int[] remap = new int[tri.numVerts];

        // initialize vertex remap in case there are unused verts
        for (i = 0; i < tri.numVerts; i++) {
            remap[i] = i;
        }

        // set the remap based on how the silhouette indexes are remapped
        for (i = 0; i < tri.numIndexes; i++) {
            remap[tri.indexes[i]] = tri.silIndexes[i];
        }

        // create duplicate vertex index based on the vertex remap
        int[] tempDupVerts = new int[tri.numVerts * 2];
        tri.numDupVerts = 0;
        for (i = 0; i < tri.numVerts; i++) {
            if (remap[i] != i) {
                tempDupVerts[tri.numDupVerts * 2 + 0] = i;
                tempDupVerts[tri.numDupVerts * 2 + 1] = remap[i];
                tri.numDupVerts++;
            }
        }

        tri.dupVerts = new int[tri.numDupVerts * 2];// triDupVertAllocator.Alloc(tri.numDupVerts * 2);
//	memcpy( tri.dupVerts, tempDupVerts, tri.numDupVerts * 2 * sizeof( tri.dupVerts[0] ) );
        System.arraycopy(tempDupVerts, 0, tri.dupVerts, 0, tri.numDupVerts * 2);
    }

    /*
     =====================
     R_DeriveFacePlanes

     Writes the facePlanes values, overwriting existing ones if present
     =====================
     */
    public static void R_DeriveFacePlanes(srfTriangles_s tri) {
        idPlane[] planes;

        if (null == tri.facePlanes) {
            R_AllocStaticTriSurfPlanes(tri, tri.numIndexes);
        }
        planes = tri.facePlanes;

        if (true) {

            SIMDProcessor.DeriveTriPlanes(planes, tri.verts, tri.numVerts, tri.indexes, tri.numIndexes);

//}else{
//
//	for ( int i = 0; i < tri.numIndexes; i+= 3, planes++ ) {
//		int		i1, i2, i3;
//		idVec3	d1, d2, normal;
//		idVec3	v1, v2, v3;
//
//		i1 = tri.indexes[i + 0];
//		i2 = tri.indexes[i + 1];
//		i3 = tri.indexes[i + 2];
//
//		v1 = tri.verts[i1].xyz;
//		v2 = tri.verts[i2].xyz;
//		v3 = tri.verts[i3].xyz;
//
//		d1[0] = v2.x - v1.x;
//		d1[1] = v2.y - v1.y;
//		d1[2] = v2.z - v1.z;
//
//		d2[0] = v3.x - v1.x;
//		d2[1] = v3.y - v1.y;
//		d2[2] = v3.z - v1.z;
//
//		normal[0] = d2.y * d1.z - d2.z * d1.y;
//		normal[1] = d2.z * d1.x - d2.x * d1.z;
//		normal[2] = d2.x * d1.y - d2.y * d1.x;
//
//		float sqrLength, invLength;
//
//		sqrLength = normal.x * normal.x + normal.y * normal.y + normal.z * normal.z;
//		invLength = idMath.RSqrt( sqrLength );
//
//		(*planes)[0] = normal[0] * invLength;
//		(*planes)[1] = normal[1] * invLength;
//		(*planes)[2] = normal[2] * invLength;
//
//		planes.FitThroughPoint( *v1 );
//	}
        }

        tri.facePlanesCalculated = true;
    }

    /*
     =====================
     R_CreateVertexNormals

     Averages together the contributions of all faces that are
     used by a vertex, creating drawVert.normal
     =====================
     */
    public static void R_CreateVertexNormals(srfTriangles_s tri) {
        int i, j, p;
        idPlane plane;

        for (i = 0; i < tri.numVerts; i++) {
            tri.verts[i].normal.Zero();
        }

        if (null == tri.facePlanes || !tri.facePlanesCalculated) {
            R_DeriveFacePlanes(tri);
        }
        if (null == tri.silIndexes) {
            R_CreateSilIndexes(tri);
        }
        plane = tri.facePlanes[p = 0];
        for (i = 0; i < tri.numIndexes; i += 3, plane = tri.facePlanes[++p]) {
            for (j = 0; j < 3; j++) {
                int index = tri.silIndexes[i + j];
                tri.verts[index].normal.oPluSet(plane.Normal());
            }
        }

        // normalize and replicate from silIndexes to all indexes
        for (i = 0; i < tri.numIndexes; i++) {
            tri.verts[tri.indexes[i]].normal = tri.verts[tri.silIndexes[i]].normal;
            tri.verts[tri.indexes[i]].normal.Normalize();
        }
    }

    /*
     ===============
     R_DefineEdge
     ===============
     */
    static int c_duplicatedEdges, c_tripledEdges;

    public static void R_DefineEdge(int v1, int v2, int planeNum) {
        int i, hashKey;

        // check for degenerate edge
        if (v1 == v2) {
            return;
        }
        hashKey = silEdgeHash.GenerateKey(v1, v2);
        // search for a matching other side
        for (i = silEdgeHash.First(hashKey); i >= 0 && i < MAX_SIL_EDGES; i = silEdgeHash.Next(i)) {
            if (silEdges[i].v1 == v1 && silEdges[i].v2 == v2) {
                c_duplicatedEdges++;
                // allow it to still create a new edge
                continue;
            }
            if (silEdges[i].v2 == v1 && silEdges[i].v1 == v2) {
                if (silEdges[i].p2 != numPlanes) {
                    c_tripledEdges++;
                    // allow it to still create a new edge
                    continue;
                }
                // this is a matching back side
                silEdges[i].p2 = planeNum;
                return;
            }

        }

        // define the new edge
        if (numSilEdges == MAX_SIL_EDGES) {
            common.DWarning("MAX_SIL_EDGES");
            return;
        }

        silEdgeHash.Add(hashKey, numSilEdges);

        silEdges[numSilEdges].p1 = planeNum;
        silEdges[numSilEdges].p2 = numPlanes;
        silEdges[numSilEdges].v1 = v1;
        silEdges[numSilEdges].v2 = v2;

        numSilEdges++;
    }

    /*
     =================
     SilEdgeSort
     =================
     */
    public static class SilEdgeSort implements cmp_t<silEdge_t> {

        @Override
        public int compare(silEdge_t a, silEdge_t b) {
            if (a.p1 < b.p1) {
                return -1;
            }
            if (a.p1 > b.p1) {
                return 1;
            }
            if (a.p2 < b.p2) {
                return -1;//TODO:returning 1 is like true, 0 false...what is -1 then?
            }
            if (a.p2 > b.p2) {
                return 1;
            }
            return 0;
        }
    };

    /*
     =================
     R_IdentifySilEdges

     If the surface will not deform, coplanar edges (polygon interiors)
     can never create silhouette plains, and can be omited
     =================
     */
    static int c_coplanarSilEdges;
    static int c_totalSilEdges;

    public static void R_IdentifySilEdges(srfTriangles_s tri, boolean omitCoplanarEdges) {
        int i;
        int numTris;
        int shared, single;

        omitCoplanarEdges = false;	// optimization doesn't work for some reason

        numTris = tri.numIndexes / 3;

        numSilEdges = 0;
        silEdgeHash.Clear();
        numPlanes = numTris;

        c_duplicatedEdges = 0;
        c_tripledEdges = 0;

        for (i = 0; i < numTris; i++) {
            int i1, i2, i3;

            i1 = tri.silIndexes[i * 3 + 0];
            i2 = tri.silIndexes[i * 3 + 1];
            i3 = tri.silIndexes[i * 3 + 2];

            // create the edges
            R_DefineEdge(i1, i2, i);
            R_DefineEdge(i2, i3, i);
            R_DefineEdge(i3, i1, i);
        }

        if (c_duplicatedEdges != 0 || c_tripledEdges != 0) {
            common.DWarning("%d duplicated edge directions, %d tripled edges", c_duplicatedEdges, c_tripledEdges);
        }

        // if we know that the vertexes aren't going
        // to deform, we can remove interior triangulation edges
        // on otherwise planar polygons.
        // I earlier believed that I could also remove concave
        // edges, because they are never silhouettes in the conventional sense,
        // but they are still needed to balance out all the true sil edges
        // for the shadow algorithm to function
        int c_coplanarCulled;

        c_coplanarCulled = 0;
        if (omitCoplanarEdges) {
            for (i = 0; i < numSilEdges; i++) {
                int i1, i2, i3;
                idPlane plane = new idPlane();
                int base;
                int j;
                float d;

                if (silEdges[i].p2 == numPlanes) {	// the fake dangling edge
                    continue;
                }

                base = silEdges[i].p1 * 3;
                i1 = tri.silIndexes[base + 0];
                i2 = tri.silIndexes[base + 1];
                i3 = tri.silIndexes[base + 2];

                plane.FromPoints(tri.verts[i1].xyz, tri.verts[i2].xyz, tri.verts[i3].xyz);

                // check to see if points of second triangle are not coplanar
                base = silEdges[i].p2 * 3;
                for (j = 0; j < 3; j++) {
                    i1 = tri.silIndexes[base + j];
                    d = plane.Distance(tri.verts[i1].xyz);
                    if (d != 0) {		// even a small epsilon causes problems
                        break;
                    }
                }

                if (j == 3) {
                    // we can cull this sil edge
//				memmove( &silEdges[i], &silEdges[i+1], (numSilEdges-i-1) * sizeof( silEdges[i] ) );
                    System.arraycopy(silEdges, i + 1, silEdges, i, numSilEdges - i - 1);
                    c_coplanarCulled++;
                    numSilEdges--;
                    i--;
                }
            }
            if (c_coplanarCulled != 0) {//TODO:should it be >0?
                c_coplanarSilEdges += c_coplanarCulled;
//			common.Printf( "%i of %i sil edges coplanar culled\n", c_coplanarCulled,
//				c_coplanarCulled + numSilEdges );
            }
        }
        c_totalSilEdges += numSilEdges;

        // sort the sil edges based on plane number
//        qsort(silEdges, numSilEdges, sizeof(silEdges[0]), SilEdgeSort);
        Arrays.sort(silEdges, 0, numSilEdges, new SilEdgeSort());

        // count up the distribution.
        // a perfectly built model should only have shared
        // edges, but most models will have some interpenetration
        // and dangling edges
        shared = 0;
        single = 0;
        for (i = 0; i < numSilEdges; i++) {
            if (silEdges[i].p2 == numPlanes) {
                single++;
            } else {
                shared++;
            }
        }

        if (0 == single) {
            tri.perfectHull = true;
        } else {
            tri.perfectHull = false;
        }

        tri.numSilEdges = numSilEdges;
        tri.silEdges = new silEdge_t[numSilEdges];//triSilEdgeAllocator.Alloc(numSilEdges);
//	memcpy( tri.silEdges, silEdges, numSilEdges * sizeof( tri.silEdges[0] ) );
        System.arraycopy(silEdges, 0, tri.silEdges, 0, numSilEdges);
        silEdges = silEdge_t.generateArray(silEdges.length);
    }

    /*
     ===============
     R_FaceNegativePolarity

     Returns true if the texture polarity of the face is negative, false if it is positive or zero
     ===============
     */
    public static boolean R_FaceNegativePolarity(final srfTriangles_s tri, int firstIndex) {
        idDrawVert a, b, c;
        float area;
        float[] d0 = new float[5], d1 = new float[5];

        a = tri.verts[tri.indexes[firstIndex + 0]];
        b = tri.verts[tri.indexes[firstIndex + 1]];
        c = tri.verts[tri.indexes[firstIndex + 2]];

        d0[3] = b.st.oGet(0) - a.st.oGet(0);
        d0[4] = b.st.oGet(1) - a.st.oGet(1);

        d1[3] = c.st.oGet(0) - a.st.oGet(0);
        d1[4] = c.st.oGet(1) - a.st.oGet(1);

        area = d0[3] * d1[4] - d0[4] * d1[3];
        if (area >= 0) {
            return false;
        }
        return true;
    }

    /*
     ==================
     R_DeriveFaceTangents
     ==================
     */
    public static class faceTangents_t {

        idVec3[] tangents = new idVec3[2];
        boolean negativePolarity;
        boolean degenerate;
        
        static faceTangents_t[] generateArray(final int length) {
            return Stream.
                    generate(faceTangents_t::new).
                    limit(length).
                    toArray(faceTangents_t[]::new);
        }
    };

    public static void R_DeriveFaceTangents(final srfTriangles_s tri, faceTangents_t[] faceTangents) {
        int i;
        int c_textureDegenerateFaces;
        int c_positive, c_negative;
        faceTangents_t ft;
        idDrawVert a, b, c;

        //
        // calculate tangent vectors for each face in isolation
        //
        c_positive = 0;
        c_negative = 0;
        c_textureDegenerateFaces = 0;
        for (i = 0; i < tri.numIndexes; i += 3) {
            float area;
            idVec3 temp;
            float[] d0 = new float[5], d1 = new float[5];

            ft = faceTangents[i / 3];

            a = tri.verts[tri.indexes[i + 0]];
            b = tri.verts[tri.indexes[i + 1]];
            c = tri.verts[tri.indexes[i + 2]];

            d0[0] = b.xyz.oGet(0) - a.xyz.oGet(0);
            d0[1] = b.xyz.oGet(1) - a.xyz.oGet(1);
            d0[2] = b.xyz.oGet(2) - a.xyz.oGet(2);
            d0[3] = b.st.oGet(0) - a.st.oGet(0);
            d0[4] = b.st.oGet(1) - a.st.oGet(1);

            d1[0] = c.xyz.oGet(0) - a.xyz.oGet(0);
            d1[1] = c.xyz.oGet(1) - a.xyz.oGet(1);
            d1[2] = c.xyz.oGet(2) - a.xyz.oGet(2);
            d1[3] = c.st.oGet(0) - a.st.oGet(0);
            d1[4] = c.st.oGet(1) - a.st.oGet(1);

            area = d0[3] * d1[4] - d0[4] * d1[3];
            if (Math.abs(area) < 1e-20f) {
                ft.negativePolarity = false;
                ft.degenerate = true;
                ft.tangents[0] = new idVec3();
                ft.tangents[1] = new idVec3();
                c_textureDegenerateFaces++;
                continue;
            }
            if (area > 0.0f) {
                ft.negativePolarity = false;
                c_positive++;
            } else {
                ft.negativePolarity = true;
                c_negative++;
            }
            ft.degenerate = false;

            if (USE_INVA) {
                final float inva = area < .0f ? -1 : 1;// was = 1.0f / area;

                temp = new idVec3(
                        (d0[0] * d1[4] - d0[4] * d1[0]) * inva,
                        (d0[1] * d1[4] - d0[4] * d1[1]) * inva,
                        (d0[2] * d1[4] - d0[4] * d1[2]) * inva);
                temp.Normalize();
                ft.tangents[0] = temp;

                temp = new idVec3(
                        (d0[3] * d1[0] - d0[0] * d1[3]) * inva,
                        (d0[3] * d1[1] - d0[1] * d1[3]) * inva,
                        (d0[3] * d1[2] - d0[2] * d1[3]) * inva);
                temp.Normalize();
                ft.tangents[1] = temp;
            } else {
                temp = new idVec3(
                        (d0[0] * d1[4] - d0[4] * d1[0]),
                        (d0[1] * d1[4] - d0[4] * d1[1]),
                        (d0[2] * d1[4] - d0[4] * d1[2]));
                temp.Normalize();
                ft.tangents[0] = temp;

                temp = new idVec3(
                        (d0[3] * d1[0] - d0[0] * d1[3]),
                        (d0[3] * d1[1] - d0[1] * d1[3]),
                        (d0[3] * d1[2] - d0[2] * d1[3]));
                temp.Normalize();
                ft.tangents[1] = temp;
            }
        }
    }

    /*
     ===================
     R_DuplicateMirroredVertexes

     Modifies the surface to bust apart any verts that are shared by both positive and
     negative texture polarities, so tangent space smoothing at the vertex doesn't
     degenerate.

     This will create some identical vertexes (which will eventually get different tangent
     vectors), so never optimize the resulting mesh, or it will get the mirrored edges back.

     Reallocates tri.verts and changes tri.indexes in place
     Silindexes are unchanged by this.

     sets mirroredVerts and mirroredVerts[]

     ===================
     */
    static class tangentVert_t {

        final boolean[] polarityUsed = new boolean[2];
        int negativeRemap;
    };

    public static void R_DuplicateMirroredVertexes(srfTriangles_s tri) {
        tangentVert_t[] tVerts;
        tangentVert_t vert;
        int i, j;
        int totalVerts;
        int numMirror;

        tVerts = new tangentVert_t[tri.numVerts];
        for (int t = 0; t < tVerts.length; t++) {
//	memset( tverts, 0, tri.numVerts * sizeof( *tverts ) );
            tVerts[t] = new tangentVert_t();
        }

        // determine texture polarity of each surface
        // mark each vert with the polarities it uses
        for (i = 0; i < tri.numIndexes; i += 3) {
            int polarity;

            polarity = btoi(R_FaceNegativePolarity(tri, i));
            for (j = 0; j < 3; j++) {
                tVerts[tri.indexes[i + j]].polarityUsed[polarity] = true;
            }
        }

        // now create new verts as needed
        totalVerts = tri.numVerts;
        for (i = 0; i < tri.numVerts; i++) {
            vert = tVerts[i];
            if (vert.polarityUsed[0] && vert.polarityUsed[1]) {
                vert.negativeRemap = totalVerts;
                totalVerts++;
            }
        }

        tri.numMirroredVerts = totalVerts - tri.numVerts;

        // now create the new list
        if (totalVerts == tri.numVerts) {
            tri.mirroredVerts = null;
            return;
        }

        tri.mirroredVerts = new int[tri.numMirroredVerts];//triMirroredVertAllocator.Alloc(tri.numMirroredVerts);

        if (USE_TRI_DATA_ALLOCATOR) {
            tri.verts = /*triVertexAllocator.*/ Resize(tri.verts, totalVerts);
        } else {
            idDrawVert[] oldVerts = tri.verts;
            R_AllocStaticTriSurfVerts(tri, totalVerts);
//	memcpy( tri.verts, oldVerts, tri.numVerts * sizeof( tri.verts[0] ) );
            System.arraycopy(oldVerts, 0, tri.verts, 0, tri.numVerts);
//            triVertexAllocator.Free(oldVerts);
        }

        // create the duplicates
        numMirror = 0;
        for (i = 0; i < tri.numVerts; i++) {
            j = tVerts[i].negativeRemap;
            if (j != 0) {
                tri.verts[j] = new idDrawVert(tri.verts[i]);
                tri.mirroredVerts[numMirror] = i;
                numMirror++;
            }
        }

        tri.numVerts = totalVerts;
        // change the indexes
        for (i = 0; i < tri.numIndexes; i++) {
            if (tVerts[tri.indexes[i]].negativeRemap != 0
                    && R_FaceNegativePolarity(tri, 3 * (i / 3))) {
                tri.indexes[i] = tVerts[tri.indexes[i]].negativeRemap;
            }
        }

        tri.numVerts = totalVerts;
    }

    /*
     =================
     R_DeriveTangentsWithoutNormals

     Build texture space tangents for bump mapping
     If a surface is deformed, this must be recalculated

     This assumes that any mirrored vertexes have already been duplicated, so
     any shared vertexes will have the tangent spaces smoothed across.

     Texture wrapping slightly complicates this, but as long as the normals
     are shared, and the tangent vectors are projected onto the normals, the
     separate vertexes should wind up with identical tangent spaces.

     mirroring a normalmap WILL cause a slightly visible seam unless the normals
     are completely flat around the edge's full bilerp support.

     Vertexes which are smooth shaded must have their tangent vectors
     in the same plane, which will allow a seamless
     rendering as long as the normal map is even on both sides of the
     seam.

     A smooth shaded surface may have multiple tangent vectors at a vertex
     due to texture seams or mirroring, but it should only have a single
     normal vector.

     Each triangle has a pair of tangent vectors in it's plane

     Should we consider having vertexes point at shared tangent spaces
     to save space or speed transforms?

     this version only handles bilateral symetry
     =================
     */    static int DEBUG_R_DeriveTangentsWithoutNormals = 0;

    public static void R_DeriveTangentsWithoutNormals(srfTriangles_s tri) {
        int i, j;
        faceTangents_t[] faceTangents;
        faceTangents_t ft;
        idDrawVert vert;

        faceTangents = faceTangents_t.generateArray(tri.numIndexes / 3);
        R_DeriveFaceTangents(tri, faceTangents);

        // clear the tangents
        for (i = 0; i < tri.numVerts; i++) {
            tri.verts[i].tangents[0].Zero();
            tri.verts[i].tangents[1].Zero();
        }

        // sum up the neighbors
        for (i = 0; i < tri.numIndexes; i += 3) {
            ft = faceTangents[i / 3];

            // for each vertex on this face
            for (j = 0; j < 3; j++) {
                DEBUG_R_DeriveTangentsWithoutNormals++;
                vert = tri.verts[tri.indexes[i + j]];

//                System.out.println("--" + System.identityHashCode(vert.tangents[0])
//                        + "--" + i + j
//                        + "--" + tri.indexes[i + j]);
                vert.tangents[0].oPluSet(ft.tangents[0]);
                vert.tangents[1].oPluSet(ft.tangents[1]);
            }
        }

//if (false){
//	// sum up both sides of the mirrored verts
//	// so the S vectors exactly mirror, and the T vectors are equal
//	for ( i = 0 ; i < tri.numMirroredVerts ; i++ ) {
//		idDrawVert	v1, v2;
//
//		v1 = tri.verts[ tri.numVerts - tri.numMirroredVerts + i ];
//		v2 = tri.verts[ tri.mirroredVerts[i] ];
//
//		v1.tangents[0] -= v2.tangents[0];
//		v1.tangents[1] += v2.tangents[1];
//
//		v2.tangents[0] = vec3_origin - v1.tangents[0];
//		v2.tangents[1] = v1.tangents[1];
//	}
//}
        // project the summed vectors onto the normal plane
        // and normalize.  The tangent vectors will not necessarily
        // be orthogonal to each other, but they will be orthogonal
        // to the surface normal.
        for (i = 0; i < tri.numVerts; i++) {
            vert = tri.verts[i];
            for (j = 0; j < 2; j++) {
                float d;

                d = vert.tangents[j].oMultiply(vert.normal);
                vert.tangents[j] = vert.tangents[j].oMinus(vert.normal.oMultiply(d));
                vert.tangents[j].Normalize();
            }
        }

        tri.tangentsCalculated = true;
    }

    public static /*ID_INLINE*/ void VectorNormalizeFast2(final idVec3 v, idVec3 out) {
        float length;

        length = idMath.RSqrt(v.oGet(0) * v.oGet(0) + v.oGet(1) * v.oGet(1) + v.oGet(2) * v.oGet(2));
        out.oSet(0, v.oGet(0) * length);
        out.oSet(1, v.oGet(1) * length);
        out.oSet(2, v.oGet(2) * length);
    }

    /*
     ===================
     R_BuildDominantTris

     Find the largest triangle that uses each vertex
     ===================
     */
    static class indexSort_t {

        int vertexNum;
        int faceNum;
    };

    static class IndexSort implements cmp_t<indexSort_t> {

        @Override
        public int compare(indexSort_t a, indexSort_t b) {
            if (a.vertexNum < b.vertexNum) {
                return -1;
            }
            if (a.vertexNum > b.vertexNum) {
                return 1;
            }
            return 0;
        }
    };

    public static void R_BuildDominantTris(srfTriangles_s tri) {
        int i, j;
        dominantTri_s[] dt;
        indexSort_t[] ind = new indexSort_t[tri.numIndexes];// R_StaticAlloc(tri.numIndexes);

        for (i = 0; i < tri.numIndexes; i++) {
            ind[i] = new indexSort_t();
            ind[i].vertexNum = tri.indexes[i];
            ind[i].faceNum = i / 3;
        }
//        qsort(ind, tri.numIndexes, sizeof(ind[]), IndexSort);
        Arrays.sort(ind, 0, tri.numIndexes, new IndexSort());

        tri.dominantTris = dt = new dominantTri_s[tri.numVerts];// triDominantTrisAllocator.Alloc(tri.numVerts);
//	memset( dt, 0, tri.numVerts * sizeof( dt[0] ) );

        for (i = 0; i < tri.numIndexes; i += j) {
            float maxArea = 0;
            int vertNum = ind[i].vertexNum;
            for (j = 0; i + j < tri.numIndexes && ind[i + j].vertexNum == vertNum; j++) {
                float[] d0 = new float[5], d1 = new float[5];
                idDrawVert a, b, c;
                idVec3 normal = new idVec3(), tangent = new idVec3(), bitangent = new idVec3();

                int i1 = tri.indexes[ind[i + j].faceNum * 3 + 0];
                int i2 = tri.indexes[ind[i + j].faceNum * 3 + 1];
                int i3 = tri.indexes[ind[i + j].faceNum * 3 + 2];

                a = tri.verts[i1];
                b = tri.verts[i2];
                c = tri.verts[i3];

                d0[0] = b.xyz.oGet(0) - a.xyz.oGet(0);
                d0[1] = b.xyz.oGet(1) - a.xyz.oGet(1);
                d0[2] = b.xyz.oGet(2) - a.xyz.oGet(2);
                d0[3] = b.st.oGet(0) - a.st.oGet(0);
                d0[4] = b.st.oGet(1) - a.st.oGet(1);

                d1[0] = c.xyz.oGet(0) - a.xyz.oGet(0);
                d1[1] = c.xyz.oGet(1) - a.xyz.oGet(1);
                d1[2] = c.xyz.oGet(2) - a.xyz.oGet(2);
                d1[3] = c.st.oGet(0) - a.st.oGet(0);
                d1[4] = c.st.oGet(1) - a.st.oGet(1);

                normal.oSet(0, (d1[1] * d0[2] - d1[2] * d0[1]));
                normal.oSet(1, (d1[2] * d0[0] - d1[0] * d0[2]));
                normal.oSet(2, (d1[0] * d0[1] - d1[1] * d0[0]));

                float area = normal.Length();

                // if this is smaller than what we already have, skip it
                if (area < maxArea) {
                    continue;
                }
                maxArea = area;

                dt[vertNum] = new dominantTri_s();
                if (i1 == vertNum) {
                    dt[vertNum].v2 = i2;
                    dt[vertNum].v3 = i3;
                } else if (i2 == vertNum) {
                    dt[vertNum].v2 = i3;
                    dt[vertNum].v3 = i1;
                } else {
                    dt[vertNum].v2 = i1;
                    dt[vertNum].v3 = i2;
                }

                float len = area;
                if (len < 0.001f) {
                    len = 0.001f;
                }
                dt[vertNum].normalizationScale[2] = 1.0f / len;		// normal

                // texture area
                area = d0[3] * d1[4] - d0[4] * d1[3];

                tangent.oSet(0, (d0[0] * d1[4] - d0[4] * d1[0]));
                tangent.oSet(1, (d0[1] * d1[4] - d0[4] * d1[1]));
                tangent.oSet(2, (d0[2] * d1[4] - d0[4] * d1[2]));
                len = tangent.Length();
                if (len < 0.001f) {
                    len = 0.001f;
                }
                dt[vertNum].normalizationScale[0] = (area > 0 ? 1 : -1) / len;	// tangents[0]

                bitangent.oSet(0, (d0[3] * d1[0] - d0[0] * d1[3]));
                bitangent.oSet(1, (d0[3] * d1[1] - d0[1] * d1[3]));
                bitangent.oSet(2, (d0[3] * d1[2] - d0[2] * d1[3]));
                len = bitangent.Length();
                if (len < 0.001f) {
                    len = 0.001f;
                }
                if (DERIVE_UNSMOOTHED_BITANGENT) {
                    dt[vertNum].normalizationScale[1] = (area > 0 ? 1 : -1);
                } else {
                    dt[vertNum].normalizationScale[1] = (area > 0 ? 1 : -1) / len;	// tangents[1]
                }
            }
        }

//        R_StaticFree(ind);
    }

    /*
     ====================
     R_DeriveUnsmoothedTangents

     Uses the single largest area triangle for each vertex, instead of smoothing over all
     ====================
     */
    public static void R_DeriveUnsmoothedTangents(srfTriangles_s tri) {
        if (tri.tangentsCalculated) {
            return;
        }

        if (true) {

            SIMDProcessor.DeriveUnsmoothedTangents(tri.verts, tri.dominantTris, tri.numVerts);

//}else{
//
//	for ( int i = 0 ; i < tri.numVerts ; i++ ) {
//		idVec3		temp;
//		float		[]d0 = new float[5], d1=new float[5];
//		idDrawVert	a, b, c;
//		dominantTri_s	dt = tri.dominantTris[i];
//
//		a = tri.verts + i;
//		b = tri.verts + dt.v2;
//		c = tri.verts + dt.v3;
//
//		d0[0] = b.xyz[0] - a.xyz[0];
//		d0[1] = b.xyz[1] - a.xyz[1];
//		d0[2] = b.xyz[2] - a.xyz[2];
//		d0[3] = b.st[0] - a.st[0];
//		d0[4] = b.st[1] - a.st[1];
//
//		d1[0] = c.xyz[0] - a.xyz[0];
//		d1[1] = c.xyz[1] - a.xyz[1];
//		d1[2] = c.xyz[2] - a.xyz[2];
//		d1[3] = c.st[0] - a.st[0];
//		d1[4] = c.st[1] - a.st[1];
//
//		a.normal[0] = dt.normalizationScale[2] * ( d1[1] * d0[2] - d1[2] * d0[1] );
//		a.normal[1] = dt.normalizationScale[2] * ( d1[2] * d0[0] - d1[0] * d0[2] );
//		a.normal[2] = dt.normalizationScale[2] * ( d1[0] * d0[1] - d1[1] * d0[0] );
//
//		a.tangents[0][0] = dt.normalizationScale[0] * ( d0[0] * d1[4] - d0[4] * d1[0] );
//		a.tangents[0][1] = dt.normalizationScale[0] * ( d0[1] * d1[4] - d0[4] * d1[1] );
//		a.tangents[0][2] = dt.normalizationScale[0] * ( d0[2] * d1[4] - d0[4] * d1[2] );
//
//if(DERIVE_UNSMOOTHED_BITANGENT){
//		// derive the bitangent for a completely orthogonal axis,
//		// instead of using the texture T vector
//		a.tangents[1][0] = dt.normalizationScale[1] * ( a.normal[2] * a.tangents[0][1] - a.normal[1] * a.tangents[0][2] );
//		a.tangents[1][1] = dt.normalizationScale[1] * ( a.normal[0] * a.tangents[0][2] - a.normal[2] * a.tangents[0][0] );
//		a.tangents[1][2] = dt.normalizationScale[1] * ( a.normal[1] * a.tangents[0][0] - a.normal[0] * a.tangents[0][1] );
//        }else{
//		// calculate the bitangent from the texture T vector
//		a.tangents[1][0] = dt.normalizationScale[1] * ( d0[3] * d1[0] - d0[0] * d1[3] );
//		a.tangents[1][1] = dt.normalizationScale[1] * ( d0[3] * d1[1] - d0[1] * d1[3] );
//		a.tangents[1][2] = dt.normalizationScale[1] * ( d0[3] * d1[2] - d0[2] * d1[3] );
//}
//	}
        }

        tri.tangentsCalculated = true;
    }

    /*
     ==================
     R_DeriveTangents

     This is called once for static surfaces, and every frame for deforming surfaces

     Builds tangents, normals, and face planes
     ==================
     */
    public static void R_DeriveTangents(srfTriangles_s tri, boolean allocFacePlanes) {
        int i;
        idPlane[] planes;

        if (tri.dominantTris != null) {
            R_DeriveUnsmoothedTangents(tri);
            return;
        }

        if (tri.tangentsCalculated) {
            return;
        }

        tr.pc.c_tangentIndexes += tri.numIndexes;

        if (null == tri.facePlanes && allocFacePlanes) {
            R_AllocStaticTriSurfPlanes(tri, tri.numIndexes);
        }
        planes = tri.facePlanes;

        if (true) {

            if (null == planes) {
                planes = Stream.generate(idPlane::new).limit(tri.numIndexes / 3).toArray(idPlane[]::new);
            }

            SIMDProcessor.DeriveTangents(planes, tri.verts, tri.numVerts, tri.indexes, tri.numIndexes);

//}else{
//
//	for ( i = 0; i < tri.numVerts; i++ ) {
//		tri.verts[i].normal.Zero();
//		tri.verts[i].tangents[0].Zero();
//		tri.verts[i].tangents[1].Zero();
//	}
//
//	for ( i = 0; i < tri.numIndexes; i += 3 ) {
//		// make face tangents
//		float		d0[5], d1[5];
//		idDrawVert	*a, *b, *c;
//		idVec3		temp, normal, tangents[2];
//
//		a = tri.verts + tri.indexes[i + 0];
//		b = tri.verts + tri.indexes[i + 1];
//		c = tri.verts + tri.indexes[i + 2];
//
//		d0[0] = b.xyz[0] - a.xyz[0];
//		d0[1] = b.xyz[1] - a.xyz[1];
//		d0[2] = b.xyz[2] - a.xyz[2];
//		d0[3] = b.st[0] - a.st[0];
//		d0[4] = b.st[1] - a.st[1];
//
//		d1[0] = c.xyz[0] - a.xyz[0];
//		d1[1] = c.xyz[1] - a.xyz[1];
//		d1[2] = c.xyz[2] - a.xyz[2];
//		d1[3] = c.st[0] - a.st[0];
//		d1[4] = c.st[1] - a.st[1];
//
//		// normal
//		temp[0] = d1[1] * d0[2] - d1[2] * d0[1];
//		temp[1] = d1[2] * d0[0] - d1[0] * d0[2];
//		temp[2] = d1[0] * d0[1] - d1[1] * d0[0];
//		VectorNormalizeFast2( temp, normal );
//
//if (USE_INVA){
//		float area = d0[3] * d1[4] - d0[4] * d1[3];
//		float inva = area < 0.0f ? -1 : 1;		// was = 1.0f / area;
//
//        temp[0] = (d0[0] * d1[4] - d0[4] * d1[0]) * inva;
//        temp[1] = (d0[1] * d1[4] - d0[4] * d1[1]) * inva;
//        temp[2] = (d0[2] * d1[4] - d0[4] * d1[2]) * inva;
//		VectorNormalizeFast2( temp, tangents[0] );
//        
//        temp[0] = (d0[3] * d1[0] - d0[0] * d1[3]) * inva;
//        temp[1] = (d0[3] * d1[1] - d0[1] * d1[3]) * inva;
//        temp[2] = (d0[3] * d1[2] - d0[2] * d1[3]) * inva;
//		VectorNormalizeFast2( temp, tangents[1] );
//}else{
//        temp[0] = (d0[0] * d1[4] - d0[4] * d1[0]);
//        temp[1] = (d0[1] * d1[4] - d0[4] * d1[1]);
//        temp[2] = (d0[2] * d1[4] - d0[4] * d1[2]);
//		VectorNormalizeFast2( temp, tangents[0] );
//        
//        temp[0] = (d0[3] * d1[0] - d0[0] * d1[3]);
//        temp[1] = (d0[3] * d1[1] - d0[1] * d1[3]);
//        temp[2] = (d0[3] * d1[2] - d0[2] * d1[3]);
//		VectorNormalizeFast2( temp, tangents[1] );
//}
//
//		// sum up the tangents and normals for each vertex on this face
//		for ( int j = 0 ; j < 3 ; j++ ) {
//			vert = &tri.verts[tri.indexes[i+j]];
//			vert.normal += normal;
//			vert.tangents[0] += tangents[0];
//			vert.tangents[1] += tangents[1];
//		}
//
//		if ( planes ) {
//			planes.Normal() = normal;
//			planes.FitThroughPoint( a.xyz );
//			planes++;
//		}
//	}
        }

//if (false){
//
//	if ( tri.silIndexes != null ) {
//		for ( i = 0; i < tri.numVerts; i++ ) {
//			tri.verts[i].normal.Zero();
//		}
//		for ( i = 0; i < tri.numIndexes; i++ ) {
//			tri.verts[tri.silIndexes[i]].normal += planes[i/3].Normal();
//		}
//		for ( i = 0 ; i < tri.numIndexes ; i++ ) {
//			tri.verts[tri.indexes[i]].normal = tri.verts[tri.silIndexes[i]].normal;
//		}
//	}
//
//}else
        {

            int[] dupVerts = tri.dupVerts;
            idDrawVert[] verts = tri.verts;

            // add the normal of a duplicated vertex to the normal of the first vertex with the same XYZ
            for (i = 0; i < tri.numDupVerts; i++) {
                verts[dupVerts[i * 2 + 0]].normal.oPluSet(verts[dupVerts[i * 2 + 1]].normal);
            }

            // copy vertex normals to duplicated vertices
            for (i = 0; i < tri.numDupVerts; i++) {
                verts[dupVerts[i * 2 + 1]].normal = verts[dupVerts[i * 2 + 0]].normal;
            }

        }

//if (false){
//	// sum up both sides of the mirrored verts
//	// so the S vectors exactly mirror, and the T vectors are equal
//	for ( i = 0 ; i < tri.numMirroredVerts ; i++ ) {
//		idDrawVert	*v1, *v2;
//
//		v1 = &tri.verts[ tri.numVerts - tri.numMirroredVerts + i ];
//		v2 = &tri.verts[ tri.mirroredVerts[i] ];
//
//		v1.tangents[0] -= v2.tangents[0];
//		v1.tangents[1] += v2.tangents[1];
//
//		v2.tangents[0] = vec3_origin - v1.tangents[0];
//		v2.tangents[1] = v1.tangents[1];
//	}
//}
        // project the summed vectors onto the normal plane
        // and normalize.  The tangent vectors will not necessarily
        // be orthogonal to each other, but they will be orthogonal
        // to the surface normal.
        if (true) {

            SIMDProcessor.NormalizeTangents(tri.verts, tri.numVerts);

//}else{
//
//	for ( i = 0 ; i < tri.numVerts ; i++ ) {
//		idDrawVert *vert = &tri.verts[i];
//
//		VectorNormalizeFast2( vert.normal, vert.normal );
//
//		// project the tangent vectors
//		for ( int j = 0 ; j < 2 ; j++ ) {
//			float d;
//
//			d = vert.tangents[j] * vert.normal;
//			vert.tangents[j] = vert.tangents[j] - d * vert.normal;
//			VectorNormalizeFast2( vert.tangents[j], vert.tangents[j] );
//		}
//	}
//
        }

        tri.tangentsCalculated = true;
        tri.facePlanesCalculated = true;
    }

    public static void R_DeriveTangents(srfTriangles_s tri) {
        R_DeriveTangents(tri, true);
    }
    /*
     =================
     R_RemoveDuplicatedTriangles

     silIndexes must have already been calculated

     silIndexes are used instead of indexes, because duplicated
     triangles could have different texture coordinates.
     =================
     */

    public static void R_RemoveDuplicatedTriangles(srfTriangles_s tri) {
        int c_removed;
        int i, j, r;
        int a, b, c;

        c_removed = 0;

        // check for completely duplicated triangles
        // any rotation of the triangle is still the same, but a mirroring
        // is considered different
        for (i = 0; i < tri.numIndexes; i += 3) {
            for (r = 0; r < 3; r++) {
                a = tri.silIndexes[i + r];
                b = tri.silIndexes[i + (r + 1) % 3];
                c = tri.silIndexes[i + (r + 2) % 3];
                for (j = i + 3; j < tri.numIndexes; j += 3) {
                    if (tri.silIndexes[j] == a && tri.silIndexes[j + 1] == b && tri.silIndexes[j + 2] == c) {
                        c_removed++;
//					memmove( tri.indexes + j, tri.indexes + j + 3, ( tri.numIndexes - j - 3 ) * sizeof( tri.indexes[0] ) );
                        System.arraycopy(tri.indexes, j + 3, tri.indexes, j, tri.numIndexes - j - 3);
//					memmove( tri.silIndexes + j, tri.silIndexes + j + 3, ( tri.numIndexes - j - 3 ) * sizeof( tri.silIndexes[0] ) );
                        System.arraycopy(tri.silIndexes, j + 3, tri.silIndexes, j, tri.numIndexes - j - 3);
                        tri.numIndexes -= 3;
                        j -= 3;
                    }
                }
            }
        }

        if (c_removed != 0) {
            common.Printf("removed %d duplicated triangles\n", c_removed);
        }

    }

    /*
     =================
     R_RemoveDegenerateTriangles

     silIndexes must have already been calculated
     =================
     */
    public static void R_RemoveDegenerateTriangles(srfTriangles_s tri) {
        int c_removed;
        int i;
        int a, b, c;

        // check for completely degenerate triangles
        c_removed = 0;
        for (i = 0; i < tri.numIndexes; i += 3) {
            a = tri.silIndexes[i];
            b = tri.silIndexes[i + 1];
            c = tri.silIndexes[i + 2];
            if (a == b || a == c || b == c) {
                c_removed++;
//			memmove( tri.indexes + i, tri.indexes + i + 3, ( tri.numIndexes - i - 3 ) * sizeof( tri.indexes[0] ) );
                System.arraycopy(tri.indexes, i + 3, tri.indexes, i, tri.numIndexes - i - 3);
                if (tri.silIndexes != null) {
//				memmove( tri.silIndexes + i, tri.silIndexes + i + 3, ( tri.numIndexes - i - 3 ) * sizeof( tri.silIndexes[0] ) );
                    System.arraycopy(tri.silIndexes, i + 3, tri.silIndexes, i, tri.numIndexes - i - 3);
                }
                tri.numIndexes -= 3;
                i -= 3;
            }
        }

        // this doesn't free the memory used by the unused verts
        if (c_removed != 0) {
            common.Printf("removed %d degenerate triangles\n", c_removed);
        }
    }

    /*
     =================
     R_TestDegenerateTextureSpace
     =================
     */
    public static void R_TestDegenerateTextureSpace(srfTriangles_s tri) {
        int c_degenerate;
        int i;

        // check for triangles with a degenerate texture space
        c_degenerate = 0;
        for (i = 0; i < tri.numIndexes; i += 3) {
            final idDrawVert a = tri.verts[tri.indexes[i + 0]];
            final idDrawVert b = tri.verts[tri.indexes[i + 1]];
            final idDrawVert c = tri.verts[tri.indexes[i + 2]];

            if (a.st == b.st || b.st == c.st || c.st == a.st) {
                c_degenerate++;
            }
        }

        if (c_degenerate != 0) {
//		common.Printf( "%d triangles with a degenerate texture space\n", c_degenerate );
        }
    }

    /*
     =================
     R_RemoveUnusedVerts
     =================
     */
    public static void R_RemoveUnusedVerts(srfTriangles_s tri) {
        int i;
        int[] mark;
        int index;
        int used;

        mark = new int[tri.numVerts];// R_ClearedStaticAlloc(tri.numVerts);

        for (i = 0; i < tri.numIndexes; i++) {
            index = tri.indexes[i];
            if (index < 0 || index >= tri.numVerts) {
                common.Error("R_RemoveUnusedVerts: bad index");
            }
            mark[index] = 1;

            if (tri.silIndexes != null) {
                index = tri.silIndexes[i];
                if (index < 0 || index >= tri.numVerts) {
                    common.Error("R_RemoveUnusedVerts: bad index");
                }
                mark[index] = 1;
            }
        }

        used = 0;
        for (i = 0; i < tri.numVerts; i++) {
            if (0 == mark[i]) {
                continue;
            }
            mark[i] = used + 1;
            used++;
        }

        if (used != tri.numVerts) {
            for (i = 0; i < tri.numIndexes; i++) {
                tri.indexes[i] = mark[tri.indexes[i]] - 1;
                if (tri.silIndexes != null) {
                    tri.silIndexes[i] = mark[tri.silIndexes[i]] - 1;
                }
            }
            tri.numVerts = used;

            for (i = 0; i < tri.numVerts; i++) {
                index = mark[i];
                if (0 == index) {
                    continue;
                }
                tri.verts[index - 1] = tri.verts[i];
            }

            // this doesn't realloc the arrays to save the memory used by the unused verts
        }

//        R_StaticFree(mark);
    }

    /*
     =================
     R_MergeSurfaceList

     Only deals with vertexes and indexes, not silhouettes, planes, etc.
     Does NOT perform a cleanup triangles, so there may be duplicated verts in the result.
     =================
     */
    public static srfTriangles_s R_MergeSurfaceList(final srfTriangles_s[] surfaces, int numSurfaces) {
        srfTriangles_s newTri;
        srfTriangles_s tri;
        int i, j;
        int totalVerts;
        int totalIndexes;

        totalVerts = 0;
        totalIndexes = 0;
        for (i = 0; i < numSurfaces; i++) {
            totalVerts += surfaces[i].numVerts;
            totalIndexes += surfaces[i].numIndexes;
        }

        newTri = R_AllocStaticTriSurf();
        newTri.numVerts = totalVerts;
        newTri.numIndexes = totalIndexes;
        R_AllocStaticTriSurfVerts(newTri, newTri.numVerts);
        R_AllocStaticTriSurfIndexes(newTri, newTri.numIndexes);

        totalVerts = 0;
        totalIndexes = 0;
        for (i = 0; i < numSurfaces; i++) {
            tri = surfaces[i];
//		memcpy( newTri.verts + totalVerts, tri.verts, tri.numVerts * sizeof( *tri.verts ) );
            System.arraycopy(tri.verts, 0, newTri.verts, totalVerts, tri.numVerts);
            for (j = 0; j < tri.numIndexes; j++) {
                newTri.indexes[totalIndexes + j] = totalVerts + tri.indexes[j];
            }
            totalVerts += tri.numVerts;
            totalIndexes += tri.numIndexes;
        }

        return newTri;
    }

    /*
     =================
     R_MergeTriangles

     Only deals with vertexes and indexes, not silhouettes, planes, etc.
     Does NOT perform a cleanup triangles, so there may be duplicated verts in the result.
     =================
     */
    public static srfTriangles_s R_MergeTriangles(final srfTriangles_s tri1, final srfTriangles_s tri2) {
        final srfTriangles_s[] tris = new srfTriangles_s[2];

        tris[0] = tri1;
        tris[1] = tri2;

        return R_MergeSurfaceList(tris, 2);
    }

    /*
     =================
     R_ReverseTriangles

     Lit two sided surfaces need to have the triangles actually duplicated,
     they can't just turn on two sided lighting, because the normal and tangents
     are wrong on the other sides.

     This should be called before R_CleanupTriangles
     =================
     */
    public static void R_ReverseTriangles(srfTriangles_s tri) {
        int i;

        // flip the normal on each vertex
        // If the surface is going to have generated normals, this won't matter,
        // but if it has explicit normals, this will keep it on the correct side
        for (i = 0; i < tri.numVerts; i++) {
            tri.verts[i].normal = getVec3_origin().oMinus(tri.verts[i].normal);
        }

        // flip the index order to make them back sided
        for (i = 0; i < tri.numIndexes; i += 3) {
            int/*glIndex_t*/ temp;

            temp = tri.indexes[i + 0];
            tri.indexes[i + 0] = tri.indexes[i + 1];
            tri.indexes[i + 1] = temp;
        }
    }

    /*
     =================
     R_CleanupTriangles

     FIXME: allow createFlat and createSmooth normals, as well as explicit
     =================
     */private static int DBG_R_CleanupTriangles = 0;
    public static void R_CleanupTriangles(srfTriangles_s tri, boolean createNormals, boolean identifySilEdges, boolean useUnsmoothedTangents) {
        DBG_R_CleanupTriangles++;
        R_RangeCheckIndexes(tri);

        R_CreateSilIndexes(tri);

//	R_RemoveDuplicatedTriangles( tri );	// this may remove valid overlapped transparent triangles
        
        R_RemoveDegenerateTriangles(tri);

        R_TestDegenerateTextureSpace(tri);

//	R_RemoveUnusedVerts( tri );
        
        if (identifySilEdges) {
            R_IdentifySilEdges(tri, true);	// assume it is non-deformable, and omit coplanar edges
        }

        // bust vertexes that share a mirrored edge into separate vertexes
        R_DuplicateMirroredVertexes(tri);

        // optimize the index order (not working?)
//	R_OrderIndexes( tri.numIndexes, tri.indexes );
        
        R_CreateDupVerts(tri);

        R_BoundTriSurf(tri);

        if (useUnsmoothedTangents) {
            R_BuildDominantTris(tri);
            R_DeriveUnsmoothedTangents(tri);
        } else if (!createNormals) {
            R_DeriveFacePlanes(tri);
            R_DeriveTangentsWithoutNormals(tri);
        } else {
            R_DeriveTangents(tri);
        }
    }

    /*
     ===================================================================================

     DEFORMED SURFACES

     ===================================================================================
     */

    /*
     ===================
     R_BuildDeformInfo
     ===================
     */
    public static deformInfo_s R_BuildDeformInfo(int numVerts, final idDrawVert verts, int numIndexes, final int[] indexes, boolean useUnsmoothedTangents) {
        deformInfo_s deform;
        srfTriangles_s tri;
        int i;

        tri = new srfTriangles_s();

        tri.numVerts = numVerts;
        R_AllocStaticTriSurfVerts(tri, tri.numVerts);
        SIMDProcessor.Memcpy(tri.verts, verts, tri.numVerts);

        tri.numIndexes = numIndexes;
        R_AllocStaticTriSurfIndexes(tri, tri.numIndexes);

        // don't memcpy, so we can change the index type from int to short without changing the interface
        for (i = 0; i < tri.numIndexes; i++) {
            tri.indexes[i] = indexes[i];
        }

        R_RangeCheckIndexes(tri);
        R_CreateSilIndexes(tri);

// should we order the indexes here?
//	R_RemoveDuplicatedTriangles( &tri );
//	R_RemoveDegenerateTriangles( &tri );
//	R_RemoveUnusedVerts( &tri );
        R_IdentifySilEdges(tri, false);			// we cannot remove coplanar edges, because
        // they can deform to silhouettes

        R_DuplicateMirroredVertexes(tri);		// split mirror points into multiple points

        R_CreateDupVerts(tri);

        if (useUnsmoothedTangents) {
            R_BuildDominantTris(tri);
        }

        deform = new deformInfo_s();//R_ClearedStaticAlloc(sizeof(deform));

        deform.numSourceVerts = numVerts;
        deform.numOutputVerts = tri.numVerts;

        deform.numIndexes = numIndexes;
        deform.indexes = tri.indexes;

        deform.silIndexes = tri.silIndexes;

        deform.numSilEdges = tri.numSilEdges;
        deform.silEdges = tri.silEdges;

        deform.dominantTris = tri.dominantTris;

        deform.numMirroredVerts = tri.numMirroredVerts;
        deform.mirroredVerts = tri.mirroredVerts;

        deform.numDupVerts = tri.numDupVerts;
        deform.dupVerts = tri.dupVerts;

        if (tri.verts != null) {
//            triVertexAllocator.Free(tri.verts);
            tri.verts = null;
        }

        if (tri.facePlanes != null) {
//            triPlaneAllocator.Free(tri.facePlanes);
            tri.facePlanes = null;
        }

        return deform;
    }

    public static deformInfo_s R_BuildDeformInfo(int numVerts, final idDrawVert[] verts, int numIndexes, final idList<Integer> indexes, boolean useUnsmoothedTangents) {
        deformInfo_s deform;
        srfTriangles_s tri;
        int i;

        tri = new srfTriangles_s();//memset( &tri, 0, sizeof( tri ) );

        tri.numVerts = numVerts;
        R_AllocStaticTriSurfVerts(tri, tri.numVerts);
        SIMDProcessor.Memcpy(tri.verts, verts, tri.numVerts);

        tri.numIndexes = numIndexes;
        R_AllocStaticTriSurfIndexes(tri, tri.numIndexes);

        // don't memcpy, so we can change the index type from int to short without changing the interface
        for (i = 0; i < tri.numIndexes; i++) {
            tri.indexes[i] = indexes.oGet(i);
        }

        R_RangeCheckIndexes(tri);
        R_CreateSilIndexes(tri);

        // should we order the indexes here?
//	R_RemoveDuplicatedTriangles( &tri );
//	R_RemoveDegenerateTriangles( &tri );
//	R_RemoveUnusedVerts( &tri );
        R_IdentifySilEdges(tri, false);			// we cannot remove coplanar edges, because
        //                                              // they can deform to silhouettes

        R_DuplicateMirroredVertexes(tri);		// split mirror points into multiple points

        R_CreateDupVerts(tri);

        if (useUnsmoothedTangents) {
            R_BuildDominantTris(tri);
        }

        deform = new deformInfo_s();//deformInfo_t *)R_ClearedStaticAlloc( sizeof( *deform ) );

        deform.numSourceVerts = numVerts;
        deform.numOutputVerts = tri.numVerts;

        deform.numIndexes = numIndexes;
        deform.indexes = tri.indexes;

        deform.silIndexes = tri.silIndexes;

        deform.numSilEdges = tri.numSilEdges;
        deform.silEdges = tri.silEdges;

        deform.dominantTris = tri.dominantTris;

        deform.numMirroredVerts = tri.numMirroredVerts;
        deform.mirroredVerts = tri.mirroredVerts;

        deform.numDupVerts = tri.numDupVerts;
        deform.dupVerts = tri.dupVerts;

//	if ( tri.verts ) {
//		triVertexAllocator.Free( tri.verts );
//	}
//
//	if ( tri.facePlanes ) {
//		triPlaneAllocator.Free( tri.facePlanes );
//	}
        return deform;
    }

    /*
     ===================
     R_FreeDeformInfo
     ===================
     */
    public static void R_FreeDeformInfo(deformInfo_s deformInfo) {
//        if (deformInfo.indexes != null) {
//            triIndexAllocator.Free(deformInfo.indexes);
//        }
//        if (deformInfo.silIndexes != null) {
//            triSilIndexAllocator.Free(deformInfo.silIndexes);
//        }
//        if (deformInfo.silEdges != null) {
//            triSilEdgeAllocator.Free(deformInfo.silEdges);
//        }
//        if (deformInfo.dominantTris != null) {
//            triDominantTrisAllocator.Free(deformInfo.dominantTris);
//        }
//        if (deformInfo.mirroredVerts != null) {
//            triMirroredVertAllocator.Free(deformInfo.mirroredVerts);
//        }
//        if (deformInfo.dupVerts != null) {
//            triDupVertAllocator.Free(deformInfo.dupVerts);
//        }
//        R_StaticFree(deformInfo);
    }

    /*
     ===================
     R_DeformInfoMemoryUsed
     ===================
     */
    public static int R_DeformInfoMemoryUsed(deformInfo_s deformInfo) {
        int total = 0;

        if (deformInfo.indexes != null) {
            total += deformInfo.numIndexes;// * sizeof( deformInfo.indexes[0] );
        }
        if (deformInfo.silIndexes != null) {
            total += deformInfo.numIndexes;// * sizeof( deformInfo.silIndexes[0] );
        }
        if (deformInfo.silEdges != null) {
            total += deformInfo.numSilEdges;//* sizeof( deformInfo.silEdges[0] );
        }
        if (deformInfo.dominantTris != null) {
            total += deformInfo.numSourceVerts;//* sizeof( deformInfo.dominantTris[0] );
        }
        if (deformInfo.mirroredVerts != null) {
            total += deformInfo.numMirroredVerts;//* sizeof( deformInfo.mirroredVerts[0] );
        }
        if (deformInfo.dupVerts != null) {
            total += deformInfo.numDupVerts;// * sizeof( deformInfo.dupVerts[0] );
        }

        total += sizeof(deformInfo_s.class);
        return total;
    }

    private static idDrawVert[] Resize(idDrawVert[] verts, int totalVerts) {
        idDrawVert[] newVerts = new idDrawVert[totalVerts];

        System.arraycopy(verts, 0, newVerts, 0, verts.length);

        return newVerts;
    }

    private static shadowCache_s[] Resize(shadowCache_s[] shadowVertexes, int numVerts) {
        shadowCache_s[] newArray = new shadowCache_s[numVerts];
        System.arraycopy(shadowVertexes, 0, newArray, 0, Math.min(shadowVertexes.length, numVerts));
        return newArray;
    }

    private static int[] Resize(int[] indexes, int numIndexes) {
        if (indexes == null) {
            return new int[numIndexes];
        }

        if (numIndexes <= 0) {
            return null;
        }

        final int size = numIndexes > indexes.length ? indexes.length : numIndexes;
        int[] newIndexes = new int[numIndexes];

        System.arraycopy(indexes, 0, newIndexes, 0, size);

        return newIndexes;
    }
}
