package neo.Renderer;

import neo.Renderer.Interaction.idInteraction;
import static neo.Renderer.Material.MF_NOPORTALFOG;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.ModelDecal.idRenderModelDecal;
import static neo.Renderer.ModelManager.renderModelManager;
import neo.Renderer.ModelOverlay.idRenderModelOverlay;
import static neo.Renderer.RenderSystem_init.r_showUpdates;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Renderer.RenderWorld_local.doublePortal_s;
import neo.Renderer.RenderWorld_local.idRenderWorldLocal;
import neo.Renderer.RenderWorld_local.portalArea_s;
import neo.Renderer.RenderWorld_local.portal_s;
import neo.Renderer.tr_local.areaReference_s;
import neo.Renderer.tr_local.idRenderEntityLocal;
import neo.Renderer.tr_local.idRenderLightLocal;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_main.R_AxisToModelMatrix;
import static neo.Renderer.tr_main.R_LocalPlaneToGlobal;
import static neo.Renderer.tr_main.R_LocalPointToGlobal;
import static neo.Renderer.tr_polytope.R_PolytopeSurface;
import static neo.Renderer.tr_stencilshadow.R_MakeShadowFrustums;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurf;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.Session.session;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Vector.getVec3_origin;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

/**
 *
 */
public class tr_lightrun {

    /*


     Prelight models

     "_prelight_<lightname>", ie "_prelight_light1"

     Static surfaces available to dmap will be processed to optimized
     shadow and lit surface geometry

     Entity models are never prelighted.

     Light entity can have a "noPrelight 1" key set to avoid the preprocessing
     and carving of the world.  A light that will move should usually have this
     set.

     Prelight models will usually have multiple surfaces

     Shadow volume surfaces will have the material "_shadowVolume"

     The exact same vertexes as the ambient surfaces will be used for the
     non-shadow surfaces, so there is opportunity to share


     Reference their parent surfaces?
     Reference their parent area?


     If we don't track parts that are in different areas, there will be huge
     losses when an areaportal closed door has a light poking slightly
     through it.

     There is potential benefit to splitting even the shadow volumes
     at area boundaries, but it would involve the possibility of an
     extra plane of shadow drawing at the area boundary.


     interaction	lightName	numIndexes

     Shadow volume surface

     Surfaces in the world cannot have "no self shadow" properties, because all
     the surfaces are considered together for the optimized shadow volume.  If
     you want no self shadow on a static surface, you must still make it into an
     entity so it isn't considered in the prelight.


     r_hidePrelights
     r_hideNonPrelights



     each surface could include prelight indexes

     generation procedure in dmap:

     carve original surfaces into areas

     for each light
     build shadow volume and beam tree
     cut all potentially lit surfaces into the beam tree
     move lit fragments into a new optimize group

     optimize groups

     build light models




     */

    /*
     =================================================================================

     LIGHT TESTING

     =================================================================================
     */
    /*
     ====================
     R_ModulateLights_f

     Modifies the shaderParms on all the lights so the level
     designers can easily test different color schemes
     ====================
     */
    public static class R_ModulateLights_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ModulateLights_f();

        private R_ModulateLights_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (null == tr.primaryWorld) {
                return;
            }
            if (args.Argc() != 4) {
                common.Printf("usage: modulateLights <redFloat> <greenFloat> <blueFloat>\n");
                return;
            }

            float[] modulate = new float[3];
            int i;
            for (i = 0; i < 3; i++) {
                modulate[i] = Float.parseFloat(args.Argv(i + 1));
            }

            int count = 0;
            for (i = 0; i < tr.primaryWorld.lightDefs.Num(); i++) {
                idRenderLightLocal light;

                light = tr.primaryWorld.lightDefs.oGet(i);
                if (light != null) {
                    count++;
                    for (int j = 0; j < 3; j++) {
                        light.parms.shaderParms[j] *= modulate[j];
                    }
                }
            }
            common.Printf("modulated %d lights\n", count);
        }
    };

//======================================================================================
    /*
     ===============
     R_CreateEntityRefs

     Creates all needed model references in portal areas,
     chaining them to both the area and the entityDef.

     Bumps tr.viewCount.
     ===============
     */
    public static void R_CreateEntityRefs(idRenderEntityLocal def) {
        int i;
        idVec3[] transformed = idVec3.generateArray(8);
        idVec3 v = new idVec3();

        if (null == def.parms.hModel) {
            def.parms.hModel = renderModelManager.DefaultModel();
        }

        // if the entity hasn't been fully specified due to expensive animation calcs
        // for md5 and particles, use the provided conservative bounds.
        if (def.parms.callback != null) {
            def.referenceBounds.oSet(def.parms.bounds);
        } else {
            def.referenceBounds.oSet(def.parms.hModel.Bounds(def.parms));
        }

        // some models, like empty particles, may not need to be added at all
        if (def.referenceBounds.IsCleared()) {
            return;
        }

        if (r_showUpdates.GetBool()
                && (def.referenceBounds.oGet(1, 0) - def.referenceBounds.oGet(0, 0) > 1024
                || def.referenceBounds.oGet(1, 1) - def.referenceBounds.oGet(0, 1) > 1024)) {
            common.Printf("big entityRef: %f,%f\n", def.referenceBounds.oGet(1, 0) - def.referenceBounds.oGet(0, 0),
                    def.referenceBounds.oGet(1, 1) - def.referenceBounds.oGet(0, 1));
        }

        for (i = 0; i < 8; i++) {
            v.oSet(0, def.referenceBounds.oGet((i >> 0) & 1, 0));
            v.oSet(1, def.referenceBounds.oGet((i >> 1) & 1, 1));
            v.oSet(2, def.referenceBounds.oGet((i >> 2) & 1, 2));

            transformed[i] = R_LocalPointToGlobal(def.modelMatrix, v);
        }

        // bump the view count so we can tell if an
        // area already has a reference
        tr.viewCount++;
//        System.out.println("tr.viewCount::R_CreateEntityRefs");

        // push these points down the BSP tree into areas
        def.world.PushVolumeIntoTree(def, null, 8, transformed);
    }


    /*
     =================================================================================

     CREATE LIGHT REFS

     =================================================================================
     */

    /*
     =====================
     R_SetLightProject

     All values are reletive to the origin
     Assumes that right and up are not normalized
     This is also called by dmap during map processing.
     =====================
     */
    public static void R_SetLightProject(idPlane[] lightProject/*[4]*/, final idVec3 origin, final idVec3 target,
            final idVec3 rightVector, final idVec3 upVector, final idVec3 start, final idVec3 stop) {
        float dist;
        float scale;
        float rLen, uLen;
        idVec3 normal;
        float ofs;
        idVec3 right, up;
        idVec3 startGlobal;
        idVec4 targetGlobal = new idVec4();

        right = rightVector;
        rLen = right.Normalize();
        up = upVector;
        uLen = up.Normalize();
        normal = up.Cross(right);
//normal = right.Cross( up );
        normal.Normalize();

        dist = target.oMultiply(normal); //  - ( origin * normal );
        if (dist < 0) {
            dist = -dist;
            normal = normal.oNegative();
        }

        scale = (0.5f * dist) / rLen;
        right.oMulSet(scale);
        scale = -(0.5f * dist) / uLen;
        up.oMulSet(scale);

        lightProject[2].oSet(normal);
        lightProject[2].oSet(3, -(origin.oMultiply(lightProject[2].Normal())));

        lightProject[0].oSet(right);
        lightProject[0].oSet(3, -(origin.oMultiply(lightProject[0].Normal())));

        lightProject[1].oSet(up);
        lightProject[1].oSet(3, -(origin.oMultiply(lightProject[1].Normal())));

        // now offset to center
        targetGlobal.ToVec3().oSet(target.oPluSet(origin));
        targetGlobal.oSet(3, 1);
        ofs = 0.5f - (targetGlobal.oMultiply(lightProject[0].ToVec4())) / (targetGlobal.oMultiply(lightProject[2].ToVec4()));
        lightProject[0].ToVec4().oPluSet(lightProject[2].ToVec4().oMultiply(ofs));
        ofs = 0.5f - (targetGlobal.oMultiply(lightProject[1].ToVec4())) / (targetGlobal.oMultiply(lightProject[2].ToVec4()));
        lightProject[1].ToVec4().oPluSet(lightProject[2].ToVec4().oMultiply(ofs));

        // set the falloff vector
        normal = stop.oMinus(start);
        dist = normal.Normalize();
        if (dist <= 0) {
            dist = 1;
        }
        lightProject[3].oSet(normal.oMultiply(1.0f / dist));
        startGlobal = start.oPlus(origin);
        lightProject[3].oSet(3, -(startGlobal.oMultiply(lightProject[3].Normal())));
    }

    /*
     ===================
     R_SetLightFrustum

     Creates plane equations from the light projection, positive sides
     face out of the light
     ===================
     */
    public static void R_SetLightFrustum(final idPlane[] lightProject/*[4]*/, idPlane[] frustum/*[6]*/) {
        int i;

        // we want the planes of s=0, s=q, t=0, and t=q
        frustum[0] = new idPlane(lightProject[0]);
        frustum[1] = new idPlane(lightProject[1]);
        frustum[2] = lightProject[2].oMinus(lightProject[0]);
        frustum[3] = lightProject[2].oMinus(lightProject[1]);

        // we want the planes of s=0 and s=1 for front and rear clipping planes
        frustum[4] = new idPlane(lightProject[3]);

        frustum[5] = new idPlane(lightProject[3]);
        frustum[5].oMinSet(3, 1.0f);
        frustum[5] = frustum[5].oNegative();

        for (i = 0; i < 6; i++) {
            float f;

            frustum[i] = frustum[i].oNegative();
            f = frustum[i].Normalize();
            frustum[i].oDivSet(3, f);
        }
    }

    /*
     ====================
     R_FreeLightDefFrustum
     ====================
     */
    public static void R_FreeLightDefFrustum(idRenderLightLocal ldef) {
        int i;

        // free the frustum tris
        if (ldef.frustumTris != null) {
            R_FreeStaticTriSurf(ldef.frustumTris);
            ldef.frustumTris = null;
        }
        // free frustum windings
        for (i = 0; i < 6; i++) {
            if (ldef.frustumWindings[i] != null) {
//			delete ldef.frustumWindings[i];
                ldef.frustumWindings[i] = null;
            }
        }
    }

    /*
     =================
     R_DeriveLightData

     Fills everything in based on light.parms
     =================
     */
    public static void R_DeriveLightData(idRenderLightLocal light) throws idException {
        int i;

        // decide which light shader we are going to use
        if (light.parms.shader != null) {
            light.lightShader = light.parms.shader;
        }
        if (null == light.lightShader) {
            if (light.parms.pointLight) {
                light.lightShader = declManager.FindMaterial("lights/defaultPointLight");
            } else {
                light.lightShader = declManager.FindMaterial("lights/defaultProjectedLight");
            }
        }

        // get the falloff image
        light.falloffImage = light.lightShader.LightFalloffImage();
        if (null == light.falloffImage) {
            // use the falloff from the default shader of the correct type
            final idMaterial defaultShader;

            if (light.parms.pointLight) {
                defaultShader = declManager.FindMaterial("lights/defaultPointLight");
                light.falloffImage = defaultShader.LightFalloffImage();
            } else {
                // projected lights by default don't diminish with distance
                defaultShader = declManager.FindMaterial("lights/defaultProjectedLight");
                light.falloffImage = defaultShader.LightFalloffImage();
            }
        }

        // set the projection
        if (!light.parms.pointLight) {
            // projected light

            R_SetLightProject(light.lightProject, getVec3_origin() /* light.parms.origin */ , light.parms.target,
                    light.parms.right, light.parms.up, light.parms.start, light.parms.end);
        } else {
            // point light
//            memset(light.lightProject, 0, sizeof(light.lightProject));
            for (int l = 0; l < light.lightProject.length; l++) {
                light.lightProject[l] = new idPlane();
            }
            light.lightProject[0].oSet(0, 0.5f / light.parms.lightRadius.oGet(0));
            light.lightProject[1].oSet(1, 0.5f / light.parms.lightRadius.oGet(1));
            light.lightProject[3].oSet(2, 0.5f / light.parms.lightRadius.oGet(2));
            light.lightProject[0].oSet(3, 0.5f);
            light.lightProject[1].oSet(3, 0.5f);
            light.lightProject[2].oSet(3, 1.0f);
            light.lightProject[3].oSet(3, 0.5f);
        }

        // set the frustum planes
        R_SetLightFrustum(light.lightProject, light.frustum);

        // rotate the light planes and projections by the axis
        R_AxisToModelMatrix(light.parms.axis, light.parms.origin, light.modelMatrix);

        for (i = 0; i < 6; i++) {
            idPlane temp;
            temp = light.frustum[i];
            R_LocalPlaneToGlobal(light.modelMatrix, temp, light.frustum[i]);
        }
        for (i = 0; i < 4; i++) {
            idPlane temp;
            temp = light.lightProject[i];
            R_LocalPlaneToGlobal(light.modelMatrix, temp, light.lightProject[i]);
        }

        // adjust global light origin for off center projections and parallel projections
        // we are just faking parallel by making it a very far off center for now
        if (light.parms.parallel) {
            idVec3 dir;

            dir = light.parms.lightCenter;
            if (0 == dir.Normalize()) {
                // make point straight up if not specified
                dir.oSet(2, 1);
            }
            light.globalLightOrigin = light.parms.origin.oPlus(dir.oMultiply(100000));
        } else {
            light.globalLightOrigin = light.parms.origin.oPlus(light.parms.axis.oMultiply(light.parms.lightCenter));
        }

        R_FreeLightDefFrustum(light);

        light.frustumTris = R_PolytopeSurface(6, light.frustum, light.frustumWindings);

        // a projected light will have one shadowFrustum, a point light will have
        // six unless the light center is outside the box
        R_MakeShadowFrustums(light);
    }

    /*
     =================
     R_CreateLightRefs
     =================
     */
    static final int MAX_LIGHT_VERTS = 40;

    public static void R_CreateLightRefs(idRenderLightLocal light) {
        idVec3[] points = new idVec3[MAX_LIGHT_VERTS];
        int i;
        srfTriangles_s tri;

        tri = light.frustumTris;

        // because a light frustum is made of only six intersecting planes,
        // we should never be able to get a stupid number of points...
        if (tri.numVerts > MAX_LIGHT_VERTS) {
            common.Error("R_CreateLightRefs: %d points in frustumTris!", tri.numVerts);
        }
        for (i = 0; i < tri.numVerts; i++) {
            points[i] = tri.verts[i].xyz;
        }

        if (r_showUpdates.GetBool() && (tri.bounds.oGet(1, 0) - tri.bounds.oGet(0, 0) > 1024
                || tri.bounds.oGet(1, 1) - tri.bounds.oGet(0, 1) > 1024)) {
            common.Printf("big lightRef: %f,%f\n", tri.bounds.oGet(1, 0) - tri.bounds.oGet(0, 0), tri.bounds.oGet(1, 1) - tri.bounds.oGet(0, 1));
        }

        // determine the areaNum for the light origin, which may let us
        // cull the light if it is behind a closed door
        // it is debatable if we want to use the entity origin or the center offset origin,
        // but we definitely don't want to use a parallel offset origin
        light.areaNum = light.world.PointInArea(light.globalLightOrigin);
        if (light.areaNum == -1) {
            light.areaNum = light.world.PointInArea(light.parms.origin);
        }

        // bump the view count so we can tell if an
        // area already has a reference
        tr.viewCount++;
//        System.out.println("tr.viewCount::R_CreateLightRefs");

        // if we have a prelight model that includes all the shadows for the major world occluders,
        // we can limit the area references to those visible through the portals from the light center.
        // We can't do this in the normal case, because shadows are cast from back facing triangles, which
        // may be in areas not directly visible to the light projection center.
        if (light.parms.prelightModel != null && RenderSystem_init.r_useLightPortalFlow.GetBool() && light.lightShader.LightCastsShadows()) {
            light.world.FlowLightThroughPortals(light);
        } else {
            // push these points down the BSP tree into areas
            light.world.PushVolumeIntoTree(null, light, tri.numVerts, points);
        }
    }

    /*
     ===============
     R_RenderLightFrustum

     Called by the editor and dmap to operate on light volumes
     ===============
     */
    public static void R_RenderLightFrustum(final renderLight_s renderLight, idPlane[] lightFrustum/*[6]*/) {
        idRenderLightLocal fakeLight = new idRenderLightLocal();

//	memset( &fakeLight, 0, sizeof( fakeLight ) );
        fakeLight.parms = renderLight;

        R_DeriveLightData(fakeLight);

        R_FreeStaticTriSurf(fakeLight.frustumTris);

        for (int i = 0; i < 6; i++) {
            lightFrustum[i] = fakeLight.frustum[i];
        }
    }

//=================================================================================

    /*
     ===============
     WindingCompletelyInsideLight
     ===============
     */
    public static boolean WindingCompletelyInsideLight(final idWinding w, final idRenderLightLocal ldef) {
        int i, j;

        for (i = 0; i < w.GetNumPoints(); i++) {
            for (j = 0; j < 6; j++) {
                float d;

                d = w.oGet(i).ToVec3().oMultiply(ldef.frustum[j].Normal()) + ldef.frustum[j].oGet(3);
                if (d > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     ======================
     R_CreateLightDefFogPortals

     When a fog light is created or moved, see if it completely
     encloses any portals, which may allow them to be fogged closed.
     ======================
     */
    public static void R_CreateLightDefFogPortals(idRenderLightLocal ldef) {
        areaReference_s lref;
        portalArea_s area;

        ldef.foggedPortals = null;

        if (!ldef.lightShader.IsFogLight()) {
            return;
        }

        // some fog lights will explicitly disallow portal fogging
        if (ldef.lightShader.TestMaterialFlag(MF_NOPORTALFOG)) {
            return;
        }

        for (lref = ldef.references; lref != null; lref = lref.ownerNext) {
            // check all the models in this area
            area = lref.area;

            portal_s prt;
            doublePortal_s dp;

            for (prt = area.portals; prt != null; prt = prt.next) {
                dp = prt.doublePortal;

                // we only handle a single fog volume covering a portal
                // this will never cause incorrect drawing, but it may
                // fail to cull a portal 
                if (dp.fogLight != null) {
                    continue;
                }

                if (WindingCompletelyInsideLight(prt.w, ldef)) {
                    dp.fogLight = ldef;
                    dp.nextFoggedPortal = ldef.foggedPortals;
                    ldef.foggedPortals = dp;
                }
            }
        }
    }

    /*
     ====================
     R_FreeLightDefDerivedData

     Frees all references and lit surfaces from the light
     ====================
     */
    public static void R_FreeLightDefDerivedData(idRenderLightLocal ldef) {
        areaReference_s lref, nextRef;

        // rmove any portal fog references
        for (doublePortal_s dp = ldef.foggedPortals; dp != null; dp = dp.nextFoggedPortal) {
            dp.fogLight = null;
        }

        // free all the interactions
        while (ldef.firstInteraction != null) {
            ldef.firstInteraction.UnlinkAndFree();
        }

        // free all the references to the light
        for (lref = ldef.references; lref != null; lref = nextRef) {
            nextRef = lref.ownerNext;

            // unlink from the area
            lref.areaNext.areaPrev = lref.areaPrev;
            lref.areaPrev.areaNext = lref.areaNext;

//            // put it back on the free list for reuse
//            ldef.world.areaReferenceAllocator.Free(lref);
        }
        ldef.references = null;

        R_FreeLightDefFrustum(ldef);
    }

    /*
     ===================
     R_FreeEntityDefDerivedData

     Used by both RE_FreeEntityDef and RE_UpdateEntityDef
     Does not actually free the entityDef.
     ===================
     */
    public static void R_FreeEntityDefDerivedData(idRenderEntityLocal def, boolean keepDecals, boolean keepCachedDynamicModel) {
        int i;
        areaReference_s ref, next;

        // demo playback needs to free the joints, while normal play
        // leaves them in the control of the game
        if (session.readDemo != null) {
            if (def.parms.joints != null) {
//			Mem_Free16( def.parms.joints );
                def.parms.joints = null;
            }
            if (def.parms.callbackData != null) {
//			Mem_Free( def.parms.callbackData );
                def.parms.callbackData = null;
            }
            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                if (def.parms.gui[i] != null) {
//				delete def.parms.gui[ i ];
                    def.parms.gui[i] = null;
                }
            }
        }

        // free all the interactions
        while (def.firstInteraction != null) {
            def.firstInteraction.UnlinkAndFree();
        }

        // clear the dynamic model if present
        if (def.dynamicModel != null) {
            def.dynamicModel = null;
        }

        if (!keepDecals) {
            R_FreeEntityDefDecals(def);
            R_FreeEntityDefOverlay(def);
        }

        if (!keepCachedDynamicModel) {
//		delete def.cachedDynamicModel;
            def.cachedDynamicModel = null;
        }

        // free the entityRefs from the areas
        for (ref = def.entityRefs; ref != null; ref = next) {
            next = ref.ownerNext;

            // unlink from the area
            ref.areaNext.areaPrev = ref.areaPrev;
            ref.areaPrev.areaNext = ref.areaNext;

//            // put it back on the free list for reuse
//            def.world.areaReferenceAllocator.Free(ref);
        }
        def.entityRefs = null;
    }

    /*
     ==================
     R_ClearEntityDefDynamicModel

     If we know the reference bounds stays the same, we
     only need to do this on entity update, not the full
     R_FreeEntityDefDerivedData
     ==================
     */
    public static void R_ClearEntityDefDynamicModel(idRenderEntityLocal def) {
        // free all the interaction surfaces
        for (idInteraction inter = def.firstInteraction; inter != null && !inter.IsEmpty(); inter = inter.entityNext) {
            inter.FreeSurfaces();
        }

        // clear the dynamic model if present
        if (def.dynamicModel != null) {
            def.dynamicModel = null;
        }
    }

    /*
     ===================
     R_FreeEntityDefDecals
     ===================
     */
    public static void R_FreeEntityDefDecals(idRenderEntityLocal def) {
        while (def.decals != null) {
            idRenderModelDecal next = def.decals.Next();
            idRenderModelDecal.Free(def.decals);
            def.decals = next;
        }
    }

    /*
     ===================
     R_FreeEntityDefFadedDecals
     ===================
     */
    public static void R_FreeEntityDefFadedDecals(idRenderEntityLocal def, int time) {
        def.decals = idRenderModelDecal.RemoveFadedDecals(def.decals, time);
    }

    /*
     ===================
     R_FreeEntityDefOverlay
     ===================
     */
    public static void R_FreeEntityDefOverlay(idRenderEntityLocal def) {
        if (def.overlay != null) {
            idRenderModelOverlay.Free(def.overlay);
            def.overlay = null;
        }
    }

    /*
     ===================
     R_FreeDerivedData

     ReloadModels and RegenerateWorld call this
     // FIXME: need to do this for all worlds
     ===================
     */
    public static void R_FreeDerivedData() {
        int i, j;
        idRenderWorldLocal rw;
        idRenderEntityLocal def;
        idRenderLightLocal light;

        for (j = 0; j < tr.worlds.Num(); j++) {
            rw = tr.worlds.oGet(j);

            for (i = 0; i < rw.entityDefs.Num(); i++) {
                def = rw.entityDefs.oGet(i);
                if (null == def) {
                    continue;
                }
                R_FreeEntityDefDerivedData(def, false, false);
            }

            for (i = 0; i < rw.lightDefs.Num(); i++) {
                light = rw.lightDefs.oGet(i);
                if (null == light) {
                    continue;
                }
                R_FreeLightDefDerivedData(light);
            }
        }
    }

    /*
     ===================
     R_CheckForEntityDefsUsingModel
     ===================
     */
    public static void R_CheckForEntityDefsUsingModel(idRenderModel model) {
        int i, j;
        idRenderWorldLocal rw;
        idRenderEntityLocal def;

        for (j = 0; j < tr.worlds.Num(); j++) {
            rw = tr.worlds.oGet(j);

            for (i = 0; i < rw.entityDefs.Num(); i++) {
                def = rw.entityDefs.oGet(i);
                if (null == def) {
                    continue;
                }
                if (def.parms.hModel == model) {
                    //assert( 0 );
                    // this should never happen but Radiant messes it up all the time so just free the derived data
                    R_FreeEntityDefDerivedData(def, false, false);
                }
            }
        }
    }

    /*
     ===================
     R_ReCreateWorldReferences

     ReloadModels and RegenerateWorld call this
     // FIXME: need to do this for all worlds
     ===================
     */
    public static void R_ReCreateWorldReferences() {
        int i, j;
        idRenderWorldLocal rw;
        idRenderEntityLocal def;
        idRenderLightLocal light;

        // let the interaction generation code know this shouldn't be optimized for
        // a particular view
        tr.viewDef = null;

        for (j = 0; j < tr.worlds.Num(); j++) {
            rw = tr.worlds.oGet(j);

            for (i = 0; i < rw.entityDefs.Num(); i++) {
                def = rw.entityDefs.oGet(i);
                if (null == def) {
                    continue;
                }
                // the world model entities are put specifically in a single
                // area, instead of just pushing their bounds into the tree
                if (i < rw.numPortalAreas) {
                    rw.AddEntityRefToArea(def, rw.portalAreas[i]);
                } else {
                    R_CreateEntityRefs(def);
                }
            }

            for (i = 0; i < rw.lightDefs.Num(); i++) {
                light = rw.lightDefs.oGet(i);
                if (null == light) {
                    continue;
                }
                renderLight_s parms = light.parms;

                light.world.FreeLightDef(i);
                rw.UpdateLightDef(i, parms);
            }
        }
    }

    /*
     ===================
     R_RegenerateWorld_f

     Frees and regenerates all references and interactions, which
     must be done when switching between display list mode and immediate mode
     ===================
     */
    public static class R_RegenerateWorld_f extends neo.framework.CmdSystem.cmdFunction_t {

        private static final cmdFunction_t instance = new R_RegenerateWorld_f();

        private R_RegenerateWorld_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            R_FreeDerivedData();

            // watch how much memory we allocate
            tr.staticAllocCount = 0;

            R_ReCreateWorldReferences();

            common.Printf("Regenerated world, staticAllocCount = %d.\n", tr.staticAllocCount);
        }
    };
}
