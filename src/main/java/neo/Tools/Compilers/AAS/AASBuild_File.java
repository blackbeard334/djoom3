package neo.Tools.Compilers.AAS;

import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.containers.HashIndex.idHashIndex;

/**
 *
 */
public class AASBuild_File {

    static final int   VERTEX_HASH_BOXSIZE      = (1 << 6);    // must be power of 2
    static final int   VERTEX_HASH_SIZE         = (VERTEX_HASH_BOXSIZE * VERTEX_HASH_BOXSIZE);
    static final int   EDGE_HASH_SIZE           = (1 << 14);
    //
    static final float INTEGRAL_EPSILON         = 0.01f;
    static final float VERTEX_EPSILON           = 0.1f;
    //
    static final float AAS_PLANE_NORMAL_EPSILON = 0.00001f;
    static final float AAS_PLANE_DIST_EPSILON   = 0.01f;
    //
    //
    static idHashIndex aas_vertexHash;
    static idHashIndex aas_edgeHash;
    static idBounds    aas_vertexBounds;
    static int         aas_vertexShift;

    static class sizeEstimate_s {

        int numEdgeIndexes;
        int numFaceIndexes;
        int numAreas;
        int numNodes;
    }
}
