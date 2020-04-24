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
            this.numUsers = 0;
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
            return this.pool;
        }

    }

    public static class idStrPool {

        private boolean caseSensitive;
        private final idList<idPoolStr> pool;
        private final idHashIndex poolHash;

        public idStrPool() {
            this.caseSensitive = true;
            this.pool = new idList<>();
            this.poolHash = new idHashIndex();
        }

        public void SetCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public int Num() {
            return this.pool.Num();
        }

        public int Allocated() {
            int i;
            int size;

            size = this.pool.Allocated() + this.poolHash.Allocated();
            for (i = 0; i < this.pool.Num(); i++) {
                size += this.pool.oGet(i).Allocated();
            }
            return size;
        }

        public int Size() {
            int i;
            int size;

            size = this.pool.Size() + this.poolHash.Size();
            for (i = 0; i < this.pool.Num(); i++) {
                size += this.pool.oGet(i).Size();
            }
            return size;
        }

        public idPoolStr oGet(int index) {
            return this.pool.oGet(index);
        }

        public idPoolStr AllocString(final String string) {
            int i, hash;
            idPoolStr poolStr;

            hash = this.poolHash.GenerateKey(string, this.caseSensitive);
            if (this.caseSensitive) {
                for (i = this.poolHash.First(hash); i != -1; i = this.poolHash.Next(i)) {
                    if (this.pool.oGet(i).Cmp(string) == 0) {
                        this.pool.oGet(i).numUsers++;
                        return this.pool.oGet(i);
                    }
                }
            } else {
                for (i = this.poolHash.First(hash); i != -1; i = this.poolHash.Next(i)) {
                    if (this.pool.oGet(i).Icmp(string) == 0) {
                        this.pool.oGet(i).numUsers++;
//                        System.out.printf("AllocString, i = %d\n", i);
                        return this.pool.oGet(i);
                    }
                }
            }

            poolStr = new idPoolStr();
            poolStr.oSet(string);//TODO:*static_cast<idStr *>(poolStr) = string;
            poolStr.pool = this;
            poolStr.numUsers = 1;
            this.poolHash.Add(hash, this.pool.Append(poolStr));
            return poolStr;
        }

        public void FreeString(final idPoolStr poolStr) {
            int i, hash;

            assert (poolStr.numUsers >= 1);
            assert (poolStr.pool == this);

            poolStr.numUsers--;
            if (poolStr.numUsers <= 0) {
                hash = this.poolHash.GenerateKey(poolStr.getData(), this.caseSensitive);
                if (this.caseSensitive) {
                    for (i = this.poolHash.First(hash); i != -1; i = this.poolHash.Next(i)) {
                        if (this.pool.oGet(i).Cmp(poolStr.getData()) == 0) {
                            break;
                        }
                    }
                } else {
                    for (i = this.poolHash.First(hash); i != -1; i = this.poolHash.Next(i)) {
                        if (this.pool.oGet(i).Icmp(poolStr.getData()) == 0) {
                            break;
                        }
                    }
                }
                assert (i != -1);
                assert (this.pool.oGet(i) == poolStr);
//		delete pool[i];
                this.pool.RemoveIndex(i);
                this.poolHash.RemoveIndex(hash, i);
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

            for (i = 0; i < this.pool.Num(); i++) {
                this.pool.oGet(i).numUsers = 0;
            }
            this.pool.DeleteContents(true);
            this.poolHash.Free();
        }
    }
}
