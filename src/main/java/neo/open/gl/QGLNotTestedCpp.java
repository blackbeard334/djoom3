package neo.open.gl;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBImaging;
import org.lwjgl.opengl.ARBMatrixPalette;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBPointParameters;
import org.lwjgl.opengl.ARBTransposeMatrix;
import org.lwjgl.opengl.ARBVertexBlend;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.ARBWindowPos;
import org.lwjgl.opengl.EXTBlendEquationSeparate;
import org.lwjgl.opengl.EXTSecondaryColor;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.NVHalfFloat;
import org.lwjgl.opengl.NVPixelDataRange;
import org.lwjgl.opengl.NVPrimitiveRestart;

/**
 * all the untested qgl stuff from cpp source Contains method not implemented in
 * QGL and QGLNotTested.
 * 
 * If successfully tested, move method to QGL!
 * 
 * The class is not visible, use QGL instead.
 */
class QGLNotTestedCpp extends QGLXNotTestedCpp {

	public static void qglActiveTexture(int texture) {
		QGL.DEBUG_printName("glActiveTexture");
		GL13.glActiveTexture(texture);
	}

	public static void qglBeginQuery(int target, int id) {
		QGL.DEBUG_printName("glBeginQuery");
		GL15.glBeginQuery(target, id);
	}

	public static void qglBindBuffer(int target, int buffer) {
		QGL.DEBUG_printName("glBindBuffer");
		GL15.glBindBuffer(target, buffer);
	}

	public static void qglBlendColor(int red, int green, int blue, int alpha) {
		QGL.DEBUG_printName("glBlendColor");
		GL14.glBlendColor(red, green, blue, alpha);
	}

	public static void qglBlendEquation(int mode) {
		QGL.DEBUG_printName("glBlendEquation");
		GL14.glBlendEquation(mode);
	}

	public static void qglBlendEquationSeparateEXT(int modeRGB, int modeAlpha) {
		QGL.DEBUG_printName("glBlendEquationSeparateEXT");
		EXTBlendEquationSeparate.glBlendEquationSeparateEXT(modeRGB, modeAlpha);
	}

	public static void qglBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
		QGL.DEBUG_printName("glBlendFuncSeparate");
		GL14.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
	}

	public static void qglBufferData(int target, int data, int usage) {
		QGL.DEBUG_printName("glBufferData");
		GL15.glBufferData(target, data, usage);
	}

	public static void qglBufferSubData(int target, long offset, ByteBuffer data) {
		QGL.DEBUG_printName("glBufferSubData");
		GL15.glBufferSubData(target, offset, data);
	}

	public static void qglClientActiveTexture(int texture) {
		QGL.DEBUG_printName("glClientActiveTexture");
		GL13.glClientActiveTexture(texture);
	}

	public static void qglColor3hNV(short red, short green, short blue) {
		QGL.DEBUG_printName("glColor3hNV");
		NVHalfFloat.glColor3hNV(red, green, blue);
	}

	public static void qglColor3hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glColor3hvNV");
		NVHalfFloat.glColor3hvNV(v);
	}

	public static void qglColor4hNV(short red, short green, short blue, short alpha) {
		QGL.DEBUG_printName("glColor4hNV");
		NVHalfFloat.glColor4hNV(red, green, blue, alpha);
	}

	public static void qglColor4hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glColor4hvNV");
		NVHalfFloat.glColor4hvNV(v);
	}

	public static void qglColorSubTable(int target, int start, int count, int format, int type, int data) {
		QGL.DEBUG_printName("glColorSubTable");
		ARBImaging.glColorSubTable(target, start, count, format, type, data);
	}

	public static void qglColorTableParameterfv(int target, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glColorTableParameterfv");
		ARBImaging.glColorTableParameterfv(target, pname, params);
	}

	public static void qglColorTableParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glColorTableParameteriv");
		ARBImaging.glColorTableParameteriv(target, pname, params);
	}

	public static void qglCompressedTexImage1D(int target, int level, int internalformat, int width, int border,
			ByteBuffer data) {
		QGL.DEBUG_printName("glCompressedTexImage1D");
		GL13.glCompressedTexImage1D(target, level, internalformat, width, border, data);
	}

	public static void qglCompressedTexImage2D(int target, int level, int internalformat, int width, int height,
			int border, ByteBuffer data) {
		QGL.DEBUG_printName("glCompressedTexImage2D");
		GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, data);
	}

	public static void qglCompressedTexImage3D(int target, int level, int internalformat, int width, int height,
			int depth, int border, ByteBuffer data) {
		QGL.DEBUG_printName("glCompressedTexImage3D");
		GL13.glCompressedTexImage3D(target, level, internalformat, width, height, depth, border, data);
	}

	public static void qglCompressedTexSubImage1D(int target, int level, int xoffset, int width, int format,
			ByteBuffer data) {
		QGL.DEBUG_printName("glCompressedTexSubImage1D");
		GL13.glCompressedTexSubImage1D(target, level, xoffset, width, format, data);
	}

	public static void qglCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
			int height, int format, ByteBuffer data) {
		QGL.DEBUG_printName("glCompressedTexSubImage2D");
		GL13.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data);
	}

	public static void qglCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
			int width, int height, int depth, int format, ByteBuffer data) {
		QGL.DEBUG_printName("glCompressedTexSubImage3D");
		GL13.glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, data);
	}

	public static void qglConvolutionFilter1D(int target, int internalformat, int width, int format, int type,
			int data) {
		QGL.DEBUG_printName("glConvolutionFilter1D");
		ARBImaging.glConvolutionFilter1D(target, internalformat, width, format, type, data);
	}

	public static void qglConvolutionFilter2D(int target, int internalformat, int width, int height, int format,
			int type, int data) {
		QGL.DEBUG_printName("glConvolutionFilter2D");
		ARBImaging.glConvolutionFilter2D(target, internalformat, width, height, format, type, data);
	}

	public static void qglConvolutionParameterf(int target, int pname, int param) {
		QGL.DEBUG_printName("glConvolutionParameterf");
		ARBImaging.glConvolutionParameterf(target, pname, param);
	}

	public static void qglConvolutionParameterfv(int target, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glConvolutionParameterfv");
		ARBImaging.glConvolutionParameterfv(target, pname, params);
	}

	public static void qglConvolutionParameteri(int target, int pname, int param) {
		QGL.DEBUG_printName("glConvolutionParameteri");
		ARBImaging.glConvolutionParameteri(target, pname, param);
	}

	public static void qglConvolutionParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glConvolutionParameteriv");
		ARBImaging.glConvolutionParameteriv(target, pname, params);
	}

	public static void qglCopyColorSubTable(int target, int start, int x, int y, int width) {
		QGL.DEBUG_printName("glCopyColorSubTable");
		ARBImaging.glCopyColorSubTable(target, start, x, y, width);
	}

	public static void qglCopyColorTable(int target, int internalformat, int x, int y, int width) {
		QGL.DEBUG_printName("glCopyColorTable");
		ARBImaging.glCopyColorTable(target, internalformat, x, y, width);
	}

	public static void qglCopyConvolutionFilter1D(int target, int internalformat, int x, int y, int width) {
		QGL.DEBUG_printName("glCopyConvolutionFilter1D");
		ARBImaging.glCopyConvolutionFilter1D(target, internalformat, x, y, width);
	}

	public static void qglCopyConvolutionFilter2D(int target, int internalformat, int x, int y, int width, int height) {
		QGL.DEBUG_printName("glCopyConvolutionFilter2D");
		ARBImaging.glCopyConvolutionFilter2D(target, internalformat, x, y, width, height);
	}

	public static void qglCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y,
			int width, int height) {
		QGL.DEBUG_printName("glCopyTexSubImage3D");
		GL12.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
	}

	public static void qglCurrentPaletteMatrixARB(int index) {
		QGL.DEBUG_printName("glCurrentPaletteMatrixARB");
		ARBMatrixPalette.glCurrentPaletteMatrixARB(index);
	}

	public static void qglDeleteBuffers(int buffer) {
		QGL.DEBUG_printName("glDeleteBuffers");
		GL15.glDeleteBuffers(buffer);
	}

	public static void qglDeleteProgramsARB(IntBuffer programs) {
		QGL.DEBUG_printName("glDeleteProgramsARB");
		ARBVertexProgram.glDeleteProgramsARB(programs);
	}

	public static void qglDeleteQueries(int id) {
		QGL.DEBUG_printName("glDeleteQueries");
		GL15.glDeleteQueries(id);
	}

	public static void qglDrawRangeElements(int mode, int start, int end, ByteBuffer indices) {
		QGL.DEBUG_printName("glDrawRangeElements");
		GL12.glDrawRangeElements(mode, start, end, indices);
	}

	public static void qglEdgeFlagPointer(int stride, ByteBuffer pointer) {
		QGL.DEBUG_printName("glEdgeFlagPointer");
		GL46.glEdgeFlagPointer(stride, pointer);
	}

	public static void qglEndQuery(int target) {
		QGL.DEBUG_printName("glEndQuery");
		GL15.glEndQuery(target);
	}

	public static void qglFlushPixelDataRangeNV(int target) {
		QGL.DEBUG_printName("glFlushPixelDataRangeNV");
		NVPixelDataRange.glFlushPixelDataRangeNV(target);
	}

	public static void qglFogCoordd(int coord) {
		QGL.DEBUG_printName("glFogCoordd");
		GL14.glFogCoordd(coord);
	}

	public static void qglFogCoorddv(DoubleBuffer coord) {
		QGL.DEBUG_printName("glFogCoorddv");
		GL14.glFogCoorddv(coord);
	}

	public static void qglFogCoordf(int coord) {
		QGL.DEBUG_printName("glFogCoordf");
		GL14.glFogCoordf(coord);
	}

	public static void qglFogCoordfv(FloatBuffer coord) {
		QGL.DEBUG_printName("glFogCoordfv");
		GL14.glFogCoordfv(coord);
	}

	public static void qglFogCoordhNV(short fog) {
		QGL.DEBUG_printName("glFogCoordhNV");
		NVHalfFloat.glFogCoordhNV(fog);
	}

	public static void qglFogCoordhvNV(ShortBuffer fog) {
		QGL.DEBUG_printName("glFogCoordhvNV");
		NVHalfFloat.glFogCoordhvNV(fog);
	}

	public static void qglFogCoordPointer(int type, int stride, int pointer) {
		QGL.DEBUG_printName("glFogCoordPointer");
		GL14.glFogCoordPointer(type, stride, pointer);
	}

	public static void qglGenBuffers() {
		QGL.DEBUG_printName("glGenBuffers");
		GL15.glGenBuffers();
	}

	public static void qglGenProgramsARB() {
		QGL.DEBUG_printName("glGenProgramsARB");
		ARBVertexProgram.glGenProgramsARB();
	}

	public static void qglGenQueries() {
		QGL.DEBUG_printName("glGenQueries");
		GL15.glGenQueries();
	}

	public static void qglGetBufferParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetBufferParameteriv");
		GL15.glGetBufferParameteriv(target, pname, params);
	}

	/**
	 * 
	 * @param target
	 * @param pname
	 * @param params
	 *
	 * PointerBuffer LWJGL Implementation specific.
	 * 
	 * TODO: make method signature LWJGL independent
	 */
	public static void qglGetBufferPointerv(int target, int pname, PointerBuffer params) {
		QGL.DEBUG_printName("glGetBufferPointerv");
		GL15.glGetBufferPointerv(target, pname, params);
	}

	public static void qglGetBufferSubData(int target, int offset, ByteBuffer data) {
		QGL.DEBUG_printName("glGetBufferSubData");
		GL15.glGetBufferSubData(target, offset, data);
	}

	public static void qglGetColorTable(int target, int format, int type, int table) {
		QGL.DEBUG_printName("glGetColorTable");
		ARBImaging.glGetColorTable(target, format, type, table);
	}

	public static void qglGetColorTableParameterfv(int target, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glGetColorTableParameterfv");
		ARBImaging.glGetColorTableParameterfv(target, pname, params);
	}

	public static void qglGetColorTableParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetColorTableParameteriv");
		ARBImaging.glGetColorTableParameteriv(target, pname, params);
	}

	public static void qglGetCompressedTexImage(int target, int level, int pixels) {
		QGL.DEBUG_printName("glGetCompressedTexImage");
		GL13.glGetCompressedTexImage(target, level, pixels);
	}

	public static void qglGetConvolutionFilter(int target, int format, int type, int image) {
		QGL.DEBUG_printName("glGetConvolutionFilter");
		ARBImaging.glGetConvolutionFilter(target, format, type, image);
	}

	public static void qglGetConvolutionParameterfv(int target, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glGetConvolutionParameterfv");
		ARBImaging.glGetConvolutionParameterfv(target, pname, params);
	}

	public static void qglGetConvolutionParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetConvolutionParameteriv");
		ARBImaging.glGetConvolutionParameteriv(target, pname, params);
	}

	public static void qglGetHistogram(int target, boolean reset, int format, int type, ByteBuffer values) {
		QGL.DEBUG_printName("glGetHistogram");
		ARBImaging.glGetHistogram(target, reset, format, type, values);
	}

	public static void qglGetHistogramParameterfv(int target, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glGetHistogramParameterfv");
		ARBImaging.glGetHistogramParameterfv(target, pname, params);
	}

	public static void qglGetHistogramParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetHistogramParameteriv");
		ARBImaging.glGetHistogramParameteriv(target, pname, params);
	}

	public static void qglGetMinmax(int target, boolean reset, int format, int type, ByteBuffer values) {
		QGL.DEBUG_printName("glGetMinmax");
		ARBImaging.glGetMinmax(target, reset, format, type, values);
	}

	public static void qglGetMinmaxParameterfv(int target, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glGetMinmaxParameterfv");
		ARBImaging.glGetMinmaxParameterfv(target, pname, params);
	}

	public static void qglGetMinmaxParameteriv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetMinmaxParameteriv");
		ARBImaging.glGetMinmaxParameteriv(target, pname, params);
	}

	/**
	 * 
	 * @param pname
	 * @param params
	 *
	 * PointerBuffer LWJGL Implementation specific.
	 * 
	 * TODO: make method signature LWJGL independent
	 */
	public static void qglGetPointerv(int pname, PointerBuffer params) {
		QGL.DEBUG_printName("glGetPointerv");
		GL46.glGetPointerv(pname, params);
	}

	public static void qglGetProgramEnvParameterdvARB(int target, int index, DoubleBuffer params) {
		QGL.DEBUG_printName("glGetProgramEnvParameterdvARB");
		ARBVertexProgram.glGetProgramEnvParameterdvARB(target, index, params);
	}

	public static void qglGetProgramEnvParameterfvARB(int target, int index, FloatBuffer params) {
		QGL.DEBUG_printName("glGetProgramEnvParameterfvARB");
		ARBVertexProgram.glGetProgramEnvParameterfvARB(target, index, params);
	}

	public static void qglGetProgramivARB(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetProgramivARB");
		ARBVertexProgram.glGetProgramivARB(target, pname, params);
	}

	public static void qglGetProgramLocalParameterdvARB(int target, int index, DoubleBuffer params) {
		QGL.DEBUG_printName("glGetProgramLocalParameterdvARB");
		ARBVertexProgram.glGetProgramLocalParameterdvARB(target, index, params);
	}

	public static void qglGetProgramLocalParameterfvARB(int target, int index, FloatBuffer params) {
		QGL.DEBUG_printName("glGetProgramLocalParameterfvARB");
		ARBVertexProgram.glGetProgramLocalParameterfvARB(target, index, params);
	}

	public static void qglGetProgramStringARB(int target, int pname, ByteBuffer string) {
		QGL.DEBUG_printName("glGetProgramStringARB");
		ARBVertexProgram.glGetProgramStringARB(target, pname, string);
	}

	public static void qglGetQueryiv(int target, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetQueryiv");
		GL15.glGetQueryiv(target, pname, params);
	}

	public static void qglGetQueryObjectiv(int id, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetQueryObjectiv");
		GL15.glGetQueryObjectiv(id, pname, params);
	}

	public static void qglGetQueryObjectuiv(int id, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetQueryObjectuiv");
		GL15.glGetQueryObjectuiv(id, pname, params);
	}

	public static void qglGetSeparableFilter(int target, int format, int type, ByteBuffer row, ByteBuffer column,
			ByteBuffer span) {
		QGL.DEBUG_printName("glGetSeparableFilter");
		ARBImaging.glGetSeparableFilter(target, format, type, row, column, span);
	}

	public static void qglGetVertexAttribdv(int index, int pname, DoubleBuffer params) {
		QGL.DEBUG_printName("glGetVertexAttribdv");
		GL20.glGetVertexAttribdv(index, pname, params);
	}

	public static void qglGetVertexAttribfv(int index, int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glGetVertexAttribfv");
		GL20.glGetVertexAttribfv(index, pname, params);
	}

	public static void qglGetVertexAttribiv(int index, int pname, IntBuffer params) {
		QGL.DEBUG_printName("glGetVertexAttribiv");
		GL20.glGetVertexAttribiv(index, pname, params);
	}

	/**
	 * 
	 * @param index
	 * @param pname
	 * @param pointer
	 *
	 * PointerBuffer LWJGL Implementation specific.
	 * 
	 * TODO: make method signature LWJGL independent
	 */
	public static void qglGetVertexAttribPointerv(int index, int pname, PointerBuffer pointer) {
		QGL.DEBUG_printName("glGetVertexAttribPointerv");
		GL20.glGetVertexAttribPointerv(index, pname, pointer);
	}

	public static void qglHistogram(int target, int width, int internalformat, boolean sink) {
		QGL.DEBUG_printName("glHistogram");
		ARBImaging.glHistogram(target, width, internalformat, sink);
	}

	public static void qglIndexf(float index) {
		QGL.DEBUG_printName("glIndexf");
		GL46.glIndexf(index);
	}

	public static void qglIndexs(short index) {
		QGL.DEBUG_printName("glIndexs");
		GL46.glIndexs(index);
	}

	public static void qglIsBuffer(int buffer) {
		QGL.DEBUG_printName("glIsBuffer");
		GL15.glIsBuffer(buffer);
	}

	public static void qglIsProgram(int program) {
		QGL.DEBUG_printName("glIsProgram");
		GL20.glIsProgram(program);
	}

	public static void qglIsQuery(int id) {
		QGL.DEBUG_printName("glIsQuery");
		GL15.glIsQuery(id);
	}

	public static void qglLoadTransposeMatrixd(DoubleBuffer m) {
		QGL.DEBUG_printName("glLoadTransposeMatrixd");
		GL13.glLoadTransposeMatrixd(m);
	}

	public static void qglLoadTransposeMatrixf(FloatBuffer m) {
		QGL.DEBUG_printName("glLoadTransposeMatrixf");
		GL13.glLoadTransposeMatrixf(m);
	}

	public static void qglMapBuffer(int target, int access) {
		QGL.DEBUG_printName("glMapBuffer");
		GL15.glMapBuffer(target, access);
	}

	public static void qglMaterialf(int face, int pname, float param) {
		QGL.DEBUG_printName("glMaterialf");
		GL46.glMaterialf(face, pname, param);
	}

	public static void qglMateriali(int face, int pname, int param) {
		QGL.DEBUG_printName("glMateriali");
		GL46.glMateriali(face, pname, param);
	}

	public static void qglMatrixIndexPointerARB(int size, int stride, ByteBuffer pointer) {
		QGL.DEBUG_printName("glMatrixIndexPointerARB");
		ARBMatrixPalette.glMatrixIndexPointerARB(size, stride, pointer);
	}

	public static void qglMatrixIndexubvARB(ByteBuffer indices) {
		QGL.DEBUG_printName("glMatrixIndexubvARB");
		ARBMatrixPalette.glMatrixIndexubvARB(indices);
	}

	public static void qglMatrixIndexuivARB(IntBuffer indices) {
		QGL.DEBUG_printName("glMatrixIndexuivARB");
		ARBMatrixPalette.glMatrixIndexuivARB(indices);
	}

	public static void qglMatrixIndexusvARB(ShortBuffer indices) {
		QGL.DEBUG_printName("glMatrixIndexusvARB");
		ARBMatrixPalette.glMatrixIndexusvARB(indices);
	}

	public static void qglMinmax(int target, int internalformat, boolean sink) {
		QGL.DEBUG_printName("glMinmax");
		ARBImaging.glMinmax(target, internalformat, sink);
	}

	public static void qglMultiDrawArrays(int mode, IntBuffer first, IntBuffer count) {
		QGL.DEBUG_printName("glMultiDrawArrays");
		GL14.glMultiDrawArrays(mode, first, count);
	}

	/**
	 * 
	 * @param mode
	 * @param count
	 * @param type
	 * @param indices
	 *
	 * PointerBuffer LWJGL Implementation specific.
	 * 
	 * TODO: make method signature LWJGL independent
	 */
	public static void qglMultiDrawElements(int mode, IntBuffer count, int type, PointerBuffer indices) {
		QGL.DEBUG_printName("glMultiDrawElements");
		GL14.glMultiDrawElements(mode, count, type, indices);
	}

	public static void qglMultiTexCoord1d(int texture, int s) {
		QGL.DEBUG_printName("glMultiTexCoord1d");
		GL13.glMultiTexCoord1d(texture, s);
	}

	public static void qglMultiTexCoord1dARB(int texture, int s) {
		QGL.DEBUG_printName("glMultiTexCoord1dARB");
		ARBMultitexture.glMultiTexCoord1dARB(texture, s);
	}

	public static void qglMultiTexCoord1dv(int texture, DoubleBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1dv");
		GL13.glMultiTexCoord1dv(texture, v);
	}

	public static void qglMultiTexCoord1dvARB(int texture, DoubleBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1dvARB");
		ARBMultitexture.glMultiTexCoord1dvARB(texture, v);
	}

	public static void qglMultiTexCoord1f(int texture, int s) {
		QGL.DEBUG_printName("glMultiTexCoord1f");
		GL13.glMultiTexCoord1f(texture, s);
	}

	public static void qglMultiTexCoord1fARB(int texture, int s) {
		QGL.DEBUG_printName("glMultiTexCoord1fARB");
		ARBMultitexture.glMultiTexCoord1fARB(texture, s);
	}

	public static void qglMultiTexCoord1fv(int texture, FloatBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1fv");
		GL13.glMultiTexCoord1fv(texture, v);
	}

	public static void qglMultiTexCoord1fvARB(int texture, FloatBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1fvARB");
		ARBMultitexture.glMultiTexCoord1fvARB(texture, v);
	}

	public static void qglMultiTexCoord1hNV(int target, short s) {
		QGL.DEBUG_printName("glMultiTexCoord1hNV");
		NVHalfFloat.glMultiTexCoord1hNV(target, s);
	}

	public static void qglMultiTexCoord1hvNV(int target, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1hvNV");
		NVHalfFloat.glMultiTexCoord1hvNV(target, v);
	}

	public static void qglMultiTexCoord1i(int texture, int s) {
		QGL.DEBUG_printName("glMultiTexCoord1i");
		GL13.glMultiTexCoord1i(texture, s);
	}

	public static void qglMultiTexCoord1iARB(int texture, int s) {
		QGL.DEBUG_printName("glMultiTexCoord1iARB");
		ARBMultitexture.glMultiTexCoord1iARB(texture, s);
	}

	public static void qglMultiTexCoord1iv(int texture, IntBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1iv");
		GL13.glMultiTexCoord1iv(texture, v);
	}

	public static void qglMultiTexCoord1ivARB(int texture, IntBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1ivARB");
		ARBMultitexture.glMultiTexCoord1ivARB(texture, v);
	}

	public static void qglMultiTexCoord1s(int texture, short s) {
		QGL.DEBUG_printName("glMultiTexCoord1s");
		GL13.glMultiTexCoord1s(texture, s);
	}

	public static void qglMultiTexCoord1sARB(int texture, short s) {
		QGL.DEBUG_printName("glMultiTexCoord1sARB");
		ARBMultitexture.glMultiTexCoord1sARB(texture, s);
	}

	public static void qglMultiTexCoord1sv(int texture, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1sv");
		GL13.glMultiTexCoord1sv(texture, v);
	}

	public static void qglMultiTexCoord1svARB(int texture, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord1svARB");
		ARBMultitexture.glMultiTexCoord1svARB(texture, v);
	}

	public static void qglMultiTexCoord2d(int texture, int s, int t) {
		QGL.DEBUG_printName("glMultiTexCoord2d");
		GL13.glMultiTexCoord2d(texture, s, t);
	}

	public static void qglMultiTexCoord2dARB(int texture, int s, int t) {
		QGL.DEBUG_printName("glMultiTexCoord2dARB");
		ARBMultitexture.glMultiTexCoord2dARB(texture, s, t);
	}

	public static void qglMultiTexCoord2dv(int texture, DoubleBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2dv");
		GL13.glMultiTexCoord2dv(texture, v);
	}

	public static void qglMultiTexCoord2dvARB(int texture, DoubleBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2dvARB");
		ARBMultitexture.glMultiTexCoord2dvARB(texture, v);
	}

	public static void qglMultiTexCoord2f(int texture, int s, int t) {
		QGL.DEBUG_printName("glMultiTexCoord2f");
		GL13.glMultiTexCoord2f(texture, s, t);
	}

	public static void qglMultiTexCoord2fARB(int texture, int s, int t) {
		QGL.DEBUG_printName("glMultiTexCoord2fARB");
		ARBMultitexture.glMultiTexCoord2fARB(texture, s, t);
	}

	public static void qglMultiTexCoord2fv(int texture, FloatBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2fv");
		GL13.glMultiTexCoord2fv(texture, v);
	}

	public static void qglMultiTexCoord2fvARB(int texture, FloatBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2fvARB");
		ARBMultitexture.glMultiTexCoord2fvARB(texture, v);
	}

	public static void qglMultiTexCoord2hNV(int target, short s, short t) {
		QGL.DEBUG_printName("glMultiTexCoord2hNV");
		NVHalfFloat.glMultiTexCoord2hNV(target, s, t);
	}

	public static void qglMultiTexCoord2hvNV(int target, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2hvNV");
		NVHalfFloat.glMultiTexCoord2hvNV(target, v);
	}

	public static void qglMultiTexCoord2i(int texture, int s, int t) {
		QGL.DEBUG_printName("glMultiTexCoord2i");
		GL13.glMultiTexCoord2i(texture, s, t);
	}

	public static void qglMultiTexCoord2iARB(int texture, int s, int t) {
		QGL.DEBUG_printName("glMultiTexCoord2iARB");
		ARBMultitexture.glMultiTexCoord2iARB(texture, s, t);
	}

	public static void qglMultiTexCoord2iv(int texture, IntBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2iv");
		GL13.glMultiTexCoord2iv(texture, v);
	}

	public static void qglMultiTexCoord2ivARB(int texture, IntBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2ivARB");
		ARBMultitexture.glMultiTexCoord2ivARB(texture, v);
	}

	public static void qglMultiTexCoord2s(int texture, short s, short t) {
		QGL.DEBUG_printName("glMultiTexCoord2s");
		GL13.glMultiTexCoord2s(texture, s, t);
	}

	public static void qglMultiTexCoord2sARB(int texture, short s, short t) {
		QGL.DEBUG_printName("glMultiTexCoord2sARB");
		ARBMultitexture.glMultiTexCoord2sARB(texture, s, t);
	}

	public static void qglMultiTexCoord2sv(int texture, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2sv");
		GL13.glMultiTexCoord2sv(texture, v);
	}

	public static void qglMultiTexCoord2svARB(int texture, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord2svARB");
		ARBMultitexture.glMultiTexCoord2svARB(texture, v);
	}

	public static void qglMultiTexCoord3d(int texture, int s, int t, int r) {
		QGL.DEBUG_printName("glMultiTexCoord3d");
		GL13.glMultiTexCoord3d(texture, s, t, r);
	}

	public static void qglMultiTexCoord3dARB(int texture, int s, int t, int r) {
		QGL.DEBUG_printName("glMultiTexCoord3dARB");
		ARBMultitexture.glMultiTexCoord3dARB(texture, s, t, r);
	}

	public static void qglMultiTexCoord3dv(int texture, DoubleBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3dv");
		GL13.glMultiTexCoord3dv(texture, v);
	}

	public static void qglMultiTexCoord3dvARB(int texture, DoubleBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3dvARB");
		ARBMultitexture.glMultiTexCoord3dvARB(texture, v);
	}

	public static void qglMultiTexCoord3f(int texture, int s, int t, int r) {
		QGL.DEBUG_printName("glMultiTexCoord3f");
		GL13.glMultiTexCoord3f(texture, s, t, r);
	}

	public static void qglMultiTexCoord3fARB(int texture, int s, int t, int r) {
		QGL.DEBUG_printName("glMultiTexCoord3fARB");
		ARBMultitexture.glMultiTexCoord3fARB(texture, s, t, r);
	}

	public static void qglMultiTexCoord3fv(int texture, FloatBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3fv");
		GL13.glMultiTexCoord3fv(texture, v);
	}

	public static void qglMultiTexCoord3fvARB(int texture, FloatBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3fvARB");
		ARBMultitexture.glMultiTexCoord3fvARB(texture, v);
	}

	public static void qglMultiTexCoord3hNV(int target, short s, short t, short r) {
		QGL.DEBUG_printName("glMultiTexCoord3hNV");
		NVHalfFloat.glMultiTexCoord3hNV(target, s, t, r);
	}

	public static void qglMultiTexCoord3hvNV(int target, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3hvNV");
		NVHalfFloat.glMultiTexCoord3hvNV(target, v);
	}

	public static void qglMultiTexCoord3i(int texture, int s, int t, int r) {
		QGL.DEBUG_printName("glMultiTexCoord3i");
		GL13.glMultiTexCoord3i(texture, s, t, r);
	}

	public static void qglMultiTexCoord3iARB(int texture, int s, int t, int r) {
		QGL.DEBUG_printName("glMultiTexCoord3iARB");
		ARBMultitexture.glMultiTexCoord3iARB(texture, s, t, r);
	}

	public static void qglMultiTexCoord3iv(int texture, IntBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3iv");
		GL13.glMultiTexCoord3iv(texture, v);
	}

	public static void qglMultiTexCoord3ivARB(int texture, IntBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3ivARB");
		ARBMultitexture.glMultiTexCoord3ivARB(texture, v);
	}

	public static void qglMultiTexCoord3s(int texture, short s, short t, short r) {
		QGL.DEBUG_printName("glMultiTexCoord3s");
		GL13.glMultiTexCoord3s(texture, s, t, r);
	}

	public static void qglMultiTexCoord3sARB(int texture, short s, short t, short r) {
		QGL.DEBUG_printName("glMultiTexCoord3sARB");
		ARBMultitexture.glMultiTexCoord3sARB(texture, s, t, r);
	}

	public static void qglMultiTexCoord3sv(int texture, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3sv");
		GL13.glMultiTexCoord3sv(texture, v);
	}

	public static void qglMultiTexCoord3svARB(int texture, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord3svARB");
		ARBMultitexture.glMultiTexCoord3svARB(texture, v);
	}

	public static void qglMultiTexCoord4d(int texture, int s, int t, int r, int q) {
		QGL.DEBUG_printName("glMultiTexCoord4d");
		GL13.glMultiTexCoord4d(texture, s, t, r, q);
	}

	public static void qglMultiTexCoord4dARB(int texture, int s, int t, int r, int q) {
		QGL.DEBUG_printName("glMultiTexCoord4dARB");
		ARBMultitexture.glMultiTexCoord4dARB(texture, s, t, r, q);
	}

	public static void qglMultiTexCoord4dv(int texture, DoubleBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4dv");
		GL13.glMultiTexCoord4dv(texture, v);
	}

	public static void qglMultiTexCoord4dvARB(int texture, DoubleBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4dvARB");
		ARBMultitexture.glMultiTexCoord4dvARB(texture, v);
	}

	public static void qglMultiTexCoord4f(int texture, int s, int t, int r, int q) {
		QGL.DEBUG_printName("glMultiTexCoord4f");
		GL13.glMultiTexCoord4f(texture, s, t, r, q);
	}

	public static void qglMultiTexCoord4fARB(int texture, int s, int t, int r, int q) {
		QGL.DEBUG_printName("glMultiTexCoord4fARB");
		ARBMultitexture.glMultiTexCoord4fARB(texture, s, t, r, q);
	}

	public static void qglMultiTexCoord4fv(int texture, FloatBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4fv");
		GL13.glMultiTexCoord4fv(texture, v);
	}

	public static void qglMultiTexCoord4fvARB(int texture, FloatBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4fvARB");
		ARBMultitexture.glMultiTexCoord4fvARB(texture, v);
	}

	public static void qglMultiTexCoord4hNV(int target, short s, short t, short r, short q) {
		QGL.DEBUG_printName("glMultiTexCoord4hNV");
		NVHalfFloat.glMultiTexCoord4hNV(target, s, t, r, q);
	}

	public static void qglMultiTexCoord4hvNV(int target, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4hvNV");
		NVHalfFloat.glMultiTexCoord4hvNV(target, v);
	}

	public static void qglMultiTexCoord4i(int texture, int s, int t, int r, int q) {
		QGL.DEBUG_printName("qglMultiTexCoord4i");
		GL13.glMultiTexCoord4i(texture, s, t, r, q);
		;
	}

	public static void qglMultiTexCoord4iARB(int texture, int s, int t, int r, int q) {
		QGL.DEBUG_printName("glMultiTexCoord4iARB");
		ARBMultitexture.glMultiTexCoord4iARB(texture, s, t, r, q);
	}

	public static void qglMultiTexCoord4iv(int texture, IntBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4iv");
		GL13.glMultiTexCoord4iv(texture, v);
	}

	public static void qglMultiTexCoord4ivARB(int texture, IntBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4ivARB");
		ARBMultitexture.glMultiTexCoord4ivARB(texture, v);
	}

	public static void qglMultiTexCoord4s(int texture, short s, short t, short r, short q) {
		QGL.DEBUG_printName("glMultiTexCoord4s");
		GL13.glMultiTexCoord4s(texture, s, t, r, q);
	}

	public static void qglMultiTexCoord4sARB(int texture, short s, short t, short r, short q) {
		QGL.DEBUG_printName("glMultiTexCoord4sARB");
		ARBMultitexture.glMultiTexCoord4sARB(texture, s, t, r, q);
	}

	public static void qglMultiTexCoord4sv(int texture, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4sv");
		GL13.glMultiTexCoord4sv(texture, v);
	}

	public static void qglMultiTexCoord4svARB(int texture, ShortBuffer v) {
		QGL.DEBUG_printName("glMultiTexCoord4svARB");
		ARBMultitexture.glMultiTexCoord4svARB(texture, v);
	}

	public static void qglMultTransposeMatrixd(DoubleBuffer m) {
		QGL.DEBUG_printName("glMultTransposeMatrixd");
		GL13.glMultTransposeMatrixd(m);
	}

	public static void qglMultTransposeMatrixdARB(DoubleBuffer m) {
		QGL.DEBUG_printName("glMultTransposeMatrixdARB");
		ARBTransposeMatrix.glMultTransposeMatrixdARB(m);
	}

	public static void qglMultTransposeMatrixf(FloatBuffer m) {
		QGL.DEBUG_printName("glMultTransposeMatrixf");
		GL13.glMultTransposeMatrixf(m);
	}

	public static void qglMultTransposeMatrixfARB(FloatBuffer m) {
		QGL.DEBUG_printName("glMultTransposeMatrixfARB");
		ARBTransposeMatrix.glMultTransposeMatrixfARB(m);
	}

	public static void qglNormal3hNV(short nx, short ny, short nz) {
		QGL.DEBUG_printName("glNormal3hNV");
		NVHalfFloat.glNormal3hNV(nx, ny, nz);
	}

	public static void qglNormal3hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glNormal3hvNV");
		NVHalfFloat.glNormal3hvNV(v);
	}

	public static void qglPixelDataRangeNV(int target, ByteBuffer pointer) {
		QGL.DEBUG_printName("glPixelDataRangeNV");
		NVPixelDataRange.glPixelDataRangeNV(target, pointer);
	}

	public static void qglPointParameterf(int pname, int param) {
		QGL.DEBUG_printName("glPointParameterf");
		GL14.glPointParameterf(pname, param);
	}

	public static void qglPointParameterfARB(int pname, int param) {
		QGL.DEBUG_printName("glPointParameterfARB");
		ARBPointParameters.glPointParameterfARB(pname, param);
	}

	public static void qglPointParameterfv(int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glPointParameterfv");
		GL14.glPointParameterfv(pname, params);
	}

	public static void qglPointParameterfvARB(int pname, FloatBuffer params) {
		QGL.DEBUG_printName("glPointParameterfvARB");
		ARBPointParameters.glPointParameterfvARB(pname, params);
	}

	public static void qglPointParameteri(int pname, int param) {
		QGL.DEBUG_printName("glPointParameteri");
		GL14.glPointParameteri(pname, param);
	}

	public static void qglPointParameteriv(int pname, IntBuffer params) {
		QGL.DEBUG_printName("glPointParameteriv");
		GL14.glPointParameteriv(pname, params);
	}

	public static void qglPrimitiveRestartIndexNV(int index) {
		QGL.DEBUG_printName("glPrimitiveRestartIndexNV");
		NVPrimitiveRestart.glPrimitiveRestartIndexNV(index);
	}

	public static void qglPrimitiveRestartNV() {
		QGL.DEBUG_printName("glPrimitiveRestartNV");
		NVPrimitiveRestart.glPrimitiveRestartNV();
	}

	public static void qglProgramEnvParameter4dARB(int target, int index, int x, int y, int z, int w) {
		QGL.DEBUG_printName("glProgramEnvParameter4dARB");
		ARBVertexProgram.glProgramEnvParameter4dARB(target, index, x, y, z, w);
	}

	public static void qglProgramEnvParameter4dvARB(int target, int index, DoubleBuffer params) {
		QGL.DEBUG_printName("glProgramEnvParameter4dvARB");
		ARBVertexProgram.glProgramEnvParameter4dvARB(target, index, params);
	}

	public static void qglProgramEnvParameter4fARB(int target, int index, int x, int y, int z, int w) {
		QGL.DEBUG_printName("glProgramEnvParameter4fARB");
		ARBVertexProgram.glProgramEnvParameter4fARB(target, index, x, y, z, w);
	}

	public static void qglProgramLocalParameter4dARB(int target, int index, int x, int y, int z, int w) {
		QGL.DEBUG_printName("glProgramLocalParameter4dARB");
		ARBVertexProgram.glProgramLocalParameter4dARB(target, index, x, y, z, w);
	}

	public static void qglProgramLocalParameter4dvARB(int target, int index, DoubleBuffer params) {
		QGL.DEBUG_printName("glProgramLocalParameter4dvARB");
		ARBVertexProgram.glProgramLocalParameter4dvARB(target, index, params);
	}

	public static void qglProgramLocalParameter4fARB(int target, int index, int x, int y, int z, int w) {
		QGL.DEBUG_printName("glProgramLocalParameter4fARB");
		ARBVertexProgram.glProgramLocalParameter4fARB(target, index, x, y, z, w);
	}

	public static void qglResetHistogram(int target) {
		QGL.DEBUG_printName("glResetHistogram");
		ARBImaging.glResetHistogram(target);
	}

	public static void qglResetMinmax(int target) {
		QGL.DEBUG_printName("glResetMinmax");
		ARBImaging.glResetMinmax(target);
	}

	public static void qglSampleCoverage(float value, boolean invert) {
		QGL.DEBUG_printName("glSampleCoverageARB");
		GL13.glSampleCoverage(value, invert);
	}

	public static void qglSampleCoverageARB(float value, boolean invert) {
		QGL.DEBUG_printName("glSampleCoverageARB");
		ARBMultisample.glSampleCoverageARB(value, invert);
	}

	public static void qglSecondaryColor3bEXT(byte red, byte green, byte blue) {
		QGL.DEBUG_printName("glSecondaryColor3bEXT");
		EXTSecondaryColor.glSecondaryColor3bEXT(red, green, blue);
	}

	public static void qglSecondaryColor3bvEXT(ByteBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3bvEXT");
		EXTSecondaryColor.glSecondaryColor3bvEXT(v);
	}

	public static void qglSecondaryColor3dEXT(int red, int green, int blue) {
		QGL.DEBUG_printName("glSecondaryColor3dEXT");
		EXTSecondaryColor.glSecondaryColor3dEXT(red, green, blue);
	}

	public static void qglSecondaryColor3dvEXT(DoubleBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3dvEXT");
		EXTSecondaryColor.glSecondaryColor3dvEXT(v);
	}

	public static void qglSecondaryColor3fEXT(int red, int green, int blue) {
		QGL.DEBUG_printName("glSecondaryColor3fEXT");
		EXTSecondaryColor.glSecondaryColor3fEXT(red, green, blue);
	}

	public static void qglSecondaryColor3fvEXT(FloatBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3fvEXT");
		EXTSecondaryColor.glSecondaryColor3fvEXT(v);
	}

	public static void qglSecondaryColor3hNV(short red, short green, short blue) {
		QGL.DEBUG_printName("glSecondaryColor3hNV");
		NVHalfFloat.glSecondaryColor3hNV(red, green, blue);
	}

	public static void qglSecondaryColor3hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3hvNV");
		NVHalfFloat.glSecondaryColor3hvNV(v);
	}

	public static void qglSecondaryColor3iEXT(int red, int green, int blue) {
		QGL.DEBUG_printName("glSecondaryColor3iEXT");
		EXTSecondaryColor.glSecondaryColor3iEXT(red, green, blue);
	}

	public static void qglSecondaryColor3ivEXT(IntBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3ivEXT");
		EXTSecondaryColor.glSecondaryColor3ivEXT(v);
	}

	public static void qglSecondaryColor3sEXT(short red, short green, short blue) {
		QGL.DEBUG_printName("glSecondaryColor3sEXT");
		EXTSecondaryColor.glSecondaryColor3sEXT(red, green, blue);
	}

	public static void qglSecondaryColor3svEXT(ShortBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3svEXT");
		EXTSecondaryColor.glSecondaryColor3svEXT(v);
	}

	public static void qglSecondaryColor3ubEXT(byte red, byte green, byte blue) {
		QGL.DEBUG_printName("glSecondaryColor3ubEXT");
		EXTSecondaryColor.glSecondaryColor3ubEXT(red, green, blue);
	}

	public static void qglSecondaryColor3ubvEXT(ByteBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3ubvEXT");
		EXTSecondaryColor.glSecondaryColor3ubvEXT(v);
	}

	public static void qglSecondaryColor3uiEXT(int red, int green, int blue) {
		QGL.DEBUG_printName("glSecondaryColor3uiEXT");
		EXTSecondaryColor.glSecondaryColor3uiEXT(red, green, blue);
	}

	public static void qglSecondaryColor3uivEXT(IntBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3uivEXT");
		EXTSecondaryColor.glSecondaryColor3uivEXT(v);
	}

	public static void qglSecondaryColor3usEXT(short red, short green, short blue) {
		QGL.DEBUG_printName("glSecondaryColor3usEXT");
		EXTSecondaryColor.glSecondaryColor3usEXT(red, green, blue);
	}

	public static void qglSecondaryColor3usvEXT(ShortBuffer v) {
		QGL.DEBUG_printName("glSecondaryColor3usvEXT");
		EXTSecondaryColor.glSecondaryColor3usvEXT(v);
	}

	public static void qglSecondaryColorPointerEXT(int size, int type, int stride, int pointer) {
		QGL.DEBUG_printName("glSecondaryColorPointerEXT");
		EXTSecondaryColor.glSecondaryColorPointerEXT(size, type, stride, pointer);
	}

	public static void qglSeparableFilter2D(int target, int internalformat, int width, int height, int format, int type,
			int row, int column) {
		QGL.DEBUG_printName("glSeparableFilter2D");
		ARBImaging.glSeparableFilter2D(target, internalformat, width, height, format, type, row, column);
	}

	public static void qglStencilFuncSeparate(int face, int func, int ref, int mask) {
		QGL.DEBUG_printName("glStencilFuncSeparate");
		GL20.glStencilFuncSeparate(face, func, ref, mask);
	}

	public static void qglStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
		QGL.DEBUG_printName("glStencilOpSeparate");
		GL20.glStencilOpSeparate(face, sfail, dpfail, dppass);
	}

	public static void qglTexCoord1hNV(short s) {
		QGL.DEBUG_printName("glTexCoord1hNV");
		NVHalfFloat.glTexCoord1hNV(s);
	}

	public static void qglTexCoord1hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glTexCoord1hvNV");
		NVHalfFloat.glTexCoord1hvNV(v);
	}

	public static void qglTexCoord2hNV(short s, short t) {
		QGL.DEBUG_printName("glTexCoord2hNV");
		NVHalfFloat.glTexCoord2hNV(s, t);
	}

	public static void qglTexCoord2hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glTexCoord2hvNV");
		NVHalfFloat.glTexCoord2hvNV(v);
	}

	public static void qglTexCoord3hNV(short s, short t, short r) {
		QGL.DEBUG_printName("glTexCoord3hNV");
		NVHalfFloat.glTexCoord3hNV(s, t, r);
	}

	public static void qglTexCoord3hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glTexCoord3hvNV");
		NVHalfFloat.glTexCoord3hvNV(v);
	}

	public static void qglTexCoord4hNV(short s, short t, short r, short q) {
		QGL.DEBUG_printName("glTexCoord4hNV");
		NVHalfFloat.glTexCoord4hNV(s, t, r, q);
	}

	public static void qglTexCoord4hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glTexCoord4hvNV");
		NVHalfFloat.glTexCoord4hvNV(v);
	}

	public static void qglTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width,
			int height, int depth, int format, int type, int pixels) {
		QGL.DEBUG_printName("glTexSubImage3D");
		GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels);
	}

	public static void qglUnmapBuffer(int target) {
		QGL.DEBUG_printName("glUnmapBuffer");
		GL15.glUnmapBuffer(target);
	}

	public static void qglVertex2hNV(short x, short y) {
		QGL.DEBUG_printName("glVertex2hNV");
		NVHalfFloat.glVertex2hNV(x, y);
	}

	public static void qglVertex2hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glVertex2hvNV");
		NVHalfFloat.glVertex2hvNV(v);
	}

	public static void qglVertex3hNV(short x, short y, short z) {
		QGL.DEBUG_printName("glVertex3hNV");
		NVHalfFloat.glVertex3hNV(x, y, z);
	}

	public static void qglVertex3hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glVertex3hvNV");
		NVHalfFloat.glVertex3hvNV(v);
	}

	public static void qglVertex4hNV(short x, short y, short z, short w) {
		QGL.DEBUG_printName("glVertex4hNV");
		NVHalfFloat.glVertex4hNV(x, y, z, w);
	}

	public static void qglVertex4hvNV(ShortBuffer v) {
		QGL.DEBUG_printName("glVertex4hvNV");
		NVHalfFloat.glVertex4hvNV(v);
	}

	public static void qglVertexAttrib1d(int index, int v0) {
		QGL.DEBUG_printName("glVertexAttrib1d");
		GL20.glVertexAttrib1d(index, v0);
	}

	public static void qglVertexAttrib1dARB(int index, int v0) {
		QGL.DEBUG_printName("glVertexAttrib1dARB");
		ARBVertexProgram.glVertexAttrib1dARB(index, v0);
	}

	public static void qglVertexAttrib1dv(int index, DoubleBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib1dv");
		GL20.glVertexAttrib1dv(index, v);
	}

	public static void qglVertexAttrib1dvARB(int index, DoubleBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib1dvARB");
		ARBVertexProgram.glVertexAttrib1dvARB(index, v);
	}

	public static void qglVertexAttrib1f(int index, int v0) {
		QGL.DEBUG_printName("glVertexAttrib1f");
		GL20.glVertexAttrib1f(index, v0);
	}

	public static void qglVertexAttrib1fARB(int index, int v0) {
		QGL.DEBUG_printName("glVertexAttrib1fARB");
		ARBVertexProgram.glVertexAttrib1fARB(index, v0);
	}

	public static void qglVertexAttrib1fv(int index, FloatBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib1fv");
		GL20.glVertexAttrib1fv(index, v);
	}

	public static void qglVertexAttrib1fvARB(int index, FloatBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib1fvARB");
		ARBVertexProgram.glVertexAttrib1fvARB(index, v);
	}

	public static void qglVertexAttrib1hNV(int index, short x) {
		QGL.DEBUG_printName("glVertexAttrib1hNV");
		NVHalfFloat.glVertexAttrib1hNV(index, x);
	}

	public static void qglVertexAttrib1hvNV(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib1hvNV");
		NVHalfFloat.glVertexAttrib1hvNV(index, v);
	}

	public static void qglVertexAttrib1s(int index, short v0) {
		QGL.DEBUG_printName("glVertexAttrib1s");
		GL20.glVertexAttrib1s(index, v0);
	}

	public static void qglVertexAttrib1sARB(int index, short v0) {
		QGL.DEBUG_printName("glVertexAttrib1sARB");
		ARBVertexProgram.glVertexAttrib1sARB(index, v0);
	}

	public static void qglVertexAttrib1sv(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib1sv");
		GL20.glVertexAttrib1sv(index, v);
	}

	public static void qglVertexAttrib1svARB(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib1svARB");
		ARBVertexProgram.glVertexAttrib1svARB(index, v);
	}

	public static void qglVertexAttrib2d(int index, int v0, int v1) {
		QGL.DEBUG_printName("glVertexAttrib2d");
		GL20.glVertexAttrib2d(index, v0, v1);
	}

	public static void qglVertexAttrib2dARB(int index, int v0, int v1) {
		QGL.DEBUG_printName("glVertexAttrib2dARB");
		ARBVertexProgram.glVertexAttrib2dARB(index, v0, v1);
	}

	public static void qglVertexAttrib2dv(int index, DoubleBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib2dv");
		GL20.glVertexAttrib2dv(index, v);
	}

	public static void qglVertexAttrib2dvARB(int index, DoubleBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib2dvARB");
		ARBVertexProgram.glVertexAttrib2dvARB(index, v);
	}

	public static void qglVertexAttrib2f(int index, int v0, int v1) {
		QGL.DEBUG_printName("glVertexAttrib2f");
		GL20.glVertexAttrib2f(index, v0, v1);
	}

	public static void qglVertexAttrib2fARB(int index, int v0, int v1) {
		QGL.DEBUG_printName("glVertexAttrib2fARB");
		ARBVertexProgram.glVertexAttrib2fARB(index, v0, v1);
	}

	public static void qglVertexAttrib2fv(int index, FloatBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib2fv");
		GL20.glVertexAttrib2fv(index, v);
	}

	public static void qglVertexAttrib2fvARB(int index, FloatBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib2fvARB");
		ARBVertexProgram.glVertexAttrib2fvARB(index, v);
	}

	public static void qglVertexAttrib2hNV(int index, short x, short y) {
		QGL.DEBUG_printName("glVertexAttrib2hNV");
		NVHalfFloat.glVertexAttrib2hNV(index, x, y);
	}

	public static void qglVertexAttrib2hvNV(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib2hvNV");
		NVHalfFloat.glVertexAttrib2hvNV(index, v);
	}

	public static void qglVertexAttrib2s(int index, short v0, short v1) {
		QGL.DEBUG_printName("glVertexAttrib2s");
		GL20.glVertexAttrib2s(index, v0, v1);
	}

	public static void qglVertexAttrib2sARB(int index, short v0, short v1) {
		QGL.DEBUG_printName("glVertexAttrib2sARB");
		ARBVertexProgram.glVertexAttrib2sARB(index, v0, v1);
	}

	public static void qglVertexAttrib2sv(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib2sv");
		GL20.glVertexAttrib2sv(index, v);
	}

	public static void qglVertexAttrib2svARB(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib2svARB");
		ARBVertexProgram.glVertexAttrib2svARB(index, v);
	}

	public static void qglVertexAttrib3d(int index, int v0, int v1, int v2) {
		QGL.DEBUG_printName("glVertexAttrib3d");
		GL20.glVertexAttrib3d(index, v0, v1, v2);
	}

	public static void qglVertexAttrib3dARB(int index, int v0, int v1, int v2) {
		QGL.DEBUG_printName("glVertexAttrib3dARB");
		ARBVertexProgram.glVertexAttrib3dARB(index, v0, v1, v2);
	}

	public static void qglVertexAttrib3dv(int index, DoubleBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib3dv");
		GL20.glVertexAttrib3dv(index, v);
	}

	public static void qglVertexAttrib3dvARB(int index, DoubleBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib3dvARB");
		ARBVertexProgram.glVertexAttrib3dvARB(index, v);
	}

	public static void qglVertexAttrib3f(int index, int v0, int v1, int v2) {
		QGL.DEBUG_printName("glVertexAttrib3f");
		GL20.glVertexAttrib3f(index, v0, v1, v2);
	}

	public static void qglVertexAttrib3fARB(int index, int v0, int v1, int v2) {
		QGL.DEBUG_printName("glVertexAttrib3fARB");
		ARBVertexProgram.glVertexAttrib3fARB(index, v0, v1, v2);
	}

	public static void qglVertexAttrib3fv(int index, FloatBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib3fv");
		GL20.glVertexAttrib3fv(index, v);
	}

	public static void qglVertexAttrib3fvARB(int index, FloatBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib3fvARB");
		ARBVertexProgram.glVertexAttrib3fvARB(index, v);
	}

	public static void qglVertexAttrib3hNV(int index, short x, short y, short z) {
		QGL.DEBUG_printName("glVertexAttrib3hNV");
		NVHalfFloat.glVertexAttrib3hNV(index, x, y, z);
	}

	public static void qglVertexAttrib3hvNV(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib3hvNV");
		NVHalfFloat.glVertexAttrib3hvNV(index, v);
	}

	public static void qglVertexAttrib3s(int index, short v0, short v1, short v2) {
		QGL.DEBUG_printName("glVertexAttrib3s");
		GL20.glVertexAttrib3s(index, v0, v1, v2);
	}

	public static void qglVertexAttrib3sARB(int index, short v0, short v1, short v2) {
		QGL.DEBUG_printName("glVertexAttrib3sARB");
		ARBVertexProgram.glVertexAttrib3sARB(index, v0, v1, v2);
	}

	public static void qglVertexAttrib3sv(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib3sv");
		GL20.glVertexAttrib3sv(index, v);
	}

	public static void qglVertexAttrib3svARB(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib3svARB");
		ARBVertexProgram.glVertexAttrib3svARB(index, v);
	}

	public static void qglVertexAttrib4bv(int index, ByteBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4bv");
		GL20.glVertexAttrib4bv(index, v);
	}

	public static void qglVertexAttrib4bvARB(int index, ByteBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4bvARB");
		ARBVertexProgram.glVertexAttrib4bvARB(index, v);
	}

	public static void qglVertexAttrib4d(int index, int v0, int v1, int v2, int v3) {
		QGL.DEBUG_printName("glVertexAttrib4d");
		GL20.glVertexAttrib4d(index, v0, v1, v2, v3);
	}

	public static void qglVertexAttrib4dARB(int index, int v0, int v1, int v2, int v3) {
		QGL.DEBUG_printName("glVertexAttrib4dARB");
		ARBVertexProgram.glVertexAttrib4dARB(index, v0, v1, v2, v3);
	}

	public static void qglVertexAttrib4dv(int index, DoubleBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4dv");
		GL20.glVertexAttrib4dv(index, v);
	}

	public static void qglVertexAttrib4dvARB(int index, DoubleBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4dvARB");
		ARBVertexProgram.glVertexAttrib4dvARB(index, v);
	}

	public static void qglVertexAttrib4f(int index, int v0, int v1, int v2, int v3) {
		QGL.DEBUG_printName("glVertexAttrib4f");
		GL20.glVertexAttrib4f(index, v0, v1, v2, v3);
	}

	public static void qglVertexAttrib4fARB(int index, int v0, int v1, int v2, int v3) {
		QGL.DEBUG_printName("glVertexAttrib4fARB");
		ARBVertexProgram.glVertexAttrib4fARB(index, v0, v1, v2, v3);
	}

	public static void qglVertexAttrib4fv(int index, FloatBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4fv");
		GL20.glVertexAttrib4fv(index, v);
	}

	public static void qglVertexAttrib4fvARB(int index, FloatBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4fvARB");
		ARBVertexProgram.glVertexAttrib4fvARB(index, v);
	}

	public static void qglVertexAttrib4hNV(int index, short x, short y, short z, short w) {
		QGL.DEBUG_printName("glVertexAttrib4hNV");
		NVHalfFloat.glVertexAttrib4hNV(index, x, y, z, w);
	}

	public static void qglVertexAttrib4hvNV(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4hvNV");
		NVHalfFloat.glVertexAttrib4hvNV(index, v);
	}

	public static void qglVertexAttrib4iv(int index, IntBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4iv");
		GL20.glVertexAttrib4iv(index, v);
	}

	public static void qglVertexAttrib4ivARB(int index, IntBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4ivARB");
		ARBVertexProgram.glVertexAttrib4ivARB(index, v);
	}

	public static void qglVertexAttrib4Nbv(int index, ByteBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4Nbv");
		GL20.glVertexAttrib4Nbv(index, v);
	}

	public static void qglVertexAttrib4NbvARB(int index, ByteBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4NbvARB");
		ARBVertexProgram.glVertexAttrib4NbvARB(index, v);
	}

	public static void qglVertexAttrib4Niv(int index, IntBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4Niv");
		GL20.glVertexAttrib4Niv(index, v);
	}

	public static void qglVertexAttrib4NivARB(int index, IntBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4NivARB");
		ARBVertexProgram.glVertexAttrib4NivARB(index, v);
	}

	public static void qglVertexAttrib4Nsv(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4Nsv");
		GL20.glVertexAttrib4Nsv(index, v);
	}

	public static void qglVertexAttrib4NsvARB(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4NsvARB");
		ARBVertexProgram.glVertexAttrib4NsvARB(index, v);
	}

	public static void qglVertexAttrib4Nub(int index, byte x, byte y, byte z, byte w) {
		QGL.DEBUG_printName("glVertexAttrib4Nub");
		GL20.glVertexAttrib4Nub(index, x, y, z, w);
	}

	public static void qglVertexAttrib4NubARB(int index, byte x, byte y, byte z, byte w) {
		QGL.DEBUG_printName("glVertexAttrib4NubARB");
		ARBVertexProgram.glVertexAttrib4NubARB(index, x, y, z, w);
	}

	public static void qglVertexAttrib4Nubv(int index, ByteBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4Nubv");
		GL20.glVertexAttrib4Nubv(index, v);
	}

	public static void qglVertexAttrib4NubvARB(int index, ByteBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4NubvARB");
		ARBVertexProgram.glVertexAttrib4NubvARB(index, v);
	}

	public static void qglVertexAttrib4Nuiv(int index, IntBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4Nuiv");
		GL20.glVertexAttrib4Nuiv(index, v);
	}

	public static void qglVertexAttrib4NuivARB(int index, IntBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4NuivARB");
		ARBVertexProgram.glVertexAttrib4NuivARB(index, v);
	}

	public static void qglVertexAttrib4Nusv(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4Nusv");
		GL20.glVertexAttrib4Nusv(index, v);
	}

	public static void qglVertexAttrib4NusvARB(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4NusvARB");
		ARBVertexProgram.glVertexAttrib4NusvARB(index, v);
	}

	public static void qglVertexAttrib4s(int index, short v0, short v1, short v2, short v3) {
		QGL.DEBUG_printName("glVertexAttrib4s");
		GL20.glVertexAttrib4s(index, v0, v1, v2, v3);
	}

	public static void qglVertexAttrib4sARB(int index, short v0, short v1, short v2, short v3) {
		QGL.DEBUG_printName("glVertexAttrib4sARB");
		ARBVertexProgram.glVertexAttrib4sARB(index, v0, v1, v2, v3);
	}

	public static void qglVertexAttrib4sv(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4sv");
		GL20.glVertexAttrib4sv(index, v);
	}

	public static void qglVertexAttrib4svARB(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4svARB");
		ARBVertexProgram.glVertexAttrib4svARB(index, v);
	}

	public static void qglVertexAttrib4ubv(int index, ByteBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4ubv");
		GL20.glVertexAttrib4ubv(index, v);
	}

	public static void qglVertexAttrib4ubvARB(int index, ByteBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4ubvARB");
		ARBVertexProgram.glVertexAttrib4ubvARB(index, v);
	}

	public static void qglVertexAttrib4uiv(int index, IntBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4uiv");
		GL20.glVertexAttrib4uiv(index, v);
	}

	public static void qglVertexAttrib4uivARB(int index, IntBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4ubvARB");
		ARBVertexProgram.glVertexAttrib4uivARB(index, v);
	}

	public static void qglVertexAttrib4usv(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4usv");
		GL20.glVertexAttrib4usv(index, v);
	}

	public static void qglVertexAttrib4usvARB(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttrib4usvARB");
		ARBVertexProgram.glVertexAttrib4usvARB(index, v);
	}

	public static void qglVertexAttribs1hvNV(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttribs1hvNV");
		NVHalfFloat.glVertexAttribs1hvNV(index, v);
	}

	public static void qglVertexAttribs2hvNV(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttribs2hvNV");
		NVHalfFloat.glVertexAttribs2hvNV(index, v);
	}

	public static void qglVertexAttribs3hvNV(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttribs3hvNV");
		NVHalfFloat.glVertexAttribs3hvNV(index, v);
	}

	public static void qglVertexAttribs4hvNV(int index, ShortBuffer v) {
		QGL.DEBUG_printName("glVertexAttribs4hvNV");
		NVHalfFloat.glVertexAttribs4hvNV(index, v);
	}

	public static void qglVertexBlendARB(int count) {
		QGL.DEBUG_printName("glVertexBlendARB");
		ARBVertexBlend.glVertexBlendARB(count);
	}

	public static void qglVertexWeighthNV(short weight) {
		QGL.DEBUG_printName("glVertexWeighthNV");
		NVHalfFloat.glVertexWeighthNV(weight);
	}

	public static void qglVertexWeighthvNV(ShortBuffer weight) {
		QGL.DEBUG_printName("glVertexWeighthvNV");
		NVHalfFloat.glVertexWeighthvNV(weight);
	}

	public static void qglWeightbvARB(ByteBuffer weights) {
		QGL.DEBUG_printName("glWeightbvARB");
		ARBVertexBlend.glWeightbvARB(weights);
	}

	public static void qglWeightdvARB(DoubleBuffer weights) {
		QGL.DEBUG_printName("glWeightdvARB");
		ARBVertexBlend.glWeightdvARB(weights);
	}

	public static void qglWeightfvARB(FloatBuffer weights) {
		QGL.DEBUG_printName("glWeightfvARB");
		ARBVertexBlend.glWeightfvARB(weights);
	}

	public static void qglWeightivARB(IntBuffer weights) {
		QGL.DEBUG_printName("glWeightivARB");
		ARBVertexBlend.glWeightivARB(weights);
	}

	public static void qglWeightPointerARB(int size, int type, int stride, int pointer) {
		QGL.DEBUG_printName("glWeightPointerARB");
		ARBVertexBlend.glWeightPointerARB(size, type, stride, pointer);
	}

	public static void qglWeightsvARB(ShortBuffer weights) {
		QGL.DEBUG_printName("glWeightsvARB");
		ARBVertexBlend.glWeightsvARB(weights);
	}

	public static void qglWeightubvARB(ByteBuffer weights) {
		QGL.DEBUG_printName("glWeightubvARB");
		ARBVertexBlend.glWeightubvARB(weights);
	}

	public static void qglWeightuivARB(IntBuffer weights) {
		QGL.DEBUG_printName("glWeightuivARB");
		ARBVertexBlend.glWeightuivARB(weights);
	}

	public static void qglWeightusvARB(ShortBuffer weights) {
		QGL.DEBUG_printName("glWeightusvARB");
		ARBVertexBlend.glWeightusvARB(weights);
	}

	public static void qglWindowPos2d(double x, double y) {
		QGL.DEBUG_printName("glWindowPos2d");
		GL14.glWindowPos2d(x, y);
	}

	public static void qglWindowPos2d(int x, int y) {
		QGL.DEBUG_printName("glWindowPos2d");
		GL14.glWindowPos2d(x, y);
	}

	public static void qglWindowPos2dARB(int x, int y) {
		QGL.DEBUG_printName("glWindowPos2dARB");
		ARBWindowPos.glWindowPos2dARB(x, y);
	}

	public static void qglWindowPos2dvARB(DoubleBuffer p) {
		QGL.DEBUG_printName("glWindowPos2dvARB");
		ARBWindowPos.glWindowPos2dvARB(p);
	}

	public static void qglWindowPos2f(float x, float y) {
		QGL.DEBUG_printName("glWindowPos2f");
		GL14.glWindowPos2f(x, y);
	}

	public static void qglWindowPos2f(int x, int y) {
		QGL.DEBUG_printName("glWindowPos2f");
		GL14.glWindowPos2f(x, y);
	}

	public static void qglWindowPos2fARB(int x, int y) {
		QGL.DEBUG_printName("glWindowPos2fARB");
		ARBWindowPos.glWindowPos2fARB(x, y);
	}

	public static void qglWindowPos2fvARB(FloatBuffer p) {
		QGL.DEBUG_printName("glWindowPos2fvARB");
		ARBWindowPos.glWindowPos2fvARB(p);
	}

	public static void qglWindowPos2i(int x, int y) {
		QGL.DEBUG_printName("glWindowPos2i");
		GL14.glWindowPos2i(x, y);
	}

	public static void qglWindowPos2iARB(int x, int y) {
		QGL.DEBUG_printName("glWindowPos2iARB");
		ARBWindowPos.glWindowPos2iARB(x, y);
	}

	public static void qglWindowPos2iv(IntBuffer p) {
		QGL.DEBUG_printName("glWindowPos2iv");
		GL14.glWindowPos2iv(p);
	}

	public static void qglWindowPos2ivARB(IntBuffer p) {
		QGL.DEBUG_printName("glWindowPos2ivARB");
		ARBWindowPos.glWindowPos2ivARB(p);
	}

	public static void qglWindowPos2s(short x, short y) {
		QGL.DEBUG_printName("glWindowPos2s");
		GL14.glWindowPos2s(x, y);
	}

	public static void qglWindowPos2sARB(short x, short y) {
		QGL.DEBUG_printName("glWindowPos2sARB");
		ARBWindowPos.glWindowPos2sARB(x, y);
	}

	public static void qglWindowPos2sv(ShortBuffer p) {
		QGL.DEBUG_printName("glWindowPos2sv");
		GL14.glWindowPos2sv(p);
	}

	public static void qglWindowPos2svARB(ShortBuffer p) {
		QGL.DEBUG_printName("glWindowPos2svARB");
		ARBWindowPos.glWindowPos2svARB(p);
	}

	public static void qglWindowPos3d(int x, int y, int z) {
		QGL.DEBUG_printName("glWindowPos3d");
		GL14.glWindowPos3d(x, y, z);
	}

	public static void qglWindowPos3dARB(int x, int y, int z) {
		QGL.DEBUG_printName("glWindowPos3dARB");
		ARBWindowPos.glWindowPos3dARB(x, y, z);
	}

	public static void qglWindowPos3dv(DoubleBuffer p) {
		QGL.DEBUG_printName("glWindowPos3dv");
		GL14.glWindowPos3dv(p);
	}

	public static void qglWindowPos3dvARB(DoubleBuffer p) {
		QGL.DEBUG_printName("glWindowPos3dvARB");
		ARBWindowPos.glWindowPos3dvARB(p);
	}

	public static void qglWindowPos3f(int x, int y, int z) {
		QGL.DEBUG_printName("glWindowPos3f");
		GL14.glWindowPos3f(x, y, z);
	}

	public static void qglWindowPos3fARB(int x, int y, int z) {
		QGL.DEBUG_printName("glWindowPos3fARB");
		ARBWindowPos.glWindowPos3fARB(x, y, z);
	}

	public static void qglWindowPos3fv(FloatBuffer p) {
		QGL.DEBUG_printName("glWindowPos3fv");
		GL14.glWindowPos3fv(p);
	}

	public static void qglWindowPos3fvARB(FloatBuffer p) {
		QGL.DEBUG_printName("glWindowPos3fvARB");
		ARBWindowPos.glWindowPos3fvARB(p);
	}

	public static void qglWindowPos3i(int x, int y, int z) {
		QGL.DEBUG_printName("glWindowPos3i");
		GL14.glWindowPos3i(x, y, z);
	}

	public static void qglWindowPos3iARB(int x, int y, int z) {
		QGL.DEBUG_printName("glWindowPos3iARB");
		ARBWindowPos.glWindowPos3iARB(x, y, z);
	}

	public static void qglWindowPos3iv(IntBuffer p) {
		QGL.DEBUG_printName("glWindowPos3iv");
		GL14.glWindowPos3iv(p);
	}

	public static void qglWindowPos3ivARB(IntBuffer p) {
		QGL.DEBUG_printName("glWindowPos3ivARB");
		ARBWindowPos.glWindowPos3ivARB(p);
	}

	public static void qglWindowPos3s(short x, short y, short z) {
		QGL.DEBUG_printName("glWindowPos3s");
		GL14.glWindowPos3s(x, y, z);
	}

	public static void qglWindowPos3sARB(short x, short y, short z) {
		QGL.DEBUG_printName("glWindowPos3sARB");
		ARBWindowPos.glWindowPos3sARB(x, y, z);
	}

	public static void qglWindowPos3sv(ShortBuffer p) {
		QGL.DEBUG_printName("glWindowPos3sv");
		GL14.glWindowPos3sv(p);
	}

	public static void qglWindowPos3svARB(ShortBuffer p) {
		QGL.DEBUG_printName("glWindowPos3svARB");
		ARBWindowPos.glWindowPos3svARB(p);
	}

	/*
	 * GL-Stuff found in Cpp-Source, but not in LWJGL, not yet
	 */
	static void ztodo() {
		// TODO GLGETPROGRAMNAMEDPARAMETERDVNV
		// TODO GLGETPROGRAMNAMEDPARAMETERFVNV
		// TODO GLGETVERTEXATTRIBARRAYOBJECTFVATI
		// TODO GLGETVERTEXATTRIBARRAYOBJECTIVATI
		// TODO GLMAPOBJECTBUFFERATI
		// TODO GLPROGRAMNAMEDPARAMETER4DNV
		// TODO GLPROGRAMNAMEDPARAMETER4DVNV
		// TODO GLPROGRAMNAMEDPARAMETER4FNV
		// TODO GLPROGRAMNAMEDPARAMETER4FVNV
		// TODO GLUNMAPOBJECTBUFFERATI
		// TODO GLVERTEXATTRIBARRAYOBJECTATI
	}

}
