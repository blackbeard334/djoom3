/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package neo.sys;

import java.util.logging.Level;
import java.util.logging.Logger;
import neo.Sound.snd_local.idAudioHardware;
import neo.Sound.snd_system.idSoundSystemLocal;
import neo.TempDump.TODO_Exception;
import static neo.framework.BuildDefines.ID_OPENAL;
import static neo.framework.Common.common;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;

/**
 *
 */
public class win_snd {

    public static class idAudioHardwareWIN32 extends idAudioHardware {

//        private LPDIRECTSOUND m_pDS;
//        private LPDIRECTSOUNDBUFFER pDSBPrimary;
//        private idAudioBufferWIN32 speakers;
//
        private int numSpeakers;
        private int bitsPerSample;
        private int bufferSize;		// allocate buffer handed over to DirectSound
        private int blockAlign;		// channels * bits per sample / 8: sound frame size

        public idAudioHardwareWIN32() {

        }
        // ~idAudioHardwareWIN32();

// public    boolean Initialize( );
// public	boolean InitializeSpeakers( byte *buffer, int bufferSize, dword dwPrimaryFreq, dword dwPrimaryBitRate, dword dwSpeakers );
// public	void SetPrimaryBufferFormat( dword dwPrimaryFreq, dword dwPrimaryBitRate, dword dwSpeakers );
// public    int Create( idWaveFile* pWaveFile, idAudioBuffer** ppiab );
// public    int Create( idAudioBuffer** ppSound, const char* strWaveFileName, dword dwCreationFlags = 0 );
// public    int CreateFromMemory( idAudioBufferWIN32** ppSound, byte* pbData, ulong ulDataSize, waveformatextensible_t *pwfx, dword dwCreationFlags = 0 );
// public	boolean Lock( void **pDSLockedBuffer, ulong *dwDSLockedBufferSize );
// public	boolean Unlock( void *pDSLockedBuffer, dword dwDSLockedBufferSize );
// public	boolean GetCurrentPosition( ulong *pdwCurrentWriteCursor );
        @Override
        public int GetNumberOfSpeakers() {
            return numSpeakers;
        }

        @Override
        public int GetMixBufferSize() {
            return MIXBUFFER_SAMPLES * blockAlign;
        }

        // WIN32 driver doesn't support write API
        @Override
        public boolean Flush() {
            return true;
        }

        @Override
        public void Write(boolean value) {
        }

        @Override
        public short[] GetMixBuffer() {
            return null;
        }

        @Override
        public boolean Initialize() {
            try {
                AL.create(/*null, PRIMARYFREQ, idSoundSystemLocal.s_numberOfSpeakers.GetInteger(), false*/);
//            throw new TODO_Exception();
//    int             hr;
//
//	bufferSize = 0;
//	numSpeakers = 0;
//	blockAlign = 0;
//
//    SAFE_RELEASE( m_pDS );
//
//    // Create IDirectSound using the primary sound device
//    if( FAILED( hr = DirectSoundCreate( NULL, &m_pDS, NULL ) )) {
//        return false;
//	}
//
//    // Set primary buffer format
//	SetPrimaryBufferFormat( PRIMARYFREQ, 16, idSoundSystemLocal::s_numberOfSpeakers.GetInteger() );
//	return true;
            } catch (LWJGLException ex) {
                Logger.getLogger(win_snd.class.getName()).log(Level.SEVERE, null, ex);
            }

            return AL.isCreated();
        }

        @Override
        public boolean Lock(Object pDSLockedBuffer, long dwDSLockedBufferSize) {
            throw new TODO_Exception();
//	if (speakers) {
//		return speakers->Lock( pDSLockedBuffer, dwDSLockedBufferSize );
//	}
//	return false;
        }

        @Override
        public boolean Unlock(Object pDSLockedBuffer, long dwDSLockedBufferSize) {
            throw new TODO_Exception();
//	if (speakers) {
//		return speakers->Unlock( pDSLockedBuffer, dwDSLockedBufferSize );
//	}
//	return false;
        }

        @Override
        public boolean GetCurrentPosition(long pdwCurrentWriteCursor) {
            throw new TODO_Exception();
//	if (speakers) {
//		return speakers->GetCurrentPosition( pdwCurrentWriteCursor );
//	}
//	return false;
        }

    };

    /*
     ===============
     Sys_LoadOpenAL
     ===============
     */
    public static boolean Sys_LoadOpenAL() {
        if (ID_OPENAL) {
            try {
                AL.create();
            } catch (LWJGLException | UnsatisfiedLinkError ex) {
                Logger.getLogger(win_snd.class.getName()).log(Level.SEVERE, null, ex);
                common.Warning("LoadLibrary %s failed.", idSoundSystemLocal.s_libOpenAL.GetString());
                return false;
            }

            return AL.isCreated();
        } else {
            return false;
        }
    }

    /*
     ===============
     Sys_FreeOpenAL
     ===============
     */
    public static void Sys_FreeOpenAL() {
        AL.destroy();
    }

}
