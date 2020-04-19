package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Image.textureDepth_t.TD_DEFAULT;
import static neo.Renderer.Image_files.R_WriteTGA;
import static neo.Renderer.Material.SS_GUI;
import static neo.Renderer.Material.textureFilter_t.TF_DEFAULT;
import static neo.Renderer.Material.textureRepeat_t.TR_REPEAT;
import static neo.Renderer.MegaTexture.RoundDownToPowerOfTwo;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderSystem.BIGCHAR_HEIGHT;
import static neo.Renderer.RenderSystem.BIGCHAR_WIDTH;
import static neo.Renderer.RenderSystem.GLYPHS_PER_FONT;
import static neo.Renderer.RenderSystem.GLYPH_END;
import static neo.Renderer.RenderSystem.GLYPH_START;
import static neo.Renderer.RenderSystem.R_CheckCvars;
import static neo.Renderer.RenderSystem.R_GetCommandBuffer;
import static neo.Renderer.RenderSystem.R_IssueRenderCommands;
import static neo.Renderer.RenderSystem.R_PerformanceCounters;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderSystem.SMALLCHAR_HEIGHT;
import static neo.Renderer.RenderSystem.SMALLCHAR_WIDTH;
import static neo.Renderer.RenderSystem_init.GL_CheckErrors;
import static neo.Renderer.RenderSystem_init.R_InitCommands;
import static neo.Renderer.RenderSystem_init.R_InitCvars;
import static neo.Renderer.RenderSystem_init.R_InitMaterials;
import static neo.Renderer.RenderSystem_init.R_InitOpenGL;
import static neo.Renderer.RenderSystem_init.R_ReadTiledPixels;
import static neo.Renderer.RenderSystem_init.R_SetColorMappings;
import static neo.Renderer.RenderSystem_init.r_frontBuffer;
import static neo.Renderer.RenderSystem_init.r_renderer;
import static neo.Renderer.RenderSystem_init.r_screenFraction;
import static neo.Renderer.RenderSystem_init.r_showDemo;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.tr_backend.RB_ShowImages;
import static neo.Renderer.tr_font.BUILD_FREETYPE;
import static neo.Renderer.tr_font.R_DoneFreeType;
import static neo.Renderer.tr_font.fdFile;
import static neo.Renderer.tr_font.fdOffset;
import static neo.Renderer.tr_font.readFloat;
import static neo.Renderer.tr_font.readInt;
import static neo.Renderer.tr_local.backEndName_t.BE_ARB;
import static neo.Renderer.tr_local.backEndName_t.BE_ARB2;
import static neo.Renderer.tr_local.backEndName_t.BE_BAD;
import static neo.Renderer.tr_local.backEndName_t.BE_NV10;
import static neo.Renderer.tr_local.backEndName_t.BE_NV20;
import static neo.Renderer.tr_local.backEndName_t.BE_R200;
import static neo.Renderer.tr_local.demoCommand_t.DC_CAPTURE_RENDER;
import static neo.Renderer.tr_local.demoCommand_t.DC_CROP_RENDER;
import static neo.Renderer.tr_local.demoCommand_t.DC_END_FRAME;
import static neo.Renderer.tr_local.demoCommand_t.DC_GUI_MODEL;
import static neo.Renderer.tr_local.demoCommand_t.DC_UNCROP_RENDER;
import static neo.Renderer.tr_local.renderCommand_t.RC_COPY_RENDER;
import static neo.Renderer.tr_local.renderCommand_t.RC_SET_BUFFER;
import static neo.Renderer.tr_local.renderCommand_t.RC_SWAP_BUFFERS;
import static neo.Renderer.tr_main.R_GlobalToNormalizedDeviceCoordinates;
import static neo.Renderer.tr_main.R_ShutdownFrameData;
import static neo.Renderer.tr_main.R_ToggleSmpFrame;
import static neo.Renderer.tr_rendertools.RB_ShutdownDebugTools;
import static neo.Renderer.tr_trisurf.R_InitTriSurfData;
import static neo.Renderer.tr_trisurf.R_ShutdownTriSurfData;
import static neo.TempDump.btoi;
import static neo.TempDump.ctos;
import static neo.TempDump.fprintf;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DemoFile.demoSystem_t.DS_RENDER;
import static neo.framework.EventLoop.eventLoop;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Session.session;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Text.Str.C_COLOR_DEFAULT;
import static neo.idlib.math.Vector.getVec3_zero;
import static neo.open.gl.QGL.qglGetError;
import static neo.open.gl.QGL.qglReadBuffer;
import static neo.open.gl.QGL.qglReadPixels;
import static neo.open.gl.QGLConstantsIfc.GL_BACK;
import static neo.open.gl.QGLConstantsIfc.GL_FRONT;
import static neo.open.gl.QGLConstantsIfc.GL_NO_ERROR;
import static neo.open.gl.QGLConstantsIfc.GL_RGB;
import static neo.open.gl.QGLConstantsIfc.GL_UNSIGNED_BYTE;
import static neo.sys.win_glimp.GLimp_Shutdown;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import neo.Renderer.Cinematic.idCinematic;
import neo.Renderer.GuiModel.idGuiModel;
import neo.Renderer.Image.idImage;
import neo.Renderer.Image.textureType_t;
import neo.Renderer.Interaction.idInteraction;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.stageVertexColor_t;
import neo.Renderer.Model.dominantTri_s;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.silEdge_t;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.ModelDecal.idRenderModelDecal;
import neo.Renderer.ModelOverlay.idRenderModelOverlay;
import neo.Renderer.RenderSystem.fontInfoEx_t;
import neo.Renderer.RenderSystem.fontInfo_t;
import neo.Renderer.RenderSystem.glconfig_s;
import neo.Renderer.RenderSystem.glyphInfo_t;
import neo.Renderer.RenderSystem.idRenderSystem;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Renderer.RenderWorld_local.doublePortal_s;
import neo.Renderer.RenderWorld_local.idRenderWorldLocal;
import neo.Renderer.RenderWorld_local.portalArea_s;
import neo.Renderer.VertexCache.vertCache_s;
import neo.framework.Common.MemInfo_t;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Frustum.idFrustum;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.open.NeoIntBuffer;
import neo.open.Nio;

/**
 *
 */
public class tr_local {
    // everything that is needed by the backend needs
    // to be double buffered to allow it to run in
    // parallel on a dual cpu machine

    final static int SMP_FRAMES = 1;
    //
    final static int FALLOFF_TEXTURE_SIZE = 64;
    //
    final static float DEFAULT_FOG_DISTANCE = 500.0f;
    //
    final static int FOG_ENTER_SIZE = 64;
    final static float FOG_ENTER = (FOG_ENTER_SIZE + 1.0f) / (FOG_ENTER_SIZE * 2);
    // picky to get the bilerp correct at terminator

    // idScreenRect gets carried around with each drawSurf, so it makes sense
    // to keep it compact, instead of just using the idBounds class
    public static class idScreenRect {

        public int x1, y1, x2, y2;					// inclusive pixel bounds inside viewport
        public float zmin, zmax;					// for depth bounds test

        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;
        //
        //

        public idScreenRect() {
        }

        //copy constructor
        public idScreenRect(final idScreenRect other) {
            this.x1 = other.x1;
            this.y1 = other.y1;
            this.x2 = other.x2;
            this.y2 = other.y2;
            this.zmin = other.zmin;
            this.zmax = other.zmax;
        }

        // clear to backwards values
        public void Clear() {
            this.x1 = this.y1 = 32000;
            this.x2 = this.y2 = -32000;
            this.zmin = 0.0f;
            this.zmax = 1.0f;
        }

        public void AddPoint(float x, float y) {			// adds a point
            final short ix = (short) idMath.FtoiFast(x);
            final short iy = (short) idMath.FtoiFast(y);

            if (ix < this.x1) {
                this.x1 = ix;
            }
            if (ix > this.x2) {
                this.x2 = ix;
            }
            if (iy < this.y1) {
                this.y1 = iy;
            }
            if (iy > this.y2) {
                this.y2 = iy;
            }
        }

        public void Expand() {								// expand by one pixel each way to fix roundoffs
            this.x1--;
            this.y1--;
            this.x2++;
            this.y2++;
        }

        public void Intersect(final idScreenRect rect) {
            if (rect.x1 > this.x1) {
                this.x1 = rect.x1;
            }
            if (rect.x2 < this.x2) {
                this.x2 = rect.x2;
            }
            if (rect.y1 > this.y1) {
                this.y1 = rect.y1;
            }
            if (rect.y2 < this.y2) {
                this.y2 = rect.y2;
            }
        }

        public void Union(final idScreenRect rect) {
            if (rect.x1 < this.x1) {
                this.x1 = rect.x1;
            }
            if (rect.x2 > this.x2) {
                this.x2 = rect.x2;
            }
            if (rect.y1 < this.y1) {
                this.y1 = rect.y1;
            }
            if (rect.y2 > this.y2) {
                this.y2 = rect.y2;
            }
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = (47 * hash) + this.x1;
            hash = (47 * hash) + this.y1;
            hash = (47 * hash) + this.x2;
            hash = (47 * hash) + this.y2;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final idScreenRect other = (idScreenRect) obj;
            if (this.x1 != other.x1) {
                return false;
            }
            if (this.y1 != other.y1) {
                return false;
            }
            if (this.x2 != other.x2) {
                return false;
            }
            if (this.y2 != other.y2) {
                return false;
            }
            return true;
        }

        @Deprecated
        public boolean Equals(final idScreenRect rect) {
            return ((this.x1 == rect.x1) && (this.x2 == rect.x2) && (this.y1 == rect.y1) && (this.y2 == rect.y2));
        }

        public boolean IsEmpty() {
            return ((this.x1 > this.x2) || (this.y1 > this.y2));
        }

        @Override
        public String toString() {
            return "idScreenRect{" + "x1=" + this.x1 + ", y1=" + this.y1 + ", x2=" + this.x2 + ", y2=" + this.y2 + '}';
        }

        static idScreenRect[] generateArray(final int length) {
            return Stream.
                    generate(idScreenRect::new).
                    limit(length).
                    toArray(idScreenRect[]::new);
        }
    }

    enum demoCommand_t {

        DC_BAD,
        DC_RENDERVIEW,
        DC_UPDATE_ENTITYDEF,
        DC_DELETE_ENTITYDEF,
        DC_UPDATE_LIGHTDEF,
        DC_DELETE_LIGHTDEF,
        DC_LOADMAP,
        DC_CROP_RENDER,
        DC_UNCROP_RENDER,
        DC_CAPTURE_RENDER,
        DC_END_FRAME,
        DC_DEFINE_MODEL,
        DC_SET_PORTAL_STATE,
        DC_UPDATE_SOUNDOCCLUSION,
        DC_GUI_MODEL
    }
    /*
     ==============================================================================

     SURFACES

     ==============================================================================
     */
//
//
//
// drawSurf_t structures command the back end to render surfaces
// a given srfTriangles_t may be used with multiple viewEntity_t,
// as when viewed in a subview or multiple viewport render, or
// with multiple shaders when skinned, or, possibly with multiple
// lights, although currently each lighting interaction creates
// unique srfTriangles_t
// drawSurf_t are always allocated and freed every frame, they are never cached
    static final int DSF_VIEW_INSIDE_SHADOW = 1;

    public static class drawSurf_s {

        public srfTriangles_s geo;
        public viewEntity_s   space;
        public idMaterial     material;             // may be NULL for shadow volumes
        public float          sort;                 // material->sort, modified by gui / entity sort offsets
        public float[]        shaderRegisters;      // evaluated and adjusted for referenceShaders
        public drawSurf_s     nextOnLight;          // viewLight chains
        public idScreenRect   scissorRect;          // for scissor clipping, local inside renderView viewport
        public int            dsFlags;              // DSF_VIEW_INSIDE_SHADOW, etc
        public vertCache_s    dynamicTexCoords;     // float * in vertex cache memory
        // specular directions for non vertex program cards, skybox texcoords, etc

        private static int DBG_counter = 0;
        private final  int DBG_count   = DBG_counter++;
    }

    public static class shadowFrustum_t {

        int numPlanes;		// this is always 6 for now
        idPlane[] planes = new idPlane[6];
        // positive sides facing inward
        // plane 5 is always the plane the projection is going to, the
        // other planes are just clip planes
        // all planes are in global coordinates
//
        boolean makeClippedPlanes;
        // a projected light with a single frustum needs to make sil planes
        // from triangles that clip against side planes, but a point light
        // that has adjacent frustums doesn't need to

        public shadowFrustum_t() {
            for (int p = 0; p < this.planes.length; p++) {
                this.planes[p] = new idPlane();
            }
        }
    }

// areas have references to hold all the lights and entities in them
    public static class areaReference_s {

        areaReference_s areaNext;           // chain in the area
        areaReference_s areaPrev;
        areaReference_s ownerNext;          // chain on either the entityDef or lightDef
        idRenderEntityLocal entity;         // only one of entity / light will be non-NULL
        idRenderLightLocal light;           // only one of entity / light will be non-NULL
        portalArea_s area;                  // so owners can find all the areas they are in
    }

    // idRenderLight should become the new public interface replacing the qhandle_t to light defs in the idRenderWorld interface
    public static abstract class idRenderLight {

        // virtual					~idRenderLight() {}
        public abstract void FreeRenderLight();

        public abstract void UpdateRenderLight(final renderLight_s re, boolean forceUpdate/* = false*/);

        public void UpdateRenderLight(final renderLight_s re) {
            UpdateRenderLight(re, false);
        }

        public abstract void GetRenderLight(renderLight_s re);

        public abstract void ForceUpdate();

        public abstract int GetIndex();
    }

    // idRenderEntity should become the new public interface replacing the qhandle_t to entity defs in the idRenderWorld interface
    public static abstract class idRenderEntity {

        // virtual					~idRenderEntity() {}
        public abstract void FreeRenderEntity();

        public abstract void UpdateRenderEntity(final renderEntity_s re, boolean forceUpdate /*= false*/);

        public void UpdateRenderEntity(final renderEntity_s re) {
            UpdateRenderEntity(re, false);
        }

        public abstract void GetRenderEntity(renderEntity_s re);

        public abstract void ForceUpdate();

        public abstract int GetIndex();

        // overlays are extra polygons that deform with animating models for blood and damage marks
        public abstract void ProjectOverlay(final idPlane[] localTextureAxis/*[2]*/, final idMaterial material);

        public abstract void RemoveDecals();
    }

    public static class idRenderLightLocal extends idRenderLight {

        public renderLight_s parms;			// specification
//
        public boolean lightHasMoved;			// the light has changed its position since it was
//                                                      // first added, so the prelight model is not valid
//
        public float[] modelMatrix = new float[16];	// this is just a rearrangement of parms.axis and parms.origin
//
        public idRenderWorldLocal world;
        public int index;				// in world lightdefs
//
        public int areaNum;				// if not -1, we may be able to cull all the light's
//                                                      // interactions if !viewDef->connectedAreas[areaNum]
//
        public int lastModifiedFrameNum;                // to determine if it is constantly changing,
//                                                      // and should go in the dynamic frame memory, or kept
//                                                      // in the cached memory
        public boolean archived;			// for demo writing
//
//
        // derived information
        public idPlane[] lightProject = new idPlane[4];
//
        public idMaterial lightShader;			// guaranteed to be valid, even if parms.shader isn't
        public idImage falloffImage;
//
        public idVec3 globalLightOrigin;		// accounting for lightCenter and parallel
//
//
        public idPlane[] frustum = new idPlane[6];	// in global space, positive side facing out, last two are front/back
        public idWinding[] frustumWindings = new idWinding[6];// used for culling
        public srfTriangles_s frustumTris;		// triangulated frustumWindings[]
//
        public int numShadowFrustums;                   // one for projected lights, usually six for point lights
        public shadowFrustum_t[] shadowFrustums = new shadowFrustum_t[6];
//
        public int viewCount;				// if == tr.viewCount, the light is on the viewDef->viewLights list
        public viewLight_s viewLight;
//
        public areaReference_s references;		// each area the light is present in will have a lightRef
        public idInteraction firstInteraction;		// doubly linked list
        public idInteraction lastInteraction;
//
        public doublePortal_s foggedPortals;
        //
        //

        public idRenderLightLocal() {
            this.parms = new renderLight_s();//memset( & parms, 0, sizeof(parms));
//            memset(modelMatrix, 0, sizeof(modelMatrix));
//            memset(shadowFrustums, 0, sizeof(shadowFrustums));
            for (int s = 0; s < this.shadowFrustums.length; s++) {
                this.shadowFrustums[s] = new shadowFrustum_t();
            }
//            memset(lightProject, 0, sizeof(lightProject));
            for (int l = 0; l < this.lightProject.length; l++) {
                this.lightProject[l] = new idPlane();
            }
//            memset(frustum, 0, sizeof(frustum));
            for (int f = 0; f < this.frustum.length; f++) {
                this.frustum[f] = new idPlane();
            }
//            memset(frustumWindings, 0, sizeof(frustumWindings));
            for (int f = 0; f < this.frustumWindings.length; f++) {
                this.frustumWindings[f] = new idWinding();
            }

            this.lightHasMoved = false;
            this.world = null;
            this.index = 0;
            this.areaNum = 0;
            this.lastModifiedFrameNum = 0;
            this.archived = false;
            this.lightShader = null;
            this.falloffImage = null;
            this.globalLightOrigin = getVec3_zero();
            this.frustumTris = null;
            this.numShadowFrustums = 0;
            this.viewCount = 0;
            this.viewLight = null;
            this.references = null;
            this.foggedPortals = null;
            this.firstInteraction = null;
            this.lastInteraction = null;
        }

        @Override
        public void FreeRenderLight() {
        }

        @Override
        public void UpdateRenderLight(renderLight_s re, boolean forceUpdate) {
        }

        @Override
        public void GetRenderLight(renderLight_s re) {
        }

        @Override
        public void ForceUpdate() {
        }

        @Override
        public int GetIndex() {
            return this.index;
        }
    }

    public static class idRenderEntityLocal extends idRenderEntity {

        public renderEntity_s parms;
//
        public float[] modelMatrix = new float[16];	// this is just a rearrangement of parms.axis and parms.origin
//
        public idRenderWorldLocal world;
        public int index;				// in world entityDefs
//
        public int lastModifiedFrameNum;                // to determine if it is constantly changing,
        // and should go in the dynamic frame memory, or kept
        // in the cached memory
        public boolean archived;			// for demo writing
//
        public idRenderModel dynamicModel;		// if parms.model->IsDynamicModel(), this is the generated data
        public int dynamicModelFrameCount;              // continuously animating dynamic models will recreate
        // dynamicModel if this doesn't == tr.viewCount
        public idRenderModel cachedDynamicModel;
//
        public idBounds referenceBounds;		// the local bounds used to place entityRefs, either from parms or a model
//
        // a viewEntity_t is created whenever a idRenderEntityLocal is considered for inclusion
        // in a given view, even if it turns out to not be visible
        public int viewCount;				// if tr.viewCount == viewCount, viewEntity is valid,
        // but the entity may still be off screen
        public viewEntity_s viewEntity;			// in frame temporary memory
//
        public int visibleCount;
        // if tr.viewCount == visibleCount, at least one ambient
        // surface has actually been added by R_AddAmbientDrawsurfs
        // note that an entity could still be in the view frustum and not be visible due
        // to portal passing
//
        public idRenderModelDecal decals;		// chain of decals that have been projected on this model
        public idRenderModelOverlay overlay;		// blood overlays on animated models
//
        public areaReference_s entityRefs;		// chain of all references
        public idInteraction firstInteraction;		// doubly linked list
        public idInteraction lastInteraction;
        boolean needsPortalSky;
        //
        //
        private static int DBG_counter = 0;
        private final  int DBG_count = DBG_counter++;

        public idRenderEntityLocal() {
            this.parms = new renderEntity_s();//memset( parms, 0, sizeof( parms ) );
//	memset( modelMatrix, 0, sizeof( modelMatrix ) );

            this.world = null;
            this.index = 0;
            this.lastModifiedFrameNum = 0;
            this.archived = false;
            this.dynamicModel = null;
            this.dynamicModelFrameCount = 0;
            this.cachedDynamicModel = null;
            this.referenceBounds = new idBounds();//bounds_zero;//TODO:replace bounds_zero with something useful?
            this.viewCount = 0;
            this.viewEntity = null;
            this.visibleCount = 0;
            this.decals = null;
            this.overlay = null;
            this.entityRefs = null;
            this.firstInteraction = null;
            this.lastInteraction = null;
            this.needsPortalSky = false;
        }

        @Override
        public void FreeRenderEntity() {
            // @see https://github.com/dhewm/dhewm3/blob/master/neo/renderer/RenderEntity.cpp Method is empty        	
        	//throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void UpdateRenderEntity(renderEntity_s re, boolean forceUpdate) {
            // @see https://github.com/dhewm/dhewm3/blob/master/neo/renderer/RenderEntity.cpp Method is empty        	
        	//throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void GetRenderEntity(renderEntity_s re) {
            // @see https://github.com/dhewm/dhewm3/blob/master/neo/renderer/RenderEntity.cpp Method is empty        	
        	//throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void ForceUpdate() {
            // @see https://github.com/dhewm/dhewm3/blob/master/neo/renderer/RenderEntity.cpp Method is empty        	
        	//throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int GetIndex() {
            return this.index;
        }

        // overlays are extra polygons that deform with animating models for blood and damage marks
        @Override
        public void ProjectOverlay(idPlane[] localTextureAxis, idMaterial material) {
            // @see https://github.com/dhewm/dhewm3/blob/master/neo/renderer/RenderEntity.cpp Method is empty        	
        	//throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void RemoveDecals() {
            // @see https://github.com/dhewm/dhewm3/blob/master/neo/renderer/RenderEntity.cpp Method is empty        	
        	//throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    // viewLights are allocated on the frame temporary stack memory
    // a viewLight contains everything that the back end needs out of an idRenderLightLocal,
    // which the front end may be modifying simultaniously if running in SMP mode.
    // a viewLight may exist even without any surfaces, and may be relevent for fogging,
    // but should never exist if its volume does not intersect the view frustum
    public static class viewLight_s {

        public viewLight_s next;
//
        // back end should NOT reference the lightDef, because it can change when running SMP
        public idRenderLightLocal lightDef;
//
        // for scissor clipping, local inside renderView viewport
        // scissorRect.Empty() is true if the viewEntity_t was never actually
        // seen through any portals
        public idScreenRect scissorRect;
//
        // if the view isn't inside the light, we can use the non-reversed
        // shadow drawing, avoiding the draws of the front and rear caps
        public boolean viewInsideLight;
//
        // true if globalLightOrigin is inside the view frustum, even if it may
        // be obscured by geometry.  This allows us to skip shadows from non-visible objects
        public boolean viewSeesGlobalLightOrigin;
//
        // if !viewInsideLight, the corresponding bit for each of the shadowFrustum
        // projection planes that the view is on the negative side of will be set,
        // allowing us to skip drawing the projected caps of shadows if we can't see the face
        public int viewSeesShadowPlaneBits;
//
        public idVec3 globalLightOrigin;			// global light origin used by backend
        public idPlane[] lightProject = new idPlane[4];		// light project used by backend
        public idPlane fogPlane;				// fog plane for backend fog volume rendering
        public srfTriangles_s frustumTris;			// light frustum for backend fog volume rendering
        public idMaterial lightShader;				// light shader used by backend
        public float[] shaderRegisters;                         // shader registers used by backend
        public idImage falloffImage;				// falloff image used by backend
//
        public final drawSurf_s[] globalShadows = {null};           // shadow everything
        public final drawSurf_s[] localInteractions = {null};       // don't get local shadows
        public final drawSurf_s[] localShadows = {null};            // don't shadow local Surfaces
        public final drawSurf_s[] globalInteractions = {null};      // get shadows from everything
        public final drawSurf_s[] translucentInteractions = {null}; // get shadows from everything
    }

    /**
     * a viewEntity is created whenever a idRenderEntityLocal is considered for
     * inclusion in the current view, but it may still turn out to be culled.
     * viewEntity are allocated on the frame temporary stack memory a viewEntity
     * contains everything that the back end needs out of a idRenderEntityLocal,
     * which the front end may be modifying simultaneously if running in SMP
     * mode. A single entityDef can generate multiple {@link viewEntity_s} in a
     * single frame, as when seen in a mirror
     */
    public static class viewEntity_s {
        public viewEntity_s        next;
        //
        // back end should NOT reference the entityDef, because it can change when running SMP
        public idRenderEntityLocal entityDef;
        //
        // for scissor clipping, local inside renderView viewport
        // scissorRect.Empty() is true if the viewEntity_t was never actually
        // seen through any portals, but was created for shadow casting.
        // a viewEntity can have a non-empty scissorRect, meaning that an area
        // that it is in is visible, and still not be visible.
        public idScreenRect        scissorRect     = new idScreenRect();
        //
        public boolean             weaponDepthHack;
        public float               modelDepthHack;
        //
        public float[]             modelMatrix     = new float[16];         // local coords to global coords
        //private final float[]      modelViewMatrix = new float[16];         // local coords to eye coords
        //public FloatBuffer         modelMatrix     = Nio.newFloatBuffer(16);         // local coords to global coords
        private final FloatBuffer  modelViewMatrix = Nio.newFloatBuffer(16);         // local coords to eye coords

        private static int DBG_COUNTER = 0;
        private final  int DBG_COUNT   = DBG_COUNTER++;

        public viewEntity_s() {
//            TempDump.printCallStack("--------------"+DBG_COUNT);
        }

        public viewEntity_s(viewEntity_s v) {
            this.next = v.next;
            this.entityDef = v.entityDef;
            this.scissorRect = new idScreenRect(v.scissorRect);
            this.weaponDepthHack = v.weaponDepthHack;
            this.modelDepthHack = v.modelDepthHack;
            //System.arraycopy(v.modelMatrix, 0, this.modelMatrix, 0, 16);
            Nio.arraycopy(v.modelMatrix, 0, this.modelMatrix, 0, 16);
            //System.arraycopy(v.getModelViewMatrix(), 0, this.getModelViewMatrix(), 0, 16);
            Nio.buffercopy(v.getModelViewMatrix(), 0, this.getModelViewMatrix(), 0, 16);
        }

        public void memSetZero() {
            this.next = new viewEntity_s();
            this.entityDef = new idRenderEntityLocal();
            this.scissorRect = new idScreenRect();
            this.weaponDepthHack = false;
            this.modelDepthHack = 0;
        }

		public FloatBuffer getModelViewMatrix() {
			return modelViewMatrix;
		}

    }
    static final int MAX_CLIP_PLANES = 1;				// we may expand this to six for some subview issues

    // viewDefs are allocated on the frame temporary stack memory
    public static class viewDef_s {
        // specified in the call to DrawScene()

        public renderView_s       renderView;
//
        private final float[]     projectionMatrix = new float[16];
        //private final FloatBuffer projectionMatrix = Nio.newFloatBuffer(16);
        public viewEntity_s       worldSpace;
//
        public idRenderWorldLocal renderWorld;
//
        public float              floatTime;
//
        public idVec3             initialViewAreaOrigin;
        // Used to find the portalArea that view flooding will take place from.
        // for a normal view, the initialViewOrigin will be renderView.viewOrg,
        // but a mirror may put the projection origin outside
        // of any valid area, or in an unconnected area of the map, so the view
        // area must be based on a point just off the surface of the mirror / subview.
        // It may be possible to get a failed portal pass if the plane of the
        // mirror intersects a portal, and the initialViewAreaOrigin is on
        // a different side than the renderView.viewOrg is.
//
        public boolean            isSubview;             // true if this view is not the main view
        public boolean            isMirror;              // the portal is a mirror, invert the face culling
        public boolean            isXraySubview;
        //
        public boolean            isEditor;
        //
        public int                numClipPlanes;         // mirrors will often use a single clip plane
        public idPlane[]          clipPlanes;            // in world space, the positive side
                                                         // of the plane is the visible side
        public idScreenRect       viewport;              // in real pixels and proper Y flip
        //
        public idScreenRect       scissor;
        // for scissor clipping, local inside renderView viewport
        // subviews may only be rendering part of the main view
        // these are real physical pixel values, possibly scaled and offset from the
        // renderView x/y/width/height
//
        public viewDef_s          superView;             // never go into an infinite subview loop
        public drawSurf_s         subviewSurface;
//
        // drawSurfs are the visible surfaces of the viewEntities, sorted
        // by the material sort parameter
        public drawSurf_s[]       drawSurfs;             // we don't use an idList for this, because
        public int                numDrawSurfs;          // it is allocated in frame temporary memory
        public int                maxDrawSurfs;          // may be resized
//
        public viewLight_s        viewLights;            // chain of all viewLights effecting view
        public viewEntity_s       viewEntitys;           // chain of all viewEntities effecting view, including off screen ones casting shadows
        public int                numViewEntitys;
        public idPlane[]          frustum;
        public idFrustum          viewFrustum;
//
        public int                areaNum;               // -1 = not in a valid area
//
        public boolean[]          connectedAreas;
        // An array in frame temporary memory that lists if an area can be reached without
        // crossing a closed door.  This is used to avoid drawing interactions
        // when the light is behind a closed door.

        public viewDef_s() {
            this.renderView = new renderView_s();
            this.worldSpace = new viewEntity_s();
            this.clipPlanes = new idPlane[MAX_CLIP_PLANES];
            this.viewport = new idScreenRect();
            this.scissor = new idScreenRect();
            this.viewFrustum = new idFrustum();
            this.frustum = idPlane.generateArray(5);
        }

        public viewDef_s(final viewDef_s v) {
            this.renderView = new renderView_s(v.renderView);
            Nio.arraycopy(v.getProjectionMatrix(), 0, this.getProjectionMatrix(), 0, 16);
            this.worldSpace = new viewEntity_s(v.worldSpace);
            this.renderWorld = v.renderWorld;
            this.floatTime = v.floatTime;
            this.initialViewAreaOrigin = new idVec3(v.initialViewAreaOrigin);
            this.isSubview = v.isSubview;
            this.isMirror = v.isMirror;
            this.isXraySubview = v.isXraySubview;
            this.isEditor = v.isEditor;
            this.numClipPlanes = v.numClipPlanes;
            this.clipPlanes = new idPlane[MAX_CLIP_PLANES];
            for (int i = 0; i < MAX_CLIP_PLANES; i++) {
                if (v.clipPlanes[i] != null) {
					this.clipPlanes[i].oSet(v.clipPlanes[i]);
				}
            }
            this.viewport = new idScreenRect(v.viewport);
            this.scissor = new idScreenRect(v.scissor);
            this.superView = v.superView;
            this.subviewSurface = v.subviewSurface;
            this.drawSurfs = v.drawSurfs;
            this.numDrawSurfs = v.numDrawSurfs;
            this.maxDrawSurfs = v.maxDrawSurfs;
            this.viewLights = v.viewLights;
            this.viewEntitys = v.viewEntitys;
            this.frustum = Arrays.stream(v.frustum).map(idPlane::new).toArray(idPlane[]::new);
            this.viewFrustum = new idFrustum(v.viewFrustum);
            this.areaNum = v.areaNum;
            if (v.connectedAreas != null) {
                this.connectedAreas = new boolean[v.connectedAreas.length];
                Nio.arraycopy(v.connectedAreas, 0, this.connectedAreas, 0, v.connectedAreas.length);
            }
        }

		public float[] getProjectionMatrix() {
			return projectionMatrix;
		}
    }

// complex light / surface interactions are broken up into multiple passes of a
// simple interaction shader
    public static class drawInteraction_t {

        public drawSurf_s surf;
//
        public idImage lightImage;
        public idImage lightFalloffImage;
        public idImage bumpImage;
        public idImage diffuseImage;
        public idImage specularImage;
//
        public final idVec4 diffuseColor = new idVec4();        // may have a light color baked into it, will be < tr.backEndRendererMaxLight
        public final idVec4 specularColor = new idVec4();       // may have a light color baked into it, will be < tr.backEndRendererMaxLight
        public stageVertexColor_t vertexColor;                  // applies to both diffuse and specular
//
        public int ambientLight;                                // use tr.ambientNormalMap instead of normalization cube map
//                                                              // (not a bool just to avoid an uninitialized memory check of the pad region by valgrind)
//
        // these are loaded into the vertex program
        public final idVec4 localLightOrigin = new idVec4();
        public final idVec4 localViewOrigin = new idVec4();
        public idVec4[] lightProjection = new idVec4[4];	// in local coordinates, possibly with a texture matrix baked in
        public idVec4[] bumpMatrix = idVec4.generateArray(2);
        public idVec4[] diffuseMatrix = idVec4.generateArray(2);
        public idVec4[] specularMatrix = idVec4.generateArray(2);

        void oSet(drawInteraction_t d) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /*
     =============================================================

     RENDERER BACK END COMMAND QUEUE

     TR_CMDS

     =============================================================
     */
    enum renderCommand_t {

        RC_NOP,
        RC_DRAW_VIEW,
        RC_SET_BUFFER,
        RC_COPY_RENDER,
        RC_SWAP_BUFFERS	// can't just assume swap at end of list because  of forced list submission before syncs
    }

    static class emptyCommand_t {

        renderCommand_t commandId;
        emptyCommand_t next;

        public void oSet(final emptyCommand_t c) {
            this.commandId = c.commandId;
            this.next = c.next;
        }

        void oSet(renderCommand_t next) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    static class setBufferCommand_t extends emptyCommand_t {

//        renderCommand_t commandId, next;
        int/*GLenum*/ buffer;
        int frameCount;
    }

    static class drawSurfsCommand_t extends emptyCommand_t {

//        renderCommand_t commandId, next;
        viewDef_s viewDef;
    }

    static class copyRenderCommand_t extends emptyCommand_t {

//        renderCommand_t commandId, next;
        int x, y, imageWidth, imageHeight;
        idImage image;
        int cubeFace;					// when copying to a cubeMap
    }
//=======================================================================
    // this is the inital allocation for max number of drawsurfs
// in a given view, but it will automatically grow if needed
    static final int INITIAL_DRAWSURFS = 0x4000;

// a request for frame memory will never fail
// (until malloc fails), but it may force the
// allocation of a new memory block that will
// be discontinuous with the existing memory
    static class frameMemoryBlock_s {

        frameMemoryBlock_s next;
        int size;
        int used;
        int poop;			// so that base is 16 byte aligned
        byte[] base = new byte[4];	// dynamically allocated as [size]
    }

// all of the information needed by the back end must be
// contained in a frameData_t.  This entire structure is
// duplicated so the front and back end can run in parallel
// on an SMP machine (OBSOLETE: this capability has been removed)
    static class frameData_t {
        // one or more blocks of memory for all frame
        // temporary allocations

        frameMemoryBlock_s memory;
//
        // alloc will point somewhere into the memory chain
        frameMemoryBlock_s alloc;
//
        srfTriangles_s firstDeferredFreeTriSurf;
        srfTriangles_s lastDeferredFreeTriSurf;
//
        int memoryHighwater;	// max used on any frame
//
        // the currently building command list 
        // commands can be inserted at the front if needed, as for required
        // dynamically generated textures
        emptyCommand_t cmdHead, cmdTail;// may be of other command type based on commandId
    }
    public static frameData_t frameData;
//=======================================================================

    /*
     ** performanceCounters_t
     */
    static class performanceCounters_t {

        int c_sphere_cull_in, c_sphere_cull_clip, c_sphere_cull_out;
        int c_box_cull_in, c_box_cull_out;
        int c_createInteractions;// number of calls to idInteraction::CreateInteraction
        int c_createLightTris;
        int c_createShadowVolumes;
        int c_generateMd5;
        int c_entityDefCallbacks;
        int c_alloc, c_free;	// counts for R_StaticAllc/R_StaticFree
        int c_visibleViewEntities;
        int c_shadowViewEntities;
        int c_viewLights;
        int c_numViews;		// number of total views rendered
        int c_deformedSurfaces;	// idMD5Mesh::GenerateSurface
        int c_deformedVerts;	// idMD5Mesh::GenerateSurface
        int c_deformedIndexes;	// idMD5Mesh::GenerateSurface
        int c_tangentIndexes;	// R_DeriveTangents()
        int c_entityUpdates, c_lightUpdates, c_entityReferences, c_lightReferences;
        int c_guiSurfs;
        int frontEndMsec;	// sum of time in all RE_RenderScene's in a frame
    }

    static class tmu_t {

        int current2DMap;
        int current3DMap;
        int currentCubeMap;
        int texEnv;
        textureType_t textureType;
    }
    static final int MAX_MULTITEXTURE_UNITS = 8;

    static class glstate_t {

        tmu_t[] tmu;
        int currenttmu;
//
        int faceCulling;
        int glStateBits;
        boolean forceGlState;	// the next GL_State will ignore glStateBits and set everything

        glstate_t() {
            this.tmu = new tmu_t[MAX_MULTITEXTURE_UNITS];
            for (int a = 0; a < this.tmu.length; a++) {
                this.tmu[a] = new tmu_t();
            }
        }
    }

    static class backEndCounters_t {

        int c_surfaces;
        int c_shaders;
        int c_vertexes;
        int c_indexes;		// one set per pass
        int c_totalIndexes;	// counting all passes
//
        int c_drawElements;
        int c_drawIndexes;
        int c_drawVertexes;
        int c_drawRefIndexes;
        int c_drawRefVertexes;
//
        int c_shadowElements;
        int c_shadowIndexes;
        int c_shadowVertexes;
//
        int c_vboIndexes;
        float c_overDraw;
//
        float maxLightValue;	// for light scale
        int msec;		// total msec for backend run
    }

// all state modified by the back end is separated
// from the front end state
    static class backEndState_t {

        int frameCount;		// used to track all images used in a frame
        viewDef_s viewDef;
        backEndCounters_t pc;
//
        viewEntity_s currentSpace;		// for detecting when a matrix must change
        idScreenRect currentScissor;
        // for scissor clipping, local inside renderView viewport
//
        viewLight_s vLight;
        int depthFunc;			// GLS_DEPTHFUNC_EQUAL, or GLS_DEPTHFUNC_LESS for translucent
        private final FloatBuffer lightTextureMatrix = Nio.newFloatBuffer(16);	// only if lightStage->texture.hasMatrix
        private final FloatBuffer lightColor = Nio.newFloatBuffer(4);		// evaluation of current light's color stage
//
        float lightScale;			// Every light color calaculation will be multiplied by this,
        // which will guarantee that the result is < tr.backEndRendererMaxLight
        // A card with high dynamic range will have this set to 1.0
        float overBright;			// The amount that all light interactions must be multiplied by
        // with post processing to get the desired total light level.
        // A high dynamic range card will have this set to 1.0.
//
        boolean currentRenderCopied;	// true if any material has already referenced _currentRender
//
        // our OpenGL state deltas
        glstate_t glState;
//
        int c_copyFrameBuffer;

        backEndState_t() {
            this.pc = new backEndCounters_t();
            this.glState = new glstate_t();
        }

        FloatBuffer getLightColor() {
			return lightColor;
		}

		FloatBuffer getLightTextureMatrix() {
			return lightTextureMatrix;
		}

    }
    static final int MAX_GUI_SURFACES = 1024;		// default size of the drawSurfs list for guis, will be automatically expanded as needed

    enum backEndName_t {

        BE_ARB,
        BE_NV10,
        BE_NV20,
        BE_R200,
        BE_ARB2,
        BE_BAD
    }

    static class renderCrop_t {

        public int x, y, width, height;	// these are in physical, OpenGL Y-at-bottom pixels
    }
    static final int MAX_RENDER_CROPS = 8;

    /*
     ** Most renderer globals are defined here.
     ** backend functions should never modify any of these fields,
     ** but may read fields that aren't dynamically modified
     ** by the frontend.
     */
    public static class idRenderSystemLocal extends idRenderSystem {

        // renderer globals
        public boolean registered;		// cleared at shutdown, set at InitOpenGL
        //
        public boolean takingScreenshot;
        //
        public int frameCount;                  // incremented every frame
        public int viewCount;                   // incremented every view (twice a scene if subviewed)
        public int DBG_viewCount;                   // incremented every view (twice a scene if subviewed)
        // and every R_MarkFragments call
        //
        public int staticAllocCount;            // running total of bytes allocated
        //
        public float frameShaderTime;           // shader time for all non-world 2D rendering
        //
        public int[] viewportOffset = new int[2];// for doing larger-than-window tiled renderings
        public int[] tiledViewport = new int[2];
        //
        // determines which back end to use, and if vertex programs are in use
        public backEndName_t backEndRenderer;
        public boolean backEndRendererHasVertexPrograms;
        public float backEndRendererMaxLight;	// 1.0 for standard, unlimited for floats
        // determines how much overbrighting needs
        // to be done post-process
        //
        public idVec4 ambientLightVector;	// used for "ambient bump mapping"
        //
        public float sortOffset;		// for determinist sorting of equal sort materials
        //
        public idList<idRenderWorldLocal> worlds;
        //
        public idRenderWorldLocal primaryWorld;
        public renderView_s primaryRenderView;
        public viewDef_s primaryView;
        // many console commands need to know which world they should operate on
        //
        public idMaterial defaultMaterial;
        public idImage testImage;
        public idCinematic testVideo;
        public float testVideoStartTime;
        //
        public idImage ambientCubeImage;	// hack for testing dependent ambient lighting
        //
        public viewDef_s viewDef;
        //
        public performanceCounters_t pc;	// performance counters
        //
        public drawSurfsCommand_t lockSurfacesCmd;	// use this when r_lockSurfaces = 1
        //
        public viewEntity_s identitySpace;	// can use if we don't know viewDef->worldSpace is valid
        public FileChannel/*FILE*/ logFile;			// for logging GL calls and frame breaks
        //
        public int stencilIncr, stencilDecr;	// GL_INCR / INCR_WRAP_EXT, GL_DECR / GL_DECR_EXT
        //
        public renderCrop_t[] renderCrops;// = new renderCrop_t[MAX_RENDER_CROPS];
        public int currentRenderCrop;
        //
        // GUI drawing variables for surface creation
        public int guiRecursionLevel;		// to prevent infinite overruns
        public idGuiModel guiModel;
        public idGuiModel demoGuiModel;
        //
        @Deprecated
        public short[] gammaTable = new short[256];	// brightness / gamma modify this
        //
        //

        // external functions
        // virtual void			Init( void );
        // virtual void			Shutdown( void );
        // virtual void			InitOpenGL( void );
        // virtual void			ShutdownOpenGL( void );
        // virtual bool			IsOpenGLRunning( void ) const;
        // virtual bool			IsFullScreen( void ) const;
        // virtual int				GetScreenWidth( void ) const;
        // virtual int				GetScreenHeight( void ) const;
        // virtual idRenderWorld *	AllocRenderWorld( void );
        // virtual void			FreeRenderWorld( idRenderWorld *rw );
        // virtual void			BeginLevelLoad( void );
        // virtual void			EndLevelLoad( void );
        // virtual bool			RegisterFont( const char *fontName, fontInfoEx_t &font );
        // virtual void			SetColor( const idVec4 &rgba );
        // virtual void			SetColor4( float r, float g, float b, float a );
        // virtual void			DrawStretchPic ( const idDrawVert *verts, const glIndex_t *indexes, int vertCount, int indexCount, const idMaterial *material,
        // bool clip = true, float x = 0.0f, float y = 0.0f, float w = 640.0f, float h = 0.0f );
        // virtual void			DrawStretchPic ( float x, float y, float w, float h, float s1, float t1, float s2, float t2, const idMaterial *material );
        // virtual void			DrawStretchTri ( idVec2 p1, idVec2 p2, idVec2 p3, idVec2 t1, idVec2 t2, idVec2 t3, const idMaterial *material );
        // virtual void			GlobalToNormalizedDeviceCoordinates( const idVec3 &global, idVec3 &ndc );
        // virtual void			GetGLSettings( int& width, int& height );
        // virtual void			PrintMemInfo( MemInfo_t *mi );
        // virtual void			DrawSmallChar( int x, int y, int ch, const idMaterial *material );
        // virtual void			DrawSmallStringExt( int x, int y, const char *string, const idVec4 &setColor, bool forceColor, const idMaterial *material );
        // virtual void			DrawBigChar( int x, int y, int ch, const idMaterial *material );
        // virtual void			DrawBigStringExt( int x, int y, const char *string, const idVec4 &setColor, bool forceColor, const idMaterial *material );
        // virtual void			WriteDemoPics();
        // virtual void			DrawDemoPics();
        // virtual void			BeginFrame( int windowWidth, int windowHeight );
        // virtual void			EndFrame( int *frontEndMsec, int *backEndMsec );
        // virtual void			TakeScreenshot( int width, int height, const char *fileName, int downSample, renderView_t *ref );
        // virtual void			CropRenderSize( int width, int height, bool makePowerOfTwo = false, bool forceDimensions = false );
        // virtual void			CaptureRenderToImage( const char *imageName );
        // virtual void			CaptureRenderToFile( const char *fileName, bool fixAlpha );
        // virtual void			UnCrop();
        // virtual void			GetCardCaps( bool &oldCard, bool &nv10or20 );
        // virtual bool			UploadImage( const char *imageName, const byte *data, int width, int height );
        // internal functions
        public idRenderSystemLocal() {
            this.ambientLightVector = new idVec4();
            this.worlds = new idList<>();
            Clear();
        }
        // ~idRenderSystemLocal( void );

        public void Clear() {
            this.registered = false;
            this.frameCount = 0;
            this.viewCount = 0;
            this.staticAllocCount = 0;
            this.frameShaderTime = 0.0f;
            this.viewportOffset[0] = 0;
            this.viewportOffset[1] = 0;
            this.tiledViewport[0] = 0;
            this.tiledViewport[1] = 0;
            this.backEndRenderer = BE_BAD;
            this.backEndRendererHasVertexPrograms = false;
            this.backEndRendererMaxLight = 1.0f;
            this.ambientLightVector.Zero();
            this.sortOffset = 0;
            this.worlds.Clear();
            this.primaryWorld = null;
//            memset(primaryRenderView, 0, sizeof(primaryRenderView));
            this.primaryRenderView = new renderView_s();
            this.primaryView = null;
            this.defaultMaterial = null;
            this.testImage = null;
            this.ambientCubeImage = null;
            this.viewDef = null;
//            memset(pc, 0, sizeof(pc));
            this.pc = new performanceCounters_t();
//            memset(lockSurfacesCmd, 0, sizeof(lockSurfacesCmd));
            this.lockSurfacesCmd = new drawSurfsCommand_t();
//            memset(identitySpace, 0, sizeof(identitySpace));
            this.identitySpace = new viewEntity_s();
            this.logFile = null;
            this.stencilIncr = 0;
            this.stencilDecr = 0;
//            memset(renderCrops, 0, sizeof(renderCrops));
            this.renderCrops = new renderCrop_t[MAX_RENDER_CROPS];
            for (int r = 0; r < this.renderCrops.length; r++) {
                this.renderCrops[r] = new renderCrop_t();
            }
            this.currentRenderCrop = 0;
            this.guiRecursionLevel = 0;
            this.guiModel = null;
            this.demoGuiModel = null;
//            memset(gammaTable, 0, sizeof(gammaTable));
            this.gammaTable = new short[256];
            this.takingScreenshot = false;
        }

        /*
         ==================
         SetBackEndRenderer

         Check for changes in the back end renderSystem, possibly invalidating cached data
         ==================
         */
        public void SetBackEndRenderer() {			// sets tr.backEndRenderer based on cvars
            if (!r_renderer.IsModified()) {
                return;
            }

            final boolean oldVPstate = this.backEndRendererHasVertexPrograms;

            this.backEndRenderer = BE_BAD;

            if (idStr.Icmp(r_renderer.GetString(), "arb") == 0) {
                this.backEndRenderer = BE_ARB;
            } else if (idStr.Icmp(r_renderer.GetString(), "arb2") == 0) {
                if (glConfig.allowARB2Path) {
                    this.backEndRenderer = BE_ARB2;
                }
            } else if (idStr.Icmp(r_renderer.GetString(), "nv10") == 0) {
                if (glConfig.allowNV10Path) {
                    this.backEndRenderer = BE_NV10;
                }
            } else if (idStr.Icmp(r_renderer.GetString(), "nv20") == 0) {
                if (glConfig.allowNV20Path) {
                    this.backEndRenderer = BE_NV20;
                }
            } else if (idStr.Icmp(r_renderer.GetString(), "r200") == 0) {
                if (glConfig.allowR200Path) {
                    this.backEndRenderer = BE_R200;
                }
            }

            // fallback
            if (this.backEndRenderer == BE_BAD) {
                // choose the best
                if (glConfig.allowARB2Path) {
                    this.backEndRenderer = BE_ARB2;
                } else if (glConfig.allowR200Path) {
                    this.backEndRenderer = BE_R200;
                } else if (glConfig.allowNV20Path) {
                    this.backEndRenderer = BE_NV20;
                } else if (glConfig.allowNV10Path) {
                    this.backEndRenderer = BE_NV10;
                } else {
                    // the others are considered experimental
                    this.backEndRenderer = BE_ARB;
                }
            }

            this.backEndRendererHasVertexPrograms = false;
            this.backEndRendererMaxLight = 1.0f;

            switch (this.backEndRenderer) {
                case BE_ARB:
                    common.Printf("using ARB renderSystem\n");
                    break;
                case BE_NV10:
                    common.Printf("using NV10 renderSystem\n");
                    break;
                case BE_NV20:
                    common.Printf("using NV20 renderSystem\n");
                    this.backEndRendererHasVertexPrograms = true;
                    break;
                case BE_R200:
                    common.Printf("using R200 renderSystem\n");
                    this.backEndRendererHasVertexPrograms = true;
                    break;
                case BE_ARB2:
                    common.Printf("using ARB2 renderSystem\n");
                    this.backEndRendererHasVertexPrograms = true;
                    this.backEndRendererMaxLight = 999;
                    break;
                default:
                    common.FatalError("SetbackEndRenderer: bad back end");
            }

            // clear the vertex cache if we are changing between
            // using vertex programs and not, because specular and
            // shadows will be different data
            if (oldVPstate != this.backEndRendererHasVertexPrograms) {
                vertexCache.PurgeAll();
                if (this.primaryWorld != null) {
                    this.primaryWorld.FreeInteractions();
                }
            }

            r_renderer.ClearModified();
        }

        /*
         =====================
         RenderViewToViewport

         Converts from SCREEN_WIDTH / SCREEN_HEIGHT coordinates to current cropped pixel coordinates
         =====================
         */
        public void RenderViewToViewport(final renderView_s renderView, idScreenRect viewport) {
            final renderCrop_t rc = this.renderCrops[this.currentRenderCrop];

            final float wRatio = (float) rc.width / SCREEN_WIDTH;
            final float hRatio = (float) rc.height / SCREEN_HEIGHT;

            viewport.x1 = idMath.Ftoi(rc.x + (renderView.x * wRatio));
            viewport.x2 = idMath.Ftoi((rc.x + (float) Math.floor(((renderView.x + renderView.width) * wRatio) + 0.5f)) - 1);
            viewport.y1 = idMath.Ftoi((rc.y + rc.height) - (float) Math.floor(((renderView.y + renderView.height) * hRatio) + 0.5f));
            viewport.y2 = idMath.Ftoi((rc.y + rc.height) - (float) Math.floor((renderView.y * hRatio) + 0.5f) - 1);
        }

        @Override
        public void Init() {

            common.Printf("------- Initializing renderSystem --------\n");

            // clear all our internal state
            this.viewCount = 1;		// so cleared structures never match viewCount
            // we used to memset tr, but now that it is a class, we can't, so
            // there may be other state we need to reset

            this.ambientLightVector.oSet(0, 0.5f);
            this.ambientLightVector.oSet(1, 0.5f - 0.385f);
            this.ambientLightVector.oSet(2, 0.8925f);
            this.ambientLightVector.oSet(3, 1.0f);

//            memset(backEnd, 0, sizeof(backEnd));
            backEnd = new backEndState_t();

            R_InitCvars();

            R_InitCommands();

            this.guiModel = new idGuiModel();
            this.guiModel.Clear();

            this.demoGuiModel = new idGuiModel();
            this.demoGuiModel.Clear();

            R_InitTriSurfData();

            globalImages.Init();

            idCinematic.InitCinematic();

            // build brightness translation tables
            R_SetColorMappings();

            R_InitMaterials();

            renderModelManager.Init();

            // set the identity space
            this.identitySpace.modelMatrix[(0 * 4) + 0] = 1.0f;
            this.identitySpace.modelMatrix[(1 * 4) + 1] = 1.0f;
            this.identitySpace.modelMatrix[(2 * 4) + 2] = 1.0f;

            // determine which back end we will use
            // ??? this is invalid here as there is not enough information to set it up correctly
            SetBackEndRenderer();

            common.Printf("renderSystem initialized.\n");
            common.Printf("--------------------------------------\n");
        }

        @Override
        public void Shutdown() {
            common.Printf("idRenderSystem::Shutdown()\n");

            R_DoneFreeType();

            if (glConfig.isInitialized) {
                globalImages.PurgeAllImages();
            }

            renderModelManager.Shutdown();

            idCinematic.ShutdownCinematic();

            globalImages.Shutdown();

            // close the r_logFile
            if (this.logFile != null) {
                try {
                    fprintf(this.logFile, "*** CLOSING LOG ***\n");
                    this.logFile.close();
                } catch (final IOException ex) {
                    Logger.getLogger(tr_local.class.getName()).log(Level.SEVERE, null, ex);
                }
                this.logFile = null;
            }

            // free frame memory
            R_ShutdownFrameData();

            // free the vertex cache, which should have nothing allocated now
            vertexCache.Shutdown();

            R_ShutdownTriSurfData();

            RB_ShutdownDebugTools();

//	delete guiModel;
//	delete demoGuiModel;
            Clear();

            ShutdownOpenGL();
        }

        @Override
        public void InitOpenGL() {
            // if OpenGL isn't started, start it now
            if (!glConfig.isInitialized) {
                int err;

                R_InitOpenGL();

                globalImages.ReloadAllImages();

                err = qglGetError();
                if (err != GL_NO_ERROR) {
                    common.Printf("glGetError() = 0x%x\n", err);
                }
            }
        }

        @Override
        public void ShutdownOpenGL() {
            // free the context and close the window
            R_ShutdownFrameData();
            GLimp_Shutdown();
            glConfig.isInitialized = false;
        }

        @Override
        public boolean IsOpenGLRunning() {
            if (!glConfig.isInitialized) {
                return false;
            }
            return true;
        }

        @Override
        public boolean IsFullScreen() {
            return glConfig.isFullscreen;
        }

        @Override
        public int GetScreenWidth() {
            return glConfig.vidWidth;
        }

        @Override
        public int GetScreenHeight() {
            return glConfig.vidHeight;
        }

        @Override
        public idRenderWorld AllocRenderWorld() {
            idRenderWorldLocal rw;
            rw = new idRenderWorldLocal();
            this.worlds.Append(rw);
            return rw;
        }

        @Override
        public void FreeRenderWorld(idRenderWorld rw) {
            if (this.primaryWorld == rw) {
                this.primaryWorld = null;
            }
            this.worlds.Remove((idRenderWorldLocal) rw);
//	delete rw;
        }

        @Override
        public void BeginLevelLoad() {
            renderModelManager.BeginLevelLoad();
            globalImages.BeginLevelLoad();
        }

        @Override
        public void EndLevelLoad() {
            renderModelManager.EndLevelLoad();
            globalImages.EndLevelLoad();
            if (RenderSystem_init.r_forceLoadImages.GetBool()) {
                RB_ShowImages();
            }
        }

        /*
         ============
         RegisterFont

         Loads 3 point sizes, 12, 24, and 48
         ============
         */
        @Override
        public boolean RegisterFont(String fontName, fontInfoEx_t font) {
//if( BUILD_FREETYPE){
//            FT_Face face;
//            int j, k, xOut, yOut, lastStart, imageNumber;
//            int scaledSize, newSize, maxHeight, left, satLevels;
//            char[] out, imageBuff;
//            glyphInfo_t glyph;
//            idImage image;
//            idMaterial h;
//            float max;
//}
            final ByteBuffer[] faceData = {null};
            final long[] fTime = {0};
            int i, len, fontCount;
//	char name[1024];
            final StringBuilder name = new StringBuilder(1024);

            int pointSize = 12;
            /*
             if ( registeredFontCount >= MAX_FONTS ) {
             common.Warning( "RegisterFont: Too many fonts registered already." );
             return false;
             }

             int pointSize = 12;
             idStr::snPrintf( name, sizeof(name), "%s/fontImage_%d.dat", fontName, pointSize );
             for ( i = 0; i < registeredFontCount; i++ ) {
             if ( idStr::Icmp(name, registeredFont[i].fontInfoSmall.name) == 0 ) {
             memcpy( &font, &registeredFont[i], sizeof( fontInfoEx_t ) );
             return true;
             }
             }
             */

//            memset(font, 0, sizeof(font));
            font.clear();

            for (fontCount = 0; fontCount < 3; fontCount++) {

                if (fontCount == 0) {
                    pointSize = 12;
                } else if (fontCount == 1) {
                    pointSize = 24;
                } else {
                    pointSize = 48;
                }
                // we also need to adjust the scale based on point size relative to 48 points as the ui scaling is based on a 48 point font
                float glyphScale = 1.0f; 		// change the scale to be relative to 1 based on 72 dpi ( so dpi of 144 means a scale of .5 )
                glyphScale *= 48.0f / pointSize;

                idStr.snPrintf(name, name.capacity(), "%s/fontImage_%d.dat", fontName, pointSize);

                final fontInfo_t outFont;
                if (0 == fontCount) {
                    outFont = font.fontInfoSmall = new fontInfo_t();
                } else if (1 == fontCount) {
                    outFont = font.fontInfoMedium = new fontInfo_t();
                } else {
                    outFont = font.fontInfoLarge = new fontInfo_t();
                }

                idStr.Copynz(outFont.name, name.toString());

                len = fileSystem.ReadFile(name.toString(), null, fTime);
                if (len != fontInfo_t.BYTES) {
                    common.Warning("RegisterFont: couldn't find font: '%s'", name);
                    return false;
                }

                fileSystem.ReadFile(name.toString(), faceData, fTime);
                fdOffset = 0;
                fdFile = faceData[0].array();
                for (i = 0; i < GLYPHS_PER_FONT; i++) {
                    outFont.glyphs[i] = new glyphInfo_t();
                    outFont.glyphs[i].height = readInt();
                    outFont.glyphs[i].top = readInt();
                    outFont.glyphs[i].bottom = readInt();
                    outFont.glyphs[i].pitch = readInt();
                    outFont.glyphs[i].xSkip = readInt();
                    outFont.glyphs[i].imageWidth = readInt();
                    outFont.glyphs[i].imageHeight = readInt();
                    outFont.glyphs[i].s = readFloat();
                    outFont.glyphs[i].t = readFloat();
                    outFont.glyphs[i].s2 = readFloat();
                    outFont.glyphs[i].t2 = readFloat();
                    final int junk /* font.glyphs[i].glyph */ = readInt();
                    //FIXME: the +6, -6 skips the embedded fonts/ 
//                    memcpy(outFont.glyphs[i].shaderName, fdFile[fdOffset + 6], 32 - 6);
                    outFont.glyphs[i].shaderName = new String(Arrays.copyOfRange(fdFile, fdOffset + 6, fdOffset + 32));
                    fdOffset += 32;
                }
                outFont.glyphScale = readFloat();

                int mw = 0;
                int mh = 0;
                for (i = GLYPH_START; i < GLYPH_END; i++) {
                    idStr.snPrintf(name, name.capacity(), "%s/%s", fontName, outFont.glyphs[i].shaderName);
                    outFont.glyphs[i].glyph = declManager.FindMaterial(name.toString());
                    outFont.glyphs[i].glyph.SetSort(SS_GUI);
                    if (mh < outFont.glyphs[i].height) {
                        mh = outFont.glyphs[i].height;
                    }
                    if (mw < outFont.glyphs[i].xSkip) {
                        mw = outFont.glyphs[i].xSkip;
                    }
                }
                if (fontCount == 0) {
                    font.maxWidthSmall = mw;
                    font.maxHeightSmall = mh;
                } else if (fontCount == 1) {
                    font.maxWidthMedium = mw;
                    font.maxHeightMedium = mh;
                } else {
                    font.maxWidthLarge = mw;
                    font.maxHeightLarge = mh;
                }
                fileSystem.FreeFile(faceData);
            }

            //memcpy( &registeredFont[registeredFontCount++], &font, sizeof( fontInfoEx_t ) );
//            return true;
//            
            if (BUILD_FREETYPE) {
                common.Warning("RegisterFont: couldn't load FreeType code %s", name);
//            } else {
//
//                if (ftLibrary == null) {
//                    common.Warning("RegisterFont: FreeType not initialized.");
//                    return;
//                }
//
//                len = fileSystem.ReadFile(fontName, faceData, ftime);
//                if (len <= 0) {
//                    common.Warning("RegisterFont: Unable to read font file");
//                    return;
//                }
//
//                // allocate on the stack first in case we fail
//                if (FT_New_Memory_Face(ftLibrary, faceData, len, 0, face)) {
//                    common.Warning("RegisterFont: FreeType2, unable to allocate new face.");
//                    return;
//                }
//
//                if (FT_Set_Char_Size(face, pointSize << 6, pointSize << 6, dpi, dpi)) {
//                    common.Warning("RegisterFont: FreeType2, Unable to set face char size.");
//                    return;
//                }
//
//                // font = registeredFonts[registeredFontCount++];
//                // make a 256x256 image buffer, once it is full, register it, clean it and keep going 
//                // until all glyphs are rendered
//                out = new char[1024 * 1024];// Mem_Alloc(1024 * 1024);
//                if (out == null) {//TODO:remove
//                    common.Warning("RegisterFont: Mem_Alloc failure during output image creation.");
//                    return;
//                }
////                memset(out, 0, 1024 * 1024);
//                out = new char[1024 * 1024];
//
//                maxHeight = 0;
//
//                for (i = GLYPH_START; i < GLYPH_END; i++) {
//                    glyph = RE_ConstructGlyphInfo(out, xOut, yOut, maxHeight, face, i, qtrue);
//                }
//
//                xOut = 0;
//                yOut = 0;
//                i = GLYPH_START;
//                lastStart = i;
//                imageNumber = 0;
//
//                while (i <= GLYPH_END) {
//
//                    glyph = RE_ConstructGlyphInfo(out, xOut, yOut, maxHeight, face, i, qfalse);
//
//                    if (xOut == -1 || yOut == -1 || i == GLYPH_END) {
//                        // ran out of room
//                        // we need to create an image from the bitmap, set all the handles in the glyphs to this point
//                        // 
//
//                        scaledSize = 256 * 256;
//                        newSize = scaledSize * 4;
//                        imageBuff = new char[newSize];// Mem_Alloc(newSize);
//                        left = 0;
//                        max = 0;
//                        satLevels = 255;
//                        for (k = 0; k < (scaledSize); k++) {
//                            if (max < out[k]) {
//                                max = out[k];
//                            }
//                        }
//
//                        if (max > 0) {
//                            max = 255 / max;
//                        }
//
//                        for (k = 0; k < (scaledSize); k++) {
//                            imageBuff[left++] = 255;
//                            imageBuff[left++] = 255;
//                            imageBuff[left++] = 255;
//                            imageBuff[left++] = (char) ((float) out[k] * max);
//                        }
//
//                        idStr.snprintf(name[0], sizeof(name[0]), "fonts/fontImage_%i_%i.tga", imageNumber++, pointSize);
//                        if (r_saveFontData.integer) {
//                            R_WriteTGA(name[0], imageBuff, 256, 256);
//                        }
//
//                        //idStr::snprintf( name, sizeof(name), "fonts/fontImage_%i_%i", imageNumber++, pointSize );
//                        image = R_CreateImage(name[0], imageBuff, 256, 256, qfalse, qfalse, GL_CLAMP);
//                        h = RE_RegisterShaderFromImage(name[0], LIGHTMAP_2D, image, qfalse);
//                        for (j = lastStart; j < i; j++) {
//                            font.glyphs[j].glyph = h;
//                            idStr.Copynz(font.glyphs[j].shaderName, name[0], sizeof(font.glyphs[j].shaderName));
//                        }
//                        lastStart = i;
////                        memset(out, 0, 1024 * 1024);
//                        out = new char[1024 * 1024];
//                        xOut = 0;
//                        yOut = 0;
//                        imageBuff = null;
//                        i++;
//                    } else {
//                        memcpy(font.glyphs[i], glyph, sizeof(glyphInfo_t));
//                        i++;
//                    }
//                }
//
//                registeredFont[registeredFontCount].glyphScale = glyphScale;
//                font.glyphScale = glyphScale;
//                memcpy(registeredFont[registeredFontCount++], font, sizeof(fontInfo_t));
//
//                if (r_saveFontData.integer) {
//                    fileSystem.WriteFile(va("fonts/fontImage_%i.dat", pointSize), font, sizeof(fontInfo_t));
//                }
//
//                out = null;
//
//                fileSystem.FreeFile(faceData);
            }
            return true;
        }

        /*
         =============
         SetColor

         This can be used to pass general information to the current material, not
         just colors
         =============
         */
        @Override
        public void SetColor(idVec4 rgba) {
            this.SetColor4(rgba.oGet(0), rgba.oGet(1), rgba.oGet(2), rgba.oGet(3));
        }

        @Override
        public void SetColor4(float r, float g, float b, float a) {
            this.guiModel.SetColor(r, g, b, a);
        }

        @Override
        public void DrawStretchPic(idDrawVert[] verts, int[] indexes, int vertCount, int indexCount, idMaterial material, boolean clip, float min_x, float min_y, float max_x, float max_y) {
            this.guiModel.DrawStretchPic(verts, indexes, vertCount, indexCount, material, clip, min_x, min_y, max_x, max_y);
        }

        /*
         =============
         DrawStretchPic

         x/y/w/h are in the 0,0 to 640,480 range
         =============
         */
        @Override
        public void DrawStretchPic(float x, float y, float w, float h, float s1, float t1, float s2, float t2, idMaterial material) {
            this.guiModel.DrawStretchPic(x, y, w, h, s1, t1, s2, t2, material);
        }

        /*
         =============
         DrawStretchTri

         x/y/w/h are in the 0,0 to 640,480 range
         =============
         */
        @Override
        public void DrawStretchTri(idVec2 p1, idVec2 p2, idVec2 p3, idVec2 t1, idVec2 t2, idVec2 t3, idMaterial material) {
            tr.guiModel.DrawStretchTri(p1, p2, p3, t1, t2, t3, material);
        }

        @Override
        public void GlobalToNormalizedDeviceCoordinates(idVec3 global, idVec3 ndc) {
            R_GlobalToNormalizedDeviceCoordinates(global, ndc);
        }

        @Override
        public void GetGLSettings(int[] width, int[] height) {
            width[0] = glConfig.vidWidth;
            height[0] = glConfig.vidHeight;
        }

        @Override
        public void PrintMemInfo(MemInfo_t mi) {
            // sum up image totals
            globalImages.PrintMemInfo(mi);

            // sum up model totals
            renderModelManager.PrintMemInfo(mi);

            // compute render totals
        }

        /*
         =====================
         idRenderSystemLocal::DrawSmallChar

         small chars are drawn at native screen resolution
         =====================
         */
        @Override
        public void DrawSmallChar(int x, int y, int ch, idMaterial material) {
            int row, col;
            float fRow, fCol;
            float size;

            ch &= 255;

            if (ch == ' ') {
                return;
            }

            if (y < -SMALLCHAR_HEIGHT) {
                return;
            }

            row = ch >> 4;
            col = ch & 15;

            fRow = row * 0.0625f;
            fCol = col * 0.0625f;
            size = 0.0625f;

            DrawStretchPic(x, y, SMALLCHAR_WIDTH, SMALLCHAR_HEIGHT, fCol, fRow,
                    fCol + size, fRow + size, material);
        }

        /*
         ==================
         idRenderSystemLocal::DrawSmallString[Color]

         Draws a multi-colored string with a drop shadow, optionally forcing
         to a fixed color.

         Coordinates are at 640 by 480 virtual resolution
         ==================
         */
        @Override
        public void DrawSmallStringExt(int x, int y, final char[] string, idVec4 setColor, boolean forceColor, idMaterial material) {
            idVec4 color;
            int s;
            int xx;

            // draw the colored text
            s = 0;//(const unsigned char*)string;
            xx = x;
            SetColor(setColor);
            while ((s < string.length) && (string[s] != '\0')) {
                if (idStr.IsColor(ctos(string).substring(s))) {
                    if (!forceColor) {
                        if (string[s + 1] == C_COLOR_DEFAULT) {
                            SetColor(setColor);
                        } else {
                            color = idStr.ColorForIndex(string[s + 1]);
                            color.oSet(3, setColor.oGet(3));
                            SetColor(color);
                        }
                    }
                    s += 2;
                    continue;
                }
                DrawSmallChar(xx, y, string[s], material);
                xx += SMALLCHAR_WIDTH;
                s++;
            }
            SetColor(colorWhite);
        }

        @Override
        public void DrawBigChar(int x, int y, int ch, idMaterial material) {
            int row, col;
            float frow, fcol;
            float size;

            ch &= 255;

            if (ch == ' ') {
                return;
            }

            if (y < -BIGCHAR_HEIGHT) {
                return;
            }

            row = ch >> 4;
            col = ch & 15;

            frow = row * 0.0625f;
            fcol = col * 0.0625f;
            size = 0.0625f;

            DrawStretchPic(x, y, BIGCHAR_WIDTH, BIGCHAR_HEIGHT, fcol, frow, fcol + size, frow + size, material);
        }

        /*
         ==================
         idRenderSystemLocal::DrawBigString[Color]

         Draws a multi-colored string with a drop shadow, optionally forcing
         to a fixed color.

         Coordinates are at 640 by 480 virtual resolution
         ==================
         */
        @Override
        public void DrawBigStringExt(int x, int y, final String string, idVec4 setColor, boolean forceColor, idMaterial material) {
            idVec4 color;
            int s;
            int xx;

            // draw the colored text
            s = 0;//string;
            xx = x;
            SetColor(setColor);
            while (s < string.length()) {
                if (idStr.IsColor(string.substring(s))) {
                    if (!forceColor) {
                        if ((string.charAt(s + 1) == C_COLOR_DEFAULT)) {
                            SetColor(setColor);
                        } else {
                            color = idStr.ColorForIndex(string.charAt(s + 1));
                            color.oSet(3, setColor.oGet(3));
                            SetColor(color);
                        }
                    }
                    s += 2;
                    continue;
                }
                DrawBigChar(xx, y, string.charAt(s), material);
                xx += BIGCHAR_WIDTH;
                s++;
            }
            SetColor(colorWhite);
        }

        @Override
        public void WriteDemoPics() {
            session.writeDemo.WriteInt(DS_RENDER);
            session.writeDemo.WriteInt(DC_GUI_MODEL);
            this.guiModel.WriteToDemo(session.writeDemo);
        }

        @Override
        public void DrawDemoPics() {
            this.demoGuiModel.EmitFullScreen();
        }

        @Override
        public void BeginFrame(int windowWidth, int windowHeight) {
            setBufferCommand_t cmd;

            if (!glConfig.isInitialized) {
                return;
            }

            // determine which back end we will use
            SetBackEndRenderer();

            this.guiModel.Clear();

            // for the larger-than-window tiled rendering screenshots
            if (this.tiledViewport[0] != 0) {
                windowWidth = this.tiledViewport[0];
                windowHeight = this.tiledViewport[1];
            }

            glConfig.vidWidth = windowWidth;
            glConfig.vidHeight = windowHeight;

            this.renderCrops[0].x = 0;
            this.renderCrops[0].y = 0;
            this.renderCrops[0].width = windowWidth;
            this.renderCrops[0].height = windowHeight;
            this.currentRenderCrop = 0;

            // screenFraction is just for quickly testing fill rate limitations
            if (r_screenFraction.GetInteger() != 100) {
                final int w = (int) ((SCREEN_WIDTH * r_screenFraction.GetInteger()) / 100.0f);
                final int h = (int) ((SCREEN_HEIGHT * r_screenFraction.GetInteger()) / 100.0f);
                CropRenderSize(w, h);
            }

            // this is the ONLY place this is modified
            this.frameCount++;

            // just in case we did a common.Error while this
            // was set
            this.guiRecursionLevel = 0;

            // the first rendering will be used for commands like
            // screenshot, rather than a possible subsequent remote
            // or mirror render
//	primaryWorld = NULL;

            // set the time for shader effects in 2D rendering
            this.frameShaderTime = (float) (eventLoop.Milliseconds() * 0.001);

            //
            // draw buffer stuff
            //
            R_GetCommandBuffer(cmd = new setBufferCommand_t()/*sizeof(cmd)*/);
            cmd.commandId = RC_SET_BUFFER;
            cmd.frameCount = this.frameCount;

            if (r_frontBuffer.GetBool()) {
                cmd.buffer = GL_FRONT;
            } else {
                cmd.buffer = GL_BACK;
            }
        }

        /*
         =============
         EndFrame

         Returns the number of msec spent in the back end
         =============
         */ private static int DBG_EndFrame = 0;

        @Override
        public void EndFrame(int[] frontEndMsec, int[] backEndMsec) {
            emptyCommand_t cmd;
            DBG_EndFrame++;
            if (!glConfig.isInitialized) {
                return;
            }

            // close any gui drawing
            this.guiModel.EmitFullScreen();
            this.guiModel.Clear();

            // save out timing information
            if (frontEndMsec != null) {
                frontEndMsec[0] = this.pc.frontEndMsec;
            }
            if (backEndMsec != null) {
                backEndMsec[0] = backEnd.pc.msec;
            }

            // print any other statistics and clear all of them
            R_PerformanceCounters();

            // check for dynamic changes that require some initialization
            R_CheckCvars();

            // check for errors
            GL_CheckErrors();

            // add the swapbuffers command
            R_GetCommandBuffer(cmd = new emptyCommand_t()/*sizeof(cmd)*/);
            cmd.commandId = RC_SWAP_BUFFERS;

            // start the back end up again with the new command list
            R_IssueRenderCommands();

            // use the other buffers next frame, because another CPU
            // may still be rendering into the current buffers
            R_ToggleSmpFrame();

            // we can now release the vertexes used this frame
            vertexCache.EndFrame();

            if (session.writeDemo != null) {
                session.writeDemo.WriteInt(DS_RENDER);
                session.writeDemo.WriteInt(DC_END_FRAME);
                if (r_showDemo.GetBool()) {
                    common.Printf("write DC_END_FRAME\n");
                }
            }

        }

        /*
         ================== 
         TakeScreenshot

         Move to tr_imagefiles.c...

         Will automatically tile render large screen shots if necessary
         Downsample is the number of steps to mipmap the image before saving it
         If ref == NULL, session->updateScreen will be used
         ================== 
         */
        @Override
        public void TakeScreenshot(int width, int height, String fileName, int blends, renderView_s ref) {
            byte[] buffer;
            int i, j, c, temp;

            this.takingScreenshot = true;

            final int pix = width * height;

            buffer = new byte[(pix * 3) + 18];// R_StaticAlloc(pix * 3 + 18);
//	memset (buffer, 0, 18);

            if (blends <= 1) {
                R_ReadTiledPixels(width, height, buffer, 18, ref);
            } else {
                final short[] shortBuffer = new short[pix * 2 * 3];// R_StaticAlloc(pix * 2 * 3);
//		memset (shortBuffer, 0, pix*2*3);

                // enable anti-aliasing jitter
                RenderSystem_init.r_jitter.SetBool(true);

                for (i = 0; i < blends; i++) {
                    R_ReadTiledPixels(width, height, buffer, 18, ref);

                    for (j = 0; j < (pix * 3); j++) {
                        shortBuffer[j] += buffer[18 + j];
                    }
                }

                // divide back to bytes
                for (i = 0; i < (pix * 3); i++) {
                    buffer[18 + i] = (byte) (shortBuffer[i] / blends);
                }

//                R_StaticFree(shortBuffer);
                RenderSystem_init.r_jitter.SetBool(false);
            }

            // fill in the header (this is vertically flipped, which qglReadPixels emits)
            buffer[ 2] = 2;		// uncompressed type
            buffer[12] = (byte) (width & 255);
            buffer[13] = (byte) (width >> 8);
            buffer[14] = (byte) (height & 255);
            buffer[15] = (byte) (height >> 8);
            buffer[16] = 24;	// pixel size

            // swap rgb to bgr
            c = 18 + (width * height * 3);
            for (i = 18; i < c; i += 3) {
                temp = buffer[i];
                buffer[i] = buffer[i + 2];
                buffer[i + 2] = (byte) temp;
            }

            // _D3XP adds viewnote screenie save to cdpath
            if (fileName.contains("viewnote")) {
                fileSystem.WriteFile(fileName, ByteBuffer.wrap(buffer), c, "fs_cdpath");
            } else {
                fileSystem.WriteFile(fileName, ByteBuffer.wrap(buffer), c);
            }
//
//            R_StaticFree(buffer);

            this.takingScreenshot = false;

        }

        /*
         ================
         CropRenderSize

         This automatically halves sizes until it fits in the current window size,
         so if you specify a power of two size for a texture copy, it may be shrunk
         down, but still valid.
         ================
         */
        @Override
        public void CropRenderSize(int width, int height, boolean makePowerOfTwo, boolean forceDimensions) {
            if (!glConfig.isInitialized) {
                return;
            }

            // close any gui drawing before changing the size
            this.guiModel.EmitFullScreen();
            this.guiModel.Clear();

            if ((width < 1) || (height < 1)) {
                common.Error("CropRenderSize: bad sizes");
            }

            if (session.writeDemo != null) {
                session.writeDemo.WriteInt(DS_RENDER);
                session.writeDemo.WriteInt(DC_CROP_RENDER);
                session.writeDemo.WriteInt(width);
                session.writeDemo.WriteInt(height);
                session.writeDemo.WriteInt(btoi(makePowerOfTwo));

                if (RenderSystem_init.r_showDemo.GetBool()) {
                    common.Printf("write DC_CROP_RENDER\n");
                }
            }

            // convert from virtual SCREEN_WIDTH/SCREEN_HEIGHT coordinates to physical OpenGL pixels
            final renderView_s renderView = new renderView_s();
            renderView.x = 0;
            renderView.y = 0;
            renderView.width = width;
            renderView.height = height;

            final idScreenRect r = new idScreenRect();
            RenderViewToViewport(renderView, r);

            width = (r.x2 - r.x1) + 1;
            height = (r.y2 - r.y1) + 1;

            if (forceDimensions) {
                // just give exactly what we ask for
                width = renderView.width;
                height = renderView.height;
            }

            // if makePowerOfTwo, drop to next lower power of two after scaling to physical pixels
            if (makePowerOfTwo) {
                width = RoundDownToPowerOfTwo(width);
                height = RoundDownToPowerOfTwo(height);
                // FIXME: megascreenshots with offset viewports don't work right with this yet
            }

            final renderCrop_t rc = this.renderCrops[this.currentRenderCrop];

            // we might want to clip these to the crop window instead
            while (width > glConfig.vidWidth) {
                width >>= 1;
            }
            while (height > glConfig.vidHeight) {
                height >>= 1;
            }

            if (this.currentRenderCrop == MAX_RENDER_CROPS) {
                common.Error("idRenderSystemLocal::CropRenderSize: currentRenderCrop == MAX_RENDER_CROPS");
            }

            this.currentRenderCrop++;

//            rc = renderCrops[currentRenderCrop];
            this.renderCrops[this.currentRenderCrop - 1] = this.renderCrops[this.currentRenderCrop];

            rc.x = 0;
            rc.y = 0;
            rc.width = width;
            rc.height = height;
        }

        @Override
        public void CaptureRenderToImage(String imageName) {
            if (!glConfig.isInitialized) {
                return;
            }
            this.guiModel.EmitFullScreen();
            this.guiModel.Clear();

            if (session.writeDemo != null) {
                session.writeDemo.WriteInt(DS_RENDER);
                session.writeDemo.WriteInt(DC_CAPTURE_RENDER);
                session.writeDemo.WriteHashString(imageName);

                if (RenderSystem_init.r_showDemo.GetBool()) {
                    common.Printf("write DC_CAPTURE_RENDER: %s\n", imageName);
                }
            }

            // look up the image before we create the render command, because it
            // may need to sync to create the image
            final idImage image = globalImages.ImageFromFile(imageName, TF_DEFAULT, true, TR_REPEAT, TD_DEFAULT);

            final renderCrop_t rc = this.renderCrops[this.currentRenderCrop];

            copyRenderCommand_t cmd;
            R_GetCommandBuffer(cmd = new copyRenderCommand_t()/*sizeof(cmd)*/);
            cmd.commandId = RC_COPY_RENDER;
            cmd.x = rc.x;
            cmd.y = rc.y;
            cmd.imageWidth = rc.width;
            cmd.imageHeight = rc.height;
            cmd.image = image;

            this.guiModel.Clear();
        }

        @Override
        public void CaptureRenderToFile(String fileName, boolean fixAlpha) {
            if (!glConfig.isInitialized) {
                return;
            }

            final renderCrop_t rc = this.renderCrops[this.currentRenderCrop];

            this.guiModel.EmitFullScreen();
            this.guiModel.Clear();
            R_IssueRenderCommands();

            qglReadBuffer(GL_BACK);

            // include extra space for OpenGL padding to word boundaries
            final int c = (rc.width + 3) * rc.height;
            ByteBuffer data = Nio.newByteBuffer(c * 3);// R_StaticAlloc(c * 3);

            qglReadPixels(rc.x, rc.y, rc.width, rc.height, GL_RGB, GL_UNSIGNED_BYTE, data);

            ByteBuffer data2 = ByteBuffer.allocate(c * 4);// R_StaticAlloc(c * 4);

            for (int i = 0; i < c; i++) {
                data2.put(i * 4, data.get(i * 3));
                data2.put((i * 4) + 1, data.get((i * 3) + 1));
                data2.put((i * 4) + 2, data.get((i * 3) + 2));
                data2.put((i * 4) + 3, (byte) 0xff);
            }

            R_WriteTGA(fileName, data2, rc.width, rc.height, true);

            data = null;// R_StaticFree(data);
            data2 = null;// R_StaticFree(data2);
        }

        @Override
        public void UnCrop() {
            if (!glConfig.isInitialized) {
                return;
            }

            if (this.currentRenderCrop < 1) {
                common.Error("idRenderSystemLocal::UnCrop: currentRenderCrop < 1");
            }

            // close any gui drawing
            this.guiModel.EmitFullScreen();
            this.guiModel.Clear();

            this.currentRenderCrop--;

            if (session.writeDemo != null) {
                session.writeDemo.WriteInt(DS_RENDER);
                session.writeDemo.WriteInt(DC_UNCROP_RENDER);

                if (RenderSystem_init.r_showDemo.GetBool()) {
                    common.Printf("write DC_UNCROP\n");
                }
            }
        }

        @Override
        public void GetCardCaps(boolean[] oldCard, boolean[] nv10or20) {
            nv10or20[0] = ((tr.backEndRenderer == BE_NV10) || (tr.backEndRenderer == BE_NV20));
            oldCard[0] = ((tr.backEndRenderer == BE_ARB) || (tr.backEndRenderer == BE_R200) || (tr.backEndRenderer == BE_NV10) || (tr.backEndRenderer == BE_NV20));
        }

        @Override
        public boolean UploadImage(String imageName, ByteBuffer data, int width, int height) {
            final idImage image = globalImages.GetImage(imageName);
            if (null == image) {
                return false;
            }
            image.UploadScratch(data, width, height);
            image.SetImageFilterAndRepeat();
            return true;
        }
    }
    public static backEndState_t backEnd;
    public static idRenderSystemLocal tr       = new idRenderSystemLocal();
    public static glconfig_s          glConfig = new glconfig_s();                 // outside of TR since it shouldn't be cleared during ref re-init

    /*
     ====================================================================

     GL wrapper/helper functions

     ====================================================================
     */
    public static final int GLS_SRCBLEND_ZERO                = 0x00000001;
    public static final int GLS_SRCBLEND_ONE                 = 0x0;
    public static final int GLS_SRCBLEND_DST_COLOR           = 0x00000003;
    public static final int GLS_SRCBLEND_ONE_MINUS_DST_COLOR = 0x00000004;
    public static final int GLS_SRCBLEND_SRC_ALPHA           = 0x00000005;
    public static final int GLS_SRCBLEND_ONE_MINUS_SRC_ALPHA = 0x00000006;
    public static final int GLS_SRCBLEND_DST_ALPHA           = 0x00000007;
    public static final int GLS_SRCBLEND_ONE_MINUS_DST_ALPHA = 0x00000008;
    public static final int GLS_SRCBLEND_ALPHA_SATURATE      = 0x00000009;
    public static final int GLS_SRCBLEND_BITS                = 0x0000000f;
    //
    public static final int GLS_DSTBLEND_ZERO                = 0x0;
    public static final int GLS_DSTBLEND_ONE                 = 0x00000020;
    public static final int GLS_DSTBLEND_SRC_COLOR           = 0x00000030;
    public static final int GLS_DSTBLEND_ONE_MINUS_SRC_COLOR = 0x00000040;
    public static final int GLS_DSTBLEND_SRC_ALPHA           = 0x00000050;
    public static final int GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA = 0x00000060;
    public static final int GLS_DSTBLEND_DST_ALPHA           = 0x00000070;
    public static final int GLS_DSTBLEND_ONE_MINUS_DST_ALPHA = 0x00000080;
    public static final int GLS_DSTBLEND_BITS                = 0x000000f0;
    //
    //
    // these masks are the inverse, meaning when set the glColorMask value will be 0,
    // preventing that channel from being written
    public static final int GLS_DEPTHMASK                    = 0x00000100;
    public static final int GLS_REDMASK                      = 0x00000200;
    public static final int GLS_GREENMASK                    = 0x00000400;
    public static final int GLS_BLUEMASK                     = 0x00000800;
    public static final int GLS_ALPHAMASK                    = 0x00001000;
    public static final int GLS_COLORMASK                    = (GLS_REDMASK | GLS_GREENMASK | GLS_BLUEMASK);
    //
    public static final int GLS_POLYMODE_LINE                = 0x00002000;
    //
    public static final int GLS_DEPTHFUNC_ALWAYS             = 0x00010000;
    public static final int GLS_DEPTHFUNC_EQUAL              = 0x00020000;
    public static final int GLS_DEPTHFUNC_LESS               = 0x0;
    //
    public static final int GLS_ATEST_EQ_255                 = 0x10000000;
    public static final int GLS_ATEST_LT_128                 = 0x20000000;
    public static final int GLS_ATEST_GE_128                 = 0x40000000;
    public static final int GLS_ATEST_BITS                   = 0x70000000;
    //
    public static final int GLS_DEFAULT                      = GLS_DEPTHFUNC_ALWAYS;
//

    //    public static void R_Init();
    /*
     ============================================================

     DRAW_*

     ============================================================
     */
    public enum program_t {

        PROG_INVALID,
        VPROG_INTERACTION,
        VPROG_ENVIRONMENT,
        VPROG_BUMPY_ENVIRONMENT,
        VPROG_R200_INTERACTION,
        VPROG_STENCIL_SHADOW,
        VPROG_NV20_BUMP_AND_LIGHT,
        VPROG_NV20_DIFFUSE_COLOR,
        VPROG_NV20_SPECULAR_COLOR,
        VPROG_NV20_DIFFUSE_AND_SPECULAR_COLOR,
        VPROG_TEST,
        FPROG_INTERACTION,
        FPROG_ENVIRONMENT,
        FPROG_BUMPY_ENVIRONMENT,
        FPROG_TEST,
        VPROG_AMBIENT,
        FPROG_AMBIENT,
        VPROG_GLASSWARP,
        FPROG_GLASSWARP,
        PROG_USER
    }

    /*

     All vertex programs use the same constant register layout:

     c[4]	localLightOrigin
     c[5]	localViewOrigin
     c[6]	lightProjection S
     c[7]	lightProjection T
     c[8]	lightProjection Q
     c[9]	lightFalloff	S
     c[10]	bumpMatrix S
     c[11]	bumpMatrix T
     c[12]	diffuseMatrix S
     c[13]	diffuseMatrix T
     c[14]	specularMatrix S
     c[15]	specularMatrix T


     c[20]	light falloff tq constant

     // texture 0 was cube map
     // texture 1 will be the per-surface bump map
     // texture 2 will be the light falloff texture
     // texture 3 will be the light projection texture
     // texture 4 is the per-surface diffuse map
     // texture 5 is the per-surface specular map
     // texture 6 is the specular half angle cube map

     */
    public enum programParameter_t {

        _0_, _1_, _2_, _3_,//fillers
        //        
        PP_LIGHT_ORIGIN,//= 4,
        PP_VIEW_ORIGIN,
        PP_LIGHT_PROJECT_S,
        PP_LIGHT_PROJECT_T,
        PP_LIGHT_PROJECT_Q,
        PP_LIGHT_FALLOFF_S,
        PP_BUMP_MATRIX_S,
        PP_BUMP_MATRIX_T,
        PP_DIFFUSE_MATRIX_S,
        PP_DIFFUSE_MATRIX_T,
        PP_SPECULAR_MATRIX_S,
        PP_SPECULAR_MATRIX_T,
        PP_COLOR_MODULATE,
        PP_COLOR_ADD,
        //        
        _8_, _9_,//more fillers
        //
        PP_LIGHT_FALLOFF_TQ //= 20	// only for NV programs
    }

    /*
     ============================================================

     util/shadowopt3

     dmap time optimization of shadow volumes, called from R_CreateShadowVolume

     ============================================================
     */
    public static class optimizedShadow_t {

        public idVec3[]           verts;                  // includes both front and back projections, caller should free
        public int                numVerts;
        public int/*glIndex_t*/[] indexes;    // caller should free
        //
        // indexes must be sorted frontCap, rearCap, silPlanes so the caps can be removed
        // when the viewer is in a position that they don't need to see them
        public int                numFrontCapIndexes;
        public int                numRearCapIndexes;
        public int                numSilPlaneIndexes;
        public int                totalIndexes;
    }
//optimizedShadow_t SuperOptimizeOccluders( idVec4 *verts, glIndex_t *indexes, int numIndexes,
//										 idPlane projectionPlane, idVec3 projectionOrigin );
//
//void CleanupOptimizedShadowTris( srfTriangles_t *tri );

    /*
     ============================================================

     TRISURF

     ============================================================
     */
    public static final boolean USE_TRI_DATA_ALLOCATOR = true;

    // deformable meshes precalculate as much as possible from a base frame, then generate
    // complete srfTriangles_t from just a new set of vertexes
    public static class deformInfo_s {
        public static final int BYTES = Integer.BYTES * 11;

        int   numSourceVerts;
        // numOutputVerts may be smaller if the input had duplicated or degenerate triangles
        // it will often be larger if the input had mirrored texture seams that needed
        // to be busted for proper tangent spaces
        int   numOutputVerts;
        //
        int   numMirroredVerts;
        int[] mirroredVerts;
        //
        //int   numIndexes;
        //int[]/*glIndex_t */ indexes;
        private NeoIntBuffer indexes = new NeoIntBuffer();
        //
        int[]/*glIndex_t */ silIndexes;
        //
        int             numDupVerts;
        int[]           dupVerts;
        //
        int             numSilEdges;
        silEdge_t[]     silEdges;
        //
        dominantTri_s[] dominantTris;

        public NeoIntBuffer getIndexes() {
			return this.indexes;
		}
    }

    /*
     =============================================================

     TR_TRACE

     =============================================================
     */
    public static class localTrace_t {

        float  fraction;
        // only valid if fraction < 1.0
        idVec3 point;
        idVec3 normal;
        final int[] indexes = new int[3];
    }
}
