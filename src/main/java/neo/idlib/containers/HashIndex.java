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
            DBG_count = DBG_counter++;
            Init(DEFAULT_HASH_SIZE, DEFAULT_HASH_SIZE);
        }

        public idHashIndex(final int initialHashSize, final int initialIndexSize) {
            DBG_count = DBG_counter++;
            Init(initialHashSize, initialIndexSize);
        }
//	public				~idHashIndex( void );
//

        // returns total size of allocated memory
        public /*size_t*/ int Allocated() {
            return hashSize + indexSize;
        }

        // returns total size of allocated memory including size of hash index type
        public /*size_t*/ int Size() {
            return Allocated();
        }

        private int bla4 = Integer.MIN_VALUE;
        private int bla5 = Integer.MIN_VALUE;

        public idHashIndex oSet(final idHashIndex other) {
            granularity = other.granularity;
            hashMask = other.hashMask;
            lookupMask = other.lookupMask;

            if (other.lookupMask == 0) {
                bla5 = hashSize = other.hashSize;
                indexSize = other.indexSize;
                Free();
            } else {
                if (other.hashSize != hashSize || hash == INVALID_INDEX) {
                    if (hash != INVALID_INDEX) {
//				delete[] hash;
                    }
                    bla4 = hashSize = other.hashSize;
                    hash = new int[hashSize];
                }
                if (other.indexSize != indexSize || indexChain == INVALID_INDEX) {
                    if (indexChain != INVALID_INDEX) {
//				delete[] indexChain;
                    }
                    indexSize = other.indexSize;
                    indexChain = new int[indexSize];
                }
//		memcpy( hash, other.hash, hashSize * sizeof( hash[0] ) );
                System.arraycopy(other.hash, 0, hash, 0, hashSize);
//		memcpy( indexChain, other.indexChain, indexSize * sizeof( indexChain[0] ) );
                System.arraycopy(other.indexChain, 0, indexChain, 0, indexSize);
            }

            return this;
        }

        // add an index to the hash, assumes the index has not yet been added to the hash
        public void Add(final int key, final int index) {
            int h;

            assert (index >= 0);
            if (hash == INVALID_INDEX) {
                Allocate(hashSize, index >= indexSize ? index + 1 : indexSize);
            } else if (index >= indexSize) {
                ResizeIndex(index + 1);
            }
            h = key & hashMask;
            indexChain[index] = hash[h];
            hash[h] = index;
        }

        // remove an index from the hash
        public void Remove(final int key, final int index) {
            int k = key & hashMask;

            if (hash == INVALID_INDEX) {
                return;
            }
            if (hash[k] == index) {
                hash[k] = indexChain[index];
            } else {
                for (int i = hash[k]; i != -1; i = indexChain[i]) {
                    if (indexChain[i] == index) {
                        indexChain[i] = indexChain[index];
                        break;
                    }
                }
            }
            indexChain[index] = -1;
        }

        // get the first index from the hash, returns -1 if empty hash entry
        public int First(final int key) {
            if (null == hash) {
                return -1;
            }
            return hash[key & hashMask & lookupMask];
        }

        // get the next index from the hash, returns -1 if at the end of the hash chain
        public int Next(final int index) {
            assert (index >= 0 && index < indexSize);
            return indexChain[index & lookupMask];
        }

        // insert an entry into the index and add it to the hash, increasing all indexes >= index
        public void InsertIndex(final int key, final int index) {
            int i, max;

            if (hash != INVALID_INDEX) {
                max = index;
                for (i = 0; i < hashSize; i++) {
                    if (hash[i] >= index) {
                        hash[i]++;
                        if (hash[i] > max) {
                            max = hash[i];
                        }
                    }
                }
                for (i = 0; i < indexSize; i++) {
                    if (indexChain[i] >= index) {
                        indexChain[i]++;
                        if (indexChain[i] > max) {
                            max = indexChain[i];
                        }
                    }
                }
                if (max >= indexSize) {
                    ResizeIndex(max + 1);
                }
                for (i = max; i > index; i--) {
                    indexChain[i] = indexChain[i - 1];
                }
                indexChain[index] = -1;
            }
            Add(key, index);
        }

        // remove an entry from the index and remove it from the hash, decreasing all indexes >= index
        public void RemoveIndex(final int key, final int index) {
            int i, max;

            Remove(key, index);
            if (hash != INVALID_INDEX) {
                max = index;
                for (i = 0; i < hashSize; i++) {
                    if (hash[i] >= index) {
                        if (hash[i] > max) {
                            max = hash[i];
                        }
                        hash[i]--;
                    }
                }
                for (i = 0; i < indexSize; i++) {
                    if (indexChain[i] >= index) {
                        if (indexChain[i] > max) {
                            max = indexChain[i];
                        }
                        indexChain[i]--;
                    }
                }
                for (i = index; i < max; i++) {
                    indexChain[i] = indexChain[i + 1];
                }
                indexChain[max] = -1;
            }
        }

        // clear the hash
        public void Clear() {
            // only clear the hash table because clearing the indexChain is not really needed
            if (hash != INVALID_INDEX) {
//		memset( hash, 0xff, hashSize * sizeof( hash[0] ) );
                Arrays.fill(hash, -1);//0xff);
            }
        }

        // clear and resize
        public void Clear(final int newHashSize, final int newIndexSize) {
            Free();
            bla3 = hashSize = newHashSize;
            indexSize = newIndexSize;
        }
        private int bla3 = Integer.MIN_VALUE;

        // free allocated memory
        public void Free() {
//            if (hash != INVALID_INDEX) {
//                hash = null;//delete[] hash;
            hash = INVALID_INDEX;
//            }
//            if (indexChain != INVALID_INDEX) {
//                indexChain = null;//delete[] indexChain;
            indexChain = INVALID_INDEX;
//            }
            lookupMask = 0;
//            TempDump.printCallStack("----" + DBG_count);
        }

        // get size of hash table
        public int GetHashSize() {
            return hashSize;
        }

        // get size of the index
        public int GetIndexSize() {
            return indexSize;
        }

        // set granularity
        public void SetGranularity(final int newGranularity) {
            assert (newGranularity > 0);
            granularity = newGranularity;
        }

        // force resizing the index, current hash table stays intact
        public void ResizeIndex(final int newIndexSize) {
            int[] oldIndexChain;
            int mod, newSize;

            if (newIndexSize <= indexSize) {
                return;
            }

            mod = newIndexSize % granularity;
            if (0 == mod) {
                newSize = newIndexSize;
            } else {
                newSize = newIndexSize + granularity - mod;
            }

            if (indexChain == INVALID_INDEX) {
                indexSize = newSize;
                return;
            }

            oldIndexChain = indexChain;
            indexChain = new int[newSize];
//	memcpy( indexChain, oldIndexChain, indexSize * sizeof(int) );
            System.arraycopy(oldIndexChain, 0, indexChain, 0, indexSize);
//	memset( indexChain + indexSize, 0xff, (newSize - indexSize) * sizeof(int) );
            Arrays.fill(indexChain, indexSize, newSize, -1);//0xff);
//	delete[] oldIndexChain;
            indexSize = newSize;
        }

        // returns number in the range [0-100] representing the spread over the hash table
        public int GetSpread() {
            int i, index, totalItems, average, error, e;
            int[] numHashItems;

            if (hash == INVALID_INDEX) {
                return 100;
            }

            totalItems = 0;
            numHashItems = new int[hashSize];
            for (i = 0; i < hashSize; i++) {
                numHashItems[i] = 0;
                for (index = hash[i]; index >= 0; index = indexChain[index]) {
                    numHashItems[i]++;
                }
                totalItems += numHashItems[i];
            }
            // if no items in hash
            if (totalItems <= 1) {
//		delete[] numHashItems;
                return 100;
            }
            average = totalItems / hashSize;
            error = 0;
            for (i = 0; i < hashSize; i++) {
                e = Math.abs(numHashItems[i] - average);
                if (e > 1) {
                    error += e - 1;
                }
            }
//	delete[] numHashItems;
            return 100 - (error * 100 / totalItems);
        }

        public int GenerateKey(final char[] string) {
            return GenerateKey(string, true);
        }

        // returns a key for a string
        public int GenerateKey(final char[] string, boolean caseSensitive) {
            if (caseSensitive) {
                return (idStr.Hash(string) & hashMask);
            } else {
                return (idStr.IHash(string) & hashMask);
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
            return ((((int) v.oGet(0)) + ((int) v.oGet(1)) + ((int) v.oGet(2))) & hashMask);
        }

        // returns a key for two integers
        public int GenerateKey(final int n1, final int n2) {
            return ((n1 + n2) & hashMask);
        }

        private void Init(final int initialHashSize, final int initialIndexSize) {
            assert (idMath.IsPowerOfTwo(initialHashSize));

            bla = hashSize = initialHashSize;
            hash = INVALID_INDEX;
            indexSize = initialIndexSize;
            indexChain = INVALID_INDEX;
            granularity = DEFAULT_HASH_GRANULARITY;
            bla2 = hashMask = hashSize - 1;
            lookupMask = 0;
        }
        private int bla = Integer.MIN_VALUE;
        private int bla2 = Integer.MIN_VALUE;//TODO:remove the "bla's".

        private void Allocate(final int newHashSize, final int newIndexSize) {
            assert (idMath.IsPowerOfTwo(newHashSize));

            Free();
            hashSize = newHashSize;
            hash = new int[hashSize];
//            memset(hash, 0xff, hashSize * sizeof(hash[0]));
            Arrays.fill(hash, -1);//0xff);
            indexSize = newIndexSize;
            indexChain = new int[indexSize];
//            memset(indexChain, 0xff, indexSize * sizeof(indexChain[0]));
            Arrays.fill(indexChain, -1);//0xff);
            hashMask = hashSize - 1;
            lookupMask = -1;
        }
    };
}
