package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Material.cullType_t.CT_FRONT_SIDED;
import static neo.Renderer.Material.cullType_t.CT_TWO_SIDED;
import static neo.Renderer.RenderSystem_init.r_debugLineDepthTest;
import static neo.Renderer.RenderSystem_init.r_debugLineWidth;
import static neo.Renderer.RenderSystem_init.r_debugPolygonFilled;
import static neo.Renderer.RenderSystem_init.r_showDepth;
import static neo.Renderer.RenderSystem_init.r_showDominantTri;
import static neo.Renderer.RenderSystem_init.r_showEdges;
import static neo.Renderer.RenderSystem_init.r_showIntensity;
import static neo.Renderer.RenderSystem_init.r_showLightCount;
import static neo.Renderer.RenderSystem_init.r_showLights;
import static neo.Renderer.RenderSystem_init.r_showNormals;
import static neo.Renderer.RenderSystem_init.r_showOverDraw;
import static neo.Renderer.RenderSystem_init.r_showPortals;
import static neo.Renderer.RenderSystem_init.r_showShadowCount;
import static neo.Renderer.RenderSystem_init.r_showSilhouette;
import static neo.Renderer.RenderSystem_init.r_showSurfaceInfo;
import static neo.Renderer.RenderSystem_init.r_showTangentSpace;
import static neo.Renderer.RenderSystem_init.r_showTexturePolarity;
import static neo.Renderer.RenderSystem_init.r_showTextureVectors;
import static neo.Renderer.RenderSystem_init.r_showTris;
import static neo.Renderer.RenderSystem_init.r_showUnsmoothedTangents;
import static neo.Renderer.RenderSystem_init.r_showVertexColor;
import static neo.Renderer.RenderSystem_init.r_showViewEntitys;
import static neo.Renderer.RenderSystem_init.r_testGamma;
import static neo.Renderer.RenderSystem_init.r_testGammaBias;
import static neo.Renderer.RenderSystem_init.r_useScissor;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.qgl.qglArrayElement;
import static neo.Renderer.qgl.qglBegin;
import static neo.Renderer.qgl.qglClear;
import static neo.Renderer.qgl.qglClearColor;
import static neo.Renderer.qgl.qglClearStencil;
import static neo.Renderer.qgl.qglColor3f;
import static neo.Renderer.qgl.qglColor3fv;
import static neo.Renderer.qgl.qglColor4f;
import static neo.Renderer.qgl.qglColor4fv;
import static neo.Renderer.qgl.qglColor4ubv;
import static neo.Renderer.qgl.qglDepthRange;
import static neo.Renderer.qgl.qglDisable;
import static neo.Renderer.qgl.qglDisableClientState;
import static neo.Renderer.qgl.qglDrawPixels;
import static neo.Renderer.qgl.qglEnable;
import static neo.Renderer.qgl.qglEnd;
import static neo.Renderer.qgl.qglLineWidth;
import static neo.Renderer.qgl.qglLoadIdentity;
import static neo.Renderer.qgl.qglLoadMatrixf;
import static neo.Renderer.qgl.qglMatrixMode;
import static neo.Renderer.qgl.qglOrtho;
import static neo.Renderer.qgl.qglPolygonOffset;
import static neo.Renderer.qgl.qglPopAttrib;
import static neo.Renderer.qgl.qglPopMatrix;
import static neo.Renderer.qgl.qglPushAttrib;
import static neo.Renderer.qgl.qglPushMatrix;
import static neo.Renderer.qgl.qglRasterPos2f;
import static neo.Renderer.qgl.qglReadPixels;
import static neo.Renderer.qgl.qglScissor;
import static neo.Renderer.qgl.qglStencilFunc;
import static neo.Renderer.qgl.qglStencilOp;
import static neo.Renderer.qgl.qglTexCoord2f;
import static neo.Renderer.qgl.qglVertex2f;
import static neo.Renderer.qgl.qglVertex3f;
import static neo.Renderer.qgl.qglVertex3fv;
import static neo.Renderer.qgl.qglVertexPointer;
import static neo.Renderer.simplex.NUM_SIMPLEX_CHARS;
import static neo.Renderer.simplex.simplex;
import static neo.Renderer.tr_backend.GL_Cull;
import static neo.Renderer.tr_backend.GL_State;
import static neo.Renderer.tr_backend.RB_LogComment;
import static neo.Renderer.tr_light.R_EntityDefDynamicModel;
import static neo.Renderer.tr_local.GLS_DEFAULT;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_ALWAYS;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_EQUAL;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_LESS;
import static neo.Renderer.tr_local.GLS_DEPTHMASK;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ZERO;
import static neo.Renderer.tr_local.GLS_POLYMODE_LINE;
import static neo.Renderer.tr_local.GLS_SRCBLEND_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE;
import static neo.Renderer.tr_local.GLS_SRCBLEND_SRC_ALPHA;
import static neo.Renderer.tr_local.backEnd;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_main.R_AxisToModelMatrix;
import static neo.Renderer.tr_main.R_LocalPointToGlobal;
import static neo.Renderer.tr_render.RB_DrawElementsWithCounters;
import static neo.Renderer.tr_render.RB_RenderDrawSurfListWithFunction;
import static neo.Renderer.tr_render.RB_RenderTriangleSurface;
import static neo.Renderer.tr_trace.RB_ShowTrace;
import static neo.TempDump.NOT;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Vector.VectorMA;
import static neo.idlib.math.Vector.VectorSubtract;
import static neo.open.gl.QGLConstantsIfc.GL_ALL_ATTRIB_BITS;
import static neo.open.gl.QGLConstantsIfc.GL_ALWAYS;
import static neo.open.gl.QGLConstantsIfc.GL_COLOR_BUFFER_BIT;
import static neo.open.gl.QGLConstantsIfc.GL_CULL_FACE;
import static neo.open.gl.QGLConstantsIfc.GL_DEPTH_COMPONENT;
import static neo.open.gl.QGLConstantsIfc.GL_DEPTH_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_EQUAL;
import static neo.open.gl.QGLConstantsIfc.GL_FLOAT;
import static neo.open.gl.QGLConstantsIfc.GL_INCR;
import static neo.open.gl.QGLConstantsIfc.GL_KEEP;
import static neo.open.gl.QGLConstantsIfc.GL_LINES;
import static neo.open.gl.QGLConstantsIfc.GL_LINE_LOOP;
import static neo.open.gl.QGLConstantsIfc.GL_MODELVIEW;
import static neo.open.gl.QGLConstantsIfc.GL_POLYGON;
import static neo.open.gl.QGLConstantsIfc.GL_POLYGON_OFFSET_FILL;
import static neo.open.gl.QGLConstantsIfc.GL_POLYGON_OFFSET_LINE;
import static neo.open.gl.QGLConstantsIfc.GL_PROJECTION;
import static neo.open.gl.QGLConstantsIfc.GL_QUADS;
import static neo.open.gl.QGLConstantsIfc.GL_RGBA;
import static neo.open.gl.QGLConstantsIfc.GL_SCISSOR_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_STENCIL_BUFFER_BIT;
import static neo.open.gl.QGLConstantsIfc.GL_STENCIL_INDEX;
import static neo.open.gl.QGLConstantsIfc.GL_STENCIL_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_2D;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_COORD_ARRAY;
import static neo.open.gl.QGLConstantsIfc.GL_TRIANGLES;
import static neo.open.gl.QGLConstantsIfc.GL_UNSIGNED_BYTE;
import static neo.ui.DeviceContext.idDeviceContext.colorBlue;
import static neo.ui.DeviceContext.idDeviceContext.colorRed;
import static neo.ui.DeviceContext.idDeviceContext.colorWhite;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import neo.TempDump;
import neo.Renderer.Cinematic.cinData_t;
import neo.Renderer.Image.idImage;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.shadowCache_s;
import neo.Renderer.Model.silEdge_t;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.RenderWorld.modelTrace_s;
import neo.Renderer.VertexCache.vertCache_s;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.idRenderLightLocal;
import neo.Renderer.tr_local.viewEntity_s;
import neo.Renderer.tr_local.viewLight_s;
import neo.Renderer.tr_render.RB_T_RenderTriangleSurface;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.open.ColorUtil;
import neo.open.Nio;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class tr_rendertools {

    static final int MAX_DEBUG_LINES = 16384;
//

    static class debugLine_s {

        idVec4 rgb;
        idVec3 start;
        idVec3 end;
        boolean depthTest;
        int lifeTime;
    }
//
    static final debugLine_s[] rb_debugLines = new debugLine_s[MAX_DEBUG_LINES];
    static int rb_numDebugLines = 0;
    static int rb_debugLineTime = 0;
//
    static final int MAX_DEBUG_TEXT = 512;
//

    public static class debugText_s {

        idStr text;
        idVec3 origin;
        float scale;
        idVec4 color;
        idMat3 viewAxis;
        int align;
        int lifeTime;
        boolean depthTest;

        public debugText_s() {
            this.text = new idStr();
            this.origin = new idVec3();
            this.color = new idVec4();
            this.viewAxis = new idMat3();
            this.scale = this.align = this.lifeTime = 0;
            this.depthTest = false;
        }
    }
//
    static debugText_s[] rb_debugText = TempDump.allocArray(debugText_s.class, MAX_DEBUG_TEXT);
    static int rb_numDebugText = 0;
    static int rb_debugTextTime = 0;
//
    static final int MAX_DEBUG_POLYGONS = 8192;
//

    static class debugPolygon_s {

        idVec4 rgb;
        idWinding winding = new idWinding();
        boolean depthTest;
        int lifeTime;
    }
//
    static final debugPolygon_s[] rb_debugPolygons;
    static int rb_numDebugPolygons = 0;
    static int rb_debugPolygonTime = 0;

    static {
        rb_debugPolygons = new debugPolygon_s[MAX_DEBUG_POLYGONS];

        for (int a = 0; a < rb_debugPolygons.length; a++) {
            rb_debugPolygons[a] = new debugPolygon_s();
        }
    }
//    
//    
    /*
     ================
     RB_DrawBounds
     ================
     */

    public static void RB_DrawBounds(final idBounds bounds) {
        if (bounds.IsCleared()) {
            return;
        }

        qglBegin(GL_LINE_LOOP);
        qglVertex3f(bounds.oGet(0, 0), bounds.oGet(0, 1), bounds.oGet(0, 2));
        qglVertex3f(bounds.oGet(0, 0), bounds.oGet(1, 1), bounds.oGet(0, 2));
        qglVertex3f(bounds.oGet(1, 0), bounds.oGet(1, 1), bounds.oGet(0, 2));
        qglVertex3f(bounds.oGet(1, 0), bounds.oGet(0, 1), bounds.oGet(0, 2));
        qglEnd();
        qglBegin(GL_LINE_LOOP);
        qglVertex3f(bounds.oGet(0, 0), bounds.oGet(0, 1), bounds.oGet(1, 2));
        qglVertex3f(bounds.oGet(0, 0), bounds.oGet(1, 1), bounds.oGet(1, 2));
        qglVertex3f(bounds.oGet(1, 0), bounds.oGet(1, 1), bounds.oGet(1, 2));
        qglVertex3f(bounds.oGet(1, 0), bounds.oGet(0, 1), bounds.oGet(1, 2));
        qglEnd();

        qglBegin(GL_LINES);
        qglVertex3f(bounds.oGet(0, 0), bounds.oGet(0, 1), bounds.oGet(0, 2));
        qglVertex3f(bounds.oGet(0, 0), bounds.oGet(0, 1), bounds.oGet(1, 2));

        qglVertex3f(bounds.oGet(0, 0), bounds.oGet(1, 1), bounds.oGet(0, 2));
        qglVertex3f(bounds.oGet(0, 0), bounds.oGet(1, 1), bounds.oGet(1, 2));

        qglVertex3f(bounds.oGet(1, 0), bounds.oGet(0, 1), bounds.oGet(0, 2));
        qglVertex3f(bounds.oGet(1, 0), bounds.oGet(0, 1), bounds.oGet(1, 2));

        qglVertex3f(bounds.oGet(1, 0), bounds.oGet(1, 1), bounds.oGet(0, 2));
        qglVertex3f(bounds.oGet(1, 0), bounds.oGet(1, 1), bounds.oGet(1, 2));
        qglEnd();
    }


    /*
     ================
     RB_SimpleSurfaceSetup
     ================
     */
    public static void RB_SimpleSurfaceSetup(final drawSurf_s drawSurf) {
        // change the matrix if needed
        if (drawSurf.space != backEnd.currentSpace) {
            qglLoadMatrixf(drawSurf.space.getModelViewMatrix());
            backEnd.currentSpace = drawSurf.space;
        }

        // change the scissor if needed
        if (r_useScissor.GetBool() && !backEnd.currentScissor.Equals(drawSurf.scissorRect)) {
            backEnd.currentScissor = drawSurf.scissorRect;
            qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
                    backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
                    (backEnd.currentScissor.x2 + 1) - backEnd.currentScissor.x1,
                    (backEnd.currentScissor.y2 + 1) - backEnd.currentScissor.y1);
        }
    }

    /*
     ================
     RB_SimpleWorldSetup
     ================
     */
    public static void RB_SimpleWorldSetup() {
        backEnd.currentSpace = backEnd.viewDef.worldSpace;
        qglLoadMatrixf(backEnd.viewDef.worldSpace.getModelViewMatrix());

        backEnd.currentScissor = backEnd.viewDef.scissor;
        qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
                backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
                (backEnd.currentScissor.x2 + 1) - backEnd.currentScissor.x1,
                (backEnd.currentScissor.y2 + 1) - backEnd.currentScissor.y1);
    }

    /*
     =================
     RB_PolygonClear

     This will cover the entire screen with normal rasterization.
     Texturing is disabled, but the existing glColor, glDepthMask,
     glColorMask, and the enabled state of depth buffering and
     stenciling will matter.
     =================
     */
    public static void RB_PolygonClear() {
        qglPushMatrix();
        qglPushAttrib(GL_ALL_ATTRIB_BITS);
        qglLoadIdentity();
        qglDisable(GL_TEXTURE_2D);
        qglDisable(GL_DEPTH_TEST);
        qglDisable(GL_CULL_FACE);
        qglDisable(GL_SCISSOR_TEST);
        qglBegin(GL_POLYGON);
        qglVertex3f(-20, -20, -10);
        qglVertex3f(20, -20, -10);
        qglVertex3f(20, 20, -10);
        qglVertex3f(-20, 20, -10);
        qglEnd();
        qglPopAttrib();
        qglPopMatrix();
    }

    /*
     ====================
     RB_ShowDestinationAlpha
     ====================
     */
    public static void RB_ShowDestinationAlpha() {
        GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ZERO | GLS_DEPTHMASK | GLS_DEPTHFUNC_ALWAYS);
        qglColor3f(1, 1, 1);
        RB_PolygonClear();
    }

    /*
     ===================
     RB_ScanStencilBuffer

     Debugging tool to see what values are in the stencil buffer
     ===================
     */
    public static void RB_ScanStencilBuffer() {
        final int[] counts = new int[256];
        int i;
        ByteBuffer stencilReadback;

//	memset( counts, 0, sizeof( counts ) );
        stencilReadback = ByteBuffer.allocate(glConfig.vidWidth * glConfig.vidHeight);// R_StaticAlloc(glConfig.vidWidth * glConfig.vidHeight);
        qglReadPixels(0, 0, glConfig.vidWidth, glConfig.vidHeight, GL_STENCIL_INDEX, GL_UNSIGNED_BYTE, stencilReadback);

        for (i = 0; i < (glConfig.vidWidth * glConfig.vidHeight); i++) {
            counts[stencilReadback.get(i)]++;
        }

        stencilReadback = null;// R_StaticFree(stencilReadback);

        // print some stats (not supposed to do from back end in SMP...)
        common.Printf("stencil values:\n");
        for (i = 0; i < 255; i++) {
            if (counts[i] != 0) {
                common.Printf("%d: %d\n", i, counts[i]);
            }
        }
    }


    /*
     ===================
     RB_CountStencilBuffer

     Print an overdraw count based on stencil index values
     ===================
     */
    public static void RB_CountStencilBuffer() {
        int count;
        int i;
        ByteBuffer stencilReadback;

        stencilReadback = Nio.newByteBuffer(glConfig.vidWidth * glConfig.vidHeight);// R_StaticAlloc(glConfig.vidWidth * glConfig.vidHeight);
        qglReadPixels(0, 0, glConfig.vidWidth, glConfig.vidHeight, GL_STENCIL_INDEX, GL_UNSIGNED_BYTE, stencilReadback);

        count = 0;
        for (i = 0; i < (glConfig.vidWidth * glConfig.vidHeight); i++) {
            count += stencilReadback.get(i);
        }

        stencilReadback = null;// R_StaticFree(stencilReadback);

        // print some stats (not supposed to do from back end in SMP...)
        common.Printf("overdraw: %5.1f\n", (float) count / (glConfig.vidWidth * glConfig.vidHeight));
    }

    /*
     ===================
     R_ColorByStencilBuffer

     Sets the screen colors based on the contents of the
     stencil buffer.  Stencil of 0 = black, 1 = red, 2 = green,
     3 = blue, ..., 7+ = white
     ===================
     */
    private static final FloatBuffer[] colors/*[8][3]*/ = {
                ColorUtil.newColorFloatBuffer(0, 0, 0),
                ColorUtil.newColorFloatBuffer(1, 0, 0),
                ColorUtil.newColorFloatBuffer(0, 1, 0),
                ColorUtil.newColorFloatBuffer(0, 0, 1),
                ColorUtil.newColorFloatBuffer(0, 1, 1),
                ColorUtil.newColorFloatBuffer(1, 0, 1),
                ColorUtil.newColorFloatBuffer(1, 1, 0),
                ColorUtil.newColorFloatBuffer(1, 1, 1)
            };

    public static void R_ColorByStencilBuffer() {
        int i;

        // clear color buffer to white (>6 passes)
        qglClearColor(1, 1, 1, 1);
        qglDisable(GL_SCISSOR_TEST);
        qglClear(GL_COLOR_BUFFER_BIT);

        // now draw color for each stencil value
        qglStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        for (i = 0; i < 6; i++) {
            qglColor3fv(colors[i]);
            qglStencilFunc(GL_EQUAL, i, 255);
            RB_PolygonClear();
        }

        qglStencilFunc(GL_ALWAYS, 0, 255);
    }

//======================================================================

    /*
     ==================
     RB_ShowOverdraw
     ==================
     */
    public static void RB_ShowOverdraw() {
        final idMaterial material;
        int i;
        drawSurf_s[] drawSurfs;
        drawSurf_s surf;
        int numDrawSurfs;
        viewLight_s vLight;

        if (r_showOverDraw.GetInteger() == 0) {
            return;
        }

        material = declManager.FindMaterial("textures/common/overdrawtest", false);
        if (material == null) {
            return;
        }

        drawSurfs = backEnd.viewDef.drawSurfs;
        numDrawSurfs = backEnd.viewDef.numDrawSurfs;

        int interactions = 0;
        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            for (surf = vLight.localInteractions[0]; surf != null; surf = surf.nextOnLight) {
                interactions++;
            }
            for (surf = vLight.globalInteractions[0]; surf != null; surf = surf.nextOnLight) {//TODO:twice?
                interactions++;
            }
        }

        final drawSurf_s[] newDrawSurfs = new drawSurf_s[numDrawSurfs + interactions];// R_FrameAlloc(numDrawSurfs + interactions);

        for (i = 0; i < numDrawSurfs; i++) {
            surf = drawSurfs[i];
            if (surf.material != null) {
                surf.material = material;
            }
            newDrawSurfs[i] = surf;
        }

        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            for (surf = vLight.localInteractions[0]; surf != null; surf = surf.nextOnLight) {
                surf.material = material;
                newDrawSurfs[i++] = surf;
            }
            for (surf = vLight.globalInteractions[0]; surf != null; surf = surf.nextOnLight) {
                surf.material = material;
                newDrawSurfs[i++] = surf;
            }
            vLight.localInteractions[0] = null;
            vLight.globalInteractions[0] = null;
        }

        switch (r_showOverDraw.GetInteger()) {
            case 1: // geometry overdraw
                backEnd.viewDef.drawSurfs = newDrawSurfs;
                backEnd.viewDef.numDrawSurfs = numDrawSurfs;
                break;
            case 2: // light interaction overdraw
                backEnd.viewDef.drawSurfs[0] = newDrawSurfs[numDrawSurfs];//TODO: check pointer refs
                backEnd.viewDef.numDrawSurfs = interactions;
                break;
            case 3: // geometry + light interaction overdraw
                backEnd.viewDef.drawSurfs = newDrawSurfs;
                backEnd.viewDef.numDrawSurfs += interactions;
                break;
        }
    }

    /*
     ===================
     RB_ShowIntensity

     Debugging tool to see how much dynamic range a scene is using.
     The greatest of the rgb values at each pixel will be used, with
     the resulting color shading from red at 0 to green at 128 to blue at 255
     ===================
     */
    public static void RB_ShowIntensity() {
        ByteBuffer colorReadback;
        int i, j, c;

        if (!r_showIntensity.GetBool()) {
            return;
        }

        colorReadback = ByteBuffer.allocate(glConfig.vidWidth * glConfig.vidHeight * 4);// R_StaticAlloc(glConfig.vidWidth * glConfig.vidHeight * 4);
        qglReadPixels(0, 0, glConfig.vidWidth, glConfig.vidHeight, GL_RGBA, GL_UNSIGNED_BYTE, colorReadback);

        c = glConfig.vidWidth * glConfig.vidHeight * 4;
        for (i = 0; i < c; i += 4) {
            j = colorReadback.get(i);
            if (colorReadback.get(i + 1) > j) {
                j = colorReadback.get(i + 1);
            }
            if (colorReadback.get(i + 2) > j) {
                j = colorReadback.get(i + 2);
            }
            if (j < 128) {
                colorReadback.put(i + 0, (byte) (2 * (128 - j)));
                colorReadback.put(i + 1, (byte) (2 * j));
                colorReadback.put(i + 2, (byte) 0);
            } else {
                colorReadback.put(i + 0, (byte) 0);
                colorReadback.put(i + 1, (byte) (2 * (255 - j)));
                colorReadback.put(i + 2, (byte) (2 * (j - 128)));
            }
        }

        // draw it back to the screen
        qglLoadIdentity();
        qglMatrixMode(GL_PROJECTION);
        GL_State(GLS_DEPTHFUNC_ALWAYS);
        qglPushMatrix();
        qglLoadIdentity();
        qglOrtho(0, 1, 0, 1, -1, 1);
        qglRasterPos2f(0, 0);
        qglPopMatrix();
        qglColor3f(1, 1, 1);
        globalImages.BindNull();
        qglMatrixMode(GL_MODELVIEW);

        qglDrawPixels(glConfig.vidWidth, glConfig.vidHeight, GL_RGBA, GL_UNSIGNED_BYTE, colorReadback);
//
//        R_StaticFree(colorReadback);
    }


    /*
     ===================
     RB_ShowDepthBuffer

     Draw the depth buffer as colors
     ===================
     */
    public static void RB_ShowDepthBuffer() {
        ByteBuffer depthReadback;

        if (!r_showDepth.GetBool()) {
            return;
        }

        qglPushMatrix();
        qglLoadIdentity();
        qglMatrixMode(GL_PROJECTION);
        qglPushMatrix();
        qglLoadIdentity();
        qglOrtho(0, 1, 0, 1, -1, 1);
        qglRasterPos2f(0, 0);
        qglPopMatrix();
        qglMatrixMode(GL_MODELVIEW);
        qglPopMatrix();

        GL_State(GLS_DEPTHFUNC_ALWAYS);
        qglColor3f(1, 1, 1);
        globalImages.BindNull();

        depthReadback = Nio.newByteBuffer(glConfig.vidWidth * glConfig.vidHeight * 4);// R_StaticAlloc(glConfig.vidWidth * glConfig.vidHeight * 4);
//	memset( depthReadback, 0, glConfig.vidWidth * glConfig.vidHeight*4 );

        qglReadPixels(0, 0, glConfig.vidWidth, glConfig.vidHeight, GL_DEPTH_COMPONENT, GL_FLOAT, depthReadback);

//if (false){
//	for ( i = 0 ; i < glConfig.vidWidth * glConfig.vidHeight ; i++ ) {
//		((byte *)depthReadback)[i*4] = 
//		((byte *)depthReadback)[i*4+1] = 
//		((byte *)depthReadback)[i*4+2] = 255 * ((float *)depthReadback)[i];
//		((byte *)depthReadback)[i*4+3] = 1;
//	}
//}
        qglDrawPixels(glConfig.vidWidth, glConfig.vidHeight, GL_RGBA, GL_UNSIGNED_BYTE, depthReadback);
//        R_StaticFree(depthReadback);
    }

    /*
     =================
     RB_ShowLightCount

     This is a debugging tool that will draw each surface with a color
     based on how many lights are effecting it
     =================
     */
    public static void RB_ShowLightCount() {
        int i;
        drawSurf_s surf;
        viewLight_s vLight;

        if (!r_showLightCount.GetBool()) {
            return;
        }

        GL_State(GLS_DEPTHFUNC_EQUAL);

        RB_SimpleWorldSetup();
        qglClearStencil(0);
        qglClear(GL_STENCIL_BUFFER_BIT);

        qglEnable(GL_STENCIL_TEST);

        // optionally count everything through walls
        if (r_showLightCount.GetInteger() >= 2) {
            qglStencilOp(GL_KEEP, GL_INCR, GL_INCR);
        } else {
            qglStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
        }

        qglStencilFunc(GL_ALWAYS, 1, 255);

        globalImages.defaultImage.Bind();

        int counter = 0;
        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            for (i = 0; i < 2; i++) {
                for (surf = (i != 0 ? vLight.localInteractions[0] : vLight.globalInteractions[0]); surf != null; surf = surf.nextOnLight) {
                    RB_SimpleSurfaceSetup(surf);counter++;
                    if (NOT(surf.geo.ambientCache)) {
                        continue;
                    }

                    final idDrawVert ac = new idDrawVert(vertexCache.Position(surf.geo.ambientCache));//TODO:figure out how to work these damn casts.
                    qglVertexPointer(3, GL_FLOAT, idDrawVert.BYTES, ac.xyzOffset());
                    RB_DrawElementsWithCounters(surf.geo);
                }
            }
        }

        // display the results
        R_ColorByStencilBuffer();

        if (r_showLightCount.GetInteger() > 2) {
            RB_CountStencilBuffer();
        }
    }


    /*
     =================
     RB_ShowSilhouette

     Blacks out all edges, then adds color for each edge that a shadow
     plane extends from, allowing you to see doubled edges
     =================
     */
    public static void RB_ShowSilhouette() {
        int i;
        drawSurf_s surf;
        viewLight_s vLight;

        if (!r_showSilhouette.GetBool()) {
            return;
        }

        //
        // clear all triangle edges to black
        //
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_TEXTURE_2D);
        qglDisable(GL_STENCIL_TEST);

        qglColor3f(0, 0, 0);

        GL_State(GLS_POLYMODE_LINE);

        GL_Cull(CT_TWO_SIDED);
        qglDisable(GL_DEPTH_TEST);

        RB_RenderDrawSurfListWithFunction(backEnd.viewDef.drawSurfs, backEnd.viewDef.numDrawSurfs, RB_T_RenderTriangleSurface.INSTANCE);

        //
        // now blend in edges that cast silhouettes
        //
        RB_SimpleWorldSetup();
        qglColor3f(0.5f, 0, 0);
        GL_State(GLS_SRCBLEND_ONE | GLS_DSTBLEND_ONE);

        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            for (i = 0; i < 2; i++) {
                surf = (i != 0 ? vLight.localShadows[0] : vLight.globalShadows[0]);
                for (; surf != null; surf = surf.nextOnLight) {
                    RB_SimpleSurfaceSetup(surf);

                    final srfTriangles_s tri = surf.geo;

                    for (final vertCache_s shadow : tri.shadowCache) {
                        qglVertexPointer(3, GL_FLOAT, shadowCache_s.BYTES, vertexCache.Position(shadow).getInt());
                    }
                    qglBegin(GL_LINES);

                    for (int j = 0; j < tri.getIndexes().getNumValues(); j += 3) {
                        final int i1 = tri.getIndexes().getValues().get(j + 0);
                        final int i2 = tri.getIndexes().getValues().get(j + 1);
                        final int i3 = tri.getIndexes().getValues().get(j + 2);

                        if (((i1 & 1) + (i2 & 1) + (i3 & 1)) == 1) {
                            if (((i1 & 1) + (i2 & 1)) == 0) {
                                qglArrayElement(i1);
                                qglArrayElement(i2);
                            } else if (((i1 & 1) + (i3 & 1)) == 0) {
                                qglArrayElement(i1);
                                qglArrayElement(i3);
                            }
                        }
                    }
                    qglEnd();

                }
            }
        }

        qglEnable(GL_DEPTH_TEST);

        GL_State(GLS_DEFAULT);
        qglColor3f(1, 1, 1);
        GL_Cull(CT_FRONT_SIDED);
    }

    /*
     =================
     RB_ShowShadowCount

     This is a debugging tool that will draw only the shadow volumes
     and count up the total fill usage
     =================
     */
    public static void RB_ShowShadowCount() {
        int i;
        drawSurf_s surf;
        viewLight_s vLight;

        if (!r_showShadowCount.GetBool()) {
            return;
        }

        GL_State(GLS_DEFAULT);

        qglClearStencil(0);
        qglClear(GL_STENCIL_BUFFER_BIT);

        qglEnable(GL_STENCIL_TEST);

        qglStencilOp(GL_KEEP, GL_INCR, GL_INCR);

        qglStencilFunc(GL_ALWAYS, 1, 255);

        globalImages.defaultImage.Bind();

        // draw both sides
        GL_Cull(CT_TWO_SIDED);

        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            for (i = 0; i < 2; i++) {
                for (surf = (i != 0 ? vLight.localShadows[0] : vLight.globalShadows[0]); surf != null; surf = surf.nextOnLight) {
                    RB_SimpleSurfaceSetup(surf);
                    final srfTriangles_s tri = surf.geo;
                    if (NOT(tri.shadowCache)) {
                        continue;
                    }

                    if (r_showShadowCount.GetInteger() == 3) {
                        // only show turboshadows
                        if (tri.numShadowIndexesNoCaps != tri.getIndexes().getNumValues()) {
                            continue;
                        }
                    }
                    if (r_showShadowCount.GetInteger() == 4) {
                        // only show static shadows
                        if (tri.numShadowIndexesNoCaps == tri.getIndexes().getNumValues()) {
                            continue;
                        }
                    }

                    final ByteBuffer cache = vertexCache.Position(tri.shadowCache);//TODO:figure out how to work these damn casts.
                    qglVertexPointer(4, GL_FLOAT, shadowCache_s.BYTES/*sizeof(cache)*/, cache);
                    RB_DrawElementsWithCounters(tri);
                }
            }
        }

        // display the results
        R_ColorByStencilBuffer();

        if (r_showShadowCount.GetInteger() == 2) {
            common.Printf("all shadows ");
        } else if (r_showShadowCount.GetInteger() == 3) {
            common.Printf("turboShadows ");
        } else if (r_showShadowCount.GetInteger() == 4) {
            common.Printf("static shadows ");
        }

        if (r_showShadowCount.GetInteger() >= 2) {
            RB_CountStencilBuffer();
        }

        GL_Cull(CT_FRONT_SIDED);
    }


    /*
     ===============
     RB_T_RenderTriangleSurfaceAsLines

     ===============
     */
    public static void RB_T_RenderTriangleSurfaceAsLines(final drawSurf_s surf) {
        final srfTriangles_s tri = surf.geo;

        if (null == tri.verts) {
            return;
        }

        qglBegin(GL_LINES);
        for (int i = 0; i < tri.getIndexes().getNumValues(); i += 3) {
            for (int j = 0; j < 3; j++) {
                final int k = (j + 1) % 3;
                qglVertex3fv(tri.verts[ tri.silIndexes[i + j]].xyz.toFloatBuffer());
                qglVertex3fv(tri.verts[ tri.silIndexes[i + k]].xyz.toFloatBuffer());
            }
        }
        qglEnd();
    }


    /*
     =====================
     RB_ShowTris

     Debugging tool
     =====================
     */
    public static void RB_ShowTris(drawSurf_s[] drawSurfs, int numDrawSurfs) {
//	modelTrace_s mt;
//	idVec3 end;

        if (0 == r_showTris.GetInteger()) {
            return;
        }

        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_TEXTURE_2D);
        qglDisable(GL_STENCIL_TEST);

        qglColor3f(1, 1, 1);

        GL_State(GLS_POLYMODE_LINE);

        switch (r_showTris.GetInteger()) {
            case 1:	// only draw visible ones
                qglPolygonOffset(-1, -2);
                qglEnable(GL_POLYGON_OFFSET_LINE);
                break;
            default:
            case 2:	// draw all front facing
                GL_Cull(CT_FRONT_SIDED);
                qglDisable(GL_DEPTH_TEST);
                break;
            case 3: // draw all
                GL_Cull(CT_TWO_SIDED);
                qglDisable(GL_DEPTH_TEST);
                break;
        }

        RB_RenderDrawSurfListWithFunction(drawSurfs, numDrawSurfs, RB_T_RenderTriangleSurface.INSTANCE);

        qglEnable(GL_DEPTH_TEST);
        qglDisable(GL_POLYGON_OFFSET_LINE);

        qglDepthRange(0, 1);
        GL_State(GLS_DEFAULT);
        GL_Cull(CT_FRONT_SIDED);
    }


    /*
     =====================
     RB_ShowSurfaceInfo

     Debugging tool
     =====================
     */
    public static void RB_ShowSurfaceInfo(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        final modelTrace_s mt = new modelTrace_s();
        idVec3 start, end;

        if (!r_showSurfaceInfo.GetBool()) {
            return;
        }

        // start far enough away that we don't hit the player model
        start = tr.primaryView.renderView.vieworg.oPlus(tr.primaryView.renderView.viewaxis.oGet(0).oMultiply(16));
        end = start.oPlus(tr.primaryView.renderView.viewaxis.oGet(0).oMultiply(1000f));
//	end = start + tr.primaryView.renderView.viewaxis[0] * 1000.0f;
        if (!tr.primaryWorld.Trace(mt, start, end, 0.0f, false)) {
            return;
        }

        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_TEXTURE_2D);
        qglDisable(GL_STENCIL_TEST);

        qglColor3f(1, 1, 1);

        GL_State(GLS_POLYMODE_LINE);

        qglPolygonOffset(-1, -2);
        qglEnable(GL_POLYGON_OFFSET_LINE);

        final idVec3[] trans = new idVec3[3];
        final float[] matrix = new float[16];

        // transform the object verts into global space
        R_AxisToModelMatrix(mt.entity.axis, mt.entity.origin, matrix);

        tr.primaryWorld.DrawText(mt.entity.hModel.Name(), mt.point.oPlus(tr.primaryView.renderView.viewaxis.oGet(2).oMultiply(12)),
                0.35f, colorRed, tr.primaryView.renderView.viewaxis);
        tr.primaryWorld.DrawText(mt.material.GetName(), mt.point,
                0.35f, colorBlue, tr.primaryView.renderView.viewaxis);

        qglEnable(GL_DEPTH_TEST);
        qglDisable(GL_POLYGON_OFFSET_LINE);

        qglDepthRange(0, 1);
        GL_State(GLS_DEFAULT);
        GL_Cull(CT_FRONT_SIDED);
    }


    /*
     =====================
     RB_ShowViewEntitys

     Debugging tool
     =====================
     */
    public static void RB_ShowViewEntitys(viewEntity_s vModels) {//TODO:should this back ref?
        if (!r_showViewEntitys.GetBool()) {
            return;
        }
        if (r_showViewEntitys.GetInteger() == 2) {
            common.Printf("view entities: ");
            for (; vModels != null; vModels = vModels.next) {
                common.Printf("%d ", vModels.entityDef.index);
            }
            common.Printf("\n");
            return;
        }

        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_TEXTURE_2D);
        qglDisable(GL_STENCIL_TEST);

        qglColor3f(1, 1, 1);

        GL_State(GLS_POLYMODE_LINE);

        GL_Cull(CT_TWO_SIDED);
        qglDisable(GL_DEPTH_TEST);
        qglDisable(GL_SCISSOR_TEST);

        for (; vModels != null; vModels = vModels.next) {
            idBounds b;

            qglLoadMatrixf(vModels.getModelViewMatrix());
//            System.out.println("vModels.modelViewMatrix="+vModels.modelViewMatrix[0]);

            if (null == vModels.entityDef) {
                continue;
            }

            // draw the reference bounds in yellow
            qglColor3f(1, 1, 0);
            RB_DrawBounds(vModels.entityDef.referenceBounds);

            // draw the model bounds in white
            qglColor3f(1, 1, 1);

            final idRenderModel model = R_EntityDefDynamicModel(vModels.entityDef);
            if (null == model) {
                continue;	// particles won't instantiate without a current view
            }
            b = model.Bounds(vModels.entityDef.parms);
            RB_DrawBounds(b);
        }

        qglEnable(GL_DEPTH_TEST);
        qglDisable(GL_POLYGON_OFFSET_LINE);

        qglDepthRange(0, 1);
        GL_State(GLS_DEFAULT);
        GL_Cull(CT_FRONT_SIDED);
    }

    /*
     =====================
     RB_ShowTexturePolarity

     Shade triangle red if they have a positive texture area
     green if they have a negative texture area, or blue if degenerate area
     =====================
     */
    public static void RB_ShowTexturePolarity(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j;
        drawSurf_s drawSurf;
        srfTriangles_s tri;

        if (!r_showTexturePolarity.GetBool()) {
            return;
        }
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_STENCIL_TEST);

        GL_State(GLS_SRCBLEND_SRC_ALPHA | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA);

        qglColor3f(1, 1, 1);

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];
            tri = drawSurf.geo;
            if (NOT(tri.verts)) {
                continue;
            }

            RB_SimpleSurfaceSetup(drawSurf);

            qglBegin(GL_TRIANGLES);
            for (j = 0; j < tri.getIndexes().getNumValues(); j += 3) {
                idDrawVert a, b, c;
                final float[] d0 = new float[5], d1 = new float[5];
                float area;

                a = tri.verts[tri.getIndexes().getValues().get(j)];
                b = tri.verts[tri.getIndexes().getValues().get(j + 1)];
                c = tri.verts[tri.getIndexes().getValues().get(j + 2)];

                // VectorSubtract( b.xyz, a.xyz, d0 );
                d0[3] = b.st.oGet(0) - a.st.oGet(0);
                d0[4] = b.st.oGet(1) - a.st.oGet(1);
                // VectorSubtract( c.xyz, a.xyz, d1 );
                d1[3] = c.st.oGet(0) - a.st.oGet(0);
                d1[4] = c.st.oGet(1) - a.st.oGet(1);

                area = (d0[3] * d1[4]) - (d0[4] * d1[3]);

                if (idMath.Fabs(area) < 0.0001) {
                    qglColor4f(0, 0, 1, 0.5f);
                } else if (area < 0) {
                    qglColor4f(1, 0, 0, 0.5f);
                } else {
                    qglColor4f(0, 1, 0, 0.5f);
                }
                qglVertex3fv(a.xyz.toFloatBuffer());
                qglVertex3fv(b.xyz.toFloatBuffer());
                qglVertex3fv(c.xyz.toFloatBuffer());
            }
            qglEnd();
        }

        GL_State(GLS_DEFAULT);
    }


    /*
     =====================
     RB_ShowUnsmoothedTangents

     Shade materials that are using unsmoothed tangents
     =====================
     */
    public static void RB_ShowUnsmoothedTangents(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j;
        drawSurf_s drawSurf;
        srfTriangles_s tri;

        if (!r_showUnsmoothedTangents.GetBool()) {
            return;
        }
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_STENCIL_TEST);

        GL_State(GLS_SRCBLEND_SRC_ALPHA | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA);

        qglColor4f(0, 1, 0, 0.5f);

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

            if (!drawSurf.material.UseUnsmoothedTangents()) {
                continue;
            }

            RB_SimpleSurfaceSetup(drawSurf);

            tri = drawSurf.geo;
            qglBegin(GL_TRIANGLES);
            for (j = 0; j < tri.getIndexes().getNumValues(); j += 3) {
                idDrawVert a, b, c;

                a = tri.verts[tri.getIndexes().getValues().get(j)];
                b = tri.verts[tri.getIndexes().getValues().get(j + 1)];
                c = tri.verts[tri.getIndexes().getValues().get(j + 2)];

                qglVertex3fv(a.xyz.toFloatBuffer());
                qglVertex3fv(b.xyz.toFloatBuffer());
                qglVertex3fv(c.xyz.toFloatBuffer());
            }
            qglEnd();
        }

        GL_State(GLS_DEFAULT);
    }


    /*
     =====================
     RB_ShowTangentSpace

     Shade a triangle by the RGB colors of its tangent space
     1 = tangents[0]
     2 = tangents[1]
     3 = normal
     =====================
     */
    public static void RB_ShowTangentSpace(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j;
        drawSurf_s drawSurf;
        srfTriangles_s tri;

        if (0 == r_showTangentSpace.GetInteger()) {
            return;
        }
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_STENCIL_TEST);

        GL_State(GLS_SRCBLEND_SRC_ALPHA | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA);

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

            RB_SimpleSurfaceSetup(drawSurf);

            tri = drawSurf.geo;
            if (null == tri.verts) {
                continue;
            }
            qglBegin(GL_TRIANGLES);
            for (j = 0; j < tri.getIndexes().getNumValues(); j++) {
                final idDrawVert v;

                v = tri.verts[tri.getIndexes().getValues().get(j)];

                if (r_showTangentSpace.GetInteger() == 1) {
                    qglColor4f(0.5f + (0.5f * v.tangents[0].oGet(0)),
                            0.5f + (0.5f * v.tangents[0].oGet(1)),
                            0.5f + (0.5f * v.tangents[0].oGet(2)),
                            0.5f);
                } else if (r_showTangentSpace.GetInteger() == 2) {
                    qglColor4f(0.5f + (0.5f * v.tangents[1].oGet(0)),
                            0.5f + (0.5f * v.tangents[1].oGet(1)),
                            0.5f + (0.5f * v.tangents[1].oGet(2)),
                            0.5f);
                } else {
                    qglColor4f(0.5f + (0.5f * v.normal.oGet(0)),
                            0.5f + (0.5f * v.normal.oGet(1)),
                            0.5f + (0.5f * v.normal.oGet(2)),
                            0.5f);
                }
                qglVertex3fv(v.xyz.toFloatBuffer());
            }
            qglEnd();
        }

        GL_State(GLS_DEFAULT);
    }

    /*
     =====================
     RB_ShowVertexColor

     Draw each triangle with the solid vertex colors
     =====================
     */
    public static void RB_ShowVertexColor(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j;
        drawSurf_s drawSurf;
        srfTriangles_s tri;

        if (!r_showVertexColor.GetBool()) {
            return;
        }
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_STENCIL_TEST);

        GL_State(GLS_DEPTHFUNC_LESS);

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

            RB_SimpleSurfaceSetup(drawSurf);

            tri = drawSurf.geo;
            if (null == tri.verts) {
                continue;
            }
            qglBegin(GL_TRIANGLES);
            for (j = 0; j < tri.getIndexes().getNumValues(); j++) {
                final idDrawVert v;

                v = tri.verts[tri.getIndexes().getValues().get(j)];
                qglColor4ubv(v.getColor());
                qglVertex3fv(v.xyz.toFloatBuffer());
            }
            qglEnd();
        }

        GL_State(GLS_DEFAULT);
    }


    /*
     =====================
     RB_ShowNormals

     Debugging tool
     =====================
     */
    public static void RB_ShowNormals(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j;
        drawSurf_s drawSurf;
        final idVec3 end = new idVec3();
        srfTriangles_s tri;
        float size;
        boolean showNumbers;
        idVec3 pos;

        if (r_showNormals.GetFloat() == 0.0f) {
            return;
        }

        GL_State(GLS_POLYMODE_LINE);
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);

        globalImages.BindNull();
        qglDisable(GL_STENCIL_TEST);
        if (!r_debugLineDepthTest.GetBool()) {
            qglDisable(GL_DEPTH_TEST);
        } else {
            qglEnable(GL_DEPTH_TEST);
        }

        size = r_showNormals.GetFloat();
        if (size < 0.0f) {
            size = -size;
            showNumbers = true;
        } else {
            showNumbers = false;
        }

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

            RB_SimpleSurfaceSetup(drawSurf);

            tri = drawSurf.geo;
            if (null == tri.verts) {
                continue;
            }

            qglBegin(GL_LINES);
            for (j = 0; j < tri.numVerts; j++) {
                qglColor3f(0, 0, 1);
                qglVertex3fv(tri.verts[j].xyz.toFloatBuffer());
                VectorMA(tri.verts[j].xyz, size, tri.verts[j].normal, end);
                qglVertex3fv(end.toFloatBuffer());

                qglColor3f(1, 0, 0);
                qglVertex3fv(tri.verts[j].xyz.toFloatBuffer());
                VectorMA(tri.verts[j].xyz, size, tri.verts[j].tangents[0], end);
                qglVertex3fv(end.toFloatBuffer());

                qglColor3f(0, 1, 0);
                qglVertex3fv(tri.verts[j].xyz.toFloatBuffer());
                VectorMA(tri.verts[j].xyz, size, tri.verts[j].tangents[1], end);
                qglVertex3fv(end.toFloatBuffer());
            }
            qglEnd();
        }

        if (showNumbers) {
            RB_SimpleWorldSetup();
            for (i = 0; i < numDrawSurfs; i++) {
                drawSurf = drawSurfs[i];
                tri = drawSurf.geo;
                if (null == tri.verts) {
                    continue;
                }
                for (j = 0; j < tri.numVerts; j++) {
                    pos = R_LocalPointToGlobal(drawSurf.space.modelMatrix, tri.verts[j].xyz.oPlus(tri.verts[j].tangents[0].oPlus(tri.verts[j].normal.oMultiply(0.2f))));
                    RB_DrawText(va("%d", j), pos, 0.01f, colorWhite, backEnd.viewDef.renderView.viewaxis, 1);
                }

                for (j = 0; j < tri.getIndexes().getNumValues(); j += 3) {
                    pos = R_LocalPointToGlobal(drawSurf.space.modelMatrix,
                            (tri.verts[ tri.getIndexes().getValues().get( j + 0)].xyz.oPlus(tri.verts[ tri.getIndexes().getValues().get( j + 1)].xyz.oPlus(tri.verts[ tri.getIndexes().getValues().get( j + 2)].xyz))).oMultiply(1.0f / 3.0f).oPlus(tri.verts[ tri.getIndexes().getValues().get( j + 0)].normal.oMultiply(0.2f)));
                    RB_DrawText(va("%d", j / 3), pos, 0.01f, colorCyan, backEnd.viewDef.renderView.viewaxis, 1);
                }
            }
        }

        qglEnable(GL_STENCIL_TEST);
    }


    /*
     =====================
     RB_ShowNormals

     Debugging tool
     =====================
     */
    public static void RB_AltShowNormals(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j, k;
        drawSurf_s drawSurf;
        final idVec3 end = new idVec3();
        srfTriangles_s tri;

        if (r_showNormals.GetFloat() == 0.0f) {
            return;
        }

        GL_State(GLS_DEFAULT);
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);

        globalImages.BindNull();
        qglDisable(GL_STENCIL_TEST);
        qglDisable(GL_DEPTH_TEST);

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

            RB_SimpleSurfaceSetup(drawSurf);

            tri = drawSurf.geo;
            qglBegin(GL_LINES);
            for (j = 0; j < tri.getIndexes().getNumValues(); j += 3) {
                final idDrawVert[] v = new idDrawVert[3];
                idVec3 mid;

                v[0] = tri.verts[tri.getIndexes().getValues().get(j + 0)];
                v[1] = tri.verts[tri.getIndexes().getValues().get(j + 1)];
                v[2] = tri.verts[tri.getIndexes().getValues().get(j + 2)];

                // make the midpoint slightly above the triangle
                mid = (v[0].xyz.oPlus(v[1].xyz).oPlus(v[2].xyz)).oMultiply(1.0f / 3.0f);
                mid.oPluSet(tri.facePlanes[ j / 3].Normal().oMultiply(0.1f));

                for (k = 0; k < 3; k++) {
                    idVec3 pos;

                    pos = (mid.oPlus(v[k].xyz.oMultiply(3f))).oMultiply(0.25f);

                    qglColor3f(0, 0, 1);
                    qglVertex3fv(pos.toFloatBuffer());
                    VectorMA(pos, r_showNormals.GetFloat(), v[k].normal, end);
                    qglVertex3fv(end.toFloatBuffer());

                    qglColor3f(1, 0, 0);
                    qglVertex3fv(pos.toFloatBuffer());
                    VectorMA(pos, r_showNormals.GetFloat(), v[k].tangents[0], end);
                    qglVertex3fv(end.toFloatBuffer());

                    qglColor3f(0, 1, 0);
                    qglVertex3fv(pos.toFloatBuffer());
                    VectorMA(pos, r_showNormals.GetFloat(), v[k].tangents[1], end);
                    qglVertex3fv(end.toFloatBuffer());

                    qglColor3f(1, 1, 1);
                    qglVertex3fv(pos.toFloatBuffer());
                    qglVertex3fv(v[k].xyz.toFloatBuffer());
                }
            }
            qglEnd();
        }

        qglEnable(GL_DEPTH_TEST);
        qglEnable(GL_STENCIL_TEST);
    }

    /*
     =====================
     RB_ShowTextureVectors

     Draw texture vectors in the center of each triangle
     =====================
     */
    public static void RB_ShowTextureVectors(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j;
        drawSurf_s drawSurf;
        srfTriangles_s tri;

        if (r_showTextureVectors.GetFloat() == 0.0f) {
            return;
        }

        GL_State(GLS_DEPTHFUNC_LESS);
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);

        globalImages.BindNull();

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

//            if (i != 101) continue;

            tri = drawSurf.geo;

            if (null == tri.verts) {
                continue;
            }
            if (null == tri.facePlanes) {
                continue;
            }
            RB_SimpleSurfaceSetup(drawSurf);

            // draw non-shared edges in yellow
            qglBegin(GL_LINES);

            for (j = 0; j < tri.getIndexes().getNumValues(); j += 3) {
                final idDrawVert a, b, c;
                float area, inva;
                final idVec3 temp = new idVec3();
                final float[] d0 = new float[5], d1 = new float[5];
                idVec3 mid;
                final idVec3[] tangents = {new idVec3(), new idVec3()};

                a = tri.verts[tri.getIndexes().getValues().get(j + 0)];
                b = tri.verts[tri.getIndexes().getValues().get(j + 1)];
                c = tri.verts[tri.getIndexes().getValues().get(j + 2)];

                // make the midpoint slightly above the triangle
                mid = (a.xyz.oPlus(b.xyz).oPlus(c.xyz)).oMultiply(1.0f / 3.0f);
                mid.oPluSet(tri.facePlanes[ j / 3].Normal().oMultiply(0.1f));

                // calculate the texture vectors
                VectorSubtract(b.xyz, a.xyz, d0);
                d0[3] = b.st.oGet(0) - a.st.oGet(0);
                d0[4] = b.st.oGet(1) - a.st.oGet(1);
                VectorSubtract(c.xyz, a.xyz, d1);
                d1[3] = c.st.oGet(0) - a.st.oGet(0);
                d1[4] = c.st.oGet(1) - a.st.oGet(1);

                area = (d0[3] * d1[4]) - (d0[4] * d1[3]);
                if (area == 0) {
                    continue;
                }
                inva = 1.0f / area;

                temp.oSet(0, ((d0[0] * d1[4]) - (d0[4] * d1[0])) * inva);
                temp.oSet(1, ((d0[1] * d1[4]) - (d0[4] * d1[1])) * inva);
                temp.oSet(2, ((d0[2] * d1[4]) - (d0[4] * d1[2])) * inva);
                temp.Normalize();
                tangents[0].oSet(temp);

                temp.oSet(0, ((d0[3] * d1[0]) - (d0[0] * d1[3])) * inva);
                temp.oSet(1, ((d0[3] * d1[1]) - (d0[1] * d1[3])) * inva);
                temp.oSet(2, ((d0[3] * d1[2]) - (d0[2] * d1[3])) * inva);
                temp.Normalize();
                tangents[1].oSet(temp);

                // draw the tangents
                tangents[0] = mid.oPlus(tangents[0].oMultiply(r_showTextureVectors.GetFloat()));
                tangents[1] = mid.oPlus(tangents[1].oMultiply(r_showTextureVectors.GetFloat()));

                qglColor3f(1, 0, 0);
                qglVertex3fv(mid.toFloatBuffer());
                qglVertex3fv(tangents[0].toFloatBuffer());

                qglColor3f(0, 1, 0);
                qglVertex3fv(mid.toFloatBuffer());
                qglVertex3fv(tangents[1].toFloatBuffer());
            }

            qglEnd();
        }
    }

    /*
     =====================
     RB_ShowDominantTris

     Draw lines from each vertex to the dominant triangle center
     =====================
     */
    public static void RB_ShowDominantTris(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j;
        drawSurf_s drawSurf;
        srfTriangles_s tri;

        if (!r_showDominantTri.GetBool()) {
            return;
        }

        GL_State(GLS_DEPTHFUNC_LESS);
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);

        qglPolygonOffset(-1, -2);
        qglEnable(GL_POLYGON_OFFSET_LINE);

        globalImages.BindNull();

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

            tri = drawSurf.geo;

            if (null == tri.verts) {
                continue;
            }
            if (null == tri.dominantTris) {
                continue;
            }
            RB_SimpleSurfaceSetup(drawSurf);

            qglColor3f(1, 1, 0);
            qglBegin(GL_LINES);

            for (j = 0; j < tri.numVerts; j++) {
                final idDrawVert a, b, c;
                idVec3 mid;

                // find the midpoint of the dominant tri
                a = tri.verts[j];
                b = tri.verts[tri.dominantTris[j].v2];
                c = tri.verts[tri.dominantTris[j].v3];

                mid = (a.xyz.oPlus(b.xyz.oPlus(c.xyz))).oMultiply(1.0f / 3.0f);

                qglVertex3fv(mid.toFloatBuffer());
                qglVertex3fv(a.xyz.toFloatBuffer());
            }

            qglEnd();
        }
        qglDisable(GL_POLYGON_OFFSET_LINE);
    }

    /*
     =====================
     RB_ShowEdges

     Debugging tool
     =====================
     */
    public static void RB_ShowEdges(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        int i, j, k, m, n, o;
        drawSurf_s drawSurf;
        srfTriangles_s tri;
        silEdge_t edge;
        int danglePlane;

        if (!r_showEdges.GetBool()) {
            return;
        }

        GL_State(GLS_DEFAULT);
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);

        globalImages.BindNull();
        qglDisable(GL_DEPTH_TEST);

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

            tri = drawSurf.geo;

            final idDrawVert[] ac = tri.verts;//TODO:which element is the pointer pointing to?
            if (null == ac) {
                continue;
            }

            RB_SimpleSurfaceSetup(drawSurf);

            // draw non-shared edges in yellow
            qglColor3f(1, 1, 0);
            qglBegin(GL_LINES);

            for (j = 0; j < tri.getIndexes().getNumValues(); j += 3) {
                for (k = 0; k < 3; k++) {
                    int l, i1, i2;
                    l = (k == 2) ? 0 : k + 1;
                    i1 = tri.getIndexes().getValues().get(j + k);
                    i2 = tri.getIndexes().getValues().get(j + l);

                    // if these are used backwards, the edge is shared
                    for (m = 0; m < tri.getIndexes().getNumValues(); m += 3) {
                        for (n = 0; n < 3; n++) {
                            o = (n == 2) ? 0 : n + 1;
                            if ((tri.getIndexes().getValues().get(m + n) == i2) && (tri.getIndexes().getValues().get(m + o) == i1)) {
                                break;
                            }
                        }
                        if (n != 3) {
                            break;
                        }
                    }

                    // if we didn't find a backwards listing, draw it in yellow
                    if (m == tri.getIndexes().getNumValues()) {
                        qglVertex3fv(ac[ i1].xyz.toFloatBuffer());
                        qglVertex3fv(ac[ i2].xyz.toFloatBuffer());
                    }

                }
            }

            qglEnd();

            // draw dangling sil edges in red
            if (null == tri.silEdges) {
                continue;
            }

            // the plane number after all real planes
            // is the dangling edge
            danglePlane = tri.getIndexes().getNumValues() / 3;

            qglColor3f(1, 0, 0);

            qglBegin(GL_LINES);
            for (j = 0; j < tri.numSilEdges; j++) {
                edge = tri.silEdges[j];

                if ((edge.p1 != danglePlane) && (edge.p2 != danglePlane)) {
                    continue;
                }

                qglVertex3fv(ac[ edge.v1].xyz.toFloatBuffer());
                qglVertex3fv(ac[ edge.v2].xyz.toFloatBuffer());
            }
            qglEnd();
        }

        qglEnable(GL_DEPTH_TEST);
    }

    /*
     ==============
     RB_ShowLights

     Visualize all light volumes used in the current scene
     r_showLights 1	: just print volumes numbers, highlighting ones covering the view
     r_showLights 2	: also draw planes of each volume
     r_showLights 3	: also draw edges of each volume
     ==============
     */
    public static void RB_ShowLights() {
        idRenderLightLocal light;
        int count;
        srfTriangles_s tri;
        viewLight_s vLight;

        if (0 == r_showLights.GetInteger()) {
            return;
        }

        // all volumes are expressed in world coordinates
        RB_SimpleWorldSetup();

        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
        globalImages.BindNull();
        qglDisable(GL_STENCIL_TEST);

        GL_Cull(CT_TWO_SIDED);
        qglDisable(GL_DEPTH_TEST);

        common.Printf("volumes: ");	// FIXME: not in back end!

        count = 0;
        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            light = vLight.lightDef;
            count++;

            tri = light.frustumTris;

            // depth buffered planes
            if (r_showLights.GetInteger() >= 2) {
                GL_State(GLS_SRCBLEND_SRC_ALPHA | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA | GLS_DEPTHMASK);
                qglColor4f(0, 0, 1, 0.25f);
                qglEnable(GL_DEPTH_TEST);
                RB_RenderTriangleSurface(tri);
            }

            // non-hidden lines
            if (r_showLights.GetInteger() >= 3) {
                GL_State(GLS_POLYMODE_LINE | GLS_DEPTHMASK);
                qglDisable(GL_DEPTH_TEST);
                qglColor3f(1, 1, 1);
                RB_RenderTriangleSurface(tri);
            }

            int index;

            index = backEnd.viewDef.renderWorld.lightDefs.FindIndex(vLight.lightDef);
            if (vLight.viewInsideLight) {
                // view is in this volume
                common.Printf("[%d] ", index);
            } else {
                common.Printf("%d ", index);
            }
        }

        qglEnable(GL_DEPTH_TEST);
        qglDisable(GL_POLYGON_OFFSET_LINE);

        qglDepthRange(0, 1);
        GL_State(GLS_DEFAULT);
        GL_Cull(CT_FRONT_SIDED);

        common.Printf(" = %d total\n", count);
    }

    /*
     =====================
     RB_ShowPortals

     Debugging tool, won't work correctly with SMP or when mirrors are present
     =====================
     */
    public static void RB_ShowPortals() {
        if (!r_showPortals.GetBool()) {
            return;
        }

        // all portals are expressed in world coordinates
        RB_SimpleWorldSetup();

        globalImages.BindNull();
        qglDisable(GL_DEPTH_TEST);

        GL_State(GLS_DEFAULT);

        backEnd.viewDef.renderWorld.ShowPortals();

        qglEnable(GL_DEPTH_TEST);
    }

    /*
     ================
     RB_ClearDebugText
     ================
     */
    public static void RB_ClearDebugText(int time) {
        int i;
        int num;
        debugText_s text;

        rb_debugTextTime = time;

        if (0 == time) {
            // free up our strings
            rb_debugText = TempDump.allocArray(debugText_s.class, rb_debugText.length);
            rb_numDebugText = 0;
            return;
        }

        // copy any text that still needs to be drawn
        for (i = num = 0; i < rb_numDebugText; i++) {
            text = rb_debugText[i];
            if (text.lifeTime > time) {
                if (num != i) {
                    rb_debugText[num] = text;
                }
                num++;
            }
        }
        rb_numDebugText = num;
    }

    /*
     ================
     RB_AddDebugText
     ================
     */
    public static void RB_AddDebugText(final String text, final idVec3 origin, float scale, final idVec4 color, final idMat3 viewAxis, final int align, final int lifetime, final boolean depthTest) {
        debugText_s debugText;

        if (rb_numDebugText < MAX_DEBUG_TEXT) {
            debugText = rb_debugText[rb_numDebugText++];
            debugText.text.oSet(text);//			= text;
            debugText.origin = origin;
            debugText.scale = scale;
            debugText.color = color;
            debugText.viewAxis = viewAxis;
            debugText.align = align;
            debugText.lifeTime = rb_debugTextTime + lifetime;
            debugText.depthTest = depthTest;
        }
    }

    /*
     ================
     RB_DrawTextLength

     returns the length of the given text
     ================
     */
    public static float RB_DrawTextLength(final String text, float scale, int len) {
        int i, num, index, charIndex;
        float spacing, textLen = 0.0f;

        if ((text != null) && !text.isEmpty()) {
            if (0 == len) {
                len = text.length();
            }
            for (i = 0; i < len; i++) {
                charIndex = text.charAt(i) - 32;
                if ((charIndex < 0) || (charIndex > NUM_SIMPLEX_CHARS)) {
                    continue;
                }
                num = simplex[charIndex][0] * 2;
                spacing = simplex[charIndex][1];
                index = 2;

                while ((index - 2) < num) {
                    if (simplex[charIndex][index] < 0) {
                        index++;
                        continue;
                    }
                    index += 2;
                    if (simplex[charIndex][index] < 0) {
                        index++;
                        continue;
                    }
                }
                textLen += spacing * scale;
            }
        }
        return textLen;
    }

    /*
     ================
     RB_DrawText

     oriented on the viewaxis
     align can be 0-left, 1-center (default), 2-right
     ================
     */
    public static void RB_DrawText(final String text, final idVec3 origin, float scale, final idVec4 color, final idMat3 viewAxis, final int align) {
        int i, j, len, num, index, charIndex, line;
        float textLen = 0, spacing;
        idVec3 org = new idVec3(), p1, p2;

        if ((text != null) && !text.isEmpty()) {
            qglBegin(GL_LINES);
            qglColor3fv(color.toFloatBuffer());

            if (text.charAt(0) == '\n') {
                line = 1;
            } else {
                line = 0;
            }

            len = text.length();
            for (i = 0; i < len; i++) {

                if ((i == 0) || (text.charAt(i) == '\n')) {
                    org = origin.oMinus(viewAxis.oGet(2)).oMultiply(line * 36.0f * scale);
                    if (align != 0) {
                        for (j = 1; (i + j) <= len; j++) {
                            if (((i + j) == len) || (text.charAt(i + j) == '\n')) {
                                textLen = RB_DrawTextLength(text.substring(i), scale, j);
                                break;
                            }
                        }
                        if (align == 2) {
                            // right
                            org.oPluSet(viewAxis.oGet(1).oMultiply(textLen));
                        } else {
                            // center
                            org.oPluSet(viewAxis.oGet(1).oMultiply(textLen * 0.5f));
                        }
                    }
                    line++;
                }

                charIndex = text.charAt(i) - 32;
                if ((charIndex < 0) || (charIndex > NUM_SIMPLEX_CHARS)) {
                    continue;
                }
                num = simplex[charIndex][0] * 2;
                spacing = simplex[charIndex][1];
                index = 2;

                while ((index - 2) < num) {
                    if (simplex[charIndex][index] < 0) {
                        index++;
                        continue;
                    }
                    p1 = org.oPlus(viewAxis.oGet(1).oNegative().oMultiply(scale * simplex[charIndex][index])).oPlus(viewAxis.oGet(2).oMultiply(scale * simplex[charIndex][index + 1]));
                    index += 2;
                    if (simplex[charIndex][index] < 0) {
                        index++;
                        continue;
                    }
//				p2 = org + scale * simplex[charIndex][index] * -viewAxis[1] + scale * simplex[charIndex][index+1] * viewAxis[2];
                    p2 = org.oPlus(viewAxis.oGet(1).oNegative().oMultiply(scale * simplex[charIndex][index])).oPlus(viewAxis.oGet(2).oMultiply(scale * simplex[charIndex][index + 1]));

                    qglVertex3fv(p1.toFloatBuffer());
                    qglVertex3fv(p2.toFloatBuffer());
                }
                org.oMinSet(viewAxis.oGet(1).oMultiply(spacing * scale));
            }

            qglEnd();
        }
    }

    /*
     ================
     RB_ShowDebugText
     ================
     */
    public static void RB_ShowDebugText() {
        int i;
        int width;
        debugText_s text;
        int text_index;

        if (0 == rb_numDebugText) {
            return;
        }

        // all lines are expressed in world coordinates
        RB_SimpleWorldSetup();

        globalImages.BindNull();

        width = r_debugLineWidth.GetInteger();
        if (width < 1) {
            width = 1;
        } else if (width > 10) {
            width = 10;
        }

        // draw lines
        GL_State(GLS_POLYMODE_LINE);
        qglLineWidth(width);

        if (!r_debugLineDepthTest.GetBool()) {
            qglDisable(GL_DEPTH_TEST);
        }

        text = rb_debugText[text_index = 0];
        for (i = 0; i < rb_numDebugText; i++, text = rb_debugText[++text_index]) {
            if (!text.depthTest) {
                RB_DrawText(text.text.getData(), text.origin, text.scale, text.color, text.viewAxis, text.align);
            }
        }

        if (!r_debugLineDepthTest.GetBool()) {
            qglEnable(GL_DEPTH_TEST);
        }

        text = rb_debugText[text_index = 0];
        for (i = 0; i < rb_numDebugText; i++, text = rb_debugText[++text_index]) {
            if (text.depthTest) {
                RB_DrawText(text.text.getData(), text.origin, text.scale, text.color, text.viewAxis, text.align);
            }
        }

        qglLineWidth(1);
        GL_State(GLS_DEFAULT);
    }

    /*
     ================
     RB_ClearDebugLines
     ================
     */
    public static void RB_ClearDebugLines(int time) {
        int i;
        int num;
        debugLine_s line;
        int line_index;

        rb_debugLineTime = time;

        if (0 == time) {
            rb_numDebugLines = 0;
            return;
        }

        // copy any lines that still need to be drawn
        num = 0;
        line = rb_debugLines[line_index = 0];
        for (i = 0; i < rb_numDebugLines; i++, line = rb_debugLines[++line_index]) {
            if (line.lifeTime > time) {
                if (num != i) {
                    rb_debugLines[num] = line;
                }
                num++;
            }
        }
        rb_numDebugLines = num;
    }

    /*
     ================
     RB_AddDebugLine
     ================
     */
    public static void RB_AddDebugLine(final idVec4 color, final idVec3 start, final idVec3 end, final int lifeTime, final boolean depthTest) {
        debugLine_s line;

        if (rb_numDebugLines < MAX_DEBUG_LINES) {
            line = rb_debugLines[rb_numDebugLines++] = new debugLine_s();
            line.rgb = new idVec4(color);
            line.start = new idVec3(start);
            line.end = new idVec3(end);
            line.depthTest = depthTest;
            line.lifeTime = rb_debugLineTime + lifeTime;
        }
    }

    /*
     ================
     RB_ShowDebugLines
     ================
     */
    public static void RB_ShowDebugLines() {
        int i;
        int width;
        debugLine_s line;
        int line_index;

        if (0 == rb_numDebugLines) {
            return;
        }

        // all lines are expressed in world coordinates
        RB_SimpleWorldSetup();

        globalImages.BindNull();

        width = r_debugLineWidth.GetInteger();
        if (width < 1) {
            width = 1;
        } else if (width > 10) {
            width = 10;
        }

        // draw lines
        GL_State(GLS_POLYMODE_LINE);//| GLS_DEPTHMASK ); //| GLS_SRCBLEND_ONE | GLS_DSTBLEND_ONE );
        qglLineWidth(width);

        if (!r_debugLineDepthTest.GetBool()) {
            qglDisable(GL_DEPTH_TEST);
        }

        qglBegin(GL_LINES);

        line = rb_debugLines[line_index = 0];
        for (i = 0; i < rb_numDebugLines; i++, line = rb_debugLines[++line_index]) {
            if (!line.depthTest) {
                qglColor3fv(line.rgb.toFloatBuffer());
                qglVertex3fv(line.start.toFloatBuffer());
                qglVertex3fv(line.end.toFloatBuffer());
            }
        }
        qglEnd();

        if (!r_debugLineDepthTest.GetBool()) {
            qglEnable(GL_DEPTH_TEST);
        }

        qglBegin(GL_LINES);

        line = rb_debugLines[line_index = 0];
        for (i = 0; i < rb_numDebugLines; i++, line = rb_debugLines[++line_index]) {
            if (line.depthTest) {
                qglColor4fv(line.rgb.toFloatBuffer());
                qglVertex3fv(line.start.toFloatBuffer());
                qglVertex3fv(line.end.toFloatBuffer());
            }
        }

        qglEnd();

        qglLineWidth(1);
        GL_State(GLS_DEFAULT);
    }

    /*
     ================
     RB_ClearDebugPolygons
     ================
     */
    static void RB_ClearDebugPolygons(int time) {
        int i;
        int num;
        debugPolygon_s poly;
        int poly_index;

        rb_debugPolygonTime = time;

        if (0 == time) {
            rb_numDebugPolygons = 0;
            return;
        }

        // copy any polygons that still need to be drawn
        num = 0;

        poly = rb_debugPolygons[poly_index = 0];
        for (i = 0; i < rb_numDebugPolygons; i++, poly = rb_debugPolygons[++poly_index]) {
            if (poly.lifeTime > time) {
                if (num != i) {
                    rb_debugPolygons[num] = poly;
                }
                num++;
            }
        }
        rb_numDebugPolygons = num;
    }

    /*
     ================
     RB_AddDebugPolygon
     ================
     */
    public static void RB_AddDebugPolygon(final idVec4 color, final idWinding winding, final int lifeTime, final boolean depthTest) {
        debugPolygon_s poly;

        if (rb_numDebugPolygons < MAX_DEBUG_POLYGONS) {
            poly = rb_debugPolygons[ rb_numDebugPolygons++];
            poly.rgb = color;
            poly.winding = winding;
            poly.depthTest = depthTest;
            poly.lifeTime = rb_debugPolygonTime + lifeTime;
        }
    }

    /*
     ================
     RB_ShowDebugPolygons
     ================
     */
    public static void RB_ShowDebugPolygons() {
        int i, j;
        debugPolygon_s poly;
        int poly_index;

        if (0 == rb_numDebugPolygons) {
            return;
        }

        // all lines are expressed in world coordinates
        RB_SimpleWorldSetup();

        globalImages.BindNull();

        qglDisable(GL_TEXTURE_2D);
        qglDisable(GL_STENCIL_TEST);

        qglEnable(GL_DEPTH_TEST);

        if (r_debugPolygonFilled.GetBool()) {
            GL_State(GLS_SRCBLEND_SRC_ALPHA | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA | GLS_DEPTHMASK);
            qglPolygonOffset(-1, -2);
            qglEnable(GL_POLYGON_OFFSET_FILL);
        } else {
            GL_State(GLS_POLYMODE_LINE);
            qglPolygonOffset(-1, -2);
            qglEnable(GL_POLYGON_OFFSET_LINE);
        }

        poly = rb_debugPolygons[poly_index = 0];
        for (i = 0; i < rb_numDebugPolygons; i++, poly = rb_debugPolygons[++poly_index]) {
//		if ( !poly.depthTest ) {

            qglColor4fv(poly.rgb.toFloatBuffer());

            qglBegin(GL_POLYGON);

            for (j = 0; j < poly.winding.GetNumPoints(); j++) {
                qglVertex3fv(poly.winding.oGet(j).toFloatBuffer());
            }

            qglEnd();
//		}
        }

        GL_State(GLS_DEFAULT);

        if (r_debugPolygonFilled.GetBool()) {
            qglDisable(GL_POLYGON_OFFSET_FILL);
        } else {
            qglDisable(GL_POLYGON_OFFSET_LINE);
        }

        qglDepthRange(0, 1);
        GL_State(GLS_DEFAULT);
    }

    /*
     ================
     RB_TestGamma
     ================
     */
    static final int G_WIDTH = 512;
    static final int G_HEIGHT = 512;
    static final int BAR_HEIGHT = 64;

    public static void RB_TestGamma() {
        final byte[][][] image = new byte[G_HEIGHT][G_WIDTH][4];
        int i, j;
        int c, comp;
        int v, dither;
        int mask, y;

        if (r_testGamma.GetInteger() <= 0) {
            return;
        }

        v = r_testGamma.GetInteger();
        if ((v <= 1) || (v >= 196)) {
            v = 128;
        }

//	memset( image, 0, sizeof( image ) );
        for (mask = 0; mask < 8; mask++) {
            y = mask * BAR_HEIGHT;
            for (c = 0; c < 4; c++) {
                v = (c * 64) + 32;
                // solid color
                for (i = 0; i < (BAR_HEIGHT / 2); i++) {
                    for (j = 0; j < (G_WIDTH / 4); j++) {
                        for (comp = 0; comp < 3; comp++) {
                            if ((mask & (1 << comp)) != 0) {
                                image[y + i][((c * G_WIDTH) / 4) + j][comp] = (byte) v;
                            }
                        }
                    }
                    // dithered color
                    for (j = 0; j < (G_WIDTH / 4); j++) {
                        if (((i ^ j) & 1) != 0) {
                            dither = c * 64;
                        } else {
                            dither = (c * 64) + 63;
                        }
                        for (comp = 0; comp < 3; comp++) {
                            if ((mask & (1 << comp)) != 0) {
                                image[y + (BAR_HEIGHT / 2) + i][((c * G_WIDTH) / 4) + j][comp] = (byte) dither;
                            }
                        }
                    }
                }
            }
        }

        // draw geometrically increasing steps in the bottom row
        y = 0 * BAR_HEIGHT;
        float scale = 1;
        for (c = 0; c < 4; c++) {
            v = (int) (64 * scale);
            if (v < 0) {
                v = 0;
            } else if (v > 255) {
                v = 255;
            }
            scale = scale * 1.5f;
            for (i = 0; i < BAR_HEIGHT; i++) {
                for (j = 0; j < (G_WIDTH / 4); j++) {
                    image[y + i][((c * G_WIDTH) / 4) + j][0] = (byte) v;
                    image[y + i][((c * G_WIDTH) / 4) + j][1] = (byte) v;
                    image[y + i][((c * G_WIDTH) / 4) + j][2] = (byte) v;
                }
            }
        }

        qglLoadIdentity();

        qglMatrixMode(GL_PROJECTION);
        GL_State(GLS_DEPTHFUNC_ALWAYS);
        qglColor3f(1, 1, 1);
        qglPushMatrix();
        qglLoadIdentity();
        qglDisable(GL_TEXTURE_2D);
        qglOrtho(0, 1, 0, 1, -1, 1);
        qglRasterPos2f(0.01f, 0.01f);
        qglDrawPixels(G_WIDTH, G_HEIGHT, GL_RGBA, GL_UNSIGNED_BYTE, image);
        qglPopMatrix();
        qglEnable(GL_TEXTURE_2D);
        qglMatrixMode(GL_MODELVIEW);
    }


    /*
     ==================
     RB_TestGammaBias
     ==================
     */
    public static void RB_TestGammaBias() {
        final byte[][][] image = new byte[G_HEIGHT][G_WIDTH][4];

        if (r_testGammaBias.GetInteger() <= 0) {
            return;
        }

        int y = 0;
        for (int bias = -40; bias < 40; bias += 10, y += BAR_HEIGHT) {
            float scale = 1;
            for (int c = 0; c < 4; c++) {
                int v = (int) ((64 * scale) + bias);
                scale = scale * 1.5f;
                if (v < 0) {
                    v = 0;
                } else if (v > 255) {
                    v = 255;
                }
                for (int i = 0; i < BAR_HEIGHT; i++) {
                    for (int j = 0; j < (G_WIDTH / 4); j++) {
                        image[y + i][((c * G_WIDTH) / 4) + j][0] = (byte) v;
                        image[y + i][((c * G_WIDTH) / 4) + j][1] = (byte) v;
                        image[y + i][((c * G_WIDTH) / 4) + j][2] = (byte) v;
                    }
                }
            }
        }

        qglLoadIdentity();
        qglMatrixMode(GL_PROJECTION);
        GL_State(GLS_DEPTHFUNC_ALWAYS);
        qglColor3f(1, 1, 1);
        qglPushMatrix();
        qglLoadIdentity();
        qglDisable(GL_TEXTURE_2D);
        qglOrtho(0, 1, 0, 1, -1, 1);
        qglRasterPos2f(0.01f, 0.01f);
        qglDrawPixels(G_WIDTH, G_HEIGHT, GL_RGBA, GL_UNSIGNED_BYTE, image);
        qglPopMatrix();
        qglEnable(GL_TEXTURE_2D);
        qglMatrixMode(GL_MODELVIEW);
    }

    /*
     ================
     RB_TestImage

     Display a single image over most of the screen
     ================
     */
    public static void RB_TestImage() {
        idImage image;
        int max;
        float w, h;

        image = tr.testImage;
        if (null == image) {
            return;
        }

        if (tr.testVideo != null) {
            cinData_t cin;

            cin = tr.testVideo.ImageForTime((int) (1000 * (backEnd.viewDef.floatTime - tr.testVideoStartTime)));
            if (cin.image != null) {
                image.UploadScratch(cin.image, cin.imageWidth, cin.imageHeight);
            } else {
                tr.testImage = null;
                return;
            }
            w = 0.25f;
            h = 0.25f;
        } else {
            max = image.uploadWidth > image.uploadHeight ? image.uploadWidth : image.uploadHeight;

            w = (0.25f * image.uploadWidth) / max;
            h = (0.25f * image.uploadHeight) / max;

            w *= (float) glConfig.vidHeight / glConfig.vidWidth;
        }

        qglLoadIdentity();

        qglMatrixMode(GL_PROJECTION);
        GL_State(GLS_DEPTHFUNC_ALWAYS | GLS_SRCBLEND_SRC_ALPHA | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA);
        qglColor3f(1, 1, 1);
        qglPushMatrix();
        qglLoadIdentity();
        qglOrtho(0, 1, 0, 1, -1, 1);

        tr.testImage.Bind();
        qglBegin(GL_QUADS);

        qglTexCoord2f(0, 1);
        qglVertex2f(0.5f - w, 0);

        qglTexCoord2f(0, 0);
        qglVertex2f(0.5f - w, h * 2);

        qglTexCoord2f(1, 0);
        qglVertex2f(0.5f + w, h * 2);

        qglTexCoord2f(1, 1);
        qglVertex2f(0.5f + w, 0);

        qglEnd();

        qglPopMatrix();
        qglMatrixMode(GL_MODELVIEW);
    }

    /*
     =================
     RB_RenderDebugTools
     =================
     */
    public static void RB_RenderDebugTools(drawSurf_s[] drawSurfs, int numDrawSurfs) {
        // don't do anything if this was a 2D rendering
        if (null == backEnd.viewDef.viewEntitys) {
            return;
        }

        RB_LogComment("---------- RB_RenderDebugTools ----------\n");

        GL_State(GLS_DEFAULT);
        backEnd.currentScissor = backEnd.viewDef.scissor;
        qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
                backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
                (backEnd.currentScissor.x2 + 1) - backEnd.currentScissor.x1,
                (backEnd.currentScissor.y2 + 1) - backEnd.currentScissor.y1);

        RB_ShowLightCount();
        RB_ShowShadowCount();
        RB_ShowTexturePolarity(drawSurfs, numDrawSurfs);
        RB_ShowTangentSpace(drawSurfs, numDrawSurfs);
        RB_ShowVertexColor(drawSurfs, numDrawSurfs);
        RB_ShowTris(drawSurfs, numDrawSurfs);
        RB_ShowUnsmoothedTangents(drawSurfs, numDrawSurfs);
        RB_ShowSurfaceInfo(drawSurfs, numDrawSurfs);
        RB_ShowEdges(drawSurfs, numDrawSurfs);
        RB_ShowNormals(drawSurfs, numDrawSurfs);
        RB_ShowViewEntitys(backEnd.viewDef.viewEntitys);
        RB_ShowLights();
        RB_ShowTextureVectors(drawSurfs, numDrawSurfs);
        RB_ShowDominantTris(drawSurfs, numDrawSurfs);
        if (r_testGamma.GetInteger() > 0) {	// test here so stack check isn't so damn slow on debug builds
            RB_TestGamma();
        }
        if (r_testGammaBias.GetInteger() > 0) {
            RB_TestGammaBias();
        }
        RB_TestImage();
        RB_ShowPortals();
        RB_ShowSilhouette();
        RB_ShowDepthBuffer();
        RB_ShowIntensity();
        RB_ShowDebugLines();
        RB_ShowDebugText();
        RB_ShowDebugPolygons();
        RB_ShowTrace(drawSurfs, numDrawSurfs);
    }

    /*
     =================
     RB_ShutdownDebugTools
     =================
     */
    public static void RB_ShutdownDebugTools() {
        for (int i = 0; i < MAX_DEBUG_POLYGONS; i++) {
            rb_debugPolygons[i].winding.Clear();
        }
    }
}
