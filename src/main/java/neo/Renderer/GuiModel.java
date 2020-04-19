package neo.Renderer;

import static neo.Renderer.RenderSystem.R_AddDrawViewCmd;
import static neo.Renderer.RenderSystem.SCREEN_HEIGHT;
import static neo.Renderer.RenderSystem.SCREEN_WIDTH;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.tr_light.R_AddDrawSurf;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_main.myGlMultMatrix;
import static neo.TempDump.NOT;
import static neo.framework.DeclManager.declManager;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import neo.Renderer.Material.idMaterial;
import neo.Renderer.Model.srfTriangles_s;
import neo.Renderer.RenderWorld.renderEntity_s;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.viewDef_s;
import neo.Renderer.tr_local.viewEntity_s;
import neo.framework.DemoFile.idDemoFile;
import neo.idlib.containers.List.idList;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idFixedWinding;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec5;
import neo.open.MatrixUtil;
import neo.open.Nio;

/**
 *
 */
public class GuiModel {

    static class guiModelSurface_t {

        idMaterial material;
        final float[] color = new float[4];
        int firstVert;
        int numVerts;
        int firstIndex;
        int numIndexes;
    }

    public static class idGuiModel {

        private guiModelSurface_t surf;
        //
        private final idList<guiModelSurface_t> surfaces;
        private final idList</*glIndex_t*/Integer> indexes;
        private final idList<idDrawVert> verts;
        //
        //

        public idGuiModel() {
            this.surfaces = new idList<>();
            this.indexes = new idList<Integer>(1000);//.SetGranularity(1000);
            this.verts = new idList<idDrawVert>(1000);//.SetGranularity(1000);
        }


        /*
         ================
         idGuiModel::Clear

         Begins collecting draw commands into surfaces
         ================
         */
        public void Clear() {
            this.surfaces.SetNum(0, false);
            this.indexes.SetNum(0, false);
            this.verts.SetNum(0, false);
            AdvanceSurf();
//            if (bla) {
            clear++;
//            }
        }

        public void WriteToDemo(idDemoFile demo) {
            int i, j;

            i = this.verts.Num();
            demo.WriteInt(i);
            ByteBuffer color;
            for (j = 0; j < i; j++) {
                demo.WriteVec3(this.verts.oGet(j).xyz);
                demo.WriteVec2(this.verts.oGet(j).st);
                demo.WriteVec3(this.verts.oGet(j).normal);
                demo.WriteVec3(this.verts.oGet(j).tangents[0]);
                demo.WriteVec3(this.verts.oGet(j).tangents[1]);
                
                color = this.verts.oGet(j).getColor();
                for (int l = 0; l < 4; l++) {
                    demo.WriteUnsignedChar((char) color.get(0));
				}
            }

            i = this.indexes.Num();
            demo.WriteInt(i);
            for (j = 0; j < i; j++) {
                demo.WriteInt(this.indexes.oGet(j));
            }

            i = this.surfaces.Num();
            demo.WriteInt(i);
            for (j = 0; j < i; j++) {
                final guiModelSurface_t surf = this.surfaces.oGet(j);

//                demo.WriteInt((int) surf.material);
                demo.Write(surf.material);
                demo.WriteFloat(surf.color[0]);
                demo.WriteFloat(surf.color[1]);
                demo.WriteFloat(surf.color[2]);
                demo.WriteFloat(surf.color[3]);
                demo.WriteInt(surf.firstVert);
                demo.WriteInt(surf.numVerts);
                demo.WriteInt(surf.firstIndex);
                demo.WriteInt(surf.numIndexes);
                demo.WriteHashString(surf.material.GetName());
            }
        }

        public void ReadFromDemo(idDemoFile demo) {
            final int[] i = new int[1];
            int j;
            final int[] k = new int[1];
            final char[] color = {0};

            i[0] = this.verts.Num();
            demo.ReadInt(i);
            this.verts.SetNum(i[0], false);
            ByteBuffer bcolor;
            for (j = 0; j < i[0]; j++) {
                demo.ReadVec3(this.verts.oGet(j).xyz);
                demo.ReadVec2(this.verts.oGet(j).st);
                demo.ReadVec3(this.verts.oGet(j).normal);
                demo.ReadVec3(this.verts.oGet(j).tangents[0]);
                demo.ReadVec3(this.verts.oGet(j).tangents[1]);
                
                bcolor = this.verts.oGet(j).getColor();
                for (int l = 0; l < 4; l++) {
                    demo.ReadUnsignedChar(color);
                    bcolor.put(l, (byte) color[0]);
				}
            }

            i[0] = this.indexes.Num();
            demo.ReadInt(i);
            this.indexes.SetNum(i[0], false);
            for (j = 0; j < i[0]; j++) {
                demo.ReadInt(k);
                this.indexes.oSet(j, k[0]);
            }

            i[0] = this.surfaces.Num();
            demo.ReadInt(i);
            this.surfaces.SetNum(i[0], false);
            for (j = 0; j < i[0]; j++) {
                final guiModelSurface_t surf = this.surfaces.oGet(j);

//                demo.ReadInt((int) surf.material);
                demo.Read(surf.material);//TODO:serialize?
                surf.color[0] = demo.ReadFloat();
                surf.color[1] = demo.ReadFloat();
                surf.color[2] = demo.ReadFloat();
                surf.color[3] = demo.ReadFloat();
                surf.firstVert = demo.ReadInt();
                surf.numVerts = demo.ReadInt();
                surf.firstIndex = demo.ReadInt();
                surf.numIndexes = demo.ReadInt();
                surf.material = declManager.FindMaterial(demo.ReadHashString());
            }
        }

        public void EmitToCurrentView(float[] modelMatrix/*[16]*/, boolean depthHack) {
            final FloatBuffer modelViewMatrix = Nio.newFloatBuffer(16);

            myGlMultMatrix(modelMatrix, tr.viewDef.worldSpace.getModelViewMatrix(),
                    modelViewMatrix);

            for (int i = 0; i < this.surfaces.Num(); i++) {
                EmitSurface(this.surfaces.oGet(i), modelMatrix, modelViewMatrix, depthHack);
            }
        }

        /*
         ================
         idGuiModel::EmitFullScreen

         Creates a view that covers the screen and emit the surfaces
         ================
         */
        public void EmitFullScreen() {
            viewDef_s viewDef;

            if (this.surfaces.oGet(0).numVerts == 0) {
                return;
            }

            viewDef = new viewDef_s();//R_ClearedFrameAlloc(sizeof(viewDef));

            // for gui editor
            if ((null == tr.viewDef) || !tr.viewDef.isEditor) {
                viewDef.renderView.x = 0;
                viewDef.renderView.y = 0;
                viewDef.renderView.width = SCREEN_WIDTH;
                viewDef.renderView.height = SCREEN_HEIGHT;

                tr.RenderViewToViewport(viewDef.renderView, viewDef.viewport);

                viewDef.scissor.x1 = 0;
                viewDef.scissor.y1 = 0;
                viewDef.scissor.x2 = viewDef.viewport.x2 - viewDef.viewport.x1;
                viewDef.scissor.y2 = viewDef.viewport.y2 - viewDef.viewport.y1;
            } else {
                viewDef.renderView.x = tr.viewDef.renderView.x;
                viewDef.renderView.y = tr.viewDef.renderView.y;
                viewDef.renderView.width = tr.viewDef.renderView.width;
                viewDef.renderView.height = tr.viewDef.renderView.height;

                viewDef.viewport.x1 = tr.viewDef.renderView.x;
                viewDef.viewport.x2 = tr.viewDef.renderView.x + tr.viewDef.renderView.width;
                viewDef.viewport.y1 = tr.viewDef.renderView.y;
                viewDef.viewport.y2 = tr.viewDef.renderView.y + tr.viewDef.renderView.height;

                viewDef.scissor.x1 = tr.viewDef.scissor.x1;
                viewDef.scissor.y1 = tr.viewDef.scissor.y1;
                viewDef.scissor.x2 = tr.viewDef.scissor.x2;
                viewDef.scissor.y2 = tr.viewDef.scissor.y2;
            }

            viewDef.floatTime = tr.frameShaderTime;

            // TODO: qglOrtho( 0, 640, 480, 0, 0, 1 );		// always assume 640x480 virtual coordinates
            MatrixUtil.emitFullScreenProjection(viewDef.getProjectionMatrix());

            MatrixUtil.emitFullScreenModelView(viewDef.worldSpace.getModelViewMatrix());

            viewDef.maxDrawSurfs = this.surfaces.Num();
            viewDef.drawSurfs = new drawSurf_s[viewDef.maxDrawSurfs];///*(drawSurf_t **)*/ R_FrameAlloc(viewDef.maxDrawSurfs * sizeof(viewDef.drawSurfs[0]));
            viewDef.numDrawSurfs = 0;

            final viewDef_s oldViewDef = tr.viewDef;
            tr.viewDef = viewDef;

            // add the surfaces to this view
            for (int i = 0; i < this.surfaces.Num(); i++) {
                if (i == 33) {
                    this.surfaces.oGet(i).material.DBG_BALLS = i;
                }
                EmitSurface(this.surfaces.oGet(i), viewDef.worldSpace.modelMatrix, viewDef.worldSpace.getModelViewMatrix(), false);
            }

            tr.viewDef = oldViewDef;

            // add the command to draw this view
            R_AddDrawViewCmd(viewDef);
        }

        // these calls are forwarded from the renderer
        public void SetColor(float r, float g, float b, float a) {
            setColorTotal++;
            if (!glConfig.isInitialized) {
                return;
            }
            if ((r == this.surf.color[0]) && (g == this.surf.color[1])
                    && (b == this.surf.color[2]) && (a == this.surf.color[3])) {
                return;	// no change
            }

            if (this.surf.numVerts != 0) {
//                if (bla) {
//                }
//                TempDump.printCallStack(setColorTotal + "");
//                System.out.printf("%d\n", setColorTotal);
                AdvanceSurf();
                setColor++;
            }

            // change the parms
            this.surf.color[0] = r;
            this.surf.color[1] = g;
            this.surf.color[2] = b;
            this.surf.color[3] = a;
        }
        public static boolean bla;
        private static int clear = 0, setColor = 0, setColorTotal = 0, drawStretchPic = 0, bla4 = 0;

        public void DrawStretchPic(final idDrawVert[] dVerts, final /*glIndex_t*/ int[] dIndexes, int vertCount, final int indexCount, final idMaterial hShader,
                final boolean clip /*= true*/, final float min_x /*= 0.0f*/, final float min_y /*= 0.0f*/, final float max_x /*= 640.0f*/, final float max_y /*= 480.0f*/) {
//            TempDump.printCallStack(bla4+"");
            bla4++;
            if (!glConfig.isInitialized) {
                return;
            }
            if (!((dVerts != null) && (dIndexes != null) && (vertCount != 0) && (indexCount != 0) && (hShader != null))) {
                return;
            }

            // break the current surface if we are changing to a new material
//                    if (bla) {
//                    }
//            System.out.printf("%s\n%s\n\n", hShader, surf.material);
            if (hShader != this.surf.material) {
                if (this.surf.numVerts != 0) {
                    AdvanceSurf();
//                    if (bla) {
//                    System.out.printf("~~ %d %d\n", Window.idWindow.bla1, Window.idWindow.bla2);
//                    }
                }
                hShader.EnsureNotPurged();	// in case it was a gui item started before a level change
                this.surf.material = hShader;
//                TempDump.printCallStack(bla4 + "");
            }

            // add the verts and indexes to the current surface
            if (clip) {
                int i, j;

                // FIXME:	this is grim stuff, and should be rewritten if we have any significant
                //			number of guis asking for clipping
                final idFixedWinding w = new idFixedWinding();
                for (i = 0; i < indexCount; i += 3) {
                    w.Clear();
                    w.AddPoint(new idVec5(dVerts[dIndexes[i + 0]].xyz.x, dVerts[dIndexes[i + 0]].xyz.y, dVerts[dIndexes[i + 0]].xyz.z, dVerts[dIndexes[i + 0]].st.x, dVerts[dIndexes[i + 0]].st.y));
                    w.AddPoint(new idVec5(dVerts[dIndexes[i + 1]].xyz.x, dVerts[dIndexes[i + 1]].xyz.y, dVerts[dIndexes[i + 1]].xyz.z, dVerts[dIndexes[i + 1]].st.x, dVerts[dIndexes[i + 1]].st.y));
                    w.AddPoint(new idVec5(dVerts[dIndexes[i + 2]].xyz.x, dVerts[dIndexes[i + 2]].xyz.y, dVerts[dIndexes[i + 2]].xyz.z, dVerts[dIndexes[i + 2]].st.x, dVerts[dIndexes[i + 2]].st.y));

                    for (j = 0; j < 3; j++) {
                        if ((w.oGet(j).x < min_x) || (w.oGet(j).x > max_x)
                                || (w.oGet(j).y < min_y) || (w.oGet(j).y > max_y)) {
                            break;
                        }
                    }
                    if (j < 3) {
                        final idPlane p = new idPlane();
                        p.NormalY(p.NormalZ(0.0f));
                        p.NormalX(1.0f);
                        p.SetDist(min_x);
                        w.ClipInPlace(p);
                        p.NormalY(p.NormalZ(0.0f));
                        p.NormalX(-1.0f);
                        p.SetDist(-max_x);
                        w.ClipInPlace(p);
                        p.NormalX(p.NormalZ(0.0f));
                        p.NormalY(1.0f);
                        p.SetDist(min_y);
                        w.ClipInPlace(p);
                        p.NormalX(p.NormalZ(0.0f));
                        p.NormalY(-1.0f);
                        p.SetDist(-max_y);
                        w.ClipInPlace(p);
                    }

                    final int numVerts = this.verts.Num();
                    this.verts.SetNum(numVerts + w.GetNumPoints(), false);
                    for (j = 0; j < w.GetNumPoints(); j++) {
                        final idDrawVert dv = this.verts.oGet(numVerts + j);

                        dv.xyz.x = w.oGet(j).x;
                        dv.xyz.y = w.oGet(j).y;
                        dv.xyz.z = w.oGet(j).z;
                        dv.st.x = w.oGet(j).s;
                        dv.st.y = w.oGet(j).t;
                        dv.normal.Set(0, 0, 1);
                        dv.tangents[0].Set(1, 0, 0);
                        dv.tangents[1].Set(0, 1, 0);
                    }
                    this.surf.numVerts += w.GetNumPoints();

                    for (j = 2; j < w.GetNumPoints(); j++) {
                        this.indexes.Append(numVerts - this.surf.firstVert);
                        this.indexes.Append((numVerts + j) - 1 - this.surf.firstVert);
                        this.indexes.Append((numVerts + j) - this.surf.firstVert);
                        this.surf.numIndexes += 3;
                    }
                }

            } else {
                drawStretchPic++;
//                if (dVerts[0].xyz.x == 212) {
                    final int numVerts = this.verts.Num();
                    final int numIndexes = this.indexes.Num();

                    this.verts.AssureSize(numVerts + vertCount);
                    this.indexes.AssureSize(numIndexes + indexCount);

                    this.surf.numVerts += vertCount;
                    this.surf.numIndexes += indexCount;

                    for (int i = 0; i < indexCount; i++) {
                        this.indexes.oSet(numIndexes + i, (numVerts + dIndexes[i]) - this.surf.firstVert);
                    }

    //                memcpy( & verts[numVerts], dverts, vertCount * sizeof(verts[0]));
                    System.arraycopy(dVerts, 0, this.verts.Ptr(), numVerts, vertCount);//no need to memcpy here. dVerts has no back references. 
//                }
            }
        }

        /*
         =============
         DrawStretchPic

         x/y/w/h are in the 0,0 to 640,480 range
         =============
         */
        public void DrawStretchPic(float x, float y, float w, float h, float s1, float t1, float s2, float t2, final idMaterial hShader) {
            final idDrawVert[] verts = {
                new idDrawVert(),
                new idDrawVert(),
                new idDrawVert(),
                new idDrawVert()};
            /*glIndex_t*/ final int[] indexes = new int[6];

            if (!glConfig.isInitialized) {
                return;
            }
            if (null == hShader) {
                return;
            }

            // clip to edges, because the pic may be going into a guiShader
            // instead of full screen
            if (x < 0) {
                s1 += ((s2 - s1) * -x) / w;
                w += x;
                x = 0;
            }
            if (y < 0) {
                t1 += ((t2 - t1) * -y) / h;
                h += y;
                y = 0;
            }
            if ((x + w) > 640) {
                s2 -= ((s2 - s1) * ((x + w) - 640)) / w;
                w = 640 - x;
            }
            if ((y + h) > 480) {
                t2 -= ((t2 - t1) * ((y + h) - 480)) / h;
                h = 480 - y;
            }

            if ((w <= 0) || (h <= 0)) {
                return;		// completely clipped away
            }

            indexes[0] = 3;
            indexes[1] = 0;
            indexes[2] = 2;
            indexes[3] = 2;
            indexes[4] = 0;
            indexes[5] = 1;
            verts[0].xyz.oSet(0, x);
            verts[0].xyz.oSet(1, y);
            verts[0].xyz.oSet(2, 0);
            verts[0].st.oSet(0, s1);
            verts[0].st.oSet(1, t1);
            verts[0].normal.oSet(0, 0);
            verts[0].normal.oSet(1, 0);
            verts[0].normal.oSet(2, 1);
            verts[0].tangents[0].oSet(0, 1);
            verts[0].tangents[0].oSet(1, 0);
            verts[0].tangents[0].oSet(2, 0);
            verts[0].tangents[1].oSet(0, 0);
            verts[0].tangents[1].oSet(1, 1);
            verts[0].tangents[1].oSet(2, 0);
            verts[1].xyz.oSet(0, x + w);
            verts[1].xyz.oSet(1, y);
            verts[1].xyz.oSet(2, 0);
            verts[1].st.oSet(0, s2);
            verts[1].st.oSet(1, t1);
            verts[1].normal.oSet(0, 0);
            verts[1].normal.oSet(1, 0);
            verts[1].normal.oSet(2, 1);
            verts[1].tangents[0].oSet(0, 1);
            verts[1].tangents[0].oSet(1, 0);
            verts[1].tangents[0].oSet(2, 0);
            verts[1].tangents[1].oSet(0, 0);
            verts[1].tangents[1].oSet(1, 1);
            verts[1].tangents[1].oSet(2, 0);
            verts[2].xyz.oSet(0, x + w);
            verts[2].xyz.oSet(1, y + h);
            verts[2].xyz.oSet(2, 0);
            verts[2].st.oSet(0, s2);
            verts[2].st.oSet(1, t2);
            verts[2].normal.oSet(0, 0);
            verts[2].normal.oSet(1, 0);
            verts[2].normal.oSet(2, 1);
            verts[2].tangents[0].oSet(0, 1);
            verts[2].tangents[0].oSet(1, 0);
            verts[2].tangents[0].oSet(2, 0);
            verts[2].tangents[1].oSet(0, 0);
            verts[2].tangents[1].oSet(1, 1);
            verts[2].tangents[1].oSet(2, 0);
            verts[3].xyz.oSet(0, x);
            verts[3].xyz.oSet(1, y + h);
            verts[3].xyz.oSet(2, 0);
            verts[3].st.oSet(0, s1);
            verts[3].st.oSet(1, t2);
            verts[3].normal.oSet(0, 0);
            verts[3].normal.oSet(1, 0);
            verts[3].normal.oSet(2, 1);
            verts[3].tangents[0].oSet(0, 1);
            verts[3].tangents[0].oSet(1, 0);
            verts[3].tangents[0].oSet(2, 0);
            verts[3].tangents[1].oSet(0, 0);
            verts[3].tangents[1].oSet(1, 1);
            verts[3].tangents[1].oSet(2, 0);

            this.DrawStretchPic(verts/*[0]*/, indexes/*[0]*/, 4, 6, hShader, false, 0.0f, 0.0f, 640.0f, 480.0f);
            bla99++;
        }
        static int bla99 = 0;

        /*
         =============
         DrawStretchTri

         x/y/w/h are in the 0,0 to 640,480 range
         =============
         */
        public void DrawStretchTri(idVec2 p1, idVec2 p2, idVec2 p3, idVec2 t1, idVec2 t2, idVec2 t3, final idMaterial material) {
            final idDrawVert[] tempVerts = new idDrawVert[3];
            /*glIndex_t*/ final int[] tempIndexes = new int[3];
            final int vertCount = 3;
            final int indexCount = 3;

            if (!glConfig.isInitialized) {
                return;
            }
            if (null == material) {
                return;
            }

            tempIndexes[0] = 1;
            tempIndexes[1] = 0;
            tempIndexes[2] = 2;

            tempVerts[0].xyz.oSet(0, p1.x);
            tempVerts[0].xyz.oSet(1, p1.y);
            tempVerts[0].xyz.oSet(2, 0);
            tempVerts[0].st.oSet(0, t1.x);
            tempVerts[0].st.oSet(1, t1.y);
            tempVerts[0].normal.oSet(0, 0);
            tempVerts[0].normal.oSet(1, 0);
            tempVerts[0].normal.oSet(2, 1);
            tempVerts[0].tangents[0].oSet(0, 1);
            tempVerts[0].tangents[0].oSet(1, 0);
            tempVerts[0].tangents[0].oSet(2, 0);
            tempVerts[0].tangents[1].oSet(0, 0);
            tempVerts[0].tangents[1].oSet(1, 1);
            tempVerts[0].tangents[1].oSet(2, 0);
            tempVerts[1].xyz.oSet(0, p2.x);
            tempVerts[1].xyz.oSet(1, p2.y);
            tempVerts[1].xyz.oSet(2, 0);
            tempVerts[1].st.oSet(0, t2.x);
            tempVerts[1].st.oSet(1, t2.y);
            tempVerts[1].normal.oSet(0, 0);
            tempVerts[1].normal.oSet(1, 0);
            tempVerts[1].normal.oSet(2, 1);
            tempVerts[1].tangents[0].oSet(0, 1);
            tempVerts[1].tangents[0].oSet(1, 0);
            tempVerts[1].tangents[0].oSet(2, 0);
            tempVerts[1].tangents[1].oSet(0, 0);
            tempVerts[1].tangents[1].oSet(1, 1);
            tempVerts[1].tangents[1].oSet(2, 0);
            tempVerts[2].xyz.oSet(0, p3.x);
            tempVerts[2].xyz.oSet(1, p3.y);
            tempVerts[2].xyz.oSet(2, 0);
            tempVerts[2].st.oSet(0, t3.x);
            tempVerts[2].st.oSet(1, t3.y);
            tempVerts[2].normal.oSet(0, 0);
            tempVerts[2].normal.oSet(1, 0);
            tempVerts[2].normal.oSet(2, 1);
            tempVerts[2].tangents[0].oSet(0, 1);
            tempVerts[2].tangents[0].oSet(1, 0);
            tempVerts[2].tangents[0].oSet(2, 0);
            tempVerts[2].tangents[1].oSet(0, 0);
            tempVerts[2].tangents[1].oSet(1, 1);
            tempVerts[2].tangents[1].oSet(2, 0);

            // break the current surface if we are changing to a new material
            if (material != this.surf.material) {
                if (this.surf.numVerts != 0) {
                    AdvanceSurf();
                    if (bla) {
                        bla4++;
                    }
                }
                /*const_cast<idMaterial *>*/
                (material).EnsureNotPurged();	// in case it was a gui item started before a level change
                this.surf.material = material;
            }

            final int numVerts = this.verts.Num();
            final int numIndexes = this.indexes.Num();

            this.verts.AssureSize(numVerts + vertCount);
            this.indexes.AssureSize(numIndexes + indexCount);

            this.surf.numVerts += vertCount;
            this.surf.numIndexes += indexCount;

            for (int i = 0; i < indexCount; i++) {
                this.indexes.oSet(numIndexes + i, (numVerts + tempIndexes[i]) - this.surf.firstVert);
            }

//            memcpy(verts[numVerts], tempVerts, vertCount * sizeof(verts[0]));
            System.arraycopy(tempVerts, 0, this.verts.Ptr(), numVerts, vertCount);
        }

        //---------------------------
        private void AdvanceSurf() {
            final guiModelSurface_t s = new guiModelSurface_t();

            if (this.surfaces.Num() != 0) {
                s.color[0] = this.surf.color[0];
                s.color[1] = this.surf.color[1];
                s.color[2] = this.surf.color[2];
                s.color[3] = this.surf.color[3];
                s.material = this.surf.material;
            } else {
                s.color[0] = 1;
                s.color[1] = 1;
                s.color[2] = 1;
                s.color[3] = 1;
                s.material = tr.defaultMaterial;
            }
            s.numIndexes = 0;
            s.firstIndex = this.indexes.Num();
            s.numVerts = 0;
            s.firstVert = this.verts.Num();

            this.surfaces.Append(s);
            this.surf = this.surfaces.oGet(this.surfaces.Num() - 1);
//            TempDump.printCallStack(bla555 + "");
            final int bla0 = setColorTotal;
            final int bla1 = setColor;
            final int bla2 = clear;
            final int bla3 = drawStretchPic;
            bla555++;
        }
        static int bla555 = 0;

        /**
         * TBD delete method after float[] to FloatBuffer
         * 
         * @param surf
         * @param modelMatrix
         * @param modelViewMatrix
         * @param depthHack
         * 
         * @deprecated use private void EmitSurface(guiModelSurface_t surf, FloatBuffer modelMatrix, FloatBuffer modelViewMatrix, boolean depthHack) instead
         */
        private void EmitSurface(guiModelSurface_t surf, float[] modelMatrix/*[16]*/, FloatBuffer modelViewMatrix/*[16]*/, boolean depthHack) {
        	EmitSurface(surf, Nio.wrap(modelMatrix), modelViewMatrix, depthHack);
        }

        private void EmitSurface(guiModelSurface_t surf, FloatBuffer modelMatrix/*[16]*/, FloatBuffer modelViewMatrix/*[16]*/, boolean depthHack) {
            srfTriangles_s tri;

            if (surf.numVerts == 0) {
                return;		// nothing in the surface
            }

            // copy verts and indexes
            tri = new srfTriangles_s();///*(srfTriangles_s *)*/ R_ClearedFrameAlloc(sizeof(tri));
            tri.getIndexes().setNumValues(surf.numIndexes);
            tri.numVerts = surf.numVerts;//TODO:see if we can get rid of these single element arrays. EDIT:done.
            //tri.getIndexes().setValues(new int[tri.getIndexes().getNumValues()]);///*(glIndex_t *)*/ R_FrameAlloc(tri.numIndexes * sizeof(tri.indexes[0]));
            tri.getIndexes().createValues(tri.getIndexes().getNumValues());///*(glIndex_t *)*/ R_FrameAlloc(tri.numIndexes * sizeof(tri.indexes[0]));
//            memcpy(tri.indexes, indexes[surf.firstIndex], tri.numIndexes * sizeof(tri.indexes[0]));
            for (int s = surf.firstIndex, d = 0; d < tri.getIndexes().getNumValues(); s++, d++) {
                tri.getIndexes().getValues().put(d, this.indexes.oGet(s));
            }

            // we might be able to avoid copying these and just let them reference the list vars
            // but some things, like deforms and recursive
            // guis, need to access the verts in cpu space, not just through the vertex range
            tri.verts = new idDrawVert[tri.numVerts];///*(idDrawVert *)*/ R_FrameAlloc(tri.numVerts * sizeof(tri.verts[0]));
//            memcpy(tri.verts,  & verts[surf.firstVert], tri.numVerts * sizeof(tri.verts[0]));
            for (int s = surf.firstVert, d = 0; d < tri.numVerts; s++, d++) {
                tri.verts[d] = new idDrawVert(this.verts.oGet(s));
            }

            // move the verts to the vertex cache
            tri.ambientCache = vertexCache.AllocFrameTemp(tri.verts, tri.numVerts * idDrawVert.BYTES);

            // if we are out of vertex cache, don't create the surface
            if (NOT(tri.ambientCache)) {
                return;
            }

            renderEntity_s renderEntity;
            renderEntity = new renderEntity_s();//memset( & renderEntity, 0, sizeof(renderEntity));
//            memcpy(renderEntity.shaderParms, surf.color, sizeof(surf.color));
            renderEntity.shaderParms[0] = surf.color[0];
            renderEntity.shaderParms[1] = surf.color[1];
            renderEntity.shaderParms[2] = surf.color[2];
            renderEntity.shaderParms[3] = surf.color[3];

            final viewEntity_s guiSpace = new viewEntity_s();///*(viewEntity_t *)*/ R_ClearedFrameAlloc(sizeof( * guiSpace));
//            memcpy(guiSpace.modelMatrix, modelMatrix, sizeof(guiSpace.modelMatrix));
            //System.arraycopy(modelMatrix, 0, guiSpace.modelMatrix, 0, guiSpace.modelMatrix.length);
            Nio.arraycopy(modelMatrix, 0, guiSpace.modelMatrix, 0, guiSpace.modelMatrix.length);
//            memcpy(guiSpace.modelViewMatrix, modelViewMatrix, sizeof(guiSpace.modelViewMatrix));
            //System.arraycopy(modelViewMatrix, 0, guiSpace.getModelViewMatrix(), 0, guiSpace.getModelViewMatrix().length);
            Nio.buffercopy(modelViewMatrix, 0, guiSpace.getModelViewMatrix(), 0, guiSpace.getModelViewMatrix().limit());
            guiSpace.weaponDepthHack = depthHack;

            // add the surface, which might recursively create another gui
            R_AddDrawSurf(tri, guiSpace, renderEntity, surf.material, tr.viewDef.scissor);
        }
    }
}
