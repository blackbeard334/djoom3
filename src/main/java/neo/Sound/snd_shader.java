package neo.Sound;

import static neo.Sound.snd_local.WAVE_FORMAT_TAG_OGG;
import static neo.Sound.snd_system.soundSystemLocal;
import static neo.TempDump.etoi;
import static neo.TempDump.sizeof;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.Common.com_makingBuild;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Text.Str.va;
import static neo.idlib.Text.Token.TT_NUMBER;
import static neo.idlib.math.Simd.speakerLabel.SPEAKER_BACKLEFT;
import static neo.idlib.math.Simd.speakerLabel.SPEAKER_BACKRIGHT;
import static neo.idlib.math.Simd.speakerLabel.SPEAKER_CENTER;
import static neo.idlib.math.Simd.speakerLabel.SPEAKER_LEFT;
import static neo.idlib.math.Simd.speakerLabel.SPEAKER_LFE;
import static neo.idlib.math.Simd.speakerLabel.SPEAKER_RIGHT;

import neo.Sound.snd_cache.idSoundSample;
import neo.Sound.snd_system.idSoundSystemLocal;
import neo.framework.DeclManager.idDecl;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.StrList.idStrList;
import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class snd_shader {

    /*
     ===============================================================================

     SOUND SHADER DECL

     ===============================================================================
     */
    // unfortunately, our minDistance / maxDistance is specified in meters, and
    // we have far too many of them to change at this time.
    static final float DOOM_TO_METERS         = 0.0254f;                    // doom to meters
    static final float METERS_TO_DOOM         = (1.0f / DOOM_TO_METERS);    // meters to doom
    //
    //
    // sound shader flags
    static final int   SSF_PRIVATE_SOUND      = BIT(0);    // only plays for the current listenerId
    static final int   SSF_ANTI_PRIVATE_SOUND = BIT(1);    // plays for everyone but the current listenerId
    static final int   SSF_NO_OCCLUSION       = BIT(2);    // don't flow through portals, only use straight line
    static final int   SSF_GLOBAL             = BIT(3);    // play full volume to all speakers and all listeners
    static final int   SSF_OMNIDIRECTIONAL    = BIT(4);    // fall off with distance, but play same volume in all speakers
    static final int   SSF_LOOPING            = BIT(5);    // repeat the sound continuously
    static final int   SSF_PLAY_ONCE          = BIT(6);    // never restart if already playing on any channel of a given emitter
    static final int   SSF_UNCLAMPED          = BIT(7);    // don't clamp calculated volumes at 1.0
    static final int   SSF_NO_FLICKER         = BIT(8);    // always return 1.0 for volume queries
    static final int   SSF_NO_DUPS            = BIT(9);    // try not to play the same sound twice in a row

    // these options can be overriden from sound shader defaults on a per-emitter and per-channel basis
    public static class soundShaderParms_t {

        public float minDistance;
        public float maxDistance;
        public float volume;            // in dB, unfortunately.  Negative values get quieter
        public float shakes;
        public int   soundShaderFlags;        // SSF_* bit flags
        public int   soundClass;            // for global fading of sounds
    }
    //
    //
    static final int SOUND_MAX_LIST_WAVS = 32;

    // sound classes are used to fade most sounds down inside cinematics, leaving dialog
    // flagged with a non-zero class full volume
    static final int SOUND_MAX_CLASSES = 4;

    // it is somewhat tempting to make this a virtual class to hide the private
    // details here, but that doesn't fit easily with the decl manager at the moment.
    public static class idSoundShader extends idDecl {

        // friend class idSoundWorldLocal;
        // friend class idSoundEmitterLocal;
        // friend class idSoundChannel;
        // friend class idSoundCache;
        //
        //
        // options from sound shader text
        soundShaderParms_t parms;                   // can be overriden on a per-channel basis
        //
        private boolean onDemand;                   // only load when played, and free when finished
        int speakerMask;
        private idSoundShader altSound;
        private final idStr         desc;                 // description
        private boolean       errorDuringParse;
        float leadinVolume;                         // allows light breaking leadin sounds to be much louder than the broken loop
        //
        idSoundSample[] leadins = new idSoundSample[SOUND_MAX_LIST_WAVS];
        private int numLeadins;
        idSoundSample[] entries = new idSoundSample[SOUND_MAX_LIST_WAVS];
        int numEntries;
        //
        //

        public idSoundShader() {
            this.parms = new soundShaderParms_t();
            this.desc = new idStr();
            Init();
        }
        // virtual					~idSoundShader();

        @Override
        public long Size() {
            return sizeof(idSoundShader.class);
        }

        @Override
        public boolean SetDefaultText() {
            idStr wavName;

            wavName = new idStr(GetName());
            wavName.DefaultFileExtension(".wav");		// if the name has .ogg in it, that will stay

            // if there exists a wav file with the same name
            if (true) { //fileSystem->ReadFile( wavname, NULL ) != -1 ) {
                final StringBuilder generated = new StringBuilder(2048);
                idStr.snPrintf(generated, generated.capacity(),
                        "sound %s // IMPLICITLY GENERATED\n"
                        + "{\n"
                        + "%s\n"
                        + "}\n", GetName(), wavName);
                SetText(generated.toString());
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "_default.wav\n"
                    + "}";
        }

        /*
         ===============
         idSoundShader::Parse

         this is called by the declManager
         ===============
         */
        @Override
        public boolean Parse(final String text, final int textLength) {
            final idLexer src = new idLexer();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            // deeper functions can set this, which will cause MakeDefault() to be called at the end
            this.errorDuringParse = false;

            if (!ParseShader(src) || this.errorDuringParse) {
                MakeDefault();
                return false;
            }
            return true;
        }

        @Override
        public void FreeData() {
            this.numEntries = 0;
            this.numLeadins = 0;
        }

        @Override
        public void List() {
            final idStrList shaders;

            common.Printf("%4d: %s\n", Index(), GetName());
            if (idStr.Icmp(GetDescription(), "<no description>") != 0) {
                common.Printf("      description: %s\n", GetDescription());
            }
            for (int k = 0; k < this.numLeadins; k++) {
                final idSoundSample objectp = this.leadins[k];
                if (objectp != null) {
                    common.Printf("      %5dms %4dKb %s (LEADIN)\n", soundSystemLocal.SamplesToMilliseconds(objectp.LengthIn44kHzSamples()), (objectp.objectMemSize / 1024), objectp.name);
                }
            }
            for (int k = 0; k < this.numEntries; k++) {
                final idSoundSample objectp = this.entries[k];
                if (objectp != null) {
                    common.Printf("      %5dms %4dKb %s\n", soundSystemLocal.SamplesToMilliseconds(objectp.LengthIn44kHzSamples()), (objectp.objectMemSize / 1024), objectp.name);
                }
            }
        }

        public String GetDescription() {
            return this.desc.getData();
        }

        // so the editor can draw correct default sound spheres
        // this is currently defined as meters, which sucks, IMHO.
        public float GetMinDistance() {        // FIXME: replace this with a GetSoundShaderParms()
            return this.parms.minDistance;
        }

        public float GetMaxDistance() {
            return this.parms.maxDistance;
        }

        // returns NULL if an AltSound isn't defined in the shader.
        // we use this for pairing a specific broken light sound with a normal light sound
        public idSoundShader GetAltSound() {
            return this.altSound;
        }

        public boolean HasDefaultSound() {
            for (int i = 0; i < this.numEntries; i++) {
                if ((this.entries[i] != null) && this.entries[i].defaultSound) {
                    return true;
                }
            }
            return false;
        }

        public soundShaderParms_t GetParms() {
            return this.parms;
        }

        public int GetNumSounds() {
            return this.numLeadins + this.numEntries;
        }

        public String GetSound(int index) {
            if (index >= 0) {
                if (index < this.numLeadins) {
                    return this.leadins[index].name.getData();
                }
                index -= this.numLeadins;
                if (index < this.numEntries) {
                    return this.entries[index].name.getData();
                }
            }
            return "";
        }

        public boolean CheckShakesAndOgg() {
            int i;
            boolean ret = false;

            for (i = 0; i < this.numLeadins; i++) {
                if (this.leadins[i].objectInfo.wFormatTag == WAVE_FORMAT_TAG_OGG) {
                    common.Warning("sound shader '%s' has shakes and uses OGG file '%s'",
                            GetName(), this.leadins[ i].name);
                    ret = true;
                }
            }
            for (i = 0; i < this.numEntries; i++) {
                if (this.entries[i].objectInfo.wFormatTag == WAVE_FORMAT_TAG_OGG) {
                    common.Warning("sound shader '%s' has shakes and uses OGG file '%s'",
                            GetName(), this.entries[ i].name);
                    ret = true;
                }
            }
            return ret;
        }

        private void Init() {
            this.desc.oSet("<no description>");
            this.errorDuringParse = false;
            this.onDemand = false;
            this.numEntries = 0;
            this.numLeadins = 0;
            this.leadinVolume = 0;
            this.altSound = null;
        }

        private boolean ParseShader(idLexer src) {
            int i;
            final idToken token = new idToken();

            this.parms.minDistance = 1;
            this.parms.maxDistance = 10;
            this.parms.volume = 1;
            this.parms.shakes = 0;
            this.parms.soundShaderFlags = 0;
            this.parms.soundClass = 0;

            this.speakerMask = 0;
            this.altSound = null;

            for (i = 0; i < SOUND_MAX_LIST_WAVS; i++) {
                this.leadins[i] = null;
                this.entries[i] = null;
            }
            this.numEntries = 0;
            this.numLeadins = 0;

            int maxSamples = idSoundSystemLocal.s_maxSoundsPerShader.GetInteger();
            if (com_makingBuild.GetBool() || (maxSamples <= 0) || (maxSamples > SOUND_MAX_LIST_WAVS)) {
                maxSamples = SOUND_MAX_LIST_WAVS;
            }

            while (true) {
                if (!src.ExpectAnyToken(token)) {
                    return false;
                } // end of definition
                else if (token.equals("}")) {
                    break;
                } // minimum number of sounds
                else if (0 == token.Icmp("minSamples")) {
                    maxSamples = idMath.ClampInt(src.ParseInt(), SOUND_MAX_LIST_WAVS, maxSamples);
                } // description
                else if (0 == token.Icmp("description")) {
                    src.ReadTokenOnLine(token);
                    this.desc.oSet(token);
                } // mindistance
                else if (0 == token.Icmp("mindistance")) {
                    this.parms.minDistance = src.ParseFloat();
                } // maxdistance
                else if (0 == token.Icmp("maxdistance")) {
                    this.parms.maxDistance = src.ParseFloat();
                } // shakes screen
                else if (0 == token.Icmp("shakes")) {
                    src.ExpectAnyToken(token);
                    if (token.type == TT_NUMBER) {
                        this.parms.shakes = token.GetFloatValue();
                    } else {
                        src.UnreadToken(token);
                        this.parms.shakes = 1.0f;
                    }
                } // reverb
                else if (0 == token.Icmp("reverb")) {
                    final float reg0 = src.ParseFloat();
                    if (!src.ExpectTokenString(",")) {
                        src.FreeSource();
                        return false;
                    }
                    final float reg1 = src.ParseFloat();
                    // no longer supported
                } // volume
                else if (0 == token.Icmp("volume")) {
                    this.parms.volume = src.ParseFloat();
                } // leadinVolume is used to allow light breaking leadin sounds to be much louder than the broken loop
                else if (0 == token.Icmp("leadinVolume")) {
                    this.leadinVolume = src.ParseFloat();
                } // speaker mask
                else if (0 == token.Icmp("mask_center")) {
                    this.speakerMask |= 1 << etoi(SPEAKER_CENTER);
                } // speaker mask
                else if (0 == token.Icmp("mask_left")) {
                    this.speakerMask |= 1 << etoi(SPEAKER_LEFT);
                } // speaker mask
                else if (0 == token.Icmp("mask_right")) {
                    this.speakerMask |= 1 << etoi(SPEAKER_RIGHT);
                } // speaker mask
                else if (0 == token.Icmp("mask_backright")) {
                    this.speakerMask |= 1 << etoi(SPEAKER_BACKRIGHT);
                } // speaker mask
                else if (0 == token.Icmp("mask_backleft")) {
                    this.speakerMask |= 1 << etoi(SPEAKER_BACKLEFT);
                } // speaker mask
                else if (0 == token.Icmp("mask_lfe")) {
                    this.speakerMask |= 1 << etoi(SPEAKER_LFE);
                } // soundClass
                else if (0 == token.Icmp("soundClass")) {
                    this.parms.soundClass = src.ParseInt();
                    if ((this.parms.soundClass < 0) || (this.parms.soundClass >= SOUND_MAX_CLASSES)) {
                        src.Warning("SoundClass out of range");
                        return false;
                    }
                } // altSound
                else if (0 == token.Icmp("altSound")) {
                    if (!src.ExpectAnyToken(token)) {
                        return false;
                    }
                    this.altSound = declManager.FindSound(token);
                } // ordered
                else if (0 == token.Icmp("ordered")) {
                    // no longer supported
                } // no_dups
                else if (0 == token.Icmp("no_dups")) {
                    this.parms.soundShaderFlags |= SSF_NO_DUPS;
                } // no_flicker
                else if (0 == token.Icmp("no_flicker")) {
                    this.parms.soundShaderFlags |= SSF_NO_FLICKER;
                } // plain
                else if (0 == token.Icmp("plain")) {
                    // no longer supported
                } // looping
                else if (0 == token.Icmp("looping")) {
                    this.parms.soundShaderFlags |= SSF_LOOPING;
                } // no occlusion
                else if (0 == token.Icmp("no_occlusion")) {
                    this.parms.soundShaderFlags |= SSF_NO_OCCLUSION;
                } // private
                else if (0 == token.Icmp("private")) {
                    this.parms.soundShaderFlags |= SSF_PRIVATE_SOUND;
                } // antiPrivate
                else if (0 == token.Icmp("antiPrivate")) {
                    this.parms.soundShaderFlags |= SSF_ANTI_PRIVATE_SOUND;
                } // once
                else if (0 == token.Icmp("playonce")) {
                    this.parms.soundShaderFlags |= SSF_PLAY_ONCE;
                } // global
                else if (0 == token.Icmp("global")) {
                    this.parms.soundShaderFlags |= SSF_GLOBAL;
                } // unclamped
                else if (0 == token.Icmp("unclamped")) {
                    this.parms.soundShaderFlags |= SSF_UNCLAMPED;
                } // omnidirectional
                else if (0 == token.Icmp("omnidirectional")) {
                    this.parms.soundShaderFlags |= SSF_OMNIDIRECTIONAL;
                } // onDemand can't be a parms, because we must track all references and overrides would confuse it
                else if (0 == token.Icmp("onDemand")) {
                    // no longer loading sounds on demand
                    //onDemand = true;
                } // the wave files
                else if (0 == token.Icmp("leadin")) {
                    // add to the leadin list
                    if (!src.ReadToken(token)) {
                        src.Warning("Expected sound after leadin");
                        return false;
                    }
                    if ((soundSystemLocal.soundCache != null) && (this.numLeadins < maxSamples)) {
                        this.leadins[ this.numLeadins] = soundSystemLocal.soundCache.FindSound(token, this.onDemand);
                        this.numLeadins++;
                    }
                } else if ((token.Find(".wav", false) != -1) || (token.Find(".ogg", false) != -1)) {
                    // add to the wav list
                    if ((soundSystemLocal.soundCache != null) && (this.numEntries < maxSamples)) {
                        token.BackSlashesToSlashes();
                        final idStr lang = new idStr(cvarSystem.GetCVarString("sys_lang"));
                        if ((lang.Icmp("english") != 0) && (token.Find("sound/vo/", false) >= 0)) {
                            final idStr work = new idStr(token);
                            work.ToLower();
                            work.StripLeading("sound/vo/");
                            work.oSet(va("sound/vo/%s/%s", lang.getData(), work.getData()));
                            if (fileSystem.ReadFile(work.getData(), null, null) > 0) {
                                token.oSet(work);
                            } else {
                                // also try to find it with the .ogg extension
                                work.SetFileExtension(".ogg");
                                if (fileSystem.ReadFile(work, null, null) > 0) {
                                    token.oSet(work);
                                }
                            }
                        }
                        this.entries[ this.numEntries] = soundSystemLocal.soundCache.FindSound(token, this.onDemand);
                        this.numEntries++;
                    }
                } else {
                    src.Warning("unknown token '%s'", token);
                    return false;
                }
            }

            if (this.parms.shakes > 0.0f) {
                CheckShakesAndOgg();
            }

            return true;
        }

        public void oSet(idSoundShader FindSound) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
