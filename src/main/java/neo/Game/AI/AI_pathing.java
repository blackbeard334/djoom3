package neo.Game.AI;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static neo.CM.CollisionModel.CM_BOX_EPSILON;
import neo.CM.CollisionModel.trace_s;
import neo.Game.AI.AAS.idAAS;
import static neo.Game.AI.AI.SE_ENTER_LEDGE_AREA;
import static neo.Game.AI.AI.SE_ENTER_OBSTACLE;
import neo.Game.AI.AI.obstaclePath_s;
import neo.Game.AI.AI.predictedPath_s;
import neo.Game.AI.AI_pathing.pathNode_s;
import neo.Game.Actor.idActor;
import neo.Game.Entity.idEntity;
import static neo.Game.GameSys.SysCvar.ai_debugMove;
import static neo.Game.GameSys.SysCvar.ai_showObstacleAvoidance;
import static neo.Game.Game_local.MASK_MONSTERSOLID;
import static neo.Game.Game_local.MAX_GENTITIES;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import neo.Game.Moveable.idMoveable;
import neo.Game.Physics.Clip.idClipModel;
import neo.Game.Physics.Physics.idPhysics;
import static neo.TempDump.NOT;
import static neo.Tools.Compilers.AAS.AASFile.AREA_LEDGE;
import static neo.Tools.Compilers.AAS.AASFile.TFL_INVALID;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WALK;
import neo.Tools.Compilers.AAS.AASFile.aasTrace_s;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.BV.Box.idBox;
import static neo.idlib.Lib.colorCyan;
import neo.idlib.containers.Queue.idQueueTemplate;
import neo.idlib.geometry.Winding2D.idWinding2D;
import static neo.idlib.math.Math_h.FLOATSIGNBITNOTSET;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import static neo.idlib.math.Math_h.Square;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Vector.RAD2DEG;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import static neo.ui.DeviceContext.idDeviceContext.colorBlue;
import static neo.ui.DeviceContext.idDeviceContext.colorGreen;
import static neo.ui.DeviceContext.idDeviceContext.colorRed;
import static neo.ui.DeviceContext.idDeviceContext.colorWhite;
import static neo.ui.DeviceContext.idDeviceContext.colorYellow;

/**
 *
 */
public class AI_pathing {

    /*
     ===============================================================================

     Dynamic Obstacle Avoidance

     - assumes the AI lives inside a bounding box aligned with the gravity direction
     - obstacles in proximity of the AI are gathered
     - if obstacles are found the AAS walls are also considered as obstacles
     - every obstacle is represented by an oriented bounding box (OBB)
     - an OBB is projected onto a 2D plane orthogonal to AI's gravity direction
     - the 2D windings of the projections are expanded for the AI bbox
     - a path tree is build using clockwise and counter clockwise edge walks along the winding edges
     - the path tree is pruned and optimized
     - the shortest path is chosen for navigation

     ===============================================================================
     */
    static final float MAX_OBSTACLE_RADIUS    = 256.0f;
    static final float PUSH_OUTSIDE_OBSTACLES = 0.5f;
    static final float CLIP_BOUNDS_EPSILON    = 10.0f;
    static final int   MAX_AAS_WALL_EDGES     = 256;
    static final int   MAX_OBSTACLES          = 256;
    static final int   MAX_PATH_NODES         = 256;
    static final int   MAX_OBSTACLE_PATH      = 64;
    //
    static final float OVERCLIP               = 1.001f;
    static final int   MAX_FRAME_SLIDE        = 5;
    //
//    static idBlockAlloc<pathNode_s> pathNodeAllocator = new Heap.idBlockAlloc<>(128);
    static       int   pathNodeAllocator      = 0;
    //    

    /*
     ===============================================================================

     Path Prediction

     Uses the AAS to quickly and accurately predict a path for a certain
     period of time based on an initial position and velocity.

     ===============================================================================
     */
    static class pathTrace_s {

        float    fraction;
        idVec3   endPos;
        idVec3   normal;
        idEntity blockingEntity;
    };

    static class obstacle_s {
        idVec2[]    bounds  = new idVec2[2];
        idWinding2D winding = new idWinding2D();
        idEntity    entity;
    };

    static class pathNode_s {

        int          dir;
        idVec2       pos;
        idVec2       delta;
        float        dist;
        int          obstacle;
        int          edgeNum;
        int          numNodes;
        pathNode_s   parent;
        pathNode_s[] children = new pathNode_s[2];
        pathNode_s   next;

        public pathNode_s() {
            pos = new idVec2();
            delta = new idVec2();
            pathNodeAllocator++;
        }

        void Init() {
            dir = 0;
            pos.Zero();
            delta.Zero();
            obstacle = -1;
            edgeNum = -1;
            numNodes = 0;
            parent = children[0] = children[1] = next = null;
        }

        private void oSet(pathNode_s parent) {//TODO:how do we reference the non objects?
            this.dir = parent.dir;
            this.pos = parent.pos;
            this.delta = parent.delta;
            this.dist = parent.dist;
            this.obstacle = parent.obstacle;
            this.edgeNum = parent.edgeNum;
            this.numNodes = parent.numNodes;
            this.parent = parent.parent;
            this.children = parent.children;
            this.next = parent.next;
        }
    };

    static class wallEdge_s {

        int edgeNum;
        int[] verts = new int[2];
        wallEdge_s next;
    };


    /*
     ============
     LineIntersectsPath
     ============
     */
    static boolean LineIntersectsPath(final idVec2 start, final idVec2 end, final pathNode_s node) {
        float d0, d1, d2, d3;
        idVec3 plane1, plane2;

        plane1 = idWinding2D.Plane2DFromPoints(start, end);
        d0 = plane1.x * node.pos.x + plane1.y * node.pos.y + plane1.z;
        while (node.parent != null) {
            d1 = plane1.x * node.parent.pos.x + plane1.y * node.parent.pos.y + plane1.z;
            if ((FLOATSIGNBITSET(d0) ^ FLOATSIGNBITSET(d1)) != 0) {
                plane2 = idWinding2D.Plane2DFromPoints(node.pos, node.parent.pos);
                d2 = plane2.x * start.x + plane2.y * start.y + plane2.z;
                d3 = plane2.x * end.x + plane2.y * end.y + plane2.z;
                if ((FLOATSIGNBITSET(d2) ^ FLOATSIGNBITSET(d3)) != 0) {
                    return true;
                }
            }
            d0 = d1;
            node.oSet(node.parent);
        }
        return false;
    }

    /*
     ============
     PointInsideObstacle
     ============
     */
    static int PointInsideObstacle(final obstacle_s[] obstacles, final int numObstacles, final idVec2 point) {
        int i;

        for (i = 0; i < numObstacles; i++) {

            final idVec2[] bounds = obstacles[i].bounds;
            if (point.x < bounds[0].x || point.y < bounds[0].y || point.x > bounds[1].x || point.y > bounds[1].y) {
                continue;
            }

            if (!obstacles[i].winding.PointInside(point, 0.1f)) {
                continue;
            }

            return i;
        }

        return -1;
    }

    /*
     ============
     GetPointOutsideObstacles
     ============
     */
    static void GetPointOutsideObstacles(final obstacle_s[] obstacles, final int numObstacles, idVec2 point, int[] obstacle, int[] edgeNum) {
        int i, j, k, n, bestObstacle, bestEdgeNum, queueStart, queueEnd;
        int[] edgeNums = new int[2];
        float d, bestd;
        float[][] scale = new float[2][1];
        idVec3 plane, bestPlane = new idVec3();
        idVec2 newPoint, dir, bestPoint = new idVec2();
        int[] queue;
        boolean[] obstacleVisited;
        idWinding2D w1, w2;

        if (obstacle != null) {
            obstacle[0] = -1;
        }
        if (edgeNum != null) {
            edgeNum[0] = -1;
        }

        bestObstacle = PointInsideObstacle(obstacles, numObstacles, point);
        if (bestObstacle == -1) {
            return;
        }

        final idWinding2D w = obstacles[bestObstacle].winding;
        bestd = idMath.INFINITY;
        bestEdgeNum = 0;
        for (i = 0; i < w.GetNumPoints(); i++) {
            plane = idWinding2D.Plane2DFromPoints(w.oGet((i + 1) % w.GetNumPoints()), w.oGet(i), true);
            d = plane.x * point.x + plane.y * point.y + plane.z;
            if (d < bestd) {
                bestd = d;
                bestPlane = plane;
                bestEdgeNum = i;
            }
            // if this is a wall always try to pop out at the first edge
            if (obstacles[bestObstacle].entity == null) {
                break;
            }
        }

        newPoint = point.oMinus(bestPlane.ToVec2().oMultiply(bestd + PUSH_OUTSIDE_OBSTACLES));
        if (PointInsideObstacle(obstacles, numObstacles, newPoint) == -1) {
            point.oSet(newPoint);
            if (obstacle != null) {
                obstacle[0] = bestObstacle;
            }
            if (edgeNum != null) {
                edgeNum[0] = bestEdgeNum;
            }
            return;
        }

        queue = new int[numObstacles];
        obstacleVisited = new boolean[numObstacles];

        queueStart = 0;
        queueEnd = 1;
        queue[0] = bestObstacle;

//	memset( obstacleVisited, 0, numObstacles * sizeof( obstacleVisited[0] ) );
        obstacleVisited[bestObstacle] = true;

        bestd = idMath.INFINITY;
        for (i = queue[0]; queueStart < queueEnd; i = queue[++queueStart]) {
            w1 = obstacles[i].winding;
            w1.Expand(PUSH_OUTSIDE_OBSTACLES);

            for (j = 0; j < numObstacles; j++) {
                // if the obstacle has been visited already
                if (obstacleVisited[j]) {
                    continue;
                }
                // if the bounds do not intersect
                if (obstacles[j].bounds[0].x > obstacles[i].bounds[1].x || obstacles[j].bounds[0].y > obstacles[i].bounds[1].y
                        || obstacles[j].bounds[1].x < obstacles[i].bounds[0].x || obstacles[j].bounds[1].y < obstacles[i].bounds[0].y) {
                    continue;
                }

                queue[queueEnd++] = j;
                obstacleVisited[j] = true;

                w2 = obstacles[j].winding;
                w2.Expand(0.2f);

                for (k = 0; k < w1.GetNumPoints(); k++) {
                    dir = w1.oGet((k + 1) % w1.GetNumPoints()).oMinus(w1.oGet(k));
                    if (!w2.RayIntersection(w1.oGet(k), dir, scale[0], scale[1], edgeNums)) {
                        continue;
                    }
                    for (n = 0; n < 2; n++) {
                        newPoint = w1.oGet(k).oPlus(dir.oMultiply(scale[n][0]));
                        if (PointInsideObstacle(obstacles, numObstacles, newPoint) == -1) {
                            d = (newPoint.oMinus(point)).LengthSqr();
                            if (d < bestd) {
                                bestd = d;
                                bestPoint = newPoint;
                                bestEdgeNum = edgeNums[n];
                                bestObstacle = j;
                            }
                        }
                    }
                }
            }

            if (bestd < idMath.INFINITY) {
                point.oSet(bestPoint);
                if (obstacle != null) {
                    obstacle[0] = bestObstacle;
                }
                if (edgeNum != null) {
                    edgeNum[0] = bestEdgeNum;
                }
                return;
            }
        }
        gameLocal.Warning("GetPointOutsideObstacles: no valid point found");
    }

    /*
     ============
     GetFirstBlockingObstacle
     ============
     */
    static boolean GetFirstBlockingObstacle(final obstacle_s[] obstacles, int numObstacles, int skipObstacle, final idVec2 startPos, final idVec2 delta, float[] blockingScale, int[] blockingObstacle, int[] blockingEdgeNum) {
        int i;
        int[] edgeNums = new int[2];
        float dist;
        float[] scale1 = {0}, scale2 = {0};
        idVec2[] bounds = new idVec2[2];

        // get bounds for the current movement delta
        bounds[0] = startPos.oMinus(new idVec2(CM_BOX_EPSILON, CM_BOX_EPSILON));
        bounds[1] = startPos.oPlus(new idVec2(CM_BOX_EPSILON, CM_BOX_EPSILON));
        bounds[FLOATSIGNBITNOTSET(delta.x)].x += delta.x;
        bounds[FLOATSIGNBITNOTSET(delta.y)].y += delta.y;

        // test for obstacles blocking the path
        blockingScale[0] = idMath.INFINITY;
        dist = delta.Length();
        for (i = 0; i < numObstacles; i++) {
            if (i == skipObstacle) {
                continue;
            }
            if (bounds[0].x > obstacles[i].bounds[1].x || bounds[0].y > obstacles[i].bounds[1].y
                    || bounds[1].x < obstacles[i].bounds[0].x || bounds[1].y < obstacles[i].bounds[0].y) {
                continue;
            }
            if (obstacles[i].winding.RayIntersection(startPos, delta, scale1, scale2, edgeNums)) {
                if (scale1[0] < blockingScale[0] && scale1[0] * dist > -0.01f && scale2[0] * dist > 0.01f) {
                    blockingScale = scale1;
                    blockingObstacle[0] = i;
                    blockingEdgeNum[0] = edgeNums[0];
                }
            }
        }
        return (blockingScale[0] < 1.0f);
    }

    /*
     ============
     GetObstacles
     ============
     */
    static int GetObstacles(final idPhysics physics, final idAAS aas, final idEntity ignore, int areaNum, final idVec3 startPos, final idVec3 seekPos, obstacle_s[] obstacles, int maxObstacles, idBounds clipBounds) {
        int i, j, numListedClipModels, numObstacles, numVerts, clipMask;
        int[] blockingObstacle = {0}, blockingEdgeNum = {0};
        int[] wallEdges = new int[MAX_AAS_WALL_EDGES], verts = new int[2], lastVerts = new int[2], nextVerts = new int[2];
        int numWallEdges;
        float[] stepHeight = {0}, headHeight = {0}, blockingScale = {0}, min = {0}, max = {0};
        idVec3 seekDelta, start = new idVec3(), end = new idVec3(), nextStart = new idVec3(), nextEnd = new idVec3();
        idVec3[] silVerts = new idVec3[32];
        idVec2 edgeDir, edgeNormal = new idVec2(), nextEdgeDir,
                nextEdgeNormal = new idVec2(), lastEdgeNormal = new idVec2();
        idVec2[] expBounds = new idVec2[2];
        idVec2 obDelta;
        idPhysics obPhys;
        idBox box;
        idEntity obEnt;
        idClipModel clipModel;
        idClipModel[] clipModelList = new idClipModel[MAX_GENTITIES];

        numObstacles = 0;

        seekDelta = seekPos.oMinus(startPos);
        expBounds[0] = physics.GetBounds().oGet(0).ToVec2().oMinus(new idVec2(CM_BOX_EPSILON, CM_BOX_EPSILON));
        expBounds[1] = physics.GetBounds().oGet(1).ToVec2().oPlus(new idVec2(CM_BOX_EPSILON, CM_BOX_EPSILON));

        physics.GetAbsBounds().AxisProjection(physics.GetGravityNormal().oNegative(), stepHeight, headHeight);
        stepHeight[0] += aas.GetSettings().maxStepHeight[0];

        // clip bounds for the obstacle search space
        clipBounds.oSet(0, clipBounds.oSet(1, startPos));
        clipBounds.AddPoint(seekPos);
        clipBounds.ExpandSelf(MAX_OBSTACLE_RADIUS);
        clipMask = physics.GetClipMask();

        // find all obstacles touching the clip bounds
        numListedClipModels = gameLocal.clip.ClipModelsTouchingBounds(clipBounds, clipMask, clipModelList, MAX_GENTITIES);

        for (i = 0; i < numListedClipModels && numObstacles < MAX_OBSTACLES; i++) {
            clipModel = clipModelList[i];
            obEnt = clipModel.GetEntity();

            if (!clipModel.IsTraceModel()) {
                continue;
            }

            if (obEnt.IsType(idActor.class)) {
                obPhys = obEnt.GetPhysics();
                // ignore myself, my enemy, and dead bodies
                if ((obPhys == physics) || (obEnt == ignore) || (obEnt.health <= 0)) {
                    continue;
                }
                // if the actor is moving
                idVec3 v1 = obPhys.GetLinearVelocity();
                if (v1.LengthSqr() > Square(10.0f)) {
                    idVec3 v2 = physics.GetLinearVelocity();
                    if (v2.LengthSqr() > Square(10.0f)) {
                        // if moving in about the same direction
                        if (v1.oMultiply(v2) > 0.0f) {
                            continue;
                        }
                    }
                }
            } else if (obEnt.IsType(idMoveable.class)) {
                // moveables are considered obstacles
            } else {
                // ignore everything else
                continue;
            }

            // check if we can step over the object
            clipModel.GetAbsBounds().AxisProjection(physics.GetGravityNormal().oNegative(), min, max);
            if (max[0] < stepHeight[0] || min[0] > headHeight[0]) {
                // can step over this one
                continue;
            }

            // project a box containing the obstacle onto the floor plane
            box = new idBox(clipModel.GetBounds(), clipModel.GetOrigin(), clipModel.GetAxis());
            numVerts = box.GetParallelProjectionSilhouetteVerts(physics.GetGravityNormal(), silVerts);

            // create a 2D winding for the obstacle;
            obstacle_s obstacle = obstacles[numObstacles++];
            obstacle.winding.Clear();
            for (j = 0; j < numVerts; j++) {
                obstacle.winding.AddPoint(silVerts[j].ToVec2());
            }

            if (ai_showObstacleAvoidance.GetBool()) {
                for (j = 0; j < numVerts; j++) {
                    silVerts[j].z = startPos.z;
                }
                for (j = 0; j < numVerts; j++) {
                    gameRenderWorld.DebugArrow(colorWhite, silVerts[j], silVerts[(j + 1) % numVerts], 4);
                }
            }

            // expand the 2D winding for collision with a 2D box
            obstacle.winding.ExpandForAxialBox(expBounds);
            obstacle.winding.GetBounds(obstacle.bounds);
            obstacle.entity = obEnt;
        }

        // if there are no dynamic obstacles the path should be through valid AAS space
        if (numObstacles == 0) {
            return 0;
        }

        // if the current path doesn't intersect any dynamic obstacles the path should be through valid AAS space
        if (PointInsideObstacle(obstacles, numObstacles, startPos.ToVec2()) == -1) {
            if (!GetFirstBlockingObstacle(obstacles, numObstacles, -1, startPos.ToVec2(), seekDelta.ToVec2(), blockingScale, blockingObstacle, blockingEdgeNum)) {
                return 0;
            }
        }

        // create obstacles for AAS walls
        if (aas != null) {
            float halfBoundsSize = (expBounds[ 1].x - expBounds[ 0].x) * 0.5f;

            numWallEdges = aas.GetWallEdges(areaNum, clipBounds, TFL_WALK, wallEdges, MAX_AAS_WALL_EDGES);
            aas.SortWallEdges(wallEdges, numWallEdges);

            lastVerts[0] = lastVerts[1] = 0;
            lastEdgeNormal.Zero();
            nextVerts[0] = nextVerts[1] = 0;
            for (i = 0; i < numWallEdges && numObstacles < MAX_OBSTACLES; i++) {
                aas.GetEdge(wallEdges[i], start, end);
                aas.GetEdgeVertexNumbers(wallEdges[i], verts);
                edgeDir = end.ToVec2().oMinus(start.ToVec2());
                edgeDir.Normalize();
                edgeNormal.x = edgeDir.y;
                edgeNormal.y = -edgeDir.x;
                if (i < numWallEdges - 1) {
                    aas.GetEdge(wallEdges[i + 1], nextStart, nextEnd);
                    aas.GetEdgeVertexNumbers(wallEdges[i + 1], nextVerts);
                    nextEdgeDir = nextEnd.ToVec2().oMinus(nextStart.ToVec2());
                    nextEdgeDir.Normalize();
                    nextEdgeNormal.x = nextEdgeDir.y;
                    nextEdgeNormal.y = -nextEdgeDir.x;
                }

                obstacle_s obstacle = obstacles[numObstacles++];
                obstacle.winding.Clear();
                obstacle.winding.AddPoint(end.ToVec2());
                obstacle.winding.AddPoint(start.ToVec2());
                obstacle.winding.AddPoint(start.ToVec2().oMinus(edgeDir.oMinus(edgeNormal.oMultiply(halfBoundsSize))));
                obstacle.winding.AddPoint(end.ToVec2().oPlus(edgeDir.oMinus(edgeNormal.oMultiply(halfBoundsSize))));
                if (lastVerts[1] == verts[0]) {
                    obstacle.winding.oMinSet(2, lastEdgeNormal.oMultiply(halfBoundsSize));
                } else {
                    obstacle.winding.oMinSet(1, edgeDir);
                }
                if (verts[1] == nextVerts[0]) {
                    obstacle.winding.oMinSet(3, nextEdgeNormal.oMultiply(halfBoundsSize));
                } else {
                    obstacle.winding.oPluSet(0, edgeDir);
                }
                obstacle.winding.GetBounds(obstacle.bounds);
                obstacle.entity = null;

//			memcpy( lastVerts, verts, sizeof( lastVerts ) );
                lastVerts[0] = verts[0];
                lastVerts[1] = verts[1];
                lastEdgeNormal = edgeNormal;
            }
        }

        // show obstacles
        if (ai_showObstacleAvoidance.GetBool()) {
            for (i = 0; i < numObstacles; i++) {
                obstacle_s obstacle = obstacles[i];
                for (j = 0; j < obstacle.winding.GetNumPoints(); j++) {
                    silVerts[j].oSet(obstacle.winding.oGet(j));
                    silVerts[j].z = startPos.z;
                }
                for (j = 0; j < obstacle.winding.GetNumPoints(); j++) {
                    gameRenderWorld.DebugArrow(colorGreen, silVerts[j], silVerts[(j + 1) % obstacle.winding.GetNumPoints()], 4);
                }
            }
        }

        return numObstacles;
    }

    /*
     ============
     FreePathTree_r
     ============
     */
    static void FreePathTree_r(pathNode_s node) {
        if (node.children[0] != null) {
            FreePathTree_r(node.children[0]);
        }
        if (node.children[1] != null) {
            FreePathTree_r(node.children[1]);
        }
//        pathNodeAllocator.Free(node);
        pathNodeAllocator--;
    }

    /*
     ============
     DrawPathTree
     ============
     */
    static void DrawPathTree(final pathNode_s root, final float height) {
        int i;
        idVec3 start = new idVec3(), end = new idVec3();
        pathNode_s node;

        for (node = root; node != null; node = node.next) {
            for (i = 0; i < 2; i++) {
                if (node.children[i] != null) {
                    start.oSet(node.pos);
                    start.z = height;
                    end.oSet(node.children[i].pos);
                    end.z = height;
                    gameRenderWorld.DebugArrow(node.edgeNum == -1 ? colorYellow : (i != 0 ? colorBlue : colorRed), start, end, 1);
                    break;
                }
            }
        }
    }

    /*
     ============
     GetPathNodeDelta
     ============
     */
    static boolean GetPathNodeDelta(pathNode_s node, final obstacle_s[] obstacles, final idVec2 seekPos, boolean blocked) {
        int numPoints, edgeNum;
        boolean facing;
        idVec2 seekDelta, dir;
        pathNode_s n;

        numPoints = obstacles[node.obstacle].winding.GetNumPoints();

        // get delta along the current edge
        while (true) {
            edgeNum = (node.edgeNum + node.dir) % numPoints;
            node.delta = obstacles[node.obstacle].winding.oGet(edgeNum).oMinus(node.pos);
            if (node.delta.LengthSqr() > 0.01f) {
                break;
            }
            node.edgeNum = (node.edgeNum + numPoints + (2 * node.dir - 1)) % numPoints;
        }

        // if not blocked
        if (!blocked) {

            // test if the current edge faces the goal
            seekDelta = seekPos.oMinus(node.pos);
            facing = ((2 * node.dir - 1) * (node.delta.x * seekDelta.y - node.delta.y * seekDelta.x)) >= 0.0f;

            // if the current edge faces goal and the line from the current
            // position to the goal does not intersect the current path
            if (facing && !LineIntersectsPath(node.pos, seekPos, node.parent)) {
                node.delta = seekPos.oMinus(node.pos);
                node.edgeNum = -1;
            }
        }

        // if the delta is along the obstacle edge
        if (node.edgeNum != -1) {
            // if the edge is found going from this node to the root node
            for (n = node.parent; n != null; n = n.parent) {

                if (node.obstacle != n.obstacle || node.edgeNum != n.edgeNum) {
                    continue;
                }

                // test whether or not the edge segments actually overlap
                if (n.pos.oMultiply(node.delta) > (node.pos.oPlus(node.delta)).oMultiply(node.delta)) {
                    continue;
                }
                if (node.pos.oMultiply(node.delta) > (n.pos.oPlus(n.delta)).oMultiply(node.delta)) {
                    continue;
                }

                break;
            }
            if (n != null) {
                return false;
            }
        }
        return true;
    }

    /*
     ============
     BuildPathTree
     ============
     */
    static pathNode_s BuildPathTree(final obstacle_s[] obstacles, int numObstacles, final idBounds clipBounds, final idVec2 startPos, final idVec2 seekPos, obstaclePath_s path) {
        int obstaclePoints, bestNumNodes = MAX_OBSTACLE_PATH;
        int[] blockingEdgeNum = {0}, blockingObstacle = {0};
        float[] blockingScale = {0};
        pathNode_s root, node, child;
        // gcc 4.0
        idQueueTemplate<pathNode_s/*, offsetof( pathNode_s, next )*/> pathNodeQueue = new idQueueTemplate<>(), treeQueue = new idQueueTemplate<>();
        root = new pathNode_s();//pathNodeAllocator.Alloc();
        root.Init();
        root.pos = startPos;

        root.delta = seekPos.oMinus(root.pos);
        root.numNodes = 0;
        pathNodeQueue.Add(root);

        for (node = pathNodeQueue.Get(); node != null && pathNodeAllocator < MAX_PATH_NODES; node = pathNodeQueue.Get()) {

            treeQueue.Add(node);

            // if this path has more than twice the number of nodes than the best path so far
            if (node.numNodes > bestNumNodes * 2) {
                continue;
            }

            // don't move outside of the clip bounds
            idVec2 endPos = node.pos.oPlus(node.delta);
            if (endPos.x - CLIP_BOUNDS_EPSILON < clipBounds.oGet(0).x || endPos.x + CLIP_BOUNDS_EPSILON > clipBounds.oGet(1).x
                    || endPos.y - CLIP_BOUNDS_EPSILON < clipBounds.oGet(0).y || endPos.y + CLIP_BOUNDS_EPSILON > clipBounds.oGet(1).y) {
                continue;
            }

            // if an obstacle is blocking the path
            if (GetFirstBlockingObstacle(obstacles, numObstacles, node.obstacle, node.pos, node.delta, blockingScale, blockingObstacle, blockingEdgeNum)) {

                if (path.firstObstacle == null) {
                    path.firstObstacle = obstacles[blockingObstacle[0]].entity;
                }

                node.delta.oMulSet(blockingScale[0]);

                if (node.edgeNum == -1) {
                    node.children[0] = new pathNode_s();// pathNodeAllocator.Alloc();
                    node.children[0].Init();
                    node.children[1] = new pathNode_s();//pathNodeAllocator.Alloc();
                    node.children[1].Init();
                    node.children[0].dir = 0;
                    node.children[1].dir = 1;
                    node.children[0].parent = node.children[1].parent = node;
                    node.children[0].pos = node.children[1].pos = node.pos.oPlus(node.delta);
                    node.children[0].obstacle = node.children[1].obstacle = blockingObstacle[0];
                    node.children[0].edgeNum = node.children[1].edgeNum = blockingEdgeNum[0];
                    node.children[0].numNodes = node.children[1].numNodes = node.numNodes + 1;
                    if (GetPathNodeDelta(node.children[0], obstacles, seekPos, true)) {
                        pathNodeQueue.Add(node.children[0]);
                    }
                    if (GetPathNodeDelta(node.children[1], obstacles, seekPos, true)) {
                        pathNodeQueue.Add(node.children[1]);
                    }
                } else {
                    node.children[node.dir] = child = new pathNode_s();//pathNodeAllocator.Alloc();
                    child.Init();
                    child.dir = node.dir;
                    child.parent = node;
                    child.pos = node.pos.oPlus(node.delta);
                    child.obstacle = blockingObstacle[0];
                    child.edgeNum = blockingEdgeNum[0];
                    child.numNodes = node.numNodes + 1;
                    if (GetPathNodeDelta(child, obstacles, seekPos, true)) {
                        pathNodeQueue.Add(child);
                    }
                }
            } else {
                node.children[node.dir] = child = new pathNode_s();//pathNodeAllocator.Alloc();
                child.Init();
                child.dir = node.dir;
                child.parent = node;
                child.pos = node.pos.oPlus(node.delta);
                child.numNodes = node.numNodes + 1;

                // there is a free path towards goal
                if (node.edgeNum == -1) {
                    if (node.numNodes < bestNumNodes) {
                        bestNumNodes = node.numNodes;
                    }
                    continue;
                }

                child.obstacle = node.obstacle;
                obstaclePoints = obstacles[node.obstacle].winding.GetNumPoints();
                child.edgeNum = (node.edgeNum + obstaclePoints + (2 * node.dir - 1)) % obstaclePoints;

                if (GetPathNodeDelta(child, obstacles, seekPos, false)) {
                    pathNodeQueue.Add(child);
                }
            }
        }

        return root;
    }

    /*
     ============
     PrunePathTree
     ============
     */
    static void PrunePathTree(pathNode_s root, final idVec2 seekPos) {
        int i;
        float bestDist;
        pathNode_s node, lastNode, n, bestNode;

        node = root;
        while (node != null) {

            node.dist = (seekPos.oMinus(node.pos)).LengthSqr();

            if (node.children[0] != null) {
                node = node.children[0];
            } else if (node.children[1] != null) {
                node = node.children[1];
            } else {

                // find the node closest to the goal along this path
                bestDist = idMath.INFINITY;
                bestNode = node;
                for (n = node; n != null; n = n.parent) {
                    if (n.children[0] != null && n.children[1] != null) {
                        break;
                    }
                    if (n.dist < bestDist) {
                        bestDist = n.dist;
                        bestNode = n;
                    }
                }

                // free tree down from the best node
                for (i = 0; i < 2; i++) {
                    if (bestNode.children[i] != null) {
                        FreePathTree_r(bestNode.children[i]);
                        bestNode.children[i] = null;
                    }
                }

                for (lastNode = bestNode, node = bestNode.parent; node != null; lastNode = node, node = node.parent) {
                    if (node.children[1] != null && (node.children[1] != lastNode)) {
                        node = node.children[1];
                        break;
                    }
                }
            }
        }
    }

    /*
     ============
     OptimizePath
     ============
     */
    static int OptimizePath(final pathNode_s root, final pathNode_s leafNode, final obstacle_s[] obstacles, int numObstacles, idVec2[] optimizedPath/*[MAX_OBSTACLE_PATH]*/) {
        int i, numPathPoints;
        int[] edgeNums = new int[2];
        pathNode_s curNode, nextNode;
        idVec2 curPos, curDelta;
        idVec2[] bounds = new idVec2[2];
        float curLength;
        float[] scale1 = {0}, scale2 = {0};

        optimizedPath[0] = root.pos;
        numPathPoints = 1;

        for (nextNode = curNode = root; curNode != leafNode; curNode = nextNode) {

            for (nextNode = leafNode; nextNode.parent != curNode; nextNode = nextNode.parent) {

                // can only take shortcuts when going from one object to another
                if (nextNode.obstacle == curNode.obstacle) {
                    continue;
                }

                curPos = curNode.pos;
                curDelta = nextNode.pos.oMinus(curPos);
                curLength = curDelta.Length();

                // get bounds for the current movement delta
                bounds[0] = curPos.oMinus(new idVec2(CM_BOX_EPSILON, CM_BOX_EPSILON));
                bounds[1] = curPos.oPlus(new idVec2(CM_BOX_EPSILON, CM_BOX_EPSILON));
                bounds[FLOATSIGNBITNOTSET(curDelta.x)].x += curDelta.x;
                bounds[FLOATSIGNBITNOTSET(curDelta.y)].y += curDelta.y;

                // test if the shortcut intersects with any obstacles
                for (i = 0; i < numObstacles; i++) {
                    if (bounds[0].x > obstacles[i].bounds[1].x || bounds[0].y > obstacles[i].bounds[1].y
                            || bounds[1].x < obstacles[i].bounds[0].x || bounds[1].y < obstacles[i].bounds[0].y) {
                        continue;
                    }
                    if (obstacles[i].winding.RayIntersection(curPos, curDelta, scale1, scale2, edgeNums)) {
                        if (scale1[0] >= 0.0f && scale1[0] <= 1.0f && (i != nextNode.obstacle || scale1[0] * curLength < curLength - 0.5f)) {
                            break;
                        }
                        if (scale2[0] >= 0.0f && scale2[0] <= 1.0f && (i != nextNode.obstacle || scale2[0] * curLength < curLength - 0.5f)) {
                            break;
                        }
                    }
                }
                if (i >= numObstacles) {
                    break;
                }
            }

            // store the next position along the optimized path
            optimizedPath[numPathPoints++] = nextNode.pos;
        }

        return numPathPoints;
    }

    /*
     ============
     PathLength
     ============
     */
    static float PathLength(idVec2[] optimizedPath/*[MAX_OBSTACLE_PATH]*/, int numPathPoints, final idVec2 curDir) {
        int i;
        float pathLength;

        // calculate the path length
        pathLength = 0.0f;
        for (i = 0; i < numPathPoints - 1; i++) {
            pathLength += (optimizedPath[i + 1].oMinus(optimizedPath[i])).LengthFast();
        }

        // add penalty if this path does not go in the current direction
        if (curDir.oMultiply(optimizedPath[1].oMinus(optimizedPath[0])) < 0.0f) {
            pathLength += 100.0f;
        }
        return pathLength;
    }

    /*
     ============
     FindOptimalPath

     Returns true if there is a path all the way to the goal.
     ============
     */
    static boolean FindOptimalPath(final pathNode_s root, final obstacle_s[] obstacles, int numObstacles, final float height, final idVec3 curDir, idVec3 seekPos) {
        int i, numPathPoints, bestNumPathPoints;
        pathNode_s node, lastNode, bestNode;
        idVec2[] optimizedPath = new idVec2[MAX_OBSTACLE_PATH];
        float pathLength, bestPathLength;
        boolean pathToGoalExists, optimizedPathCalculated;

        optimizedPath[1] = new idVec2(-107374176, -107374176);
        seekPos.Zero();
        seekPos.z = height;

        pathToGoalExists = false;
        optimizedPathCalculated = false;

        bestNode = root;
//        bestNumPathPoints = 0;
        bestPathLength = idMath.INFINITY;

        node = root;
        while (node != null) {

            pathToGoalExists |= (node.dist < 0.1f);

            if (node.dist <= bestNode.dist) {

                if (idMath.Fabs(node.dist - bestNode.dist) < 0.1f) {

                    if (!optimizedPathCalculated) {
                        bestNumPathPoints = OptimizePath(root, bestNode, obstacles, numObstacles, optimizedPath);
                        bestPathLength = PathLength(optimizedPath, bestNumPathPoints, curDir.ToVec2());
                        seekPos.oSet(optimizedPath[1]);
                    }

                    numPathPoints = OptimizePath(root, node, obstacles, numObstacles, optimizedPath);
                    pathLength = PathLength(optimizedPath, numPathPoints, curDir.ToVec2());

                    if (pathLength < bestPathLength) {
                        bestNode = node;
                        bestNumPathPoints = numPathPoints;
                        bestPathLength = pathLength;
                        seekPos.oSet(optimizedPath[1]);
                    }
                    optimizedPathCalculated = true;

                } else {

                    bestNode = node;
                    optimizedPathCalculated = false;
                }
            }

            if (node.children[0] != null) {
                node = node.children[0];
            } else if (node.children[1] != null) {
                node = node.children[1];
            } else {
                for (lastNode = node, node = node.parent; node != null; lastNode = node, node = node.parent) {
                    if (node.children[1] != null && !node.children[1].equals(lastNode)) {
                        node = node.children[1];
                        break;
                    }
                }
            }
        }

        if (!pathToGoalExists) {
            seekPos.oSet(root.children[0].pos);
        } else if (!optimizedPathCalculated) {
            OptimizePath(root, bestNode, obstacles, numObstacles, optimizedPath);
            seekPos.oSet(optimizedPath[1]);
        }

        if (ai_showObstacleAvoidance.GetBool()) {
            idVec3 start = new idVec3(), end = new idVec3();
            start.z = end.z = height + 4.0f;
            numPathPoints = OptimizePath(root, bestNode, obstacles, numObstacles, optimizedPath);
            for (i = 0; i < numPathPoints - 1; i++) {
                start.oSet(optimizedPath[i]);
                end.oSet(optimizedPath[i + 1]);
                gameRenderWorld.DebugArrow(colorCyan, start, end, 1);
            }
        }

        return pathToGoalExists;
    }

    /*
     ===============================================================================

     Path Prediction

     Uses the AAS to quickly and accurately predict a path for a certain
     period of time based on an initial position and velocity.

     ===============================================================================
     */
    /*
     ============
     PathTrace

     Returns true if a stop event was triggered.
     ============
     */
    static boolean PathTrace(final idEntity ent, final idAAS aas, final idVec3 start, final idVec3 end, int stopEvent, pathTrace_s trace, predictedPath_s path) {
        trace_s[] clipTrace = {null};
        aasTrace_s aasTrace = new aasTrace_s();

//	memset( &trace, 0, sizeof( trace ) );TODO:
        if (NOT(aas) || NOT(aas.GetSettings())) {

            gameLocal.clip.Translation(clipTrace, start, end, ent.GetPhysics().GetClipModel(),
                    ent.GetPhysics().GetClipModel().GetAxis(), MASK_MONSTERSOLID, ent);

            // NOTE: could do (expensive) ledge detection here for when there is no AAS file
            trace.fraction = clipTrace[0].fraction;
            trace.endPos = clipTrace[0].endpos;
            trace.normal = clipTrace[0].c.normal;
            trace.blockingEntity = gameLocal.entities[clipTrace[0].c.entityNum];
        } else {
            aasTrace.getOutOfSolid = 1;//true;
            if ((stopEvent & SE_ENTER_LEDGE_AREA) != 0) {
                aasTrace.flags |= AREA_LEDGE;
            }
            if ((stopEvent & SE_ENTER_OBSTACLE) != 0) {
                aasTrace.travelFlags |= TFL_INVALID;
            }

            aas.Trace(aasTrace, start, end);

            gameLocal.clip.TranslationEntities(clipTrace[0], start, aasTrace.endpos, ent.GetPhysics().GetClipModel(),
                    ent.GetPhysics().GetClipModel().GetAxis(), MASK_MONSTERSOLID, ent);

            if (clipTrace[0].fraction >= 1.0f) {

                trace.fraction = aasTrace.fraction;
                trace.endPos = aasTrace.endpos;
                trace.normal = aas.GetPlane(aasTrace.planeNum).Normal();
                trace.blockingEntity = gameLocal.world;

                if (aasTrace.fraction < 1.0f) {
                    if ((stopEvent & SE_ENTER_LEDGE_AREA) != 0) {
                        if ((aas.AreaFlags(aasTrace.blockingAreaNum) & AREA_LEDGE) != 0) {
                            path.endPos = trace.endPos;
                            path.endNormal = trace.normal;
                            path.endEvent = SE_ENTER_LEDGE_AREA;
                            path.blockingEntity = trace.blockingEntity;

                            if (ai_debugMove.GetBool()) {
                                gameRenderWorld.DebugLine(colorRed, start, aasTrace.endpos);
                            }
                            return true;
                        }
                    }
                    if ((stopEvent & SE_ENTER_OBSTACLE) != 0) {
                        if ((aas.AreaTravelFlags(aasTrace.blockingAreaNum) & TFL_INVALID) != 0) {
                            path.endPos = trace.endPos;
                            path.endNormal = trace.normal;
                            path.endEvent = SE_ENTER_OBSTACLE;
                            path.blockingEntity = trace.blockingEntity;

                            if (ai_debugMove.GetBool()) {
                                gameRenderWorld.DebugLine(colorRed, start, aasTrace.endpos);
                            }
                            return true;
                        }
                    }
                }
            } else {
                trace.fraction = clipTrace[0].fraction;
                trace.endPos = clipTrace[0].endpos;
                trace.normal = clipTrace[0].c.normal;
                trace.blockingEntity = gameLocal.entities[ clipTrace[0].c.entityNum];
            }
        }

        if (trace.fraction >= 1.0f) {
            trace.blockingEntity = null;
        }

        return false;
    }

    /*
     ===============================================================================

     Trajectory Prediction

     Finds the best collision free trajectory for a clip model based on an
     initial position, target position and speed.

     ===============================================================================
     */

    /*
     =====================
     Ballistics

     get the ideal aim pitch angle in order to hit the target
     also get the time it takes for the projectile to arrive at the target
     =====================
     */
    static class ballistics_s {

        float angle;		// angle in degrees in the range [-180, 180]
        float time;		// time it takes before the projectile arrives
    };

    static int Ballistics(final idVec3 start, final idVec3 end, float speed, float gravity, ballistics_s[] bal/*[2]*/) {
        int n, i;
        float x, y, a, b, c, d, sqrtd, inva;
        float[] p = new float[2];

        x = (end.ToVec2().oMinus(start.ToVec2())).Length();
        y = end.oGet(2) - start.oGet(2);

        a = 4.0f * y * y + 4.0f * x * x;
        b = -4.0f * speed * speed - 4.0f * y * gravity;
        c = gravity * gravity;

        d = b * b - 4.0f * a * c;
        if (d <= 0.0f || a == 0.0f) {
            return 0;
        }
        sqrtd = idMath.Sqrt(d);
        inva = 0.5f / a;
        p[0] = (-b + sqrtd) * inva;
        p[1] = (-b - sqrtd) * inva;
        n = 0;
        for (i = 0; i < 2; i++) {
            if (p[i] <= 0.0f) {
                continue;
            }
            d = idMath.Sqrt(p[i]);
            bal[n].angle = (float) atan2(0.5f * (2.0f * y * p[i] - gravity) / d, d * x);
            bal[n].time = (float) (x / (cos(bal[n].angle) * speed));
            bal[n].angle = idMath.AngleNormalize180(RAD2DEG(bal[n].angle));
            n++;
        }

        return n;
    }

    /*
     =====================
     HeightForTrajectory

     Returns the maximum hieght of a given trajectory
     =====================
     */
    static float HeightForTrajectory(final idVec3 start, float zVel, float gravity) {
        float maxHeight, t;

        t = zVel / gravity;
        // maximum height of projectile
        maxHeight = start.z - 0.5f * gravity * (t * t);

        return maxHeight;
    }
}
