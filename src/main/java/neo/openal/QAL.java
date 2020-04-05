package neo.openal;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

public class QAL implements QALConstantsIfc {

	public static void alBufferData(int bufferName, int format, ByteBuffer data, int frequency) {
    	AL10.alBufferData(bufferName, format, data, frequency);
    }

	public static boolean alcCloseDevice(long deviceHandle) {
    	return ALC10.alcCloseDevice(deviceHandle);
    }

	public static long alcCreateContext(long deviceHandle, IntBuffer attrList) {
    	return ALC10.alcCreateContext(deviceHandle, attrList);
    }

	public static void alcDestroyContext(long context) {
    	ALC10.alcDestroyContext(context);
    }

	public static String alcGetString(long deviceHandle, int token) {
    	return ALC10.alcGetString(deviceHandle, token);
    }

	public static boolean alcMakeContextCurrent(long context) {
    	return ALC10.alcMakeContextCurrent(context);
    }

	public static long alcOpenDevice(ByteBuffer deviceSpecifier) {
    	return ALC10.alcOpenDevice(deviceSpecifier);
    }

	public static void alcProcessContext(long context) {
    	ALC10.alcProcessContext(context);
    }

	public static void alcSuspendContext(long context) {
    	ALC10.alcSuspendContext(context);
    }

	public static void alDeleteBuffers(int bufferName) {
		AL10.alDeleteBuffers(bufferName);
	}

	public static void alDeleteBuffers(IntBuffer bufferNames) {
    	AL10.alDeleteBuffers(bufferNames);
    }

	public static void alDeleteSources(int source) {
		AL10.alDeleteSources(source);
	}

	public static void alDeleteSources(IntBuffer source) {
		AL10.alDeleteSources(source);
	}

    public static int alGenBuffers() {
		return AL10.alGenBuffers();
	}

    public static void alGenBuffers(IntBuffer bufferNames) {
		AL10.alGenBuffers(bufferNames);
    }

    public static int alGenSources() {
		return AL10.alGenSources();
	}

    public static int alGetError() {
		return AL10.alGetError();
	}

    public static int alGetSourcei(int source, int param) {
		return AL10.alGetSourcei(source, param);
	}

    public static boolean alIsBuffer(int bufferName) {
    	return AL10.alIsBuffer(bufferName);
    }

    public static boolean alIsExtensionPresent(ByteBuffer extName) {
		return AL10.alIsExtensionPresent(extName);
	}

    public static boolean alIsExtensionPresent(CharSequence extName) {
		return AL10.alIsExtensionPresent(extName);
	}

    public static boolean alIsSource(int sourceName) {
		return AL10.alIsSource(sourceName);
	}

    public static void alListener3f(int paramName, float value1, float value2, float value3) {
    	AL10.alListener3f(paramName, value1, value2, value3);
    }

    public static void alListenerf(int paramName, float value) {
    	AL10.alListenerf(paramName, value);
    }

    public static void alListenerfv(int paramName, FloatBuffer values) {
    	AL10.alListenerfv(paramName, values);
    }

    public static void alSource3f(int source, int param, float v1, float v2, float v3) {
    	AL10.alSource3f(source, param, v1, v2, v3);
    }

    public static void alSourcef(int source, int param, float value) {
		AL10.alSourcef(source, param, value);
	}

    public static void alSourcei(int source, int param, int value) {
		AL10.alSourcei(source, param, value);
	}

    public static void alSourcePlay(int source) {
    	AL10.alSourcePlay(source);
    }

    public static void alSourceQueueBuffers(int sourceName, int bufferName) {
    	AL10.alSourceQueueBuffers(sourceName, bufferName);
    }

    public static void alSourceStop(int source) {
		AL10.alSourceStop(source);
	}

    public static int alSourceUnqueueBuffers(int sourceName) {
    	return AL10.alSourceUnqueueBuffers(sourceName);
    }

    public static void createCapabilities(long openalDevice) {
        final ALCCapabilities alcCapabilities = ALC.createCapabilities(openalDevice);
        final ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
	}

    public static void freeOpenAL() {
		ALC.destroy();
	}

    public static void loadOpenAL() {
		ALC.create();
	}

}
