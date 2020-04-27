package neo.sys;

import static neo.Sound.snd_local.PRIMARYFREQ;
import static neo.framework.BuildDefines.ID_OPENAL;
import static neo.framework.Common.common;
import static neo.idlib.math.Simd.MIXBUFFER_SAMPLES;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.SourceDataLine;

import org.lwjgl.openal.ALC;

import neo.TempDump.TODO_Exception;
import neo.Sound.snd_local.idAudioHardware;
import neo.Sound.snd_system.idSoundSystemLocal;

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

// public	boolean InitializeSpeakers( byte *buffer, int bufferSize, dword dwPrimaryFreq, dword dwPrimaryBitRate, dword dwSpeakers );

        /*
         ===============
         idAudioHardwareWIN32::SetPrimaryBufferFormat
         Set primary buffer to a specified format 
         For example, to set the primary buffer format to 22kHz stereo, 16-bit
         then:   dwPrimaryChannels = 2
         dwPrimaryFreq     = 22050, 
         dwPrimaryBitRate  = 16
         ===============
         */
        public void SetPrimaryBufferFormat(int dwPrimaryFreq, int dwPrimaryBitRate, int dwSpeakers) {
//    HRESULT             hr;
//
//    if( m_pDS == null ) {
//        return;
//	}
//
//	ulong cfgSpeakers;
//	m_pDS->GetSpeakerConfig( &cfgSpeakers );
//
//	DSCAPS dscaps; 
//	dscaps.dwSize = sizeof(DSCAPS); 
//    m_pDS->GetCaps(&dscaps); 
//
//	if (dscaps.dwFlags & DSCAPS_EMULDRIVER) {
//		return;
//	}
//
//	// Get the primary buffer 
//    DSBUFFERDESC dsbd;
//    ZeroMemory( &dsbd, sizeof(DSBUFFERDESC) );
//    dsbd.dwSize        = sizeof(DSBUFFERDESC);
//    dsbd.dwFlags       = DSBCAPS_PRIMARYBUFFER;
//    dsbd.dwBufferBytes = 0;
//    dsbd.lpwfxFormat   = NULL;
//       
//	// Obtain write-primary cooperative level. 
//	if( FAILED( hr = m_pDS->SetCooperativeLevel(win32.hWnd, DSSCL_PRIORITY ) ) ) {
//        DXTRACE_ERR( TEXT("SetPrimaryBufferFormat"), hr );
//		return;
//	}
//
//	if( FAILED( hr = m_pDS->CreateSoundBuffer( &dsbd, &pDSBPrimary, NULL ) ) ) {
//		return;
//	}
//
//	if ( dwSpeakers == 6 && (cfgSpeakers == DSSPEAKER_5POINT1 || cfgSpeakers == DSSPEAKER_SURROUND) ) {
//		WAVEFORMATEXTENSIBLE 	waveFormatPCMEx;
//		ZeroMemory( &waveFormatPCMEx, sizeof(WAVEFORMATEXTENSIBLE) ); 
//
// 		waveFormatPCMEx.Format.wFormatTag = WAVE_FORMAT_EXTENSIBLE;
//		waveFormatPCMEx.Format.nChannels = 6;
//		waveFormatPCMEx.Format.nSamplesPerSec = dwPrimaryFreq;
//		waveFormatPCMEx.Format.wBitsPerSample  = (WORD) dwPrimaryBitRate; 
//		waveFormatPCMEx.Format.nBlockAlign = waveFormatPCMEx.Format.wBitsPerSample / 8 * waveFormatPCMEx.Format.nChannels;
//		waveFormatPCMEx.Format.nAvgBytesPerSec = waveFormatPCMEx.Format.nSamplesPerSec * waveFormatPCMEx.Format.nBlockAlign;
//		waveFormatPCMEx.dwChannelMask = KSAUDIO_SPEAKER_5POINT1;
//									 // SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT |
//									 // SPEAKER_FRONT_CENTER | SPEAKER_LOW_FREQUENCY |
//									 // SPEAKER_BACK_LEFT  | SPEAKER_BACK_RIGHT
//		waveFormatPCMEx.SubFormat =  KSDATAFORMAT_SUBTYPE_PCM;  // Specify PCM
//		waveFormatPCMEx.Format.cbSize = sizeof(WAVEFORMATEXTENSIBLE);
//		waveFormatPCMEx.Samples.wValidBitsPerSample = 16;
//
//		if( FAILED( hr = pDSBPrimary->SetFormat((WAVEFORMATEX*)&waveFormatPCMEx) ) ) {
//	        DXTRACE_ERR( TEXT("SetPrimaryBufferFormat"), hr );
//			return;
//		}
//		numSpeakers = 6;		// force it to think 5.1
//		blockAlign = waveFormatPCMEx.Format.nBlockAlign;
//	} else {
//		if (dwSpeakers == 6) {
//			common->Printf("sound: hardware reported unable to use multisound, defaulted to stereo\n");
//		}
//		WAVEFORMATEX wfx;
//		ZeroMemory( &wfx, sizeof(WAVEFORMATEX) ); 
//		wfx.wFormatTag      = WAVE_FORMAT_PCM; 
//		wfx.nChannels       = 2; 
//		wfx.nSamplesPerSec  = dwPrimaryFreq; 
//		wfx.wBitsPerSample  = (WORD) dwPrimaryBitRate; 
//		wfx.nBlockAlign     = wfx.wBitsPerSample / 8 * wfx.nChannels;
//		wfx.nAvgBytesPerSec = wfx.nSamplesPerSec * wfx.nBlockAlign;
//		wfx.cbSize = sizeof(WAVEFORMATEX);
//
//		if( FAILED( hr = pDSBPrimary->SetFormat(&wfx) ) ) {
//			return;
//		}
//		numSpeakers = 2;		// force it to think stereo
//		blockAlign = wfx.nBlockAlign;
//	}
//
//	byte *speakerData;
//	bufferSize = MIXBUFFER_SAMPLES * sizeof(word) * numSpeakers * ROOM_SLICES_IN_BUFFER;
//	speakerData = (byte *)Mem_Alloc( bufferSize );
//	memset( speakerData, 0, bufferSize );
//
//	InitializeSpeakers( speakerData, bufferSize, dwPrimaryFreq, dwPrimaryBitRate, numSpeakers );
        }
// public    int Create( idWaveFile* pWaveFile, idAudioBuffer** ppiab );
// public    int Create( idAudioBuffer** ppSound, const char* strWaveFileName, dword dwCreationFlags = 0 );
// public    int CreateFromMemory( idAudioBufferWIN32** ppSound, byte* pbData, ulong ulDataSize, waveformatextensible_t *pwfx, dword dwCreationFlags = 0 );

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
//            throw new TODO_Exception();
            SourceDataLine dataLine;//for streaming
            int hr;
//            AudioInputStream  audioInputStream = AudioSystem.getAudioInputStream(null);
//            dataLine.
//
//            bufferSize = 0;
//            numSpeakers = 0;
//            blockAlign = 0;
//
//            SAFE_RELEASE(m_pDS);
//
//            // Create IDirectSound using the primary sound device
//            if (FAILED(hr = DirectSoundCreate(NULL,  & m_pDS, null))) {
//                return false;
//            }

            // Set primary buffer format
            SetPrimaryBufferFormat(PRIMARYFREQ, 16, idSoundSystemLocal.s_numberOfSpeakers.GetInteger());
            return true;
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
                ALC.create();
            } catch (UnsatisfiedLinkError ex) {
                Logger.getLogger(win_snd.class.getName()).log(Level.SEVERE, null, ex);
                common.Warning("LoadLibrary %s failed.", idSoundSystemLocal.s_libOpenAL.GetString());
                return false;
            } catch (IllegalStateException ex) {
                return "ALC has already been created.".equals(ex.getMessage());
            }

            return true;
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
        ALC.destroy();
    }

}
