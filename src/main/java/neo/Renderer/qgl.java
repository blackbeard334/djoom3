package neo.Renderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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

import neo.open.Nio;

/**
 * so yeah, it's easier to use this class as an interface. rather than refactor
 * all the qwgl stuff to gl and such.
 */
public class qgl {

	public final static int GL_ADD = 0x104;

	public final static int GL_ALL_ATTRIB_BITS = 0xFFFFF;

	public final static int GL_ALPHA = 0x1906;

	public final static int GL_ALPHA_SCALE = 0xD1C;

	public final static int GL_ALPHA_TEST = 0x0BC0;

	public final static int GL_ALPHA8 = 0x803C;

	public final static int GL_ALWAYS = 0x0207;

	public final static int GL_ARRAY_BUFFER_ARB = 0x8892;

	public final static int GL_BACK = 0x0405;

	public final static int GL_BGR = 0x80E0;

	public final static int GL_BGR_EXT = 0x80E0;

	public final static int GL_BGRA = 0x80E1;

	public final static int GL_BGRA_EXT = 0x80E1;

	public final static int GL_BLEND = 0x0BE2;

	public final static int GL_BLUE = 0x1905;

	public final static int GL_BYTE = 0x1400;

	public final static int GL_CLAMP = 0x2900;

	public final static int GL_CLAMP_TO_BORDER = 0x812D;

	public final static int GL_CLAMP_TO_EDGE = 0x812F;

	public final static int GL_COLOR_ARRAY = 0x8076;

	public final static int GL_COLOR_BUFFER_BIT = 0x00004000;

	public final static int GL_COLOR_INDEX = 0x1900;

	public final static int GL_COMBINE = 0x8570;

	public final static int GL_COMBINE_ALPHA_ARB = 0x8572;

	public final static int GL_COMBINE_ARB = 0x8570;

	public final static int GL_COMBINE_RGB_ARB = 0x8571;

	public final static int GL_COMPRESSED_RGB_ARB = 0x84ED;

	public final static int GL_COMPRESSED_RGB_S3TC_DXT1_EXT = 0x83F0;

	public final static int GL_COMPRESSED_RGBA_ARB = 0x84EE;

	public final static int GL_COMPRESSED_RGBA_S3TC_DXT1_EXT = 0x83F1;

	public final static int GL_COMPRESSED_RGBA_S3TC_DXT3_EXT = 0x83F2;

	public final static int GL_COMPRESSED_RGBA_S3TC_DXT5_EXT = 0x83F3;

	public final static int GL_CONSTANT_ARB = 0x8576;

	public final static int GL_CULL_FACE = 0x0B44;

	public final static int GL_DEBUG_OUTPUT = 0x92E0;

	public final static int GL_DECAL = 0x2101;

	public final static int GL_DECR = 0x1E03;

	public final static int GL_DECR_WRAP_EXT = 0x8508;

	public final static int GL_DEPTH_BOUNDS_TEST_EXT = 0x8890;

	public final static int GL_DEPTH_BUFFER_BIT = 0x00000100;

	public final static int GL_DEPTH_COMPONENT = 0x1902;

	public final static int GL_DEPTH_TEST = 0x0B71;

	public final static int GL_DISTANCE_ATTENUATION_EXT = 0x8129;

	public final static int GL_DONT_CARE = 0x1100;

	public final static int GL_DOT3_RGB_ARB = 0x86AE;

	public final static int GL_DOT3_RGBA_ARB = 0x86AF;

	public final static int GL_DST_ALPHA = 0x0304;

	public final static int GL_DST_COLOR = 0x306;

	public final static int GL_ELEMENT_ARRAY_BUFFER_ARB = 0x8893;

	public final static int GL_EQUAL = 0x0202;

	public final static int GL_EXTENSIONS = 0x1F03;

	public final static int GL_EYE_PLANE = 0x2502;

	public final static int GL_FALSE = 0;

	public final static int GL_FASTEST = 0x1101;

	public final static int GL_FILL = 0x1B02;

	public final static int GL_FLAT = 0x1D00;

	public final static int GL_FLOAT = 0x1406;

	public final static int GL_FRAGMENT_PROGRAM_ARB = 0x8804;

	public final static int GL_FRONT = 0x0404;

	public final static int GL_FRONT_AND_BACK = 0x0408;

	public final static int GL_GEQUAL = 0x0206;

	public final static int GL_GREATER = 0x0204;

	public final static int GL_GREEN = 0x1904;

	public final static int GL_INCR = 0x1E02;

	public final static int GL_INCR_WRAP_EXT = 0x8507;

	public final static int GL_INT = 0x1404;

	public final static int GL_INTENSITY8 = 0x804B;

	public final static int GL_INVALID_ENUM = 0x500;

	public final static int GL_INVALID_OPERATION = 0x502;

	public final static int GL_INVALID_VALUE = 0x501;

	public final static int GL_KEEP = 0x1E00;

	public final static int GL_LEQUAL = 0x0203;

	public final static int GL_LESS = 0x0201;

	public final static int GL_LIGHTING = 0xB50;

	public final static int GL_LINE = 0x1B01;

	public final static int GL_LINE_LOOP = 0x0002;

	public final static int GL_LINE_STIPPLE = 0xB24;

	public final static int GL_LINE_STRIP = 0x0003;

	public final static int GL_LINEAR = 0x2601;

	public final static int GL_LINEAR_MIPMAP_LINEAR = 0x2703;

	public final static int GL_LINEAR_MIPMAP_NEAREST = 0x2701;

	public final static int GL_LINES = 0x0001;

	public final static int GL_LUMINANCE = 0x1909;

	public final static int GL_LUMINANCE_ALPHA = 0x190A;

	public final static int GL_LUMINANCE8 = 0x8040;

	public final static int GL_LUMINANCE8_ALPHA8 = 0x8045;

	public final static int GL_MAX_TEXTURE_COORDS_ARB = 0x8871;

	public final static int GL_MAX_TEXTURE_IMAGE_UNITS_ARB = 0x8872;

	public final static int GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF;

	public final static int GL_MAX_TEXTURE_SIZE = 0xD33;

	public final static int GL_MAX_TEXTURE_UNITS_ARB = 0x84E2;

	public final static int GL_MODELVIEW = 0x1700;

	public final static int GL_MODELVIEW_MATRIX = 0x0BA6;

	public final static int GL_MODULATE = 0x2100;

	public final static int GL_NEAREST = 0x2600;

	public final static int GL_NEAREST_MIPMAP_LINEAR = 0x2702;

	public final static int GL_NEAREST_MIPMAP_NEAREST = 0x2700;

	public final static int GL_NEVER = 0x0200;

	public final static int GL_NICEST = 0x1102;

	public final static int GL_NO_ERROR = 0;

	public final static int GL_NORMAL_ARRAY = 0x8075;

	public final static int GL_NORMAL_MAP = 0x8511;

	public final static int GL_NOTEQUAL = 0x0205;

	public final static int GL_OBJECT_LINEAR = 0x2401;

	public final static int GL_OBJECT_PLANE = 0x2501;

	public final static int GL_ONE = 1;

	public final static int GL_ONE_MINUS_DST_ALPHA = 0x0305;

	public final static int GL_ONE_MINUS_DST_COLOR = 0x307;

	public final static int GL_ONE_MINUS_SRC_ALPHA = 0x0303;

	public final static int GL_ONE_MINUS_SRC_COLOR = 0x0301;

	public final static int GL_OPERAND0_ALPHA_ARB = 0x8598;

	public final static int GL_OPERAND0_RGB_ARB = 0x8590;

	public final static int GL_OPERAND1_ALPHA_ARB = 0x8599;

	public final static int GL_OPERAND1_RGB_ARB = 0x8591;

	public final static int GL_OPERAND2_ALPHA_ARB = 0x859A;

	public final static int GL_OPERAND2_RGB_ARB = 0x8592;

	public final static int GL_OUT_OF_MEMORY = 0x505;

	public final static int GL_PACK_ALIGNMENT = 0x0D05;

	public final static int GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50;

	public final static int GL_POINT = 0x1B00;

	public final static int GL_POINT_FADE_THRESHOLD_SIZE_EXT = 0x8128;

	public final static int GL_POINT_SIZE_MAX_EXT = 0x8127;

	public final static int GL_POINT_SIZE_MIN_EXT = 0x8126;

	public final static int GL_POINT_SMOOTH = 0x0B10;

	public final static int GL_POINTS = 0x0000;

	public final static int GL_POLYGON = 0x0009;

	public final static int GL_POLYGON_OFFSET_FILL = 0x8037;

	public final static int GL_POLYGON_OFFSET_LINE = 0x2A02;

	public final static int GL_PREVIOUS_ARB = 0x8578;

	public final static int GL_PRIMARY_COLOR_ARB = 0x8577;

	public final static int GL_PROGRAM_ERROR_POSITION_ARB = 0x864B;

	public final static int GL_PROGRAM_ERROR_STRING_ARB = 0x8874;

	public final static int GL_PROGRAM_FORMAT_ASCII_ARB = 0x8875;

	public final static int GL_PROJECTION = 0x1701;

	public final static int GL_Q = 0x2003;

	public final static int GL_QUAD_STRIP = 0x0008;

	public final static int GL_QUADS = 0x0007;

	public final static int GL_R = 0x2002;

	public final static int GL_R3_G3_B2 = 0x2A10;

	public final static int GL_RED = 0x1903;

	public final static int GL_REFLECTION_MAP = 0x8512;

	public final static int GL_RENDERER = 0x1F01;

	public final static int GL_REPEAT = 0x2901;

	public final static int GL_REPLACE = 0x1E01;

	public final static int GL_RGB = 0x1907;

	public final static int GL_RGB_SCALE_ARB = 0x8573;

	public final static int GL_RGB4 = 0x804F;

	public final static int GL_RGB5 = 0x8050;

	public final static int GL_RGB5_A1 = 0x8057;

	public final static int GL_RGB8 = 0x8051;

	public final static int GL_RGBA = 0x1908;

	public final static int GL_RGBA2 = 0x8055;

	public final static int GL_RGBA4 = 0x8056;

	public final static int GL_RGBA8 = 0x8058;

	public final static int GL_S = 0x2000;

	public final static int GL_SCISSOR_TEST = 0x0C11;

	public final static int GL_SHARED_TEXTURE_PALETTE_EXT = 0x81FB;

	public final static int GL_SHORT = 0x1402;

	public final static int GL_SMOOTH = 0x1D01;

	public final static int GL_SOURCE0_ALPHA_ARB = 0x8588;

	public final static int GL_SOURCE0_RGB_ARB = 0x8580;

	public final static int GL_SOURCE1_ALPHA_ARB = 0x8589;

	public final static int GL_SOURCE1_RGB_ARB = 0x8581;

	public final static int GL_SOURCE2_ALPHA_ARB = 0x858A;

	public final static int GL_SOURCE2_RGB_ARB = 0x8582;

	public final static int GL_SRC_ALPHA = 0x0302;

	public final static int GL_SRC_ALPHA_SATURATE = 0x308;

	public final static int GL_SRC_COLOR = 0x0300;

	public final static int GL_STACK_OVERFLOW = 0x503;

	public final static int GL_STACK_UNDERFLOW = 0x504;

	public final static int GL_STATIC_DRAW_ARB = 0x88E4;

	public final static int GL_STENCIL_BUFFER_BIT = 0x00000400;

	public final static int GL_STENCIL_INDEX = 0x1901;

	public final static int GL_STENCIL_TEST = 0xB90;

	public final static int GL_STREAM_DRAW_ARB = 0x88E0;

	public final static int GL_T = 0x2001;

	public final static int GL_T2F_V3F = 0x2A27;

	public final static int GL_TEXTURE = 0x1702;

	public final static int GL_TEXTURE_2D = 0x0DE1;

	public final static int GL_TEXTURE_3D = 0x806F;

	public final static int GL_TEXTURE_BORDER_COLOR = 0x1004;

	public final static int GL_TEXTURE_COORD_ARRAY = 0x8078;

	public final static int GL_TEXTURE_CUBE_MAP = 0x8513;

	public final static int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515;

	public final static int GL_TEXTURE_ENV = 0x2300;

	public final static int GL_TEXTURE_ENV_COLOR = 0x2201;

	public final static int GL_TEXTURE_ENV_MODE = 0x2200;

	public final static int GL_TEXTURE_GEN_MODE = 0x2500;

	public final static int GL_TEXTURE_GEN_Q = 0xC63;

	public final static int GL_TEXTURE_GEN_R = 0xC62;

	public final static int GL_TEXTURE_GEN_S = 0xC60;

	public final static int GL_TEXTURE_GEN_T = 0xC61;

	public final static int GL_TEXTURE_LOD_BIAS = 0x8501;

	public final static int GL_TEXTURE_MAG_FILTER = 0x2800;

	public final static int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE;

	public final static int GL_TEXTURE_MIN_FILTER = 0x2801;

	public final static int GL_TEXTURE_RECTANGLE = 0x84F5;

	public final static int GL_TEXTURE_WRAP_R = 0x8072;

	public final static int GL_TEXTURE_WRAP_S = 0x2802;

	public final static int GL_TEXTURE_WRAP_T = 0x2803;

	public final static int GL_TEXTURE0 = 0x84C0;

	public final static int GL_TEXTURE0_ARB = 0x84C0;

	public final static int GL_TEXTURE1 = 0x84C1;

	public final static int GL_TEXTURE1_ARB = 0x84C1;

	public final static int GL_TRIANGLE_FAN = 0x0006;

	public final static int GL_TRIANGLE_STRIP = 0x0005;

	public final static int GL_TRIANGLES = 0x0004;

	public final static int GL_TRUE = 1;

	public final static int GL_UNSIGNED_BYTE = 0x1401;

	public final static int GL_UNSIGNED_INT = 0x1405;

	public final static int GL_UNSIGNED_SHORT = 0x1403;

	public final static int GL_VENDOR = 0x1F00;

	public final static int GL_VERSION = 0x1F02;

	public final static int GL_VERTEX_ARRAY = 0x8074;

	public final static int GL_VERTEX_PROGRAM_ARB = 0x8620;

	public final static int GL_ZERO = 0;

	private final static boolean GL_DEBUG = false;

	public static final boolean qGL_FALSE = false;

	public static final boolean qGL_TRUE = true;

	{
		if (GL_DEBUG) {
			qglEnable(GL_DEBUG_OUTPUT);
		}
	}

	public static void checkGLError() {
		if (GL_DEBUG) {
			final ByteBuffer messageLog = Nio.newByteBuffer(1000);
//            while (GL43.glGetDebugMessageLog(1, null, null, null, null, null, messageLog) > 0) {
//                System.out.println(TempDump.bbtoa(messageLog));
//                messageLog.clear();
//            }
//            Util.checkGLError();
		}
	}

	static void DEBUG_printName(final String functionName) {
		if (GL_DEBUG) {
//            System.out.println(functionName);
		}
	}

	public static void qcreateCapabilities() {
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
//        System.out.printf("qglBindTexture(%d, %d)\n", target, texture);
		GL11.glBindTexture(target, texture);
	}

	public static void qglBlendFunc(int sFactor, int dFactor) {
		DEBUG_printName("glBlendFunc");
//        System.out.printf("--%d, %d\n", sFactor, dFactor);
		GL11.glBlendFunc(sFactor, dFactor);
	}

	public static void qglBufferDataARB(int target, int size, ByteBuffer data, int usage) {
		DEBUG_printName("glBufferDataARB");
//        GL15.glBufferData(target, data, usage);//TODO:!!!!!!!!!!!!!!!!!!!!!!!!!
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

	public static void qglColor3fv(FloatBuffer v) {
		DEBUG_printName("glColor3fv");
		GL11.glColor3fv(v);
	}

	public static void qglColor3ubv(ByteBuffer v) {
		DEBUG_printName("glColor3ubv");
		GL11.glColor3ubv(v);
	}

	public static void qglColor4f(float red, float green, float blue, float alpha) {
		DEBUG_printName("glColor4f");
		GL11.glColor4f(red, green, blue, alpha);
	}

	public static void qglColor4fv(FloatBuffer v) {
		DEBUG_printName("glColor4fv");
		GL11.glColor4fv(v);
	}

	public static void qglColor4ubv(ByteBuffer v) {
		DEBUG_printName("glColor4ubv");
		GL11.glColor4ubv(v);
	}

	public static void qglColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		DEBUG_printName("glColorMask");
		GL11.glColorMask(red, green, blue, alpha);
	}

	public static void qglColorMask(int red, int green, int blue, int alpha) {
		qglColorMask(red != 0, green != 0, blue != 0, alpha != 0);
	}

	public static void qglColorPointer(int size, int type, int stride, ByteBuffer pointer) {
		DEBUG_printName("glColorPointer");
		GL11.glColorPointer(size, type, stride, pointer);
	}

	public static void qglColorPointer(int size, int type, int stride, long pointer) {
		DEBUG_printName("glColorPointer");
		GL11.glColorPointer(size, type, stride, pointer);
	}

	public static void qglColorTableEXT(int target, int internalformat, int width, int format, int type,
			ByteBuffer table) {
		DEBUG_printName("glColorTableEXT");
		ARBImaging.glColorTable(target, internalformat, width, format, type, table);
	}

	public static void /* PFNGLCOMPRESSEDTEXIMAGE2DARBPROC */ qglCompressedTexImage2DARB(int target, int level,
			int internalformat, int width, int height, int border, int imageSize, final ByteBuffer data) {
		DEBUG_printName("glCompressedTexImage2DARB");
//        ARBTextureCompression.glCompressedTexImage2DARB(target, level, internalformat, width, height, border, data);
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

	public static void qglDrawElements(int mode, int count, int type, IntBuffer indices) {
		DEBUG_printName("glDrawElements2");
		GL11.glDrawElements(mode, indices);
	}

	public static void qglDrawPixels(int width, int height, int format, int type, byte[][][] pixels) {
		DEBUG_printName("glDrawPixels");
		//GL11.glDrawPixels(width, height, format, type, (ByteBuffer) (Object) pixels);
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static void qglDrawPixels(int width, int height, int format, int type, ByteBuffer pixels) {
		DEBUG_printName("glDrawPixels");
		GL11.glDrawPixels(width, height, format, type, pixels);
	}

	public static void qglEnable(int cap) {
		DEBUG_printName("glEnable");
//        System.out.println("--"+cap);
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

	public static int qglGetError() {// DEBUG_printName("glGetError");
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

	public static void qglLoadMatrixf(FloatBuffer m) {
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

	public static void qglPrioritizeTextures(IntBuffer textures, FloatBuffer priorities) {
		DEBUG_printName("glPrioritizeTextures");
		GL11.glPrioritizeTextures(textures, priorities);
	}

	public static /* PFNGLPROGRAMENVPARAMETER4FVARBPROC */ void qglProgramEnvParameter4fvARB(int target, Enum<?> index,
			final FloatBuffer params) {
		DEBUG_printName("glProgramEnvParameter4fvARB");
		ARBVertexProgram.glProgramEnvParameter4fvARB(target, index.ordinal(), params);
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

	public static void qglTexCoord2fv(FloatBuffer v) {
		DEBUG_printName("glTexCoord2fv");
		GL11.glTexCoord2fv(v);
	}

	public static void qglTexCoordPointer(int size, int type, int stride, ByteBuffer pointer) {
		DEBUG_printName("glTexCoordPointer");
		GL11.glTexCoordPointer(size, type, stride, pointer);
	}

	public static void qglTexCoordPointer(int size, int type, int stride, FloatBuffer pointer) {
		DEBUG_printName("glTexCoordPointer");
		GL11.glTexCoordPointer(size, type, stride, pointer);
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
		DEBUG_printName("glTexEnvi");// ENVY!!
		GL11.glTexEnvi(target, pName, param);
	}

	public static void qglTexGenf(int coord, int pName, float param) {
		DEBUG_printName("glTexGenf");
		GL11.glTexGenf(coord, pName, param);
	}

	public static void qglTexGenfv(int coord, int pname, FloatBuffer params) {
		DEBUG_printName("glTexGenfv");
		GL11.glTexGenfv(coord, pname, params);
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

	public static void qglVertex3fv(FloatBuffer coords) {
		DEBUG_printName("glVertex3fv");
		GL11.glVertex3fv(coords);
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

	public static void qglVertexPointer(int size, int type, int stride, FloatBuffer pointer) {
		DEBUG_printName("qglVertexPointer");
		GL11.glVertexPointer(size, type, stride, pointer);
	}

	public static void qglVertexPointer(int size, int type, int stride, long pointer) {
		DEBUG_printName("glVertexPointer");
		GL11.glVertexPointer(size, type, stride, pointer);
	}

	public static void qglViewport(int x, int y, int width, int height) {
		DEBUG_printName("glViewport");
		GL11.glViewport(x, y, width, height);
	}
}
