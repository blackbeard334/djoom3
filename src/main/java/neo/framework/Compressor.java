package neo.framework;

import java.nio.ByteBuffer;
import java.util.Arrays;
import static neo.framework.Common.common;
import neo.framework.File_h.fsOrigin_t;
import neo.framework.File_h.idFile;
import neo.idlib.Lib;
import neo.idlib.Lib.idException;
import neo.idlib.containers.HashIndex.idHashIndex;

/**
 *
 */
public class Compressor {

    /*
     ===============================================================================

     idCompressor is a layer ontop of idFile which provides lossless data
     compression. The compressor can be used as a regular file and multiple
     compressors can be stacked ontop of each other.

     ===============================================================================
     */
    public static abstract class idCompressor extends idFile {

        // compressor allocation
        public static idCompressor AllocNoCompression() {
            return new idCompressor_None();
        }

        public static idCompressor AllocBitStream() {
            return new idCompressor_BitStream();
        }

        public static idCompressor AllocRunLength() {
            return new idCompressor_RunLength();
        }

        public static idCompressor AllocRunLength_ZeroBased() {
            return new idCompressor_RunLength_ZeroBased();
        }

        public static idCompressor AllocHuffman() {
            return new idCompressor_Huffman();
        }

        public static idCompressor AllocArithmetic() {
            return new idCompressor_Arithmetic();
        }

        public static idCompressor AllocLZSS() {
            return new idCompressor_LZSS();
        }

        public static idCompressor AllocLZSS_WordAligned() {
            return new idCompressor_LZSS_WordAligned();
        }

        public static idCompressor AllocLZW() {
            return new idCompressor_LZW();
        }
//

        // initialization
        public abstract void Init(idFile f, boolean compress, int wordLength);

        public abstract void FinishCompress();

        public abstract float GetCompressionRatio();
//

        // common idFile interface
        @Override
        public String GetName() {
            return super.GetName();
        }

        @Override
        public String GetFullPath() {
            return super.GetFullPath();
        }

        @Override
        public int Read(ByteBuffer buffer) {
            return super.Read(buffer);
        }

        @Override
        public int Write(ByteBuffer buffer) {
            return super.Write(buffer);
        }

        @Override
        public int Length() {
            return super.Length();
        }

        @Override
        public long Timestamp() {
            return super.Timestamp();
        }

        @Override
        public int Tell() {
            return super.Tell();
        }

        @Override
        public void ForceFlush() {
            super.ForceFlush();
        }

        @Override
        public void Flush() {
            super.Flush();
        }

        @Override
        public boolean Seek(long offset, fsOrigin_t origin) throws idException {
            return super.Seek(offset, origin);
        }
    };

    /*
     =================================================================================

     idCompressor_None

     =================================================================================
     */
    static class idCompressor_None extends idCompressor {

        public idCompressor_None() {
            file = null;
            compress = true;
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {
            this.file = f;
            this.compress = compress;
        }

        @Override
        public void FinishCompress() {
        }

        @Override
        public float GetCompressionRatio() {
            return 0.0f;
        }

        @Override
        public String GetName() {
            if (file != null) {
                return file.GetName();
            } else {
                return "";
            }
        }

        @Override
        public String GetFullPath() {
            if (file != null) {
                return file.GetFullPath();
            } else {
                return "";
            }
        }

        @Override
        public int Read(ByteBuffer outData) {
            if (compress == true /*|| outLength <= 0*/) {
                return 0;
            }
            return file.Read(outData);
        }

        @Override
        public int Write(ByteBuffer inData) {
            if (compress == false) {
                return 0;
            }
            return file.Write(inData);
        }

        @Override
        public int Length() {
            if (file != null) {
                return file.Length();
            } else {
                return 0;
            }
        }

        @Override
        public long Timestamp() {
            if (file != null) {
                return file.Timestamp();
            } else {
                return 0;
            }
        }

        @Override
        public int Tell() {
            if (file != null) {
                return file.Tell();
            } else {
                return 0;
            }
        }

        @Override
        public void ForceFlush() {
            if (file != null) {
                file.ForceFlush();
            }
        }

        @Override
        public void Flush() {
            if (file != null) {
                file.ForceFlush();
            }
        }

        @Override
        public boolean Seek(long offset, fsOrigin_t origin) throws idException {
            common.Error("cannot seek on idCompressor");
            return false;//-1;
        }
//
//
        protected idFile file;
        protected boolean compress;
    };

    /*
     =================================================================================

     idCompressor_BitStream

     Base class for bit stream compression.

     =================================================================================
     */
    static class idCompressor_BitStream extends idCompressor_None {

        protected ByteBuffer buffer = ByteBuffer.allocate(65536);
        protected int wordLength;
        //
        protected int readTotalBytes;
        protected int readLength;
        protected int readByte;
        protected int readBit;
        protected ByteBuffer readData;//= new byte[1];
        //
        protected int writeTotalBytes;
        protected int writeLength;
        protected int writeByte;
        protected int writeBit;
        protected ByteBuffer writeData;//= new byte[1];
        //
        //

        public idCompressor_BitStream() {
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {

            assert (wordLength >= 1 && wordLength <= 32);

            this.file = f;
            this.compress = compress;
            this.wordLength = wordLength;

            readTotalBytes = 0;
            readLength = 0;
            readByte = 0;
            readBit = 0;
            readData = null;

            writeTotalBytes = 0;
            writeLength = 0;
            writeByte = 0;
            writeBit = 0;
            writeData = null;
        }

        @Override
        public void FinishCompress() {
            if (compress == false) {
                return;
            }

            if (writeByte != 0) {//TODO:wtf?
                file.Write(buffer, writeByte);
            }
            writeLength = 0;
            writeByte = 0;
            writeBit = 0;
        }

        @Override
        public float GetCompressionRatio() {
            if (compress) {
                return (readTotalBytes - writeTotalBytes) * 100.0f / readTotalBytes;
            } else {
                return (writeTotalBytes - readTotalBytes) * 100.0f / writeTotalBytes;
            }
        }

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int i;

            if (compress == false || inLength <= 0) {
                return 0;
            }

            InitCompress(inData, inLength);

            for (i = 0; i < inLength; i++) {
                WriteBits(ReadBits(8), 8);
            }
            return i;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int i;

            if (compress == true || outLength <= 0) {
                return 0;
            }

            InitDecompress(outData, outLength);

            for (i = 0; i < outLength && readLength >= 0; i++) {
                WriteBits(ReadBits(8), 8);
            }
            return i;
        }

        protected void InitCompress(final ByteBuffer inData, final int inLength) {

            readLength = inLength;
            readByte = 0;
            readBit = 0;
            readData = inData;

            if (0 == writeLength) {
                writeLength = buffer.capacity();
                writeByte = 0;
                writeBit = 0;
                writeData = buffer;
            }
        }

        protected void InitCompress(final byte[] inData, final int inLength) {
            InitCompress(ByteBuffer.wrap(inData), inLength);
        }

        protected void InitDecompress(ByteBuffer outData, int outLength) {

            if (0 == readLength) {
                readLength = file.Read(buffer);
                readByte = 0;
                readBit = 0;
                readData = buffer;
            }

            writeLength = outLength;
            writeByte = 0;
            writeBit = 0;
            writeData = outData;
        }

        protected void InitDecompress(byte[] outData, int outLength) {
            InitDecompress(ByteBuffer.wrap(outData), outLength);
        }

        protected void WriteBits(int value, int numBits) {
            int put, fraction;

            // Short circuit for writing single bytes at a time
            if (writeBit == 0 && numBits == 8 && writeByte < writeLength) {
                writeByte++;
                writeTotalBytes++;
                writeData.putInt(writeByte - 1, value);//TODO:check if inputs should be cast to bytes or stores as INT in this case (4 bytes)
                return;
            }

            while (numBits != 0) {
                if (writeBit == 0) {
                    if (writeByte >= writeLength) {
                        if (writeData == buffer) {
                            file.Write(buffer, writeByte);
                            writeByte = 0;
                        } else {
                            put = numBits;
                            writeBit = put & 7;
                            writeByte += (put >> 3) + (writeBit != 0 ? 1 : 0);
                            writeTotalBytes += (put >> 3) + (writeBit != 0 ? 1 : 0);
                            return;
                        }
                    }
                    writeData.putInt(writeByte, 0);
                    writeByte++;
                    writeTotalBytes++;
                }
                put = 8 - writeBit;
                if (put > numBits) {
                    put = numBits;
                }
                fraction = value & ((1 << put) - 1);
                {
                    final int pos = writeByte - 1;
                    final int val = writeData.getInt(pos) | fraction << writeBit;
                    writeData.putInt(pos, val);
                }
                numBits -= put;
                value >>= put;
                writeBit = (writeBit + put) & 7;
            }
        }

        protected int ReadBits(int numBits) {
            int value, valueBits, get, fraction;

            value = 0;
            valueBits = 0;

            // Short circuit for reading single bytes at a time
            if (readBit == 0 && numBits == 8 && readByte < readLength) {
                readByte++;
                readTotalBytes++;
                return readData.getInt(readByte - 1);
            }

            while (valueBits < numBits) {
                if (readBit == 0) {
                    if (readByte >= readLength) {
                        if (readData == buffer) {
                            readLength = file.Read(buffer);
                            readByte = 0;
                        } else {
                            get = numBits - valueBits;
                            readBit = get & 7;
                            readByte += (get >> 3) + (readBit != 0 ? 1 : 0);
                            readTotalBytes += (get >> 3) + (readBit != 0 ? 1 : 0);
                            return value;
                        }
                    }
                    readByte++;
                    readTotalBytes++;
                }
                get = 8 - readBit;
                if (get > (numBits - valueBits)) {
                    get = (numBits - valueBits);
                }
                fraction = readData.get(readByte - 1);
                fraction >>= readBit;
                fraction &= (1 << get) - 1;
                value |= fraction << valueBits;
                valueBits += get;
                readBit = (readBit + get) & 7;
            }

            return value;
        }

        protected void UnreadBits(int numBits) {
            readByte -= (numBits >> 3);
            readTotalBytes -= (numBits >> 3);
            if (readBit == 0) {
                readBit = 8 - (numBits & 7);
            } else {
                readBit -= numBits & 7;
                if (readBit <= 0) {
                    readByte--;
                    readTotalBytes--;
                    readBit = (readBit + 8) & 7;
                }
            }
            if (readByte < 0) {
                readByte = 0;
                readBit = 0;
            }
        }

        protected int Compare(final byte[] src1, int bitPtr1, final byte[] src2, int bitPtr2, int maxBits) {
            int i;

            // If the two bit pointers are aligned then we can use a faster comparison
            if ((bitPtr1 & 7) == (bitPtr2 & 7) && maxBits > 16) {
                int p1 = bitPtr1 >> 3;
                int p2 = bitPtr2 >> 3;

                int bits = 0;

                int bitsRemain = maxBits;

                // Compare the first couple bits (if any)
                if ((bitPtr1 & 7) != 0) {
                    for (i = (bitPtr1 & 7); i < 8; i++, bits++) {
                        if ((((src1[p1] >> i) ^ (src2[p2] >> i)) & 1) == 1) {
                            return bits;
                        }
                        bitsRemain--;
                    }
                    p1++;
                    p2++;
                }

                int remain = bitsRemain >> 3;

                // Compare the middle bytes as ints
                while (remain >= 4 && ((int) src1[p1] == (int) src2[p2])) {
                    p1 += 4;
                    p2 += 4;
                    remain -= 4;
                    bits += 32;
                }

                // Compare the remaining bytes
                while (remain > 0 && (src1[p1] == src2[p2])) {
                    p1++;
                    p2++;
                    remain--;
                    bits += 8;
                }

                // Compare the last couple of bits (if any)
                int finalBits = 8;
                if (remain == 0) {
                    finalBits = (bitsRemain & 7);
                }
                for (i = 0; i < finalBits; i++, bits++) {
                    if ((((src1[p1] >> i) ^ (src2[p2] >> i)) & 1) == 1) {
                        return bits;
                    }
                }

                assert (bits == maxBits);
                return bits;
            } else {
                for (i = 0; i < maxBits; i++) {
                    if ((((src1[bitPtr1 >> 3] >> (bitPtr1 & 7)) ^ (src2[bitPtr2 >> 3] >> (bitPtr2 & 7))) & 1) == 1) {
                        break;
                    }
                    bitPtr1++;
                    bitPtr2++;
                }
                return i;
            }
        }
    };

    /*
     =================================================================================

     idCompressor_RunLength

     The following algorithm implements run length compression with an arbitrary
     word size.

     =================================================================================
     */
    static class idCompressor_RunLength extends idCompressor_BitStream {

        public idCompressor_RunLength() {
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {
            super.Init(f, compress, wordLength);
            runLengthCode = (1 << wordLength) - 1;
        }

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int bits, nextBits, count;

            if (compress == false || inLength <= 0) {
                return 0;
            }

            InitCompress(inData, inLength);

            while (readByte <= readLength) {
                count = 1;
                bits = ReadBits(wordLength);
                for (nextBits = ReadBits(wordLength); nextBits == bits; nextBits = ReadBits(wordLength)) {
                    count++;
                    if (count >= (1 << wordLength)) {
                        if (count >= (1 << wordLength) + 3 || bits == runLengthCode) {
                            break;
                        }
                    }
                }
                if (nextBits != bits) {
                    UnreadBits(wordLength);
                }
                if (count > 3 || bits == runLengthCode) {
                    WriteBits(runLengthCode, wordLength);
                    WriteBits(bits, wordLength);
                    if (bits != runLengthCode) {
                        count -= 3;
                    }
                    WriteBits(count - 1, wordLength);
                } else {
                    while (count-- != 0) {
                        WriteBits(bits, wordLength);
                    }
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int bits, count;

            if (compress == true || outLength <= 0) {
                return 0;
            }

            InitDecompress(outData, outLength);

            while (writeByte <= writeLength && readLength >= 0) {
                bits = ReadBits(wordLength);
                if (bits == runLengthCode) {
                    bits = ReadBits(wordLength);
                    count = ReadBits(wordLength) + 1;
                    if (bits != runLengthCode) {
                        count += 3;
                    }
                    while (count-- != 0) {
                        WriteBits(bits, wordLength);
                    }
                } else {
                    WriteBits(bits, wordLength);
                }
            }

            return writeByte;
        }
//
//
        private int runLengthCode;
    };

    /*
     =================================================================================

     idCompressor_RunLength_ZeroBased

     The following algorithm implements run length compression with an arbitrary
     word size for data with a lot of zero bits.

     =================================================================================
     */
    static class idCompressor_RunLength_ZeroBased extends idCompressor_BitStream {

        public idCompressor_RunLength_ZeroBased() {
        }

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int bits, count;

            if (compress == false || inLength <= 0) {
                return 0;
            }

            InitCompress(inData, inLength);

            while (readByte <= readLength) {
                count = 0;
                for (bits = ReadBits(wordLength); bits == 0 && count < (1 << wordLength); bits = ReadBits(wordLength)) {
                    count++;
                }
                if (count != 0) {
                    WriteBits(0, wordLength);
                    WriteBits(count - 1, wordLength);
                    UnreadBits(wordLength);
                } else {
                    WriteBits(bits, wordLength);
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int bits, count;

            if (compress == true || outLength <= 0) {
                return 0;
            }

            InitDecompress(outData, outLength);

            while (writeByte <= writeLength && readLength >= 0) {
                bits = ReadBits(wordLength);
                if (bits == 0) {
                    count = ReadBits(wordLength) + 1;
                    while (count-- > 0) {
                        WriteBits(0, wordLength);
                    }
                } else {
                    WriteBits(bits, wordLength);
                }
            }

            return writeByte;
        }
    };
    static final int HMAX = 256;				// Maximum symbol
    static final int NYT = HMAX;				// NYT = Not Yet Transmitted
    static final int INTERNAL_NODE = HMAX + 1;			// internal node

    class nodetype {

        nodetype left, right, parent; // tree structure
        nodetype next, prev;			// doubly-linked list
        nodetype head;					// highest ranked node in block
        int weight;
        int symbol;
    };

    class huffmanNode_t extends nodetype {
    };

    /*
     =================================================================================

     idCompressor_Huffman

     The following algorithm is based on the adaptive Huffman algorithm described
     in Sayood's Data Compression book. The ranks are not actually stored, but
     implicitly defined by the location of a node within a doubly-linked list

     =================================================================================
     */
    static class idCompressor_Huffman extends idCompressor_None {

        private ByteBuffer seq = ByteBuffer.allocate(65536);//TODO:allocateDirect?
        private int bloc;
        private int blocMax;
        private int blocIn;
        private int blocNode;
        private int blocPtrs;
        //
        private int compressedSize;
        private int unCompressedSize;
        //
        private huffmanNode_t tree;
        private huffmanNode_t lhead;
        private huffmanNode_t ltail;
        private huffmanNode_t[] loc = new huffmanNode_t[HMAX + 1];
        private huffmanNode_t[] freelist;
        //
        private huffmanNode_t[] nodeList = new huffmanNode_t[768];
        private huffmanNode_t[] nodePtrs = new huffmanNode_t[768];
        //
        //

        public idCompressor_Huffman() {
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {
            int i;

            this.file = f;
            this.compress = compress;
            bloc = 0;
            blocMax = 0;
            blocIn = 0;
            blocNode = 0;
            blocPtrs = 0;
            compressedSize = 0;
            unCompressedSize = 0;

            tree = null;
            lhead = null;
            ltail = null;
            for (i = 0; i < (HMAX + 1); i++) {
                loc[i] = null;
            }
            freelist = null;

            for (i = 0; i < 768; i++) {
//		memset( &nodeList[i], 0, sizeof(huffmanNode_t) );
                nodePtrs[i] = null;
            }

            if (compress) {
                // Add the NYT (not yet transmitted) node into the tree/list
                tree = lhead = loc[NYT] = nodeList[blocNode++];
                tree.symbol = NYT;
                tree.weight = 0;
                lhead.next = lhead.prev = null;
                tree.parent = tree.left = tree.right = null;
                loc[NYT] = tree;
            } else {
                // Initialize the tree & list with the NYT node 
                tree = lhead = ltail = loc[NYT] = nodeList[blocNode++];
                tree.symbol = NYT;
                tree.weight = 0;
                lhead.next = lhead.prev = null;
                tree.parent = tree.left = tree.right = null;
            }
        }

        @Override
        public void FinishCompress() {

            if (compress == false) {
                return;
            }

            bloc += 7;
            int str = (bloc >> 3);
            if (str != 0) {
                file.Write(seq, str);
                compressedSize += str;
            }
        }

        @Override
        public float GetCompressionRatio() {
            return (unCompressedSize - compressedSize) * 100.0f / unCompressedSize;
        }
//

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int i;
            int ch;

            if (compress == false || inLength <= 0) {
                return 0;
            }

            for (i = 0; i < inLength; i++) {
                ch = inData.getInt(i);
                Transmit(ch, seq);  // Transmit symbol 
                AddRef((byte) ch);         // Do update 

                int b = (bloc >> 3);
                if (b > 32768) {
                    file.Write(seq, b);
                    seq.put(0, seq.get(b));
                    bloc &= 7;
                    compressedSize += b;
                }
            }

            unCompressedSize += i;
            return i;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int i, j;
            int[] ch = new int[1];

            if (compress == true || outLength <= 0) {
                return 0;
            }

            if (bloc == 0) {
                blocMax = file.Read(seq);
                blocIn = 0;
            }

            for (i = 0; i < outLength; i++) {
                ch[0] = 0;
                // don't overflow reading from the file
                if ((bloc >> 3) > blocMax) {
                    break;
                }
                Receive(tree, ch);		// Get a character 
                if (ch[0] == NYT) {		// We got a NYT, get the symbol associated with it

                    ch[0] = 0;
                    for (j = 0; j < 8; j++) {
                        ch[0] = (ch[0] << 1) + Get_bit();
                    }
                }

                outData.putInt(i, ch[0]);		// Write symbol 
                AddRef((byte) ch[0]);				// Increment node 
            }

            compressedSize = bloc >> 3;
            unCompressedSize += i;
            return i;
        }

        private void AddRef(byte ch) {
            huffmanNode_t tnode, tnode2;
            if (loc[ch] == null) { /* if this is the first transmission of this node */

                tnode = nodeList[blocNode++];
                tnode2 = nodeList[blocNode++];

                tnode2.symbol = INTERNAL_NODE;
                tnode2.weight = 1;
                tnode2.next = lhead.next;
                if (lhead.next != null) {
                    lhead.next.prev = tnode2;
                    if (lhead.next.weight == 1) {
                        tnode2.head = lhead.next.head;
                    } else {
                        tnode2.head = Get_ppnode();
                        tnode2.head = tnode2;
                    }
                } else {
                    tnode2.head = Get_ppnode();
                    tnode2.head = tnode2;
                }
                lhead.next = tnode2;
                tnode2.prev = lhead;

                tnode.symbol = ch;
                tnode.weight = 1;
                tnode.next = lhead.next;
                if (lhead.next != null) {
                    lhead.next.prev = tnode;
                    if (lhead.next.weight == 1) {
                        tnode.head = lhead.next.head;
                    } else {
                        /* this should never happen */
                        tnode.head = Get_ppnode();
                        tnode.head = tnode2;
                    }
                } else {
                    /* this should never happen */
                    tnode.head = Get_ppnode();
                    tnode.head = tnode;
                }
                lhead.next = tnode;
                tnode.prev = lhead;
                tnode.left = tnode.right = null;

                if (lhead.parent != null) {
                    if (lhead.parent.left == lhead) { /* lhead is guaranteed to by the NYT */

                        lhead.parent.left = tnode2;
                    } else {
                        lhead.parent.right = tnode2;
                    }
                } else {
                    tree = tnode2;
                }

                tnode2.right = tnode;
                tnode2.left = lhead;

                tnode2.parent = lhead.parent;
                lhead.parent = tnode.parent = tnode2;

                loc[ch] = tnode;

                Increment((huffmanNode_t) tnode2.parent);
            } else {
                Increment(loc[ch]);
            }
        }

        /*
         ================
         idCompressor_Huffman::Receive

         Get a symbol.
         ================
         */
        private int Receive(huffmanNode_t node, int[] ch) {
            while (node != null && node.symbol == INTERNAL_NODE) {
                if (Get_bit() != 0) {
                    node = (huffmanNode_t) node.right;
                } else {
                    node = (huffmanNode_t) node.left;
                }
            }
            if (null == node) {
                return 0;
            }
            return (ch[0] = node.symbol);
        }

        /*
         ================
         idCompressor_Huffman::Transmit

         Send a symbol.
         ================
         */
        private void Transmit(int ch, ByteBuffer fout) {
            int i;
            if (loc[ch] == null) {
                /* huffmanNode_t hasn't been transmitted, send a NYT, then the symbol */
                Transmit(NYT, fout);
                for (i = 7; i >= 0; i--) {
                    Add_bit((char) ((ch >> i) & 0x1), fout);
                }
            } else {
                Send(loc[ch], null, fout);
            }
        }

        private void PutBit(int bit, byte[] fout, int[] offset) {
            bloc = offset[0];
            if ((bloc & 7) == 0) {
                fout[(bloc >> 3)] = 0;
            }
            fout[(bloc >> 3)] |= bit << (bloc & 7);
            bloc++;
            offset[0] = bloc;
        }

        private int GetBit(byte[] fin, int[] offset) {
            int t;
            bloc = offset[0];
            t = (fin[(bloc >> 3)] >> (bloc & 7)) & 0x1;
            bloc++;
            offset[0] = bloc;
            return t;
        }
//


        /*
         ================
         idCompressor_Huffman::Add_bit

         Add a bit to the output file (buffered)
         ================
         */
        private void Add_bit(int bit, ByteBuffer fout) {
            final int pos = bloc >> 3;
            final int val = bit << (bloc & 7);

            if ((bloc & 7) == 0) {
                fout.putInt(pos, 0);
            }
            fout.putInt(pos, val);
            bloc++;
        }

        /*
         ================
         idCompressor_Huffman::Get_bit

         Get one bit from the input file (buffered)
         ================
         */
        private int Get_bit() {
            int t;
            int wh = bloc >> 3;
            int whb = wh >> 16;
            if (whb != blocIn) {
                blocMax += file.Read(seq/*, sizeof( seq )*/);
                blocIn++;
            }
            wh &= 0xffff;
            t = (seq.get(wh) >> (bloc & 7)) & 0x1;
            bloc++;
            return t;
        }

        private huffmanNode_t Get_ppnode() {
            final huffmanNode_t tppnode;
            if (null == freelist) {
                return nodePtrs[blocPtrs++];
            } else {
                tppnode = freelist[0];
//                freelist = /*(huffmanNode_t **)**/tppnode;
                return tppnode;
            }
        }

        private void Free_ppnode(huffmanNode_t[] ppnode) {
            ppnode[0] = /*(huffmanNode_t *)*/ freelist[0];//TODO:fix
            freelist = ppnode;
        }

        /*
         ================
         idCompressor_Huffman::Swap

         Swap the location of the given two nodes in the tree.
         ================
         */
        private void Swap(final huffmanNode_t node1, final huffmanNode_t node2) {
            nodetype par1, par2;

            par1 = node1.parent;
            par2 = node2.parent;

            if (par1 != null) {
                if (par1.left == node1) {
                    par1.left = node2;
                } else {
                    par1.right = node2;
                }
            } else {
                tree = node2;
            }

            if (par2 != null) {
                if (par2.left == node2) {
                    par2.left = node1;
                } else {
                    par2.right = node1;
                }
            } else {
                tree = node1;
            }

            node1.parent = par2;
            node2.parent = par1;
        }

        /*
         ================
         idCompressor_Huffman::Swaplist

         Swap the given two nodes in the linked list (update ranks)
         ================
         */
        private void Swaplist(final huffmanNode_t node1, final huffmanNode_t node2) {
            nodetype par1;

            par1 = node1.next;
            node1.next = node2.next;
            node2.next = par1;

            par1 = node1.prev;
            node1.prev = node2.prev;
            node2.prev = par1;

            if (node1.next == node1) {
                node1.next = node2;
            }
            if (node2.next == node2) {
                node2.next = node1;
            }
            if (node1.next != null) {
                node1.next.prev = node1;
            }
            if (node2.next != null) {
                node2.next.prev = node2;
            }
            if (node1.prev != null) {
                node1.prev.next = node1;
            }
            if (node2.prev != null) {
                node2.prev.next = node2;
            }
        }

        private void Increment(final huffmanNode_t node) {
            huffmanNode_t lnode;

            if (null == node) {
                return;
            }

            if (node.next != null && node.next.weight == node.weight) {
                lnode = (huffmanNode_t) node.head;
                if (lnode != node.parent) {
                    Swap(lnode, node);
                }
                Swaplist(lnode, node);
            }
            if (node.prev != null && node.prev.weight == node.weight) {
                node.head = node.prev;
            } else {
                huffmanNode_t[] temp = new huffmanNode_t[1];
                Free_ppnode(temp);
                node.head = temp[0];
            }
            node.weight++;
            if (node.next != null && node.next.weight == node.weight) {
                node.head = node.next.head;
            } else {
                node.head = Get_ppnode();
                node.head = node;
            }
            if (node.parent != null) {
                Increment((huffmanNode_t) node.parent);
                if (node.prev == node.parent) {
                    Swaplist(node, (huffmanNode_t) node.parent);
                    if (node.head == node) {
                        node.head = node.parent;
                    }
                }
            }
        }

        /*
         ================
         idCompressor_Huffman::Send

         Send the prefix code for this node.
         ================
         */
        private void Send(huffmanNode_t node, huffmanNode_t child, ByteBuffer fout) {
            if (node.parent != null) {
                Send((huffmanNode_t) node.parent, node, fout);
            }
            if (child != null) {
                if (node.right == child) {
                    Add_bit(1, fout);
                } else {
                    Add_bit(0, fout);
                }
            }
        }
    };
    static final int AC_WORD_LENGTH = 8;
    static final int AC_NUM_BITS = 16;
    static final int AC_MSB_SHIFT = 15;
    static final int AC_MSB2_SHIFT = 14;
    static final int AC_MSB_MASK = 0x8000;
    static final int AC_MSB2_MASK = 0x4000;
    static final int AC_HIGH_INIT = 0xffff;
    static final int AC_LOW_INIT = 0x0000;

    /*
     =================================================================================

     idCompressor_Arithmetic

     The following algorithm is based on the Arithmetic Coding methods described
     by Mark Nelson. The probability table is implicitly stored.

     =================================================================================
     */
    static class idCompressor_Arithmetic extends idCompressor_BitStream {

        private class acProbs_s {

            long low;
            long high;
        };

        private class acProbs_t extends acProbs_s {
        };

        private class acSymbol_s {

            long low;
            long high;
            int position;
        };

        private class acSymbol_t extends acSymbol_s {
        };

        private acProbs_t[] probabilities = new acProbs_t[1 << AC_WORD_LENGTH];
        //
        private int symbolBuffer;
        private int symbolBit;
        //
        private int low;
        private int high;
        private int code;
        private long underflowBits;
        private long scale;
        //
        //

        public idCompressor_Arithmetic() {
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {
            super.Init(f, compress, wordLength);

            symbolBuffer = 0;
            symbolBit = 0;
        }

        @Override
        public void FinishCompress() {
            if (compress == false) {
                return;
            }

            WriteOverflowBits();

            super.FinishCompress();
        }

        @Override
        public int Write(ByteBuffer inData, int inLength) {
            int i, j;

            if (compress == false || inLength <= 0) {
                return 0;
            }

            InitCompress(inData, inLength);

            for (i = 0; i < inLength; i++) {
                if ((readTotalBytes & ((1 << 14) - 1)) == 0) {
                    if (readTotalBytes != 0) {
                        WriteOverflowBits();
                        WriteBits(0, 15);
                        while (writeBit != 0) {
                            WriteBits(0, 1);
                        }
                        WriteBits(255, 8);
                    }
                    InitProbabilities();
                }
                for (j = 0; j < 8; j++) {
                    PutBit(ReadBits(1));
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int i, j;

            if (compress == true || outLength <= 0) {
                return 0;
            }

            InitDecompress(outData, outLength);

            for (i = 0; i < outLength && readLength >= 0; i++) {
                if ((writeTotalBytes & ((1 << 14) - 1)) == 0) {
                    if (writeTotalBytes != 0) {
                        while (readBit != 0) {
                            ReadBits(1);
                        }
                        while (ReadBits(8) == 0 && readLength > 0) {
                        }
                    }
                    InitProbabilities();
                    for (j = 0; j < AC_NUM_BITS; j++) {
                        code <<= 1;
                        code |= ReadBits(1);
                    }
                }
                for (j = 0; j < 8; j++) {
                    WriteBits(GetBit(), 1);
                }
            }

            return i;
        }

        private void InitProbabilities() {
            high = AC_HIGH_INIT;
            low = AC_LOW_INIT;
            underflowBits = 0;
            code = 0;

            for (int i = 0; i < (1 << AC_WORD_LENGTH); i++) {
                probabilities[i].low = i;
                probabilities[i].high = i + 1;
            }

            scale = (1 << AC_WORD_LENGTH);
        }

        private void UpdateProbabilities(acSymbol_t symbol) {
            int i, x;

            x = symbol.position;

            probabilities[x].high++;

            for (i = x + 1; i < (1 << AC_WORD_LENGTH); i++) {
                probabilities[i].low++;
                probabilities[i].high++;
            }

            scale++;
        }

        private int ProbabilityForCount(long count) {
            if (true) {

                int len, mid, offset, res;

                len = (1 << AC_WORD_LENGTH);
                mid = len;
                offset = 0;
                res = 0;
                while (mid > 0) {
                    mid = len >> 1;
                    if (count >= probabilities[offset + mid].high) {
                        offset += mid;
                        len -= mid;
                        res = 1;
                    } else if (count < probabilities[offset + mid].low) {
                        len -= mid;
                        res = 0;
                    } else {
                        return offset + mid;
                    }
                }
                return offset + res;

            } else {

                int j;

                for (j = 0; j < (1 << AC_WORD_LENGTH); j++) {
                    if (count >= probabilities[j].low && count < probabilities[j].high) {
                        return j;
                    }
                }

                assert (false);

                return 0;

            }
        }
//

        private void CharToSymbol(int c, acSymbol_t symbol) {
            symbol.low = probabilities[c].low;
            symbol.high = probabilities[c].high;
            symbol.position = c;
        }

        private void EncodeSymbol(acSymbol_t symbol) {
            int range;

            // rescale high and low for the new symbol.
            range = (high - low) + 1;
            high = (int) (low + (range * symbol.high) / scale - 1);
            low = (int) (low + (range * symbol.low) / scale);

            while (true) {
                if ((high & AC_MSB_MASK) == (low & AC_MSB_MASK)) {
                    // the high digits of low and high have converged, and can be written to the stream
                    WriteBits(high >> AC_MSB_SHIFT, 1);

                    while (underflowBits > 0) {

                        WriteBits(~high >> AC_MSB_SHIFT, 1);

                        underflowBits--;
                    }
                } else if ((low & AC_MSB2_MASK) != 0 && 0 == (high & AC_MSB2_MASK)) {
                    // underflow is in danger of happening, 2nd digits are converging but 1st digits don't match
                    underflowBits += 1;
                    low &= AC_MSB2_MASK - 1;
                    high |= AC_MSB2_MASK;
                } else {
                    UpdateProbabilities(symbol);
                    return;
                }

                low <<= 1;
                high <<= 1;
                high |= 1;
            }
        }
//

        private int SymbolFromCount(long count, acSymbol_t symbol) {
            int p = ProbabilityForCount(count);
            symbol.low = probabilities[p].low;
            symbol.high = probabilities[p].high;
            symbol.position = p;
            return p;
        }

        private int GetCurrentCount() {
            return (int) (((code - low + 1) * scale - 1) / (high - low + 1));
        }

        private void RemoveSymbolFromStream(acSymbol_t symbol) {
            long range;

            range = (long) (high - low) + 1;
            high = low + (int) ((range * symbol.high) / scale - 1);
            low = low + (int) ((range * symbol.low) / scale);

            while (true) {

                if ((high & AC_MSB_MASK) == (low & AC_MSB_MASK)) {
                } else if ((low & AC_MSB2_MASK) == AC_MSB2_MASK && (high & AC_MSB2_MASK) == 0) {
                    code ^= AC_MSB2_MASK;
                    low &= AC_MSB2_MASK - 1;
                    high |= AC_MSB2_MASK;
                } else {
                    UpdateProbabilities(symbol);
                    return;
                }

                low <<= 1;
                high <<= 1;
                high |= 1;
                code <<= 1;
                code |= ReadBits(1);
            }
        }
//

        private void PutBit(int putbit) {
            symbolBuffer |= (putbit & 1) << symbolBit;
            symbolBit++;

            if (symbolBit >= AC_WORD_LENGTH) {
                acSymbol_t symbol = new acSymbol_t();

                CharToSymbol(symbolBuffer, symbol);
                EncodeSymbol(symbol);

                symbolBit = 0;
                symbolBuffer = 0;
            }
        }

        private int GetBit() {
            int getbit;

            if (symbolBit <= 0) {
                // read a new symbol out
                acSymbol_t symbol = new acSymbol_t();
                symbolBuffer = SymbolFromCount(GetCurrentCount(), symbol);
                RemoveSymbolFromStream(symbol);
                symbolBit = AC_WORD_LENGTH;
            }

            getbit = (symbolBuffer >> (AC_WORD_LENGTH - symbolBit)) & 1;
            symbolBit--;

            return getbit;
        }
//

        private void WriteOverflowBits() {

            WriteBits(low >> AC_MSB2_SHIFT, 1);

            underflowBits++;
            while (underflowBits-- > 0) {
                WriteBits(~low >> AC_MSB2_SHIFT, 1);
            }
        }
    };
    static final int LZSS_BLOCK_SIZE = 65535;
    static final int LZSS_HASH_BITS = 10;
    static final int LZSS_HASH_SIZE = (1 << LZSS_HASH_BITS);
    static final int LZSS_HASH_MASK = (1 << LZSS_HASH_BITS) - 1;
    static final int LZSS_OFFSET_BITS = 11;
    static final int LZSS_LENGTH_BITS = 5;

    /*
     =================================================================================

     idCompressor_LZSS

     In 1977 Abraham Lempel and Jacob Ziv presented a dictionary based scheme for
     text compression called LZ77. For any new text LZ77 outputs an offset/length
     pair to previously seen text and the next new byte after the previously seen
     text.

     In 1982 James Storer and Thomas Szymanski presented a modification on the work
     of Lempel and Ziv called LZSS. LZ77 always outputs an offset/length pair, even
     if a match is only one byte long. An offset/length pair usually takes more than
     a single byte to store and the compression is not optimal for small match sizes.
     LZSS uses a bit flag which tells whether the following data is a literal (byte)
     or an offset/length pair.

     The following algorithm is an implementation of LZSS with arbitrary word size.

     =================================================================================
     */
    static class idCompressor_LZSS extends idCompressor_BitStream {

        protected int offsetBits;
        protected int lengthBits;
        protected int minMatchWords;
        //
        protected byte[] block = new byte[LZSS_BLOCK_SIZE];
        protected int blockSize;
        protected int blockIndex;
        //
        protected int[] hashTable = new int[LZSS_HASH_SIZE];
        protected int[] hashNext = new int[LZSS_BLOCK_SIZE * 8];
        //
        //

        public idCompressor_LZSS() {
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {
            super.Init(f, compress, wordLength);

            offsetBits = LZSS_OFFSET_BITS;
            lengthBits = LZSS_LENGTH_BITS;

            minMatchWords = (offsetBits + lengthBits + wordLength) / wordLength;
            blockSize = 0;
            blockIndex = 0;
        }

        @Override
        public void FinishCompress() {
            if (compress == false) {
                return;
            }
            if (blockSize != 0) {
                CompressBlock();
            }
            super.FinishCompress();
        }

//
        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int i, n;

            if (compress == false || inLength <= 0) {
                return 0;
            }

            for (n = i = 0; i < inLength; i += n) {
                n = LZSS_BLOCK_SIZE - blockSize;
                if (inLength - i >= n) {
//			memcpy( block + blockSize, ((const byte *)inData) + i, n );
                    inData.get(block, i, n);
                    blockSize = LZSS_BLOCK_SIZE;
                    CompressBlock();
                    blockSize = 0;
                } else {
//			memcpy( block + blockSize, ((const byte *)inData) + i, inLength - i );
                    System.arraycopy(inData.array(), i, block, blockSize, inLength - i);
                    n = inLength - i;
                    blockSize += n;
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int i, n;

            if (compress == true || outLength <= 0) {
                return 0;
            }

            if (0 == blockSize) {
                DecompressBlock();
            }

            for (n = i = 0; i < outLength; i += n) {
                if (0 == blockSize) {
                    return i;
                }
                n = blockSize - blockIndex;
                if (outLength - i >= n) {
//			memcpy( ((byte *)outData) + i, block + blockIndex, n );
                    System.arraycopy(block, blockIndex, outData.array(), i, n);
                    DecompressBlock();
                    blockIndex = 0;
                } else {
//			memcpy( ((byte *)outData) + i, block + blockIndex, outLength - i );
                    System.arraycopy(block, blockIndex, outData.array(), i, outLength - i);
                    n = outLength - i;
                    blockIndex += n;
                }
            }

            return outLength;
        }

        protected boolean FindMatch(int startWord, int startValue, int[] wordOffset, int[] numWords) {
            int i, n, hash, bottom, maxBits;

            wordOffset[0] = startWord;
            numWords[0] = minMatchWords - 1;

            bottom = Lib.Max(0, startWord - ((1 << offsetBits) - 1));
            maxBits = (blockSize << 3) - startWord * wordLength;

            hash = startValue & LZSS_HASH_MASK;
            for (i = hashTable[hash]; i >= bottom; i = hashNext[i]) {
                n = Compare(block, i * wordLength, block, startWord * wordLength, Lib.Min(maxBits, (startWord - i) * wordLength));
                if (n > numWords[0] * wordLength) {
                    numWords[0] = n / wordLength;
                    wordOffset[0] = i;
                    if (numWords[0] > ((1 << lengthBits) - 1 + minMatchWords) - 1) {
                        numWords[0] = ((1 << lengthBits) - 1 + minMatchWords) - 1;
                        break;
                    }
                }
            }

            return (numWords[0] >= minMatchWords);
        }

        protected void AddToHash(int index, int hash) {
            hashNext[index] = hashTable[hash];
            hashTable[hash] = index;
        }

        protected int GetWordFromBlock(int wordOffset) {
            int blockBit, blockByte, value, valueBits, get, fraction;

            blockBit = (wordOffset * wordLength) & 7;
            blockByte = (wordOffset * wordLength) >> 3;
            if (blockBit != 0) {
                blockByte++;
            }

            value = 0;
            valueBits = 0;

            while (valueBits < wordLength) {
                if (blockBit == 0) {
                    if (blockByte >= LZSS_BLOCK_SIZE) {
                        return value;
                    }
                    blockByte++;
                }
                get = 8 - blockBit;
                if (get > (wordLength - valueBits)) {
                    get = (wordLength - valueBits);
                }
                fraction = block[blockByte - 1];
                fraction >>= blockBit;
                fraction &= (1 << get) - 1;
                value |= fraction << valueBits;
                valueBits += get;
                blockBit = (blockBit + get) & 7;
            }

            return value;
        }

        protected void CompressBlock() {
            int i, startWord, startValue;
            int[] wordOffset = new int[1], numWords = new int[1];

            InitCompress(block, blockSize);

//	memset( hashTable, -1, sizeof( hashTable ) );
//	memset( hashNext, -1, sizeof( hashNext ) );
            Arrays.fill(hashTable, -1);
            Arrays.fill(hashNext, -1);

            startWord = 0;
            while (readByte < readLength) {
                startValue = ReadBits(wordLength);
                if (FindMatch(startWord, startValue, wordOffset, numWords)) {
                    WriteBits(1, 1);
                    WriteBits(startWord - wordOffset[0], offsetBits);
                    WriteBits(numWords[0] - minMatchWords, lengthBits);
                    UnreadBits(wordLength);
                    for (i = 0; i < numWords[0]; i++) {
                        startValue = ReadBits(wordLength);
                        AddToHash(startWord, startValue & LZSS_HASH_MASK);
                        startWord++;
                    }
                } else {
                    WriteBits(0, 1);
                    WriteBits(startValue, wordLength);
                    AddToHash(startWord, startValue & LZSS_HASH_MASK);
                    startWord++;
                }
            }

            blockSize = 0;
        }

        protected void DecompressBlock() {
            int i, offset, startWord, numWords;

            InitDecompress(block, LZSS_BLOCK_SIZE);

            startWord = 0;
            while (writeByte < writeLength && readLength >= 0) {
                if (ReadBits(1) != 0) {
                    offset = startWord - ReadBits(offsetBits);
                    numWords = ReadBits(lengthBits) + minMatchWords;
                    for (i = 0; i < numWords; i++) {
                        WriteBits(GetWordFromBlock(offset + i), wordLength);
                        startWord++;
                    }
                } else {
                    WriteBits(ReadBits(wordLength), wordLength);
                    startWord++;
                }
            }

            blockSize = Lib.Min(writeByte, LZSS_BLOCK_SIZE);
        }
    };

    /*
     =================================================================================

     idCompressor_LZSS_WordAligned

     Outputs word aligned compressed data.

     =================================================================================
     */
    static class idCompressor_LZSS_WordAligned extends idCompressor_LZSS {

        public idCompressor_LZSS_WordAligned() {
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {
            super.Init(f, compress, wordLength);

            offsetBits = 2 * wordLength;
            lengthBits = wordLength;

            minMatchWords = (offsetBits + lengthBits + wordLength) / wordLength;
            blockSize = 0;
            blockIndex = 0;
        }

        @Override
        protected void CompressBlock() {
            int i, startWord, startValue;
            final int[] wordOffset = new int[1], numWords = new int[1];

            InitCompress(block, blockSize);

//	memset( hashTable, -1, sizeof( hashTable ) );
//	memset( hashNext, -1, sizeof( hashNext ) );
            Arrays.fill(hashTable, -1);
            Arrays.fill(hashNext, -1);

            startWord = 0;
            while (readByte < readLength) {
                startValue = ReadBits(wordLength);
                if (FindMatch(startWord, startValue, wordOffset, numWords)) {
                    WriteBits(numWords[0] - (minMatchWords - 1), lengthBits);
                    WriteBits(startWord - wordOffset[0], offsetBits);
                    UnreadBits(wordLength);
                    for (i = 0; i < numWords[0]; i++) {
                        startValue = ReadBits(wordLength);
                        AddToHash(startWord, startValue & LZSS_HASH_MASK);
                        startWord++;
                    }
                } else {
                    WriteBits(0, lengthBits);
                    WriteBits(startValue, wordLength);
                    AddToHash(startWord, startValue & LZSS_HASH_MASK);
                    startWord++;
                }
            }

            blockSize = 0;
        }

        @Override
        protected void DecompressBlock() {
            int i, offset, startWord, numWords;

            InitDecompress(block, LZSS_BLOCK_SIZE);

            startWord = 0;
            while (writeByte < writeLength && readLength >= 0) {
                numWords = ReadBits(lengthBits);
                if (numWords != 0) {
                    numWords += (minMatchWords - 1);
                    offset = startWord - ReadBits(offsetBits);
                    for (i = 0; i < numWords; i++) {
                        WriteBits(GetWordFromBlock(offset + i), wordLength);
                        startWord++;
                    }
                } else {
                    WriteBits(ReadBits(wordLength), wordLength);
                    startWord++;
                }
            }

            blockSize = Lib.Min(writeByte, LZSS_BLOCK_SIZE);
        }
    };

    /*
     =================================================================================

     idCompressor_LZW

     http://www.unisys.com/about__unisys/lzw
     http://www.dogma.net/markn/articles/lzw/lzw.htm
     http://www.cs.cf.ac.uk/Dave/Multimedia/node214.html
     http://www.cs.duke.edu/csed/curious/compression/lzw.html
     http://oldwww.rasip.fer.hr/research/compress/algorithms/fund/lz/lzw.html

     This is the same compression scheme used by GIF with the exception that
     the EOI and clear codes are not explicitly stored.  Instead EOI happens
     when the input stream runs dry and CC happens when the table gets to big.

     This is a derivation of LZ78, but the dictionary starts with all single
     character values so only code words are output.  It is similar in theory
     to LZ77, but instead of using the previous X bytes as a lookup table, a table
     is built as the stream is read.  The	compressor and decompressor use the
     same formula, so the tables should be exactly alike.  The only catch is the
     decompressor is always one step behind the compressor and may get a code not
     yet in the table.  In this case, it is easy to determine what the next code
     is going to be (it will be the previous string plus the first byte of the
     previous string).

     The dictionary can be any size, but 12 bits seems to produce best results for
     most sample data.  The code size is variable.  It starts with the minimum
     number of bits required to store the dictionary and automatically increases
     as the dictionary gets bigger (it starts at 9 bits and grows to 10 bits when
     item 512 is added, 11 bits when 1024 is added, etc...) once the the dictionary
     is filled (4096 items for a 12 bit dictionary), the whole thing is cleared and
     the process starts over again.

     The compressor increases the bit size after it adds the item, while the
     decompressor does before it adds the item.  The difference is subtle, but
     it's because decompressor being one step behind.  Otherwise, the decompressor
     would read 512 with only 9 bits.

     If "Hello" is in the dictionary, then "Hell", "Hel", "He" and "H" will be too.
     We use this to our advantage by storing the index of the previous code, and
     the value of the last character.  This means when we traverse through the
     dictionary, we get the characters in reverse.

     Dictionary entries 0-255 are always going to have the values 0-255

     =================================================================================
     */
    static class idCompressor_LZW extends idCompressor_BitStream {

        protected static final int LZW_BLOCK_SIZE = 32767;
        protected static final int LZW_START_BITS = 9;
        protected static final int LZW_FIRST_CODE = (1 << (LZW_START_BITS - 1));
        protected static final int LZW_DICT_BITS = 12;
        protected static final int LZW_DICT_SIZE = 1 << LZW_DICT_BITS;
        //
        //
        // Dictionary data

        protected class dictionary {

            int k;
            int w;
        };
        protected dictionary[] dictionary = new dictionary[LZW_DICT_SIZE];
        protected idHashIndex index;
        //
        protected int nextCode;
        protected int codeBits;
        //
        // Block data
        protected byte[] block = new byte[LZW_BLOCK_SIZE];
        protected int blockSize;
        protected int blockIndex;
        //
        // Used by the compressor
        protected int w;
        //
        // Used by the decompressor
        protected int oldCode;
        //
        //

        public idCompressor_LZW() {
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {
            super.Init(f, compress, wordLength);

            for (int i = 0; i < LZW_FIRST_CODE; i++) {
                dictionary[i].k = i;
                dictionary[i].w = -1;
            }
            index.Clear();

            nextCode = LZW_FIRST_CODE;
            codeBits = LZW_START_BITS;

            blockSize = 0;
            blockIndex = 0;

            w = -1;
            oldCode = -1;
        }

        @Override
        public void FinishCompress() {
            WriteBits(w, codeBits);
            super.FinishCompress();
        }

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int i;

            InitCompress(inData, inLength);

            for (i = 0; i < inLength; i++) {
                int k = ReadBits(8);

                int code = Lookup(w, k);
                if (code >= 0) {
                    w = code;
                } else {
                    WriteBits(w, codeBits);
                    if (!BumpBits()) {
                        AddToDict(w, k);
                    }
                    w = k;
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int i, n;

            if (compress == true || outLength <= 0) {
                return 0;
            }

            if (0 == blockSize) {
                DecompressBlock();
            }

            for (n = i = 0; i < outLength; i += n) {
                if (0 == blockSize) {
                    return i;
                }
                n = blockSize - blockIndex;
                if (outLength - i >= n) {
//			memcpy( ((byte *)outData) + i, block + blockIndex, n );
                    System.arraycopy(block, blockIndex, outData.array(), i, n);
                    DecompressBlock();
                    blockIndex = 0;
                } else {
//			memcpy( ((byte *)outData) + i, block + blockIndex, outLength - i );
                    System.arraycopy(block, blockIndex, outData.array(), i, outLength - i);
                    n = outLength - i;
                    blockIndex += n;
                }
            }

            return outLength;
        }

        protected int AddToDict(int w, int k) {
            dictionary[nextCode].k = k;
            dictionary[nextCode].w = w;
            index.Add(w ^ k, nextCode);
            return nextCode++;
        }

        protected int Lookup(int w, int k) {
            int j;

            if (w == -1) {
                return k;
            } else {
                for (j = index.First(w ^ k); j >= 0; j = index.Next(j)) {
                    if (dictionary[j].k == k && dictionary[j].w == w) {
                        return j;
                    }
                }
            }

            return -1;
        }


        /*
         ================
         idCompressor_LZW::BumpBits

         Possibly increments codeBits
         Returns true if the dictionary was cleared
         ================
         */
        protected boolean BumpBits() {
            if (nextCode == (1 << codeBits)) {
                codeBits++;
                if (codeBits > LZW_DICT_BITS) {
                    nextCode = LZW_FIRST_CODE;
                    codeBits = LZW_START_BITS;
                    index.Clear();
                    return true;
                }
            }
            return false;
        }


        /*
         ================
         idCompressor_LZW::WriteCain
         The chain is stored backwards, so we have to write it to a buffer then output the buffer in reverse
         ================
         */
        protected int WriteChain(int code) {
            byte[] chain = new byte[LZW_DICT_SIZE];
            int firstChar = 0;
            int i = 0;
            do {
                assert (i < LZW_DICT_SIZE - 1 && code >= 0);
                chain[i++] = (byte) dictionary[code].k;
                code = dictionary[code].w;
            } while (code >= 0);
            firstChar = chain[--i];
            for (; i >= 0; i--) {
                WriteBits(chain[i], 8);
            }
            return firstChar;
        }

        protected void DecompressBlock() {
            int code, firstChar;

            InitDecompress(block, LZW_BLOCK_SIZE);

            while (writeByte < writeLength - LZW_DICT_SIZE && readLength > 0) {
                assert (codeBits <= LZW_DICT_BITS);

                code = ReadBits(codeBits);
                if (readLength == 0) {
                    break;
                }

                if (oldCode == -1) {
                    assert (code < 256);
                    WriteBits(code, 8);
                    oldCode = code;
                    firstChar = code;
                    continue;
                }

                if (code >= nextCode) {
                    assert (code == nextCode);
                    firstChar = WriteChain(oldCode);
                    WriteBits(firstChar, 8);
                } else {
                    firstChar = WriteChain(code);
                }
                AddToDict(oldCode, firstChar);
                if (BumpBits()) {
                    oldCode = -1;
                } else {
                    oldCode = code;
                }
            }

            blockSize = Lib.Min(writeByte, LZW_BLOCK_SIZE);
        }
    };
}
