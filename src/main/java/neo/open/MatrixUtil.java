package neo.open;

import java.nio.FloatBuffer;

public class MatrixUtil {

	public static void matrixToClipGet3Set4(final FloatOGetSet get, final FloatOGetSet set,
			final FloatOGetSet view, final FloatBuffer modelViewMatrix, final float[] projectionMatrix) {
		MatrixUtil.multiplyGet3(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, Nio.wrap(projectionMatrix));
	}

	public static void matrixToClipGet3Set4(final FloatOGetSet get, final FloatOGetSet set,
			final FloatOGetSet view, final FloatBuffer modelViewMatrix, final FloatBuffer projectionMatrix) {
		MatrixUtil.multiplyGet3(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, projectionMatrix);
	}

	public static void matrixToClipGet4Set4(final FloatOGetSet get, final FloatOGetSet set,
			final FloatOGetSet view, final FloatBuffer modelViewMatrix, final float[] projectionMatrix) {
		MatrixUtil.multiplyGet4(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, Nio.wrap(projectionMatrix));
	}

	public static void matrixToClipGet4Set4(final FloatOGetSet get, final FloatOGetSet set,
			final FloatOGetSet view, final FloatBuffer modelViewMatrix, final FloatBuffer projectionMatrix) {
		MatrixUtil.multiplyGet4(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, projectionMatrix);
	}

	private static void multiplyGet3(final FloatOGetSet set, final FloatOGetSet get, final FloatBuffer matrix) {
		int j;
		for (int i = 0; i < 4; i++) {
			j = 0;
			set.oSet(i, (get.oGet(j) * matrix.get(i + (j * 4)))
					// increment j
					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
					// increment j
					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
					// increment j
					+ (matrix.get(i + (++j * 4))));
		}
	}

	private static void multiplyGet4(final FloatOGetSet set, final FloatOGetSet get, final FloatBuffer matrix) {
		int j;
		for (int i = 0; i < 4; i++) {
			j = 0;
			set.oSet(i, (get.oGet(j) * matrix.get(i + (j * 4)))
					// increment j
					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
					// increment j
					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
					// increment j
					+ (get.oGet(++j) * matrix.get(i + (j * 4))));
		}
	}

}
