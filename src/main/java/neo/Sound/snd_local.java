package neo.Sound;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import neo.Sound.snd_cache.idSoundSample;
import neo.Sound.snd_decoder.idSampleDecoderLocal;
import neo.TempDump.SERiAL;
import neo.TempDump.TODO_Exception;
import static neo.framework.UsercmdGen.USERCMD_MSEC;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;
import neo.sys.win_snd.idAudioHardwareWIN32;

/**
 *
 */
public class snd_local {

//    static final idDynamicBlockAlloc<Byte> decoderMemoryAllocator = new idDynamicBlockAlloc<>(1 << 20, 128);
    //
    static final int MIN_OGGVORBIS_MEMORY = 768 * 1024;
    //  
//    static final idBlockAlloc<idSampleDecoderLocal> sampleDecoderAllocator = new idBlockAlloc<>(64);

    // demo sound commands
    public enum soundDemoCommand_t {

        SCMD_STATE, // followed by a load game state
        SCMD_PLACE_LISTENER,
        SCMD_ALLOC_EMITTER,
        SCMD_FREE,
        SCMD_UPDATE,
        SCMD_START,
        SCMD_MODIFY,
        SCMD_STOP,
        SCMD_FADE
    };

    static final        int   SOUND_MAX_CHANNELS       = 8;
    static final        int   SOUND_DECODER_FREE_DELAY = 1000 * MIXBUFFER_SAMPLES / USERCMD_MSEC;        // four seconds
    //
    public static final int   PRIMARYFREQ              = 44100;              // samples per second
    static final        float SND_EPSILON              = 1.0f / 32768.0f;    // if volume is below this, it will always multiply to zero
    //
    static final        int   ROOM_SLICES_IN_BUFFER    = 10;


    /*
     ===================================================================================

     General extended waveform format structure.
     Use this for all NON PCM formats.

     ===================================================================================
     */
    static class waveformatex_s {

        private static final int SIZE
                = Short.SIZE
                + Short.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Short.SIZE
                + Short.SIZE
                + Short.SIZE;
        private static final int BYTES = SIZE / Byte.SIZE;

        //byte offsets
        public int  wFormatTag;      // format type
        public int  nChannels;       // number of channels (i.e. mono, stereo...)
        public int  nSamplesPerSec;  // sample rate
        public int  nAvgBytesPerSec; // for buffer estimation
        public int  nBlockAlign;     // block size of data
        public int  wBitsPerSample;  // Number of bits per sample of mono data
        public int  cbSize;          // The count in bytes of the size of extra information (after cbSize)

        waveformatex_s() {
        }

        waveformatex_s(waveformatex_s mpwfx) {
            this.wFormatTag = mpwfx.wFormatTag;
            this.nChannels = mpwfx.nChannels;
            this.nSamplesPerSec = mpwfx.nSamplesPerSec;
            this.nAvgBytesPerSec = mpwfx.nAvgBytesPerSec;
            this.nBlockAlign = mpwfx.nBlockAlign;
            this.wBitsPerSample = mpwfx.wBitsPerSample;
            this.cbSize = mpwfx.cbSize;
        }

    };


    /* OLD general waveform format structure (information common to all formats) */
    static class waveformat_s {

        private static final int SIZE
                = Short.SIZE
                + Short.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Short.SIZE;
        private static final int BYTES = SIZE / Byte.SIZE;

        //offsets
        public int/*word*/ wFormatTag;      // format type
        public int/*word*/ nChannels;       // number of channels (i.e. mono, stereo, etc.)
        public int/*dword*/nSamplesPerSec;  // sample rate
        public int/*dword*/nAvgBytesPerSec; // for buffer estimation
        public int/*word*/ nBlockAlign;     // block size of data
    };


    /* flags for wFormatTag field of WAVEFORMAT */
// enum {
    static final int WAVE_FORMAT_TAG_PCM = 1;
    static final int WAVE_FORMAT_TAG_OGG = 2;
// };

    /* specific waveform format structure for PCM data */
    static class pcmwaveformat_s implements SERiAL{

        private static final int SIZE
                = waveformat_s.SIZE
                + Short.SIZE;
        static final         int BYTES = SIZE / Byte.SIZE;

        public waveformat_s wf;
        public int/*word*/ wBitsPerSample;

        @Override
        public ByteBuffer AllocBuffer() {
            return ByteBuffer.allocate(BYTES);
        }

        @Override
        public void Read(ByteBuffer buffer) {
            this.wf = new waveformat_s();
            this.wf.wFormatTag = Short.toUnsignedInt(buffer.getShort());
            this.wf.nChannels = Short.toUnsignedInt(buffer.getShort());
            this.wf.nSamplesPerSec = buffer.getInt();
            this.wf.nAvgBytesPerSec = buffer.getInt();
            this.wf.nBlockAlign = Short.toUnsignedInt(buffer.getShort());

            this.wBitsPerSample = Short.toUnsignedInt(buffer.getShort());
        }

        @Override
        public ByteBuffer Write() {
            ByteBuffer data = ByteBuffer.allocate(pcmwaveformat_s.BYTES);
            data.order(ByteOrder.LITTLE_ENDIAN);//very importante.

            data.putShort((short) wf.wFormatTag);
            data.putShort((short) wf.nChannels);
            data.putInt(wf.nSamplesPerSec);
            data.putInt(wf.nAvgBytesPerSec);
            data.putShort((short) wf.nBlockAlign);

            data.putShort((short) wBitsPerSample);

            return data;
        }
    };

// #ifndef mmioFOURCC
// #define mmioFOURCC( ch0, ch1, ch2, ch3 )				\
    // ( (dword)(byte)(ch0) | ( (dword)(byte)(ch1) << 8 ) |	\
    // ( (dword)(byte)(ch2) << 16 ) | ( (dword)(byte)(ch3) << 24 ) )
// #endif
// #define fourcc_riff     mmioFOURCC('R', 'I', 'F', 'F')
    static class waveformatextensible_s {

        private static final int SIZE
                = waveformatex_s.SIZE
                + Short.SIZE //union
                + Integer.SIZE
                + Integer.SIZE;
        private static final int BYTES = SIZE / Byte.SIZE;

        public waveformatex_s Format;
//        union {
//            word wValidBitsPerSample;       /* bits of precision  */
//            word wSamplesPerBlock;          /* valid if wBitsPerSample==0*/
//            word wReserved;                 /* If neither applies, set to zero*/
//            } Samples;
        public int/*word*/ Samples;
        public int/*dword*/ dwChannelMask;   // which channels are */
//                                            // present in stream  */
        public int SubFormat;

        waveformatextensible_s(){
            this.Format = new waveformatex_s();
        }

        waveformatextensible_s(pcmwaveformat_s pcmWaveFormat) {
            this();
            this.Format.wFormatTag = pcmWaveFormat.wf.wFormatTag;
            this.Format.nChannels = pcmWaveFormat.wf.nChannels;
            this.Format.nSamplesPerSec = pcmWaveFormat.wf.nSamplesPerSec;
            this.Format.nAvgBytesPerSec = pcmWaveFormat.wf.nAvgBytesPerSec;
            this.Format.nBlockAlign = pcmWaveFormat.wf.nBlockAlign;
            this.Format.wBitsPerSample = pcmWaveFormat.wBitsPerSample;
        }
    };

// typedef dword fourcc;

    /* RIFF chunk information data structure */
    static class mminfo_s implements SERiAL{
        private static final int SIZE
                = Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE;
        private static final int BYTES = SIZE / Byte.SIZE;

        long/*fourcc*/ ckid;         // chunk ID 
        int/*dword*/ cksize;       // chunk size 
        long/*fourcc*/ fccType;      // form type or list type 
        int/*dword*/ dwDataOffset; // offset of data portion of chunk 

        @Override
        public ByteBuffer AllocBuffer() {
            return ByteBuffer.allocate(BYTES);
        }

        @Override
        public void Read(ByteBuffer buffer) {
            this.ckid = Integer.toUnsignedLong(buffer.getInt());
            this.cksize = buffer.getInt();

            if (buffer.hasRemaining()) {
                this.fccType = Integer.toUnsignedLong(buffer.getInt());
            }

            if (buffer.hasRemaining()) {
                this.dwDataOffset = buffer.getInt();
            }
        }

        @Override
        public ByteBuffer Write() {
            ByteBuffer data = ByteBuffer.allocate(mminfo_s.BYTES);
            data.order(ByteOrder.LITTLE_ENDIAN);//very importante.

            data.putInt((int) ckid);
            data.putInt(cksize);
            data.putInt((int) fccType);
            data.putInt(dwDataOffset);

            return data;
        }
    };

    /*
     ===================================================================================

     Sound sample decoder.

     ===================================================================================
     */
    public static abstract class idSampleDecoder {

        public static void Init() {
//            decoderMemoryAllocator.Init();
//            decoderMemoryAllocator.SetLockMemory(true);
//            decoderMemoryAllocator.SetFixedBlocks(idSoundSystemLocal.s_realTimeDecoding.GetBool() ? 10 : 1);
        }

        public static void Shutdown() {
//            decoderMemoryAllocator.Shutdown();
//            sampleDecoderAllocator.Shutdown();
        }

        public static idSampleDecoder Alloc() {
            idSampleDecoderLocal decoder = new idSampleDecoderLocal();//sampleDecoderAllocator.Alloc();
            decoder.Clear();
            return decoder;
        }

        public static void Free(idSampleDecoder decoder) {
            idSampleDecoderLocal localDecoder = (idSampleDecoderLocal) decoder;
            localDecoder.ClearDecoder();
//            sampleDecoderAllocator.Free(localDecoder);
        }

        @Deprecated
        public static int GetNumUsedBlocks() {
            throw new TODO_Exception();
//            return decoderMemoryAllocator.GetNumUsedBlocks();
        }

        @Deprecated
        public static int GetUsedBlockMemory() {
            throw new TODO_Exception();
//            return decoderMemoryAllocator.GetUsedBlockMemory();
        }

        // virtual					~idSampleDecoder() {}
        public abstract void Decode(idSoundSample sample, int sampleOffset44k, int sampleCount44k, FloatBuffer dest);

        public abstract void ClearDecoder();

        public abstract idSoundSample GetSample();

        public abstract int GetLastDecodeTime();
    };

    /*
     ===================================================================================

     idAudioHardware

     ===================================================================================
     */
    public static abstract class idAudioHardware {

        public static idAudioHardware Alloc() {
            return new idAudioHardwareWIN32();
        }

//    virtual					~idAudioHardware();
        public abstract boolean Initialize();

        public abstract boolean Lock(Object pDSLockedBuffer, long dwDSLockedBufferSize);

        public abstract boolean Unlock(Object pDSLockedBuffer, long/*dword*/ dwDSLockedBufferSize);

        public abstract boolean GetCurrentPosition(long pdwCurrentWriteCursor);

        // try to write as many sound samples to the device as possible without blocking and prepare for a possible new mixing call
        // returns wether there is *some* space for writing available
        public abstract boolean Flush();

        public abstract void Write(boolean flushing);

        public abstract int GetNumberOfSpeakers();

        public abstract int GetMixBufferSize();

        public abstract short[] GetMixBuffer();
    };
}
