package neo.idlib;

/**
 *
 */
@Deprecated
public class Heap {

//===============================================================
//
//	idHeap
//
//===============================================================
//#define SMALL_HEADER_SIZE		( (int) ( sizeof( byte ) + sizeof( byte ) ) )
//#define MEDIUM_HEADER_SIZE		( (int) ( sizeof( mediumHeapEntry_s ) + sizeof( byte ) ) )
//#define LARGE_HEADER_SIZE		( (int) ( sizeof( dword * ) + sizeof( byte ) ) )
//
//#define ALIGN_SIZE( bytes )		( ( (bytes) + ALIGN - 1 ) & ~(ALIGN - 1) )
//#define SMALL_ALIGN( bytes )	( ALIGN_SIZE( (bytes) + SMALL_HEADER_SIZE ) - SMALL_HEADER_SIZE )
//#define MEDIUM_SMALLEST_SIZE	( ALIGN_SIZE( 256 ) + ALIGN_SIZE( MEDIUM_HEADER_SIZE ) )
//    public static class idHeap {
//
//        //
//        //	enum {
//        private static final int ALIGN = 8;			// memory alignment in bytes
//        //	};
//        //
//        //	enum {
//        private static final int INVALID_ALLOC = 0xdd;
//        private static final int SMALL_ALLOC = 0xaa;		// small allocation
//        private static final int MEDIUM_ALLOC = 0xbb;		// medium allocaction
//        private static final int LARGE_ALLOC = 0xcc;		// large allocaction
//        //	};
//        //
//
//        // allocation page
//        private class page_s {
//
//            Object data;					// data pointer to allocated memory
//            int dataSize;                                       // number of bytes of memory 'data' points to
//            page_s next;					// next free page in same page manager
//            page_s prev;					// used only when allocated
//            int largestFree;                                    // this data used by the medium-size heap manager
//            Object firstFree;                                   // pointer to first free entry
//        };
//        //
//
//        class mediumHeapEntry_s {
//
//            page_s page;					// pointer to page
//            int size;                                           // size of block
//            mediumHeapEntry_s prev;				// previous block
//            mediumHeapEntry_s next;				// next block
//            mediumHeapEntry_s prevFree;				// previous free block
//            mediumHeapEntry_s nextFree;				// next free block
//            int freeBlock;                                      // non-zero if free block
//        };
//        // variables
//        private Object smallFirstFree;///[256/ALIGN+1];         // small heap allocator lists (for allocs of 1-255 bytes)
//        private page_s smallCurPage;				// current page for small allocations
//        private int smallCurPageOffset;				// byte offset in current page
//        private page_s smallFirstUsedPage;			// first used page of the small heap manager
//        //
//        private page_s mediumFirstFreePage;			// first partially free page
//        private page_s mediumLastFreePage;			// last partially free page
//        private page_s mediumFirstUsedPage;			// completely used page
//        //
//        private page_s largeFirstUsedPage;			// first page used by the large heap manager
//        //
//        private page_s swapPage;
//        //
//        private int pagesAllocated;				// number of pages currently allocated
//        private int pageSize;					// size of one alloc page in bytes
//        //
//        private int pageRequests;				// page requests
//        private int OSAllocs;					// number of allocs made to the OS
//        //
//        private int c_heapAllocRunningCount;
//        //
//        private Object defragBlock;				// a single huge block that can be allocated
//        //							// at startup, then freed when needed
//        //
//
//        public idHeap() {
//            Init();
//        }
////	public				~idHeap( void );				// frees all associated data
//
//        public void Init() {					// initialize
//            OSAllocs = 0;
//            pageRequests = 0;
//            pageSize = 65536;//- sizeof(idHeap.page_s);
//            pagesAllocated = 0;								// reset page allocation counter
//
//            largeFirstUsedPage = null;								// init large heap manager
//            swapPage = null;
//
////	memset( smallFirstFree, 0, sizeof(smallFirstFree) );	// init small heap manager
//            smallFirstUsedPage = null;
//            smallCurPage = AllocatePage(pageSize);
//            assert (smallCurPage != null);
//            smallCurPageOffset = SMALL_ALIGN(0);
//
//            defragBlock = null;
//
//            mediumFirstFreePage = null;								// init medium heap manager
//            mediumLastFreePage = null;
//            mediumFirstUsedPage = null;
//
//            c_heapAllocRunningCount = 0;
//        }
//
//        public Object Allocate(final int bytes) {	// allocate memory
//            if (0 == bytes) {
//                return null;
//            }
//            c_heapAllocRunningCount++;
//
////#if USE_LIBC_MALLOC
////	return malloc( bytes );
////#else
//            if (0 == (bytes & ~255)) {
//                return SmallAllocate(bytes);
//            }
//            if (0 == (bytes & ~32767)) {
//                return MediumAllocate(bytes);
//            }
//            return LargeAllocate(bytes);
////#endif
//        }
//
//        public void Free(Object p) {				// free memory
//            if (null == p) {
//                return;
//            }
//            c_heapAllocRunningCount--;
//
////#if USE_LIBC_MALLOC
////	free( p );
////#else
//            switch (((int[]) p)[-1]) {//TODO:out of range?
//                case SMALL_ALLOC: {
//                    SmallFree(p);
//                    break;
//                }
//                case MEDIUM_ALLOC: {
//                    MediumFree(p);
//                    break;
//                }
//                case LARGE_ALLOC: {
//                    LargeFree(p);
//                    break;
//                }
//                default: {
//                    idLib.common.FatalError("idHeap::Free: invalid memory block (%s)", idLib.sys.GetCallStackCurStr(4));
//                    break;
//                }
//            }
////#endif
//        }
//
//        public Object Allocate16(final int bytes) {// allocate 16 byte aligned memory
//            byte[] ptr, alignedPtr;
//
//            ptr = new byte[bytes + 16 + 4];
//            if (NOT(ptr)) {
//                if (defragBlock != null) {
//                    idLib.common.Printf("Freeing defragBlock on alloc of %i.\n", bytes);
//                    free(defragBlock);
//                    defragBlock = null;
//                    ptr = (byte *) malloc(bytes + 16 + 4);
//                    AllocDefragBlock();
//                }
//                if (!ptr) {
//                    common.FatalError("malloc failure for %i", bytes);
//                }
//            }
//            alignedPtr = (byte *) (((int) ptr
//            ) + 15 & ~15);
//            if (alignedPtr - ptr < 4) {
//                alignedPtr += 16;
//            }
//             * ((int *) (alignedPtr - 4)) = (int) ptr;
//            return (void *) alignedPtr;
//        }
//
//        public void Free16(Object p) {				// free 16 byte aligned memory
//            free((void *) * ((int *) (((byte *) p
//        
//
//        
//
//        
//
//        
//
//        
//
//        ) - 4)));
//        }
//
//        /*
//         ================
//         idHeap::Msize
//
//         returns size of allocated memory block
//         p	= pointer to memory block
//         Notes:	size may not be the same as the size in the original
//         allocation request (due to block alignment reasons).
//         ================
//         */
//        public int Msize(Object p) {				// return size of data block
//
//            if (null == p) {
//                return 0;
//            }
//
////#if USE_LIBC_MALLOC
////	#ifdef _WIN32
////		return _msize( p );
////	#else
////		return 0;
////	#endif
////#else
//            switch (((int[]) p)[-1]) {
//                case SMALL_ALLOC: {
//                    return SMALL_ALIGN(((byte *) (p))[-SMALL_HEADER_SIZE] * ALIGN
//                
//                
//                
//                
//                
//                );
//                }
//                case MEDIUM_ALLOC: {
//                    return ((mediumHeapEntry_s *) (((byte *) (p
//                    )) - ALIGN_SIZE(MEDIUM_HEADER_SIZE))).size - ALIGN_SIZE(MEDIUM_HEADER_SIZE);
//                }
//                case LARGE_ALLOC: {
//                    return ((idHeap.page_s *) ( * ((dword *) (((byte *) p
//                    ) - ALIGN_SIZE(LARGE_HEADER_SIZE))))).dataSize - ALIGN_SIZE(LARGE_HEADER_SIZE);
//                }
//                default: {
//                    idLib.common.FatalError("idHeap::Msize: invalid memory block (%s)", idLib.sys.GetCallStackCurStr(4));
//                    return 0;
//                }
//            }
////#endif
//        }
//
//        /*
//         ================
//         idHeap::Dump
//
//         dump contents of the heap
//         ================
//         */
//        public void Dump() {
//            idHeap.page_s pg;
//
//            for (pg = smallFirstUsedPage; pg != null; pg = pg.next) {
//                idLib.common.Printf("%p  bytes %-8d  (in use by small heap)\n", pg.data, pg.dataSize);
//            }
//
//            if (smallCurPage != null) {
//                pg = smallCurPage;
//                idLib.common.Printf("%p  bytes %-8d  (small heap active page)\n", pg.data, pg.dataSize);
//            }
//
//            for (pg = mediumFirstUsedPage; pg != null; pg = pg.next) {
//                idLib.common.Printf("%p  bytes %-8d  (completely used by medium heap)\n", pg.data, pg.dataSize);
//            }
//
//            for (pg = mediumFirstFreePage; pg != null; pg = pg.next) {
//                idLib.common.Printf("%p  bytes %-8d  (partially used by medium heap)\n", pg.data, pg.dataSize);
//            }
//
//            for (pg = largeFirstUsedPage; pg != null; pg = pg.next) {
//                idLib.common.Printf("%p  bytes %-8d  (fully used by large heap)\n", pg.data, pg.dataSize);
//            }
//
//            idLib.common.Printf("pages allocated : %d\n", pagesAllocated);
//        }
////
//
//        public void AllocDefragBlock() {		// hack for huge renderbumps
//            int size = 0x40000000;
//
//            if (defragBlock != null) {
//                return;
//            }
//            while (true) {
//                defragBlock = malloc(size);
//                if (defragBlock != null) {
//                    break;
//                }
//                size >>= 1;
//            }
//            idLib.common.Printf("Allocated a %i mb defrag block\n", size / (1024 * 1024));
//        }
//
////	// methods
//
//        /*
//         ================
//         idHeap::AllocatePage
//
//         allocates memory from the OS
//         bytes	= page size in bytes
//         returns pointer to page
//         ================
//         */
//        private page_s AllocatePage(int bytes) {	// allocate page from the OS
//            idHeap.page_s p;
//
//            pageRequests++;
//
//            if (swapPage != null && swapPage.dataSize == bytes) {			// if we've got a swap page somewhere
//                p = swapPage;
//                swapPage = null;
//            } else {
//                int size;
//
//                size = bytes + sizeof(idHeap.page_s);
//
//                p = (idHeap.page_s *).malloc(size + ALIGN - 1);
//                if (null == p) {
//                    if (defragBlock != null) {
//                        idLib.common.Printf("Freeing defragBlock on alloc of %i.\n", size + ALIGN - 1);
//                        free(defragBlock);
//                        defragBlock = null;
//                        p = (idHeap.page_s *).malloc(size + ALIGN - 1);
//                        AllocDefragBlock();
//                    }
//                    if (null == p) {
//                        common.FatalError("malloc failure for %i", bytes);
//                    }
//                }
//
//                p.data = (void *) ALIGN_SIZE((int) ((byte *) (p
//                )) + sizeof(idHeap.page_s)
//                );
//                p.dataSize = size - sizeof(idHeap.page_s);
//                p.firstFree = null;
//                p.largestFree = 0;
//                OSAllocs++;
//            }
//
//            p.prev = null;
//            p.next = null;
//
//            pagesAllocated++;
//
//            return p;
//        }
//
//        /*
//         ================
//         idHeap::FreePage
//
//         frees a page back to the operating system
//         p	= pointer to page
//         ================
//         */
//        private void FreePage(idHeap.page_s p) {	// free an OS allocated page 
//            assert (p != null);
//
//            if (p.dataSize == pageSize && null == swapPage) {			// add to swap list?
//                swapPage = p;
//            } else {
//                FreePageReal(p);
//            }
//
//            pagesAllocated--;
//        }
////
//
////===============================================================
////
////	small heap code
////
////===============================================================
//
//        /*
//         ================
//         idHeap::SmallAllocate
//
//         allocate memory (1-255 bytes) from the small heap manager
//         bytes = number of bytes to allocate
//         returns pointer to allocated memory
//         ================
//         */
//        private Object SmallAllocate(int bytes) {	// allocate memory (1-255 bytes) from small heap manager
//            // we need the at least sizeof( int ) bytes for the free list
//            if (bytes < sizeof(int)) {
//                bytes = sizeof(int);
//            }
//
//            // increase the number of bytes if necessary to make sure the next small allocation is aligned
//            bytes = SMALL_ALIGN(bytes);
//
//            byte[] smallBlock = (byte *) (smallFirstFree[bytes / ALIGN]
//            );
//            if (smallBlock) {
//                int *link = (int *) (smallBlock + SMALL_HEADER_SIZE
//                );
//                smallBlock[1] = SMALL_ALLOC;					// allocation identifier
//                smallFirstFree[bytes / ALIGN] = (void *) ( * link
//                );
//                return (void *) (link
//            
//            
//            
//            
//            
//            );
//            }
//
//            int bytesLeft = pageSize - smallCurPageOffset;
//            // if we need to allocate a new page
//            if (bytes >= bytesLeft) {
//
//                smallCurPage.next = smallFirstUsedPage;
//                smallFirstUsedPage = smallCurPage;
//                smallCurPage = AllocatePage(pageSize);
//                if (null == smallCurPage) {
//                    return null;
//                }
//                // make sure the first allocation is aligned
//                smallCurPageOffset = SMALL_ALIGN(0);
//            }
//
//            smallBlock = ((byte *) smallCurPage.data) + smallCurPageOffset;
//            smallBlock[0] = (byte) (bytes / ALIGN);		// write # of bytes/ALIGN
//            smallBlock[1] = SMALL_ALLOC;					// allocation identifier
//            smallCurPageOffset += bytes + SMALL_HEADER_SIZE;	// increase the offset on the current page
//            return (smallBlock + SMALL_HEADER_SIZE);			// skip the first two bytes
//        }
//
//        /*
//         ================
//         idHeap::SmallFree
//
//         frees a block of memory allocated by SmallAllocate() call
//         data = pointer to block of memory
//         ================
//         */
//        void SmallFree(Object ptr) {			// free memory allocated by small heap manager
//            ((int[]) ptr)[-1] = INVALID_ALLOC;
//
//            byte[] d = {((int[]) ptr)[0] - SMALL_HEADER_SIZE};
//            int[] dt = (int[]) ptr;
//            // index into the table with free small memory blocks
//            int ix = d[0];
//
//            // check if the index is correct
//            if (ix > (256 / ALIGN)) {
//                idLib.common.FatalError("SmallFree: invalid memory block");
//            }
//
//            dt = smallFirstFree[ix];	// write next index
//            smallFirstFree[ix] = d;		// link
//        }
////
//
////===============================================================
////
////	medium heap code
////
////	Medium-heap allocated pages not returned to OS until heap destructor
////	called (re-used instead on subsequent medium-size malloc requests).
////
////===============================================================
//
//        /*
//         ================
//         idHeap::MediumAllocateFromPage
//
//         performs allocation using the medium heap manager from a given page
//         p				= page
//         sizeNeeded	= # of bytes needed
//         returns pointer to allocated memory
//         ================
//         */
//        private Object MediumAllocateFromPage(idHeap.page_s p, int sizeNeeded) {
//
//            mediumHeapEntry_s best, nw = null;
//            byte[] ret;
//
//            best = (mediumHeapEntry_s) p.firstFree;			// first block is largest
//
//            assert (best != null);
//            assert (best.size == p.largestFree);
//            assert (best.size >= sizeNeeded);
//
//            // if we can allocate another block from this page after allocating sizeNeeded bytes
//            if (best.size >= (sizeNeeded + MEDIUM_SMALLEST_SIZE)) {
//                nw = (mediumHeapEntry_s) ((byte[]) best + best.size - sizeNeeded);
//                nw.page = p;
//                nw.prev = best;
//                nw.next = best.next;
//                nw.prevFree = null;
//                nw.nextFree = null;
//                nw.size = sizeNeeded;
//                nw.freeBlock = 0;			// used block
//                if (best.next != null) {
//                    best.next.prev = nw;
//                }
//                best.next = nw;
//                best.size -= sizeNeeded;
//
//                p.largestFree = best.size;
//            } else {
//                if (best.prevFree != null) {
//                    best.prevFree.nextFree = best.nextFree;
//                } else {
//                    p.firstFree = best.nextFree;
//                }
//                if (best.nextFree != null) {
//                    best.nextFree.prevFree = best.prevFree;
//                }
//
//                best.prevFree = null;
//                best.nextFree = null;
//                best.freeBlock = 0;			// used block
//                nw = best;
//
//                p.largestFree = 0;
//            }
//
//            ret = (byte *) (nw
//            ) + ALIGN_SIZE(MEDIUM_HEADER_SIZE);
//            ret[-1] = MEDIUM_ALLOC;		// allocation identifier
//
//            return ret;
//        }
//
//        /*
//         ================
//         idHeap::MediumAllocate
//
//         allocate memory (256-32768 bytes) from medium heap manager
//         bytes	= number of bytes to allocate
//         returns pointer to allocated memory
//         ================
//         */
//        private Object MediumAllocate(int bytes) {	// allocate memory (256-32768 bytes) from medium heap manager
//            idHeap.page_s p;
//            Object data;
//
//            int sizeNeeded = ALIGN_SIZE(bytes) + ALIGN_SIZE(MEDIUM_HEADER_SIZE);
//
//            // find first page with enough space
//            for (p = mediumFirstFreePage; p != null; p = p.next) {
//                if (p.largestFree >= sizeNeeded) {
//                    break;
//                }
//            }
//
//            if (null == p) {								// need to allocate new page?
//                p = AllocatePage(pageSize);
//                if (null == p) {
//                    return null;					// malloc failure!
//                }
//                p.prev = null;
//                p.next = mediumFirstFreePage;
//                if (p.next != null) {
//                    p.next.prev = p;
//                } else {
//                    mediumLastFreePage = p;
//                }
//
//                mediumFirstFreePage = p;
//
//                p.largestFree = pageSize;
//                p.firstFree = p.data;
//
//                mediumHeapEntry_s e;
//                e = (mediumHeapEntry_s *) (p.firstFree
//                );
//                e.page = p;
//                // make sure ((byte *)e + e.size) is aligned
//                e.size = pageSize & ~(ALIGN - 1);
//                e.prev = null;
//                e.next = null;
//                e.prevFree = null;
//                e.nextFree = null;
//                e.freeBlock = 1;
//            }
//
//            data = MediumAllocateFromPage(p, sizeNeeded);		// allocate data from page
//
//            // if the page can no longer serve memory, move it away from free list
//            // (so that it won't slow down the later alloc queries)
//            // this modification speeds up the pageWalk from O(N) to O(sqrt(N))
//            // a call to free may swap this page back to the free list
//            if (p.largestFree < MEDIUM_SMALLEST_SIZE) {
//                if (p == mediumLastFreePage) {
//                    mediumLastFreePage = p.prev;
//                }
//
//                if (p == mediumFirstFreePage) {
//                    mediumFirstFreePage = p.next;
//                }
//
//                if (p.prev != null) {
//                    p.prev.next = p.next;
//                }
//                if (p.next != null) {
//                    p.next.prev = p.prev;
//                }
//
//                // link to "completely used" list
//                p.prev = null;
//                p.next = mediumFirstUsedPage;
//                if (p.next != null) {
//                    p.next.prev = p;
//                }
//                mediumFirstUsedPage = p;
//                return data;
//            }
//
//            // re-order linked list (so that next malloc query starts from current
//            // matching block) -- this speeds up both the page walks and block walks
//            if (p != mediumFirstFreePage) {
//                assert (mediumLastFreePage != null);
//                assert (mediumFirstFreePage != null);
//                assert (p.prev != null);
//
//                mediumLastFreePage.next = mediumFirstFreePage;
//                mediumFirstFreePage.prev = mediumLastFreePage;
//                mediumLastFreePage = p.prev;
//                p.prev.next = null;
//                p.prev = null;
//                mediumFirstFreePage = p;
//            }
//
//            return data;
//        }
//
//        /*
//         ================
//         idHeap::MediumFree
//
//         frees a block allocated by the medium heap manager
//         ptr	= pointer to data block
//         ================
//         */
//        private void MediumFree(Object ptr) {		// free memory allocated by medium heap manager
//            ((byte[]) ptr)[-1] = INVALID_ALLOC;
//
//            mediumHeapEntry_s e = (mediumHeapEntry_s *) ((byte *) ptr - ALIGN_SIZE(MEDIUM_HEADER_SIZE)
//            );
//            idHeap.page_s p = e.page;
//            boolean isInFreeList;
//
//            isInFreeList = p.largestFree >= MEDIUM_SMALLEST_SIZE;
//
//            assert (e.size != 0);
//            assert (e.freeBlock == 0);
//
//            mediumHeapEntry_s prev = e.prev;
//
//            // if the previous block is free we can merge
//            if (prev != null && prev.freeBlock != 0) {
//                prev.size += e.size;
//                prev.next = e.next;
//                if (e.next != null) {
//                    e.next.prev = prev;
//                }
//                e = prev;
//            } else {
//                e.prevFree = null;				// link to beginning of free list
//                e.nextFree = (mediumHeapEntry_s) p.firstFree;
//                if (e.nextFree != null) {
//                    assert (0 == (e.nextFree.prevFree));
//                    e.nextFree.prevFree = e;
//                }
//
//                p.firstFree = e;
//                p.largestFree = e.size;
//                e.freeBlock = 1;				// mark block as free
//            }
//
//            mediumHeapEntry_s next = e.next;
//
//            // if the next block is free we can merge
//            if (next != null && next.freeBlock != 0) {
//                e.size += next.size;
//                e.next = next.next;
//
//                if (next.next != null) {
//                    next.next.prev = e;
//                }
//
//                if (next.prevFree != null) {
//                    next.prevFree.nextFree = next.nextFree;
//                } else {
//                    assert (next == p.firstFree);
//                    p.firstFree = next.nextFree;
//                }
//
//                if (next.nextFree != null) {
//                    next.nextFree.prevFree = next.prevFree;
//                }
//            }
//
//            if (p.firstFree != null) {
//                p.largestFree = ((mediumHeapEntry_s) p.firstFree).size;
//            } else {
//                p.largestFree = 0;
//            }
//
//            // did e become the largest block of the page ?
//            if (e.size > p.largestFree) {
//                assert (e != p.firstFree);
//                p.largestFree = e.size;
//
//                if (e.prevFree != null) {
//                    e.prevFree.nextFree = e.nextFree;
//                }
//                if (e.nextFree != null) {
//                    e.nextFree.prevFree = e.prevFree;
//                }
//
//                e.nextFree = (mediumHeapEntry_s) p.firstFree;
//                e.prevFree = null;
//                if (e.nextFree != null) {
//                    e.nextFree.prevFree = e;
//                }
//                p.firstFree = e;
//            }
//
//            // if page wasn't in free list (because it was near-full), move it back there
//            if (!isInFreeList) {
//
//                // remove from "completely used" list
//                if (p.prev != null) {
//                    p.prev.next = p.next;
//                }
//                if (p.next != null) {
//                    p.next.prev = p.prev;
//                }
//                if (p == mediumFirstUsedPage) {
//                    mediumFirstUsedPage = p.next;
//                }
//
//                p.next = null;
//                p.prev = mediumLastFreePage;
//
//                if (mediumLastFreePage != null) {
//                    mediumLastFreePage.next = p;
//                }
//                mediumLastFreePage = p;
//                if (null == mediumFirstFreePage) {
//                    mediumFirstFreePage = p;
//                }
//            }
//        }
////
//
////===============================================================
////
////	large heap code
////
////===============================================================
//
//        /*
//         ================
//         idHeap::LargeAllocate
//
//         allocates a block of memory from the operating system
//         bytes	= number of bytes to allocate
//         returns pointer to allocated memory
//         ================
//         */
//        private Object LargeAllocate(int bytes) {	// allocate large block from OS directly
//            idHeap.page_s p = AllocatePage(bytes + ALIGN_SIZE(LARGE_HEADER_SIZE));
//
//            assert (p != null);
//
//            if (null == p) {
//                return null;
//            }
//
//            byte[] d = (byte[]) (p.data) + ALIGN_SIZE(LARGE_HEADER_SIZE);
//            int[] dw = (dword *) (d - ALIGN_SIZE(LARGE_HEADER_SIZE)
//            );
//            dw[0] = p;				// write pointer back to page table
//            d[-1] = LARGE_ALLOC;			// allocation identifier
//
//            // link to 'large used page list'
//            p.prev = null;
//            p.next = largeFirstUsedPage;
//            if (p.next != null) {
//                p.next.prev = p;
//            }
//            largeFirstUsedPage = p;
//
//            return d;
//        }
//
//        /*
//         ================
//         idHeap::LargeFree
//
//         frees a block of memory allocated by the 'large memory allocator'
//         p	= pointer to allocated memory
//         ================
//         */
//        private void LargeFree(Object ptr) {			// free memory allocated by large heap manager
//            idHeap.page_s pg;
//
//            (byte[]) ptr[-1] = INVALID_ALLOC;
//
//            // get page pointer
//            pg = (idHeap.page_s *) ( * ((dword *) (((byte *) ptr
//            ) - ALIGN_SIZE(LARGE_HEADER_SIZE)
//            )));
//
//            // unlink from doubly linked list
//            if (pg.prev != null) {
//                pg.prev.next = pg.next;
//            }
//            if (pg.next != null) {
//                pg.next.prev = pg.prev;
//            }
//            if (pg == largeFirstUsedPage) {
//                largeFirstUsedPage = pg.next;
//            }
//            pg.next = pg.prev = null;
//
//            FreePage(pg);
//        }
////
//
//        /*
//         ================
//         idHeap::ReleaseSwappedPages
//
//         releases the swap page to OS
//         ================
//         */
//        private void ReleaseSwappedPages() {
//            if (swapPage != null) {
////                FreePageReal(swapPage);
//            }
//            swapPage = null;
//        }
//
//        /*
//         ================
//         idHeap::FreePageReal
//
//         frees page to be used by the OS
//         p	= page to free
//         ================
//         */
//        private void FreePageReal(idHeap.page_s[] p) {
//            assert (p != null);
////	::free( p );
//            p[0] = null;
//        }
//    };
//
//    /*
//     ===============================================================================
//
//     Memory Management
//
//     This is a replacement for the compiler heap code (i.e. "C" malloc() and
//     free() calls). On average 2.5-3.0 times faster than MSVC malloc()/free().
//     Worst case performance is 1.65 times faster and best case > 70 times.
// 
//     ===============================================================================
//     */
    public static class memoryStats_t {

        public int num;
        public int minSize;
        public int maxSize;
        public int totalSize;

        public memoryStats_t(int num, int minSize, int maxSize, int totalSize) {
            this.num = num;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.totalSize = totalSize;
        }
    };
////
////
//

    static void Mem_Init() {
        throw new UnsupportedOperationException();
//        mem_heap = new idHeap();
//        Mem_ClearFrameStats();
    }
//

    static void Mem_Shutdown() {
        throw new UnsupportedOperationException();
//	idHeap m = mem_heap;
//        mem_heap = null;
//	delete m;
    }

    public static void Mem_EnableLeakTest(final String name) {
        //TODO:something
    }
//
//    public static void Mem_ClearFrameStats() {
//        mem_frame_allocs.num = mem_frame_frees.num = 0;
//        mem_frame_allocs.minSize = mem_frame_frees.minSize = 0x0fffffff;
//        mem_frame_allocs.maxSize = mem_frame_frees.maxSize = -1;
//        mem_frame_allocs.totalSize = mem_frame_frees.totalSize = 0;
//    }
//
//    public static void Mem_GetFrameStats(memoryStats_t[] allocs, memoryStats_t[] frees) {
//        allocs[0] = mem_frame_allocs;
//        frees[0] = mem_frame_frees;
//    }
//
//    public static void Mem_GetStats(memoryStats_t[] stats) {
//        stats[0] = mem_total_allocs;
//    }
//
//    /*
//     ==================
//     Mem_Dump_f
//     ==================
//     */
//    public static class Mem_Dump_f extends cmdFunction_t {
//
//        private static final cmdFunction_t instance = new Mem_DumpCompressed_f();
//
//        public static cmdFunction_t getInstance() {
//            return instance;
//        }
//
//        @Override
//        public void run(idCmdArgs args) {
//            final String fileName;
//
//            if (args.Argc() >= 2) {
//                fileName = args.Argv(1);
//            } else {
//                fileName = "memorydump.txt";
//            }
//            Mem_Dump(fileName);
//        }
//    };
//
//    enum memorySortType_t {
//
//        MEMSORT_SIZE,
//        MEMSORT_LOCATION,
//        MEMSORT_NUMALLOCS,
//        MEMSORT_CALLSTACK
//    };
//
//
//    /*
//     ==================
//     Mem_DumpCompressed_f
//     ==================
//     */
//    public static class Mem_DumpCompressed_f extends cmdFunction_t {
//
//        private static final cmdFunction_t instance = new Mem_DumpCompressed_f();
//
//        public static cmdFunction_t getInstance() {
//            return instance;
//        }
//
//        @Override
//        public void run(idCmdArgs args) {
//            int argNum;
//            String arg;
//            final String fileName;
//            memorySortType_t memSort = MEMSORT_LOCATION;
//            int sortCallStack = 0, numFrames = 0;
//
//            // get cmd-line options
//            argNum = 1;
//            arg = args.Argv(argNum);
//            while (arg.charAt(0) == '-') {
//                arg = args.Argv(++argNum);
//                if (idStr.Icmp(arg, "s") == 0) {
//                    memSort = MEMSORT_SIZE;
//                } else if (idStr.Icmp(arg, "l") == 0) {
//                    memSort = MEMSORT_LOCATION;
//                } else if (idStr.Icmp(arg, "a") == 0) {
//                    memSort = MEMSORT_NUMALLOCS;
//                } else if (idStr.Icmp(arg, "cs1") == 0) {
//                    memSort = MEMSORT_CALLSTACK;
//                    sortCallStack = 2;
//                } else if (idStr.Icmp(arg, "cs2") == 0) {
//                    memSort = MEMSORT_CALLSTACK;
//                    sortCallStack = 1;
//                } else if (idStr.Icmp(arg, "cs3") == 0) {
//                    memSort = MEMSORT_CALLSTACK;
//                    sortCallStack = 0;
//                } else if (arg.charAt(0) == 'f') {
//                    numFrames = Integer.parseInt(arg.substring(1));
//                } else {
//                    idLib.common.Printf("memoryDumpCompressed [options] [filename]\n"
//                            + "options:\n"
//                            + "  -s     sort on size\n"
//                            + "  -l     sort on location\n"
//                            + "  -a     sort on the number of allocations\n"
//                            + "  -cs1   sort on first function on call stack\n"
//                            + "  -cs2   sort on second function on call stack\n"
//                            + "  -cs3   sort on third function on call stack\n"
//                            + "  -f<X>  only report allocations the last X frames\n"
//                            + "By default the memory allocations are sorted on location.\n"
//                            + "By default a 'memorydump.txt' is written if no file name is specified.\n");
//                    return;
//                }
//                arg = args.Argv(++argNum);
//            }
//            if (argNum >= args.Argc()) {
//                fileName = "memorydump.txt";
//            } else {
//                fileName = arg;
//            }
//            Mem_DumpCompressed(fileName, memSort, sortCallStack, numFrames);
//        }
//    };
//
//    void Mem_AllocDefragBlock() {
//        mem_heap.AllocDefragBlock();
//    }
////
////
//////#ifndef ID_DEBUG_MEMORY
////
//
//    public static Object Mem_Alloc(final int size) {
//        if (0 == size) {
//            return null;
//        }
//        if (null == mem_heap) {
////#ifdef CRASH_ON_STATIC_ALLOCATION
////		*((int*)0x0) = 1;
////#endif
////            return malloc(size);
//            return new Object[size];
//        }
//        Object mem = mem_heap.Allocate(size);
//        Mem_UpdateAllocStats(mem_heap.Msize(mem));
//        return mem;
//    }
//
//    public static Object Mem_ClearedAlloc(final int size) {
//        Object[] mem = new Object[size];
//        mem[0] = Mem_Alloc(size);
//        Simd.SIMDProcessor.Memset(mem, 0, size);
//        return mem;
//    }
//
//    public static void Mem_Free(Object ptr) {
//        Mem_Free((Object[]) ptr);//TODO:test!!
//    }
//
//    public static void Mem_Free(Object[] ptr) {
//        if (null == ptr) {
//            return;
//        }
//        if (null == mem_heap) {
////#ifdef CRASH_ON_STATIC_ALLOCATION
////		*((int*)0x0) = 1;
////#endif
////		free( ptr );
//            ptr[0] = null;
//            return;
//        }
//        Mem_UpdateFreeStats(mem_heap.Msize(ptr[0]));
//        mem_heap.Free(ptr[0]);
//    }
//
//    public static String Mem_CopyString(final String in) {
////	char	*out;
////	
////	out = (char *)Mem_Alloc( strlen(in) + 1 );
////	strcpy( out, in );
//        return in;
//    }
//
//    Object Mem_Alloc16(final int size) {
//        if (0 == size) {
//            return null;
//        }
//        if (null == mem_heap) {
////#ifdef CRASH_ON_STATIC_ALLOCATION
////		*((int*)0x0) = 1;
////#endif
//            return malloc(size);
//        }
//        Object mem = mem_heap.Allocate16(size);
//        // make sure the memory is 16 byte aligned
//        assert ((((int) mem) & 15) == 0);
//        return mem;
//    }
//
//    void Mem_Free16(Object[] ptr) {
//        if (null == ptr) {
//            return;
//        }
//        if (null == mem_heap) {
////#ifdef CRASH_ON_STATIC_ALLOCATION
////		*((int*)0x0) = 1;
////#endif
////            free(ptr);
//            ptr[0] = null;
//            return;
//        }
//        // make sure the memory is 16 byte aligned
//        assert ((((int) ptr[0] & 15) == 0);
//        mem_heap.Free16(ptr);
//    }
////
//////#ifdef ID_REDIRECT_NEWDELETE
////===============================================================
////
////	memory allocation all in one place
////
////===============================================================
//    static idHeap mem_heap = null;
//    static memoryStats_t mem_total_allocs = new memoryStats_t(0, 0x0fffffff, -1, 0);
//    static memoryStats_t mem_frame_allocs;
//    static memoryStats_t mem_frame_frees;
//

    /*
     ===============================================================================

     Block based allocator for fixed size objects.

     All objects of the 'type' are properly constructed.
     However, the constructor is not called for re-used objects.

     ===============================================================================
     */
    @Deprecated//not used
    public static class idBlockAlloc<type> {

//        private block_t blocks;//TODO:array or pointer?
//        private element_t free;
//        private int total;
//        private int active;
//        private final int blockSize;
//        //
//        //
//
        public idBlockAlloc(int blockSize) {
//            this.blockSize = blockSize;
//            blocks = null;
//            free = null;
//            total = active = 0;
        }

////public							~idBlockAlloc( void );
//
        public void Shutdown() {
            throw new UnsupportedOperationException();
//            while (blocks != null) {
//                block_s block = blocks;
//                blocks = (block_t) blocks.next;
////		delete block;
//            }
//            blocks = null;
//            free = null;
//            total = active = 0;
        }
//

        public type Alloc() {
            throw new UnsupportedOperationException();
//            if (null == free) {
//                block_t block = new block_t();
//                block.next = blocks;
//                blocks = block;
//                for (int i = 0; i < blockSize; i++) {
//                    block.elements[i].next = free;
//                    free = block.elements[i];
//                }
//                total += blockSize;
//            }
//            active++;
//            element_t element = free;
//            free = (element_t) free.next;
//            element.next = null;
//            return element.t;
        }
//

        public void Free(type t) {
            throw new UnsupportedOperationException();
//            element_t element = t - blocks.t;
//            element.next = free;
//            free = element;
//            active--;
        }
//
//        public int GetTotalCount() {
//            return total;
//        }
//

        public int GetAllocCount() {
            throw new UnsupportedOperationException();
//            return active;
        }
//
//        public int GetFreeCount() {
//            return total - active;
//        }
////
////
//
//        private class element_s {
//
//            element_s next;
//            type t;
//        };
//
//        private class element_t extends element_s {
//        }
//
//        private class block_s {
//
//            element_t[] elements = (element_t[]) new Object[blockSize];
//            block_s next;
//        };
//
//        private class block_t extends block_s {
//        }
    };

//    /*
//     ==============================================================================
//
//     Dynamic allocator, simple wrapper for normal allocations which can
//     be interchanged with idDynamicBlockAlloc.
//
//     No constructor is called for the 'type'.
//     Allocated blocks are always 16 byte aligned.
//
//     ==============================================================================
//     */
//    public static class idDynamicAlloc<type> {
//
//        public idDynamicAlloc(int baseBlockSize, int minBlockSize) {
//            Clear();
//            this.baseBlockSize = baseBlockSize;
//            this.minBlockSize = minBlockSize;
//        }
////public									~idDynamicAlloc( void );
////
//
//        public void Init() {
//        }
//
//        public void Shutdown() {
//            Clear();
//        }
//
//        public void SetFixedBlocks(int numBlocks) {
//        }
//
//        public void SetLockMemory(boolean lock) {
//        }
//
//        public void FreeEmptyBaseBlocks() {
//        }
////
//
//        public type Alloc(final int num) {
//            numAllocs++;
//            if (num <= 0) {
//                return null;
//            }
//            numUsedBlocks++;
//            usedBlockMemory += num * sizeof(type);
//            return Mem_Alloc16(num * sizeof(type));
//        }
//
//        public type Resize(type[] ptr, final int num) {
//
//            numResizes++;
//
//            if (ptr == null) {
//                return Alloc(num);
//            }
//
//            if (num <= 0) {
//                Free(ptr);
//                return null;
//            }
//
//            assert (false);
//            return ptr[0];
//        }
//
//        public void Free(type[] ptr) {
//            numFrees++;
//            if (ptr == null) {
//                return;
//            }
//            Mem_Free16(ptr);
//        }
//
//        public String CheckMemory(final type[] ptr) {
//            return null;
//        }
//
//        public int GetNumBaseBlocks() {
//            return 0;
//        }
//
//        public int GetBaseBlockMemory() {
//            return 0;
//        }
//
//        public int GetNumUsedBlocks() {
//            return numUsedBlocks;
//        }
//
//        public int GetUsedBlockMemory() {
//            return usedBlockMemory;
//        }
//
//        public int GetNumFreeBlocks() {
//            return 0;
//        }
//
//        public int GetFreeBlockMemory() {
//            return 0;
//        }
//
//        public int GetNumEmptyBaseBlocks() {
//            return 0;
//        }
////
////
//        private int numUsedBlocks;			// number of used blocks
//        private int usedBlockMemory;		// total memory in used blocks
////
//        private int numAllocs;
//        private int numResizes;
//        private int numFrees;
////
//
//        private void Clear() {
//            numUsedBlocks = 0;
//            usedBlockMemory = 0;
//            numAllocs = 0;
//            numResizes = 0;
//            numFrees = 0;
//        }
////
//        private final int baseBlockSize, minBlockSize;
//    };
//
//    /*
//     ==============================================================================
//
//     Fast dynamic block allocator.
//
//     No constructor is called for the 'type'.
//     Allocated blocks are always 16 byte aligned.
//
//     ==============================================================================
//     */
////#define DYNAMIC_BLOCK_ALLOC_CHECK
//    class idDynamicBlock<type> {
////
//
//        public type GetMemory() {
//            return (type *) (((byte *) this
//            ) + sizeof(idDynamicBlock < type >)
//        
//
//        
//
//        
//
//        
//
//        
//
//        );
//        }
//
//        public int GetSize() {
//            return Math.abs(size);
//        }
//
//        public void SetSize(int s, boolean isBaseBlock) {
//            size = isBaseBlock ? -s : s;
//        }
//
//        public boolean IsBaseBlock() {
//            return (size < 0);
//        }
////
//////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
//////	int								id[3];
//////	void *							allocator;
//////#endif
////
//        public int size;					// size in bytes of the block
//        public idDynamicBlock<type> prev;					// previous memory block
//        public idDynamicBlock<type> next;					// next memory block
//        public idBTreeNode<idDynamicBlock<type>, Integer>[] node;			// node in the B-Tree with free blocks
//    };
//
//    public static class idDynamicBlockAlloc<type> {
//
//        public idDynamicBlockAlloc(int baseBlockSize, int minBlockSize) {
//            Clear();
//            this.baseBlockSize = baseBlockSize;
//            this.minBlockSize = minBlockSize;
//        }
////public									~idDynamicBlockAlloc( void );
////
//
//        public void Init() {
//            freeTree.Init();
//        }
//
//        public void Shutdown() {
//            idDynamicBlock<type> block;
//
//            for (block = firstBlock; block != null; block = block.next) {
//                if (block.node == null) {
//                    FreeInternal(block);
//                }
//            }
//
//            for (block = firstBlock; block != null; block = firstBlock) {
//                firstBlock = block.next;
//                assert (block.IsBaseBlock());
//                if (lockMemory) {
//                    idLib.sys.UnlockMemory(block, block.GetSize() + (int) sizeof(idDynamicBlock < type >));
//                }
//                Mem_Free16(block);
//            }
//
//            freeTree.Shutdown();
//
//            Clear();
//        }
//
//        public void SetFixedBlocks(int numBlocks) {
//            idDynamicBlock<type> block;
//
//            for (int i = numBaseBlocks; i < numBlocks; i++) {
//                block = (idDynamicBlock<type>) Mem_Alloc16(baseBlockSize);
//                if (lockMemory) {
//                    idLib.sys.LockMemory(block, baseBlockSize);
//                }
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////		memcpy( block.id, blockId, sizeof( block.id ) );
////		block.allocator = (void*)this;
////#endif
//                block.SetSize(baseBlockSize - (int) sizeof(idDynamicBlock < type >), true);
//                block.next = null;
//                block.prev = lastBlock;
//                if (lastBlock != null) {
//                    lastBlock.next = block;
//                } else {
//                    firstBlock = block;
//                }
//                lastBlock = block;
//                block.node = null;
//
//                FreeInternal(block);
//
//                numBaseBlocks++;
//                baseBlockMemory += baseBlockSize;
//            }
//
//            allowAllocs = false;
//        }
//
//        public void SetLockMemory(boolean lock) {
//            lockMemory = lock;
//        }
//
//        public void FreeEmptyBaseBlocks() {
//            idDynamicBlock<type> block, next;
//
//            for (block = firstBlock; block != null; block = next) {
//                next = block.next;
//
//                if (block.IsBaseBlock() && block.node != null && (next == null || next.IsBaseBlock())) {
//                    UnlinkFreeInternal(block);
//                    if (block.prev != null) {
//                        block.prev.next = block.next;
//                    } else {
//                        firstBlock = block.next;
//                    }
//                    if (block.next != null) {
//                        block.next.prev = block.prev;
//                    } else {
//                        lastBlock = block.prev;
//                    }
//                    if (lockMemory) {
//                        idLib.sys.UnlockMemory(block, block.GetSize() + (int) sizeof(idDynamicBlock < type >));
//                    }
//                    numBaseBlocks--;
//                    baseBlockMemory -= block.GetSize() + (int) sizeof(idDynamicBlock < type >);
//                    Mem_Free16(block);
//                }
//            }
//
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	CheckMemory();
////#endif
//        }
////
//
//        public type Alloc(final int num) {
//            idDynamicBlock<type> block;
//
//            numAllocs++;
//
//            if (num <= 0) {
//                return null;
//            }
//
//            block = AllocInternal(num);
//            if (block == null) {
//                return null;
//            }
//            block = ResizeInternal(block, num);
//            if (block == null) {
//                return null;
//            }
//
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	CheckMemory();
////#endif
//            numUsedBlocks++;
//            usedBlockMemory += block.GetSize();
//
//            return block.GetMemory();
//        }
//
//        public type Resize(type ptr, final int num) {
//
//            numResizes++;
//
//            if (ptr == null) {
//                return Alloc(num);
//            }
//
//            if (num <= 0) {
//                Free(ptr);
//                return null;
//            }
//
//            idDynamicBlock<type> block = (idDynamicBlock < type>                                          * ) ( ( (byte *) ptr ) - (int
//            )sizeof(idDynamicBlock < type >) );
//
//	usedBlockMemory -= block.GetSize();
//
//            block = ResizeInternal(block, num);
//            if (block == null) {
//                return null;
//            }
//
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	CheckMemory();
////#endif
//            usedBlockMemory += block.GetSize();
//
//            return block.GetMemory();
//        }
//
//        public void Free(type ptr) {
//
//            numFrees++;
//
//            if (ptr == null) {
//                return;
//            }
//
//            idDynamicBlock<type> block = (idDynamicBlock < type>                                         * ) ( ( (byte *) ptr ) - (int
//            )sizeof(idDynamicBlock < type >) );
//
//	numUsedBlocks--;
//            usedBlockMemory -= block.GetSize();
//
//            FreeInternal(block);
//
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	CheckMemory();
////#endif
//        }
//
//        public String CheckMemory(final type ptr) {
//            idDynamicBlock<type> block;
//
//            if (ptr == null) {
//                return null;
//            }
//
//            block = (idDynamicBlock < type>                                         * ) ( ( (byte *) ptr ) - (int
//            )sizeof(idDynamicBlock < type >) );
//
//	if (block.node != null) {
//                return "memory has been freed";
//            }
//
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	if ( block->id[0] != 0x11111111 || block->id[1] != 0x22222222 || block->id[2] != 0x33333333 ) {
////		return "memory has invalid id";
////	}
////	if ( block->allocator != (void*)this ) {
////		return "memory was allocated with different allocator";
////	}
////#endif
//
//            /* base blocks can be larger than baseBlockSize which can cause this code to fail
//             idDynamicBlock<type> *base;
//             for ( base = firstBlock; base != NULL; base = base->next ) {
//             if ( base->IsBaseBlock() ) {
//             if ( ((int)block) >= ((int)base) && ((int)block) < ((int)base) + baseBlockSize ) {
//             break;
//             }
//             }
//             }
//             if ( base == NULL ) {
//             return "no base block found for memory";
//             }
//             */
//            return null;
//        }
////
//
//        public int GetNumBaseBlocks() {
//            return numBaseBlocks;
//        }
//
//        public int GetBaseBlockMemory() {
//            return baseBlockMemory;
//        }
//
//        public int GetNumUsedBlocks() {
//            return numUsedBlocks;
//        }
//
//        public int GetUsedBlockMemory() {
//            return usedBlockMemory;
//        }
//
//        public int GetNumFreeBlocks() {
//            return numFreeBlocks;
//        }
//
//        public int GetFreeBlockMemory() {
//            return freeBlockMemory;
//        }
//
//        public int GetNumEmptyBaseBlocks() {
//            int numEmptyBaseBlocks;
//            idDynamicBlock<type> block;
//
//            numEmptyBaseBlocks = 0;
//            for (block = firstBlock; block != null; block = block.next) {
//                if (block.IsBaseBlock() && block.node != null && (block.next == null || block.next.IsBaseBlock())) {
//                    numEmptyBaseBlocks++;
//                }
//            }
//            return numEmptyBaseBlocks;
//        }
////
////
//        private idDynamicBlock<type> firstBlock;				// first block in list in order of increasing address
//        private idDynamicBlock<type> lastBlock;				// last block in list in order of increasing address
//        private idBTree<idDynamicBlock<type>, Integer> freeTree = new idBTree<>(4);			// B-Tree with free memory blocks
//        private boolean allowAllocs;			// allow base block allocations
//        private boolean lockMemory;				// lock memory so it cannot get swapped out
////
//////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
//////	int								blockId[3];
//////#endif
//        private int numBaseBlocks;			// number of base blocks
//        private int baseBlockMemory;		// total memory in base blocks
//        private int numUsedBlocks;			// number of used blocks
//        private int usedBlockMemory;		// total memory in used blocks
//        private int numFreeBlocks;			// number of free blocks
//        private int freeBlockMemory;		// total memory in free blocks
//        private int numAllocs;
//        private int numResizes;
//        private int numFrees;
////
//
//        private void Clear() {
//            firstBlock = lastBlock = null;
//            allowAllocs = true;
//            lockMemory = false;
//            numBaseBlocks = 0;
//            baseBlockMemory = 0;
//            numUsedBlocks = 0;
//            usedBlockMemory = 0;
//            numFreeBlocks = 0;
//            freeBlockMemory = 0;
//            numAllocs = 0;
//            numResizes = 0;
//            numFrees = 0;
//
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	blockId[0] = 0x11111111;
////	blockId[1] = 0x22222222;
////	blockId[2] = 0x33333333;
////#endif
//        }
//
//        private idDynamicBlock<type> AllocInternal(final int num) {
//            idDynamicBlock<type> block;
//            int alignedBytes = (num * sizeof(type) + 15) & ~15;
//
//            block = freeTree.FindSmallestLargerEqual(alignedBytes);
//            if (block != null) {
//                UnlinkFreeInternal(block);
//            } else if (allowAllocs) {
//                int allocSize = Max(baseBlockSize, alignedBytes + (int) sizeof(idDynamicBlock < type >));
//                block = (idDynamicBlock < type>                                      * ) Mem_Alloc16(allocSize);
//                if (lockMemory) {
//                    idLib.sys.LockMemory(block, baseBlockSize);
//                }
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////		memcpy( block->id, blockId, sizeof( block->id ) );
////		block->allocator = (void*)this;
////#endif
//                block.SetSize(allocSize - (int) sizeof(idDynamicBlock < type >), true);
//                block.next = null;
//                block.prev = lastBlock;
//                if (lastBlock != null) {
//                    lastBlock.next = block;
//                } else {
//                    firstBlock = block;
//                }
//                lastBlock = block;
//                block.node = null;
//
//                numBaseBlocks++;
//                baseBlockMemory += allocSize;
//            }
//
//            return block;
//        }
//
//        private idDynamicBlock<type> ResizeInternal(idDynamicBlock<type> block, final int num) {
//            int alignedBytes = (num * sizeof(type) + 15) & ~15;
//
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	assert( block.id[0] == 0x11111111 && block.id[1] == 0x22222222 && block.id[2] == 0x33333333 && block.allocator == (void*)this );
////#endif
//            // if the new size is larger
//            if (alignedBytes > block.GetSize()) {
//
//                idDynamicBlock<type> nextBlock = block.next;
//
//                // try to annexate the next block if it's free
//                if (nextBlock != null && !nextBlock.IsBaseBlock() && nextBlock.node != null
//                        && block.GetSize() + (int) sizeof(idDynamicBlock < type >) + nextBlock.GetSize() >= alignedBytes) {
//
//                    UnlinkFreeInternal(nextBlock);
//                    block.SetSize(block.GetSize() + (int) sizeof(idDynamicBlock < type >) + nextBlock.GetSize(), block.IsBaseBlock());
//                    block.next = nextBlock.next;
//                    if (nextBlock.next != null) {
//                        nextBlock.next.prev = block;
//                    } else {
//                        lastBlock = block;
//                    }
//                } else {
//                    // allocate a new block and copy
//                    idDynamicBlock<type> oldBlock = block;
//                    block = AllocInternal(num);
//                    if (block == null) {
//                        return null;
//                    }
//                    memcpy(block.GetMemory(), oldBlock.GetMemory(), oldBlock.GetSize());
//                    FreeInternal(oldBlock);
//                }
//            }
//
//            // if the unused space at the end of this block is large enough to hold a block with at least one element
//            if (block.GetSize() - alignedBytes - (int) sizeof(idDynamicBlock < type >) < Max(minBlockSize, (int) sizeof(type))) {
//                return block;
//            }
//
//            idDynamicBlock<type> newBlock;
//
//            newBlock = (idDynamicBlock < type>                                     * ) ( ( (byte *) block ) + (int
//            )sizeof(idDynamicBlock < type >) + alignedBytes );
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	memcpy( newBlock.id, blockId, sizeof( newBlock.id ) );
////	newBlock.allocator = (void*)this;
////#endif
//	newBlock.SetSize(block.GetSize() - alignedBytes - (int) sizeof(idDynamicBlock < type >), false);
//            newBlock.next = block.next;
//            newBlock.prev = block;
//            if (newBlock.next != null) {
//                newBlock.next.prev = newBlock;
//            } else {
//                lastBlock = newBlock;
//            }
//            newBlock.node = null;
//            block.next = newBlock;
//            block.SetSize(alignedBytes, block.IsBaseBlock());
//
//            FreeInternal(newBlock);
//
//            return block;
//        }
//
//        private void FreeInternal(idDynamicBlock<type> block) {
//
//            assert (block.node == null);
//
////#ifdef DYNAMIC_BLOCK_ALLOC_CHECK
////	assert( block.id[0] == 0x11111111 && block.id[1] == 0x22222222 && block.id[2] == 0x33333333 && block.allocator == (void*)this );
////#endif
//            // try to merge with a next free block
//            idDynamicBlock<type> nextBlock = block.next;
//            if (nextBlock != null && !nextBlock.IsBaseBlock() && nextBlock.node != null) {
//                UnlinkFreeInternal(nextBlock);
//                block.SetSize(block.GetSize() + (int) sizeof(idDynamicBlock < type >) + nextBlock.GetSize(), block.IsBaseBlock());
//                block.next = nextBlock.next;
//                if (nextBlock.next != null) {
//                    nextBlock.next.prev = block;
//                } else {
//                    lastBlock = block;
//                }
//            }
//
//            // try to merge with a previous free block
//            idDynamicBlock<type> prevBlock = block.prev;
//            if (prevBlock != null && !block.IsBaseBlock() && prevBlock.node != null) {
//                UnlinkFreeInternal(prevBlock);
//                prevBlock.SetSize(prevBlock.GetSize() + (int) sizeof(idDynamicBlock < type >) + block.GetSize(), prevBlock.IsBaseBlock());
//                prevBlock.next = block.next;
//                if (block.next != null) {
//                    block.next.prev = prevBlock;
//                } else {
//                    lastBlock = prevBlock;
//                }
//                LinkFreeInternal(prevBlock);
//            } else {
//                LinkFreeInternal(block);
//            }
//        }
//
//        private void LinkFreeInternal(idDynamicBlock<type> block) {
//            block.node = freeTree.Add(block, block.GetSize());
//            numFreeBlocks++;
//            freeBlockMemory += block.GetSize();
//        }
//
//        private void UnlinkFreeInternal(idDynamicBlock<type> block) {
//            freeTree.Remove(block.node);
//            block.node = null;
//            numFreeBlocks--;
//            freeBlockMemory -= block.GetSize();
//        }
//
//        private void CheckMemory() {
//            idDynamicBlock<type> block;
//
//            for (block = firstBlock; block != null; block = block.next) {
//                // make sure the block is properly linked
//                if (block.prev == null) {
//                    assert (firstBlock == block);
//                } else {
//                    assert (block.prev.next == block);
//                }
//                if (block.next == null) {
//                    assert (lastBlock == block);
//                } else {
//                    assert (block.next.prev == block);
//                }
//            }
//        }
//        private final int baseBlockSize, minBlockSize;
//    };
}
