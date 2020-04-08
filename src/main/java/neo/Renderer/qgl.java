package neo.Renderer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.EXTDepthBoundsTest;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

import neo.TempDump;
import neo.open.Nio;

/**
 * so yeah, it's easier to use this class as an interface. rather than refactor
 * all the qwgl stuff to gl and such.
 * @deprecated use {@link QGL} instead
 */
public class qgl {

    // ATI_fragment_shader
    static class ATI_fragment_shader {
        private static /*PFNGLALPHAFRAGMENTOP1ATIPROC*/void qglAlphaFragmentOp1ATI(int op, int dst, int dstMod, int arg1, int arg1Rep, int arg1Mod) {
        }

        private static /*PFNGLALPHAFRAGMENTOP2ATIPROC*/void qglAlphaFragmentOp2ATI(int op, int dst, int dstMod, int arg1, int arg1Rep, int arg1Mod, int arg2, int arg2Rep, int arg2Mod) {
        }

        private static /*PFNGLALPHAFRAGMENTOP3ATIPROC*/void qglAlphaFragmentOp3ATI(int op, int dst, int dstMod, int arg1, int arg1Rep, int arg1Mod, int arg2, int arg2Rep, int arg2Mod, int arg3, int arg3Rep, int arg3Mod) {
        }

        private static /*PFNGLBEGINFRAGMENTSHADERATIPROC*/void qglBeginFragmentShaderATI() {
        }

        private static /*PFNGLBINDFRAGMENTSHADERATIPROC*/void qglBindFragmentShaderATI(int id) {
        }

        private static /*PFNGLCOLORFRAGMENTOP1ATIPROC*/void qglColorFragmentOp1ATI(int op, int dst, int dstMask, int dstMod, int arg1, int arg1Rep, int arg1Mod) {
        }

        private static /*PFNGLCOLORFRAGMENTOP2ATIPROC*/void qglColorFragmentOp2ATI(int op, int dst, int dstMask, int dstMod, int arg1, int arg1Rep, int arg1Mod, int arg2, int arg2Rep, int arg2Mod) {
        }

        private static /*PFNGLCOLORFRAGMENTOP3ATIPROC*/void qglColorFragmentOp3ATI(int op, int dst, int dstMask, int dstMod, int arg1, int arg1Rep, int arg1Mod, int arg2, int arg2Rep, int arg2Mod, int arg3, int arg3Rep, int arg3Mod) {
        }

        private static /*PFNGLDELETEFRAGMENTSHADERATIPROC*/void qglDeleteFragmentShaderATI(int id) {
        }

        private static /*PFNGLENDFRAGMENTSHADERATIPROC*/void qglEndFragmentShaderATI() {
        }

        private static /*PFNGLGENFRAGMENTSHADERSATIPROC*/    void qglGenFragmentShadersATI(int range) {
        }

        private static /*PFNGLPASSTEXCOORDATIPROC*/void qglPassTexCoordATI(int dst, int coord, int swizzle) {
        }

        private static /*PFNGLSAMPLEMAPATIPROC*/void qglSampleMapATI(int dst, int interp, int swizzle) {
        }

        private static /*PFNGLSETFRAGMENTSHADERCONSTANTATIPROC*/void qglSetFragmentShaderConstantATI(int dst, final float[] value) {
        }
    }

    private static final boolean GL_DEBUG = false;

    static {
        if (GL_DEBUG) {
			qglEnable(GL43.GL_DEBUG_OUTPUT);
		}
    }
    static final boolean qGL_FALSE = false;

    static final boolean qGL_TRUE  = true;

    static int bla = 0;

    private static void checkGLError() {
        if (GL_DEBUG) {
            final ByteBuffer messageLog = Nio.newByteBuffer(1000);
//            while (GL43.glGetDebugMessageLog(1, null, null, null, null, null, messageLog) > 0) {
//                System.out.println(TempDump.bbtoa(messageLog));
//                messageLog.clear();
//            }
//            Util.checkGLError();
        }
    }

    private static void DEBUG_printName(final String functionName) {
        if (GL_DEBUG) {
//            System.out.println(functionName);
        }
    }

    /**
     *
     *
     *
     *
     *
     *
     *
     *
     */
//    private static boolean qwglUseFontOutlines(long hdc, long dword1, long dword2, long dword3, float f1, float f2, int, LPGLYPHMETRICSFLOAT lpglyphmetricsfloat) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    private static boolean qwglDescribeLayerPlane(long hdc, int i1, int i2, int uint, LPLAYERPLANEDESCRIPTOR lplayerplanedescriptor) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    private static int qwglSetLayerPaletteEntries(long hdc, int i1, int i2, int i3, long colorref) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    private static int qwglGetLayerPaletteEntries(long hdc, int i1, int i2, int i3, long colorref) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    private static boolean qwglRealizeLayerPalette(long hdc, int i, boolean b) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    private static boolean qwglSwapLayerBuffers(long hdc, int uint) {
//        return WGL.wglSwapLayerBuffers(hdc, uint);
//    }
    private static void qglAccum(int op, float value) {DEBUG_printName("glAccum");
        GL11.glAccum(op, value);
    }

    //
//    // multitexture
    public static void qglActiveTextureARB(int texture) {DEBUG_printName("glActiveTextureARB");
        ARBMultitexture.glActiveTextureARB(texture);
    }

    public static void qglAlphaFunc(int func, float ref) {DEBUG_printName("glAlphaFunc");
        GL11.glAlphaFunc(func, ref);
    }

    private static boolean qglAreTexturesResident(int n, IntBuffer textures, ByteBuffer residences) {DEBUG_printName("glAreTexturesResident");
        return GL11.glAreTexturesResident(textures, residences);//TODO:is n really necessary?
    }

    public static void qglArrayElement(int i) {DEBUG_printName("glArrayElement");
        GL11.glArrayElement(i);
    }

    public static void qglBegin(int mode) {DEBUG_printName("glBegin");
        GL11.glBegin(mode);
    }

    // ARB_vertex_buffer_object
    public static void qglBindBufferARB(int target, int buffer) {DEBUG_printName("glBindBufferARB");
        ARBVertexBufferObject.glBindBufferARB(target, buffer);
    }

    public static /*PFNGLBINDPROGRAMARBPROC*/void qglBindProgramARB(int target, Enum program) {DEBUG_printName("glBindProgramARB");
        qglBindProgramARB(target, program.ordinal());
    }

    public static /*PFNGLBINDPROGRAMARBPROC*/void qglBindProgramARB(int target, int program) {DEBUG_printName("glBindProgramARB");
        ARBVertexProgram.glBindProgramARB(target, program);
    }
//extern PFNGLGENPROGRAMSARBPROC				qglGenProgramsARB;
//

    public static void qglBindTexture(int target, int texture) {DEBUG_printName("glBindTexture");
//        System.out.printf("qglBindTexture(%d, %d)\n", target, texture);
        GL11.glBindTexture(target, texture);
    }

    private static void qglBitmap(int width, int height, float xorig, float yorig, float xmove, float ymove, ByteBuffer bitmap) {DEBUG_printName("glBitmap");
        GL11.glBitmap(width, height, xorig, yorig, xmove, ymove, bitmap);
    }

    public static void qglBlendFunc(int sFactor, int dFactor) {DEBUG_printName("glBlendFunc");
//        System.out.printf("--%d, %d\n", sFactor, dFactor);
        GL11.glBlendFunc(sFactor, dFactor);
    }

    //extern PFNGLISBUFFERARBPROC qglIsBufferARB;
    public static void qglBufferDataARB(int target, int size, ByteBuffer data, int usage) {DEBUG_printName("glBufferDataARB");
//        GL15.glBufferData(target, data, usage);//TODO:!!!!!!!!!!!!!!!!!!!!!!!!!
        ARBVertexBufferObject.glBufferDataARB(target, data, usage);
    }

    public static  /*PFNGLBUFFERSUBDATAARBPROC*/void qglBufferSubDataARB(int target, long offset, long size, ByteBuffer data) {DEBUG_printName("glBufferSubDataARB");
        ARBVertexBufferObject.glBufferSubDataARB(target, offset, data);
    }

    private static void qglCallList(int list) {DEBUG_printName("glCallList");
        GL11.glCallList(list);
    }

    private static void qglCallLists(int n, int type, Object lists) {
//        GL11.glCallLists(lists);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglClear(int mask) {DEBUG_printName("glClear");
        GL11.glClear(mask);
    }

    private static void qglClearAccum(float red, float green, float blue, float alpha) {DEBUG_printName("glClearAccum");
        GL11.glClearAccum(red, green, blue, alpha);
    }

    public static void qglClearColor(float red, float green, float blue, float alpha) {DEBUG_printName("glClearColor");
        GL11.glClearColor(red, green, blue, alpha);
    }

    public static void qglClearDepth(double depth) {DEBUG_printName("glClearDepth");
        GL11.glClearDepth(depth);
    }

    private static void qglClearIndex(float c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglClearStencil(int s) {DEBUG_printName("glClearStencil");
        GL11.glClearStencil(s);
    }

    public static void qglClientActiveTextureARB(int texture) {DEBUG_printName("glClientActiveTextureARB");
        ARBMultitexture.glClientActiveTextureARB(texture);
    }

    private static void qglClipPlane(int plane, DoubleBuffer equation) {DEBUG_printName("glClipPlane");
        GL11.glClipPlane(plane, equation);
    }

    private static void qglColor3b(byte red, byte green, byte blue) {DEBUG_printName("glColor3b");
        GL11.glColor3b(red, green, blue);
    }

    private static void qglColor3bv(byte[] v) {DEBUG_printName("glColor3bv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3d(double red, double green, double blue) {DEBUG_printName("glColor3d");
        GL11.glColor3d(red, green, blue);
    }

//    private static int qwglChoosePixelFormat(long hdc, PIXELFORMATDESCRIPTOR pixelformatdescriptor) {
//        return WGL.wglChoosePixelFormat(hdc, pixelformatdescriptor);
//    }
//
//    private static int qwglDescribePixelFormat(long hdc, int i, int uint, PIXELFORMATDESCRIPTOR lppixelformatdescriptor) {
//        return WGL.wglDescribePixelFormat(hdc, i, uint, lppixelformatdescriptor);
//    }
//
//    private static int qwglGetPixelFormat(long hdc) {
//        return WGL.wglGetPixelFormat(hdc);
//    }
//
//    private static boolean qwglSetPixelFormat(long hdc, int i, PIXELFORMATDESCRIPTOR pixelformatdescriptor) {
//        return WGL.wglSetPixelFormat(hdc, i, pixelformatdescriptor);
//    }
//
//    private static boolean qwglSwapBuffers(long hdc) {
//        return WGL.wglSwapBuffers(hdc);
//    }

    /**
     *
     *
     *
     *
     *
     *
     *
     *
     */
//    private static boolean qwglCopyContext(long hglrc1, long hglrc2, int uint) {
//        return WGL.wglCopyContext(hglrc1, hglrc2, uint);
//    }
//
//    private static long qwglCreateContext(long hdc) {
//        return WGL.wglCreateContext(hdc);
//    }
//
//    private static long qwglCreateLayerContext(long hdc, int i) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    private static boolean qwglDeleteContext(long hglrc) {
//        return WGL.wglDeleteContext(hglrc);
//    }
//
//    private static long qwglGetCurrentContext() {
//        return WGL.wglGetCurrentContext();
//    }
//
//    private static long qwglGetCurrentDC() {
//        return WGL.wglGetCurrentDC();
//    }
//
//    private static long qwglGetProcAddress(String lpcstr) {
//        return WGL.wglGetProcAddress(lpcstr);
//    }
//
//    private static boolean qwglMakeCurrent(long hdc, long hglrc) {
//        return WGL.wglMakeCurrent(hdc, hglrc);
//    }
//
//    private static boolean qwglShareLists(long hglrc1, long hglrc2) {
//        return WGL.wglShareLists(hglrc1, hglrc2);
//    }
//
//    private static boolean qwglUseFontBitmaps(long hdc, long dword1, long dword2, long dword3) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

    private static void qglColor3dv(double[] v) {DEBUG_printName("glColor3dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglColor3f(float red, float green, float blue) {DEBUG_printName("glColor3f");
        GL11.glColor3f(red, green, blue);
    }

    public static void qglColor3fv(float[] v) {DEBUG_printName("glColor3fv");
        qglColor3f(v[0], v[1], v[2]);
    }

    private static void qglColor3i(int red, int green, int blue) {DEBUG_printName("glColor3i");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3iv(int[] v) {DEBUG_printName("glColor3iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3s(short red, short green, short blue) {DEBUG_printName("glColor3s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3sv(short[] v) {DEBUG_printName("glColor3sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3ub(byte red, byte green, byte blue) {DEBUG_printName("glColor3ub");
        GL11.glColor3ub(red, green, blue);
    }

    public static void qglColor3ubv(byte[] v) {DEBUG_printName("glColor3ubv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3ui(int red, int green, int blue) {DEBUG_printName("glColor3ui");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3uiv(int[] v) {DEBUG_printName("glColor3uiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3us(short red, short green, short blue) {DEBUG_printName("glColor3us");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor3usv(short[] v) {DEBUG_printName("glColor3usv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4b(byte red, byte green, byte blue, byte alpha) {DEBUG_printName("glColor4b");
        GL11.glColor4b(red, green, blue, alpha);
    }

    private static void qglColor4bv(byte[] v) {DEBUG_printName("glColor4bv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4d(double red, double green, double blue, double alpha) {DEBUG_printName("glColor4d");
        GL11.glColor4d(red, green, blue, alpha);
    }

    private static void qglColor4dv(double[] v) {DEBUG_printName("glColor4dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglColor4f(float red, float green, float blue, float alpha) {DEBUG_printName("glColor4f");
        GL11.glColor4f(red, green, blue, alpha);
    }

    public static void qglColor4fv(float[] v) {DEBUG_printName("glColor4fv");
        qglColor4f(v[0], v[1], v[2], v[3]);
    }

    private static void qglColor4i(int red, int green, int blue, int alpha) {DEBUG_printName("glColor4i");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4iv(int[] v) {DEBUG_printName("glColor4iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4s(short red, short green, short blue, short alpha) {DEBUG_printName("glColor4s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4sv(short[] v) {DEBUG_printName("glColor4sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4ub(byte red, byte green, byte blue, byte alpha) {DEBUG_printName("glColor4ub");
        GL11.glColor4ub(red, green, blue, alpha);
    }

    public static void qglColor4ubv(byte[] v) {DEBUG_printName("glColor4ubv");
        GL11.glColor4ub(v[0], v[1], v[2], v[3]);
    }

    private static void qglColor4ui(int red, int green, int blue, int alpha) {DEBUG_printName("glColor4ui");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4uiv(int[] v) {DEBUG_printName("glColor4uiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4us(short red, short green, short blue, short alpha) {DEBUG_printName("glColor4us");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglColor4usv(short[] v) {DEBUG_printName("glColor4usv");
        GL11.glColor4usv(v);
    }

    public static void qglColorMask(boolean red, boolean green, boolean blue, boolean alpha) {DEBUG_printName("glColorMask");
        GL11.glColorMask(red, green, blue, alpha);
    }

    public static void qglColorMask(int red, int green, int blue, int alpha) {
        qglColorMask(red != 0, green != 0, blue != 0, alpha != 0);
    }

    private static void qglColorMaterial(int face, int mode) {DEBUG_printName("glColorMaterial");
        GL11.glColorMaterial(face, mode);
    }

    public static void qglColorPointer(int size, int type, int stride, long pointer) {DEBUG_printName("glColorPointer");
        GL11.glColorPointer(size, type, stride, pointer);
    }

    @Deprecated
    public static void qglColorPointer(int size, int type, int stride, byte[] pointer) {DEBUG_printName("glColorPointer");
        GL11.glColorPointer(size, type, stride, ByteBuffer.wrap(pointer));
    }

    @Deprecated
    private static void qglColorPointer(int size, int type, int stride, Object pointer) {DEBUG_printName("glColorPointer");
//        GL11.glColorPointer(size, type, stride, );
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // shared texture palette
    public static void qglColorTableEXT(int target, int internalFormat, int width, int format, int type, byte[] data) {DEBUG_printName("glColorTableEXT");
        ARBImaging.glColorTable(target, internalFormat, width, format, type, ByteBuffer.wrap(data));
    }
//

    private static void qglCombinerInputNV(int stage, int portion, int variable, int input, int mapping, int componentUsage) {DEBUG_printName("glCombinerInputNV");
//        NVRegisterCombiners.glCombinerInputNV(stage, portion, variable, input, mapping, componentUsage);
        throw new UnsupportedOperationException();
    }

    private static void qglCombinerOutputNV(int stage, int portion, int abOutput, int cdOutput, int sumOutput, int scale, int bias,
                                           boolean abDotProduct, boolean cdDotProduct, boolean muxSum) {DEBUG_printName("glCombinerOutputNV");
//        NVRegisterCombiners.glCombinerOutputNV(stage, portion, abOutput, cdOutput, sumOutput, scale, bias, abDotProduct, cdDotProduct, muxSum);
        throw new UnsupportedOperationException();
    }

    //extern PFNGLGETBUFFERSUBDATAARBPROC qglGetBufferSubDataARB;
//extern PFNGLMAPBUFFERARBPROC qglMapBufferARB;
//extern PFNGLUNMAPBUFFERARBPROC qglUnmapBufferARB;
//extern PFNGLGETBUFFERPARAMETERIVARBPROC qglGetBufferParameterivARB;
//extern PFNGLGETBUFFERPOINTERVARBPROC qglGetBufferPointervARB;
//
//
// NV_register_combiners
    private static void qglCombinerParameterfvNV(int pName, final float[] params) {
        throw new UnsupportedOperationException();
    }

    //extern	void ( APIENTRY *qglCombinerParameterivNV )( GLenum pName, const GLint *params );
//extern	void ( APIENTRY *qglCombinerParameterfNV )( GLenum pName, const GLfloat param );
    private static void qglCombinerParameteriNV(int pName, final int param) {DEBUG_printName("glCombinerParameteriNV");
//        NVRegisterCombiners.glCombinerParameteriNV(pName, param);
        throw new UnsupportedOperationException();
    }

    //// EXT_stencil_two_side
//extern	PFNGLACTIVESTENCILFACEEXTPROC	qglActiveStencilFaceEXT;
//
//
//// ATI_separate_stencil
//extern	PFNGLSTENCILOPSEPARATEATIPROC		qglStencilOpSeparateATI;
//extern	PFNGLSTENCILFUNCSEPARATEATIPROC		qglStencilFuncSeparateATI;
    //
    // ARB_texture_compression
    public static void	/*PFNGLCOMPRESSEDTEXIMAGE2DARBPROC*/    qglCompressedTexImage2DARB(int target, int level, int internalformat,
                                                                                             int width, int height, int border, int imageSize, final ByteBuffer data) {DEBUG_printName("glCompressedTexImage2DARB");
//        ARBTextureCompression.glCompressedTexImage2DARB(target, level, internalformat, width, height, border, data);
        GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, data);
    }

    @Deprecated
    private static void qglCompressedTexImage2DARB(int target, int level, int internalformat, int width, int height, int border, int imageSize, long pData_buffer_offset) {
        GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, pData_buffer_offset);
        throw new UnsupportedOperationException();
    }

    private static void qglCopyPixels(int x, int y, int width, int height, int type) {DEBUG_printName("glCopyPixels");
        GL11.glCopyPixels(x, y, width, height, type);
    }

    private static void qglCopyTexImage1D(int target, int level, int internalFormat, int x, int y, int width, int border) {DEBUG_printName("glCopyTexImage1D");
        GL11.glCopyTexImage1D(target, level, internalFormat, x, y, width, border);
    }

    public static void qglCopyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) {DEBUG_printName("glCopyTexImage2D");
        GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border);
    }

    private static void qglCopyTexSubImage1D(int target, int level, int xoffset, int x, int y, int width) {DEBUG_printName("glCopyTexSubImage1D");
        GL11.glCopyTexSubImage1D(target, level, xoffset, x, y, width);
    }

    public static void qglCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {DEBUG_printName("glCopyTexSubImage2D");
        GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    public static void qglCullFace(int mode) {DEBUG_printName("glCullFace");
        GL11.glCullFace(mode);
    }

    private static void qglDeleteLists(int list, int range) {DEBUG_printName("glDeleteLists");
        GL11.glDeleteLists(list, range);
    }

    public static void qglDeleteTextures(int n, int texture) {DEBUG_printName("glDeleteTextures");
        GL11.glDeleteTextures(texture);
    }

    private static void qglDeleteTextures(int n, int[] textures) {DEBUG_printName("glDeleteTextures");
//        GL11.glDeleteTextures();
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void /*PFNGLDEPTHBOUNDSEXTPROC*/ qglDepthBoundsEXT(double zmin, double zmax) {DEBUG_printName("glDepthBoundsEXT");
        EXTDepthBoundsTest.glDepthBoundsEXT(zmin, zmax);
    }
//
////===========================================================================

    public static void qglDepthFunc(int func) {DEBUG_printName("glDepthFunc");
        GL11.glDepthFunc(func);
    }

    public static void qglDepthMask(boolean flag) {DEBUG_printName("glDepthMask");
        GL11.glDepthMask(flag);
    }

    public static void qglDepthRange(double zNear, double zFar) {DEBUG_printName("glDepthRange");
        GL11.glDepthRange(zNear, zFar);
    }

    public static void qglDisable(int cap) {DEBUG_printName("glDisable");
        GL11.glDisable(cap);
    }

    public static void qglDisableClientState(int array) {DEBUG_printName("glDisableClientState");
        GL11.glDisableClientState(array);
    }

    public static /*PFNGLDISABLEVERTEXATTRIBARRAYARBPROC*/    void qglDisableVertexAttribArrayARB(int index) {DEBUG_printName("glDisableVertexAttribArrayARB");
        ARBVertexShader.glDisableVertexAttribArrayARB(index);
    }

    private static void qglDrawArrays(int mode, int first, int count) {DEBUG_printName("glDrawArrays");
        GL11.glDrawArrays(mode, first, count);
    }

    public static void qglDrawBuffer(int mode) {DEBUG_printName("glDrawBuffer");
        GL11.glDrawBuffer(mode);
    }

    public static void qglDrawElements(int mode, int count, int type, ByteBuffer indices) {DEBUG_printName("glDrawElements1");
        GL11.glDrawElements(mode, type, indices);
    }

    public static void qglDrawElements(int mode, int count, int type, int[] indices) {DEBUG_printName("glDrawElements2");
        GL11.glDrawElements(mode, (IntBuffer) wrap(indices).position(count).flip());//TODO:subarray
    }

    public static void qglDrawPixels(int width, int height, int format, int type, byte[][][] pixels) {DEBUG_printName("glDrawPixels");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglDrawPixels(int width, int height, int format, int type, ByteBuffer pixels) {DEBUG_printName("glDrawPixels");
        GL11.glDrawPixels(width, height, format, type, pixels);
    }

    private static void qglEdgeFlag(boolean flag) {DEBUG_printName("glEdgeFlag");
        GL11.glEdgeFlag(flag);
    }

    private static void qglEdgeFlagPointer(int stride, Object pointer) {DEBUG_printName("glEdgeFlagPointer");
//        GL11.glEdgeFlagPointer(stride, );
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglEdgeFlagv(boolean flag) {DEBUG_printName("glEdgeFlagv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglEnable(int cap) {DEBUG_printName("glEnable");
//        System.out.println("--"+cap);
        GL11.glEnable(cap);
    }

    public static void qglEnableClientState(int array) {DEBUG_printName("glEnableClientState");
        GL11.glEnableClientState(array);
    }

    public static /*PFNGLENABLEVERTEXATTRIBARRAYARBPROC*/    void qglEnableVertexAttribArrayARB(int index) {DEBUG_printName("glEnableVertexAttribArrayARB");
        ARBVertexShader.glEnableVertexAttribArrayARB(index);
    }

    public static void qglEnd() {DEBUG_printName("glEnd");
        GL11.glEnd();

    }

    private static void qglEndList() {DEBUG_printName("glEndList");
        GL11.glEndList();
    }

    private static void qglEvalCoord1d(double u) {DEBUG_printName("glEvalCoord1d");
        GL11.glEvalCoord1d(u);
    }

    private static void qglEvalCoord1dv(double[] u) {DEBUG_printName("glEvalCoord1dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglEvalCoord1f(float u) {DEBUG_printName("glEvalCoord1f");
        GL11.glEvalCoord1f(u);
    }

    private static void qglEvalCoord1fv(float[] u) {DEBUG_printName("glEvalCoord1fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglEvalCoord2d(double u, double v) {DEBUG_printName("glEvalCoord2d");
        GL11.glEvalCoord2d(u, v);
    }

    private static void qglEvalCoord2dv(double[] u) {DEBUG_printName("glEvalCoord2dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglEvalCoord2f(float u, float v) {DEBUG_printName("glEvalCoord2f");
        GL11.glEvalCoord2f(u, v);
    }

    private static void qglEvalCoord2fv(float[] u) {DEBUG_printName("glEvalCoord2fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglEvalMesh1(int mode, int i1, int i2) {DEBUG_printName("glEvalMesh1");
        GL11.glEvalMesh1(mode, i1, i2);
    }

    private static void qglEvalMesh2(int mode, int i1, int i2, int j1, int j2) {DEBUG_printName("glEvalMesh2");
        GL11.glEvalMesh2(mode, i1, i2, j1, j2);
    }

    private static void qglEvalPoint1(int i) {DEBUG_printName("glEvalPoint1");
        GL11.glEvalPoint1(i);
    }

    private static void qglEvalPoint2(int i, int j) {DEBUG_printName("glEvalPoint2");
        GL11.glEvalPoint2(i, j);
    }

    private static void qglFeedbackBuffer(int size, int type, FloatBuffer buffer) {DEBUG_printName("glFeedbackBuffer");
        GL11.glFeedbackBuffer(type, buffer);
    }

    private static void qglFinalCombinerInputNV(int variable, int input, int mapping, int componentUsage) {DEBUG_printName("glFinalCombinerInputNV");
//        NVRegisterCombiners.glFinalCombinerInputNV(variable, input, mapping, componentUsage);
        throw new UnsupportedOperationException();
    }

    public static void qglFinish() {DEBUG_printName("glFinish");
        GL11.glFinish();
    }

    public static void qglFlush() {DEBUG_printName("glFlush");
        GL11.glFlush();
    }

    private static void qglFogf(int pName, float param) {DEBUG_printName("glFogf");
        GL11.glFogf(pName, param);
    }

    private static void qglFogfv(int pName, float[] params) {DEBUG_printName("glFogfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglFogi(int pName, int param) {DEBUG_printName("glFogi");
        GL11.glFogi(pName, param);
    }

    private static void qglFogiv(int pName, int[] params) {DEBUG_printName("glFogiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglFrontFace(int mode) {DEBUG_printName("glFrontFace");
        GL11.glFrontFace(mode);
    }

    private static void qglFrustum(double left, double right, double bottom, double top, double zNear, double zFar) {DEBUG_printName("glFrustum");
        GL11.glFrustum(left, right, bottom, top, zNear, zFar);
    }

    public static int qglGenBuffersARB() {DEBUG_printName("glGenBuffersARB");
        return ARBVertexBufferObject.glGenBuffersARB();
    }

    //extern PFNGLDELETEBUFFERSARBPROC qglDeleteBuffersARB;
    private static void qglGenBuffersARB(int n, IntBuffer[] buffers) {DEBUG_printName("glGenBuffersARB");
        ARBVertexBufferObject.glGenBuffersARB(buffers[0] = Nio.newIntBuffer(n));
    }

    private static int qglGenLists(Enum range) {
        return qglGenLists(range.ordinal());
    }

    private static int qglGenLists(int range) {DEBUG_printName("glGenLists");
        return GL11.glGenLists(range);
    }

    public static int qglGenTextures() {DEBUG_printName("glGenTextures");
//        System.out.println("-----"+ (bla++));
//        TempDump.printCallStack("" + (bla++));
        return GL11.glGenTextures();
    }

    private static void qglGenTextures(int n, int[] textures) {DEBUG_printName("glGenTextures");
        GL11.glGenTextures();
    }

    private static void qglGetBooleanv(int pName, boolean[] params) {DEBUG_printName("glGetBooleanv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetClipPlane(int plane, DoubleBuffer equation) {DEBUG_printName("glGetClipPlane");
        GL11.glGetClipPlane(plane, equation);
    }

    public static void /*PFNGLGETCOMPRESSEDTEXIMAGEARBPROC*/ qglGetCompressedTexImageARB(int target, int index, ByteBuffer img) {DEBUG_printName("glGetCompressedTexImageARB");
        ARBTextureCompression.glGetCompressedTexImageARB(target, index, img);
    }

    private static void qglGetDoublev(int pName, double[] params) {DEBUG_printName("glGetDoublev");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static int qglGetError() {//DEBUG_printName("glGetError");
        checkGLError();
        return GL11.glGetError();
    }

    public static void qglGetFloatv(int pName, FloatBuffer params) {DEBUG_printName("glGetFloatv");
        GL11.glGetFloatv(pName, params);
    }

    public static int qglGetInteger(int pName) {DEBUG_printName("glGetInteger");
        return GL11.glGetInteger(pName);
    }

    public static void qglGetIntegerv(int pName, IntBuffer params) {DEBUG_printName("glGetIntegerv");
        GL11.glGetIntegerv(pName, params);
    }

    private static void qglGetLightfv(int light, int pName, float[] params) {DEBUG_printName("glGetLightfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetLightiv(int light, int pName, int[] params) {DEBUG_printName("glGetLightiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetMapdv(int target, int query, double[] v) {DEBUG_printName("glGetMapdv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetMapfv(int target, int query, float[] v) {DEBUG_printName("glGetMapfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetMapiv(int target, int query, int[] v) {DEBUG_printName("glGetMapiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetMaterialfv(int face, int pName, float[] params) {DEBUG_printName("glGetMaterialfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetMaterialiv(int face, int pName, int[] params) {DEBUG_printName("glGetMaterialiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetPixelMapfv(int map, float[] values) {DEBUG_printName("glGetPixelMapfv");
//        GL11.glGetPixelMapfv(map, );
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetPixelMapuiv(int map, int[] values) {DEBUG_printName("glGetPixelMapuiv");
//        GL11.glGetPixelMapuiv(map, );
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetPixelMapusv(int map, short[] values) {DEBUG_printName("glGetPixelMapusv");
//        GL11.glGetPixelMapusv(map, );
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetPointerv(int pName, Object[] params) {DEBUG_printName("glGetPointerv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetPolygonStipple(byte mask) {DEBUG_printName("glGetPolygonStipple");
//        GL11.glGetPolygonStipple();
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static String qglGetString(int name) {DEBUG_printName("glGetString");
        return GL11.glGetString(name);
    }

    public static String qglGetStringi(int name, int index) {DEBUG_printName("glGetStringi");
        return GL30.glGetStringi(name, index);
    }

    private static void qglGetTexEnvfv(int target, int pName, float[] params) {DEBUG_printName("glGetTexEnvfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetTexEnviv(int target, int pName, int[] params) {DEBUG_printName("glGetTexEnviv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetTexGendv(int coord, int pName, double[] params) {DEBUG_printName("glGetTexGendv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetTexGenfv(int coord, int pName, float[] params) {DEBUG_printName("glGetTexGenfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetTexGeniv(int coord, int pName, int[] params) {DEBUG_printName("glGetTexGeniv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglGetTexImage(int target, int level, int format, int type, ByteBuffer pixels) {DEBUG_printName("glGetTexImage");
        GL11.glGetTexImage(target, level, format, type, pixels);
    }

    private static void qglGetTexLevelParameterfv(int target, int level, int pName, float[] params) {DEBUG_printName("glGetTexLevelParameterfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetTexLevelParameteriv(int target, int level, int pName, int[] params) {DEBUG_printName("glGetTexLevelParameteriv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetTexParameterfv(int target, int pName, float[] params) {DEBUG_printName("glGetTexParameterfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglGetTexParameteriv(int target, int pName, int[] params) {DEBUG_printName("glGetTexParameteriv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglHint(int target, int mode) {DEBUG_printName("glHint");
        GL11.glHint(target, mode);
    }

    private static void qglIndexd(double c) {DEBUG_printName("glIndexd");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexdv(double[] c) {DEBUG_printName("glIndexdv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexf(float c) {DEBUG_printName("glIndexf");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexfv(float[] c) {DEBUG_printName("glIndexfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexi(int c) {DEBUG_printName("glIndexi");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexiv(int[] c) {DEBUG_printName("glIndexiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexMask(int mask) {DEBUG_printName("glIndexMask");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexPointer(int type, int stride, Object pointer) {DEBUG_printName("glIndexPointer");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexs(short c) {DEBUG_printName("glIndexs");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexsv(short[] c) {DEBUG_printName("glIndexsv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexub(byte c) {DEBUG_printName("glIndexub");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglIndexubv(byte[] c) {DEBUG_printName("glIndexubv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglInitNames() {DEBUG_printName("glInitNames");
        GL11.glInitNames();
    }

    private static void qglInterleavedArrays(int format, int stride, ByteBuffer pointer) {DEBUG_printName("glInterleavedArrays");
        GL11.glInterleavedArrays(format, stride, pointer);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static boolean qglIsEnabled(int cap) {DEBUG_printName("glIsEnabled");
        return GL11.glIsEnabled(cap);
    }

    private static boolean qglIsList(int list) {DEBUG_printName("glIsList");
        return GL11.glIsList(list);
    }

    private static boolean qglIsTexture(int texture) {DEBUG_printName("glIsTexture");
        return GL11.glIsTexture(texture);
    }

    private static void qglLightf(int light, int pName, float param) {DEBUG_printName("glLightf");
        GL11.glLightf(light, pName, param);
    }

    private static void qglLightfv(int light, int pName, FloatBuffer params) {DEBUG_printName("glLightfv");
        GL11.glLightfv(light, pName, params);
    }

    private static void qglLighti(int light, int pName, int param) {DEBUG_printName("glLighti");
        GL11.glLighti(light, pName, param);
    }

    private static void qglLightiv(int light, int pName, IntBuffer params) {DEBUG_printName("glLightiv");
        GL11.glLightiv(light, pName, params);
    }

    private static void qglLightModelf(int pName, float param) {DEBUG_printName("glLightModelf");
        GL11.glLightModelf(pName, param);
    }

    private static void qglLightModelfv(int pName, FloatBuffer params) {DEBUG_printName("glLightModelfv");
        GL11.glLightModelfv(pName, params);
    }

    private static void qglLightModeli(int pName, int param) {DEBUG_printName("glLightModeli");
        GL11.glLightModeli(pName, param);
    }

    private static void qglLightModeliv(int pName, IntBuffer params) {DEBUG_printName("glLightModeliv");
        GL11.glLightModeliv(pName, params);
    }

    private static void qglLineStipple(int factor, short pattern) {DEBUG_printName("glLineStipple");
        GL11.glLineStipple(factor, pattern);
    }

    public static void qglLineWidth(float width) {DEBUG_printName("glLineWidth");
        GL11.glLineWidth(width);
    }

    private static void qglListBase(int base) {DEBUG_printName("glListBase");
        GL11.glListBase(base);
    }

    public static void qglLoadIdentity() {DEBUG_printName("glLoadIdentity");
        GL11.glLoadIdentity();
    }

    private static void qglLoadMatrixd(DoubleBuffer m) {DEBUG_printName("glLoadMatrixd");
        GL11.glLoadMatrixd(m);
    }

    public static void qglLoadMatrixf(float[] m) {DEBUG_printName("glLoadMatrixf");//TODO:convert to FloatBuffer.
        GL11.glLoadMatrixf(m);
    }

    private static void qglLoadName(int name) {DEBUG_printName("glLoadName");
        GL11.glLoadName(name);
    }

    private static void qglLogicOp(int opcode) {DEBUG_printName("glLogicOp");
        GL11.glLogicOp(opcode);
    }

    private static void qglMap1d(int target, double u1, double u2, int stride, int order, DoubleBuffer points) {DEBUG_printName("glMap1d");
        GL11.glMap1d(target, u1, u2, stride, order, points);
    }

    private static void qglMap1f(int target, float u1, float u2, int stride, int order, FloatBuffer points) {DEBUG_printName("glMap1f");
        GL11.glMap1f(target, u1, u2, stride, order, points);
    }

    private static void qglMap2d(int target, double u1, double u2, int ustride, int uorder, double v1, double v2, int vstride, int vorder, DoubleBuffer points) {DEBUG_printName("glMap2d");
        GL11.glMap2d(target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points);
    }

    private static void qglMap2f(int target, float u1, float u2, int ustride, int uorder, float v1, float v2, int vstride, int vorder, FloatBuffer points) {DEBUG_printName("glMap2f");
        GL11.glMap2f(target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points);
    }

    private static void qglMapGrid1d(int un, double u1, double u2) {DEBUG_printName("glMapGrid1d");
        GL11.glMapGrid1d(un, u1, u2);
    }

    private static void qglMapGrid1f(int un, float u1, float u2) {DEBUG_printName("glMapGrid1f");
        GL11.glMapGrid1f(un, u1, u2);
    }

    private static void qglMapGrid2d(int un, double u1, double u2, int vn, double v1, double v2) {DEBUG_printName("glMapGrid2d");
        GL11.glMapGrid2d(un, u1, u2, vn, v1, v2);
    }

    private static void qglMapGrid2f(int un, float u1, float u2, int vn, float v1, float v2) {DEBUG_printName("glMapGrid2f");
        GL11.glMapGrid2f(un, u1, u2, vn, v1, v2);
    }

    private static void qglMaterialf(int face, int pName, float param) {DEBUG_printName("glMaterialf");
        GL11.glMaterialf(face, pName, param);
    }

    private static void qglMaterialfv(int face, int pName, float[] params) {DEBUG_printName("glMaterialfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglMateriali(int face, int pName, int param) {DEBUG_printName("glMateriali");
        GL11.glMateriali(face, pName, param);
    }

    private static void qglMaterialiv(int face, int pName, int[] params) {DEBUG_printName("glMaterialiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglMatrixMode(int mode) {DEBUG_printName("glMatrixMode");
        GL11.glMatrixMode(mode);
    }

    private static void qglMultMatrixd(double[] m) {DEBUG_printName("glMultMatrixd");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglMultMatrixf(float[] m) {DEBUG_printName("glMultMatrixf");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglNewList(int list, int mode) {DEBUG_printName("glNewList");
        GL11.glNewList(list, mode);
    }

    private static void qglNormal3b(byte nx, byte ny, byte nz) {DEBUG_printName("glNormal3b");
        GL11.glNormal3b(nx, ny, nz);
    }

    private static void qglNormal3bv(byte[] v) {DEBUG_printName("glNormal3bv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglNormal3d(double nx, double ny, double nz) {DEBUG_printName("glNormal3d");
        GL11.glNormal3d(nx, ny, nz);
    }

    private static void qglNormal3dv(double[] v) {DEBUG_printName("glNormal3dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglNormal3f(float nx, float ny, float nz) {DEBUG_printName("glNormal3f");
        GL11.glNormal3f(nx, ny, nz);
    }

    private static void qglNormal3fv(float[] v) {DEBUG_printName("glNormal3fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglNormal3i(int nx, int ny, int nz) {DEBUG_printName("glNormal3i");
        GL11.glNormal3i(nx, ny, nz);
    }

    private static void qglNormal3iv(int[] v) {DEBUG_printName("glNormal3iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglNormal3s(short nx, short ny, short nz) {DEBUG_printName("glNormal3s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglNormal3sv(short[] v) {DEBUG_printName("glNormal3sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglNormalPointer(int type, int stride, long pointer) {DEBUG_printName("glNormalPointer");
        GL11.glNormalPointer(type, stride, pointer);
    }

    public static void qglOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {DEBUG_printName("glOrtho");
        GL11.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    private static void qglPassThrough(float token) {DEBUG_printName("glPassThrough");
        GL11.glPassThrough(token);
    }

    private static void qglPixelMapfv(int map, int mapsize, float[] values) {DEBUG_printName("glPixelMapfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglPixelMapuiv(int map, int mapsize, int[] values) {DEBUG_printName("glPixelMapuiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglPixelMapusv(int map, int mapsize, short[] values) {DEBUG_printName("glPixelMapusv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglPixelStoref(int pName, float param) {DEBUG_printName("glPixelStoref");
        GL11.glPixelStoref(pName, param);
    }

    public static void qglPixelStorei(int pName, int param) {DEBUG_printName("glPixelStorei");
        GL11.glPixelStorei(pName, param);
    }

    private static void qglPixelTransferf(int pName, float param) {DEBUG_printName("glPixelTransferf");
        GL11.glPixelTransferf(pName, param);
    }

    private static void qglPixelTransferi(int pName, int param) {DEBUG_printName("glPixelTransferi");
        GL11.glPixelTransferi(pName, param);
    }

    private static void qglPixelZoom(float xfactor, float yfactor) {DEBUG_printName("glPixelZoom");
        GL11.glPixelZoom(xfactor, yfactor);
    }

    public static void qglPointSize(float size) {DEBUG_printName("glPointSize");
        GL11.glPointSize(size);
    }

    public static void qglPolygonMode(int face, int mode) {DEBUG_printName("glPolygonMode");
        GL11.glPolygonMode(face, mode);
    }

    public static void qglPolygonOffset(float factor, float units) {DEBUG_printName("glPolygonOffset");
        GL11.glPolygonOffset(factor, units);
    }

    private static void qglPolygonStipple(ByteBuffer mask) {DEBUG_printName("glPolygonStipple");
        GL11.glPolygonStipple(mask);
    }

    public static void qglPopAttrib() {DEBUG_printName("glPopAttrib");
        GL11.glPopAttrib();
    }

    private static void qglPopClientAttrib() {DEBUG_printName("glPopClientAttrib");
        GL11.glPopClientAttrib();
    }

    public static void qglPopMatrix() {DEBUG_printName("glPopMatrix");
        GL11.glPopMatrix();
    }

    private static void qglPopName() {DEBUG_printName("glPopName");
        GL11.glPopName();
    }

    public static void qglPrioritizeTextures(int n, int textures, float priorities) {DEBUG_printName("glPrioritizeTextures");
        throw new TempDump.TODO_Exception();
    }

    private static void qglPrioritizeTextures(int n, IntBuffer textures, FloatBuffer priorities) {DEBUG_printName("glPrioritizeTextures");
        GL11.glPrioritizeTextures(textures, priorities);
    }

    //    @Deprecated
    public static /*PFNGLPROGRAMENVPARAMETER4FVARBPROC*/ void qglProgramEnvParameter4fvARB(int target, Enum index, final float[] params) {DEBUG_printName("glProgramEnvParameter4fvARB");//TODO:convert calls to floatbuffer
        qglProgramEnvParameter4fvARB(target, index.ordinal(), params);
    }

    public static /*PFNGLPROGRAMENVPARAMETER4FVARBPROC*/ void qglProgramEnvParameter4fvARB(int target, Enum index, final FloatBuffer params) {DEBUG_printName("glProgramEnvParameter4fvARB");
        ARBVertexProgram.glProgramEnvParameter4fvARB(target, index.ordinal(), params);
    }

    //    @Deprecated
    public static /*PFNGLPROGRAMENVPARAMETER4FVARBPROC*/ void qglProgramEnvParameter4fvARB(int target, int index, final float[] params) {DEBUG_printName("glProgramEnvParameter4fvARB");//TODO:convert calls to floatbuffer
        ARBVertexProgram.glProgramEnvParameter4fvARB(target, index, params);
//        qglProgramEnvParameter4fvARB(target, index, wrap(params));
    }

    public static /*PFNGLPROGRAMENVPARAMETER4FVARBPROC*/ void qglProgramEnvParameter4fvARB(int target, int index, final FloatBuffer params) {DEBUG_printName("glProgramEnvParameter4fvARB");
        ARBVertexProgram.glProgramEnvParameter4fvARB(target, index, params);
    }

    public static /*PFNGLPROGRAMLOCALPARAMETER4FVARBPROC*/ void qglProgramLocalParameter4fvARB(int target, int index, final FloatBuffer params) {DEBUG_printName("glProgramLocalParameter4fvARB");
        ARBVertexProgram.glProgramLocalParameter4fvARB(target, index, params);
    }
//extern PFNGLPROGRAMLOCALPARAMETER4FVARBPROC	qglProgramLocalParameter4fvARB;
//
// GL_EXT_depth_bounds_test

    public static  /*PFNGLPROGRAMSTRINGARBPROC*/void qglProgramStringARB(int target, int format, int len, final ByteBuffer string) {DEBUG_printName("glProgramStringARB");
        ARBVertexProgram.glProgramStringARB(target, format, string);
    }

    public static void qglPushAttrib(int mask) {DEBUG_printName("glPushAttrib");
        GL11.glPushAttrib(mask);
    }

    private static void qglPushClientAttrib(int mask) {DEBUG_printName("glPushClientAttrib");
        GL11.glPushClientAttrib(mask);
    }

    public static void qglPushMatrix() {DEBUG_printName("glPushMatrix");
        GL11.glPushMatrix();
    }

    private static void qglPushName(int name) {DEBUG_printName("glPushName");
        GL11.glPushName(name);
    }

    private static void qglRasterPos2d(double x, double y) {DEBUG_printName("glRasterPos2d");
        GL11.glRasterPos2d(x, y);
    }

    private static void qglRasterPos2dv(double[] v) {DEBUG_printName("glRasterPos2dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglRasterPos2f(float x, float y) {DEBUG_printName("glRasterPos2f");
        GL11.glRasterPos2f(x, y);
    }

    private static void qglRasterPos2fv(float[] v) {DEBUG_printName("glRasterPos2fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos2i(int x, int y) {DEBUG_printName("glRasterPos2i");
        GL11.glRasterPos2i(x, y);
    }

    private static void qglRasterPos2iv(int[] v) {DEBUG_printName("glRasterPos2iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos2s(short x, short y) {DEBUG_printName("glRasterPos2s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos2sv(short[] v) {DEBUG_printName("glRasterPos2sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos3d(double x, double y, double z) {DEBUG_printName("glRasterPos3d");
        GL11.glRasterPos3d(x, y, z);
    }

    private static void qglRasterPos3dv(double[] v) {DEBUG_printName("glRasterPos3dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos3f(float x, float y, float z) {DEBUG_printName("glRasterPos3f");
        GL11.glRasterPos3f(x, y, z);
    }

    private static void qglRasterPos3fv(float[] v) {DEBUG_printName("glRasterPos3fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos3i(int x, int y, int z) {DEBUG_printName("glRasterPos3i");
        GL11.glRasterPos3i(x, y, z);
    }

    private static void qglRasterPos3iv(int[] v) {DEBUG_printName("glRasterPos3iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos3s(short x, short y, short z) {DEBUG_printName("glRasterPos3s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos3sv(short[] v) {DEBUG_printName("glRasterPos3sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos4d(double x, double y, double z, double w) {DEBUG_printName("glRasterPos4d");
        GL11.glRasterPos4d(x, y, z, w);
    }

    private static void qglRasterPos4dv(double[] v) {DEBUG_printName("glRasterPos4dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos4f(float x, float y, float z, float w) {DEBUG_printName("glRasterPos4f");
        GL11.glRasterPos4f(x, y, z, w);
    }

    private static void qglRasterPos4fv(float[] v) {DEBUG_printName("glRasterPos4fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos4i(int x, int y, int z, int w) {DEBUG_printName("glRasterPos4i");
        GL11.glRasterPos4i(x, y, z, w);
    }

    private static void qglRasterPos4iv(int[] v) {DEBUG_printName("glRasterPos4iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos4s(short x, short y, short z, short w) {DEBUG_printName("glRasterPos4s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRasterPos4sv(short[] v) {DEBUG_printName("glRasterPos4sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglReadBuffer(int mode) {DEBUG_printName("glReadBuffer");
        GL11.glReadBuffer(mode);
    }

    public static void qglReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {DEBUG_printName("glReadPixels");
        GL11.glReadPixels(x, y, width, height, format, type, pixels);
    }

    private static void qglRectd(double x1, double y1, double x2, double y2) {DEBUG_printName("glRectd");
        GL11.glRectd(x1, y1, x2, y2);
    }

    private static void qglRectdv(double[] v1, double[] v2) {DEBUG_printName("glRectdv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRectf(float x1, float y1, float x2, float y2) {DEBUG_printName("glRectf");
        GL11.glRectf(x1, y1, x2, y2);
    }

    private static void qglRectfv(float[] v1, float[] v2) {DEBUG_printName("glRectfv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRecti(int x1, int y1, int x2, int y2) {DEBUG_printName("glRecti");
        GL11.glRecti(x1, y1, x2, y2);
    }

    private static void qglRectiv(int[] v1, int[] v2) {DEBUG_printName("glRectiv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRects(short x1, short y1, short x2, short y2) {DEBUG_printName("glRects");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglRectsv(short[] v1, short[] v2) {DEBUG_printName("glRectsv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static int qglRenderMode(int mode) {DEBUG_printName("glRenderMode");
        return GL11.glRenderMode(mode);
    }

    private static void qglRotated(double angle, double x, double y, double z) {DEBUG_printName("glRotated");
        GL11.glRotated(angle, x, y, z);
    }

    private static void qglRotatef(float angle, float x, float y, float z) {DEBUG_printName("glRotatef");
        GL11.glRotatef(angle, x, y, z);
    }

    private static void qglScaled(double x, double y, double z) {DEBUG_printName("glScaled");
        GL11.glScaled(x, y, z);
    }

    private static void qglScalef(float x, float y, float z) {DEBUG_printName("glScalef");
        GL11.glScalef(x, y, z);
    }

    public static void qglScissor(int x, int y, int width, int height) {DEBUG_printName("glScissor");
        GL11.glScissor(x, y, width, height);
    }

    private static void qglSelectBuffer(int size, IntBuffer buffer) {DEBUG_printName("glSelectBuffer");
        GL11.glSelectBuffer(buffer);
    }

    public static void qglShadeModel(int mode) {DEBUG_printName("glShadeModel");
        GL11.glShadeModel(mode);
    }

    public static void qglStencilFunc(int func, int ref, int mask) {DEBUG_printName("glStencilFunc");
        GL11.glStencilFunc(func, ref, mask);
    }

    public static void qglStencilMask(int mask) {DEBUG_printName("glStencilMask");
        GL11.glStencilMask(mask);
    }

    public static void qglStencilOp(int fail, int zfail, int zpass) {DEBUG_printName("glStencilOp");
        GL11.glStencilOp(fail, zfail, zpass);
    }

    private static void qglTexCoord1d(double s) {DEBUG_printName("glTexCoord1d");
        GL11.glTexCoord1d(s);
    }

    private static void qglTexCoord1dv(double[] v) {DEBUG_printName("glTexCoord1dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord1f(float s) {DEBUG_printName("glTexCoord1f");
        GL11.glTexCoord1f(s);
    }

    private static void qglTexCoord1fv(float[] v) {DEBUG_printName("glTexCoord1fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord1i(int s) {DEBUG_printName("glTexCoord1i");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord1iv(int[] v) {DEBUG_printName("glTexCoord1iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord1s(short s) {DEBUG_printName("glTexCoord1s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord1sv(short[] v) {DEBUG_printName("glTexCoord1sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord2d(double s, double t) {DEBUG_printName("glTexCoord2d");
        GL11.glTexCoord2d(s, t);
    }

    private static void qglTexCoord2dv(double[] v) {DEBUG_printName("glTexCoord2dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglTexCoord2f(float s, float t) {DEBUG_printName("glTexCoord2f");
        GL11.glTexCoord2f(s, t);
    }

    public static void qglTexCoord2fv(float[] v) {DEBUG_printName("glTexCoord2fv");
        qglTexCoord2f(v[0], v[1]);
    }

    private static void qglTexCoord2i(int s, int t) {DEBUG_printName("glTexCoord2i");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord2iv(int[] v) {DEBUG_printName("glTexCoord2iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord2s(short s, short t) {DEBUG_printName("glTexCoord2s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord2sv(short[] v) {DEBUG_printName("glTexCoord2sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord3d(double s, double t, double r) {DEBUG_printName("glTexCoord3d");
        GL11.glTexCoord3d(s, t, r);
    }

    private static void qglTexCoord3dv(double[] v) {DEBUG_printName("glTexCoord3dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord3f(float s, float t, float r) {DEBUG_printName("glTexCoord3f");
        GL11.glTexCoord3f(s, t, r);
    }

    private static void qglTexCoord3fv(float[] v) {DEBUG_printName("glTexCoord3fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord3i(int s, int t, int r) {DEBUG_printName("glTexCoord3i");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord3iv(int[] v) {DEBUG_printName("glTexCoord3iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord3s(short s, short t, short r) {DEBUG_printName("glTexCoord3s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord3sv(short[] v) {DEBUG_printName("glTexCoord3sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord4d(double s, double t, double r, double q) {DEBUG_printName("glTexCoord4d");
        GL11.glTexCoord4d(s, t, r, q);
    }

    private static void qglTexCoord4dv(double[] v) {DEBUG_printName("glTexCoord4dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord4f(float s, float t, float r, float q) {DEBUG_printName("glTexCoord4f");
        GL11.glTexCoord4f(s, t, r, q);
    }

    private static void qglTexCoord4fv(float[] v) {DEBUG_printName("glTexCoord4fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord4i(int s, int t, int r, int q) {DEBUG_printName("glTexCoord4i");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord4iv(int[] v) {DEBUG_printName("glTexCoord4iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord4s(short s, short t, short r, short q) {DEBUG_printName("glTexCoord4s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexCoord4sv(short[] v) {DEBUG_printName("glTexCoord4sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglTexCoordPointer(int size, int type, int stride, ByteBuffer pointer) {DEBUG_printName("glTexCoordPointer");
        GL11.glTexCoordPointer(size, type, stride, pointer);
    }

    @Deprecated
    public static void qglTexCoordPointer(int size, int type, int stride, float[] pointer) {DEBUG_printName("glTexCoordPointer");
//        GL11.glTexCoordPointer(size, stride, FloatBuffer.wrap(pointer));
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglTexCoordPointer(int size, int type, int stride, long pointer) {DEBUG_printName("glTexCoordPointer");
        GL11.glTexCoordPointer(size, type, stride, pointer);
    }

    private static void qglTexEnvf(int target, int pName, float param) {DEBUG_printName("glTexEnvf");
        GL11.glTexEnvf(target, pName, param);
    }

    public static void qglTexEnvfv(int target, int pName, FloatBuffer params) {DEBUG_printName("glTexEnvfv");
        GL11.glTexEnvfv(target, pName, params);
    }

    public static void qglTexEnvi(int target, int pName, int param) {DEBUG_printName("glTexEnvi");//ENVY!!
        GL11.glTexEnvi(target, pName, param);
    }

    private static void qglTexEnviv(int target, int pName, int[] params) {DEBUG_printName("glTexEnviv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexGend(int coord, int pName, double param) {DEBUG_printName("glTexGend");
        GL11.glTexGend(coord, pName, param);
    }

    private static void qglTexGendv(int coord, int pName, double[] params) {DEBUG_printName("glTexGendv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglTexGenf(int coord, int pName, float param) {DEBUG_printName("glTexGenf");
        GL11.glTexGenf(coord, pName, param);
    }

    public static void qglTexGenfv(int coord, int pName, float[] params) {DEBUG_printName("glTexGenfv");
        GL11.glTexGenfv(coord, pName, params);
    }

    private static void qglTexGeni(int coord, int pName, int param) {DEBUG_printName("glTexGeni");
        GL11.glTexGeni(coord, pName, param);
    }

    private static void qglTexGeniv(int coord, int pName, int[] params) {DEBUG_printName("glTexGeniv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, ByteBuffer pixels) {DEBUG_printName("glTexImage1D");
        GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels);
    }

    @Deprecated
    public static void qglTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, byte[] pixels) {DEBUG_printName("glTexImage2D");
        qglTexImage2D(target, level, internalformat, width, height, border, format, type, wrap(pixels));
        throw new UnsupportedOperationException();
    }

    public static void qglTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {DEBUG_printName("glTexImage2D");
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    // 3D textures
    public static void qglTexImage3D(int GLenum1, int GLint1, int GLint2, int GLsizei1, int GLsizei2, int GLsizei3, int GLint4, int GLenum2, int GLenum3, ByteBuffer GLvoid) {DEBUG_printName("glTexImage3D");
        GL12.glTexImage3D(GLenum1, GLint1, GLint2, GLsizei1, GLsizei2, GLsizei3, GLint4, GLenum2, GLenum3, GLvoid);
    }

    public static void qglTexParameterf(int target, int pName, float param) {DEBUG_printName("glTexParameterf");
        GL11.glTexParameterf(target, pName, param);
    }

    public static void qglTexParameterfv(int target, int pName, FloatBuffer params) {DEBUG_printName("glTexParameterfv");
        GL11.glTexParameterfv(target, pName, params);
    }

    public static void qglTexParameteri(int target, int pName, int param) {DEBUG_printName("glTexParameteri");
        GL11.glTexParameteri(target, pName, param);
    }

    private static void qglTexParameteriv(int target, int pName, int[] params) {DEBUG_printName("glTexParameteriv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, ByteBuffer pixels) {DEBUG_printName("glTexSubImage1D");
        GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels);
    }

    public static void qglTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer pixels) {DEBUG_printName("glTexSubImage2D");
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    private static void qglTranslated(double x, double y, double z) {DEBUG_printName("glTranslated");
        GL11.glTranslated(x, y, z);
    }

    private static void qglTranslatef(float x, float y, float z) {DEBUG_printName("glTranslatef");
        GL11.glTranslatef(x, y, z);
    }

    private static void qglVertex2d(double x, double y) {DEBUG_printName("glVertex2d");
        GL11.glVertex2d(x, y);
    }

    private static void qglVertex2dv(double[] v) {DEBUG_printName("glVertex2dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglVertex2f(float x, float y) {DEBUG_printName("glVertex2f");
        GL11.glVertex2f(x, y);
    }

    private static void qglVertex2fv(float[] v) {DEBUG_printName("glVertex2fv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex2i(int x, int y) {DEBUG_printName("glVertex2i");
        GL11.glVertex2i(x, y);
    }

    private static void qglVertex2iv(int[] v) {DEBUG_printName("glVertex2iv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex2s(short x, short y) {DEBUG_printName("glVertex2s");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex2sv(short[] v) {DEBUG_printName("glVertex2sv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex3d(double x, double y, double z) {DEBUG_printName("glVertex3d");
        GL11.glVertex3d(x, y, z);
    }

    private static void qglVertex3dv(double[] v) {DEBUG_printName("glVertex3dv");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglVertex3f(float x, float y, float z) {DEBUG_printName("glVertex3f");
        GL11.glVertex3f(x, y, z);
    }

    public static void qglVertex3fv(float[] v) {DEBUG_printName("glVertex3fv");
        qglVertex3f(v[0], v[1], v[2]);
    }

    private static void qglVertex3i(int x, int y, int z) {DEBUG_printName("glVertex3i");
        GL11.glVertex3i(x, y, z);
    }

    private static void qglVertex3iv(int[] v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex3s(short x, short y, short z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex3sv(short[] v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex4d(double x, double y, double z, double w) {DEBUG_printName("glVertex4d");
        GL11.glVertex4d(x, y, z, w);
    }

    private static void qglVertex4dv(double[] v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex4f(float x, float y, float z, float w) {DEBUG_printName("glVertex4f");
        GL11.glVertex4f(x, y, z, w);
    }

    private static void qglVertex4fv(float[] v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex4i(int x, int y, int z, int w) {DEBUG_printName("glVertex4i");
        GL11.glVertex4i(x, y, z, w);
    }

    private static void qglVertex4iv(int[] v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex4s(short x, short y, short z, short w) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void qglVertex4sv(short[] v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //
    // ARB_vertex_program / ARB_fragment_program
    @Deprecated
    private static void /*PFNGLVERTEXATTRIBPOINTERARBPROC*/ qglVertexAttribPointerARB(int index, int size, int type, boolean normalized, int stride, float[] pointer) {
//        GL20.glVertexAttribPointer(index, size, normalized, stride, FloatBuffer.wrap(pointer));
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglVertexAttribPointerARB(int index, int size, int type, boolean normalized, int stride, long pointer) {DEBUG_printName("glVertexAttribPointerARB");
        ARBVertexShader.glVertexAttribPointerARB(index, size, type, normalized, stride, pointer);
    }

    public static void qglVertexPointer(int size, int type, int stride, ByteBuffer pointer) {DEBUG_printName("glVertexPointer");
        GL11.glVertexPointer(size, type, stride, pointer);
    }

    @Deprecated
    public static void qglVertexPointer(int size, int type, int stride, float[] pointer) {
//        GL11.glVertexPointer(size, type, stride, 0);
//        GL11.glVertexPointer(size, stride, wrap(pointer));//TODO:use FloatBuffer.
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void qglVertexPointer(int size, int type, int stride, long pointer) {DEBUG_printName("glVertexPointer");
        GL11.glVertexPointer(size, type, stride, pointer);
    }

    public static void qglViewport(int x, int y, int width, int height) {DEBUG_printName("glViewport");
        GL11.glViewport(x, y, width, height);
    }
//
//    
//    
//    extern  int   ( WINAPI * qwglChoosePixelFormat )(HDC, CONST PIXELFORMATDESCRIPTOR *);
//extern  int   ( WINAPI * qwglDescribePixelFormat) (HDC, int, UINT, LPPIXELFORMATDESCRIPTOR);
//extern  int   ( WINAPI * qwglGetPixelFormat)(HDC);
//extern  BOOL  ( WINAPI * qwglSetPixelFormat)(HDC, int, CONST PIXELFORMATDESCRIPTOR *);
//extern  BOOL  ( WINAPI * qwglSwapBuffers)(HDC);
//
//extern BOOL  ( WINAPI * qwglCopyContext)(HGLRC, HGLRC, UINT);
//extern HGLRC ( WINAPI * qwglCreateContext)(HDC);
//extern HGLRC ( WINAPI * qwglCreateLayerContext)(HDC, int);
//extern BOOL  ( WINAPI * qwglDeleteContext)(HGLRC);
//extern HGLRC ( WINAPI * qwglGetCurrentContext)(VOID);
//extern HDC   ( WINAPI * qwglGetCurrentDC)(VOID);
//extern PROC  ( WINAPI * qwglGetProcAddress)(LPCSTR);
//extern BOOL  ( WINAPI * qwglMakeCurrent)(HDC, HGLRC);
//extern BOOL  ( WINAPI * qwglShareLists)(HGLRC, HGLRC);
//extern BOOL  ( WINAPI * qwglUseFontBitmaps)(HDC, DWORD, DWORD, DWORD);
//
//extern BOOL  ( WINAPI * qwglUseFontOutlines)(HDC, DWORD, DWORD, DWORD, FLOAT,
//                                           FLOAT, int, LPGLYPHMETRICSFLOAT);
//
//extern BOOL ( WINAPI * qwglDescribeLayerPlane)(HDC, int, int, UINT,
//                                            LPLAYERPLANEDESCRIPTOR);
//extern int  ( WINAPI * qwglSetLayerPaletteEntries)(HDC, int, int, int,
//                                                CONST COLORREF *);
//extern int  ( WINAPI * qwglGetLayerPaletteEntries)(HDC, int, int, int,
//                                                COLORREF *);
//extern BOOL ( WINAPI * qwglRealizeLayerPalette)(HDC, int, BOOL);
//extern BOOL ( WINAPI * qwglSwapLayerBuffers)(HDC, UINT);

    /**
     * @deprecated the calling functions should send ByteBuffers instead.
     */
    @Deprecated
    private static ByteBuffer wrap(final byte[] byteArray) {

        return (ByteBuffer) Nio.
        		newByteBuffer(byteArray.length | 16).
                put(byteArray).
                flip();
    }

    /**
     * @deprecated the calling functions should send FloatBuffers instead.
     */
    @Deprecated
    private static FloatBuffer wrap(final float[] floatArray) {

        return (FloatBuffer) Nio.
        		newFloatBuffer(floatArray.length | 16).
                put(floatArray).
                flip();
    }

    /**
     * @deprecated the calling functions should send IntBuffers instead.
     */
    @Deprecated
    private static IntBuffer wrap(final int[] intArray) {

        return (IntBuffer) Nio.
                newIntBuffer(intArray.length).
                put(intArray).
                flip();
    }

}
