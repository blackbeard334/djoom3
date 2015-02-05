package neo.Renderer;

import java.util.Arrays;
import neo.Renderer.Cinematic.cinData_t;
import static neo.Renderer.Image.globalImages;
import neo.Renderer.Image.idImage;
import static neo.Renderer.Material.cullType_t.CT_FRONT_SIDED;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.shaderStage_t;
import static neo.Renderer.Material.texgen_t.TG_DIFFUSE_CUBE;
import static neo.Renderer.Material.texgen_t.TG_REFLECT_CUBE;
import static neo.Renderer.Material.texgen_t.TG_SKYBOX_CUBE;
import static neo.Renderer.Material.texgen_t.TG_WOBBLESKY_CUBE;
import neo.Renderer.Material.textureStage_t;
import static neo.Renderer.Model.GL_INDEX_TYPE;
import neo.Renderer.Model.srfTriangles_s;
import static neo.Renderer.RenderSystem_init.r_lightScale;
import static neo.Renderer.RenderSystem_init.r_singleTriangle;
import static neo.Renderer.RenderSystem_init.r_skipBump;
import static neo.Renderer.RenderSystem_init.r_skipDiffuse;
import static neo.Renderer.RenderSystem_init.r_skipDynamicTextures;
import static neo.Renderer.RenderSystem_init.r_skipInteractions;
import static neo.Renderer.RenderSystem_init.r_skipRender;
import static neo.Renderer.RenderSystem_init.r_skipRenderContext;
import static neo.Renderer.RenderSystem_init.r_skipSpecular;
import static neo.Renderer.RenderSystem_init.r_useIndexBuffers;
import static neo.Renderer.RenderSystem_init.r_useScissor;
import neo.Renderer.VertexCache.vertCache_s;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.draw_common.RB_BakeTextureMatrixIntoTexgen;
import static neo.Renderer.draw_common.RB_STD_DrawView;
import static neo.Renderer.qgl.qglBegin;
import static neo.Renderer.qgl.qglClear;
import static neo.Renderer.qgl.qglClearStencil;
import static neo.Renderer.qgl.qglDepthRange;
import static neo.Renderer.qgl.qglDisable;
import static neo.Renderer.qgl.qglDisableClientState;
import static neo.Renderer.qgl.qglDrawElements;
import static neo.Renderer.qgl.qglEnable;
import static neo.Renderer.qgl.qglEnableClientState;
import static neo.Renderer.qgl.qglEnd;
import static neo.Renderer.qgl.qglLoadIdentity;
import static neo.Renderer.qgl.qglLoadMatrixf;
import static neo.Renderer.qgl.qglMatrixMode;
import static neo.Renderer.qgl.qglNormalPointer;
import static neo.Renderer.qgl.qglScissor;
import static neo.Renderer.qgl.qglStencilMask;
import static neo.Renderer.qgl.qglTexCoord2fv;
import static neo.Renderer.qgl.qglTexCoordPointer;
import static neo.Renderer.qgl.qglTexGenf;
import static neo.Renderer.qgl.qglVertex3fv;
import static neo.Renderer.qgl.qglVertexPointer;
import static neo.Renderer.qgl.qglViewport;
import static neo.Renderer.tr_backend.GL_Cull;
import static neo.Renderer.tr_backend.GL_State;
import static neo.Renderer.tr_backend.RB_LogComment;
import static neo.Renderer.tr_backend.RB_SetDefaultGLState;
import static neo.Renderer.tr_local.GLS_DEFAULT;
import static neo.Renderer.tr_local.backEnd;
import neo.Renderer.tr_local.drawInteraction_t;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.drawSurfsCommand_t;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import neo.Renderer.tr_local.viewLight_s;
import static neo.Renderer.tr_main.R_ClearedStaticAlloc;
import static neo.Renderer.tr_main.R_GlobalPlaneToLocal;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_main.R_TransposeGLMatrix;
import static neo.Renderer.tr_rendertools.RB_ShowOverdraw;
import neo.TempDump;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.win_glimp.GLimp_ActivateContext;
import static neo.sys.win_glimp.GLimp_DeactivateContext;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY;
import static org.lwjgl.opengl.GL11.GL_OBJECT_LINEAR;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_R;
import static org.lwjgl.opengl.GL11.GL_S;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.GL_T;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_MODE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_R;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_GEN_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL13.GL_REFLECTION_MAP;

/**
 *
 */
public class tr_render {
    /*

     back end scene + lights rendering functions

     */
//    
//    
//    
//    
//    
//    

    static abstract class DrawInteraction {

        abstract void run(final drawInteraction_t din);
    };

    static abstract class triFunc {

        abstract void run(final drawSurf_s surf);
    };

    /*
     =================
     RB_DrawElementsImmediate

     Draws with immediate mode commands, which is going to be very slow.
     This should never happen if the vertex cache is operating properly.
     =================
     */
    public static void RB_DrawElementsImmediate(final srfTriangles_s tri) {
        backEnd.pc.c_drawElements++;
        backEnd.pc.c_drawIndexes += tri.numIndexes;
        backEnd.pc.c_drawVertexes += tri.numVerts;

        if (tri.ambientSurface != null) {
            if (tri.indexes == tri.ambientSurface.indexes) {
                backEnd.pc.c_drawRefIndexes += tri.numIndexes;
            }
            if (tri.verts == tri.ambientSurface.verts) {
                backEnd.pc.c_drawRefVertexes += tri.numVerts;
            }
        }

        qglBegin(GL_TRIANGLES);
        for (int i = 0; i < tri.numIndexes; i++) {
            qglTexCoord2fv(tri.verts[tri.indexes[i]].st.ToFloatPtr());
            qglVertex3fv(tri.verts[tri.indexes[i]].xyz.ToFloatPtr());
        }
        qglEnd();
    }


    /*
     ================
     RB_DrawElementsWithCounters
     ================
     */    static int DEBUG_RB_DrawElementsWithCounters = 0;

    public static void RB_DrawElementsWithCounters(final srfTriangles_s tri) {

        backEnd.pc.c_drawElements++;
        backEnd.pc.c_drawIndexes += tri.numIndexes;
        backEnd.pc.c_drawVertexes += tri.numVerts;
        DEBUG_RB_DrawElementsWithCounters++;

        if (tri.ambientSurface != null) {
            if (tri.indexes == tri.ambientSurface.indexes) {
                backEnd.pc.c_drawRefIndexes += tri.numIndexes;
            }
            if (tri.verts == tri.ambientSurface.verts) {
                backEnd.pc.c_drawRefVertexes += tri.numVerts;
            }
        }

        if (tri.indexCache != null && r_useIndexBuffers.GetBool()) {
            qglDrawElements(GL_TRIANGLES,
                    r_singleTriangle.GetBool() ? 3 : tri.numIndexes,
                    GL_INDEX_TYPE,
                    vertexCache.Position(tri.indexCache));
            backEnd.pc.c_vboIndexes += tri.numIndexes;
        } else {
            if (r_useIndexBuffers.GetBool()) {
                vertexCache.UnbindIndex();
            }
//            GL30.glBindVertexArray(tri.ambientCache.vao);
//            GL20.glEnableVertexAttribArray(0);
            final int count = r_singleTriangle.GetBool() ? 3 : tri.numIndexes;
//            GL20.glVertexAttribPointer(0, size, GL11.GL_FLOAT, false, 0, 0);
            qglDrawElements(GL_TRIANGLES, count, GL_INDEX_TYPE/*GL_UNSIGNED_INT*/, tri.indexes);
//            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, tri.ambientCache.vbo);
//
//            GL11.glDrawElements(GL_TRIANGLES,
//                    r_singleTriangle.GetBool() ? 3 : tri.numIndexes,
//                    GL11.GL_UNSIGNED_BYTE, 0);
//
//            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
//            GL20.glDisableVertexAttribArray(0);
//            GL30.glBindVertexArray(0);
        }
    }

    /*
     ================
     RB_DrawShadowElementsWithCounters

     May not use all the indexes in the surface if caps are skipped
     ================
     */
    public static void RB_DrawShadowElementsWithCounters(final srfTriangles_s tri, int numIndexes) {
        backEnd.pc.c_shadowElements++;
        backEnd.pc.c_shadowIndexes += numIndexes;
        backEnd.pc.c_shadowVertexes += tri.numVerts;

        if (tri.indexCache != null && r_useIndexBuffers.GetBool()) {
            qglDrawElements(GL_TRIANGLES,
                    r_singleTriangle.GetBool() ? 3 : numIndexes,
                    GL_INDEX_TYPE,
                    vertexCache.Position(tri.indexCache));
            backEnd.pc.c_vboIndexes += numIndexes;
        } else {
            if (r_useIndexBuffers.GetBool()) {
                vertexCache.UnbindIndex();
            }
            qglDrawElements(GL_TRIANGLES,
                    r_singleTriangle.GetBool() ? 3 : numIndexes,
                    GL_INDEX_TYPE,
                    tri.indexes);
        }
    }


    /*
     ===============
     RB_RenderTriangleSurface

     Sets texcoord and vertex pointers
     ===============
     */
    public static void RB_RenderTriangleSurface(final srfTriangles_s tri) {
        if (NOT(tri.ambientCache)) {
            RB_DrawElementsImmediate(tri);
            return;
        }

        idDrawVert ac = new idDrawVert(vertexCache.Position(tri.ambientCache));//TODO:figure out how to work these damn casts.
        qglVertexPointer(3, GL_FLOAT, idDrawVert.SIZE_B, ac.xyzOffset());
        qglTexCoordPointer(2, GL_FLOAT, idDrawVert.SIZE_B, ac.stOffset());

        RB_DrawElementsWithCounters(tri);
    }

    /*
     ===============
     RB_T_RenderTriangleSurface

     ===============
     */
    public static class RB_T_RenderTriangleSurface extends triFunc {

        static final triFunc INSTANCE = new RB_T_RenderTriangleSurface();

        private RB_T_RenderTriangleSurface() {
        }

        @Override
        void run(final drawSurf_s surf) {
            RB_RenderTriangleSurface(surf.geo);
        }
    };

    /*
     ===============
     RB_EnterWeaponDepthHack
     ===============
     */
    public static void RB_EnterWeaponDepthHack() {
        qglDepthRange(0, 0.5);

        float[] matrix = new float[16];

//	memcpy( matrix, backEnd.viewDef.projectionMatrix, sizeof( matrix ) );
        System.arraycopy(backEnd.viewDef.projectionMatrix, 0, matrix, 0, matrix.length);

        qglMatrixMode(GL_PROJECTION);
        qglLoadMatrixf(matrix);
        qglMatrixMode(GL_MODELVIEW);
    }

    /*
     ===============
     RB_EnterModelDepthHack
     ===============
     */
    public static void RB_EnterModelDepthHack(float depth) {
        qglDepthRange(0.0f, 1.0f);

        float[] matrix = new float[16];

//	memcpy( matrix, backEnd.viewDef.projectionMatrix, sizeof( matrix ) );
        System.arraycopy(backEnd.viewDef.projectionMatrix, 0, matrix, 0, matrix.length);

        matrix[14] -= depth;

        qglMatrixMode(GL_PROJECTION);
        qglLoadMatrixf(matrix);
        qglMatrixMode(GL_MODELVIEW);
    }

    /*
     ===============
     RB_LeaveDepthHack
     ===============
     */
    public static void RB_LeaveDepthHack() {
        qglDepthRange(0, 1);

        qglMatrixMode(GL_PROJECTION);
        qglLoadMatrixf(backEnd.viewDef.projectionMatrix);
        qglMatrixMode(GL_MODELVIEW);
    }

    /*
     ====================
     RB_RenderDrawSurfListWithFunction

     The triangle functions can check backEnd.currentSpace != surf.space
     to see if they need to perform any new matrix setup.  The modelview
     matrix will already have been loaded, and backEnd.currentSpace will
     be updated after the triangle function completes.
     ====================
     */
    public static void RB_RenderDrawSurfListWithFunction(drawSurf_s[] drawSurfs, int numDrawSurfs, triFunc triFunc_) {
        int i;
        drawSurf_s drawSurf;

        backEnd.currentSpace = null;

        for (i = 0; i < numDrawSurfs; i++) {
            drawSurf = drawSurfs[i];

            // change the matrix if needed
            if (drawSurf.space != backEnd.currentSpace) {
                qglLoadMatrixf(drawSurf.space.modelViewMatrix);
            }

            if (drawSurf.space.weaponDepthHack) {
                RB_EnterWeaponDepthHack();
            }

            if (drawSurf.space.modelDepthHack != 0.0f) {
                RB_EnterModelDepthHack(drawSurf.space.modelDepthHack);
            }

            // change the scissor if needed
            if (r_useScissor.GetBool() && !backEnd.currentScissor.Equals(drawSurf.scissorRect)) {
                backEnd.currentScissor = drawSurf.scissorRect;
                qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
                        backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
                        backEnd.currentScissor.x2 + 1 - backEnd.currentScissor.x1,
                        backEnd.currentScissor.y2 + 1 - backEnd.currentScissor.y1);
            }

            // render it
            triFunc_.run(drawSurf);

            if (drawSurf.space.weaponDepthHack || drawSurf.space.modelDepthHack != 0.0f) {
                RB_LeaveDepthHack();
            }

            backEnd.currentSpace = drawSurf.space;
        }
    }

    /*
     ======================
     RB_RenderDrawSurfChainWithFunction
     ======================
     */
    public static void RB_RenderDrawSurfChainWithFunction(final drawSurf_s drawSurfs, triFunc triFunc_) {
        drawSurf_s drawSurf;

        backEnd.currentSpace = null;

        for (drawSurf = drawSurfs; drawSurf != null; drawSurf = drawSurf.nextOnLight) {
            // change the matrix if needed
            if (drawSurf.space != backEnd.currentSpace) {
                qglLoadMatrixf(drawSurf.space.modelViewMatrix);
            }

            if (drawSurf.space.weaponDepthHack) {
                RB_EnterWeaponDepthHack();
            }

            if (drawSurf.space.modelDepthHack != 0) {
                RB_EnterModelDepthHack(drawSurf.space.modelDepthHack);
            }

            // change the scissor if needed
            if (r_useScissor.GetBool() && !backEnd.currentScissor.Equals(drawSurf.scissorRect)) {
                backEnd.currentScissor = drawSurf.scissorRect;
                qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
                        backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
                        backEnd.currentScissor.x2 + 1 - backEnd.currentScissor.x1,
                        backEnd.currentScissor.y2 + 1 - backEnd.currentScissor.y1);
            }

            // render it
            triFunc_.run(drawSurf);

            if (drawSurf.space.weaponDepthHack || drawSurf.space.modelDepthHack != 0.0f) {
                RB_LeaveDepthHack();
            }

            backEnd.currentSpace = drawSurf.space;
        }
    }

    /*
     ======================
     RB_GetShaderTextureMatrix
     ======================
     */private static int DBG_RB_GetShaderTextureMatrix = 0;
    public static void RB_GetShaderTextureMatrix(final float[] shaderRegisters, final textureStage_t texture, float[] matrix/*[16]*/) {
        matrix[0] = shaderRegisters[texture.matrix[0][0]];
        matrix[4] = shaderRegisters[texture.matrix[0][1]];
        matrix[8] = 0;
        matrix[12] = shaderRegisters[texture.matrix[0][2]];
        
        DBG_RB_GetShaderTextureMatrix++;
//        System.out.println(">>>>>>" + DBG_RB_GetShaderTextureMatrix);
//        System.out.println("0:" + Arrays.toString(texture.matrix[0]));
//        System.out.println("1:" + Arrays.toString(texture.matrix[1]));
//        System.out.println("<<<<<<" + DBG_RB_GetShaderTextureMatrix);

        // we attempt to keep scrolls from generating incredibly large texture values, but
        // center rotations and center scales can still generate offsets that need to be > 1
        if (matrix[12] < -40 || matrix[12] > 40) {
            matrix[12] -= (int) matrix[12];
        }

        matrix[1] = shaderRegisters[texture.matrix[1][0]];
        matrix[5] = shaderRegisters[texture.matrix[1][1]];
        matrix[9] = 0;
        matrix[13] = shaderRegisters[texture.matrix[1][2]];
        if (matrix[13] < -40 || matrix[13] > 40) {
            matrix[13] -= (int) matrix[13];
        }

        matrix[2] = 0;
        matrix[6] = 0;
        matrix[10] = 1;
        matrix[14] = 0;

        matrix[3] = 0;
        matrix[7] = 0;
        matrix[11] = 0;
        matrix[15] = 1;
    }

    /*
     ======================
     RB_LoadShaderTextureMatrix
     ======================
     */
    public static void RB_LoadShaderTextureMatrix(final float[] shaderRegisters, final textureStage_t texture) {
        final float[] matrix = new float[16];

        RB_GetShaderTextureMatrix(shaderRegisters, texture, matrix);
//        final float[] m = matrix;
//        System.out.printf("RB_LoadShaderTextureMatrix("
//                + "%f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f)\n",
//                m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10], m[11], m[12], m[13], m[14], m[15]);
        
//        TempDump.printCallStack("------->" + (DBG_RB_LoadShaderTextureMatrix++));
        qglMatrixMode(GL_TEXTURE);
        qglLoadMatrixf(matrix);
        qglMatrixMode(GL_MODELVIEW);
    }
    
    /*
     ======================
     RB_BindVariableStageImage

     Handles generating a cinematic frame if needed
     ======================
     */ private static int DBG_RB_BindVariableStageImage = 0;
    public static void RB_BindVariableStageImage(final textureStage_t texture, final float[] shaderRegisters) {
        DBG_RB_BindVariableStageImage++;
//        if (DBG_RB_BindVariableStageImage == 50) {
//            for (drawSurf_s draw : backEnd.viewDef.drawSurfs) {
//                System.out.println("=============================");
//                for (int s = 0; draw != null && s < draw.material.GetNumStages(); s++) {
//                    if(draw.material.GetStage(s).texture.image[0]!=null)
//                        System.out.println("ss::"+draw.material.GetStage(s).texture.image[0].texNum);
//                }
//            }
//        }
        if (texture.cinematic[0] != null) {
            cinData_t cin;

            if (r_skipDynamicTextures.GetBool()) {
                globalImages.defaultImage.Bind();
                return;
            }

            // offset time by shaderParm[7] (FIXME: make the time offset a parameter of the shader?)
            // We make no attempt to optimize for multiple identical cinematics being in view, or
            // for cinematics going at a lower framerate than the renderer.
            cin = texture.cinematic[0].ImageForTime((int) (1000 * (backEnd.viewDef.floatTime + backEnd.viewDef.renderView.shaderParms[11])));

            if (cin.image != null) {
                globalImages.cinematicImage.UploadScratch(cin.image, cin.imageWidth, cin.imageHeight);
            } else {
                globalImages.blackImage.Bind();
            }
        } else {
            //FIXME: see why image is invalid
            if (texture.image[0] != null) {
//                final int titty = texture.image[0].texNum;
//                if (titty != 58) return;
                texture.image[0].Bind();
            }
        }
    }

    /*
     ======================
     RB_BindStageTexture
     ======================
     */
    public static void RB_BindStageTexture(final float[] shaderRegisters, final textureStage_t texture, final drawSurf_s surf) {
        // image
        RB_BindVariableStageImage(texture, shaderRegisters);

        // texgens
        if (texture.texgen == TG_DIFFUSE_CUBE) {
            idDrawVert vert = new idDrawVert(vertexCache.Position(surf.geo.ambientCache));//TODO:figure out how to work these damn casts.
            qglTexCoordPointer(3, GL_FLOAT, idDrawVert.SIZE_B, vert.normal.ToFloatPtr());

        }
        if (texture.texgen == TG_SKYBOX_CUBE || texture.texgen == TG_WOBBLESKY_CUBE) {
            for (vertCache_s dynamicTexCoords : surf.dynamicTexCoords) {
                qglTexCoordPointer(3, GL_FLOAT, 0, vertexCache.Position(dynamicTexCoords));
            }
        }
        if (texture.texgen == TG_REFLECT_CUBE) {
            qglEnable(GL_TEXTURE_GEN_S);
            qglEnable(GL_TEXTURE_GEN_T);
            qglEnable(GL_TEXTURE_GEN_R);
            qglTexGenf(GL_S, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP/*_EXT*/);
            qglTexGenf(GL_T, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP/*_EXT*/);
            qglTexGenf(GL_R, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP/*_EXT*/);
            qglEnableClientState(GL_NORMAL_ARRAY);
            idDrawVert vert = new idDrawVert(vertexCache.Position(surf.geo.ambientCache));// {//TODO:figure out how to work these damn casts.
            qglNormalPointer(GL_FLOAT, idDrawVert.SIZE_B, vert.normalOffset());

            qglMatrixMode(GL_TEXTURE);
            float[] mat = new float[16];

            R_TransposeGLMatrix(backEnd.viewDef.worldSpace.modelViewMatrix, mat);

            qglLoadMatrixf(mat);
            qglMatrixMode(GL_MODELVIEW);
        }

        // matrix
        if (texture.hasMatrix) {
            RB_LoadShaderTextureMatrix(shaderRegisters, texture);
        }
    }

    /*
     ======================
     RB_FinishStageTexture
     ======================
     */
    public static void RB_FinishStageTexture(final textureStage_t texture, final drawSurf_s surf) {
        if (texture.texgen == TG_DIFFUSE_CUBE || texture.texgen == TG_SKYBOX_CUBE
                || texture.texgen == TG_WOBBLESKY_CUBE) {
            idDrawVert vert = new idDrawVert(vertexCache.Position(surf.geo.ambientCache));// {//TODO:figure out how to work these damn casts.
            qglTexCoordPointer(2, GL_FLOAT, idDrawVert.SIZE_B,
                    //			(void *)&(((idDrawVert *)vertexCache.Position( surf.geo.ambientCache )).st) );
                    vert.st.ToFloatPtr());//TODO:WDF?
        }

        if (texture.texgen == TG_REFLECT_CUBE) {
            qglDisable(GL_TEXTURE_GEN_S);
            qglDisable(GL_TEXTURE_GEN_T);
            qglDisable(GL_TEXTURE_GEN_R);
            qglTexGenf(GL_S, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            qglTexGenf(GL_T, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            qglTexGenf(GL_R, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
            qglDisableClientState(GL_NORMAL_ARRAY);

            qglMatrixMode(GL_TEXTURE);
            qglLoadIdentity();
            qglMatrixMode(GL_MODELVIEW);
        }

        if (texture.hasMatrix) {
            qglMatrixMode(GL_TEXTURE);
            qglLoadIdentity();
            qglMatrixMode(GL_MODELVIEW);
        }
    }

//=============================================================================================
    /*
     =================
     RB_DetermineLightScale

     Sets:
     backEnd.lightScale
     backEnd.overBright

     Find out how much we are going to need to overscale the lighting, so we
     can down modulate the pre-lighting passes.

     We only look at light calculations, but an argument could be made that
     we should also look at surface evaluations, which would let surfaces
     overbright past 1.0
     =================
     */
    public static void RB_DetermineLightScale() {
        viewLight_s vLight;
        idMaterial shader;
        float max;
        int i, j, numStages;
        shaderStage_t stage;

        // the light scale will be based on the largest color component of any surface
        // that will be drawn.
        // should we consider separating rgb scales?
        // if there are no lights, this will remain at 1.0, so GUI-only
        // rendering will not lose any bits of precision
        max = 1.0f;

        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            // lights with no surfaces or shaderparms may still be present
            // for debug display
            if (null == vLight.localInteractions[0] && null == vLight.globalInteractions[0]
                    && null == vLight.translucentInteractions[0]) {
                continue;
            }

            shader = vLight.lightShader;
            numStages = shader.GetNumStages();
            for (i = 0; i < numStages; i++) {
                stage = shader.GetStage(i);
                for (j = 0; j < 3; j++) {
                    float v = r_lightScale.GetFloat() * vLight.shaderRegisters[stage.color.registers[j]];
                    if (v > max) {
                        max = v;
                    }
                }
            }
        }

        backEnd.pc.maxLightValue = max;
        if (max <= tr.backEndRendererMaxLight) {
            backEnd.lightScale = r_lightScale.GetFloat();
            backEnd.overBright = 1.0f;
        } else {
            backEnd.lightScale = r_lightScale.GetFloat() * tr.backEndRendererMaxLight / max;
            backEnd.overBright = max / tr.backEndRendererMaxLight;
        }
    }


    /*
     =================
     RB_BeginDrawingView

     Any mirrored or portaled views have already been drawn, so prepare
     to actually render the visible surfaces for this view
     =================
     */
    public static void RB_BeginDrawingView() {
        // set the modelview matrix for the viewer
        qglMatrixMode(GL_PROJECTION);
        qglLoadMatrixf(backEnd.viewDef.projectionMatrix);
        qglMatrixMode(GL_MODELVIEW);

        // set the window clipping
        qglViewport(tr.viewportOffset[0] + backEnd.viewDef.viewport.x1,
                tr.viewportOffset[1] + backEnd.viewDef.viewport.y1,
                backEnd.viewDef.viewport.x2 + 1 - backEnd.viewDef.viewport.x1,
                backEnd.viewDef.viewport.y2 + 1 - backEnd.viewDef.viewport.y1);

        // the scissor may be smaller than the viewport for subviews
        qglScissor(tr.viewportOffset[0] + backEnd.viewDef.viewport.x1 + backEnd.viewDef.scissor.x1,
                tr.viewportOffset[1] + backEnd.viewDef.viewport.y1 + backEnd.viewDef.scissor.y1,
                backEnd.viewDef.scissor.x2 + 1 - backEnd.viewDef.scissor.x1,
                backEnd.viewDef.scissor.y2 + 1 - backEnd.viewDef.scissor.y1);
        backEnd.currentScissor = backEnd.viewDef.scissor;

        // ensures that depth writes are enabled for the depth clear
        GL_State(GLS_DEFAULT);

        // we don't have to clear the depth / stencil buffer for 2D rendering
        if (backEnd.viewDef.viewEntitys != null) {
            qglStencilMask(0xff);
            // some cards may have 7 bit stencil buffers, so don't assume this
            // should be 128
            qglClearStencil(1 << (glConfig.stencilBits - 1));
            qglClear(GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
            qglEnable(GL_DEPTH_TEST);
        } else {
            qglDisable(GL_DEPTH_TEST);
            qglDisable(GL_STENCIL_TEST);
        }

        backEnd.glState.faceCulling = -1;		// force face culling to set next time
        GL_Cull(CT_FRONT_SIDED);
    }

    /*
     ==================
     R_SetDrawInteractions
     ==================
     */
    public static void R_SetDrawInteraction(final shaderStage_t surfaceStage, final float[] surfaceRegs, idImage[] image, idVec4[] matrix/*[2]*/, idVec4 color/*[4]*/) {
        image[0] = surfaceStage.texture.image[0];
        if (surfaceStage.texture.hasMatrix) {
            matrix[0].oSet(0, surfaceRegs[surfaceStage.texture.matrix[0][0]]);
            matrix[0].oSet(1, surfaceRegs[surfaceStage.texture.matrix[0][1]]);
            matrix[0].oSet(2, 0);
            matrix[0].oSet(3, surfaceRegs[surfaceStage.texture.matrix[0][2]]);

            matrix[1].oSet(0, surfaceRegs[surfaceStage.texture.matrix[1][0]]);
            matrix[1].oSet(1, surfaceRegs[surfaceStage.texture.matrix[1][1]]);
            matrix[1].oSet(2, 0);
            matrix[1].oSet(3, surfaceRegs[surfaceStage.texture.matrix[1][2]]);

            // we attempt to keep scrolls from generating incredibly large texture values, but
            // center rotations and center scales can still generate offsets that need to be > 1
            if (matrix[0].oGet(3) < -40 || matrix[0].oGet(3) > 40) {
                matrix[0].oMinSet(3, (int) matrix[0].oGet(3));
            }
            if (matrix[1].oGet(3) < -40 || matrix[1].oGet(3) > 40) {
                matrix[1].oMinSet(3, (int) matrix[1].oGet(3));
            }
        } else {
            matrix[0].oSet(0, 1);
            matrix[0].oSet(1, 0);
            matrix[0].oSet(2, 0);
            matrix[0].oSet(3, 0);

            matrix[1].oSet(0, 0);
            matrix[1].oSet(1, 1);
            matrix[1].oSet(2, 0);
            matrix[1].oSet(3, 0);
        }

        if (color != null) {
            for (int i = 0; i < 4; i++) {
                color.oSet(i, surfaceRegs[surfaceStage.color.registers[i]]);
                // clamp here, so card with greater range don't look different.
                // we could perform overbrighting like we do for lights, but
                // it doesn't currently look worth it.
                if (color.oGet(i) < 0) {
                    color.oSet(i, 0);
                } else if (color.oGet(i) > 1.0) {
                    color.oSet(i, 1.0f);
                }
            }
        }
    }

    /*
     =================
     RB_SubmittInteraction
     =================
     */private static int DBG_RB_SubmittInteraction=0;
    public static void RB_SubmittInteraction(drawInteraction_t din, DrawInteraction drawInteraction) {
        if (null == din.bumpImage) {
            return;
        }

        if (null == din.diffuseImage || r_skipDiffuse.GetBool()) {
            din.diffuseImage = globalImages.blackImage;
        }
        if (null == din.specularImage || r_skipSpecular.GetBool() || din.ambientLight != 0) {
            din.specularImage = globalImages.blackImage;
        }
        if (null == din.bumpImage || r_skipBump.GetBool()) {
            din.bumpImage = globalImages.flatNormalMap;
        }

        // if we wouldn't draw anything, don't call the Draw function
        if (((din.diffuseColor.oGet(0) > 0
                || din.diffuseColor.oGet(1) > 0
                || din.diffuseColor.oGet(2) > 0) && din.diffuseImage != globalImages.blackImage)
                || ((din.specularColor.oGet(0) > 0
                || din.specularColor.oGet(1) > 0
                || din.specularColor.oGet(2) > 0) && din.specularImage != globalImages.blackImage)) {
            DBG_RB_SubmittInteraction++;
            drawInteraction.run(din);
        }
    }

    /*
     =============
     RB_CreateSingleDrawInteractions

     This can be used by different draw_* backends to decompose a complex light / surface
     interaction into primitive interactions
     =============
     */
    public static void RB_CreateSingleDrawInteractions(final drawSurf_s surf, DrawInteraction drawInteraction) {
        final idMaterial surfaceShader = surf.material;
        final float[] surfaceRegs = surf.shaderRegisters;
        final viewLight_s vLight = backEnd.vLight;
        final idMaterial lightShader = vLight.lightShader;
        final float[] lightRegs = vLight.shaderRegisters;
        drawInteraction_t inter = new drawInteraction_t();

        if (r_skipInteractions.GetBool() || NOT(surf.geo) || NOT(surf.geo.ambientCache)) {
            return;
        }

        if (tr.logFile != null) {
            RB_LogComment("---------- RB_CreateSingleDrawInteractions %s on %s ----------\n", lightShader.GetName(), surfaceShader.GetName());
        }

        // change the matrix and light projection vectors if needed
        if (surf.space != backEnd.currentSpace) {
            backEnd.currentSpace = surf.space;
            qglLoadMatrixf(surf.space.modelViewMatrix);
        }

        // change the scissor if needed
        if (r_useScissor.GetBool() && !backEnd.currentScissor.Equals(surf.scissorRect)) {
            backEnd.currentScissor = surf.scissorRect;
            qglScissor(backEnd.viewDef.viewport.x1 + backEnd.currentScissor.x1,
                    backEnd.viewDef.viewport.y1 + backEnd.currentScissor.y1,
                    backEnd.currentScissor.x2 + 1 - backEnd.currentScissor.x1,
                    backEnd.currentScissor.y2 + 1 - backEnd.currentScissor.y1);
        }

        // hack depth range if needed
        if (surf.space.weaponDepthHack) {
            RB_EnterWeaponDepthHack();
        }

        if (surf.space.modelDepthHack != 0) {
            RB_EnterModelDepthHack(surf.space.modelDepthHack);
        }

        inter.surf = surf;
        inter.lightFalloffImage = vLight.falloffImage;

        R_GlobalPointToLocal(surf.space.modelMatrix, vLight.globalLightOrigin, inter.localLightOrigin);
        R_GlobalPointToLocal(surf.space.modelMatrix, backEnd.viewDef.renderView.vieworg, inter.localViewOrigin);
        inter.localLightOrigin.oSet(3, 0);
        inter.localViewOrigin.oSet(3, 1);
        inter.ambientLight = btoi(lightShader.IsAmbientLight());

        // the base projections may be modified by texture matrix on light stages
        idPlane[] lightProject = R_ClearedStaticAlloc(4, idPlane.class);
        for (int i = 0; i < 4; i++) {
            R_GlobalPlaneToLocal(surf.space.modelMatrix, backEnd.vLight.lightProject[i], lightProject[i]);
        }

        for (int lightStageNum = 0; lightStageNum < lightShader.GetNumStages(); lightStageNum++) {
            final shaderStage_t lightStage = lightShader.GetStage(lightStageNum);

            // ignore stages that fail the condition
            if (0 == lightRegs[lightStage.conditionRegister]) {
                continue;
            }

            inter.lightImage = lightStage.texture.image[0];//TODO:pointeR?

//            memcpy(inter.lightProjection, lightProject, sizeof(inter.lightProjection));
            for (int i = 0; i < inter.lightProjection.length; i++) {
                inter.lightProjection[i] = lightProject[i].ToVec4();
            }
            // now multiply the texgen by the light texture matrix
            if (lightStage.texture.hasMatrix) {
                RB_GetShaderTextureMatrix(lightRegs, lightStage.texture, backEnd.lightTextureMatrix);
                {
                    idPlane[] planes = new idPlane[inter.lightProjection.length];
                    for (int a = 0; a < planes.length; a++) {
                        planes[a] = new idPlane(inter.lightProjection[a]);
                    }
                    RB_BakeTextureMatrixIntoTexgen( /*reinterpret_cast<class idPlane *>*/planes, backEnd.lightTextureMatrix);
                }
            }

            inter.bumpImage = null;
            inter.specularImage = null;
            inter.diffuseImage = null;
            inter.diffuseColor.Set(0, 0, 0, 0);
            inter.specularColor.Set(0, 0, 0, 0);

            float[] lightColor = new float[4];

            // backEnd.lightScale is calculated so that lightColor[] will never exceed
            // tr.backEndRendererMaxLight
            lightColor[0] = backEnd.lightScale * lightRegs[lightStage.color.registers[0]];
            lightColor[1] = backEnd.lightScale * lightRegs[lightStage.color.registers[1]];
            lightColor[2] = backEnd.lightScale * lightRegs[lightStage.color.registers[2]];
            lightColor[3] = lightRegs[lightStage.color.registers[3]];

            // go through the individual stages
            for (int surfaceStageNum = 0; surfaceStageNum < surfaceShader.GetNumStages(); surfaceStageNum++) {
                final shaderStage_t surfaceStage = surfaceShader.GetStage(surfaceStageNum);

                switch (surfaceStage.lighting) {
                    case SL_AMBIENT: {
                        // ignore ambient stages while drawing interactions
                        break;
                    }
                    case SL_BUMP: {
                        // ignore stage that fails the condition
                        if (0 == surfaceRegs[surfaceStage.conditionRegister]) {
                            break;
                        }
                        // draw any previous interaction
                        RB_SubmittInteraction(inter, drawInteraction);
                        inter.diffuseImage = null;
                        inter.specularImage = null;
                        {
                            idImage[] bumpImage = {null};
                            R_SetDrawInteraction(surfaceStage, surfaceRegs, bumpImage, inter.bumpMatrix, null);
                            inter.bumpImage = bumpImage[0];
                        }
                        break;
                    }
                    case SL_DIFFUSE: {
                        // ignore stage that fails the condition
                        if (0 == surfaceRegs[surfaceStage.conditionRegister]) {
                            break;
                        }
                        if (inter.diffuseImage != null) {
                            RB_SubmittInteraction(inter, drawInteraction);
                        }
                        {
                            idImage[] diffuseImage = {null};
                            R_SetDrawInteraction(surfaceStage, surfaceRegs, diffuseImage, inter.diffuseMatrix, inter.diffuseColor);
                            inter.diffuseImage = diffuseImage[0];
                        }
                        inter.diffuseColor.oMulSet(0, lightColor[0]);
                        inter.diffuseColor.oMulSet(2, lightColor[2]);
                        inter.diffuseColor.oMulSet(1, lightColor[1]);
                        inter.diffuseColor.oMulSet(3, lightColor[3]);
                        inter.vertexColor = surfaceStage.vertexColor;
                        break;
                    }
                    case SL_SPECULAR: {
                        // ignore stage that fails the condition
                        if (0 == surfaceRegs[surfaceStage.conditionRegister]) {
                            break;
                        }
                        if (inter.specularImage != null) {
                            RB_SubmittInteraction(inter, drawInteraction);
                        }
                        {
                            idImage[] specularImage = {null};
                            R_SetDrawInteraction(surfaceStage, surfaceRegs, specularImage, inter.specularMatrix, inter.specularColor);
                            inter.specularImage = specularImage[0];
                        }
                        inter.specularColor.oMulSet(0, lightColor[0]);
                        inter.specularColor.oMulSet(1, lightColor[1]);
                        inter.specularColor.oMulSet(2, lightColor[2]);
                        inter.specularColor.oMulSet(3, lightColor[3]);
                        inter.vertexColor = surfaceStage.vertexColor;
                        break;
                    }
                }
            }

            // draw the final interaction
            RB_SubmittInteraction(inter, drawInteraction);
        }

        // unhack depth range if needed
        if (surf.space.weaponDepthHack || surf.space.modelDepthHack != 0.0f) {
            RB_LeaveDepthHack();
        }
    }

    /*
     =============
     RB_DrawView
     =============
     */
    public static void RB_DrawView(final Object data) {
        final drawSurfsCommand_t cmd;

        cmd = (drawSurfsCommand_t) data;

        backEnd.viewDef = cmd.viewDef;

        // we will need to do a new copyTexSubImage of the screen
        // when a SS_POST_PROCESS material is used
        backEnd.currentRenderCopied = false;

        // if there aren't any drawsurfs, do nothing
        if (0 == backEnd.viewDef.numDrawSurfs) {
            return;
        }

        // skip render bypasses everything that has models, assuming
        // them to be 3D views, but leaves 2D rendering visible
        if (r_skipRender.GetBool() && backEnd.viewDef.viewEntitys != null) {
            return;
        }

        // skip render context sets the wgl context to NULL,
        // which should factor out the API cost, under the assumption
        // that all gl calls just return if the context isn't valid
        if (r_skipRenderContext.GetBool() && backEnd.viewDef.viewEntitys != null) {
            GLimp_DeactivateContext();
        }

        backEnd.pc.c_surfaces += backEnd.viewDef.numDrawSurfs;

        RB_ShowOverdraw();

        // render the scene, jumping to the hardware specific interaction renderers
        RB_STD_DrawView();

        // restore the context for 2D drawing if we were stubbing it out
        if (r_skipRenderContext.GetBool() && backEnd.viewDef.viewEntitys != null) {
            GLimp_ActivateContext();
            RB_SetDefaultGLState();
        }
    }
}
