package neo.idlib.Text;

import static neo.idlib.Lib.IntForSixtets;
import static neo.idlib.Lib.SixtetsForInt;

import java.nio.ByteBuffer;

import neo.framework.File_h.idFile;
import neo.idlib.Text.Str.idStr;

/**
 *
 */
public class Base64 {

    /*
     ===============================================================================

     base64

     ===============================================================================
     */
    public static class idBase64 {

        public idBase64() {
//            Init();
        }

        public idBase64(final idStr s) {
//            Init();
            this.data = s.getData().getBytes();
//            this.len = s.Length();
//            this.alloced = s.alloced;
        }
//public				~idBase64( void );
//
        static final char[] sixtet_to_base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

        public void Encode(final byte[] from, int size) {
            final int[] from2 = new int[size];
            System.arraycopy(from, 0, from2, 0, size);

            Encode(from2, size);
        }

        public void Encode(final ByteBuffer from, int size) {
            Encode(from.array(), size);
        }

        public void Encode(final char[] from, int size) {
            final int[] from2 = new int[size];
            System.arraycopy(from, 0, from2, 0, size);

            Encode(from2, size);
        }

        public void Encode(final int[] from, int size) {
            int i, j;
            long w;
            byte[] to;
            int f_ptr = 0, t_ptr = 0;

            EnsureAlloced(((4 * (size + 3)) / 3) + 2); // ratio and padding + trailing \0
            to = this.data;

            w = 0;
            i = 0;
            while (size > 0) {
                w |= from[f_ptr] << (i * 8);
                ++f_ptr;
                --size;
                ++i;
                if ((size == 0) || (i == 3)) {
                    final byte[] out = new byte[4];
                    SixtetsForInt(out, (int) w);
                    for (j = 0; (j * 6) < (i * 8); ++j) {
                        to[t_ptr++] = (byte) sixtet_to_base64[ out[j]];
                    }
                    if (size == 0) {
                        for (j = i; j < 3; ++j) {
                            to[t_ptr++] = '=';
                        }
                    }
                    w = 0;
                    i = 0;
                }
            }

            to[t_ptr++] = '\0';
            this.len = t_ptr;
        }

        public void Encode(final idStr src) {
            Encode(src.toString().getBytes(), src.Length());
        }

        /*
         ============
         idBase64::DecodeLength
         returns the minimum size in bytes of the target buffer for decoding
         4 base64 digits <-> 3 bytes
         ============
         */
        public int DecodeLength() {// minimum size in bytes of destination buffer for decoding
            return (3 * this.len) / 4;
        }

        public int Decode(byte[] to) {// does not append a \0 - needs a DecodeLength() bytes buffer
            long w;
            int i, j;
            int n;
            final char[] base64_to_sixtet = new char[256];
            boolean tab_init = false;//TODO:useless, remove?
            final byte[] from = this.data;
            int f_ptr = 0, t_ptr = 0;

            if (!tab_init) {
//                memset(base64_to_sixtet, 0, 256);
                for (i = 0; (j = sixtet_to_base64[i]) != '\0'; ++i) {
                    base64_to_sixtet[j] = (char) i;
                }
                tab_init = true;
            }

            w = 0;
            i = 0;
            n = 0;
            final byte[] in = {0, 0, 0, 0};
            while ((from[f_ptr] != '\0') && (from[f_ptr] != '=')) {
                if ((from[f_ptr] == ' ') || (from[f_ptr] == '\n')) {
                    ++f_ptr;
                    continue;
                }
                in[i] = (byte) base64_to_sixtet[from[f_ptr]];
                ++i;
                ++f_ptr;
                if ((from[f_ptr] == '\0') || (from[f_ptr] == '=') || (i == 4)) {
                    w = IntForSixtets(in);
                    for (j = 0; (j * 8) < (i * 6); ++j) {
                        to[t_ptr++] = (byte) (w & 0xff);
                        ++n;
                        w >>= 8;
                    }
                    i = 0;
                    w = 0;
                }
            }
            return n;
        }

        public void Decode(idStr[] dest) {// decodes the binary content to an idStr (a bit dodgy, \0 and other non-ascii are possible in the decoded content) 
            final byte[] buf = new byte[DecodeLength() + 1]; // +1 for trailing \0
            final int out = Decode(buf);
//            buf[out] = '\0';
            dest[0] = new idStr(new String(buf));
//	delete[] buf;
        }

        public void Decode(idFile dest) {
            final ByteBuffer buf = ByteBuffer.allocate(DecodeLength() + 1); // +1 for trailing \0
            final int out = Decode(buf.array());
            dest.Write(buf, out);
//	delete[] buf;
        }

//
        public char[] c_str() {
            return new String(this.data).toCharArray();
        }
//

        public void oSet(final idStr s) {
            EnsureAlloced(s.Length() + 1); // trailing \0 - beware, this does a Release
//	strcpy( (char *)data, s.c_str() );
            this.data = s.getData().getBytes();
            this.len = s.Length();
        }
//
        private byte[] data;
        private int len;
        //private int alloced;
//

//        private void Init() {
//            len = 0;
//            //alloced = 0;
//            data = null;
//        }
//
//        private void Release() {
////	if ( data ) {
////		delete[] data;
////	}
//            Init();
//        }

        private void EnsureAlloced(int size) {
//            if (size > alloced) {
//                Release();
//            }
            this.data = new byte[size];
//            alloced = size;
        }
    }
}
