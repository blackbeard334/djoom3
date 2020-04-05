package neo.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.EXTDepthBoundsTest;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

/**
 * so yeah, it's easier to use this class as an interface. rather than refactor
 * all the qwgl stuff to qgl and such.
 */
public class QGL {

	private final static boolean GL_DEBUG = false;

	{
		if (GL_DEBUG) {
			qglEnable(GL43.GL_DEBUG_OUTPUT);
		}
	}

	public static final boolean qGL_FALSE = false;

	public static final boolean qGL_TRUE = true;

	public static void checkGLError() {
		if (GL_DEBUG) {
			final ByteBuffer messageLog = BufferUtils.createByteBuffer(1000);
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

	public static void createCapabilities() {
		GL.createCapabilities();
	}

	public static void qglActiveTextureARB(int texture) {
		DEBUG_printName("glActiveTextureARB");
		ARBMultitexture.glActiveTextureARB(texture);
	}

	public static void qglAlphaFunc(int func, float ref) {
		DEBUG_printName("glAlphaFunc");
		GL11.glAlphaFunc(func, ref);
	}

	public static void qglArrayElement(int i) {
		DEBUG_printName("glArrayElement");
		GL11.glArrayElement(i);
	}

	public static void qglBegin(int mode) {
		DEBUG_printName("glBegin");
		GL11.glBegin(mode);
	}

	public static void qglBindBufferARB(int target, int buffer) {
		DEBUG_printName("glBindBufferARB");
		ARBVertexBufferObject.glBindBufferARB(target, buffer);
	}

	public static /* PFNGLBINDPROGRAMARBPROC */void qglBindProgramARB(int target, Enum<?> program) {
		DEBUG_printName("glBindProgramARB");
		qglBindProgramARB(target, program.ordinal());
	}

	public static /* PFNGLBINDPROGRAMARBPROC */void qglBindProgramARB(int target, int program) {
		DEBUG_printName("glBindProgramARB");
		ARBVertexProgram.glBindProgramARB(target, program);
	}

	public static void qglBindTexture(int target, int texture) {
		DEBUG_printName("glBindTexture");
		GL11.glBindTexture(target, texture);
	}

	public static void qglBlendFunc(int sFactor, int dFactor) {
		DEBUG_printName("glBlendFunc");
		GL11.glBlendFunc(sFactor, dFactor);
	}

	public static void qglBufferDataARB(int target, int size, ByteBuffer data, int usage) {
		DEBUG_printName("glBufferDataARB");
		ARBVertexBufferObject.glBufferDataARB(target, data, usage);
	}

	public static /* PFNGLBUFFERSUBDATAARBPROC */void qglBufferSubDataARB(int target, long offset, long size,
			ByteBuffer data) {
		DEBUG_printName("glBufferSubDataARB");
		ARBVertexBufferObject.glBufferSubDataARB(target, offset, data);
	}

	public static void qglClear(int mask) {
		DEBUG_printName("glClear");
		GL11.glClear(mask);
	}

	public static void qglClearColor(float red, float green, float blue, float alpha) {
		DEBUG_printName("glClearColor");
		GL11.glClearColor(red, green, blue, alpha);
	}

	public static void qglClearDepth(double depth) {
		DEBUG_printName("glClearDepth");
		GL11.glClearDepth(depth);
	}

	public static void qglClearStencil(int s) {
		DEBUG_printName("glClearStencil");
		GL11.glClearStencil(s);
	}

	public static void qglClientActiveTextureARB(int texture) {
		DEBUG_printName("glClientActiveTextureARB");
		ARBMultitexture.glClientActiveTextureARB(texture);
	}

	public static void qglColor3f(float red, float green, float blue) {
		DEBUG_printName("glColor3f");
		GL11.glColor3f(red, green, blue);
	}

	public static void qglColor3fv(float[] v) {
		DEBUG_printName("glColor3fv");
		qglColor3f(v[0], v[1], v[2]);
	}

	public static void qglColor3ubv(byte[] v) {
		DEBUG_printName("glColor3ubv");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static void qglColor4f(float red, float green, float blue, float alpha) {
		DEBUG_printName("glColor4f");
		GL11.glColor4f(red, green, blue, alpha);
	}

	public static void qglColor4fv(float[] v) {
		DEBUG_printName("glColor4fv");
		qglColor4f(v[0], v[1], v[2], v[3]);
	}

	public static void qglColor4ubv(byte[] v) {
		DEBUG_printName("glColor4ubv");
		GL11.glColor4ub(v[0], v[1], v[2], v[3]);
	}

	public static void qglColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		DEBUG_printName("glColorMask");
		GL11.glColorMask(red, green, blue, alpha);
	}

	public static void qglColorMask(int red, int green, int blue, int alpha) {
		qglColorMask(red != 0, green != 0, blue != 0, alpha != 0);
	}

	@Deprecated
	public static void qglColorPointer(int size, int type, int stride, byte[] pointer) {
		DEBUG_printName("glColorPointer");
		GL11.glColorPointer(size, type, stride, ByteBuffer.wrap(pointer));
	}

	public static void qglColorPointer(int size, int type, int stride, long pointer) {
		DEBUG_printName("glColorPointer");
		GL11.glColorPointer(size, type, stride, pointer);
	}

	public static void qglColorTableEXT(int target, int internalFormat, int width, int format, int type, byte[] data) {
		DEBUG_printName("glColorTableEXT");
		ARBImaging.glColorTable(target, internalFormat, width, format, type, ByteBuffer.wrap(data));
	}

	public static void /* PFNGLCOMPRESSEDTEXIMAGE2DARBPROC */ qglCompressedTexImage2DARB(int target, int level,
			int internalformat, int width, int height, int border, int imageSize, final ByteBuffer data) {
		DEBUG_printName("glCompressedTexImage2DARB");
		GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, data);
	}

	public static void qglCopyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height,
			int border) {
		DEBUG_printName("glCopyTexImage2D");
		GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border);
	}

	public static void qglCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width,
			int height) {
		DEBUG_printName("glCopyTexSubImage2D");
		GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
	}

	public static void qglCullFace(int mode) {
		DEBUG_printName("glCullFace");
		GL11.glCullFace(mode);
	}

	public static void qglDeleteTextures(int n, int texture) {
		DEBUG_printName("glDeleteTextures");
		GL11.glDeleteTextures(texture);
	}

	public static void /* PFNGLDEPTHBOUNDSEXTPROC */ qglDepthBoundsEXT(double zmin, double zmax) {
		DEBUG_printName("glDepthBoundsEXT");
		EXTDepthBoundsTest.glDepthBoundsEXT(zmin, zmax);
	}

	public static void qglDepthFunc(int func) {
		DEBUG_printName("glDepthFunc");
		GL11.glDepthFunc(func);
	}

	public static void qglDepthMask(boolean flag) {
		DEBUG_printName("glDepthMask");
		GL11.glDepthMask(flag);
	}

	public static void qglDepthRange(double zNear, double zFar) {
		DEBUG_printName("glDepthRange");
		GL11.glDepthRange(zNear, zFar);
	}

	public static void qglDisable(int cap) {
		DEBUG_printName("glDisable");
		GL11.glDisable(cap);
	}

	public static void qglDisableClientState(int array) {
		DEBUG_printName("glDisableClientState");
		GL11.glDisableClientState(array);
	}

	public static /* PFNGLDISABLEVERTEXATTRIBARRAYARBPROC */ void qglDisableVertexAttribArrayARB(int index) {
		DEBUG_printName("glDisableVertexAttribArrayARB");
		ARBVertexShader.glDisableVertexAttribArrayARB(index);
	}

	public static void qglDrawBuffer(int mode) {
		DEBUG_printName("glDrawBuffer");
		GL11.glDrawBuffer(mode);
	}

	public static void qglDrawElements(int mode, int count, int type, ByteBuffer indices) {
		DEBUG_printName("glDrawElements1");
		GL11.glDrawElements(mode, type, indices);
	}

	public static void qglDrawElements(int mode, int count, int type, int[] indices) {
		DEBUG_printName("glDrawElements2");
		GL11.glDrawElements(mode, (IntBuffer) wrap(indices).position(count).flip());
	}

	public static void qglDrawPixels(int width, int height, int format, int type, byte[][][] pixels) {
		DEBUG_printName("glDrawPixels");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static void qglDrawPixels(int width, int height, int format, int type, ByteBuffer pixels) {
		DEBUG_printName("glDrawPixels");
		GL11.glDrawPixels(width, height, format, type, pixels);
	}

	public static void qglEnable(int cap) {
		DEBUG_printName("glEnable");
		GL11.glEnable(cap);
	}

	public static void qglEnableClientState(int array) {
		DEBUG_printName("glEnableClientState");
		GL11.glEnableClientState(array);
	}

	public static /* PFNGLENABLEVERTEXATTRIBARRAYARBPROC */ void qglEnableVertexAttribArrayARB(int index) {
		DEBUG_printName("glEnableVertexAttribArrayARB");
		ARBVertexShader.glEnableVertexAttribArrayARB(index);
	}

	public static void qglEnd() {
		DEBUG_printName("glEnd");
		GL11.glEnd();

	}

	public static void qglFinish() {
		DEBUG_printName("glFinish");
		GL11.glFinish();
	}

	public static void qglFlush() {
		DEBUG_printName("glFlush");
		GL11.glFlush();
	}

	public static int qglGenBuffersARB() {
		DEBUG_printName("glGenBuffersARB");
		return ARBVertexBufferObject.glGenBuffersARB();
	}

	public static int qglGenTextures() {
		DEBUG_printName("glGenTextures");
		return GL11.glGenTextures();
	}

	public static void /* PFNGLGETCOMPRESSEDTEXIMAGEARBPROC */ qglGetCompressedTexImageARB(int target, int index,
			ByteBuffer img) {
		DEBUG_printName("glGetCompressedTexImageARB");
		ARBTextureCompression.glGetCompressedTexImageARB(target, index, img);
	}

	public static int qglGetError() {
		checkGLError();
		return GL11.glGetError();
	}

	public static void qglGetFloatv(int pName, FloatBuffer params) {
		DEBUG_printName("glGetFloatv");
		GL11.glGetFloatv(pName, params);
	}

	public static int qglGetInteger(int pName) {
		DEBUG_printName("glGetInteger");
		return GL11.glGetInteger(pName);
	}

	public static void qglGetIntegerv(int pName, IntBuffer params) {
		DEBUG_printName("glGetIntegerv");
		GL11.glGetIntegerv(pName, params);
	}

	public static String qglGetString(int name) {
		DEBUG_printName("glGetString");
		return GL11.glGetString(name);
	}

	public static String qglGetStringi(int name, int index) {
		DEBUG_printName("glGetStringi");
		return GL30.glGetStringi(name, index);
	}

	public static void qglGetTexImage(int target, int level, int format, int type, ByteBuffer pixels) {
		DEBUG_printName("glGetTexImage");
		GL11.glGetTexImage(target, level, format, type, pixels);
	}

	public static void qglLineWidth(float width) {
		DEBUG_printName("glLineWidth");
		GL11.glLineWidth(width);
	}

	public static void qglLoadIdentity() {
		DEBUG_printName("glLoadIdentity");
		GL11.glLoadIdentity();
	}

	public static void qglLoadMatrixf(float[] m) {
		DEBUG_printName("glLoadMatrixf");
		GL11.glLoadMatrixf(m);
	}

	public static void qglMatrixMode(int mode) {
		DEBUG_printName("glMatrixMode");
		GL11.glMatrixMode(mode);
	}

	public static void qglNormalPointer(int type, int stride, long pointer) {
		DEBUG_printName("glNormalPointer");
		GL11.glNormalPointer(type, stride, pointer);
	}

	public static void qglOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
		DEBUG_printName("glOrtho");
		GL11.glOrtho(left, right, bottom, top, zNear, zFar);
	}

	public static void qglPixelStorei(int pName, int param) {
		DEBUG_printName("glPixelStorei");
		GL11.glPixelStorei(pName, param);
	}

	public static void qglPointSize(float size) {
		DEBUG_printName("glPointSize");
		GL11.glPointSize(size);
	}

	public static void qglPolygonMode(int face, int mode) {
		DEBUG_printName("glPolygonMode");
		GL11.glPolygonMode(face, mode);
	}

	public static void qglPolygonOffset(float factor, float units) {
		DEBUG_printName("glPolygonOffset");
		GL11.glPolygonOffset(factor, units);
	}

	public static void qglPopAttrib() {
		DEBUG_printName("glPopAttrib");
		GL11.glPopAttrib();
	}

	public static void qglPopMatrix() {
		DEBUG_printName("glPopMatrix");
		GL11.glPopMatrix();
	}

	public static void qglPrioritizeTextures(int n, int textures, float priorities) {
		DEBUG_printName("glPrioritizeTextures");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static /* PFNGLPROGRAMENVPARAMETER4FVARBPROC */ void qglProgramEnvParameter4fvARB(int target, Enum<?> index,
			final float[] params) {
		DEBUG_printName("glProgramEnvParameter4fvARB");
		qglProgramEnvParameter4fvARB(target, index.ordinal(), params);
	}

	public static /* PFNGLPROGRAMENVPARAMETER4FVARBPROC */ void qglProgramEnvParameter4fvARB(int target, Enum<?> index,
			final FloatBuffer params) {
		DEBUG_printName("glProgramEnvParameter4fvARB");
		ARBVertexProgram.glProgramEnvParameter4fvARB(target, index.ordinal(), params);
	}

	public static /* PFNGLPROGRAMENVPARAMETER4FVARBPROC */ void qglProgramEnvParameter4fvARB(int target, int index,
			final float[] params) {
		DEBUG_printName("glProgramEnvParameter4fvARB");
		ARBVertexProgram.glProgramEnvParameter4fvARB(target, index, params);
	}

	public static /* PFNGLPROGRAMENVPARAMETER4FVARBPROC */ void qglProgramEnvParameter4fvARB(int target, int index,
			final FloatBuffer params) {
		DEBUG_printName("glProgramEnvParameter4fvARB");
		ARBVertexProgram.glProgramEnvParameter4fvARB(target, index, params);
	}

	public static /* PFNGLPROGRAMLOCALPARAMETER4FVARBPROC */ void qglProgramLocalParameter4fvARB(int target, int index,
			final FloatBuffer params) {
		DEBUG_printName("glProgramLocalParameter4fvARB");
		ARBVertexProgram.glProgramLocalParameter4fvARB(target, index, params);
	}

	public static /* PFNGLPROGRAMSTRINGARBPROC */void qglProgramStringARB(int target, int format, int len,
			final ByteBuffer string) {
		DEBUG_printName("glProgramStringARB");
		ARBVertexProgram.glProgramStringARB(target, format, string);
	}

	public static void qglPushAttrib(int mask) {
		DEBUG_printName("glPushAttrib");
		GL11.glPushAttrib(mask);
	}

	public static void qglPushMatrix() {
		DEBUG_printName("glPushMatrix");
		GL11.glPushMatrix();
	}

	public static void qglRasterPos2f(float x, float y) {
		DEBUG_printName("glRasterPos2f");
		GL11.glRasterPos2f(x, y);
	}

	public static void qglReadBuffer(int mode) {
		DEBUG_printName("glReadBuffer");
		GL11.glReadBuffer(mode);
	}

	public static void qglReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
		DEBUG_printName("glReadPixels");
		GL11.glReadPixels(x, y, width, height, format, type, pixels);
	}

	public static void qglScissor(int x, int y, int width, int height) {
		DEBUG_printName("glScissor");
		GL11.glScissor(x, y, width, height);
	}

	public static void qglShadeModel(int mode) {
		DEBUG_printName("glShadeModel");
		GL11.glShadeModel(mode);
	}

	public static void qglStencilFunc(int func, int ref, int mask) {
		DEBUG_printName("glStencilFunc");
		GL11.glStencilFunc(func, ref, mask);
	}

	public static void qglStencilMask(int mask) {
		DEBUG_printName("glStencilMask");
		GL11.glStencilMask(mask);
	}

	public static void qglStencilOp(int fail, int zfail, int zpass) {
		DEBUG_printName("glStencilOp");
		GL11.glStencilOp(fail, zfail, zpass);
	}

	public static void qglTexCoord2f(float s, float t) {
		DEBUG_printName("glTexCoord2f");
		GL11.glTexCoord2f(s, t);
	}

	public static void qglTexCoord2fv(float[] v) {
		DEBUG_printName("glTexCoord2fv");
		qglTexCoord2f(v[0], v[1]);
	}

	public static void qglTexCoordPointer(int size, int type, int stride, ByteBuffer pointer) {
		DEBUG_printName("glTexCoordPointer");
		GL11.glTexCoordPointer(size, type, stride, pointer);
	}

	@Deprecated
	public static void qglTexCoordPointer(int size, int type, int stride, float[] pointer) {
		DEBUG_printName("glTexCoordPointer");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static void qglTexCoordPointer(int size, int type, int stride, long pointer) {
		DEBUG_printName("glTexCoordPointer");
		GL11.glTexCoordPointer(size, type, stride, pointer);
	}

	public static void qglTexEnvfv(int target, int pName, FloatBuffer params) {
		DEBUG_printName("glTexEnvfv");
		GL11.glTexEnvfv(target, pName, params);
	}

	public static void qglTexEnvi(int target, int pName, int param) {
		DEBUG_printName("glTexEnvi");
		GL11.glTexEnvi(target, pName, param);
	}

	public static void qglTexGenf(int coord, int pName, float param) {
		DEBUG_printName("glTexGenf");
		GL11.glTexGenf(coord, pName, param);
	}

	public static void qglTexGenfv(int coord, int pName, float[] params) {
		DEBUG_printName("glTexGenfv");
		GL11.glTexGenfv(coord, pName, params);
	}

	@Deprecated
	public static void qglTexImage2D(int target, int level, int internalformat, int width, int height, int border,
			int format, int type, byte[] pixels) {
		DEBUG_printName("glTexImage2D");
		qglTexImage2D(target, level, internalformat, width, height, border, format, type, wrap(pixels));
		throw new UnsupportedOperationException();
	}

	public static void qglTexImage2D(int target, int level, int internalformat, int width, int height, int border,
			int format, int type, ByteBuffer pixels) {
		DEBUG_printName("glTexImage2D");
		GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
	}

	public static void qglTexImage3D(int GLenum1, int GLint1, int GLint2, int GLsizei1, int GLsizei2, int GLsizei3,
			int GLint4, int GLenum2, int GLenum3, ByteBuffer GLvoid) {
		DEBUG_printName("glTexImage3D");
		GL12.glTexImage3D(GLenum1, GLint1, GLint2, GLsizei1, GLsizei2, GLsizei3, GLint4, GLenum2, GLenum3, GLvoid);
	}

	public static void qglTexParameterf(int target, int pName, float param) {
		DEBUG_printName("glTexParameterf");
		GL11.glTexParameterf(target, pName, param);
	}

	public static void qglTexParameterfv(int target, int pName, FloatBuffer params) {
		DEBUG_printName("glTexParameterfv");
		GL11.glTexParameterfv(target, pName, params);
	}

	public static void qglTexParameteri(int target, int pName, int param) {
		DEBUG_printName("glTexParameteri");
		GL11.glTexParameteri(target, pName, param);
	}

	public static void qglTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
			int format, int type, ByteBuffer pixels) {
		DEBUG_printName("glTexSubImage2D");
		GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
	}

	public static void qglVertex2f(float x, float y) {
		DEBUG_printName("glVertex2f");
		GL11.glVertex2f(x, y);
	}

	public static void qglVertex3f(float x, float y, float z) {
		DEBUG_printName("glVertex3f");
		GL11.glVertex3f(x, y, z);
	}

	public static void qglVertex3fv(float[] v) {
		DEBUG_printName("glVertex3fv");
		qglVertex3f(v[0], v[1], v[2]);
	}

	public static void qglVertexAttribPointerARB(int index, int size, int type, boolean normalized, int stride,
			long pointer) {
		DEBUG_printName("glVertexAttribPointerARB");
		ARBVertexShader.glVertexAttribPointerARB(index, size, type, normalized, stride, pointer);
	}

	public static void qglVertexPointer(int size, int type, int stride, ByteBuffer pointer) {
		DEBUG_printName("glVertexPointer");
		GL11.glVertexPointer(size, type, stride, pointer);
	}

	@Deprecated
	public static void qglVertexPointer(int size, int type, int stride, float[] pointer) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static void qglVertexPointer(int size, int type, int stride, long pointer) {
		DEBUG_printName("glVertexPointer");
		GL11.glVertexPointer(size, type, stride, pointer);
	}

	public static void qglViewport(int x, int y, int width, int height) {
		DEBUG_printName("glViewport");
		GL11.glViewport(x, y, width, height);
	}

	/**
	 * @deprecated the calling functions should send ByteBuffers instead.
	 */
	@Deprecated
	private static ByteBuffer wrap(final byte[] byteArray) {

		return (ByteBuffer) BufferUtils.createByteBuffer(byteArray.length | 16).put(byteArray).flip();
	}

	/**
	 * @deprecated the calling functions should send FloatBuffers instead.
	 */
	@Deprecated
	private static FloatBuffer wrap(final float[] floatArray) {

		return (FloatBuffer) BufferUtils.createFloatBuffer(floatArray.length | 16).put(floatArray).flip();
	}

	/**
	 * @deprecated the calling functions should send IntBuffers instead.
	 */
	@Deprecated
	private static IntBuffer wrap(final int[] intArray) {

		return (IntBuffer) BufferUtils.createIntBuffer(intArray.length).put(intArray).flip();
	}

}
