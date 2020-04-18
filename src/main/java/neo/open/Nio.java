package neo.open;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Util to use openal & opengl and nio Buffers.<br>
 * Reduce dependency on openal & opengl implementation.<br>
 * No need to use {@link org.lwjgl.BufferUtils}
 */
public class Nio {

	public static final int SIZEOF_DOUBLE = 4;
	public static final int SIZEOF_FLOAT = 4;
	public static final int SIZEOF_INT = 4;

	public static FloatBuffer arrayCopy(final float[] src, final FloatBuffer dest) {
		for (int i = 0; i < src.length; i++) {
			dest.put(i, src[i]);
		}
		return dest;
	}

	public static float[] arrayCopy(final FloatBuffer src, final float[] dest) {
		for (int i = 0; i < src.limit(); i++) {
			dest[i] = src.get(i);
		}
		return dest;
	}

	public static void arraycopy(final int[] src, int srcPos, final IntBuffer dest, int destPos, int length) {
		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src[srcPos + i]);
		}
	}

	public static void buffercopy(final FloatBuffer src, int srcPos, final FloatBuffer dest, int destPos, int length) {
		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src.get(srcPos + i));
		}
	}

	public static void buffercopy(final IntBuffer src, int srcPos, final IntBuffer dest, int destPos, int length) {
		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src.get(srcPos + i));
		}
	}

	public static ByteBuffer newByteBuffer(int numElements) {
		ByteBuffer bb = ByteBuffer.allocateDirect(numElements);
		bb.order(ByteOrder.nativeOrder());
		return bb;
	}

	public static ByteBuffer newByteBuffer(int numElements, ByteOrder order) {
		ByteBuffer bb = ByteBuffer.allocateDirect(numElements);
		bb.order(order);
		return bb;
	}

	public static DoubleBuffer newDoubleBuffer(int numElements) {
		ByteBuffer bb = newByteBuffer(numElements * SIZEOF_DOUBLE);
		return bb.asDoubleBuffer();
	}

	public static DoubleBuffer newDoubleBuffer(int numElements, ByteOrder order) {
		ByteBuffer bb = newByteBuffer(numElements * SIZEOF_DOUBLE, order);
		return bb.asDoubleBuffer();
	}

	public static FloatBuffer newFloatBuffer(int numElements) {
		ByteBuffer bb = newByteBuffer(numElements * SIZEOF_FLOAT);
		return bb.asFloatBuffer();
	}

	public static FloatBuffer newFloatBuffer(int numElements, ByteOrder order) {
		ByteBuffer bb = newByteBuffer(numElements * SIZEOF_FLOAT, order);
		return bb.asFloatBuffer();
	}

	public static IntBuffer newIntBuffer(int numElements) {
		ByteBuffer bb = newByteBuffer(numElements * SIZEOF_INT);
		return bb.asIntBuffer();
	}

	public static IntBuffer newIntBuffer(int numElements, ByteOrder order) {
		ByteBuffer bb = newByteBuffer(numElements * SIZEOF_INT, order);
		return bb.asIntBuffer();
	}

//	/**
//	 * @deprecated the calling functions should send ByteBuffers instead.
//	 */
//	public static ByteBuffer wrap(final boolean booleanValue) {
//		return (ByteBuffer) newByteBuffer(1).put(booleanValue ? (byte) 1 : (byte) 0).flip();
//	}

//	/**
//	 * @deprecated the calling functions should send ByteBuffers instead.
//	 */
//	public static ByteBuffer wrap(final boolean[] booleanArray) {
//		byte[] byteArray = new byte[booleanArray.length];
//		for (int i = 0; i < byteArray.length; i++) {
//			byteArray[i] = booleanArray[i] ? (byte) 1 : (byte) 0;
//		}
//		return (ByteBuffer) newByteBuffer(byteArray.length).put(byteArray).flip();
//	}

//	/**
//	 * @deprecated the calling functions should send ByteBuffers instead.
//	 */
//	public static ByteBuffer wrap(final byte byteValue) {
//
//		return (ByteBuffer) newByteBuffer(1).put(byteValue).flip();
//	}

//	/**
//	 * @deprecated the calling functions should send ByteBuffers instead.
//	 */
//	public static ByteBuffer wrap(final byte[] byteArray) {
//
//		return (ByteBuffer) newByteBuffer(byteArray.length).put(byteArray).flip();
//	}

//	/**
//	 * @deprecated the calling functions should send DoubleBuffers instead.
//	 */
//	public static DoubleBuffer wrap(final double doubleValue) {
//
//		return (DoubleBuffer) newDoubleBuffer(1).put(doubleValue).flip();
//	}

//	/**
//	 * @deprecated the calling functions should send DoubleBuffers instead.
//	 */
//	public static DoubleBuffer wrap(final double[] doubleArray) {
//
//		return (DoubleBuffer) newDoubleBuffer(doubleArray.length).put(doubleArray).flip();
//	}

//	/**
//	 * @deprecated the calling functions should send FloatBuffers instead.
//	 */
//	public static FloatBuffer wrap(final float floatValue) {
//
//		return (FloatBuffer) newFloatBuffer(1).put(floatValue).flip();
//	}

	/**
	 * @deprecated the calling functions should send FloatBuffers instead.
	 *
	 * @see neo.idlib.math.Vector.idVec6#toFloatBuffer()
	 * @see neo.idlib.math.Vector.idVecX#toFloatBuffer()
	 */
	public static FloatBuffer wrap(final float[] floatArray) {

		return (FloatBuffer) newFloatBuffer(floatArray.length).put(floatArray).flip();
	}

//	/**
//	 * @deprecated the calling functions should send FloatBuffers instead.
//	 */
//	public static FloatBuffer wrap(final float[] floatArray, int length) {
//
//		if (floatArray.length == length) {
//			return wrap(floatArray);
//		} else {
//			FloatBuffer buffer = newFloatBuffer(length);
//			int len = length;
//			if (length > floatArray.length) {
//				len = floatArray.length;
//			}
//			for (int i = 0; i < len; i++) {
//				buffer.put(floatArray[i]);
//			}
//			return (FloatBuffer) buffer.flip();
//		}
//	}

//	/**
//	 * @deprecated the calling functions should send IntBuffers instead.
//	 */
//	public static IntBuffer wrap(final int intValue) {
//
//		return (IntBuffer) newIntBuffer(1).put(intValue).flip();
//	}

//	/**
//	 * @deprecated the calling functions should send IntBuffers instead.
//	 */
//	public static IntBuffer wrap(final int[] intArray) {
//
//		return (IntBuffer) newIntBuffer(intArray.length).put(intArray).flip();
//	}

}
