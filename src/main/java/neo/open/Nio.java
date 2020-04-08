package neo.open;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Nio {

	public static final int SIZEOF_FLOAT = 4;
	public static final int SIZEOF_INT = 4;

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

	/**
	 * @deprecated the calling functions should send ByteBuffers instead.
	 */
	@Deprecated
	public static ByteBuffer wrap(final boolean[] booleanArray) {
		byte[] byteArray = new byte[booleanArray.length];
		for (int i = 0; i < byteArray.length; i++) {
			byteArray[i] = booleanArray[i] ? (byte) 1 : (byte) 0;
		}
		return (ByteBuffer) newByteBuffer(byteArray.length).put(byteArray).flip();
	}

	/**
	 * @deprecated the calling functions should send ByteBuffers instead.
	 */
	@Deprecated
	public static ByteBuffer wrap(final byte[] byteArray) {

		return (ByteBuffer) newByteBuffer(byteArray.length).put(byteArray).flip();
	}

	/**
	 * @deprecated the calling functions should send FloatBuffers instead.
	 */
	@Deprecated
	public static FloatBuffer wrap(final float[] floatArray) {

		return (FloatBuffer) newFloatBuffer(floatArray.length).put(floatArray).flip();
	}

	/**
	 * @deprecated the calling functions should send FloatBuffers instead.
	 */
	@Deprecated
	public static FloatBuffer wrap(final float[] floatArray, int length) {

		if (floatArray.length == length) {
			return wrap(floatArray);
		} else {
			FloatBuffer buffer = newFloatBuffer(length);
			int len = length;
			if (length > floatArray.length) {
				len = floatArray.length;
			}
			for (int i = 0; i < len; i++) {
				buffer.put(floatArray[i]);
			}
			return (FloatBuffer) buffer.flip();
		}
	}

	/**
	 * @deprecated the calling functions should send IntBuffers instead.
	 */
	@Deprecated
	public static IntBuffer wrap(final int[] intArray) {

		return (IntBuffer) newIntBuffer(intArray.length).put(intArray).flip();
	}

}
