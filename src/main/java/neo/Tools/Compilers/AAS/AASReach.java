package neo.Tools.Compilers.AAS;

import static java.lang.Math.abs;
import static neo.TempDump.SNOT;
import static neo.TempDump.btoi;
import static neo.TempDump.itob;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_CLUSTERPORTAL;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_WATER;
import static neo.Tools.Compilers.AAS.AASFile.AREA_CROUCH;
import static neo.Tools.Compilers.AAS.AASFile.AREA_FLOOR;
import static neo.Tools.Compilers.AAS.AASFile.AREA_LADDER;
import static neo.Tools.Compilers.AAS.AASFile.AREA_LIQUID;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_FLY;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_WALK;
import static neo.Tools.Compilers.AAS.AASFile.FACE_FLOOR;
import static neo.Tools.Compilers.AAS.AASFile.TFL_BARRIERJUMP;
import static neo.Tools.Compilers.AAS.AASFile.TFL_FLY;
import static neo.Tools.Compilers.AAS.AASFile.TFL_SWIM;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WALK;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WALKOFFLEDGE;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WATERJUMP;
import static neo.framework.Common.common;
import static neo.idlib.math.Math_h.INTSIGNBITNOTSET;
import static neo.idlib.math.Math_h.INTSIGNBITSET;

import neo.Tools.Compilers.AAS.AASFile.aasArea_s;
import neo.Tools.Compilers.AAS.AASFile.aasEdge_s;
import neo.Tools.Compilers.AAS.AASFile.aasFace_s;
import neo.Tools.Compilers.AAS.AASFile.aasTrace_s;
import neo.Tools.Compilers.AAS.AASFile.idReachability;
import neo.Tools.Compilers.AAS.AASFile.idReachability_BarrierJump;
import neo.Tools.Compilers.AAS.AASFile.idReachability_Fly;
import neo.Tools.Compilers.AAS.AASFile.idReachability_Swim;
import neo.Tools.Compilers.AAS.AASFile.idReachability_Walk;
import neo.Tools.Compilers.AAS.AASFile.idReachability_WalkOffLedge;
import neo.Tools.Compilers.AAS.AASFile.idReachability_WaterJump;
import neo.Tools.Compilers.AAS.AASFile_local.idAASFileLocal;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class AASReach {

    static final float INSIDEUNITS           = 2.0f;
    static final float INSIDEUNITS_WALKEND   = 0.5f;
    static final float INSIDEUNITS_WALKSTART = 0.1f;
    static final float INSIDEUNITS_SWIMEND   = 0.5f;
    static final float INSIDEUNITS_FLYEND    = 0.5f;
    static final float INSIDEUNITS_WATERJUMP = 15.0f;

    /*
     ===============================================================================

     Reachabilities

     ===============================================================================
     */
    static class idAASReach {

        private idMapFile      mapFile;
        private idAASFileLocal file;
        private int            numReachabilities;
        private boolean        allowSwimReachabilities;
        private boolean        allowFlyReachabilities;
//
//

        public boolean Build(final idMapFile mapFile, idAASFileLocal file) {
            int i, j, lastPercent, percent;

            this.mapFile = mapFile;
            this.file = file;
            this.numReachabilities = 0;

            common.Printf("[Reachability]\n");

            // delete all existing reachabilities
            file.DeleteReachabilities();

            FlagReachableAreas(file);

            for (i = 1; i < file.areas.Num(); i++) {
                if (0 == (file.areas.oGet(i).flags & AREA_REACHABLE_WALK)) {
                    continue;
                }
                if (file.GetSettings().allowSwimReachabilities[0]) {
                    Reachability_Swim(i);
                }
                Reachability_EqualFloorHeight(i);
            }

            lastPercent = -1;
            for (i = 1; i < file.areas.Num(); i++) {

                if (0 == (file.areas.oGet(i).flags & AREA_REACHABLE_WALK)) {
                    continue;
                }

                for (j = 0; j < file.areas.Num(); j++) {
                    if (i == j) {
                        continue;
                    }

                    if (0 == (file.areas.oGet(j).flags & AREA_REACHABLE_WALK)) {
                        continue;
                    }

                    if (ReachabilityExists(i, j)) {
                        continue;
                    }

                    Reachability_Step_Barrier_WaterJump_WalkOffLedge(i, j);
//                    if (Reachability_Step_Barrier_WaterJump_WalkOffLedge(i, j)) {
//                        continue;
//                    }
                }

                //Reachability_WalkOffLedge( i );
                percent = (100 * i) / file.areas.Num();
                if (percent > lastPercent) {
                    common.Printf("\r%6d%%", percent);
                    lastPercent = percent;
                }
            }

            if (file.GetSettings().allowFlyReachabilities[0]) {
                for (i = 1; i < file.areas.Num(); i++) {
                    Reachability_Fly(i);
                }
            }

            file.LinkReversedReachability();

            common.Printf("\r%6d reachabilities\n", this.numReachabilities);

            return true;
        }

        // reachability
        private void FlagReachableAreas(idAASFileLocal file) {
            int i, numReachableAreas;

            numReachableAreas = 0;
            for (i = 1; i < file.areas.Num(); i++) {

                if (((file.areas.oGet(i).flags & (AREA_FLOOR | AREA_LADDER)) != 0)
                        || ((file.areas.oGet(i).contents & AREACONTENTS_WATER) != 0)) {
                    file.areas.oGet(i).flags |= AREA_REACHABLE_WALK;
                }
                if (file.GetSettings().allowFlyReachabilities[0]) {
                    file.areas.oGet(i).flags |= AREA_REACHABLE_FLY;
                }
                numReachableAreas++;
            }

            common.Printf("%6d reachable areas\n", numReachableAreas);
        }

        private boolean ReachabilityExists(int fromAreaNum, int toAreaNum) {
            aasArea_s area;
            idReachability reach;

            area = this.file.areas.oGet(fromAreaNum);
            for (reach = area.reach; reach != null; reach = reach.next) {
                if (reach.toAreaNum == toAreaNum) {
                    return true;
                }
            }
            return false;
        }

        private boolean CanSwimInArea(int areaNum) {
            return (this.file.areas.oGet(areaNum).contents & AREACONTENTS_WATER) != 0;
        }

        private boolean AreaHasFloor(int areaNum) {
            return (this.file.areas.oGet(areaNum).flags & AREA_FLOOR) != 0;
        }

        private boolean AreaIsClusterPortal(int areaNum) {
            return (this.file.areas.oGet(areaNum).flags & AREACONTENTS_CLUSTERPORTAL) != 0;
        }

        private void AddReachabilityToArea(idReachability reach, int areaNum) {
            aasArea_s area;

            area = this.file.areas.oGet(areaNum);
            reach.next = area.reach;
            area.reach = reach;
            this.numReachabilities++;
        }

        private void Reachability_Fly(int areaNum) {
            int i, faceNum, otherAreaNum;
            aasArea_s area;
            aasFace_s face;
            idReachability_Fly reach;

            area = this.file.areas.oGet(areaNum);

            for (i = 0; i < area.numFaces; i++) {
                faceNum = this.file.faceIndex.oGet(area.firstFace + i);
                face = this.file.faces.oGet(abs(faceNum));

                otherAreaNum = face.areas[INTSIGNBITNOTSET(faceNum)];

                if (otherAreaNum == 0) {
                    continue;
                }

                if (ReachabilityExists(areaNum, otherAreaNum)) {
                    continue;
                }

                // create reachability going through this face
                reach = new idReachability_Fly();
                reach.travelType = TFL_FLY;
                reach.toAreaNum = (short) otherAreaNum;
                reach.fromAreaNum = (short) areaNum;
                reach.edgeNum = 0;
                reach.travelTime = 1;
                reach.start = this.file.FaceCenter(abs(faceNum));
                if (faceNum < 0) {
                    reach.end = reach.start.oPlus(this.file.planeList.oGet(face.planeNum).Normal().oMultiply(INSIDEUNITS_FLYEND));
                } else {
                    reach.end = reach.start.oMinus(this.file.planeList.oGet(face.planeNum).Normal().oMultiply(INSIDEUNITS_FLYEND));
                }
                AddReachabilityToArea(reach, areaNum);
            }
        }

        private void Reachability_Swim(int areaNum) {
            int i, faceNum, otherAreaNum;
            aasArea_s area;
            aasFace_s face;
            idReachability_Swim reach;

            if (!CanSwimInArea(areaNum)) {
                return;
            }

            area = this.file.areas.oGet(areaNum);

            for (i = 0; i < area.numFaces; i++) {
                faceNum = this.file.faceIndex.oGet(area.firstFace + i);
                face = this.file.faces.oGet(abs(faceNum));

                otherAreaNum = face.areas[INTSIGNBITNOTSET(faceNum)];

                if (otherAreaNum == 0) {
                    continue;
                }

                if (!CanSwimInArea(otherAreaNum)) {
                    continue;
                }

                if (ReachabilityExists(areaNum, otherAreaNum)) {
                    continue;
                }

                // create reachability going through this face
                reach = new idReachability_Swim();
                reach.travelType = TFL_SWIM;
                reach.toAreaNum = (short) otherAreaNum;
                reach.fromAreaNum = (short) areaNum;
                reach.edgeNum = 0;
                reach.travelTime = 1;
                reach.start = this.file.FaceCenter(abs(faceNum));
                if (faceNum < 0) {
                    reach.end = reach.start.oPlus(this.file.planeList.oGet(face.planeNum).Normal().oMultiply(INSIDEUNITS_SWIMEND));
                } else {
                    reach.end = reach.start.oMinus(this.file.planeList.oGet(face.planeNum).Normal().oMultiply(INSIDEUNITS_SWIMEND));
                }
                AddReachabilityToArea(reach, areaNum);
            }
        }

        private void Reachability_EqualFloorHeight(int areaNum) {
            int i, k, l, m, n, faceNum, face1Num, face2Num, otherAreaNum, edge1Num = 0, edge2Num;
            aasArea_s area, otherArea;
            aasFace_s face, face1, face2;
            idReachability_Walk reach;

            if (!AreaHasFloor(areaNum)) {
                return;
            }

            area = this.file.areas.oGet(areaNum);

            for (i = 0; i < area.numFaces; i++) {
                faceNum = this.file.faceIndex.oGet(area.firstFace + i);
                face = this.file.faces.oGet(abs(faceNum));

                otherAreaNum = face.areas[INTSIGNBITNOTSET(faceNum)];
                if (!AreaHasFloor(otherAreaNum)) {
                    continue;
                }

                otherArea = this.file.areas.oGet(otherAreaNum);

                for (k = 0; k < area.numFaces; k++) {
                    face1Num = this.file.faceIndex.oGet(area.firstFace + k);
                    face1 = this.file.faces.oGet(abs(face1Num));

                    if (0 == (face1.flags & FACE_FLOOR)) {
                        continue;
                    }
                    for (l = 0; l < otherArea.numFaces; l++) {
                        face2Num = this.file.faceIndex.oGet(otherArea.firstFace + l);
                        face2 = this.file.faces.oGet(abs(face2Num));

                        if (0 == (face2.flags & FACE_FLOOR)) {
                            continue;
                        }

                        for (m = 0; m < face1.numEdges; m++) {
                            edge1Num = abs(this.file.edgeIndex.oGet(face1.firstEdge + m));
                            for (n = 0; n < face2.numEdges; n++) {
                                edge2Num = abs(this.file.edgeIndex.oGet(face2.firstEdge + n));
                                if (edge1Num == edge2Num) {
                                    break;
                                }
                            }
                            if (n < face2.numEdges) {
                                break;
                            }
                        }
                        if (m < face1.numEdges) {
                            break;
                        }
                    }
                    if (l < otherArea.numFaces) {
                        break;
                    }
                }
                if (k < area.numFaces) {
                    // create reachability
                    reach = new idReachability_Walk();
                    reach.travelType = TFL_WALK;
                    reach.toAreaNum = (short) otherAreaNum;
                    reach.fromAreaNum = (short) areaNum;
                    reach.edgeNum = abs(edge1Num);
                    reach.travelTime = 1;
                    reach.start = this.file.EdgeCenter(edge1Num);
                    if (faceNum < 0) {
                        reach.end = reach.start.oPlus(this.file.planeList.oGet(face.planeNum).Normal().oMultiply(INSIDEUNITS_WALKEND));
                    } else {
                        reach.end = reach.start.oMinus(this.file.planeList.oGet(face.planeNum).Normal().oMultiply(INSIDEUNITS_SWIMEND));
                    }
                    AddReachabilityToArea(reach, areaNum);
                }
            }
        }

        private boolean Reachability_Step_Barrier_WaterJump_WalkOffLedge(int fromAreaNum, int toAreaNum) {
            int i, j, k, l, edge1Num, edge2Num;
            final int[] areas = new int[10];
            int floor_bestArea1FloorEdgeNum = 0, floor_bestArea2FloorEdgeNum, floor_foundReach;
            int water_bestArea1FloorEdgeNum, water_bestArea2FloorEdgeNum, water_foundReach;
            int side1, floorFace1Num;
            boolean faceSide1;
            float dist, dist1, dist2, diff, invGravityDot, orthogonalDot;
            float x1, x2, x3, x4, y1, y2, y3, y4, tmp, y;
            float length, floor_bestLength, water_bestLength, floor_bestDist, water_bestDist;
            idVec3 v1, v2, v3, v4, tmpv, p1area1, p1area2, p2area1, p2area2;
            idVec3 normal, orthogonal, edgeVec, start, end;
            idVec3 floor_bestStart = new idVec3(), floor_bestEnd = new idVec3(), floor_bestNormal = new idVec3();
            idVec3 water_bestStart = new idVec3(), water_bestEnd = new idVec3(), water_bestNormal = new idVec3();
            idVec3 testPoint;
            idPlane plane;
            aasArea_s area1, area2;
            aasFace_s floorFace1, floorFace2, floor_bestFace1, water_bestFace1;
            aasEdge_s edge1, edge2;
            idReachability_Walk walkReach;
            idReachability_BarrierJump barrierJumpReach;
            idReachability_WaterJump waterJumpReach;
            idReachability_WalkOffLedge walkOffLedgeReach;
            final aasTrace_s trace = new aasTrace_s();

            // must be able to walk or swim in the first area
            if (!AreaHasFloor(fromAreaNum) && !CanSwimInArea(fromAreaNum)) {
                return false;
            }

            if (!AreaHasFloor(toAreaNum) && !CanSwimInArea(toAreaNum)) {
                return false;
            }

            area1 = this.file.areas.oGet(fromAreaNum);
            area2 = this.file.areas.oGet(toAreaNum);

            // if the areas are not near anough in the x-y direction
            for (i = 0; i < 2; i++) {
                if (area1.bounds.oGet(0, i) > (area2.bounds.oGet(1, i) + 2.0f)) {
                    return false;
                }
                if (area1.bounds.oGet(1, i) < (area2.bounds.oGet(0, i) - 2.0f)) {
                    return false;
                }
            }

            floor_foundReach = 0;//false;
            floor_bestDist = 99999;
            floor_bestLength = 0;
            floor_bestArea2FloorEdgeNum = 0;

            water_foundReach = 0;//false;
            water_bestDist = 99999;
            water_bestLength = 0;
            water_bestArea2FloorEdgeNum = 0;

            for (i = 0; i < area1.numFaces; i++) {
                floorFace1Num = this.file.faceIndex.oGet(area1.firstFace + i);
                faceSide1 = floorFace1Num < 0;
                floorFace1 = this.file.faces.oGet(abs(floorFace1Num));

                // if this isn't a floor face
                if (0 == (floorFace1.flags & FACE_FLOOR)) {

                    // if we can swim in the first area
                    if (CanSwimInArea(fromAreaNum)) {

                        // face plane must be more or less horizontal
                        plane = this.file.planeList.oGet(floorFace1.planeNum ^ ((!faceSide1) ? 1 : 0));
                        if (plane.Normal().oMultiply(this.file.settings.invGravityDir) < this.file.settings.minFloorCos[0]) {
                            continue;
                        }
                    } else {
                        // if we can't swim in the area it must be a ground face
                        continue;
                    }
                }

                for (k = 0; k < floorFace1.numEdges; k++) {
                    edge1Num = this.file.edgeIndex.oGet(floorFace1.firstEdge + k);
                    side1 = btoi(edge1Num < 0);
                    // NOTE: for water faces we must take the side area 1 is on into
                    // account because the face is shared and doesn't have to be oriented correctly
                    if (0 == (floorFace1.flags & FACE_FLOOR)) {
                        side1 = btoi(itob(side1) == faceSide1);
                    }
                    edge1Num = abs(edge1Num);
                    edge1 = this.file.edges.oGet(edge1Num);
                    // vertices of the edge
                    v1 = this.file.vertices.oGet(edge1.vertexNum[SNOT(side1)]);
                    v2 = this.file.vertices.oGet(edge1.vertexNum[side1]);
                    // get a vertical plane through the edge
                    // NOTE: normal is pointing into area 2 because the face edges are stored counter clockwise
                    edgeVec = v2.oMinus(v1);
                    normal = edgeVec.Cross(this.file.settings.invGravityDir);
                    normal.Normalize();
                    dist = normal.oMultiply(v1);

                    // check the faces from the second area
                    for (j = 0; j < area2.numFaces; j++) {
                        floorFace2 = this.file.faces.oGet(abs(this.file.faceIndex.oGet(area2.firstFace + j)));
                        // must be a ground face
                        if (0 == (floorFace2.flags & FACE_FLOOR)) {
                            continue;
                        }
                        // check the edges of this ground face
                        for (l = 0; l < floorFace2.numEdges; l++) {
                            edge2Num = abs(this.file.edgeIndex.oGet(floorFace2.firstEdge + l));
                            edge2 = this.file.edges.oGet(edge2Num);
                            // vertices of the edge
                            v3 = this.file.vertices.oGet(edge2.vertexNum[0]);
                            v4 = this.file.vertices.oGet(edge2.vertexNum[1]);
                            // check the distance between the two points and the vertical plane through the edge of area1
                            diff = normal.oMultiply(v3) - dist;
                            if ((diff < -0.2f) || (diff > 0.2f)) {
                                continue;
                            }
                            diff = normal.oMultiply(v4) - dist;
                            if ((diff < -0.2f) || (diff > 0.2f)) {
                                continue;
                            }

                            // project the two ground edges into the step side plane
                            // and calculate the shortest distance between the two
                            // edges if they overlap in the direction orthogonal to
                            // the gravity direction
                            orthogonal = this.file.settings.invGravityDir.Cross(normal);
                            invGravityDot = this.file.settings.invGravityDir.oMultiply(this.file.settings.invGravityDir);
                            orthogonalDot = orthogonal.oMultiply(orthogonal);
                            // projection into the step plane
                            // NOTE: since gravity is vertical this is just the z coordinate
                            y1 = v1.oGet(2);//(v1 * file->settings.invGravity) / invGravityDot;
                            y2 = v2.oGet(2);//(v2 * file->settings.invGravity) / invGravityDot;
                            y3 = v3.oGet(2);//(v3 * file->settings.invGravity) / invGravityDot;
                            y4 = v4.oGet(2);//(v4 * file->settings.invGravity) / invGravityDot;

                            x1 = (v1.oMultiply(orthogonal)) / orthogonalDot;
                            x2 = (v2.oMultiply(orthogonal)) / orthogonalDot;
                            x3 = (v3.oMultiply(orthogonal)) / orthogonalDot;
                            x4 = (v4.oMultiply(orthogonal)) / orthogonalDot;

                            if (x1 > x2) {
                                tmp = x1;
                                x1 = x2;
                                x2 = tmp;
                                tmp = y1;
                                y1 = y2;
                                y2 = tmp;
                                tmpv = v1;
                                v1 = v2;
                                v2 = tmpv;
                            }
                            if (x3 > x4) {
                                tmp = x3;
                                x3 = x4;
                                x4 = tmp;
                                tmp = y3;
                                y3 = y4;
                                y4 = tmp;
                                tmpv = v3;
                                v3 = v4;
                                v4 = tmpv;
                            }
                            // if the two projected edge lines have no overlap
                            if ((x2 <= x3) || (x4 <= x1)) {
                                continue;
                            }
                            // if the two lines fully overlap
                            if ((((x1 - 0.5f) < x3) && (x4 < (x2 + 0.5f))) && (((x3 - 0.5f) < x1) && (x2 < (x4 + 0.5f)))) {
                                dist1 = y3 - y1;
                                dist2 = y4 - y2;
                                p1area1 = v1;
                                p2area1 = v2;
                                p1area2 = v3;
                                p2area2 = v4;
                            } else {
                                // if the points are equal
                                if ((x1 > (x3 - 0.1f)) && (x1 < (x3 + 0.1f))) {
                                    dist1 = y3 - y1;
                                    p1area1 = v1;
                                    p1area2 = v3;
                                } else if (x1 < x3) {
                                    y = y1 + (((x3 - x1) * (y2 - y1)) / (x2 - x1));
                                    dist1 = y3 - y;
                                    p1area1 = v3;
                                    p1area1.oSet(2, y);
                                    p1area2 = v3;
                                } else {
                                    y = y3 + (((x1 - x3) * (y4 - y3)) / (x4 - x3));
                                    dist1 = y - y1;
                                    p1area1 = v1;
                                    p1area2 = v1;
                                    p1area2.oSet(2, y);
                                }
                                // if the points are equal
                                if ((x2 > (x4 - 0.1f)) && (x2 < (x4 + 0.1f))) {
                                    dist2 = y4 - y2;
                                    p2area1 = v2;
                                    p2area2 = v4;
                                } else if (x2 < x4) {
                                    y = y3 + (((x2 - x3) * (y4 - y3)) / (x4 - x3));
                                    dist2 = y - y2;
                                    p2area1 = v2;
                                    p2area2 = v2;
                                    p2area2.oSet(2, y);
                                } else {
                                    y = y1 + (((x4 - x1) * (y2 - y1)) / (x2 - x1));
                                    dist2 = y4 - y;
                                    p2area1 = v4;
                                    p2area1.oSet(2, y);
                                    p2area2 = v4;
                                }
                            }

                            // if both distances are pretty much equal then we take the middle of the points
                            if ((dist1 > (dist2 - 1.0f)) && (dist1 < (dist2 + 1.0f))) {
                                dist = dist1;
                                start = (p1area1.oPlus(p2area1)).oMultiply(0.5f);
                                end = (p1area2.oPlus(p2area2)).oMultiply(0.5f);
                            } else if (dist1 < dist2) {
                                dist = dist1;
                                start = p1area1;
                                end = p1area2;
                            } else {
                                dist = dist2;
                                start = p2area1;
                                end = p2area2;
                            }

                            // get the length of the overlapping part of the edges of the two areas
                            length = (p2area2.oMinus(p1area2)).Length();

                            if ((floorFace1.flags & FACE_FLOOR) != 0) {
                                // if the vertical distance is smaller
                                if ((dist < floor_bestDist)
                                        || // or the vertical distance is pretty much the same
                                        // but the overlapping part of the edges is longer
                                        ((dist < (floor_bestDist + 1.0f)) && (length > floor_bestLength))) {
                                    floor_bestDist = dist;
                                    floor_bestLength = length;
                                    floor_foundReach = 1;//true;
                                    floor_bestArea1FloorEdgeNum = edge1Num;
                                    floor_bestArea2FloorEdgeNum = edge2Num;
                                    floor_bestFace1 = floorFace1;
                                    floor_bestStart = start;
                                    floor_bestNormal = normal;
                                    floor_bestEnd = end;
                                }
                            } else {
                                // if the vertical distance is smaller
                                if ((dist < water_bestDist)
                                        || //or the vertical distance is pretty much the same
                                        //but the overlapping part of the edges is longer
                                        ((dist < (water_bestDist + 1.0f)) && (length > water_bestLength))) {
                                    water_bestDist = dist;
                                    water_bestLength = length;
                                    water_foundReach = 1;//true;
                                    water_bestArea1FloorEdgeNum = edge1Num;
                                    water_bestArea2FloorEdgeNum = edge2Num;
                                    water_bestFace1 = floorFace1;
                                    water_bestStart = start;	// best start point in area1
                                    water_bestNormal = normal;	// normal is pointing into area2
                                    water_bestEnd = end;		// best point towards area2
                                }
                            }
                        }
                    }
                }
            }
            //
            // NOTE: swim reachabilities should already be filtered out
            //
            // Steps
            //
            //         ---------
            //         |          step height -> TFL_WALK
            // --------|
            //
            //         ---------
            // ~~~~~~~~|          step height and low water -> TFL_WALK
            // --------|
            //
            // ~~~~~~~~~~~~~~~~~~
            //         ---------
            //         |          step height and low water up to the step -> TFL_WALK
            // --------|
            //
            // check for a step reachability
            if (floor_foundReach != 0) {
                // if area2 is higher but lower than the maximum step height
                // NOTE: floor_bestDist >= 0 also catches equal floor reachabilities
                if ((floor_bestDist >= 0) && (floor_bestDist < this.file.settings.maxStepHeight[0])) {
                    // create walk reachability from area1 to area2
                    walkReach = new idReachability_Walk();
                    walkReach.travelType = TFL_WALK;
                    walkReach.toAreaNum = (short) toAreaNum;
                    walkReach.fromAreaNum = (short) fromAreaNum;
                    walkReach.start = floor_bestStart.oPlus(floor_bestNormal.oMultiply(INSIDEUNITS_WALKSTART));
                    walkReach.end = floor_bestEnd.oPlus(floor_bestNormal.oMultiply(INSIDEUNITS_WALKEND));
                    walkReach.edgeNum = abs(floor_bestArea1FloorEdgeNum);
                    walkReach.travelTime = 0;
                    if ((area2.flags & AREA_CROUCH) != 0) {
                        walkReach.travelTime += this.file.settings.tt_startCrouching[0];
                    }
                    AddReachabilityToArea(walkReach, fromAreaNum);
                    return true;
                }
            }
            //
            // Water Jumps
            //
            //         ---------
            //         |
            // ~~~~~~~~|
            //         |
            //         |          higher than step height and water up to waterjump height -> TFL_WATERJUMP
            // --------|
            //
            // ~~~~~~~~~~~~~~~~~~
            //         ---------
            //         |
            //         |
            //         |
            //         |          higher than step height and low water up to the step -> TFL_WATERJUMP
            // --------|
            //
            // check for a waterjump reachability
            if (water_foundReach != 0) {
                // get a test point a little bit towards area1
                testPoint = water_bestEnd.oMinus(water_bestNormal.oMultiply(INSIDEUNITS));
                // go down the maximum waterjump height
                testPoint.oMinSet(2, this.file.settings.maxWaterJumpHeight[0]);
                // if there IS water the sv_maxwaterjump height below the bestend point
                if ((area1.flags & AREA_LIQUID) != 0) {
                    // don't create rediculous water jump reachabilities from areas very far below the water surface
                    if (water_bestDist < (this.file.settings.maxWaterJumpHeight[0] + 24)) {
                        // water jumping from or towards a crouch only areas is not possible
                        if ((0 == (area1.flags & AREA_CROUCH)) && (0 == (area2.flags & AREA_CROUCH))) {
                            // create water jump reachability from area1 to area2
                            waterJumpReach = new idReachability_WaterJump();
                            waterJumpReach.travelType = TFL_WATERJUMP;
                            waterJumpReach.toAreaNum = (short) toAreaNum;
                            waterJumpReach.fromAreaNum = (short) fromAreaNum;
                            waterJumpReach.start = water_bestStart;
                            waterJumpReach.end = water_bestEnd.oPlus(water_bestNormal.oMultiply(INSIDEUNITS_WATERJUMP));
                            waterJumpReach.edgeNum = abs(floor_bestArea1FloorEdgeNum);
                            waterJumpReach.travelTime = this.file.settings.tt_waterJump[0];
                            AddReachabilityToArea(waterJumpReach, fromAreaNum);
                            return true;
                        }
                    }
                }
            }
            //
            // Barrier Jumps
            //
            //         ---------
            //         |
            //         |
            //         |
            //         |         higher than max step height lower than max barrier height -> TFL_BARRIERJUMP
            // --------|
            //
            //         ---------
            //         |
            //         |
            //         |
            // ~~~~~~~~|         higher than max step height lower than max barrier height
            // --------|         and a thin layer of water in the area to jump from -> TFL_BARRIERJUMP
            //
            // check for a barrier jump reachability
            if (floor_foundReach != 0) {
                //if area2 is higher but lower than the maximum barrier jump height
                if ((floor_bestDist > 0) && (floor_bestDist < this.file.settings.maxBarrierHeight[0])) {
                    //if no water in area1 or a very thin layer of water on the ground
                    if ((0 == water_foundReach) || ((floor_bestDist - water_bestDist) < 16)) {
                        // cannot perform a barrier jump towards or from a crouch area
                        if ((0 == (area1.flags & AREA_CROUCH)) && (0 == (area2.flags & AREA_CROUCH))) {
                            // create barrier jump reachability from area1 to area2
                            barrierJumpReach = new idReachability_BarrierJump();
                            barrierJumpReach.travelType = TFL_BARRIERJUMP;
                            barrierJumpReach.toAreaNum = (short) toAreaNum;
                            barrierJumpReach.fromAreaNum = (short) fromAreaNum;
                            barrierJumpReach.start = floor_bestStart.oPlus(floor_bestNormal.oMultiply(INSIDEUNITS_WALKSTART));
                            barrierJumpReach.end = floor_bestEnd.oPlus(floor_bestNormal.oMultiply(INSIDEUNITS_WALKEND));
                            barrierJumpReach.edgeNum = abs(floor_bestArea1FloorEdgeNum);
                            barrierJumpReach.travelTime = this.file.settings.tt_barrierJump[0];
                            AddReachabilityToArea(barrierJumpReach, fromAreaNum);
                            return true;
                        }
                    }
                }
            }
            //
            // Walk and Walk Off Ledge
            //
            // --------|
            //         |          can walk or step back -> TFL_WALK
            //         ---------
            //
            // --------|
            //         |
            //         |
            //         |
            //         |          cannot walk/step back -> TFL_WALKOFFLEDGE
            //         ---------
            //
            // --------|
            //         |
            //         |~~~~~~~~
            //         |
            //         |          cannot step back but can waterjump back -> TFL_WALKOFFLEDGE
            //         ---------  FIXME: create TFL_WALK reach??
            //
            // check for a walk or walk off ledge reachability
            if (floor_foundReach != 0) {
                if (floor_bestDist < 0) {
                    if (floor_bestDist > -this.file.settings.maxStepHeight[0]) {
                        // create walk reachability from area1 to area2
                        walkReach = new idReachability_Walk();
                        walkReach.travelType = TFL_WALK;
                        walkReach.toAreaNum = (short) toAreaNum;
                        walkReach.fromAreaNum = (short) fromAreaNum;
                        walkReach.start = floor_bestStart.oPlus(floor_bestNormal.oMultiply(INSIDEUNITS_WALKSTART));
                        walkReach.end = floor_bestEnd.oPlus(floor_bestNormal.oMultiply(INSIDEUNITS_WALKEND));
                        walkReach.edgeNum = abs(floor_bestArea1FloorEdgeNum);
                        walkReach.travelTime = 1;
                        AddReachabilityToArea(walkReach, fromAreaNum);
                        return true;
                    }
                    // if no maximum fall height set or less than the max
                    if ((0 == this.file.settings.maxFallHeight[0]) || (idMath.Fabs(floor_bestDist) < this.file.settings.maxFallHeight[0])) {
                        // trace a bounding box vertically to check for solids
                        floor_bestEnd.oPluSet(floor_bestNormal.oMultiply(INSIDEUNITS));
                        start = floor_bestEnd;
                        start.oSet(2, floor_bestStart.oGet(2));
                        end = floor_bestEnd;
                        end.oPluSet(2, 4);
                        trace.areas = areas;
                        trace.maxAreas = areas.length;
                        this.file.Trace(trace, start, end);
                        // if the trace didn't start in solid and nothing was hit
                        if ((trace.lastAreaNum != 0) && (trace.fraction >= 1.0f)) {
                            // the trace end point must be in the goal area
                            if (trace.lastAreaNum == toAreaNum) {
                                // don't create reachability if going through a cluster portal
                                for (i = 0; i < trace.numAreas; i++) {
                                    if (AreaIsClusterPortal(trace.areas[i])) {
                                        break;
                                    }
                                }
                                if (i >= trace.numAreas) {
                                    // create a walk off ledge reachability from area1 to area2
                                    walkOffLedgeReach = new idReachability_WalkOffLedge();
                                    walkOffLedgeReach.travelType = TFL_WALKOFFLEDGE;
                                    walkOffLedgeReach.toAreaNum = (short) toAreaNum;
                                    walkOffLedgeReach.fromAreaNum = (short) fromAreaNum;
                                    walkOffLedgeReach.start = floor_bestStart;
                                    walkOffLedgeReach.end = floor_bestEnd;
                                    walkOffLedgeReach.edgeNum = abs(floor_bestArea1FloorEdgeNum);
                                    walkOffLedgeReach.travelTime = (int) (this.file.settings.tt_startWalkOffLedge[0] + ((idMath.Fabs(floor_bestDist) * 50) / this.file.settings.gravityValue));
                                    AddReachabilityToArea(walkOffLedgeReach, fromAreaNum);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }

        private void Reachability_WalkOffLedge(int areaNum) {
            int i, j, faceNum, edgeNum, side, reachAreaNum, p;
            final int[] areas = new int[10];
            aasArea_s area;
            aasFace_s face;
            aasEdge_s edge;
            idPlane plane;
            idVec3 v1, v2, mid, dir, testEnd;
            idReachability_WalkOffLedge reach;
            final aasTrace_s trace = new aasTrace_s();

            if (!AreaHasFloor(areaNum) || CanSwimInArea(areaNum)) {
                return;
            }

            area = this.file.areas.oGet(areaNum);

            for (i = 0; i < area.numFaces; i++) {
                faceNum = this.file.faceIndex.oGet(area.firstFace + i);
                face = this.file.faces.oGet(abs(faceNum));

                // face must be a floor face
                if (0 == (face.flags & FACE_FLOOR)) {
                    continue;
                }

                for (j = 0; j < face.numEdges; j++) {

                    edgeNum = this.file.edgeIndex.oGet(face.firstEdge + j);
                    edge = this.file.edges.oGet(abs(edgeNum));

                    //if ( !(edge.flags & EDGE_LEDGE) ) {
                    //	continue;
                    //}
                    side = btoi(edgeNum < 0);

                    v1 = this.file.vertices.oGet(edge.vertexNum[side]);
                    v2 = this.file.vertices.oGet(edge.vertexNum[SNOT(side)]);

                    plane = this.file.planeList.oGet(face.planeNum ^ INTSIGNBITSET(faceNum));

                    // get the direction into the other area
                    dir = plane.Normal().Cross(v2.oMinus(v1));
                    dir.Normalize();

                    mid = (v1.oPlus(v2)).oMultiply(0.5f);
                    testEnd = mid.oPlus(dir.oMultiply(INSIDEUNITS_WALKEND));
                    testEnd.oMinSet(2, this.file.settings.maxFallHeight[0] + 1.0f);
                    trace.areas = areas;
                    trace.maxAreas = areas.length;
                    this.file.Trace(trace, mid, testEnd);

                    reachAreaNum = trace.lastAreaNum;
                    if ((0 == reachAreaNum) || (reachAreaNum == areaNum)) {
                        continue;
                    }
                    if (idMath.Fabs(mid.oGet(2) - trace.endpos.oGet(2)) > this.file.settings.maxFallHeight[0]) {
                        continue;
                    }
                    if (!AreaHasFloor(reachAreaNum) && !CanSwimInArea(reachAreaNum)) {
                        continue;
                    }
                    if (ReachabilityExists(areaNum, reachAreaNum)) {
                        continue;
                    }
                    // if not going through a cluster portal
                    for (p = 0; p < trace.numAreas; p++) {
                        if (AreaIsClusterPortal(trace.areas[p])) {
                            break;
                        }
                    }
                    if (p < trace.numAreas) {
                        continue;
                    }

                    reach = new idReachability_WalkOffLedge();
                    reach.travelType = TFL_WALKOFFLEDGE;
                    reach.toAreaNum = (short) reachAreaNum;
                    reach.fromAreaNum = (short) areaNum;
                    reach.start = mid;
                    reach.end = trace.endpos;
                    reach.edgeNum = abs(edgeNum);
                    reach.travelTime = (int) (this.file.settings.tt_startWalkOffLedge[0] + ((idMath.Fabs(mid.oGet(2) - trace.endpos.oGet(2)) * 50) / this.file.settings.gravityValue));
                    AddReachabilityToArea(reach, areaNum);
                }
            }
        }
    }
}
