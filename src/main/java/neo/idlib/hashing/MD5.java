package neo.idlib.hashing;

import java.nio.ByteBuffer;
import static neo.idlib.hashing.MD4.BlockChecksum;

/**
 *
 */
public class MD5 {

    private static final boolean MD5 = false;

    /*
     ===============
     MD5_BlockChecksum
     ===============
     */
    public static String MD5_BlockChecksum(final byte[] data, int length) {
        return BlockChecksum(ByteBuffer.wrap(data), length, MD5);
    }

    public static String MD5_BlockChecksum(final String data, int length) {
        return MD5_BlockChecksum(data.getBytes(), length);
    }

}
