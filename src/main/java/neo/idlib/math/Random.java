package neo.idlib.math;

import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class Random {

    /*
     ===============================================================================

     Random number generator

     ===============================================================================
     */
    public static class idRandom {
        public static final int MAX_RAND = 0x7fff;

        private int seed;

        public idRandom() {
            this.seed = 0;
        }

        public idRandom(int seed) {
            this.seed = seed;
        }

        public idRandom(idRandom random) {
            this.seed = random.seed;
        }

        public void SetSeed(int seed) {
            this.seed = seed;
        }

        public int GetSeed() {
            return seed;
        }

        public int RandomInt() {// random integer in the range [0, MAX_RAND]
            seed = 69069 * seed + 1;
            return (seed & idRandom.MAX_RAND);
        }

        public int RandomInt(double max) {// random integer in the range [0, max[
            if (max == 0) {
                return 0;            // avoid divide by zero error
            }
            return (int) (RandomInt() % max);
        }

        public float RandomFloat() {// random number in the range [0.0f, 1.0f]
            return (RandomInt() / (float) (idRandom.MAX_RAND + 1));
        }

        public float CRandomFloat() {// random number in the range [-1.0f, 1.0f]
            return (2.0f * (RandomFloat() - 0.5f));
        }
    }

    /*
     ===============================================================================

     Random number generator

     ===============================================================================
     */
    static class idRandom2 {

        public idRandom2() {
            this.seed = 0;
        }

        public idRandom2(long seed) {
            this.seed = seed;
        }

        public void SetSeed(long seed) {
            this.seed = seed;
        }

        public long GetSeed() {
            return seed;
        }

        public int RandomInt() {// random integer in the range [0, MAX_RAND]
            seed = 1664525L * seed + 1013904223L;
            return ((int) seed & idRandom2.MAX_RAND);
        }

        public int RandomInt(int max) {// random integer in the range [0, max]
            if (max == 0) {
                return 0;        // avoid divide by zero error
            }
            return (RandomInt() >> (16 - idMath.BitsForInteger(max))) % max;
        }

        public float RandomFloat() {// random number in the range [0.0f, 1.0f]
            long i;
            seed = 1664525L * seed + 1013904223L;
            i = idRandom2.IEEE_ONE | (seed & idRandom2.IEEE_MASK);
            return (i - 1.0f);
        }

        public float CRandomFloat() {// random number in the range [-1.0f, 1.0f]
            long i;
            seed = 1664525L * seed + 1013904223L;
            i = idRandom2.IEEE_ONE | (seed & idRandom2.IEEE_MASK);
            return (2.0f * i - 3.0f);
        }

        public static final  int  MAX_RAND  = 0x7fff;
        private              long seed;
        private static final long IEEE_ONE  = 0x3f800000;
        private static final long IEEE_MASK = 0x007fffff;
    }
}
