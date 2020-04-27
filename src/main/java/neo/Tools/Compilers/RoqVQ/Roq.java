package neo.Tools.Compilers.RoqVQ;

import static neo.TempDump.NOT;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.CCC;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.DEAD;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.DEP;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.FCC;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.MINSIZE;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.MOT;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.PAT;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.RoQ_ID;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.RoQ_QUAD_CODEBOOK;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.RoQ_QUAD_HANG;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.RoQ_QUAD_INFO;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.RoQ_QUAD_VQ;
import static neo.Tools.Compilers.RoqVQ.QuadDefs.SLD;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Session.session;
import static neo.sys.win_shared.Sys_Milliseconds;

import java.nio.ByteBuffer;

import neo.TempDump.TODO_Exception;
import neo.Tools.Compilers.RoqVQ.Codec.codec;
import neo.Tools.Compilers.RoqVQ.QuadDefs.quadcel;
import neo.Tools.Compilers.RoqVQ.RoqParam.roqParam;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.File_h.idFile;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Lib.idException;
import neo.idlib.Text.Str.idStr;

/**
 *
 */
public class Roq {

    static class roq {

        private codec            encoder;
        private roqParam         paramFile;
        //
        private idFile           RoQFile;
        private NSBitmapImageRep image;
        private int              numQuadCels;
        private boolean          quietMode;
        private boolean          lastFrame;
        private idStr            roqOutfile;
        private idStr            currentFile;
        private int              numberOfFrames;
        private int              previousSize;
        private byte[] codes = new byte[4096];
        private boolean dataStuff;
        //
        //

        public roq() {
            image = null;//0;
            quietMode = false;
            encoder = null;//0;
            previousSize = 0;
            lastFrame = false;
            dataStuff = false;
        }
        // ~roq();

        public void WriteLossless() {
            throw new TODO_Exception();
//
//            int/*word*/ direct;
//            int/*uint*/ directdw;
//
//            if (!dataStuff) {
//                InitRoQPatterns();
//                dataStuff = true;
//            }
//            direct = RoQ_QUAD_JPEG;
//            Write16Word(direct, RoQFile);
//
//            /* This struct contains the JPEG compression parameters and pointers to
//             * working space (which is allocated as needed by the JPEG library).
//             * It is possible to have several such structures, representing multiple
//             * compression/decompression processes, in existence at once.  We refer
//             * to any one struct (and its associated working data) as a "JPEG object".
//             */
//            jpeg_compress_struct cinfo;
//            /* This struct represents a JPEG error handler.  It is declared separately
//             * because applications often want to supply a specialized error handler
//             * (see the second half of this file for an example).  But here we just
//             * take the easy way out and use the standard error handler, which will
//             * print a message on stderr and call exit() if compression fails.
//             * Note that this struct must live as long as the main JPEG parameter
//             * struct, to avoid dangling-pointer problems.
//             */
//            jpeg_error_mgr jerr;
//            /* More stuff */
//            JSAMPROW[] row_pointer = new JSAMPROW[1];	/* pointer to JSAMPLE row[s] */
//
//            int row_stride;		/* physical row width in image buffer */
//
//            ByteBuffer out;
//
//            /* Step 1: allocate and initialize JPEG compression object */
//
//            /* We have to set up the error handler first, in case the initialization
//             * step fails.  (Unlikely, but it could happen if you are out of memory.)
//             * This routine fills in the contents of struct jerr, and returns jerr's
//             * address which we place into the link field in cinfo.
//             */
//            cinfo.err = jpeg_std_error(jerr);
//            /* Now we can initialize the JPEG compression object. */
//            jpeg_create_compress(cinfo);
//
//            /* Step 2: specify data destination (eg, a file) */
//            /* Note: steps 2 and 3 can be done in either order. */
//
//            /* Here we use the library-supplied code to send compressed data to a
//             * stdio stream.  You can also write your own code to do something else.
//             * VERY IMPORTANT: use "b" option to fopen() if you are on a machine that
//             * requires it in order to write binary files.
//             */
//            out = ByteBuffer.allocate(image.pixelsWide() * image.pixelsHigh() * 4);// Mem_Alloc(image.pixelsWide() * image.pixelsHigh() * 4);
//            JPEGDest(cinfo, out, image.pixelsWide() * image.pixelsHigh() * 4);
//
//            /* Step 3: set parameters for compression */
//
//            /* First we supply a description of the input image.
//             * Four fields of the cinfo struct must be filled in:
//             */
//            cinfo.image_width = image.pixelsWide(); 	/* image width and height, in pixels */
//
//            cinfo.image_height = image.pixelsHigh();
//            cinfo.input_components = 4;		/* # of color components per pixel */
//
//            cinfo.in_color_space = JCS_RGB; 	/* colorspace of input image */
//            /* Now use the library's routine to set default compression parameters.
//             * (You must set at least cinfo.in_color_space before calling this,
//             * since the defaults depend on the source color space.)
//             */
//
//            jpeg_set_defaults(cinfo);
//            /* Now you can set any non-default parameters you wish to.
//             * Here we just illustrate the use of quality (quantization table) scaling:
//             */
//            jpeg_set_quality(cinfo, paramFile.JpegQuality(), true /* limit to baseline-JPEG values */);
//
//            /* Step 4: Start compressor */
//
//            /* true ensures that we will write a complete interchange-JPEG file.
//             * Pass true unless you are very sure of what you're doing.
//             */
//            JPEGStartCompress(cinfo, true);
//
//            /* Step 5: while (scan lines remain to be written) */
//            /*           jpeg_write_scanlines(...); */
//
//            /* Here we use the library's state variable cinfo.next_scanline as the
//             * loop counter, so that we don't have to keep track ourselves.
//             * To keep things simple, we pass one scanline per call; you can pass
//             * more if you wish, though.
//             */
//            row_stride = image.pixelsWide() * 4;	/* JSAMPLEs per row in image_buffer */
//
//            byte[] pixbuf = image.bitmapData();
//            while (cinfo.next_scanline < cinfo.image_height) {
//                /* jpeg_write_scanlines expects an array of pointers to scanlines.
//                 * Here the array is only one element long, but you could pass
//                 * more than one scanline at a time if that's more convenient.
//                 */
//                row_pointer[0] = pixbuf[((cinfo.image_height - 1) * row_stride) - cinfo.next_scanline * row_stride];
//                /*(void)*/ JPEGWriteScanlines(cinfo, row_pointer, 1);
//            }
//
//            /* Step 6: Finish compression */
//            jpeg_finish_compress(cinfo);
//            /* After finish_compress, we can close the output file. */
//
//            directdw = hackSize;
//            common.Printf("writeLossless: writing %d bytes to RoQ_QUAD_JPEG\n", hackSize);
//            Write32Word(directdw, RoQFile);
//            direct = 0;		// flags
//            Write16Word(direct, RoQFile);
//
//            RoQFile.Write(out, hackSize);
//            out = null;//Mem_Free(out);
//
//            /* Step 7: release JPEG compression object */
//
//            /* This is an important step since it will release a good deal of memory. */
//            jpeg_destroy_compress(cinfo);
//
//            /* And we're done! */
//            encoder.SetPreviousImage("first frame", image);
        }

        //
        // load a frame, create a window (if neccesary) and display the frame
        //
        public void LoadAndDisplayImage(final String filename) {
//	if (image) delete image;

            common.Printf("loadAndDisplayImage: %s\n", filename);

            currentFile.oSet(filename);

            image = new NSBitmapImageRep(filename);

            numQuadCels = ((image.pixelsWide() & 0xfff0) * (image.pixelsHigh() & 0xfff0)) / (MINSIZE * MINSIZE);
            numQuadCels += numQuadCels / 4 + numQuadCels / 16;

//	if (paramFile->deltaFrames] == true && cleared == false && [image isPlanar] == false) {
//		cleared = true;
//		imageData = [image data];
//		memset( imageData, 0, image->pixelsWide()*image->pixelsHigh()*[image samplesPerPixel]);
//	}
            if (!quietMode) {
                common.Printf("loadAndDisplayImage: %dx%d\n", image.pixelsWide(), image.pixelsHigh());
            }
        }

        public void CloseRoQFile(boolean which) {
            common.Printf("closeRoQFile: closing RoQ file\n");
            fileSystem.CloseFile(RoQFile);
        }
        private static int finit = 0;

        public void InitRoQFile(final String RoQFilename) {
            int/*word*/ i;

            if (0 == finit) {
                finit++;
                common.Printf("initRoQFile: %s\n", RoQFilename);
                RoQFile = fileSystem.OpenFileWrite(RoQFilename);
//		chmod(RoQFilename, S_IREAD|S_IWRITE|S_ISUID|S_ISGID|0070|0007 );
                if (null == RoQFile) {
                    common.Error("Unable to open output file %s.\n", RoQFilename);
                }

                i = RoQ_ID;
                Write16Word(i, RoQFile);

                i = 0xffff;
                Write16Word(i, RoQFile);
                Write16Word(i, RoQFile);

                // to retain exact file format write out 32 for new roq's
                // on loading this will be noted and converted to 1000 / 30
                // as with any new sound dump avi demos we need to playback
                // at the speed the sound engine dumps the audio
                i = 30;						// framerate
                Write16Word(i, RoQFile);
            }
            roqOutfile.oSet(RoQFilename);
        }

        public void InitRoQPatterns() {
            int/*uint*/ j;
            int/*word*/ direct;

            direct = RoQ_QUAD_INFO;
            Write16Word(direct, RoQFile);

            j = 8;

            Write32Word(j, RoQFile);
            common.Printf("initRoQPatterns: outputting %d bytes to RoQ_INFO\n", j);
            direct = image.hasAlpha() ? 1 : 0;
            if (ParamNoAlpha() == true) {
                direct = 0;
            }

            Write16Word(direct, RoQFile);

            direct = image.pixelsWide();
            Write16Word(direct, RoQFile);
            direct = image.pixelsHigh();
            Write16Word(direct, RoQFile);
            direct = 8;
            Write16Word(direct, RoQFile);
            direct = 4;
            Write16Word(direct, RoQFile);
        }

        public void EncodeStream(final String paramInputFile) {
            int onFrame;
            String f0, f1, f2 = null;
            int morestuff;

            onFrame = 1;

            encoder = new codec();
            paramFile = new roqParam();
            paramFile.numInputFiles = 0;

            paramFile.InitFromFile(paramInputFile);

            if (NOT(paramFile.NumberOfFrames())) {
                return;
            }

            InitRoQFile(paramFile.outputFilename.toString());

            numberOfFrames = paramFile.NumberOfFrames();

            if (paramFile.NoAlpha() == true) {
                common.Printf("encodeStream: eluding alpha\n");
            }

            f0 = "";
            f1 = paramFile.GetNextImageFilename();
            if ((paramFile.MoreFrames() == true)) {
                f2 = paramFile.GetNextImageFilename();
            }
            morestuff = numberOfFrames;

            while (morestuff != 0) {
                LoadAndDisplayImage(f1);

                if (onFrame == 1) {
                    encoder.SparseEncode();
//			WriteLossless();
                } else {
                    if (f0.equals(f1) && !f1.equals(f2)) {
                        WriteHangFrame();
                    } else {
                        encoder.SparseEncode();
                    }
                }

                onFrame++;
                f0 = f1;
                f1 = f2;
                if (paramFile.MoreFrames() == true) {
                    f2 = paramFile.GetNextImageFilename();
                }
                morestuff--;
                session.UpdateScreen();
            }

//	if (numberOfFrames != 1) {
//		if (image->hasAlpha() && paramFile->NoAlpha()==false) {
//			lastFrame = true;
//			encoder->SparseEncode();
//		} else {
//			WriteLossless();
//		}
//	}
            CloseRoQFile();
        }

        public void EncodeQuietly(boolean which) {
            quietMode = which;
        }

        public boolean IsQuiet() {
            return quietMode;
        }

        public boolean IsLastFrame() {
            return lastFrame;
        }

        public NSBitmapImageRep CurrentImage() {
            return image;
        }

        public void MarkQuadx(int xat, int yat, int size, float cerror, int choice) {
        }
//
//        public void WritePuzzleFrame(quadcel pquad);
//

        public void WriteFrame(quadcel[] pquad) {
            int/*word*/ action, direct;
            int onCCC, onAction, i, code;
            int/*uint*/ j;
            byte[] cccList;
            boolean[] use2, use4;
            int dx, dy, dxMean, dyMean, dimension;
            int[] index2 = new int[256], index4 = new int[256];

            cccList = new byte[numQuadCels * 8];// Mem_Alloc(numQuadCels * 8);					// maximum length 
            use2 = new boolean[256];// Mem_Alloc(256);
            use4 = new boolean[256];// Mem_Alloc(256);

            for (i = 0; i < 256; i++) {
                use2[i] = false;
                use4[i] = false;
            }

            action = 0;
            j = onAction = 0;
            onCCC = 2;											// onAction going to go at zero

            dxMean = encoder.MotMeanX();
            dyMean = encoder.MotMeanY();

            if (image.hasAlpha()) {
                dimension = 10;
            } else {
                dimension = 6;
            }

            for (i = 0; i < numQuadCels; i++) {
                if (pquad[i].size != 0 && pquad[i].size < 16) {
                    switch (pquad[i].status) {
                        case SLD:
                            use4[pquad[i].patten[0]] = true;
                            use2[codes[dimension * 256 + (pquad[i].patten[0] * 4) + 0]] = true;
                            use2[codes[dimension * 256 + (pquad[i].patten[0] * 4) + 1]] = true;
                            use2[codes[dimension * 256 + (pquad[i].patten[0] * 4) + 2]] = true;
                            use2[codes[dimension * 256 + (pquad[i].patten[0] * 4) + 3]] = true;
                            break;
                        case PAT:
                            use4[pquad[i].patten[0]] = true;
                            use2[codes[dimension * 256 + (pquad[i].patten[0] * 4) + 0]] = true;
                            use2[codes[dimension * 256 + (pquad[i].patten[0] * 4) + 1]] = true;
                            use2[codes[dimension * 256 + (pquad[i].patten[0] * 4) + 2]] = true;
                            use2[codes[dimension * 256 + (pquad[i].patten[0] * 4) + 3]] = true;
                            break;
                        case CCC:
                            use2[pquad[i].patten[1]] = true;
                            use2[pquad[i].patten[2]] = true;
                            use2[pquad[i].patten[3]] = true;
                            use2[pquad[i].patten[4]] = true;
                    }
                }
            }

            if (!dataStuff) {
                dataStuff = true;
                InitRoQPatterns();
                if (image.hasAlpha()) {
                    i = 3584;
                } else {
                    i = 2560;
                }
                WriteCodeBookToStream(codes, i, 0);
                for (i = 0; i < 256; i++) {
                    index2[i] = i;
                    index4[i] = i;
                }
            } else {
                j = 0;
                for (i = 0; i < 256; i++) {
                    if (use2[i]) {
                        index2[i] = j;
                        for (dx = 0; dx < dimension; dx++) {
                            cccList[j * dimension + dx] = codes[i * dimension + dx];
                        }
                        j++;
                    }
                }
                code = j * dimension;
                direct = j;
                common.Printf("writeFrame: really used %d 2x2 cels\n", j);
                j = 0;
                for (i = 0; i < 256; i++) {
                    if (use4[i]) {
                        index4[i] = j;
                        for (dx = 0; dx < 4; dx++) {
                            cccList[j * 4 + code + dx] = (byte) index2[codes[i * 4 + (dimension * 256) + dx]];
                        }
                        j++;
                    }
                }
                code += j * 4;
                direct = (direct << 8) + j;
                common.Printf("writeFrame: really used %d 4x4 cels\n", j);
                if (image.hasAlpha()) {
                    i = 3584;
                } else {
                    i = 2560;
                }
                if (code == i || j == 256) {
                    WriteCodeBookToStream(codes, i, 0);
                } else {
                    WriteCodeBookToStream(cccList, code, direct);
                }
            }

            action = 0;
            j = onAction = 0;

            for (i = 0; i < numQuadCels; i++) {
                if (pquad[i].size != 0 && pquad[i].size < 16) {
                    code = -1;
                    switch (pquad[i].status) {
                        case DEP:
                            code = 3;
                            break;
                        case SLD:
                            code = 2;
                            cccList[onCCC++] = (byte) index4[pquad[i].patten[0]];
                            break;
                        case MOT:
                            code = 0;
                            break;
                        case FCC:
                            code = 1;
                            dx = ((pquad[i].domain >> 8)) - 128 - dxMean + 8;
                            dy = ((pquad[i].domain & 0xff)) - 128 - dyMean + 8;
                            if (dx > 15 || dx < 0 || dy > 15 || dy < 0) {
                                common.Error("writeFrame: FCC error %d,%d mean %d,%d at %d,%d,%d rmse %f\n", dx, dy, dxMean, dyMean, pquad[i].xat, pquad[i].yat, pquad[i].size, pquad[i].snr[FCC]);
                            }
                            cccList[onCCC++] = (byte) ((dx << 4) + dy);
                            break;
                        case PAT:
                            code = 2;
                            cccList[onCCC++] = (byte) index4[pquad[i].patten[0]];
                            break;
                        case CCC:
                            code = 3;
                            cccList[onCCC++] = (byte) index2[pquad[i].patten[1]];
                            cccList[onCCC++] = (byte) index2[pquad[i].patten[2]];
                            cccList[onCCC++] = (byte) index2[pquad[i].patten[3]];
                            cccList[onCCC++] = (byte) index2[pquad[i].patten[4]];
                            break;
                        case DEAD:
                            common.Error("dead cels in picture\n");
                            break;
                    }
                    if (code == -1) {
                        common.Error("writeFrame: an error occurred writing the frame\n");
                    }

                    action = (action << 2) | code;
                    j++;
                    if (j == 8) {
                        j = 0;
                        cccList[onAction + 0] = (byte) (action & 0xff);
                        cccList[onAction + 1] = (byte) ((action >> 8) & 0xff);
                        onAction = onCCC;
                        onCCC += 2;
                    }
                }
            }

            if (j != 0) {
                action <<= ((8 - j) * 2);
                cccList[onAction + 0] = (byte) (action & 0xff);
                cccList[onAction + 1] = (byte) ((action >> 8) & 0xff);
            }

            direct = RoQ_QUAD_VQ;

            Write16Word(direct, RoQFile);

            j = onCCC;
            Write32Word(j, RoQFile);

            direct = dyMean;
            direct &= 0xff;
            direct += (dxMean << 8);		// flags

            Write16Word(direct, RoQFile);

            common.Printf("writeFrame: outputting %d bytes to RoQ_QUAD_VQ\n", j);

            previousSize = j;

            RoQFile.Write(ByteBuffer.wrap(cccList), onCCC);

            cccList = null;//Mem_Free(cccList);
            use2 = null;//Mem_Free(use2);
            use4 = null;//Mem_Free(use4);
        }

        public void WriteCodeBook(byte[] codebook) {
//	memcpy( codes, codebook, 4096 );
            System.arraycopy(codebook, 0, codes, 0, 4096);
        }

        public void WriteCodeBookToStream(byte[] codebook, int csize, int/*word*/ cflags) {
            int/*uint*/ j;
            int/*word*/ direct;

            if (0 == csize) {
                common.Printf("writeCodeBook: false VQ DATA!!!!\n");
                return;
            }

            direct = RoQ_QUAD_CODEBOOK;

            Write16Word(direct, RoQFile);

            j = csize;

            Write32Word(j, RoQFile);
            common.Printf("writeCodeBook: outputting %d bytes to RoQ_QUAD_CODEBOOK\n", j);

            direct = cflags;
            Write16Word(direct, RoQFile);

            RoQFile.Write(ByteBuffer.wrap(codebook), j);
        }

        public int PreviousFrameSize() {
            return previousSize;
        }

        public boolean MakingVideo() {
            return true;	//paramFile->timecode];
        }

        public boolean ParamNoAlpha() {
            return paramFile.NoAlpha();
        }

        public boolean SearchType() {
            return paramFile.SearchType();
        }

        public boolean HasSound() {
            return paramFile.HasSound();
        }

        public String CurrentFilename() {
            return currentFile.toString();
        }

        public int NormalFrameSize() {
            return paramFile.NormalFrameSize();
        }

        public int FirstFrameSize() {
            return paramFile.FirstFrameSize();
        }

        public boolean Scaleable() {
            return paramFile.IsScaleable();
        }

        public void WriteHangFrame() {
            int/*uint*/ j;
            int/*word*/ direct;
            common.Printf("*******************************************************************\n");
            direct = RoQ_QUAD_HANG;
            Write16Word(direct, RoQFile);
            j = 0;
            Write32Word(j, RoQFile);
            direct = 0;
            Write16Word(direct, RoQFile);
        }

        public int NumberOfFrames() {
            return numberOfFrames;
        }

        private void Write16Word(int/*word*/ aWord, idFile stream) {
//            byte a, b;
//
//            a = (byte) (aWord & 0xff);
//            b = (byte) (aWord >> 8);
//
//            stream.Write(a, 1);
//            stream.Write(b, 1);

            stream.WriteInt(aWord);
        }

        private void Write32Word(int/*unsigned int*/ aWord, idFile stream) {
//            byte a, b, c, d;
//
//            a = (byte) (aWord & 0xff);
//            b = (byte) ((aWord >> 8) & 0xff);
//            c = (byte) ((aWord >> 16) & 0xff);
//            d = (byte) ((aWord >> 24) & 0xff);
//
//            stream.Write(a, 1);
//            stream.Write(b, 1);
//            stream.Write(c, 1);
//            stream.Write(d, 1);
//            
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putInt(aWord);

            stream.Write(buffer);
        }

        private int SizeFile(idFile ftoSize) {
            return ftoSize.Length();
        }

        private void CloseRoQFile() {
            common.Printf("closeRoQFile: closing RoQ file\n");
            fileSystem.CloseFile(RoQFile);
        }
        /*
         * Initialize destination --- called by jpeg_start_compress
         * before any data is actually written.
         */

        private static void JPEGInitDestination(j_compress_ptr cinfo) {
            throw new TODO_Exception();
//            my_dest_ptr dest = (my_dest_ptr) cinfo.dest;
//
//            dest.pub.next_output_byte = dest.outfile;
//            dest.pub.free_in_buffer = dest.size;
        }

        /*
         * Empty the output buffer --- called whenever buffer fills up.
         *
         * In typical applications, this should write the entire output buffer
         * (ignoring the current state of next_output_byte & free_in_buffer),
         * reset the pointer & count to the start of the buffer, and return true
         * indicating that the buffer has been dumped.
         *
         * In applications that need to be able to suspend compression due to output
         * overrun, a FALSE return indicates that the buffer cannot be emptied now.
         * In this situation, the compressor will return to its caller (possibly with
         * an indication that it has not accepted all the supplied scanlines).  The
         * application should resume compression after it has made more room in the
         * output buffer.  Note that there are substantial restrictions on the use of
         * suspension --- see the documentation.
         *
         * When suspending, the compressor will back up to a convenient restart point
         * (typically the start of the current MCU). next_output_byte & free_in_buffer
         * indicate where the restart point will be if the current call returns FALSE.
         * Data beyond this point will be regenerated after resumption, so do not
         * write it out when emptying the buffer externally.
         */
        private static boolean JPEGEmptyOutputBuffer(j_compress_ptr cinfo) {
            return true;
        }

        /*
         * Terminate destination --- called by jpeg_finish_compress
         * after all data has been written.  Usually needs to flush buffer.
         *
         * NB: *not* called by jpeg_abort or jpeg_destroy; surrounding
         * application must deal with any cleanup that should happen even
         * for error exit.
         */
        static int hackSize;

        private static void JPEGTermDestination(j_compress_ptr cinfo) {
            throw new TODO_Exception();
//            my_dest_ptr dest = (my_dest_ptr) cinfo.dest;
//            size_t datacount = dest.size - dest.pub.free_in_buffer;
//            hackSize = datacount;
        }

        /*
         * Compression initialization.
         * Before calling this, all parameters and a data destination must be set up.
         *
         * We require a write_all_tables parameter as a failsafe check when writing
         * multiple datastreams from the same compression object.  Since prior runs
         * will have left all the tables marked sent_table=true, a subsequent run
         * would emit an abbreviated stream (no tables) by default.  This may be what
         * is wanted, but for safety's sake it should not be the default behavior:
         * programmers should have to make a deliberate choice to emit abbreviated
         * images.  Therefore the documentation and examples should encourage people
         * to pass write_all_tables=true; then it will take active thought to do the
         * wrong thing.
         */
        private void JPEGStartCompress(j_compress_ptr cinfo, boolean write_all_tables) {
            throw new TODO_Exception();
//            if (cinfo.global_state != CSTATE_START) {
//                ERREXIT1(cinfo, JERR_BAD_STATE, cinfo.global_state);
//            }
//
//            if (write_all_tables) {
//                jpeg_suppress_tables(cinfo, false);	/* mark all tables to be written */
//
//            }
//            /* (Re)initialize error mgr and destination modules */
//            (cinfo.err.reset_error_mgr) ((j_common_ptr) cinfo);
//            (cinfo.dest.init_destination) (cinfo);
//            /* Perform master selection of active modules */
//            jinit_compress_master(cinfo);
//            /* Set up for the first pass */
//            (cinfo.master.prepare_for_pass) (cinfo);
//            /* Ready for application to drive first pass through jpeg_write_scanlines
//             * or jpeg_write_raw_data.
//             */
//            cinfo.next_scanline = 0;
//            cinfo.global_state = (cinfo.raw_data_in ? CSTATE_RAW_OK : CSTATE_SCANNING);
        }

        /*
         * Write some scanlines of data to the JPEG compressor.
         *
         * The return value will be the number of lines actually written.
         * This should be less than the supplied num_lines only in case that
         * the data destination module has requested suspension of the compressor,
         * or if more than image_height scanlines are passed in.
         *
         * Note: we warn about excess calls to jpeg_write_scanlines() since
         * this likely signals an application programmer error.  However,
         * excess scanlines passed in the last valid call are *silently* ignored,
         * so that the application need not adjust num_lines for end-of-image
         * when using a multiple-scanline buffer.
         */
        private int/*JDIMENSION*/ JPEGWriteScanlines(j_compress_ptr cinfo, char[]/*JSAMPARRAY*/ scanlines, int/*JDIMENSION*/ num_lines) {
            throw new TODO_Exception();
//            JDIMENSION row_ctr, rows_left;
//
//            if (cinfo.global_state != CSTATE_SCANNING) {
//                ERREXIT1(cinfo, JERR_BAD_STATE, cinfo.global_state);
//            }
//            if (cinfo.next_scanline >= cinfo.image_height) {
//                WARNMS(cinfo, JWRN_TOO_MUCH_DATA);
//            }
//
//            /* Call progress monitor hook if present */
//            if (cinfo.progress != null) {
//                cinfo.progress.pass_counter = (long) cinfo.next_scanline;
//                cinfo.progress.pass_limit = (long) cinfo.image_height;
//                (cinfo.progress.progress_monitor) ((j_common_ptr) cinfo);
//            }
//
//            /* Give master control module another chance if this is first call to
//             * jpeg_write_scanlines.  This lets output of the frame/scan headers be
//             * delayed so that application can write COM, etc, markers between
//             * jpeg_start_compress and jpeg_write_scanlines.
//             */
//            if (cinfo.master.call_pass_startup) {
//                (cinfo.master.pass_startup) (cinfo);
//            }
//
//            /* Ignore any extra scanlines at bottom of image. */
//            rows_left = cinfo.image_height - cinfo.next_scanline;
//            if (num_lines > rows_left) {
//                num_lines = rows_left;
//            }
//
//            row_ctr = 0;
//            cinfo.main.process_data(cinfo, scanlines, row_ctr, num_lines);
//            cinfo.next_scanline += row_ctr;
//            return row_ctr;
        }

        /*
         * Prepare for output to a stdio stream.
         * The caller must have already opened the stream, and is responsible
         * for closing it after finishing compression.
         */
        private void JPEGDest(j_compress_ptr cinfo, ByteBuffer outfile, int size) {
            throw new TODO_Exception();
//            my_dest_ptr dest;
//
//            /* The destination object is made permanent so that multiple JPEG images
//             * can be written to the same file without re-executing jpeg_stdio_dest.
//             * This makes it dangerous to use this manager and a different destination
//             * manager serially with the same JPEG object, because their private object
//             * sizes may be different.  Caveat programmer.
//             */
//            if (cinfo.dest == null) {	/* first time for this JPEG object? */
//
//                cinfo.dest = (jpeg_destination_mgr) cinfo.mem.alloc_small((j_common_ptr) cinfo, JPOOL_PERMANENT, sizeof(my_destination_mgr));
//            }
//
//            dest = (my_dest_ptr) cinfo.dest;
//            dest.pub.init_destination = JPEGInitDestination;
//            dest.pub.empty_output_buffer = JPEGEmptyOutputBuffer;
//            dest.pub.term_destination = JPEGTermDestination;
//            dest.outfile = outfile;
//            dest.size = size;
        }

//        private void JPEGSave(String[] filename, int quality, int image_width, int image_height, /*unsigned*/ char[] image_buffer);
    };
    public static roq theRoQ;				// current roq 

    public static class RoQFileEncode_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new RoQFileEncode_f();

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) throws idException {
            if (args.Argc() != 2) {
                common.Printf("Usage: roq <paramfile>\n");
                return;
            }
            theRoQ = new roq();
            int startMsec = Sys_Milliseconds();
            theRoQ.EncodeStream(args.Argv(1));
            int stopMsec = Sys_Milliseconds();
            common.Printf("total encoding time: %d second\n", (stopMsec - startMsec) / 1000);

        }
    }

    private static class j_compress_ptr {

        public j_compress_ptr() {
        }
    }
}
