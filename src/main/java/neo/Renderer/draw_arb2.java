package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.RenderSystem_init.r_skipTranslucent;
import static neo.Renderer.RenderSystem_init.r_testARBProgram;
import static neo.Renderer.RenderSystem_init.r_useScissor;
import static neo.Renderer.RenderSystem_init.r_useShadowVertexProgram;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.draw_common.RB_StencilShadowPass;
import static neo.Renderer.tr_backend.GL_SelectTexture;
import static neo.Renderer.tr_backend.GL_State;
import static neo.Renderer.tr_backend.RB_LogComment;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_EQUAL;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_LESS;
import static neo.Renderer.tr_local.GLS_DEPTHMASK;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE;
import static neo.Renderer.tr_local.backEnd;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.programParameter_t.PP_BUMP_MATRIX_S;
import static neo.Renderer.tr_local.programParameter_t.PP_BUMP_MATRIX_T;
import static neo.Renderer.tr_local.programParameter_t.PP_COLOR_ADD;
import static neo.Renderer.tr_local.programParameter_t.PP_COLOR_MODULATE;
import static neo.Renderer.tr_local.programParameter_t.PP_DIFFUSE_MATRIX_S;
import static neo.Renderer.tr_local.programParameter_t.PP_DIFFUSE_MATRIX_T;
import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_FALLOFF_S;
import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_ORIGIN;
import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_PROJECT_Q;
import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_PROJECT_S;
import static neo.Renderer.tr_local.programParameter_t.PP_LIGHT_PROJECT_T;
import static neo.Renderer.tr_local.programParameter_t.PP_SPECULAR_MATRIX_S;
import static neo.Renderer.tr_local.programParameter_t.PP_SPECULAR_MATRIX_T;
import static neo.Renderer.tr_local.programParameter_t.PP_VIEW_ORIGIN;
import static neo.Renderer.tr_local.program_t.FPROG_AMBIENT;
import static neo.Renderer.tr_local.program_t.FPROG_BUMPY_ENVIRONMENT;
import static neo.Renderer.tr_local.program_t.FPROG_ENVIRONMENT;
import static neo.Renderer.tr_local.program_t.FPROG_GLASSWARP;
import static neo.Renderer.tr_local.program_t.FPROG_INTERACTION;
import static neo.Renderer.tr_local.program_t.FPROG_TEST;
import static neo.Renderer.tr_local.program_t.PROG_INVALID;
import static neo.Renderer.tr_local.program_t.PROG_USER;
import static neo.Renderer.tr_local.program_t.VPROG_AMBIENT;
import static neo.Renderer.tr_local.program_t.VPROG_BUMPY_ENVIRONMENT;
import static neo.Renderer.tr_local.program_t.VPROG_ENVIRONMENT;
import static neo.Renderer.tr_local.program_t.VPROG_GLASSWARP;
import static neo.Renderer.tr_local.program_t.VPROG_INTERACTION;
import static neo.Renderer.tr_local.program_t.VPROG_NV20_BUMP_AND_LIGHT;
import static neo.Renderer.tr_local.program_t.VPROG_NV20_DIFFUSE_AND_SPECULAR_COLOR;
import static neo.Renderer.tr_local.program_t.VPROG_NV20_DIFFUSE_COLOR;
import static neo.Renderer.tr_local.program_t.VPROG_NV20_SPECULAR_COLOR;
import static neo.Renderer.tr_local.program_t.VPROG_R200_INTERACTION;
import static neo.Renderer.tr_local.program_t.VPROG_STENCIL_SHADOW;
import static neo.Renderer.tr_local.program_t.VPROG_TEST;
import static neo.Renderer.tr_render.RB_CreateSingleDrawInteractions;
import static neo.Renderer.tr_render.RB_DrawElementsWithCounters;
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.Common.common;
import static neo.idlib.Lib.idLib.fileSystem;
import static neo.open.gl.QGL.qglActiveTextureARB;
import static neo.open.gl.QGL.qglBindProgramARB;
import static neo.open.gl.QGL.qglClear;
import static neo.open.gl.QGL.qglColorPointer;
import static neo.open.gl.QGL.qglDisable;
import static neo.open.gl.QGL.qglDisableClientState;
import static neo.open.gl.QGL.qglDisableVertexAttribArrayARB;
import static neo.open.gl.QGL.qglEnable;
import static neo.open.gl.QGL.qglEnableClientState;
import static neo.open.gl.QGL.qglEnableVertexAttribArrayARB;
import static neo.open.gl.QGL.qglGetError;
import static neo.open.gl.QGL.qglGetIntegerv;
import static neo.open.gl.QGL.qglGetString;
import static neo.open.gl.QGL.qglProgramEnvParameter4fvARB;
import static neo.open.gl.QGL.qglProgramStringARB;
import static neo.open.gl.QGL.qglScissor;
import static neo.open.gl.QGL.qglStencilFunc;
import static neo.open.gl.QGL.qglVertexAttribPointerARB;
import static neo.open.gl.QGL.qglVertexPointer;
import static neo.open.gl.QGLConstantsIfc.GL_ALWAYS;
import static neo.open.gl.QGLConstantsIfc.GL_COLOR_ARRAY;
import static neo.open.gl.QGLConstantsIfc.GL_FLOAT;
import static neo.open.gl.QGLConstantsIfc.GL_FRAGMENT_PROGRAM_ARB;
import static neo.open.gl.QGLConstantsIfc.GL_INVALID_OPERATION;
import static neo.open.gl.QGLConstantsIfc.GL_PROGRAM_ERROR_POSITION_ARB;
import static neo.open.gl.QGLConstantsIfc.GL_PROGRAM_ERROR_STRING_ARB;
import static neo.open.gl.QGLConstantsIfc.GL_PROGRAM_FORMAT_ASCII_ARB;
import static neo.open.gl.QGLConstantsIfc.GL_STENCIL_BUFFER_BIT;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE0_ARB;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_COORD_ARRAY;
import static neo.open.gl.QGLConstantsIfc.GL_UNSIGNED_BYTE;
import static neo.open.gl.QGLConstantsIfc.GL_VERTEX_PROGRAM_ARB;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import neo.TempDump.TODO_Exception;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.idScreenRect;
import neo.Renderer.tr_local.program_t;
import neo.Renderer.tr_local.viewLight_s;
import neo.Renderer.tr_render.DrawInteraction;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.open.Nio;

/**
 *
 */
public class draw_arb2 {

    public static void cg_error_callback() {
        throw new TODO_Exception();
//        CGerror i = cgGetError();
//        common.Printf("Cg error (%d): %s\n", i, cgGetErrorString(i));
    }

    /*
     =========================================================================================

     GENERAL INTERACTION RENDERING

     =========================================================================================
     */

    /*
     ====================
     GL_SelectTextureNoClient
     ====================
     */
    public static void GL_SelectTextureNoClient(int unit) {
        backEnd.glState.currenttmu = unit;
        qglActiveTextureARB(GL_TEXTURE0_ARB + unit);
        RB_LogComment("glActiveTextureARB( %d )\n", unit);
    }
//        

    /*
     ==================
     RB_ARB2_DrawInteraction
     ==================
     */
    //
    private static final FloatBuffer ZERO = createFloatBuffer(0);
    private static final FloatBuffer ONE = createFloatBuffer(1);
    private static final FloatBuffer NEG_ONE = createFloatBuffer(-1);

    static FloatBuffer createFloatBuffer(int n) {
    	return (FloatBuffer) Nio.newFloatBuffer(4).put(n).put(n).put(n).put(n).flip();
    }

    static class RB_ARB2_DrawInteraction extends DrawInteraction {

        static final DrawInteraction INSTANCE = new RB_ARB2_DrawInteraction();
        private static int DBG_RB_ARB2_DrawInteraction = 0;

        private RB_ARB2_DrawInteraction() {
        }

        @Override
        void run(tr_local.drawInteraction_t din) {
            DBG_RB_ARB2_DrawInteraction++;
            // load all the vertex program parameters
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_ORIGIN, din.localLightOrigin.toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_VIEW_ORIGIN, din.localViewOrigin.toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_PROJECT_S, din.lightProjection[0].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_PROJECT_T, din.lightProjection[1].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_PROJECT_Q, din.lightProjection[2].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_LIGHT_FALLOFF_S, din.lightProjection[3].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_BUMP_MATRIX_S, din.bumpMatrix[0].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_BUMP_MATRIX_T, din.bumpMatrix[1].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_DIFFUSE_MATRIX_S, din.diffuseMatrix[0].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_DIFFUSE_MATRIX_T, din.diffuseMatrix[1].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_SPECULAR_MATRIX_S, din.specularMatrix[0].toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_SPECULAR_MATRIX_T, din.specularMatrix[1].toFloatBuffer());

            // testing fragment based normal mapping
            if (r_testARBProgram.GetBool()) {
                qglProgramEnvParameter4fvARB(GL_FRAGMENT_PROGRAM_ARB, 2, din.localLightOrigin.toFloatBuffer());
                qglProgramEnvParameter4fvARB(GL_FRAGMENT_PROGRAM_ARB, 3, din.localViewOrigin.toFloatBuffer());
            }

            switch (din.vertexColor) {
                case SVC_IGNORE:
                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_MODULATE, ZERO);
                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_ADD, ONE);
                    break;
                case SVC_MODULATE:
                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_MODULATE, ONE);
                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_ADD, ZERO);
                    break;
                case SVC_INVERSE_MODULATE:
                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_MODULATE, NEG_ONE);
                    qglProgramEnvParameter4fvARB(GL_VERTEX_PROGRAM_ARB, PP_COLOR_ADD, ONE);
                    break;
            }

            // set the constant colors
            qglProgramEnvParameter4fvARB(GL_FRAGMENT_PROGRAM_ARB, 0, din.diffuseColor.toFloatBuffer());
            qglProgramEnvParameter4fvARB(GL_FRAGMENT_PROGRAM_ARB, 1, din.specularColor.toFloatBuffer());

            // set the textures
            // texture 1 will be the per-surface bump map
            GL_SelectTextureNoClient(1);
            din.bumpImage.Bind();

            // texture 2 will be the light falloff texture
            GL_SelectTextureNoClient(2);
            din.lightFalloffImage.Bind();

            // texture 3 will be the light projection texture
            GL_SelectTextureNoClient(3);
            din.lightImage.Bind();

            // texture 4 is the per-surface diffuse map
            GL_SelectTextureNoClient(4);
            din.diffuseImage.Bind();

            // texture 5 is the per-surface specular map
            GL_SelectTextureNoClient(5);
            din.specularImage.Bind();

            // draw it
            RB_DrawElementsWithCounters(din.surf.geo);
        }
    }


    /*
     =============
     RB_ARB2_CreateDrawInteractions

     =============
     */
    public static void RB_ARB2_CreateDrawInteractions(drawSurf_s surf) {
        if (NOT(surf)) {
            return;
        }

        // perform setup here that will be constant for all interactions
        GL_State(GLS_SRCBLEND_ONE | GLS_DSTBLEND_ONE | GLS_DEPTHMASK | backEnd.depthFunc);

        // bind the vertex program
        if (RenderSystem_init.r_testARBProgram.GetBool()) {
            qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_TEST);
            qglBindProgramARB(GL_FRAGMENT_PROGRAM_ARB, FPROG_TEST);
        } else {
            qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_INTERACTION);
            qglBindProgramARB(GL_FRAGMENT_PROGRAM_ARB, FPROG_INTERACTION);
        }

        qglEnable(GL_VERTEX_PROGRAM_ARB);
        qglEnable(GL_FRAGMENT_PROGRAM_ARB);

        // enable the vertex arrays
        qglEnableVertexAttribArrayARB(8);
        qglEnableVertexAttribArrayARB(9);
        qglEnableVertexAttribArrayARB(10);
        qglEnableVertexAttribArrayARB(11);
        qglEnableClientState(GL_COLOR_ARRAY);

        // texture 0 is the normalization cube map for the vector towards the light
        GL_SelectTextureNoClient(0);
        if (backEnd.vLight.lightShader.IsAmbientLight()) {
            globalImages.ambientNormalMap.Bind();
        } else {
            globalImages.normalCubeMapImage.Bind();
        }

        // texture 6 is the specular lookup table
        GL_SelectTextureNoClient(6);
        if (RenderSystem_init.r_testARBProgram.GetBool()) {
            globalImages.specular2DTableImage.Bind();	// variable specularity in alpha channel
        } else {
            globalImages.specularTableImage.Bind();
        }

        for (; surf != null; surf = surf.nextOnLight) {
            // perform setup here that will not change over multiple interaction passes

            // set the vertex pointers
            final idDrawVert ac = new idDrawVert(vertexCache.Position(surf.geo.ambientCache));//TODO:figure out how to work these damn casts.
//            qglColorPointer(4, GL_UNSIGNED_BYTE, 0/*sizeof(idDrawVert)*/, ac.colorOffset());
//            qglVertexAttribPointerARB(11, 3, GL_FLOAT, false, 0/*sizeof(idDrawVert)*/, ac.normalOffset());
//            qglVertexAttribPointerARB(10, 3, GL_FLOAT, false, 0/*sizeof(idDrawVert)*/, ac.tangentsOffset_1());
//            qglVertexAttribPointerARB(9, 3, GL_FLOAT, false, 0/*sizeof(idDrawVert)*/, ac.tangentsOffset_0());
//            qglVertexAttribPointerARB(8, 2, GL_FLOAT, false, 0/*sizeof(idDrawVert)*/, ac.stOffset());
//            qglVertexPointer(3, GL_FLOAT, 0/*sizeof(idDrawVert)*/, ac.xyzOffset());

            qglColorPointer(4, GL_UNSIGNED_BYTE, idDrawVert.BYTES, ac.colorOffset());
            qglVertexAttribPointerARB(11, 3, GL_FLOAT, false, idDrawVert.BYTES, ac.normalOffset());
            qglVertexAttribPointerARB(10, 3, GL_FLOAT, false, idDrawVert.BYTES, ac.tangentsOffset_1());
            qglVertexAttribPointerARB(9, 3, GL_FLOAT, false, idDrawVert.BYTES, ac.tangentsOffset_0());
            qglVertexAttribPointerARB(8, 2, GL_FLOAT, false, idDrawVert.BYTES, ac.stOffset());
            qglVertexPointer(3, GL_FLOAT, idDrawVert.BYTES, ac.xyzOffset());

            // this may cause RB_ARB2_DrawInteraction to be exacuted multiple
            // times with different colors and images if the surface or light have multiple layers
            RB_CreateSingleDrawInteractions(surf, RB_ARB2_DrawInteraction.INSTANCE);
        }

        qglDisableVertexAttribArrayARB(8);
        qglDisableVertexAttribArrayARB(9);
        qglDisableVertexAttribArrayARB(10);
        qglDisableVertexAttribArrayARB(11);
        qglDisableClientState(GL_COLOR_ARRAY);

        // disable features
        GL_SelectTextureNoClient(6);
        globalImages.BindNull();

        GL_SelectTextureNoClient(5);
        globalImages.BindNull();

        GL_SelectTextureNoClient(4);
        globalImages.BindNull();

        GL_SelectTextureNoClient(3);
        globalImages.BindNull();

        GL_SelectTextureNoClient(2);
        globalImages.BindNull();

        GL_SelectTextureNoClient(1);
        globalImages.BindNull();

        backEnd.glState.currenttmu = -1;
        GL_SelectTexture(0);

        qglDisable(GL_VERTEX_PROGRAM_ARB);
        qglDisable(GL_FRAGMENT_PROGRAM_ARB);
    }


    /*
     ==================
     RB_ARB2_DrawInteractions
     ==================
     */
    public static void RB_ARB2_DrawInteractions() {
        viewLight_s vLight;
        idMaterial lightShader;

        GL_SelectTexture(0);
        qglDisableClientState(GL_TEXTURE_COORD_ARRAY);

        //
        // for each light, perform adding and shadowing
        //
        for (vLight = backEnd.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            backEnd.vLight = vLight;

            // do fogging later
            if (vLight.lightShader.IsFogLight()) {
                continue;
            }
            if (vLight.lightShader.IsBlendLight()) {
                continue;
            }

            if (NOT(vLight.localInteractions[0]) && NOT(vLight.globalInteractions[0])
                    && NOT(vLight.translucentInteractions[0])) {
                continue;
            }

            lightShader = vLight.lightShader;

            // clear the stencil buffer if needed
            if ((vLight.globalShadows[0] != null) || (vLight.localShadows[0] != null)) {
                backEnd.currentScissor = new idScreenRect(vLight.scissorRect);
                if (r_useScissor.GetBool()) {
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

            if (r_useShadowVertexProgram.GetBool()) {
                qglEnable(GL_VERTEX_PROGRAM_ARB);
                qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_STENCIL_SHADOW);
                RB_StencilShadowPass(vLight.globalShadows[0]);
                RB_ARB2_CreateDrawInteractions(vLight.localInteractions[0]);
                qglEnable(GL_VERTEX_PROGRAM_ARB);
                qglBindProgramARB(GL_VERTEX_PROGRAM_ARB, VPROG_STENCIL_SHADOW);
                RB_StencilShadowPass(vLight.localShadows[0]);
                RB_ARB2_CreateDrawInteractions(vLight.globalInteractions[0]);
                qglDisable(GL_VERTEX_PROGRAM_ARB);	// if there weren't any globalInteractions, it would have stayed on
            } else {
                RB_StencilShadowPass(vLight.globalShadows[0]);
                RB_ARB2_CreateDrawInteractions(vLight.localInteractions[0]);
                RB_StencilShadowPass(vLight.localShadows[0]);
                RB_ARB2_CreateDrawInteractions(vLight.globalInteractions[0]);
            }

            // translucent surfaces never get stencil shadowed
            if (r_skipTranslucent.GetBool()) {
                continue;
            }

            qglStencilFunc(GL_ALWAYS, 128, 255);

            backEnd.depthFunc = GLS_DEPTHFUNC_LESS;
            RB_ARB2_CreateDrawInteractions(vLight.translucentInteractions[0]);

            backEnd.depthFunc = GLS_DEPTHFUNC_EQUAL;
        }

        // disable stencil shadow test
        qglStencilFunc(GL_ALWAYS, 128, 255);

        GL_SelectTexture(0);
        qglEnableClientState(GL_TEXTURE_COORD_ARRAY);
    }

//===================================================================================
    static class progDef_t {

    int    target;
    int    ident;
    String name;// char			name[64];

        progDef_t(int target, program_t ident, String name) {
            this(target, ident.ordinal(), name);
        }

        progDef_t(int target, int ident, String name) {
            this.target = target;
            this.ident = ident;
            this.name = name;
        }
    }
    static final int MAX_GLPROGS = 200;
    // a single file can have both a vertex program and a fragment program
    static progDef_t[] progs = new progDef_t[MAX_GLPROGS];

    static {
        int a = 0;
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_TEST, "test.vfp");
        progs[a++] = new progDef_t(GL_FRAGMENT_PROGRAM_ARB, FPROG_TEST, "test.vfp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_INTERACTION, "interaction.vfp");
        progs[a++] = new progDef_t(GL_FRAGMENT_PROGRAM_ARB, FPROG_INTERACTION, "interaction.vfp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_BUMPY_ENVIRONMENT, "bumpyEnvironment.vfp");
        progs[a++] = new progDef_t(GL_FRAGMENT_PROGRAM_ARB, FPROG_BUMPY_ENVIRONMENT, "bumpyEnvironment.vfp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_AMBIENT, "ambientLight.vfp");
        progs[a++] = new progDef_t(GL_FRAGMENT_PROGRAM_ARB, FPROG_AMBIENT, "ambientLight.vfp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_STENCIL_SHADOW, "shadow.vp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_R200_INTERACTION, "R200_interaction.vp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_NV20_BUMP_AND_LIGHT, "nv20_bumpAndLight.vp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_NV20_DIFFUSE_COLOR, "nv20_diffuseColor.vp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_NV20_SPECULAR_COLOR, "nv20_specularColor.vp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_NV20_DIFFUSE_AND_SPECULAR_COLOR, "nv20_diffuseAndSpecularColor.vp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_ENVIRONMENT, "environment.vfp");
        progs[a++] = new progDef_t(GL_FRAGMENT_PROGRAM_ARB, FPROG_ENVIRONMENT, "environment.vfp");
        progs[a++] = new progDef_t(GL_VERTEX_PROGRAM_ARB, VPROG_GLASSWARP, "arbVP_glasswarp.txt");
        progs[a++] = new progDef_t(GL_FRAGMENT_PROGRAM_ARB, FPROG_GLASSWARP, "arbFP_glasswarp.txt");

        // additional programs can be dynamically specified in materials
    }

    /*
     =================
     R_LoadARBProgram
     =================
     */
    public static void R_LoadARBProgram(int progIndex) {
        final IntBuffer ofs = Nio.newIntBuffer(16);
        int err;
        final idStr fullPath = new idStr("glprogs/" + progs[progIndex].name);
        final ByteBuffer[] fileBuffer = {null};
        String buffer;
        int start = 0, end;

        common.Printf("%s", fullPath);

        // load the program even if we don't support it, so
        // fs_copyfiles can generate cross-platform data dumps
        fileSystem.ReadFile(fullPath.getData(), /*(void **)&*/ fileBuffer, null);
        if (NOT(fileBuffer[0])) {
            common.Printf(": File not found\n");
            return;
        }

        // copy to stack memory and free
//        buffer = /*(char *)*/ _alloca(strlen(fileBuffer) + 1);
        buffer = new String(fileBuffer[0].array());
//        fileSystem.FreeFile(fileBuffer);

        if (!glConfig.isInitialized) {
            return;
        }

        //
        // submit the program string at start to GL
        //
        if (progs[progIndex].ident == PROG_INVALID.ordinal()) {
            // allocate a new identifier for this program
            progs[progIndex].ident = PROG_USER.ordinal() + progIndex;
        }

        // vertex and fragment programs can both be present in a single file, so
        // scan for the proper header to be the start point, and stamp a 0 in after the end
        if (progs[progIndex].target == GL_VERTEX_PROGRAM_ARB) {
            if (!glConfig.ARBVertexProgramAvailable) {
                common.Printf(": GL_VERTEX_PROGRAM_ARB not available\n");
                return;
            }
            start = buffer.indexOf("!!ARBvp");
        }
        if (progs[progIndex].target == GL_FRAGMENT_PROGRAM_ARB) {
            if (!glConfig.ARBFragmentProgramAvailable) {
                common.Printf(": GL_FRAGMENT_PROGRAM_ARB not available\n");
                return;
            }
            start = buffer.indexOf("!!ARBfp");
        }
        if (-1 == start) {
            common.Printf(": !!ARB not found\n");
            return;
        }
        end = start + buffer.substring(start).indexOf("END");

        if (-1 == end) {
            common.Printf(": END not found\n");
            return;
        }
        buffer = buffer.substring(start, end + 3);//end[3] = 0;
        final ByteBuffer substring = Nio.newByteBuffer(buffer.length());
        substring.put(buffer.getBytes()).flip();

        qglBindProgramARB(progs[progIndex].target, progs[progIndex].ident);
        qglGetError();

        qglProgramStringARB(progs[progIndex].target, GL_PROGRAM_FORMAT_ASCII_ARB, 0, /*(unsigned char *)*/ substring);

        err = qglGetError();
        qglGetIntegerv(GL_PROGRAM_ERROR_POSITION_ARB, ofs);
        if (err == GL_INVALID_OPERATION) {
            final String/*GLubyte*/ str = qglGetString(GL_PROGRAM_ERROR_STRING_ARB);
            common.Printf("\nGL_PROGRAM_ERROR_STRING_ARB: %s\n", str);
            if (ofs.get(0) < 0) {
                common.Printf("GL_PROGRAM_ERROR_POSITION_ARB < 0 with error\n");
            } else if (ofs.get(0) >= (buffer.length() - start)) {
                common.Printf("error at end of program\n");
            } else {
                common.Printf("error at %d:\n%s", ofs.get(0), start + ofs.get(0));
            }
            return;
        }
        if (ofs.get(0) != -1) {
            common.Printf("\nGL_PROGRAM_ERROR_POSITION_ARB != -1 without error\n");
            return;
        }

        common.Printf("\n");
    }

    /*
     ==================
     R_FindARBProgram

     Returns a GL identifier that can be bound to the given target, parsing
     a text file if it hasn't already been loaded.
     ==================
     */
    public static int R_FindARBProgram( /*GLenum */int target, final String program) {
        int i;
        final idStr stripped = new idStr(program);

        stripped.StripFileExtension();

        // see if it is already loaded
        for (i = 0; (progs[i] != null) && isNotNullOrEmpty(progs[i].name); i++) {
            if (progs[i].target != target) {
                continue;
            }

            final idStr compare = new idStr(progs[i].name);
            compare.StripFileExtension();

            if (NOT(idStr.Icmp(stripped, compare))) {
                return progs[i].ident;
            }
        }

        if (i == MAX_GLPROGS) {
            common.Error("R_FindARBProgram: MAX_GLPROGS");
        }

        // add it to the list and load it
        progs[i] = new progDef_t(target, PROG_INVALID, program);// will be gen'd by R_LoadARBProgram

        R_LoadARBProgram(i);

        return progs[i].ident;
    }

    /*
     ==================
     R_ReloadARBPrograms_f
     ==================
     */
    public static class R_ReloadARBPrograms_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ReloadARBPrograms_f();

        private R_ReloadARBPrograms_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;

            common.Printf("----- R_ReloadARBPrograms -----\n");
            for (i = 0; (progs[i] != null) && isNotNullOrEmpty(progs[i].name); i++) {
                R_LoadARBProgram(i);
            }
            common.Printf("-------------------------------\n");
        }
    }

    /*
     ==================
     R_ARB2_Init

     ==================
     */
    public static void R_ARB2_Init() {
        glConfig.allowARB2Path = false;

        common.Printf("---------- R_ARB2_Init ----------\n");

        if (!glConfig.ARBVertexProgramAvailable || !glConfig.ARBFragmentProgramAvailable) {
            common.Printf("Not available.\n");
            return;
        }

        common.Printf("Available.\n");

        common.Printf("---------------------------------\n");

        glConfig.allowARB2Path = true;
    }
}
