package neo.Sound;

import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import neo.Sound.snd_cache.idSoundSample;

import static neo.Sound.snd_local.WAVE_FORMAT_TAG_OGG;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_PCM;

import neo.Sound.snd_local.idSampleDecoder;

import static neo.Sound.snd_system.soundSystemLocal;

import neo.TempDump.TODO_Exception;
import neo.framework.File_h.idFile;
import neo.framework.File_h.idFile_Memory;

import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.sys.sys_public.CRITICAL_SECTION_ONE;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;

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
    static VorbisFile ov_openFile(idFile f) throws JOrbisException {
        ByteBuffer fileBuffer = ByteBuffer.allocate(f.Length());
//        final int curPtr = f.Tell();
//        f.Seek(0, FS_SEEK_SET);
        f.Read(fileBuffer);
//        System.out.println(Arrays.toString(fileBuffer.array()));
//        f.Seek(curPtr, FS_SEEK_SET);
//        OggPacketReader oggPacketReader = new OggPacketReader(new ByteArrayInputStream(buffer.array()));
//        return new VorbisFile(oggPacketReader);
//        return new VorbisFile(new OggFile(new ByteArrayInputStream(fileBuffer.array())));
        return new VorbisFile(fileBuffer);
//        throw new TODO_Exception();
//        ov_callbacks callbacks;
//
//        memset(vf, 0, sizeof(OggVorbis_File));
//
//        callbacks.read_func = FS_ReadOGG;
//        callbacks.seek_func = FS_SeekOGG;
//        callbacks.close_func = FS_CloseOGG;
//        callbacks.tell_func = FS_TellOGG;
//        return ov_open_callbacks(f, vf, null, -1, callbacks);
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
        private idFile_Memory file;               // encoded file in memory
        //
        private VorbisFile    ogg;                // OggVorbis file
        //
        //

        idSampleDecoderLocal() {
            this.file = new idFile_Memory();
        }

        private static int DBG_Decode = 0;

        @Override
        public void Decode(idSoundSample sample, int sampleOffset44k, int sampleCount44k, FloatBuffer dest) {
            int readSamples44k;

            if (sample.objectInfo.wFormatTag != lastFormat || sample != lastSample) {
                ClearDecoder();
            }

            lastDecodeTime = soundSystemLocal.CurrentSoundTime;

            if (failed) {
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
                switch (lastFormat) {
                    case WAVE_FORMAT_TAG_PCM: {
                        break;
                    }
                    case WAVE_FORMAT_TAG_OGG: {
//                    ov_clear(ogg);
//                    memset(ogg, 0, sizeof(ogg));
                        ogg = null;
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
            return lastSample;
        }

        @Override
        public int GetLastDecodeTime() {
            return lastDecodeTime;
        }

        public void Clear() {
            failed = false;
            lastFormat = WAVE_FORMAT_TAG_PCM;
            lastSample = null;
            lastSampleOffset = 0;
            lastDecodeTime = 0;
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

            int shift = (int) (22050 / sample.objectInfo.nSamplesPerSec);
            int sampleOffset = sampleOffset44k >> shift;
            int sampleCount = sampleCount44k >> shift;

            // open OGG file if not yet opened
            if (lastSample == null) {
                // make sure there is enough space for another decoder
//                if (decoderMemoryAllocator.GetFreeBlockMemory() < MIN_OGGVORBIS_MEMORY) {
//                    return 0;
//                }
                if (sample.nonCacheData == null) {
                    assert (false);    // this should never happen
                    failed = true;
                    return 0;
                }
                file.SetData(sample.nonCacheData, sample.objectMemSize);
                try {
                    ogg = ov_openFile(file);
                } catch (JOrbisException ex) {
                    Logger.getLogger(snd_decoder.class.getName()).log(Level.SEVERE, null, ex);
                    failed = true;
                    return 0;
                }
                lastFormat = WAVE_FORMAT_TAG_OGG;
                lastSample = sample;
            }

            // seek to the right offset if necessary
            if (sampleOffset != lastSampleOffset) {
                if (ogg.pcm_seek((sampleOffset / sample.objectInfo.nChannels)) != 0) {
                    failed = true;
                    return 0;
                }
            }

            lastSampleOffset = sampleOffset;

            // decode OGG samples
            totalSamples = sampleCount;
            readSamples = 0;
            do {
                float[][][] samples = new float[1][][];
                int ret = ogg.read_float(samples, totalSamples / sample.objectInfo.nChannels);
                if (ret == 0) {
                    failed = true;
                    break;
                }
                if (ret < 0) {
                    failed = true;
                    return 0;
                }

                ret *= sample.objectInfo.nChannels;

                SIMDProcessor.UpSampleOGGTo44kHz(dest, (readSamples << shift), samples[0], ret, sample.objectInfo.nSamplesPerSec, sample.objectInfo.nChannels);
                readSamples += ret;
                totalSamples -= ret;
            } while (totalSamples > 0);

            lastSampleOffset += readSamples;

            return (readSamples << shift);
        }
    }

//    static final idBlockAlloc<idSampleDecoderLocal> sampleDecoderAllocator = new idBlockAlloc<>(64);
}
