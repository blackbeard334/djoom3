package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Material.SS_SUBVIEW;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.shaderStage_t;
import neo.Renderer.Material.textureStage_t;
import neo.Renderer.Model.srfTriangles_s;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.RenderSystem_init.r_skipSubviews;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.idScreenRect;
import static neo.Renderer.tr_local.tr;
import neo.Renderer.tr_local.viewDef_s;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_main.R_GlobalToNormalizedDeviceCoordinates;
import static neo.Renderer.tr_main.R_LocalPlaneToGlobal;
import static neo.Renderer.tr_main.R_LocalPointToGlobal;
import static neo.Renderer.tr_main.R_RenderView;
import static neo.Renderer.tr_main.R_TransformModelToClip;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class tr_subview {

    static class orientation_t {

        idVec3 origin;
        idMat3 axis;
    };


    /*
     =================
     R_MirrorPoint
     =================
     */
    public static void R_MirrorPoint(final idVec3 in, orientation_t surface, orientation_t camera, idVec3 out) {
        int i;
        idVec3 local;
        idVec3 transformed;
        float d;

        local = in.oMinus(surface.origin);

        transformed = getVec3_origin();
        for (i = 0; i < 3; i++) {
            d = local.oMultiply(surface.axis.oGet(i));
            transformed.oPluSet(camera.axis.oGet(i).oMultiply(d));
        }

        out.oSet(transformed.oPlus(camera.origin));
    }

    /*
     =================
     R_MirrorVector
     =================
     */
    public static void R_MirrorVector(final idVec3 in, orientation_t surface, orientation_t camera, idVec3 out) {
        int i;
        float d;

        out.oSet(getVec3_origin());
        for (i = 0; i < 3; i++) {
            d = in.oMultiply(surface.axis.oGet(i));
            out.oPluSet(camera.axis.oGet(i).oMultiply(d));
        }
    }

    /*
     =============
     R_PlaneForSurface

     Returns the plane for the first triangle in the surface
     FIXME: check for degenerate triangle?
     =============
     */
    public static void R_PlaneForSurface(final srfTriangles_s tri, idPlane plane) {
        idDrawVert v1, v2, v3;

        v1 = tri.verts[tri.indexes[0]];
        v2 = tri.verts[tri.indexes[1]];
        v3 = tri.verts[tri.indexes[2]];
        plane.FromPoints(v1.xyz, v2.xyz, v3.xyz);
    }

    /*
     =========================
     R_PreciseCullSurface

     Check the surface for visibility on a per-triangle basis
     for cases when it is going to be VERY expensive to draw (subviews)

     If not culled, also returns the bounding box of the surface in 
     Normalized Device Coordinates, so it can be used to crop the scissor rect.

     OPTIMIZE: we could also take exact portal passing into consideration
     =========================
     */
    public static boolean R_PreciseCullSurface(final drawSurf_s drawSurf, idBounds ndcBounds) {
        final srfTriangles_s tri;
        int numTriangles;
        idPlane clip = new idPlane(), eye = new idPlane();
        int i, j;
        int pointOr;
        int pointAnd;
        idVec3 localView = new idVec3();
        idFixedWinding w = new idFixedWinding();

        tri = drawSurf.geo;

        pointOr = 0;
        pointAnd = (int) ~0;

        // get an exact bounds of the triangles for scissor cropping
        ndcBounds.Clear();

        for (i = 0; i < tri.numVerts; i++) {
//		int j;
            int pointFlags;

            R_TransformModelToClip(tri.verts[i].xyz, drawSurf.space.modelViewMatrix,
                    tr.viewDef.projectionMatrix, eye, clip);

            pointFlags = 0;
            for (j = 0; j < 3; j++) {
                if (clip.oGet(j) >= clip.oGet(3)) {
                    pointFlags |= (1 << (j * 2));
                } else if (clip.oGet(j) <= -clip.oGet(3)) {
                    pointFlags |= (1 << (j * 2 + 1));
                }
            }

            pointAnd &= pointFlags;
            pointOr |= pointFlags;
        }

        // trivially reject
        if (pointAnd != 0) {
            return true;
        }

        // backface and frustum cull
        numTriangles = tri.numIndexes / 3;

        R_GlobalPointToLocal(drawSurf.space.modelMatrix, tr.viewDef.renderView.vieworg, localView);

        for (i = 0; i < tri.numIndexes; i += 3) {
            idVec3 dir, normal;
            float dot;
            idVec3 d1, d2;

            final idVec3 v1 = tri.verts[tri.indexes[i]].xyz;
            final idVec3 v2 = tri.verts[tri.indexes[i + 1]].xyz;
            final idVec3 v3 = tri.verts[tri.indexes[i + 2]].xyz;

            // this is a hack, because R_GlobalPointToLocal doesn't work with the non-normalized
            // axis that we get from the gui view transform.  It doesn't hurt anything, because
            // we know that all gui generated surfaces are front facing
            if (tr.guiRecursionLevel == 0) {
                // we don't care that it isn't normalized,
                // all we want is the sign
                d1 = v2.oMinus(v1);
                d2 = v3.oMinus(v1);
                normal = d2.Cross(d1);

                dir = v1.oMinus(localView);

                dot = normal.oMultiply(dir);
                if (dot >= 0.0f) {
                    return true;
                }
            }

            // now find the exact screen bounds of the clipped triangle
            w.SetNumPoints(3);
            w.oSet(0, R_LocalPointToGlobal(drawSurf.space.modelMatrix, v1));
            w.oSet(1, R_LocalPointToGlobal(drawSurf.space.modelMatrix, v2));
            w.oSet(2, R_LocalPointToGlobal(drawSurf.space.modelMatrix, v3));
            w.oGet(0).s = w.oGet(0).t = w.oGet(1).s = w.oGet(1).t = w.oGet(2).s = w.oGet(2).t = 0.0f;

            for (j = 0; j < 4; j++) {
                if (!w.ClipInPlace(tr.viewDef.frustum[j].oNegative(), 0.1f)) {
                    break;
                }
            }
            for (j = 0; j < w.GetNumPoints(); j++) {
                idVec3 screen = new idVec3();

                R_GlobalToNormalizedDeviceCoordinates(w.oGet(j).ToVec3(), screen);
                ndcBounds.AddPoint(screen);
            }
        }

        // if we don't enclose any area, return
        if (ndcBounds.IsCleared()) {
            return true;
        }

        return false;
    }

    /*
     ========================
     R_MirrorViewBySurface
     ========================
     */
    public static viewDef_s R_MirrorViewBySurface(drawSurf_s drawSurf) {
        viewDef_s parms;
        orientation_t surface = new orientation_t(), camera = new orientation_t();
        idPlane originalPlane = new idPlane(), plane = new idPlane();

        // copy the viewport size from the original
//        parms = (viewDef_s) R_FrameAlloc(sizeof(parms));
        parms = tr.viewDef;
        parms.renderView.viewID = 0;	// clear to allow player bodies to show up, and suppress view weapons

        parms.isSubview = true;
        parms.isMirror = true;

        // create plane axis for the portal we are seeing
        R_PlaneForSurface(drawSurf.geo, originalPlane);
        R_LocalPlaneToGlobal(drawSurf.space.modelMatrix, originalPlane, plane);

        surface.origin = plane.Normal().oMultiply(-plane.oGet(3));
        surface.axis.oSet(0, plane.Normal());
        surface.axis.oGet(0).NormalVectors(surface.axis.oGet(1), surface.axis.oGet(2));
        surface.axis.oSet(2, surface.axis.oGet(2).oNegative());

        camera.origin = surface.origin;
        camera.axis.oSet(0, surface.axis.oGet(0).oNegative());
        camera.axis.oSet(1, surface.axis.oGet(1));
        camera.axis.oSet(2, surface.axis.oGet(2));

        // set the mirrored origin and axis
        R_MirrorPoint(tr.viewDef.renderView.vieworg, surface, camera, parms.renderView.vieworg);

        R_MirrorVector(tr.viewDef.renderView.viewaxis.oGet(0), surface, camera, parms.renderView.viewaxis.oGet(0));
        R_MirrorVector(tr.viewDef.renderView.viewaxis.oGet(1), surface, camera, parms.renderView.viewaxis.oGet(1));
        R_MirrorVector(tr.viewDef.renderView.viewaxis.oGet(2), surface, camera, parms.renderView.viewaxis.oGet(2));

        // make the view origin 16 units away from the center of the surface
        idVec3 viewOrigin = (drawSurf.geo.bounds.oGet(0).oPlus(drawSurf.geo.bounds.oGet(1))).oMultiply(0.5f);
        viewOrigin.oPluSet(originalPlane.Normal().oMultiply(16));

        parms.initialViewAreaOrigin = R_LocalPointToGlobal(drawSurf.space.modelMatrix, viewOrigin);

        // set the mirror clip plane
        parms.numClipPlanes = 1;
        parms.clipPlanes[0].oSet(camera.axis.oGet(0).oNegative());

        parms.clipPlanes[0].oSet(3, -(camera.origin.oMultiply(parms.clipPlanes[0].Normal())));

        return parms;
    }

    /*
     ========================
     R_XrayViewBySurface
     ========================
     */
    public static viewDef_s R_XrayViewBySurface(drawSurf_s drawSurf) {
        viewDef_s parms;
//	orientation_t	surface, camera;
//	idPlane			originalPlane, plane;

        // copy the viewport size from the original
//	parms = (viewDef_s )R_FrameAlloc( sizeof( parms ) );
        parms = tr.viewDef;
        parms.renderView.viewID = 0;	// clear to allow player bodies to show up, and suppress view weapons

        parms.isSubview = true;
        parms.isXraySubview = true;

        return parms;
    }

    /*
     ===============
     R_RemoteRender
     ===============
     */
    public static void R_RemoteRender(drawSurf_s surf, textureStage_t stage) {
        viewDef_s parms;

        // remote views can be reused in a single frame
        if (stage.dynamicFrameCount == tr.frameCount) {
            return;
        }

        // if the entity doesn't have a remoteRenderView, do nothing
        if (null == surf.space.entityDef.parms.remoteRenderView) {
            return;
        }

        // copy the viewport size from the original
//	parms = (viewDef_t *)R_FrameAlloc( sizeof( *parms ) );
        parms = tr.viewDef;

        parms.isSubview = true;
        parms.isMirror = false;

        parms.renderView = (renderView_s) surf.space.entityDef.parms.remoteRenderView;
        parms.renderView.viewID = 0;	// clear to allow player bodies to show up, and suppress view weapons
        parms.initialViewAreaOrigin = parms.renderView.vieworg;

        tr.CropRenderSize(stage.width, stage.height, true);

        parms.renderView.x = 0;
        parms.renderView.y = 0;
        parms.renderView.width = SCREEN_WIDTH;
        parms.renderView.height = SCREEN_HEIGHT;

        tr.RenderViewToViewport(parms.renderView, parms.viewport);

        parms.scissor.x1 = 0;
        parms.scissor.y1 = 0;
        parms.scissor.x2 = parms.viewport.x2 - parms.viewport.x1;
        parms.scissor.y2 = parms.viewport.y2 - parms.viewport.y1;

        parms.superView = tr.viewDef;
        parms.subviewSurface = surf;

        // generate render commands for it
        R_RenderView(parms);

        // copy this rendering to the image
        stage.dynamicFrameCount = tr.frameCount;
        if (null == stage.image[0]) {
            stage.image[0] = globalImages.scratchImage;
        }

        tr.CaptureRenderToImage(stage.image[0].imgName.toString());
        tr.UnCrop();
    }

    /*
     =================
     R_MirrorRender
     =================
     */
    public static void R_MirrorRender(drawSurf_s surf, textureStage_t stage, idScreenRect scissor) {
        viewDef_s parms;

        // remote views can be reused in a single frame
        if (stage.dynamicFrameCount == tr.frameCount) {
            return;
        }

        // issue a new view command
        parms = R_MirrorViewBySurface(surf);
        if (null == parms) {
            return;
        }

        tr.CropRenderSize(stage.width, stage.height, true);

        parms.renderView.x = 0;
        parms.renderView.y = 0;
        parms.renderView.width = SCREEN_WIDTH;
        parms.renderView.height = SCREEN_HEIGHT;

        tr.RenderViewToViewport(parms.renderView, parms.viewport);

        parms.scissor.x1 = 0;
        parms.scissor.y1 = 0;
        parms.scissor.x2 = parms.viewport.x2 - parms.viewport.x1;
        parms.scissor.y2 = parms.viewport.y2 - parms.viewport.y1;

        parms.superView = tr.viewDef;
        parms.subviewSurface = surf;

        // triangle culling order changes with mirroring
        parms.isMirror = (parms.isMirror ^ tr.viewDef.isMirror);

        // generate render commands for it
        R_RenderView(parms);

        // copy this rendering to the image
        stage.dynamicFrameCount = tr.frameCount;
        stage.image[0] = globalImages.scratchImage;

        tr.CaptureRenderToImage(stage.image[0].imgName.toString());
        tr.UnCrop();
    }

    /*
     =================
     R_XrayRender
     =================
     */
    public static void R_XrayRender(drawSurf_s surf, textureStage_t stage, idScreenRect scissor) {
        viewDef_s parms;

        // remote views can be reused in a single frame
        if (stage.dynamicFrameCount == tr.frameCount) {
            return;
        }

        // issue a new view command
        parms = R_XrayViewBySurface(surf);
        if (null == parms) {
            return;
        }

        tr.CropRenderSize(stage.width, stage.height, true);

        parms.renderView.x = 0;
        parms.renderView.y = 0;
        parms.renderView.width = SCREEN_WIDTH;
        parms.renderView.height = SCREEN_HEIGHT;

        tr.RenderViewToViewport(parms.renderView, parms.viewport);

        parms.scissor.x1 = 0;
        parms.scissor.y1 = 0;
        parms.scissor.x2 = parms.viewport.x2 - parms.viewport.x1;
        parms.scissor.y2 = parms.viewport.y2 - parms.viewport.y1;

        parms.superView = tr.viewDef;
        parms.subviewSurface = surf;

        // triangle culling order changes with mirroring
        parms.isMirror = (parms.isMirror ^ tr.viewDef.isMirror);// != 0 );

        // generate render commands for it
        R_RenderView(parms);

        // copy this rendering to the image
        stage.dynamicFrameCount = tr.frameCount;
        stage.image[0] = globalImages.scratchImage2;

        tr.CaptureRenderToImage(stage.image[0].imgName.toString());
        tr.UnCrop();
    }

    /*
     ==================
     R_GenerateSurfaceSubview
     ==================
     */
    public static boolean R_GenerateSurfaceSubview(drawSurf_s drawSurf) {
        idBounds ndcBounds = new idBounds();
        viewDef_s parms;
        final idMaterial shader;

        // for testing the performance hit
        if (r_skipSubviews.GetBool()) {
            return false;
        }

        if (R_PreciseCullSurface(drawSurf, ndcBounds)) {
            return false;
        }

        shader = drawSurf.material;

        // never recurse through a subview surface that we are
        // already seeing through
        for (parms = tr.viewDef; parms != null; parms = parms.superView) {
            if (parms.subviewSurface != null
                    && parms.subviewSurface.geo == drawSurf.geo
                    && parms.subviewSurface.space.entityDef == drawSurf.space.entityDef) {
                break;
            }
        }
        if (parms != null) {
            return false;
        }

        // crop the scissor bounds based on the precise cull
        idScreenRect scissor = new idScreenRect();

        idScreenRect v = tr.viewDef.viewport;
        scissor.x1 = v.x1 + (int) ((v.x2 - v.x1 + 1) * 0.5f * (ndcBounds.oGet(0, 0) + 1.0f));
        scissor.y1 = v.y1 + (int) ((v.y2 - v.y1 + 1) * 0.5f * (ndcBounds.oGet(0, 1) + 1.0f));
        scissor.x2 = v.x1 + (int) ((v.x2 - v.x1 + 1) * 0.5f * (ndcBounds.oGet(1, 0) + 1.0f));
        scissor.y2 = v.y1 + (int) ((v.y2 - v.y1 + 1) * 0.5f * (ndcBounds.oGet(1, 1) + 1.0f));

        // nudge a bit for safety
        scissor.Expand();

        scissor.Intersect(tr.viewDef.scissor);

        if (scissor.IsEmpty()) {
            // cropped out
            return false;
        }

        // see what kind of subview we are making
        if (shader.GetSort() != SS_SUBVIEW) {
            for (int i = 0; i < shader.GetNumStages(); i++) {
                final shaderStage_t stage = shader.GetStage(i);
                switch (stage.texture.dynamic) {
                    case DI_REMOTE_RENDER:
                        R_RemoteRender(drawSurf, stage.texture);
                        break;
                    case DI_MIRROR_RENDER:
                        R_MirrorRender(drawSurf, /*const_cast<textureStage_t *>*/ (stage.texture), scissor);
                        break;
                    case DI_XRAY_RENDER:
                        R_XrayRender(drawSurf, /*const_cast<textureStage_t *>*/ (stage.texture), scissor);
                        break;
                }
            }
            return true;
        }

        // issue a new view command
        parms = R_MirrorViewBySurface(drawSurf);
        if (null == parms) {
            return false;
        }

        parms.scissor = scissor;
        parms.superView = tr.viewDef;
        parms.subviewSurface = drawSurf;

        // triangle culling order changes with mirroring
        parms.isMirror = (parms.isMirror ^ tr.viewDef.isMirror);// != 0 );

        // generate render commands for it
        R_RenderView(parms);

        return true;
    }

    /*
     ================
     R_GenerateSubViews

     If we need to render another view to complete the current view,
     generate it first.

     It is important to do this after all drawSurfs for the current
     view have been generated, because it may create a subview which
     would change tr.viewCount.
     ================
     */
    public static boolean R_GenerateSubViews() {
        drawSurf_s drawSurf;
        int i;
        boolean subviews;
        idMaterial shader;

        // for testing the performance hit
        if (RenderSystem_init.r_skipSubviews.GetBool()) {
            return false;
        }

        subviews = false;

        // scan the surfaces until we either find a subview, or determine
        // there are no more subview surfaces.
        for (i = 0; i < tr.viewDef.numDrawSurfs; i++) {
            drawSurf = tr.viewDef.drawSurfs[i];
            shader = drawSurf.material;

            if (null == shader || !shader.HasSubview()) {
                continue;
            }

            if (R_GenerateSurfaceSubview(drawSurf)) {
                subviews = true;
            }
        }

        return subviews;
    }
}
