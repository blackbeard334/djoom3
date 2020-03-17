package neo.Renderer;

import static neo.TempDump.NOT;
import static neo.TempDump.bbtocb;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import neo.idlib.Lib.idException;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.HashTable.idHashTable;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat4;

/**
 *
 */
public class Model_ma {

    /*
     ===============================================================================

     MA loader. (Maya Ascii Format)

     ===============================================================================
     */
    static class maNodeHeader_t {

//	char					name[128];
        String name;
//	char					parent[128];
        String parent;
    };

    static class maAttribHeader_t {

//	char					name[128];
        String name;
        int size;
    };

    static class maTransform_s {

        idVec3 translate;
        idVec3 rotate;
        idVec3 scale;
        maTransform_s parent;
    };

    static class maFace_t {

        int[] edge = new int[3];
        int[] vertexNum = new int[3];
        int[] tVertexNum = new int[3];
        int[] vertexColors = new int[3];
        idVec3[] vertexNormals = new idVec3[3];
    };

    static class maMesh_t {

        //Transform to be applied
        maTransform_s transform;
//
        //Verts
        int numVertexes;
        idVec3[] vertexes;
        int numVertTransforms;
        idVec4[] vertTransforms;
        int nextVertTransformIndex;
//
        //Texture Coordinates
        int numTVertexes;
        idVec2[] tvertexes;
//
        //Edges
        int numEdges;
        idVec3[] edges;
//
        //Colors
        int numColors;
        byte[] colors;
//
        //Faces
        int numFaces;
        maFace_t[] faces;
//
        //Normals
        int numNormals;
        idVec3[] normals;
        boolean normalsParsed;
        int nextNormal;
    };

    static class maMaterial_t {

//	char					name[128];
        String name;
        float uOffset, vOffset;		// max lets you offset by material without changing texCoords
        float uTiling, vTiling;		// multiply tex coords by this
        float angle;					// in clockwise radians
    };

    static class maObject_t {

//	char					name[128];
        String name;
        int materialRef;
//	char					materialName[128];
        String materialName;
//
        maMesh_t mesh;
    };

    static class maFileNode_t {

//	char					name[128];
        String name;
//	char					path[1024];
        String path;
    };

    static class maMaterialNode_s {

//	char					name[128];
        String name;
//
        maMaterialNode_s child;
        maFileNode_t file;
    };

    static class maModel_s {

        long[]/*ID_TIME_T*/ timeStamp = new long[1];
        idList<maMaterial_t> materials;
        idList<maObject_t> objects;
        idHashTable<maTransform_s> transforms;
//	
        //Material Resolution
        idHashTable<maFileNode_t> fileNodes;
        idHashTable<maMaterialNode_s> materialNodes;
    };

    /*
     ======================================================================

     Parses Maya ASCII files.

     ======================================================================
     */
    public static void MA_VERBOSE(String fmt, Object... x) {
        if (maGlobal.verbose) {
            common.Printf(fmt, x);
        }
    }

// working variables used during parsing
    public static class ma_t {

        boolean verbose;
        maModel_s model;
        maObject_t currentObject;
    };

    public static void MA_ParseNodeHeader(idParser parser, maNodeHeader_t header) throws idException {

//	memset(header, 0, sizeof(maNodeHeader_t));//TODO:
        idToken token = new idToken();
        while (parser.ReadToken(token)) {
            if (0 == token.Icmp("-")) {
                parser.ReadToken(token);
                if (0 == token.Icmp("n")) {
                    parser.ReadToken(token);
                    header.name = token.getData();
                } else if (0 == token.Icmp("p")) {
                    parser.ReadToken(token);
                    header.parent = token.getData();
                }
            } else if (0 == token.Icmp(";")) {
                break;
            }
        }
    }

    public static boolean MA_ParseHeaderIndex(maAttribHeader_t header, int[] minIndex, int[] maxIndex, final String headerType, final String skipString) throws idException {

        idParser miniParse = new idParser();
        idToken token = new idToken();

        miniParse.LoadMemory(header.name, header.name.length(), headerType);
        if (skipString != null) {
            miniParse.SkipUntilString(skipString);
        }

        if (!miniParse.SkipUntilString("[")) {
            //This was just a header
            return false;
        }
        minIndex[0] = miniParse.ParseInt();
        miniParse.ReadToken(token);
        if (0 == token.Icmp("]")) {
            maxIndex[0] = minIndex[0];
        } else {
            maxIndex[0] = miniParse.ParseInt();
        }
        return true;
    }

    public static boolean MA_ParseAttribHeader(idParser parser, maAttribHeader_t header) throws idException {

        idToken token = new idToken();

        // memset(header, 0, sizeof(maAttribHeader_t));
        parser.ReadToken(token);
        if (0 == token.Icmp("-")) {
            parser.ReadToken(token);
            if (0 == token.Icmp("s")) {
                header.size = parser.ParseInt();
                parser.ReadToken(token);
            }
        }
        header.name = token.getData();
        return true;
    }

    public static boolean MA_ReadVec3(idParser parser, idVec3 vec) throws idException {
        // idToken token;
        if (!parser.SkipUntilString("double3")) {
            throw new idException(va("Maya Loader '%s': Invalid Vec3", parser.GetFileName()));
//		return false;
        }

        //We need to flip y and z because of the maya coordinate system
        vec.x = parser.ParseFloat();
        vec.z = parser.ParseFloat();
        vec.y = parser.ParseFloat();

        return true;
    }

    public static boolean IsNodeComplete(idToken token) {
        return 0 == token.Icmp("createNode")
                || 0 == token.Icmp("connectAttr")
                || 0 == token.Icmp("select");
    }

    public static boolean MA_ParseTransform(idParser parser) throws idException {

        maNodeHeader_t header = new maNodeHeader_t();
        maTransform_s transform;
        // memset(&header, 0, sizeof(header));

        //Allocate room for the transform
        transform = new maTransform_s();// Mem_Alloc(sizeof(maTransform_s));
        // memset(transform, 0, sizeof(maTransform_t));
        transform.scale.x = transform.scale.y = transform.scale.z = 1;

        //Get the header info from the transform
        MA_ParseNodeHeader(parser, header);

        //Read the transform attributes
        idToken token = new idToken();
        while (parser.ReadToken(token)) {
            if (IsNodeComplete(token)) {
                parser.UnreadToken(token);
                break;
            }
            if (0 == token.Icmp("setAttr")) {
                parser.ReadToken(token);
                if (0 == token.Icmp(".t")) {
                    if (!MA_ReadVec3(parser, transform.translate)) {
                        return false;
                    }
                    transform.translate.y *= -1;
                } else if (0 == token.Icmp(".r")) {
                    if (!MA_ReadVec3(parser, transform.rotate)) {
                        return false;
                    }
                } else if (0 == token.Icmp(".s")) {
                    if (!MA_ReadVec3(parser, transform.scale)) {
                        return false;
                    }
                } else {
                    parser.SkipRestOfLine();
                }
            }
        }

        if (!header.parent.isEmpty()) {
            //Find the parent
            maTransform_s[] parent = new maTransform_s[1];
            maGlobal.model.transforms.Get(header.parent, parent);
            if (parent != null) {
                transform.parent = parent[0];
            }
        }

        //Add this transform to the list
        maGlobal.model.transforms.Set(header.name, transform);
        return true;
    }

    public static boolean MA_ParseVertex(idParser parser, maAttribHeader_t header) throws idException {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        // idToken token;

        //Allocate enough space for all the verts if this is the first attribute for verticies
        if (null == pMesh.vertexes) {
            pMesh.numVertexes = header.size;
            pMesh.vertexes = new idVec3[pMesh.numVertexes];// Mem_Alloc(pMesh.numVertexes);
        }

        //Get the start and end index for this attribute
        int[] minIndex = new int[1], maxIndex = new int[1];
        if (!MA_ParseHeaderIndex(header, minIndex, maxIndex, "VertexHeader", null)) {
            //This was just a header
            return true;
        }

        //Read each vert
        for (int i = minIndex[0]; i <= maxIndex[0]; i++) {
            pMesh.vertexes[i].x = parser.ParseFloat();
            pMesh.vertexes[i].z = parser.ParseFloat();
            pMesh.vertexes[i].y = -parser.ParseFloat();
        }

        return true;
    }

    public static boolean MA_ParseVertexTransforms(idParser parser, maAttribHeader_t header) throws idException {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        idToken token = new idToken();

        //Allocate enough space for all the verts if this is the first attribute for verticies
        if (null == pMesh.vertTransforms) {
            if (header.size == 0) {
                header.size = 1;
            }

            pMesh.numVertTransforms = header.size;
            pMesh.vertTransforms = new idVec4[pMesh.numVertTransforms];// Mem_Alloc(pMesh.numVertTransforms);
            pMesh.nextVertTransformIndex = 0;
        }

        //Get the start and end index for this attribute
        int[] minIndex = new int[1], maxIndex = new int[1];
        if (!MA_ParseHeaderIndex(header, minIndex, maxIndex, "VertexTransformHeader", null)) {
            //This was just a header
            return true;
        }

        parser.ReadToken(token);
        if (0 == token.Icmp("-")) {
            idToken tk2 = new idToken();
            parser.ReadToken(tk2);
            if (0 == tk2.Icmp("type")) {
                parser.SkipUntilString("float3");
            } else {
                parser.UnreadToken(tk2);
                parser.UnreadToken(token);
            }
        } else {
            parser.UnreadToken(token);
        }

        //Read each vert
        for (int i = minIndex[0]; i <= maxIndex[0]; i++) {
            pMesh.vertTransforms[pMesh.nextVertTransformIndex].x = parser.ParseFloat();
            pMesh.vertTransforms[pMesh.nextVertTransformIndex].z = parser.ParseFloat();
            pMesh.vertTransforms[pMesh.nextVertTransformIndex].y = -parser.ParseFloat();

            //w hold the vert index
            pMesh.vertTransforms[pMesh.nextVertTransformIndex].w = i;

            pMesh.nextVertTransformIndex++;
        }

        return true;
    }

    public static boolean MA_ParseEdge(idParser parser, maAttribHeader_t header) throws idException {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        // idToken token;

        //Allocate enough space for all the verts if this is the first attribute for verticies
        if (null == pMesh.edges) {
            pMesh.numEdges = header.size;
            pMesh.edges = new idVec3[pMesh.numEdges];// Mem_Alloc(pMesh.numEdges);
        }

        //Get the start and end index for this attribute
        int[] minIndex = new int[1], maxIndex = new int[1];
        if (!MA_ParseHeaderIndex(header, minIndex, maxIndex, "EdgeHeader", null)) {
            //This was just a header
            return true;
        }

        //Read each vert
        for (int i = minIndex[0]; i <= maxIndex[0]; i++) {
            pMesh.edges[i].x = parser.ParseFloat();
            pMesh.edges[i].y = parser.ParseFloat();
            pMesh.edges[i].z = parser.ParseFloat();
        }

        return true;
    }

    public static boolean MA_ParseNormal(idParser parser, maAttribHeader_t header) throws idException {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        idToken token = new idToken();

        //Allocate enough space for all the verts if this is the first attribute for verticies
        if (null == pMesh.normals) {
            pMesh.numNormals = header.size;
            pMesh.normals = new idVec3[pMesh.numNormals];// Mem_Alloc(pMesh.numNormals);
        }

        //Get the start and end index for this attribute
        int[] minIndex = new int[1], maxIndex = new int[1];
        if (!MA_ParseHeaderIndex(header, minIndex, maxIndex, "NormalHeader", null)) {
            //This was just a header
            return true;
        }

        parser.ReadToken(token);
        if (0 == token.Icmp("-")) {
            idToken tk2 = new idToken();
            parser.ReadToken(tk2);
            if (0 == tk2.Icmp("type")) {
                parser.SkipUntilString("float3");
            } else {
                parser.UnreadToken(tk2);
                parser.UnreadToken(token);
            }
        } else {
            parser.UnreadToken(token);
        }

        //Read each vert
        for (int i = minIndex[0]; i <= maxIndex[0]; i++) {
            pMesh.normals[i].x = parser.ParseFloat();

            //Adjust the normals for the change in coordinate systems
            pMesh.normals[i].z = parser.ParseFloat();
            pMesh.normals[i].y = -parser.ParseFloat();

            pMesh.normals[i].Normalize();

        }

        pMesh.normalsParsed = true;
        pMesh.nextNormal = 0;

        return true;
    }

    public static boolean MA_ParseFace(idParser parser, maAttribHeader_t header) throws idException {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        idToken token = new idToken();

        //Allocate enough space for all the verts if this is the first attribute for verticies
        if (null == pMesh.faces) {
            pMesh.numFaces = header.size;
            pMesh.faces = new maFace_t[pMesh.numFaces];// Mem_Alloc(pMesh.numFaces);
        }

        //Get the start and end index for this attribute
        int[] minIndex = new int[1], maxIndex = new int[1];
        if (!MA_ParseHeaderIndex(header, minIndex, maxIndex, "FaceHeader", null)) {
            //This was just a header
            return true;
        }

        //Read the face data
        int currentFace = minIndex[0] - 1;
        while (parser.ReadToken(token)) {
            if (IsNodeComplete(token)) {
                parser.UnreadToken(token);
                break;
            }

            if (0 == token.Icmp("f")) {
                int count = parser.ParseInt();
                if (count != 3) {
                    throw new idException(va("Maya Loader '%s': Face is not a triangle.", parser.GetFileName()));
//                    return false;
                }
                //Increment the face number because a new face always starts with an "f" token
                currentFace++;

                //We cannot reorder edges until later because the normal processing
                //assumes the edges are in the original order
                pMesh.faces[currentFace].edge[0] = parser.ParseInt();
                pMesh.faces[currentFace].edge[1] = parser.ParseInt();
                pMesh.faces[currentFace].edge[2] = parser.ParseInt();

                //Some more init stuff
                pMesh.faces[currentFace].vertexColors[0] = pMesh.faces[currentFace].vertexColors[1] = pMesh.faces[currentFace].vertexColors[2] = -1;

            } else if (0 == token.Icmp("mu")) {
                int uvstIndex = parser.ParseInt();
                int count = parser.ParseInt();
                if (count != 3) {
                    throw new idException(va("Maya Loader '%s': Invalid texture coordinates.", parser.GetFileName()));
//                    return false;
                }
                pMesh.faces[currentFace].tVertexNum[0] = parser.ParseInt();
                pMesh.faces[currentFace].tVertexNum[1] = parser.ParseInt();
                pMesh.faces[currentFace].tVertexNum[2] = parser.ParseInt();

            } else if (0 == token.Icmp("mf")) {
                int count = parser.ParseInt();
                if (count != 3) {
                    throw new idException(va("Maya Loader '%s': Invalid texture coordinates.", parser.GetFileName()));
//                    return false;
                }
                pMesh.faces[currentFace].tVertexNum[0] = parser.ParseInt();
                pMesh.faces[currentFace].tVertexNum[1] = parser.ParseInt();
                pMesh.faces[currentFace].tVertexNum[2] = parser.ParseInt();

            } else if (0 == token.Icmp("fc")) {

                int count = parser.ParseInt();
                if (count != 3) {
                    throw new idException(va("Maya Loader '%s': Invalid vertex color.", parser.GetFileName()));
//                    return false;
                }
                pMesh.faces[currentFace].vertexColors[0] = parser.ParseInt();
                pMesh.faces[currentFace].vertexColors[1] = parser.ParseInt();
                pMesh.faces[currentFace].vertexColors[2] = parser.ParseInt();

            }
        }

        return true;
    }

    public static boolean MA_ParseColor(idParser parser, maAttribHeader_t header) throws idException {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        // idToken token;

        //Allocate enough space for all the verts if this is the first attribute for verticies
        if (null == pMesh.colors) {
            pMesh.numColors = header.size;
            pMesh.colors = new byte[pMesh.numColors * 4];// Mem_Alloc(pMesh.numColors * 4);
        }

        //Get the start and end index for this attribute
        int[] minIndex = new int[1], maxIndex = new int[1];
        if (!MA_ParseHeaderIndex(header, minIndex, maxIndex, "ColorHeader", null)) {
            //This was just a header
            return true;
        }

        //Read each vert
        for (int i = minIndex[0]; i <= maxIndex[0]; i++) {
            pMesh.colors[i * 4 + 0] = (byte) (parser.ParseFloat() * 255);
            pMesh.colors[i * 4 + 1] = (byte) (parser.ParseFloat() * 255);
            pMesh.colors[i * 4 + 2] = (byte) (parser.ParseFloat() * 255);
            pMesh.colors[i * 4 + 3] = (byte) (parser.ParseFloat() * 255);
        }

        return true;
    }

    public static boolean MA_ParseTVert(idParser parser, maAttribHeader_t header) throws idException {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        idToken token = new idToken();

        //This is not the texture coordinates. It is just the name so ignore it
        if (header.name.contains("uvsn")) {
            return true;
        }

        //Allocate enough space for all the data
        if (null == pMesh.tvertexes) {
            pMesh.numTVertexes = header.size;
            pMesh.tvertexes = new idVec2[pMesh.numTVertexes];// Mem_Alloc(pMesh.numTVertexes);
        }

        //Get the start and end index for this attribute
        int[] minIndex = new int[1], maxIndex = new int[1];
        if (!MA_ParseHeaderIndex(header, minIndex, maxIndex, "TextureCoordHeader", "uvsp")) {
            //This was just a header
            return true;
        }

        parser.ReadToken(token);
        if (0 == token.Icmp("-")) {
            idToken tk2 = new idToken();
            parser.ReadToken(tk2);
            if (0 == tk2.Icmp("type")) {
                parser.SkipUntilString("float2");
            } else {
                parser.UnreadToken(tk2);
                parser.UnreadToken(token);
            }
        } else {
            parser.UnreadToken(token);
        }

        //Read each tvert
        for (int i = minIndex[0]; i <= maxIndex[0]; i++) {
            pMesh.tvertexes[i].x = parser.ParseFloat();
            pMesh.tvertexes[i].y = 1.0f - parser.ParseFloat();
        }

        return true;
    }

    /*
     *	Quick check to see if the vert participates in a shared normal
     */
    public static boolean MA_QuickIsVertShared(int faceIndex, int vertIndex) {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        int vertNum = pMesh.faces[faceIndex].vertexNum[vertIndex];

        for (int i = 0; i < 3; i++) {
            int edge = pMesh.faces[faceIndex].edge[i];
            if (edge < 0) {
                edge = (int) (idMath.Fabs(edge) - 1);
            }
            if (pMesh.edges[edge].z == 1 && (pMesh.edges[edge].x == vertNum || pMesh.edges[edge].y == vertNum)) {
                return true;
            }
        }
        return false;
    }

    public static void MA_GetSharedFace(int faceIndex, int vertIndex, int[] sharedFace, int[] sharedVert) {

        maMesh_t pMesh = maGlobal.currentObject.mesh;
        int vertNum = pMesh.faces[faceIndex].vertexNum[vertIndex];

        sharedFace[0] = -1;
        sharedVert[0] = -1;

        //Find a shared edge on this face that contains the specified vert
        for (int edgeIndex = 0; edgeIndex < 3; edgeIndex++) {

            int edge = pMesh.faces[faceIndex].edge[edgeIndex];
            if (edge < 0) {
                edge = (int) (idMath.Fabs(edge) - 1);
            }

            if (pMesh.edges[edge].z == 1 && (pMesh.edges[edge].x == vertNum || pMesh.edges[edge].y == vertNum)) {

                for (int i = 0; i < faceIndex; i++) {

                    for (int j = 0; j < 3; j++) {
                        if (pMesh.faces[i].vertexNum[j] == vertNum) {
                            sharedFace[0] = i;
                            sharedVert[0] = j;
                            break;
                        }
                    }
                }
            }
            if (sharedFace[0] != -1) {
                break;
            }

        }
    }

    public static void MA_ParseMesh(idParser parser) throws idException {

        maObject_t object;
        object = new maObject_t();// Mem_Alloc(sizeof(maObject_t));
//	memset( object, 0, sizeof( maObject_t ) );
        maGlobal.model.objects.Append(object);
        maGlobal.currentObject = object;
        object.materialRef = -1;

        //Get the header info from the mesh
        maNodeHeader_t nodeHeader = new maNodeHeader_t();
        MA_ParseNodeHeader(parser, nodeHeader);

        //Find my parent
        if (!nodeHeader.parent.isEmpty()) {
            //Find the parent
            maTransform_s[] parent = new maTransform_s[1];
            maGlobal.model.transforms.Get(nodeHeader.parent, parent);
            if (parent[0] != null) {
                maGlobal.currentObject.mesh.transform = parent[0];
            }
        }

        object.name = nodeHeader.name;

        //Read the transform attributes
        idToken token = new idToken();
        while (parser.ReadToken(token)) {
            if (IsNodeComplete(token)) {
                parser.UnreadToken(token);
                break;
            }
            if (0 == token.Icmp("setAttr")) {
                maAttribHeader_t attribHeader = new maAttribHeader_t();
                MA_ParseAttribHeader(parser, attribHeader);

                if (attribHeader.name.contains(".vt")) {
                    MA_ParseVertex(parser, attribHeader);
                } else if (attribHeader.name.contains(".ed")) {
                    MA_ParseEdge(parser, attribHeader);
                } else if (attribHeader.name.contains(".pt")) {
                    MA_ParseVertexTransforms(parser, attribHeader);
                } else if (attribHeader.name.contains(".n")) {
                    MA_ParseNormal(parser, attribHeader);
                } else if (attribHeader.name.contains(".fc")) {
                    MA_ParseFace(parser, attribHeader);
                } else if (attribHeader.name.contains(".clr")) {
                    MA_ParseColor(parser, attribHeader);
                } else if (attribHeader.name.contains(".uvst")) {
                    MA_ParseTVert(parser, attribHeader);
                } else {
                    parser.SkipRestOfLine();
                }
            }
        }

        maMesh_t pMesh = maGlobal.currentObject.mesh;

        //Get the verts from the edge
        for (int i = 0; i < pMesh.numFaces; i++) {
            for (int j = 0; j < 3; j++) {
                int edge = pMesh.faces[i].edge[j];
                if (edge < 0) {
                    edge = (int) (idMath.Fabs(edge) - 1);
                    pMesh.faces[i].vertexNum[j] = (int) pMesh.edges[edge].y;
                } else {
                    pMesh.faces[i].vertexNum[j] = (int) pMesh.edges[edge].x;
                }
            }
        }

        //Get the normals
        if (pMesh.normalsParsed) {
            for (int i = 0; i < pMesh.numFaces; i++) {
                for (int j = 0; j < 3; j++) {

                    //Is this vertex shared
                    int[] sharedFace = {-1};
                    int[] sharedVert = {-1};

                    if (MA_QuickIsVertShared(i, j)) {
                        MA_GetSharedFace(i, j, sharedFace, sharedVert);
                    }

                    if (sharedFace[0] != -1) {
                        //Get the normal from the share
                        pMesh.faces[i].vertexNormals[j] = pMesh.faces[sharedFace[0]].vertexNormals[sharedVert[0]];

                    } else {
                        //The vertex is not shared so get the next normal
                        if (pMesh.nextNormal >= pMesh.numNormals) {
                            //We are using more normals than exist
                            throw new idException(va("Maya Loader '%s': Invalid Normals Index.", parser.GetFileName()));
                        }
                        pMesh.faces[i].vertexNormals[j] = pMesh.normals[pMesh.nextNormal];
                        pMesh.nextNormal++;
                    }
                }
            }
        }

        //Now that the normals are good...lets reorder the verts to make the tris face the right way
        for (int i = 0; i < pMesh.numFaces; i++) {
            int tmp = pMesh.faces[i].vertexNum[1];
            pMesh.faces[i].vertexNum[1] = pMesh.faces[i].vertexNum[2];
            pMesh.faces[i].vertexNum[2] = tmp;

            idVec3 tmpVec = pMesh.faces[i].vertexNormals[1];
            pMesh.faces[i].vertexNormals[1] = pMesh.faces[i].vertexNormals[2];
            pMesh.faces[i].vertexNormals[2] = tmpVec;

            tmp = pMesh.faces[i].tVertexNum[1];
            pMesh.faces[i].tVertexNum[1] = pMesh.faces[i].tVertexNum[2];
            pMesh.faces[i].tVertexNum[2] = tmp;

            tmp = pMesh.faces[i].vertexColors[1];
            pMesh.faces[i].vertexColors[1] = pMesh.faces[i].vertexColors[2];
            pMesh.faces[i].vertexColors[2] = tmp;
        }

        //Now apply the pt transformations
        for (int i = 0; i < pMesh.numVertTransforms; i++) {
            pMesh.vertexes[(int) pMesh.vertTransforms[i].w].oPluSet(pMesh.vertTransforms[i].ToVec3());
        }

        MA_VERBOSE((va("MESH %s - parent %s\n", nodeHeader.name, nodeHeader.parent)));
        MA_VERBOSE((va("\tverts:%d\n", maGlobal.currentObject.mesh.numVertexes)));
        MA_VERBOSE((va("\tfaces:%d\n", maGlobal.currentObject.mesh.numFaces)));
    }

    public static void MA_ParseFileNode(idParser parser) throws idException {

        //Get the header info from the node
        maNodeHeader_t header = new maNodeHeader_t();
        MA_ParseNodeHeader(parser, header);

        //Read the transform attributes
        idToken token = new idToken();
        while (parser.ReadToken(token)) {
            if (IsNodeComplete(token)) {
                parser.UnreadToken(token);
                break;
            }
            if (0 == token.Icmp("setAttr")) {
                maAttribHeader_t attribHeader = new maAttribHeader_t();
                MA_ParseAttribHeader(parser, attribHeader);

                if (attribHeader.name.contains(".ftn")) {
                    parser.SkipUntilString("string");
                    parser.ReadToken(token);
                    if (0 == token.Icmp("(")) {
                        parser.ReadToken(token);
                    }

                    maFileNode_t fileNode;
                    fileNode = new maFileNode_t();// Mem_Alloc(sizeof(maFileNode_t));
                    fileNode.name = header.name;
                    fileNode.path = token.getData();

                    maGlobal.model.fileNodes.Set(fileNode.name, fileNode);
                } else {
                    parser.SkipRestOfLine();
                }
            }
        }
    }

    public static void MA_ParseMaterialNode(idParser parser) {

        //Get the header info from the node
        maNodeHeader_t header = new maNodeHeader_t();
        MA_ParseNodeHeader(parser, header);

        maMaterialNode_s matNode = new maMaterialNode_s();
//        matNode = (maMaterialNode_s) Mem_Alloc(sizeof(maMaterialNode_t));
//	memset(matNode, 0, sizeof(maMaterialNode_t));

        matNode.name = header.name;

        maGlobal.model.materialNodes.Set(matNode.name, matNode);
    }

    public static void MA_ParseCreateNode(idParser parser) throws idException {

        idToken token = new idToken();
        parser.ReadToken(token);

        if (0 == token.Icmp("transform")) {
            MA_ParseTransform(parser);
        } else if (0 == token.Icmp("mesh")) {
            MA_ParseMesh(parser);
        } else if (0 == token.Icmp("file")) {
            MA_ParseFileNode(parser);
        } else if (0 == token.Icmp("shadingEngine") || 0 == token.Icmp("lambert") || 0 == token.Icmp("phong") || 0 == token.Icmp("blinn")) {
            MA_ParseMaterialNode(parser);
        }
    }

    public static int MA_AddMaterial(final String materialName) {

        maMaterialNode_s[] destNode = new maMaterialNode_s[1];
        maGlobal.model.materialNodes.Get(materialName, destNode);
        if (destNode[0] != null) {
            maMaterialNode_s matNode = destNode[0];

            //Iterate down the tree until we get a file
            while (matNode != null && null == matNode.file) {
                matNode = matNode.child;
            }
            if (matNode != null && matNode.file != null) {

                //Got the file
                maMaterial_t material;
                material = new maMaterial_t();//Mem_Alloc(sizeof(maMaterial_t));
//			memset( material, 0, sizeof( maMaterial_t ) );

                //Remove the OS stuff
                String qPath;
                qPath = fileSystem.OSPathToRelativePath(matNode.file.path);

                material.name = qPath;

                maGlobal.model.materials.Append(material);
                return maGlobal.model.materials.Num() - 1;
            }
        }
        return -1;
    }

    public static boolean MA_ParseConnectAttr(idParser parser) throws idException {

        idStr temp;
        idStr srcName;
        idStr srcType;
        idStr destName;
        idStr destType;

        idToken token = new idToken();
        parser.ReadToken(token);
        temp = token;
        int dot = temp.Find(".");
        if (dot == -1) {
            throw new idException(va("Maya Loader '%s': Invalid Connect Attribute.", parser.GetFileName()));
//		return false;
        }
        srcName = temp.Left(dot);
        srcType = temp.Right(temp.Length() - dot - 1);

        parser.ReadToken(token);
        temp = token;
        dot = temp.Find(".");
        if (dot == -1) {
            throw new idException(va("Maya Loader '%s': Invalid Connect Attribute.", parser.GetFileName()));
//		return false;
        }
        destName = temp.Left(dot);
        destType = temp.Right(temp.Length() - dot - 1);

        if (srcType.Find("oc") != -1) {

            //Is this attribute a material node attribute
            maMaterialNode_s[] matNode = new maMaterialNode_s[1];
            maGlobal.model.materialNodes.Get(srcName.getData(), matNode);
            if (matNode[0] != null) {
                maMaterialNode_s[] destNode = new maMaterialNode_s[1];
                maGlobal.model.materialNodes.Get(destName.getData(), destNode);
                if (destNode[0] != null) {
                    destNode[0].child = matNode[0];
                }
            }

            //Is this attribute a file node
            maFileNode_t[] fileNode = new maFileNode_t[1];
            maGlobal.model.fileNodes.Get(srcName.getData(), fileNode);
            if (fileNode[0] != null) {
                maMaterialNode_s[] destNode = new maMaterialNode_s[1];
                maGlobal.model.materialNodes.Get(destName.getData(), destNode);
                if (destNode[0] != null) {
                    destNode[0].file = fileNode[0];
                }
            }
        }

        if (srcType.Find("iog") != -1) {
            //Is this an attribute for one of our meshes
            for (int i = 0; i < maGlobal.model.objects.Num(); i++) {
                if (maGlobal.model.objects.oGet(i).name.equals(srcName)) {
                    //maGlobal.model.objects.oGet(i).materialRef = MA_AddMaterial(destName);
                    maGlobal.model.objects.oGet(i).materialName = destName.getData();
                    break;
                }
            }
        }

        return true;
    }

    public static void MA_BuildScale(idMat4 mat, float x, float y, float z) {
        mat.Identity();
        mat.oGet(0).oSet(0, x);
        mat.oGet(1).oSet(1, y);
        mat.oGet(2).oSet(2, z);
    }

    public static void MA_BuildAxisRotation(idMat4 mat, float ang, int axis) {

        final float sinAng = idMath.Sin(ang);
        final float cosAng = idMath.Cos(ang);

        mat.Identity();
        switch (axis) {
            case 0: //x
                mat.oGet(1).oSet(1, cosAng);
                mat.oGet(1).oSet(2, sinAng);
                mat.oGet(2).oSet(1, -sinAng);
                mat.oGet(2).oSet(2, cosAng);
                break;
            case 1:	//y
                mat.oGet(0).oSet(0, cosAng);
                mat.oGet(0).oSet(2, -sinAng);
                mat.oGet(2).oSet(0, sinAng);
                mat.oGet(2).oSet(2, cosAng);
                break;
            case 2://z
                mat.oGet(0).oSet(0, cosAng);
                mat.oGet(0).oSet(1, sinAng);
                mat.oGet(1).oSet(0, -sinAng);
                mat.oGet(1).oSet(1, cosAng);
                break;
        }
    }

    public static void MA_ApplyTransformation(maModel_s model) {

        for (int i = 0; i < model.objects.Num(); i++) {
            maMesh_t mesh = model.objects.oGet(i).mesh;
            maTransform_s transform = mesh.transform;

            while (transform != null) {

                idMat4 rotx = new idMat4(), roty = new idMat4(), rotz = new idMat4();
                idMat4 scale = new idMat4();

                rotx.Identity();
                roty.Identity();
                rotz.Identity();

                if (Math.abs(transform.rotate.x) > 0.0f) {
                    MA_BuildAxisRotation(rotx, (float) DEG2RAD(-transform.rotate.x), 0);
                }
                if (Math.abs(transform.rotate.y) > 0.0f) {
                    MA_BuildAxisRotation(roty, (float) DEG2RAD(transform.rotate.y), 1);
                }
                if (Math.abs(transform.rotate.z) > 0.0f) {
                    MA_BuildAxisRotation(rotz, (float) DEG2RAD(-transform.rotate.z), 2);
                }

                MA_BuildScale(scale, transform.scale.x, transform.scale.y, transform.scale.z);

                //Apply the transformation to each vert
                for (int j = 0; j < mesh.numVertexes; j++) {
                    mesh.vertexes[j] = scale.oMultiply(mesh.vertexes[j]);

                    mesh.vertexes[j] = rotx.oMultiply(mesh.vertexes[j]);
                    mesh.vertexes[j] = rotz.oMultiply(mesh.vertexes[j]);
                    mesh.vertexes[j] = roty.oMultiply(mesh.vertexes[j]);

                    mesh.vertexes[j] = mesh.vertexes[j].oPlus(transform.translate);
                }

                transform = transform.parent;
            }
        }
    }

    /*
     =================
     MA_Parse
     =================
     */
    public static maModel_s MA_Parse(final CharBuffer buffer, final String filename, boolean verbose) throws idException {
        // // memset( &maGlobal, 0, sizeof( maGlobal ) );

        maGlobal.verbose = verbose;

        maGlobal.currentObject = null;

        // NOTE: using new operator because aseModel_t contains idList class objects
        maGlobal.model = new maModel_s();
        maGlobal.model.objects.Resize(32, 32);
        maGlobal.model.materials.Resize(32, 32);

        idParser parser = new idParser();
        parser.SetFlags(LEXFL_NOSTRINGCONCAT);
        parser.LoadMemory(buffer, buffer.length(), filename);//TODO:use capacity instead of length?

        idToken token = new idToken();
        while (parser.ReadToken(token)) {

            if (0 == token.Icmp("createNode")) {
                MA_ParseCreateNode(parser);
            } else if (0 == token.Icmp("connectAttr")) {
                MA_ParseConnectAttr(parser);
            }
        }

        //Resolve The Materials
        for (int i = 0; i < maGlobal.model.objects.Num(); i++) {
            maGlobal.model.objects.oGet(i).materialRef = MA_AddMaterial(maGlobal.model.objects.oGet(i).materialName);
        }

        //Apply Transformation
        MA_ApplyTransformation(maGlobal.model);

        return maGlobal.model;
    }

    /*
     =================
     MA_Load
     =================
     */
    public static maModel_s MA_Load(final String fileName) {
        ByteBuffer[] buf = {null};
        long[] timeStamp = new long[1];
        maModel_s ma;

        fileSystem.ReadFile(fileName, buf, timeStamp);
        if (null == buf[0]) {
            return null;
        }

        try {
            ma = MA_Parse(bbtocb(buf[0]), fileName, false);
            ma.timeStamp = timeStamp;
        } catch (idException e) {
            common.Warning("%s", e.error);
            if (maGlobal.model != null) {
                MA_Free(maGlobal.model);
            }
            ma = null;
        }

//        fileSystem.FreeFile(buf);
        return ma;
    }

    /*
     =================
     MA_Free
     =================
     */
    public static void MA_Free(maModel_s ma) {
        int i;
        maObject_t obj;
        maMesh_t mesh;
        maMaterial_t material;

        if (NOT(ma)) {
            return;
        }
//        for (i = 0; i < ma.objects.Num(); i++) {
//            obj = ma.objects.oGet(i);
//
//            // free the base nesh
//            mesh = obj.mesh;
//
//            if (mesh.vertexes != null) {
//                Mem_Free(mesh.vertexes);
//            }
//            if (mesh.vertTransforms != null) {
//                Mem_Free(mesh.vertTransforms);
//            }
//            if (mesh.normals != null) {
//                Mem_Free(mesh.normals);
//            }
//            if (mesh.tvertexes != null) {
//                Mem_Free(mesh.tvertexes);
//            }
//            if (mesh.edges != null) {
//                Mem_Free(mesh.edges);
//            }
//            if (mesh.colors != null) {
//                Mem_Free(mesh.colors);
//            }
//            if (mesh.faces != null) {
//                Mem_Free(mesh.faces);
//            }
//            Mem_Free(obj);
//        }
//        ma.objects.Clear();
//
//        for (i = 0; i < ma.materials.Num(); i++) {
//            material = ma.materials.oGet(i);
//            Mem_Free(material);
//        }
//        ma.materials.Clear();
//
//        maTransform_s trans;
//        for (i = 0; i < ma.transforms.Num(); i++) {
//            trans = ma.transforms.GetIndex(i);
//            Mem_Free(trans);
//        }
//        ma.transforms.Clear();
//
//        maFileNode_t fileNode;
//        for (i = 0; i < ma.fileNodes.Num(); i++) {
//            fileNode = ma.fileNodes.GetIndex(i);
//            Mem_Free(fileNode);
//        }
//        ma.fileNodes.Clear();
//
//        maMaterialNode_s matNode;
//        for (i = 0; i < ma.materialNodes.Num(); i++) {
//            matNode = ma.materialNodes.GetIndex(i);
//            Mem_Free(matNode);
//        }
        ma.materialNodes.Clear();
//	delete ma;
    }
    /**
     *
     */
    public static ma_t maGlobal;
}
