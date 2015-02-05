package neo.idlib.math;

import neo.idlib.math.Simd_MMX.idSIMD_MMX;

/**
 *
 */
@Deprecated
public class Simd_3DNow {

//===============================================================
//
//	3DNow! implementation of idSIMDProcessor
//
//===============================================================
    static class idSIMD_3DNow extends idSIMD_MMX {

        @Override
        public String GetName() {
            return "MMX & 3DNow!";
        }

        @Override
        public void Memcpy(Object[] dest0, Object[] src0, int count0) {
            super.Memcpy(dest0, src0, count0);
        }
    };
}
