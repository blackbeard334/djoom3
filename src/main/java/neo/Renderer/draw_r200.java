package neo.Renderer;

//import java.nio.IntBuffer;
//import java.util.Arrays;
//import static neo.Renderer.Image.globalImages;
//import static neo.Renderer.Material.materialCoverage_t.MC_TRANSLUCENT;
//import static neo.Renderer.Material.stageVertexColor_t.SVC_IGNORE;
//import neo.Renderer.Model.srfTriangles_s;
//import static neo.Renderer.RenderSystem_init.GL_CheckErrors;
//import static neo.Renderer.VertexCache.vertexCache;
//import static neo.Renderer.draw_common.RB_StencilShadowPass;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglAlphaFragmentOp1ATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglBeginFragmentShaderATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglBindFragmentShaderATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglColorFragmentOp1ATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglColorFragmentOp2ATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglColorFragmentOp3ATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglEndFragmentShaderATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglPassTexCoordATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglSampleMapATI;
//import static neo.Renderer.QGL.ATI_fragment_shader.qglSetFragmentShaderConstantATI;
//import static neo.Renderer.QGL.qglBindProgramARB;
//import static neo.Renderer.QGL.qglClear;
//import static neo.Renderer.QGL.qglColor4f;
//import static neo.Renderer.QGL.qglColorPointer;
//import static neo.Renderer.QGL.qglDisable;
//import static neo.Renderer.QGL.qglDisableClientState;
//import static neo.Renderer.QGL.qglEnable;
//import static neo.Renderer.QGL.qglEnableClientState;
//import static neo.Renderer.QGL.qglGetInteger;
//import static neo.Renderer.QGL.qglGetIntegerv;
//import static neo.Renderer.QGL.qglProgramEnvParameter4fvARB;
//import static neo.Renderer.QGL.qglScissor;
//import static neo.Renderer.QGL.qglStencilFunc;
//import static neo.Renderer.QGL.qglTexCoordPointer;
//import static neo.Renderer.QGL.qglVertexPointer;
//import static neo.Renderer.tr_backend.GL_SelectTexture;
//import static neo.Renderer.tr_backend.GL_State;
//import static neo.Renderer.tr_backend.RB_LogComment;
//import static neo.Renderer.tr_local.GLS_DEPTHFUNC_EQUAL;
//import static neo.Renderer.tr_local.GLS_DEPTHFUNC_LESS;
//import static neo.Renderer.tr_local.GLS_DEPTHMASK;
//import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE;
//import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE;
//import static neo.Renderer.tr_local.backEnd;
//import neo.Renderer.tr_local.drawInteraction_t;
//import neo.Renderer.tr_local.drawSurf_s;
//import static neo.Renderer.tr_local.glConfig;
//import neo.Renderer.tr_local.idScreenRect;
//import static neo.Renderer.tr_local.programParameter_t.PP_BUMP_MATRIX_S;
//import static neo.Renderer.tr_local.programParameter_t.PP_BUMP_MATRIX_T;
//import static neo.Renderer.tr_local.programParameter_t.PP_COLOR_ADD;
//import static neo.Renderer.tr_local.programParameter_t.PP_COLOR_MODULATE;
//import static neo.Renderer.tr_local.programParameter_t.PP_DIFFUSE_MATRIX_S;
//import static neo.Renderer.tr_local.programParameter_t.PP_DIFFUSE_MATRIX_T;
//import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_FALLOFF_S;
//import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_ORIGIN;
//import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_PROJECT_Q;
//import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_PROJECT_S;
//import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_PROJECT_T;
//import static neo.Renderer.tr_local.programParameter_t.PP_SPECULAR_MATRIX_S;
//import static neo.Renderer.tr_local.programParameter_t.PP_SPECULAR_MATRIX_T;
//import static neo.Renderer.tr_local.programParameter_t.PP_VIEW_ORIGIN;
//import static neo.Renderer.tr_local.program_t.VPROG_R200_INTERACTION;
//import static neo.Renderer.tr_local.program_t.VPROG_STENCIL_SHADOW;
//import neo.Renderer.tr_local.viewLight_s;
//import neo.Renderer.tr_render.DrawInteraction;
//import static neo.Renderer.tr_render.RB_CreateSingleDrawInteractions;
//import static neo.Renderer.tr_render.RB_DrawElementsWithCounters;
//import static neo.TempDump.NOT;
//import static neo.framework.BuildDefines.MACOS_X;
//import static neo.framework.Common.common;
//import neo.idlib.geometry.DrawVert.idDrawVert;
//import org.lwjgl.BufferUtils;
//
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE0_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE1_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE2_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE3_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE4_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE5_ARB;
//import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_PRIMARY_COLOR_ARB;
//import static org.lwjgl.opengl.ARBVertexProgram.GL_VERTEX_PROGRAM_ARB;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_2X_BIT_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_4X_BIT_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_BIAS_BIT_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_COLOR_ALPHA_PAIRING_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_CON_0_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_CON_1_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_CON_5_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_DOT3_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_FRAGMENT_SHADER_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_MAD_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_MOV_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_MUL_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_NUM_FRAGMENT_CONSTANTS_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_NUM_FRAGMENT_REGISTERS_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_NUM_INPUT_INTERPOLATOR_COMPONENTS_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_NUM_INSTRUCTIONS_PER_PASS_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_NUM_INSTRUCTIONS_TOTAL_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_NUM_LOOPBACK_COMPONENTS_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_NUM_PASSES_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_RED_BIT_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_REG_0_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_REG_1_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_REG_2_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_REG_3_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_REG_4_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_REG_5_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_SATURATE_BIT_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_SUB_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_SWIZZLE_STQ_DQ_ATI;
//import static org.lwjgl.opengl.ATIFragmentShader.GL_SWIZZLE_STR_ATI;
//import static org.lwjgl.opengl.ATITextFragmentShader.GL_TEXT_FRAGMENT_SHADER_ATI;
//import static org.lwjgl.opengl.GL11.GL_ALPHA;
//import static org.lwjgl.opengl.GL11.GL_ALWAYS;
//import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
//import static org.lwjgl.opengl.GL11.GL_FLOAT;
//import static org.lwjgl.opengl.GL11.GL_NONE;
//import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
//import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
//import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;
//import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
//import static org.lwjgl.opengl.GL11.GL_ZERO;

/**
 *
 */
public class draw_r200 {

//    /*
//
//     There are not enough vertex program texture coordinate outputs
//     to have unique texture coordinates for bump, specular, and diffuse,
//     so diffuse and specular are assumed to be the same mapping.
//
//     To handle properly, those cases with different diffuse and specular
//     mapping will need to be run as two passes.
//
//     */
//// changed from 1 to 255 to not conflict with ARB2 program names
//    static final int FPROG_FAST_PATH = 255;
//
//    static class atiFragmentShaderInfo_t {
//        /*GLint*/
//
//        //TODO:init
//        int numFragmentRegisters;               // 6
//        int numFragmentConstants;               // 8
//        int numPasses;                          // 2
//        int numInstructionsPerPass;             // 8
//        int numInstructionsTotal;               // 16
//        int colorAlphaPairing;                  // 1
//        int numLoopbackComponenets;             // 3
//        int numInputInterpolatorComponents;	    // 3
//    };
//    static atiFragmentShaderInfo_t fsi = new atiFragmentShaderInfo_t();
//
//    static class atiVertexShaderInfo_t {
//        // vertex shader invariants
//
//        int lightPos;           // light position in object coordinates
//        int viewerPos;		// viewer position in object coordinates
//        int lightProjectS;	// projected light s texgen
//        int lightProjectT;	// projected light t texgen
//        int lightProjectQ;	// projected light q texgen
//        int lightFalloffS;	// projected light falloff s texgen
//        int bumpTransformS;	// bump TEX0 S transformation
//        int bumpTransformT;	// bump TEX0 T transformation
//        int colorTransformS;	// diffuse/specular texture matrix
//        int colorTransformT;	// diffuse/specular texture matrix
////
//        // vertex shader variants
//        int texCoords;
//        int vertexColors;
//        int normals;
//        int tangents;
//        int biTangents;
//    };
//    static atiVertexShaderInfo_t vsi;
////
//    static final float[] zero = {0, 0, 0, 0};
//    static final float[] one = {1, 1, 1, 1};
//    static final float[] negOne = {-1, -1, -1, -1};
////
//
//    /*
//     ===================
//     RB_R200_ARB_DrawInteraction
//
//     ===================
//     */
//    public static class RB_R200_ARB_DrawInteraction extends DrawInteraction {
//
//        static final DrawInteraction INSTANCE = new RB_R200_ARB_DrawInteraction();
//
//        private RB_R200_ARB_DrawInteraction() {
//        }
//
//        @Override
//        void run(drawInteraction_t din) {
//            // check for the case we can't handle in a single pass (we could calculate this at shader parse time to optimize)
//            if (din.diffuseImage != globalImages.blackImage && din.specularImage != globalImages.blackImage
//                    && Arrays.equals(din.specularMatrix, din.diffuseMatrix)) {
////		common.Printf( "Note: Shader %s drawn as two pass on R200\n", din.surf.shader.getName() );
//
//                // draw the specular as a separate pass with a black diffuse map
//                drawInteraction_t d;
//                d = din;
//                d.diffuseImage = globalImages.blackImage;
//                Nio.arraycopy(d.specularMatrix, 0, d.diffuseMatrix, 0, d.specularMatrix.length);
//                run(d);
//
//                // now fall through and draw the diffuse pass with a black specular map
//                d = din;
//                din.oSet(d);
//                d.specularImage = globalImages.blackImage;
//            }
//
//            // load all the vertex program parameters
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_ORIGIN, din.localLightOrigin.ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_VIEW_ORIGIN, din.localViewOrigin.ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_PROJECT_S, din.lightProjection[0].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_PROJECT_T, din.lightProjection[1].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_PROJECT_Q, din.lightProjection[2].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_FALLOFF_S, din.lightProjection[3].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_BUMP_MATRIX_S, din.bumpMatrix[0].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_BUMP_MATRIX_T, din.bumpMatrix[1].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_DIFFUSE_MATRIX_S, din.diffuseMatrix[0].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_DIFFUSE_MATRIX_T, din.diffuseMatrix[1].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_SPECULAR_MATRIX_S, din.diffuseMatrix[0].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_SPECULAR_MATRIX_T, din.diffuseMatrix[1].ToFloatPtr());
//
//            srfTriangles_s tri = din.surf.geo;
//            idDrawVert ac = new idDrawVert(vertexCache.Position(tri.ambientCache));//TODO:figure out how to work these damn casts.
//            qglVertexPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, /*(void *)&*/ ac.xyz.ToFloatPtr());
//
//            switch (din.vertexColor) {
//                case SVC_IGNORE:
//                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_MODULATE, zero);
//                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_ADD, one);
//                    break;
//                case SVC_MODULATE:
//                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_MODULATE, one);
//                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_ADD, zero);
//                    break;
//                case SVC_INVERSE_MODULATE:
//                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_MODULATE, negOne);
//                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_ADD, one);
//                    break;
//            }
//
//            // texture 0 = light projection
//            // texture 1 = light falloff
//            // texture 2 = surface diffuse
//            // texture 3 = surface specular
//            // texture 4 = surface bump
//            // texture 5 = normalization cube map
//            GL_SelectTexture(5);
//            if (din.ambientLight != 0) {
//                globalImages.ambientNormalMap.Bind();
//            } else {
//                globalImages.normalCubeMapImage.Bind();
//            }
//
//            GL_SelectTexture(4);
//            din.bumpImage.Bind();
//
//            GL_SelectTexture(3);
//            din.specularImage.Bind();
//            qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.normal.ToFloatPtr());
//
//            GL_SelectTexture(2);
//            din.diffuseImage.Bind();
//            qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.tangents[1].ToFloatPtr());
//
//            GL_SelectTexture(1);
//            din.lightFalloffImage.Bind();
//            qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.tangents[0].ToFloatPtr());
//
//            GL_SelectTexture(0);
//            din.lightImage.Bind();
//            qglTexCoordPointer(2, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.st.ToFloatPtr());
//
//            qglSetFragmentShaderConstantATI(GL_CON_0_ATI, din.diffuseColor.ToFloatPtr());
//            qglSetFragmentShaderConstantATI(GL_CON_1_ATI, din.specularColor.ToFloatPtr());
//
//            if (din.vertexColor != SVC_IGNORE) {
//                qglColorPointer(4, GL_UNSIGNED_BYTE, 0/*sizeof(idDrawVert)*/, ac.color);
//                qglEnableClientState(GL_COLOR_ARRAY);
//
//                RB_DrawElementsWithCounters(tri);
//
//                qglDisableClientState(GL_COLOR_ARRAY);
//                qglColor4f(1, 1, 1, 1);
//            } else {
//                RB_DrawElementsWithCounters(tri);
//            }
//        }
//    };
//
//    /*
//     ==================
//     RB_R200_ARB_CreateDrawInteractions
//     ==================
//     */
//    public static void RB_R200_ARB_CreateDrawInteractions(final drawSurf_s surf) {
//        if (NOT(surf)) {
//            return;
//        }
//
//        // force a space calculation for light vectors
//        backEnd.currentSpace = null;
//
//        // set the depth test
//        if (surf.material.Coverage() == MC_TRANSLUCENT /* != C_PERFORATED */) {
//            GL_State(GLS_SRCBLEND_ONE | GLS_DSTBLEND_ONE | GLS_DEPTHMASK | GLS_DEPTHFUNC_LESS);
//        } else {
//            // only draw on the alpha tested pixels that made it to the depth buffer
//            GL_State(GLS_SRCBLEND_ONE | GLS_DSTBLEND_ONE | GLS_DEPTHMASK | GLS_DEPTHFUNC_EQUAL);
//        }
//
//        // start the vertex shader
//        qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_R200_INTERACTION);
//        qglEnable(GL_VERTEX_PROGRAM_ARB);
//
//        // start the fragment shader
//        qglBindFragmentShaderATI(FPROG_FAST_PATH);
//        if (MACOS_X) {
//            qglEnable(GL_TEXT_FRAGMENT_SHADER_ATI);
//        } else {
//            qglEnable(GL_FRAGMENT_SHADER_ATI);
//        }
//
//        qglColor4f(1, 1, 1, 1);
//
//        GL_SelectTexture(1);
//        qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        GL_SelectTexture(2);
//        qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        GL_SelectTexture(3);
//        qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//
//        for (drawSurf_s surf2 = surf; surf2 != null; surf2 = surf2.nextOnLight) {
//            RB_CreateSingleDrawInteractions(surf2, RB_R200_ARB_DrawInteraction.INSTANCE);
//        }
//
//        GL_SelectTexture(5);
//        globalImages.BindNull();
//
//        GL_SelectTexture(4);
//        globalImages.BindNull();
//
//        GL_SelectTexture(3);
//        globalImages.BindNull();
//        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//
//        GL_SelectTexture(2);
//        globalImages.BindNull();
//        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//
//        GL_SelectTexture(1);
//        globalImages.BindNull();
//        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//
//        GL_SelectTexture(0);
//
//        qglDisable(GL_VERTEX_PROGRAM_ARB);
//        if (MACOS_X) {
//            qglDisable(GL_TEXT_FRAGMENT_SHADER_ATI);
//        } else {
//            qglDisable(GL_FRAGMENT_SHADER_ATI);
//        }
//    }
//
//    /*
//     ==================
//     RB_R200_DrawInteractions
//
//     ==================
//     */
//    public static void RB_R200_DrawInteractions() {
//        qglEnable(GL_STENCIL_TEST);
//
//        for (viewLight_s vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
//            // do fogging later
//            if (vLight.lightShader.IsFogLight()) {
//                continue;
//            }
//            if (vLight.lightShader.IsBlendLight()) {
//                continue;
//            }
//
//            backEnd.vLight = vLight;
//
//            RB_LogComment("---------- RB_RenderViewLight 0x%p ----------\n", vLight);
//
//            // clear the stencil buffer if needed
//            if (vLight.globalShadows[0] != null || vLight.localShadows[0] != null) {
//                backEnd.currentScissor = new idScreenRect(vLight.scissorRect);
//                if (RenderSystem_init.r_useScissor.GetBool()) {
//                    qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
//                            backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
//                            backEnd.currentScissor.x2 + 1 - backEnd.currentScissor.x1,
//                            backEnd.currentScissor.y2 + 1 - backEnd.currentScissor.y1);
//                }
//                qglClear(GL_STENCIL_BUFFER_BIT);
//            } else {
//                // no shadows, so no need to read or write the stencil buffer
//                // we might in theory want to use GL_ALWAYS instead of disabling
//                // completely, to satisfy the invarience rules
//                qglStencilFunc(GL_ALWAYS, 128, 255);
//            }
//
//            if (RenderSystem_init.r_useShadowVertexProgram.GetBool()) {
//                qglEnable(GL_VERTEX_PROGRAM_ARB);
//                qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_STENCIL_SHADOW);
//                RB_StencilShadowPass(vLight.globalShadows[0]);
//
//                RB_R200_ARB_CreateDrawInteractions(vLight.localInteractions[0]);
//
//                qglEnable(GL_VERTEX_PROGRAM_ARB);
//                qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_STENCIL_SHADOW);
//                RB_StencilShadowPass(vLight.localShadows[0]);
//
//                RB_R200_ARB_CreateDrawInteractions(vLight.globalInteractions[0]);
//
//                qglDisable(GL_VERTEX_PROGRAM_ARB);	// if there weren't any globalInteractions, it would have stayed on
//            } else {
//                RB_StencilShadowPass(vLight.globalShadows[0]);
//                RB_R200_ARB_CreateDrawInteractions(vLight.localInteractions[0]);
//
//                RB_StencilShadowPass(vLight.localShadows[0]);
//                RB_R200_ARB_CreateDrawInteractions(vLight.globalInteractions[0]);
//            }
//
//            if (RenderSystem_init.r_skipTranslucent.GetBool()) {
//                continue;
//            }
//
//            // disable stencil testing for translucent interactions, because
//            // the shadow isn't calculated at their point, and the shadow
//            // behind them may be depth fighting with a back side, so there
//            // isn't any reasonable thing to do
//            qglStencilFunc(GL_ALWAYS, 128, 255);
//            RB_R200_ARB_CreateDrawInteractions(vLight.translucentInteractions[0]);
//        }
//    }
//
//
//    /*
//     =================
//     R_BuildSurfaceFragmentProgram
//     =================
//     */
//    public static void R_BuildSurfaceFragmentProgram(int programNum) {
//        qglBindFragmentShaderATI(programNum);
//
//        qglBeginFragmentShaderATI();
//
//        // texture 0 = light projection
//        // texture 1 = light falloff
//        // texture 2 = surface diffuse
//        // texture 3 = surface specular
//        // texture 4 = surface bump
//        // texture 5 = normalization cube map
//        // texcoord 0 = light projection texGen
//        // texcoord 1 = light falloff texGen
//        // texcoord 2 = bumpmap texCoords
//        // texcoord 3 = specular / diffuse texCoords
//        // texcoord 4 = halfangle vector in local tangent space
//        // texcoord 5 = vector to light in local tangent space
//        // constant 0 = diffuse modulate
//        // constant 1 = specular modulate
//        // constant 5 = internal use for 0.75 constant
//        qglSampleMapATI(GL_REG_0_ATI, GL_TEXTURE0_ARB, GL_SWIZZLE_STQ_DQ_ATI);
//        qglSampleMapATI(GL_REG_1_ATI, GL_TEXTURE1_ARB, GL_SWIZZLE_STR_ATI);
//        qglSampleMapATI(GL_REG_4_ATI, GL_TEXTURE2_ARB, GL_SWIZZLE_STR_ATI);
//        qglSampleMapATI(GL_REG_5_ATI, GL_TEXTURE5_ARB, GL_SWIZZLE_STR_ATI);
//
//        // move the alpha component to the red channel to support rxgb normal map compression
//        if (globalImages.image_useNormalCompression.GetInteger() == 2) {
//            qglColorFragmentOp1ATI(GL_MOV_ATI, GL_REG_4_ATI, GL_RED_BIT_ATI, GL_NONE,
//                    GL_REG_4_ATI, GL_ALPHA, GL_NONE);
//        }
//
//        // light projection * light falloff
//        qglColorFragmentOp2ATI(GL_MUL_ATI, GL_REG_0_ATI, GL_NONE, GL_NONE,
//                GL_REG_0_ATI, GL_NONE, GL_NONE,
//                GL_REG_1_ATI, GL_NONE, GL_NONE);
//
//        // vectorToLight dot bumpMap
//        qglColorFragmentOp2ATI(GL_DOT3_ATI, GL_REG_1_ATI, GL_NONE, GL_SATURATE_BIT_ATI,
//                GL_REG_4_ATI, GL_NONE, GL_2X_BIT_ATI | GL_BIAS_BIT_ATI,
//                GL_REG_5_ATI, GL_NONE, GL_2X_BIT_ATI | GL_BIAS_BIT_ATI);
//
//        // bump * light
//        qglColorFragmentOp2ATI(GL_MUL_ATI, GL_REG_0_ATI, GL_NONE, GL_NONE,
//                GL_REG_0_ATI, GL_NONE, GL_NONE,
//                GL_REG_1_ATI, GL_NONE, GL_NONE);
//
//        //-------------------
//        // carry over the incomingLight calculation
//        qglPassTexCoordATI(GL_REG_0_ATI, GL_REG_0_ATI, GL_SWIZZLE_STR_ATI);
//
//        // sample the diffuse surface map
//        qglSampleMapATI(GL_REG_2_ATI, GL_TEXTURE3_ARB, GL_SWIZZLE_STR_ATI);
//
//        // sample the specular surface map
//        qglSampleMapATI(GL_REG_3_ATI, GL_TEXTURE3_ARB, GL_SWIZZLE_STR_ATI);
//
//        // we will use the surface bump map again
//        qglPassTexCoordATI(GL_REG_4_ATI, GL_REG_4_ATI, GL_SWIZZLE_STR_ATI);
//
//        // normalize the specular halfangle
//        qglSampleMapATI(GL_REG_5_ATI, GL_TEXTURE4_ARB, GL_SWIZZLE_STR_ATI);
//
//        // R1 = halfangle dot surfaceNormal
//        qglColorFragmentOp2ATI(GL_DOT3_ATI, GL_REG_1_ATI, GL_NONE, GL_SATURATE_BIT_ATI,
//                GL_REG_4_ATI, GL_NONE, GL_2X_BIT_ATI | GL_BIAS_BIT_ATI,
//                GL_REG_5_ATI, GL_NONE, GL_2X_BIT_ATI | GL_BIAS_BIT_ATI);
//
//        // R1 = 4 * ( R1 - 0.75 )
//        // subtract 0.75 and quadruple to tighten the specular spot
//        float[] data = {0.75f, 0.75f, 0.75f, 0.75f};
//        qglSetFragmentShaderConstantATI(GL_CON_5_ATI, data);
//        qglColorFragmentOp2ATI(GL_SUB_ATI, GL_REG_1_ATI, GL_NONE, GL_4X_BIT_ATI | GL_SATURATE_BIT_ATI,
//                GL_REG_1_ATI, GL_NONE, GL_NONE,
//                GL_CON_5_ATI, GL_NONE, GL_NONE);
//
//        // R1 = R1 * R1
//        // sqare the stretched specular result
//        qglColorFragmentOp2ATI(GL_MUL_ATI, GL_REG_1_ATI, GL_NONE, GL_SATURATE_BIT_ATI,
//                GL_REG_1_ATI, GL_NONE, GL_NONE,
//                GL_REG_1_ATI, GL_NONE, GL_NONE);
//
//        // R1 = R1 * R3
//        // R1 = specular power * specular texture * 2
//        qglColorFragmentOp2ATI(GL_MUL_ATI, GL_REG_1_ATI, GL_NONE, GL_2X_BIT_ATI | GL_SATURATE_BIT_ATI,
//                GL_REG_1_ATI, GL_NONE, GL_NONE,
//                GL_REG_3_ATI, GL_NONE, GL_NONE);
//
//        // R2 = R2 * CONST0
//        // down modulate the diffuse map
//        qglColorFragmentOp2ATI(GL_MUL_ATI, GL_REG_2_ATI, GL_NONE, GL_SATURATE_BIT_ATI,
//                GL_REG_2_ATI, GL_NONE, GL_NONE,
//                GL_CON_0_ATI, GL_NONE, GL_NONE);
//
//        // R2 = R2 + R1 * CONST1
//        // diffuse + specular * specular color
//        qglColorFragmentOp3ATI(GL_MAD_ATI, GL_REG_2_ATI, GL_NONE, GL_SATURATE_BIT_ATI,
//                GL_REG_1_ATI, GL_NONE, GL_NONE,
//                GL_CON_1_ATI, GL_NONE, GL_NONE,
//                GL_REG_2_ATI, GL_NONE, GL_NONE);
//
//        // out = reflectance * incoming light
//        qglColorFragmentOp2ATI(GL_MUL_ATI, GL_REG_0_ATI, GL_NONE, GL_SATURATE_BIT_ATI,
//                GL_REG_0_ATI, GL_NONE, GL_NONE,
//                GL_REG_2_ATI, GL_NONE, GL_NONE);
//
//        // out * vertex color
//        qglColorFragmentOp2ATI(GL_MUL_ATI, GL_REG_0_ATI, GL_NONE, GL_NONE,
//                GL_REG_0_ATI, GL_NONE, GL_NONE,
//                GL_PRIMARY_COLOR_ARB, GL_NONE, GL_NONE);
//
//        // out alpha = 0 to allow blending optimization
//        qglAlphaFragmentOp1ATI(GL_MOV_ATI, GL_REG_0_ATI, GL_NONE,
//                GL_ZERO, GL_NONE, GL_NONE);
//
//        qglEndFragmentShaderATI();
//
//        GL_CheckErrors();
//    }
//
//    /*
//     =================
//     R_R200_Init
//     =================
//     */
//    public static void R_R200_Init() {
//        glConfig.allowR200Path = false;
//
//        common.Printf("----------- R200_Init -----------\n");
//
//        if (!glConfig.atiFragmentShaderAvailable || !glConfig.ARBVertexProgramAvailable || !glConfig.ARBVertexBufferObjectAvailable) {
//            common.Printf("Not available.\n");
//            return;
//        }
//
//        GL_CheckErrors();
//
//        fsi.numFragmentRegisters = qglGetInteger(GL_NUM_FRAGMENT_REGISTERS_ATI);
//        fsi.numFragmentConstants = qglGetInteger(GL_NUM_FRAGMENT_CONSTANTS_ATI);
//        fsi.numPasses = qglGetInteger(GL_NUM_PASSES_ATI);
//        fsi.numInstructionsPerPass = qglGetInteger(GL_NUM_INSTRUCTIONS_PER_PASS_ATI);
//        fsi.numInstructionsTotal = qglGetInteger(GL_NUM_INSTRUCTIONS_TOTAL_ATI);
//        fsi.colorAlphaPairing = qglGetInteger(GL_COLOR_ALPHA_PAIRING_ATI);
//        fsi.numLoopbackComponenets = qglGetInteger(GL_NUM_LOOPBACK_COMPONENTS_ATI);
//        fsi.numInputInterpolatorComponents = qglGetInteger(GL_NUM_INPUT_INTERPOLATOR_COMPONENTS_ATI);
//
//        common.Printf("GL_NUM_FRAGMENT_REGISTERS_ATI: %d\n", fsi.numFragmentRegisters);
//        common.Printf("GL_NUM_FRAGMENT_CONSTANTS_ATI: %d\n", fsi.numFragmentConstants);
//        common.Printf("GL_NUM_PASSES_ATI: %d\n", fsi.numPasses);
//        common.Printf("GL_NUM_INSTRUCTIONS_PER_PASS_ATI: %d\n", fsi.numInstructionsPerPass);
//        common.Printf("GL_NUM_INSTRUCTIONS_TOTAL_ATI: %d\n", fsi.numInstructionsTotal);
//        common.Printf("GL_COLOR_ALPHA_PAIRING_ATI: %d\n", fsi.colorAlphaPairing);
//        common.Printf("GL_NUM_LOOPBACK_COMPONENTS_ATI: %d\n", fsi.numLoopbackComponenets);
//        common.Printf("GL_NUM_INPUT_INTERPOLATOR_COMPONENTS_ATI: %d\n", fsi.numInputInterpolatorComponents);
//
//        common.Printf("FPROG_FAST_PATH\n");
//        R_BuildSurfaceFragmentProgram(FPROG_FAST_PATH);
//
//        common.Printf("---------------------\n");
//
//        glConfig.allowR200Path = true;
//    }
}
