package neo.Tools.Compilers.AAS;

import static neo.TempDump.NOT;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_ENTITYDEF;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.Text.Token.TT_STRING;

import java.nio.IntBuffer;

import neo.Tools.Compilers.AAS.AASFile.aasEdge_s;
import neo.Tools.Compilers.AAS.AASFile.aasFace_s;
import neo.framework.DeclEntityDef.idDeclEntityDef;
import neo.framework.File_h.idFile;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Lib.idException;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.PlaneSet.idPlaneSet;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class AASFile {

    /*
     ===============================================================================

     AAS File

     ===============================================================================
     */
    public static final String AAS_FILEID = "DewmAAS";
    public static final String AAS_FILEVERSION = "1.07";
    //
    // travel flags
    public static final int TFL_INVALID = BIT(0);              // not valid
    public static final int TFL_WALK = BIT(1);                 // walking
    public static final int TFL_CROUCH = BIT(2);               // crouching
    public static final int TFL_WALKOFFLEDGE = 1 << 3;//BIT(3);// walking of a ledge
    public static final int TFL_BARRIERJUMP = 1 << 4;//BIT(4); // jumping onto a barrier
    public static final int TFL_JUMP = 1 << 5;//BIT(5);        // jumping
    public static final int TFL_LADDER = BIT(6);               // climbing a ladder
    public static final int TFL_SWIM = BIT(7);                 // swimming
    public static final int TFL_WATERJUMP = BIT(8);            // jump out of the water
    public static final int TFL_TELEPORT = BIT(9);             // teleportation
    public static final int TFL_ELEVATOR = BIT(10);            // travel by elevator
    public static final int TFL_FLY = BIT(11);                 // fly
    public static final int TFL_SPECIAL = BIT(12);             // special
    public static final int TFL_WATER = BIT(21);               // travel through water
    public static final int TFL_AIR = BIT(22);                 // travel through air
    //
    // face flags
    public static final int FACE_SOLID = BIT(0);               // solid at the other side
    public static final int FACE_LADDER = BIT(1);              // ladder surface
    public static final int FACE_FLOOR = BIT(2);               // standing on floor when on this face
    public static final int FACE_LIQUID = BIT(3);              // face seperating two areas with liquid
    public static final int FACE_LIQUIDSURFACE = BIT(4);       // face seperating liquid and air
    //
    // area flags
    public static final int AREA_FLOOR = BIT(0);               // AI can stand on the floor in this area
    public static final int AREA_GAP = BIT(1);                 // area has a gap
    public static final int AREA_LEDGE = BIT(2);               // if entered the AI bbox partly floats above a ledge
    public static final int AREA_LADDER = BIT(3);              // area contains one or more ladder faces
    public static final int AREA_LIQUID = BIT(4);              // area contains a liquid
    public static final int AREA_CROUCH = BIT(5);              // AI cannot walk but can only crouch in this area
    public static final int AREA_REACHABLE_WALK = BIT(6);	// area is reachable by walking or swimming
    public static final int AREA_REACHABLE_FLY = BIT(7);	// area is reachable by flying
    //
    // area contents flags
    public static final int AREACONTENTS_SOLID = BIT(0);	// solid, not a valid area
    public static final int AREACONTENTS_WATER = BIT(1);	// area contains water
    public static final int AREACONTENTS_CLUSTERPORTAL = BIT(2);// area is a cluster portal
    public static final int AREACONTENTS_OBSTACLE = BIT(3);	// area contains (part of) a dynamic obstacle
    public static final int AREACONTENTS_TELEPORTER = BIT(4);	// area contains (part of) a teleporter trigger
    //
    // bits for different bboxes
    public static final int AREACONTENTS_BBOX_BIT = 24;
    //
    public static final int MAX_REACH_PER_AREA = 256;
    public static final int MAX_AAS_TREE_DEPTH = 128;
    //
    public static final int MAX_AAS_BOUNDING_BOXES = 4;
    //

    // reachability to another area
    public static class idReachability {

        public int travelType;                              // type of travel required to get to the area
        public short toAreaNum;                             // number of the reachable area
        public short fromAreaNum;                           // number of area the reachability starts
        public idVec3 start;                                // start point of inter area movement
        public idVec3 end;                                  // end point of inter area movement
        public int edgeNum;                                 // edge crossed by this reachability
        public /*unsigned short*/ int travelTime;           // travel time of the inter area movement
        public byte number;                                 // reachability number within the fromAreaNum (must be < 256)
        public byte disableCount;                           // number of times this reachability has been disabled
        public idReachability next;                         // next reachability in list
        public idReachability rev_next;                     // next reachability in reversed list
        public IntBuffer areaTravelTimes;                 // travel times within the fromAreaNum from reachabilities that lead towards this area
        //
        //

        public idReachability() {
            this.start = new idVec3();
            this.end = new idVec3();
        }
        
        public void CopyBase(idReachability reach) {
            this.travelType = reach.travelType;
            this.toAreaNum = reach.toAreaNum;
            this.start = reach.start;
            this.end = reach.end;
            this.edgeNum = reach.edgeNum;
            this.travelTime = reach.travelTime;
        }
    }

    public static class idReachability_Walk extends idReachability {
    }

    public static class idReachability_BarrierJump extends idReachability {
    }

    public static class idReachability_WaterJump extends idReachability {
    }

    public static class idReachability_WalkOffLedge extends idReachability {
    }

    public static class idReachability_Swim extends idReachability {
    }

    public static class idReachability_Fly extends idReachability {
    }

    public static class idReachability_Special extends idReachability {

        public idDict dict;
    }

    // edge
    public static class aasEdge_s {

        public int[] vertexNum = new int[2];	// numbers of the vertexes of this edge
    }

    // area boundary face
    public static class aasFace_s {
        public int planeNum;            // number of the plane this face is on
        public int flags;               // face flags
        public int numEdges;            // number of edges in the boundary of the face
        public int firstEdge;           // first edge in the edge index
        public short[] areas = new short[2];    // area at the front and back of this face
    }

    // area with a boundary of faces
    public static class aasArea_s {

        public int            numFaces;      // number of faces used for the boundary of the area
        public int            firstFace;     // first face in the face index used for the boundary of the area
        public idBounds       bounds;        // bounds of the area
        public idVec3         center;        // center of the area an AI can move towards
        public int            flags;         // several area flags
        public int            contents;      // contents of the area
        public short          cluster;       // cluster the area belongs to, if negative it's a portal
        public short          clusterAreaNum;// number of the area in the cluster
        public int            travelFlags;   // travel flags for traveling through this area
        public idReachability reach;         // reachabilities that start from this area
        public idReachability rev_reach;     // reachabilities that lead to this area
    }

    // nodes of the bsp tree
    public static class aasNode_s {
        /*unsigned short*/

        public int planeNum;                // number of the plane that splits the subspace at this node
        public int[] children = new int[2]; // child nodes, zero is solid, negative is -(area number)
    }

    // cluster portal
    public static class aasPortal_s {

        public short areaNum;                   // number of the area that is the actual portal
        public short[] clusters       = new short[2];    // number of cluster at the front and back of the portal
        public short[] clusterAreaNum = new short[2];    // number of this portal area in the front and back cluster
        public int maxAreaTravelTime;           // maximum travel time through the portal area
    }

    // cluster
    public static class aasCluster_s {

        public int numAreas;             // number of areas in the cluster
        public int numReachableAreas;    // number of areas with reachabilities
        public int numPortals;             // number of cluster portals
        public int firstPortal;          // first cluster portal in the index
    }

    // trace through the world
    public static class aasTrace_s {
        // parameters

        public int      flags;           // areas with these flags block the trace
        public int      travelFlags;     // areas with these travel flags block the trace
        public int      maxAreas;        // size of the 'areas' array
        public int      getOutOfSolid;   // trace out of solid if the trace starts in solid
        // output
        public float    fraction;        // fraction of trace completed
        public idVec3   endpos;          // end position of trace
        public int      planeNum;        // plane hit
        public int      lastAreaNum;     // number of last area the trace went through
        public int      blockingAreaNum; // area that could not be entered
        public int      numAreas;        // number of areas the trace went through
        public int[]    areas;           // array to store areas the trace went through
        public idVec3[] points;          // points where the trace entered each new area

        public aasTrace_s() {
            this.areas = null;
            this.points = null;
            this.getOutOfSolid = //false;
                    this.flags = this.travelFlags = this.maxAreas = 0;
        }
    }

    /*
     ===============================================================================

     idAASSettings

     ===============================================================================
     */
    public static class idAASSettings {

        // collision settings
        public int numBoundingBoxes;
        public idBounds[] boundingBoxes = new idBounds[MAX_AAS_BOUNDING_BOXES];
        public boolean[]  usePatches    = {false};
        public boolean[]  writeBrushMap = {false};
        public boolean[]  playerFlood   = {false};
        public boolean noOptimize;
        public boolean[] allowSwimReachabilities = {false};
        public boolean[] allowFlyReachabilities  = {false};
        public idStr  fileExtension;
        // physics settings
        public idVec3 gravity;
        public idVec3 gravityDir;
        public idVec3 invGravityDir;
        public float  gravityValue;
        public float[] maxStepHeight        = {0};
        public float[] maxBarrierHeight     = {0};
        public float[] maxWaterJumpHeight   = {0};
        public float[] maxFallHeight        = {0};
        public float[] minFloorCos          = {0};
        // fixed travel times
        public int[]   tt_barrierJump       = {0};
        public int[]   tt_startCrouching    = {0};
        public int[]   tt_waterJump         = {0};
        public int[]   tt_startWalkOffLedge = {0};
        //
        //

        public idAASSettings() {
            this.numBoundingBoxes = 1;
            this.boundingBoxes[0] = new idBounds(new idVec3(-16, -16, 0), new idVec3(16, 16, 72));
            this.usePatches[0] = false;
            this.writeBrushMap[0] = false;
            this.playerFlood[0] = false;
            this.noOptimize = false;
            this.allowSwimReachabilities[0] = false;
            this.allowFlyReachabilities[0] = false;
            this.fileExtension = new idStr("aas48");
            // physics settings
            this.gravity = new idVec3(0, 0, -1066);
            this.gravityDir = this.gravity;
            this.gravityValue = this.gravityDir.Normalize();
            this.invGravityDir = this.gravityDir.oNegative();
            this.maxStepHeight[0] = 14.0f;
            this.maxBarrierHeight[0] = 32.0f;
            this.maxWaterJumpHeight[0] = 20.0f;
            this.maxFallHeight[0] = 64.0f;
            this.minFloorCos[0] = 0.7f;
            // fixed travel times
            this.tt_barrierJump[0] = 100;
            this.tt_startCrouching[0] = 100;
            this.tt_waterJump[0] = 100;
            this.tt_startWalkOffLedge[0] = 100;
        }

        public boolean FromFile(final idStr fileName) throws idException {
            final idLexer src = new idLexer(LEXFL_ALLOWPATHNAMES | LEXFL_NOSTRINGESCAPECHARS | LEXFL_NOSTRINGCONCAT);
            idStr name;

            name = fileName;

            common.Printf("loading %s\n", name);

            if (!src.LoadFile(name.getData())) {
                common.Error("WARNING: couldn't load %s\n", name);
                return false;
            }

            if (!src.ExpectTokenString("settings")) {
                common.Error("%s is not a settings file", name);
                return false;
            }

            if (!FromParser(src)) {
                common.Error("failed to parse %s", name);
                return false;
            }

            return true;
        }

        public boolean FromParser(idLexer src) throws idException {
            final idToken token = new idToken();

            if (!src.ExpectTokenString("{")) {
                return false;
            }

            // parse the file
            while (true) {
                if (!src.ReadToken(token)) {
                    break;
                }

                if (token.equals("}")) {
                    break;
                }

                if (token.equals("bboxes")) {
                    if (!ParseBBoxes(src)) {
                        return false;
                    }
                } else if (token.equals("usePatches")) {
                    if (!ParseBool(src, this.usePatches)) {
                        return false;
                    }
                } else if (token.equals("writeBrushMap")) {
                    if (!ParseBool(src, this.writeBrushMap)) {
                        return false;
                    }
                } else if (token.equals("playerFlood")) {
                    if (!ParseBool(src, this.playerFlood)) {
                        return false;
                    }
                } else if (token.equals("allowSwimReachabilities")) {
                    if (!ParseBool(src, this.allowSwimReachabilities)) {
                        return false;
                    }
                } else if (token.equals("allowFlyReachabilities")) {
                    if (!ParseBool(src, this.allowFlyReachabilities)) {
                        return false;
                    }
                } else if (token.equals("fileExtension")) {
                    src.ExpectTokenString("=");
                    src.ExpectTokenType(TT_STRING, 0, token);
                    this.fileExtension = token;
                } else if (token.equals("gravity")) {
                    ParseVector(src, this.gravity);
                    this.gravityDir = this.gravity;
                    this.gravityValue = this.gravityDir.Normalize();
                    this.invGravityDir = this.gravityDir.oNegative();
                } else if (token.equals("maxStepHeight")) {
                    if (!ParseFloat(src, this.maxStepHeight)) {
                        return false;
                    }
                } else if (token.equals("maxBarrierHeight")) {
                    if (!ParseFloat(src, this.maxBarrierHeight)) {
                        return false;
                    }
                } else if (token.equals("maxWaterJumpHeight")) {
                    if (!ParseFloat(src, this.maxWaterJumpHeight)) {
                        return false;
                    }
                } else if (token.equals("maxFallHeight")) {
                    if (!ParseFloat(src, this.maxFallHeight)) {
                        return false;
                    }
                } else if (token.equals("minFloorCos")) {
                    if (!ParseFloat(src, this.minFloorCos)) {
                        return false;
                    }
                } else if (token.equals("tt_barrierJump")) {
                    if (!ParseInt(src, this.tt_barrierJump)) {
                        return false;
                    }
                } else if (token.equals("tt_startCrouching")) {
                    if (!ParseInt(src, this.tt_startCrouching)) {
                        return false;
                    }
                } else if (token.equals("tt_waterJump")) {
                    if (!ParseInt(src, this.tt_waterJump)) {
                        return false;
                    }
                } else if (token.equals("tt_startWalkOffLedge")) {
                    if (!ParseInt(src, this.tt_startWalkOffLedge)) {
                        return false;
                    }
                } else {
                    src.Error("invalid token '%s'", token);
                }
            }

            if (this.numBoundingBoxes <= 0) {
                src.Error("no valid bounding box");
            }

            return true;
        }

        public boolean FromDict(final String name, final idDict dict) {
            final idBounds bounds = new idBounds();

            if (!dict.GetVector("mins", "0 0 0", bounds.oGet(0))) {
                common.Error("Missing 'mins' in entityDef '%s'", name);
            }
            if (!dict.GetVector("maxs", "0 0 0", bounds.oGet(1))) {
                common.Error("Missing 'maxs' in entityDef '%s'", name);
            }

            this.numBoundingBoxes = 1;
            this.boundingBoxes[0] = bounds;

            if (!dict.GetBool("usePatches", "0", this.usePatches)) {
                common.Error("Missing 'usePatches' in entityDef '%s'", name);
            }

            if (!dict.GetBool("writeBrushMap", "0", this.writeBrushMap)) {
                common.Error("Missing 'writeBrushMap' in entityDef '%s'", name);
            }

            if (!dict.GetBool("playerFlood", "0", this.playerFlood)) {
                common.Error("Missing 'playerFlood' in entityDef '%s'", name);
            }

            if (!dict.GetBool("allowSwimReachabilities", "0", this.allowSwimReachabilities)) {
                common.Error("Missing 'allowSwimReachabilities' in entityDef '%s'", name);
            }

            if (!dict.GetBool("allowFlyReachabilities", "0", this.allowFlyReachabilities)) {
                common.Error("Missing 'allowFlyReachabilities' in entityDef '%s'", name);
            }

            if (!dict.GetString("fileExtension", "", this.fileExtension)) {
                common.Error("Missing 'fileExtension' in entityDef '%s'", name);
            }

            if (!dict.GetVector("gravity", "0 0 -1066", this.gravity)) {
                common.Error("Missing 'gravity' in entityDef '%s'", name);
            }
            this.gravityDir = this.gravity;
            this.gravityValue = this.gravityDir.Normalize();
            this.invGravityDir = this.gravityDir.oNegative();

            if (!dict.GetFloat("maxStepHeight", "0", this.maxStepHeight)) {
                common.Error("Missing 'maxStepHeight' in entityDef '%s'", name);
            }

            if (!dict.GetFloat("maxBarrierHeight", "0", this.maxBarrierHeight)) {
                common.Error("Missing 'maxBarrierHeight' in entityDef '%s'", name);
            }

            if (!dict.GetFloat("maxWaterJumpHeight", "0", this.maxWaterJumpHeight)) {
                common.Error("Missing 'maxWaterJumpHeight' in entityDef '%s'", name);
            }

            if (!dict.GetFloat("maxFallHeight", "0", this.maxFallHeight)) {
                common.Error("Missing 'maxFallHeight' in entityDef '%s'", name);
            }

            if (!dict.GetFloat("minFloorCos", "0", this.minFloorCos)) {
                common.Error("Missing 'minFloorCos' in entityDef '%s'", name);
            }

            if (!dict.GetInt("tt_barrierJump", "0", this.tt_barrierJump)) {
                common.Error("Missing 'tt_barrierJump' in entityDef '%s'", name);
            }

            if (!dict.GetInt("tt_startCrouching", "0", this.tt_startCrouching)) {
                common.Error("Missing 'tt_startCrouching' in entityDef '%s'", name);
            }

            if (!dict.GetInt("tt_waterJump", "0", this.tt_waterJump)) {
                common.Error("Missing 'tt_waterJump' in entityDef '%s'", name);
            }

            if (!dict.GetInt("tt_startWalkOffLedge", "0", this.tt_startWalkOffLedge)) {
                common.Error("Missing 'tt_startWalkOffLedge' in entityDef '%s'", name);
            }

            return true;
        }

        public boolean WriteToFile(idFile fp) {
            int i;

            fp.WriteFloatString("{\n");
            fp.WriteFloatString("\tbboxes\n\t{\n");
            for (i = 0; i < this.numBoundingBoxes; i++) {
                fp.WriteFloatString("\t\t(%f %f %f)-(%f %f %f)\n",
                        this.boundingBoxes[i].oGet(0).x, this.boundingBoxes[i].oGet(0).y, this.boundingBoxes[i].oGet(0).z,
                        this.boundingBoxes[i].oGet(1).x, this.boundingBoxes[i].oGet(1).y, this.boundingBoxes[i].oGet(1).z);
            }
            fp.WriteFloatString("\t}\n");
            fp.WriteFloatString("\tusePatches = %d\n", this.usePatches[0]);
            fp.WriteFloatString("\twriteBrushMap = %d\n", this.writeBrushMap[0]);
            fp.WriteFloatString("\tplayerFlood = %d\n", this.playerFlood[0]);
            fp.WriteFloatString("\tallowSwimReachabilities = %d\n", this.allowSwimReachabilities[0]);
            fp.WriteFloatString("\tallowFlyReachabilities = %d\n", this.allowFlyReachabilities[0]);
            fp.WriteFloatString("\tfileExtension = \"%s\"\n", this.fileExtension);
            fp.WriteFloatString("\tgravity = (%f %f %f)\n", this.gravity.x, this.gravity.y, this.gravity.z);
            fp.WriteFloatString("\tmaxStepHeight = %f\n", this.maxStepHeight[0]);
            fp.WriteFloatString("\tmaxBarrierHeight = %f\n", this.maxBarrierHeight[0]);
            fp.WriteFloatString("\tmaxWaterJumpHeight = %f\n", this.maxWaterJumpHeight[0]);
            fp.WriteFloatString("\tmaxFallHeight = %f\n", this.maxFallHeight[0]);
            fp.WriteFloatString("\tminFloorCos = %f\n", this.minFloorCos[0]);
            fp.WriteFloatString("\ttt_barrierJump = %d\n", this.tt_barrierJump[0]);
            fp.WriteFloatString("\ttt_startCrouching = %d\n", this.tt_startCrouching[0]);
            fp.WriteFloatString("\ttt_waterJump = %d\n", this.tt_waterJump[0]);
            fp.WriteFloatString("\ttt_startWalkOffLedge = %d\n", this.tt_startWalkOffLedge[0]);
            fp.WriteFloatString("}\n");
            return true;
        }

        public boolean ValidForBounds(final idBounds bounds) {
            int i;

            for (i = 0; i < 3; i++) {
                if (bounds.oGet(0, i) < this.boundingBoxes[0].oGet(0, i)) {
                    return false;
                }
                if (bounds.oGet(1, i) > this.boundingBoxes[0].oGet(1, i)) {
                    return false;
                }
            }
            return true;
        }

        public boolean ValidEntity(final String classname) {
            final idStr use_aas = new idStr();
            final idVec3 size = new idVec3();
            final idBounds bounds = new idBounds();

            if (this.playerFlood[0]) {
                if (classname.equals("info_player_start") || classname.equals("info_player_deathmatch") || classname.equals("func_teleporter")) {
                    return true;
                }
            }

            final idDeclEntityDef decl = (idDeclEntityDef) declManager.FindType(DECL_ENTITYDEF, classname, false);
            if ((decl != null) && decl.dict.GetString("use_aas", null, use_aas) && NOT(this.fileExtension.Icmp(use_aas))) {
                if (decl.dict.GetVector("mins", null, bounds.oGet(0))) {
                    decl.dict.GetVector("maxs", null, bounds.oGet(1));
                } else if (decl.dict.GetVector("size", null, size)) {
                    bounds.oGet(0).Set(size.x * -0.5f, size.y * -0.5f, 0.0f);
                    bounds.oGet(1).Set(size.x * 0.5f, size.y * 0.5f, size.z);
                }

                if (!ValidForBounds(bounds)) {
                    common.Error("%s cannot use %s\n", classname, this.fileExtension);
                }

                return true;
            }

            return false;
        }

        private boolean ParseBool(idLexer src, boolean[] b) {
            if (!src.ExpectTokenString("=")) {
                return false;
            }
            b[0] = src.ParseBool();
            return true;
        }

        private boolean ParseInt(idLexer src, int[] i) {
            if (!src.ExpectTokenString("=")) {
                return false;
            }
            i[0] = src.ParseInt();
            return true;
        }

        private boolean ParseFloat(idLexer src, float[] f) {
            if (!src.ExpectTokenString("=")) {
                return false;
            }
            f[0] = src.ParseFloat();
            return true;
        }

        private boolean ParseVector(idLexer src, idVec3 vec) {
            if (!src.ExpectTokenString("=")) {
                return false;
            }
            return src.Parse1DMatrix(3, vec);
        }

        private boolean ParseBBoxes(idLexer src) {
            final idToken token = new idToken();
            final idBounds bounds = new idBounds();

            this.numBoundingBoxes = 0;

            if (!src.ExpectTokenString("{")) {
                return false;
            }
            while (src.ReadToken(token)) {
                if (token.equals("}")) {
                    return true;
                }
                src.UnreadToken(token);
                src.Parse1DMatrix(3, bounds.oGet(0));
                if (!src.ExpectTokenString("-")) {
                    return false;
                }
                src.Parse1DMatrix(3, bounds.oGet(1));

                this.boundingBoxes[this.numBoundingBoxes++] = bounds;
            }
            return false;
        }
    }

    /*

     -	when a node child is a solid leaf the node child number is zero
     -	two adjacent areas (sharing a plane at opposite sides) share a face
     this face is a portal between the areas
     -	when an area uses a face from the faceindex with a positive index
     then the face plane normal points into the area
     -	the face edges are stored counter clockwise using the edgeindex
     -	two adjacent convex areas (sharing a face) only share One face
     this is a simple result of the areas being convex
     -	the areas can't have a mixture of ground and gap faces
     other mixtures of faces in one area are allowed
     -	areas with the AREACONTENTS_CLUSTERPORTAL in the settings have
     the cluster number set to the negative portal number
     -	edge zero is a dummy
     -	face zero is a dummy
     -	area zero is a dummy
     -	node zero is a dummy
     -	portal zero is a dummy
     -	cluster zero is a dummy

     */
    public static abstract class idAASFile {

        protected idStr                         name;
        protected long/*unsigned int*/          crc;
        //
        protected idPlaneSet                    planeList;
        protected idList<idVec3/*aasVertex_t*/> vertices;
        protected idList<aasEdge_s>             edges;
        protected idList<Integer/*aasIndex_t*/> edgeIndex;
        protected idList<aasFace_s>             faces;
        protected idList<Integer/*aasIndex_t*/> faceIndex;
        protected idList<aasArea_s>             areas;
        protected idList<aasNode_s>             nodes;
        protected idList<aasPortal_s>           portals;
        protected idList<Integer/*aasIndex_t*/> portalIndex;
        protected idList<aasCluster_s>          clusters;
        protected idAASSettings                 settings;
        //
        //

        protected idAASFile() {
            this.name = new idStr();

            this.planeList = new idPlaneSet();
            this.vertices = new idList<>();
            this.edges = new idList<>();
            this.edgeIndex = new idList<>();
            this.faces = new idList<>();
            this.faceIndex = new idList<>();
            this.areas = new idList<>();
            this.nodes = new idList<>();
            this.portals = new idList<>();
            this.portalIndex = new idList<>();
            this.clusters = new idList<>();
            this.settings = new idAASSettings();
        }

        // virtual 					~idAASFile() {}
        public String GetName() {
            return this.name.getData();
        }

        public long/*unsigned int*/ GetCRC() {
            return this.crc;
        }

        public int GetNumPlanes() {
            return this.planeList.Num();
        }

        public idPlane GetPlane(int index) {
            return this.planeList.oGet(index);
        }

        public int GetNumVertices() {
            return this.vertices.Num();
        }

        public idVec3/*aasVertex_t*/ GetVertex(int index) {
            return this.vertices.oGet(index);
        }

        public int GetNumEdges() {
            return this.edges.Num();
        }

        public aasEdge_s GetEdge(int index) {
            return this.edges.oGet(index);
        }

        public int GetNumEdgeIndexes() {
            return this.edgeIndex.Num();
        }

        public int/*aasIndex_t*/ GetEdgeIndex(int index) {
            return this.edgeIndex.oGet(index);
        }

        public int GetNumFaces() {
            return this.faces.Num();
        }

        public aasFace_s GetFace(int index) {
            return this.faces.oGet(index);
        }

        public int GetNumFaceIndexes() {
            return this.faceIndex.Num();
        }

        public int/*aasIndex_t*/ GetFaceIndex(int index) {
            return this.faceIndex.oGet(index);
        }

        public int GetNumAreas() {
            return this.areas.Num();
        }

        public aasArea_s GetArea(int index) {
            return this.areas.oGet(index);
        }

        public int GetNumNodes() {
            return this.nodes.Num();
        }

        public aasNode_s GetNode(int index) {
            return this.nodes.oGet(index);
        }

        public int GetNumPortals() {
            return this.portals.Num();
        }

        public aasPortal_s GetPortal(int index) {
            return this.portals.oGet(index);
        }

        public int GetNumPortalIndexes() {
            return this.portalIndex.Num();
        }

        public int/*aasIndex_t*/ GetPortalIndex(int index) {
            return this.portalIndex.oGet(index);
        }

        public int GetNumClusters() {
            return this.clusters.Num();
        }

        public aasCluster_s GetCluster(int index) {
            return this.clusters.oGet(index);
        }

        public idAASSettings GetSettings() {
            return this.settings;
        }

        public void SetPortalMaxTravelTime(int index, int time) {
            this.portals.oGet(index).maxAreaTravelTime = time;
        }

        public void SetAreaTravelFlag(int index, int flag) {
            this.areas.oGet(index).travelFlags |= flag;
        }

        public void RemoveAreaTravelFlag(int index, int flag) {
            this.areas.oGet(index).travelFlags &= ~flag;
        }

        public abstract idVec3 EdgeCenter(int edgeNum);

        public abstract idVec3 FaceCenter(int faceNum);

        public abstract idVec3 AreaCenter(int areaNum);

        //
        public abstract idBounds EdgeBounds(int edgeNum);

        public abstract idBounds FaceBounds(int faceNum);

        public abstract idBounds AreaBounds(int areaNum);

        //
        public abstract int PointAreaNum(final idVec3 origin);

        public abstract int PointReachableAreaNum(final idVec3 origin, final idBounds searchBounds, final int areaFlags, final int excludeTravelFlags);

        public abstract int BoundsReachableAreaNum(final idBounds bounds, final int areaFlags, final int excludeTravelFlags);

        public abstract void PushPointIntoAreaNum(int areaNum, idVec3 point);

        public abstract boolean Trace(aasTrace_s trace, final idVec3 start, final idVec3 end);

        public abstract void PrintInfo();
    }

    /*
     ================
     Reachability_Write
     ================
     */
    static boolean Reachability_Write(idFile fp, idReachability reach) {
        fp.WriteFloatString("\t\t%d %d (%f %f %f) (%f %f %f) %d %d",
                reach.travelType, (int) reach.toAreaNum, reach.start.x, reach.start.y, reach.start.z,
                reach.end.x, reach.end.y, reach.end.z, reach.edgeNum, reach.travelTime);
        return true;
    }

    /*
     ================
     Reachability_Read
     ================
     */
    static boolean Reachability_Read(idLexer src, idReachability reach) {
        reach.travelType = src.ParseInt();
        reach.toAreaNum = (short) src.ParseInt();
        src.Parse1DMatrix(3, reach.start);
        src.Parse1DMatrix(3, reach.end);
        reach.edgeNum = src.ParseInt();
        reach.travelTime = src.ParseInt();
        return true;
    }

    /*
     ================
     Reachability_Special_Write
     ================
     */
    static boolean Reachability_Special_Write(idFile fp, idReachability_Special reach) {
        int i;
        idKeyValue keyValue;

        fp.WriteFloatString("\n\t\t{\n");
        for (i = 0; i < reach.dict.GetNumKeyVals(); i++) {
            keyValue = reach.dict.GetKeyVal(i);
            fp.WriteFloatString("\t\t\t\"%s\" \"%s\"\n", keyValue.GetKey().getData(), keyValue.GetValue().getData());
        }
        fp.WriteFloatString("\t\t}\n");

        return true;
    }

    /*
     ================
     Reachability_Special_Read
     ================
     */
    static boolean Reachability_Special_Read(idLexer src, idReachability_Special reach) {
        final idToken key = new idToken();
        final idToken value = new idToken();

        src.ExpectTokenString("{");
        while (src.ReadToken(key)) {
            if (key.equals("}")) {
                return true;
            }
            src.ExpectTokenType(TT_STRING, 0, value);
            reach.dict.Set(key, value);
        }
        return false;
    }

}
