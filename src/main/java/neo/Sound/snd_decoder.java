package neo.Sound;

import static neo.Sound.snd_local.WAVE_FORMAT_TAG_OGG;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_PCM;
import static neo.Sound.snd_system.soundSystemLocal;
import static neo.open.Vorbis.getErrorMessage;
import static neo.sys.sys_public.CRITICAL_SECTION_ONE;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import neo.TempDump.TODO_Exception;
import neo.Sound.snd_cache.idSoundSample;
import neo.Sound.snd_local.idSampleDecoder;
import neo.framework.File_h.idFile_Memory;
import neo.open.Vorbis;

/**
 *
 */
public class snd_decoder {

    /*
     ===================================================================================

     Thread safe decoder memory allocator.

     Each OggVorbis decoder consumes about 150kB of memory.

     ===================================================================================
     */
//    idDynamicBlockAlloc<Byte> decoderMemoryAllocator = new idDynamicBlockAlloc(1 << 20, 128);
//
//    static final int MIN_OGGVORBIS_MEMORY = 768 * 1024;
//
//    public static Object _decoder_malloc(int/*size_t*/ size) {
//        Object ptr = decoderMemoryAllocator.Alloc(size);
//        assert (size == 0 || ptr != null);
//        return ptr;
//    }
//
//    public static Object _decoder_calloc(int/*size_t*/ num, int/*size_t*/ size) {
//        Object ptr = decoderMemoryAllocator.Alloc(num * size);
//        assert ((num * size) == 0 || ptr != null);
//        memset(ptr, 0, num * size);
//        return ptr;
//    }
//
//    public static Object _decoder_realloc(Object memblock, int/*size_t*/ size) {
//        Object ptr = decoderMemoryAllocator.Resize((byte[]) memblock, size);
//        assert (size == 0 || ptr != null);
//        return ptr;
//    }
//
//    public static void _decoder_free(Object memblock) {
//        decoderMemoryAllocator.Free((byte[]) memblock);
//    }
//
//
    /*
     ===================================================================================

     OggVorbis file loading/decoding.

     ===================================================================================
     */

    /*
     ====================
     FS_ReadOGG
     ====================
     */
    static int/*size_t*/ FS_ReadOGG(ByteBuffer dest, int/*size_t*/ size1, int/*size_t*/ size2, ByteBuffer fh) {
        throw new TODO_Exception();
//        idFile f = reinterpret_cast < idFile > (fh);
//        return f.Read(dest, size1 * size2);
    }

    /*
     ====================
     FS_SeekOGG
     ====================
     */
    static int FS_SeekOGG(Object fh, long/*ogg_int64_t*/ to, int type) {
        throw new TODO_Exception();
//        fsOrigin_t retype = FS_SEEK_SET;
//
//        if (type == SEEK_CUR) {
//            retype = FS_SEEK_CUR;
//        } else if (type == SEEK_END) {
//            retype = FS_SEEK_END;
//        } else if (type == SEEK_SET) {
//            retype = FS_SEEK_SET;
//        } else {
//            common.FatalError("fs_seekOGG: seek without type\n");
//        }
//        idFile f = reinterpret_cast < idFile > (fh);
//        return f.Seek(to, retype);
    }

    /*
     ====================
     FS_CloseOGG
     ====================
     */
    static int FS_CloseOGG(Object fh) {
        return 0;
    }

    /*
     ====================
     FS_TellOGG
     ====================
     */
    static long FS_TellOGG(Object fh) {
        throw new TODO_Exception();
//        idFile f = reinterpret_cast < idFile > (fh);
//        return f.Tell();
    }

    /*
     ====================
     ov_openFile
     ====================
     */
    static long ov_openFile(final idFile_Memory f, int[] error) {
        return Vorbis.openMemory(f.GetDataPtr(), error);
    }

    /*
     ===================================================================================

     idSampleDecoderLocal

     ===================================================================================
     */
    public static class idSampleDecoderLocal extends idSampleDecoder {

        private boolean       failed;             // set if decoding failed
        private int           lastFormat;         // last format being decoded
        private idSoundSample lastSample;         // last sample being decoded
        private int           lastSampleOffset;   // last offset into the decoded sample
        private int           lastDecodeTime;     // last time decoding sound
        private final idFile_Memory file;               // encoded file in memory
        //
        private Long          ogg;                // OggVorbis file
        //
        //

        idSampleDecoderLocal() {
            this.file = new idFile_Memory();
        }

        private static int DBG_Decode = 0;

        @Override
        public void Decode(idSoundSample sample, int sampleOffset44k, int sampleCount44k, FloatBuffer dest) {
            int readSamples44k;

            if ((sample.objectInfo.wFormatTag != this.lastFormat) || (sample != this.lastSample)) {
                ClearDecoder();
            }

            this.lastDecodeTime = soundSystemLocal.CurrentSoundTime;

            if (this.failed) {
//                memset(dest, 0, sampleCount44k * sizeof(dest[0]));
                dest.clear();
                return;
            }

            // samples can be decoded both from the sound thread and the main thread for shakes
            Sys_EnterCriticalSection(CRITICAL_SECTION_ONE);

            try {
                switch (sample.objectInfo.wFormatTag) {
                    case WAVE_FORMAT_TAG_PCM: {
                        readSamples44k = DecodePCM(sample, sampleOffset44k, sampleCount44k, dest.array());//TODO:fix with offset
                        break;
                    }
                    case WAVE_FORMAT_TAG_OGG: {
                        DBG_Decode++;
                        readSamples44k = DecodeOGG(sample, sampleOffset44k, sampleCount44k, dest);
                        break;
                    }
                    default: {
                        readSamples44k = 0;
                        break;
                    }
                }
            } finally {
                Sys_LeaveCriticalSection(CRITICAL_SECTION_ONE);
            }

            if (readSamples44k < sampleCount44k) {
//                memset(dest + readSamples44k, 0, (sampleCount44k - readSamples44k) * sizeof(dest[0]));
                Arrays.fill(dest.array(), readSamples44k, (sampleCount44k - readSamples44k), 0);
            }
        }

        @Override
        public void ClearDecoder() {
            Sys_EnterCriticalSection(CRITICAL_SECTION_ONE);

            try {
                switch (this.lastFormat) {
                    case WAVE_FORMAT_TAG_PCM: {
                        break;
                    }
                    case WAVE_FORMAT_TAG_OGG: {
//                    ov_clear(ogg);
//                    memset(ogg, 0, sizeof(ogg));
                        this.ogg = null;
                        break;
                    }
                }

                Clear();
            } finally {
                Sys_LeaveCriticalSection(CRITICAL_SECTION_ONE);
            }
        }

        @Override
        public idSoundSample GetSample() {
            return this.lastSample;
        }

        @Override
        public int GetLastDecodeTime() {
            return this.lastDecodeTime;
        }

        public void Clear() {
            this.failed = false;
            this.lastFormat = WAVE_FORMAT_TAG_PCM;
            this.lastSample = null;
            this.lastSampleOffset = 0;
            this.lastDecodeTime = 0;
        }

        public int DecodePCM(idSoundSample sample, int sampleOffset44k, int sampleCount44k, float[] dest) {
            throw new TODO_Exception();
//            ByteBuffer first;
//            int[] pos = {0}, size = {0};
//            int readSamples;
//
//            lastFormat = WAVE_FORMAT_TAG_PCM;
//            lastSample = sample;
//
//            int shift = (int) (22050 / sample.objectInfo.nSamplesPerSec);
//            int sampleOffset = sampleOffset44k >> shift;
//            int sampleCount = sampleCount44k >> shift;
//
//            if (sample.nonCacheData == null) {
//                assert (false);	// this should never happen ( note: I've seen that happen with the main thread down in idGameLocal::MapClear clearing entities - TTimo )
//                failed = true;
//                return 0;
//            }
//
//            if (!sample.FetchFromCache(sampleOffset /* sizeof( short )*/, first, pos, size, false)) {
//                failed = true;
//                return 0;
//            }
//
//            if (size[0] - pos[0] < sampleCount /*sizeof(short)*/) {
//                readSamples = (size[0] - pos[0]) /* sizeof(short)*/;
//            } else {
//                readSamples = sampleCount;
//            }
//
//            // duplicate samples for 44kHz output
//            first.position(pos[0]);
//            SIMDProcessor.UpSamplePCMTo44kHz(dest, first, readSamples, sample.objectInfo.nSamplesPerSec, sample.objectInfo.nChannels);
//
//            return (readSamples << shift);
        }

        public int DecodeOGG(idSoundSample sample, int sampleOffset44k, int sampleCount44k, FloatBuffer dest) {
            int readSamples, totalSamples;

            final int shift = 22050 / sample.objectInfo.nSamplesPerSec;
            final int sampleOffset = sampleOffset44k >> shift;
            final int sampleCount = sampleCount44k >> shift;

            // open OGG file if not yet opened
            if (this.lastSample == null) {
                // make sure there is enough space for another decoder
//                if (decoderMemoryAllocator.GetFreeBlockMemory() < MIN_OGGVORBIS_MEMORY) {
//                    return 0;
//                }
                if (sample.nonCacheData == null) {
                    assert (false);    // this should never happen
                    this.failed = true;
                    return 0;
                }
                this.file.SetData(sample.nonCacheData, sample.objectMemSize);
                final int[] error = {0};
                this.ogg = ov_openFile(this.file, error);
                if (error[0] != 0) {
                    Logger.getLogger(snd_decoder.class.getName()).log(Level.SEVERE, getErrorMessage(error[0]));
                    this.failed = true;
                    return 0;
                }
                this.lastFormat = WAVE_FORMAT_TAG_OGG;
                this.lastSample = sample;
            }

            // seek to the right offset if necessary
            if (sampleOffset != this.lastSampleOffset) {
                if (!Vorbis.seek(this.ogg, (sampleOffset / sample.objectInfo.nChannels))) {
                    this.failed = true;
                    return 0;
                }
            }

            this.lastSampleOffset = sampleOffset;

            // decode OGG samples
            totalSamples = sampleCount;
            readSamples = 0;
            do {
                int ret = Vorbis.getSample(sample.objectInfo.nChannels, totalSamples, this.ogg);
                if (ret == 0) {
                    this.failed = true;
                    break;
                }
                if (ret < 0) {
                    this.failed = true;
                    return 0;
                }

                ret *= sample.objectInfo.nChannels;

                readSamples += ret;
                totalSamples -= ret;
            } while (totalSamples > 0);

            this.lastSampleOffset += readSamples;

            return (readSamples << shift);
        }
    }

//    static final idBlockAlloc<idSampleDecoderLocal> sampleDecoderAllocator = new idBlockAlloc<>(64);

}
