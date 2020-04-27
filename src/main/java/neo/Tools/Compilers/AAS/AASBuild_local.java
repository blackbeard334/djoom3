package neo.Tools.Compilers.AAS;

import neo.idlib.math.Plane.idPlane;

/**
 *
 */
public class AASBuild_local {

    //===============================================================
    //
    //	idAASBuild
    //
    //===============================================================
    static class aasProcNode_s {

        idPlane plane;
        int[] children = new int[2];        // negative numbers are (-1 - areaNumber), 0 = solid
    };
}
