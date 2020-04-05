package neo.Renderer;

//import static neo.Renderer.Image.globalImages;
//import static neo.Renderer.Material.stageVertexColor_t.SVC_IGNORE;
//import static neo.Renderer.Material.stageVertexColor_t.SVC_INVERSE_MODULATE;
//import static neo.Renderer.RenderSystem_init.GL_CheckErrors;
//import static neo.Renderer.VertexCache.vertexCache;
//import static neo.Renderer.draw_common.RB_StencilShadowPass;
//import static neo.Renderer.draw_nv20.fragmentProgram_t.FPROG_BUMP_AND_LIGHT;
//import static neo.Renderer.draw_nv20.fragmentProgram_t.FPROG_DIFFUSE_AND_SPECULAR_COLOR;
//import static neo.Renderer.draw_nv20.fragmentProgram_t.FPROG_DIFFUSE_COLOR;
//import static neo.Renderer.draw_nv20.fragmentProgram_t.FPROG_NUM_FRAGMENT_PROGRAMS;
//import static neo.Renderer.draw_nv20.fragmentProgram_t.FPROG_SPECULAR_COLOR;
//import static neo.Renderer.QGL.qGL_FALSE;
//import static neo.Renderer.QGL.qGL_TRUE;
//import static neo.Renderer.QGL.qglActiveTextureARB;
//import static neo.Renderer.QGL.qglBindProgramARB;
//import static neo.Renderer.QGL.qglCallList;
//import static neo.Renderer.QGL.qglClear;
//import static neo.Renderer.QGL.qglColor3f;
//import static neo.Renderer.QGL.qglColorPointer;
//import static neo.Renderer.QGL.qglCombinerInputNV;
//import static neo.Renderer.QGL.qglCombinerOutputNV;
//import static neo.Renderer.QGL.qglCombinerParameterfvNV;
//import static neo.Renderer.QGL.qglCombinerParameteriNV;
//import static neo.Renderer.QGL.qglDisable;
//import static neo.Renderer.QGL.qglDisableClientState;
//import static neo.Renderer.QGL.qglDisableVertexAttribArrayARB;
//import static neo.Renderer.QGL.qglEnable;
//import static neo.Renderer.QGL.qglEnableClientState;
//import static neo.Renderer.QGL.qglEnableVertexAttribArrayARB;
//import static neo.Renderer.QGL.qglEndList;
//import static neo.Renderer.QGL.qglFinalCombinerInputNV;
//import static neo.Renderer.QGL.qglGenLists;
//import static neo.Renderer.QGL.qglNewList;
//import static neo.Renderer.QGL.qglProgramEnvParameter4fvARB;
//import static neo.Renderer.QGL.qglScissor;
//import static neo.Renderer.QGL.qglStencilFunc;
//import static neo.Renderer.QGL.qglTexCoordPointer;
//import static neo.Renderer.QGL.qglVertexAttribPointerARB;
//import static neo.Renderer.QGL.qglVertexPointer;
//import static neo.Renderer.tr_backend.GL_SelectTexture;
//import static neo.Renderer.tr_backend.GL_State;
//import static neo.Renderer.tr_backend.RB_LogComment;
//import static neo.Renderer.tr_local.GLS_ALPHAMASK;
//import static neo.Renderer.tr_local.GLS_COLORMASK;
//import static neo.Renderer.tr_local.GLS_DEPTHFUNC_EQUAL;
//import static neo.Renderer.tr_local.GLS_DEPTHFUNC_LESS;
//import static neo.Renderer.tr_local.GLS_DEPTHMASK;
//import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE;
//import static neo.Renderer.tr_local.GLS_SRCBLEND_DST_ALPHA;
//import static neo.Renderer.tr_local.backEnd;
//import neo.Renderer.tr_local.drawInteraction_t;
//import neo.Renderer.tr_local.drawSurf_s;
//import static neo.Renderer.tr_local.glConfig;
//import static neo.Renderer.tr_local.programParameter_t.PP_BUMP_MATRIX_S;
//import static neo.Renderer.tr_local.programParameter_t.PP_BUMP_MATRIX_T;
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
//import static neo.Renderer.tr_local.program_t.VPROG_NV20_BUMP_AND_LIGHT;
//import static neo.Renderer.tr_local.program_t.VPROG_NV20_DIFFUSE_AND_SPECULAR_COLOR;
//import static neo.Renderer.tr_local.program_t.VPROG_NV20_DIFFUSE_COLOR;
//import static neo.Renderer.tr_local.program_t.VPROG_NV20_SPECULAR_COLOR;
//import static neo.Renderer.tr_local.program_t.VPROG_STENCIL_SHADOW;
//import neo.Renderer.tr_local.viewLight_s;
//import neo.Renderer.tr_render.DrawInteraction;
//import static neo.Renderer.tr_render.RB_CreateSingleDrawInteractions;
//import static neo.Renderer.tr_render.RB_DrawElementsWithCounters;
//import static neo.TempDump.NOT;
//import static neo.framework.BuildDefines.MACOS_X;
//import static neo.framework.Common.common;
//import neo.idlib.geometry.DrawVert.idDrawVert;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE0_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE1_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE2_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE3_ARB;
//import static org.lwjgl.opengl.ARBVertexProgram.GL_VERTEX_PROGRAM_ARB;
//import static org.lwjgl.opengl.GL11.GL_ALPHA;
//import static org.lwjgl.opengl.GL11.GL_ALWAYS;
//import static org.lwjgl.opengl.GL11.GL_BLUE;
//import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
//import static org.lwjgl.opengl.GL11.GL_COMPILE;
//import static org.lwjgl.opengl.GL11.GL_FLOAT;
//import static org.lwjgl.opengl.GL11.GL_NONE;
//import static org.lwjgl.opengl.GL11.GL_RGB;
//import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
//import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;
//import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
//import static org.lwjgl.opengl.GL11.GL_ZERO;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_BIAS_BY_NEGATIVE_ONE_HALF_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_COMBINER0_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_COMBINER1_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_COMBINER2_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_COMBINER3_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_CONSTANT_COLOR0_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_CONSTANT_COLOR1_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_DISCARD_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_EXPAND_NORMAL_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_E_TIMES_F_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_NUM_GENERAL_COMBINERS_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_PRIMARY_COLOR_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_REGISTER_COMBINERS_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_SCALE_BY_TWO_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_SECONDARY_COLOR_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_SPARE0_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_SPARE1_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_UNSIGNED_IDENTITY_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_UNSIGNED_INVERT_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_A_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_B_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_C_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_D_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_E_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_F_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_G_NV;

/**
 *
 */
public class draw_nv20 {

//    enum fragmentProgram_t {
//
//        FPROG_BUMP_AND_LIGHT,
//        FPROG_DIFFUSE_COLOR,
//        FPROG_SPECULAR_COLOR,
//        FPROG_DIFFUSE_AND_SPECULAR_COLOR,
//        FPROG_NUM_FRAGMENT_PROGRAMS
//    };
//    static int /*GLuint*/ fragmentDisplayListBase;	// FPROG_NUM_FRAGMENT_PROGRAMS lists
//
//// void RB_NV20_DependentSpecularPass( const drawInteraction_t *din );
//// void RB_NV20_DependentAmbientPass( void );
//
//    /*
//     =========================================================================================
//
//     GENERAL INTERACTION RENDERING
//
//     =========================================================================================
//     */
//
//    /*
//     ====================
//     GL_SelectTextureNoClient
//     ====================
//     */
//    public static void GL_SelectTextureNoClient(int unit) {
//        backEnd.glState.currenttmu = unit;
//        qglActiveTextureARB(GL_TEXTURE0_ARB + unit);
//        RB_LogComment("glActiveTextureARB( %d )\n", unit);
//    }
//
//    /*
//     ==================
//     RB_NV20_BumpAndLightFragment
//     ==================
//     */
//    public static void RB_NV20_BumpAndLightFragment() {
//        if (RenderSystem_init.r_useCombinerDisplayLists.GetBool()) {
//            qglCallList(fragmentDisplayListBase + FPROG_BUMP_AND_LIGHT.ordinal());
//            return;
//        }
//
//        // program the nvidia register combiners
//        qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 3);
//
//        // stage 0 rgb performs the dot product
//        // SPARE0 = TEXTURE0 dot TEXTURE1
//        qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_TEXTURE1_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_TEXTURE0_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER0_NV, GL_RGB,
//                GL_SPARE0_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_NONE, GL_NONE, qGL_TRUE, qGL_FALSE, qGL_FALSE);
//
//        // stage 1 rgb multiplies texture 2 and 3 together
//        // SPARE1 = TEXTURE2 * TEXTURE3
//        qglCombinerInputNV(GL_COMBINER1_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_TEXTURE2_ARB, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER1_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_TEXTURE3_ARB, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER1_NV, GL_RGB,
//                GL_SPARE1_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // stage 1 alpha does nohing
//        // stage 2 color multiplies spare0 * spare 1 just for debugging
//        // SPARE0 = SPARE0 * SPARE1
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_SPARE0_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_SPARE1_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER2_NV, GL_RGB,
//                GL_SPARE0_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // stage 2 alpha multiples spare0 * spare 1
//        // SPARE0 = SPARE0 * SPARE1
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_ALPHA, GL_VARIABLE_A_NV,
//                GL_SPARE0_NV, GL_UNSIGNED_IDENTITY_NV, GL_BLUE);
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_ALPHA, GL_VARIABLE_B_NV,
//                GL_SPARE1_NV, GL_UNSIGNED_IDENTITY_NV, GL_BLUE);
//        qglCombinerOutputNV(GL_COMBINER2_NV, GL_ALPHA,
//                GL_SPARE0_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // final combiner
//        qglFinalCombinerInputNV(GL_VARIABLE_D_NV, GL_SPARE0_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_A_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_B_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_C_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_SPARE0_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//    }
//
//    /*
//     ==================
//     RB_NV20_DI_BumpAndLightPass
//
//     We are going to write alpha as light falloff * ( bump dot light ) * lightProjection
//     If the light isn't a monoLightShader, the lightProjection will be skipped, because
//     it will have to be done on an itterated basis
//     ==================
//     */
//    public static void RB_NV20_DI_BumpAndLightPass(final drawInteraction_t din, boolean monoLightShader) {
//        RB_LogComment("---------- RB_NV_BumpAndLightPass ----------\n");
//
//        GL_State(GLS_COLORMASK | GLS_DEPTHMASK | backEnd.depthFunc);
//
//        // texture 0 is the normalization cube map
//        // GL_TEXTURE0_ARB will be the normalized vector
//        // towards the light source
//        if (MACOS_X) {
//            GL_SelectTexture(0);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(0);
//        }
//        if (din.ambientLight != 0) {
//            globalImages.ambientNormalMap.Bind();
//        } else {
//            globalImages.normalCubeMapImage.Bind();
//        }
//
//        // texture 1 will be the per-surface bump map
//        if (MACOS_X) {
//            GL_SelectTexture(1);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(1);
//        }
//        din.bumpImage.Bind();
//
//        // texture 2 will be the light falloff texture
//        if (MACOS_X) {
//            GL_SelectTexture(2);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(2);
//        }
//        din.lightFalloffImage.Bind();
//
//        // texture 3 will be the light projection texture
//        if (MACOS_X) {
//            GL_SelectTexture(3);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(3);
//        }
//        if (monoLightShader) {
//            din.lightImage.Bind();
//        } else {
//            // if the projected texture is multi-colored, we
//            // will need to do it in subsequent passes
//            globalImages.whiteImage.Bind();
//        }
//
//        // bind our "fragment program"
//        RB_NV20_BumpAndLightFragment();
//
//        // draw it
//        qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_NV20_BUMP_AND_LIGHT);
//        RB_DrawElementsWithCounters(din.surf.geo);
//    }
//
//
//    /*
//     ==================
//     RB_NV20_DiffuseColorFragment
//     ==================
//     */
//    public static void RB_NV20_DiffuseColorFragment() {
//        if (RenderSystem_init.r_useCombinerDisplayLists.GetBool()) {
//            qglCallList(fragmentDisplayListBase + FPROG_DIFFUSE_COLOR.ordinal());
//            return;
//        }
//
//        // program the nvidia register combiners
//        qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 1);
//
//        // stage 0 is free, so we always do the multiply of the vertex color
//        // when the vertex color is inverted, qglCombinerInputNV(GL_VARIABLE_B_NV) will be changed
//        qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_TEXTURE0_ARB, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_PRIMARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER0_NV, GL_RGB,
//                GL_TEXTURE0_ARB, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        qglCombinerOutputNV(GL_COMBINER0_NV, GL_ALPHA,
//                GL_DISCARD_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // for GL_CONSTANT_COLOR0_NV * TEXTURE0 * TEXTURE1
//        qglFinalCombinerInputNV(GL_VARIABLE_A_NV, GL_CONSTANT_COLOR0_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_B_NV, GL_E_TIMES_F_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_C_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_D_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_E_NV, GL_TEXTURE0_ARB,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_F_NV, GL_TEXTURE1_ARB,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//
//    }
//
//    /*
//     ==================
//     RB_NV20_DI_DiffuseColorPass
//
//     ==================
//     */
//    public static void RB_NV20_DI_DiffuseColorPass(final drawInteraction_t din) {
//        RB_LogComment("---------- RB_NV20_DiffuseColorPass ----------\n");
//
//        GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ONE | GLS_DEPTHMASK | GLS_ALPHAMASK
//                | backEnd.depthFunc);
//
//        // texture 0 will be the per-surface diffuse map
//        if (MACOS_X) {
//            GL_SelectTexture(0);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(0);
//        }
//        din.diffuseImage.Bind();
//
//        // texture 1 will be the light projected texture
//        if (MACOS_X) {
//            GL_SelectTexture(1);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(1);
//        }
//        din.lightImage.Bind();
//
//        // texture 2 is disabled
//        if (MACOS_X) {
//            GL_SelectTexture(2);
//            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(2);
//        }
//        globalImages.BindNull();
//
//        // texture 3 is disabled
//        if (MACOS_X) {
//            GL_SelectTexture(3);
//            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(3);
//        }
//        globalImages.BindNull();
//
//        // bind our "fragment program"
//        RB_NV20_DiffuseColorFragment();
//
//        // override one parameter for inverted vertex color
//        if (din.vertexColor == SVC_INVERSE_MODULATE) {
//            qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                    GL_PRIMARY_COLOR_NV, GL_UNSIGNED_INVERT_NV, GL_RGB);
//        }
//
//        // draw it
//        qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_NV20_DIFFUSE_COLOR);
//        RB_DrawElementsWithCounters(din.surf.geo);
//    }
//
//
//    /*
//     ==================
//     RB_NV20_SpecularColorFragment
//     ==================
//     */
//    public static void RB_NV20_SpecularColorFragment() {
//        if (RenderSystem_init.r_useCombinerDisplayLists.GetBool()) {
//            qglCallList(fragmentDisplayListBase + FPROG_SPECULAR_COLOR.ordinal());
//            return;
//        }
//
//        // program the nvidia register combiners
//        qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 4);
//
//        // we want GL_CONSTANT_COLOR1_NV * PRIMARY_COLOR * TEXTURE2 * TEXTURE3 * specular( TEXTURE0 * TEXTURE1 )
//        // stage 0 rgb performs the dot product
//        // GL_SPARE0_NV = ( TEXTURE0 dot TEXTURE1 - 0.5 ) * 2
//        // TEXTURE2 = TEXTURE2 * PRIMARY_COLOR
//        // the scale and bias steepen the specular curve
//        qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_TEXTURE1_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_TEXTURE0_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER0_NV, GL_RGB,
//                GL_SPARE0_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_SCALE_BY_TWO_NV, GL_BIAS_BY_NEGATIVE_ONE_HALF_NV, qGL_TRUE, qGL_FALSE, qGL_FALSE);
//
//        // stage 0 alpha does nothing
//        // stage 1 color takes bump * bump
//        // GL_SPARE0_NV = ( GL_SPARE0_NV * GL_SPARE0_NV - 0.5 ) * 2
//        // the scale and bias steepen the specular curve
//        qglCombinerInputNV(GL_COMBINER1_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_SPARE0_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER1_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_SPARE0_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER1_NV, GL_RGB,
//                GL_SPARE0_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_SCALE_BY_TWO_NV, GL_BIAS_BY_NEGATIVE_ONE_HALF_NV, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // stage 1 alpha does nothing
//        // stage 2 color
//        // GL_SPARE0_NV = GL_SPARE0_NV * TEXTURE3
//        // SECONDARY_COLOR = CONSTANT_COLOR * TEXTURE2
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_SPARE0_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_TEXTURE3_ARB, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_C_NV,
//                GL_CONSTANT_COLOR1_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_D_NV,
//                GL_TEXTURE2_ARB, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER2_NV, GL_RGB,
//                GL_SPARE0_NV, GL_SECONDARY_COLOR_NV, GL_DISCARD_NV,
//                GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // stage 2 alpha does nothing
//        // stage 3 scales the texture by the vertex color
//        qglCombinerInputNV(GL_COMBINER3_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_SECONDARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER3_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_PRIMARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER3_NV, GL_RGB,
//                GL_SECONDARY_COLOR_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // stage 3 alpha does nothing
//        // final combiner = GL_SPARE0_NV * SECONDARY_COLOR + PRIMARY_COLOR * SECONDARY_COLOR
//        qglFinalCombinerInputNV(GL_VARIABLE_A_NV, GL_SPARE0_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_B_NV, GL_SECONDARY_COLOR_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_C_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_D_NV, GL_E_TIMES_F_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_E_NV, GL_SPARE0_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_F_NV, GL_SECONDARY_COLOR_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//    }
//
//
//    /*
//     ==================
//     RB_NV20_DI_SpecularColorPass
//
//     ==================
//     */
//    public static void RB_NV20_DI_SpecularColorPass(final drawInteraction_t din) {
//        RB_LogComment("---------- RB_NV20_SpecularColorPass ----------\n");
//
//        GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ONE | GLS_DEPTHMASK | GLS_ALPHAMASK
//                | backEnd.depthFunc);
//
//        // texture 0 is the normalization cube map for the half angle
//        if (MACOS_X) {
//            GL_SelectTexture(0);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(0);
//        }
//        globalImages.normalCubeMapImage.Bind();
//
//        // texture 1 will be the per-surface bump map
//        if (MACOS_X) {
//            GL_SelectTexture(1);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(1);
//        }
//        din.bumpImage.Bind();
//
//        // texture 2 will be the per-surface specular map
//        if (MACOS_X) {
//            GL_SelectTexture(2);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(2);
//        }
//        din.specularImage.Bind();
//
//        // texture 3 will be the light projected texture
//        if (MACOS_X) {
//            GL_SelectTexture(3);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(3);
//        }
//        din.lightImage.Bind();
//
//        // bind our "fragment program"
//        RB_NV20_SpecularColorFragment();
//
//        // override one parameter for inverted vertex color
//        if (din.vertexColor == SVC_INVERSE_MODULATE) {
//            qglCombinerInputNV(GL_COMBINER3_NV, GL_RGB, GL_VARIABLE_B_NV,
//                    GL_PRIMARY_COLOR_NV, GL_UNSIGNED_INVERT_NV, GL_RGB);
//        }
//
//        // draw it
//        qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_NV20_SPECULAR_COLOR);
//        RB_DrawElementsWithCounters(din.surf.geo);
//    }
//
//    /*
//     ==================
//     RB_NV20_DiffuseAndSpecularColorFragment
//     ==================
//     */
//    public static void RB_NV20_DiffuseAndSpecularColorFragment() {
//        if (RenderSystem_init.r_useCombinerDisplayLists.GetBool()) {
//            qglCallList(fragmentDisplayListBase + FPROG_DIFFUSE_AND_SPECULAR_COLOR.ordinal());
//            return;
//        }
//
//        // program the nvidia register combiners
//        qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 3);
//
//        // GL_CONSTANT_COLOR0_NV will be the diffuse color
//        // GL_CONSTANT_COLOR1_NV will be the specular color
//        // stage 0 rgb performs the dot product
//        // GL_SECONDARY_COLOR_NV = ( TEXTURE0 dot TEXTURE1 - 0.5 ) * 2
//        // the scale and bias steepen the specular curve
//        qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_TEXTURE1_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_TEXTURE0_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER0_NV, GL_RGB,
//                GL_SECONDARY_COLOR_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_SCALE_BY_TWO_NV, GL_BIAS_BY_NEGATIVE_ONE_HALF_NV, qGL_TRUE, qGL_FALSE, qGL_FALSE);
//
//        // stage 0 alpha does nothing
//        // stage 1 color takes bump * bump
//        // PRIMARY_COLOR = ( GL_SECONDARY_COLOR_NV * GL_SECONDARY_COLOR_NV - 0.5 ) * 2
//        // the scale and bias steepen the specular curve
//        qglCombinerInputNV(GL_COMBINER1_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_SECONDARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER1_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_SECONDARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER1_NV, GL_RGB,
//                GL_SECONDARY_COLOR_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                GL_SCALE_BY_TWO_NV, GL_BIAS_BY_NEGATIVE_ONE_HALF_NV, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // stage 1 alpha does nothing
//        // stage 2 color
//        // PRIMARY_COLOR = ( PRIMARY_COLOR * TEXTURE3 ) * 2
//        // SPARE0 = 1.0 * 1.0 (needed for final combiner)
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_A_NV,
//                GL_SECONDARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_B_NV,
//                GL_TEXTURE3_ARB, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_C_NV,
//                GL_ZERO, GL_UNSIGNED_INVERT_NV, GL_RGB);
//        qglCombinerInputNV(GL_COMBINER2_NV, GL_RGB, GL_VARIABLE_D_NV,
//                GL_ZERO, GL_UNSIGNED_INVERT_NV, GL_RGB);
//        qglCombinerOutputNV(GL_COMBINER2_NV, GL_RGB,
//                GL_SECONDARY_COLOR_NV, GL_SPARE0_NV, GL_DISCARD_NV,
//                GL_SCALE_BY_TWO_NV, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//        // stage 2 alpha does nothing
//        // final combiner = TEXTURE2_ARB * CONSTANT_COLOR0_NV + PRIMARY_COLOR_NV * CONSTANT_COLOR1_NV
//        // alpha = GL_ZERO
//        qglFinalCombinerInputNV(GL_VARIABLE_A_NV, GL_CONSTANT_COLOR1_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_B_NV, GL_SECONDARY_COLOR_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_C_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_D_NV, GL_E_TIMES_F_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_E_NV, GL_TEXTURE2_ARB,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_F_NV, GL_CONSTANT_COLOR0_NV,
//                GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//        qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_ZERO,
//                GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//    }
//
//
//    /*
//     ==================
//     RB_NV20_DI_DiffuseAndSpecularColorPass
//
//     ==================
//     */
//    public static void RB_NV20_DI_DiffuseAndSpecularColorPass(final drawInteraction_t din) {
//        RB_LogComment("---------- RB_NV20_DI_DiffuseAndSpecularColorPass ----------\n");
//
//        GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ONE | GLS_DEPTHMASK | backEnd.depthFunc);
//
//        // texture 0 is the normalization cube map for the half angle
//// still bound from RB_NV_BumpAndLightPass
////	GL_SelectTextureNoClient( 0 );
////	GL_Bind( tr.normalCubeMapImage );
//        // texture 1 is the per-surface bump map
//// still bound from RB_NV_BumpAndLightPass
////	GL_SelectTextureNoClient( 1 );
////	GL_Bind( din.bumpImage );
//        // texture 2 is the per-surface diffuse map
//        if (MACOS_X) {
//            GL_SelectTexture(2);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(2);
//        }
//        din.diffuseImage.Bind();
//
//        // texture 3 is the per-surface specular map
//        if (MACOS_X) {
//            GL_SelectTexture(3);
//            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(3);
//        }
//        din.specularImage.Bind();
//
//        // bind our "fragment program"
//        RB_NV20_DiffuseAndSpecularColorFragment();
//
//        // draw it
//        qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_NV20_DIFFUSE_AND_SPECULAR_COLOR);
//        RB_DrawElementsWithCounters(din.surf.geo);
//    }
//
//
//    /*
//     ==================
//     RB_NV20_DrawInteraction
//     ==================
//     */
//    public static class RB_NV20_DrawInteraction extends DrawInteraction {
//
//        static final DrawInteraction INSTANCE = new RB_NV20_DrawInteraction();
//
//        private RB_NV20_DrawInteraction() {
//        }
//
//        @Override
//        void run(final drawInteraction_t din) {
//            drawSurf_s surf = din.surf;
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
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_SPECULAR_MATRIX_S, din.specularMatrix[0].ToFloatPtr());
//            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_SPECULAR_MATRIX_T, din.specularMatrix[1].ToFloatPtr());
//
//            // set the constant colors
//            qglCombinerParameterfvNV(GL_CONSTANT_COLOR0_NV, din.diffuseColor.ToFloatPtr());
//            qglCombinerParameterfvNV(GL_CONSTANT_COLOR1_NV, din.specularColor.ToFloatPtr());
//
//            // vertex color passes should be pretty rare (cross-faded bump map surfaces), so always
//            // run them down as three-passes
//            if (din.vertexColor != SVC_IGNORE) {
//                qglEnableClientState(GL_COLOR_ARRAY);
//                RB_NV20_DI_BumpAndLightPass(din, false);
//                RB_NV20_DI_DiffuseColorPass(din);
//                RB_NV20_DI_SpecularColorPass(din);
//                qglDisableClientState(GL_COLOR_ARRAY);
//                return;
//            }
//
//            qglColor3f(1, 1, 1);
//
//            // on an ideal card, we would now just bind the textures and call a
//            // single pass vertex / fragment program, but
//            // on NV20, we need to decide which single / dual / tripple pass set of programs to use
//            // ambient light could be done as a single pass if we want to optimize for it
//            // monochrome light is two passes
//            int internalFormat = din.lightImage.internalFormat;
//            if ((RenderSystem_init.r_useNV20MonoLights.GetInteger() == 2)
//                    || (din.lightImage.isMonochrome != null && RenderSystem_init.r_useNV20MonoLights.GetInteger() != 0)) {
//                // do a two-pass rendering
//                RB_NV20_DI_BumpAndLightPass(din, true);
//                RB_NV20_DI_DiffuseAndSpecularColorPass(din);
//            } else {
//                // general case is three passes
//                // ( bump dot lightDir ) * lightFalloff
//                // diffuse * lightProject
//                // specular * ( bump dot halfAngle extended ) * lightProject
//                RB_NV20_DI_BumpAndLightPass(din, false);
//                RB_NV20_DI_DiffuseColorPass(din);
//                RB_NV20_DI_SpecularColorPass(din);
//            }
//        }
//    };
//
//
//    /*
//     =============
//     RB_NV20_CreateDrawInteractions
//
//     =============
//     */
//    public static void RB_NV20_CreateDrawInteractions(final drawSurf_s surf) {
//        if (NOT(surf)) {
//            return;
//        }
//
//        qglEnable(GL_VERTEX_PROGRAM_ARB);
//        qglEnable(GL_REGISTER_COMBINERS_NV);
//
//        if (MACOS_X) {
//            GL_SelectTexture(0);
//            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//
//            qglEnableVertexAttribArrayARB(8);
//            qglEnableVertexAttribArrayARB(9);
//            qglEnableVertexAttribArrayARB(10);
//            qglEnableVertexAttribArrayARB(11);
//        }
//
//        for (; surf != null; surf.oSet(surf.nextOnLight)) {
//            // set the vertex pointers
//            idDrawVert ac = new idDrawVert(vertexCache.Position(surf.geo.ambientCache));//TODO:figure out how to work these damn casts.
//            qglColorPointer(4, GL_UNSIGNED_BYTE, 0/*sizeof(idDrawVert)*/, ac.color);
//            if (MACOS_X) {
//                GL_SelectTexture(0);
//                qglTexCoordPointer(2, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.st.ToFloatPtr());
//                GL_SelectTexture(1);
//                qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.tangents[0].ToFloatPtr());
//                GL_SelectTexture(2);
//                qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.tangents[1].ToFloatPtr());
//                GL_SelectTexture(3);
//                qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.normal.ToFloatPtr());
//                GL_SelectTexture(0);
//            } else {
//                qglVertexAttribPointerARB(11, 3, GL_FLOAT, false, 0/*sizeof(idDrawVert)*/, ac.normal.ToFloatPtr());
//                qglVertexAttribPointerARB(10, 3, GL_FLOAT, false, 0/*sizeof(idDrawVert)*/, ac.tangents[1].ToFloatPtr());
//                qglVertexAttribPointerARB(9, 3, GL_FLOAT, false, 0/*sizeof(idDrawVert)*/, ac.tangents[0].ToFloatPtr());
//                qglVertexAttribPointerARB(8, 2, GL_FLOAT, false, 0/*sizeof(idDrawVert)*/, ac.st.ToFloatPtr());
//            }
//            qglVertexPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.xyz.ToFloatPtr());
//
//            RB_CreateSingleDrawInteractions(surf, RB_NV20_DrawInteraction.INSTANCE);
//        }
//
//        if (MACOS_X) {
//            qglDisableVertexAttribArrayARB(8);
//            qglDisableVertexAttribArrayARB(9);
//            qglDisableVertexAttribArrayARB(10);
//            qglDisableVertexAttribArrayARB(11);
//        }
//
//        // disable features
//        if (MACOS_X) {
//            GL_SelectTexture(3);
//            globalImages.BindNull();
//            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//
//            GL_SelectTexture(2);
//            globalImages.BindNull();
//            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//
//            GL_SelectTexture(1);
//            globalImages.BindNull();
//            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//        } else {
//            GL_SelectTextureNoClient(3);
//            globalImages.BindNull();
//
//            GL_SelectTextureNoClient(2);
//            globalImages.BindNull();
//
//            GL_SelectTextureNoClient(1);
//            globalImages.BindNull();
//        }
//
//        backEnd.glState.currenttmu = -1;
//        GL_SelectTexture(0);
//
//        qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//
//        qglDisable(GL_VERTEX_PROGRAM_ARB);
//        qglDisable(GL_REGISTER_COMBINERS_NV);
//    }
//
////======================================================================================
//    /*
//     ==================
//     RB_NV20_DrawInteractions
//     ==================
//     */
//    public static void RB_NV20_DrawInteractions() {
//        viewLight_s vLight;
//
//        //
//        // for each light, perform adding and shadowing
//        //
//        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
//            // do fogging later
//            if (vLight.lightShader.IsFogLight()) {
//                continue;
//            }
//            if (vLight.lightShader.IsBlendLight()) {
//                continue;
//            }
//            if (NOT(vLight.localInteractions[0]) && NOT(vLight.globalInteractions[0])
//                    && NOT(vLight.translucentInteractions[0])) {
//                continue;
//            }
//
//            backEnd.vLight = vLight;
//
//            RB_LogComment("---------- RB_RenderViewLight 0x%p ----------\n", vLight);
//
//            // clear the stencil buffer if needed
//            if (vLight.globalShadows [0]!= null || vLight.localShadows[0] != null) {
//                backEnd.currentScissor = vLight.scissorRect;
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
//            backEnd.depthFunc = GLS_DEPTHFUNC_EQUAL;
//
//            if (RenderSystem_init.r_useShadowVertexProgram.GetBool()) {
//                qglEnable(GL_VERTEX_PROGRAM_ARB);
//                qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_STENCIL_SHADOW);
//                RB_StencilShadowPass(vLight.globalShadows[0]);
//                RB_NV20_CreateDrawInteractions(vLight.localInteractions[0]);
//                qglEnable(GL_VERTEX_PROGRAM_ARB);
//                qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_STENCIL_SHADOW);
//                RB_StencilShadowPass(vLight.localShadows[0]);
//                RB_NV20_CreateDrawInteractions(vLight.globalInteractions[0]);
//                qglDisable(GL_VERTEX_PROGRAM_ARB);	// if there weren't any globalInteractions, it would have stayed on
//            } else {
//                RB_StencilShadowPass(vLight.globalShadows[0]);
//                RB_NV20_CreateDrawInteractions(vLight.localInteractions[0]);
//                RB_StencilShadowPass(vLight.localShadows[0]);
//                RB_NV20_CreateDrawInteractions(vLight.globalInteractions[0]);
//            }
//
//            // translucent surfaces never get stencil shadowed
//            if (RenderSystem_init.r_skipTranslucent.GetBool()) {
//                continue;
//            }
//
//            qglStencilFunc(GL_ALWAYS, 128, 255);
//
//            backEnd.depthFunc = GLS_DEPTHFUNC_LESS;
//            RB_NV20_CreateDrawInteractions(vLight.translucentInteractions[0]);
//
//            backEnd.depthFunc = GLS_DEPTHFUNC_EQUAL;
//        }
//    }
//
////=======================================================================
//
//    /*
//     ==================
//     R_NV20_Init
//
//     ==================
//     */
//    public static void R_NV20_Init() {
//        glConfig.allowNV20Path = false;
//
//        common.Printf("---------- R_NV20_Init ----------\n");
//
//        if (!glConfig.registerCombinersAvailable || !glConfig.ARBVertexProgramAvailable || glConfig.maxTextureUnits < 4) {
//            common.Printf("Not available.\n");
//            return;
//        }
//
//        GL_CheckErrors();
//
//        // create our "fragment program" display lists
//        fragmentDisplayListBase = qglGenLists(FPROG_NUM_FRAGMENT_PROGRAMS);
//
//        // force them to issue commands to build the list
//        boolean temp = RenderSystem_init.r_useCombinerDisplayLists.GetBool();
//        RenderSystem_init.r_useCombinerDisplayLists.SetBool(false);
//
//        qglNewList(fragmentDisplayListBase + FPROG_BUMP_AND_LIGHT.ordinal(), GL_COMPILE);
//        RB_NV20_BumpAndLightFragment();
//        qglEndList();
//
//        qglNewList(fragmentDisplayListBase + FPROG_DIFFUSE_COLOR.ordinal(), GL_COMPILE);
//        RB_NV20_DiffuseColorFragment();
//        qglEndList();
//
//        qglNewList(fragmentDisplayListBase + FPROG_SPECULAR_COLOR.ordinal(), GL_COMPILE);
//        RB_NV20_SpecularColorFragment();
//        qglEndList();
//
//        qglNewList(fragmentDisplayListBase + FPROG_DIFFUSE_AND_SPECULAR_COLOR.ordinal(), GL_COMPILE);
//        RB_NV20_DiffuseAndSpecularColorFragment();
//        qglEndList();
//
//        RenderSystem_init.r_useCombinerDisplayLists.SetBool(temp);
//
//        common.Printf("---------------------------------\n");
//
//        glConfig.allowNV20Path = true;
//    }
}
