package neo.Sound;

import static java.lang.Math.sin;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_OGG;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_PCM;
import static neo.TempDump.NOT;
import static neo.framework.BuildDefines.MACOS_X;
import static neo.framework.Common.com_purgeAll;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.FILE_NOT_FOUND_TIMESTAMP;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.Min;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;
import static neo.open.al.QAL.alBufferData;
import static neo.open.al.QAL.alDeleteBuffers;
import static neo.open.al.QAL.alGenBuffers;
import static neo.open.al.QAL.alGetError;
import static neo.open.al.QAL.alIsBuffer;
import static neo.open.al.QAL.alIsExtensionPresent;
import static neo.open.al.QALConstantsIfc.AL_FORMAT_MONO16;
import static neo.open.al.QALConstantsIfc.AL_FORMAT_STEREO16;
import static neo.open.al.QALConstantsIfc.AL_NO_ERROR;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import neo.Sound.snd_local.idSampleDecoder;
import neo.Sound.snd_local.waveformatex_s;
import neo.Sound.snd_system.idSoundSystemLocal;
import neo.Sound.snd_wavefile.idWaveFile;
import neo.framework.Common.MemInfo_t;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.open.Nio;

/**
 *
 */
public class snd_cache {
    
//    static final boolean USE_SOUND_CACHE_ALLOCATOR = true;
//    static final idDynamicBlockAlloc<Byte> soundCacheAllocator;
//
//    static {
//        if (USE_SOUND_CACHE_ALLOCATOR) {
//            soundCacheAllocator = new idDynamicBlockAlloc<>(1 << 20, 1 << 10);
////        } else {
////            soundCacheAllocator = new idDynamicAlloc<>(1 << 20, 1 << 10);
//        }
//    }

    /*
     ===================================================================================

     This class holds the actual wavefile bitmap, size, and info.

     ===================================================================================
     */
    static final int SCACHE_SIZE = MIXBUFFER_SAMPLES * 20;    // 1/2 of a second (aroundabout)

    public static class idSoundSample {

        public idStr             name;                 // name of the sample file
        public long/*ID_TIME_T*/ timestamp;            // the most recent of all images used in creation, for reloadImages command
        //
        public waveformatex_s    objectInfo;           // what are we caching
        public int               objectSize;           // size of waveform in samples, excludes the header
        public int               objectMemSize;        // object size in memory
        public ByteBuffer        nonCacheData;         // if it's not cached
        public ByteBuffer        amplitudeData;        // precomputed min,max amplitude pairs
        public int/*ALuint*/     openalBuffer;         // openal buffer
        public boolean           hardwareBuffer;
        public boolean           defaultSound;
        public boolean onDemand;
        public boolean purged;
        public boolean levelLoadReferenced;            // so we can tell which samples aren't needed any more
//
//

        public idSoundSample() {
//	memset( &objectInfo, 0, sizeof(waveformatex_t) );
            this.objectInfo = new waveformatex_s();
            this.objectSize = 0;
            this.objectMemSize = 0;
            this.nonCacheData = null;
            this.amplitudeData = null;
            this.openalBuffer = 0;
            this.hardwareBuffer = false;
            this.defaultSound = false;
            this.onDemand = false;
            this.purged = false;
            this.levelLoadReferenced = false;
        }
        // ~idSoundSample();

        public int LengthIn44kHzSamples() {
            // objectSize is samples
            if (this.objectInfo.nSamplesPerSec == 11025) {
                return this.objectSize << 2;
            } else if (this.objectInfo.nSamplesPerSec == 22050) {
                return this.objectSize << 1;
            } else {
                return this.objectSize << 0;
            }
        }

        public long/*ID_TIME_T*/ GetNewTimeStamp() {
            final long[] timestamp = {0};

            fileSystem.ReadFile(this.name.getData(), null, timestamp);
            if (timestamp[0] == FILE_NOT_FOUND_TIMESTAMP) {
                final idStr oggName = new idStr(this.name);
                oggName.SetFileExtension(".ogg");
                fileSystem.ReadFile(oggName.getData(), null, timestamp);
            }
            return timestamp[0];
        }

        // turns it into a beep	
        public void MakeDefault() {
            int i;
            float v;
            short sample;

//	memset( &objectInfo, 0, sizeof( objectInfo ) );
            this.objectInfo = new waveformatex_s();

            this.objectInfo.nChannels = 1;
            this.objectInfo.wBitsPerSample = 16;
            this.objectInfo.nSamplesPerSec = 44100;

            this.objectSize = MIXBUFFER_SAMPLES * 2;
            this.objectMemSize = this.objectSize * 2;//* sizeof(short);

            this.nonCacheData = Nio.newByteBuffer(this.objectMemSize);//soundCacheAllocator.Alloc(objectMemSize);

            final ShortBuffer ncd = this.nonCacheData.asShortBuffer();

            for (i = 0; i < MIXBUFFER_SAMPLES; i++) {
                v = (float) sin((idMath.PI * 2 * i) / 64);
                sample = (short) (v * 0x4000);
                ncd.put((i * 2) + 0, sample);
                ncd.put((i * 2) + 1, sample);
            }

            if (idSoundSystemLocal.useOpenAL) {
                alGetError();
//                alGenBuffers(1, openalBuffer);
                this.openalBuffer = alGenBuffers();
                if (alGetError() != AL_NO_ERROR) {
                    common.Error("idSoundCache: error generating OpenAL hardware buffer");
                }

                alGetError();
//                alBufferData(openalBuffer, objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, nonCacheData, objectMemSize, objectInfo.nSamplesPerSec);
                alBufferData(this.openalBuffer/*  <<TODO>>   */, this.objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, this.nonCacheData, this.objectInfo.nSamplesPerSec);
                if (alGetError() != AL_NO_ERROR) {
                    common.Error("idSoundCache: error loading data into OpenAL hardware buffer");
                } else {
                    this.hardwareBuffer = true;
                }
            }

            this.defaultSound = true;
        }

        /*
         ===================
         idSoundSample::Load

         Loads based on name, possibly doing a MakeDefault if necessary
         ===================
         */
        // loads the current sound based on name
        public void Load() {
            this.defaultSound = false;
            this.purged = false;
            this.hardwareBuffer = false;

            this.timestamp = GetNewTimeStamp();

            if (this.timestamp == FILE_NOT_FOUND_TIMESTAMP) {
                common.Warning("Couldn't load sound '%s' using default", this.name);
                MakeDefault();
                return;
            }

            // load it
            final idWaveFile fh = new idWaveFile();
            final waveformatex_s[] info = {null};

            if (fh.Open(this.name.getData(), info) == -1) {
                common.Warning("Couldn't load sound '%s' using default", this.name);
                MakeDefault();
                return;
            }

            if ((info[0].nChannels != 1) && (info[0].nChannels != 2)) {
                common.Warning("idSoundSample: %s has %d channels, using default", this.name, info[0].nChannels);
                fh.Close();
                MakeDefault();
                return;
            }

            if (info[0].wBitsPerSample != 16) {
                common.Warning("idSoundSample: %s is %dbits, expected 16bits using default", this.name, info[0].wBitsPerSample);
                fh.Close();
                MakeDefault();
                return;
            }

            if ((info[0].nSamplesPerSec != 44100) && (info[0].nSamplesPerSec != 22050) && (info[0].nSamplesPerSec != 11025)) {
                common.Warning("idSoundCache: %s is %dHz, expected 11025, 22050 or 44100 Hz. Using default", this.name, info[0].nSamplesPerSec);
                fh.Close();
                MakeDefault();
                return;
            }

            this.objectInfo = info[0];
            this.objectSize = fh.GetOutputSize();
            this.objectMemSize = fh.GetMemorySize();

            this.nonCacheData = Nio.newByteBuffer(this.objectMemSize);//soundCacheAllocator.Alloc( objectMemSize );
            final ByteBuffer temp = ByteBuffer.allocate(this.objectMemSize);
            fh.Read(temp, this.objectMemSize, null);
            this.nonCacheData.put(temp).rewind();

            // optionally convert it to 22kHz to save memory
            CheckForDownSample();

            // create hardware audio buffers 
            if (idSoundSystemLocal.useOpenAL) {
                // PCM loads directly;
                if (this.objectInfo.wFormatTag == WAVE_FORMAT_TAG_PCM) {
                    alGetError();
//                    alGenBuffers(1, openalBuffer);
                    this.openalBuffer = alGenBuffers();
                    if (alGetError() != AL_NO_ERROR) {
                        common.Error("idSoundCache: error generating OpenAL hardware buffer");
                    }
//                    if (alIsBuffer(openalBuffer)) {
                    if (alIsBuffer(this.openalBuffer)) {
                        alGetError();
//                        alBufferData(openalBuffer, objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, nonCacheData, objectMemSize, objectInfo.nSamplesPerSec);
                        alBufferData(this.openalBuffer, this.objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, this.nonCacheData, this.objectInfo.nSamplesPerSec);
                        if (alGetError() != AL_NO_ERROR) {
                            common.Error("idSoundCache: error loading data into OpenAL hardware buffer");
                        } else {
                            // Compute amplitude block size
                            final int blockSize = (512 * this.objectInfo.nSamplesPerSec) / 44100;

                            // Allocate amplitude data array
                            this.amplitudeData = Nio.newByteBuffer(((this.objectSize / blockSize) + 1) * 2 * Short.BYTES);//soundCacheAllocator.Alloc( ( objectSize / blockSize + 1 ) * 2 * sizeof( short) );

                            // Creating array of min/max amplitude pairs per blockSize samples
                            final ShortBuffer ncd = this.nonCacheData.asShortBuffer();
                            int i;
                            for (i = 0; i < this.objectSize; i += blockSize) {
                                short min = 32767;
                                short max = -32768;

                                int j;
                                for (j = 0; j < Min(this.objectSize - i, blockSize); j++) {
                                    min = (short) Math.min(ncd.get(i + j), min);
                                    max = (short) Math.max(ncd.get(i + j), max);
                                }

                                this.amplitudeData.putShort((i / blockSize) * 2, min);
                                this.amplitudeData.putShort(((i / blockSize) * 2) + 1, max);
                            }

                            this.hardwareBuffer = true;
                        }
                    }
                }

                // OGG decompressed at load time (when smaller than s_decompressionLimit seconds, 6 seconds by default)
                if (this.objectInfo.wFormatTag == WAVE_FORMAT_TAG_OGG) {
                    if ((MACOS_X && (this.objectSize < (this.objectInfo.nSamplesPerSec * idSoundSystemLocal.s_decompressionLimit.GetInteger())))
                            || (alIsExtensionPresent("EAX-RAM") &&  (this.objectSize < (this.objectInfo.nSamplesPerSec * idSoundSystemLocal.s_decompressionLimit.GetInteger())))) {
                        alGetError();
                        this.openalBuffer = alGenBuffers();
                        if (alGetError() != AL_NO_ERROR) {
                            common.Error("idSoundCache: error generating OpenAL hardware buffer");
                        }
                        if (alIsBuffer(this.openalBuffer)) {
                            final idSampleDecoder decoder = idSampleDecoder.Alloc();
                            ByteBuffer destData = Nio.newByteBuffer((LengthIn44kHzSamples() + 1) * Float.BYTES);//soundCacheAllocator.Alloc( ( LengthIn44kHzSamples() + 1 ) * sizeof( float ) );

                            // Decoder *always* outputs 44 kHz data
                            decoder.Decode(this, 0, LengthIn44kHzSamples(), destData.asFloatBuffer());

                            // Downsample back to original frequency (save memory)
                            if (this.objectInfo.nSamplesPerSec == 11025) {
                                for (int i = 0; i < this.objectSize; i++) {
                                    if (destData.getFloat(i * 4) < -32768.0f) {
                                        destData.putShort(i, Short.MIN_VALUE);
                                    } else if (destData.getFloat(i * 4) > 32767.0f) {
                                        destData.putShort(i, Short.MAX_VALUE);
                                    } else {
                                        destData.putShort(i, (short) idMath.FtoiFast(destData.getFloat(i * 4)));
                                    }
                                }
                            } else if (this.objectInfo.nSamplesPerSec == 22050) {
                                for (int i = 0; i < this.objectSize; i++) {
                                    if (destData.getFloat(i * 2) < -32768.0f) {
                                        destData.putShort(i, Short.MIN_VALUE);
                                    } else if (destData.getFloat(i * 2) > 32767.0f) {
                                        destData.putShort(i, Short.MAX_VALUE);
                                    } else {
                                        destData.putShort(i, (short) idMath.FtoiFast(destData.getFloat(i * 2)));
                                    }
                                }
                            } else {
                                for (int i = 0; i < this.objectSize; i++) {
                                    if (destData.getFloat(i) < -32768.0f) {
                                        destData.putShort(i, Short.MIN_VALUE);
                                    } else if (destData.getFloat(i) > 32767.0f) {
                                        destData.putShort(i, Short.MAX_VALUE);
                                    } else {
                                        destData.putShort(i, (short) idMath.FtoiFast(destData.getFloat(i)));
                                    }
                                }
                            }

                            alGetError();
//                            alBufferData(openalBuffer, objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, destData, objectSize * sizeof(short), objectInfo.nSamplesPerSec);
                            alBufferData(this.openalBuffer, this.objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, destData, this.objectInfo.nSamplesPerSec);
                            if (alGetError() != AL_NO_ERROR) {
                                common.Error("idSoundCache: error loading data into OpenAL hardware buffer");
                            } else {
                                // Compute amplitude block size
                                final int blockSize = (512 * this.objectInfo.nSamplesPerSec) / 44100;

                                // Allocate amplitude data array
                                this.amplitudeData = Nio.newByteBuffer(((this.objectSize / blockSize) + 1) * 2 * Short.BYTES);//soundCacheAllocator.Alloc( ( objectSize / blockSize + 1 ) * 2 * sizeof( short ) );

                                // Creating array of min/max amplitude pairs per blockSize samples
                                int i;
                                for (i = 0; i < this.objectSize; i += blockSize) {
                                    short min = 32767;
                                    short max = -32768;

                                    int j;
                                    for (j = 0; j < Min(this.objectSize - i, blockSize); j++) {
                                        min = destData.getShort(i + j) < min ? destData.getShort(i + j) : min;
                                        max = destData.getShort(i + j) > max ? destData.getShort(i + j) : max;
                                    }

                                    this.amplitudeData.putShort((i / blockSize) * 2, min);
                                    this.amplitudeData.putShort(((i / blockSize) * 2) + 1, max);
                                }

                                this.hardwareBuffer = true;
                            }

//					soundCacheAllocator.Free( (byte *)destData );
                            destData = null;
                            idSampleDecoder.Free(decoder);
                        }
                    }
                }

                // Free memory if sample was loaded into hardware
                if (this.hardwareBuffer) {
//			soundCacheAllocator.Free( nonCacheData );
                    this.nonCacheData = null;
                }
            }

            fh.Close();
        }

        // reloads if timestamp has changed, or always if force
        public void Reload(boolean force) {
            if (!force) {
                long newTimestamp;

                // check the timestamp
                newTimestamp = GetNewTimeStamp();

                if (newTimestamp == FILE_NOT_FOUND_TIMESTAMP) {
                    if (!this.defaultSound) {
                        common.Warning("Couldn't load sound '%s' using default", this.name);
                        MakeDefault();
                    }
                    return;
                }
                if (newTimestamp == this.timestamp) {
                    return;	// don't need to reload it
                }
            }

            common.Printf("reloading %s\n", this.name);
            PurgeSoundSample();
            Load();
        }

        public void PurgeSoundSample() {			// frees all data
            this.purged = true;

            if (this.hardwareBuffer && idSoundSystemLocal.useOpenAL) {
                alGetError();
//                alDeleteBuffers(1, openalBuffer);
                alDeleteBuffers(this.openalBuffer);
                if (alGetError() != AL_NO_ERROR) {
                    common.Error("idSoundCache: error unloading data from OpenAL hardware buffer");
                } else {
                    this.openalBuffer = 0;
                    this.hardwareBuffer = false;
                }
            }

            if (this.amplitudeData != null) {
//                soundCacheAllocator.Free(amplitudeData);
                this.amplitudeData = null;
            }

            if (this.nonCacheData != null) {
//                soundCacheAllocator.Free(nonCacheData);
                this.nonCacheData = null;
            }
        }

        public void CheckForDownSample() {		// down sample if required
            if (!idSoundSystemLocal.s_force22kHz.GetBool()) {
                return;
            }
            if ((this.objectInfo.wFormatTag != WAVE_FORMAT_TAG_PCM) || (this.objectInfo.nSamplesPerSec != 44100)) {
                return;
            }
            final int shortSamples = this.objectSize >> 1;
            final ByteBuffer converted = Nio.newByteBuffer(shortSamples * 2);// soundCacheAllocator.Alloc(shortSamples);

            if (this.objectInfo.nChannels == 1) {
                for (int i = 0; i < shortSamples; i++) {
                    converted.putShort(i, this.nonCacheData.getShort(i * 2));
                }
            } else {
                for (int i = 0; i < shortSamples; i += 2) {
                    converted.putShort(i + 0, this.nonCacheData.getShort((i * 2) + 0));
                    converted.putShort(i + 1, this.nonCacheData.getShort((i * 2) + 1));
                }
            }
//            soundCacheAllocator.Free(nonCacheData);
            this.nonCacheData = converted;
            this.objectSize >>= 1;
            this.objectMemSize >>= 1;
            this.objectInfo.nAvgBytesPerSec >>= 1;
            this.objectInfo.nSamplesPerSec >>= 1;
        }

        /*
         ===================
         idSoundSample::FetchFromCache

         Returns true on success.
         ===================
         */
        public boolean FetchFromCache(int offset, final ByteBuffer output, int[] position, int[] size, final boolean allowIO) {
            offset &= 0xfffffffe;

            if ((this.objectSize == 0) || (offset < 0) || (offset > (this.objectSize * 2/*(int) sizeof(short)*/)) || NOT(this.nonCacheData)) {
                return false;
            }

            if (output != null) {
                this.nonCacheData.mark();
                this.nonCacheData.position(offset);

                output.put(this.nonCacheData);

                this.nonCacheData.reset();
            }
            if (position != null) {
                position[0] = 0;
            }
            if (size != null) {
                size[0] = (this.objectSize * 2/*sizeof(short)*/) - offset;
                if (size[0] > SCACHE_SIZE) {
                    size[0] = SCACHE_SIZE;
                }
            }
            return true;
        }
    }

    /*
     ===================================================================================

     The actual sound cache.

     ===================================================================================
     */
    public static class idSoundCache {

        private boolean insideLevelLoad;
        private final idList<idSoundSample> listCache;
        //
        //

        public idSoundCache() {
            this.listCache = new idList<>();
//            soundCacheAllocator.Init();
//            soundCacheAllocator.SetLockMemory(true);
            this.listCache.AssureSize(1024, null);
            this.listCache.SetGranularity(256);
            this.insideLevelLoad = false;
        }
        // ~idSoundCache();

        /*
         ===================
         idSoundCache::FindSound

         Adds a sound object to the cache and returns a handle for it.
         ===================
         */
        public idSoundSample FindSound(final idStr filename, boolean loadOnDemandOnly) {
            idStr fname;

            fname = new idStr(filename);
            fname.BackSlashesToSlashes();
            fname.ToLower();

            declManager.MediaPrint("%s\n", fname);

            // check to see if object is already in cache
            for (int i = 0; i < this.listCache.Num(); i++) {
                final idSoundSample def = this.listCache.oGet(i);
                if ((def != null) && def.name.equals(fname)) {
                    def.levelLoadReferenced = true;
                    if (def.purged && !loadOnDemandOnly) {
                        def.Load();
                    }
                    return def;
                }
            }

            // create a new entry
            final idSoundSample def = new idSoundSample();

            int shandle = this.listCache.FindNull();
            if (shandle != -1) {
                this.listCache.oSet(shandle, def);
            } else {
                shandle = this.listCache.Append(def);
            }

            def.name = fname;
            def.levelLoadReferenced = true;
            def.onDemand = loadOnDemandOnly;
            def.purged = true;

            if (!loadOnDemandOnly) {
                // this may make it a default sound if it can't be loaded
                def.Load();
            }

            return def;
        }

        public int GetNumObjects() {
            return this.listCache.Num();
        }

        /*
         ===================
         idSoundCache::::GetObject

         returns a single cached object pointer
         ===================
         */
        public idSoundSample GetObject(final int index) {
            if ((index < 0) || (index > this.listCache.Num())) {
                return null;
            }
            return this.listCache.oGet(index);
        }

        /*
         ===================
         idSoundCache::ReloadSounds

         Completely nukes the current cache
         ===================
         */
        public void ReloadSounds(boolean force) {
            int i;

            for (i = 0; i < this.listCache.Num(); i++) {
                final idSoundSample def = this.listCache.oGet(i);
                if (def != null) {
                    def.Reload(force);
                }
            }
        }


        /*
         ====================
         BeginLevelLoad

         Mark all file based images as currently unused,
         but don't free anything.  Calls to ImageFromFile() will
         either mark the image as used, or create a new image without
         loading the actual data.
         ====================
         */
        public void BeginLevelLoad() {
            this.insideLevelLoad = true;

            for (int i = 0; i < this.listCache.Num(); i++) {
                final idSoundSample sample = this.listCache.oGet(i);
                if (null == sample) {
                    continue;
                }

                if (com_purgeAll.GetBool()) {
                    sample.PurgeSoundSample();
                }

                sample.levelLoadReferenced = false;
            }

//            soundCacheAllocator.FreeEmptyBaseBlocks();
        }

        /*
         ====================
         EndLevelLoad

         Free all samples marked as unused
         ====================
         */
        public void EndLevelLoad() {
            int useCount, purgeCount;
            common.Printf("----- idSoundCache::EndLevelLoad -----\n");

            this.insideLevelLoad = false;

            // purge the ones we don't need
            useCount = 0;
            purgeCount = 0;
            for (int i = 0; i < this.listCache.Num(); i++) {
                final idSoundSample sample = this.listCache.oGet(i);
                if (null == sample) {
                    continue;
                }
                if (sample.purged) {
                    continue;
                }
                if (!sample.levelLoadReferenced) {
//			common.Printf( "Purging %s\n", sample.name.c_str() );
                    purgeCount += sample.objectMemSize;
                    sample.PurgeSoundSample();
                } else {
                    useCount += sample.objectMemSize;
                }
            }

//            soundCacheAllocator.FreeEmptyBaseBlocks();
            common.Printf("%5dk referenced\n", useCount / 1024);
            common.Printf("%5dk purged\n", purgeCount / 1024);
            common.Printf("----------------------------------------\n");
        }

        public void PrintMemInfo(MemInfo_t mi) {
            int i, j, num = 0, total = 0;
            int[] sortIndex;
            idFile f;

            f = fileSystem.OpenFileWrite(mi.filebase + "_sounds.txt");
            if (null == f) {
                return;
            }

            // count
            for (i = 0; i < this.listCache.Num(); i++, num++) {
                if (null == this.listCache.oGet(i)) {
                    break;
                }
            }

            // sort first
            sortIndex = new int[num];

            for (i = 0; i < num; i++) {
                sortIndex[i] = i;
            }

            for (i = 0; i < (num - 1); i++) {
                for (j = i + 1; j < num; j++) {
                    if (this.listCache.oGet(sortIndex[i]).objectMemSize < this.listCache.oGet(sortIndex[j]).objectMemSize) {
                        final int temp = sortIndex[i];
                        sortIndex[i] = sortIndex[j];
                        sortIndex[j] = temp;
                    }
                }
            }

            // print next
            for (i = 0; i < num; i++) {
                final idSoundSample sample = this.listCache.oGet(sortIndex[i]);

                // this is strange
                if (null == sample) {
                    continue;
                }

                total += sample.objectMemSize;
                f.Printf("%s %s\n", idStr.FormatNumber(sample.objectMemSize).getData(), sample.name.getData());
            }

            mi.soundAssetsTotal = total;

            f.Printf("\nTotal sound bytes allocated: %s\n", idStr.FormatNumber(total).getData());
            fileSystem.CloseFile(f);
        }
    }
}
