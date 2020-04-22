package neo.open;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Util to use openal & opengl and nio Buffers.<br>
 * Reduce dependency on openal & opengl implementation.<br>
 * No need to use {@link org.lwjgl.BufferUtils}
 */
public class Nio {

	public static final int SIZEOF_DOUBLE = 4;
	public static final int SIZEOF_FLOAT = 4;
	public static final int SIZEOF_INT = 4;

	public static void arraycopy(final boolean[] src, int srcPos, final boolean[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(final byte[] src, int srcPos, final byte[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(final byte[] src, int srcPos, final ByteBuffer dest, int destPos, int length) {
		dest.clear();

		for (int i = 0; i < length; i++) {
			// dest.put(src[srcPos + i]);
			dest.put(destPos + i, src[srcPos + i]);
		}
	}

	public static void arraycopy(final char[] src, int srcPos, final byte[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(final char[] src, int srcPos, final char[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(final String src, int srcPos, final char[] dest, int destPos, int length) {
		System.arraycopy(src.toCharArray(), srcPos, dest, destPos, length);
	}

	public static void arraycopy(final float[] src, int srcPos, final float[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(final Float[] src, int srcPos, final Float[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(final FloatBuffer src, int srcPos, final float[] dest, int destPos, int length) {
		for (int i = 0; i < length; i++) {
			dest[destPos + i] = src.get(srcPos + i);
		}
	}

	public static void arraycopy(final int[] src, int srcPos, final int[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(final int[] src, int srcPos, final IntBuffer dest, int destPos, int length) {
		dest.clear();

		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src[srcPos + i]);
		}
	}

	public static void arraycopy(final int[] src, int srcPos, final Integer[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void arraycopy(final long[] src, int srcPos, final long[] dest, int destPos, int length) {
		System.arraycopy(src, srcPos, dest, destPos, length);
	}

	public static void buffercopy(final ByteBuffer src, int srcPos, final ByteBuffer dest, int destPos, int length) {
		dest.clear();

		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src.get(srcPos + i));
		}
	}

	public static void buffercopy(final CharBuffer src, int srcPos, final CharBuffer dest, int destPos, int length) {
		dest.clear();

		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src.get(srcPos + i));
		}
	}

	public static void buffercopy(final DoubleBuffer src, int srcPos, final DoubleBuffer dest, int destPos,
			int length) {
		dest.clear();

		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src.get(srcPos + i));
		}
	}

	public static void buffercopy(final FloatBuffer src, int srcPos, final FloatBuffer dest, int destPos, int length) {
		dest.clear();

		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src.get(srcPos + i));
		}
	}

	public static void buffercopy(final IntBuffer src, int srcPos, final IntBuffer dest, int destPos, int length) {
		dest.clear();

		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src.get(srcPos + i));
		}
	}

	public static void buffercopy(final LongBuffer src, int srcPos, final LongBuffer dest, int destPos, int length) {
		dest.clear();

		for (int i = 0; i < length; i++) {
			dest.put(destPos + i, src.get(srcPos + i));
		}
	}

	public static void buffercopy(final ShortBuffer src, int srcPos, final ShortBuffer dest, int destPos, int length) {
		dest.clear();

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

	/**
	 * @deprecated the calling functions should send FloatBuffers instead.
	 *
	 * @see neo.idlib.math.Vector.idVec6#toFloatBuffer()
	 * @see neo.idlib.math.Vector.idVecX#toFloatBuffer()
	 */
	public static FloatBuffer wrap(final float[] floatArray) {
		return (FloatBuffer) newFloatBuffer(floatArray.length).put(floatArray).flip();
	}

	/**
	 * @deprecated the calling functions should send direct FloatBuffers instead.
	 *
	 *             to change array FloatBuffer in direct FloatBuffer
	 */
	public static FloatBuffer wrap(final FloatBuffer floatBuffer) {
		// return floatBuffer;
		return wrap(floatBuffer.array());
	}
}
