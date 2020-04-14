package neo.Renderer;

import static neo.Renderer.Interaction.idInteraction.frustumStates.FRUSTUM_INVALID;
import static neo.Renderer.Interaction.idInteraction.frustumStates.FRUSTUM_UNINITIALIZED;
import static neo.Renderer.Interaction.idInteraction.frustumStates.FRUSTUM_VALID;
import static neo.Renderer.Interaction.idInteraction.frustumStates.FRUSTUM_VALIDAREAS;
import static neo.Renderer.Material.MF_NOSELFSHADOW;
import static neo.Renderer.Material.materialCoverage_t.MC_OPAQUE;
import static neo.Renderer.Material.materialCoverage_t.MC_TRANSLUCENT;
import static neo.Renderer.RenderSystem_init.r_lightAllBackFaces;
import static neo.Renderer.RenderSystem_init.r_showInteractionFrustums;
import static neo.Renderer.RenderSystem_init.r_showInteractionScissors;
import static neo.Renderer.RenderSystem_init.r_skipSuppress;
import static neo.Renderer.RenderSystem_init.r_useIndexBuffers;
import static neo.Renderer.RenderSystem_init.r_useInteractionCulling;
import static neo.Renderer.RenderSystem_init.r_useInteractionScissors;
import static neo.Renderer.RenderSystem_init.r_useOptimizedShadows;
import static neo.Renderer.RenderSystem_init.r_usePreciseTriangleInteractions;
import static neo.Renderer.RenderSystem_init.r_useShadowCulling;
import static neo.Renderer.RenderSystem_init.r_znear;
import static neo.Renderer.RenderWorld.R_GlobalShaderOverride;
import static neo.Renderer.RenderWorld.R_RemapShaderBySkin;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.tr_light.R_CreateAmbientCache;
import static neo.Renderer.tr_light.R_CreateLightingCache;
import static neo.Renderer.tr_light.R_CreatePrivateShadowCache;
import static neo.Renderer.tr_light.R_CreateVertexProgramShadowCache;
import static neo.Renderer.tr_light.R_EntityDefDynamicModel;
import static neo.Renderer.tr_light.R_LinkLightSurf;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_main.R_CullLocalBox;
import static neo.Renderer.tr_main.R_GlobalPlaneToLocal;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_main.R_ScreenRectFromViewFrustumBounds;
import static neo.Renderer.tr_main.R_ShowColoredScreenRect;
import static neo.Renderer.tr_shadowbounds.R_CalcIntersectionScissor;
import static neo.Renderer.tr_stencilshadow.R_CreateShadowVolume;
import static neo.Renderer.tr_stencilshadow.shadowGen_t.SG_DYNAMIC;
import static neo.Renderer.tr_stencilshadow.shadowGen_t.SG_STATIC;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_DeriveFacePlanes;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_ReallyFreeStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_ReferenceStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_ReferenceStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_ResizeStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_TriSurfMemory;
import static neo.TempDump.NOT;
import static neo.framework.Common.common;
import static neo.idlib.Lib.MAX_WORLD_SIZE;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorMagenta;
import static neo.idlib.Lib.colorPurple;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.math.Plane.SIDE_BACK;
import static neo.idlib.math.Plane.SIDE_FRONT;
import static neo.idlib.math.Simd.SIMDProcessor;

import java.util.stream.Stream;

import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.RenderWorld_local.idRenderWorldLocal;
import neo.Renderer.tr_local.areaReference_s;
import neo.Renderer.tr_local.idRenderEntityLocal;
import neo.Renderer.tr_local.idRenderLightLocal;
import neo.Renderer.tr_local.idScreenRect;
import neo.Renderer.tr_local.viewEntity_s;
import neo.Renderer.tr_local.viewLight_s;
import neo.Renderer.tr_stencilshadow.shadowGen_t;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Box.idBox;
import neo.idlib.BV.Frustum.idFrustum;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class Interaction {
    /*
     ===============================================================================

     Interaction between entityDef surfaces and a lightDef.

     Interactions with no lightTris and no shadowTris are still
     valid, because they show that a given entityDef / lightDef
     do not interact, even though they share one or more areas.

     ===============================================================================
     */

    static final srfTriangles_s LIGHT_TRIS_DEFERRED;// = -03146;//((srfTriangles_s *)-1)
    static byte[] LIGHT_CULL_ALL_FRONT;//((byte *)-1)
    static final float LIGHT_CLIP_EPSILON = 0.1f;

    static {
        final srfTriangles_s s = LIGHT_TRIS_DEFERRED = new srfTriangles_s();
        s.ambientViewCount = s.numDupVerts = s.numVerts =
                s.numMirroredVerts = s.getIndexes().setNumValues(s.numShadowIndexesNoCaps =
                        s.numShadowIndexesNoFrontCaps = s.numSilEdges = s.shadowCapPlaneBits =
                                -03146);

    }

    public static class srfCullInfo_t {

        // For each triangle a byte set to 1 if facing the light origin.
        public byte[] facing;
//
        // For each vertex a byte with the bits [0-5] set if the
        // vertex is at the back side of the corresponding clip plane.
        // If the 'cullBits' pointer equals LIGHT_CULL_ALL_FRONT all
        // vertices are at the front of all the clip planes.
        public byte[] cullBits;
//        
        // Clip planes in surface space used to calculate the cull bits.
        public final idPlane[] localClipPlanes = idPlane.generateArray(6);
    }

    static class surfaceInteraction_t {

        // if lightTris == LIGHT_TRIS_DEFERRED, then the calculation of the
        // lightTris has been deferred, and must be done if ambientTris is visible
        srfTriangles_s lightTris;
//
        // shadow volume triangle surface
        srfTriangles_s shadowTris;
//
        // so we can check ambientViewCount before adding lightTris, and get
        // at the shared vertex and possibly shadowVertex caches
        srfTriangles_s ambientTris;
//
        idMaterial shader;
        int expCulled;			// only for the experimental shadow buffer renderer
//        
        srfCullInfo_t cullInfo;
        //
        //

        public surfaceInteraction_t() {
            this.cullInfo = new srfCullInfo_t();
        }
        
        static surfaceInteraction_t[] generateArray(final int length) {
            return Stream.
                    generate(surfaceInteraction_t::new).
                    limit(length).
                    toArray(surfaceInteraction_t[]::new);
        }
    }

    static class areaNumRef_s {

        areaNumRef_s next;
        int areaNum;
    }

    /*
     ===========================================================================

     idInteraction implementation

     ===========================================================================
     */
    public static class idInteraction {

        // this may be 0 if the light and entity do not actually intersect
        // -1 = an untested interaction
        public int numSurfaces;
        //
        // if there is a whole-entity optimized shadow hull, it will
        // be present as a surfaceInteraction_t with a NULL ambientTris, but
        // possibly having a shader to specify the shadow sorting order
        public surfaceInteraction_t[] surfaces;
        //	
        // get space from here, if NULL, it is a pre-generated shadow volume from dmap
        public idRenderEntityLocal entityDef;
        public idRenderLightLocal lightDef;
        //
        public idInteraction lightNext;			// for lightDef chains
        public idInteraction lightPrev;
        public idInteraction entityNext;		// for entityDef chains
        public idInteraction entityPrev;
        //

        enum frustumStates {

            FRUSTUM_UNINITIALIZED,
            FRUSTUM_INVALID,
            FRUSTUM_VALID,
            FRUSTUM_VALIDAREAS
        }
        private frustumStates frustumState;
        private final idFrustum frustum;		// frustum which contains the interaction
        private areaNumRef_s frustumAreas;		// numbers of the areas the frustum touches
        //
        private int dynamicModelFrameCount;     // so we can tell if a callback model animated
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count   = DBG_counter++;

        public idInteraction() {
            this.numSurfaces = 0;
            this.surfaces = null;
            this.entityDef = null;
            this.lightDef = null;
            this.lightNext = null;
            this.lightPrev = null;
            this.entityNext = null;
            this.entityPrev = null;
            this.dynamicModelFrameCount = 0;
            this.frustumState = FRUSTUM_UNINITIALIZED;
            this.frustum = new idFrustum();
            this.frustumAreas = null;
        }

        // because these are generated and freed each game tic for active elements all
        // over the world, we use a custom pool allocater to avoid memory allocation overhead
        // and fragmentation
        public static idInteraction AllocAndLink(idRenderEntityLocal eDef, idRenderLightLocal lDef) {
            if (NOT(eDef) || NOT(lDef)) {
                common.Error("idInteraction::AllocAndLink: null parm");
            }

            final idRenderWorldLocal renderWorld = eDef.world;

            final idInteraction interaction = new idInteraction();//renderWorld.interactionAllocator.Alloc();

            // link and initialize
            interaction.dynamicModelFrameCount = 0;

            interaction.lightDef = lDef;
            interaction.entityDef = eDef;

            interaction.numSurfaces = -1;// not checked yet
            interaction.surfaces = null;

            interaction.frustumState = FRUSTUM_UNINITIALIZED;
            interaction.frustumAreas = null;

            // link at the start of the entity's list
            interaction.lightNext = lDef.firstInteraction;
            interaction.lightPrev = null;
            lDef.firstInteraction = interaction;
            if (interaction.lightNext != null) {
                interaction.lightNext.lightPrev = interaction;
            } else {
                lDef.lastInteraction = interaction;
            }

            // link at the start of the light's list
            interaction.entityNext = eDef.firstInteraction;
            interaction.entityPrev = null;
            eDef.firstInteraction = interaction;
            if (interaction.entityNext != null) {
                interaction.entityNext.entityPrev = interaction;
            } else {
                eDef.lastInteraction = interaction;
            }

            // update the interaction table
            if (renderWorld.interactionTable != null) {
                final int index = (lDef.index * renderWorld.interactionTableWidth) + eDef.index;
                if (renderWorld.interactionTable[index] != null) {
                    common.Error("idInteraction::AllocAndLink: non null table entry");
                }
                renderWorld.interactionTable[ index] = interaction;
            }

            return interaction;
        }

        /*
         ===============
         idInteraction::UnlinkAndFree

         Removes links and puts it back on the free list.
         ===============
         */
        // unlinks from the entity and light, frees all surfaceInteractions,
        // and puts it back on the free list
        public void UnlinkAndFree() {

            // clear the table pointer
            final idRenderWorldLocal renderWorld = this.lightDef.world;
            if (renderWorld.interactionTable != null) {
                final int index = (this.lightDef.index * renderWorld.interactionTableWidth) + this.entityDef.index;
                if (renderWorld.interactionTable[index] != this) {
                    common.Error("idInteraction::UnlinkAndFree: interactionTable wasn't set");
                }
                renderWorld.interactionTable[index] = null;
            }

            Unlink();

            FreeSurfaces();

            // free the interaction area references
            areaNumRef_s area, nextArea;
            for (area = this.frustumAreas; area != null; area = nextArea) {
                nextArea = area.next;
//                renderWorld.areaNumRefAllocator.Free(area);
            }

            // put it back on the free list
//            renderWorld.interactionAllocator.Free(this);
        }

        /*
         ===============
         idInteraction::FreeSurfaces

         Frees the surfaces, but leaves the interaction linked in, so it
         will be regenerated automatically
         ===============
         */
        // free the interaction surfaces
        public void FreeSurfaces() {
            if (this.surfaces != null) {
                for (int i = 0; i < this.numSurfaces; i++) {
                    final surfaceInteraction_t sint = this.surfaces[i];

                    if (sint.lightTris != null) {
                        if (sint.lightTris != LIGHT_TRIS_DEFERRED) {
                            R_FreeStaticTriSurf(sint.lightTris);
                        }
                        sint.lightTris = null;
                    }
                    if (sint.shadowTris != null) {
                        // if it doesn't have an entityDef, it is part of a prelight
                        // model, not a generated interaction
                        if (this.entityDef != null) {
                            R_FreeStaticTriSurf(sint.shadowTris);
                            sint.shadowTris = null;
                        }
                    }
                    R_FreeInteractionCullInfo(sint.cullInfo);
                }

//                R_StaticFree(this.surfaces);
                this.surfaces = null;
            }
            this.numSurfaces = -1;
        }

        /*
         ===============
         idInteraction::MakeEmpty

         Makes the interaction empty and links it at the end of the entity's and light's interaction lists.
         ===============
         */
        // makes the interaction empty for when the light and entity do not actually intersect
        // all empty interactions are linked at the end of the light's and entity's interaction list
        public void MakeEmpty() {

            // an empty interaction has no surfaces
            this.numSurfaces = 0;

            Unlink();

            // relink at the end of the entity's list
            this.entityNext = null;
            this.entityPrev = this.entityDef.lastInteraction;
            this.entityDef.lastInteraction = this;
            if (this.entityPrev != null) {
                this.entityPrev.entityNext = this;
            } else {
                this.entityDef.firstInteraction = this;
            }

            // relink at the end of the light's list
            this.lightNext = null;
            this.lightPrev = this.lightDef.lastInteraction;
            this.lightDef.lastInteraction = this;
            if (this.lightPrev != null) {
                this.lightPrev.lightNext = this;
            } else {
                this.lightDef.firstInteraction = this;
            }
        }

        // returns true if the interaction is empty
        public boolean IsEmpty() {
            return (this.numSurfaces == 0);
        }

        // returns true if the interaction is not yet completely created
        public boolean IsDeferred() {
            return (this.numSurfaces == -1);
        }

        // returns true if the interaction has shadows
        public boolean HasShadows() {
            return (!this.lightDef.parms.noShadows && !this.entityDef.parms.noShadow && this.lightDef.lightShader.LightCastsShadows());
        }

        /*
         ===============
         idInteraction::MemoryUsed

         Counts up the memory used by all the surfaceInteractions, which
         will be used to determine when we need to start purging old interactions.
         ===============
         */
        // counts up the memory used by all the surfaceInteractions, which
        // will be used to determine when we need to start purging old interactions
        public int MemoryUsed() {
            int total = 0;

            for (int i = 0; i < this.numSurfaces; i++) {
                final surfaceInteraction_t inter = this.surfaces[i];

                total += R_TriSurfMemory(inter.lightTris);
                total += R_TriSurfMemory(inter.shadowTris);
            }

            return total;
        }

        /*
         ==================
         idInteraction::AddActiveInteraction

         Create and add any necessary light and shadow triangles

         If the model doesn't have any surfaces that need interactions
         with this type of light, it can be skipped, but we might need to
         instantiate the dynamic model to find out
         ==================
         */
        // makes sure all necessary light surfaces and shadow surfaces are created, and
        // calls R_LinkLightSurf() for each one
        public void AddActiveInteraction() {
            viewLight_s vLight;
            viewEntity_s vEntity;
            idScreenRect shadowScissor;
            idScreenRect lightScissor;
            final idVec3 localLightOrigin = new idVec3();
            final idVec3 localViewOrigin = new idVec3();

            vLight = this.lightDef.viewLight;
            vEntity = this.entityDef.viewEntity;

            // do not waste time culling the interaction frustum if there will be no shadows
            if (!HasShadows()) {

                // use the entity scissor rectangle
                shadowScissor = new idScreenRect(vEntity.scissorRect);

                // culling does not seem to be worth it for static world models
            } else if (this.entityDef.parms.hModel.IsStaticWorldModel()) {

                // use the light scissor rectangle
                shadowScissor = new idScreenRect(vLight.scissorRect);

            } else {

                // try to cull the interaction
                // this will also cull the case where the light origin is inside the
                // view frustum and the entity bounds are outside the view frustum
                if (CullInteractionByViewFrustum(tr.viewDef.viewFrustum)) {
                    return;
                }

                // calculate the shadow scissor rectangle
                shadowScissor = new idScreenRect(CalcInteractionScissorRectangle(tr.viewDef.viewFrustum));
            }

            // get out before making the dynamic model if the shadow scissor rectangle is empty
            if (shadowScissor.IsEmpty()) {
                return;
            }

            // We will need the dynamic surface created to make interactions, even if the
            // model itself wasn't visible.  This just returns a cached value after it
            // has been generated once in the view.
            final idRenderModel model = R_EntityDefDynamicModel(this.entityDef);
            if ((model == null) || (model.NumSurfaces() <= 0)) {
                return;
            }

            // the dynamic model may have changed since we built the surface list
            if (!IsDeferred() && (this.entityDef.dynamicModelFrameCount != this.dynamicModelFrameCount)) {
                FreeSurfaces();
            }
            this.dynamicModelFrameCount = this.entityDef.dynamicModelFrameCount;

            // actually create the interaction if needed, building light and shadow surfaces as needed
            if (IsDeferred()) {

                CreateInteraction(model);
            }

            R_GlobalPointToLocal(vEntity.modelMatrix, this.lightDef.globalLightOrigin, localLightOrigin);
            R_GlobalPointToLocal(vEntity.modelMatrix, tr.viewDef.renderView.vieworg, localViewOrigin);

            // calculate the scissor as the intersection of the light and model rects
            // this is used for light triangles, but not for shadow triangles
            lightScissor = new idScreenRect(vLight.scissorRect);
            lightScissor.Intersect(vEntity.scissorRect);

            final boolean lightScissorsEmpty = lightScissor.IsEmpty();

            // for each surface of this entity / light interaction
            for (int i = 0; i < this.numSurfaces; i++) {
                final surfaceInteraction_t sint = this.surfaces[i];

                // see if the base surface is visible, we may still need to add shadows even if empty
                if (!lightScissorsEmpty && (sint.ambientTris != null) && (sint.ambientTris.ambientViewCount == tr.viewCount)) {

                    // make sure we have created this interaction, which may have been deferred
                    // on a previous use that only needed the shadow
                    if (sint.lightTris == LIGHT_TRIS_DEFERRED) {
                        sint.lightTris = R_CreateLightTris(vEntity.entityDef, sint.ambientTris, vLight.lightDef, sint.shader, sint.cullInfo);
                        R_FreeInteractionCullInfo(sint.cullInfo);
                    }

                    final srfTriangles_s lightTris = sint.lightTris;

                    if (lightTris != null) {

                        // try to cull before adding
                        // FIXME: this may not be worthwhile. We have already done culling on the ambient,
                        // but individual surfaces may still be cropped somewhat more
                        if (!R_CullLocalBox(lightTris.bounds, vEntity.modelMatrix, 5, tr.viewDef.frustum)) {

                            // make sure the original surface has its ambient cache created
                            final srfTriangles_s tri = sint.ambientTris;
                            if (NOT(tri.ambientCache)) {
                                if (!R_CreateAmbientCache(tri, sint.shader.ReceivesLighting())) {
                                    // skip if we were out of vertex memory
                                    continue;
                                }
                            }

                            // reference the original surface's ambient cache
                            lightTris.ambientCache = tri.ambientCache;

                            // touch the ambient surface so it won't get purged
                            vertexCache.Touch(lightTris.ambientCache);

                            // regenerate the lighting cache (for non-vertex program cards) if it has been purged
                            if (NOT(lightTris.lightingCache)) {
                                if (!R_CreateLightingCache(this.entityDef, this.lightDef, lightTris)) {
                                    // skip if we are out of vertex memory
                                    continue;
                                }
                            }
                            // touch the light surface so it won't get purged
                            // (vertex program cards won't have a light cache at all)
                            if (lightTris.lightingCache != null) {
                                vertexCache.Touch(lightTris.lightingCache);
                            }

                            if (NOT(lightTris.indexCache) && r_useIndexBuffers.GetBool()) {
                                lightTris.indexCache = vertexCache.Alloc(lightTris.getIndexes().getValues(), lightTris.getIndexes().getNumValues(), true);
                            }
                            if (lightTris.indexCache != null) {
                                vertexCache.Touch(lightTris.indexCache);
                            }

                            // add the surface to the light list
                            final idMaterial[] shader = {sint.shader};
                            R_GlobalShaderOverride(shader);
                            sint.shader = shader[0];

                            // there will only be localSurfaces if the light casts shadows and
                            // there are surfaces with NOSELFSHADOW
                            if (sint.shader.Coverage() == MC_TRANSLUCENT) {
                                R_LinkLightSurf(vLight.translucentInteractions, lightTris, vEntity, this.lightDef, shader[0], lightScissor, false);
                            } else if (!this.lightDef.parms.noShadows && sint.shader.TestMaterialFlag(MF_NOSELFSHADOW)) {
                                R_LinkLightSurf(vLight.localInteractions, lightTris, vEntity, this.lightDef, shader[0], lightScissor, false);
                            } else {
                                R_LinkLightSurf(vLight.globalInteractions, lightTris, vEntity, this.lightDef, shader[0], lightScissor, false);
                            }
                        }
                    }
                }

                final srfTriangles_s shadowTris = sint.shadowTris;

                // the shadows will always have to be added, unless we can tell they
                // are from a surface in an unconnected area
                if (shadowTris != null) {

                    // check for view specific shadow suppression (player shadows, etc)
                    if (!r_skipSuppress.GetBool()) {
                        if ((this.entityDef.parms.suppressShadowInViewID != 0)
                                && (this.entityDef.parms.suppressShadowInViewID == tr.viewDef.renderView.viewID)) {
                            continue;
                        }
                        if ((this.entityDef.parms.suppressShadowInLightID != 0)
                                && (this.entityDef.parms.suppressShadowInLightID == this.lightDef.parms.lightId)) {
                            continue;
                        }
                    }

                    // cull static shadows that have a non-empty bounds
                    // dynamic shadows that use the turboshadow code will not have valid
                    // bounds, because the perspective projection extends them to infinity
                    if (r_useShadowCulling.GetBool() && !shadowTris.bounds.IsCleared()) {
                        if (R_CullLocalBox(shadowTris.bounds, vEntity.modelMatrix, 5, tr.viewDef.frustum)) {
                            continue;
                        }
                    }

                    // copy the shadow vertexes to the vertex cache if they have been purged
                    // if we are using shared shadowVertexes and letting a vertex program fix them up,
                    // get the shadowCache from the parent ambient surface
                    if (NOT(shadowTris.shadowVertexes)) {
                        // the data may have been purged, so get the latest from the "home position"
                        shadowTris.shadowCache = sint.ambientTris.shadowCache;
                    }

                    // if we have been purged, re-upload the shadowVertexes
                    if (NOT(shadowTris.shadowCache)) {
                        if (shadowTris.shadowVertexes != null) {
                            // each interaction has unique vertexes
                            R_CreatePrivateShadowCache(shadowTris);
                        } else {
                            R_CreateVertexProgramShadowCache(sint.ambientTris);
                            shadowTris.shadowCache = sint.ambientTris.shadowCache;
                        }
                        // if we are out of vertex cache space, skip the interaction
                        if (NOT(shadowTris.shadowCache)) {
                            continue;
                        }
                    }

                    // touch the shadow surface so it won't get purged
                    vertexCache.Touch(shadowTris.shadowCache);

                    if (NOT(shadowTris.indexCache) && r_useIndexBuffers.GetBool()) {
                        shadowTris.indexCache = vertexCache.Alloc(shadowTris.getIndexes().getValues(), shadowTris.getIndexes().getNumValues(), true);

                        vertexCache.Touch(shadowTris.indexCache);
                    }

                    // see if we can avoid using the shadow volume caps
                    final boolean inside = R_PotentiallyInsideInfiniteShadow(sint.ambientTris, localViewOrigin, localLightOrigin);

                    if (sint.shader.TestMaterialFlag(MF_NOSELFSHADOW)) {
                        R_LinkLightSurf(vLight.localShadows, shadowTris, vEntity, this.lightDef, null, shadowScissor, inside);
                    } else {
                        R_LinkLightSurf(vLight.globalShadows, shadowTris, vEntity, this.lightDef, null, shadowScissor, inside);
                    }
                }
            }
        }

        /*
         ====================
         idInteraction::CreateInteraction

         Called when a entityDef and a lightDef are both present in a
         portalArea, and might be visible.  Performs cull checking before doing the expensive
         computations.

         References tr.viewCount so lighting surfaces will only be created if the ambient surface is visible,
         otherwise it will be marked as deferred.

         The results of this are cached and valid until the light or entity change.
         ====================
         */
        // actually create the interaction
        private void CreateInteraction(final idRenderModel model) {
            final idMaterial lightShader = this.lightDef.lightShader;
            idMaterial shader;
            boolean interactionGenerated;
            idBounds bounds;

            tr.pc.c_createInteractions++;

            bounds = model.Bounds(this.entityDef.parms);

            // if it doesn't contact the light frustum, none of the surfaces will
            if (R_CullLocalBox(bounds, this.entityDef.modelMatrix, 6, this.lightDef.frustum)) {
                MakeEmpty();
                return;
            }

            // use the turbo shadow path
            shadowGen_t shadowGen = SG_DYNAMIC;

            // really large models, like outside terrain meshes, should use
            // the more exactly culled static shadow path instead of the turbo shadow path.
            // FIXME: this is a HACK, we should probably have a material flag.
            if ((bounds.oGet(1).oGet(0) - bounds.oGet(0).oGet(0)) > 3000) {
                shadowGen = SG_STATIC;
            }

            //
            // create slots for each of the model's surfaces
            //
            this.numSurfaces = model.NumSurfaces();
            this.surfaces = surfaceInteraction_t.generateArray(this.numSurfaces);

            interactionGenerated = false;

            // check each surface in the model
            for (int c = 0; c < model.NumSurfaces(); c++) {
                final modelSurface_s surf;
                srfTriangles_s tri;

                surf = model.Surface(c);

                tri = surf.geometry;
                if (NOT(tri)) {
                    continue;
                }

                // determine the shader for this surface, possibly by skinning
                shader = surf.shader;
                shader = R_RemapShaderBySkin(shader, this.entityDef.parms.customSkin, this.entityDef.parms.customShader);

                if (null == shader) {
                    continue;
                }

                // try to cull each surface
                if (R_CullLocalBox(tri.bounds, this.entityDef.modelMatrix, 6, this.lightDef.frustum)) {
                    continue;
                }

                final surfaceInteraction_t sint = this.surfaces[c];

                sint.shader = shader;

                // save the ambient tri pointer so we can reject lightTri interactions
                // when the ambient surface isn't in view, and we can get shared vertex
                // and shadow data from the source surface
                sint.ambientTris = tri;

                // "invisible ink" lights and shaders
                if (shader.Spectrum() != lightShader.Spectrum()) {
                    continue;
                }

                // generate a lighted surface and add it
                if (shader.ReceivesLighting()) {
                    if (tri.ambientViewCount == tr.viewCount) {
                        sint.lightTris = R_CreateLightTris(this.entityDef, tri, this.lightDef, shader, sint.cullInfo);
                    } else {
                        // this will be calculated when sint.ambientTris is actually in view
                        sint.lightTris = LIGHT_TRIS_DEFERRED;//HACKME::1:this throws a null pointer after the planet goes out of the screen, hitting you in the head!
                    }
                    interactionGenerated = true;
                }

                // if the interaction has shadows and this surface casts a shadow
                if (HasShadows() && shader.SurfaceCastsShadow() && (tri.silEdges != null)) {

                    // if the light has an optimized shadow volume, don't create shadows for any models that are part of the base areas
                    if ((this.lightDef.parms.prelightModel == null) || !model.IsStaticWorldModel() || !r_useOptimizedShadows.GetBool()) {

                        // this is the only place during gameplay (outside the utilities) that R_CreateShadowVolume() is called
                        sint.shadowTris = R_CreateShadowVolume(this.entityDef, tri, this.lightDef, shadowGen, sint.cullInfo);
                        if (sint.shadowTris != null) {
                            if ((shader.Coverage() != MC_OPAQUE) || (!r_skipSuppress.GetBool() && (this.entityDef.parms.suppressSurfaceInViewID != 0))) {
                                // if any surface is a shadow-casting perforated or translucent surface, or the
                                // base surface is suppressed in the view (world weapon shadows) we can't use
                                // the external shadow optimizations because we can see through some of the faces
                                sint.shadowTris.numShadowIndexesNoCaps = sint.shadowTris.getIndexes().getNumValues();
                                sint.shadowTris.numShadowIndexesNoFrontCaps = sint.shadowTris.getIndexes().getNumValues();
                            }
                        }
                        interactionGenerated = true;
                    }
                }

                // free the cull information when it's no longer needed
                if (sint.lightTris != LIGHT_TRIS_DEFERRED) {//HACKME::2:related to HACKME1
                    R_FreeInteractionCullInfo(sint.cullInfo);
                }
            }

            // if none of the surfaces generated anything, don't even bother checking?
            if (!interactionGenerated) {
                MakeEmpty();
            }
        }

        // unlink from entity and light lists
        private void Unlink() {

            // unlink from the entity's list
            if (this.entityPrev != null) {
                this.entityPrev.entityNext = this.entityNext;
            } else {
                this.entityDef.firstInteraction = this.entityNext;
            }
            if (this.entityNext != null) {
                this.entityNext.entityPrev = this.entityPrev;
            } else {
                this.entityDef.lastInteraction = this.entityPrev;
            }
            this.entityNext = this.entityPrev = null;

            // unlink from the light's list
            if (this.lightPrev != null) {
                this.lightPrev.lightNext = this.lightNext;
            } else {
                this.lightDef.firstInteraction = this.lightNext;
            }
            if (this.lightNext != null) {
                this.lightNext.lightPrev = this.lightPrev;
            } else {
                this.lightDef.lastInteraction = this.lightPrev;
            }
            this.lightNext = this.lightPrev = null;
        }

        // try to determine if the entire interaction, including shadows, is guaranteed
        // to be outside the view frustum
        private boolean CullInteractionByViewFrustum(final idFrustum viewFrustum) {

            if (!r_useInteractionCulling.GetBool()) {
                return false;
            }

            if (this.frustumState == FRUSTUM_INVALID) {
                return false;
            }

            if (this.frustumState == FRUSTUM_UNINITIALIZED) {

                this.frustum.FromProjection(new idBox(this.entityDef.referenceBounds, this.entityDef.parms.origin, this.entityDef.parms.axis), this.lightDef.globalLightOrigin, MAX_WORLD_SIZE);

                if (!this.frustum.IsValid()) {
                    this.frustumState = FRUSTUM_INVALID;
                    return false;
                }

                if (this.lightDef.parms.pointLight) {
                    this.frustum.ConstrainToBox(new idBox(this.lightDef.parms.origin, this.lightDef.parms.lightRadius, this.lightDef.parms.axis));
                } else {
                    this.frustum.ConstrainToBox(new idBox(this.lightDef.frustumTris.bounds));
                }

                this.frustumState = FRUSTUM_VALID;
            }

            if (!viewFrustum.IntersectsFrustum(this.frustum)) {
                return true;
            }

            if (r_showInteractionFrustums.GetInteger() != 0) {
                tr.viewDef.renderWorld.DebugFrustum(colors[this.lightDef.index & 7], this.frustum, (r_showInteractionFrustums.GetInteger() > 1));
                if (r_showInteractionFrustums.GetInteger() > 2) {
                    tr.viewDef.renderWorld.DebugBox(colorWhite, new idBox(this.entityDef.referenceBounds, this.entityDef.parms.origin, this.entityDef.parms.axis));
                }
            }

            return false;
        }
        private static final idVec4[] colors = {colorRed, colorGreen, colorBlue, colorYellow, colorMagenta, colorCyan, colorWhite, colorPurple};

        // determine the minimum scissor rect that will include the interaction shadows
        // projected to the bounds of the light
        private idScreenRect CalcInteractionScissorRectangle(final idFrustum viewFrustum) {
            final idBounds projectionBounds = new idBounds();
            idScreenRect portalRect = new idScreenRect();
            idScreenRect scissorRect;

            if (r_useInteractionScissors.GetInteger() == 0) {
                return this.lightDef.viewLight.scissorRect;
            }

            if (r_useInteractionScissors.GetInteger() < 0) {
                // this is the code from Cass at nvidia, it is more precise, but slower
                return R_CalcIntersectionScissor(this.lightDef, this.entityDef, tr.viewDef);
            }

            // the following is Mr.E's code
            // frustum must be initialized and valid
            if ((this.frustumState == FRUSTUM_UNINITIALIZED) || (this.frustumState == FRUSTUM_INVALID)) {
                return this.lightDef.viewLight.scissorRect;
            }

            // calculate scissors for the portals through which the interaction is visible
            if (r_useInteractionScissors.GetInteger() > 1) {
                areaNumRef_s area;

                if (this.frustumState == FRUSTUM_VALID) {
                    // retrieve all the areas the interaction frustum touches
                    for (areaReference_s ref = this.entityDef.entityRefs; ref != null; ref = ref.ownerNext) {
                        area = new areaNumRef_s();//entityDef.world.areaNumRefAllocator.Alloc();
                        area.areaNum = ref.area.areaNum;
                        area.next = this.frustumAreas;
                        this.frustumAreas = area;
                    }
                    this.frustumAreas = tr.viewDef.renderWorld.FloodFrustumAreas(this.frustum, this.frustumAreas);
                    this.frustumState = FRUSTUM_VALIDAREAS;
                }

                portalRect.Clear();
                for (area = this.frustumAreas; area != null; area = area.next) {
                    portalRect.Union(this.entityDef.world.GetAreaScreenRect(area.areaNum));
                }
                portalRect.Intersect(this.lightDef.viewLight.scissorRect);
            } else {
                portalRect = this.lightDef.viewLight.scissorRect;
            }

            // early out if the interaction is not visible through any portals
            if (portalRect.IsEmpty()) {
                return portalRect;
            }

            // calculate bounds of the interaction frustum projected into the view frustum
            if (this.lightDef.parms.pointLight) {
                viewFrustum.ClippedProjectionBounds(this.frustum, new idBox(this.lightDef.parms.origin, this.lightDef.parms.lightRadius, this.lightDef.parms.axis), projectionBounds);
            } else {
                viewFrustum.ClippedProjectionBounds(this.frustum, new idBox(this.lightDef.frustumTris.bounds), projectionBounds);
            }

            if (projectionBounds.IsCleared()) {
                return portalRect;
            }

            // derive a scissor rectangle from the projection bounds
            scissorRect = R_ScreenRectFromViewFrustumBounds(projectionBounds);

            // intersect with the portal crossing scissor rectangle
            scissorRect.Intersect(portalRect);

            if (r_showInteractionScissors.GetInteger() > 0) {
                R_ShowColoredScreenRect(scissorRect, this.lightDef.index);
            }

            return scissorRect;
        }
    }

    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */
    /*
     ================
     R_CalcInteractionFacing

     Determines which triangles of the surface are facing towards the light origin.

     The facing array should be allocated with one extra index than
     the number of surface triangles, which will be used to handle dangling
     edge silhouettes.
     ================
     */
    static void R_CalcInteractionFacing(final idRenderEntityLocal ent, final srfTriangles_s tri, final idRenderLightLocal light, srfCullInfo_t cullInfo) {
        final idVec3 localLightOrigin = new idVec3();

        if (cullInfo.facing != null) {
            return;
        }

        R_GlobalPointToLocal(ent.modelMatrix, light.globalLightOrigin, localLightOrigin);

        final int numFaces = tri.getIndexes().getNumValues() / 3;

        if (NOT(tri.facePlanes) || !tri.facePlanesCalculated) {
            R_DeriveFacePlanes( /*const_cast<srfTriangles_s *>*/(tri));
        }

        cullInfo.facing = new byte[numFaces + 1];// R_StaticAlloc((numFaces + 1) * sizeof(cullInfo.facing[0]));

        // calculate back face culling
        final float[] planeSide = new float[numFaces];

        // exact geometric cull against face
        SIMDProcessor.Dot(planeSide, localLightOrigin, tri.facePlanes, numFaces);
        SIMDProcessor.CmpGE(cullInfo.facing, planeSide, 0.0f, numFaces);

        cullInfo.facing[ numFaces] = 1;	// for dangling edges to reference
    }

    /*
     =====================
     R_CalcInteractionCullBits

     We want to cull a little on the sloppy side, because the pre-clipping
     of geometry to the lights in dmap will give many cases that are right
     at the border we throw things out on the border, because if any one
     vertex is clearly inside, the entire triangle will be accepted.
     =====================
     */
    static void R_CalcInteractionCullBits(final idRenderEntityLocal ent, final srfTriangles_s tri, final idRenderLightLocal light, srfCullInfo_t cullInfo) {
        int i, frontBits;

        if (cullInfo.cullBits != null) {
            return;
        }

        frontBits = 0;

        // cull the triangle surface bounding box
        for (i = 0; i < 6; i++) {

            R_GlobalPlaneToLocal(ent.modelMatrix, light.frustum[i].oNegative(), cullInfo.localClipPlanes[i]);

            // get front bits for the whole surface
            if (tri.bounds.PlaneDistance(cullInfo.localClipPlanes[i]) >= LIGHT_CLIP_EPSILON) {
                frontBits |= 1 << i;
            }
        }

        // if the surface is completely inside the light frustum
        if (frontBits == ((1 << 6) - 1)) {
            cullInfo.cullBits = LIGHT_CULL_ALL_FRONT;
            return;
        }

        cullInfo.cullBits = new byte[tri.numVerts];// R_StaticAlloc(tri.numVerts /* sizeof(cullInfo.cullBits[0])*/);
        SIMDProcessor.Memset(cullInfo.cullBits, 0, tri.numVerts /* sizeof(cullInfo.cullBits[0])*/);

        final float[] planeSide = new float[tri.numVerts];

        for (i = 0; i < 6; i++) {
            // if completely infront of this clipping plane
            if ((frontBits & (1 << i)) != 0) {
                continue;
            }
            SIMDProcessor.Dot(planeSide, cullInfo.localClipPlanes[i], tri.verts, tri.numVerts);
            SIMDProcessor.CmpLT(cullInfo.cullBits, (byte) i, planeSide, LIGHT_CLIP_EPSILON, tri.numVerts);
        }
    }

    /*
     ================
     R_FreeInteractionCullInfo
     ================
     */
    public static void R_FreeInteractionCullInfo(srfCullInfo_t cullInfo) {
//        if (cullInfo.facing != null) {
//            R_StaticFree(cullInfo.facing);
        cullInfo.facing = null;
//        }
//        if (cullInfo.cullBits != null) {
//            if (cullInfo.cullBits != LIGHT_CULL_ALL_FRONT) {
//                R_StaticFree(cullInfo.cullBits);
//            }
        cullInfo.cullBits = null;
//        }
    }
    static final int MAX_CLIPPED_POINTS = 20;

    static class clipTri_t {

        int numVerts;
        idVec3[] verts = new idVec3[MAX_CLIPPED_POINTS];
    }

    /*
     =============
     R_ChopWinding

     Clips a triangle from one buffer to another, setting edge flags
     The returned buffer may be the same as inNum if no clipping is done
     If entirely clipped away, clipTris[returned].numVerts == 0

     I have some worries about edge flag cases when polygons are clipped
     multiple times near the epsilon.
     =============
     */
    static int R_ChopWinding(clipTri_t[] clipTris/*[2]*/, int inNum, final idPlane plane) {
        clipTri_t in, out;
        final float[] dists = new float[MAX_CLIPPED_POINTS];
        final int[] sides = new int[MAX_CLIPPED_POINTS];
        final int[] counts = new int[3];
        float dot;
        int i, j;
        final idVec3 mid = new idVec3();
        boolean front;

        in = clipTris[inNum];
        out = clipTris[inNum ^ 1];
        counts[0] = counts[1] = counts[2] = 0;

        // determine sides for each point
        front = false;
        for (i = 0; i < in.numVerts; i++) {
            dot = in.verts[i].oMultiply(plane.Normal()) + plane.oGet(3);
            dists[i] = dot;
            if (dot < LIGHT_CLIP_EPSILON) {	// slop onto the back
                sides[i] = SIDE_BACK;
            } else {
                sides[i] = SIDE_FRONT;
                if (dot > LIGHT_CLIP_EPSILON) {
                    front = true;
                }
            }
            counts[sides[i]]++;
        }

        // if none in front, it is completely clipped away
        if (!front) {
            in.numVerts = 0;
            return inNum;
        }
        if (0 == counts[SIDE_BACK]) {
            return inNum;		// inout stays the same
        }

        // avoid wrapping checks by duplicating first value to end
        sides[i] = sides[0];
        dists[i] = dists[0];
        in.verts[in.numVerts] = in.verts[0];

        out.numVerts = 0;
        for (i = 0; i < in.numVerts; i++) {
            final idVec3 p1 = in.verts[i];

            if (sides[i] == SIDE_FRONT) {
                out.verts[out.numVerts] = p1;
                out.numVerts++;
            }

            if (sides[i + 1] == sides[i]) {
                continue;
            }

            // generate a split point
            final idVec3 p2 = in.verts[i + 1];

            dot = dists[i] / (dists[i] - dists[i + 1]);
            for (j = 0; j < 3; j++) {
                mid.oSet(j, p1.oGet(j) + (dot * (p2.oGet(j) - p1.oGet(j))));
            }

            out.verts[out.numVerts] = mid;

            out.numVerts++;
        }

        return inNum ^ 1;
    }

    /*
     ===================
     R_ClipTriangleToLight

     Returns false if nothing is left after clipping
     ===================
     */
    static boolean R_ClipTriangleToLight(final idVec3 a, final idVec3 b, final idVec3 c, int planeBits, final idPlane[] frustum/*[6]*/) {
        int i;
        final clipTri_t[] pingPong = new clipTri_t[2];
        int p;

        pingPong[0].numVerts = 3;
        pingPong[0].verts[0] = a;
        pingPong[0].verts[1] = b;
        pingPong[0].verts[2] = c;

        p = 0;
        for (i = 0; i < 6; i++) {
            if ((planeBits & (1 << i)) != 0) {
                p = R_ChopWinding(pingPong, p, frustum[i]);
                if (pingPong[p].numVerts < 1) {
                    return false;
                }
            }
        }

        return true;
    }

    /*
     ====================
     R_CreateLightTris

     The resulting surface will be a subset of the original triangles,
     it will never clip triangles, but it may cull on a per-triangle basis.
     ====================
     */
    static srfTriangles_s R_CreateLightTris(final idRenderEntityLocal ent,
            final srfTriangles_s tri, final idRenderLightLocal light,
            final idMaterial shader, srfCullInfo_t cullInfo) {
        int i;
        int numIndexes;
        int/*glIndex_t*/[] indexes;
        srfTriangles_s newTri;
        int c_backfaced;
        int c_distance;
        idBounds bounds = new idBounds();
        boolean includeBackFaces;
        int faceNum;

        tr.pc.c_createLightTris++;
        c_backfaced = 0;
        c_distance = 0;

        numIndexes = 0;
//	indexes = null;

        // it is debatable if non-shadowing lights should light back faces. we aren't at the moment
        if (r_lightAllBackFaces.GetBool() || light.lightShader.LightEffectsBackSides()
                || shader.ReceivesLightingOnBackSides()
                || ent.parms.noSelfShadow || ent.parms.noShadow) {
            includeBackFaces = true;
        } else {
            includeBackFaces = false;
        }

        // allocate a new surface for the lit triangles
        newTri = R_AllocStaticTriSurf();

        // save a reference to the original surface
        newTri.ambientSurface = /*const_cast<srfTriangles_s *>*/ (tri);

        // the light surface references the verts of the ambient surface
        newTri.numVerts = tri.numVerts;
        R_ReferenceStaticTriSurfVerts(newTri, tri);

        // calculate cull information
        if (!includeBackFaces) {
            R_CalcInteractionFacing(ent, tri, light, cullInfo);
        }
        R_CalcInteractionCullBits(ent, tri, light, cullInfo);

        // if the surface is completely inside the light frustum
        if (cullInfo.cullBits == LIGHT_CULL_ALL_FRONT) {

            // if we aren't self shadowing, let back facing triangles get
            // through so the smooth shaded bump maps light all the way around
            if (includeBackFaces) {

                // the whole surface is lit so the light surface just references the indexes of the ambient surface
                R_ReferenceStaticTriSurfIndexes(newTri, tri);
                numIndexes = tri.getIndexes().getNumValues();
                bounds = new idBounds(tri.bounds);

            } else {

                // the light tris indexes are going to be a subset of the original indexes so we generally
                // allocate too much memory here but we decrease the memory block when the number of indexes is known
                R_AllocStaticTriSurfIndexes(newTri, tri.getIndexes().getNumValues());

                // back face cull the individual triangles
                indexes = newTri.getIndexes().getValues();
                final byte[] facing = cullInfo.facing;
                for (faceNum = i = 0; i < tri.getIndexes().getNumValues(); i += 3, faceNum++) {
                    if (0 == facing[ faceNum]) {
                        c_backfaced++;
                        continue;
                    }
                    indexes[numIndexes + 0] = tri.getIndexes().getValues()[i + 0];
                    indexes[numIndexes + 1] = tri.getIndexes().getValues()[i + 1];
                    indexes[numIndexes + 2] = tri.getIndexes().getValues()[i + 2];
                    numIndexes += 3;
                }

                // get bounds for the surface
                SIMDProcessor.MinMax(bounds.oGet(0), bounds.oGet(1), tri.verts, indexes, numIndexes);

                // decrease the size of the memory block to the size of the number of used indexes
                R_ResizeStaticTriSurfIndexes(newTri, numIndexes);
            }

        } else {

            // the light tris indexes are going to be a subset of the original indexes so we generally
            // allocate too much memory here but we decrease the memory block when the number of indexes is known
            R_AllocStaticTriSurfIndexes(newTri, tri.getIndexes().getNumValues());

            // cull individual triangles
            indexes = newTri.getIndexes().getValues();
            final byte[] facing = cullInfo.facing;
            final byte[] cullBits = cullInfo.cullBits;
            for (faceNum = i = 0; i < tri.getIndexes().getNumValues(); i += 3, faceNum++) {
                int i1, i2, i3;

                // if we aren't self shadowing, let back facing triangles get
                // through so the smooth shaded bump maps light all the way around
                if (!includeBackFaces) {
                    // back face cull
                    if (0 == facing[ faceNum]) {
                        c_backfaced++;
                        continue;
                    }
                }

                i1 = tri.getIndexes().getValues()[i + 0];
                i2 = tri.getIndexes().getValues()[i + 1];
                i3 = tri.getIndexes().getValues()[i + 2];

                // fast cull outside the frustum
                // if all three points are off one plane side, it definately isn't visible
                if ((cullBits[i1] & cullBits[i2] & cullBits[i3]) != 0) {
                    c_distance++;
                    continue;
                }

                if (r_usePreciseTriangleInteractions.GetBool()) {
                    // do a precise clipped cull if none of the points is completely inside the frustum
                    // note that we do not actually use the clipped triangle, which would have Z fighting issues.
                    if ((cullBits[i1] & cullBits[i2] & cullBits[i3]) != 0) {
                        final int cull = cullBits[i1] | cullBits[i2] | cullBits[i3];
                        if (!R_ClipTriangleToLight(tri.verts[i1].xyz, tri.verts[i2].xyz, tri.verts[i3].xyz, cull, cullInfo.localClipPlanes)) {
                            continue;
                        }
                    }
                }

                // add to the list
                indexes[numIndexes + 0] = i1;
                indexes[numIndexes + 1] = i2;
                indexes[numIndexes + 2] = i3;
                numIndexes += 3;
            }

            // get bounds for the surface
            SIMDProcessor.MinMax(bounds.oGet(0), bounds.oGet(1), tri.verts, indexes, numIndexes);

            // decrease the size of the memory block to the size of the number of used indexes
            R_ResizeStaticTriSurfIndexes(newTri, numIndexes);
        }

        if (0 == numIndexes) {
            R_ReallyFreeStaticTriSurf(newTri);
            return null;
        }

        newTri.getIndexes().setNumValues(numIndexes);

        newTri.bounds.oSet(bounds);

        return newTri;
    }

    /*
     ===================
     R_ShowInteractionMemory_f
     ===================
     */
    static class R_ShowInteractionMemory_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ShowInteractionMemory_f();

        private R_ShowInteractionMemory_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int total = 0;
            int entities = 0;
            int interactions = 0;
            int deferredInteractions = 0;
            int emptyInteractions = 0;
            int lightTris = 0;
            int lightTriVerts = 0;
            int lightTriIndexes = 0;
            int shadowTris = 0;
            int shadowTriVerts = 0;
            int shadowTriIndexes = 0;

            for (int i = 0; i < tr.primaryWorld.entityDefs.Num(); i++) {
                final idRenderEntityLocal def = tr.primaryWorld.entityDefs.oGet(i);
                if (NOT(def)) {
                    continue;
                }
                if (def.firstInteraction == null) {
                    continue;
                }
                entities++;

                for (idInteraction inter = def.firstInteraction; inter != null; inter = inter.entityNext) {
                    interactions++;
                    total += inter.MemoryUsed();

                    if (inter.IsDeferred()) {
                        deferredInteractions++;
                        continue;
                    }
                    if (inter.IsEmpty()) {
                        emptyInteractions++;
                        continue;
                    }

                    for (int j = 0; j < inter.numSurfaces; j++) {
                        final surfaceInteraction_t srf = inter.surfaces[j];

                        if ((srf.lightTris != null) && (srf.lightTris != LIGHT_TRIS_DEFERRED)) {
                            lightTris++;
                            lightTriVerts += srf.lightTris.numVerts;
                            lightTriIndexes += srf.lightTris.getIndexes().getNumValues();
                        }
                        if (srf.shadowTris != null) {
                            shadowTris++;
                            shadowTriVerts += srf.shadowTris.numVerts;
                            shadowTriIndexes += srf.shadowTris.getIndexes().getNumValues();
                        }
                    }
                }
            }

            common.Printf("%d entities with %d total interactions totalling %dk\n", entities, interactions, total / 1024);
            common.Printf("%d deferred interactions, %d empty interactions\n", deferredInteractions, emptyInteractions);
            common.Printf("%5d indexes %5d verts in %5d light tris\n", lightTriIndexes, lightTriVerts, lightTris);
            common.Printf("%5d indexes %5d verts in %5d shadow tris\n", shadowTriIndexes, shadowTriVerts, shadowTris);
        }
    }

    /*
     ======================
     R_PotentiallyInsideInfiniteShadow

     If we know that we are "off to the side" of an infinite shadow volume,
     we can draw it without caps in zpass mode
     ======================
     */
    static boolean R_PotentiallyInsideInfiniteShadow(final srfTriangles_s occluder, final idVec3 localView, final idVec3 localLight) {
        final idBounds exp = new idBounds();

        // expand the bounds to account for the near clip plane, because the
        // view could be mathematically outside, but if the near clip plane
        // chops a volume edge, the zpass rendering would fail.
        float znear = r_znear.GetFloat();
        if (tr.viewDef.renderView.cramZNear) {
            znear *= 0.25f;
        }
        final float stretch = znear * 2;	// in theory, should vary with FOV
        exp.oSet(0, 0, occluder.bounds.oGet(0, 0) - stretch);
        exp.oSet(0, 1, occluder.bounds.oGet(0, 1) - stretch);
        exp.oSet(0, 2, occluder.bounds.oGet(0, 2) - stretch);
        exp.oSet(1, 0, occluder.bounds.oGet(1, 0) + stretch);
        exp.oSet(1, 1, occluder.bounds.oGet(1, 1) + stretch);
        exp.oSet(1, 2, occluder.bounds.oGet(1, 2) + stretch);

        if (exp.ContainsPoint(localView)) {
            return true;
        }
        if (exp.ContainsPoint(localLight)) {
            return true;
        }

        // if the ray from localLight to localView intersects a face of the
        // expanded bounds, we will be inside the projection
        final idVec3 ray = localView.oMinus(localLight);

        // intersect the ray from the view to the light with the near side of the bounds
        for (int axis = 0; axis < 3; axis++) {
            float d, frac;
            idVec3 hit;
            final float eza = exp.oGet(0, axis);
            final float ezo = exp.oGet(1, axis);//eoa
            final float l_axis = localLight.oGet(axis);

            if (l_axis < eza) {
                if (localView.oGet(axis) < eza) {
                    continue;
                }
                d = eza - l_axis;
                frac = d / ray.oGet(axis);
                hit = localLight.oPlus(ray.oMultiply(frac));
                hit.oSet(axis, eza);
            } else if (l_axis > ezo) {
                if (localView.oGet(axis) > ezo) {
                    continue;
                }
                d = ezo - l_axis;
                frac = d / ray.oGet(axis);
                hit = localLight.oPlus(ray.oMultiply(frac));
                hit.oSet(axis, ezo);
            } else {
                continue;
            }

            if (exp.ContainsPoint(hit)) {
                return true;
            }
        }

        // the view is definitely not inside the projected shadow
        return false;
    }
}
