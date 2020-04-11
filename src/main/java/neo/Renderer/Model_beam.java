package neo.Renderer;

import static neo.Renderer.Model.dynamicModel_t.DM_CONTINUOUS;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BEAM_END_X;
import static neo.Renderer.RenderWorld.SHADERPARM_BEAM_WIDTH;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_main.R_AxisToModelMatrix;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;

import neo.Renderer.Model.dynamicModel_t;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.Model_local.idRenderModelStatic;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.tr_local.viewDef_s;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Model_beam {

    /*

     This is a simple dynamic model that just creates a stretched quad between
     two points that faces the view, like a dynamic deform tube.

     */
    static final String beam_SnapshotName = "_beam_Snapshot_";


    /*
     ===============================================================================

     Beam model

     ===============================================================================
     */
    public static class idRenderModelBeam extends idRenderModelStatic {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
        public dynamicModel_t IsDynamicModel() {
            return DM_CONTINUOUS;	// regenerate for every view
        }

        @Override
        public boolean IsLoaded() {
            return true;	// don't ever need to load
        }

        @Override
        public idRenderModel InstantiateDynamicModel(renderEntity_s renderEntity, viewDef_s viewDef, idRenderModel cachedModel) {
            idRenderModelStatic staticModel;
            srfTriangles_s tri;
            modelSurface_s surf = new modelSurface_s();

            if (cachedModel != null) {
//		delete cachedModel;
                cachedModel = null;
            }

            if ((renderEntity == null) || (viewDef == null)) {
//		delete cachedModel;
                return null;
            }

            if (cachedModel != null) {

//		assert( dynamic_cast<idRenderModelStatic *>( cachedModel ) != null );
//		assert( idStr.Icmp( cachedModel.Name(), beam_SnapshotName ) == 0 );
                staticModel = (idRenderModelStatic) cachedModel;
                surf = staticModel.Surface(0);
                tri = surf.geometry;

            } else {

                staticModel = new idRenderModelStatic();
                staticModel.InitEmpty(beam_SnapshotName);

                tri = R_AllocStaticTriSurf();
                R_AllocStaticTriSurfVerts(tri, 4);
                R_AllocStaticTriSurfIndexes(tri, 6);

                tri.verts[0].Clear();
                tri.verts[0].st.oSet(0, 0);
                tri.verts[0].st.oSet(1, 0);

                tri.verts[1].Clear();
                tri.verts[1].st.oSet(0, 0);
                tri.verts[1].st.oSet(1, 1);

                tri.verts[2].Clear();
                tri.verts[2].st.oSet(0, 1);
                tri.verts[2].st.oSet(1, 0);

                tri.verts[3].Clear();
                tri.verts[3].st.oSet(0, 1);
                tri.verts[3].st.oSet(1, 1);

                tri.indexes[0] = 0;
                tri.indexes[1] = 2;
                tri.indexes[2] = 1;
                tri.indexes[3] = 2;
                tri.indexes[4] = 3;
                tri.indexes[5] = 1;

                tri.numVerts = 4;
                tri.numIndexes = 6;

                surf.geometry = tri;
                surf.id = 0;
                surf.shader = tr.defaultMaterial;
                staticModel.AddSurface(surf);
            }

            final idVec3 target = /*reinterpret_cast<const idVec3 *>*/ new idVec3(renderEntity.shaderParms, SHADERPARM_BEAM_END_X);

            // we need the view direction to project the minor axis of the tube
            // as the view changes
            final idVec3 localView = new idVec3(), localTarget = new idVec3();
            final float[] modelMatrix = new float[16];
            R_AxisToModelMatrix(renderEntity.axis, renderEntity.origin, modelMatrix);
            R_GlobalPointToLocal(modelMatrix, viewDef.renderView.vieworg, localView);
            R_GlobalPointToLocal(modelMatrix, target, localTarget);

            final idVec3 major = localTarget;
            final idVec3 minor = new idVec3();

            final idVec3 mid = localTarget.oMultiply(0.5f);
            final idVec3 dir = mid.oMinus(localView);
            minor.Cross(major, dir);
            minor.Normalize();
            if (renderEntity.shaderParms[SHADERPARM_BEAM_WIDTH] != 0.0f) {
                minor.oMulSet(renderEntity.shaderParms[SHADERPARM_BEAM_WIDTH] * 0.5f);
            }

            final byte red = (byte) idMath.FtoiFast(renderEntity.shaderParms[SHADERPARM_RED] * 255.0f);
            final byte green = (byte) idMath.FtoiFast(renderEntity.shaderParms[SHADERPARM_GREEN] * 255.0f);
            final byte blue = (byte) idMath.FtoiFast(renderEntity.shaderParms[SHADERPARM_BLUE] * 255.0f);
            final byte alpha = (byte) idMath.FtoiFast(renderEntity.shaderParms[SHADERPARM_ALPHA] * 255.0f);

            tri.verts[0].xyz = minor;
            tri.verts[0].color[0] = red;
            tri.verts[0].color[1] = green;
            tri.verts[0].color[2] = blue;
            tri.verts[0].color[3] = alpha;

            tri.verts[1].xyz = minor.oNegative();
            tri.verts[1].color[0] = red;
            tri.verts[1].color[1] = green;
            tri.verts[1].color[2] = blue;
            tri.verts[1].color[3] = alpha;

            tri.verts[2].xyz = localTarget.oPlus(minor);
            tri.verts[2].color[0] = red;
            tri.verts[2].color[1] = green;
            tri.verts[2].color[2] = blue;
            tri.verts[2].color[3] = alpha;

            tri.verts[3].xyz = localTarget.oMinus(minor);
            tri.verts[3].color[0] = red;
            tri.verts[3].color[1] = green;
            tri.verts[3].color[2] = blue;
            tri.verts[3].color[3] = alpha;

            R_BoundTriSurf(tri);

            staticModel.bounds = new idBounds(tri.bounds);

            return staticModel;
        }

        @Override
        public idBounds Bounds(renderEntity_s renderEntity) {
            final idBounds b = new idBounds();

            b.Zero();
            if (null == renderEntity) {
                b.ExpandSelf(8.0f);
            } else {
                final idVec3 target = /* * reinterpret_cast<const idVec3 *>*/ new idVec3(renderEntity.shaderParms, SHADERPARM_BEAM_END_X);
                final idVec3 localTarget = new idVec3();
                final float[] modelMatrix = new float[16];
                R_AxisToModelMatrix(renderEntity.axis, renderEntity.origin, modelMatrix);
                R_GlobalPointToLocal(modelMatrix, target, localTarget);

                b.AddPoint(localTarget);
                if (renderEntity.shaderParms[SHADERPARM_BEAM_WIDTH] != 0.0f) {
                    b.ExpandSelf(renderEntity.shaderParms[SHADERPARM_BEAM_WIDTH] * 0.5f);
                }
            }
            return b;
        }
    }
}
