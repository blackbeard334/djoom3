package neo.Game.AI;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static neo.Game.AI.AAS.PATHTYPE_BARRIERJUMP;
import static neo.Game.AI.AAS.PATHTYPE_JUMP;
import static neo.Game.AI.AAS.PATHTYPE_WALK;
import static neo.Game.AI.AAS.PATHTYPE_WALKOFFLEDGE;
import static neo.Game.AI.AAS_pathing.SUBSAMPLE_WALK_PATH;
import static neo.Game.AI.AAS_pathing.flyPathSampleDistance;
import static neo.Game.AI.AAS_pathing.maxFlyPathDistance;
import static neo.Game.AI.AAS_pathing.maxFlyPathIterations;
import static neo.Game.AI.AAS_pathing.maxWalkPathDistance;
import static neo.Game.AI.AAS_pathing.maxWalkPathIterations;
import static neo.Game.AI.AAS_pathing.walkPathSampleDistance;
import static neo.Game.AI.AAS_routing.CACHETYPE_AREA;
import static neo.Game.AI.AAS_routing.CACHETYPE_PORTAL;
import static neo.Game.AI.AAS_routing.LEDGE_TRAVELTIME_PANALTY;
import static neo.Game.AI.AAS_routing.MAX_ROUTING_CACHE_MEMORY;
import static neo.Game.GameSys.SysCvar.aas_goalArea;
import static neo.Game.GameSys.SysCvar.aas_pullPlayer;
import static neo.Game.GameSys.SysCvar.aas_randomPullPlayer;
import static neo.Game.GameSys.SysCvar.aas_showAreas;
import static neo.Game.GameSys.SysCvar.aas_showFlyPath;
import static neo.Game.GameSys.SysCvar.aas_showHideArea;
import static neo.Game.GameSys.SysCvar.aas_showPath;
import static neo.Game.GameSys.SysCvar.aas_showPushIntoArea;
import static neo.Game.GameSys.SysCvar.aas_showWallEdges;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameRenderWorld;
import static neo.TempDump.NOT;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_CLUSTERPORTAL;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_OBSTACLE;
import static neo.Tools.Compilers.AAS.AASFile.AREA_LEDGE;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_FLY;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_WALK;
import static neo.Tools.Compilers.AAS.AASFile.FACE_FLOOR;
import static neo.Tools.Compilers.AAS.AASFile.MAX_REACH_PER_AREA;
import static neo.Tools.Compilers.AAS.AASFile.TFL_AIR;
import static neo.Tools.Compilers.AAS.AASFile.TFL_BARRIERJUMP;
import static neo.Tools.Compilers.AAS.AASFile.TFL_CROUCH;
import static neo.Tools.Compilers.AAS.AASFile.TFL_FLY;
import static neo.Tools.Compilers.AAS.AASFile.TFL_INVALID;
import static neo.Tools.Compilers.AAS.AASFile.TFL_JUMP;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WALK;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WALKOFFLEDGE;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WATER;
import static neo.Tools.Compilers.AAS.AASFileManager.AASFileManager;
import static neo.framework.Common.common;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorPurple;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import static neo.idlib.math.Math_h.INTSIGNBITNOTSET;
import static neo.idlib.math.Math_h.INTSIGNBITSET;
import static neo.idlib.math.Math_h.Square;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.idlib.math.Vector.getVec3_origin;

import java.nio.IntBuffer;

import neo.Game.Game_local.idGameLocal;
import neo.Game.Player.idPlayer;
import neo.Game.AI.AAS.aasGoal_s;
import neo.Game.AI.AAS.aasObstacle_s;
import neo.Game.AI.AAS.aasPath_s;
import neo.Game.AI.AAS.idAAS;
import neo.Game.AI.AAS.idAASCallback;
import neo.Game.AI.AAS_routing.idRoutingCache;
import neo.Game.AI.AAS_routing.idRoutingObstacle;
import neo.Game.AI.AAS_routing.idRoutingUpdate;
import neo.Game.AI.AI.idAASFindCover;
import neo.Game.AI.AI_pathing.wallEdge_s;
import neo.Game.Physics.Physics.idPhysics;
import neo.Tools.Compilers.AAS.AASFile.aasArea_s;
import neo.Tools.Compilers.AAS.AASFile.aasCluster_s;
import neo.Tools.Compilers.AAS.AASFile.aasEdge_s;
import neo.Tools.Compilers.AAS.AASFile.aasFace_s;
import neo.Tools.Compilers.AAS.AASFile.aasNode_s;
import neo.Tools.Compilers.AAS.AASFile.aasPortal_s;
import neo.Tools.Compilers.AAS.AASFile.aasTrace_s;
import neo.Tools.Compilers.AAS.AASFile.idAASFile;
import neo.Tools.Compilers.AAS.AASFile.idAASSettings;
import neo.Tools.Compilers.AAS.AASFile.idReachability;
import neo.Tools.Compilers.AAS.AASFile.idReachability_Walk;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class AAS_local {

    static class idAASLocal extends idAAS {

        private idAASFile                 file;
        private idStr                     name;
        //
        // routing data
        private idRoutingCache[][]        areaCacheIndex;        // for each area in each cluster the travel times to all other areas in the cluster
        private int                       areaCacheIndexSize;    // number of area cache entries
        private idRoutingCache[]          portalCacheIndex;      // for each area in the world the travel times from each portal
        private int                       portalCacheIndexSize;  // number of portal cache entries
        private idRoutingUpdate[]         areaUpdate;            // memory used to update the area routing cache
        private idRoutingUpdate[]         portalUpdate;          // memory used to update the portal routing cache
        private int[]                     goalAreaTravelTimes;   // travel times to goal areas
        private int[]                     areaTravelTimes;       // travel times through the areas
        private int                       numAreaTravelTimes;    // number of area travel times
        private idRoutingCache            cacheListStart;        // start of list with cache sorted from oldest to newest
        private idRoutingCache            cacheListEnd;          // end of list with cache sorted from oldest to newest
        private int                       totalCacheMemory;      // total cache memory used
        private final idList<idRoutingObstacle> obstacleList;          // list with obstacles
        //
        //

        public idAASLocal() {
            this.file = null;
            this.obstacleList = new idList<>();
        }
        // virtual						~idAASLocal();

        @Override
        public boolean Init(final idStr mapName, /*unsigned int*/ long mapFileCRC) {
            if ((this.file != null) && (mapName.Icmp(this.file.GetName()) == 0) && (mapFileCRC == this.file.GetCRC())) {
                common.Printf("Keeping %s\n", this.file.GetName());
                RemoveAllObstacles();
            } else {
                Shutdown();

                this.file = AASFileManager.LoadAAS(mapName.getData(), mapFileCRC);
                if (NOT(this.file)) {
                    common.DWarning("Couldn't load AAS file: '%s'", mapName.getData());
                    return false;
                }
                SetupRouting();
            }
            return true;
        }

        public void Shutdown() {
            if (this.file != null) {
                ShutdownRouting();
                RemoveAllObstacles();
                AASFileManager.FreeAAS(this.file);
                this.file = null;
            }
        }

        @Override
        public void Stats() {
            if (NOT(this.file)) {
                return;
            }
            common.Printf("[%s]\n", this.file.GetName());
            this.file.PrintInfo();
            RoutingStats();
        }

        @Override
        public void Test(final idVec3 origin) {

            if (NOT(this.file)) {
                return;
            }

            if (aas_randomPullPlayer.GetBool()) {
                RandomPullPlayer(origin);
            }
            if ((aas_pullPlayer.GetInteger() > 0) && (aas_pullPlayer.GetInteger() < this.file.GetNumAreas())) {
                ShowWalkPath(origin, aas_pullPlayer.GetInteger(), AreaCenter(aas_pullPlayer.GetInteger()));
                PullPlayer(origin, aas_pullPlayer.GetInteger());
            }
            if ((aas_showPath.GetInteger() > 0) && (aas_showPath.GetInteger() < this.file.GetNumAreas())) {
                ShowWalkPath(origin, aas_showPath.GetInteger(), AreaCenter(aas_showPath.GetInteger()));
            }
            if ((aas_showFlyPath.GetInteger() > 0) && (aas_showFlyPath.GetInteger() < this.file.GetNumAreas())) {
                ShowFlyPath(origin, aas_showFlyPath.GetInteger(), AreaCenter(aas_showFlyPath.GetInteger()));
            }
            if ((aas_showHideArea.GetInteger() > 0) && (aas_showHideArea.GetInteger() < this.file.GetNumAreas())) {
                ShowHideArea(origin, aas_showHideArea.GetInteger());
            }
            if (aas_showAreas.GetBool()) {
                ShowArea(origin);
            }
            if (aas_showWallEdges.GetBool()) {
                ShowWallEdges(origin);
            }
            if (aas_showPushIntoArea.GetBool()) {
                ShowPushIntoArea(origin);
            }
        }

        @Override
        public idAASSettings GetSettings() {
            if (NOT(this.file)) {
                return null;
            }
            return this.file.GetSettings();
        }

        @Override
        public int PointAreaNum(final idVec3 origin) {
            if (NOT(this.file)) {
                return 0;
            }
            return this.file.PointAreaNum(origin);
        }

        @Override
        public int PointReachableAreaNum(final idVec3 origin, final idBounds searchBounds, final int areaFlags) {
            if (NOT(this.file)) {
                return 0;
            }

            return this.file.PointReachableAreaNum(origin, searchBounds, areaFlags, TFL_INVALID);
        }

        @Override
        public int BoundsReachableAreaNum(final idBounds bounds, final int areaFlags) {
            if (NOT(this.file)) {
                return 0;
            }

            return this.file.BoundsReachableAreaNum(bounds, areaFlags, TFL_INVALID);
        }

        @Override
        public void PushPointIntoAreaNum(int areaNum, idVec3 origin) {
            if (NOT(this.file)) {
                return;
            }
            this.file.PushPointIntoAreaNum(areaNum, origin);
        }

        @Override
        public idVec3 AreaCenter(int areaNum) {
            if (NOT(this.file)) {
                return getVec3_origin();
            }
            return this.file.GetArea(areaNum).center;
        }

        @Override
        public int AreaFlags(int areaNum) {
            if (NOT(this.file)) {
                return 0;
            }
            return this.file.GetArea(areaNum).flags;
        }

        @Override
        public int AreaTravelFlags(int areaNum) {
            if (NOT(this.file)) {
                return 0;
            }
            return this.file.GetArea(areaNum).travelFlags;
        }

        @Override
        public boolean Trace(aasTrace_s trace, final idVec3 start, final idVec3 end) {
            if (NOT(this.file)) {
                trace.fraction
                        = trace.lastAreaNum
                        = trace.numAreas = 0;
                return true;
            }
            return this.file.Trace(trace, start, end);
        }
        private static idPlane dummy;

        @Override
        public idPlane GetPlane(int planeNum) {
            if (NOT(this.file)) {
                return dummy;
            }
            return this.file.GetPlane(planeNum);
        }

        @Override
        public int GetWallEdges(int areaNum, final idBounds bounds, int travelFlags, int[] edges, int maxEdges) {
            int i, j, k, l, face1Num, face2Num, edge1Num, edge2Num, numEdges, absEdge1Num;
            int curArea, queueStart, queueEnd;
            int[] areaQueue;
            byte[] areasVisited;
            aasArea_s area;
            aasFace_s face1, face2;
            idReachability reach;

            if (NOT(this.file)) {
                return 0;
            }

            numEdges = 0;

            areasVisited = new byte[this.file.GetNumAreas()];//	memset( areasVisited, 0, file.GetNumAreas() * sizeof( byte ) );
            areaQueue = new int[this.file.GetNumAreas()];

            queueStart = -1;
            queueEnd = 0;
            areaQueue[0] = areaNum;
            areasVisited[areaNum] = 1;//true;

            for (curArea = areaNum; queueStart < queueEnd; curArea = areaQueue[++queueStart]) {

                area = this.file.GetArea(curArea);

                for (i = 0; i < area.numFaces; i++) {
                    face1Num = this.file.GetFaceIndex(area.firstFace + i);
                    face1 = this.file.GetFace(abs(face1Num));

                    if (0 == (face1.flags & FACE_FLOOR)) {
                        continue;
                    }

                    for (j = 0; j < face1.numEdges; j++) {
                        edge1Num = this.file.GetEdgeIndex(face1.firstEdge + j);
                        absEdge1Num = abs(edge1Num);

                        // test if the edge is shared by another floor face of this area
                        for (k = 0; k < area.numFaces; k++) {
                            if (k == i) {
                                continue;
                            }
                            face2Num = this.file.GetFaceIndex(area.firstFace + k);
                            face2 = this.file.GetFace(abs(face2Num));

                            if (0 == (face2.flags & FACE_FLOOR)) {
                                continue;
                            }

                            for (l = 0; l < face2.numEdges; l++) {
                                edge2Num = abs(this.file.GetEdgeIndex(face2.firstEdge + l));
                                if (edge2Num == absEdge1Num) {
                                    break;
                                }
                            }
                            if (l < face2.numEdges) {
                                break;
                            }
                        }
                        if (k < area.numFaces) {
                            continue;
                        }

                        // test if the edge is used by a reachability
                        for (reach = area.reach; reach != null; reach = reach.next) {
                            if ((reach.travelType & travelFlags) != 0) {
                                if (reach.edgeNum == absEdge1Num) {
                                    break;
                                }
                            }
                        }
                        if (reach != null) {
                            continue;
                        }

                        // test if the edge is already in the list
                        for (k = 0; k < numEdges; k++) {
                            if (edge1Num == edges[k]) {
                                break;
                            }
                        }
                        if (k < numEdges) {
                            continue;
                        }

                        // add the edge to the list
                        edges[numEdges++] = edge1Num;
                        if (numEdges >= maxEdges) {
                            return numEdges;
                        }
                    }
                }

                // add new areas to the queue
                for (reach = area.reach; reach != null; reach = reach.next) {
                    if ((reach.travelType & travelFlags) != 0) {
                        // if the area the reachability leads to hasn't been visited yet and the area bounds touch the search bounds
                        if ((0 == areasVisited[reach.toAreaNum]) && bounds.IntersectsBounds(this.file.GetArea(reach.toAreaNum).bounds)) {
                            areaQueue[queueEnd++] = reach.toAreaNum;
                            areasVisited[reach.toAreaNum] = 1;//true;
                        }
                    }
                }
            }
            return numEdges;
        }

        @Override
        public void SortWallEdges(int[] edges, int numEdges) {
            int i, j, k, numSequences;
            wallEdge_s[] sequenceFirst, sequenceLast;
            wallEdge_s[] wallEdges;
            wallEdge_s wallEdge;

            wallEdges = new wallEdge_s[numEdges];
            sequenceFirst = new wallEdge_s[numEdges];
            sequenceLast = new wallEdge_s[numEdges];

            for (i = 0; i < numEdges; i++) {
                wallEdges[i] = new wallEdge_s();
                wallEdges[i].edgeNum = edges[i];
                GetEdgeVertexNumbers(edges[i], wallEdges[i].verts);
                wallEdges[i].next = null;
                sequenceFirst[i] = wallEdges[i];
                sequenceLast[i] = wallEdges[i];
            }
            numSequences = numEdges;

            for (i = 0; i < numSequences; i++) {
                for (j = i + 1; j < numSequences; j++) {
                    if (sequenceFirst[i].verts[0] == sequenceLast[j].verts[1]) {
                        sequenceLast[j].next = sequenceFirst[i];
                        sequenceFirst[i] = sequenceFirst[j];
                        break;
                    }
                    if (sequenceLast[i].verts[1] == sequenceFirst[j].verts[0]) {
                        sequenceLast[i].next = sequenceFirst[j];
                        break;
                    }
                }
                if (j < numSequences) {
                    numSequences--;
                    for (k = j; k < numSequences; k++) {
                        sequenceFirst[k] = sequenceFirst[k + 1];
                        sequenceLast[k] = sequenceLast[k + 1];
                    }
                    i = -1;
                }
            }

            k = 0;
            for (i = 0; i < numSequences; i++) {
                for (wallEdge = sequenceFirst[i]; wallEdge != null; wallEdge = wallEdge.next) {
                    edges[k++] = wallEdge.edgeNum;
                }
            }
        }

        @Override
        public void GetEdgeVertexNumbers(int edgeNum, int[] verts/*[2]*/) {
            if (NOT(this.file)) {
                verts[0] = verts[1] = 0;
                return;
            }
            final int[] v = this.file.GetEdge(abs(edgeNum)).vertexNum;
            verts[0] = v[INTSIGNBITSET(edgeNum)];
            verts[1] = v[INTSIGNBITNOTSET(edgeNum)];
        }

        @Override
        public void GetEdge(int edgeNum, idVec3 start, idVec3 end) {
            if (NOT(this.file)) {
                start.Zero();
                end.Zero();
                return;
            }
            final int[] v = this.file.GetEdge(abs(edgeNum)).vertexNum;
            start.oSet(this.file.GetVertex(v[INTSIGNBITSET(edgeNum)]));
            end.oSet(this.file.GetVertex(v[INTSIGNBITNOTSET(edgeNum)]));
        }

        @Override
        public boolean SetAreaState(final idBounds bounds, final int areaContents, boolean disabled) {
            final idBounds expBounds = new idBounds();

            if (NOT(this.file)) {
                return false;
            }

            expBounds.oSet(0, bounds.oGet(0).oMinus(this.file.GetSettings().boundingBoxes[0].oGet(1)));
            expBounds.oSet(1, bounds.oGet(1).oMinus(this.file.GetSettings().boundingBoxes[0].oGet(0)));

            // find all areas within or touching the bounds with the given contents and disable/enable them for routing
            return SetAreaState_r(1, expBounds, areaContents, disabled);
        }

        @Override
        public int/*aasHandle_t*/ AddObstacle(final idBounds bounds) {
            idRoutingObstacle obstacle;

            if (NOT(this.file)) {
                return -1;
            }

            obstacle = new idRoutingObstacle();
            obstacle.bounds.oSet(0, bounds.oGet(0).oMinus(this.file.GetSettings().boundingBoxes[0].oGet(1)));
            obstacle.bounds.oSet(1, bounds.oGet(1).oMinus(this.file.GetSettings().boundingBoxes[0].oGet(0)));
            GetBoundsAreas_r(1, obstacle.bounds, obstacle.areas);
            SetObstacleState(obstacle, true);

            this.obstacleList.Append(obstacle);
            return this.obstacleList.Num() - 1;
        }

        @Override
        public void RemoveObstacle(final int/*aasHandle_t*/ handle) {
            if (NOT(this.file)) {
                return;
            }
            if ((handle >= 0) && (handle < this.obstacleList.Num())) {
                SetObstacleState(this.obstacleList.oGet(handle), false);

//		delete obstacleList[handle];
                this.obstacleList.RemoveIndex(handle);
            }
        }

        @Override
        public void RemoveAllObstacles() {
            int i;

            if (NOT(this.file)) {
                return;
            }

            for (i = 0; i < this.obstacleList.Num(); i++) {
                SetObstacleState(this.obstacleList.oGet(i), false);
//		delete obstacleList[i];
            }
            this.obstacleList.Clear();
        }

        @Override
        public int TravelTimeToGoalArea(int areaNum, final idVec3 origin, int goalAreaNum, int travelFlags) {
            final int[] travelTime = {0};
            final idReachability[] reach = {null};

            if (NOT(this.file)) {
                return 0;
            }

            if (!RouteToGoalArea(areaNum, origin, goalAreaNum, travelFlags, travelTime, reach)) {
                return 0;
            }
            return travelTime[0];
        }

        @Override
        public boolean RouteToGoalArea(int areaNum, final idVec3 origin, int goalAreaNum, int travelFlags, int[] travelTime, idReachability[] reach) {
            int clusterNum, goalClusterNum, portalNum, i, clusterAreaNum;
            /*unsigned short*/ int t, bestTime;
            aasPortal_s portal;
            aasCluster_s cluster;
            idRoutingCache areaCache, portalCache, clusterCache;
            idReachability bestReach, r, nextr;

            travelTime[0] = 0;
            reach[0] = null;

            if (NOT(this.file)) {
                return false;
            }

            if (areaNum == goalAreaNum) {
                return true;
            }

            if ((areaNum <= 0) || (areaNum >= this.file.GetNumAreas())) {
                gameLocal.Printf("RouteToGoalArea: areaNum %d out of range\n", areaNum);
                return false;
            }
            if ((goalAreaNum <= 0) || (goalAreaNum >= this.file.GetNumAreas())) {
                gameLocal.Printf("RouteToGoalArea: goalAreaNum %d out of range\n", goalAreaNum);
                return false;
            }

            while (this.totalCacheMemory > MAX_ROUTING_CACHE_MEMORY) {
                DeleteOldestCache();
            }

            clusterNum = this.file.GetArea(areaNum).cluster;
            goalClusterNum = this.file.GetArea(goalAreaNum).cluster;

            // if the source area is a cluster portal, read directly from the portal cache
            if (clusterNum < 0) {
                // if the goal area is a portal
                if (goalClusterNum < 0) {
                    // just assume the goal area is part of the front cluster
                    portal = this.file.GetPortal(-goalClusterNum);
                    goalClusterNum = portal.clusters[0];
                }
                // get the portal routing cache
                portalCache = GetPortalRoutingCache(goalClusterNum, goalAreaNum, travelFlags);
                reach[0] = GetAreaReachability(areaNum, portalCache.reachabilities[-clusterNum]);
                travelTime[0] = portalCache.travelTimes[-clusterNum] + AreaTravelTime(areaNum, origin, (reach[0]).start);
                return true;
            }

            bestTime = 0;
            bestReach = null;

            // check if the goal area is a portal of the source area cluster
            if (goalClusterNum < 0) {
                portal = this.file.GetPortal(-goalClusterNum);
                if ((portal.clusters[0] == clusterNum) || (portal.clusters[1] == clusterNum)) {
                    goalClusterNum = clusterNum;
                }
            }

            // if both areas are in the same cluster
            if ((clusterNum > 0) && (goalClusterNum > 0) && (clusterNum == goalClusterNum)) {
                clusterCache = GetAreaRoutingCache(clusterNum, goalAreaNum, travelFlags);
                clusterAreaNum = ClusterAreaNum(clusterNum, areaNum);
                if (clusterCache.travelTimes[clusterAreaNum] != 0) {
                    bestReach = GetAreaReachability(areaNum, clusterCache.reachabilities[clusterAreaNum]);
                    bestTime = clusterCache.travelTimes[clusterAreaNum] + AreaTravelTime(areaNum, origin, bestReach.start);
                } else {
                    clusterCache = null;
                }
            } else {
                clusterCache = null;
            }

            clusterNum = this.file.GetArea(areaNum).cluster;
            goalClusterNum = this.file.GetArea(goalAreaNum).cluster;

            // if the goal area is a portal
            if (goalClusterNum < 0) {
                // just assume the goal area is part of the front cluster
                portal = this.file.GetPortal(-goalClusterNum);
                goalClusterNum = portal.clusters[0];
            }
            // get the portal routing cache
            portalCache = GetPortalRoutingCache(goalClusterNum, goalAreaNum, travelFlags);

            // the cluster the area is in
            cluster = this.file.GetCluster(clusterNum);
            // current area inside the current cluster
            clusterAreaNum = ClusterAreaNum(clusterNum, areaNum);
            // if the area is not a reachable area
            if (clusterAreaNum >= cluster.numReachableAreas) {
                return false;
            }

            // find the portal of the source area cluster leading towards the goal area
            for (i = 0; i < cluster.numPortals; i++) {
                portalNum = this.file.GetPortalIndex(cluster.firstPortal + i);

                // if the goal area isn't reachable from the portal
                if (0 == portalCache.travelTimes[portalNum]) {
                    continue;
                }

                portal = this.file.GetPortal(portalNum);
                // get the cache of the portal area
                areaCache = GetAreaRoutingCache(clusterNum, portal.areaNum, travelFlags);
                // if the portal is not reachable from this area
                if (0 == areaCache.travelTimes[clusterAreaNum]) {
                    continue;
                }

                r = GetAreaReachability(areaNum, areaCache.reachabilities[clusterAreaNum]);

                if (clusterCache != null) {
                    // if the next reachability from the portal leads back into the cluster
                    nextr = GetAreaReachability(portal.areaNum, portalCache.reachabilities[portalNum]);
                    if ((this.file.GetArea(nextr.toAreaNum).cluster < 0) || (this.file.GetArea(nextr.toAreaNum).cluster == clusterNum)) {
                        continue;
                    }
                }

                // the total travel time is the travel time from the portal area to the goal area
                // plus the travel time from the source area towards the portal area
                t = portalCache.travelTimes[portalNum] + areaCache.travelTimes[clusterAreaNum];
                // NOTE:	Should add the exact travel time through the portal area.
                //			However we add the largest travel time through the portal area.
                //			We cannot directly calculate the exact travel time through the portal area
                //			because the reachability used to travel into the portal area is not known.
                t += portal.maxAreaTravelTime;

                // if the time is better than the one already found
                if ((0 == bestTime) || (t < bestTime)) {
                    bestReach = r;
                    bestTime = t;
                }
            }

            if (NOT(bestReach)) {
                return false;
            }

            reach[0] = bestReach;
            travelTime[0] = bestTime;

            return true;
        }

        /*
         ============
         idAASLocal::WalkPathToGoal

         FIXME: don't stop optimizing on first failure ?
         ============
         */
        @Override
        public boolean WalkPathToGoal(aasPath_s path, int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin, int travelFlags) {
            int i, curAreaNum, lastAreaIndex;
            final int[] travelTime = {0}, endAreaNum = {0}, moveAreaNum = {0};
            final int[] lastAreas = new int[4];
            final idReachability[] reach = {null};
            final idVec3 endPos = new idVec3();

            path.type = PATHTYPE_WALK;
            path.moveGoal = origin;
            path.moveAreaNum = areaNum;
            path.secondaryGoal = origin;
            path.reachability = null;

            if ((this.file == null) || (areaNum == goalAreaNum)) {
                path.moveGoal = goalOrigin;
                return true;
            }

            lastAreas[0] = lastAreas[1] = lastAreas[2] = lastAreas[3] = areaNum;
            lastAreaIndex = 0;

            curAreaNum = areaNum;

            for (i = 0; i < maxWalkPathIterations; i++) {

                if (!this.RouteToGoalArea(curAreaNum, path.moveGoal, goalAreaNum, travelFlags, travelTime, reach)) {
                    break;
                }

                if (NOT(reach[0])) {
                    return false;
                }

                // no need to check through the first area
                if (areaNum != curAreaNum) {
                    // only optimize a limited distance ahead
                    if ((reach[0].start.oMinus(origin)).LengthSqr() > Square(maxWalkPathDistance)) {
                        if (SUBSAMPLE_WALK_PATH != 0) {
                            path.moveGoal = SubSampleWalkPath(areaNum, origin, path.moveGoal, reach[0].start, travelFlags, moveAreaNum);
                            path.moveAreaNum = moveAreaNum[0];
                        }
                        return true;
                    }

                    if (!this.WalkPathValid(areaNum, origin, 0, reach[0].start, travelFlags, endPos, endAreaNum)) {
                        if (SUBSAMPLE_WALK_PATH != 0) {
                            path.moveGoal = SubSampleWalkPath(areaNum, origin, path.moveGoal, reach[0].start, travelFlags, moveAreaNum);
                            path.moveAreaNum = moveAreaNum[0];
                        }
                        return true;
                    }
                }

                path.moveGoal = reach[0].start;
                path.moveAreaNum = curAreaNum;

                if (reach[0].travelType != TFL_WALK) {
                    break;
                }

                if (!this.WalkPathValid(areaNum, origin, 0, reach[0].end, travelFlags, endPos, endAreaNum)) {
                    return true;
                }

                path.moveGoal = reach[0].end;
                path.moveAreaNum = reach[0].toAreaNum;

                if (reach[0].toAreaNum == goalAreaNum) {
                    if (!this.WalkPathValid(areaNum, origin, 0, goalOrigin, travelFlags, endPos, endAreaNum)) {
                        if (SUBSAMPLE_WALK_PATH != 0) {
                            path.moveGoal = SubSampleWalkPath(areaNum, origin, path.moveGoal, goalOrigin, travelFlags, moveAreaNum);
                            path.moveAreaNum = moveAreaNum[0];
                        }
                        return true;
                    }
                    path.moveGoal = goalOrigin;
                    path.moveAreaNum = goalAreaNum;
                    return true;
                }

                lastAreas[lastAreaIndex] = curAreaNum;
                lastAreaIndex = (lastAreaIndex + 1) & 3;

                curAreaNum = reach[0].toAreaNum;

                if ((curAreaNum == lastAreas[0]) || (curAreaNum == lastAreas[1])
                        || (curAreaNum == lastAreas[2]) || (curAreaNum == lastAreas[3])) {
                    common.Warning("idAASLocal::WalkPathToGoal: local routing minimum going from area %d to area %d", areaNum, goalAreaNum);
                    break;
                }
            }

            if (NOT(reach[0])) {
                return false;
            }

            switch (reach[0].travelType) {
                case TFL_WALKOFFLEDGE:
                    path.type = PATHTYPE_WALKOFFLEDGE;
                    path.secondaryGoal = reach[0].end;
                    path.reachability = reach[0];
                    break;
                case TFL_BARRIERJUMP:
                    path.type |= PATHTYPE_BARRIERJUMP;
                    path.secondaryGoal = reach[0].end;
                    path.reachability = reach[0];
                    break;
                case TFL_JUMP:
                    path.type |= PATHTYPE_JUMP;
                    path.secondaryGoal = reach[0].end;
                    path.reachability = reach[0];
//                    break;
                default:
                    break;
            }

            return true;
        }


        /*
         ============
         idAASLocal::WalkPathValid

         returns true if one can walk in a straight line between origin and goalOrigin
         ============
         */
        @Override
        public boolean WalkPathValid(int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin, int travelFlags, idVec3 endPos, int[] endAreaNum) {
            int curAreaNum, lastAreaNum, lastAreaIndex;
            final int[] lastAreas = new int[4];
            final idPlane pathPlane = new idPlane(), frontPlane = new idPlane(), farPlane = new idPlane();
            idReachability reach;
            aasArea_s area;
            final idVec3 p = new idVec3();
			idVec3 dir;

            if (this.file == null) {
                endPos.oSet(goalOrigin);
                endAreaNum[0] = 0;
                return true;
            }

            lastAreas[0] = lastAreas[1] = lastAreas[2] = lastAreas[3] = areaNum;
            lastAreaIndex = 0;

            pathPlane.SetNormal((goalOrigin.oMinus(origin)).Cross(this.file.GetSettings().gravityDir));
            pathPlane.Normalize();
            pathPlane.FitThroughPoint(origin);

            frontPlane.SetNormal(goalOrigin.oMinus(origin));
            frontPlane.Normalize();
            frontPlane.FitThroughPoint(origin);

            farPlane.SetNormal(frontPlane.Normal());
            farPlane.FitThroughPoint(goalOrigin);

            curAreaNum = areaNum;
            lastAreaNum = curAreaNum;

            while (true) {

                // find the furthest floor face split point on the path
                if (!FloorEdgeSplitPoint(endPos, curAreaNum, pathPlane, frontPlane, false)) {
                    endPos = origin;
                }

                // if we found a point near or further than the goal we're done
                if (farPlane.Distance(endPos) > -0.5f) {
                    break;
                }

                // if we reached the goal area we're done
                if (curAreaNum == goalAreaNum) {
                    break;
                }

                frontPlane.SetDist(frontPlane.Normal().oMultiply(endPos));

                area = this.file.GetArea(curAreaNum);

                for (reach = area.reach; reach != null; reach = reach.next) {
                    if (reach.travelType != TFL_WALK) {
                        continue;
                    }

                    // if the reachability goes back to a previous area
                    if ((reach.toAreaNum == lastAreas[0]) || (reach.toAreaNum == lastAreas[1])
                            || (reach.toAreaNum == lastAreas[2]) || (reach.toAreaNum == lastAreas[3])) {
                        continue;
                    }

                    // if undesired travel flags are required to travel through the area
                    if ((this.file.GetArea(reach.toAreaNum).travelFlags & ~travelFlags) != 0) {
                        continue;
                    }

                    // don't optimize through an area near a ledge
                    if ((this.file.GetArea(reach.toAreaNum).flags & AREA_LEDGE) != 0) {
                        continue;
                    }

                    // find the closest floor face split point on the path
                    if (!FloorEdgeSplitPoint(p, reach.toAreaNum, pathPlane, frontPlane, true)) {
                        continue;
                    }

                    // direction parallel to gravity
                    dir = (this.file.GetSettings().gravityDir.oMultiply(endPos.oMultiply(this.file.GetSettings().gravityDir))).
                            oMinus(this.file.GetSettings().gravityDir.oMultiply(p.oMultiply(this.file.GetSettings().gravityDir)));
                    if (dir.LengthSqr() > Square(this.file.GetSettings().maxStepHeight[0])) {
                        continue;
                    }

                    // direction orthogonal to gravity
                    dir = endPos.oMinus(p.oMinus(dir));
                    if (dir.LengthSqr() > Square(0.2f)) {
                        continue;
                    }

                    break;
                }

                if (NOT(reach)) {
                    return false;
                }

                lastAreas[lastAreaIndex] = curAreaNum;
                lastAreaIndex = (lastAreaIndex + 1) & 3;

                curAreaNum = reach.toAreaNum;
            }

            endAreaNum[0] = curAreaNum;

            return true;
        }

        /*
         ============
         idAASLocal::FlyPathToGoal

         FIXME: don't stop optimizing on first failure ?
         ============
         */
        @Override
        public boolean FlyPathToGoal(aasPath_s path, int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin, int travelFlags) {
            int i, curAreaNum, lastAreaIndex;
            final int[] travelTime = {0}, endAreaNum = {0}, moveAreaNum = {0};
            final int[] lastAreas = new int[4];
            final idReachability[] reach = {null};
            final idVec3 endPos = new idVec3();

            path.type = PATHTYPE_WALK;
            path.moveGoal = origin;
            path.moveAreaNum = areaNum;
            path.secondaryGoal = origin;
            path.reachability = null;

            if ((this.file == null) || (areaNum == goalAreaNum)) {
                path.moveGoal = goalOrigin;
                return true;
            }

            lastAreas[0] = lastAreas[1] = lastAreas[2] = lastAreas[3] = areaNum;
            lastAreaIndex = 0;

            curAreaNum = areaNum;

            for (i = 0; i < maxFlyPathIterations; i++) {

                if (!this.RouteToGoalArea(curAreaNum, path.moveGoal, goalAreaNum, travelFlags, travelTime, reach)) {
                    break;
                }

                if (null == reach[0]) {
                    return false;
                }

                // no need to check through the first area
                if (areaNum != curAreaNum) {
                    if ((reach[0].start.oMinus(origin)).LengthSqr() > Square(maxFlyPathDistance)) {
                        // There is no preprocessor in Java
                    	//if (SUBSAMPLE_FLY_PATH != 0) {
                            path.moveGoal = SubSampleFlyPath(areaNum, origin, path.moveGoal, reach[0].start, travelFlags, moveAreaNum);
                            path.moveAreaNum = moveAreaNum[0];
                        //}
                        return true;
                    }

                    if (!this.FlyPathValid(areaNum, origin, 0, reach[0].start, travelFlags, endPos, endAreaNum)) {
                        // There is no preprocessor in Java
                        //if (SUBSAMPLE_FLY_PATH != 0) {
                            path.moveGoal = SubSampleFlyPath(areaNum, origin, path.moveGoal, reach[0].start, travelFlags, moveAreaNum);
                            path.moveAreaNum = moveAreaNum[0];
                        //}
                        return true;
                    }
                }

                path.moveGoal = reach[0].start;
                path.moveAreaNum = curAreaNum;

                if (!this.FlyPathValid(areaNum, origin, 0, reach[0].end, travelFlags, endPos, endAreaNum)) {
                    return true;
                }

                path.moveGoal = reach[0].end;
                path.moveAreaNum = reach[0].toAreaNum;

                if (reach[0].toAreaNum == goalAreaNum) {
                    if (!this.FlyPathValid(areaNum, origin, 0, goalOrigin, travelFlags, endPos, endAreaNum)) {
                        // There is no preprocessor in Java
                        //if (SUBSAMPLE_FLY_PATH != 0) {
                            path.moveGoal = SubSampleFlyPath(areaNum, origin, path.moveGoal, goalOrigin, travelFlags, moveAreaNum);
                            path.moveAreaNum = moveAreaNum[0];
                        //}
                        return true;
                    }
                    path.moveGoal = goalOrigin;
                    path.moveAreaNum = goalAreaNum;
                    return true;
                }

                lastAreas[lastAreaIndex] = curAreaNum;
                lastAreaIndex = (lastAreaIndex + 1) & 3;

                curAreaNum = reach[0].toAreaNum;

                if ((curAreaNum == lastAreas[0]) || (curAreaNum == lastAreas[1])
                        || (curAreaNum == lastAreas[2]) || (curAreaNum == lastAreas[3])) {
                    common.Warning("idAASLocal::FlyPathToGoal: local routing minimum going from area %d to area %d", areaNum, goalAreaNum);
                    break;
                }
            }

            if (null == reach[0]) {
                return false;
            }

            return true;
        }

        /*
         ============
         idAASLocal::FlyPathValid

         returns true if one can fly in a straight line between origin and goalOrigin
         ============
         */
        @Override
        public boolean FlyPathValid(int areaNum, final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin, int travelFlags, idVec3 endPos, int[] endAreaNum) {
            final aasTrace_s trace = new aasTrace_s();

            if (this.file == null) {
                endPos = goalOrigin;
                endAreaNum[0] = 0;
                return true;
            }

            this.file.Trace(trace, origin, goalOrigin);

            endPos = trace.endpos;
            endAreaNum[0] = trace.lastAreaNum;

            if (trace.fraction >= 1.0f) {
                return true;
            }

            return false;
        }

        @Override
        public void ShowWalkPath(final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin) {
            int i, areaNum, curAreaNum;
            final int[] travelTime = {0};
            final idReachability[] reach = {null};
            idVec3 org;
			final idVec3 areaCenter = new idVec3();
            final aasPath_s path = new aasPath_s();

            if (NOT(this.file)) {
                return;
            }

            org = origin;
            areaNum = PointReachableAreaNum(org, DefaultSearchBounds(), AREA_REACHABLE_WALK);
            PushPointIntoAreaNum(areaNum, org);
            curAreaNum = areaNum;

            for (i = 0; i < 100; i++) {

                if (!RouteToGoalArea(curAreaNum, org, goalAreaNum, TFL_WALK | TFL_AIR, travelTime, reach)) {
                    break;
                }

                if (NOT(reach[0])) {
                    break;
                }

                gameRenderWorld.DebugArrow(colorGreen, org, reach[0].start, 2);
                DrawReachability(reach[0]);

                if (reach[0].toAreaNum == goalAreaNum) {
                    break;
                }

                curAreaNum = reach[0].toAreaNum;
                org = reach[0].end;
            }

            if (WalkPathToGoal(path, areaNum, origin, goalAreaNum, goalOrigin, TFL_WALK | TFL_AIR)) {
                gameRenderWorld.DebugArrow(colorBlue, origin, path.moveGoal, 2);
            }
        }

        @Override
        public void ShowFlyPath(final idVec3 origin, int goalAreaNum, final idVec3 goalOrigin) {
            int i, areaNum, curAreaNum;
            final int[] travelTime = {0};
            final idReachability[] reach = {null};
            idVec3 org;
			final idVec3 areaCenter;
            final aasPath_s path = new aasPath_s();

            if (NOT(this.file)) {
                return;
            }

            org = origin;
            areaNum = PointReachableAreaNum(org, DefaultSearchBounds(), AREA_REACHABLE_FLY);
            PushPointIntoAreaNum(areaNum, org);
            curAreaNum = areaNum;

            for (i = 0; i < 100; i++) {

                if (!RouteToGoalArea(curAreaNum, org, goalAreaNum, TFL_WALK | TFL_FLY | TFL_AIR, travelTime, reach)) {
                    break;
                }

                if (NOT(reach[0])) {
                    break;
                }

                gameRenderWorld.DebugArrow(colorPurple, org, reach[0].start, 2);
                DrawReachability(reach[0]);

                if (reach[0].toAreaNum == goalAreaNum) {
                    break;
                }

                curAreaNum = reach[0].toAreaNum;
                org = reach[0].end;
            }

            if (FlyPathToGoal(path, areaNum, origin, goalAreaNum, goalOrigin, TFL_WALK | TFL_FLY | TFL_AIR)) {
                gameRenderWorld.DebugArrow(colorBlue, origin, path.moveGoal, 2);
            }
        }

        @Override
        public boolean FindNearestGoal(aasGoal_s goal, int areaNum, final idVec3 origin, final idVec3 target, int travelFlags, aasObstacle_s[] obstacles, int numObstacles, idAASCallback callback) {
            int i, j, k, badTravelFlags, nextAreaNum, bestAreaNum;
            /*unsigned short*/ int t, bestTravelTime;
            idRoutingUpdate updateListStart, updateListEnd, curUpdate, nextUpdate;
            idReachability reach;
            aasArea_s nextArea;
            idVec3 v1, v2, p;
            float targetDist, dist;

            if ((this.file == null) || (areaNum <= 0)) {
                goal.areaNum = areaNum;
                goal.origin = origin;
                return false;
            }

            // if the first area is valid goal, just return the origin
            if (callback.TestArea(this, areaNum)) {
                goal.areaNum = areaNum;
                goal.origin = origin;
                return true;
            }

            // setup obstacles
            for (k = 0; k < numObstacles; k++) {
                obstacles[k].expAbsBounds.oSet(0, obstacles[k].absBounds.oGet(0).oMinus(this.file.GetSettings().boundingBoxes[0].oGet(1)));
                obstacles[k].expAbsBounds.oSet(1, obstacles[k].absBounds.oGet(1).oMinus(this.file.GetSettings().boundingBoxes[0].oGet(0)));
            }

            badTravelFlags = ~travelFlags;
            SIMDProcessor.Memset(this.goalAreaTravelTimes, 0, this.file.GetNumAreas() /*sizeof(unsigned short )*/);

            targetDist = (target.oMinus(origin)).Length();

            // initialize first update
            curUpdate = this.areaUpdate[areaNum];
            curUpdate.areaNum = areaNum;
            curUpdate.tmpTravelTime = 0;
            curUpdate.start = origin;
            curUpdate.next = null;
            curUpdate.prev = null;
            updateListStart = curUpdate;
            updateListEnd = curUpdate;

            bestTravelTime = 0;
            bestAreaNum = 0;

            // while there are updates in the list
            while (updateListStart != null) {

                curUpdate = updateListStart;
                if (curUpdate.next != null) {
                    curUpdate.next.prev = null;
                } else {
                    updateListEnd = null;
                }
                updateListStart = curUpdate.next;

                curUpdate.isInList = false;

                // if we already found a closer location
                if ((bestTravelTime != 0) && (curUpdate.tmpTravelTime >= bestTravelTime)) {
                    continue;
                }

                for (i = 0, reach = this.file.GetArea(curUpdate.areaNum).reach; reach != null; reach = reach.next, i++) {

                    // if the reachability uses an undesired travel type
                    if ((reach.travelType & badTravelFlags) != 0) {
                        continue;
                    }

                    // next area the reversed reachability leads to
                    nextAreaNum = reach.toAreaNum;
                    nextArea = this.file.GetArea(nextAreaNum);

                    // if traveling through the next area requires an undesired travel flag
                    if ((nextArea.travelFlags & badTravelFlags) != 0) {
                        continue;
                    }

                    t = curUpdate.tmpTravelTime
                            + AreaTravelTime(curUpdate.areaNum, curUpdate.start, reach.start)
                            + reach.travelTime;

                    // project target origin onto movement vector through the area
                    v1 = reach.end.oMinus(curUpdate.start);
                    v1.Normalize();
                    v2 = target.oMinus(curUpdate.start);
                    p = curUpdate.start.oPlus(v1.oMultiply(v2.oMultiply(v1)));

                    // get the point on the path closest to the target
                    for (j = 0; j < 3; j++) {
                        if (((p.oGet(j) > (curUpdate.start.oGet(j) + 0.1f)) && (p.oGet(j) > (reach.end.oGet(j) + 0.1f)))
                                || ((p.oGet(j) < (curUpdate.start.oGet(j) - 0.1f)) && (p.oGet(j) < (reach.end.oGet(j) - 0.1f)))) {
                            break;
                        }
                    }
                    if (j >= 3) {
                        dist = (target.oMinus(p)).Length();
                    } else {
                        dist = (target.oMinus(reach.end)).Length();
                    }

                    // avoid moving closer to the target
                    if (dist < targetDist) {
                        t += (targetDist - dist) * 10;
                    }

                    // if we already found a closer location
                    if ((bestTravelTime != 0) && (t >= bestTravelTime)) {
                        continue;
                    }

                    // if this is not the best path towards the next area
                    if ((this.goalAreaTravelTimes[nextAreaNum] != 0) && (t >= this.goalAreaTravelTimes[nextAreaNum])) {
                        continue;
                    }

                    // path may not go through any obstacles
                    for (k = 0; k < numObstacles; k++) {
                        // if the movement vector intersects the expanded obstacle bounds
                        if (obstacles[k].expAbsBounds.LineIntersection(curUpdate.start, reach.end)) {
                            break;
                        }
                    }
                    if (k < numObstacles) {
                        continue;
                    }

                    this.goalAreaTravelTimes[nextAreaNum] = t;
                    nextUpdate = this.areaUpdate[nextAreaNum];
                    nextUpdate.areaNum = nextAreaNum;
                    nextUpdate.tmpTravelTime = t;
                    nextUpdate.start = reach.end;

                    // if we are not allowed to fly
                    if ((badTravelFlags & TFL_FLY) != 0) {
                        // avoid areas near ledges
                        if ((this.file.GetArea(nextAreaNum).flags & AREA_LEDGE) != 0) {
                            nextUpdate.tmpTravelTime += LEDGE_TRAVELTIME_PANALTY;
                        }
                    }

                    if (!nextUpdate.isInList) {
                        nextUpdate.next = null;
                        nextUpdate.prev = updateListEnd;
                        if (updateListEnd != null) {
                            updateListEnd.next = nextUpdate;
                        } else {
                            updateListStart = nextUpdate;
                        }
                        updateListEnd = nextUpdate;
                        nextUpdate.isInList = true;
                    }

                    // don't put goal near a ledge
                    if (0 == (nextArea.flags & AREA_LEDGE)) {

                        // add travel time through the area
                        t += AreaTravelTime(reach.toAreaNum, reach.end, nextArea.center);

                        if ((0 == bestTravelTime) || (t < bestTravelTime)) {
                            // if the area is not visible to the target
                            if (callback.TestArea(this, reach.toAreaNum)) {
                                bestTravelTime = t;
                                bestAreaNum = reach.toAreaNum;
                            }
                        }
                    }
                }
            }

            if (bestAreaNum != 0) {
                goal.areaNum = bestAreaNum;
                goal.origin = AreaCenter(bestAreaNum);
                return true;
            }

            return false;
        }

        // routing
        private boolean SetupRouting() {
            CalculateAreaTravelTimes();
            SetupRoutingCache();
            return true;
        }

        private void ShutdownRouting() {
            DeleteAreaTravelTimes();
            ShutdownRoutingCache();
        }

        private /*unsigned short*/ int AreaTravelTime(int areaNum, final idVec3 start, final idVec3 end) {
            float dist;

            dist = (end.oMinus(start)).Length();

            if ((this.file.GetArea(areaNum).travelFlags & TFL_CROUCH) != 0) {
                dist *= 100.0f / 100.0f;
            } else if ((this.file.GetArea(areaNum).travelFlags & TFL_WATER) != 0) {
                dist *= 100.0f / 150.0f;
            } else {
                dist *= 100.0f / 300.0f;
            }
            if (dist < 1.0f) {
                return 1;
            }
            return idMath.FtoiFast(dist);
        }

        private void CalculateAreaTravelTimes() {
            int n, i, j, numReach, numRevReach, t, maxt;
            int bytePtr;
            idReachability reach, rev_reach;

            // get total memory for all area travel times
            this.numAreaTravelTimes = 0;
            for (n = 0; n < this.file.GetNumAreas(); n++) {

                if (NOT(this.file.GetArea(n).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY))) {
                    continue;
                }

                numReach = 0;
                for (reach = this.file.GetArea(n).reach; reach != null; reach = reach.next) {
                    numReach++;
                }

                numRevReach = 0;
                for (rev_reach = this.file.GetArea(n).rev_reach; rev_reach != null; rev_reach = rev_reach.rev_next) {
                    numRevReach++;
                }
                this.numAreaTravelTimes += numReach * numRevReach;
            }

            this.areaTravelTimes = new int[this.numAreaTravelTimes];// Mem_Alloc(numAreaTravelTimes /* sizeof(unsigned short )*/);
            bytePtr = 0;//(byte *) areaTravelTimes;

            for (n = 0; n < this.file.GetNumAreas(); n++) {

                if (NOT(this.file.GetArea(n).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY))) {
                    continue;
                }

                // for each reachability that starts in this area calculate the travel time
                // towards all the reachabilities that lead towards this area
                for (maxt = i = 0, reach = this.file.GetArea(n).reach; reach != null; reach = reach.next, i++) {
                    assert (i < MAX_REACH_PER_AREA);
                    if (i >= MAX_REACH_PER_AREA) {
                        idGameLocal.Error("i >= MAX_REACH_PER_AREA");
                    }
                    reach.number = (byte) i;
                    reach.disableCount = 0;
                    reach.areaTravelTimes = ((IntBuffer) IntBuffer.wrap(this.areaTravelTimes).position(bytePtr)).slice();
                    for (j = 0, rev_reach = this.file.GetArea(n).rev_reach; rev_reach != null; rev_reach = rev_reach.rev_next, j++) {
                        t = AreaTravelTime(n, reach.start, rev_reach.end);
                        reach.areaTravelTimes.put(j, t);
                        if (t > maxt) {
                            maxt = t;
                        }
                    }
                    bytePtr += j;// * sizeof( unsigned short );//TODO:double check the increment size.
                }

                // if this area is a portal
                if (this.file.GetArea(n).cluster < 0) {
                    // set the maximum travel time through this portal
                    this.file.SetPortalMaxTravelTime(-this.file.GetArea(n).cluster, maxt);
                }
            }

//	assert( ( (unsigned int) bytePtr - (unsigned int) areaTravelTimes ) <= numAreaTravelTimes * sizeof( unsigned short ) );
        }

        private void DeleteAreaTravelTimes() {
//            Mem_Free(areaTravelTimes);
            this.areaTravelTimes = null;
            this.numAreaTravelTimes = 0;
        }

        private void SetupRoutingCache() {
            int i;
            final int bytePtr;

            this.areaCacheIndexSize = 0;
            for (i = 0; i < this.file.GetNumClusters(); i++) {
                this.areaCacheIndexSize += this.file.GetCluster(i).numReachableAreas;
            }
            this.areaCacheIndex = new idRoutingCache[this.file.GetNumClusters()][this.areaCacheIndexSize];// Mem_ClearedAlloc(file.GetNumClusters() /* sizeof( idRoutingCache ** )*/ + areaCacheIndexSize /* sizeof( idRoutingCache *)*/);
//	bytePtr = ((byte *)areaCacheIndex) + file.GetNumClusters() * sizeof( idRoutingCache ** );
//            bytePtr = file.GetNumClusters();
//            for (i = 0; i < file.GetNumClusters(); i++) {
//                areaCacheIndex[i] = new idRoutingCache[bytePtr];
//                bytePtr += file.GetCluster(i).numReachableAreas /* sizeof( idRoutingCache * )*/;
//            }

            this.portalCacheIndexSize = this.file.GetNumAreas();
            this.portalCacheIndex = new idRoutingCache[this.portalCacheIndexSize];// Mem_ClearedAlloc(portalCacheIndexSize /* sizeof( idRoutingCache * )*/);

            this.areaUpdate = new idRoutingUpdate[this.file.GetNumAreas()];// Mem_ClearedAlloc(file.GetNumAreas() /* sizeof( idRoutingUpdate )*/);
            this.portalUpdate = new idRoutingUpdate[this.file.GetNumPortals() + 1];// Mem_ClearedAlloc((file.GetNumPortals() + 1) /* sizeof( idRoutingUpdate )*/);

            this.goalAreaTravelTimes = new int[this.file.GetNumAreas()];// Mem_ClearedAlloc(file.GetNumAreas() /* sizeof( unsigned short )*/);

            this.cacheListStart = this.cacheListEnd = null;
            this.totalCacheMemory = 0;
        }

        private void DeleteClusterCache(int clusterNum) {
            int i;
            idRoutingCache cache;

            for (i = 0; i < this.file.GetCluster(clusterNum).numReachableAreas; i++) {
                for (cache = this.areaCacheIndex[clusterNum][i]; cache != null; cache = this.areaCacheIndex[clusterNum][i]) {
                    this.areaCacheIndex[clusterNum][i] = cache.next;
                    UnlinkCache(cache);
//			delete cache;
                }
            }
        }

        private void DeletePortalCache() {
            int i;
            idRoutingCache cache;

            for (i = 0; i < this.file.GetNumAreas(); i++) {
                for (cache = this.portalCacheIndex[i]; cache != null; cache = this.portalCacheIndex[i]) {
                    this.portalCacheIndex[i] = cache.next;
                    UnlinkCache(cache);
//			delete cache;
                }
            }
        }

        private void ShutdownRoutingCache() {
            int i;

            for (i = 0; i < this.file.GetNumClusters(); i++) {
                DeleteClusterCache(i);
            }

            DeletePortalCache();

//            Mem_Free(areaCacheIndex);
            this.areaCacheIndex = null;
            this.areaCacheIndexSize = 0;
//            Mem_Free(portalCacheIndex);
            this.portalCacheIndex = null;
            this.portalCacheIndexSize = 0;
//            Mem_Free(areaUpdate);
            this.areaUpdate = null;
//            Mem_Free(portalUpdate);
            this.portalUpdate = null;
//            Mem_Free(goalAreaTravelTimes);
            this.goalAreaTravelTimes = null;

            this.cacheListStart = this.cacheListEnd = null;
            this.totalCacheMemory = 0;
        }

        private void RoutingStats() {
            idRoutingCache cache;
            int numAreaCache, numPortalCache;
            int totalAreaCacheMemory, totalPortalCacheMemory;

            numAreaCache = numPortalCache = 0;
            totalAreaCacheMemory = totalPortalCacheMemory = 0;
            for (cache = this.cacheListStart; cache != null; cache = cache.time_next) {
                if (cache.type == CACHETYPE_AREA) {
                    numAreaCache++;
//			totalAreaCacheMemory += sizeof( idRoutingCache ) + cache.size * (sizeof( unsigned short ) + sizeof( byte ));
                    totalAreaCacheMemory += cache.size;
                } else {
                    numPortalCache++;
//			totalPortalCacheMemory += sizeof( idRoutingCache ) + cache.size * (sizeof( unsigned short ) + sizeof( byte ));
                    totalPortalCacheMemory += cache.size;
                }
            }

            gameLocal.Printf("%6d area cache (%d KB)\n", numAreaCache, totalAreaCacheMemory >> 10);
            gameLocal.Printf("%6d portal cache (%d KB)\n", numPortalCache, totalPortalCacheMemory >> 10);
            gameLocal.Printf("%6d total cache (%d KB)\n", numAreaCache + numPortalCache, this.totalCacheMemory >> 10);
            gameLocal.Printf("%6d area travel times (%d KB)\n", this.numAreaTravelTimes, (this.numAreaTravelTimes /* sizeof( unsigned short )*/) >> 10);
            gameLocal.Printf("%6d area cache entries (%d KB)\n", this.areaCacheIndexSize, (this.areaCacheIndexSize /* sizeof( idRoutingCache * )*/) >> 10);
            gameLocal.Printf("%6d portal cache entries (%d KB)\n", this.portalCacheIndexSize, (this.portalCacheIndexSize /* sizeof( idRoutingCache * )*/) >> 10);
        }

        /*
         ============
         idAASLocal::LinkCache

         link the cache in the cache list sorted from oldest to newest cache
         ============
         */
        private void LinkCache(idRoutingCache cache) {

            // if the cache is already linked
            if ((cache.time_next != null) || (cache.time_prev != null) || (this.cacheListStart == cache)) {
                UnlinkCache(cache);
            }

            this.totalCacheMemory += cache.Size();

            // add cache to the end of the list
            cache.time_next = null;
            cache.time_prev = this.cacheListEnd;
            if (this.cacheListEnd != null) {
                this.cacheListEnd.time_next = cache;
            }
            this.cacheListEnd = cache;
            if (null == this.cacheListStart) {
                this.cacheListStart = cache;
            }
        }

        private void UnlinkCache(idRoutingCache cache) {

            this.totalCacheMemory -= cache.Size();

            // unlink the cache
            if (cache.time_next != null) {
                cache.time_next.time_prev = cache.time_prev;
            } else {
                this.cacheListEnd = cache.time_prev;
            }
            if (cache.time_prev != null) {
                cache.time_prev.time_next = cache.time_next;
            } else {
                this.cacheListStart = cache.time_next;
            }
            cache.time_next = cache.time_prev = null;
        }

        private void DeleteOldestCache() {
            idRoutingCache cache;

            assert (this.cacheListStart != null);

            // unlink the oldest cache
            cache = this.cacheListStart;
            UnlinkCache(cache);

            // unlink the oldest cache from the area or portal cache index
            if (cache.next != null) {
                cache.next.prev = cache.prev;
            }
            if (cache.prev != null) {
                cache.prev.next = cache.next;
            } else if (cache.type == CACHETYPE_AREA) {
                this.areaCacheIndex[cache.cluster][ClusterAreaNum(cache.cluster, cache.areaNum)] = cache.next;
            } else if (cache.type == CACHETYPE_PORTAL) {
                this.portalCacheIndex[cache.areaNum] = cache.next;
            }

//	delete cache;
        }

        private idReachability GetAreaReachability(int areaNum, int reachabilityNum) {
            idReachability reach;

            for (reach = this.file.GetArea(areaNum).reach; reach != null; reach = reach.next) {
                if (--reachabilityNum < 0) {
                    return reach;
                }
            }
            return null;
        }

        private int ClusterAreaNum(int clusterNum, int areaNum) {
            int side, areaCluster;

            areaCluster = this.file.GetArea(areaNum).cluster;
            if (areaCluster > 0) {
                return this.file.GetArea(areaNum).clusterAreaNum;
            } else {
                side = (this.file.GetPortal(-areaCluster).clusters[0] != clusterNum) ? 1 : 0;
                return this.file.GetPortal(-areaCluster).clusterAreaNum[side];
            }
        }

        private void UpdateAreaRoutingCache(idRoutingCache areaCache) {
            int i, nextAreaNum, cluster, badTravelFlags, clusterAreaNum, numReachableAreas;
            int t;
            final int[] startAreaTravelTimes = new int[MAX_REACH_PER_AREA];
            idRoutingUpdate updateListStart, updateListEnd, curUpdate, nextUpdate;
            idReachability reach;
            aasArea_s nextArea;

            // number of reachability areas within this cluster
            numReachableAreas = this.file.GetCluster(areaCache.cluster).numReachableAreas;

            // number of the start area within the cluster
            clusterAreaNum = ClusterAreaNum(areaCache.cluster, areaCache.areaNum);
            if (clusterAreaNum >= numReachableAreas) {
                return;
            }

            areaCache.travelTimes[clusterAreaNum] = areaCache.startTravelTime;
            badTravelFlags = ~areaCache.travelFlags;

            // initialize first update
            curUpdate = this.areaUpdate[clusterAreaNum] = new idRoutingUpdate();
            curUpdate.areaNum = areaCache.areaNum;
            curUpdate.areaTravelTimes = IntBuffer.wrap(startAreaTravelTimes);
            curUpdate.tmpTravelTime = areaCache.startTravelTime;
            curUpdate.next = null;
            curUpdate.prev = null;
            updateListStart = curUpdate;
            updateListEnd = curUpdate;

            // while there are updates in the list
            while (updateListStart != null) {

                curUpdate = updateListStart;
                if (curUpdate.next != null) {
                    curUpdate.next.prev = null;
                } else {
                    updateListEnd = null;
                }
                updateListStart = curUpdate.next;

                curUpdate.isInList = false;

                for (i = 0, reach = this.file.GetArea(curUpdate.areaNum).rev_reach; reach != null; reach = reach.rev_next, i++) {

                    // if the reachability uses an undesired travel type
                    if ((reach.travelType & badTravelFlags) != 0) {
                        continue;
                    }

                    // next area the reversed reachability leads to
                    nextAreaNum = reach.fromAreaNum;
                    nextArea = this.file.GetArea(nextAreaNum);

                    // if traveling through the next area requires an undesired travel flag
                    if ((nextArea.travelFlags & badTravelFlags) != 0) {
                        continue;
                    }

                    // get the cluster number of the area
                    cluster = nextArea.cluster;
                    // don't leave the cluster, however do flood into cluster portals
                    if ((cluster > 0) && (cluster != areaCache.cluster)) {
                        continue;
                    }

                    // get the number of the area in the cluster
                    clusterAreaNum = ClusterAreaNum(areaCache.cluster, nextAreaNum);
                    if (clusterAreaNum >= numReachableAreas) {
                        continue;	// should never happen
                    }

                    assert (clusterAreaNum < areaCache.size);

                    // time already travelled plus the traveltime through the current area
                    // plus the travel time of the reachability towards the next area
                    t = curUpdate.tmpTravelTime + curUpdate.areaTravelTimes.get(i) + reach.travelTime;

                    if ((0 == areaCache.travelTimes[clusterAreaNum]) || (t < areaCache.travelTimes[clusterAreaNum])) {

                        areaCache.travelTimes[clusterAreaNum] = t;
                        areaCache.reachabilities[clusterAreaNum] = reach.number; // reversed reachability used to get into this area
                        nextUpdate = this.areaUpdate[clusterAreaNum] = (this.areaUpdate[clusterAreaNum] == null ? new idRoutingUpdate() : this.areaUpdate[clusterAreaNum]);
                        nextUpdate.areaNum = nextAreaNum;
                        nextUpdate.tmpTravelTime = t;
                        nextUpdate.areaTravelTimes = reach.areaTravelTimes;

                        // if we are not allowed to fly
                        if ((badTravelFlags & TFL_FLY) != 0) {
                            // avoid areas near ledges
                            if ((this.file.GetArea(nextAreaNum).flags & AREA_LEDGE) != 0) {
                                nextUpdate.tmpTravelTime += LEDGE_TRAVELTIME_PANALTY;
                            }
                        }

                        if (!nextUpdate.isInList) {
                            nextUpdate.next = null;
                            nextUpdate.prev = updateListEnd;
                            if (updateListEnd != null) {
                                updateListEnd.next = nextUpdate;
                            } else {
                                updateListStart = nextUpdate;
                            }
                            updateListEnd = nextUpdate;
                            nextUpdate.isInList = true;
                        }
                    }
                }
            }
        }

        private idRoutingCache GetAreaRoutingCache(int clusterNum, int areaNum, int travelFlags) {
            int clusterAreaNum;
            idRoutingCache cache, clusterCache;

            // number of the area in the cluster
            clusterAreaNum = ClusterAreaNum(clusterNum, areaNum);
            // pointer to the cache for the area in the cluster
            clusterCache = this.areaCacheIndex[clusterNum][clusterAreaNum];
            // check if cache without undesired travel flags already exists
            for (cache = clusterCache; cache != null; cache = cache.next) {
                if (cache.travelFlags == travelFlags) {
                    break;
                }
            }
            // if no cache found
            if (null == cache) {
                cache = new idRoutingCache(this.file.GetCluster(clusterNum).numReachableAreas);
                cache.type = CACHETYPE_AREA;
                cache.cluster = clusterNum;
                cache.areaNum = areaNum;
                cache.startTravelTime = 1;
                cache.travelFlags = travelFlags;
                cache.prev = null;
                cache.next = clusterCache;
                if (clusterCache != null) {
                    clusterCache.prev = cache;
                }
                this.areaCacheIndex[clusterNum][clusterAreaNum] = cache;
                UpdateAreaRoutingCache(cache);
            }
            LinkCache(cache);
            return cache;
        }

        private void UpdatePortalRoutingCache(idRoutingCache portalCache) {
            int i, portalNum, clusterAreaNum;
            int t;
            aasPortal_s portal;
            aasCluster_s cluster;
            idRoutingCache cache;
            idRoutingUpdate updateListStart, updateListEnd, curUpdate, nextUpdate;

            curUpdate = this.portalUpdate[this.file.GetNumPortals()] = new idRoutingUpdate();
            curUpdate.cluster = portalCache.cluster;
            curUpdate.areaNum = portalCache.areaNum;
            curUpdate.tmpTravelTime = portalCache.startTravelTime;

            //put the area to start with in the current read list
            curUpdate.next = null;
            curUpdate.prev = null;
            updateListStart = curUpdate;
            updateListEnd = curUpdate;

            // while there are updates in the current list
            while (updateListStart != null) {

                curUpdate = updateListStart;
                // remove the current update from the list
                if (curUpdate.next != null) {
                    curUpdate.next.prev = null;
                } else {
                    updateListEnd = null;
                }
                updateListStart = curUpdate.next;
                // current update is removed from the list
                curUpdate.isInList = false;

                cluster = this.file.GetCluster(curUpdate.cluster);
                cache = GetAreaRoutingCache(curUpdate.cluster, curUpdate.areaNum, portalCache.travelFlags);

                // take all portals of the cluster
                for (i = 0; i < cluster.numPortals; i++) {
                    portalNum = this.file.GetPortalIndex(cluster.firstPortal + i);
                    assert (portalNum < portalCache.size);
                    portal = this.file.GetPortal(portalNum);

                    clusterAreaNum = ClusterAreaNum(curUpdate.cluster, portal.areaNum);
                    if (clusterAreaNum >= cluster.numReachableAreas) {
                        continue;
                    }

                    t = cache.travelTimes[clusterAreaNum];
                    if (t == 0) {
                        continue;
                    }
                    t += curUpdate.tmpTravelTime;

                    if ((0 == portalCache.travelTimes[portalNum]) || (t < portalCache.travelTimes[portalNum])) {

                        portalCache.travelTimes[portalNum] = t;
                        portalCache.reachabilities[portalNum] = cache.reachabilities[clusterAreaNum];
                        nextUpdate = this.portalUpdate[portalNum] = (this.portalUpdate[portalNum] == null ? new idRoutingUpdate() : this.portalUpdate[portalNum]);
                        if (portal.clusters[0] == curUpdate.cluster) {
                            nextUpdate.cluster = portal.clusters[1];
                        } else {
                            nextUpdate.cluster = portal.clusters[0];
                        }
                        nextUpdate.areaNum = portal.areaNum;
                        // add travel time through the actual portal area for the next update
                        nextUpdate.tmpTravelTime = t + portal.maxAreaTravelTime;

                        if (!nextUpdate.isInList) {

                            nextUpdate.next = null;
                            nextUpdate.prev = updateListEnd;
                            if (updateListEnd != null) {
                                updateListEnd.next = nextUpdate;
                            } else {
                                updateListStart = nextUpdate;
                            }
                            updateListEnd = nextUpdate;
                            nextUpdate.isInList = true;
                        }
                    }
                }
            }
        }

        private idRoutingCache GetPortalRoutingCache(int clusterNum, int areaNum, int travelFlags) {
            idRoutingCache cache;

            // check if cache without undesired travel flags already exists
            for (cache = this.portalCacheIndex[areaNum]; cache != null; cache = cache.next) {
                if (cache.travelFlags == travelFlags) {
                    break;
                }
            }
            // if no cache found
            if (null == cache) {
                cache = new idRoutingCache(this.file.GetNumPortals());
                cache.type = CACHETYPE_PORTAL;
                cache.cluster = clusterNum;
                cache.areaNum = areaNum;
                cache.startTravelTime = 1;
                cache.travelFlags = travelFlags;
                cache.prev = null;
                cache.next = this.portalCacheIndex[areaNum];
                if (this.portalCacheIndex[areaNum] != null) {
                    this.portalCacheIndex[areaNum].prev = cache;
                }
                this.portalCacheIndex[areaNum] = cache;
                UpdatePortalRoutingCache(cache);
            }
            LinkCache(cache);
            return cache;
        }

        private void RemoveRoutingCacheUsingArea(int areaNum) {
            int clusterNum;

            clusterNum = this.file.GetArea(areaNum).cluster;
            if (clusterNum > 0) {
                // remove all the cache in the cluster the area is in
                DeleteClusterCache(clusterNum);
            } else {
                // if this is a portal remove all cache in both the front and back cluster
                DeleteClusterCache(this.file.GetPortal(-clusterNum).clusters[0]);
                DeleteClusterCache(this.file.GetPortal(-clusterNum).clusters[1]);
            }
            DeletePortalCache();
        }

        private void DisableArea(int areaNum) {
            assert ((areaNum > 0) && (areaNum < this.file.GetNumAreas()));

            if ((this.file.GetArea(areaNum).travelFlags & TFL_INVALID) != 0) {
                return;
            }

            this.file.SetAreaTravelFlag(areaNum, TFL_INVALID);

            RemoveRoutingCacheUsingArea(areaNum);
        }

        private void EnableArea(int areaNum) {
            assert ((areaNum > 0) && (areaNum < this.file.GetNumAreas()));

            if (0 == (this.file.GetArea(areaNum).travelFlags & TFL_INVALID)) {
                return;
            }

            this.file.RemoveAreaTravelFlag(areaNum, TFL_INVALID);

            RemoveRoutingCacheUsingArea(areaNum);
        }

        private boolean SetAreaState_r(int nodeNum, final idBounds bounds, final int areaContents, boolean disabled) {
            int res;
            aasNode_s node;
            boolean foundClusterPortal = false;

            while (nodeNum != 0) {
                if (nodeNum < 0) {
                    // if this area is a cluster portal
                    if ((this.file.GetArea(-nodeNum).contents & areaContents) != 0) {
                        if (disabled) {
                            DisableArea(-nodeNum);
                        } else {
                            EnableArea(-nodeNum);
                        }
                        foundClusterPortal |= true;
                    }
                    break;
                }
                node = this.file.GetNode(nodeNum);
                res = bounds.PlaneSide(this.file.GetPlane(node.planeNum));
                if (res == PLANESIDE_BACK) {
                    nodeNum = node.children[1];
                } else if (res == PLANESIDE_FRONT) {
                    nodeNum = node.children[0];
                } else {
                    foundClusterPortal |= SetAreaState_r(node.children[1], bounds, areaContents, disabled);
                    nodeNum = node.children[0];
                }
            }

            return foundClusterPortal;
        }

        private void GetBoundsAreas_r(int nodeNum, final idBounds bounds, idList<Integer> areas) {
            int res;
            aasNode_s node;

            while (nodeNum != 0) {
                if (nodeNum < 0) {
                    areas.Append(-nodeNum);
                    break;
                }
                node = this.file.GetNode(nodeNum);
                res = bounds.PlaneSide(this.file.GetPlane(node.planeNum));
                if (res == PLANESIDE_BACK) {
                    nodeNum = node.children[1];
                } else if (res == PLANESIDE_FRONT) {
                    nodeNum = node.children[0];
                } else {
                    GetBoundsAreas_r(node.children[1], bounds, areas);
                    nodeNum = node.children[0];
                }
            }
        }

        private void SetObstacleState(final idRoutingObstacle obstacle, boolean enable) {
            int i;
            aasArea_s area;
            idReachability reach, rev_reach;
            boolean inside;

            for (i = 0; i < obstacle.areas.Num(); i++) {

                RemoveRoutingCacheUsingArea(obstacle.areas.oGet(i));

                area = this.file.GetArea(obstacle.areas.oGet(i));

                for (rev_reach = area.rev_reach; rev_reach != null; rev_reach = rev_reach.rev_next) {

                    if ((rev_reach.travelType & TFL_INVALID) != 0) {
                        continue;
                    }

                    inside = false;

                    if (obstacle.bounds.ContainsPoint(rev_reach.end)) {
                        inside = true;
                    } else {
                        for (reach = area.reach; reach != null; reach = reach.next) {
                            if (obstacle.bounds.LineIntersection(rev_reach.end, reach.start)) {
                                inside = true;
                                break;
                            }
                        }
                    }

                    if (inside) {
                        if (enable) {
                            rev_reach.disableCount--;
                            if (rev_reach.disableCount <= 0) {
                                rev_reach.travelType &= ~TFL_INVALID;
                                rev_reach.disableCount = 0;
                            }
                        } else {
                            rev_reach.travelType |= TFL_INVALID;
                            rev_reach.disableCount++;
                        }
                    }
                }
            }
        }

        // pathing
        /*
         ============
         idAASLocal::EdgeSplitPoint

         calculates split point of the edge with the plane
         returns true if the split point is between the edge vertices
         ============
         */
        private boolean EdgeSplitPoint(idVec3 split, int edgeNum, final idPlane plane) {
            aasEdge_s edge;
            idVec3 v1, v2;
            float d1, d2;

            edge = this.file.GetEdge(edgeNum);
            v1 = this.file.GetVertex(edge.vertexNum[0]);
            v2 = this.file.GetVertex(edge.vertexNum[1]);
            d1 = v1.oMultiply(plane.Normal()) - plane.Dist();
            d2 = v2.oMultiply(plane.Normal()) - plane.Dist();

            //if ( (d1 < CM_CLIP_EPSILON && d2 < CM_CLIP_EPSILON) || (d1 > -CM_CLIP_EPSILON && d2 > -CM_CLIP_EPSILON) ) {
            if (FLOATSIGNBITSET(d1) == FLOATSIGNBITSET(d2)) {
                return false;
            }
            split.oSet(v1.oPlus((v2.oMinus(v1).oMultiply(d1 / (d1 - d2)))));
            return true;
        }

        /*
         ============
         idAASLocal::FloorEdgeSplitPoint

         calculates either the closest or furthest point on the floor of the area which also lies on the pathPlane
         the point has to be on the front side of the frontPlane to be valid
         ============
         */
        private boolean FloorEdgeSplitPoint(idVec3 bestSplit, int areaNum, final idPlane pathPlane, final idPlane frontPlane, boolean closest) {
            int i, j, faceNum, edgeNum;
            aasArea_s area;
            aasFace_s face;
            final idVec3 split = new idVec3();
            float dist, bestDist;

            if (closest) {
                bestDist = maxWalkPathDistance;
            } else {
                bestDist = -0.1f;
            }

            area = this.file.GetArea(areaNum);

            for (i = 0; i < area.numFaces; i++) {
                faceNum = this.file.GetFaceIndex(area.firstFace + i);
                face = this.file.GetFace(abs(faceNum));

                if (0 == (face.flags & FACE_FLOOR)) {
                    continue;
                }

                for (j = 0; j < face.numEdges; j++) {
                    edgeNum = this.file.GetEdgeIndex(face.firstEdge + j);

                    if (!EdgeSplitPoint(split, abs(edgeNum), pathPlane)) {
                        continue;
                    }
                    dist = frontPlane.Distance(split);
                    if (closest) {
                        if ((dist >= -0.1f) && (dist < bestDist)) {
                            bestDist = dist;
                            bestSplit.oSet(split);
                        }
                    } else {
                        if (dist > bestDist) {
                            bestDist = dist;
                            bestSplit.oSet(split);
                        }
                    }
                }
            }

            if (closest) {
                return (bestDist < maxWalkPathDistance);
            } else {
                return (bestDist > -0.1f);
            }
        }

        private idVec3 SubSampleWalkPath(int areaNum, final idVec3 origin, final idVec3 start, final idVec3 end, int travelFlags, int[] endAreaNum) {
            int i, numSamples;
            final int[] curAreaNum = {0};
            idVec3 dir, point, nextPoint;
			final idVec3 endPos = new idVec3();

            dir = end.oMinus(start);
            numSamples = (int) (dir.Length() / walkPathSampleDistance) + 1;

            point = start;
            for (i = 1; i < numSamples; i++) {
                nextPoint = start.oPlus(dir.oMultiply((float) i / numSamples));
                if ((point.oMinus(nextPoint)).LengthSqr() > Square(maxWalkPathDistance)) {
                    return point;
                }
                if (!this.WalkPathValid(areaNum, origin, 0, nextPoint, travelFlags, endPos, curAreaNum)) {
                    return point;
                }
                point = nextPoint;
                endAreaNum[0] = curAreaNum[0];
            }
            return point;
        }

        private idVec3 SubSampleFlyPath(int areaNum, final idVec3 origin, final idVec3 start, final idVec3 end, int travelFlags, int[] endAreaNum) {
            int i, numSamples;
            final int[] curAreaNum = {0};
            idVec3 dir, point, nextPoint;
			final idVec3 endPos = new idVec3();

            dir = end.oMinus(start);
            numSamples = (int) (dir.Length() / flyPathSampleDistance) + 1;

            point = start;
            for (i = 1; i < numSamples; i++) {
                nextPoint = start.oPlus(dir.oMultiply((float) i / numSamples));
                if ((point.oMinus(nextPoint)).LengthSqr() > Square(maxFlyPathDistance)) {
                    return point;
                }
                if (!this.FlyPathValid(areaNum, origin, 0, nextPoint, travelFlags, endPos, curAreaNum)) {
                    return point;
                }
                point = nextPoint;
                endAreaNum[0] = curAreaNum[0];
            }
            return point;
        }

        // debug
        private idBounds DefaultSearchBounds() {
            return this.file.GetSettings().boundingBoxes[0];
        }

        private void DrawCone(final idVec3 origin, final idVec3 dir, float radius, final idVec4 color) {
            int i;
            final idMat3 axis = new idMat3();
            idVec3 center, top, p, lastp;

            axis.oSet(2, dir);
            axis.oGet(2).NormalVectors(axis.oGet(0), axis.oGet(1));
            axis.oSet(1, axis.oGet(1).oNegative());

            center = origin.oPlus(dir);
            top = center.oPlus(dir.oMultiply(3.0f * radius));
            lastp = center.oPlus(axis.oGet(1).oMultiply(radius));

            for (i = 20; i <= 360; i += 20) {
                p = center.oPlus(axis.oGet(0).oMultiply((float) (sin(DEG2RAD(i)) * radius)).oPlus(axis.oGet(1).oMultiply((float) (cos(DEG2RAD(i)) * radius))));
                gameRenderWorld.DebugLine(color, lastp, p, 0);
                gameRenderWorld.DebugLine(color, p, top, 0);
                lastp = p;
            }
        }

        private void DrawArea(int areaNum) {
            int i, numFaces, firstFace;
            aasArea_s area;
            idReachability reach;

            if (NOT(this.file)) {
                return;
            }

            area = this.file.GetArea(areaNum);
            numFaces = area.numFaces;
            firstFace = area.firstFace;

            for (i = 0; i < numFaces; i++) {
                DrawFace(abs(this.file.GetFaceIndex(firstFace + i)), this.file.GetFaceIndex(firstFace + i) < 0);
            }

            for (reach = area.reach; reach != null; reach = reach.next) {
                DrawReachability(reach);
            }
        }

        private void DrawFace(int faceNum, boolean side) {
            int i, j, numEdges, firstEdge;
            aasFace_s face;
            idVec3 mid, end;

            if (NOT(this.file)) {
                return;
            }

            face = this.file.GetFace(faceNum);
            numEdges = face.numEdges;
            firstEdge = face.firstEdge;

            mid = getVec3_origin();
            for (i = 0; i < numEdges; i++) {
                DrawEdge(abs(this.file.GetEdgeIndex(firstEdge + i)), (face.flags & FACE_FLOOR) != 0);
                j = this.file.GetEdgeIndex(firstEdge + i);
                mid.oPluSet(this.file.GetVertex(this.file.GetEdge(abs(j)).vertexNum[(j < 0) ? 1 : 0]));
            }

            mid.oDivSet(numEdges);
            if (side) {
                end = mid.oMinus(this.file.GetPlane(this.file.GetFace(faceNum).planeNum).Normal().oMultiply(5.0f));
            } else {
                end = mid.oPlus(this.file.GetPlane(this.file.GetFace(faceNum).planeNum).Normal().oMultiply(5.0f));
            }
            gameRenderWorld.DebugArrow(colorGreen, mid, end, 1);
        }

        private void DrawEdge(int edgeNum, boolean arrow) {
            aasEdge_s edge;
            idVec4 color;

            if (NOT(this.file)) {
                return;
            }

            edge = this.file.GetEdge(edgeNum);
            color = colorRed;
            if (arrow) {
                gameRenderWorld.DebugArrow(color, this.file.GetVertex(edge.vertexNum[0]), this.file.GetVertex(edge.vertexNum[1]), 1);
            } else {
                gameRenderWorld.DebugLine(color, this.file.GetVertex(edge.vertexNum[0]), this.file.GetVertex(edge.vertexNum[1]));
            }

            if (gameLocal.GetLocalPlayer() != null) {
                gameRenderWorld.DrawText(va("%d", edgeNum), (this.file.GetVertex(edge.vertexNum[0]).oPlus(this.file.GetVertex(edge.vertexNum[1]))).
                        oMultiply(0.5f).oPlus(new idVec3(0, 0, 4)), 0.1f, colorRed, gameLocal.GetLocalPlayer().viewAxis);
            }
        }

        private void DrawReachability(final idReachability reach) {
            gameRenderWorld.DebugArrow(colorCyan, reach.start, reach.end, 2);

            if (gameLocal.GetLocalPlayer() != null) {
                gameRenderWorld.DrawText(va("%d", reach.edgeNum), (reach.start.oPlus(reach.end)).oMultiply(0.5f), 0.1f, colorWhite, gameLocal.GetLocalPlayer().viewAxis);
            }

            if (reach.travelType == TFL_WALK) {
                final idReachability_Walk walk = (idReachability_Walk) reach;
            }
        }
        private static int lastAreaNum;

        private void ShowArea(final idVec3 origin) {
            int areaNum;
            aasArea_s area;
            idVec3 org;

            areaNum = PointReachableAreaNum(origin, DefaultSearchBounds(), (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY));
            org = origin;
            PushPointIntoAreaNum(areaNum, org);

            if (aas_goalArea.GetInteger() != 0) {
                final int[] travelTime = {0};
                final idReachability[] reach = {null};

                RouteToGoalArea(areaNum, org, aas_goalArea.GetInteger(), TFL_WALK | TFL_AIR, travelTime, reach);
                gameLocal.Printf("\rtt = %4d", travelTime[0]);
                if (reach[0] != null) {
                    gameLocal.Printf(" to area %4d", reach[0].toAreaNum);
                    DrawArea(reach[0].toAreaNum);
                }
            }

            if (areaNum != lastAreaNum) {
                area = this.file.GetArea(areaNum);
                gameLocal.Printf("area %d: ", areaNum);
                if ((area.flags & AREA_LEDGE) != 0) {
                    gameLocal.Printf("AREA_LEDGE ");
                }
                if ((area.flags & AREA_REACHABLE_WALK) != 0) {
                    gameLocal.Printf("AREA_REACHABLE_WALK ");
                }
                if ((area.flags & AREA_REACHABLE_FLY) != 0) {
                    gameLocal.Printf("AREA_REACHABLE_FLY ");
                }
                if ((area.contents & AREACONTENTS_CLUSTERPORTAL) != 0) {
                    gameLocal.Printf("AREACONTENTS_CLUSTERPORTAL ");
                }
                if ((area.contents & AREACONTENTS_OBSTACLE) != 0) {
                    gameLocal.Printf("AREACONTENTS_OBSTACLE ");
                }
                gameLocal.Printf("\n");
                lastAreaNum = areaNum;
            }

            if (!org.equals(origin)) {
                final idBounds bnds = this.file.GetSettings().boundingBoxes[ 0];
                bnds.oGet(1).z = bnds.oGet(0).z;
                gameRenderWorld.DebugBounds(colorYellow, bnds, org);
            }

            DrawArea(areaNum);
        }

        private void ShowWallEdges(final idVec3 origin) {
            int i, areaNum, numEdges;
            final int[] edges = new int[1024];
            final idVec3 start = new idVec3(), end = new idVec3();
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (null == player) {
                return;
            }

            areaNum = PointReachableAreaNum(origin, DefaultSearchBounds(), (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY));
            numEdges = GetWallEdges(areaNum, new idBounds(origin).Expand(256.0f), TFL_WALK, edges, 1024);
            for (i = 0; i < numEdges; i++) {
                GetEdge(edges[i], start, end);
                gameRenderWorld.DebugLine(colorRed, start, end);
                gameRenderWorld.DrawText(va("%d", edges[i]), (start.oPlus(end)).oMultiply(0.5f), 0.1f, colorWhite, player.viewAxis);
            }
        }

        private void ShowHideArea(final idVec3 origin, int targetAreaNum) {
            int areaNum, numObstacles;
            idVec3 target;
            final aasGoal_s goal = new aasGoal_s();
            final aasObstacle_s[] obstacles = new aasObstacle_s[10];

            areaNum = PointReachableAreaNum(origin, DefaultSearchBounds(), (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY));
            target = AreaCenter(targetAreaNum);

            // consider the target an obstacle
            obstacles[0].absBounds = new idBounds(target).Expand(16);
            numObstacles = 1;

            DrawCone(target, new idVec3(0, 0, 1), 16.0f, colorYellow);

            final idAASFindCover findCover = new idAASFindCover(target);
            if (FindNearestGoal(goal, areaNum, origin, target, TFL_WALK | TFL_AIR, obstacles, numObstacles, findCover)) {
                DrawArea(goal.areaNum);
                ShowWalkPath(origin, goal.areaNum, goal.origin);
                DrawCone(goal.origin, new idVec3(0, 0, 1), 16.0f, colorWhite);
            }
        }

        private boolean PullPlayer(final idVec3 origin, int toAreaNum) {
            int areaNum;
            idVec3 areaCenter, dir, vel;
            idAngles delta;
            final aasPath_s path = new aasPath_s();
            idPlayer player;

            player = gameLocal.GetLocalPlayer();
            if (null == player) {
                return true;
            }

            final idPhysics physics = player.GetPhysics();
            if (null == physics) {
                return true;
            }

            if (0 == toAreaNum) {
                return false;
            }

            areaNum = PointReachableAreaNum(origin, DefaultSearchBounds(), (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY));
            areaCenter = AreaCenter(toAreaNum);
            if (player.GetPhysics().GetAbsBounds().Expand(8).ContainsPoint(areaCenter)) {
                return false;
            }
            if (WalkPathToGoal(path, areaNum, origin, toAreaNum, areaCenter, TFL_WALK | TFL_AIR)) {
                dir = path.moveGoal.oMinus(origin);
                dir.oMulSet(2, 0.5f);
                dir.Normalize();
                delta = dir.ToAngles().oMinus(player.cmdAngles.oMinus(player.GetDeltaViewAngles()));
                delta.Normalize180();
                player.SetDeltaViewAngles(player.GetDeltaViewAngles().oPlus(delta.oMultiply(0.1f)));
                dir.oSet(2, 0.0f);
                dir.Normalize();
                dir.oMulSet(100.0f);
                vel = physics.GetLinearVelocity();
                dir.oSet(2, vel.oGet(2));
                physics.SetLinearVelocity(dir);
                return true;
            } else {
                return false;
            }
        }

        private void RandomPullPlayer(final idVec3 origin) {
            int rnd, i, n;

            if (!PullPlayer(origin, aas_pullPlayer.GetInteger())) {

                rnd = (int) (gameLocal.random.RandomFloat() * this.file.GetNumAreas());

                for (i = 0; i < this.file.GetNumAreas(); i++) {
                    n = (rnd + i) % this.file.GetNumAreas();
                    if ((this.file.GetArea(n).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY)) != 0) {
                        aas_pullPlayer.SetInteger(n);
                    }
                }
            } else {
                ShowWalkPath(origin, aas_pullPlayer.GetInteger(), AreaCenter(aas_pullPlayer.GetInteger()));
            }
        }

        private void ShowPushIntoArea(final idVec3 origin) {
            int areaNum;
            idVec3 target;

            target = origin;
            areaNum = PointReachableAreaNum(target, DefaultSearchBounds(), (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY));
            PushPointIntoAreaNum(areaNum, target);
            gameRenderWorld.DebugArrow(colorGreen, origin, target, 1);
        }

    }
}
