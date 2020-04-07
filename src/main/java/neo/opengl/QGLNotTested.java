package neo.opengl;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.ARBShadowAmbient;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

/**
 * all the untested qgl stuff.
 * If successfully tested, move method to QGL!
 * The class is not visible, use QGL instead.
 */
class QGLNotTested {

	public static void qglAccum(int op, float value) {
		QGL.DEBUG_printName("glAccum");
		GL11.glAccum(op, value);
	}
										
	public static boolean qglAreTexturesResident(IntBuffer textures, ByteBuffer residences) {
		QGL.DEBUG_printName("glAreTexturesResident");
		return GL11.glAreTexturesResident(textures, residences);
	}
										
	public static void qglBitmap(int w, int h, float xOrig, float yOrig, float xInc, float yInc,
			ByteBuffer data) {
		QGL.DEBUG_printName("glBitmap");
		GL11.glBitmap(w, h, xOrig, yOrig, xInc, yInc, data);
	}
										
	public static void qglCallList(int list) {
		QGL.DEBUG_printName("glCallList");
		GL11.glCallList(list);
	}
										
	public static void qglCallLists(int type, ByteBuffer lists) {
		QGL.DEBUG_printName("glCallLists");
		GL11.glCallLists(type, lists);
	}
										
	public static void qglClearAccum(float red, float green, float blue, float alpha) {
		QGL.DEBUG_printName("glClearAccum");
		GL11.glClearAccum(red, green, blue, alpha);
	}
										
	public static void qglClearIndex(float index) {
		QGL.DEBUG_printName("glClearIndex");
		GL11.glClearIndex(index);
	}
										
	public static void qglClipPlane(int plane, DoubleBuffer equation) {
		QGL.DEBUG_printName("glClipPlane");
		GL11.glClipPlane(plane, equation);
	}

	public static void qglColor3b(byte red, byte green, byte blue) {
		QGL.DEBUG_printName("glColor3b");
		GL11.glColor3b(red, green, blue);
	}

	public static void qglColor3bv(ByteBuffer v) {
		QGL.DEBUG_printName("glColor3bv");
		GL11.glColor3bv(v);
	}

	public static void qglColor3d(double red, double green, double blue) {
		QGL.DEBUG_printName("glColor3d");
		GL11.glColor3d(red, green, blue);
	}

	public static void qglColor3dv(DoubleBuffer v) {
		QGL.DEBUG_printName("glColor3dv");
		GL11.glColor3dv(v);
	}

	public static void qglColor3i(int red, int green, int blue) {
		QGL.DEBUG_printName("glColor3i");
		GL11.glColor3i(red, green, blue);
	}

	public static void qglColor3iv(IntBuffer v) {
		QGL.DEBUG_printName("glColor3iv");
		GL11.glColor3iv(v);
	}

	public static void qglColor3s(short red, short green, short blue) {
		QGL.DEBUG_printName("glColor3s");
		GL11.glColor3s(red, green, blue);
	}

	public static void qglColor3sv(ShortBuffer v) {
		QGL.DEBUG_printName("glColor3sv");
		GL11.glColor3sv(v);
	}

	public static void qglColor3ub(byte red, byte green, byte blue) {
		QGL.DEBUG_printName("glColor3ub");
		GL11.glColor3ub(red, green, blue);
	}

	public static void qglColor3ui(int red, int green, int blue) {
		QGL.DEBUG_printName("glColor3ui");
		GL11.glColor3ui(red, green, blue);
	}

	public static void qglColor3uiv(IntBuffer v) {
		QGL.DEBUG_printName("glColor3uiv");
		GL11.glColor3uiv(v);
	}

	public static void qglColor3us(short red, short green, short blue) {
		QGL.DEBUG_printName("glColor3us");
		GL11.glColor3us(red, green, blue);
	}

	public static void qglColor3usv(ShortBuffer v) {
		QGL.DEBUG_printName("glColor3usv");
		GL11.glColor3usv(v);
	}

	public static void qglColor4b(byte red, byte green, byte blue, byte alpha) {
		QGL.DEBUG_printName("glColor4b");
		GL11.glColor4b(red, green, blue, alpha);
	}

	public static void qglColor4bv(ByteBuffer v) {
		QGL.DEBUG_printName("glColor4bv");
		GL11.glColor4bv(v);
	}

	public static void qglColor4d(double red, double green, double blue, double alpha) {
		QGL.DEBUG_printName("glColor4d");
		GL11.glColor4d(red, green, blue, alpha);
	}

	public static void qglColor4dv(DoubleBuffer v) {
		QGL.DEBUG_printName("glColor4dv");
		GL11.glColor4dv(v);
	}

	public static void qglColor4i(int red, int green, int blue, int alpha) {
		QGL.DEBUG_printName("glColor4i");
		GL11.glColor4i(red, green, blue, alpha);
	}

	public static void qglColor4iv(IntBuffer v) {
		QGL.DEBUG_printName("glColor4iv");
		GL11.glColor4iv(v);
	}

	public static void qglColor4s(short red, short green, short blue, short alpha) {
		QGL.DEBUG_printName("glColor4s");
		GL11.glColor4s(red, green, blue, alpha);
	}

	public static void qglColor4sv(ShortBuffer v) {
		QGL.DEBUG_printName("glColor4sv");
		GL11.glColor4sv(v);
	}

	public static void qglColor4ub(byte red, byte green, byte blue, byte alpha) {
		QGL.DEBUG_printName("glColor4ub");
		GL11.glColor4ub(red, green, blue, alpha);
	}

	public static void qglColor4ui(int red, int green, int blue, int alpha) {
		QGL.DEBUG_printName("glColor4ui");
		GL11.glColor4ui(red, green, blue, alpha);
	}


	public static void qglColor4uiv(IntBuffer v) {
		QGL.DEBUG_printName("glColor4uiv");
		GL11.glColor4uiv(v);
	}

	public static void qglColor4us(short red, short green, short blue, short alpha) {
		QGL.DEBUG_printName("glColor4us");
		GL11.glColor4us(red, green, blue, alpha);
	}

	public static void qglColor4usv(ShortBuffer v) {
		QGL.DEBUG_printName("glColor4usv");
		GL11.glColor4usv(v);
	}

	public static void qglColorMaterial(int face, int mode) {
		QGL.DEBUG_printName("glColorMaterial");
		GL11.glColorMaterial(face, mode);
	}

	/**
	 * 
	 * @param stage
	 * @param portion
	 * @param variable
	 * @param input
	 * @param mapping
	 * @param componentUsage
	 * 
	 * @see ARBShadowAmbient.GL_TEXTURE_COMPARE_FAIL_VALUE_ARB
	 * @see GL11.glTexEnv... 
	 * @see GL11.glTexGen... 
	 * @see GL11.glTexPar... 
	 * @see http://www.paulsprojects.net/tutorials/smt/smt.html
	 */
	public static void qglCombinerInputNV(int stage, int portion, int variable, int input, int mapping, int componentUsage) {
		QGL.DEBUG_printName("glCombinerInputNV");
		// TODO LWJGL 3 not supported.
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * 
	 * @param stage
	 * @param portion
	 * @param abOutput
	 * @param cdOutput
	 * @param sumOutput
	 * @param scale
	 * @param bias
	 * @param abDotProduct
	 * @param cdDotProduct
	 * @param muxSum
	 * 
	 * @see ARBShadowAmbient.GL_TEXTURE_COMPARE_FAIL_VALUE_ARB
	 * @see GL11.glTexEnv... 
	 * @see GL11.glTexGen... 
	 * @see GL11.glTexPar... 
	 * @see http://www.paulsprojects.net/tutorials/smt/smt.html
	 */
	public static void qglCombinerOutputNV(int stage, int portion, int abOutput, int cdOutput, int sumOutput, int scale,
			int bias, boolean abDotProduct, boolean cdDotProduct, boolean muxSum) {
		QGL.DEBUG_printName("glCombinerOutputNV");
		// TODO LWJGL 3 not supported.
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * 
	 * @param pname
	 * @param param
	 * 
	 * @see ARBShadowAmbient.GL_TEXTURE_COMPARE_FAIL_VALUE_ARB
	 * @see GL11.glTexEnv... 
	 * @see GL11.glTexGen... 
	 * @see GL11.glTexPar... 
	 * @see http://www.paulsprojects.net/tutorials/smt/smt.html
	 */
	public static void qglCombinerParameteriNV(int pname, int param) {
		QGL.DEBUG_printName("glCombinerParameteriNV");
		// TODO LWJGL 3 not supported.
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static void qglCompressedTexImage2DARB(int target, int level, int internalformat, int width, int height, ByteBuffer data) {
		QGL.DEBUG_printName("glCompressedTexImage2DARB");
		ARBTextureCompression.glCompressedTexImage2DARB(target, level, internalformat, width, height, data);
	}

	public static void qglCompressedTexImage2DARB(int target, int level, int internalformat, int width, int height, int border,
			int imageSize, long data) {
		QGL.DEBUG_printName("glCompressedTexImage2DARB");
		ARBTextureCompression.glCompressedTexImage2DARB(target, level, internalformat, width, height, border, imageSize, data);
	}

	public static void qglCopyPixels(int x, int y, int width, int height, int type) {
		QGL.DEBUG_printName("glCopyPixels");
		GL11.glCopyPixels(x, y, width, height, type);
	}

	public static void qglCopyTexImage1D(int target, int level, int internalFormat, int x, int y, int width, int border) {
		QGL.DEBUG_printName("glCopyTexImage1D");
		GL11.glCopyTexImage1D(target, level, internalFormat, x, y, width, border);
	}

	public static void qglCopyTexSubImage1D(int target, int level, int xoffset, int x, int y, int width) {
		QGL.DEBUG_printName("glCopyTexSubImage1D");
		GL11.glCopyTexSubImage1D(target, level, xoffset, x, y, width);
	}

	public static void qglDeleteLists(int list, int range) {
		QGL.DEBUG_printName("glDeleteLists");
		GL11.glDeleteLists(list, range);
	}

	public static void qglDeleteTextures(IntBuffer textures) {
		QGL.DEBUG_printName("glDeleteTextures");
		GL11.glDeleteTextures(textures);
	}

	public static void qglDrawArrays(int mode, int first, int count) {
		QGL.DEBUG_printName("glDrawArrays");
		GL11.glDrawArrays(mode, first, count);
	}

	public static void qglEdgeFlag(boolean flag) {
		QGL.DEBUG_printName("glEdgeFlag");
		GL11.glEdgeFlag(flag);
	}

	public static void qglEdgeFlagv(ByteBuffer flag) {
		QGL.DEBUG_printName("glEdgeFlagv");
		GL11.glEdgeFlagv(flag);
	}

	public static void qglEndList() {
		QGL.DEBUG_printName("glEndList");
		GL11.glEndList();
	}

	public static void qglEvalCoord1d(double u) {
		QGL.DEBUG_printName("glEvalCoord1d");
		GL11.glEvalCoord1d(u);
	}

	public static void qglEvalCoord1dv(DoubleBuffer u) {
		QGL.DEBUG_printName("glEvalCoord1dv");
		GL11.glEvalCoord1dv(u);
	}

	public static void qglEvalCoord1f(float u) {
		QGL.DEBUG_printName("glEvalCoord1f");
		GL11.glEvalCoord1f(u);
	}

	public static void qglEvalCoord1fv(FloatBuffer u) {
		QGL.DEBUG_printName("glEvalCoord1fv");
		GL11.glEvalCoord1fv(u);
	}

	public static void qglEvalCoord2d(double u, double v) {
		QGL.DEBUG_printName("glEvalCoord2d");
		GL11.glEvalCoord2d(u, v);
	}

	public static void qglEvalCoord2dv(DoubleBuffer u) {
		QGL.DEBUG_printName("glEvalCoord2dv");
		GL11.glEvalCoord2dv(u);
	}

	public static void qglEvalCoord2f(float u, float v) {
		QGL.DEBUG_printName("glEvalCoord2f");
		GL11.glEvalCoord2f(u, v);
	}

	public static void qglEvalCoord2fv(FloatBuffer u) {
		QGL.DEBUG_printName("glEvalCoord2fv");
		GL11.glEvalCoord2fv(u);
	}

	public static void qglEvalMesh1(int mode, int i1, int i2) {
		QGL.DEBUG_printName("glEvalMesh1");
		GL11.glEvalMesh1(mode, i1, i2);
	}

	public static void qglEvalMesh2(int mode, int i1, int i2, int j1, int j2) {
		QGL.DEBUG_printName("glEvalMesh2");
		GL11.glEvalMesh2(mode, i1, i2, j1, j2);
	}

	public static void qglEvalPoint1(int i) {
		QGL.DEBUG_printName("glEvalPoint1");
		GL11.glEvalPoint1(i);
	}

	public static void qglEvalPoint2(int i, int j) {
		QGL.DEBUG_printName("glEvalPoint2");
		GL11.glEvalPoint2(i, j);
	}

	public static void qglFeedbackBuffer(int size, int type, FloatBuffer buffer) {
		QGL.DEBUG_printName("glFeedbackBuffer");
		GL11.glFeedbackBuffer(type, buffer);
	}

	/**
	 * 
	 * @param variable
	 * @param input
	 * @param mapping
	 * @param componentUsage
	 * 
	 * @see ARBShadowAmbient.GL_TEXTURE_COMPARE_FAIL_VALUE_ARB
	 * @see GL11.glTexEnv... 
	 * @see GL11.glTexGen... 
	 * @see GL11.glTexPar... 
	 * @see http://www.paulsprojects.net/tutorials/smt/smt.html
	 */
	public static void qglFinalCombinerInputNV(int variable, int input, int mapping, int componentUsage) {
		QGL.DEBUG_printName("glFinalCombinerInputNV");
		// TODO LWJGL 3 not supported.
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static void qglFogf(int pname, float param) {
		QGL.DEBUG_printName("glFogf");
		GL11.glFogf(pname, param);
	}

	public static void qglFogfv(int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glFogfv");
		GL11.glFogfv(pname, params);
	}

	public static void qglFogi(int pname, int param) {
		QGL.DEBUG_printName("glFogi");
		GL11.glFogi(pname, param);
	}

	public static void qglFogiv(int pname, IntBuffer params) {
		QGL.DEBUG_printName("glFogiv");
		GL11.glFogiv(pname, params);
	}

	public static void qglFrontFace(int mode) {
		QGL.DEBUG_printName("glFrontFace");
		GL11.glFrontFace(mode);
	}

	public static void qglFrustum(double left, double right, double bottom, double top, double zNear, double zFar) {
		QGL.DEBUG_printName("glFrustum");
		GL11.glFrustum(left, right, bottom, top, zNear, zFar);
	}

	public static void qglGenBuffersARB(IntBuffer buffers) {
		QGL.DEBUG_printName("glGenBuffersARB");
		ARBVertexBufferObject.glGenBuffersARB(buffers);
	}

	public static int qglGenLists(int s) {
		QGL.DEBUG_printName("glGenLists");
		return GL11.glGenLists(s);
	}

	public static void qglGenTextures(IntBuffer textures) {
		QGL.DEBUG_printName("glGenTextures");
		GL11.glGenTextures(textures);
	}

	public static void qglGetBooleanv(int pname, ByteBuffer params) {
		QGL.DEBUG_printName("glGetBooleanv");
		GL11.glGetBooleanv(pname, params);
	}

	public static void qglGetClipPlane(int plane, DoubleBuffer equation) {
		QGL.DEBUG_printName("glGetClipPlane");
		GL11.glGetClipPlane(plane, equation);
	}

	public static void qglGetDoublev(int pname, DoubleBuffer params) {
		QGL.DEBUG_printName("glGetDoublev");
		GL11.glGetDoublev(pname, params);
	}

	public static void qglGetLightfv(int light, int pname, FloatBuffer data) {
		QGL.DEBUG_printName("glGetLightfv");
		GL11.glGetLightfv(light, pname, data);
	}

	public static void qglGetLightiv(int light, int pname, IntBuffer data) {
		QGL.DEBUG_printName("glGetLightiv");
		GL11.glGetLightiv(light, pname, data);
	}

	public static void qglGetMapdv(int target, int query, DoubleBuffer data) {
		QGL.DEBUG_printName("glGetMapdv");
		GL11.glGetMapdv(target, query, data);
	}

	public static void qglGetMapfv(int target, int query, FloatBuffer data) {
		QGL.DEBUG_printName("glGetMapfv");
		GL11.glGetMapfv(target, query, data);
	}

	public static void qglGetMapiv(int target, int query, IntBuffer data) {
		QGL.DEBUG_printName("glGetMapiv");
		GL11.glGetMapiv(target, query, data);
	}

	public static void qglGetMaterialfv(int face, int pname, FloatBuffer data) {
		QGL.DEBUG_printName("glGetMaterialfv");
		GL11.glGetMaterialfv(face, pname, data);
	}

	public static void qglGetMaterialiv(int face, int pname, IntBuffer data) {
		QGL.DEBUG_printName("glGetMaterialiv");
		GL11.glGetMaterialiv(face, pname, data);
	}

	public static void qglGetPixelMapfv(int map, FloatBuffer data) {
		QGL.DEBUG_printName("glGetPixelMapfv");
		GL11.glGetPixelMapfv(map, data);
	}

	public static void qglGetPixelMapuiv(int map, IntBuffer data) {
		QGL.DEBUG_printName("glGetPixelMapuiv");
		GL11.glGetPixelMapuiv(map, data);
	}

	public static void qglGetPixelMapusv(int map, ShortBuffer data) {
		QGL.DEBUG_printName("glGetPixelMapusv");
		GL11.glGetPixelMapusv(map, data);
	}

	public static long qglGetPointer(int pname) {
		QGL.DEBUG_printName("glGetPointer");
		return GL11.glGetPointer(pname);
	}

	public static void qglGetPolygonStipple(ByteBuffer pattern) {
		QGL.DEBUG_printName("glGetPolygonStipple");
		GL11.glGetPolygonStipple(pattern);
	}

	public static void qglGetTexEnvfv(int env, int pname, FloatBuffer data) {
		QGL.DEBUG_printName("glGetTexEnvfv");
		GL11.glGetTexEnvfv(env, pname, data);
	}

	public static void qglGetTexEnviv(int env, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetTexEnviv");
		GL11.glGetTexEnviv(env, pname, params);
	}

	public static void qglGetTexGendv(int coord, int pname, DoubleBuffer data) {
		QGL.DEBUG_printName("glGetTexGendv");
		GL11.glGetTexGendv(coord, pname, data);
	}

	public static void qglGetTexGenfv(int coord, int pname, FloatBuffer data) {
		QGL.DEBUG_printName("glGetTexGenfv");
		GL11.glGetTexGenfv(coord, pname, data);
	}

	public static void qglGetTexGeniv(int coord, int pname, IntBuffer data) {
		QGL.DEBUG_printName("glGetTexGeniv");
		GL11.glGetTexGeniv(coord, pname, data);
	}

	public static void qglGetTexLevelParameterfv(int target, int level, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glGetTexLevelParameterfv");
		GL11.glGetTexLevelParameterfv(target, level, pname, params);
	}

	public static void qglGetTexLevelParameteriv(int target, int level, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetTexLevelParameteriv");
		GL11.glGetTexLevelParameteriv(target, level, pname, params);
	}

	public static void qglGetTexParameterfv(int target, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glGetTexParameterfv");
		GL11.glGetTexParameterfv(target, pname, params);
	}

	public static void qglGetTexParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetTexParameteriv");
		GL11.glGetTexParameteriv(target, pname, params);
	}

	public static void qglHint(int target, int hint) {
		QGL.DEBUG_printName("glHint");
		GL11.glHint(target, hint);
	}

	public static void qglIndexd(double index) {
		QGL.DEBUG_printName("glIndexd");
		GL11.glIndexd(index);
	}

	public static void qglIndexdv(DoubleBuffer index) {
		QGL.DEBUG_printName("glIndexdv");
		GL11.glIndexdv(index);
	}

	public static void qglIndexfv(FloatBuffer index) {
		QGL.DEBUG_printName("glIndexfv");
		GL11.glIndexfv(index);
	}

	public static void qglIndexi(int index) {
		QGL.DEBUG_printName("glIndexi");
		GL11.glIndexi(index);
	}

	public static void qglIndexiv(IntBuffer index) {
		QGL.DEBUG_printName("glIndexiv");
		GL11.glIndexiv(index);
	}

	public static void qglIndexMask(int mask) {
		QGL.DEBUG_printName("glIndexMask");
		GL11.glIndexMask(mask);
	}

	public static void qglIndexPointer(int type, int stride, ByteBuffer pointer) {
		QGL.DEBUG_printName("glIndexPointer");
		GL11.glIndexPointer(type, stride, pointer);
	}

	public static void qglIndexsv(ShortBuffer index) {
		QGL.DEBUG_printName("glIndexsv");
		GL11.glIndexsv(index);
	}

	public static void qglIndexub(byte index) {
		QGL.DEBUG_printName("glIndexub");
		GL11.glIndexub(index);
	}

	public static void qglIndexubv(ByteBuffer index) {
		QGL.DEBUG_printName("glIndexubv");
		GL11.glIndexubv(index);
	}

	public static void qglInitNames() {
		QGL.DEBUG_printName("glInitNames");
		GL11.glInitNames();
	}

	public static void qglInterleavedArrays(int format, int stride, ByteBuffer pointer) {
		QGL.DEBUG_printName("glInterleavedArrays");
		GL11.glInterleavedArrays(format, stride, pointer);
	}

	public static boolean qglIsEnabled(int cap) {
		QGL.DEBUG_printName("glIsEnabled");
		return GL11.glIsEnabled(cap);
	}

	public static boolean qglIsList(int list) {
		QGL.DEBUG_printName("glIsList");
		return GL11.glIsList(list);
	}

	public static boolean qglIsTexture(int texture) {
		QGL.DEBUG_printName("glIsTexture");
		return GL11.glIsTexture(texture);
	}

	public static void qglLightf(int light, int pname, float param) {
		QGL.DEBUG_printName("glLightf");
		GL11.glLightf(light, pname, param);
	}

	public static void qglLightfv(int light, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glLightfv");
		GL11.glLightfv(light, pname, params);
	}

	public static void qglLighti(int light, int pname, int param) {
		QGL.DEBUG_printName("glLighti");
		GL11.glLighti(light, pname, param);
	}

	public static void qglLightiv(int light, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glLightiv");
		GL11.glLightiv(light, pname, params);
	}

	public static void qglLightModelf(int pname, float param) {
		QGL.DEBUG_printName("glLightModelf");
		GL11.glLightModelf(pname, param);
	}

	public static void qglLightModelfv(int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glLightModelfv");
		GL11.glLightModelfv(pname, params);
	}

	public static void qglLightModeli(int pname, int param) {
		QGL.DEBUG_printName("glLightModeli");
		GL11.glLightModeli(pname, param);
	}

	public static void qglLightModeliv(int pname, IntBuffer params) {
		QGL.DEBUG_printName("glLightModeliv");
		GL11.glLightModeliv(pname, params);
	}

	public static void qglLineStipple(int factor, short pattern) {
		QGL.DEBUG_printName("glLineStipple");
		GL11.glLineStipple(factor, pattern);
	}

	public static void qglListBase(int base) {
		QGL.DEBUG_printName("glListBase");
		GL11.glListBase(base);
	}

	public static void qglLoadMatrixd(DoubleBuffer m) {
		QGL.DEBUG_printName("glLoadMatrixd");
		GL11.glLoadMatrixd(m);
	}

	public static void qglLoadName(int name) {
		QGL.DEBUG_printName("glLoadName");
		GL11.glLoadName(name);
	}

	public static void qglLogicOp(int op) {
		QGL.DEBUG_printName("glLogicOp");
		GL11.glLogicOp(op);
	}

	public static void qglMap1d(int target, double u1, double u2, int stride, int order, DoubleBuffer points) {
		QGL.DEBUG_printName("glMap1d");
		GL11.glMap1d(target, u1, u2, stride, order, points);
	}

	public static void qglMap1f(int target, float u1, float u2, int stride, int order, FloatBuffer points) {
		QGL.DEBUG_printName("glMap1f");
		GL11.glMap1f(target, u1, u2, stride, order, points);
	}

	public static void qglMap2d(int target, double u1, double u2, int ustride, int uorder, double v1, double v2, int vstride,
			int vorder, DoubleBuffer points) {
		QGL.DEBUG_printName("glMap2d");
		GL11.glMap2d(target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points);
	}

	public static void qglMap2f(int target, float u1, float u2, int ustride, int uorder, float v1, float v2, int vstride,
			int vorder, FloatBuffer points) {
		QGL.DEBUG_printName("glMap2f");
		GL11.glMap2f(target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points);
	}

	public static void qglMapGrid1d(int n, double u1, double u2) {
		QGL.DEBUG_printName("glMapGrid1d");
		GL11.glMapGrid1d(n, u1, u2);
	}

	public static void qglMapGrid1f(int n, float u1, float u2) {
		QGL.DEBUG_printName("glMapGrid1f");
		GL11.glMapGrid1f(n, u1, u2);
	}

	public static void qglMapGrid2d(int un, double u1, double u2, int vn, double v1, double v2) {
		QGL.DEBUG_printName("glMapGrid2d");
		GL11.glMapGrid2d(un, u1, u2, vn, v1, v2);
	}

	public static void qglMapGrid2f(int un, float u1, float u2, int vn, float v1, float v2) {
		QGL.DEBUG_printName("glMapGrid2f");
		GL11.glMapGrid2f(un, u1, u2, vn, v1, v2);
	}

	public static void qglMaterialfv(int face, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glMaterialfv");
		GL11.glMaterialfv(face, pname, params);
	}

	public static void qglMaterialiv(int face, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glMaterialiv");
		GL11.glMaterialiv(face, pname, params);
	}

	public static void qglMultMatrixd(DoubleBuffer m) {
		QGL.DEBUG_printName("glMultMatrixd");
		GL11.glMultMatrixd(m);
	}

	public static void qglMultMatrixf(FloatBuffer m) {
		QGL.DEBUG_printName("glMultMatrixf");
		GL11.glMultMatrixf(m);
	}

	public static void qglNewList(int n, int mode) {
		QGL.DEBUG_printName("glNewList");
		GL11.glNewList(n, mode);
	}

	public static void qglNormal3b(byte nx, byte ny, byte nz) {
		QGL.DEBUG_printName("glNormal3b");
		GL11.glNormal3b(nx, ny, nz);
	}

	public static void qglNormal3bv(ByteBuffer v) {
		QGL.DEBUG_printName("glNormal3bv");
		GL11.glNormal3bv(v);
	}

	public static void qglNormal3d(double nx, double ny, double nz) {
		QGL.DEBUG_printName("glNormal3d");
		GL11.glNormal3d(nx, ny, nz);
	}

	public static void qglNormal3dv(DoubleBuffer v) {
		QGL.DEBUG_printName("glNormal3dv");
		GL11.glNormal3dv(v);
	}

	public static void qglNormal3f(float nx, float ny, float nz) {
		QGL.DEBUG_printName("glNormal3f");
		GL11.glNormal3f(nx, ny, nz);
	}

	public static void qglNormal3fv(FloatBuffer v) {
		QGL.DEBUG_printName("glNormal3fv");
		GL11.glNormal3fv(v);
	}

	public static void qglNormal3i(int nx, int ny, int nz) {
		QGL.DEBUG_printName("glNormal3i");
		GL11.glNormal3i(nx, ny, nz);
	}

	public static void qglNormal3iv(IntBuffer v) {
		QGL.DEBUG_printName("glNormal3iv");
		GL11.glNormal3iv(v);
	}

	public static void qglNormal3s(short nx, short ny, short nz) {
		QGL.DEBUG_printName("glNormal3s");
		GL11.glNormal3s(nx, ny, nz);
	}

	public static void qglNormal3sv(ShortBuffer v) {
		QGL.DEBUG_printName("glNormal3sv");
		GL11.glNormal3sv(v);
	}

	public static void qglPassThrough(float token) {
		QGL.DEBUG_printName("glPassThrough");
		GL11.glPassThrough(token);
	}

	public static void qglPixelMapfv(int mapsize, FloatBuffer values) {
		QGL.DEBUG_printName("glPixelMapfv");
		GL11.glPixelMapfv(mapsize, values);
	}

	public static void qglPixelMapuiv(int mapsize, IntBuffer values) {
		QGL.DEBUG_printName("glPixelMapuiv");
		GL11.glPixelMapuiv(mapsize, values);
	}

	public static void qglPixelMapusv(int mapsize, ShortBuffer values) {
		QGL.DEBUG_printName("glPixelMapusv");
		GL11.glPixelMapusv(mapsize, values);
	}

	public static void qglPixelStoref(int pname, float param) {
		QGL.DEBUG_printName("glPixelStoref");
		GL11.glPixelStoref(pname, param);
	}

	public static void qglPixelTransferf(int pname, float param) {
		QGL.DEBUG_printName("glPixelTransferf");
		GL11.glPixelTransferf(pname, param);
	}

	public static void qglPixelTransferi(int pname, int param) {
		QGL.DEBUG_printName("glPixelTransferi");
		GL11.glPixelTransferi(pname, param);
	}

	public static void qglPixelZoom(float xfactor, float yfactor) {
		QGL.DEBUG_printName("glPixelZoom");
		GL11.glPixelZoom(xfactor, yfactor);
	}

	public static void qglPolygonStipple(ByteBuffer pattern) {
		QGL.DEBUG_printName("glPolygonStipple");
		GL11.glPolygonStipple(pattern);
	}

	public static void qglPopClientAttrib() {
		QGL.DEBUG_printName("glPopClientAttrib");
		GL11.glPopClientAttrib();
	}

	public static void qglPopName() {
		QGL.DEBUG_printName("glPopName");
		GL11.glPopName();
	}

	public static void qglPushClientAttrib(int mask) {
		QGL.DEBUG_printName("glPushClientAttrib");
		GL11.glPushClientAttrib(mask);
	}

	public static void qglPushName(int name) {
		QGL.DEBUG_printName("glPushName");
		GL11.glPushName(name);
	}

	public static void qglRasterPos2d(double x, double y) {
		QGL.DEBUG_printName("glRasterPos2d");
		GL11.glRasterPos2d(x, y);
	}

	public static void qglRasterPos2dv(DoubleBuffer coords) {
		QGL.DEBUG_printName("glRasterPos2dv");
		GL11.glRasterPos2dv(coords);
	}

	public static void qglRasterPos2fv(FloatBuffer coords) {
		QGL.DEBUG_printName("glRasterPos2fv");
		GL11.glRasterPos2fv(coords);
	}

	public static void qglRasterPos2i(int x, int y) {
		QGL.DEBUG_printName("glRasterPos2i");
		GL11.glRasterPos2i(x, y);
	}

	public static void qglRasterPos2iv(IntBuffer coords) {
		QGL.DEBUG_printName("glRasterPos2iv");
		GL11.glRasterPos2iv(coords);
	}

	public static void qglRasterPos2s(short x, short y) {
		QGL.DEBUG_printName("glRasterPos2s");
		GL11.glRasterPos2s(x, y);
	}

	public static void qglRasterPos2sv(ShortBuffer coords) {
		QGL.DEBUG_printName("glRasterPos2sv");
		GL11.glRasterPos2sv(coords);
	}

	public static void qglRasterPos3d(double x, double y, double z) {
		QGL.DEBUG_printName("glRasterPos3d");
		GL11.glRasterPos3d(x, y, z);
	}

	public static void qglRasterPos3dv(DoubleBuffer coords) {
		QGL.DEBUG_printName("glRasterPos3dv");
		GL11.glRasterPos3dv(coords);
	}

	public static void qglRasterPos3f(float x, float y, float z) {
		QGL.DEBUG_printName("glRasterPos3f");
		GL11.glRasterPos3f(x, y, z);
	}

	public static void qglRasterPos3fv(FloatBuffer coords) {
		QGL.DEBUG_printName("glRasterPos3fv");
		GL11.glRasterPos3fv(coords);
	}

	public static void qglRasterPos3i(int x, int y, int z) {
		QGL.DEBUG_printName("glRasterPos3i");
		GL11.glRasterPos3i(x, y, z);
	}

	public static void qglRasterPos3iv(IntBuffer coords) {
		QGL.DEBUG_printName("glRasterPos3iv");
		GL11.glRasterPos3iv(coords);
	}

	public static void qglRasterPos3s(short x, short y, short z) {
		QGL.DEBUG_printName("glRasterPos3s");
		GL11.glRasterPos3s(x, y, z);
	}

	public static void qglRasterPos3sv(ShortBuffer coords) {
		QGL.DEBUG_printName("glRasterPos3sv");
		GL11.glRasterPos3sv(coords);
	}

	public static void qglRasterPos4d(double x, double y, double z, double w) {
		QGL.DEBUG_printName("glRasterPos4d");
		GL11.glRasterPos4d(x, y, z, w);
	}

	public static void qglRasterPos4dv(DoubleBuffer coords) {
		QGL.DEBUG_printName("glRasterPos4dv");
		GL11.glRasterPos4dv(coords);
	}

	public static void qglRasterPos4f(float x, float y, float z, float w) {
		QGL.DEBUG_printName("glRasterPos4f");
		GL11.glRasterPos4f(x, y, z, w);
	}

	public static void qglRasterPos4fv(FloatBuffer coords) {
		QGL.DEBUG_printName("glRasterPos4fv");
		GL11.glRasterPos4fv(coords);
	}

	public static void qglRasterPos4i(int x, int y, int z, int w) {
		QGL.DEBUG_printName("glRasterPos4i");
		GL11.glRasterPos4i(x, y, z, w);
	}

	public static void qglRasterPos4iv(IntBuffer coords) {
		QGL.DEBUG_printName("glRasterPos4iv");
		GL11.glRasterPos4iv(coords);
	}

	public static void qglRasterPos4s(short x, short y, short z, short w) {
		QGL.DEBUG_printName("glRasterPos4s");
		GL11.glRasterPos4s(x, y, z, w);
	}

	public static void qglRasterPos4sv(ShortBuffer coords) {
		QGL.DEBUG_printName("glRasterPos4sv");
		GL11.glRasterPos4sv(coords);
	}

	public static void qglRectd(double x1, double y1, double x2, double y2) {
		QGL.DEBUG_printName("glRectd");
		GL11.glRectd(x1, y1, x2, y2);
	}

	public static void qglRectdv(DoubleBuffer v1, DoubleBuffer v2) {
		QGL.DEBUG_printName("glRectdv");
		GL11.glRectdv(v1, v2);
	}

	public static void qglRectf(float x1, float y1, float x2, float y2) {
		QGL.DEBUG_printName("glRectf");
		GL11.glRectf(x1, y1, x2, y2);
	}

	public static void qglRectfv(FloatBuffer v1, FloatBuffer v2) {
		QGL.DEBUG_printName("glRectfv");
		GL11.glRectfv(v1, v2);
	}

	public static void qglRecti(int x1, int y1, int x2, int y2) {
		QGL.DEBUG_printName("glRecti");
		GL11.glRecti(x1, y1, x2, y2);
	}

	public static void qglRectiv(IntBuffer v1, IntBuffer v2) {
		QGL.DEBUG_printName("glRectiv");
		GL11.glRectiv(v1, v2);
	}

	public static void qglRects(short x1, short y1, short x2, short y2) {
		QGL.DEBUG_printName("glRects");
		GL11.glRects(x1, y1, x2, y2);
	}

	public static void qglRectsv(ShortBuffer v1, ShortBuffer v2) {
		QGL.DEBUG_printName("glRectsv");
		GL11.glRectsv(v1, v2);
	}

	public static int qglRenderMode(int mode) {
		QGL.DEBUG_printName("glRenderMode");
		return GL11.glRenderMode(mode);
	}

	public static void qglRotated(double angle, double x, double y, double z) {
		QGL.DEBUG_printName("glRotated");
		GL11.glRotated(angle, x, y, z);
	}

	public static void qglRotatef(float angle, float x, float y, float z) {
		QGL.DEBUG_printName("glRotatef");
		GL11.glRotatef(angle, x, y, z);
	}

	public static void qglScaled(double x, double y, double z) {
		QGL.DEBUG_printName("glScaled");
		GL11.glScaled(x, y, z);
	}

	public static void qglScalef(float x, float y, float z) {
		QGL.DEBUG_printName("glScalef");
		GL11.glScalef(x, y, z);
	}

	public static void qglSelectBuffer(IntBuffer buffer) {
		QGL.DEBUG_printName("glSelectBuffer");
		GL11.glSelectBuffer(buffer);
	}

	public static void qglTexCoord1d(double s) {
		QGL.DEBUG_printName("glTexCoord1d");
		GL11.glTexCoord1d(s);
	}

	public static void qglTexCoord1dv(DoubleBuffer v) {
		QGL.DEBUG_printName("glTexCoord1dv");
		GL11.glTexCoord1dv(v);
	}

	public static void qglTexCoord1f(float s) {
		QGL.DEBUG_printName("glTexCoord1f");
		GL11.glTexCoord1f(s);
	}

	public static void qglTexCoord1fv(FloatBuffer v) {
		QGL.DEBUG_printName("glTexCoord1fv");
		GL11.glTexCoord1fv(v);
	}

	public static void qglTexCoord1i(int s) {
		QGL.DEBUG_printName("glTexCoord1i");
		GL11.glTexCoord1i(s);
	}

	public static void qglTexCoord1iv(IntBuffer v) {
		QGL.DEBUG_printName("glTexCoord1iv");
		GL11.glTexCoord1iv(v);
	}

	public static void qglTexCoord1s(short s) {
		QGL.DEBUG_printName("glTexCoord1s");
		GL11.glTexCoord1s(s);
	}

	public static void qglTexCoord1sv(ShortBuffer v) {
		QGL.DEBUG_printName("glTexCoord1sv");
		GL11.glTexCoord1sv(v);
	}

	public static void qglTexCoord2d(double s, double t) {
		QGL.DEBUG_printName("glTexCoord2d");
		GL11.glTexCoord2d(s, t);
	}

	public static void qglTexCoord2dv(DoubleBuffer v) {
		QGL.DEBUG_printName("glTexCoord2dv");
		GL11.glTexCoord2dv(v);
	}

	public static void qglTexCoord2i(int s, int t) {
		QGL.DEBUG_printName("glTexCoord2i");
		GL11.glTexCoord2i(s, t);
	}

	public static void qglTexCoord2iv(IntBuffer v) {
		QGL.DEBUG_printName("glTexCoord2iv");
		GL11.glTexCoord2iv(v);
	}

	public static void qglTexCoord2s(short s, short t) {
		QGL.DEBUG_printName("glTexCoord2s");
		GL11.glTexCoord2s(s, t);
	}

	public static void qglTexCoord2sv(ShortBuffer v) {
		QGL.DEBUG_printName("glTexCoord2sv");
		GL11.glTexCoord2sv(v);
	}

	public static void qglTexCoord3d(double s, double t, double r) {
		QGL.DEBUG_printName("glTexCoord3d");
		GL11.glTexCoord3d(s, t, r);
	}

	public static void qglTexCoord3dv(DoubleBuffer v) {
		QGL.DEBUG_printName("glTexCoord3dv");
		GL11.glTexCoord3dv(v);
	}

	public static void qglTexCoord3f(float s, float t, float r) {
		QGL.DEBUG_printName("glTexCoord3f");
		GL11.glTexCoord3f(s, t, r);
	}

	public static void qglTexCoord3fv(FloatBuffer v) {
		QGL.DEBUG_printName("glTexCoord3fv");
		GL11.glTexCoord3fv(v);
	}

	public static void qglTexCoord3i(int s, int t, int r) {
		QGL.DEBUG_printName("glTexCoord3i");
		GL11.glTexCoord3i(s, t, r);
	}

	public static void qglTexCoord3iv(IntBuffer v) {
		QGL.DEBUG_printName("glTexCoord3iv");
		GL11.glTexCoord3iv(v);
	}

	public static void qglTexCoord3s(short s, short t, short r) {
		QGL.DEBUG_printName("glTexCoord3s");
		GL11.glTexCoord3s(s, t, r);
	}

	public static void qglTexCoord3sv(ShortBuffer v) {
		QGL.DEBUG_printName("glTexCoord3sv");
		GL11.glTexCoord3sv(v);
	}

	public static void qglTexCoord4d(double s, double t, double r, double q) {
		QGL.DEBUG_printName("glTexCoord4d");
		GL11.glTexCoord4d(s, t, r, q);
	}

	public static void qglTexCoord4dv(DoubleBuffer v) {
		QGL.DEBUG_printName("glTexCoord4dv");
		GL11.glTexCoord4dv(v);
	}

	public static void qglTexCoord4f(float s, float t, float r, float q) {
		QGL.DEBUG_printName("glTexCoord4f");
		GL11.glTexCoord4f(s, t, r, q);
	}

	public static void qglTexCoord4fv(FloatBuffer v) {
		QGL.DEBUG_printName("glTexCoord4fv");
		GL11.glTexCoord4fv(v);
	}

	public static void qglTexCoord4i(int s, int t, int r, int q) {
		QGL.DEBUG_printName("glTexCoord4i");
		GL11.glTexCoord4i(s, t, r, q);
	}

	public static void qglTexCoord4iv(IntBuffer v) {
		QGL.DEBUG_printName("glTexCoord4iv");
		GL11.glTexCoord4iv(v);
	}

	public static void qglTexCoord4s(short s, short t, short r, short q) {
		QGL.DEBUG_printName("glTexCoord4s");
		GL11.glTexCoord4s(s, t, r, q);
	}

	public static void qglTexCoord4sv(ShortBuffer v) {
		QGL.DEBUG_printName("glTexCoord4sv");
		GL11.glTexCoord4sv(v);
	}

	public static void qglTexEnvf(int target, int pname, float param) {
		QGL.DEBUG_printName("glTexEnvf");
		GL11.glTexEnvf(target, pname, param);
	}

	public static void qglTexEnviv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glTexEnviv");
		GL11.glTexEnviv(target, pname, params);
	}

	public static void qglTexGend(int coord, int pname, double param) {
		QGL.DEBUG_printName("glTexGend");
		GL11.glTexGend(coord, pname, param);
	}

	public static void qglTexGendv(int coord, int pname, DoubleBuffer params) {
		QGL.DEBUG_printName("glTexGendv");
		GL11.glTexGendv(coord, pname, params);
	}

	public static void qglTexGeni(int coord, int pname, int param) {
		QGL.DEBUG_printName("glTexGeni");
		GL11.glTexGeni(coord, pname, param);
	}

	public static void qglTexGeniv(int coord, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glTexGeniv");
		GL11.glTexGeniv(coord, pname, params);
	}

	public static void qglTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type,
			ByteBuffer pixels) {
		QGL.DEBUG_printName("glTexImage1D");
		GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels);
	}

	public static void qglTexParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glTexParameteriv");
		GL11.glTexParameteriv(target, pname, params);
	}

	public static void qglTexSubImage1D(int target, int level, int xoffset, int width, int format, int type,
			ByteBuffer pixels) {
		QGL.DEBUG_printName("glTexSubImage1D");
		GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels);
	}

	public static void qglTranslated(double x, double y, double z) {
		QGL.DEBUG_printName("glTranslated");
		GL11.glTranslated(x, y, z);
	}

	public static void qglTranslatef(float x, float y, float z) {
		QGL.DEBUG_printName("glTranslatef");
		GL11.glTranslatef(x, y, z);
	}

	public static void qglVertex2d(double x, double y) {
		QGL.DEBUG_printName("glVertex2d");
		GL11.glVertex2d(x, y);
	}

	public static void qglVertex2dv(DoubleBuffer coords) {
		QGL.DEBUG_printName("glVertex2dv");
		GL11.glVertex2dv(coords);
	}

	public static void qglVertex2fv(FloatBuffer coords) {
		QGL.DEBUG_printName("glVertex2fv");
		GL11.glVertex2fv(coords);
	}

	public static void qglVertex2i(int x, int y) {
		QGL.DEBUG_printName("glVertex2i");
		GL11.glVertex2i(x, y);
	}

	public static void qglVertex2iv(IntBuffer coords) {
		QGL.DEBUG_printName("glVertex2iv");
		GL11.glVertex2iv(coords);
	}

	public static void qglVertex2s(short x, short y) {
		QGL.DEBUG_printName("glVertex2s");
		GL11.glVertex2s(x, y);
	}

	public static void qglVertex2sv(ShortBuffer coords) {
		QGL.DEBUG_printName("glVertex2sv");
		GL11.glVertex2sv(coords);
	}

	public static void qglVertex3d(double x, double y, double z) {
		QGL.DEBUG_printName("glVertex3d");
		GL11.glVertex3d(x, y, z);
	}

	public static void qglVertex3dv(DoubleBuffer coords) {
		QGL.DEBUG_printName("glVertex3dv");
		GL11.glVertex3dv(coords);
	}

	public static void qglVertex3i(int x, int y, int z) {
		QGL.DEBUG_printName("glVertex3i");
		GL11.glVertex3i(x, y, z);
	}

	public static void qglVertex3iv(IntBuffer coords) {
		QGL.DEBUG_printName("glVertex3iv");
		GL11.glVertex3iv(coords);
	}

	public static void qglVertex3s(short x, short y, short z) {
		QGL.DEBUG_printName("glVertex3s");
		GL11.glVertex3s(x, y, z);
	}

	public static void qglVertex3sv(ShortBuffer coords) {
		QGL.DEBUG_printName("glVertex3sv");
		GL11.glVertex3sv(coords);
	}

	public static void qglVertex4d(double x, double y, double z, double w) {
		QGL.DEBUG_printName("glVertex4d");
		GL11.glVertex4d(x, y, z, w);
	}

	public static void qglVertex4dv(DoubleBuffer coords) {
		QGL.DEBUG_printName("glVertex4dv");
		GL11.glVertex4dv(coords);
	}

	public static void qglVertex4f(float x, float y, float z, float w) {
		QGL.DEBUG_printName("glVertex4f");
		GL11.glVertex4f(x, y, z, w);
	}

	public static void qglVertex4fv(FloatBuffer coords) {
		QGL.DEBUG_printName("glVertex4fv");
		GL11.glVertex4fv(coords);
	}

	public static void qglVertex4i(int x, int y, int z, int w) {
		QGL.DEBUG_printName("glVertex4i");
		GL11.glVertex4i(x, y, z, w);
	}

	public static void qglVertex4iv(IntBuffer coords) {
		QGL.DEBUG_printName("glVertex4iv");
		GL11.glVertex4iv(coords);
	}

	public static void qglVertex4s(short x, short y, short z, short w) {
		QGL.DEBUG_printName("glVertex4s");
		GL11.glVertex4s(x, y, z, w);
	}

	public static void qglVertex4sv(ShortBuffer coords) {
		QGL.DEBUG_printName("glVertex4sv");
		GL11.glVertex4sv(coords);
	}

	public static void qglVertexAttribPointerARB(int index, int size, int type, boolean normalized, int stride,
			FloatBuffer pointer) {
		QGL.DEBUG_printName("glVertexAttribPointerARB");
		ARBVertexShader.glVertexAttribPointerARB(index, size, type, normalized, stride, pointer);
	}

}
