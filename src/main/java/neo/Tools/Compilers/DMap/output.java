package neo.Tools.Compilers.DMap;

import static neo.Renderer.RenderWorld.PROC_FILE_EXT;
import static neo.Renderer.RenderWorld.PROC_FILE_ID;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_CreateSilIndexes;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurfSilIndexes;
import static neo.Renderer.tr_trisurf.R_RangeCheckIndexes;
import static neo.Renderer.tr_trisurf.R_RemoveDegenerateTriangles;
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.Tools.Compilers.DMap.dmap.PLANENUM_LEAF;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import static neo.Tools.Compilers.DMap.facebsp.FreeTreePortals_r;
import static neo.Tools.Compilers.DMap.facebsp.FreeTree_r;
import static neo.Tools.Compilers.DMap.portals.interAreaPortals;
import static neo.Tools.Compilers.DMap.portals.numInterAreaPortals;
import static neo.Tools.Compilers.DMap.tritools.CopyTriList;
import static neo.Tools.Compilers.DMap.tritools.CountTriList;
import static neo.Tools.Compilers.DMap.tritools.FreeTriList;
import static neo.Tools.Compilers.DMap.tritools.MergeTriLists;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.math.Vector.DotProduct;

import neo.Renderer.Model.srfTriangles_s;
import neo.Tools.Compilers.DMap.dmap.mapLight_t;
import neo.Tools.Compilers.DMap.dmap.mapTri_s;
import neo.Tools.Compilers.DMap.dmap.node_s;
import neo.Tools.Compilers.DMap.dmap.optimizeGroup_s;
import neo.Tools.Compilers.DMap.dmap.uArea_t;
import neo.Tools.Compilers.DMap.dmap.uEntity_t;
import neo.Tools.Compilers.DMap.portals.interAreaPortal_t;
import neo.framework.File_h.idFile;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;

/**
 *
 */
public class output {

    //=================================================================================
//#if 0
//
//should we try and snap values very close to 0.5, 0.25, 0.125, etc?
//
//  do we write out normals, or just a "smooth shade" flag?
//resolved: normals.  otherwise adjacent facet shaded surfaces get their
//		  vertexes merged, and they would have to be split apart before drawing
//
//  do we save out "wings" for shadow silhouette info?
//
//
//#endif
    static idFile procFile;
    //
    static final int    AREANUM_DIFFERENT = -2;
    //
    static final double XYZ_EPSILON       = 0.01;
    static final double ST_EPSILON        = 0.001;
    static final double COSINE_EPSILON    = 0.999;


    /*
     =============
     PruneNodes_r

     Any nodes that have all children with the same
     area can be combined into a single leaf node

     Returns the area number of all children, or
     AREANUM_DIFFERENT if not the same.
     =============
     */
    static int PruneNodes_r(node_s node) {
        int a1, a2;

        if (node.planenum == PLANENUM_LEAF) {
            return node.area;
        }

        a1 = PruneNodes_r(node.children[0]);
        a2 = PruneNodes_r(node.children[1]);

        if (a1 != a2 || a1 == AREANUM_DIFFERENT) {
            return AREANUM_DIFFERENT;
        }

        // free all the nodes below this point
        FreeTreePortals_r(node.children[0]);
        FreeTreePortals_r(node.children[1]);
        FreeTree_r(node.children[0]);
        FreeTree_r(node.children[1]);

        // change this node to a leaf
        node.planenum = PLANENUM_LEAF;
        node.area = a1;

        return a1;
    }

    static void WriteFloat(idFile f, float v) {
        if (idMath.Fabs(v - idMath.Rint(v)) < 0.001) {
            f.WriteFloatString("%d ", (int) idMath.Rint(v));
        } else {
            f.WriteFloatString("%f ", v);
        }
    }

    static void Write1DMatrix(idFile f, int x, float[] m) {
        int i;

        f.WriteFloatString("( ");

        for (i = 0; i < x; i++) {
            WriteFloat(f, m[i]);
        }

        f.WriteFloatString(") ");
    }

    static int CountUniqueShaders(optimizeGroup_s groups) {
        optimizeGroup_s a, b;
        int count;

        count = 0;

        for (a = groups; a != null; a = a.nextGroup) {
            if (NOT(a.triList)) {    // ignore groups with no tris
                continue;
            }
            for (b = groups; b != a; b = b.nextGroup) {
                if (NOT(b.triList)) {
                    continue;
                }
                if (a.material != b.material) {
                    continue;
                }
                if (a.mergeGroup != b.mergeGroup) {
                    continue;
                }
                break;
            }
            if (a == b) {
                count++;
            }
        }

        return count;
    }


    /*
     ==============
     MatchVert
     ==============
     */
    static boolean MatchVert(final idDrawVert a, final idDrawVert b) {
        if (idMath.Fabs(a.xyz.oGet(0) - b.xyz.oGet(0)) > XYZ_EPSILON) {
            return false;
        }
        if (idMath.Fabs(a.xyz.oGet(1) - b.xyz.oGet(1)) > XYZ_EPSILON) {
            return false;
        }
        if (idMath.Fabs(a.xyz.oGet(2) - b.xyz.oGet(2)) > XYZ_EPSILON) {
            return false;
        }
        if (idMath.Fabs(a.st.oGet(0) - b.st.oGet(0)) > ST_EPSILON) {
            return false;
        }
        if (idMath.Fabs(a.st.oGet(1) - b.st.oGet(1)) > ST_EPSILON) {
            return false;
        }

        // if the normal is 0 (smoothed normals), consider it a match
        if (a.normal.oGet(0) == 0 && a.normal.oGet(1) == 0 && a.normal.oGet(2) == 0
                && b.normal.oGet(0) == 0 && b.normal.oGet(1) == 0 && b.normal.oGet(2) == 0) {
            return true;
        }

        // otherwise do a dot-product cosine check
        if (DotProduct(a.normal, b.normal) < COSINE_EPSILON) {
            return false;
        }

        return true;
    }

    /*
     ====================
     ShareMapTriVerts

     Converts independent triangles to shared vertex triangles
     ====================
     */
    static srfTriangles_s ShareMapTriVerts(final mapTri_s tris) {
        mapTri_s step;
        int count;
        int i, j;
        int numVerts;
        int numIndexes;
        srfTriangles_s uTri;

        // unique the vertexes
        count = CountTriList(tris);

        uTri = R_AllocStaticTriSurf();
        R_AllocStaticTriSurfVerts(uTri, count * 3);
        R_AllocStaticTriSurfIndexes(uTri, count * 3);

        numVerts = 0;
        numIndexes = 0;

        for (step = tris; step != null; step = step.next) {
            for (i = 0; i < 3; i++) {
                idDrawVert dv;

                dv = step.v[i];

                // search for a match
                for (j = 0; j < numVerts; j++) {
                    if (MatchVert(uTri.verts[j], dv)) {
                        break;
                    }
                }
                if (j == numVerts) {
                    numVerts++;
                    uTri.verts[j].xyz = dv.xyz;
                    uTri.verts[j].normal = dv.normal;
                    uTri.verts[j].st.oSet(0, dv.st.oGet(0));
                    uTri.verts[j].st.oSet(1, dv.st.oGet(1));
                }

                uTri.getIndexes().getValues().put(numIndexes++, j);
            }
        }

        uTri.numVerts = numVerts;
        uTri.getIndexes().setNumValues(numIndexes);

        return uTri;
    }

    /*
     ==================
     CleanupUTriangles
     ==================
     */
    static void CleanupUTriangles(srfTriangles_s tri) {
        // perform cleanup operations

        R_RangeCheckIndexes(tri);
        R_CreateSilIndexes(tri);
//	R_RemoveDuplicatedTriangles( tri );	// this may remove valid overlapped transparent triangles
        R_RemoveDegenerateTriangles(tri);
//	R_RemoveUnusedVerts( tri );

        R_FreeStaticTriSurfSilIndexes(tri);
    }

    /*
     ====================
     WriteUTriangles

     Writes text verts and indexes to procfile
     ====================
     */
    static void WriteUTriangles(final srfTriangles_s uTris) {
        int col;
        int i;

        // emit this chain
        procFile.WriteFloatString("/* numVerts = */ %d /* numIndexes = */ %d\n",
                uTris.numVerts, uTris.getIndexes().getNumValues());

        // verts
        col = 0;
        for (i = 0; i < uTris.numVerts; i++) {
            final float[] vec = new float[8];
            final idDrawVert dv;

            dv = uTris.verts[i];

            vec[0] = dv.xyz.oGet(0);
            vec[1] = dv.xyz.oGet(1);
            vec[2] = dv.xyz.oGet(2);
            vec[3] = dv.st.oGet(0);
            vec[4] = dv.st.oGet(1);
            vec[5] = dv.normal.oGet(0);
            vec[6] = dv.normal.oGet(1);
            vec[7] = dv.normal.oGet(2);
            Write1DMatrix(procFile, 8, vec);

            if (++col == 3) {
                col = 0;
                procFile.WriteFloatString("\n");
            }
        }
        if (col != 0) {
            procFile.WriteFloatString("\n");
        }

        // indexes
        col = 0;
        for (i = 0; i < uTris.getIndexes().getNumValues(); i++) {
            procFile.WriteFloatString("%d ", uTris.getIndexes().getValues().get(i));

            if (++col == 18) {
                col = 0;
                procFile.WriteFloatString("\n");
            }
        }
        if (col != 0) {
            procFile.WriteFloatString("\n");
        }
    }


    /*
     ====================
     WriteShadowTriangles

     Writes text verts and indexes to procfile
     ====================
     */
    static void WriteShadowTriangles(final srfTriangles_s tri) {
        int col;
        int i;

        // emit this chain
        procFile.WriteFloatString("/* numVerts = */ %d /* noCaps = */ %d /* noFrontCaps = */ %d /* numIndexes = */ %d /* planeBits = */ %d\n",
                tri.numVerts, tri.numShadowIndexesNoCaps, tri.numShadowIndexesNoFrontCaps, tri.getIndexes().getNumValues(), tri.shadowCapPlaneBits);

        // verts
        col = 0;
        for (i = 0; i < tri.numVerts; i++) {
            Write1DMatrix(procFile, 3, tri.shadowVertexes[i].xyz.ToFloatPtr());

            if (++col == 5) {
                col = 0;
                procFile.WriteFloatString("\n");
            }
        }
        if (col != 0) {
            procFile.WriteFloatString("\n");
        }

        // indexes
        col = 0;
        for (i = 0; i < tri.getIndexes().getNumValues(); i++) {
            procFile.WriteFloatString("%d ", tri.getIndexes().getValues().get(i));

            if (++col == 18) {
                col = 0;
                procFile.WriteFloatString("\n");
            }
        }
        if (col != 0) {
            procFile.WriteFloatString("\n");
        }
    }


    /*
     =======================
     GroupsAreSurfaceCompatible

     Planes, texcoords, and groupLights can differ,
     but the material and mergegroup must match
     =======================
     */
    static boolean GroupsAreSurfaceCompatible(final optimizeGroup_s a, final optimizeGroup_s b) {
        if (!a.material.equals(b.material)) {
            return false;
        }
        if (!a.mergeGroup.equals(b.mergeGroup)) {
            return false;
        }
        return true;
    }

    /*
     ====================
     WriteOutputSurfaces
     ====================
     */
    static void WriteOutputSurfaces(int entityNum, int areaNum) {
        mapTri_s ambient, copy;
        int surfaceNum;
        int numSurfaces;
        idMapEntity entity;
        uArea_t area;
        optimizeGroup_s group, groupStep;
        int i; // , j;
//	int			col;
        srfTriangles_s uTri;
//	mapTri_s	*tri;
        class interactionTris_s {

            interactionTris_s next;
            mapTri_s triList;
            mapLight_t light;
        }

        interactionTris_s interactions, checkInter; //, *nextInter;

        area = dmapGlobals.uEntities[entityNum].areas[areaNum];
        entity = dmapGlobals.uEntities[entityNum].mapEntity;

        numSurfaces = CountUniqueShaders(area.groups);

        if (entityNum == 0) {
            procFile.WriteFloatString("model { /* name = */ \"_area%d\" /* numSurfaces = */ %d\n\n",
                    areaNum, numSurfaces);
        } else {
            final String[] name = {null};

            entity.epairs.GetString("name", "", name);
            if (isNotNullOrEmpty(name[0])) {
                common.Error("Entity %d has surfaces, but no name key", entityNum);
            }
            procFile.WriteFloatString("model { /* name = */ \"%s\" /* numSurfaces = */ %d\n\n",
                    name, numSurfaces);
        }

        surfaceNum = 0;
        for (group = area.groups; group != null; group = group.nextGroup) {
            if (group.surfaceEmited) {
                continue;
            }

            // combine all groups compatible with this one
            // usually several optimizeGroup_s can be combined into a single
            // surface, even though they couldn't be merged together to save
            // vertexes because they had different planes, texture coordinates, or lights.
            // Different mergeGroups will stay in separate surfaces.
            ambient = null;

            // each light that illuminates any of the groups in the surface will
            // get its own list of indexes out of the original surface
            interactions = null;

            for (groupStep = group; groupStep != null; groupStep = groupStep.nextGroup) {
                if (groupStep.surfaceEmited) {
                    continue;
                }
                if (!GroupsAreSurfaceCompatible(group, groupStep)) {
                    continue;
                }

                // copy it out to the ambient list
                copy = CopyTriList(groupStep.triList);
                ambient = MergeTriLists(ambient, copy);
                groupStep.surfaceEmited = true;

                // duplicate it into an interaction for each groupLight
                for (i = 0; i < groupStep.numGroupLights; i++) {
                    for (checkInter = interactions; checkInter != null; checkInter = checkInter.next) {
                        if (checkInter.light == groupStep.groupLights[i]) {
                            break;
                        }
                    }
                    if (NOT(checkInter)) {
                        // create a new interaction
                        checkInter = new interactionTris_s();// Mem_ClearedAlloc(sizeof(checkInter));
                        checkInter.light = groupStep.groupLights[i];
                        checkInter.next = interactions;
                        interactions = checkInter;
                    }
                    copy = CopyTriList(groupStep.triList);
                    checkInter.triList = MergeTriLists(checkInter.triList, copy);
                }
            }

            if (NOT(ambient)) {
                continue;
            }

            if (surfaceNum >= numSurfaces) {
                common.Error("WriteOutputSurfaces: surfaceNum >= numSurfaces");
            }

            procFile.WriteFloatString("/* surface %d */ { ", surfaceNum);
            surfaceNum++;
            procFile.WriteFloatString("\"%s\" ", ambient.material.GetName());

            uTri = ShareMapTriVerts(ambient);
            FreeTriList(ambient);

            CleanupUTriangles(uTri);
            WriteUTriangles(uTri);
            R_FreeStaticTriSurf(uTri);

            procFile.WriteFloatString("}\n\n");
        }

        procFile.WriteFloatString("}\n\n");
    }

    /*
     ===============
     WriteNode_r

     ===============
     */
    static void WriteNode_r(node_s node) {
        final int[] child = new int[2];
        int i;
        idPlane plane;

        if (node.planenum == PLANENUM_LEAF) {
            // we shouldn't get here unless the entire world
            // was a single leaf
            procFile.WriteFloatString("/* node 0 */ ( 0 0 0 0 ) -1 -1\n");
            return;
        }

        for (i = 0; i < 2; i++) {
            if (node.children[i].planenum == PLANENUM_LEAF) {
                child[i] = -1 - node.children[i].area;
            } else {
                child[i] = node.children[i].nodeNumber;
            }
        }

        plane = dmapGlobals.mapPlanes.oGet(node.planenum);

        procFile.WriteFloatString("/* node %d */ ", node.nodeNumber);
        Write1DMatrix(procFile, 4, plane.ToFloatPtr());
        procFile.WriteFloatString("%d %d\n", child[0], child[1]);

        if (child[0] > 0) {
            WriteNode_r(node.children[0]);
        }
        if (child[1] > 0) {
            WriteNode_r(node.children[1]);
        }
    }

    static int NumberNodes_r(node_s node, int nextNumber) {
        if (node.planenum == PLANENUM_LEAF) {
            return nextNumber;
        }
        node.nodeNumber = nextNumber;
        nextNumber++;
        nextNumber = NumberNodes_r(node.children[0], nextNumber);
        nextNumber = NumberNodes_r(node.children[1], nextNumber);

        return nextNumber;
    }

    /*
     ====================
     WriteOutputNodes
     ====================
     */
    static void WriteOutputNodes(node_s node) {
        int numNodes;

        // prune unneeded nodes and count
        PruneNodes_r(node);
        numNodes = NumberNodes_r(node, 0);

        // output
        procFile.WriteFloatString("nodes { /* numNodes = */ %d\n\n", numNodes);
        procFile.WriteFloatString("/* node format is: ( planeVector ) positiveChild negativeChild */\n");
        procFile.WriteFloatString("/* a child number of 0 is an opaque, solid area */\n");
        procFile.WriteFloatString("/* negative child numbers are areas: (-1-child) */\n");

        WriteNode_r(node);

        procFile.WriteFloatString("}\n\n");
    }

    /*
     ====================
     WriteOutputPortals
     ====================
     */
    static void WriteOutputPortals(uEntity_t e) {
        int i, j;
        interAreaPortal_t iap;
        idWinding w;

        procFile.WriteFloatString("interAreaPortals { /* numAreas = */ %d /* numIAP = */ %d\n\n",
                e.numAreas, numInterAreaPortals);
        procFile.WriteFloatString("/* interAreaPortal format is: numPoints positiveSideArea negativeSideArea ( point) ... */\n");
        for (i = 0; i < numInterAreaPortals; i++) {
            iap = interAreaPortals[i];
            w = iap.side.winding;
            procFile.WriteFloatString("/* iap %d */ %d %d %d ", i, w.GetNumPoints(), iap.area0, iap.area1);
            for (j = 0; j < w.GetNumPoints(); j++) {
                Write1DMatrix(procFile, 3, w.oGet(j).ToFloatPtr());
            }
            procFile.WriteFloatString("\n");
        }

        procFile.WriteFloatString("}\n\n");
    }


    /*
     ====================
     WriteOutputEntity
     ====================
     */
    static void WriteOutputEntity(int entityNum) {
        int i;
        uEntity_t e;

        e = dmapGlobals.uEntities[entityNum];

        if (entityNum != 0) {
            // entities may have enclosed, empty areas that we don't need to write out
            if (e.numAreas > 1) {
                e.numAreas = 1;
            }
        }

        for (i = 0; i < e.numAreas; i++) {
            WriteOutputSurfaces(entityNum, i);
        }

        // we will completely skip the portals and nodes if it is a single area
        if ((entityNum == 0) && (e.numAreas > 1)) {
            // output the area portals
            WriteOutputPortals(e);

            // output the nodes
            WriteOutputNodes(e.tree.headnode);
        }
    }


    /*
     ====================
     WriteOutputFile
     ====================
     */
    static void WriteOutputFile() {
        int i;
        uEntity_t entity;
        String qpath;

        // write the file
        common.Printf("----- WriteOutputFile -----\n");

        qpath = String.format("%s." + PROC_FILE_EXT, dmapGlobals.mapFileBase);

        common.Printf("writing %s\n", qpath);
        // _D3XP used fs_cdpath
        procFile = fileSystem.OpenFileWrite(qpath, "fs_devpath");
        if (NOT(procFile)) {
            common.Error("Error opening %s", qpath);
        }

        procFile.WriteFloatString("%s\n\n", PROC_FILE_ID);

        // write the entity models and information, writing entities first
        for (i = dmapGlobals.num_entities - 1; i >= 0; i--) {
            entity = dmapGlobals.uEntities[i];

            if (NOT(entity.primitives)) {
                continue;
            }

            WriteOutputEntity(i);
        }

        // write the shadow volumes
        for (i = 0; i < dmapGlobals.mapLights.Num(); i++) {
            final mapLight_t light = dmapGlobals.mapLights.oGet(i);
            if (NOT(light.shadowTris)) {
                continue;
            }

            procFile.WriteFloatString("shadowModel { /* name = */ \"_prelight_%s\"\n\n", light.name);
            WriteShadowTriangles(light.shadowTris);
            procFile.WriteFloatString("}\n\n");

            R_FreeStaticTriSurf(light.shadowTris);
            light.shadowTris = null;
        }

        fileSystem.CloseFile(procFile);
    }
}
