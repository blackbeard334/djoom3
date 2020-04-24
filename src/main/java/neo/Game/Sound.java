package neo.Game;

import static neo.Game.Entity.EV_Activate;
import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import static neo.TempDump.etoi;
import static neo.framework.Common.EDITOR_SOUND;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.idlib.Lib.BIT;
import static neo.idlib.math.Angles.getAng_zero;
import static neo.idlib.math.Vector.getVec3_zero;

import java.util.HashMap;
import java.util.Map;

import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Class.eventCallback_t;
import neo.Game.GameSys.Class.eventCallback_t0;
import neo.Game.GameSys.Class.eventCallback_t1;
import neo.Game.GameSys.Class.idEventArg;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.sound.idSoundEmitter;
import neo.idlib.Dict_h.idDict;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;

/**
 *
 */
public class Sound {

    public static final idEventDef EV_Speaker_On    = new idEventDef("On", null);
    public static final idEventDef EV_Speaker_Off   = new idEventDef("Off", null);
    public static final idEventDef EV_Speaker_Timer = new idEventDef("<timer>", null);

    /*
     ===============================================================================

     SOUND SHADER DECL

     ===============================================================================
     */
    // unfortunately, our minDistance / maxDistance is specified in meters, and
    // we have far too many of them to change at this time.
    static final float DOOM_TO_METERS = 0.0254f;                    // doom to meters
    static final float METERS_TO_DOOM = (1.0f / DOOM_TO_METERS);    // meters to doom

    // sound shader flags
    public static final int SSF_PRIVATE_SOUND      = BIT(0);    // only plays for the current listenerId
    public static final int SSF_ANTI_PRIVATE_SOUND = BIT(1);    // plays for everyone but the current listenerId
    public static final int SSF_NO_OCCLUSION       = BIT(2);    // don't flow through portals, only use straight line
    public static final int SSF_GLOBAL             = BIT(3);    // play full volume to all speakers and all listeners
    public static final int SSF_OMNIDIRECTIONAL    = BIT(4);    // fall off with distance, but play same volume in all speakers
    public static final int SSF_LOOPING            = BIT(5);    // repeat the sound continuously
    public static final int SSF_PLAY_ONCE          = BIT(6);    // never restart if already playing on any channel of a given emitter
    public static final int SSF_UNCLAMPED          = BIT(7);    // don't clamp calculated volumes at 1.0
    public static final int SSF_NO_FLICKER         = BIT(8);    // always return 1.0 for volume queries
    public static final int SSF_NO_DUPS            = BIT(9);    // try not to play the same sound twice in a row

    // these options can be overriden from sound shader defaults on a per-emitter and per-channel basis
    static class soundShaderParms_t {

        float minDistance;
        float maxDistance;
        float volume;                  // in dB, unfortunately.  Negative values get quieter
        float shakes;
        int   soundShaderFlags;        // SSF_* bit flags
        int   soundClass;              // for global fading of sounds
    }

    static final int SOUND_MAX_LIST_WAVS = 32;

    // sound classes are used to fade most sounds down inside cinematics, leaving dialog
    // flagged with a non-zero class full volume
    static final int SOUND_MAX_CLASSES = 4;

    /*
     ===============================================================================

     Generic sound emitter.

     ===============================================================================
     */
    public static class idSound extends idEntity {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static Map<idEventDef, eventCallback_t> eventCallbacks = new HashMap<>();
        static {
            eventCallbacks.putAll(idEntity.getEventCallBacks());
            eventCallbacks.put(EV_Activate, (eventCallback_t1<idSound>) idSound::Event_Trigger);
            eventCallbacks.put(EV_Speaker_On, (eventCallback_t0<idSound>) idSound::Event_On);
            eventCallbacks.put(EV_Speaker_Off, (eventCallback_t0<idSound>) idSound::Event_Off);
            eventCallbacks.put(EV_Speaker_Timer, (eventCallback_t0<idSound>) idSound::Event_Timer);
        }


        private float    lastSoundVol;
        private float    soundVol;
        private float    random;
        private float    wait;
        private boolean  timerOn;
        private final idVec3   shakeTranslate;
        private final idAngles shakeRotate;
        private int      playingUntilTime;
        //
        //

//	CLASS_PROTOTYPE( idSound );
        public idSound() {
            this.lastSoundVol = 0.0f;
            this.soundVol = 0.0f;
            this.shakeTranslate = new idVec3();
            this.shakeRotate = new idAngles();
            this.random = 0.0f;
            this.wait = 0.0f;
            this.timerOn = false;
            this.playingUntilTime = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(this.lastSoundVol);
            savefile.WriteFloat(this.soundVol);
            savefile.WriteFloat(this.random);
            savefile.WriteFloat(this.wait);
            savefile.WriteBool(this.timerOn);
            savefile.WriteVec3(this.shakeTranslate);
            savefile.WriteAngles(this.shakeRotate);
            savefile.WriteInt(this.playingUntilTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            this.lastSoundVol = savefile.ReadFloat();
            this.soundVol = savefile.ReadFloat();
            this.random = savefile.ReadFloat();
            this.wait = savefile.ReadFloat();
            this.timerOn = savefile.ReadBool();
            savefile.ReadVec3(this.shakeTranslate);
            savefile.ReadAngles(this.shakeRotate);
            this.playingUntilTime = savefile.ReadInt();
        }

        @Override
        public void UpdateChangeableSpawnArgs(final idDict source) {

            super.UpdateChangeableSpawnArgs(source);

            if (source != null) {
                FreeSoundEmitter(true);
                this.spawnArgs.Copy(source);
                final idSoundEmitter saveRef = this.refSound.referenceSound;
                GameEdit.gameEdit.ParseSpawnArgsToRefSound(this.spawnArgs, this.refSound);
                this.refSound.referenceSound = saveRef;

                final idVec3 origin = new idVec3();
                final idMat3 axis = new idMat3();

                if (GetPhysicsToSoundTransform(origin, axis)) {
                    this.refSound.origin = GetPhysics().GetOrigin().oPlus(origin.oMultiply(axis));
                } else {
                    this.refSound.origin = GetPhysics().GetOrigin();
                }

                this.random = this.spawnArgs.GetFloat("random", "0");
                this.wait = this.spawnArgs.GetFloat("wait", "0");

                if ((this.wait > 0.0f) && (this.random >= this.wait)) {
                    this.random = this.wait - 0.001f;
                    gameLocal.Warning("speaker '%s' at (%s) has random >= wait", this.name, GetPhysics().GetOrigin().ToString(0));
                }

                if (!this.refSound.waitfortrigger && (this.wait > 0.0f)) {
                    this.timerOn = true;
                    DoSound(false);
                    CancelEvents(EV_Speaker_Timer);
                    PostEventSec(EV_Speaker_Timer, this.wait + (gameLocal.random.CRandomFloat() * this.random));
                } else if (!this.refSound.waitfortrigger && !((this.refSound.referenceSound != null) && this.refSound.referenceSound.CurrentlyPlaying())) {
                    // start it if it isn't already playing, and we aren't waitForTrigger
                    DoSound(true);
                    this.timerOn = false;
                }
            }
        }

        @Override
        public void Spawn() {
            super.Spawn();
            
            this.spawnArgs.GetVector("move", "0 0 0", this.shakeTranslate);
            this.spawnArgs.GetAngles("rotate", "0 0 0", this.shakeRotate);
            this.random = this.spawnArgs.GetFloat("random", "0");
            this.wait = this.spawnArgs.GetFloat("wait", "0");

            if ((this.wait > 0.0f) && (this.random >= this.wait)) {
                this.random = this.wait - 0.001f;
                gameLocal.Warning("speaker '%s' at (%s) has random >= wait", this.name, GetPhysics().GetOrigin().ToString(0));
            }

            this.soundVol = 0.0f;
            this.lastSoundVol = 0.0f;

            if (!this.shakeRotate.equals(getAng_zero()) || !this.shakeTranslate.equals(getVec3_zero())) {
                BecomeActive(TH_THINK);
            }

            if (!this.refSound.waitfortrigger && (this.wait > 0.0f)) {
                this.timerOn = true;
                PostEventSec(EV_Speaker_Timer, this.wait + (gameLocal.random.CRandomFloat() * this.random));
            } else {
                this.timerOn = false;
            }
        }

//        public void ToggleOnOff(idEntity other, idEntity activator);
        @Override
        public void Think() {
//	idAngles	ang;

            // run physics
            RunPhysics();

            // clear out our update visuals think flag since we never call Present
            BecomeInactive(TH_UPDATEVISUALS);
        }

        public void SetSound(final String sound, int channel /*= SND_CHANNEL_ANY*/) {
            final idSoundShader shader = declManager.FindSound(sound);
            if (!shader.equals(this.refSound.shader)) {
                FreeSoundEmitter(true);
            }
            GameEdit.gameEdit.ParseSpawnArgsToRefSound(this.spawnArgs, this.refSound);
            this.refSound.shader = shader;
            // start it if it isn't already playing, and we aren't waitForTrigger
            if (!this.refSound.waitfortrigger && !((this.refSound.referenceSound != null) && this.refSound.referenceSound.CurrentlyPlaying())) {
                DoSound(true);
            }
        }

        public void SetSound(final String sound) {
            SetSound(sound, SND_CHANNEL_ANY.ordinal());
        }

        @Override
        public void ShowEditingDialog() {
            common.InitTool(EDITOR_SOUND, this.spawnArgs);
        }


        /*
         ================
         idSound::Event_Trigger

         this will toggle the idle idSound on and off
         ================
         */
        private void Event_Trigger(idEventArg<idEntity> activator) {
            if (this.wait > 0.0f) {
                if (this.timerOn) {
                    this.timerOn = false;
                    CancelEvents(EV_Speaker_Timer);
                } else {
                    this.timerOn = true;
                    DoSound(true);
                    PostEventSec(EV_Speaker_Timer, this.wait + (gameLocal.random.CRandomFloat() * this.random));
                }
            } else {
                if (gameLocal.isMultiplayer) {
                    if ((this.refSound.referenceSound != null) && (gameLocal.time < this.playingUntilTime)) {
                        DoSound(false);
                    } else {
                        DoSound(true);
                    }
                } else {
                    if ((this.refSound.referenceSound != null) && this.refSound.referenceSound.CurrentlyPlaying()) {
                        DoSound(false);
                    } else {
                        DoSound(true);
                    }
                }
            }
        }

        private void Event_Timer() {
            DoSound(true);
            PostEventSec(EV_Speaker_Timer, this.wait + (gameLocal.random.CRandomFloat() * this.random));
        }

        private void Event_On() {
            if (this.wait > 0.0f) {
                this.timerOn = true;
                PostEventSec(EV_Speaker_Timer, this.wait + (gameLocal.random.CRandomFloat() * this.random));
            }
            DoSound(true);
        }

        private void Event_Off() {
            if (this.timerOn) {
                this.timerOn = false;
                CancelEvents(EV_Speaker_Timer);
            }
            DoSound(false);
        }

        private void DoSound(boolean play) {
            if (play) {
                final int[] playingUntilTime = {0};
                StartSoundShader(this.refSound.shader, etoi(SND_CHANNEL_ANY), this.refSound.parms.soundShaderFlags, true, playingUntilTime);
                this.playingUntilTime = playingUntilTime[0] + gameLocal.time;
            } else {
                StopSound(etoi(SND_CHANNEL_ANY), true);
                this.playingUntilTime = 0;
            }
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class<?> /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public eventCallback_t<?> getEventCallBack(idEventDef event) {
            return eventCallbacks.get(event);
        }

        public static Map<idEventDef, eventCallback_t> getEventCallBacks() {
            return eventCallbacks;
        }

    }
}
