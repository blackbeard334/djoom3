package neo.idlib.hashing;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class MD4 {

    private static final boolean MD4 = true;

    /*
     ===============================================================================

     Calculates a checksum for a block of data
     using the MD4 message-digest algorithm.

     ===============================================================================
     */
    public static String MD4_BlockChecksum(final ByteBuffer data, int length) {

        return BlockChecksum(data, length, MD4);
    }

    public static String MD4_BlockChecksum(final int[] data, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length * 4);
        buffer.asIntBuffer().put(data);

        return BlockChecksum(buffer, length, MD4);
    }

    public static String MD4_BlockChecksum(final byte[] data, int length) {
//        final StringBuffer hash = new StringBuffer(16);
//        final MessageDigest messageDigest = sun.security.provider.MD4.getInstance();
//
////        if (data instanceof byte[]) {
//        messageDigest.update(data);
//        for (byte b : messageDigest.digest()) {
//            hash.append(String.format("%02d", 0xff & b));
//        }

//        } else if (data instanceof short[]) {
//        } else if (data instanceof char[]) {
//        } else if (data instanceof int[]) {
//        } else if (data instanceof long[]) {
//        }
        return BlockChecksum(ByteBuffer.wrap(data), length, MD4);//TODO: make sure checksums match the original c++ rsa version.
    }

    static String BlockChecksum(final ByteBuffer data, final int length, final boolean MD4) {
        final StringBuffer hash = new StringBuffer(16);

        try {
            final int currentPosition = data.position();
            MessageDigest messageDigest = MD4 ? sun.security.provider.MD4.getInstance() : MessageDigest.getInstance("MD5");

            messageDigest.update(data);

            data.position(currentPosition);

            for (byte b : messageDigest.digest()) {
                hash.append(String.format("%02d", 0xff & b));
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(MD4.class.getName()).log(Level.SEVERE, null, ex);
        }

        return hash.toString();
    }
}
