package neo.Tools.Compilers.RoqVQ;

import static neo.Renderer.Image_files.R_LoadImage;
import static neo.framework.Common.common;

/**
 *
 */
public class NSBitmapImageRep {

    //    static class NSBitmapImageRep {
    private byte[]            bmap;
    private int               width;
    private int               height;
    private long/*ID_TIME_T*/ timestamp;
    //
    //

    public NSBitmapImageRep() {
        bmap = null;
        width = height = 0;
        timestamp = 0;
    }

    public NSBitmapImageRep(final String filename) {

        final int[] w = {0}, h = {0};
        final long[] t = {0};

        R_LoadImage(filename, bmap, w, h, t, false);
        width = w[0];
        height = h[0];
        timestamp = t[0];
        if (0 == width || 0 == height) {
            common.FatalError("roqvq: unable to load image %s\n", filename);
        }
    }

    public NSBitmapImageRep(int wide, int high) {
        bmap = new byte[wide * high * 4];// Mem_ClearedAlloc(wide * high * 4);
        width = wide;
        height = high;
    }
    // ~NSBitmapImageRep();

    // NSBitmapImageRep &	operator=( const NSBitmapImageRep &a );
    public NSBitmapImageRep oSet(final NSBitmapImageRep a) {

        // check for assignment to self
        if (this.equals(a)) {
            return this;
        }

        if (bmap != null) {
            bmap = null;//Mem_Free(bmap);
        }
        bmap = new byte[a.width * a.height * 4];// Mem_Alloc(a.width * a.height * 4);
//	memcpy( bmap, a.bmap, a.width * a.height * 4 );
//        System.arraycopy(a.bmap, 0, this.bmap, 0, a.width * a.height * 4);
        this.bmap = a.bmap;
        width = a.width;
        height = a.height;
        timestamp = a.timestamp;

        return this;
    }

    public int samplesPerPixel() {
        return 4;
    }

    public int pixelsWide() {
        return width;
    }

    public int pixelsHigh() {
        return height;
    }

    public byte[] bitmapData() {
        return bmap;
    }

    public boolean hasAlpha() {
        return false;
    }

    public boolean isPlanar() {
        return false;
    }
//    };
}
