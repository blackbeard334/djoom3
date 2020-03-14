package neo.Renderer;

import static neo.Renderer.Material.MAX_ENTITY_SHADER_PARMS;
import static neo.Renderer.Material.SURF_COLLISION;
import static neo.Renderer.Material.materialCoverage_t.MC_OPAQUE;
import static neo.Renderer.Material.materialCoverage_t.MC_PERFORATED;
import static neo.Renderer.Model.dynamicModel_t.DM_CACHED;
import static neo.Renderer.Model.dynamicModel_t.DM_STATIC;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderSystem.R_LockSurfaceScene;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.Renderer.RenderSystem_init.r_lockSurfaces;
import static neo.Renderer.RenderSystem_init.r_showDemo;
import static neo.Renderer.RenderSystem_init.r_singleArea;
import static neo.Renderer.RenderSystem_init.r_singleEntity;
import static neo.Renderer.RenderSystem_init.r_singleLight;
import static neo.Renderer.RenderSystem_init.r_skipFrontEnd;
import static neo.Renderer.RenderSystem_init.r_skipSuppress;
import static neo.Renderer.RenderSystem_init.r_skipUpdates;
import static neo.Renderer.RenderSystem_init.r_useEntityCallbacks;
import static neo.Renderer.RenderSystem_init.r_useEntityCulling;
import static neo.Renderer.RenderSystem_init.r_useInteractionTable;
import static neo.Renderer.RenderSystem_init.r_useLightCulling;
import static neo.Renderer.RenderSystem_init.r_useNodeCommonChildren;
import static neo.Renderer.RenderSystem_init.r_usePortals;
import static neo.Renderer.RenderWorld.MAX_GLOBAL_SHADER_PARMS;
import static neo.Renderer.RenderWorld.MAX_RENDERENTITY_GUI;
import static neo.Renderer.RenderWorld.NUM_PORTAL_ATTRIBUTES;
import static neo.Renderer.RenderWorld.PROC_FILE_EXT;
import static neo.Renderer.RenderWorld.PROC_FILE_ID;
import static neo.Renderer.RenderWorld.R_RemapShaderBySkin;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_NONE;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_VIEW;
import static neo.Renderer.RenderWorld_portals.MAX_PORTAL_PLANES;
import static neo.Renderer.qgl.qglBegin;
import static neo.Renderer.qgl.qglColor3f;
import static neo.Renderer.qgl.qglEnd;
import static neo.Renderer.qgl.qglVertex3fv;
import static neo.Renderer.tr_guisurf.R_SurfaceToTextureAxis;
import static neo.Renderer.tr_light.R_EntityDefDynamicModel;
import static neo.Renderer.tr_light.R_IssueEntityDefCallback;
import static neo.Renderer.tr_light.R_SetEntityDefViewEntity;
import static neo.Renderer.tr_light.R_SetLightDefViewLight;
import static neo.Renderer.tr_lightrun.R_ClearEntityDefDynamicModel;
import static neo.Renderer.tr_lightrun.R_CreateEntityRefs;
import static neo.Renderer.tr_lightrun.R_CreateLightDefFogPortals;
import static neo.Renderer.tr_lightrun.R_CreateLightRefs;
import static neo.Renderer.tr_lightrun.R_DeriveLightData;
import static neo.Renderer.tr_lightrun.R_FreeEntityDefDecals;
import static neo.Renderer.tr_lightrun.R_FreeEntityDefDerivedData;
import static neo.Renderer.tr_lightrun.R_FreeEntityDefFadedDecals;
import static neo.Renderer.tr_lightrun.R_FreeEntityDefOverlay;
import static neo.Renderer.tr_lightrun.R_FreeLightDefDerivedData;
import static neo.Renderer.tr_local.DEFAULT_FOG_DISTANCE;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_local.demoCommand_t.DC_DELETE_ENTITYDEF;
import static neo.Renderer.tr_local.demoCommand_t.DC_DELETE_LIGHTDEF;
import static neo.Renderer.tr_local.demoCommand_t.DC_LOADMAP;
import static neo.Renderer.tr_local.demoCommand_t.DC_RENDERVIEW;
import static neo.Renderer.tr_local.demoCommand_t.DC_SET_PORTAL_STATE;
import static neo.Renderer.tr_local.demoCommand_t.DC_UPDATE_ENTITYDEF;
import static neo.Renderer.tr_local.demoCommand_t.DC_UPDATE_LIGHTDEF;
import static neo.Renderer.tr_main.R_AxisToModelMatrix;
import static neo.Renderer.tr_main.R_CullLocalBox;
import static neo.Renderer.tr_main.R_GlobalPointToLocal;
import static neo.Renderer.tr_main.R_GlobalToNormalizedDeviceCoordinates;
import static neo.Renderer.tr_main.R_LocalPointToGlobal;
import static neo.Renderer.tr_main.R_RenderView;
import static neo.Renderer.tr_rendertools.RB_AddDebugLine;
import static neo.Renderer.tr_rendertools.RB_AddDebugPolygon;
import static neo.Renderer.tr_rendertools.RB_AddDebugText;
import static neo.Renderer.tr_rendertools.RB_ClearDebugLines;
import static neo.Renderer.tr_rendertools.RB_ClearDebugPolygons;
import static neo.Renderer.tr_rendertools.RB_ClearDebugText;
import static neo.Renderer.tr_rendertools.RB_DrawTextLength;
import static neo.Renderer.tr_trace.R_LocalTrace;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfShadowVerts;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.TempDump.NOT;
import static neo.TempDump.ctos;
import static neo.TempDump.indexOf;
import static neo.TempDump.sizeof;
import static neo.framework.BuildDefines.ID_DEDICATED;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DemoFile.demoSystem_t.DS_RENDER;
import static neo.framework.EventLoop.eventLoop;
import static neo.framework.FileSystem_h.FILE_NOT_FOUND_TIMESTAMP;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Session.session;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Text.Lexer.LEXFL_NODOLLARPRECOMPILE;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_CROSS;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_BACK;
import static neo.sys.win_shared.Sys_Milliseconds;
import static neo.ui.UserInterface.uiManager;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import neo.TempDump;
import neo.TempDump.Atomics;
import neo.TempDump.Atomics.renderEntityShadow;
import neo.TempDump.Atomics.renderLightShadow;
import neo.TempDump.Atomics.renderViewShadow;
import neo.Renderer.Interaction.areaNumRef_s;
import neo.Renderer.Interaction.idInteraction;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.shaderStage_t;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.shadowCache_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.ModelDecal.decalProjectionInfo_s;
import neo.Renderer.ModelDecal.idRenderModelDecal;
import neo.Renderer.ModelOverlay.idRenderModelOverlay;
import neo.Renderer.RenderWorld.exitPortal_t;
import neo.Renderer.RenderWorld.guiPoint_t;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Renderer.RenderWorld.modelTrace_s;
import neo.Renderer.RenderWorld.portalConnection_t;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.RenderWorld.renderLight_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Renderer.RenderWorld_demo.demoHeader_t;
import neo.Renderer.RenderWorld_portals.portalStack_s;
import neo.Renderer.tr_lightrun.R_RegenerateWorld_f;
import neo.Renderer.tr_local.areaReference_s;
import neo.Renderer.tr_local.demoCommand_t;
import neo.Renderer.tr_local.idRenderEntityLocal;
import neo.Renderer.tr_local.idRenderLightLocal;
import neo.Renderer.tr_local.idScreenRect;
import neo.Renderer.tr_local.localTrace_t;
import neo.Renderer.tr_local.viewDef_s;
import neo.Renderer.tr_local.viewEntity_s;
import neo.Renderer.tr_local.viewLight_s;
import neo.framework.DemoFile.idDemoFile;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Box.idBox;
import neo.idlib.BV.Frustum.idFrustum;
import neo.idlib.BV.Sphere.idSphere;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class RenderWorld_local {

    // assume any lightDef or entityDef index above this is an internal error
    static final int LUDICROUS_INDEX = 10000;
    //
    static final boolean WRITE_GUIS = false;

    public static class portal_s {

        int            intoArea;     // area this portal leads to
        idWinding      w;            // winding points have counter clockwise ordering seen this area
        idPlane        plane;        // view must be on the positive side of the plane to cross
        portal_s       next;         // next portal of the area
        doublePortal_s doublePortal;
        
        public portal_s(){
            intoArea = 0;
            w = new idWinding();
            plane = new idPlane();
        }
    };

    public static class doublePortal_s {

        portal_s[] portals = new portal_s[2];
        int blockingBits;               // PS_BLOCK_VIEW, PS_BLOCK_AIR, etc, set by doors that shut them off
//
        // A portal will be considered closed if it is past the
        // fog-out point in a fog volume.  We only support a single
        // fog volume over each portal.
        idRenderLightLocal fogLight;
        doublePortal_s nextFoggedPortal;
    };

    public static class portalArea_s {
        int             areaNum;
        int[]           connectedAreaNum;   // if two areas have matching connectedAreaNum, they are
                                            // not separated by a portal with the apropriate PS_BLOCK_* blockingBits
        int             viewCount;          // set by R_FindViewLightsAndEntities
        portal_s        portals;            // never changes after load
        areaReference_s entityRefs;         // head/tail of doubly linked list, may change
        areaReference_s lightRefs;          // head/tail of doubly linked list, may change

        public portalArea_s() {
            this.connectedAreaNum = new int[NUM_PORTAL_ATTRIBUTES];
            this.entityRefs = new areaReference_s();
            this.lightRefs = new areaReference_s();
        }

        public static portalArea_s[] generateArray(final int length) {
            return Stream.
                    generate(portalArea_s::new).
                    limit(length).
                    toArray(portalArea_s[]::new);
        }
    };
    static final int CHILDREN_HAVE_MULTIPLE_AREAS = -2;
    static final int AREANUM_SOLID                = -1;

    public static class areaNode_t {

        idPlane plane = new idPlane();
        int[] children = new int[2];	// negative numbers are (-1 - areaNumber), 0 = solid
        int commonChildrenArea;         // if all children are either solid or a single area,
        //                              // this is the area number, else CHILDREN_HAVE_MULTIPLE_AREAS
    };

    public static class idRenderWorldLocal extends idRenderWorld {

        public idStr                mapName;         // ie: maps/tim_dm2.proc, written to demoFile
        public long[] /*ID_TIME_T*/ mapTimeStamp;    // for fast reloads of the same level
        //
        public areaNode_t[]         areaNodes;
        public int                  numAreaNodes;
        //
        public portalArea_s[]       portalAreas;
        public int                  numPortalAreas;
        public int                  connectedAreaNum;// incremented every time a door portal state changes
        //
        public idScreenRect[]       areaScreenRect;
        //
        public doublePortal_s[]     doublePortals;
        public int                  numInterAreaPortals;
        //
        public idList<idRenderModel> localModels = new idList<>();
        //
        public idList<idRenderEntityLocal> entityDefs = new idList<>();
        public idList<idRenderLightLocal> lightDefs = new idList<>();
        //
//        public final idBlockAlloc<areaReference_s> areaReferenceAllocator = new idBlockAlloc<>(1024);
//        public final idBlockAlloc<idInteraction> interactionAllocator = new idBlockAlloc<>(256);
//        public idBlockAlloc<areaNumRef_s> areaNumRefAllocator = new idBlockAlloc<>(1024);
        // all light / entity interactions are referenced here for fast lookup without
        // having to crawl the doubly linked lists.  EnntityDefs are sequential for better
        // cache access, because the table is accessed by light in idRenderWorldLocal::CreateLightDefInteractions()
        // Growing this table is time consuming, so we add a pad value to the number
        // of entityDefs and lightDefs
        public idInteraction[] interactionTable;
        public int interactionTableWidth;		// entityDefs
        public int interactionTableHeight;		// lightDefs
        //
        //
        public boolean generateAllInteractionsCalled;
        //
        //

        public idRenderWorldLocal() {
            mapName = new idStr();//.Clear();
            mapTimeStamp = new long[]{FILE_NOT_FOUND_TIMESTAMP};

            generateAllInteractionsCalled = false;

            areaNodes = null;
            numAreaNodes = 0;

            portalAreas = null;
            numPortalAreas = 0;

            doublePortals = null;
            numInterAreaPortals = 0;

            interactionTable = null;
            interactionTableWidth = 0;
            interactionTableHeight = 0;
        }
        // virtual					~idRenderWorldLocal();

        @Override
        public int AddEntityDef(renderEntity_s re) {
            // try and reuse a free spot
            int entityHandle = entityDefs.FindNull();
            if (entityHandle == -1) {
                entityHandle = entityDefs.Append((idRenderEntityLocal) null);
                if (interactionTable != null && entityDefs.Num() > interactionTableWidth) {
                    ResizeInteractionTable();
                }
            }

            UpdateEntityDef(entityHandle, re);

            return entityHandle;
        }

        /*
         ==============
         UpdateEntityDef

         Does not write to the demo file, which will only be updated for
         visible entities
         ==============
         */
        int c_callbackUpdate;

        @Override
        public void UpdateEntityDef(int entityHandle, renderEntity_s re) {
            if (r_skipUpdates.GetBool()) {
                return;
            }

            tr.pc.c_entityUpdates++;

            if (NOT(re.hModel) && NOT(re.callback)) {
                common.Error("idRenderWorld::UpdateEntityDef: NULL hModel");
            }

            // create new slots if needed
            if (entityHandle < 0 || entityHandle > LUDICROUS_INDEX) {
                common.Error("idRenderWorld::UpdateEntityDef: index = %d", entityHandle);
            }
            while (entityHandle >= entityDefs.Num()) {
                entityDefs.Append((idRenderEntityLocal) null);
            }

            idRenderEntityLocal def = entityDefs.oGet(entityHandle);
            if (def != null) {

                if (0 == re.forceUpdate) {

                    // check for exact match (OPTIMIZE: check through pointers more)
                    if (NOT(re.joints) && NOT(re.callbackData) && NOT(def.dynamicModel) && re.equals(def.parms)) {
                        return;
                    }

                    // if the only thing that changed was shaderparms, we can just leave things as they are
                    // after updating parms
                    // if we have a callback function and the bounds, origin, axis and model match,
                    // then we can leave the references as they are
                    if (re.callback != null) {

                        boolean axisMatch = re.axis.equals(def.parms.axis);
                        boolean originMatch = re.origin.equals(def.parms.origin);
                        boolean boundsMatch = re.bounds.equals(def.referenceBounds);
                        boolean modelMatch = (re.hModel == def.parms.hModel);

                        if (boundsMatch && originMatch && axisMatch && modelMatch) {
                            // only clear the dynamic model and interaction surfaces if they exist
                            c_callbackUpdate++;
                            R_ClearEntityDefDynamicModel(def);
                            def.parms = new renderEntity_s(re);
                            return;
                        }
                    }
                }

                // save any decals if the model is the same, allowing marks to move with entities
                if (def.parms.hModel == re.hModel) {
                    R_FreeEntityDefDerivedData(def, true, true);
                } else {
                    R_FreeEntityDefDerivedData(def, false, false);
                }
            } else {
                // creating a new one
                def = new idRenderEntityLocal();
                entityDefs.oSet(entityHandle, def);

                def.world = this;
                def.index = entityHandle;
            }

            def.parms = new renderEntity_s(re);
//        TempDump.printCallStack("~~~~~~~~~~~~~~~~~" );
            R_AxisToModelMatrix(def.parms.axis, def.parms.origin, def.modelMatrix);

            def.lastModifiedFrameNum = tr.frameCount;
            if (session.writeDemo != null && def.archived) {
                WriteFreeEntity(entityHandle);
                def.archived = false;
            }

            // optionally immediately issue any callbacks
            if (!r_useEntityCallbacks.GetBool() && def.parms.callback != null) {
                R_IssueEntityDefCallback(def);
            }

            // based on the model bounds, add references in each area
            // that may contain the updated surface
            R_CreateEntityRefs(def);
        }

        /*
         ===================
         FreeEntityDef

         Frees all references and lit surfaces from the model, and
         NULL's out it's entry in the world list
         ===================
         */
        @Override
        public void FreeEntityDef(int entityHandle) {
            idRenderEntityLocal def;

            if (entityHandle < 0 || entityHandle >= entityDefs.Num()) {
                common.Printf("idRenderWorld::FreeEntityDef: handle %d > %d\n", entityHandle, entityDefs.Num());
                return;
            }

            def = entityDefs.oGet(entityHandle);
            if (NOT(def)) {
                common.Printf("idRenderWorld::FreeEntityDef: handle %d is NULL\n", entityHandle);
                return;
            }

            R_FreeEntityDefDerivedData(def, false, false);

            if (session.writeDemo != null && def.archived) {
                WriteFreeEntity(entityHandle);
            }

            // if we are playing a demo, these will have been freed
            // in R_FreeEntityDefDerivedData(), otherwise the gui
            // object still exists in the game
            def.parms.gui[0] = null;
            def.parms.gui[1] = null;
            def.parms.gui[2] = null;

//	delete def;
            entityDefs.oSet(entityHandle, null);
        }

        @Override
        public renderEntity_s GetRenderEntity(int entityHandle) {
            idRenderEntityLocal def;

            if (entityHandle < 0 || entityHandle >= entityDefs.Num()) {
                common.Printf("idRenderWorld::GetRenderEntity: invalid handle %d [0, %d]\n", entityHandle, entityDefs.Num());
                return null;
            }

            def = entityDefs.oGet(entityHandle);
            if (NOT(def)) {
                common.Printf("idRenderWorld::GetRenderEntity: handle %d is NULL\n", entityHandle);
                return null;
            }

            return def.parms;
        }

        @Override
        public int AddLightDef(renderLight_s rlight) {
            // try and reuse a free spot
            int lightHandle = lightDefs.FindNull();

            if (lightHandle == -1) {
                lightHandle = lightDefs.Append((idRenderLightLocal) null);
                if (interactionTable != null && lightDefs.Num() > interactionTableHeight) {
                    ResizeInteractionTable();
                }
            }
            UpdateLightDef(lightHandle, rlight);

            return lightHandle;
        }

        /*
         =================
         UpdateLightDef

         The generation of all the derived interaction data will
         usually be deferred until it is visible in a scene

         Does not write to the demo file, which will only be done for visible lights
         =================
         */
        @Override
        public void UpdateLightDef(int lightHandle, renderLight_s rlight) {
            if (r_skipUpdates.GetBool()) {
                return;
            }

            tr.pc.c_lightUpdates++;

            // create new slots if needed
            if (lightHandle < 0 || lightHandle > LUDICROUS_INDEX) {
                common.Error("idRenderWorld::UpdateLightDef: index = %d", lightHandle);
            }
            while (lightHandle >= lightDefs.Num()) {
                lightDefs.Append((idRenderLightLocal) null);
            }

            boolean justUpdate = false;
            idRenderLightLocal light = lightDefs.oGet(lightHandle);
            if (light != null) {
                // if the shape of the light stays the same, we don't need to dump
                // any of our derived data, because shader parms are calculated every frame
                if (rlight.axis.equals(light.parms.axis) && rlight.end.equals(light.parms.end)
                        && rlight.lightCenter.equals(light.parms.lightCenter) && rlight.lightRadius.equals(light.parms.lightRadius)
                        && rlight.noShadows == light.parms.noShadows && rlight.origin.equals(light.parms.origin)
                        && rlight.parallel == light.parms.parallel && rlight.pointLight == light.parms.pointLight
                        && rlight.right.equals(light.parms.right) && rlight.start.equals(light.parms.start)
                        && rlight.target.equals(light.parms.target) && rlight.up.equals(light.parms.up)
                        && rlight.shader == light.lightShader && rlight.prelightModel == light.parms.prelightModel) {
                    justUpdate = true;
                } else {
                    // if we are updating shadows, the prelight model is no longer valid
                    light.lightHasMoved = true;
                    R_FreeLightDefDerivedData(light);
                }
            } else {
                // create a new one
                light = new idRenderLightLocal();
                lightDefs.oSet(lightHandle, light);

                light.world = this;
                light.index = lightHandle;
            }

            light.parms = new renderLight_s(rlight);
            light.lastModifiedFrameNum = tr.frameCount;
            if (session.writeDemo != null && light.archived) {
                WriteFreeLight(lightHandle);
                light.archived = false;
            }

            if (light.lightHasMoved) {
                light.parms.prelightModel = null;
            }

            if (!justUpdate) {
                R_DeriveLightData(light);
                R_CreateLightRefs(light);
                R_CreateLightDefFogPortals(light);
            }
        }

        /*
         ====================
         FreeLightDef

         Frees all references and lit surfaces from the light, and
         NULL's out it's entry in the world list
         ====================
         */
        @Override
        public void FreeLightDef(int lightHandle) {
            idRenderLightLocal light;

            if (lightHandle < 0 || lightHandle >= lightDefs.Num()) {
                common.Printf("idRenderWorld::FreeLightDef: invalid handle %d [0, %d]\n", lightHandle, lightDefs.Num());
                return;
            }

            light = lightDefs.oGet(lightHandle);
            if (NOT(light)) {
                common.Printf("idRenderWorld::FreeLightDef: handle %d is NULL\n", lightHandle);
                return;
            }

            R_FreeLightDefDerivedData(light);

            if (session.writeDemo != null && light.archived) {
                WriteFreeLight(lightHandle);
            }

            //delete light;
            lightDefs.oSet(lightHandle, null);
        }

        @Override
        public renderLight_s GetRenderLight(int lightHandle) {
            idRenderLightLocal def;

            if (lightHandle < 0 || lightHandle >= lightDefs.Num()) {
                common.Printf("idRenderWorld::GetRenderLight: handle %d > %d\n", lightHandle, lightDefs.Num());
                return null;
            }

            def = lightDefs.oGet(lightHandle);
            if (null == def) {
                common.Printf("idRenderWorld::GetRenderLight: handle %d is NULL\n", lightHandle);
                return null;
            }

            return def.parms;
        }

        @Override
        public boolean CheckAreaForPortalSky(int areaNum) {
            areaReference_s ref;

            assert (areaNum >= 0 && areaNum < numPortalAreas);

            for (ref = portalAreas[areaNum].entityRefs.areaNext; ref.entity != null; ref = ref.areaNext) {
                assert (ref.area == portalAreas[areaNum]);

                if (ref.entity != null && ref.entity.needsPortalSky) {
                    return true;
                }
            }

            return false;
        }

        /*
         ===================
         GenerateAllInteractions

         Force the generation of all light / surface interactions at the start of a level
         If this isn't called, they will all be dynamically generated

         This really isn't all that helpful anymore, because the calculation of shadows
         and light interactions is deferred from idRenderWorldLocal::CreateLightDefInteractions(), but we
         use it as an oportunity to size the interactionTable
         ===================
         */
        @Override
        public void GenerateAllInteractions() {
            if (!glConfig.isInitialized) {
                return;
            }

            int start = Sys_Milliseconds();

            generateAllInteractionsCalled = false;

            // watch how much memory we allocate
            tr.staticAllocCount = 0;

            // let idRenderWorldLocal::CreateLightDefInteractions() know that it shouldn't
            // try and do any view specific optimizations
            tr.viewDef = null;

            for (int i = 0; i < this.lightDefs.Num(); i++) {
                idRenderLightLocal ldef = this.lightDefs.oGet(i);
                if (NOT(ldef)) {
                    continue;
                }
                this.CreateLightDefInteractions(ldef);
            }

            int end = Sys_Milliseconds();
            int msec = end - start;

            common.Printf("idRenderWorld::GenerateAllInteractions, msec = %d, staticAllocCount = %d.\n", msec, tr.staticAllocCount);

            // build the interaction table
            if (RenderSystem_init.r_useInteractionTable.GetBool()) {
                interactionTableWidth = entityDefs.Num() + 100;
                interactionTableHeight = lightDefs.Num() + 100;
                int size = interactionTableWidth * interactionTableHeight;//* sizeof(interactionTable);
                interactionTable = new idInteraction[size];// R_ClearedStaticAlloc(size);

                int count = 0;
                for (int i = 0; i < this.lightDefs.Num(); i++) {
                    idRenderLightLocal ldef = this.lightDefs.oGet(i);
                    if (NOT(ldef)) {
                        continue;
                    }
                    idInteraction inter;
                    for (inter = ldef.firstInteraction; inter != null; inter = inter.lightNext) {
                        idRenderEntityLocal edef = inter.entityDef;
                        int index = ldef.index * interactionTableWidth + edef.index;

                        interactionTable[index] = inter;
                        count++;
                    }
                }

                common.Printf("interactionTable size: %d bytes\n", size);
                common.Printf("%d interaction take %d bytes\n", count, count /* sizeof(idInteraction)*/);
            }

            // entities flagged as noDynamicInteractions will no longer make any
            generateAllInteractionsCalled = true;
        }

        @Override
        public void RegenerateWorld() {
            R_RegenerateWorld_f.getInstance().run(new idCmdArgs());
        }

        @Override
        public void ProjectDecalOntoWorld(idFixedWinding winding, idVec3 projectionOrigin, boolean parallel, float fadeDepth, idMaterial material, int startTime) {
            int i, numAreas;
            int[] areas = new int[10];
            areaReference_s ref;
            portalArea_s area;
            idRenderModel model;
            idRenderEntityLocal def;
            decalProjectionInfo_s info = new decalProjectionInfo_s(), localInfo = new decalProjectionInfo_s();

            if (!idRenderModelDecal.CreateProjectionInfo(info, winding, projectionOrigin, parallel, fadeDepth, material, startTime)) {
                return;
            }

            // get the world areas touched by the projection volume
            numAreas = BoundsInAreas(info.projectionBounds, areas, 10);

            // check all areas for models
            for (i = 0; i < numAreas; i++) {

                area = portalAreas[areas[i]];

                // check all models in this area
                for (ref = area.entityRefs.areaNext; ref != area.entityRefs; ref = ref.areaNext) {
                    def = ref.entity;

                    // completely ignore any dynamic or callback models
                    model = def.parms.hModel;
                    if (model == null || model.IsDynamicModel() != DM_STATIC || def.parms.callback != null) {
                        continue;
                    }

                    if (def.parms.customShader != null && !def.parms.customShader.AllowOverlays()) {
                        continue;
                    }

                    idBounds bounds = new idBounds();
                    bounds.FromTransformedBounds(model.Bounds(def.parms), def.parms.origin, def.parms.axis);

                    // if the model bounds do not overlap with the projection bounds
                    if (!info.projectionBounds.IntersectsBounds(bounds)) {
                        continue;
                    }

                    // transform the bounding planes, fade planes and texture axis into local space
                    idRenderModelDecal.GlobalProjectionInfoToLocal(localInfo, info, def.parms.origin, def.parms.axis);
                    localInfo.force = (def.parms.customShader != null);

                    if (NOT(def.decals)) {
                        def.decals = idRenderModelDecal.Alloc();
                    }
                    def.decals.CreateDecal(model, localInfo);
                }
            }
        }

        @Override
        public void ProjectDecal(int entityHandle, idFixedWinding winding, idVec3 projectionOrigin, boolean parallel, float fadeDepth, idMaterial material, int startTime) {
            decalProjectionInfo_s info = new decalProjectionInfo_s(), localInfo = new decalProjectionInfo_s();

            if (entityHandle < 0 || entityHandle >= entityDefs.Num()) {
                common.Error("idRenderWorld::ProjectOverlay: index = %d", entityHandle);
                return;
            }

            idRenderEntityLocal def = entityDefs.oGet(entityHandle);
            if (NOT(def)) {
                return;
            }

            final idRenderModel model = def.parms.hModel;

            if (model == null || model.IsDynamicModel() != DM_STATIC || def.parms.callback != null) {
                return;
            }

            if (!idRenderModelDecal.CreateProjectionInfo(info, winding, projectionOrigin, parallel, fadeDepth, material, startTime)) {
                return;
            }

            idBounds bounds = new idBounds();
            bounds.FromTransformedBounds(model.Bounds(def.parms), def.parms.origin, def.parms.axis);

            // if the model bounds do not overlap with the projection bounds
            if (!info.projectionBounds.IntersectsBounds(bounds)) {
                return;
            }

            // transform the bounding planes, fade planes and texture axis into local space
            idRenderModelDecal.GlobalProjectionInfoToLocal(localInfo, info, def.parms.origin, def.parms.axis);
            localInfo.force = (def.parms.customShader != null);

            if (def.decals == null) {
                def.decals = idRenderModelDecal.Alloc();
            }
            def.decals.CreateDecal(model, localInfo);
        }

        @Override
        public void ProjectOverlay(int entityHandle, idPlane[] localTextureAxis, idMaterial material) {

            if (entityHandle < 0 || entityHandle >= entityDefs.Num()) {
                common.Error("idRenderWorld::ProjectOverlay: index = %d", entityHandle);
                return;
            }

            idRenderEntityLocal def = entityDefs.oGet(entityHandle);
            if (NOT(def)) {
                return;
            }

            final renderEntity_s refEnt = def.parms;

            idRenderModel model = refEnt.hModel;
            if (model.IsDynamicModel() != DM_CACHED) {	// FIXME: probably should be MD5 only
                return;
            }
            model = R_EntityDefDynamicModel(def);

            if (def.overlay == null) {
                def.overlay = idRenderModelOverlay.Alloc();
            }
            def.overlay.CreateOverlay(model, localTextureAxis, material);
        }

        @Override
        public void RemoveDecals(int entityHandle) {
            if (entityHandle < 0 || entityHandle >= entityDefs.Num()) {
                common.Error("idRenderWorld::ProjectOverlay: index = %d", entityHandle);
                return;
            }

            idRenderEntityLocal def = entityDefs.oGet(entityHandle);
            if (NOT(def)) {
                return;
            }

            R_FreeEntityDefDecals(def);
            R_FreeEntityDefOverlay(def);
        }

        /*
         ====================
         SetRenderView

         Sets the current view so any calls to the render world will use the correct parms.
         ====================
         */
        @Override
        public void SetRenderView(renderView_s renderView) {
            tr.primaryRenderView = new renderView_s(renderView);
        }

        /*
         ====================
         RenderScene

         Draw a 3D view into a part of the window, then return
         to 2D drawing.

         Rendering a scene may require multiple views to be rendered
         to handle mirrors,
         ====================
         */
        @Override
        public void RenderScene(renderView_s renderView) {
            if (!ID_DEDICATED) {
                renderView_s copy;

                if (!glConfig.isInitialized) {
                    return;
                }

                copy = new renderView_s(renderView);

                // skip front end rendering work, which will result
                // in only gui drawing
                if (r_skipFrontEnd.GetBool()) {
                    return;
                }

                if (renderView.fov_x <= 0 || renderView.fov_y <= 0) {
                    common.Error("idRenderWorld::RenderScene: bad FOVs: %f, %f", renderView.fov_x, renderView.fov_y);
                }

                // close any gui drawing
                tr.guiModel.EmitFullScreen();
                tr.guiModel.Clear();

                int startTime = Sys_Milliseconds();

                // setup view parms for the initial view
                //
                viewDef_s parms = new viewDef_s();// R_ClearedFrameAlloc(sizeof(parms));
                parms.renderView = new renderView_s(renderView);

                if (tr.takingScreenshot) {
                    parms.renderView.forceUpdate = true;
                }

                // set up viewport, adjusted for resolution and OpenGL style 0 at the bottom
                tr.RenderViewToViewport(parms.renderView, parms.viewport);

                // the scissor bounds may be shrunk in subviews even if
                // the viewport stays the same
                // this scissor range is local inside the viewport
                parms.scissor.x1 = 0;
                parms.scissor.y1 = 0;
                parms.scissor.x2 = parms.viewport.x2 - parms.viewport.x1;
                parms.scissor.y2 = parms.viewport.y2 - parms.viewport.y1;

                parms.isSubview = false;
                parms.initialViewAreaOrigin = new idVec3(renderView.vieworg);
                parms.floatTime = parms.renderView.time * 0.001f;
                parms.renderWorld = this;

                // use this time for any subsequent 2D rendering, so damage blobs/etc 
                // can use level time
                tr.frameShaderTime = parms.floatTime;

                // see if the view needs to reverse the culling sense in mirrors
                // or environment cube sides
                idVec3 cross;
                cross = parms.renderView.viewaxis.oGet(1).Cross(parms.renderView.viewaxis.oGet(2));
                if (cross.oMultiply(parms.renderView.viewaxis.oGet(0)) > 0) {
                    parms.isMirror = false;
                } else {
                    parms.isMirror = true;
                }

                if (r_lockSurfaces.GetBool()) {
                    R_LockSurfaceScene(parms);
                    return;
                }

                // save this world for use by some console commands
                tr.primaryWorld = this;
                tr.primaryRenderView = new renderView_s(renderView);
                tr.primaryView = parms;

                // rendering this view may cause other views to be rendered
                // for mirrors / portals / shadows / environment maps
                // this will also cause any necessary entities and lights to be
                // updated to the demo file
                R_RenderView(parms);

                // now write delete commands for any modified-but-not-visible entities, and
                // add the renderView command to the demo
                if (session.writeDemo != null) {
                    WriteRenderView(renderView);
                }

//                if (false) {
//                    for (int i = 0; i < entityDefs.Num(); i++) {
//                        idRenderEntityLocal def = entityDefs.oGet(i);
//                        if (!def) {
//                            continue;
//                        }
//                        if (def.parms.callback) {
//                            continue;
//                        }
//                        if (def.parms.hModel.IsDynamicModel() == DM_CONTINUOUS) {
//                        }
//                    }
//                }
                int endTime = Sys_Milliseconds();

                tr.pc.frontEndMsec += endTime - startTime;

                // prepare for any 2D drawing after this
                tr.guiModel.Clear();
            }
        }

        @Override
        public int NumAreas() {
            return numPortalAreas;
        }

        /*
         ===============
         PointInAreaNum

         Will return -1 if the point is not in an area, otherwise
         it will return 0 <= value < tr.world->numPortalAreas
         ===============
         */
        @Override
        public int PointInArea(final idVec3 point) {
            areaNode_t node;
            int nodeNum;
            float d;

            if (null == areaNodes) {
                return -1;
            }
            node = areaNodes[0];
            while (true) {
                d = node.plane.Normal().oMultiply(point) + node.plane.oGet(3);
                if (d > 0) {
                    nodeNum = node.children[0];
                } else {
                    nodeNum = node.children[1];
                }
                if (nodeNum == 0) {
                    return -1;		// in solid
                }
                if (nodeNum < 0) {
                    nodeNum = -1 - nodeNum;
                    if (nodeNum >= numPortalAreas) {
                        common.Error("idRenderWorld::PointInArea: area out of range");
                    }
                    return nodeNum;
                }
                node = areaNodes[nodeNum];
            }

//            return -1;
        }

        /*
         ===================
         BoundsInAreas

         fills the *areas array with the number of the areas the bounds are in
         returns the total number of areas the bounds are in
         ===================
         */
        @Override
        public int BoundsInAreas(idBounds bounds, int[] areas, int maxAreas) {
            int[] numAreas = new int[1];

            assert (areas != null);
            assert (bounds.oGet(0).oGet(0) <= bounds.oGet(1).oGet(0) && bounds.oGet(0).oGet(1) <= bounds.oGet(1).oGet(1) && bounds.oGet(0).oGet(2) <= bounds.oGet(1).oGet(2));
            assert (bounds.oGet(1).oGet(0) - bounds.oGet(0).oGet(0) < 1e4f && bounds.oGet(1).oGet(1) - bounds.oGet(0).oGet(1) < 1e4f && bounds.oGet(1).oGet(2) - bounds.oGet(0).oGet(2) < 1e4f);

            if (null == areaNodes) {
                return 0;
            }
            BoundsInAreas_r(0, bounds, areas, numAreas, maxAreas);
            return numAreas[0];
        }

        @Override
        public int NumPortalsInArea(int areaNum) {
            portalArea_s area;
            int count;
            portal_s portal;

            if (areaNum >= numPortalAreas || areaNum < 0) {
                common.Error("idRenderWorld::NumPortalsInArea: bad areanum %d", areaNum);
            }
            area = portalAreas[areaNum];

            count = 0;
            for (portal = area.portals; portal != null; portal = portal.next) {
                count++;
            }
            return count;
        }

        @Override
        public exitPortal_t GetPortal(int areaNum, int portalNum) {
            portalArea_s area;
            int count;
            portal_s portal;
            exitPortal_t ret = new exitPortal_t();

            if (areaNum > numPortalAreas) {
                common.Error("idRenderWorld::GetPortal: areaNum > numAreas");
            }
            area = portalAreas[areaNum];

            count = 0;
            for (portal = area.portals; portal != null; portal = portal.next) {
                if (count == portalNum) {
                    ret.areas[0] = areaNum;
                    ret.areas[1] = portal.intoArea;
                    ret.w = portal.w;
                    ret.blockingBits = portal.doublePortal.blockingBits;
                    ret.portalHandle = indexOf(portal.doublePortal, doublePortals) + 1;
                    return ret;
                }
                count++;
            }

            common.Error("idRenderWorld::GetPortal: portalNum > numPortals");

//            memset(ret, 0, sizeof(ret));
            return new exitPortal_t();
        }

        /*
         ================
         GuiTrace

         checks a ray trace against any gui surfaces in an entity, returning the
         fraction location of the trace on the gui surface, or -1,-1 if no hit.
         this doesn't do any occlusion testing, simply ignoring non-gui surfaces.
         start / end are in global world coordinates.
         ================
         */
        @Override
        public guiPoint_t GuiTrace(int entityHandle, idVec3 start, idVec3 end) {
            localTrace_t local;
            idVec3 localStart = new idVec3(), localEnd = new idVec3(), bestPoint;
            int j;
            idRenderModel model;
            srfTriangles_s tri;
            idMaterial shader;
            guiPoint_t pt = new guiPoint_t();

            pt.x = pt.y = -1;
            pt.guiId = 0;

            if ((entityHandle < 0) || (entityHandle >= entityDefs.Num())) {
                common.Printf("idRenderWorld::GuiTrace: invalid handle %d\n", entityHandle);
                return pt;
            }

            idRenderEntityLocal def = entityDefs.oGet(entityHandle);
            if (NOT(def)) {
                common.Printf("idRenderWorld::GuiTrace: handle %d is NULL\n", entityHandle);
                return pt;
            }

            model = def.parms.hModel;
            if (def.parms.callback != null || NOT(def.parms.hModel) || def.parms.hModel.IsDynamicModel() != DM_STATIC) {
                return pt;
            }

            // transform the points into local space
            R_GlobalPointToLocal(def.modelMatrix, start, localStart);
            R_GlobalPointToLocal(def.modelMatrix, end, localEnd);

            float best = 99999f;
            final modelSurface_s bestSurf = null;

            for (j = 0; j < model.NumSurfaces(); j++) {
                final modelSurface_s surf = model.Surface(j);

                tri = surf.geometry;
                if (null == tri) {
                    continue;
                }

                shader = R_RemapShaderBySkin(surf.shader, def.parms.customSkin, def.parms.customShader);
                if (null == shader) {
                    continue;
                }
                // only trace against gui surfaces
                if (!shader.HasGui()) {
                    continue;
                }

                local = R_LocalTrace(localStart, localEnd, 0.0f, tri);
                if (local.fraction < 1.0) {
                    idVec3 origin = new idVec3();
                    idVec3[] axis = idVec3.generateArray(3);
                    idVec3 cursor;
                    float[] axisLen = new float[2];

                    R_SurfaceToTextureAxis(tri, origin, axis);
                    cursor = local.point.oMinus(origin);

                    axisLen[0] = axis[0].Length();
                    axisLen[1] = axis[1].Length();

                    pt.x = (cursor.oMultiply(axis[0])) / (axisLen[0] * axisLen[0]);
                    pt.y = (cursor.oMultiply(axis[1])) / (axisLen[1] * axisLen[1]);
                    pt.guiId = shader.GetEntityGui();

                    return pt;
                }
            }

            return pt;
        }

        @Override
        public boolean ModelTrace(modelTrace_s trace, int entityHandle, idVec3 start, idVec3 end, float radius) {
            int i;
            boolean collisionSurface;
            modelSurface_s surf;
            localTrace_t localTrace;
            idRenderModel model;
            float[] modelMatrix = new float[16];
            idVec3 localStart = new idVec3(), localEnd = new idVec3();
            idMaterial shader;

            trace.fraction = 1.0f;

            if (entityHandle < 0 || entityHandle >= entityDefs.Num()) {
//		common.Error( "idRenderWorld::ModelTrace: index = %i", entityHandle );
                return false;
            }

            idRenderEntityLocal def = entityDefs.oGet(entityHandle);
            if (null == def) {
                return false;
            }

            renderEntity_s refEnt = def.parms;

            model = R_EntityDefDynamicModel(def);
            if (null == model) {
                return false;
            }

            // transform the points into local space
            R_AxisToModelMatrix(refEnt.axis, refEnt.origin, modelMatrix);
            R_GlobalPointToLocal(modelMatrix, start, localStart);
            R_GlobalPointToLocal(modelMatrix, end, localEnd);

            // if we have explicit collision surfaces, only collide against them
            // (FIXME, should probably have a parm to control this)
            collisionSurface = false;
            for (i = 0; i < model.NumBaseSurfaces(); i++) {
                surf = model.Surface(i);

                shader = R_RemapShaderBySkin(surf.shader, def.parms.customSkin, def.parms.customShader);

                if ((shader.GetSurfaceFlags() & SURF_COLLISION) != 0) {
                    collisionSurface = true;
                    break;
                }
            }

            // only use baseSurfaces, not any overlays
            for (i = 0; i < model.NumBaseSurfaces(); i++) {
                surf = model.Surface(i);

                shader = R_RemapShaderBySkin(surf.shader, def.parms.customSkin, def.parms.customShader);

                if (null == surf.geometry || null == shader) {
                    continue;
                }

                if (collisionSurface) {
                    // only trace vs collision surfaces
                    if (0 == (shader.GetSurfaceFlags() & SURF_COLLISION)) {
                        continue;
                    }
                } else {
                    // skip if not drawn or translucent
                    if (!shader.IsDrawn() || (shader.Coverage() != MC_OPAQUE && shader.Coverage() != MC_PERFORATED)) {
                        continue;
                    }
                }

                localTrace = R_LocalTrace(localStart, localEnd, radius, surf.geometry);

                if (localTrace.fraction < trace.fraction) {
                    trace.fraction = localTrace.fraction;
                    trace.point = R_LocalPointToGlobal(modelMatrix, localTrace.point);
                    trace.normal = localTrace.normal.oMultiply(refEnt.axis);
                    trace.material = shader;
                    trace.entity = def.parms;
                    trace.jointNumber = refEnt.hModel.NearestJoint(i, localTrace.indexes[0], localTrace.indexes[1], localTrace.indexes[2]);
                }
            }

            return (trace.fraction < 1.0f);
        }
        // FIXME: _D3XP added those.
        static final String[] playerModelExcludeList = {
            "models/md5/characters/player/d3xp_spplayer.md5mesh",
            "models/md5/characters/player/head/d3xp_head.md5mesh",
            "models/md5/weapons/pistol_world/worldpistol.md5mesh",
            null
        };
        static final String[] playerMaterialExcludeList = {
            "muzzlesmokepuff",
            null
        };

        @Override
        public boolean Trace(modelTrace_s trace, idVec3 start, idVec3 end, float radius, boolean skipDynamic, boolean skipPlayer /*_D3XP*/) {
            areaReference_s ref;
            idRenderEntityLocal def;
            portalArea_s area;
            idRenderModel model;
            srfTriangles_s tri;
            localTrace_t localTrace;
            int[] areas = new int[128];
            int numAreas, i, j, numSurfaces;
            idBounds traceBounds = new idBounds(), bounds = new idBounds();
            float[] modelMatrix = new float[16];
            idVec3 localStart = new idVec3(), localEnd = new idVec3();
            idMaterial shader;

            trace.fraction = 1.0f;
            trace.point = end;

            // bounds for the whole trace
            traceBounds.Clear();
            traceBounds.AddPoint(start);
            traceBounds.AddPoint(end);

            // get the world areas the trace is in
            numAreas = BoundsInAreas(traceBounds, areas, 128);

            numSurfaces = 0;

            // check all areas for models
            for (i = 0; i < numAreas; i++) {

                area = portalAreas[areas[i]];

                // check all models in this area
                for (ref = area.entityRefs.areaNext; ref != area.entityRefs; ref = ref.areaNext) {
                    def = ref.entity;

                    model = def.parms.hModel;
                    if (null == model) {
                        continue;
                    }

                    if (model.IsDynamicModel() != DM_STATIC) {
                        if (skipDynamic) {
                            continue;
                        }

                        if (true) {	/* _D3XP addition. could use a cleaner approach */

                            if (skipPlayer) {
                                final String name = model.Name();
                                String exclude;
                                int k;

                                for (k = 0; playerModelExcludeList.length > k; k++) {
                                    exclude = playerModelExcludeList[k];
                                    if (name.equals(exclude)) {
                                        break;
                                    }
                                }

                                if (playerModelExcludeList[k] != null) {
                                    continue;
                                }
                            }
                        }

                        model = R_EntityDefDynamicModel(def);
                        if (null == model) {
                            continue;	// can happen with particle systems, which don't instantiate without a valid view
                        }
                    }

                    bounds.FromTransformedBounds(model.Bounds(def.parms), def.parms.origin, def.parms.axis);

                    // if the model bounds do not overlap with the trace bounds
                    if (!traceBounds.IntersectsBounds(bounds) || !bounds.LineIntersection(start, trace.point)) {
                        continue;
                    }

                    // check all model surfaces
                    for (j = 0; j < model.NumSurfaces(); j++) {
                        modelSurface_s surf = model.Surface(j);

                        shader = R_RemapShaderBySkin(surf.shader, def.parms.customSkin, def.parms.customShader);

                        // if no geometry or no shader
                        if (null == surf.geometry || null == shader) {
                            continue;
                        }

                        if (true) { /* _D3XP addition. could use a cleaner approach */

                            if (skipPlayer) {
                                final String name = shader.GetName();
                                String exclude;
                                int k;

                                for (k = 0; k < playerMaterialExcludeList.length; k++) {
                                    exclude = playerMaterialExcludeList[k];
                                    if (name.equals(exclude)) {
                                        break;
                                    }
                                }

                                if (playerMaterialExcludeList[k] != null) {
                                    continue;
                                }
                            }
                        }

                        tri = surf.geometry;

                        bounds.FromTransformedBounds(tri.bounds, def.parms.origin, def.parms.axis);

                        // if triangle bounds do not overlap with the trace bounds
                        if (!traceBounds.IntersectsBounds(bounds) || !bounds.LineIntersection(start, trace.point)) {
                            continue;
                        }

                        numSurfaces++;

                        // transform the points into local space
                        R_AxisToModelMatrix(def.parms.axis, def.parms.origin, modelMatrix);
                        R_GlobalPointToLocal(modelMatrix, start, localStart);
                        R_GlobalPointToLocal(modelMatrix, end, localEnd);

                        localTrace = R_LocalTrace(localStart, localEnd, radius, surf.geometry);

                        if (localTrace.fraction < trace.fraction) {
                            trace.fraction = localTrace.fraction;
                            trace.point = R_LocalPointToGlobal(modelMatrix, localTrace.point);
                            trace.normal = localTrace.normal.oMultiply(def.parms.axis);
                            trace.material = shader;
                            trace.entity = def.parms;
                            trace.jointNumber = model.NearestJoint(j, localTrace.indexes[0], localTrace.indexes[1], localTrace.indexes[2]);

                            traceBounds.Clear();
                            traceBounds.AddPoint(start);
                            traceBounds.AddPoint(start.oPlus((end.oMinus(start)).oMultiply(trace.fraction)));
                        }
                    }
                }
            }
            return (trace.fraction < 1.0f);
        }

        @Override
        public boolean FastWorldTrace(modelTrace_s results, idVec3 start, idVec3 end) {
//            memset(results, 0, sizeof(modelTrace_t));
            results.clear();
            results.fraction = 1.0f;
            if (areaNodes != null) {
                RecurseProcBSP_r(results, -1, 0, 0.0f, 1.0f, start, end);
                return (results.fraction < 1.0f);
            }
            return false;
        }

        @Override
        public void DebugClearLines(int time) {
            RB_ClearDebugLines(time);
            RB_ClearDebugText(time);
        }

        @Override
        public void DebugLine(idVec4 color, idVec3 start, idVec3 end, int lifetime, boolean depthTest) {
            RB_AddDebugLine(color, start, end, lifetime, depthTest);
        }
        private static final float[] arrowCos = new float[40];
        private static final float[] arrowSin = new float[40];
        private static int arrowStep;

        @Override
        public void DebugArrow(idVec4 color, idVec3 start, idVec3 end, int size, int lifetime) {
            idVec3 forward, right = new idVec3(), up = new idVec3(), v1, v2;
            float a, s;
            int i;

            DebugLine(color, start, end, lifetime);

            if (RenderSystem_init.r_debugArrowStep.GetInteger() <= 10) {
                return;
            }
            // calculate sine and cosine when step size changes
            if (arrowStep != RenderSystem_init.r_debugArrowStep.GetInteger()) {
                arrowStep = RenderSystem_init.r_debugArrowStep.GetInteger();
                for (i = 0, a = 0; a < 360.0f; a += arrowStep, i++) {
                    arrowCos[i] = idMath.Cos16((float) DEG2RAD(a));
                    arrowSin[i] = idMath.Sin16((float) DEG2RAD(a));
                }
                arrowCos[i] = arrowCos[0];
                arrowSin[i] = arrowSin[0];
            }
            // draw a nice arrow
            forward = end.oMinus(start);
            forward.Normalize();
            forward.NormalVectors(right, up);
            for (i = 0, a = 0; a < 360.0f; a += arrowStep, i++) {
                s = 0.5f * size * arrowCos[i];
                v1 = end.oMinus(forward.oMultiply(size));
                v1 = v1.oPlus(right.oMultiply(s));
                s = 0.5f * size * arrowSin[i];
                v1 = v1.oPlus(up.oMultiply(s));

                s = 0.5f * size * arrowCos[i + 1];
                v2 = end.oMinus(forward.oMultiply(size));
                v2 = v2.oPlus(right.oMultiply(s));
                s = 0.5f * size * arrowSin[i + 1];
                v2 = v2.oPlus(up.oMultiply(s));

                DebugLine(color, v1, end, lifetime);
                DebugLine(color, v1, v2, lifetime);
            }
        }

        @Override
        public void DebugWinding(idVec4 color, idWinding w, idVec3 origin, idMat3 axis, int lifetime, boolean depthTest) {
            int i;
            idVec3 point, lastPoint;

            if (w.GetNumPoints() < 2) {
                return;
            }

            lastPoint = origin.oPlus(w.oGet(w.GetNumPoints() - 1).ToVec3().oMultiply(axis));
            for (i = 0; i < w.GetNumPoints(); i++) {
                point = origin.oPlus(w.oGet(i).ToVec3().oMultiply(axis));
                DebugLine(color, lastPoint, point, lifetime, depthTest);
                lastPoint = point;
            }
        }

        @Override
        public void DebugCircle(idVec4 color, idVec3 origin, idVec3 dir, float radius, int numSteps, int lifetime, boolean depthTest) {
            int i;
            float a;
            idVec3 left = new idVec3(), up = new idVec3(), point, lastPoint;

            dir.OrthogonalBasis(left, up);
            left.oMulSet(radius);
            up.oMulSet(radius);
            lastPoint = origin.oPlus(up);
            for (i = 1; i <= numSteps; i++) {
                a = idMath.TWO_PI * i / numSteps;
                point = origin.oPlus(left.oMultiply(idMath.Sin16(a)).oPlus(up.oMultiply(idMath.Cos16(a))));
                DebugLine(color, lastPoint, point, lifetime, depthTest);
                lastPoint = point;
            }
        }

        @Override
        public void DebugSphere(idVec4 color, idSphere sphere, int lifetime, boolean depthTest) {
            int i, j, n, num;
            float s, c;
            idVec3 p = new idVec3(), lastp = new idVec3();
            idVec3[] lastArray;

            num = 360 / 15;
            lastArray = new idVec3[num];
            lastArray[0] = sphere.GetOrigin().oPlus(new idVec3(0, 0, sphere.GetRadius()));
            for (n = 1; n < num; n++) {
                lastArray[n] = lastArray[0];
            }

            for (i = 15; i <= 360; i += 15) {
                s = idMath.Sin16(DEG2RAD(i));
                c = idMath.Cos16(DEG2RAD(i));
                lastp.oSet(0, sphere.GetOrigin().oGet(0));
                lastp.oSet(1, sphere.GetOrigin().oGet(1) + sphere.GetRadius() * s);
                lastp.oSet(2, sphere.GetOrigin().oGet(2) + sphere.GetRadius() * c);
                for (n = 0, j = 15; j <= 360; j += 15, n++) {
                    p.oSet(0, sphere.GetOrigin().oGet(0) + idMath.Sin16(DEG2RAD(j)) * sphere.GetRadius() * s);
                    p.oSet(1, sphere.GetOrigin().oGet(1) + idMath.Cos16(DEG2RAD(j)) * sphere.GetRadius() * s);
                    p.oSet(2, lastp.oGet(2));

                    DebugLine(color, lastp, p, lifetime, depthTest);
                    DebugLine(color, lastp, lastArray[n], lifetime, depthTest);

                    lastArray[n] = lastp;
                    lastp = p;
                }
            }
        }

        @Override
        public void DebugBounds(idVec4 color, idBounds bounds, idVec3 org, int lifetime) {
            int i;
            idVec3[] v = new idVec3[8];

            if (bounds.IsCleared()) {
                return;
            }

            for (i = 0; i < 8; i++) {
                v[i].oSet(0, org.oGet(0) + bounds.oGet((i ^ (i >> 1)) & 1).oGet(0));
                v[i].oSet(1, org.oGet(1) + bounds.oGet((i >> 1) & 1).oGet(1));
                v[i].oSet(2, org.oGet(2) + bounds.oGet((i >> 2) & 1).oGet(2));
            }
            for (i = 0; i < 4; i++) {
                DebugLine(color, v[i], v[(i + 1) & 3], lifetime);
                DebugLine(color, v[4 + i], v[4 + ((i + 1) & 3)], lifetime);
                DebugLine(color, v[i], v[4 + i], lifetime);
            }
        }

        @Override
        public void DebugBox(idVec4 color, idBox box, int lifetime) {
            int i;
            idVec3[] v = new idVec3[8];

            box.ToPoints(v);
            for (i = 0; i < 4; i++) {
                DebugLine(color, v[i], v[(i + 1) & 3], lifetime);
                DebugLine(color, v[4 + i], v[4 + ((i + 1) & 3)], lifetime);
                DebugLine(color, v[i], v[4 + i], lifetime);
            }
        }

        @Override
        public void DebugFrustum(idVec4 color, idFrustum frustum, boolean showFromOrigin, int lifetime) {
            int i;
            idVec3[] v = new idVec3[8];

            frustum.ToPoints(v);

            if (frustum.GetNearDistance() > 0.0f) {
                for (i = 0; i < 4; i++) {
                    DebugLine(color, v[i], v[(i + 1) & 3], lifetime);
                }
                if (showFromOrigin) {
                    for (i = 0; i < 4; i++) {
                        DebugLine(color, frustum.GetOrigin(), v[i], lifetime);
                    }
                }
            }
            for (i = 0; i < 4; i++) {
                DebugLine(color, v[4 + i], v[4 + ((i + 1) & 3)], lifetime);
                DebugLine(color, v[i], v[4 + i], lifetime);
            }
        }

        /*
         ============
         idRenderWorldLocal::DebugCone

         dir is the cone axis
         radius1 is the radius at the apex
         radius2 is the radius at apex+dir
         ============
         */
        @Override
        public void DebugCone(idVec4 color, idVec3 apex, idVec3 dir, float radius1, float radius2, int lifetime) {
            int i;
            idMat3 axis = new idMat3();
            idVec3 top, p1, p2, lastp1, lastp2, d;

            axis.oSet(2, dir);
            axis.oGet(2).Normalize();
            axis.oGet(2).NormalVectors(axis.oGet(0), axis.oGet(1));
            axis.oSet(1, axis.oGet(1).oNegative());

            top = apex.oPlus(dir);
            lastp2 = top.oPlus(axis.oGet(1).oMultiply(radius2));

            if (radius1 == 0.0f) {
                for (i = 20; i <= 360; i += 20) {
                    d = axis.oGet(0).oMultiply(idMath.Sin16(DEG2RAD(i))).oPlus(axis.oGet(1).oMultiply(idMath.Cos16(DEG2RAD(i))));
                    p2 = top.oPlus(d.oMultiply(radius2));
                    DebugLine(color, lastp2, p2, lifetime);
                    DebugLine(color, p2, apex, lifetime);
                    lastp2 = p2;
                }
            } else {
                lastp1 = apex.oPlus(axis.oGet(1).oMultiply(radius1));
                for (i = 20; i <= 360; i += 20) {
                    d = axis.oGet(0).oMultiply(idMath.Sin16(DEG2RAD(i))).oPlus(axis.oGet(1).oMultiply(idMath.Cos16(DEG2RAD(i))));
                    p1 = apex.oPlus(d.oMultiply(radius1));
                    p2 = top.oPlus(d.oMultiply(radius2));
                    DebugLine(color, lastp1, p1, lifetime);
                    DebugLine(color, lastp2, p2, lifetime);
                    DebugLine(color, p1, p2, lifetime);
                    lastp1 = p1;
                    lastp2 = p2;
                }
            }
        }

        public void DebugScreenRect(final idVec4 color, final idScreenRect rect, final viewDef_s viewDef) {
            DebugScreenRect(color, rect, viewDef, 0);
        }

        public void DebugScreenRect(final idVec4 color, final idScreenRect rect, final viewDef_s viewDef, final int lifetime) {
            int i;
            float centerx, centery, dScale, hScale, vScale;
            idBounds bounds = new idBounds();
            idVec3[] p = new idVec3[4];

            centerx = (viewDef.viewport.x2 - viewDef.viewport.x1) * 0.5f;
            centery = (viewDef.viewport.y2 - viewDef.viewport.y1) * 0.5f;

            dScale = RenderSystem_init.r_znear.GetFloat() + 1.0f;
            hScale = dScale * idMath.Tan16((float) DEG2RAD(viewDef.renderView.fov_x * 0.5f));
            vScale = dScale * idMath.Tan16((float) DEG2RAD(viewDef.renderView.fov_y * 0.5f));

            bounds.oSet(0, 0,
                    bounds.oSet(1, 0, dScale));
            bounds.oSet(0, 1, -(rect.x1 - centerx) / centerx * hScale);
            bounds.oSet(1, 1, -(rect.x2 - centerx) / centerx * hScale);
            bounds.oSet(0, 2, (rect.y1 - centery) / centery * vScale);
            bounds.oSet(1, 2, (rect.y2 - centery) / centery * vScale);

            for (i = 0; i < 4; i++) {
                p[i] = new idVec3(bounds.oGet(0).oGet(0),
                        bounds.oGet((i ^ (i >> 1)) & 1).y,
                        bounds.oGet((i >> 1) & 1).z);
                p[i] = viewDef.renderView.vieworg.oPlus(p[i].oMultiply(viewDef.renderView.viewaxis));
            }
            for (i = 0; i < 4; i++) {
                DebugLine(color, p[i], p[(i + 1) & 3], 0);//false);
            }
        }

        @Override
        public void DebugAxis(idVec3 origin, idMat3 axis) {
            idVec3 start = origin;
            idVec3 end = start.oPlus(axis.oGet(0).oMultiply(20.0f));
            DebugArrow(colorWhite, start, end, 2);
            end = start.oPlus(axis.oGet(0).oMultiply(-20.0f));
            DebugArrow(colorWhite, start, end, 2);
            end = start.oPlus(axis.oGet(1).oMultiply(20.0f));
            DebugArrow(colorGreen, start, end, 2);
            end = start.oPlus(axis.oGet(1).oMultiply(-20.0f));
            DebugArrow(colorGreen, start, end, 2);
            end = start.oPlus(axis.oGet(2).oMultiply(20.0f));
            DebugArrow(colorBlue, start, end, 2);
            end = start.oPlus(axis.oGet(2).oMultiply(-20.0f));
            DebugArrow(colorBlue, start, end, 2);
        }

        @Override
        public void DebugClearPolygons(int time) {
            RB_ClearDebugPolygons(time);
        }

        @Override
        public void DebugPolygon(idVec4 color, idWinding winding, int lifeTime, boolean depthTest) {
            RB_AddDebugPolygon(color, winding, lifeTime, depthTest);
        }

        @Override
        public void DrawText(String text, idVec3 origin, float scale, idVec4 color, idMat3 viewAxis, int align, int lifetime, boolean depthTest) {
        	RB_AddDebugText( text, origin, scale, color, viewAxis, align, lifetime, depthTest );
        }

        //-----------------------
        // RenderWorld_load.cpp
        public idRenderModel ParseModel(idLexer src) throws idException {
            idRenderModel model;
            idToken token = new idToken();
            int i, j;
            srfTriangles_s tri;
            modelSurface_s surf = new modelSurface_s();

            src.ExpectTokenString("{");

            // parse the name
            src.ExpectAnyToken(token);

            model = renderModelManager.AllocModel();
            model.InitEmpty(token.toString());

            int numSurfaces = src.ParseInt();
            if (numSurfaces < 0) {
                src.Error("R_ParseModel: bad numSurfaces");
            }

            for (i = 0; i < numSurfaces; i++) {
                src.ExpectTokenString("{");

                src.ExpectAnyToken(token);

                surf.shader = declManager.FindMaterial(token);

                ((idMaterial) surf.shader).AddReference();

                tri = R_AllocStaticTriSurf();
                surf.geometry = tri;

                tri.numVerts = src.ParseInt();
                tri.numIndexes = src.ParseInt();

                R_AllocStaticTriSurfVerts(tri, tri.numVerts);
                for (j = 0; j < tri.numVerts; j++) {
                    float[] vec = new float[8];

                    src.Parse1DMatrix(8, vec);

                    tri.verts[j].xyz.oSet(0, vec[0]);
                    tri.verts[j].xyz.oSet(1, vec[1]);
                    tri.verts[j].xyz.oSet(2, vec[2]);
                    tri.verts[j].st.oSet(0, vec[3]);
                    tri.verts[j].st.oSet(1, vec[4]);
                    tri.verts[j].normal.oSet(0, vec[5]);
                    tri.verts[j].normal.oSet(1, vec[6]);
                    tri.verts[j].normal.oSet(2, vec[7]);
                }

                R_AllocStaticTriSurfIndexes(tri, tri.numIndexes);
                for (j = 0; j < tri.numIndexes; j++) {
                    tri.indexes[j] = src.ParseInt();
                }
                src.ExpectTokenString("}");

                // add the completed surface to the model
                model.AddSurface(surf);
            }

            src.ExpectTokenString("}");

            model.FinishSurfaces();

            return model;
        }

        public idRenderModel ParseShadowModel(idLexer src) throws idException {
            idRenderModel model;
            idToken token = new idToken();
            int j;
            srfTriangles_s tri;
            modelSurface_s surf = new modelSurface_s();

            src.ExpectTokenString("{");

            // parse the name
            src.ExpectAnyToken(token);

            model = renderModelManager.AllocModel();
            model.InitEmpty(token.toString());

            surf.shader = tr.defaultMaterial;

            tri = R_AllocStaticTriSurf();
            surf.geometry = tri;

            tri.numVerts = src.ParseInt();
            tri.numShadowIndexesNoCaps = src.ParseInt();
            tri.numShadowIndexesNoFrontCaps = src.ParseInt();
            tri.numIndexes = src.ParseInt();
            tri.shadowCapPlaneBits = src.ParseInt();

            R_AllocStaticTriSurfShadowVerts(tri, tri.numVerts);
            tri.bounds.Clear();
            tri.shadowVertexes = new Model.shadowCache_s[tri.numVerts];
            for (j = 0; j < tri.numVerts; j++) {
                float[] vec = new float[8];

                src.Parse1DMatrix(3, vec);
                tri.shadowVertexes[j] = new shadowCache_s();
                tri.shadowVertexes[j].xyz.oSet(0, vec[0]);
                tri.shadowVertexes[j].xyz.oSet(1, vec[1]);
                tri.shadowVertexes[j].xyz.oSet(2, vec[2]);
                tri.shadowVertexes[j].xyz.oSet(3, 1);// no homogenous value

                tri.bounds.AddPoint(tri.shadowVertexes[j].xyz.ToVec3());
                int a = 0;
            }

            R_AllocStaticTriSurfIndexes(tri, tri.numIndexes);
            for (j = 0; j < tri.numIndexes; j++) {
                tri.indexes[j] = src.ParseInt();
            }

            // add the completed surface to the model
            model.AddSurface(surf);

            src.ExpectTokenString("}");

            // we do NOT do a model.FinishSurfaceces, because we don't need sil edges, planes, tangents, etc.
//	model.FinishSurfaces();
            return model;
        }

        public void SetupAreaRefs() {
            int i;

            connectedAreaNum = 0;
            for (i = 0; i < numPortalAreas; i++) {
                portalAreas[i].areaNum = i;
                portalAreas[i].lightRefs.areaNext
                        = portalAreas[i].lightRefs.areaPrev
                        = portalAreas[i].lightRefs;
                portalAreas[i].entityRefs.areaNext
                        = portalAreas[i].entityRefs.areaPrev
                        = portalAreas[i].entityRefs;
            }
        }

        public void ParseInterAreaPortals(idLexer src) throws idException {
            int i, j;

            src.ExpectTokenString("{");

            numPortalAreas = src.ParseInt();
            if (numPortalAreas < 0) {
                src.Error("R_ParseInterAreaPortals: bad numPortalAreas");
                return;
            }
            portalAreas = portalArea_s.generateArray(numPortalAreas);
            areaScreenRect = idScreenRect.generateArray(numPortalAreas);

            // set the doubly linked lists
            SetupAreaRefs();

            numInterAreaPortals = src.ParseInt();
            if (numInterAreaPortals < 0) {
                src.Error("R_ParseInterAreaPortals: bad numInterAreaPortals");
                return;
            }

            doublePortals = TempDump.allocArray(doublePortal_s.class, numInterAreaPortals);

            for (i = 0; i < numInterAreaPortals; i++) {
                int numPoints, a1, a2;
                idWinding w;
                portal_s p;

                numPoints = src.ParseInt();
                a1 = src.ParseInt();
                a2 = src.ParseInt();

                w = new idWinding(numPoints);
                w.SetNumPoints(numPoints);
                for (j = 0; j < numPoints; j++) {
                    src.Parse1DMatrix(3, w.oGet(j));
                    // no texture coordinates
                    w.oGet(j).oSet(3, 0);
                    w.oGet(j).oSet(4, 0);
                }

                // add the portal to a1
                p = new portal_s();// R_ClearedStaticAlloc(sizeof(p));
                p.intoArea = a2;
                p.doublePortal = doublePortals[i];
                p.w = w;
                p.w.GetPlane(p.plane);

                p.next = portalAreas[a1].portals;
                portalAreas[a1].portals = p;

                doublePortals[i].portals[0] = p;

                // reverse it for a2
                p = new portal_s();// R_ClearedStaticAlloc(sizeof(p));
                p.intoArea = a1;
                p.doublePortal = doublePortals[i];
                p.w = w.Reverse();
                p.w.GetPlane(p.plane);

                p.next = portalAreas[a2].portals;
                portalAreas[a2].portals = p;

                doublePortals[i].portals[1] = p;
            }

            src.ExpectTokenString("}");
        }

        public void ParseNodes(idLexer src) throws idException {

            src.ExpectTokenString("{");

            numAreaNodes = src.ParseInt();
            if (numAreaNodes < 0) {
                src.Error("R_ParseNodes: bad numAreaNodes");
            }
            areaNodes = TempDump.allocArray(areaNode_t.class, numAreaNodes);

            for (areaNode_t node : areaNodes) {
                src.Parse1DMatrix(4, node.plane);
                node.children[0] = src.ParseInt();
                node.children[1] = src.ParseInt();
            }

            src.ExpectTokenString("}");
        }

        public int CommonChildrenArea_r(areaNode_t node) {
            final int[] nums = new int[2];

            for (int i = 0; i < 2; i++) {
                if (node.children[i] <= 0) {
                    nums[i] = -1 - node.children[i];
                } else {
                    nums[i] = CommonChildrenArea_r(areaNodes[node.children[i]]);
                }
            }

            // solid nodes will match any area
            if (nums[0] == AREANUM_SOLID) {
                nums[0] = nums[1];
            }
            if (nums[1] == AREANUM_SOLID) {
                nums[1] = nums[0];
            }

            int common;
            if (nums[0] == nums[1]) {
                common = nums[0];
            } else {
                common = CHILDREN_HAVE_MULTIPLE_AREAS;
            }

            node.commonChildrenArea = common;

            return common;
        }

        public void FreeWorld() {
            int i;

            // this will free all the lightDefs and entityDefs
            FreeDefs();

            // free all the portals and check light/model references
            for (i = 0; i < numPortalAreas; i++) {
                portalArea_s area;
                portal_s portal, nextPortal;

                area = portalAreas[i];
                for (portal = area.portals; portal != null; portal = nextPortal) {//TODO:linkage?
                    nextPortal = portal.next;
//			delete portal.w;
                    portal.w = null;
//                    R_StaticFree(portal);
                }

                // there shouldn't be any remaining lightRefs or entityRefs
                if (area.lightRefs.areaNext != area.lightRefs) {
                    common.Error("FreeWorld: unexpected remaining lightRefs");
                }
                if (area.entityRefs.areaNext != area.entityRefs) {
                    common.Error("FreeWorld: unexpected remaining entityRefs");
                }
            }

            if (portalAreas != null) {
//                R_StaticFree(portalAreas);
                portalAreas = null;
                numPortalAreas = 0;
//                R_StaticFree(areaScreenRect);
                areaScreenRect = null;
            }

            if (doublePortals != null) {
//                R_StaticFree(doublePortals);
                doublePortals = null;
                numInterAreaPortals = 0;
            }

            if (areaNodes != null) {
//                R_StaticFree(areaNodes);
                areaNodes = null;
            }

            // free all the inline idRenderModels 
            for (i = 0; i < localModels.Num(); i++) {
                renderModelManager.RemoveModel(localModels.oGet(i));
                localModels.RemoveIndex(i);
//		delete localModels[i];
            }
            localModels.Clear();

//            areaReferenceAllocator.Shutdown();
//            interactionAllocator.Shutdown();
//            areaNumRefAllocator.Shutdown();
            mapName.oSet("<FREED>");
        }

        /*
         =================
         idRenderWorldLocal::ClearWorld

         Sets up for a single area world
         =================
         */
        public void ClearWorld() {
            numPortalAreas = 1;
            portalAreas = portalArea_s.generateArray(1);
            areaScreenRect = idScreenRect.generateArray(1);

            SetupAreaRefs();

            // even though we only have a single area, create a node
            // that has both children pointing at it so we don't need to
            //
            areaNodes = new areaNode_t[]{new areaNode_t()};// R_ClearedStaticAlloc(sizeof(areaNodes[0]));
            areaNodes[0].plane.oSet(3, 1);
            areaNodes[0].children[0] = -1;
            areaNodes[0].children[1] = -1;
        }

        /*
         =================
         idRenderWorldLocal::FreeDefs

         dump all the interactions
         =================
         */
        public void FreeDefs() {
            int i;

            generateAllInteractionsCalled = false;

            if (interactionTable != null) {
//                R_StaticFree(interactionTable);
                interactionTable = null;
            }

            // free all lightDefs
            for (i = 0; i < lightDefs.Num(); i++) {
                idRenderLightLocal light;

                light = lightDefs.oGet(i);
                if (light != null && light.world.equals(this)) {
                    FreeLightDef(i);
                    lightDefs.oSet(i, null);
                }
            }

            // free all entityDefs
            for (i = 0; i < entityDefs.Num(); i++) {
                idRenderEntityLocal mod;

                mod = entityDefs.oGet(i);
                if (mod != null && mod.world.equals(this)) {
                    FreeEntityDef(i);
                    entityDefs.oSet(i, null);
                }
            }
        }

        public void TouchWorldModels() {
            int i;

            for (i = 0; i < localModels.Num(); i++) {
                renderModelManager.CheckModel(localModels.oGet(i).Name());
            }
        }

        public void AddWorldModelEntities() {
            int i;

            // add the world model for each portal area
            // we can't just call AddEntityDef, because that would place the references
            // based on the bounding box, rather than explicitly into the correct area
            for (i = 0; i < numPortalAreas; i++) {
                idRenderEntityLocal def;
                int index;

                def = new idRenderEntityLocal();

                // try and reuse a free spot
                index = entityDefs.FindNull();
                if (index == -1) {
                    index = entityDefs.Append(def);
                } else {
                    entityDefs.oSet(index, def);
                }

                def.index = index;
                def.world = this;

                def.parms.hModel = renderModelManager.FindModel(va("_area%d", i));
                if (def.parms.hModel.IsDefaultModel() || !def.parms.hModel.IsStaticWorldModel()) {
                    common.Error("idRenderWorldLocal::InitFromMap: bad area model lookup");
                }

                idRenderModel hModel = def.parms.hModel;

                for (int j = 0; j < hModel.NumSurfaces(); j++) {
                    final modelSurface_s surf = hModel.Surface(j);

                    if ("textures/smf/portal_sky".equals(surf.shader.GetName())) {
                        def.needsPortalSky = true;
                    }
                }

                def.referenceBounds.oSet(def.parms.hModel.Bounds());

                def.parms.axis.oSet(0, 0, 1);
                def.parms.axis.oSet(1, 1, 1);
                def.parms.axis.oSet(2, 2, 1);

                R_AxisToModelMatrix(def.parms.axis, def.parms.origin, def.modelMatrix);

                // in case an explicit shader is used on the world, we don't
                // want it to have a 0 alpha or color
                def.parms.shaderParms[0]
                        = def.parms.shaderParms[1]
                        = def.parms.shaderParms[2]
                        = def.parms.shaderParms[3] = 1;

                AddEntityRefToArea(def, portalAreas[i]);
            }
        }

        public void ClearPortalStates() {
            int i, j;

            // all portals start off open
            for (i = 0; i < numInterAreaPortals; i++) {
                doublePortals[i].blockingBits = PS_BLOCK_NONE.ordinal();
            }

            // flood fill all area connections
            for (i = 0; i < numPortalAreas; i++) {
                for (j = 0; j < NUM_PORTAL_ATTRIBUTES; j++) {
                    connectedAreaNum++;
                    FloodConnectedAreas(portalAreas[i], j);
                }
            }
        }

        /*
         =================
         idRenderWorldLocal::InitFromMap

         A NULL or empty name will make a world without a map model, which
         is still useful for displaying a bare model
         =================
         */
        @Override
        public boolean InitFromMap(final String name) throws idException {
            idLexer src;
            idToken token = new idToken();
            idStr filename;
            idRenderModel lastModel;

            // if this is an empty world, initialize manually
            if (null == name || name.isEmpty()) {
                FreeWorld();
                mapName.Clear();
                ClearWorld();
                return true;
            }

            // load it
            filename = new idStr(name);
            filename.SetFileExtension(PROC_FILE_EXT);

            // if we are reloading the same map, check the timestamp
            // and try to skip all the work
            final long[] currentTimeStamp = new long[1];
            fileSystem.ReadFile(filename.toString(), null, currentTimeStamp);

            if (mapName.equals(name)) {
                if (currentTimeStamp[0] != FILE_NOT_FOUND_TIMESTAMP && currentTimeStamp[0] == mapTimeStamp[0]) {
                    common.Printf("idRenderWorldLocal::InitFromMap: retaining existing map\n");
                    FreeDefs();
                    TouchWorldModels();
                    AddWorldModelEntities();
                    ClearPortalStates();
                    return true;
                }
                common.Printf("idRenderWorldLocal::InitFromMap: timestamp has changed, reloading.\n");
            }

            FreeWorld();

            src = new idLexer(filename.toString(), LEXFL_NOSTRINGCONCAT | LEXFL_NODOLLARPRECOMPILE);
            if (!src.IsLoaded()) {
                common.Printf("idRenderWorldLocal::InitFromMap: %s not found\n", filename);
                ClearWorld();
                return false;
            }

            mapName = new idStr(name);
            mapTimeStamp[0] = currentTimeStamp[0];

            // if we are writing a demo, archive the load command
            if (session.writeDemo != null) {
                WriteLoadMap();
            }

            if (!src.ReadToken(token) || token.Icmp(PROC_FILE_ID) != 0) {
                common.Printf("idRenderWorldLocal::InitFromMap: bad id '%s' instead of '%s'\n", token, PROC_FILE_ID);
//		delete src;
                return false;
            }

            // parse the file
            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("model")) {
                    lastModel = ParseModel(src);

                    // add it to the model manager list
                    renderModelManager.AddModel(lastModel);

                    // save it in the list to free when clearing this map
                    localModels.Append(lastModel);
                    continue;
                }

                if (token.equals("shadowModel")) {
                    lastModel = ParseShadowModel(src);

                    // add it to the model manager list
                    renderModelManager.AddModel(lastModel);

                    // save it in the list to free when clearing this map
                    localModels.Append(lastModel);
                    continue;
                }

                if (token.equals("interAreaPortals")) {
                    ParseInterAreaPortals(src);
                    continue;
                }

                if (token.equals("nodes")) {
                    ParseNodes(src);
                    continue;
                }

                src.Error("idRenderWorldLocal::InitFromMap: bad token \"%s\"", token);
            }

//	delete src;
            // if it was a trivial map without any areas, create a single area
            if (0 == numPortalAreas) {
                ClearWorld();
            }

            // find the points where we can early-our of reference pushing into the BSP tree
            CommonChildrenArea_r(areaNodes[0]);

            AddWorldModelEntities();
            ClearPortalStates();

            // done!
            return true;
        }
        //--------------------------
        // RenderWorld_portals.cpp

        public idScreenRect ScreenRectFromWinding(final idWinding w, viewEntity_s space) {
            idScreenRect r = new idScreenRect();
            int i;
            idVec3 v;
            idVec3 ndc = new idVec3();
            float windowX, windowY;

            r.Clear();
            for (i = 0; i < w.GetNumPoints(); i++) {
                v = R_LocalPointToGlobal(space.modelMatrix, w.oGet(i).ToVec3());
                R_GlobalToNormalizedDeviceCoordinates(v, ndc);

                windowX = 0.5f * (1.0f + ndc.oGet(0)) * (tr.viewDef.viewport.x2 - tr.viewDef.viewport.x1);
                windowY = 0.5f * (1.0f + ndc.oGet(1)) * (tr.viewDef.viewport.y2 - tr.viewDef.viewport.y1);

                r.AddPoint(windowX, windowY);
            }

            r.Expand();

            return r;
        }

        public boolean PortalIsFoggedOut(final portal_s p) {
            idRenderLightLocal ldef;
            final idWinding w;
            int i;
            idPlane forward = new idPlane();

            ldef = p.doublePortal.fogLight;
            if (NOT(ldef)) {
                return false;
            }

            // find the current density of the fog
            final idMaterial lightShader = ldef.lightShader;
            int size = lightShader.GetNumRegisters();
            float[] regs = new float[size];

            lightShader.EvaluateRegisters(regs, ldef.parms.shaderParms, tr.viewDef, ldef.parms.referenceSound);

            final shaderStage_t stage = lightShader.GetStage(0);

            float alpha = regs[stage.color.registers[3]];

            // if they left the default value on, set a fog distance of 500
            float a;

            if (alpha <= 1.0f) {
                a = -0.5f / DEFAULT_FOG_DISTANCE;
            } else {
                // otherwise, distance = alpha color
                a = -0.5f / alpha;
            }

            forward.oSet(0, a * tr.viewDef.worldSpace.modelViewMatrix[2]);
            forward.oSet(1, a * tr.viewDef.worldSpace.modelViewMatrix[6]);
            forward.oSet(2, a * tr.viewDef.worldSpace.modelViewMatrix[10]);
            forward.oSet(3, a * tr.viewDef.worldSpace.modelViewMatrix[14]);

            w = p.w;
            for (i = 0; i < w.GetNumPoints(); i++) {
                float d;

                d = forward.Distance(w.oGet(i).ToVec3());
                if (d < 0.5f) {
                    return false;		// a point not clipped off
                }
            }

            return true;
        }

        public void FloodViewThroughArea_r(final idVec3 origin, int areaNum, final portalStack_s ps) {
            portal_s p;
            float d;
            portalArea_s area;
            portalStack_s check;
            portalStack_s newStack = new portalStack_s();
            int i, j;
            idVec3 v1, v2;
            int addPlanes;
            idFixedWinding w;		// we won't overflow because MAX_PORTAL_PLANES = 20

            area = portalAreas[areaNum];

            // cull models and lights to the current collection of planes
            AddAreaRefs(areaNum, ps);

            if (areaScreenRect[areaNum].IsEmpty()) {
                areaScreenRect[areaNum] = new idScreenRect(ps.rect);
            } else {
                areaScreenRect[areaNum].Union(ps.rect);
            }

            // go through all the portals
            for (p = area.portals; p != null; p = p.next) {
                // an enclosing door may have sealed the portal off
                if ((p.doublePortal.blockingBits & PS_BLOCK_VIEW.ordinal()) != 0) {
                    continue;
                }

                // make sure this portal is facing away from the view
                d = p.plane.Distance(origin);
                if (d < -0.1f) {
                    continue;
                }

                // make sure the portal isn't in our stack trace,
                // which would cause an infinite loop
                for (check = ps; check != null; check = check.next) {
                    if (p.equals(check.p)) {
                        break;		// don't recursively enter a stack
                    }
                }
                if (check != null) {
                    continue;	// already in stack
                }

                // if we are very close to the portal surface, don't bother clipping
                // it, which tends to give epsilon problems that make the area vanish
                if (d < 1.0f) {

                    // go through this portal
                    newStack = new portalStack_s(ps);
                    newStack.p = p;
                    newStack.next = ps;
                    FloodViewThroughArea_r(origin, p.intoArea, newStack);
                    continue;
                }

                // clip the portal winding to all of the planes
                w = new idFixedWinding(p.w);
                for (j = 0; j < ps.numPortalPlanes; j++) {
                    if (!w.ClipInPlace(ps.portalPlanes[j].oNegative(), 0)) {
                        break;
                    }
                }
                if (0 == w.GetNumPoints()) {
                    continue;	// portal not visible
                }

                // see if it is fogged out
                if (PortalIsFoggedOut(p)) {
                    continue;
                }

                // go through this portal
                newStack.p = p;
                newStack.next = ps;

                // find the screen pixel bounding box of the remaining portal
                // so we can scissor things outside it
                newStack.rect = ScreenRectFromWinding(w, tr.identitySpace);

                // slop might have spread it a pixel outside, so trim it back
                newStack.rect.Intersect(ps.rect);

                // generate a set of clipping planes that will further restrict
                // the visible view beyond just the scissor rect
                addPlanes = w.GetNumPoints();
                if (addPlanes > MAX_PORTAL_PLANES) {
                    addPlanes = MAX_PORTAL_PLANES;
                }

                newStack.numPortalPlanes = 0;
                for (i = 0; i < addPlanes; i++) {
                    j = i + 1;
                    if (j == w.GetNumPoints()) {
                        j = 0;
                    }

                    v1 = origin.oMinus(w.oGet(i).ToVec3());
                    v2 = origin.oMinus(w.oGet(j).ToVec3());

                    newStack.portalPlanes[newStack.numPortalPlanes].Normal().Cross(v2, v1);

                    // if it is degenerate, skip the plane
                    if (newStack.portalPlanes[newStack.numPortalPlanes].Normalize() < 0.01f) {
                        continue;
                    }
                    newStack.portalPlanes[newStack.numPortalPlanes].FitThroughPoint(origin);

                    newStack.numPortalPlanes++;
                }

                // the last stack plane is the portal plane
                newStack.portalPlanes[newStack.numPortalPlanes] = new idPlane(p.plane);
                newStack.numPortalPlanes++;

                FloodViewThroughArea_r(origin, p.intoArea, newStack);
            }
        }

        /*
         =======================
         FlowViewThroughPortals

         Finds viewLights and viewEntities by flowing from an origin through the visible portals.
         origin point can see into.  The planes array defines a volume (positive
         sides facing in) that should contain the origin, such as a view frustum or a point light box.
         Zero planes assumes an unbounded volume.
         =======================
         */
        public void FlowViewThroughPortals(final idVec3 origin, int numPlanes, final idPlane[] planes) {
            portalStack_s ps = new portalStack_s();
            int i;

            ps.next = null;
            ps.p = null;

            for (i = 0; i < numPlanes; i++) {
                ps.portalPlanes[i] = new idPlane(planes[i]);
            }

            ps.numPortalPlanes = numPlanes;
            ps.rect = new idScreenRect(tr.viewDef.scissor);

            if (tr.viewDef.areaNum < 0) {

                for (i = 0; i < numPortalAreas; i++) {
                    areaScreenRect[i] = new idScreenRect(tr.viewDef.scissor);//TODO:copy constructor?
                }

                // if outside the world, mark everything
                for (i = 0; i < numPortalAreas; i++) {
                    AddAreaRefs(i, ps);
                }
            } else {

                for (i = 0; i < numPortalAreas; i++) {
                    areaScreenRect[i].Clear();
                }

                // flood out through portals, setting area viewCount
                FloodViewThroughArea_r(origin, tr.viewDef.areaNum, ps);
            }
        }

        public void FloodLightThroughArea_r(idRenderLightLocal light, int areaNum, final portalStack_s ps) {
            portal_s p;
            float d;
            portalArea_s area;
            portalStack_s check, firstPortalStack = new portalStack_s();
            portalStack_s newStack = new portalStack_s();
            int i, j;
            idVec3 v1, v2;
            int addPlanes;
            idFixedWinding w;		// we won't overflow because MAX_PORTAL_PLANES = 20

            area = portalAreas[areaNum];

            // add an areaRef
            AddLightRefToArea(light, area);

            // go through all the portals
            for (p = area.portals; p != null; p = p.next) {
                // make sure this portal is facing away from the view
                d = p.plane.Distance(light.globalLightOrigin);
                if (d < -0.1f) {
                    continue;
                }

                // make sure the portal isn't in our stack trace,
                // which would cause an infinite loop
                for (check = ps; check != null; check = check.next) {
                    firstPortalStack = check;
                    if (check.p == p) {
                        break;		// don't recursively enter a stack
                    }
                }
                if (check != null) {
                    continue;	// already in stack
                }

                // if we are very close to the portal surface, don't bother clipping
                // it, which tends to give epsilon problems that make the area vanish
                if (d < 1.0f) {
                    // go through this portal
                    newStack = new portalStack_s(ps);
                    newStack.p = p;
                    newStack.next = ps;
                    FloodLightThroughArea_r(light, p.intoArea, newStack);
                    continue;
                }

                // clip the portal winding to all of the planes
                w = new idFixedWinding(p.w);
                for (j = 0; j < ps.numPortalPlanes; j++) {
                    if (!w.ClipInPlace(ps.portalPlanes[j].oNegative(), 0)) {
                        break;
                    }
                }
                if (0 == w.GetNumPoints()) {
                    continue;	// portal not visible
                }
                // also always clip to the original light planes, because they aren't
                // necessarily extending to infinitiy like a view frustum
                for (j = 0; j < firstPortalStack.numPortalPlanes; j++) {
                    if (!w.ClipInPlace(firstPortalStack.portalPlanes[j].oNegative(), 0)) {
                        break;
                    }
                }
                if (0 == w.GetNumPoints()) {
                    continue;	// portal not visible
                }

                // go through this portal
                newStack.p = p;
                newStack.next = ps;

                // generate a set of clipping planes that will further restrict
                // the visible view beyond just the scissor rect
                addPlanes = w.GetNumPoints();
                if (addPlanes > MAX_PORTAL_PLANES) {
                    addPlanes = MAX_PORTAL_PLANES;
                }

                newStack.numPortalPlanes = 0;
                for (i = 0; i < addPlanes; i++) {
                    j = i + 1;
                    if (j == w.GetNumPoints()) {
                        j = 0;
                    }

                    v1 = light.globalLightOrigin.oMinus(w.oGet(i).ToVec3());
                    v2 = light.globalLightOrigin.oMinus(w.oGet(j).ToVec3());

                    newStack.portalPlanes[newStack.numPortalPlanes].Normal().Cross(v2, v1);

                    // if it is degenerate, skip the plane
                    if (newStack.portalPlanes[newStack.numPortalPlanes].Normalize() < 0.01f) {
                        continue;
                    }
                    newStack.portalPlanes[newStack.numPortalPlanes].FitThroughPoint(light.globalLightOrigin);

                    newStack.numPortalPlanes++;
                }

                FloodLightThroughArea_r(light, p.intoArea, newStack);
            }
        }

        /*
         =======================
         FlowLightThroughPortals

         Adds an arearef in each area that the light center flows into.
         This can only be used for shadow casting lights that have a generated
         prelight, because shadows are cast from back side which may not be in visible areas.
         =======================
         */
        public void FlowLightThroughPortals(idRenderLightLocal light) {
            portalStack_s ps;
            int i;
            final idVec3 origin = light.globalLightOrigin;

            // if the light origin areaNum is not in a valid area,
            // the light won't have any area refs
            if (light.areaNum == -1) {
                return;
            }

//            memset(ps, 0, sizeof(ps));
            ps = new portalStack_s();

            ps.numPortalPlanes = 6;
            for (i = 0; i < 6; i++) {
                ps.portalPlanes[i] = new idPlane(light.frustum[i]);
            }

            FloodLightThroughArea_r(light, light.areaNum, ps);
        }

        public areaNumRef_s FloodFrustumAreas_r(final idFrustum frustum, final int areaNum, final idBounds bounds, areaNumRef_s areas) {
            portal_s p;
            portalArea_s portalArea;
            idBounds newBounds = new idBounds();
            areaNumRef_s a;

            portalArea = portalAreas[areaNum];

            // go through all the portals
            for (p = portalArea.portals; p != null; p = p.next) {

                // check if we already visited the area the portal leads to
                for (a = areas; a != null; a = a.next) {
                    if (a.areaNum == p.intoArea) {
                        break;
                    }
                }
                if (a != null) {
                    continue;
                }

                // the frustum origin must be at the front of the portal plane
                if (p.plane.Side(frustum.GetOrigin(), 0.1f) == SIDE_BACK) {
                    continue;
                }

                // the frustum must cross the portal plane
                if (frustum.PlaneSide(p.plane, 0.0f) != PLANESIDE_CROSS) {
                    continue;
                }

                // get the bounds for the portal winding projected in the frustum
                frustum.ProjectionBounds(p.w, newBounds);

                newBounds.IntersectSelf(bounds);

                if (newBounds.oGet(0).oGet(0) > newBounds.oGet(1).oGet(0)
                        || newBounds.oGet(0).oGet(1) > newBounds.oGet(1).oGet(1)
                        || newBounds.oGet(0).oGet(2) > newBounds.oGet(1).oGet(2)) {
                    continue;
                }

                newBounds.oSet(1, 0, frustum.GetFarDistance());

                a = new areaNumRef_s();//areaNumRefAllocator.Alloc();
                a.areaNum = p.intoArea;
                a.next = areas;
                areas = a;

                areas = FloodFrustumAreas_r(frustum, p.intoArea, newBounds, areas);
            }

            return areas;
        }

        /*
         ===================
         idRenderWorldLocal::FloodFrustumAreas

         Retrieves all the portal areas the frustum floods into where the frustum starts in the given areas.
         All portals are assumed to be open.
         ===================
         */
        public areaNumRef_s FloodFrustumAreas(final idFrustum frustum, areaNumRef_s areas) {
            idBounds bounds = new idBounds();
            areaNumRef_s a;

            // bounds that cover the whole frustum
            bounds.oGet(0).Set(frustum.GetNearDistance(), -1.0f, -1.0f);
            bounds.oGet(1).Set(frustum.GetFarDistance(), 1.0f, 1.0f);

            for (a = areas; a != null; a = a.next) {
                areas = FloodFrustumAreas_r(frustum, a.areaNum, bounds, areas);
            }

            return areas;
        }

        /*
         ================
         CullEntityByPortals

         Return true if the entity reference bounds do not intersect the current portal chain.
         ================
         */
        public boolean CullEntityByPortals(final idRenderEntityLocal entity, final portalStack_s ps) {

            if (!r_useEntityCulling.GetBool()) {
                return false;
            }

            // try to cull the entire thing using the reference bounds.
            // we do not yet do callbacks or dynamic model creation,
            // because we want to do all touching of the model after
            // we have determined all the lights that may effect it,
            // which optimizes cache usage
            if (R_CullLocalBox(entity.referenceBounds, entity.modelMatrix, ps.numPortalPlanes, ps.portalPlanes)) {
                return true;
            }

            return false;
        }

        /*
         ===================
         AddAreaEntityRefs

         Any models that are visible through the current portalStack will
         have their scissor 
         ===================
         */
        public void AddAreaEntityRefs(int areaNum, final portalStack_s ps) {
            areaReference_s ref;
            idRenderEntityLocal entity;
            portalArea_s area;
            viewEntity_s vEnt;
//            idBounds b;

            area = portalAreas[areaNum];

            for (ref = area.entityRefs.areaNext; ref != area.entityRefs; ref = ref.areaNext) {
                entity = ref.entity;

                // debug tool to allow viewing of only one entity at a time
                if (r_singleEntity.GetInteger() >= 0 && r_singleEntity.GetInteger() != entity.index) {
                    continue;
                }

                // remove decals that are completely faded away
                R_FreeEntityDefFadedDecals(entity, tr.viewDef.renderView.time);

                // check for completely suppressing the model
                if (!r_skipSuppress.GetBool()) {
                    if (entity.parms.suppressSurfaceInViewID != 0 && entity.parms.suppressSurfaceInViewID == tr.viewDef.renderView.viewID) {
                        continue;
                    }
                    if (entity.parms.allowSurfaceInViewID != 0 && entity.parms.allowSurfaceInViewID != tr.viewDef.renderView.viewID) {
                        continue;
                    }
                }

                // cull reference bounds
                if (CullEntityByPortals(entity, ps)) {
                    // we are culled out through this portal chain, but it might
                    // still be visible through others
                    continue;
                }

                vEnt = R_SetEntityDefViewEntity(entity);

                // possibly expand the scissor rect
                vEnt.scissorRect.Union(ps.rect);
            }
        }

        /*
         ================
         CullLightByPortals

         Return true if the light frustum does not intersect the current portal chain.
         The last stack plane is not used because lights are not near clipped.
         ================
         */
        public boolean CullLightByPortals(final idRenderLightLocal light, final portalStack_s ps) {
            int i, j;
            srfTriangles_s tri;
            float d;
            idFixedWinding w = new idFixedWinding();		// we won't overflow because MAX_PORTAL_PLANES = 20

            if (r_useLightCulling.GetInteger() == 0) {
                return false;
            }

            if (r_useLightCulling.GetInteger() >= 2) {
                // exact clip of light faces against all planes
                for (i = 0; i < 6; i++) {
                    // the light frustum planes face out from the light,
                    // so the planes that have the view origin on the negative
                    // side will be the "back" faces of the light, which must have
                    // some fragment inside the portalStack to be visible
                    if (light.frustum[i].Distance(tr.viewDef.renderView.vieworg) >= 0) {
                        continue;
                    }

                    // get the exact winding for this side
                    final idWinding ow = light.frustumWindings[i];

                    // projected lights may have one of the frustums degenerated
                    if (null == ow) {
                        continue;
                    }

                    w.oSet(ow);

                    // now check the winding against each of the portalStack planes
                    for (j = 0; j < ps.numPortalPlanes - 1; j++) {
                        if (!w.ClipInPlace(ps.portalPlanes[j].oNegative())) {
                            break;
                        }
                    }

                    if (w.GetNumPoints() != 0) {
                        // part of the winding is visible through the portalStack,
                        // so the light is not culled
                        return false;
                    }
                }
                // none of the light surfaces were visible
                return true;

            } else {

                // simple point check against each plane
                tri = light.frustumTris;

                // check against frustum planes
                for (i = 0; i < ps.numPortalPlanes - 1; i++) {
                    for (j = 0; j < tri.numVerts; j++) {
                        d = ps.portalPlanes[i].Distance(tri.verts[j].xyz);
                        if (d < 0.0f) {
                            break;	// point is inside this plane
                        }
                    }
                    if (j == tri.numVerts) {
                        // all points were outside one of the planes
                        tr.pc.c_box_cull_out++;
                        return true;
                    }
                }
            }

            return false;
        }

        /*
         ===================
         AddAreaLightRefs

         This is the only point where lights get added to the viewLights list
         ===================
         */ static int DEBUG_AddAreaLightRefs = 0;

        public void AddAreaLightRefs(int areaNum, final portalStack_s ps) {
            areaReference_s lref;
            portalArea_s area;
            idRenderLightLocal light;
            viewLight_s vLight;
            DEBUG_AddAreaLightRefs++;

            area = portalAreas[areaNum];

            for (lref = area.lightRefs.areaNext; lref != area.lightRefs; lref = lref.areaNext) {
                light = lref.light;

                // debug tool to allow viewing of only one light at a time
                if (r_singleLight.GetInteger() >= 0 && r_singleLight.GetInteger() != light.index) {
                    continue;
                }

                // check for being closed off behind a door
                // a light that doesn't cast shadows will still light even if it is behind a door
                if (r_useLightCulling.GetInteger() >= 3
                        && !light.parms.noShadows && light.lightShader.LightCastsShadows()
                        && light.areaNum != -1 && !tr.viewDef.connectedAreas[light.areaNum]) {
                    continue;
                }

                // cull frustum
                if (CullLightByPortals(light, ps)) {
                    // we are culled out through this portal chain, but it might
                    // still be visible through others
                    continue;
                }

                vLight = R_SetLightDefViewLight(light);

                // expand the scissor rect
                vLight.scissorRect.Union(ps.rect);
            }
        }

        /*
         ===================
         AddAreaRefs

         This may be entered multiple times with different planes
         if more than one portal sees into the area
         ===================
         */
        public void AddAreaRefs(int areaNum, final portalStack_s ps) {
            // mark the viewCount, so r_showPortals can display the
            // considered portals
            portalAreas[areaNum].viewCount = tr.viewCount;

            // add the models and lights, using more precise culling to the planes
            AddAreaEntityRefs(areaNum, ps);
            AddAreaLightRefs(areaNum, ps);
        }

        public void BuildConnectedAreas_r(int areaNum) {
            portalArea_s area;
            portal_s portal;

            if (tr.viewDef.connectedAreas[areaNum]) {
                return;
            }

            tr.viewDef.connectedAreas[areaNum] = true;

            // flood through all non-blocked portals
            area = portalAreas[areaNum];
            for (portal = area.portals; portal != null; portal = portal.next) {
                if (0 == (portal.doublePortal.blockingBits & PS_BLOCK_VIEW.ordinal())) {
                    BuildConnectedAreas_r(portal.intoArea);
                }
            }
        }

        /*
         ===================
         BuildConnectedAreas

         This is only valid for a given view, not all views in a frame
         ===================
         */
        public void BuildConnectedAreas() {
            int i;

            tr.viewDef.connectedAreas = new boolean[numPortalAreas];

            // if we are outside the world, we can see all areas
            if (tr.viewDef.areaNum == -1) {
                Arrays.fill(tr.viewDef.connectedAreas, true);
                return;
            }

            // start with none visible, and flood fill from the current area
//            memset(tr.viewDef.connectedAreas, 0, numPortalAreas);
            tr.viewDef.connectedAreas = new boolean[numPortalAreas];
            BuildConnectedAreas_r(tr.viewDef.areaNum);
        }

        /*
         =============
         FindViewLightsAndEntites

         All the modelrefs and lightrefs that are in visible areas
         will have viewEntitys and viewLights created for them.

         The scissorRects on the viewEntitys and viewLights may be empty if
         they were considered, but not actually visible.
         =============
         */
        private static int lastPrintedAreaNum;

        public void FindViewLightsAndEntities() {
            // clear the visible lightDef and entityDef lists
            tr.viewDef.viewLights = null;
            tr.viewDef.viewEntitys = null;
            tr.viewDef.numViewEntitys = 0;

            // find the area to start the portal flooding in
            if (!r_usePortals.GetBool()) {
                // debug tool to force no portal culling
                tr.viewDef.areaNum = -1;
            } else {
                tr.viewDef.areaNum = PointInArea(tr.viewDef.initialViewAreaOrigin);
            }

            // determine all possible connected areas for
            // light-behind-door culling
            BuildConnectedAreas();

            // bump the view count, invalidating all
            // visible areas
            tr.viewCount++;
//            System.out.println("tr.viewCount::FindViewLightsAndEntities");
            tr.DBG_viewCount++;

            // flow through all the portals and add models / lights
            if (r_singleArea.GetBool()) {
                // if debugging, only mark this area
                // if we are outside the world, don't draw anything
                if (tr.viewDef.areaNum >= 0) {
                    portalStack_s ps = new portalStack_s();
                    int i;

                    if (tr.viewDef.areaNum != lastPrintedAreaNum) {
                        lastPrintedAreaNum = tr.viewDef.areaNum;
                        common.Printf("entering portal area %d\n", tr.viewDef.areaNum);
                    }

                    for (i = 0; i < 5; i++) {
                        ps.portalPlanes[i] = new idPlane(tr.viewDef.frustum[i]);
                    }
                    ps.numPortalPlanes = 5;
                    ps.rect = new idScreenRect(tr.viewDef.scissor);

                    AddAreaRefs(tr.viewDef.areaNum, ps);
                }
            } else {
                // note that the center of projection for flowing through portals may
                // be a different point than initialViewAreaOrigin for subviews that
                // may have the viewOrigin in a solid/invalid area
                FlowViewThroughPortals(tr.viewDef.renderView.vieworg, 5, tr.viewDef.frustum);
            }
        }

        @Override
        public int NumPortals() {
            return numInterAreaPortals;
        }

        /*
         ==============
         FindPortal

         Game code uses this to identify which portals are inside doors.
         Returns 0 if no portal contacts the bounds
         ==============
         */
        @Override
        public int/*qhandle_t*/ FindPortal(final idBounds b) {
            int i, j;
            idBounds wb = new idBounds();
            doublePortal_s portal;
            idWinding w;

            for (i = 0; i < numInterAreaPortals; i++) {
                portal = doublePortals[i];
                w = portal.portals[0].w;

                wb.Clear();
                for (j = 0; j < w.GetNumPoints(); j++) {
                    wb.AddPoint(w.oGet(j).ToVec3());
                }
                if (wb.IntersectsBounds(b)) {
                    return i + 1;
                }
            }

            return 0;
        }

        /*
         ==============
         SetPortalState

         doors explicitly close off portals when shut
         ==============
         */
        @Override
        public void SetPortalState( /*qhandle_t*/int portal, int blockTypes) {
            if (portal == 0) {
                return;
            }

            if (portal < 1 || portal > numInterAreaPortals) {
                common.Error("SetPortalState: bad portal number %d", portal);
            }
            int old = doublePortals[portal - 1].blockingBits;
            if (old == blockTypes) {
                return;
            }
            doublePortals[portal - 1].blockingBits = blockTypes;

            // leave the connectedAreaGroup the same on one side,
            // then flood fill from the other side with a new number for each changed attribute
            for (int i = 0; i < NUM_PORTAL_ATTRIBUTES; i++) {
                if (((old ^ blockTypes) & (1 << i)) != 0) {
                    connectedAreaNum++;
                    FloodConnectedAreas(portalAreas[doublePortals[portal - 1].portals[1].intoArea], i);
                }
            }

            if (session.writeDemo != null) {
                session.writeDemo.WriteInt(DS_RENDER);
                session.writeDemo.WriteInt(DC_SET_PORTAL_STATE);
                session.writeDemo.WriteInt(portal);
                session.writeDemo.WriteInt(blockTypes);
            }
        }

        @Override
        public int GetPortalState(int/*qhandle_t */ portal) {
            if (portal == 0) {
                return 0;
            }

            if (portal < 1 || portal > numInterAreaPortals) {
                common.Error("GetPortalState: bad portal number %d", portal);
            }

            return doublePortals[portal - 1].blockingBits;
        }

        @Override
        public boolean AreasAreConnected(int areaNum1, int areaNum2, portalConnection_t connection) {
            if (areaNum1 == -1 || areaNum2 == -1) {
                return false;
            }
            if (areaNum1 > numPortalAreas || areaNum2 > numPortalAreas || areaNum1 < 0 || areaNum2 < 0) {
                common.Error("idRenderWorldLocal::AreAreasConnected: bad parms: %d, %d", areaNum1, areaNum2);
            }

            int attribute = 0;

            int intConnection = connection.ordinal();

            while (intConnection > 1) {
                attribute++;
                intConnection >>= 1;
            }
            if (attribute >= NUM_PORTAL_ATTRIBUTES || (1 << attribute) != connection.ordinal()) {
                common.Error("idRenderWorldLocal::AreasAreConnected: bad connection number: %d\n", connection.ordinal());
            }

            return portalAreas[areaNum1].connectedAreaNum[attribute] == portalAreas[areaNum2].connectedAreaNum[attribute];
        }

        public void FloodConnectedAreas(portalArea_s area, int portalAttributeIndex) {
            if (area.connectedAreaNum[portalAttributeIndex] == connectedAreaNum) {
                return;
            }
            area.connectedAreaNum[portalAttributeIndex] = connectedAreaNum;

            for (portal_s p = area.portals; p != null; p = p.next) {
                if (0 == (p.doublePortal.blockingBits & (1 << portalAttributeIndex))) {
                    FloodConnectedAreas(portalAreas[p.intoArea], portalAttributeIndex);
                }
            }
        }

        public idScreenRect GetAreaScreenRect(int areaNum) {
            return areaScreenRect[areaNum];
        }

        /*
         =====================
         idRenderWorldLocal::ShowPortals

         Debugging tool, won't work correctly with SMP or when mirrors are present
         =====================
         */
        public void ShowPortals() {
            int i, j;
            portalArea_s area;
            portal_s p;
            idWinding w;

            // flood out through portals, setting area viewCount
            for (i = 0; i < numPortalAreas; i++) {
                area = portalAreas[i];
                if (area.viewCount != tr.viewCount) {
                    continue;
                }
                for (p = area.portals; p != null; p = p.next) {
                    w = p.w;
                    if (null == w) {
                        continue;
                    }

                    if (portalAreas[p.intoArea].viewCount != tr.viewCount) {
                        // red = can't see
                        qglColor3f(1, 0, 0);
                    } else {
                        // green = see through
                        qglColor3f(0, 1, 0);
                    }

                    qglBegin(GL_LINE_LOOP);
                    for (j = 0; j < w.GetNumPoints(); j++) {
                        qglVertex3fv(w.oGet(j).ToFloatPtr());
                    }
                    qglEnd();
                }
            }
        }
        //===============================================================================================================

        // RenderWorld_demo.cpp
        @Override
        public void StartWritingDemo(idDemoFile demo) {
            int i;

            // FIXME: we should track the idDemoFile locally, instead of snooping into session for it
            WriteLoadMap();

            // write the door portal state
            for (i = 0; i < numInterAreaPortals; i++) {
                if (doublePortals[i].blockingBits != 0) {
                    SetPortalState(i + 1, doublePortals[i].blockingBits);
                }
            }

            // clear the archive counter on all defs
            for (i = 0; i < lightDefs.Num(); i++) {
                if (lightDefs.oGet(i) != null) {
                    lightDefs.oGet(i).archived = false;
                }
            }
            for (i = 0; i < entityDefs.Num(); i++) {
                if (entityDefs.oGet(i) != null) {
                    entityDefs.oGet(i).archived = false;
                }
            }
        }

        @Override
        public void StopWritingDemo() {
            //	writeDemo = NULL;
        }

        @Override
        public boolean ProcessDemoCommand(idDemoFile readDemo, renderView_s renderView, int[] demoTimeOffset) {
            boolean newMap = false;
            renderViewShadow viewShadow = new Atomics.renderViewShadow();

            if (null == readDemo) {
                return false;
            }

            demoCommand_t dc;
            int[] d = {0};
            int/*qhandle_t*/[] h = {0};

            if (NOT(readDemo.ReadInt(d))) {
                // a demoShot may not have an endFrame, but it is still valid
                return false;
            }

            dc = demoCommand_t.values()[d[0]];

            switch (dc) {
                case DC_LOADMAP:
                    // read the initial data
                    demoHeader_t header = new demoHeader_t();

                    readDemo.ReadInt(header.version);
                    readDemo.ReadInt(header.sizeofRenderEntity);
                    readDemo.ReadInt(header.sizeofRenderLight);
                    for (int i = 0; i < 256; i++) {
                        short[] c = {0};
                        readDemo.ReadChar(c);
                        header.mapname[i] = (char) c[0];
                    }
                    // the internal version value got replaced by DS_VERSION at toplevel
                    if (header.version[0] != 4) {
                        common.Error("Demo version mismatch.\n");
                    }

                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_LOADMAP: %s\n", header.mapname);
                    }
                    InitFromMap(ctos(header.mapname));

                    newMap = true;		// we will need to set demoTimeOffset

                    break;

                case DC_RENDERVIEW:
                    readDemo.ReadInt(viewShadow.viewID);
                    readDemo.ReadInt(viewShadow.x);
                    readDemo.ReadInt(viewShadow.y);
                    readDemo.ReadInt(viewShadow.width);
                    readDemo.ReadInt(viewShadow.height);
                    readDemo.ReadFloat(viewShadow.fov_x);
                    readDemo.ReadFloat(viewShadow.fov_y);
                    readDemo.ReadVec3(viewShadow.vieworg);
                    readDemo.ReadMat3(viewShadow.viewaxis);
                    readDemo.ReadBool(viewShadow.cramZNear);
                    readDemo.ReadBool(viewShadow.forceUpdate);
                    // binary compatibility with win32 padded structures
                    short[] tmp = new short[1];
                    readDemo.ReadChar(tmp);
                    readDemo.ReadChar(tmp);
                    readDemo.ReadInt(viewShadow.time);
                    for (int i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                        readDemo.ReadFloat(viewShadow.shaderParms[i]);
                    }

//                    if (!readDemo.ReadInt(viewShadow.globalMaterial)) {
                    if (NOT(readDemo.Read(viewShadow.globalMaterial))) {
                        return false;
                    }

                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_RENDERVIEW: %d\n", viewShadow.time);
                    }

                    // possibly change the time offset if this is from a new map
                    if (newMap && demoTimeOffset[0] != 0) {
                        demoTimeOffset[0] = viewShadow.time[0] - eventLoop.Milliseconds();
                    }

                    renderView.atomicSet(viewShadow);
                    return false;

                case DC_UPDATE_ENTITYDEF:
                    ReadRenderEntity();
                    break;
                case DC_DELETE_ENTITYDEF:
                    if (NOT(readDemo.ReadInt(h))) {
                        return false;
                    }
                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_DELETE_ENTITYDEF: %d\n", h[0]);
                    }
                    FreeEntityDef(h[0]);
                    break;
                case DC_UPDATE_LIGHTDEF:
                    ReadRenderLight();
                    break;
                case DC_DELETE_LIGHTDEF:
                    if (NOT(readDemo.ReadInt(h))) {
                        return false;
                    }
                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_DELETE_LIGHTDEF: %d\n", h[0]);
                    }
                    FreeLightDef(h[0]);
                    break;

                case DC_CAPTURE_RENDER:
                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_CAPTURE_RENDER\n");
                    }
                    renderSystem.CaptureRenderToImage(readDemo.ReadHashString());
                    break;

                case DC_CROP_RENDER:
                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_CROP_RENDER\n");
                    }
                    int[][] size = new int[3][1];
                    readDemo.ReadInt(size[0]);
                    readDemo.ReadInt(size[1]);
                    readDemo.ReadInt(size[2]);
                    renderSystem.CropRenderSize(size[0][0], size[1][0], size[2][0] != 0);
                    break;

                case DC_UNCROP_RENDER:
                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_UNCROP\n");
                    }
                    renderSystem.UnCrop();
                    break;

                case DC_GUI_MODEL:
                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_GUI_MODEL\n");
                    }
                    tr.demoGuiModel.ReadFromDemo(readDemo);
                    break;

                case DC_DEFINE_MODEL: {
                    idRenderModel model = renderModelManager.AllocModel();
                    model.ReadFromDemoFile(session.readDemo);
                    // add to model manager, so we can find it
                    renderModelManager.AddModel(model);

                    // save it in the list to free when clearing this map
                    localModels.Append(model);

                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_DEFINE_MODEL\n");
                    }
                    break;
                }
                case DC_SET_PORTAL_STATE: {
                    int[][] data = new int[2][1];
                    readDemo.ReadInt(data[0]);
                    readDemo.ReadInt(data[1]);
                    SetPortalState(data[0][0], data[1][0]);
                    if (r_showDemo.GetBool()) {
                        common.Printf("DC_SET_PORTAL_STATE: %d %d\n", data[0][0], data[1][0]);
                    }
                }

                break;
                case DC_END_FRAME:
                    return true;

                default:
                    common.Error("Bad token in demo stream");
            }

            return false;
        }

        public void WriteLoadMap() {

            // only the main renderWorld writes stuff to demos, not the wipes or
            // menu renders
            if (this != session.rw) {
                return;
            }

            session.writeDemo.WriteInt(DS_RENDER);
            session.writeDemo.WriteInt(DC_LOADMAP);

            demoHeader_t header = new demoHeader_t();
//            strncpy(header.mapname, mapName.c_str(), sizeof(header.mapname) - 1);
            header.mapname = mapName.c_str();
            header.version[0] = 4;
            header.sizeofRenderEntity[0] = sizeof(renderEntity_s.class);
            header.sizeofRenderLight[0] = sizeof(renderLight_s.class);
            session.writeDemo.WriteInt(header.version[0]);
            session.writeDemo.WriteInt(header.sizeofRenderEntity[0]);
            session.writeDemo.WriteInt(header.sizeofRenderLight[0]);
            for (int i = 0; i < 256; i++) {
                session.writeDemo.WriteChar((short) header.mapname[i]);
            }

            if (RenderSystem_init.r_showDemo.GetBool()) {
                common.Printf("write DC_DELETE_LIGHTDEF: %s\n", mapName);
            }
        }

        public void WriteRenderView(final renderView_s renderView) {
            int i;

            // only the main renderWorld writes stuff to demos, not the wipes or
            // menu renders
            if (this != session.rw) {
                return;
            }

            // write the actual view command
            session.writeDemo.WriteInt(DS_RENDER);
            session.writeDemo.WriteInt(DC_RENDERVIEW);
            session.writeDemo.WriteInt(renderView.viewID);
            session.writeDemo.WriteInt(renderView.x);
            session.writeDemo.WriteInt(renderView.y);
            session.writeDemo.WriteInt(renderView.width);
            session.writeDemo.WriteInt(renderView.height);
            session.writeDemo.WriteFloat(renderView.fov_x);
            session.writeDemo.WriteFloat(renderView.fov_y);
            session.writeDemo.WriteVec3(renderView.vieworg);
            session.writeDemo.WriteMat3(renderView.viewaxis);
            session.writeDemo.WriteBool(renderView.cramZNear);
            session.writeDemo.WriteBool(renderView.forceUpdate);
            // binary compatibility with old win32 version writing padded structures directly to disk
            session.writeDemo.WriteUnsignedChar((char) 0);
            session.writeDemo.WriteUnsignedChar((char) 0);
            session.writeDemo.WriteInt(renderView.time);
            for (i = 0; i < MAX_GLOBAL_SHADER_PARMS; i++) {
                session.writeDemo.WriteFloat(renderView.shaderParms[i]);
            }
//            session.writeDemo.WriteInt(renderView.globalMaterial);
            session.writeDemo.Write(renderView.globalMaterial);

            if (RenderSystem_init.r_showDemo.GetBool()) {
                common.Printf("write DC_RENDERVIEW: %d\n", renderView.time);
            }
        }

        public void WriteVisibleDefs(final viewDef_s viewDef) {
            // only the main renderWorld writes stuff to demos, not the wipes or
            // menu renders
            if (this != session.rw) {
                return;
            }

            // make sure all necessary entities and lights are updated
            for (viewEntity_s viewEnt = viewDef.viewEntitys; viewEnt != null; viewEnt = viewEnt.next) {
                idRenderEntityLocal ent = viewEnt.entityDef;

                if (ent.archived) {
                    // still up to date
                    continue;
                }

                // write it out
                WriteRenderEntity(ent.index, ent.parms);
                ent.archived = true;
            }

            for (viewLight_s viewLight = viewDef.viewLights; viewLight != null; viewLight = viewLight.next) {
                idRenderLightLocal light = viewLight.lightDef;

                if (light.archived) {
                    // still up to date
                    continue;
                }
                // write it out
                WriteRenderLight(light.index, light.parms);
                light.archived = true;
            }
        }

        public void WriteFreeLight(int/*qhandle_t*/ handle) {

            // only the main renderWorld writes stuff to demos, not the wipes or
            // menu renders
            if (this != session.rw) {
                return;
            }

            session.writeDemo.WriteInt(DS_RENDER.ordinal());
            session.writeDemo.WriteInt(DC_DELETE_LIGHTDEF);
            session.writeDemo.WriteInt(handle);

            if (RenderSystem_init.r_showDemo.GetBool()) {
                common.Printf("write DC_DELETE_LIGHTDEF: %d\n", handle);
            }
        }

        public void WriteFreeEntity(int/*qhandle_t*/ handle) {

            // only the main renderWorld writes stuff to demos, not the wipes or
            // menu renders
            if (this != session.rw) {
                return;
            }

            session.writeDemo.WriteInt(DS_RENDER.ordinal());
            session.writeDemo.WriteInt(DC_DELETE_ENTITYDEF);
            session.writeDemo.WriteInt(handle);

            if (RenderSystem_init.r_showDemo.GetBool()) {
                common.Printf("write DC_DELETE_ENTITYDEF: %d\n", handle);
            }
        }

        public void WriteRenderLight(int/*qhandle_t*/ handle, final renderLight_s light) {

            // only the main renderWorld writes stuff to demos, not the wipes or
            // menu renders
            if (this != session.rw) {
                return;
            }

            session.writeDemo.WriteInt(DS_RENDER.ordinal());
            session.writeDemo.WriteInt(DC_UPDATE_LIGHTDEF);
            session.writeDemo.WriteInt(handle);

            session.writeDemo.WriteMat3(light.axis);
            session.writeDemo.WriteVec3(light.origin);
            session.writeDemo.WriteInt(light.suppressLightInViewID);
            session.writeDemo.WriteInt(light.allowLightInViewID);
            session.writeDemo.WriteBool(light.noShadows);
            session.writeDemo.WriteBool(light.noSpecular);
            session.writeDemo.WriteBool(light.pointLight);
            session.writeDemo.WriteBool(light.parallel);
            session.writeDemo.WriteVec3(light.lightRadius);
            session.writeDemo.WriteVec3(light.lightCenter);
            session.writeDemo.WriteVec3(light.target);
            session.writeDemo.WriteVec3(light.right);
            session.writeDemo.WriteVec3(light.up);
            session.writeDemo.WriteVec3(light.start);
            session.writeDemo.WriteVec3(light.end);
//            session.writeDemo.WriteInt(light.prelightModel);
            session.writeDemo.Write(light.prelightModel);
            session.writeDemo.WriteInt(light.lightId);
//            session.writeDemo.WriteInt(light.shader);
            session.writeDemo.Write(light.shader);
            for (int i = 0; i < MAX_ENTITY_SHADER_PARMS; i++) {
                session.writeDemo.WriteFloat(light.shaderParms[i]);
            }
//            session.writeDemo.WriteInt(light.referenceSound);
            session.writeDemo.Write(light.referenceSound);

            if (light.prelightModel != null) {
                session.writeDemo.WriteHashString(light.prelightModel.Name());
            }
            if (light.shader != null) {
                session.writeDemo.WriteHashString(light.shader.GetName());
            }
            if (light.referenceSound != null) {
                int index = light.referenceSound.Index();
                session.writeDemo.WriteInt(index);
            }

            if (RenderSystem_init.r_showDemo.GetBool()) {
                common.Printf("write DC_UPDATE_LIGHTDEF: %d\n", handle);
            }
        }

        public void WriteRenderEntity(int/*qhandle_t*/ handle, final renderEntity_s ent) {

            // only the main renderWorld writes stuff to demos, not the wipes or
            // menu renders
            if (this != session.rw) {
                return;
            }

            session.writeDemo.WriteInt(DS_RENDER.ordinal());
            session.writeDemo.WriteInt(DC_UPDATE_ENTITYDEF);
            session.writeDemo.WriteInt(handle);

//            session.writeDemo.WriteInt((int) ent.hModel);
            session.writeDemo.Write(ent.hModel);
            session.writeDemo.WriteInt(ent.entityNum);
            session.writeDemo.WriteInt(ent.bodyId);
            session.writeDemo.WriteVec3(ent.bounds.oGet(0));
            session.writeDemo.WriteVec3(ent.bounds.oGet(1));
//            session.writeDemo.WriteInt((int) ent.callback);
            session.writeDemo.Write(ent.callback);
//            session.writeDemo.WriteInt((int) ent.callbackData);
            session.writeDemo.Write(ent.callbackData);
            session.writeDemo.WriteInt(ent.suppressSurfaceInViewID);
            session.writeDemo.WriteInt(ent.suppressShadowInViewID);
            session.writeDemo.WriteInt(ent.suppressShadowInLightID);
            session.writeDemo.WriteInt(ent.allowSurfaceInViewID);
            session.writeDemo.WriteVec3(ent.origin);
            session.writeDemo.WriteMat3(ent.axis);
//            session.writeDemo.WriteInt((int) ent.customShader);
//            session.writeDemo.WriteInt((int) ent.referenceShader);
//            session.writeDemo.WriteInt((int) ent.customSkin);
//            session.writeDemo.WriteInt((int) ent.referenceSound);
            session.writeDemo.Write(ent.customShader);
            session.writeDemo.Write(ent.referenceShader);
            session.writeDemo.Write(ent.customSkin);
            session.writeDemo.Write(ent.referenceSound);
            for (int i = 0; i < MAX_ENTITY_SHADER_PARMS; i++) {
                session.writeDemo.WriteFloat(ent.shaderParms[i]);
            }
            for (int i = 0; i < MAX_RENDERENTITY_GUI; i++) {
//                session.writeDemo.WriteInt((int &) ent.gui[i]);
                session.writeDemo.Write(ent.gui[i]);
            }
//            session.writeDemo.WriteInt((int) ent.remoteRenderView);
            session.writeDemo.Write(ent.remoteRenderView);
            session.writeDemo.WriteInt(ent.numJoints);
//            session.writeDemo.WriteInt((int) ent.joints);
            for (idJointMat joint : ent.joints) {//TODO: double check if writing individual floats is equavalent to the int cast above.
                float[] mat = joint.ToFloatPtr();
                ByteBuffer buffer = ByteBuffer.allocate(mat.length * 4);
                buffer.asFloatBuffer().put(mat);
                session.readDemo.Write(buffer);
//                for (int a = 0; a < mat.length; a++) {
//                    session.readDemo.WriteFloat(mat[a]);
//                }
            }
            session.writeDemo.WriteFloat(ent.modelDepthHack);
            session.writeDemo.WriteBool(ent.noSelfShadow);
            session.writeDemo.WriteBool(ent.noShadow);
            session.writeDemo.WriteBool(ent.noDynamicInteractions);
            session.writeDemo.WriteBool(ent.weaponDepthHack);
            session.writeDemo.WriteInt(ent.forceUpdate);

            if (ent.customShader != null) {
                session.writeDemo.WriteHashString(ent.customShader.GetName());
            }
            if (ent.customSkin != null) {
                session.writeDemo.WriteHashString(ent.customSkin.GetName());
            }
            if (ent.hModel != null) {
                session.writeDemo.WriteHashString(ent.hModel.Name());
            }
            if (ent.referenceShader != null) {
                session.writeDemo.WriteHashString(ent.referenceShader.GetName());
            }
            if (ent.referenceSound != null) {
                int index = ent.referenceSound.Index();
                session.writeDemo.WriteInt(index);
            }
            if (ent.numJoints != 0) {
                for (int i = 0; i < ent.numJoints; i++) {
                    float[] data = ent.joints[i].ToFloatPtr();
                    for (int j = 0; j < 12; ++j) {
                        session.writeDemo.WriteFloat(data[j]);
                    }
                }
            }

            /*
             if ( ent.decals ) {
             ent.decals.WriteToDemoFile( session.readDemo );
             }
             if ( ent.overlay ) {
             ent.overlay.WriteToDemoFile( session.writeDemo );
             }
             */
            if (WRITE_GUIS) {
//                if (ent.gui != null) {
//                    ent.gui.WriteToDemoFile(session.writeDemo);
//                }
//                if (ent.gui2 != null) {
//                    ent.gui2.WriteToDemoFile(session.writeDemo);
//                }
//                if (ent.gui3 != null) {
//                    ent.gui3.WriteToDemoFile(session.writeDemo);
//                }
            }

            // RENDERDEMO_VERSION >= 2 ( Doom3 1.2 )
            session.writeDemo.WriteInt(ent.timeGroup);
            session.writeDemo.WriteInt(ent.xrayIndex);

            if (RenderSystem_init.r_showDemo.GetBool()) {
                common.Printf("write DC_UPDATE_ENTITYDEF: %d = %s\n", handle, ent.hModel != null ? ent.hModel.Name() : "NULL");
            }
        }

        public void ReadRenderEntity() {
            renderEntity_s ent = new renderEntity_s();
            renderEntityShadow shadow = new Atomics.renderEntityShadow();
            int[] index = new int[1];
            int i;

            session.readDemo.ReadInt(index);
            if (index[0] < 0) {
                common.Error("ReadRenderEntity: index < 0");
            }

//            session.readDemo.ReadInt((int) shadow.hModel);
            session.readDemo.Read(shadow.hModel);
            session.readDemo.ReadInt(shadow.entityNum);
            session.readDemo.ReadInt(shadow.bodyId);
            session.readDemo.ReadVec3(shadow.bounds.oGet(0));
            session.readDemo.ReadVec3(shadow.bounds.oGet(1));
//            session.readDemo.ReadInt((int) shadow.callback);
//            session.readDemo.ReadInt((int) shadow.callbackData);
            session.readDemo.Read(shadow.callback);
            session.readDemo.Read(shadow.callbackData);
            session.readDemo.ReadInt(shadow.suppressSurfaceInViewID);
            session.readDemo.ReadInt(shadow.suppressShadowInViewID);
            session.readDemo.ReadInt(shadow.suppressShadowInLightID);
            session.readDemo.ReadInt(shadow.allowSurfaceInViewID);
            session.readDemo.ReadVec3(shadow.origin);
            session.readDemo.ReadMat3(shadow.axis);
//            session.readDemo.ReadInt((int) shadow.customShader);
//            session.readDemo.ReadInt((int) shadow.referenceShader);
//            session.readDemo.ReadInt((int) shadow.customSkin);
//            session.readDemo.ReadInt((int) shadow.referenceSound);
            session.readDemo.Read(shadow.customShader);
            session.readDemo.Read(shadow.referenceShader);
            session.readDemo.Read(shadow.customSkin);
            session.readDemo.Read(shadow.referenceSound);
            for (i = 0; i < MAX_ENTITY_SHADER_PARMS; i++) {
                session.readDemo.ReadFloat(shadow.shaderParms[i]);
            }
            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
//                session.readDemo.ReadInt((int) shadow.gui[i]);
                session.readDemo.Read(shadow.gui[i]);
            }
//            session.readDemo.ReadInt((int) shadow.remoteRenderView);
            session.readDemo.Read(shadow.remoteRenderView);
            session.readDemo.ReadInt(shadow.numJoints);
//            session.readDemo.ReadInt((int) shadow.joints);
            for (idJointMat joint : shadow.joints) {//TODO: double check if writing individual floats is equavalent to the int cast above.
                float[] mat = joint.ToFloatPtr();
                ByteBuffer buffer = ByteBuffer.allocate(mat.length * 4);
                buffer.asFloatBuffer().put(mat);
                session.readDemo.Read(buffer);
//                for (int a = 0; a < mat.length; a++) {
//                    float[] b = {0};
//                    session.readDemo.ReadFloat(b);
//                    mat[a] = b[0];
//                }
            }
            session.readDemo.ReadFloat(shadow.modelDepthHack);
            session.readDemo.ReadBool(shadow.noSelfShadow);
            session.readDemo.ReadBool(shadow.noShadow);
            session.readDemo.ReadBool(shadow.noDynamicInteractions);
            session.readDemo.ReadBool(shadow.weaponDepthHack);
            session.readDemo.ReadInt(shadow.forceUpdate);
            shadow.callback = null;
            if (shadow.customShader != null) {
                shadow.customShader = declManager.FindMaterial(session.readDemo.ReadHashString());
            }
            if (shadow.customSkin != null) {
                shadow.customSkin = declManager.FindSkin(session.readDemo.ReadHashString());
            }
            if (shadow.hModel != null) {
                shadow.hModel = renderModelManager.FindModel(session.readDemo.ReadHashString());
            }
            if (shadow.referenceShader != null) {
                shadow.referenceShader = declManager.FindMaterial(session.readDemo.ReadHashString());
            }
            if (shadow.referenceSound != null) {
//		int	index;
                session.readDemo.ReadInt(index);
                shadow.referenceSound = session.sw.EmitterForIndex(index[0]);
            }
            if (shadow.numJoints[0] != 0) {
                shadow.joints = new idJointMat[shadow.numJoints[0]];//Mem_Alloc16(ent.numJoints);
                for (i = 0; i < shadow.numJoints[0]; i++) {
                    float[] data = shadow.joints[i].ToFloatPtr();
                    for (int j = 0; j < 12; ++j) {
                        float[] d = {0};
                        session.readDemo.ReadFloat(d);
                        data[j] = d[0];
                    }
                }
            }

            shadow.callbackData = null;

            /*
             if ( ent.decals ) {
             ent.decals = idRenderModelDecal::Alloc();
             ent.decals.ReadFromDemoFile( session.readDemo );
             }
             if ( ent.overlay ) {
             ent.overlay = idRenderModelOverlay::Alloc();
             ent.overlay.ReadFromDemoFile( session.readDemo );
             }
             */
            for (i = 0; i < MAX_RENDERENTITY_GUI; i++) {
                if (shadow.gui[i] != null) {
                    shadow.gui[i] = uiManager.Alloc();
                    if (WRITE_GUIS) {
                        shadow.gui[i].ReadFromDemoFile(session.readDemo);
                    }
                }
            }

            // >= Doom3 v1.2 only
            if (session.renderdemoVersion >= 2) {
                session.readDemo.ReadInt(shadow.timeGroup);
                session.readDemo.ReadInt(shadow.xrayIndex);
            } else {
                shadow.timeGroup[0] = 0;
                shadow.xrayIndex[0] = 0;
            }

            ent.atomicSet(shadow);
            UpdateEntityDef(index[0], ent);

            if (RenderSystem_init.r_showDemo.GetBool()) {
                common.Printf("DC_UPDATE_ENTITYDEF: %d = %s\n", index[0], shadow.hModel != null ? shadow.hModel.Name() : "NULL");
            }
        }

        public void ReadRenderLight() {
            renderLightShadow shadow = new Atomics.renderLightShadow();
            renderLight_s light = new renderLight_s();
            int[] index = new int[1];

            session.readDemo.ReadInt(index);
            if (index[0] < 0) {
                common.Error("ReadRenderLight: index < 0 ");
            }

            session.readDemo.ReadMat3(shadow.axis);
            session.readDemo.ReadVec3(shadow.origin);
            session.readDemo.ReadInt(shadow.suppressLightInViewID);
            session.readDemo.ReadInt(shadow.allowLightInViewID);
            session.readDemo.ReadBool(shadow.noShadows);
            session.readDemo.ReadBool(shadow.noSpecular);
            session.readDemo.ReadBool(shadow.pointLight);
            session.readDemo.ReadBool(shadow.parallel);
            session.readDemo.ReadVec3(shadow.lightRadius);
            session.readDemo.ReadVec3(shadow.lightCenter);
            session.readDemo.ReadVec3(shadow.target);
            session.readDemo.ReadVec3(shadow.right);
            session.readDemo.ReadVec3(shadow.up);
            session.readDemo.ReadVec3(shadow.start);
            session.readDemo.ReadVec3(shadow.end);
//            session.readDemo.ReadInt((int) shadow.prelightModel);
            session.readDemo.Read(shadow.prelightModel);
            session.readDemo.ReadInt(shadow.lightId);
//            session.readDemo.ReadInt((int) shadow.shader);
            session.readDemo.Read(shadow.shader);
            for (int i = 0; i < MAX_ENTITY_SHADER_PARMS; i++) {
                float[] parm = {0};
                session.readDemo.ReadFloat(parm);
                shadow.shaderParms[i] = parm[0];
            }
//            session.readDemo.ReadInt((int) shadow.referenceSound);
            session.readDemo.Read(shadow.referenceSound);
            if (shadow.prelightModel != null) {
                shadow.prelightModel = renderModelManager.FindModel(session.readDemo.ReadHashString());
            }
            if (shadow.shader != null) {
                shadow.shader = declManager.FindMaterial(session.readDemo.ReadHashString());
            }
            if (shadow.referenceSound != null) {
//		int	index;
                session.readDemo.ReadInt(index);
                shadow.referenceSound = session.sw.EmitterForIndex(index[0]);
            }

            light.atomicSet(shadow);
            UpdateLightDef(index[0], light);

            if (RenderSystem_init.r_showDemo.GetBool()) {
                common.Printf("DC_UPDATE_LIGHTDEF: %d\n", index[0]);
            }
        }
        //--------------------------
        // RenderWorld.cpp

        public void ResizeInteractionTable() {
            // we overflowed the interaction table, so dump it
            // we may want to resize this in the future if it turns out to be common
            common.Printf("idRenderWorldLocal::ResizeInteractionTable: overflowed interactionTableWidth, dumping\n");
//            R_StaticFree(interactionTable);
            interactionTable = null;
        }

        public void AddEntityRefToArea(idRenderEntityLocal def, portalArea_s area) {
            areaReference_s ref;

            if (NOT(def)) {
                common.Error("idRenderWorldLocal::AddEntityRefToArea: NULL def");
            }

            ref = new areaReference_s();//areaReferenceAllocator.Alloc();

            tr.pc.c_entityReferences++;

            ref.entity = def;

            // link to entityDef
            ref.ownerNext = def.entityRefs;
            def.entityRefs = ref;

            // link to end of area list
            ref.area = area;
            ref.areaNext = area.entityRefs;
            ref.areaPrev = area.entityRefs.areaPrev;
            ref.areaNext.areaPrev = ref;
            ref.areaPrev.areaNext = ref;
        }

        public void AddLightRefToArea(idRenderLightLocal light, portalArea_s area) {
            areaReference_s lref;

            // add a lightref to this area
            lref = new areaReference_s();//areaReferenceAllocator.Alloc();
            lref.light = light;
            lref.area = area;
            lref.ownerNext = light.references;
            light.references = lref;
            tr.pc.c_lightReferences++;

            // doubly linked list so we can free them easily later
            area.lightRefs.areaNext.areaPrev = lref;
            lref.areaNext = area.lightRefs.areaNext;
            lref.areaPrev = area.lightRefs;
            area.lightRefs.areaNext = lref;
        }

        public void RecurseProcBSP_r(modelTrace_s results, int parentNodeNum, int nodeNum, float p1f, float p2f, final idVec3 p1, final idVec3 p2) {
            float t1, t2;
            float frac;
            idVec3 mid = new idVec3();
            int side;
            float midf;
            areaNode_t node;

            if (results.fraction <= p1f) {
                return;		// already hit something nearer
            }
            // empty leaf
            if (nodeNum < 0) {
                return;
            }
            // if solid leaf node
            if (nodeNum == 0) {
                if (parentNodeNum != -1) {

                    results.fraction = p1f;
                    results.point = p1;
                    node = areaNodes[parentNodeNum];
                    results.normal = node.plane.Normal();//TODO:ref?
                    return;
                }
            }
            node = areaNodes[nodeNum];

            // distance from plane for trace start and end
            t1 = node.plane.Normal().oMultiply(p1) + node.plane.oGet(3);
            t2 = node.plane.Normal().oMultiply(p2) + node.plane.oGet(3);

            if (t1 >= 0.0f && t2 >= 0.0f) {
                RecurseProcBSP_r(results, nodeNum, node.children[0], p1f, p2f, p1, p2);
                return;
            }
            if (t1 < 0.0f && t2 < 0.0f) {
                RecurseProcBSP_r(results, nodeNum, node.children[1], p1f, p2f, p1, p2);
                return;
            }
            side = t1 < t2 ? 1 : 0;
            frac = t1 / (t1 - t2);
            midf = p1f + frac * (p2f - p1f);
            mid.oSet(0, p1.oGet(0) + frac * (p2.oGet(0) - p1.oGet(0)));
            mid.oSet(1, p1.oGet(1) + frac * (p2.oGet(1) - p1.oGet(1)));
            mid.oSet(2, p1.oGet(2) + frac * (p2.oGet(2) - p1.oGet(2)));
            RecurseProcBSP_r(results, nodeNum, node.children[side], p1f, midf, p1, mid);
            RecurseProcBSP_r(results, nodeNum, node.children[side ^ 1], midf, p2f, mid, p2);
        }

        public void BoundsInAreas_r(int nodeNum, final idBounds bounds, int[] areas, int[] numAreas, int maxAreas) {
            int side, i;
            areaNode_t node;

            do {
                if (nodeNum < 0) {
                    nodeNum = -1 - nodeNum;

                    for (i = 0; i < numAreas[0]; i++) {
                        if (areas[i] == nodeNum) {
                            break;
                        }
                    }
                    if (i >= numAreas[0] && numAreas[0] < maxAreas) {
                        areas[numAreas[0]++] = nodeNum;
                    }
                    return;
                }

                node = areaNodes[nodeNum];

                side = bounds.PlaneSide(node.plane);
                if (side == PLANESIDE_FRONT) {
                    nodeNum = node.children[0];
                } else if (side == PLANESIDE_BACK) {
                    nodeNum = node.children[1];
                } else {
                    if (node.children[1] != 0) {
                        BoundsInAreas_r(node.children[1], bounds, areas, numAreas, maxAreas);
                        if (numAreas[0] >= maxAreas) {
                            return;
                        }
                    }
                    nodeNum = node.children[0];
                }
            } while (nodeNum != 0);

            return;
        }

        public float DrawTextLength(final String text, float scale, int len /*= 0*/) {
            return RB_DrawTextLength(text, scale, len);
        }

        public void FreeInteractions() {
            int i;
            idRenderEntityLocal def;

            for (i = 0; i < entityDefs.Num(); i++) {
                def = entityDefs.oGet(i);
                if (NOT(def)) {
                    continue;
                }
                // free all the interactions
                while (def.firstInteraction != null) {
                    def.firstInteraction.UnlinkAndFree();
                }
            }
        }

        /*
         ==================
         PushVolumeIntoTree

         Used for both light volumes and model volumes.

         This does not clip the points by the planes, so some slop
         occurs.

         tr.viewCount should be bumped before calling, allowing it
         to prevent double checking areas.

         We might alternatively choose to do this with an area flow.
         ==================
         */
        public void PushVolumeIntoTree_r(idRenderEntityLocal def, idRenderLightLocal light, final idSphere sphere, int numPoints, final idVec3[] points, int nodeNum) {
            int i;
            areaNode_t node;
            boolean front, back;

            if (nodeNum < 0) {
                portalArea_s area;
                int areaNum = -1 - nodeNum;

                area = portalAreas[areaNum];
                if (area.viewCount == tr.viewCount) {
                    return;	// already added a reference here
                }
                area.viewCount = tr.viewCount;

                if (def != null) {
                    AddEntityRefToArea(def, area);
                }
                if (light != null) {
                    AddLightRefToArea(light, area);
                }

                return;
            }

            node = areaNodes[nodeNum];

            // if we know that all possible children nodes only touch an area
            // we have already marked, we can early out
            if (r_useNodeCommonChildren.GetBool()
                    && node.commonChildrenArea != CHILDREN_HAVE_MULTIPLE_AREAS) {
                // note that we do NOT try to set a reference in this area
                // yet, because the test volume may yet wind up being in the
                // solid part, which would cause bounds slightly poked into
                // a wall to show up in the next room
                if (portalAreas[node.commonChildrenArea].viewCount == tr.viewCount) {
                    return;
                }
            }

            // if the bounding sphere is completely on one side, don't
            // bother checking the individual points
            float sd = node.plane.Distance(sphere.GetOrigin());
            if (sd >= sphere.GetRadius()) {
                nodeNum = node.children[0];
                if (nodeNum != 0) {	// 0 = solid
                    PushVolumeIntoTree_r(def, light, sphere, numPoints, points, nodeNum);
                }
                return;
            }
            if (sd <= -sphere.GetRadius()) {
                nodeNum = node.children[1];
                if (nodeNum != 0) {	// 0 = solid
                    PushVolumeIntoTree_r(def, light, sphere, numPoints, points, nodeNum);
                }
                return;
            }

            // exact check all the points against the node plane
            front = back = false;
//if(MACOS_X){	//loop unrolling & pre-fetching for performance
//	const idVec3 norm = node.plane.Normal();
//	const float plane3 = node.plane[3];
//	float D0, D1, D2, D3;
//
//	for ( i = 0 ; i < numPoints - 4; i+=4 ) {
//		D0 = points[i+0] * norm + plane3;
//		D1 = points[i+1] * norm + plane3;
//		if ( !front && D0 >= 0.0f ) {
//		    front = true;
//		} else if ( !back && D0 <= 0.0f ) {
//		    back = true;
//		}
//		D2 = points[i+1] * norm + plane3;
//		if ( !front && D1 >= 0.0f ) {
//		    front = true;
//		} else if ( !back && D1 <= 0.0f ) {
//		    back = true;
//		}
//		D3 = points[i+1] * norm + plane3;
//		if ( !front && D2 >= 0.0f ) {
//		    front = true;
//		} else if ( !back && D2 <= 0.0f ) {
//		    back = true;
//		}
//		
//		if ( !front && D3 >= 0.0f ) {
//		    front = true;
//		} else if ( !back && D3 <= 0.0f ) {
//		    back = true;
//		}
//		if ( back && front ) {
//		    break;
//		}
//	}
//	if(!(back && front)) {
//		for (; i < numPoints ; i++ ) {
//			float d;
//			d = points[i] * node.plane.Normal() + node.plane[3];
//			if ( d >= 0.0f ) {
//				front = true;
//			} else if ( d <= 0.0f ) {
//				back = true;
//			}
//			if ( back && front ) {
//				break;
//			}
//		}	
//	}
//}else
            {
                for (i = 0; i < numPoints; i++) {
                    float d;

                    d = points[i].oMultiply(node.plane.Normal()) + node.plane.oGet(3);
                    if (d >= 0.0f) {
                        front = true;
                    } else if (d <= 0.0f) {
                        back = true;
                    }
                    if (back && front) {
                        break;
                    }
                }
            }
            if (front) {
                nodeNum = node.children[0];
                if (nodeNum != 0) {	// 0 = solid
                    PushVolumeIntoTree_r(def, light, sphere, numPoints, points, nodeNum);
                }
            }
            if (back) {
                nodeNum = node.children[1];
                if (nodeNum != 0) {	// 0 = solid
                    PushVolumeIntoTree_r(def, light, sphere, numPoints, points, nodeNum);
                }
            }
        }

        public void PushVolumeIntoTree(idRenderEntityLocal def, idRenderLightLocal light, int numPoints, final idVec3[] points) {
            int i;
            float radSquared, lr;
            idVec3 mid = new idVec3(), dir;

            if (areaNodes == null) {
                return;
            }

            // calculate a bounding sphere for the points
            mid.Zero();
            for (i = 0; i < numPoints; i++) {
                mid.oPluSet(points[i]);
            }
            mid.oMulSet(1.0f / numPoints);

            radSquared = 0;

            for (i = 0; i < numPoints; i++) {
                dir = points[i].oMinus(mid);
                lr = dir.oMultiply(dir);
                if (lr > radSquared) {
                    radSquared = lr;
                }
            }

            idSphere sphere = new idSphere(mid, (float) Math.sqrt(radSquared));

            PushVolumeIntoTree_r(def, light, sphere, numPoints, points, 0);
        }
        //===============================================================================================================
        // tr_light.c

        /*
         =================
         idRenderWorldLocal::CreateLightDefInteractions

         When a lightDef is determined to effect the view (contact the frustum and non-0 light), it will check to
         make sure that it has interactions for all the entityDefs that it might possibly contact.

         This does not guarantee that all possible interactions for this light are generated, only that
         the ones that may effect the current view are generated. so it does need to be called every view.

         This does not cause entityDefs to create dynamic models, all work is done on the referenceBounds.

         All entities that have non-empty interactions with viewLights will
         have viewEntities made for them and be put on the viewEntity list,
         even if their surfaces aren't visible, because they may need to cast shadows.

         Interactions are usually removed when a entityDef or lightDef is modified, unless the change
         is known to not effect them, so there is no danger of getting a stale interaction, we just need to
         check that needed ones are created.

         An interaction can be at several levels:

         Don't interact (but share an area) (numSurfaces = 0)
         Entity reference bounds touches light frustum, but surfaces haven't been generated (numSurfaces = -1)
         Shadow surfaces have been generated, but light surfaces have not.  The shadow surface may still be empty due to bounds being conservative.
         Both shadow and light surfaces have been generated.  Either or both surfaces may still be empty due to conservative bounds.

         =================
         */
        public void CreateLightDefInteractions(idRenderLightLocal lDef) {
            areaReference_s eRef;
            areaReference_s lRef;
            idRenderEntityLocal eDef;
            portalArea_s area;
            idInteraction inter;
            int i = 0, j = 0;

            for (lRef = lDef.references; lRef != null; lRef = lRef.ownerNext, i++) {
                area = lRef.area;

                // check all the models in this area
                for (eRef = area.entityRefs.areaNext; eRef != area.entityRefs; eRef = eRef.areaNext, j++) {
                    eDef = eRef.entity;

                    // if the entity doesn't have any light-interacting surfaces, we could skip this,
                    // but we don't want to instantiate dynamic models yet, so we can't check that on
                    // most things
                    // if the entity isn't viewed
                    if (tr.viewDef != null && eDef.viewCount != tr.viewCount) {
                        // if the light doesn't cast shadows, skip
                        if (!lDef.lightShader.LightCastsShadows()) {
                            continue;
                        }
                        // if we are suppressing its shadow in this view, skip
                        if (!RenderSystem_init.r_skipSuppress.GetBool()) {
                            if (eDef.parms.suppressShadowInViewID != 0 && eDef.parms.suppressShadowInViewID == tr.viewDef.renderView.viewID) {
                                continue;
                            }
                            if (eDef.parms.suppressShadowInLightID != 0 && eDef.parms.suppressShadowInLightID == lDef.parms.lightId) {
                                continue;
                            }
                        }
                    }

                    // some big outdoor meshes are flagged to not create any dynamic interactions
                    // when the level designer knows that nearby moving lights shouldn't actually hit them
                    if (eDef.parms.noDynamicInteractions && eDef.world.generateAllInteractionsCalled) {
                        continue;
                    }

                    // if any of the edef's interaction match this light, we don't
                    // need to consider it. 
                    if (r_useInteractionTable.GetBool() && this.interactionTable != null) {
                        // allocating these tables may take several megs on big maps, but it saves 3% to 5% of
                        // the CPU time.  The table is updated at interaction::AllocAndLink() and interaction::UnlinkAndFree()
                        int index = lDef.index * this.interactionTableWidth + eDef.index;
                        inter = this.interactionTable[index];
                        if (index == 441291) {
                            int x = 0;
                        }
                        if (inter != null) {
                            // if this entity wasn't in view already, the scissor rect will be empty,
                            // so it will only be used for shadow casting
                            if (!inter.IsEmpty()) {
                                R_SetEntityDefViewEntity(eDef);
                            }
                            continue;
                        }
                    } else {
                        // scan the doubly linked lists, which may have several dozen entries

                        // we could check either model refs or light refs for matches, but it is
                        // assumed that there will be less lights in an area than models
                        // so the entity chains should be somewhat shorter (they tend to be fairly close).
                        for (inter = eDef.firstInteraction; inter != null; inter = inter.entityNext) {
                            if (inter.lightDef == lDef) {
                                break;
                            }
                        }

                        // if we already have an interaction, we don't need to do anything
                        if (inter != null) {
                            // if this entity wasn't in view already, the scissor rect will be empty,
                            // so it will only be used for shadow casting
                            if (!inter.IsEmpty()) {
                                R_SetEntityDefViewEntity(eDef);
                            }
                            continue;
                        }
                    }

                    //
                    // create a new interaction, but don't do any work other than bbox to frustum culling
                    //
                    inter = idInteraction.AllocAndLink(eDef, lDef);

                    // do a check of the entity reference bounds against the light frustum,
                    // trying to avoid creating a viewEntity if it hasn't been already
                    float[] modelMatrix = new float[16];
                    float[] m;

                    if (eDef.viewCount == tr.viewCount) {
                        m = eDef.viewEntity.modelMatrix;
                    } else {
                        R_AxisToModelMatrix(eDef.parms.axis, eDef.parms.origin, modelMatrix);
                        m = modelMatrix;
                    }

                    if (R_CullLocalBox(eDef.referenceBounds, m, 6, lDef.frustum)) {
                        inter.MakeEmpty();
                        continue;
                    }

                    // we will do a more precise per-surface check when we are checking the entity
                    // if this entity wasn't in view already, the scissor rect will be empty,
                    // so it will only be used for shadow casting
                    R_SetEntityDefViewEntity(eDef);
                }
            }
        }
    };
}
