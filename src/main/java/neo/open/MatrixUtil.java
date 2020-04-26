package neo.open;

import static neo.open.gl.QGL.qglLoadMatrixf;
import static neo.open.gl.QGL.qglMatrixMode;
import static neo.open.gl.QGLConstantsIfc.GL_MODELVIEW;
import static neo.open.gl.QGLConstantsIfc.GL_PROJECTION;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE;

import java.nio.FloatBuffer;

public class MatrixUtil {

	public static void emitFullScreenModelView(FloatBuffer modelViewMatrix) {
        modelViewMatrix.put( 0, 1.0f);
        modelViewMatrix.put( 5, 1.0f);
        modelViewMatrix.put(10, 1.0f);
        modelViewMatrix.put(15, 1.0f);
	}

	public static void emitFullScreenProjection(FloatBuffer projectionMatrix) {
        projectionMatrix.put( 0, +2.0f / 640.0f);
        projectionMatrix.put( 5, -2.0f / 480.0f);
        projectionMatrix.put(10, -2.0f / 1.0f);
        projectionMatrix.put(12, -1.0f);
        projectionMatrix.put(13, +1.0f);
        projectionMatrix.put(14, -1.0f);
        projectionMatrix.put(15, +1.0f);
	}

	public static void enterModelDepthHack(FloatBuffer projectionMatrix, float depth) {
		float f = projectionMatrix.get(14);
		projectionMatrix.put(14, f - depth);

        qglMatrixMode(GL_PROJECTION);
        qglLoadMatrixf(projectionMatrix);
        qglMatrixMode(GL_MODELVIEW);

		projectionMatrix.put(14, f);
	}

	public static void loadModelViewMatrix(FloatBuffer modelViewMatrix) {
		//qglMatrixMode(GL_MODELVIEW);
        qglLoadMatrixf(modelViewMatrix);
        //qglMatrixMode(GL_MODELVIEW);
	}

	public static void loadProjectionMatrix(FloatBuffer projectionMatrix) {

		qglMatrixMode(GL_PROJECTION);
        qglLoadMatrixf(projectionMatrix);
        qglMatrixMode(GL_MODELVIEW);
	}

	public static void loadTextureMatrix(FloatBuffer textureMatrix) {
		qglMatrixMode(GL_TEXTURE);
        qglLoadMatrixf(textureMatrix);
        qglMatrixMode(GL_MODELVIEW);
	}

	public static void matrixToClipGet3Set4(final FloatOGetSet get, final FloatOGetSet set,
			final FloatOGetSet view, final FloatBuffer modelViewMatrix, final FloatBuffer projectionMatrix) {
		MatrixUtil.multiplyGet3(view, get, modelViewMatrix);

		MatrixUtil.multiplyGet4(set, view, projectionMatrix);
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
			set.oSet(i, (get.oGet(j) * matrix.get((j * 4) + i))
					// increment j
					+ (get.oGet(++j) * matrix.get((j * 4) + i))
					// increment j
					+ (get.oGet(++j) * matrix.get((j * 4) + i))
					// increment j
					+ (matrix.get(i + (++j * 4))));
		}
	}

	private static void multiplyGet4(final FloatOGetSet set, final FloatOGetSet get, final FloatBuffer matrix) {
		int j;
		for (int i = 0; i < 4; i++) {
			j = 0;
			set.oSet(i, (get.oGet(j) * matrix.get((j * 4) + i))
					// increment j
					+ (get.oGet(++j) * matrix.get((j * 4) + i))
					// increment j
					+ (get.oGet(++j) * matrix.get((j * 4) + i))
					// increment j
					+ (get.oGet(++j) * matrix.get((j * 4) + i)));
		}
	}

	public static void setupProjection(FloatBuffer projectionMatrix, float zNear, float width, float xmax, float xmin,
			float height, float ymax, float ymin) {
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
	}

}
