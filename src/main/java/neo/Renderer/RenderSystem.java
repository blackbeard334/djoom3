package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.RenderSystem_init.R_SetColorMappings;
import static neo.Renderer.RenderSystem_init.r_brightness;
import static neo.Renderer.RenderSystem_init.r_gamma;
import static neo.Renderer.RenderSystem_init.r_logFile;
import static neo.Renderer.RenderSystem_init.r_showAlloc;
import static neo.Renderer.RenderSystem_init.r_showCull;
import static neo.Renderer.RenderSystem_init.r_showDefs;
import static neo.Renderer.RenderSystem_init.r_showDynamic;
import static neo.Renderer.RenderSystem_init.r_showInteractions;
import static neo.Renderer.RenderSystem_init.r_showLightScale;
import static neo.Renderer.RenderSystem_init.r_showMemory;
import static neo.Renderer.RenderSystem_init.r_showPrimitives;
import static neo.Renderer.RenderSystem_init.r_showSurfaces;
import static neo.Renderer.RenderSystem_init.r_showUpdates;
import static neo.Renderer.RenderSystem_init.r_skipBackEnd;
import static neo.Renderer.tr_backend.RB_ExecuteBackEndCommands;
import static neo.Renderer.tr_local.backEnd;
import static neo.Renderer.tr_local.frameData;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_local.renderCommand_t.RC_DRAW_VIEW;
import static neo.Renderer.tr_local.renderCommand_t.RC_NOP;
import static neo.Renderer.tr_main.R_CountFrameData;
import static neo.Renderer.tr_main.R_SetViewMatrix;
import static neo.Renderer.tr_main.myGlMultMatrix;
import static neo.TempDump.NOT;
import static neo.framework.Common.common;
import static neo.sys.win_glimp.GLimp_EnableLogging;

import java.nio.ByteBuffer;

import neo.TempDump.CPP_class.Char;
import neo.TempDump.CPP_class.Pointer;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Renderer.tr_local.drawSurfsCommand_t;
import neo.Renderer.tr_local.emptyCommand_t;
import neo.Renderer.tr_local.viewDef_s;
import neo.Renderer.tr_local.viewEntity_s;
import neo.framework.Common.MemInfo_t;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class RenderSystem {

    public static idRenderSystem renderSystem = tr_local.tr;

    /*
     ===============================================================================

     idRenderSystem is responsible for managing the screen, which can have
     multiple idRenderWorld and 2D drawing done on it.

     ===============================================================================
     */
    public static class glconfig_s {

        public String renderer_string;
        public String vendor_string;
        public String version_string;
        public String extensions_string;
        public String wgl_extensions_string;
//   
        public float glVersion;				// atof( version_string )
//   
//   
        public int maxTextureSize;			// queried from GL
        public int maxTextureUnits;
        public int maxTextureCoords;
        public int maxTextureImageUnits;
        public float maxTextureAnisotropy;
//   
        public int colorBits, depthBits, stencilBits = 8;
//   
        public boolean multitextureAvailable;
        public boolean textureCompressionAvailable;
        public boolean anisotropicAvailable;
        public boolean textureLODBiasAvailable;
        public boolean textureEnvAddAvailable;
        public boolean textureEnvCombineAvailable;
        public boolean registerCombinersAvailable;
        public boolean cubeMapAvailable;
        public boolean envDot3Available;
        public boolean texture3DAvailable;
        public boolean sharedTexturePaletteAvailable;
        public boolean ARBVertexBufferObjectAvailable;
        public boolean ARBVertexProgramAvailable;
        public boolean ARBFragmentProgramAvailable;
        public boolean twoSidedStencilAvailable;
        public boolean textureNonPowerOfTwoAvailable;
        public boolean depthBoundsTestAvailable;
//   
        // ati r200 extensions
        public boolean atiFragmentShaderAvailable;
//   
        // ati r300
        public boolean atiTwoSidedStencilAvailable;
//   
        public int vidWidth, vidHeight;	// passed to R_BeginFrame
//   
        public int displayFrequency;
//   
        public boolean isFullscreen;
//   
        public boolean allowNV30Path;
        public boolean allowNV20Path;
        public boolean allowNV10Path;
        public boolean allowR200Path;
        public boolean allowARB2Path;
//   
        public boolean isInitialized;
    }
    // font support 
    public static final int GLYPH_START = 0;
    public static final int GLYPH_END = 255;
    public static final int GLYPH_CHARSTART = 32;
    public static final int GLYPH_CHAREND = 127;
    public static final int GLYPHS_PER_FONT = (GLYPH_END - GLYPH_START) + 1;

    public static class glyphInfo_t {

        public static final transient int SIZE
                = Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Float.SIZE
                + Float.SIZE
                + Float.SIZE
                + Float.SIZE
                + Pointer.SIZE//const idMaterial *	glyph
                + (Char.SIZE * 32);

        public int height;		// number of scan lines
        public int top;		// top of glyph in buffer
        int bottom;		// bottom of glyph in buffer
        int pitch;		// width for copying
        public int xSkip;		// x adjustment
        public int imageWidth;		// width of actual image
        public int imageHeight;	// height of actual image
        public float s;		// x offset in image where glyph starts
        public float t;		// y offset in image where glyph starts
        public float s2;
        public float t2;
        public idMaterial glyph;	// shader with the glyph
        // char				shaderName[32];
        String shaderName;
    }

    public static class fontInfo_t {

        public static final transient int SIZE
                = (glyphInfo_t.SIZE * GLYPHS_PER_FONT)
                + Float.SIZE
                + (Char.SIZE * 64);
        public static final transient int BYTES = SIZE / Byte.SIZE;

        public glyphInfo_t[] glyphs = new glyphInfo_t[GLYPHS_PER_FONT];
        public float glyphScale;
        public StringBuilder name = new StringBuilder(64);

        public fontInfo_t() {
            for (int g = 0; g < this.glyphs.length; g++) {
                this.glyphs[g] = new glyphInfo_t();
            }
        }
    }

    public static class fontInfoEx_t {

        public fontInfo_t fontInfoSmall = new fontInfo_t();
        public fontInfo_t fontInfoMedium = new fontInfo_t();
        public fontInfo_t fontInfoLarge = new fontInfo_t();
        public int maxHeight;
        public int maxWidth;
        public int maxHeightSmall;
        public int maxWidthSmall;
        public int maxHeightMedium;
        public int maxWidthMedium;
        public int maxHeightLarge;
        public int maxWidthLarge;
        // char				name[64];
        public String name;

        /**
         * memset(font, 0, sizeof(font));
         */
        public void clear() {
            this.fontInfoSmall = new fontInfo_t();
            this.fontInfoMedium = new fontInfo_t();
            this.fontInfoLarge = new fontInfo_t();
            this.maxHeight = this.maxWidth
                    = this.maxHeightSmall = this.maxWidthSmall = this.maxHeightMedium
                    = this.maxWidthMedium = this.maxHeightLarge = this.maxWidthLarge = 0;
            this.name = null;
        }
    }
    public static final int SMALLCHAR_WIDTH = 8;
    public static final int SMALLCHAR_HEIGHT = 16;
    public static final int BIGCHAR_WIDTH = 16;
    public static final int BIGCHAR_HEIGHT = 16;
//    
// all drawing is done to a 640 x 480 virtual screen size
// and will be automatically scaled to the real resolution
    public static final int SCREEN_WIDTH = 640;
    public static final int SCREEN_HEIGHT = 480;

    public static abstract class idRenderSystem {

        // virtual					~idRenderSystem() {}
        // set up cvars and basic data structures, but don't
        // init OpenGL, so it can also be used for dedicated servers
        public abstract void Init();

        // only called before quitting
        public abstract void Shutdown();

        public abstract void InitOpenGL();

        public abstract void ShutdownOpenGL();

        public abstract boolean IsOpenGLRunning();

        public abstract boolean IsFullScreen();

        public abstract int GetScreenWidth();

        public abstract int GetScreenHeight();

        // allocate a renderWorld to be used for drawing
        public abstract idRenderWorld AllocRenderWorld();

        public abstract void FreeRenderWorld(idRenderWorld rw);

        // All data that will be used in a level should be
        // registered before rendering any frames to prevent disk hits,
        // but they can still be registered at a later time
        // if necessary.
        public abstract void BeginLevelLoad();

        public abstract void EndLevelLoad();

        // font support
        public abstract boolean RegisterFont(final String fontName, fontInfoEx_t font);

        // GUI drawing just involves shader parameter setting and axial image subsections
        public abstract void SetColor(final idVec4 rgba);

        public abstract void SetColor4(float r, float g, float b, float a);

        public abstract void DrawStretchPic(final idDrawVert[] verts, final int/*glIndex_t*/[] indexes, int vertCount, int indexCount, final idMaterial material,
                boolean clip /*= true*/, float min_x/* = 0.0f*/, float min_y /*= 0.0f*/, float max_x/*= 640.0f*/, float max_y /*= 480.0f */);

        public void DrawStretchPic(final idDrawVert[] verts, final int/*glIndex_t*/[] indexes, int vertCount, int indexCount, final idMaterial material,
                boolean clip /*= true*/, float min_x/* = 0.0f*/, float min_y /*= 0.0f*/, float max_x/*= 640.0f*/) {
            DrawStretchPic(verts, indexes, vertCount, indexCount, material, clip, min_x, min_y, max_x, 480.0f);
        }

        public void DrawStretchPic(final idDrawVert[] verts, final int/*glIndex_t*/[] indexes, int vertCount, int indexCount, final idMaterial material,
                boolean clip /*= true*/, float min_x/* = 0.0f*/, float min_y /*= 0.0f*/) {
            DrawStretchPic(verts, indexes, vertCount, indexCount, material, clip, min_x, min_y, 640.0f);
        }

        public void DrawStretchPic(final idDrawVert[] verts, final int/*glIndex_t*/[] indexes, int vertCount, int indexCount, final idMaterial material,
                boolean clip /*= true*/, float min_x/* = 0.0f*/) {
            DrawStretchPic(verts, indexes, vertCount, indexCount, material, clip, min_x, 0.0f);
        }

        public void DrawStretchPic(final idDrawVert[] verts, final int/*glIndex_t*/[] indexes, int vertCount, int indexCount, final idMaterial material, boolean clip /*= true*/) {
            DrawStretchPic(verts, indexes, vertCount, indexCount, material, clip, 0.0f);
        }

        public void DrawStretchPic(final idDrawVert[] verts, final int/*glIndex_t*/[] indexes, int vertCount, int indexCount, final idMaterial material) {
            DrawStretchPic(verts, indexes, vertCount, indexCount, material, true);
        }

        public abstract void DrawStretchPic(float x, float y, float w, float h, float s1, float t1, float s2, float t2, final idMaterial material);

        public abstract void DrawStretchTri(idVec2 p1, idVec2 p2, idVec2 p3, idVec2 t1, idVec2 t2, idVec2 t3, final idMaterial material);

        public abstract void GlobalToNormalizedDeviceCoordinates(final idVec3 global, idVec3 ndc);

        public abstract void GetGLSettings(int[] width, int[] height);

        public abstract void PrintMemInfo(MemInfo_t mi);

        public abstract void DrawSmallChar(int x, int y, int ch, final idMaterial material);

        public abstract void DrawSmallStringExt(int x, int y, final char[] string, final idVec4 setColor, boolean forceColor, final idMaterial material);

        public abstract void DrawBigChar(int x, int y, int ch, final idMaterial material);

        public abstract void DrawBigStringExt(int x, int y, final String string, final idVec4 setColor, boolean forceColor, final idMaterial material);

        // dump all 2D drawing so far this frame to the demo file
        public abstract void WriteDemoPics();

        // draw the 2D pics that were saved out with the current demo frame
        public abstract void DrawDemoPics();

        // FIXME: add an interface for arbitrary point/texcoord drawing
        // a frame cam consist of 2D drawing and potentially multiple 3D scenes
        // window sizes are needed to convert SCREEN_WIDTH / SCREEN_HEIGHT values
        public abstract void BeginFrame(int windowWidth, int windowHeight);

        // if the pointers are not NULL, timing info will be returned
        public abstract void EndFrame(int[] frontEndMsec, int[] backEndMsec);

        // aviDemo uses this.
        // Will automatically tile render large screen shots if necessary
        // Samples is the number of jittered frames for anti-aliasing
        // If ref == NULL, session->updateScreen will be used
        // This will perform swapbuffers, so it is NOT an approppriate way to
        // generate image files that happen during gameplay, as for savegame
        // markers.  Use WriteRender() instead.
        public abstract void TakeScreenshot(int width, int height, final String fileName, int samples, renderView_s ref);

        // the render output can be cropped down to a subset of the real screen, as
        // for save-game reviews and split-screen multiplayer.  Users of the renderer
        // will not know the actual pixel size of the area they are rendering to
        // the x,y,width,height values are in public abstract SCREEN_WIDTH / SCREEN_HEIGHT coordinates
        // to render to a texture, first set the crop size with makePowerOfTwo = true,
        // then perform all desired rendering, then capture to an image
        // if the specified physical dimensions are larger than the current cropped region, they will be cut down to fit
        public abstract void CropRenderSize(int width, int height, boolean makePowerOfTwo /*= false*/, boolean forceDimensions /*= false */);

        public void CropRenderSize(int width, int height, boolean makePowerOfTwo /*= false*/) {
            CropRenderSize(width, height, makePowerOfTwo, false);
        }

        public void CropRenderSize(int width, int height) {
            CropRenderSize(width, height, false);
        }

        public abstract void CaptureRenderToImage(final String imageName);
        // fixAlpha will set all the alpha channel values to 0xff, which allows screen captures
        // to use the default tga loading code without having dimmed down areas in many places

        public abstract void CaptureRenderToFile(final String fileName, boolean fixAlpha/* = false */);

        public void CaptureRenderToFile(final String fileName) {
            CaptureRenderToFile(fileName, false);
        }

        public abstract void UnCrop();

        public abstract void GetCardCaps(boolean[] oldCard, boolean[] nv10or20);

        // the image has to be already loaded ( most straightforward way would be through a FindMaterial )
        // texture filter / mipmapping / repeat won't be modified by the upload
        // returns false if the image wasn't found
        public abstract boolean UploadImage(final String imageName, final ByteBuffer data, int width, int height);
    }

    /*
     =====================
     R_PerformanceCounters

     This prints both front and back end counters, so it should
     only be called when the back end thread is idle.
     =====================
     */
    static void R_PerformanceCounters() {
        if (r_showPrimitives.GetInteger() != 0) {

            final float megaBytes = globalImages.SumOfUsedImages() / (1024 * 1024.0f);

            if (r_showPrimitives.GetInteger() > 1) {
                common.Printf("v:%d ds:%d t:%d/%d v:%d/%d st:%d sv:%d image:%5.1f MB\n",
                        tr.pc.c_numViews,
                        backEnd.pc.c_drawElements + backEnd.pc.c_shadowElements,
                        backEnd.pc.c_drawIndexes / 3,
                        (backEnd.pc.c_drawIndexes - backEnd.pc.c_drawRefIndexes) / 3,
                        backEnd.pc.c_drawVertexes,
                        (backEnd.pc.c_drawVertexes - backEnd.pc.c_drawRefVertexes),
                        backEnd.pc.c_shadowIndexes / 3,
                        backEnd.pc.c_shadowVertexes,
                        megaBytes);
            } else {
                common.Printf("views:%d draws:%d tris:%d (shdw:%d) (vbo:%d) image:%5.1f MB\n",
                        tr.pc.c_numViews,
                        backEnd.pc.c_drawElements + backEnd.pc.c_shadowElements,
                        (backEnd.pc.c_drawIndexes + backEnd.pc.c_shadowIndexes) / 3,
                        backEnd.pc.c_shadowIndexes / 3,
                        backEnd.pc.c_vboIndexes / 3,
                        megaBytes);
            }
        }

        if (r_showDynamic.GetBool()) {
            common.Printf("callback:%d md5:%d dfrmVerts:%d dfrmTris:%d tangTris:%d guis:%d\n",
                    tr.pc.c_entityDefCallbacks,
                    tr.pc.c_generateMd5,
                    tr.pc.c_deformedVerts,
                    tr.pc.c_deformedIndexes / 3,
                    tr.pc.c_tangentIndexes / 3,
                    tr.pc.c_guiSurfs);
        }

        if (r_showCull.GetBool()) {
            common.Printf("%d sin %d sclip  %d sout %d bin %d bout\n",
                    tr.pc.c_sphere_cull_in, tr.pc.c_sphere_cull_clip, tr.pc.c_sphere_cull_out,
                    tr.pc.c_box_cull_in, tr.pc.c_box_cull_out);
        }

        if (r_showAlloc.GetBool()) {
            common.Printf("alloc:%d free:%d\n", tr.pc.c_alloc, tr.pc.c_free);
        }

        if (r_showInteractions.GetBool()) {
            common.Printf("createInteractions:%d createLightTris:%d createShadowVolumes:%d\n",
                    tr.pc.c_createInteractions, tr.pc.c_createLightTris, tr.pc.c_createShadowVolumes);
        }
        if (r_showDefs.GetBool()) {
            common.Printf("viewEntities:%d  shadowEntities:%d  viewLights:%d\n", tr.pc.c_visibleViewEntities,
                    tr.pc.c_shadowViewEntities, tr.pc.c_viewLights);
        }
        if (r_showUpdates.GetBool()) {
            common.Printf("entityUpdates:%d  entityRefs:%d  lightUpdates:%d  lightRefs:%d\n",
                    tr.pc.c_entityUpdates, tr.pc.c_entityReferences,
                    tr.pc.c_lightUpdates, tr.pc.c_lightReferences);
        }
        if (r_showMemory.GetBool()) {
            final int m1 = frameData != null ? frameData.memoryHighwater : 0;
            common.Printf("frameData: %d (%d)\n", R_CountFrameData(), m1);
        }
        if (r_showLightScale.GetBool()) {
            common.Printf("lightScale: %f\n", backEnd.pc.maxLightValue);
        }

//        memset(tr.pc, 0, sizeof(tr.pc));
        tr.pc = new tr_local.performanceCounters_t();
//        memset(backEnd.pc, 0, sizeof(backEnd.pc));
        backEnd.pc = new tr_local.backEndCounters_t();
    }

    /*
     ====================
     R_IssueRenderCommands

     Called by R_EndFrame each frame
     ====================
     */
    static void R_IssueRenderCommands() {
        if ((RC_NOP == frameData.cmdHead.commandId) && NOT(frameData.cmdHead.next)) {
            // nothing to issue
            return;
        }

        // r_skipBackEnd allows the entire time of the back end
        // to be removed from performance measurements, although
        // nothing will be drawn to the screen.  If the prints
        // are going to a file, or r_skipBackEnd is later disabled,
        // usefull data can be received.
        //
        // r_skipRender is usually more useful, because it will still
        // draw 2D graphics
        if (!r_skipBackEnd.GetBool()) {
            RB_ExecuteBackEndCommands(frameData.cmdHead);
        }

        R_ClearCommandChain();
    }

    /*
     ============
     R_GetCommandBuffer

     Returns memory for a command buffer (stretchPicCommand_t, 
     drawSurfsCommand_t, etc) and links it to the end of the
     current command chain.
     ============
     */
    static emptyCommand_t R_GetCommandBuffer(emptyCommand_t command_t) {
        final emptyCommand_t cmd;

//        cmd = R_FrameAlloc(bytes);
//        cmd.next = null;
        cmd = command_t;//our little trick for downcasting. EDIT:??
        frameData.cmdTail.next = cmd;
        frameData.cmdTail = cmd;

        return cmd;
    }


    /*
     ====================
     R_ClearCommandChain

     Called after every buffer submission
     and by R_ToggleSmpFrame
     ====================
     */
    static void R_ClearCommandChain() {
        // clear the command chain
        frameData.cmdHead = frameData.cmdTail = new emptyCommand_t();// R_FrameAlloc(sizeof(frameData.cmdHead));
        frameData.cmdHead.commandId = RC_NOP;
        frameData.cmdHead.next = null;
    }

    /*
     =================
     R_ViewStatistics
     =================
     */
    static void R_ViewStatistics(viewDef_s parms) {
        // report statistics about this view
        if (!r_showSurfaces.GetBool()) {
            return;
        }
        common.Printf("view:%p surfs:%d\n", parms, parms.numDrawSurfs);
    }

    /*
     =============
     R_AddDrawViewCmd

     This is the main 3D rendering command.  A single scene may
     have multiple views if a mirror, portal, or dynamic texture is present.
     =============
     */
    public static void R_AddDrawViewCmd(viewDef_s parms) {
        drawSurfsCommand_t cmd;

        R_GetCommandBuffer(cmd = new drawSurfsCommand_t()/*sizeof(cmd)*/);
        cmd.commandId = RC_DRAW_VIEW;

        cmd.viewDef = parms;

        if (parms.viewEntitys != null) {
            // save the command for r_lockSurfaces debugging
            tr.lockSurfacesCmd = cmd;
        }

        tr.pc.c_numViews++;

        R_ViewStatistics(parms);
    }

//=================================================================================
    /*
     ======================
     R_LockSurfaceScene

     r_lockSurfaces allows a developer to move around
     without changing the composition of the scene, including
     culling.  The only thing that is modified is the
     view position and axis, no front end work is done at all


     Add the stored off command again, so the new rendering will use EXACTLY
     the same surfaces, including all the culling, even though the transformation
     matricies have been changed.  This allow the culling tightness to be
     evaluated interactively.
     ======================
     */
    static void R_LockSurfaceScene(viewDef_s parms) {
        final drawSurfsCommand_t cmd;
        viewEntity_s vModel;

        // set the matrix for world space to eye space
        R_SetViewMatrix(parms);
        tr.lockSurfacesCmd.viewDef.worldSpace = parms.worldSpace;

        // update the view origin and axis, and all
        // the entity matricies
        for (vModel = tr.lockSurfacesCmd.viewDef.viewEntitys; vModel != null; vModel = vModel.next) {
            myGlMultMatrix(vModel.modelMatrix,
                    tr.lockSurfacesCmd.viewDef.worldSpace.modelViewMatrix,
                    vModel.modelViewMatrix);
        }

        // add the stored off surface commands again
//        cmd = (drawSurfsCommand_t) R_GetCommandBuffer(sizeof(cmd));
        R_GetCommandBuffer(tr.lockSurfacesCmd);//TODO:double check to make sure the casting and casting back preserves our values.
    }

    /*
     =============
     R_CheckCvars

     See if some cvars that we watch have changed
     =============
     */
    static void R_CheckCvars() {
        globalImages.CheckCvars();

        // gamma stuff
        if (r_gamma.IsModified() || r_brightness.IsModified()) {
            r_gamma.ClearModified();
            r_brightness.ClearModified();
            R_SetColorMappings();
        }

        // check for changes to logging state
        GLimp_EnableLogging(r_logFile.GetInteger() != 0);
    }

    public static void setRenderSystem(idRenderSystem renderSystem) {
        RenderSystem.renderSystem = tr_local.tr = (tr_local.idRenderSystemLocal) renderSystem;
    }
}
