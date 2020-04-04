package neo.Sound;

import static java.lang.Math.pow;
import static neo.Sound.snd_local.PRIMARYFREQ;
import static neo.Sound.snd_local.ROOM_SLICES_IN_BUFFER;
import static neo.Sound.snd_local.SND_EPSILON;
import static neo.Sound.snd_local.SOUND_MAX_CHANNELS;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_OGG;
import static neo.Sound.snd_shader.SSF_LOOPING;
import static neo.TempDump.NOT;
import static neo.framework.BuildDefines.ID_DEDICATED;
import static neo.framework.BuildDefines.ID_OPENAL;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_FLOAT;
import static neo.framework.CVarSystem.CVAR_INIT;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_NOCHEAT;
import static neo.framework.CVarSystem.CVAR_ROM;
import static neo.framework.CVarSystem.CVAR_SOUND;
import static neo.framework.CmdSystem.CMD_FL_CHEAT;
import static neo.framework.CmdSystem.CMD_FL_SOUND;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.Common.common;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;
import static neo.sys.win_main.Sys_Printf;
import static neo.sys.win_main.Sys_Sleep;
import static neo.sys.win_shared.Sys_Milliseconds;
import static neo.sys.win_snd.Sys_FreeOpenAL;
import static neo.sys.win_snd.Sys_LoadOpenAL;
import static neo.openal.QALConstantsIfc.AL_BUFFER;
import static neo.openal.QALConstantsIfc.AL_NO_ERROR;
import static neo.openal.QALConstantsIfc.AL_ROLLOFF_FACTOR;
import static neo.openal.QALConstantsIfc.ALC_DEVICE_SPECIFIER;
import static neo.openal.QAL.alDeleteSources;
import static neo.openal.QAL.alGenSources;
import static neo.openal.QAL.alGetError;
import static neo.openal.QAL.alIsExtensionPresent;
import static neo.openal.QAL.alSourceStop;
import static neo.openal.QAL.alSourcef;
import static neo.openal.QAL.alSourcei;
import static neo.openal.QAL.alcCloseDevice;
import static neo.openal.QAL.alcCreateContext;
import static neo.openal.QAL.alcDestroyContext;
import static neo.openal.QAL.alcGetString;
import static neo.openal.QAL.alcMakeContextCurrent;
import static neo.openal.QAL.alcOpenDevice;
import static neo.openal.QAL.alcProcessContext;
import static neo.openal.QAL.alcSuspendContext;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;

import neo.Renderer.Cinematic.cinData_t;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Sound.snd_cache.idSoundCache;
import neo.Sound.snd_cache.idSoundSample;
import neo.Sound.snd_efxfile.idEFXFile;
import neo.Sound.snd_emitter.SoundFX;
import neo.Sound.snd_emitter.SoundFX_Comb;
import neo.Sound.snd_emitter.SoundFX_Lowpass;
import neo.Sound.snd_emitter.idSoundChannel;
import neo.Sound.snd_emitter.idSoundEmitterLocal;
import neo.Sound.snd_local.idAudioHardware;
import neo.Sound.snd_local.idSampleDecoder;
import neo.Sound.snd_local.waveformatex_s;
import neo.Sound.snd_world.idSoundWorldLocal;
import neo.Sound.snd_world.s_stats;
import neo.Sound.sound.idSoundSystem;
import neo.Sound.sound.idSoundWorld;
import neo.Sound.sound.soundDecoderInfo_t;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.Common.MemInfo_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.openal.QAL;

/**
 *
 */
public class snd_system {

    static idSoundSystemLocal soundSystemLocal = new idSoundSystemLocal();
    public static idSoundSystem soundSystem = soundSystemLocal;

    /*
     ===================================================================================

     idSoundSystemLocal

     ===================================================================================
     */
    static class openalSource_t {

        int/*ALuint*/ handle;
        int            startTime;
        idSoundChannel chan;
        boolean        inUse;
        boolean        looping;
        boolean        stereo;
    }

    public static class idSoundSystemLocal extends neo.Sound.sound.idSoundSystem {

        public              idAudioHardware   snd_audio_hw;
        public              idSoundCache      soundCache;
        //
        public              idSoundWorldLocal currentSoundWorld;    // the one to mix each async tic
        //
        public              int               olddwCurrentWritePos; // statistics
        public              int               buffers;              // statistics
        public              int               CurrentSoundTime;     // set by the async thread and only used by the main thread
        //
        public /*unsigned*/ int               nextWriteBlock;
        //
        public float[] realAccum = new float[(6 * MIXBUFFER_SAMPLES) + 16];
        public float[] finalMixBuffer;                              // points inside realAccum at a 16 byte aligned boundary
        //
        public boolean isInitialized;
        public boolean muted;
        public boolean shutdown;
        //
        public s_stats soundStats    = new s_stats();               // NOTE: updated throughout the code, not displayed anywhere
        //
        public int[]   meterTops     = new int[256];
        public int[]   meterTopsTime = new int[256];
        //
        public int/*dword*/[] graph;
        //
        public float[] volumesDB = new float[1200];                 // dB to float volume conversion
        //
        public idList<SoundFX> fxList;
        //
        public long       openalDevice;
        public long      openalContext;
        public int/*ALsizei*/  openalSourceCount;
        public openalSource_t[] openalSources = new openalSource_t[256];
//        public boolean alEAXSet;
//        public boolean alEAXGet;
//        public boolean alEAXSetBufferMode;
//        public boolean alEAXGetBufferMode;
        public idEFXFile EFXDatabase = new idEFXFile();
        public boolean efxloaded;
        // latches
        public static boolean useOpenAL;
        public static boolean useEAXReverb = false;
        // mark available during initialization, or through an explicit test
        public static int     EAXAvailable = -1;
        //
        //
        public static final idCVar s_noSound;

        static {
            if (ID_DEDICATED) {
                s_noSound = new idCVar("s_noSound", "1", CVAR_SOUND | CVAR_BOOL | CVAR_ROM, "");
            } else {
                s_noSound = new idCVar("s_noSound", "0", CVAR_SOUND | CVAR_BOOL | CVAR_NOCHEAT, "");
            }
        }

        public static final idCVar s_quadraticFalloff      = new idCVar("s_quadraticFalloff", "1", CVAR_SOUND | CVAR_BOOL, "");
        public static final idCVar s_drawSounds            = new idCVar("s_drawSounds", "0", CVAR_SOUND | CVAR_INTEGER, "", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        public static final idCVar s_showStartSound        = new idCVar("s_showStartSound", "0", CVAR_SOUND | CVAR_BOOL, "");
        public static final idCVar s_useOcclusion          = new idCVar("s_useOcclusion", "1", CVAR_SOUND | CVAR_BOOL, "");
        public static final idCVar s_maxSoundsPerShader    = new idCVar("s_maxSoundsPerShader", "0", CVAR_SOUND | CVAR_ARCHIVE, "", 0, 10, new idCmdSystem.ArgCompletion_Integer(0, 10));
        public static final idCVar s_showLevelMeter        = new idCVar("s_showLevelMeter", "0", CVAR_SOUND | CVAR_BOOL, "");
        public static final idCVar s_constantAmplitude     = new idCVar("s_constantAmplitude", "-1", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_minVolume6            = new idCVar("s_minVolume6", "0", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_dotbias6              = new idCVar("s_dotbias6", "0.8", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_minVolume2            = new idCVar("s_minVolume2", "0.25", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_dotbias2              = new idCVar("s_dotbias2", "1.1", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_spatializationDecay   = new idCVar("s_spatializationDecay", "2", CVAR_SOUND | CVAR_ARCHIVE | CVAR_FLOAT, "");
        public static final idCVar s_reverse               = new idCVar("s_reverse", "0", CVAR_SOUND | CVAR_ARCHIVE | CVAR_BOOL, "");
        public static final idCVar s_meterTopTime          = new idCVar("s_meterTopTime", "2000", CVAR_SOUND | CVAR_ARCHIVE | CVAR_INTEGER, "");
        public static final idCVar s_volume                = new idCVar("s_volume_dB", "0", CVAR_SOUND | CVAR_ARCHIVE | CVAR_FLOAT, "volume in dB");
        public static final idCVar s_playDefaultSound      = new idCVar("s_playDefaultSound", "1", CVAR_SOUND | CVAR_ARCHIVE | CVAR_BOOL, "play a beep for missing sounds");
        public static final idCVar s_subFraction           = new idCVar("s_subFraction", "0.75", CVAR_SOUND | CVAR_ARCHIVE | CVAR_FLOAT, "volume to subwoofer in 5.1");
        public static final idCVar s_globalFraction        = new idCVar("s_globalFraction", "0.8", CVAR_SOUND | CVAR_ARCHIVE | CVAR_FLOAT, "volume to all speakers when not spatialized");
        public static final idCVar s_doorDistanceAdd       = new idCVar("s_doorDistanceAdd", "150", CVAR_SOUND | CVAR_ARCHIVE | CVAR_FLOAT, "reduce sound volume with this distance when going through a door");
        public static final idCVar s_singleEmitter         = new idCVar("s_singleEmitter", "0", CVAR_SOUND | CVAR_INTEGER, "mute all sounds but this emitter");
        public static final idCVar s_numberOfSpeakers      = new idCVar("s_numberOfSpeakers", "2", CVAR_SOUND | CVAR_ARCHIVE, "number of speakers");
        public static final idCVar s_force22kHz            = new idCVar("s_force22kHz", "0", CVAR_SOUND | CVAR_BOOL, "");
        public static final idCVar s_clipVolumes           = new idCVar("s_clipVolumes", "1", CVAR_SOUND | CVAR_BOOL, "");
        public static final idCVar s_realTimeDecoding      = new idCVar("s_realTimeDecoding", "1", CVAR_SOUND | CVAR_BOOL | CVAR_INIT, "");
        //
        public static final idCVar s_slowAttenuate         = new idCVar("s_slowAttenuate", "1", CVAR_SOUND | CVAR_BOOL, "slowmo sounds attenuate over shorted distance");
        public static final idCVar s_enviroSuitCutoffFreq  = new idCVar("s_enviroSuitCutoffFreq", "2000", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_enviroSuitCutoffQ     = new idCVar("s_enviroSuitCutoffQ", "2", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_reverbTime            = new idCVar("s_reverbTime", "1000", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_reverbFeedback        = new idCVar("s_reverbFeedback", "0.333", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_enviroSuitVolumeScale = new idCVar("s_enviroSuitVolumeScale", "0.9", CVAR_SOUND | CVAR_FLOAT, "");
        public static final idCVar s_skipHelltimeFX        = new idCVar("s_skipHelltimeFX", "0", CVAR_SOUND | CVAR_BOOL, "");
        //
        public static final idCVar s_libOpenAL;
        public static final idCVar s_useOpenAL;
        public static final idCVar s_useEAXReverb;
        public static final idCVar s_muteEAXReverb;
        public static final idCVar s_decompressionLimit;

        static {
            if (ID_OPENAL) {//TODO: turn on the rest of our openAL extensions.
                // off by default. OpenAL DLL gets loaded on-demand. EDIT: not anymore.
                s_libOpenAL = new idCVar("s_libOpenAL", "openal32.dll", CVAR_SOUND | CVAR_ARCHIVE, "OpenAL DLL name/path");
                s_useOpenAL = new idCVar("s_useOpenAL", "1", CVAR_SOUND | CVAR_BOOL | CVAR_ARCHIVE, "use OpenAL");
                s_useEAXReverb = new idCVar("s_useEAXReverb", "1", CVAR_SOUND | CVAR_BOOL | CVAR_ARCHIVE, "use EAX reverb");
                s_muteEAXReverb = new idCVar("s_muteEAXReverb", "0", CVAR_SOUND | CVAR_BOOL, "mute eax reverb");
                s_decompressionLimit = new idCVar("s_decompressionLimit", "6", CVAR_SOUND | CVAR_INTEGER | CVAR_ARCHIVE, "specifies maximum uncompressed sample length in seconds");
            } else {
                s_libOpenAL = new idCVar("s_libOpenAL", "openal32.dll", CVAR_SOUND | CVAR_ARCHIVE, "OpenAL is not supported in this build");
                s_useOpenAL = new idCVar("s_useOpenAL", "0", CVAR_SOUND | CVAR_BOOL | CVAR_ROM, "OpenAL is not supported in this build");
                s_useEAXReverb = new idCVar("s_useEAXReverb", "0", CVAR_SOUND | CVAR_BOOL | CVAR_ROM, "EAX not available in this build");
                s_muteEAXReverb = new idCVar("s_muteEAXReverb", "0", CVAR_SOUND | CVAR_BOOL | CVAR_ROM, "mute eax reverb");
                s_decompressionLimit = new idCVar("s_decompressionLimit", "6", CVAR_SOUND | CVAR_INTEGER | CVAR_ROM, "specifies maximum uncompressed sample length in seconds");
            }
        }
        //
        //

        public idSoundSystemLocal() {
            this.isInitialized = false;
        }

        // all non-hardware initialization
        /*
         ===============
         idSoundSystemLocal::Init

         initialize the sound system
         ===============
         */
        @Override
        public void Init() {

            common.Printf("----- Initializing Sound System ------\n");

            this.isInitialized = false;
            this.muted = false;
            this.shutdown = false;

            this.currentSoundWorld = null;
            this.soundCache = null;

            this.olddwCurrentWritePos = 0;
            this.buffers = 0;
            this.CurrentSoundTime = 0;

            this.nextWriteBlock = 0xffffffff;

//            memset(meterTops, 0, sizeof(meterTops));
            this.meterTops = new int[this.meterTops.length];
//            memset(meterTopsTime, 0, sizeof(meterTopsTime));
            this.meterTopsTime = new int[this.meterTopsTime.length];

            for (int i = -600; i < 600; i++) {
                final float pt = i * 0.1f;
                this.volumesDB[i + 600] = (float) pow(2.0f, (pt * (1.0f / 6.0f)));
            }

            // make a 16 byte aligned finalMixBuffer
            this.finalMixBuffer = this.realAccum;//(float[]) ((((int) realAccum) + 15) & ~15);

            this.graph = null;

            if (!s_noSound.GetBool()) {
                idSampleDecoder.Init();
                this.soundCache = new idSoundCache();
            }

            // set up openal device and context
            common.StartupVariable("s_useOpenAL", true);
            common.StartupVariable("s_useEAXReverb", true);

            if (idSoundSystemLocal.s_useOpenAL.GetBool() || idSoundSystemLocal.s_useEAXReverb.GetBool()) {
                if (!Sys_LoadOpenAL()) {
                    idSoundSystemLocal.s_useOpenAL.SetBool(false);
                } else {
                    common.Printf("Setup OpenAL device and context... ");
                    this.openalDevice = alcOpenDevice((ByteBuffer) null);
                    this.openalContext = alcCreateContext(this.openalDevice, (IntBuffer) null);

                    alcMakeContextCurrent(this.openalContext);
                    
                    QAL.createCapabilities(this.openalDevice);
                    common.Printf("Done.\n");

                    // try to obtain EAX extensions
                    if (idSoundSystemLocal.s_useEAXReverb.GetBool() && alIsExtensionPresent(/*ID_ALCHAR*/"EAX4.0")) {
                        idSoundSystemLocal.s_useOpenAL.SetBool(true);	// EAX presence causes AL enable
//                        alEAXSet = true;//(EAXSet) alGetProcAddress(/*ID_ALCHAR*/"EAXSet");
//                        alEAXGet = true;//(EAXGet) alGetProcAddress(/*ID_ALCHAR*/"EAXGet");
                        common.Printf("OpenAL: found EAX 4.0 extension\n");
                    } else {
                        common.Printf("OpenAL: EAX 4.0 extension not found\n");
                        idSoundSystemLocal.s_useEAXReverb.SetBool(false);
//                        alEAXSet = false;//(EAXSet) null;
//                        alEAXGet = false;//(EAXGet) null;
                    }

                    // try to obtain EAX-RAM extension - not required for operation
//                    if (alIsExtensionPresent(/*ID_ALCHAR*/"EAX-RAM")) {
//                        alEAXSetBufferMode = true;//(EAXSetBufferMode) alGetProcAddress(/*ID_ALCHAR*/"EAXSetBufferMode");
//                        alEAXGetBufferMode = true;//(EAXGetBufferMode) alGetProcAddress(/*ID_ALCHAR*/"EAXGetBufferMode");
//                        common.Printf("OpenAL: found EAX-RAM extension, %dkB\\%dkB\n", alGetInteger(alGetEnumValue(/*ID_ALCHAR*/"AL_EAX_RAM_FREE")) / 1024, alGetInteger(alGetEnumValue(/*ID_ALCHAR*/"AL_EAX_RAM_SIZE")) / 1024);
//                    } else {
//                        alEAXSetBufferMode = false;//(EAXSetBufferMode) null;
//                        alEAXGetBufferMode = false;//(EAXGetBufferMode) null;
//                        common.Printf("OpenAL: no EAX-RAM extension\n");
//                    }

                    if (!idSoundSystemLocal.s_useOpenAL.GetBool()) {
                        common.Printf("OpenAL: disabling ( no EAX ). Using legacy mixer.\n");

                        alcMakeContextCurrent(this.openalContext);

                        alcDestroyContext(this.openalContext);
                        this.openalContext = 0;

                        alcCloseDevice(this.openalDevice);
                        this.openalDevice = 0;
                    } else {

                        int/*ALuint*/ handle;
                        this.openalSourceCount = 0;

                        while (this.openalSourceCount < 256) {
                            alGetError();
                            handle = alGenSources();//alGenSources(1, handle);
                            if (alGetError() != AL_NO_ERROR) {
                                break;
                            } else {
                                // store in source array
                                this.openalSources[this.openalSourceCount] = new openalSource_t();
                                this.openalSources[this.openalSourceCount].handle = handle;
                                this.openalSources[this.openalSourceCount].startTime = 0;
                                this.openalSources[this.openalSourceCount].chan = null;
                                this.openalSources[this.openalSourceCount].inUse = false;
                                this.openalSources[this.openalSourceCount].looping = false;

                                // initialise sources
                                alSourcef(handle, AL_ROLLOFF_FACTOR, 0.0f);

                                // found one source
                                this.openalSourceCount++;
                            }
                        }

                        common.Printf("OpenAL: found %s\n", alcGetString(this.openalDevice, ALC_DEVICE_SPECIFIER));
                        common.Printf("OpenAL: found %d hardware voices\n", this.openalSourceCount);

                        // adjust source count to allow for at least eight stereo sounds to play
                        this.openalSourceCount -= 8;

                        EAXAvailable = 1;
                    }
                }
            }

            useOpenAL = idSoundSystemLocal.s_useOpenAL.GetBool();
            useEAXReverb = idSoundSystemLocal.s_useEAXReverb.GetBool();

            cmdSystem.AddCommand("listSounds", ListSounds_f.INSTANCE, CMD_FL_SOUND, "lists all sounds");
            cmdSystem.AddCommand("listSoundDecoders", ListSoundDecoders_f.INSTANCE, CMD_FL_SOUND, "list active sound decoders");
            cmdSystem.AddCommand("reloadSounds", SoundReloadSounds_f.INSTANCE, CMD_FL_SOUND | CMD_FL_CHEAT, "reloads all sounds");
            cmdSystem.AddCommand("testSound", TestSound_f.INSTANCE, CMD_FL_SOUND | CMD_FL_CHEAT, "tests a sound", idCmdSystem.ArgCompletion_SoundName.getInstance());
            cmdSystem.AddCommand("s_restart", SoundSystemRestart_f.INSTANCE, CMD_FL_SOUND, "restarts the sound system");

            common.Printf("sound system initialized.\n");
            common.Printf("--------------------------------------\n");
        }

        // shutdown routine
        @Override
        public void Shutdown() {
            ShutdownHW();

            // EAX or not, the list needs to be cleared
            this.EFXDatabase.Clear();

            // destroy openal sources
            if (useOpenAL) {

                this.efxloaded = false;

                // adjust source count back up to allow for freeing of all resources
                this.openalSourceCount += 8;

                for (int/*ALsizei*/ i = 0; i < this.openalSourceCount; i++) {
                    // stop source
                    alSourceStop(this.openalSources[i].handle);
                    alSourcei(this.openalSources[i].handle, AL_BUFFER, 0);

                    // delete source
//                    alDeleteSources(1, openalSources[i].handle);
                    alDeleteSources(this.openalSources[i].handle);

                    // clear entry in source array
                    this.openalSources[i].handle = 0;
                    this.openalSources[i].startTime = 0;
                    this.openalSources[i].chan = null;
                    this.openalSources[i].inUse = false;
                    this.openalSources[i].looping = false;

                }
            }

            // destroy all the sounds (hardware buffers as well)
//	delete soundCache;
            this.soundCache = null;

            // destroy openal device and context
            if (useOpenAL) {
                alcMakeContextCurrent(this.openalContext);

                alcDestroyContext(this.openalContext);
                this.openalContext = 0;

                alcCloseDevice(this.openalDevice);
                this.openalDevice = 0;
            }

            Sys_FreeOpenAL();

            idSampleDecoder.Shutdown();
        }

        @Override
        public void ClearBuffer() {

            // check to make sure hardware actually exists
            if (NOT(this.snd_audio_hw)) {
                return;
            }

            final short[] fBlock = {0};
            final long /*ulong*/ fBlockLen = 0;

            //TODO:see what this block does.
//            if (!snd_audio_hw.Lock( /*(void **)*/fBlock, fBlockLen)) {
//                return;
//            }
            if (fBlock[0] != 0) {
//                SIMDProcessor.Memset(fBlock, 0, fBlockLen);
                Arrays.fill(fBlock, 0, (int) fBlockLen, (byte) 0);
//                snd_audio_hw.Unlock(fBlock, fBlockLen);
            }
        }

        // sound is attached to the window, and must be recreated when the window is changed
        @Override
        public boolean ShutdownHW() {
            if (!this.isInitialized) {
                return false;
            }

            this.shutdown = true;		// don't do anything at AsyncUpdate() time
            Sys_Sleep(100);		// sleep long enough to make sure any async sound talking to hardware has returned

            common.Printf("Shutting down sound hardware\n");

//	delete snd_audio_hw;
            this.snd_audio_hw = null;

            this.isInitialized = false;

            if (this.graph != null) {
//                Mem_Free(graph);//TODO:remove all this memory crap.
                this.graph = null;
            }

            return true;
        }

        @Override
        public boolean InitHW() {

            if (s_noSound.GetBool()) {
                return false;
            }

//	delete snd_audio_hw;
            this.snd_audio_hw = idAudioHardware.Alloc();

            if (this.snd_audio_hw == null) {
                return false;
            }

            if (!useOpenAL) {
                if (!this.snd_audio_hw.Initialize()) {
//			delete snd_audio_hw;
                    this.snd_audio_hw = null;
                    return false;
                }

                if (this.snd_audio_hw.GetNumberOfSpeakers() == 0) {
                    return false;
                }
                // put the real number in there
                s_numberOfSpeakers.SetInteger(this.snd_audio_hw.GetNumberOfSpeakers());
            }

            this.isInitialized = true;
            this.shutdown = false;

            return true;
        }

        /*
         ===================
         idSoundSystemLocal::AsyncUpdate
         called from async sound thread when com_asyncSound == 1 ( Windows )
         ===================
         */
        // async loop, called at 60Hz
        @Override
        public int AsyncUpdate(int time) {

            if (!this.isInitialized || this.shutdown || NOT(this.snd_audio_hw)) {
                return 0;
            }

            long/*ulong*/ dwCurrentWritePos = 0;
            int/*dword*/ dwCurrentBlock;

            // If not using openal, get actual playback position from sound hardware
            if (useOpenAL) {
                // here we do it in samples ( overflows in 27 hours or so )
                dwCurrentWritePos = idMath.Ftol(Sys_Milliseconds() * 44.1f) % (MIXBUFFER_SAMPLES * ROOM_SLICES_IN_BUFFER);
                dwCurrentBlock = (int) (dwCurrentWritePos / MIXBUFFER_SAMPLES);
            } else {
                // and here in bytes
                // get the current byte position in the buffer where the sound hardware is currently reading
                if (!this.snd_audio_hw.GetCurrentPosition(dwCurrentWritePos)) {
                    return 0;
                }
                // mixBufferSize is in bytes
                dwCurrentBlock = (int) (dwCurrentWritePos / this.snd_audio_hw.GetMixBufferSize());
            }

            if (this.nextWriteBlock == 0xffffffff) {
                this.nextWriteBlock = dwCurrentBlock;
            }

            if (dwCurrentBlock != this.nextWriteBlock) {
                return 0;
            }

            // lock the buffer so we can actually write to it
            final short[] fBlock = null;
            final long/*ulong*/ fBlockLen = 0;
            if (!useOpenAL) {
                this.snd_audio_hw.Lock( /*(void **)*/fBlock, fBlockLen);
                if (null == fBlock) {
                    return 0;
                }
            }

            int j;
            this.soundStats.runs++;
            this.soundStats.activeSounds = 0;

            final int numSpeakers = this.snd_audio_hw.GetNumberOfSpeakers();

            this.nextWriteBlock++;
            this.nextWriteBlock %= ROOM_SLICES_IN_BUFFER;

            final int newPosition = this.nextWriteBlock * MIXBUFFER_SAMPLES;

            if (newPosition < this.olddwCurrentWritePos) {
                this.buffers++;					// buffer wrapped
            }

            // nextWriteSample is in multi-channel samples inside the buffer
            final int nextWriteSamples = this.nextWriteBlock * MIXBUFFER_SAMPLES;

            this.olddwCurrentWritePos = newPosition;

            // newSoundTime is in multi-channel samples since the sound system was started
            final int newSoundTime = (this.buffers * MIXBUFFER_SAMPLES * ROOM_SLICES_IN_BUFFER) + nextWriteSamples;

            // check for impending overflow
            // FIXME: we don't handle sound wrap-around correctly yet
            if (newSoundTime > 0x6fffffff) {
                this.buffers = 0;
            }

            if ((newSoundTime - this.CurrentSoundTime) > MIXBUFFER_SAMPLES) {
                this.soundStats.missedWindow++;
            }

            if (useOpenAL) {
                // enable audio hardware caching
                alcSuspendContext(this.openalContext);
            } else {
                // clear the buffer for all the mixing output
//                SIMDProcessor.Memset(finalMixBuffer, 0, MIXBUFFER_SAMPLES * sizeof(float) * numSpeakers);
                Arrays.fill(this.finalMixBuffer, 0, 0, MIXBUFFER_SAMPLES * numSpeakers);
            }

            // let the active sound world mix all the channels in unless muted or avi demo recording
            if (!this.muted && (this.currentSoundWorld != null) && (null == this.currentSoundWorld.fpa[0])) {
                this.currentSoundWorld.MixLoop(newSoundTime, numSpeakers, this.finalMixBuffer);
            }

            if (useOpenAL) {
                // disable audio hardware caching (this updates ALL settings since last alcSuspendContext)
                alcProcessContext(this.openalContext);
            } else {
//                short[] dest = fBlock + nextWriteSamples * numSpeakers;
                final int dest = nextWriteSamples * numSpeakers;

                SIMDProcessor.MixedSoundToSamples(fBlock, dest, this.finalMixBuffer, MIXBUFFER_SAMPLES * numSpeakers);

                // allow swapping the left / right speaker channels for people with miswired systems
                if ((numSpeakers == 2) && s_reverse.GetBool()) {
                    for (j = 0; j < MIXBUFFER_SAMPLES; j++) {
                        final short temp = fBlock[dest + (j * 2)];
                        fBlock[dest + (j * 2)] = fBlock[dest + (j * 2) + 1];
                        fBlock[dest + (j * 2) + 1] = temp;
                    }
                }
                this.snd_audio_hw.Unlock(fBlock, fBlockLen);
            }

            this.CurrentSoundTime = newSoundTime;

            this.soundStats.timeinprocess = Sys_Milliseconds() - time;

            return this.soundStats.timeinprocess;
        }

        /*
         ===================
         idSoundSystemLocal::AsyncUpdateWrite
         sound output using a write API. all the scheduling based on time
         we mix MIXBUFFER_SAMPLES at a time, but we feed the audio device with smaller chunks (and more often)
         called by the sound thread when com_asyncSound is 3 ( Linux )
         ===================
         */
        // async loop, when the sound driver uses a write strategy
        @Override
        public int AsyncUpdateWrite(int inTime) {

            if (!this.isInitialized || this.shutdown || NOT(this.snd_audio_hw)) {
                return 0;
            }

            if (!useOpenAL) {
                this.snd_audio_hw.Flush();
            }

            final long/*unsigned int*/ dwCurrentBlock = (/*unsigned int*/long) ((inTime * 44.1f) / MIXBUFFER_SAMPLES);

            if (this.nextWriteBlock == 0xffffffff) {
                this.nextWriteBlock = (int) dwCurrentBlock;
            }

            if (dwCurrentBlock < this.nextWriteBlock) {
                return 0;
            }

            if (this.nextWriteBlock != dwCurrentBlock) {
                Sys_Printf("missed %d sound updates\n", dwCurrentBlock - this.nextWriteBlock);
            }

            final int sampleTime = (int) (dwCurrentBlock * MIXBUFFER_SAMPLES);
            final int numSpeakers = this.snd_audio_hw.GetNumberOfSpeakers();

            if (useOpenAL) {
                // enable audio hardware caching
                alcSuspendContext(this.openalContext);
            } else {
                // clear the buffer for all the mixing output
//                SIMDProcessor.Memset(finalMixBuffer, 0, MIXBUFFER_SAMPLES * sizeof(float) * numSpeakers);
                Arrays.fill(this.finalMixBuffer, 0);
            }

            // let the active sound world mix all the channels in unless muted or avi demo recording
            if (!this.muted && (this.currentSoundWorld != null) && (null == this.currentSoundWorld.fpa[0])) {
                this.currentSoundWorld.MixLoop(sampleTime, numSpeakers, this.finalMixBuffer);
            }

            if (useOpenAL) {
                // disable audio hardware caching (this updates ALL settings since last alcSuspendContext)
                alcProcessContext(this.openalContext);
            } else {
                final short[] dest = this.snd_audio_hw.GetMixBuffer();

                SIMDProcessor.MixedSoundToSamples(dest, this.finalMixBuffer, MIXBUFFER_SAMPLES * numSpeakers);

                // allow swapping the left / right speaker channels for people with miswired systems
                if ((numSpeakers == 2) && s_reverse.GetBool()) {
                    int j;
                    for (j = 0; j < MIXBUFFER_SAMPLES; j++) {
                        final short temp = dest[j * 2];
                        dest[j * 2] = dest[(j * 2) + 1];
                        dest[(j * 2) + 1] = temp;
                    }
                }
                this.snd_audio_hw.Write(false);
            }

            // only move to the next block if the write was successful
            this.nextWriteBlock = (int) (dwCurrentBlock + 1);
            this.CurrentSoundTime = sampleTime;

            return Sys_Milliseconds() - inTime;
        }

        /*
         ===================
         idSoundSystemLocal::AsyncMix
         Mac OSX version. The system uses it's own thread and an IOProc callback
         ===================
         */// direct mixing called from the sound driver thread for OSes that support it
        @Override
        public int AsyncMix(int soundTime, float[] mixBuffer) {
            int inTime, numSpeakers;

            if (!this.isInitialized || this.shutdown || NOT(this.snd_audio_hw)) {
                return 0;
            }

            inTime = Sys_Milliseconds();
            numSpeakers = this.snd_audio_hw.GetNumberOfSpeakers();

            // let the active sound world mix all the channels in unless muted or avi demo recording
            if (!this.muted && (this.currentSoundWorld != null) && (null == this.currentSoundWorld.fpa[0])) {
                this.currentSoundWorld.MixLoop(soundTime, numSpeakers, mixBuffer);
            }

            this.CurrentSoundTime = soundTime;

            return Sys_Milliseconds() - inTime;
        }

        @Override
        public void SetMute(boolean muteOn) {
            this.muted = muteOn;
        }

        @Override
        public cinData_t ImageForTime(final int milliseconds, final boolean waveform) {
            final cinData_t ret = new cinData_t();
            int i, j;

            if (!this.isInitialized || NOT(this.snd_audio_hw)) {
//		memset( &ret, 0, sizeof( ret ) );
                return ret;
            }

            Sys_EnterCriticalSection();

            if (null == this.graph) {
                this.graph = new int[256 * 128 * 4];// Mem_Alloc(256 * 128 * 4);
            }
//	memset( graph, 0, 256*128 * 4 );
            final float[] accum = this.finalMixBuffer;	// unfortunately, these are already clamped
            final int time = Sys_Milliseconds();

            final int numSpeakers = this.snd_audio_hw.GetNumberOfSpeakers();

            if (!waveform) {
                for (j = 0; j < numSpeakers; j++) {
                    int meter = 0;
                    for (i = 0; i < MIXBUFFER_SAMPLES; i++) {
                        final float result = idMath.Fabs(accum[(i * numSpeakers) + j]);
                        if (result > meter) {
                            meter = (int) result;
                        }
                    }

                    meter /= 256;		// 32768 becomes 128
                    if (meter > 128) {
                        meter = 128;
                    }
                    int offset;
                    int xsize;
                    if (numSpeakers == 6) {
                        offset = j * 40;
                        xsize = 20;
                    } else {
                        offset = j * 128;
                        xsize = 63;
                    }
                    int x, y;
                    final int/*dword*/ color = 0xff00ff00;
                    for (y = 0; y < 128; y++) {
                        for (x = 0; x < xsize; x++) {
                            this.graph[((127 - y) * 256) + offset + x] = color;
                        }
// #if 0
                        // if ( y == 80 ) {
                        // color = 0xff00ffff;
                        // } else if ( y == 112 ) {
                        // color = 0xff0000ff;
                        // }
// #endif
                        if (y > meter) {
                            break;
                        }
                    }

                    if (meter > this.meterTops[j]) {
                        this.meterTops[j] = meter;
                        this.meterTopsTime[j] = time + s_meterTopTime.GetInteger();
                    } else if ((time > this.meterTopsTime[j]) && (this.meterTops[j] > 0)) {
                        this.meterTops[j]--;
                        if (this.meterTops[j] != 0) {
                            this.meterTops[j]--;
                        }
                    }
                }

                for (j = 0; j < numSpeakers; j++) {
                    final int meter = this.meterTops[j];

                    int offset;
                    int xsize;
                    if (numSpeakers == 6) {
                        offset = j * 40;
                        xsize = 20;
                    } else {
                        offset = j * 128;
                        xsize = 63;
                    }
                    int x, y;
                    int/*dword*/ color;
                    if (meter <= 80) {
                        color = 0xff007f00;
                    } else if (meter <= 112) {
                        color = 0xff007f7f;
                    } else {
                        color = 0xff00007f;
                    }
                    for (y = meter; (y < 128) && (y < (meter + 4)); y++) {
                        for (x = 0; x < xsize; x++) {
                            this.graph[((127 - y) * 256) + offset + x] = color;
                        }
                    }
                }
            } else {
                final int/*dword*/[] colors = {0xff007f00, 0xff007f7f, 0xff00007f, 0xff00ff00, 0xff00ffff, 0xff0000ff};

                for (j = 0; j < numSpeakers; j++) {
                    int xx = 0;
                    float fmeter;
                    final int step = MIXBUFFER_SAMPLES / 256;
                    for (i = 0; i < MIXBUFFER_SAMPLES; i += step) {
                        fmeter = 0.0f;
                        for (int x = 0; x < step; x++) {
                            float result = accum[((i + x) * numSpeakers) + j];
                            result = result / 32768.0f;
                            fmeter += result;
                        }
                        fmeter /= 4.0f;
                        if (fmeter < -1.0f) {
                            fmeter = -1.0f;
                        } else if (fmeter > 1.0f) {
                            fmeter = 1.0f;
                        }
                        int meter = (int) (fmeter * 63.0f);
                        this.graph[ ((meter + 64) * 256) + xx] = colors[j];

                        if (meter < 0) {
                            meter = -meter;
                        }
                        if (meter > this.meterTops[xx]) {
                            this.meterTops[xx] = meter;
                            this.meterTopsTime[xx] = time + 100;
                        } else if ((time > this.meterTopsTime[xx]) && (this.meterTops[xx] > 0)) {
                            this.meterTops[xx]--;
                            if (this.meterTops[xx] != 0) {
                                this.meterTops[xx]--;
                            }
                        }
                        xx++;
                    }
                }
                for (i = 0; i < 256; i++) {
                    final int meter = this.meterTops[i];
                    for (int y = -meter; y < meter; y++) {
                        this.graph[ ((y + 64) * 256) + i] = colors[j];
                    }
                }
            }
            ret.imageHeight = 128;
            ret.imageWidth = 256;

            final ByteBuffer image = BufferUtils.createByteBuffer(this.graph.length * 4);
            image.asIntBuffer().put(this.graph);
            ret.image = image;

            Sys_LeaveCriticalSection();

            return ret;
        }

        @Override
        public int GetSoundDecoderInfo(int index, soundDecoderInfo_t decoderInfo) {
            int i, j, firstEmitter, firstChannel;
            final idSoundWorldLocal sw = soundSystemLocal.currentSoundWorld;

            if (index < 0) {
                firstEmitter = 0;
                firstChannel = 0;
            } else {
                firstEmitter = index / SOUND_MAX_CHANNELS;
                firstChannel = (index - (firstEmitter * SOUND_MAX_CHANNELS)) + 1;
            }

            for (i = firstEmitter; i < sw.emitters.Num(); i++) {
                final idSoundEmitterLocal sound = sw.emitters.oGet(i);

                if (null == sound) {
                    continue;
                }

                // run through all the channels
                for (j = firstChannel; j < SOUND_MAX_CHANNELS; j++) {
                    final idSoundChannel chan = sound.channels[j];

                    if (chan.decoder == null) {
                        continue;
                    }

                    final idSoundSample sample = chan.decoder.GetSample();

                    if (sample == null) {
                        continue;
                    }

                    decoderInfo.name = sample.name;
                    decoderInfo.format.oSet((sample.objectInfo.wFormatTag == WAVE_FORMAT_TAG_OGG) ? "OGG" : "WAV");
                    decoderInfo.numChannels = sample.objectInfo.nChannels;
                    decoderInfo.numSamplesPerSecond = sample.objectInfo.nSamplesPerSec;
                    decoderInfo.num44kHzSamples = sample.LengthIn44kHzSamples();
                    decoderInfo.numBytes = sample.objectMemSize;
                    decoderInfo.looping = (chan.parms.soundShaderFlags & SSF_LOOPING) != 0;
                    decoderInfo.lastVolume = chan.lastVolume;
                    decoderInfo.start44kHzTime = chan.trigger44kHzTime;
                    decoderInfo.current44kHzTime = soundSystemLocal.GetCurrent44kHzTime();

                    return ((i * SOUND_MAX_CHANNELS) + j);
                }

                firstChannel = 0;
            }
            return -1;
        }

        // if rw == NULL, no portal occlusion or rendered debugging is available
        @Override
        public idSoundWorld AllocSoundWorld(idRenderWorld rw) {
            final idSoundWorldLocal local = new idSoundWorldLocal();

            local.Init(rw);

            return local;
        }

        /*
         ===================
         idSoundSystemLocal::SetPlayingSoundWorld

         specifying NULL will cause silence to be played
         ===================
         */
        // specifying NULL will cause silence to be played
        @Override
        public void SetPlayingSoundWorld(idSoundWorld soundWorld) {
            this.currentSoundWorld = (idSoundWorldLocal) soundWorld;
        }

        // some tools, like the sound dialog, may be used in both the game and the editor
        // This can return NULL, so check!
        @Override
        public idSoundWorld GetPlayingSoundWorld() {
            return this.currentSoundWorld;
        }

        @Override
        public void BeginLevelLoad() {
            if (!this.isInitialized) {
                return;
            }
            this.soundCache.BeginLevelLoad();

            if (this.efxloaded) {
                this.EFXDatabase.UnloadFile();
                this.efxloaded = false;
            }
        }

        @Override
        public void EndLevelLoad(final String mapString) {
            if (!this.isInitialized) {
                return;
            }
            this.soundCache.EndLevelLoad();

            final idStr efxname = new idStr("efxs/");
            final idStr mapname = new idStr(mapString);

            mapname.SetFileExtension(".efx");
            mapname.StripPath();
            efxname.oPluSet(mapname);

            this.efxloaded = this.EFXDatabase.LoadFile(efxname.getData());

            if (this.efxloaded) {
                common.Printf("sound: found %s\n", efxname);
            } else {
                common.Printf("sound: missing %s\n", efxname);
            }
        }

        @Override
        public void PrintMemInfo(MemInfo_t mi) {
            this.soundCache.PrintMemInfo(mi);
        }

        @Override
        public int IsEAXAvailable() {
//#if !ID_OPENAL
            return -1;
//#else
//	ALCdevice	*device;
//	ALCcontext	*context;
//
//	if ( EAXAvailable != -1 ) {
//		return EAXAvailable;
//	}
//
//	if ( !Sys_LoadOpenAL() ) {
//		EAXAvailable = 2;
//		return 2;
//	}
//	// when dynamically loading the OpenAL subsystem, we need to get a context before alIsExtensionPresent would work
//	device = alcOpenDevice( NULL );
//	context = alcCreateContext( device, NULL );
//	alcMakeContextCurrent( context );
//	if ( alIsExtensionPresent( ID_ALCHAR "EAX4.0" ) ) {
//		alcMakeContextCurrent( NULL );
//		alcDestroyContext( context );
//		alcCloseDevice( device );
//		EAXAvailable = 1;
//		return 1;
//	}
//	alcMakeContextCurrent( NULL );
//	alcDestroyContext( context );
//	alcCloseDevice( device );
//	EAXAvailable = 0;
//	return 0;
//#endif
        }

        //-------------------------
        public int GetCurrent44kHzTime() {
            if (this.snd_audio_hw != null) {
                return this.CurrentSoundTime;
            } else {
                // NOTE: this would overflow 31bits within about 1h20 ( not that important since we get a snd_audio_hw right away pbly )
                //return ( ( Sys_Milliseconds()*441 ) / 10 ) * 4; 
                return idMath.FtoiFast(Sys_Milliseconds() * 176.4f);
            }
        }

        public float dB2Scale(final float val) {
            if (val == 0.0f) {
                return 1.0f;				// most common
            } else if (val <= -60.0f) {
                return 0.0f;
            } else if (val >= 60.0f) {
                return (float) pow(2.0f, val * (1.0f / 6.0f));
            }
            final int ival = (int) ((val + 60.0f) * 10.0f);
            return this.volumesDB[ival];
        }

        public int SamplesToMilliseconds(int samples) {
            return (samples / (PRIMARYFREQ / 1000));
        }

        public int MillisecondsToSamples(int ms) {
            return (ms * (PRIMARYFREQ / 1000));
        }

        public void DoEnviroSuit(float[] samples, int numSamples, int numSpeakers) {
            float[] out;
            final int out_p = 2;
            float[] in;
            final int in_p = 2;

            assert (!idSoundSystemLocal.useOpenAL);

            if (0 == this.fxList.Num()) {
                for (int i = 0; i < 6; i++) {
                    SoundFX fx;

                    // lowpass filter
                    fx = new SoundFX_Lowpass();
                    fx.SetChannel(i);
                    this.fxList.Append(fx);

                    // comb
                    fx = new SoundFX_Comb();
                    fx.SetChannel(i);
                    fx.SetParameter(i * 100);
                    this.fxList.Append(fx);

                    // comb
                    fx = new SoundFX_Comb();
                    fx.SetChannel(i);
                    fx.SetParameter((i * 100) + 5);
                    this.fxList.Append(fx);
                }
            }

            for (int i = 0; i < numSpeakers; i++) {
                int j;

                // restore previous samples
//		memset( in, 0, 10000 * sizeof( float ) );
                out = new float[10000];
//		memset( out, 0, 10000 * sizeof( float ) );
                in = new float[10000];

                // fx loop
                for (int k = 0; k < this.fxList.Num(); k++) {
                    final SoundFX fx = this.fxList.oGet(k);

                    // skip if we're not the right channel
                    if (fx.GetChannel() != i) {
                        continue;
                    }

                    // get samples and continuity
                    {
                        final float[] in1 = {0}, in2 = {0}, out1 = {0}, out2 = {0};
                        fx.GetContinuitySamples(in1, in2, out1, out2);
                        in[in_p - 1] = in1[0];
                        in[in_p - 2] = in2[0];
                        out[out_p - 1] = out1[0];
                        out[out_p - 2] = out2[0];
                    }
                    for (j = 0; j < numSamples; j++) {
                        in[in_p + j] = samples[(j * numSpeakers) + i] * s_enviroSuitVolumeScale.GetFloat();
                    }

                    // process fx loop
                    for (j = 0; j < numSamples; j++) {
                        fx.ProcessSample(in, in_p + j, out, out_p + j);//TODO:float[], int index, float[], int index
                    }

                    // store samples and continuity
                    fx.SetContinuitySamples(in[(in_p + numSamples) - 2], in[(in_p + numSamples) - 3], out[(out_p + numSamples) - 2], out[(out_p + numSamples) - 3]);

                    for (j = 0; j < numSamples; j++) {
                        samples[(j * numSpeakers) + i] = out[out_p + j];
                    }
                }
            }
        }

        public int/*ALuint*/ AllocOpenALSource(idSoundChannel chan, boolean looping, boolean stereo) {
            int timeOldestZeroVolSingleShot = Sys_Milliseconds();
            int timeOldestZeroVolLooping = Sys_Milliseconds();
            int timeOldestSingle = Sys_Milliseconds();
            int iOldestZeroVolSingleShot = -1;
            int iOldestZeroVolLooping = -1;
            int iOldestSingle = -1;
            int iUnused = -1;
            int index = -1;
            int/*ALsizei*/ i;

            // Grab current msec time
            final int time = Sys_Milliseconds();

            // Cycle through all sources
            for (i = 0; i < this.openalSourceCount; i++) {
                // Use any unused source first,
                // Then find oldest single shot quiet source,
                // Then find oldest looping quiet source and
                // Lastly find oldest single shot non quiet source..
                if (!this.openalSources[i].inUse) {
                    iUnused = i;
                    break;
                } else if (!this.openalSources[i].looping && (this.openalSources[i].chan.lastVolume < SND_EPSILON)) {
                    if (this.openalSources[i].startTime < timeOldestZeroVolSingleShot) {
                        timeOldestZeroVolSingleShot = this.openalSources[i].startTime;
                        iOldestZeroVolSingleShot = i;
                    }
                } else if (this.openalSources[i].looping && (this.openalSources[i].chan.lastVolume < SND_EPSILON)) {
                    if (this.openalSources[i].startTime < timeOldestZeroVolLooping) {
                        timeOldestZeroVolLooping = this.openalSources[i].startTime;
                        iOldestZeroVolLooping = i;
                    }
                } else if (!this.openalSources[i].looping) {
                    if (this.openalSources[i].startTime < timeOldestSingle) {
                        timeOldestSingle = this.openalSources[i].startTime;
                        iOldestSingle = i;
                    }
                }
            }

            if (iUnused != -1) {
                index = iUnused;
            } else if (iOldestZeroVolSingleShot != - 1) {
                index = iOldestZeroVolSingleShot;
            } else if (iOldestZeroVolLooping != -1) {
                index = iOldestZeroVolLooping;
            } else if (iOldestSingle != -1) {
                index = iOldestSingle;
            }

            if (index != -1) {
                // stop the channel that is being ripped off
                if (this.openalSources[index].chan != null) {
                    // stop the channel only when not looping
                    if (!this.openalSources[index].looping) {
                        this.openalSources[index].chan.Stop();
                    } else {
                        this.openalSources[index].chan.triggered = true;
                    }

                    // Free hardware resources
                    this.openalSources[index].chan.ALStop();
                }

                // Initialize structure
                this.openalSources[index].startTime = time;
                this.openalSources[index].chan = chan;
                this.openalSources[index].inUse = true;
                this.openalSources[index].looping = looping;
                this.openalSources[index].stereo = stereo;

                return this.openalSources[index].handle;
            } else {
                return 0;
            }
        }

        public void FreeOpenALSource(int/*ALuint*/ handle) {
            int/*ALsizei*/ i;
            for (i = 0; i < this.openalSourceCount; i++) {
                if (this.openalSources[i].handle == handle) {
                    if (this.openalSources[i].chan != null) {
                        this.openalSources[i].chan.openalSource = 0;
                    }
// #if ID_OPENAL
                    // // Reset source EAX ROOM level when freeing stereo source
                    // if ( openalSources[i].stereo && alEAXSet ) {
                    // long Room = EAXSOURCE_DEFAULTROOM;
                    // alEAXSet( &EAXPROPERTYID_EAX_Source, EAXSOURCE_ROOM, openalSources[i].handle, &Room, sizeof(Room));
                    // }
// #endif
                    // Initialize structure
                    this.openalSources[i].startTime = 0;
                    this.openalSources[i].chan = null;
                    this.openalSources[i].inUse = false;
                    this.openalSources[i].looping = false;
                    this.openalSources[i].stereo = false;
                }
            }
        }
    }

    /*
     ===============
     SoundReloadSounds_f

     this is called from the main thread
     ===============
     */
    static class SoundReloadSounds_f extends cmdFunction_t {

        public static final cmdFunction_t INSTANCE = new SoundReloadSounds_f();

        private SoundReloadSounds_f() {
        }

        @Override
        public void run(idCmdArgs args) {
            if (NOT(soundSystemLocal.soundCache)) {
                return;
            }
            boolean force = false;
            if (args.Argc() == 2) {
                force = true;
            }
            soundSystem.SetMute(true);
            soundSystemLocal.soundCache.ReloadSounds(force);
            soundSystem.SetMute(false);
            common.Printf("sound: changed sounds reloaded\n");
        }
    }

    /*
     ===============
     ListSounds_f

     Optional parameter to only list sounds containing that string
     ===============
     */
    static class ListSounds_f extends cmdFunction_t {

        public static final cmdFunction_t INSTANCE = new ListSounds_f();

        private ListSounds_f() {
        }

        @Override
        public void run(idCmdArgs args) {
            int i;
            final String snd = args.Argv(1);

            if (NOT(soundSystemLocal.soundCache)) {
                common.Printf("No sound.\n");
                return;
            }

            int totalSounds = 0;
            int totalSamples = 0;
            int totalMemory = 0;
            int totalPCMMemory = 0;
            for (i = 0; i < soundSystemLocal.soundCache.GetNumObjects(); i++) {
                final idSoundSample sample = soundSystemLocal.soundCache.GetObject(i);
                if (NOT(sample)) {
                    continue;
                }
                if ((snd != null) && (sample.name.Find(snd, false) < 0)) {
                    continue;
                }

                final waveformatex_s info = sample.objectInfo;

                final String stereo = (info.nChannels == 2 ? "ST" : "  ");
                final String format = (info.wFormatTag == WAVE_FORMAT_TAG_OGG) ? "OGG" : "WAV";
                final String defaulted = (sample.defaultSound ? "(DEFAULTED)" : sample.purged ? "(PURGED)" : "");

                common.Printf("%s %dkHz %6dms %5dkB %4s %s%s\n", stereo, sample.objectInfo.nSamplesPerSec / 1000,
                        soundSystemLocal.SamplesToMilliseconds(sample.LengthIn44kHzSamples()),
                        sample.objectMemSize >> 10, format, sample.name, defaulted);

                if (!sample.purged) {
                    totalSamples += sample.objectSize;
                    if (info.wFormatTag != WAVE_FORMAT_TAG_OGG) {
                        totalPCMMemory += sample.objectMemSize;
                    }
                    if (!sample.hardwareBuffer) {
                        totalMemory += sample.objectMemSize;
                    }
                }
                totalSounds++;
            }
            common.Printf("%8d total sounds\n", totalSounds);
            common.Printf("%8d total samples loaded\n", totalSamples);
            common.Printf("%8d kB total system memory used\n", totalMemory >> 10);
//#if ID_OPENAL
//	common.Printf( "%8d kB total OpenAL audio memory used\n", ( alGetInteger( alGetEnumValue( "AL_EAX_RAM_SIZE" ) ) - alGetInteger( alGetEnumValue( "AL_EAX_RAM_FREE" ) ) ) >> 10 );
//#endif
        }
    }

    /*
     ===============
     ListSoundDecoders_f
     ===============
     */
    static class ListSoundDecoders_f extends cmdFunction_t {

        public static final cmdFunction_t INSTANCE = new ListSoundDecoders_f();

        private ListSoundDecoders_f() {
        }

        @Override
        public void run(idCmdArgs args) {
            int i, j, numActiveDecoders, numWaitingDecoders;
            final idSoundWorldLocal sw = soundSystemLocal.currentSoundWorld;

            numActiveDecoders = numWaitingDecoders = 0;

            for (i = 0; i < sw.emitters.Num(); i++) {
                final idSoundEmitterLocal sound = sw.emitters.oGet(i);

                if (NOT(sound)) {
                    continue;
                }

                // run through all the channels
                for (j = 0; j < SOUND_MAX_CHANNELS; j++) {
                    final idSoundChannel chan = sound.channels[j];

                    if (chan.decoder == null) {
                        continue;
                    }

                    final idSoundSample sample = chan.decoder.GetSample();

                    if (sample != null) {
                        continue;
                    }

                    final String format = (chan.leadinSample.objectInfo.wFormatTag == WAVE_FORMAT_TAG_OGG) ? "OGG" : "WAV";
                    common.Printf("%3d waiting %s: %s\n", numWaitingDecoders, format, chan.leadinSample.name);

                    numWaitingDecoders++;
                }
            }

            for (i = 0; i < sw.emitters.Num(); i++) {
                final idSoundEmitterLocal sound = sw.emitters.oGet(i);

                if (NOT(sound)) {
                    continue;
                }

                // run through all the channels
                for (j = 0; j < SOUND_MAX_CHANNELS; j++) {
                    final idSoundChannel chan = sound.channels[j];

                    if (chan.decoder == null) {
                        continue;
                    }

                    final idSoundSample sample = chan.decoder.GetSample();

                    if (sample == null) {
                        continue;
                    }

                    final String format = (sample.objectInfo.wFormatTag == WAVE_FORMAT_TAG_OGG) ? "OGG" : "WAV";

                    final int localTime = soundSystemLocal.GetCurrent44kHzTime() - chan.trigger44kHzTime;
                    final int sampleTime = sample.LengthIn44kHzSamples() * sample.objectInfo.nChannels;
                    int percent;
                    if (localTime > sampleTime) {
                        if ((chan.parms.soundShaderFlags & SSF_LOOPING) != 0) {
                            percent = ((localTime % sampleTime) * 100) / sampleTime;
                        } else {
                            percent = 100;
                        }
                    } else {
                        percent = (localTime * 100) / sampleTime;
                    }

                    common.Printf("%3d decoding %3d%% %s: %s\n", numActiveDecoders, percent, format, sample.name);

                    numActiveDecoders++;
                }
            }

            common.Printf("%d decoders\n", numWaitingDecoders + numActiveDecoders);
            common.Printf("%d waiting decoders\n", numWaitingDecoders);
            common.Printf("%d active decoders\n", numActiveDecoders);
            common.Printf("%d kB decoder memory in %d blocks\n", idSampleDecoder.GetUsedBlockMemory() >> 10, idSampleDecoder.GetNumUsedBlocks());
        }
    }

    /*
     ===============
     TestSound_f

     this is called from the main thread
     ===============
     */
    static class TestSound_f extends cmdFunction_t {

        public static final cmdFunction_t INSTANCE = new TestSound_f();

        private TestSound_f() {
        }

        @Override
        public void run(idCmdArgs args) {
            if (args.Argc() != 2) {
                common.Printf("Usage: testSound <file>\n");
                return;
            }
            if (soundSystemLocal.currentSoundWorld != null) {
                soundSystemLocal.currentSoundWorld.PlayShaderDirectly(args.Argv(1));
            }
        }
    }

    /*
     ===============
     SoundSystemRestart_f

     restart the sound thread

     this is called from the main thread
     ===============
     */
    static class SoundSystemRestart_f extends cmdFunction_t {

        public static final cmdFunction_t INSTANCE = new SoundSystemRestart_f();

        private SoundSystemRestart_f() {
        }

        @Override
        public void run(idCmdArgs args) {
            soundSystem.SetMute(true);
            soundSystemLocal.ShutdownHW();
            soundSystemLocal.InitHW();
            soundSystem.SetMute(false);
        }
    }

    public static void setSoundSystem(idSoundSystem soundSystem) {
        snd_system.soundSystem = snd_system.soundSystemLocal = (idSoundSystemLocal) soundSystem;
    }
}
