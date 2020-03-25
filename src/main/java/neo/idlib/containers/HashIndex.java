package neo.idlib.containers;

import java.util.Arrays;

import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class HashIndex {

    /*
     ===============================================================================

     Fast hash table for indexes and arrays.
     Does not allocate memory until the first key/index pair is added.

     ===============================================================================
     */
    static final int DEFAULT_HASH_SIZE = 1024;
    static final int DEFAULT_HASH_GRANULARITY = 1024;

    public static class idHashIndex {

        private int hashSize;
        private int[] hash;
        private int indexSize;
        private int[] indexChain;
        private int granularity;
        private int hashMask;
        private int lookupMask;
        //
        private static final int[] INVALID_INDEX = {-1};
        //
        //
        private static int DBG_counter = 0;
        private final int DBG_count;

        public idHashIndex() {
            this.DBG_count = DBG_counter++;
            Init(DEFAULT_HASH_SIZE, DEFAULT_HASH_SIZE);
        }

        public idHashIndex(final int initialHashSize, final int initialIndexSize) {
            this.DBG_count = DBG_counter++;
            Init(initialHashSize, initialIndexSize);
        }
//	public				~idHashIndex( void );
//

        // returns total size of allocated memory
        public /*size_t*/ int Allocated() {
            return this.hashSize + this.indexSize;
        }

        // returns total size of allocated memory including size of hash index type
        public /*size_t*/ int Size() {
            return Allocated();
        }

        private int bla4 = Integer.MIN_VALUE;
        private int bla5 = Integer.MIN_VALUE;

        public idHashIndex oSet(final idHashIndex other) {
            this.granularity = other.granularity;
            this.hashMask = other.hashMask;
            this.lookupMask = other.lookupMask;

            if (other.lookupMask == 0) {
                this.bla5 = this.hashSize = other.hashSize;
                this.indexSize = other.indexSize;
                Free();
            } else {
                if ((other.hashSize != this.hashSize) || (this.hash == INVALID_INDEX)) {
                    if (this.hash != INVALID_INDEX) {
//				delete[] hash;
                    }
                    this.bla4 = this.hashSize = other.hashSize;
                    this.hash = new int[this.hashSize];
                }
                if ((other.indexSize != this.indexSize) || (this.indexChain == INVALID_INDEX)) {
                    if (this.indexChain != INVALID_INDEX) {
//				delete[] indexChain;
                    }
                    this.indexSize = other.indexSize;
                    this.indexChain = new int[this.indexSize];
                }
//		memcpy( hash, other.hash, hashSize * sizeof( hash[0] ) );
                System.arraycopy(other.hash, 0, this.hash, 0, this.hashSize);
//		memcpy( indexChain, other.indexChain, indexSize * sizeof( indexChain[0] ) );
                System.arraycopy(other.indexChain, 0, this.indexChain, 0, this.indexSize);
            }

            return this;
        }

        // add an index to the hash, assumes the index has not yet been added to the hash
        public void Add(final int key, final int index) {
            int h;

            assert (index >= 0);
            if (this.hash == INVALID_INDEX) {
                Allocate(this.hashSize, index >= this.indexSize ? index + 1 : this.indexSize);
            } else if (index >= this.indexSize) {
                ResizeIndex(index + 1);
            }
            h = key & this.hashMask;
            this.indexChain[index] = this.hash[h];
            this.hash[h] = index;
        }

        // remove an index from the hash
        public void Remove(final int key, final int index) {
            final int k = key & this.hashMask;

            if (this.hash == INVALID_INDEX) {
                return;
            }
            if (this.hash[k] == index) {
                this.hash[k] = this.indexChain[index];
            } else {
                for (int i = this.hash[k]; i != -1; i = this.indexChain[i]) {
                    if (this.indexChain[i] == index) {
                        this.indexChain[i] = this.indexChain[index];
                        break;
                    }
                }
            }
            this.indexChain[index] = -1;
        }

        // get the first index from the hash, returns -1 if empty hash entry
        public int First(final int key) {
            if (null == this.hash) {
                return -1;
            }
            return this.hash[key & this.hashMask & this.lookupMask];
        }

        // get the next index from the hash, returns -1 if at the end of the hash chain
        public int Next(final int index) {
            assert ((index >= 0) && (index < this.indexSize));
            return this.indexChain[index & this.lookupMask];
        }

        // insert an entry into the index and add it to the hash, increasing all indexes >= index
        public void InsertIndex(final int key, final int index) {
            int i, max;

            if (this.hash != INVALID_INDEX) {
                max = index;
                for (i = 0; i < this.hashSize; i++) {
                    if (this.hash[i] >= index) {
                        this.hash[i]++;
                        if (this.hash[i] > max) {
                            max = this.hash[i];
                        }
                    }
                }
                for (i = 0; i < this.indexSize; i++) {
                    if (this.indexChain[i] >= index) {
                        this.indexChain[i]++;
                        if (this.indexChain[i] > max) {
                            max = this.indexChain[i];
                        }
                    }
                }
                if (max >= this.indexSize) {
                    ResizeIndex(max + 1);
                }
                for (i = max; i > index; i--) {
                    this.indexChain[i] = this.indexChain[i - 1];
                }
                this.indexChain[index] = -1;
            }
            Add(key, index);
        }

        // remove an entry from the index and remove it from the hash, decreasing all indexes >= index
        public void RemoveIndex(final int key, final int index) {
            int i, max;

            Remove(key, index);
            if (this.hash != INVALID_INDEX) {
                max = index;
                for (i = 0; i < this.hashSize; i++) {
                    if (this.hash[i] >= index) {
                        if (this.hash[i] > max) {
                            max = this.hash[i];
                        }
                        this.hash[i]--;
                    }
                }
                for (i = 0; i < this.indexSize; i++) {
                    if (this.indexChain[i] >= index) {
                        if (this.indexChain[i] > max) {
                            max = this.indexChain[i];
                        }
                        this.indexChain[i]--;
                    }
                }
                for (i = index; i < max; i++) {
                    this.indexChain[i] = this.indexChain[i + 1];
                }
                this.indexChain[max] = -1;
            }
        }

        // clear the hash
        public void Clear() {
            // only clear the hash table because clearing the indexChain is not really needed
            if (this.hash != INVALID_INDEX) {
//		memset( hash, 0xff, hashSize * sizeof( hash[0] ) );
                Arrays.fill(this.hash, -1);//0xff);
            }
        }

        // clear and resize
        public void Clear(final int newHashSize, final int newIndexSize) {
            Free();
            this.bla3 = this.hashSize = newHashSize;
            this.indexSize = newIndexSize;
        }
        private int bla3 = Integer.MIN_VALUE;

        // free allocated memory
        public void Free() {
//            if (hash != INVALID_INDEX) {
//                hash = null;//delete[] hash;
            this.hash = INVALID_INDEX;
//            }
//            if (indexChain != INVALID_INDEX) {
//                indexChain = null;//delete[] indexChain;
            this.indexChain = INVALID_INDEX;
//            }
            this.lookupMask = 0;
//            TempDump.printCallStack("----" + DBG_count);
        }

        // get size of hash table
        public int GetHashSize() {
            return this.hashSize;
        }

        // get size of the index
        public int GetIndexSize() {
            return this.indexSize;
        }

        // set granularity
        public void SetGranularity(final int newGranularity) {
            assert (newGranularity > 0);
            this.granularity = newGranularity;
        }

        // force resizing the index, current hash table stays intact
        public void ResizeIndex(final int newIndexSize) {
            int[] oldIndexChain;
            int mod, newSize;

            if (newIndexSize <= this.indexSize) {
                return;
            }

            mod = newIndexSize % this.granularity;
            if (0 == mod) {
                newSize = newIndexSize;
            } else {
                newSize = (newIndexSize + this.granularity) - mod;
            }

            if (this.indexChain == INVALID_INDEX) {
                this.indexSize = newSize;
                return;
            }

            oldIndexChain = this.indexChain;
            this.indexChain = new int[newSize];
//	memcpy( indexChain, oldIndexChain, indexSize * sizeof(int) );
            System.arraycopy(oldIndexChain, 0, this.indexChain, 0, this.indexSize);
//	memset( indexChain + indexSize, 0xff, (newSize - indexSize) * sizeof(int) );
            Arrays.fill(this.indexChain, this.indexSize, newSize, -1);//0xff);
//	delete[] oldIndexChain;
            this.indexSize = newSize;
        }

        // returns number in the range [0-100] representing the spread over the hash table
        public int GetSpread() {
            int i, index, totalItems, average, error, e;
            int[] numHashItems;

            if (this.hash == INVALID_INDEX) {
                return 100;
            }

            totalItems = 0;
            numHashItems = new int[this.hashSize];
            for (i = 0; i < this.hashSize; i++) {
                numHashItems[i] = 0;
                for (index = this.hash[i]; index >= 0; index = this.indexChain[index]) {
                    numHashItems[i]++;
                }
                totalItems += numHashItems[i];
            }
            // if no items in hash
            if (totalItems <= 1) {
//		delete[] numHashItems;
                return 100;
            }
            average = totalItems / this.hashSize;
            error = 0;
            for (i = 0; i < this.hashSize; i++) {
                e = Math.abs(numHashItems[i] - average);
                if (e > 1) {
                    error += e - 1;
                }
            }
//	delete[] numHashItems;
            return 100 - ((error * 100) / totalItems);
        }

        public int GenerateKey(final char[] string) {
            return GenerateKey(string, true);
        }

        // returns a key for a string
        public int GenerateKey(final char[] string, boolean caseSensitive) {
            if (caseSensitive) {
                return (idStr.Hash(string) & this.hashMask);
            } else {
                return (idStr.IHash(string) & this.hashMask);
            }
        }

        public int GenerateKey(final String string, boolean caseSensitive) {
            return GenerateKey(string.toCharArray(), caseSensitive);
        }

        public int GenerateKey(final String string) {
            return GenerateKey(string, true);
        }

        // returns a key for a vector
        public int GenerateKey(final idVec3 v) {
            return ((((int) v.oGet(0)) + ((int) v.oGet(1)) + ((int) v.oGet(2))) & this.hashMask);
        }

        // returns a key for two integers
        public int GenerateKey(final int n1, final int n2) {
            return ((n1 + n2) & this.hashMask);
        }

        private void Init(final int initialHashSize, final int initialIndexSize) {
            assert (idMath.IsPowerOfTwo(initialHashSize));

            this.bla = this.hashSize = initialHashSize;
            this.hash = INVALID_INDEX;
            this.indexSize = initialIndexSize;
            this.indexChain = INVALID_INDEX;
            this.granularity = DEFAULT_HASH_GRANULARITY;
            this.bla2 = this.hashMask = this.hashSize - 1;
            this.lookupMask = 0;
        }
        private int bla = Integer.MIN_VALUE;
        private int bla2 = Integer.MIN_VALUE;//TODO:remove the "bla's".

        private void Allocate(final int newHashSize, final int newIndexSize) {
            assert (idMath.IsPowerOfTwo(newHashSize));

            Free();
            this.hashSize = newHashSize;
            this.hash = new int[this.hashSize];
//            memset(hash, 0xff, hashSize * sizeof(hash[0]));
            Arrays.fill(this.hash, -1);//0xff);
            this.indexSize = newIndexSize;
            this.indexChain = new int[this.indexSize];
//            memset(indexChain, 0xff, indexSize * sizeof(indexChain[0]));
            Arrays.fill(this.indexChain, -1);//0xff);
            this.hashMask = this.hashSize - 1;
            this.lookupMask = -1;
        }
    }
}
