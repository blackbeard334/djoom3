package neo.openal;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public interface QALIfc {

    void alBufferData(int bufferName, int format, ByteBuffer data, int frequency);

    boolean alcCloseDevice(long deviceHandle);

    long alcCreateContext(long deviceHandle, IntBuffer attrList);

    void alcDestroyContext(long context);

    String alcGetString(long deviceHandle, int token);

    boolean alcMakeContextCurrent(long context);

    long alcOpenDevice(ByteBuffer deviceSpecifier);

    void alcProcessContext(long context);

    void alcSuspendContext(long context);

    void alDeleteBuffers(int bufferName);

    void alDeleteBuffers(IntBuffer bufferNames);

    void alDeleteSources(int source);

    void alDeleteSources(IntBuffer source);

    int alGenBuffers();

    void alGenBuffers(IntBuffer bufferNames);

    int alGenSources();

    int alGetError();

    int alGetSourcei(int source, int param);

    boolean alIsBuffer(int bufferName);

    boolean alIsExtensionPresent(ByteBuffer extName);

    boolean alIsExtensionPresent(CharSequence extName);

    boolean alIsSource(int sourceName);

    void alListener3f(int paramName, float value1, float value2, float value3);

    void alListenerf(int paramName, float value);

    void alListenerfv(int paramName, FloatBuffer values);

    void alSource3f(int source, int param, float v1, float v2, float v3);

    void alSourcef(int source, int param, float value);

    void alSourcei(int source, int param, int value);

    void alSourcePlay(int source);

    void alSourceQueueBuffers(int sourceName, int bufferName);

    void alSourceStop(int source);

    int alSourceUnqueueBuffers(int sourceName);

    void createCapabilities(long openalDevice);

    void loadOpenAL();

    void freeOpenAL();

}
