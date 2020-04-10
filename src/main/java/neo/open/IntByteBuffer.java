package neo.open;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IntByteBuffer {
	private ByteBuffer byteBuffer;
	private IntBuffer intBuffer;

	public void createBuffer(ByteBuffer bb) {
		this.byteBuffer = bb.duplicate();
		this.intBuffer = byteBuffer.asIntBuffer();
	}

	public void createBuffer(int length) {
		this.byteBuffer = Nio.newByteBuffer(length * 4);
		this.intBuffer = byteBuffer.asIntBuffer();
	}

	@Deprecated
	public void createBuffer(int[] indexes) {
		int length = indexes == null ? 0 : indexes.length;
		this.byteBuffer = Nio.newByteBuffer(length * 4);
		this.intBuffer = byteBuffer.asIntBuffer();
		if (length > 0) {
			intBuffer.put(indexes);
		}
	}

	@Deprecated
	public int[] getAsIntArray() {
		if (intBuffer.hasArray()) {
			return intBuffer.array();
		} else {
			return new int[intBuffer.capacity()];
		}

	}

	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	public IntBuffer getIntBuffer() {
		return intBuffer;
	}

	@Deprecated
	public void putFromIntArray(int[] in, int length) {
		for (int i = 0; i < length; i++) {
			intBuffer.put(i, in[i]);
		}
	}

	@Deprecated
	public void putFromIntArray(int[] in, int inIndex, int index, int length) {
		for (int i = 0; i < length; i++) {
			intBuffer.put(index + i, in[inIndex + i]);
		}
	}

	public void putFromIntBuffer(IntBuffer in, int length) {
		for (int i = 0; i < length; i++) {
			intBuffer.put(i, in.get(i));
		}
	}

	public void putFromIntBuffer(IntBuffer in, int inIndex, int index, int length) {
		for (int i = 0; i < length; i++) {
			intBuffer.put(index + i, in.get(inIndex + i));
		}
	}
}
