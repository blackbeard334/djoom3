package neo.Renderer;

import neo.Renderer.Model.dynamicModel_t;
import static neo.Renderer.Model.dynamicModel_t.DM_CONTINUOUS;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.Model_local.idRenderModelStatic;
import static neo.Renderer.RenderWorld.SHADERPARM_ALPHA;
import static neo.Renderer.RenderWorld.SHADERPARM_BLUE;
import static neo.Renderer.RenderWorld.SHADERPARM_GREEN;
import static neo.Renderer.RenderWorld.SHADERPARM_RED;
import static neo.Renderer.RenderWorld.SHADERPARM_SPRITE_HEIGHT;
import static neo.Renderer.RenderWorld.SHADERPARM_SPRITE_WIDTH;
import neo.Renderer.RenderWorld.renderEntity_s;
import static neo.Renderer.tr_local.tr;
import neo.Renderer.tr_local.viewDef_s;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import neo.idlib.BV.Bounds.idBounds;
import static neo.idlib.Lib.Max;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Model_sprite {

    /*

     A simple sprite model that always faces the view axis.

     */
    static final String sprite_SnapshotName = "_sprite_Snapshot_";

    /*
     ================================================================================

     idRenderModelSprite 

     ================================================================================
     */
    public static class idRenderModelSprite extends idRenderModelStatic {

        @Override
        public dynamicModel_t IsDynamicModel() {
            return DM_CONTINUOUS;
        }

        @Override
        public boolean IsLoaded() {
            return true;
        }

        @Override
        public idRenderModel InstantiateDynamicModel(renderEntity_s renderEntity, viewDef_s viewDef, idRenderModel cachedModel) {
            idRenderModelStatic staticModel;
            srfTriangles_s tri;
            modelSurface_s surf = new modelSurface_s();

            if (cachedModel != null && !RenderSystem_init.r_useCachedDynamicModels.GetBool()) {
//		delete cachedModel;
                cachedModel = null;
            }

            if (renderEntity == null || viewDef == null) {
//		delete cachedModel;
                return null;
            }

            if (cachedModel != null) {

//		assert( dynamic_cast<idRenderModelStatic *>( cachedModel ) != null );
//		assert( idStr.Icmp( cachedModel.Name(), sprite_SnapshotName ) == 0 );
                staticModel = (idRenderModelStatic) cachedModel;
                surf = staticModel.Surface(0);
                tri = surf.geometry;

            } else {

                staticModel = new idRenderModelStatic();
                staticModel.InitEmpty(sprite_SnapshotName);

                tri = R_AllocStaticTriSurf();
                R_AllocStaticTriSurfVerts(tri, 4);
                R_AllocStaticTriSurfIndexes(tri, 6);

                tri.verts[ 0].Clear();
                tri.verts[ 0].normal.Set(1.0f, 0.0f, 0.0f);
                tri.verts[ 0].tangents[0].Set(0.0f, 1.0f, 0.0f);
                tri.verts[ 0].tangents[1].Set(0.0f, 0.0f, 1.0f);
                tri.verts[ 0].st.oSet(0, 0.0f);
                tri.verts[ 0].st.oSet(1, 0.0f);

                tri.verts[ 1].Clear();
                tri.verts[ 1].normal.Set(1.0f, 0.0f, 0.0f);
                tri.verts[ 1].tangents[0].Set(0.0f, 1.0f, 0.0f);
                tri.verts[ 1].tangents[1].Set(0.0f, 0.0f, 1.0f);
                tri.verts[ 1].st.oSet(0, 1.0f);
                tri.verts[ 1].st.oSet(1, 0.0f);

                tri.verts[ 2].Clear();
                tri.verts[ 2].normal.Set(1.0f, 0.0f, 0.0f);
                tri.verts[ 2].tangents[0].Set(0.0f, 1.0f, 0.0f);
                tri.verts[ 2].tangents[1].Set(0.0f, 0.0f, 1.0f);
                tri.verts[ 2].st.oSet(0, 1.0f);
                tri.verts[ 2].st.oSet(1, 1.0f);

                tri.verts[ 3].Clear();
                tri.verts[ 3].normal.Set(1.0f, 0.0f, 0.0f);
                tri.verts[ 3].tangents[0].Set(0.0f, 1.0f, 0.0f);
                tri.verts[ 3].tangents[1].Set(0.0f, 0.0f, 1.0f);
                tri.verts[ 3].st.oSet(0, 0.0f);
                tri.verts[ 3].st.oSet(1, 1.0f);

                tri.indexes[ 0] = 0;
                tri.indexes[ 1] = 1;
                tri.indexes[ 2] = 3;
                tri.indexes[ 3] = 1;
                tri.indexes[ 4] = 2;
                tri.indexes[ 5] = 3;

                tri.numVerts = 4;
                tri.numIndexes = 6;

                surf.geometry = tri;
                surf.id = 0;
                surf.shader = tr.defaultMaterial;
                staticModel.AddSurface(surf);
            }

            final byte red = (byte) idMath.FtoiFast(renderEntity.shaderParms[SHADERPARM_RED] * 255.0f);
            final byte green = (byte) idMath.FtoiFast(renderEntity.shaderParms[SHADERPARM_GREEN] * 255.0f);
            final byte blue = (byte) idMath.FtoiFast(renderEntity.shaderParms[SHADERPARM_BLUE] * 255.0f);
            final byte alpha = (byte) idMath.FtoiFast(renderEntity.shaderParms[SHADERPARM_ALPHA] * 255.0f);

            idVec3 right = new idVec3(0.0f, renderEntity.shaderParms[SHADERPARM_SPRITE_WIDTH] * 0.5f, 0.0f);
            idVec3 up = new idVec3(0.0f, 0.0f, renderEntity.shaderParms[SHADERPARM_SPRITE_HEIGHT] * 0.5f);

            tri.verts[ 0].xyz = up.oPlus(right);
            tri.verts[ 0].color[ 0] = red;
            tri.verts[ 0].color[ 1] = green;
            tri.verts[ 0].color[ 2] = blue;
            tri.verts[ 0].color[ 3] = alpha;

            tri.verts[ 1].xyz = up.oMinus(right);
            tri.verts[ 1].color[ 0] = red;
            tri.verts[ 1].color[ 1] = green;
            tri.verts[ 1].color[ 2] = blue;
            tri.verts[ 1].color[ 3] = alpha;

            tri.verts[ 2].xyz = right.oMinus(up).oNegative();
            tri.verts[ 2].color[ 0] = red;
            tri.verts[ 2].color[ 1] = green;
            tri.verts[ 2].color[ 2] = blue;
            tri.verts[ 2].color[ 3] = alpha;

            tri.verts[ 3].xyz = right.oMinus(up);
            tri.verts[ 3].color[ 0] = red;
            tri.verts[ 3].color[ 1] = green;
            tri.verts[ 3].color[ 2] = blue;
            tri.verts[ 3].color[ 3] = alpha;

            R_BoundTriSurf(tri);

            staticModel.bounds = new idBounds(tri.bounds);

            return staticModel;
        }

        @Override
        public idBounds Bounds(renderEntity_s renderEntity) {
            idBounds b = new idBounds();

            b.Zero();
            if (renderEntity == null) {
                b.ExpandSelf(8.0f);
            } else {
                b.ExpandSelf((float) (Max(renderEntity.shaderParms[SHADERPARM_SPRITE_WIDTH], renderEntity.shaderParms[SHADERPARM_SPRITE_HEIGHT]) * 0.5f));
            }
            return b;
        }
    };
}
