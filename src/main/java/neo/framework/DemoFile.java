package neo.framework;

import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_SYSTEM;
import static neo.framework.Common.common;
import static neo.framework.DemoFile.demoSystem_t.DS_FINISHED;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Licensee.GAME_NAME;
import static neo.idlib.Text.Str.va;

import java.nio.ByteBuffer;

import neo.framework.CVarSystem.idCVar;
import neo.framework.Compressor.idCompressor;
import neo.framework.File_h.idFile;
import neo.framework.File_h.idFile_Memory;
import neo.idlib.Dict_h.idDict;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;

/**
 *
 */
public class DemoFile {

    static final String DEMO_MAGIC = GAME_NAME + " RDEMO";

    /*
     ===============================================================================

     Demo file

     ===============================================================================
     */
    public enum demoSystem_t {

        DS_FINISHED,
        DS_RENDER,
        DS_SOUND,
        DS_VERSION
    }

    public static class idDemoFile extends idFile {

        private boolean writing;
        private ByteBuffer fileImage;
        private idFile f;
        private idCompressor compressor;
        //
        private idList<idStr> demoStrings;
        private idFile fLog;
        private boolean log;
        private idStr logStr;
        //
        private static final idCVar com_logDemos = new idCVar("com_logDemos", "0", CVAR_SYSTEM | CVAR_BOOL, "Write demo.log with debug information in it");
        private static final idCVar com_compressDemos = new idCVar("com_compressDemos", "1", CVAR_SYSTEM | CVAR_INTEGER | CVAR_ARCHIVE, "Compression scheme for demo files\n0: None    "
                + "(Fast, large files)\n1: LZW     (Fast to compress, Fast to decompress, medium/small files)\n2: LZSS    (Slow to compress, Fast to decompress, small files)\n3: Huffman "
                + "(Fast to compress, Slow to decompress, medium files)\nSee also: The 'CompressDemo' command");
        private static final idCVar com_preloadDemos = new idCVar("com_preloadDemos", "0", CVAR_SYSTEM | CVAR_BOOL | CVAR_ARCHIVE, "Load the whole demo in to RAM before running it");
        //
        //

        public idDemoFile() {
            this.f = null;
            this.fLog = null;
            this.log = false;
            this.fileImage = null;
            this.compressor = null;
            this.writing = false;
        }
//					~idDemoFile();

        @Override
        public String GetName() {
            return (this.f != null ? this.f.GetName() : "");
        }

        @Override
        public String GetFullPath() {
            return (this.f != null ? this.f.GetFullPath() : "");
        }

        public void SetLog(boolean b, final String p) {
            this.log = b;
            if (p != null) {
                this.logStr = new idStr(p);
            }
        }

        public void Log(final String p) {
            if ((this.fLog != null) && (p != null) && !p.isEmpty()) {
                this.fLog.WriteString(p/*, strlen(p)*/);
            }
        }
        static final int magicLen = DEMO_MAGIC.length();

        public boolean OpenForReading(final String fileName) {
            final ByteBuffer magicBuffer = ByteBuffer.allocate(magicLen);
            final int[] compression = new int[1];
            int fileLength;

            Close();

            this.f = fileSystem.OpenFileRead(fileName);
            if (null == this.f) {
                return false;
            }

            fileLength = this.f.Length();

            if (com_preloadDemos.GetBool()) {
                this.fileImage = ByteBuffer.allocate(fileLength);// Mem_Alloc(fileLength);
                this.f.Read(this.fileImage, fileLength);
                fileSystem.CloseFile(this.f);
                this.f = new idFile_Memory(va("preloaded(%s)", fileName), this.fileImage, fileLength);//TODO:should fileImage be a reference??
            }

            if (com_logDemos.GetBool()) {
                this.fLog = fileSystem.OpenFileWrite("demoread.log");
            }

            this.writing = false;

            this.f.Read(magicBuffer);//, magicLen);
            if (DEMO_MAGIC.equals(new String(magicBuffer.array()).substring(0, magicLen))) {
//	if ( memcmp(magicBuffer, DEMO_MAGIC, magicLen) == 0 ) {
                this.f.ReadInt(compression);
            } else {
                // Ideally we would error out if the magic string isn't there,
                // but for backwards compatibility we are going to assume it's just an uncompressed demo file
                compression[0] = 0;
                this.f.Rewind();
            }

            this.compressor = AllocCompressor(compression[0]);
            this.compressor.Init(this.f, false, 8);

            return true;
        }

        public boolean OpenForWriting(final String fileName) {
            Close();

            this.f = fileSystem.OpenFileWrite(fileName);
            if (this.f == null) {
                return false;
            }

            if (com_logDemos.GetBool()) {
                this.fLog = fileSystem.OpenFileWrite("demowrite.log");
            }

            this.writing = true;

            this.f.WriteString(DEMO_MAGIC/*, sizeof(DEMO_MAGIC)*/);
            this.f.WriteInt(com_compressDemos.GetInteger());
            this.f.Flush();

            this.compressor = AllocCompressor(com_compressDemos.GetInteger());
            this.compressor.Init(this.f, true, 8);

            return true;
        }

        public void Close() {
            if (this.writing && (this.compressor != null)) {
                this.compressor.FinishCompress();
            }

            if (this.f != null) {
                fileSystem.CloseFile(this.f);
                this.f = null;
            }
            if (this.fLog != null) {
                fileSystem.CloseFile(this.fLog);
                this.fLog = null;
            }
            if (this.fileImage != null) {
//                Mem_Free(fileImage);
                this.fileImage = null;
            }
            if (this.compressor != null) {
//		delete compressor;
                this.compressor = null;
            }

            this.demoStrings.DeleteContents(true);
        }

        public String ReadHashString() throws idException {
            final int[] index = new int[1];

            if (this.log && (this.fLog != null)) {
                final String text = va("%s > Reading hash string\n", this.logStr.toString());
                this.fLog.WriteString(text);
            }

            ReadInt(index);

            if (index[0] == -1) {
                // read a new string for the table
                idStr str;

                final idStr data = new idStr();
                ReadString(data);
                str = data;

                this.demoStrings.Append(str);

                return str.toString();
            }

            if ((index[0] < -1) || (index[0] >= this.demoStrings.Num())) {
                Close();
                common.Error("demo hash index out of range");
            }

            return this.demoStrings.oGet(index[0]).toString();//TODO:return c_str?
        }

        public void WriteHashString(final String str) {
            if (this.log && (this.fLog != null)) {
                final String text = va("%s > Writing hash string\n", this.logStr.toString());
                this.fLog.WriteString(text);
            }
            // see if it is already in the has table
            for (int i = 0; i < this.demoStrings.Num(); i++) {
                if (this.demoStrings.oGet(i).toString().equals(str)) {
                    WriteInt(i);
                    return;
                }
            }

            // add it to our table and the demo table
            final idStr copy = new idStr(str);
//common.Printf( "hash:%i = %s\n", demoStrings.Num(), str );
            this.demoStrings.Append(copy);
            final int cmd = -1;
            WriteInt(cmd);
            WriteString(str);
        }

        public void ReadDict(idDict dict) throws idException {
            int i;
            final int[] c = new int[1];
            String key, val;

            dict.Clear();
            ReadInt(c);
            for (i = 0; i < c[0]; i++) {
                key = ReadHashString();
                val = ReadHashString();
                dict.Set(key, val);
            }
        }

        public void WriteDict(final idDict dict) {
            int i, c;

            c = dict.GetNumKeyVals();
            WriteInt(c);
            for (i = 0; i < c; i++) {
                WriteHashString(dict.GetKeyVal(i).GetKey().toString());
                WriteHashString(dict.GetKeyVal(i).GetValue().toString());
            }
        }

        @Override
        public int Read(ByteBuffer buffer, int len) {
            final int read = this.compressor.Read(buffer, len);
            if ((read == 0) && (len >= 4)) {
//                *(demoSystem_t *)buffer = DS_FINISHED;
                buffer.putInt(DS_FINISHED.ordinal());
            }
            return read;
        }

        @Override
        public int Write(final ByteBuffer buffer, int len) {
            return this.compressor.Write(buffer, len);
        }

        private static idCompressor AllocCompressor(int type) {
            switch (type) {
                case 0:
                    return idCompressor.AllocNoCompression();
                default:
                case 1:
                    return idCompressor.AllocLZW();
                case 2:
                    return idCompressor.AllocLZSS();
                case 3:
                    return idCompressor.AllocHuffman();
            }
        }

    }
}
