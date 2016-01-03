package neo.idlib.containers;

import com.rits.cloning.Cloner;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import neo.TempDump.CPP_class;
import static neo.TempDump.NOT;
import static neo.TempDump.reflects._Minus;
import neo.framework.CVarSystem;
import neo.framework.CVarSystem.idInternalCVar;
import neo.framework.CmdSystem;
import neo.framework.CmdSystem.commandDef_s;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.StrList.idStrPtr;
import neo.idlib.containers.StrPool.idPoolStr;

/**
 *
 */
public class List {

    /*
     ===============================================================================

     List template
     Does not allocate memory until the first item is added.

     ===============================================================================
     */
    public static class idList<type> {//TODO: implement java.util.List

        public static final int SIZE = Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + CPP_class.Pointer.SIZE;//type

        protected int num;
        private   int size;
        protected int granularity = 16;
        private type[]      list;
        private Class<type> type;
        private static final Cloner CLONER = new Cloner();//TODO:
        //
        private static int DBG_counter = 0;
        private final  int DBG_count   = DBG_counter++;
        //

//public	typedef int		cmp_t( const type *, const type * );
//public	typedef type	new_t( );
//
        public idList() {
            //            this(16);//disabled to prevent inherited constructors from calling the overridden clear function.
        }

        public idList(Class<type> type) {
            this();
            this.type = type;
        }

        public idList(int newgranularity) {
            assert (newgranularity > 0);

            list = null;
            granularity = newgranularity;
            Clear();
        }

        public idList(int newgranularity, Class<type> type) {
            this(newgranularity);
            this.type = type;
        }

        public idList(final idList<type> other) {
            list = null;
            this.oSet(other);
        }
//public					~idList<type>( );
//

        /*
         ================
         idList<type>::Clear

         Frees up the memory allocated by the list.  Assumes that type automatically handles freeing up memory.
         ================
         */
        public void Clear() {										// clear the list
//            if (list) {
//                delete[] list;
//            }

            list = null;
            num = 0;
            size = 0;
        }

        /*
         ================
         idList<type>::Num

         Returns the number of elements currently contained in the list.
         Note that this is NOT an indication of the memory allocated.
         ================
         */
        public int Num() {									// returns number of elements in list
            return num;
        }

        /*
         ================
         idList<type>::NumAllocated

         Returns the number of elements currently allocated for.
         ================
         */
        public int NumAllocated() {							// returns number of elements allocated for
            return size;
        }

        /*
         ================
         idList<type>::SetGranularity

         Sets the base size of the array and resizes the array to match.
         ================
         */
        public void SetGranularity(int newgranularity) {			// set new granularity
            int newsize;

            assert (newgranularity > 0);
            granularity = newgranularity;

            if (list != null) {
                // resize it to the closest level of granularity
                newsize = num + granularity - 1;
                newsize -= newsize % granularity;
                if (newsize != size) {
                    Resize(newsize);
                }
            }
        }

        /*
         ================
         idList<type>::GetGranularity

         Get the current granularity.
         ================
         */
        public int GetGranularity() {						// get the current granularity
            return granularity;
        }
//

        /*
         ================
         idList<type>::Allocated

         return total memory allocated for the list in bytes, but doesn't take into account additional memory allocated by type
         ================
         */
        public int Allocated() {						// returns total size of allocated memory
            return size;
        }

        public /*size_t*/ int Size() {						// returns total size of allocated memory including size of list type
            return Allocated();
        }

        public /*size_t*/ int MemoryUsed() {					// returns size of the used elements in the list

            return num /* sizeof( *list )*/;
        }

        /*
         ================
         idList<type>::operator=

         Copies the contents and size attributes of another list.
         ================
         */
        public idList<type> oSet(final idList<type> other) {
            int i;

            Clear();

            this.num = other.num;
            this.size = other.size;
            this.granularity = other.granularity;
            this.type = other.type;

            if (this.size != 0) {
                this.list = (type[]) new Object[size];
                for (i = 0; i < this.num; i++) {
                    this.list[i] = other.list[i];
                }
            }

            return this;
        }

        /*
         ================
         idList<type>::operator[] const

         Access operator.  Index must be within range or an assert will be issued in debug builds.
         Release builds do no range checking.
         ================
         */
        public type oGet(int index) {
            assert (index >= 0);
            assert (index < num);
            
            return list[index];
        }
//public	type &			operator[]( int index );
//
//        public type oSet(int index, type value) {
//            assert (index >= 0);
//            assert (index < num);
//
//            return list[index] = value;
//        }

        public type oSet(int index, Object value) {
            assert (index >= 0);
            assert (index < num);

            return list[index] = (type) value;
        }

        public type oPluSet(int index, type value) {
            assert (index >= 0);
            assert (index < num);

//            if (list[index] instanceof Double) {
//                return list[index] = (type) (Object) ((Double) list[index] + (Double) value);//TODO:test thsi shit
//            }
//            if (list[index] instanceof Float) {
//                return list[index] = (type) (Object) ((Float) list[index] + (Float) value);//TODO:test thsi shit
//            }
//            if (list[index] instanceof Integer) {
            return list[index] = (type) (Object) (((Number) list[index]).doubleValue() + ((Number) value).doubleValue());//TODO:test thsi shit
//            }
        }
//

        /*
         ================
         idList<type>::Condense

         Resizes the array to exactly the number of elements it contains or frees up memory if empty.
         ================
         */
        public void Condense() {									// resizes list to exactly the number of elements it contains
            if (list != null) {
                if (num != 0) {
                    Resize(num);
                } else {
                    Clear();
                }
            }
        }

        /*
         ================
         idList<type>::Resize

         Allocates memory for the amount of elements requested while keeping the contents intact.
         Contents are copied using their = operator so that data is correnctly instantiated.
         ================
         */
        public void Resize(int newsize) {								// resizes list to the given number of elements
            type[] temp;
            int i;

            assert (newsize >= 0);

            // free up the list if no data is being reserved
            if (newsize <= 0) {
                Clear();
                return;
            }

            if (newsize == size) {
                // not changing the size, so just exit
                return;
            }

            temp = list;
            size = newsize;
            if (size < num) {
                num = size;
            }

            // copy the old list into our new one
            list = (type[]) new Object[size];
            for (i = 0; i < num; i++) {
                list[i] = temp[i];
            }

            // delete the old list if it exists
//	if ( temp ) {
//		delete[] temp;
//	}
        }

        /*
         ================
         idList<type>::Resize

         Allocates memory for the amount of elements requested while keeping the contents intact.
         Contents are copied using their = operator so that data is correnctly instantiated.
         ================
         */
        public void Resize(int newsize, int newgranularity) {			// resizes list and sets new granularity
            type[] temp;
            int i;

            assert (newsize >= 0);

            assert (newgranularity > 0);
            granularity = newgranularity;

            // free up the list if no data is being reserved
            if (newsize <= 0) {
                Clear();
                return;
            }

            temp = list;
            size = newsize;
            if (size < num) {
                num = size;
            }

            // copy the old list into our new one
            list = (type[]) new Object[size];
            for (i = 0; i < num; i++) {
                list[i] = temp[i];
            }

            // delete the old list if it exists
//	if ( temp ) {
//		delete[] temp;
//	}
        }

        public void SetNum(int newnum) {			// set number of elements in list and resize to exactly this number if necessary
            SetNum(newnum, true);
        }

        /*
         ================
         idList<type>::SetNum

         Resize to the exact size specified irregardless of granularity
         ================
         */
        public void SetNum(int newnum, boolean resize) {			// set number of elements in list and resize to exactly this number if necessary
            assert (newnum >= 0);
            if (resize || newnum > size) {
                Resize(newnum);
            }
            num = newnum;
        }

        /*
         ================
         idList<type>::AssureSize

         Makes sure the list has at least the given number of elements.
         ================
         */
        public void AssureSize(int newSize) {							// assure list has given number of elements, but leave them uninitialized
            int newNum = newSize;

            if (newSize > size) {

                if (granularity == 0) {	// this is a hack to fix our memset classes
                    granularity = 16;
                }

                newSize += granularity - 1;
                newSize -= newSize % granularity;
                Resize(newSize);
            }

            num = newNum;
        }

        /*
         ================
         idList<type>::AssureSize

         Makes sure the list has at least the given number of elements and initialize any elements not yet initialized.
         ================
         */
        public void AssureSize(int newSize, final type initValue) {	// assure list has given number of elements and initialize any new elements
            int newNum = newSize;

            if (newSize > size) {

                if (granularity == 0) {	// this is a hack to fix our memset classes
                    granularity = 16;
                }

                newSize += granularity - 1;
                newSize -= newSize % granularity;
                num = size;
                Resize(newSize);

                for (int i = num; i < newSize; i++) {
                    list[i] = initValue;
                }
            }

            num = newNum;
        }

        /*
         ================
         idList<type>::AssureSizeAlloc

         Makes sure the list has at least the given number of elements and allocates any elements using the allocator.

         NOTE: This function can only be called on lists containing pointers. Calling it
         on non-pointer lists will cause a compiler error.
         ================
         */
        public void AssureSizeAlloc(int newSize, /*new_t*/ Class allocator) {	// assure the pointer list has the given number of elements and allocate any new elements
            int newNum = newSize;

            if (newSize > size) {

                if (granularity == 0) {	// this is a hack to fix our memset classes
                    granularity = 16;
                }

                newSize += granularity - 1;
                newSize -= newSize % granularity;
                num = size;
                Resize(newSize);

                for (int i = num; i < newSize; i++) {
                    try {
                        list[i] = /*( * allocator) ()*/ (type) allocator.newInstance();//TODO: check if any of this is necessary?
                    } catch (InstantiationException | IllegalAccessException ex) {
                        Logger.getLogger(List.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            num = newNum;
        }
//

        /*
         ================
         idList<type>::Ptr

         Returns a pointer to the begining of the array.  Useful for iterating through the list in loops.

         Note: may return NULL if the list is empty.

         FIXME: Create an iterator template for this kind of thing.
         ================
         */
        @Deprecated
        public type[] Ptr() {										// returns a pointer to the list
            return list;
        }
        
        public <T extends Object> T[] Ptr(final Class<? extends T[]> type) {										// returns a pointer to the list
            return Arrays.copyOf(this.list, this.num, type);
        }
//public	const type *	Ptr( ) const;									// returns a pointer to the list

        /*
         ================
         idList<type>::Alloc

         Returns a reference to a new data element at the end of the list.
         ================
         */
        public type Alloc() {									// returns reference to a new data element at the end of the list
            if (NOT(list)) {
                Resize(granularity);
            }

            if (num == size) {
                Resize(size + granularity);
            }
            try {
                return list[num++] = type.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
//                Logger.getLogger(List.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null;
        }

        /*
         ================
         idList<type>::Append

         Increases the size of the list by one element and copies the supplied data into it.

         Returns the index of the new element.
         ================
         */
        public int Append(final type obj) {// append element
            if (NOT(list)) {
                Resize(granularity);
            }

            if (num == size) {
                int newsize;

                if (granularity == 0) {	// this is a hack to fix our memset classes
                    granularity = 16;
                }
                newsize = size + granularity;
                Resize(newsize - newsize % granularity);
            }

            list[num] = obj;
            num++;

            return num - 1;
        }

        /**
         * Appends a shallow copy of the object.
         *
         * Because we have a mix of const refs and member pointers that we don't
         * want to rewrite in java, we will copy the const refs where necessary.
         * @see Append(type)
         */
        public int AppendClone(final type obj) {
            return this.Append(CLONER.shallowClone(obj));//TODO:create copy constructors instead of reflecting.
        }

        /*
         ================
         idList<type>::Append

         adds the other list to this one

         Returns the size of the new combined list
         ================
         */
        public int Append(final idList<type> other) {				// append list
            if (NOT(list)) {
                if (granularity == 0) {	// this is a hack to fix our memset classes
                    granularity = 16;
                }
                Resize(granularity);
            }

            int n = other.Num();
            for (int i = 0; i < n; i++) {
                Append(other.oGet(i));
            }

            return Num();
        }

        /*
         ================
         idList<type>::AddUnique

         Adds the data to the list if it doesn't already exist.  Returns the index of the data in the list.
         ================
         */
        public int AddUnique(final type obj) {			// add unique element
            int index;

            index = FindIndex(obj);
            if (index < 0) {
                index = Append(obj);
            }

            return index;
        }

        public int Insert(final type obj) {			// insert the element at the given index
            return Insert(obj, 0);
        }

        /*
         ================
         idList<type>::Insert

         Increases the size of the list by at leat one element if necessary 
         and inserts the supplied data into it.

         Returns the index of the new element.
         ================
         */
        public int Insert(final type obj, int index) {			// insert the element at the given index
            if (NOT(list)) {
                Resize(granularity);
            }

            if (num == size) {
                int newsize;

                if (granularity == 0) {	// this is a hack to fix our memset classes
                    granularity = 16;
                }
                newsize = size + granularity;
                Resize(newsize - newsize % granularity);
            }

            if (index < 0) {
                index = 0;
            } else if (index > num) {
                index = num;
            }
            for (int i = num; i > index; --i) {
                list[i] = list[i - 1];
            }
            num++;
            list[index] = obj;
            return index;
        }

        /*
         ================
         idList<type>::FindIndex

         Searches for the specified data in the list and returns it's index.  Returns -1 if the data is not found.
         ================
         */
        public int FindIndex(final type obj) {				// find the index for the given element
            int i;

            for (i = 0; i < num; i++) {
                if (Objects.equals(list[i], obj)) {
                    return i;
                }
            }

            // Not found
            return -1;
        }

        /*
         ================
         idList<type>::Find

         Searches for the specified data in the list and returns it's address. Returns NULL if the data is not found.
         ================
         */
        public Integer Find(final type obj) {						// find pointer to the given element
            int i;

            i = FindIndex(obj);
            if (i >= 0) {
                return i;//TODO:test whether returning the index instead of the address works!!!
            }

            return null;
        }

        /*
         ================
         idList<type>::FindNull

         Searches for a NULL pointer in the list.  Returns -1 if NULL is not found.

         NOTE: This function can only be called on lists containing pointers. Calling it
         on non-pointer lists will cause a compiler error.
         ================
         */
        public int FindNull() {								// find the index for the first NULL pointer in the list
            int i;

            for (i = 0; i < num; i++) {
                if (NOT(list[i])) {
                    return i;
                }
            }

            // Not found
            return -1;
        }

        /*
         ================
         idList<type>::IndexOf

         Takes a pointer to an element in the list and returns the index of the element.
         This is NOT a guarantee that the object is really in the list. 
         Function will assert in debug builds if pointer is outside the bounds of the list,
         but remains silent in release builds.
         ================
         */
        public int IndexOf(final type objptr) {					// returns the index for the pointer to an element in the list
            int index;

//            index = objptr - list;
            index = FindIndex(objptr);

            assert (index >= 0);
            assert (index < num);

            return index;
        }

        /*
         ================
         idList<type>::RemoveIndex

         Removes the element at the specified index and moves all data following the element down to fill in the gap.
         The number of elements in the list is reduced by one.  Returns false if the index is outside the bounds of the list.
         Note that the element is not destroyed, so any memory used by it may not be freed until the destruction of the list.
         ================
         */
        public boolean RemoveIndex(int index) {							// remove the element at the given index
            int i;

            assert (list != null);
            assert (index >= 0);
            assert (index < num);

            if ((index < 0) || (index >= num)) {
                return false;
            }

            num--;
            for (i = index; i < num; i++) {
                list[i] = list[i + 1];
            }

            return true;
        }

        /*
         ================
         idList<type>::Remove

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

        /*
         ================
         idList<type>::Sort

         Performs a qsort on the list using the supplied comparison function.  Note that the data is merely moved around the
         list, so any pointers to data within the list may no longer be valid.
         ================
         */
        public void Sort() {
            if (NOT(list)) {
                return;
            }

            if (list[0] instanceof idStr
                    || list[0] instanceof idPoolStr
                    || list[0] instanceof idStrPtr) {
                this.Sort(new StrList.idListSortCompare());

            } else if (list[0] instanceof idInternalCVar) {
                this.Sort(new CVarSystem.idListSortCompare());

            } else if (list[0] instanceof commandDef_s) {
                this.Sort(new CmdSystem.idListSortCompare());

            } else {
                this.Sort(new idListSortCompare<type>());
            }
        }

        public void Sort(cmp_t compare /*= ( cmp_t * )&idListSortCompare<type> */) {

//	typedef int cmp_c(const void *, const void *);
//
//	cmp_c *vCompare = (cmp_c *)compare;
//	qsort( ( void * )list, ( size_t )num, sizeof( type ), vCompare );
            if (list != null) {
                Arrays.sort(list, compare);
            }
        }


        /*
         ================
         idList<type>::SortSubSection

         Sorts a subsection of the list.
         ================
         */
        public void SortSubSection(int startIndex, int endIndex) {
            this.SortSubSection(startIndex, endIndex, new idListSortCompare<type>());
        }

        public void SortSubSection(int startIndex, int endIndex, cmp_t compare /*= ( cmp_t * )&idListSortCompare<type>*/) {
            if (NOT(list)) {
                return;
            }
            if (startIndex < 0) {
                startIndex = 0;
            }
            if (endIndex >= num) {
                endIndex = num - 1;
            }
            if (startIndex >= endIndex) {
                return;
            }
//	typedef int cmp_c(const void *, const void *);
//
//	cmp_c *vCompare = (cmp_c *)compare;
//	qsort( ( void * )( &list[startIndex] ), ( size_t )( endIndex - startIndex + 1 ), sizeof( type ), vCompare );
            Arrays.sort(list, startIndex, endIndex, compare);
        }

        /*
         ================
         idList<type>::Swap

         Swaps the contents of two lists
         ================
         */
        public void Swap(idList<type> other) {						// swap the contents of the lists
            final int swap_num, swap_size, swap_granularity;
            final type[] swap_list;

            swap_num = num;
            swap_size = size;
            swap_granularity = granularity;
            swap_list = list;

            num = other.num;
            size = other.size;
            granularity = other.granularity;
            list = other.list;

            other.num = swap_num;
            other.size = swap_size;
            other.granularity = swap_granularity;
            other.list = swap_list;
        }

        /*
         ================
         idList<type>::DeleteContents

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

            for (i = 0; i < num; i++) {
//		delete list[i ];
                list[i] = null;
            }

            if (clear) {
                Clear();
            } else {
//		memset( list, 0, size * sizeof( type ) );
                list = (type[]) new Object[list.length];
            }
        }
    };

//    @Deprecated
//    public static <T> void idSwap(T a, T b) {
//        T c = a;
//        a = b;
//        b = c;
//    }
//    
    public static void idSwap(final int[] array, final int p1, final int p2) {
        idSwap(array, array, p1, p2);
    }

    public static void idSwap(final int[] a1, final int[] a2, final int p1, final int p2) {
        final int c = a1[p1];
        a1[p1] = a2[p2];
        a2[p2] = c;
    }

    public static void idSwap(final Object[] a1, final int p1, final Object[] a2, final int p2) {
        final Object c = a1[p1];
        a1[p1] = a2[p2];
        a2[p2] = c;
    }

    public static void idSwap(final int[][] a1, final int p11, final int p12, final int[][] a2, final int p21, final int p22) {
        final int c = a1[p11][p12];
        a1[p11][12] = a2[p21][p22];
        a2[p21][p22] = c;
    }

    public static void idSwap(final float[] a1, final float[] a2, final int p1, final int p2) {
        final float c = a1[p1];
        a1[p1] = a2[p2];
        a2[p2] = c;
    }

    public static void idSwap(final float[][] a1, final float[][] a2, final int p1, final int p2) {
        final float[] c = a1[p1];
        a1[p1] = a2[p2];
        a2[p2] = c;
    }

    public static void idSwap(final float[] a, final float[] b) {
        final int length = a.length;
        final float[] c = new float[length];

        System.arraycopy(a, 0, c, 0, length);
        System.arraycopy(b, 0, a, 0, length);
        System.arraycopy(c, 0, b, 0, length);
    }

    public static void idSwap(final Float[] a, final Float[] b) {
        final int length = a.length;
        final Float[] c = new Float[length];

        System.arraycopy(a, 0, c, 0, length);
        System.arraycopy(b, 0, a, 0, length);
        System.arraycopy(c, 0, b, 0, length);
    }

    private static class idListSortCompare<type> implements cmp_t<type> {

        @Override
        public int compare(final type a, final type b) {
            return (int) _Minus(a, b);
        }
    }

    public static interface cmp_t<type> extends Comparator<type> {
    }
}
