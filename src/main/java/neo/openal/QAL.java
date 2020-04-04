package neo.openal;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import neo.openal.lwjgl3.Lwjgl3QAL;

public class QAL implements QALConstantsIfc {

	// TODO: Change fix initialization to dynamic
	private static QALIfc impl = new Lwjgl3QAL();

	public static void alBufferData(int bufferName, int format, ByteBuffer data, int frequency) {
    	impl.alBufferData(bufferName, format, data, frequency);
    }

	public static boolean alcCloseDevice(long deviceHandle) {
    	return impl.alcCloseDevice(deviceHandle);
    }

	public static long alcCreateContext(long deviceHandle, IntBuffer attrList) {
    	return impl.alcCreateContext(deviceHandle, attrList);
    }

	public static void alcDestroyContext(long context) {
    	impl.alcDestroyContext(context);
    }

	public static String alcGetString(long deviceHandle, int token) {
    	return impl.alcGetString(deviceHandle, token);
    }

	public static boolean alcMakeContextCurrent(long context) {
    	return impl.alcMakeContextCurrent(context);
    }

	public static long alcOpenDevice(ByteBuffer deviceSpecifier) {
    	return impl.alcOpenDevice(deviceSpecifier);
    }

	public static void alcProcessContext(long context) {
    	impl.alcProcessContext(context);
    }

	public static void alcSuspendContext(long context) {
    	impl.alcSuspendContext(context);
    }

	public static void alDeleteBuffers(int bufferName) {
		impl.alDeleteBuffers(bufferName);
	}

	public static void alDeleteBuffers(IntBuffer bufferNames) {
    	impl.alDeleteBuffers(bufferNames);
    }

	public static void alDeleteSources(int source) {
		impl.alDeleteSources(source);
	}

	public static void alDeleteSources(IntBuffer source) {
		impl.alDeleteSources(source);
	}

    public static int alGenBuffers() {
		return impl.alGenBuffers();
	}

    public static void alGenBuffers(IntBuffer bufferNames) {
		impl.alGenBuffers(bufferNames);
    }

    public static int alGenSources() {
		return impl.alGenSources();
	}

    public static int alGetError() {
		return impl.alGetError();
	}

    public static int alGetSourcei(int source, int param) {
		return impl.alGetSourcei(source, param);
	}

    public static boolean alIsBuffer(int bufferName) {
    	return impl.alIsBuffer(bufferName);
    }

    public static boolean alIsExtensionPresent(ByteBuffer extName) {
		return impl.alIsExtensionPresent(extName);
	}

    public static boolean alIsExtensionPresent(CharSequence extName) {
		return impl.alIsExtensionPresent(extName);
	}

    public static boolean alIsSource(int sourceName) {
		return impl.alIsSource(sourceName);
	}

    public static void alListener3f(int paramName, float value1, float value2, float value3) {
    	impl.alListener3f(paramName, value1, value2, value3);
    }

    public static void alListenerf(int paramName, float value) {
    	impl.alListenerf(paramName, value);
    }

    public static void alListenerfv(int paramName, FloatBuffer values) {
    	impl.alListenerfv(paramName, values);
    }

    public static void alSource3f(int source, int param, float v1, float v2, float v3) {
    	impl.alSource3f(source, param, v1, v2, v3);
    }

    public static void alSourcef(int source, int param, float value) {
		impl.alSourcef(source, param, value);
	}

    public static void alSourcei(int source, int param, int value) {
		impl.alSourcei(source, param, value);
	}

    public static void alSourcePlay(int source) {
    	impl.alSourcePlay(source);
    }

    public static void alSourceQueueBuffers(int sourceName, int bufferName) {
    	impl.alSourceQueueBuffers(sourceName, bufferName);
    }

    public static void alSourceStop(int source) {
		impl.alSourceStop(source);
	}

    public static int alSourceUnqueueBuffers(int sourceName) {
    	return impl.alSourceUnqueueBuffers(sourceName);
    }

    public static void createCapabilities(long openalDevice) {
    	impl.createCapabilities(openalDevice);
    }

    public static void freeOpenAL() {
    	impl.freeOpenAL();
    }

   public static void loadOpenAL() {
    	impl.loadOpenAL();
    }

}
