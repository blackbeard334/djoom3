package neo.idlib.containers;

import static neo.idlib.containers.BTree.*;

/**
 *
 */
public class BinSearch {

    /*
     ===============================================================================

     Binary Search templates

     The array elements have to be ordered in increasing order.

     ===============================================================================
     */

    /*
     ====================
     idBinSearch_GreaterEqual

     Finds the last array element which is smaller than the given value.
     ====================
     */
    static <type> int idBinSearch_Less(final type[] array, final int arraySize, final type value) {
        int len = arraySize;
        int mid = len;
        int offset = 0;
        while (mid > 0) {
            mid = len >> 1;
            if (LT(array[offset + mid],/*<*/ value)) {
                offset += mid;
            }
            len -= mid;
        }
        return offset;
    }

    /*
     ====================
     idBinSearch_GreaterEqual

     Finds the last array element which is smaller than or equal to the given value.
     ====================
     */
    public static <type> int idBinSearch_LessEqual(final type[] array, final int arraySize, final type value) {
        int len = arraySize;
        int mid = len;
        int offset = 0;
        while (mid > 0) {
            mid = len >> 1;
            if (LTE(array[offset + mid],/*<=*/ value)) {
                offset += mid;
            }
            len -= mid;
        }
        return offset;
    }

    /*
     ====================
     idBinSearch_Greater

     Finds the first array element which is greater than the given value.
     ====================
     */
    static <type> int idBinSearch_Greater(final type[] array, final int arraySize, final type value) {
        int len = arraySize;
        int mid = len;
        int offset = 0;
        int res = 0;
        while (mid > 0) {
            mid = len >> 1;
            if (GT(array[offset + mid],/*>*/ value)) {
                res = 0;
            } else {
                offset += mid;
                res = 1;
            }
            len -= mid;
        }
        return offset + res;
    }

    /*
     ====================
     idBinSearch_GreaterEqual

     Finds the first array element which is greater than or equal to the given value.
     ====================
     */
    public static <type> int idBinSearch_GreaterEqual(final type[] array, final int arraySize, final type value) {
        int len = arraySize;
        int mid = len;
        int offset = 0;
        int res = 0;
        while (mid > 0) {
            mid = len >> 1;
            if (GTE(array[offset + mid],/*>=*/ value)) {
                res = 0;
            } else {
                offset += mid;
                res = 1;
            }
            len -= mid;
        }
        return offset + res;
    }
}
