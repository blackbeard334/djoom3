package neo.Renderer;

import static neo.Renderer.Model.dynamicModel_t.DM_CONTINUOUS;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import static neo.Renderer.tr_trisurf.R_BuildDeformInfo;
import static neo.Renderer.tr_trisurf.R_DeriveTangents;
import static neo.TempDump.NOT;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import static neo.idlib.containers.List.idSwap;
import static neo.idlib.math.Math_h.SEC2MS;
import static neo.idlib.math.Simd.SIMDProcessor;

import java.util.Arrays;

import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.dynamicModel_t;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.Model_local.idRenderModelStatic;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.tr_local.deformInfo_s;
import neo.Renderer.tr_local.viewDef_s;
import neo.idlib.Lib.idException;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Parser.idParser;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class Model_liquid {

    public static final int LIQUID_MAX_SKIP_FRAMES = 5;
    public static final int LIQUID_MAX_TYPES = 3;
//    

    /*
     ===============================================================================

     Liquid model

     ===============================================================================
     */
    public static class idRenderModelLiquid extends idRenderModelStatic {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int verts_x;
        private int verts_y;
        private float scale_x;
        private float scale_y;
        private int time;
        private int liquid_type;
        private int update_tics;
        private int seed;
        //
        private idRandom random;
        //						
        private idMaterial shader;
        private deformInfo_s deformInfo;		// used to create srfTriangles_s from base frames and new vertexes
        //        
        //						
        private float density;
        private float drop_height;
        private int drop_radius;
        private float drop_delay;
        //
        private idList<Float> pages;
        private Float[] page1;
        private Float[] page2;
        //
        private idList<idDrawVert> verts;
        //
        int nextDropTime;
        //
        //

        public idRenderModelLiquid() {
            this.verts_x = 32;
            this.verts_y = 32;
            this.scale_x = 256.0f;
            this.scale_y = 256.0f;
            this.liquid_type = 0;
            this.density = 0.97f;
            this.drop_height = 4;
            this.drop_radius = 4;
            this.drop_delay = 1000;
            this.shader = declManager.FindMaterial((String) null);
            this.update_tics = 33;  // ~30 hz
            this.time = 0;
            this.seed = 0;

            this.random.SetSeed(0);
        }

        @Override
        public void InitFromFile(String fileName) throws idException {
            int i, x, y;
            final idToken token = new idToken();
            final idParser parser = new idParser(LEXFL_ALLOWPATHNAMES | LEXFL_NOSTRINGESCAPECHARS);
            final idList<Integer> tris = new idList<>();
            float size_x, size_y;
            float rate;

            this.name = new idStr(fileName);

            if (!parser.LoadFile(fileName)) {
                MakeDefaultModel();
                return;
            }

            size_x = this.scale_x * this.verts_x;
            size_y = this.scale_y * this.verts_y;

            while (parser.ReadToken(token)) {
                if (0 == token.Icmp("seed")) {
                    this.seed = parser.ParseInt();
                } else if (0 == token.Icmp("size_x")) {
                    size_x = parser.ParseFloat();
                } else if (0 == token.Icmp("size_y")) {
                    size_y = parser.ParseFloat();
                } else if (0 == token.Icmp("verts_x")) {
                    this.verts_x = (int) parser.ParseFloat();
                    if (this.verts_x < 2) {
                        parser.Warning("Invalid # of verts.  Using default model.");
                        MakeDefaultModel();
                        return;
                    }
                } else if (0 == token.Icmp("verts_y")) {
                    this.verts_y = (int) parser.ParseFloat();
                    if (this.verts_y < 2) {
                        parser.Warning("Invalid # of verts.  Using default model.");
                        MakeDefaultModel();
                        return;
                    }
                } else if (0 == token.Icmp("liquid_type")) {
                    this.liquid_type = parser.ParseInt() - 1;
                    if ((this.liquid_type < 0) || (this.liquid_type >= LIQUID_MAX_TYPES)) {
                        parser.Warning("Invalid liquid_type.  Using default model.");
                        MakeDefaultModel();
                        return;
                    }
                } else if (0 == token.Icmp("density")) {
                    this.density = parser.ParseFloat();
                } else if (0 == token.Icmp("drop_height")) {
                    this.drop_height = parser.ParseFloat();
                } else if (0 == token.Icmp("drop_radius")) {
                    this.drop_radius = parser.ParseInt();
                } else if (0 == token.Icmp("drop_delay")) {
                    this.drop_delay = SEC2MS(parser.ParseFloat());
                } else if (0 == token.Icmp("shader")) {
                    parser.ReadToken(token);
                    this.shader = declManager.FindMaterial(token);
                } else if (0 == token.Icmp("seed")) {
                    this.seed = parser.ParseInt();
                } else if (0 == token.Icmp("update_rate")) {
                    rate = parser.ParseFloat();
                    if ((rate <= 0.0f) || (rate > 60.0f)) {
                        parser.Warning("Invalid update_rate.  Must be between 0 and 60.  Using default model.");
                        MakeDefaultModel();
                        return;
                    }
                    this.update_tics = (int) (1000 / rate);
                } else {
                    parser.Warning("Unknown parameter '%s'.  Using default model.", token);
                    MakeDefaultModel();
                    return;
                }
            }

            this.scale_x = size_x / (this.verts_x - 1);
            this.scale_y = size_y / (this.verts_y - 1);

            this.pages.SetNum(2 * this.verts_x * this.verts_y);
            this.page1 = this.pages.Ptr();
            this.page2 = Arrays.copyOfRange(this.page1, this.verts_x * this.verts_y, this.page1.length);

            this.verts.SetNum(this.verts_x * this.verts_y);
            for (i = 0, y = 0; y < this.verts_y; y++) {
                for (x = 0; x < this.verts_x; x++, i++) {
                    this.page1[ i] = 0.0f;
                    this.page2[ i] = 0.0f;
                    this.verts.oGet(i).Clear();
                    this.verts.oGet(i).xyz.Set(x * this.scale_x, y * this.scale_y, 0.0f);
                    this.verts.oGet(i).st.Set((float) x / (float) (this.verts_x - 1), (float) -y / (float) (this.verts_y - 1));
                }
            }

            tris.SetNum((this.verts_x - 1) * (this.verts_y - 1) * 6);
            for (i = 0, y = 0; y < (this.verts_y - 1); y++) {
                for (x = 1; x < this.verts_x; x++, i += 6) {
                    tris.oSet(i + 0, (y * this.verts_x) + x);
                    tris.oSet(i + 1, ((y * this.verts_x) + x) - 1);
                    tris.oSet(i + 2, (((y + 1) * this.verts_x) + x) - 1);

                    tris.oSet(i + 3, (((y + 1) * this.verts_x) + x) - 1);
                    tris.oSet(i + 4, ((y + 1) * this.verts_x) + x);
                    tris.oSet(i + 0, (y * this.verts_x) + x);
                }
            }

            // build the information that will be common to all animations of this mesh:
            // sil edge connectivity and normal / tangent generation information
            this.deformInfo = R_BuildDeformInfo(this.verts.Num(), this.verts.Ptr(), tris.Num(), tris, true);

            this.bounds.Clear();
            this.bounds.AddPoint(new idVec3(0.0f, 0.0f, this.drop_height * -10.0f));
            this.bounds.AddPoint(new idVec3((this.verts_x - 1) * this.scale_x, (this.verts_y - 1) * this.scale_y, this.drop_height * 10.0f));

            // set the timestamp for reloadmodels
            fileSystem.ReadFile(this.name, null, this.timeStamp);

            Reset();
        }

        @Override
        public dynamicModel_t IsDynamicModel() {
            return DM_CONTINUOUS;
        }

        @Override
        public idRenderModel InstantiateDynamicModel(renderEntity_s ent, viewDef_s view, idRenderModel cachedModel) {
            idRenderModelStatic staticModel;
            int frames;
            int t;
            float lerp;

            if (cachedModel != null) {
//		delete cachedModel;
                cachedModel = null;
            }

            if (NOT(this.deformInfo)) {
                return null;
            }

            if (NOT(view)) {
                t = 0;
            } else {
                t = view.renderView.time;
            }

            // update the liquid model
            frames = (t - this.time) / this.update_tics;
            if (frames > LIQUID_MAX_SKIP_FRAMES) {
                // don't let time accumalate when skipping frames
                this.time += this.update_tics * (frames - LIQUID_MAX_SKIP_FRAMES);

                frames = LIQUID_MAX_SKIP_FRAMES;
            }

            while (frames > 0) {
                Update();
                frames--;
            }

            // create the surface
            lerp = (float) (t - this.time) / (float) this.update_tics;
            final modelSurface_s surf = GenerateSurface(lerp);

            staticModel = new idRenderModelStatic();
            staticModel.AddSurface(surf);
            staticModel.bounds = new idBounds(surf.geometry.bounds);

            return staticModel;
        }

        @Override
        public idBounds Bounds(renderEntity_s ent) {
            // FIXME: need to do this better
            return this.bounds;
        }

        @Override
        public void Reset() {
            int i, x, y;

            if (this.pages.Num() < (2 * this.verts_x * this.verts_y)) {
                return;
            }

            this.nextDropTime = 0;
            this.time = 0;
            this.random.SetSeed(this.seed);

            this.page1 = this.pages.Ptr();
            this.page2 = Arrays.copyOfRange(this.page1, this.verts_x * this.verts_y, this.page1.length);

            for (i = 0, y = 0; y < this.verts_y; y++) {
                for (x = 0; x < this.verts_x; x++, i++) {
                    this.page1[ i] = 0.0f;
                    this.page2[ i] = 0.0f;
                    this.verts.oGet(i).xyz.z = 0.0f;
                }
            }
        }

        public void IntersectBounds(final idBounds bounds, float displacement) {
            int cx, cy;
            int left, top, right, bottom;
            float up, down;
            float pos;

            left = (int) (bounds.oGet(0).x / this.scale_x);
            right = (int) (bounds.oGet(1).x / this.scale_x);
            top = (int) (bounds.oGet(0).y / this.scale_y);
            bottom = (int) (bounds.oGet(1).y / this.scale_y);
            down = bounds.oGet(0).z;
            up = bounds.oGet(1).z;

            if ((right < 1) || (left >= this.verts_x) || (bottom < 1) || (top >= this.verts_x)) {
                return;
            }

            // Perform edge clipping...
            if (left < 1) {
                left = 1;
            }
            if (right >= this.verts_x) {
                right = this.verts_x - 1;
            }
            if (top < 1) {
                top = 1;
            }
            if (bottom >= this.verts_y) {
                bottom = this.verts_y - 1;
            }

            for (cy = top; cy < bottom; cy++) {
                for (cx = left; cx < right; cx++) {
                    pos = this.page1[ (this.verts_x * cy) + cx];
                    if (pos > down) {//&& ( *pos < up ) ) {
                        this.page1[ (this.verts_x * cy) + cx] = down;
                    }
                }
            }
        }

        private modelSurface_s GenerateSurface(float lerp) {
            srfTriangles_s tri;
            int i, base;
            idDrawVert vert;
            final modelSurface_s surf = new modelSurface_s();
            float inv_lerp;

            inv_lerp = 1.0f - lerp;
            vert = this.verts.oGet(0);
            for (i = 0; i < this.verts.Num(); vert = this.verts.oGet(++i)) {
                vert.xyz.z = (this.page1[ i] * lerp) + (this.page2[ i] * inv_lerp);
            }

            tr.pc.c_deformedSurfaces++;
            tr.pc.c_deformedVerts += this.deformInfo.numOutputVerts;
            tr.pc.c_deformedIndexes += this.deformInfo.getIndexes().getNumValues();

            tri = R_AllocStaticTriSurf();

            // note that some of the data is references, and should not be freed
            tri.deformedSurface = true;

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
            R_AllocStaticTriSurfVerts(tri, tri.numVerts);
            SIMDProcessor.Memcpy(tri.verts, this.verts.Ptr(), this.deformInfo.numSourceVerts);

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

            surf.geometry = tri;
            surf.shader = this.shader;

            return surf;
        }

        private void WaterDrop(int x, int y, Float[] page) {
            int cx, cy;
            int left, top, right, bottom;
            int square;
            final int radsquare = this.drop_radius * this.drop_radius;
            final float invlength = 1.0f / radsquare;
            float dist;

            if (x < 0) {
                x = 1 + this.drop_radius + this.random.RandomInt(this.verts_x - (2 * this.drop_radius) - 1);
            }
            if (y < 0) {
                y = 1 + this.drop_radius + this.random.RandomInt(this.verts_y - (2 * this.drop_radius) - 1);
            }

            left = -this.drop_radius;
            right = this.drop_radius;
            top = -this.drop_radius;
            bottom = this.drop_radius;

            // Perform edge clipping...
            if ((x - this.drop_radius) < 1) {
                left -= (x - this.drop_radius - 1);
            }
            if ((y - this.drop_radius) < 1) {
                top -= (y - this.drop_radius - 1);
            }
            if ((x + this.drop_radius) > (this.verts_x - 1)) {
                right -= (((x + this.drop_radius) - this.verts_x) + 1);
            }
            if ((y + this.drop_radius) > (this.verts_y - 1)) {
                bottom -= (((y + this.drop_radius) - this.verts_y) + 1);
            }

            for (cy = top; cy < bottom; cy++) {
                for (cx = left; cx < right; cx++) {
                    square = (cy * cy) + (cx * cx);
                    if (square < radsquare) {
                        dist = idMath.Sqrt(square * invlength);
                        page[(this.verts_x * (cy + y)) + cx + x] += idMath.Cos16(dist * idMath.PI * 0.5f) * this.drop_height;
                    }
                }
            }
        }

        private void Update() {
            int x, y;
            int p2;
            int p1;
            float value;

            this.time += this.update_tics;

            idSwap(this.page1, this.page2);

            if (this.time > this.nextDropTime) {
                WaterDrop(-1, -1, this.page2);
                this.nextDropTime = (int) (this.time + this.drop_delay);
            } else if (this.time < (this.nextDropTime - this.drop_delay)) {
                this.nextDropTime = (int) (this.time + this.drop_delay);
            }

//            p1 = page1;
//            p2 = page2;
            p1 = p2 = 0;
            switch (this.liquid_type) {
                case 0:
                    for (y = 1; y < (this.verts_y - 1); y++) {
                        p2 += this.verts_x;
                        p1 += this.verts_x;
                        for (x = 1; x < (this.verts_x - 1); x++) {
                            value
                                    = ((this.page2[p2 + x + this.verts_x]
                                    + this.page2[(p2 + x) - this.verts_x]
                                    + this.page2[p2 + x + 1]
                                    + this.page2[(p2 + x) - 1]
                                    + this.page2[(p2 + x) - this.verts_x - 1]
                                    + this.page2[((p2 + x) - this.verts_x) + 1]
                                    + this.page2[(p2 + x + this.verts_x) - 1]
                                    + this.page2[p2 + x + this.verts_x + 1]
                                    + this.page2[p2 + x]) * (2.0f / 9.0f))
                                    - this.page1[p1 + x];

                            this.page1[p1 + x] = value * this.density;
                        }
                    }
                    break;

                case 1:
                    for (y = 1; y < (this.verts_y - 1); y++) {
                        p2 += this.verts_x;
                        p1 += this.verts_x;
                        for (x = 1; x < (this.verts_x - 1); x++) {
                            value
                                    = ((this.page2[p2 + x + this.verts_x]
                                    + this.page2[(p2 + x) - this.verts_x]
                                    + this.page2[p2 + x + 1]
                                    + this.page2[(p2 + x) - 1]
                                    + this.page2[(p2 + x) - this.verts_x - 1]
                                    + this.page2[((p2 + x) - this.verts_x) + 1]
                                    + this.page2[(p2 + x + this.verts_x) - 1]
                                    + this.page2[p2 + x + this.verts_x + 1]) * 0.25f)
                                    - this.page1[p1 + x];

                            this.page1[p1 + x] = value * this.density;
                        }
                    }
                    break;

                case 2:
                    for (y = 1; y < (this.verts_y - 1); y++) {
                        p2 += this.verts_x;
                        p1 += this.verts_x;
                        for (x = 1; x < (this.verts_x - 1); x++) {
                            value
                                    = (this.page2[p2 + x + this.verts_x]
                                    + this.page2[(p2 + x) - this.verts_x]
                                    + this.page2[p2 + x + 1]
                                    + this.page2[(p2 + x) - 1]
                                    + this.page2[(p2 + x) - this.verts_x - 1]
                                    + this.page2[((p2 + x) - this.verts_x) + 1]
                                    + this.page2[(p2 + x + this.verts_x) - 1]
                                    + this.page2[p2 + x + this.verts_x + 1]
                                    + this.page2[p2 + x]) * (1.0f / 9.0f);

                            this.page1[p1 + x] = value * this.density;
                        }
                    }
                    break;
            }
        }
    }
}
