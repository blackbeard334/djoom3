package neo.Game.AI;

/**
 *
 */
public class AAS_pathing {

    public static final int   SUBSAMPLE_WALK_PATH    = 1;
    public static final int   SUBSAMPLE_FLY_PATH     = 0;
    //
    public static final int   maxWalkPathIterations  = 10;
    public static final float maxWalkPathDistance    = 500.0f;
    public static final float walkPathSampleDistance = 8.0f;
    //
    public static final int   maxFlyPathIterations   = 10;
    public static final float maxFlyPathDistance     = 500.0f;
    public static final float flyPathSampleDistance  = 8.0f;

    public static class wallEdge_s {

        public int edgeNum;
        public int[] verts = new int[2];
        public wallEdge_s next;
    };

}
