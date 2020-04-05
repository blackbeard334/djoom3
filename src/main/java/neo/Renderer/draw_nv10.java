package neo.Renderer;

//import static neo.Renderer.Image.globalImages;
//import neo.Renderer.Material.idMaterial;
//import neo.Renderer.Material.shaderStage_t;
//import static neo.Renderer.Material.stageLighting_t.SL_AMBIENT;
//import static neo.Renderer.Material.stageLighting_t.SL_BUMP;
//import static neo.Renderer.Material.stageLighting_t.SL_DIFFUSE;
//import static neo.Renderer.Material.stageLighting_t.SL_SPECULAR;
//import static neo.Renderer.Material.stageVertexColor_t.SVC_IGNORE;
//import static neo.Renderer.Material.stageVertexColor_t.SVC_INVERSE_MODULATE;
//import static neo.Renderer.Material.stageVertexColor_t.SVC_MODULATE;
//import neo.Renderer.Model.lightingCache_s;
//import neo.Renderer.Model.srfTriangles_s;
//import static neo.Renderer.VertexCache.vertexCache;
//import static neo.Renderer.draw_common.RB_StencilShadowPass;
//import static neo.Renderer.QGL.qGL_FALSE;
//import static neo.Renderer.QGL.qGL_TRUE;
//import static neo.Renderer.QGL.qglClear;
//import static neo.Renderer.QGL.qglColorPointer;
//import static neo.Renderer.QGL.qglCombinerInputNV;
//import static neo.Renderer.QGL.qglCombinerOutputNV;
//import static neo.Renderer.QGL.qglCombinerParameterfvNV;
//import static neo.Renderer.QGL.qglCombinerParameteriNV;
//import static neo.Renderer.QGL.qglDisable;
//import static neo.Renderer.QGL.qglDisableClientState;
//import static neo.Renderer.QGL.qglEnable;
//import static neo.Renderer.QGL.qglEnableClientState;
//import static neo.Renderer.QGL.qglFinalCombinerInputNV;
//import static neo.Renderer.QGL.qglLoadMatrixf;
//import static neo.Renderer.QGL.qglScissor;
//import static neo.Renderer.QGL.qglStencilFunc;
//import static neo.Renderer.QGL.qglTexCoord2f;
//import static neo.Renderer.QGL.qglTexCoordPointer;
//import static neo.Renderer.QGL.qglTexGenfv;
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
//import static neo.Renderer.tr_local.GLS_DSTBLEND_SRC_ALPHA;
//import static neo.Renderer.tr_local.GLS_DSTBLEND_ZERO;
//import static neo.Renderer.tr_local.GLS_SRCBLEND_DST_ALPHA;
//import static neo.Renderer.tr_local.backEnd;
//import neo.Renderer.tr_local.drawSurf_s;
//import static neo.Renderer.tr_local.glConfig;
//import neo.Renderer.tr_local.idScreenRect;
//import static neo.Renderer.tr_local.tr;
//import neo.Renderer.tr_local.viewLight_s;
//import static neo.Renderer.tr_main.R_GlobalPlaneToLocal;
//import static neo.Renderer.tr_render.RB_BindStageTexture;
//import static neo.Renderer.tr_render.RB_DrawElementsWithCounters;
//import static neo.Renderer.tr_render.RB_EnterModelDepthHack;
//import static neo.Renderer.tr_render.RB_EnterWeaponDepthHack;
//import static neo.Renderer.tr_render.RB_FinishStageTexture;
//import static neo.Renderer.tr_render.RB_LeaveDepthHack;
//import static neo.TempDump.NOT;
//import static neo.framework.Common.common;
//import neo.idlib.geometry.DrawVert.idDrawVert;
//import neo.idlib.math.Plane.idPlane;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE0_ARB;
//import static org.lwjgl.opengl.ARBMultitexture.GL_TEXTURE1_ARB;
//import static org.lwjgl.opengl.GL11.GL_ALPHA;
//import static org.lwjgl.opengl.GL11.GL_ALWAYS;
//import static org.lwjgl.opengl.GL11.GL_BLUE;
//import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
//import static org.lwjgl.opengl.GL11.GL_FLOAT;
//import static org.lwjgl.opengl.GL11.GL_NONE;
//import static org.lwjgl.opengl.GL11.GL_OBJECT_PLANE;
//import static org.lwjgl.opengl.GL11.GL_Q;
//import static org.lwjgl.opengl.GL11.GL_RGB;
//import static org.lwjgl.opengl.GL11.GL_S;
//import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
//import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
//import static org.lwjgl.opengl.GL11.GL_T;
//import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;
//import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_Q;
//import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_S;
//import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_T;
//import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
//import static org.lwjgl.opengl.GL11.GL_ZERO;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_BIAS_BY_NEGATIVE_ONE_HALF_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_COMBINER0_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_COMBINER1_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_CONSTANT_COLOR0_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_CONSTANT_COLOR1_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_DISCARD_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_EXPAND_NORMAL_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_NUM_GENERAL_COMBINERS_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_PRIMARY_COLOR_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_REGISTER_COMBINERS_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_SCALE_BY_TWO_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_SPARE0_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_SPARE1_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_UNSIGNED_IDENTITY_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_UNSIGNED_INVERT_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_A_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_B_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_C_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_D_NV;
//import static org.lwjgl.opengl.NVRegisterCombiners.GL_VARIABLE_G_NV;

/**
 *
 */
public class draw_nv10 {
//
//    /*
//     ==================
//     RB_RenderInteraction
//
//     backEnd.vLight
//     backEnd.lightScale
//
//
//     backEnd.depthFunc must be equal for alpha tested surfaces to work right,
//     it is set to lessThan for blended transparent surfaces
//
//     This expects a bumpmap stage before a diffuse stage before a specular stage
//     The material code is responsible for guaranteeing that, but conditional stages
//     can still make it invalid.
//
//     you can't blend two bumpmaps, but you can change bump maps between
//     blended diffuse / specular maps to get the same effect
//
//
//     ==================
//     */
//    private static final idPlane[] lightProject = new idPlane[4];	// reused across function calls
//
//    public static void RB_RenderInteraction(final drawSurf_s surf) {
//        idMaterial surfaceShader = surf.material;
//        final float[] surfaceRegs = surf.shaderRegisters;
//        viewLight_s vLight = backEnd.vLight;
//        idMaterial lightShader = vLight.lightShader;
//        final float[] lightRegs = vLight.shaderRegisters;
//        srfTriangles_s tri = surf.geo;
//        shaderStage_t lastBumpStage = null;
//
//        RB_LogComment("---------- RB_RenderInteraction %s on %s ----------\n",
//                lightShader.GetName(), surfaceShader.GetName());
//
//        // change the matrix and light projection vectors if needed
//        if (surf.space != backEnd.currentSpace) {
//            backEnd.currentSpace = surf.space;
//            qglLoadMatrixf(surf.space.modelViewMatrix);
//
//            for (int i = 0; i < 4; i++) {
//                R_GlobalPlaneToLocal(surf.space.modelMatrix, backEnd.vLight.lightProject[i], lightProject[i]);
//            }
//        }
//
//        // change the scissor if needed
//        if (RenderSystem_init.r_useScissor.GetBool() && !backEnd.currentScissor.Equals(surf.scissorRect)) {
//            backEnd.currentScissor = surf.scissorRect;
//            qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
//                    backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
//                    backEnd.currentScissor.x2 + 1 - backEnd.currentScissor.x1,
//                    backEnd.currentScissor.y2 + 1 - backEnd.currentScissor.y1);
//        }
//
//        // hack depth range if needed
//        if (surf.space.weaponDepthHack) {
//            RB_EnterWeaponDepthHack();
//        }
//
//        if (surf.space.modelDepthHack != 0.0f) {
//            RB_EnterModelDepthHack(surf.space.modelDepthHack);
//        }
//
//        // set the vertex arrays, which may not all be enabled on a given pass
//        idDrawVert ac = new idDrawVert(vertexCache.Position(tri.ambientCache));//TODO:figure out how to work these damn casts.
//        qglVertexPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.xyz.ToFloatPtr());
//        GL_SelectTexture(0);
//        qglTexCoordPointer(2, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.st.ToFloatPtr());
//        qglColorPointer(4, GL_UNSIGNED_BYTE, 0/*sizeof(idDrawVert)*/, ac.color);
//
//        // go through the individual stages
//        for (int i = 0; i < surfaceShader.GetNumStages(); i++) {
//            shaderStage_t surfaceStage = surfaceShader.GetStage(i);
//
//            // ignore ambient stages while drawing interactions
//            if (surfaceStage.lighting == SL_AMBIENT) {
//                continue;
//            }
//
//            // ignore stages that fail the condition
//            if (0 == surfaceRegs[surfaceStage.conditionRegister]) {
//                continue;
//            }
//
//            //-----------------------------------------------------
//            //
//            // bump / falloff
//            //
//            //-----------------------------------------------------
//            if (surfaceStage.lighting == SL_BUMP) {
//                // render light falloff * bumpmap lighting
//
//                if (surfaceStage.vertexColor != SVC_IGNORE) {
//                    common.Printf("shader %s: vertexColor on a bump stage\n",
//                            surfaceShader.GetName());
//                }
//
//                // check for RGBA modulations in the stage, which are also illegal?
//                // save the bump map stage for the specular calculation and diffuse
//                // error checking
//                lastBumpStage = surfaceStage;
//
//                //
//                // ambient lights combine non-directional bump and falloff
//                // and write to the alpha channel
//                //
//                if (lightShader.IsAmbientLight()) {
//                    GL_State(GLS_COLORMASK | GLS_DEPTHMASK | backEnd.depthFunc);
//
//                    // texture 0 will be the per-surface bump map
//                    GL_SelectTexture(0);
//                    qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//
//                    RB_BindStageTexture(surfaceRegs, surfaceStage.texture, surf);
//                    // development aid
//                    if (RenderSystem_init.r_skipBump.GetBool()) {
//                        globalImages.flatNormalMap.Bind();
//                    }
//
//                    // texture 1 will be the light falloff
//                    GL_SelectTexture(1);
//
//                    qglEnable(GL_TEXTURE_GEN_S);
//                    qglTexGenfv(GL_S, GL_OBJECT_PLANE, lightProject[3].ToFloatPtr());
//                    qglTexCoord2f(0, 0.5f);
//                    vLight.falloffImage.Bind();
//
//                    qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 2);
//
//                    // set the constant color to a bit of an angle
//                    qglCombinerParameterfvNV(GL_CONSTANT_COLOR0_NV, tr.ambientLightVector.ToFloatPtr());
//
//                    // stage 0 sets primary_color = bump dot constant color
//                    qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_A_NV,
//                            GL_CONSTANT_COLOR0_NV, GL_EXPAND_NORMAL_NV, GL_RGB);
//                    qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                            GL_TEXTURE0_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//                    qglCombinerOutputNV(GL_COMBINER0_NV, GL_RGB,
//                            GL_PRIMARY_COLOR_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                            GL_NONE, GL_NONE, qGL_TRUE, qGL_FALSE, qGL_FALSE);
//
//                    // stage 1 alpha sets primary_color = primary_color * falloff
//                    qglCombinerInputNV(GL_COMBINER1_NV, GL_ALPHA, GL_VARIABLE_A_NV,
//                            GL_PRIMARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_BLUE);
//                    qglCombinerInputNV(GL_COMBINER1_NV, GL_ALPHA, GL_VARIABLE_B_NV,
//                            GL_TEXTURE1_ARB, GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//                    qglCombinerOutputNV(GL_COMBINER1_NV, GL_ALPHA,
//                            GL_PRIMARY_COLOR_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                            GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//                    // final combiner takes the result for the alpha channel
//                    qglFinalCombinerInputNV(GL_VARIABLE_A_NV, GL_ZERO,
//                            GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                    qglFinalCombinerInputNV(GL_VARIABLE_B_NV, GL_ZERO,
//                            GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                    qglFinalCombinerInputNV(GL_VARIABLE_C_NV, GL_ZERO,
//                            GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                    qglFinalCombinerInputNV(GL_VARIABLE_D_NV, GL_ZERO,
//                            GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                    qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_PRIMARY_COLOR_NV,
//                            GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//
//                    // draw it
//                    RB_DrawElementsWithCounters(tri);
//
//                    globalImages.BindNull();
//                    qglDisable(GL_TEXTURE_GEN_S);
//
//                    GL_SelectTexture(0);
//                    RB_FinishStageTexture(surfaceStage.texture, surf);
//                    continue;
//                }
//
//                //
//                // draw light falloff to the alpha channel
//                //
//                GL_State(GLS_COLORMASK | GLS_DEPTHMASK | backEnd.depthFunc);
//                qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//                qglDisableClientState(GL_COLOR_ARRAY);
//                qglEnable(GL_TEXTURE_GEN_S);
//                qglTexGenfv(GL_S, GL_OBJECT_PLANE, lightProject[3].ToFloatPtr());
//                qglTexCoord2f(0, 0.5f);
//                vLight.falloffImage.Bind();
//
//                // make sure a combiner output doesn't step on the texture
//                qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 1);
//                qglCombinerOutputNV(GL_COMBINER0_NV, GL_ALPHA,
//                        GL_DISCARD_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                        GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//                // final combiner
//                qglFinalCombinerInputNV(GL_VARIABLE_A_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_B_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_C_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_D_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_TEXTURE0_ARB,
//                        GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//
//                // draw it
//                RB_DrawElementsWithCounters(tri);
//
//                qglDisable(GL_TEXTURE_GEN_S);
//
//                //
//                // draw the bump map result onto the alpha channel
//                //
//                GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ZERO | GLS_COLORMASK | GLS_DEPTHMASK
//                        | backEnd.depthFunc);
//
//                // texture 0 will be the per-surface bump map
//                GL_SelectTexture(0);
//                qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//                RB_BindStageTexture(surfaceRegs, surfaceStage.texture, surf);
//
//                // texture 1 is the normalization cube map
//                // the texccords are the non-normalized vector towards the light origin
//                GL_SelectTexture(1);
//                globalImages.normalCubeMapImage.Bind();
//                qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//
//                lightingCache_s c = new lightingCache_s(vertexCache.Position(tri.lightingCache));//TODO:figure out how to work these damn casts.
//                qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(lightingCache_s)*/, c.localLightVector.ToFloatPtr());
//
//                qglDisableClientState(GL_COLOR_ARRAY);
//
//                // program the nvidia register combiners
//                // I just want alpha = Dot( texture0, texture1 )
//                qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 1);
//
//                // stage 0 rgb performs the dot product
//                // SPARE0 = TEXTURE0 dot TEXTURE1
//                qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_A_NV,
//                        GL_TEXTURE1_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//                qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                        GL_TEXTURE0_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//                qglCombinerOutputNV(GL_COMBINER0_NV, GL_RGB,
//                        GL_SPARE0_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                        GL_NONE, GL_NONE, qGL_TRUE, qGL_FALSE, qGL_FALSE);
//
//                // final combiner just takes the dot result and puts it in alpha
//                qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_SPARE0_NV,
//                        GL_UNSIGNED_IDENTITY_NV, GL_BLUE);
//
//                // draw it
//                RB_DrawElementsWithCounters(tri);
//
//                globalImages.BindNull();
//                qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//
//                GL_SelectTexture(0);
//                RB_FinishStageTexture(surfaceStage.texture, surf);
//                continue;
//            }
//
//            if (surfaceStage.lighting == SL_DIFFUSE) {
//                if (NOT(lastBumpStage)) {
//                    common.Printf("shader %s: diffuse stage without a preceeding bumpmap stage\n",
//                            surfaceShader.GetName());
//                    continue;
//                }
//            }
//
//            //-----------------------------------------------------
//            //
//            // specular exponent modification of the bump / falloff
//            //
//            //-----------------------------------------------------
//            if (surfaceStage.lighting == SL_SPECULAR) {
//                // put specular bump map into alpha channel, then treat as a diffuse
//
//                // allow the specular to be skipped as a user speed optimization
//                if (RenderSystem_init.r_skipSpecular.GetBool()) {
//                    continue;
//                }
//
//                // ambient lights don't have specular
//                if (lightShader.IsAmbientLight()) {
//                    continue;
//                }
//
//                if (NOT(lastBumpStage)) {
//                    common.Printf("shader %s: specular stage without a preceeding bumpmap stage\n",
//                            surfaceShader.GetName());
//                    continue;
//                }
//
//                GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_SRC_ALPHA | GLS_COLORMASK | GLS_DEPTHMASK
//                        | backEnd.depthFunc);
//
//                // texture 0 will be the per-surface bump map
//                GL_SelectTexture(0);
//                qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//                RB_BindStageTexture(surfaceRegs, lastBumpStage.texture, surf);
//
//                // development aid
//                if (RenderSystem_init.r_skipBump.GetBool()) {
//                    globalImages.flatNormalMap.Bind();
//                }
//
//                // texture 1 is the normalization cube map
//                // indexed by the dynamic halfangle texcoords
//                GL_SelectTexture(1);
//                globalImages.normalCubeMapImage.Bind();
//                qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//
//                for (VertexCache.vertCache_s dynamicTexCoords : surf.dynamicTexCoords) {
//                    qglTexCoordPointer(4, GL_FLOAT, 0, vertexCache.Position(dynamicTexCoords));
//                }
//
//                // program the nvidia register combiners
//                qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 2);
//
//                // stage 0 rgb performs the dot product
//                // GL_PRIMARY_COLOR_NV = ( TEXTURE0 dot TEXTURE1 - 0.5 ) * 2
//                // the scale and bias steepen the specular curve
//                qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_A_NV,
//                        GL_TEXTURE1_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//                qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                        GL_TEXTURE0_ARB, GL_EXPAND_NORMAL_NV, GL_RGB);
//                qglCombinerOutputNV(GL_COMBINER0_NV, GL_RGB,
//                        GL_PRIMARY_COLOR_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                        GL_SCALE_BY_TWO_NV, GL_BIAS_BY_NEGATIVE_ONE_HALF_NV, qGL_TRUE, qGL_FALSE, qGL_FALSE);
//
//                // stage 0 alpha does nothing
//                qglCombinerOutputNV(GL_COMBINER0_NV, GL_ALPHA,
//                        GL_DISCARD_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                        GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//                // stage 1 rgb does nothing
//                qglCombinerOutputNV(GL_COMBINER1_NV, GL_RGB,
//                        GL_PRIMARY_COLOR_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                        GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//                // stage 1 alpha takes bump * bump
//                // PRIMARY_COLOR = ( GL_PRIMARY_COLOR_NV * GL_PRIMARY_COLOR_NV - 0.5 ) * 2
//                // the scale and bias steepen the specular curve
//                qglCombinerInputNV(GL_COMBINER1_NV, GL_ALPHA, GL_VARIABLE_A_NV,
//                        GL_PRIMARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_BLUE);
//                qglCombinerInputNV(GL_COMBINER1_NV, GL_ALPHA, GL_VARIABLE_B_NV,
//                        GL_PRIMARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_BLUE);
//                qglCombinerOutputNV(GL_COMBINER1_NV, GL_ALPHA,
//                        GL_PRIMARY_COLOR_NV, GL_DISCARD_NV, GL_DISCARD_NV,
//                        GL_SCALE_BY_TWO_NV, GL_BIAS_BY_NEGATIVE_ONE_HALF_NV, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//                // final combiner
//                qglFinalCombinerInputNV(GL_VARIABLE_D_NV, GL_PRIMARY_COLOR_NV,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_A_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_B_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_C_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_PRIMARY_COLOR_NV,
//                        GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//
//                // draw it
//                RB_DrawElementsWithCounters(tri);
//
//                globalImages.BindNull();
//                qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//
//                GL_SelectTexture(0);
//
//                RB_FinishStageTexture(lastBumpStage.texture, surf);
//
//                // the bump map in the alpha channel is now corrupted, so a normal diffuse
//                // map can't be drawn unless a new bumpmap is put down
//                lastBumpStage = null;
//
//                // fall through to the common handling of diffuse and specular projected lighting
//            }
//
//            //-----------------------------------------------------
//            //
//            // projected light / surface color for diffuse and specular maps
//            //
//            //-----------------------------------------------------
//            if (surfaceStage.lighting == SL_DIFFUSE || surfaceStage.lighting == SL_SPECULAR) {
//                // don't trash alpha
//                GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ONE | GLS_ALPHAMASK | GLS_DEPTHMASK
//                        | backEnd.depthFunc);
//
//                // texture 0 will get the surface color texture
//                GL_SelectTexture(0);
//                qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//                RB_BindStageTexture(surfaceRegs, surfaceStage.texture, surf);
//
//                // development aid
//                if ((surfaceStage.lighting == SL_DIFFUSE && RenderSystem_init.r_skipDiffuse.GetBool())
//                        || (surfaceStage.lighting == SL_SPECULAR && RenderSystem_init.r_skipSpecular.GetBool())) {
//                    globalImages.blackImage.Bind();
//                }
//
//                // texture 1 will get the light projected texture
//                GL_SelectTexture(1);
//                qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
//                qglEnable(GL_TEXTURE_GEN_S);
//                qglEnable(GL_TEXTURE_GEN_T);
//                qglEnable(GL_TEXTURE_GEN_Q);
//                qglTexGenfv(GL_S, GL_OBJECT_PLANE, lightProject[0].ToFloatPtr());
//                qglTexGenfv(GL_T, GL_OBJECT_PLANE, lightProject[1].ToFloatPtr());
//                qglTexGenfv(GL_Q, GL_OBJECT_PLANE, lightProject[2].ToFloatPtr());
//
//                // texture0 * texture1 * primaryColor * constantColor
//                qglCombinerParameteriNV(GL_NUM_GENERAL_COMBINERS_NV, 1);
//
//                // SPARE0 = TEXTURE0 * PRIMARY_COLOR
//                // SPARE1 = TEXTURE1 * CONSTANT_COLOR
//                qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_A_NV,
//                        GL_TEXTURE0_ARB, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                // variable B will be overriden based on the stage vertexColor option
//                if (surfaceStage.vertexColor == SVC_MODULATE) {
//                    qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                            GL_PRIMARY_COLOR_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                    qglEnableClientState(GL_COLOR_ARRAY);
//                } else if (surfaceStage.vertexColor == SVC_INVERSE_MODULATE) {
//                    qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                            GL_PRIMARY_COLOR_NV, GL_UNSIGNED_INVERT_NV, GL_RGB);
//                    qglEnableClientState(GL_COLOR_ARRAY);
//                } else {	// SVC_IGNORE
//                    qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_B_NV,
//                            GL_ZERO, GL_UNSIGNED_INVERT_NV, GL_RGB);
//                    qglDisableClientState(GL_COLOR_ARRAY);
//                }
//                qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_C_NV,
//                        GL_TEXTURE1_ARB, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglCombinerInputNV(GL_COMBINER0_NV, GL_RGB, GL_VARIABLE_D_NV,
//                        GL_CONSTANT_COLOR1_NV, GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglCombinerOutputNV(GL_COMBINER0_NV, GL_RGB,
//                        GL_SPARE0_NV, GL_SPARE1_NV, GL_DISCARD_NV,
//                        GL_NONE, GL_NONE, qGL_FALSE, qGL_FALSE, qGL_FALSE);
//
//                // final combiner
//                qglFinalCombinerInputNV(GL_VARIABLE_A_NV, GL_SPARE1_NV,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_B_NV, GL_SPARE0_NV,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_C_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_D_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_RGB);
//                qglFinalCombinerInputNV(GL_VARIABLE_G_NV, GL_ZERO,
//                        GL_UNSIGNED_IDENTITY_NV, GL_ALPHA);
//
//                // for all light stages, multiply the projected color by the surface
//                // color, and blend with the framebuffer
//                for (int j = 0; j < lightShader.GetNumStages(); j++) {
//                    shaderStage_t lightStage = lightShader.GetStage(j);
//                    float[] color = new float[4];
//
//                    // ignore stages that fail the condition
//                    if (0 == lightRegs[lightStage.conditionRegister]) {
//                        continue;
//                    }
//
//                    // set the color to the light color times the surface color
//                    color[0] = backEnd.lightScale
//                            * lightRegs[lightStage.color.registers[0]]
//                            * surfaceRegs[surfaceStage.color.registers[0]];
//                    color[1] = backEnd.lightScale
//                            * lightRegs[lightStage.color.registers[1]]
//                            * surfaceRegs[surfaceStage.color.registers[1]];
//                    color[2] = backEnd.lightScale
//                            * lightRegs[lightStage.color.registers[2]]
//                            * surfaceRegs[surfaceStage.color.registers[2]];
//                    color[3] = 1;
//
//                    // don't draw if it would be all black
//                    if (color[0] == 0 && color[1] == 0 && color[2] == 0) {
//                        continue;
//                    }
//
//                    qglCombinerParameterfvNV(GL_CONSTANT_COLOR1_NV, color);
//
//                    RB_BindStageTexture(lightRegs, lightStage.texture, surf);
//
//                    RB_DrawElementsWithCounters(tri);
//
//                    RB_FinishStageTexture(lightStage.texture, surf);
//                }
//
//                if (surfaceStage.vertexColor != SVC_IGNORE) {
//                    qglDisableClientState(GL_COLOR_ARRAY);
//                }
//
//                qglDisable(GL_TEXTURE_GEN_S);
//                qglDisable(GL_TEXTURE_GEN_T);
//                qglDisable(GL_TEXTURE_GEN_Q);
//
//                globalImages.BindNull();
//                GL_SelectTexture(0);
//                RB_FinishStageTexture(surfaceStage.texture, surf);
//
//                continue;
//            }
//
//        }
//        // unhack depth range if needed
//        if (surf.space.weaponDepthHack || surf.space.modelDepthHack != 0.0f) {
//            RB_LeaveDepthHack();
//        }
//    }
//
//    /*
//     ==================
//     RB_RenderInteractionList
//     ==================
//     */
//    public static void RB_RenderInteractionList(final drawSurf_s surf) {
//        if (null == surf) {
//            return;
//        }
//
//        qglEnable(GL_REGISTER_COMBINERS_NV);
//
//        // force a space calculation for light vectors
//        backEnd.currentSpace = null;
//
//        for (drawSurf_s s = surf; s != null; s = s.nextOnLight) {
//            RB_RenderInteraction(s);
//        }
//
//        qglDisable(GL_REGISTER_COMBINERS_NV);
//    }
//
//    /*
//     ==================
//     RB_RenderViewLight
//
//     ==================
//     */
//    public static void RB_RenderViewLight(viewLight_s vLight) {
//        backEnd.vLight = vLight;
//
//        // do fogging later
//        if (vLight.lightShader.IsFogLight()) {
//            return;
//        }
//        if (vLight.lightShader.IsBlendLight()) {
//            return;
//        }
//
//        RB_LogComment("---------- RB_RenderViewLight 0x%p ----------\n", vLight);
//
//        // clear the stencil buffer if needed
//        if (vLight.globalShadows[0] != null || vLight.localShadows[0] != null) {
//            backEnd.currentScissor = new idScreenRect(vLight.scissorRect);
//            if (RenderSystem_init.r_useScissor.GetBool()) {
//                qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
//                        backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
//                        backEnd.currentScissor.x2 + 1 - backEnd.currentScissor.x1,
//                        backEnd.currentScissor.y2 + 1 - backEnd.currentScissor.y1);
//            }
//            qglClear(GL_STENCIL_BUFFER_BIT);
//        } else {
//            // no shadows, so no need to read or write the stencil buffer
//            // we might in theory want to use GL_ALWAYS instead of disabling
//            // completely, to satisfy the invarience rules
//            qglStencilFunc(GL_ALWAYS, 128, 255);
//        }
//
//        backEnd.depthFunc = GLS_DEPTHFUNC_EQUAL;
//        RB_StencilShadowPass(vLight.globalShadows[0]);
//        RB_RenderInteractionList(vLight.localInteractions[0]);
//        RB_StencilShadowPass(vLight.localShadows[0]);
//        RB_RenderInteractionList(vLight.globalInteractions[0]);
//
//        if (RenderSystem_init.r_skipTranslucent.GetBool()) {
//            return;
//        }
//
//        // disable stencil testing for translucent interactions, because
//        // the shadow isn't calculated at their point, and the shadow
//        // behind them may be depth fighting with a back side, so there
//        // isn't any reasonable thing to do
//        qglStencilFunc(GL_ALWAYS, 128, 255);
//        backEnd.depthFunc = GLS_DEPTHFUNC_LESS;
//        RB_RenderInteractionList(vLight.translucentInteractions[0]);
//    }
//
//
//    /*
//     ==================
//     RB_NV10_DrawInteractions
//     ==================
//     */
//    public static void RB_NV10_DrawInteractions() {
//        qglEnable(GL_STENCIL_TEST);
//
//        for (viewLight_s vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
//            RB_RenderViewLight(vLight);
//        }
//    }
//
//
//    /*
//     ==================
//     R_NV10_Init
//
//     ==================
//     */
//    public static void R_NV10_Init() {
//        glConfig.allowNV10Path = false;
//
//        if (!glConfig.registerCombinersAvailable) {
//            return;
//        }
//
//        glConfig.allowNV10Path = true;
//    }
}
