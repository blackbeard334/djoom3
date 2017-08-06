package neo.Sound;

import java.nio.ByteBuffer;

import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;

/**
 *
 */
public class snd_efxfile {

    static class idSoundEffect {

        public idStr      name;
        public int        datasize;
        public ByteBuffer data;

        public idSoundEffect() {
        }
//	~idSoundEffect() { 
//		if ( data && datasize ) {
//			Mem_Free( data );
//			data = NULL;
//		}
//	}
    };

    static class idEFXFile {

        public idList<idSoundEffect> effects;

        public idEFXFile() {
            effects = new idList<>();
        }
//	~idEFXFile();

        public boolean FindEffect(idStr name, idSoundEffect[] effect, int[] index) {
            int i;

            for (i = 0; i < effects.Num(); i++) {
                if (effects.oGet(i) != null && (effects.oGet(i).name.equals(name))) {
                    effect[0] = effects.oGet(i);
                    index[0] = i;
                    return true;
                }
            }
            return false;
        }

        public boolean ReadEffect(idLexer src, idSoundEffect effect) {
            return false;
//            idToken name, token;
//
//            if (!src.ReadToken(token))
//                return false;
//
//            // reverb effect
//            if (token.equals("reverb")) {
//                EAXREVERBPROPERTIES reverb = (EAXREVERBPROPERTIES *) Mem_Alloc(sizeof(EAXREVERBPROPERTIES));
//                if (EFXUtil.isEffectSupported(EFX10.AL_EFFECT_REVERB)) {
//                    src.ReadTokenOnLine(token);
//                    name = token;
//
//                    if (!src.ReadToken(token)) {
////				Mem_Free( reverb );
//                        return false;
//                    }
//
//                    if (!token.equals("{")) {
//                        src.Error("idEFXFile::ReadEffect: { not found, found %s", token.c_str());
////				Mem_Free( reverb );
//                        return false;
//                    }
//
//                    do {
//                        if (!src.ReadToken(token)) {
//                            src.Error("idEFXFile::ReadEffect: EOF without closing brace");
////					Mem_Free( reverb );
//                            return false;
//                        }
//
//                        if (token.equals("}")) {
//                            effect.name = name;
//                            effect.data = reverb;
//                            effect.datasize = sizeof(EAXREVERBPROPERTIES);
//                            break;
//                        }
//
//                        switch (token.toString()) {
//                            case "environment":
//                                src.ReadTokenOnLine(token);
//                                reverb.ulEnvironment = token.GetUnsignedLongValue();
//                                break;
//                            case "environment size":
//                                reverb.flEnvironmentSize = src.ParseFloat();
//                                break;
//                            case "environment diffusion":
//                                reverb.flEnvironmentDiffusion = src.ParseFloat();
//                                break;
//                            case "room":
//                                reverb.lRoom = src.ParseInt();
//                                break;
//                            case "room hf":
//                                reverb.lRoomHF = src.ParseInt();
//                                break;
//                            case "room lf":
//                                reverb.lRoomLF = src.ParseInt();
//                                break;
//                            case "decay time":
//                                reverb.flDecayTime = src.ParseFloat();
//                                break;
//                            case "decay hf ratio":
//                                reverb.flDecayHFRatio = src.ParseFloat();
//                                break;
//                            case "decay lf ratio":
//                                reverb.flDecayLFRatio = src.ParseFloat();
//                                break;
//                            case "reflections":
//                                reverb.lReflections = src.ParseInt();
//                                break;
//                            case "reflections delay":
//                                reverb.flReflectionsDelay = src.ParseFloat();
//                                break;
//                            case "reflections pan":
//                                reverb.vReflectionsPan.x = src.ParseFloat();
//                                reverb.vReflectionsPan.y = src.ParseFloat();
//                                reverb.vReflectionsPan.z = src.ParseFloat();
//                                break;
//                            case "reverb":
//                                reverb.lReverb = src.ParseInt();
//                                break;
//                            case "reverb delay":
//                                reverb.flReverbDelay = src.ParseFloat();
//                                break;
//                            case "reverb pan":
//                                reverb.vReverbPan.x = src.ParseFloat();
//                                reverb.vReverbPan.y = src.ParseFloat();
//                                reverb.vReverbPan.z = src.ParseFloat();
//                                break;
//                            case "echo time":
//                                reverb.flEchoTime = src.ParseFloat();
//                                break;
//                            case "echo depth":
//                                reverb.flEchoDepth = src.ParseFloat();
//                                break;
//                            case "modulation time":
//                                reverb.flModulationTime = src.ParseFloat();
//                                break;
//                            case "modulation depth":
//                                reverb.flModulationDepth = src.ParseFloat();
//                                break;
//                            case "air absorption hf":
//                                reverb.flAirAbsorptionHF = src.ParseFloat();
//                                break;
//                            case "hf reference":
//                                reverb.flHFReference = src.ParseFloat();
//                                break;
//                            case "l`f reference":
//                                reverb.flLFReference = src.ParseFloat();
//                                break;
//                            case "room rolloff factor":
//                                reverb.flRoomRolloffFactor = src.ParseFloat();
//                                break;
//                            case "flags":
//                                src.ReadTokenOnLine(token);
//                                reverb.ulFlags = token.GetUnsignedLongValue();
//                                break;
//                            default:
//                                src.ReadTokenOnLine(token);
//                                src.Error("idEFXFile::ReadEffect: Invalid parameter in reverb definition");
////					Mem_Free( reverb );
//                        }
//                    } while (true);
//
//                    return true;
//                }
//            } else {
//                // other effect (not supported at the moment)
//                src.Error("idEFXFile::ReadEffect: Unknown effect definition");
//            }
//
//            return false;
        }

        public boolean LoadFile(final String filename, boolean OSPath /*= false*/) {
            idLexer src = new idLexer(LEXFL_NOSTRINGCONCAT);
            idToken token;

            src.LoadFile(filename, OSPath);
            if (!src.IsLoaded()) {
                return false;
            }

            if (!src.ExpectTokenString("Version")) {
                return false;//NULL;
            }

            if (src.ParseInt() != 1) {
                src.Error("idEFXFile::LoadFile: Unknown file version");
                return false;
            }

//            while (!src.EndOfFile()) {//TODO: would be nice to have some reverb
//                idSoundEffect effect = new idSoundEffect();
//                if (ReadEffect(src, effect)) {
//                    effects.Append(effect);
//                }
//            };

            return true;
        }

        public boolean LoadFile(final String filename) {
            return this.LoadFile(filename, false);
        }

        public void UnloadFile() {
            Clear();
        }

        public void Clear() {
            effects.DeleteContents(true);
        }

    };
}
