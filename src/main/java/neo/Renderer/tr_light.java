package neo.Renderer;

import static java.lang.Math.random;
import static neo.Game.Game_local.game;
import neo.Renderer.Interaction.idInteraction;
import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.shaderStage_t;
import static neo.Renderer.Model.dynamicModel_t.DM_CONTINUOUS;
import static neo.Renderer.Model.dynamicModel_t.DM_STATIC;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.lightingCache_s;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.shadowCache_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.ModelDecal.idRenderModelDecal;
import neo.Renderer.ModelOverlay.idRenderModelOverlay;
import static neo.Renderer.RenderSystem_init.r_checkBounds;
import static neo.Renderer.RenderSystem_init.r_lightSourceRadius;
import static neo.Renderer.RenderSystem_init.r_showEntityScissors;
import static neo.Renderer.RenderSystem_init.r_showLightScissors;
import static neo.Renderer.RenderSystem_init.r_singleSurface;
import static neo.Renderer.RenderSystem_init.r_skipOverlays;
import static neo.Renderer.RenderSystem_init.r_skipSpecular;
import static neo.Renderer.RenderSystem_init.r_skipSuppress;
import static neo.Renderer.RenderSystem_init.r_useClippedLightScissors;
import static neo.Renderer.RenderSystem_init.r_useEntityScissors;
import static neo.Renderer.RenderSystem_init.r_useIndexBuffers;
import static neo.Renderer.RenderSystem_init.r_useLightScissors;
import static neo.Renderer.RenderSystem_init.r_useOptimizedShadows;
import static neo.Renderer.RenderSystem_init.r_useShadowCulling;
import static neo.Renderer.RenderSystem_init.r_useShadowSurfaceScissor;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.Renderer.RenderWorld.R_GlobalShaderOverride;
import static neo.Renderer.RenderWorld.R_RemapShaderBySkin;
import neo.Renderer.RenderWorld.renderEntity_s;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.tr_deform.R_DeformDrawSurf;
import static neo.Renderer.tr_guisurf.R_RenderGuiSurf;
import static neo.Renderer.tr_lightrun.R_ClearEntityDefDynamicModel;
import static neo.Renderer.tr_local.DSF_VIEW_INSIDE_SHADOW;
import static neo.Renderer.tr_local.INITIAL_DRAWSURFS;
import neo.Renderer.tr_local.areaReference_s;
import static neo.Renderer.tr_local.backEndName_t.BE_ARB;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.idRenderEntityLocal;
import neo.Renderer.tr_local.idRenderLightLocal;
import neo.Renderer.tr_local.idScreenRect;
import static neo.Renderer.tr_local.tr;
import neo.Renderer.tr_local.viewEntity_s;
import neo.Renderer.tr_local.viewLight_s;
import static neo.Renderer.tr_main.R_AxisToModelMatrix;
import static neo.Renderer.tr_main.R_CullLocalBox;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_main.R_LocalPointToGlobal;
import static neo.Renderer.tr_main.R_ScreenRectFromViewFrustumBounds;
import static neo.Renderer.tr_main.R_ShowColoredScreenRect;
import static neo.Renderer.tr_main.R_TransformClipToDevice;
import static neo.Renderer.tr_main.R_TransformModelToClip;
import static neo.Renderer.tr_main.myGlMultMatrix;
import static neo.Renderer.tr_subview.R_PreciseCullSurface;
import static neo.Renderer.tr_trisurf.R_DeriveTangents;
import static neo.TempDump.NOT;
import static neo.framework.Common.common;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Box.idBox;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Simd.SIMDProcessor;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import static neo.idlib.precompiled.MAX_EXPRESSION_REGISTERS;
import neo.ui.UserInterface.idUserInterface;

import java.util.stream.Stream;

/**
 *
 */
public class tr_light {

    public static final float CHECK_BOUNDS_EPSILON = 1.0f;

    /*
     ======================================================================================================================================================================================

     VERTEX CACHE GENERATORS

     ======================================================================================================================================================================================
     */
    /*
     ==================
     R_CreateAmbientCache

     Create it if needed
     ==================
     */
    public static boolean R_CreateAmbientCache(srfTriangles_s tri, boolean needsLighting) {
        if (tri.ambientCache != null) {
            return true;
        }
        // we are going to use it for drawing, so make sure we have the tangents and normals
        if (needsLighting && !tri.tangentsCalculated) {
            R_DeriveTangents(tri);
        }

        tri.ambientCache = vertexCache.Alloc(tri.verts, tri.numVerts * idDrawVert.BYTES);
        if (NOT(tri.ambientCache)) {
            return false;
        }
        return true;
    }

    /*
     ==================
     R_CreateLightingCache

     Returns false if the cache couldn't be allocated, in which case the surface should be skipped.
     ==================
     */
    public static boolean R_CreateLightingCache(final idRenderEntityLocal ent, final idRenderLightLocal light, srfTriangles_s tri) {
        idVec3 localLightOrigin = new idVec3();

        // fogs and blends don't need light vectors
        if (light.lightShader.IsFogLight() || light.lightShader.IsBlendLight()) {
            return true;
        }

        // not needed if we have vertex programs
        if (tr.backEndRendererHasVertexPrograms) {
            return true;
        }

        R_GlobalPointToLocal(ent.modelMatrix, light.globalLightOrigin, localLightOrigin);

        int size = tri.ambientSurface.numVerts;
        lightingCache_s[] cache = new lightingCache_s[size];

        if (true) {

            SIMDProcessor.CreateTextureSpaceLightVectors(cache[0].localLightVector, localLightOrigin, tri.ambientSurface.verts, tri.ambientSurface.numVerts, tri.indexes, tri.numIndexes);

        } else {
//	boolean []used = new boolean[tri.ambientSurface.numVerts];
//	memset( used, 0, tri.ambientSurface.numVerts * sizeof( used[0] ) );
//
//	// because the interaction may be a very small subset of the full surface,
//	// it makes sense to only deal with the verts used
//	for ( int j = 0; j < tri.numIndexes; j++ ) {
//		int i = tri.indexes[j];
//		if ( used[i] ) {
//			continue;
//		}
//		used[i] = true;
//
//		idVec3 lightDir;
//		const idDrawVert *v;
//
//		v = &tri.ambientSurface.verts[i];
//
//		lightDir = localLightOrigin - v.xyz;
//
//		cache[i].localLightVector[0] = lightDir * v.tangents[0];
//		cache[i].localLightVector[1] = lightDir * v.tangents[1];
//		cache[i].localLightVector[2] = lightDir * v.normal;
//	}
        }

        tri.lightingCache = vertexCache.Alloc(cache, size)[0];
        if (NOT(tri.lightingCache)) {
            return false;
        }
        return true;
    }

    /*
     ==================
     R_CreatePrivateShadowCache

     This is used only for a specific light
     ==================
     */
    public static void R_CreatePrivateShadowCache(srfTriangles_s tri) {
        if (null == tri.shadowVertexes) {
            return;
        }

        tri.shadowCache = vertexCache.Alloc(tri.shadowVertexes, tri.numVerts)[0];
    }

    /*
     ==================
     R_CreateVertexProgramShadowCache

     This is constant for any number of lights, the vertex program
     takes care of projecting the verts to infinity.
     ==================
     */
    public static void R_CreateVertexProgramShadowCache(srfTriangles_s tri) {
        if (tri.verts == null) {
            return;
        }

        shadowCache_s[] temp = Stream.generate(shadowCache_s::new).limit(tri.numVerts * 2).toArray(shadowCache_s[]::new);

//        if (true) {
//
//            SIMDProcessor.CreateVertexProgramShadowCache(temp[0].xyz, tri.verts, tri.numVerts);
//
//        } else {
        for (int i = 0; i < tri.numVerts; i++) {
            final float[] v = tri.verts[i].xyz.ToFloatPtr();
            temp[i * 2 + 0].xyz.oSet(0, v[0]);
            temp[i * 2 + 1].xyz.oSet(0, v[0]);
            temp[i * 2 + 0].xyz.oSet(1, v[1]);
            temp[i * 2 + 1].xyz.oSet(1, v[1]);
            temp[i * 2 + 0].xyz.oSet(2, v[2]);
            temp[i * 2 + 1].xyz.oSet(2, v[2]);
            temp[i * 2 + 0].xyz.oSet(3, 1.0f);        // on the model surface
            temp[i * 2 + 1].xyz.oSet(3, 0.0f);        // will be projected to infinity
        }

        tri.shadowCache = vertexCache.Alloc(temp, tri.numVerts * 2)[0];
    }

    /*
     ==================
     R_SkyboxTexGen
     ==================
     */
    public static void R_SkyboxTexGen(drawSurf_s surf, final idVec3 viewOrg) {
        int i;
        idVec3 localViewOrigin = new idVec3();

        R_GlobalPointToLocal(surf.space.modelMatrix, viewOrg, localViewOrigin);

        int numVerts = surf.geo.numVerts;
        int size = numVerts;//* sizeof( idVec3 );
        idVec3[] texCoords = new idVec3[size];

        final idDrawVert[] verts = surf.geo.verts;
        for (i = 0; i < numVerts; i++) {
            texCoords[i].oSet(0, verts[i].xyz.oGet(0) - localViewOrigin.oGet(0));
            texCoords[i].oSet(1, verts[i].xyz.oGet(1) - localViewOrigin.oGet(1));
            texCoords[i].oSet(2, verts[i].xyz.oGet(2) - localViewOrigin.oGet(2));
        }

        surf.dynamicTexCoords[0] = vertexCache.AllocFrameTemp(texCoords, size);//TODO:should [0] be set?
    }

    /*
     ==================
     R_WobbleskyTexGen
     ==================
     */
    public static void R_WobbleskyTexGen(drawSurf_s surf, final idVec3 viewOrg) {
        int i;
        idVec3 localViewOrigin = new idVec3();

        final int[] parms = surf.material.GetTexGenRegisters();

        float wobbleDegrees = surf.shaderRegisters[ parms[0]];
        float wobbleSpeed = surf.shaderRegisters[ parms[1]];
        float rotateSpeed = surf.shaderRegisters[ parms[2]];

        wobbleDegrees = wobbleDegrees * idMath.PI / 180;
        wobbleSpeed = wobbleSpeed * 2 * idMath.PI / 60;
        rotateSpeed = rotateSpeed * 2 * idMath.PI / 60;

        // very ad-hoc "wobble" transform
        float[] transform = new float[16];
        float a = tr.viewDef.floatTime * wobbleSpeed;
        float s = (float) (Math.sin(a) * Math.sin(wobbleDegrees));
        float c = (float) (Math.cos(a) * Math.sin(wobbleDegrees));
        float z = (float) Math.cos(wobbleDegrees);

        idVec3[] axis = new idVec3[3];

        axis[2].oSet(0, c);
        axis[2].oSet(1, s);
        axis[2].oSet(2, z);

        axis[1].oSet(0, (float) (-Math.sin(a * 2) * Math.sin(wobbleDegrees)));
        axis[1].oSet(2, (float) (-s * Math.sin(wobbleDegrees)));
        axis[1].oSet(1, (float) Math.sqrt(1.0f - (axis[1].oGet(0) * axis[1].oGet(0) + axis[1].oGet(2) * axis[1].oGet(2))));

        // make the second vector exactly perpendicular to the first
        axis[1].oMinSet(axis[2].oMultiply((axis[2].oMultiply(axis[1]))));
        axis[1].Normalize();

        // construct the third with a cross
        axis[0].Cross(axis[1], axis[2]);

        // add the rotate
        s = (float) Math.sin(rotateSpeed * tr.viewDef.floatTime);
        c = (float) Math.cos(rotateSpeed * tr.viewDef.floatTime);

        transform[ 0] = axis[0].oGet(0) * c + axis[1].oGet(0) * s;
        transform[ 4] = axis[0].oGet(1) * c + axis[1].oGet(1) * s;
        transform[ 8] = axis[0].oGet(2) * c + axis[1].oGet(2) * s;

        transform[ 1] = axis[1].oGet(0) * c - axis[0].oGet(0) * s;
        transform[ 5] = axis[1].oGet(1) * c - axis[0].oGet(1) * s;
        transform[ 9] = axis[1].oGet(2) * c - axis[0].oGet(2) * s;

        transform[ 2] = axis[2].oGet(0);
        transform[ 6] = axis[2].oGet(1);
        transform[10] = axis[2].oGet(2);

        transform[3] = transform[7] = transform[11] = 0.0f;
        transform[12] = transform[13] = transform[14] = 0.0f;

        R_GlobalPointToLocal(surf.space.modelMatrix, viewOrg, localViewOrigin);

        int numVerts = surf.geo.numVerts;
        int size = numVerts;// sizeof(idVec3);
        idVec3[] texCoords = new idVec3[size];

        final idDrawVert[] verts = surf.geo.verts;
        for (i = 0; i < numVerts; i++) {
            idVec3 v = new idVec3();

            v.oSet(0, verts[i].xyz.oGet(0) - localViewOrigin.oGet(0));
            v.oSet(1, verts[i].xyz.oGet(1) - localViewOrigin.oGet(1));
            v.oSet(2, verts[i].xyz.oGet(2) - localViewOrigin.oGet(2));

            texCoords[i] = R_LocalPointToGlobal(transform, v);
        }

        surf.dynamicTexCoords[0] = vertexCache.AllocFrameTemp(texCoords, size);
    }

    /*
     =================
     R_SpecularTexGen

     Calculates the specular coordinates for cards without vertex programs.
     =================
     */
    public static void R_SpecularTexGen(drawSurf_s surf, final idVec3 globalLightOrigin, final idVec3 viewOrg) {
        srfTriangles_s tri;
        idVec3 localLightOrigin = new idVec3();
        idVec3 localViewOrigin = new idVec3();

        R_GlobalPointToLocal(surf.space.modelMatrix, globalLightOrigin, localLightOrigin);
        R_GlobalPointToLocal(surf.space.modelMatrix, viewOrg, localViewOrigin);

        tri = surf.geo;

        // FIXME: change to 3 component?
        int size = tri.numVerts;// * sizeof( idVec4 );
        idVec4[] texCoords = new idVec4[size];

        if (true) {

            SIMDProcessor.CreateSpecularTextureCoords(texCoords, localLightOrigin, localViewOrigin,
                    tri.verts, tri.numVerts, tri.indexes, tri.numIndexes);

        } else {
//	bool *used = (bool *)_alloca16( tri.numVerts * sizeof( used[0] ) );
//	memset( used, 0, tri.numVerts * sizeof( used[0] ) );
//
//	// because the interaction may be a very small subset of the full surface,
//	// it makes sense to only deal with the verts used
//	for ( int j = 0; j < tri.numIndexes; j++ ) {
//		int i = tri.indexes[j];
//		if ( used[i] ) {
//			continue;
//		}
//		used[i] = true;
//
//		float ilength;
//
//		const idDrawVert *v = &tri.verts[i];
//
//		idVec3 lightDir = localLightOrigin - v.xyz;
//		idVec3 viewDir = localViewOrigin - v.xyz;
//
//		ilength = idMath::RSqrt( lightDir * lightDir );
//		lightDir[0] *= ilength;
//		lightDir[1] *= ilength;
//		lightDir[2] *= ilength;
//
//		ilength = idMath::RSqrt( viewDir * viewDir );
//		viewDir[0] *= ilength;
//		viewDir[1] *= ilength;
//		viewDir[2] *= ilength;
//
//		lightDir += viewDir;
//
//		texCoords[i][0] = lightDir * v.tangents[0];
//		texCoords[i][1] = lightDir * v.tangents[1];
//		texCoords[i][2] = lightDir * v.normal;
//		texCoords[i][3] = 1;
//	}
        }

        surf.dynamicTexCoords[0] = vertexCache.AllocFrameTemp(texCoords, size);
    }

//==================================================================================================================================================================================================
    /*
     =============
     R_SetEntityDefViewEntity

     If the entityDef isn't already on the viewEntity list, create
     a viewEntity and add it to the list with an empty scissor rect.

     This does not instantiate dynamic models for the entity yet.
     =============
     */static int DBG_R_SetEntityDefViewEntity=0;
    static viewEntity_s R_SetEntityDefViewEntity(idRenderEntityLocal def) {
        viewEntity_s vModel;

        if (def.viewCount == tr.viewCount) {
            return def.viewEntity;
        }
        DBG_R_SetEntityDefViewEntity++;
        def.viewCount = tr.viewCount;

        // set the model and modelview matricies
        vModel = new viewEntity_s();// R_ClearedFrameAlloc(sizeof(vModel));
//        TempDump.printCallStack("~~~~~~~~~~~~~~~~~" + vModel.DBG_COUNTER + "\\\\//" + tr.viewCount);
        vModel.entityDef = def;

        // the scissorRect will be expanded as the model bounds is accepted into visible portal chains
        vModel.scissorRect.Clear();
                

        // copy the model and weapon depth hack for back-end use
        vModel.modelDepthHack = def.parms.modelDepthHack;
        vModel.weaponDepthHack = def.parms.weaponDepthHack;

        R_AxisToModelMatrix(def.parms.axis, def.parms.origin, vModel.modelMatrix);

        // we may not have a viewDef if we are just creating shadows at entity creation time
        if (tr.viewDef != null) {
            myGlMultMatrix(vModel.modelMatrix, tr.viewDef.worldSpace.modelViewMatrix, vModel.modelViewMatrix);

            vModel.next = tr.viewDef.viewEntitys;
            tr.viewDef.viewEntitys = vModel;
            tr.viewDef.numViewEntitys++;
        }

        def.viewEntity = vModel;

        return vModel;
    }
    /*
     ====================
     R_TestPointInViewLight
     ====================
     */
    static final float INSIDE_LIGHT_FRUSTUM_SLOP = 32;
// this needs to be greater than the dist from origin to corner of near clip plane

    public static boolean R_TestPointInViewLight(final idVec3 org, final idRenderLightLocal light) {
        int i;
//	idVec3	local;

        for (i = 0; i < 6; i++) {
            float d = light.frustum[i].Distance(org);
            if (d > INSIDE_LIGHT_FRUSTUM_SLOP) {
                return false;
            }
        }

        return true;
    }

    /*
     ===================
     R_PointInFrustum

     Assumes positive sides face outward
     ===================
     */
    public static boolean R_PointInFrustum(idVec3 p, idPlane[] planes, int numPlanes) {
        for (int i = 0; i < numPlanes; i++) {
            float d = planes[i].Distance(p);
            if (d > 0) {
                return false;
            }
        }
        return true;
    }

    /*
     =============
     R_SetLightDefViewLight

     If the lightDef isn't already on the viewLight list, create
     a viewLight and add it to the list with an empty scissor rect.
     =============
     */
    public static viewLight_s R_SetLightDefViewLight(idRenderLightLocal light) {
        viewLight_s vLight;

        if (light.viewCount == tr.viewCount) {
            return light.viewLight;
        }
        light.viewCount = tr.viewCount;

        // add to the view light chain
        vLight = new viewLight_s();// R_ClearedFrameAlloc(sizeof(vLight));
        vLight.lightDef = light;

        // the scissorRect will be expanded as the light bounds is accepted into visible portal chains
        vLight.scissorRect = new idScreenRect();
        vLight.scissorRect.Clear();

        // calculate the shadow cap optimization states
        vLight.viewInsideLight = R_TestPointInViewLight(tr.viewDef.renderView.vieworg, light);
        if (!vLight.viewInsideLight) {
            vLight.viewSeesShadowPlaneBits = 0;
            for (int i = 0; i < light.numShadowFrustums; i++) {
                float d = light.shadowFrustums[i].planes[5].Distance(tr.viewDef.renderView.vieworg);
                if (d < INSIDE_LIGHT_FRUSTUM_SLOP) {
                    vLight.viewSeesShadowPlaneBits |= 1 << i;
                }
            }
        } else {
            // this should not be referenced in this case
            vLight.viewSeesShadowPlaneBits = 63;
        }

        // see if the light center is in view, which will allow us to cull invisible shadows
        vLight.viewSeesGlobalLightOrigin = R_PointInFrustum(light.globalLightOrigin, tr.viewDef.frustum, 4);

        // copy data used by backend
        vLight.globalLightOrigin = new idVec3(light.globalLightOrigin);
        vLight.lightProject[0] = new idPlane(light.lightProject[0]);
        vLight.lightProject[1] = new idPlane(light.lightProject[1]);
        vLight.lightProject[2] = new idPlane(light.lightProject[2]);
        vLight.lightProject[3] = new idPlane(light.lightProject[3]);
        vLight.fogPlane = new idPlane(light.frustum[5]);
        vLight.frustumTris = light.frustumTris;
        vLight.falloffImage = light.falloffImage;
        vLight.lightShader = light.lightShader;
        vLight.shaderRegisters = null;		// allocated and evaluated in R_AddLightSurfaces

        // link the view light
        vLight.next = tr.viewDef.viewLights;
        tr.viewDef.viewLights = vLight;

        light.viewLight = vLight;

        return vLight;
    }
    //=============================================================================================================================================================================================

    /*
     =================
     R_LinkLightSurf
     =================
     */
    public static void R_LinkLightSurf(final drawSurf_s[] link, final srfTriangles_s tri, final viewEntity_s spaceView,
            final idRenderLightLocal light, final idMaterial shader, final idScreenRect scissor, boolean viewInsideShadow) {
        drawSurf_s drawSurf;
        viewEntity_s space = spaceView;//TODO:should a back reference be set here?

        if (null == space) {
            space = tr.viewDef.worldSpace;
        }

        drawSurf = new drawSurf_s();//R_FrameAlloc(sizeof(drawSurf));

        drawSurf.geo = tri;
        drawSurf.space = space;
        drawSurf.material = shader;
        drawSurf.scissorRect = new idScreenRect(scissor);
        drawSurf.dsFlags = 0;
        if (viewInsideShadow) {
            drawSurf.dsFlags |= DSF_VIEW_INSIDE_SHADOW;
        }

        if (null == shader) {
            // shadows won't have a shader
            drawSurf.shaderRegisters = null;
        } else {
            // process the shader expressions for conditionals / color / texcoords
            final float[] constRegs = shader.ConstantRegisters();
            if (constRegs != null) {
                // this shader has only constants for parameters
                drawSurf.shaderRegisters = constRegs;
            } else {
                // FIXME: share with the ambient surface?
                float[] regs = new float[shader.GetNumRegisters()];//R_FrameAlloc(shader.GetNumRegisters());
                drawSurf.shaderRegisters = regs;
                shader.EvaluateRegisters(regs, space.entityDef.parms.shaderParms, tr.viewDef, space.entityDef.parms.referenceSound);
            }

            // calculate the specular coordinates if we aren't using vertex programs
            if (!tr.backEndRendererHasVertexPrograms && !r_skipSpecular.GetBool() && tr.backEndRenderer != BE_ARB) {
                R_SpecularTexGen(drawSurf, light.globalLightOrigin, tr.viewDef.renderView.vieworg);
                // if we failed to allocate space for the specular calculations, drop the surface
                if (NOT(drawSurf.dynamicTexCoords)) {
                    return;
                }
            }
        }

        // actually link it in
        drawSurf.nextOnLight = link[0];
        link[0] = drawSurf;
    }

    /*
     ======================
     R_ClippedLightScissorRectangle
     ======================
     */
    public static idScreenRect R_ClippedLightScissorRectangle(viewLight_s vLight) {
        int i, j;
        idRenderLightLocal light = vLight.lightDef;
        idScreenRect r = new idScreenRect();
        idFixedWinding w;

        r.Clear();

        for (i = 0; i < 6; i++) {
            final idWinding ow = light.frustumWindings[i];

            // projected lights may have one of the frustums degenerated
            if (null == ow) {
                continue;
            }

            // the light frustum planes face out from the light,
            // so the planes that have the view origin on the negative
            // side will be the "back" faces of the light, which must have
            // some fragment inside the portalStack to be visible
            if (light.frustum[i].Distance(tr.viewDef.renderView.vieworg) >= 0) {
                continue;
            }

            w = new idFixedWinding(ow);

            // now check the winding against each of the frustum planes
            for (j = 0; j < 5; j++) {
                if (!w.ClipInPlace(tr.viewDef.frustum[j].oNegative())) {
                    break;
                }
            }

            // project these points to the screen and add to bounds
            for (j = 0; j < w.GetNumPoints(); j++) {
                idPlane eye = new idPlane(), clip = new idPlane();
                idVec3 ndc = new idVec3();

                R_TransformModelToClip(w.oGet(j).ToVec3(), tr.viewDef.worldSpace.modelViewMatrix, tr.viewDef.projectionMatrix, eye, clip);

                if (clip.oGet(3) <= 0.01f) {
                    clip.oSet(3, 0.01f);;
                }

                R_TransformClipToDevice(clip, tr.viewDef, ndc);

                float windowX = 0.5f * (1.0f + ndc.oGet(0)) * (tr.viewDef.viewport.x2 - tr.viewDef.viewport.x1);
                float windowY = 0.5f * (1.0f + ndc.oGet(1)) * (tr.viewDef.viewport.y2 - tr.viewDef.viewport.y1);

                if (windowX > tr.viewDef.scissor.x2) {
                    windowX = tr.viewDef.scissor.x2;
                } else if (windowX < tr.viewDef.scissor.x1) {
                    windowX = tr.viewDef.scissor.x1;
                }
                if (windowY > tr.viewDef.scissor.y2) {
                    windowY = tr.viewDef.scissor.y2;
                } else if (windowY < tr.viewDef.scissor.y1) {
                    windowY = tr.viewDef.scissor.y1;
                }

                r.AddPoint(windowX, windowY);
            }
        }

        // add the fudge boundary
        r.Expand();

        return r;
    }

    /*
     ==================
     R_CalcLightScissorRectangle

     The light screen bounds will be used to crop the scissor rect during
     stencil clears and interaction drawing
     ==================
     */
    static int c_clippedLight, c_unclippedLight;

    public static idScreenRect R_CalcLightScissorRectangle(viewLight_s vLight) {
        idScreenRect r = new idScreenRect();
        srfTriangles_s tri;
        idPlane eye = new idPlane(), clip = new idPlane();
        idVec3 ndc = new idVec3();

        if (vLight.lightDef.parms.pointLight) {
            idBounds bounds = new idBounds();
            idRenderLightLocal lightDef = vLight.lightDef;
            tr.viewDef.viewFrustum.ProjectionBounds(new idBox(lightDef.parms.origin, lightDef.parms.lightRadius, lightDef.parms.axis), bounds);
            return R_ScreenRectFromViewFrustumBounds(bounds);
        }

        if (r_useClippedLightScissors.GetInteger() == 2) {
            return R_ClippedLightScissorRectangle(vLight);
        }

        r.Clear();

        tri = vLight.lightDef.frustumTris;
        for (int i = 0; i < tri.numVerts; i++) {
            R_TransformModelToClip(tri.verts[i].xyz, tr.viewDef.worldSpace.modelViewMatrix,
                    tr.viewDef.projectionMatrix, eye, clip);

            // if it is near clipped, clip the winding polygons to the view frustum
            if (clip.oGet(3) <= 1) {
                c_clippedLight++;
                if (r_useClippedLightScissors.GetInteger() != 0) {
                    return R_ClippedLightScissorRectangle(vLight);
                } else {
                    r.x1 = r.y1 = 0;
                    r.x2 = (tr.viewDef.viewport.x2 - tr.viewDef.viewport.x1) - 1;
                    r.y2 = (tr.viewDef.viewport.y2 - tr.viewDef.viewport.y1) - 1;
                    return r;
                }
            }

            R_TransformClipToDevice(clip, tr.viewDef, ndc);

            float windowX = 0.5f * (1.0f + ndc.oGet(0)) * (tr.viewDef.viewport.x2 - tr.viewDef.viewport.x1);
            float windowY = 0.5f * (1.0f + ndc.oGet(1)) * (tr.viewDef.viewport.y2 - tr.viewDef.viewport.y1);

            if (windowX > tr.viewDef.scissor.x2) {
                windowX = tr.viewDef.scissor.x2;
            } else if (windowX < tr.viewDef.scissor.x1) {
                windowX = tr.viewDef.scissor.x1;
            }
            if (windowY > tr.viewDef.scissor.y2) {
                windowY = tr.viewDef.scissor.y2;
            } else if (windowY < tr.viewDef.scissor.y1) {
                windowY = tr.viewDef.scissor.y1;
            }

            r.AddPoint(windowX, windowY);
        }

        // add the fudge boundary
        r.Expand();

        c_unclippedLight++;

        return r;
    }

    /*
     =================
     R_AddLightSurfaces

     Calc the light shader values, removing any light from the viewLight list
     if it is determined to not have any visible effect due to being flashed off or turned off.

     Adds entities to the viewEntity list if they are needed for shadow casting.

     Add any precomputed shadow volumes.

     Removes lights from the viewLights list if they are completely
     turned off, or completely off screen.

     Create any new interactions needed between the viewLights
     and the viewEntitys due to game movement
     =================
     */
    public static void R_AddLightSurfaces() throws idException {
        viewLight_s vLight;
        idRenderLightLocal light;
        viewLight_s ptr;
        int z = 0;

        // go through each visible light, possibly removing some from the list
        ptr = tr.viewDef.viewLights;
        while (ptr != null) {
            z++;
            vLight = ptr;
            light = vLight.lightDef;

            final idMaterial lightShader = light.lightShader;
            if (null == lightShader) {
                common.Error("R_AddLightSurfaces: NULL lightShader");
            }

            // see if we are suppressing the light in this view
            if (!r_skipSuppress.GetBool()) {
                if (light.parms.suppressLightInViewID != 0
                        && light.parms.suppressLightInViewID == tr.viewDef.renderView.viewID) {
                    ptr = vLight.next;
                    light.viewCount = -1;
                    continue;
                }
                if (light.parms.allowLightInViewID != 0
                        && light.parms.allowLightInViewID != tr.viewDef.renderView.viewID) {
                    ptr = vLight.next;
                    light.viewCount = -1;
                    continue;
                }
            }

            // evaluate the light shader registers
            float[] lightRegs = new float[lightShader.GetNumRegisters()];// R_FrameAlloc(lightShader.GetNumRegisters());
            vLight.shaderRegisters = lightRegs;
            lightShader.EvaluateRegisters(lightRegs, light.parms.shaderParms, tr.viewDef, light.parms.referenceSound);

            // if this is a purely additive light and no stage in the light shader evaluates
            // to a positive light value, we can completely skip the light
            if (!lightShader.IsFogLight() && !lightShader.IsBlendLight()) {
                int lightStageNum;
                for (lightStageNum = 0; lightStageNum < lightShader.GetNumStages(); lightStageNum++) {
                    final shaderStage_t lightStage = lightShader.GetStage(lightStageNum);

                    // ignore stages that fail the condition
                    if (0 == lightRegs[ lightStage.conditionRegister]) {
                        continue;
                    }

                    final int[] registers = lightStage.color.registers;

                    // snap tiny values to zero to avoid lights showing up with the wrong color
                    if (lightRegs[ registers[0]] < 0.001f) {
                        lightRegs[ registers[0]] = 0.0f;
                    }
                    if (lightRegs[ registers[1]] < 0.001f) {
                        lightRegs[ registers[1]] = 0.0f;
                    }
                    if (lightRegs[ registers[2]] < 0.001f) {
                        lightRegs[ registers[2]] = 0.0f;
                    }

                    // FIXME:	when using the following values the light shows up bright red when using nvidia drivers/hardware
                    //			this seems to have been fixed ?
                    //lightRegs[ registers[0] ] = 1.5143074e-005f;
                    //lightRegs[ registers[1] ] = 1.5483369e-005f;
                    //lightRegs[ registers[2] ] = 1.7014690e-005f;
                    if (lightRegs[ registers[0]] > 0.0f
                            || lightRegs[ registers[1]] > 0.0f
                            || lightRegs[ registers[2]] > 0.0f) {
                        break;
                    }
                }
                if (lightStageNum == lightShader.GetNumStages()) {
                    // we went through all the stages and didn't find one that adds anything
                    // remove the light from the viewLights list, and change its frame marker
                    // so interaction generation doesn't think the light is visible and
                    // create a shadow for it
                    ptr = vLight.next;
                    light.viewCount = -1;
                    continue;
                }
            }

            if (r_useLightScissors.GetBool()) {
                // calculate the screen area covered by the light frustum
                // which will be used to crop the stencil cull
                idScreenRect scissorRect = R_CalcLightScissorRectangle(vLight);
                // intersect with the portal crossing scissor rectangle
                vLight.scissorRect.Intersect(scissorRect);
//                System.out.println("LoveTheRide===="+vLight.scissorRect);

                if (r_showLightScissors.GetBool()) {
                    R_ShowColoredScreenRect(vLight.scissorRect, light.index);
                }
            }

//            if (false) {
//		// this never happens, because CullLightByPortals() does a more precise job
//		if ( vLight.scissorRect.IsEmpty() ) {
//			// this light doesn't touch anything on screen, so remove it from the list
//			ptr = vLight.next;
//			continue;
//		}
//            }
            // this one stays on the list
            ptr = vLight.next;

            // if we are doing a soft-shadow novelty test, regenerate the light with
            // a random offset every time
            if (r_lightSourceRadius.GetFloat() != 0.0f) {
                for (int i = 0; i < 3; i++) {
                    light.globalLightOrigin.oPluSet(i, r_lightSourceRadius.GetFloat() * (-1 + 2 * (((int) random()) & 0xfff) / (float) 0xfff));
                }
            }

            // create interactions with all entities the light may touch, and add viewEntities
            // that may cast shadows, even if they aren't directly visible.  Any real work
            // will be deferred until we walk through the viewEntities
            tr.viewDef.renderWorld.CreateLightDefInteractions(light);
            tr.pc.c_viewLights++;

            // fog lights will need to draw the light frustum triangles, so make sure they
            // are in the vertex cache
            if (lightShader.IsFogLight()) {
                if (NOT(light.frustumTris.ambientCache)) {
                    if (!R_CreateAmbientCache(light.frustumTris, false)) {
                        // skip if we are out of vertex memory
                        continue;
                    }
                }
                // touch the surface so it won't get purged
                vertexCache.Touch(light.frustumTris.ambientCache);
            }

            // add the prelight shadows for the static world geometry
            if (light.parms.prelightModel != null && r_useOptimizedShadows.GetBool()) {

                if (0 == light.parms.prelightModel.NumSurfaces()) {
                    common.Error("no surfs in prelight model '%s'", light.parms.prelightModel.Name());
                }

                Model.srfTriangles_s tri = light.parms.prelightModel.Surface(0).geometry;
                if (null == tri.shadowVertexes) {
                    common.Error("R_AddLightSurfaces: prelight model '%s' without shadowVertexes", light.parms.prelightModel.Name());
                }

                // these shadows will all have valid bounds, and can be culled normally
                if (r_useShadowCulling.GetBool()) {
                    if (R_CullLocalBox(tri.bounds, tr.viewDef.worldSpace.modelMatrix, 5, tr.viewDef.frustum)) {
                        continue;
                    }
                }

                // if we have been purged, re-upload the shadowVertexes
                if (NOT(tri.shadowCache)) {
                    R_CreatePrivateShadowCache(tri);
                    if (NOT(tri.shadowCache)) {
                        continue;
                    }
                }

                // touch the shadow surface so it won't get purged
                vertexCache.Touch(tri.shadowCache);

                if (NOT(tri.indexCache) && r_useIndexBuffers.GetBool()) {
                    vertexCache.Alloc(tri.indexes, tri.numIndexes, tri.indexCache, true);
                }
                if (tri.indexCache != null) {
                    vertexCache.Touch(tri.indexCache);
                }

                R_LinkLightSurf(vLight.globalShadows, tri, null, light, null, vLight.scissorRect, true /* FIXME? */);
            }
        }
    }
    //================================================================================================================================================================================================

    /*
     ==================
     R_IssueEntityDefCallback
     ==================
     */
    public static boolean R_IssueEntityDefCallback(idRenderEntityLocal def) {
        boolean update;
        idBounds oldBounds = null;

        if (r_checkBounds.GetBool()) {
            oldBounds = def.referenceBounds;
        }

        def.archived = false;		// will need to be written to the demo file
        tr.pc.c_entityDefCallbacks++;
        if (tr.viewDef != null) {
            update = def.parms.callback.run(def.parms, tr.viewDef.renderView);
        } else {
            update = def.parms.callback.run(def.parms, null);
        }

        if (null == def.parms.hModel) {
            common.Error("R_IssueEntityDefCallback: dynamic entity callback didn't set model");
        }

        if (r_checkBounds.GetBool()) {
            if (oldBounds.oGet(0, 0) > def.referenceBounds.oGet(0, 0) + CHECK_BOUNDS_EPSILON
                    || oldBounds.oGet(0, 1) > def.referenceBounds.oGet(0, 1) + CHECK_BOUNDS_EPSILON
                    || oldBounds.oGet(0, 2) > def.referenceBounds.oGet(0, 2) + CHECK_BOUNDS_EPSILON
                    || oldBounds.oGet(1, 0) < def.referenceBounds.oGet(1, 0) - CHECK_BOUNDS_EPSILON
                    || oldBounds.oGet(1, 1) < def.referenceBounds.oGet(1, 1) - CHECK_BOUNDS_EPSILON
                    || oldBounds.oGet(1, 2) < def.referenceBounds.oGet(1, 2) - CHECK_BOUNDS_EPSILON) {
                common.Printf("entity %d callback extended reference bounds\n", def.index);
            }
        }

        return update;
    }

    /*
     ===================
     R_EntityDefDynamicModel

     Issues a deferred entity callback if necessary.
     If the model isn't dynamic, it returns the original.
     Returns the cached dynamic model if present, otherwise creates
     it and any necessary overlays
     ===================
     */
    public static idRenderModel R_EntityDefDynamicModel(idRenderEntityLocal def) {
        boolean callbackUpdate;

        // allow deferred entities to construct themselves
        if (def.parms.callback != null) {
            callbackUpdate = R_IssueEntityDefCallback(def);
        } else {
            callbackUpdate = false;
        }

        idRenderModel model = def.parms.hModel;

        if (null == model) {
            common.Error("R_EntityDefDynamicModel: NULL model");
        }

        if (model.IsDynamicModel() == DM_STATIC) {
            def.dynamicModel = null;
            def.dynamicModelFrameCount = 0;
            return model;
        }

        // continously animating models (particle systems, etc) will have their snapshot updated every single view
        if (callbackUpdate || (model.IsDynamicModel() == DM_CONTINUOUS && def.dynamicModelFrameCount != tr.frameCount)) {
            R_ClearEntityDefDynamicModel(def);
        }

        // if we don't have a snapshot of the dynamic model, generate it now
        if (null == def.dynamicModel) {

            // instantiate the snapshot of the dynamic model, possibly reusing memory from the cached snapshot
            def.cachedDynamicModel = model.InstantiateDynamicModel(def.parms, tr.viewDef, def.cachedDynamicModel);

            if (def.cachedDynamicModel != null) {

                // add any overlays to the snapshot of the dynamic model
                if (def.overlay != null && !r_skipOverlays.GetBool()) {
                    def.overlay.AddOverlaySurfacesToModel(def.cachedDynamicModel);
                } else {
                    idRenderModelOverlay.RemoveOverlaySurfacesFromModel(def.cachedDynamicModel);
                }

                if (r_checkBounds.GetBool()) {
                    idBounds b = def.cachedDynamicModel.Bounds();
                    if (b.oGet(0, 0) < def.referenceBounds.oGet(0, 0) - CHECK_BOUNDS_EPSILON
                            || b.oGet(0, 1) < def.referenceBounds.oGet(0, 1) - CHECK_BOUNDS_EPSILON
                            || b.oGet(0, 2) < def.referenceBounds.oGet(0, 2) - CHECK_BOUNDS_EPSILON
                            || b.oGet(1, 0) > def.referenceBounds.oGet(1, 0) + CHECK_BOUNDS_EPSILON
                            || b.oGet(1, 1) > def.referenceBounds.oGet(1, 1) + CHECK_BOUNDS_EPSILON
                            || b.oGet(1, 2) > def.referenceBounds.oGet(1, 2) + CHECK_BOUNDS_EPSILON) {
                        common.Printf("entity %d dynamic model exceeded reference bounds\n", def.index);
                    }
                }
            }

            def.dynamicModel = def.cachedDynamicModel;
            def.dynamicModelFrameCount = tr.frameCount;
        }

        // set model depth hack value
        if (def.dynamicModel != null && model.DepthHack() != 0.0f && tr.viewDef != null) {
            idPlane eye = new idPlane(), clip = new idPlane();
            idVec3 ndc = new idVec3();
            R_TransformModelToClip(def.parms.origin, tr.viewDef.worldSpace.modelViewMatrix, tr.viewDef.projectionMatrix, eye, clip);
            R_TransformClipToDevice(clip, tr.viewDef, ndc);
            def.parms.modelDepthHack = model.DepthHack() * (1.0f - ndc.z);
        }

        // FIXME: if any of the surfaces have deforms, create a frame-temporary model with references to the
        // undeformed surfaces.  This would allow deforms to be light interacting.
        return def.dynamicModel;
    }
    /*
     =================
     R_AddDrawSurf
     =================
     */
    private static final float[] refRegs = new float[MAX_EXPRESSION_REGISTERS];	// don't put on stack, or VC++ will do a page touch
    static int DEBUG_drawZurf = 0;

    public static void R_AddDrawSurf(final srfTriangles_s tri, final viewEntity_s space, final renderEntity_s renderEntity,
            final idMaterial shader, final idScreenRect scissor) {
        DEBUG_drawZurf++;
//        TempDump.printCallStack("" + drawZurf);
        drawSurf_s drawSurf;
        final float[] shaderParms;
        final float[] generatedShaderParms = new float[MAX_ENTITY_SHADER_PARMS];

        drawSurf = new drawSurf_s();// R_FrameAlloc(sizeof(drawSurf));
        drawSurf.geo = tri;
        drawSurf.space = space;
        drawSurf.material = shader;
        drawSurf.scissorRect = new idScreenRect(scissor);
        drawSurf.sort = shader.GetSort() + tr.sortOffset;
        drawSurf.dsFlags = 0;

        // bumping this offset each time causes surfaces with equal sort orders to still
        // deterministically draw in the order they are added
        tr.sortOffset += 0.000001f;

        // if it doesn't fit, resize the list
        if (tr.viewDef.numDrawSurfs == tr.viewDef.maxDrawSurfs) {
            drawSurf_s[] old = tr.viewDef.drawSurfs;
            int count;

            if (tr.viewDef.maxDrawSurfs == 0) {
                tr.viewDef.maxDrawSurfs = INITIAL_DRAWSURFS;
                count = 0;
            } else {
                count = tr.viewDef.maxDrawSurfs /*sizeof(tr.viewDef.drawSurfs[0])*/;
                tr.viewDef.maxDrawSurfs *= 2;
            }
            tr.viewDef.drawSurfs = new drawSurf_s[tr.viewDef.maxDrawSurfs];// R_FrameAlloc(tr.viewDef.maxDrawSurfs);
//		memcpy( tr.viewDef.drawSurfs, old, count );
            if (old != null) {
                System.arraycopy(old, 0, tr.viewDef.drawSurfs, 0, count);
            }
        }
        tr.viewDef.drawSurfs[tr.viewDef.numDrawSurfs++] = drawSurf;

        // process the shader expressions for conditionals / color / texcoords
        final float[] constRegs = shader.ConstantRegisters();
        if (constRegs != null) {
            // shader only uses constant values
            drawSurf.shaderRegisters = constRegs;
        } else {
            float[] regs = new float[shader.GetNumRegisters()];// R_FrameAlloc(shader.GetNumRegisters());
            drawSurf.shaderRegisters = regs;

            // a reference shader will take the calculated stage color value from another shader
            // and use that for the parm0-parm3 of the current shader, which allows a stage of
            // a light model and light flares to pick up different flashing tables from
            // different light shaders
            if (renderEntity.referenceShader != null) {
                // evaluate the reference shader to find our shader parms
                final shaderStage_t pStage;

                renderEntity.referenceShader.EvaluateRegisters(refRegs, renderEntity.shaderParms, tr.viewDef, renderEntity.referenceSound);
                pStage = renderEntity.referenceShader.GetStage(0);

//			memcpy( generatedShaderParms, renderEntity.shaderParms, sizeof( generatedShaderParms ) );
                System.arraycopy(renderEntity.shaderParms, 0, generatedShaderParms, 0, renderEntity.shaderParms.length);
                generatedShaderParms[0] = refRegs[pStage.color.registers[0]];
                generatedShaderParms[1] = refRegs[pStage.color.registers[1]];
                generatedShaderParms[2] = refRegs[pStage.color.registers[2]];

                shaderParms = generatedShaderParms;
            } else {
                // evaluate with the entityDef's shader parms
                shaderParms = renderEntity.shaderParms;
            }

            float oldFloatTime = 0;
            int oldTime = 0;

            if (space.entityDef != null && space.entityDef.parms.timeGroup != 0) {
                oldFloatTime = tr.viewDef.floatTime;
                oldTime = tr.viewDef.renderView.time;

                tr.viewDef.floatTime = game.GetTimeGroupTime(space.entityDef.parms.timeGroup) * 0.001f;
                tr.viewDef.renderView.time = game.GetTimeGroupTime(space.entityDef.parms.timeGroup);
            }

            shader.EvaluateRegisters(regs, shaderParms, tr.viewDef, renderEntity.referenceSound);

            if (space.entityDef != null && space.entityDef.parms.timeGroup != 0) {
                tr.viewDef.floatTime = oldFloatTime;
                tr.viewDef.renderView.time = oldTime;
            }
        }

        // check for deformations
        R_DeformDrawSurf(drawSurf);

        // skybox surfaces need a dynamic texgen
        switch (shader.Texgen()) {
            case TG_SKYBOX_CUBE:
                R_SkyboxTexGen(drawSurf, tr.viewDef.renderView.vieworg);
                break;
            case TG_WOBBLESKY_CUBE:
                R_WobbleskyTexGen(drawSurf, tr.viewDef.renderView.vieworg);
                break;
        }

        // check for gui surfaces
        idUserInterface gui = null;

        if (null == space.entityDef) {
            gui = shader.GlobalGui();
        } else {
            int guiNum = shader.GetEntityGui() - 1;
            if (guiNum >= 0 && guiNum < MAX_RENDERENTITY_GUI) {
                gui = renderEntity.gui[ guiNum];
            }
            if (gui == null) {
                gui = shader.GlobalGui();
            }
        }

        if (gui != null) {
            // force guis on the fast time
            float oldFloatTime;
            int oldTime;

            oldFloatTime = tr.viewDef.floatTime;
            oldTime = tr.viewDef.renderView.time;

            tr.viewDef.floatTime = game.GetTimeGroupTime(1) * 0.001f;
            tr.viewDef.renderView.time = game.GetTimeGroupTime(1);

            idBounds ndcBounds = new idBounds();

            if (!R_PreciseCullSurface(drawSurf, ndcBounds)) {
                // did we ever use this to forward an entity color to a gui that didn't set color?
//			memcpy( tr.guiShaderParms, shaderParms, sizeof( tr.guiShaderParms ) );
                R_RenderGuiSurf(gui, drawSurf);
            }

            tr.viewDef.floatTime = oldFloatTime;
            tr.viewDef.renderView.time = oldTime;
        }

        // we can't add subviews at this point, because that would
        // increment tr.viewCount, messing up the rest of the surface
        // adds for this view
    }

    /*
     ===============
     R_AddAmbientDrawsurfs

     Adds surfaces for the given viewEntity
     Walks through the viewEntitys list and creates drawSurf_t for each surface of
     each viewEntity that has a non-empty scissorRect
     ===============
     */
    public static void R_AddAmbientDrawsurfs(viewEntity_s vEntity) {
        int i, total;
        idRenderEntityLocal def;
        srfTriangles_s tri;
        idRenderModel model;
        idMaterial[] shader = {null};

        def = vEntity.entityDef;

        if (def.dynamicModel != null) {
            model = def.dynamicModel;
        } else {
            model = def.parms.hModel;
        }

        // add all the surfaces
        total = model.NumSurfaces();
        for (i = 0; i < total; i++) {
            final modelSurface_s surf = model.Surface(i);

            // for debugging, only show a single surface at a time
            if (r_singleSurface.GetInteger() >= 0 && i != r_singleSurface.GetInteger()) {
                continue;
            }

            tri = surf.geometry;
            if (null == tri) {
                continue;
            }
            if (0 == tri.numIndexes) {
                continue;
            }
            shader[0] = surf.shader = R_RemapShaderBySkin(surf.shader, def.parms.customSkin, def.parms.customShader);

            R_GlobalShaderOverride(shader);

            if (null == shader[0]) {
                continue;
            }
            if (!shader[0].IsDrawn()) {
                continue;
            }

            // debugging tool to make sure we are have the correct pre-calculated bounds
            if (r_checkBounds.GetBool()) {
                int j, k;
                for (j = 0; j < tri.numVerts; j++) {
                    for (k = 0; k < 3; k++) {
                        if (tri.verts[j].xyz.oGet(k) > tri.bounds.oGet(1, k) + CHECK_BOUNDS_EPSILON
                                || tri.verts[j].xyz.oGet(k) < tri.bounds.oGet(0, k) - CHECK_BOUNDS_EPSILON) {
                            common.Printf("bad tri.bounds on %s:%s\n", def.parms.hModel.Name(), shader[0].GetName());
                            break;
                        }
                        if (tri.verts[j].xyz.oGet(k) > def.referenceBounds.oGet(1, k) + CHECK_BOUNDS_EPSILON
                                || tri.verts[j].xyz.oGet(k) < def.referenceBounds.oGet(0, k) - CHECK_BOUNDS_EPSILON) {
                            common.Printf("bad referenceBounds on %s:%s\n", def.parms.hModel.Name(), shader[0].GetName());
                            break;
                        }
                    }
                    if (k != 3) {
                        break;
                    }
                }
            }

            if (!R_CullLocalBox(tri.bounds, vEntity.modelMatrix, 5, tr.viewDef.frustum)) {

                def.visibleCount = tr.viewCount;

                // make sure we have an ambient cache
                if (!R_CreateAmbientCache(tri, shader[0].ReceivesLighting())) {
                    // don't add anything if the vertex cache was too full to give us an ambient cache
                    return;
                }
                // touch it so it won't get purged
                vertexCache.Touch(tri.ambientCache);

                if (r_useIndexBuffers.GetBool() && NOT(tri.indexCache)) {
                    vertexCache.Alloc(tri.indexes, tri.numIndexes /* sizeof( tri.indexes[0] */, tri.indexCache, true);
                }
                if (tri.indexCache != null) {
                    vertexCache.Touch(tri.indexCache);
                }

                // add the surface for drawing
                R_AddDrawSurf(tri, vEntity, vEntity.entityDef.parms, shader[0], vEntity.scissorRect);

                // ambientViewCount is used to allow light interactions to be rejected
                // if the ambient surface isn't visible at all
                tri.ambientViewCount = tr.viewCount;
            }
        }

        // add the lightweight decal surfaces
        for (idRenderModelDecal decal = def.decals; decal != null; decal = decal.Next()) {
            decal.AddDecalDrawSurf(vEntity);
        }
    }

    /*
     ==================
     R_CalcEntityScissorRectangle
     ==================
     */
    public static idScreenRect R_CalcEntityScissorRectangle(viewEntity_s vEntity) {
        idBounds bounds = new idBounds();
        idRenderEntityLocal def = vEntity.entityDef;

        tr.viewDef.viewFrustum.ProjectionBounds(new idBox(def.referenceBounds, def.parms.origin, def.parms.axis), bounds);

        return R_ScreenRectFromViewFrustumBounds(bounds);
    }

    /*
     ===================
     R_ListRenderLightDefs_f
     ===================
     */
    public static void R_ListRenderLightDefs_f(final idCmdArgs args) {
        int i;
        idRenderLightLocal ldef;

        if (null == tr.primaryWorld) {
            return;
        }
        int active = 0;
        int totalRef = 0;
        int totalIntr = 0;

        for (i = 0; i < tr.primaryWorld.lightDefs.Num(); i++) {
            ldef = tr.primaryWorld.lightDefs.oGet(i);
            if (null == ldef) {
                common.Printf("%4d: FREED\n", i);
                continue;
            }

            // count up the interactions
            int iCount = 0;
            for (idInteraction inter = ldef.firstInteraction; inter != null; inter = inter.lightNext) {
                iCount++;
            }
            totalIntr += iCount;

            // count up the references
            int rCount = 0;
            for (areaReference_s ref = ldef.references; ref != null; ref = ref.ownerNext) {
                rCount++;
            }
            totalRef += rCount;

            common.Printf("%4d: %3d intr %2d refs %s\n", i, iCount, rCount, ldef.lightShader.GetName());
            active++;
        }

        common.Printf("%d lightDefs, %d interactions, %d areaRefs\n", active, totalIntr, totalRef);
    }

    /*
     ===================
     R_ListRenderEntityDefs_f
     ===================
     */
    public static void R_ListRenderEntityDefs_f(final idCmdArgs args) {
        int i;
        idRenderEntityLocal mdef;

        if (null == tr.primaryWorld) {
            return;
        }
        int active = 0;
        int totalRef = 0;
        int totalIntr = 0;

        for (i = 0; i < tr.primaryWorld.entityDefs.Num(); i++) {
            mdef = tr.primaryWorld.entityDefs.oGet(i);
            if (null == mdef) {
                common.Printf("%4d: FREED\n", i);
                continue;
            }

            // count up the interactions
            int iCount = 0;
            for (idInteraction inter = mdef.firstInteraction; inter != null; inter = inter.entityNext) {
                iCount++;
            }
            totalIntr += iCount;

            // count up the references
            int rCount = 0;
            for (areaReference_s ref = mdef.entityRefs; ref != null; ref = ref.ownerNext) {
                rCount++;
            }
            totalRef += rCount;

            common.Printf("%4d: %3d intr %2d refs %s\n", i, iCount, rCount, mdef.parms.hModel.Name());
            active++;
        }

        common.Printf("total active: %d\n", active);
    }

    /*
     ===================
     R_AddModelSurfaces

     Here is where dynamic models actually get instantiated, and necessary
     interactions get created.  This is all done on a sort-by-model basis
     to keep source data in cache (most likely L2) as any interactions and
     shadows are generated, since dynamic models will typically be lit by
     two or more lights.
     ===================
     */
    public static void R_AddModelSurfaces() {
        viewEntity_s vEntity;
        idInteraction inter, next;
        idRenderModel model;
        int i = 0;

        // clear the ambient surface list
        tr.viewDef.numDrawSurfs = 0;
        tr.viewDef.maxDrawSurfs = 0;	// will be set to INITIAL_DRAWSURFS on R_AddDrawSurf

        // go through each entity that is either visible to the view, or to
        // any light that intersects the view (for shadows)
        for (vEntity = tr.viewDef.viewEntitys; vEntity != null; vEntity = vEntity.next, i++) {

            if (r_useEntityScissors.GetBool()) {
                // calculate the screen area covered by the entity
                idScreenRect scissorRect = R_CalcEntityScissorRectangle(vEntity);
                // intersect with the portal crossing scissor rectangle
                vEntity.scissorRect.Intersect(scissorRect);

                if (r_showEntityScissors.GetBool()) {
                    R_ShowColoredScreenRect(vEntity.scissorRect, vEntity.entityDef.index);
                }
            }

            float oldFloatTime = 0;
            int oldTime = 0;

            game.SelectTimeGroup(vEntity.entityDef.parms.timeGroup);

            if (vEntity.entityDef.parms.timeGroup != 0) {
                oldFloatTime = tr.viewDef.floatTime;
                oldTime = tr.viewDef.renderView.time;

                tr.viewDef.floatTime = game.GetTimeGroupTime(vEntity.entityDef.parms.timeGroup) * 0.001f;
                tr.viewDef.renderView.time = game.GetTimeGroupTime(vEntity.entityDef.parms.timeGroup);
            }

            if (tr.viewDef.isXraySubview && vEntity.entityDef.parms.xrayIndex == 1) {
                if (vEntity.entityDef.parms.timeGroup != 0) {
                    tr.viewDef.floatTime = oldFloatTime;
                    tr.viewDef.renderView.time = oldTime;
                }
                continue;
            } else if (!tr.viewDef.isXraySubview && vEntity.entityDef.parms.xrayIndex == 2) {
                if (vEntity.entityDef.parms.timeGroup != 0) {
                    tr.viewDef.floatTime = oldFloatTime;
                    tr.viewDef.renderView.time = oldTime;
                }
                continue;
            }

            // add the ambient surface if it has a visible rectangle
            if (!vEntity.scissorRect.IsEmpty()) {
                model = R_EntityDefDynamicModel(vEntity.entityDef);
                if (model == null || model.NumSurfaces() <= 0) {
                    if (vEntity.entityDef.parms.timeGroup != 0) {
                        tr.viewDef.floatTime = oldFloatTime;
                        tr.viewDef.renderView.time = oldTime;
                    }
                    continue;
                }

                R_AddAmbientDrawsurfs(vEntity);
                tr.pc.c_visibleViewEntities++;
            } else {
                tr.pc.c_shadowViewEntities++;//what happens after the scissorsView is set??
            }

            //
            // for all the entity / light interactions on this entity, add them to the view
            //
            if (tr.viewDef.isXraySubview) {
                if (vEntity.entityDef.parms.xrayIndex == 2) {
                    for (inter = vEntity.entityDef.firstInteraction; inter != null && !inter.IsEmpty(); inter = next) {
                        next = inter.entityNext;
                        if (inter.lightDef.viewCount != tr.viewCount) {
                            continue;
                        }
                        inter.AddActiveInteraction();
                    }
                }
            } else {
                // all empty interactions are at the end of the list so once the
                // first is encountered all the remaining interactions are empty
                for (inter = vEntity.entityDef.firstInteraction; inter != null && !inter.IsEmpty(); inter = next) {
                    next = inter.entityNext;

                    // skip any lights that aren't currently visible
                    // this is run after any lights that are turned off have already
                    // been removed from the viewLights list, and had their viewCount cleared
                    if (inter.lightDef.viewCount != tr.viewCount) {
                        continue;
                    }
                    inter.AddActiveInteraction();
                }
            }

            if (vEntity.entityDef.parms.timeGroup != 0) {
                tr.viewDef.floatTime = oldFloatTime;
                tr.viewDef.renderView.time = oldTime;
            }
        }
    }

    /*
     =====================
     R_RemoveUnecessaryViewLights
     =====================
     */
    public static void R_RemoveUnecessaryViewLights() {
        viewLight_s vLight;

        // go through each visible light
        for (vLight = tr.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            // if the light didn't have any lit surfaces visible, there is no need to
            // draw any of the shadows.  We still keep the vLight for debugging
            // draws
            if (NOT(vLight.localInteractions[0]) && NOT(vLight.globalInteractions[0]) && NOT(vLight.translucentInteractions[0])) {
                vLight.localShadows[0] = null;
                vLight.globalShadows[0] = null;
            }
        }

        if (r_useShadowSurfaceScissor.GetBool()) {
            // shrink the light scissor rect to only intersect the surfaces that will actually be drawn.
            // This doesn't seem to actually help, perhaps because the surface scissor
            // rects aren't actually the surface, but only the portal clippings.
            for (vLight = tr.viewDef.viewLights; vLight != null; vLight = vLight.next) {
                drawSurf_s surf;
                idScreenRect surfRect = new idScreenRect();

                if (!vLight.lightShader.LightCastsShadows()) {
                    continue;
                }

                surfRect.Clear();

                for (surf = vLight.globalInteractions[0]; surf != null; surf = surf.nextOnLight) {
                    surfRect.Union(surf.scissorRect);
                }
                for (surf = vLight.localShadows[0]; surf != null; surf = surf.nextOnLight) {
                    surf.scissorRect.Intersect(surfRect);
                }

                for (surf = vLight.localInteractions[0]; surf != null; surf = surf.nextOnLight) {
                    surfRect.Union(surf.scissorRect);
                }
                for (surf = vLight.globalShadows[0]; surf != null; surf = surf.nextOnLight) {
                    surf.scissorRect.Intersect(surfRect);
                }

                for (surf = vLight.translucentInteractions[0]; surf != null; surf = surf.nextOnLight) {
                    surfRect.Union(surf.scissorRect);
                }

                vLight.scissorRect.Intersect(surfRect);
            }
        }
    }
}
