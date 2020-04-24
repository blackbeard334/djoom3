package neo.idlib.containers;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class StaticList {

    /*
     ===============================================================================

     Static list template
     A non-growing, memset-able list using no memory allocation.

     ===============================================================================
     */
    public static class idStaticList<type> {

        private       int         num;
        private       type[]      list;
        private final int         size;
        private       Class<type> type;
        //
        //

        public idStaticList(int size) {
            this.num = 0;
            this.size = size;
            this.list = (type[]) new Object[size];
        }

        public idStaticList(int size, Class<type> type) {
            this(size);
            this.type = type;
        }

        public idStaticList(int size, Object object) {
            this(size);
            this.list = (type[])((idStaticList<type>) object).list;
        }
//	public					idStaticList( const idStaticList<type,size> &other );
//	public					~idStaticList<type,size>( void );
//

        /*
         ================
         idStaticList<type,size>::Clear

         Sets the number of elements in the list to 0.  Assumes that type automatically handles freeing up memory.
         ================
         */
        public void Clear() {										// marks the list as empty.  does not deallocate or intialize data.
            this.num = 0;
        }

        /*
         ================
         idStaticList<type,size>::Num

         Returns the number of elements currently contained in the list.
         ================
         */
        public int Num() {									// returns number of elements in list
            return this.num;
        }

        /*
         ================
         idStaticList<type,size>::Max

         Returns the maximum number of elements in the list.
         ================
         */
        public int Max() {									// returns the maximum number of elements in the list
            return this.size;
        }

        /*
         ================
         idStaticList<type,size>::SetNum

         Set number of elements in list.
         ================
         */
        public void SetNum(int newnum) {								// set number of elements in list
            assert (newnum >= 0);
            assert (newnum <= this.size);
            this.num = newnum;
        }
//
//public		size_t				Allocated( void ) const;							// returns total size of allocated memory
//public		size_t				Size( void ) const;									// returns total size of allocated memory including size of list type

        // returns size of the used elements in the list
        public int/*size_t*/ MemoryUsed() {
            return this.num * Integer.BYTES;//TODO: * sizeof(list[0]);
        }
//

        /*
         ================
         idStaticList<type,size>::operator[] const

         Access operator.  Index must be within range or an assert will be issued in debug builds.
         Release builds do no range checking.
         ================
         */
        public type oGet(int index) {
            assert (index >= 0);
            assert (index < this.num);

            return this.list[index];
        }

        public type oSet(int index, type value) {
            assert (index >= 0);
            assert (index < this.num);

            return this.list[index] = value;
        }
//public		type &				operator[]( int index );
//

        /*
         ================
         idStaticList<type,size>::Ptr

         Returns a pointer to the begining of the array.  Useful for iterating through the list in loops.

         Note: may return NULL if the list is empty.

         FIXME: Create an iterator template for this kind of thing.
         ================
         */
        public type[] Ptr() {										// returns a pointer to the list
            return this.list;
        }
//public		const type *		Ptr( void ) const;									// returns a pointer to the list

        /*
         ================
         idStaticList<type,size>::Alloc

         Returns a pointer to a new data element at the end of the list.
         ================
         */
        public type Alloc() {										// returns reference to a new data element at the end of the list.  returns NULL when full.
            if (this.num >= this.size) {
                return null;
            }
            try {
                return this.list[this.num++] = this.type.newInstance();//TODO:init value before sending back. EDIT:ugly, but working.
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(StaticList.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        /*
         ================
         idStaticList<type,size>::Append

         Increases the size of the list by one element and copies the supplied data into it.

         Returns the index of the new element, or -1 when list is full.
         ================
         */
        public int Append(final type obj) {// append element
            assert (this.num < this.size);
            if (this.num < this.size) {
                this.list[ this.num] = obj;
                this.num++;
                return this.num - 1;
            }

            return -1;
        }

        /*
         ================
         idStaticList<type,size>::Append

         adds the other list to this one

         Returns the size of the new combined list
         ================
         */
        public int Append(final idStaticList<type> other) {		// append list
            int i;
            int n = other.Num();

            if ((this.num + n) > other.size) {//TODO:which size??
                n = this.size - this.num;
            }
            for (i = 0; i < n; i++) {
                this.list[i + this.num] = other.list[i];
            }
            this.num += n;
            return Num();
        }

        /*
         ================
         idStaticList<type,size>::AddUnique

         Adds the data to the list if it doesn't already exist.  Returns the index of the data in the list.
         ================
         */
        public int AddUnique(final type obj) {						// add unique element
            int index;

            index = FindIndex(obj);
            if (index < 0) {
                index = Append(obj);
            }

            return index;
        }

        /*
         ================
         idStaticList<type,size>::Insert

         Increases the size of the list by at leat one element if necessary 
         and inserts the supplied data into it.

         Returns the index of the new element, or -1 when list is full.
         ================
         */
        public int Insert(final type obj, int index) {				// insert the element at the given index
            int i;

            assert (this.num < this.size);
            if (this.num >= this.size) {
                return -1;
            }

            assert (index >= 0);
            if (index < 0) {
                index = 0;
            } else if (index > this.num) {
                index = this.num;
            }

            for (i = this.num; i > index; --i) {
                this.list[i] = this.list[i - 1];
            }

            this.num++;
            this.list[index] = obj;
            return index;
        }

        /*
         ================
         idStaticList<type,size>::FindIndex

         Searches for the specified data in the list and returns it's index.  Returns -1 if the data is not found.
         ================
         */
        public int FindIndex(final type obj) {				// find the index for the given element
            int i;

            for (i = 0; i < this.num; i++) {
                if (this.list[i] == obj) {
                    return i;
                }
            }

            // Not found
            return -1;
        }
//public		type *				Find( type const & obj ) const;						// find pointer to the given element

        /*
         ================
         idStaticList<type,size>::FindNull

         Searches for a NULL pointer in the list.  Returns -1 if NULL is not found.

         NOTE: This function can only be called on lists containing pointers. Calling it
         on non-pointer lists will cause a compiler error.
         ================
         */
        public int FindNull() {								// find the index for the first NULL pointer in the list
            int i;

            for (i = 0; i < this.num; i++) {
                if (this.list[ i] == null) {
                    return i;
                }
            }

            // Not found
            return -1;
        }

        /*
         ================
         idStaticList<type,size>::IndexOf

         Takes a pointer to an element in the list and returns the index of the element.
         This is NOT a guarantee that the object is really in the list. 
         Function will assert in debug builds if pointer is outside the bounds of the list,
         but remains silent in release builds.
         ================
         */
        public int IndexOf(final type obj) {					// returns the index for the pointer to an element in the list
//    int index;
//
//	index = objptr - list;
//
//	assert( index >= 0 );
//	assert( index < num );
//
//	return index;
            return FindIndex(obj);
        }

        /*
         ================
         idStaticList<type,size>::RemoveIndex

         Removes the element at the specified index and moves all data following the element down to fill in the gap.
         The number of elements in the list is reduced by one.  Returns false if the index is outside the bounds of the list.
         Note that the element is not destroyed, so any memory used by it may not be freed until the destruction of the list.
         ================
         */
        public boolean RemoveIndex(int index) {							// remove the element at the given index
            int i;

            assert (index >= 0);
            assert (index < this.num);

            if ((index < 0) || (index >= this.num)) {
                return false;
            }

            this.num--;
            for (i = index; i < this.num; i++) {
                this.list[ i] = this.list[ i + 1];
            }

            return true;
        }

        /*
         ================
         idStaticList<type,size>::Remove

         Removes the element if it is found within the list and moves all data following the element down to fill in the gap.
         The number of elements in the list is reduced by one.  Returns false if the data is not found in the list.  Note that
         the element is not destroyed, so any memory used by it may not be freed until the destruction of the list.
         ================
         */
        public boolean Remove(final type obj) {							// remove the element
            int index;

            index = FindIndex(obj);
            if (index >= 0) {
                return RemoveIndex(index);
            }

            return false;
        }
//public		void				Swap( idStaticList<type,size> &other );				// swap the contents of the lists

        /*
         ================
         idStaticList<type,size>::DeleteContents

         Calls the destructor of all elements in the list.  Conditionally frees up memory used by the list.
         Note that this only works on lists containing pointers to objects and will cause a compiler error
         if called with non-pointers.  Since the list was not responsible for allocating the object, it has
         no information on whether the object still exists or not, so care must be taken to ensure that
         the pointers are still valid when this function is called.  Function will set all pointers in the
         list to NULL.
         ================
         */
        public void DeleteContents(boolean clear) {						// delete the contents of the list
            int i;

            for (i = 0; i < this.size; i++) {
//		delete list[ i ];
                this.list[i] = null;
            }

            if (clear) {
                Clear();
            } else {
//		memset( list, 0, sizeof( list ) );
                Arrays.fill(this.list, 0);
            }
        }

    }
}
