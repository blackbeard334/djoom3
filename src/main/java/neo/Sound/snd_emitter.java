package neo.Sound;

import static neo.Sound.snd_local.PRIMARYFREQ;
import static neo.Sound.snd_local.SOUND_DECODER_FREE_DELAY;
import static neo.Sound.snd_local.SOUND_MAX_CHANNELS;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_FADE;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_FREE;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_MODIFY;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_START;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_STOP;
import static neo.Sound.snd_local.soundDemoCommand_t.SCMD_UPDATE;
import static neo.Sound.snd_shader.DOOM_TO_METERS;
import static neo.Sound.snd_shader.METERS_TO_DOOM;
import static neo.Sound.snd_shader.SSF_LOOPING;
import static neo.Sound.snd_shader.SSF_NO_DUPS;
import static neo.Sound.snd_shader.SSF_PLAY_ONCE;
import static neo.Sound.snd_system.soundSystemLocal;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.indexOf;
import static neo.framework.Common.common;
import static neo.framework.DemoFile.demoSystem_t.DS_SOUND;
import static neo.framework.Session.session;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;
import static neo.openal.QAL.alDeleteBuffers;
import static neo.openal.QAL.alGetError;
import static neo.openal.QAL.alGetSourcei;
import static neo.openal.QAL.alIsSource;
import static neo.openal.QAL.alSourceStop;
import static neo.openal.QAL.alSourcei;
import static neo.openal.QALConstantsIfc.AL_BUFFER;
import static neo.openal.QALConstantsIfc.AL_NO_ERROR;
import static neo.openal.QALConstantsIfc.AL_PLAYING;
import static neo.openal.QALConstantsIfc.AL_SOURCE_STATE;
import static neo.openal.QALConstantsIfc.AL_STOPPED;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;
import static neo.sys.win_shared.Sys_Milliseconds;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Sound.snd_cache.idSoundSample;
import neo.Sound.snd_local.idSampleDecoder;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.snd_shader.soundShaderParms_t;
import neo.Sound.snd_system.idSoundSystemLocal;
import neo.Sound.snd_world.idSoundWorldLocal;
import neo.Sound.sound.idSoundEmitter;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import neo.opengl.Nio;

/**
 *
 */
public class snd_emitter {
//    typedef enum {

    public static final int REMOVE_STATUS_INVALID            = -1;
    public static final int REMOVE_STATUS_ALIVE              = 0;
    public static final int REMOVE_STATUS_WAITSAMPLEFINISHED = 1;
    public static final int REMOVE_STATUS_SAMPLEFINISHED     = 2;
//} removeStatus_t;

    static class idSoundFade {

        public int   fadeStart44kHz;
        public int   fadeEnd44kHz;
        public float fadeStartVolume;  // in dB
        public float fadeEndVolume;    // in dB
//
//

        public void Clear() {
            this.fadeStart44kHz = 0;
            this.fadeEnd44kHz = 0;
            this.fadeStartVolume = 0;
            this.fadeEndVolume = 0;
        }

        public float FadeDbAt44kHz(int current44kHz) {
            float fadeDb;

            if (current44kHz >= this.fadeEnd44kHz) {
                fadeDb = this.fadeEndVolume;
            } else if (current44kHz > this.fadeStart44kHz) {
                final float fraction = (this.fadeEnd44kHz - this.fadeStart44kHz);
                final float over = (current44kHz - this.fadeStart44kHz);
                fadeDb = this.fadeStartVolume + (((this.fadeEndVolume - this.fadeStartVolume) * over) / fraction);
            } else {
                fadeDb = this.fadeStartVolume;
            }
            return fadeDb;
        }
    }

    static class SoundFX {

        protected boolean initialized;
        //
        protected int     channel;
        protected int     maxlen;
        //
        protected float[] buffer;
        protected float[] continuitySamples = new float[4];
        //
        protected float param;
//
//

        public SoundFX() {
            this.channel = 0;
            this.buffer = null;
            this.initialized = false;
            this.maxlen = 0;
//            memset(continuitySamples, 0, sizeof(float) * 4);
        }

        //	virtual				~SoundFX()										{ if ( buffer ) delete buffer; };
        public void Initialize() {
        }

        public void ProcessSample(float[] in, int in_offset, float[] out, int out_offset) {
        }

        public void SetChannel(int chan) {
            this.channel = chan;
        }

        public int GetChannel() {
            return this.channel;
        }

        public void SetContinuitySamples(float in1, float in2, float out1, float out2) {
            this.continuitySamples[0] = in1;
            this.continuitySamples[1] = in2;
            this.continuitySamples[2] = out1;
            this.continuitySamples[3] = out2;
        }

        // FIXME?
        public void GetContinuitySamples(float[] in1, float[] in2, float[] out1, float[] out2) {
            in1[0] = this.continuitySamples[0];
            in2[0] = this.continuitySamples[1];
            out1[0] = this.continuitySamples[2];
            out2[0] = this.continuitySamples[3];
        }

        public void SetParameter(float val) {
            this.param = val;
        }
    }

    static class SoundFX_Lowpass extends SoundFX {

        @Override
        public void ProcessSample(float[] in, int in_offset, float[] out, int out_offset) {
            float c, a1, a2, a3, b1, b2;
            final float resonance = idSoundSystemLocal.s_enviroSuitCutoffQ.GetFloat();
            final float cutoffFrequency = idSoundSystemLocal.s_enviroSuitCutoffFreq.GetFloat();

            Initialize();

            c = 1.0f / idMath.Tan16((idMath.PI * cutoffFrequency) / 44100);

            // compute coefs
            a1 = 1.0f / (1.0f + (resonance * c) + (c * c));
            a2 = 2 * a1;
            a3 = a1;
            b1 = 2.0f * (1.0f - (c * c)) * a1;
            b2 = ((1.0f - (resonance * c)) + (c * c)) * a1;

            // compute output value
            out[out_offset + 0] = ((a1 * in[in_offset + 0])
                    + (a2 * in[in_offset + -1])
                    + (a3 * in[in_offset + -2]))
                    - (b1 * out[out_offset + -1])
                    - (b2 * out[out_offset + -2]);
        }
    }

    static class SoundFX_LowpassFast extends SoundFX {

        float freq;
        float res;
        float a1, a2, a3;
        float b1, b2;
//        
//        

        @Override
        public void ProcessSample(float[] in, final int inOffset, float[] out, final int outOffset) {
            // compute output value
            out[outOffset + 0] = ((this.a1 * in[inOffset + 0])
                    + (this.a2 * in[inOffset - 1])
                    + (this.a3 * in[inOffset - 2]))
                    - (this.b1 * out[outOffset - 1])
                    - (this.b2 * out[outOffset - 2]);
        }

        public void SetParms(float p1 /*= 0*/, float p2 /*= 0*/, float p3 /*= 0*/) {
            float c;

            // set the vars
            this.freq = p1;
            this.res = p2;

            // precompute the coefs
            c = 1.0f / idMath.Tan((idMath.PI * this.freq) / 44100);

            // compute coefs
            this.a1 = 1.0f / (1.0f + (this.res * c) + (c * c));
            this.a2 = 2 * this.a1;
            this.a3 = this.a1;

            this.b1 = 2.0f * (1.0f - (c * c)) * this.a1;
            this.b2 = ((1.0f - (this.res * c)) + (c * c)) * this.a1;
        }

        public void SetParms(float p1 /*= 0*/, float p2 /*= 0*/) {
            SetParms(p1, p2, 0);
        }

        public void SetParms(float p1 /*= 0*/) {
            SetParms(p1, 0);
        }

        public void SetParms() {
            SetParms(0);
        }
    }

    static class SoundFX_Comb extends SoundFX {

        int currentTime;
//        
//        

        @Override
        public void Initialize() {
            if (this.initialized) {
                return;
            }

            this.initialized = true;
            this.maxlen = 50000;
            this.buffer = new float[this.maxlen];
            this.currentTime = 0;
        }

        @Override
        public void ProcessSample(float[] in, int in_offset, float[] out, int out_offset) {
            final float gain = idSoundSystemLocal.s_reverbFeedback.GetFloat();
            final int len = (int) (idSoundSystemLocal.s_reverbTime.GetFloat() + this.param);

            Initialize();

            // sum up and output
            out[out_offset + 0] = this.buffer[this.currentTime];
            this.buffer[this.currentTime] = (this.buffer[this.currentTime] * gain) + in[in_offset + 0];

            // increment current time
            this.currentTime++;
            if (this.currentTime >= len) {
                this.currentTime -= len;
            }
        }
    }

    static class FracTime {

        public int   time;
        public float frac;
//
//

        public void Set(int val) {
            this.time = val;
            this.frac = 0;
        }

        public void Increment(float val) {
            this.frac += val;
            while (this.frac >= 1.f) {
                this.time++;
                this.frac--;
            }
        }
    }
    
    //enum {
    public static final int PLAYBACK_RESET     = 0;
    public static final int PLAYBACK_ADVANCING = 1;
//};

    static class idSlowChannel {

        boolean        active;
        idSoundChannel chan;
        //
        int            playbackState;
        int            triggerOffset;
        //
        FracTime newPosition = new FracTime();
        int newSampleOffset;
        //
        FracTime curPosition = new FracTime();
        int                 curSampleOffset;
        //
        SoundFX_LowpassFast lowpass;
//
//

        // functions
        void GenerateSlowChannel(FracTime playPos, int sampleCount44k, float[] finalBuffer) {
            final idSoundWorldLocal sw = (idSoundWorldLocal) soundSystemLocal.GetPlayingSoundWorld();
            final float[] in = new float[MIXBUFFER_SAMPLES + 3], out = new float[MIXBUFFER_SAMPLES + 3];
            final float[] src = new float[(MIXBUFFER_SAMPLES + 3) - 2], spline = new float[(MIXBUFFER_SAMPLES + 3) - 2];
//            int src, spline;
            float slowmoSpeed;
            int i, neededSamples, orgTime, zeroedPos, count = 0;

//            src = in + 2;
//            spline = out + 2;
            if (sw != null) {
                slowmoSpeed = sw.slowmoSpeed;
            } else {
                slowmoSpeed = 1;
            }

            neededSamples = (int) ((sampleCount44k * slowmoSpeed) + 4);
            orgTime = playPos.time;

            // get the channel's samples
            this.chan.GatherChannelSamples(playPos.time * 2, neededSamples, FloatBuffer.wrap(src));
            for (i = 0; i < (neededSamples >> 1); i++) {
                spline[i] = src[i * 2];
            }

            // interpolate channel
            zeroedPos = playPos.time;
            playPos.time = 0;

            for (i = 0; i < (sampleCount44k >> 1); i++, count += 2) {
                float val;
                val = spline[playPos.time];
                src[i] = val;
                playPos.Increment(slowmoSpeed);
            }

            // lowpass filter
//            float *in_p = in + 2, *out_p = out + 2;
            final float[] in_p1 = {0}, in_p2 = {0}, out_p1 = {0}, out_p2 = {0};
            final int numSamples = sampleCount44k >> 1;

            this.lowpass.GetContinuitySamples(in_p1, in_p2, out_p1, out_p2);
            this.lowpass.SetParms(slowmoSpeed * 15000, 1.2f, 9);

            System.arraycopy(src, 0, in, 2, (MIXBUFFER_SAMPLES + 3) - 2);
            System.arraycopy(spline, 0, out, 2, (MIXBUFFER_SAMPLES + 3) - 2);
            in[0] = in_p1[0];//FIXME:ugly block.
            in[1] = in_p2[0];
            out[0] = out_p1[0];
            out[1] = out_p2[0];

            for (i = 0, count = 0; i < numSamples; i++, count += 2) {
                this.lowpass.ProcessSample(in, 2 + i, out, 2 + i);
                finalBuffer[count] = finalBuffer[count + 1] = out[i];
            }

            this.lowpass.SetContinuitySamples(in[(2 + numSamples) - 2], in[(2 + numSamples) - 3], out[(2 + numSamples) - 2], out[(2 + numSamples) - 3]);//2 = pointer offset

            playPos.time += zeroedPos;
        }

        float GetSlowmoSpeed() {
            final idSoundWorldLocal sw = (idSoundWorldLocal) soundSystemLocal.GetPlayingSoundWorld();

            if (sw != null) {
                return sw.slowmoSpeed;
            } else {
                return 0;
            }
        }

        public void AttachSoundChannel(final idSoundChannel chan) {
            this.chan = chan;
        }

        public void Reset() {
//	memset( this, 0, sizeof( *this ) );//TODO:

//	this.chan = chan;
            this.curPosition.Set(0);
            this.newPosition.Set(0);

            this.curSampleOffset = -10000;
            this.newSampleOffset = -10000;

            this.triggerOffset = 0;
        }

        public void GatherChannelSamples(int sampleOffset44k, int sampleCount44k, float[] dest) {
            int state = 0;

            // setup chan
            this.active = true;
            this.newSampleOffset = sampleOffset44k >> 1;

            // set state
            if (this.newSampleOffset < this.curSampleOffset) {
                state = PLAYBACK_RESET;
            } else if (this.newSampleOffset > this.curSampleOffset) {
                state = PLAYBACK_ADVANCING;
            }

            if (state == PLAYBACK_RESET) {
                this.curPosition.Set(this.newSampleOffset);
            }

            // set current vars
            this.curSampleOffset = this.newSampleOffset;
            this.newPosition = this.curPosition;

            // do the slow processing
            GenerateSlowChannel(this.newPosition, sampleCount44k, dest);

            // finish off
            if (state == PLAYBACK_ADVANCING) {
                this.curPosition = this.newPosition;
            }
        }

        public boolean IsActive() {
            return this.active;
        }

        public FracTime GetCurrentPosition() {
            return this.curPosition;
        }
    }

    static class idSoundChannel {

        public boolean              triggerState;
        public int                  trigger44kHzTime;        // hardware time sample the channel started
        public int                  triggerGame44kHzTime;    // game time sample time the channel started
        public soundShaderParms_t   parms;    // combines the shader parms and the per-channel overrides
        public idSoundSample        leadinSample;    // if not looped, this is the only sample
        public int/*s_channelType*/ triggerChannel;
        public idSoundShader        soundShader;
        public idSampleDecoder      decoder;
        public float                diversity;
        public float                lastVolume;        // last calculated volume based on distance
        public float[] lastV = new float[6];    // last calculated volume for each speaker, so we can smoothly fade
        public idSoundFade         channelFade;
        public boolean             triggered;
        public int/*ALuint*/       openalSource;
        public int/*ALuint*/       openalStreamingOffset;
        public IntBuffer/*ALuint*/ openalStreamingBuffer;
        public IntBuffer/*ALuint*/ lastopenalStreamingBuffer;
        //
        public boolean             disallowSlow;
        //
        //

        public idSoundChannel() {
            this.decoder = null;
            this.channelFade = new idSoundFade();
            this.openalStreamingBuffer = Nio.newIntBuffer(3);
            this.lastopenalStreamingBuffer = Nio.newIntBuffer(3);
            Clear();
        }
//						~idSoundChannel( void );

        public void Clear() {
            int j;

            Stop();
            this.soundShader = null;
            this.lastVolume = 0.0f;
            this.triggerChannel = SCHANNEL_ANY;
            this.channelFade.Clear();
            this.diversity = 0.0f;
            this.leadinSample = null;
            this.trigger44kHzTime = 0;
            for (j = 0; j < 6; j++) {
                this.lastV[j] = 0.0f;
            }
//	memset( &parms, 0, sizeof(parms) );
            this.parms = new soundShaderParms_t();

            this.triggered = false;
            this.openalSource = 0;//null;
            this.openalStreamingOffset = 0;
//            openalStreamingBuffer[0] = openalStreamingBuffer[1] = openalStreamingBuffer[2] = 0;
//            lastopenalStreamingBuffer[0] = lastopenalStreamingBuffer[1] = lastopenalStreamingBuffer[2] = 0;
            this.openalStreamingBuffer.clear();
            this.lastopenalStreamingBuffer.clear();
        }

        public void Start() {
            this.triggerState = true;
            if (this.decoder == null) {
                this.decoder = idSampleDecoder.Alloc();
            }
        }

        public void Stop() {
            this.triggerState = false;
            if (this.decoder != null) {
                idSampleDecoder.Free(this.decoder);
                this.decoder = null;
            }
        }

        /*
         ===================
         idSoundChannel::GatherChannelSamples

         Will always return 44kHz samples for the given range, even if it deeply looped or
         out of the range of the unlooped samples.  Handles looping between multiple different
         samples and leadins
         ===================
         */
        public void GatherChannelSamples(int sampleOffset44k, int sampleCount44k, FloatBuffer dest) {
            int dest_p = 0;
            int len;

//Sys_DebugPrintf( "msec:%i sample:%i : %i : %i\n", Sys_Milliseconds(), soundSystemLocal.GetCurrent44kHzTime(), sampleOffset44k, sampleCount44k );	//!@#
            // negative offset times will just zero fill
            if (sampleOffset44k < 0) {
                len = -sampleOffset44k;
                if (len > sampleCount44k) {
                    len = sampleCount44k;
                }
//		memset( dest_p, 0, len * sizeof( dest_p[0] ) );
//                dest.clear();
                dest_p += len;
                sampleCount44k -= len;
                sampleOffset44k += len;
            }

            // grab part of the leadin sample
            final idSoundSample leadin = this.leadinSample;
            if (NOT(leadin) || (sampleOffset44k < 0) || (sampleCount44k <= 0)) {
//		memset( dest_p, 0, sampleCount44k * sizeof( dest_p[0] ) );
//                dest.clear();
                return;
            }

            if (sampleOffset44k < leadin.LengthIn44kHzSamples()) {
                len = leadin.LengthIn44kHzSamples() - sampleOffset44k;
                if (len > sampleCount44k) {
                    len = sampleCount44k;
                }

                // decode the sample
                this.decoder.Decode(leadin, sampleOffset44k, len, dest);

                dest.position(dest_p += len);
                sampleCount44k -= len;
                sampleOffset44k += len;
            }

            // if not looping, zero fill any remaining spots
            if ((null == this.soundShader) || (0 == (this.parms.soundShaderFlags & SSF_LOOPING))) {
//		memset( dest_p, 0, sampleCount44k * sizeof( dest_p[0] ) );
//                dest.clear();
                return;
            }

            // fill the remainder with looped samples
            final idSoundSample loop = this.soundShader.entries[0];

            if (null == loop) {
//		memset( dest_p, 0, sampleCount44k * sizeof( dest_p[0] ) );
//                dest.clear();
                return;
            }

            sampleOffset44k -= leadin.LengthIn44kHzSamples();

            while (sampleCount44k > 0) {
                final int totalLen = loop.LengthIn44kHzSamples();

                sampleOffset44k %= totalLen;

                len = totalLen - sampleOffset44k;
                if (len > sampleCount44k) {
                    len = sampleCount44k;
                }

                // decode the sample
                this.decoder.Decode(loop, sampleOffset44k, len, dest);//TODO:

                dest.position(dest_p += len);
                sampleCount44k -= len;
                sampleOffset44k += len;
            }
        }

        public void ALStop() {			// free OpenAL resources if any
            if (idSoundSystemLocal.useOpenAL) {

                if (alIsSource(this.openalSource)) {
                    alSourceStop(this.openalSource);
                    alSourcei(this.openalSource, AL_BUFFER, 0);
                    soundSystemLocal.FreeOpenALSource(this.openalSource);
                }

                if ((this.openalStreamingBuffer.get(0) != 0) && (this.openalStreamingBuffer.get(1) != 0) && (this.openalStreamingBuffer.get(2) != 0)) {
                    alGetError();
//                    alDeleteBuffers(3, openalStreamingBuffer[0]);
                    alDeleteBuffers(this.openalStreamingBuffer);
                    if (alGetError() == AL_NO_ERROR) {
                        this.openalStreamingBuffer.clear();
                    }
                }

                if ((this.lastopenalStreamingBuffer.get(0) != 0) && (this.lastopenalStreamingBuffer.get(1) != 0) && (this.lastopenalStreamingBuffer.get(2) != 0)) {
                    alGetError();
//                    alDeleteBuffers(3, lastopenalStreamingBuffer[0]);
                    alDeleteBuffers(this.lastopenalStreamingBuffer);
                    if (alGetError() == AL_NO_ERROR) {
                        this.lastopenalStreamingBuffer.clear();
                    }
                }
            }
        }
    }
    
    /*
     ===============================================================================

     SOUND EMITTER

     ===============================================================================
     */
    // sound channels
    static final int SCHANNEL_ANY = 0;    // used in queries and commands to effect every channel at once, in
    // startSound to have it not override any other channel
    static final int SCHANNEL_ONE = 1;    // any following integer can be used as a channel number
    // typedef int s_channelType;	  // the game uses its own series of enums, and we don't want to require casts

    static class idSoundEmitterLocal extends idSoundEmitter {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public idSoundWorldLocal     soundWorld;            // the world that holds this emitter
        //
        public int                   index;                 // in world emitter list
        public int/*removeStatus_t*/ removeStatus;
        //
        public idVec3                origin;
        public int                   listenerId;
        public soundShaderParms_t    parms;                 // default overrides for all channels
        //
        //
        // the following are calculated in UpdateEmitter, and don't need to be archived
        public float                 maxDistance;           // greatest of all playing channel distances
        public int                   lastValidPortalArea;   // so an emitter that slides out of the world continues playing
        public boolean               playing;               // if false, no channel is active
        public boolean               hasShakes;
        public idVec3                spatializedOrigin;     // the virtual sound origin, either the real sound origin,
        //						    // or a point through a portal chain
        public float                 realDistance;          // in meters
        public float                 distance;              // in meters, this may be the straight-line distance, or
        public idSoundChannel[]      channels;
        public idSlowChannel[]       slowChannels;
        //
        // this is just used for feedback to the game or rendering system:
        // flashing lights and screen shakes.  Because the material expression
        // evaluation doesn't do common subexpression removal, we cache the
        // last generated value
        public int   ampTime;
        public float amplitude;
        //
        //

        public idSoundEmitterLocal() {
            this.soundWorld = null;
            this.origin = new idVec3();
            this.spatializedOrigin = new idVec3();

            this.channels = new idSoundChannel[SOUND_MAX_CHANNELS];
            for (int c = 0; c < this.channels.length; c++) {
                this.channels[c] = new idSoundChannel();
            }

            this.slowChannels = new idSlowChannel[SOUND_MAX_CHANNELS];
            for (int s = 0; s < this.slowChannels.length; s++) {
                this.slowChannels[s] = new idSlowChannel();
            }
            Clear();
        }
        //----------------------------------------------

        // the "time" parameters should be game time in msec, which is used to make queries
        // return deterministic values regardless of async buffer scheduling

        /*
         =====================
         idSoundEmitterLocal::Free

         They are never truly freed, just marked so they can be reused by the soundWorld
         // a non-immediate free will let all currently playing sounds complete
         =====================
         */
        @Override
        public void Free(boolean immediate) {
            if (this.removeStatus != REMOVE_STATUS_ALIVE) {
                return;
            }

            if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                common.Printf("FreeSound (%d,%d)\n", this.index, immediate);
            }
            if ((this.soundWorld != null) && (this.soundWorld.writeDemo != null)) {
                this.soundWorld.writeDemo.WriteInt(DS_SOUND);
                this.soundWorld.writeDemo.WriteInt(SCMD_FREE);
                this.soundWorld.writeDemo.WriteInt(this.index);
                this.soundWorld.writeDemo.WriteInt(btoi(immediate));
            }

            if (!immediate) {
                this.removeStatus = REMOVE_STATUS_WAITSAMPLEFINISHED;
            } else {
                Clear();
            }
        }

        // the parms specified will be the default overrides for all sounds started on this emitter.
        // NULL is acceptable for parms
        @Override
        public void UpdateEmitter(final idVec3 origin, int listenerId, final soundShaderParms_t parms) {
            if (null == parms) {
                common.Error("idSoundEmitterLocal::UpdateEmitter: NULL parms");
            }
            if ((this.soundWorld != null) && (this.soundWorld.writeDemo != null)) {
                this.soundWorld.writeDemo.WriteInt(DS_SOUND);
                this.soundWorld.writeDemo.WriteInt(SCMD_UPDATE);
                this.soundWorld.writeDemo.WriteInt(this.index);
                this.soundWorld.writeDemo.WriteVec3(origin);
                this.soundWorld.writeDemo.WriteInt(listenerId);
                this.soundWorld.writeDemo.WriteFloat(parms.minDistance);
                this.soundWorld.writeDemo.WriteFloat(parms.maxDistance);
                this.soundWorld.writeDemo.WriteFloat(parms.volume);
                this.soundWorld.writeDemo.WriteFloat(parms.shakes);
                this.soundWorld.writeDemo.WriteInt(parms.soundShaderFlags);
                this.soundWorld.writeDemo.WriteInt(parms.soundClass);
            }

            this.origin = origin;
            this.listenerId = listenerId;
            this.parms = parms;

            // FIXME: change values on all channels?
        }

        /*
         =====================
         idSoundEmitterLocal::StartSound

         returns the length of the started sound in msec
         =====================
         */
        @Override
        public int StartSound(final idSoundShader shader, final int channel, float diversity /*= 0*/, int soundShaderFlags /*= 0*/, boolean allowSlow /*= true*/) {
            int i;

            if (null == shader) {
                return 0;
            }

            if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                common.Printf("StartSound %dms (%d,%d,%s) = ", this.soundWorld.gameMsec, this.index, channel, shader.GetName());
            }

            if ((this.soundWorld != null) && (this.soundWorld.writeDemo != null)) {
                this.soundWorld.writeDemo.WriteInt(DS_SOUND);
                this.soundWorld.writeDemo.WriteInt(SCMD_START);
                this.soundWorld.writeDemo.WriteInt(this.index);

                this.soundWorld.writeDemo.WriteHashString(shader.GetName());

                this.soundWorld.writeDemo.WriteInt(channel);
                this.soundWorld.writeDemo.WriteFloat(diversity);
                this.soundWorld.writeDemo.WriteInt(soundShaderFlags);
            }

            // build the channel parameters by taking the shader parms and optionally overriding
            final soundShaderParms_t[] chanParms = {null};

            chanParms[0] = shader.parms;
            OverrideParms(chanParms[0], this.parms, chanParms);
            chanParms[0].soundShaderFlags |= soundShaderFlags;

            if (chanParms[0].shakes > 0.0f) {
                shader.CheckShakesAndOgg();
            }

            // this is the sample time it will be first mixed
            int start44kHz;

            if (this.soundWorld.fpa[0] != null) {
                // if we are recording an AVI demo, don't use hardware time
                start44kHz = this.soundWorld.lastAVI44kHz + MIXBUFFER_SAMPLES;
            } else {
                start44kHz = soundSystemLocal.GetCurrent44kHzTime() + MIXBUFFER_SAMPLES;
            }

            //
            // pick which sound to play from the shader
            //
            if (0 == shader.numEntries) {
                if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                    common.Printf("no samples in sound shader\n");
                }
                return 0;				// no sounds
            }
            int choice;

            // pick a sound from the list based on the passed diversity
            choice = (int) (diversity * shader.numEntries);
            if ((choice < 0) || (choice >= shader.numEntries)) {
                choice = 0;
            }

            // bump the choice if the exact sound was just played and we are NO_DUPS
            if ((chanParms[0].soundShaderFlags & SSF_NO_DUPS) != 0) {
                idSoundSample sample;
                if (shader.leadins[choice] != null) {
                    sample = shader.leadins[choice];
                } else {
                    sample = shader.entries[choice];
                }
                for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                    final idSoundChannel chan = this.channels[i];
                    if (chan.leadinSample == sample) {
                        choice = (choice + 1) % shader.numEntries;
                        break;
                    }
                }
            }

            // PLAY_ONCE sounds will never be restarted while they are running
            if ((chanParms[0].soundShaderFlags & SSF_PLAY_ONCE) != 0) {
                for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                    final idSoundChannel chan = this.channels[i];
                    if (chan.triggerState && (chan.soundShader == shader)) {
                        if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                            common.Printf("PLAY_ONCE not restarting\n");
                        }
                        return 0;
                    }
                }
            }

            // never play the same sound twice with the same starting time, even
            // if they are on different channels
            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                final idSoundChannel chan = this.channels[i];
                if (chan.triggerState && (chan.soundShader == shader) && (chan.trigger44kHzTime == start44kHz)) {
                    if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                        common.Printf("already started this frame\n");
                    }
                    return 0;
                }
            }

            Sys_EnterCriticalSection();

            // kill any sound that is currently playing on this channel
            if (channel != SCHANNEL_ANY) {
                for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                    final idSoundChannel chan = this.channels[i];
                    if (chan.triggerState && (chan.soundShader != null) && (chan.triggerChannel == channel)) {
                        if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                            common.Printf("(override %s)", chan.soundShader.base.GetName());
                        }

                        chan.Stop();

                        // if this was an onDemand sound, purge the sample now
                        if (chan.leadinSample.onDemand) {
                            chan.ALStop();
                            chan.leadinSample.PurgeSoundSample();
                        }
                        break;
                    }
                }
            }

            // find a free channel to play the sound on
            idSoundChannel chan;
            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                chan = this.channels[i];
                if (!chan.triggerState) {
                    break;
                }
            }

            if (i == SOUND_MAX_CHANNELS) {
                // we couldn't find a channel for it
                Sys_LeaveCriticalSection();
                if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                    common.Printf("no channels available\n");
                }
                return 0;
            }

            chan = this.channels[i];

            if (shader.leadins[choice] != null) {
                chan.leadinSample = shader.leadins[choice];
            } else {
                chan.leadinSample = shader.entries[choice];
            }

            // if the sample is onDemand (voice mails, etc), load it now
            if (chan.leadinSample.purged) {
                final int start = Sys_Milliseconds();
                chan.leadinSample.Load();
                final int end = Sys_Milliseconds();
                session.TimeHitch(end - start);
                // recalculate start44kHz, because loading may have taken a fair amount of time
                if (NOT(this.soundWorld.fpa[0])) {
                    start44kHz = soundSystemLocal.GetCurrent44kHzTime() + MIXBUFFER_SAMPLES;
                }
            }

            if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                common.Printf("'%s'\n", chan.leadinSample.name);
            }

            if (idSoundSystemLocal.s_skipHelltimeFX.GetBool()) {
                chan.disallowSlow = true;
            } else {
                chan.disallowSlow = !allowSlow;
            }

            ResetSlowChannel(chan);

            // the sound will start mixing in the next async mix block
            chan.triggered = true;
            chan.openalStreamingOffset = 0;
            chan.trigger44kHzTime = start44kHz;
            chan.parms = chanParms[0];
            chan.triggerGame44kHzTime = this.soundWorld.game44kHz;
            chan.soundShader = shader;
            chan.triggerChannel = channel;
            chan.Start();

            // we need to start updating the def and mixing it in
            this.playing = true;

            // spatialize it immediately, so it will start the next mix block
            // even if that happens before the next PlaceOrigin()
            Spatialize(this.soundWorld.listenerPos, this.soundWorld.listenerArea, this.soundWorld.rw);

            // return length of sound in milliseconds
            int length = chan.leadinSample.LengthIn44kHzSamples();

            if (chan.leadinSample.objectInfo.nChannels == 2) {
                length /= 2;	// stereo samples
            }

            // adjust the start time based on diversity for looping sounds, so they don't all start
            // at the same point
            if (((chan.parms.soundShaderFlags & SSF_LOOPING) != 0) && NOT(chan.leadinSample.LengthIn44kHzSamples())) {
                chan.trigger44kHzTime -= diversity * length;
                chan.trigger44kHzTime &= ~7;		// so we don't have to worry about the 22kHz and 11kHz expansions
                // starting in fractional samples
                chan.triggerGame44kHzTime -= diversity * length;
                chan.triggerGame44kHzTime &= ~7;
            }

            length *= 1000 / (float) PRIMARYFREQ;

            Sys_LeaveCriticalSection();

            return length;
        }

        // pass SCHANNEL_ANY to effect all channels
        @Override
        public void ModifySound(final int channel, final soundShaderParms_t parms) {
            if (null == parms) {
                common.Error("idSoundEmitterLocal::ModifySound: NULL parms");
            }
            if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                common.Printf("ModifySound(%d,%d)\n", this.index, channel);
            }
            if ((this.soundWorld != null) && (this.soundWorld.writeDemo != null)) {
                this.soundWorld.writeDemo.WriteInt(DS_SOUND);
                this.soundWorld.writeDemo.WriteInt(SCMD_MODIFY);
                this.soundWorld.writeDemo.WriteInt(this.index);
                this.soundWorld.writeDemo.WriteInt(channel);
                this.soundWorld.writeDemo.WriteFloat(parms.minDistance);
                this.soundWorld.writeDemo.WriteFloat(parms.maxDistance);
                this.soundWorld.writeDemo.WriteFloat(parms.volume);
                this.soundWorld.writeDemo.WriteFloat(parms.shakes);
                this.soundWorld.writeDemo.WriteInt(parms.soundShaderFlags);
                this.soundWorld.writeDemo.WriteInt(parms.soundClass);
            }

            for (int i = 0; i < SOUND_MAX_CHANNELS; i++) {
                final idSoundChannel chan = this.channels[i];

                if (!chan.triggerState) {
                    continue;
                }
                if ((channel != SCHANNEL_ANY) && (chan.triggerChannel != channel)) {
                    continue;
                }

                final soundShaderParms_t[] chanParms = {chan.parms};
                OverrideParms(chan.parms, parms, chanParms);
                chan.parms = chanParms[0];

                if ((chan.parms.shakes > 0.0f) && (chan.soundShader != null)) {
                    chan.soundShader.CheckShakesAndOgg();
                }
            }
        }

        /*
         ===================
         idSoundEmitterLocal::StopSound

         can pass SCHANNEL_ANY
         ===================
         */
        @Override
        public void StopSound(final int channel) {
            int i;

            if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                common.Printf("StopSound(%d,%d)\n", this.index, channel);
            }

            if ((this.soundWorld != null) && (this.soundWorld.writeDemo != null)) {
                this.soundWorld.writeDemo.WriteInt(DS_SOUND);
                this.soundWorld.writeDemo.WriteInt(SCMD_STOP);
                this.soundWorld.writeDemo.WriteInt(this.index);
                this.soundWorld.writeDemo.WriteInt(channel);
            }

            Sys_EnterCriticalSection();

            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                final idSoundChannel chan = this.channels[i];

                if (!chan.triggerState) {
                    continue;
                }
                if ((channel != SCHANNEL_ANY) && (chan.triggerChannel != channel)) {
                    continue;
                }

                // stop it
                chan.Stop();

                // free hardware resources
                chan.ALStop();

                // if this was an onDemand sound, purge the sample now
                if (chan.leadinSample.onDemand) {
                    chan.leadinSample.PurgeSoundSample();
                }

                chan.leadinSample = null;
                chan.soundShader = null;
            }

            Sys_LeaveCriticalSection();
        }

        /*
         ===================
         idSoundEmitterLocal::FadeSound

         to is in Db (sigh), over is in seconds
         ===================
         */
        @Override
        public void FadeSound(final int channel, float to, float over) {
            if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                common.Printf("FadeSound(%d,%d,%f,%f )\n", this.index, channel, to, over);
            }
            if (NOT(this.soundWorld)) {
                return;
            }
            if (this.soundWorld.writeDemo != null) {
                this.soundWorld.writeDemo.WriteInt(DS_SOUND);
                this.soundWorld.writeDemo.WriteInt(SCMD_FADE);
                this.soundWorld.writeDemo.WriteInt(this.index);
                this.soundWorld.writeDemo.WriteInt(channel);
                this.soundWorld.writeDemo.WriteFloat(to);
                this.soundWorld.writeDemo.WriteFloat(over);
            }

            int start44kHz;

            if (this.soundWorld.fpa[0] != null) {
                // if we are recording an AVI demo, don't use hardware time
                start44kHz = this.soundWorld.lastAVI44kHz + MIXBUFFER_SAMPLES;
            } else {
                start44kHz = soundSystemLocal.GetCurrent44kHzTime() + MIXBUFFER_SAMPLES;
            }

            final int length44kHz = soundSystemLocal.MillisecondsToSamples((int) (over * 1000));

            for (int i = 0; i < SOUND_MAX_CHANNELS; i++) {
                final idSoundChannel chan = this.channels[i];

                if (!chan.triggerState) {
                    continue;
                }
                if ((channel != SCHANNEL_ANY) && (chan.triggerChannel != channel)) {
                    continue;
                }

                // if it is already fading to this volume at this rate, don't change it
                if ((chan.channelFade.fadeEndVolume == to)
                        && ((chan.channelFade.fadeEnd44kHz - chan.channelFade.fadeStart44kHz) == length44kHz)) {
                    continue;
                }

                // fade it
                chan.channelFade.fadeStartVolume = chan.channelFade.FadeDbAt44kHz(start44kHz);
                chan.channelFade.fadeStart44kHz = start44kHz;
                chan.channelFade.fadeEnd44kHz = start44kHz + length44kHz;
                chan.channelFade.fadeEndVolume = to;
            }
        }

        @Override
        public boolean CurrentlyPlaying() {
            return this.playing;
        }

        /*
         ===================
         idSoundEmitterLocal::CurrentAmplitude

         this is called from the main thread by the material shader system
         to allow lights and surface flares to vary with the sound amplitude
         ===================
         */
        @Override
        public float CurrentAmplitude() {
            if (idSoundSystemLocal.s_constantAmplitude.GetFloat() >= 0.0f) {
                return idSoundSystemLocal.s_constantAmplitude.GetFloat();
            }

            if (this.removeStatus > REMOVE_STATUS_WAITSAMPLEFINISHED) {
                return 0.0f;
            }

            final int localTime = soundSystemLocal.GetCurrent44kHzTime();

            // see if we can use our cached value
            if (this.ampTime == localTime) {
                return this.amplitude;
            }

            // calculate a new value
            this.ampTime = localTime;
            this.amplitude = this.soundWorld.FindAmplitude(this, localTime, null, SCHANNEL_ANY, false);

            return this.amplitude;
        }

        // for save games.  Index will always be > 0
        @Override
        public int Index() {
            return this.index;
        }
        //----------------------------------------------

        public void Clear() {
            int i;

            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                this.channels[i].ALStop();
                this.channels[i].Clear();
            }

            this.removeStatus = REMOVE_STATUS_SAMPLEFINISHED;
            this.distance = 0.0f;

            this.lastValidPortalArea = -1;

            this.playing = false;
            this.hasShakes = false;
            this.ampTime = 0;								// last time someone queried
            this.amplitude = 0;
            this.maxDistance = 10.0f;						// meters
            this.spatializedOrigin.Zero();

//	memset( &parms, 0, sizeof( parms ) );
            this.parms = new soundShaderParms_t();
        }

        public void OverrideParms(final soundShaderParms_t base, final soundShaderParms_t over, soundShaderParms_t[] out) {
            if (null == over) {
                out[0] = base;
                return;
            }
            if (over.minDistance != 0) {
                out[0].minDistance = over.minDistance;
            } else {
                out[0].minDistance = base.minDistance;
            }
            if (over.maxDistance != 0) {
                out[0].maxDistance = over.maxDistance;
            } else {
                out[0].maxDistance = base.maxDistance;
            }
            if (over.shakes != 0) {
                out[0].shakes = over.shakes;
            } else {
                out[0].shakes = base.shakes;
            }
            if (over.volume != 0) {
                out[0].volume = over.volume;
            } else {
                out[0].volume = base.volume;
            }
            if (over.soundClass != 0) {
                out[0].soundClass = over.soundClass;
            } else {
                out[0].soundClass = base.soundClass;
            }
            out[0].soundShaderFlags = base.soundShaderFlags | over.soundShaderFlags;
        }

        /*
         ==================
         idSoundEmitterLocal::CheckForCompletion

         Checks to see if all the channels have completed, clearing the playing flag if necessary.
         Sets the playing and shakes bools.
         ==================
         */
        public void CheckForCompletion(int current44kHzTime) {
            boolean hasActive;
            int i;

            hasActive = false;
            this.hasShakes = false;

            if (this.playing) {
                for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                    final idSoundChannel chan = this.channels[i];

                    if (!chan.triggerState) {
                        continue;
                    }
                    final idSoundShader shader = chan.soundShader;
                    if (null == shader) {
                        continue;
                    }

                    // see if this channel has completed
                    if (0 == (chan.parms.soundShaderFlags & SSF_LOOPING)) {
                        int/*ALint*/ state = AL_PLAYING;

                        if (idSoundSystemLocal.useOpenAL && alIsSource(chan.openalSource)) {
//                            alGetSourcei(chan.openalSource, AL_SOURCE_STATE, state);
                            state = alGetSourcei(chan.openalSource, AL_SOURCE_STATE);
                        }
                        final idSlowChannel slow = GetSlowChannel(chan);

                        if (this.soundWorld.slowmoActive && slow.IsActive()) {
                            if (slow.GetCurrentPosition().time >= (chan.leadinSample.LengthIn44kHzSamples() / 2)) {
                                chan.Stop();
                                // if this was an onDemand sound, purge the sample now
                                if (chan.leadinSample.onDemand) {
                                    chan.leadinSample.PurgeSoundSample();
                                }
                                continue;
                            }
                        } else if (((chan.trigger44kHzTime + chan.leadinSample.LengthIn44kHzSamples()) < current44kHzTime) || (state == AL_STOPPED)) {
                            chan.Stop();

                            // free hardware resources
                            chan.ALStop();

                            // if this was an onDemand sound, purge the sample now
                            if (chan.leadinSample.onDemand) {
                                chan.leadinSample.PurgeSoundSample();
                            }
                            continue;
                        }
                    }

                    // free decoder memory if no sound was decoded for a while
                    if ((chan.decoder != null) && (chan.decoder.GetLastDecodeTime() < (current44kHzTime - SOUND_DECODER_FREE_DELAY))) {
                        chan.decoder.ClearDecoder();
                    }

                    hasActive = true;

                    if (chan.parms.shakes > 0.0f) {
                        this.hasShakes = true;
                    }
                }
            }

            // mark the entire sound emitter as non-playing if there aren't any active channels
            if (!hasActive) {
                this.playing = false;
                if (this.removeStatus == REMOVE_STATUS_WAITSAMPLEFINISHED) {
                    // this can now be reused by the next request for a new soundEmitter
                    this.removeStatus = REMOVE_STATUS_SAMPLEFINISHED;
                }
            }
        }

        /*
         ===================
         idSoundEmitterLocal::Spatialize

         Called once each sound frame by the main thread from idSoundWorldLocal::PlaceOrigin
         ===================
         */
        public void Spatialize(idVec3 listenerPos, int listenerArea, idRenderWorld rw) {
            int i;
            final boolean hasActive = false;

            //
            // work out the maximum distance of all the playing channels
            //
            this.maxDistance = 0;

            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                final idSoundChannel chan = this.channels[i];

                if (!chan.triggerState) {
                    continue;
                }
                if (chan.parms.maxDistance > this.maxDistance) {
                    this.maxDistance = chan.parms.maxDistance;
                }
            }

            //
            // work out where the sound comes from
            //
            final idVec3 realOrigin = this.origin.oMultiply(DOOM_TO_METERS);
            final idVec3 len = listenerPos.oMinus(realOrigin);
            this.realDistance = len.LengthFast();

            if (this.realDistance >= this.maxDistance) {
                // no way to possibly hear it
                this.distance = this.realDistance;
                return;
            }

            //
            // work out virtual origin and distance, which may be from a portal instead of the actual origin
            //
            this.distance = this.maxDistance * METERS_TO_DOOM;
            if (listenerArea == -1) {		// listener is outside the world
                return;
            }
            if (rw != null) {
                // we have a valid renderWorld
                int soundInArea = rw.PointInArea(this.origin);
                if (soundInArea == -1) {
                    if (this.lastValidPortalArea == -1) {		// sound is outside the world
                        this.distance = this.realDistance;
                        this.spatializedOrigin = this.origin;			// sound is in our area
                        return;
                    }
                    soundInArea = this.lastValidPortalArea;
                }
                this.lastValidPortalArea = soundInArea;
                if (soundInArea == listenerArea) {
                    this.distance = this.realDistance;
                    this.spatializedOrigin = this.origin;			// sound is in our area
                    return;
                }

                this.soundWorld.ResolveOrigin(0, null, soundInArea, 0.0f, this.origin, this);
                this.distance /= METERS_TO_DOOM;
            } else {
                // no portals available
                this.distance = this.realDistance;
                this.spatializedOrigin = this.origin;			// sound is in our area
            }
        }

        public idSlowChannel GetSlowChannel(final idSoundChannel chan) {
            return this.slowChannels[indexOf(chan, this.channels)];//TODO: pointer subtraction
        }

        public void SetSlowChannel(final idSoundChannel chan, idSlowChannel slow) {
            this.slowChannels[indexOf(chan, this.channels)] = slow;
        }

        public void ResetSlowChannel(final idSoundChannel chan) {
            final int index = indexOf(chan, this.channels);
            this.slowChannels[index].Reset();
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
