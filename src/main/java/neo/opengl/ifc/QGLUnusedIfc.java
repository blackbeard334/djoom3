package neo.opengl.ifc;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public interface QGLUnusedIfc {// extends QGLIfc {

	void qglAccum(int op, float value);

	boolean qglAreTexturesResident(int n, IntBuffer textures, ByteBuffer residences);

	void qglBitmap(int width, int height, float xorig, float yorig, float xmove, float ymove, ByteBuffer bitmap);

	void qglCallList(int list);

	void qglCallLists(int type, ByteBuffer lists);

	void qglClearAccum(float red, float green, float blue, float alpha);

	void qglClearIndex(float c);

	void qglClipPlane(int plane, DoubleBuffer equation);

	void qglColor3b(byte red, byte green, byte blue);

	/**
	 * 
	 * @param v
	 */
	void qglColor3bv(byte[] v);

	void qglColor3bv(ByteBuffer v);

	void qglColor3d(double red, double green, double blue);

	/**
	 * 
	 * @param v
	 */
	void qglColor3dv(double[] v);

	void qglColor3dv(DoubleBuffer v);

	void qglColor3i(int red, int green, int blue);

	/**
	 * 
	 * @param v
	 */
	void qglColor3iv(int[] v);

	void qglColor3iv(IntBuffer v);

	void qglColor3s(short red, short green, short blue);

	/**
	 * 
	 * @param v
	 */
	void qglColor3sv(short[] v);

	void qglColor3sv(ShortBuffer v);

	void qglColor3ub(byte red, byte green, byte blue);

	void qglColor3ui(int red, int green, int blue);

	/**
	 * 
	 * @param v
	 */
	void qglColor3uiv(int[] v);

	void qglColor3uiv(IntBuffer v);

	void qglColor3us(short red, short green, short blue);

	/**
	 * 
	 * @param v
	 */
	void qglColor3usv(short[] v);

	void qglColor3usv(ShortBuffer v);

	void qglColor4b(byte red, byte green, byte blue, byte alpha);

	/**
	 * 
	 * @param v
	 */
	void qglColor4bv(byte[] v);

	void qglColor4bv(ByteBuffer v);

	void qglColor4d(double red, double green, double blue, double alpha);

	/**
	 * 
	 * @param v
	 */
	void qglColor4dv(double[] v);

	void qglColor4dv(DoubleBuffer v);

	void qglColor4i(int red, int green, int blue, int alpha);

	/**
	 * 
	 * @param v
	 */
	void qglColor4iv(int[] v);

	void qglColor4iv(IntBuffer v);

	void qglColor4s(short red, short green, short blue, short alpha);

	/**
	 * 
	 * @param v
	 */
	void qglColor4sv(short[] v);

	void qglColor4sv(ShortBuffer v);

	void qglColor4ub(byte red, byte green, byte blue, byte alpha);

	void qglColor4ui(int red, int green, int blue, int alpha);

	/**
	 * 
	 * @param v
	 */
	void qglColor4uiv(int[] v);

	void qglColor4uiv(IntBuffer v);

	void qglColor4us(short red, short green, short blue, short alpha);

	/**
	 * 
	 * @param v
	 */
	void qglColor4usv(short[] v);

	void qglColor4usv(ShortBuffer v);

	void qglColorMaterial(int face, int mode);

	void qglCombinerInputNV(int stage, int portion, int variable, int input, int mapping, int componentUsage);

	void qglCombinerOutputNV(int stage, int portion, int abOutput, int cdOutput, int sumOutput, int scale, int bias,
			boolean abDotProduct, boolean cdDotProduct, boolean muxSum);

	/**
	 * 
	 * @param pname
	 * @param params
	 */
	void qglCombinerParameterfvNV(int pname, float[] params);

	void qglCombinerParameteriNV(int pname, int param);

	void qglCompressedTexImage2DARB(int target, int level, int internalformat, int width, int height, int border,
			int imageSize, long pData_buffer_offset);

	void qglCopyPixels(int x, int y, int width, int height, int type);

	void qglCopyTexImage1D(int target, int level, int internalFormat, int x, int y, int width, int border);

	void qglCopyTexSubImage1D(int target, int level, int xoffset, int x, int y, int width);

	void qglDeleteLists(int list, int range);

	/**
	 * 
	 * @param n
	 * @param textures
	 */
	void qglDeleteTextures(int n, int[] textures);

	void qglDeleteTextures(int n, IntBuffer textures);

	void qglDrawArrays(int mode, int first, int count);

	void qglEdgeFlag(boolean flag);

	void qglEdgeFlagv(ByteBuffer flag);

	void qglEndList();

	void qglEvalCoord1d(double u);

	/**
	 * 
	 * @param u
	 */
	void qglEvalCoord1dv(double[] u);

	void qglEvalCoord1dv(DoubleBuffer u);

	void qglEvalCoord1f(float u);

	/**
	 * 
	 * @param u
	 */
	void qglEvalCoord1fv(float[] u);

	void qglEvalCoord1fv(FloatBuffer u);

	void qglEvalCoord2d(double u, double v);

	/**
	 * 
	 * @param u
	 */
	void qglEvalCoord2dv(double[] u);

	void qglEvalCoord2dv(DoubleBuffer u);

	void qglEvalCoord2f(float u, float v);

	/**
	 * 
	 * @param u
	 */
	void qglEvalCoord2fv(float[] u);

	void qglEvalCoord2fv(FloatBuffer u);

	void qglEvalMesh1(int mode, int i1, int i2);

	void qglEvalMesh2(int mode, int i1, int i2, int j1, int j2);

	void qglEvalPoint1(int i);

	void qglEvalPoint2(int i, int j);

	void qglFeedbackBuffer(int size, int type, FloatBuffer buffer);

	void qglFinalCombinerInputNV(int variable, int input, int mapping, int componentUsage);

	void qglFogf(int pname, float param);

	/**
	 * 
	 * @param pname
	 * @param params
	 */
	void qglFogfv(int pname, float[] params);

	void qglFogfv(int pname, FloatBuffer params);

	void qglFogi(int pname, int param);

	/**
	 * 
	 * @param pname
	 * @param params
	 */
	void qglFogiv(int pname, int[] params);

	void qglFogiv(int pname, IntBuffer params);

	void qglFrontFace(int mode);

	void qglFrustum(double left, double right, double bottom, double top, double zNear, double zFar);

	/**
	 * 
	 * @param n
	 * @param buffers
	 */
	void qglGenBuffersARB(int n, IntBuffer[] buffers);

	int qglGenLists(Enum<?> range);

	int qglGenLists(int range);

	/**
	 * 
	 * @param n
	 * @param textures
	 */
	void qglGenTextures(int n, int[] textures);

	void qglGenTextures(int n, IntBuffer textures);

	void qglGetBooleanv(int pname, boolean[] params);

	void qglGetBooleanv(int pname, ByteBuffer params);

	void qglGetClipPlane(int plane, DoubleBuffer equation);

	void qglGetDoublev(int pname, double[] params);

	void glGetDoublev(int pname, DoubleBuffer params);

	/**
	 * 
	 * @param light
	 * @param pname
	 * @param params
	 */
	void qglGetLightfv(int light, int pname, float[] params);

	void qglGetLightfv(int light, int pname, FloatBuffer params);

	/**
	 * 
	 * @param light
	 * @param pname
	 * @param params
	 */
	void qglGetLightiv(int light, int pname, int[] params);

	void qglGetLightiv(int light, int pname, IntBuffer params);

	/**
	 * 
	 * @param target
	 * @param query
	 * @param v
	 */
	void qglGetMapdv(int target, int query, double[] v);

	void qglGetMapdv(int target, int query, DoubleBuffer v);

	/**
	 * 
	 * @param target
	 * @param query
	 * @param v
	 */
	void qglGetMapfv(int target, int query, float[] v);

	void qglGetMapfv(int target, int query, FloatBuffer v);

	/**
	 * 
	 * @param target
	 * @param query
	 * @param v
	 */
	void qglGetMapiv(int target, int query, int[] v);

	void qglGetMapiv(int target, int query, IntBuffer v);

	/**
	 * 
	 * @param face
	 * @param pname
	 * @param params
	 */
	void qglGetMaterialfv(int face, int pname, float[] params);

	void qglGetMaterialfv(int face, int pname, FloatBuffer params);

	/**
	 * 
	 * @param face
	 * @param pname
	 * @param params
	 */
	void qglGetMaterialiv(int face, int pname, int[] params);

	void qglGetMaterialiv(int face, int pname, IntBuffer params);

	/**
	 * 
	 * @param map
	 * @param values
	 */
	void qglGetPixelMapfv(int map, float[] values);

	void qglGetPixelMapfv(int map, FloatBuffer values);

	/**
	 * 
	 * @param map
	 * @param values
	 */
	void qglGetPixelMapuiv(int map, int[] values);

	void qglGetPixelMapuiv(int map, IntBuffer values);

	/**
	 * 
	 * @param map
	 * @param values
	 */
	void qglGetPixelMapusv(int map, short[] values);

	void qglGetPixelMapusv(int map, ShortBuffer values);

	long qglGetPointer(int pname);

	void qglGetPolygonStipple(byte mask);

	/**
	 * 
	 * @param target
	 * @param pname
	 * @param params
	 */
	void qglGetTexEnvfv(int target, int pname, float[] params);

	void qglGetTexEnvfv(int target, int pname, FloatBuffer params);

	/**
	 * 
	 * @param target
	 * @param pname
	 * @param params
	 */
	void qglGetTexEnviv(int target, int pname, int[] params);

	void qglGetTexEnviv(int target, int pname, IntBuffer params);

	/**
	 * 
	 * @param coord
	 * @param pname
	 * @param params
	 */
	void qglGetTexGendv(int coord, int pname, double[] params);

	void qglGetTexGendv(int coord, int pname, DoubleBuffer params);

	/**
	 * 
	 * @param coord
	 * @param pname
	 * @param params
	 */
	void qglGetTexGenfv(int coord, int pname, float[] params);

	void qglGetTexGenfv(int coord, int pname, FloatBuffer params);

	/**
	 * 
	 * @param coord
	 * @param pname
	 * @param params
	 */
	void qglGetTexGeniv(int coord, int pname, int[] params);

	void qglGetTexGeniv(int coord, int pname, IntBuffer params);

	/**
	 * 
	 * @param target
	 * @param level
	 * @param pname
	 * @param params
	 */
	void qglGetTexLevelParameterfv(int target, int level, int pname, float[] params);

	void qglGetTexLevelParameterfv(int target, int level, int pname, FloatBuffer params);

	/**
	 * 
	 * @param target
	 * @param level
	 * @param pname
	 * @param params
	 */
	void qglGetTexLevelParameteriv(int target, int level, int pname, int[] params);

	void qglGetTexLevelParameteriv(int target, int level, int pname, IntBuffer params);

	/**
	 * 
	 * @param target
	 * @param pname
	 * @param params
	 */
	void qglGetTexParameterfv(int target, int pname, float[] params);

	void qglGetTexParameterfv(int target, int pname, FloatBuffer params);

	/**
	 * 
	 * @param target
	 * @param pname
	 * @param params
	 */
	void qglGetTexParameteriv(int target, int pname, int[] params);

	void qglGetTexParameteriv(int target, int pname, IntBuffer params);

	void qglHint(int target, int mode);

	void qglIndexd(double c);

	/**
	 * 
	 * @param c
	 */
	void qglIndexdv(double[] c);

	void qglIndexdv(DoubleBuffer c);

	void qglIndexf(float c);

	/**
	 * 
	 * @param c
	 */
	void qglIndexfv(float[] c);

	void qglIndexfv(FloatBuffer c);

	void qglIndexi(int c);

	/**
	 * 
	 * @param c
	 */
	void qglIndexiv(int[] c);

	void qglIndexiv(IntBuffer c);

	void qglIndexMask(int mask);

	void qglIndexPointer(int type, int stride, ByteBuffer pointer);

	void qglIndexs(short c);

	/**
	 * 
	 * @param c
	 */
	void qglIndexsv(short[] c);

	void qglIndexsv(ShortBuffer c);

	void qglIndexub(byte c);

	void qglIndexubv(ByteBuffer c);

	void qglInitNames();

	void qglInterleavedArrays(int format, int stride, ByteBuffer pointer);

	boolean qglIsEnabled(int cap);

	boolean qglIsList(int list);

	boolean qglIsTexture(int texture);

	void qglLightf(int light, int pname, float param);

	void qglLightfv(int light, int pname, FloatBuffer params);

	void qglLighti(int light, int pname, int param);

	void qglLightiv(int light, int pname, IntBuffer params);

	void qglLightModelf(int pname, float param);

	void qglLightModelfv(int pname, FloatBuffer params);

	void qglLightModeli(int pname, int param);

	void qglLightModeliv(int pname, IntBuffer params);

	void qglLineStipple(int factor, short pattern);

	void qglListBase(int base);

	void qglLoadMatrixd(DoubleBuffer m);

	void qglLoadName(int name);

	void qglLogicOp(int opcode);

	void qglMap1d(int target, double u1, double u2, int stride, int order, DoubleBuffer points);

	void qglMap1f(int target, float u1, float u2, int stride, int order, FloatBuffer points);

	void qglMap2d(int target, double u1, double u2, int ustride, int uorder, double v1, double v2, int vstride,
			int vorder, DoubleBuffer points);

	void qglMap2f(int target, float u1, float u2, int ustride, int uorder, float v1, float v2, int vstride, int vorder,
			FloatBuffer points);

	void qglMapGrid1d(int un, double u1, double u2);

	void qglMapGrid1f(int un, float u1, float u2);

	void qglMapGrid2d(int un, double u1, double u2, int vn, double v1, double v2);

	void qglMapGrid2f(int un, float u1, float u2, int vn, float v1, float v2);

	void qglMaterialf(int face, int pname, float param);

	/**
	 * 
	 * @param face
	 * @param pname
	 * @param params
	 */
	void qglMaterialfv(int face, int pname, float[] params);

	void qglMaterialfv(int face, int pname, FloatBuffer params);

	void qglMateriali(int face, int pname, int param);

	/**
	 * 
	 * @param face
	 * @param pname
	 * @param params
	 */
	void qglMaterialiv(int face, int pname, int[] params);

	void qglMaterialiv(int face, int pname, IntBuffer params);

	/**
	 * 
	 * @param m
	 */
	void qglMultMatrixd(double[] m);

	void qglMultMatrixd(DoubleBuffer m);

	/**
	 * 
	 * @param m
	 */
	void qglMultMatrixf(float[] m);

	void qglMultMatrixf(FloatBuffer m);

	void qglNewList(int list, int mode);

	void qglNormal3b(byte nx, byte ny, byte nz);

	void qglNormal3bv(ByteBuffer v);

	void qglNormal3d(double nx, double ny, double nz);

	/**
	 * 
	 * @param v
	 */
	void qglNormal3dv(double[] v);

	void qglNormal3dv(DoubleBuffer v);

	void qglNormal3f(float nx, float ny, float nz);

	/**
	 * 
	 * @param v
	 */
	void qglNormal3fv(float[] v);

	void qglNormal3fv(FloatBuffer v);

	void qglNormal3i(int nx, int ny, int nz);

	/**
	 * 
	 * @param v
	 */
	void qglNormal3iv(int[] v);

	void qglNormal3iv(IntBuffer v);

	void qglNormal3s(short nx, short ny, short nz);

	/**
	 * 
	 * @param v
	 */
	void qglNormal3sv(short[] v);

	void qglNormal3sv(ShortBuffer v);

	void qglPassThrough(float token);

	/**
	 * 
	 * @param map
	 * @param mapsize
	 * @param values
	 */
	void qglPixelMapfv(int map, int mapsize, float[] values);

	void qglPixelMapfv(int mapsize, FloatBuffer values);

	/**
	 * 
	 * @param map
	 * @param mapsize
	 * @param values
	 */
	void qglPixelMapuiv(int map, int mapsize, int[] values);

	void qglPixelMapuiv(int mapsize, IntBuffer values);

	/**
	 * 
	 * @param map
	 * @param values
	 */
	void qglPixelMapusv(int map, short[] values);

	void qglPixelMapusv(int mapsize, ShortBuffer values);

	void qglPixelStoref(int pname, float param);

	void qglPixelTransferf(int pname, float param);

	void qglPixelTransferi(int pname, int param);

	void qglPixelZoom(float xfactor, float yfactor);

	void qglPolygonStipple(ByteBuffer mask);

	void qglPopClientAttrib();

	void qglPopName();

	void qglPrioritizeTextures(int n, IntBuffer textures, FloatBuffer priorities);

	void qglPushClientAttrib(int mask);

	void qglPushName(int name);

	void qglRasterPos2d(double x, double y);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos2dv(double[] v);

	void qglRasterPos2dv(DoubleBuffer v);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos2fv(float[] v);

	void qglRasterPos2fv(FloatBuffer v);

	void qglRasterPos2i(int x, int y);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos2iv(int[] v);

	void qglRasterPos2iv(IntBuffer v);

	void qglRasterPos2s(short x, short y);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos2sv(short[] v);

	void qglRasterPos2sv(ShortBuffer v);

	void qglRasterPos3d(double x, double y, double z);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos3dv(double[] v);

	void qglRasterPos3dv(DoubleBuffer v);

	void qglRasterPos3f(float x, float y, float z);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos3fv(float[] v);

	void qglRasterPos3fv(FloatBuffer v);

	void qglRasterPos3i(int x, int y, int z);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos3iv(int[] v);

	void qglRasterPos3iv(IntBuffer v);

	void qglRasterPos3s(short x, short y, short z);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos3sv(short[] v);

	void qglRasterPos3sv(ShortBuffer v);

	void qglRasterPos4d(double x, double y, double z, double w);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos4dv(double[] v);

	void qglRasterPos4dv(DoubleBuffer v);

	void qglRasterPos4f(float x, float y, float z, float w);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos4fv(float[] v);

	void qglRasterPos4fv(FloatBuffer v);

	void qglRasterPos4i(int x, int y, int z, int w);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos4iv(int[] v);

	void qglRasterPos4iv(IntBuffer v);

	void qglRasterPos4s(short x, short y, short z, short w);

	/**
	 * 
	 * @param v
	 */
	void qglRasterPos4sv(short[] v);

	void qglRasterPos4sv(ShortBuffer v);

	void qglRectd(double x1, double y1, double x2, double y2);

	/**
	 * 
	 * @param v1
	 * @param v2
	 */
	void qglRectdv(double[] v1, double[] v2);

	void qglRectdv(DoubleBuffer v1, DoubleBuffer v2);

	void qglRectf(float x1, float y1, float x2, float y2);

	/**
	 * 
	 * @param v1
	 * @param v2
	 */
	void qglRectfv(float[] v1, float[] v2);

	void qglRectfv(FloatBuffer v1, FloatBuffer v2);

	void qglRecti(int x1, int y1, int x2, int y2);

	/**
	 * 
	 * @param v1
	 * @param v2
	 */
	void qglRectiv(int[] v1, int[] v2);

	void qglRectiv(IntBuffer v1, IntBuffer v2);

	void qglRects(short x1, short y1, short x2, short y2);

	/**
	 * 
	 * @param v1
	 * @param v2
	 */
	void qglRectsv(short[] v1, short[] v2);

	void qglRectsv(ShortBuffer v1, ShortBuffer v2);

	int qglRenderMode(int mode);

	void qglRotated(double angle, double x, double y, double z);

	void qglRotatef(float angle, float x, float y, float z);

	void qglScaled(double x, double y, double z);

	void qglScalef(float x, float y, float z);

	void qglSelectBuffer(int size, IntBuffer buffer);

	void qglTexCoord1d(double s);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord1dv(double[] v);

	void qglTexCoord1dv(DoubleBuffer v);

	void qglTexCoord1f(float s);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord1fv(float[] v);

	void qglTexCoord1fv(FloatBuffer v);

	void qglTexCoord1i(int s);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord1iv(int[] v);

	void qglTexCoord1iv(IntBuffer v);

	void qglTexCoord1s(short s);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord1sv(short[] v);

	void qglTexCoord1sv(ShortBuffer v);

	void qglTexCoord2d(double s, double t);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord2dv(double[] v);

	void qglTexCoord2dv(DoubleBuffer v);

	void qglTexCoord2i(int s, int t);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord2iv(int[] v);

	void qglTexCoord2iv(IntBuffer v);

	void qglTexCoord2s(short s, short t);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord2sv(short[] v);

	void qglTexCoord2sv(ShortBuffer v);

	void qglTexCoord3d(double s, double t, double r);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord3dv(double[] v);

	void qglTexCoord3dv(DoubleBuffer v);

	void qglTexCoord3f(float s, float t, float r);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord3fv(float[] v);

	void qglTexCoord3fv(FloatBuffer v);

	void qglTexCoord3i(int s, int t, int r);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord3iv(int[] v);

	void qglTexCoord3iv(IntBuffer v);

	void qglTexCoord3s(short s, short t, short r);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord3sv(short[] v);

	void qglTexCoord3sv(ShortBuffer v);

	void qglTexCoord4d(double s, double t, double r, double q);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord4dv(double[] v);

	void qglTexCoord4dv(DoubleBuffer v);

	void qglTexCoord4f(float s, float t, float r, float q);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord4fv(float[] v);

	void qglTexCoord4fv(FloatBuffer v);

	void qglTexCoord4i(int s, int t, int r, int q);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord4iv(int[] v);

	void qglTexCoord4iv(IntBuffer v);

	void qglTexCoord4s(short s, short t, short r, short q);

	/**
	 * 
	 * @param v
	 */
	void qglTexCoord4sv(short[] v);

	void qglTexCoord4sv(ShortBuffer v);

	void qglTexEnvf(int target, int pname, float param);

	/**
	 * 
	 * @param target
	 * @param pname
	 * @param params
	 */
	void qglTexEnviv(int target, int pname, int[] params);

	void qglTexEnviv(int target, int pname, IntBuffer params);

	void qglTexGend(int coord, int pname, double param);

	void qglTexGendv(int coord, int pname, double[] params);

	void qglTexGendv(int coord, int pname, DoubleBuffer params);

	void qglTexGeni(int coord, int pname, int param);

	/**
	 * 
	 * @param coord
	 * @param pname
	 * @param params
	 */
	void qglTexGeniv(int coord, int pname, int[] params);

	void qglTexGeniv(int coord, int pname, IntBuffer params);

	void qglTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type,
			ByteBuffer pixels);

	/**
	 * 
	 * @param target
	 * @param pname
	 * @param params
	 */
	void qglTexParameteriv(int target, int pname, int[] params);

	void qglTexParameteriv(int target, int pname, IntBuffer params);

	void qglTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, ByteBuffer pixels);

	void qglTranslated(double x, double y, double z);

	void qglTranslatef(float x, float y, float z);

	void qglVertex2d(double x, double y);

	/**
	 * 
	 * @param v
	 */
	void qglVertex2dv(double[] v);

	void qglVertex2dv(DoubleBuffer v);

	/**
	 * 
	 * @param v
	 */
	void qglVertex2fv(float[] v);

	void qglVertex2fv(FloatBuffer v);

	void qglVertex2i(int x, int y);

	/**
	 * 
	 * @param v
	 */
	void qglVertex2iv(int[] v);

	void qglVertex2iv(IntBuffer v);

	void qglVertex2s(short x, short y);

	/**
	 * 
	 * @param v
	 */
	void qglVertex2sv(short[] v);

	void qglVertex2sv(ShortBuffer v);

	void qglVertex3d(double x, double y, double z);

	/**
	 * 
	 * @param v
	 */
	void qglVertex3dv(double[] v);

	void qglVertex3dv(DoubleBuffer v);

	void qglVertex3i(int x, int y, int z);

	/**
	 * 
	 * @param v
	 */
	void qglVertex3iv(int[] v);

	void qglVertex3iv(IntBuffer v);

	void qglVertex3s(short x, short y, short z);

	/**
	 * 
	 * @param v
	 */
	void qglVertex3sv(short[] v);

	void qglVertex3sv(ShortBuffer v);

	void qglVertex4d(double x, double y, double z, double w);

	/**
	 * 
	 * @param v
	 */
	void qglVertex4dv(double[] v);

	void qglVertex4dv(DoubleBuffer v);

	void qglVertex4f(float x, float y, float z, float w);

	/**
	 * 
	 * @param v
	 */
	void qglVertex4fv(float[] v);

	void qglVertex4fv(FloatBuffer v);

	void qglVertex4i(int x, int y, int z, int w);

	/**
	 * 
	 * @param v
	 */
	void qglVertex4iv(int[] v);

	void qglVertex4iv(IntBuffer v);

	void qglVertex4s(short x, short y, short z, short w);

	/**
	 * 
	 * @param v
	 */
	void qglVertex4sv(short[] v);

	void qglVertex4sv(ShortBuffer v);

	/**
	 * 
	 * @param index
	 * @param size
	 * @param type
	 * @param normalized
	 * @param stride
	 * @param pointer
	 */
	void qglVertexAttribPointerARB(int index, int size, int type,
			boolean normalized, int stride, float[] pointer);

	void qglVertexAttribPointerARB(int index, int size, int type,
			boolean normalized, int stride, FloatBuffer pointer);

}
