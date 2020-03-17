package neo.idlib.containers;

import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;

/**
 *
 */
public class StrPool {

    /*
     ===============================================================================

     idStrPool

     ===============================================================================
     */
    public static class idPoolStr extends idStr {
//	friend class idStrPool;
//

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private idStrPool pool;
        private int numUsers;
        //
        //

        public idPoolStr() {
            numUsers = 0;
        }
//public						~idPoolStr() { assert( numUsers == 0 ); }

        // returns total size of allocated memory
        @Override
        public int Allocated() {
            return super.Allocated();
        }

        // returns total size of allocated memory including size of string pool type
        @Override
        public int Size() {
            return /*sizeof( *this ) + */ Allocated();
        }

        // returns a pointer to the pool this string was allocated from
        public idStrPool GetPool() {
            return pool;
        }

    };

    public static class idStrPool {

        private boolean caseSensitive;
        private idList<idPoolStr> pool;
        private idHashIndex poolHash;

        public idStrPool() {
            caseSensitive = true;
            this.pool = new idList<>();
            this.poolHash = new idHashIndex();
        }

        public void SetCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public int Num() {
            return pool.Num();
        }

        public int Allocated() {
            int i;
            int size;

            size = pool.Allocated() + poolHash.Allocated();
            for (i = 0; i < pool.Num(); i++) {
                size += pool.oGet(i).Allocated();
            }
            return size;
        }

        public int Size() {
            int i;
            int size;

            size = pool.Size() + poolHash.Size();
            for (i = 0; i < pool.Num(); i++) {
                size += pool.oGet(i).Size();
            }
            return size;
        }

        public idPoolStr oGet(int index) {
            return pool.oGet(index);
        }

        public idPoolStr AllocString(final String string) {
            int i, hash;
            idPoolStr poolStr;

            hash = poolHash.GenerateKey(string, caseSensitive);
            if (caseSensitive) {
                for (i = poolHash.First(hash); i != -1; i = poolHash.Next(i)) {
                    if (pool.oGet(i).Cmp(string) == 0) {
                        pool.oGet(i).numUsers++;
                        return pool.oGet(i);
                    }
                }
            } else {
                for (i = poolHash.First(hash); i != -1; i = poolHash.Next(i)) {
                    if (pool.oGet(i).Icmp(string) == 0) {
                        pool.oGet(i).numUsers++;
//                        System.out.printf("AllocString, i = %d\n", i);
                        return pool.oGet(i);
                    }
                }
            }

            poolStr = new idPoolStr();
            poolStr.oSet(string);//TODO:*static_cast<idStr *>(poolStr) = string;
            poolStr.pool = this;
            poolStr.numUsers = 1;
            poolHash.Add(hash, pool.Append(poolStr));
            return poolStr;
        }

        public void FreeString(final idPoolStr poolStr) {
            int i, hash;

            assert (poolStr.numUsers >= 1);
            assert (poolStr.pool == this);

            poolStr.numUsers--;
            if (poolStr.numUsers <= 0) {
                hash = poolHash.GenerateKey(poolStr.c_str(), caseSensitive);
                if (caseSensitive) {
                    for (i = poolHash.First(hash); i != -1; i = poolHash.Next(i)) {
                        if (pool.oGet(i).Cmp(poolStr.getData()) == 0) {
                            break;
                        }
                    }
                } else {
                    for (i = poolHash.First(hash); i != -1; i = poolHash.Next(i)) {
                        if (pool.oGet(i).Icmp(poolStr.getData()) == 0) {
                            break;
                        }
                    }
                }
                assert (i != -1);
                assert (pool.oGet(i) == poolStr);
//		delete pool[i];
                pool.RemoveIndex(i);
                poolHash.RemoveIndex(hash, i);
            }
        }

        public idPoolStr CopyString(final idPoolStr poolStr) {

            assert (poolStr.numUsers >= 1);

            if (poolStr.pool == this) {
                // the string is from this pool so just increase the user count
                poolStr.numUsers++;
                return poolStr;
            } else {
                // the string is from another pool so it needs to be re-allocated from this pool.
                return AllocString(poolStr.getData());
            }
        }

        public void Clear() {
            int i;

            for (i = 0; i < pool.Num(); i++) {
                pool.oGet(i).numUsers = 0;
            }
            pool.DeleteContents(true);
            poolHash.Free();
        }
    };
}
