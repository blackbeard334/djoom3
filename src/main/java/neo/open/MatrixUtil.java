package neo.open;

import java.nio.FloatBuffer;

public class MatrixUtil {

	public static void emitFullScreenModelView(float[] modelViewMatrix) {
        modelViewMatrix[ 0] = 1.0f;
        modelViewMatrix[ 5] = 1.0f;
        modelViewMatrix[10] = 1.0f;
        modelViewMatrix[15] = 1.0f;
	}

	public static void emitFullScreenModelView(FloatBuffer modelViewMatrix) {
        modelViewMatrix.clear();

        modelViewMatrix.put( 0, 1.0f);
        modelViewMatrix.put( 5, 1.0f);
        modelViewMatrix.put(10, 1.0f);
        modelViewMatrix.put(15, 1.0f);

        modelViewMatrix.position(modelViewMatrix.capacity());
        modelViewMatrix.flip();
        modelViewMatrix.rewind();
	}

	public static void emitFullScreenProjection(float[] projectionMatrix) {
        projectionMatrix[ 0] = +2.0f / 640.0f;
        projectionMatrix[ 5] = -2.0f / 480.0f;
        projectionMatrix[10] = -2.0f / 1.0f;
        projectionMatrix[12] = -1.0f;
        projectionMatrix[13] = +1.0f;
        projectionMatrix[14] = -1.0f;
        projectionMatrix[15] = +1.0f;
	}

	public static void emitFullScreenProjection(FloatBuffer projectionMatrix) {
        projectionMatrix.clear();

        projectionMatrix.put( 0, +2.0f / 640.0f);
        projectionMatrix.put( 5, -2.0f / 480.0f);
        projectionMatrix.put(10, -2.0f / 1.0f);
        projectionMatrix.put(12, -1.0f);
        projectionMatrix.put(13, +1.0f);
        projectionMatrix.put(14, -1.0f);
        projectionMatrix.put(15, +1.0f);

        projectionMatrix.position(projectionMatrix.capacity());
        projectionMatrix.flip();
        projectionMatrix.rewind();
	}

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

	public static void setupProjection(float[] projectionMatrix, float zNear, float width, float xmax, float xmin,
			float height, float ymax, float ymin) {
        projectionMatrix[ 0] = (2 * zNear) / width;
        projectionMatrix[ 4] = 0;
        projectionMatrix[ 8] = (xmax + xmin) / width;	// normally 0
        projectionMatrix[12] = 0;

        projectionMatrix[ 1] = 0;
        projectionMatrix[ 5] = (2 * zNear) / height;
        projectionMatrix[ 9] = (ymax + ymin) / height;	// normally 0
        projectionMatrix[13] = 0;

        // this is the far-plane-at-infinity formulation, and
        // crunches the Z range slightly so w=0 vertexes do not
        // rasterize right at the wraparound point
        projectionMatrix[ 2] = 0;
        projectionMatrix[ 6] = 0;
        projectionMatrix[10] = -0.999f;
        projectionMatrix[14] = -2.0f * zNear;

        projectionMatrix[ 3] = 0;
        projectionMatrix[ 7] = 0;
        projectionMatrix[11] = -1;
        projectionMatrix[15] = 0;
	}

	public static void setupProjection(FloatBuffer projectionMatrix, float zNear, float width, float xmax, float xmin,
			float height, float ymax, float ymin) {
        projectionMatrix.clear();

        projectionMatrix.put( 0, (2 * zNear) / width);
        projectionMatrix.put( 4, 0);
        projectionMatrix.put( 8, (xmax + xmin) / width);	// normally 0
        projectionMatrix.put(12, 0);

        projectionMatrix.put( 1, 0);
        projectionMatrix.put( 5, (2 * zNear) / height);
        projectionMatrix.put( 9, (ymax + ymin) / height);	// normally 0
        projectionMatrix.put(13, 0);

        // this is the far-plane-at-infinity formulation, and
        // crunches the Z range slightly so w=0 vertexes do not
        // rasterize right at the wraparound point
        projectionMatrix.put( 2, 0);
        projectionMatrix.put( 6, 0);
        projectionMatrix.put(10, -0.999f);
        projectionMatrix.put(14, -2.0f * zNear);

        projectionMatrix.put( 3, 0);
        projectionMatrix.put( 7, 0);
        projectionMatrix.put(11, -1);
        projectionMatrix.put(15, 0);
        
        projectionMatrix.position(projectionMatrix.capacity());
        projectionMatrix.flip();
        projectionMatrix.rewind();
	}

}
