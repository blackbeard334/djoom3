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
		this.byteBuffer = Nio.newByteBuffer(indexes.length * 4);
		this.intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(indexes);
	}

	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}

	@Deprecated
	public int[] getIntArray() {
		return intBuffer.array();
	}

	public IntBuffer getIntBuffer() {
		return intBuffer;
	}
}
