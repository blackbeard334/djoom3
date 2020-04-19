package neo.open;

import java.nio.ByteBuffer;

public class ColorUtil {

	public static void setElements(ByteBuffer color, byte red, byte green, byte blue, byte alpha) {
		color.put(0, red);
		color.put(1, green);
		color.put(2, blue);
		color.put(3, alpha);
	}

	public static void setElements(ByteBuffer color, byte value) {
		for (int i = 0; i < 4; i++) {
			color.put(i, value);
		}
	}

	public static void setElements(ByteBuffer color, byte[] values) {
		for (int i = 0; i < 4; i++) {
			color.put(i, values[i]);
		}
	}

	public static void setElements(ByteBuffer src, ByteBuffer dest) {
		for (int i = 0; i < src.capacity(); i++) {
			dest.put(src.get(i));
		}
	}

}
