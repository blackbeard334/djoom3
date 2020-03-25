package neo.idlib.containers;

import static neo.TempDump.NOT;
import static neo.TempDump.reflects._Minus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import neo.TempDump;
import neo.TempDump.CPP_class;
import neo.framework.CVarSystem;
import neo.framework.CVarSystem.idInternalCVar;
import neo.framework.CmdSystem;
import neo.framework.CmdSystem.commandDef_s;
import neo.idlib.Text.Str.idStr;
//import neo.idlib.containers.StrList.idStrPtr;
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

        // the number of elements currently contained in the array.
        private int num;
        // the number of elements currently allocated for.
        private int size;
        private int granularity = 16;
        private type[]      array;
        private Class<type> type;
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
            this.setType(type);
        }

        public idList(int newgranularity) {
            assert (newgranularity > 0);

            this.setArray(null);
            this.setGranularity(newgranularity);
            Clear();
        }

        idList(int newgranularity, Class<type> type) {
            this(newgranularity);
            this.setType(type);
        }

//        private idList(final idList<type> other) {
//            this.setArray(null); // necessary ???
//            this.oSet(other);
//        }

//public					~idList<type>( );
//

		private type[] getArray() {
			return array;
		}

    	private type getArrayType(int index) {
    		return getArray()[index];
    	}

		private void setArray(type[] array) {
			this.array = array;
		}

    	private type setArrayType(int index, type element) {
    		return this.getArray()[index] = element;
    	}

		private int getSize() {
			return size;
		}


		private void setSize(int size) {
			this.size = size;
		}


		private int getGranularity() {
			return granularity;
		}


		private void setGranularity(int granularity) {
			this.granularity = granularity;
		}

		private Class<type> getType() {
			return type;
		}


		private void setType(Class<type> type) {
			this.type = type;
		}


		private int getNum() {
			return num;
		}


		private void setNum(int num) {
			this.num = num;
		}

		@SuppressWarnings("unchecked")
		private type[] castArrayType(ArrayList<type> types) {
			return (type[]) types.toArray();
		}

		private ArrayList<type> generateArray(int size) {
			ArrayList<type> types = new ArrayList<type>();
			addInitializedValues(types, size);
			return types;
		}

		private ArrayList<type> addInitializedValues(ArrayList<type> types, int size) {
			for (int i = 0; i < size; i++) {
				types.add(instantiateType());// TODO: check if any of this is necessary?
			}
			return types;
		}

		private type instantiateType() {
			return instantiateType(type);
		}

		private type instantiateType(Class<?> type) {
			if (type != null) {
	            try {
	            	@SuppressWarnings("unchecked")
					type instance = (type) type.newInstance();
					return instance;//TODO: check if any of this is necessary?
	            } catch (InstantiationException | IllegalAccessException ex) {
	                Logger.getLogger(List.class.getName()).log(Level.SEVERE, "could not initialize " + ((type==null) ? "null" : type), ex);
	            }
			}
            return null;
		}

		private void adjustArray() {
            if (NOT(getArray())) {
                Resize(getGranularity());
            }

            if (Num() == getSize()) {
                int newsize;

                if (getGranularity() == 0) {	// this is a hack to fix our memset classes
                	this.setGranularity(16);
                }
                newsize = getSize() + getGranularity();
                Resize(newsize - newsize % getGranularity());
            }
		}

        /*
         ================
         idList<type>::Clear

         Frees up the memory allocated by the array.  Assumes that type automatically handles freeing up memory.
         ================
         */
        public void Clear() {										// clear the array
//            if (array) {
//                delete[] array;
//            }

        	this.setArray(null);
        	this.setNum(0);
        	this.setSize(0);
        }

        /*
         ================
         idList<type>::Num

         Returns the number of elements currently contained in the array.
         Note that this is NOT an indication of the memory allocated.
         ================
         */
        public int Num() {									// returns number of elements in array
            return getNum();
        }

        /*
         ================
         idList<type>::NumAllocated

         Returns the number of elements currently allocated for.
         ================
         */
//        public int NumAllocated() {							// returns number of elements allocated for
//            return size;
//        }

        /*
         ================
         idList<type>::SetGranularity

         Sets the base size of the array and resizes the array to match.
         ================
         */
        public void SetGranularity(int newgranularity) {			// set new granularity
            int newsize;

            assert (newgranularity > 0);
            this.setGranularity(newgranularity);

            if (getArray() != null) {
                // resize it to the closest level of granularity
                newsize = Num() + getGranularity() - 1;
                newsize -= newsize % getGranularity();
                if (newsize != getSize()) {
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
            return getGranularity();
        }
//

        /*
         ================
         idList<type>::Allocated

         return total memory allocated for the array in bytes, but doesn't take into account additional memory allocated by type
         ================
         */
        public int Allocated() {						// returns total size of allocated memory
            return getSize();
        }

        public /*size_t*/ int Size() {						// returns total size of allocated memory including size of array type
            return size;
        }

        public /*size_t*/ int MemoryUsed() {					// returns size of the used elements in the array

            return Num() /* sizeof( *array )*/;
        }

        /*
         ================
         idList<type>::operator=

         Copies the contents and size attributes of another array.
         ================
         */
        public idList<type> oSet(final idList<type> other) {
            int i;

            Clear();

            this.setNum(other.getNum());
            this.setSize(other.getSize());
            this.setGranularity(other.getGranularity());
            this.setType(other.getType());

            if (this.getSize() != 0) {
                this.setArray(castArrayType(generateArray(getSize())));
                for (i = 0; i < this.Num(); i++) {
                    this.getArray()[i] = other.getArray()[i];
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
            assert (index < Num());
            
            return getArrayType(index);
        }
//public	type &			operator[]( int index );
//
//        public type oSet(int index, type value) {
//            assert (index >= 0);
//            assert (index < num);
//
//            return array[index] = value;
//        }

        /**
         * @deprecated use {@link idList}#oSet(int index, type value) instead
         */
        @SuppressWarnings("unchecked")
		public type oSetType(int index, Object value) {
        	return oSet(index, (type) value);
        }

        public type oSet(int index, type value) {
            assert (index >= 0);
            assert (index < Num());

            return setArrayType(index, (type) value);
        }

        public type oPluSet(int index, type value) {
            assert (index >= 0);
            assert (index < Num());

//            if (array[index] instanceof Double) {
//                return array[index] = (type) (Object) ((Double) array[index] + (Double) value);//TODO:test thsi shit
//            }
//            if (array[index] instanceof Float) {
//                return array[index] = (type) (Object) ((Float) array[index] + (Float) value);//TODO:test thsi shit
//            }
//            if (array[index] instanceof Integer) {
            @SuppressWarnings("unchecked")
            type element = (type) (Object) (((Number) getArrayType(index)).doubleValue() + ((Number) value).doubleValue());//TODO:test thsi shit
            this.setArrayType(index, element);
            return element;
//            }
        }
//

        /*
         ================
         idList<type>::Condense

         Resizes the array to exactly the number of elements it contains or frees up memory if empty.
         ================
         */
        public void Condense() {									// resizes array to exactly the number of elements it contains
            if (getArray() != null) {
                if (Num() != 0) {
                    Resize(Num());
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
        public void Resize(int newsize) {								// resizes array to the given number of elements
            type[] temp;
            int i;

            assert (newsize >= 0);

            // free up the array if no data is being reserved
            if (newsize <= 0) {
                Clear();
                return;
            }

            if (newsize == getSize()) {
                // not changing the size, so just exit
                return;
            }

            temp = getArray();
            this.setSize(newsize);
            if (newsize < Num()) {
            	this.setNum(newsize);
            }

            // copy the old array into our new one
            this.setArray(castArrayType(generateArray(newsize)));
            for (i = 0; i < Num(); i++) {
            	getArray()[i] = temp[i];
            }

            // delete the old array if it exists
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
        public void Resize(int newsize, int newgranularity) {			// resizes array and sets new granularity
            type[] temp;
            int i;

            assert (newsize >= 0);

            assert (newgranularity > 0);
            this.setGranularity(newgranularity);

            // free up the array if no data is being reserved
            if (newsize <= 0) {
                Clear();
                return;
            }

            temp = getArray();
            this.setSize(newsize);
            if (newsize < Num()) {
            	this.setNum(newsize);
            }

            // copy the old array into our new one
            this.setArray(castArrayType(generateArray(newsize)));
            for (i = 0; i < Num(); i++) {
            	getArray()[i] = temp[i];
            }

            // delete the old array if it exists
//	if ( temp ) {
//		delete[] temp;
//	}
        }

        public void SetNum(int newnum) {			// set number of elements in array and resize to exactly this number if necessary
            SetNum(newnum, true);
        }

        /*
         ================
         idList<type>::SetNum

         Resize to the exact size specified irregardless of granularity
         ================
         */
        public void SetNum(int newnum, boolean resize) {			// set number of elements in array and resize to exactly this number if necessary
            assert (newnum >= 0);
            if (resize || newnum > getSize()) {
                Resize(newnum);
            }
            this.setNum(newnum);
        }

        /*
         ================
         idList<type>::AssureSize

         Makes sure the array has at least the given number of elements.
         ================
         */
        public void AssureSize(int newSize) {							// assure array has given number of elements, but leave them uninitialized
            int newNum = newSize;

            if (newSize > getSize()) {

                if (getGranularity() == 0) {	// this is a hack to fix our memset classes
                	this.setGranularity(16);
                }

                newSize += getGranularity() - 1;
                newSize -= newSize % getGranularity();
                Resize(newSize);
            }

            this.setNum(newNum);
        }

        /*
         ================
         idList<type>::AssureSize

         Makes sure the array has at least the given number of elements and initialize any elements not yet initialized.
         ================
         */
        public void AssureSize(int newSize, final type initValue) {	// assure array has given number of elements and initialize any new elements
            int oldNum = Num();

            AssureSize(newSize);

            if (newSize > oldNum) {
                for (int i = oldNum; i < newSize; i++) {
                	getArray()[i] = initValue;
                }
            }
        }

        /*
         ================
         idList<type>::AssureSizeAlloc

         Makes sure the array has at least the given number of elements and allocates any elements using the allocator.

         NOTE: This function can only be called on lists containing pointers. Calling it
         on non-pointer lists will cause a compiler error.
         ================
         */
        public void AssureSizeAlloc(int newSize, /*new_t*/ Class<type> allocator) {	// assure the pointer array has the given number of elements and allocate any new elements
            int oldNum = Num();

            AssureSize(newSize);

            if (newSize > oldNum) {
                for (int i = oldNum; i < newSize; i++) {
                	getArray()[i] = /*( * allocator) ()*/ instantiateType(allocator);//TODO: check if any of this is necessary?
                }
            }

        }
//

        /*
         ================
         idList<type>::Ptr

         Returns a pointer to the begining of the array.  Useful for iterating through the array in loops.

         Note: may return NULL if the array is empty.

         FIXME: Create an iterator template for this kind of thing.
         ================
         */
        @Deprecated
        public type[] Ptr() {										// returns a pointer to the array
            return getArray();
        }

        public <T> T[] Ptr(final Class<? extends T[]> type) {
            if (this.Num() == 0)
                return null;
            
            // returns a pointer to the array
            return Arrays.copyOf(this.getArray(), this.getNum(), type);
        }
//public	const type *	Ptr( ) const;									// returns a pointer to the array

        /*
         ================
         idList<type>::Alloc

         Returns a reference to a new data element at the end of the array.
         ================
         */
        public type Alloc() {									// returns reference to a new data element at the end of the array
            if (NOT(getArray())) {
                Resize(getGranularity());
            }

            if (Num() == getSize()) {
                Resize(getSize() + getGranularity());
            }
            type instance = instantiateType();
            if (instance != null) {
            	this.setArrayType(this.num++, instance);
            	return instance;
            }

            return null;
        }

        /*
         ================
         idList<type>::Append

         Increases the size of the array by one element and copies the supplied data into it.

         Returns the index of the new element.
         ================
         */
        public int Append(final type obj) {// append element
        	adjustArray();

            getArray()[Num()] = obj;
            this.setNum(getNum()+1);

            return Num() - 1;
        }

        /**
         * Appends a shallow copy of the object.
         *
         * Because we have a mix of const refs and member pointers that we don't
         * want to rewrite in java, we will copy the const refs where necessary.
         * @see #Append(type)
         */
        public int AppendClone(final type obj) {
            return this.Append(TempDump.clone(obj));//TODO:create copy constructors instead of reflecting.
        }

        /*
         ================
         idList<type>::Append

         adds the other array to this one

         Returns the size of the new combined array
         ================
         */
        public int Append(final idList<type> other) {				// append array
            if (NOT(getArray())) {
                if (getGranularity() == 0) {	// this is a hack to fix our memset classes
                	this.setGranularity(16);
                }
                Resize(getGranularity());
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

         Adds the data to the array if it doesn't already exist.  Returns the index of the data in the array.
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

         Increases the size of the array by at leat one element if necessary 
         and inserts the supplied data into it.

         Returns the index of the new element.
         ================
         */
        public int Insert(final type obj, int index) {			// insert the element at the given index
        	adjustArray();

            if (index < 0) {
                index = 0;
            } else if (index > Num()) {
                index = Num();
            }
            for (int i = Num(); i > index; --i) {
            	getArray()[i] = getArray()[i - 1];
            }
            this.setNum(getNum()+1);
            this.setArrayType(index, obj);
            return index;
        }

        /*
         ================
         idList<type>::FindIndex

         Searches for the specified data in the array and returns it's index.  Returns -1 if the data is not found.
         ================
         */
        public int FindIndex(final type obj) {				// find the index for the given element
            int i;

            for (i = 0; i < Num(); i++) {
                if (Objects.equals(getArray()[i], obj)) {
                    return i;
                }
            }

            // Not found
            return -1;
        }

        /*
         ================
         idList<type>::Find

         Searches for the specified data in the array and returns it's address. Returns NULL if the data is not found.
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

         Searches for a NULL pointer in the array.  Returns -1 if NULL is not found.

         NOTE: This function can only be called on lists containing pointers. Calling it
         on non-pointer lists will cause a compiler error.
         ================
         */
        public int FindNull() {								// find the index for the first NULL pointer in the array
            int i;

            for (i = 0; i < Num(); i++) {
                if (NOT(getArray()[i])) {
                    return i;
                }
            }

            // Not found
            return -1;
        }

        /*
         ================
         idList<type>::IndexOf

         Takes a pointer to an element in the array and returns the index of the element.
         This is NOT a guarantee that the object is really in the array. 
         Function will assert in debug builds if pointer is outside the bounds of the array,
         but remains silent in release builds.
         ================
         */
        public int IndexOf(final type objptr) {					// returns the index for the pointer to an element in the array
            int index;

//            index = objptr - array;
            index = FindIndex(objptr);

            assert (index >= 0);
            assert (index < Num());

            return index;
        }

        /*
         ================
         idList<type>::RemoveIndex

         Removes the element at the specified index and moves all data following the element down to fill in the gap.
         The number of elements in the array is reduced by one.  Returns false if the index is outside the bounds of the array.
         Note that the element is not destroyed, so any memory used by it may not be freed until the destruction of the array.
         ================
         */
        public boolean RemoveIndex(int index) {							// remove the element at the given index
            int i;

            assert (getArray() != null);
            assert (index >= 0);
            assert (index < Num());

            if ((index < 0) || (index >= Num())) {
                return false;
            }

            this.setNum(getNum() - 1);
            for (i = index; i < Num(); i++) {
            	getArray()[i] = getArray()[i + 1];
            }

            return true;
        }

        /*
         ================
         idList<type>::Remove

         Removes the element if it is found within the array and moves all data following the element down to fill in the gap.
         The number of elements in the array is reduced by one.  Returns false if the data is not found in the array.  Note that
         the element is not destroyed, so any memory used by it may not be freed until the destruction of the array.
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

         Performs a qsort on the array using the supplied comparison function.  Note that the data is merely moved around the
         array, so any pointers to data within the array may no longer be valid.
         ================
         */
        public void Sort() {
            if (NOT(getArray())) {
                return;
            }

            if (getArray()[0] instanceof idStr
            		//|| getArray()[0] instanceof idStrPtr
                    || getArray()[0] instanceof idPoolStr) {
            	@SuppressWarnings("unchecked")
            	cmp_t<type> compare = (cmp_t<type>) new StrList.idListSortCompare();
            	this.Sort(compare);

            } else if (getArray()[0] instanceof idInternalCVar) {
            	@SuppressWarnings("unchecked")
            	cmp_t<type> compare = (cmp_t<type>) new CVarSystem.idListSortCompare();
                this.Sort(compare);

            } else if (getArray()[0] instanceof commandDef_s) {
            	@SuppressWarnings("unchecked")
            	cmp_t<type> compare = (cmp_t<type>) new CmdSystem.idListSortCompare();
                this.Sort(compare);

            } else {
                this.Sort(new idListSortCompare<type>());
            }
        }

        public void Sort(cmp_t<type> compare /*= ( cmp_t * )&idListSortCompare<type> */) {

//	typedef int cmp_c(const void *, const void *);
//
//	cmp_c *vCompare = (cmp_c *)compare;
//	qsort( ( void * )array, ( size_t )num, sizeof( type ), vCompare );
            if (getArray() != null) {
                Arrays.sort(getArray(), compare);
            }
        }


        /*
         ================
         idList<type>::SortSubSection

         Sorts a subsection of the array.
         ================
         */
//        public void SortSubSection(int startIndex, int endIndex) {
//            this.SortSubSection(startIndex, endIndex, new idListSortCompare<type>());
//        }
//
//        public void SortSubSection(int startIndex, int endIndex, cmp_t compare /*= ( cmp_t * )&idListSortCompare<type>*/) {
//            if (NOT(array)) {
//                return;
//            }
//            if (startIndex < 0) {
//                startIndex = 0;
//            }
//            if (endIndex >= Num()) {
//                endIndex = Num() - 1;
//            }
//            if (startIndex >= endIndex) {
//                return;
//            }
////	typedef int cmp_c(const void *, const void *);
////
////	cmp_c *vCompare = (cmp_c *)compare;
////	qsort( ( void * )( &array[startIndex] ), ( size_t )( endIndex - startIndex + 1 ), sizeof( type ), vCompare );
//            Arrays.sort(array, startIndex, endIndex, compare);
//        }

        /*
         ================
         idList<type>::Swap

         Swaps the contents of two lists
         ================
         */
        public void Swap(idList<type> other) {						// swap the contents of the lists
            final int swap_num, swap_size, swap_granularity;
            final type[] swap_list;

            swap_num = Num();
            swap_size = getSize();
            swap_granularity = getGranularity();
            swap_list = getArray();

            this.setNum(other.Num());
            this.setSize(other.getSize());
            this.setGranularity(other.getGranularity());
            this.setArray(other.getArray());

            other.setNum(swap_num);
            other.setSize(swap_size);
            other.setGranularity(swap_granularity);
            other.setArray(swap_list);
        }

        /*
         ================
         idList<type>::DeleteContents

         Calls the destructor of all elements in the array.  Conditionally frees up memory used by the array.
         Note that this only works on lists containing pointers to objects and will cause a compiler error
         if called with non-pointers.  Since the array was not responsible for allocating the object, it has
         no information on whether the object still exists or not, so care must be taken to ensure that
         the pointers are still valid when this function is called.  Function will set all pointers in the
         array to NULL.
         ================
         */
        public void DeleteContents(boolean clear) {						// delete the contents of the array
            int i;

            for (i = 0; i < Num(); i++) {
//		delete array[i ];
                getArray()[i] = null;
            }

            if (clear) {
                Clear();
            } else {
//		memset( array, 0, size * sizeof( type ) );
            	this.setArray(castArrayType(generateArray(array.length)));
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

    public static <T> void idSwap(final T[] a1, final T[] a2, final int p1, final int p2) {
        final T c = a1[p1];
        a1[p1] = a2[p2];
        a2[p2] = c;
    }

    public static void idSwap(final int[] array, final int p1, final int p2) {
        idSwap(array, array, p1, p2);
    }

    public static void idSwap(final int[] a1, final int[] a2, final int p1, final int p2) {
        final int c = a1[p1];
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
