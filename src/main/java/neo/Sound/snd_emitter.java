package neo.Sound;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import neo.Renderer.RenderWorld.idRenderWorld;
import neo.Sound.snd_cache.idSoundSample;
import static neo.Sound.snd_local.PRIMARYFREQ;
import static neo.Sound.snd_local.SOUND_DECODER_FREE_DELAY;
import static neo.Sound.snd_local.SOUND_MAX_CHANNELS;
import neo.Sound.snd_local.idSampleDecoder;
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
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.snd_shader.soundShaderParms_t;
import neo.Sound.snd_system.idSoundSystemLocal;
import static neo.Sound.snd_system.soundSystemLocal;
import neo.Sound.snd_world.idSoundWorldLocal;
import neo.Sound.sound.idSoundEmitter;
import static neo.TempDump.NOT;
import static neo.TempDump.btoi;
import static neo.TempDump.indexOf;
import static neo.framework.Common.common;
import static neo.framework.DemoFile.demoSystem_t.DS_SOUND;
import static neo.framework.Session.session;
import neo.idlib.math.Math_h.idMath;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;
import neo.idlib.math.Vector.idVec3;
import static neo.sys.win_main.Sys_EnterCriticalSection;
import static neo.sys.win_main.Sys_LeaveCriticalSection;
import static neo.sys.win_shared.Sys_Milliseconds;
import org.lwjgl.openal.AL10;
import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.AL10.AL_PLAYING;
import static org.lwjgl.openal.AL10.AL_SOURCE_STATE;
import static org.lwjgl.openal.AL10.AL_STOPPED;
import static org.lwjgl.openal.AL10.alGetError;
import static org.lwjgl.openal.AL10.alIsSource;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourcei;

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
        public float fadeStartVolume;    // in dB
        public float fadeEndVolume;    // in dB
//
//

        public void Clear() {
            fadeStart44kHz = 0;
            fadeEnd44kHz = 0;
            fadeStartVolume = 0;
            fadeEndVolume = 0;
        }

        public float FadeDbAt44kHz(int current44kHz) {
            float fadeDb;

            if (current44kHz >= fadeEnd44kHz) {
                fadeDb = fadeEndVolume;
            } else if (current44kHz > fadeStart44kHz) {
                float fraction = (fadeEnd44kHz - fadeStart44kHz);
                float over = (current44kHz - fadeStart44kHz);
                fadeDb = fadeStartVolume + (fadeEndVolume - fadeStartVolume) * over / fraction;
            } else {
                fadeDb = fadeStartVolume;
            }
            return fadeDb;
        }
    };

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
            channel = 0;
            buffer = null;
            initialized = false;
            maxlen = 0;
//            memset(continuitySamples, 0, sizeof(float) * 4);
        }

//	virtual				~SoundFX()										{ if ( buffer ) delete buffer; };
        public void Initialize() {
        }

        public void ProcessSample(float[] in, int in_offset, float[] out, int out_offset) {
        }

        public void SetChannel(int chan) {
            channel = chan;
        }

        public int GetChannel() {
            return channel;
        }

        public void SetContinuitySamples(float in1, float in2, float out1, float out2) {
            continuitySamples[0] = in1;
            continuitySamples[1] = in2;
            continuitySamples[2] = out1;
            continuitySamples[3] = out2;
        }

        // FIXME?
        public void GetContinuitySamples(float[] in1, float[] in2, float[] out1, float[] out2) {
            in1[0] = continuitySamples[0];
            in2[0] = continuitySamples[1];
            out1[0] = continuitySamples[2];
            out2[0] = continuitySamples[3];
        }

        public void SetParameter(float val) {
            param = val;
        }
    };

    static class SoundFX_Lowpass extends SoundFX {

        @Override
        public void ProcessSample(float[] in, int in_offset, float[] out, int out_offset) {
            float c, a1, a2, a3, b1, b2;
            float resonance = idSoundSystemLocal.s_enviroSuitCutoffQ.GetFloat();
            float cutoffFrequency = idSoundSystemLocal.s_enviroSuitCutoffFreq.GetFloat();

            Initialize();

            c = 1.0f / idMath.Tan16(idMath.PI * cutoffFrequency / 44100);

            // compute coefs
            a1 = 1.0f / (1.0f + resonance * c + c * c);
            a2 = 2 * a1;
            a3 = a1;
            b1 = 2.0f * (1.0f - c * c) * a1;
            b2 = (1.0f - resonance * c + c * c) * a1;

            // compute output value
            out[out_offset + 0] = a1 * in[in_offset + 0]
                    + a2 * in[in_offset + -1]
                    + a3 * in[in_offset + -2]
                    - b1 * out[out_offset + -1]
                    - b2 * out[out_offset + -2];
        }
    };

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
            out[outOffset + 0] = a1 * in[inOffset + 0]
                    + a2 * in[inOffset - 1]
                    + a3 * in[inOffset - 2]
                    - b1 * out[outOffset - 1]
                    - b2 * out[outOffset - 2];
        }

        public void SetParms(float p1 /*= 0*/, float p2 /*= 0*/, float p3 /*= 0*/) {
            float c;

            // set the vars
            freq = p1;
            res = p2;

            // precompute the coefs
            c = 1.0f / idMath.Tan(idMath.PI * freq / 44100);

            // compute coefs
            a1 = 1.0f / (1.0f + res * c + c * c);
            a2 = 2 * a1;
            a3 = a1;

            b1 = 2.0f * (1.0f - c * c) * a1;
            b2 = (1.0f - res * c + c * c) * a1;
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
    };

    static class SoundFX_Comb extends SoundFX {

        int currentTime;
//        
//        

        @Override
        public void Initialize() {
            if (initialized) {
                return;
            }

            initialized = true;
            maxlen = 50000;
            buffer = new float[maxlen];
            currentTime = 0;
        }

        @Override
        public void ProcessSample(float[] in, int in_offset, float[] out, int out_offset) {
            float gain = idSoundSystemLocal.s_reverbFeedback.GetFloat();
            int len = (int) (idSoundSystemLocal.s_reverbTime.GetFloat() + param);

            Initialize();

            // sum up and output
            out[out_offset + 0] = buffer[currentTime];
            buffer[currentTime] = buffer[currentTime] * gain + in[in_offset + 0];

            // increment current time
            currentTime++;
            if (currentTime >= len) {
                currentTime -= len;
            }
        }
    };

    static class FracTime {

        public int time;
        public float frac;
//
//

        public void Set(int val) {
            time = val;
            frac = 0;
        }

        public void Increment(float val) {
            frac += val;
            while (frac >= 1.f) {
                time++;
                frac--;
            }
        }
    };
//enum {
    public static final int PLAYBACK_RESET = 0;
    public static final int PLAYBACK_ADVANCING = 1;
//};

    static class idSlowChannel {

        boolean active;
        idSoundChannel chan;
//
        int playbackState;
        int triggerOffset;
//
        FracTime newPosition;
        int newSampleOffset;
//
        FracTime curPosition;
        int curSampleOffset;
//
        SoundFX_LowpassFast lowpass;
//
//

        // functions
        void GenerateSlowChannel(FracTime playPos, int sampleCount44k, float[] finalBuffer) {
            idSoundWorldLocal sw = (idSoundWorldLocal) soundSystemLocal.GetPlayingSoundWorld();
            float[] in = new float[MIXBUFFER_SAMPLES + 3], out = new float[MIXBUFFER_SAMPLES + 3];
            float[] src = new float[MIXBUFFER_SAMPLES + 3 - 2], spline = new float[MIXBUFFER_SAMPLES + 3 - 2];
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

            neededSamples = (int) (sampleCount44k * slowmoSpeed + 4);
            orgTime = playPos.time;

            // get the channel's samples
            chan.GatherChannelSamples(playPos.time * 2, neededSamples, FloatBuffer.wrap(src));
            for (i = 0; i < neededSamples >> 1; i++) {
                spline[i] = src[i * 2];
            }

            // interpolate channel
            zeroedPos = playPos.time;
            playPos.time = 0;

            for (i = 0; i < sampleCount44k >> 1; i++, count += 2) {
                float val;
                val = spline[playPos.time];
                src[i] = val;
                playPos.Increment(slowmoSpeed);
            }

            // lowpass filter
//            float *in_p = in + 2, *out_p = out + 2;
            float[] in_p1 = {0}, in_p2 = {0}, out_p1 = {0}, out_p2 = {0};
            int numSamples = sampleCount44k >> 1;

            lowpass.GetContinuitySamples(in_p1, in_p2, out_p1, out_p2);
            lowpass.SetParms(slowmoSpeed * 15000, 1.2f, 9);

            System.arraycopy(src, 0, in, 2, MIXBUFFER_SAMPLES + 3 - 2);
            System.arraycopy(spline, 0, out, 2, MIXBUFFER_SAMPLES + 3 - 2);
            in[0] = in_p1[0];//FIXME:ugly block.
            in[1] = in_p2[0];
            out[0] = out_p1[0];
            out[1] = out_p2[0];

            for (i = 0, count = 0; i < numSamples; i++, count += 2) {
                lowpass.ProcessSample(in, 2 + i, out, 2 + i);
                finalBuffer[count] = finalBuffer[count + 1] = out[i];
            }

            lowpass.SetContinuitySamples(in[2 + numSamples - 2], in[2 + numSamples - 3], out[2 + numSamples - 2], out[2 + numSamples - 3]);//2 = pointer offset

            playPos.time += zeroedPos;
        }

        float GetSlowmoSpeed() {
            idSoundWorldLocal sw = (idSoundWorldLocal) soundSystemLocal.GetPlayingSoundWorld();

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
            curPosition.Set(0);
            newPosition.Set(0);

            curSampleOffset = -10000;
            newSampleOffset = -10000;

            triggerOffset = 0;
        }

        public void GatherChannelSamples(int sampleOffset44k, int sampleCount44k, float[] dest) {
            int state = 0;

            // setup chan
            active = true;
            newSampleOffset = sampleOffset44k >> 1;

            // set state
            if (newSampleOffset < curSampleOffset) {
                state = PLAYBACK_RESET;
            } else if (newSampleOffset > curSampleOffset) {
                state = PLAYBACK_ADVANCING;
            }

            if (state == PLAYBACK_RESET) {
                curPosition.Set(newSampleOffset);
            }

            // set current vars
            curSampleOffset = newSampleOffset;
            newPosition = curPosition;

            // do the slow processing
            GenerateSlowChannel(newPosition, sampleCount44k, dest);

            // finish off
            if (state == PLAYBACK_ADVANCING) {
                curPosition = newPosition;
            }
        }

        public boolean IsActive() {
            return active;
        }

        public FracTime GetCurrentPosition() {
            return curPosition;
        }
    };

    static class idSoundChannel {

        public boolean triggerState;
        public int trigger44kHzTime;		// hardware time sample the channel started
        public int triggerGame44kHzTime;	// game time sample time the channel started
        public soundShaderParms_t parms;	// combines the shader parms and the per-channel overrides
        public idSoundSample leadinSample;	// if not looped, this is the only sample
        public int/*s_channelType*/ triggerChannel;
        public idSoundShader soundShader;
        public idSampleDecoder decoder;
        public float diversity;
        public float lastVolume;		// last calculated volume based on distance
        public float[] lastV = new float[6];	// last calculated volume for each speaker, so we can smoothly fade
        public idSoundFade channelFade;
        public boolean triggered;
        public int/*ALuint*/ openalSource;
        public int/*ALuint*/ openalStreamingOffset;
        public IntBuffer/*ALuint*/ openalStreamingBuffer;
        public IntBuffer/*ALuint*/ lastopenalStreamingBuffer;
        //
        public boolean disallowSlow;
        //
        //

        public idSoundChannel() {
            decoder = null;
            this.channelFade = new idSoundFade();
            this.openalStreamingBuffer = IntBuffer.allocate(3);
            this.lastopenalStreamingBuffer = IntBuffer.allocate(3);
            Clear();
        }
//						~idSoundChannel( void );

        public void Clear() {
            int j;

            Stop();
            soundShader = null;
            lastVolume = 0.0f;
            triggerChannel = SCHANNEL_ANY;
            channelFade.Clear();
            diversity = 0.0f;
            leadinSample = null;
            trigger44kHzTime = 0;
            for (j = 0; j < 6; j++) {
                lastV[j] = 0.0f;
            }
//	memset( &parms, 0, sizeof(parms) );
            parms = new soundShaderParms_t();

            triggered = false;
            openalSource = 0;//null;
            openalStreamingOffset = 0;
//            openalStreamingBuffer[0] = openalStreamingBuffer[1] = openalStreamingBuffer[2] = 0;
//            lastopenalStreamingBuffer[0] = lastopenalStreamingBuffer[1] = lastopenalStreamingBuffer[2] = 0;
            openalStreamingBuffer.clear();
            lastopenalStreamingBuffer.clear();
        }

        public void Start() {
            triggerState = true;
            if (decoder == null) {
                decoder = idSampleDecoder.Alloc();
            }
        }

        public void Stop() {
            triggerState = false;
            if (decoder != null) {
                idSampleDecoder.Free(decoder);
                decoder = null;
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
                dest.clear();
                dest_p += len;
                sampleCount44k -= len;
                sampleOffset44k += len;
            }

            // grab part of the leadin sample
            idSoundSample leadin = leadinSample;
            if (NOT(leadin) || sampleOffset44k < 0 || sampleCount44k <= 0) {
//		memset( dest_p, 0, sampleCount44k * sizeof( dest_p[0] ) );
                dest.clear();
                return;
            }

            if (sampleOffset44k < leadin.LengthIn44kHzSamples()) {
                len = leadin.LengthIn44kHzSamples() - sampleOffset44k;
                if (len > sampleCount44k) {
                    len = sampleCount44k;
                }

                // decode the sample
                decoder.Decode(leadin, sampleOffset44k, len, dest);

                dest.position(dest_p += len);
                sampleCount44k -= len;
                sampleOffset44k += len;
            }

            // if not looping, zero fill any remaining spots
            if (null == soundShader || 0 == (parms.soundShaderFlags & SSF_LOOPING)) {
//		memset( dest_p, 0, sampleCount44k * sizeof( dest_p[0] ) );
                dest.clear();
                return;
            }

            // fill the remainder with looped samples
            idSoundSample loop = soundShader.entries[0];

            if (null == loop) {
//		memset( dest_p, 0, sampleCount44k * sizeof( dest_p[0] ) );
                dest.clear();
                return;
            }

            sampleOffset44k -= leadin.LengthIn44kHzSamples();

            while (sampleCount44k > 0) {
                int totalLen = loop.LengthIn44kHzSamples();

                sampleOffset44k %= totalLen;

                len = totalLen - sampleOffset44k;
                if (len > sampleCount44k) {
                    len = sampleCount44k;
                }

                // decode the sample
                decoder.Decode(loop, sampleOffset44k, len, dest);//TODO:

                dest.position(dest_p += len);
                sampleCount44k -= len;
                sampleOffset44k += len;
            }
        }

        public void ALStop() {			// free OpenAL resources if any
            if (idSoundSystemLocal.useOpenAL) {

                if (alIsSource(openalSource)) {
                    alSourceStop(openalSource);
                    alSourcei(openalSource, AL_BUFFER, 0);
                    soundSystemLocal.FreeOpenALSource(openalSource);
                }

                if (openalStreamingBuffer.get(0) != 0 && openalStreamingBuffer.get(1) != 0 && openalStreamingBuffer.get(2) != 0) {
                    alGetError();
//                    alDeleteBuffers(3, openalStreamingBuffer[0]);
                    AL10.alDeleteBuffers(openalStreamingBuffer);
                    if (alGetError() == AL_NO_ERROR) {
                        openalStreamingBuffer.clear();
                    }
                }

                if (lastopenalStreamingBuffer.get(0) != 0 && lastopenalStreamingBuffer.get(1) != 0 && lastopenalStreamingBuffer.get(2) != 0) {
                    alGetError();
//                    alDeleteBuffers(3, lastopenalStreamingBuffer[0]);
                    AL10.alDeleteBuffers(lastopenalStreamingBuffer);
                    if (alGetError() == AL_NO_ERROR) {
                        lastopenalStreamingBuffer.clear();
                    }
                }
            }
        }
    };
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

    static class idSoundEmitterLocal extends idSoundEmitter {

        public idSoundWorldLocal soundWorld;			// the world that holds this emitter
        //
        public int index;					// in world emitter list
        public int/*removeStatus_t*/ removeStatus;
        //
        public idVec3 origin;
        public int listenerId;
        public soundShaderParms_t parms;			// default overrides for all channels
        //
        //
        // the following are calculated in UpdateEmitter, and don't need to be archived
        public float maxDistance;				// greatest of all playing channel distances
        public int lastValidPortalArea;                         // so an emitter that slides out of the world continues playing
        public boolean playing;					// if false, no channel is active
        public boolean hasShakes;
        public idVec3 spatializedOrigin;			// the virtual sound origin, either the real sound origin,
        //							// or a point through a portal chain
        public float realDistance;				// in meters
        public float distance;					// in meters, this may be the straight-line distance, or
        //                                                      // it may go through a chain of portals.  If there
        //                                                      // is not an open-portal path, distance will be > maxDistance
        //
        // a single soundEmitter can have many channels playing from the same point
        public idSoundChannel[] channels = new idSoundChannel[SOUND_MAX_CHANNELS];
        //
        public idSlowChannel[] slowChannels = new idSlowChannel[SOUND_MAX_CHANNELS];
        //
        // this is just used for feedback to the game or rendering system:
        // flashing lights and screen shakes.  Because the material expression
        // evaluation doesn't do common subexpression removal, we cache the
        // last generated value
        public int ampTime;
        public float amplitude;
        //
        //

        public idSoundEmitterLocal() {
            soundWorld = null;
            this.spatializedOrigin = new idVec3();

            for (int c = 0; c < channels.length; c++) {
                channels[c] = new idSoundChannel();
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
            if (removeStatus != REMOVE_STATUS_ALIVE) {
                return;
            }

            if (idSoundSystemLocal.s_showStartSound.GetInteger() != 0) {
                common.Printf("FreeSound (%d,%d)\n", index, immediate);
            }
            if (soundWorld != null && soundWorld.writeDemo != null) {
                soundWorld.writeDemo.WriteInt(DS_SOUND);
                soundWorld.writeDemo.WriteInt(SCMD_FREE);
                soundWorld.writeDemo.WriteInt(index);
                soundWorld.writeDemo.WriteInt(btoi(immediate));
            }

            if (!immediate) {
                removeStatus = REMOVE_STATUS_WAITSAMPLEFINISHED;
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
            if (soundWorld != null && soundWorld.writeDemo != null) {
                soundWorld.writeDemo.WriteInt(DS_SOUND);
                soundWorld.writeDemo.WriteInt(SCMD_UPDATE);
                soundWorld.writeDemo.WriteInt(index);
                soundWorld.writeDemo.WriteVec3(origin);
                soundWorld.writeDemo.WriteInt(listenerId);
                soundWorld.writeDemo.WriteFloat(parms.minDistance);
                soundWorld.writeDemo.WriteFloat(parms.maxDistance);
                soundWorld.writeDemo.WriteFloat(parms.volume);
                soundWorld.writeDemo.WriteFloat(parms.shakes);
                soundWorld.writeDemo.WriteInt(parms.soundShaderFlags);
                soundWorld.writeDemo.WriteInt(parms.soundClass);
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
                common.Printf("StartSound %dms (%d,%d,%s) = ", soundWorld.gameMsec, index, (int) channel, shader.GetName());
            }

            if (soundWorld != null && soundWorld.writeDemo != null) {
                soundWorld.writeDemo.WriteInt(DS_SOUND);
                soundWorld.writeDemo.WriteInt(SCMD_START);
                soundWorld.writeDemo.WriteInt(index);

                soundWorld.writeDemo.WriteHashString(shader.GetName());

                soundWorld.writeDemo.WriteInt(channel);
                soundWorld.writeDemo.WriteFloat(diversity);
                soundWorld.writeDemo.WriteInt(soundShaderFlags);
            }

            // build the channel parameters by taking the shader parms and optionally overriding
            soundShaderParms_t[] chanParms = {null};

            chanParms[0] = shader.parms;
            OverrideParms(chanParms[0], this.parms, chanParms);
            chanParms[0].soundShaderFlags |= soundShaderFlags;

            if (chanParms[0].shakes > 0.0f) {
                shader.CheckShakesAndOgg();
            }

            // this is the sample time it will be first mixed
            int start44kHz;

            if (soundWorld.fpa[0] != null) {
                // if we are recording an AVI demo, don't use hardware time
                start44kHz = soundWorld.lastAVI44kHz + MIXBUFFER_SAMPLES;
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
            if (choice < 0 || choice >= shader.numEntries) {
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
                    idSoundChannel chan = channels[i];
                    if (chan.leadinSample == sample) {
                        choice = (choice + 1) % shader.numEntries;
                        break;
                    }
                }
            }

            // PLAY_ONCE sounds will never be restarted while they are running
            if ((chanParms[0].soundShaderFlags & SSF_PLAY_ONCE) != 0) {
                for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                    idSoundChannel chan = channels[i];
                    if (chan.triggerState && chan.soundShader == shader) {
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
                idSoundChannel chan = channels[i];
                if (chan.triggerState && chan.soundShader == shader && chan.trigger44kHzTime == start44kHz) {
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
                    idSoundChannel chan = channels[i];
                    if (chan.triggerState && chan.soundShader != null && chan.triggerChannel == channel) {
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
                chan = channels[i];
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

            chan = channels[i];

            if (shader.leadins[choice] != null) {
                chan.leadinSample = shader.leadins[ choice];
            } else {
                chan.leadinSample = shader.entries[ choice];
            }

            // if the sample is onDemand (voice mails, etc), load it now
            if (chan.leadinSample.purged) {
                int start = Sys_Milliseconds();
                chan.leadinSample.Load();
                int end = Sys_Milliseconds();
                session.TimeHitch(end - start);
                // recalculate start44kHz, because loading may have taken a fair amount of time
                if (NOT(soundWorld.fpa[0])) {
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
            chan.triggerGame44kHzTime = soundWorld.game44kHz;
            chan.soundShader = shader;
            chan.triggerChannel = channel;
            chan.Start();

            // we need to start updating the def and mixing it in
            playing = true;

            // spatialize it immediately, so it will start the next mix block
            // even if that happens before the next PlaceOrigin()
            Spatialize(soundWorld.listenerPos, soundWorld.listenerArea, soundWorld.rw);

            // return length of sound in milliseconds
            int length = chan.leadinSample.LengthIn44kHzSamples();

            if (chan.leadinSample.objectInfo.nChannels == 2) {
                length /= 2;	// stereo samples
            }

            // adjust the start time based on diversity for looping sounds, so they don't all start
            // at the same point
            if ((chan.parms.soundShaderFlags & SSF_LOOPING) != 0 && NOT(chan.leadinSample.LengthIn44kHzSamples())) {
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
                common.Printf("ModifySound(%d,%d)\n", index, channel);
            }
            if (soundWorld != null && soundWorld.writeDemo != null) {
                soundWorld.writeDemo.WriteInt(DS_SOUND);
                soundWorld.writeDemo.WriteInt(SCMD_MODIFY);
                soundWorld.writeDemo.WriteInt(index);
                soundWorld.writeDemo.WriteInt(channel);
                soundWorld.writeDemo.WriteFloat(parms.minDistance);
                soundWorld.writeDemo.WriteFloat(parms.maxDistance);
                soundWorld.writeDemo.WriteFloat(parms.volume);
                soundWorld.writeDemo.WriteFloat(parms.shakes);
                soundWorld.writeDemo.WriteInt(parms.soundShaderFlags);
                soundWorld.writeDemo.WriteInt(parms.soundClass);
            }

            for (int i = 0; i < SOUND_MAX_CHANNELS; i++) {
                idSoundChannel chan = channels[i];

                if (!chan.triggerState) {
                    continue;
                }
                if (channel != SCHANNEL_ANY && chan.triggerChannel != channel) {
                    continue;
                }

                soundShaderParms_t[] chanParms = {chan.parms};
                OverrideParms(chan.parms, parms, chanParms);
                chan.parms = chanParms[0];

                if (chan.parms.shakes > 0.0f && chan.soundShader != null) {
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
                common.Printf("StopSound(%d,%d)\n", index, channel);
            }

            if (soundWorld != null && soundWorld.writeDemo != null) {
                soundWorld.writeDemo.WriteInt(DS_SOUND);
                soundWorld.writeDemo.WriteInt(SCMD_STOP);
                soundWorld.writeDemo.WriteInt(index);
                soundWorld.writeDemo.WriteInt(channel);
            }

            Sys_EnterCriticalSection();

            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                idSoundChannel chan = channels[i];

                if (!chan.triggerState) {
                    continue;
                }
                if (channel != SCHANNEL_ANY && chan.triggerChannel != channel) {
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
                common.Printf("FadeSound(%d,%d,%f,%f )\n", index, channel, to, over);
            }
            if (NOT(soundWorld)) {
                return;
            }
            if (soundWorld.writeDemo != null) {
                soundWorld.writeDemo.WriteInt(DS_SOUND);
                soundWorld.writeDemo.WriteInt(SCMD_FADE);
                soundWorld.writeDemo.WriteInt(index);
                soundWorld.writeDemo.WriteInt(channel);
                soundWorld.writeDemo.WriteFloat(to);
                soundWorld.writeDemo.WriteFloat(over);
            }

            int start44kHz;

            if (soundWorld.fpa[0] != null) {
                // if we are recording an AVI demo, don't use hardware time
                start44kHz = soundWorld.lastAVI44kHz + MIXBUFFER_SAMPLES;
            } else {
                start44kHz = soundSystemLocal.GetCurrent44kHzTime() + MIXBUFFER_SAMPLES;
            }

            int length44kHz = soundSystemLocal.MillisecondsToSamples((int) (over * 1000));

            for (int i = 0; i < SOUND_MAX_CHANNELS; i++) {
                idSoundChannel chan = channels[i];

                if (!chan.triggerState) {
                    continue;
                }
                if (channel != SCHANNEL_ANY && chan.triggerChannel != channel) {
                    continue;
                }

                // if it is already fading to this volume at this rate, don't change it
                if (chan.channelFade.fadeEndVolume == to
                        && chan.channelFade.fadeEnd44kHz - chan.channelFade.fadeStart44kHz == length44kHz) {
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
            return playing;
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

            if (removeStatus > REMOVE_STATUS_WAITSAMPLEFINISHED) {
                return 0.0f;
            }

            int localTime = soundSystemLocal.GetCurrent44kHzTime();

            // see if we can use our cached value
            if (ampTime == localTime) {
                return amplitude;
            }

            // calculate a new value
            ampTime = localTime;
            amplitude = soundWorld.FindAmplitude(this, localTime, null, SCHANNEL_ANY, false);

            return amplitude;
        }

        // for save games.  Index will always be > 0
        @Override
        public int Index() {
            return index;
        }
        //----------------------------------------------

        public void Clear() {
            int i;

            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                channels[i].ALStop();
                channels[i].Clear();
            }

            removeStatus = REMOVE_STATUS_SAMPLEFINISHED;
            distance = 0.0f;

            lastValidPortalArea = -1;

            playing = false;
            hasShakes = false;
            ampTime = 0;								// last time someone queried
            amplitude = 0;
            maxDistance = 10.0f;						// meters
            spatializedOrigin.Zero();

//	memset( &parms, 0, sizeof( parms ) );
            parms = new soundShaderParms_t();
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
            hasShakes = false;

            if (playing) {
                for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                    idSoundChannel chan = channels[i];

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
                            AL10.alGetSourcei(chan.openalSource, AL_SOURCE_STATE);
                        }
                        idSlowChannel slow = GetSlowChannel(chan);

                        if (soundWorld.slowmoActive && slow.IsActive()) {
                            if (slow.GetCurrentPosition().time >= chan.leadinSample.LengthIn44kHzSamples() / 2) {
                                chan.Stop();
                                // if this was an onDemand sound, purge the sample now
                                if (chan.leadinSample.onDemand) {
                                    chan.leadinSample.PurgeSoundSample();
                                }
                                continue;
                            }
                        } else if ((chan.trigger44kHzTime + chan.leadinSample.LengthIn44kHzSamples() < current44kHzTime) || (state == AL_STOPPED)) {
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
                    if (chan.decoder != null && chan.decoder.GetLastDecodeTime() < current44kHzTime - SOUND_DECODER_FREE_DELAY) {
                        chan.decoder.ClearDecoder();
                    }

                    hasActive = true;

                    if (chan.parms.shakes > 0.0f) {
                        hasShakes = true;
                    }
                }
            }

            // mark the entire sound emitter as non-playing if there aren't any active channels
            if (!hasActive) {
                playing = false;
                if (removeStatus == REMOVE_STATUS_WAITSAMPLEFINISHED) {
                    // this can now be reused by the next request for a new soundEmitter
                    removeStatus = REMOVE_STATUS_SAMPLEFINISHED;
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
            boolean hasActive = false;

            //
            // work out the maximum distance of all the playing channels
            //
            maxDistance = 0;

            for (i = 0; i < SOUND_MAX_CHANNELS; i++) {
                idSoundChannel chan = channels[i];

                if (!chan.triggerState) {
                    continue;
                }
                if (chan.parms.maxDistance > maxDistance) {
                    maxDistance = chan.parms.maxDistance;
                }
            }

            //
            // work out where the sound comes from
            //
            idVec3 realOrigin = origin.oMultiply(DOOM_TO_METERS);
            idVec3 len = listenerPos.oMinus(realOrigin);
            realDistance = len.LengthFast();

            if (realDistance >= maxDistance) {
                // no way to possibly hear it
                distance = realDistance;
                return;
            }

            //
            // work out virtual origin and distance, which may be from a portal instead of the actual origin
            //
            distance = maxDistance * METERS_TO_DOOM;
            if (listenerArea == -1) {		// listener is outside the world
                return;
            }
            if (rw != null) {
                // we have a valid renderWorld
                int soundInArea = rw.PointInArea(origin);
                if (soundInArea == -1) {
                    if (lastValidPortalArea == -1) {		// sound is outside the world
                        distance = realDistance;
                        spatializedOrigin = origin;			// sound is in our area
                        return;
                    }
                    soundInArea = lastValidPortalArea;
                }
                lastValidPortalArea = soundInArea;
                if (soundInArea == listenerArea) {
                    distance = realDistance;
                    spatializedOrigin = origin;			// sound is in our area
                    return;
                }

                soundWorld.ResolveOrigin(0, null, soundInArea, 0.0f, origin, this);
                distance /= METERS_TO_DOOM;
            } else {
                // no portals available
                distance = realDistance;
                spatializedOrigin = origin;			// sound is in our area
            }
        }

        public idSlowChannel GetSlowChannel(final idSoundChannel chan) {
            return slowChannels[indexOf(chan, channels)];//TODO: pointer subtraction
        }

        public void SetSlowChannel(final idSoundChannel chan, idSlowChannel slow) {
            slowChannels[indexOf(chan, channels)] = slow;
        }

        public void ResetSlowChannel(final idSoundChannel chan) {
            int index = indexOf(chan, channels);
            slowChannels[index].Reset();
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
    };
}
