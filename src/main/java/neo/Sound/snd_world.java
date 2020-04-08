package neo.Sound;

import static java.lang.Math.atan;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_AIR;
import static neo.Renderer.RenderWorld.portalConnection_t.PS_BLOCK_VIEW;
import static neo.Sound.snd_emitter.REMOVE_STATUS_ALIVE;
import static neo.Sound.snd_emitter.REMOVE_STATUS_SAMPLEFINISHED;
import static neo.Sound.snd_local.SND_EPSILON;
import static neo.Sound.snd_local.SOUND_MAX_CHANNELS;
import static neo.Sound.snd_local.WAVE_FORMAT_TAG_PCM;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_ALLOC_EMITTER;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_PLACE_LISTENER;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_STATE;
import static neo.Sound.snd_shader.DOOM_TO_METERS;
import static neo.Sound.snd_shader.SOUND_MAX_CLASSES;
import static neo.Sound.snd_shader.SSF_ANTI_PRIVATE_SOUND;
import static neo.Sound.snd_shader.SSF_GLOBAL;
import static neo.Sound.snd_shader.SSF_LOOPING;
import static neo.Sound.snd_shader.SSF_NO_FLICKER;
import static neo.Sound.snd_shader.SSF_NO_OCCLUSION;
import static neo.Sound.snd_shader.SSF_OMNIDIRECTIONAL;
import static neo.Sound.snd_shader.SSF_PRIVATE_SOUND;
import static neo.Sound.snd_shader.SSF_UNCLAMPED;
import static neo.Sound.snd_system.soundSystemLocal;
import static neo.Sound.snd_system.idSoundSystemLocal.s_showLevelMeter;
import static neo.Sound.snd_wavefile.fourcc_riff;
import static neo.Sound.snd_wavefile.mmioFOURCC;
import static neo.Sound.sound.SCHANNEL_ANY;
import static neo.Sound.sound.SCHANNEL_ONE;
import static neo.TempDump.NOT;
import static neo.TempDump.etoi;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.framework.BuildDefines.MACOS_X;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DemoFile.demoSystem_t.DS_SOUND;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Session.session;
import static neo.idlib.Lib.colorRed;
import static neo.idlib.Text.Str.va;
import static neo.idlib.math.Math_h.DEG2RAD;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;
import static neo.idlib.math.Simd.SIMDProcessor;
import static neo.open.al.QAL.alBufferData;
import static neo.open.al.QAL.alDeleteBuffers;
import static neo.open.al.QAL.alGenBuffers;
import static neo.open.al.QAL.alGetSourcei;
import static neo.open.al.QAL.alIsSource;
import static neo.open.al.QAL.alListener3f;
import static neo.open.al.QAL.alListenerf;
import static neo.open.al.QAL.alListenerfv;
import static neo.open.al.QAL.alSource3f;
import static neo.open.al.QAL.alSourcePlay;
import static neo.open.al.QAL.alSourceQueueBuffers;
import static neo.open.al.QAL.alSourceStop;
import static neo.open.al.QAL.alSourceUnqueueBuffers;
import static neo.open.al.QAL.alSourcef;
import static neo.open.al.QAL.alSourcei;
import static neo.open.al.QALConstantsIfc.AL_BUFFER;
import static neo.open.al.QALConstantsIfc.AL_BUFFERS_PROCESSED;
import static neo.open.al.QALConstantsIfc.AL_FALSE;
import static neo.open.al.QALConstantsIfc.AL_FORMAT_MONO16;
import static neo.open.al.QALConstantsIfc.AL_FORMAT_STEREO16;
import static neo.open.al.QALConstantsIfc.AL_GAIN;
import static neo.open.al.QALConstantsIfc.AL_LOOPING;
import static neo.open.al.QALConstantsIfc.AL_MAX_DISTANCE;
import static neo.open.al.QALConstantsIfc.AL_ORIENTATION;
import static neo.open.al.QALConstantsIfc.AL_PITCH;
import static neo.open.al.QALConstantsIfc.AL_POSITION;
import static neo.open.al.QALConstantsIfc.AL_REFERENCE_DISTANCE;
import static neo.open.al.QALConstantsIfc.AL_SOURCE_RELATIVE;
import static neo.open.al.QALConstantsIfc.AL_TRUE;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import neo.TempDump.TODO_Exception;
import neo.Renderer.Cinematic.idSndWindow;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.Material.shaderStage_t;
import neo.Renderer.RenderWorld.exitPortal_t;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Sound.snd_cache.idSoundSample;
import neo.Sound.snd_emitter.idSlowChannel;
import neo.Sound.snd_emitter.idSoundChannel;
import neo.Sound.snd_emitter.idSoundEmitterLocal;
import neo.Sound.snd_emitter.idSoundFade;
import neo.Sound.snd_local.idSampleDecoder;
import neo.Sound.snd_local.mminfo_s;
import neo.Sound.snd_local.pcmwaveformat_s;
import neo.Sound.snd_local.soundDemoCommand_t;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.snd_shader.soundShaderParms_t;
import neo.Sound.snd_system.idSoundSystemLocal;
import neo.Sound.sound.idSoundEmitter;
import neo.Sound.sound.idSoundWorld;
import neo.framework.DemoFile.idDemoFile;
import neo.framework.File_h.idFile;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Plane.idPlane;
import neo.idlib.math.Random.idRandom;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.open.Nio;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class snd_world {

    static class s_stats {

        int rinuse;
        int runs;
        int timeinprocess;
        int missedWindow;
        int missedUpdateWindow;
        int activeSounds;

        public s_stats() {
            this.rinuse = 0;
            this.runs = 1;
            this.timeinprocess = 0;
            this.missedWindow = 0;
            this.missedUpdateWindow = 0;
            this.activeSounds = 0;
        }
    }

    static class soundPortalTrace_s {

        int                portalArea;
        soundPortalTrace_s prevStack;
    }

    public static class idSoundWorldLocal extends idSoundWorld {

        public idRenderWorld               rw;            // for portals and debug drawing
        public idDemoFile                  writeDemo;            // if not NULL, archive commands here
        //
        public idMat3                      listenerAxis;
        public idVec3                      listenerPos;                      // position in meters
        public int                         listenerPrivateId;
        public idVec3                      listenerQU;            // position in "quake units"
        public int                         listenerArea;
        public idStr                       listenerAreaName;
        public int                         listenerEnvironmentID;
        //
        public int                         gameMsec;
        public int                         game44kHz;
        public int                         pause44kHz;
        public int                         lastAVI44kHz;                        // determine when we need to mix and write another block
        //
        public idList<idSoundEmitterLocal> emitters;
        //
        public idSoundFade[] soundClassFade = new idSoundFade[SOUND_MAX_CLASSES];    // for global sound fading
        //
        // avi stuff
        public idFile[]      fpa            = new idFile[6];
        public idStr               aviDemoPath;
        public idStr               aviDemoName;
        //
        public idSoundEmitterLocal localSound;        // just for playShaderDirectly()
        //
        public boolean             slowmoActive;
        public float               slowmoSpeed;
        public boolean             enviroSuitActive;
        //
        //
        //============================================

        public idSoundWorldLocal() {
            this.listenerAxis = new idMat3();
            this.listenerPos = new idVec3();
            this.listenerQU = new idVec3();
            this.listenerAreaName = new idStr();
            this.emitters = new idList<>();
            this.aviDemoPath = new idStr();
            this.aviDemoName = new idStr();
        }

        // virtual					~idSoundWorldLocal();
        // call at each map start
        @Override
        public void ClearAllSoundEmitters() {
            int i;

            Sys_EnterCriticalSection();

            AVIClose();

            for (i = 0; i < this.emitters.Num(); i++) {
                final idSoundEmitterLocal sound = this.emitters.oGet(i);
                sound.Clear();
            }
            this.localSound = null;

            Sys_LeaveCriticalSection();
        }

        /*
         ===============
         idSoundWorldLocal::StopAllSounds

         this is called from the main thread
         ===============
         */
        @Override
        public void StopAllSounds() {

            for (int i = 0; i < this.emitters.Num(); i++) {
                final idSoundEmitterLocal def = this.emitters.oGet(i);
                def.StopSound(SCHANNEL_ANY);
            }
        }

        /*
         ===================
         idSoundWorldLocal::AllocSoundEmitter

         this is called from the main thread
         ===================
         */
        // get a new emitter that can play sounds in this world
        @Override
        public idSoundEmitter AllocSoundEmitter() {
            final idSoundEmitterLocal emitter = AllocLocalSoundEmitter();

            if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                common.Printf("AllocSoundEmitter = %d\n", emitter.index);
            }
            if (this.writeDemo != null) {
                this.writeDemo.WriteInt(DS_SOUND);
                this.writeDemo.WriteInt(SCMD_ALLOC_EMITTER);
                this.writeDemo.WriteInt(emitter.index);
            }

            return emitter;
        }

        // for load games
        @Override
        public idSoundEmitter EmitterForIndex(int index) {
            if (index == 0) {
                return null;
            }
            if (index >= this.emitters.Num()) {
                common.Error("idSoundWorldLocal::EmitterForIndex: %d > %d", index, this.emitters.Num());
            }
            return this.emitters.oGet(index);
        }

        /*
         ===================
         idSoundWorldLocal::CurrentShakeAmplitudeForPosition

         this is called from the main thread
         ===================
         */
        // query data from all emitters in the world
        @Override
        public float CurrentShakeAmplitudeForPosition(final int time, final idVec3 listererPosition) {
            float amp = 0.0f;
            int localTime;

            if (idSoundSystemLocal.s_constantAmplitude.GetFloat() >= 0.0f) {
                return 0.0f;
            }

            localTime = soundSystemLocal.GetCurrent44kHzTime();

            for (int i = 1; i < this.emitters.Num(); i++) {
                final idSoundEmitterLocal sound = this.emitters.oGet(i);
                if (!sound.hasShakes) {
                    continue;
                }
                amp += FindAmplitude(sound, localTime, listererPosition, SCHANNEL_ANY, true);
            }
            return amp;
        }

        /*
         ===================
         idSoundWorldLocal::PlaceListener

         this is called by the main thread
         ===================
         */
        // where is the camera/microphone
        // listenerId allows listener-private sounds to be added
        @Override
        public void PlaceListener(final idVec3 origin, final idMat3 axis, final int listenerId, final int gameTime, final idStr areaName) {

            int current44kHzTime;

            if (!soundSystemLocal.isInitialized) {
                return;
            }

            if (this.pause44kHz >= 0) {
                return;
            }

            if (this.writeDemo != null) {
                this.writeDemo.WriteInt(DS_SOUND);
                this.writeDemo.WriteInt(SCMD_PLACE_LISTENER);
                this.writeDemo.WriteVec3(origin);
                this.writeDemo.WriteMat3(axis);
                this.writeDemo.WriteInt(listenerId);
                this.writeDemo.WriteInt(gameTime);
            }

            current44kHzTime = soundSystemLocal.GetCurrent44kHzTime();

            // we usually expect gameTime to be increasing by 16 or 32 msec, but when
            // a cinematic is fast-forward skipped through, it can jump by a significant
            // amount, while the hardware 44kHz position will not have changed accordingly,
            // which would make sounds (like long character speaches) continue from the
            // old time.  Fix this by killing all non-looping sounds
            if (gameTime > (this.gameMsec + 500)) {
                OffsetSoundTime((int) (-(gameTime - this.gameMsec) * 0.001f * 44100.0f));
            }

            this.gameMsec = gameTime;
            if (this.fpa[0] != null) {
                // exactly 30 fps so the wave file can be used for exact video frames
                this.game44kHz = idMath.FtoiFast(this.gameMsec * ((1000.0f / 60.0f) / 16.0f) * 0.001f * 44100.0f);
            } else {
                // the normal 16 msec / frame
                this.game44kHz = idMath.FtoiFast(this.gameMsec * 0.001f * 44100.0f);
            }

            this.listenerPrivateId = listenerId;

            this.listenerQU.oSet(origin);                            // Doom units
            this.listenerPos = origin.oMultiply(DOOM_TO_METERS);     // meters
            this.listenerAxis.oSet(axis);
            this.listenerAreaName.oSet(areaName);
            this.listenerAreaName.ToLower();

            if (this.rw != null) {
                this.listenerArea = this.rw.PointInArea(this.listenerQU);    // where are we?
            } else {
                this.listenerArea = 0;
            }

            if (this.listenerArea < 0) {
                return;
            }

            ForegroundUpdate(current44kHzTime);
        }

        /*
         =================
         idSoundWorldLocal::FadeSoundClasses

         fade all sounds in the world with a given shader soundClass
         to is in Db (sigh), over is in seconds
         =================
         */
        @Override
        public void FadeSoundClasses(final int soundClass, final float to, final float over) {
            if ((soundClass < 0) || (soundClass >= SOUND_MAX_CLASSES)) {
                common.Error("idSoundWorldLocal::FadeSoundClasses: bad soundClass %d", soundClass);
            }

            final idSoundFade fade = this.soundClassFade[soundClass];

            final int length44kHz = soundSystemLocal.MillisecondsToSamples((int) (over * 1000));

            // if it is already fading to this volume at this rate, don't change it
            if ((fade.fadeEndVolume == to)
                    && ((fade.fadeEnd44kHz - fade.fadeStart44kHz) == length44kHz)) {
                return;
            }

            int start44kHz;

            if (this.fpa[0] != null) {
                // if we are recording an AVI demo, don't use hardware time
                start44kHz = this.lastAVI44kHz + MIXBUFFER_SAMPLES;
            } else {
                start44kHz = soundSystemLocal.GetCurrent44kHzTime() + MIXBUFFER_SAMPLES;
            }

            // fade it
            fade.fadeStartVolume = fade.FadeDbAt44kHz(start44kHz);
            fade.fadeStart44kHz = start44kHz;
            fade.fadeEnd44kHz = start44kHz + length44kHz;
            fade.fadeEndVolume = to;
        }

        // dumps the current state and begins archiving commands
        /*
         ===================
         idSoundWorldLocal::StartWritingDemo

         this is called from the main thread
         ===================
         */
        @Override
        public void StartWritingDemo(idDemoFile demo) {
            this.writeDemo = demo;

            this.writeDemo.WriteInt(DS_SOUND);
            this.writeDemo.WriteInt(SCMD_STATE);

            // use the normal save game code to archive all the emitters
            WriteToSaveGame(this.writeDemo);
        }

        /*
         ===================
         idSoundWorldLocal::StopWritingDemo

         this is called from the main thread
         ===================
         */
        @Override
        public void StopWritingDemo() {
            this.writeDemo = null;//TODO:booleanize?
        }

        /*
         ===================
         idSoundWorldLocal::ProcessDemoCommand

         this is called from the main thread
         ===================
         */
        // read a sound command from a demo file
        @Override
        public void ProcessDemoCommand(idDemoFile readDemo) {
            int index;
            idSoundEmitterLocal def;

            if (null == readDemo) {
                return;
            }

            int _dc;

            if (NOT(_dc = readDemo.ReadInt())) {
                return;
            }

            final soundDemoCommand_t dc = soundDemoCommand_t.values()[_dc];
            switch (dc) {
                case SCMD_STATE:
                    // we need to protect this from the async thread
                    // other instances of calling idSoundWorldLocal::ReadFromSaveGame do this while the sound code is muted
                    // setting muted and going right in may not be good enough here, as we async thread may already be in an async tick (in which case we could still race to it)
                    Sys_EnterCriticalSection();
                    ReadFromSaveGame(readDemo);
                    Sys_LeaveCriticalSection();
                    UnPause();
                    break;
                case SCMD_PLACE_LISTENER: {
                    final idVec3 origin = new idVec3();
                    final idMat3 axis = new idMat3();
                    int listenerId;
                    int gameTime;

                    readDemo.ReadVec3(origin);
                    readDemo.ReadMat3(axis);
                    listenerId = readDemo.ReadInt();
                    gameTime = readDemo.ReadInt();

                    PlaceListener(origin, axis, listenerId, gameTime, "");
                }
                break;
                case SCMD_ALLOC_EMITTER:
                    index = readDemo.ReadInt();
                    if ((index < 1) || (index > this.emitters.Num())) {
                        common.Error("idSoundWorldLocal::ProcessDemoCommand: bad emitter number");
                    }
                    if (index == this.emitters.Num()) {
                        // append a brand new one
                        def = new idSoundEmitterLocal();
                        this.emitters.Append(def);
                    }
                    def = this.emitters.oGet(index);
                    def.Clear();
                    def.index = index;
                    def.removeStatus = REMOVE_STATUS_ALIVE;
                    def.soundWorld = this;
                    break;
                case SCMD_FREE: {
                    int immediate;

                    index = readDemo.ReadInt();
                    immediate = readDemo.ReadInt();
                    EmitterForIndex(index).Free(immediate != 0);
                }
                break;
                case SCMD_UPDATE: {
                    final idVec3 origin = new idVec3();
                    int listenerId;
                    final soundShaderParms_t parms = new soundShaderParms_t();

                    index = readDemo.ReadInt();
                    readDemo.ReadVec3(origin);
                    listenerId = readDemo.ReadInt();
                    parms.minDistance = readDemo.ReadFloat();
                    parms.maxDistance = readDemo.ReadFloat();
                    parms.volume = readDemo.ReadFloat();
                    parms.shakes = readDemo.ReadFloat();
                    parms.soundShaderFlags = readDemo.ReadInt();
                    parms.soundClass = readDemo.ReadInt();
                    EmitterForIndex(index).UpdateEmitter(origin, listenerId, parms);
                }
                break;
                case SCMD_START: {
                    idSoundShader shader;
                    int channel;
                    float diversity;
                    int shaderFlags;

                    index = readDemo.ReadInt();
                    shader = declManager.FindSound(readDemo.ReadHashString());
                    channel = readDemo.ReadInt();
                    diversity = readDemo.ReadFloat();
                    shaderFlags = readDemo.ReadInt();
                    EmitterForIndex(index).StartSound(shader, channel, diversity, shaderFlags);
                }
                break;
                case SCMD_MODIFY: {
                    int channel;
                    final soundShaderParms_t parms = new soundShaderParms_t();

                    index = readDemo.ReadInt();
                    channel = readDemo.ReadInt();
                    parms.minDistance = readDemo.ReadFloat();
                    parms.maxDistance = readDemo.ReadFloat();
                    parms.volume = readDemo.ReadFloat();
                    parms.shakes = readDemo.ReadFloat();
                    parms.soundShaderFlags = readDemo.ReadInt();
                    parms.soundClass = readDemo.ReadInt();
                    EmitterForIndex(index).ModifySound(channel, parms);
                }
                break;
                case SCMD_STOP: {
                    int channel;

                    index = readDemo.ReadInt();
                    channel = readDemo.ReadInt();
                    EmitterForIndex(index).StopSound(channel);
                }
                break;
                case SCMD_FADE: {
                    int channel;
                    float to, over;

                    index = readDemo.ReadInt();
                    channel = readDemo.ReadInt();
                    to = readDemo.ReadFloat();
                    over = readDemo.ReadFloat();
                    EmitterForIndex(index).FadeSound(channel, to, over);
                }
                break;
            }
        }

        // background music
        /*
         ===============
         idSoundWorldLocal::PlayShaderDirectly

         start a music track

         this is called from the main thread
         ===============
         */
        private static final idRandom rnd = new idRandom();

        @Override
        public void PlayShaderDirectly(final String shaderName, int channel /*= -1*/) {

            if ((this.localSound != null) && (channel == -1)) {
                this.localSound.StopSound(SCHANNEL_ANY);
            } else if (this.localSound != null) {
                this.localSound.StopSound(channel);
            }

            if (!isNotNullOrEmpty(shaderName)) {
//            if (!shaderName || !shaderName[0]) {
                return;
            }

            final idSoundShader shader = declManager.FindSound(shaderName);
            if (null == shader) {
                return;
            }

            if (null == this.localSound) {
                this.localSound = AllocLocalSoundEmitter();
            }

            final float diversity = rnd.RandomFloat();

            this.localSound.StartSound(shader, (channel == -1) ? SCHANNEL_ONE : channel, diversity, SSF_GLOBAL);

            // in case we are at the console without a game doing updates, force an update
            ForegroundUpdate(soundSystemLocal.GetCurrent44kHzTime());
        }

        // pause and unpause the sound world
        @Override
        public void Pause() {
            if (this.pause44kHz >= 0) {
                common.Warning("idSoundWorldLocal::Pause: already paused");
                return;
            }

            this.pause44kHz = soundSystemLocal.GetCurrent44kHzTime();
        }

        @Override
        public void UnPause() {
            int offset44kHz;

            if (this.pause44kHz < 0) {
                common.Warning("idSoundWorldLocal::UnPause: not paused");
                return;
            }

            offset44kHz = soundSystemLocal.GetCurrent44kHzTime() - this.pause44kHz;
            OffsetSoundTime(offset44kHz);

            this.pause44kHz = -1;
        }

        @Override
        public boolean IsPaused() {
            return (this.pause44kHz >= 0);
        }

        /*
         ===================
         idSoundWorldLocal::AVIOpen

         this is called by the main thread
         ===================
         */
        // avidump
        @Override
        public void AVIOpen(final String path, final String name) {
            this.aviDemoPath.oSet(path);
            this.aviDemoName.oSet(name);

            this.lastAVI44kHz = this.game44kHz - (this.game44kHz % MIXBUFFER_SAMPLES);

            if (soundSystemLocal.snd_audio_hw.GetNumberOfSpeakers() == 6) {
                this.fpa[0] = fileSystem.OpenFileWrite(this.aviDemoPath + "channel_51_left.raw");
                this.fpa[1] = fileSystem.OpenFileWrite(this.aviDemoPath + "channel_51_right.raw");
                this.fpa[2] = fileSystem.OpenFileWrite(this.aviDemoPath + "channel_51_center.raw");
                this.fpa[3] = fileSystem.OpenFileWrite(this.aviDemoPath + "channel_51_lfe.raw");
                this.fpa[4] = fileSystem.OpenFileWrite(this.aviDemoPath + "channel_51_backleft.raw");
                this.fpa[5] = fileSystem.OpenFileWrite(this.aviDemoPath + "channel_51_backright.raw");
            } else {
                this.fpa[0] = fileSystem.OpenFileWrite(this.aviDemoPath + "channel_left.raw");
                this.fpa[1] = fileSystem.OpenFileWrite(this.aviDemoPath + "channel_right.raw");
            }

            soundSystemLocal.SetMute(true);
        }

        @Override
        public void AVIClose() {
            int i;

            if (null == this.fpa[0]) {
                return;
            }

            // make sure the final block is written
            this.game44kHz += MIXBUFFER_SAMPLES;
            AVIUpdate();
            this.game44kHz -= MIXBUFFER_SAMPLES;

            for (i = 0; i < 6; i++) {
                if (this.fpa[i] != null) {
                    fileSystem.CloseFile(this.fpa[i]);
                    this.fpa[i] = null;
                }
            }
            if (soundSystemLocal.snd_audio_hw.GetNumberOfSpeakers() == 2) {
                // convert it to a wave file
                idFile rL, lL, wO;
                idStr name;

                name = new idStr(this.aviDemoPath.getData() + this.aviDemoName + ".wav");
                wO = fileSystem.OpenFileWrite(name.getData());
                if (null == wO) {
                    common.Error("Couldn't write %s", name.c_str());
                }

                name.oSet(this.aviDemoPath + "channel_right.raw");
                rL = fileSystem.OpenFileRead(name.getData());
                if (null == rL) {
                    common.Error("Couldn't open %s", name.c_str());
                }

                name.oSet(this.aviDemoPath + "channel_left.raw");
                lL = fileSystem.OpenFileRead(name.getData());
                if (null == lL) {
                    common.Error("Couldn't open %s", name.c_str());
                }

                final int numSamples = rL.Length() / 2;
                final mminfo_s info = new mminfo_s();
                final pcmwaveformat_s format = new pcmwaveformat_s();

                info.ckid = fourcc_riff;
                info.fccType = mmioFOURCC('W', 'A', 'V', 'E');
                info.cksize = ((rL.Length() * 2) - 8) + 4 + 16 + 8 + 8;
                info.dwDataOffset = 12;

                wO.Write(info.Write(), 12);

                info.ckid = mmioFOURCC('f', 'm', 't', ' ');
                info.cksize = 16;

                wO.Write(info.Write(), 8);

                format.wBitsPerSample = 16;
                format.wf.nAvgBytesPerSec = 44100 * 4;		// sample rate * block align
                format.wf.nChannels = 2;
                format.wf.nSamplesPerSec = 44100;
                format.wf.wFormatTag = WAVE_FORMAT_TAG_PCM;
                format.wf.nBlockAlign = 4;			// channels * bits/sample / 8

                wO.Write(format.Write(), 16);

                info.ckid = mmioFOURCC('d', 'a', 't', 'a');
                info.cksize = rL.Length() * 2;

                wO.Write(info.Write(), 8);

                short s0, s1;
                for (i = 0; i < numSamples; i++) {
                    s0 = lL.ReadShort();
                    s1 = rL.ReadShort();
                    wO.WriteShort(s0);
                    wO.WriteShort(s1);
                }

                fileSystem.CloseFile(wO);
                fileSystem.CloseFile(lL);
                fileSystem.CloseFile(rL);

                fileSystem.RemoveFile(this.aviDemoPath + "channel_right.raw");
                fileSystem.RemoveFile(this.aviDemoPath + "channel_left.raw");
            }

            soundSystemLocal.SetMute(false);
        }

        // SaveGame Support
        @Override
        public void WriteToSaveGame(idFile savefile) {
            int i, j, num, currentSoundTime;
            String name;

            // the game soundworld is always paused at this point, save that time down
            if (this.pause44kHz > 0) {
                currentSoundTime = this.pause44kHz;
            } else {
                currentSoundTime = soundSystemLocal.GetCurrent44kHzTime();
            }

            // write listener data
            savefile.WriteVec3(this.listenerQU);
            savefile.WriteMat3(this.listenerAxis);
            savefile.WriteInt(this.listenerPrivateId);
            savefile.WriteInt(this.gameMsec);
            savefile.WriteInt(this.game44kHz);
            savefile.WriteInt(currentSoundTime);

            num = this.emitters.Num();
            savefile.WriteInt(num);

            for (i = 1; i < this.emitters.Num(); i++) {
                final idSoundEmitterLocal def = this.emitters.oGet(i);

                if (def.removeStatus != REMOVE_STATUS_ALIVE) {
                    final int skip = -1;
//                    savefile.Write(skip, sizeof(skip));
                    savefile.WriteInt(skip);
                    continue;
                }

                savefile.WriteInt(i);

                // Write the emitter data
                savefile.WriteVec3(def.origin);
                savefile.WriteInt(def.listenerId);
                WriteToSaveGameSoundShaderParams(savefile, def.parms);
                savefile.WriteFloat(def.amplitude);
                savefile.WriteInt(def.ampTime);
                for (int k = 0; k < SOUND_MAX_CHANNELS; k++) {
                    WriteToSaveGameSoundChannel(savefile, def.channels[k]);
                }
                savefile.WriteFloat(def.distance);
                savefile.WriteBool(def.hasShakes);
                savefile.WriteInt(def.lastValidPortalArea);
                savefile.WriteFloat(def.maxDistance);
                savefile.WriteBool(def.playing);
                savefile.WriteFloat(def.realDistance);
                savefile.WriteInt(def.removeStatus);
                savefile.WriteVec3(def.spatializedOrigin);

                // write the channel data
                for (j = 0; j < SOUND_MAX_CHANNELS; j++) {
                    final idSoundChannel chan = def.channels[j];

                    // Write out any sound commands for this def
                    if (chan.triggerState && (chan.soundShader != null) && (chan.leadinSample != null)) {

                        savefile.WriteInt(j);

                        // write the pointers out separately
                        name = chan.soundShader.GetName();
                        savefile.WriteString(name);

                        name = chan.leadinSample.name.getData();
                        savefile.WriteString(name);
                    }
                }

                // End active channels with -1
                final int end = -1;
                savefile.WriteInt(end);
            }

            // new in Doom3 v1.2
            savefile.WriteBool(this.slowmoActive);
            savefile.WriteFloat(this.slowmoSpeed);
            savefile.WriteBool(this.enviroSuitActive);
        }

        @Override
        public void ReadFromSaveGame(idFile savefile) {
            int i, num, handle, listenerId, gameTime, channel;
            int currentSoundTime, soundTimeOffset, savedSoundTime;
            idSoundEmitterLocal def;
            final idVec3 origin = new idVec3();
            final idMat3 axis = new idMat3();
            final idStr soundShader = new idStr();

            ClearAllSoundEmitters();

            savefile.ReadVec3(origin);
            savefile.ReadMat3(axis);
            listenerId = savefile.ReadInt();
            gameTime = savefile.ReadInt();
            this.game44kHz = savefile.ReadInt();
            savedSoundTime = savefile.ReadInt();

            // we will adjust the sound starting times from those saved with the demo
            currentSoundTime = soundSystemLocal.GetCurrent44kHzTime();
            soundTimeOffset = currentSoundTime - savedSoundTime;

            // at the end of the level load we unpause the sound world and adjust the sound starting times once more
            this.pause44kHz = currentSoundTime;

            // place listener
            PlaceListener(origin, axis, listenerId, gameTime, "Undefined");

            // make sure there are enough
            // slots to read the saveGame in.  We don't shrink the list
            // if there are extras.
            num = savefile.ReadInt();

            while (this.emitters.Num() < num) {
                def = new idSoundEmitterLocal();
                def.index = this.emitters.Append(def);
                def.soundWorld = this;
            }

            // read in the state
            for (i = 1; i < num; i++) {

                handle = savefile.ReadInt();
                if (handle < 0) {
                    continue;
                }
                if (handle != i) {
                    common.Error("idSoundWorldLocal::ReadFromSaveGame: index mismatch");
                }
                def = this.emitters.oGet(i);

                def.removeStatus = REMOVE_STATUS_ALIVE;
                def.playing = true;        // may be reset by the first UpdateListener

                savefile.ReadVec3(def.origin);
                def.listenerId = savefile.ReadInt();
                ReadFromSaveGameSoundShaderParams(savefile, def.parms);
                def.amplitude = savefile.ReadFloat();
                def.ampTime = savefile.ReadInt();
                for (int k = 0; k < SOUND_MAX_CHANNELS; k++) {
                    ReadFromSaveGameSoundChannel(savefile, def.channels[k]);
                }
                def.distance = savefile.ReadFloat();
                def.hasShakes = savefile.ReadBool();
                def.lastValidPortalArea = savefile.ReadInt();
                def.maxDistance = savefile.ReadFloat();
                def.playing = savefile.ReadBool();
                def.realDistance = savefile.ReadFloat();
                def.removeStatus = savefile.ReadInt();
                savefile.ReadVec3(def.spatializedOrigin);

                // read the individual channels
                channel = savefile.ReadInt();

                while (channel >= 0) {
                    if (channel > SOUND_MAX_CHANNELS) {
                        common.Error("idSoundWorldLocal::ReadFromSaveGame: channel > SOUND_MAX_CHANNELS");
                    }

                    final idSoundChannel chan = def.channels[channel];

                    if (chan.decoder != null) {
                        // The pointer in the save file is not valid, so we grab a new one
                        chan.decoder = idSampleDecoder.Alloc();
                    }

                    savefile.ReadString(soundShader);
                    chan.soundShader = declManager.FindSound(soundShader);

                    savefile.ReadString(soundShader);
                    // load savegames with s_noSound 1
                    if (soundSystemLocal.soundCache != null) {
                        chan.leadinSample = soundSystemLocal.soundCache.FindSound(soundShader, false);
                    } else {
                        chan.leadinSample = null;
                    }

                    // adjust the hardware start time
                    chan.trigger44kHzTime += soundTimeOffset;

                    // make sure we start up the hardware voice if needed
                    chan.triggered = chan.triggerState;
                    chan.openalStreamingOffset = currentSoundTime - chan.trigger44kHzTime;

                    // adjust the hardware fade time
                    if (chan.channelFade.fadeStart44kHz != 0) {
                        chan.channelFade.fadeStart44kHz += soundTimeOffset;
                        chan.channelFade.fadeEnd44kHz += soundTimeOffset;
                    }

                    // next command
                    channel = savefile.ReadInt();
                }
            }

            if (session.GetSaveGameVersion() >= 17) {
                this.slowmoActive = savefile.ReadBool(/*sizeof(slowmoActive)*/);
                this.slowmoSpeed = savefile.ReadFloat(/*sizeof(slowmoSpeed)*/);
                this.enviroSuitActive = savefile.ReadBool(/*sizeof(enviroSuitActive)*/);
            } else {
                this.slowmoActive = false;
                this.slowmoSpeed = 0;
                this.enviroSuitActive = false;
            }
        }

        public void ReadFromSaveGameSoundChannel(idFile saveGame, idSoundChannel ch) {
            throw new TODO_Exception();
//            ch.triggerState = saveGame.ReadBool();
//            short tmp;
//            tmp = saveGame.ReadChar();
//            tmp = saveGame.ReadChar();
//            tmp = saveGame.ReadChar();
//            ch.trigger44kHzTime = saveGame.ReadInt();
//            ch.triggerGame44kHzTime = saveGame.ReadInt();
//            ReadFromSaveGameSoundShaderParams(saveGame, ch.parms);
//            saveGame.ReadInt((int &) ch.leadinSample);
//            ch.triggerChannel = saveGame.ReadInt();
//            saveGame.ReadInt((int &) ch.soundShader);
//            saveGame.ReadInt((int &) ch.decoder);
//            ch.diversity = saveGame.ReadFloat();
//            ch.lastVolume = saveGame.ReadFloat();
//            for (int m = 0; m < 6; m++) {
//                ch.lastV[m] = saveGame.ReadFloat();
//            }
//            ch.channelFade.fadeStart44kHz = saveGame.ReadInt();
//            ch.channelFade.fadeEnd44kHz = saveGame.ReadInt();
//            ch.channelFade.fadeStartVolume = saveGame.ReadFloat();
//            ch.channelFade.fadeEndVolume = saveGame.ReadFloat();
        }

        public void ReadFromSaveGameSoundShaderParams(idFile saveGame, soundShaderParms_t params) {
            params.minDistance = saveGame.ReadFloat();
            params.maxDistance = saveGame.ReadFloat();
            params.volume = saveGame.ReadFloat();
            params.shakes = saveGame.ReadFloat();
            params.soundShaderFlags = saveGame.ReadInt();
            params.soundClass = saveGame.ReadInt();
        }

        public void WriteToSaveGameSoundChannel(idFile saveGame, idSoundChannel ch) {
            throw new TODO_Exception();
//            saveGame.WriteBool(ch.triggerState);
//            saveGame.WriteUnsignedChar((char) 0);
//            saveGame.WriteUnsignedChar((char) 0);
//            saveGame.WriteUnsignedChar((char) 0);
//            saveGame.WriteInt(ch.trigger44kHzTime);
//            saveGame.WriteInt(ch.triggerGame44kHzTime);
//            WriteToSaveGameSoundShaderParams(saveGame, ch.parms);
//            saveGame.WriteInt((int) ch.leadinSample);
//            saveGame.WriteInt(ch.triggerChannel);
//            saveGame.WriteInt((int) ch.soundShader);
//            saveGame.WriteInt((int) ch.decoder);
//            saveGame.WriteFloat(ch.diversity);
//            saveGame.WriteFloat(ch.lastVolume);
//            for (int m = 0; m < 6; m++) {
//                saveGame.WriteFloat(ch.lastV[m]);
//            }
//            saveGame.WriteInt(ch.channelFade.fadeStart44kHz);
//            saveGame.WriteInt(ch.channelFade.fadeEnd44kHz);
//            saveGame.WriteFloat(ch.channelFade.fadeStartVolume);
//            saveGame.WriteFloat(ch.channelFade.fadeEndVolume);
        }

        public void WriteToSaveGameSoundShaderParams(idFile saveGame, soundShaderParms_t params) {
            saveGame.WriteFloat(params.minDistance);
            saveGame.WriteFloat(params.maxDistance);
            saveGame.WriteFloat(params.volume);
            saveGame.WriteFloat(params.shakes);
            saveGame.WriteInt(params.soundShaderFlags);
            saveGame.WriteInt(params.soundClass);
        }

        @Override
        public void SetSlowmo(boolean active) {
            this.slowmoActive = active;
        }

        @Override
        public void SetSlowmoSpeed(float speed) {
            this.slowmoSpeed = speed;
        }

        @Override
        public void SetEnviroSuit(boolean active) {
            this.enviroSuitActive = active;
        }

        //=======================================
        /*
         ===============
         idSoundWorldLocal::Shutdown

         this is called from the main thread
         ===============
         */
        public void Shutdown() {
            int i;

            if (soundSystemLocal.currentSoundWorld.equals(this)) {
                soundSystemLocal.currentSoundWorld = null;
            }

            AVIClose();

            for (i = 0; i < this.emitters.Num(); i++) {
                if (this.emitters.oGet(i) != null) {
//			delete emitters[i];
                    this.emitters.oSet(i, null);
                }
            }
            this.localSound = null;
        }

        public void Init(idRenderWorld rw) {
            this.rw = rw;
            this.writeDemo = null;

            this.listenerAxis.Identity();
            this.listenerPos.Zero();
            this.listenerPrivateId = 0;
            this.listenerQU.Zero();
            this.listenerArea = 0;
            this.listenerAreaName.oSet("Undefined");
            this.listenerEnvironmentID = -2;

            this.gameMsec = 0;
            this.game44kHz = 0;
            this.pause44kHz = -1;
            this.lastAVI44kHz = 0;

            for (int i = 0; i < SOUND_MAX_CLASSES; i++) {
                this.soundClassFade[i] = new idSoundFade();
                this.soundClassFade[i].Clear();
            }

            // fill in the 0 index spot
            final idSoundEmitterLocal placeHolder = new idSoundEmitterLocal();
            this.emitters.Append(placeHolder);

            this.fpa[0] = this.fpa[1] = this.fpa[2] = this.fpa[3] = this.fpa[4] = this.fpa[5] = null;

            this.aviDemoPath.oSet("");
            this.aviDemoName.oSet("");

            this.localSound = null;

            this.slowmoActive = false;
            this.slowmoSpeed = 0;
            this.enviroSuitActive = false;
        }

        public void ClearBuffer() {
            throw new TODO_Exception();
//
//            // check to make sure hardware actually exists
//            if (NOT(snd_audio_hw)) {
//                return;
//            }
//
//            short[] fBlock;
//            long/*ulong*/ fBlockLen;
//
//            if (!snd_audio_hw.Lock( /*(void **)&*/fBlock, fBlockLen)) {
//                return;
//            }
//
//            if (fBlock != null) {//TODO:create an init flag within all classes??
//                SIMDProcessor.Memset(fBlock, 0, fBlockLen);
//                snd_audio_hw.Unlock(fBlock, fBlockLen);
//            }
        }

        // update
        public void ForegroundUpdate(int current44kHzTime) {
            int j, k;
            idSoundEmitterLocal def;

            if (!soundSystemLocal.isInitialized) {
                return;
            }

            Sys_EnterCriticalSection();

            // if we are recording an AVI demo, don't use hardware time
            if (this.fpa[0] != null) {
                current44kHzTime = this.lastAVI44kHz;
            }

            //
            // check to see if each sound is visible or not
            // speed up by checking maxdistance to origin
            // although the sound may still need to play if it has
            // just become occluded so it can ramp down to 0
            //
            for (j = 1; j < this.emitters.Num(); j++) {
                def = this.emitters.oGet(j);

                if (def.removeStatus >= REMOVE_STATUS_SAMPLEFINISHED) {
                    continue;
                }

                // see if our last channel just finished
                def.CheckForCompletion(current44kHzTime);

                if (!def.playing) {
                    continue;
                }

                // update virtual origin / distance, etc
                def.Spatialize(this.listenerPos, this.listenerArea, this.rw);

                // per-sound debug options
                if ((idSoundSystemLocal.s_drawSounds.GetInteger() != 0) && (this.rw != null)) {
                    if ((def.distance < def.maxDistance) || (idSoundSystemLocal.s_drawSounds.GetInteger() > 1)) {
                        final idBounds ref = new idBounds();
                        ref.Clear();
                        ref.AddPoint(new idVec3(-10, -10, -10));
                        ref.AddPoint(new idVec3(10, 10, 10));
                        final float vis = (1.0f - (def.distance / def.maxDistance));

                        // draw a box
                        this.rw.DebugBounds(new idVec4(vis, 0.25f, vis, vis), ref, def.origin);

                        // draw an arrow to the audible position, possible a portal center
                        if (def.origin != def.spatializedOrigin) {
                            this.rw.DebugArrow(colorRed, def.origin, def.spatializedOrigin, 4);
                        }

                        // draw the index
                        final idVec3 textPos = def.origin;
                        textPos.oMinSet(2, 8);
                        this.rw.DrawText(va("%d", def.index), textPos, 0.1f, new idVec4(1, 0, 0, 1), this.listenerAxis);
                        textPos.oPluSet(2, 8);

                        // run through all the channels
                        for (k = 0; k < SOUND_MAX_CHANNELS; k++) {
                            final idSoundChannel chan = def.channels[k];

                            // see if we have a sound triggered on this channel
                            if (!chan.triggerState) {
                                continue;
                            }

//					char	[]text = new char[1024];
                            String text;
                            final float min = chan.parms.minDistance;
                            final float max = chan.parms.maxDistance;
                            final String defaulted = chan.leadinSample.defaultSound ? "(DEFAULTED)" : "";
                            text = String.format("%s (%d/%d %d/%d)%s", chan.soundShader.GetName(), (int) def.distance, (int) def.realDistance, (int) min, (int) max, defaulted);
                            this.rw.DrawText(text, textPos, 0.1f, new idVec4(1, 0, 0, 1), this.listenerAxis);
                            textPos.oPluSet(2, 8);
                        }
                    }
                }
            }

            Sys_LeaveCriticalSection();

            //
            // the sound meter
            //
            if (s_showLevelMeter.GetInteger() != 0) {
                final idMaterial gui = declManager.FindMaterial("guis/assets/soundmeter/audiobg", false);
                if (gui != null) {
                    final shaderStage_t foo = gui.GetStage(0);
                    if (NOT(foo.texture.cinematic[0])) {
                        foo.texture.cinematic[0] = new idSndWindow();
                    }
                }
            }

            //
            // optionally dump out the generated sound
            //
            if (this.fpa[0] != null) {
                AVIUpdate();
            }
        }

        public void OffsetSoundTime(int offset44kHz) {
            int i, j;

            for (i = 0; i < this.emitters.Num(); i++) {
                if (this.emitters.oGet(i) == null) {
                    continue;
                }
                for (j = 0; j < SOUND_MAX_CHANNELS; j++) {
                    final idSoundChannel chan = this.emitters.oGet(i).channels[j];

                    if (!chan.triggerState) {
                        continue;
                    }

                    chan.trigger44kHzTime += offset44kHz;
                }
            }
        }

        public idSoundEmitterLocal AllocLocalSoundEmitter() {
            int i, index;
            idSoundEmitterLocal def = null;

            index = -1;

            // never use the 0 index spot
            for (i = 1; i < this.emitters.Num(); i++) {
                def = this.emitters.oGet(i);

                // check for a completed and freed spot
                if (def.removeStatus >= REMOVE_STATUS_SAMPLEFINISHED) {
                    index = i;
                    if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                        common.Printf("sound: recycling sound def %d\n", i);
                    }
                    break;
                }
            }

            if (index == -1) {
                // append a brand new one
                def = new idSoundEmitterLocal();

                // we need to protect this from the async thread
                Sys_EnterCriticalSection();
                index = this.emitters.Append(def);
                Sys_LeaveCriticalSection();

                if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                    common.Printf("sound: appended new sound def %d\n", index);
                }
            }

            def.Clear();
            def.index = index;
            def.removeStatus = REMOVE_STATUS_ALIVE;
            def.soundWorld = this;

            return def;
        }

        /*
         ===============
         idSoundWorldLocal::CalcEars

         Determine the volumes from each speaker for a given sound emitter
         ===============
         */
        private static final idVec3[] speakerVector = {
                new idVec3(0.707f, 0.707f, 0.0f), // front left
                new idVec3(0.707f, -0.707f, 0.0f), // front right
                new idVec3(0.707f, 0.0f, 0.0f), // front center
                new idVec3(0.0f, 0.0f, 0.0f), // sub
                new idVec3(-0.707f, 0.707f, 0.0f), // rear left
                new idVec3(-0.707f, -0.707f, 0.0f) // rear right
        };

        public void CalcEars(int numSpeakers, idVec3 spatializedOrigin, idVec3 listenerPos, idMat3 listenerAxis, float[] ears/*[6]*/, float spatialize) {
            final idVec3 svec = spatializedOrigin.oMinus(listenerPos);
            final idVec3 ovec = new idVec3(svec.oMultiply(listenerAxis.oGet(0)), svec.oMultiply(listenerAxis.oGet(1)), svec.oMultiply(listenerAxis.oGet(2)));

            ovec.Normalize();

            if (numSpeakers == 6) {
                for (int i = 0; i < 6; i++) {
                    if (i == 3) {
                        ears[i] = idSoundSystemLocal.s_subFraction.GetFloat();        // subwoofer
                        continue;
                    }
                    final float dot = ovec.oMultiply(speakerVector[i]);
                    ears[i] = (idSoundSystemLocal.s_dotbias6.GetFloat() + dot) / (1.0f + idSoundSystemLocal.s_dotbias6.GetFloat());
                    if (ears[i] < idSoundSystemLocal.s_minVolume6.GetFloat()) {
                        ears[i] = idSoundSystemLocal.s_minVolume6.GetFloat();
                    }
                }
            } else {
                final float dot = ovec.y;
                float dotBias = idSoundSystemLocal.s_dotbias2.GetFloat();

                // when we are inside the minDistance, start reducing the amount of spatialization
                // so NPC voices right in front of us aren't quieter that off to the side
                dotBias += (idSoundSystemLocal.s_spatializationDecay.GetFloat() - dotBias) * (1.0f - spatialize);

                ears[0] = (idSoundSystemLocal.s_dotbias2.GetFloat() + dot) / (1.0f + dotBias);
                ears[1] = (idSoundSystemLocal.s_dotbias2.GetFloat() - dot) / (1.0f + dotBias);

                if (ears[0] < idSoundSystemLocal.s_minVolume2.GetFloat()) {
                    ears[0] = idSoundSystemLocal.s_minVolume2.GetFloat();
                }
                if (ears[1] < idSoundSystemLocal.s_minVolume2.GetFloat()) {
                    ears[1] = idSoundSystemLocal.s_minVolume2.GetFloat();
                }

                ears[2] = ears[3] = ears[4] = ears[5] = 0.0f;
            }
        }

        /*
         ===============
         idSoundWorldLocal::AddChannelContribution

         Adds the contribution of a single sound channel to finalMixBuffer
         this is called from the async thread

         Mixes MIXBUFFER_SAMPLES samples starting at current44kHz sample time into
         finalMixBuffer
         ===============
         */private static int DBG_AddChannelContribution = 0;

        public void AddChannelContribution(idSoundEmitterLocal sound, idSoundChannel chan, int current44kHz, int numSpeakers, float[] finalMixBuffer) {
            int j;
            float volume;

            //
            // get the sound definition and parameters from the entity
            //
            final soundShaderParms_t parms = chan.parms;

            // assume we have a sound triggered on this channel
            assert (chan.triggerState);

            // fetch the actual wave file and see if it's valid
            final idSoundSample sample = chan.leadinSample;
            if (sample == null) {
                return;
            }

            // if you don't want to hear all the beeps from missing sounds
            if (sample.defaultSound && !idSoundSystemLocal.s_playDefaultSound.GetBool()) {
                return;
            }

            // get the actual shader
            final idSoundShader shader = chan.soundShader;

            // this might happen if the foreground thread just deleted the sound emitter
            if (null == shader) {
                return;
            }

            float maxD = parms.maxDistance;
            final float minD = parms.minDistance;

            int mask = shader.speakerMask;
            boolean omni = (parms.soundShaderFlags & SSF_OMNIDIRECTIONAL) != 0;
            final boolean looping = (parms.soundShaderFlags & SSF_LOOPING) != 0;
            boolean global = (parms.soundShaderFlags & SSF_GLOBAL) != 0;
            final boolean noOcclusion = ((parms.soundShaderFlags & SSF_NO_OCCLUSION) != 0) || !idSoundSystemLocal.s_useOcclusion.GetBool();

            // speed goes from 1 to 0.2
            if (idSoundSystemLocal.s_slowAttenuate.GetBool() && this.slowmoActive && !chan.disallowSlow) {
                maxD *= this.slowmoSpeed;
            }

            // stereo samples are always omni
            if (sample.objectInfo.nChannels == 2) {
                omni = true;
            }

            // if the sound is playing from the current listener, it will not be spatialized at all
            if (sound.listenerId == this.listenerPrivateId) {
                global = true;
            }

            //
            // see if it's in range
            //
            // convert volumes from decibels to float scale
            // leadin volume scale for shattering lights
            // this isn't exactly correct, because the modified volume will get applied to
            // some initial chunk of the loop as well, because the volume is scaled for the
            // entire mix buffer
            if ((shader.leadinVolume != 0) && ((current44kHz - chan.trigger44kHzTime) < sample.LengthIn44kHzSamples())) {
                volume = soundSystemLocal.dB2Scale(shader.leadinVolume);
            } else {
                volume = soundSystemLocal.dB2Scale(parms.volume);
            }

            // global volume scale
            volume *= soundSystemLocal.dB2Scale(idSoundSystemLocal.s_volume.GetFloat());

            // volume fading
            float fadeDb = chan.channelFade.FadeDbAt44kHz(current44kHz);
            volume *= soundSystemLocal.dB2Scale(fadeDb);

            fadeDb = this.soundClassFade[parms.soundClass].FadeDbAt44kHz(current44kHz);
            volume *= soundSystemLocal.dB2Scale(fadeDb);

            //
            // if it's a global sound then
            // it's not affected by distance or occlusion
            //
            float spatialize = 1;
            idVec3 spatializedOriginInMeters = new idVec3();
            if (!global) {
                float dlen;

                if (noOcclusion) {
                    // use the real origin and distance
                    spatializedOriginInMeters = sound.origin.oMultiply(DOOM_TO_METERS);
                    dlen = sound.realDistance;
                } else {
                    // use the possibly portal-occluded origin and distance
                    spatializedOriginInMeters = sound.spatializedOrigin.oMultiply(DOOM_TO_METERS);
                    dlen = sound.distance;
                }

                // reduce volume based on distance
                if (dlen >= maxD) {
                    volume = 0.0f;
                } else if (dlen > minD) {
                    float frac = idMath.ClampFloat(0.0f, 1.0f, 1.0f - ((dlen - minD) / (maxD - minD)));
                    if (idSoundSystemLocal.s_quadraticFalloff.GetBool()) {
                        frac *= frac;
                    }
                    volume *= frac;
                } else if (minD > 0.0f) {
                    // we tweak the spatialization bias when you are inside the minDistance
                    spatialize = dlen / minD;
                }
            }

            //
            // if it is a private sound, set the volume to zero
            // unless we match the listenerId
            //
            if ((parms.soundShaderFlags & SSF_PRIVATE_SOUND) != 0) {
                if (sound.listenerId != this.listenerPrivateId) {
                    volume = 0;
                }
            }
            if ((parms.soundShaderFlags & SSF_ANTI_PRIVATE_SOUND) != 0) {
                if (sound.listenerId == this.listenerPrivateId) {
                    volume = 0;
                }
            }

            //
            // do we have anything to add?
            //
            if ((volume < SND_EPSILON) && (chan.lastVolume < SND_EPSILON)) {
                return;
            }
            chan.lastVolume = volume;

            //
            // fetch the sound from the cache as 44kHz, 16 bit samples
            //
            final int offset = current44kHz - chan.trigger44kHzTime;
//            float[] inputSamples = new float[MIXBUFFER_SAMPLES * 2 + 16];
//            float[] alignedInputSamples = (float[]) ((((int) inputSamples) + 15) & ~15);
            float[] alignedInputSamples = new float[(MIXBUFFER_SAMPLES * 2) + 16];

            //
            // allocate and initialize hardware source
            //
            if (idSoundSystemLocal.useOpenAL && (sound.removeStatus < REMOVE_STATUS_SAMPLEFINISHED)) {
                if (!alIsSource(chan.openalSource)) {
                    chan.openalSource = soundSystemLocal.AllocOpenALSource(chan, !chan.leadinSample.hardwareBuffer || !chan.soundShader.entries[0].hardwareBuffer || looping, chan.leadinSample.objectInfo.nChannels == 2);
                }

                if (alIsSource(chan.openalSource)) {

                    // stop source if needed..
                    if (chan.triggered) {
                        alSourceStop(chan.openalSource);
                    }

                    // update source parameters
                    if (global || omni) {
                        alSourcei(chan.openalSource, AL_SOURCE_RELATIVE, AL_TRUE);
                        alSource3f(chan.openalSource, AL_POSITION, 0.0f, 0.0f, 0.0f);
                        alSourcef(chan.openalSource, AL_GAIN, Math.min((volume), (1.0f)));
                    } else {
                        alSourcei(chan.openalSource, AL_SOURCE_RELATIVE, AL_FALSE);
                        alSource3f(chan.openalSource, AL_POSITION, -spatializedOriginInMeters.y, spatializedOriginInMeters.z, -spatializedOriginInMeters.x);
                        alSourcef(chan.openalSource, AL_GAIN, Math.min((volume), (1.0f)));
                    }
                    alSourcei(chan.openalSource, AL_LOOPING, (looping && chan.soundShader.entries[0].hardwareBuffer) ? AL_TRUE : AL_FALSE);
                    if (!MACOS_X) {
                        alSourcef(chan.openalSource, AL_REFERENCE_DISTANCE, minD);
                        alSourcef(chan.openalSource, AL_MAX_DISTANCE, maxD);
                    }
                    alSourcef(chan.openalSource, AL_PITCH, (this.slowmoActive && !chan.disallowSlow) ? (this.slowmoSpeed) : (1.0f));
//                    if (ID_OPENAL) {
//                        long lOcclusion = (enviroSuitActive ? -1150 : 0);
//                        if (soundSystemLocal.alEAXSet) {
//                            soundSystemLocal.alEAXSet(EAXPROPERTYID_EAX_Source, EAXSOURCE_OCCLUSION, chan.openalSource, lOcclusion, sizeof(lOcclusion));
//                        }
//                    }
                    if ((!looping && chan.leadinSample.hardwareBuffer) || (looping && chan.soundShader.entries[0].hardwareBuffer)) {
                        // handle uncompressed (non streaming) single shot and looping sounds
                        if (chan.triggered) {
                            alSourcei(chan.openalSource, AL_BUFFER, looping ? chan.soundShader.entries[0].openalBuffer : chan.leadinSample.openalBuffer);
                        }
                    } else {
                        final int/*ALint*/ finishedbuffers;
                        final IntBuffer buffers = Nio.newIntBuffer(3);

                        // handle streaming sounds (decode on the fly) both single shot AND looping
                        if (chan.triggered) {
                            alSourcei(chan.openalSource, AL_BUFFER, 0);
                            alDeleteBuffers(chan.lastopenalStreamingBuffer);//alDeleteBuffers(3, chan.lastopenalStreamingBuffer[0]);
                            chan.lastopenalStreamingBuffer.put(0, chan.openalStreamingBuffer.get(0));
                            chan.lastopenalStreamingBuffer.put(1, chan.openalStreamingBuffer.get(1));
                            chan.lastopenalStreamingBuffer.put(2, chan.openalStreamingBuffer.get(2));
                            alGenBuffers(chan.openalStreamingBuffer);//alGenBuffers(3, chan.openalStreamingBuffer[0]);
//                            if (soundSystemLocal.alEAXSetBufferMode) {
//                                soundSystemLocal.alEAXSetBufferMode(3, chan.openalStreamingBuffer[0], alGetEnumValue(ID_ALCHAR + "AL_STORAGE_ACCESSIBLE"));
//                            }
                            buffers.put(0, chan.openalStreamingBuffer.get(0));
                            buffers.put(1, chan.openalStreamingBuffer.get(1));
                            buffers.put(2, chan.openalStreamingBuffer.get(2));
                            finishedbuffers = 3;
                        } else {
                            finishedbuffers = alGetSourcei(chan.openalSource, AL_BUFFERS_PROCESSED);//alGetSourcei(chan.openalSource, AL_BUFFERS_PROCESSED, finishedbuffers);
                            DBG_AddChannelContribution++;
                            for (int i = 0; i < finishedbuffers; i++) {//jake2
                                buffers.put(i, alSourceUnqueueBuffers(chan.openalSource));//alSourceUnqueueBuffers(chan.openalSource, finishedbuffers, buffers[0]);
                            }
//                            System.out.println("====" + AL10.alGetError());
                            if (finishedbuffers == 3) {
                                chan.triggered = true;
                            }
                        }

                        final int length = MIXBUFFER_SAMPLES * sample.objectInfo.nChannels;
                        for (j = 0; j < finishedbuffers; j++) {
                            final FloatBuffer samples = FloatBuffer.wrap(alignedInputSamples);
                            chan.GatherChannelSamples(chan.openalStreamingOffset * sample.objectInfo.nChannels, length, samples);
                            final ByteBuffer data = Nio.newByteBuffer(length * Short.BYTES);
                            final ShortBuffer dataS = data.asShortBuffer();
                            for (int i = 0; i < length; i++) {
                                if (alignedInputSamples[i] < -32768.0f) {
                                    dataS.put(i, Short.MIN_VALUE);
                                } else if (alignedInputSamples[i] > 32767.0f) {
                                    dataS.put(i, Short.MAX_VALUE);
                                } else {
                                    final short bla = (short) idMath.FtoiFast(alignedInputSamples[i]);
                                    dataS.put(i, bla);
//                                    System.out.println("<<" + bla);
                                }
                            }
                            final ByteBuffer d = (ByteBuffer) data.duplicate().position(0);
//                            System.out.printf(">>\n%f\n%f\n%f\n%f\n%f\n%f\n%f\n%f\n%f\n%f\n", d.get(), d.get(), d.get(), d.get(), d.get(), d.get(), d.get(), d.get(), d.get(), d.get());
//                            System.out.printf(">>\n%f\n%f\n%f\n%f\n%f\n%f\n%f\n%f\n%f\n%f\n", d.getFloat(), d.getFloat(), d.getFloat(), d.getFloat(), d.getFloat(), d.getFloat(), d.getFloat(), d.getFloat(), d.getFloat(), d.getFloat());
                            alBufferData(buffers.get(j), chan.leadinSample.objectInfo.nChannels == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, data, 44100);
//                                fc.write(d);
//                            System.out.println("  buffers2 " + AL10.alGetError());
                            chan.openalStreamingOffset += MIXBUFFER_SAMPLES;
                        }

                        for (int i = 0; i < finishedbuffers; i++) {
                            alSourceQueueBuffers(chan.openalSource, buffers.get(i));
                        }
                    }

                    // (re)start if needed..
                    if (chan.triggered) {
                        alSourcePlay(chan.openalSource);
                        chan.triggered = false;
                    }
                }
            } else {

                if (this.slowmoActive && !chan.disallowSlow) {
                    final idSlowChannel slow = sound.GetSlowChannel(chan);

                    slow.AttachSoundChannel(chan);

                    if (sample.objectInfo.nChannels == 2) {
                        // need to add a stereo path, but very few samples go through this
                        alignedInputSamples = new float[MIXBUFFER_SAMPLES * 2];//memset(alignedInputSamples, 0, sizeof(alignedInputSamples[0]) * MIXBUFFER_SAMPLES * 2);
                    } else {
                        slow.GatherChannelSamples(offset, MIXBUFFER_SAMPLES, alignedInputSamples);
                    }

                    sound.SetSlowChannel(chan, slow);
                } else {
                    sound.ResetSlowChannel(chan);

                    // if we are getting a stereo sample adjust accordingly
                    if (sample.objectInfo.nChannels == 2) {
                        // we should probably check to make sure any looping is also to a stereo sample...
                        chan.GatherChannelSamples(offset * 2, MIXBUFFER_SAMPLES * 2, FloatBuffer.wrap(alignedInputSamples));
                    } else {
                        chan.GatherChannelSamples(offset, MIXBUFFER_SAMPLES, FloatBuffer.wrap(alignedInputSamples));
                    }
                }

                //
                // work out the left / right ear values
                //
                final float[] ears = new float[6];
                if (global || omni) {
                    // same for all speakers
                    for (int i = 0; i < 6; i++) {
                        ears[i] = idSoundSystemLocal.s_globalFraction.GetFloat() * volume;
                    }
                    ears[3] = idSoundSystemLocal.s_subFraction.GetFloat() * volume;// subwoofer

                } else {
                    CalcEars(numSpeakers, spatializedOriginInMeters, this.listenerPos, this.listenerAxis, ears, spatialize);

                    for (int i = 0; i < 6; i++) {
                        ears[i] *= volume;
                    }
                }

                // if the mask is 0, it really means do every channel
                if (0 == mask) {
                    mask = 255;
                }
                // cleared mask bits set the mix volume to zero
                for (int i = 0; i < 6; i++) {
                    if (0 == (mask & (1 << i))) {
                        ears[i] = 0;
                    }
                }

                // if sounds are generally normalized, using a mixing volume over 1.0 will
                // almost always cause clipping noise.  If samples aren't normalized, there
                // is a good call to allow overvolumes
                if (idSoundSystemLocal.s_clipVolumes.GetBool() && (0 == (parms.soundShaderFlags & SSF_UNCLAMPED))) {
                    for (int i = 0; i < 6; i++) {
                        if (ears[i] > 1.0f) {
                            ears[i] = 1.0f;
                        }
                    }
                }

                // if this is the very first mixing block, set the lastV
                // to the current volume
                if (current44kHz == chan.trigger44kHzTime) {
                    for (j = 0; j < 6; j++) {
                        chan.lastV[j] = ears[j];
                    }
                }

                if (numSpeakers == 6) {
                    if (sample.objectInfo.nChannels == 1) {
                        SIMDProcessor.MixSoundSixSpeakerMono(finalMixBuffer, alignedInputSamples, MIXBUFFER_SAMPLES, chan.lastV, ears);
                    } else {
                        SIMDProcessor.MixSoundSixSpeakerStereo(finalMixBuffer, alignedInputSamples, MIXBUFFER_SAMPLES, chan.lastV, ears);
                    }
                } else {
                    if (sample.objectInfo.nChannels == 1) {
                        SIMDProcessor.MixSoundTwoSpeakerMono(finalMixBuffer, alignedInputSamples, MIXBUFFER_SAMPLES, chan.lastV, ears);
                    } else {
                        SIMDProcessor.MixSoundTwoSpeakerStereo(finalMixBuffer, alignedInputSamples, MIXBUFFER_SAMPLES, chan.lastV, ears);
                    }
                }

                for (j = 0; j < 6; j++) {
                    chan.lastV[j] = ears[j];
                }

            }
            soundSystemLocal.soundStats.activeSounds++;
        }

        /*
         ===================
         idSoundWorldLocal::MixLoop

         Sum all sound contributions into finalMixBuffer, an unclamped float buffer holding
         all output channels.  MIXBUFFER_SAMPLES samples will be created, with each sample consisting
         of 2 or 6 floats depending on numSpeakers.

         this is normally called from the sound thread, but also from the main thread
         for AVIdemo writing
         ===================
         */
        public void MixLoop(int current44kHz, int numSpeakers, float[] finalMixBuffer) {
            int i, j;
            idSoundEmitterLocal sound;

            // if noclip flying outside the world, leave silence
            if (this.listenerArea == -1) {
                if (idSoundSystemLocal.useOpenAL) {
                    alListenerf(AL_GAIN, 0.0f);
                }
                return;
            }

            // update the listener position and orientation
            if (idSoundSystemLocal.useOpenAL) {
                final float/*ALfloat*/[] listenerPosition = new float[3];

                listenerPosition[0] = -this.listenerPos.y;
                listenerPosition[1] = this.listenerPos.z;
                listenerPosition[2] = -this.listenerPos.x;

                final FloatBuffer listenerOrientation = Nio.newFloatBuffer(6);

                listenerOrientation.put(0, -this.listenerAxis.oGet(0).y);
                listenerOrientation.put(1, +this.listenerAxis.oGet(0).z);
                listenerOrientation.put(2, -this.listenerAxis.oGet(0).x);

                listenerOrientation.put(3, -this.listenerAxis.oGet(2).y);
                listenerOrientation.put(4, +this.listenerAxis.oGet(2).z);
                listenerOrientation.put(5, -this.listenerAxis.oGet(2).x);

                alListenerf(AL_GAIN, 1.0f);
                alListener3f(AL_POSITION, listenerPosition[0], listenerPosition[1], listenerPosition[2]);
                alListenerfv(AL_ORIENTATION, listenerOrientation);//SO6874122

// #if ID_OPENAL
                // if ( soundSystemLocal.s_useEAXReverb.GetBool() ) {
                // if ( soundSystemLocal.efxloaded ) {
                // idSoundEffect *effect = NULL;
                // int EnvironmentID = -1;
                // idStr defaultStr( "default" );
                // idStr listenerAreaStr( listenerArea );
                // soundSystemLocal.EFXDatabase.FindEffect( listenerAreaStr, &effect, &EnvironmentID );
                // if (!effect)
                // soundSystemLocal.EFXDatabase.FindEffect( listenerAreaName, &effect, &EnvironmentID );
                // if (!effect)
                // soundSystemLocal.EFXDatabase.FindEffect( defaultStr, &effect, &EnvironmentID );
                // // only update if change in settings
                // if ( soundSystemLocal.s_muteEAXReverb.GetBool() || ( listenerEnvironmentID != EnvironmentID ) ) {
                // EAXREVERBPROPERTIES EnvironmentParameters;
                // // get area reverb setting from EAX Manager
                // if ( ( effect ) && ( effect.data) && ( memcpy( &EnvironmentParameters, effect.data, effect.datasize ) ) ) {
                // if ( soundSystemLocal.s_muteEAXReverb.GetBool() ) {
                // EnvironmentParameters.lRoom = -10000;
                // EnvironmentID = -2;
// }
                // if ( soundSystemLocal.alEAXSet ) {
                // soundSystemLocal.alEAXSet( &EAXPROPERTYID_EAX_FXSlot0, EAXREVERB_ALLPARAMETERS, 0, &EnvironmentParameters, sizeof( EnvironmentParameters ) );
                // }
                // }
                // listenerEnvironmentID = EnvironmentID;
                // }
                // }
                // }
// #endif
            }

            // debugging option to mute all but a single soundEmitter
            if ((idSoundSystemLocal.s_singleEmitter.GetInteger() > 0) && (idSoundSystemLocal.s_singleEmitter.GetInteger() < this.emitters.Num())) {
                sound = this.emitters.oGet(idSoundSystemLocal.s_singleEmitter.GetInteger());

                if ((sound != null) && sound.playing) {
                    // run through all the channels
                    for (j = 0; j < SOUND_MAX_CHANNELS; j++) {
                        final idSoundChannel chan = sound.channels[j];

                        // see if we have a sound triggered on this channel
                        if (!chan.triggerState) {
                            chan.ALStop();
                            continue;
                        }

                        AddChannelContribution(sound, chan, current44kHz, numSpeakers, finalMixBuffer);
                    }
                }
                return;
            }

            for (i = 1; i < this.emitters.Num(); i++) {
                sound = this.emitters.oGet(i);

                if (null == sound) {
                    continue;
                }
                // if no channels are active, do nothing
                if (!sound.playing) {
                    continue;
                }
                // run through all the channels
                for (j = 0; j < SOUND_MAX_CHANNELS; j++) {
                    final idSoundChannel chan = sound.channels[j];

                    // see if we have a sound triggered on this channel
                    if (!chan.triggerState) {
                        chan.ALStop();
                        continue;
                    }

                    AddChannelContribution(sound, chan, current44kHz, numSpeakers, finalMixBuffer);
                }
            }

            if (!idSoundSystemLocal.useOpenAL && this.enviroSuitActive) {
                soundSystemLocal.DoEnviroSuit(finalMixBuffer, MIXBUFFER_SAMPLES, numSpeakers);
            }
        }

        /*
         ===================
         idSoundWorldLocal::AVIUpdate

         this is called by the main thread
         writes one block of sound samples if enough time has passed
         This can be used to write wave files even if no sound hardware exists
         ===================
         */
        public void AVIUpdate() {
            int numSpeakers;

            if ((this.game44kHz - this.lastAVI44kHz) < MIXBUFFER_SAMPLES) {
                return;
            }

            if (NOT(soundSystemLocal.snd_audio_hw)) {
                numSpeakers = 2;
            } else {
                numSpeakers = soundSystemLocal.snd_audio_hw.GetNumberOfSpeakers();
            }

//            float[] mix = new float[MIXBUFFER_SAMPLES * 6 + 16];
//            float[] mix_p = (float[]) (((int) mix + 15) & ~15);	// SIMD align
            final float[] mix_p = new float[(MIXBUFFER_SAMPLES * 6) + 16];

//            SIMDProcessor.Memset(mix_p, 0, MIXBUFFER_SAMPLES * sizeof(float) * numSpeakers);
//
            MixLoop(this.lastAVI44kHz, numSpeakers, mix_p);

            for (int i = 0; i < numSpeakers; i++) {
                final ByteBuffer outD = ByteBuffer.allocate(MIXBUFFER_SAMPLES * 2);

                for (int j = 0; j < MIXBUFFER_SAMPLES; j++) {
                    final float s = mix_p[(j * numSpeakers) + i];
                    if (s < -32768.0f) {
                        outD.putShort(Short.MIN_VALUE);
                    } else if (s > 32767.0f) {
                        outD.putShort(Short.MAX_VALUE);
                    } else {
                        outD.putShort((short) idMath.FtoiFast(s));
                    }
                }
                // write to file
                this.fpa[i].Write(outD);//, MIXBUFFER_SAMPLES * sizeof(short));
            }

            this.lastAVI44kHz += MIXBUFFER_SAMPLES;

            return;
        }

        /*
         ===================
         idSoundWorldLocal::ResolveOrigin

         Find out of the sound is completely occluded by a closed door portal, or
         the virtual sound origin position at the portal closest to the listener.
         this is called by the main thread

         dist is the distance from the orignial sound origin to the current portal that enters soundArea
         def->distance is the distance we are trying to reduce.

         If there is no path through open portals from the sound to the listener, def->distance will remain
         set at maxDistance
         ===================
         */
        static final int MAX_PORTAL_TRACE_DEPTH = 10;

        public void ResolveOrigin(final int stackDepth, final soundPortalTrace_s prevStack, final int soundArea, final float dist, final idVec3 soundOrigin, idSoundEmitterLocal def) {

            if (dist >= def.distance) {
                // we can't possibly hear the sound through this chain of portals
                return;
            }

            if (soundArea == this.listenerArea) {
                final float fullDist = dist + (soundOrigin.oMinus(this.listenerQU)).LengthFast();
                if (fullDist < def.distance) {
                    def.distance = fullDist;
                    def.spatializedOrigin = soundOrigin;
                }
                return;
            }

            if (stackDepth == MAX_PORTAL_TRACE_DEPTH) {
                // don't spend too much time doing these calculations in big maps
                return;
            }

            final soundPortalTrace_s newStack = new soundPortalTrace_s();
            newStack.portalArea = soundArea;
            newStack.prevStack = prevStack;

            final int numPortals = this.rw.NumPortalsInArea(soundArea);
            for (int p = 0; p < numPortals; p++) {
                final exitPortal_t re = this.rw.GetPortal(soundArea, p);

                float occlusionDistance = 0;

                // air blocking windows will block sound like closed doors
                if (0 == (re.blockingBits & (etoi(PS_BLOCK_VIEW) | etoi(PS_BLOCK_AIR)))) {
                    // we could just completely cut sound off, but reducing the volume works better
                    // continue;
                    occlusionDistance = idSoundSystemLocal.s_doorDistanceAdd.GetFloat();
                }

                // what area are we about to go look at
                int otherArea = re.areas[0];
                if (re.areas[0] == soundArea) {
                    otherArea = re.areas[1];
                }

                // if this area is already in our portal chain, don't bother looking into it
                soundPortalTrace_s prev;
                for (prev = prevStack; prev != null; prev = prev.prevStack) {
                    if (prev.portalArea == otherArea) {
                        break;
                    }
                }
                if (prev != null) {
                    continue;
                }

                // pick a point on the portal to serve as our virtual sound origin
// #if 1
                idVec3 source;

                final idPlane pl = new idPlane();
                re.w.GetPlane(pl);

                final float[] scale = {0};
                final idVec3 dir = this.listenerQU.oMinus(soundOrigin);
                if (!pl.RayIntersection(soundOrigin, dir, scale)) {
                    source = re.w.GetCenter();
                } else {
                    source = soundOrigin.oPlus(dir.oMultiply(scale[0]));

                    // if this point isn't inside the portal edges, slide it in
                    for (int i = 0; i < re.w.GetNumPoints(); i++) {
                        final int j = (i + 1) % re.w.GetNumPoints();
                        final idVec3 edgeDir = re.w.oGet(j).ToVec3().oMinus(re.w.oGet(i).ToVec3());
                        final idVec3 edgeNormal = new idVec3();

                        edgeNormal.Cross(pl.Normal(), edgeDir);

                        final idVec3 fromVert = source.oMinus(re.w.oGet(j).ToVec3());

                        float d = edgeNormal.oMultiply(fromVert);
                        if (d > 0) {
                            // move it in
                            final float div = edgeNormal.Normalize();
                            d /= div;

                            source.oMinSet(edgeNormal.oMultiply(d));
                        }
                    }
                }
// #else
                // // clip the ray from the listener to the center of the portal by
                // // all the portal edge planes, then project that point (or the original if not clipped)
                // // onto the portal plane to get the spatialized origin

                // idVec3	start = listenerQU;
                // idVec3	mid = re.w.GetCenter();
                // bool	wasClipped = false;
                // for ( int i = 0 ; i < re.w.GetNumPoints() ; i++ ) {
                // int j = ( i + 1 ) % re.w.GetNumPoints();
                // idVec3	v1 = (*(re.w))[j].ToVec3() - soundOrigin;
                // idVec3	v2 = (*(re.w))[i].ToVec3() - soundOrigin;
                // v1.Normalize();
                // v2.Normalize();
                // idVec3	edgeNormal;
                // edgeNormal.Cross( v1, v2 );
                // idVec3	fromVert = start - soundOrigin;
                // float	d1 = edgeNormal * fromVert;
                // if ( d1 > 0.0f ) {
                // fromVert = mid - (*(re.w))[j].ToVec3();
                // float d2 = edgeNormal * fromVert;
                // // move it in
                // float	f = d1 / ( d1 - d2 );
                // idVec3	clipped = start * ( 1.0f - f ) + mid * f;
                // start = clipped;
                // wasClipped = true;
                // }
                // }
                // idVec3	source;
                // if ( wasClipped ) {
                // // now project it onto the portal plane
                // idPlane	pl;
                // re.w.GetPlane( pl );
                // float	f1 = pl.Distance( start );
                // float	f2 = pl.Distance( soundOrigin );
                // float	f = f1 / ( f1 - f2 );
                // source = start * ( 1.0f - f ) + soundOrigin * f;
                // } else {
                // source = soundOrigin;
                // }
// #endif
                final idVec3 tlen = source.oMinus(soundOrigin);
                final float tlenLength = tlen.LengthFast();

                ResolveOrigin(stackDepth + 1, newStack, otherArea, dist + tlenLength + occlusionDistance, source, def);
            }
        }

        /*
         ===============
         idSoundWorldLocal::FindAmplitude

         this is called from the main thread

         if listenerPosition is NULL, this is being used for shader parameters,
         like flashing lights and glows based on sound level.  Otherwise, it is being used for
         the screen-shake on a player.

         This doesn't do the portal-occlusion currently, because it would have to reset all the defs
         which would be problematic in multiplayer
         ===============
         */
        private static final int AMPLITUDE_SAMPLES = MIXBUFFER_SAMPLES / 8;

        public float FindAmplitude(idSoundEmitterLocal sound, final int localTime, final idVec3 listenerPosition, final int/*s_channelType*/ channel, boolean shakesOnly) {
            int i, j;
            soundShaderParms_t parms;
            float volume;
            int activeChannelCount;
            final float[] sourceBuffer = new float[AMPLITUDE_SAMPLES];
            final float[] sumBuffer = new float[AMPLITUDE_SAMPLES];
            // work out the distance from the listener to the emitter
            float dlen;

            if (!sound.playing) {
                return 0;
            }

            if (listenerPosition != null) {
                // this doesn't do the portal spatialization
                final idVec3 dist = sound.origin.oMinus(listenerPosition);
                dlen = dist.Length();
                dlen *= DOOM_TO_METERS;
            } else {
                dlen = 1;
            }

            activeChannelCount = 0;

            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                final idSoundChannel chan = sound.channels[i];

                if (!chan.triggerState) {
                    continue;
                }

                if ((channel != SCHANNEL_ANY) && (chan.triggerChannel != channel)) {
                    continue;
                }

                parms = chan.parms;

                final int localTriggerTimes = chan.trigger44kHzTime;

                final boolean looping = (parms.soundShaderFlags & SSF_LOOPING) != 0;

                // check for screen shakes
                final float shakes = parms.shakes;
                if (shakesOnly && (shakes <= 0.0f)) {
                    continue;
                }

                //
                // calculate volume
                //
                if (null == listenerPosition) {
                    // just look at the raw wav data for light shader evaluation
                    volume = 1.0f;
                } else {
                    volume = parms.volume;
                    volume = soundSystemLocal.dB2Scale(volume);
                    if (shakesOnly) {
                        volume *= shakes;
                    }

                    if ((listenerPosition != null) && (0 == (parms.soundShaderFlags & SSF_GLOBAL))) {
                        // check for overrides
                        final float maxd = parms.maxDistance;
                        final float mind = parms.minDistance;

                        if (dlen >= maxd) {
                            volume = 0.0f;
                        } else if (dlen > mind) {
                            float frac = idMath.ClampFloat(0, 1, 1.0f - ((dlen - mind) / (maxd - mind)));
                            if (idSoundSystemLocal.s_quadraticFalloff.GetBool()) {
                                frac *= frac;
                            }
                            volume *= frac;
                        }
                    }
                }

                if (volume <= 0) {
                    continue;
                }

                //
                // fetch the sound from the cache
                // this doesn't handle stereo samples correctly...
                //
                if ((null == listenerPosition) && ((chan.parms.soundShaderFlags != 0) & (SSF_NO_FLICKER != 0))) {
                    // the NO_FLICKER option is to allow a light to still play a sound, but
                    // not have it effect the intensity
                    for (j = 0; j < (AMPLITUDE_SAMPLES); j++) {
                        sourceBuffer[j] = (j & 1) == 1 ? 32767.0f : -32767.0f;
                    }
                } else {
                    int offset = Math.abs(localTime - localTriggerTimes);    // offset in samples
                    final int size = (looping ? chan.soundShader.entries[0].LengthIn44kHzSamples() : chan.leadinSample.LengthIn44kHzSamples());
                    final ByteBuffer plitudeData = looping ? chan.soundShader.entries[0].amplitudeData : chan.leadinSample.amplitudeData;

                    if (plitudeData != null) {
                        final ShortBuffer amplitudeData = plitudeData.asShortBuffer();
                        // when the amplitudeData is present use that fill a dummy sourceBuffer
                        // this is to allow for amplitude based effect on hardware audio solutions
                        if (looping) {
                            offset %= size;
                        }
                        if (offset < size) {
                            for (j = 0; j < (AMPLITUDE_SAMPLES); j++) {
                                sourceBuffer[j] = (j & 1) == 1 ? amplitudeData.get((offset / 512) * 2) : amplitudeData.get(((offset / 512) * 2) + 1);
                            }
                        }
                    } else {
                        // get actual sample data
                        chan.GatherChannelSamples(offset, AMPLITUDE_SAMPLES, FloatBuffer.wrap(sourceBuffer));
                    }
                }
                activeChannelCount++;
                if (activeChannelCount == 1) {
                    // store to the buffer
                    for (j = 0; j < AMPLITUDE_SAMPLES; j++) {
                        sumBuffer[j] = volume * sourceBuffer[j];
                    }
                } else {
                    // add to the buffer
                    for (j = 0; j < AMPLITUDE_SAMPLES; j++) {
                        sumBuffer[j] += volume * sourceBuffer[j];
                    }
                }
            }

            if (activeChannelCount == 0) {
                return 0.0f;
            }

            float high = -32767.0f;
            float low = 32767.0f;

            // use a 20th of a second
            for (i = 0; i < (AMPLITUDE_SAMPLES); i++) {
                final float fabval = sumBuffer[i];
                if (high < fabval) {
                    high = fabval;
                }
                if (low > fabval) {
                    low = fabval;
                }
            }

            float sout;
            sout = (float) (atan((high - low) / 32767.0f) / DEG2RAD(45));

            return sout;
        }

    }
}
