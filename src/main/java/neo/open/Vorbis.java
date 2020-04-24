package neo.open;

import static org.lwjgl.stb.STBVorbis.VORBIS__no_error;
import static org.lwjgl.stb.STBVorbis.VORBIS_bad_packet_type;
import static org.lwjgl.stb.STBVorbis.VORBIS_cant_find_last_page;
import static org.lwjgl.stb.STBVorbis.VORBIS_continued_packet_flag_invalid;
import static org.lwjgl.stb.STBVorbis.VORBIS_feature_not_supported;
import static org.lwjgl.stb.STBVorbis.VORBIS_file_open_failure;
import static org.lwjgl.stb.STBVorbis.VORBIS_incorrect_stream_serial_number;
import static org.lwjgl.stb.STBVorbis.VORBIS_invalid_api_mixing;
import static org.lwjgl.stb.STBVorbis.VORBIS_invalid_first_page;
import static org.lwjgl.stb.STBVorbis.VORBIS_invalid_setup;
import static org.lwjgl.stb.STBVorbis.VORBIS_invalid_stream;
import static org.lwjgl.stb.STBVorbis.VORBIS_invalid_stream_structure_version;
import static org.lwjgl.stb.STBVorbis.VORBIS_missing_capture_pattern;
import static org.lwjgl.stb.STBVorbis.VORBIS_need_more_data;
import static org.lwjgl.stb.STBVorbis.VORBIS_ogg_skeleton_not_supported;
import static org.lwjgl.stb.STBVorbis.VORBIS_outofmem;
import static org.lwjgl.stb.STBVorbis.VORBIS_seek_failed;
import static org.lwjgl.stb.STBVorbis.VORBIS_seek_invalid;
import static org.lwjgl.stb.STBVorbis.VORBIS_seek_without_length;
import static org.lwjgl.stb.STBVorbis.VORBIS_too_many_channels;
import static org.lwjgl.stb.STBVorbis.VORBIS_unexpected_eof;

import java.nio.ByteBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;

public class Vorbis {

	public final int channels; // number of channels (i.e. mono, stereo...)
	public final int sampleRate; // sample rate
	public final long mdwSize; // size in samples

	private Vorbis(int nChannels, int nSamplesPerSec, long mdwSize) {
		super();
		this.channels = nChannels;
		this.sampleRate = nSamplesPerSec;
		this.mdwSize = mdwSize;
	}

	public static long openMemory(ByteBuffer mem, int[] error) {
		return STBVorbis.stb_vorbis_open_memory(mem, error, null);
	}

	public static boolean seek(long f, int sample_number) {
		return STBVorbis.stb_vorbis_seek(f, sample_number);
	}

	public static Vorbis getInfo(ByteBuffer mem, int[] error) {
		STBVorbisInfo vi = null;
		final long ov = Vorbis.openMemory(mem, error);
		if (error[0] != 0) {
			return null;
		}
		vi = STBVorbis.stb_vorbis_get_info(ov, STBVorbisInfo.create());

		return  new Vorbis(vi.channels(), vi.sample_rate(),
				STBVorbis.stb_vorbis_stream_length_in_samples(ov) * vi.channels() // pcm samples * num
		);
	}

	public static int getSample(int nChannels, int totalSamples, Long ogg) {
		final PointerBuffer samples = PointerBuffer.allocateDirect(nChannels);
		final int num_samples = totalSamples / nChannels;
		for (int i = 0; i < nChannels; i++) {
			samples.put(i, Nio.newFloatBuffer(num_samples));
		}
		int ret = STBVorbis.stb_vorbis_get_samples_float(ogg, samples, num_samples);
		if (ret == 0) {
			return ret;
		}
		if (ret < 0) {
			return ret;
		}

		ret *= nChannels;

		final float[][] samplesArray = new float[nChannels][num_samples];
		for (int i = 0; i < nChannels; i++) {
			samples.getFloatBuffer(i, num_samples).get(samplesArray[i]);
		}
		return ret;
	}

	public static String getErrorMessage(final int errorCode) {
		switch (errorCode) {
		case VORBIS__no_error:
			return "VORBIS__no_error";
		case VORBIS_need_more_data:
			return "VORBIS_need_more_data";
		case VORBIS_invalid_api_mixing:
			return "VORBIS_invalid_api_mixing";
		case VORBIS_outofmem:
			return "VORBIS_outofmem";
		case VORBIS_feature_not_supported:
			return "VORBIS_feature_not_supported";
		case VORBIS_too_many_channels:
			return "VORBIS_too_many_channels";
		case VORBIS_file_open_failure:
			return "VORBIS_file_open_failure";
		case VORBIS_seek_without_length:
			return "VORBIS_seek_without_length";
		case VORBIS_unexpected_eof:
			return "VORBIS_unexpected_eof";
		case VORBIS_seek_invalid:
			return "VORBIS_seek_invalid";
		case VORBIS_invalid_setup:
			return "VORBIS_invalid_setup";
		case VORBIS_invalid_stream:
			return "VORBIS_invalid_stream";
		case VORBIS_missing_capture_pattern:
			return "VORBIS_missing_capture_pattern";
		case VORBIS_invalid_stream_structure_version:
			return "VORBIS_invalid_stream_structure_version";
		case VORBIS_continued_packet_flag_invalid:
			return "VORBIS_continued_packet_flag_invalid";
		case VORBIS_incorrect_stream_serial_number:
			return "VORBIS_incorrect_stream_serial_number";
		case VORBIS_invalid_first_page:
			return "VORBIS_invalid_first_page";
		case VORBIS_bad_packet_type:
			return "VORBIS_bad_packet_type";
		case VORBIS_cant_find_last_page:
			return "VORBIS_cant_find_last_page";
		case VORBIS_seek_failed:
			return "VORBIS_seek_failed";
		case VORBIS_ogg_skeleton_not_supported:
			return "VORBIS_ogg_skeleton_not_supported";
		}
		return "Unknown error";
	}

}
