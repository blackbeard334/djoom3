package neo.open;

import java.nio.FloatBuffer;

public class MatrixUtil {

	public static void matrixToClipGet3Set4(final FloatOGet get, final FloatOSet set,
			final FloatOSet view, final float[] modelViewMatrix, final float[] projectionMatrix) {
		MatrixUtil.multiplyGet3(view, get, Nio.wrap(modelViewMatrix));

		MatrixUtil.multiplyGet4(set, view, Nio.wrap(projectionMatrix));
	}

	public static void matrixToClipGet3Set4(final FloatOGet get, final FloatOSet set,
			final FloatOSet view, final float[] modelViewMatrix, final FloatBuffer projectionMatrix) {
		MatrixUtil.multiplyGet3(view, get, Nio.wrap(modelViewMatrix));

		MatrixUtil.multiplyGet4(set, view, projectionMatrix);
	}

	public static void matrixToClipGet3Set4(final FloatOGet get, final FloatOSet set,
			final FloatOSet view, final FloatBuffer modelViewMatrix, final float[] projectionMatrix) {
		MatrixUtil.multiplyGet3(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, Nio.wrap(projectionMatrix));
	}

	public static void matrixToClipGet3Set4(final FloatOGet get, final FloatOSet set,
			final FloatOSet view, final FloatBuffer modelViewMatrix, final FloatBuffer projectionMatrix) {
		MatrixUtil.multiplyGet3(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, projectionMatrix);
	}

	public static void matrixToClipGet4Set4(final FloatOGet get, final FloatOSet set,
			final FloatOSet view, final float[] modelViewMatrix, final float[] projectionMatrix) {
		MatrixUtil.multiplyGet4(view, get, Nio.wrap(modelViewMatrix));

		MatrixUtil.multiplyGet4(set, view, Nio.wrap(projectionMatrix));
	}

	public static void matrixToClipGet4Set4(final FloatOGet get, final FloatOSet set,
			final FloatOSet view, final float[] modelViewMatrix, final FloatBuffer projectionMatrix) {
		MatrixUtil.multiplyGet4(view, get, Nio.wrap(modelViewMatrix));

		MatrixUtil.multiplyGet4(set, view, projectionMatrix);
	}

	public static void matrixToClipGet4Set4(final FloatOGet get, final FloatOSet set,
			final FloatOSet view, final FloatBuffer modelViewMatrix, final float[] projectionMatrix) {
		MatrixUtil.multiplyGet4(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, Nio.wrap(projectionMatrix));
	}

	public static void matrixToClipGet4Set4(final FloatOGet get, final FloatOSet set,
			final FloatOSet view, final FloatBuffer modelViewMatrix, final FloatBuffer projectionMatrix) {
		MatrixUtil.multiplyGet4(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, projectionMatrix);
	}

	private static void multiplyGet3(final FloatOSet set, final FloatOGet get, final float[] matrix) {
		multiplyGet3(set, get, Nio.wrap(matrix));
	}

	private static void multiplyGet3(final FloatOSet set, final FloatOGet get, final FloatBuffer matrix) {
		int j = 0;
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

	private static void multiplyGet4(final FloatOSet set, final FloatOGet get, final float[] matrix) {
		multiplyGet4(set, get, Nio.wrap(matrix));
	}

	private static void multiplyGet4(final FloatOSet set, final FloatOGet get, final FloatBuffer matrix) {
		int j = 0;
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

//	public static void multiplyIdPlaneGet3(final idPlane set, final idPlane get, final float[] matrix) {
//		int j = 0;
//		for (int i = 0; i < 4; i++) {
//			j = 0;
//			set.oSet(i, (get.oGet(j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)])
//					// increment j
//					+ (matrix[i + (++j * 4)]));
//		}
//	}
//
//	public static void multiplyIdPlaneGet3(final idPlane set, final idPlane get, final FloatBuffer matrix) {
//		int j = 0;
//		for (int i = 0; i < 4; i++) {
//			j = 0;
//			set.oSet(i, (get.oGet(j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (matrix.get(i + (++j * 4))));
//		}
//	}
//
//	public static void multiplyIdPlaneGet4(final idPlane set, final idPlane get, final float[] matrix) {
//		int j = 0;
//		for (int i = 0; i < 4; i++) {
//			j = 0;
//			set.oSet(i, (get.oGet(j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)]));
//		}
//	}
//
//	public static void multiplyIdPlaneGet4(final idPlane set, final idPlane get, final FloatBuffer matrix) {
//		int j = 0;
//		for (int i = 0; i < 4; i++) {
//			j = 0;
//			set.oSet(i, (get.oGet(j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4))));
//		}
//	}
//
//	public static void multiplyIdVecGet3(final idVec<?> set, final idVec<?> get, final float[] matrix) {
//		int j = 0;
//		for (int i = 0; i < 4; i++) {
//			j = 0;
//			set.oSet(i, (get.oGet(j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)])
//					// increment j
//					+ (matrix[i + (++j * 4)]));
//		}
//	}
//
//	public static void multiplyIdVecGet3(final idVec<?> set, final idVec<?> get, final FloatBuffer matrix) {
//		int j = 0;
//		for (int i = 0; i < 4; i++) {
//			j = 0;
//			set.oSet(i, (get.oGet(j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (matrix.get(i + (++j * 4))));
//		}
//	}
//
//	public static void multiplyIdVecGet4(final idVec<?> set, final idVec<?> get, final float[] matrix) {
//		int j = 0;
//		for (int i = 0; i < 4; i++) {
//			j = 0;
//			set.oSet(i, (get.oGet(j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)])
//					// increment j
//					+ (get.oGet(++j) * matrix[i + (j * 4)]));
//		}
//	}
//
//	public static void multiplyIdVecGet4(final idVec<?> set, final idVec<?> get, final FloatBuffer matrix) {
//		int j = 0;
//		for (int i = 0; i < 4; i++) {
//			j = 0;
//			set.oSet(i, (get.oGet(j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4)))
//					// increment j
//					+ (get.oGet(++j) * matrix.get(i + (j * 4))));
//		}
//	}

}
