package neo.idlib.math.Matrix;

import neo.idlib.math.Vector;

import java.util.stream.Stream;

/**
 * Yes, the one, the only, the ever illusive zero matrix.
 */
public class idMat0 {

    static final double MATRIX_INVERSE_EPSILON = 1.0E-14;
    static final double MATRIX_EPSILON         = 1.0E-6;//TODO: re-type to float.

    static void matrixPrint(idMatX x, String label) {
        int rows = x.GetNumRows();
        int columns = x.GetNumColumns();
        System.out.println("START " + label);
        for (int b = 0; b < rows; b++) {
            for (int a = 0; a < columns; a++) {
                System.out.print(x.oGet(b, a) + "\t");
            }
            System.out.println();
        }
        System.out.println("STOP " + label);
    }

    static Vector.idVec3[] genVec3Array(final int size) {
        return Stream.generate(Vector.idVec3::new).limit(size).toArray(Vector.idVec3[]::new);
    }
}
