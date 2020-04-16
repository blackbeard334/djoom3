package neo.Renderer;

import static neo.Renderer.Model.dynamicModel_t.DM_STATIC;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurfVertexCaches;
import static neo.framework.Common.common;
import static neo.idlib.math.Simd.SIMDProcessor;

import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.Model_local.idRenderModelStatic;
import neo.framework.DemoFile.idDemoFile;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec2;

/**
 *
 */
public class ModelOverlay {
    /*
     ===============================================================================

     Render model overlay for adding decals on top of dynamic models.

     ===============================================================================
     */

    static final int MAX_OVERLAY_SURFACES = 16;

    static class overlayVertex_s {

        int vertexNum;
        float[] st = new float[2];
    }

    private static class overlaySurface_s {

        int[] surfaceNum = {0};
        int surfaceId;
        int numIndexes;
        int/*glIndex_t*/[] indexes;
        int numVerts;
        overlayVertex_s[] verts;

        private void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    static class overlayMaterial_s {

        idMaterial material;
        idList<overlaySurface_s> surfaces;
    }

    static class idRenderModelOverlay {

        public idRenderModelOverlay() {
        }
        // ~idRenderModelOverlay();

        public static idRenderModelOverlay Alloc() {
            return new idRenderModelOverlay();
        }

        @Deprecated
        public static void Free(idRenderModelOverlay overlay) {
//	delete overlay;
        }

        /*
         =====================
         idRenderModelOverlay::CreateOverlay

         This projects on both front and back sides to avoid seams
         The material should be clamped, because entire triangles are added, some of which
         may extend well past the 0.0 to 1.0 texture range
         =====================
         */
        // Projects an overlay onto deformable geometry and can be added to
        // a render entity to allow decals on top of dynamic models.
        // This does not generate tangent vectors, so it can't be used with
        // light interaction shaders. Materials for overlays should always
        // be clamped, because the projected texcoords can run well off the
        // texture since no new clip vertexes are generated.
        public void CreateOverlay(final idRenderModel model, final idPlane[] localTextureAxis/*[2]*/, final idMaterial mtr) {
            int i, maxVerts, maxIndexes, surfNum;
            final idRenderModelOverlay overlay = null;

            // count up the maximum possible vertices and indexes per surface
            maxVerts = 0;
            maxIndexes = 0;
            for (surfNum = 0; surfNum < model.NumSurfaces(); surfNum++) {
                final modelSurface_s surf = model.Surface(surfNum);
                if (surf.geometry.numVerts > maxVerts) {
                    maxVerts = surf.geometry.numVerts;
                }
                if (surf.geometry.getIndexes().getNumValues() > maxIndexes) {
                    maxIndexes = surf.geometry.getIndexes().getNumValues();
                }
            }

            // make temporary buffers for the building process
            final overlayVertex_s[] overlayVerts = new overlayVertex_s[maxVerts];
            final int[]/*glIndex_t*/ overlayIndexes = new int[maxIndexes];

            // pull out the triangles we need from the base surfaces
            for (surfNum = 0; surfNum < model.NumBaseSurfaces(); surfNum++) {
                final modelSurface_s surf = model.Surface(surfNum);
                float d;

                if ((null == surf.geometry) || (null == surf.shader)) {
                    continue;
                }

                // some surfaces can explicitly disallow overlays
                if (!surf.shader.AllowOverlays()) {
                    continue;
                }

                final srfTriangles_s stri = surf.geometry;

                // try to cull the whole surface along the first texture axis
                d = stri.bounds.PlaneDistance(localTextureAxis[0]);
                if ((d < 0.0f) || (d > 1.0f)) {
                    continue;
                }

                // try to cull the whole surface along the second texture axis
                d = stri.bounds.PlaneDistance(localTextureAxis[1]);
                if ((d < 0.0f) || (d > 1.0f)) {
                    continue;
                }

                final byte[] cullBits = new byte[stri.numVerts];
                final idVec2[] texCoords = new idVec2[stri.numVerts];

                SIMDProcessor.OverlayPointCull(cullBits, texCoords, localTextureAxis, stri.verts, stri.numVerts);

                final int[]/*glIndex_t */ vertexRemap = new int[stri.numVerts];
                SIMDProcessor.Memset(vertexRemap, -1, stri.numVerts);

                // find triangles that need the overlay
                int numVerts = 0;
                int numIndexes = 0;
                int triNum = 0;
                for (int index = 0; index < stri.getIndexes().getNumValues(); index += 3, triNum++) {
                    final int v1 = stri.getIndexes().getValues().get(index + 0);
                    final int v2 = stri.getIndexes().getValues().get(index + 1);
                    final int v3 = stri.getIndexes().getValues().get(index + 2);

                    // skip triangles completely off one side
                    if ((cullBits[v1] & cullBits[v2] & cullBits[v3]) != 0) {
                        continue;
                    }

                    // we could do more precise triangle culling, like the light interaction does, if desired
                    // keep this triangle
                    for (int vnum = 0; vnum < 3; vnum++) {
                        final int ind = stri.getIndexes().getValues().get(index + vnum);
                        if (vertexRemap[ind] == -1) {
                            vertexRemap[ind] = numVerts;

                            overlayVerts[numVerts].vertexNum = ind;
                            overlayVerts[numVerts].st[0] = texCoords[ind].oGet(0);
                            overlayVerts[numVerts].st[1] = texCoords[ind].oGet(1);

                            numVerts++;
                        }
                        overlayIndexes[numIndexes++] = vertexRemap[ind];
                    }
                }

                if (0 == numIndexes) {
                    continue;
                }

                final overlaySurface_s s = new overlaySurface_s();// Mem_Alloc(sizeof(overlaySurface_t));
                s.surfaceNum[0] = surfNum;
                s.surfaceId = surf.id;
                s.verts = new overlayVertex_s[numVerts];// Mem_Alloc(numVerts);
//                memcpy(s.verts, overlayVerts, numVerts * sizeof(s.verts[0]));
                System.arraycopy(overlayVerts, 0, s.verts, 0, numVerts);
                s.numVerts = numVerts;
                s.indexes = new int[numIndexes];///*(glIndex_t *)*/Mem_Alloc(numIndexes);
//                memcpy(s.indexes, overlayIndexes, numIndexes * sizeof(s.indexes[0]));
                System.arraycopy(overlayIndexes, 0, s.indexes, 0, numIndexes);
                s.numIndexes = numIndexes;

                for (i = 0; i < this.materials.Num(); i++) {
                    if (this.materials.oGet(i).material == mtr) {
                        break;
                    }
                }
                if (i < this.materials.Num()) {
                    this.materials.oGet(i).surfaces.Append(s);
                } else {
                    final overlayMaterial_s mat = new overlayMaterial_s();
                    mat.material = mtr;
                    mat.surfaces.Append(s);
                    this.materials.Append(mat);
                }
            }

            // remove the oldest overlay surfaces if there are too many per material
            for (i = 0; i < this.materials.Num(); i++) {
                while (this.materials.oGet(i).surfaces.Num() > MAX_OVERLAY_SURFACES) {
                    FreeSurface(this.materials.oGet(i).surfaces.oGet(0));
                    this.materials.oGet(i).surfaces.RemoveIndex(0);
                }
            }
        }

        // Creates new model surfaces for baseModel, which should be a static instantiation of a dynamic model.
        public void AddOverlaySurfacesToModel(idRenderModel baseModel) {
            int i, j, k, numVerts, numIndexes;
            final int[] surfaceNum = new int[1];
            modelSurface_s baseSurf;
            idRenderModelStatic staticModel;
            overlaySurface_s surf;
            srfTriangles_s newTri;
            modelSurface_s newSurf;

            if ((baseModel == null) || baseModel.IsDefaultModel()) {
                return;
            }

            // md5 models won't have any surfaces when r_showSkel is set
            if (0 == baseModel.NumSurfaces()) {
                return;
            }

            if (baseModel.IsDynamicModel() != DM_STATIC) {
                common.Error("idRenderModelOverlay::AddOverlaySurfacesToModel: baseModel is not a static model");
            }

//	assert( dynamic_cast<idRenderModelStatic *>(baseModel) != null );
            staticModel = (idRenderModelStatic) baseModel;

            staticModel.overlaysAdded = 0;

            if (0 == this.materials.Num()) {
                staticModel.DeleteSurfacesWithNegativeId();
                return;
            }

            for (k = 0; k < this.materials.Num(); k++) {

                numVerts = numIndexes = 0;
                for (i = 0; i < this.materials.oGet(k).surfaces.Num(); i++) {
                    numVerts += this.materials.oGet(k).surfaces.oGet(i).numVerts;
                    numIndexes += this.materials.oGet(k).surfaces.oGet(i).numIndexes;
                }

                if (staticModel.FindSurfaceWithId(-1 - k, surfaceNum)) {
                    newSurf = staticModel.surfaces.oGet(surfaceNum[0]);
                } else {
                    newSurf = staticModel.surfaces.Alloc();
                    newSurf.geometry = null;
                    newSurf.shader = this.materials.oGet(k).material;
                    newSurf.id = -1 - k;
                }

                if ((newSurf.geometry == null) || (newSurf.geometry.numVerts < numVerts) || (newSurf.geometry.getIndexes().getNumValues() < numIndexes)) {
                    R_FreeStaticTriSurf(newSurf.geometry);
                    newSurf.geometry = R_AllocStaticTriSurf();
                    R_AllocStaticTriSurfVerts(newSurf.geometry, numVerts);
                    R_AllocStaticTriSurfIndexes(newSurf.geometry, numIndexes);
                    SIMDProcessor.Memset(newSurf.geometry.verts, 0, numVerts);
                } else {
                    R_FreeStaticTriSurfVertexCaches(newSurf.geometry);
                }

                newTri = newSurf.geometry;
                numVerts = numIndexes = 0;

                for (i = 0; i < this.materials.oGet(k).surfaces.Num(); i++) {
                    surf = this.materials.oGet(k).surfaces.oGet(i);

                    // get the model surface for this overlay surface
                    if (surf.surfaceNum[0] < staticModel.NumSurfaces()) {
                        baseSurf = staticModel.Surface(surf.surfaceNum[0]);
                    } else {
                        baseSurf = null;
                    }

                    // if the surface ids no longer match
                    if ((null == baseSurf) || (baseSurf.id != surf.surfaceId)) {
                        // find the surface with the correct id
                        if (staticModel.FindSurfaceWithId(surf.surfaceId, surf.surfaceNum)) {
                            baseSurf = staticModel.Surface(surf.surfaceNum[0]);
                        } else {
                            // the surface with this id no longer exists
                            FreeSurface(surf);
                            this.materials.oGet(k).surfaces.RemoveIndex(i);
                            i--;
                            continue;
                        }
                    }

                    // copy indexes;
                    for (j = 0; j < surf.numIndexes; j++) {
                        newTri.getIndexes().getValues().put(numIndexes + j, numVerts + surf.indexes[j]);
                    }
                    numIndexes += surf.numIndexes;

                    // copy vertices
                    for (j = 0; j < surf.numVerts; j++) {
                        final overlayVertex_s overlayVert = surf.verts[j];

                        newTri.verts[numVerts].st.oSet(0, overlayVert.st[0]);
                        newTri.verts[numVerts].st.oSet(1, overlayVert.st[1]);

                        if (overlayVert.vertexNum >= baseSurf.geometry.numVerts) {
                            // This can happen when playing a demofile and a model has been changed since it was recorded, so just issue a warning and go on.
                            common.Warning("idRenderModelOverlay::AddOverlaySurfacesToModel: overlay vertex out of range.  Model has probably changed since generating the overlay.");
                            FreeSurface(surf);
                            this.materials.oGet(k).surfaces.RemoveIndex(i);
                            staticModel.DeleteSurfaceWithId(newSurf.id);
                            return;
                        }
                        newTri.verts[numVerts].xyz = baseSurf.geometry.verts[overlayVert.vertexNum].xyz;
                        numVerts++;
                    }
                }

                newTri.numVerts = numVerts;
                newTri.getIndexes().setNumValues(numIndexes);
                R_BoundTriSurf(newTri);

                staticModel.overlaysAdded++;	// so we don't create an overlay on an overlay surface
            }
        }

        // Removes overlay surfaces from the model.
        public static void RemoveOverlaySurfacesFromModel(idRenderModel baseModel) {
            idRenderModelStatic staticModel;

//	assert( dynamic_cast<idRenderModelStatic *>(baseModel) != NULL );
            staticModel = (idRenderModelStatic) baseModel;

            staticModel.DeleteSurfacesWithNegativeId();
            staticModel.overlaysAdded = 0;
        }

        public void ReadFromDemoFile(idDemoFile f) {
            // FIXME: implement
        }

        public void WriteToDemoFile(idDemoFile f) {
            // FIXME: implement
        }
//
        private idList<overlayMaterial_s> materials;
//

        private void FreeSurface(overlaySurface_s surface) {
            if (surface.verts != null) {
//                Mem_Free(surface.verts);
                surface.verts = null;
            }
            if (surface.indexes != null) {
//                Mem_Free(surface.indexes);
                surface.indexes = null;
            }
            surface.clear();
        }
    }
}
