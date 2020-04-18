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
            this.maxSize = 0;
            this.curSize = 0;
            this.writeBit = 0;
            this.readCount = 0;
            this.readBit = 0;
            this.allowOverflow = false;
            this.overflowed = false;
        }
//public					~idBitMsg() {}

        public void Init(byte[] data) {
            this.Init(ByteBuffer.wrap(data), data.length);
        }

        public void Init(ByteBuffer data) {
            this.Init(data, data.capacity());
        }

        public void Init(ByteBuffer data, int length) {
            this.writeData = data;
            this.readData = data;
            this.maxSize = length;
        }

        public void InitReadOnly(final ByteBuffer data, int length) {
            this.writeData = null;
            this.readData = data;
            this.maxSize = length;
        }

        // get data for writing
        public ByteBuffer GetData() {
            return this.writeData;
        }

        // get data for reading
        public ByteBuffer GetDataReadOnly() {
            return this.readData.duplicate();
        }

        // get the maximum message size
        public int GetMaxSize() {
            return this.maxSize;
        }

        // generate error if not set and message is overflowed
        public void SetAllowOverflow(boolean set) {
            this.allowOverflow = set;
        }

        // returns true if the message was overflowed
        public boolean IsOverflowed() {
            return this.overflowed;
        }

        // size of the message in bytes
        public int GetSize() {
            return this.curSize;
        }

        // set the message size
        public void SetSize(int size) {
            if (size > this.maxSize) {
                this.curSize = this.maxSize;
            } else {
                if (size < 0) {
                    this.curSize = 0;
                } else {
                    this.curSize = size;
                }
            }
        }

        // get current write bit
        public int GetWriteBit() {
            return this.writeBit;
        }

        // set current write bit
        public void SetWriteBit(int bit) {
            this.writeBit = bit & 7;
            if (this.writeBit != 0) {
                final int pos = this.curSize - 1;
                final int val = this.writeData.getInt(pos);
                this.writeData.putInt(pos, val & ((1 << this.writeBit) - 1));
            }
        }

        // returns number of bits written
        public int GetNumBitsWritten() {
            return ((this.curSize << 3) - ((8 - this.writeBit) & 7));
        }

        // space left in bits for writing
        public int GetRemainingWriteBits() {
            return (this.maxSize << 3) - GetNumBitsWritten();
        }

        // save the write state
        public void SaveWriteState(int[] s, int[] b) {
            s[0] = this.curSize;
            b[0] = this.writeBit;
        }

        // restore the write state
        public void RestoreWriteState(int s, int b) {
            this.curSize = s;
            this.writeBit = b & 7;
            if (this.writeBit != 0) {
                final int pos = this.curSize - 1;
                final int val = this.writeData.getInt(pos);
                this.writeData.putInt(pos, val & ((1 << this.writeBit) - 1));
            }
        }

        // bytes read so far
        public int GetReadCount() {
            return this.readCount;
        }

        // set the number of bytes and bits read
        public void SetReadCount(int bytes) {
            this.readCount = bytes;
        }

        // get current read bit
        public int GetReadBit() {
            return this.readBit;
        }

        // set current read bit
        public void SetReadBit(int bit) {
            this.readBit = bit & 7;
        }

        // returns number of bits read
        public int GetNumBitsRead() {
            return ((this.readCount << 3) - ((8 - this.readBit) & 7));
        }

        // number of bits left to read
        public int GetRemainingReadBits() {
            return (this.curSize << 3) - GetNumBitsRead();
        }

        // save the read state
        public void SaveReadState(int[] c, int[] b) {
            c[0] = this.readCount;
            b[0] = this.readBit;
        }

        // restore the read state
        public void RestoreReadState(int c, int b) {
            this.readCount = c;
            this.readBit = b & 7;
        }

        // begin writing
        public void BeginWriting() {
            this.curSize = 0;
            this.overflowed = false;
            this.writeBit = 0;
        }

        // space left in bytes
        public int GetRemainingSpace() {
            return this.maxSize - this.curSize;
        }

        // write up to the next byte boundary
        public void WriteByteAlign() {
            this.writeBit = 0;
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
                if (null == this.writeData) {
                    idLib.common.Error("idBitMsg.WriteBits: cannot write to message");
                }

                // check if the number of bits is valid
                if ((numBits == 0) || (numBits < -31) || (numBits > 32)) {
                    idLib.common.Error("idBitMsg.WriteBits: bad numBits %d", numBits);
                }

                // check for value overflows
                // this should be an error really, as it can go unnoticed and cause either bandwidth or corrupted data transmitted
                if (numBits != 32) {
                    if (numBits > 0) {
                        if (value > ((1 << numBits) - 1)) {
                            idLib.common.Warning("idBitMsg.WriteBits: value overflow %d %d", value, numBits);
                        } else if (value < 0) {
                            idLib.common.Warning("idBitMsg.WriteBits: value overflow %d %d", value, numBits);
                        }
                    } else {
                        final int r = 1 << (- 1 - numBits);
                        if (value > (r - 1)) {
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
                    if (this.writeBit == 0) {
//                        writeData.putInt(curSize, 0);
                        this.writeData.put((byte) 0);
                        this.curSize++;
                    }
                    put = 8 - this.writeBit;
                    if (put > numBits) {
                        put = numBits;
                    }
                    fraction = value & ((1 << put) - 1);
                    final int pos = this.curSize - 1;
                    final int val = this.writeData.get(pos);
                    this.writeData.put(pos, (byte) (val | (fraction << this.writeBit)));
                    numBits -= put;
                    value >>= put;
                    this.writeBit = (this.writeBit + put) & 7;
                }
            } catch (final Lib.idException ex) {
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
            final int bits = idMath.FloatToBits(f, exponentBits, mantissaBits);
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
                if ((maxLength >= 0) && (l >= maxLength)) {
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
//            Nio.arraycopy(data, offset, GetByteSpace(length), 0, length);
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
            final int oldBits = idMath.FloatToBits(oldValue, exponentBits, mantissaBits);
            final int newBits = idMath.FloatToBits(newValue, exponentBits, mantissaBits);
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
                    if ((basekv == null) || (basekv.GetValue().Icmp(kv.GetValue().getData()) != 0)) {
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
            this.readCount = 0;
            this.readBit = 0;
        }

        public int GetRemaingData() {			// number of bytes left to read
            return this.curSize - this.readCount;
        }

        public void ReadByteAlign() {			// read up to the next byte boundary
            this.readBit = 0;
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

            if (null == this.readData) {
                idLib.common.FatalError("idBitMsg.ReadBits: cannot read from message");
            }

            // check if the number of bits is valid
            if ((numBits == 0) || (numBits < -31) || (numBits > 32)) {
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
                if (this.readBit == 0) {
                    this.readCount++;
                }
                get = 8 - this.readBit;
                if (get > (numBits - valueBits)) {
                    get = numBits - valueBits;
                }
                fraction = this.readData.get(this.readCount - 1);
                fraction >>= this.readBit;
                fraction &= (1 << get) - 1;
                value |= fraction << valueBits;

                valueBits += get;
                this.readBit = (this.readBit + get) & 7;
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
            final int bits = ReadBits(1 + exponentBits + mantissaBits);
            return idMath.BitsToFloat(bits, exponentBits, mantissaBits);
        }

        public float ReadAngle8() throws idException {
            return BYTE2ANGLE(ReadByte());
        }

        public float ReadAngle16() throws idException {
            return SHORT2ANGLE(ReadShort());
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
                if ((c <= 0) || (c >= 255)) {
                    break;
                }
                // translate all fmt spec to avoid crash bugs in string routines
                if (c == '%') {
                    c = '.';
                }

                // we will read past any excessively long string, so
                // the following data can be read, but the string will
                // be truncated
                if (l < (bufferSize - 1)) {
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
            cnt = this.readCount;

            if ((this.readCount + length) > this.curSize) {
                if (data != null) {
//                    memcpy(data, readData + readCount, GetRemaingData());
                    data.put(this.readData.array(), this.readCount, GetRemaingData());
                }
                this.readCount = this.curSize;
            } else {
                if (data != null) {
//                    memcpy(data, readData + readCount, length);
                    data.put(this.readData.array(), this.readCount, length);
                }
                this.readCount += length;
            }

            return (this.readCount - cnt);
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
            final int oldBits = idMath.FloatToBits(oldValue, exponentBits, mantissaBits);
            final int newBits = ReadDelta(oldBits, 1 + exponentBits + mantissaBits);
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
            final char[] key = new char[Lib.MAX_STRING_CHARS];
            final char[] value = new char[Lib.MAX_STRING_CHARS];
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

            assert ((numBits >= 6) && (numBits <= 32));
            assert ((dir.LengthSqr() - 1.0f) < 0.01f);

            numBits /= 3;
            max = (1 << (numBits - 1)) - 1;
            bias = 0.5f / max;

            bits = FLOATSIGNBITSET(dir.x) << ((numBits * 3) - 1);
            bits |= (idMath.Ftoi((idMath.Fabs(dir.x) + bias) * max)) << (numBits * 2);
            bits |= FLOATSIGNBITSET(dir.y) << ((numBits * 2) - 1);
            bits |= (idMath.Ftoi((idMath.Fabs(dir.y) + bias) * max)) << (numBits * 1);
            bits |= FLOATSIGNBITSET(dir.z) << ((numBits * 1) - 1);
            bits |= (idMath.Ftoi((idMath.Fabs(dir.z) + bias) * max)) << (numBits * 0);
            return bits;
        }

        public static idVec3 BitsToDir(int bits, int numBits) {
            final float[] sign = {1.0f, -1.0f};
            int max;
            float invMax;
            final idVec3 dir = new idVec3();

            assert ((numBits >= 6) && (numBits <= 32));

            numBits /= 3;
            max = (1 << (numBits - 1)) - 1;
            invMax = 1.0f / max;

            dir.x = sign[(bits >> ((numBits * 3) - 1)) & 1] * ((bits >> (numBits * 2)) & max) * invMax;
            dir.y = sign[(bits >> ((numBits * 2) - 1)) & 1] * ((bits >> (numBits * 1)) & max) * invMax;
            dir.z = sign[(bits >> ((numBits * 1) - 1)) & 1] * ((bits >> (numBits * 0)) & max) * invMax;
            dir.NormalizeFast();
            return dir;
        }

        private boolean CheckOverflow(int numBits) throws Lib.idException {
            assert (numBits >= 0);
            if (numBits > GetRemainingWriteBits()) {
                if (!this.allowOverflow) {
                    idLib.common.FatalError("idBitMsg: overflow without allowOverflow set");
                }
                if (numBits > (this.maxSize << 3)) {
                    idLib.common.FatalError("idBitMsg: %d bits is > full message size", numBits);
                }
                idLib.common.Printf("idBitMsg: overflow\n");
                BeginWriting();
                this.overflowed = true;
                return true;
            }
            return false;
        }

        private byte[] GetByteSpace(int length) throws Lib.idException {
            byte[] ptr;

            if (null == this.writeData) {
                idLib.common.FatalError("idBitMsg::GetByteSpace: cannot write to message");
            }

            // round up to the next byte
            WriteByteAlign();

            // check for overflow
            CheckOverflow(length << 3);

            ptr = new byte[this.writeData.capacity() - this.curSize];
            ((ByteBuffer) this.writeData.mark().position(this.curSize)).get(ptr).rewind();
            this.curSize += length;
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
    }
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
            this.base = null;
            this.newBase = null;
            this.writeDelta = null;
            this.readDelta = null;
            this.changed = false;
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
            return this.changed;
        }

        public void WriteBits(int value, int numBits) throws idException {
            if (this.newBase != null) {
                this.newBase.WriteBits(value, numBits);
            }

            if (null == this.base) {
                this.writeDelta.WriteBits(value, numBits);
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(numBits);
                if (baseValue == value) {
                    this.writeDelta.WriteBits(0, 1);
                } else {
                    this.writeDelta.WriteBits(1, 1);
                    this.writeDelta.WriteBits(value, numBits);
                    this.changed = true;
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
            final int bits = idMath.FloatToBits(f, exponentBits, mantissaBits);
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
            if (this.newBase != null) {
                this.newBase.WriteString(s, maxLength);
            }

            if (null == this.base) {
                this.writeDelta.WriteString(s, maxLength);
                this.changed = true;
            } else {
                final char[] baseString = new char[MAX_DATA_BUFFER];
                this.base.ReadString(baseString, MAX_DATA_BUFFER);
                if (idStr.Cmp(s, ctos(baseString)) == 0) {
                    this.writeDelta.WriteBits(0, 1);
                } else {
                    this.writeDelta.WriteBits(1, 1);
                    this.writeDelta.WriteString(s, maxLength);
                    this.changed = true;
                }
            }
        }

        public void WriteData(final ByteBuffer data, int length) throws idException {
            if (this.newBase != null) {
                this.newBase.WriteData(data, length);
            }

            if (null == this.base) {
                this.writeDelta.WriteData(data, length);
                this.changed = true;
            } else {
                final ByteBuffer baseData = ByteBuffer.allocate(MAX_DATA_BUFFER);
                assert (length < MAX_DATA_BUFFER);
                this.base.ReadData(baseData, length);
                if (data.equals(baseData)) {//TODO:compareTo??
                    this.writeDelta.WriteBits(0, 1);
                } else {
                    this.writeDelta.WriteBits(1, 1);
                    this.writeDelta.WriteData(data, length);
                    this.changed = true;
                }
            }
        }

        public void WriteDict(final idDict dict) throws idException {
            if (this.newBase != null) {
                this.newBase.WriteDeltaDict(dict, null);
            }

            if (null == this.base) {
                this.writeDelta.WriteDeltaDict(dict, null);
                this.changed = true;
            } else {
                final idDict baseDict = new idDict();
                this.base.ReadDeltaDict(baseDict, null);
                this.changed = this.writeDelta.WriteDeltaDict(dict, baseDict);
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
            final int oldBits = idMath.FloatToBits(oldValue, exponentBits, mantissaBits);
            final int newBits = idMath.FloatToBits(newValue, exponentBits, mantissaBits);
            WriteDelta(oldBits, newBits, 1 + exponentBits + mantissaBits);
        }

        public void WriteDeltaByteCounter(int oldValue, int newValue) throws idException {
            if (this.newBase != null) {
                this.newBase.WriteBits(newValue, 8);
            }

            if (null == this.base) {
                this.writeDelta.WriteDeltaByteCounter(oldValue, newValue);
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(8);
                if (baseValue == newValue) {
                    this.writeDelta.WriteBits(0, 1);
                } else {
                    this.writeDelta.WriteBits(1, 1);
                    this.writeDelta.WriteDeltaByteCounter(oldValue, newValue);
                    this.changed = true;
                }
            }
        }

        public void WriteDeltaShortCounter(int oldValue, int newValue) throws idException {
            if (this.newBase != null) {
                this.newBase.WriteBits(newValue, 16);
            }

            if (null == this.base) {
                this.writeDelta.WriteDeltaShortCounter(oldValue, newValue);
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(16);
                if (baseValue == newValue) {
                    this.writeDelta.WriteBits(0, 1);
                } else {
                    this.writeDelta.WriteBits(1, 1);
                    this.writeDelta.WriteDeltaShortCounter(oldValue, newValue);
                    this.changed = true;
                }
            }
        }

        public void WriteDeltaLongCounter(int oldValue, int newValue) throws idException {
            if (this.newBase != null) {
                this.newBase.WriteBits(newValue, 32);
            }

            if (null == this.base) {
                this.writeDelta.WriteDeltaLongCounter(oldValue, newValue);
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(32);
                if (baseValue == newValue) {
                    this.writeDelta.WriteBits(0, 1);
                } else {
                    this.writeDelta.WriteBits(1, 1);
                    this.writeDelta.WriteDeltaLongCounter(oldValue, newValue);
                    this.changed = true;
                }
            }
        }
//

        public int ReadBits(int numBits) throws idException {
            int value;

            if (null == this.base) {
                value = this.readDelta.ReadBits(numBits);
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(numBits);
                if ((null == this.readDelta) || (this.readDelta.ReadBits(1) == 0)) {
                    value = baseValue;
                } else {
                    value = this.readDelta.ReadBits(numBits);
                    this.changed = true;
                }
            }

            if (this.newBase != null) {
                this.newBase.WriteBits(value, numBits);
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
            final int bits = ReadBits(1 + exponentBits + mantissaBits);
            return idMath.BitsToFloat(bits, exponentBits, mantissaBits);
        }

        public float ReadAngle8() throws idException {
            return BYTE2ANGLE(ReadByte());
        }

        public float ReadAngle16() throws idException {
            return SHORT2ANGLE(ReadShort());
        }

        public idVec3 ReadDir(int numBits) throws idException {
            return idBitMsg.BitsToDir(ReadBits(numBits), numBits);
        }

        public void ReadString(char[] buffer, int bufferSize) throws idException {
            if (null == this.base) {
                this.readDelta.ReadString(buffer, bufferSize);
                this.changed = true;
            } else {
                final char[] baseString = new char[MAX_DATA_BUFFER];
                this.base.ReadString(baseString, MAX_DATA_BUFFER);
                if ((null == this.readDelta) || (this.readDelta.ReadBits(1) == 0)) {
                    idStr.Copynz(buffer, ctos(baseString), bufferSize);
                } else {
                    this.readDelta.ReadString(buffer, bufferSize);
                    this.changed = true;
                }
            }

            if (this.newBase != null) {
                this.newBase.WriteString(ctos(buffer));
            }
        }

        public void ReadData(ByteBuffer data, int length) throws idException {
            if (null == this.base) {
                this.readDelta.ReadData(data, length);
                this.changed = true;
            } else {
                final ByteBuffer baseData = ByteBuffer.allocate(MAX_DATA_BUFFER);
                assert (length < MAX_DATA_BUFFER);
                this.base.ReadData(baseData, length);
                if ((null == this.readDelta) || (this.readDelta.ReadBits(1) == 0)) {
//			memcpy( data, baseData, length );
                    data.put(data);//.array(), 0, length);
                } else {
                    this.readDelta.ReadData(data, length);
                    this.changed = true;
                }
            }

            if (this.newBase != null) {
                this.newBase.WriteData(data, length);
            }
        }

        public void ReadDict(idDict dict) throws idException {
            if (null == this.base) {
                this.readDelta.ReadDeltaDict(dict, null);
                this.changed = true;
            } else {
                final idDict baseDict = new idDict();
                this.base.ReadDeltaDict(baseDict, null);
                if (null == this.readDelta) {
                    dict = baseDict;
                } else {
                    this.changed = this.readDelta.ReadDeltaDict(dict, baseDict);
                }
            }

            if (this.newBase != null) {
                this.newBase.WriteDeltaDict(dict, null);
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
            final int oldBits = idMath.FloatToBits(oldValue, exponentBits, mantissaBits);
            final int newBits = ReadDelta(oldBits, 1 + exponentBits + mantissaBits);
            return idMath.BitsToFloat(newBits, exponentBits, mantissaBits);
        }

        public int ReadDeltaByteCounter(int oldValue) throws idException {
            int value;

            if (null == this.base) {
                value = this.readDelta.ReadDeltaByteCounter(oldValue);
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(8);
                if ((null == this.readDelta) || (this.readDelta.ReadBits(1) == 0)) {
                    value = baseValue;
                } else {
                    value = this.readDelta.ReadDeltaByteCounter(oldValue);
                    this.changed = true;
                }
            }

            if (this.newBase != null) {
                this.newBase.WriteBits(value, 8);
            }
            return value;
        }

        public int ReadDeltaShortCounter(int oldValue) throws idException {
            int value;

            if (null == this.base) {
                value = this.readDelta.ReadDeltaShortCounter(oldValue);
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(16);
                if ((null == this.readDelta) || (this.readDelta.ReadBits(1) == 0)) {
                    value = baseValue;
                } else {
                    value = this.readDelta.ReadDeltaShortCounter(oldValue);
                    this.changed = true;
                }
            }

            if (this.newBase != null) {
                this.newBase.WriteBits(value, 16);
            }
            return value;
        }

        public int ReadDeltaLongCounter(int oldValue) throws idException {
            int value;

            if (null == this.base) {
                value = this.readDelta.ReadDeltaLongCounter(oldValue);
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(32);
                if ((null == this.readDelta) || (this.readDelta.ReadBits(1) == 0)) {
                    value = baseValue;
                } else {
                    value = this.readDelta.ReadDeltaLongCounter(oldValue);
                    this.changed = true;
                }
            }

            if (this.newBase != null) {
                this.newBase.WriteBits(value, 32);
            }
            return value;
        }

        private void WriteDelta(int oldValue, int newValue, int numBits) throws idException {
            if (this.newBase != null) {
                this.newBase.WriteBits(newValue, numBits);
            }

            if (null == this.base) {
                if (oldValue == newValue) {
                    this.writeDelta.WriteBits(0, 1);
                } else {
                    this.writeDelta.WriteBits(1, 1);
                    this.writeDelta.WriteBits(newValue, numBits);
                }
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(numBits);
                if (baseValue == newValue) {
                    this.writeDelta.WriteBits(0, 1);
                } else {
                    this.writeDelta.WriteBits(1, 1);
                    if (oldValue == newValue) {
                        this.writeDelta.WriteBits(0, 1);
                        this.changed = true;
                    } else {
                        this.writeDelta.WriteBits(1, 1);
                        this.writeDelta.WriteBits(newValue, numBits);
                        this.changed = true;
                    }
                }
            }
        }

        private int ReadDelta(int oldValue, int numBits) throws idException {
            int value;

            if (null == this.base) {
                if (this.readDelta.ReadBits(1) == 0) {
                    value = oldValue;
                } else {
                    value = this.readDelta.ReadBits(numBits);
                }
                this.changed = true;
            } else {
                final int baseValue = this.base.ReadBits(numBits);
                if ((null == this.readDelta) || (this.readDelta.ReadBits(1) == 0)) {
                    value = baseValue;
                } else if (this.readDelta.ReadBits(1) == 0) {
                    value = oldValue;
                    this.changed = true;
                } else {
                    value = this.readDelta.ReadBits(numBits);
                    this.changed = true;
                }
            }

            if (this.newBase != null) {
                this.newBase.WriteBits(value, numBits);
            }
            return value;
        }
    }
}
