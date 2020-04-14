package neo.open;

import java.nio.FloatBuffer;

import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Vector.idVec;

public class MatrixUtil {

    public static void multiplyIdPlaneGet3(final idPlane set, final idPlane get, float[] matrix) {
        int j = 0;
        for (int i = 0; i < 4; i++) {
        	j = 0;
        	set.oSet(i,
                    (get.oGet(j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)])
                    // increment j
                    + (matrix[i + (++j * 4)]));
        }
    }

    public static void multiplyIdPlaneGet3(final idPlane set, final idPlane get, FloatBuffer matrix) {
        int j = 0;
        for (int i = 0; i < 4; i++) {
        	j = 0;
        	set.oSet(i,
                    (get.oGet(j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (matrix.get(i + (++j * 4))));
        }
    }

    public static void multiplyIdPlaneGet4(final idPlane set, final idPlane get, float[] matrix) {
        int j = 0;
        for (int i = 0; i < 4; i++) {
        	j = 0;
        	set.oSet(i,
                    (get.oGet(j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)]));
        }
    }

    public static void multiplyIdPlaneGet4(final idPlane set, final idPlane get, FloatBuffer matrix) {
        int j = 0;
        for (int i = 0; i < 4; i++) {
        	j = 0;
        	set.oSet(i,
                    (get.oGet(j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4))));
        }
    }

    public static void multiplyIdVecGet3(final idVec<?> set, final idVec<?> get, float[] matrix) {
        int j = 0;
        for (int i = 0; i < 4; i++) {
        	j = 0;
        	set.oSet(i,
                    (get.oGet(j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)])
                    // increment j
                    + (matrix[i + (++j * 4)]));
        }
    }

    public static void multiplyIdVecGet3(final idVec<?> set, final idVec<?> get, FloatBuffer matrix) {
        int j = 0;
        for (int i = 0; i < 4; i++) {
        	j = 0;
        	set.oSet(i,
                    (get.oGet(j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (matrix.get(i + (++j * 4))));
        }
    }

    public static void multiplyIdVecGet4(final idVec<?> set, final idVec<?> get, float[] matrix) {
        int j = 0;
        for (int i = 0; i < 4; i++) {
        	j = 0;
        	set.oSet(i,
                    (get.oGet(j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)])
                    // increment j
                    + (get.oGet(++j) * matrix[i + (j * 4)]));
        }
    }

    public static void multiplyIdVecGet4(final idVec<?> set, final idVec<?> get, FloatBuffer matrix) {
        int j = 0;
        for (int i = 0; i < 4; i++) {
        	j = 0;
        	set.oSet(i,
                    (get.oGet(j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4)))
                    // increment j
                    + (get.oGet(++j) * matrix.get(i + (j * 4))));
        }
    }

}
