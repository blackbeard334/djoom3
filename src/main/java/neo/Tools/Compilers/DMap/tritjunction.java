package neo.Tools.Compilers.DMap;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.TempDump.NOT;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.Tools.Compilers.DMap.dmap.dmapGlobals;
import neo.Tools.Compilers.DMap.dmap.mapTri_s;
import neo.Tools.Compilers.DMap.dmap.optimizeGroup_s;
import neo.Tools.Compilers.DMap.dmap.uEntity_t;
import neo.Tools.Compilers.DMap.tritjunction.hashVert_s;
import static neo.Tools.Compilers.DMap.tritools.CopyMapTri;
import static neo.Tools.Compilers.DMap.tritools.CountTriList;
import static neo.Tools.Compilers.DMap.tritools.FreeTri;
import static neo.Tools.Compilers.DMap.tritools.FreeTriList;
import static neo.Tools.Compilers.DMap.tritools.MergeTriLists;
import static neo.framework.Common.common;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Plane.idPlane;
import static neo.idlib.math.Vector.DotProduct;
import static neo.idlib.math.Vector.VectorCopy;
import static neo.idlib.math.Vector.VectorMA;
import static neo.idlib.math.Vector.VectorSubtract;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class tritjunction {

    /*

     T junction fixing never creates more xyz points, but
     new vertexes will be created when different surfaces
     cause a fix

     The vertex cleaning accomplishes two goals: removing extranious low order
     bits to avoid numbers like 1.000001233, and grouping nearby vertexes
     together.  Straight truncation accomplishes the first foal, but two vertexes
     only a tiny epsilon apart could still be spread to different snap points.
     To avoid this, we allow the merge test to group points together that
     snapped to neighboring integer coordinates.

     Snaping verts can drag some triangles backwards or collapse them to points,
     which will cause them to be removed.
  

     When snapping to ints, a point can move a maximum of sqrt(3)/2 distance
     Two points that were an epsilon apart can then become sqrt(3) apart

     A case that causes recursive overflow with point to triangle fixing:

     ///////////A
     C            D
     ///////////B

     Triangle ABC tests against point D and splits into triangles ADC and DBC
     Triangle DBC then tests against point A again and splits into ABC and ADB
     infinite recursive loop


     For a given source triangle
     init the no-check list to hold the three triangle hashVerts

     recursiveFixTriAgainstHash

     recursiveFixTriAgainstHashVert_r
     if hashVert is on the no-check list
     exit
     if the hashVert should split the triangle
     add to the no-check list
     recursiveFixTriAgainstHash(a)
     recursiveFixTriAgainstHash(b)

     */
    static final int    SNAP_FRACTIONS   = 32;
    //#define	SNAP_FRACTIONS	8
    //#define	SNAP_FRACTIONS	1
    //
    static final double VERTEX_EPSILON   = (1.0 / SNAP_FRACTIONS);
    //
    static final double COLINEAR_EPSILON = (1.8 * VERTEX_EPSILON);
    //
    static final int    HASH_BINS        = 16;

    static class hashVert_s {

        hashVert_s next;
        idVec3     v;
        int[] iv = new int[3];

        public static void memset(final hashVert_s[][][] hashVerts) {
            for (int a = 0; a < HASH_BINS; a++) {
                for (int b = 0; b < HASH_BINS; b++) {
                    for (int c = 0; c < HASH_BINS; c++) {
                        hashVerts[a][b][c] = null;
                    }
                }
            }
        }
    };
    static idBounds hashBounds;
    static idVec3   hashScale;
    static final hashVert_s[][][] hashVerts = new hashVert_s[HASH_BINS][HASH_BINS][HASH_BINS];
    static int numHashVerts, numTotalVerts;
    static final int[] hashIntMins = new int[3], hashIntScale = new int[3];

    /*
     ===============
     GetHashVert

     Also modifies the original vert to the snapped value
     ===============
     */
    static hashVert_s GetHashVert(idVec3 v) {
        int[] iv = new int[3];
        int[] block = new int[3];
        int i;
        hashVert_s hv;

        numTotalVerts++;

        // snap the vert to integral values
        for (i = 0; i < 3; i++) {
            iv[i] = (int) floor((v.oGet(i) + 0.5 / SNAP_FRACTIONS) * SNAP_FRACTIONS);
            block[i] = (iv[i] - hashIntMins[i]) / hashIntScale[i];
            if (block[i] < 0) {
                block[i] = 0;
            } else if (block[i] >= HASH_BINS) {
                block[i] = HASH_BINS - 1;
            }
        }

        // see if a vertex near enough already exists
        // this could still fail to find a near neighbor right at the hash block boundary
        for (hv = hashVerts[block[0]][block[1]][block[2]]; hv != null; hv = hv.next) {
//#if 0
//		if ( hv.iv[0] == iv[0] && hv.iv[1] == iv[1] && hv.iv[2] == iv[2] ) {
//			VectorCopy( hv.v, v );
//			return hv;
//		}
//#else
            for (i = 0; i < 3; i++) {
                int d;
                d = hv.iv[i] - iv[i];
                if (d < -1 || d > 1) {
                    break;
                }
            }
            if (i == 3) {
                VectorCopy(hv.v, v);
                return hv;
            }
//#endif
        }

        // create a new one 
        hv = new hashVert_s();// Mem_Alloc(sizeof(hv));

        hv.next = hashVerts[block[0]][block[1]][block[2]];
        hashVerts[block[0]][block[1]][block[2]] = hv;

        hv.iv[0] = iv[0];
        hv.iv[1] = iv[1];
        hv.iv[2] = iv[2];

        hv.v.oSet(0, (float) iv[0] / SNAP_FRACTIONS);
        hv.v.oSet(1, (float) iv[1] / SNAP_FRACTIONS);
        hv.v.oSet(2, (float) iv[2] / SNAP_FRACTIONS);

        VectorCopy(hv.v, v);

        numHashVerts++;

        return hv;
    }


    /*
     ==================
     HashBlocksForTri

     Returns an inclusive bounding box of hash
     bins that should hold the triangle
     ==================
     */
    static void HashBlocksForTri(final mapTri_s tri, int[][] blocks/*[2][3]*/) {
        idBounds bounds = new idBounds();
        int i;

        bounds.Clear();
        bounds.AddPoint(tri.v[0].xyz);
        bounds.AddPoint(tri.v[1].xyz);
        bounds.AddPoint(tri.v[2].xyz);

        // add a 1.0 slop margin on each side
        for (i = 0; i < 3; i++) {
            blocks[0][i] = (int) ((bounds.oGet(0, i) - 1.0 - hashBounds.oGet(0, i)) / hashScale.oGet(i));
            if (blocks[0][i] < 0) {
                blocks[0][i] = 0;
            } else if (blocks[0][i] >= HASH_BINS) {
                blocks[0][i] = HASH_BINS - 1;
            }

            blocks[1][i] = (int) ((bounds.oGet(1, i) + 1.0 - hashBounds.oGet(0, i)) / hashScale.oGet(i));
            if (blocks[1][i] < 0) {
                blocks[1][i] = 0;
            } else if (blocks[1][i] >= HASH_BINS) {
                blocks[1][i] = HASH_BINS - 1;
            }
        }
    }


    /*
     =================
     HashTriangles

     Removes triangles that are degenerated or flipped backwards
     =================
     */
    static void HashTriangles(optimizeGroup_s groupList) {
        mapTri_s a;
        int vert;
        int i;
        optimizeGroup_s group;

        // clear the hash tables
//        memset(hashVerts, 0, sizeof(hashVerts));
        hashVert_s.memset(hashVerts);

        numHashVerts = 0;
        numTotalVerts = 0;

        // bound all the triangles to determine the bucket size
        hashBounds.Clear();
        for (group = groupList; group != null; group = group.nextGroup) {
            for (a = group.triList; a != null; a = a.next) {
                hashBounds.AddPoint(a.v[0].xyz);
                hashBounds.AddPoint(a.v[1].xyz);
                hashBounds.AddPoint(a.v[2].xyz);
            }
        }

        // spread the bounds so it will never have a zero size
        for (i = 0; i < 3; i++) {
            hashBounds.oSet(0, i, (float) floor(hashBounds.oGet(0, i) - 1));
            hashBounds.oSet(1, i, (float) ceil(hashBounds.oGet(1, i) + 1));
            hashIntMins[i] = (int) (hashBounds.oGet(0, i) * SNAP_FRACTIONS);

            hashScale.oSet(i, (hashBounds.oGet(1, i) - hashBounds.oGet(0, i)) / HASH_BINS);
            hashIntScale[i] = (int) (hashScale.oGet(i) * SNAP_FRACTIONS);
            if (hashIntScale[i] < 1) {
                hashIntScale[i] = 1;
            }
        }

        // add all the points to the hash buckets
        for (group = groupList; group != null; group = group.nextGroup) {
            // don't create tjunctions against discrete surfaces (blood decals, etc)
            if (group.material != null && group.material.IsDiscrete()) {
                continue;
            }
            for (a = group.triList; a != null; a = a.next) {
                for (vert = 0; vert < 3; vert++) {
                    a.hashVert[vert] = GetHashVert(a.v[vert].xyz);
                }
            }
        }
    }

    /*
     =================
     FreeTJunctionHash

     The optimizer may add some more crossing verts
     after t junction processing
     =================
     */
    static void FreeTJunctionHash() {
        int i, j, k;
        hashVert_s hv, next;

        for (i = 0; i < HASH_BINS; i++) {
            for (j = 0; j < HASH_BINS; j++) {
                for (k = 0; k < HASH_BINS; k++) {
                    for (hv = hashVerts[i][j][k]; hv != null; hv = next) {
                        next = hv.next;
                        hv = hashVerts[i][j][k] = null;//Mem_Free(hv);
                    }
                }
            }
        }
//        memset(hashVerts, 0, sizeof(hashVerts));
        hashVert_s.memset(hashVerts);
    }


    /*
     ==================
     FixTriangleAgainstHashVert

     Returns a list of two new mapTri if the hashVert is
     on an edge of the given mapTri, otherwise returns NULL.
     ==================
     */
    static mapTri_s FixTriangleAgainstHashVert(final mapTri_s a, final hashVert_s hv) {
        int i;
        idDrawVert v1, v2, v3;
        idDrawVert split = new idDrawVert();
        idVec3 dir = new idVec3();
        float len;
        float frac;
        mapTri_s new1, new2;
        idVec3 temp = new idVec3();
        float d, off;
        idVec3 v;
        idPlane plane1 = new idPlane(), plane2 = new idPlane();

        v = hv.v;

        // if the triangle already has this hashVert as a vert,
        // it can't be split by it
        if (a.hashVert[0].equals(hv) || a.hashVert[1].equals(hv) || a.hashVert[2].equals(hv)) {
            return null;
        }

        // we probably should find the edge that the vertex is closest to.
        // it is possible to be < 1 unit away from multiple
        // edges, but we only want to split by one of them
        for (i = 0; i < 3; i++) {
            v1 = a.v[i];
            v2 = a.v[(i + 1) % 3];
            v3 = a.v[(i + 2) % 3];
            VectorSubtract(v2.xyz, v1.xyz, dir);
            len = dir.Normalize();

            // if it is close to one of the edge vertexes, skip it
            VectorSubtract(v, v1.xyz, temp);
            d = DotProduct(temp, dir);
            if (d <= 0 || d >= len) {
                continue;
            }

            // make sure it is on the line
            VectorMA(v1.xyz, d, dir, temp);
            VectorSubtract(temp, v, temp);
            off = temp.Length();
            if (off <= -COLINEAR_EPSILON || off >= COLINEAR_EPSILON) {
                continue;
            }

            // take the x/y/z from the splitter,
            // but interpolate everything else from the original tri
            VectorCopy(v, split.xyz);
            frac = d / len;
            split.st.oSet(0, v1.st.oGet(0) + frac * (v2.st.oGet(0) - v1.st.oGet(0)));
            split.st.oSet(1, v1.st.oGet(1) + frac * (v2.st.oGet(1) - v1.st.oGet(1)));
            split.normal.oSet(0, v1.normal.oGet(0) + frac * (v2.normal.oGet(0) - v1.normal.oGet(0)));
            split.normal.oSet(1, v1.normal.oGet(1) + frac * (v2.normal.oGet(1) - v1.normal.oGet(1)));
            split.normal.oSet(2, v1.normal.oGet(2) + frac * (v2.normal.oGet(2) - v1.normal.oGet(2)));
            split.normal.Normalize();

            // split the tri
            new1 = CopyMapTri(a);
            new1.v[(i + 1) % 3] = split;
            new1.hashVert[(i + 1) % 3] = hv;
            new1.next = null;

            new2 = CopyMapTri(a);
            new2.v[i] = split;
            new2.hashVert[i] = hv;
            new2.next = new1;

            plane1.FromPoints(new1.hashVert[0].v, new1.hashVert[1].v, new1.hashVert[2].v);
            plane2.FromPoints(new2.hashVert[0].v, new2.hashVert[1].v, new2.hashVert[2].v);

            d = DotProduct(plane1, plane2);

            // if the two split triangle's normals don't face the same way,
            // it should not be split
            if (d <= 0) {
                FreeTriList(new2);
                continue;
            }

            return new2;
        }

        return null;
    }

    /*
     ==================
     FixTriangleAgainstHash

     Potentially splits a triangle into a list of triangles based on tjunctions
     ==================
     */
    static mapTri_s FixTriangleAgainstHash(final mapTri_s tri) {
        mapTri_s fixed;
        mapTri_s a;
        mapTri_s test, next;
        int[][] blocks = new int[2][3];
        int i, j, k;
        hashVert_s hv;

        // if this triangle is degenerate after point snapping,
        // do nothing (this shouldn't happen, because they should
        // be removed as they are hashed)
        if (tri.hashVert[0] == tri.hashVert[1]
                || tri.hashVert[0] == tri.hashVert[2]
                || tri.hashVert[1] == tri.hashVert[2]) {
            return null;
        }

        fixed = CopyMapTri(tri);
        fixed.next = null;

        HashBlocksForTri(tri, blocks);
        for (i = blocks[0][0]; i <= blocks[1][0]; i++) {
            for (j = blocks[0][1]; j <= blocks[1][1]; j++) {
                for (k = blocks[0][2]; k <= blocks[1][2]; k++) {
                    for (hv = hashVerts[i][j][k]; hv != null; hv = hv.next) {
                        // fix all triangles in the list against this point
                        test = fixed;
                        fixed = null;
                        for (; test != null; test = next) {
                            next = test.next;
                            a = FixTriangleAgainstHashVert(test, hv);
                            if (a != null) {
                                // cut into two triangles
                                a.next.next = fixed;
                                fixed = a;
                                FreeTri(test);
                            } else {
                                test.next = fixed;
                                fixed = test;
                            }
                        }
                    }
                }
            }
        }

        return fixed;
    }


    /*
     ==================
     CountGroupListTris
     ==================
     */
    static int CountGroupListTris(final optimizeGroup_s groupList) {
        int c;

        c = 0;
        for (; groupList != null; groupList.oSet(groupList.nextGroup)) {
            c += CountTriList(groupList.triList);
        }

        return c;
    }

    /*
     ==================
     FixAreaGroupsTjunctions
     ==================
     */
    static void FixAreaGroupsTjunctions(optimizeGroup_s groupList) {
        mapTri_s tri;
        mapTri_s newList;
        mapTri_s fixed;
        int startCount, endCount;
        optimizeGroup_s group;

        if (dmapGlobals.noTJunc) {
            return;
        }

        if (NOT(groupList)) {
            return;
        }

        startCount = CountGroupListTris(groupList);

        if (dmapGlobals.verbose) {
            common.Printf("----- FixAreaGroupsTjunctions -----\n");
            common.Printf("%6i triangles in\n", startCount);
        }

        HashTriangles(groupList);

        for (group = groupList; group != null; group = group.nextGroup) {
            // don't touch discrete surfaces
            if (group.material != null && group.material.IsDiscrete()) {
                continue;
            }

            newList = null;
            for (tri = group.triList; tri != null; tri = tri.next) {
                fixed = FixTriangleAgainstHash(tri);
                newList = MergeTriLists(newList, fixed);
            }
            FreeTriList(group.triList);
            group.triList = newList;
        }

        endCount = CountGroupListTris(groupList);
        if (dmapGlobals.verbose) {
            common.Printf("%6i triangles out\n", endCount);
        }
    }


    /*
     ==================
     FixEntityTjunctions
     ==================
     */
    static void FixEntityTjunctions(uEntity_t e) {
        int i;

        for (i = 0; i < e.numAreas; i++) {
            FixAreaGroupsTjunctions(e.areas[i].groups);
            FreeTJunctionHash();
        }
    }

    /*
     ==================
     FixGlobalTjunctions
     ==================
     */
    static void FixGlobalTjunctions(uEntity_t e) {
        mapTri_s a;
        int vert;
        int i;
        optimizeGroup_s group;
        int areaNum;

        common.Printf("----- FixGlobalTjunctions -----\n");

        // clear the hash tables
//        memset(hashVerts, 0, sizeof(hashVerts));
        hashVert_s.memset(hashVerts);

        numHashVerts = 0;
        numTotalVerts = 0;

        // bound all the triangles to determine the bucket size
        hashBounds.Clear();
        for (areaNum = 0; areaNum < e.numAreas; areaNum++) {
            for (group = e.areas[areaNum].groups; group != null; group = group.nextGroup) {
                for (a = group.triList; a != null; a = a.next) {
                    hashBounds.AddPoint(a.v[0].xyz);
                    hashBounds.AddPoint(a.v[1].xyz);
                    hashBounds.AddPoint(a.v[2].xyz);
                }
            }
        }

        // spread the bounds so it will never have a zero size
        for (i = 0; i < 3; i++) {
            hashBounds.oSet(0, i, (float) floor(hashBounds.oGet(0, i) - 1));
            hashBounds.oSet(1, i, (float) ceil(hashBounds.oGet(1, i) + 1));
            hashIntMins[i] = (int) (hashBounds.oGet(0, i) * SNAP_FRACTIONS);

            hashScale.oSet(i, (hashBounds.oGet(1, i) - hashBounds.oGet(0, i)) / HASH_BINS);
            hashIntScale[i] = (int) (hashScale.oGet(i) * SNAP_FRACTIONS);
            if (hashIntScale[i] < 1) {
                hashIntScale[i] = 1;
            }
        }

        // add all the points to the hash buckets
        for (areaNum = 0; areaNum < e.numAreas; areaNum++) {
            for (group = e.areas[areaNum].groups; group != null; group = group.nextGroup) {
                // don't touch discrete surfaces
                if (group.material != null && group.material.IsDiscrete()) {
                    continue;
                }

                for (a = group.triList; a != null; a = a.next) {
                    for (vert = 0; vert < 3; vert++) {
                        a.hashVert[vert] = GetHashVert(a.v[vert].xyz);
                    }
                }
            }
        }

        // add all the func_static model vertexes to the hash buckets
        // optionally inline some of the func_static models
        if (dmapGlobals.entityNum == 0) {
            for (int eNum = 1; eNum < dmapGlobals.num_entities; eNum++) {
                uEntity_t entity = dmapGlobals.uEntities[eNum];
                final String className = entity.mapEntity.epairs.GetString("classname");
                if (idStr.Icmp(className, "func_static") != 0) {
                    continue;
                }
                final String modelName = entity.mapEntity.epairs.GetString("model");
                if (isNotNullOrEmpty(modelName)) {
                    continue;
                }
                if (!modelName.contains(".lwo") && !modelName.contains(".ase") && !modelName.contains(".ma")) {
                    continue;
                }

                idRenderModel model = renderModelManager.FindModel(modelName);

//			common.Printf( "adding T junction verts for %s.\n", entity.mapEntity.epairs.GetString( "name" ) );
                idMat3 axis = new idMat3();
                // get the rotation matrix in either full form, or single angle form
                if (!entity.mapEntity.epairs.GetMatrix("rotation", "1 0 0 0 1 0 0 0 1", axis)) {
                    float angle = entity.mapEntity.epairs.GetFloat("angle");
                    if (angle != 0.0f) {
                        axis = new idAngles(0.0f, angle, 0.0f).ToMat3();
                    } else {
                        axis.Identity();
                    }
                }

                idVec3 origin = entity.mapEntity.epairs.GetVector("origin");

                for (i = 0; i < model.NumSurfaces(); i++) {
                    final modelSurface_s surface = model.Surface(i);
                    final srfTriangles_s tri = surface.geometry;

                    mapTri_s mapTri = new mapTri_s();
//				memset( &mapTri, 0, sizeof( mapTri ) );
                    mapTri.material = surface.shader;
                    // don't let discretes (autosprites, etc) merge together
                    if (mapTri.material.IsDiscrete()) {
                        mapTri.mergeGroup = surface;
                    }
                    for (int j = 0; j < tri.numVerts; j += 3) {
                        idVec3 v = tri.verts[j].xyz.oMultiply(axis).oPlus(origin);
                        GetHashVert(v);
                    }
                }
            }
        }

        // now fix each area
        for (areaNum = 0; areaNum < e.numAreas; areaNum++) {
            for (group = e.areas[areaNum].groups; group != null; group = group.nextGroup) {
                // don't touch discrete surfaces
                if (group.material != null && group.material.IsDiscrete()) {
                    continue;
                }

                mapTri_s newList = null;
                for (mapTri_s tri = group.triList; tri != null; tri = tri.next) {
                    mapTri_s fixed = FixTriangleAgainstHash(tri);
                    newList = MergeTriLists(newList, fixed);
                }
                FreeTriList(group.triList);
                group.triList = newList;
            }
        }

        // done
        FreeTJunctionHash();
    }
}
