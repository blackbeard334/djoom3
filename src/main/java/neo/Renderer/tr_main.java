package neo.Renderer;

import static neo.Renderer.RenderSystem.R_AddDrawViewCmd;
import static neo.Renderer.RenderSystem.R_ClearCommandChain;
import static neo.Renderer.RenderSystem_init.r_jitter;
import static neo.Renderer.RenderSystem_init.r_subviewOnly;
import static neo.Renderer.RenderSystem_init.r_useCulling;
import static neo.Renderer.RenderSystem_init.r_useDepthBoundsTest;
import static neo.Renderer.RenderSystem_init.r_useFrustumFarDistance;
import static neo.Renderer.RenderSystem_init.r_znear;
import static neo.Renderer.tr_light.R_AddLightSurfaces;
import static neo.Renderer.tr_light.R_AddModelSurfaces;
import static neo.Renderer.tr_light.R_RemoveUnecessaryViewLights;
import static neo.Renderer.tr_local.frameData;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_trisurf.R_FreeDeferredTriSurfs;
import static neo.framework.Common.common;
import static neo.framework.Session.session;
import static neo.idlib.Lib.MAX_WORLD_SIZE;
import static neo.idlib.Lib.colorBlue;
import static neo.idlib.Lib.colorCyan;
import static neo.idlib.Lib.colorGreen;
import static neo.idlib.Lib.colorMagenta;
import static neo.idlib.Lib.colorPurple;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Lib.colorWhite;
import static neo.idlib.Lib.colorYellow;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Vector.DotProduct;
import static neo.idlib.math.Vector.VectorSubtract;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import neo.TempDump.TODO_Exception;
import neo.Renderer.RenderWorld_local.idRenderWorldLocal;
import neo.Renderer.tr_local.drawSurf_s;
import neo.Renderer.tr_local.frameData_t;
import neo.Renderer.tr_local.frameMemoryBlock_s;
import neo.Renderer.tr_local.idScreenRect;
import neo.Renderer.tr_local.viewDef_s;
import neo.Renderer.tr_local.viewEntity_s;
import neo.Renderer.tr_local.viewLight_s;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Matrix.idMat3;
import neo.open.Nio;

/**
 *
 */
public class tr_main {

    //====================================================================

    /*
     ======================
     R_ScreenRectFromViewFrustumBounds
     ======================
     */
    public static idScreenRect R_ScreenRectFromViewFrustumBounds(final idBounds bounds) {
        final idScreenRect screenRect = new idScreenRect();

        screenRect.x1 = idMath.FtoiFast(0.5f * (1.0f - bounds.oGet(1).y) * (tr.viewDef.viewport.x2 - tr.viewDef.viewport.x1));
        screenRect.x2 = idMath.FtoiFast(0.5f * (1.0f - bounds.oGet(0).y) * (tr.viewDef.viewport.x2 - tr.viewDef.viewport.x1));
        screenRect.y1 = idMath.FtoiFast(0.5f * (1.0f + bounds.oGet(0).z) * (tr.viewDef.viewport.y2 - tr.viewDef.viewport.y1));
        screenRect.y2 = idMath.FtoiFast(0.5f * (1.0f + bounds.oGet(1).z) * (tr.viewDef.viewport.y2 - tr.viewDef.viewport.y1));

        if (r_useDepthBoundsTest.GetInteger() != 0) {
            float[] zmin = {screenRect.zmin}, zmax = {screenRect.zmax};
            R_TransformEyeZToWin(-bounds.oGet(0).x, tr.viewDef.projectionMatrix, zmin);
            R_TransformEyeZToWin(-bounds.oGet(1).x, tr.viewDef.projectionMatrix, zmax);
            screenRect.zmin = zmin[0];
            screenRect.zmax = zmax[0];
        }

        return screenRect;
    }

    /*
     ======================
     R_ShowColoredScreenRect
     ======================
     */
    final static idVec4[] colors/*[]*/ = {colorRed, colorGreen, colorBlue, colorYellow, colorMagenta, colorCyan, colorWhite, colorPurple};

    public static void R_ShowColoredScreenRect(final idScreenRect rect, int colorIndex) {
        if (!rect.IsEmpty()) {
            tr.viewDef.renderWorld.DebugScreenRect(colors[colorIndex & 7], rect, tr.viewDef);
        }
    }

    /*
     ====================
     R_ToggleSmpFrame
     ====================
     */
    public static void R_ToggleSmpFrame() {
        if (RenderSystem_init.r_lockSurfaces.GetBool()) {
            return;
        }
        R_FreeDeferredTriSurfs(frameData);

        // clear frame-temporary data
        frameData_t frame;
        frameMemoryBlock_s block;

        // update the highwater mark
        R_CountFrameData();

        frame = frameData;

        // reset the memory allocation to the first block
        frame.alloc = frame.memory;

        // clear all the blocks
        for (block = frame.memory; block != null; block = block.next) {
            block.used = 0;
        }

        R_ClearCommandChain();
    }
//=====================================================
    static final int MEMORY_BLOCK_SIZE = 0x100000;

    /*
     =====================
     R_ShutdownFrameData
     =====================
     */
    public static void R_ShutdownFrameData() {
        frameData_t frame;
        frameMemoryBlock_s block;

        // free any current data
        frame = frameData;
        if (null == frame) {
            return;
        }

        R_FreeDeferredTriSurfs(frame);

        frameMemoryBlock_s nextBlock;
        for (block = frame.memory; block != null; block = nextBlock) {
            nextBlock = block.next;
            block = null;
        }
        frame = null;
        frameData = null;
    }

    /*
     =====================
     R_InitFrameData
     =====================
     */
    public static void R_InitFrameData() {
        int size;
        frameData_t frame;
        frameMemoryBlock_s block;

        R_ShutdownFrameData();

        frameData = new frameData_t();// Mem_ClearedAlloc(sizeof(frameData));
        frame = frameData;
        size = MEMORY_BLOCK_SIZE;
        block = new frameMemoryBlock_s();// Mem_Alloc(size /*+ sizeof( *block )*/);
        if (null == block) {
            common.FatalError("R_InitFrameData: Mem_Alloc() failed");
        }
        block.size = size;
        block.used = 0;
        block.next = null;
        frame.memory = block;
        frame.memoryHighwater = 0;

        R_ToggleSmpFrame();
    }

    /*
     ================
     R_CountFrameData
     ================
     */
    @Deprecated
    public static int R_CountFrameData() {
        frameData_t frame;
        frameMemoryBlock_s block;
        int count;

        count = 0;
        frame = frameData;
        for (block = frame.memory; block != null; block = block.next) {
            count += block.used;
            if (block == frame.alloc) {
                break;
            }
        }

        // note if this is a new highwater mark
        if (count > frame.memoryHighwater) {
            frame.memoryHighwater = count;
        }

        return count;
    }

    /*
     =================
     R_StaticAlloc
     =================
     */
    @Deprecated
    public static Object R_StaticAlloc(int bytes) {
        throw new UnsupportedOperationException();
//        Object buf;
//
//        tr.pc.c_alloc++;
//
//        tr.staticAllocCount += bytes;
//
//        buf = Mem_Alloc(bytes);
//
//        // don't exit on failure on zero length allocations since the old code didn't
//        if (null == buf && (bytes != 0)) {
//            common.FatalError("R_StaticAlloc failed on %d bytes", bytes);
//        }
//        return buf;
    }

    /*
     =================
     R_ClearedStaticAlloc
     =================
     */
    @Deprecated
    private static <T> T[] R_ClearedStaticAlloc(int length, Class<T> clazz) {
        final T[] array = (T[]) Array.newInstance(clazz, length);

        for (int a = 0; a < length; a++) {
            try {
                array[a] = (T) clazz.getConstructor().newInstance();
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new TODO_Exception();
//                Logger.getLogger(tr_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return array;
    }

    /*
     =================
     R_StaticFree
     =================
     */
    @Deprecated
    public static void R_StaticFree(Object data) {
//        tr.pc.c_free++;
//        Mem_Free(data);
        throw new UnsupportedOperationException();
    }

    /*
     ================
     R_FrameAlloc

     This data will be automatically freed when the
     current frame's back end completes.

     This should only be called by the front end.  The
     back end shouldn't need to allocate memory.

     If we passed smpFrame in, the back end could
     alloc memory, because it will always be a
     different frameData than the front end is using.

     All temporary data, like dynamic tesselations
     and local spaces are allocated here.

     The memory will not move, but it may not be
     contiguous with previous allocations even
     from this frame.

     The memory is NOT zero filled.
     Should part of this be inlined in a macro?
     ================
     */
    @Deprecated
    public static Object R_FrameAlloc(int bytes) {
//        frameData_t frame;
//        frameMemoryBlock_s block;
//        Object buf;
//
//        bytes = (bytes + 16) & ~15;
//        // see if it can be satisfied in the current block
//        frame = frameData;
//        block = frame.alloc;
//
//        if (block.size - block.used >= bytes) {
//            buf = block.base + block.used;
//            block.used += bytes;
//            return buf;
//        }
//
//        // advance to the next memory block if available
//        block = block.next;
//        // create a new block if we are at the end of
//        // the chain
//        if (null == block) {
//            int size;
//
//            size = MEMORY_BLOCK_SIZE;
//            block = (frameMemoryBlock_s) Mem_Alloc(size /*+ sizeof( *block )*/);
//            if (null == block) {
//                common.FatalError("R_FrameAlloc: Mem_Alloc() failed");
//            }
//            block.size = size;
//            block.used = 0;
//            block.next = null;
//            frame.alloc.next = block;
//        }
//
//        // we could fix this if we needed to...
//        if (bytes > block.size) {
//            common.FatalError("R_FrameAlloc of %d exceeded MEMORY_BLOCK_SIZE",
//                    bytes);
//        }
//
//        frame.alloc = block;
//
//        block.used = bytes;
//
//        return block.base;
        throw new UnsupportedOperationException();
    }

    /*
     ==================
     R_ClearedFrameAlloc
     ==================
     */
    @Deprecated
    public static Object R_ClearedFrameAlloc(int bytes) {
//        Object r;
//
//        r = R_FrameAlloc(bytes);
//        SIMDProcessor.Memset(r, 0, bytes);
//        return r;
        throw new UnsupportedOperationException();
    }


    /*
     ==================
     R_FrameFree

     This does nothing at all, as the frame data is reused every frame
     and can only be stack allocated.

     The only reason for it's existance is so functions that can
     use either static or frame memory can set function pointers
     to both alloc and free.
     ==================
     */
    public static void R_FrameFree(Object data) {
    }

//==========================================================================
    public static void R_AxisToModelMatrix(final idMat3 axis, final idVec3 origin, float[] modelMatrix/*[16]*/) {
        modelMatrix[ 0] = axis.oGet(0, 0);
        modelMatrix[ 4] = axis.oGet(1, 0);
        modelMatrix[ 8] = axis.oGet(2, 0);
        modelMatrix[12] = origin.oGet(0);

        modelMatrix[ 1] = axis.oGet(0, 1);
        modelMatrix[ 5] = axis.oGet(1, 1);
        modelMatrix[ 9] = axis.oGet(2, 1);
        modelMatrix[13] = origin.oGet(1);

        modelMatrix[ 2] = axis.oGet(0, 2);
        modelMatrix[ 6] = axis.oGet(1, 2);
        modelMatrix[10] = axis.oGet(2, 2);
        modelMatrix[14] = origin.oGet(2);

        modelMatrix[ 3] = 0;
        modelMatrix[ 7] = 0;
        modelMatrix[11] = 0;
        modelMatrix[15] = 1;
    }

    // FIXME: these assume no skewing or scaling transforms
    public static idVec3 R_LocalPointToGlobal(final float[] modelMatrix/*[16]*/, final idVec3 in) {
        idVec3 out;

// if (MACOS_X && __i386__){
        // __m128 m0, m1, m2, m3;
        // __m128 in0, in1, in2;
        // float i0,i1,i2;
        // i0 = in[0];
        // i1 = in[1];
        // i2 = in[2];
        // m0 = _mm_loadu_ps(&modelMatrix[0]);
        // m1 = _mm_loadu_ps(&modelMatrix[4]);
        // m2 = _mm_loadu_ps(&modelMatrix[8]);
        // m3 = _mm_loadu_ps(&modelMatrix[12]);
        // in0 = _mm_load1_ps(&i0);
        // in1 = _mm_load1_ps(&i1);
        // in2 = _mm_load1_ps(&i2);
        // m0 = _mm_mul_ps(m0, in0);
        // m1 = _mm_mul_ps(m1, in1);
        // m2 = _mm_mul_ps(m2, in2);
        // m0 = _mm_add_ps(m0, m1);
        // m0 = _mm_add_ps(m0, m2);
        // m0 = _mm_add_ps(m0, m3);
        // _mm_store_ss(&out[0], m0);
        // m1 = (__m128) _mm_shuffle_epi32((__m128i)m0, 0x55);
        // _mm_store_ss(&out[1], m1);
        // m2 = _mm_movehl_ps(m2, m0);
        // _mm_store_ss(&out[2], m2);
// }else
        {
            out = new idVec3(
                    (in.oGet(0) * modelMatrix[0] + in.oGet(1) * modelMatrix[4] + in.oGet(2) * modelMatrix[ 8] + modelMatrix[12]),
                    (in.oGet(0) * modelMatrix[1] + in.oGet(1) * modelMatrix[5] + in.oGet(2) * modelMatrix[ 9] + modelMatrix[13]),
                    (in.oGet(0) * modelMatrix[2] + in.oGet(1) * modelMatrix[6] + in.oGet(2) * modelMatrix[10] + modelMatrix[14])
            );
        }
        return out;
    }

    public static void R_PointTimesMatrix(final float[] modelMatrix/*[16]*/, final idVec4 in, idVec4 out) {
        out.oSet(0, in.oGet(0) * modelMatrix[0] + in.oGet(1) * modelMatrix[4] + in.oGet(2) * modelMatrix[ 8] + modelMatrix[12]);
        out.oSet(1, in.oGet(0) * modelMatrix[1] + in.oGet(1) * modelMatrix[5] + in.oGet(2) * modelMatrix[ 9] + modelMatrix[13]);
        out.oSet(2, in.oGet(0) * modelMatrix[2] + in.oGet(1) * modelMatrix[6] + in.oGet(2) * modelMatrix[10] + modelMatrix[14]);
        out.oSet(3, in.oGet(0) * modelMatrix[3] + in.oGet(1) * modelMatrix[7] + in.oGet(2) * modelMatrix[11] + modelMatrix[15]);
    }

    public static void R_GlobalPointToLocal(final float[] modelMatrix/*[16]*/, final idVec3 in, idVec out) {
        float[] temp = new float[4];

        VectorSubtract(in.ToFloatPtr(), Arrays.copyOfRange(modelMatrix, 12, 16), temp);

        out.oSet(0, DotProduct(temp, modelMatrix));
        out.oSet(1, DotProduct(temp, Arrays.copyOfRange(modelMatrix, 4, 8)));
        out.oSet(2, DotProduct(temp, Arrays.copyOfRange(modelMatrix, 8, 12)));
    }

    public static void R_GlobalPointToLocal(final float[] modelMatrix/*[16]*/, final idVec3 in, float[] out) {
        float[] temp = new float[4];

        VectorSubtract(in.ToFloatPtr(), Arrays.copyOfRange(modelMatrix, 12, 16), temp);

        out[0] = DotProduct(temp, modelMatrix);
        out[1] = DotProduct(temp, Arrays.copyOfRange(modelMatrix, 4, 8));
        out[2] = DotProduct(temp, Arrays.copyOfRange(modelMatrix, 8, 12));
    }

    public static void R_GlobalPointToLocal(final float[] modelMatrix/*[16]*/, final idVec3 in, FloatBuffer out) {
        float[] temp = new float[4];

        VectorSubtract(in.ToFloatPtr(), Arrays.copyOfRange(modelMatrix, 12, 16), temp);

        out.put(0, DotProduct(temp, modelMatrix));
        out.put(1, DotProduct(temp, Arrays.copyOfRange(modelMatrix, 4, 8)));
        out.put(2, DotProduct(temp, Arrays.copyOfRange(modelMatrix, 8, 12)));
    }

    public static void R_LocalVectorToGlobal(final float[] modelMatrix/*[16]*/, final idVec3 in, idVec3 out) {
        out.oSet(0, in.oGet(0) * modelMatrix[0] + in.oGet(1) * modelMatrix[4] + in.oGet(2) * modelMatrix[ 8]);
        out.oSet(1, in.oGet(0) * modelMatrix[1] + in.oGet(1) * modelMatrix[5] + in.oGet(2) * modelMatrix[ 9]);
        out.oSet(2, in.oGet(0) * modelMatrix[2] + in.oGet(1) * modelMatrix[6] + in.oGet(2) * modelMatrix[10]);
    }

    public static void R_GlobalVectorToLocal(final float[] modelMatrix/*[16]*/, final idVec3 in, idVec3 out) {
        out.oSet(0, DotProduct(in.ToFloatPtr(), modelMatrix));
        out.oSet(1, DotProduct(in.ToFloatPtr(), Arrays.copyOfRange(modelMatrix, 4, 8)));
        out.oSet(2, DotProduct(in.ToFloatPtr(), Arrays.copyOfRange(modelMatrix, 8, 12)));
    }

    public static void R_GlobalPlaneToLocal(final float[] modelMatrix/*[16]*/, final idPlane in, idPlane out) {
        out.oSet(0, DotProduct(in.ToFloatPtr(), modelMatrix));
        out.oSet(1, DotProduct(in.ToFloatPtr(), Arrays.copyOfRange(modelMatrix, 4, 8)));
        out.oSet(2, DotProduct(in.ToFloatPtr(), Arrays.copyOfRange(modelMatrix, 8, 12)));
        out.oSet(3, in.oGet(3) + modelMatrix[12] * in.oGet(0) + modelMatrix[13] * in.oGet(1) + modelMatrix[14] * in.oGet(2));
    }

    public static void R_LocalPlaneToGlobal(final float[] modelMatrix/*[16]*/, final idPlane in, idPlane out) {
        final float offset;

        R_LocalVectorToGlobal(modelMatrix, in.Normal(), out.Normal());

        offset = modelMatrix[12] * out.oGet(0) + modelMatrix[13] * out.oGet(1) + modelMatrix[14] * out.oGet(2);
        out.oSet(3, in.oGet(3) - offset);
    }

    // transform Z in eye coordinates to window coordinates
    public static void R_TransformEyeZToWin(float src_z, final FloatBuffer projectionMatrix, float[] dst_z) {
        final float clip_z, clip_w;

        // projection
        clip_z = (src_z * projectionMatrix.get(2 + (2 * 4))) + projectionMatrix.get(2 + (3 * 4));
        clip_w = (src_z * projectionMatrix.get(3 + (2 * 4))) + projectionMatrix.get(3 + (3 * 4));

        if (clip_w <= 0.0f) {
            dst_z[0] = 0.0f;					// clamp to near plane
        } else {
            dst_z[0] = clip_z / clip_w;
            dst_z[0] = (dst_z[0] * 0.5f) + 0.5f;	// convert to window coords
        }
    }

    /*
     =================
     R_RadiusCullLocalBox

     A fast, conservative center-to-corner culling test
     Returns true if the box is outside the given global frustum, (positive sides are out)
     =================
     */
    public static boolean R_RadiusCullLocalBox(final idBounds bounds, final float[] modelMatrix/*[16]*/, int numPlanes, final idPlane[] planes) {
        int i;
        float d;
        idVec3 worldOrigin;
        float worldRadius;
        idPlane frust;

        if (r_useCulling.GetInteger() == 0) {
            return false;
        }

        // transform the surface bounds into world space
        final idVec3 localOrigin = (bounds.oGet(0).oPlus(bounds.oGet(1))).oMultiply(0.5f);

        worldOrigin = R_LocalPointToGlobal(modelMatrix, localOrigin);

        worldRadius = (bounds.oGet(0).oMinus(localOrigin)).Length();	// FIXME: won't be correct for scaled objects

        for (i = 0; i < numPlanes; i++) {
            frust = planes[i];
            d = frust.Distance(worldOrigin);
            if (d > worldRadius) {                
                return true;	// culled
            }
        }

        return false;		// not culled
    }

    /*
     =================
     R_CornerCullLocalBox

     Tests all corners against the frustum.
     Can still generate a few false positives when the box is outside a corner.
     Returns true if the box is outside the given global frustum, (positive sides are out)
     =================
     */    private static int DBG_R_CornerCullLocalBox = 0;
    public static boolean R_CornerCullLocalBox(final idBounds bounds, final float[] modelMatrix/*[16]*/, int numPlanes, final idPlane[] planes) {
        int i, j;
        final idVec3[] transformed = idVec3.generateArray(8);
        final float[] dists = new float[8];
        final idVec3 v = new idVec3();
        idPlane frust;
        
        DBG_R_CornerCullLocalBox++;

        // we can disable box culling for experimental timing purposes
        if (RenderSystem_init.r_useCulling.GetInteger() < 2) {
            return false;
        }

        // transform into world space
        for (i = 0; i < 8; i++) {
            v.oSet(0, bounds.oGet((i >> 0) & 1, 0));
            v.oSet(1, bounds.oGet((i >> 1) & 1, 1));
            v.oSet(2, bounds.oGet((i >> 2) & 1, 2));

            transformed[i] = R_LocalPointToGlobal(modelMatrix, v);
        }

        // check against frustum planes
        for (i = 0; i < numPlanes; i++) {
            frust = planes[i];
            for (j = 0; j < 8; j++) {
                dists[j] = frust.Distance(transformed[j]);
                if (dists[j] < 0) {
                    break;
                }
            }
            if (j == 8) {
//                System.out.println("<<<<<<<<<<< " + DBG_R_CornerCullLocalBox);
//                System.out.println(">>>>>>>>>>> " + Arrays.toString(transformed));
//                System.out.println(">>>>>>>>>>> " + Arrays.toString(dists));
//                System.out.println("<<<<<<<<<<< " + DBG_R_CornerCullLocalBox);
                // all points were behind one of the planes
                tr.pc.c_box_cull_out++;
                return true;
            }
        }
//        System.out.println("<<<<<<<<<<< " + DBG_R_CornerCullLocalBox);
//        System.out.println(">>>>>>>>>>> " + Arrays.toString(transformed));
//        System.out.println(">>>>>>>>>>> " + Arrays.toString(dists));
//        System.out.println("<<<<<<<<<<< " + DBG_R_CornerCullLocalBox);

        tr.pc.c_box_cull_in++;

        return false;		// not culled
    }

    /*
     =================
     R_CullLocalBox

     Performs quick test before expensive test
     Returns true if the box is outside the given global frustum, (positive sides are out)
     =================
     */
    public static boolean R_CullLocalBox(final idBounds bounds, final float[] modelMatrix/*[16]*/, int numPlanes, final idPlane[] planes) {
        if (R_RadiusCullLocalBox(bounds, modelMatrix, numPlanes, planes)) {
            return true;
        }
        return R_CornerCullLocalBox(bounds, modelMatrix, numPlanes, planes);
    }

    /*
     ==========================
     R_TransformModelToClip
     ==========================
     */
    public static void R_TransformModelToClip(final idVec3 src, final FloatBuffer modelMatrix, final FloatBuffer projectionMatrix, idPlane eye, idPlane dst) {
        int i, j;

        for (i = 0; i < 4; i++) {
            j = 0;
            eye.oSet(i,
                    (src.oGet(j) * modelMatrix.get(i + (j * 4)))
                    // increment j
                    + (src.oGet(++j) * modelMatrix.get(i + (j * 4)))
                    // increment j
                    + (src.oGet(++j) * modelMatrix.get(i + (j * 4)))
                    // increment j
                    + (modelMatrix.get(i + (++j * 4))));
        }

        for (i = 0; i < 4; i++) {
            j = 0;
            dst.oSet(i,
                    (eye.oGet(j) * projectionMatrix.get(i + (j * 4)))
                    // increment j
                    + (eye.oGet(++j) * projectionMatrix.get(i + (j * 4)))
                    // increment j
                    + (eye.oGet(++j) * projectionMatrix.get(i + (j * 4)))
                    // increment j
                    + (eye.oGet(++j) * projectionMatrix.get(i + (j * 4))));
        }
    }

    /*
     ==========================
     R_GlobalToNormalizedDeviceCoordinates

     -1 to 1 range in x, y, and z
     ==========================
     */
    public static void R_GlobalToNormalizedDeviceCoordinates(final idVec3 global, idVec3 ndc) {
    	int j;
    	idPlane view = new idPlane();
    	final idPlane clip = new idPlane();
        FloatBuffer modelViewMatrix;
        FloatBuffer projectionMatrix;

        // _D3XP added work on primaryView when no viewDef
        if (null == tr.viewDef) {

            modelViewMatrix = tr.primaryView.worldSpace.modelViewMatrix;
            projectionMatrix = tr.primaryView.projectionMatrix;

        } else {

            modelViewMatrix = tr.viewDef.worldSpace.modelViewMatrix;
            projectionMatrix = tr.viewDef.projectionMatrix;

        }

		for (int i = 0; i < 4; i++) {
			j = 0;
			view.oSet(i, (global.oGet(j) * modelViewMatrix.get(i + (j * 4)))
					// increment j
					+ (global.oGet(++j) * modelViewMatrix.get(i + (j * 4)))
					// increment j
					+ (global.oGet(++j) * modelViewMatrix.get(i + (j * 4)))
					// increment j
					+ (global.oGet(++j) * modelViewMatrix.get(i + (j * 4))));
		}

		for (int i = 0; i < 4; i++) {
			j = 0;
			clip.oSet(i, (view.oGet(j) * projectionMatrix.get(i + (j * 4)))
					// increment j
					+ (view.oGet(++j) * projectionMatrix.get(i + (j * 4)))
					// increment j
					+ (view.oGet(++j) * projectionMatrix.get(i + (j * 4)))
					// increment j
					+ (view.oGet(++j) * projectionMatrix.get(i + (j * 4))));
		}

        ndc.oSet(0, clip.oGet(0) / clip.oGet(3));
        ndc.oSet(1, clip.oGet(1) / clip.oGet(3));
        ndc.oSet(2, (clip.oGet(2) + clip.oGet(3)) / (2 * clip.oGet(3)));
    }

    /*
     ==========================
     R_TransformClipToDevice

     Clip to normalized device coordinates
     ==========================
     */
    public static void R_TransformClipToDevice(final idPlane clip, final viewDef_s view, idVec3 normalized) {
        normalized.oSet(0, clip.oGet(0) / clip.oGet(3));
        normalized.oSet(1, clip.oGet(1) / clip.oGet(3));
        normalized.oSet(2, clip.oGet(2) / clip.oGet(3));
    }

    /*
     ==========================
     myGlMultMatrix
     ==========================
     */
    public static void myGlMultMatrix(final FloatBuffer a/*[16]*/, final FloatBuffer b/*[16]*/, FloatBuffer out/*[16]*/) {
        if (false) { //if (TempDump.isDeadCodeTrue()) {
//            int i, j;
//
//            for (i = 0; i < 4; i++) {
//                for (j = 0; j < 4; j++) {
//                    out[ i * 4 + j] =
//                            a[ i * 4 + 0] * b[ 0 * 4 + j]
//                            + a[ i * 4 + 1] * b[ 1 * 4 + j]
//                            + a[ i * 4 + 2] * b[ 2 * 4 + j]
//                            + a[ i * 4 + 3] * b[ 3 * 4 + j];
//                }
//            }
        } else {
            out.put((0 * 4) + 0, (a.get((0 * 4) + 0) * b.get((0 * 4) + 0)) + (a.get((0 * 4) + 1) * b.get((1 * 4) + 0)) + (a.get((0 * 4) + 2) * b.get((2 * 4) + 0)) + (a.get((0 * 4) + 3) * b.get((3 * 4) + 0)));
            out.put((0 * 4) + 1, (a.get((0 * 4) + 0) * b.get((0 * 4) + 1)) + (a.get((0 * 4) + 1) * b.get((1 * 4) + 1)) + (a.get((0 * 4) + 2) * b.get((2 * 4) + 1)) + (a.get((0 * 4) + 3) * b.get((3 * 4) + 1)));
            out.put((0 * 4) + 2, (a.get((0 * 4) + 0) * b.get((0 * 4) + 2)) + (a.get((0 * 4) + 1) * b.get((1 * 4) + 2)) + (a.get((0 * 4) + 2) * b.get((2 * 4) + 2)) + (a.get((0 * 4) + 3) * b.get((3 * 4) + 2)));
            out.put((0 * 4) + 3, (a.get((0 * 4) + 0) * b.get((0 * 4) + 3)) + (a.get((0 * 4) + 1) * b.get((1 * 4) + 3)) + (a.get((0 * 4) + 2) * b.get((2 * 4) + 3)) + (a.get((0 * 4) + 3) * b.get((3 * 4) + 3)));
            out.put((1 * 4) + 0, (a.get((1 * 4) + 0) * b.get((0 * 4) + 0)) + (a.get((1 * 4) + 1) * b.get((1 * 4) + 0)) + (a.get((1 * 4) + 2) * b.get((2 * 4) + 0)) + (a.get((1 * 4) + 3) * b.get((3 * 4) + 0)));
            out.put((1 * 4) + 1, (a.get((1 * 4) + 0) * b.get((0 * 4) + 1)) + (a.get((1 * 4) + 1) * b.get((1 * 4) + 1)) + (a.get((1 * 4) + 2) * b.get((2 * 4) + 1)) + (a.get((1 * 4) + 3) * b.get((3 * 4) + 1)));
            out.put((1 * 4) + 2, (a.get((1 * 4) + 0) * b.get((0 * 4) + 2)) + (a.get((1 * 4) + 1) * b.get((1 * 4) + 2)) + (a.get((1 * 4) + 2) * b.get((2 * 4) + 2)) + (a.get((1 * 4) + 3) * b.get((3 * 4) + 2)));
            out.put((1 * 4) + 3, (a.get((1 * 4) + 0) * b.get((0 * 4) + 3)) + (a.get((1 * 4) + 1) * b.get((1 * 4) + 3)) + (a.get((1 * 4) + 2) * b.get((2 * 4) + 3)) + (a.get((1 * 4) + 3) * b.get((3 * 4) + 3)));
            out.put((2 * 4) + 0, (a.get((2 * 4) + 0) * b.get((0 * 4) + 0)) + (a.get((2 * 4) + 1) * b.get((1 * 4) + 0)) + (a.get((2 * 4) + 2) * b.get((2 * 4) + 0)) + (a.get((2 * 4) + 3) * b.get((3 * 4) + 0)));
            out.put((2 * 4) + 1, (a.get((2 * 4) + 0) * b.get((0 * 4) + 1)) + (a.get((2 * 4) + 1) * b.get((1 * 4) + 1)) + (a.get((2 * 4) + 2) * b.get((2 * 4) + 1)) + (a.get((2 * 4) + 3) * b.get((3 * 4) + 1)));
            out.put((2 * 4) + 2, (a.get((2 * 4) + 0) * b.get((0 * 4) + 2)) + (a.get((2 * 4) + 1) * b.get((1 * 4) + 2)) + (a.get((2 * 4) + 2) * b.get((2 * 4) + 2)) + (a.get((2 * 4) + 3) * b.get((3 * 4) + 2)));
            out.put((2 * 4) + 3, (a.get((2 * 4) + 0) * b.get((0 * 4) + 3)) + (a.get((2 * 4) + 1) * b.get((1 * 4) + 3)) + (a.get((2 * 4) + 2) * b.get((2 * 4) + 3)) + (a.get((2 * 4) + 3) * b.get((3 * 4) + 3)));
            out.put((3 * 4) + 0, (a.get((3 * 4) + 0) * b.get((0 * 4) + 0)) + (a.get((3 * 4) + 1) * b.get((1 * 4) + 0)) + (a.get((3 * 4) + 2) * b.get((2 * 4) + 0)) + (a.get((3 * 4) + 3) * b.get((3 * 4) + 0)));
            out.put((3 * 4) + 1, (a.get((3 * 4) + 0) * b.get((0 * 4) + 1)) + (a.get((3 * 4) + 1) * b.get((1 * 4) + 1)) + (a.get((3 * 4) + 2) * b.get((2 * 4) + 1)) + (a.get((3 * 4) + 3) * b.get((3 * 4) + 1)));
            out.put((3 * 4) + 2, (a.get((3 * 4) + 0) * b.get((0 * 4) + 2)) + (a.get((3 * 4) + 1) * b.get((1 * 4) + 2)) + (a.get((3 * 4) + 2) * b.get((2 * 4) + 2)) + (a.get((3 * 4) + 3) * b.get((3 * 4) + 2)));
            out.put((3 * 4) + 3, (a.get((3 * 4) + 0) * b.get((0 * 4) + 3)) + (a.get((3 * 4) + 1) * b.get((1 * 4) + 3)) + (a.get((3 * 4) + 2) * b.get((2 * 4) + 3)) + (a.get((3 * 4) + 3) * b.get((3 * 4) + 3)));
        }
    }

    /**
     * TBD - delete method after converting float[] to FloatBuffer
     *  
     * @param a
     * @param b
     * @param out
     * 
     * @Deprecated use public static void myGlMultMatrix(final FloatBuffer a, final FloatBuffer b, FloatBuffer out) instead
     */
    public static void myGlMultMatrix(final float[] a/*[16]*/, final float[] b/*[16]*/, FloatBuffer out/*[16]*/) {
    	myGlMultMatrix(Nio.wrap(a), Nio.wrap(b), out);
    }

    /**
     * TBD - delete method after converting float[] to FloatBuffer
     * 
     * @param a
     * @param b
     * @param out
     * 
     * @Deprecated use public static void myGlMultMatrix(final FloatBuffer a, final FloatBuffer b, FloatBuffer out) instead
     */
    public static void myGlMultMatrix(final float[] a/*[16]*/, final FloatBuffer b/*[16]*/, FloatBuffer out/*[16]*/) {
    	myGlMultMatrix(Nio.wrap(a), b, out);
    }

    /**
     * TBD - delete method after converting float[] to FloatBuffer
     * 
     * @param a
     * @param b
     * @param out
     * 
     * @Deprecated use public static void myGlMultMatrix(final FloatBuffer a, final FloatBuffer b, FloatBuffer out) instead
     */
    public static void myGlMultMatrix(final float[] a/*[16]*/, final float[] b/*[16]*/, float[] out/*[16]*/) {
    	myGlMultMatrix(Nio.wrap(a), Nio.wrap(b), out);
    }

    /**
     * TBD - delete method after converting float[] to FloatBuffer
     * 
     * @param a
     * @param b
     * @param out
     * 
     * @Deprecated use public static void myGlMultMatrix(final FloatBuffer a, final FloatBuffer b, FloatBuffer out) instead
     */
    public static void myGlMultMatrix(final float[] a/*[16]*/, final FloatBuffer b/*[16]*/, float[] out/*[16]*/) {
    	myGlMultMatrix(Nio.wrap(a), b, out);
    }

    /**
     * TBD - delete method after converting float[] to FloatBuffer
     * 
     * @param a
     * @param b
     * @param out
     * 
     * @Deprecated use public static void myGlMultMatrix(final FloatBuffer a, final FloatBuffer b, FloatBuffer out) instead
     */
    public static void myGlMultMatrix(final FloatBuffer a/*[16]*/, final float[] b/*[16]*/, float[] out/*[16]*/) {
    	myGlMultMatrix(a, Nio.wrap(b), out);
    }

    /**
     * TBD - delete method after converting float[] to FloatBuffer
     * 
     * @param a
     * @param b
     * @param out
     * 
     * @Deprecated use public static void myGlMultMatrix(final FloatBuffer a, final FloatBuffer b, FloatBuffer out) instead
     */
    public static void myGlMultMatrix(final FloatBuffer a/*[16]*/, final FloatBuffer b/*[16]*/, float[] out/*[16]*/) {
		ByteBuffer bb = ByteBuffer.allocate(16 * Nio.SIZEOF_FLOAT);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		myGlMultMatrix(a, b, fb);
		for (int i = 0; i < out.length; i++) {
			out[i] = fb.get(i);
		}
    }

    /*
     ================
     R_TransposeGLMatrix
     ================
     */
    public static void R_TransposeGLMatrix(final FloatBuffer in, FloatBuffer out/*[16]*/) {
        int i, j;

        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                //out[i * 4 + j] = in[j * 4 + i];
            	out.put(i * 4 + j, in.get(j * 4 + i));
            }
        }
    }

    /*
     =================
     R_SetViewMatrix

     Sets up the world to view matrix for a given viewParm
     =================
     */
    private static final float[] s_flipMatrix/*[16]*/ = {
                // convert from our coordinate system (looking down X)
                // to OpenGL's coordinate system (looking down -Z)
                -0, 0, -1, 0,
                -1, 0, -0, 0,
                -0, 1, -0, 0,
                -0, 0, -0, 1
            };

    public static void R_SetViewMatrix(viewDef_s viewDef) {
        idVec3 origin;
        viewEntity_s world;
        float[] viewerMatrix = new float[16];

        world = viewDef.worldSpace = new viewEntity_s();//memset(world, 0, sizeof(world));

        // the model matrix is an identity
        world.modelMatrix[0 * 4 + 0] = 1;
        world.modelMatrix[1 * 4 + 1] = 1;
        world.modelMatrix[2 * 4 + 2] = 1;

        // transform by the camera placement
        origin = viewDef.renderView.vieworg;

        viewerMatrix[ 0] = viewDef.renderView.viewaxis.oGet(0, 0);
        viewerMatrix[ 4] = viewDef.renderView.viewaxis.oGet(0, 1);
        viewerMatrix[ 8] = viewDef.renderView.viewaxis.oGet(0, 2);
        viewerMatrix[12] = -origin.oGet(0) * viewerMatrix[0] + -origin.oGet(1) * viewerMatrix[4] + -origin.oGet(2) * viewerMatrix[8];

        viewerMatrix[ 1] = viewDef.renderView.viewaxis.oGet(1, 0);
        viewerMatrix[ 5] = viewDef.renderView.viewaxis.oGet(1, 1);
        viewerMatrix[ 9] = viewDef.renderView.viewaxis.oGet(1, 2);
        viewerMatrix[13] = -origin.oGet(0) * viewerMatrix[1] + -origin.oGet(1) * viewerMatrix[5] + -origin.oGet(2) * viewerMatrix[9];

        viewerMatrix[ 2] = viewDef.renderView.viewaxis.oGet(2, 0);
        viewerMatrix[ 6] = viewDef.renderView.viewaxis.oGet(2, 1);
        viewerMatrix[10] = viewDef.renderView.viewaxis.oGet(2, 2);
        viewerMatrix[14] = -origin.oGet(0) * viewerMatrix[2] + -origin.oGet(1) * viewerMatrix[6] + -origin.oGet(2) * viewerMatrix[10];

        viewerMatrix[ 3] = 0;
        viewerMatrix[ 7] = 0;
        viewerMatrix[11] = 0;
        viewerMatrix[15] = 1;

        // convert from our coordinate system (looking down X)
        // to OpenGL's coordinate system (looking down -Z)
        myGlMultMatrix(viewerMatrix, s_flipMatrix, world.modelViewMatrix);
    }
    /*
     ===============
     R_SetupProjection

     This uses the "infinite far z" trick
     ===============
     */
    private static idRandom random;

    public static void R_SetupProjection() {
        float xmin, xmax, ymin, ymax;
        float width, height;
        float zNear;
        float jitterx, jittery;

        // random jittering is usefull when multiple
        // frames are going to be blended together
        // for motion blurred anti-aliasing
        if (r_jitter.GetBool()) {
            jitterx = random.RandomFloat();
            jittery = random.RandomFloat();
        } else {
            jitterx = jittery = 0;
        }

        //
        // set up projection matrix
        //
        zNear = r_znear.GetFloat();
        if (tr.viewDef.renderView.cramZNear) {
            zNear *= 0.25;
        }

        ymax = (float) (zNear * Math.tan(tr.viewDef.renderView.fov_y * idMath.PI / 360.0f));
        ymin = -ymax;

        xmax = (float) (zNear * Math.tan(tr.viewDef.renderView.fov_x * idMath.PI / 360.0f));
        xmin = -xmax;

        width = xmax - xmin;
        height = ymax - ymin;

        jitterx = jitterx * width / (tr.viewDef.viewport.x2 - tr.viewDef.viewport.x1 + 1);
        xmin += jitterx;
        xmax += jitterx;
        jittery = jittery * height / (tr.viewDef.viewport.y2 - tr.viewDef.viewport.y1 + 1);
        ymin += jittery;
        ymax += jittery;

        tr.viewDef.projectionMatrix.put( 0, (2 * zNear) / width);
        tr.viewDef.projectionMatrix.put( 4, 0);
        tr.viewDef.projectionMatrix.put( 8, (xmax + xmin) / width);	// normally 0
        tr.viewDef.projectionMatrix.put(12, 0);

        tr.viewDef.projectionMatrix.put( 1, 0);
        tr.viewDef.projectionMatrix.put( 5, (2 * zNear) / height);
        tr.viewDef.projectionMatrix.put( 9, (ymax + ymin) / height);	// normally 0
        tr.viewDef.projectionMatrix.put(13, 0);

        // this is the far-plane-at-infinity formulation, and
        // crunches the Z range slightly so w=0 vertexes do not
        // rasterize right at the wraparound point
        tr.viewDef.projectionMatrix.put( 2, 0);
        tr.viewDef.projectionMatrix.put( 6, 0);
        tr.viewDef.projectionMatrix.put(10, -0.999f);
        tr.viewDef.projectionMatrix.put(14, -2.0f * zNear);

        tr.viewDef.projectionMatrix.put( 3, 0);
        tr.viewDef.projectionMatrix.put( 7, 0);
        tr.viewDef.projectionMatrix.put(11, -1);
        tr.viewDef.projectionMatrix.put(15, 0);
    }

    /*
     =================
     R_SetupViewFrustum

     Setup that culling frustum planes for the current view
     FIXME: derive from modelview matrix times projection matrix
     =================
     */
    public static void R_SetupViewFrustum() {
        int i;
        float[] xs = {0.0f}, xc = {0.00f};
        float ang;

        ang = (float) (DEG2RAD(tr.viewDef.renderView.fov_x) * 0.5f);
        idMath.SinCos(ang, xs, xc);

        tr.viewDef.frustum[0].oSet(tr.viewDef.renderView.viewaxis.oGet(0).oMultiply(xs[0]).oPlus(tr.viewDef.renderView.viewaxis.oGet(1).oMultiply(xc[0])));
        tr.viewDef.frustum[1].oSet(tr.viewDef.renderView.viewaxis.oGet(0).oMultiply(xs[0]).oMinus(tr.viewDef.renderView.viewaxis.oGet(1).oMultiply(xc[0])));

        ang = (float) (DEG2RAD(tr.viewDef.renderView.fov_y) * 0.5f);
        idMath.SinCos(ang, xs, xc);

        tr.viewDef.frustum[2].oSet(tr.viewDef.renderView.viewaxis.oGet(0).oMultiply(xs[0]).oPlus(tr.viewDef.renderView.viewaxis.oGet(2).oMultiply(xc[0])));
        tr.viewDef.frustum[3].oSet(tr.viewDef.renderView.viewaxis.oGet(0).oMultiply(xs[0]).oMinus(tr.viewDef.renderView.viewaxis.oGet(2).oMultiply(xc[0])));

        // plane four is the front clipping plane
        tr.viewDef.frustum[4].oSet( /* vec3_origin - */tr.viewDef.renderView.viewaxis.oGet(0));

        for (i = 0; i < 5; i++) {
            // flip direction so positive side faces out (FIXME: globally unify this)
            tr.viewDef.frustum[i].oSet(tr.viewDef.frustum[i].Normal().oNegative());
            tr.viewDef.frustum[i].oSet(3, -(tr.viewDef.renderView.vieworg.oMultiply(tr.viewDef.frustum[i].Normal())));
        }

        // eventually, plane five will be the rear clipping plane for fog
        float dNear, dFar, dLeft, dUp;

        dNear = r_znear.GetFloat();
        if (tr.viewDef.renderView.cramZNear) {
            dNear *= 0.25f;
        }

        dFar = MAX_WORLD_SIZE;
        dLeft = (float) (dFar * Math.tan(DEG2RAD(tr.viewDef.renderView.fov_x * 0.5f)));
        dUp = (float) (dFar * Math.tan(DEG2RAD(tr.viewDef.renderView.fov_y * 0.5f)));
        tr.viewDef.viewFrustum.SetOrigin(tr.viewDef.renderView.vieworg);
        tr.viewDef.viewFrustum.SetAxis(tr.viewDef.renderView.viewaxis);
        tr.viewDef.viewFrustum.SetSize(dNear, dFar, dLeft, dUp);
    }


    /*
     ===================
     R_ConstrainViewFrustum
     ===================
     */
    public static void R_ConstrainViewFrustum() {
        idBounds bounds = new idBounds();

        // constrain the view frustum to the total bounds of all visible lights and visible entities
        bounds.Clear();
        for (viewLight_s vLight = tr.viewDef.viewLights; vLight != null; vLight = vLight.next) {
            bounds.AddBounds(vLight.lightDef.frustumTris.bounds);
        }
        for (viewEntity_s vEntity = tr.viewDef.viewEntitys; vEntity != null; vEntity = vEntity.next) {
            bounds.AddBounds(vEntity.entityDef.referenceBounds);
        }
        tr.viewDef.viewFrustum.ConstrainToBounds(bounds);

        if (r_useFrustumFarDistance.GetFloat() > 0.0f) {
            tr.viewDef.viewFrustum.MoveFarDistance(r_useFrustumFarDistance.GetFloat());
        }
    }

    /*
     ==========================================================================================

     DRAWSURF SORTING

     ==========================================================================================
     */
    /*
     =======================
     R_QsortSurfaces

     =======================
     */
    public static class R_QsortSurfaces implements cmp_t<drawSurf_s> {

        @Override
        public int compare(drawSurf_s a, drawSurf_s b) {

            //this check assumes that the array contains nothing but nulls from this point.
            if (null == a && null == b) {
                return 0;
            }

            if (null == b || (null != a && a.sort < b.sort)) {
                return -1;
            }
            if (null == a || (null != b && a.sort > b.sort)) {
                return 1;
            }
            return 0;
        }
    };

    /*
     =================
     R_SortDrawSurfs
     =================
     */
    public static void R_SortDrawSurfs() {
        // sort the drawsurfs by sort type, then orientation, then shader
//        qsort(tr.viewDef.drawSurfs, tr.viewDef.numDrawSurfs, sizeof(tr.viewDef.drawSurfs[0]), R_QsortSurfaces);
        if (tr.viewDef.drawSurfs != null) {
            Arrays.sort(tr.viewDef.drawSurfs, 0, tr.viewDef.numDrawSurfs, new R_QsortSurfaces());
//            int bla = 0;
//            for (int i = 0; i < tr.viewDef.numDrawSurfs; i++) {
//                Material.shaderStage_t[] stages = tr.viewDef.drawSurfs[i].material.stages;
//                if (stages != null && stages[0].texture.image[0] != null &&
//                        stages[0].texture.image[0].imgName.toString().contains("env/cloudy")) {
//                    tr.viewDef.drawSurfs[bla++] = tr.viewDef.drawSurfs[i];
//                    System.out.println(stages[0].texture.image[0].imgName);
//                }
//            }
//            tr.viewDef.numDrawSurfs = bla;
//
//            final int from = 61;
//            final int to = Math.min(tr.viewDef.numDrawSurfs - from, from + 1);
//            tr.viewDef.drawSurfs = Arrays.copyOfRange(tr.viewDef.drawSurfs, from, to);
//            tr.viewDef.numDrawSurfs = to - from;
//           tr.viewDef.drasawwwwwwwwwwwwwwwwwwwwwwwwwwwwaw a    wSurfs[0].geo.indexes = null;
        }
    }

//========================================================================
//    
//    
//==============================================================================

    /*
     ================
     R_RenderView

     A view may be either the actual camera view,
     a mirror / remote location, or a 3D view on a gui surface.

     Parms will typically be allocated with R_FrameAlloc
     ================
     */ static int DEBUG_R_RenderView = 0;

    static void R_RenderView(viewDef_s parms) {
        viewDef_s oldView;
        DEBUG_R_RenderView++;

        if (parms.renderView.width <= 0 || parms.renderView.height <= 0) {
            return;
        }

        tr.viewCount++;
//        System.out.println("tr.viewCount::R_RenderView");

        // save view in case we are a subview
        oldView = tr.viewDef;

        tr.viewDef = parms;

        tr.sortOffset = 0;

        // set the matrix for world space to eye space
        R_SetViewMatrix(tr.viewDef);

        // the four sides of the view frustum are needed
        // for culling and portal visibility
        R_SetupViewFrustum();

        // we need to set the projection matrix before doing
        // portal-to-screen scissor box calculations
        R_SetupProjection();

        // identify all the visible portalAreas, and the entityDefs and
        // lightDefs that are in them and pass culling.
//	static_cast<idRenderWorldLocal *>(parms.renderWorld).FindViewLightsAndEntities();
        ((idRenderWorldLocal) parms.renderWorld).FindViewLightsAndEntities();

        // constrain the view frustum to the view lights and entities
        R_ConstrainViewFrustum();

        // make sure that interactions exist for all light / entity combinations
        // that are visible
        // add any pre-generated light shadows, and calculate the light shader values
        R_AddLightSurfaces();

        // adds ambient surfaces and create any necessary interaction surfaces to add to the light
        // lists
        R_AddModelSurfaces();

        // any viewLight that didn't have visible surfaces can have it's shadows removed
        R_RemoveUnecessaryViewLights();

        // sort all the ambient surfaces for translucency ordering
        R_SortDrawSurfs();

        // generate any subviews (mirrors, cameras, etc) before adding this view
        if (tr_subview.R_GenerateSubViews()) {
            // if we are debugging subviews, allow the skipping of the
            // main view draw
            if (r_subviewOnly.GetBool()) {
                return;
            }
        }

        // write everything needed to the demo file
        if (session.writeDemo != null) {
//		static_cast<idRenderWorldLocal *>(parms.renderWorld)->WriteVisibleDefs( tr.viewDef );
            ((idRenderWorldLocal) parms.renderWorld).WriteVisibleDefs(tr.viewDef);
        }

        // add the rendering commands for this viewDef
        R_AddDrawViewCmd(parms);

        // restore view in case we are a subview
        tr.viewDef = oldView;
    }
}
