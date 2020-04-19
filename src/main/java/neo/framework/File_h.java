package neo.framework;

import static neo.TempDump.NOT;
import static neo.TempDump.atobb;
import static neo.TempDump.ctos;
import static neo.TempDump.etoi;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.FileSystem_h.fsMode_t.FS_READ;
import static neo.framework.FileSystem_h.fsMode_t.FS_WRITE;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_SET;
import static neo.idlib.Lib.LittleFloat;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.LittleShort;
import static neo.idlib.Lib.idLib.common;
import static neo.sys.win_main.Sys_FileTimeStamp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import neo.TempDump.SERiAL;
import neo.idlib.BitMsg.idBitMsg;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Vector.idVec2;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Vector.idVec6;
import neo.idlib.math.Matrix.idMat3;
import neo.open.Nio;

/**
 *
 */
public class File_h {

    /*
     =================
     FS_WriteFloatString
     =================
     */
    static int FS_WriteFloatString(char[] buf, final String fmtString, Object... argPtr) {
        long i;
        long u;
        double f;
        String str;
        int index;
        idStr tmp;
        String format;
        int fmt_ptr = 0;
        char[] fmt;
        int va_ptr = 0;
        String temp;

        index = 0;

        while (fmtString != null) {
            fmt = fmtString.toCharArray();
            switch (fmt[fmt_ptr]) {
                case '%':
                    format = "";
                    format += fmt[fmt_ptr++];
                    while (((fmt[fmt_ptr] >= '0') && (fmt[fmt_ptr] <= '9'))
                            || (fmt[fmt_ptr] == '.') || (fmt[fmt_ptr] == '-') || (fmt[fmt_ptr] == '+') || (fmt[fmt_ptr] == '#')) {
                        format += fmt[fmt_ptr++];
                    }
                    format += fmt[fmt_ptr];
                    switch (fmt[fmt_ptr]) {
                        case 'f':
                        case 'e':
                        case 'E':
                        case 'g':
                        case 'G':
                            f = (double) argPtr[va_ptr++];
                            if (format.length() <= 2) {
                                // high precision floating point number without trailing zeros
//                                sprintf(tmp, "%1.10f", f);
                                tmp = new idStr(String.format("%1.10f", f));
                                tmp.StripTrailing('0');
                                tmp.StripTrailing('.');
                                temp = String.format("%s", tmp.getData());
                                Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                                index += temp.length();
//                                index += sprintf(buf + index, "%s", tmp.c_str());
                            } else {
//                                index += sprintf(buf + index, format, f);
                                temp = String.format(format, f);
                                Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                                index += temp.length();
                            }
                            break;
                        case 'd':
                        case 'i':
                            i = (long) argPtr[va_ptr++];
//                            index += sprintf(buf + index, format, i);
                            temp = String.format(format, i);
                            Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                            index += temp.length();
                            break;
                        case 'u':
                            u = (long) argPtr[va_ptr++];
//                            index += sprintf(buf + index, format, u);
                            temp = String.format(format, u);
                            Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                            index += temp.length();
                            break;
                        case 'o':
                            u = (long) argPtr[va_ptr++];
//                            index += sprintf(buf + index, format, u);
                            temp = String.format(format, u);
                            Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                            index += temp.length();
                            break;
                        case 'x':
                            u = (long) argPtr[va_ptr++];
//                            index += sprintf(buf + index, format, u);
                            temp = String.format(format, u);
                            Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                            index += temp.length();
                            break;
                        case 'X':
                            u = (long) argPtr[va_ptr++];
//                            index += sprintf(buf + index, format, u);
                            temp = String.format(format, u);
                            Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                            index += temp.length();
                            break;
                        case 'c':
                            i = (long) argPtr[va_ptr++];
//                            index += sprintf(buf + index, format, (char) i);
                            temp = String.format(format, i);
                            Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                            index += temp.length();
                            break;
                        case 's':
                            str = (String) argPtr[va_ptr++];
//                            index += sprintf(buf + index, format, str);
                            temp = String.format(format, str);
                            Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                            index += temp.length();
                            break;
                        case '%':
//                            index += sprintf(buf + index, format);
                            temp = String.format(format);
                            Nio.arraycopy(temp.toCharArray(), 0, buf, index, temp.length());
                            index += temp.length();
                            break;
                        default:
                            common.Error("FS_WriteFloatString: invalid format %s", format);
                            break;
                    }
                    fmt_ptr++;
                    break;
                case '\\':
                    fmt_ptr++;
                    switch (fmt[fmt_ptr]) {
                        case 't':
//                            index += sprintf(buf + index, "\t");
                            buf[index++] = '\t';
                            break;
                        case 'v':
//                            index += sprintf(buf + index, "\v");
                            buf[index++] = '\013';//vertical tab
                            break;
                        case 'n':
//                            index += sprintf(buf + index, "\n");
                            buf[index++] = '\n';
                            break;
                        case '\\':
//                            index += sprintf(buf + index, "\\");
                            buf[index++] = '\\';
                            break;
                        default:
                            common.Error("FS_WriteFloatString: unknown escape character \'%c\'", fmt[fmt_ptr]);
                            break;
                    }
                    fmt_ptr++;
                    break;
                default:
//                    index += sprintf(buf + index, "%c", fmt[fmt_ptr]);
                    buf[index++] = fmt[fmt_ptr];
                    fmt_ptr++;
                    break;
            }
        }

        return index;
    }


    /*
     ==============================================================

     File Streams.

     ==============================================================
     */
// mode parm for Seek
    public enum fsOrigin_t {

        FS_SEEK_CUR,
        FS_SEEK_END,
        FS_SEEK_SET
    }
    static final int MAX_PRINT_MSG = 4096;

    /*
     =================================================================================

     idFile

     =================================================================================
     */
    public static abstract class idFile {//TODO:implement closable?
//	abstract					~idFile( ) {};

        // Get the name of the file.
        public String GetName() {
            return "";
        }

        // Get the full file path.
        public String GetFullPath() {
            return "";
        }

        // Read data from the file to the buffer.
        public int Read(ByteBuffer buffer/*, int len*/) {
            return Read(buffer, buffer.capacity());
        }

        public final int Read(SERiAL object) {
            final ByteBuffer buffer = object.AllocBuffer();
            final int reads = Read(buffer, buffer.capacity());
            object.Read(buffer);

            return reads;
        }

        public final int Read(SERiAL object, int len) {
            final ByteBuffer buffer = object.AllocBuffer();
            final int reads = Read(buffer, len);
            buffer.position(len).flip();
            object.Read(buffer);

            return reads;
        }

        @Deprecated// Read data from the file to the buffer.
        public int Read(ByteBuffer buffer, int len) {
            common.FatalError("idFile::Read: cannot read from idFile");
            return 0;
        }

        @Deprecated// Write all data from the buffer to the file.
        public int Write(final ByteBuffer buffer/*, int len*/) {
            common.FatalError("idFile::Write: cannot write to idFile");
            return 0;
        }

        @Deprecated
        public final int Write(SERiAL object) {
            return Write(object.Write());
        }

        @Deprecated// Write some data from the buffer to the file.
        public int Write(final ByteBuffer buffer, int len) {
            common.FatalError("idFile::Write: cannot write to idFile");
            return 0;
        }

        // Returns the length of the file.
        public int Length() {
            return 0;
        }

        // Return a time value for reload operations.
        public long Timestamp() {
            return 0;
        }

        // Returns offset in file.
        public int Tell() {
            return 0;
        }

        // Forces flush on files being writting to.
        public void ForceFlush() {
        }

        // Causes any buffered data to be written to the file.
        public void Flush() {
        }

        // Seek on a file.
        public boolean Seek(long offset, fsOrigin_t origin) throws idException {
            return false;//-1;
        }

        // Go back to the beginning of the file.
        public void Rewind() {
            Seek(0, FS_SEEK_SET);
        }

        // Like fprintf.
        public int Printf(final String fmt, Object... args)/* id_attribute((format(printf,2,3)))*/ {
            final String[] buf = {null};// new char[MAX_PRINT_MSG];
            int length;
//            va_list argptr;

//            va_start(argptr, fmt);
            length = idStr.vsnPrintf(buf, MAX_PRINT_MSG - 1, fmt, args/*, argptr*/);
//            va_end(argptr);

            // so notepad formats the lines correctly
            final idStr work = new idStr(buf[0]);
            work.Replace("\n", "\r\n");

            return Write(atobb(work));
        }

        // Like fprintf but with argument pointer
        public int VPrintf(final String fmt, Object... args/*, va_list arg*/) {
            final String[] buf = {null};//new char[MAX_PRINT_MSG];
            int length;

            length = idStr.vsnPrintf(buf, MAX_PRINT_MSG - 1, fmt, args/*, args*/);
            return Write(atobb(buf[0]));
        }

        // Write a string with high precision floating point numbers to the file.
        public int WriteFloatString(final String fmt, final Object... args)/* id_attribute((format(printf,2,3)))*/ {
            final char[] buf = new char[MAX_PRINT_MSG];
            int len;
            final Object[] argPtr = new Object[args.length];
            System.arraycopy(args, 0, argPtr, 0, argPtr.length);

//            va_start(argPtr, fmt);
            len = FS_WriteFloatString(buf, fmt, argPtr);
//            va_end(argPtr);

            return Write(atobb(buf), len);
        }

        // Endian portable alternatives to Read(...)
        public int ReadInt(int[] value) {
            final ByteBuffer intBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            final int result = Read(intBytes);
            value[0] = LittleLong(intBytes.getInt(0));
            return result;
        }

        public int ReadInt() {
            final int[] value = {0};
            this.ReadInt(value);

            return value[0];
        }

        // Endian portable alternatives to Write(...)
        public int WriteInt(final int value) {
            final ByteBuffer intBytes = ByteBuffer.allocate(4);
            final int v = LittleLong(value);
            intBytes.putInt(v);
            return Write(intBytes);
        }

        public int WriteInt(final Enum value) {
            return WriteInt(value.ordinal());
        }

        public int ReadUnsignedInt(long[] value) {
            final ByteBuffer uintBytes = ByteBuffer.allocate(4);
            final int result = Read(uintBytes);
            value[0] = LittleLong(uintBytes.getInt()) & 0xFFFF_FFFFL;
            return result;
        }

        public int WriteUnsignedInt(final long value) {
            final ByteBuffer uintBytes = ByteBuffer.allocate(2);
            final long v = LittleLong((int) value);
            uintBytes.putInt((int) v);
            return Write(uintBytes);
        }

        public int ReadShort(short[] value) {
            final ByteBuffer shortBytes = ByteBuffer.allocate(2);
            final int result = Read(shortBytes);
            value[0] = LittleShort(shortBytes.getShort());
            return result;
        }

        public short ReadShort() {
            final short[] value = {0};
            this.ReadShort(value);

            return value[0];
        }

        public int WriteShort(final short value) {
            final ByteBuffer shortBytes = ByteBuffer.allocate(2);
            final short v = LittleShort(value);
            shortBytes.putShort(v);
            return Write(shortBytes);
        }

        public int ReadUnsignedShort(int[] value) {
            final ByteBuffer ushortBytes = ByteBuffer.allocate(2);
            final int result = Read(ushortBytes);
            value[0] = LittleShort(ushortBytes.getShort()) & 0xFFFF;
            return result;
        }

        public int ReadUnsignedShort() {
            final int[] value = {0};
            ReadUnsignedShort(value);

            return value[0];
        }

        public int WriteUnsignedShort(final int value) {
            final ByteBuffer ushortBytes = ByteBuffer.allocate(2);
            final short v = LittleShort((short) value);
            ushortBytes.putShort(v);
            return Write(ushortBytes);
        }

        public int ReadChar(short[] value) {
            final ByteBuffer charBytes = ByteBuffer.allocate(2);
            final int result = Read(charBytes);
            value[0] = charBytes.getShort();
            return result;
        }

        public short ReadChar() {
            final short[] value = {0};
            this.ReadChar(value);

            return value[0];
        }

        public int WriteChar(final short value) {
            final ByteBuffer charBytes = ByteBuffer.allocate(2);
            charBytes.putShort(value);
            return Write(charBytes);
        }

        public int WriteChar(final char value) {
            return WriteChar((short) value);
        }

        public int ReadUnsignedChar(char[] value) {
            final ByteBuffer ucharBytes = ByteBuffer.allocate(2);
            final int result = Read(ucharBytes);
            value[0] = ucharBytes.getChar();
            return result;
        }

        public int WriteUnsignedChar(final char value) {
            final ByteBuffer ucharBytes = ByteBuffer.allocate(2);
            ucharBytes.putChar(value);
            return Write(ucharBytes);
        }

        public int ReadFloat(float[] value) {
            final ByteBuffer floatBytes = ByteBuffer.allocate(4);
            final int result = Read(floatBytes);
            value[0] = LittleFloat(floatBytes.getFloat());
            return result;
        }

        public float ReadFloat() {
            final float[] value = {0};
            ReadFloat(value);

            return value[0];
        }

        public int WriteFloat(final float value) {
            final ByteBuffer floatBytes = ByteBuffer.allocate(4);
            final float v = LittleFloat(value);
            floatBytes.putFloat(v);
            return Write(floatBytes);
        }

        public int ReadBool(boolean[] value) {
            final char[] c = new char[1];
            final int result = ReadUnsignedChar(c);
            value[0] = (c[0] != '\0');
            return result;
        }

        public boolean ReadBool() {
            final boolean[] value = {false};
            ReadBool(value);

            return value[0];
        }

        public int WriteBool(final boolean value) {
            final char c = value ? 'c' : '\0';
            return WriteUnsignedChar(c);
        }

        public int ReadString(idStr string) {
            final int[] len = new int[1];
            int result = 0;
            ByteBuffer stringBytes;

            ReadInt(len);
            if (len[0] >= 0) {
                stringBytes = ByteBuffer.allocate(len[0] * 2);//2 bytes per char
//                string.Fill(' ', len[0]);
                result = Read(stringBytes);
                string.oSet(new String(stringBytes.array()));
            }
            return result;
        }

        public int WriteString(final String value) {
            int len;

            len = value.length();
            WriteInt(len);
            return Write(ByteBuffer.wrap(value.getBytes()));
        }

        public int WriteString(final char[] value) {
            return WriteString(ctos(value));
        }

        public int WriteString(final idStr value) {
            return WriteString(value.getData());
        }

        public int ReadVec2(idVec2 vec) {
            final ByteBuffer buffer = ByteBuffer.allocate(idVec2.SIZE);
            final int result = Read(buffer);
//            LittleRevBytes(vec.ToFloatPtr(), vec.GetDimension());//TODO:is this necessary?
            vec.oSet(new idVec2(buffer.getFloat(), buffer.getFloat()));
            return result;
        }

        public int WriteVec2(final idVec2 vec) {
//            idVec2 v = vec;
//            LittleRevBytes(v.ToFloatPtr(), v.GetDimension());
            final ByteBuffer buffer = ByteBuffer.allocate(idVec2.BYTES);
            buffer.asFloatBuffer()
                    .put(vec.ToFloatPtr())
                    .flip();
            return Write(buffer);
        }

        public int ReadVec3(idVec3 vec) {
            final ByteBuffer buffer = ByteBuffer.allocate(idVec3.SIZE);
            final int result = Read(buffer);
//            LittleRevBytes(vec.ToFloatPtr(), vec.GetDimension());
            vec.oSet(new idVec3(buffer.getFloat(), buffer.getFloat(), buffer.getFloat()));
            return result;
        }

        public int WriteVec3(final idVec3 vec) {
//            idVec3 v = vec;
//            LittleRevBytes(v.ToFloatPtr(), v.GetDimension());
            final ByteBuffer buffer = ByteBuffer.allocate(idVec3.BYTES);
            buffer.asFloatBuffer()
                    .put(vec.ToFloatPtr())
                    .flip();
            return Write(buffer);
        }

        public int ReadVec4(idVec4 vec) {
            final ByteBuffer buffer = ByteBuffer.allocate(idVec4.SIZE);
            final int result = Read(buffer);
//            LittleRevBytes(vec.ToFloatPtr(), vec.GetDimension());
            vec.oSet(new idVec4(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat()));
            return result;
        }

        public int WriteVec4(final idVec4 vec) {
//            idVec4 v = vec;
//            LittleRevBytes(v.ToFloatPtr(), v.GetDimension());
            final ByteBuffer buffer = ByteBuffer.allocate(idVec4.BYTES);
            buffer.asFloatBuffer()
                    .put(vec.ToFloatPtr())
                    .flip();
            return Write(buffer);
        }

        public int ReadVec6(idVec6 vec) {
            final ByteBuffer buffer = ByteBuffer.allocate(idVec6.SIZE);
            final int result = Read(buffer);
//            LittleRevBytes(vec.ToFloatPtr(), vec.GetDimension());
            vec.oSet(new idVec6(
                    buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
                    buffer.getFloat(), buffer.getFloat(), buffer.getFloat()));
            return result;
        }

        public int WriteVec6(final idVec6 vec) {
//            idVec6 v = vec;
//            LittleRevBytes(v.ToFloatPtr(), v.GetDimension());
            final ByteBuffer buffer = ByteBuffer.allocate(idVec6.BYTES);
            buffer.asFloatBuffer()
                    .put(vec.ToFloatPtr())
                    .flip();
            return Write(buffer);
        }

        public int ReadMat3(idMat3 mat) {
            final ByteBuffer buffer = ByteBuffer.allocate(idMat3.BYTES);
            final int result = Read(buffer);
//            LittleRevBytes(mat.ToFloatPtr(), mat.GetDimension());
            mat.oSet(new idMat3(
                    buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
                    buffer.getFloat(), buffer.getFloat(), buffer.getFloat(),
                    buffer.getFloat(), buffer.getFloat(), buffer.getFloat()));
            return result;
        }

        public int WriteMat3(final idMat3 mat) {
//            idMat3 v = mat;
//            LittleRevBytes(v.ToFloatPtr(), v.GetDimension());
            final ByteBuffer buffer = ByteBuffer.allocate(idMat3.BYTES);
            buffer.asFloatBuffer()
                    .put(mat.oGet(0).ToFloatPtr())
                    .put(mat.oGet(1).ToFloatPtr())
                    .put(mat.oGet(2).ToFloatPtr())
                    .flip();
            return Write(buffer);
        }

    }

    /*
     =================================================================================

     idFile_Memory

     =================================================================================
     */
    public static class idFile_Memory extends idFile {
        // friend class			idFileSystemLocal;

        private final idStr      name;        // name of the file
        private int        mode;        // open mode
        private int        maxSize;     // maximum size of file
        private int        fileSize;    // size of the file
        private int        allocated;   // allocated size
        private int        granularity; // file granularity
        private ByteBuffer filePtr;     // buffer holding the file data
        private int        curPtr;      // current read/write pointer
        //
        //

        public idFile_Memory() {	// file for writing without name
            this.name = new idStr("*unknown*");
            this.maxSize = 0;
            this.fileSize = 0;
            this.allocated = 0;
            this.granularity = 16384;

            this.mode = (1 << FS_WRITE.ordinal());
            this.filePtr = null;
            this.curPtr = 0;
        }

        public idFile_Memory(final String name) {	// file for writing
            this.name = new idStr(name);
            this.maxSize = 0;
            this.fileSize = 0;
            this.allocated = 0;
            this.granularity = 16384;

            this.mode = (1 << FS_WRITE.ordinal());
            this.filePtr = null;
            this.curPtr = 0;
        }

        public idFile_Memory(final String name, ByteBuffer data, int length) {	// file for writing
            this.name = new idStr(name);
            this.maxSize = length;
            this.fileSize = 0;
            this.allocated = length;
            this.granularity = 16384;

            this.mode = (1 << FS_WRITE.ordinal());
            this.filePtr = data;
            this.curPtr = 0;
        }
//public							idFile_Memory( const char *name, const char *data, int length );	// file for reading
//public						~idFile_Memory( void );
//

        @Override
        public String GetName() {
            return this.name.getData();
        }

        @Override
        public String GetFullPath() {
            return this.name.getData();
        }

        @Override
        public int Read(ByteBuffer buffer) {
            return Read(buffer, buffer.capacity());
        }

        @Override
        public int Read(ByteBuffer buffer, int len) {

            if (0 == (this.mode & (1 << etoi(FS_READ)))) {
                common.FatalError("idFile_Memory::Read: %s not opened in read mode", this.name);
                return 0;
            }

            if ((this.curPtr + len) > this.fileSize) {
                len = this.fileSize - this.curPtr;
            }
//            memcpy(buffer, curPtr, len);
            this.filePtr.get(buffer.array(), this.curPtr, len);
            this.curPtr += len;
            return len;
        }

        @Override
        public int Write(final ByteBuffer buffer/*, int len*/) {
            final int len = buffer.capacity();

            if (0 == (this.mode & (1 << etoi(FS_WRITE)))) {
                common.FatalError("idFile_Memory::Write: %s not opened in write mode", this.name);
                return 0;
            }

            final int alloc = (this.curPtr + len + 1) - this.allocated; // need room for len+1
            if (alloc > 0) {
                if (this.maxSize != 0) {
                    common.Error("idFile_Memory::Write: exceeded maximum size %d", this.maxSize);
                    return 0;
                }
                final int extra = this.granularity * (1 + (alloc / this.granularity));
                final ByteBuffer newPtr = ByteBuffer.allocate(this.allocated + extra);// Heap.Mem_Alloc(allocated + extra);
                if (this.allocated != 0) {
//                    memcpy(newPtr, filePtr, allocated);
                    //copy old data to new array
                    newPtr.put(this.filePtr);
                }
                this.allocated += extra;
//                curPtr = newPtr + (curPtr - filePtr);
//                if (filePtr != null) {
//                    Mem_Free(filePtr);
//                    filePtr = null;
//                }
                //copy new (resized) array to old one
                this.filePtr = newPtr;
            }
//            memcpy(curPtr, buffer, len);
            this.filePtr.position(this.curPtr);
            this.filePtr.put(buffer);
            this.curPtr += len;
            this.fileSize += len;
            this.filePtr.put(this.fileSize, (byte) 0); // len + 1
            return len;
        }

        @Override
        public int Length() {
            return this.fileSize;
        }

        @Override
        public long Timestamp() {
            return 0;
        }

        @Override
        public int Tell() {
            return this.curPtr;
        }

        @Override
        public void ForceFlush() {
        }

        @Override
        public void Flush() {
        }

        /*
         =================
         idFile_Memory::Seek

         returns zero(true) on success and -1(false) on failure
         =================
         */
        @Override
        public boolean Seek(long offset, fsOrigin_t origin) {

            switch (origin) {
                case FS_SEEK_CUR: {
                    this.curPtr += offset;
                    break;
                }
                case FS_SEEK_END: {
                    this.curPtr = (int) (this.fileSize - offset);
                    break;
                }
                case FS_SEEK_SET: {
                    this.curPtr = (int) offset;
                    break;
                }
                default: {
                    common.FatalError("idFile_Memory::Seek: bad origin for %s\n", this.name);
                    return false;//-1;
                }
            }
            if (this.curPtr < /*filePtr*/ 0) {
//		curPtr = filePtr;
                this.curPtr = 0;
                return false;//-1;
            }
            if (this.curPtr > this./*filePtr +*/ fileSize) {
//		curPtr = filePtr + fileSize;
                this.curPtr = this.fileSize;//TODO:-1
                return false;//-1;
            }
            return true;//0;
        }
//

        // changes memory file to read only
        public void MakeReadOnly() {
            this.mode = (1 << FS_READ.ordinal());
            Rewind();
        }

        public void Clear() {
            Clear(true);
        }

        // clear the file
        public void Clear(boolean freeMemory /*= true*/) {
            this.fileSize = 0;
            this.granularity = 16384;
            if (freeMemory) {
                this.allocated = 0;
//		Mem_Free( filePtr );
                this.filePtr = null;
                this.curPtr = 0;
            } else {
                this.curPtr = 0;
            }
        }

        // set data for reading
        public void SetData(final ByteBuffer data, final int length) {
            this.maxSize = 0;
            this.fileSize = length;
            this.allocated = 0;
            this.granularity = 16384;

            this.mode = (1 << etoi(FS_READ));
            this.filePtr = data.duplicate();
            this.curPtr = 0;
        }

        // returns const pointer to the memory buffer
        public ByteBuffer GetDataPtr() {
            return this.filePtr;
        }

        // set the file granularity
        public void SetGranularity(int g) {
            assert (g > 0);
            this.granularity = g;
        }
    }

    /*
     =================================================================================

     idFile_BitMsg

     =================================================================================
     */
    public static class idFile_BitMsg extends idFile {
        // friend class			idFileSystemLocal;

        private final idStr    name;            // name of the file
        private final int      mode;            // open mode
        private final idBitMsg msg;
        //
        //

        public idFile_BitMsg(idBitMsg msg) {
            this.name = new idStr("*unknown*");
            this.mode = (1 << FS_WRITE.ordinal());
            this.msg = msg;
        }

        public idFile_BitMsg(final idBitMsg msg, boolean readOnly) {
            this.name = new idStr("*unknown*");
            this.mode = (1 << FS_READ.ordinal());
            this.msg = msg;
        }
// public	virtual					~idFile_BitMsg( void );

        @Override
        public String GetName() {
            return this.name.getData();
        }

        @Override
        public String GetFullPath() {
            return this.name.getData();
        }

        @Override
        public int Read(ByteBuffer buffer) {
            return Read(buffer, buffer.capacity());
        }

        @Override
        public int Read(ByteBuffer buffer, int len) {

            if (0 == (this.mode & (1 << FS_READ.ordinal()))) {
                common.FatalError("idFile_BitMsg::Read: %s not opened in read mode", this.name);
                return 0;
            }

            return this.msg.ReadData(buffer, len);//TODO:cast self to self???????
        }

        @Override
        public int Write(final ByteBuffer buffer/*, int len*/) {
            final int len = buffer.capacity();

            if (0 == (this.mode & (1 << FS_WRITE.ordinal()))) {
                common.FatalError("idFile_Memory::Write: %s not opened in write mode", this.name);
                return 0;
            }

            this.msg.WriteData(buffer, len);
            return len;
        }

        @Override
        public int Length() {
            return this.msg.GetSize();
        }

        @Override
        public long Timestamp() {
            return 0;
        }

        @Override
        public int Tell() {
            if ((this.mode & FS_READ.ordinal()) != 0) {
                return this.msg.GetReadCount();
            } else {
                return this.msg.GetSize();
            }
        }

        @Override
        public void ForceFlush() {
        }

        @Override
        public void Flush() {
        }

        /*
         =================
         idFile_BitMsg::Seek

         returns zero on success and -1 on failure
         =================
         */
        @Override
        public boolean Seek(long offset, fsOrigin_t origin) {
            return false;//-1;
        }
    }

    /*
     =================================================================================

     idFile_Permanent

     =================================================================================
     */
    public static class idFile_Permanent extends idFile {
        // friend class			idFileSystemLocal;

        idStr       name;        // relative path of the file - relative path
        idStr       fullPath;    // full file path - OS path
        int         mode;        // open mode
        int         fileSize;    // size of the file
        FileChannel o;           // file handle
        boolean     handleSync;  // true if written data is immediately flushed
        //
        //

        public idFile_Permanent() {
            this.name = new idStr("invalid");
            this.fullPath = new idStr();
            this.o = null;
            this.mode = 0;
            this.fileSize = 0;
            this.handleSync = false;
        }
// public	virtual					~idFile_Permanent( void );

        @Override
        public String GetName() {
            return this.name.getData();
        }

        @Override
        public String GetFullPath() {
            return this.fullPath.getData();
        }

        @Override
        public int Read(ByteBuffer buffer) {
            return Read(buffer, buffer.capacity());
        }

        /*
         =================
         idFile_Permanent::Read

         Properly handles partial reads
         =================
         */
        @Override
        public int Read(ByteBuffer buffer, int len) {
            final int block;
			int remaining;
            int read;
//            byte[] buf;
            boolean tries;

            if (0 == (this.mode & (1 << FS_READ.ordinal()))) {
                common.FatalError("idFile_Permanent::Read: %s not opened in read mode", this.name);
                return 0;
            }

            if (null == this.o) {
                return 0;
            }

//            buf = (byte[]) buffer;
            remaining = len;
            tries = false;
            try {
                while (remaining != 0) {
//                block = remaining;
//                read = fread(buf, 1, block, o);
                    read = this.o.read(buffer);
                    if (read == 0) {
                        // we might have been trying to read from a CD, which
                        // sometimes returns a 0 read on windows
                        if (!tries) {
                            tries = true;
                        } else {
                            fileSystem.AddToReadCount(len - remaining);
                            return len - remaining;
                        }
                    }

                    if (read == -1) {
                        common.FatalError("idFile_Permanent::Read: -1 bytes read from %s", this.name);
                    }

                    remaining -= read;
//                buf += read;
                }
            } catch (final IOException ex) {
                Logger.getLogger(File_h.class.getName()).log(Level.SEVERE, null, ex);
            }
            fileSystem.AddToReadCount(len);
            return len;
        }

        /*
         =================
         idFile_Permanent::Write

         Properly handles partial writes
         =================
         */
        @Override
        public int Write(final ByteBuffer buffer) {
            return Write(buffer, buffer.limit());
        }

        @Override
        public int Write(final ByteBuffer buffer, int len) {
            int block, remaining;
            int written;
//            byte[] buf;
            int tries;

            if (0 == (this.mode & (1 << FS_WRITE.ordinal()))) {
                common.FatalError("idFile_Permanent::Write: %s not opened in write mode", this.name);
                return 0;
            }

            if (NOT(this.o)) {
                return 0;
            }

//            buf = (byte[]) buffer;

            remaining = len;
            tries = 0;
            try {
                while (remaining != 0) {
                    block = remaining;
                    //                written = fwrite(buf, 1, block, o);
                    written = this.o.write(buffer);
                    if (written == 0) {
                        if (0 == tries) {
                            tries = 1;
                        } else {
                            common.Printf("idFile_Permanent::Write: 0 bytes written to %s\n", this.name);
                            return 0;
                        }
                    }

                    if (written == -1) {
                        common.Printf("idFile_Permanent::Write: -1 bytes written to %s\n", this.name);
                        return 0;
                    }

                    remaining -= written;
//                buf += written;
                    this.fileSize += written;
                }
                if (this.handleSync) {
                    this.o.force(false);
                }
            } catch (final IOException ex) {
                Logger.getLogger(File_h.class.getName()).log(Level.SEVERE, null, ex);
            }
            return len;
        }

        @Override
        public int Length() {
            return this.fileSize;
        }

        @Override
        public long Timestamp() {
            return Sys_FileTimeStamp(GetFullPath());
        }

        @Override
        public int Tell() {
            try {
                return (int) this.o.position();//return ftell(o);
            } catch (final IOException ex) {
                Logger.getLogger(File_h.class.getName()).log(Level.SEVERE, null, ex);
            }
            return -1;
        }

        @Override
        public void ForceFlush() {
//            setvbuf(o, null, _IONBF, 0);
        }

        @Override
        public void Flush() {
            try {
                this.o.force(false);
            } catch (final IOException ex) {
                Logger.getLogger(File_h.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /*
         =================
         idFile_Permanent::Seek

         returns zero on success and -1 on failure
         =================
         */
        @Override
        public boolean Seek(long offset, fsOrigin_t origin) {
            long _origin = 0;

            try {
                switch (origin) {
                    case FS_SEEK_CUR:
                        _origin = this.o.position();//SEEK_CUR;
                        break;
                    case FS_SEEK_END:
                        _origin = this.o.size();//SEEK_END;
                        break;
                    case FS_SEEK_SET:
                        _origin = 0;//SEEK_SET;
                        break;
                    default:
                        _origin = this.o.position();//SEEK_CUR;
                        common.FatalError("idFile_Permanent::Seek: bad origin for %s\n", this.name);
                        break;
                }
            } catch (final IOException ex) {
                Logger.getLogger(File_h.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                //            return fseek(o, offset, _origin);
                return this.o.position(_origin) != null;
            } catch (final IOException ex) {
                Logger.getLogger(File_h.class.getName()).log(Level.SEVERE, null, ex);
            }

            return false;
        }

        // returns file pointer
        public FileChannel GetFilePtr() {
            return this.o;
        }

    }

    /*
     =================================================================================

     idFile_InZip

     =================================================================================
     */
    static class idFile_InZip extends idFile {
        // friend class			idFileSystemLocal;

        idStr    name;                  // name of the file in the pak
        idStr    fullPath;              // full file path including pak file name
        int      zipFilePos;            // zip file info position in pak
        int      fileSize;              // size of the file
        ZipEntry z;                     // unzip info //TODO:use faster zip method
        private int         byteCounter;// current offset within zip archive.
        private InputStream inputStream;
        //
        //

        public idFile_InZip() {
            this.name = new idStr("invalid");
            this.fullPath = new idStr();
            this.zipFilePos = 0;
            this.fileSize = 0;
            this.byteCounter = 0;
            // memset( &z, 0, sizeof( z ) );//TODO:size of void ptr
        }

// public	virtual					~idFile_InZip( void );
        @Override
        public String GetName() {
            return this.name.getData();
        }

        @Override
        public String GetFullPath() {
            return this.fullPath.oPlus('/').oPlus(this.name).getData();
        }

        @Override
        public int Read(ByteBuffer buffer) {
            return this.Read(buffer, buffer.capacity());
        }

        @Override
        public int Read(ByteBuffer buffer, int len) {
            int l = 0;
            int read = 0;

            try {
                if (null == this.inputStream) {
                    this.inputStream = new ZipFile(this.fullPath.getData()).getInputStream(this.z);
                }

                while ((read > -1) && (len != 0)) {
                    read = this.inputStream.read(buffer.array(), l, len);
                    l += read;
                    len -= read;
                }
            } catch (final IOException ex) {
                common.FatalError("idFile_InZip::Read: error whilest reading from %s", this.name);
            }

            fileSystem.AddToReadCount(l);
            this.byteCounter += l;
            return l;
        }

        @Override
        public int Write(final ByteBuffer buffer/*, int len*/) {
            common.FatalError("idFile_InZip::Write: cannot write to the zipped file %s", this.name);
            return 0;
        }

        @Override
        public int Length() {
            return this.fileSize;
        }

        @Override
        public long Timestamp() {
            return 0;
        }

        @Override
        public int Tell() {
            return this.byteCounter;
        }

        @Override
        public void ForceFlush() {
            common.FatalError("idFile_InZip::ForceFlush: cannot flush the zipped file %s", this.name);
        }

        @Override
        public void Flush() {
            common.FatalError("idFile_InZip::Flush: cannot flush the zipped file %s", this.name);
        }

        /*
         =================
         idFile_InZip::Seek

         returns zero on success and -1 on failure
         =================
         */
        static final int ZIP_SEEK_BUF_SIZE = 1 << 15;

        @Override
        public boolean Seek(long offset, fsOrigin_t origin) {
            int res, i;
            ByteBuffer buf;

            switch (origin) {
                case FS_SEEK_END:
                    offset = this.fileSize - offset;

                case FS_SEEK_SET: {
                    // set the file position in the zip file (also sets the current file info)
//                    unzSetCurrentFileInfoPosition( z, zipFilePos );
                    unzOpenCurrentFile();
                    if (offset <= 0) {
                        return true;//0;
                    }
                }
                case FS_SEEK_CUR: {//TODO: negative offsets?
                    buf = ByteBuffer.allocate(ZIP_SEEK_BUF_SIZE);
                    for (i = 0; i < (offset - ZIP_SEEK_BUF_SIZE); i += ZIP_SEEK_BUF_SIZE) {
                        res = Read(buf, ZIP_SEEK_BUF_SIZE);
                        if (res < ZIP_SEEK_BUF_SIZE) {
                            return false;//-1;
                        }
                    }

                    res = i + Read(buf, (int) offset - i);
                    return (res == offset);//? 0 : -1;
                }
                default: {
                    common.FatalError("idFile_InZip::Seek: bad origin for %s\n", this.name);
                    break;
                }
            }
            return false;//-1;
        }

        private void unzOpenCurrentFile() {
            try {
                this.byteCounter = 0;        //reset counter.
                if (this.inputStream != null) {//FS_SEEK_SET -> FS_SEEK_CUR
                    this.inputStream.close();
                }
            } catch (final IOException ex) {
                common.FatalError("idFile_InZip::unzOpenCurrentFile: we're in deep shit bub \n");
            }
            this.inputStream = null; //reload inputStream.
        }
    }
}
