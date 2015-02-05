/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package neo.Sound;

import java.nio.ByteBuffer;
import neo.TempDump.TODO_Exception;
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
            effects = new idList<idSoundEffect>();
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

        public boolean ReadEffect(idLexer lexer, idSoundEffect effect) {
            throw new TODO_Exception();
//	idToken name, token;
//	
//	if ( !src.ReadToken( &token ) )
//		return false;
//
//	// reverb effect
//	if ( token == "reverb" ) {
//		EAXREVERBPROPERTIES *reverb = ( EAXREVERBPROPERTIES * )Mem_Alloc( sizeof( EAXREVERBPROPERTIES ) );
//		if ( reverb ) {
//			src.ReadTokenOnLine( &token );
//			name = token;
//				
//			if ( !src.ReadToken( &token ) ) {
//				Mem_Free( reverb );
//				return false;
//			}
//			
//			if ( token != "{" ) {
//				src.Error( "idEFXFile::ReadEffect: { not found, found %s", token.c_str() );
//				Mem_Free( reverb );
//				return false;
//			}
//			
//			do {
//				if ( !src.ReadToken( &token ) ) {
//					src.Error( "idEFXFile::ReadEffect: EOF without closing brace" );
//					Mem_Free( reverb );
//					return false;
//				}
//
//				if ( token == "}" ) {
//					effect.name = name;
//					effect.data = ( void * )reverb;
//					effect.datasize = sizeof( EAXREVERBPROPERTIES );
//					break;
//				}
//
//				if ( token == "environment" ) {
//					src.ReadTokenOnLine( &token );
//					reverb.ulEnvironment = token.GetUnsignedLongValue();
//				} else if ( token == "environment size" ) {
//					reverb.flEnvironmentSize = src.ParseFloat();
//				} else if ( token == "environment diffusion" ) {
//					reverb.flEnvironmentDiffusion = src.ParseFloat();
//				} else if ( token == "room" ) {
//					reverb.lRoom = src.ParseInt();
//				} else if ( token == "room hf" ) {
//					reverb.lRoomHF = src.ParseInt();
//				} else if ( token == "room lf" ) {
//					reverb.lRoomLF = src.ParseInt();
//				} else if ( token == "decay time" ) {
//					reverb.flDecayTime = src.ParseFloat();
//				} else if ( token == "decay hf ratio" ) {
//					reverb.flDecayHFRatio = src.ParseFloat();
//				} else if ( token == "decay lf ratio" ) {
//					reverb.flDecayLFRatio = src.ParseFloat();
//				} else if ( token == "reflections" ) {
//					reverb.lReflections = src.ParseInt();
//				} else if ( token == "reflections delay" ) {
//					reverb.flReflectionsDelay = src.ParseFloat();
//				} else if ( token == "reflections pan" ) {
//					reverb.vReflectionsPan.x = src.ParseFloat();
//					reverb.vReflectionsPan.y = src.ParseFloat();
//					reverb.vReflectionsPan.z = src.ParseFloat();
//				} else if ( token == "reverb" ) {
//					reverb.lReverb = src.ParseInt();
//				} else if ( token == "reverb delay" ) {
//					reverb.flReverbDelay = src.ParseFloat();
//				} else if ( token == "reverb pan" ) {
//					reverb.vReverbPan.x = src.ParseFloat();
//					reverb.vReverbPan.y = src.ParseFloat();
//					reverb.vReverbPan.z = src.ParseFloat();
//				} else if ( token == "echo time" ) {
//					reverb.flEchoTime = src.ParseFloat();
//				} else if ( token == "echo depth" ) {
//					reverb.flEchoDepth = src.ParseFloat();
//				} else if ( token == "modulation time" ) {
//					reverb.flModulationTime = src.ParseFloat();
//				} else if ( token == "modulation depth" ) {
//					reverb.flModulationDepth = src.ParseFloat();
//				} else if ( token == "air absorption hf" ) {
//					reverb.flAirAbsorptionHF = src.ParseFloat();
//				} else if ( token == "hf reference" ) {
//					reverb.flHFReference = src.ParseFloat();
//				} else if ( token == "lf reference" ) {
//					reverb.flLFReference = src.ParseFloat();
//				} else if ( token == "room rolloff factor" ) {
//					reverb.flRoomRolloffFactor = src.ParseFloat();
//				} else if ( token == "flags" ) {
//					src.ReadTokenOnLine( &token );
//					reverb.ulFlags = token.GetUnsignedLongValue();
//				} else {
//					src.ReadTokenOnLine( &token );
//					src.Error( "idEFXFile::ReadEffect: Invalid parameter in reverb definition" );
//					Mem_Free( reverb );
//				}
//			} while ( 1 );
//
//			return true;
//		}
//	} else {
//		// other effect (not supported at the moment)
//		src.Error( "idEFXFile::ReadEffect: Unknown effect definition" );
//	}
//
//	return false;
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

            while (!src.EndOfFile()) {
                idSoundEffect effect = new idSoundEffect();
                if (ReadEffect(src, effect)) {
                    effects.Append(effect);
                }
            };

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
