package neo.framework;

import static neo.framework.Common.common;

import java.nio.ByteBuffer;
import java.util.Arrays;

import neo.TempDump;
import neo.framework.File_h.fsOrigin_t;
import neo.framework.File_h.idFile;
import neo.idlib.Lib;
import neo.idlib.Lib.idException;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.open.Nio;

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
    }

    /*
     =================================================================================

     idCompressor_None

     =================================================================================
     */
    static class idCompressor_None extends idCompressor {

        public idCompressor_None() {
            this.file = null;
            this.compress = true;
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
            if (this.file != null) {
                return this.file.GetName();
            } else {
                return "";
            }
        }

        @Override
        public String GetFullPath() {
            if (this.file != null) {
                return this.file.GetFullPath();
            } else {
                return "";
            }
        }

        @Override
        public int Read(ByteBuffer outData) {
            if (this.compress == true /*|| outLength <= 0*/) {
                return 0;
            }
            return this.file.Read(outData);
        }

        @Override
        public int Write(ByteBuffer inData) {
            if (this.compress == false) {
                return 0;
            }
            return this.file.Write(inData);
        }

        @Override
        public int Length() {
            if (this.file != null) {
                return this.file.Length();
            } else {
                return 0;
            }
        }

        @Override
        public long Timestamp() {
            if (this.file != null) {
                return this.file.Timestamp();
            } else {
                return 0;
            }
        }

        @Override
        public int Tell() {
            if (this.file != null) {
                return this.file.Tell();
            } else {
                return 0;
            }
        }

        @Override
        public void ForceFlush() {
            if (this.file != null) {
                this.file.ForceFlush();
            }
        }

        @Override
        public void Flush() {
            if (this.file != null) {
                this.file.ForceFlush();
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
    }

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

            assert ((wordLength >= 1) && (wordLength <= 32));

            this.file = f;
            this.compress = compress;
            this.wordLength = wordLength;

            this.readTotalBytes = 0;
            this.readLength = 0;
            this.readByte = 0;
            this.readBit = 0;
            this.readData = null;

            this.writeTotalBytes = 0;
            this.writeLength = 0;
            this.writeByte = 0;
            this.writeBit = 0;
            this.writeData = null;
        }

        @Override
        public void FinishCompress() {
            if (this.compress == false) {
                return;
            }

            if (this.writeByte != 0) {//TODO:wtf?
                this.file.Write(this.buffer, this.writeByte);
            }
            this.writeLength = 0;
            this.writeByte = 0;
            this.writeBit = 0;
        }

        @Override
        public float GetCompressionRatio() {
            if (this.compress) {
                return ((this.readTotalBytes - this.writeTotalBytes) * 100.0f) / this.readTotalBytes;
            } else {
                return ((this.writeTotalBytes - this.readTotalBytes) * 100.0f) / this.writeTotalBytes;
            }
        }

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int i;

            if ((this.compress == false) || (inLength <= 0)) {
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

            if ((this.compress == true) || (outLength <= 0)) {
                return 0;
            }

            InitDecompress(outData, outLength);

            for (i = 0; (i < outLength) && (this.readLength >= 0); i++) {
                WriteBits(ReadBits(8), 8);
            }
            return i;
        }

        protected void InitCompress(final ByteBuffer inData, final int inLength) {

            this.readLength = inLength;
            this.readByte = 0;
            this.readBit = 0;
            this.readData = inData;

            if (0 == this.writeLength) {
                this.writeLength = this.buffer.capacity();
                this.writeByte = 0;
                this.writeBit = 0;
                this.writeData = this.buffer;
            }
        }

        protected void InitCompress(final byte[] inData, final int inLength) {
            InitCompress(ByteBuffer.wrap(inData), inLength);
        }

        protected void InitDecompress(ByteBuffer outData, int outLength) {

            if (0 == this.readLength) {
                this.readLength = this.file.Read(this.buffer);
                this.readByte = 0;
                this.readBit = 0;
                this.readData = this.buffer;
            }

            this.writeLength = outLength;
            this.writeByte = 0;
            this.writeBit = 0;
            this.writeData = outData;
        }

        protected void InitDecompress(byte[] outData, int outLength) {
            InitDecompress(ByteBuffer.wrap(outData), outLength);
        }

        protected void WriteBits(int value, int numBits) {
            int put, fraction;

            // Short circuit for writing single bytes at a time
            if ((this.writeBit == 0) && (numBits == 8) && (this.writeByte < this.writeLength)) {
                this.writeByte++;
                this.writeTotalBytes++;
                this.writeData.putInt(this.writeByte - 1, value);//TODO:check if inputs should be cast to bytes or stores as INT in this case (4 bytes)
                return;
            }

            while (numBits != 0) {
                if (this.writeBit == 0) {
                    if (this.writeByte >= this.writeLength) {
                        if (this.writeData == this.buffer) {
                            this.file.Write(this.buffer, this.writeByte);
                            this.writeByte = 0;
                        } else {
                            put = numBits;
                            this.writeBit = put & 7;
                            this.writeByte += (put >> 3) + (this.writeBit != 0 ? 1 : 0);
                            this.writeTotalBytes += (put >> 3) + (this.writeBit != 0 ? 1 : 0);
                            return;
                        }
                    }
                    this.writeData.putInt(this.writeByte, 0);
                    this.writeByte++;
                    this.writeTotalBytes++;
                }
                put = 8 - this.writeBit;
                if (put > numBits) {
                    put = numBits;
                }
                fraction = value & ((1 << put) - 1);
                {
                    final int pos = this.writeByte - 1;
                    final int val = this.writeData.getInt(pos) | (fraction << this.writeBit);
                    this.writeData.putInt(pos, val);
                }
                numBits -= put;
                value >>= put;
                this.writeBit = (this.writeBit + put) & 7;
            }
        }

        protected int ReadBits(int numBits) {
            int value, valueBits, get, fraction;

            value = 0;
            valueBits = 0;

            // Short circuit for reading single bytes at a time
            if ((this.readBit == 0) && (numBits == 8) && (this.readByte < this.readLength)) {
                this.readByte++;
                this.readTotalBytes++;
                return this.readData.getInt(this.readByte - 1);
            }

            while (valueBits < numBits) {
                if (this.readBit == 0) {
                    if (this.readByte >= this.readLength) {
                        if (this.readData == this.buffer) {
                            this.readLength = this.file.Read(this.buffer);
                            this.readByte = 0;
                        } else {
                            get = numBits - valueBits;
                            this.readBit = get & 7;
                            this.readByte += (get >> 3) + (this.readBit != 0 ? 1 : 0);
                            this.readTotalBytes += (get >> 3) + (this.readBit != 0 ? 1 : 0);
                            return value;
                        }
                    }
                    this.readByte++;
                    this.readTotalBytes++;
                }
                get = 8 - this.readBit;
                if (get > (numBits - valueBits)) {
                    get = (numBits - valueBits);
                }
                fraction = this.readData.get(this.readByte - 1);
                fraction >>= this.readBit;
                fraction &= (1 << get) - 1;
                value |= fraction << valueBits;
                valueBits += get;
                this.readBit = (this.readBit + get) & 7;
            }

            return value;
        }

        protected void UnreadBits(int numBits) {
            this.readByte -= (numBits >> 3);
            this.readTotalBytes -= (numBits >> 3);
            if (this.readBit == 0) {
                this.readBit = 8 - (numBits & 7);
            } else {
                this.readBit -= numBits & 7;
                if (this.readBit <= 0) {
                    this.readByte--;
                    this.readTotalBytes--;
                    this.readBit = (this.readBit + 8) & 7;
                }
            }
            if (this.readByte < 0) {
                this.readByte = 0;
                this.readBit = 0;
            }
        }

        protected int Compare(final byte[] src1, int bitPtr1, final byte[] src2, int bitPtr2, int maxBits) {
            int i;

            // If the two bit pointers are aligned then we can use a faster comparison
            if (((bitPtr1 & 7) == (bitPtr2 & 7)) && (maxBits > 16)) {
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
                while ((remain >= 4) && (src1[p1] == src2[p2])) {
                    p1 += 4;
                    p2 += 4;
                    remain -= 4;
                    bits += 32;
                }

                // Compare the remaining bytes
                while ((remain > 0) && (src1[p1] == src2[p2])) {
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
    }

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
            this.runLengthCode = (1 << wordLength) - 1;
        }

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int bits, nextBits, count;

            if ((this.compress == false) || (inLength <= 0)) {
                return 0;
            }

            InitCompress(inData, inLength);

            while (this.readByte <= this.readLength) {
                count = 1;
                bits = ReadBits(this.wordLength);
                for (nextBits = ReadBits(this.wordLength); nextBits == bits; nextBits = ReadBits(this.wordLength)) {
                    count++;
                    if (count >= (1 << this.wordLength)) {
                        if ((count >= ((1 << this.wordLength) + 3)) || (bits == this.runLengthCode)) {
                            break;
                        }
                    }
                }
                if (nextBits != bits) {
                    UnreadBits(this.wordLength);
                }
                if ((count > 3) || (bits == this.runLengthCode)) {
                    WriteBits(this.runLengthCode, this.wordLength);
                    WriteBits(bits, this.wordLength);
                    if (bits != this.runLengthCode) {
                        count -= 3;
                    }
                    WriteBits(count - 1, this.wordLength);
                } else {
                    while (count-- != 0) {
                        WriteBits(bits, this.wordLength);
                    }
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int bits, count;

            if ((this.compress == true) || (outLength <= 0)) {
                return 0;
            }

            InitDecompress(outData, outLength);

            while ((this.writeByte <= this.writeLength) && (this.readLength >= 0)) {
                bits = ReadBits(this.wordLength);
                if (bits == this.runLengthCode) {
                    bits = ReadBits(this.wordLength);
                    count = ReadBits(this.wordLength) + 1;
                    if (bits != this.runLengthCode) {
                        count += 3;
                    }
                    while (count-- != 0) {
                        WriteBits(bits, this.wordLength);
                    }
                } else {
                    WriteBits(bits, this.wordLength);
                }
            }

            return this.writeByte;
        }
//
//
        private int runLengthCode;
    }

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

            if ((this.compress == false) || (inLength <= 0)) {
                return 0;
            }

            InitCompress(inData, inLength);

            while (this.readByte <= this.readLength) {
                count = 0;
                for (bits = ReadBits(this.wordLength); (bits == 0) && (count < (1 << this.wordLength)); bits = ReadBits(this.wordLength)) {
                    count++;
                }
                if (count != 0) {
                    WriteBits(0, this.wordLength);
                    WriteBits(count - 1, this.wordLength);
                    UnreadBits(this.wordLength);
                } else {
                    WriteBits(bits, this.wordLength);
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int bits, count;

            if ((this.compress == true) || (outLength <= 0)) {
                return 0;
            }

            InitDecompress(outData, outLength);

            while ((this.writeByte <= this.writeLength) && (this.readLength >= 0)) {
                bits = ReadBits(this.wordLength);
                if (bits == 0) {
                    count = ReadBits(this.wordLength) + 1;
                    while (count-- > 0) {
                        WriteBits(0, this.wordLength);
                    }
                } else {
                    WriteBits(bits, this.wordLength);
                }
            }

            return this.writeByte;
        }
    }
    static final int HMAX = 256;				// Maximum symbol
    static final int NYT = HMAX;				// NYT = Not Yet Transmitted
    static final int INTERNAL_NODE = HMAX + 1;			// internal node

    class nodetype {

        nodetype left, right, parent; // tree structure
        nodetype next, prev;			// doubly-linked list
        nodetype head;					// highest ranked node in block
        int weight;
        int symbol;
    }

    class huffmanNode_t extends nodetype {
    }

    /*
     =================================================================================

     idCompressor_Huffman

     The following algorithm is based on the adaptive Huffman algorithm described
     in Sayood's Data Compression book. The ranks are not actually stored, but
     implicitly defined by the location of a node within a doubly-linked list

     =================================================================================
     */
    static class idCompressor_Huffman extends idCompressor_None {

        private final ByteBuffer seq = ByteBuffer.allocate(65536);//TODO:allocateDirect?
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
        private final huffmanNode_t[] loc = new huffmanNode_t[HMAX + 1];
        private huffmanNode_t[] freelist;
        //
        private final huffmanNode_t[] nodeList = new huffmanNode_t[768];
        private final huffmanNode_t[] nodePtrs = new huffmanNode_t[768];
        //
        //

        public idCompressor_Huffman() {
        }

        @Override
        public void Init(idFile f, boolean compress, int wordLength) {
            int i;

            this.file = f;
            this.compress = compress;
            this.bloc = 0;
            this.blocMax = 0;
            this.blocIn = 0;
            this.blocNode = 0;
            this.blocPtrs = 0;
            this.compressedSize = 0;
            this.unCompressedSize = 0;

            this.tree = null;
            this.lhead = null;
            this.ltail = null;
            for (i = 0; i < (HMAX + 1); i++) {
                this.loc[i] = null;
            }
            this.freelist = null;

            for (i = 0; i < 768; i++) {
//		memset( &nodeList[i], 0, sizeof(huffmanNode_t) );
                this.nodePtrs[i] = null;
            }

            if (compress) {
                // Add the NYT (not yet transmitted) node into the tree/list
                this.tree = this.lhead = this.loc[NYT] = this.nodeList[this.blocNode++];
                this.tree.symbol = NYT;
                this.tree.weight = 0;
                this.lhead.next = this.lhead.prev = null;
                this.tree.parent = this.tree.left = this.tree.right = null;
                this.loc[NYT] = this.tree;
            } else {
                // Initialize the tree & list with the NYT node 
                this.tree = this.lhead = this.ltail = this.loc[NYT] = this.nodeList[this.blocNode++];
                this.tree.symbol = NYT;
                this.tree.weight = 0;
                this.lhead.next = this.lhead.prev = null;
                this.tree.parent = this.tree.left = this.tree.right = null;
            }
        }

        @Override
        public void FinishCompress() {

            if (this.compress == false) {
                return;
            }

            this.bloc += 7;
            final int str = (this.bloc >> 3);
            if (str != 0) {
                this.file.Write(this.seq, str);
                this.compressedSize += str;
            }
        }

        @Override
        public float GetCompressionRatio() {
            return ((this.unCompressedSize - this.compressedSize) * 100.0f) / this.unCompressedSize;
        }
//

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int i;
            int ch;

            if ((this.compress == false) || (inLength <= 0)) {
                return 0;
            }

            for (i = 0; i < inLength; i++) {
                ch = inData.getInt(i);
                Transmit(ch, this.seq);  // Transmit symbol 
                AddRef((byte) ch);         // Do update 

                final int b = (this.bloc >> 3);
                if (b > 32768) {
                    this.file.Write(this.seq, b);
                    this.seq.put(0, this.seq.get(b));
                    this.bloc &= 7;
                    this.compressedSize += b;
                }
            }

            this.unCompressedSize += i;
            return i;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int i, j;
            final int[] ch = new int[1];

            if ((this.compress == true) || (outLength <= 0)) {
                return 0;
            }

            if (this.bloc == 0) {
                this.blocMax = this.file.Read(this.seq);
                this.blocIn = 0;
            }

            for (i = 0; i < outLength; i++) {
                ch[0] = 0;
                // don't overflow reading from the file
                if ((this.bloc >> 3) > this.blocMax) {
                    break;
                }
                Receive(this.tree, ch);		// Get a character 
                if (ch[0] == NYT) {		// We got a NYT, get the symbol associated with it

                    ch[0] = 0;
                    for (j = 0; j < 8; j++) {
                        ch[0] = (ch[0] << 1) + Get_bit();
                    }
                }

                outData.putInt(i, ch[0]);		// Write symbol 
                AddRef((byte) ch[0]);				// Increment node 
            }

            this.compressedSize = this.bloc >> 3;
            this.unCompressedSize += i;
            return i;
        }

        private void AddRef(byte ch) {
            huffmanNode_t tnode, tnode2;
            if (this.loc[ch] == null) { /* if this is the first transmission of this node */

                tnode = this.nodeList[this.blocNode++];
                tnode2 = this.nodeList[this.blocNode++];

                tnode2.symbol = INTERNAL_NODE;
                tnode2.weight = 1;
                tnode2.next = this.lhead.next;
                if (this.lhead.next != null) {
                    this.lhead.next.prev = tnode2;
                    if (this.lhead.next.weight == 1) {
                        tnode2.head = this.lhead.next.head;
                    } else {
                        tnode2.head = Get_ppnode();
                        tnode2.head = tnode2;
                    }
                } else {
                    tnode2.head = Get_ppnode();
                    tnode2.head = tnode2;
                }
                this.lhead.next = tnode2;
                tnode2.prev = this.lhead;

                tnode.symbol = ch;
                tnode.weight = 1;
                tnode.next = this.lhead.next;
                if (this.lhead.next != null) {
                    this.lhead.next.prev = tnode;
                    if (this.lhead.next.weight == 1) {
                        tnode.head = this.lhead.next.head;
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
                this.lhead.next = tnode;
                tnode.prev = this.lhead;
                tnode.left = tnode.right = null;

                if (this.lhead.parent != null) {
                    if (this.lhead.parent.left == this.lhead) { /* lhead is guaranteed to by the NYT */

                        this.lhead.parent.left = tnode2;
                    } else {
                        this.lhead.parent.right = tnode2;
                    }
                } else {
                    this.tree = tnode2;
                }

                tnode2.right = tnode;
                tnode2.left = this.lhead;

                tnode2.parent = this.lhead.parent;
                this.lhead.parent = tnode.parent = tnode2;

                this.loc[ch] = tnode;

                Increment((huffmanNode_t) tnode2.parent);
            } else {
                Increment(this.loc[ch]);
            }
        }

        /*
         ================
         idCompressor_Huffman::Receive

         Get a symbol.
         ================
         */
        private int Receive(huffmanNode_t node, int[] ch) {
            while ((node != null) && (node.symbol == INTERNAL_NODE)) {
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
            if (this.loc[ch] == null) {
                /* huffmanNode_t hasn't been transmitted, send a NYT, then the symbol */
                Transmit(NYT, fout);
                for (i = 7; i >= 0; i--) {
                    Add_bit((char) ((ch >> i) & 0x1), fout);
                }
            } else {
                Send(this.loc[ch], null, fout);
            }
        }

        private void PutBit(int bit, byte[] fout, int[] offset) {
            this.bloc = offset[0];
            if ((this.bloc & 7) == 0) {
                fout[(this.bloc >> 3)] = 0;
            }
            fout[(this.bloc >> 3)] |= bit << (this.bloc & 7);
            this.bloc++;
            offset[0] = this.bloc;
        }

        private int GetBit(byte[] fin, int[] offset) {
            int t;
            this.bloc = offset[0];
            t = (fin[(this.bloc >> 3)] >> (this.bloc & 7)) & 0x1;
            this.bloc++;
            offset[0] = this.bloc;
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
            final int pos = this.bloc >> 3;
            final int val = bit << (this.bloc & 7);

            if ((this.bloc & 7) == 0) {
                fout.putInt(pos, 0);
            }
            fout.putInt(pos, val);
            this.bloc++;
        }

        /*
         ================
         idCompressor_Huffman::Get_bit

         Get one bit from the input file (buffered)
         ================
         */
        private int Get_bit() {
            int t;
            int wh = this.bloc >> 3;
            final int whb = wh >> 16;
            if (whb != this.blocIn) {
                this.blocMax += this.file.Read(this.seq/*, sizeof( seq )*/);
                this.blocIn++;
            }
            wh &= 0xffff;
            t = (this.seq.get(wh) >> (this.bloc & 7)) & 0x1;
            this.bloc++;
            return t;
        }

        private huffmanNode_t Get_ppnode() {
            final huffmanNode_t tppnode;
            if (null == this.freelist) {
                return this.nodePtrs[this.blocPtrs++];
            } else {
                tppnode = this.freelist[0];
//                freelist = /*(huffmanNode_t **)**/tppnode;
                return tppnode;
            }
        }

        private void Free_ppnode(huffmanNode_t[] ppnode) {
            ppnode[0] = /*(huffmanNode_t *)*/ this.freelist[0];//TODO:fix
            this.freelist = ppnode;
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
                this.tree = node2;
            }

            if (par2 != null) {
                if (par2.left == node2) {
                    par2.left = node1;
                } else {
                    par2.right = node1;
                }
            } else {
                this.tree = node1;
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

            if ((node.next != null) && (node.next.weight == node.weight)) {
                lnode = (huffmanNode_t) node.head;
                if (lnode != node.parent) {
                    Swap(lnode, node);
                }
                Swaplist(lnode, node);
            }
            if ((node.prev != null) && (node.prev.weight == node.weight)) {
                node.head = node.prev;
            } else {
                final huffmanNode_t[] temp = new huffmanNode_t[1];
                Free_ppnode(temp);
                node.head = temp[0];
            }
            node.weight++;
            if ((node.next != null) && (node.next.weight == node.weight)) {
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
    }
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
        }

        private class acProbs_t extends acProbs_s {
        }

        private class acSymbol_s {

            long low;
            long high;
            int position;
        }

        private class acSymbol_t extends acSymbol_s {
        }

        private final acProbs_t[] probabilities = new acProbs_t[1 << AC_WORD_LENGTH];
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

            this.symbolBuffer = 0;
            this.symbolBit = 0;
        }

        @Override
        public void FinishCompress() {
            if (this.compress == false) {
                return;
            }

            WriteOverflowBits();

            super.FinishCompress();
        }

        @Override
        public int Write(ByteBuffer inData, int inLength) {
            int i, j;

            if ((this.compress == false) || (inLength <= 0)) {
                return 0;
            }

            InitCompress(inData, inLength);

            for (i = 0; i < inLength; i++) {
                if ((this.readTotalBytes & ((1 << 14) - 1)) == 0) {
                    if (this.readTotalBytes != 0) {
                        WriteOverflowBits();
                        WriteBits(0, 15);
                        while (this.writeBit != 0) {
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

            if ((this.compress == true) || (outLength <= 0)) {
                return 0;
            }

            InitDecompress(outData, outLength);

            for (i = 0; (i < outLength) && (this.readLength >= 0); i++) {
                if ((this.writeTotalBytes & ((1 << 14) - 1)) == 0) {
                    if (this.writeTotalBytes != 0) {
                        while (this.readBit != 0) {
                            ReadBits(1);
                        }
                        while ((ReadBits(8) == 0) && (this.readLength > 0)) {
                        }
                    }
                    InitProbabilities();
                    for (j = 0; j < AC_NUM_BITS; j++) {
                        this.code <<= 1;
                        this.code |= ReadBits(1);
                    }
                }
                for (j = 0; j < 8; j++) {
                    WriteBits(GetBit(), 1);
                }
            }

            return i;
        }

        private void InitProbabilities() {
            this.high = AC_HIGH_INIT;
            this.low = AC_LOW_INIT;
            this.underflowBits = 0;
            this.code = 0;

            for (int i = 0; i < (1 << AC_WORD_LENGTH); i++) {
                this.probabilities[i].low = i;
                this.probabilities[i].high = i + 1;
            }

            this.scale = (1 << AC_WORD_LENGTH);
        }

        private void UpdateProbabilities(acSymbol_t symbol) {
            int i, x;

            x = symbol.position;

            this.probabilities[x].high++;

            for (i = x + 1; i < (1 << AC_WORD_LENGTH); i++) {
                this.probabilities[i].low++;
                this.probabilities[i].high++;
            }

            this.scale++;
        }

        private int ProbabilityForCount(long count) {
            if (TempDump.isDeadCodeFalse()) {

                int len, mid, offset, res;

                len = (1 << AC_WORD_LENGTH);
                mid = len;
                offset = 0;
                res = 0;
                while (mid > 0) {
                    mid = len >> 1;
                    if (count >= this.probabilities[offset + mid].high) {
                        offset += mid;
                        len -= mid;
                        res = 1;
                    } else if (count < this.probabilities[offset + mid].low) {
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
                    if ((count >= this.probabilities[j].low) && (count < this.probabilities[j].high)) {
                        return j;
                    }
                }

                assert (false);

                return 0;

            }
        }
//

        private void CharToSymbol(int c, acSymbol_t symbol) {
            symbol.low = this.probabilities[c].low;
            symbol.high = this.probabilities[c].high;
            symbol.position = c;
        }

        private void EncodeSymbol(acSymbol_t symbol) {
            int range;

            // rescale high and low for the new symbol.
            range = (this.high - this.low) + 1;
            this.high = (int) ((this.low + ((range * symbol.high) / this.scale)) - 1);
            this.low = (int) (this.low + ((range * symbol.low) / this.scale));

            while (true) {
                if ((this.high & AC_MSB_MASK) == (this.low & AC_MSB_MASK)) {
                    // the high digits of low and high have converged, and can be written to the stream
                    WriteBits(this.high >> AC_MSB_SHIFT, 1);

                    while (this.underflowBits > 0) {

                        WriteBits(~this.high >> AC_MSB_SHIFT, 1);

                        this.underflowBits--;
                    }
                } else if (((this.low & AC_MSB2_MASK) != 0) && (0 == (this.high & AC_MSB2_MASK))) {
                    // underflow is in danger of happening, 2nd digits are converging but 1st digits don't match
                    this.underflowBits += 1;
                    this.low &= AC_MSB2_MASK - 1;
                    this.high |= AC_MSB2_MASK;
                } else {
                    UpdateProbabilities(symbol);
                    return;
                }

                this.low <<= 1;
                this.high <<= 1;
                this.high |= 1;
            }
        }
//

        private int SymbolFromCount(long count, acSymbol_t symbol) {
            final int p = ProbabilityForCount(count);
            symbol.low = this.probabilities[p].low;
            symbol.high = this.probabilities[p].high;
            symbol.position = p;
            return p;
        }

        private int GetCurrentCount() {
            return (int) (((((this.code - this.low) + 1) * this.scale) - 1) / ((this.high - this.low) + 1));
        }

        private void RemoveSymbolFromStream(acSymbol_t symbol) {
            long range;

            range = (long) (this.high - this.low) + 1;
            this.high = this.low + (int) (((range * symbol.high) / this.scale) - 1);
            this.low = this.low + (int) ((range * symbol.low) / this.scale);

            while (true) {

                if ((this.high & AC_MSB_MASK) == (this.low & AC_MSB_MASK)) {
                } else if (((this.low & AC_MSB2_MASK) == AC_MSB2_MASK) && ((this.high & AC_MSB2_MASK) == 0)) {
                    this.code ^= AC_MSB2_MASK;
                    this.low &= AC_MSB2_MASK - 1;
                    this.high |= AC_MSB2_MASK;
                } else {
                    UpdateProbabilities(symbol);
                    return;
                }

                this.low <<= 1;
                this.high <<= 1;
                this.high |= 1;
                this.code <<= 1;
                this.code |= ReadBits(1);
            }
        }
//

        private void PutBit(int putbit) {
            this.symbolBuffer |= (putbit & 1) << this.symbolBit;
            this.symbolBit++;

            if (this.symbolBit >= AC_WORD_LENGTH) {
                final acSymbol_t symbol = new acSymbol_t();

                CharToSymbol(this.symbolBuffer, symbol);
                EncodeSymbol(symbol);

                this.symbolBit = 0;
                this.symbolBuffer = 0;
            }
        }

        private int GetBit() {
            int getbit;

            if (this.symbolBit <= 0) {
                // read a new symbol out
                final acSymbol_t symbol = new acSymbol_t();
                this.symbolBuffer = SymbolFromCount(GetCurrentCount(), symbol);
                RemoveSymbolFromStream(symbol);
                this.symbolBit = AC_WORD_LENGTH;
            }

            getbit = (this.symbolBuffer >> (AC_WORD_LENGTH - this.symbolBit)) & 1;
            this.symbolBit--;

            return getbit;
        }
//

        private void WriteOverflowBits() {

            WriteBits(this.low >> AC_MSB2_SHIFT, 1);

            this.underflowBits++;
            while (this.underflowBits-- > 0) {
                WriteBits(~this.low >> AC_MSB2_SHIFT, 1);
            }
        }
    }
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

            this.offsetBits = LZSS_OFFSET_BITS;
            this.lengthBits = LZSS_LENGTH_BITS;

            this.minMatchWords = (this.offsetBits + this.lengthBits + wordLength) / wordLength;
            this.blockSize = 0;
            this.blockIndex = 0;
        }

        @Override
        public void FinishCompress() {
            if (this.compress == false) {
                return;
            }
            if (this.blockSize != 0) {
                CompressBlock();
            }
            super.FinishCompress();
        }

//
        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int i, n;

            if ((this.compress == false) || (inLength <= 0)) {
                return 0;
            }

            for (n = i = 0; i < inLength; i += n) {
                n = LZSS_BLOCK_SIZE - this.blockSize;
                if ((inLength - i) >= n) {
//			memcpy( block + blockSize, ((const byte *)inData) + i, n );
                    inData.get(this.block, i, n);
                    this.blockSize = LZSS_BLOCK_SIZE;
                    CompressBlock();
                    this.blockSize = 0;
                } else {
//			memcpy( block + blockSize, ((const byte *)inData) + i, inLength - i );
                    Nio.arraycopy(inData.array(), i, this.block, this.blockSize, inLength - i);
                    n = inLength - i;
                    this.blockSize += n;
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int i, n;

            if ((this.compress == true) || (outLength <= 0)) {
                return 0;
            }

            if (0 == this.blockSize) {
                DecompressBlock();
            }

            for (n = i = 0; i < outLength; i += n) {
                if (0 == this.blockSize) {
                    return i;
                }
                n = this.blockSize - this.blockIndex;
                if ((outLength - i) >= n) {
//			memcpy( ((byte *)outData) + i, block + blockIndex, n );
                    Nio.arraycopy(this.block, this.blockIndex, outData.array(), i, n);
                    DecompressBlock();
                    this.blockIndex = 0;
                } else {
//			memcpy( ((byte *)outData) + i, block + blockIndex, outLength - i );
                    Nio.arraycopy(this.block, this.blockIndex, outData.array(), i, outLength - i);
                    n = outLength - i;
                    this.blockIndex += n;
                }
            }

            return outLength;
        }

        protected boolean FindMatch(int startWord, int startValue, int[] wordOffset, int[] numWords) {
            int i, n, hash, bottom, maxBits;

            wordOffset[0] = startWord;
            numWords[0] = this.minMatchWords - 1;

            bottom = Lib.Max(0, startWord - ((1 << this.offsetBits) - 1));
            maxBits = (this.blockSize << 3) - (startWord * this.wordLength);

            hash = startValue & LZSS_HASH_MASK;
            for (i = this.hashTable[hash]; i >= bottom; i = this.hashNext[i]) {
                n = Compare(this.block, i * this.wordLength, this.block, startWord * this.wordLength, Lib.Min(maxBits, (startWord - i) * this.wordLength));
                if (n > (numWords[0] * this.wordLength)) {
                    numWords[0] = n / this.wordLength;
                    wordOffset[0] = i;
                    if (numWords[0] > ((((1 << this.lengthBits) - 1) + this.minMatchWords) - 1)) {
                        numWords[0] = (((1 << this.lengthBits) - 1) + this.minMatchWords) - 1;
                        break;
                    }
                }
            }

            return (numWords[0] >= this.minMatchWords);
        }

        protected void AddToHash(int index, int hash) {
            this.hashNext[index] = this.hashTable[hash];
            this.hashTable[hash] = index;
        }

        protected int GetWordFromBlock(int wordOffset) {
            int blockBit, blockByte, value, valueBits, get, fraction;

            blockBit = (wordOffset * this.wordLength) & 7;
            blockByte = (wordOffset * this.wordLength) >> 3;
            if (blockBit != 0) {
                blockByte++;
            }

            value = 0;
            valueBits = 0;

            while (valueBits < this.wordLength) {
                if (blockBit == 0) {
                    if (blockByte >= LZSS_BLOCK_SIZE) {
                        return value;
                    }
                    blockByte++;
                }
                get = 8 - blockBit;
                if (get > (this.wordLength - valueBits)) {
                    get = (this.wordLength - valueBits);
                }
                fraction = this.block[blockByte - 1];
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
            final int[] wordOffset = new int[1], numWords = new int[1];

            InitCompress(this.block, this.blockSize);

//	memset( hashTable, -1, sizeof( hashTable ) );
//	memset( hashNext, -1, sizeof( hashNext ) );
            Arrays.fill(this.hashTable, -1);
            Arrays.fill(this.hashNext, -1);

            startWord = 0;
            while (this.readByte < this.readLength) {
                startValue = ReadBits(this.wordLength);
                if (FindMatch(startWord, startValue, wordOffset, numWords)) {
                    WriteBits(1, 1);
                    WriteBits(startWord - wordOffset[0], this.offsetBits);
                    WriteBits(numWords[0] - this.minMatchWords, this.lengthBits);
                    UnreadBits(this.wordLength);
                    for (i = 0; i < numWords[0]; i++) {
                        startValue = ReadBits(this.wordLength);
                        AddToHash(startWord, startValue & LZSS_HASH_MASK);
                        startWord++;
                    }
                } else {
                    WriteBits(0, 1);
                    WriteBits(startValue, this.wordLength);
                    AddToHash(startWord, startValue & LZSS_HASH_MASK);
                    startWord++;
                }
            }

            this.blockSize = 0;
        }

        protected void DecompressBlock() {
            int i, offset, startWord, numWords;

            InitDecompress(this.block, LZSS_BLOCK_SIZE);

            startWord = 0;
            while ((this.writeByte < this.writeLength) && (this.readLength >= 0)) {
                if (ReadBits(1) != 0) {
                    offset = startWord - ReadBits(this.offsetBits);
                    numWords = ReadBits(this.lengthBits) + this.minMatchWords;
                    for (i = 0; i < numWords; i++) {
                        WriteBits(GetWordFromBlock(offset + i), this.wordLength);
                        startWord++;
                    }
                } else {
                    WriteBits(ReadBits(this.wordLength), this.wordLength);
                    startWord++;
                }
            }

            this.blockSize = Lib.Min(this.writeByte, LZSS_BLOCK_SIZE);
        }
    }

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

            this.offsetBits = 2 * wordLength;
            this.lengthBits = wordLength;

            this.minMatchWords = (this.offsetBits + this.lengthBits + wordLength) / wordLength;
            this.blockSize = 0;
            this.blockIndex = 0;
        }

        @Override
        protected void CompressBlock() {
            int i, startWord, startValue;
            final int[] wordOffset = new int[1], numWords = new int[1];

            InitCompress(this.block, this.blockSize);

//	memset( hashTable, -1, sizeof( hashTable ) );
//	memset( hashNext, -1, sizeof( hashNext ) );
            Arrays.fill(this.hashTable, -1);
            Arrays.fill(this.hashNext, -1);

            startWord = 0;
            while (this.readByte < this.readLength) {
                startValue = ReadBits(this.wordLength);
                if (FindMatch(startWord, startValue, wordOffset, numWords)) {
                    WriteBits(numWords[0] - (this.minMatchWords - 1), this.lengthBits);
                    WriteBits(startWord - wordOffset[0], this.offsetBits);
                    UnreadBits(this.wordLength);
                    for (i = 0; i < numWords[0]; i++) {
                        startValue = ReadBits(this.wordLength);
                        AddToHash(startWord, startValue & LZSS_HASH_MASK);
                        startWord++;
                    }
                } else {
                    WriteBits(0, this.lengthBits);
                    WriteBits(startValue, this.wordLength);
                    AddToHash(startWord, startValue & LZSS_HASH_MASK);
                    startWord++;
                }
            }

            this.blockSize = 0;
        }

        @Override
        protected void DecompressBlock() {
            int i, offset, startWord, numWords;

            InitDecompress(this.block, LZSS_BLOCK_SIZE);

            startWord = 0;
            while ((this.writeByte < this.writeLength) && (this.readLength >= 0)) {
                numWords = ReadBits(this.lengthBits);
                if (numWords != 0) {
                    numWords += (this.minMatchWords - 1);
                    offset = startWord - ReadBits(this.offsetBits);
                    for (i = 0; i < numWords; i++) {
                        WriteBits(GetWordFromBlock(offset + i), this.wordLength);
                        startWord++;
                    }
                } else {
                    WriteBits(ReadBits(this.wordLength), this.wordLength);
                    startWord++;
                }
            }

            this.blockSize = Lib.Min(this.writeByte, LZSS_BLOCK_SIZE);
        }
    }

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
        }
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
                this.dictionary[i].k = i;
                this.dictionary[i].w = -1;
            }
            this.index.Clear();

            this.nextCode = LZW_FIRST_CODE;
            this.codeBits = LZW_START_BITS;

            this.blockSize = 0;
            this.blockIndex = 0;

            this.w = -1;
            this.oldCode = -1;
        }

        @Override
        public void FinishCompress() {
            WriteBits(this.w, this.codeBits);
            super.FinishCompress();
        }

        @Override
        public int Write(final ByteBuffer inData, int inLength) {
            int i;

            InitCompress(inData, inLength);

            for (i = 0; i < inLength; i++) {
                final int k = ReadBits(8);

                final int code = Lookup(this.w, k);
                if (code >= 0) {
                    this.w = code;
                } else {
                    WriteBits(this.w, this.codeBits);
                    if (!BumpBits()) {
                        AddToDict(this.w, k);
                    }
                    this.w = k;
                }
            }

            return inLength;
        }

        @Override
        public int Read(ByteBuffer outData, int outLength) {
            int i, n;

            if ((this.compress == true) || (outLength <= 0)) {
                return 0;
            }

            if (0 == this.blockSize) {
                DecompressBlock();
            }

            for (n = i = 0; i < outLength; i += n) {
                if (0 == this.blockSize) {
                    return i;
                }
                n = this.blockSize - this.blockIndex;
                if ((outLength - i) >= n) {
//			memcpy( ((byte *)outData) + i, block + blockIndex, n );
                    Nio.arraycopy(this.block, this.blockIndex, outData.array(), i, n);
                    DecompressBlock();
                    this.blockIndex = 0;
                } else {
//			memcpy( ((byte *)outData) + i, block + blockIndex, outLength - i );
                    Nio.arraycopy(this.block, this.blockIndex, outData.array(), i, outLength - i);
                    n = outLength - i;
                    this.blockIndex += n;
                }
            }

            return outLength;
        }

        protected int AddToDict(int w, int k) {
            this.dictionary[this.nextCode].k = k;
            this.dictionary[this.nextCode].w = w;
            this.index.Add(w ^ k, this.nextCode);
            return this.nextCode++;
        }

        protected int Lookup(int w, int k) {
            int j;

            if (w == -1) {
                return k;
            } else {
                for (j = this.index.First(w ^ k); j >= 0; j = this.index.Next(j)) {
                    if ((this.dictionary[j].k == k) && (this.dictionary[j].w == w)) {
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
            if (this.nextCode == (1 << this.codeBits)) {
                this.codeBits++;
                if (this.codeBits > LZW_DICT_BITS) {
                    this.nextCode = LZW_FIRST_CODE;
                    this.codeBits = LZW_START_BITS;
                    this.index.Clear();
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
            final byte[] chain = new byte[LZW_DICT_SIZE];
            int firstChar = 0;
            int i = 0;
            do {
                assert ((i < (LZW_DICT_SIZE - 1)) && (code >= 0));
                chain[i++] = (byte) this.dictionary[code].k;
                code = this.dictionary[code].w;
            } while (code >= 0);
            firstChar = chain[--i];
            for (; i >= 0; i--) {
                WriteBits(chain[i], 8);
            }
            return firstChar;
        }

        protected void DecompressBlock() {
            int code, firstChar;

            InitDecompress(this.block, LZW_BLOCK_SIZE);

            while ((this.writeByte < (this.writeLength - LZW_DICT_SIZE)) && (this.readLength > 0)) {
                assert (this.codeBits <= LZW_DICT_BITS);

                code = ReadBits(this.codeBits);
                if (this.readLength == 0) {
                    break;
                }

                if (this.oldCode == -1) {
                    assert (code < 256);
                    WriteBits(code, 8);
                    this.oldCode = code;
                    firstChar = code;
                    continue;
                }

                if (code >= this.nextCode) {
                    assert (code == this.nextCode);
                    firstChar = WriteChain(this.oldCode);
                    WriteBits(firstChar, 8);
                } else {
                    firstChar = WriteChain(code);
                }
                AddToDict(this.oldCode, firstChar);
                if (BumpBits()) {
                    this.oldCode = -1;
                } else {
                    this.oldCode = code;
                }
            }

            this.blockSize = Lib.Min(this.writeByte, LZW_BLOCK_SIZE);
        }
    }
}
