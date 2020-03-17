package neo.idlib;

import static neo.TempDump.ctos;
import static neo.idlib.math.Math_h.ANGLE2BYTE;
import static neo.idlib.math.Math_h.ANGLE2SHORT;
import static neo.idlib.math.Math_h.BYTE2ANGLE;
import static neo.idlib.math.Math_h.FLOATSIGNBITSET;
import static neo.idlib.math.Math_h.SHORT2ANGLE;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import neo.idlib.Dict_h.idDict;
import neo.idlib.Dict_h.idKeyValue;
import neo.idlib.Lib.idException;
import neo.idlib.Lib.idLib;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.sys.sys_public;
import neo.sys.sys_public.netadr_t;

/**
 *
 */
public class BitMsg {

    /*
     ===============================================================================

     idBitMsg

     Handles byte ordering and avoids alignment errors.
     Allows concurrent writing and reading.
     The data set with Init is never freed.

     ===============================================================================
     */
    public static class idBitMsg {

        private ByteBuffer writeData;           // pointer to data for writing
        private ByteBuffer readData;            // pointer to data for reading
        private int        maxSize;             // maximum size of message in bytes
        private int        curSize;             // current size of message in bytes
        private int        writeBit;            // number of bits written to the last written byte
        private int        readCount;           // number of bytes read so far
        private int        readBit;             // number of bits read from the last read byte
        private boolean    allowOverflow;       // if false, generate an error when the message is overflowed
        private boolean    overflowed;          // set to true if the buffer size failed (with allowOverflow set)
        //
        //

        public idBitMsg() {
//	writeData = null;
//	readData = null;
            maxSize = 0;
            curSize = 0;
            writeBit = 0;
            readCount = 0;
            readBit = 0;
            allowOverflow = false;
            overflowed = false;
        }
//public					~idBitMsg() {}

        public void Init(byte[] data) {
            this.Init(ByteBuffer.wrap(data), data.length);
        }

        public void Init(ByteBuffer data) {
            this.Init(data, data.capacity());
        }

        public void Init(ByteBuffer data, int length) {
            writeData = data;
            readData = data;
            maxSize = length;
        }

        public void InitReadOnly(final ByteBuffer data, int length) {
            writeData = null;
            readData = data;
            maxSize = length;
        }

        // get data for writing
        public ByteBuffer GetData() {
            return writeData;
        }

        // get data for reading
        public ByteBuffer GetDataReadOnly() {
            return readData.duplicate();
        }

        // get the maximum message size
        public int GetMaxSize() {
            return maxSize;
        }

        // generate error if not set and message is overflowed
        public void SetAllowOverflow(boolean set) {
            allowOverflow = set;
        }

        // returns true if the message was overflowed
        public boolean IsOverflowed() {
            return overflowed;
        }

        // size of the message in bytes
        public int GetSize() {
            return curSize;
        }

        // set the message size
        public void SetSize(int size) {
            if (size > maxSize) {
                curSize = maxSize;
            } else {
                if (size < 0) {
                    curSize = 0;
                } else {
                    curSize = size;
                }
            }
        }

        // get current write bit
        public int GetWriteBit() {
            return writeBit;
        }

        // set current write bit
        public void SetWriteBit(int bit) {
            writeBit = bit & 7;
            if (writeBit != 0) {
                final int pos = curSize - 1;
                final int val = writeData.getInt(pos);
                writeData.putInt(pos, val & (1 << writeBit) - 1);
            }
        }

        // returns number of bits written
        public int GetNumBitsWritten() {
            return ((curSize << 3) - ((8 - writeBit) & 7));
        }

        // space left in bits for writing
        public int GetRemainingWriteBits() {
            return (maxSize << 3) - GetNumBitsWritten();
        }

        // save the write state
        public void SaveWriteState(int[] s, int[] b) {
            s[0] = curSize;
            b[0] = writeBit;
        }

        // restore the write state
        public void RestoreWriteState(int s, int b) {
            curSize = s;
            writeBit = b & 7;
            if (writeBit != 0) {
                final int pos = curSize - 1;
                final int val = writeData.getInt(pos);
                writeData.putInt(pos, val & (1 << writeBit) - 1);
            }
        }

        // bytes read so far
        public int GetReadCount() {
            return readCount;
        }

        // set the number of bytes and bits read
        public void SetReadCount(int bytes) {
            readCount = bytes;
        }

        // get current read bit
        public int GetReadBit() {
            return readBit;
        }

        // set current read bit
        public void SetReadBit(int bit) {
            readBit = bit & 7;
        }

        // returns number of bits read
        public int GetNumBitsRead() {
            return ((readCount << 3) - ((8 - readBit) & 7));
        }

        // number of bits left to read
        public int GetRemainingReadBits() {
            return (curSize << 3) - GetNumBitsRead();
        }

        // save the read state
        public void SaveReadState(int[] c, int[] b) {
            c[0] = readCount;
            b[0] = readBit;
        }

        // restore the read state
        public void RestoreReadState(int c, int b) {
            readCount = c;
            readBit = b & 7;
        }

        // begin writing
        public void BeginWriting() {
            curSize = 0;
            overflowed = false;
            writeBit = 0;
        }

        // space left in bytes
        public int GetRemainingSpace() {
            return maxSize - curSize;
        }

        // write up to the next byte boundary
        public void WriteByteAlign() {
            writeBit = 0;
        }

        /*
         ================
         idBitMsg::WriteBits

         If the number of bits is negative a sign is included.
         ================
         */// write the specified number of bits
        public void WriteBits(int value, int numBits) {
            int put;
            int fraction;

            try {
                if (null == writeData) {
                    idLib.common.Error("idBitMsg.WriteBits: cannot write to message");
                }

                // check if the number of bits is valid
                if (numBits == 0 || numBits < -31 || numBits > 32) {
                    idLib.common.Error("idBitMsg.WriteBits: bad numBits %d", numBits);
                }

                // check for value overflows
                // this should be an error really, as it can go unnoticed and cause either bandwidth or corrupted data transmitted
                if (numBits != 32) {
                    if (numBits > 0) {
                        if (value > (1 << numBits) - 1) {
                            idLib.common.Warning("idBitMsg.WriteBits: value overflow %d %d", value, numBits);
                        } else if (value < 0) {
                            idLib.common.Warning("idBitMsg.WriteBits: value overflow %d %d", value, numBits);
                        }
                    } else {
                        int r = 1 << (- 1 - numBits);
                        if (value > r - 1) {
                            idLib.common.Warning("idBitMsg.WriteBits: value overflow %d %d", value, numBits);
                        } else if (value < -r) {
                            idLib.common.Warning("idBitMsg.WriteBits: value overflow %d %d", value, numBits);
                        }
                    }
                }

                if (numBits < 0) {
                    numBits = -numBits;
                }

                // check for msg overflow
                if (CheckOverflow(numBits)) {
                    return;
                }

                // write the bits
                while (numBits != 0) {
                    if (writeBit == 0) {
//                        writeData.putInt(curSize, 0);
                        writeData.put((byte) 0);
                        curSize++;
                    }
                    put = 8 - writeBit;
                    if (put > numBits) {
                        put = numBits;
                    }
                    fraction = value & ((1 << put) - 1);
                    final int pos = curSize - 1;
                    final int val = writeData.get(pos);
                    writeData.put(pos, (byte) (val | fraction << writeBit));
                    numBits -= put;
                    value >>= put;
                    writeBit = (writeBit + put) & 7;
                }
            } catch (Lib.idException ex) {
                Logger.getLogger(BitMsg.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void WriteChar(int c) {
            WriteBits(c, -8);
        }

        public void WriteByte(int c) {
            WriteBits(c, 8);
        }

        public void WriteShort(int c) {
            WriteBits(c, -16);
        }

        public void WriteUShort(int c) {
            WriteBits(c, 16);
        }

        public void WriteLong(int c) {
            WriteBits(c, 32);
        }

        public void WriteFloat(float f) {
            WriteBits(Float.floatToIntBits(f), 32);
        }

        public void WriteFloat(float f, int exponentBits, int mantissaBits) {
            int bits = idMath.FloatToBits(f, exponentBits, mantissaBits);
            WriteBits(bits, 1 + exponentBits + mantissaBits);
        }

        public void WriteAngle8(float f) {
            WriteByte((byte) ANGLE2BYTE(f));
        }

        public void WriteAngle16(float f) {
            WriteShort((byte) ANGLE2SHORT(f));
        }

        public void WriteDir(final idVec3 dir, int numBits) {
            WriteBits(DirToBits(dir, numBits), numBits);
        }

        public void WriteString(final String s) throws idException {
            WriteString(s, -1);
        }

        public void WriteString(final String s, int maxLength) throws idException {
            WriteString(s, maxLength, true);
        }

        public void WriteString(final String s, int maxLength, boolean make7Bit) throws idException {
            if (null == s) {
                WriteData(ByteBuffer.wrap("".getBytes()), 1);//TODO:huh?
            } else {
                int i, l;
                byte[] dataPtr;
                byte[] bytePtr;

                l = s.length();
                if (maxLength >= 0 && l >= maxLength) {
                    l = maxLength - 1;
                }
                dataPtr = GetByteSpace(l + 1);
                bytePtr = s.getBytes();
                if (make7Bit) {
                    for (i = 0; i < l; i++) {
                        if (bytePtr[i] > 127) {
                            dataPtr[i] = '.';
                        } else {
                            dataPtr[i] = bytePtr[i];
                        }
                    }
                } else {
                    for (i = 0; i < l; i++) {
                        dataPtr[i] = bytePtr[i];
                    }
                }
                dataPtr[i] = '\0';
            }
        }

        public void WriteData(final ByteBuffer data, int length) throws idException {
//            memcpy(GetByteSpace(length), data, length);
            WriteData(data, 0, length);
        }

        public void WriteData(final ByteBuffer data, int offset, int length) throws idException {
//            System.arraycopy(data, offset, GetByteSpace(length), 0, length);
            data.get(GetByteSpace(length), offset, length);
        }

        public void WriteNetadr(final netadr_t adr) throws idException {
            byte[] dataPtr;
            dataPtr = GetByteSpace(4);
            System.arraycopy(adr.ip, 0, dataPtr, 0, 4);
            WriteUShort(adr.port);
        }

        public void WriteDeltaChar(int oldValue, int newValue) {
            WriteDelta(oldValue, newValue, -8);
        }

        public void WriteDeltaByte(int oldValue, int newValue) {
            WriteDelta(oldValue, newValue, 8);
        }

        public void WriteDeltaShort(int oldValue, int newValue) {
            WriteDelta(oldValue, newValue, -16);
        }

        public void WriteDeltaLong(int oldValue, int newValue) {
            WriteDelta(oldValue, newValue, 32);
        }

        public void WriteDeltaFloat(float oldValue, float newValue) {
            WriteDelta(Float.floatToIntBits(oldValue), Float.floatToIntBits(newValue), 32);
        }

        public void WriteDeltaFloat(float oldValue, float newValue, int exponentBits, int mantissaBits) {
            int oldBits = idMath.FloatToBits(oldValue, exponentBits, mantissaBits);
            int newBits = idMath.FloatToBits(newValue, exponentBits, mantissaBits);
            WriteDelta(oldBits, newBits, 1 + exponentBits + mantissaBits);
        }

        public void WriteDeltaByteCounter(int oldValue, int newValue) {
            int i, x;

            x = oldValue ^ newValue;
            for (i = 7; i > 0; i--) {
                if ((x & (1 << i)) != 0) {
                    i++;
                    break;
                }
            }
            WriteBits(i, 3);
            if (i != 0) {
                WriteBits(((1 << i) - 1) & newValue, i);
            }
        }

        public void WriteDeltaShortCounter(int oldValue, int newValue) {
            int i, x;

            x = oldValue ^ newValue;
            for (i = 15; i > 0; i--) {
                if ((x & (1 << i)) != 0) {
                    i++;
                    break;
                }
            }
            WriteBits(i, 4);
            if (i != 0) {
                WriteBits(((1 << i) - 1) & newValue, i);
            }
        }

        public void WriteDeltaLongCounter(int oldValue, int newValue) {
            int i, x;

            x = oldValue ^ newValue;
            for (i = 31; i > 0; i--) {
                if ((x & (1 << i)) != 0) {
                    i++;
                    break;
                }
            }
            WriteBits(i, 5);
            if (i != 0) {
                WriteBits(((1 << i) - 1) & newValue, i);
            }
        }

        public boolean WriteDeltaDict(final idDict dict, final idDict base) throws idException {
            int i;
            idKeyValue kv, basekv;
            boolean changed = false;

            if (base != null) {

                for (i = 0; i < dict.GetNumKeyVals(); i++) {
                    kv = dict.GetKeyVal(i);
                    basekv = base.FindKey(kv.GetKey().getData());
                    if (basekv == null || basekv.GetValue().Icmp(kv.GetValue().getData()) != 0) {
                        WriteString(kv.GetKey().getData());
                        WriteString(kv.GetValue().getData());
                        changed = true;
                    }
                }

                WriteString("");

                for (i = 0; i < base.GetNumKeyVals(); i++) {
                    basekv = base.GetKeyVal(i);
                    kv = dict.FindKey(basekv.GetKey().getData());
                    if (kv == null) {
                        WriteString(basekv.GetKey().getData());
                        changed = true;
                    }
                }

                WriteString("");

            } else {

                for (i = 0; i < dict.GetNumKeyVals(); i++) {
                    kv = dict.GetKeyVal(i);
                    WriteString(kv.GetKey().getData());
                    WriteString(kv.GetValue().getData());
                    changed = true;
                }
                WriteString("");

                WriteString("");

            }

            return changed;
        }
//

        public void BeginReading() {				// begin reading.
            readCount = 0;
            readBit = 0;
        }

        public int GetRemaingData() {			// number of bytes left to read
            return curSize - readCount;
        }

        public void ReadByteAlign() {			// read up to the next byte boundary
            readBit = 0;
        }

        /*
         ================
         idBitMsg::ReadBits

         If the number of bits is negative a sign is included.
         ================
         */// read the specified number of bits
        public int ReadBits(int numBits) throws idException {
            int value;
            int valueBits;
            int get;
            int fraction;
            boolean sgn;

            if (null == readData) {
                idLib.common.FatalError("idBitMsg.ReadBits: cannot read from message");
            }

            // check if the number of bits is valid
            if (numBits == 0 || numBits < -31 || numBits > 32) {
                idLib.common.FatalError("idBitMsg.ReadBits: bad numBits %d", numBits);
            }

            value = 0;
            valueBits = 0;

            if (numBits < 0) {
                numBits = -numBits;
                sgn = true;
            } else {
                sgn = false;
            }

            // check for overflow
            if (numBits > GetRemainingReadBits()) {
                return -1;
            }

            while (valueBits < numBits) {
                if (readBit == 0) {
                    readCount++;
                }
                get = 8 - readBit;
                if (get > (numBits - valueBits)) {
                    get = numBits - valueBits;
                }
                fraction = readData.get(readCount - 1);
                fraction >>= readBit;
                fraction &= (1 << get) - 1;
                value |= fraction << valueBits;

                valueBits += get;
                readBit = (readBit + get) & 7;
            }

            if (sgn) {
                if ((value & (1 << (numBits - 1))) != 0) {
                    value |= -1 ^ ((1 << numBits) - 1);
                }
            }

            return value;
        }

        public int ReadChar() throws idException {
            return ReadBits(-8);
        }

        public int ReadByte() throws idException {
            return ReadBits(8);
        }

        public int ReadShort() throws idException {
            return ReadBits(-16);
        }

        public int ReadUShort() throws idException {
            return ReadBits(16);
        }

        public int ReadLong() throws idException {
            return ReadBits(32);
        }

        public float ReadFloat() throws idException {
            float value;
            value = Float.intBitsToFloat(ReadBits(32));
            return value;
        }

        public float ReadFloat(int exponentBits, int mantissaBits) throws idException {
            int bits = ReadBits(1 + exponentBits + mantissaBits);
            return idMath.BitsToFloat(bits, exponentBits, mantissaBits);
        }

        public float ReadAngle8() throws idException {
            return (float) BYTE2ANGLE(ReadByte());
        }

        public float ReadAngle16() throws idException {
            return (float) SHORT2ANGLE(ReadShort());
        }

        public idVec3 ReadDir(int numBits) throws idException {
            return BitsToDir(ReadBits(numBits), numBits);
        }

        public int ReadString(char[] buffer, int bufferSize) throws idException {
            int l, c;

            ReadByteAlign();
            l = 0;
            while (1 != 0) {
                c = ReadByte();
                if (c <= 0 || c >= 255) {
                    break;
                }
                // translate all fmt spec to avoid crash bugs in string routines
                if (c == '%') {
                    c = '.';
                }

                // we will read past any excessively long string, so
                // the following data can be read, but the string will
                // be truncated
                if (l < bufferSize - 1) {
                    buffer[l] = (char) c;
                    l++;
                }
            }

            buffer[l] = 0;
            return l;
        }

        public int ReadData(ByteBuffer data, int length) {
            int cnt;

            ReadByteAlign();
            cnt = readCount;

            if (readCount + length > curSize) {
                if (data != null) {
//                    memcpy(data, readData + readCount, GetRemaingData());
                    data.put(readData.array(), readCount, GetRemaingData());
                }
                readCount = curSize;
            } else {
                if (data != null) {
//                    memcpy(data, readData + readCount, length);
                    data.put(readData.array(), readCount, length);
                }
                readCount += length;
            }

            return (readCount - cnt);
        }

        public void ReadNetadr(netadr_t adr) throws idException {
            int i;

            adr.type = sys_public.netadrtype_t.NA_IP;
            for (i = 0; i < 4; i++) {
                adr.ip[i] = (char) ReadByte();
            }
            adr.port = (short) ReadUShort();
        }
//

        public int ReadDeltaChar(int oldValue) throws idException {
            return ReadDelta(oldValue, -8);
        }

        public int ReadDeltaByte(int oldValue) throws idException {
            return ReadDelta(oldValue, 8);
        }

        public int ReadDeltaShort(int oldValue) throws idException {
            return ReadDelta(oldValue, -16);
        }

        public int ReadDeltaLong(int oldValue) throws idException {
            return ReadDelta(oldValue, 32);
        }

        public float ReadDeltaFloat(float oldValue) throws idException {
            float value;
            value = Float.intBitsToFloat(ReadDelta(Float.floatToIntBits(oldValue), 32));
            return value;
        }

        public float ReadDeltaFloat(float oldValue, int exponentBits, int mantissaBits) throws idException {
            int oldBits = idMath.FloatToBits(oldValue, exponentBits, mantissaBits);
            int newBits = ReadDelta(oldBits, 1 + exponentBits + mantissaBits);
            return idMath.BitsToFloat(newBits, exponentBits, mantissaBits);
        }

        public int ReadDeltaByteCounter(int oldValue) throws idException {
            int i, newValue;

            i = ReadBits(3);
            if (0 == i) {
                return oldValue;
            }
            newValue = ReadBits(i);
            return ((oldValue & ~((1 << i) - 1)) | newValue);
        }

        public int ReadDeltaShortCounter(int oldValue) throws idException {
            int i, newValue;

            i = ReadBits(4);
            if (0 == i) {
                return oldValue;
            }
            newValue = ReadBits(i);
            return ((oldValue & ~((1 << i) - 1)) | newValue);
        }

        public int ReadDeltaLongCounter(int oldValue) throws idException {
            int i, newValue;

            i = ReadBits(5);
            if (0 == i) {
                return oldValue;
            }
            newValue = ReadBits(i);
            return ((oldValue & ~((1 << i) - 1)) | newValue);
        }

        public boolean ReadDeltaDict(idDict dict, final idDict base) throws idException {
            char[] key = new char[Lib.MAX_STRING_CHARS];
            char[] value = new char[Lib.MAX_STRING_CHARS];
            boolean changed = false;

            if (base != null) {
                dict = base;
            } else {
                dict.Clear();
            }

            while (ReadString(key, key.length) != 0) {
                ReadString(value, value.length);
                dict.Set(ctos(key), ctos(value));
                changed = true;
            }

            while (ReadString(key, key.length) != 0) {
                dict.Delete(ctos(key));
                changed = true;
            }

            return changed;
        }
//

        public static int DirToBits(final idVec3 dir, int numBits) {
            int max, bits;
            float bias;

            assert (numBits >= 6 && numBits <= 32);
            assert (dir.LengthSqr() - 1.0f < 0.01f);

            numBits /= 3;
            max = (1 << (numBits - 1)) - 1;
            bias = 0.5f / max;

            bits = FLOATSIGNBITSET(dir.x) << (numBits * 3 - 1);
            bits |= (idMath.Ftoi((idMath.Fabs(dir.x) + bias) * max)) << (numBits * 2);
            bits |= FLOATSIGNBITSET(dir.y) << (numBits * 2 - 1);
            bits |= (idMath.Ftoi((idMath.Fabs(dir.y) + bias) * max)) << (numBits * 1);
            bits |= FLOATSIGNBITSET(dir.z) << (numBits * 1 - 1);
            bits |= (idMath.Ftoi((idMath.Fabs(dir.z) + bias) * max)) << (numBits * 0);
            return bits;
        }

        public static idVec3 BitsToDir(int bits, int numBits) {
            float[] sign = {1.0f, -1.0f};
            int max;
            float invMax;
            idVec3 dir = new idVec3();

            assert (numBits >= 6 && numBits <= 32);

            numBits /= 3;
            max = (1 << (numBits - 1)) - 1;
            invMax = 1.0f / max;

            dir.x = sign[(bits >> (numBits * 3 - 1)) & 1] * ((bits >> (numBits * 2)) & max) * invMax;
            dir.y = sign[(bits >> (numBits * 2 - 1)) & 1] * ((bits >> (numBits * 1)) & max) * invMax;
            dir.z = sign[(bits >> (numBits * 1 - 1)) & 1] * ((bits >> (numBits * 0)) & max) * invMax;
            dir.NormalizeFast();
            return dir;
        }

        private boolean CheckOverflow(int numBits) throws Lib.idException {
            assert (numBits >= 0);
            if (numBits > GetRemainingWriteBits()) {
                if (!allowOverflow) {
                    idLib.common.FatalError("idBitMsg: overflow without allowOverflow set");
                }
                if (numBits > (maxSize << 3)) {
                    idLib.common.FatalError("idBitMsg: %d bits is > full message size", numBits);
                }
                idLib.common.Printf("idBitMsg: overflow\n");
                BeginWriting();
                overflowed = true;
                return true;
            }
            return false;
        }

        private byte[] GetByteSpace(int length) throws Lib.idException {
            byte[] ptr;

            if (null == writeData) {
                idLib.common.FatalError("idBitMsg::GetByteSpace: cannot write to message");
            }

            // round up to the next byte
            WriteByteAlign();

            // check for overflow
            CheckOverflow(length << 3);

            ptr = new byte[writeData.capacity() - curSize];
            ((ByteBuffer) writeData.mark().position(curSize)).get(ptr).rewind();
            curSize += length;
            return ptr;
        }

        private void WriteDelta(int oldValue, int newValue, int numBits) {
            if (oldValue == newValue) {
                WriteBits(0, 1);
                return;
            }
            WriteBits(1, 1);
            WriteBits(newValue, numBits);
        }

        private int ReadDelta(int oldValue, int numBits) throws idException {
            if (ReadBits(1) != 0) {
                return ReadBits(numBits);
            }
            return oldValue;
        }
    };
    /*
     ==============================================================================

     idBitMsgDelta

     ==============================================================================
     */
    static final int MAX_DATA_BUFFER = 1024;

    public static class idBitMsgDelta {

        private idBitMsg base;			// base
        private idBitMsg newBase;		// new base
        private idBitMsg writeDelta;		// delta from base to new base for writing
        private idBitMsg readDelta;		// delta from base to new base for reading
        private boolean changed;		// true if the new base is different from the base
        //
        //

        public idBitMsgDelta() {
            base = null;
            newBase = null;
            writeDelta = null;
            readDelta = null;
            changed = false;
        }
//public					~idBitMsgDelta() {}
//

        public void Init(final idBitMsg base, idBitMsg newBase, idBitMsg delta) {
            this.base = base;
            this.newBase = newBase;
            this.writeDelta = delta;
            this.readDelta = delta;
            this.changed = false;
        }

        public void InitReadOnly(final idBitMsg base, idBitMsg newBase, final idBitMsg delta) {
            this.base = base;
            this.newBase = newBase;
            this.writeDelta = null;
            this.readDelta = delta;
            this.changed = false;
        }

        public boolean HasChanged() {
            return changed;
        }

        public void WriteBits(int value, int numBits) throws idException {
            if (newBase != null) {
                newBase.WriteBits(value, numBits);
            }

            if (null == base) {
                writeDelta.WriteBits(value, numBits);
                changed = true;
            } else {
                int baseValue = base.ReadBits(numBits);
                if (baseValue == value) {
                    writeDelta.WriteBits(0, 1);
                } else {
                    writeDelta.WriteBits(1, 1);
                    writeDelta.WriteBits(value, numBits);
                    changed = true;
                }
            }
        }

        public void WriteChar(int c) throws idException {
            WriteBits(c, -8);
        }

        public void WriteByte(int c) throws idException {
            WriteBits(c, 8);
        }

        public void WriteShort(int c) throws idException {
            WriteBits(c, -16);
        }

        public void WriteUShort(int c) throws idException {
            WriteBits(c, 16);
        }

        public void WriteLong(int c) throws idException {
            WriteBits(c, 32);
        }

        public void WriteFloat(float f) throws idException {
            WriteBits(Float.floatToIntBits(f), 32);
        }

        public void WriteFloat(float f, int exponentBits, int mantissaBits) throws idException {
            int bits = idMath.FloatToBits(f, exponentBits, mantissaBits);
            WriteBits(bits, 1 + exponentBits + mantissaBits);
        }

        public void WriteAngle8(float f) throws idException {
            WriteBits((byte) ANGLE2BYTE(f), 8);
        }

        public void WriteAngle16(float f) throws idException {
            WriteBits((short) ANGLE2SHORT(f), 16);
        }

        public void WriteDir(final idVec3 dir, int numBits) throws idException {
            WriteBits(idBitMsg.DirToBits(dir, numBits), numBits);
        }

//public	void			WriteString( final String s, int maxLength = -1 );
        public void WriteString(final String s, int maxLength) throws idException {
            if (newBase != null) {
                newBase.WriteString(s, maxLength);
            }

            if (null == base) {
                writeDelta.WriteString(s, maxLength);
                changed = true;
            } else {
                char[] baseString = new char[MAX_DATA_BUFFER];
                base.ReadString(baseString, MAX_DATA_BUFFER);
                if (idStr.Cmp(s, ctos(baseString)) == 0) {
                    writeDelta.WriteBits(0, 1);
                } else {
                    writeDelta.WriteBits(1, 1);
                    writeDelta.WriteString(s, maxLength);
                    changed = true;
                }
            }
        }

        public void WriteData(final ByteBuffer data, int length) throws idException {
            if (newBase != null) {
                newBase.WriteData(data, length);
            }

            if (null == base) {
                writeDelta.WriteData(data, length);
                changed = true;
            } else {
                ByteBuffer baseData = ByteBuffer.allocate(MAX_DATA_BUFFER);
                assert (length < MAX_DATA_BUFFER);
                base.ReadData(baseData, length);
                if (data.equals(baseData)) {//TODO:compareTo??
                    writeDelta.WriteBits(0, 1);
                } else {
                    writeDelta.WriteBits(1, 1);
                    writeDelta.WriteData(data, length);
                    changed = true;
                }
            }
        }

        public void WriteDict(final idDict dict) throws idException {
            if (newBase != null) {
                newBase.WriteDeltaDict(dict, null);
            }

            if (null == base) {
                writeDelta.WriteDeltaDict(dict, null);
                changed = true;
            } else {
                idDict baseDict = new idDict();
                base.ReadDeltaDict(baseDict, null);
                changed = writeDelta.WriteDeltaDict(dict, baseDict);
            }
        }
//

        public void WriteDeltaChar(int oldValue, int newValue) throws idException {
            WriteDelta(oldValue, newValue, -8);
        }

        public void WriteDeltaByte(int oldValue, int newValue) throws idException {
            WriteDelta(oldValue, newValue, 8);
        }

        public void WriteDeltaShort(int oldValue, int newValue) throws idException {
            WriteDelta(oldValue, newValue, -16);
        }

        public void WriteDeltaLong(int oldValue, int newValue) throws idException {
            WriteDelta(oldValue, newValue, 32);
        }

        public void WriteDeltaFloat(float oldValue, float newValue) throws idException {
            WriteDelta(Float.floatToIntBits(oldValue), Float.floatToIntBits(newValue), 32);
        }

        public void WriteDeltaFloat(float oldValue, float newValue, int exponentBits, int mantissaBits) throws idException {
            int oldBits = idMath.FloatToBits(oldValue, exponentBits, mantissaBits);
            int newBits = idMath.FloatToBits(newValue, exponentBits, mantissaBits);
            WriteDelta(oldBits, newBits, 1 + exponentBits + mantissaBits);
        }

        public void WriteDeltaByteCounter(int oldValue, int newValue) throws idException {
            if (newBase != null) {
                newBase.WriteBits(newValue, 8);
            }

            if (null == base) {
                writeDelta.WriteDeltaByteCounter(oldValue, newValue);
                changed = true;
            } else {
                int baseValue = base.ReadBits(8);
                if (baseValue == newValue) {
                    writeDelta.WriteBits(0, 1);
                } else {
                    writeDelta.WriteBits(1, 1);
                    writeDelta.WriteDeltaByteCounter(oldValue, newValue);
                    changed = true;
                }
            }
        }

        public void WriteDeltaShortCounter(int oldValue, int newValue) throws idException {
            if (newBase != null) {
                newBase.WriteBits(newValue, 16);
            }

            if (null == base) {
                writeDelta.WriteDeltaShortCounter(oldValue, newValue);
                changed = true;
            } else {
                int baseValue = base.ReadBits(16);
                if (baseValue == newValue) {
                    writeDelta.WriteBits(0, 1);
                } else {
                    writeDelta.WriteBits(1, 1);
                    writeDelta.WriteDeltaShortCounter(oldValue, newValue);
                    changed = true;
                }
            }
        }

        public void WriteDeltaLongCounter(int oldValue, int newValue) throws idException {
            if (newBase != null) {
                newBase.WriteBits(newValue, 32);
            }

            if (null == base) {
                writeDelta.WriteDeltaLongCounter(oldValue, newValue);
                changed = true;
            } else {
                int baseValue = base.ReadBits(32);
                if (baseValue == newValue) {
                    writeDelta.WriteBits(0, 1);
                } else {
                    writeDelta.WriteBits(1, 1);
                    writeDelta.WriteDeltaLongCounter(oldValue, newValue);
                    changed = true;
                }
            }
        }
//

        public int ReadBits(int numBits) throws idException {
            int value;

            if (null == base) {
                value = readDelta.ReadBits(numBits);
                changed = true;
            } else {
                int baseValue = base.ReadBits(numBits);
                if (null == readDelta || readDelta.ReadBits(1) == 0) {
                    value = baseValue;
                } else {
                    value = readDelta.ReadBits(numBits);
                    changed = true;
                }
            }

            if (newBase != null) {
                newBase.WriteBits(value, numBits);
            }
            return value;
        }

        public int ReadChar() throws idException {
            return ReadBits(-8);
        }

        public int ReadByte() throws idException {
            return ReadBits(8);
        }

        public int ReadShort() throws idException {
            return ReadBits(-16);
        }

        public int ReadUShort() throws idException {
            return ReadBits(16);
        }

        public int ReadLong() throws idException {
            return ReadBits(32);
        }

        public float ReadFloat() throws idException {
            float value;
            value = Float.intBitsToFloat(ReadBits(32));
            return value;
        }

        public float ReadFloat(int exponentBits, int mantissaBits) throws idException {
            int bits = ReadBits(1 + exponentBits + mantissaBits);
            return idMath.BitsToFloat(bits, exponentBits, mantissaBits);
        }

        public float ReadAngle8() throws idException {
            return (float) BYTE2ANGLE(ReadByte());
        }

        public float ReadAngle16() throws idException {
            return (float) SHORT2ANGLE(ReadShort());
        }

        public idVec3 ReadDir(int numBits) throws idException {
            return idBitMsg.BitsToDir(ReadBits(numBits), numBits);
        }

        public void ReadString(char[] buffer, int bufferSize) throws idException {
            if (null == base) {
                readDelta.ReadString(buffer, bufferSize);
                changed = true;
            } else {
                char[] baseString = new char[MAX_DATA_BUFFER];
                base.ReadString(baseString, MAX_DATA_BUFFER);
                if (null == readDelta || readDelta.ReadBits(1) == 0) {
                    idStr.Copynz(buffer, ctos(baseString), bufferSize);
                } else {
                    readDelta.ReadString(buffer, bufferSize);
                    changed = true;
                }
            }

            if (newBase != null) {
                newBase.WriteString(ctos(buffer));
            }
        }

        public void ReadData(ByteBuffer data, int length) throws idException {
            if (null == base) {
                readDelta.ReadData(data, length);
                changed = true;
            } else {
                ByteBuffer baseData = ByteBuffer.allocate(MAX_DATA_BUFFER);
                assert (length < MAX_DATA_BUFFER);
                base.ReadData(baseData, length);
                if (null == readDelta || readDelta.ReadBits(1) == 0) {
//			memcpy( data, baseData, length );
                    data.put(data);//.array(), 0, length);
                } else {
                    readDelta.ReadData(data, length);
                    changed = true;
                }
            }

            if (newBase != null) {
                newBase.WriteData(data, length);
            }
        }

        public void ReadDict(idDict dict) throws idException {
            if (null == base) {
                readDelta.ReadDeltaDict(dict, null);
                changed = true;
            } else {
                idDict baseDict = new idDict();
                base.ReadDeltaDict(baseDict, null);
                if (null == readDelta) {
                    dict = baseDict;
                } else {
                    changed = readDelta.ReadDeltaDict(dict, baseDict);
                }
            }

            if (newBase != null) {
                newBase.WriteDeltaDict(dict, null);
            }
        }
//

        public int ReadDeltaChar(int oldValue) throws idException {
            return ReadDelta(oldValue, -8);
        }

        public int ReadDeltaByte(int oldValue) throws idException {
            return ReadDelta(oldValue, 8);
        }

        public int ReadDeltaShort(int oldValue) throws idException {
            return ReadDelta(oldValue, -16);
        }

        public int ReadDeltaLong(int oldValue) throws idException {
            return ReadDelta(oldValue, 32);
        }

        public float ReadDeltaFloat(float oldValue) throws idException {
            float value;
            value = Float.intBitsToFloat(ReadDelta(Float.floatToIntBits(oldValue), 32));
            return value;
        }

        public float ReadDeltaFloat(float oldValue, int exponentBits, int mantissaBits) throws idException {
            int oldBits = idMath.FloatToBits(oldValue, exponentBits, mantissaBits);
            int newBits = ReadDelta(oldBits, 1 + exponentBits + mantissaBits);
            return idMath.BitsToFloat(newBits, exponentBits, mantissaBits);
        }

        public int ReadDeltaByteCounter(int oldValue) throws idException {
            int value;

            if (null == base) {
                value = readDelta.ReadDeltaByteCounter(oldValue);
                changed = true;
            } else {
                int baseValue = base.ReadBits(8);
                if (null == readDelta || readDelta.ReadBits(1) == 0) {
                    value = baseValue;
                } else {
                    value = readDelta.ReadDeltaByteCounter(oldValue);
                    changed = true;
                }
            }

            if (newBase != null) {
                newBase.WriteBits(value, 8);
            }
            return value;
        }

        public int ReadDeltaShortCounter(int oldValue) throws idException {
            int value;

            if (null == base) {
                value = readDelta.ReadDeltaShortCounter(oldValue);
                changed = true;
            } else {
                int baseValue = base.ReadBits(16);
                if (null == readDelta || readDelta.ReadBits(1) == 0) {
                    value = baseValue;
                } else {
                    value = readDelta.ReadDeltaShortCounter(oldValue);
                    changed = true;
                }
            }

            if (newBase != null) {
                newBase.WriteBits(value, 16);
            }
            return value;
        }

        public int ReadDeltaLongCounter(int oldValue) throws idException {
            int value;

            if (null == base) {
                value = readDelta.ReadDeltaLongCounter(oldValue);
                changed = true;
            } else {
                int baseValue = base.ReadBits(32);
                if (null == readDelta || readDelta.ReadBits(1) == 0) {
                    value = baseValue;
                } else {
                    value = readDelta.ReadDeltaLongCounter(oldValue);
                    changed = true;
                }
            }

            if (newBase != null) {
                newBase.WriteBits(value, 32);
            }
            return value;
        }

        private void WriteDelta(int oldValue, int newValue, int numBits) throws idException {
            if (newBase != null) {
                newBase.WriteBits(newValue, numBits);
            }

            if (null == base) {
                if (oldValue == newValue) {
                    writeDelta.WriteBits(0, 1);
                } else {
                    writeDelta.WriteBits(1, 1);
                    writeDelta.WriteBits(newValue, numBits);
                }
                changed = true;
            } else {
                int baseValue = base.ReadBits(numBits);
                if (baseValue == newValue) {
                    writeDelta.WriteBits(0, 1);
                } else {
                    writeDelta.WriteBits(1, 1);
                    if (oldValue == newValue) {
                        writeDelta.WriteBits(0, 1);
                        changed = true;
                    } else {
                        writeDelta.WriteBits(1, 1);
                        writeDelta.WriteBits(newValue, numBits);
                        changed = true;
                    }
                }
            }
        }

        private int ReadDelta(int oldValue, int numBits) throws idException {
            int value;

            if (null == base) {
                if (readDelta.ReadBits(1) == 0) {
                    value = oldValue;
                } else {
                    value = readDelta.ReadBits(numBits);
                }
                changed = true;
            } else {
                int baseValue = base.ReadBits(numBits);
                if (null == readDelta || readDelta.ReadBits(1) == 0) {
                    value = baseValue;
                } else if (readDelta.ReadBits(1) == 0) {
                    value = oldValue;
                    changed = true;
                } else {
                    value = readDelta.ReadBits(numBits);
                    changed = true;
                }
            }

            if (newBase != null) {
                newBase.WriteBits(value, numBits);
            }
            return value;
        }
    };
}
