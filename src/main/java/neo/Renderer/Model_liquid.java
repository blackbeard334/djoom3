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
            verts_x = 32;
            verts_y = 32;
            scale_x = 256.0f;
            scale_y = 256.0f;
            liquid_type = 0;
            density = 0.97f;
            drop_height = 4;
            drop_radius = 4;
            drop_delay = 1000;
            shader = declManager.FindMaterial((String) null);
            update_tics = 33;  // ~30 hz
            time = 0;
            seed = 0;

            random.SetSeed(0);
        }

        @Override
        public void InitFromFile(String fileName) throws idException {
            int i, x, y;
            final idToken token = new idToken();
            final idParser parser = new idParser(LEXFL_ALLOWPATHNAMES | LEXFL_NOSTRINGESCAPECHARS);
            final idList<Integer> tris = new idList<>();
            float size_x, size_y;
            float rate;

            name = new idStr(fileName);

            if (!parser.LoadFile(fileName)) {
                MakeDefaultModel();
                return;
            }

            size_x = scale_x * verts_x;
            size_y = scale_y * verts_y;

            while (parser.ReadToken(token)) {
                if (0 == token.Icmp("seed")) {
                    seed = parser.ParseInt();
                } else if (0 == token.Icmp("size_x")) {
                    size_x = parser.ParseFloat();
                } else if (0 == token.Icmp("size_y")) {
                    size_y = parser.ParseFloat();
                } else if (0 == token.Icmp("verts_x")) {
                    verts_x = (int) parser.ParseFloat();
                    if (verts_x < 2) {
                        parser.Warning("Invalid # of verts.  Using default model.");
                        MakeDefaultModel();
                        return;
                    }
                } else if (0 == token.Icmp("verts_y")) {
                    verts_y = (int) parser.ParseFloat();
                    if (verts_y < 2) {
                        parser.Warning("Invalid # of verts.  Using default model.");
                        MakeDefaultModel();
                        return;
                    }
                } else if (0 == token.Icmp("liquid_type")) {
                    liquid_type = parser.ParseInt() - 1;
                    if ((liquid_type < 0) || (liquid_type >= LIQUID_MAX_TYPES)) {
                        parser.Warning("Invalid liquid_type.  Using default model.");
                        MakeDefaultModel();
                        return;
                    }
                } else if (0 == token.Icmp("density")) {
                    density = parser.ParseFloat();
                } else if (0 == token.Icmp("drop_height")) {
                    drop_height = parser.ParseFloat();
                } else if (0 == token.Icmp("drop_radius")) {
                    drop_radius = parser.ParseInt();
                } else if (0 == token.Icmp("drop_delay")) {
                    drop_delay = (float) SEC2MS(parser.ParseFloat());
                } else if (0 == token.Icmp("shader")) {
                    parser.ReadToken(token);
                    shader = declManager.FindMaterial(token);
                } else if (0 == token.Icmp("seed")) {
                    seed = parser.ParseInt();
                } else if (0 == token.Icmp("update_rate")) {
                    rate = parser.ParseFloat();
                    if ((rate <= 0.0f) || (rate > 60.0f)) {
                        parser.Warning("Invalid update_rate.  Must be between 0 and 60.  Using default model.");
                        MakeDefaultModel();
                        return;
                    }
                    update_tics = (int) (1000 / rate);
                } else {
                    parser.Warning("Unknown parameter '%s'.  Using default model.", token);
                    MakeDefaultModel();
                    return;
                }
            }

            scale_x = size_x / (verts_x - 1);
            scale_y = size_y / (verts_y - 1);

            pages.SetNum(2 * verts_x * verts_y);
            page1 = pages.Ptr();
            page2 = Arrays.copyOfRange(page1, verts_x * verts_y, page1.length);

            verts.SetNum(verts_x * verts_y);
            for (i = 0, y = 0; y < verts_y; y++) {
                for (x = 0; x < verts_x; x++, i++) {
                    page1[ i] = 0.0f;
                    page2[ i] = 0.0f;
                    verts.oGet(i).Clear();
                    verts.oGet(i).xyz.Set(x * scale_x, y * scale_y, 0.0f);
                    verts.oGet(i).st.Set((float) x / (float) (verts_x - 1), (float) -y / (float) (verts_y - 1));
                }
            }

            tris.SetNum((verts_x - 1) * (verts_y - 1) * 6);
            for (i = 0, y = 0; y < verts_y - 1; y++) {
                for (x = 1; x < verts_x; x++, i += 6) {
                    tris.oSet(i + 0, y * verts_x + x);
                    tris.oSet(i + 1, y * verts_x + x - 1);
                    tris.oSet(i + 2, (y + 1) * verts_x + x - 1);

                    tris.oSet(i + 3, (y + 1) * verts_x + x - 1);
                    tris.oSet(i + 4, (y + 1) * verts_x + x);
                    tris.oSet(i + 0, y * verts_x + x);
                }
            }

            // build the information that will be common to all animations of this mesh:
            // sil edge connectivity and normal / tangent generation information
            deformInfo = R_BuildDeformInfo(verts.Num(), verts.Ptr(), tris.Num(), tris, true);

            bounds.Clear();
            bounds.AddPoint(new idVec3(0.0f, 0.0f, drop_height * -10.0f));
            bounds.AddPoint(new idVec3((verts_x - 1) * scale_x, (verts_y - 1) * scale_y, drop_height * 10.0f));

            // set the timestamp for reloadmodels
            fileSystem.ReadFile(name, null, timeStamp);

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

            if (NOT(deformInfo)) {
                return null;
            }

            if (NOT(view)) {
                t = 0;
            } else {
                t = view.renderView.time;
            }

            // update the liquid model
            frames = (t - time) / update_tics;
            if (frames > LIQUID_MAX_SKIP_FRAMES) {
                // don't let time accumalate when skipping frames
                time += update_tics * (frames - LIQUID_MAX_SKIP_FRAMES);

                frames = LIQUID_MAX_SKIP_FRAMES;
            }

            while (frames > 0) {
                Update();
                frames--;
            }

            // create the surface
            lerp = (float) (t - time) / (float) update_tics;
            modelSurface_s surf = GenerateSurface(lerp);

            staticModel = new idRenderModelStatic();
            staticModel.AddSurface(surf);
            staticModel.bounds = new idBounds(surf.geometry.bounds);

            return staticModel;
        }

        @Override
        public idBounds Bounds(renderEntity_s ent) {
            // FIXME: need to do this better
            return bounds;
        }

        @Override
        public void Reset() {
            int i, x, y;

            if (pages.Num() < 2 * verts_x * verts_y) {
                return;
            }

            nextDropTime = 0;
            time = 0;
            random.SetSeed(seed);

            page1 = pages.Ptr();
            page2 = Arrays.copyOfRange(page1, verts_x * verts_y, page1.length);

            for (i = 0, y = 0; y < verts_y; y++) {
                for (x = 0; x < verts_x; x++, i++) {
                    page1[ i] = 0.0f;
                    page2[ i] = 0.0f;
                    verts.oGet(i).xyz.z = 0.0f;
                }
            }
        }

        public void IntersectBounds(final idBounds bounds, float displacement) {
            int cx, cy;
            int left, top, right, bottom;
            float up, down;
            float pos;

            left = (int) (bounds.oGet(0).x / scale_x);
            right = (int) (bounds.oGet(1).x / scale_x);
            top = (int) (bounds.oGet(0).y / scale_y);
            bottom = (int) (bounds.oGet(1).y / scale_y);
            down = bounds.oGet(0).z;
            up = bounds.oGet(1).z;

            if ((right < 1) || (left >= verts_x) || (bottom < 1) || (top >= verts_x)) {
                return;
            }

            // Perform edge clipping...
            if (left < 1) {
                left = 1;
            }
            if (right >= verts_x) {
                right = verts_x - 1;
            }
            if (top < 1) {
                top = 1;
            }
            if (bottom >= verts_y) {
                bottom = verts_y - 1;
            }

            for (cy = top; cy < bottom; cy++) {
                for (cx = left; cx < right; cx++) {
                    pos = page1[ verts_x * cy + cx];
                    if (pos > down) {//&& ( *pos < up ) ) {
                        page1[ verts_x * cy + cx] = down;
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
            vert = verts.oGet(0);
            for (i = 0; i < verts.Num(); vert = verts.oGet(++i)) {
                vert.xyz.z = page1[ i] * lerp + page2[ i] * inv_lerp;
            }

            tr.pc.c_deformedSurfaces++;
            tr.pc.c_deformedVerts += deformInfo.numOutputVerts;
            tr.pc.c_deformedIndexes += deformInfo.getIndexes().getNumValues();

            tri = R_AllocStaticTriSurf();

            // note that some of the data is references, and should not be freed
            tri.deformedSurface = true;

            tri.getIndexes().setNumValues(this.deformInfo.getIndexes().getNumValues());
            tri.getIndexes().setValues(this.deformInfo.getIndexes().getValues());
            tri.silIndexes = deformInfo.silIndexes;
            tri.numMirroredVerts = deformInfo.numMirroredVerts;
            tri.mirroredVerts = deformInfo.mirroredVerts;
            tri.numDupVerts = deformInfo.numDupVerts;
            tri.dupVerts = deformInfo.dupVerts;
            tri.numSilEdges = deformInfo.numSilEdges;
            tri.silEdges = deformInfo.silEdges;
            tri.dominantTris = deformInfo.dominantTris;

            tri.numVerts = deformInfo.numOutputVerts;
            R_AllocStaticTriSurfVerts(tri, tri.numVerts);
            SIMDProcessor.Memcpy(tri.verts, verts.Ptr(), deformInfo.numSourceVerts);

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

            surf.geometry = tri;
            surf.shader = shader;

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
                x = 1 + drop_radius + random.RandomInt(verts_x - 2 * drop_radius - 1);
            }
            if (y < 0) {
                y = 1 + drop_radius + random.RandomInt(verts_y - 2 * drop_radius - 1);
            }

            left = -drop_radius;
            right = drop_radius;
            top = -drop_radius;
            bottom = drop_radius;

            // Perform edge clipping...
            if (x - drop_radius < 1) {
                left -= (x - drop_radius - 1);
            }
            if (y - drop_radius < 1) {
                top -= (y - drop_radius - 1);
            }
            if (x + drop_radius > verts_x - 1) {
                right -= (x + drop_radius - verts_x + 1);
            }
            if (y + drop_radius > verts_y - 1) {
                bottom -= (y + drop_radius - verts_y + 1);
            }

            for (cy = top; cy < bottom; cy++) {
                for (cx = left; cx < right; cx++) {
                    square = cy * cy + cx * cx;
                    if (square < radsquare) {
                        dist = idMath.Sqrt((float) square * invlength);
                        page[verts_x * (cy + y) + cx + x] += idMath.Cos16(dist * idMath.PI * 0.5f) * drop_height;
                    }
                }
            }
        }

        private void Update() {
            int x, y;
            int p2;
            int p1;
            float value;

            time += update_tics;

            idSwap(page1, page2);

            if (time > nextDropTime) {
                WaterDrop(-1, -1, page2);
                nextDropTime = (int) (time + drop_delay);
            } else if (time < nextDropTime - drop_delay) {
                nextDropTime = (int) (time + drop_delay);
            }

//            p1 = page1;
//            p2 = page2;
            p1 = p2 = 0;
            switch (liquid_type) {
                case 0:
                    for (y = 1; y < verts_y - 1; y++) {
                        p2 += verts_x;
                        p1 += verts_x;
                        for (x = 1; x < verts_x - 1; x++) {
                            value
                                    = (page2[p2 + x + verts_x]
                                    + page2[p2 + x - verts_x]
                                    + page2[p2 + x + 1]
                                    + page2[p2 + x - 1]
                                    + page2[p2 + x - verts_x - 1]
                                    + page2[p2 + x - verts_x + 1]
                                    + page2[p2 + x + verts_x - 1]
                                    + page2[p2 + x + verts_x + 1]
                                    + page2[p2 + x]) * (2.0f / 9.0f)
                                    - page1[p1 + x];

                            page1[p1 + x] = value * density;
                        }
                    }
                    break;

                case 1:
                    for (y = 1; y < verts_y - 1; y++) {
                        p2 += verts_x;
                        p1 += verts_x;
                        for (x = 1; x < verts_x - 1; x++) {
                            value
                                    = (page2[p2 + x + verts_x]
                                    + page2[p2 + x - verts_x]
                                    + page2[p2 + x + 1]
                                    + page2[p2 + x - 1]
                                    + page2[p2 + x - verts_x - 1]
                                    + page2[p2 + x - verts_x + 1]
                                    + page2[p2 + x + verts_x - 1]
                                    + page2[p2 + x + verts_x + 1]) * 0.25f
                                    - page1[p1 + x];

                            page1[p1 + x] = value * density;
                        }
                    }
                    break;

                case 2:
                    for (y = 1; y < verts_y - 1; y++) {
                        p2 += verts_x;
                        p1 += verts_x;
                        for (x = 1; x < verts_x - 1; x++) {
                            value
                                    = (page2[p2 + x + verts_x]
                                    + page2[p2 + x - verts_x]
                                    + page2[p2 + x + 1]
                                    + page2[p2 + x - 1]
                                    + page2[p2 + x - verts_x - 1]
                                    + page2[p2 + x - verts_x + 1]
                                    + page2[p2 + x + verts_x - 1]
                                    + page2[p2 + x + verts_x + 1]
                                    + page2[p2 + x]) * (1.0f / 9.0f);

                            page1[p1 + x] = value * density;
                        }
                    }
                    break;
            }
        }
    };
}
