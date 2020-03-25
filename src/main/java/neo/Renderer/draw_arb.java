package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Material.stageVertexColor_t.SVC_IGNORE;
import static neo.Renderer.Material.stageVertexColor_t.SVC_INVERSE_MODULATE;
import static neo.Renderer.RenderSystem_init.r_useTripleTextureARB;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.draw_common.RB_StencilShadowPass;
import static neo.Renderer.qgl.qglClear;
import static neo.Renderer.qgl.qglColor3f;
import static neo.Renderer.qgl.qglColor4fv;
import static neo.Renderer.qgl.qglColorPointer;
import static neo.Renderer.qgl.qglDisable;
import static neo.Renderer.qgl.qglDisableClientState;
import static neo.Renderer.qgl.qglEnable;
import static neo.Renderer.qgl.qglEnableClientState;
import static neo.Renderer.qgl.qglScissor;
import static neo.Renderer.qgl.qglStencilFunc;
import static neo.Renderer.qgl.qglTexCoord2f;
import static neo.Renderer.qgl.qglTexCoordPointer;
import static neo.Renderer.qgl.qglTexEnvi;
import static neo.Renderer.qgl.qglTexGenfv;
import static neo.Renderer.qgl.qglVertexPointer;
import static neo.Renderer.tr_backend.GL_SelectTexture;
import static neo.Renderer.tr_backend.GL_State;
import static neo.Renderer.tr_backend.GL_TexEnv;
import static neo.Renderer.tr_backend.RB_LogComment;
import static neo.Renderer.tr_local.GLS_ALPHAMASK;
import static neo.Renderer.tr_local.GLS_COLORMASK;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_EQUAL;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_LESS;
import static neo.Renderer.tr_local.GLS_DEPTHMASK;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ZERO;
import static neo.Renderer.tr_local.GLS_SRCBLEND_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE;
import static neo.Renderer.tr_local.backEnd;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_render.RB_CreateSingleDrawInteractions;
import static neo.Renderer.tr_render.RB_DrawElementsWithCounters;
import static neo.TempDump.NOT;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_COMBINE_ARB;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_COMBINE_RGB_ARB;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_OPERAND0_RGB_ARB;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_OPERAND1_RGB_ARB;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_PREVIOUS_ARB;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_PRIMARY_COLOR_ARB;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_RGB_SCALE_ARB;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_SOURCE0_RGB_ARB;
import static org.lwjgl.opengl.ARBTextureEnvCombine.GL_SOURCE1_RGB_ARB;
import static org.lwjgl.opengl.ARBTextureEnvDot3.GL_DOT3_RGBA_ARB;
import static org.lwjgl.opengl.GL11.GL_ALPHA_SCALE;
import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_MODULATE;
import static org.lwjgl.opengl.GL11.GL_OBJECT_PLANE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_COLOR;
import static org.lwjgl.opengl.GL11.GL_Q;
import static org.lwjgl.opengl.GL11.GL_S;
import static org.lwjgl.opengl.GL11.GL_SRC_COLOR;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.GL_T;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_ENV;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_Q;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

import neo.Renderer.Model.lightingCache_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.tr_local.drawInteraction_t;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.idScreenRect;
import neo.Renderer.tr_local.viewLight_s;
import neo.Renderer.tr_render.DrawInteraction;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class draw_arb {

    /*

     with standard calls, we can't do bump mapping or vertex colors with
     shader colors

     2 texture units:

     falloff
     --
     light cube
     bump
     --
     light projection
     diffuse


     3 texture units:

     light cube
     bump
     --
     falloff
     light projection
     diffuse


     5 texture units:

     light cube
     bump
     falloff
     light projection
     diffuse

     */

    /*
     ==================
     RB_ARB_DrawInteraction

     backEnd.vLight

     backEnd.depthFunc must be equal for alpha tested surfaces to work right,
     it is set to lessThan for blended transparent surfaces

     ==================
     */
    public static class RB_ARB_DrawInteraction extends DrawInteraction {

        static final DrawInteraction INSTANCE = new RB_ARB_DrawInteraction();

        private RB_ARB_DrawInteraction() {

        }

        @Override
        public void run(final drawInteraction_t din) {
            final drawSurf_s surf = din.surf;
            final srfTriangles_s tri = din.surf.geo;

            // set the vertex arrays, which may not all be enabled on a given pass
            final idDrawVert ac = new idDrawVert(vertexCache.Position(tri.ambientCache));//TODO:figure out how to work these damn casts.
            qglVertexPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.xyz.ToFloatPtr());
            GL_SelectTexture(0);
            qglTexCoordPointer(2, GL_FLOAT, 0/*sizeof(idDrawVert)*/, /*(void *)&*/ ac.st.ToFloatPtr());

            //-----------------------------------------------------
            //
            // bump / falloff
            //
            //-----------------------------------------------------
            // render light falloff * bumpmap lighting
            //
            // draw light falloff to the alpha channel
            //
            GL_State(GLS_COLORMASK | GLS_DEPTHMASK | backEnd.depthFunc);

            qglColor3f(1, 1, 1);
            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
            qglEnable(GL_TEXTURE_GEN_S);
            qglTexGenfv(GL_S, GL_OBJECT_PLANE, din.lightProjection[3].ToFloatPtr());
            qglTexCoord2f(0, 0.5f);

// ATI R100 can't do partial texgens
            final boolean NO_MIXED_TEXGEN = true;

            if (NO_MIXED_TEXGEN) {
                final idVec4 plane = new idVec4(0, 0, 0, 0.5f);
//plane[0] = 0;
//plane[1] = 0;
//plane[2] = 0;
//plane[3] = 0.5;
                qglEnable(GL_TEXTURE_GEN_T);
                qglTexGenfv(GL_T, GL_OBJECT_PLANE, plane.ToFloatPtr());

                plane.oSet(0, 0f);
                plane.oSet(1, 0f);
                plane.oSet(2, 0f);
                plane.oSet(3, 1f);
                qglEnable(GL_TEXTURE_GEN_Q);
                qglTexGenfv(GL_Q, GL_OBJECT_PLANE, plane.ToFloatPtr());

            }

            din.lightFalloffImage.Bind();

            // draw it
            RB_DrawElementsWithCounters(tri);

            qglDisable(GL_TEXTURE_GEN_S);
            if (NO_MIXED_TEXGEN) {
                qglDisable(GL_TEXTURE_GEN_T);
                qglDisable(GL_TEXTURE_GEN_Q);
            }

//if (false){
//GL_State( GLS_SRCBLEND_ONE | GLS_DSTBLEND_ZERO | GLS_DEPTHMASK 
//			| backEnd.depthFunc );
//// the texccords are the non-normalized vector towards the light origin
//GL_SelectTexture( 0 );
//globalImages.normalCubeMapImage.Bind();
//qglEnableClientState( GL_TEXTURE_COORD_ARRAY );
//qglTexCoordPointer( 3, GL_FLOAT, sizeof( lightingCache_t ), ((lightingCache_t *)vertexCache.Position(tri.lightingCache)).localLightVector.ToFloatPtr() );
//// draw it
//RB_DrawElementsWithCounters( tri );
//return;
//}
            // we can't do bump mapping with standard calls, so skip it
            if (glConfig.envDot3Available && glConfig.cubeMapAvailable) {
                //
                // draw the bump map result onto the alpha channel
                //
                GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ZERO | GLS_COLORMASK | GLS_DEPTHMASK
                        | backEnd.depthFunc);

                // texture 0 will be the per-surface bump map
                GL_SelectTexture(0);
                qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//	FIXME: matrix work!	RB_BindStageTexture( surfaceRegs, &surfaceStage.texture, surf );
                din.bumpImage.Bind();

                // texture 1 is the normalization cube map
                // the texccords are the non-normalized vector towards the light origin
                GL_SelectTexture(1);
                if (din.ambientLight != 0) {
                    globalImages.ambientNormalMap.Bind();	// fixed value
                } else {
                    globalImages.normalCubeMapImage.Bind();
                }
                qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
                //TODO:figure out how to work these damn casts.
                final lightingCache_s c = new lightingCache_s(vertexCache.Position(tri.lightingCache));
                qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(lightingCache_s)*/, c.localLightVector.ToFloatPtr());

                // I just want alpha = Dot( texture0, texture1 )
                GL_TexEnv(GL_COMBINE_ARB);

                qglTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB_ARB, GL_DOT3_RGBA_ARB);
                qglTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB_ARB, GL_TEXTURE);
                qglTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB_ARB, GL_PREVIOUS_ARB);
                qglTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB_ARB, GL_SRC_COLOR);
                qglTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB_ARB, GL_SRC_COLOR);
                qglTexEnvi(GL_TEXTURE_ENV, GL_RGB_SCALE_ARB, 1);
                qglTexEnvi(GL_TEXTURE_ENV, GL_ALPHA_SCALE, 1);

                // draw it
                RB_DrawElementsWithCounters(tri);

                GL_TexEnv(GL_MODULATE);

                globalImages.BindNull();
                qglDisableClientState(GL_TEXTURE_COORD_ARRAY);

                GL_SelectTexture(0);
//		RB_FinishStageTexture( &surfaceStage.texture, surf );
            }

            //-----------------------------------------------------
            //
            // projected light / surface color for diffuse maps
            //
            //-----------------------------------------------------
            // don't trash alpha
            GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ONE | GLS_ALPHAMASK | GLS_DEPTHMASK
                    | backEnd.depthFunc);

            // texture 0 will get the surface color texture
            GL_SelectTexture(0);

            // select the vertex color source
            if (din.vertexColor == SVC_IGNORE) {
                qglColor4fv(din.diffuseColor.ToFloatPtr());
            } else {
                // FIXME: does this not get diffuseColor blended in?
                qglColorPointer(4, GL_UNSIGNED_BYTE, 0/*sizeof(idDrawVert)*/, /*(void *)&*/ ac.color);
                qglEnableClientState(GL_COLOR_ARRAY);

                if (din.vertexColor == SVC_INVERSE_MODULATE) {
                    GL_TexEnv(GL_COMBINE_ARB);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB_ARB, GL_MODULATE);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB_ARB, GL_TEXTURE);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB_ARB, GL_PRIMARY_COLOR_ARB);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB_ARB, GL_SRC_COLOR);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB_ARB, GL_ONE_MINUS_SRC_COLOR);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_RGB_SCALE_ARB, 1);
                }
            }

            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
            // FIXME: does this not get the texture matrix?
//	RB_BindStageTexture( surfaceRegs, &surfaceStage.texture, surf );
            din.diffuseImage.Bind();

            // texture 1 will get the light projected texture
            GL_SelectTexture(1);
            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
            qglEnable(GL_TEXTURE_GEN_S);
            qglEnable(GL_TEXTURE_GEN_T);
            qglEnable(GL_TEXTURE_GEN_Q);
            qglTexGenfv(GL_S, GL_OBJECT_PLANE, din.lightProjection[0].ToFloatPtr());
            qglTexGenfv(GL_T, GL_OBJECT_PLANE, din.lightProjection[1].ToFloatPtr());
            qglTexGenfv(GL_Q, GL_OBJECT_PLANE, din.lightProjection[2].ToFloatPtr());

            din.lightImage.Bind();

            // draw it
            RB_DrawElementsWithCounters(tri);

            qglDisable(GL_TEXTURE_GEN_S);
            qglDisable(GL_TEXTURE_GEN_T);
            qglDisable(GL_TEXTURE_GEN_Q);

            globalImages.BindNull();
            GL_SelectTexture(0);

            if (din.vertexColor != SVC_IGNORE) {
                qglDisableClientState(GL_COLOR_ARRAY);
                GL_TexEnv(GL_MODULATE);
            }

//	RB_FinishStageTexture( &surfaceStage.texture, surf );
        }
    }

    /*
     ==================
     RB_ARB_DrawThreeTextureInteraction

     Used by radeon R100 and Intel graphics parts

     backEnd.vLight

     backEnd.depthFunc must be equal for alpha tested surfaces to work right,
     it is set to lessThan for blended transparent surfaces

     ==================
     */
    public static class RB_ARB_DrawThreeTextureInteraction extends DrawInteraction {

        static final DrawInteraction INSTANCE = new RB_ARB_DrawThreeTextureInteraction();

        private RB_ARB_DrawThreeTextureInteraction() {
        }

        @Override
        public void run(final drawInteraction_t din) {
            final drawSurf_s surf = din.surf;
            final srfTriangles_s tri = din.surf.geo;

            // set the vertex arrays, which may not all be enabled on a given pass
            final idDrawVert ac = new idDrawVert(vertexCache.Position(tri.ambientCache));//TODO:figure out how to work these damn casts.
            qglVertexPointer(3, GL_FLOAT, idDrawVert.BYTES, ac.xyzOffset());
            GL_SelectTexture(0);
            qglTexCoordPointer(2, GL_FLOAT, idDrawVert.BYTES, ac.stOffset());
            qglColor3f(1, 1, 1);

            //
            // bump map dot cubeMap into the alpha channel
            //
            GL_State(GLS_SRCBLEND_ONE | GLS_DSTBLEND_ZERO | GLS_COLORMASK | GLS_DEPTHMASK
                    | backEnd.depthFunc);

            // texture 0 will be the per-surface bump map
            GL_SelectTexture(0);
            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
//	FIXME: matrix work!	RB_BindStageTexture( surfaceRegs, &surfaceStage.texture, surf );
            din.bumpImage.Bind();

            // texture 1 is the normalization cube map
            // the texccords are the non-normalized vector towards the light origin
            GL_SelectTexture(1);
            if (din.ambientLight != 0) {
                globalImages.ambientNormalMap.Bind();	// fixed value
            } else {
                globalImages.normalCubeMapImage.Bind();
            }
            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
            final lightingCache_s c = new lightingCache_s(vertexCache.Position(tri.lightingCache));//{//TODO:figure out how to work these damn casts.
            qglTexCoordPointer(3, GL_FLOAT, 0/*sizeof(lightingCache_s)*/, c.localLightVector.ToFloatPtr());

            // I just want alpha = Dot( texture0, texture1 )
            GL_TexEnv(GL_COMBINE_ARB);

            qglTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB_ARB, GL_DOT3_RGBA_ARB);
            qglTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB_ARB, GL_TEXTURE);
            qglTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB_ARB, GL_PREVIOUS_ARB);
            qglTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB_ARB, GL_SRC_COLOR);
            qglTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB_ARB, GL_SRC_COLOR);
            qglTexEnvi(GL_TEXTURE_ENV, GL_RGB_SCALE_ARB, 1);
            qglTexEnvi(GL_TEXTURE_ENV, GL_ALPHA_SCALE, 1);

            // draw it
            RB_DrawElementsWithCounters(tri);

            GL_TexEnv(GL_MODULATE);

            globalImages.BindNull();
            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);

            GL_SelectTexture(0);
//		RB_FinishStageTexture( &surfaceStage.texture, surf );

            //-----------------------------------------------------
            //
            // light falloff / projected light / surface color for diffuse maps
            //
            //-----------------------------------------------------
            // multiply result by alpha, but don't trash alpha
            GL_State(GLS_SRCBLEND_DST_ALPHA | GLS_DSTBLEND_ONE | GLS_ALPHAMASK | GLS_DEPTHMASK
                    | backEnd.depthFunc);

            // texture 0 will get the surface color texture
            GL_SelectTexture(0);

            // select the vertex color source
            if (din.vertexColor == SVC_IGNORE) {
                qglColor4fv(din.diffuseColor.ToFloatPtr());
            } else {
                // FIXME: does this not get diffuseColor blended in?
                qglColorPointer(4, GL_UNSIGNED_BYTE, 0/*sizeof(idDrawVert)*/, /*(void *)&*/ ac.color);
                qglEnableClientState(GL_COLOR_ARRAY);

                if (din.vertexColor == SVC_INVERSE_MODULATE) {
                    GL_TexEnv(GL_COMBINE_ARB);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB_ARB, GL_MODULATE);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB_ARB, GL_TEXTURE);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB_ARB, GL_PRIMARY_COLOR_ARB);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB_ARB, GL_SRC_COLOR);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB_ARB, GL_ONE_MINUS_SRC_COLOR);
                    qglTexEnvi(GL_TEXTURE_ENV, GL_RGB_SCALE_ARB, 1);
                }
            }

            qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
            // FIXME: does this not get the texture matrix?
//	RB_BindStageTexture( surfaceRegs, &surfaceStage.texture, surf );
            din.diffuseImage.Bind();

            // texture 1 will get the light projected texture
            GL_SelectTexture(1);
            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
            qglEnable(GL_TEXTURE_GEN_S);
            qglEnable(GL_TEXTURE_GEN_T);
            qglEnable(GL_TEXTURE_GEN_Q);
            qglTexGenfv(GL_S, GL_OBJECT_PLANE, din.lightProjection[0].ToFloatPtr());
            qglTexGenfv(GL_T, GL_OBJECT_PLANE, din.lightProjection[1].ToFloatPtr());
            qglTexGenfv(GL_Q, GL_OBJECT_PLANE, din.lightProjection[2].ToFloatPtr());
            din.lightImage.Bind();

            // texture 2 will get the light falloff texture
            GL_SelectTexture(2);
            qglDisableClientState(GL_TEXTURE_COORD_ARRAY);
            qglEnable(GL_TEXTURE_GEN_S);
            qglEnable(GL_TEXTURE_GEN_T);
            qglEnable(GL_TEXTURE_GEN_Q);

            qglTexGenfv(GL_S, GL_OBJECT_PLANE, din.lightProjection[3].ToFloatPtr());

            final idVec4 plane = new idVec4();
            plane.oSet(0, 0f);
            plane.oSet(1, 0f);
            plane.oSet(2, 0f);
            plane.oSet(3, 0.5f);
            qglTexGenfv(GL_T, GL_OBJECT_PLANE, plane.ToFloatPtr());

            plane.oSet(0, 0f);
            plane.oSet(1, 0f);
            plane.oSet(2, 0f);
            plane.oSet(3, 1f);
            qglTexGenfv(GL_Q, GL_OBJECT_PLANE, plane.ToFloatPtr());

            din.lightFalloffImage.Bind();

            // draw it
            RB_DrawElementsWithCounters(tri);

            qglDisable(GL_TEXTURE_GEN_S);
            qglDisable(GL_TEXTURE_GEN_T);
            qglDisable(GL_TEXTURE_GEN_Q);
            globalImages.BindNull();

            GL_SelectTexture(1);
            qglDisable(GL_TEXTURE_GEN_S);
            qglDisable(GL_TEXTURE_GEN_T);
            qglDisable(GL_TEXTURE_GEN_Q);
            globalImages.BindNull();

            GL_SelectTexture(0);

            if (din.vertexColor != SVC_IGNORE) {
                qglDisableClientState(GL_COLOR_ARRAY);
                GL_TexEnv(GL_MODULATE);
            }

//	RB_FinishStageTexture( &surfaceStage.texture, surf );
        }
    }


    /*
     ==================
     RB_CreateDrawInteractions
     ==================
     */
    public static void RB_CreateDrawInteractions(final drawSurf_s surfs) {
        drawSurf_s surf = surfs;
        if (NOT(surf)) {
            return;
        }

        // force a space calculation
        backEnd.currentSpace = null;

        if (r_useTripleTextureARB.GetBool() && (glConfig.maxTextureUnits >= 3)) {
            for (; surf != null; surf = surf.nextOnLight) {
                // break it up into multiple primitive draw interactions if necessary
                RB_CreateSingleDrawInteractions(surf, RB_ARB_DrawThreeTextureInteraction.INSTANCE);
            }
        } else {
            for (; surf != null; surf = surf.nextOnLight) {
                // break it up into multiple primitive draw interactions if necessary
                RB_CreateSingleDrawInteractions(surf, RB_ARB_DrawInteraction.INSTANCE);
            }
        }
    }

    /*
     ==================
     RB_RenderViewLight

     ==================
     */
    public static void RB_RenderViewLight(viewLight_s vLight) {
        backEnd.vLight = vLight;

        // do fogging later
        if (vLight.lightShader.IsFogLight()) {
            return;
        }
        if (vLight.lightShader.IsBlendLight()) {
            return;
        }

        RB_LogComment("---------- RB_RenderViewLight 0x%p ----------\n", vLight);

        // clear the stencil buffer if needed
        if ((vLight.globalShadows[0] != null) || (vLight.localShadows[0] != null)) {
            backEnd.currentScissor = new idScreenRect(vLight.scissorRect);
            if (RenderSystem_init.r_useScissor.GetBool()) {
                qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
                        backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
                        (backEnd.currentScissor.x2 + 1) - backEnd.currentScissor.x1,
                        (backEnd.currentScissor.y2 + 1) - backEnd.currentScissor.y1);
            }
            qglClear(GL_STENCIL_BUFFER_BIT);
        } else {
            // no shadows, so no need to read or write the stencil buffer
            // we might in theory want to use GL_ALWAYS instead of disabling
            // completely, to satisfy the invarience rules
            qglStencilFunc(GL_ALWAYS, 128, 255);
        }

        backEnd.depthFunc = GLS_DEPTHFUNC_EQUAL;
        RB_StencilShadowPass(vLight.globalShadows[0]);
        RB_CreateDrawInteractions(vLight.localInteractions[0]);
        RB_StencilShadowPass(vLight.localShadows[0]);
        RB_CreateDrawInteractions(vLight.globalInteractions[0]);

        if (RenderSystem_init.r_skipTranslucent.GetBool()) {
            return;
        }

        // disable stencil testing for translucent interactions, because
        // the shadow isn't calculated at their point, and the shadow
        // behind them may be depth fighting with a back side, so there
        // isn't any reasonable thing to do
        qglStencilFunc(GL_ALWAYS, 128, 255);
        backEnd.depthFunc = GLS_DEPTHFUNC_LESS;
        RB_CreateDrawInteractions(vLight.translucentInteractions[0]);
    }


    /*
     ==================
     RB_ARB_DrawInteractions
     ==================
     */
    public static void RB_ARB_DrawInteractions() {
        qglEnable(GL_STENCIL_TEST);

        for (viewLight_s vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            RB_RenderViewLight(vLight);
        }
    }
}
