package neo.Renderer;

import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import org.lwjgl.BufferUtils;

import neo.TempDump.SERiAL;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.VertexCache.vertCache_s;
import neo.Renderer.tr_local.viewDef_s;
import neo.framework.DemoFile.idDemoFile;
import neo.idlib.Lib.idException;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.JointTransform.idJointQuat;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Model {

    /*
     ===============================================================================

     Render Model

     ===============================================================================
     */
    // shared between the renderer, game, and Maya export DLL
    public static final String MD5_VERSION_STRING = "MD5Version";
    public static final String MD5_MESH_EXT = "md5mesh";
    public static final String MD5_ANIM_EXT = "md5anim";
    public static final String MD5_CAMERA_EXT = "md5camera";
    public static final int MD5_VERSION = 10;
    //
    // using shorts for triangle indexes can save a significant amount of traffic, but
    // to support the large models that renderBump loads, they need to be 32 bits
    static final int GL_INDEX_TYPE;

    static {
        if (true) {
            GL_INDEX_TYPE = GL_UNSIGNED_INT;
            //        } else {
            //            GL_INDEX_TYPE = GL_UNSIGNED_SHORT;
        }
    }

    static class silEdge_t {
        // NOTE: making this a glIndex is dubious, as there can be 2x the faces as verts

        int/*glIndex_t*/ p1, p2;		// planes defining the edge
        int/*glIndex_t*/ v1, v2;		// verts defining the edge

        public silEdge_t() {
        }        
        
        static silEdge_t[] generateArray(final int length) {
            return Stream.
                    generate(silEdge_t::new).
                    limit(length).
                    toArray(silEdge_t[]::new);
        }
    };

    // this is used for calculating unsmoothed normals and tangents for deformed models
    public static class dominantTri_s {

        public int/*glIndex_t*/ v2, v3;
        public final float[] normalizationScale = new float[3];
    };

    static class lightingCache_s {
        static final int BYTES = idVec3.BYTES;

        idVec3 localLightVector;		// this is the statically computed vector to the light
        // in texture space for cards without vertex programs

        lightingCache_s(ByteBuffer Position) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public static ByteBuffer toByteBuffer(lightingCache_s[] cache) {
            ByteBuffer data = BufferUtils.createByteBuffer(lightingCache_s.BYTES * cache.length);

            for (lightingCache_s c : cache) {
                data.put(c.localLightVector.Write());
            }

            return (ByteBuffer) data.flip();
        }
    };

    public static class shadowCache_s {

        public static final int BYTES = idVec4.BYTES;

        public idVec4 xyz;			// we use homogenous coordinate tricks

        public shadowCache_s() {
            xyz = new idVec4();
        }

        shadowCache_s(ByteBuffer Position) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public static ByteBuffer toByteBuffer(shadowCache_s[] cache) {
            ByteBuffer data = BufferUtils.createByteBuffer(shadowCache_s.BYTES * cache.length);

            for (shadowCache_s c : cache) {
                data.put(c.xyz.Write());
            }

            return (ByteBuffer) data.flip();
        }
    };
    static final int SHADOW_CAP_INFINITE = 64;

    // our only drawing geometry type
    public static class srfTriangles_s {

        public idBounds bounds;                 // for culling

        int     ambientViewCount;               // if == tr.viewCount, it is visible this view

        boolean generateNormals;                // create normals from geometry, instead of using explicit ones
        public boolean tangentsCalculated;      // set when the vertex tangents have been calculated
        boolean facePlanesCalculated;           // set when the face planes have been calculated
        boolean perfectHull;                    // true if there aren't any dangling edges
        boolean deformedSurface;                // if true, indexes, silIndexes, mirrorVerts, and silEdges are
                                                // pointers into the original surface, and should not be freed

        public int                 numVerts;    // number of vertices
        public idDrawVert[]        verts;       // vertices, allocated with special allocator

        public int                 numIndexes;  // for shadows, this has both front and rear end caps and silhouette planes
        public int /*glIndex_t*/[] indexes;     // indexes, allocated with special allocator

        public int/*glIndex_t*/[]  silIndexes;  // indexes changed to be the first vertex with same XYZ, ignoring normal and texcoords

        int         numMirroredVerts;           // this many verts at the end of the vert list are tangent mirrors
        int[]       mirroredVerts;              // tri->mirroredVerts[0] is the mirror of tri->numVerts - tri->numMirroredVerts + 0

        int         numDupVerts;                // number of duplicate vertexes
        int[]       dupVerts;                   // pairs of the number of the first vertex and the number of the duplicate vertex

        int         numSilEdges;                // number of silhouette edges
        silEdge_t[] silEdges;                   // silhouette edges

        public idPlane[] facePlanes;            // [numIndexes/3] plane equations

        dominantTri_s[] dominantTris;           // [numVerts] for deformed surface fast tangent calculation

        public int  numShadowIndexesNoFrontCaps;// shadow volumes with front caps omitted
        public int  numShadowIndexesNoCaps;     // shadow volumes with the front and rear caps omitted

        public int  shadowCapPlaneBits;         // bits 0-5 are set when that plane of the interacting light has triangles
                                                // projected on it, which means that if the view is on the outside of that
                                                // plane, we need to draw the rear caps of the shadow volume
                                                // turboShadows will have SHADOW_CAP_INFINITE

        public shadowCache_s[] shadowVertexes;  // these will be copied to shadowCache when it is going to be drawn.
                                                // these are NULL when vertex programs are available

        srfTriangles_s ambientSurface;          // for light interactions, point back at the original surface that generated
                                                // the interaction, which we will get the ambientCache from

        srfTriangles_s nextDeferredFree;        // chain of tris to free next frame

                                                // data in vertex object space, not directly readable by the CPU
        vertCache_s    indexCache;              // int
        vertCache_s    ambientCache;            // idDrawVert
        vertCache_s    lightingCache;           // lightingCache_t
        vertCache_s    shadowCache;             // shadowCache_t

        private static int DBG_counter = 0;
        public  final  int DBG_count = DBG_counter++;
        
        public srfTriangles_s() {
            this.bounds = new idBounds();
            this.ambientViewCount = 0;
            this.generateNormals = false;
            this.tangentsCalculated = false;
            this.facePlanesCalculated = false;
            this.perfectHull = false;
            this.deformedSurface = false;
            this.numVerts = 0;
            this.verts = null;
            this.numIndexes = 0;
            this.indexes = null;
            this.silIndexes = null;
            this.numMirroredVerts = 0;
            this.mirroredVerts = null;
            this.numDupVerts = 0;
            this.dupVerts = null;
            this.numSilEdges = 0;
            this.silEdges = null;
            this.facePlanes = null;
            this.dominantTris = null;
            this.numShadowIndexesNoFrontCaps = 0;
            this.numShadowIndexesNoCaps = 0;
            this.shadowCapPlaneBits = 0;
            this.shadowVertexes = null;
            this.ambientSurface = null;
            this.nextDeferredFree = null;
            this.indexCache = null;
            this.ambientCache = null;
            this.lightingCache = null;
            this.shadowCache = null;
        }
    };

    static class idTriList extends idList<srfTriangles_s> {
    };

    public static class modelSurface_s{

        public int            id;
        public idMaterial     shader;
        public srfTriangles_s geometry;

        private static int DBG_counter = 0;
        public final   int DBG_count   = DBG_counter++;
        
        public modelSurface_s(){
            int a = 1;
        }
    };

    public enum dynamicModel_t {

        DM_STATIC, // never creates a dynamic model
        DM_CACHED, // once created, stays constant until the entity is updated (animating characters)
        DM_CONTINUOUS	// must be recreated for every single view (time dependent things like particles)
    };
    //typedef enum {
    public static final int INVALID_JOINT = -1;
    //} jointHandle_t;

    public static class idMD5Joint {

        public idStr name;
        public idMD5Joint parent;

        public idMD5Joint() {
            parent = null;
        }
    };

    // the init methods may be called again on an already created model when
    // a reloadModels is issued
    public static abstract class idRenderModel implements SERiAL {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static int DBG_counter = 0;
        protected final int DBG_count = DBG_counter++;

        // public abstract						~idRenderModel() {};
        // Loads static models only, dynamic models must be loaded by the modelManager
        public abstract void InitFromFile(final String fileName) throws idException;

        // renderBump uses this to load the very high poly count models, skipping the
        // shadow and tangent generation, along with some surface cleanup to make it load faster
        public abstract void PartialInitFromFile(final String fileName);

        // this is used for dynamically created surfaces, which are assumed to not be reloadable.
        // It can be called again to clear out the surfaces of a dynamic model for regeneration.
        public abstract void InitEmpty(final String name);

        // dynamic model instantiations will be created with this
        // the geometry data will be owned by the model, and freed when it is freed
        // the geoemtry should be raw triangles, with no extra processing
        public abstract void AddSurface(modelSurface_s surface);

        // cleans all the geometry and performs cross-surface processing
        // like shadow hulls
        // Creates the duplicated back side geometry for two sided, alpha tested, lit materials
        // This does not need to be called if none of the surfaces added with AddSurface require
        // light interaction, and all the triangles are already well formed.
        public abstract void FinishSurfaces();

        // frees all the data, but leaves the class around for dangling references,
        // which can regenerate the data with LoadModel()
        public abstract void PurgeModel();

        // resets any model information that needs to be reset on a same level load etc.. 
        // currently only implemented for liquids
        public abstract void Reset();

        // used for initial loads, reloadModel, and reloading the data of purged models
        // Upon exit, the model will absolutely be valid, but possibly as a default model
        public abstract void LoadModel();

        // internal use
        public abstract boolean IsLoaded();

        public abstract void SetLevelLoadReferenced(boolean referenced);

        public abstract boolean IsLevelLoadReferenced();

        // models that are already loaded at level start time
        // will still touch their data to make sure they
        // are kept loaded
        public abstract void TouchData();

        // dump any ambient caches on the model surfaces
        public abstract void FreeVertexCache();

        // returns the name of the model
        public abstract String Name();

        // prints a detailed report on the model for printModel
        public abstract void Print();

        // prints a single line report for listModels
        public abstract void List();

        // reports the amount of memory (roughly) consumed by the model
        public abstract int Memory();

        // for reloadModels
        public abstract long[]/*ID_TIME_T*/ Timestamp();

        // returns the number of surfaces
        public abstract int NumSurfaces();

        // NumBaseSurfaces will not count any overlays added to dynamic models
        public abstract int NumBaseSurfaces();

        // get a pointer to a surface
        public abstract modelSurface_s Surface(int surfaceNum);

        // Allocates surface triangles.
        // Allocates memory for srfTriangles_t::verts and srfTriangles_t::indexes
        // The allocated memory is not initialized.
        // srfTriangles_t::numVerts and srfTriangles_t::numIndexes are set to zero.
        public abstract srfTriangles_s AllocSurfaceTriangles(int numVerts, int numIndexes);

        // Frees surfaces triangles.
        public abstract void FreeSurfaceTriangles(srfTriangles_s tris);

        // created at load time by stitching together all surfaces and sharing
        // the maximum number of edges.  This may be incorrect if a skin file
        // remaps surfaces between shadow casting and non-shadow casting, or
        // if some surfaces are noSelfShadow and others aren't
        public abstract srfTriangles_s ShadowHull();

        // models of the form "_area*" may have a prelight shadow model associated with it
        public abstract boolean IsStaticWorldModel();

        // models parsed from inside map files or dynamically created cannot be reloaded by
        // reloadmodels
        public abstract boolean IsReloadable();

        // md3, md5, particles, etc
        public abstract dynamicModel_t IsDynamicModel();

        // if the load failed for any reason, this will return true
        public abstract boolean IsDefaultModel();

        // dynamic models should return a fast, conservative approximation
        // static models should usually return the exact value
        public abstract idBounds Bounds(final renderEntity_s ent /*= NULL*/);

        public abstract idBounds Bounds();

        // returns value != 0.0f if the model requires the depth hack
        public abstract float DepthHack();

        // returns a static model based on the definition and view
        // currently, this will be regenerated for every view, even though
        // some models, like character meshes, could be used for multiple (mirror)
        // views in a frame, or may stay static for multiple frames (corpses)
        // The renderer will delete the returned dynamic model the next view
        // This isn't const, because it may need to reload a purged model if it
        // wasn't precached correctly.
        public abstract idRenderModel InstantiateDynamicModel(final renderEntity_s ent, final viewDef_s view, idRenderModel cachedModel);

        // Returns the number of joints or 0 if the model is not an MD5
        public abstract int NumJoints();

        // Returns the MD5 joints or NULL if the model is not an MD5
        public abstract idMD5Joint[] GetJoints();

        // Returns the handle for the joint with the given name.
        public abstract /*jointHandle_t*/ int GetJointHandle(final String name);

        // Returns the name for the joint with the given handle.
        public abstract String GetJointName(int jointHandle_t);

        // Returns the default animation pose or NULL if the model is not an MD5.
        public abstract idJointQuat[] GetDefaultPose();

        // Returns number of the joint nearest to the given triangle.
        public abstract int NearestJoint(int surfaceNum, int a, int c, int b);

        // Writing to and reading from a demo file.
        public abstract void ReadFromDemoFile(idDemoFile f);

        public abstract void WriteToDemoFile(idDemoFile f);

        public abstract void oSet(idRenderModel FindModel);
    };
}
