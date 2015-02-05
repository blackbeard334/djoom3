package neo.idlib.math;

import neo.idlib.math.Simd_Generic.idSIMD_Generic;

/**
 *
 */
public class Simd_MMX {

    /*
     ===============================================================================

     MMX implementation of idSIMDProcessor

     ===============================================================================
     */
    static class idSIMD_MMX extends idSIMD_Generic {

        private static final boolean __ASM_ENABLED = false;
//public:
//#if defined(MACOS_X) && defined(__i386__)
//	virtual const char * VPCALL GetName( void ) const;
//
//#elif defined(_WIN32)
//

        void MMX_Memcpy8B(Object dest, final Object src, final int count) {
            throw new UnsupportedOperationException("Not supported yet.");
            /*_asm { 
             mov		esi, src 
             mov		edi, dest 
             mov		ecx, count 
             shr		ecx, 3			// 8 bytes per iteration 

             loop1: 
             movq	mm1,  0[ESI]	// Read in source data 
             movntq	0[EDI], mm1		// Non-temporal stores 

             add		esi, 8
             add		edi, 8
             dec		ecx 
             jnz		loop1 

             } 
             EMMS_INSTRUCTION*/
        }

        /*
         ================
         MMX_Memcpy64B

         165MB/sec
         ================
         */
        void MMX_Memcpy64B(Object dest, final Object src, final int count) {
            throw new UnsupportedOperationException("Not supported yet.");
            /*_asm { 
             mov		esi, src 
             mov		edi, dest 
             mov		ecx, count 
             shr		ecx, 6		// 64 bytes per iteration

             loop1: 
             prefetchnta 64[ESI]	// Prefetch next loop, non-temporal 
             prefetchnta 96[ESI] 

             movq mm1,  0[ESI]	// Read in source data 
             movq mm2,  8[ESI] 
             movq mm3, 16[ESI] 
             movq mm4, 24[ESI] 
             movq mm5, 32[ESI] 
             movq mm6, 40[ESI] 
             movq mm7, 48[ESI] 
             movq mm0, 56[ESI] 

             movntq  0[EDI], mm1	// Non-temporal stores 
             movntq  8[EDI], mm2 
             movntq 16[EDI], mm3 
             movntq 24[EDI], mm4 
             movntq 32[EDI], mm5 
             movntq 40[EDI], mm6 
             movntq 48[EDI], mm7 
             movntq 56[EDI], mm0 

             add		esi, 64 
             add		edi, 64 
             dec		ecx 
             jnz		loop1 
             } 
             EMMS_INSTRUCTION*/
        }

        /*
         ================
         MMX_Memcpy2kB

         240MB/sec
         ================
         */
        void MMX_Memcpy2kB(Object dest, final Object src, final int count) {
            throw new UnsupportedOperationException("Not supported yet.");
            /*	byte *tbuf = (byte *)_alloca16(2048);
             __asm { 
             push	ebx
             mov		esi, src
             mov		ebx, count
             shr		ebx, 11		// 2048 bytes at a time 
             mov		edi, dest

             loop2k:
             push	edi			// copy 2k into temporary buffer
             mov		edi, tbuf
             mov		ecx, 32

             loopMemToL1: 
             prefetchnta 64[ESI] // Prefetch next loop, non-temporal
             prefetchnta 96[ESI]

             movq mm1,  0[ESI]	// Read in source data
             movq mm2,  8[ESI]
             movq mm3, 16[ESI]
             movq mm4, 24[ESI]
             movq mm5, 32[ESI]
             movq mm6, 40[ESI]
             movq mm7, 48[ESI]
             movq mm0, 56[ESI]

             movq  0[EDI], mm1	// Store into L1
             movq  8[EDI], mm2
             movq 16[EDI], mm3
             movq 24[EDI], mm4
             movq 32[EDI], mm5
             movq 40[EDI], mm6
             movq 48[EDI], mm7
             movq 56[EDI], mm0
             add		esi, 64
             add		edi, 64
             dec		ecx
             jnz		loopMemToL1

             pop		edi			// Now copy from L1 to system memory
             push	esi
             mov		esi, tbuf
             mov		ecx, 32

             loopL1ToMem:
             movq mm1, 0[ESI]	// Read in source data from L1
             movq mm2, 8[ESI]
             movq mm3, 16[ESI]
             movq mm4, 24[ESI]
             movq mm5, 32[ESI]
             movq mm6, 40[ESI]
             movq mm7, 48[ESI]
             movq mm0, 56[ESI]

             movntq 0[EDI], mm1	// Non-temporal stores
             movntq 8[EDI], mm2
             movntq 16[EDI], mm3
             movntq 24[EDI], mm4
             movntq 32[EDI], mm5
             movntq 40[EDI], mm6
             movntq 48[EDI], mm7
             movntq 56[EDI], mm0

             add		esi, 64
             add		edi, 64
             dec		ecx
             jnz		loopL1ToMem

             pop		esi			// Do next 2k block
             dec		ebx
             jnz		loop2k
             pop		ebx
             }
             EMMS_INSTRUCTION*/
        }

        @Override
        public String GetName() {
            return "MMX";
        }
//

        /*
         ================
         idSIMD_MMX::Memcpy

         optimized memory copy routine that handles all alignment cases and block sizes efficiently
         ================
         */
        @Override
        public void Memcpy(Object[] dest0, Object[] src0, int count0) {

            // if copying more than 16 bytes and we can copy 8 byte aligned
            if (__ASM_ENABLED
                    && count0 > 16 && ((dest0.length ^ src0.length) & 7) == 0) {

                // copy up to the first 8 byte aligned boundary
                int count = dest0.length & 7;
                int pos = count;
                System.arraycopy(src0, pos, dest0, pos, count);
                count = count0 - count;

                // if there are multiple blocks of 2kB
                if ((count & ~4095) != 0) {
                    MMX_Memcpy2kB(dest0, src0, count);
                    pos += (count & ~2047);
                    count &= 2047;
                }

                // if there are blocks of 64 bytes
                if ((count & ~63) != 0) {
                    MMX_Memcpy64B(dest0, src0, count);
                    pos += (count & ~63);
                    count &= 63;
                }

                // if there are blocks of 8 bytes
                if ((count & ~7) != 0) {
                    MMX_Memcpy8B(dest0, src0, count);
                    pos += (count & ~7);
                    count &= 7;
                }

                // copy any remaining bytes
                System.arraycopy(src0, pos, dest0, pos, count);
                throw new UnsupportedOperationException("Not supported yet.");
            } else {
                // use the regular one if we cannot copy 8 byte aligned
                System.arraycopy(src0, 0, dest0, 0, count0);
            }

            // the MMX_Memcpy* functions use MOVNTQ, issue a fence operation
	/*__asm {
             sfence//TODO:sfence
             }*/
        }

        @Override
        public void Memset(Object[] dst0, int val, int count0) {
            class dat {

                byte[] bytes = new byte[8];
                short[] words = new short[4];
                int[] dwords = new int[2];
            };
            dat dat = new dat();

            byte dst = 0;
            int count = count0;

            while (count > 0 && (dst0.length & 7) != 0) {
                dst0[dst++] = val;
                count--;
            }
            if (0 == count) {
                return;
            }

            dat.bytes[0] = (byte) val;
            dat.bytes[1] = (byte) val;
            dat.words[1] = dat.words[0];
            dat.dwords[1] = dat.dwords[0];

            if (__ASM_ENABLED && count >= 64) {
                /*__asm {
                 mov edi, dest 
                 mov ecx, count 
                 shr ecx, 6				// 64 bytes per iteration 
                 movq mm1, dat			// Read in source data 
                 movq mm2, mm1
                 movq mm3, mm1
                 movq mm4, mm1
                 movq mm5, mm1
                 movq mm6, mm1
                 movq mm7, mm1
                 movq mm0, mm1
                 loop1: 
                 movntq  0[EDI], mm1		// Non-temporal stores 
                 movntq  8[EDI], mm2 
                 movntq 16[EDI], mm3 
                 movntq 24[EDI], mm4 
                 movntq 32[EDI], mm5 
                 movntq 40[EDI], mm6 
                 movntq 48[EDI], mm7 
                 movntq 56[EDI], mm0 

                 add edi, 64 
                 dec ecx 
                 jnz loop1 
                 }*/
                dst += (count & ~63);
                count &= 63;
            }

            if (__ASM_ENABLED && count >= 8) {
                /*__asm {
                 mov edi, dest 
                 mov ecx, count 
                 shr ecx, 3				// 8 bytes per iteration 
                 movq mm1, dat			// Read in source data 
                 loop2: 
                 movntq  0[EDI], mm1		// Non-temporal stores 

                 add edi, 8
                 dec ecx 
                 jnz loop2
                 }*/
                dst += (count & ~7);
                count &= 7;
            }

            while (count > 0) {
                dst0[dst] = val;
                dst++;
                count--;
            }

            /*EMMS_INSTRUCTION 

             // the MMX_Memcpy* functions use MOVNTQ, issue a fence operation
             __asm {
             sfence
             }*/
        }
//#endif
    };
}
