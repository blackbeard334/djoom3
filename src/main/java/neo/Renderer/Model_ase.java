package neo.Renderer;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import neo.Renderer.Model_ase.aseFace_t;
import neo.Renderer.Model_ase.aseMaterial_t;
import neo.Renderer.Model_ase.aseMesh_t;
import neo.Renderer.Model_ase.aseObject_t;
import static neo.TempDump.bbtocb;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.replaceByIndex;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Model_ase {

    public static ase_t ase;

    /*
     ===============================================================================

     ASE loader. (3D Studio Max ASCII Export)

     ===============================================================================
     */
    public static class aseFace_t {

        public final int[] vertexNum = new int[3];
        public final int[] tVertexNum = new int[3];
        public idVec3 faceNormal;
        public final idVec3[] vertexNormals = new idVec3[3];
        public final byte[][] vertexColors = new byte[3][4];
    };

    public static class aseMesh_t {

        int timeValue;
        int numVertexes;
        int numTVertexes;
        int numCVertexes;
        int numFaces;
        int numTVFaces;
        int numCVFaces;
//
        final idVec3[] transform = new idVec3[4];			// applied to normals
//
        boolean colorsParsed;
        boolean normalsParsed;
        idVec3[] vertexes;
        idVec2[] tvertexes;
        idVec3[] cvertexes;
        aseFace_t[] faces;
    };

    public static class aseMaterial_t {

        final char[] name = new char[128];
//        String name;
        float uOffset, vOffset;		// max lets you offset by material without changing texCoords
        float uTiling, vTiling;		// multiply tex coords by this
        float angle;					// in clockwise radians
    };

    public static class aseObject_t {

//	char					name[128];
        char[] name;
        int materialRef;
//
        aseMesh_t mesh;
//
        // frames are only present with animations
        idList<aseMesh_t> frames;			// aseMesh_t
    };

    public static class aseModel_s {

//	ID_TIME_T					timeStamp;
        final long[] timeStamp = new long[1];
        idList<aseMaterial_t> materials;
        idList<aseObject_t> objects;
    };

    /*
     =================
     ASE_Load
     =================
     */
    public static aseModel_s ASE_Load(final String fileName) {
        final ByteBuffer[] buf = {null};
        final long[] timeStamp = new long[1];
        aseModel_s ase;

        fileSystem.ReadFile(fileName, buf, timeStamp);
        if (null == buf) {
            return null;
        }

        ase = ASE_Parse(buf[0], false);
        ase.timeStamp[0] = timeStamp[0];

        fileSystem.FreeFile(buf);

        return ase;
    }

    /*
     =================
     ASE_Free
     =================
     */
    public static void ASE_Free(aseModel_s ase) {
        int i, j;
        aseObject_t obj;
        aseMesh_t mesh;
        aseMaterial_t material;

        if (null == ase) {
            return;
        }
        for (i = 0; i < ase.objects.Num(); i++) {
            obj = ase.objects.oGet(i);
            for (j = 0; j < obj.frames.Num(); j++) {
                mesh = obj.frames.oGet(j);
                if (mesh.vertexes != null) {
//                    Mem_Free(mesh.vertexes);
                    mesh.vertexes = null;
                }
                if (mesh.tvertexes != null) {
//                    Mem_Free(mesh.tvertexes);
                    mesh.tvertexes = null;
                }
                if (mesh.cvertexes != null) {
//                    Mem_Free(mesh.cvertexes);
                    mesh.cvertexes = null;
                }
                if (mesh.faces != null) {
//                    Mem_Free(mesh.faces);
                    mesh.faces = null;
                }
//                Mem_Free(mesh);
                mesh = null;
            }

            obj.frames.Clear();

            // free the base nesh
            mesh = obj.mesh;
            if (mesh.vertexes != null) {
//                Mem_Free(mesh.vertexes);
                mesh.vertexes = null;
            }
            if (mesh.tvertexes != null) {
//                Mem_Free(mesh.tvertexes);
                mesh.tvertexes = null;
            }
            if (mesh.cvertexes != null) {
//                Mem_Free(mesh.cvertexes);
                mesh.cvertexes = null;
            }
            if (mesh.faces != null) {
//                Mem_Free(mesh.faces);
                mesh.faces = null;
            }
//            Mem_Free(obj);
            obj = null;
        }
        ase.objects.Clear();

        for (i = 0; i < ase.materials.Num(); i++) {
//            material = ase.materials.oGet(i);
//            Mem_Free(material);
            ase.materials.oSet(i, null);
        }
        ase.materials.Clear();

//	delete ase;
    }

    /*
     ======================================================================

     Parses 3D Studio Max ASCII export files.
     The goal is to parse the information into memory exactly as it is
     represented in the file.  Users of the data will then move it
     into a form that is more convenient for them.

     ======================================================================
     */
    public static void VERBOSE(String fmt, Object... x) {
        if (ase.verbose) {
            common.Printf(fmt, x);
        }
    }

    // working variables used during parsing
    public static class ase_t {

        CharBuffer buffer;
        int curpos;
        int len;
//        final char[] token = new char[1024];
        String token;
//
        boolean verbose;
//
        aseModel_s model;
        aseObject_t currentObject;
        aseMesh_t currentMesh;
        aseMaterial_t currentMaterial;
        int currentFace;
        int currentVertex;
    };

    public static aseMesh_t ASE_GetCurrentMesh() {
        return ase.currentMesh;
    }

    public static boolean CharIsTokenDelimiter(int ch) {
        if (ch <= 32) {
            return true;
        }
        return false;
    }

    public static boolean ASE_GetToken(boolean restOfLine) {
        int i = 0;

        if (ase.buffer == null) {
            return false;
        }

        if (ase.curpos == ase.len) {
            return false;
        }

        // skip over crap
        while ((ase.curpos < ase.len)
                && (ase.curpos <= 32)) {
            ase.curpos++;
        }

        while (ase.curpos < ase.len) {
//            ase.token[i] = *ase.curpos;
            ase.token = replaceByIndex(ase.buffer.get(ase.curpos), i, ase.token);

            ase.curpos++;
            i++;

            if ((CharIsTokenDelimiter(ase.token.charAt(i - 1)) && !restOfLine)
                    || ((ase.token.charAt(i - 1) == '\n') || (ase.token.charAt(i - 1) == '\r'))) {
                ase.token = replaceByIndex((char) 0, i - 1, ase.token);
                break;
            }
        }

        ase.token = replaceByIndex((char) 0, i, ase.token);

        return true;
    }

    /**
     *
     *
     *
     */
    public static void ASE_ParseBracedBlock(final ASE parser) {
        int indent = 0;

        while (ASE_GetToken(false)) {
            if ("{".equals(ase.token)) {
                indent++;
            } else if ("}".equals(ase.token)) {
                --indent;
                if (indent == 0) {
                    break;
                } else if (indent < 0) {
                    common.Error("Unexpected '}'");
                }
            } else {
                if (parser != null) {
                    parser.run(ase.token);
                }
            }
        }
    }

    public static void ASE_SkipEnclosingBraces() {
        int indent = 0;

        while (ASE_GetToken(false)) {
            if ("{".equals(ase.token)) {
                indent++;
            } else if ("}".equals(ase.token)) {
                indent--;
                if (indent == 0) {
                    break;
                } else if (indent < 0) {
                    common.Error("Unexpected '}'");
                }
            }
        }
    }

    public static void ASE_SkipRestOfLine() {
        ASE_GetToken(true);
    }

    public static abstract class ASE {

        public abstract void run(final String token);
    };

    public static class ASE_KeyMAP_DIFFUSE extends ASE {

        private static final ASE instance = new ASE_KeyMAP_DIFFUSE();

        private ASE_KeyMAP_DIFFUSE() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMaterial_t material;

            if ("*BITMAP".equals(token)) {
                idStr qpath;
                idStr matname;

                ASE_GetToken(false);

                // remove the quotes
                int s = ase.token.substring(1).indexOf("\"");
                if (s != 0) {
                    ase.token = replaceByIndex((char) 0, s, ase.token);
                }
                matname = new idStr(ase.token.substring(1));

                // convert the 3DSMax material pathname to a qpath
                matname.BackSlashesToSlashes();
                qpath = new idStr(fileSystem.OSPathToRelativePath(matname.toString()));
                idStr.Copynz(ase.currentMaterial.name, qpath.toString(), ase.currentMaterial.name.length);
            } else if ("*UVW_U_OFFSET".equals(token)) {
                material = ase.model.materials.oGet(ase.model.materials.Num() - 1);
                ASE_GetToken(false);
                material.uOffset = Float.parseFloat(ase.token);
            } else if ("*UVW_V_OFFSET".equals(token)) {
                material = ase.model.materials.oGet(ase.model.materials.Num() - 1);
                ASE_GetToken(false);
                material.vOffset = Float.parseFloat(ase.token);
            } else if ("*UVW_U_TILING".equals(token)) {
                material = ase.model.materials.oGet(ase.model.materials.Num() - 1);
                ASE_GetToken(false);
                material.uTiling = Float.parseFloat(ase.token);
            } else if ("*UVW_V_TILING".equals(token)) {
                material = ase.model.materials.oGet(ase.model.materials.Num() - 1);
                ASE_GetToken(false);
                material.vTiling = Float.parseFloat(ase.token);
            } else if ("*UVW_ANGLE".equals(token)) {
                material = ase.model.materials.oGet(ase.model.materials.Num() - 1);
                ASE_GetToken(false);
                material.angle = Float.parseFloat(ase.token);
            } else {
            }
        }
    };

    public static class ASE_KeyMATERIAL extends ASE {

        private static final ASE instance = new ASE_KeyMATERIAL();

        private ASE_KeyMATERIAL() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            {
                if ("*MAP_DIFFUSE".equals(token)) {
                    ASE_ParseBracedBlock(ASE_KeyMAP_DIFFUSE.getInstance());
                } else {
                }
            }
        }
    };

    public static class ASE_KeyMATERIAL_LIST extends ASE {

        private static final ASE instance = new ASE_KeyMATERIAL_LIST();

        private ASE_KeyMATERIAL_LIST() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            if ("*MATERIAL_COUNT".equals(token)) {
                ASE_GetToken(false);
                VERBOSE("..num materials: %s\n", ase.token);
            } else if ("*MATERIAL".equals(token)) {
                VERBOSE("..material %d\n", ase.model.materials.Num());

//                ase.currentMaterial = (aseMaterial_t) Mem_Alloc(sizeof(aseMaterial_t));
//                memset(ase.currentMaterial, 0, sizeof(aseMaterial_t));
                ase.currentMaterial = new aseMaterial_t();
                ase.currentMaterial.uTiling = 1;
                ase.currentMaterial.vTiling = 1;
                ase.model.materials.Append(ase.currentMaterial);

                ASE_ParseBracedBlock(ASE_KeyMATERIAL.getInstance());
            }
        }
    };

    public static class ASE_KeyNODE_TM extends ASE {

        private static final ASE instance = new ASE_KeyNODE_TM();

        private ASE_KeyNODE_TM() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            int i;

            if ("*TM_ROW0".equals(token)) {
                for (i = 0; i < 3; i++) {
                    ASE_GetToken(false);
                    ase.currentObject.mesh.transform[0].oSet(i, Float.parseFloat(ase.token));
                }
            } else if ("*TM_ROW1".equals(token)) {
                for (i = 0; i < 3; i++) {
                    ASE_GetToken(false);
                    ase.currentObject.mesh.transform[1].oSet(i, Float.parseFloat(ase.token));
                }
            } else if ("*TM_ROW2".equals(token)) {
                for (i = 0; i < 3; i++) {
                    ASE_GetToken(false);
                    ase.currentObject.mesh.transform[2].oSet(i, Float.parseFloat(ase.token));
                }
            } else if ("*TM_ROW3".equals(token)) {
                for (i = 0; i < 3; i++) {
                    ASE_GetToken(false);
                    ase.currentObject.mesh.transform[3].oSet(i, Float.parseFloat(ase.token));
                }
            }
        }
    };

    public static class ASE_KeyMESH_VERTEX_LIST extends ASE {

        private static final ASE instance = new ASE_KeyMESH_VERTEX_LIST();

        private ASE_KeyMESH_VERTEX_LIST() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            {
                aseMesh_t pMesh = ASE_GetCurrentMesh();

                if ("*MESH_VERTEX".equals(token)) {
                    ASE_GetToken(false);		// skip number

                    ASE_GetToken(false);
                    pMesh.vertexes[ase.currentVertex].x = Float.parseFloat(ase.token);

                    ASE_GetToken(false);
                    pMesh.vertexes[ase.currentVertex].y = Float.parseFloat(ase.token);

                    ASE_GetToken(false);
                    pMesh.vertexes[ase.currentVertex].z = Float.parseFloat(ase.token);

                    ase.currentVertex++;

                    if (ase.currentVertex > pMesh.numVertexes) {
                        common.Error("ase.currentVertex >= pMesh.numVertexes");
                    }
                } else {
                    common.Error("Unknown token '%s' while parsing MESH_VERTEX_LIST", token);
                }
            }
        }
    };

    public static class ASE_KeyMESH_FACE_LIST extends ASE {

        private static final ASE instance = new ASE_KeyMESH_FACE_LIST();

        private ASE_KeyMESH_FACE_LIST() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMesh_t pMesh = ASE_GetCurrentMesh();

            if ("*MESH_FACE".equals(token)) {
                ASE_GetToken(false);	// skip face number

                // we are flipping the order here to change the front/back facing
                // from 3DS to our standard (clockwise facing out)
                ASE_GetToken(false);	// skip label
                ASE_GetToken(false);	// first vertex
                pMesh.faces[ase.currentFace].vertexNum[0] = Integer.parseInt(ase.token);

                ASE_GetToken(false);	// skip label
                ASE_GetToken(false);	// second vertex
                pMesh.faces[ase.currentFace].vertexNum[2] = Integer.parseInt(ase.token);

                ASE_GetToken(false);	// skip label
                ASE_GetToken(false);	// third vertex
                pMesh.faces[ase.currentFace].vertexNum[1] = Integer.parseInt(ase.token);

                ASE_GetToken(true);

                // we could parse material id and smoothing groups here
/*
                 if ( ( p = strstr( ase.token, "*MESH_MTLID" ) ) != 0 )
                 {
                 p += strlen( "*MESH_MTLID" ) + 1;
                 mtlID = Integer.parseInt( p );
                 }
                 else
                 {
                 common.Error( "No *MESH_MTLID found for face!" );
                 }
                 */
                ase.currentFace++;
            } else {
                common.Error("Unknown token '%s' while parsing MESH_FACE_LIST", token);
            }
        }
    };

    public static class ASE_KeyTFACE_LIST extends ASE {

        private static final ASE instance = new ASE_KeyTFACE_LIST();

        private ASE_KeyTFACE_LIST() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMesh_t pMesh = ASE_GetCurrentMesh();

            if ("*MESH_TFACE".equals(token)) {
                int a, b, c;

                ASE_GetToken(false);

                ASE_GetToken(false);
                a = Integer.parseInt(ase.token);
                ASE_GetToken(false);
                c = Integer.parseInt(ase.token);
                ASE_GetToken(false);
                b = Integer.parseInt(ase.token);

                pMesh.faces[ase.currentFace].tVertexNum[0] = a;
                pMesh.faces[ase.currentFace].tVertexNum[1] = b;
                pMesh.faces[ase.currentFace].tVertexNum[2] = c;

                ase.currentFace++;
            } else {
                common.Error("Unknown token '%s' in MESH_TFACE", token);
            }
        }
    };

    public static class ASE_KeyCFACE_LIST extends ASE {

        private static final ASE instance = new ASE_KeyCFACE_LIST();
        private static final int[] remap/*[3]*/ = {0, 2, 1};

        private ASE_KeyCFACE_LIST() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMesh_t pMesh = ASE_GetCurrentMesh();

            if ("*MESH_CFACE".equals(token)) {
                ASE_GetToken(false);

                for (int i = 0; i < 3; i++) {
                    ASE_GetToken(false);
                    int a = Integer.parseInt(ase.token);

                    // we flip the vertex order to change the face direction to our style
                    pMesh.faces[ase.currentFace].vertexColors[remap[i]][0] = (byte) (pMesh.cvertexes[a].oGet(0) * 255);
                    pMesh.faces[ase.currentFace].vertexColors[remap[i]][1] = (byte) (pMesh.cvertexes[a].oGet(1) * 255);
                    pMesh.faces[ase.currentFace].vertexColors[remap[i]][2] = (byte) (pMesh.cvertexes[a].oGet(2) * 255);
                }

                ase.currentFace++;
            } else {
                common.Error("Unknown token '%s' in MESH_CFACE", token);
            }
        }
    };

    public static class ASE_KeyMESH_TVERTLIST extends ASE {

        private static final ASE instance = new ASE_KeyMESH_TVERTLIST();

        private ASE_KeyMESH_TVERTLIST() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMesh_t pMesh = ASE_GetCurrentMesh();

            if ("*MESH_TVERT".equals(token)) {
//		char u[80], v[80], w[80];
                final String u, v, w;

                ASE_GetToken(false);

                ASE_GetToken(false);
//		strcpy( u, ase.token );
                u = ase.token;

                ASE_GetToken(false);
//		strcpy( v, ase.token );
                v = ase.token;

                ASE_GetToken(false);
//		strcpy( w, ase.token );
                w = ase.token;

                pMesh.tvertexes[ase.currentVertex].x = Float.parseFloat(u);
                // our OpenGL second texture axis is inverted from MAX's sense
                pMesh.tvertexes[ase.currentVertex].y = 1.0f - Float.parseFloat(v);

                ase.currentVertex++;

                if (ase.currentVertex > pMesh.numTVertexes) {
                    common.Error("ase.currentVertex > pMesh.numTVertexes");
                }
            } else {
                common.Error("Unknown token '%s' while parsing MESH_TVERTLIST", token);
            }
        }
    };

    public static class ASE_KeyMESH_CVERTLIST extends ASE {

        private static final ASE instance = new ASE_KeyMESH_CVERTLIST();

        private ASE_KeyMESH_CVERTLIST() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMesh_t pMesh = ASE_GetCurrentMesh();

            pMesh.colorsParsed = true;

            if ("*MESH_VERTCOL".equals(token)) {
                ASE_GetToken(false);

                ASE_GetToken(false);
                pMesh.cvertexes[ase.currentVertex].oSet(0, Float.parseFloat(token));

                ASE_GetToken(false);
                pMesh.cvertexes[ase.currentVertex].oSet(1, Float.parseFloat(token));

                ASE_GetToken(false);
                pMesh.cvertexes[ase.currentVertex].oSet(2, Float.parseFloat(token));

                ase.currentVertex++;

                if (ase.currentVertex > pMesh.numCVertexes) {
                    common.Error("ase.currentVertex > pMesh.numCVertexes");
                }
            } else {
                common.Error("Unknown token '%s' while parsing MESH_CVERTLIST", token);
            }
        }
    };

    public static class ASE_KeyMESH_NORMALS extends ASE {

        private static final ASE instance = new ASE_KeyMESH_NORMALS();

        private ASE_KeyMESH_NORMALS() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMesh_t pMesh = ASE_GetCurrentMesh();
            aseFace_t f;
            idVec3 n = new idVec3();

            pMesh.normalsParsed = true;
            f = pMesh.faces[ase.currentFace];

            if ("*MESH_FACENORMAL".equals(token)) {
                int num;

                ASE_GetToken(false);
                num = Integer.parseInt(ase.token);

                if (num >= pMesh.numFaces || num < 0) {
                    common.Error("MESH_NORMALS face index out of range: %d", num);
                }

                if (num != ase.currentFace) {
                    common.Error("MESH_NORMALS face index != currentFace");
                }

                ASE_GetToken(false);
                n.oSet(0, Float.parseFloat(ase.token));
                ASE_GetToken(false);
                n.oSet(1, Float.parseFloat(ase.token));
                ASE_GetToken(false);
                n.oSet(2, Float.parseFloat(ase.token));

                f.faceNormal.oSet(0, n.oGet(0) * pMesh.transform[0].oGet(0) + n.oGet(1) * pMesh.transform[1].oGet(0) + n.oGet(2) * pMesh.transform[2].oGet(0));
                f.faceNormal.oSet(1, n.oGet(0) * pMesh.transform[0].oGet(1) + n.oGet(1) * pMesh.transform[1].oGet(1) + n.oGet(2) * pMesh.transform[2].oGet(1));
                f.faceNormal.oSet(2, n.oGet(0) * pMesh.transform[0].oGet(2) + n.oGet(1) * pMesh.transform[1].oGet(2) + n.oGet(2) * pMesh.transform[2].oGet(2));

                f.faceNormal.Normalize();

                ase.currentFace++;
            } else if ("*MESH_VERTEXNORMAL".equals(token)) {
                int num;
                int v;

                ASE_GetToken(false);
                num = Integer.parseInt(ase.token);

                if (num >= pMesh.numVertexes || num < 0) {
                    common.Error("MESH_NORMALS vertex index out of range: %d", num);
                }

                f = pMesh.faces[ ase.currentFace - 1];

                for (v = 0; v < 3; v++) {
                    if (num == f.vertexNum[ v]) {
                        break;
                    }
                }

                if (v == 3) {
                    common.Error("MESH_NORMALS vertex index doesn't match face");
                }

                ASE_GetToken(false);
                n.oSet(0, Float.parseFloat(ase.token));
                ASE_GetToken(false);
                n.oSet(1, Float.parseFloat(ase.token));
                ASE_GetToken(false);
                n.oSet(2, Float.parseFloat(ase.token));

                f.vertexNormals[ v].oSet(0, n.oGet(0) * pMesh.transform[0].oGet(0) + n.oGet(1) * pMesh.transform[1].oGet(0) + n.oGet(2) * pMesh.transform[2].oGet(0));
                f.vertexNormals[ v].oSet(0, n.oGet(0) * pMesh.transform[0].oGet(1) + n.oGet(1) * pMesh.transform[1].oGet(1) + n.oGet(2) * pMesh.transform[2].oGet(2));
                f.vertexNormals[ v].oSet(0, n.oGet(0) * pMesh.transform[0].oGet(2) + n.oGet(1) * pMesh.transform[1].oGet(2) + n.oGet(2) * pMesh.transform[2].oGet(1));

                f.vertexNormals[v].Normalize();
            }
        }
    };

    public static class ASE_KeyMESH extends ASE {

        private static final ASE instance = new ASE_KeyMESH();

        private ASE_KeyMESH() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMesh_t pMesh = ASE_GetCurrentMesh();

            if ("*TIMEVALUE".equals(token)) {
                ASE_GetToken(false);

                pMesh.timeValue = Integer.parseInt(ase.token);
                VERBOSE(".....timevalue: %d\n", pMesh.timeValue);
            } else if ("*MESH_NUMVERTEX".equals(token)) {
                ASE_GetToken(false);

                pMesh.numVertexes = Integer.parseInt(ase.token);
                VERBOSE(".....num vertexes: %d\n", pMesh.numVertexes);
            } else if ("*MESH_NUMTVERTEX".equals(token)) {
                ASE_GetToken(false);

                pMesh.numTVertexes = Integer.parseInt(ase.token);
                VERBOSE(".....num tvertexes: %d\n", pMesh.numTVertexes);
            } else if ("*MESH_NUMCVERTEX".equals(token)) {
                ASE_GetToken(false);

                pMesh.numCVertexes = Integer.parseInt(ase.token);
                VERBOSE(".....num cvertexes: %d\n", pMesh.numCVertexes);
            } else if ("*MESH_NUMFACES".equals(token)) {
                ASE_GetToken(false);

                pMesh.numFaces = Integer.parseInt(ase.token);
                VERBOSE(".....num faces: %d\n", pMesh.numFaces);
            } else if ("*MESH_NUMTVFACES".equals(token)) {
                ASE_GetToken(false);

                pMesh.numTVFaces = Integer.parseInt(ase.token);
                VERBOSE(".....num tvfaces: %d\n", pMesh.numTVFaces);

                if (pMesh.numTVFaces != pMesh.numFaces) {
                    common.Error("MESH_NUMTVFACES != MESH_NUMFACES");
                }
            } else if ("*MESH_NUMCVFACES".equals(token)) {
                ASE_GetToken(false);

                pMesh.numCVFaces = Integer.parseInt(ase.token);
                VERBOSE(".....num cvfaces: %d\n", pMesh.numCVFaces);

                if (pMesh.numTVFaces != pMesh.numFaces) {
                    common.Error("MESH_NUMCVFACES != MESH_NUMFACES");
                }
            } else if ("*MESH_VERTEX_LIST".equals(token)) {
                pMesh.vertexes = new idVec3[pMesh.numVertexes];// Mem_Alloc(pMesh.numVertexes);
                ase.currentVertex = 0;
                VERBOSE((".....parsing MESH_VERTEX_LIST\n"));
                ASE_ParseBracedBlock(ASE_KeyMESH_VERTEX_LIST.getInstance());
            } else if ("*MESH_TVERTLIST".equals(token)) {
                ase.currentVertex = 0;
                pMesh.tvertexes = new idVec2[pMesh.numTVertexes];// Mem_Alloc(pMesh.numTVertexes);
                VERBOSE((".....parsing MESH_TVERTLIST\n"));
                ASE_ParseBracedBlock(ASE_KeyMESH_TVERTLIST.getInstance());
            } else if ("*MESH_CVERTLIST".equals(token)) {
                ase.currentVertex = 0;
                pMesh.cvertexes = new idVec3[pMesh.numCVertexes];// Mem_Alloc(pMesh.numCVertexes);
                VERBOSE((".....parsing MESH_CVERTLIST\n"));
                ASE_ParseBracedBlock(ASE_KeyMESH_CVERTLIST.getInstance());
            } else if ("*MESH_FACE_LIST".equals(token)) {
                pMesh.faces = new aseFace_t[pMesh.numFaces];// Mem_Alloc(pMesh.numFaces);
                ase.currentFace = 0;
                VERBOSE((".....parsing MESH_FACE_LIST\n"));
                ASE_ParseBracedBlock(ASE_KeyMESH_FACE_LIST.getInstance());
            } else if ("*MESH_TFACELIST".equals(token)) {
                if (null == pMesh.faces) {
                    common.Error("*MESH_TFACELIST before *MESH_FACE_LIST");
                }
                ase.currentFace = 0;
                VERBOSE((".....parsing MESH_TFACE_LIST\n"));
                ASE_ParseBracedBlock(ASE_KeyTFACE_LIST.getInstance());
            } else if ("*MESH_CFACELIST".equals(token)) {
                if (null == pMesh.faces) {//TODO:check pointer position instead of entire array
                    common.Error("*MESH_CFACELIST before *MESH_FACE_LIST");
                }
                ase.currentFace = 0;
                VERBOSE((".....parsing MESH_CFACE_LIST\n"));
                ASE_ParseBracedBlock(ASE_KeyCFACE_LIST.getInstance());
            } else if ("*MESH_NORMALS".equals(token)) {
                if (null == pMesh.faces) {
                    common.Warning("*MESH_NORMALS before *MESH_FACE_LIST");
                }
                ase.currentFace = 0;
                VERBOSE((".....parsing MESH_NORMALS\n"));
                ASE_ParseBracedBlock(ASE_KeyMESH_NORMALS.getInstance());
            }
        }
    };

    public static class ASE_KeyMESH_ANIMATION extends ASE {

        private static final ASE instance = new ASE_KeyMESH_ANIMATION();

        private ASE_KeyMESH_ANIMATION() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseMesh_t mesh;

            // loads a single animation frame
            if ("*MESH".equals(token)) {
                VERBOSE(("...found MESH\n"));

//                mesh = (aseMesh_t) Mem_Alloc(sizeof(aseMesh_t));
//                memset(mesh, 0, sizeof(aseMesh_t));
                mesh = new aseMesh_t();
                ase.currentMesh = mesh;

                ase.currentObject.frames.Append(mesh);

                ASE_ParseBracedBlock(ASE_KeyMESH.getInstance());
            } else {
                common.Error("Unknown token '%s' while parsing MESH_ANIMATION", token);
            }
        }
    };

    public static class ASE_KeyGEOMOBJECT extends ASE {

        private static final ASE instance = new ASE_KeyGEOMOBJECT();

        private ASE_KeyGEOMOBJECT() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            aseObject_t object;

            object = ase.currentObject;

            if ("*NODE_NAME".equals(token)) {
                ASE_GetToken(true);
                VERBOSE(" %s\n", ase.token);
                idStr.Copynz(object.name, ase.token, object.name.length);
            } else if ("*NODE_PARENT".equals(token)) {
                ASE_SkipRestOfLine();
            } // ignore unused data blocks
            else if ("*NODE_TM".equals(token)
                    || "*TM_ANIMATION".equals(token)) {
                ASE_ParseBracedBlock(ASE_KeyNODE_TM.getInstance());
            } // ignore regular meshes that aren't part of animation
            else if ("*MESH".equals(token)) {
                ase.currentMesh = ase.currentObject.mesh;
//                memset(ase.currentMesh, 0, sizeof(ase.currentMesh));
                ase.currentMesh = new aseMesh_t();

                ASE_ParseBracedBlock(ASE_KeyMESH.getInstance());
            } // according to spec these are obsolete
            else if ("*MATERIAL_REF".equals(token)) {
                ASE_GetToken(false);

                object.materialRef = Integer.parseInt(ase.token);
            } // loads a sequence of animation frames
            else if ("*MESH_ANIMATION".equals(token)) {
                VERBOSE(("..found MESH_ANIMATION\n"));

                ASE_ParseBracedBlock(ASE_KeyMESH_ANIMATION.getInstance());
            } // skip unused info
            else if ("*PROP_MOTIONBLUR".equals(token)
                    || "*PROP_CASTSHADOW".equals(token)
                    || "*PROP_RECVSHADOW".equals(token)) {
                ASE_SkipRestOfLine();
            }

        }
    };

    public static void ASE_ParseGeomObject() {
        aseObject_t object;

        VERBOSE(("GEOMOBJECT"));

//        object = (aseObject_t *) Mem_Alloc(sizeof(aseObject_t));
//        memset(object, 0, sizeof(aseObject_t));
        object = new aseObject_t();
        ase.model.objects.Append(object);
        ase.currentObject = object;

        object.frames.Resize(32, 32);

        ASE_ParseBracedBlock(ASE_KeyGEOMOBJECT.getInstance());
    }

    public static class ASE_KeyGROUP extends ASE {

        private static final ASE instance = new ASE_KeyGROUP();

        private ASE_KeyGROUP() {
        }

        public static ASE getInstance() {
            return instance;
        }

        @Override
        public void run(final String token) {
            if ("*GEOMOBJECT".equals(token)) {
                ASE_ParseGeomObject();
            }
        }
    };

    /*
     =================
     ASE_Parse
     =================
     */
    public static aseModel_s ASE_Parse(final ByteBuffer buffer, boolean verbose) {
//	memset( &ase, 0, sizeof( ase ) );

        ase.verbose = verbose;

        ase.buffer = bbtocb(buffer);//.asCharBuffer();
        ase.len = ase.buffer.length();//TODO:capacity?
        ase.curpos = 0;//ase.buffer;
        ase.currentObject = null;

        // NOTE: using new operator because aseModel_t contains idList class objects
        ase.model = new aseModel_s();
//        memset(ase.model, 0, sizeof(aseModel_t));
        ase.model.objects.Resize(32, 32);
        ase.model.materials.Resize(32, 32);

        while (ASE_GetToken(false)) {
            if ("*3DSMAX_ASCIIEXPORT".equals(ase.token)
                    || "*COMMENT".equals(ase.token)) {
                ASE_SkipRestOfLine();
            } else if ("*SCENE".equals(ase.token)) {
                ASE_SkipEnclosingBraces();
            } else if ("*GROUP".equals(ase.token)) {
                ASE_GetToken(false);		// group name
                ASE_ParseBracedBlock(ASE_KeyGROUP.getInstance());
            } else if ("*SHAPEOBJECT".equals(ase.token)) {
                ASE_SkipEnclosingBraces();
            } else if ("*CAMERAOBJECT".equals(ase.token)) {
                ASE_SkipEnclosingBraces();
            } else if ("*MATERIAL_LIST".equals(ase.token)) {
                VERBOSE(("MATERIAL_LIST\n"));

                ASE_ParseBracedBlock(ASE_KeyMATERIAL_LIST.getInstance());
            } else if ("*GEOMOBJECT".equals(ase.token)) {
                ASE_ParseGeomObject();
            } else if (isNotNullOrEmpty(ase.token)) {
                common.Printf("Unknown token '%s'\n", ase.token);
            }
        }

        return ase.model;
    }
}
