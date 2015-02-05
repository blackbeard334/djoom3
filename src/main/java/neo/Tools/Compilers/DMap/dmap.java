package neo.Tools.Compilers.DMap;

import neo.CM.CollisionModel_local;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.tr_local.idRenderLightLocal;
import static neo.TempDump.NOT;
import neo.TempDump.TODO_Exception;
import static neo.TempDump.atoi;
import static neo.TempDump.isNotNullOrEmpty;
import neo.Tools.Compilers.AAS.AASBuild.RunAAS_f;
import static neo.Tools.Compilers.DMap.dmap.shadowOptLevel_t.SO_MERGE_SURFACES;
import static neo.Tools.Compilers.DMap.dmap.shadowOptLevel_t.SO_NONE;
import static neo.Tools.Compilers.DMap.facebsp.FaceBSP;
import static neo.Tools.Compilers.DMap.facebsp.MakeStructuralBspFaceList;
import static neo.Tools.Compilers.DMap.leakfile.LeakFile;
import static neo.Tools.Compilers.DMap.map.FreeDMapFile;
import static neo.Tools.Compilers.DMap.map.LoadDMapFile;
import static neo.Tools.Compilers.DMap.optimize.OptimizeEntity;
import neo.Tools.Compilers.DMap.optimize.optVertex_s;
import static neo.Tools.Compilers.DMap.output.WriteOutputFile;
import static neo.Tools.Compilers.DMap.portals.FillOutside;
import static neo.Tools.Compilers.DMap.portals.FloodAreas;
import static neo.Tools.Compilers.DMap.portals.FloodEntities;
import static neo.Tools.Compilers.DMap.portals.MakeTreePortals;
import static neo.Tools.Compilers.DMap.tritjunction.FixEntityTjunctions;
import static neo.Tools.Compilers.DMap.tritjunction.FixGlobalTjunctions;
import neo.Tools.Compilers.DMap.tritjunction.hashVert_s;
import static neo.Tools.Compilers.DMap.ubrush.FilterBrushesIntoTree;
import static neo.Tools.Compilers.DMap.usurface.ClipSidesByTree;
import static neo.Tools.Compilers.DMap.usurface.Prelight;
import static neo.Tools.Compilers.DMap.usurface.PutPrimitivesInAreas;
import static neo.framework.BuildDefines._WIN32;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import neo.framework.CmdSystem.cmdFunction_t;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.Common.com_outputMsg;
import static neo.framework.FileSystem_h.fileSystem;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.CmdArgs.idCmdArgs;
import static neo.idlib.Lib.idLib.common;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.PlaneSet.idPlaneSet;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import static neo.sys.win_shared.Sys_Milliseconds;

/**
 *
 */
public class dmap {

    static final int MAX_PATCH_SIZE = 32;
    //
    static final int PLANENUM_LEAF = -1;
    //
    static final int MAX_QPATH = 256;			// max length of a game pathname
    //
    static final int MAX_GROUP_LIGHTS = 16;

    static class primitive_s {

        primitive_s next;
        //
        // only one of these will be non-NULL
        bspbrush_s brush;
        mapTri_s tris;
    };

    static class uArea_t {

        optimizeGroup_s groups;
        // we might want to add other fields later
    };

    static class uEntity_t {

        idMapEntity mapEntity;                          // points into mapFile_t data
        //
        idVec3 origin;
        primitive_s primitives;
        tree_s tree;
        //
        int numAreas;
        uArea_t[] areas;
    };

    // chains of mapTri_t are the general unit of processing
    static class mapTri_s {

        mapTri_s next;
        //
        idMaterial material;
        Object mergeGroup;		// we want to avoid merging triangles
        // from different fixed groups, like guiSurfs and mirrors
        int planeNum;			// not set universally, just in some areas
        //
        idDrawVert[] v = new idDrawVert[3];
        hashVert_s[] hashVert = new hashVert_s[3];
        optVertex_s[] optVert = new optVertex_s[3];

        void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        void oSet(mapTri_s next) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    static class mesh_t {

        int width, height;
        idDrawVert verts;
    };

    static class parseMesh_s {

        parseMesh_s next;
        mesh_t mesh;
        idMaterial material;
    };

    static class bspface_s {

        bspface_s next;
        int planenum;
        boolean portal;			// all portals will be selected before
        // any non-portals
        boolean checked;		// used by SelectSplitPlaneNum()
        idWinding w;

        void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    static class textureVectors_t {

        idVec4[] v = new idVec4[2];	// the offset value will always be in the 0.0 to 1.0 range
    };

    static class side_s {

        int planenum;
        //
        idMaterial material;
        textureVectors_t texVec;
        //
        idWinding winding;		// only clipped to the other sides of the brush
        idWinding visibleHull;          // also clipped to the solid parts of the world
    };

    static class bspbrush_s {

        bspbrush_s next;
        bspbrush_s original;            // chopped up brushes will reference the originals
        //
        int entitynum;			// editor numbering for messages
        int brushnum;			// editor numbering for messages
        //
        idMaterial contentShader;	// one face's shader will determine the volume attributes
        //
        int contents;
        boolean opaque;
        int outputNumber;		// set when the brush is written to the file list
        //
        idBounds bounds;
        int numsides;
        side_s[] sides = new side_s[6];	// variably sized
    };

    static class uBrush_t extends bspbrush_s {

        uBrush_t() {
        }

        //copy constructor
        uBrush_t(uBrush_t brush) {
            this.next = brush.next;
            this.original = brush.original;

            this.entitynum = brush.entitynum;
            this.brushnum = brush.brushnum;

            this.contentShader = brush.contentShader;

            this.contents = brush.contents;
            this.opaque = brush.opaque;
            this.outputNumber = brush.outputNumber;

            this.bounds = brush.bounds;
            this.numsides = brush.numsides;
            System.arraycopy(brush.sides, 0, this.sides, 0, 6);
        }

        void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        void oSet(uBrush_t CopyBrush) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    static class drawSurfRef_s {

        drawSurfRef_s nextRef;
        int outputNumber;
    };

    static class node_s {
        // both leafs and nodes

        int planenum;	// -1 = leaf node
        node_s parent;
        idBounds bounds;		// valid after portalization
        //
        // nodes only
        side_s side;                    // the side that created the node
        node_s[] children = new node_s[2];
        int nodeNumber;                 // set after pruning
        //
        // leafs only
        boolean opaque;                 // view can never be inside
        //
        uBrush_t brushlist;             // fragments of all brushes in this leaf
        // needed for FindSideForPortal
        //
        int area;                       // determined by flood filling up to areaportals
        int occupied;                   // 1 or greater can reach entity
        uEntity_t occupant;             // for leak file testing
        //
        uPortal_s portals;              // also on nodes during construction

        void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    static class uPortal_s {

        idPlane plane;
        node_s onnode;                  // NULL = outside box
        node_s[] nodes = new node_s[2];	// [0] = front side of plane
        uPortal_s[] next = new uPortal_s[2];
        idWinding winding;

        void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    // a tree_t is created by FaceBSP()
    static class tree_s {

        node_s headnode;
        node_s outside_node;
        idBounds bounds;

        void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    static class mapLight_t {

        idRenderLightLocal def;
        char[] name = new char[MAX_QPATH];	// for naming the shadow volume surface and interactions
        srfTriangles_s shadowTris;
    };

    static class optimizeGroup_s {

        optimizeGroup_s nextGroup;
        //
        idBounds bounds;			// set in CarveGroupsByLight
        //
        // all of these must match to add a triangle to the triList
        boolean smoothed;			// curves will never merge with brushes
        int planeNum;
        int areaNum;
        idMaterial material;
        int numGroupLights;
        mapLight_t[] groupLights = new mapLight_t[MAX_GROUP_LIGHTS];	// lights effecting this list
        Object mergeGroup;                      // if this differs (guiSurfs, mirrors, etc), the
        // groups will not be combined into model surfaces
        // after optimization
        textureVectors_t texVec;
        //
        boolean surfaceEmited;
        //
        mapTri_s triList;
        mapTri_s regeneratedTris;               // after each island optimization
        idVec3[] axis = new idVec3[2];		// orthogonal to the plane, so optimization can be 2D

        void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        void oSet(optimizeGroup_s nextGroup) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    // all primitives from the map are added to optimzeGroups, creating new ones as needed
    // each optimizeGroup is then split into the map areas, creating groups in each area
    // each optimizeGroup is then divided by each light, creating more groups
    // the final list of groups is then tjunction fixed against all groups, then optimized internally
    // multiple optimizeGroups will be merged together into .proc surfaces, but no further optimization
    // is done on them
    //=============================================================================
    static enum shadowOptLevel_t {

        SO_NONE, // 0
        SO_MERGE_SURFACES, // 1
        SO_CULL_OCCLUDED, // 2
        SO_CLIP_OCCLUDERS, // 3
        SO_CLIP_SILS, // 4
        SO_SIL_OPTIMIZE		// 5
    };

    public static class dmapGlobals_t {
        // mapFileBase will contain the qpath without any extension: "maps/test_box"

        char[] mapFileBase = new char[1024];
        //
        idMapFile dmapFile;
        //
        idPlaneSet mapPlanes;
        //
        int num_entities;
        uEntity_t[] uEntities;
        //
        int entityNum;
        //
        idList<mapLight_t> mapLights;
        //
        boolean verbose;
        //
        boolean glview;
        boolean noOptimize;
        boolean verboseentities;
        boolean noCurves;
        boolean fullCarve;
        boolean noModelBrushes;
        boolean noTJunc;
        boolean nomerge;
        boolean noFlood;
        boolean noClipSides;		// don't cut sides by solid leafs, use the entire thing
        boolean noLightCarve;		// extra triangle subdivision by light frustums
        shadowOptLevel_t shadowOptLevel;
        boolean noShadow;		// don't create optimized shadow volumes
        //
        idBounds drawBounds;
        boolean drawflag;
        //
        int totalShadowTriangles;
        int totalShadowVerts;
    };
    //
    public static dmapGlobals_t dmapGlobals;
    //=============================================================================

    /*
     ============
     ProcessModel
     ============
     */
    static boolean ProcessModel(uEntity_t e, boolean floodFill) {
        bspface_s faces;

        // build a bsp tree using all of the sides
        // of all of the structural brushes
        faces = MakeStructuralBspFaceList(e.primitives);
        e.tree = FaceBSP(faces);

        // create portals at every leaf intersection
        // to allow flood filling
        MakeTreePortals(e.tree);

        // classify the leafs as opaque or areaportal
        FilterBrushesIntoTree(e);

        // see if the bsp is completely enclosed
        if (floodFill && !dmapGlobals.noFlood) {
            if (FloodEntities(e.tree)) {
                // set the outside leafs to opaque
                FillOutside(e);
            } else {
                common.Printf("**********************\n");
                common.Warning("******* leaked *******");
                common.Printf("**********************\n");
                LeakFile(e.tree);
                // bail out here.  If someone really wants to
                // process a map that leaks, they should use
                // -noFlood
                return false;
            }
        }

        // get minimum convex hulls for each visible side
        // this must be done before creating area portals,
        // because the visible hull is used as the portal
        ClipSidesByTree(e);

        // determine areas before clipping tris into the
        // tree, so tris will never cross area boundaries
        FloodAreas(e);

        // we now have a BSP tree with solid and non-solid leafs marked with areas
        // all primitives will now be clipped into this, throwing away
        // fragments in the solid areas
        PutPrimitivesInAreas(e);

        // now build shadow volumes for the lights and split
        // the optimize lists by the light beam trees
        // so there won't be unneeded overdraw in the static
        // case
        Prelight(e);

        // optimizing is a superset of fixing tjunctions
        if (!dmapGlobals.noOptimize) {
            OptimizeEntity(e);
        } else if (!dmapGlobals.noTJunc) {
            FixEntityTjunctions(e);
        }

        // now fix t junctions across areas
        FixGlobalTjunctions(e);

        return true;
    }

    /*
     ============
     ProcessModels
     ============
     */
    static boolean ProcessModels() {
        boolean oldVerbose;
        uEntity_t entity;

        oldVerbose = dmapGlobals.verbose;

        for (dmapGlobals.entityNum = 0; dmapGlobals.entityNum < dmapGlobals.num_entities; dmapGlobals.entityNum++) {

            entity = dmapGlobals.uEntities[dmapGlobals.entityNum];
            if (NOT(entity.primitives)) {
                continue;
            }

            common.Printf("############### entity %d ###############\n", dmapGlobals.entityNum);

            // if we leaked, stop without any more processing
            if (!ProcessModel(entity, (dmapGlobals.entityNum == 0))) {
                return false;
            }

            // we usually don't want to see output for submodels unless
            // something strange is going on
            if (!dmapGlobals.verboseentities) {
                dmapGlobals.verbose = false;
            }
        }

        dmapGlobals.verbose = oldVerbose;

        return true;
    }

    /*
     ============
     DmapHelp
     ============
     */
    static void DmapHelp() {
        common.Printf(
                "Usage: dmap [options] mapfile\n"
                + "Options:\n"
                + "noCurves          = don't process curves\n"
                + "noCM              = don't create collision map\n"
                + "noAAS             = don't create AAS files\n"
        );
    }

    /*
     ============
     ResetDmapGlobals
     ============
     */
    static void ResetDmapGlobals() {
        dmapGlobals.mapFileBase[0] = '\0';
        dmapGlobals.dmapFile = null;
        dmapGlobals.mapPlanes.Clear();
        dmapGlobals.num_entities = 0;
        dmapGlobals.uEntities = null;
        dmapGlobals.entityNum = 0;
        dmapGlobals.mapLights.Clear();
        dmapGlobals.verbose = false;
        dmapGlobals.glview = false;
        dmapGlobals.noOptimize = false;
        dmapGlobals.verboseentities = false;
        dmapGlobals.noCurves = false;
        dmapGlobals.fullCarve = false;
        dmapGlobals.noModelBrushes = false;
        dmapGlobals.noTJunc = false;
        dmapGlobals.nomerge = false;
        dmapGlobals.noFlood = false;
        dmapGlobals.noClipSides = false;
        dmapGlobals.noLightCarve = false;
        dmapGlobals.noShadow = false;
        dmapGlobals.shadowOptLevel = SO_NONE;
        dmapGlobals.drawBounds.Clear();
        dmapGlobals.drawflag = false;
        dmapGlobals.totalShadowTriangles = 0;
        dmapGlobals.totalShadowVerts = 0;
    }

    /*
     ============
     Dmap
     ============
     */
    static void Dmap(final idCmdArgs args) {
        int i;
        int start, end;
        String path;//= new char[1024];
        idStr passedName = new idStr();
        boolean leaked = false;
        boolean noCM = false;
        boolean noAAS = false;

        ResetDmapGlobals();

        if (args.Argc() < 2) {
            DmapHelp();
            return;
        }

        common.Printf("---- dmap ----\n");

        dmapGlobals.fullCarve = true;
        dmapGlobals.shadowOptLevel = SO_MERGE_SURFACES;		// create shadows by merging all surfaces, but no super optimization
//	dmapGlobals.shadowOptLevel = SO_CLIP_OCCLUDERS;		// remove occluders that are completely covered
//	dmapGlobals.shadowOptLevel = SO_SIL_OPTIMIZE;
//	dmapGlobals.shadowOptLevel = SO_CULL_OCCLUDED;

        dmapGlobals.noLightCarve = true;

        for (i = 1; i < args.Argc(); i++) {
            String s;

            s = args.Argv(i);
            if (isNotNullOrEmpty(s) && s.length() > 0
                    && s.startsWith("-")) {
                s = s.substring(1);
                if (s.length() == 0 || s.startsWith("\0")) {
                    continue;
                }
            }

            if (NOT(idStr.Icmp(s, "glview"))) {
                dmapGlobals.glview = true;
            } else if (NOT(idStr.Icmp(s, "v"))) {
                common.Printf("verbose = true\n");
                dmapGlobals.verbose = true;
            } else if (NOT(idStr.Icmp(s, "draw"))) {
                common.Printf("drawflag = true\n");
                dmapGlobals.drawflag = true;
            } else if (NOT(idStr.Icmp(s, "noFlood"))) {
                common.Printf("noFlood = true\n");
                dmapGlobals.noFlood = true;
            } else if (NOT(idStr.Icmp(s, "noLightCarve"))) {
                common.Printf("noLightCarve = true\n");
                dmapGlobals.noLightCarve = true;
            } else if (NOT(idStr.Icmp(s, "lightCarve"))) {
                common.Printf("noLightCarve = false\n");
                dmapGlobals.noLightCarve = false;
            } else if (NOT(idStr.Icmp(s, "noOpt"))) {
                common.Printf("noOptimize = true\n");
                dmapGlobals.noOptimize = true;
            } else if (NOT(idStr.Icmp(s, "verboseentities"))) {
                common.Printf("verboseentities = true\n");
                dmapGlobals.verboseentities = true;
            } else if (NOT(idStr.Icmp(s, "noCurves"))) {
                common.Printf("noCurves = true\n");
                dmapGlobals.noCurves = true;
            } else if (NOT(idStr.Icmp(s, "noModels"))) {
                common.Printf("noModels = true\n");
                dmapGlobals.noModelBrushes = true;
            } else if (NOT(idStr.Icmp(s, "noClipSides"))) {
                common.Printf("noClipSides = true\n");
                dmapGlobals.noClipSides = true;
            } else if (NOT(idStr.Icmp(s, "noCarve"))) {
                common.Printf("noCarve = true\n");
                dmapGlobals.fullCarve = false;
            } else if (NOT(idStr.Icmp(s, "shadowOpt"))) {
                dmapGlobals.shadowOptLevel = shadowOptLevel_t.values()[atoi(args.Argv(i + 1))];
                common.Printf("shadowOpt = %d\n", dmapGlobals.shadowOptLevel);
                i += 1;
            } else if (NOT(idStr.Icmp(s, "noTjunc"))) {
                // triangle optimization won't work properly without tjunction fixing
                common.Printf("noTJunc = true\n");
                dmapGlobals.noTJunc = true;
                dmapGlobals.noOptimize = true;
                common.Printf("forcing noOptimize = true\n");
            } else if (NOT(idStr.Icmp(s, "noCM"))) {
                noCM = true;
                common.Printf("noCM = true\n");
            } else if (NOT(idStr.Icmp(s, "noAAS"))) {
                noAAS = true;
                common.Printf("noAAS = true\n");
            } else if (NOT(idStr.Icmp(s, "editorOutput"))) {
                if (_WIN32) {
                    com_outputMsg = true;
                }
            } else {
                break;
            }
        }

        if (i >= args.Argc()) {
            common.Error("usage: dmap [options] mapfile");
        }

        passedName.oSet(args.Argv(i));		// may have an extension
        passedName.BackSlashesToSlashes();
        if (passedName.Icmpn("maps/", 4) != 0) {
            passedName.oSet("maps/" + passedName);
        }

        idStr stripped = passedName;
        stripped.StripFileExtension();
        idStr.Copynz(dmapGlobals.mapFileBase, stripped.c_str(), dmapGlobals.mapFileBase.length);

        boolean region = false;
        // if this isn't a regioned map, delete the last saved region map
        if (!passedName.Right(4).equals(".reg")) {
            path = String.format("%s.reg", dmapGlobals.mapFileBase);
            fileSystem.RemoveFile(path);
        } else {
            region = true;
        }

        passedName = stripped;

        // delete any old line leak files
        path = String.format("%s.lin", dmapGlobals.mapFileBase);
        fileSystem.RemoveFile(path);

        //
        // start from scratch
        //
        start = Sys_Milliseconds();

        if (!LoadDMapFile(passedName.toString())) {
            return;
        }

        if (ProcessModels()) {
            WriteOutputFile();
        } else {
            leaked = true;
        }

        FreeDMapFile();

        common.Printf("%d total shadow triangles\n", dmapGlobals.totalShadowTriangles);
        common.Printf("%d total shadow verts\n", dmapGlobals.totalShadowVerts);

        end = Sys_Milliseconds();
        common.Printf("-----------------------\n");
        common.Printf("%5.0f seconds for dmap\n", (end - start) * 0.001f);

        if (!leaked) {

            if (!noCM) {

                // make sure the collision model manager is not used by the game
                cmdSystem.BufferCommandText(CMD_EXEC_NOW, "disconnect");

                // create the collision map
                start = Sys_Milliseconds();

                CollisionModel_local.collisionModelManager.LoadMap(dmapGlobals.dmapFile);
                CollisionModel_local.collisionModelManager.FreeMap();

                end = Sys_Milliseconds();
                common.Printf("-------------------------------------\n");
                common.Printf("%5.0f seconds to create collision map\n", (end - start) * 0.001f);
            }

            if (!noAAS && !region) {
                // create AAS files
                RunAAS_f.getInstance().run(args);
            }
        }

        // free the common .map representation
//        delete dmapGlobals.dmapFile;
        dmapGlobals.dmapFile = null;

        // clear the map plane list
        dmapGlobals.mapPlanes.Clear();

        if (_WIN32) {
            throw new TODO_Exception();
//            if (com_outputMsg && com_hwndMsg != 0) {
//                long msg = RegisterWindowMessage(DMAP_DONE);
//                PostMessage(com_hwndMsg, msg, 0, 0);
//            }
        }
    }


    /*
     ============
     Dmap_f
     ============
     */
    public static class Dmap_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Dmap_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {

            common.ClearWarnings("running dmap");

            // refresh the screen each time we print so it doesn't look
            // like it is hung
            common.SetRefreshOnPrint(true);
            Dmap(args);
            common.SetRefreshOnPrint(false);

            common.PrintWarnings();
        }
    }
}
