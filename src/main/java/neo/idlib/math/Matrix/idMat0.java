/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package neo.idlib.math.Matrix;

/**
 * Yes, the one, the only, the ever illusive zero matrix.
 */
public class idMat0 {

    public static final double MATRIX_INVERSE_EPSILON = 1.0E-14;
    public static final double MATRIX_EPSILON = 1.0E-6;//TODO: re-type to float.

    public static void matrixPrint(idMatX x, String label) {
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

}
