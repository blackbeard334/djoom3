package neo.openal.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;

import neo.openal.QALIfc;

public class Lwjgl3QAL implements QALIfc {

	public void alBufferData(int bufferName, int format, ByteBuffer data, int frequency) {
    	AL10.alBufferData(bufferName, format, data, frequency);
    }

	public boolean alcCloseDevice(long deviceHandle) {
    	return ALC10.alcCloseDevice(deviceHandle);
    }

	public long alcCreateContext(long deviceHandle, IntBuffer attrList) {
    	return ALC10.alcCreateContext(deviceHandle, attrList);
    }

	public void alcDestroyContext(long context) {
    	ALC10.alcDestroyContext(context);
    }

	public String alcGetString(long deviceHandle, int token) {
    	return ALC10.alcGetString(deviceHandle, token);
    }

	public boolean alcMakeContextCurrent(long context) {
    	return ALC10.alcMakeContextCurrent(context);
    }

	public long alcOpenDevice(ByteBuffer deviceSpecifier) {
    	return ALC10.alcOpenDevice(deviceSpecifier);
    }

	public void alcProcessContext(long context) {
    	ALC10.alcProcessContext(context);
    }

	public void alcSuspendContext(long context) {
    	ALC10.alcSuspendContext(context);
    }

	public void alDeleteBuffers(int bufferName) {
		AL10.alDeleteBuffers(bufferName);
	}

	public void alDeleteBuffers(IntBuffer bufferNames) {
    	AL10.alDeleteBuffers(bufferNames);
    }

	public void alDeleteSources(int source) {
		AL10.alDeleteSources(source);
	}

	public void alDeleteSources(IntBuffer source) {
		AL10.alDeleteSources(source);
	}

    public int alGenBuffers() {
		return AL10.alGenBuffers();
	}

    public void alGenBuffers(IntBuffer bufferNames) {
		AL10.alGenBuffers(bufferNames);
    }

    public int alGenSources() {
		return AL10.alGenSources();
	}

    public int alGetError() {
		return AL10.alGetError();
	}

    public int alGetSourcei(int source, int param) {
		return AL10.alGetSourcei(source, param);
	}

    public boolean alIsBuffer(int bufferName) {
    	return AL10.alIsBuffer(bufferName);
    }

    public boolean alIsExtensionPresent(ByteBuffer extName) {
		return AL10.alIsExtensionPresent(extName);
	}

    public boolean alIsExtensionPresent(CharSequence extName) {
		return AL10.alIsExtensionPresent(extName);
	}

    public boolean alIsSource(int sourceName) {
		return AL10.alIsSource(sourceName);
	}

    public void alListener3f(int paramName, float value1, float value2, float value3) {
    	AL10.alListener3f(paramName, value1, value2, value3);
    }

    public void alListenerf(int paramName, float value) {
    	AL10.alListenerf(paramName, value);
    }

    public void alListenerfv(int paramName, FloatBuffer values) {
    	AL10.alListenerfv(paramName, values);
    }

    public void alSource3f(int source, int param, float v1, float v2, float v3) {
    	AL10.alSource3f(source, param, v1, v2, v3);
    }

    public void alSourcef(int source, int param, float value) {
		AL10.alSourcef(source, param, value);
	}

    public void alSourcei(int source, int param, int value) {
		AL10.alSourcei(source, param, value);
	}

    public void alSourcePlay(int source) {
    	AL10.alSourcePlay(source);
    }

    public void alSourceQueueBuffers(int sourceName, int bufferName) {
    	AL10.alSourceQueueBuffers(sourceName, bufferName);
    }

    public void alSourceStop(int source) {
		AL10.alSourceStop(source);
	}

    public int alSourceUnqueueBuffers(int sourceName) {
    	return AL10.alSourceUnqueueBuffers(sourceName);
    }

}
