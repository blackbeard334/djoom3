package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Material.cullType_t.CT_BACK_SIDED;
import static neo.Renderer.Material.cullType_t.CT_TWO_SIDED;
import static neo.Renderer.RenderSystem_init.r_clear;
import static neo.Renderer.RenderSystem_init.r_debugRenderToTexture;
import static neo.Renderer.RenderSystem_init.r_finish;
import static neo.Renderer.RenderSystem_init.r_frontBuffer;
import static neo.Renderer.RenderSystem_init.r_lockSurfaces;
import static neo.Renderer.RenderSystem_init.r_showImages;
import static neo.Renderer.RenderSystem_init.r_showOverDraw;
import static neo.Renderer.RenderSystem_init.r_singleArea;
import static neo.Renderer.RenderSystem_init.r_skipCopyTexture;
import static neo.Renderer.RenderSystem_init.r_useScissor;
import static neo.Renderer.RenderSystem_init.r_useStateCaching;
import static neo.Renderer.tr_local.GLS_ALPHAMASK;
import static neo.Renderer.tr_local.GLS_ATEST_BITS;
import static neo.Renderer.tr_local.GLS_ATEST_EQ_255;
import static neo.Renderer.tr_local.GLS_ATEST_GE_128;
import static neo.Renderer.tr_local.GLS_ATEST_LT_128;
import static neo.Renderer.tr_local.GLS_BLUEMASK;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_ALWAYS;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_EQUAL;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_LESS;
import static neo.Renderer.tr_local.GLS_DEPTHMASK;
import static neo.Renderer.tr_local.GLS_DSTBLEND_BITS;
import static neo.Renderer.tr_local.GLS_DSTBLEND_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE_MINUS_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE_MINUS_SRC_COLOR;
import static neo.Renderer.tr_local.GLS_DSTBLEND_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_SRC_COLOR;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ZERO;
import static neo.Renderer.tr_local.GLS_GREENMASK;
import static neo.Renderer.tr_local.GLS_POLYMODE_LINE;
import static neo.Renderer.tr_local.GLS_REDMASK;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ALPHA_SATURATE;
import static neo.Renderer.tr_local.GLS_SRCBLEND_BITS;
import static neo.Renderer.tr_local.GLS_SRCBLEND_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_DST_COLOR;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE_MINUS_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE_MINUS_DST_COLOR;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE_MINUS_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ZERO;
import static neo.Renderer.tr_local.backEnd;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_local.renderCommand_t.RC_NOP;
import static neo.Renderer.tr_render.RB_DrawView;
import static neo.TempDump.NOT;
import static neo.framework.Common.common;
import static neo.open.gl.QGL.QGL_FALSE;
import static neo.open.gl.QGL.QGL_TRUE;
import static neo.open.gl.QGL.qglActiveTextureARB;
import static neo.open.gl.QGL.qglAlphaFunc;
import static neo.open.gl.QGL.qglBegin;
import static neo.open.gl.QGL.qglBindTexture;
import static neo.open.gl.QGL.qglBlendFunc;
import static neo.open.gl.QGL.qglClear;
import static neo.open.gl.QGL.qglClearColor;
import static neo.open.gl.QGL.qglClearDepth;
import static neo.open.gl.QGL.qglClientActiveTextureARB;
import static neo.open.gl.QGL.qglColor4f;
import static neo.open.gl.QGL.qglColorMask;
import static neo.open.gl.QGL.qglCullFace;
import static neo.open.gl.QGL.qglDepthFunc;
import static neo.open.gl.QGL.qglDepthMask;
import static neo.open.gl.QGL.qglDisable;
import static neo.open.gl.QGL.qglDisableClientState;
import static neo.open.gl.QGL.qglDrawBuffer;
import static neo.open.gl.QGL.qglEnable;
import static neo.open.gl.QGL.qglEnableClientState;
import static neo.open.gl.QGL.qglEnd;
import static neo.open.gl.QGL.qglFinish;
import static neo.open.gl.QGL.qglLoadIdentity;
import static neo.open.gl.QGL.qglMatrixMode;
import static neo.open.gl.QGL.qglOrtho;
import static neo.open.gl.QGL.qglPolygonMode;
import static neo.open.gl.QGL.qglScissor;
import static neo.open.gl.QGL.qglShadeModel;
import static neo.open.gl.QGL.qglTexCoord2f;
import static neo.open.gl.QGL.qglTexEnvi;
import static neo.open.gl.QGL.qglTexGenf;
import static neo.open.gl.QGL.qglVertex2f;
import static neo.open.gl.QGL.qglViewport;
import static neo.open.gl.QGLConstantsIfc.GL_ADD;
import static neo.open.gl.QGLConstantsIfc.GL_ALPHA_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_ALWAYS;
import static neo.open.gl.QGLConstantsIfc.GL_BACK;
import static neo.open.gl.QGLConstantsIfc.GL_BLEND;
import static neo.open.gl.QGLConstantsIfc.GL_COLOR_ARRAY;
import static neo.open.gl.QGLConstantsIfc.GL_COLOR_BUFFER_BIT;
import static neo.open.gl.QGLConstantsIfc.GL_COMBINE;
import static neo.open.gl.QGLConstantsIfc.GL_CULL_FACE;
import static neo.open.gl.QGLConstantsIfc.GL_DECAL;
import static neo.open.gl.QGLConstantsIfc.GL_DEPTH_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_DST_ALPHA;
import static neo.open.gl.QGLConstantsIfc.GL_DST_COLOR;
import static neo.open.gl.QGLConstantsIfc.GL_EQUAL;
import static neo.open.gl.QGLConstantsIfc.GL_FILL;
import static neo.open.gl.QGLConstantsIfc.GL_FRONT;
import static neo.open.gl.QGLConstantsIfc.GL_FRONT_AND_BACK;
import static neo.open.gl.QGLConstantsIfc.GL_GEQUAL;
import static neo.open.gl.QGLConstantsIfc.GL_LEQUAL;
import static neo.open.gl.QGLConstantsIfc.GL_LESS;
import static neo.open.gl.QGLConstantsIfc.GL_LIGHTING;
import static neo.open.gl.QGLConstantsIfc.GL_LINE;
import static neo.open.gl.QGLConstantsIfc.GL_LINE_STIPPLE;
import static neo.open.gl.QGLConstantsIfc.GL_MODELVIEW;
import static neo.open.gl.QGLConstantsIfc.GL_MODULATE;
import static neo.open.gl.QGLConstantsIfc.GL_OBJECT_LINEAR;
import static neo.open.gl.QGLConstantsIfc.GL_ONE;
import static neo.open.gl.QGLConstantsIfc.GL_ONE_MINUS_DST_ALPHA;
import static neo.open.gl.QGLConstantsIfc.GL_ONE_MINUS_DST_COLOR;
import static neo.open.gl.QGLConstantsIfc.GL_ONE_MINUS_SRC_ALPHA;
import static neo.open.gl.QGLConstantsIfc.GL_ONE_MINUS_SRC_COLOR;
import static neo.open.gl.QGLConstantsIfc.GL_PROJECTION;
import static neo.open.gl.QGLConstantsIfc.GL_Q;
import static neo.open.gl.QGLConstantsIfc.GL_QUADS;
import static neo.open.gl.QGLConstantsIfc.GL_R;
import static neo.open.gl.QGLConstantsIfc.GL_REPLACE;
import static neo.open.gl.QGLConstantsIfc.GL_S;
import static neo.open.gl.QGLConstantsIfc.GL_SCISSOR_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_SMOOTH;
import static neo.open.gl.QGLConstantsIfc.GL_SRC_ALPHA;
import static neo.open.gl.QGLConstantsIfc.GL_SRC_ALPHA_SATURATE;
import static neo.open.gl.QGLConstantsIfc.GL_SRC_COLOR;
import static neo.open.gl.QGLConstantsIfc.GL_STENCIL_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_T;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE0_ARB;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_2D;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_3D;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_COORD_ARRAY;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_CUBE_MAP;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_ENV;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_ENV_MODE;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_GEN_MODE;
import static neo.open.gl.QGLConstantsIfc.GL_VERTEX_ARRAY;
import static neo.open.gl.QGLConstantsIfc.GL_ZERO;
import static neo.sys.win_glimp.GLimp_SwapBuffers;
import static neo.sys.win_shared.Sys_Milliseconds;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import neo.Renderer.Image.idImage;
import neo.Renderer.tr_local.copyRenderCommand_t;
import neo.Renderer.tr_local.drawSurfsCommand_t;
import neo.Renderer.tr_local.emptyCommand_t;
import neo.Renderer.tr_local.glstate_t;
import neo.Renderer.tr_local.setBufferCommand_t;
import neo.Renderer.tr_local.tmu_t;

/**
 *
 */
public class tr_backend {

    /*
     ======================
     RB_SetDefaultGLState

     This should initialize all GL state that any part of the entire program
     may touch, including the editor.
     ======================
     */
    public static void RB_SetDefaultGLState() {
        int i;

        RB_LogComment("--- R_SetDefaultGLState ---\n");

        qglClearDepth(1.0f);
        qglColor4f(1, 1, 1, 1);

        // the vertex array is always enabled
        qglEnableClientState(GL_VERTEX_ARRAY);
        qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
        qglDisableClientState(GL_COLOR_ARRAY);

        //
        // make sure our GL state vector is set correctly
        //
        backEnd.glState = new glstate_t();//memset(backEnd.glState, 0, sizeof(backEnd.glState));
        backEnd.glState.forceGlState = true;

        qglColorMask(1, 1, 1, 1);

        qglEnable(GL_DEPTH_TEST);
        qglEnable(GL_BLEND);
        qglEnable(GL_SCISSOR_TEST);
        qglEnable(GL_CULL_FACE);
        qglDisable(GL_LIGHTING);
        qglDisable(GL_LINE_STIPPLE);
        qglDisable(GL_STENCIL_TEST);

        qglPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        qglDepthMask(QGL_TRUE);
        qglDepthFunc(GL_ALWAYS);

        qglCullFace(GL_FRONT_AND_BACK);
        qglShadeModel(GL_SMOOTH);

        if (r_useScissor.GetBool()) {
            qglScissor(0, 0, glConfig.vidWidth, glConfig.vidHeight);
        }

        for (i = glConfig.maxTextureUnits - 1; i >= 0; i--) {
            GL_SelectTexture(i);

            // object linear texgen is our default
            qglTexGenf(GL_S, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            qglTexGenf(GL_T, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            qglTexGenf(GL_R, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            qglTexGenf(GL_Q, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);

            GL_TexEnv(GL_MODULATE);
            qglDisable(GL_TEXTURE_2D);
            if (glConfig.texture3DAvailable) {
                qglDisable(GL_TEXTURE_3D);
            }
            if (glConfig.cubeMapAvailable) {
                qglDisable(GL_TEXTURE_CUBE_MAP/*_EXT*/);
            }
        }
    }


    /*
     ====================
     RB_LogComment
     ====================
     */
    public static void RB_LogComment(final Object... comment) {
//   va_list marker;

        if (NOT(tr.logFile)) {
            return;
        }

        fprintf(tr.logFile, "// ");
//	va_start( marker, comment );
        vfprintf(tr.logFile, comment);
//	va_end( marker );
    }

    //=============================================================================
    /*
     ====================
     GL_SelectTexture
     ====================
     */
    public static void GL_SelectTexture(int unit) {
        if (tr_local.backEnd.glState.currenttmu == unit) {
            return;
        }
        if ((unit < 0) || ((unit >= tr_local.glConfig.maxTextureUnits) && (unit >= tr_local.glConfig.maxTextureImageUnits))) {
            common.Warning("GL_SelectTexture: unit = %d", unit);
            return;
        }
        qglActiveTextureARB(GL_TEXTURE0_ARB + unit);
        qglClientActiveTextureARB(GL_TEXTURE0_ARB + unit);
        RB_LogComment("glActiveTextureARB( %d );\nglClientActiveTextureARB( %d );\n", unit, unit);
        tr_local.backEnd.glState.currenttmu = unit;
    }

    /*
     ====================
     GL_Cull
     This handles the flipping needed when the view being
     rendered is a mirored view.
     ====================
     */
    public static void GL_Cull(int cullType) {
        if (tr_local.backEnd.glState.faceCulling == cullType) {
            return;
        }
        if (cullType == CT_TWO_SIDED.ordinal()) {
            qglDisable(GL_CULL_FACE);
        } else {
            if (tr_local.backEnd.glState.faceCulling == CT_TWO_SIDED.ordinal()) {
                qglEnable(GL_CULL_FACE);
            }
            if (cullType == CT_BACK_SIDED.ordinal()) {
                if (tr_local.backEnd.viewDef.isMirror) {
                    qglCullFace(GL_FRONT);
                } else {
                    qglCullFace(GL_BACK);
                }
            } else {
                if (tr_local.backEnd.viewDef.isMirror) {
                    qglCullFace(GL_BACK);
                } else {
                    qglCullFace(GL_FRONT);
                }
            }
        }
        tr_local.backEnd.glState.faceCulling = cullType;
    }

    public static void GL_Cull(Enum cullType) {
        GL_Cull(cullType.ordinal());
    }

    /*
     ====================
     GL_TexEnv
     ====================
     */
    public static void GL_TexEnv(int env) {
        tmu_t tmu;
        tmu = tr_local.backEnd.glState.tmu[tr_local.backEnd.glState.currenttmu];
        if (env == tmu.texEnv) {
            return;
        }
        tmu.texEnv = env;
        switch (env) {
            case GL_COMBINE:
            case GL_MODULATE:
            case GL_REPLACE:
            case GL_DECAL:
            case GL_ADD:
                qglTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, env);
                break;
            default:
                common.Error("GL_TexEnv: invalid env '%d' passed\n", env);
                break;
        }
    }

    /*
     =================
     GL_ClearStateDelta
     Clears the state delta bits, so the next GL_State
     will set every item
     =================
     */
    public static void GL_ClearStateDelta() {
        tr_local.backEnd.glState.forceGlState = true;
    }

    /*
     ====================
     GL_State
     This routine is responsible for setting the most commonly changed state
     ====================
     */private static int DBG_GL_State=0;
    public static void GL_State(int stateBits) {
        int diff;
        DBG_GL_State++;
        if (!r_useStateCaching.GetBool() || backEnd.glState.forceGlState) {
            // make sure everything is set all the time, so we
            // can see if our delta checking is screwing up
            diff = -1;
            backEnd.glState.forceGlState = false;
        } else {
            diff = stateBits ^ backEnd.glState.glStateBits;
            if (0 == diff) {
                return;
            }
        }

        //
        // check depthFunc bits
        //
        if ((diff & (GLS_DEPTHFUNC_EQUAL | GLS_DEPTHFUNC_LESS | GLS_DEPTHFUNC_ALWAYS)) != 0) {
            if ((stateBits & GLS_DEPTHFUNC_EQUAL) != 0) {
                qglDepthFunc(GL_EQUAL);
            } else if ((stateBits & GLS_DEPTHFUNC_ALWAYS) != 0) {
                qglDepthFunc(GL_ALWAYS);
            } else {
                qglDepthFunc(GL_LEQUAL);
            }
        }

        //
        // check blend bits
        //
        if ((diff & (GLS_SRCBLEND_BITS | GLS_DSTBLEND_BITS)) != 0) {
            int/*GLenum*/ srcFactor, dstFactor;
            switch (stateBits & GLS_SRCBLEND_BITS) {
                case GLS_SRCBLEND_ZERO:
                    srcFactor = GL_ZERO;
                    break;
                case GLS_SRCBLEND_ONE:
                    srcFactor = GL_ONE;
                    break;
                case GLS_SRCBLEND_DST_COLOR:
                    srcFactor = GL_DST_COLOR;
                    break;
                case GLS_SRCBLEND_ONE_MINUS_DST_COLOR:
                    srcFactor = GL_ONE_MINUS_DST_COLOR;
                    break;
                case GLS_SRCBLEND_SRC_ALPHA:
                    srcFactor = GL_SRC_ALPHA;
                    break;
                case GLS_SRCBLEND_ONE_MINUS_SRC_ALPHA:
                    srcFactor = GL_ONE_MINUS_SRC_ALPHA;
                    break;
                case GLS_SRCBLEND_DST_ALPHA:
                    srcFactor = GL_DST_ALPHA;
                    break;
                case GLS_SRCBLEND_ONE_MINUS_DST_ALPHA:
                    srcFactor = GL_ONE_MINUS_DST_ALPHA;
                    break;
                case GLS_SRCBLEND_ALPHA_SATURATE:
                    srcFactor = GL_SRC_ALPHA_SATURATE;
                    break;
                default:
                    srcFactor = GL_ONE;
                    common.Error("GL_State: invalid src blend state bits\n");
                    break;
            }

            switch (stateBits & GLS_DSTBLEND_BITS) {
                case GLS_DSTBLEND_ZERO:
                    dstFactor = GL_ZERO;
                    break;
                case GLS_DSTBLEND_ONE:
                    dstFactor = GL_ONE;
                    break;
                case GLS_DSTBLEND_SRC_COLOR:
                    dstFactor = GL_SRC_COLOR;
                    break;
                case GLS_DSTBLEND_ONE_MINUS_SRC_COLOR:
                    dstFactor = GL_ONE_MINUS_SRC_COLOR;
                    break;
                case GLS_DSTBLEND_SRC_ALPHA:
                    dstFactor = GL_SRC_ALPHA;
                    break;
                case GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA:
                    dstFactor = GL_ONE_MINUS_SRC_ALPHA;
                    break;
                case GLS_DSTBLEND_DST_ALPHA:
                    dstFactor = GL_DST_ALPHA;
                    break;
                case GLS_DSTBLEND_ONE_MINUS_DST_ALPHA:
                    dstFactor = GL_ONE_MINUS_DST_ALPHA;
                    break;
                default:
                    dstFactor = GL_ONE;
                    common.Error("GL_State: invalid dst blend state bits\n");
                    break;
            }
//            qglEnable(34336);
//            qglEnable(34820);
//            if (srcFactor == 770) {
                qglBlendFunc(srcFactor, dstFactor);
//            }
//            System.out.printf("GL_State(%d, %d)--%d;\n", srcFactor, dstFactor, DBG_GL_State);
        }
        
        //
        // check depthmask
        //
        if ((diff & GLS_DEPTHMASK) != 0) {
            if ((stateBits & GLS_DEPTHMASK) != 0) {
                qglDepthMask(QGL_FALSE);
            } else {
                qglDepthMask(QGL_TRUE);
            }
        }

        //
        // check colormask
        //
        if ((diff & (GLS_REDMASK | GLS_GREENMASK | GLS_BLUEMASK | GLS_ALPHAMASK)) != 0) {
           final boolean r = (stateBits & GLS_REDMASK) == 0;
           final boolean g = (stateBits & GLS_GREENMASK) == 0;
           final boolean b = (stateBits & GLS_BLUEMASK) == 0;
           final boolean a = (stateBits & GLS_ALPHAMASK) == 0;
            qglColorMask(r, g, b, a);//solid backgroundus
        }

        //
        // fill/line mode
        //
        if ((diff & GLS_POLYMODE_LINE) != 0) {
            if ((stateBits & GLS_POLYMODE_LINE) != 0) {
                qglPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            } else {
                qglPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            }
        }

        //
        // alpha test
        //
        if ((diff & GLS_ATEST_BITS) != 0) {
            if(backEnd.viewDef.numDrawSurfs==5){
                final tr_local.drawSurf_s temp = backEnd.viewDef.drawSurfs[3];
//                backEnd.viewDef.drawSurfs[0] =
//                backEnd.viewDef.drawSurfs[1] =
//                backEnd.viewDef.drawSurfs[2] =
//                backEnd.viewDef.drawSurfs[3] =
//                backEnd.viewDef.drawSurfs[4] =
//                temp;
////                temp.shaderRegisters[0] = 330.102997f;
////                temp.shaderRegisters[1] = 1.00000000f;
////                temp.shaderRegisters[2] = 1.00000000f;
////                temp.shaderRegisters[3] = 1.00000000f;
////                temp.shaderRegisters[4] = 1.00000000f;
////                temp.shaderRegisters[5] = 0.000000000f;
////                temp.shaderRegisters[6] = 0.000000000f;
////                temp.shaderRegisters[7] = 0.000000000f;
////                temp.shaderRegisters[8] = 0.000000000f;
////                temp.shaderRegisters[9] = 0.000000000f;
////                temp.shaderRegisters[10] = 0.000000000f;
////                temp.shaderRegisters[11] = 0.000000000f;
////                temp.shaderRegisters[12] = 0.000000000f;
////                temp.shaderRegisters[13] = 0.000000000f;
////                temp.shaderRegisters[14] = 0.000000000f;
////                temp.shaderRegisters[15] = 0.000000000f;
////                temp.shaderRegisters[16] = 0.000000000f;
////                temp.shaderRegisters[17] = 0.000000000f;
////                temp.shaderRegisters[18] = 0.000000000f;
////                temp.shaderRegisters[19] = 0.000000000f;
////                temp.shaderRegisters[20] = 0.000000000f;
////                temp.shaderRegisters[21] = 1.00000000f;
////                temp.shaderRegisters[22] = 0.00999999978f;
////                temp.shaderRegisters[23] = 3.30102992f;
////                temp.shaderRegisters[24] = 0.0120000001f;
////                temp.shaderRegisters[25] = 3.96123600f;
////                temp.shaderRegisters[26] = 0.000000000f;
            }
            switch (stateBits & GLS_ATEST_BITS) {
                case 0:
                    qglDisable(GL_ALPHA_TEST);
                    break;
                case GLS_ATEST_EQ_255:
                    qglEnable(GL_ALPHA_TEST);
                    qglAlphaFunc(GL_EQUAL, 1);
                    break;
                case GLS_ATEST_LT_128:
                    qglEnable(GL_ALPHA_TEST);
                    qglAlphaFunc(GL_LESS, 0.5f);
                    break;
                case GLS_ATEST_GE_128:
                    qglEnable(GL_ALPHA_TEST);
                    qglAlphaFunc(GL_GEQUAL, 0.5f);
                    break;
                default:
                    assert (false);
                    break;
            }
        }
        backEnd.glState.glStateBits = stateBits;
    }

    /*
     ============================================================================

     RENDER BACK END THREAD FUNCTIONS

     ============================================================================
     */

    /*
     =============
     RB_SetGL2D

     This is not used by the normal game paths, just by some tools
     =============
     */
    public static void RB_SetGL2D() {
        // set 2D virtual screen size
        qglViewport(0, 0, glConfig.vidWidth, glConfig.vidHeight);
        if (r_useScissor.GetBool()) {
            qglScissor(0, 0, glConfig.vidWidth, glConfig.vidHeight);
        }
        qglMatrixMode(GL_PROJECTION);
        qglLoadIdentity();
        qglOrtho(0, 640, 480, 0, 0, 1);		// always assume 640x480 virtual coordinates
        qglMatrixMode(GL_MODELVIEW);
        qglLoadIdentity();

        GL_State(GLS_DEPTHFUNC_ALWAYS
                | GLS_SRCBLEND_SRC_ALPHA
                | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA);

        GL_Cull(CT_TWO_SIDED);

        qglDisable(GL_DEPTH_TEST);
        qglDisable(GL_STENCIL_TEST);
    }

    /*
     =============
     RB_SetBuffer

     =============
     */
    public static void RB_SetBuffer(final Object data) {
        final setBufferCommand_t cmd;

        // see which draw buffer we want to render the frame to
        cmd = (setBufferCommand_t) data;

        backEnd.frameCount = cmd.frameCount;

        qglDrawBuffer(cmd.buffer);

        // clear screen for debugging
        // automatically enable this with several other debug tools
        // that might leave unrendered portions of the screen
        if ((r_clear.GetFloat() != 0) || (r_clear.GetString().length() != 1) || r_lockSurfaces.GetBool() || r_singleArea.GetBool() || r_showOverDraw.GetBool()) {
            try (Scanner sscanf = new Scanner(r_clear.GetString())) {
//		if ( sscanf( r_clear.GetString(), "%f %f %f", c[0], c[1], c[2] ) == 3 ) {
                final float[] c = {sscanf.nextFloat(), sscanf.nextFloat(), sscanf.nextFloat()};
                //if 3 floats are parsed
                qglClearColor(c[0], c[1], c[2], 1);
            } catch (final NoSuchElementException elif) {
                if (r_clear.GetInteger() == 2) {
                    qglClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                } else if (r_showOverDraw.GetBool()) {
                    qglClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                } else {
                    qglClearColor(0.4f, 0.0f, 0.25f, 1.0f);
                }
            }
            qglClear(GL_COLOR_BUFFER_BIT);
        }
    }

    /*
     ===============
     RB_ShowImages

     Draw all the images to the screen, on top of whatever
     was there.  This is used to test for texture thrashing.
     ===============
     */
    public static void RB_ShowImages() {
        int i;
        idImage image;
        float x, y, w, h;
        int start, end;

        RB_SetGL2D();

        //qglClearColor( 0.2, 0.2, 0.2, 1 );
        //qglClear( GL_COLOR_BUFFER_BIT );
        qglFinish();

        start = Sys_Milliseconds();

        for (i = 0; i < globalImages.images.Num(); i++) {
            image = globalImages.images.oGet(i);
            
            if ((image.texNum == idImage.TEXTURE_NOT_LOADED) && (image.partialImage == null)) {
                continue;
            }

            w = glConfig.vidWidth / 20;
            h = glConfig.vidHeight / 15;
            x = (i % 20) * w;
            y = (i / 20) * h;

            // show in proportional size in mode 2
            if (r_showImages.GetInteger() == 2) {
                w *= image.uploadWidth / 512.0f;
                h *= image.uploadHeight / 512.0f;
            }

            image.Bind();
            qglBegin(GL_QUADS);
            qglTexCoord2f(0, 0);
            qglVertex2f(x, y);
            qglTexCoord2f(1, 0);
            qglVertex2f(x + w, y);
            qglTexCoord2f(1, 1);
            qglVertex2f(x + w, y + h);
            qglTexCoord2f(0, 1);
            qglVertex2f(x, y + h);
            qglEnd();
        }

        qglFinish();

        end = Sys_Milliseconds();
        common.Printf("%d msec to draw all images\n", end - start);
    }


    /*
     =============
     RB_SwapBuffers

     =============
     */
    public static void RB_SwapBuffers(final Object data) {
        // texture swapping test
        if (r_showImages.GetInteger() != 0) {
            RB_ShowImages();
        }

        // force a gl sync if requested
        if (r_finish.GetBool()) {
            qglFinish();
        }

        RB_LogComment("***************** RB_SwapBuffers *****************\n\n\n");

        // don't flip if drawing to front buffer
        if (!r_frontBuffer.GetBool()) {
            GLimp_SwapBuffers();
        }
    }

    /*
     =============
     RB_CopyRender

     Copy part of the current framebuffer to an image
     =============
     */
    public static void RB_CopyRender(final Object data) {
        final copyRenderCommand_t cmd;

        cmd = (copyRenderCommand_t) data;

        if (r_skipCopyTexture.GetBool()) {
            return;
        }

        RB_LogComment("***************** RB_CopyRender *****************\n");

        if (cmd.image != null) {
            final int[] imageWidth = {cmd.imageWidth}, imageHeight = {cmd.imageHeight};
            cmd.image.CopyFramebuffer(cmd.x, cmd.y, imageWidth, imageHeight, false);
            cmd.imageWidth = imageWidth[0];
            cmd.imageHeight = imageHeight[0];
        }
    }

    /*
     ====================
     RB_ExecuteBackEndCommands

     This function will be called syncronously if running without
     smp extensions, or asyncronously by another thread.
     ====================
     */
    static int backEndStartTime, backEndFinishTime;

    public static void RB_ExecuteBackEndCommands(emptyCommand_t cmds) {
        // r_debugRenderToTexture
        int c_draw3d = 0, c_draw2d = 0, c_setBuffers = 0, c_swapBuffers = 0, c_copyRenders = 0;

        if ((RC_NOP == cmds.commandId) && (null == cmds.next)) {
            return;
        }

        backEndStartTime = Sys_Milliseconds();

        // needed for editor rendering
        RB_SetDefaultGLState();

        // upload any image loads that have completed
        globalImages.CompleteBackgroundImageLoads();

        for (; cmds != null; cmds = cmds.next) {
            switch (cmds.commandId) {
                case RC_NOP:
                    break;
                case RC_DRAW_VIEW:
                    RB_DrawView(cmds);
                    if (((drawSurfsCommand_t) cmds).viewDef.viewEntitys != null) {
                        c_draw3d++;
                    } else {
                        c_draw2d++;
                    }
                    break;
                case RC_SET_BUFFER:
                    RB_SetBuffer(cmds);
                    c_setBuffers++;
                    break;
                case RC_SWAP_BUFFERS:
                    RB_SwapBuffers(cmds);
                    c_swapBuffers++;
                    break;
                case RC_COPY_RENDER:
                    RB_CopyRender(cmds);
                    c_copyRenders++;
                    break;
                default:
                    common.Error("RB_ExecuteBackEndCommands: bad commandId");
                    break;
            }
        }

        // go back to the default texture so the editor doesn't mess up a bound image
        qglBindTexture(GL_TEXTURE_2D, 0);
        backEnd.glState.tmu[0].current2DMap = -1;

        // stop rendering on this thread
        backEndFinishTime = Sys_Milliseconds();
        backEnd.pc.msec = backEndFinishTime - backEndStartTime;

        if (r_debugRenderToTexture.GetInteger() == 1) {
            common.Printf("3d: %d, 2d: %d, SetBuf: %d, SwpBuf: %d, CpyRenders: %d, CpyFrameBuf: %d\n", c_draw3d, c_draw2d, c_setBuffers, c_swapBuffers, c_copyRenders, backEnd.c_copyFrameBuffer);
            backEnd.c_copyFrameBuffer = 0;
        }
    }

    private static void fprintf(FileChannel logFile, String string) {
        if (NOT(logFile)) {
            return;
        }

        try {
            logFile.write(ByteBuffer.wrap(string.getBytes()));
        } catch (final IOException ex) {
            Logger.getLogger(tr_backend.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void vfprintf(FileChannel logFile, Object... comments) {
        if (NOT(logFile)) {
            return;
        }

        try {
            String bla = "";
            for (final Object c : comments) {
                bla += c;
            }

            logFile.write(ByteBuffer.wrap(bla.getBytes()));
        } catch (final IOException ex) {
            Logger.getLogger(tr_backend.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
