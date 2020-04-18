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
        this.bmap = null;
        this.width = this.height = 0;
        this.timestamp = 0;
    }

    public NSBitmapImageRep(final String filename) {

        final int[] w = {0}, h = {0};
        final long[] t = {0};

        R_LoadImage(filename, this.bmap, w, h, t, false);
        this.width = w[0];
        this.height = h[0];
        this.timestamp = t[0];
        if ((0 == this.width) || (0 == this.height)) {
            common.FatalError("roqvq: unable to load image %s\n", filename);
        }
    }

    public NSBitmapImageRep(int wide, int high) {
        this.bmap = new byte[wide * high * 4];// Mem_ClearedAlloc(wide * high * 4);
        this.width = wide;
        this.height = high;
    }
    // ~NSBitmapImageRep();

    // NSBitmapImageRep &	operator=( const NSBitmapImageRep &a );
    public NSBitmapImageRep oSet(final NSBitmapImageRep a) {

        // check for assignment to self
        if (this.equals(a)) {
            return this;
        }

        if (this.bmap != null) {
            this.bmap = null;//Mem_Free(bmap);
        }
        this.bmap = new byte[a.width * a.height * 4];// Mem_Alloc(a.width * a.height * 4);
//	memcpy( bmap, a.bmap, a.width * a.height * 4 );
//        Nio.arraycopy(a.bmap, 0, this.bmap, 0, a.width * a.height * 4);
        this.bmap = a.bmap;
        this.width = a.width;
        this.height = a.height;
        this.timestamp = a.timestamp;

        return this;
    }

    public int samplesPerPixel() {
        return 4;
    }

    public int pixelsWide() {
        return this.width;
    }

    public int pixelsHigh() {
        return this.height;
    }

    public byte[] bitmapData() {
        return this.bmap;
    }

    public boolean hasAlpha() {
        return false;
    }

    public boolean isPlanar() {
        return false;
    }
//    };
}
