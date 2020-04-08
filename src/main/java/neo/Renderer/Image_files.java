package neo.Renderer;

import static neo.Renderer.Image.MAX_IMAGE_NAME;
import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Image.cubeFiles_t.CF_CAMERA;
import static neo.Renderer.Image_process.R_HorizontalFlip;
import static neo.Renderer.Image_process.R_ResampleTexture;
import static neo.Renderer.Image_process.R_RotatePic;
import static neo.Renderer.Image_process.R_VerticalFlip;
import static neo.Renderer.Image_program.R_LoadImageProgram;
import static neo.TempDump.NOT;
import static neo.TempDump.ctos;
import static neo.framework.FileSystem_h.FILE_NOT_FOUND_TIMESTAMP;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.idlib.Lib.LittleLong;
import static neo.idlib.Lib.LittleShort;
import static neo.idlib.Lib.idLib.common;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import neo.TempDump.TODO_Exception;
import neo.Renderer.Image.cubeFiles_t;
import neo.framework.File_h.idFile;
import neo.idlib.Text.Str.idStr;
import neo.open.Nio;

/**
 *
 */
public class Image_files {

    /*
     ========================================================================

     PCX files are used for 8 bit images

     ========================================================================
     */
    private static class pcx_t {

        char manufacturer;
        char version;
        char encoding;
        char bits_per_pixel;
        /*unsigned*/ short xmin, ymin, xmax, ymax;
        /*unsigned*/ short hres, vres;
        /*unsigned*/ char[] palette = new char[48];
        char reserved;
        char color_planes;
        /*unsigned*/ short bytes_per_line;
        /*unsigned*/ short palette_type;
        char[] filler = new char[58];
//        unsigned char data;			// unbounded
        int dataPosition;			// unbounded

        private pcx_t(ByteBuffer byteBuffer) {
            throw new TODO_Exception();
        }
    }


    /*
     ========================================================================

     TGA files are used for 24/32 bit images

     ========================================================================
     */
    private static class TargaHeader {
        /*unsigned*/ byte id_length, colormap_type, image_type;
        /*unsigned*/ short colormap_index, colormap_length;
        /*unsigned*/ byte colormap_size;
        /*unsigned*/ short x_origin, y_origin, width, height;
        /*unsigned*/ byte pixel_size, attributes;
    }

    private static class BMPHeader_t {

        char[] id = new char[2];
        /*unsigned*/ long fileSize;
        /*unsigned*/ long reserved0;
        /*unsigned*/ long bitmapDataOffset;
        /*unsigned*/ long bitmapHeaderSize;
        /*unsigned*/ long width;
        /*unsigned*/ long height;
        /*unsigned*/ short planes;
        /*unsigned*/ short bitsPerPixel;
        /*unsigned*/ long compression;
        /*unsigned*/ long bitmapDataSize;
        /*unsigned*/ long hRes;
        /*unsigned*/ long vRes;
        /*unsigned*/ long colors;
        /*unsigned*/ long importantColors;
        /*unsigned*/ char[][] palette = new char[256][4];
    }

    /*
     ================
     R_WritePalTGA
     ================
     */
    // data is an 8 bit index into palette, which is RGB (no A)
    static void R_WritePalTGA(final String filename, final ByteBuffer data, final byte[] palette, int width, int height, boolean flipVertical) {
        throw new TODO_Exception();
//	byte	*buffer;
//	int		i;
//	int		bufferSize = (width * height) + (256 * 3) + 18;
//	int     palStart = 18;
//	int     imgStart = 18 + (256 * 3);
//
//	buffer = (byte *)Mem_Alloc( bufferSize );
//	memset( buffer, 0, 18 );
//	buffer[1] = 1;		// color map type
//	buffer[2] = 1;		// uncompressed color mapped image
//	buffer[5] = 0;		// number of palette entries (lo)
//	buffer[6] = 1;		// number of palette entries (hi)
//	buffer[7] = 24;		// color map bpp
//	buffer[12] = width&255;
//	buffer[13] = width>>8;
//	buffer[14] = height&255;
//	buffer[15] = height>>8;
//	buffer[16] = 8;	// pixel size
//	if ( !flipVertical ) {
//		buffer[17] = (1<<5);	// flip bit, for normal top to bottom raster order
//	}
//
//	// store palette, swapping rgb to bgr
//	for ( i=palStart ; i<imgStart ; i+=3 ) {
//		buffer[i] = palette[i-palStart+2];		// blue
//		buffer[i+1] = palette[i-palStart+1];		// green
//		buffer[i+2] = palette[i-palStart+0];		// red
//	}
//
//	// store the image data
//	for ( i=imgStart ; i<bufferSize ; i++ ) {
//		buffer[i] = data[i-imgStart];
//	}
//
//	fileSystem->WriteFile( filename, buffer, bufferSize );
//
//	Mem_Free (buffer);
    }

    static void R_WritePalTGA(final String filename, final ByteBuffer data, final byte[] palette, int width, int height) {
        R_WritePalTGA(filename, data, palette, width, height, false);
    }

    /*
     ================
     R_WriteTGA
     ================
     */
    public static void R_WriteTGA(final String filename, final ByteBuffer data, int width, int height, boolean flipVertical) {
        ByteBuffer buffer;
        int i;
        final int bufferSize = (width * height * 4) + 18;
        final int imgStart = 18;

        buffer = ByteBuffer.allocate(bufferSize);// Mem_Alloc(bufferSize);
//	memset( buffer, 0, 18 );
        buffer.put(2, (byte) 2);		// uncompressed type
        buffer.put(12, (byte) (width & 255));
        buffer.put(13, (byte) (width >> 8));
        buffer.put(14, (byte) (height & 255));
        buffer.put(15, (byte) (height >> 8));
        buffer.put(16, (byte) 32);	// pixel size
        if (!flipVertical) {
            buffer.put(17, (byte) (1 << 5));	// flip bit, for normal top to bottom raster order
        }

        // swap rgb to bgr
        for (i = imgStart; i < bufferSize; i += 4) {
            buffer.put(i, data.get((i - imgStart) + 2));		// blue
            buffer.put(i + 1, data.get((i - imgStart) + 1));		// green
            buffer.put(i + 2, data.get((i - imgStart) + 0));		// red
            buffer.put(i + 3, data.get((i - imgStart) + 3));		// alpha
        }

        fileSystem.WriteFile(filename, buffer, bufferSize);

//        Mem_Free(buffer);
    }

    public static void R_WriteTGA(final String filename, final ByteBuffer data, int width, int height) {
        R_WriteTGA(filename, data, width, height, false);
    }

    public static void R_WriteTGA(final idStr filename, final ByteBuffer data, int width, int height) {
        R_WriteTGA(filename.getData(), data, width, height);
    }

    /*
     ========================================================================================================

     BMP LOADING

     ========================================================================================================
     */
    /*
     ==============
     LoadBMP
     ==============
     */
    static ByteBuffer LoadBMP(final String name, int[] width, int[] height, long[] timestamp) {
        int columns, rows, numPixels;
        ByteBuffer pixbuf;
        int row, column;
        ByteBuffer buf_p;
        final ByteBuffer[] buffer = {null};
        int length;
        final BMPHeader_t bmpHeader = new BMPHeader_t();
        ByteBuffer bmpRGBA;

        if (NOT(width, height)) {
            fileSystem.ReadFile(name, null, timestamp);
            return null;	// just getting timestamp
        }

        //
        // load the file
        //
        length = fileSystem.ReadFile(name, buffer, timestamp);
        if (NOT(buffer)) {
            return null;
        }

        buf_p = buffer[0].duplicate();

        bmpHeader.id[0] = (char) buf_p.get();//*buf_p++;
        bmpHeader.id[1] = (char) buf_p.get();//*buf_p++;
        bmpHeader.fileSize = LittleLong( /* ( long * )*/buf_p.getLong());//	buf_p += 4;
        bmpHeader.reserved0 = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.bitmapDataOffset = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.bitmapHeaderSize = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.width = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.height = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.planes = LittleShort(buf_p.getShort());//	buf_p += 2;
        bmpHeader.bitsPerPixel = LittleShort(buf_p.getShort());//	buf_p += 2;
        bmpHeader.compression = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.bitmapDataSize = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.hRes = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.vRes = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.colors = LittleLong(buf_p.getLong());//	buf_p += 4;
        bmpHeader.importantColors = LittleLong(buf_p.getLong());//	buf_p += 4;
        for (final char[] palette : bmpHeader.palette) {
            for (int a = 0; a < bmpHeader.palette[0].length; a++) {
//	memcpy( bmpHeader.palette, buf_p, sizeof( bmpHeader.palette ) );
                palette[a] = buf_p.getChar();//TODO:should this be getByte()????
            }
        }

        if (bmpHeader.bitsPerPixel == 8) {
            buf_p.position(buf_p.position() + 1024);
        }

        if ((bmpHeader.id[0] != 'B') && (bmpHeader.id[1] != 'M')) {
            common.Error("LoadBMP: only Windows-style BMP files supported (%s)\n", name);
        }
        if (bmpHeader.fileSize != length) {
            common.Error("LoadBMP: header size does not match file size (%lu vs. %d) (%s)\n", bmpHeader.fileSize, length, name);
        }
        if (bmpHeader.compression != 0) {
            common.Error("LoadBMP: only uncompressed BMP files supported (%s)\n", name);
        }
        if (bmpHeader.bitsPerPixel < 8) {
            common.Error("LoadBMP: monochrome and 4-bit BMP files not supported (%s)\n", name);
        }

        columns = (int) bmpHeader.width;
        rows = (int) bmpHeader.height;
        if (rows < 0) {
            rows = -rows;
        }
        numPixels = columns * rows;

        if (width != null) {
            width[0] = columns;
        }
        if (height != null) {
            height[0] = rows;
        }

        bmpRGBA = Nio.newByteBuffer(numPixels * 4);//byte *)R_StaticAlloc( numPixels * 4 );

        for (row = rows - 1; row >= 0; row--) {
            pixbuf = bmpRGBA.duplicate();
            pixbuf.position(row * columns * 4);

            for (column = 0; column < columns; column++) {
                /*unsigned*/ byte red, green, blue, alpha;
                int palIndex;
                /*unsigned*/ short shortPixel;

                switch (bmpHeader.bitsPerPixel) {
                    case 8:
                        palIndex = buf_p.get();
                        pixbuf.put((byte) bmpHeader.palette[palIndex][2]);
                        pixbuf.put((byte) bmpHeader.palette[palIndex][1]);
                        pixbuf.put((byte) bmpHeader.palette[palIndex][0]);
                        pixbuf.put((byte) 0xff);
                        break;
                    case 16:
                        shortPixel = pixbuf.getShort();
                        pixbuf.getShort();// += 2;
                        pixbuf.put((byte) ((shortPixel & (31 << 10)) >> 7));
                        pixbuf.put((byte) ((shortPixel & (31 << 5)) >> 2));
                        pixbuf.put((byte) ((shortPixel & (31)) << 3));
                        pixbuf.put((byte) 0xff);
                        break;

                    case 24:
                        blue = buf_p.get();
                        green = buf_p.get();
                        red = buf_p.get();
                        pixbuf.put(red);
                        pixbuf.put(green);
                        pixbuf.put(blue);
                        pixbuf.put((byte) 255);
                        break;
                    case 32:
                        blue = buf_p.get();
                        green = buf_p.get();
                        red = buf_p.get();
                        alpha = buf_p.get();
                        pixbuf.put(red);
                        pixbuf.put(green);
                        pixbuf.put(blue);
                        pixbuf.put(alpha);
                        break;
                    default:
                        common.Error("LoadBMP: illegal pixel_size '%d' in file '%s'\n", bmpHeader.bitsPerPixel, name);
                        break;
                }
            }
        }
        return  bmpRGBA;
//	fileSystem->FreeFile( buffer );
    }

    /*
     ========================================================================================================

     PCX LOADING

     ========================================================================================================
     */
    /*
     ==============
     LoadPCX
     ==============
     */
    private static void LoadPCX(final String filename, ByteBuffer[] pic, ByteBuffer[] palette, int[] width, int[] height, long[] timestamp) {
        final ByteBuffer[] raw = {null};
        pcx_t pcx;
        int x, y;
        int len;
        int runLength;
        byte dataByte;
        ByteBuffer out, pix;
        int xmax, ymax;

        if (NOT(pic)) {
            fileSystem.ReadFile(filename, null, timestamp);
            return;	// just getting timestamp
        }

        pic[0] = null;
        palette[0] = null;

        //
        // load the file
        //
        len = fileSystem.ReadFile(filename, raw, timestamp);
        if (NOT(raw)) {
            return;
        }

        //
        // parse the PCX file
        //
        pcx = new pcx_t(raw[0]);
        raw[0].position(pcx.dataPosition);

        xmax = LittleShort(pcx.xmax);
        ymax = LittleShort(pcx.ymax);

        if ((pcx.manufacturer != 0x0a)
                || (pcx.version != 5)
                || (pcx.encoding != 1)
                || (pcx.bits_per_pixel != 8)
                || (xmax >= 1024)
                || (ymax >= 1024)) {
            common.Printf("Bad pcx file %s (%d x %d) (%d x %d)\n", filename, xmax + 1, ymax + 1, pcx.xmax, pcx.ymax);
            return;
        }

        out = ByteBuffer.allocate((ymax + 1) * (xmax + 1));//(byte *)R_StaticAlloc( (ymax+1) * (xmax+1) );

        pic[0] = out;

        pix = out;

        if (palette != null) {
            palette[0] = ByteBuffer.allocate(768);//(byte *)R_StaticAlloc(768);
//		memcpy (*palette, (byte *)pcx + len - 768, 768);
        }

        if (width != null) {
            width[0] = xmax + 1;
        }
        if (height != null) {
            height[0] = ymax + 1;
        }
// FIXME: use bytes_per_line here?

        for (y = 0; y <= ymax; y++, pix.position(pix.position() + xmax + 1)) {
            for (x = 0; x <= xmax;) {
                dataByte = raw[0].get();

                if ((dataByte & 0xC0) == 0xC0) {
                    runLength = dataByte & 0x3F;
                    dataByte = raw[0].get();
                } else {
                    runLength = 1;
                }

                while (runLength-- > 0) {
                    pix.put(x++, dataByte);
                }
            }

        }

        if (raw[0].position() > len)//TODO: is this even possible?
        {
            common.Printf("PCX file %s was malformed", filename);
//		R_StaticFree (*pic);
            pic[0] = null;
        }

//	fileSystem->FreeFile( pcx );
    }


    /*
     ==============
     LoadPCX32
     ==============
     */
    static ByteBuffer LoadPCX32(final String filename, int[] width, int[] height, long[] timestamp) {
        final ByteBuffer[] palette = {null};
        final ByteBuffer[] pic8 = {null};
        ByteBuffer pic = null;
        int i, c, p;

        if (NOT(width, height)) {
            fileSystem.ReadFile(filename, null, timestamp);
            return null;	// just getting timestamp
        }
        LoadPCX(filename, pic8, palette, width, height, timestamp);
        if (NOT(pic8[0])) {
            return null;
        }

        c = width[0] * height[0];
        pic = Nio.newByteBuffer(4 * c);//(byte *)R_StaticAlloc(4 * c );
        for (i = 0; i < c; i++) {
            p = pic8[0].get(i);
            pic.put(0, palette[0].get(p * 3));
            pic.put(1, palette[0].get((p * 3) + 1));
            pic.put(2, palette[0].get((p * 3) + 2));
            pic.put(3, (byte) 255);
//		pic += 4;
        }

//	R_StaticFree( pic8 );
//	R_StaticFree( palette );
        return pic;
    }

    /*
     ========================================================================================================

     TARGA LOADING

     ========================================================================================================
     */

    /*
     =============
     LoadTGA
     =============
     */
    private static ByteBuffer LoadTGA(final String name, int[] width, int[] height, long[] timestamp) {
        int columns, rows, numPixels, fileSize, numBytes;
        ByteBuffer pixbuf;
        int row, column;
        ByteBuffer buf_p;
        final ByteBuffer[] buffer = {null};
        final TargaHeader targa_header = new TargaHeader();
        ByteBuffer targa_rgba;

        if (NOT(width, height)) {
            fileSystem.ReadFile(name, null, timestamp);
            return null;    // just getting timestamp
        }

        //
        // load the file
        //
        fileSize = fileSystem.ReadFile(name, buffer, timestamp);
        if (NOT(buffer[0])) {
            return null;
        }

        buf_p = buffer[0];
        buf_p.order(ByteOrder.LITTLE_ENDIAN).rewind();

        targa_header.id_length = buf_p.get();
        targa_header.colormap_type = buf_p.get();
        targa_header.image_type = buf_p.get();

        targa_header.colormap_index = LittleShort(buf_p.getShort());//	buf_p += 2;
        targa_header.colormap_length = LittleShort(buf_p.getShort());//	buf_p += 2;
        targa_header.colormap_size = buf_p.get();
        targa_header.x_origin = LittleShort(buf_p.getShort());//	buf_p += 2;
        targa_header.y_origin = LittleShort(buf_p.getShort());//	buf_p += 2;
        targa_header.width = LittleShort(buf_p.getShort());//	buf_p += 2;
        targa_header.height = LittleShort(buf_p.getShort());//	buf_p += 2;
        targa_header.pixel_size = buf_p.get();
        targa_header.attributes = buf_p.get();

        if ((targa_header.image_type != 2) && (targa_header.image_type != 10) && (targa_header.image_type != 3)) {
            common.Error("LoadTGA( %s ): Only type 2 (RGB), 3 (gray), and 10 (RGB) TGA images supported\n", name);
        }

        if (targa_header.colormap_type != 0) {
            common.Error("LoadTGA( %s ): colormaps not supported\n", name);
        }

        if (((targa_header.pixel_size != 32) && (targa_header.pixel_size != 24)) && (targa_header.image_type != 3)) {
            common.Error("LoadTGA( %s ): Only 32 or 24 bit images supported (no colormaps)\n", name);
        }

        if ((targa_header.image_type == 2) || (targa_header.image_type == 3)) {
            numBytes = targa_header.width * targa_header.height * (targa_header.pixel_size >> 3);
            if (numBytes > (fileSize - 18 - targa_header.id_length)) {
                common.Error("LoadTGA( %s ): incomplete file\n", name);
            }
        }

        columns = targa_header.width;
        rows = targa_header.height;
        numPixels = columns * rows;

        if (width != null) {
            width[0] = columns;
        }
        if (height != null) {
            height[0] = rows;
        }

        targa_rgba = Nio.newByteBuffer(numPixels * 4);// (byte *)R_StaticAlloc(numPixels*4);

        if (targa_header.id_length != 0) {
            buf_p.position(buf_p.position() + targa_header.id_length);  // skip TARGA image comment
        }

        if ((targa_header.image_type == 2) || (targa_header.image_type == 3)) {
            // Uncompressed RGB or gray scale image
            for (row = rows - 1; row >= 0; row--) {
                pixbuf = targa_rgba.duplicate();
                pixbuf.position(row * columns * 4);
                for (column = 0; column < columns; column++) {
                    byte /*unsigned char*/ red, green, blue, alphabyte;
                    switch (targa_header.pixel_size) {

                        case 8:
                            blue = buf_p.get();
                            green = blue;
                            red = blue;
                            pixbuf.put(red);
                            pixbuf.put(green);
                            pixbuf.put(blue);
                            pixbuf.put((byte) 255);
                            break;

                        case 24:
                            blue = buf_p.get();
                            green = buf_p.get();
                            red = buf_p.get();
                            pixbuf.put(red);
                            pixbuf.put(green);
                            pixbuf.put(blue);
                            pixbuf.put((byte) 255);
                            break;
                        case 32:
                            blue = buf_p.get();
                            green = buf_p.get();
                            red = buf_p.get();
                            alphabyte = buf_p.get();
                            pixbuf.put(red);
                            pixbuf.put(green);
                            pixbuf.put(blue);
                            pixbuf.put(alphabyte);
                            break;
                        default:
                            common.Error("LoadTGA( %s ): illegal pixel_size '%d'\n", name, targa_header.pixel_size);
                            break;
                    }
                }
            }
        } else if (targa_header.image_type == 10) {   // Runlength encoded RGB images
            byte/*unsigned char*/ red, green, blue, alphabyte;
            int packetHeader, packetSize, j;

            red = 0;
            green = 0;
            blue = 0;
            alphabyte = (byte) 0xff;

            breakOut:
            for (row = rows - 1; row >= 0; row--) {
                pixbuf = targa_rgba.duplicate();
                pixbuf.position(row * columns * 4);
                for (column = 0; column < columns;) {
                    packetHeader = buf_p.get();
                    packetSize = 1 + (packetHeader & 0x7f);
                    if ((packetHeader & 0x80) != 0) {        // run-length packet
                        switch (targa_header.pixel_size) {
                            case 24:
                                blue = buf_p.get();
                                green = buf_p.get();
                                red = buf_p.get();
                                alphabyte = (byte) 255;
                                break;
                            case 32:
                                blue = buf_p.get();
                                green = buf_p.get();
                                red = buf_p.get();
                                alphabyte = buf_p.get();
                                break;
                            default:
                                common.Error("LoadTGA( %s ): illegal pixel_size '%d'\n", name, targa_header.pixel_size);
                                break;
                        }

                        for (j = 0; j < packetSize; j++) {
                            pixbuf.put(red);
                            pixbuf.put(green);
                            pixbuf.put(blue);
                            pixbuf.put(alphabyte);
                            column++;
                            if (column == columns) { // run spans across rows
                                column = 0;
                                if (row > 0) {
                                    row--;
                                } else {
                                    break breakOut;
                                }
                                pixbuf = targa_rgba.duplicate();
                                pixbuf.position(row * columns * 4);
                            }
                        }
                    } else {                            // non run-length packet
                        for (j = 0; j < packetSize; j++) {
                            switch (targa_header.pixel_size) {
                                case 24:
                                    blue = buf_p.get();
                                    green = buf_p.get();
                                    red = buf_p.get();
                                    pixbuf.put(red);
                                    pixbuf.put(green);
                                    pixbuf.put(blue);
                                    pixbuf.put((byte) 255);
                                    break;
                                case 32:
                                    blue = buf_p.get();
                                    green = buf_p.get();
                                    red = buf_p.get();
                                    alphabyte = buf_p.get();
                                    pixbuf.put(red);
                                    pixbuf.put(green);
                                    pixbuf.put(blue);
                                    pixbuf.put(alphabyte);
                                    break;
                                default:
                                    common.Error("LoadTGA( %s ): illegal pixel_size '%d'\n", name, targa_header.pixel_size);
                                    break;
                            }
                            column++;
                            if (column == columns) { // pixel packet run spans across rows
                                column = 0;
                                if (row > 0) {
                                    row--;
                                } else {
                                    break breakOut;
                                }
                                pixbuf = targa_rgba.duplicate();
                                pixbuf.position(row * columns * 4);
                            }
                        }
                    }
                }
//			breakOut: ;
            }
        }

        if ((targa_header.attributes & (1 << 5)) != 0) {			// image flp bit
            R_VerticalFlip(targa_rgba, width[0], height[0]);
        }

//	fileSystem->FreeFile( buffer );

        return targa_rgba;
    }

    /*
     ========================================================================================================

     JPG LOADING

     Interfaces with the huge libjpeg
     EDIT: not anymore    
     ========================================================================================================
     */

    /*
     =============
     LoadJPG
     =============
     */
    private static ByteBuffer LoadJPG(final String filename, int[] width, int[] height, long[] timestamp) {
        /* This struct contains the JPEG decompression parameters and pointers to
         * working space (which is allocated as needed by the JPEG library).
         */
//  struct jpeg_decompress_struct cinfo;
        /* We use our private extension JPEG error handler.
         * Note that this struct must live as long as the main JPEG parameter
         * struct, to avoid dangling-pointer problems.
         */
        /* This struct represents a JPEG error handler.  It is declared separately
         * because applications often want to supply a specialized error handler
         * (see the second half of this file for an example).  But here we just
         * take the easy way out and use the standard error handler, which will
         * print a message on stderr and call exit() if compression fails.
         * Note that this struct must live as long as the main JPEG parameter
         * struct, to avoid dangling-pointer problems.
         */
//  struct jpeg_error_mgr jerr;
  /* More stuff */
        BufferedImage /*JSAMPARRAY*/ buffer;		// Output row buffer
//  int row_stride;		// physical row width in output buffer 
//  unsigned char *out;
        ByteBuffer fbuffer;
//  byte  *bbuf;

        /* In this example we want to open the input file before doing anything else,
         * so that the setjmp() error recovery below can assume the file is open.
         * VERY IMPORTANT: use "b" option to fopen() if you are on a machine that
         * requires it in order to read binary files.
         */
        // JDC: because fill_input_buffer() blindly copies INPUT_BUF_SIZE bytes,
        // we need to make sure the file buffer is padded or it may crash
//        if ( pic ) {
//            *pic = NULL;		// until proven otherwise
//        }
        {
            int len;
            idFile f;

            f = fileSystem.OpenFileRead(filename);
            if (NOT(f)) {
                return null;
            }
            len = f.Length();
            if (timestamp != null) {
                timestamp[0] = f.Timestamp();
            }
            if (NOT(width, height)) {
                fileSystem.CloseFile(f);
                return null;    // just getting timestamp
            }
            fbuffer = Nio.newByteBuffer(len + 4096);//(byte *)Mem_ClearedAlloc( len + 4096 );
            f.Read(fbuffer/*, len*/);
            fileSystem.CloseFile(f);
        }
        try {
            buffer = ImageIO.read(new ByteArrayInputStream(fbuffer.array()));
        } catch (final IOException ex) {
            Logger.getLogger(Image_files.class.getName()).log(Level.SEVERE, null, ex);
            common.Error("Failed to load JPEG ", filename);
            return null;
        }

//
//  /* Step 1: allocate and initialize JPEG decompression object */
//
//  /* We have to set up the error handler first, in case the initialization
//   * step fails.  (Unlikely, but it could happen if you are out of memory.)
//   * This routine fills in the contents of struct jerr, and returns jerr's
//   * address which we place into the link field in cinfo.
//   */
////  cinfo.err = jpeg_std_error(&jerr);
//
//  /* Now we can initialize the JPEG decompression object. */
////  jpeg_create_decompress(&cinfo);
//
//  /* Step 2: specify data source (eg, a file) */
//
////  jpeg_stdio_src(&cinfo, fbuffer);
//
//  /* Step 3: read file parameters with jpeg_read_header() */
//
////  (void) jpeg_read_header(&cinfo, true );
//  /* We can ignore the return value from jpeg_read_header since
//   *   (a) suspension is not possible with the stdio data source, and
//   *   (b) we passed TRUE to reject a tables-only JPEG file as an error.
//   * See libjpeg.doc for more info.
//   */
//
//  /* Step 4: set parameters for decompression */
//
//  /* In this example, we don't need to change any of the defaults set by
//   * jpeg_read_header(), so we do nothing here.
//   */
//
//  /* Step 5: Start decompressor */
//
////  (void) jpeg_start_decompress(&cinfo);
//  /* We can ignore the return value since suspension is not possible
//   * with the stdio data source.
//   */
//
//  /* We may need to do some setup of our own at this point before reading
//   * the data.  After jpeg_start_decompress() we have the correct scaled
//   * output image dimensions available, as well as the output colormap
//   * if we asked for color quantization.
//   * In this example, we need to make an output work buffer of the right size.
//   */ 
//  /* JSAMPLEs per row in output buffer */
//  row_stride = cinfo.output_width * cinfo.output_components;
//
//  if (cinfo.output_components!=4) {
//		common.DWarning( "JPG %s is unsupported color depth (%d)", 
//			filename, cinfo.output_components);
//  }
//  out = (byte *)R_StaticAlloc(cinfo.output_width*cinfo.output_height*4);
//
        final byte[] out = ((DataBufferByte) buffer.getRaster().getDataBuffer()).getData();
        width[0] = buffer.getWidth();//cinfo.output_width;
        height[0] = buffer.getHeight();//cinfo.output_height;
        return ByteBuffer.wrap(out);
//
//  /* Step 6: while (scan lines remain to be read) */
//  /*           jpeg_read_scanlines(...); */
//
//  /* Here we use the library's state variable cinfo.output_scanline as the
//   * loop counter, so that we don't have to keep track ourselves.
//   */
//  while (cinfo.output_scanline < cinfo.output_height) {
//    /* jpeg_read_scanlines expects an array of pointers to scanlines.
//     * Here the array is only one element long, but you could ask for
//     * more than one scanline at a time if that's more convenient.
//     */
//	bbuf = ((out+(row_stride*cinfo.output_scanline)));
//	buffer = &bbuf;
//    (void) jpeg_read_scanlines(&cinfo, buffer, 1);
//  }
//
//  // clear all the alphas to 255
//        {//TODO:should this be enabled?
//	  int	i, j;
//		byte	*buf;
//
//		buf = *pic;
//
//	  j = cinfo.output_width * cinfo.output_height * 4;
//	  for ( i = 3 ; i < j ; i+=4 ) {
//		  buf[i] = 255;
//	  }
//        }
//
//  /* Step 7: Finish decompression */
//
//  (void) jpeg_finish_decompress(&cinfo);
//  /* We can ignore the return value since suspension is not possible
//   * with the stdio data source.
//   */
//
//  /* Step 8: Release JPEG decompression object */
//
//  /* This is an important step since it will release a good deal of memory. */
//  jpeg_destroy_decompress(&cinfo);
//
//  /* After finish_decompress, we can close the input file.
//   * Here we postpone it until after no more JPEG errors are possible,
//   * so as to simplify the setjmp error logic above.  (Actually, I don't
//   * think that jpeg_destroy can do an error exit, but why assume anything...)
//   */
//  Mem_Free( fbuffer );
//
//  /* At this point you may want to check to see whether any corrupt-data
//   * warnings occurred (test whether jerr.pub.num_warnings is nonzero).
//   */
//
//  /* And we're done! */
    }

//===================================================================
    /*
     =================
     R_LoadImage

     Loads any of the supported image types into a cannonical
     32 bit format.

     Automatically attempts to load .jpg files if .tga files fail to load.

     *pic will be NULL if the load failed.

     Anything that is going to make this into a texture would use
     makePowerOf2 = true, but something loading an image as a lookup
     table of some sort would leave it in identity form.

     It is important to do this at image load time instead of texture load
     time for bump maps.

     Timestamp may be NULL if the value is going to be ignored

     If pic is NULL, the image won't actually be loaded, it will just find the
     timestamp.
     =================
     */
    public static ByteBuffer R_LoadImage(final String cname, int[] width, int[] height, long[] timestamp, boolean makePowerOf2) {

        final idStr name = new idStr(cname);
        ByteBuffer pic = null;

        if (timestamp != null) {
            timestamp[0] = 0xFFFFFFFF;
        }
        if (width != null) {
            width[0] = 0;
        }
        if (height != null) {
            height[0] = 0;
        }

        name.DefaultFileExtension(".tga");

        if (name.Length() < 5) {
            return null;
        }

        name.ToLower();
        final idStr ext = new idStr();
        name.ExtractFileExtension(ext);

        if (ext.equals("tga")) {
            pic = LoadTGA(name.getData(), width, height, timestamp);            // try tga first
            if (((pic != null) && (pic.capacity() == 0)) || ((timestamp != null) && (timestamp[0] == -1))) {
                name.StripFileExtension();
                name.DefaultFileExtension(".jpg");
                pic = LoadJPG(name.getData(), width, height, timestamp);
            }
        } else if (ext.equals("pcx")) {
            pic = LoadPCX32(name.getData(), width, height, timestamp);
        } else if (ext.equals("bmp")) {
            pic = LoadBMP(name.getData(), width, height, timestamp);
        } else if (ext.equals("jpg")) {
            pic = LoadJPG(name.getData(), width, height, timestamp);
        }

        if (((width != null) && (width[0] < 1))
                || ((height != null) && (height[0] < 1))) {
            if (pic != null) {
                pic.clear();//R_StaticFree( *pic );
            }
        }

        //
        // convert to exact power of 2 sizes
        //
        if ((pic != null) && makePowerOf2) {
            int w, h;
            int scaled_width, scaled_height;
            ByteBuffer resampledBuffer;

            w = width[0];
            h = height[0];

            for (scaled_width = 1; scaled_width < w; scaled_width <<= 1) {
				;
			}

            for (scaled_height = 1; scaled_height < h; scaled_height <<= 1) {
				;
			}

            if ((scaled_width != w) || (scaled_height != h)) {
                if (globalImages.image_roundDown.GetBool() && (scaled_width > w)) {
                    scaled_width >>= 1;
                }
                if (globalImages.image_roundDown.GetBool() && (scaled_height > h)) {
                    scaled_height >>= 1;
                }

                resampledBuffer = R_ResampleTexture(pic, w, h, scaled_width, scaled_height);
                pic.clear();//R_StaticFree( *pic );
                pic.put(resampledBuffer);
                width[0] = scaled_width;
                height[0] = scaled_height;
            }
        }

        return pic;
    }

    /*
     =======================
     R_LoadCubeImages

     Loads six files with proper extensions
     =======================
     */
    static boolean R_LoadCubeImages(final String imgName, cubeFiles_t extensions, ByteBuffer[] pics/*[6]*/, int[] outSize, /*ID_TIME_T */ long[] timestamp) {
        int i, j;
        final String[] cameraSides = {"_forward.tga", "_back.tga", "_left.tga", "_right.tga", "_up.tga", "_down.tga"};
        final String[] axisSides = {"_px.tga", "_nx.tga", "_py.tga", "_ny.tga", "_pz.tga", "_nz.tga"};
        String[] sides;
        final char[] fullName = new char[MAX_IMAGE_NAME];
        final int[] width = {0}, height = {0};
        int size = 0;

        if (extensions == CF_CAMERA) {
            sides = cameraSides;
        } else {
            sides = axisSides;
        }

        // FIXME: precompressed cube map files
        if (pics != null) {
            for (int k = 0; k < pics.length; k++) {
                pics[k] = null;
            }
        }
        if (timestamp != null) {
            timestamp[0] = 0;
        }

        for (i = 0; i < 6; i++) {
            idStr.snPrintf(fullName, fullName.length, "%s%s", imgName, sides[i]);

            final long[] thisTime = new long[1];
            if (null == pics) {
                // just checking timestamps
                R_LoadImageProgram(ctos(fullName), width, height, thisTime);
            } else {
                pics[i] = R_LoadImageProgram(ctos(fullName),width, height, thisTime);
            }
            if (thisTime[0] == FILE_NOT_FOUND_TIMESTAMP) {
                break;
            }
            if (i == 0) {
                size = width[0];
            }
            if ((width[0] != size) || (height[0] != size)) {
                common.Warning("Mismatched sizes on cube map '%s'", imgName);
                break;
            }
            if (timestamp[0] != 0) {
                if (thisTime[0] > timestamp[0]) {
                    timestamp[0] = thisTime[0];
                }
            }
            if ((pics != null) && (extensions == CF_CAMERA)) {
                // convert from "camera" images to native cube map images
                switch (i) {
                    case 0:	// forward
                        R_RotatePic(pics[i], width[0]);
                        break;
                    case 1:	// back
                        R_RotatePic(pics[i], width[0]);
                        R_HorizontalFlip(pics[i], width[0], height[0]);
                        R_VerticalFlip(pics[i], width[0], height[0]);
                        break;
                    case 2:	// left
                        R_VerticalFlip(pics[i], width[0], height[0]);
                        break;
                    case 3:	// right
                        R_HorizontalFlip(pics[i], width[0], height[0]);
                        break;
                    case 4:	// up
                        R_RotatePic(pics[i], width[0]);
                        break;
                    case 5:     // down
                        R_RotatePic(pics[i], width[0]);
                        break;
                }
            }
        }

        if (i != 6) {
            // we had an error, so free everything
            if (pics != null) {
                for (j = 0; j < i; j++) {
                    pics[j] = null;// R_StaticFree(pics[j]);
                }
            }

            if (timestamp != null) {
                timestamp[0] = 0;
            }
            return false;
        }

        if (outSize != null) {
            outSize[0] = size;
        }
        return true;
    }

    @Deprecated
    public static void R_LoadImage(final String cname, byte[] pic, int[] width, int[] height, long[] timestamp, boolean makePowerOf2) {
        throw new TODO_Exception();
    }
}
