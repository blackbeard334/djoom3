package neo.Tools.Compilers.RenderBump;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.sqrt;
import static neo.Renderer.Image_files.R_WriteTGA;
import static neo.Renderer.Image_process.R_MipMap;
import static neo.Renderer.Image_process.R_VerticalFlip;
import static neo.Renderer.Material.SURF_NULLNORMAL;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurf;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfIndexes;
import static neo.Renderer.tr_trisurf.R_AllocStaticTriSurfVerts;
import static neo.Renderer.tr_trisurf.R_BoundTriSurf;
import static neo.Renderer.tr_trisurf.R_DeriveFacePlanes;
import static neo.TempDump.NOT;
import static neo.TempDump.itob;
import static neo.framework.Common.common;
import static neo.idlib.math.Vector.DotProduct;
import static neo.idlib.math.Vector.VectorSubtract;
import static neo.idlib.math.Vector.getVec3_origin;
import static neo.open.gl.QGL.qglBegin;
import static neo.open.gl.QGL.qglClear;
import static neo.open.gl.QGL.qglClearColor;
import static neo.open.gl.QGL.qglColor3f;
import static neo.open.gl.QGL.qglColor3ubv;
import static neo.open.gl.QGL.qglCullFace;
import static neo.open.gl.QGL.qglDepthFunc;
import static neo.open.gl.QGL.qglDepthMask;
import static neo.open.gl.QGL.qglDisable;
import static neo.open.gl.QGL.qglEnable;
import static neo.open.gl.QGL.qglEnd;
import static neo.open.gl.QGL.qglFlush;
import static neo.open.gl.QGL.qglLoadIdentity;
import static neo.open.gl.QGL.qglMatrixMode;
import static neo.open.gl.QGL.qglOrtho;
import static neo.open.gl.QGL.qglReadPixels;
import static neo.open.gl.QGL.qglVertex3f;
import static neo.open.gl.QGL.qglViewport;
import static neo.open.gl.QGLConstantsIfc.GL_ALPHA_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_BLEND;
import static neo.open.gl.QGLConstantsIfc.GL_COLOR_BUFFER_BIT;
import static neo.open.gl.QGLConstantsIfc.GL_CULL_FACE;
import static neo.open.gl.QGLConstantsIfc.GL_DEPTH_BUFFER_BIT;
import static neo.open.gl.QGLConstantsIfc.GL_DEPTH_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_FRONT;
import static neo.open.gl.QGLConstantsIfc.GL_LEQUAL;
import static neo.open.gl.QGLConstantsIfc.GL_MODELVIEW;
import static neo.open.gl.QGLConstantsIfc.GL_PROJECTION;
import static neo.open.gl.QGLConstantsIfc.GL_RGBA;
import static neo.open.gl.QGLConstantsIfc.GL_SCISSOR_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_STENCIL_TEST;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_2D;
import static neo.open.gl.QGLConstantsIfc.GL_TRIANGLES;
import static neo.open.gl.QGLConstantsIfc.GL_TRUE;
import static neo.open.gl.QGLConstantsIfc.GL_UNSIGNED_BYTE;
import static neo.sys.win_glimp.GLimp_SwapBuffers;
import static neo.sys.win_shared.Sys_Milliseconds;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import neo.TempDump.TODO_Exception;
import neo.Renderer.Model.idRenderModel;
import neo.Renderer.Model.modelSurface_s;
import neo.Renderer.Model.srfTriangles_s;
import neo.Tools.Compilers.DMap.dmap.Dmap_f;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.geometry.DrawVert.idDrawVert;
import neo.idlib.geometry.Winding.idWinding;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;
import neo.open.Nio;

/**
 *
 */
public class renderbump {

    /*

     render a normalmap tga file from an ase model for bump mapping

     To make ray-tracing into the high poly mesh efficient, we preconstruct
     a 3D hash table of the triangles that need to be tested for a given source
     point.

     This task is easier than a general ray tracing optimization, because we
     known that all of the triangles are going to be "near" the source point.

     TraceFraction determines the maximum distance in any direction that
     a trace will go.  It is expressed as a fraction of the largest axis of
     the bounding box, so it doesn't matter what units are used for modeling.


     */
    static final int    MAX_QPATH                     = 256;
    //
    static final double DEFAULT_TRACE_FRACTION        = 0.05;
    //
    static final int    INITIAL_TRI_TO_LINK_EXPANSION = 16;// can grow as needed
    static final int    HASH_AXIS_BINS                = 100;

    static class triLink_t {

        int faceNum;
        int nextLink;
    }

    static class binLink_t {

        int triLink;
        int rayNumber;        // don't need to test again if still on same ray
    }
    static final int MAX_LINKS_PER_BLOCK = 0x100000;
    static final int MAX_LINK_BLOCKS     = 0x100;

    static class triHash_t {

        idBounds bounds;
        float[] binSize = new float[3];
        int numLinkBlocks;
        triLink_t[][]   linkBlocks = new triLink_t[MAX_LINK_BLOCKS][];
        binLink_t[][][] binLinks   = new binLink_t[HASH_AXIS_BINS][HASH_AXIS_BINS][HASH_AXIS_BINS];

        private void clear() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    static class renderBump_t {

        char[] outputName = new char[MAX_QPATH];
        char[] highName   = new char[MAX_QPATH];
        ByteBuffer localPic;
        ByteBuffer globalPic;
        ByteBuffer colorPic;
        float[]    edgeDistances;        // starts out -1 for untraced, for each texel, 0 = true interior, >0 = off-edge rasterization
        int        width, height;
        int            antiAlias;
        int            outline;
        boolean        saveGlobalMap;
        boolean        saveColorMap;
        float          traceFrac;
        float          traceDist;
        srfTriangles_s mesh;            // high poly mesh
        idRenderModel  highModel;
        triHash_t      hash;
    }
    static float traceFraction;
    static int   rayNumber;        // for avoiding retests of bins and faces
    //
    static int   oldWidth, oldHeight;

    /*
     ===============
     SaveWindow
     ===============
     */
    static void SaveWindow() {
        oldWidth = glConfig.vidWidth;
        oldHeight = glConfig.vidHeight;
    }

    /*
     ===============
     ResizeWindow
     ===============
     */
    static void ResizeWindow(int width, int height) {
        throw new TODO_Exception();
//        if (WIN32) {
//            int winWidth, winHeight;
//            if (glConfig.isFullscreen) {
//                winWidth = width;
//                winHeight = height;
//            } else {
//                RECT r;
//
//                // adjust width and height for window border
//                r.bottom = height;
//                r.left = 0;
//                r.top = 0;
//                r.right = width;
//
//                AdjustWindowRect(r, WINDOW_STYLE | WS_SYSMENU, FALSE);
//                winHeight = r.bottom - r.top;
//                winWidth = r.right - r.left;
//
//            }
//            SetWindowPos(win32.hWnd, HWND_TOP, 0, 0, winWidth, winHeight, SWP_SHOWWINDOW);
//
//            qwglMakeCurrent(win32.hDC, win32.hGLRC);
//        }
    }

    /*
     ===============
     RestoreWindow
     ===============
     */
    static void RestoreWindow() {
        throw new TODO_Exception();
//        if (WIN32) {
//            int winWidth, winHeight;
//            if (glConfig.isFullscreen) {
//                winWidth = oldWidth;
//                winHeight = oldHeight;
//            } else {
//                RECT r;
//
//                // adjust width and height for window border
//                r.bottom = oldHeight;
//                r.left = 0;
//                r.top = 0;
//                r.right = oldWidth;
//
//                AdjustWindowRect(r, WINDOW_STYLE | WS_SYSMENU, FALSE);
//                winHeight = r.bottom - r.top;
//                winWidth = r.right - r.left;
//            }
//            SetWindowPos(win32.hWnd, HWND_TOP, 0, 0, winWidth, winHeight, SWP_SHOWWINDOW);
//        }
    }

    /*
     ================
     OutlineNormalMap

     Puts a single pixel border around all non-empty pixels
     Does NOT copy the alpha channel, so it can be used as
     an alpha test map.
     ================
     */
    static void OutlineNormalMap(ByteBuffer data, int width, int height, int emptyR, int emptyG, int emptyB) {
        byte[] orig;
        int i, j, k, l;
        idVec3 normal;
        int out;

        orig = new byte[width * height * 4];// Mem_Alloc(width * height * 4);
//	memcpy( orig, data, width * height * 4 );
        System.arraycopy(orig, 0, data.array(), 0, width * height * 4);

        for (i = 0; i < width; i++) {
            for (j = 0; j < height; j++) {
//			out = data + ( j * width + i ) * 4;
                out = ((j * width) + i) * 4;
                if ((data.get(out + 0) != emptyR)
                        || (data.get(out + 1) != emptyG)
                        || (data.get(out + 2) != emptyB)) {
                    continue;
                }

                normal = getVec3_origin();
                for (k = -1; k < 2; k++) {
                    for (l = -1; l < 2; l++) {
                        int in;

//                        in = orig + ( ((j+l)&(height-1))*width + ((i+k)&(width-1)) ) * 4;
                        in = ((((j + l) & (height - 1)) * width) + ((i + k) & (width - 1))) * 4;

                        if ((orig[in + 0] == emptyR) && (orig[in + 1] == emptyG) && (orig[in + 2] == emptyB)) {
                            continue;
                        }

                        normal.oPluSet(0, orig[in + 0] - 128);
                        normal.oPluSet(1, orig[in + 1] - 128);
                        normal.oPluSet(2, orig[in + 2] - 128);
                    }
                }

                if (normal.Normalize() < 0.5) {
                    continue;	// no valid samples
                }

                data.put(out + 0, (byte) (128 + (127 * normal.oGet(0))));
                data.put(out + 1, (byte) (128 + (127 * normal.oGet(1))));
                data.put(out + 2, (byte) (128 + (127 * normal.oGet(2))));
            }
        }

        orig = null;//Mem_Free(orig);
    }

    /*
     ================
     OutlineColorMap

     Puts a single pixel border around all non-empty pixels
     Does NOT copy the alpha channel, so it can be used as
     an alpha test map.
     ================
     */
    static void OutlineColorMap(ByteBuffer data, int width, int height, int emptyR, int emptyG, int emptyB) {
        byte[] orig;
        int i, j, k, l;
        idVec3 normal;
        int out;

        orig = new byte[width * height * 4];// Mem_Alloc(width * height * 4);
//	memcpy( orig, data, width * height * 4 );
        System.arraycopy(orig, 0, data, 0, width * height * 4);

        for (i = 0; i < width; i++) {
            for (j = 0; j < height; j++) {
                out = ((j * width) + i) * 4;
                if ((data.get(out + 0) != emptyR)
                        || (data.get(out + 1) != emptyG)
                        || (data.get(out + 2) != emptyB)) {
                    continue;
                }

                normal = getVec3_origin();
                int count = 0;
                for (k = -1; k < 2; k++) {
                    for (l = -1; l < 2; l++) {
                        int in;

                        in = ((((j + l) & (height - 1)) * width) + ((i + k) & (width - 1))) * 4;

                        if ((orig[in + 0] == emptyR) && (orig[in + 1] == emptyG) && (orig[in + 2] == emptyB)) {
                            continue;
                        }

                        normal.oPluSet(0, orig[in + 0]);
                        normal.oPluSet(1, orig[in + 1]);
                        normal.oPluSet(2, orig[in + 2]);
                        count++;
                    }
                }

                if (0 == count) {
                    continue;
                }
                normal.oMulSet(1.0f / count);

                data.put(out + 0, (byte) normal.oGet(0));
                data.put(out + 1, (byte) normal.oGet(1));
                data.put(out + 2, (byte) normal.oGet(2));
            }
        }

        orig = null;//Mem_Free(orig);
    }

    /*
     ================
     FreeTriHash
     ================
     */
    static void FreeTriHash(triHash_t hash) {
        for (int i = 0; i < hash.numLinkBlocks; i++) {
            hash.linkBlocks[i] = null;//Mem_Free(hash.linkBlocks[i]);
        }
        hash.clear();//Mem_Free(hash);
    }

    /*
     ================
     CreateTriHash
     ================
     */
    static triHash_t CreateTriHash(final srfTriangles_s highMesh) {
        triHash_t hash;
        int i, j, k, l;
        final idBounds bounds = new idBounds(), triBounds = new idBounds();
        final int[][] iBounds = new int[2][3];
        int maxLinks, numLinks;

        hash = new triHash_t();//Mem_Alloc(sizeof(hash));
//	memset( hash, 0, sizeof( *hash ) );

        // find the bounding volume for the mesh
        bounds.Clear();
        for (i = 0; i < highMesh.numVerts; i++) {
            bounds.AddPoint(highMesh.verts[i].xyz);
        }

        hash.bounds = bounds;

        // divide each axis as needed
        for (i = 0; i < 3; i++) {
            hash.binSize[i] = (bounds.oGet(1, i) - bounds.oGet(0, i)) / HASH_AXIS_BINS;
            if (hash.binSize[i] <= 0) {
                common.FatalError("CreateTriHash: bad bounds: (%f %f %f) to (%f %f %f)",
                        bounds.oGet(0, 0), bounds.oGet(0, 1), bounds.oGet(0, 2),
                        bounds.oGet(1, 0), bounds.oGet(1, 1), bounds.oGet(1, 2));
            }
        }

        // a -1 link number terminated the link chain
//        memset(hash.binLinks, -1, sizeof(hash.binLinks));
        for (final binLink_t[][] A : hash.binLinks) {
            for (final binLink_t[] B : A) {
                for (final binLink_t C : B) {
                    C.rayNumber = -1;
                    C.triLink = -1;
                }
            }
        }

        numLinks = 0;

        hash.linkBlocks[hash.numLinkBlocks] = new triLink_t[MAX_LINKS_PER_BLOCK];// Mem_Alloc(MAX_LINKS_PER_BLOCK * sizeof(triLink_t));
        hash.numLinkBlocks++;
        maxLinks = hash.numLinkBlocks * MAX_LINKS_PER_BLOCK;

        // for each triangle, place a triLink in each bin that might reference it
        for (i = 0; i < highMesh.getIndexes().getNumValues(); i += 3) {
            // determine which hash bins the triangle will need to be in
            triBounds.Clear();
            for (j = 0; j < 3; j++) {
                triBounds.AddPoint(highMesh.verts[ highMesh.getIndexes().getValues().get(i + j)].xyz);
            }
            for (j = 0; j < 3; j++) {
                iBounds[0][j] = (int) ((triBounds.oGet(0, j) - hash.bounds.oGet(0, j)) / hash.binSize[j]);
                iBounds[0][j] -= 0.001;	// epsilon
                if (iBounds[0][j] < 0) {
                    iBounds[0][j] = 0;
                } else if (iBounds[0][j] >= HASH_AXIS_BINS) {
                    iBounds[0][j] = HASH_AXIS_BINS - 1;
                }

                iBounds[1][j] = (int) ((triBounds.oGet(1, j) - hash.bounds.oGet(0, j)) / hash.binSize[j]);
                iBounds[0][j] += 0.001;	// epsilon
                if (iBounds[1][j] < 0) {
                    iBounds[1][j] = 0;
                } else if (iBounds[1][j] >= HASH_AXIS_BINS) {
                    iBounds[1][j] = HASH_AXIS_BINS - 1;
                }
            }

            // add the links
            for (j = iBounds[0][0]; j <= iBounds[1][0]; j++) {
                for (k = iBounds[0][1]; k <= iBounds[1][1]; k++) {
                    for (l = iBounds[0][2]; l <= iBounds[1][2]; l++) {
                        if (numLinks == maxLinks) {
                            hash.linkBlocks[hash.numLinkBlocks] = new triLink_t[MAX_LINKS_PER_BLOCK];// Mem_Alloc(MAX_LINKS_PER_BLOCK * sizeof(triLink_t));
                            hash.numLinkBlocks++;
                            maxLinks = hash.numLinkBlocks * MAX_LINKS_PER_BLOCK;
                        }

                        final triLink_t link = hash.linkBlocks[ numLinks / MAX_LINKS_PER_BLOCK][ numLinks % MAX_LINKS_PER_BLOCK];//TODO:pointer??
                        link.faceNum = i / 3;
                        link.nextLink = hash.binLinks[j][k][l].triLink;
                        hash.binLinks[j][k][l].triLink = numLinks;
                        numLinks++;
                    }
                }
            }
        }

        common.Printf("%d triangles made %d links\n", highMesh.getIndexes().getNumValues() / 3, numLinks);

        return hash;
    }
    /*
     =================
     TraceToMeshFace

     Returns the distance from the point to the intersection, or DIST_NO_INTERSECTION
     =================
     */
    static final float DIST_NO_INTERSECTION = -999999999.0f;

    static float TraceToMeshFace(final srfTriangles_s highMesh, int faceNum, float minDist, float maxDist,
            final idVec3 point, final idVec3 normal, idVec3 sampledNormal, byte[] sampledColor/*[4]*/) {
        int j;
        float dist;
        final idVec3[] v = new idVec3[3];
        idPlane plane;
        idVec3 edge;
        float d;
        final idVec3[] dir = new idVec3[3];
        float baseArea;
        final float[] bary = new float[3];
        idVec3 testVert;

        v[0] = highMesh.verts[ highMesh.getIndexes().getValues().get( (faceNum * 3) + 0)].xyz;
        v[1] = highMesh.verts[ highMesh.getIndexes().getValues().get( (faceNum * 3) + 1)].xyz;
        v[2] = highMesh.verts[ highMesh.getIndexes().getValues().get( (faceNum * 3) + 2)].xyz;

        plane = highMesh.facePlanes[faceNum];

        // only test against planes facing the same direction as our normal
        d = plane.Normal().oMultiply(normal);
        if (d <= 0.0001f) {
            return DIST_NO_INTERSECTION;
        }

        // find the point of impact on the plane
        dist = plane.Distance(point);
        dist /= -d;

        testVert = point.oPlus(normal.oMultiply(dist));

        // if this would be beyond our requested trace distance,
        // don't even check it
        if (dist > maxDist) {
            return DIST_NO_INTERSECTION;
        }

        if (dist < minDist) {
            return DIST_NO_INTERSECTION;
        }

        // if normal is inside all edge planes, this face is hit
        VectorSubtract(v[0], point, dir[0]);
        VectorSubtract(v[1], point, dir[1]);
        edge = dir[0].Cross(dir[1]);
        d = DotProduct(normal, edge);
        if (d > 0.0f) {
            return DIST_NO_INTERSECTION;
        }
        VectorSubtract(v[2], point, dir[2]);
        edge = dir[1].Cross(dir[2]);
        d = DotProduct(normal, edge);
        if (d > 0.0f) {
            return DIST_NO_INTERSECTION;
        }
        edge = dir[2].Cross(dir[0]);
        d = DotProduct(normal, edge);
        if (d > 0.0f) {
            return DIST_NO_INTERSECTION;
        }

        // calculate barycentric coordinates of the impact point
        // on the high poly triangle
        bary[0] = idWinding.TriangleArea(testVert, v[1], v[2]);
        bary[1] = idWinding.TriangleArea(v[0], testVert, v[2]);
        bary[2] = idWinding.TriangleArea(v[0], v[1], testVert);

        baseArea = idWinding.TriangleArea(v[0], v[1], v[2]);
        bary[0] /= baseArea;
        bary[1] /= baseArea;
        bary[2] /= baseArea;

        if ((bary[0] + bary[1] + bary[2]) > 1.1) {
            bary[0] = bary[0];
            return DIST_NO_INTERSECTION;
        }

        // triangularly interpolate the normals to the sample point
        sampledNormal.oSet(getVec3_origin());
        for (j = 0; j < 3; j++) {
            sampledNormal.oPluSet(highMesh.verts[ highMesh.getIndexes().getValues().get( (faceNum * 3) + j)].normal.oMultiply(bary[j]));
        }
        sampledNormal.Normalize();

        sampledColor[0] = sampledColor[1] = sampledColor[2] = sampledColor[3] = 0;
        for (int i = 0; i < 4; i++) {
            float color = 0.0f;
            for (j = 0; j < 3; j++) {
                color += bary[j] * highMesh.verts[ highMesh.getIndexes().getValues().get( (faceNum * 3) + j)].getColor().get(i);
            }
            sampledColor[i] = (byte) color;
        }
        return dist;
    }
    static final int RAY_STEPS = 100;

    /*
     ================
     SampleHighMesh

     Find the best surface normal in the high poly mesh 
     for a ray coming from the surface of the low poly mesh

     Returns false if the trace doesn't hit anything
     ================
     */
    static boolean SampleHighMesh(final renderBump_t rb, final idVec3 point, final idVec3 direction, idVec3 sampledNormal, byte[] sampledColor/*[4]*/) {
        idVec3 p;
        binLink_t bl;
        int linkNum;
        int faceNum;
        float dist, bestDist;
        final int[] block = new int[3];
        float maxDist;
        int c_hits;
        int i;
        idVec3 normal;

        // we allow non-normalized directions on input
        normal = direction;
        normal.Normalize();

        // increment our uniqueness counter (FIXME: make thread safe?)
        rayNumber++;

        // the max distance will be the traceFrac times the longest axis of the high poly model
        bestDist = -rb.traceDist;
        maxDist = rb.traceDist;

        sampledNormal.oSet(getVec3_origin());

        c_hits = 0;

        // this is a pretty damn lazy way to walk through a 3D grid, and has a (very slight)
        // chance of missing a triangle in a corner crossing case
        for (i = 0; i < RAY_STEPS; i++) {
            p = point.oMinus(rb.hash.bounds.oGet(0).oPlus(normal.oMultiply(-1.0f + ((2.0f * i) / RAY_STEPS)).oMultiply(rb.traceDist)));//TODO:check if downcasting from doubles to floats has any effect

            block[0] = (int) floor(p.oGet(0) / rb.hash.binSize[0]);
            block[1] = (int) floor(p.oGet(1) / rb.hash.binSize[1]);
            block[2] = (int) floor(p.oGet(2) / rb.hash.binSize[2]);

            if ((block[0] < 0) || (block[0] >= HASH_AXIS_BINS)) {
                continue;
            }
            if ((block[1] < 0) || (block[1] >= HASH_AXIS_BINS)) {
                continue;
            }
            if ((block[2] < 0) || (block[2] >= HASH_AXIS_BINS)) {
                continue;
            }

            // FIXME: casting away const
            bl = rb.hash.binLinks[block[0]][block[1]][block[2]];
            if (bl.rayNumber == rayNumber) {
                continue;		// already tested this block
            }
            bl.rayNumber = rayNumber;
            linkNum = bl.triLink;
            triLink_t link;
            for (; linkNum != -1; linkNum = link.nextLink) {
                link = rb.hash.linkBlocks[ linkNum / MAX_LINKS_PER_BLOCK][ linkNum % MAX_LINKS_PER_BLOCK];

                faceNum = link.faceNum;
                dist = TraceToMeshFace(rb.mesh, faceNum,
                        bestDist, maxDist, point, normal, sampledNormal, sampledColor);
                if (dist == DIST_NO_INTERSECTION) {
                    continue;
                }

                c_hits++;
                // continue looking for a better match
                bestDist = dist;
            }
        }

        return bestDist > -rb.traceDist;
    }

    /*
     =============
     TriTextureArea

     This may be negatove
     =============
     */
    static float TriTextureArea(final float[] a/*[2]*/, final float[] b/*[2]*/, final float[] c/*[2]*/) {
        final idVec3 d1 = new idVec3(), d2 = new idVec3();
        idVec3 cross;
        float area;

        d1.oSet(0, b[0] - a[0]);
        d1.oSet(1, b[1] - a[1]);
        d1.oSet(2, 0);

        d2.oSet(0, c[0] - a[0]);
        d2.oSet(1, c[1] - a[1]);
        d2.oSet(2, 0);

        cross = d1.Cross(d2);
        area = 0.5f * cross.Length();

        if (cross.oGet(2) < 0) {
            return -area;
        } else {
            return area;
        }
    }
    private static final boolean SKIP_MIRRORS = false;//TODO:set default value

    /*
     ================
     RasterizeTriangle

     It is ok for the texcoords to wrap around, the rasterization
     will deal with it properly.
     ================
     */
    static void RasterizeTriangle(final srfTriangles_s lowMesh, final idVec3[] lowMeshNormals, int lowFaceNum, renderBump_t[] rbs) {
        int i, j, k, q;
        final float[][] bounds = new float[2][2];
        final float[][] ibounds = new float[2][2];
        final float[][] verts = new float[3][2];
        final float[] testVert = new float[2];
        final float[] bary = new float[3];
        ByteBuffer localDest, globalDest, colorDest;
        final float[][] edge = new float[3][3];
        final idVec3 sampledNormal = new idVec3();
        final byte[] sampledColor = new byte[4];
        idVec3 point, normal, traceNormal;
        final idVec3[] tangents = new idVec3[2];
        float baseArea, totalArea;
        int r, g, b;
        idVec3 localNormal;

        // this is a brain-dead rasterizer, but compared to the ray trace,
        // nothing we do here is going to matter performance-wise
        // adjust for resolution and texel centers
        verts[0][0] = (lowMesh.verts[ lowMesh.getIndexes().getValues().get((lowFaceNum * 3) + 0)].st.oGet(0) * rbs[0].width) - 0.5f;
        verts[1][0] = (lowMesh.verts[ lowMesh.getIndexes().getValues().get((lowFaceNum * 3) + 1)].st.oGet(0) * rbs[0].width) - 0.5f;
        verts[2][0] = (lowMesh.verts[ lowMesh.getIndexes().getValues().get((lowFaceNum * 3) + 2)].st.oGet(0) * rbs[0].width) - 0.5f;
        verts[0][1] = (lowMesh.verts[ lowMesh.getIndexes().getValues().get((lowFaceNum * 3) + 0)].st.oGet(1) * rbs[0].width) - 0.5f;
        verts[1][1] = (lowMesh.verts[ lowMesh.getIndexes().getValues().get((lowFaceNum * 3) + 1)].st.oGet(1) * rbs[0].width) - 0.5f;
        verts[2][1] = (lowMesh.verts[ lowMesh.getIndexes().getValues().get((lowFaceNum * 3) + 2)].st.oGet(1) * rbs[0].width) - 0.5f;

        // find the texcoord bounding box
        bounds[0][0] = 99999;
        bounds[0][1] = 99999;
        bounds[1][0] = -99999;
        bounds[1][1] = -99999;
        for (i = 0; i < 2; i++) {
            for (j = 0; j < 3; j++) {
                if (verts[j][i] < bounds[0][i]) {
                    bounds[0][i] = verts[j][i];
                }
                if (verts[j][i] > bounds[1][i]) {
                    bounds[1][i] = verts[j][i];
                }
            }
        }

        // we intentionally rasterize somewhat outside the triangles, so
        // the bilerp support texels (which may be anti-aliased down)
        // are not just duplications of what is on the interior
        final float edgeOverlap = 4.0f;

        ibounds[0][0] = (float) floor(bounds[0][0] - edgeOverlap);
        ibounds[1][0] = (float) ceil(bounds[1][0] + edgeOverlap);
        ibounds[0][1] = (float) floor(bounds[0][1] - edgeOverlap);
        ibounds[1][1] = (float) ceil(bounds[1][1] + edgeOverlap);

        // calculate edge vectors
        for (i = 0; i < 3; i++) {
            float[] v1, v2;

            v1 = verts[i];
            v2 = verts[(i + 1) % 3];

            edge[i][0] = v2[1] - v1[1];
            edge[i][1] = v1[0] - v2[0];
            final float len = (float) sqrt((edge[i][0] * edge[i][0]) + (edge[i][1] * edge[i][1]));
            edge[i][0] /= len;
            edge[i][1] /= len;
            edge[i][2] = -((v1[0] * edge[i][0]) + (v1[1] * edge[i][1]));
        }

        // itterate over the bounding box, testing against edge vectors
        for (i = (int) ibounds[0][1], q = 0; i < ibounds[1][1]; i++) {
            for (j = (int) ibounds[0][0]; j < ibounds[1][0]; j++, q++) {
                final float[] dists = new float[3];
                final renderBump_t rb = rbs[q];//TODO: triple check the 'q' value against 'k', and make sure we don't go out of bounds.

                k = (((i & (rb.height - 1)) * rb.width) + (j & (rb.width - 1))) * 4;
                colorDest = rb.colorPic;//[k];
                localDest = rb.localPic;//[k];
                globalDest = rb.globalPic;//[k];

//                float[] edgeDistance = rb.edgeDistances[k / 4];
                if (SKIP_MIRRORS) {
                    // if this texel has already been filled by a true interior pixel, don't overwrite it
                    if (rb.edgeDistances[0 + (k / 4)] == 0) {
                        continue;
                    }
                }

                // check against the three edges to see if the pixel is inside the triangle
                for (k = 0; k < 3; k++) {
                    float v;

                    v = (i * edge[k][1]) + (j * edge[k][0]) + edge[k][2];
                    dists[k] = v;
                }

                // the edge polarities might be either way
                if (!(((dists[0] >= -edgeOverlap) && (dists[1] >= -edgeOverlap) && (dists[2] >= -edgeOverlap))
                        || ((dists[0] <= edgeOverlap) && (dists[1] <= edgeOverlap) && (dists[2] <= edgeOverlap)))) {
                    continue;
                }

                boolean edgeTexel;

                if (((dists[0] >= 0) && (dists[1] >= 0) && (dists[2] >= 0))
                        || ((dists[0] <= 0) && (dists[1] <= 0) && (dists[2] <= 0))) {
                    edgeTexel = false;
                } else {
                    edgeTexel = true;
                    if (SKIP_MIRRORS) {
                        // if this texel has already been filled by another edge pixel, don't overwrite it
                        if (rb.edgeDistances[1 + (k / 4)] == 1) {
                            continue;
                        }
                    }
                }

                // calculate the barycentric coordinates in the triangle for this sample
                testVert[0] = j;
                testVert[1] = i;

                baseArea = TriTextureArea(verts[0], verts[1], verts[2]);
                bary[0] = TriTextureArea(testVert, verts[1], verts[2]) / baseArea;
                bary[1] = TriTextureArea(verts[0], testVert, verts[2]) / baseArea;
                bary[2] = TriTextureArea(verts[0], verts[1], testVert) / baseArea;

                totalArea = bary[0] + bary[1] + bary[2];
                if ((totalArea < 0.99) || (totalArea > 1.01)) {
                    continue;	// should never happen
                }

                // calculate the interpolated xyz, normal, and tangents of this sample
                point = getVec3_origin();
                traceNormal = getVec3_origin();
                normal = getVec3_origin();
                tangents[0] = getVec3_origin();
                tangents[1] = getVec3_origin();
                for (k = 0; k < 3; k++) {
                    int index;

                    index = lowMesh.getIndexes().getValues().get((lowFaceNum * 3) + k);
                    point.oPluSet(lowMesh.verts[index].xyz.oMultiply(bary[k]));

                    // traceNormal will differ from normal if the surface uses unsmoothedTangents
                    traceNormal.oPluSet(lowMeshNormals[index].oMultiply(bary[k]));

                    normal.oPluSet(lowMesh.verts[index].normal.oMultiply(bary[k]));
                    tangents[0].oPluSet(lowMesh.verts[index].tangents[0].oMultiply(bary[k]));
                    tangents[1].oPluSet(lowMesh.verts[index].tangents[1].oMultiply(bary[k]));
                }

//#if 0
//			// this doesn't seem to make much difference
//			// an argument can be made that these should not be normalized, because the interpolation
//			// of the light position at rasterization time will be linear, not spherical
//			normal.Normalize();
//			tangents[0].Normalize();
//			tangents[1].Normalize();
//}
//
//			// find the best triangle in the high poly model for this
//			// sampledNormal will  normalized
//			if ( !SampleHighMesh( rb, point, traceNormal, sampledNormal, sampledColor ) ) {
//#if 0
//				// put bright red where all traces missed for debugging.
//				// for production use, it is better to leave it blank so
//				// the outlining fills it in
//				globalDest[0] = 255;
//				globalDest[1] = 0;
//				globalDest[2] = 0;
//				globalDest[3] = 255;
//
//				localDest[0] = 255;
//				localDest[1] = 0;
//				localDest[2] = 0;
//				localDest[3] = 255;
//}
//				continue;
//			}
                // mark whether this is an interior or edge texel
                rb.edgeDistances[0 + (k / 4)] = (edgeTexel ? 1.0f : 0);

                // fill the object space normal map spot
                r = (int) (128 + (127 * sampledNormal.oGet(0)));
                g = (int) (128 + (127 * sampledNormal.oGet(1)));
                b = (int) (128 + (127 * sampledNormal.oGet(2)));

                globalDest.put(0, (byte) r);
                globalDest.put(1, (byte) g);
                globalDest.put(2, (byte) b);
                globalDest.put(3, (byte) 255);

                // transform to local tangent space
                final idMat3 mat = new idMat3(tangents[0], tangents[1], normal);
                mat.InverseSelf();
                localNormal = mat.oMultiply(sampledNormal);

                localNormal.Normalize();

                r = (int) (128 + (127 * localNormal.oGet(0)));
                g = (int) (128 + (127 * localNormal.oGet(1)));
                b = (int) (128 + (127 * localNormal.oGet(2)));

                localDest.put(0, (byte) r);
                localDest.put(1, (byte) g);
                localDest.put(2, (byte) b);
                localDest.put(3, (byte) 255);

                colorDest.put(0, sampledColor[0]);
                colorDest.put(1, sampledColor[1]);
                colorDest.put(2, sampledColor[2]);
                colorDest.put(3, sampledColor[3]);
            }
        }
    }

    /*
     ================
     CombineModelSurfaces

     Frees the model and returns a new model with all triangles combined
     into one surface
     ================
     */
    static idRenderModel CombineModelSurfaces(idRenderModel model) {
        int totalVerts;
        int totalIndexes;
        int numIndexes;
        int numVerts;
        int i, j;

        totalVerts = 0;
        totalIndexes = 0;

        for (i = 0; i < model.NumSurfaces(); i++) {
            final modelSurface_s surf = model.Surface(i);

            totalVerts += surf.geometry.numVerts;
            totalIndexes += surf.geometry.getIndexes().getNumValues();
        }

        final srfTriangles_s newTri = R_AllocStaticTriSurf();
        R_AllocStaticTriSurfVerts(newTri, totalVerts);
        R_AllocStaticTriSurfIndexes(newTri, totalIndexes);

        newTri.numVerts = totalVerts;
        newTri.getIndexes().setNumValues(totalIndexes);

        newTri.bounds.Clear();

        final idDrawVert[] verts = newTri.verts;
        final IntBuffer/*glIndex_t*/ indexes = newTri.getIndexes().getValues();
        numIndexes = 0;
        numVerts = 0;
        for (i = 0; i < model.NumSurfaces(); i++) {
            final modelSurface_s surf = model.Surface(i);
            final srfTriangles_s tri = surf.geometry;

//            memcpy(verts + numVerts, tri.verts, tri.numVerts * sizeof(tri.verts[0]));
            System.arraycopy(tri.verts, 0, verts, numVerts, tri.numVerts);
            for (j = 0; j < tri.getIndexes().getNumValues(); j++) {
            	indexes.put(numIndexes + j, numVerts + tri.getIndexes().getValues().get(j));
            }
            newTri.bounds.AddBounds(tri.bounds);
            numIndexes += tri.getIndexes().getNumValues();
            numVerts += tri.numVerts;
        }

        final modelSurface_s surf = new modelSurface_s();

        surf.id = 0;
        surf.geometry = newTri;
        surf.shader = tr.defaultMaterial;

        final idRenderModel newModel = renderModelManager.AllocModel();
        newModel.AddSurface(surf);

        renderModelManager.FreeModel(model);

        return newModel;
    }

    /*
     ==============
     RenderBumpTriangles

     ==============
     */
    static void RenderBumpTriangles(srfTriangles_s lowMesh, renderBump_t rb) {
        throw new TODO_Exception();
//        int i, j;
//
//        RB_SetGL2D();
//
//        qglDisable(GL_CULL_FACE);
//
//        qglColor3f(1, 1, 1);
//
//        qglMatrixMode(GL_PROJECTION);
//        qglLoadIdentity();
//        qglOrtho(0, 1, 1, 0, -1, 1);
//        qglDisable(GL_BLEND);
//        qglMatrixMode(GL_MODELVIEW);
//        qglLoadIdentity();
//
//        qglDisable(GL_DEPTH_TEST);
//
//        qglClearColor(1, 0, 0, 1);
//        qglClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
//
//        qglColor3f(1, 1, 1);
//
//        // create smoothed normals for the surface, which might be
//        // different than the normals at the vertexes if the
//        // surface uses unsmoothedNormals, which only takes the
//        // normal from a single triangle.  We need properly smoothed
//        // normals to make sure that the traces always go off normal
//        // to the true surface.
//        idVec3[] lowMeshNormals = new idVec3[lowMesh.numVerts];// Mem_ClearedAlloc(lowMesh.numVerts /* sizeof( lowMeshNormals )*/);
//        R_DeriveFacePlanes(lowMesh);
//        R_CreateSilIndexes(lowMesh);	// recreate, merging the mirrored verts back together
//        idPlane plane = lowMesh.facePlanes[0];
//        int p;
//        for (i = 0, p = 0; i < lowMesh.numIndexes; i += 3, plane = lowMesh.facePlanes[++p]) {
//            for (j = 0; j < 3; j++) {
//                int index;
//
//                index = lowMesh.silIndexes[i + j];
//                lowMeshNormals[index].oPluSet(plane.Normal());
//            }
//        }
//        // normalize and replicate from silIndexes to all indexes
//        for (i = 0; i < lowMesh.numIndexes; i++) {
//            lowMeshNormals[lowMesh.indexes[i]] = lowMeshNormals[lowMesh.silIndexes[i]];//TODO: create shuffle function that moves
//            lowMeshNormals[lowMesh.indexes[i]].Normalize();
//        }
//
//        // rasterize each low poly face
//        for (j = 0; j < lowMesh.numIndexes; j += 3) {
//            // pump the event loop so the window can be dragged around
//            Sys_GenerateEvents();
//
//            RasterizeTriangle(lowMesh, lowMeshNormals, j / 3, rb);
//
//            qglClearColor(1, 0, 0, 1);
//            qglClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
//            qglRasterPos2f(0, 1);
//            qglPixelZoom(glConfig.vidWidth / (float) rb.width, glConfig.vidHeight / (float) rb.height);
//            qglDrawPixels(rb.width, rb.height, GL_RGBA, GL_UNSIGNED_BYTE, rb.localPic);
//            qglPixelZoom(1, 1);
//            qglFlush();
//            GLimp_SwapBuffers();
//        }
//
//        lowMeshNormals = null;//Mem_Free(lowMeshNormals);
    }


    /*
     ==============
     WriteRenderBump

     ==============
     */
    static void WriteRenderBump(renderBump_t rb, int outLinePixels) {
        int width, height;
        int i;
        idStr filename;

//        renderModelManager.FreeModel(rb.highModel);
        rb.highModel = null;

        FreeTriHash(rb.hash);

        width = rb.width;
        height = rb.height;

//#if 0
//	// save the non-outlined version
//	filename = source;
//	filename.setFileExtension();
//	filename.append( "_nooutline.tga" );
//	common.Printf( "writing %s\n", filename.c_str() );
//	WriteTGA( filename, globalPic, width, height );
//}
        // outline the image several times to help bilinear filtering across disconnected
        // edges, and mip-mapping
        for (i = 0; i < outLinePixels; i++) {
            OutlineNormalMap(rb.localPic, width, height, 128, 128, 128);
            OutlineNormalMap(rb.globalPic, width, height, 128, 128, 128);
            OutlineColorMap(rb.colorPic, width, height, 128, 128, 128);
        }

        // filter down if we are anti-aliasing
        for (i = 0; i < rb.antiAlias; i++) {
            ByteBuffer old;

            old = rb.localPic;
            rb.localPic = R_MipMap(rb.localPic, width, height, false);
            //Mem_Free(old);

            old = rb.globalPic;
            rb.globalPic = R_MipMap(rb.globalPic, width, height, false);
            //Mem_Free(old);

            old = rb.colorPic;
            rb.colorPic = R_MipMap(rb.colorPic, width, height, false);
            old = null;//Mem_Free(old);

            width >>= 1;
            height >>= 1;
        }

        // write out the local map
        filename = new idStr(rb.outputName);
        filename.SetFileExtension(".tga");
        common.Printf("writing %s (%d,%d)\n", filename, width, height);
        R_WriteTGA(filename, rb.localPic, width, height);

        if (rb.saveGlobalMap) {
            filename.oSet(rb.outputName);
            filename.StripFileExtension();
            filename.Append("_global.tga");
            common.Printf("writing %s (%d,%d)\n", filename, width, height);
            R_WriteTGA(filename, rb.globalPic, width, height);
        }

        if (rb.saveColorMap) {
            filename.oSet(rb.outputName);
            filename.StripFileExtension();
            filename.Append("_color.tga");
            common.Printf("writing %s (%d,%d)\n", filename, width, height);
            R_WriteTGA(filename, rb.colorPic, width, height);
        }

        rb.localPic = null;//Mem_Free(rb.localPic);
        rb.globalPic = null;//Mem_Free(rb.globalPic);
        rb.colorPic = null;//Mem_Free(rb.colorPic);
        rb.edgeDistances = null;//Mem_Free(rb.edgeDistances);
    }

    /*
     ===============
     InitRenderBump
     ===============
     */
    static void InitRenderBump(renderBump_t rb) {
        srfTriangles_s mesh;
        idBounds bounds;
        int i, c;

        // load the ase file
        common.Printf("loading %s...\n", rb.highName);

        rb.highModel = renderModelManager.AllocModel();
        rb.highModel.PartialInitFromFile(rb.highName.toString());
        if (NOT(rb.highModel)) {
            common.Error("failed to load %s", rb.highName);
        }

        // combine the high poly model into a single polyset
        if (rb.highModel.NumSurfaces() != 1) {
            rb.highModel = CombineModelSurfaces(rb.highModel);
        }

        final modelSurface_s surf = rb.highModel.Surface(0);
        mesh = surf.geometry;

        rb.mesh = mesh;

        R_DeriveFacePlanes(mesh);

        // create a face hash table to accelerate the tracing
        rb.hash = CreateTriHash(mesh);

        // bound the entire file
        R_BoundTriSurf(mesh);
        bounds = mesh.bounds;

        // the traceDist will be the traceFrac times the larges bounds axis
        rb.traceDist = 0;
        for (i = 0; i < 3; i++) {
            float d;

            d = rb.traceFrac * (bounds.oGet(1, i) - bounds.oGet(0, i));
            if (d > rb.traceDist) {
                rb.traceDist = d;
            }
        }
        common.Printf("trace fraction %4.2f = %6.2f model units\n", rb.traceFrac, rb.traceDist);

        c = rb.width * rb.height * 4;

        // local normal map
        rb.localPic = ByteBuffer.allocate(c);// Mem_Alloc(c);

        // global (object space, not surface space) normal map
        rb.globalPic = ByteBuffer.allocate(c);// Mem_Alloc(c);

        // color pic for artist reference
        rb.colorPic = ByteBuffer.allocate(c);// Mem_Alloc(c);

        // edgeDistance for marking outside-the-triangle traces
        rb.edgeDistances = new float[c]; // Mem_Alloc(c);

        for (i = 0; i < c; i += 4) {
            rb.localPic.put(i + 0, (byte) 128);
            rb.localPic.put(i + 1, (byte) 128);
            rb.localPic.put(i + 2, (byte) 128);
            rb.localPic.put(i + 3, (byte) 0);	// the artists use this for masking traced pixels sometimes

            rb.globalPic.put(i + 0, (byte) 128);
            rb.globalPic.put(i + 1, (byte) 128);
            rb.globalPic.put(i + 2, (byte) 128);
            rb.globalPic.put(i + 3, (byte) 0);

            rb.colorPic.put(i + 0, (byte) 128);
            rb.colorPic.put(i + 1, (byte) 128);
            rb.colorPic.put(i + 2, (byte) 128);
            rb.colorPic.put(i + 3, (byte) 0);

            rb.edgeDistances[i / 4] = -1;	// not traced yet
        }

    }

    /*
     ==============
     RenderBump_f

     ==============
     */
    public static class RenderBump_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new Dmap_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            idRenderModel lowPoly;
            String source;
            int i, j;
            String cmdLine;
            int numRenderBumps;
            renderBump_t[] renderBumps;
            renderBump_t rb = new renderBump_t();
            renderBump_t opt = new renderBump_t();
            int startTime, endTime;

            // update the screen as we print
            common.SetRefreshOnPrint(true);

            // there should be a single parameter, the filename for a game loadable low-poly model
            if (args.Argc() != 2) {
                common.Error("Usage: renderbump <lowPolyModel>");
            }

            common.Printf("----- Renderbump %s -----\n", args.Argv(1));

            startTime = Sys_Milliseconds();

            // get the lowPoly model
            source = args.Argv(1);
            lowPoly = renderModelManager.CheckModel(source);
            if (null == lowPoly) {
                common.Error("Can't load model %s", source);
            }

//        renderBumps = (renderBump_t) R_StaticAlloc(lowPoly.NumSurfaces() * sizeof(renderBumps));
            renderBumps = new renderBump_t[lowPoly.NumSurfaces()];
            numRenderBumps = 0;
            for (i = 0; i < lowPoly.NumSurfaces(); i++) {
                final modelSurface_s ms = lowPoly.Surface(i);

                // default options
//            memset(opt, 0, sizeof(opt));
                opt = new renderBump_t();
                opt.width = 512;
                opt.height = 512;
                opt.antiAlias = 1;
                opt.outline = 8;
                opt.traceFrac = 0.05f;

                // parse the renderbump parameters for this surface
                cmdLine = ms.shader.GetRenderBump();

                common.Printf("surface %d, shader %s\nrenderBump = %s ", i,
                        ms.shader.GetName(), cmdLine);

                if (NOT(ms.geometry)) {
                    common.Printf("(no geometry)\n");
                    continue;
                }

                final idCmdArgs localArgs = new idCmdArgs();
                localArgs.TokenizeString(cmdLine, false);

                if (localArgs.Argc() < 2) {
                    common.Printf("(no action)\n");
                    continue;
                }

                common.Printf("(rendering)\n");

                for (j = 0; j < (localArgs.Argc() - 2); j++) {
                    String s;

                    s = localArgs.Argv(j);
                    if (s.charAt(0) == '-') {
                        j++;
                        s = localArgs.Argv(j);
                        if (s.charAt(0) == '\0') {
                            continue;
                        }
                    }

                    if (0 == idStr.Icmp(s, "size")) {
                        if ((j + 2) >= localArgs.Argc()) {
                            j = localArgs.Argc();
                            break;
                        }
                        opt.width = Integer.parseInt(localArgs.Argv(j + 1));
                        opt.height = Integer.parseInt(localArgs.Argv(j + 2));
                        j += 2;
                    } else if (0 == idStr.Icmp(s, "trace")) {
                        opt.traceFrac = Float.parseFloat(localArgs.Argv(j + 1));
                        j += 1;
                    } else if (0 == idStr.Icmp(s, "globalMap")) {
                        opt.saveGlobalMap = true;
                    } else if (0 == idStr.Icmp(s, "colorMap")) {
                        opt.saveColorMap = true;
                    } else if (0 == idStr.Icmp(s, "outline")) {
                        opt.outline = Integer.parseInt(localArgs.Argv(j + 1));
                        j += 1;
                    } else if (0 == idStr.Icmp(s, "aa")) {
                        opt.antiAlias = Integer.parseInt(localArgs.Argv(j + 1));
                        j += 1;
                    } else {
                        common.Printf("WARNING: Unknown option \"%s\"\n", s);
                        break;
                    }
                }

                if (j != (localArgs.Argc() - 2)) {
                    common.Error("usage: renderBump [-size width height] [-aa <1-2>] [globalMap] [colorMap] [-trace <0.01 - 1.0>] normalMapImageFile highPolyAseFile");
                }
                idStr.Copynz(opt.outputName, localArgs.Argv(j), localArgs.Argv(j).length());
                idStr.Copynz(opt.highName, localArgs.Argv(j + 1), localArgs.Argv(j + 1).length());

                // adjust size for anti-aliasing
                opt.width <<= opt.antiAlias;
                opt.height <<= opt.antiAlias;

                // see if we already have a renderbump going for another surface that this should use
                for (j = 0; j < numRenderBumps; j++) {
                    rb = renderBumps[j];

                    if (idStr.Icmp(rb.outputName, opt.outputName) != 0) {
                        continue;
                    }
                    // all the other parameters must match, or it is an error
                    if ((idStr.Icmp(rb.highName, opt.highName) != 0) || (rb.width != opt.width)
                            || (rb.height != opt.height) || (rb.antiAlias != opt.antiAlias)
                            || (rb.traceFrac != opt.traceFrac)) {
                        common.Error("mismatched renderbump parameters on image %s", rb.outputName);
                        continue;
                    }

                    // saveGlobalMap will be a sticky option
                    rb.saveGlobalMap = rb.saveGlobalMap | opt.saveGlobalMap;
                    break;
                }

                // create a new renderbump if needed
                if (j == numRenderBumps) {
                    numRenderBumps++;
                    rb = renderBumps[j] = opt;

                    InitRenderBump(rb);
                }

                // render the triangles for this surface
                RenderBumpTriangles(ms.geometry, rb);
            }

            //
            // anti-alias and write out all renderbumps that we have completed
            //
            for (i = 0; i < numRenderBumps; i++) {
                WriteRenderBump(renderBumps[i], opt.outline << opt.antiAlias);
            }
//
//            R_StaticFree(renderBumps);

            endTime = Sys_Milliseconds();
            common.Printf("%5.2f seconds for renderBump\n", (endTime - startTime) / 1000.0);
            common.Printf("---------- RenderBump Completed ----------\n");

            // stop updating the screen as we print
            common.SetRefreshOnPrint(false);
        }
    }

    /*
     ==================================================================================

     FLAT

     The flat case is trivial, and accomplished with hardware rendering

     ==================================================================================
     */
    /*
     ==============
     RenderBumpFlat_f

     ==============
     */
    public static class RenderBumpFlat_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new RenderBumpFlat_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int width, height;
            String source;
            int i;
            idBounds bounds;
            srfTriangles_s mesh;
            float boundsScale;

            // update the screen as we print
            common.SetRefreshOnPrint(true);

            width = height = 256;
            boundsScale = 0;

            // check options
            for (i = 1; i < (args.Argc() - 1); i++) {
                String s;

                s = args.Argv(i);
                if (s.charAt(0) == '-') {
                    i++;
                    s = args.Argv(i);
                }

                if (0 == idStr.Icmp(s, "size")) {
                    if ((i + 2) >= args.Argc()) {
                        i = args.Argc();
                        break;
                    }
                    width = Integer.parseInt(args.Argv(i + 1));
                    height = Integer.parseInt(args.Argv(i + 2));
                    i += 2;
                } else {
                    common.Printf("WARNING: Unknown option \"%s\"\n", s);
                    break;
                }
            }

            if (i != (args.Argc() - 1)) {
                common.Error("usage: renderBumpFlat [-size width height] asefile");
            }

            common.Printf("Final image size: %d, %d\n", width, height);

            // load the source in "fastload" mode, because we don't
            // need tangent and shadow information
            source = args.Argv(i);

            idRenderModel highPolyModel = renderModelManager.AllocModel();

            highPolyModel.PartialInitFromFile(source);

            if (highPolyModel.IsDefaultModel()) {
                common.Error("failed to load %s", source);
            }

            // combine the high poly model into a single polyset
            if (highPolyModel.NumSurfaces() != 1) {
                highPolyModel = CombineModelSurfaces(highPolyModel);
            }

            // create normals if not present in file
            final modelSurface_s surf = highPolyModel.Surface(0);
            mesh = surf.geometry;

            // bound the entire file
            R_BoundTriSurf(mesh);
            bounds = mesh.bounds;

            SaveWindow();
            ResizeWindow(width, height);

            // for small images, the viewport may be less than the minimum window
            qglViewport(0, 0, width, height);

            qglEnable(GL_CULL_FACE);
            qglCullFace(GL_FRONT);
            qglDisable(GL_STENCIL_TEST);
            qglDisable(GL_SCISSOR_TEST);
            qglDisable(GL_ALPHA_TEST);
            qglDisable(GL_BLEND);
            qglEnable(GL_DEPTH_TEST);
            qglDisable(GL_TEXTURE_2D);
            qglDepthMask(itob(GL_TRUE));
            qglDepthFunc(GL_LEQUAL);

            qglColor3f(1, 1, 1);

            qglMatrixMode(GL_PROJECTION);
            qglLoadIdentity();
            qglOrtho(bounds.oGet(0, 0), bounds.oGet(1, 0), bounds.oGet(0, 2),
                    bounds.oGet(1, 2), -(bounds.oGet(0, 1) - 1), -(bounds.oGet(1, 1) + 1));

            qglMatrixMode(GL_MODELVIEW);
            qglLoadIdentity();

            // flat maps are automatically anti-aliased
            idStr filename;
            int j, k, c = 0;
            ByteBuffer buffer;
            int[] sumBuffer, colorSumBuffer;
            boolean flat;
            int sample;

            sumBuffer = new int[width * height * 4 * 4];// Mem_Alloc(width * height * 4 * 4);
//	memset( sumBuffer, 0, width * height * 4 * 4 );
            buffer = Nio.newByteBuffer(width * height * 4);// Mem_Alloc(width * height * 4);

            colorSumBuffer = new int[width * height * 4 * 4];// Mem_Alloc(width * height * 4 * 4);
//	memset( sumBuffer, 0, width * height * 4 * 4 );

            flat = false;
//flat = true;

            for (sample = 0; sample < 16; sample++) {
                float xOff, yOff;

                xOff = (((sample & 3) / 4.0f) * (bounds.oGet(1, 0) - bounds.oGet(0, 0))) / width;//TODO:loss of precision, float instead of double.
                yOff = (((sample / 4) / 4.0f) * (bounds.oGet(1, 2) - bounds.oGet(0, 2))) / height;

                for (int colorPass = 0; colorPass < 2; colorPass++) {
                    qglClearColor(0.5f, 0.5f, 0.5f, 0);
                    qglClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                    qglBegin(GL_TRIANGLES);
                    for (i = 0; i < highPolyModel.NumSurfaces(); i++) {
                        final modelSurface_s surf2 = highPolyModel.Surface(i);

                        mesh = surf2.geometry;

                        if (colorPass != 0) {
                            // just render the surface color for artist visualization
                            for (j = 0; j < mesh.getIndexes().getNumValues(); j += 3) {
                                for (k = 0; k < 3; k++) {
                                    int v;
                                    float[] a;

                                    v = mesh.getIndexes().getValues().get(j + k);
                                    qglColor3ubv(mesh.verts[v].getColor());
                                    a = mesh.verts[v].xyz.ToFloatPtr();
                                    qglVertex3f(a[0] + xOff, a[2] + yOff, a[1]);
                                }
                            }
                        } else {
                            // render as normal map
                            // we can either flat shade from the plane,
                            // or smooth shade from the vertex normals
                            for (j = 0; j < mesh.getIndexes().getNumValues(); j += 3) {
                                if (flat) {
                                    final idPlane plane = new idPlane();
                                    idVec3 a2, b2, c2;
                                    int v1, v2, v3;

                                    v1 = mesh.getIndexes().getValues().get(j + 0);
                                    v2 = mesh.getIndexes().getValues().get(j + 1);
                                    v3 = mesh.getIndexes().getValues().get(j + 2);

                                    a2 = mesh.verts[ v1].xyz;
                                    b2 = mesh.verts[ v2].xyz;
                                    c2 = mesh.verts[ v3].xyz;

                                    plane.FromPoints(a2, b2, c2);

                                    // NULLNORMAL is used by the artists to force an area to reflect no
                                    // light at all
                                    if ((surf2.shader.GetSurfaceFlags() & SURF_NULLNORMAL) != 0) {
                                        qglColor3f(0.5f, 0.5f, 0.5f);
                                    } else {
                                        qglColor3f(0.5f + (0.5f * plane.oGet(0)), 0.5f - (0.5f * plane.oGet(2)), 0.5f - (0.5f * plane.oGet(1)));
                                    }

//							qglVertex3f( (*a2)[0] + xOff, (*a2)[2] + yOff, (*a2)[1] );//TODO:check this pointer cast thing
//							qglVertex3f( (*b2)[0] + xOff, (*b2)[2] + yOff, (*b2)[1] );
//							qglVertex3f( (*c2)[0] + xOff, (*c2)[2] + yOff, (*c2)[1] );
                                    qglVertex3f(a2.oGet(0) + xOff, a2.oGet(2) + yOff, a2.oGet(1));
                                    qglVertex3f(b2.oGet(0) + xOff, b2.oGet(2) + yOff, b2.oGet(1));
                                    qglVertex3f(c2.oGet(0) + xOff, c2.oGet(2) + yOff, c2.oGet(1));
                                } else {
                                    for (k = 0; k < 3; k++) {
                                        int v;
                                        float[] n;
                                        float[] a;

                                        v = mesh.getIndexes().getValues().get(j + k);
                                        n = mesh.verts[v].normal.ToFloatPtr();

                                        // NULLNORMAL is used by the artists to force an area to reflect no
                                        // light at all
                                        if ((surf2.shader.GetSurfaceFlags() & SURF_NULLNORMAL) != 0) {
                                            qglColor3f(0.5f, 0.5f, 0.5f);
                                        } else {
                                            // we are going to flip the normal Z direction
                                            qglColor3f(0.5f + (0.5f * n[0]), 0.5f - (0.5f * n[2]), 0.5f - (0.5f * n[1]));
                                        }

                                        a = mesh.verts[v].xyz.ToFloatPtr();
                                        qglVertex3f(a[0] + xOff, a[2] + yOff, a[1]);
                                    }
                                }
                            }
                        }
                    }

                    qglEnd();
                    qglFlush();
                    GLimp_SwapBuffers();
                    qglReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

                    if (colorPass != 0) {
                        // add to the sum buffer
                        for (i = 0; i < c; i++) {
                            colorSumBuffer[(i * 4) + 0] += buffer.get((i * 4) + 0);
                            colorSumBuffer[(i * 4) + 1] += buffer.get((i * 4) + 1);
                            colorSumBuffer[(i * 4) + 2] += buffer.get((i * 4) + 2);
                            colorSumBuffer[(i * 4) + 3] += buffer.get((i * 4) + 3);
                        }
                    } else {
                        // normalize
                        c = width * height;
                        for (i = 0; i < c; i++) {
                            final idVec3 v = new idVec3();

                            v.oSet(0, (buffer.get((i * 4) + 0) - 128) / 127.0f);
                            v.oSet(1, (buffer.get((i * 4) + 1) - 128) / 127.0f);
                            v.oSet(2, (buffer.get((i * 4) + 2) - 128) / 127.0f);

                            v.Normalize();

                            buffer.put((i * 4) + 0, (byte) (128 + (127 * v.oGet(0))));
                            buffer.put((i * 4) + 1, (byte) (128 + (127 * v.oGet(1))));
                            buffer.put((i * 4) + 2, (byte) (128 + (127 * v.oGet(2))));
                        }

                        // outline into non-drawn areas
                        for (i = 0; i < 8; i++) {
                            OutlineNormalMap(buffer, width, height, 128, 128, 128);
                        }

                        // add to the sum buffer
                        for (i = 0; i < c; i++) {
                            sumBuffer[(i * 4) + 0] += buffer.get((i * 4) + 0);
                            sumBuffer[(i * 4) + 1] += buffer.get((i * 4) + 1);
                            sumBuffer[(i * 4) + 2] += buffer.get((i * 4) + 2);
                            sumBuffer[(i * 4) + 3] += buffer.get((i * 4) + 3);
                        }
                    }
                }
            }

            c = width * height;

            // save out the color map
            for (i = 0; i < c; i++) {
                buffer.put((i * 4) + 0, (byte) (colorSumBuffer[(i * 4) + 0] / 16));
                buffer.put((i * 4) + 1, (byte) (colorSumBuffer[(i * 4) + 1] / 16));
                buffer.put((i * 4) + 2, (byte) (colorSumBuffer[(i * 4) + 2] / 16));
                buffer.put((i * 4) + 3, (byte) (colorSumBuffer[(i * 4) + 3] / 16));
            }
            filename = new idStr(source);
            filename.StripFileExtension();
            filename.Append("_color.tga");
            R_VerticalFlip(buffer, width, height);
            R_WriteTGA(filename, buffer, width, height);

            // save out the local map
            // scale the sum buffer back down to the sample buffer
            // we allow this to denormalize
            for (i = 0; i < c; i++) {
                buffer.put((i * 4) + 0, (byte) (sumBuffer[(i * 4) + 0] / 16));
                buffer.put((i * 4) + 1, (byte) (sumBuffer[(i * 4) + 1] / 16));
                buffer.put((i * 4) + 2, (byte) (sumBuffer[(i * 4) + 2] / 16));
                buffer.put((i * 4) + 3, (byte) (sumBuffer[(i * 4) + 3] / 16));
            }

            filename.oSet(source);
            filename.StripFileExtension();
            filename.Append("_local.tga");
            common.Printf("writing %s (%d,%d)\n", filename, width, height);
            R_VerticalFlip(buffer, width, height);
            R_WriteTGA(filename, buffer, width, height);

            // free the model
            renderModelManager.FreeModel(highPolyModel);

            // free our work buffer
            buffer = null;//Mem_Free(buffer);
            sumBuffer = null;//Mem_Free(sumBuffer);
            colorSumBuffer = null;//Mem_Free(colorSumBuffer);

            RestoreWindow();

            // stop updating the screen as we print
            common.SetRefreshOnPrint(false);

            common.Error("Completed.");
        }
    }
}
