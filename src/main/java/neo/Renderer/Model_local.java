package neo.Renderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.stream.Stream;
import static neo.Renderer.Image_files.R_WriteTGA;
import static neo.Renderer.Material.deform_t.DFRM_NONE;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.Model.INVALID_JOINT;
import neo.Renderer.Model.dynamicModel_t;
import static neo.Renderer.Model.dynamicModel_t.DM_CACHED;
import static neo.Renderer.Model.dynamicModel_t.DM_CONTINUOUS;
import static neo.Renderer.Model.dynamicModel_t.DM_STATIC;
import neo.Renderer.Model.idMD5Joint;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import static neo.Renderer.Model_ase.ASE_Free;
import static neo.Renderer.Model_ase.ASE_Load;
import neo.Renderer.Model_ase.aseFace_t;
import neo.Renderer.Model_ase.aseMaterial_t;
import neo.Renderer.Model_ase.aseMesh_t;
import neo.Renderer.Model_ase.aseModel_s;
import neo.Renderer.Model_ase.aseObject_t;
import static neo.Renderer.Model_lwo.LWID_;
import static neo.Renderer.Model_lwo.lwGetObject;
import neo.Renderer.Model_lwo.lwLayer;
import neo.Renderer.Model_lwo.lwObject;
import neo.Renderer.Model_lwo.lwPoint;
import neo.Renderer.Model_lwo.lwPolygon;
import neo.Renderer.Model_lwo.lwSurface;
import neo.Renderer.Model_lwo.lwVMap;
import neo.Renderer.Model_lwo.lwVMapPt;
import static neo.Renderer.Model_ma.MA_Free;
import static neo.Renderer.Model_ma.MA_Load;
import neo.Renderer.Model_ma.maMaterial_t;
import neo.Renderer.Model_ma.maMesh_t;
import neo.Renderer.Model_ma.maModel_s;
import neo.Renderer.Model_ma.maObject_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.tr_local.demoCommand_t.DC_DEFINE_MODEL;
import static neo.Renderer.tr_local.tr;
import neo.Renderer.tr_local.viewDef_s;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import static neo.Renderer.tr_trisurf.R_CleanupTriangles;
import static neo.Renderer.tr_trisurf.R_CopyStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_MergeTriangles;
import static neo.Renderer.tr_trisurf.R_ReverseTriangles;
import static neo.Renderer.tr_trisurf.R_TriSurfMemory;
import static neo.TempDump.NOT;
import static neo.TempDump.ctos;
import static neo.TempDump.sizeof;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_RENDERER;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import neo.framework.DemoFile.idDemoFile;
import static neo.framework.FileSystem_h.fileSystem;
import neo.idlib.BV.Bounds.idBounds;
import static neo.idlib.Lib.BigFloat;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.VectorSet.idVectorSubset;
import neo.idlib.geometry.JointTransform.idJointQuat;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Simd.SIMDProcessor;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Model_local {

    /*
     ===============================================================================

     Static model

     ===============================================================================
     */
    public static class idRenderModelStatic extends idRenderModel {

        public final idList<modelSurface_s> surfaces;
        public       idBounds               bounds;
        public       int                    overlaysAdded;
        //
        //
        protected    int                    lastModifiedFrame;
        protected    int                    lastArchivedFrame;
        //
        protected    idStr                  name;
        protected    srfTriangles_s         shadowHull;
        protected    boolean                isStaticWorldModel;
        protected    boolean                defaulted;
        protected    boolean                purged;                // eventually we will have dynamic reloading
        protected    boolean                fastLoad;              // don't generate tangents and shadow data
        protected    boolean                reloadable;            // if not, reloadModels won't check timestamp
        protected    boolean                levelLoadReferenced;   // for determining if it needs to be freed
        protected final        long[]/*ID_TIME_T*/ timeStamp            = new long[1];
        //
        protected static final idCVar              r_mergeModelSurfaces = new idCVar("r_mergeModelSurfaces", "1", CVAR_BOOL | CVAR_RENDERER, "combine model surfaces with the same material");
        protected static final idCVar              r_slopVertex         = new idCVar("r_slopVertex", "0.01", CVAR_RENDERER, "merge xyz coordinates this far apart");
        protected static final idCVar              r_slopTexCoord       = new idCVar("r_slopTexCoord", "0.001", CVAR_RENDERER, "merge texture coordinates this far apart");
        protected static final idCVar              r_slopNormal         = new idCVar("r_slopNormal", "0.02", CVAR_RENDERER, "merge normals that dot less than this");
        //
        //

        // the inherited public interface
        public idRenderModelStatic() {
            this.surfaces = new idList<>(modelSurface_s.class);
            name = new idStr("<undefined>");
            (bounds = new idBounds()).Clear();
            lastModifiedFrame = 0;
            lastArchivedFrame = 0;
            overlaysAdded = 0;
            shadowHull = null;
            isStaticWorldModel = false;
            defaulted = false;
            purged = false;
            fastLoad = false;
            reloadable = true;
            levelLoadReferenced = false;
            timeStamp[0] = 0;
        }

        @Override
        public void InitFromFile(String fileName) throws idException {
            boolean loaded;
            idStr extension = new idStr();

            InitEmpty(fileName);

            // FIXME: load new .proc map format
            name.ExtractFileExtension(extension);

            if (extension.Icmp("ase") == 0) {
                loaded = LoadASE(name.toString());
                reloadable = true;
            } else if (extension.Icmp("lwo") == 0) {
                loaded = LoadLWO(name.toString());
                reloadable = true;
            } else if (extension.Icmp("flt") == 0) {
                loaded = LoadFLT(name.toString());
                reloadable = true;
            } else if (extension.Icmp("ma") == 0) {
                loaded = LoadMA(name.toString());
                reloadable = true;
            } else {
                common.Warning("idRenderModelStatic::InitFromFile: unknown type for model: \'%s\'", name);
                loaded = false;
            }

            if (!loaded) {
                common.Warning("Couldn't load model: '%s'", name);
                MakeDefaultModel();
                return;
            }

            // it is now available for use
            purged = false;

            // create the bounds for culling and dynamic surface creation
            FinishSurfaces();
        }

        @Override
        public void PartialInitFromFile(String fileName) {
            fastLoad = true;
            InitFromFile(fileName);
        }

        @Override
        public void PurgeModel() {
            int i;
            modelSurface_s surf;

            for (i = 0; i < surfaces.Num(); i++) {
                surf = surfaces.oGet(i);

                if (surf.geometry != null) {
                    R_FreeStaticTriSurf(surf.geometry);
                }
            }
            surfaces.Clear();

            purged = true;
        }

        @Override
        public void Reset() {
        }

        @Override
        public void LoadModel() {
            PurgeModel();
            InitFromFile(name.toString());
        }

        @Override
        public boolean IsLoaded() {
            return !purged;
        }

        @Override
        public void SetLevelLoadReferenced(boolean referenced) {
            levelLoadReferenced = referenced;
        }

        @Override
        public boolean IsLevelLoadReferenced() {
            return levelLoadReferenced;
        }

        @Override
        public void TouchData() {
            for (int i = 0; i < surfaces.Num(); i++) {
                modelSurface_s surf = surfaces.oGet(i);

                // re-find the material to make sure it gets added to the
                // level keep list
                declManager.FindMaterial(surf.shader.GetName());
            }
        }

        @Override
        public void InitEmpty(String fileName) {
            // model names of the form _area* are static parts of the
            // world, and have already been considered for optimized shadows
            // other model names are inline entity models, and need to be
            // shadowed normally
            if (0 == idStr.Cmpn(fileName, "_area", 5)) {
                isStaticWorldModel = true;
            } else {
                isStaticWorldModel = false;
            }

            name = new idStr(fileName);
            reloadable = false;	// if it didn't come from a file, we can't reload it
            PurgeModel();
            purged = false;
            bounds.Zero();
        }

        @Override
        public void AddSurface(modelSurface_s surface) {
            surfaces.AppendClone(surface);
            if (surface.geometry != null) {
                bounds.oPluSet(surface.geometry.bounds);
            }
        }

        /*
         ================
         idRenderModelStatic::FinishSurfaces

         The mergeShadows option allows surfaces with different textures to share
         silhouette edges for shadow calculation, instead of leaving shared edges
         hanging.

         If any of the original shaders have the noSelfShadow flag set, the surfaces
         can't be merged, because they will need to be drawn in different order.

         If there is only one surface, a separate merged surface won't be generated.

         A model with multiple surfaces can't later have a skinned shader change the
         state of the noSelfShadow flag.

         -----------------

         Creates mirrored copies of two sided surfaces with normal maps, which would
         otherwise light funny.

         Extends the bounds of deformed surfaces so they don't cull incorrectly at screen edges.

         ================
         */
        private static int DBG_FinishSurfaces = 0;
        @Override
        public void FinishSurfaces() {DBG_FinishSurfaces++;
            int i;
            int totalVerts, totalIndexes;

            purged = false;

            // make sure we don't have a huge bounds even if we don't finish everything
            bounds.Zero();

            if (surfaces.Num() == 0) {
                return;
            }

            // renderBump doesn't care about most of this
            if (fastLoad) {
                bounds.Zero();
                for (i = 0; i < surfaces.Num(); i++) {
                    modelSurface_s surf = surfaces.oGet(i);

                    R_BoundTriSurf(surf.geometry);
                    bounds.AddBounds(surf.geometry.bounds);
                }

                return;
            }

            // cleanup all the final surfaces, but don't create sil edges
            totalVerts = 0;
            totalIndexes = 0;

            // decide if we are going to merge all the surfaces into one shadower
            int numOriginalSurfaces = surfaces.Num();

            // make sure there aren't any NULL shaders or geometry
            for (i = 0; i < numOriginalSurfaces; i++) {
                modelSurface_s surf = surfaces.oGet(i);

                if (surf.geometry == null || surf.shader == null) {
                    MakeDefaultModel();
                    common.Error("Model %s, surface %d had NULL geometry", name, i);
                }
                if (surf.shader == null) {
                    MakeDefaultModel();
                    common.Error("Model %s, surface %d had NULL shader", name, i);
                }
            }

            // duplicate and reverse triangles for two sided bump mapped surfaces
            // note that this won't catch surfaces that have their shaders dynamically
            // changed, and won't work with animated models.
            // It is better to create completely separate surfaces, rather than
            // add vertexes and indexes to the existing surface, because the
            // tangent generation wouldn't like the acute shared edges
            for (i = 0; i < numOriginalSurfaces; i++) {
                modelSurface_s surf = surfaces.oGet(i);

                if (surf.shader.ShouldCreateBackSides()) {
                    srfTriangles_s newTri;

                    newTri = R_CopyStaticTriSurf(surf.geometry);
                    R_ReverseTriangles(newTri);

                    modelSurface_s newSurf = new modelSurface_s();

                    newSurf.shader = surf.shader;
                    newSurf.geometry = newTri;

                    AddSurface(newSurf);
                }
            }

            // clean the surfaces
            for (i = 0; i < surfaces.Num(); i++) {
                modelSurface_s surf = surfaces.oGet(i);

                R_CleanupTriangles(surf.geometry, surf.geometry.generateNormals, true, surf.shader.UseUnsmoothedTangents());
                if (surf.shader.SurfaceCastsShadow()) {
                    totalVerts += surf.geometry.numVerts;
                    totalIndexes += surf.geometry.numIndexes;
                }
            }

            // add up the total surface area for development information
            for (i = 0; i < surfaces.Num(); i++) {
                modelSurface_s surf = surfaces.oGet(i);
                srfTriangles_s tri = surf.geometry;

                for (int j = 0; j < tri.numIndexes; j += 3) {
                    float area = idWinding.TriangleArea(tri.verts[tri.indexes[j]].xyz,
                            tri.verts[tri.indexes[j + 1]].xyz, tri.verts[tri.indexes[j + 2]].xyz);
                    surf.shader.AddToSurfaceArea(area);
                }
            }

            // calculate the bounds
            if (surfaces.Num() == 0) {
                bounds.Zero();
            } else {
                bounds.Clear();
                for (i = 0; i < surfaces.Num(); i++) {
                    modelSurface_s surf = surfaces.oGet(i);

                    // if the surface has a deformation, increase the bounds
                    // the amount here is somewhat arbitrary, designed to handle
                    // autosprites and flares, but could be done better with exact
                    // deformation information.
                    // Note that this doesn't handle deformations that are skinned in
                    // at run time...
                    if (surf.shader.Deform() != DFRM_NONE) {
                        srfTriangles_s tri = surf.geometry;
                        idVec3 mid = (tri.bounds.oGet(1).oPlus(tri.bounds.oGet(0))).oMultiply(0.5f);
                        float radius = (tri.bounds.oGet(0).oMinus(mid)).Length();
                        radius += 20.0f;

                        tri.bounds.oSet(0, 0, mid.oGet(0) - radius);
                        tri.bounds.oSet(0, 1, mid.oGet(1) - radius);
                        tri.bounds.oSet(0, 2, mid.oGet(2) - radius);

                        tri.bounds.oSet(1, 0, mid.oGet(0) + radius);
                        tri.bounds.oSet(1, 1, mid.oGet(1) + radius);
                        tri.bounds.oSet(1, 2, mid.oGet(2) + radius);
                    }

                    // add to the model bounds
                    bounds.AddBounds(surf.geometry.bounds);
                }
            }
        }

        /*
         ==============
         idRenderModelStatic::FreeVertexCache

         We are about to restart the vertex cache, so dump everything
         ==============
         */
        @Override
        public void FreeVertexCache() {
            for (int j = 0; j < surfaces.Num(); j++) {
                srfTriangles_s tri = surfaces.oGet(j).geometry;
                if (null == tri) {
                    continue;
                }
                if (tri.ambientCache != null) {
                    vertexCache.Free(tri.ambientCache);
                    tri.ambientCache = null;
                }
                // static shadows may be present
                if (tri.shadowCache != null) {
                    vertexCache.Free(tri.shadowCache);
                    tri.shadowCache = null;
                }
            }
        }

        @Override
        public String Name() {
            return name.toString();
        }

        @Override
        public void Print() {
            int totalTris = 0;
            int totalVerts = 0;
            int totalBytes;// = 0;

            totalBytes = Memory();

            char closed = 'C';
            for (int j = 0; j < NumSurfaces(); j++) {
                final modelSurface_s surf = Surface(j);
                if (null == surf.geometry) {
                    continue;
                }
                if (!surf.geometry.perfectHull) {
                    closed = ' ';
                }
                totalTris += surf.geometry.numIndexes / 3;
                totalVerts += surf.geometry.numVerts;
            }
            common.Printf("%c%4dk %3d %4d %4d %s", closed, totalBytes / 1024, NumSurfaces(), totalVerts, totalTris, Name());

            if (IsDynamicModel() == DM_CACHED) {
                common.Printf(" (DM_CACHED)");
            }
            if (IsDynamicModel() == DM_CONTINUOUS) {
                common.Printf(" (DM_CONTINUOUS)");
            }
            if (defaulted) {
                common.Printf(" (DEFAULTED)");
            }
            if (bounds.oGet(0).oGet(0) >= bounds.oGet(1).oGet(0)) {
                common.Printf(" (EMPTY BOUNDS)");
            }
            if (bounds.oGet(1).oGet(0) - bounds.oGet(0).oGet(0) > 100000) {
                common.Printf(" (HUGE BOUNDS)");
            }

            common.Printf("\n");
        }

        @Override
        public void List() {
            int totalTris = 0;
            int totalVerts = 0;
            int totalBytes;//= 0;

            totalBytes = Memory();

            char closed = 'C';
            for (int j = 0; j < NumSurfaces(); j++) {
                final modelSurface_s surf = Surface(j);
                if (null == surf.geometry) {
                    continue;
                }
                if (!surf.geometry.perfectHull) {
                    closed = ' ';
                }
                totalTris += surf.geometry.numIndexes / 3;
                totalVerts += surf.geometry.numVerts;
            }
            common.Printf("%c%4dk %3d %4d %4d %s", closed, totalBytes / 1024, NumSurfaces(), totalVerts, totalTris, Name());

            if (IsDynamicModel() == DM_CACHED) {
                common.Printf(" (DM_CACHED)");
            }
            if (IsDynamicModel() == DM_CONTINUOUS) {
                common.Printf(" (DM_CONTINUOUS)");
            }
            if (defaulted) {
                common.Printf(" (DEFAULTED)");
            }
            if (bounds.oGet(0).oGet(0) >= bounds.oGet(1).oGet(0)) {
                common.Printf(" (EMPTY BOUNDS)");
            }
            if (bounds.oGet(1).oGet(0) - bounds.oGet(0).oGet(0) > 100000) {
                common.Printf(" (HUGE BOUNDS)");
            }

            common.Printf("\n");
        }

        @Override
        public int Memory() {
            int totalBytes = 0;

            totalBytes += sizeof(this);
            totalBytes += name.DynamicMemoryUsed();
            totalBytes += surfaces.MemoryUsed();

            if (shadowHull != null) {
                totalBytes += R_TriSurfMemory(shadowHull);
            }

            for (int j = 0; j < NumSurfaces(); j++) {
                final modelSurface_s surf = Surface(j);
                if (null == surf.geometry) {
                    continue;
                }
                totalBytes += R_TriSurfMemory(surf.geometry);
            }

            return totalBytes;
        }

        @Override
        public long[]/*ID_TIME_T*/ Timestamp() {
            return timeStamp;
        }

        @Override
        public int NumSurfaces() {
            return surfaces.Num();
        }

        @Override
        public int NumBaseSurfaces() {
            return surfaces.Num() - overlaysAdded;
        }

        @Override
        public modelSurface_s Surface(int surfaceNum) {
            return surfaces.oGet(surfaceNum);
        }

        @Override
        public srfTriangles_s AllocSurfaceTriangles(int numVerts, int numIndexes) {
            srfTriangles_s tri = R_AllocStaticTriSurf();
            R_AllocStaticTriSurfVerts(tri, numVerts);
            R_AllocStaticTriSurfIndexes(tri, numIndexes);
            return tri;
        }

        @Override
        public void FreeSurfaceTriangles(srfTriangles_s tris) {
            R_FreeStaticTriSurf(tris);
        }

        @Override
        public srfTriangles_s ShadowHull() {
            return shadowHull;
        }

        @Override
        public boolean IsStaticWorldModel() {
            return isStaticWorldModel;
        }

        @Override
        public boolean IsReloadable() {
            return reloadable;
        }

        @Override
        public dynamicModel_t IsDynamicModel() {
            // dynamic subclasses will override this
            return DM_STATIC;
        }

        @Override
        public boolean IsDefaultModel() {
            return defaulted;
        }

        @Override
        public idRenderModel InstantiateDynamicModel(renderEntity_s ent, viewDef_s view, idRenderModel cachedModel) {
            if (cachedModel != null) {
//		delete cachedModel;
//		cachedModel = NULL;
            }
            common.Error("InstantiateDynamicModel called on static model '%s'", name.toString());
            return null;
        }

        @Override
        public int NumJoints() {
            return 0;
        }

        @Override
        public idMD5Joint[] GetJoints() {
            return null;
        }

        @Override
        public int GetJointHandle(String name) {
            return INVALID_JOINT;
        }

        @Override
        public String GetJointName(int jointHandle_t) {
            return "";
        }

        @Override
        public idJointQuat[] GetDefaultPose() {
            return null;
        }

        @Override
        public int NearestJoint(int surfaceNum, int a, int c, int b) {
            return INVALID_JOINT;
        }

        @Override
        public idBounds Bounds(renderEntity_s ent) {
            return new idBounds(bounds.oGet(0), bounds.oGet(1));
        }

        @Override
        public idBounds Bounds() {
            return Bounds(null);
        }

        @Override
        public void ReadFromDemoFile(idDemoFile f) {
            PurgeModel();

            InitEmpty(f.ReadHashString());

            int i, j;
            int[] numSurfaces = new int[1];
            int[] index = new int[1];
            int[] vert = new int[1];
            f.ReadInt(numSurfaces);

            for (i = 0; i < numSurfaces[0]; i++) {
                modelSurface_s surf = new modelSurface_s();

                surf.shader = declManager.FindMaterial(f.ReadHashString());

                srfTriangles_s tri = R_AllocStaticTriSurf();

                f.ReadInt(index);
                tri.numIndexes = index[0];
                R_AllocStaticTriSurfIndexes(tri, tri.numIndexes);
                for (j = 0; j < tri.numIndexes; ++j) {
                    f.ReadInt(index);
                    tri.indexes[j] = index[0];
                }

                f.ReadInt(vert);
                tri.numVerts = vert[0];
                R_AllocStaticTriSurfVerts(tri, tri.numVerts);
                for (j = 0; j < tri.numVerts; ++j) {
                    char[][] color = new char[4][1];
                    f.ReadVec3(tri.verts[j].xyz);
                    f.ReadVec2(tri.verts[j].st);
                    f.ReadVec3(tri.verts[j].normal);
                    f.ReadVec3(tri.verts[j].tangents[0]);
                    f.ReadVec3(tri.verts[j].tangents[1]);
                    f.ReadUnsignedChar(color[0]);
                    tri.verts[j].color[0] = (short) color[0][0];
                    f.ReadUnsignedChar(color[0]);
                    tri.verts[j].color[1] = (short) color[1][0];
                    f.ReadUnsignedChar(color[0]);
                    tri.verts[j].color[2] = (short) color[2][0];
                    f.ReadUnsignedChar(color[0]);
                    tri.verts[j].color[3] = (short) color[3][0];
                }

                surf.geometry = tri;

                this.AddSurface(surf);
            }
            this.FinishSurfaces();
        }

        @Override
        public void WriteToDemoFile(idDemoFile f) {
//            int[] data = new int[1];

            // note that it has been updated
            lastArchivedFrame = tr.frameCount;

//            data = DC_DEFINE_MODEL.ordinal();//FIXME:WHY?
            f.WriteInt(DC_DEFINE_MODEL);
            f.WriteHashString(this.Name());

            int i, j, iData = surfaces.Num();
            f.WriteInt(iData);

            for (i = 0; i < surfaces.Num(); i++) {
                final modelSurface_s surf = surfaces.oGet(i);

                f.WriteHashString(surf.shader.GetName());

                srfTriangles_s tri = surf.geometry;
                f.WriteInt(tri.numIndexes);
                for (j = 0; j < tri.numIndexes; ++j) {
                    f.WriteInt(tri.indexes[j]);
                }
                f.WriteInt(tri.numVerts);
                for (j = 0; j < tri.numVerts; ++j) {
                    f.WriteVec3(tri.verts[j].xyz);
                    f.WriteVec2(tri.verts[j].st);
                    f.WriteVec3(tri.verts[j].normal);
                    f.WriteVec3(tri.verts[j].tangents[0]);
                    f.WriteVec3(tri.verts[j].tangents[1]);
                    f.WriteUnsignedChar((char) tri.verts[j].color[0]);
                    f.WriteUnsignedChar((char) tri.verts[j].color[1]);
                    f.WriteUnsignedChar((char) tri.verts[j].color[2]);
                    f.WriteUnsignedChar((char) tri.verts[j].color[3]);
                }
            }
        }

        @Override
        public float DepthHack() {
            return 0.0f;
        }

        public void MakeDefaultModel() {

            defaulted = true;

            // throw out any surfaces we already have
            PurgeModel();

            // create one new surface
            modelSurface_s surf = new modelSurface_s();

            srfTriangles_s tri = R_AllocStaticTriSurf();

            surf.shader = tr.defaultMaterial;
            surf.geometry = tri;

            R_AllocStaticTriSurfVerts(tri, 24);
            R_AllocStaticTriSurfIndexes(tri, 36);

            AddCubeFace(tri, new idVec3(-1, 1, 1), new idVec3(1, 1, 1), new idVec3(1, -1, 1), new idVec3(-1, -1, 1));
            AddCubeFace(tri, new idVec3(-1, 1, -1), new idVec3(-1, -1, -1), new idVec3(1, -1, -1), new idVec3(1, 1, -1));

            AddCubeFace(tri, new idVec3(1, -1, 1), new idVec3(1, 1, 1), new idVec3(1, 1, -1), new idVec3(1, -1, -1));
            AddCubeFace(tri, new idVec3(-1, -1, 1), new idVec3(-1, -1, -1), new idVec3(-1, 1, -1), new idVec3(-1, 1, 1));

            AddCubeFace(tri, new idVec3(-1, -1, 1), new idVec3(1, -1, 1), new idVec3(1, -1, -1), new idVec3(-1, -1, -1));
            AddCubeFace(tri, new idVec3(-1, 1, 1), new idVec3(-1, 1, -1), new idVec3(1, 1, -1), new idVec3(1, 1, 1));

            tri.generateNormals = true;

            AddSurface(surf);
            FinishSurfaces();
        }

        public boolean LoadASE(final String fileName) {
            aseModel_s ase;

            ase = ASE_Load(fileName);
            if (ase == null) {
                return false;
            }

            ConvertASEToModelSurfaces(ase);

            ASE_Free(ase);

            return true;
        }

        public boolean LoadLWO(String fileName) {
            int[] failID = {0};
            int[] failPos = {0};
            lwObject lwo;

            lwo = lwGetObject(fileName, failID, failPos);
            if (null == lwo) {
                return false;
            }

            ConvertLWOToModelSurfaces(lwo);
//
//            lwFreeObject(lwo);

            return true;
        }

        /*
         =================
         idRenderModelStatic::LoadFLT

         USGS height map data for megaTexture experiments
         =================
         */
        public boolean LoadFLT(final String fileName) {
            ByteBuffer[] buffer = {null};
            FloatBuffer data;
            int len;

            len = fileSystem.ReadFile(fileName, buffer);
            if (len <= 0) {
                return false;
            }
            int size = (int) Math.sqrt(len / 4.0f);
            data = buffer[0].asFloatBuffer();

            // bound the altitudes
            float min = 9999999;
            float max = -9999999;
            for (int i = 0; i < len / 4; i++) {
                data.put(i, BigFloat(data.get(i)));
                if (data.get(i) == -9999) {
                    data.put(i, 0);		// unscanned areas
                }

                if (data.get(i) < min) {
                    min = data.get(i);
                }
                if (data.get(i) > max) {
                    max = data.get(i);
                }
            }
            if (true) {
                // write out a gray scale height map
                ByteBuffer image = ByteBuffer.allocate(len);// R_StaticAlloc(len);
                int image_p = 0;
                for (int i = 0; i < len / 4; i++) {
                    float v = (data.get(i) - min) / (max - min);
                    image.putFloat(image_p, v * 255);
                    image.put(image_p + 3, (byte) 255);
                    image_p += 4;
                }
                idStr tgaName = new idStr(fileName);
                tgaName.StripFileExtension();
                tgaName.Append(".tga");
                R_WriteTGA(tgaName.toString(), image, size, size, false);
//                R_StaticFree(image);
//return false;
            }

            // find the island above sea level
            int minX, maxX, minY, maxY;
            {
                int i;
                for (minX = 0; minX < size; minX++) {
                    for (i = 0; i < size; i++) {
                        if (data.get(i * size + minX) > 1.0) {
                            break;
                        }
                    }
                    if (i != size) {
                        break;
                    }
                }

                for (maxX = size - 1; maxX > 0; maxX--) {
                    for (i = 0; i < size; i++) {
                        if (data.get(i * size + maxX) > 1.0) {
                            break;
                        }
                    }
                    if (i != size) {
                        break;
                    }
                }

                for (minY = 0; minY < size; minY++) {
                    for (i = 0; i < size; i++) {
                        if (data.get(minY * size + i) > 1.0) {
                            break;
                        }
                    }
                    if (i != size) {
                        break;
                    }
                }

                for (maxY = size - 1; maxY < size; maxY--) {
                    for (i = 0; i < size; i++) {
                        if (data.get(maxY * size + i) > 1.0) {
                            break;
                        }
                    }
                    if (i != size) {
                        break;
                    }
                }
            }

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;

//width /= 2;
            // allocate triangle surface
            srfTriangles_s tri = R_AllocStaticTriSurf();
            tri.numVerts = width * height;
            tri.numIndexes = (width - 1) * (height - 1) * 6;

            fastLoad = true;		// don't do all the sil processing

            R_AllocStaticTriSurfIndexes(tri, tri.numIndexes);
            R_AllocStaticTriSurfVerts(tri, tri.numVerts);

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int v = i * width + j;
                    tri.verts[ v].Clear();
                    tri.verts[ v].xyz.oSet(0, j * 10);	// each sample is 10 meters
                    tri.verts[ v].xyz.oSet(1, -i * 10);
                    tri.verts[ v].xyz.oSet(2, data.get((minY + i) * size + minX + j));	// height is in meters
                    tri.verts[ v].st.oSet(0, (float) j / (width - 1));
                    tri.verts[ v].st.oSet(1, 1.0f - ((float) i / (height - 1)));
                }
            }

            for (int i = 0; i < height - 1; i++) {
                for (int j = 0; j < width - 1; j++) {
                    int v = (i * (width - 1) + j) * 6;
//if (false){
//			tri.indexes[ v + 0 ] = i * width + j;
//			tri.indexes[ v + 1 ] = (i+1) * width + j;
//			tri.indexes[ v + 2 ] = (i+1) * width + j + 1;
//			tri.indexes[ v + 3 ] = i * width + j;
//			tri.indexes[ v + 4 ] = (i+1) * width + j + 1;
//			tri.indexes[ v + 5 ] = i * width + j + 1;
//}else
                    {
                        tri.indexes[ v + 0] = i * width + j;
                        tri.indexes[ v + 1] = i * width + j + 1;
                        tri.indexes[ v + 2] = (i + 1) * width + j + 1;
                        tri.indexes[ v + 3] = i * width + j;
                        tri.indexes[ v + 4] = (i + 1) * width + j + 1;
                        tri.indexes[ v + 5] = (i + 1) * width + j;
                    }
                }
            }

//            fileSystem.FreeFile(data);
            data = null;

            modelSurface_s surface = new modelSurface_s();

            surface.geometry = tri;
            surface.id = 0;
            surface.shader = tr.defaultMaterial; // declManager.FindMaterial( "shaderDemos/megaTexture" );

            this.AddSurface(surface);

            return true;
        }

        public boolean LoadMA(final String filename) {
            maModel_s ma;

            ma = MA_Load(filename);
            if (ma == null) {
                return false;
            }

            ConvertMAToModelSurfaces(ma);

            MA_Free(ma);

            return true;
        }

        @Override
        public void oSet(idRenderModel FindModel) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        static class matchVert_s {

            matchVert_s next;
            int v, tv;
            short[] color = new short[4];
            idVec3 normal = new idVec3();

            final int index;

            public matchVert_s(int numVerts) {
                this.index = numVerts;
            }

//            static int getPosition(matchVert_s v1, matchVert_s[] vList) {
//                int i;
//
//                for (i = 0; i < vList.length; i++) {
//                    if (vList[i].equals(v1)) {
//                        break;
//                    }
//                }
//
//                return i;
//            }


            @Override
            public int hashCode() {
                int result = v;
                result = 31 * result + tv;
                return result;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                final matchVert_s that = (matchVert_s) o;

                if (v != that.v) return false;
                return tv == that.tv;

            }

//            static matchVert_s[] generateArray(final int length) {
//                return Stream.
//                        generate(matchVert_s::new).
//                        limit(length).
//                        toArray(matchVert_s[]::new);
//            }
        };
        static final short[] identityColor/*[4]*/ = {255, 255, 255, 255};
                              private static int DBG_ConvertASEToModelSurfaces = 0;
        public boolean ConvertASEToModelSurfaces(final aseModel_s ase) {
            aseObject_t object;
            aseMesh_t mesh;
            aseMaterial_t material;
            idMaterial im1, im2;
            srfTriangles_s tri;
            int objectNum;
            int i, j, k;
            int v, tv;
            int[] vRemap;
            int[] tvRemap;
            matchVert_s[] mvTable;	// all of the match verts
            matchVert_s[] mvHash;	// points inside mvTable for each xyz index
            matchVert_s lastmv;
            matchVert_s mv;
            idVec3 normal = new idVec3();
            float uOffset, vOffset, textureSin, textureCos;
            float uTiling, vTiling;
            int[] mergeTo;
            short[] color;
            modelSurface_s surf = new modelSurface_s(), modelSurf;

            if (NOT(ase)) {
                return false;
            }
            if (ase.objects.Num() < 1) {
                return false;
            }

            timeStamp[0] = ase.timeStamp[0];

            // the modeling programs can save out multiple surfaces with a common
            // material, but we would like to mege them together where possible
            // meaning that this.NumSurfaces() <= ase.objects.currentElements
            mergeTo = new int[ase.objects.Num()];
            surf.geometry = null;
            if (ase.materials.Num() == 0) {
                // if we don't have any materials, dump everything into a single surface
                surf.shader = tr.defaultMaterial;
                surf.id = 0;
                this.AddSurface(surf);
                for (i = 0; i < ase.objects.Num(); i++) {
                    mergeTo[i] = 0;
                }
            } else if (!r_mergeModelSurfaces.GetBool()) {
                // don't merge any
                for (i = 0; i < ase.objects.Num(); i++) {
                    mergeTo[i] = i;
                    object = ase.objects.oGet(i);
                    material = ase.materials.oGet(object.materialRef);
                    surf.shader = declManager.FindMaterial(ctos(material.name));
                    surf.id = this.NumSurfaces();
                    this.AddSurface(surf);
                }
            } else {
                // search for material matches
                for (i = 0; i < ase.objects.Num(); i++) {
                    object = ase.objects.oGet(i);
                    material = ase.materials.oGet(object.materialRef);
                    im1 = declManager.FindMaterial(ctos(material.name));
                    if (im1.IsDiscrete()) {
                        // flares, autosprites, etc
                        j = this.NumSurfaces();
                    } else {
                        for (j = 0; j < this.NumSurfaces(); j++) {
                            modelSurf = this.surfaces.oGet(j);
                            im2 = modelSurf.shader;
                            if (im1 == im2) {
                                // merge this
                                mergeTo[i] = j;
                                break;
                            }
                        }
                    }
                    if (j == this.NumSurfaces()) {
                        // didn't merge
                        mergeTo[i] = j;
                        surf.shader = im1;
                        surf.id = this.NumSurfaces();
                        this.AddSurface(surf);
                    }
                }
            }

            idVectorSubset<idVec3> vertexSubset = new idVectorSubset<>(3);
            idVectorSubset<idVec2> texCoordSubset = new idVectorSubset<>(2);

            // build the surfaces
            for (objectNum = 0; objectNum < ase.objects.Num(); objectNum++) {
                object = ase.objects.oGet(objectNum);
                mesh = object.mesh;
                material = ase.materials.oGet(object.materialRef);
                im1 = declManager.FindMaterial(ctos(material.name));

                boolean normalsParsed = mesh.normalsParsed;

                // completely ignore any explict normals on surfaces with a renderbump command
                // which will guarantee the best contours and least vertexes.
                final String rb = im1.GetRenderBump();
                if (rb != null && !rb.isEmpty()) {
                    normalsParsed = false;
                }

                // It seems like the tools our artists are using often generate
                // verts and texcoords slightly separated that should be merged
                // note that we really should combine the surfaces with common materials
                // before doing this operation, because we can miss a slop combination
                // if they are in different surfaces
                vRemap = new int[mesh.numVertexes];// R_StaticAlloc(mesh.numVertexes /* sizeof( vRemap[0] ) */);

                if (fastLoad) {
                    // renderbump doesn't care about vertex count
                    for (j = 0; j < mesh.numVertexes; j++) {
                        vRemap[j] = j;
                    }
                } else {
                    float vertexEpsilon = r_slopVertex.GetFloat();
                    float expand = 2 * 32 * vertexEpsilon;
                    idVec3 mins = new idVec3(), maxs = new idVec3();

                    SIMDProcessor.MinMax(mins, maxs, mesh.vertexes, mesh.numVertexes);
                    mins.oMinSet(new idVec3(expand, expand, expand));
                    maxs.oPluSet(new idVec3(expand, expand, expand));
                    vertexSubset.Init(mins, maxs, 32, 1024);
                    for (j = 0; j < mesh.numVertexes; j++) {
                        vRemap[j] = vertexSubset.FindVector(mesh.vertexes, j, vertexEpsilon);
                    }
                }

                tvRemap = new int[mesh.numTVertexes];// R_StaticAlloc(mesh.numTVertexes /* sizeof( tvRemap[0] )*/);

                if (fastLoad) {
                    // renderbump doesn't care about vertex count
                    for (j = 0; j < mesh.numTVertexes; j++) {
                        tvRemap[j] = j;
                    }
                } else {
                    float texCoordEpsilon = r_slopTexCoord.GetFloat();
                    float expand = 2 * 32 * texCoordEpsilon;
                    idVec2 mins = new idVec2(), maxs = new idVec2();

                    SIMDProcessor.MinMax(mins, maxs, mesh.tvertexes, mesh.numTVertexes);
                    mins.oMinSet(new idVec2(expand, expand));
                    maxs.oPluSet(new idVec2(expand, expand));
                    texCoordSubset.Init(mins, maxs, 32, 1024);
                    for (j = 0; j < mesh.numTVertexes; j++) {
                        tvRemap[j] = texCoordSubset.FindVector(mesh.tvertexes, j, texCoordEpsilon);
                    }
                }

                // we need to find out how many unique vertex / texcoord combinations
                // there are, because ASE tracks them separately but we need them unified
                // the maximum possible number of combined vertexes is the number of indexes
                mvTable = new matchVert_s[mesh.numFaces * 3];

                // we will have a hash chain based on the xyz values
                mvHash = new matchVert_s[mesh.numVertexes];

                // allocate triangle surface
                tri = R_AllocStaticTriSurf();
                tri.numVerts = 0;
                tri.numIndexes = 0;
                R_AllocStaticTriSurfIndexes(tri, mesh.numFaces * 3);
                tri.generateNormals = !normalsParsed;

                // init default normal, color and tex coord index
                normal.Zero();
                color = identityColor;
                tv = 0;

                // find all the unique combinations
                float normalEpsilon = 1.0f - r_slopNormal.GetFloat();
                for (j = 0; j < mesh.numFaces; j++) {
                    for (k = 0; k < 3; k++) {
                        v = mesh.faces[j].vertexNum[k];

                        if (v < 0 || v >= mesh.numVertexes) {
                            common.Error("ConvertASEToModelSurfaces: bad vertex index in ASE file %s", name);
                        }

                        // collapse the position if it was slightly offset 
                        v = vRemap[v];

                        // we may or may not have texcoords to compare
                        if (mesh.numTVFaces == mesh.numFaces && mesh.numTVertexes != 0) {
                            tv = mesh.faces[j].tVertexNum[k];
                            if (tv < 0 || tv >= mesh.numTVertexes) {
                                common.Error("ConvertASEToModelSurfaces: bad tex coord index in ASE file %s", name);
                            }
                            // collapse the tex coord if it was slightly offset
                            tv = tvRemap[tv];
                        }

                        // we may or may not have normals to compare
                        if (normalsParsed) {
                            normal = mesh.faces[j].vertexNormals[k];
                        }

                        // we may or may not have colors to compare
                        if (mesh.colorsParsed) {
                            color[0] = mesh.faces[j].vertexColors[k][0];
                            color[1] = mesh.faces[j].vertexColors[k][1];
                            color[2] = mesh.faces[j].vertexColors[k][2];
                            color[3] = mesh.faces[j].vertexColors[k][3];
                        }

                        // find a matching vert
                        for (lastmv = null, mv = mvHash[v]; mv != null; lastmv = mv, mv = mv.next) {
                            if (mv.tv != tv) {
                                continue;
                            }
                            if (!Arrays.equals(mv.color, color)) {
                                continue;
                            }
                            if (!normalsParsed) {
                                // if we are going to create the normals, just
                                // matching texcoords is enough
                                break;
                            }
                            if (mv.normal.oMultiply(normal) > normalEpsilon) {
                                break;		// we already have this one
                            }
                        }
                        if (null == mv) {
                            // allocate a new match vert and link to hash chain
                            mv = mvTable[tri.numVerts] = new matchVert_s(tri.numVerts);
                            mv.v = v;
                            mv.tv = tv;
                            mv.normal.oSet(normal);
                            System.arraycopy(color, 0, mv.color, 0, color.length);
                            mv.next = null;
                            if (lastmv != null) {
                                lastmv.next = mv;
                            } else {
                                mvHash[v] = mv;
                            }
                            tri.numVerts++;
                        }

                        tri.indexes[tri.numIndexes] = mv.index;
                        tri.numIndexes++;
                    }
                }

                // allocate space for the indexes and copy them
                if (tri.numIndexes > mesh.numFaces * 3) {
                    common.FatalError("ConvertASEToModelSurfaces: index miscount in ASE file %s", name);
                }
                if (tri.numVerts > mesh.numFaces * 3) {
                    common.FatalError("ConvertASEToModelSurfaces: vertex miscount in ASE file %s", name);
                }

                // an ASE allows the texture coordinates to be scaled, translated, and rotated
                if (ase.materials.Num() == 0) {
                    uOffset = vOffset = 0.0f;
                    uTiling = vTiling = 1.0f;
                    textureSin = 0.0f;
                    textureCos = 1.0f;
                } else {
                    material = ase.materials.oGet(object.materialRef);
                    uOffset = -material.uOffset;
                    vOffset = material.vOffset;
                    uTiling = material.uTiling;
                    vTiling = material.vTiling;
                    textureSin = idMath.Sin(material.angle);
                    textureCos = idMath.Cos(material.angle);
                }

                // now allocate and generate the combined vertexes
                R_AllocStaticTriSurfVerts(tri, tri.numVerts);
                for (j = 0; j < tri.numVerts; j++) {
                    mv = mvTable[j];
                    tri.verts[ j].Clear();
                    tri.verts[ j].xyz.oSet(mesh.vertexes[ mv.v]);
                    tri.verts[ j].normal.oSet(mv.normal);
                    System.arraycopy(mv.color, 0, tri.verts[j].color = mv.color, 0, mv.color.length);
                    if (mesh.numTVFaces == mesh.numFaces && mesh.numTVertexes != 0) {
                        final idVec2 tv2 = mesh.tvertexes[ mv.tv];
                        float u = tv2.x * uTiling + uOffset;
                        float V = tv2.y * vTiling + vOffset;
                        tri.verts[ j].st.oSet(0, u * textureCos + V * textureSin);
                        tri.verts[ j].st.oSet(1, u * -textureSin + V * textureCos);
                    }
                }
//
//                R_StaticFree(mvTable);
//                R_StaticFree(mvHash);
//                R_StaticFree(tvRemap);
//                R_StaticFree(vRemap);

                // see if we need to merge with a previous surface of the same material
                modelSurf = this.surfaces.oGet(mergeTo[ objectNum]);
                srfTriangles_s mergeTri = modelSurf.geometry;
                if (null == mergeTri) {
                    modelSurf.geometry = tri;
                } else {
                    modelSurf.geometry = R_MergeTriangles(mergeTri, tri);
                    R_FreeStaticTriSurf(tri);
                    R_FreeStaticTriSurf(mergeTri);
                }
            }

            return true;
        }

        private static int DBG_ConvertLWOToModelSurfaces = 0;
        public boolean ConvertLWOToModelSurfaces(final lwObject lwo) {DBG_ConvertLWOToModelSurfaces++;
            idMaterial im1, im2;
            srfTriangles_s tri;
            lwSurface lwoSurf;
            int numTVertexes;
            int i, j, k;
            int v, tv;
            idVec3[] vList;
            int[] vRemap;
            idVec2[] tvList;
            int[] tvRemap;
            matchVert_s[] mvTable;	// all of the match verts
            matchVert_s[] mvHash;		// points inside mvTable for each xyz index
            matchVert_s lastmv;
            matchVert_s mv;
            idVec3 normal = new idVec3();
            int[] mergeTo;
            short[] color = new short[4];
            modelSurface_s surf, modelSurf;

            if (NOT(lwo)) {
                return false;
            }
            if (lwo.surf == null) {
                return false;
            }

            timeStamp[0] = lwo.timeStamp[0];

            // count the number of surfaces
            i = 0;
            for (lwoSurf = lwo.surf; lwoSurf != null; lwoSurf = lwoSurf.next) {
                i++;
            }

            // the modeling programs can save out multiple surfaces with a common
            // material, but we would like to merge them together where possible
            mergeTo = new int[i];
//	memset( &surf, 0, sizeof( surf ) );

            if (!r_mergeModelSurfaces.GetBool()) {
                // don't merge any
                for (lwoSurf = lwo.surf, i = 0; lwoSurf != null; lwoSurf = lwoSurf.next, i++) {
                    surf = new modelSurface_s();
                    mergeTo[i] = i;
                    surf.shader = declManager.FindMaterial(lwoSurf.name);
                    surf.id = this.NumSurfaces();
                    this.AddSurface(surf);
                }
            } else {
                // search for material matches
                for (lwoSurf = lwo.surf, i = 0; lwoSurf != null; lwoSurf = lwoSurf.next, i++) {
                    surf = new modelSurface_s();
                    im1 = declManager.FindMaterial(lwoSurf.name);
                    if (im1.IsDiscrete()) {
                        // flares, autosprites, etc
                        j = this.NumSurfaces();
                    } else {
                        for (j = 0; j < this.NumSurfaces(); j++) {
                            modelSurf = this.surfaces.oGet(j);
                            im2 = modelSurf.shader;
                            if (im1 == im2) {
                                // merge this
                                mergeTo[i] = j;
                                break;
                            }
                        }
                    }
                    if (j == this.NumSurfaces()) {
                        // didn't merge
                        mergeTo[i] = j;
                        surf.shader = im1;
                        surf.id = this.NumSurfaces();
                        this.AddSurface(surf);
                    }
                }
            }

            idVectorSubset<idVec3> vertexSubset = new idVectorSubset<>(3);
            idVectorSubset<idVec2> texCoordSubset = new idVectorSubset<>(2);

            // we only ever use the first layer
            lwLayer layer = lwo.layer;

            // vertex positions
            if (layer.point.count <= 0) {
                common.Warning("ConvertLWOToModelSurfaces: model \'%s\' has bad or missing vertex data", name);
                return false;
            }

            vList = new idVec3[layer.point.count];// R_StaticAlloc(layer.point.count /* sizeof( vList[0] ) */);
            for (j = 0; j < layer.point.count; j++) {
                vList[j] = new idVec3(
                        layer.point.pt[j].pos[0],
                        layer.point.pt[j].pos[2],
                        layer.point.pt[j].pos[1]);
            }

            // vertex texture coords
            numTVertexes = 0;

            if (layer.nvmaps != 0) {
                for (lwVMap vm = layer.vmap; vm != null; vm = vm.next) {
                    if (vm.type == LWID_('T', 'X', 'U', 'V')) {
                        numTVertexes += vm.nverts;
                    }
                }
            }

            if (numTVertexes != 0) {
                tvList = idVec2.generateArray(numTVertexes);
                int offset = 0;
                for (lwVMap vm = layer.vmap; vm != null; vm = vm.next) {
                    if (vm.type == LWID_('T', 'X', 'U', 'V')) {
                        vm.offset = offset;
                        for (k = 0; k < vm.nverts; k++) {
                            tvList[k + offset].x = vm.val[k][0];
                            tvList[k + offset].y = 1.0f - vm.val[k][1];	// invert the t
                        }
                        offset += vm.nverts;
                    }
                }
            } else {
                common.Warning("ConvertLWOToModelSurfaces: model \'%s\' has bad or missing uv data", name);
                numTVertexes = 1;
                tvList = new idVec2[numTVertexes];// Mem_ClearedAlloc(numTVertexes /* sizeof( tvList[0] )*/);
                tvList[0] = new idVec2();
            }

            // It seems like the tools our artists are using often generate
            // verts and texcoords slightly separated that should be merged
            // note that we really should combine the surfaces with common materials
            // before doing this operation, because we can miss a slop combination
            // if they are in different surfaces
            vRemap = new int[layer.point.count];// R_StaticAlloc(layer.point.count /* sizeof( vRemap[0] )*/);

            if (fastLoad) {
                // renderbump doesn't care about vertex count
                for (j = 0; j < layer.point.count; j++) {
                    vRemap[j] = j;
                }
            } else {
                float vertexEpsilon = r_slopVertex.GetFloat();
                float expand = 2 * 32 * vertexEpsilon;
                idVec3 mins = new idVec3(), maxs = new idVec3();

                SIMDProcessor.MinMax(mins, maxs, vList, layer.point.count);
                mins.oMinSet(new idVec3(expand, expand, expand));
                maxs.oPluSet(new idVec3(expand, expand, expand));
                vertexSubset.Init(mins, maxs, 32, 1024);
                for (j = 0; j < layer.point.count; j++) {
                    vRemap[j] = vertexSubset.FindVector(vList, j, vertexEpsilon);
                }
            }

            tvRemap = new int[numTVertexes];// R_StaticAlloc(numTVertexes /* sizeof( tvRemap[0] )*/);

            if (fastLoad) {
                // renderbump doesn't care about vertex count
                for (j = 0; j < numTVertexes; j++) {
                    tvRemap[j] = j;
                }
            } else {
                float texCoordEpsilon = r_slopTexCoord.GetFloat();
                float expand = 2 * 32 * texCoordEpsilon;
                idVec2 mins = new idVec2(), maxs = new idVec2();

                SIMDProcessor.MinMax(mins, maxs, tvList, numTVertexes);
                mins.oMinSet(new idVec2(expand, expand));
                maxs.oPluSet(new idVec2(expand, expand));
                texCoordSubset.Init(mins, maxs, 32, 1024);
                for (j = 0; j < numTVertexes; j++) {
                    tvRemap[j] = texCoordSubset.FindVector(tvList, j, texCoordEpsilon);
                }
            }

            // build the surfaces
            for (lwoSurf = lwo.surf, i = 0; lwoSurf != null; lwoSurf = lwoSurf.next, i++) {
                im1 = declManager.FindMaterial(lwoSurf.name);

                boolean normalsParsed = true;

                // completely ignore any explict normals on surfaces with a renderbump command
                // which will guarantee the best contours and least vertexes.
                final String rb = im1.GetRenderBump();
                if (rb != null && !rb.isEmpty()) {
                    normalsParsed = false;
                }

                // we need to find out how many unique vertex / texcoord combinations there are
                // the maximum possible number of combined vertexes is the number of indexes
                mvTable = new matchVert_s[layer.polygon.count * 3];

                // we will have a hash chain based on the xyz values
                mvHash = new matchVert_s[layer.point.count];// R_ClearedStaticAlloc(layer.point.count, matchVert_s.class/* sizeof( mvHash[0] ) */);

                // allocate triangle surface
                tri = R_AllocStaticTriSurf();
                tri.numVerts = 0;
                tri.numIndexes = 0;
                R_AllocStaticTriSurfIndexes(tri, layer.polygon.count * 3);
                tri.generateNormals = !normalsParsed;

                // find all the unique combinations
                float normalEpsilon;
                if (fastLoad) {
                    normalEpsilon = 1.0f;	// don't merge unless completely exact
                } else {
                    normalEpsilon = 1.0f - r_slopNormal.GetFloat();
                }
                for (j = 0; j < layer.polygon.count; j++) {
                    lwPolygon poly = layer.polygon.pol[j];

                    if (!poly.surf.equals(lwoSurf)) {
                        continue;
                    }

                    if (poly.nverts != 3) {
                        common.Warning("ConvertLWOToModelSurfaces: model %s has too many verts for a poly! Make sure you triplet it down", name);
                        continue;
                    }

                    for (k = 0; k < 3; k++) {

                        v = vRemap[poly.getV(k).index];

                        normal.x = poly.getV(k).norm[0];
                        normal.y = poly.getV(k).norm[2];
                        normal.z = poly.getV(k).norm[1];

                        // LWO models aren't all that pretty when it comes down to the floating point values they store
                        normal.FixDegenerateNormal();

                        tv = 0;

                        color[0] = (short) (lwoSurf.color.rgb[0] * 255);
                        color[1] = (short) (lwoSurf.color.rgb[1] * 255);
                        color[2] = (short) (lwoSurf.color.rgb[2] * 255);
                        color[3] = 255;

                        // first set attributes from the vertex
                        lwPoint pt = layer.point.pt[poly.getV(k).index];
                        int nvm;
                        for (nvm = 0; nvm < pt.nvmaps; nvm++) {
                            lwVMapPt vm = pt.vm[nvm];

                            if (vm.vmap.type == LWID_('T', 'X', 'U', 'V')) {
                                tv = tvRemap[vm.index + vm.vmap.offset];
                            }
                            if (vm.vmap.type == LWID_('R', 'G', 'B', 'A')) {
                                for (int chan = 0; chan < 4; chan++) {
                                    color[chan] = (short) (255 * vm.vmap.val[vm.index][chan]);
                                }
                            }
                        }

                        // then override with polygon attributes
                        for (nvm = 0; nvm < poly.getV(k).nvmaps; nvm++) {
                            lwVMapPt vm = poly.getV(k).vm[nvm];

                            if (vm.vmap.type == LWID_('T', 'X', 'U', 'V')) {
                                tv = tvRemap[vm.index + vm.vmap.offset];
                            }
                            if (vm.vmap.type == LWID_('R', 'G', 'B', 'A')) {
                                for (int chan = 0; chan < 4; chan++) {
                                    color[chan] = (short) (255 * vm.vmap.val[vm.index][chan]);
                                }
                            }
                        }

                        // find a matching vert
                        for (lastmv = null, mv = mvHash[v]; mv != null; lastmv = mv, mv = mv.next) {
                            if (mv.tv != tv) {
                                continue;
                            }
                            if (!Arrays.equals(mv.color, color)) {
                                continue;
                            }
                            if (!normalsParsed) {
                                // if we are going to create the normals, just
                                // matching texcoords is enough
                                break;
                            }
                            if (mv.normal.oMultiply(normal) > normalEpsilon) {
                                break;		// we already have this one
                            }
                        }
                        if (null == mv) {
                            // allocate a new match vert and link to hash chain
                            mv = mvTable[tri.numVerts] = new matchVert_s(tri.numVerts);
                            mv.v = v;
                            mv.tv = tv;
                            mv.normal.oSet(normal);
                            System.arraycopy(color, 0, mv.color, 0, color.length);
                            mv.next = null;
                            if (lastmv != null) {
                                lastmv.next = mv;
                            } else {
                                mvHash[v] = mv;
                            }
                            tri.numVerts++;
                        }

                        tri.indexes[tri.numIndexes] = mv.index;
                        tri.numIndexes++;
                    }
                }

                // allocate space for the indexes and copy them
                if (tri.numIndexes > layer.polygon.count * 3) {
                    common.FatalError("ConvertLWOToModelSurfaces: index miscount in LWO file %s", name);
                }
                if (tri.numVerts > layer.polygon.count * 3) {
                    common.FatalError("ConvertLWOToModelSurfaces: vertex miscount in LWO file %s", name);
                }

                // now allocate and generate the combined vertexes
                R_AllocStaticTriSurfVerts(tri, tri.numVerts);
                for (j = 0; j < tri.numVerts; j++) {
                    mv = mvTable[j];
                    tri.verts[j].Clear();
                    tri.verts[j].xyz = vList[mv.v];
                    tri.verts[j].st = tvList[mv.tv];
                    tri.verts[j].normal = mv.normal;
                    tri.verts[j].color = mv.color;
                }
//
//                R_StaticFree(mvTable);
//                R_StaticFree(mvHash);

                // see if we need to merge with a previous surface of the same material
                modelSurf = this.surfaces.oGet(mergeTo[ i]);
                srfTriangles_s mergeTri = modelSurf.geometry;
                if (null == mergeTri) {
                    modelSurf.geometry = tri;
                } else {
                    modelSurf.geometry = R_MergeTriangles(mergeTri, tri);
                    R_FreeStaticTriSurf(tri);
                    R_FreeStaticTriSurf(mergeTri);
                }
            }
//
//            R_StaticFree(tvRemap);
//            R_StaticFree(vRemap);
//            R_StaticFree(tvList);
//            R_StaticFree(vList);

            return true;
        }
//	static short []identityColor/*[4]*/ = { 255, 255, 255, 255 };

        public boolean ConvertMAToModelSurfaces(final maModel_s ma) {

            maObject_t object;
            maMesh_t mesh;
            maMaterial_t material;

            idMaterial im1, im2;
            srfTriangles_s tri;
            int objectNum;
            int i, j, k;
            int v, tv;
            int[] vRemap;
            int[] tvRemap;
            matchVert_s[] mvTable;	// all of the match verts
            matchVert_s[] mvHash;		// points inside mvTable for each xyz index
            matchVert_s lastmv;
            matchVert_s mv;
            idVec3 normal = new idVec3();
            float uOffset, vOffset, textureSin, textureCos;
            float uTiling, vTiling;
            int[] mergeTo;
            short[] color;
            modelSurface_s surf = new modelSurface_s(), modelSurf;

            if (NOT(ma)) {
                return false;
            }
            if (ma.objects.Num() < 1) {
                return false;
            }

            timeStamp[0] = ma.timeStamp[0];

            // the modeling programs can save out multiple surfaces with a common
            // material, but we would like to mege them together where possible
            // meaning that this.NumSurfaces() <= ma.objects.currentElements
            mergeTo = new int[ma.objects.Num()];

            surf.geometry = null;
            if (ma.materials.Num() == 0) {
                // if we don't have any materials, dump everything into a single surface
                surf.shader = tr.defaultMaterial;
                surf.id = 0;
                this.AddSurface(surf);
                for (i = 0; i < ma.objects.Num(); i++) {
                    mergeTo[i] = 0;
                }
            } else if (!r_mergeModelSurfaces.GetBool()) {
                // don't merge any
                for (i = 0; i < ma.objects.Num(); i++) {
                    mergeTo[i] = i;
                    object = ma.objects.oGet(i);
                    if (object.materialRef >= 0) {
                        material = ma.materials.oGet(object.materialRef);
                        surf.shader = declManager.FindMaterial(material.name);
                    } else {
                        surf.shader = tr.defaultMaterial;
                    }
                    surf.id = this.NumSurfaces();
                    this.AddSurface(surf);
                }
            } else {
                // search for material matches
                for (i = 0; i < ma.objects.Num(); i++) {
                    object = ma.objects.oGet(i);
                    if (object.materialRef >= 0) {
                        material = ma.materials.oGet(object.materialRef);
                        im1 = declManager.FindMaterial(material.name);
                    } else {
                        im1 = tr.defaultMaterial;
                    }
                    if (im1.IsDiscrete()) {
                        // flares, autosprites, etc
                        j = this.NumSurfaces();
                    } else {
                        for (j = 0; j < this.NumSurfaces(); j++) {
                            modelSurf = this.surfaces.oGet(j);
                            im2 = modelSurf.shader;
                            if (im1 == im2) {
                                // merge this
                                mergeTo[i] = j;
                                break;
                            }
                        }
                    }
                    if (j == this.NumSurfaces()) {
                        // didn't merge
                        mergeTo[i] = j;
                        surf.shader = im1;
                        surf.id = this.NumSurfaces();
                        this.AddSurface(surf);
                    }
                }
            }

            idVectorSubset<idVec3> vertexSubset = new idVectorSubset<>(3);
            idVectorSubset<idVec2> texCoordSubset = new idVectorSubset<>(3);

            // build the surfaces
            for (objectNum = 0; objectNum < ma.objects.Num(); objectNum++) {
                object = ma.objects.oGet(objectNum);
                mesh = object.mesh;
                if (object.materialRef >= 0) {
                    material = ma.materials.oGet(object.materialRef);
                    im1 = declManager.FindMaterial(material.name);
                } else {
                    im1 = tr.defaultMaterial;
                }

                boolean normalsParsed = mesh.normalsParsed;

                // completely ignore any explict normals on surfaces with a renderbump command
                // which will guarantee the best contours and least vertexes.
                final String rb = im1.GetRenderBump();
                if (rb != null && !rb.isEmpty()) {
                    normalsParsed = false;
                }

                // It seems like the tools our artists are using often generate
                // verts and texcoords slightly separated that should be merged
                // note that we really should combine the surfaces with common materials
                // before doing this operation, because we can miss a slop combination
                // if they are in different surfaces
                vRemap = new int[mesh.numVertexes];// R_StaticAlloc(mesh.numVertexes /* sizeof( vRemap[0] )*/);

                if (fastLoad) {
                    // renderbump doesn't care about vertex count
                    for (j = 0; j < mesh.numVertexes; j++) {
                        vRemap[j] = j;
                    }
                } else {
                    float vertexEpsilon = r_slopVertex.GetFloat();
                    float expand = 2 * 32 * vertexEpsilon;
                    idVec3 mins = new idVec3(), maxs = new idVec3();

                    SIMDProcessor.MinMax(mins, maxs, mesh.vertexes, mesh.numVertexes);
                    mins.oMinSet(new idVec3(expand, expand, expand));
                    maxs.oPluSet(new idVec3(expand, expand, expand));
                    vertexSubset.Init(mins, maxs, 32, 1024);
                    for (j = 0; j < mesh.numVertexes; j++) {
                        vRemap[j] = vertexSubset.FindVector(mesh.vertexes, j, vertexEpsilon);
                    }
                }

                tvRemap = new int[mesh.numTVertexes];// R_StaticAlloc(mesh.numTVertexes /* sizeof( tvRemap[0] ) */);

                if (fastLoad) {
                    // renderbump doesn't care about vertex count
                    for (j = 0; j < mesh.numTVertexes; j++) {
                        tvRemap[j] = j;
                    }
                } else {
                    float texCoordEpsilon = r_slopTexCoord.GetFloat();
                    float expand = 2 * 32 * texCoordEpsilon;
                    idVec2 mins = new idVec2(), maxs = new idVec2();

                    SIMDProcessor.MinMax(mins, maxs, mesh.tvertexes, mesh.numTVertexes);
                    mins.oMinSet(new idVec2(expand, expand));
                    maxs.oPluSet(new idVec2(expand, expand));
                    texCoordSubset.Init(mins, maxs, 32, 1024);
                    for (j = 0; j < mesh.numTVertexes; j++) {
                        tvRemap[j] = texCoordSubset.FindVector(mesh.tvertexes, j, texCoordEpsilon);
                    }
                }

                // we need to find out how many unique vertex / texcoord / color combinations
                // there are, because MA tracks them separately but we need them unified
                // the maximum possible number of combined vertexes is the number of indexes
                mvTable = new matchVert_s[mesh.numFaces * 3];// R_ClearedStaticAlloc(mesh.numFaces * 3 /* sizeof( mvTable[0] )*/);

                // we will have a hash chain based on the xyz values
                mvHash = new matchVert_s[mesh.numFaces];// R_ClearedStaticAlloc(mesh.numVertexes /* sizeof( mvHash[0] )*/);

                // allocate triangle surface
                tri = R_AllocStaticTriSurf();
                tri.numVerts = 0;
                tri.numIndexes = 0;
                R_AllocStaticTriSurfIndexes(tri, mesh.numFaces * 3);
                tri.generateNormals = !normalsParsed;

                // init default normal, color and tex coord index
                normal.Zero();
                color = identityColor;
                tv = 0;

                // find all the unique combinations
                float normalEpsilon = 1.0f - r_slopNormal.GetFloat();
                for (j = 0; j < mesh.numFaces; j++) {
                    for (k = 0; k < 3; k++) {
                        v = mesh.faces[j].vertexNum[k];

                        if (v < 0 || v >= mesh.numVertexes) {
                            common.Error("ConvertMAToModelSurfaces: bad vertex index in MA file %s", name);
                        }

                        // collapse the position if it was slightly offset 
                        v = vRemap[v];

                        // we may or may not have texcoords to compare
                        if (mesh.numTVertexes != 0) {
                            tv = mesh.faces[j].tVertexNum[k];
                            if (tv < 0 || tv >= mesh.numTVertexes) {
                                common.Error("ConvertMAToModelSurfaces: bad tex coord index in MA file %s", name);
                            }
                            // collapse the tex coord if it was slightly offset
                            tv = tvRemap[tv];
                        }

                        // we may or may not have normals to compare
                        if (normalsParsed) {
                            normal = mesh.faces[j].vertexNormals[k];
                        }

                        //BSM: Todo: Fix the vertex colors
                        // we may or may not have colors to compare
                        if (mesh.faces[j].vertexColors[k] != -1 && mesh.faces[j].vertexColors[k] != -999) {
                            final int offset = mesh.faces[j].vertexColors[k] * 4;
                            color = Arrays.copyOfRange(mesh.colors, offset, offset + 4);
                        }

                        // find a matching vert
                        for (lastmv = null, mv = mvHash[v]; mv != null; lastmv = mv, mv = mv.next) {
                            if (mv.tv != tv) {
                                continue;
                            }
                            if (!Arrays.equals(mv.color, color)) {
                                continue;
                            }
                            if (!normalsParsed) {
                                // if we are going to create the normals, just
                                // matching texcoords is enough
                                break;
                            }
                            if (mv.normal.oMultiply(normal) > normalEpsilon) {
                                break;		// we already have this one
                            }
                        }
                        if (null == mv) {
                            // allocate a new match vert and link to hash chain
                            mv = mvTable[tri.numVerts] = new matchVert_s(tri.numVerts);
                            mv.v = v;
                            mv.tv = tv;
                            mv.normal.oSet(normal);
                            System.arraycopy(color, 0, mv.color, 0, color.length);
                            mv.next = null;
                            if (lastmv != null) {
                                lastmv.next = mv;
                            } else {
                                mvHash[v] = mv;
                            }
                            tri.numVerts++;
                        }

                        tri.indexes[tri.numIndexes] = mv.index;
                        tri.numIndexes++;
                    }
                }

                // allocate space for the indexes and copy them
                if (tri.numIndexes > mesh.numFaces * 3) {
                    common.FatalError("ConvertMAToModelSurfaces: index miscount in MA file %s", name);
                }
                if (tri.numVerts > mesh.numFaces * 3) {
                    common.FatalError("ConvertMAToModelSurfaces: vertex miscount in MA file %s", name);
                }

                // an MA allows the texture coordinates to be scaled, translated, and rotated
                //BSM: Todo: Does Maya support this and if so how
                //if ( ase.materials.Num() == 0 ) {
                uOffset = vOffset = 0.0f;
                uTiling = vTiling = 1.0f;
                textureSin = 0.0f;
                textureCos = 1.0f;
                //} else {
                //	material = ase.materials[object.materialRef];
                //	uOffset = -material.uOffset;
                //	vOffset = material.vOffset;
                //	uTiling = material.uTiling;
                //	vTiling = material.vTiling;
                //	textureSin = idMath::Sin( material.angle );
                //	textureCos = idMath::Cos( material.angle );
                //}

                // now allocate and generate the combined vertexes
                R_AllocStaticTriSurfVerts(tri, tri.numVerts);
                for (j = 0; j < tri.numVerts; j++) {
                    mv = mvTable[j];
                    tri.verts[ j].Clear();
                    tri.verts[ j].xyz = mesh.vertexes[ mv.v];
                    tri.verts[ j].normal = mv.normal;
                    tri.verts[j].color = mv.color;
                    if (mesh.numTVertexes != 0) {
                        final idVec2 tv2 = mesh.tvertexes[ mv.tv];
                        float U = tv2.x * uTiling + uOffset;
                        float V = tv2.y * vTiling + vOffset;
                        tri.verts[ j].st.oSet(0, U * textureCos + V * textureSin);
                        tri.verts[ j].st.oSet(1, U * -textureSin + V * textureCos);
                    }
                }
//
//                R_StaticFree(mvTable);
//                R_StaticFree(mvHash);
//                R_StaticFree(tvRemap);
//                R_StaticFree(vRemap);

                // see if we need to merge with a previous surface of the same material
                modelSurf = this.surfaces.oGet(mergeTo[ objectNum]);
                srfTriangles_s mergeTri = modelSurf.geometry;
                if (null == mergeTri) {
                    modelSurf.geometry = tri;
                } else {
                    modelSurf.geometry = R_MergeTriangles(mergeTri, tri);
                    R_FreeStaticTriSurf(tri);
                    R_FreeStaticTriSurf(mergeTri);
                }
            }

            return true;
        }

        public aseModel_s ConvertLWOToASE(final lwObject obj, final String fileName) {
            int j, k;
            aseModel_s ase;

            if (NOT(obj)) {
                return null;
            }

            // NOTE: using new operator because aseModel_s contains idList class objects
            ase = new aseModel_s();
            ase.timeStamp[0] = obj.timeStamp[0];
            ase.objects.Resize(obj.nlayers, obj.nlayers);

            int materialRef = 0;

            for (lwSurface surf = obj.surf; surf != null; surf = surf.next) {

                aseMaterial_t mat = new aseMaterial_t();// Mem_ClearedAlloc(sizeof( * mat));
                System.arraycopy(surf.name.toCharArray(), 0, mat.name, 0, surf.name.length());
                mat.uTiling = mat.vTiling = 1;
                mat.angle = mat.uOffset = mat.vOffset = 0;
                ase.materials.Append(mat);

                lwLayer layer = obj.layer;

                aseObject_t object = new aseObject_t();// Mem_ClearedAlloc(sizeof( * object));
                object.materialRef = materialRef++;

                aseMesh_t mesh = object.mesh;
                ase.objects.Append(object);

                mesh.numFaces = layer.polygon.count;
                mesh.numTVFaces = mesh.numFaces;
                mesh.faces = new aseFace_t[mesh.numFaces];// Mem_Alloc(mesh.numFaces /* sizeof( mesh.faces[0] )*/);

                mesh.numVertexes = layer.point.count;
                mesh.vertexes = new idVec3[mesh.numVertexes];// Mem_Alloc(mesh.numVertexes /* sizeof( mesh.vertexes[0] )*/);

                // vertex positions
                if (layer.point.count <= 0) {
                    common.Warning("ConvertLWOToASE: model \'%s\' has bad or missing vertex data", name);
                }

                for (j = 0; j < layer.point.count; j++) {
                    mesh.vertexes[j].x = layer.point.pt[j].pos[0];
                    mesh.vertexes[j].y = layer.point.pt[j].pos[2];
                    mesh.vertexes[j].z = layer.point.pt[j].pos[1];
                }

                // vertex texture coords
                mesh.numTVertexes = 0;

                if (layer.nvmaps != 0) {
                    for (lwVMap vm = layer.vmap; vm != null; vm = vm.next) {
                        if (vm.type == LWID_('T', 'X', 'U', 'V')) {
                            mesh.numTVertexes += vm.nverts;
                        }
                    }
                }

                if (mesh.numTVertexes != 0) {
                    mesh.tvertexes = new idVec2[mesh.numTVertexes];// Mem_Alloc(mesh.numTVertexes /* sizeof( mesh.tvertexes[0] )*/);
                    int offset = 0;
                    for (lwVMap vm = layer.vmap; vm != null; vm = vm.next) {
                        if (vm.type == LWID_('T', 'X', 'U', 'V')) {
                            vm.offset = offset;
                            for (k = 0; k < vm.nverts; k++) {
                                mesh.tvertexes[k + offset].x = vm.val[k][0];
                                mesh.tvertexes[k + offset].y = 1.0f - vm.val[k][1];	// invert the t
                            }
                            offset += vm.nverts;
                        }
                    }
                } else {
                    common.Warning("ConvertLWOToASE: model \'%s\' has bad or missing uv data", fileName);
                    mesh.numTVertexes = 1;
                    mesh.tvertexes = new idVec2[mesh.numTVertexes];// Mem_ClearedAlloc(mesh.numTVertexes /* sizeof( mesh.tvertexes[0] )*/);
                }

                mesh.normalsParsed = true;
                mesh.colorsParsed = true;	// because we are falling back to the surface color

                // triangles
                int faceIndex = 0;
                for (j = 0; j < layer.polygon.count; j++) {
                    lwPolygon poly = layer.polygon.pol[j];

                    if (poly.surf != surf) {
                        continue;
                    }

                    if (poly.nverts != 3) {
                        common.Warning("ConvertLWOToASE: model %s has too many verts for a poly! Make sure you triplet it down", fileName);
                        continue;
                    }

                    mesh.faces[faceIndex].faceNormal.x = poly.norm[0];
                    mesh.faces[faceIndex].faceNormal.y = poly.norm[2];
                    mesh.faces[faceIndex].faceNormal.z = poly.norm[1];

                    for (k = 0; k < 3; k++) {

                        mesh.faces[faceIndex].vertexNum[k] = poly.getV(k).index;

                        mesh.faces[faceIndex].vertexNormals[k].x = poly.getV(k).norm[0];
                        mesh.faces[faceIndex].vertexNormals[k].y = poly.getV(k).norm[2];
                        mesh.faces[faceIndex].vertexNormals[k].z = poly.getV(k).norm[1];

                        // complete fallbacks
                        mesh.faces[faceIndex].tVertexNum[k] = 0;

                        mesh.faces[faceIndex].vertexColors[k][0] = (byte) (surf.color.rgb[0] * 255);
                        mesh.faces[faceIndex].vertexColors[k][1] = (byte) (surf.color.rgb[1] * 255);
                        mesh.faces[faceIndex].vertexColors[k][2] = (byte) (surf.color.rgb[2] * 255);
                        mesh.faces[faceIndex].vertexColors[k][3] = (byte) 255;

                        // first set attributes from the vertex
                        lwPoint pt = layer.point.pt[poly.getV(k).index];
                        int nvm;
                        for (nvm = 0; nvm < pt.nvmaps; nvm++) {
                            lwVMapPt vm = pt.vm[nvm];

                            if (vm.vmap.type == LWID_('T', 'X', 'U', 'V')) {
                                mesh.faces[faceIndex].tVertexNum[k] = vm.index + vm.vmap.offset;
                            }
                            if (vm.vmap.type == LWID_('R', 'G', 'B', 'A')) {
                                for (int chan = 0; chan < 4; chan++) {
                                    mesh.faces[faceIndex].vertexColors[k][chan] = (byte) (255 * vm.vmap.val[vm.index][chan]);
                                }
                            }
                        }

                        // then override with polygon attributes
                        for (nvm = 0; nvm < poly.getV(k).nvmaps; nvm++) {
                            lwVMapPt vm = poly.getV(k).vm[nvm];

                            if (vm.vmap.type == LWID_('T', 'X', 'U', 'V')) {
                                mesh.faces[faceIndex].tVertexNum[k] = vm.index + vm.vmap.offset;
                            }
                            if (vm.vmap.type == LWID_('R', 'G', 'B', 'A')) {
                                for (int chan = 0; chan < 4; chan++) {
                                    mesh.faces[faceIndex].vertexColors[k][chan] = (byte) (255 * vm.vmap.val[vm.index][chan]);
                                }
                            }
                        }
                    }

                    faceIndex++;
                }

                mesh.numFaces = faceIndex;
                mesh.numTVFaces = faceIndex;

                aseFace_t[] newFaces = new aseFace_t[mesh.numFaces];// Mem_Alloc(mesh.numFaces /* sizeof ( mesh.faces[0] ) */);
//		memcpy( newFaces, mesh.faces, sizeof( mesh.faces[0] ) * mesh.numFaces );
                System.arraycopy(mesh.faces, 0, newFaces, 0, mesh.numFaces);
//                Mem_Free(mesh.faces);
                mesh.faces = newFaces;
            }

            return ase;
        }

        public boolean DeleteSurfaceWithId(int id) {
            int i;

            for (i = 0; i < surfaces.Num(); i++) {
                if (surfaces.oGet(i).id == id) {
                    R_FreeStaticTriSurf(surfaces.oGet(i).geometry);
                    surfaces.RemoveIndex(i);
                    return true;
                }
            }
            return false;
        }

        public void DeleteSurfacesWithNegativeId() {
            int i;

            for (i = 0; i < surfaces.Num(); i++) {
                if (surfaces.oGet(i).id < 0) {
                    R_FreeStaticTriSurf(surfaces.oGet(i).geometry);
                    surfaces.RemoveIndex(i);
                    i--;
                }
            }
        }

        public boolean FindSurfaceWithId(int id, int[] surfaceNum) {
            int i;

            for (i = 0; i < surfaces.Num(); i++) {
                if (surfaces.oGet(i).id == id) {
                    surfaceNum[0] = i;
                    return true;
                }
            }
            return false;
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    /*
     ================
     AddCubeFace
     ================
     */
    static void AddCubeFace(srfTriangles_s tri, final idVec3 v1, final idVec3 v2, final idVec3 v3, final idVec3 v4) {
        tri.verts[tri.numVerts + 0].Clear();
        tri.verts[tri.numVerts + 0].xyz = v1.oMultiply(8);
        tri.verts[tri.numVerts + 0].st.oSet(0, 0);
        tri.verts[tri.numVerts + 0].st.oSet(1, 0);

        tri.verts[tri.numVerts + 1].Clear();
        tri.verts[tri.numVerts + 1].xyz = v2.oMultiply(8);
        tri.verts[tri.numVerts + 1].st.oSet(0, 1);
        tri.verts[tri.numVerts + 1].st.oSet(1, 0);

        tri.verts[tri.numVerts + 2].Clear();
        tri.verts[tri.numVerts + 2].xyz = v3.oMultiply(8);
        tri.verts[tri.numVerts + 2].st.oSet(0, 1);
        tri.verts[tri.numVerts + 2].st.oSet(1, 1);

        tri.verts[tri.numVerts + 3].Clear();
        tri.verts[tri.numVerts + 3].xyz = v4.oMultiply(8);
        tri.verts[tri.numVerts + 3].st.oSet(0, 0);
        tri.verts[tri.numVerts + 3].st.oSet(1, 1);

        tri.indexes[tri.numIndexes + 0] = tri.numVerts + 0;
        tri.indexes[tri.numIndexes + 1] = tri.numVerts + 1;
        tri.indexes[tri.numIndexes + 2] = tri.numVerts + 2;
        tri.indexes[tri.numIndexes + 3] = tri.numVerts + 0;
        tri.indexes[tri.numIndexes + 4] = tri.numVerts + 2;
        tri.indexes[tri.numIndexes + 5] = tri.numVerts + 3;

        tri.numVerts += 4;
        tri.numIndexes += 6;
    }
}
