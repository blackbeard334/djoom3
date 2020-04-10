package neo.idlib.containers;

import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec;

/**
 *
 */
public class VectorSet {

    /*
     ===============================================================================

     Vector Set

     Creates a set of vectors without duplicates.

     ===============================================================================
     */
    public static class idVectorSet<type> extends idList<type> {

        private       idHashIndex hash;
        private       idVec       mins;
        private       idVec       maxs;
        private       int         boxHashSize;
        private       float[]     boxInvSize/*= new float[dimension]*/;
        private       float[]     boxHalfSize /*= new float[dimension]*/;
        //        
        //  
        private final int         dimension;
        //
        //

        public idVectorSet(final int dimension) {
            this.dimension = dimension;
            this.boxInvSize = new float[dimension];
            this.boxHalfSize = new float[dimension];
            this.hash.Clear(idMath.IPow(this.boxHashSize, dimension), 128);
            this.boxHashSize = 16;
//	memset( boxInvSize, 0, dimension * sizeof( boxInvSize[0] ) );
//	memset( boxHalfSize, 0, dimension * sizeof( boxHalfSize[0] ) );
        }

        public idVectorSet(final idVec mins, final idVec maxs, final int boxHashSize, final int initialSize, final int dimension) {
            this.dimension = dimension;
            Init(mins, maxs, boxHashSize, initialSize);
        }

//
//							// returns total size of allocated memory
//public	size_t					Allocated( void ) const { return idList<type>::Allocated() + hash.Allocated(); }
//							// returns total size of allocated memory including size of type
//public	size_t					Size( void ) const { return sizeof( *this ) + Allocated(); }
//
        public void Init(final idVec mins, final idVec maxs, final int boxHashSize, final int initialSize) {
            int i;
            float boxSize;

            super.AssureSize(initialSize);
            super.SetNum(0, false);

            this.hash.Clear(idMath.IPow(boxHashSize, this.dimension), initialSize);

            this.mins = mins;
            this.maxs = maxs;
            this.boxHashSize = boxHashSize;

            for (i = 0; i < this.dimension; i++) {
                boxSize = (maxs.oGet(i) - mins.oGet(i)) / boxHashSize;
                this.boxInvSize[i] = 1.0f / boxSize;
                this.boxHalfSize[i] = boxSize * 0.5f;
            }
        }

        public void ResizeIndex(final int newSize) {
            super.Resize(newSize);
            this.hash.ResizeIndex(newSize);
        }

        @Override
        public void Clear() {
            super.Clear();
            this.hash.Clear();
        }
//

        public int FindVector(final idVec v, final float epsilon) {
            int i, j, k, hashKey;
            final int[] partialHashKey = new int[this.dimension];

            for (i = 0; i < this.dimension; i++) {
                assert (epsilon <= this.boxHalfSize[i]);
                partialHashKey[i] = (int) ((v.oGet(i) - this.mins.oGet(i) - this.boxHalfSize[i]) * this.boxInvSize[i]);
            }

            for (i = 0; i < (1 << this.dimension); i++) {

                hashKey = 0;
                for (j = 0; j < this.dimension; j++) {
                    hashKey *= this.boxHashSize;
                    hashKey += partialHashKey[j] + ((i >> j) & 1);
                }

                for (j = this.hash.First(hashKey); j >= 0; j = this.hash.Next(j)) {
                    final idVec lv = (idVec) this.oGet(j);
                    for (k = 0; k < this.dimension; k++) {
                        if (idMath.Fabs(lv.oGet(k) - v.oGet(k)) > epsilon) {
                            break;
                        }
                    }
                    if (k >= this.dimension) {
                        return j;
                    }
                }
            }

            hashKey = 0;
            for (i = 0; i < this.dimension; i++) {
                hashKey *= this.boxHashSize;
                hashKey += (int) ((v.oGet(i) - this.mins.oGet(i)) * this.boxInvSize[i]);
            }

            this.hash.Add(hashKey, super.Num());
            this.Append((type)v);
            return super.Num() - 1;
        }
    }

    /*
     ===============================================================================

     Vector Subset

     Creates a subset without duplicates from an existing list with vectors.

     ===============================================================================
     */
    public static class idVectorSubset<type> {

        private final idHashIndex   hash = new idHashIndex();
        private       idVec   mins;
        private       idVec   maxs;
        private       int     boxHashSize;
        private       float[] boxInvSize /*= new float[dimension]*/;
        private       float[] boxHalfSize/*= new float[dimension]*/;
        //        
        private final int     dimension;
        //
        //

        //private idVectorSubset() {
        //    this.dimension = -1;
        //}

        public idVectorSubset(int dimension) {
            this.dimension = dimension;
            this.boxInvSize = new float[dimension];
            this.boxHalfSize = new float[dimension];
            this.hash.Clear(idMath.IPow(this.boxHashSize, dimension), 128);
            this.boxHashSize = 16;
//	memset( boxInvSize, 0, dimension * sizeof( boxInvSize[0] ) );
//	memset( boxHalfSize, 0, dimension * sizeof( boxHalfSize[0] ) );
        }

        public idVectorSubset(final idVec mins, final idVec maxs, final int boxHashSize, final int initialSize, final int dimension) {
            this.dimension = dimension;
            Init(mins, maxs, boxHashSize, initialSize);
        }
//
//							// returns total size of allocated memory
//	size_t					Allocated( void ) const { return idList<type>::Allocated() + hash.Allocated(); }
//							// returns total size of allocated memory including size of type
//	size_t					Size( void ) const { return sizeof( *this ) + Allocated(); }
//

        public void Init(final idVec mins, final idVec maxs, final int boxHashSize, final int initialSize) {
            int i;
            float boxSize;

            this.hash.Clear(idMath.IPow(boxHashSize, this.dimension), initialSize);

            this.mins = mins;
            this.maxs = maxs;
            this.boxHashSize = boxHashSize;

            for (i = 0; i < this.dimension; i++) {
                boxSize = (maxs.oGet(i) - mins.oGet(i)) / boxHashSize;
                this.boxInvSize[i] = 1.0f / boxSize;
                this.boxHalfSize[i] = boxSize * 0.5f;
            }
        }

        public void Clear() {
//	idList<type>::Clear();
            this.hash.Clear();
        }
//

        // returns either vectorNum or an index to a previously found vector
        public int FindVector(final idVec[] vectorList, final int vectorNum, final float epsilon) {
            int i, j, k, hashKey;
            final int[] partialHashKey = new int[this.dimension];
            final idVec v = vectorList[vectorNum];

            for (i = 0; i < this.dimension; i++) {
                assert (epsilon <= this.boxHalfSize[i]);
                partialHashKey[i] = (int) (((v.oGet(i) - this.mins.oGet(i)) - this.boxHalfSize[i]) * this.boxInvSize[i]);
            }

            for (i = 0; i < (1 << this.dimension); i++) {

                hashKey = 0;
                for (j = 0; j < this.dimension; j++) {
                    hashKey *= this.boxHashSize;
                    hashKey += partialHashKey[j] + ((i >> j) & 1);
                }

                for (j = this.hash.First(hashKey); j >= 0; j = this.hash.Next(j)) {
                    final idVec lv = vectorList[j];
                    for (k = 0; k < this.dimension; k++) {
                        if (idMath.Fabs(lv.oGet(k) - v.oGet(k)) > epsilon) {
                            break;
                        }
                    }
                    if (k >= this.dimension) {
                        return j;
                    }
                }
            }

            hashKey = 0;
            for (i = 0; i < this.dimension; i++) {
                hashKey *= this.boxHashSize;
                hashKey += (int) ((v.oGet(i) - this.mins.oGet(i)) * this.boxInvSize[i]);
            }

            this.hash.Add(hashKey, vectorNum);
            return vectorNum;
        }
    }
}
