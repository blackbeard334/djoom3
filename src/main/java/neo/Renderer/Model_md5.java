package neo.Renderer;

import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.dynamicModel_t;
import static neo.Renderer.Model.dynamicModel_t.DM_CACHED;
import neo.Renderer.Model.idMD5Joint;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.Model_local.idRenderModelStatic;
import static neo.Renderer.RenderWorld.SHADERPARM_MD5_SKINSCALE;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.tr_local.deformInfo_s;
import static neo.Renderer.tr_local.tr;
import neo.Renderer.tr_local.viewDef_s;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import static neo.Renderer.tr_trisurf.R_BuildDeformInfo;
import static neo.Renderer.tr_trisurf.R_DeriveTangents;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurfVertexCaches;
import static neo.framework.DeclManager.declManager;
import static neo.framework.Session.session;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Lib;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorMagenta;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.JointTransform.idJointQuat;
import static neo.idlib.math.Simd.SIMDProcessor;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import static neo.idlib.math.Vector.vec3_zero;

/**
 *
 */
public class Model_md5 {

    public static final String MD5_SnapshotName = "_MD5_Snapshot_";
    /**
     * *********************************************************************
     *
     * idMD5Mesh
     *
     **********************************************************************
     */
    static int c_numVerts = 0;
    static int c_numWeights = 0;
    static int c_numWeightJoints = 0;

    static class vertexWeight_s {

        int vert;
        int joint;
        idVec3 offset;
        float jointWeight;
    };

    /*
     ===============================================================================

     MD5 animated model

     ===============================================================================
     */
    public static class idMD5Mesh {
        // friend class				idRenderModelMD5;

        private idList<idVec2> texCoords;	// texture coordinates
        private int numWeights;			// number of weights
        private idVec4[] scaledWeights;		// joint weights
        private int[] weightIndex;		// pairs of: joint offset + bool true if next weight is for next vertex
        private idMaterial shader;		// material applied to mesh
        private int numTris;			// number of triangles
        private deformInfo_s deformInfo;	// used to create srfTriangles_t from base frames and new vertexes
        private int surfaceNum;			// number of the static surface created for this mesh
        //
        //

        public idMD5Mesh() {
            scaledWeights = null;
            weightIndex = null;
            shader = null;
            numTris = 0;
            deformInfo = null;
            surfaceNum = 0;
        }
        // ~idMD5Mesh();

        public void ParseMesh(idLexer parser, int numJoints, final idJointMat[] joints) throws Lib.idException {
            idToken token = new idToken();
            idToken name = new idToken();
            int num;
            int count;
            int jointnum;
            idStr shaderName;
            int i, j;
            idList<Integer> tris = new idList<>();
            idList<Integer> firstWeightForVertex = new idList<>();
            idList<Integer> numWeightsForVertex = new idList<>();
            int maxweight;
            idList<vertexWeight_s> tempWeights = new idList<>();

            parser.ExpectTokenString("{");

            //
            // parse name
            //
            if (parser.CheckTokenString("name")) {
                parser.ReadToken(name);
            }

            //
            // parse shader
            //
            parser.ExpectTokenString("shader");

            parser.ReadToken(token);
            shaderName = token;

            shader = declManager.FindMaterial(shaderName);

            //
            // parse texture coordinates
            //
            parser.ExpectTokenString("numverts");
            count = parser.ParseInt();
            if (count < 0) {
                parser.Error("Invalid size: %s", token.toString());
            }

            texCoords.SetNum(count);
            firstWeightForVertex.SetNum(count);
            numWeightsForVertex.SetNum(count);

            numWeights = 0;
            maxweight = 0;
            for (i = 0; i < texCoords.Num(); i++) {
                parser.ExpectTokenString("vert");
                parser.ParseInt();

                parser.Parse1DMatrix(2, texCoords.oGet(i).ToFloatPtr());

                firstWeightForVertex.oSet(i, parser.ParseInt());
                numWeightsForVertex.oSet(i, parser.ParseInt());

                if (0 == numWeightsForVertex.oGet(i)) {
                    parser.Error("Vertex without any joint weights.");
                }

                numWeights += numWeightsForVertex.oGet(i);
                if (numWeightsForVertex.oGet(i) + firstWeightForVertex.oGet(i) > maxweight) {
                    maxweight = numWeightsForVertex.oGet(i) + firstWeightForVertex.oGet(i);
                }
            }

            //
            // parse tris
            //
            parser.ExpectTokenString("numtris");
            count = parser.ParseInt();
            if (count < 0) {
                parser.Error("Invalid size: %d", count);
            }

            tris.SetNum(count * 3);
            numTris = count;
            for (i = 0; i < count; i++) {
                parser.ExpectTokenString("tri");
                parser.ParseInt();

                tris.oSet(i * 3 + 0, parser.ParseInt());
                tris.oSet(i * 3 + 1, parser.ParseInt());
                tris.oSet(i * 3 + 2, parser.ParseInt());
            }

            //
            // parse weights
            //
            parser.ExpectTokenString("numweights");
            count = parser.ParseInt();
            if (count < 0) {
                parser.Error("Invalid size: %d", count);
            }

            if (maxweight > count) {
                parser.Warning("Vertices reference out of range weights in model (%d of %d weights).", maxweight, count);
            }

            tempWeights.SetNum(count);

            for (i = 0; i < count; i++) {
                parser.ExpectTokenString("weight");
                parser.ParseInt();

                jointnum = parser.ParseInt();
                if ((jointnum < 0) || (jointnum >= numJoints)) {
                    parser.Error("Joint Index out of range(%d): %d", numJoints, jointnum);
                }

                tempWeights.oGet(i).joint = jointnum;
                tempWeights.oGet(i).jointWeight = parser.ParseFloat();

                parser.Parse1DMatrix(3, tempWeights.oGet(i).offset.ToFloatPtr());
            }

            // create pre-scaled weights and an index for the vertex/joint lookup
            scaledWeights = new idVec4[numWeights];
            weightIndex = new int[numWeights * 2];// Mem_Alloc16(numWeights * 2 /* sizeof( weightIndex[0] ) */);
//	memset( weightIndex, 0, numWeights * 2 * sizeof( weightIndex[0] ) );

            count = 0;
            for (i = 0; i < texCoords.Num(); i++) {
                num = firstWeightForVertex.oGet(i);
                for (j = 0; j < numWeightsForVertex.oGet(i); j++, num++, count++) {
                    scaledWeights[count].ToVec3().oSet(tempWeights.oGet(num).offset.oMultiply(tempWeights.oGet(num).jointWeight));
                    scaledWeights[count].w = tempWeights.oGet(num).jointWeight;
                    weightIndex[count * 2 + 0] = tempWeights.oGet(num).joint /* sizeof( idJointMat )*/;
                }
                weightIndex[count * 2 - 1] = 1;
            }

            tempWeights.Clear();
            numWeightsForVertex.Clear();
            firstWeightForVertex.Clear();

            parser.ExpectTokenString("}");

            // update counters
            c_numVerts += texCoords.Num();
            c_numWeights += numWeights;
            c_numWeightJoints++;
            for (i = 0; i < numWeights; i++) {
                c_numWeightJoints += weightIndex[i * 2 + 1];
            }

            //
            // build the information that will be common to all animations of this mesh:
            // silhouette edge connectivity and normal / tangent generation information
            //
            idDrawVert[] verts = new idDrawVert[texCoords.Num()];
            for (i = 0; i < texCoords.Num(); i++) {
                verts[i].Clear();
                verts[i].st = texCoords.oGet(i);
            }
            TransformVerts(verts, joints);
            deformInfo = R_BuildDeformInfo(texCoords.Num(), verts, tris.Num(), tris.Ptr(), shader.UseUnsmoothedTangents());
        }

        public void UpdateSurface(final renderEntity_s ent, final idJointMat[] entJoints, modelSurface_s surf) {
            int i, base;
            srfTriangles_s tri;

            tr.pc.c_deformedSurfaces++;
            tr.pc.c_deformedVerts += deformInfo.numOutputVerts;
            tr.pc.c_deformedIndexes += deformInfo.numIndexes;

            surf.shader = shader;

            if (surf.geometry != null) {
                // if the number of verts and indexes are the same we can re-use the triangle surface
                // the number of indexes must be the same to assure the correct amount of memory is allocated for the facePlanes
                if (surf.geometry.numVerts == deformInfo.numOutputVerts && surf.geometry.numIndexes == deformInfo.numIndexes) {
                    R_FreeStaticTriSurfVertexCaches(surf.geometry);
                } else {
                    R_FreeStaticTriSurf(surf.geometry);
                    surf.geometry = R_AllocStaticTriSurf();
                }
            } else {
                surf.geometry = R_AllocStaticTriSurf();
            }

            tri = surf.geometry;

            // note that some of the data is references, and should not be freed
            tri.deformedSurface = true;
            tri.tangentsCalculated = false;
            tri.facePlanesCalculated = false;

            tri.numIndexes = deformInfo.numIndexes;
            tri.indexes = deformInfo.indexes;
            tri.silIndexes = deformInfo.silIndexes;
            tri.numMirroredVerts = deformInfo.numMirroredVerts;
            tri.mirroredVerts = deformInfo.mirroredVerts;
            tri.numDupVerts = deformInfo.numDupVerts;
            tri.dupVerts = deformInfo.dupVerts;
            tri.numSilEdges = deformInfo.numSilEdges;
            tri.silEdges = deformInfo.silEdges;
            tri.dominantTris = deformInfo.dominantTris;
            tri.numVerts = deformInfo.numOutputVerts;

            if (tri.verts == null) {
                R_AllocStaticTriSurfVerts(tri, tri.numVerts);
                for (i = 0; i < deformInfo.numSourceVerts; i++) {
                    tri.verts[i].Clear();
                    tri.verts[i].st = texCoords.oGet(i);
                }
            }

            if (ent.shaderParms[ SHADERPARM_MD5_SKINSCALE] != 0.0f) {
                TransformScaledVerts(tri.verts, entJoints, ent.shaderParms[ SHADERPARM_MD5_SKINSCALE]);
            } else {
                TransformVerts(tri.verts, entJoints);
            }

            // replicate the mirror seam vertexes
            base = deformInfo.numOutputVerts - deformInfo.numMirroredVerts;
            for (i = 0; i < deformInfo.numMirroredVerts; i++) {
                tri.verts[base + i] = tri.verts[deformInfo.mirroredVerts[i]];
            }

            R_BoundTriSurf(tri);

            // If a surface is going to be have a lighting interaction generated, it will also have to call
            // R_DeriveTangents() to get normals, tangents, and face planes.  If it only
            // needs shadows generated, it will only have to generate face planes.  If it only
            // has ambient drawing, or is culled, no additional work will be necessary
            if (!RenderSystem_init.r_useDeferredTangents.GetBool()) {
                // set face planes, vertex normals, tangents
                R_DeriveTangents(tri);
            }
        }

        public idBounds CalcBounds(final idJointMat[] entJoints) {
            idBounds bounds = new idBounds();
            idDrawVert[] verts = new idDrawVert[texCoords.Num()];

            TransformVerts(verts, entJoints);

            SIMDProcessor.MinMax(bounds.oGet(0), bounds.oGet(1), verts, texCoords.Num());

            return bounds;
        }

        public int NearestJoint(int a, int b, int c) {
            int i, bestJoint, vertNum, weightVertNum;
            float bestWeight;

            // duplicated vertices might not have weights
            if (a >= 0 && a < texCoords.Num()) {
                vertNum = a;
            } else if (b >= 0 && b < texCoords.Num()) {
                vertNum = b;
            } else if (c >= 0 && c < texCoords.Num()) {
                vertNum = c;
            } else {
                // all vertices are duplicates which shouldn't happen
                return 0;
            }

            // find the first weight for this vertex
            weightVertNum = 0;
            for (i = 0; weightVertNum < vertNum; i++) {
                weightVertNum += weightIndex[i * 2 + 1];
            }

            // get the joint for the largest weight
            bestWeight = scaledWeights[i].w;
            bestJoint = weightIndex[i * 2 + 0];// sizeof(idJointMat);
            for (; weightIndex[i * 2 + 1] == 0; i++) {
                if (scaledWeights[i].w > bestWeight) {
                    bestWeight = scaledWeights[i].w;
                    bestJoint = weightIndex[i * 2 + 0];// sizeof(idJointMat);
                }
            }
            return bestJoint;
        }

        public int NumVerts() {
            return texCoords.Num();
        }

        public int NumTris() {
            return numTris;
        }

        public int NumWeights() {
            return numWeights;
        }

        private void TransformVerts(idDrawVert[] verts, final idJointMat[] entJoints) {
            SIMDProcessor.TransformVerts(verts, texCoords.Num(), entJoints, scaledWeights, weightIndex, numWeights);
        }

        /*
         ====================
         idMD5Mesh::TransformScaledVerts

         Special transform to make the mesh seem fat or skinny.  May be used for zombie deaths
         ====================
         */
        private void TransformScaledVerts(idDrawVert[] verts, final idJointMat[] entJoints, float scale) {
            idVec4[] scaledWeights = new idVec4[numWeights];
            SIMDProcessor.Mul(scaledWeights[0].ToFloatPtr(), scale, scaledWeights[0].ToFloatPtr(), numWeights * 4);
            SIMDProcessor.TransformVerts(verts, texCoords.Num(), entJoints, scaledWeights, weightIndex, numWeights);
        }
    };

    public static class idRenderModelMD5 extends idRenderModelStatic {

        private idList<idMD5Joint> joints;
        private idList<idJointQuat> defaultPose;
        private idList<idMD5Mesh> meshes;
        //
        //

        @Override
        public void InitFromFile(String fileName) {
            name = new idStr(fileName);
            LoadModel();
        }

        @Override
        public dynamicModel_t IsDynamicModel() {
            return DM_CACHED;
        }

        /*
         ====================
         idRenderModelMD5::Bounds

         This calculates a rough bounds by using the joint radii without
         transforming all the points
         ====================
         */
        @Override
        public idBounds Bounds(renderEntity_s ent) {
//            if (false) {
//                // we can't calculate a rational bounds without an entity,
//                // because joints could be positioned to deform it into an
//                // arbitrarily large shape
//                if (null == ent) {
//                    common.Error("idRenderModelMD5::Bounds: called without entity");
//                }
//            }

            if (null == ent) {
                // this is the bounds for the reference pose
                return bounds;
            }

            return ent.bounds;
        }

        @Override
        public void Print() {
            super.Print();
        }

        @Override
        public void List() {
            super.List();
        }

        @Override
        public void TouchData() {
            super.TouchData();
        }

        @Override
        public void PurgeModel() {
            super.PurgeModel();
        }

        @Override
        public void LoadModel() {
            super.LoadModel();
        }

        @Override
        public int Memory() {
            return super.Memory();
        }

        @Override
        public idRenderModel InstantiateDynamicModel(renderEntity_s ent, viewDef_s view, idRenderModel cachedModel) {
            return super.InstantiateDynamicModel(ent, view, cachedModel);
        }

        @Override
        public int NumJoints() {
            return super.NumJoints();
        }

        @Override
        public idMD5Joint[] GetJoints() {
            return super.GetJoints();
        }

        @Override
        public int GetJointHandle(String name) {
            return super.GetJointHandle(name);
        }

        @Override
        public String GetJointName(int jointHandle_t) {
            return super.GetJointName(jointHandle_t);
        }

        @Override
        public idJointQuat GetDefaultPose() {
            return super.GetDefaultPose();
        }

        @Override
        public int NearestJoint(int surfaceNum, int a, int c, int b) {
            return super.NearestJoint(surfaceNum, a, c, b);
        }

        private void CalculateBounds(final idJointMat[] entJoints) {
            int i;
            idMD5Mesh mesh;

            bounds.Clear();
            for (mesh = meshes.oGet(i = 0); i < meshes.Num(); mesh = meshes.oGet(++i)) {
                bounds.AddBounds(mesh.CalcBounds(entJoints));
            }
        }

//        private void GetFrameBounds(final renderEntity_t ent, idBounds bounds);
        private void DrawJoints(final renderEntity_s ent, final viewDef_s view) {
            int i;
            int num;
            idVec3 pos;
            idJointMat joint;
            idMD5Joint md5Joint;
            int parentNum;

            num = ent.numJoints;
            joint = ent.joints[0];
            md5Joint = joints.oGet(0);
            for (i = 0; i < num; joint = ent.joints[++i], md5Joint = joints.oGet(i)) {
                pos = ent.origin.oPlus(joint.ToVec3().oMultiply(ent.axis));
                if (md5Joint.parent != null) {
//                    parentNum = indexOf(md5Joint.parent, joints.Ptr());
                    parentNum = joints.IndexOf(md5Joint.parent);
                    session.rw.DebugLine(colorWhite, ent.origin.oPlus(ent.joints[parentNum].ToVec3().oMultiply(ent.axis)), pos);
                }

                session.rw.DebugLine(colorRed, pos, pos.oPlus(joint.ToMat3().oGet(0).oMultiply(2.0f).oMultiply(ent.axis)));
                session.rw.DebugLine(colorGreen, pos, pos.oPlus(joint.ToMat3().oGet(1).oMultiply(2.0f).oMultiply(ent.axis)));
                session.rw.DebugLine(colorBlue, pos, pos.oPlus(joint.ToMat3().oGet(2).oMultiply(2.0f).oMultiply(ent.axis)));
            }

            idBounds bounds = new idBounds();

            bounds.FromTransformedBounds(ent.bounds, vec3_zero, ent.axis);
            session.rw.DebugBounds(colorMagenta, bounds, ent.origin);

            if ((RenderSystem_init.r_jointNameScale.GetFloat() != 0.0f) && (bounds.Expand(128.0f).ContainsPoint(view.renderView.vieworg.oMinus(ent.origin)))) {
                idVec3 offset = new idVec3(0, 0, RenderSystem_init.r_jointNameOffset.GetFloat());
                float scale;

                scale = RenderSystem_init.r_jointNameScale.GetFloat();
                joint = ent.joints[0];
                num = ent.numJoints;
                for (i = 0; i < num; joint = ent.joints[++i]) {
                    pos = ent.origin.oPlus(joint.ToVec3().oMultiply(ent.axis));
                    session.rw.DrawText(joints.oGet(i).name.toString(), pos.oPlus(offset), scale, colorWhite, view.renderView.viewaxis, 1);
                }
            }
        }

        private void ParseJoint(idLexer parser, idMD5Joint joint, idJointQuat defaultPose) throws Lib.idException {
            final idToken token = new idToken();
            int num;

            //
            // parse name
            //
            parser.ReadToken(token);
            joint.name = token;

            //
            // parse parent
            //
            num = parser.ParseInt();
            if (num < 0) {
                joint.parent = null;
            } else {
                if (num >= joints.Num() - 1) {
                    parser.Error("Invalid parent for joint '%s'", joint.name);
                }
                joint.parent = joints.oGet(num);
            }

            //
            // parse default pose
            //
            parser.Parse1DMatrix(3, defaultPose.t.ToFloatPtr());
            parser.Parse1DMatrix(3, defaultPose.q.ToFloatPtr());
            defaultPose.q.w = defaultPose.q.CalcW();
        }
    };
}
