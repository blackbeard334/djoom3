package neo.Renderer;

import static neo.framework.Common.common;

import neo.idlib.Lib.idException;
import neo.open.Nio;

/**
 *
 */
public class tr_orderIndexes {

    /*
     ===============
     R_MeshCost
     ===============
     */
    static final int CACHE_SIZE = 24;
    static final int STALL_SIZE = 8;

    public static int R_MeshCost(int numIndexes, int/*glIndex_t */[] indexes) {
        final int[] inCache = new int[CACHE_SIZE];
        int i, j, v;
        int c_stalls;
        int c_loads;
        int fifo;

        for (i = 0; i < CACHE_SIZE; i++) {
            inCache[i] = -1;
        }

        c_loads = 0;
        c_stalls = 0;
        fifo = 0;

        for (i = 0; i < numIndexes; i++) {
            v = indexes[i];
            for (j = 0; j < CACHE_SIZE; j++) {
                if (inCache[ (fifo + j) % CACHE_SIZE] == v) {
                    break;
                }
            }
            if (j == CACHE_SIZE) {
                c_loads++;
                inCache[ fifo % CACHE_SIZE] = v;
                fifo++;
            } else if (j < STALL_SIZE) {
                c_stalls++;
            }
        }

        return c_loads;
    }

    static class vertRef_s {

        vertRef_s next;
        int tri;
    }

    /*
     ====================
     R_OrderIndexes

     Reorganizes the indexes so they will take best advantage
     of the internal GPU vertex caches
     ====================
     */
    public static void R_OrderIndexes(int numIndexes, int /*glIndex_t */[] indexes) throws idException {
        boolean[] triangleUsed;
        int numTris;
        int /*glIndex_t */[] oldIndexes;
        int /*glIndex_t */[] base;
        int base_index;
        int numOldIndexes;
        int tri;
        int i;
        vertRef_s vref;
        vertRef_s[] vrefs, vrefTable;
        int numVerts;
        int v1, v2;
        int c_starts;
        int c_cost;

        if (!RenderSystem_init.r_orderIndexes.GetBool()) {
            return;
        }

        // save off the original indexes
        oldIndexes = new int[numIndexes];
//	memcpy( oldIndexes, indexes, numIndexes * sizeof( *oldIndexes ) );
        Nio.arraycopy(indexes, 0, oldIndexes, 0, numIndexes);
        numOldIndexes = numIndexes;

        // make a table to mark the triangles when they are emited
        numTris = numIndexes / 3;
        triangleUsed = new boolean[numTris];
//	memset( triangleUsed, 0, numTris * sizeof( *triangleUsed ) );

        // find the highest vertex number
        numVerts = 0;
        for (i = 0; i < numIndexes; i++) {
            if (indexes[i] > numVerts) {
                numVerts = indexes[i];
            }
        }
        numVerts++;

        // create a table of triangles used by each vertex
        vrefs = new vertRef_s[numVerts];
//	memset( vrefs, 0, numVerts * sizeof( *vrefs ) );

        vrefTable = new vertRef_s[numIndexes];
        for (i = 0; i < numIndexes; i++) {
            tri = i / 3;

            vrefTable[i].tri = tri;
            vrefTable[i].next = vrefs[oldIndexes[i]];
            vrefs[oldIndexes[i]] = vrefTable[i];
        }

        // generate new indexes
        numIndexes = 0;
        c_starts = 0;
        while (numIndexes != numOldIndexes) {
            // find a triangle that hasn't been used
            for (tri = 0; tri < numTris; tri++) {
                if (!triangleUsed[tri]) {
                    break;
                }
            }
            if (tri == numTris) {
                common.Error("R_OrderIndexes: ran out of unused tris");
            }

            c_starts++;

            do {
                // emit this tri
                base = oldIndexes;//[tri * 3];
                base_index = tri * 3;
                indexes[numIndexes + 0] = base[base_index + 0];
                indexes[numIndexes + 1] = base[base_index + 1];
                indexes[numIndexes + 2] = base[base_index + 2];
                numIndexes += 3;

                triangleUsed[tri] = true;

                // try to find a shared edge to another unused tri
                for (i = 0; i < 3; i++) {
                    v1 = base[base_index + i];
                    v2 = base[base_index + ((i + 1) % 3)];

                    for (vref = vrefs[v1]; vref != null; vref = vref.next) {
                        tri = vref.tri;
                        if (triangleUsed[tri]) {
                            continue;
                        }

                        // if this triangle also uses v2, grab it
                        if ((oldIndexes[(tri * 3) + 0] == v2)
                                || (oldIndexes[(tri * 3) + 1] == v2)
                                || (oldIndexes[(tri * 3) + 2] == v2)) {
                            break;
                        }
                    }
                    if (vref != null) {
                        break;
                    }
                }

                // if we couldn't chain off of any verts, we need to find a new one
                if (i == 3) {
                    break;
                }
            } while (true);
        }

        c_cost = R_MeshCost(numIndexes, indexes);

    }
    /*

     add all triangles that can be specified by the vertexes in the last 14 cache positions

     pick a new vert to add to the cache
     don't pick one in the 24 previous cache positions
     try to pick one that will enable the creation of as many triangles as possible

     look for a vert that shares an edge with the vert about to be evicted


     */
}
