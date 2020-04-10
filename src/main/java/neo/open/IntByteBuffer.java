package neo.open;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IntByteBuffer {
    private ByteBuffer byteBuffer;
    private IntBuffer intBuffer;
    
	public void createBuffer(int length) {
		this.byteBuffer = Nio.newByteBuffer(length * 4);
		this.intBuffer = byteBuffer.asIntBuffer();
	}
	public ByteBuffer getByteBuffer() {
		return byteBuffer;
	}
	public IntBuffer getIntBuffer() {
		return intBuffer;
	}
}
