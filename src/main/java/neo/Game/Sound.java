package neo.Game;

import static neo.Game.Entity.TH_THINK;
import static neo.Game.Entity.TH_UPDATEVISUALS;
import neo.Game.Entity.idEntity;
import neo.Game.GameSys.Class;
import neo.Game.GameSys.Event.idEventDef;
import neo.Game.GameSys.SaveGame.idRestoreGame;
import neo.Game.GameSys.SaveGame.idSaveGame;
import static neo.Game.Game_local.gameLocal;
import static neo.Game.Game_local.gameSoundChannel_t.SND_CHANNEL_ANY;
import neo.Sound.snd_shader.idSoundShader;
import neo.Sound.sound.idSoundEmitter;
import static neo.TempDump.etoi;
import static neo.framework.Common.EDITOR_SOUND;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import neo.idlib.Dict_h.idDict;
import static neo.idlib.Lib.BIT;
import static neo.idlib.math.Angles.ang_zero;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Matrix.idMat3;
import neo.idlib.math.Vector.idVec3;
import static neo.idlib.math.Vector.vec3_zero;

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

    ;

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

        private float   lastSoundVol;
        private float   soundVol;
        private float   random;
        private float   wait;
        private boolean timerOn;
        private idVec3 shakeTranslate;
        private idAngles shakeRotate;
        private int playingUntilTime;
        //
        //

//	CLASS_PROTOTYPE( idSound );
        public idSound() {
            lastSoundVol = 0.0f;
            soundVol = 0.0f;
            shakeTranslate.Zero();
            shakeRotate.Zero();
            random = 0.0f;
            wait = 0.0f;
            timerOn = false;
            playingUntilTime = 0;
        }

        @Override
        public void Save(idSaveGame savefile) {
            savefile.WriteFloat(lastSoundVol);
            savefile.WriteFloat(soundVol);
            savefile.WriteFloat(random);
            savefile.WriteFloat(wait);
            savefile.WriteBool(timerOn);
            savefile.WriteVec3(shakeTranslate);
            savefile.WriteAngles(shakeRotate);
            savefile.WriteInt(playingUntilTime);
        }

        @Override
        public void Restore(idRestoreGame savefile) {
            lastSoundVol = savefile.ReadFloat();
            soundVol = savefile.ReadFloat();
            random = savefile.ReadFloat();
            wait = savefile.ReadFloat();
            timerOn = savefile.ReadBool();
            savefile.ReadVec3(shakeTranslate);
            savefile.ReadAngles(shakeRotate);
            playingUntilTime = savefile.ReadInt();
        }

        @Override
        public void UpdateChangeableSpawnArgs(final idDict source) {

            super.UpdateChangeableSpawnArgs(source);

            if (source != null) {
                FreeSoundEmitter(true);
                spawnArgs.Copy(source);
                idSoundEmitter saveRef = refSound.referenceSound;
                GameEdit.gameEdit.ParseSpawnArgsToRefSound(spawnArgs, refSound);
                refSound.referenceSound = saveRef;

                idVec3 origin = new idVec3();
                idMat3 axis = new idMat3();

                if (GetPhysicsToSoundTransform(origin, axis)) {
                    refSound.origin = GetPhysics().GetOrigin().oPlus(origin.oMultiply(axis));
                } else {
                    refSound.origin = GetPhysics().GetOrigin();
                }

                random = spawnArgs.GetFloat("random", "0");
                wait = spawnArgs.GetFloat("wait", "0");

                if ((wait > 0.0f) && (random >= wait)) {
                    random = wait - 0.001f;
                    gameLocal.Warning("speaker '%s' at (%s) has random >= wait", name, GetPhysics().GetOrigin().ToString(0));
                }

                if (!refSound.waitfortrigger && (wait > 0.0f)) {
                    timerOn = true;
                    DoSound(false);
                    CancelEvents(EV_Speaker_Timer);
                    PostEventSec(EV_Speaker_Timer, wait + gameLocal.random.CRandomFloat() * random);
                } else if (!refSound.waitfortrigger && !(refSound.referenceSound != null && refSound.referenceSound.CurrentlyPlaying())) {
                    // start it if it isn't already playing, and we aren't waitForTrigger
                    DoSound(true);
                    timerOn = false;
                }
            }
        }

        @Override
        public void Spawn() {
            spawnArgs.GetVector("move", "0 0 0", shakeTranslate);
            spawnArgs.GetAngles("rotate", "0 0 0", shakeRotate);
            random = spawnArgs.GetFloat("random", "0");
            wait = spawnArgs.GetFloat("wait", "0");

            if ((wait > 0.0f) && (random >= wait)) {
                random = wait - 0.001f;
                gameLocal.Warning("speaker '%s' at (%s) has random >= wait", name, GetPhysics().GetOrigin().ToString(0));
            }

            soundVol = 0.0f;
            lastSoundVol = 0.0f;

            if ((shakeRotate != ang_zero) || (shakeTranslate != vec3_zero)) {
                BecomeActive(TH_THINK);
            }

            if (!refSound.waitfortrigger && (wait > 0.0f)) {
                timerOn = true;
                PostEventSec(EV_Speaker_Timer, wait + gameLocal.random.CRandomFloat() * random);
            } else {
                timerOn = false;
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
            if (!shader.equals(refSound.shader)) {
                FreeSoundEmitter(true);
            }
            GameEdit.gameEdit.ParseSpawnArgsToRefSound(spawnArgs, refSound);
            refSound.shader = shader;
            // start it if it isn't already playing, and we aren't waitForTrigger
            if (!refSound.waitfortrigger && !(refSound.referenceSound != null && refSound.referenceSound.CurrentlyPlaying())) {
                DoSound(true);
            }
        }

        public void SetSound(final String sound) {
            SetSound(sound, SND_CHANNEL_ANY.ordinal());
        }

        @Override
        public void ShowEditingDialog() {
            common.InitTool(EDITOR_SOUND, spawnArgs);
        }


        /*
         ================
         idSound::Event_Trigger

         this will toggle the idle idSound on and off
         ================
         */
        private void Event_Trigger(idEntity activator) {
            if (wait > 0.0f) {
                if (timerOn) {
                    timerOn = false;
                    CancelEvents(EV_Speaker_Timer);
                } else {
                    timerOn = true;
                    DoSound(true);
                    PostEventSec(EV_Speaker_Timer, wait + gameLocal.random.CRandomFloat() * random);
                }
            } else {
                if (gameLocal.isMultiplayer) {
                    if (refSound.referenceSound != null && (gameLocal.time < playingUntilTime)) {
                        DoSound(false);
                    } else {
                        DoSound(true);
                    }
                } else {
                    if (refSound.referenceSound != null && refSound.referenceSound.CurrentlyPlaying()) {
                        DoSound(false);
                    } else {
                        DoSound(true);
                    }
                }
            }
        }

        private void Event_Timer() {
            DoSound(true);
            PostEventSec(EV_Speaker_Timer, wait + gameLocal.random.CRandomFloat() * random);
        }

        private void Event_On() {
            if (wait > 0.0f) {
                timerOn = true;
                PostEventSec(EV_Speaker_Timer, wait + gameLocal.random.CRandomFloat() * random);
            }
            DoSound(true);
        }

        private void Event_Off() {
            if (timerOn) {
                timerOn = false;
                CancelEvents(EV_Speaker_Timer);
            }
            DoSound(false);
        }

        private void DoSound(boolean play) {
            if (play) {
                int[] playingUntilTime = {0};
                StartSoundShader(refSound.shader, etoi(SND_CHANNEL_ANY), refSound.parms.soundShaderFlags, true, playingUntilTime);
                this.playingUntilTime = playingUntilTime[0] + gameLocal.time;
            } else {
                StopSound(etoi(SND_CHANNEL_ANY), true);
                playingUntilTime = 0;
            }
        }

        @Override
        public Class.idClass CreateInstance() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public java.lang.Class /*idTypeInfo*/ GetType() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };
}
