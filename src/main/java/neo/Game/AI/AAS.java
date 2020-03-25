package neo.Game.AI;

import neo.Game.AI.AAS_local.idAASLocal;
import neo.Tools.Compilers.AAS.AASFile.aasTrace_s;
import neo.Tools.Compilers.AAS.AASFile.idAASSettings;
import neo.Tools.Compilers.AAS.AASFile.idReachability;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class AAS {

    /*
     ===============================================================================

     Area Awareness System

     ===============================================================================
     */
// enum {
    static final int PATHTYPE_WALK         = 0;
    static final int PATHTYPE_WALKOFFLEDGE = 1;
    static final int PATHTYPE_BARRIERJUMP  = 2;
    static final int PATHTYPE_JUMP         = 3;
// };

    static class aasPath_s {

        int            type;            // path type
        idVec3         moveGoal;        // point the AI should move towards
        int            moveAreaNum;     // number of the area the AI should move towards
        idVec3         secondaryGoal;   // secondary move goal for complex navigation
        idReachability reachability;    // reachability used for navigation
    }

    static class aasGoal_s {

        int    areaNum;                 // area the goal is in
        idVec3 origin;			        // position of goal
    }

    static class aasObstacle_s {

        idBounds absBounds;		        // absolute bounds of obstacle
        idBounds expAbsBounds;          // expanded absolute bounds of obstacle
    }

    public static abstract class idAASCallback {

        // virtual						~idAASCallback() {};
        public abstract boolean TestArea(final idAAS aas, int areaNum);
    }

    public abstract static class idAAS {

        public static idAAS Alloc() {
            return new idAASLocal();
        }
        // virtual						~idAAS() = 0;

        // Initialize for the given map.
        public abstract boolean Init(final idStr mapName, /*unsigned int*/ long mapFileCRC);

        // Print AAS stats.
        public abstract void Stats();

        // Test from the given origin.
        public abstract void Test(final idVec3 origin);

        // Get the AAS settings.
        public abstract idAASSettings GetSettings();

        // Returns the number of the area the origin is in.
        public abstract int PointAreaNum(final idVec3 origin);

        // Returns the number of the nearest reachable area for the given point.
        public abstract int PointReachableAreaNum(final idVec3 origin, final idBounds bounds, final int areaFlags);

        // Returns the number of the first reachable area in or touching the bounds.
        public abstract int BoundsReachableAreaNum(final idBounds bounds, final int areaFlags);

        // Push the point into the area.
        public abstract void PushPointIntoAreaNum(int areaNum, idVec3 origin);

        // Returns a reachable point inside the given area.
        public abstract idVec3 AreaCenter(int areaNum);

        // Returns the area flags.
        public abstract int AreaFlags(int areaNum);

        // Returns the travel flags for traveling through the area.
        public abstract int AreaTravelFlags(int areaNum);

        // Trace through the areas and report the first collision.
        public abstract boolean Trace(aasTrace_s trace, final idVec3 start, final idVec3 end);

        // Get a plane for a trace.
        public abstract idPlane GetPlane(int planeNum);

        // Get wall edges.
        public abstract int GetWallEdges(int areaNum, final idBounds bounds, int travelFlags, int[] edges, int maxEdges);

        // Sort the wall edges to create continuous sequences of walls.
        public abstract void SortWallEdges(int[] edges, int numEdges);

        // Get the vertex numbers for an edge.
        public abstract void GetEdgeVertexNumbers(int edgeNum, int[] verts/*[2]*/);

        // Get an edge.
        public abstract void GetEdge(int edgeNum, idVec3 start, idVec3 end);

        // Find all areas within or touching the bounds with the given contents and disable/enable them for routing.
        public abstract boolean SetAreaState(final idBounds bounds, final int areaContents, boolean disabled);

        // Add an obstacle to the routing system.
        public abstract int/*aasHandle_t*/ AddObstacle(final idBounds bounds);

        // Remove an obstacle from the routing system.
        public abstract void RemoveObstacle(final int/*aasHandle_t*/ handle);

        // Remove all obstacles from the routing system.
        public abstract void RemoveAllObstacles();

        // Returns the travel time towards the goal area in 100th of a second.
        public abstract int TravelTimeToGoalArea(int areaNum, final idVec3 origin, int goalAreaNum, int travelFlags);

        // Get the travel time and first reachability to be used towards the goal, returns true if there is a path.
        public abstract boolean RouteToGoalArea(int areaNum, final idVec3 origin, int goalAreaNum, int travelFlags, int[] travelTime, idReachability[] reach);

        // Creates a walk path towards the goal.
        public abstract boolean WalkPathToGoal(aasPath_s path, int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin, int travelFlags);

        // Returns true if one can walk along a straight line from the origin to the goal origin.
        public abstract boolean WalkPathValid(int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin, int travelFlags, idVec3 endPos, int[] endAreaNum);

        // Creates a fly path towards the goal.
        public abstract boolean FlyPathToGoal(aasPath_s path, int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin, int travelFlags);

        // Returns true if one can fly along a straight line from the origin to the goal origin.
        public abstract boolean FlyPathValid(int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin, int travelFlags, idVec3 endPos, int[] endAreaNum);

        // Show the walk path from the origin towards the area.
        public abstract void ShowWalkPath(final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin);

        // Show the fly path from the origin towards the area.
        public abstract void ShowFlyPath(final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin);

        // Find the nearest goal which satisfies the callback.
        public abstract boolean FindNearestGoal(aasGoal_s goal, int areaNum, final idVec3 origin, final idVec3 target, int travelFlags, aasObstacle_s[] obstacles, int numObstacles, idAASCallback callback);
    }
}
