package neo.open;

import java.nio.ByteBuffer;

import neo.framework.DemoFile.idDemoFile;

public class ColorUtil {

	public static boolean equalsElements0(ByteBuffer color) {
		return ((color.get(0) == 0) && (color.get(1) == 0) && (color.get(2) == 0) && (color.get(3) == 0));
	}

	public static void putElementsTo(ByteBuffer src, ByteBuffer dest) {
		for (int i = 0; i < src.capacity(); i++) {
			dest.put(src.get(i));
		}
	}

	public static void setElementsWith(ByteBuffer color, byte value) {
		for (int i = 0; i < 4; i++) {
			color.put(i, value);
		}
	}

	public static void putElements(ByteBuffer color, byte[] values) {
		for (int i = 0; i < 4; i++) {
			color.put(i, values[i]);
		}
	}

	public static void readFile(ByteBuffer bcolor, idDemoFile f) {
		final char[][] color = new char[4][1];
		for (int i = 0; i < 4; i++) {
		    // TODO check if color[0] should be color[i]
			f.ReadUnsignedChar(color[0]);
		    bcolor.put(i, (byte) color[i][0]);
		}
	}

	public static void writeFile(ByteBuffer color, idDemoFile f) {
		for (int i = 0; i < 4; i++) {
		    f.WriteUnsignedChar((char) color.get(i));
		}
	}

	public static void setColors(ByteBuffer color, byte red, byte green, byte blue, byte alpha) {
        color.put(0, red);
        color.put(1, green);
        color.put(2, blue);
        color.put(3, alpha);
	}

	public static void muliplyElementsWith(ByteBuffer color, float faktor) {
		for (int i = 0; i < 4; i++) {
			color.put(i, (byte) (color.get(i) * faktor));
		}
    }

}
