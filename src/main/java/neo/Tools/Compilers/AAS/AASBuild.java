package neo.Tools.Compilers.AAS;

import static java.lang.Math.abs;
import static neo.Renderer.Material.CONTENTS_AAS_OBSTACLE;
import static neo.Renderer.Material.CONTENTS_AAS_SOLID;
import static neo.Renderer.Material.CONTENTS_AREAPORTAL;
import static neo.Renderer.Material.CONTENTS_MONSTERCLIP;
import static neo.Renderer.Material.CONTENTS_SOLID;
import static neo.Renderer.Material.CONTENTS_WATER;
import static neo.Renderer.RenderWorld.PROC_FILE_EXT;
import static neo.Renderer.RenderWorld.PROC_FILE_ID;
import static neo.TempDump.NOT;
import static neo.Tools.Compilers.AAS.AASBuild_File.AAS_PLANE_DIST_EPSILON;
import static neo.Tools.Compilers.AAS.AASBuild_File.AAS_PLANE_NORMAL_EPSILON;
import static neo.Tools.Compilers.AAS.AASBuild_File.EDGE_HASH_SIZE;
import static neo.Tools.Compilers.AAS.AASBuild_File.INTEGRAL_EPSILON;
import static neo.Tools.Compilers.AAS.AASBuild_File.VERTEX_EPSILON;
import static neo.Tools.Compilers.AAS.AASBuild_File.VERTEX_HASH_BOXSIZE;
import static neo.Tools.Compilers.AAS.AASBuild_File.VERTEX_HASH_SIZE;
import static neo.Tools.Compilers.AAS.AASBuild_File.aas_edgeHash;
import static neo.Tools.Compilers.AAS.AASBuild_File.aas_vertexBounds;
import static neo.Tools.Compilers.AAS.AASBuild_File.aas_vertexHash;
import static neo.Tools.Compilers.AAS.AASBuild_File.aas_vertexShift;
import static neo.Tools.Compilers.AAS.AASBuild_ledge.LEDGE_EPSILON;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_BBOX_BIT;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_CLUSTERPORTAL;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_OBSTACLE;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_SOLID;
import static neo.Tools.Compilers.AAS.AASFile.AREACONTENTS_WATER;
import static neo.Tools.Compilers.AAS.AASFile.AREA_FLOOR;
import static neo.Tools.Compilers.AAS.AASFile.AREA_GAP;
import static neo.Tools.Compilers.AAS.AASFile.AREA_LEDGE;
import static neo.Tools.Compilers.AAS.AASFile.FACE_FLOOR;
import static neo.Tools.Compilers.AAS.AASFile.FACE_SOLID;
import static neo.Tools.Compilers.AAS.Brush.DisplayRealTimeString;
import static neo.Tools.Compilers.AAS.Brush.SFL_USED_SPLITTER;
import static neo.Tools.Compilers.AAS.BrushBSP.NODE_DONE;
import static neo.Tools.Compilers.AAS.BrushBSP.NODE_VISITED;
import static neo.framework.Common.EDITOR_AAS;
import static neo.framework.Common.com_editors;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.BIT;
import static neo.idlib.MapFile.DEFAULT_CURVE_MAX_ERROR_CD;
import static neo.idlib.MapFile.DEFAULT_CURVE_MAX_LENGTH_CD;
import static neo.idlib.Text.Lexer.LEXFL_NODOLLARPRECOMPILE;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.geometry.Winding.MAX_POINTS_ON_WINDING;
import static neo.idlib.math.Math_h.INTSIGNBITNOTSET;
import static neo.idlib.math.Plane.DEGENERATE_DIST_EPSILON;
import static neo.idlib.math.Plane.ON_EPSILON;
import static neo.idlib.math.Plane.SIDE_BACK;
import static neo.idlib.math.Plane.SIDE_CROSS;
import static neo.idlib.math.Plane.SIDE_FRONT;
import static neo.idlib.math.Plane.SIDE_ON;
import static neo.sys.win_shared.Sys_Milliseconds;

import neo.Game.GameEdit;
import neo.Renderer.Material.idMaterial;
import neo.Tools.Compilers.AAS.AASBuild_File.sizeEstimate_s;
import neo.Tools.Compilers.AAS.AASBuild_ledge.idLedge;
import neo.Tools.Compilers.AAS.AASBuild_local.aasProcNode_s;
import neo.Tools.Compilers.AAS.AASCluster.idAASCluster;
import neo.Tools.Compilers.AAS.AASFile.aasArea_s;
import neo.Tools.Compilers.AAS.AASFile.aasEdge_s;
import neo.Tools.Compilers.AAS.AASFile.aasFace_s;
import neo.Tools.Compilers.AAS.AASFile.aasNode_s;
import neo.Tools.Compilers.AAS.AASFile.idAASSettings;
import neo.Tools.Compilers.AAS.AASFile_local.idAASFileLocal;
import neo.Tools.Compilers.AAS.AASReach.idAASReach;
import neo.Tools.Compilers.AAS.Brush.idBrush;
import neo.Tools.Compilers.AAS.Brush.idBrushList;
import neo.Tools.Compilers.AAS.Brush.idBrushMap;
import neo.Tools.Compilers.AAS.Brush.idBrushSide;
import neo.Tools.Compilers.AAS.BrushBSP.idBrushBSP;
import neo.Tools.Compilers.AAS.BrushBSP.idBrushBSPNode;
import neo.Tools.Compilers.AAS.BrushBSP.idBrushBSPPortal;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.FileSystem_h.idFileList;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Lib.idException;
import neo.idlib.MapFile.idMapBrush;
import neo.idlib.MapFile.idMapBrushSide;
import neo.idlib.MapFile.idMapEntity;
import neo.idlib.MapFile.idMapFile;
import neo.idlib.MapFile.idMapPatch;
import neo.idlib.MapFile.idMapPrimitive;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.PlaneSet.idPlaneSet;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.geometry.Surface_Patch.idSurface_Patch;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class AASBuild {

    static final int BFL_PATCH = 0x1000;

    static abstract class Allowance {

        public abstract boolean run(idBrush b1, idBrush b2);
    }

    //===============================================================
    //
    //	idAASBuild
    //
    //===============================================================
    static class idAASBuild {

        private idAASSettings aasSettings;
        private idAASFileLocal file;
        private aasProcNode_s[] procNodes;
        private int numProcNodes;
        private int numGravitationalSubdivisions;
        private int numMergedLeafNodes;
        private int numLedgeSubdivisions;
        private idList<idLedge> ledgeList;
        private idBrushMap ledgeMap;
        //
        //

        public idAASBuild() {
            this.file = null;
            this.procNodes = null;
            this.numProcNodes = 0;
            this.numGravitationalSubdivisions = 0;
            this.numMergedLeafNodes = 0;
            this.numLedgeSubdivisions = 0;
            this.ledgeMap = null;
        }
        // ~idAASBuild();//TODO:deconstructors?

        public boolean Build(final idStr fileName, final idAASSettings settings) {
            int i, bit, mask, startTime;
            idMapFile mapFile;
            idBrushList brushList = new idBrushList();
            final idList<idBrushList> expandedBrushes = new idList<>();
            idBrush b;
            final idBrushBSP bsp = new idBrushBSP();
            idStr name;
            final idAASReach reach = new idAASReach();
            final idAASCluster cluster = new idAASCluster();
            final idStrList entityClassNames = new idStrList();

            startTime = Sys_Milliseconds();

            Shutdown();

            this.aasSettings = settings;

            name = fileName;
            name.SetFileExtension("map");

            mapFile = new idMapFile();
            if (!mapFile.Parse(name.getData())) {
//		delete mapFile;
                common.Error("Couldn't load map file: '%s'", name);
                return false;
            }

            // check if this map has any entities that use this AAS file
            if (!CheckForEntities(mapFile, entityClassNames)) {
//		delete mapFile;
                common.Printf("no entities in map that use %s\n", settings.fileExtension);
                return true;
            }

            // load map file brushes
            brushList = AddBrushesForMapFile(mapFile, brushList);

            // if empty map
            if (brushList.Num() == 0) {
//		delete mapFile;
                common.Error("%s is empty", name);
                return false;
            }

            // merge as many brushes as possible before expansion
            brushList.Merge(MergeAllowed.INSTANCE);//TODO:like cmp_t

            // if there is a .proc file newer than the .map file
            if (LoadProcBSP(fileName.getData(), mapFile.GetFileTime())) {
                ClipBrushSidesWithProcBSP(brushList);
                DeleteProcBSP();
            }

            // make copies of the brush list
            expandedBrushes.Append(brushList);
            for (i = 1; i < this.aasSettings.numBoundingBoxes; i++) {
                expandedBrushes.Append(brushList.Copy());
            }

            // expand brushes for the axial bounding boxes
            mask = AREACONTENTS_SOLID;
            for (i = 0; i < expandedBrushes.Num(); i++) {
                for (b = expandedBrushes.oGet(i).Head(); b != null; b = b.Next()) {
                    b.ExpandForAxialBox(this.aasSettings.boundingBoxes[i]);
                    bit = 1 << (i + AREACONTENTS_BBOX_BIT);
                    mask |= bit;
                    b.SetContents(b.GetContents() | bit);
                }
            }

            // move all brushes back into the original list
            for (i = 1; i < this.aasSettings.numBoundingBoxes; i++) {
                brushList.AddToTail(expandedBrushes.oGet(i));
//		delete expandedBrushes[i];
            }

            if (this.aasSettings.writeBrushMap[0]) {
                bsp.WriteBrushMap(fileName, new idStr("_" + this.aasSettings.fileExtension), AREACONTENTS_SOLID);
            }

            // build BSP tree from brushes
            bsp.Build(brushList, AREACONTENTS_SOLID, ExpandedChopAllowed.INSTANCE, ExpandedMergeAllowed.INSTANCE);

            // only solid nodes with all bits set for all bounding boxes need to stay solid
            ChangeMultipleBoundingBoxContents_r(bsp.GetRootNode(), mask);

            // portalize the bsp tree
            bsp.Portalize();

            // remove subspaces not reachable by entities
            if (!bsp.RemoveOutside(mapFile, AREACONTENTS_SOLID, entityClassNames)) {
                bsp.LeakFile(name);
//                delete mapFile;
                common.Printf("%s has no outside", name);
                return false;
            }

            // gravitational subdivision
            GravitationalSubdivision(bsp);

            // merge portals where possible
            bsp.MergePortals(AREACONTENTS_SOLID);

            // melt portal windings
            bsp.MeltPortals(AREACONTENTS_SOLID);

            if (this.aasSettings.writeBrushMap[0]) {
                WriteLedgeMap(fileName, new idStr("_" + this.aasSettings.fileExtension + "_ledge"));
            }

            // ledge subdivisions
            LedgeSubdivision(bsp);

            // merge leaf nodes
            MergeLeafNodes(bsp);

            // merge portals where possible
            bsp.MergePortals(AREACONTENTS_SOLID);

            // melt portal windings
            bsp.MeltPortals(AREACONTENTS_SOLID);

            // store the file from the bsp tree
            StoreFile(bsp);
            this.file.settings = this.aasSettings;

            // calculate reachability
            reach.Build(mapFile, this.file);

            // build clusters
            cluster.Build(this.file);

            // optimize the file
            if (!this.aasSettings.noOptimize) {
                this.file.Optimize();
            }

            // write the file
            name.SetFileExtension(this.aasSettings.fileExtension);
            this.file.Write(name, mapFile.GetGeometryCRC());

            // delete the map file
//	delete mapFile;
            common.Printf("%6d seconds to create AAS\n", (Sys_Milliseconds() - startTime) / 1000);

            return true;
        }

        public boolean BuildReachability(final idStr fileName, final idAASSettings settings) {
            int startTime;
            idMapFile mapFile;
            idStr name;
            final idAASReach reach = new idAASReach();
            final idAASCluster cluster = new idAASCluster();

            startTime = Sys_Milliseconds();

            this.aasSettings = settings;

            name = fileName;
            name.SetFileExtension("map");

            mapFile = new idMapFile();
            if (!mapFile.Parse(name.getData())) {
//		delete mapFile;
                common.Error("Couldn't load map file: '%s'", name);
                return false;
            }

            this.file = new idAASFileLocal();

            name.SetFileExtension(this.aasSettings.fileExtension);
            if (!this.file.Load(name, 0)) {
//		delete mapFile;
                common.Error("Couldn't load AAS file: '%s'", name);
                return false;
            }

            this.file.settings = this.aasSettings;

            // calculate reachability
            reach.Build(mapFile, this.file);

            // build clusters
            cluster.Build(this.file);

            // write the file
            this.file.Write(name, mapFile.GetGeometryCRC());

//	// delete the map file
//	delete mapFile;
            common.Printf("%6d seconds to calculate reachability\n", (Sys_Milliseconds() - startTime) / 1000);

            return true;
        }

        public void Shutdown() {
            this.aasSettings = null;
            if (this.file != null) {
//		delete file;
                this.file = null;
            }
            DeleteProcBSP();
            this.numGravitationalSubdivisions = 0;
            this.numMergedLeafNodes = 0;
            this.numLedgeSubdivisions = 0;
            this.ledgeList.Clear();
            if (this.ledgeMap != null) {
//		delete ledgeMap;
                this.ledgeMap = null;
            }
        }

        // map loading
        private void ParseProcNodes(idLexer src) {
            int i;

            src.ExpectTokenString("{");

            this.numProcNodes = src.ParseInt();
            if (this.numProcNodes < 0) {
                src.Error("idAASBuild::ParseProcNodes: bad numProcNodes");
            }
            this.procNodes = new aasProcNode_s[this.numProcNodes];// Mem_ClearedAlloc(idAASBuild.numProcNodes /* sizeof( aasProcNode_s )*/);

            for (i = 0; i < this.numProcNodes; i++) {
                aasProcNode_s node;

                node = (this.procNodes[i]);

                src.Parse1DMatrix(4, node.plane);
                node.children[0] = src.ParseInt();
                node.children[1] = src.ParseInt();
            }

            src.ExpectTokenString("}");
        }

        private boolean LoadProcBSP(final String name, long minFileTime) {
            idStr fileName;
            final idToken token = new idToken();
            idLexer src;

            // load it
            fileName = new idStr(name);
            fileName.SetFileExtension(PROC_FILE_EXT);
            src = new idLexer(fileName.getData(), LEXFL_NOSTRINGCONCAT | LEXFL_NODOLLARPRECOMPILE);
            if (!src.IsLoaded()) {
                common.Warning("idAASBuild::LoadProcBSP: couldn't load %s", fileName);
//		delete src;
                return false;
            }

            // if the file is too old
            if (src.GetFileTime() < minFileTime) {
//		delete src;
                return false;
            }

            if (!src.ReadToken(token) || (token.Icmp(PROC_FILE_ID) != 0)) {
                common.Warning("idAASBuild::LoadProcBSP: bad id '%s' instead of '%s'", token, PROC_FILE_ID);
//		delete src;
                return false;
            }

            // parse the file
            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("model")) {
                    src.SkipBracedSection();
                    continue;
                }

                if (token.equals("shadowModel")) {
                    src.SkipBracedSection();
                    continue;
                }

                if (token.equals("interAreaPortals")) {
                    src.SkipBracedSection();
                    continue;
                }

                if (token.equals("nodes")) {
                    this.ParseProcNodes(src);
                    break;
                }

                src.Error("idAASBuild::LoadProcBSP: bad token \"%s\"", token);
            }

//	delete src;
            return true;
        }

        private void DeleteProcBSP() {
            if (this.procNodes != null) {
//                Mem_Free(procNodes);
                this.procNodes = null;
            }
            this.numProcNodes = 0;
        }

        private boolean ChoppedAwayByProcBSP(int nodeNum, idFixedWinding w, final idVec3 normal, final idVec3 origin, final float radius) {
            int res;
            final idFixedWinding back = new idFixedWinding();
            aasProcNode_s node;
            float dist;

            do {
                node = this.procNodes[nodeNum];
                dist = node.plane.Normal().oMultiply(origin) + node.plane.oGet(3);
                if (dist > radius) {
                    res = SIDE_FRONT;
                } else if (dist < -radius) {
                    res = SIDE_BACK;
                } else {
                    res = w.Split(back, node.plane, ON_EPSILON);
                }
                if (res == SIDE_FRONT) {
                    nodeNum = node.children[0];
                } else if (res == SIDE_BACK) {
                    nodeNum = node.children[1];
                } else if (res == SIDE_ON) {
                    // continue with the side the winding faces
                    if (node.plane.Normal().oMultiply(normal) > 0.0f) {
                        nodeNum = node.children[0];
                    } else {
                        nodeNum = node.children[1];
                    }
                } else {
                    // if either node is not solid
                    if ((node.children[0] < 0) || (node.children[1] < 0)) {
                        return false;
                    }
                    // only recurse if the node is not solid
                    if (node.children[1] > 0) {
                        if (!this.ChoppedAwayByProcBSP(node.children[1], back, normal, origin, radius)) {
                            return false;
                        }
                    }
                    nodeNum = node.children[0];
                }
            } while (nodeNum > 0);
            if (nodeNum < 0) {
                return false;
            }
            return true;
        }

        private void ClipBrushSidesWithProcBSP(idBrushList brushList) {
            int i, clippedSides;
            idBrush brush;
            idFixedWinding neww;
            final idBounds bounds = new idBounds();
            float radius;
            idVec3 origin;

            // if the .proc file has no BSP tree
            if (this.procNodes == null) {
                return;
            }

            clippedSides = 0;
            for (brush = brushList.Head(); brush != null; brush = brush.Next()) {
                for (i = 0; i < brush.GetNumSides(); i++) {

                    if (NOT(brush.GetSide(i).GetWinding())) {
                        continue;
                    }

                    // make a local copy of the winding
                    neww = (idFixedWinding) brush.GetSide(i).GetWinding();
                    neww.GetBounds(bounds);
                    origin = (bounds.oGet(1).oMinus(bounds.oGet(0)).oMultiply(0.5f));
                    radius = origin.Length() + ON_EPSILON;
                    origin = bounds.oGet(0).oPlus(origin);

                    if (ChoppedAwayByProcBSP(0, neww, brush.GetSide(i).GetPlane().Normal(), origin, radius)) {
                        brush.GetSide(i).SetFlag(SFL_USED_SPLITTER);
                        clippedSides++;
                    }
                }
            }

            common.Printf("%6d brush sides clipped\n", clippedSides);
        }

        private int ContentsForAAS(int contents) {
            int c;

            if ((contents & (CONTENTS_SOLID | CONTENTS_AAS_SOLID | CONTENTS_MONSTERCLIP)) != 0) {
                return AREACONTENTS_SOLID;
            }
            c = 0;
            if ((contents & CONTENTS_WATER) != 0) {
                c |= AREACONTENTS_WATER;
            }
            if ((contents & CONTENTS_AREAPORTAL) != 0) {
                c |= AREACONTENTS_CLUSTERPORTAL;
            }
            if ((contents & CONTENTS_AAS_OBSTACLE) != 0) {
                c |= AREACONTENTS_OBSTACLE;
            }
            return c;
        }

        private idBrushList AddBrushesForMapBrush(final idMapBrush mapBrush, final idVec3 origin, final idMat3 axis, int entityNum, int primitiveNum, idBrushList brushList) {
            int contents, i;
            idMapBrushSide mapSide;
            idMaterial mat;
            final idList<idBrushSide> sideList = new idList<>();
            idBrush brush;
            idPlane plane;

            contents = 0;
            for (i = 0; i < mapBrush.GetNumSides(); i++) {
                mapSide = mapBrush.GetSide(i);
                mat = declManager.FindMaterial(mapSide.GetMaterial());
                contents |= mat.GetContentFlags();
                plane = mapSide.GetPlane();
                plane.FixDegeneracies(DEGENERATE_DIST_EPSILON);
                sideList.Append(new idBrushSide(plane, -1));
            }

            contents = ContentsForAAS(contents);
            if (0 == contents) {
                for (i = 0; i < sideList.Num(); i++) {
//			delete sideList[i];
                    sideList.oSet(i, null);
                }
                return brushList;
            }

            brush = new idBrush();
            brush.SetContents(contents);

            if (!brush.FromSides(sideList)) {
                common.Warning("brush primitive %d on entity %d is degenerate", primitiveNum, entityNum);
//		delete brush;
                brush = null;
                return brushList;
            }

            brush.SetEntityNum(entityNum);
            brush.SetPrimitiveNum(primitiveNum);
            brush.Transform(origin, axis);
            brushList.AddToTail(brush);

            return brushList;
        }

        private idBrushList AddBrushesForMapPatch(final idMapPatch mapPatch, final idVec3 origin, final idMat3 axis, int entityNum, int primitiveNum, idBrushList brushList) {
            int i, j, contents, validBrushes;
            float dot;
            int v1, v2, v3, v4;
            final idFixedWinding w = new idFixedWinding();
            final idPlane plane = new idPlane();
            idVec3 d1, d2;
            idBrush brush;
            idSurface_Patch mesh;
            idMaterial mat;

            mat = declManager.FindMaterial(mapPatch.GetMaterial());
            contents = ContentsForAAS(mat.GetContentFlags());

            if (0 == contents) {
                return brushList;
            }

            mesh = new idSurface_Patch(mapPatch);

            // if the patch has an explicit number of subdivisions use it to avoid cracks
            if (mapPatch.GetExplicitlySubdivided()) {
                mesh.SubdivideExplicit(mapPatch.GetHorzSubdivisions(), mapPatch.GetVertSubdivisions(), false, true);
            } else {
                mesh.Subdivide(DEFAULT_CURVE_MAX_ERROR_CD, DEFAULT_CURVE_MAX_ERROR_CD, DEFAULT_CURVE_MAX_LENGTH_CD, false);
            }

            validBrushes = 0;

            for (i = 0; i < (mesh.GetWidth() - 1); i++) {
                for (j = 0; j < (mesh.GetHeight() - 1); j++) {

                    v1 = (j * mesh.GetWidth()) + i;
                    v2 = v1 + 1;
                    v3 = v1 + mesh.GetWidth() + 1;
                    v4 = v1 + mesh.GetWidth();

                    d1 = mesh.oGet(v2).xyz.oMinus(mesh.oGet(v1).xyz);
                    d2 = mesh.oGet(v3).xyz.oMinus(mesh.oGet(v1).xyz);
                    plane.SetNormal(d1.Cross(d2));
                    if (plane.Normalize() != 0.0f) {
                        plane.FitThroughPoint(mesh.oGet(v1).xyz);
                        dot = plane.Distance(mesh.oGet(v4).xyz);
                        // if we can turn it into a quad
                        if (idMath.Fabs(dot) < 0.1f) {
                            w.Clear();
                            w.oPluSet(mesh.oGet(v1).xyz);
                            w.oPluSet(mesh.oGet(v2).xyz);
                            w.oPluSet(mesh.oGet(v3).xyz);
                            w.oPluSet(mesh.oGet(v4).xyz);

                            brush = new idBrush();
                            brush.SetContents(contents);
                            if (brush.FromWinding(w, plane)) {
                                brush.SetEntityNum(entityNum);
                                brush.SetPrimitiveNum(primitiveNum);
                                brush.SetFlag(BFL_PATCH);
                                brush.Transform(origin, axis);
                                brushList.AddToTail(brush);
                                validBrushes++;
                            } else {
//						delete brush;
//                                brush = null;
                            }
                            continue;
                        } else {
                            // create one of the triangles
                            w.Clear();
                            w.oPluSet(mesh.oGet(v1).xyz);
                            w.oPluSet(mesh.oGet(v2).xyz);
                            w.oPluSet(mesh.oGet(v3).xyz);

                            brush = new idBrush();
                            brush.SetContents(contents);
                            if (brush.FromWinding(w, plane)) {
                                brush.SetEntityNum(entityNum);
                                brush.SetPrimitiveNum(primitiveNum);
                                brush.SetFlag(BFL_PATCH);
                                brush.Transform(origin, axis);
                                brushList.AddToTail(brush);
                                validBrushes++;
                            } else {
//						delete brush;
//                                brush = null;
                            }
                        }
                    }
                    // create the other triangle
                    d1 = mesh.oGet(v3).xyz.oMinus(mesh.oGet(v1).xyz);
                    d2 = mesh.oGet(v4).xyz.oMinus(mesh.oGet(v1).xyz);
                    plane.SetNormal(d1.Cross(d2));
                    if (plane.Normalize() != 0.0f) {
                        plane.FitThroughPoint(mesh.oGet(v1).xyz);

                        w.Clear();
                        w.oPluSet(mesh.oGet(v1).xyz);
                        w.oPluSet(mesh.oGet(v3).xyz);
                        w.oPluSet(mesh.oGet(v4).xyz);

                        brush = new idBrush();
                        brush.SetContents(contents);
                        if (brush.FromWinding(w, plane)) {
                            brush.SetEntityNum(entityNum);
                            brush.SetPrimitiveNum(primitiveNum);
                            brush.SetFlag(BFL_PATCH);
                            brush.Transform(origin, axis);
                            brushList.AddToTail(brush);
                            validBrushes++;
                        } else {
//					delete brush;
//                            brush = null;
                        }
                    }
                }
            }

            if (0 == validBrushes) {
                common.Warning("patch primitive %d on entity %d is completely degenerate", primitiveNum, entityNum);
            }

            return brushList;
        }

        private idBrushList AddBrushesForMapEntity(final idMapEntity mapEnt, int entityNum, idBrushList brushList) {
            int i;
            final idVec3 origin = new idVec3();
            idMat3 axis = new idMat3();

            if (mapEnt.GetNumPrimitives() < 1) {
                return brushList;
            }

            mapEnt.epairs.GetVector("origin", "0 0 0", origin);
            if (!mapEnt.epairs.GetMatrix("rotation", "1 0 0 0 1 0 0 0 1", axis)) {
                final float angle = mapEnt.epairs.GetFloat("angle");
                if (angle != 0.0f) {
                    axis = new idAngles(0.0f, angle, 0.0f).ToMat3();
                } else {
                    axis.Identity();
                }
            }

            for (i = 0; i < mapEnt.GetNumPrimitives(); i++) {
                idMapPrimitive mapPrim;

                mapPrim = mapEnt.GetPrimitive(i);
                if (mapPrim.GetType() == idMapPrimitive.TYPE_BRUSH) {
                    brushList = AddBrushesForMapBrush((idMapBrush) mapPrim, origin, axis, entityNum, i, brushList);
                    continue;
                }
                if (mapPrim.GetType() == idMapPrimitive.TYPE_PATCH) {
                    if (this.aasSettings.usePatches[0]) {
                        brushList = AddBrushesForMapPatch((idMapPatch) mapPrim, origin, axis, entityNum, i, brushList);
                    }
//                    continue;
                }
            }

            return brushList;
        }

        private idBrushList AddBrushesForMapFile(final idMapFile mapFile, idBrushList brushList) {
            int i;

            common.Printf("[Brush Load]\n");

            brushList = AddBrushesForMapEntity(mapFile.GetEntity(0), 0, brushList);

            for (i = 1; i < mapFile.GetNumEntities(); i++) {
                final String classname = mapFile.GetEntity(i).epairs.GetString("classname");

                if (idStr.Icmp(classname, "func_aas_obstacle") == 0) {
                    brushList = AddBrushesForMapEntity(mapFile.GetEntity(i), i, brushList);
                }
            }

            common.Printf("%6d brushes\n", brushList.Num());

            return brushList;
        }

        private boolean CheckForEntities(final idMapFile mapFile, idStrList entityClassNames) {
            int i;
            final idStr classname = new idStr();

            com_editors |= EDITOR_AAS;

            for (i = 0; i < mapFile.GetNumEntities(); i++) {
                if (!mapFile.GetEntity(i).epairs.GetString("classname", "", classname)) {
                    continue;
                }

                if (this.aasSettings.ValidEntity(classname.getData())) {
                    entityClassNames.AddUnique(classname);
                }
            }

            com_editors &= ~EDITOR_AAS;

            return (entityClassNames.Num() != 0);
        }

        private void ChangeMultipleBoundingBoxContents_r(idBrushBSPNode node, int mask) {
            while (node != null) {
                if (0 == (node.GetContents() & mask)) {
                    node.SetContents(node.GetContents() & ~AREACONTENTS_SOLID);
                }
                ChangeMultipleBoundingBoxContents_r(node.GetChild(0), mask);
                node = node.GetChild(1);
            }
        }

        // gravitational subdivision
        private void SetPortalFlags_r(idBrushBSPNode node) {
            int s;
            idBrushBSPPortal p;
            idVec3 normal;

            if (NOT(node)) {
                return;
            }

            if ((node.GetContents() & AREACONTENTS_SOLID) != 0) {
                return;
            }

            if (NOT(node.GetChild(0)) && NOT(node.GetChild(1))) {
                for (p = node.GetPortals(); p != null; p = p.Next(s)) {
                    s = (p.GetNode(1).equals(node)) ? 1 : 0;

                    // if solid at the other side of the portal
                    if ((p.GetNode(/*!s*/1 ^ s).GetContents() & AREACONTENTS_SOLID) != 0) {//TODO:check that the answer is always 1 or 0.
                        if (s != 0) {
                            normal = p.GetPlane().Normal().oNegative();
                        } else {
                            normal = p.GetPlane().Normal();
                        }
                        if (normal.oMultiply(this.aasSettings.invGravityDir) > this.aasSettings.minFloorCos[0]) {
                            p.SetFlag(FACE_FLOOR);
                        } else {
                            p.SetFlag(FACE_SOLID);
                        }
                    }
                }
                return;
            }

            SetPortalFlags_r(node.GetChild(0));
            SetPortalFlags_r(node.GetChild(1));
        }

        private boolean PortalIsGap(idBrushBSPPortal portal, int side) {
            idVec3 normal;

            // if solid at the other side of the portal
            if ((portal.GetNode(/*!side*/1 ^ side).GetContents() & AREACONTENTS_SOLID) != 0) {
                return false;
            }

            if (side != 0) {
                normal = portal.GetPlane().Normal().oNegative();
            } else {
                normal = portal.GetPlane().Normal();
            }
            if (normal.oMultiply(this.aasSettings.invGravityDir) > this.aasSettings.minFloorCos[0]) {
                return true;
            }
            return false;
        }
        private static final int FACE_CHECKED = BIT(31);
        private static final float GRAVSUBDIV_EPSILON = 0.1f;

        private void GravSubdivLeafNode(idBrushBSPNode node) {
            int s1, s2, i, j, k, side1;
            int numSplits, numSplitters;
            idBrushBSPPortal p1, p2;
            idWinding w1, w2;
            idVec3 normal;
            final idPlane plane = new idPlane();
            final idPlaneSet planeList = new idPlaneSet();
            float d, min, max;
            int[] splitterOrder;
            int[] bestNumSplits;
            int floor, gap, numFloorChecked;

            // if this leaf node is already classified it cannot have a combination of floor and gap portals
            if ((node.GetFlags() & (AREA_FLOOR | AREA_GAP)) != 0) {
                return;
            }

            floor = gap = 0;

            // check if the area has a floor
            for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;

                if ((p1.GetFlags() & FACE_FLOOR) != 0) {
                    floor++;
                }
            }

            // find seperating planes between gap and floor portals
            for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;

                // if the portal is a gap seen from this side
                if (PortalIsGap(p1, s1)) {
                    gap++;
                    // if the area doesn't have a floor
                    if (0 == floor) {
                        break;
                    }
                } else {
                    continue;
                }

                numFloorChecked = 0;

                w1 = p1.GetWinding();

                // test all edges of the gap
                for (i = 0; i < w1.GetNumPoints(); i++) {

                    // create a plane through the edge of the gap parallel to the direction of gravity
                    normal = w1.oGet((i + 1) % w1.GetNumPoints()).ToVec3().oMinus(w1.oGet(i).ToVec3());
                    normal = normal.Cross(this.aasSettings.invGravityDir);
                    if (normal.Normalize() < 0.2f) {
                        continue;
                    }
                    plane.SetNormal(normal);
                    plane.FitThroughPoint(w1.oGet(i).ToVec3());

                    // get the side of the plane the gap is on
                    side1 = w1.PlaneSide(plane, GRAVSUBDIV_EPSILON);
                    if (side1 == SIDE_ON) {
                        break;
                    }

                    // test if the plane through the edge of the gap seperates the gap from a floor portal
                    for (p2 = node.GetPortals(); p2 != null; p2 = p2.Next(s2)) {
                        s2 = (p2.GetNode(1).equals(node)) ? 1 : 0;

                        if (0 == (p2.GetFlags() & FACE_FLOOR)) {
                            continue;
                        }

                        if ((p2.GetFlags() & FACE_CHECKED) != 0) {
                            continue;
                        }

                        w2 = p2.GetWinding();

                        min = 2.0f * GRAVSUBDIV_EPSILON;
                        max = GRAVSUBDIV_EPSILON;
                        if (side1 == SIDE_FRONT) {
                            for (j = 0; j < w2.GetNumPoints(); j++) {
                                d = plane.Distance(w2.oGet(j).ToVec3());
                                if (d >= GRAVSUBDIV_EPSILON) {
                                    break;	// point at the same side of the plane as the gap
                                }
                                d = idMath.Fabs(d);
                                if (d < min) {
                                    min = d;
                                }
                                if (d > max) {
                                    max = d;
                                }
                            }
                        } else {
                            for (j = 0; j < w2.GetNumPoints(); j++) {
                                d = plane.Distance(w2.oGet(j).ToVec3());
                                if (d <= -GRAVSUBDIV_EPSILON) {
                                    break;	// point at the same side of the plane as the gap
                                }
                                d = idMath.Fabs(d);
                                if (d < min) {
                                    min = d;
                                }
                                if (d > max) {
                                    max = d;
                                }
                            }
                        }

                        // a point of the floor portal was found to be at the same side of the plane as the gap
                        if (j < w2.GetNumPoints()) {
                            continue;
                        }

                        // if the floor portal touches the plane
                        if ((min < GRAVSUBDIV_EPSILON) && (max > GRAVSUBDIV_EPSILON)) {
                            planeList.FindPlane(plane, 0.00001f, 0.1f);
                        }

                        p2.SetFlag(FACE_CHECKED);
                        numFloorChecked++;

                    }
                    if (numFloorChecked == floor) {
                        break;
                    }
                }

                for (p2 = node.GetPortals(); p2 != null; p2 = p2.Next(s2)) {
                    s2 = (p2.GetNode(1).equals(node)) ? 1 : 0;
                    p2.RemoveFlag(FACE_CHECKED);
                }
            }

            // if the leaf node does not have both floor and gap portals
            if (!((gap != 0) && (floor != 0))) {
//                if (0 == (gap & floor)) {//TODO:check i this works better.
                if (floor != 0) {
                    node.SetFlag(AREA_FLOOR);
                } else if (gap != 0) {
                    node.SetFlag(AREA_GAP);
                }
                return;
            }

            // if no valid seperators found
            if (planeList.Num() == 0) {
                // NOTE: this should never happend, if it does the leaf node has degenerate portals
                return;
            }

//	splitterOrder = (int *) _alloca( planeList.Num() * sizeof( int ) );
//	bestNumSplits = (int *) _alloca( planeList.Num() * sizeof( int ) );
            splitterOrder = new int[planeList.Num()];
            bestNumSplits = new int[planeList.Num()];
            numSplitters = 0;

            // test all possible seperators and sort them from best to worst
            for (i = 0; i < planeList.Num(); i += 2) {
                numSplits = 0;

                for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                    s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;
                    if (p1.GetWinding().PlaneSide(planeList.oGet(i), 0.1f) == SIDE_CROSS) {
                        numSplits++;
                    }
                }

                for (j = 0; j < numSplitters; j++) {
                    if (numSplits < bestNumSplits[j]) {
                        for (k = numSplitters; k > j; k--) {
                            bestNumSplits[k] = bestNumSplits[k - 1];
                            splitterOrder[k] = splitterOrder[k - 1];
                        }
                        bestNumSplits[j] = numSplits;
                        splitterOrder[j] = i;
                        numSplitters++;
                        break;
                    }
                }
                if (j >= numSplitters) {
                    bestNumSplits[j] = numSplits;
                    splitterOrder[j] = i;
                    numSplitters++;
                }
            }

            // try all seperators in order from best to worst
            for (i = 0; i < numSplitters; i++) {
                if (node.Split(planeList.oGet(splitterOrder[i]), -1)) {
                    // we found a seperator that works
                    break;
                }
            }
            if (i >= numSplitters) {
                return;
            }

            DisplayRealTimeString("\r%6d", ++this.numGravitationalSubdivisions);

            // test children for further splits
            GravSubdivLeafNode(node.GetChild(0));
            GravSubdivLeafNode(node.GetChild(1));
        }

        private void GravSubdiv_r(idBrushBSPNode node) {

            if (NOT(node)) {
                return;
            }

            if ((node.GetContents() & AREACONTENTS_SOLID) != 0) {
                return;
            }

            if (NOT(node.GetChild(0)) && NOT(node.GetChild(1))) {
                GravSubdivLeafNode(node);
                return;
            }

            GravSubdiv_r(node.GetChild(0));
            GravSubdiv_r(node.GetChild(1));
        }

        private void GravitationalSubdivision(idBrushBSP bsp) {
            this.numGravitationalSubdivisions = 0;

            common.Printf("[Gravitational Subdivision]\n");

            SetPortalFlags_r(bsp.GetRootNode());
            GravSubdiv_r(bsp.GetRootNode());

            common.Printf("\r%6d subdivisions\n", this.numGravitationalSubdivisions);
        }

        // ledge subdivision
        private void LedgeSubdivFlood_r(idBrushBSPNode node, final idLedge ledge) {
            int s1, i;
            idBrushBSPPortal p1;
            idWinding w;
            final idList<idBrushBSPNode> nodeList = new idList<>();

            if ((node.GetFlags() & NODE_VISITED) != 0) {
                return;
            }

            // if this is not already a ledge area
            if (0 == (node.GetFlags() & AREA_LEDGE)) {
                for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                    s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;

                    if (0 == (p1.GetFlags() & FACE_FLOOR)) {
                        continue;
                    }

                    // split the area if some part of the floor portal is inside the expanded ledge
                    w = ledge.ChopWinding(p1.GetWinding());
                    if (NOT(w)) {
                        continue;
                    }
//			delete w;
//                    w = null;

                    for (i = 0; i < ledge.numSplitPlanes; i++) {
                        if (node.PlaneSide(ledge.planes[i], 0.1f) != SIDE_CROSS) {
                            continue;
                        }
                        if (!node.Split(ledge.planes[i], -1)) {
                            continue;
                        }
                        this.numLedgeSubdivisions++;
                        DisplayRealTimeString("\r%6d", this.numLedgeSubdivisions);
                        node.GetChild(0).SetFlag(NODE_VISITED);
                        LedgeSubdivFlood_r(node.GetChild(1), ledge);
                        return;
                    }

                    node.SetFlag(AREA_LEDGE);
                    break;
                }
            }

            node.SetFlag(NODE_VISITED);

            // get all nodes we might need to flood into
            for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;

                if ((p1.GetNode(/*!s1*/1 ^ s1).GetContents() & AREACONTENTS_SOLID) != 0) {
                    continue;
                }

                // flood through this portal if the portal is partly inside the expanded ledge
                w = ledge.ChopWinding(p1.GetWinding());
                if (NOT(w)) {
                    continue;
                }
//		delete w;
//                w = null;
                // add to list, cannot flood directly cause portals might be split on the way
                nodeList.Append(p1.GetNode( /*!s1*/s1 ^ 1));
            }

            // flood into other nodes
            for (i = 0; i < nodeList.Num(); i++) {
                LedgeSubdivLeafNodes_r(nodeList.oGet(i), ledge);
            }
        }

        /*
         ============
         idAASBuild::LedgeSubdivLeafNodes_r

         The node the ledge was originally part of might be split by other ledges.
         Here we recurse down the tree from the original node to find all the new leaf nodes the ledge might be part of.
         ============
         */
        private void LedgeSubdivLeafNodes_r(idBrushBSPNode node, final idLedge ledge) {
            if (NOT(node)) {//TODO:use NOT function
                return;
            }
            if (NOT(node.GetChild(0)) && NOT(node.GetChild(1))) {
                LedgeSubdivFlood_r(node, ledge);
                return;
            }
            LedgeSubdivLeafNodes_r(node.GetChild(0), ledge);
            LedgeSubdivLeafNodes_r(node.GetChild(1), ledge);
        }

        private void LedgeSubdiv(idBrushBSPNode root) {
            int i, j;
            idBrush brush;
            final idList<idBrushSide> sideList = new idList<>();

            // create ledge bevels and expand ledges
            for (i = 0; i < this.ledgeList.Num(); i++) {

                this.ledgeList.oGet(i).CreateBevels(this.aasSettings.gravityDir);
                this.ledgeList.oGet(i).Expand(this.aasSettings.boundingBoxes[0], this.aasSettings.maxStepHeight[0]);

                // if we should write out a ledge map
                if (this.ledgeMap != null) {
                    sideList.SetNum(0);
                    for (j = 0; j < this.ledgeList.oGet(i).numPlanes; j++) {
                        sideList.Append(new idBrushSide(this.ledgeList.oGet(i).planes[j], -1));
                    }

                    brush = new idBrush();
                    brush.FromSides(sideList);

                    this.ledgeMap.WriteBrush(brush);

//			delete brush;
//                    brush = null;
                }

                // flood tree from the ledge node and subdivide areas with the ledge
                LedgeSubdivLeafNodes_r(this.ledgeList.oGet(i).node, this.ledgeList.oGet(i));

                // remove the node visited flags
                this.ledgeList.oGet(i).node.RemoveFlagRecurseFlood(NODE_VISITED);
            }
        }

        private boolean IsLedgeSide_r(idBrushBSPNode node, idFixedWinding w, final idPlane plane, final idVec3 normal, final idVec3 origin, final float radius) {
            int res, i;
            final idFixedWinding back = new idFixedWinding();
            float dist;

            if (NOT(node)) {
                return false;
            }

            while ((node.GetChild(0) != null) && (node.GetChild(1) != null)) {
                dist = node.GetPlane().Distance(origin);
                if (dist > radius) {
                    res = SIDE_FRONT;
                } else if (dist < -radius) {
                    res = SIDE_BACK;
                } else {
                    res = w.Split(back, node.GetPlane(), LEDGE_EPSILON);
                }
                if (res == SIDE_FRONT) {
                    node = node.GetChild(0);
                } else if (res == SIDE_BACK) {
                    node = node.GetChild(1);
                } else if (res == SIDE_ON) {
                    // continue with the side the winding faces
                    if (node.GetPlane().Normal().oMultiply(normal) > 0.0f) {
                        node = node.GetChild(0);
                    } else {
                        node = node.GetChild(1);
                    }
                } else {
                    if (IsLedgeSide_r(node.GetChild(1), back, plane, normal, origin, radius)) {
                        return true;
                    }
                    node = node.GetChild(0);
                }
            }

            if ((node.GetContents() & AREACONTENTS_SOLID) != 0) {
                return false;
            }

            for (i = 0; i < w.GetNumPoints(); i++) {
                if (plane.Distance(w.oGet(i).ToVec3()) > 0.0f) {
                    return true;
                }
            }

            return false;
        }

        private void AddLedge(final idVec3 v1, final idVec3 v2, idBrushBSPNode node) {
            int i, j, merged;

            // first try to merge the ledge with existing ledges
            merged = -1;
            for (i = 0; i < this.ledgeList.Num(); i++) {

                for (j = 0; j < 2; j++) {
                    if (idMath.Fabs(this.ledgeList.oGet(i).planes[j].Distance(v1)) > LEDGE_EPSILON) {
                        break;
                    }
                    if (idMath.Fabs(this.ledgeList.oGet(i).planes[j].Distance(v2)) > LEDGE_EPSILON) {
                        break;
                    }
                }
                if (j < 2) {
                    continue;
                }

                if (!this.ledgeList.oGet(i).PointBetweenBounds(v1)
                        && !this.ledgeList.oGet(i).PointBetweenBounds(v2)) {
                    continue;
                }

                if (merged == -1) {
                    this.ledgeList.oGet(i).AddPoint(v1);
                    this.ledgeList.oGet(i).AddPoint(v2);
                    merged = i;
                } else {
                    this.ledgeList.oGet(merged).AddPoint(this.ledgeList.oGet(i).start);
                    this.ledgeList.oGet(merged).AddPoint(this.ledgeList.oGet(i).end);
                    this.ledgeList.RemoveIndex(i);
                    break;
                }
            }

            // if the ledge could not be merged
            if (merged == -1) {
                this.ledgeList.Append(new idLedge(v1, v2, this.aasSettings.gravityDir, node));
            }
        }

        private void FindLeafNodeLedges(idBrushBSPNode root, idBrushBSPNode node) {
            int s1, i;
            idBrushBSPPortal p1;
            idWinding w;
            idVec3 v1, v2, normal, origin;
            final idFixedWinding winding = new idFixedWinding();
            final idBounds bounds = new idBounds();
            idPlane plane;
            float radius;

            for (p1 = node.GetPortals(); p1 != null; p1 = p1.Next(s1)) {
                s1 = (p1.GetNode(1).equals(node)) ? 1 : 0;

                if (0 == (p1.GetFlags() & FACE_FLOOR)) {
                    continue;
                }

                if (s1 != 0) {
                    plane = p1.GetPlane();
                    w = p1.GetWinding().Reverse();
                } else {
                    plane = p1.GetPlane().oNegative();
                    w = p1.GetWinding();
                }

                for (i = 0; i < w.GetNumPoints(); i++) {

                    v1 = w.oGet(i).ToVec3();
                    v2 = w.oGet((i + 1) % w.GetNumPoints()).ToVec3();
                    normal = (v2.oMinus(v1)).Cross(this.aasSettings.gravityDir);
                    if (normal.Normalize() < 0.5f) {
                        continue;
                    }

                    winding.Clear();
                    winding.oPluSet(v1.oPlus(normal.oMultiply(LEDGE_EPSILON * 0.5f)));
                    winding.oPluSet(v2.oPlus(normal.oMultiply(LEDGE_EPSILON * 0.5f)));
                    winding.oPluSet(winding.oGet(1).ToVec3().oPlus(this.aasSettings.gravityDir.oMultiply(this.aasSettings.maxStepHeight[0] + 1.0f)));
                    winding.oPluSet(winding.oGet(0).ToVec3().oPlus(this.aasSettings.gravityDir.oMultiply(this.aasSettings.maxStepHeight[0] + 1.0f)));

                    winding.GetBounds(bounds);
                    origin = (bounds.oGet(1).oMinus(bounds.oGet(0)).oMultiply(0.5f));
                    radius = origin.Length() + LEDGE_EPSILON;
                    origin = bounds.oGet(0).oPlus(origin);

                    plane.FitThroughPoint(v1.oPlus(this.aasSettings.gravityDir.oMultiply(this.aasSettings.maxStepHeight[0])));

                    if (!IsLedgeSide_r(root, winding, plane, normal, origin, radius)) {
                        continue;
                    }

                    AddLedge(v1, v2, node);
                }

//                if (w != p1.GetWinding()) {
//			delete w;
//                    w = null;
//                }
            }
        }

        private void FindLedges_r(idBrushBSPNode root, idBrushBSPNode node) {
            if (NOT(node)) {
                return;
            }

            if ((node.GetContents() & AREACONTENTS_SOLID) != 0) {
                return;
            }

            if (NOT(node.GetChild(0)) && NOT(node.GetChild(1))) {
                if ((node.GetFlags() & NODE_VISITED) != 0) {
                    return;
                }
                FindLeafNodeLedges(root, node);
                node.SetFlag(NODE_VISITED);
                return;
            }

            FindLedges_r(root, node.GetChild(0));
            FindLedges_r(root, node.GetChild(1));
        }

        /*
         ============
         idAASBuild::LedgeSubdivision

         NOTE: this assumes the bounding box is higher than the maximum step height
         only ledges with vertical sides are considered
         ============
         */
        private void LedgeSubdivision(idBrushBSP bsp) {
            this.numLedgeSubdivisions = 0;
            this.ledgeList.Clear();

            common.Printf("[Ledge Subdivision]\n");

            bsp.GetRootNode().RemoveFlagRecurse(NODE_VISITED);
            FindLedges_r(bsp.GetRootNode(), bsp.GetRootNode());
            bsp.GetRootNode().RemoveFlagRecurse(NODE_VISITED);

            common.Printf("\r%6d ledges\n", this.ledgeList.Num());

            LedgeSubdiv(bsp.GetRootNode());

            common.Printf("\r%6d subdivisions\n", this.numLedgeSubdivisions);
        }

        private void WriteLedgeMap(final idStr fileName, final idStr ext) {
            this.ledgeMap = new idBrushMap(fileName, ext);
            this.ledgeMap.SetTexture("textures/base_trim/bluetex4q_ed");
        }

        // merging
        private boolean AllGapsLeadToOtherNode(idBrushBSPNode nodeWithGaps, idBrushBSPNode otherNode) {
            int s;
            idBrushBSPPortal p;

            for (p = nodeWithGaps.GetPortals(); p != null; p = p.Next(s)) {
                s = (p.GetNode(1).equals(nodeWithGaps)) ? 1 : 0;

                if (!PortalIsGap(p, s)) {
                    continue;
                }

                if (!p.GetNode(/*!s*/1 ^ s).equals(otherNode)) {
                    return false;
                }
            }
            return true;
        }

        private boolean MergeWithAdjacentLeafNodes(idBrushBSP bsp, idBrushBSPNode node) {
            int s, numMerges = 0, otherNodeFlags;
            idBrushBSPPortal p;

            do {
                for (p = node.GetPortals(); p != null; p = p.Next(s)) {
                    s = (p.GetNode(1).equals(node)) ? 1 : 0;

                    // both leaf nodes must have the same contents
                    if (node.GetContents() != p.GetNode(/*!s*/1 ^ s).GetContents()) {
                        continue;
                    }

                    // cannot merge leaf nodes if one is near a ledge and the other is not
                    if ((node.GetFlags() & AREA_LEDGE) != (p.GetNode(/*!s*/1 ^ s).GetFlags() & AREA_LEDGE)) {
                        continue;
                    }

                    // cannot merge leaf nodes if one has a floor portal and the other a gap portal
                    if ((node.GetFlags() & AREA_FLOOR) != 0) {
                        if ((p.GetNode(/*!s*/1 ^ s).GetFlags() & AREA_GAP) != 0) {
                            if (!AllGapsLeadToOtherNode(p.GetNode(/*!s*/1 ^ s), node)) {
                                continue;
                            }
                        }
                    } else if ((node.GetFlags() & AREA_GAP) != 0) {
                        if ((p.GetNode(/*!s*/1 ^ s).GetFlags() & AREA_FLOOR) != 0) {
                            if (!AllGapsLeadToOtherNode(node, p.GetNode(/*!s*/1 ^ s))) {
                                continue;
                            }
                        }
                    }

                    otherNodeFlags = p.GetNode(/*!s*/1 ^ s).GetFlags();

                    // try to merge the leaf nodes
                    if (bsp.TryMergeLeafNodes(p, s)) {
                        node.SetFlag(otherNodeFlags);
                        if ((node.GetFlags() & AREA_FLOOR) != 0) {
                            node.RemoveFlag(AREA_GAP);
                        }
                        numMerges++;
                        DisplayRealTimeString("\r%6d", ++this.numMergedLeafNodes);
                        break;
                    }
                }
            } while (p != null);

            if (numMerges != 0) {
                return true;
            }
            return false;
        }

        private void MergeLeafNodes_r(idBrushBSP bsp, idBrushBSPNode node) {

            if (NOT(node)) {
                return;
            }

            if ((node.GetContents() & AREACONTENTS_SOLID) != 0) {
                return;
            }

            if ((node.GetFlags() & NODE_DONE) != 0) {
                return;
            }

            if (NOT(node.GetChild(0)) && NOT(node.GetChild(1))) {
                MergeWithAdjacentLeafNodes(bsp, node);
                node.SetFlag(NODE_DONE);
                return;
            }

            MergeLeafNodes_r(bsp, node.GetChild(0));
            MergeLeafNodes_r(bsp, node.GetChild(1));

//            return;
        }

        private void MergeLeafNodes(idBrushBSP bsp) {
            this.numMergedLeafNodes = 0;

            common.Printf("[Merge Leaf Nodes]\n");

            MergeLeafNodes_r(bsp, bsp.GetRootNode());
            bsp.GetRootNode().RemoveFlagRecurse(NODE_DONE);
            bsp.PruneMergedTree_r(bsp.GetRootNode());

            common.Printf("\r%6d leaf nodes merged\n", this.numMergedLeafNodes);
        }

        // storing file
        private void SetupHash() {
            aas_vertexHash = new idHashIndex(VERTEX_HASH_SIZE, 1024);
            aas_edgeHash = new idHashIndex(EDGE_HASH_SIZE, 1024);
        }

        private void ShutdownHash() {
//	delete aas_vertexHash;
//	delete aas_edgeHash;
            aas_vertexHash = null;
            aas_edgeHash = null;
        }

        private void ClearHash(final idBounds bounds) {
            int i;
            float f, max;

            aas_vertexHash.Clear();
            aas_edgeHash.Clear();
            aas_vertexBounds = bounds;

            max = bounds.oGet(1).x - bounds.oGet(0).x;
            f = bounds.oGet(1).y - bounds.oGet(0).y;
            if (f > max) {
                max = f;
            }
            aas_vertexShift = (int) (max / VERTEX_HASH_BOXSIZE);
            for (i = 0; (1 << i) < aas_vertexShift; i++) {
            }
            if (i == 0) {
                aas_vertexShift = 1;
            } else {
                aas_vertexShift = i;
            }
        }

        private int HashVec(final idVec3 vec) {
            int x, y;

            x = (((int) ((vec.oGet(0) - aas_vertexBounds.oGet(0).x) + 0.5)) + 2) >> 2;
            y = (((int) ((vec.oGet(1) - aas_vertexBounds.oGet(0).y) + 0.5)) + 2) >> 2;
            return (x + (y * VERTEX_HASH_BOXSIZE)) & (VERTEX_HASH_SIZE - 1);
        }

        private boolean GetVertex(final idVec3 v, int[] vertexNum) {
            int i, hashKey, vn;
            final idVec3 /*aasVertex_t*/ vert = new idVec3();
			idVec3 /*aasVertex_t*/ p;

            for (i = 0; i < 3; i++) {
                if (idMath.Fabs(v.oGet(i) - idMath.Rint(v.oGet(i))) < INTEGRAL_EPSILON) {
                    vert.oSet(i, idMath.Rint(v.oGet(i)));
                } else {
                    vert.oSet(i, v.oGet(i));
                }
            }

            hashKey = this.HashVec(vert);

            for (vn = aas_vertexHash.First(hashKey); vn >= 0; vn = aas_vertexHash.Next(vn)) {
                p = this.file.vertices.oGet(vn);
                // first compare z-axis because hash is based on x-y plane
                if ((idMath.Fabs(vert.z - p.z) < VERTEX_EPSILON)
                        && (idMath.Fabs(vert.x - p.x) < VERTEX_EPSILON)
                        && (idMath.Fabs(vert.y - p.y) < VERTEX_EPSILON)) {
                    vertexNum[0] = vn;
                    return true;
                }
            }

            vertexNum[0] = this.file.vertices.Num();
            aas_vertexHash.Add(hashKey, this.file.vertices.Num());
            this.file.vertices.Append(vert);

            return false;
        }

        private boolean GetEdge(final idVec3 v1, final idVec3 v2, int[] edgeNum, int[] v1num) {
            return GetEdge(v1, v2, edgeNum, 0, v1num);
        }

        private boolean GetEdge(final idVec3 v1, final idVec3 v2, int[] edgeNum, final int edgeOffset, int[] v1num) {
            int hashKey, e;
            final int[] v2num = new int[1];
            int[] vertexNum;
            final aasEdge_s edge = new aasEdge_s();
            boolean found;

            if (v1num[0] != -1) {
                found = true;
            } else {
                found = GetVertex(v1, v1num);
            }
            found &= GetVertex(v2, v2num);
            // if both vertexes are the same or snapped onto each other
            if (v1num[0] == v2num[0]) {
                edgeNum[edgeOffset + 0] = 0;
                return true;
            }
            hashKey = aas_edgeHash.GenerateKey(v1num[0], v2num[0]);
            // if both vertexes where already stored
            if (found) {
                for (e = aas_edgeHash.First(hashKey); e >= 0; e = aas_edgeHash.Next(e)) {

                    vertexNum = this.file.edges.oGet(e).vertexNum;
                    if (vertexNum[0] == v2num[0]) {
                        if (vertexNum[1] == v1num[0]) {
                            // negative for a reversed edge
                            edgeNum[edgeOffset + 0] = -e;
                            break;
                        }
                    } else if (vertexNum[0] == v1num[0]) {
                        if (vertexNum[1] == v2num[0]) {
                            edgeNum[edgeOffset + 0] = e;
                            break;
                        }
                    }
                }
                // if edge found in hash
                if (e >= 0) {
                    return true;
                }
            }

            edgeNum[edgeOffset + 0] = this.file.edges.Num();
            aas_edgeHash.Add(hashKey, this.file.edges.Num());

            edge.vertexNum[0] = v1num[0];
            edge.vertexNum[1] = v2num[0];

            this.file.edges.Append(edge);

            return false;
        }

        private boolean GetFaceForPortal(idBrushBSPPortal portal, int side, int[] faceNum) {
            int i, j;
            final int[] v1num = {0};
            int numFaceEdges;
            final int[] faceEdges = new int[MAX_POINTS_ON_WINDING];//TODO:make these kind of arrays final?
            idWinding w;
            final aasFace_s face = new aasFace_s();

            if (portal.GetFaceNum() > 0) {
                if (side != 0) {
                    faceNum[0] = -portal.GetFaceNum();
                } else {
                    faceNum[0] = portal.GetFaceNum();
                }
                return true;
            }

            w = portal.GetWinding();
            // turn the winding into a sequence of edges
            numFaceEdges = 0;
            v1num[0] = -1;		// first vertex unknown
            for (i = 0; i < w.GetNumPoints(); i++) {

                GetEdge(w.oGet(i).ToVec3(), w.oGet((i + 1) % w.GetNumPoints()).ToVec3(), faceEdges, numFaceEdges, v1num);

                if (faceEdges[numFaceEdges] != 0) {
                    // last vertex of this edge is the first vertex of the next edge
                    v1num[0] = this.file.edges.oGet(abs(faceEdges[numFaceEdges])).vertexNum[INTSIGNBITNOTSET(faceEdges[numFaceEdges])];

                    // this edge is valid so keep it
                    numFaceEdges++;
                }
            }

            // should have at least 3 edges
            if (numFaceEdges < 3) {
                return false;
            }

            // the polygon is invalid if some edge is found twice
            for (i = 0; i < numFaceEdges; i++) {
                for (j = i + 1; j < numFaceEdges; j++) {
                    if ((faceEdges[i] == faceEdges[j]) || (faceEdges[i] == -faceEdges[j])) {
                        return false;
                    }
                }
            }

            portal.SetFaceNum(this.file.faces.Num());

            face.planeNum = this.file.planeList.FindPlane(portal.GetPlane(), AAS_PLANE_NORMAL_EPSILON, AAS_PLANE_DIST_EPSILON);
            face.flags = portal.GetFlags();
            face.areas[0] = face.areas[1] = 0;
            face.firstEdge = this.file.edgeIndex.Num();
            face.numEdges = numFaceEdges;
            for (i = 0; i < numFaceEdges; i++) {
                this.file.edgeIndex.Append(faceEdges[i]);
            }
            if (side != 0) {
                faceNum[0] = -this.file.faces.Num();
            } else {
                faceNum[0] = this.file.faces.Num();
            }
            this.file.faces.Append(face);

            return true;
        }

        private boolean GetAreaForLeafNode(idBrushBSPNode node, int[] areaNum) {
            int s;
            final int[] faceNum = new int[1];
            idBrushBSPPortal p;
            final aasArea_s area = new aasArea_s();

            if (node.GetAreaNum() != 0) {
                areaNum[0] = -node.GetAreaNum();
                return true;
            }

            area.flags = node.GetFlags();
            area.cluster = area.clusterAreaNum = 0;
            area.contents = node.GetContents();
            area.firstFace = this.file.faceIndex.Num();
            area.numFaces = 0;
            area.reach = null;
            area.rev_reach = null;

            for (p = node.GetPortals(); p != null; p = p.Next(s)) {
                s = (p.GetNode(1).equals(node)) ? 1 : 0;

                if (!GetFaceForPortal(p, s, faceNum)) {
                    continue;
                }

                this.file.faceIndex.Append(faceNum[0]);
                area.numFaces++;

                if (faceNum[0] > 0) {
                    this.file.faces.oGet(abs(faceNum[0])).areas[0] = (short) this.file.areas.Num();
                } else {
                    this.file.faces.oGet(abs(faceNum[0])).areas[1] = (short) this.file.areas.Num();
                }
            }

            if (0 == area.numFaces) {
                areaNum[0] = 0;
                return false;
            }

            areaNum[0] = -this.file.areas.Num();
            node.SetAreaNum(this.file.areas.Num());
            this.file.areas.Append(area);

            DisplayRealTimeString("\r%6d", this.file.areas.Num());

            return true;
        }

        private int StoreTree_r(idBrushBSPNode node) {
            int nodeNum, child0, child1;
            final int[] areaNum = new int[1];
            final aasNode_s aasNode = new aasNode_s();

            if (NOT(node)) {
                return 0;
            }

            if ((node.GetContents() & AREACONTENTS_SOLID) != 0) {
                return 0;
            }

            if (NOT(node.GetChild(0)) && NOT(node.GetChild(1))) {
                if (GetAreaForLeafNode(node, areaNum)) {
                    return areaNum[0];
                }
                return 0;
            }

            aasNode.planeNum = this.file.planeList.FindPlane(node.GetPlane(), AAS_PLANE_NORMAL_EPSILON, AAS_PLANE_DIST_EPSILON);
            aasNode.children[0] = aasNode.children[1] = 0;
            nodeNum = this.file.nodes.Num();
            this.file.nodes.Append(aasNode);

            // !@#$%^ cause of some bug we cannot set the children directly with the StoreTree_r return value
            child0 = StoreTree_r(node.GetChild(0));
            this.file.nodes.oGet(nodeNum).children[0] = child0;
            child1 = StoreTree_r(node.GetChild(1));
            this.file.nodes.oGet(nodeNum).children[1] = child1;

            if ((0 == child0) && (0 == child1)) {
                this.file.nodes.SetNum(this.file.nodes.Num() - 1);
                return 0;
            }

            return nodeNum;
        }

        private void GetSizeEstimate_r(idBrushBSPNode parent, idBrushBSPNode node, sizeEstimate_s size) {
            idBrushBSPPortal p;
            int s;

            if (NOT(node)) {
                return;
            }

            if ((node.GetContents() & AREACONTENTS_SOLID) != 0) {
                return;
            }

            if (NOT(node.GetChild(0)) && NOT(node.GetChild(1))) {
                // multiple branches of the bsp tree might point to the same leaf node
                if (node.GetParent() == parent) {
                    size.numAreas++;
                    for (p = node.GetPortals(); p != null; p = p.Next(s)) {
                        s = (p.GetNode(1).equals(node)) ? 1 : 0;
                        size.numFaceIndexes++;
                        size.numEdgeIndexes += p.GetWinding().GetNumPoints();
                    }
                }
            } else {
                size.numNodes++;
            }

            GetSizeEstimate_r(node, node.GetChild(0), size);
            GetSizeEstimate_r(node, node.GetChild(1), size);
        }

        private void SetSizeEstimate(final idBrushBSP bsp, idAASFileLocal file) {
            final sizeEstimate_s size = new sizeEstimate_s();

            size.numEdgeIndexes = 1;
            size.numFaceIndexes = 1;
            size.numAreas = 1;
            size.numNodes = 1;

            GetSizeEstimate_r(null, bsp.GetRootNode(), size);

            file.planeList.Resize(size.numNodes / 2, 1024);
            file.vertices.Resize(size.numEdgeIndexes / 3, 1024);
            file.edges.Resize(size.numEdgeIndexes / 2, 1024);
            file.edgeIndex.Resize(size.numEdgeIndexes, 4096);
            file.faces.Resize(size.numFaceIndexes, 1024);
            file.faceIndex.Resize(size.numFaceIndexes, 4096);
            file.areas.Resize(size.numAreas, 1024);
            file.nodes.Resize(size.numNodes, 1024);
        }

        private boolean StoreFile(final idBrushBSP bsp) {
            aasEdge_s edge;
            aasFace_s face;
            aasArea_s area;
            aasNode_s node;

            common.Printf("[Store AAS]\n");

            SetupHash();
            ClearHash(bsp.GetTreeBounds());

            this.file = new idAASFileLocal();

            this.file.Clear();

            SetSizeEstimate(bsp, this.file);

            // the first edge is a dummy
//	memset( &edge, 0, sizeof( edge ) );
            edge = new aasEdge_s();
            this.file.edges.Append(edge);

            // the first face is a dummy
//	memset( &face, 0, sizeof( face ) );
            face = new aasFace_s();
            this.file.faces.Append(face);

            // the first area is a dummy
//	memset( &area, 0, sizeof( area ) );
            area = new aasArea_s();
            this.file.areas.Append(area);

            // the first node is a dummy
//	memset( &node, 0, sizeof( node ) );
            node = new aasNode_s();
            this.file.nodes.Append(node);

            // store the tree
            StoreTree_r(bsp.GetRootNode());

            // calculate area bounds and a reachable point in the area
            this.file.FinishAreas();

            ShutdownHash();

            common.Printf("\r%6d areas\n", this.file.areas.Num());

            return true;
        }
    }

    /*
     ============
     ParseOptions
     ============
     */
    static int ParseOptions(final idCmdArgs args, idAASSettings settings) {
        int i;
        idStr str;

        for (i = 1; i < args.Argc(); i++) {

            str = new idStr(args.Argv(i));
            str.StripLeading('-');

            if (str.Icmp("usePatches") == 0) {
                settings.usePatches[0] = true;
                common.Printf("usePatches = true\n");
            } else if (str.Icmp("writeBrushMap") == 0) {
                settings.writeBrushMap[0] = true;
                common.Printf("writeBrushMap = true\n");
            } else if (str.Icmp("playerFlood") == 0) {
                settings.playerFlood[0] = true;
                common.Printf("playerFlood = true\n");
            } else if (str.Icmp("noOptimize") == 0) {
                settings.noOptimize = true;
                common.Printf("noOptimize = true\n");
            }
        }
        return args.Argc() - 1;
    }

    /*
     ============
     RunAAS_f
     ============
     */
    public static class RunAAS_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new RunAAS_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            int i;
            final idAASBuild aas = new idAASBuild();
            final idAASSettings settings = new idAASSettings();
            idStr mapName;

            if (args.Argc() <= 1) {
                common.Printf("runAAS [options] <mapfile>\n"
                        + "options:\n"
                        + "  -usePatches        = use bezier patches for collision detection.\n"
                        + "  -writeBrushMap     = write a brush map with the AAS geometry.\n"
                        + "  -playerFlood       = use player spawn points as valid AAS positions.\n");
                return;
            }

            common.ClearWarnings("compiling AAS");

            common.SetRefreshOnPrint(true);

            // get the aas settings definitions
            final idDict dict = GameEdit.gameEdit.FindEntityDefDict("aas_types", false);
            if (NOT(dict)) {
                common.Error("Unable to find entityDef for 'aas_types'");
            }

            idKeyValue kv = dict.MatchPrefix("type");
            while (kv != null) {
                final idDict settingsDict = GameEdit.gameEdit.FindEntityDefDict(kv.GetValue().getData(), false);
                if (NOT(settingsDict)) {
                    common.Warning("Unable to find '%s' in def/aas.def", kv.GetValue());
                } else {
                    settings.FromDict(kv.GetValue().getData(), settingsDict);
                    i = ParseOptions(args, settings);
                    mapName = new idStr(args.Argv(i));
                    mapName.BackSlashesToSlashes();
                    if (mapName.Icmpn("maps/", 4) != 0) {
                        mapName.oSet("maps/" + mapName);
                    }
                    aas.Build(mapName, settings);
                }

                kv = dict.MatchPrefix("type", kv);
                if (kv != null) {
                    common.Printf("=======================================================\n");
                }
            }
            common.SetRefreshOnPrint(false);
            common.PrintWarnings();
        }
    }

    /*
     ============
     RunAASDir_f
     ============
     */
    public static class RunAASDir_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new RunAASDir_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            int i;
            final idAASBuild aas = new idAASBuild();
            final idAASSettings settings = new idAASSettings();
            idFileList mapFiles;

            if (args.Argc() <= 1) {
                common.Printf("runAASDir <folder>\n");
                return;
            }

            common.ClearWarnings("compiling AAS");

            common.SetRefreshOnPrint(true);

            // get the aas settings definitions
            final idDict dict = GameEdit.gameEdit.FindEntityDefDict("aas_types", false);
            if (NOT(dict)) {
                common.Error("Unable to find entityDef for 'aas_types'");
            }

            // scan for .map files
            mapFiles = fileSystem.ListFiles(new idStr("maps/") + args.Argv(1), ".map");

            // create AAS files for all the .map files
            for (i = 0; i < mapFiles.GetNumFiles(); i++) {
                if (i != 0) {
                    common.Printf("=======================================================\n");
                }

                idKeyValue kv = dict.MatchPrefix("type");
                while (kv != null) {
                    final idDict settingsDict = GameEdit.gameEdit.FindEntityDefDict(kv.GetValue().getData(), false);
                    if (NOT(settingsDict)) {
                        common.Warning("Unable to find '%s' in def/aas.def", kv.GetValue());
                    } else {
                        settings.FromDict(kv.GetValue().getData(), settingsDict);
                        aas.Build(new idStr("maps/" + args.Argv(1) + "/" + mapFiles.GetFile(i)), settings);
                    }

                    kv = dict.MatchPrefix("type", kv);
                    if (kv != null) {
                        common.Printf("=======================================================\n");
                    }
                }
            }

            fileSystem.FreeFileList(mapFiles);

            common.SetRefreshOnPrint(false);
            common.PrintWarnings();
        }
    }

    /*
     ============
     RunReach_f
     ============
     */
    public static class RunReach_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new RunReach_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            int i;
            final idAASBuild aas = new idAASBuild();
            final idAASSettings settings = new idAASSettings();

            if (args.Argc() <= 1) {
                common.Printf("runReach [options] <mapfile>\n");
                return;
            }

            common.ClearWarnings("calculating AAS reachability");

            common.SetRefreshOnPrint(true);

            // get the aas settings definitions
            final idDict dict = GameEdit.gameEdit.FindEntityDefDict("aas_types", false);
            if (NOT(dict)) {
                common.Error("Unable to find entityDef for 'aas_types'");
            }

            idKeyValue kv = dict.MatchPrefix("type");
            while (kv != null) {
                final idDict settingsDict = GameEdit.gameEdit.FindEntityDefDict(kv.GetValue().getData(), false);
                if (NOT(settingsDict)) {
                    common.Warning("Unable to find '%s' in def/aas.def", kv.GetValue());
                } else {
                    settings.FromDict(kv.GetValue().getData(), settingsDict);
                    i = ParseOptions(args, settings);
                    aas.BuildReachability(new idStr("maps/" + args.Argv(i)), settings);
                }

                kv = dict.MatchPrefix("type", kv);
                if (kv != null) {
                    common.Printf("=======================================================\n");
                }
            }

            common.SetRefreshOnPrint(false);
            common.PrintWarnings();
        }
    }

    /*
     ============
     MergeAllowed
     ============
     */
    static class MergeAllowed extends Allowance {

        static final Allowance INSTANCE = new MergeAllowed();

        private MergeAllowed() {
        }

        @Override
        public boolean run(idBrush b1, idBrush b2) {
            return ((b1.GetContents() == b2.GetContents()) && NOT((b1.GetFlags() | b2.GetFlags()) & BFL_PATCH));
        }
    }

    /*
     ============
     ExpandedChopAllowed
     ============
     */
    static class ExpandedChopAllowed extends Allowance {

        static final Allowance INSTANCE = new ExpandedChopAllowed();

        private ExpandedChopAllowed() {
        }

        @Override
        public boolean run(idBrush b1, idBrush b2) {
            return (b1.GetContents() == b2.GetContents());
        }
    }

    /*
     ============
     ExpandedMergeAllowed
     ============
     */
    static class ExpandedMergeAllowed extends Allowance {

        static final Allowance INSTANCE = new ExpandedMergeAllowed();

        private ExpandedMergeAllowed() {
        }

        @Override
        public boolean run(idBrush b1, idBrush b2) {
            return (b1.GetContents() == b2.GetContents());
        }
    }
}
