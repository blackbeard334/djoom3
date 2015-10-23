package neo.Tools.Compilers.AAS;

import static java.lang.Math.abs;
import neo.TempDump;
import static neo.TempDump.NOT;
import static neo.Tools.Compilers.AAS.AASFile.AAS_FILEID;
import static neo.Tools.Compilers.AAS.AASFile.AAS_FILEVERSION;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_WATER;
import static neo.Tools.Compilers.AAS.AASFile.AREA_LIQUID;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_FLY;
import static neo.Tools.Compilers.AAS.AASFile.AREA_REACHABLE_WALK;
import static neo.Tools.Compilers.AAS.AASFile.FACE_FLOOR;
import static neo.Tools.Compilers.AAS.AASFile.FACE_LADDER;
import static neo.Tools.Compilers.AAS.AASFile.MAX_AAS_TREE_DEPTH;
import static neo.Tools.Compilers.AAS.AASFile.Reachability_Read;
import static neo.Tools.Compilers.AAS.AASFile.Reachability_Special_Read;
import static neo.Tools.Compilers.AAS.AASFile.Reachability_Special_Write;
import static neo.Tools.Compilers.AAS.AASFile.Reachability_Write;
import static neo.Tools.Compilers.AAS.AASFile.TFL_AIR;
import static neo.Tools.Compilers.AAS.AASFile.TFL_SPECIAL;
import static neo.Tools.Compilers.AAS.AASFile.TFL_WATER;
import neo.Tools.Compilers.AAS.AASFile.aasArea_s;
import neo.Tools.Compilers.AAS.AASFile.aasCluster_s;
import neo.Tools.Compilers.AAS.AASFile.aasEdge_s;
import neo.Tools.Compilers.AAS.AASFile.aasFace_s;
import neo.Tools.Compilers.AAS.AASFile.aasNode_s;
import neo.Tools.Compilers.AAS.AASFile.aasPortal_s;
import neo.Tools.Compilers.AAS.AASFile.aasTrace_s;
import neo.Tools.Compilers.AAS.AASFile.idAASFile;
import neo.Tools.Compilers.AAS.AASFile.idReachability;
import neo.Tools.Compilers.AAS.AASFile.idReachability_Special;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.File_h.idFile;
import neo.idlib.BV.Bounds.idBounds;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Token.TT_INTEGER;
import static neo.idlib.Text.Token.TT_NUMBER;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import static neo.idlib.math.Math_h.INTSIGNBITSET;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.PLANESIDE_BACK;
import static neo.idlib.math.Plane.PLANESIDE_FRONT;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import static neo.idlib.math.Vector.vec3_origin;

/**
 *
 */
public class AASFile_local {

    static final int   AAS_LIST_GRANULARITY   = 1024;
    static final int   AAS_INDEX_GRANULARITY  = 4096;
    static final int   AAS_PLANE_GRANULARITY  = 4096;
    static final int   AAS_VERTEX_GRANULARITY = 4096;
    static final int   AAS_EDGE_GRANULARITY   = 4096;
    //
    static final float TRACEPLANE_EPSILON     = 0.125f;

    /*
     ===============================================================================

     AAS File Local

     ===============================================================================
     */
    public static class idAASFileLocal extends idAASFile {
        // friend class idAASBuild;
        // friend class idAASReach;
        // friend class idAASCluster;

        public idAASFileLocal() {
            super();
            planeList.SetGranularity(AAS_PLANE_GRANULARITY);
            vertices.SetGranularity(AAS_VERTEX_GRANULARITY);
            edges.SetGranularity(AAS_EDGE_GRANULARITY);
            edgeIndex.SetGranularity(AAS_INDEX_GRANULARITY);
            faces.SetGranularity(AAS_LIST_GRANULARITY);
            faceIndex.SetGranularity(AAS_INDEX_GRANULARITY);
            areas.SetGranularity(AAS_LIST_GRANULARITY);
            nodes.SetGranularity(AAS_LIST_GRANULARITY);
            portals.SetGranularity(AAS_LIST_GRANULARITY);
            portalIndex.SetGranularity(AAS_INDEX_GRANULARITY);
            clusters.SetGranularity(AAS_LIST_GRANULARITY);
        }
        // virtual 					~idAASFileLocal();

        @Override
        public idVec3 EdgeCenter(int edgeNum) {
            final aasEdge_s edge;
            edge = edges.oGet(edgeNum);
            return (vertices.oGet(edge.vertexNum[0]).oPlus(vertices.oGet(edge.vertexNum[1]))).oMultiply(0.5f);
        }

        @Override
        public idVec3 FaceCenter(int faceNum) {
            int i, edgeNum;
            final aasFace_s face;
            aasEdge_s edge;
            idVec3 center;

            center = vec3_origin;

            face = faces.oGet(faceNum);
            if (face.numEdges > 0) {
                for (i = 0; i < face.numEdges; i++) {
                    edgeNum = edgeIndex.oGet(face.firstEdge + i);
                    edge = edges.oGet(abs(edgeNum));
                    center.oPluSet(vertices.oGet(edge.vertexNum[INTSIGNBITSET(edgeNum)]));
                }
                center.oDivSet(face.numEdges);
            }
            return center;
        }

        @Override
        public idVec3 AreaCenter(int areaNum) {
            int i, faceNum;
            final aasArea_s area;
            idVec3 center;

            center = vec3_origin;

            area = areas.oGet(areaNum);
            if (area.numFaces > 0) {
                for (i = 0; i < area.numFaces; i++) {
                    faceNum = faceIndex.oGet(area.firstFace + i);
                    center.oPluSet(FaceCenter(abs(faceNum)));
                }
                center.oDivSet(area.numFaces);
            }
            return center;
        }

        @Override
        public idBounds EdgeBounds(int edgeNum) {
            final aasEdge_s edge;
            idBounds bounds = new idBounds();

            edge = edges.oGet(abs(edgeNum));
            bounds.oSet(0, bounds.oSet(1, vertices.oGet(edge.vertexNum[0])));
            bounds.oPluSet(vertices.oGet(edge.vertexNum[1]));
            return bounds;
        }

        @Override
        public idBounds FaceBounds(int faceNum) {
            int i, edgeNum;
            final aasFace_s face;
            aasEdge_s edge;
            idBounds bounds = new idBounds();

            face = faces.oGet(faceNum);
            bounds.Clear();

            for (i = 0; i < face.numEdges; i++) {
                edgeNum = edgeIndex.oGet(face.firstEdge + i);
                edge = edges.oGet(abs(edgeNum));
                bounds.AddPoint(vertices.oGet(edge.vertexNum[ INTSIGNBITSET(edgeNum)]));
            }
            return bounds;
        }

        @Override
        public idBounds AreaBounds(int areaNum) {
            int i, faceNum;
            final aasArea_s area;
            idBounds bounds = new idBounds();

            area = areas.oGet(areaNum);
            bounds.Clear();

            for (i = 0; i < area.numFaces; i++) {
                faceNum = faceIndex.oGet(area.firstFace + i);
                bounds.oPluSet(FaceBounds(abs(faceNum)));
            }
            return bounds;
        }

        @Override
        public int PointAreaNum(final idVec3 origin) {
            int nodeNum;
            aasNode_s node;

            nodeNum = 1;
            do {
                node = nodes.oGet(nodeNum);
                if (planeList.oGet(node.planeNum).Side(origin) == PLANESIDE_BACK) {
                    nodeNum = node.children[1];
                } else {
                    nodeNum = node.children[0];
                }
                if (nodeNum < 0) {
                    return -nodeNum;
                }
            } while (nodeNum != 0);

            return 0;
        }

        @Override
        public int PointReachableAreaNum(final idVec3 origin, final idBounds searchBounds, final int areaFlags, final int excludeTravelFlags) {
            int areaNum, i;
            idVec3 start, end;
            aasTrace_s trace = new aasTrace_s();
            idBounds bounds = new idBounds();
            float frak;
            final int[] areaList = new int[32];
            final idVec3[] pointList = new idVec3[32];

            start = origin;

            trace.areas = areaList;
            trace.points = pointList;
            trace.maxAreas = areaList.length;
            trace.getOutOfSolid = 1;// true;

            areaNum = PointAreaNum(start);
            if (areaNum != 0) {
                if (((areas.oGet(areaNum).flags & areaFlags) != 0) && ((areas.oGet(areaNum).travelFlags & excludeTravelFlags) == 0)) {
                    return areaNum;
                }
            } else {
                // trace up
                end = start;
                end.oPluSet(2, 32.0f);
                Trace(trace, start, end);
                if (trace.numAreas >= 1) {
                    if (((areas.oGet(0).flags & areaFlags) != 0) && ((areas.oGet(0).travelFlags & excludeTravelFlags) == 0)) {
                        return areaList[0];
                    }
                    start.oSet(pointList[0]);
                    start.oPluSet(2, 1.0f);
                }
            }

            // trace down
            end = start;
            end.oMinSet(2, 32.0f);
            Trace(trace, start, end);
            if (trace.lastAreaNum != 0) {
                if (((areas.oGet(trace.lastAreaNum).flags & areaFlags) != 0) && ((areas.oGet(trace.lastAreaNum).travelFlags & excludeTravelFlags) == 0)) {
                    return trace.lastAreaNum;
                }
                start.oSet(trace.endpos);
            }

            // expand bounds until an area is found
            for (i = 1; i <= 12; i++) {
                frak = i * (1.0f / 12.0f);
                bounds.oSet(0, origin.oPlus(searchBounds.oGet(0).oMultiply(frak)));
                bounds.oSet(1, origin.oPlus(searchBounds.oGet(1).oMultiply(frak)));
                areaNum = BoundsReachableAreaNum(bounds, areaFlags, excludeTravelFlags);
                if (areaNum != 0 && ((areas.oGet(areaNum).flags & areaFlags) != 0) && ((areas.oGet(areaNum).travelFlags & excludeTravelFlags) == 0)) {
                    return areaNum;
                }
            }
            return 0;
        }

        @Override
        public int BoundsReachableAreaNum(final idBounds bounds, final int areaFlags, final int excludeTravelFlags) {

            return BoundsReachableAreaNum_r(1, bounds, areaFlags, excludeTravelFlags);
        }

        @Override
        public void PushPointIntoAreaNum(int areaNum, idVec3 point) {
            int i, faceNum;
            final aasArea_s area;
            aasFace_s face;

            area = areas.oGet(areaNum);

            // push the point to the right side of all area face planes
            for (i = 0; i < area.numFaces; i++) {
                faceNum = faceIndex.oGet(area.firstFace + i);
                face = faces.oGet(abs(faceNum));

                final idPlane plane = planeList.oGet(face.planeNum ^ INTSIGNBITSET(faceNum));
                float dist = plane.Distance(point);

                // project the point onto the face plane if it is on the wrong side
                if (dist < 0.0f) {
                    point.oMinSet(plane.Normal().oMultiply(dist));
                }
            }
        }

        @Override
        public boolean Trace(aasTrace_s trace, final idVec3 start, final idVec3 end) {
            int side, nodeNum, tmpPlaneNum;
            double front, back, frac;
            idVec3 cur_start, cur_end, cur_mid, v1, v2;
            aasTraceStack_s[] tracestack = TempDump.allocArray(aasTraceStack_s.class, MAX_AAS_TREE_DEPTH);
            int tstack_p;
            aasNode_s node;
            idPlane plane;

            trace.numAreas = 0;
            trace.lastAreaNum = 0;
            trace.blockingAreaNum = 0;

            tstack_p = 0;//tracestack;
            tracestack[tstack_p].start = start;
            tracestack[tstack_p].end = end;
            tracestack[tstack_p].planeNum = 0;
            tracestack[tstack_p].nodeNum = 1;		//start with the root of the tree
            tstack_p++;

            while (true) {

                tstack_p--;
                // if the trace stack is empty
                if (tstack_p < 0) {
                    if (NOT(trace.lastAreaNum)) {
                        // completely in solid
                        trace.fraction = 0.0f;
                        trace.endpos = start;
                    } else {
                        // nothing was hit
                        trace.fraction = 1.0f;
                        trace.endpos = end;
                    }
                    trace.planeNum = 0;
                    return false;
                }

                // number of the current node to test the line against
                nodeNum = tracestack[tstack_p].nodeNum;

                // if it is an area
                if (nodeNum < 0) {
                    // if can't enter the area
                    if (((areas.oGet(-nodeNum).flags & trace.flags) != 0) || ((areas.oGet(-nodeNum).travelFlags & trace.travelFlags) != 0)) {
                        if (NOT(trace.lastAreaNum)) {
                            trace.fraction = 0.0f;
                            v1 = vec3_origin;
                        } else {
                            v1 = end.oMinus(start);
                            v2 = tracestack[tstack_p].start.oMinus(start);
                            trace.fraction = v2.Length() / v1.Length();
                        }
                        trace.endpos = tracestack[tstack_p].start;
                        trace.blockingAreaNum = -nodeNum;
                        trace.planeNum = tracestack[tstack_p].planeNum;
                        // always take the plane with normal facing towards the trace start
                        plane = planeList.oGet(trace.planeNum);
                        if (v1.oMultiply(plane.Normal()) > 0.0f) {
                            trace.planeNum ^= 1;
                        }
                        return true;
                    }
                    trace.lastAreaNum = -nodeNum;
                    if (trace.numAreas < trace.maxAreas) {
                        if (trace.areas != null) {
                            trace.areas[trace.numAreas] = -nodeNum;
                        }
                        if (trace.points != null) {
                            trace.points[trace.numAreas] = tracestack[tstack_p].start;
                        }
                        trace.numAreas++;
                    }
                    continue;
                }

                // if it is a solid leaf
                if (0 == nodeNum) {
                    if (0 == trace.lastAreaNum) {
                        trace.fraction = 0.0f;
                        v1 = vec3_origin;
                    } else {
                        v1 = end.oMinus(start);
                        v2 = tracestack[tstack_p].start.oMinus(start);
                        trace.fraction = v2.Length() / v1.Length();
                    }
                    trace.endpos = tracestack[tstack_p].start;
                    trace.blockingAreaNum = 0;	// hit solid leaf
                    trace.planeNum = tracestack[tstack_p].planeNum;
                    // always take the plane with normal facing towards the trace start
                    plane = planeList.oGet(trace.planeNum);
                    if (v1.oMultiply(plane.Normal()) > 0.0f) {
                        trace.planeNum ^= 1;
                    }
                    if (0 == trace.lastAreaNum && trace.getOutOfSolid != 0) {
                        continue;
                    } else {
                        return true;
                    }
                }

                // the node to test against
                node = nodes.oGet(nodeNum);
                // start point of current line to test against node
                cur_start = tracestack[tstack_p].start;
                // end point of the current line to test against node
                cur_end = tracestack[tstack_p].end;
                // the current node plane
                plane = planeList.oGet(node.planeNum);

                front = plane.Distance(cur_start);
                back = plane.Distance(cur_end);

                // if the whole to be traced line is totally at the front of this node
                // only go down the tree with the front child
                if (front >= -ON_EPSILON && back >= -ON_EPSILON) {
                    // keep the current start and end point on the stack and go down the tree with the front child
                    tracestack[tstack_p].nodeNum = node.children[0];
                    tstack_p++;
                    if (tstack_p >= MAX_AAS_TREE_DEPTH) {//TODO:check that pointer to address comparison is the same as this.
                        common.Error("idAASFileLocal::Trace: stack overflow\n");
                        return false;
                    }
                } // if the whole to be traced line is totally at the back of this node
                // only go down the tree with the back child
                else if (front < ON_EPSILON && back < ON_EPSILON) {
                    // keep the current start and end point on the stack and go down the tree with the back child
                    tracestack[tstack_p].nodeNum = node.children[1];
                    tstack_p++;
                    if (tstack_p >= MAX_AAS_TREE_DEPTH) {
                        common.Error("idAASFileLocal::Trace: stack overflow\n");
                        return false;
                    }
                } // go down the tree both at the front and back of the node
                else {
                    tmpPlaneNum = tracestack[tstack_p].planeNum;
                    // calculate the hit point with the node plane
                    // put the cross point TRACEPLANE_EPSILON on the near side
                    if (front < 0) {
                        frac = (front + TRACEPLANE_EPSILON) / (front - back);
                    } else {
                        frac = (front - TRACEPLANE_EPSILON) / (front - back);
                    }

                    if (frac < 0) {
                        frac = 0.001f; //0
                    } else if (frac > 1) {
                        frac = 0.999f; //1
                    }

                    cur_mid = cur_start.oPlus((cur_end.oMinus(cur_start)).oMultiply((float) frac));//TODO:downcast?

                    // side the front part of the line is on
                    side = front < 0 ? 1 : 0;

                    // first put the end part of the line on the stack (back side)
                    tracestack[tstack_p].start = cur_mid;
                    tracestack[tstack_p].planeNum = node.planeNum;
                    tracestack[tstack_p].nodeNum = node.children[/*!side*/1 ^ side];
                    tstack_p++;
                    if (tstack_p >= MAX_AAS_TREE_DEPTH) {
                        common.Error("idAASFileLocal::Trace: stack overflow\n");
                        return false;
                    }
                    // now put the part near the start of the line on the stack so we will
                    // continue with that part first.
                    tracestack[tstack_p].start = cur_start;
                    tracestack[tstack_p].end = cur_mid;
                    tracestack[tstack_p].planeNum = tmpPlaneNum;
                    tracestack[tstack_p].nodeNum = node.children[side];
                    tstack_p++;
                    if (tstack_p >= MAX_AAS_TREE_DEPTH) {
                        common.Error("idAASFileLocal::Trace: stack overflow\n");
                        return false;
                    }
                }
            }
//            return false;
        }

        @Override
        public void PrintInfo() {
            common.Printf("%6d KB file size\n", MemorySize() >> 10);
            common.Printf("%6d areas\n", areas.Num());
            common.Printf("%6d max tree depth\n", MaxTreeDepth());
            ReportRoutingEfficiency();
        }

        public boolean Load(final idStr fileName, long/*unsigned int*/ mapFileCRC) {
            idLexer src = new idLexer(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGESCAPECHARS | LEXFL_NOSTRINGCONCAT | LEXFL_ALLOWPATHNAMES);
            idToken token = new idToken();
            int depth;
            long/*unsigned int*/ c;

            name = fileName;
            crc = mapFileCRC;

            common.Printf("[Load AAS]\n");
            common.Printf("loading %s\n", name);

            if (!src.LoadFile(name)) {
                return false;
            }

            if (!src.ExpectTokenString(AAS_FILEID)) {
                common.Warning("Not an AAS file: '%s'", name);
                return false;
            }

            if (!src.ReadToken(token) || !token.equals(AAS_FILEVERSION)) {
                common.Warning("AAS file '%s' has version %s instead of %s", name, token, AAS_FILEVERSION);
                return false;
            }

            if (0 == src.ExpectTokenType(TT_NUMBER, TT_INTEGER, token)) {
                common.Warning("AAS file '%s' has no map file CRC", name);
                return false;
            }

            c = token.GetUnsignedLongValue();
            if (mapFileCRC != 0 && c != mapFileCRC) {
                common.Warning("AAS file '%s' is out of date", name);
                return false;
            }

            // clear the file in memory
            Clear();

            // parse the file
            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("settings")) {
                    if (!settings.FromParser(src)) {
                        return false;
                    }
                } else if (token.equals("planes")) {
                    if (!ParsePlanes(src)) {
                        return false;
                    }
                } else if (token.equals("vertices")) {
                    if (!ParseVertices(src)) {
                        return false;
                    }
                } else if (token.equals("edges")) {
                    if (!ParseEdges(src)) {
                        return false;
                    }
                } else if (token.equals("edgeIndex")) {
                    if (!ParseIndex(src, edgeIndex)) {
                        return false;
                    }
                } else if (token.equals("faces")) {
                    if (!ParseFaces(src)) {
                        return false;
                    }
                } else if (token.equals("faceIndex")) {
                    if (!ParseIndex(src, faceIndex)) {
                        return false;
                    }
                } else if (token.equals("areas")) {
                    if (!ParseAreas(src)) {
                        return false;
                    }
                } else if (token.equals("nodes")) {
                    if (!ParseNodes(src)) {
                        return false;
                    }
                } else if (token.equals("portals")) {
                    if (!ParsePortals(src)) {
                        return false;
                    }
                } else if (token.equals("portalIndex")) {
                    if (!ParseIndex(src, portalIndex)) {
                        return false;
                    }
                } else if (token.equals("clusters")) {
                    if (!ParseClusters(src)) {
                        return false;
                    }
                } else {
                    src.Error("idAASFileLocal::Load: bad token \"%s\"", token);
                    return false;
                }
            }

            FinishAreas();

            depth = MaxTreeDepth();
            if (depth > MAX_AAS_TREE_DEPTH) {
                src.Error("idAASFileLocal::Load: tree depth = %d", depth);
            }

            common.Printf("done.\n");

            return true;
        }

        public boolean Write(final idStr fileName, long /*unsigned int*/ mapFileCRC) {
            int i, num;
            idFile aasFile;
            idReachability reach;

            common.Printf("[Write AAS]\n");
            common.Printf("writing %s\n", fileName);

            name = fileName;
            crc = mapFileCRC;

            aasFile = fileSystem.OpenFileWrite(fileName.toString(), "fs_devpath");
            if (NOT(aasFile)) {
                common.Error("Error opening %s", fileName);
                return false;
            }

            aasFile.WriteFloatString("%s \"%s\"\n\n", AAS_FILEID, AAS_FILEVERSION);
            aasFile.WriteFloatString("%u\n\n", mapFileCRC);

            // write out the settings
            aasFile.WriteFloatString("settings\n");
            settings.WriteToFile(aasFile);

            // write out planes
            aasFile.WriteFloatString("planes %d {\n", planeList.Num());
            for (i = 0; i < planeList.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %f %f %f %f )\n", i,
                        planeList.oGet(i).Normal().x, planeList.oGet(i).Normal().y, planeList.oGet(i).Normal().z, planeList.oGet(i).Dist());
            }
            aasFile.WriteFloatString("}\n");

            // write out vertices
            aasFile.WriteFloatString("vertices %d {\n", vertices.Num());
            for (i = 0; i < vertices.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %f %f %f )\n", i, vertices.oGet(i).x, vertices.oGet(i).y, vertices.oGet(i).z);
            }
            aasFile.WriteFloatString("}\n");

            // write out edges
            aasFile.WriteFloatString("edges %d {\n", edges.Num());
            for (i = 0; i < edges.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %d %d )\n", i, edges.oGet(i).vertexNum[0], edges.oGet(i).vertexNum[1]);
            }
            aasFile.WriteFloatString("}\n");

            // write out edgeIndex
            aasFile.WriteFloatString("edgeIndex %d {\n", edgeIndex.Num());
            for (i = 0; i < edgeIndex.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %d )\n", i, edgeIndex.oGet(i));
            }
            aasFile.WriteFloatString("}\n");

            // write out faces
            aasFile.WriteFloatString("faces %d {\n", faces.Num());
            for (i = 0; i < faces.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %d %d %d %d %d %d )\n", i, faces.oGet(i).planeNum, faces.oGet(i).flags,
                        faces.oGet(i).areas[0], faces.oGet(i).areas[1], faces.oGet(i).firstEdge, faces.oGet(i).numEdges);
            }
            aasFile.WriteFloatString("}\n");

            // write out faceIndex
            aasFile.WriteFloatString("faceIndex %d {\n", faceIndex.Num());
            for (i = 0; i < faceIndex.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %d )\n", i, faceIndex.oGet(i));
            }
            aasFile.WriteFloatString("}\n");

            // write out areas
            aasFile.WriteFloatString("areas %d {\n", areas.Num());
            for (i = 0; i < areas.Num(); i++) {
                for (num = 0, reach = areas.oGet(i).reach; reach != null; reach = reach.next) {
                    num++;
                }
                aasFile.WriteFloatString("\t%d ( %d %d %d %d %d %d ) %d {\n", i, areas.oGet(i).flags, areas.oGet(i).contents,
                        areas.oGet(i).firstFace, areas.oGet(i).numFaces, areas.oGet(i).cluster, areas.oGet(i).clusterAreaNum, num);
                for (reach = areas.oGet(i).reach; reach != null; reach = reach.next) {
                    Reachability_Write(aasFile, reach);
//                    switch (reach.travelType) {
//                        case TFL_SPECIAL:
//                            Reachability_Special_Write(aasFile, (idReachability_Special) reach);
//                            break;
//                    }
                    if (reach.travelType == TFL_SPECIAL) {
                        Reachability_Special_Write(aasFile, (idReachability_Special) reach);
                    }
                    aasFile.WriteFloatString("\n");
                }
                aasFile.WriteFloatString("\t}\n");
            }
            aasFile.WriteFloatString("}\n");

            // write out nodes
            aasFile.WriteFloatString("nodes %d {\n", nodes.Num());
            for (i = 0; i < nodes.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %d %d %d )\n", i, nodes.oGet(i).planeNum, nodes.oGet(i).children[0], nodes.oGet(i).children[1]);
            }
            aasFile.WriteFloatString("}\n");

            // write out portals
            aasFile.WriteFloatString("portals %d {\n", portals.Num());
            for (i = 0; i < portals.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %d %d %d %d %d )\n", i, portals.oGet(i).areaNum, portals.oGet(i).clusters[0],
                        portals.oGet(i).clusters[1], portals.oGet(i).clusterAreaNum[0], portals.oGet(i).clusterAreaNum[1]);
            }
            aasFile.WriteFloatString("}\n");

            // write out portalIndex
            aasFile.WriteFloatString("portalIndex %d {\n", portalIndex.Num());
            for (i = 0; i < portalIndex.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %d )\n", i, portalIndex.oGet(i));
            }
            aasFile.WriteFloatString("}\n");

            // write out clusters
            aasFile.WriteFloatString("clusters %d {\n", clusters.Num());
            for (i = 0; i < clusters.Num(); i++) {
                aasFile.WriteFloatString("\t%d ( %d %d %d %d )\n", i, clusters.oGet(i).numAreas, clusters.oGet(i).numReachableAreas,
                        clusters.oGet(i).firstPortal, clusters.oGet(i).numPortals);
            }
            aasFile.WriteFloatString("}\n");

            // close file
            fileSystem.CloseFile(aasFile);

            common.Printf("done.\n");

            return true;
        }

        public int MemorySize() {
            int size;

            size = planeList.Size();
            size += vertices.Size();
            size += edges.Size();
            size += edgeIndex.Size();
            size += faces.Size();
            size += faceIndex.Size();
            size += areas.Size();
            size += nodes.Size();
            size += portals.Size();
            size += portalIndex.Size();
            size += clusters.Size();
//	size += sizeof( idReachability_Walk ) * NumReachabilities();
            size += NumReachabilities();

            return size;
        }

        public void ReportRoutingEfficiency() {
            int numReachableAreas, total, i, n;

            numReachableAreas = 0;
            total = 0;
            for (i = 0; i < clusters.Num(); i++) {
                n = clusters.oGet(i).numReachableAreas;
                numReachableAreas += n;
                total += n * n;
            }
            total += numReachableAreas * portals.Num();

            common.Printf("%6d reachable areas\n", numReachableAreas);
            common.Printf("%6d reachabilities\n", NumReachabilities());
            common.Printf("%6d KB max routing cache\n", (total * 3) >> 10);
        }

        public void Optimize() {
            int i, j, k, faceNum, edgeNum, areaFirstFace, faceFirstEdge;
            aasArea_s area;
            aasFace_s face;
            aasEdge_s edge;
            idReachability reach;
            idList<Integer> vertexRemap = new idList<>();
            idList<Integer> edgeRemap = new idList<>();
            idList<Integer> faceRemap = new idList<>();
            idList<idVec3/*aasVertex_t*/> newVertices = new idList<>();
            idList<aasEdge_s> newEdges = new idList<>();
            idList<Integer/*aasIndex_t*/> newEdgeIndex = new idList<>();
            idList<aasFace_s> newFaces = new idList<>();
            idList<Integer/*aasIndex_t*/> newFaceIndex = new idList<>();

            vertexRemap.AssureSize(vertices.Num(), -1);
            edgeRemap.AssureSize(edges.Num(), 0);
            faceRemap.AssureSize(faces.Num(), 0);

            newVertices.Resize(vertices.Num());
            newEdges.Resize(edges.Num());
            newEdges.SetNum(1, false);
            newEdgeIndex.Resize(edgeIndex.Num());
            newFaces.Resize(faces.Num());
            newFaces.SetNum(1, false);
            newFaceIndex.Resize(faceIndex.Num());

            for (i = 0; i < areas.Num(); i++) {
                area = areas.oGet(i);

                areaFirstFace = newFaceIndex.Num();
                for (j = 0; j < area.numFaces; j++) {
                    faceNum = faceIndex.oGet(area.firstFace + j);
                    face = faces.oGet(abs(faceNum));

                    // store face
                    if (NOT(faceRemap.oGet(abs(faceNum)))) {
                        faceRemap.oSet(abs(faceNum), newFaces.Num());
                        newFaces.Append(face);

                        // don't store edges for faces we don't care about
                        if (0 == (face.flags & (FACE_FLOOR | FACE_LADDER))) {

                            newFaces.oGet(newFaces.Num() - 1).firstEdge = 0;
                            newFaces.oGet(newFaces.Num() - 1).numEdges = 0;

                        } else {

                            // store edges
                            faceFirstEdge = newEdgeIndex.Num();
                            for (k = 0; k < face.numEdges; k++) {
                                edgeNum = edgeIndex.oGet(face.firstEdge + k);
                                edge = edges.oGet(abs(edgeNum));

                                if (NOT(edgeRemap.oGet(abs(edgeNum)))) {
                                    if (edgeNum < 0) {
                                        edgeRemap.oSet(abs(edgeNum), -newEdges.Num());
                                    } else {
                                        edgeRemap.oSet(abs(edgeNum), newEdges.Num());
                                    }

                                    // remap vertices if not yet remapped
                                    if (vertexRemap.oGet(edge.vertexNum[0]) == -1) {
                                        vertexRemap.oSet(edge.vertexNum[0], newVertices.Num());
                                        newVertices.Append(vertices.oGet(edge.vertexNum[0]));
                                    }
                                    if (vertexRemap.oGet(edge.vertexNum[1]) == -1) {
                                        vertexRemap.oSet(edge.vertexNum[1], newVertices.Num());
                                        newVertices.Append(vertices.oGet(edge.vertexNum[1]));
                                    }

                                    newEdges.Append(edge);
                                    newEdges.oGet(newEdges.Num() - 1).vertexNum[0] = vertexRemap.oGet(edge.vertexNum[0]);
                                    newEdges.oGet(newEdges.Num() - 1).vertexNum[1] = vertexRemap.oGet(edge.vertexNum[1]);
                                }

                                newEdgeIndex.Append(edgeRemap.oGet(abs(edgeNum)));
                            }

                            newFaces.oGet(newFaces.Num() - 1).firstEdge = faceFirstEdge;
                            newFaces.oGet(newFaces.Num() - 1).numEdges = newEdgeIndex.Num() - faceFirstEdge;
                        }
                    }

                    if (faceNum < 0) {
                        newFaceIndex.Append(-faceRemap.oGet(abs(faceNum)));
                    } else {
                        newFaceIndex.Append(faceRemap.oGet(abs(faceNum)));
                    }
                }

                area.firstFace = areaFirstFace;
                area.numFaces = newFaceIndex.Num() - areaFirstFace;

                // remap the reachability edges
                for (reach = area.reach; reach != null; reach = reach.next) {
                    reach.edgeNum = abs(edgeRemap.oGet(reach.edgeNum));
                }
            }

            // store new list
            vertices = newVertices;
            edges = newEdges;
            edgeIndex = newEdgeIndex;
            faces = newFaces;
            faceIndex = newFaceIndex;
        }

        public void LinkReversedReachability() {
            int i;
            idReachability reach;

            // link reversed reachabilities
            for (i = 0; i < areas.Num(); i++) {
                for (reach = areas.oGet(i).reach; reach != null; reach = reach.next) {
                    reach.rev_next = areas.oGet(reach.toAreaNum).rev_reach;
                    areas.oGet(reach.toAreaNum).rev_reach = reach;
                }
            }
        }

        public void FinishAreas() {
            int i;

            for (i = 0; i < areas.Num(); i++) {
                areas.oGet(i).center = AreaReachableGoal(i);
                areas.oGet(i).bounds = AreaBounds(i);
            }
        }

        public void Clear() {
            planeList.Clear();
            vertices.Clear();
            edges.Clear();
            edgeIndex.Clear();
            faces.Clear();
            faceIndex.Clear();
            areas.Clear();
            nodes.Clear();
            portals.Clear();
            portalIndex.Clear();
            clusters.Clear();
        }

        public void DeleteReachabilities() {
            int i;
            idReachability reach, nextReach;

            for (i = 0; i < areas.Num(); i++) {
                for (reach = areas.oGet(i).reach; reach != null; reach = nextReach) {
                    nextReach = reach.next;
//			delete reach;
//                    reach = null;
                }
                areas.oGet(i).reach = null;
                areas.oGet(i).rev_reach = null;
            }
        }

        public void DeleteClusters() {
            aasPortal_s portal;
            aasCluster_s cluster;

            portals.Clear();
            portalIndex.Clear();
            clusters.Clear();

            // first portal is a dummy
//	memset( &portal, 0, sizeof( portal ) );
            portal = new aasPortal_s();
            portals.Append(portal);

            // first cluster is a dummy
//	memset( &cluster, 0, sizeof( portal ) );
            cluster = new aasCluster_s();
            clusters.Append(cluster);
        }

        private boolean ParseIndex(idLexer src, idList<Integer/*aasIndex_t*/> indexes) {
            int numIndexes, i;
            int/*aasIndex_s*/ index;

            numIndexes = src.ParseInt();
            indexes.Resize(numIndexes);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numIndexes; i++) {
                src.ParseInt();
                src.ExpectTokenString("(");
                index = src.ParseInt();
                src.ExpectTokenString(")");
                indexes.Append(index);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }
            return true;
        }

        private boolean ParsePlanes(idLexer src) {
            int numPlanes, i;
            idPlane plane = new idPlane();
            idVec4 vec = new idVec4();

            numPlanes = src.ParseInt();
            planeList.Resize(numPlanes);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numPlanes; i++) {
                src.ParseInt();
                if (!src.Parse1DMatrix(4, vec)) {
                    return false;
                }
                plane.SetNormal(vec.ToVec3());
                plane.SetDist(vec.oGet(3));
                planeList.Append(plane);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }
            return true;
        }

        private boolean ParseVertices(idLexer src) {
            int numVertices, i;
            idVec3 vec = new idVec3();

            numVertices = src.ParseInt();
            vertices.Resize(numVertices);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numVertices; i++) {
                src.ParseInt();
                if (!src.Parse1DMatrix(3, vec)) {
                    return false;
                }
                vertices.Append(vec);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }
            return true;
        }

        private boolean ParseEdges(idLexer src) {
            int numEdges, i;
            aasEdge_s edge = new aasEdge_s();

            numEdges = src.ParseInt();
            edges.Resize(numEdges);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numEdges; i++) {
                src.ParseInt();
                src.ExpectTokenString("(");
                edge.vertexNum[0] = src.ParseInt();
                edge.vertexNum[1] = src.ParseInt();
                src.ExpectTokenString(")");
                edges.Append(edge);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }
            return true;
        }

        private boolean ParseFaces(idLexer src) {
            int numFaces, i;
            aasFace_s face = new aasFace_s();

            numFaces = src.ParseInt();
            faces.Resize(numFaces);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numFaces; i++) {
                src.ParseInt();
                src.ExpectTokenString("(");
                face.planeNum = src.ParseInt();
                face.flags = src.ParseInt();
                face.areas[0] = (short) src.ParseInt();
                face.areas[1] = (short) src.ParseInt();
                face.firstEdge = src.ParseInt();
                face.numEdges = src.ParseInt();
                src.ExpectTokenString(")");
                faces.Append(face);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }
            return true;
        }

        private boolean ParseReachabilities(idLexer src, int areaNum) {
            int num, j;
            aasArea_s area;
            idReachability reach = new idReachability(), newReach;
            idReachability_Special special;

            area = areas.oGet(areaNum);

            num = src.ParseInt();
            src.ExpectTokenString("{");
            area.reach = null;
            area.rev_reach = null;
            area.travelFlags = AreaContentsTravelFlags(areaNum);
            for (j = 0; j < num; j++) {
                Reachability_Read(src, reach);
//		switch( reach.travelType ) {
//			case TFL_SPECIAL:
//				newReach = special = new idReachability_Special();
//				Reachability_Special_Read( src, special );
//				break;
//			default:
//				newReach = new idReachability();
//				break;
//		}
                if (reach.travelType == TFL_SPECIAL) {
                    newReach = special = new idReachability_Special();
                    Reachability_Special_Read(src, special);
                } else {
                    newReach = new idReachability();
                }
                newReach.CopyBase(reach);
                newReach.fromAreaNum = (short) areaNum;
                newReach.next = area.reach;
                area.reach = newReach;
            }
            src.ExpectTokenString("}");
            return true;
        }

        private boolean ParseAreas(idLexer src) {
            int numAreas, i;
            aasArea_s area = new aasArea_s();

            numAreas = src.ParseInt();
            areas.Resize(numAreas);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numAreas; i++) {
                src.ParseInt();
                src.ExpectTokenString("(");
                area.flags = src.ParseInt();
                area.contents = src.ParseInt();
                area.firstFace = src.ParseInt();
                area.numFaces = src.ParseInt();
                area.cluster = (short) src.ParseInt();
                area.clusterAreaNum = (short) src.ParseInt();
                src.ExpectTokenString(")");
                areas.Append(area);
                ParseReachabilities(src, i);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }

            LinkReversedReachability();

            return true;
        }

        private boolean ParseNodes(idLexer src) {
            int numNodes, i;
            aasNode_s node = new aasNode_s();

            numNodes = src.ParseInt();
            nodes.Resize(numNodes);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numNodes; i++) {
                src.ParseInt();
                src.ExpectTokenString("(");
                node.planeNum = src.ParseInt();
                node.children[0] = src.ParseInt();
                node.children[1] = src.ParseInt();
                src.ExpectTokenString(")");
                nodes.Append(node);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }
            return true;
        }

        private boolean ParsePortals(idLexer src) {
            int numPortals, i;
            aasPortal_s portal = new aasPortal_s();

            numPortals = src.ParseInt();
            portals.Resize(numPortals);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numPortals; i++) {
                src.ParseInt();
                src.ExpectTokenString("(");
                portal.areaNum = (short) src.ParseInt();
                portal.clusters[0] = (short) src.ParseInt();
                portal.clusters[1] = (short) src.ParseInt();
                portal.clusterAreaNum[0] = (short) src.ParseInt();
                portal.clusterAreaNum[1] = (short) src.ParseInt();
                src.ExpectTokenString(")");
                portals.Append(portal);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }
            return true;
        }

        private boolean ParseClusters(idLexer src) {
            int numClusters, i;
            aasCluster_s cluster = new aasCluster_s();

            numClusters = src.ParseInt();
            clusters.Resize(numClusters);
            if (!src.ExpectTokenString("{")) {
                return false;
            }
            for (i = 0; i < numClusters; i++) {
                src.ParseInt();
                src.ExpectTokenString("(");
                cluster.numAreas = src.ParseInt();
                cluster.numReachableAreas = src.ParseInt();
                cluster.firstPortal = src.ParseInt();
                cluster.numPortals = src.ParseInt();
                src.ExpectTokenString(")");
                clusters.Append(cluster);
            }
            if (!src.ExpectTokenString("}")) {
                return false;
            }
            return true;
        }

        private int BoundsReachableAreaNum_r(int nodeNum, final idBounds bounds, final int areaFlags, final int excludeTravelFlags) {
            int res;
            aasNode_s node;

            while (nodeNum != 0) {
                if (nodeNum < 0) {
                    if (((areas.oGet(-nodeNum).flags & areaFlags) != 0) && ((areas.oGet(-nodeNum).travelFlags & excludeTravelFlags) == 0)) {
                        return -nodeNum;
                    }
                    return 0;
                }
                node = nodes.oGet(nodeNum);
                res = bounds.PlaneSide(planeList.oGet(node.planeNum));
                if (res == PLANESIDE_BACK) {
                    nodeNum = node.children[1];
                } else if (res == PLANESIDE_FRONT) {
                    nodeNum = node.children[0];
                } else {
                    nodeNum = BoundsReachableAreaNum_r(node.children[1], bounds, areaFlags, excludeTravelFlags);
                    if (nodeNum != 0) {
                        return nodeNum;
                    }
                    nodeNum = node.children[0];
                }
            }

            return 0;
        }

        private void MaxTreeDepth_r(int nodeNum, int[] depth, int[] maxDepth) {
            final aasNode_s node;

            if (nodeNum <= 0) {
                return;
            }

            depth[0]++;
            if (depth[0] > maxDepth[0]) {
                maxDepth = depth;
            }

            node = nodes.oGet(nodeNum);
            MaxTreeDepth_r(node.children[0], depth, maxDepth);
            MaxTreeDepth_r(node.children[1], depth, maxDepth);

            depth[0]--;
        }

        private int MaxTreeDepth() {
            int[] depth = new int[1], maxDepth = new int[1];

//	depth = maxDepth = 0;
            MaxTreeDepth_r(1, depth, maxDepth);
            return maxDepth[0];
        }

        private int AreaContentsTravelFlags(int areaNum) {
            if ((areas.oGet(areaNum).contents & AREACONTENTS_WATER) != 0) {
                return TFL_WATER;
            }
            return TFL_AIR;
        }

        private idVec3 AreaReachableGoal(int areaNum) {
            int i, faceNum, numFaces;
            final aasArea_s area;
            idVec3 center;
            idVec3 start, end;
            aasTrace_s trace = new aasTrace_s();

            area = areas.oGet(areaNum);

            if (0 == (area.flags & (AREA_REACHABLE_WALK | AREA_REACHABLE_FLY)) || (area.flags & AREA_LIQUID) != 0) {
                return AreaCenter(areaNum);
            }

            center = vec3_origin;

            numFaces = 0;
            for (i = 0; i < area.numFaces; i++) {
                faceNum = faceIndex.oGet(area.firstFace + i);
                if (0 == (faces.oGet(abs(faceNum)).flags & FACE_FLOOR)) {
                    continue;
                }
                center.oPluSet(FaceCenter(abs(faceNum)));
                numFaces++;
            }
            if (numFaces > 0) {
                center.oDivSet(numFaces);
            }
            center.oPluSet(2, 1.0f);
            end = center;
            end.oMinSet(2, 1024);
            Trace(trace, center, end);

            return trace.endpos;
        }

        private int NumReachabilities() {
            int i, num;
            idReachability reach;

            num = 0;
            for (i = 0; i < areas.Num(); i++) {
                for (reach = areas.oGet(i).reach; reach != null; reach = reach.next) {
                    num++;
                }
            }
            return num;
        }
    };

    public static class aasTraceStack_s {

        idVec3 start;
        idVec3 end;
        int    planeNum;
        int    nodeNum;

        public aasTraceStack_s() {
            start = new idVec3();
            end = new idVec3();
        }
    };
}
