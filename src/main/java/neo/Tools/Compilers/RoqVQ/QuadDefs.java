package neo.Tools.Compilers.RoqVQ;

/**
 *
 */
public class QuadDefs {

    static final int DEP               = 0;
    static final int FCC               = 1;
    static final int CCC               = 2;
    static final int SLD               = 3;
    static final int PAT               = 4;
    static final int MOT               = 5;
    static final int DEAD              = 6;
    //                     
    static final int COLA              = 0;
    static final int COLB              = 1;
    static final int COLC              = 2;
    static final int COLS              = 3;
    static final int COLPATA           = 4;
    static final int COLPATB           = 5;
    static final int COLPATS           = 6;
    static final int GENERATION        = 7;
    //
    static final int CCCBITMAP         = 0;
    static final int FCCDOMAIN         = 1;
    static final int PATNUMBER         = 2;
    static final int PATNUMBE2         = 3;
    static final int PATNUMBE3         = 4;
    static final int PATNUMBE4         = 5;
    static final int PATNUMBE5         = 6;
    //
    static final int MAXSIZE           = 16;
    static final int MINSIZE           = 4;
    //
    static final int RoQ_ID            = 0x1084;
    static final int RoQ_QUAD          = 0x1000;
    static final int RoQ_PUZZLE_QUAD   = 0x1003;
    static final int RoQ_QUAD_HANG     = 0x1013;
    static final int RoQ_QUAD_SMALL    = 0x1010;
    static final int RoQ_QUAD_INFO     = 0x1001;
    static final int RoQ_QUAD_VQ       = 0x1011;
    static final int RoQ_QUAD_JPEG     = 0x1012;
    static final int RoQ_QUAD_CODEBOOK = 0x1002;

    static class shortQuadCel {

        byte size;                                      //  32, 16, 8, or 4
        int/*word*/ xat;                // where is it at on the screen
        int/*word*/ yat;				// 
    }

    static class quadcel {

        byte size;                                      //  32, 16, 8, or 4
        int/*word*/ xat;                // where is it at on the screen
        int/*word*/ yat;                //
        //
        float cccsnr;                                   // ccc bitmap snr to actual image
        float fccsnr;                                   // fcc bitmap snr to actual image
        float motsnr;                                   // delta snr to previous image
        float sldsnr;                                   // solid color snr
        float patsnr;
        float dctsnr;
        float rsnr;                                     // what's the current snr
        //
        long/*unsigned int*/ cola;            // color a for ccc
        long/*unsigned int*/ colb;            // color b for ccc
        long/*unsigned int*/ colc;            // color b for ccc
        long/*unsigned int*/ sldcol;            // sold color
        long/*unsigned int*/ colpata;
        long/*unsigned int*/ colpatb;
        long/*unsigned int*/ colpats;
        long/*unsigned int*/ bitmap;            // ccc bitmap
        //
        int/*word*/          domain;                // where to copy from for fcc
        int/*word*/[] patten = new int[5];        // which pattern
        //
        int     status;
        boolean mark;
        float[] snr = new float[DEAD + 1];        // snrssss
    }

    static class dataQuadCel {

        float[]                snr     = new float[DEAD + 1];        // snrssss
        long/*unsigned int*/[] cols    = new long[8];
        long/*unsigned int*/[] bitmaps = new long[7];    // ccc bitmap
    }

    static class norm {

        float normal;
        /*unsigned short*/ int index;
    }

    static class dtlCel {
        /*unsigned*/ char[] dtlMap = new char[256];
        int[] r = new int[4];
        int[] g = new int[4];
        int[] b = new int[4];
        int[] a = new int[4];
        float ymean;
    }

    static class pPixel {

        byte r, g, b, a;
    }
}
