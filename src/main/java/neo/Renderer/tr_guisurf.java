package neo.Renderer;

import static neo.Renderer.RenderSystem_init.r_skipGuiShaders;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_main.myGlMultMatrix;
import static neo.framework.Common.common;
import static neo.idlib.math.Vector.VectorMA;
import static neo.idlib.math.Vector.VectorSubtract;
import static neo.ui.UserInterface.uiManager;

import java.nio.FloatBuffer;

import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.tr_local.drawSurf_s;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.open.Nio;
import neo.ui.UserInterface.idUserInterface;

/**
 *
 */
public class tr_guisurf {

    /*
     ==========================================================================================

     GUI SHADERS

     ==========================================================================================
     */

    /*
     ================
     R_SurfaceToTextureAxis

     Calculates two axis for the surface sutch that a point dotted against
     the axis will give a 0.0 to 1.0 range in S and T when inside the gui surface
     ================
     */
    public static void R_SurfaceToTextureAxis(final srfTriangles_s tri, idVec3 origin, idVec3[] axis/*[3]*/) {
        float area, inva;
        final float[] d0 = new float[5], d1 = new float[5];
        idDrawVert a, b, c;
        final float[][] bounds = new float[2][2];
        final float[] boundsOrg = new float[2];
        int i, j;
        float v;

        // find the bounds of the texture
        bounds[0][0] = bounds[0][1] = 999999;
        bounds[1][0] = bounds[1][1] = -999999;
        for (i = 0; i < tri.numVerts; i++) {
            for (j = 0; j < 2; j++) {
                v = tri.verts[i].st.oGet(j);
                if (v < bounds[0][j]) {
                    bounds[0][j] = v;
                }
                if (v > bounds[1][j]) {
                    bounds[1][j] = v;
                }
            }
        }

        // use the floor of the midpoint as the origin of the
        // surface, which will prevent a slight misalignment
        // from throwing it an entire cycle off
        boundsOrg[0] = (float) Math.floor((bounds[0][0] + bounds[1][0]) * 0.5);
        boundsOrg[1] = (float) Math.floor((bounds[0][1] + bounds[1][1]) * 0.5);

        // determine the world S and T vectors from the first drawSurf triangle
        a = tri.verts[tri.getIndexes().getValues().get(0)];
        b = tri.verts[tri.getIndexes().getValues().get(1)];
        c = tri.verts[tri.getIndexes().getValues().get(2)];

        VectorSubtract(b.xyz, a.xyz, d0);
        d0[3] = b.st.oGet(0) - a.st.oGet(0);
        d0[4] = b.st.oGet(1) - a.st.oGet(1);
        VectorSubtract(c.xyz, a.xyz, d1);
        d1[3] = c.st.oGet(0) - a.st.oGet(0);
        d1[4] = c.st.oGet(1) - a.st.oGet(1);

        area = d0[3] * d1[4] - d0[4] * d1[3];
        if (area == 0.0) {
            axis[0].Zero();
            axis[1].Zero();
            axis[2].Zero();
            return;	// degenerate
        }
        inva = 1.0f / area;

        axis[0].oSet(0, (d0[0] * d1[4] - d0[4] * d1[0]) * inva);
        axis[0].oSet(1, (d0[1] * d1[4] - d0[4] * d1[1]) * inva);
        axis[0].oSet(2, (d0[2] * d1[4] - d0[4] * d1[2]) * inva);

        axis[1].oSet(0, (d0[3] * d1[0] - d0[0] * d1[3]) * inva);
        axis[1].oSet(1, (d0[3] * d1[1] - d0[1] * d1[3]) * inva);
        axis[1].oSet(2, (d0[3] * d1[2] - d0[2] * d1[3]) * inva);

        final idPlane plane = new idPlane();
        plane.FromPoints(a.xyz, b.xyz, c.xyz);
        axis[2].oSet(0, plane.oGet(0));
        axis[2].oSet(1, plane.oGet(1));
        axis[2].oSet(2, plane.oGet(2));

        // take point 0 and project the vectors to the texture origin
        VectorMA(a.xyz, boundsOrg[0] - a.st.oGet(0), axis[0], origin);
        VectorMA(origin, boundsOrg[1] - a.st.oGet(1), axis[1], origin);
    }

    /*
     =================
     R_RenderGuiSurf

     Create a texture space on the given surface and
     call the GUI generator to create quads for it.
     =================
     */
    public static void R_RenderGuiSurf(idUserInterface gui, drawSurf_s drawSurf) {
        final idVec3 origin = new idVec3();
        final idVec3[] axis = {new idVec3(), new idVec3(), new idVec3()};

        // for testing the performance hit
        if (r_skipGuiShaders.GetInteger() == 1) {
            return;
        }

        // don't allow an infinite recursion loop
        if (tr.guiRecursionLevel == 4) {
            return;
        }

        tr.pc.c_guiSurfs++;

        // create the new matrix to draw on this surface
        R_SurfaceToTextureAxis(drawSurf.geo, origin, axis);

        final FloatBuffer guiModelMatrix = Nio.newFloatBuffer(16);
        final FloatBuffer modelMatrix = Nio.newFloatBuffer(16);

        guiModelMatrix.put(0 * 4 + 0, axis[0].oGet(0) / 640.0f);
        guiModelMatrix.put(1 * 4 + 0, axis[1].oGet(0) / 480.0f);
        guiModelMatrix.put(2 * 4 + 0, axis[2].oGet(0));
        guiModelMatrix.put(3 * 4 + 0, origin.oGet(0));

        guiModelMatrix.put(0 * 4 + 1, axis[0].oGet(1) / 640.0f);
        guiModelMatrix.put(1 * 4 + 1, axis[1].oGet(1) / 480.0f);
        guiModelMatrix.put(2 * 4 + 1, axis[2].oGet(1));
        guiModelMatrix.put(3 * 4 + 1, origin.oGet(1));

        guiModelMatrix.put(0 * 4 + 2, axis[0].oGet(2) / 640.0f);
        guiModelMatrix.put(1 * 4 + 2, axis[1].oGet(2) / 480.0f);
        guiModelMatrix.put(2 * 4 + 2, axis[2].oGet(2));
        guiModelMatrix.put(3 * 4 + 2, origin.oGet(2));

        guiModelMatrix.put(0 * 4 + 3, 0);
        guiModelMatrix.put(1 * 4 + 3, 0);
        guiModelMatrix.put(2 * 4 + 3, 0);
        guiModelMatrix.put(3 * 4 + 3, 1);

        myGlMultMatrix(guiModelMatrix, drawSurf.space.modelMatrix,
                modelMatrix);

        tr.guiRecursionLevel++;

        // call the gui, which will call the 2D drawing functions
        tr.guiModel.Clear();
        gui.Redraw(tr.viewDef.renderView.time);
        tr.guiModel.EmitToCurrentView(modelMatrix, drawSurf.space.weaponDepthHack);
        tr.guiModel.Clear();

        tr.guiRecursionLevel--;
    }

    /*
     ================,
     R_ReloadGuis_f

     Reloads any guis that have had their file timestamps changed.
     An optional "all" parameter will cause all models to reload, even
     if they are not out of date.

     Should we also reload the map models?
     ================
     */
    public static class R_ReloadGuis_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ReloadGuis_f();

        private R_ReloadGuis_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            boolean all;

            if (0 == idStr.Icmp(args.Argv(1), "all")) {
                all = true;
                common.Printf("Reloading all gui files...\n");
            } else {
                all = false;
                common.Printf("Checking for changed gui files...\n");
            }

            uiManager.Reload(all);
        }
    }

    /*
     ================,
     R_ListGuis_f

     ================
     */
    public static class R_ListGuis_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ListGuis_f();

        private R_ListGuis_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            uiManager.ListGuis();
        }
    }
}
