package neo.Sound;

import static neo.Sound.snd_local.WAVE_FORMAT_TAG_OGG;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_PCM;
import static neo.Sound.snd_system.idSoundSystemLocal.s_realTimeDecoding;
import static neo.TempDump.stobb;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_SET;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.LittleRevBytes;
import static neo.idlib.Lib.LittleShort;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.sys.sys_public.CRITICAL_SECTION_ONE;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;

import java.nio.ByteBuffer;

//import org.lwjgl.stb.STBVorbis;
//import org.lwjgl.stb.STBVorbisInfo;

import neo.TempDump.TODO_Exception;
import neo.Sound.snd_local.mminfo_s;
import neo.Sound.snd_local.pcmwaveformat_s;
import neo.Sound.snd_local.waveformatex_s;
import neo.Sound.snd_local.waveformatextensible_s;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Str.idStr;
import neo.openal.Vorbis;
import neo.opengl.Nio;

/**
 *
 */
public class snd_wavefile {

    static long mmioFOURCC(int ch0, int ch1, int ch2, int ch3) {
        return ((byte) ch0
                | (((byte) ch1) << 8)
                | (((byte) ch2) << 16)
                | (((byte) ch3) << 24));
    }

    static final long fourcc_riff = mmioFOURCC('R', 'I', 'F', 'F');

    /*
     ===================================================================================

     idWaveFile

     ===================================================================================
     */
    public static class idWaveFile {

        public  waveformatextensible_s mpwfx;       // Pointer to waveformatex structure
        //
        private idFile                 mhmmio;      // I/O handle for the WAVE
        private final mminfo_s               mck;         // Multimedia RIFF chunk
        private final mminfo_s               mckRiff;     // used when opening a WAVE file
        private long/*dword*/          mdwSize;     // size in samples
        private long/*dword*/          mMemSize;    // size of the wave data in memory
        private long/*dword*/          mseekBase;
        private long/*ID_TIME_T*/      mfileTime;
        //
        private boolean                mbIsReadingFromMemory;
        private ByteBuffer             mpbData;
        private ByteBuffer             mpbDataCur;
        private long/*dword*/          mulDataSize;
        //
        private Object                 ogg;         // only !NULL when !s_realTimeDecoding
        private boolean                isOgg;
        //
        //

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::idWaveFile()
        // Desc: Constructs the class.  Call Open() to open a wave file for reading.  
        //       Then call Read() as needed.  Calling the destructor or Close() 
        //       will close the file.  
        //-----------------------------------------------------------------------------
        public idWaveFile() {
//	memset( &mpwfx, 0, sizeof( waveformatextensible_t ) );
            this.mpwfx = new waveformatextensible_s();
            this.mhmmio = null;
            this.mck = new mminfo_s();
            this.mckRiff = new mminfo_s();
            this.mdwSize = 0;
            this.mseekBase = 0;
            this.mbIsReadingFromMemory = false;
            this.mpbData = null;
            this.ogg = null;
            this.isOgg = false;
        }
        // ~idWaveFile();

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::Open()
        // Desc: Opens a wave file for reading
        //-----------------------------------------------------------------------------
        public int Open(final String strFileName, waveformatex_s[] pwfx /*= NULL*/) {

            this.mbIsReadingFromMemory = false;

            this.mpbData = null;
            this.mpbDataCur = this.mpbData;

            if (strFileName == null) {
                return -1;
            }

            final idStr name = new idStr(strFileName);

            // note: used to only check for .wav when making a build
            name.SetFileExtension(".ogg");
            if (fileSystem.ReadFile(name.getData(), null, null) != -1) {
                return OpenOGG(name.getData(), pwfx);
            }

//	memset( &mpwfx, 0, sizeof( waveformatextensible_t ) );
            this.mpwfx = new waveformatextensible_s();

            this.mhmmio = fileSystem.OpenFileRead(strFileName);
            if (null == this.mhmmio) {
                this.mdwSize = 0;
                return -1;
            }
            if (this.mhmmio.Length() <= 0) {
                this.mhmmio = null;
                return -1;
            }
            if (ReadMMIO() != 0) {
                // ReadMMIO will fail if its an not a wave file
                Close();
                return -1;
            }

            this.mfileTime = this.mhmmio.Timestamp();

            if (ResetFile() != 0) {
                Close();
                return -1;
            }

            // After the reset, the size of the wav file is mck.cksize so store it now
            this.mdwSize = this.mck.cksize / Short.BYTES;
            this.mMemSize = this.mck.cksize;

            if (this.mck.cksize != 0xffffffff) {
                if (pwfx != null) {
                    pwfx[0] = new waveformatex_s(this.mpwfx.Format);//memcpy(pwfx, (waveformatex_t *) & mpwfx, sizeof(waveformatex_t));
                }
                return 0;
            }
            return -1;
        }

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::OpenFromMemory()
        // Desc: copy data to idWaveFile member variable from memory
        //-----------------------------------------------------------------------------
        public int OpenFromMemory(short[] pbData, int ulDataSize, waveformatextensible_s pwfx) {
            this.mpwfx = pwfx;
            this.mulDataSize = ulDataSize;
            this.mpbData = stobb(pbData);
            this.mpbDataCur = this.mpbData.duplicate();
            this.mdwSize = ulDataSize / 2;//sizeof(short);
            this.mMemSize = ulDataSize;
            this.mbIsReadingFromMemory = true;

            return 0;
        }

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::Read()
        // Desc: Reads section of data from a wave file into pBuffer and returns 
        //       how much read in pdwSizeRead, reading not more than dwSizeToRead.
        //       This uses mck to determine where to start reading from.  So 
        //       subsequent calls will be continue where the last left off unless 
        //       Reset() is called.
        //-----------------------------------------------------------------------------
        public int Read(ByteBuffer pBuffer, int dwSizeToRead, int[] pdwSizeRead) {

            if (this.ogg != null) {

                return ReadOGG(pBuffer.array(), dwSizeToRead, pdwSizeRead);

            } else if (this.mbIsReadingFromMemory) {

                if (this.mpbDataCur == null) {
                    return -1;
                }
                final int pos = dwSizeToRead + this.mpbDataCur.position();//add current offset
                if (this.mpbDataCur.get(dwSizeToRead) > this.mpbData.get((int) this.mulDataSize)) {
                    dwSizeToRead = (int) (this.mulDataSize - this.mpbDataCur.position());
                }
                SIMDProcessor.Memcpy(pBuffer, this.mpbDataCur, dwSizeToRead);
                this.mpbDataCur.position(pos);

                if (pdwSizeRead != null) {
                    pdwSizeRead[0] = dwSizeToRead;
                }

                return dwSizeToRead;

            } else {

                if (this.mhmmio == null) {
                    return -1;
                }
                if (pBuffer == null) {
                    return -1;
                }

                dwSizeToRead = this.mhmmio.Read(pBuffer, dwSizeToRead);
                // this is hit by ogg code, which does it's own byte swapping internally
                if (!this.isOgg) {
                    LittleRevBytes(pBuffer.array(), 2, dwSizeToRead / 2);
                }

                if (pdwSizeRead != null) {
                    pdwSizeRead[0] = dwSizeToRead;
                }

                return dwSizeToRead;
            }
        }

        public int Seek(int offset) {
            throw new TODO_Exception();
//
//            if (ogg != null) {
//
//                common.FatalError("idWaveFile::Seek: cannot seek on an OGG file\n");
//
//            } else if (mbIsReadingFromMemory) {
//
//                mpbDataCur = mpbData + offset;
//
//            } else {
//                if (mhmmio == null) {
//                    return -1;
//                }
//
//                if ((int) (offset + mseekBase) == mhmmio.Tell()) {
//                    return 0;
//                }
//                mhmmio.Seek(offset + mseekBase, FS_SEEK_SET);
//                return 0;
//            }
//            return -1;
        }

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::Close()
        // Desc: Closes the wave file 
        //-----------------------------------------------------------------------------
        public int Close() {
            if (this.ogg != null) {
                return CloseOGG();
            }
            if (this.mhmmio != null) {
                fileSystem.CloseFile(this.mhmmio);
                this.mhmmio = null;
            }
            return 0;
        }

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::ResetFile()
        // Desc: Resets the internal mck pointer so reading starts from the 
        //       beginning of the file again 
        //-----------------------------------------------------------------------------
        public int ResetFile() {
            if (this.mbIsReadingFromMemory) {
                this.mpbDataCur = this.mpbData;
            } else {
                if (this.mhmmio == null) {
                    return -1;
                }

                // Seek to the data
                if (!this.mhmmio.Seek(this.mckRiff.dwDataOffset + Integer.BYTES, FS_SEEK_SET)) {
                    return -1;
                }

                // Search the input file for for the 'fmt ' chunk.
                this.mck.ckid = 0;
                do {
                    final ByteBuffer ioin = ByteBuffer.allocate(1);
                    if (0 == this.mhmmio.Read(ioin, 1)) {
                        return -1;
                    }
                    this.mck.ckid = Integer.toUnsignedLong((int) (this.mck.ckid >>> 8) | (ioin.get() << 24));
                } while (this.mck.ckid != mmioFOURCC('d', 'a', 't', 'a'));

                this.mck.cksize = this.mhmmio.ReadInt();
                assert (!this.isOgg);
                this.mck.cksize = LittleLong(this.mck.cksize);
                this.mseekBase = this.mhmmio.Tell();
            }

            return 0;
        }

        public int GetOutputSize() {
            return (int) this.mdwSize;
        }

        public int GetMemorySize() {
            return (int) this.mMemSize;
        }

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::ReadMMIO()
        // Desc: Support function for reading from a multimedia I/O stream.
        //       mhmmio must be valid before calling.  This function uses it to
        //       update mckRiff, and mpwfx. 
        //-----------------------------------------------------------------------------
        private int ReadMMIO() {
            final mminfo_s ckIn = new mminfo_s(); // chunk info. for general use.
            final pcmwaveformat_s pcmWaveFormat = new pcmwaveformat_s();  // Temp PCM structure to load in.       

            this.mpwfx = new waveformatextensible_s();//memset( &mpwfx, 0, sizeof( waveformatextensible_t ) );

            this.mhmmio.Read(this.mckRiff, 12);
            assert (!this.isOgg);
            this.mckRiff.ckid = LittleLong(this.mckRiff.ckid);
            this.mckRiff.cksize = LittleLong(this.mckRiff.cksize);
            this.mckRiff.fccType = LittleLong(this.mckRiff.fccType);
            this.mckRiff.dwDataOffset = 12;

            // Check to make sure this is a valid wave file
            if ((this.mckRiff.ckid != fourcc_riff) || (this.mckRiff.fccType != mmioFOURCC('W', 'A', 'V', 'E'))) {
                return -1;
            }

            // Search the input file for for the 'fmt ' chunk.
            ckIn.dwDataOffset = 12;
            do {
                if (8 != this.mhmmio.Read(ckIn, 8)) {
                    return -1;
                }
                assert (!this.isOgg);
                ckIn.ckid = LittleLong(ckIn.ckid);
                ckIn.cksize = LittleLong(ckIn.cksize);
                ckIn.dwDataOffset += ckIn.cksize - 8;
            } while (ckIn.ckid != mmioFOURCC('f', 'm', 't', ' '));

            // Expect the 'fmt' chunk to be at least as large as <PCMWAVEFORMAT>;
            // if there are extra parameters at the end, we'll ignore them
            if (ckIn.cksize < pcmwaveformat_s.BYTES) {
                return -1;
            }

            // Read the 'fmt ' chunk into <pcmWaveFormat>.
            if (this.mhmmio.Read(pcmWaveFormat) != pcmwaveformat_s.BYTES) {
                return -1;
            }
            assert (!this.isOgg);
            pcmWaveFormat.wf.wFormatTag = LittleShort((short) pcmWaveFormat.wf.wFormatTag);
            pcmWaveFormat.wf.nChannels = LittleShort((short) pcmWaveFormat.wf.nChannels);
            pcmWaveFormat.wf.nSamplesPerSec = LittleLong(pcmWaveFormat.wf.nSamplesPerSec);
            pcmWaveFormat.wf.nAvgBytesPerSec = LittleLong(pcmWaveFormat.wf.nAvgBytesPerSec);
            pcmWaveFormat.wf.nBlockAlign = LittleShort((short) pcmWaveFormat.wf.nBlockAlign);
            pcmWaveFormat.wBitsPerSample = LittleShort((short) pcmWaveFormat.wBitsPerSample);

            // Copy the bytes from the pcm structure to the waveformatex_t structure
            this.mpwfx = new waveformatextensible_s(pcmWaveFormat);

            // Allocate the waveformatex_t, but if its not pcm format, read the next
            // word, and thats how many extra bytes to allocate.
            if (pcmWaveFormat.wf.wFormatTag == WAVE_FORMAT_TAG_PCM) {
                this.mpwfx.Format.cbSize = 0;
            } else {
                return -1;    // we don't handle these (32 bit wavefiles, etc)
// #if 0
                // // Read in length of extra bytes.
                // word cbExtraBytes = 0L;
                // if( mhmmio.Read( (char*)&cbExtraBytes, sizeof(word) ) != sizeof(word) )
                // return -1;

                // mpwfx.Format.cbSize = cbExtraBytes;
                // // Now, read those extra bytes into the structure, if cbExtraAlloc != 0.
                // if( mhmmio.Read( (char*)(((byte*)&(mpwfx.Format.cbSize))+sizeof(word)), cbExtraBytes ) != cbExtraBytes ) {
                // memset( &mpwfx, 0, sizeof( waveformatextensible_t ) );
                // return -1;
                // }
// #endif
            }

            return 0;
        }

        private int OpenOGG(final String strFileName, waveformatex_s[] pwfx /*= NULL*/) {
//            memset(pwfx, 0, sizeof(waveformatex_t));
            final int error[] = {0};
            this.mhmmio = fileSystem.OpenFileRead(strFileName);
            if (null == this.mhmmio) {
                return -1;
            }

            Sys_EnterCriticalSection(CRITICAL_SECTION_ONE);

            final ByteBuffer buffer = ByteBuffer.allocate(this.mhmmio.Length());
            this.mhmmio.Read(buffer);
            try {
                final ByteBuffer d_buffer = (ByteBuffer) Nio.newByteBuffer(buffer.capacity()).put(buffer).rewind();
                final Vorbis info = Vorbis.getInfo(d_buffer, error);
                if ((error[0] != 0) || (info == null)) {
                    fileSystem.CloseFile(this.mhmmio);
                    this.mhmmio = null;
                    return -1;
                }
                this.mfileTime = this.mhmmio.Timestamp();

                this.mpwfx.Format.nSamplesPerSec = info.sampleRate;
                this.mpwfx.Format.nChannels = info.channels;
                this.mpwfx.Format.wBitsPerSample = Short.SIZE;
                this.mdwSize = info.mdwSize;    // pcm samples * num channels
                this.mbIsReadingFromMemory = false;

                if (s_realTimeDecoding.GetBool()) {
                    fileSystem.CloseFile(this.mhmmio);
                    this.mhmmio = null;

                    this.mpwfx.Format.wFormatTag = WAVE_FORMAT_TAG_OGG;
                    this.mhmmio = fileSystem.OpenFileRead(strFileName);
                    this.mMemSize = this.mhmmio.Length();

                } else {
                    this.ogg = "we only check if this is not null";

                    this.mpwfx.Format.wFormatTag = WAVE_FORMAT_TAG_PCM;
                    this.mMemSize = (this.mdwSize * Short.SIZE) / Byte.SIZE;
                }

                if (pwfx != null) {
                    pwfx[0] = new waveformatex_s(this.mpwfx.Format);
                }
            } finally {
                Sys_LeaveCriticalSection(CRITICAL_SECTION_ONE);
            }

            this.isOgg = true;

            return 0;
        }

        private int ReadOGG(byte[] pBuffer, int dwSizeToRead, int[] pdwSizeRead) {
            throw new TODO_Exception();
//            int total = dwSizeToRead;
//            String bufferPtr = (char[]) pBuffer;
//            OggVorbis_File ov = (OggVorbis_File) ogg;
//
//            do {
//                int ret = ov_read(ov, bufferPtr, total >= 4096 ? 4096 : total, Swap_IsBigEndian(), 2, 1, ov.stream);
//                if (ret == 0) {
//                    break;
//                }
//                if (ret < 0) {
//                    return -1;
//                }
//                bufferPtr += ret;
//                total -= ret;
//            } while (total > 0);
//
//            dwSizeToRead = (byte[]) bufferPtr - pBuffer;
//
//            if (pdwSizeRead != null) {
//                pdwSizeRead[0] = dwSizeToRead;
//            }
//
//            return dwSizeToRead;
        }

        private int CloseOGG() {
            throw new TODO_Exception();
//            OggVorbis_File ov = (OggVorbis_File) ogg;
//            if (ov != null) {
//                Sys_EnterCriticalSection(CRITICAL_SECTION_ONE);
//                ov_clear(ov);
////		delete ov;
//                Sys_LeaveCriticalSection(CRITICAL_SECTION_ONE);
//                fileSystem.CloseFile(mhmmio);
//                mhmmio = null;
//                ogg = null;
//                return 0;
//            }
//            return -1;
        }
    }
}
