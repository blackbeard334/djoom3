package neo.Renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import static neo.Renderer.Cinematic.cinStatus_t.FMV_EOF;
import static neo.Renderer.Cinematic.cinStatus_t.FMV_IDLE;
import static neo.Renderer.Cinematic.cinStatus_t.FMV_LOOPED;
import static neo.Renderer.Cinematic.cinStatus_t.FMV_PLAY;
import static neo.Renderer.RenderSystem_init.r_skipROQ;
import static neo.Renderer.tr_local.backEnd;
import static neo.Sound.snd_system.soundSystem;
import static neo.TempDump.NOT;
import neo.TempDump.TODO_Exception;
import static neo.TempDump.wrapToNativeBuffer;
import static neo.framework.Common.common;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_SET;
import neo.framework.File_h.idFile;
import static neo.idlib.Lib.LittleLong;
import neo.idlib.Text.Str.idStr;

/**
 *
 */
public class Cinematic {

    static final int INPUT_BUF_SIZE = 32768;/* choose an efficiently fread'able size */


    /*
     ===============================================================================

     RoQ cinematic

     Multiple idCinematics can run simultaniously.
     A single idCinematic can be reused for multiple files if desired.

     ===============================================================================
     */
// cinematic states
    enum cinStatus_t {

        FMV_IDLE,
        FMV_PLAY, // play
        FMV_EOF,  // all other conditions, i.e. stop/EOF/abort
        FMV_ID_BLT,
        FMV_ID_IDLE,
        FMV_LOOPED,
        FMV_ID_WAIT
    };

    // a cinematic stream generates an image buffer, which the caller will upload to a texture
    public static class cinData_t {

        public int imageWidth, imageHeight;    // will be a power of 2
        public ByteBuffer  image;                // RGBA format, alpha will be 255
        public cinStatus_t status;
    };
    //
    static final int        CIN_system         = 1;
    static final int        CIN_loop           = 2;
    static final int        CIN_hold           = 4;
    static final int        CIN_silent         = 8;
    static final int        CIN_shader         = 16;
    //
    static final int        DEFAULT_CIN_WIDTH  = 512;
    static final int        DEFAULT_CIN_HEIGHT = 512;
    static final int        MAXSIZE            = 8;
    static final int        MINSIZE            = 4;
    //
    static final int        ROQ_FILE           = 0x1084;
    static final int        ROQ_QUAD           = 0x1000;
    static final int        ROQ_QUAD_INFO      = 0x1001;
    static final int        ROQ_CODEBOOK       = 0x1002;
    static final int        ROQ_QUAD_VQ        = 0x1011;
    static final int        ROQ_QUAD_JPEG      = 0x1012;
    static final int        ROQ_QUAD_HANG      = 0x1013;
    static final int        ROQ_PACKET         = 0x1030;
    static final int        ZA_SOUND_MONO      = 0x1020;
    static final int        ZA_SOUND_STEREO    = 0x1021;
    //
// temporary buffers used by all cinematics
    static final long[]     ROQ_YY_tab         = new long[256];
    static final long[]     ROQ_UB_tab         = new long[256];
    static final long[]     ROQ_UG_tab         = new long[256];
    static final long[]     ROQ_VG_tab         = new long[256];
    static final long[]     ROQ_VR_tab         = new long[256];
    static       int[]      file               = null;
    static       ByteBuffer vq2                = null;
    static       ByteBuffer vq4                = null;
    static       ByteBuffer vq8                = null;
//

    public static abstract class idCinematic implements Cloneable {

        // initialize cinematic play back data
        public static void InitCinematic() {
            float t_ub, t_vr, t_ug, t_vg;
            int i;

            // generate YUV tables
            t_ub = (1.77200f / 2.0f) * (float) (1 << 6) + 0.5f;
            t_vr = (1.40200f / 2.0f) * (float) (1 << 6) + 0.5f;
            t_ug = (0.34414f / 2.0f) * (float) (1 << 6) + 0.5f;
            t_vg = (0.71414f / 2.0f) * (float) (1 << 6) + 0.5f;
            for (i = 0; i < 256; i++) {
                float x = (float) (2 * i - 255);

                ROQ_UB_tab[i] = (long) ((t_ub * x) + (1 << 5));
                ROQ_VR_tab[i] = (long) ((t_vr * x) + (1 << 5));
                ROQ_UG_tab[i] = (long) ((-t_ug * x));
                ROQ_VG_tab[i] = (long) ((-t_vg * x) + (1 << 5));
                ROQ_YY_tab[i] = (i << 6) | (i >> 2);
            }

            file = new int[65536];// Mem_Alloc(65536);
            vq2 = ByteBuffer.allocate(256 * 16 * 4 * 2).order(ByteOrder.LITTLE_ENDIAN);// Mem_Alloc(256*16*4 * sizeof( word ));
            vq4 = ByteBuffer.allocate(256 * 64 * 4 * 2).order(ByteOrder.LITTLE_ENDIAN);// Mem_Alloc(256*64*4 * sizeof( word ));
            vq8 = ByteBuffer.allocate(256 * 256 * 4 * 2).order(ByteOrder.LITTLE_ENDIAN);// Mem_Alloc(256*256*4 * sizeof( word ));

//            //TODO:for debug purposes only.
//            short[] bla2 = new short[256 * 16 * 4];
//            short[] bla4 = new short[256 * 64 * 4];
//            short[] bla8 = new short[256 * 256 * 4];
//            Arrays.fill(bla2, (short) 0xCD);
//            Arrays.fill(bla4, (short) 0xCD);
//            Arrays.fill(bla8, (short) 0xCD);
//            Arrays.fill(file, 0xCD);
//            vq2.asShortBuffer().put(bla2);
//            vq4.asShortBuffer().put(bla4);
//            vq8.asShortBuffer().put(bla8);
        }

        // shutdown cinematic play back data
        public static void ShutdownCinematic() {
            file = null;
            vq2 = null;
            vq4 = null;
            vq8 = null;
        }

        // allocates and returns a private subclass that implements the methods
        // This should be used instead of new
        public static idCinematic Alloc() {
            return new idCinematicLocal();
        }

//	// frees all allocated memory
// public	abstract				~idCinematic();
        // returns false if it failed to load
        public boolean InitFromFile(final String qpath, boolean looping) {
            return false;
        }

        // returns the length of the animation in milliseconds
        public int AnimationLength() {
            return 0;
        }

        // the pointers in cinData_t will remain valid until the next UpdateForTime() call
        public cinData_t ImageForTime(int milliseconds) {
            cinData_t c = new cinData_t();
//	memset( &c, 0, sizeof( c ) );
            return c;
        }

        // closes the file and frees all allocated memory
        public void Close() {
        }

        // closes the file and frees all allocated memory
        public void ResetTime(int time) {
        }

        @Override
        @Deprecated//remove if not used.
        protected abstract idCinematic clone() throws CloneNotSupportedException;

    };

    /*
     ===============================================

     Sound meter.

     ===============================================
     */
    public static class idSndWindow extends idCinematic {

        private boolean showWaveform;
        //
        //

        public idSndWindow() {
            showWaveform = false;
        }
//						~idSndWindow() {}

        private idSndWindow(idSndWindow window) {
            this.showWaveform = window.showWaveform;
        }

        @Override
        public boolean InitFromFile(final String qpath, boolean looping) {
            idStr fname = new idStr(qpath);

            fname.ToLower();
            if (0 == fname.Icmp("waveform")) {
                showWaveform = true;
            } else {
                showWaveform = false;
            }
            return true;
        }

        @Override
        public cinData_t ImageForTime(int milliseconds) {
            return soundSystem.ImageForTime(milliseconds, showWaveform);
        }

        @Override
        public int AnimationLength() {
            return -1;
        }

        @Override
        protected idCinematic clone() throws CloneNotSupportedException {

            return new idSndWindow(this);
        }
    };

    static class idCinematicLocal extends idCinematic {

        private final long[]         mComp   = new long[256];
        //        private byte[][][] qStatus = new byte[2][][];
        private       ByteBuffer[][] qStatus = new ByteBuffer[2][];
        private idStr fileName;
        private int   CIN_WIDTH, CIN_HEIGHT;
        private idFile      iFile;
        private cinStatus_t status;
        private long        tfps;
        private long        RoQPlayed;
        private long        ROQSize;
        private int         RoQFrameSize;
        private long        onQuad;
        private long        numQuads;
        private long        samplesPerLine;
        private int         roq_id;
        private int         screenDelta;
        private ByteBuffer  buf;
        private long samplesPerPixel = 2;// defaults to 2
        private int xSize, ySize, maxSize, minSize;
        private long normalBuffer0;
        private long roq_flags;
        private long roqF0;
        private long roqF1;
        private final long[] t = new long[2];
        private long roqFPS;
        private long drawX, drawY;
        //
        private int        animationLength;
        private int        startTime;
        private float      frameRate;
        //
        private ByteBuffer image;
        //
        private boolean    looping;
        private boolean    dirty;
        private boolean    half;
        private boolean    smoothedDouble;
        private boolean    inMemory;
        //
        //

        public idCinematicLocal() {
            image = null;
            status = FMV_EOF;
            buf = null;
            iFile = null;

            qStatus[0] = new ByteBuffer[32768];// Mem_Alloc(32768);
            qStatus[1] = new ByteBuffer[32768];// Mem_Alloc(32768);
        }

        private idCinematicLocal(idCinematicLocal local) {
            this();

            System.arraycopy(local.mComp, 0, this.mComp, 0, this.mComp.length);
            this.qStatus = local.qStatus;//pointer
            this.fileName = local.fileName;
            this.CIN_WIDTH = local.CIN_WIDTH;
            this.CIN_HEIGHT = local.CIN_HEIGHT;
            this.iFile = local.iFile;//pointer
            this.status = local.status;
            this.tfps = local.tfps;
            this.RoQPlayed = local.RoQPlayed;
            this.ROQSize = local.ROQSize;
            this.RoQFrameSize = local.RoQFrameSize;
            this.onQuad = local.onQuad;
            this.numQuads = local.numQuads;
            this.samplesPerLine = local.samplesPerLine;
            this.roq_id = local.roq_id;
            this.screenDelta = local.screenDelta;
            this.buf = local.buf;//pointer
            samplesPerPixel = local.samplesPerPixel;
            this.xSize = local.xSize;
            this.ySize = local.ySize;
            this.maxSize = local.maxSize;
            this.minSize = local.minSize;
            this.normalBuffer0 = local.normalBuffer0;
            this.roq_flags = local.roq_flags;
            this.roqF0 = local.roqF0;
            this.roqF1 = local.roqF1;
            System.arraycopy(local.t, 0, this.t, 0, this.t.length);
            this.roqFPS = local.roqFPS;
            this.drawX = local.drawX;
            this.drawY = local.drawY;
            this.animationLength = local.animationLength;
            this.startTime = local.startTime;
            this.frameRate = local.frameRate;
            this.image = local.image;//pointer
            this.looping = local.looping;
            this.dirty = local.dirty;
            this.half = local.half;
            this.smoothedDouble = local.smoothedDouble;
            this.inMemory = local.inMemory;
        }

        static int debugInitFromFile = 0;

        @Override
        public boolean InitFromFile(String qpath, boolean amilooping) {
            int RoQID;
            ByteBuffer tempFile;
            debugInitFromFile++;

            Close();

            inMemory = false;
            animationLength = 100000;

            if (!qpath.contains("/") && !qpath.contains("\\")) {
                fileName = new idStr(String.format("video/%s", qpath));
            } else {
                fileName = new idStr(String.format("%s", qpath));
            }

            iFile = fileSystem.OpenFileRead(fileName.toString());

            if (null == iFile) {
                return false;
            }

            ROQSize = iFile.Length();

            looping = amilooping;

            CIN_HEIGHT = DEFAULT_CIN_HEIGHT;
            CIN_WIDTH = DEFAULT_CIN_WIDTH;
            samplesPerPixel = 4;
            startTime = 0;    //Sys_Milliseconds();
            buf = null;

            tempFile = ByteBuffer.allocate(file.length);
            iFile.Read(tempFile, 16);
            file = expandBuffer(tempFile);

            RoQID = file[0] + (file[1] << 8);

            frameRate = file[6];
            if (frameRate == 32.0f) {
                frameRate = 1000.0f / 32.0f;
            }

            if (RoQID == ROQ_FILE) {
                RoQ_init();
                status = FMV_PLAY;
                ImageForTime(0);
                status = (looping) ? FMV_PLAY : FMV_IDLE;
                return true;
            }

            RoQShutdown();
            return false;
        }

        private static int debugImageForTime = 0;

        @Override
        public cinData_t ImageForTime(int thisTime) {
            debugImageForTime++;
            cinData_t cinData;

            if (thisTime < 0) {
                thisTime = 0;
            }

            cinData = new cinData_t();//memset( &cinData, 0, sizeof(cinData) );
            if (r_skipROQ.GetBool()) {
                return cinData;
            }

            if (status == FMV_EOF || status == FMV_IDLE) {
                return cinData;
            }

            if (null == buf || startTime == -1) {
                if (startTime == -1) {
                    RoQReset();
                }
                startTime = thisTime;
            }

            tfps = (long) (((thisTime - startTime) * frameRate) / 1000);

            if (tfps < 0) {
                tfps = 0;
            }

            if (tfps < numQuads) {
                RoQReset();
                buf = null;
                status = FMV_PLAY;
            }

            if (null == buf) {
                while (null == buf) {
                    RoQInterrupt();
                }
            } else {
                while ((tfps != numQuads && status == FMV_PLAY)) {
                    RoQInterrupt();
                }
            }

            if (status == FMV_LOOPED) {
                status = FMV_PLAY;
                while (null == buf && status == FMV_PLAY) {
                    RoQInterrupt();
                }
                startTime = thisTime;
            }

            if (status == FMV_EOF) {
                if (looping) {
                    RoQReset();
                    buf = null;
                    if (status == FMV_LOOPED) {
                        status = FMV_PLAY;
                    }
                    while (null == buf && status == FMV_PLAY) {
                        RoQInterrupt();
                    }
                    startTime = thisTime;
                } else {
                    status = FMV_IDLE;
                    RoQShutdown();
                }
            }

            cinData.imageWidth = CIN_WIDTH;
            cinData.imageHeight = CIN_HEIGHT;
            cinData.status = status;
            cinData.image = wrapToNativeBuffer(buf.slice().array());
//            if (tr_render.variable >= 189) {
//            flushBufferToDisk(buf);
//            }

            return cinData;
        }

        @Override
        public int AnimationLength() {
            return animationLength;
        }

        @Override
        public void Close() {
            if (image != null) {
//                Mem_Free(image);
                image = null;
                buf = null;
                status = FMV_EOF;
            }
            RoQShutdown();
        }

        @Override
        public void ResetTime(int time) {
            startTime = (int) ((backEnd.viewDef != null) ? 1000 * backEnd.viewDef.floatTime : -1);
            status = FMV_PLAY;
        }

        private void RoQ_init() {

            RoQPlayed = 24;

            /*	get frame rate */
            roqFPS = file[6] + file[7] * 256;

            if (0 == roqFPS) {
                roqFPS = 30;
            }

            numQuads = -1;

            roq_id = file[8] + file[9] * 256;
            RoQFrameSize = file[10] + file[11] * 256 + file[12] * 65536;
            roq_flags = file[14] + file[15] * 256;
        }

        private void blitVQQuad32fs(ByteBuffer[] status, int[] data) {
            blitVQQuad32fs(status, data, 0);
        }

        private void blitVQQuad32fs(ByteBuffer[] status, int[] data, final int offset) {
            short newd;
            int celdata, code;
            int index, i;
            int d_index;

            newd = 0;
            celdata = 0;
            index = 0;
            d_index = 0;

            do {
                if (0 == newd) {
                    newd = 7;
                    celdata = data[offset + d_index + 0]
                            + (data[offset + d_index + 1] << 8);
                    d_index += 2;
                } else {
                    newd--;
                }

                code = celdata & 0xc000;
                celdata <<= 2;

                switch (code) {
                    case 0x8000:			// vq code
                        blit8_32(vqPoint(vq8, data[offset + d_index] * 128), status[index], samplesPerLine);
                        d_index++;
                        index += 5;
                        break;
                    case 0xc000:			// drop
                        index++;			// skip 8x8
                        for (i = 0; i < 4; i++) {
                            if (0 == newd) {
                                newd = 7;
                                celdata = data[offset + d_index + 0] + data[offset + d_index + 1] * 256;
                                d_index += 2;
                            } else {
                                newd--;
                            }

                            code = (celdata & 0xc000);
                            celdata <<= 2;

                            switch (code) {		// code in top two bits of code
                                case 0x8000:		// 4x4 vq code
                                    blit4_32(vqPoint(vq4, data[offset + d_index] * 32), status[index], samplesPerLine);
                                    d_index++;
                                    break;
                                case 0xc000:		// 2x2 vq code
                                    blit2_32(vqPoint(vq2, data[offset + d_index] * 8), status[index], samplesPerLine);
                                    d_index++;
                                    blit2_32(vqPoint(vq2, data[offset + d_index] * 8), point(status[index], 8), samplesPerLine);
                                    d_index++;
                                    blit2_32(vqPoint(vq2, data[offset + d_index] * 8), point(status[index], samplesPerLine * 2), samplesPerLine);
                                    d_index++;
                                    blit2_32(vqPoint(vq2, data[offset + d_index] * 8), point(status[index], samplesPerLine * 2 + 8), samplesPerLine);
                                    d_index++;
                                    break;
                                case 0x4000:		// motion compensation
                                    move4_32(point(status[index], mComp[data[offset + d_index]]), status[index], samplesPerLine);
                                    d_index++;
                                    break;
                            }
                            index++;
                        }
                        break;
                    case 0x4000:			// motion compensation
                        move8_32(point(status[index], mComp[data[offset + d_index]]), status[index], samplesPerLine);
                        d_index++;
                        index += 5;
                        break;
                    case 0x0000:
                        index += 5;
                        break;
                }
            } while (status[index] != null);
        }

        private void RoQShutdown() {
            if (status == FMV_IDLE) {
                return;
            }
            status = FMV_IDLE;

            if (iFile != null) {
                fileSystem.CloseFile(iFile);
                iFile = null;
            }

            fileName = new idStr("");
        }

        static int debugRoQInterrupt = 0;

        private void RoQInterrupt() {
            int framedata;
            ByteBuffer tempFile;
            boolean redump;

            tempFile = ByteBuffer.allocate(file.length);
            iFile.Read(tempFile, RoQFrameSize + 8);
            file = expandBuffer(tempFile);
            if (RoQPlayed >= ROQSize) {
                if (looping) {
                    RoQReset();
                } else {
                    status = FMV_EOF;
                }
                return;
            }

            framedata = 0;//file;
//
// new frame is ready
//
            do {
                redump = false;
                switch (roq_id) {
                    case ROQ_QUAD_VQ:
                        if ((numQuads & 1) == 1) {
                            normalBuffer0 = t[1];
                            RoQPrepMcomp(roqF0, roqF1);
                            blitVQQuad32fs(qStatus[1], file, framedata);
                            buf = point(image, screenDelta);
                        } else {
                            normalBuffer0 = t[0];
                            RoQPrepMcomp(roqF0, roqF1);
                            blitVQQuad32fs(qStatus[0], file, framedata);
                            buf = image;
                        }
                        if (numQuads == 0) {		// first frame
//				memcpy(image+screenDelta, image, samplesPerLine*ysize);
                            System.arraycopy(image.array(), 0, image.array(), (int) screenDelta, (int) samplesPerLine * ySize);
                        }
                        numQuads++;
                        dirty = true;
                        break;
                    case ROQ_CODEBOOK:
                        debugRoQInterrupt++;
                        decodeCodeBook(file, framedata, roq_flags);
                        break;
                    case ZA_SOUND_MONO:
                        break;
                    case ZA_SOUND_STEREO:
                        break;
                    case ROQ_QUAD_INFO:
                        if (numQuads == -1) {
                            readQuadInfo(file, framedata);
                            setupQuad(0, 0);
                        }
                        if (numQuads != 1) {
                            numQuads = 0;
                        }
                        break;
                    case ROQ_PACKET:
                        inMemory = (roq_flags != 0);
                        RoQFrameSize = 0;           // for header
                        break;
                    case ROQ_QUAD_HANG:
                        RoQFrameSize = 0;
                        break;
                    case ROQ_QUAD_JPEG:
                        if (0 == numQuads) {
                            normalBuffer0 = t[0];
                            JPEGBlit(image, file, framedata, RoQFrameSize);
//				memcpy(image+screenDelta, image, samplesPerLine*ysize);
                            System.arraycopy(image, 0, image, (int) screenDelta, (int) samplesPerLine * ySize);
                            numQuads++;
                        }
                        break;
                    default:
                        status = FMV_EOF;
                        break;
                }
//
// read in next frame data
//
                if (RoQPlayed >= ROQSize) {
                    if (looping) {
                        RoQReset();
                    } else {
                        status = FMV_EOF;
                    }
                    return;
                }

                framedata += RoQFrameSize;
                roq_id = file[framedata + 0] + file[framedata + 1] * 256;
                RoQFrameSize = file[framedata + 2] + file[framedata + 3] * 256 + file[framedata + 4] * 65536;
                roq_flags = file[framedata + 6] + file[framedata + 7] * 256;
                roqF0 = (byte) file[framedata + 7];
                roqF1 = (byte) file[framedata + 6];
//                System.out.printf("roq_id=%d, roqF0=%d, roqF1=%d\n", roq_id, roqF0, roqF1);

                if (RoQFrameSize > 65536 || roq_id == 0x1084) {
                    common.DPrintf("roq_size>65536||roq_id==0x1084\n");
                    status = FMV_EOF;
                    if (looping) {
                        RoQReset();
                    }
                    return;
                }
                if (inMemory && (status != FMV_EOF)) {
                    inMemory = false;
                    framedata += 8;
                    redump = true;//goto redump; 
                }
            } while (redump);//{ 

//
// one more frame hits the dust
//
//	assert(RoQFrameSize <= 65536);
//	r = Sys_StreamedRead( file, RoQFrameSize+8, 1, iFile );
            RoQPlayed += RoQFrameSize + 8;
        }

        private void move8_32(ByteBuffer src, ByteBuffer dst, long spl) {
//            if (true) {
            IntBuffer dsrc, ddst;
            final int dspl;

            dsrc = src.asIntBuffer();
            ddst = dst.asIntBuffer();
            dspl = (int) spl >> 2;

            ddst.put(0 * dspl + 0, dsrc.get(0 * dspl + 0));
            ddst.put(0 * dspl + 1, dsrc.get(0 * dspl + 1));
            ddst.put(0 * dspl + 2, dsrc.get(0 * dspl + 2));
            ddst.put(0 * dspl + 3, dsrc.get(0 * dspl + 3));
            ddst.put(0 * dspl + 4, dsrc.get(0 * dspl + 4));
            ddst.put(0 * dspl + 5, dsrc.get(0 * dspl + 5));
            ddst.put(0 * dspl + 6, dsrc.get(0 * dspl + 6));
            ddst.put(0 * dspl + 7, dsrc.get(0 * dspl + 7));

            ddst.put(1 * dspl + 0, dsrc.get(1 * dspl + 0));
            ddst.put(1 * dspl + 1, dsrc.get(1 * dspl + 1));
            ddst.put(1 * dspl + 2, dsrc.get(1 * dspl + 2));
            ddst.put(1 * dspl + 3, dsrc.get(1 * dspl + 3));
            ddst.put(1 * dspl + 4, dsrc.get(1 * dspl + 4));
            ddst.put(1 * dspl + 5, dsrc.get(1 * dspl + 5));
            ddst.put(1 * dspl + 6, dsrc.get(1 * dspl + 6));
            ddst.put(1 * dspl + 7, dsrc.get(1 * dspl + 7));

            ddst.put(2 * dspl + 0, dsrc.get(2 * dspl + 0));
            ddst.put(2 * dspl + 1, dsrc.get(2 * dspl + 1));
            ddst.put(2 * dspl + 2, dsrc.get(2 * dspl + 2));
            ddst.put(2 * dspl + 3, dsrc.get(2 * dspl + 3));
            ddst.put(2 * dspl + 4, dsrc.get(2 * dspl + 4));
            ddst.put(2 * dspl + 5, dsrc.get(2 * dspl + 5));
            ddst.put(2 * dspl + 6, dsrc.get(2 * dspl + 6));
            ddst.put(2 * dspl + 7, dsrc.get(2 * dspl + 7));

            ddst.put(3 * dspl + 0, dsrc.get(3 * dspl + 0));
            ddst.put(3 * dspl + 1, dsrc.get(3 * dspl + 1));
            ddst.put(3 * dspl + 2, dsrc.get(3 * dspl + 2));
            ddst.put(3 * dspl + 3, dsrc.get(3 * dspl + 3));
            ddst.put(3 * dspl + 4, dsrc.get(3 * dspl + 4));
            ddst.put(3 * dspl + 5, dsrc.get(3 * dspl + 5));
            ddst.put(3 * dspl + 6, dsrc.get(3 * dspl + 6));
            ddst.put(3 * dspl + 7, dsrc.get(3 * dspl + 7));

            ddst.put(4 * dspl + 0, dsrc.get(4 * dspl + 0));
            ddst.put(4 * dspl + 1, dsrc.get(4 * dspl + 1));
            ddst.put(4 * dspl + 2, dsrc.get(4 * dspl + 2));
            ddst.put(4 * dspl + 3, dsrc.get(4 * dspl + 3));
            ddst.put(4 * dspl + 4, dsrc.get(4 * dspl + 4));
            ddst.put(4 * dspl + 5, dsrc.get(4 * dspl + 5));
            ddst.put(4 * dspl + 6, dsrc.get(4 * dspl + 6));
            ddst.put(4 * dspl + 7, dsrc.get(4 * dspl + 7));

            ddst.put(5 * dspl + 0, dsrc.get(5 * dspl + 0));
            ddst.put(5 * dspl + 1, dsrc.get(5 * dspl + 1));
            ddst.put(5 * dspl + 2, dsrc.get(5 * dspl + 2));
            ddst.put(5 * dspl + 3, dsrc.get(5 * dspl + 3));
            ddst.put(5 * dspl + 4, dsrc.get(5 * dspl + 4));
            ddst.put(5 * dspl + 5, dsrc.get(5 * dspl + 5));
            ddst.put(5 * dspl + 6, dsrc.get(5 * dspl + 6));
            ddst.put(5 * dspl + 7, dsrc.get(5 * dspl + 7));

            ddst.put(6 * dspl + 0, dsrc.get(6 * dspl + 0));
            ddst.put(6 * dspl + 1, dsrc.get(6 * dspl + 1));
            ddst.put(6 * dspl + 2, dsrc.get(6 * dspl + 2));
            ddst.put(6 * dspl + 3, dsrc.get(6 * dspl + 3));
            ddst.put(6 * dspl + 4, dsrc.get(6 * dspl + 4));
            ddst.put(6 * dspl + 5, dsrc.get(6 * dspl + 5));
            ddst.put(6 * dspl + 6, dsrc.get(6 * dspl + 6));
            ddst.put(6 * dspl + 7, dsrc.get(6 * dspl + 7));

            ddst.put(7 * dspl + 0, dsrc.get(7 * dspl + 0));
            ddst.put(7 * dspl + 1, dsrc.get(7 * dspl + 1));
            ddst.put(7 * dspl + 2, dsrc.get(7 * dspl + 2));
            ddst.put(7 * dspl + 3, dsrc.get(7 * dspl + 3));
            ddst.put(7 * dspl + 4, dsrc.get(7 * dspl + 4));
            ddst.put(7 * dspl + 5, dsrc.get(7 * dspl + 5));
            ddst.put(7 * dspl + 6, dsrc.get(7 * dspl + 6));
            ddst.put(7 * dspl + 7, dsrc.get(7 * dspl + 7));
//}else{
//	// double *dsrc, *ddst;
//	int dspl;
//
//	// dsrc = (double *)src;
//	// ddst = (double *)dst;
//	dspl = spl>>3;
//
//	dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
//            }
        }

        private void move4_32(ByteBuffer src, ByteBuffer dst, long spl) {
//            if (true) {
            IntBuffer dsrc, ddst;
            final int dspl;

            dsrc = src.asIntBuffer();
            ddst = dst.asIntBuffer();
            dspl = (int) spl >> 2;

            ddst.put(0 * dspl + 0, dsrc.get(0 * dspl + 0));
            ddst.put(0 * dspl + 1, dsrc.get(0 * dspl + 1));
            ddst.put(0 * dspl + 2, dsrc.get(0 * dspl + 2));
            ddst.put(0 * dspl + 3, dsrc.get(0 * dspl + 3));

            ddst.put(1 * dspl + 0, dsrc.get(1 * dspl + 0));
            ddst.put(1 * dspl + 1, dsrc.get(1 * dspl + 1));
            ddst.put(1 * dspl + 2, dsrc.get(1 * dspl + 2));
            ddst.put(1 * dspl + 3, dsrc.get(1 * dspl + 3));

            ddst.put(2 * dspl + 0, dsrc.get(2 * dspl + 0));
            ddst.put(2 * dspl + 1, dsrc.get(2 * dspl + 1));
            ddst.put(2 * dspl + 2, dsrc.get(2 * dspl + 2));
            ddst.put(2 * dspl + 3, dsrc.get(2 * dspl + 3));

            ddst.put(3 * dspl + 0, dsrc.get(3 * dspl + 0));
            ddst.put(3 * dspl + 1, dsrc.get(3 * dspl + 1));
            ddst.put(3 * dspl + 2, dsrc.get(3 * dspl + 2));
            ddst.put(3 * dspl + 3, dsrc.get(3 * dspl + 3));
//}else{
//	// double *dsrc, *ddst;
//	int dspl;
//
//	// dsrc = (double *)src;
//	// ddst = (double *)dst;
//	dspl = spl>>3;
//
//	dst[0] = src[0]; dst[1] = src[1];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1];
//	src += dspl; dst += dspl;
//	dst[0] = src[0]; dst[1] = src[1];
//            }
        }

        private void blit8_32(ByteBuffer src, ByteBuffer dst, long spl) {
//            if (true) {
            IntBuffer dsrc, ddst;
            final int dspl;

            dsrc = src.asIntBuffer();
            ddst = dst.asIntBuffer();
            dspl = (int) spl >> 2;

            ddst.put(0 * dspl + 0, dsrc.get());
            ddst.put(0 * dspl + 1, dsrc.get());
            ddst.put(0 * dspl + 2, dsrc.get());
            ddst.put(0 * dspl + 3, dsrc.get());
            ddst.put(0 * dspl + 4, dsrc.get());
            ddst.put(0 * dspl + 5, dsrc.get());
            ddst.put(0 * dspl + 6, dsrc.get());
            ddst.put(0 * dspl + 7, dsrc.get());

            ddst.put(1 * dspl + 0, dsrc.get());
            ddst.put(1 * dspl + 1, dsrc.get());
            ddst.put(1 * dspl + 2, dsrc.get());
            ddst.put(1 * dspl + 3, dsrc.get());
            ddst.put(1 * dspl + 4, dsrc.get());
            ddst.put(1 * dspl + 5, dsrc.get());
            ddst.put(1 * dspl + 6, dsrc.get());
            ddst.put(1 * dspl + 7, dsrc.get());

            ddst.put(2 * dspl + 0, dsrc.get());
            ddst.put(2 * dspl + 1, dsrc.get());
            ddst.put(2 * dspl + 2, dsrc.get());
            ddst.put(2 * dspl + 3, dsrc.get());
            ddst.put(2 * dspl + 4, dsrc.get());
            ddst.put(2 * dspl + 5, dsrc.get());
            ddst.put(2 * dspl + 6, dsrc.get());
            ddst.put(2 * dspl + 7, dsrc.get());

            ddst.put(3 * dspl + 0, dsrc.get());
            ddst.put(3 * dspl + 1, dsrc.get());
            ddst.put(3 * dspl + 2, dsrc.get());
            ddst.put(3 * dspl + 3, dsrc.get());
            ddst.put(3 * dspl + 4, dsrc.get());
            ddst.put(3 * dspl + 5, dsrc.get());
            ddst.put(3 * dspl + 6, dsrc.get());
            ddst.put(3 * dspl + 7, dsrc.get());

            ddst.put(4 * dspl + 0, dsrc.get());
            ddst.put(4 * dspl + 1, dsrc.get());
            ddst.put(4 * dspl + 2, dsrc.get());
            ddst.put(4 * dspl + 3, dsrc.get());
            ddst.put(4 * dspl + 4, dsrc.get());
            ddst.put(4 * dspl + 5, dsrc.get());
            ddst.put(4 * dspl + 6, dsrc.get());
            ddst.put(4 * dspl + 7, dsrc.get());

            ddst.put(5 * dspl + 0, dsrc.get());
            ddst.put(5 * dspl + 1, dsrc.get());
            ddst.put(5 * dspl + 2, dsrc.get());
            ddst.put(5 * dspl + 3, dsrc.get());
            ddst.put(5 * dspl + 4, dsrc.get());
            ddst.put(5 * dspl + 5, dsrc.get());
            ddst.put(5 * dspl + 6, dsrc.get());
            ddst.put(5 * dspl + 7, dsrc.get());

            ddst.put(6 * dspl + 0, dsrc.get());
            ddst.put(6 * dspl + 1, dsrc.get());
            ddst.put(6 * dspl + 2, dsrc.get());
            ddst.put(6 * dspl + 3, dsrc.get());
            ddst.put(6 * dspl + 4, dsrc.get());
            ddst.put(6 * dspl + 5, dsrc.get());
            ddst.put(6 * dspl + 6, dsrc.get());
            ddst.put(6 * dspl + 7, dsrc.get());

            ddst.put(7 * dspl + 0, dsrc.get());
            ddst.put(7 * dspl + 1, dsrc.get());
            ddst.put(7 * dspl + 2, dsrc.get());
            ddst.put(7 * dspl + 3, dsrc.get());
            ddst.put(7 * dspl + 4, dsrc.get());
            ddst.put(7 * dspl + 5, dsrc.get());
            ddst.put(7 * dspl + 6, dsrc.get());
            ddst.put(7 * dspl + 7, dsrc.get());
//}else{
//	// double *dsrc, *ddst;
//	int dspl;
//
//	// dsrc = (double *)src;
//	// ddst = (double *)dst;
//	dspl = spl>>3;
//
//	dst[0] = dsrc[0]; dst[1] = dsrc[1]; dst[2] = dsrc[2]; dst[3] = dsrc[3];
//	dsrc += 4; ddst += dspl;
//	dst[0] = dsrc[0]; dst[1] = dsrc[1]; dst[2] = dsrc[2]; dst[3] = dsrc[3];
//	dsrc += 4; ddst += dspl;
//	dst[0] = dsrc[0]; dst[1] = dsrc[1]; dst[2] = dsrc[2]; dst[3] = dsrc[3];
//	dsrc += 4; ddst += dspl;
//	dst[0] = dsrc[0]; dst[1] = dsrc[1]; dst[2] = dsrc[2]; dst[3] = dsrc[3];
//	dsrc += 4; ddst += dspl;
//	dst[0] = dsrc[0]; dst[1] = dsrc[1]; dst[2] = dsrc[2]; dst[3] = dsrc[3];
//	dsrc += 4; ddst += dspl;
//	dst[0] = dsrc[0]; dst[1] = dsrc[1]; dst[2] = dsrc[2]; dst[3] = dsrc[3];
//	dsrc += 4; ddst += dspl;
//	dst[0] = dsrc[0]; dst[1] = dsrc[1]; dst[2] = dsrc[2]; dst[3] = dsrc[3];
//	dsrc += 4; ddst += dspl;
//	dst[0] = dsrc[0]; dst[1] = dsrc[1]; dst[2] = dsrc[2]; dst[3] = dsrc[3];
//            }
        }

        private void blit4_32(ByteBuffer src, ByteBuffer dst, long spl) {
//            if (true) {
            IntBuffer dsrc, ddst;
            final int dspl;

            dsrc = src.asIntBuffer();
            ddst = dst.asIntBuffer();
            dspl = (int) spl >> 2;

            ddst.put(0 * dspl + 0, dsrc.get());
            ddst.put(0 * dspl + 1, dsrc.get());
            ddst.put(0 * dspl + 2, dsrc.get());
            ddst.put(0 * dspl + 3, dsrc.get());
            ddst.put(1 * dspl + 0, dsrc.get());
            ddst.put(1 * dspl + 1, dsrc.get());
            ddst.put(1 * dspl + 2, dsrc.get());
            ddst.put(1 * dspl + 3, dsrc.get());
            ddst.put(2 * dspl + 0, dsrc.get());
            ddst.put(2 * dspl + 1, dsrc.get());
            ddst.put(2 * dspl + 2, dsrc.get());
            ddst.put(2 * dspl + 3, dsrc.get());
            ddst.put(3 * dspl + 0, dsrc.get());
            ddst.put(3 * dspl + 1, dsrc.get());
            ddst.put(3 * dspl + 2, dsrc.get());
            ddst.put(3 * dspl + 3, dsrc.get());
//}else{
//	// double *dsrc, *ddst;
//	int dspl;
//
//	// dsrc = (double *)src;
//	// ddst = (double *)dst;
//	dspl = spl>>3;
//
//	dst[0] = src[0]; dst[1] = src[1];
//	dsrc += 2; ddst += dspl;
//	dst[0] = src[0]; dst[1] = src[1];
//	dsrc += 2; ddst += dspl;
//	dst[0] = src[0]; dst[1] = src[1];
//	dsrc += 2; ddst += dspl;
//	dst[0] = src[0]; dst[1] = src[1];
//            }
        }

        private void blit2_32(ByteBuffer src, ByteBuffer dst, long spl) {
//            if (true) {
            IntBuffer dsrc, ddst;
            final int dspl;

            dsrc = src.asIntBuffer();
            ddst = dst.asIntBuffer();
            dspl = (int) spl >> 2;

            ddst.put(0 * dspl + 0, dsrc.get());
            ddst.put(0 * dspl + 1, dsrc.get());
            ddst.put(1 * dspl + 0, dsrc.get());
            ddst.put(1 * dspl + 1, dsrc.get());
//}else{
//	// double *dsrc, *ddst;
//	int dspl;
//
//	// dsrc = (double *)src;
//	// ddst = (double *)dst;
//	dspl = spl>>3;
//
//	dst[0] = src[0];
//	dst[dspl] = src[1];
//            }
        }
//

        private short yuv_to_rgb(long y, long u, long v) {
            long r, g, b, YY = (ROQ_YY_tab[(int) y]);

            r = (YY + ROQ_VR_tab[(int) v]) >> 9;
            g = (YY + ROQ_UG_tab[(int) u] + ROQ_VG_tab[(int) v]) >> 8;
            b = (YY + ROQ_UB_tab[(int) u]) >> 9;

            if (r < 0) {
                r = 0;
            }
            if (g < 0) {
                g = 0;
            }
            if (b < 0) {
                b = 0;
            }

            if (r > 31) {
                r = 31;
            }
            if (g > 63) {
                g = 63;
            }
            if (b > 31) {
                b = 31;
            }

            return (short) ((r << 11) + (g << 5) + (b));
        }

        private int yuv_to_rgb24(long y, long u, long v) {
            long r, g, b, YY = (long) (ROQ_YY_tab[(int) y]);

            r = (YY + ROQ_VR_tab[(int) v]) >> 6;
            g = (YY + ROQ_UG_tab[(int) u] + ROQ_VG_tab[(int) v]) >> 6;
            b = (YY + ROQ_UB_tab[(int) u]) >> 6;

            if (r < 0) {
                r = 0;
            }
            if (g < 0) {
                g = 0;
            }
            if (b < 0) {
                b = 0;
            }

            if (r > 255) {
                r = 255;
            }
            if (g > 255) {
                g = 255;
            }
            if (b > 255) {
                b = 255;
            }

//            System.out.printf("----- %d, %d, %d, %d, %d, %d, %d, %d\n", LittleLong((r) + (g << 8) + (b << 16)), y, u, v, YY, r, g, b);
            return LittleLong((r) + (g << 8) + (b << 16));
        }

        private void decodeCodeBook(final int[] input, final int offset, long roq_flags) {
            long i, j, two, four;
            ByteBuffer aptr, bptr, cptr, dptr;
            long y0, y1, y2, y3, cr, cb;
            IntBuffer iaptr, ibptr, icptr, idptr;
            int i_ptr;

            if (0 == roq_flags) {
                two = four = 256;
            } else {
                two = roq_flags >> 8;
                if (0 == two) {
                    two = 256;
                }
                four = roq_flags & 0xff;
            }

            four *= 2;

            bptr = vq2.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            i_ptr = offset;

            if (!half) {
                if (!smoothedDouble) {
////////////////////////////////////////////////////////////////////////////////
// normal height
////////////////////////////////////////////////////////////////////////////////
                    if (samplesPerPixel == 2) {
                        for (i = 0; i < two; i++) {
                            y0 = input[i_ptr++];
                            y1 = input[i_ptr++];
                            y2 = input[i_ptr++];
                            y3 = input[i_ptr++];
                            cr = input[i_ptr++];
                            cb = input[i_ptr++];
                            bptr.putShort(yuv_to_rgb(y0, cr, cb));
                            bptr.putShort(yuv_to_rgb(y1, cr, cb));
                            bptr.putShort(yuv_to_rgb(y2, cr, cb));
                            bptr.putShort(yuv_to_rgb(y3, cr, cb));
                        }

                        cptr = vq4.duplicate();
                        dptr = vq8.duplicate();
                        for (i = 0; i < four; i++) {
                            aptr = vqPoint(vq2, input[i_ptr++] * 4);
                            bptr = vqPoint(vq2, input[i_ptr++] * 4);
                            for (j = 0; j < 2; j++) {
                                VQ2TO4(aptr, bptr, cptr, dptr);
                            }
                        }
                    } else if (samplesPerPixel == 4) {
                        ibptr = bptr.asIntBuffer();
                        int x0, x1, x2, x3;
                        for (i = 0; i < two; i++) {
                            y0 = input[i_ptr++];
                            y1 = input[i_ptr++];
                            y2 = input[i_ptr++];
                            y3 = input[i_ptr++];//TODO:beware the signed vs unsigned shit.
                            cr = input[i_ptr++];
                            cb = input[i_ptr++];
                            ibptr.put(x0 = yuv_to_rgb24(y0, cr, cb));
                            ibptr.put(x1 = yuv_to_rgb24(y1, cr, cb));
                            ibptr.put(x2 = yuv_to_rgb24(y2, cr, cb));
                            ibptr.put(x3 = yuv_to_rgb24(y3, cr, cb));
//                            System.out.printf("x:: %d, %d, %d, %d\n", x0, x1, x2, x3);
                        }
                        icptr = vq4.asIntBuffer();
                        idptr = vq8.asIntBuffer();
                        for (i = 0; i < four; i++) {
                            iaptr = (IntBuffer) vq2.asIntBuffer().position(input[i_ptr++] * 4);
                            ibptr = (IntBuffer) vq2.asIntBuffer().position(input[i_ptr++] * 4);
                            for (j = 0; j < 2; j++) {
                                VQ2TO4(iaptr, ibptr, icptr, idptr);
                            }
                        }
                    }
                } else {
////////////////////////////////////////////////////////////////////////////////
// double height, smoothed
////////////////////////////////////////////////////////////////////////////////
                    if (samplesPerPixel == 2) {
                        for (i = 0; i < two; i++) {
                            y0 = input[i_ptr++];
                            y1 = input[i_ptr++];
                            y2 = input[i_ptr++];
                            y3 = input[i_ptr++];
                            cr = input[i_ptr++];
                            cb = input[i_ptr++];
                            bptr.putShort(yuv_to_rgb(y0, cr, cb));
                            bptr.putShort(yuv_to_rgb(y1, cr, cb));
                            bptr.putShort(yuv_to_rgb(((y0 * 3) + y2) / 4, cr, cb));
                            bptr.putShort(yuv_to_rgb(((y1 * 3) + y3) / 4, cr, cb));
                            bptr.putShort(yuv_to_rgb((y0 + (y2 * 3)) / 4, cr, cb));
                            bptr.putShort(yuv_to_rgb((y1 + (y3 * 3)) / 4, cr, cb));
                            bptr.putShort(yuv_to_rgb(y2, cr, cb));
                            bptr.putShort(yuv_to_rgb(y3, cr, cb));
                        }

                        cptr = vq4.duplicate();
                        dptr = vq8.duplicate();
                        for (i = 0; i < four; i++) {
                            aptr = vqPoint(vq2, input[i_ptr++] * 8);
                            bptr = vqPoint(vq2, input[i_ptr++] * 8);
                            for (j = 0; j < 2; j++) {
                                VQ2TO4(aptr, bptr, cptr, dptr);
                                VQ2TO4(aptr, bptr, cptr, dptr);
                            }
                        }
                    } else if (samplesPerPixel == 4) {
                        ibptr = bptr.asIntBuffer();
                        for (i = 0; i < two; i++) {
                            y0 = input[i_ptr++];
                            y1 = input[i_ptr++];
                            y2 = input[i_ptr++];
                            y3 = input[i_ptr++];
                            cr = input[i_ptr++];
                            cb = input[i_ptr++];
                            ibptr.put(yuv_to_rgb24(y0, cr, cb));
                            ibptr.put(yuv_to_rgb24(y1, cr, cb));
                            ibptr.put(yuv_to_rgb24(((y0 * 3) + y2) / 4, cr, cb));
                            ibptr.put(yuv_to_rgb24(((y1 * 3) + y3) / 4, cr, cb));
                            ibptr.put(yuv_to_rgb24((y0 + (y2 * 3)) / 4, cr, cb));
                            ibptr.put(yuv_to_rgb24((y1 + (y3 * 3)) / 4, cr, cb));
                            ibptr.put(yuv_to_rgb24(y2, cr, cb));
                            ibptr.put(yuv_to_rgb24(y3, cr, cb));
                        }

                        icptr = vq4.asIntBuffer();
                        idptr = vq8.asIntBuffer();
                        for (i = 0; i < four; i++) {
                            iaptr = (IntBuffer) vq2.asIntBuffer().position(input[i_ptr++] * 8);
                            ibptr = (IntBuffer) vq2.asIntBuffer().position(input[i_ptr++] * 8);
                            for (j = 0; j < 2; j++) {
                                VQ2TO4(iaptr, ibptr, icptr, idptr);
                                VQ2TO4(iaptr, ibptr, icptr, idptr);
                            }
                        }
                    }
                }
            } else {
////////////////////////////////////////////////////////////////////////////////
// 1/4 screen
////////////////////////////////////////////////////////////////////////////////
                if (samplesPerPixel == 2) {
                    for (i = 0; i < two; i++) {
                        y0 = input[i_ptr];
                        i_ptr += 2;
                        y2 = input[i_ptr];
                        i_ptr += 2;
                        cr = input[i_ptr++];
                        cb = input[i_ptr++];
                        bptr.putShort(yuv_to_rgb(y0, cr, cb));
                        bptr.putShort(yuv_to_rgb(y2, cr, cb));
                    }

                    cptr = vq4.duplicate();
                    dptr = vq8.duplicate();
                    for (i = 0; i < four; i++) {
                        aptr = vqPoint(vq2, input[i_ptr++] * 2);
                        bptr = vqPoint(vq2, input[i_ptr++] * 2);
                        for (j = 0; j < 2; j++) {
                            VQ2TO2(aptr, bptr, cptr, dptr);
                        }
                    }
                } else if (samplesPerPixel == 4) {
                    ibptr = bptr.asIntBuffer();
                    for (i = 0; i < two; i++) {
                        y0 = input[i_ptr];
                        i_ptr += 2;
                        y2 = input[i_ptr];
                        i_ptr += 2;
                        cr = input[i_ptr++];
                        cb = input[i_ptr++];
                        ibptr.put(yuv_to_rgb24(y0, cr, cb));
                        ibptr.put(yuv_to_rgb24(y2, cr, cb));
                    }

                    icptr = vq4.asIntBuffer();
                    idptr = vq8.asIntBuffer();
                    for (i = 0; i < four; i++) {
                        iaptr = (IntBuffer) vq2.asIntBuffer().position(input[i_ptr++] * 2);
                        ibptr = (IntBuffer) vq2.asIntBuffer().position(input[i_ptr++] * 2);
                        for (j = 0; j < 2; j++) {
                            VQ2TO2(iaptr, ibptr, icptr, idptr);
                        }
                    }
                }
            }
        }

        private void recurseQuad(long startX, long startY, long quadSize, long xOff, long yOff) {
            ByteBuffer scrOff;
            long bigX, bigY, lowX, lowY, useY;
            int offset;

            offset = screenDelta;

            lowX = lowY = 0;
            bigX = xSize;
            bigY = ySize;

            if (bigX > CIN_WIDTH) {
                bigX = CIN_WIDTH;
            }
            if (bigY > CIN_HEIGHT) {
                bigY = CIN_HEIGHT;
            }

            if ((startX >= lowX) && (startX + quadSize <= bigX) && (startY + quadSize <= bigY) && (startY >= lowY) && quadSize <= MAXSIZE) {
                useY = startY;
                final int offering = (int) ((useY + ((CIN_HEIGHT - bigY) >> 1) + yOff) * samplesPerLine + ((startX + xOff) * samplesPerPixel));
                scrOff = point(image, offering);

                qStatus[0][(int) onQuad] = scrOff;
                qStatus[1][(int) onQuad++] = point(scrOff, offset);
            }

            if (quadSize != MINSIZE) {
                quadSize >>= 1;
                recurseQuad(startX, startY, quadSize, xOff, yOff);
                recurseQuad(startX + quadSize, startY, quadSize, xOff, yOff);
                recurseQuad(startX, startY + quadSize, quadSize, xOff, yOff);
                recurseQuad(startX + quadSize, startY + quadSize, quadSize, xOff, yOff);
            }
        }

        private void setupQuad(long xOff, long yOff) {
            long numQuadCels, x, y;
            int i;
            ByteBuffer temp;

            numQuadCels = (CIN_WIDTH * CIN_HEIGHT) / 16;
            numQuadCels += numQuadCels / 4 + numQuadCels / 16;
            numQuadCels += 64;				// for overflow

            numQuadCels = (xSize * ySize) / 16;
            numQuadCels += numQuadCels / 4;
            numQuadCels += 64;				// for overflow

            onQuad = 0;

            for (y = 0; y < (long) ySize; y += 16) {
                for (x = 0; x < (long) xSize; x += 16) {
                    recurseQuad(x, y, 16, xOff, yOff);
                }
            }

            temp = null;
            for (i = (int) (numQuadCels - 64); i < numQuadCels; i++) {
                qStatus[0][i] = //temp;			// eoq
                        qStatus[1][i] = temp;           // eoq
            }
        }

        private void readQuadInfo(int[] qData, int offset) {
            xSize = qData[offset + 0] + qData[offset + 1] * 256;
            ySize = qData[offset + 2] + qData[offset + 3] * 256;
            maxSize = qData[offset + 4] + qData[offset + 5] * 256;
            minSize = qData[offset + 6] + qData[offset + 7] * 256;

            CIN_HEIGHT = ySize;
            CIN_WIDTH = xSize;

            samplesPerLine = CIN_WIDTH * samplesPerPixel;
            screenDelta = (int) (CIN_HEIGHT * samplesPerLine);

            if (NOT(image)) {
                image = ByteBuffer.allocate((int) (CIN_WIDTH * CIN_HEIGHT * samplesPerPixel * 2)).order(ByteOrder.LITTLE_ENDIAN);//Mem_Alloc((int) (CIN_WIDTH * CIN_HEIGHT * samplesPerPixel * 2));
            }

            half = false;
            smoothedDouble = false;

            t[0] = screenDelta;//t[0] = (0 - (unsigned int)image)+(unsigned int)image+screenDelta;
            t[1] = -screenDelta;//t[1] = (0 - ((unsigned int)image + screenDelta))+(unsigned int)image;

            drawX = CIN_WIDTH;
            drawY = CIN_HEIGHT;
        }

        private void RoQPrepMcomp(long xOff, long yOff) {
            int x, y;
            long i, j, temp, temp2;

            i = samplesPerLine;
            j = samplesPerPixel;
            if (xSize == (ySize * 4) && !half) {
                j = j + j;
                i = i + i;
            }

            for (y = 0; y < 16; y++) {
                temp2 = (y + yOff - 8) * i;
                for (x = 0; x < 16; x++) {
                    temp = (x + xOff - 8) * j;
                    mComp[(x * 16) + y] = (normalBuffer0 - (temp2 + temp)) & 0xFFFF_FFFFL;
                }
            }
        }

        private void RoQReset() {
            ByteBuffer tempFile;

            tempFile = ByteBuffer.allocate(file.length);
            iFile.Seek(0, FS_SEEK_SET);
            iFile.Read(tempFile, 16);
            file = expandBuffer(tempFile);
            RoQ_init();
            status = FMV_LOOPED;
        }

        @Override
        protected idCinematic clone() throws CloneNotSupportedException {
            return new idCinematicLocal(this);
        }

    };

    private static void VQ2TO4(ByteBuffer a, ByteBuffer b, ByteBuffer c, ByteBuffer d) {
        final int aPos = a.position();
        final int bPos = b.position();

        c.putShort(a.getShort(aPos + 0));
        c.putShort(a.getShort(aPos + 2));
        c.putShort(b.getShort(bPos + 0));
        c.putShort(b.getShort(bPos + 2));
        d.putShort(a.getShort(aPos + 0));
        d.putShort(a.getShort(aPos + 0));
        d.putShort(a.getShort(aPos + 2));
        d.putShort(a.getShort(aPos + 2));
        d.putShort(b.getShort(bPos + 0));
        d.putShort(b.getShort(bPos + 0));
        d.putShort(b.getShort(bPos + 2));
        d.putShort(b.getShort(bPos + 2));
        d.putShort(a.getShort(aPos + 0));
        d.putShort(a.getShort(aPos + 0));
        d.putShort(a.getShort(aPos + 2));
        d.putShort(a.getShort(aPos + 2));
        d.putShort(b.getShort(bPos + 0));
        d.putShort(b.getShort(bPos + 0));
        d.putShort(b.getShort(bPos + 2));
        d.putShort(b.getShort(bPos + 2));
        a.position(aPos + 4);// += 2;
        b.position(bPos + 4);// += 2;
    }

    private static void VQ2TO4(IntBuffer a, IntBuffer b, IntBuffer c, IntBuffer d) {

        final int aPos = a.position();
        final int bPos = b.position();

        c.put(a.get(aPos + 0));
        c.put(a.get(aPos + 1));
        c.put(b.get(bPos + 0));
        c.put(b.get(bPos + 1));
        d.put(a.get(aPos + 0));
        d.put(a.get(aPos + 0));
        d.put(a.get(aPos + 1));
        d.put(a.get(aPos + 1));
        d.put(b.get(bPos + 0));
        d.put(b.get(bPos + 0));
        d.put(b.get(bPos + 1));
        d.put(b.get(bPos + 1));
        d.put(a.get(aPos + 0));
        d.put(a.get(aPos + 0));
        d.put(a.get(aPos + 1));
        d.put(a.get(aPos + 1));
        d.put(b.get(bPos + 0));
        d.put(b.get(bPos + 0));
        d.put(b.get(bPos + 1));
        d.put(b.get(bPos + 1));
        a.position(aPos + 2);// += 2;
        b.position(bPos + 2);// += 2;
    }

    private static void VQ2TO2(ByteBuffer a, ByteBuffer b, ByteBuffer c, ByteBuffer d) {
        final int aPos = a.position();
        final int bPos = b.position();

        c.putShort(a.getShort(aPos));//TODO:use shortBuffers instead?
        c.putShort(b.getShort(bPos));
        d.putShort(a.getShort(aPos));
        d.putShort(a.getShort(aPos));
        d.putShort(b.getShort(bPos));
        d.putShort(b.getShort(bPos));
        d.putShort(a.getShort(aPos));
        d.putShort(a.getShort(aPos));
        d.putShort(b.getShort(bPos));
        d.putShort(b.getShort(bPos));
        a.position(aPos + 2);//++;
        b.position(bPos + 2);//++;
    }

    private static void VQ2TO2(IntBuffer a, IntBuffer b, IntBuffer c, IntBuffer d) {
        final int aPos = a.position();
        final int bPos = b.position();

        c.put(a.get(aPos));
        c.put(b.get(bPos));
        d.put(a.get(aPos));
        d.put(a.get(aPos));
        d.put(b.get(bPos));
        d.put(b.get(bPos));
        d.put(a.get(aPos));
        d.put(a.get(aPos));
        d.put(b.get(bPos));
        d.put(b.get(bPos));
        a.get();//++;
        b.get();//++;
    }

    private static int JPEGBlit(ByteBuffer wStatus, int[] data, int offset, int datasize) {
        throw new TODO_Exception();
    }

    /**
     * The original file[] was a byte array.
     */
    private static int[] expandBuffer(ByteBuffer tempFile) {
        for (int f = 0; f < file.length; f++) {
            file[f] = tempFile.get(f) & 0xFF;
        }
        return file;
    }

    /**
     * @return A ByteBuffer duplicate of the {@code src} buffer with
     * {@code offset} as start position.
     */
    private static ByteBuffer point(ByteBuffer src, long offset) {
        final int pos = src.position();

        try {
            return ((ByteBuffer) src.duplicate().position((int) (pos + offset))).order(src.order());
        } catch (Exception e) {
            System.err.printf("point---> %d, %d, %d\n", src.capacity(), src.remaining(), offset);
            throw e;
        }
    }

    /**
     * {@code offset} is doubled to account for the short->byte conversion.
     *
     * @see point(ByteBuffer, long)
     */
    private static ByteBuffer vqPoint(ByteBuffer src, long offset) {
        offset = offset * 2;//because we use bytebuffers isntead of short arrays.

        return point(src, offset);
    }

//    private static void flushBufferToDisk(final ByteBuffer buffer) {
//        try {
//            File file = new File("/temp/j" + fileNumber);
//            file.createNewFile();
//            try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
//                fileChannel.write(buffer.slice());
//            }
////            File fileVQ2 = new File("/temp/jvq2" + fileNumber);
////            fileVQ2.createNewFile();
////            try (FileChannel fileChannel = FileChannel.open(fileVQ2.toPath(), StandardOpenOption.WRITE)) {
////                fileChannel.write(vq2.duplicate());
////            }
////            File fileVQ4 = new File("/temp/jvq4" + fileNumber);
////            fileVQ4.createNewFile();
////            try (FileChannel fileChannel = FileChannel.open(fileVQ4.toPath(), StandardOpenOption.WRITE)) {
////                fileChannel.write(vq4.duplicate());
////            }
////            File fileVQ8 = new File("/temp/jvq8" + (fileNumber));
////            fileVQ8.createNewFile();
////            try (FileChannel fileChannel = FileChannel.open(fileVQ8.toPath(), StandardOpenOption.WRITE)) {
////                fileChannel.write(vq8.duplicate());
////            }
//            fileNumber++;
//        } catch (IOException ex) {
//            Logger.getLogger(Cinematic.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//    static int fileNumber = 0;
}
