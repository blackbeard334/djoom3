package neo.idlib.hashing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import neo.idlib.Lib.idException;

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
        final ByteBuffer buffer = ByteBuffer.allocate(data.length * 4);
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
        int hash = 0;

        try {
            final int currentPosition = data.position();
            // The type sun.security.provider.MD4 is not accessible in JDK 11, replaced with neo.open.MD4
            final MessageDigest messageDigest = MD4 ? neo.open.MD4.getInstance() : MessageDigest.getInstance("MD5");

            messageDigest.update(data);

            data.position(currentPosition);

            final ByteBuffer digest = ByteBuffer.wrap(messageDigest.digest());
            digest.order(ByteOrder.LITTLE_ENDIAN);
            hash = digest.getInt() ^ digest.getInt() ^ digest.getInt() ^ digest.getInt();

        } catch (final NoSuchAlgorithmException ex) {
            throw new idException(ex);
        }

        return Integer.toUnsignedString(hash);
    }
}
