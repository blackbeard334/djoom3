package neo.Sound;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import neo.Sound.snd_cache.idSoundSample;
import neo.Sound.snd_decoder.idSampleDecoderLocal;
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
        private static final int SIZE_B = SIZE / Byte.SIZE;

        //byte offsets
        private static final int/*word*/  wFormatTag      = 0;                                          // format type
        private static final int/*word*/  nChannels       = wFormatTag + (Short.SIZE / Byte.SIZE);      // number of channels (i.e. mono, stereo...)
        private static final int/*dword*/ nSamplesPerSec  = nChannels + (Integer.SIZE / Byte.SIZE);     // sample rate
        private static final int/*dword*/ nAvgBytesPerSec = nSamplesPerSec + (Integer.SIZE / Byte.SIZE);// for buffer estimation
        private static final int/*word*/  nBlockAlign     = nAvgBytesPerSec + (Short.SIZE / Byte.SIZE); // block size of data
        private static final int/*word*/  wBitsPerSample  = nBlockAlign + (Short.SIZE / Byte.SIZE);     // Number of bits per sample of mono data
        private static final int/*word*/  cbSize          = wBitsPerSample + (Short.SIZE / Byte.SIZE);  // The count in bytes of the size of extra information (after cbSize)
    };


    /* OLD general waveform format structure (information common to all formats) */
    static class waveformat_s {

        private static final int SIZE
                = Short.SIZE
                + Short.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Short.SIZE;
        private static final int SIZE_B = SIZE / Byte.SIZE;

        //offsets
        private static final int/*word*/  wFormatTag      = 0;                                           // format type
        private static final int/*word*/  nChannels       = Short.SIZE / Byte.SIZE;                      // number of channels (i.e. mono, stereo, etc.)
        private static final int/*dword*/ nSamplesPerSec  = nChannels + Short.SIZE / Byte.SIZE;          // sample rate
        private static final int/*dword*/ nAvgBytesPerSec = nSamplesPerSec + Integer.SIZE / Byte.SIZE;   // for buffer estimation
        private static final int/*word*/  nBlockAlign     = nAvgBytesPerSec + Integer.SIZE / Byte.SIZE;  // block size of data
    };


    /* flags for wFormatTag field of WAVEFORMAT */
// enum {
    static final int WAVE_FORMAT_TAG_PCM = 1;
    static final int WAVE_FORMAT_TAG_OGG = 2;
// };

    /* specific waveform format structure for PCM data */
    static class pcmwaveformat_s {

        private static final int SIZE
                = waveformat_s.SIZE
                + Short.SIZE;
        static final         int SIZE_B = SIZE / Byte.SIZE;

        private waveformat_s wf;
        private static final int/*word*/ wBitsPerSample = waveformat_s.SIZE_B;
        //
        final                ByteBuffer  buffer         = ByteBuffer.allocate(SIZE_B);
        //
        //

        public int getwBitsPerSample() {
            return buffer.getShort(wBitsPerSample) & 0xFFFF;
        }

        public void setwBitsPerSample(int wBitsPerSample) {
            buffer.putShort(wBitsPerSample, (short) wBitsPerSample);
        }
        //
        //
        //
        //

        public int getWf_wFormatTag() {
            return buffer.getShort(waveformat_s.wFormatTag) & 0xFFFF;
        }

        public void setWf_wFormatTag(int wFormatTag) {
            buffer.putShort(waveformat_s.wFormatTag, (short) wFormatTag);
        }

        public int getWf_nChannels() {
            return buffer.getShort(waveformat_s.nChannels) & 0xFFFF;
        }

        public void setWf_nChannels(int nChannels) {
            buffer.putShort(waveformat_s.nChannels, (short) nChannels);
        }

        public long getWf_nSamplesPerSec() {
            return buffer.getInt(waveformat_s.nSamplesPerSec) & 0xFFFF_FFFFL;
        }

        public void setWf_nSamplesPerSec(long nSamplesPerSec) {
            buffer.putInt(waveformat_s.nSamplesPerSec, (int) nSamplesPerSec);
        }

        public long getWf_nAvgBytesPerSec() {
            return buffer.getInt(waveformat_s.nAvgBytesPerSec) & 0xFFFF_FFFFL;
        }

        public void setWf_nAvgBytesPerSec(long nAvgBytesPerSec) {
            buffer.putInt(waveformat_s.nAvgBytesPerSec, (int) nAvgBytesPerSec);
        }

        public int getWf_nBlockAlign() {
            return buffer.getShort(waveformat_s.nBlockAlign) & 0xFFFF;
        }

        public void setWf_nBlockAlign(int nBlockAlign) {
            buffer.putShort(waveformat_s.nBlockAlign, (short) nBlockAlign);
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
        private static final int SIZE_B = SIZE / Byte.SIZE;

//        waveformatex_s Format;
//        // union {
        private static final int/*word*/ wValidBitsPerSample = waveformatex_s.SIZE_B;// bits of precision  
//        int/*word*/ wSamplesPerBlock;          // valid if wBitsPerSample==0
//        int/*word*/ wReserved;                 // If neither applies, set to zero
//        // } Samples;
        private static final int/*dword*/ dwChannelMask = wValidBitsPerSample + (Short.SIZE / Byte.SIZE);// which channels are */
//                                               // present in stream  */
        private static final int SubFormat = dwChannelMask + (Integer.SIZE / Byte.SIZE);
        //
        final ByteBuffer buffer = ByteBuffer.allocate(SIZE_B);
        //
        //

        public int getwValidBitsPerSample() {
            return buffer.getShort(this.wValidBitsPerSample) & 0xFFFF;
        }

        public void setwValidBitsPerSample(int wValidBitsPerSample) {
            buffer.putShort(this.wValidBitsPerSample, (short) wValidBitsPerSample);
        }

        public int getwSamplesPerBlock() {
            return getwValidBitsPerSample();
        }

        public void setwSamplesPerBlock(int wSamplesPerBlock) {
            this.setwValidBitsPerSample(wSamplesPerBlock);
        }

        public int getwReserved() {
            return getwValidBitsPerSample();
        }

        public void setwReserved(int wReserved) {
            this.setwValidBitsPerSample(wReserved);
        }
        //
        //

        public long getDwChannelMask() {
            return buffer.getInt(this.dwChannelMask) & 0xFFFF_FFFFL;
        }

        public void setDwChannelMask(long dwChannelMask) {
            buffer.putInt(this.dwChannelMask, (int) dwChannelMask);
        }

        public int getSubFormat() {
            return buffer.getInt(this.SubFormat);
        }

        public void setSubFormat(int SubFormat) {
            buffer.putInt(this.SubFormat, SubFormat);
        }
        //
        //
        //
        //

        public int getFormat_wFormatTag() {
            return buffer.getShort(waveformatex_s.wFormatTag) & 0xFFFF;
        }

        public void setFormat_wFormatTag(int wFormatTag) {
            buffer.putInt(waveformatex_s.wFormatTag, wFormatTag);
        }

        public int getFormat_nChannels() {
            return buffer.getShort(waveformatex_s.nChannels) & 0xFFFF;
        }

        public void setFormat_nChannels(int nChannels) {
            buffer.putInt(waveformatex_s.nChannels, nChannels);
        }

        public long getFormat_nSamplesPerSec() {
            return buffer.getInt(waveformatex_s.nSamplesPerSec) & 0xFFFF_FFFFL;
        }

        public void setFormat_nSamplesPerSec(long nSamplesPerSec) {
            buffer.putInt(waveformatex_s.nSamplesPerSec, (int) nSamplesPerSec);
        }

        public long getFormat_nAvgBytesPerSec() {
            return buffer.getInt(waveformatex_s.nAvgBytesPerSec) & 0xFFFF_FFFFL;
        }

        public void setFormat_nAvgBytesPerSec(long nAvgBytesPerSec) {
            buffer.putInt(waveformatex_s.nAvgBytesPerSec, (int) nAvgBytesPerSec);
        }

        public int getFormat_nBlockAlign() {
            return buffer.getShort(waveformatex_s.nBlockAlign) & 0xFFFF;
        }

        public void setFormat_nBlockAlign(int nBlockAlign) {
            buffer.putInt(waveformatex_s.nBlockAlign, nBlockAlign);
        }

        public int getFormat_wBitsPerSample() {
            return buffer.getShort(waveformatex_s.wBitsPerSample) & 0xFFFF;
        }

        public void setFormat_wBitsPerSample(int wBitsPerSample) {
            buffer.putInt(waveformatex_s.wBitsPerSample, wBitsPerSample);
        }

        public int getFormat_CbSize() {
            return buffer.getShort(waveformatex_s.cbSize) & 0xFFFF;
        }

        public void setFormat_CbSize(int cbSize) {
            buffer.putInt(waveformatex_s.cbSize, cbSize);
        }
    };

// typedef dword fourcc;

    /* RIFF chunk information data structure */
    static class mminfo_s {

        //offsets
        private static final int/*fourcc*/ ckid = 0;                            // chunk ID 
        private static final int/*dword*/ cksize = Integer.SIZE / Byte.SIZE;    // chunk size 
        private static final int/*fourcc*/ fccType = cksize * 2;                // form type or list type 
        private static final int/*dword*/ dwDataOffset = cksize * 3;            // offset of data portion of chunk 
        //
        final ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE * 4 / Byte.SIZE);
        //
        //

        public long getCkid() {
            return buffer.getInt(ckid) & 0xFFFF_FFFFL;
        }

        public void setCkid(long ckid) {
            buffer.putInt(this.ckid, (int) ckid);
        }

        public long getCksize() {
            return buffer.getInt(cksize) & 0xFFFF_FFFFL;
        }

        public void setCksize(long cksize) {
            buffer.putInt(this.cksize, (int) cksize);
        }

        public long getFccType() {
            return buffer.getInt(fccType) & 0xFFFF_FFFFL;
        }

        public void setFccType(long fccType) {
            buffer.putInt(this.fccType, (int) fccType);
        }

        public long getDwDataOffset() {
            return buffer.getInt(dwDataOffset) & 0xFFFF_FFFFL;
        }

        public void setDwDataOffset(long dwDataOffset) {
            buffer.putInt(this.dwDataOffset, (int) dwDataOffset);
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
