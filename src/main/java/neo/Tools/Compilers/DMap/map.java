package neo.Tools.Compilers.DMap;

import static java.lang.Math.floor;
import neo.Game.GameEdit;
import static neo.Renderer.Material.CONTENTS_AREAPORTAL;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.Material.materialCoverage_t.MC_OPAQUE;
import static neo.Renderer.tr_lightrun.R_DeriveLightData;
import static neo.Renderer.tr_lightrun.R_FreeLightDefDerivedData;
import static neo.TempDump.NOT;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import neo.Tools.Compilers.DMap.dmap.mapLight_t;
import neo.Tools.Compilers.DMap.dmap.mapTri_s;
import neo.Tools.Compilers.DMap.dmap.optimizeGroup_s;
import neo.Tools.Compilers.DMap.dmap.primitive_s;
import neo.Tools.Compilers.DMap.dmap.side_s;
import neo.Tools.Compilers.DMap.dmap.uArea_t;
import neo.Tools.Compilers.DMap.dmap.uBrush_t;
import neo.Tools.Compilers.DMap.dmap.uEntity_t;
import static neo.Tools.Compilers.DMap.facebsp.FreeTree;
import static neo.Tools.Compilers.DMap.tritools.AllocTri;
import static neo.Tools.Compilers.DMap.tritools.FreeTriList;
import static neo.Tools.Compilers.DMap.ubrush.CopyBrush;
import static neo.Tools.Compilers.DMap.ubrush.CreateBrushWindings;
import static neo.Tools.Compilers.DMap.ubrush.c_active_brushes;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import neo.idlib.BV.Bounds.idBounds;
import static neo.idlib.MapFile.DEFAULT_CURVE_MAX_ERROR;
import static neo.idlib.MapFile.DEFAULT_CURVE_MAX_LENGTH;
import neo.idlib.MapFile.idMapBrush;
import neo.idlib.MapFile.idMapBrushSide;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.MapFile.idMapPatch;
import neo.idlib.MapFile.idMapPrimitive;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.Surface.idSurface;
import neo.idlib.geometry.Surface_Patch.idSurface_Patch;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Vector.DotProduct;

/**
 *
 */
public class map {

    /*

     After parsing, there will be a list of entities that each has
     a list of primitives.
  
     Primitives are either brushes, triangle soups, or model references.

     Curves are tesselated to triangle soups at load time, but model
     references are 
     Brushes will have 
  
     brushes, each of which has a side definition.

     */
    //
    // private declarations
    //
    static final int   MAX_BUILD_SIDES = 300;
    //
    static int       entityPrimitive;        // to track editor brush numbers
    static int       c_numMapPatches;
    static int       c_areaportals;
    //
    static uEntity_t uEntity;
    //
    // brushes are parsed into a temporary array of sides,
    // which will have duplicates removed before the final brush is allocated
    static uBrush_t  buildBrush;
    //
    //
    static final float NORMAL_EPSILON = 0.00001f;
    static final float DIST_EPSILON   = 0.01f;


    /*
     ===========
     FindFloatPlane
     ===========
     */
    static int FindFloatPlane(final idPlane plane, boolean[] fixedDegeneracies) {
        idPlane p = plane;
        boolean fixed = p.FixDegeneracies(DIST_EPSILON);
        if (fixed && fixedDegeneracies != null) {
            fixedDegeneracies[0] = true;
        }
        return dmapGlobals.mapPlanes.FindPlane(p, NORMAL_EPSILON, DIST_EPSILON);
    }

    static int FindFloatPlane(final idPlane plane) {
        return FindFloatPlane(plane, null);
    }

    /*
     ===========
     SetBrushContents

     The contents on all sides of a brush should be the same
     Sets contentsShader, contents, opaque
     ===========
     */
    static void SetBrushContents(uBrush_t b) {
        int contents, c2;
        side_s s;
        int i;
        boolean mixed;

        s = b.sides[0];
        contents = s.material.GetContentFlags();

        b.contentShader = s.material;
        mixed = false;

        // a brush is only opaque if all sides are opaque
        b.opaque = true;

        for (i = 1; i < b.numsides; i++) {
            s = b.sides[i];

            if (NOT(s.material)) {
                continue;
            }

            c2 = s.material.GetContentFlags();
            if (c2 != contents) {
                mixed = true;
                contents |= c2;
            }

            if (s.material.Coverage() != MC_OPAQUE) {
                b.opaque = false;
            }
        }

        if ((contents & CONTENTS_AREAPORTAL) != 0) {
            c_areaportals++;
        }

        b.contents = contents;
    }

//============================================================================

    /*
     ===============
     FreeBuildBrush
     ===============
     */
    static void FreeBuildBrush() {
        int i;

        for (i = 0; i < buildBrush.numsides; i++) {
            if (buildBrush.sides[i].winding != null) {
//			delete buildBrush.sides[i].winding;
                buildBrush.sides[i].winding = null;
            }
        }
        buildBrush.numsides = 0;
    }

    /*
     ===============
     FinishBrush

     Produces a final brush based on the buildBrush.sides array
     and links it to the current entity
     ===============
     */
    static uBrush_t FinishBrush() {
        uBrush_t b;
        primitive_s prim;

        // create windings for sides and bounds for brush
        if (!CreateBrushWindings(buildBrush)) {
            // don't keep this brush
            FreeBuildBrush();
            return null;
        }

        if ((buildBrush.contents & CONTENTS_AREAPORTAL) != 0) {
            if (dmapGlobals.num_entities != 1) {
                common.Printf("Entity %d, Brush %d: areaportals only allowed in world\n", dmapGlobals.num_entities - 1, entityPrimitive);
                FreeBuildBrush();
                return null;
            }
        }

        // keep it
        b = CopyBrush(buildBrush);

        FreeBuildBrush();

        b.entitynum = dmapGlobals.num_entities - 1;
        b.brushnum = entityPrimitive;

        b.original = b;

        prim = new primitive_s();// Mem_Alloc(sizeof(prim));
//	memset( prim, 0, sizeof( *prim ) );
        prim.next = uEntity.primitives;
        uEntity.primitives = prim;

        prim.brush = b;

        return b;
    }

    /*
     ================
     AdjustEntityForOrigin
     ================
     */
    static void AdjustEntityForOrigin(uEntity_t ent) {
        primitive_s prim;
        uBrush_t b;
        int i;
        side_s s;

        for (prim = ent.primitives; prim != null; prim = prim.next) {
            b = (uBrush_t) prim.brush;
            if (NOT(b)) {
                continue;
            }
            for (i = 0; i < b.numsides; i++) {
                idPlane plane;

                s = b.sides[i];

                plane = dmapGlobals.mapPlanes.oGet(s.planenum);
                plane.oPluSet(3, plane.Normal().oMultiply(ent.origin));

                s.planenum = FindFloatPlane(plane);

                s.texVec.v[0].oPluSet(3, DotProduct(ent.origin, s.texVec.v[0]));
                s.texVec.v[1].oPluSet(3, DotProduct(ent.origin, s.texVec.v[1]));

                // remove any integral shift
                s.texVec.v[0].oMinSet(3, (float) floor(s.texVec.v[0].oGet(3)));
                s.texVec.v[1].oMinSet(3, (float) floor(s.texVec.v[1].oGet(3)));
            }
            CreateBrushWindings(b);
        }
    }

    /*
     =================
     RemoveDuplicateBrushPlanes

     Returns false if the brush has a mirrored set of planes,
     meaning it encloses no volume.
     Also removes planes without any normal
     =================
     */
    static boolean RemoveDuplicateBrushPlanes(uBrush_t b) {
        int i, j, k;
        side_s[] sides;

        sides = b.sides;

        for (i = 1; i < b.numsides; i++) {

            // check for a degenerate plane
            if (sides[i].planenum == -1) {
                common.Printf("Entity %d, Brush %d: degenerate plane\n", b.entitynum, b.brushnum);
                // remove it
                for (k = i + 1; k < b.numsides; k++) {
                    sides[k - 1] = sides[k];
                }
                b.numsides--;
                i--;
                continue;
            }

            // check for duplication and mirroring
            for (j = 0; j < i; j++) {
                if (sides[i].planenum == sides[j].planenum) {
                    common.Printf("Entity %d, Brush %d: duplicate plane\n", b.entitynum, b.brushnum);
                    // remove the second duplicate
                    for (k = i + 1; k < b.numsides; k++) {
                        sides[k - 1] = sides[k];
                    }
                    b.numsides--;
                    i--;
                    break;
                }

                if (sides[i].planenum == (sides[j].planenum ^ 1)) {
                    // mirror plane, brush is invalid
                    common.Printf("Entity %d, Brush %d: mirrored plane\n", b.entitynum, b.brushnum);
                    return false;
                }
            }
        }
        return true;
    }


    /*
     =================
     ParseBrush
     =================
     */
    static void ParseBrush(final idMapBrush mapBrush, int primitiveNum) {
        uBrush_t b;
        side_s s;
        idMapBrushSide ms;
        int i;
        boolean[] fixedDegeneracies = {false};

        buildBrush.entitynum = dmapGlobals.num_entities - 1;
        buildBrush.brushnum = entityPrimitive;
        buildBrush.numsides = mapBrush.GetNumSides();
        for (i = 0; i < mapBrush.GetNumSides(); i++) {
//            s = buildBrush.sides[i];
            ms = mapBrush.GetSide(i);

//		memset( s, 0, sizeof( *s ) );
            s = buildBrush.sides[i] = new side_s();
            s.planenum = FindFloatPlane(ms.GetPlane(), fixedDegeneracies);
            s.material = declManager.FindMaterial(ms.GetMaterial());
            ms.GetTextureVectors(s.texVec.v);
            // remove any integral shift, which will help with grouping
            s.texVec.v[0].oMinSet(3, (float) floor(s.texVec.v[0].oGet(3)));
            s.texVec.v[1].oMinSet(3, (float) floor(s.texVec.v[1].oGet(3)));
        }

        // if there are mirrored planes, the entire brush is invalid
        if (!RemoveDuplicateBrushPlanes(buildBrush)) {
            return;
        }

        // get the content for the entire brush
        SetBrushContents(buildBrush);

        b = FinishBrush();
        if (NOT(b)) {
            return;
        }

        if (fixedDegeneracies[0] && dmapGlobals.verboseentities) {
            common.Warning("brush %d has degenerate plane equations", primitiveNum);
        }
    }

    /*
     ================
     ParseSurface
     ================
     */
    static void ParseSurface(final idMapPatch patch, final idSurface surface, final idMaterial material) {
        int i;
        mapTri_s tri;
        primitive_s prim;

        prim = new primitive_s();// Mem_Alloc(sizeof(prim));
//	memset( prim, 0, sizeof( *prim ) );
        prim.next = uEntity.primitives;
        uEntity.primitives = prim;

        for (i = 0; i < surface.GetNumIndexes(); i += 3) {
            tri = AllocTri();
            tri.v[2] = surface.oGet(surface.GetIndexes()[i + 0]);
            tri.v[1] = surface.oGet(surface.GetIndexes()[i + 2]);
            tri.v[0] = surface.oGet(surface.GetIndexes()[i + 1]);
            tri.material = material;
            tri.next = prim.tris;
            prim.tris = tri;
        }

        // set merge groups if needed, to prevent multiple sides from being
        // merged into a single surface in the case of gui shaders, mirrors, and autosprites
        if (material.IsDiscrete()) {
            for (tri = prim.tris; tri != null; tri = tri.next) {
                tri.mergeGroup = patch;
            }
        }
    }

    /*
     ================
     ParsePatch
     ================
     */
    static void ParsePatch(final idMapPatch patch, int primitiveNum) {
        idMaterial mat;

        if (dmapGlobals.noCurves) {
            return;
        }

        c_numMapPatches++;

        mat = declManager.FindMaterial(patch.GetMaterial());

        idSurface_Patch cp = new idSurface_Patch(patch);

        if (patch.GetExplicitlySubdivided()) {
            cp.SubdivideExplicit(patch.GetHorzSubdivisions(), patch.GetVertSubdivisions(), true);
        } else {
            cp.Subdivide(DEFAULT_CURVE_MAX_ERROR, DEFAULT_CURVE_MAX_ERROR, DEFAULT_CURVE_MAX_LENGTH, true);
        }

        ParseSurface(patch, cp, mat);

//	delete cp;
    }

    /*
     ================
     ProcessMapEntity
     ================
     */
    static boolean ProcessMapEntity(idMapEntity mapEnt) {
        idMapPrimitive prim;

        uEntity = dmapGlobals.uEntities[dmapGlobals.num_entities];
//	memset( uEntity, 0, sizeof(*uEntity) );
        uEntity.mapEntity = mapEnt;
        dmapGlobals.num_entities++;

        for (entityPrimitive = 0; entityPrimitive < mapEnt.GetNumPrimitives(); entityPrimitive++) {
            prim = mapEnt.GetPrimitive(entityPrimitive);

            if (prim.GetType() == idMapPrimitive.TYPE_BRUSH) {
                ParseBrush((idMapBrush) prim, entityPrimitive);
            } else if (prim.GetType() == idMapPrimitive.TYPE_PATCH) {
                ParsePatch((idMapPatch) prim, entityPrimitive);
            }
        }

        // never put an origin on the world, even if the editor left one there
        if (dmapGlobals.num_entities != 1) {
            uEntity.mapEntity.epairs.GetVector("origin", "", uEntity.origin);
        }

        return true;
    }

//===================================================================

    /*
     ==============
     CreateMapLight

     ==============
     */
    static void CreateMapLight(final idMapEntity mapEnt) {
        mapLight_t light;
        boolean[] dynamic = {false};

        // designers can add the "noPrelight" flag to signal that
        // the lights will move around, so we don't want
        // to bother chopping up the surfaces under it or creating
        // shadow volumes
        mapEnt.epairs.GetBool("noPrelight", "0", dynamic);
        if (dynamic[0]) {
            return;
        }

        light = new mapLight_t();
        light.name[0] = '\0';
        light.shadowTris = null;

        // parse parms exactly as the game do
        // use the game's epair parsing code so
        // we can use the same renderLight generation
        GameEdit.gameEdit.ParseSpawnArgsToRenderLight(mapEnt.epairs, light.def.parms);

        R_DeriveLightData(light.def);

        // get the name for naming the shadow surfaces
        String[] name = {null};

        mapEnt.epairs.GetString("name", "", name);

        idStr.Copynz(light.name, name[0], light.name.length);
        if (NOT(light.name[0])) {
            common.Error("Light at (%f,%f,%f) didn't have a name",
                    light.def.parms.origin.oGet(0), light.def.parms.origin.oGet(1), light.def.parms.origin.oGet(2));
        }
//#if 0
//	// use the renderer code to get the bounding planes for the light
//	// based on all the parameters
//	R_RenderLightFrustum( light.parms, light.frustum );
//	light.lightShader = light.parms.shader;
//#endif

        dmapGlobals.mapLights.Append(light);

    }

    /*
     ==============
     CreateMapLights

     ==============
     */
    static void CreateMapLights(final idMapFile dmapFile) {
        int i;
        idMapEntity mapEnt;
        String[] value = {null};

        for (i = 0; i < dmapFile.GetNumEntities(); i++) {
            mapEnt = dmapFile.GetEntity(i);
            mapEnt.epairs.GetString("classname", "", value);
            if (0 == idStr.Icmp(value[0], "light")) {
                CreateMapLight(mapEnt);
            }

        }

    }

    /*
     ================
     LoadDMapFile
     ================
     */
    static boolean LoadDMapFile(final String filename) {
        primitive_s prim;
        idBounds mapBounds = new idBounds();
        int brushes, triSurfs;
        int i;
        int size;

        common.Printf("--- LoadDMapFile ---\n");
        common.Printf("loading %s\n", filename);

        // load and parse the map file into canonical form
        dmapGlobals.dmapFile = new idMapFile();
        if (!dmapGlobals.dmapFile.Parse(filename)) {
//		delete dmapGlobals.dmapFile;
            dmapGlobals.dmapFile = null;
            common.Warning("Couldn't load map file: '%s'", filename);
            return false;
        }

        dmapGlobals.mapPlanes.Clear();
        dmapGlobals.mapPlanes.SetGranularity(1024);

        // process the canonical form into utility form
        dmapGlobals.num_entities = 0;
        c_numMapPatches = 0;
        c_areaportals = 0;

        size = dmapGlobals.dmapFile.GetNumEntities();//* sizeof(dmapGlobals.uEntities[0]);
        dmapGlobals.uEntities = new uEntity_t[size];// Mem_Alloc(size);
//	memset( dmapGlobals.uEntities, 0, size );

        // allocate a very large temporary brush for building
        // the brushes as they are loaded
        buildBrush = new uBrush_t();//AllocBrush(MAX_BUILD_SIDES);
        c_active_brushes++;

        for (i = 0; i < dmapGlobals.dmapFile.GetNumEntities(); i++) {
            ProcessMapEntity(dmapGlobals.dmapFile.GetEntity(i));
        }

        CreateMapLights(dmapGlobals.dmapFile);

        brushes = 0;
        triSurfs = 0;

        mapBounds.Clear();
        for (prim = dmapGlobals.uEntities[0].primitives; prim != null; prim = prim.next) {
            if (prim.brush != null) {
                brushes++;
                mapBounds.AddBounds(prim.brush.bounds);
            } else if (prim.tris != null) {
                triSurfs++;
            }
        }

        common.Printf("%5d total world brushes\n", brushes);
        common.Printf("%5d total world triSurfs\n", triSurfs);
        common.Printf("%5d patches\n", c_numMapPatches);
        common.Printf("%5d entities\n", dmapGlobals.num_entities);
        common.Printf("%5d planes\n", dmapGlobals.mapPlanes.Num());
        common.Printf("%5d areaportals\n", c_areaportals);
        common.Printf("size: %5.0f,%5.0f,%5.0f to %5.0f,%5.0f,%5.0f\n",
                mapBounds.oGet(0, 0), mapBounds.oGet(0, 1), mapBounds.oGet(0, 2),
                mapBounds.oGet(1, 0), mapBounds.oGet(1, 1), mapBounds.oGet(1, 2));

        return true;
    }

    /*
     ================
     FreeOptimizeGroupList
     ================
     */
    static void FreeOptimizeGroupList(optimizeGroup_s groups) {
        optimizeGroup_s next;

        for (; groups != null; groups = next) {
            next = groups.nextGroup;
            FreeTriList(groups.triList);
            groups.clear();//Mem_Free(groups);
        }
    }

    /*
     ================
     FreeDMapFile
     ================
     */
    static void FreeDMapFile() {
        int i, j;

//        FreeBrush(buildBrush);
        buildBrush = null;

        // free the entities and brushes
        for (i = 0; i < dmapGlobals.num_entities; i++) {
            uEntity_t ent;
            primitive_s prim, nextPrim;

            ent = dmapGlobals.uEntities[i];

            FreeTree(ent.tree);

            // free primitives
            for (prim = ent.primitives; prim != null; prim = nextPrim) {
                nextPrim = prim.next;
                if (prim.brush != null) {
//                    FreeBrush(prim.brush);
                    prim.brush = null;
                }
                if (prim.tris != null) {
                    FreeTriList(prim.tris);
                }
                prim = null;//Mem_Free(prim);
            }

            // free area surfaces
            if (ent.areas != null) {
                for (j = 0; j < ent.numAreas; j++) {
                    uArea_t area;

                    area = ent.areas[j];
                    FreeOptimizeGroupList(area.groups);

                }
                ent.areas = null;//Mem_Free(ent.areas);
            }
        }

        dmapGlobals.uEntities = null;//Mem_Free(dmapGlobals.uEntities);

        dmapGlobals.num_entities = 0;

        // free the map lights
        for (i = 0; i < dmapGlobals.mapLights.Num(); i++) {
            R_FreeLightDefDerivedData(dmapGlobals.mapLights.oGet(i).def);
        }
        dmapGlobals.mapLights.DeleteContents(true);
    }
}
