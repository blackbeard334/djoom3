package neo.Renderer;

import java.nio.ByteBuffer;

import static neo.idlib.Lib.idLib.common;
import org.lwjgl.BufferUtils;

/**
 *
 */
public class Image_process {

    private static final int MAX_DIMENSION = 4096;

    /*
     ================
     R_ResampleTexture

     Used to resample images in a more general than quartering fashion.

     This will only have filter coverage if the resampled size
     is greater than half the original size.

     If a larger shrinking is needed, use the mipmap function 
     after resampling to the next lower power of two.
     ================
     */
    static ByteBuffer R_ResampleTexture(final ByteBuffer in, int inwidth, int inheight, int outwidth, int outheight) {
        int i, j;
        ByteBuffer inrow, inrow2;
        /*unsigned*/ int frac, fracstep;
        /*unsigned*/ int[] p1 = new int[MAX_DIMENSION], p2 = new int[MAX_DIMENSION];
        ByteBuffer pix1, pix2, pix3, pix4;
        ByteBuffer out, out_p;

        if (outwidth > MAX_DIMENSION) {
            outwidth = MAX_DIMENSION;
        }
        if (outheight > MAX_DIMENSION) {
            outheight = MAX_DIMENSION;
        }

        out = ByteBuffer.allocate(outwidth * outheight * 4);//(byte *)R_StaticAlloc( outwidth * outheight * 4 );
        out_p = out;

        fracstep = inwidth * 0x10000 / outwidth;

        frac = fracstep >> 2;
        for (i = 0; i < outwidth; i++) {
            p1[i] = 4 * (frac >> 16);
            frac += fracstep;
        }
        frac = 3 * (fracstep >> 2);
        for (i = 0; i < outwidth; i++) {
            p2[i] = 4 * (frac >> 16);
            frac += fracstep;
        }

        for (i = 0; i < outheight; i++/*, out_p += outwidth*4*/) {
            inrow = in.duplicate();
            inrow.position(4 * inwidth * (int) ((i + 0.25f) * inheight / outheight));
            inrow2 = in.duplicate();//
            inrow2.position(4 * inwidth * (int) ((i + 0.75f) * inheight / outheight));
            frac = fracstep >> 1;
            for (j = 0; j < outwidth; j++) {
                pix1 = (ByteBuffer) inrow.duplicate().position(p1[j]);
                pix2 = (ByteBuffer) inrow.duplicate().position(p2[j]);
                pix3 = (ByteBuffer) inrow2.duplicate().position(p1[j]);
                pix4 = (ByteBuffer) inrow2.duplicate().position(p2[j]);
                out_p.put((byte) (addUnsignedBytes(pix1.get(), pix2.get(), pix3.get(), pix4.get()) >> 2));
                out_p.put((byte) (addUnsignedBytes(pix1.get(), pix2.get(), pix3.get(), pix4.get()) >> 2));
                out_p.put((byte) (addUnsignedBytes(pix1.get(), pix2.get(), pix3.get(), pix4.get()) >> 2));
                out_p.put((byte) (addUnsignedBytes(pix1.get(), pix2.get(), pix3.get(), pix4.get()) >> 2));
            }
        }

        return out;
    }

    /*
     ================
     R_Dropsample

     Used to resample images in a more general than quartering fashion.
     Normal maps and such should not be bilerped.
     ================
     */
    static byte[] R_Dropsample(final ByteBuffer in, int inwidth, int inheight, int outwidth, int outheight) {
        int i, j, k;
        int inrow;
        int pix1;
        byte[] out;
        int out_p;

        out = new byte[outwidth * outheight * 4];// R_StaticAlloc(outwidth * outheight * 4);
        out_p = 0;

        for (i = 0; i < outheight; i++, out_p += outwidth * 4) {
            inrow = /*in +*/ (4 * inwidth * (int) ((i + 0.25f) * inheight / outheight));
            for (j = 0; j < outwidth; j++) {
                k = j * inwidth / outwidth;
                pix1 = inrow + k * 4;
                out[out_p + j * 4 + 0] = in.get(pix1 + 0);
                out[out_p + j * 4 + 1] = in.get(pix1 + 1);
                out[out_p + j * 4 + 2] = in.get(pix1 + 2);
                out[out_p + j * 4 + 3] = in.get(pix1 + 3);
            }
        }

        return out;
    }

    /*
     ===============
     R_SetBorderTexels

     ===============
     */
    static void R_SetBorderTexels(ByteBuffer inBase, int width, int height, final byte[] border/*[4]*/) {
        int i;
        int out;

        out = 0;//inBase;
        for (i = 0; i < height; i++, out += width * 4) {
            inBase.put(out + 0, border[0]);
            inBase.put(out + 1, border[1]);
            inBase.put(out + 2, border[2]);
            inBase.put(out + 3, border[3]);
        }
        out = /*inBase+*/ (width - 1) * 4;
        for (i = 0; i < height; i++, out += width * 4) {
            inBase.put(out + 0, border[0]);
            inBase.put(out + 1, border[1]);
            inBase.put(out + 2, border[2]);
            inBase.put(out + 3, border[3]);
        }
        out = 0;//inBase;
        for (i = 0; i < width; i++, out += 4) {
            inBase.put(out + 0, border[0]);
            inBase.put(out + 1, border[1]);
            inBase.put(out + 2, border[2]);
            inBase.put(out + 3, border[3]);
        }
        out = /*inBase+*/ width * 4 * (height - 1);
        for (i = 0; i < width; i++, out += 4) {
            inBase.put(out + 0, border[0]);
            inBase.put(out + 1, border[1]);
            inBase.put(out + 2, border[2]);
            inBase.put(out + 3, border[3]);
        }
    }

    /*
     ===============
     R_SetBorderTexels3D

     ===============
     */
    static void R_SetBorderTexels3D(ByteBuffer inBase, int width, int height, int depth, final byte[] border/*[4]*/) {
        int i, j;
        int out;
        int row, plane;

        row = width * 4;
        plane = row * depth;

        for (j = 1; j < depth - 1; j++) {
            out = /*inBase +*/ j * plane;
            for (i = 0; i < height; i++, out += row) {
                inBase.put(out + 0, border[0]);
                inBase.put(out + 1, border[1]);
                inBase.put(out + 2, border[2]);
                inBase.put(out + 3, border[3]);
            }
            out = /*inBase+*/ (width - 1) * 4 + j * plane;
            for (i = 0; i < height; i++, out += row) {
                inBase.put(out + 0, border[0]);
                inBase.put(out + 1, border[1]);
                inBase.put(out + 2, border[2]);
                inBase.put(out + 3, border[3]);
            }
            out = /*inBase +*/ j * plane;
            for (i = 0; i < width; i++, out += 4) {
                inBase.put(out + 0, border[0]);
                inBase.put(out + 1, border[1]);
                inBase.put(out + 2, border[2]);
                inBase.put(out + 3, border[3]);
            }
            out = /*inBase+*/ width * 4 * (height - 1) + j * plane;
            for (i = 0; i < width; i++, out += 4) {
                inBase.put(out + 0, border[0]);
                inBase.put(out + 1, border[1]);
                inBase.put(out + 2, border[2]);
                inBase.put(out + 3, border[3]);
            }
        }

        out = 0;//inBase;
        for (i = 0; i < plane; i += 4, out += 4) {
            inBase.put(out + 0, border[0]);
            inBase.put(out + 1, border[1]);
            inBase.put(out + 2, border[2]);
            inBase.put(out + 3, border[3]);
        }
        out = /*inBase+*/ (depth - 1) * plane;
        for (i = 0; i < plane; i += 4, out += 4) {
            inBase.put(out + 0, border[0]);
            inBase.put(out + 1, border[1]);
            inBase.put(out + 2, border[2]);
            inBase.put(out + 3, border[3]);
        }
    }

    /*
     ================
     R_MipMap

     Returns a new copy of the texture, quartered in size and filtered.

     If a texture is intended to be used in GL_CLAMP or GL_CLAMP_TO_EDGE mode with
     a completely transparent border, we must prevent any blurring into the outer
     ring of texels by filling it with the border from the previous level.  This
     will result in a slight shrinking of the texture as it mips, but better than
     smeared clamps...
     ================
     */
    public static ByteBuffer R_MipMap(final ByteBuffer in, int width, int height, boolean preserveBorder) {
        int i, j;
        int in_p;
        ByteBuffer out;
        int out_p;
        int row;
        byte[] border = new byte[4];
        int newWidth, newHeight;

        if (width < 1 || height < 1 || (width + height == 2)) {
            common.FatalError("R_MipMap called with size %d,%d", width, height);
        }

        border[0] = in.get(0);
        border[1] = in.get(1);
        border[2] = in.get(2);
        border[3] = in.get(3);

        row = width * 4;

        newWidth = width >> 1;
        newHeight = height >> 1;
        if (0 == newWidth) {
            newWidth = 1;
        }
        if (0 == newHeight) {
            newHeight = 1;
        }
        out = BufferUtils.createByteBuffer(newWidth * newHeight * 4);// R_StaticAlloc(newWidth * newHeight * 4);
        out_p = 0;//out;

        in_p = 0;//in;

        width >>= 1;
        height >>= 1;

        if (width == 0 || height == 0) {
            width += height;	// get largest
            if (preserveBorder) {
                for (i = 0; i < width; i++, out_p += 4) {
                    out.put(out_p + 0, border[0]);
                    out.put(out_p + 1, border[1]);
                    out.put(out_p + 2, border[2]);
                    out.put(out_p + 3, border[3]);
                }
            } else {
                for (i = 0; i < width; i++, out_p += 4, in_p += 8) {
                    out.put(out_p + 0, (byte) (addUnsignedBytes(in.get(in_p + 0), in.get(in_p + 4)) >> 1));
                    out.put(out_p + 1, (byte) (addUnsignedBytes(in.get(in_p + 1), in.get(in_p + 5)) >> 1));
                    out.put(out_p + 2, (byte) (addUnsignedBytes(in.get(in_p + 2), in.get(in_p + 6)) >> 1));
                    out.put(out_p + 3, (byte) (addUnsignedBytes(in.get(in_p + 3), in.get(in_p + 7)) >> 1));
                }
            }
            return out;
        }

        for (i = 0; i < height; i++, in_p += row) {
            for (j = 0; j < width; j++, out_p += 4, in_p += 8) {
                out.put(out_p + 0, (byte) (addUnsignedBytes(in.get(in_p + 0), in.get(in_p + 4), in.get(in_p + row + 0), in.get(in_p + row + 4)) >> 2));
                out.put(out_p + 1, (byte) (addUnsignedBytes(in.get(in_p + 1), in.get(in_p + 5), in.get(in_p + row + 1), in.get(in_p + row + 5)) >> 2));
                out.put(out_p + 2, (byte) (addUnsignedBytes(in.get(in_p + 2), in.get(in_p + 6), in.get(in_p + row + 2), in.get(in_p + row + 6)) >> 2));
                out.put(out_p + 3, (byte) (addUnsignedBytes(in.get(in_p + 3), in.get(in_p + 7), in.get(in_p + row + 3), in.get(in_p + row + 7)) >> 2));
            }
        }

        // copy the old border texel back around if desired
        if (preserveBorder) {
            R_SetBorderTexels(out, width, height, border);
        }

        return out;
    }

    static int addUnsignedBytes(byte... bytes) {
        int result = 0;
        for (byte b : bytes) {
            result += b & 0xFF;
        }
        return result;
    }

    /*
     ================
     R_MipMap3D

     Returns a new copy of the texture, eigthed in size and filtered.

     If a texture is intended to be used in GL_CLAMP or GL_CLAMP_TO_EDGE mode with
     a completely transparent border, we must prevent any blurring into the outer
     ring of texels by filling it with the border from the previous level.  This
     will result in a slight shrinking of the texture as it mips, but better than
     smeared clamps...
     ================
     */
    static ByteBuffer R_MipMap3D(final ByteBuffer in, int width, int height, int depth, boolean preserveBorder) {
        int i, j, k;
        int in_p;
        ByteBuffer out;
        int out_p;
        int row, plane;
        final byte[] border = new byte[4];
        int newWidth, newHeight, newDepth;

        if (depth == 1) {
            return R_MipMap(in, width, height, preserveBorder);
        }

        // assume symetric for now
        if (width < 2 || height < 2 || depth < 2) {
            common.FatalError("R_MipMap3D called with size %d,%d,%d", width, height, depth);
        }

        border[0] = in.get(0);
        border[1] = in.get(1);
        border[2] = in.get(2);
        border[3] = in.get(3);

        row = width * 4;
        plane = row * height;

        newWidth = width >> 1;
        newHeight = height >> 1;
        newDepth = depth >> 1;

        out = ByteBuffer.allocate(newWidth * newHeight * newDepth * 4);// R_StaticAlloc(newWidth * newHeight * newDepth * 4);
        out_p = 0;//out;

        in_p = 0;//in;

        width >>= 1;
        height >>= 1;
        depth >>= 1;

        for (k = 0; k < depth; k++, in_p += plane) {
            for (i = 0; i < height; i++, in_p += row) {
                for (j = 0; j < width; j++, out_p += 4, in_p += 8) {
                    out.put(out_p + 0, (byte) (addUnsignedBytes(in.get(in_p + 0),
                            in.get(in_p + 4),
                            in.get(in_p + row + 0),
                            in.get(in_p + row + 4),
                            in.get(in_p + plane + 0),
                            in.get(in_p + plane + 4),
                            in.get(in_p + plane + row + 0),
                            in.get(in_p + plane + row + 4)) >> 3));
                    out.put(out_p + 1, (byte) (addUnsignedBytes(in.get(in_p + 1),
                            in.get(in_p + 5),
                            in.get(in_p + row + 1),
                            in.get(in_p + row + 5),
                            in.get(in_p + plane + 1),
                            in.get(in_p + plane + 5),
                            in.get(in_p + plane + row + 1),
                            in.get(in_p + plane + row + 5)) >> 3));
                    out.put(out_p + 2, (byte) (addUnsignedBytes(in.get(in_p + 2),
                            in.get(in_p + 6),
                            in.get(in_p + row + 2),
                            in.get(in_p + row + 6),
                            in.get(in_p + plane + 2),
                            in.get(in_p + plane + 6),
                            in.get(in_p + plane + row + 2),
                            in.get(in_p + plane + row + 6)) >> 3));
                    out.put(out_p + 3, (byte) (addUnsignedBytes(in.get(in_p + 3),
                            in.get(in_p + 7),
                            in.get(in_p + row + 3),
                            in.get(in_p + row + 7),
                            in.get(in_p + plane + 3),
                            in.get(in_p + plane + 6),
                            in.get(in_p + plane + row + 3),
                            in.get(in_p + plane + row + 6)) >> 3));
                }
            }
        }

        // copy the old border texel back around if desired
        if (preserveBorder) {
            R_SetBorderTexels3D(out, width, height, depth, border);
        }

        return out;
    }

    /*
     ==================
     R_BlendOverTexture

     Apply a color blend over a set of pixels
     ==================
     */
    static void R_BlendOverTexture(ByteBuffer data, int pixelCount, final int[] blend/*[4]*/) {
        int i;
        final int inverseAlpha;
        final int[] premult = new int[3];

        inverseAlpha = 255 - blend[3];
        premult[0] = blend[0] * blend[3];
        premult[1] = blend[1] * blend[3];
        premult[2] = blend[2] * blend[3];

        for (i = 0; i < pixelCount; i++/*, data+=4*/) {
            data.put(i * 4 + 0, (byte) ((data.get(i * 4 + 0) & 0xFF * inverseAlpha + premult[0]) >> 9));//TODO:signed byte arithmetic(overflow)
            data.put(i * 4 + 1, (byte) ((data.get(i * 4 + 1) & 0xFF * inverseAlpha + premult[1]) >> 9));
            data.put(i * 4 + 2, (byte) ((data.get(i * 4 + 2) & 0xFF * inverseAlpha + premult[2]) >> 9));
        }
    }

    /*
     ==================
     R_HorizontalFlip

     Flip the image in place
     ==================
     */
    static void R_HorizontalFlip(ByteBuffer data, int width, int height) {
        int i, j;
        int temp;

        for (i = 0; i < height; i++) {
            for (j = 0; j < width / 2; j++) {
                temp = data.getInt(i * width + j);
                data.putInt(i * width + j, data.getInt(i * width + width - 1 - j));
                data.putInt(i * width + width - 1 - j, temp);
            }
        }
    }

    public static void R_VerticalFlip(ByteBuffer data, int width, int height) {
        int i, j;
        int temp;

        for (i = 0; i < width; i++) {
            for (j = 0; j < height / 2; j++) {
                temp = data.getInt(j * width + i);
                final int index = (height - 1 - j) * width + i;
                data.putInt(j * width + i, data.getInt(index));
                data.putInt(index, temp);
            }
        }
    }

    static void R_RotatePic(ByteBuffer data, int width) {
        int i, j;
        ByteBuffer temp;

        temp = ByteBuffer.allocate(width * width * 4);// R_StaticAlloc(width * width * 4);

        for (i = 0; i < width; i++) {
            for (j = 0; j < width; j++) {
                temp.putInt(i * width + j, data.getInt(j * width + i));
            }
        }

//	memcpy( data, temp, width * width * 4 );
//        System.arraycopy(temp, 0, data, 0, width * width * 4);
        data.put(temp);

//        R_StaticFree(temp);
    }
}
