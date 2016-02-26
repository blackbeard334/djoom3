package neo.Sound;

import static java.lang.Math.sin;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import neo.Sound.snd_cache.idSoundSample;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_OGG;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_PCM;
import neo.Sound.snd_local.idSampleDecoder;
import neo.Sound.snd_local.waveformatex_s;
import neo.Sound.snd_system.idSoundSystemLocal;
import neo.Sound.snd_wavefile.idWaveFile;
import static neo.TempDump.NOT;
import static neo.framework.BuildDefines.MACOS_X;
import neo.framework.Common.MemInfo_t;
import static neo.framework.Common.com_purgeAll;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.FILE_NOT_FOUND_TIMESTAMP;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.File_h.idFile;
import static neo.idlib.Lib.Min;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.alGetError;
import static org.lwjgl.openal.AL10.alIsExtensionPresent;

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
            objectInfo = new waveformatex_s();
            objectSize = 0;
            objectMemSize = 0;
            nonCacheData = null;
            amplitudeData = null;
            openalBuffer = 0;
            hardwareBuffer = false;
            defaultSound = false;
            onDemand = false;
            purged = false;
            levelLoadReferenced = false;
        }
        // ~idSoundSample();

        public int LengthIn44kHzSamples() {
            // objectSize is samples
            if (objectInfo.nSamplesPerSec == 11025) {
                return objectSize << 2;
            } else if (objectInfo.nSamplesPerSec == 22050) {
                return objectSize << 1;
            } else {
                return objectSize << 0;
            }
        }

        public long/*ID_TIME_T*/ GetNewTimeStamp() {
            long[] timestamp = {0};

            fileSystem.ReadFile(name.toString(), null, timestamp);
            if (timestamp[0] == FILE_NOT_FOUND_TIMESTAMP) {
                idStr oggName = new idStr(name);
                oggName.SetFileExtension(".ogg");
                fileSystem.ReadFile(oggName.toString(), null, timestamp);
            }
            return timestamp[0];
        }

        // turns it into a beep	
        public void MakeDefault() {
            int i;
            float v;
            short sample;

//	memset( &objectInfo, 0, sizeof( objectInfo ) );
            objectInfo = new waveformatex_s();

            objectInfo.nChannels = 1;
            objectInfo.wBitsPerSample = 16;
            objectInfo.nSamplesPerSec = 44100;

            objectSize = MIXBUFFER_SAMPLES * 2;
            objectMemSize = objectSize * 2;//* sizeof(short);

            nonCacheData = BufferUtils.createByteBuffer(objectMemSize);//soundCacheAllocator.Alloc(objectMemSize);

            ShortBuffer ncd = nonCacheData.asShortBuffer();

            for (i = 0; i < MIXBUFFER_SAMPLES; i++) {
                v = (float) sin(idMath.PI * 2 * i / 64);
                sample = (short) (v * 0x4000);
                ncd.put(i * 2 + 0, sample);
                ncd.put(i * 2 + 1, sample);
            }

            if (idSoundSystemLocal.useOpenAL) {
                alGetError();
//                alGenBuffers(1, openalBuffer);
                openalBuffer = AL10.alGenBuffers();
                if (alGetError() != AL_NO_ERROR) {
                    common.Error("idSoundCache: error generating OpenAL hardware buffer");
                }

                alGetError();
//                alBufferData(openalBuffer, objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, nonCacheData, objectMemSize, objectInfo.nSamplesPerSec);
                AL10.alBufferData(openalBuffer/*  <<TODO>>   */, objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, nonCacheData, (int) objectInfo.nSamplesPerSec);
                if (alGetError() != AL_NO_ERROR) {
                    common.Error("idSoundCache: error loading data into OpenAL hardware buffer");
                } else {
                    hardwareBuffer = true;
                }
            }

            defaultSound = true;
        }

        /*
         ===================
         idSoundSample::Load

         Loads based on name, possibly doing a MakeDefault if necessary
         ===================
         */
        // loads the current sound based on name
        public void Load() {
            defaultSound = false;
            purged = false;
            hardwareBuffer = false;

            timestamp = GetNewTimeStamp();

            if (timestamp == FILE_NOT_FOUND_TIMESTAMP) {
                common.Warning("Couldn't load sound '%s' using default", name);
                MakeDefault();
                return;
            }

            // load it
            idWaveFile fh = new idWaveFile();
            waveformatex_s[] info = {null};

            if (fh.Open(name.toString(), info) == -1) {
                common.Warning("Couldn't load sound '%s' using default", name);
                MakeDefault();
                return;
            }

            if (info[0].nChannels != 1 && info[0].nChannels != 2) {
                common.Warning("idSoundSample: %s has %d channels, using default", name, info[0].nChannels);
                fh.Close();
                MakeDefault();
                return;
            }

            if (info[0].wBitsPerSample != 16) {
                common.Warning("idSoundSample: %s is %dbits, expected 16bits using default", name, info[0].wBitsPerSample);
                fh.Close();
                MakeDefault();
                return;
            }

            if (info[0].nSamplesPerSec != 44100 && info[0].nSamplesPerSec != 22050 && info[0].nSamplesPerSec != 11025) {
                common.Warning("idSoundCache: %s is %dHz, expected 11025, 22050 or 44100 Hz. Using default", name, info[0].nSamplesPerSec);
                fh.Close();
                MakeDefault();
                return;
            }

            objectInfo = info[0];
            objectSize = fh.GetOutputSize();
            objectMemSize = fh.GetMemorySize();

            nonCacheData = BufferUtils.createByteBuffer(objectMemSize);//soundCacheAllocator.Alloc( objectMemSize );
            ByteBuffer temp = ByteBuffer.allocate(objectMemSize);
            fh.Read(temp, objectMemSize, null);
            nonCacheData.put(temp).rewind();

            // optionally convert it to 22kHz to save memory
            CheckForDownSample();

            // create hardware audio buffers 
            if (idSoundSystemLocal.useOpenAL) {
                // PCM loads directly;
                if (objectInfo.wFormatTag == WAVE_FORMAT_TAG_PCM) {
                    alGetError();
//                    alGenBuffers(1, openalBuffer);
                    openalBuffer = AL10.alGenBuffers();
                    if (alGetError() != AL_NO_ERROR) {
                        common.Error("idSoundCache: error generating OpenAL hardware buffer");
                    }
//                    if (alIsBuffer(openalBuffer)) {
                    if (AL10.alIsBuffer(openalBuffer)) {
                        alGetError();
//                        alBufferData(openalBuffer, objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, nonCacheData, objectMemSize, objectInfo.nSamplesPerSec);
                        AL10.alBufferData(openalBuffer, objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, nonCacheData, (int) objectInfo.nSamplesPerSec);
                        if (alGetError() != AL_NO_ERROR) {
                            common.Error("idSoundCache: error loading data into OpenAL hardware buffer");
                        } else {
                            // Compute amplitude block size
                            int blockSize = (int) (512 * objectInfo.nSamplesPerSec / 44100);

                            // Allocate amplitude data array
                            amplitudeData = BufferUtils.createByteBuffer((objectSize / blockSize + 1) * 2 * Short.BYTES);//soundCacheAllocator.Alloc( ( objectSize / blockSize + 1 ) * 2 * sizeof( short) );

                            // Creating array of min/max amplitude pairs per blockSize samples
                            final ShortBuffer ncd = nonCacheData.asShortBuffer();
                            int i;
                            for (i = 0; i < objectSize; i += blockSize) {
                                short min = 32767;
                                short max = -32768;

                                int j;
                                for (j = 0; j < Min(objectSize - i, blockSize); j++) {
                                    min = (short) Math.min(ncd.get(i + j), min);
                                    max = (short) Math.max(ncd.get(i + j), max);
                                }

                                amplitudeData.putShort((i / blockSize) * 2, min);
                                amplitudeData.putShort((i / blockSize) * 2 + 1, max);
                            }

                            hardwareBuffer = true;
                        }
                    }
                }

                // OGG decompressed at load time (when smaller than s_decompressionLimit seconds, 6 seconds by default)
                if (objectInfo.wFormatTag == WAVE_FORMAT_TAG_OGG) {
                    if ((MACOS_X && (objectSize < (objectInfo.nSamplesPerSec * idSoundSystemLocal.s_decompressionLimit.GetInteger())))
                            || (alIsExtensionPresent("EAX-RAM") &&  (objectSize < (objectInfo.nSamplesPerSec * idSoundSystemLocal.s_decompressionLimit.GetInteger())))) {
                        alGetError();
                        openalBuffer = AL10.alGenBuffers();
                        if (alGetError() != AL_NO_ERROR) {
                            common.Error("idSoundCache: error generating OpenAL hardware buffer");
                        }
                        if (AL10.alIsBuffer(openalBuffer)) {
                            idSampleDecoder decoder = idSampleDecoder.Alloc();
                            ByteBuffer destData = BufferUtils.createByteBuffer((LengthIn44kHzSamples() + 1) * Float.BYTES);//soundCacheAllocator.Alloc( ( LengthIn44kHzSamples() + 1 ) * sizeof( float ) );

                            // Decoder *always* outputs 44 kHz data
                            decoder.Decode(this, 0, LengthIn44kHzSamples(), destData.asFloatBuffer());

                            // Downsample back to original frequency (save memory)
                            if (objectInfo.nSamplesPerSec == 11025) {
                                for (int i = 0; i < objectSize; i++) {
                                    if (destData.getFloat(i * 4) < -32768.0f) {
                                        destData.putShort(i, Short.MIN_VALUE);
                                    } else if (destData.getFloat(i * 4) > 32767.0f) {
                                        destData.putShort(i, Short.MAX_VALUE);
                                    } else {
                                        destData.putShort(i, (short) idMath.FtoiFast(destData.getFloat(i * 4)));
                                    }
                                }
                            } else if (objectInfo.nSamplesPerSec == 22050) {
                                for (int i = 0; i < objectSize; i++) {
                                    if (destData.getFloat(i * 2) < -32768.0f) {
                                        destData.putShort(i, Short.MIN_VALUE);
                                    } else if (destData.getFloat(i * 2) > 32767.0f) {
                                        destData.putShort(i, Short.MAX_VALUE);
                                    } else {
                                        destData.putShort(i, (short) idMath.FtoiFast(destData.getFloat(i * 2)));
                                    }
                                }
                            } else {
                                for (int i = 0; i < objectSize; i++) {
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
                            AL10.alBufferData(openalBuffer, objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, destData, (int) objectInfo.nSamplesPerSec);
                            if (alGetError() != AL_NO_ERROR) {
                                common.Error("idSoundCache: error loading data into OpenAL hardware buffer");
                            } else {
                                // Compute amplitude block size
                                int blockSize = (int) (512 * objectInfo.nSamplesPerSec / 44100);

                                // Allocate amplitude data array
                                amplitudeData = BufferUtils.createByteBuffer((objectSize / blockSize + 1) * 2 * Short.BYTES);//soundCacheAllocator.Alloc( ( objectSize / blockSize + 1 ) * 2 * sizeof( short ) );

                                // Creating array of min/max amplitude pairs per blockSize samples
                                int i;
                                for (i = 0; i < objectSize; i += blockSize) {
                                    short min = 32767;
                                    short max = -32768;

                                    int j;
                                    for (j = 0; j < Min(objectSize - i, blockSize); j++) {
                                        min = destData.getShort(i + j) < min ? destData.getShort(i + j) : min;
                                        max = destData.getShort(i + j) > max ? destData.getShort(i + j) : max;
                                    }

                                    amplitudeData.putShort((i / blockSize) * 2, min);
                                    amplitudeData.putShort((i / blockSize) * 2 + 1, max);
                                }

                                hardwareBuffer = true;
                            }

//					soundCacheAllocator.Free( (byte *)destData );
                            destData = null;
                            idSampleDecoder.Free(decoder);
                        }
                    }
                }

                // Free memory if sample was loaded into hardware
                if (hardwareBuffer) {
//			soundCacheAllocator.Free( nonCacheData );
                    nonCacheData = null;
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
                    if (!defaultSound) {
                        common.Warning("Couldn't load sound '%s' using default", name);
                        MakeDefault();
                    }
                    return;
                }
                if (newTimestamp == timestamp) {
                    return;	// don't need to reload it
                }
            }

            common.Printf("reloading %s\n", name);
            PurgeSoundSample();
            Load();
        }

        public void PurgeSoundSample() {			// frees all data
            purged = true;

            if (hardwareBuffer && idSoundSystemLocal.useOpenAL) {
                alGetError();
//                alDeleteBuffers(1, openalBuffer);
                AL10.alDeleteBuffers(openalBuffer);
                if (alGetError() != AL_NO_ERROR) {
                    common.Error("idSoundCache: error unloading data from OpenAL hardware buffer");
                } else {
                    openalBuffer = 0;
                    hardwareBuffer = false;
                }
            }

            if (amplitudeData != null) {
//                soundCacheAllocator.Free(amplitudeData);
                amplitudeData = null;
            }

            if (nonCacheData != null) {
//                soundCacheAllocator.Free(nonCacheData);
                nonCacheData = null;
            }
        }

        public void CheckForDownSample() {		// down sample if required
            if (!idSoundSystemLocal.s_force22kHz.GetBool()) {
                return;
            }
            if (objectInfo.wFormatTag != WAVE_FORMAT_TAG_PCM || objectInfo.nSamplesPerSec != 44100) {
                return;
            }
            int shortSamples = objectSize >> 1;
            ByteBuffer converted = BufferUtils.createByteBuffer(shortSamples * 2);// soundCacheAllocator.Alloc(shortSamples);

            if (objectInfo.nChannels == 1) {
                for (int i = 0; i < shortSamples; i++) {
                    converted.putShort(i, nonCacheData.getShort(i * 2));
                }
            } else {
                for (int i = 0; i < shortSamples; i += 2) {
                    converted.putShort(i + 0, nonCacheData.getShort(i * 2 + 0));
                    converted.putShort(i + 1, nonCacheData.getShort(i * 2 + 1));
                }
            }
//            soundCacheAllocator.Free(nonCacheData);
            nonCacheData = converted;
            objectSize >>= 1;
            objectMemSize >>= 1;
            objectInfo.nAvgBytesPerSec >>= 1;
            objectInfo.nSamplesPerSec >>= 1;
        }

        /*
         ===================
         idSoundSample::FetchFromCache

         Returns true on success.
         ===================
         */
        public boolean FetchFromCache(int offset, final ByteBuffer output, int[] position, int[] size, final boolean allowIO) {
            offset &= 0xfffffffe;

            if (objectSize == 0 || offset < 0 || offset > objectSize * 2/*(int) sizeof(short)*/ || NOT(nonCacheData)) {
                return false;
            }

            if (output != null) {
                nonCacheData.mark();
                nonCacheData.position(offset);

                output.put(nonCacheData);

                nonCacheData.reset();
            }
            if (position != null) {
                position[0] = 0;
            }
            if (size != null) {
                size[0] = objectSize * 2/*sizeof(short)*/ - offset;
                if (size[0] > SCACHE_SIZE) {
                    size[0] = SCACHE_SIZE;
                }
            }
            return true;
        }
    };

    /*
     ===================================================================================

     The actual sound cache.

     ===================================================================================
     */
    public static class idSoundCache {

        private boolean insideLevelLoad;
        private idList<idSoundSample> listCache;
        //
        //

        public idSoundCache() {
            this.listCache = new idList<>();
//            soundCacheAllocator.Init();
//            soundCacheAllocator.SetLockMemory(true);
            listCache.AssureSize(1024, null);
            listCache.SetGranularity(256);
            insideLevelLoad = false;
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
            for (int i = 0; i < listCache.Num(); i++) {
                idSoundSample def = listCache.oGet(i);
                if (def != null && def.name.equals(fname)) {
                    def.levelLoadReferenced = true;
                    if (def.purged && !loadOnDemandOnly) {
                        def.Load();
                    }
                    return def;
                }
            }

            // create a new entry
            idSoundSample def = new idSoundSample();

            int shandle = listCache.FindNull();
            if (shandle != -1) {
                listCache.oSet(shandle, def);
            } else {
                shandle = listCache.Append(def);
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
            return listCache.Num();
        }

        /*
         ===================
         idSoundCache::::GetObject

         returns a single cached object pointer
         ===================
         */
        public idSoundSample GetObject(final int index) {
            if (index < 0 || index > listCache.Num()) {
                return null;
            }
            return listCache.oGet(index);
        }

        /*
         ===================
         idSoundCache::ReloadSounds

         Completely nukes the current cache
         ===================
         */
        public void ReloadSounds(boolean force) {
            int i;

            for (i = 0; i < listCache.Num(); i++) {
                idSoundSample def = listCache.oGet(i);
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
            insideLevelLoad = true;

            for (int i = 0; i < listCache.Num(); i++) {
                idSoundSample sample = listCache.oGet(i);
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

            insideLevelLoad = false;

            // purge the ones we don't need
            useCount = 0;
            purgeCount = 0;
            for (int i = 0; i < listCache.Num(); i++) {
                idSoundSample sample = listCache.oGet(i);
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
            for (i = 0; i < listCache.Num(); i++, num++) {
                if (null == listCache.oGet(i)) {
                    break;
                }
            }

            // sort first
            sortIndex = new int[num];

            for (i = 0; i < num; i++) {
                sortIndex[i] = i;
            }

            for (i = 0; i < num - 1; i++) {
                for (j = i + 1; j < num; j++) {
                    if (listCache.oGet(sortIndex[i]).objectMemSize < listCache.oGet(sortIndex[j]).objectMemSize) {
                        int temp = sortIndex[i];
                        sortIndex[i] = sortIndex[j];
                        sortIndex[j] = temp;
                    }
                }
            }

            // print next
            for (i = 0; i < num; i++) {
                idSoundSample sample = listCache.oGet(sortIndex[i]);

                // this is strange
                if (null == sample) {
                    continue;
                }

                total += sample.objectMemSize;
                f.Printf("%s %s\n", idStr.FormatNumber(sample.objectMemSize).toString(), sample.name.toString());
            }

            mi.soundAssetsTotal = total;

            f.Printf("\nTotal sound bytes allocated: %s\n", idStr.FormatNumber(total).toString());
            fileSystem.CloseFile(f);
        }
    };
}
