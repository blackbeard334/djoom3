package neo.Sound;

import com.jcraft.jorbis.Info;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_OGG;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_PCM;
import neo.Sound.snd_local.mminfo_s;
import neo.Sound.snd_local.pcmwaveformat_s;
import neo.Sound.snd_local.waveformatex_s;
import neo.Sound.snd_local.waveformatextensible_s;
import static neo.Sound.snd_system.idSoundSystemLocal.s_realTimeDecoding;
import neo.TempDump.TODO_Exception;
import static neo.TempDump.stobb;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.File_h.idFile;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.LittleRevBytes;
import static neo.idlib.Lib.LittleShort;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.sys.sys_public.CRITICAL_SECTION_ONE;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;

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
        private mminfo_s               mck;         // Multimedia RIFF chunk
        private mminfo_s mckRiff = new mminfo_s();  // used when opening a WAVE file
        private long/*dword*/     mdwSize;          // size in samples
        private long/*dword*/     mMemSize;         // size of the wave data in memory
        private long/*dword*/     mseekBase;
        private long/*ID_TIME_T*/ mfileTime;
        //
        private boolean           mbIsReadingFromMemory;
        private ByteBuffer        mpbData;
        private ByteBuffer        mpbDataCur;
        private long/*dword*/     mulDataSize;
        //
        private Object            ogg;              // only !NULL when !s_realTimeDecoding
        private boolean           isOgg;
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
            mpwfx = new waveformatextensible_s();
            mhmmio = null;
            mdwSize = 0;
            mseekBase = 0;
            mbIsReadingFromMemory = false;
            mpbData = null;
            ogg = null;
            isOgg = false;
        }
        // ~idWaveFile();

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::Open()
        // Desc: Opens a wave file for reading
        //-----------------------------------------------------------------------------
        public int Open(final String strFileName, waveformatex_s[] pwfx /*= NULL*/) {

            mbIsReadingFromMemory = false;

            mpbData = null;
            mpbDataCur = mpbData;

            if (strFileName == null) {
                return -1;
            }

            idStr name = new idStr(strFileName);

            // note: used to only check for .wav when making a build
            name.SetFileExtension(".ogg");
            if (fileSystem.ReadFile(name.toString(), null, null) != -1) {
                return OpenOGG(name.toString(), pwfx);
            }

//	memset( &mpwfx, 0, sizeof( waveformatextensible_t ) );
            mpwfx = new waveformatextensible_s();

            mhmmio = fileSystem.OpenFileRead(strFileName);
            if (null == mhmmio) {
                mdwSize = 0;
                return -1;
            }
            if (mhmmio.Length() <= 0) {
                mhmmio = null;
                return -1;
            }
            if (ReadMMIO() != 0) {
                // ReadMMIO will fail if its an not a wave file
                Close();
                return -1;
            }

            mfileTime = mhmmio.Timestamp();

            if (ResetFile() != 0) {
                Close();
                return -1;
            }

            // After the reset, the size of the wav file is mck.cksize so store it now
            mdwSize = mck.cksize / 2;//sizeof(short);
            mMemSize = mck.cksize;

            if (mck.cksize != 0xffffffff) {
                if (pwfx != null) {
//                    pwfx = mpwfx;//memcpy(pwfx, (waveformatex_t *) & mpwfx, sizeof(waveformatex_t));
                    throw new TODO_Exception();
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
            mpwfx = pwfx;
            mulDataSize = ulDataSize;
            mpbData = stobb(pbData);
            mpbDataCur = mpbData.duplicate();
            mdwSize = ulDataSize / 2;//sizeof(short);
            mMemSize = ulDataSize;
            mbIsReadingFromMemory = true;

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

            if (ogg != null) {

                return ReadOGG(pBuffer.array(), dwSizeToRead, pdwSizeRead);

            } else if (mbIsReadingFromMemory) {

                if (mpbDataCur == null) {
                    return -1;
                }
                final int pos = dwSizeToRead + mpbDataCur.position();//add current offset
                if (mpbDataCur.get(dwSizeToRead) > mpbData.get((int) mulDataSize)) {
                    dwSizeToRead = (int) (mulDataSize - mpbDataCur.position());
                }
                SIMDProcessor.Memcpy(pBuffer, mpbDataCur, dwSizeToRead);
                mpbDataCur.position(pos);

                if (pdwSizeRead != null) {
                    pdwSizeRead[0] = dwSizeToRead;
                }

                return dwSizeToRead;

            } else {

                if (mhmmio == null) {
                    return -1;
                }
                if (pBuffer == null) {
                    return -1;
                }

                dwSizeToRead = mhmmio.Read(pBuffer, dwSizeToRead);
                // this is hit by ogg code, which does it's own byte swapping internally
                if (!isOgg) {
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
            if (ogg != null) {
                return CloseOGG();
            }
            if (mhmmio != null) {
                fileSystem.CloseFile(mhmmio);
                mhmmio = null;
            }
            return 0;
        }

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::ResetFile()
        // Desc: Resets the internal mck pointer so reading starts from the 
        //       beginning of the file again 
        //-----------------------------------------------------------------------------
        public int ResetFile() {
            throw new TODO_Exception();
//            if (mbIsReadingFromMemory) {
//                mpbDataCur = mpbData;
//            } else {
//                if (mhmmio == null) {
//                    return -1;
//                }
//
//                // Seek to the data
//                if (-1 == mhmmio.Seek(mckRiff.dwDataOffset + sizeof(fourcc), FS_SEEK_SET)) {
//                    return -1;
//                }
//
//                // Search the input file for for the 'fmt ' chunk.
//                mck.ckid = 0;
//                do {
//                    byte ioin;
//                    if (!mhmmio.Read(ioin, 1)) {
//                        return -1;
//                    }
//                    mck.ckid = (mck.ckid >> 8) | (ioin << 24);
//                } while (mck.ckid != mmioFOURCC('d', 'a', 't', 'a'));
//
//                mhmmio.Read(mck.cksize, 4);
//                assert (!isOgg);
//                mck.cksize = LittleLong(mck.cksize);
//                mseekBase = mhmmio.Tell();
//            }
//
//            return 0;
        }

        public int GetOutputSize() {
            return (int) mdwSize;
        }

        public int GetMemorySize() {
            return (int) mMemSize;
        }

        //-----------------------------------------------------------------------------
        // Name: idWaveFile::ReadMMIO()
        // Desc: Support function for reading from a multimedia I/O stream.
        //       mhmmio must be valid before calling.  This function uses it to
        //       update mckRiff, and mpwfx. 
        //-----------------------------------------------------------------------------
        private int ReadMMIO() {
            mminfo_s ckIn = new mminfo_s(); // chunk info. for general use.
            pcmwaveformat_s pcmWaveFormat = new pcmwaveformat_s();  // Temp PCM structure to load in.       

            mpwfx = new waveformatextensible_s();//memset( &mpwfx, 0, sizeof( waveformatextensible_t ) );

            mhmmio.Read(mckRiff, 12);
            assert (!isOgg);
            mckRiff.ckid = LittleLong(mckRiff.ckid);
            mckRiff.cksize = LittleLong(mckRiff.cksize);
            mckRiff.fccType = LittleLong(mckRiff.fccType);
            mckRiff.dwDataOffset = 12;

            // Check to make sure this is a valid wave file
            if ((mckRiff.ckid != fourcc_riff) || (mckRiff.fccType != mmioFOURCC('W', 'A', 'V', 'E'))) {
                return -1;
            }

            // Search the input file for for the 'fmt ' chunk.
            ckIn.dwDataOffset = 12;
            do {
                if (8 != mhmmio.Read(ckIn, 8)) {
                    return -1;
                }
                assert (!isOgg);
                ckIn.ckid = LittleLong(ckIn.ckid);
                ckIn.cksize = LittleLong(ckIn.cksize);
                ckIn.dwDataOffset += ckIn.cksize - 8;
            } while (ckIn.ckid != mmioFOURCC('f', 'm', 't', ' '));

            // Expect the 'fmt' chunk to be at least as large as <PCMWAVEFORMAT>;
            // if there are extra parameters at the end, we'll ignore them
            if (ckIn.cksize < pcmwaveformat_s.SIZE_B) {
                return -1;
            }

            // Read the 'fmt ' chunk into <pcmWaveFormat>.
            if (mhmmio.Read(pcmWaveFormat) != pcmwaveformat_s.SIZE_B) {
                return -1;
            }
            assert (!isOgg);
            pcmWaveFormat.wf.wFormatTag = LittleShort((short) pcmWaveFormat.wf.wFormatTag);
            pcmWaveFormat.wf.nChannels = LittleShort((short) pcmWaveFormat.wf.nChannels);
            pcmWaveFormat.wf.nSamplesPerSec = LittleLong(pcmWaveFormat.wf.nSamplesPerSec);
            pcmWaveFormat.wf.nAvgBytesPerSec = LittleLong(pcmWaveFormat.wf.nAvgBytesPerSec);
            pcmWaveFormat.wf.nBlockAlign = LittleShort((short) pcmWaveFormat.wf.nBlockAlign);
            pcmWaveFormat.wBitsPerSample = LittleShort((short) pcmWaveFormat.wBitsPerSample);

            // Copy the bytes from the pcm structure to the waveformatex_t structure
            mpwfx = new waveformatextensible_s(pcmWaveFormat);

            // Allocate the waveformatex_t, but if its not pcm format, read the next
            // word, and thats how many extra bytes to allocate.
            if (pcmWaveFormat.wf.wFormatTag == WAVE_FORMAT_TAG_PCM) {
                mpwfx.Format.cbSize = 0;
            } else {
                return -1;	// we don't handle these (32 bit wavefiles, etc)
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
            mhmmio = fileSystem.OpenFileRead(strFileName);
            if (null == mhmmio) {
                return -1;
            }

            Sys_EnterCriticalSection(CRITICAL_SECTION_ONE);

            ByteBuffer buffer = ByteBuffer.allocate(mhmmio.Length());
            mhmmio.Read(buffer);
            try (VorbisFile ov = new VorbisFile(buffer)) {
                mfileTime = mhmmio.Timestamp();

                Info vi = ov.getInfo()[0];

                mpwfx.Format.nSamplesPerSec = vi.rate;
                mpwfx.Format.nChannels = vi.channels;
                mpwfx.Format.wBitsPerSample = Short.SIZE;
                mdwSize = ov.pcm_total(-1) * vi.channels;	// pcm samples * num channels
                mbIsReadingFromMemory = false;

                if (s_realTimeDecoding.GetBool()) {
                    fileSystem.CloseFile(mhmmio);
                    mhmmio = null;

                    mpwfx.Format.wFormatTag = WAVE_FORMAT_TAG_OGG;
                    mhmmio = fileSystem.OpenFileRead(strFileName);
                    mMemSize = mhmmio.Length();

                } else {
                    ogg = ov;

                    mpwfx.Format.wFormatTag = WAVE_FORMAT_TAG_PCM;
                    mMemSize = mdwSize * Short.SIZE / Byte.SIZE;
                }

                if (pwfx != null) {
                    pwfx[0] = new waveformatex_s(mpwfx.Format);
                }
            } catch (JOrbisException | IOException ex) {
                fileSystem.CloseFile(mhmmio);
                mhmmio = null;
                return -1;
            } finally {
                Sys_LeaveCriticalSection(CRITICAL_SECTION_ONE);
            }

            isOgg = true;

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
    };
}
