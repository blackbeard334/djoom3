package neo.Sound;

import neo.Renderer.Cinematic.cinData_t;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.snd_shader.soundShaderParms_t;
import neo.TempDump.SERiAL;
import neo.framework.Common.MemInfo_t;
import neo.framework.DemoFile.idDemoFile;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Vector.idVec3;

/**
 *
 */
public class sound {

    /*
     ===============================================================================

     SOUND EMITTER

     ===============================================================================
     */
    // sound channels
    static final int SCHANNEL_ANY = 0;	// used in queries and commands to effect every channel at once, in
    // startSound to have it not override any other channel
    static final int SCHANNEL_ONE = 1;	// any following integer can be used as a channel number
    // typedef int s_channelType;	// the game uses its own series of enums, and we don't want to require casts

    public static abstract class idSoundEmitter implements SERiAL {
        // virtual					~idSoundEmitter() {}

        // a non-immediate free will let all currently playing sounds complete
        // soundEmitters are not actually deleted, they are just marked as
        // reusable by the soundWorld
        public abstract void Free(boolean immediate);

        // the parms specified will be the default overrides for all sounds started on this emitter.
        // NULL is acceptable for parms
        public abstract void UpdateEmitter(final idVec3 origin, int listenerId, final soundShaderParms_t parms);

        // returns the length of the started sound in msec
        public abstract int StartSound(final idSoundShader shader, final int channel, float diversity /*= 0*/, int shaderFlags /*= 0*/, boolean allowSlow /*= true*/);

        public int StartSound(final idSoundShader shader, final int channel, float diversity /*= 0*/, int shaderFlags /*= 0*/) {
            return StartSound(shader, channel, diversity, shaderFlags, true);
        }

        // pass SCHANNEL_ANY to effect all channels
        public abstract void ModifySound(final int channel, final soundShaderParms_t parms);

        public abstract void StopSound(final int channel);

        // to is in Db (sigh), over is in seconds
        public abstract void FadeSound(final int channel, float to, float over);

        // returns true if there are any sounds playing from this emitter.  There is some conservative
        // slop at the end to remove inconsistent race conditions with the sound thread updates.
        // FIXME: network game: on a dedicated server, this will always be false
        public abstract boolean CurrentlyPlaying();

        // returns a 0.0 to 1.0 value based on the current sound amplitude, allowing
        // graphic effects to be modified in time with the audio.
        // just samples the raw wav file, it doesn't account for volume overrides in the
        public abstract float CurrentAmplitude();

        // for save games.  Index will always be > 0
        public abstract int Index();
    };

    /*
     ===============================================================================

     SOUND WORLD

     There can be multiple independent sound worlds, just as there can be multiple
     independent render worlds.  The prime example is the editor sound preview
     option existing simultaniously with a live game.
     ===============================================================================
     */
    public static abstract class idSoundWorld {
        // virtual					~idSoundWorld() {}

        // call at each map start
        public abstract void ClearAllSoundEmitters();

        public abstract void StopAllSounds();

        // get a new emitter that can play sounds in this world
        public abstract idSoundEmitter AllocSoundEmitter();

        // for load games, index 0 will return NULL
        public abstract idSoundEmitter EmitterForIndex(int index);

        // query sound samples from all emitters reaching a given position
        public abstract float CurrentShakeAmplitudeForPosition(final int time, final idVec3 listenerPosition);

        // where is the camera/microphone
        // listenerId allows listener-private and antiPrivate sounds to be filtered
        // gameTime is in msec, and is used to time sound queries and removals so that they are independent
        // of any race conditions with the async update
        public abstract void PlaceListener(final idVec3 origin, final idMat3 axis, final int listenerId, final int gameTime, final idStr areaName);

        public void PlaceListener(final idVec3 origin, final idMat3 axis, final int listenerId, final int gameTime, final String areaName) {
            PlaceListener(origin, axis, listenerId, gameTime, new idStr(areaName));
        }

        // fade all sounds in the world with a given shader soundClass
        // to is in Db (sigh), over is in seconds
        public abstract void FadeSoundClasses(final int soundClass, final float to, final float over);

        // background music
        public abstract void PlayShaderDirectly(final String name, int channel /*= -1*/);

        public void PlayShaderDirectly(final String name) {
            PlayShaderDirectly(name, -1);
        }

        // dumps the current state and begins archiving commands
        public abstract void StartWritingDemo(idDemoFile demo);

        public abstract void StopWritingDemo();

        // read a sound command from a demo file
        public abstract void ProcessDemoCommand(idDemoFile demo);

        // pause and unpause the sound world
        public abstract void Pause();

        public abstract void UnPause();

        public abstract boolean IsPaused();

        // Write the sound output to multiple wav files.  Note that this does not use the
        // work done by AsyncUpdate, it mixes explicitly in the foreground every PlaceOrigin(),
        // under the assumption that we are rendering out screenshots and the gameTime is going
        // much slower than real time.
        // path should not include an extension, and the generated filenames will be:
        // <path>_left.raw, <path>_right.raw, or <path>_51left.raw, <path>_51right.raw, 
        // <path>_51center.raw, <path>_51lfe.raw, <path>_51backleft.raw, <path>_51backright.raw, 
        // If only two channel mixing is enabled, the left and right .raw files will also be
        // combined into a stereo .wav file.
        public abstract void AVIOpen(final String path, final String name);

        public abstract void AVIClose();

        // SaveGame / demo Support
        public abstract void WriteToSaveGame(idFile savefile);

        public abstract void ReadFromSaveGame(idFile savefile);

        public abstract void SetSlowmo(boolean active);

        public abstract void SetSlowmoSpeed(float speed);

        public abstract void SetEnviroSuit(boolean active);
    };

    /*
     ===============================================================================

     SOUND SYSTEM

     ===============================================================================
     */
    public static class soundDecoderInfo_t {

        public idStr name;
        public idStr format;
        public int numChannels;
        public long numSamplesPerSecond;
        public int num44kHzSamples;
        public int numBytes;
        public boolean looping;
        public float lastVolume;
        public int start44kHzTime;
        public int current44kHzTime;
    };

    public static abstract class idSoundSystem {
        // virtual					~idSoundSystem( void ) {}

        // all non-hardware initialization
        public abstract void Init();

        // shutdown routine
        public abstract void Shutdown();

        // call ClearBuffer if there is a chance that the AsyncUpdate won't get called
        // for 20+ msec, which would cause a stuttering repeat of the current
        // buffer contents
        public abstract void ClearBuffer();

        // sound is attached to the window, and must be recreated when the window is changed
        public abstract boolean InitHW();

        public abstract boolean ShutdownHW();

        // asyn loop, called at 60Hz
        public abstract int AsyncUpdate(int time);

        // async loop, when the sound driver uses a write strategy
        public abstract int AsyncUpdateWrite(int time);

        // it is a good idea to mute everything when starting a new level,
        // because sounds may be started before a valid listener origin
        // is specified
        public abstract void SetMute(boolean mute);

        // for the sound level meter window
        public abstract cinData_t ImageForTime(final int milliseconds, final boolean waveform);

        // get sound decoder info
        public abstract int GetSoundDecoderInfo(int index, soundDecoderInfo_t decoderInfo);

        // if rw == NULL, no portal occlusion or rendered debugging is available
        public abstract idSoundWorld AllocSoundWorld(idRenderWorld rw);

        // specifying NULL will cause silence to be played
        public abstract void SetPlayingSoundWorld(idSoundWorld soundWorld);

        // some tools, like the sound dialog, may be used in both the game and the editor
        // This can return NULL, so check!
        public abstract idSoundWorld GetPlayingSoundWorld();

        // Mark all soundSamples as currently unused,
        // but don't free anything.
        public abstract void BeginLevelLoad();

        // Free all soundSamples marked as unused
        // We might want to defer the loading of new sounds to this point,
        // as we do with images, to avoid having a union in memory at one time.
        public abstract void EndLevelLoad(final String mapString);

        // direct mixing for OSes that support it
        public abstract int AsyncMix(int soundTime, float[] mixBuffer);

        // prints memory info
        public abstract void PrintMemInfo(MemInfo_t mi);

        // is EAX support present - -1: disabled at compile time, 0: no suitable hardware, 1: ok, 2: failed to load OpenAL DLL
        public abstract int IsEAXAvailable();
    };
}
