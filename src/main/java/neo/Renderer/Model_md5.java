package neo.Renderer;

import static neo.Renderer.Model.INVALID_JOINT;
import static neo.Renderer.Model.MD5_VERSION;
import static neo.Renderer.Model.MD5_VERSION_STRING;
import static neo.Renderer.Model.dynamicModel_t.DM_CACHED;
import static neo.Renderer.RenderSystem_init.r_showSkel;
import static neo.Renderer.RenderSystem_init.r_skipSuppress;
import static neo.Renderer.RenderSystem_init.r_useCachedDynamicModels;
import static neo.Renderer.RenderWorld.R_RemapShaderBySkin;
import static neo.Renderer.RenderWorld.SHADERPARM_MD5_SKINSCALE;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import static neo.Renderer.tr_trisurf.R_BuildDeformInfo;
import static neo.Renderer.tr_trisurf.R_DeformInfoMemoryUsed;
import static neo.Renderer.tr_trisurf.R_DeriveTangents;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_FreeStaticTriSurfVertexCaches;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.Session.session;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorMagenta;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.idLib.fileSystem;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.idlib.math.Vector.getVec3_zero;

import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.dynamicModel_t;
import neo.Renderer.Model.idMD5Joint;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.ModelOverlay.idRenderModelOverlay;
import neo.Renderer.Model_local.idRenderModelStatic;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.tr_local.deformInfo_s;
import neo.Renderer.tr_local.viewDef_s;
import neo.idlib.Lib;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.JointTransform.idJointMat;
import neo.idlib.geometry.JointTransform.idJointQuat;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;

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

        public vertexWeight_s() {
            this.offset = new idVec3();
        }
    }

    /*
     ===============================================================================

     MD5 animated model

     ===============================================================================
     */
    public static class idMD5Mesh {
        // friend class				idRenderModelMD5;

        private final idList<idVec2> texCoords;    // texture coordinates
        private int            numWeights;   // number of weights
        private idVec4[]       scaledWeights;// joint weights
        private int[]          weightIndex;  // pairs of: joint offset + bool true if next weight is for next vertex
        private idMaterial     shader;       // material applied to mesh
        private int            numTris;      // number of triangles
        private deformInfo_s   deformInfo;   // used to create srfTriangles_t from base frames and new vertexes
        private int            surfaceNum;   // number of the static surface created for this mesh
        //
        //

        public idMD5Mesh() {
            this.texCoords = new idList<>();
            this.scaledWeights = null;
            this.weightIndex = null;
            this.shader = null;
            this.numTris = 0;
            this.deformInfo = null;
            this.surfaceNum = 0;
        }
        // ~idMD5Mesh();

        public void ParseMesh(idLexer parser, int numJoints, final idJointMat[] joints) throws Lib.idException {
            final idToken token = new idToken();
            final idToken name = new idToken();
            int num;
            int count;
            int jointnum;
            idStr shaderName;
            int i, j;
            final idList<Integer> tris = new idList<>();
            final idList<Integer> firstWeightForVertex = new idList<>();
            final idList<Integer> numWeightsForVertex = new idList<>();
            int maxweight;
            final idList<vertexWeight_s> tempWeights = new idList<>();

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

            this.shader = declManager.FindMaterial(shaderName);

            //
            // parse texture coordinates
            //
            parser.ExpectTokenString("numverts");
            count = parser.ParseInt();
            if (count < 0) {
                parser.Error("Invalid size: %s", token.getData());
            }

            this.texCoords.SetNum(count);
            firstWeightForVertex.SetNum(count);
            numWeightsForVertex.SetNum(count);

            this.numWeights = 0;
            maxweight = 0;
            for (i = 0; i < this.texCoords.Num(); i++) {
                parser.ExpectTokenString("vert");
                parser.ParseInt();

                parser.Parse1DMatrix(2, this.texCoords.oSet(i, new idVec2()));

                firstWeightForVertex.oSet(i, parser.ParseInt());
                numWeightsForVertex.oSet(i, parser.ParseInt());

                if (0 == numWeightsForVertex.oGet(i)) {
                    parser.Error("Vertex without any joint weights.");
                }

                this.numWeights += numWeightsForVertex.oGet(i);
                if ((numWeightsForVertex.oGet(i) + firstWeightForVertex.oGet(i)) > maxweight) {
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
            this.numTris = count;
            for (i = 0; i < count; i++) {
                parser.ExpectTokenString("tri");
                parser.ParseInt();

                tris.oSet((i * 3) + 0, parser.ParseInt());
                tris.oSet((i * 3) + 1, parser.ParseInt());
                tris.oSet((i * 3) + 2, parser.ParseInt());
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

                tempWeights.oSet(i, new vertexWeight_s());
                tempWeights.oGet(i).joint = jointnum;
                tempWeights.oGet(i).jointWeight = parser.ParseFloat();

                parser.Parse1DMatrix(3, tempWeights.oGet(i).offset);
            }

            // create pre-scaled weights and an index for the vertex/joint lookup
            this.scaledWeights = new idVec4[this.numWeights];
            this.weightIndex = new int[this.numWeights * 2];// Mem_Alloc16(numWeights * 2 /* sizeof( weightIndex[0] ) */);
//	memset( weightIndex, 0, numWeights * 2 * sizeof( weightIndex[0] ) );

            count = 0;
            for (i = 0; i < this.texCoords.Num(); i++) {
                num = firstWeightForVertex.oGet(i);
                for (j = 0; j < numWeightsForVertex.oGet(i); j++, num++, count++) {
                    this.scaledWeights[count] = new idVec4();
                    this.scaledWeights[count].oSet(tempWeights.oGet(num).offset.oMultiply(tempWeights.oGet(num).jointWeight));
                    this.scaledWeights[count].w = tempWeights.oGet(num).jointWeight;
                    this.weightIndex[(count * 2) + 0] = tempWeights.oGet(num).joint * idJointMat.SIZE;
                }
                this.weightIndex[(count * 2) - 1] = 1;
            }

            tempWeights.Clear();
            numWeightsForVertex.Clear();
            firstWeightForVertex.Clear();

            parser.ExpectTokenString("}");

            // update counters
            c_numVerts += this.texCoords.Num();
            c_numWeights += this.numWeights;
            c_numWeightJoints++;
            for (i = 0; i < this.numWeights; i++) {
                c_numWeightJoints += this.weightIndex[(i * 2) + 1];
            }

            //
            // build the information that will be common to all animations of this mesh:
            // silhouette edge connectivity and normal / tangent generation information
            //
            final idDrawVert[] verts = new idDrawVert[this.texCoords.Num()];
            for (i = 0; i < this.texCoords.Num(); i++) {
                verts[i] = new idDrawVert();
                verts[i].st = this.texCoords.oGet(i);
            }
            TransformVerts(verts, joints);
            this.deformInfo = R_BuildDeformInfo(this.texCoords.Num(), verts, tris.Num(), tris, this.shader.UseUnsmoothedTangents());
        }

        public void UpdateSurface(final renderEntity_s ent, final idJointMat[] entJoints, modelSurface_s surf) {
            int i, base;
            srfTriangles_s tri;

            tr.pc.c_deformedSurfaces++;
            tr.pc.c_deformedVerts += this.deformInfo.numOutputVerts;
            tr.pc.c_deformedIndexes += this.deformInfo.getIndexes().getNumValues();

            surf.shader = this.shader;

            if (surf.geometry != null) {
                // if the number of verts and indexes are the same we can re-use the triangle surface
                // the number of indexes must be the same to assure the correct amount of memory is allocated for the facePlanes
                if ((surf.geometry.numVerts == this.deformInfo.numOutputVerts) && (surf.geometry.getIndexes().getNumValues() == this.deformInfo.getIndexes().getNumValues())) {
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

            tri.getIndexes().setNumValues(this.deformInfo.getIndexes().getNumValues());
            tri.getIndexes().setValues(this.deformInfo.getIndexes().getValues());
            tri.silIndexes = this.deformInfo.silIndexes;
            tri.numMirroredVerts = this.deformInfo.numMirroredVerts;
            tri.mirroredVerts = this.deformInfo.mirroredVerts;
            tri.numDupVerts = this.deformInfo.numDupVerts;
            tri.dupVerts = this.deformInfo.dupVerts;
            tri.numSilEdges = this.deformInfo.numSilEdges;
            tri.silEdges = this.deformInfo.silEdges;
            tri.dominantTris = this.deformInfo.dominantTris;
            tri.numVerts = this.deformInfo.numOutputVerts;

            if (tri.verts == null) {
                R_AllocStaticTriSurfVerts(tri, tri.numVerts);
                for (i = 0; i < this.deformInfo.numSourceVerts; i++) {
                    tri.verts[i].Clear();
                    tri.verts[i].st.oSet(this.texCoords.oGet(i));
                }
            }

            if (ent.shaderParms[ SHADERPARM_MD5_SKINSCALE] != 0.0f) {
                TransformScaledVerts(tri.verts, entJoints, ent.shaderParms[ SHADERPARM_MD5_SKINSCALE]);
            } else {
                TransformVerts(tri.verts, entJoints);
            }

            // replicate the mirror seam vertexes
            base = this.deformInfo.numOutputVerts - this.deformInfo.numMirroredVerts;
            for (i = 0; i < this.deformInfo.numMirroredVerts; i++) {
                tri.verts[base + i] = tri.verts[this.deformInfo.mirroredVerts[i]];
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
            final idBounds bounds = new idBounds();
            final idDrawVert[] verts = new idDrawVert[this.texCoords.Num()];

            TransformVerts(verts, entJoints);

            SIMDProcessor.MinMax(bounds.oGet(0), bounds.oGet(1), verts, this.texCoords.Num());

            return bounds;
        }

        public int NearestJoint(int a, int b, int c) {
            int i, bestJoint, vertNum, weightVertNum;
            float bestWeight;

            // duplicated vertices might not have weights
            if ((a >= 0) && (a < this.texCoords.Num())) {
                vertNum = a;
            } else if ((b >= 0) && (b < this.texCoords.Num())) {
                vertNum = b;
            } else if ((c >= 0) && (c < this.texCoords.Num())) {
                vertNum = c;
            } else {
                // all vertices are duplicates which shouldn't happen
                return 0;
            }

            // find the first weight for this vertex
            weightVertNum = 0;
            for (i = 0; weightVertNum < vertNum; i++) {
                weightVertNum += this.weightIndex[(i * 2) + 1] * idJointMat.SIZE;
            }

            // get the joint for the largest weight
            bestWeight = this.scaledWeights[i].w;
            bestJoint = this.weightIndex[(i * 2) + 0] / idJointMat.SIZE;
            for (; this.weightIndex[(i * 2) + 1] == 0; i++) {
                if (this.scaledWeights[i].w > bestWeight) {
                    bestWeight = this.scaledWeights[i].w;
                    bestJoint = this.weightIndex[(i * 2) + 0] / idJointMat.SIZE;
                }
            }
            return bestJoint;
        }

        public int NumVerts() {
            return this.texCoords.Num();
        }

        public int NumTris() {
            return this.numTris;
        }

        public int NumWeights() {
            return this.numWeights;
        }

        private void TransformVerts(idDrawVert[] verts, final idJointMat[] entJoints) {
            SIMDProcessor.TransformVerts(verts, this.texCoords.Num(), entJoints, this.scaledWeights, this.weightIndex, this.numWeights);
        }

        /*
         ====================
         idMD5Mesh::TransformScaledVerts

         Special transform to make the mesh seem fat or skinny.  May be used for zombie deaths
         ====================
         */
        private void TransformScaledVerts(idDrawVert[] verts, final idJointMat[] entJoints, float scale) {
            final idVec4[] scaledWeights = new idVec4[this.numWeights];
            SIMDProcessor.Mul(scaledWeights[0].ToFloatPtr(), scale, scaledWeights[0].ToFloatPtr(), this.numWeights * 4);
            SIMDProcessor.TransformVerts(verts, this.texCoords.Num(), entJoints, scaledWeights, this.weightIndex, this.numWeights);
        }
    }

    public static class idRenderModelMD5 extends idRenderModelStatic {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final int BYTES = Integer.BYTES * 3;

        private final idList<idMD5Joint> joints;
        private final idList<idJointQuat> defaultPose;
        private final idList<idMD5Mesh> meshes;
        //
        //

        public idRenderModelMD5() {
            this.joints = new idList<>();
            this.defaultPose = new idList<>();
            this.meshes = new idList<>();
        }

        @Override
        public void InitFromFile(String fileName) {
            this.name = new idStr(fileName);
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
                return this.bounds;
            }

            return ent.bounds;
        }

        @Override
        public void Print() {
            int i = 0;

            common.Printf("%s\n", this.name.getData());
            common.Printf("Dynamic model.\n");
            common.Printf("Generated smooth normals.\n");
            common.Printf("    verts  tris weights material\n");
            int totalVerts = 0;
            int totalTris = 0;
            int totalWeights = 0;
            for (final idMD5Mesh mesh : this.meshes.Ptr()) {
                totalVerts += mesh.NumVerts();
                totalTris += mesh.NumTris();
                totalWeights += mesh.NumWeights();
                common.Printf("%2d: %5d %5d %7d %s\n", i++, mesh.NumVerts(), mesh.NumTris(), mesh.NumWeights(), mesh.shader.GetName());
            }
            common.Printf("-----\n");
            common.Printf("%4d verts.\n", totalVerts);
            common.Printf("%4d tris.\n", totalTris);
            common.Printf("%4d weights.\n", totalWeights);
            common.Printf("%4d joints.\n", this.joints.Num());
        }

        @Override
        public void List() {
            int totalTris = 0;
            int totalVerts = 0;

            for (final idMD5Mesh mesh : this.meshes.Ptr()) {
                totalTris += mesh.numTris;
                totalVerts += mesh.NumVerts();
            }
            common.Printf(" %4dk %3d %4d %4d %s(MD5)", Memory() / 1024, this.meshes.Num(), totalVerts, totalTris, Name());

            if (this.defaulted) {
                common.Printf(" (DEFAULTED)");
            }

            common.Printf("\n");
        }

        /*
         ====================
         idRenderModelMD5::TouchData

         models that are already loaded at level start time
         will still touch their materials to make sure they
         are kept loaded
         ====================
         */
        @Override
        public void TouchData() {
            for (final idMD5Mesh mesh : this.meshes.Ptr(idMD5Mesh[].class)) {
                declManager.FindMaterial(mesh.shader.GetName());
            }
        }

        /*
         ===================
         idRenderModelMD5::PurgeModel

         frees all the data, but leaves the class around for dangling references,
         which can regenerate the data with LoadModel()
         ===================
         */
        @Override
        public void PurgeModel() {
            this.purged = true;
            this.joints.Clear();
            this.defaultPose.Clear();
            this.meshes.Clear();
        }

        /*
         ====================
         idRenderModelMD5::LoadModel

         used for initial loads, reloadModel, and reloading the data of purged models
         Upon exit, the model will absolutely be valid, but possibly as a default model
         ====================
         */
        @Override
        public void LoadModel() {
            int version;
            int i;
            int num;
            int parentNum;
            final idToken token = new idToken();
            final idLexer parser = new idLexer(LEXFL_ALLOWPATHNAMES | LEXFL_NOSTRINGESCAPECHARS);
            idJointMat[] poseMat3;

            if (!this.purged) {
                PurgeModel();
            }
            this.purged = false;

            if (!parser.LoadFile(this.name)) {
                MakeDefaultModel();
                return;
            }

            parser.ExpectTokenString(MD5_VERSION_STRING);
            version = parser.ParseInt();

            if (version != MD5_VERSION) {
                parser.Error("Invalid version %d.  Should be version %d\n", version, MD5_VERSION);
            }

            //
            // skip commandline
            //
            parser.ExpectTokenString("commandline");
            parser.ReadToken(token);

            // parse num joints
            parser.ExpectTokenString("numJoints");
            num = parser.ParseInt();
            this.joints.SetGranularity(1);
            this.joints.SetNum(num);
            this.defaultPose.SetGranularity(1);
            this.defaultPose.SetNum(num);
            poseMat3 = new idJointMat[num];

            // parse num meshes
            parser.ExpectTokenString("numMeshes");
            num = parser.ParseInt();
            if (num < 0) {
                parser.Error("Invalid size: %d", num);
            }
            this.meshes.SetGranularity(1);
            this.meshes.SetNum(num);

            //
            // parse joints
            //
            parser.ExpectTokenString("joints");
            parser.ExpectTokenString("{");
            for (i = 0; i < this.joints.Num(); i++) {
                final idJointQuat pose = this.defaultPose.oSet(i, new idJointQuat());
                final idMD5Joint joint = this.joints.oSet(i, new idMD5Joint());
                ParseJoint(parser, joint, pose);
                poseMat3[i] = new idJointMat();
                poseMat3[i].SetRotation(pose.q.ToMat3());
                poseMat3[i].SetTranslation(pose.t);
                if (joint.parent != null) {
                    parentNum = this.joints.Find(joint.parent);
                    pose.q = (poseMat3[i].ToMat3().oMultiply(poseMat3[parentNum].ToMat3().Transpose())).ToQuat();
                    pose.t = (poseMat3[i].ToVec3().oMinus(poseMat3[parentNum].ToVec3())).oMultiply(poseMat3[parentNum].ToMat3().Transpose());
                }
            }
            parser.ExpectTokenString("}");

            for( i = 0; i < this.meshes.Num(); i++ ) {
                final idMD5Mesh mesh = this.meshes.oSet(i, new idMD5Mesh());
                parser.ExpectTokenString("mesh");
                mesh.ParseMesh(parser, this.defaultPose.Num(), poseMat3);
            }

            //
            // calculate the bounds of the model
            //
            CalculateBounds(poseMat3);

            // set the timestamp for reloadmodels
            fileSystem.ReadFile(this.name, null, this.timeStamp);
        }


        @Override
        public int Memory() {
            int total;

            total = idRenderModelMD5.BYTES;
            total += this.joints.MemoryUsed() + this.defaultPose.MemoryUsed() + this.meshes.MemoryUsed();

            // count up strings
            for (final idMD5Joint joint : this.joints.Ptr()) {
                total += joint.name.DynamicMemoryUsed();
            }

            // count up meshes
            for (final idMD5Mesh mesh : this.meshes.Ptr()) {

                total += mesh.texCoords.MemoryUsed() + (mesh.numWeights * idVec4.BYTES) + (Integer.BYTES * 2);

                // sum up deform info
                total += deformInfo_s.BYTES;
                total += R_DeformInfoMemoryUsed(mesh.deformInfo);
            }
            return total;
        }

        @Override
        public idRenderModel InstantiateDynamicModel(final renderEntity_s ent, final viewDef_s view, idRenderModel cachedModel) {
            final int[] surfaceNum = {0};
            idRenderModelStatic staticModel;

            if ((cachedModel != null) && !r_useCachedDynamicModels.GetBool()) {
                cachedModel = null;
            }

            if (this.purged) {
                common.DWarning("model %s instantiated while purged", Name());
                LoadModel();
            }

            if (null == ent.joints) {
                common.Printf("idRenderModelMD5::InstantiateDynamicModel: NULL joints on renderEntity for '%s'\n", Name());
                return null;
            } else if (ent.numJoints != this.joints.Num()) {
                common.Printf("idRenderModelMD5::InstantiateDynamicModel: renderEntity has different number of joints than model for '%s'\n", Name());
                return null;
            }

            tr.pc.c_generateMd5++;

            if (cachedModel != null) {
                assert (cachedModel instanceof idRenderModelStatic);
                assert (idStr.Icmp(cachedModel.Name(), MD5_SnapshotName) == 0);
                staticModel = (idRenderModelStatic) cachedModel;
            } else {
                staticModel = new idRenderModelStatic();
                staticModel.InitEmpty(MD5_SnapshotName);
            }

            staticModel.bounds.Clear();

            if (r_showSkel.GetInteger() != 0) {
                if ((view != null) && (!r_skipSuppress.GetBool() || (0 == ent.suppressSurfaceInViewID) || (ent.suppressSurfaceInViewID != view.renderView.viewID))) {
                    // only draw the skeleton
                    DrawJoints(ent, view);
                }

                if (r_showSkel.GetInteger() > 1) {
                    // turn off the model when showing the skeleton
                    staticModel.InitEmpty(MD5_SnapshotName);
                    return staticModel;
                }
            }

            // create all the surfaces
            for (int i = 0; i < this.meshes.Num(); i++) {
                final idMD5Mesh mesh = this.meshes.Ptr(idMD5Mesh[].class)[i];
                        
		// avoid deforming the surface if it will be a nodraw due to a skin remapping
                // FIXME: may have to still deform clipping hulls
                idMaterial shader = mesh.shader;

                shader = R_RemapShaderBySkin(shader, ent.customSkin, ent.customShader);

                if ((null == shader) || (!shader.IsDrawn() && !shader.SurfaceCastsShadow())) {
                    staticModel.DeleteSurfaceWithId(i);
                    mesh.surfaceNum = -1;
                    continue;
                }

                modelSurface_s surf;

                if (staticModel.FindSurfaceWithId(i, surfaceNum)) {
                    mesh.surfaceNum = surfaceNum[0];
                    surf = staticModel.surfaces.oGet(surfaceNum[0]);
                } else {

                    // Remove Overlays before adding new surfaces
                    idRenderModelOverlay.RemoveOverlaySurfacesFromModel(staticModel);

                    mesh.surfaceNum = staticModel.NumSurfaces();
                    surf = staticModel.surfaces.Alloc();
                    surf.geometry = null;
                    surf.shader = null;
                    surf.id = i;
                }

                mesh.UpdateSurface(ent, ent.joints, surf);

                staticModel.bounds.AddPoint(surf.geometry.bounds.oGet(0));
                staticModel.bounds.AddPoint(surf.geometry.bounds.oGet(1));
                final int a = 0;
            }

            return staticModel;
        }

        @Override
        public int NumJoints() {
            return this.joints.Num();
        }

        @Override
        public idMD5Joint[] GetJoints() {
            return this.joints.Ptr(idMD5Joint[].class);
        }

        @Override
        public int GetJointHandle(final String name) {
            int i = 0;

            for (final idMD5Joint joint : this.joints.Ptr(idMD5Joint[].class)) {
                if (idStr.Icmp(joint.name, name) == 0) {
                    return i;
                }
                i++;
            }

            return INVALID_JOINT;
        }

        @Override
        public String GetJointName(int handle) {
            if ((handle < 0) || (handle >= this.joints.Num())) {
                return "<invalid joint>";
            }

            return this.joints.oGet(handle).name.getData();
        }

        @Override
        public idJointQuat[] GetDefaultPose() {
            return this.defaultPose.Ptr(idJointQuat[].class);
        }

        @Override
        public int NearestJoint(int surfaceNum, int a, int c, int b) {
            if (surfaceNum > this.meshes.Num()) {
                common.Error("idRenderModelMD5::NearestJoint: surfaceNum > meshes.Num()");
            }

            for (final idMD5Mesh mesh : this.meshes.Ptr(idMD5Mesh[].class)) {
                if (mesh.surfaceNum == surfaceNum) {
                    return mesh.NearestJoint(a, b, c);
                }
            }
            return 0;
        }

        private void CalculateBounds(final idJointMat[] entJoints) {
            int i;
            
            this.bounds.Clear();
            for (i = 0; i < this.meshes.Num(); ++i) {
                this.bounds.AddBounds(this.meshes.oGet(i).CalcBounds(entJoints));
                final int a = 0;
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
            md5Joint = this.joints.oGet(0);
            for (i = 0; i < num; joint = ent.joints[++i], md5Joint = this.joints.oGet(i)) {
                pos = ent.origin.oPlus(joint.ToVec3().oMultiply(ent.axis));
                if (md5Joint.parent != null) {
//                    parentNum = indexOf(md5Joint.parent, joints.Ptr());
                    parentNum = this.joints.IndexOf(md5Joint.parent);
                    session.rw.DebugLine(colorWhite, ent.origin.oPlus(ent.joints[parentNum].ToVec3().oMultiply(ent.axis)), pos);
                }

                session.rw.DebugLine(colorRed, pos, pos.oPlus(joint.ToMat3().oGet(0).oMultiply(2.0f).oMultiply(ent.axis)));
                session.rw.DebugLine(colorGreen, pos, pos.oPlus(joint.ToMat3().oGet(1).oMultiply(2.0f).oMultiply(ent.axis)));
                session.rw.DebugLine(colorBlue, pos, pos.oPlus(joint.ToMat3().oGet(2).oMultiply(2.0f).oMultiply(ent.axis)));
            }

            final idBounds bounds = new idBounds();

            bounds.FromTransformedBounds(ent.bounds, getVec3_zero(), ent.axis);
            session.rw.DebugBounds(colorMagenta, bounds, ent.origin);

            if ((RenderSystem_init.r_jointNameScale.GetFloat() != 0.0f) && (bounds.Expand(128.0f).ContainsPoint(view.renderView.vieworg.oMinus(ent.origin)))) {
                final idVec3 offset = new idVec3(0, 0, RenderSystem_init.r_jointNameOffset.GetFloat());
                float scale;

                scale = RenderSystem_init.r_jointNameScale.GetFloat();
                joint = ent.joints[0];
                num = ent.numJoints;
                for (i = 0; i < num; joint = ent.joints[++i]) {
                    pos = ent.origin.oPlus(joint.ToVec3().oMultiply(ent.axis));
                    session.rw.DrawText(this.joints.oGet(i).name.getData(), pos.oPlus(offset), scale, colorWhite, view.renderView.viewaxis, 1);
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
                if (num >= (this.joints.Num() - 1)) {
                    parser.Error("Invalid parent for joint '%s'", joint.name);
                }
                joint.parent = this.joints.oGet(num);
            }

            //
            // parse default pose
            //
            parser.Parse1DMatrix(3, defaultPose.t);
            parser.Parse1DMatrix(3, defaultPose.q);
            defaultPose.q.w = defaultPose.q.CalcW();
        }
    }
}
