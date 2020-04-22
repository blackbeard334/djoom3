package neo.idlib.containers;

import java.util.stream.Stream;

import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class HashTable {
    /*
     ===============================================================================

     General hash table. Slower than idHashIndex but it can also be used for
     linked lists and other data structures than just indexes or arrays.

     ===============================================================================
     */

    public static class idHashTable<Type> {

        private final hashnode_s<Type>[] heads;
        //
        private final int          tablesize;
        private int          numentries;
        private final int          tablesizemask;
        //
        //

        public idHashTable() {
            final int newtablesize = 256;

            this.tablesize = newtablesize;
            assert (this.tablesize > 0);

            this.heads = Stream.generate(hashnode_s::new).limit(this.tablesize).toArray(hashnode_s[]::new);//	memset( heads, 0, sizeof( *heads ) * tablesize );
            this.numentries = 0;

            this.tablesizemask = this.tablesize - 1;
        }

        public idHashTable(int newtablesize) {

            assert (idMath.IsPowerOfTwo(newtablesize));

            this.tablesize = newtablesize;
            assert (this.tablesize > 0);

            this.heads = Stream.generate(hashnode_s::new).limit(this.tablesize).toArray(hashnode_s[]::new);//	memset( heads, 0, sizeof( *heads ) * tablesize );

            this.numentries = 0;

            this.tablesizemask = this.tablesize - 1;
        }

        public idHashTable(final idHashTable<Type> map) {
            int i;
            hashnode_s<Type> node;
            int prev;

            assert (map.tablesize > 0);

            this.tablesize = map.tablesize;
            this.heads = new hashnode_s[this.tablesize];
            this.numentries = map.numentries;
            this.tablesizemask = map.tablesizemask;

            for (i = 0; i < this.tablesize; i++) {
                if (null == map.heads[i]) {
                    this.heads[i] = null;
                    continue;
                }

//                prev = heads[i];
                prev = 0;
                for (node = map.heads[i + prev]; node != null; node = node.next) {
                    map.heads[i + prev] = new hashnode_s(node.key, node.value, null);//TODO:ECHKECE
//                    prev = prev.next;
                    prev++;
                }
            }
        }
//public					~idHashTable( void );
//
//					// returns total size of allocated memory
//public	size_t			Allocated( void ) const;
//					// returns total size of allocated memory including size of hash table type
//public	size_t			Size( void ) const;
//

        public void Set(final String key, Type value) {
            hashnode_s<Type> node;
            hashnode_s<Type> nextPtr;
            int hash, s;

            hash = GetHash(key);
            for (nextPtr = this.heads[hash], node = nextPtr; node != null; nextPtr = node.next, node = nextPtr) {//TODO:what moves us?
                s = node.key.Cmp(key);
                if (s == 0) {
                    node.value = value;
                    return;
                }
                if (s > 0) {
                    break;
                }
            }

            this.numentries++;

            nextPtr = new hashnode_s(key, value, this.heads[hash]);
            nextPtr.next = node;
        }

        public boolean Get(final String key) {
            return Get(key, null);
        }

        public boolean Get(final String key, Type[] value) {
            hashnode_s<Type> node;
            int hash, s;

            hash = GetHash(key);
            for (node = this.heads[hash]; node != null; node = node.next) {
                s = node.key.Cmp(key);
                if (s == 0) {
                    if (value != null) {
                        value[0] = (Type) node.value;
                    }
                    return true;
                }
                if (s > 0) {
                    break;
                }
            }

            if (value != null) {
                value[0] = null;
            }

            return false;
        }

        public boolean Remove(final String key) {
            hashnode_s<Type> head;
            hashnode_s<Type> node;
            hashnode_s<Type> prev;
            int hash;

            hash = GetHash(key);
            head = this.heads[hash];
            if (head != null) {
                for (prev = null, node = head; node != null; prev = node, node = node.next) {//TODO:fuck me if any of this shit works.
                    if (node.key.Cmp(key) != 0) {
                        if (prev != null) {
                            prev.next = node.next;
                        } else {
                            this.heads[hash] = node.next;//TODO:double check these pointers.
                        }

//				delete node;
                        this.numentries--;
                        return true;
                    }
                }
            }

            return false;
        }

        public void Clear() {
            int i;
            //hashnode_s<Type> node;
            hashnode_s<Type> next;

            for (i = 0; i < this.tablesize; i++) {
                next = this.heads[i];
                while (next != null) {
                    //node = next;
                    next = next.next;
//			delete node;
                }

                this.heads[i] = null;
            }

            this.numentries = 0;
        }

        public void DeleteContents() {
            int i;
            //hashnode_s<Type> node;
            hashnode_s<Type> next;

            for (i = 0; i < this.tablesize; i++) {
                next = this.heads[i];
                while (next != null) {
                    //node = next;
                    next = next.next;
//			delete node->value;
//			delete node;
                }

                this.heads[i] = null;
            }

            this.numentries = 0;
        }

        // the entire contents can be itterated over, but note that the
        // exact index for a given element may change when new elements are added
        public int Num() {
            return this.numentries;
        }

        /*
         ================
         idHashTable<Type>::GetIndex

         the entire contents can be itterated over, but note that the
         exact index for a given element may change when new elements are added
         ================
         */
        public Type GetIndex(int index) {
            hashnode_s<Type> node;
            int count;
            int i;

            if ((index < 0) || (index > this.numentries)) {
                assert (false);
                return null;
            }

            count = 0;
            for (i = 0; i < this.tablesize; i++) {
                for (node = this.heads[i]; node != null; node = node.next) {
                    if (count == index) {
                        return (Type) node.value;
                    }
                    count++;
                }
            }

            return null;
        }

        public int GetSpread() {
            int i, average, error, e;
            hashnode_s<Type> node;

            // if no items in hash
            if (0 == this.numentries) {
                return 100;
            }
            average = this.numentries / this.tablesize;
            error = 0;
            for (i = 0; i < this.tablesize; i++) {
                int numItems = 0;
                for (node = this.heads[i]; node != null; node = node.next) {
                    numItems++;
                }
                e = Math.abs(numItems - average);
                if (e > 1) {
                    error += e - 1;
                }
            }
            return 100 - ((error * 100) / this.numentries);
        }

        private class hashnode_s<type> {

            idStr      key;
            Type       value;
            hashnode_s<Type> next;
            //
            //

            public hashnode_s() {
                this.key = new idStr();
            }

            hashnode_s(final idStr k, Type v, hashnode_s<Type> n) {
                this.key = new idStr(k);
                this.value = v;
                this.next = n;
            }

            hashnode_s(final String k, Type v, hashnode_s<Type> n) {
                this(new idStr(k), v, n);
            }
        }

        int GetHash(final String key) {
            return (idStr.Hash(key) & this.tablesizemask);
        }
    }
}
