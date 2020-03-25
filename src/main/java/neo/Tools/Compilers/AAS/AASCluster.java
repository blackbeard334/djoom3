package neo.Tools.Compilers.AAS;

import static java.lang.Math.abs;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_CLUSTERPORTAL;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_FLY;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_WALK;
import static neo.framework.Common.common;

import neo.Tools.Compilers.AAS.AASFile.aasArea_s;
import neo.Tools.Compilers.AAS.AASFile.aasCluster_s;
import neo.Tools.Compilers.AAS.AASFile.aasFace_s;
import neo.Tools.Compilers.AAS.AASFile.aasPortal_s;
import neo.Tools.Compilers.AAS.AASFile.idReachability;
import neo.Tools.Compilers.AAS.AASFile_local.idAASFileLocal;

/**
 *
 */
public class AASCluster {

    /*
     ===============================================================================

     Area Clustering

     ===============================================================================
     */
    static class idAASCluster {

        private idAASFileLocal file;
        private boolean        noFaceFlood;
        //
        //

        public boolean Build(idAASFileLocal file) {

            common.Printf("[Clustering]\n");

            this.file = file;
            this.noFaceFlood = true;

            RemoveInvalidPortals();

            while (true) {

                // delete all existing clusters
                file.DeleteClusters();

                // create the portals from the portal areas
                CreatePortals();

                common.Printf("\r%6d", file.portals.Num());

                // find the clusters
                if (!FindClusters()) {
                    continue;
                }

                // test the portals
                if (!TestPortals()) {
                    continue;
                }

                break;
            }

            common.Printf("\r%6d portals\n", file.portals.Num());
            common.Printf("%6d clusters\n", file.clusters.Num());

            for (int i = 0; i < file.clusters.Num(); i++) {
                common.Printf("%6d reachable areas in cluster %d\n", file.clusters.oGet(i).numReachableAreas, i);
            }

            file.ReportRoutingEfficiency();

            return true;
        }

        public boolean BuildSingleCluster(idAASFileLocal file) {
            int i, numAreas;
            final aasCluster_s cluster = new aasCluster_s();

            common.Printf("[Clustering]\n");

            this.file = file;

            // delete all existing clusters
            file.DeleteClusters();

            cluster.firstPortal = 0;
            cluster.numPortals = 0;
            cluster.numAreas = file.areas.Num();
            cluster.numReachableAreas = 0;
            // give all reachable areas in the cluster a number
            for (i = 0; i < file.areas.Num(); i++) {
                file.areas.oGet(i).cluster = (short) file.clusters.Num();
                if ((file.areas.oGet(i).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY)) != 0) {
                    file.areas.oGet(i).clusterAreaNum = (short) cluster.numReachableAreas++;
                }
            }
            // give the remaining areas a number within the cluster
            numAreas = cluster.numReachableAreas;
            for (i = 0; i < file.areas.Num(); i++) {
                if ((file.areas.oGet(i).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY)) != 0) {
                    continue;
                }
                file.areas.oGet(i).clusterAreaNum = (short) numAreas++;
            }
            file.clusters.Append(cluster);

            common.Printf("%6d portals\n", file.portals.Num());
            common.Printf("%6d clusters\n", file.clusters.Num());

            for (i = 0; i < file.clusters.Num(); i++) {
                common.Printf("%6d reachable areas in cluster %d\n", file.clusters.oGet(i).numReachableAreas, i);
            }

            file.ReportRoutingEfficiency();

            return true;
        }

        private boolean UpdatePortal(int areaNum, int clusterNum) {
            int portalNum;
            aasPortal_s portal;

            // find the portal for this area
            for (portalNum = 1; portalNum < this.file.portals.Num(); portalNum++) {
                if (this.file.portals.oGet(portalNum).areaNum == areaNum) {
                    break;
                }
            }

            if (portalNum >= this.file.portals.Num()) {
                common.Error("no portal for area %d", areaNum);
                return true;
            }

            portal = this.file.portals.oGet(portalNum);

            // if the portal is already fully updated
            if (portal.clusters[0] == clusterNum) {
                return true;
            }
            if (portal.clusters[1] == clusterNum) {
                return true;
            }
            // if the portal has no front cluster yet
            if (0 == portal.clusters[0]) {
                portal.clusters[0] = (short) clusterNum;
            } // if the portal has no back cluster yet
            else if (0 == portal.clusters[1]) {
                portal.clusters[1] = (short) clusterNum;
            } else {
                // remove the cluster portal flag contents
                this.file.areas.oGet(areaNum).contents &= ~AREACONTENTS_CLUSTERPORTAL;
                return false;
            }

            // set the area cluster number to the negative portal number
            this.file.areas.oGet(areaNum).cluster = (short) -portalNum;

            // add the portal to the cluster using the portal index
            this.file.portalIndex.Append(portalNum);
            this.file.clusters.oGet(clusterNum).numPortals++;
            return true;
        }

        private boolean FloodClusterAreas_r(int areaNum, int clusterNum) {
            aasArea_s area;
            aasFace_s face;
            int faceNum, i;
            idReachability reach;

            area = this.file.areas.oGet(areaNum);

            // if the area is already part of a cluster
            if (area.cluster > 0) {
                if (area.cluster == clusterNum) {
                    return true;
                }
                // there's a reachability going from one cluster to another only in one direction
                common.Error("cluster %d touched cluster %d at area %d\r\n", clusterNum, this.file.areas.oGet(areaNum).cluster, areaNum);
                return false;
            }

            // if this area is a cluster portal
            if ((area.contents & AREACONTENTS_CLUSTERPORTAL) != 0) {
                return UpdatePortal(areaNum, clusterNum);
            }

            // set the area cluster number
            area.cluster = (short) clusterNum;

            if (!this.noFaceFlood) {
                // use area faces to flood into adjacent areas
                for (i = 0; i < area.numFaces; i++) {
                    faceNum = abs(this.file.faceIndex.oGet(area.firstFace + i));
                    face = this.file.faces.oGet(faceNum);
                    if (face.areas[0] == areaNum) {
                        if (face.areas[1] != 0) {
                            if (!FloodClusterAreas_r(face.areas[1], clusterNum)) {
                                return false;
                            }
                        }
                    } else {
                        if (face.areas[0] != 0) {
                            if (!FloodClusterAreas_r(face.areas[0], clusterNum)) {
                                return false;
                            }
                        }
                    }
                }
            }

            // use the reachabilities to flood into other areas
            for (reach = this.file.areas.oGet(areaNum).reach; reach != null; reach = reach.next) {
                if (!FloodClusterAreas_r(reach.toAreaNum, clusterNum)) {
                    return false;
                }
            }

            // use the reversed reachabilities to flood into other areas
            for (reach = this.file.areas.oGet(areaNum).rev_reach; reach != null; reach = reach.rev_next) {
                if (!FloodClusterAreas_r(reach.fromAreaNum, clusterNum)) {
                    return false;
                }
            }

            return true;
        }

        private void RemoveAreaClusterNumbers() {
            int i;

            for (i = 1; i < this.file.areas.Num(); i++) {
                this.file.areas.oGet(i).cluster = 0;
            }
        }

        private void NumberClusterAreas(int clusterNum) {
            int i, portalNum;
            aasCluster_s cluster;
            aasPortal_s portal;

            cluster = this.file.clusters.oGet(clusterNum);
            cluster.numAreas = 0;
            cluster.numReachableAreas = 0;

            // number all areas in this cluster WITH reachabilities
            for (i = 1; i < this.file.areas.Num(); i++) {

                if (this.file.areas.oGet(i).cluster != clusterNum) {
                    continue;
                }

                if (0 == (this.file.areas.oGet(i).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY))) {
                    continue;
                }

                this.file.areas.oGet(i).clusterAreaNum = (short) cluster.numAreas++;
                cluster.numReachableAreas++;
            }

            // number all portals in this cluster WITH reachabilities
            for (i = 0; i < cluster.numPortals; i++) {
                portalNum = this.file.portalIndex.oGet(cluster.firstPortal + i);
                portal = this.file.portals.oGet(portalNum);

                if (0 == (this.file.areas.oGet(portal.areaNum).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY))) {
                    continue;
                }

                if (portal.clusters[0] == clusterNum) {
                    portal.clusterAreaNum[0] = (short) cluster.numAreas++;
                } else {
                    portal.clusterAreaNum[1] = (short) cluster.numAreas++;
                }
                cluster.numReachableAreas++;
            }

            // number all areas in this cluster WITHOUT reachabilities
            for (i = 1; i < this.file.areas.Num(); i++) {

                if (this.file.areas.oGet(i).cluster != clusterNum) {
                    continue;
                }

                if ((this.file.areas.oGet(i).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY)) != 0) {
                    continue;
                }

                this.file.areas.oGet(i).clusterAreaNum = (short) cluster.numAreas++;
            }

            // number all portals in this cluster WITHOUT reachabilities
            for (i = 0; i < cluster.numPortals; i++) {
                portalNum = this.file.portalIndex.oGet(cluster.firstPortal + i);
                portal = this.file.portals.oGet(portalNum);

                if ((this.file.areas.oGet(portal.areaNum).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY)) != 0) {
                    continue;
                }

                if (portal.clusters[0] == clusterNum) {
                    portal.clusterAreaNum[0] = (short) cluster.numAreas++;
                } else {
                    portal.clusterAreaNum[1] = (short) cluster.numAreas++;
                }
            }
        }

        private boolean FindClusters() {
            int i, clusterNum;
            final aasCluster_s cluster = new aasCluster_s();

            RemoveAreaClusterNumbers();

            for (i = 1; i < this.file.areas.Num(); i++) {
                // if the area is already part of a cluster
                if (this.file.areas.oGet(i).cluster != 0) {
                    continue;
                }

                // if not flooding through faces only use areas that have reachabilities
                if (this.noFaceFlood) {
                    if (0 == (this.file.areas.oGet(i).flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY))) {
                        continue;
                    }
                }

                // if the area is a cluster portal
                if ((this.file.areas.oGet(i).contents & AREACONTENTS_CLUSTERPORTAL) != 0) {
                    continue;
                }

                cluster.numAreas = 0;
                cluster.numReachableAreas = 0;
                cluster.firstPortal = this.file.portalIndex.Num();
                cluster.numPortals = 0;
                clusterNum = this.file.clusters.Num();
                this.file.clusters.Append(cluster);

                // flood the areas in this cluster
                if (!FloodClusterAreas_r(i, clusterNum)) {
                    return false;
                }

                // number the cluster areas
                NumberClusterAreas(clusterNum);
            }
            return true;
        }

        private void CreatePortals() {
            int i;
            aasPortal_s portal = null;

            for (i = 1; i < this.file.areas.Num(); i++) {
                // if the area is a cluster portal
                if ((this.file.areas.oGet(i).contents & AREACONTENTS_CLUSTERPORTAL) != 0) {
                	portal = new aasPortal_s();
                	portal.areaNum = (short) i;
                    portal.clusters[0] = portal.clusters[1] = 0;
                    portal.maxAreaTravelTime = 0;
                    this.file.portals.Append(portal);
                }
            }
        }

        private boolean TestPortals() {
            int i;
            aasPortal_s portal, portal2;
            aasArea_s area, area2;
            idReachability reach;
            boolean ok;

            ok = true;
            for (i = 1; i < this.file.portals.Num(); i++) {
                portal = this.file.portals.oGet(i);
                area = this.file.areas.oGet(portal.areaNum);

                // if this portal was already removed
                if (0 == (area.contents & AREACONTENTS_CLUSTERPORTAL)) {
                    continue;
                }

                // may not removed this portal if it has a reachability to a removed portal
                for (reach = area.reach; reach != null; reach = reach.next) {
                    area2 = this.file.areas.oGet(reach.toAreaNum);
                    if ((area2.contents & AREACONTENTS_CLUSTERPORTAL) != 0) {
                        continue;
                    }
                    if (area2.cluster < 0) {
                        break;
                    }
                }
                if (reach != null) {
                    continue;
                }

                // may not removed this portal if it has a reversed reachability to a removed portal
                for (reach = area.rev_reach; reach != null; reach = reach.rev_next) {
                    area2 = this.file.areas.oGet(reach.toAreaNum);
                    if ((area2.contents & AREACONTENTS_CLUSTERPORTAL) != 0) {
                        continue;
                    }
                    if (area2.cluster < 0) {
                        break;
                    }
                }
                if (reach != null) {
                    continue;
                }

                // portal should have two clusters set
                if (0 == portal.clusters[0]) {
                    area.contents &= ~AREACONTENTS_CLUSTERPORTAL;
                    ok = false;
                    continue;
                }
                if (0 == portal.clusters[1]) {
                    area.contents &= ~AREACONTENTS_CLUSTERPORTAL;
                    ok = false;
                    continue;
                }

                // this portal may not have reachabilities to a portal that doesn't seperate the same clusters
                for (reach = area.reach; reach != null; reach = reach.next) {
                    area2 = this.file.areas.oGet(reach.toAreaNum);

                    if (0 == (area2.contents & AREACONTENTS_CLUSTERPORTAL)) {
                        continue;
                    }

                    if (area2.cluster > 0) {
                        area2.contents &= ~AREACONTENTS_CLUSTERPORTAL;
                        ok = false;
                        continue;
                    }

                    portal2 = this.file.portals.oGet(-this.file.areas.oGet(reach.toAreaNum).cluster);

                    if (((portal2.clusters[0] != portal.clusters[0]) && (portal2.clusters[0] != portal.clusters[1]))
                            || ((portal2.clusters[1] != portal.clusters[0]) && (portal2.clusters[1] != portal.clusters[1]))) {
                        area2.contents &= ~AREACONTENTS_CLUSTERPORTAL;
                        ok = false;
//                        continue;
                    }
                }
            }

            return ok;
        }

//        private void ReportEfficiency();
        private void RemoveInvalidPortals() {
            int i, j, k, face1Num, face2Num, otherAreaNum, numOpenAreas, numInvalidPortals;
            aasFace_s face1, face2;

            numInvalidPortals = 0;
            for (i = 0; i < this.file.areas.Num(); i++) {
                if (0 == (this.file.areas.oGet(i).contents & AREACONTENTS_CLUSTERPORTAL)) {
                    continue;
                }

                numOpenAreas = 0;
                for (j = 0; j < this.file.areas.oGet(i).numFaces; j++) {
                    face1Num = this.file.faceIndex.oGet(this.file.areas.oGet(i).firstFace + j);
                    face1 = this.file.faces.oGet(abs(face1Num));
                    otherAreaNum = face1.areas[ face1Num < 0 ? 1 : 0];

                    if (0 == otherAreaNum) {
                        continue;
                    }

                    for (k = 0; k < j; k++) {
                        face2Num = this.file.faceIndex.oGet(this.file.areas.oGet(i).firstFace + k);
                        face2 = this.file.faces.oGet(abs(face2Num));
                        if (otherAreaNum == face2.areas[ face2Num < 0 ? 1 : 0]) {
                            break;
                        }
                    }
                    if (k < j) {
                        continue;
                    }

                    if (0 == (this.file.areas.oGet(otherAreaNum).contents & AREACONTENTS_CLUSTERPORTAL)) {
                        numOpenAreas++;
                    }
                }

                if (numOpenAreas <= 1) {
                    this.file.areas.oGet(i).contents &= AREACONTENTS_CLUSTERPORTAL;
                    numInvalidPortals++;
                }
            }

            common.Printf("\r%6d invalid portals removed\n", numInvalidPortals);
        }
    }
}
