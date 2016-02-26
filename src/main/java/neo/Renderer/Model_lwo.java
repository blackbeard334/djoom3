package neo.Renderer;

import static java.lang.Math.log;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import neo.Renderer.Model_lwo.lwClip;
import neo.Renderer.Model_lwo.lwEnvelope;
import neo.Renderer.Model_lwo.lwGradKey;
import neo.Renderer.Model_lwo.lwKey;
import neo.Renderer.Model_lwo.lwLayer;
import neo.Renderer.Model_lwo.lwNode;
import neo.Renderer.Model_lwo.lwObject;
import neo.Renderer.Model_lwo.lwPlugin;
import neo.Renderer.Model_lwo.lwPoint;
import neo.Renderer.Model_lwo.lwPointList;
import neo.Renderer.Model_lwo.lwPolVert;
import neo.Renderer.Model_lwo.lwPolygon;
import neo.Renderer.Model_lwo.lwPolygonList;
import neo.Renderer.Model_lwo.lwSurface;
import neo.Renderer.Model_lwo.lwTagList;
import neo.Renderer.Model_lwo.lwTexture;
import neo.Renderer.Model_lwo.lwVMap;
import neo.Renderer.Model_lwo.lwVMapPt;
import static neo.TempDump.NOT;
import neo.TempDump.NiLLABLE;
import neo.TempDump.TODO_Exception;
import static neo.TempDump.bbtocb;
import static neo.TempDump.btoi;
import static neo.TempDump.replaceByIndex;
import static neo.TempDump.strLen;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_CUR;
import static neo.framework.File_h.fsOrigin_t.FS_SEEK_SET;
import neo.framework.File_h.idFile;
import static neo.idlib.Lib.BigRevBytes;
import neo.idlib.containers.List.cmp_t;
import static neo.idlib.math.Math_h.FLOAT_IS_DENORMAL;
import neo.idlib.math.Math_h.idMath;

/**
 *
 */
public class Model_lwo {

    /*
     ======================================================================

     LWO2 loader. (LightWave Object)

     Ernie Wright  17 Sep 00

     ======================================================================
     */

    /* chunk and subchunk IDs */
    public static int LWID_(final char a, final char b, final char c, final char d) {
        return ((a << 24) | (b << 16) | (c << 8) | (d << 0));
    }
//    
    public static final int ID_FORM = ('F' << 24 | 'O' << 16 | 'R' << 8 | 'M');
    public static final int ID_LWO2 = ('L' << 24 | 'W' << 16 | 'O' << 8 | '2');
    public static final int ID_LWOB = ('L' << 24 | 'W' << 16 | 'O' << 8 | 'B');
//                                       
    /* top-level chunks */
    public static final int ID_LAYR = ('L' << 24 | 'A' << 16 | 'Y' << 8 | 'R');
    public static final int ID_TAGS = ('T' << 24 | 'A' << 16 | 'G' << 8 | 'S');
    public static final int ID_PNTS = ('P' << 24 | 'N' << 16 | 'T' << 8 | 'S');
    public static final int ID_BBOX = ('B' << 24 | 'B' << 16 | 'O' << 8 | 'X');
    public static final int ID_VMAP = ('V' << 24 | 'M' << 16 | 'A' << 8 | 'P');
    public static final int ID_VMAD = ('V' << 24 | 'M' << 16 | 'A' << 8 | 'D');
    public static final int ID_POLS = ('P' << 24 | 'O' << 16 | 'L' << 8 | 'S');
    public static final int ID_PTAG = ('P' << 24 | 'T' << 16 | 'A' << 8 | 'G');
    public static final int ID_ENVL = ('E' << 24 | 'N' << 16 | 'V' << 8 | 'L');
    public static final int ID_CLIP = ('C' << 24 | 'L' << 16 | 'I' << 8 | 'P');
    public static final int ID_SURF = ('S' << 24 | 'U' << 16 | 'R' << 8 | 'F');
    public static final int ID_DESC = ('D' << 24 | 'E' << 16 | 'S' << 8 | 'C');
    public static final int ID_TEXT = ('T' << 24 | 'E' << 16 | 'X' << 8 | 'T');
    public static final int ID_ICON = ('I' << 24 | 'C' << 16 | 'O' << 8 | 'N');
//                                      
    /* polygon types */
    public static final int ID_FACE = ('F' << 24 | 'A' << 16 | 'C' << 8 | 'E');
    public static final int ID_CURV = ('C' << 24 | 'U' << 16 | 'R' << 8 | 'V');
    public static final int ID_PTCH = ('P' << 24 | 'T' << 16 | 'C' << 8 | 'H');
    public static final int ID_MBAL = ('M' << 24 | 'B' << 16 | 'A' << 8 | 'L');
    public static final int ID_BONE = ('B' << 24 | 'O' << 16 | 'N' << 8 | 'E');
//                                      
    /* polygon tags */
//    public static final int ID_SURF = ('S' << 24 | 'U' << 16 | 'R' << 8 | 'F');
    public static final int ID_PART = ('P' << 24 | 'A' << 16 | 'R' << 8 | 'T');
    public static final int ID_SMGP = ('S' << 24 | 'M' << 16 | 'G' << 8 | 'P');
//                                     
    /* envelopes */
    public static final int ID_PRE = ('P' << 24 | 'R' << 16 | 'E' << 8 | ' ');
    public static final int ID_POST = ('P' << 24 | 'O' << 16 | 'S' << 8 | 'T');
    public static final int ID_KEY = ('K' << 24 | 'E' << 16 | 'Y' << 8 | ' ');
    public static final int ID_SPAN = ('S' << 24 | 'P' << 16 | 'A' << 8 | 'N');
    public static final int ID_TCB = ('T' << 24 | 'C' << 16 | 'B' << 8 | ' ');
    public static final int ID_HERM = ('H' << 24 | 'E' << 16 | 'R' << 8 | 'M');
    public static final int ID_BEZI = ('B' << 24 | 'E' << 16 | 'Z' << 8 | 'I');
    public static final int ID_BEZ2 = ('B' << 24 | 'E' << 16 | 'Z' << 8 | '2');
    public static final int ID_LINE = ('L' << 24 | 'I' << 16 | 'N' << 8 | 'E');
    public static final int ID_STEP = ('S' << 24 | 'T' << 16 | 'E' << 8 | 'P');
//                                     
    /* clips */
    public static final int ID_STIL = ('S' << 24 | 'T' << 16 | 'I' << 8 | 'L');
    public static final int ID_ISEQ = ('I' << 24 | 'S' << 16 | 'E' << 8 | 'Q');
    public static final int ID_ANIM = ('A' << 24 | 'N' << 16 | 'I' << 8 | 'M');
    public static final int ID_XREF = ('X' << 24 | 'R' << 16 | 'E' << 8 | 'F');
    public static final int ID_STCC = ('S' << 24 | 'T' << 16 | 'C' << 8 | 'C');
    public static final int ID_TIME = ('T' << 24 | 'I' << 16 | 'M' << 8 | 'E');
    public static final int ID_CONT = ('C' << 24 | 'O' << 16 | 'N' << 8 | 'T');
    public static final int ID_BRIT = ('B' << 24 | 'R' << 16 | 'I' << 8 | 'T');
    public static final int ID_SATR = ('S' << 24 | 'A' << 16 | 'T' << 8 | 'R');
    public static final int ID_HUE = ('H' << 24 | 'U' << 16 | 'E' << 8 | ' ');
    public static final int ID_GAMM = ('G' << 24 | 'A' << 16 | 'M' << 8 | 'M');
    public static final int ID_NEGA = ('N' << 24 | 'E' << 16 | 'G' << 8 | 'A');
    public static final int ID_IFLT = ('I' << 24 | 'F' << 16 | 'L' << 8 | 'T');
    public static final int ID_PFLT = ('P' << 24 | 'F' << 16 | 'L' << 8 | 'T');
//                                    
    /* surfaces */
    public static final int ID_COLR = ('C' << 24 | 'O' << 16 | 'L' << 8 | 'R');
    public static final int ID_LUMI = ('L' << 24 | 'U' << 16 | 'M' << 8 | 'I');
    public static final int ID_DIFF = ('D' << 24 | 'I' << 16 | 'F' << 8 | 'F');
    public static final int ID_SPEC = ('S' << 24 | 'P' << 16 | 'E' << 8 | 'C');
    public static final int ID_GLOS = ('G' << 24 | 'L' << 16 | 'O' << 8 | 'S');
    public static final int ID_REFL = ('R' << 24 | 'E' << 16 | 'F' << 8 | 'L');
    public static final int ID_RFOP = ('R' << 24 | 'F' << 16 | 'O' << 8 | 'P');
    public static final int ID_RIMG = ('R' << 24 | 'I' << 16 | 'M' << 8 | 'G');
    public static final int ID_RSAN = ('R' << 24 | 'S' << 16 | 'A' << 8 | 'N');
    public static final int ID_TRAN = ('T' << 24 | 'R' << 16 | 'A' << 8 | 'N');
    public static final int ID_TROP = ('T' << 24 | 'R' << 16 | 'O' << 8 | 'P');
    public static final int ID_TIMG = ('T' << 24 | 'I' << 16 | 'M' << 8 | 'G');
    public static final int ID_RIND = ('R' << 24 | 'I' << 16 | 'N' << 8 | 'D');
    public static final int ID_TRNL = ('T' << 24 | 'R' << 16 | 'N' << 8 | 'L');
    public static final int ID_BUMP = ('B' << 24 | 'U' << 16 | 'M' << 8 | 'P');
    public static final int ID_SMAN = ('S' << 24 | 'M' << 16 | 'A' << 8 | 'N');
    public static final int ID_SIDE = ('S' << 24 | 'I' << 16 | 'D' << 8 | 'E');
    public static final int ID_CLRH = ('C' << 24 | 'L' << 16 | 'R' << 8 | 'H');
    public static final int ID_CLRF = ('C' << 24 | 'L' << 16 | 'R' << 8 | 'F');
    public static final int ID_ADTR = ('A' << 24 | 'D' << 16 | 'T' << 8 | 'R');
    public static final int ID_SHRP = ('S' << 24 | 'H' << 16 | 'R' << 8 | 'P');
//    public static final int ID_LINE = ('L' << 24 | 'I' << 16 | 'N' << 8 | 'E');
    public static final int ID_LSIZ = ('L' << 24 | 'S' << 16 | 'I' << 8 | 'Z');
    public static final int ID_ALPH = ('A' << 24 | 'L' << 16 | 'P' << 8 | 'H');
    public static final int ID_AVAL = ('A' << 24 | 'V' << 16 | 'A' << 8 | 'L');
    public static final int ID_GVAL = ('G' << 24 | 'V' << 16 | 'A' << 8 | 'L');
    public static final int ID_BLOK = ('B' << 24 | 'L' << 16 | 'O' << 8 | 'K');
//                                      
/* texture layer */
    public static final int ID_TYPE = ('T' << 24 | 'Y' << 16 | 'P' << 8 | 'E');
    public static final int ID_CHAN = ('C' << 24 | 'H' << 16 | 'A' << 8 | 'N');
    public static final int ID_NAME = ('N' << 24 | 'A' << 16 | 'M' << 8 | 'E');
    public static final int ID_ENAB = ('E' << 24 | 'N' << 16 | 'A' << 8 | 'B');
    public static final int ID_OPAC = ('O' << 24 | 'P' << 16 | 'A' << 8 | 'C');
    public static final int ID_FLAG = ('F' << 24 | 'L' << 16 | 'A' << 8 | 'G');
    public static final int ID_PROJ = ('P' << 24 | 'R' << 16 | 'O' << 8 | 'J');
    public static final int ID_STCK = ('S' << 24 | 'T' << 16 | 'C' << 8 | 'K');
    public static final int ID_TAMP = ('T' << 24 | 'A' << 16 | 'M' << 8 | 'P');
//                                      
    /* texture coordinates */
    public static final int ID_TMAP = ('T' << 24 | 'M' << 16 | 'A' << 8 | 'P');
    public static final int ID_AXIS = ('A' << 24 | 'X' << 16 | 'I' << 8 | 'S');
    public static final int ID_CNTR = ('C' << 24 | 'N' << 16 | 'T' << 8 | 'R');
    public static final int ID_SIZE = ('S' << 24 | 'I' << 16 | 'Z' << 8 | 'E');
    public static final int ID_ROTA = ('R' << 24 | 'O' << 16 | 'T' << 8 | 'A');
    public static final int ID_OREF = ('O' << 24 | 'R' << 16 | 'E' << 8 | 'F');
    public static final int ID_FALL = ('F' << 24 | 'A' << 16 | 'L' << 8 | 'L');
    public static final int ID_CSYS = ('C' << 24 | 'S' << 16 | 'Y' << 8 | 'S');
//                                      
    /* image map */
    public static final int ID_IMAP = ('I' << 24 | 'M' << 16 | 'A' << 8 | 'P');
    public static final int ID_IMAG = ('I' << 24 | 'M' << 16 | 'A' << 8 | 'G');
    public static final int ID_WRAP = ('W' << 24 | 'R' << 16 | 'A' << 8 | 'P');
    public static final int ID_WRPW = ('W' << 24 | 'R' << 16 | 'P' << 8 | 'W');
    public static final int ID_WRPH = ('W' << 24 | 'R' << 16 | 'P' << 8 | 'H');
//    public static final int ID_VMAP = ('V' << 24 | 'M' << 16 | 'A' << 8 | 'P');
    public static final int ID_AAST = ('A' << 24 | 'A' << 16 | 'S' << 8 | 'T');
    public static final int ID_PIXB = ('P' << 24 | 'I' << 16 | 'X' << 8 | 'B');
//                                      
    /* procedural */
    public static final int ID_PROC = ('P' << 24 | 'R' << 16 | 'O' << 8 | 'C');
//    public static final int ID_COLR = ('C' << 24 | 'O' << 16 | 'L' << 8 | 'R');
    public static final int ID_VALU = ('V' << 24 | 'A' << 16 | 'L' << 8 | 'U');
    public static final int ID_FUNC = ('F' << 24 | 'U' << 16 | 'N' << 8 | 'C');
    public static final int ID_FTPS = ('F' << 24 | 'T' << 16 | 'P' << 8 | 'S');
    public static final int ID_ITPS = ('I' << 24 | 'T' << 16 | 'P' << 8 | 'S');
    public static final int ID_ETPS = ('E' << 24 | 'T' << 16 | 'P' << 8 | 'S');
//                                      
    /* gradient */
    public static final int ID_GRAD = ('G' << 24 | 'R' << 16 | 'A' << 8 | 'D');
    public static final int ID_GRST = ('G' << 24 | 'R' << 16 | 'S' << 8 | 'T');
    public static final int ID_GREN = ('G' << 24 | 'R' << 16 | 'E' << 8 | 'N');
    public static final int ID_PNAM = ('P' << 24 | 'N' << 16 | 'A' << 8 | 'M');
    public static final int ID_INAM = ('I' << 24 | 'N' << 16 | 'A' << 8 | 'M');
    public static final int ID_GRPT = ('G' << 24 | 'R' << 16 | 'P' << 8 | 'T');
    public static final int ID_FKEY = ('F' << 24 | 'K' << 16 | 'E' << 8 | 'Y');
    public static final int ID_IKEY = ('I' << 24 | 'K' << 16 | 'E' << 8 | 'Y');
//                                      
    /* shader */
    public static final int ID_SHDR = ('S' << 24 | 'H' << 16 | 'D' << 8 | 'R');
    public static final int ID_DATA = ('D' << 24 | 'A' << 16 | 'T' << 8 | 'A');

    /* generic linked list */
    static abstract class lwNode implements NiLLABLE<lwNode> {

//        lwNode next, prev;
        Object data;
        boolean NULL;

        @Override
        public lwNode oSet(lwNode node) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isNULL() {
            return NULL;
        }

        static int getPosition(lwNode n) {
            int position = 0;

            while (n != null) {
                position++;
                n = n.getPrev();
            }

            return position;
        }

        static lwNode getPosition(lwNode n, int pos) {
            int position = getPosition(n);

            if (position > pos) {
                while (position != pos) {
                    if (null == n) {//TODO:make sure the returning null isn't recast into an int somewhere.
                        break;
                    }
                    position--;
                    n = n.getPrev();
                }
            } else {
                while (position != pos) {
                    if (null == n) {
                        break;
                    }
                    position--;
                    n = n.getNext();
                }
            }

            return n;
        }

        public abstract lwNode getNext();

        public abstract void setNext(lwNode next);

        public abstract lwNode getPrev();

        public abstract void setPrev(lwNode prev);
    };


    /* plug-in reference */
    static class lwPlugin extends lwNode {

        lwPlugin next, prev;
        String ord;
        String name;
        int flags;

//        Object data;
        @Override
        public lwNode getNext() {
            return next;
        }

        @Override
        public void setNext(lwNode next) {
            this.next = (lwPlugin) next;
        }

        @Override
        public lwNode getPrev() {
            return prev;
        }

        @Override
        public void setPrev(lwNode prev) {
            this.prev = (lwPlugin) prev;
        }
    };


    /* envelopes */
    static class lwKey extends lwNode {

        lwKey next, prev;
        float value;
        float time;
        long shape;               // ID_TCB, ID_BEZ2, etc. 
        float tension;
        float continuity;
        float bias;
        float[] param = new float[4];

        @Override
        public lwNode getNext() {
            return next;
        }

        @Override
        public void setNext(lwNode next) {
            this.next = (lwKey) next;
        }

        @Override
        public lwNode getPrev() {
            return prev;
        }

        @Override
        public void setPrev(lwNode prev) {
            this.prev = (lwKey) prev;
        }
    };

    static class lwEnvelope extends lwNode {

        lwEnvelope next, prev;
        int index;
        int type;
        String name;
        lwKey key;                    // linked list of keys 
        int nkeys;
        int[] behavior = new int[2];  // pre and post (extrapolation) 
        lwPlugin cfilter;             // linked list of channel filters 
        int ncfilters;

        @Override
        public lwNode getNext() {
            return next;
        }

        @Override
        public void setNext(lwNode next) {
            this.next = (lwEnvelope) next;
        }

        @Override
        public lwNode getPrev() {
            return prev;
        }

        @Override
        public void setPrev(lwNode prev) {
            this.prev = (lwEnvelope) prev;
        }
    };
    public static final int BEH_RESET = 0;
    public static final int BEH_CONSTANT = 1;
    public static final int BEH_REPEAT = 2;
    public static final int BEH_OSCILLATE = 3;
    public static final int BEH_OFFSET = 4;
    public static final int BEH_LINEAR = 5;


    /* values that can be enveloped */
    static class lwEParam {

        float val;
        int eindex;
    };

    static class lwVParam {

        float[] val = new float[3];
        int eindex;
    };


    /* clips */
    static class lwClipStill {

        String name;
    };

    static class lwClipSeq {

        String prefix;              // filename before sequence digits 
        String suffix;              // after digits, e.g. extensions 
        int digits;
        int flags;
        int offset;
        int start;
        int end;
    };

    static class lwClipAnim {

        String name;
        String server;              // anim loader plug-in 
        Object data;
    };

    static class lwClipXRef {

        String string;
        int index;
        lwClip clip;
    };

    static class lwClipCycle {

        String name;
        int lo;
        int hi;
    };

    static class lwClip extends lwNode {

        lwClip next, prev;
        int index;
        int type;                // ID_STIL, ID_ISEQ, etc. 

        static class Source {

            lwClipStill still = new lwClipStill();
            lwClipSeq seq = new lwClipSeq();
            lwClipAnim anim = new lwClipAnim();
            lwClipXRef xref = new lwClipXRef();
            lwClipCycle cycle = new lwClipCycle();
        };
        Source source = new Source();
        float start_time;
        float duration;
        float frame_rate;
        lwEParam contrast = new lwEParam();
        lwEParam brightness = new lwEParam();
        lwEParam saturation = new lwEParam();
        lwEParam hue = new lwEParam();
        lwEParam gamma = new lwEParam();
        int negative;
        lwPlugin ifilter;             // linked list of image filters 
        int nifilters;
        lwPlugin pfilter;             // linked list of pixel filters 
        int npfilters;

        @Override
        public lwNode getNext() {
            return next;
        }

        @Override
        public void setNext(lwNode next) {
            this.next = (lwClip) next;
        }

        @Override
        public lwNode getPrev() {
            return prev;
        }

        @Override
        public void setPrev(lwNode prev) {
            this.prev = (lwClip) prev;
        }
    };

    /* textures */
    static class lwTMap {

        lwVParam size = new lwVParam();
        lwVParam center = new lwVParam();
        lwVParam rotate = new lwVParam();
        lwVParam falloff = new lwVParam();
        int fall_type;
        String ref_object;
        int coord_sys;
    };

    static class lwImageMap {

        int cindex;
        int projection;
        String vmap_name;
        int axis;
        int wrapw_type;
        int wraph_type;
        lwEParam wrapw = new lwEParam();
        lwEParam wraph = new lwEParam();
        float aa_strength;
        int aas_flags;
        int pblend;
        lwEParam stck = new lwEParam();
        lwEParam amplitude = new lwEParam();
    };
    public static final int PROJ_PLANAR = 0;
    public static final int PROJ_CYLINDRICAL = 1;
    public static final int PROJ_SPHERICAL = 2;
    public static final int PROJ_CUBIC = 3;
    public static final int PROJ_FRONT = 4;
//
    public static final int WRAP_NONE = 0;
    public static final int WRAP_EDGE = 1;
    public static final int WRAP_REPEAT = 2;
    public static final int WRAP_MIRROR = 3;

    static class lwProcedural {

        int axis;
        float[] value = new float[3];
        String name;
        Object data;
    };

    static class lwGradKey {

        lwGradKey next, prev;
        float value;
        float[] rgba = new float[4];
    };

    static class lwGradient {

        String paramname;
        String itemname;
        float start;
        float end;
        int repeat;
        lwGradKey[] key;             // array of gradient keys 
        short[] ikey;                // array of interpolation codes 
    };

    static class lwTexture extends lwNode {

        lwTexture next, prev;
        String ord;
        long type;
        long chan;
        lwEParam opacity = new lwEParam();
        short opac_type;
        short enabled;
        short negative;
        short axis;

        static class Param {

            lwImageMap imap = new lwImageMap();
            lwProcedural proc = new lwProcedural();
            lwGradient grad = new lwGradient();
        };
        Param param = new Param();
        lwTMap tmap = new lwTMap();

        public lwTexture() {
            NULL = true;
        }

        @Override
        public lwNode oSet(lwNode node) {
            lwTexture tempNode = (lwTexture) node;

            NULL = false;

            this.next = tempNode.next;
            this.prev = tempNode.prev;
            this.ord = tempNode.ord;
            this.type = tempNode.type;
            this.chan = tempNode.chan;
            this.opac_type = tempNode.opac_type;
            this.enabled = tempNode.enabled;
            this.negative = tempNode.negative;
            this.axis = tempNode.axis;

            return this;
        }

        @Override
        public lwNode getNext() {
            return next;
        }

        @Override
        public void setNext(lwNode next) {
            this.next = (lwTexture) next;
        }

        @Override
        public lwNode getPrev() {
            return prev;
        }

        @Override
        public void setPrev(lwNode prev) {
            this.prev = (lwTexture) prev;
        }
    };


    /* values that can be textured */
    static class lwTParam {

        float val;
        int eindex;
        lwTexture tex = new lwTexture();                 // linked list of texture layers
    };

    static class lwCParam {

        float[] rgb = new float[3];
        int eindex;
        lwTexture tex = new lwTexture();                 // linked list of texture layers
    };


    /* surfaces */
    static class lwGlow {

        short enabled;
        short type;
        lwEParam intensity;
        lwEParam size;
    };

    static class lwRMap {

        lwTParam val = new lwTParam();
        int options;
        int cindex;
        float seam_angle;
    };

    static class lwLine {

        short enabled;
        int flags;
        lwEParam size;
    };

    static class lwSurface extends lwNode {

        lwSurface next, prev;
        String name;
        String srcname;
        lwCParam color = new lwCParam();
        lwTParam luminosity = new lwTParam();
        lwTParam diffuse = new lwTParam();
        lwTParam specularity = new lwTParam();
        lwTParam glossiness = new lwTParam();
        lwRMap reflection = new lwRMap();
        lwRMap transparency = new lwRMap();
        lwTParam eta = new lwTParam();
        lwTParam translucency = new lwTParam();
        lwTParam bump = new lwTParam();
        float smooth;
        int sideflags;
        float alpha;
        int alpha_mode;
        lwEParam color_hilite = new lwEParam();
        lwEParam color_filter = new lwEParam();
        lwEParam add_trans = new lwEParam();
        lwEParam dif_sharp = new lwEParam();
        lwEParam glow = new lwEParam();
        lwLine line = new lwLine();
        lwPlugin shader = new lwPlugin();           // linked list of shaders 
        int nshaders;

        @Override
        public lwNode oSet(lwNode node) {

            lwSurface surface = (lwSurface) node;

            this.next = surface.next;
            this.prev = surface.prev;
            this.name = surface.name;
            this.srcname = surface.srcname;
            this.color = surface.color;
            this.luminosity = surface.luminosity;
            this.diffuse = surface.diffuse;
            this.specularity = surface.specularity;
            this.glossiness = surface.glossiness;
            this.reflection = surface.reflection;
            this.transparency = surface.transparency;
            this.eta = surface.eta;
            this.translucency = surface.translucency;
            this.bump = surface.bump;
            this.smooth = surface.smooth;
            this.sideflags = surface.sideflags;
            this.alpha = surface.alpha;
            this.alpha_mode = surface.alpha_mode;
            this.color_hilite = surface.color_hilite;
            this.color_filter = surface.color_filter;
            this.add_trans = surface.add_trans;
            this.dif_sharp = surface.dif_sharp;
            this.glow = surface.glow;
            this.line = surface.line;
            this.shader = surface.shader;
            this.nshaders = surface.nshaders;

            return this;
        }

        @Override
        public lwNode getNext() {
            return next;
        }

        @Override
        public void setNext(lwNode next) {
            this.next = (lwSurface) next;
        }

        @Override
        public lwNode getPrev() {
            return prev;
        }

        @Override
        public void setPrev(lwNode prev) {
            this.prev = (lwSurface) prev;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override//TODO:make sure the name is enough for equality.
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final lwSurface other = (lwSurface) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }
    };

    /* vertex maps */
    static class lwVMap extends lwNode {

        lwVMap next, prev;
        String name;
        long type;
        int dim;
        int nverts;
        int perpoly;
        int[] vindex;             // array of point indexes 
        int[] pindex;             // array of polygon indexes 
        float[][] val;
        // added by duffy
        int offset;

        private void clear() {
            this.next = null;
            this.prev = null;
            this.name = null;
            this.type = 0;
            this.dim = 0;
            this.nverts = 0;
            this.perpoly = 0;
            this.vindex = null;
            this.pindex = null;
            this.val = null;
            this.offset = 0;
        }

        @Override
        public lwNode getNext() {
            return next;
        }

        @Override
        public void setNext(lwNode next) {
            this.next = (lwVMap) next;
        }

        @Override
        public lwNode getPrev() {
            return prev;
        }

        @Override
        public void setPrev(lwNode prev) {
            this.prev = (lwVMap) prev;
        }
    };

    static class lwVMapPt {

        lwVMap vmap;
        int index;               // vindex or pindex element 

        public lwVMapPt() {
        }

        static lwVMapPt[] generateArray(final int length) {
            return Stream.
                    generate(() -> new lwVMapPt()).
                    limit(length).
                    toArray(lwVMapPt[]::new);
        }
    };


    /* points and polygons */
    static class lwPoint {

        float[] pos = new float[3];
        int npols;               // number of polygons sharing the point 
        int[] pol;               // array of polygon indexes 
        int nvmaps;
        lwVMapPt[] vm;           // array of vmap references 
    };

    static class lwPolVert {

        int index;               // index into the point array 
        float[] norm = new float[3];
        int nvmaps;
        lwVMapPt[] vm;           // array of vmap references 
    };

    static class lwPolygon {

        lwSurface surf;
        int part;                // part index 
        int smoothgrp;           // smoothing group 
        int flags;
        long type;
        float[] norm = new float[3];
        int nverts;
        private lwPolVert[] v;   // array of vertex records 
        private int vOffset;     // the offset from the start of v to point towards.

        public lwPolVert getV(int index) {
            return v[vOffset + index];
        }

        public void setV(lwPolVert[] v, int vOffset) {
            this.v = v;
            this.vOffset = vOffset;
        }

    };

    static class lwPointList {

        int count;
        int offset;              // only used during reading 
        lwPoint[] pt;            // array of points 
    };

    static class lwPolygonList {

        int count;
        int offset;              // only used during reading 
        int vcount;              // total number of vertices 
        int voffset;             // only used during reading 
        lwPolygon[] pol;         // array of polygons 
    };


    /* geometry layers */
    static class lwLayer extends lwNode {

        lwLayer next, prev;
        String name;
        int index;
        int parent;
        int flags;
        float[] pivot = new float[3];
        float[] bbox = new float[6];
        lwPointList point = new lwPointList();
        lwPolygonList polygon = new lwPolygonList();
        int nvmaps;
        lwVMap vmap;          // linked list of vmaps 

        @Override
        public lwNode getNext() {
            return next;
        }

        @Override
        public void setNext(lwNode next) {
            this.next = (lwLayer) next;
        }

        @Override
        public lwNode getPrev() {
            return prev;
        }

        @Override
        public void setPrev(lwNode prev) {
            this.prev = (lwLayer) prev;
        }
    };


    /* tag strings */
    static class lwTagList {

        int count;
        int offset;             // only used during reading 
        String[] tag;           // array of strings 
    };


    /* an object */
    static class lwObject {

        long[] timeStamp = {0};
        lwLayer layer;          // linked list of layers 
        lwEnvelope env;         // linked list of envelopes 
        lwClip clip;            // linked list of clips 
        lwSurface surf;         // linked list of surfaces 
        final lwTagList taglist = new lwTagList();
        int nlayers;
        int nenvs;
        int nclips;
        int nsurfs;
    };

    /*
     ======================================================================

     Converted from lwobject sample prog from LW 6.5 SDK.

     ======================================================================
     */
    static abstract class LW {

        public abstract void run(final Object p);
    };

    /*
     ======================================================================
     lwFreeClip()

     Free memory used by an lwClip.
     ====================================================================== */
    @Deprecated
    public static class lwFreeClip extends LW {

        private static final LW instance = new lwFreeClip();

        private lwFreeClip() {
        }

        public static LW getInstance() {
            return instance;
        }

        @Override
        @Deprecated
        public void run(Object p) {
            lwClip clip = (lwClip) p;
            if (clip != null) {
//                lwListFree(clip.ifilter, lwFreePlugin.getInstance());
//                lwListFree(clip.pfilter, lwFreePlugin.getInstance());
                switch (clip.type) {
                    case ID_STIL: {
                        if (clip.source.still.name != null) {
                            clip.source.still.name = null;
                        }
                        break;
                    }
                    case ID_ISEQ: {
                        if (clip.source.seq.suffix != null) {
                            clip.source.seq.suffix = null;
                        }
                        if (clip.source.seq.prefix != null) {
                            clip.source.seq.prefix = null;
                        }
                        break;
                    }
                    case ID_ANIM: {
                        if (clip.source.anim.server != null) {
                            clip.source.anim.server = null;
                        }
                        if (clip.source.anim.name != null) {
                            clip.source.anim.name = null;
                        }
                        break;
                    }
                    case ID_XREF: {
                        if (clip.source.xref.string != null) {
                            clip.source.xref.string = null;
                        }
                        break;
                    }
                    case ID_STCC: {
                        if (clip.source.cycle.name != null) {
                            clip.source.cycle.name = null;
                        }
                        break;
                    }
                }
                clip = null;
            }
        }
    };


    /*
     ======================================================================
     lwGetClip()

     Read image references from a CLIP chunk in an LWO2 file.
     ====================================================================== */
    public static lwClip lwGetClip(idFile fp, int cksize) {
        lwClip clip;
        lwPlugin filt;
        int id;
        int sz;
        int pos, rlen;


        /* allocate the Clip structure */
        Fail:
        if (true) {
            clip = new lwClip();// Mem_ClearedAlloc(sizeof(lwClip));
//            if (NOT(clip)) {
//                break Fail;
//            }

            clip.contrast.val = 1.0f;
            clip.brightness.val = 1.0f;
            clip.saturation.val = 1.0f;
            clip.gamma.val = 1.0f;

            /* remember where we started */
            set_flen(0);
            pos = fp.Tell();

            /* index */
            clip.index = getI4(fp);

            /* first subchunk header */
            clip.type = getU4(fp);
            sz = getU2(fp);
            if (0 > get_flen()) {
                break Fail;
            }

            sz += sz & 1;
            set_flen(0);

            switch (clip.type) {
                case ID_STIL:
                    clip.source.still.name = getS0(fp);
                    break;

                case ID_ISEQ:
                    clip.source.seq.digits = getU1(fp);
                    clip.source.seq.flags = getU1(fp);
                    clip.source.seq.offset = getI2(fp);
                    clip.source.seq.start = getI2(fp);
                    clip.source.seq.end = getI2(fp);
                    clip.source.seq.prefix = getS0(fp);
                    clip.source.seq.suffix = getS0(fp);
                    break;

                case ID_ANIM:
                    clip.source.anim.name = getS0(fp);
                    clip.source.anim.server = getS0(fp);
                    rlen = get_flen();
                    clip.source.anim.data = getbytes(fp, sz - rlen);
                    break;

                case ID_XREF:
                    clip.source.xref.index = getI4(fp);
                    clip.source.xref.string = getS0(fp);
                    break;

                case ID_STCC:
                    clip.source.cycle.lo = getI2(fp);
                    clip.source.cycle.hi = getI2(fp);
                    clip.source.cycle.name = getS0(fp);
                    break;

                default:
                    break;
            }

            /* error while reading current subchunk? */
            rlen = get_flen();
            if (rlen < 0 || rlen > sz) {
                break Fail;
            }

            /* skip unread parts of the current subchunk */
            if (rlen < sz) {
                fp.Seek(sz - rlen, FS_SEEK_CUR);
            }

            /* end of the CLIP chunk? */
            rlen = fp.Tell() - pos;
            if (cksize < rlen) {
                break Fail;
            }

            if (cksize == rlen) {
                return clip;
            }

            /* process subchunks as they're encountered */
            id = getU4(fp);
            sz = getU2(fp);
            if (0 > get_flen()) {
                break Fail;
            }

            while (true) {
                sz += sz & 1;
                set_flen(0);

                switch (id) {
                    case ID_TIME:
                        clip.start_time = getF4(fp);
                        clip.duration = getF4(fp);
                        clip.frame_rate = getF4(fp);
                        break;

                    case ID_CONT:
                        clip.contrast.val = getF4(fp);
                        clip.contrast.eindex = getVX(fp);
                        break;

                    case ID_BRIT:
                        clip.brightness.val = getF4(fp);
                        clip.brightness.eindex = getVX(fp);
                        break;

                    case ID_SATR:
                        clip.saturation.val = getF4(fp);
                        clip.saturation.eindex = getVX(fp);
                        break;

                    case ID_HUE:
                        clip.hue.val = getF4(fp);
                        clip.hue.eindex = getVX(fp);
                        break;

                    case ID_GAMM:
                        clip.gamma.val = getF4(fp);
                        clip.gamma.eindex = getVX(fp);
                        break;

                    case ID_NEGA:
                        clip.negative = getU2(fp);
                        break;

                    case ID_IFLT:
                    case ID_PFLT:
                        filt = new lwPlugin();// Mem_ClearedAlloc(sizeof(lwPlugin));
                        if (NOT(filt)) {
                            break Fail;
                        }

                        filt.name = getS0(fp);
                        filt.flags = getU2(fp);
                        rlen = get_flen();
                        filt.data = getbytes(fp, sz - rlen);

                        if (id == ID_IFLT) {
                            clip.ifilter = lwListAdd(clip.ifilter, filt);//TODO:check this construction
                            clip.nifilters++;
                        } else {
                            clip.ifilter = lwListAdd(clip.ifilter, filt);
                            clip.npfilters++;
                        }
                        break;

                    default:
                        break;
                }

                /* error while reading current subchunk? */
                rlen = get_flen();
                if (rlen < 0 || rlen > sz) {
                    break Fail;
                }

                /* skip unread parts of the current subchunk */
                if (rlen < sz) {
                    fp.Seek(sz - rlen, FS_SEEK_CUR);
                }

                /* end of the CLIP chunk? */
                rlen = fp.Tell() - pos;
                if (cksize < rlen) {
                    break Fail;
                }
                if (cksize == rlen) {
                    break;
                }

                /* get the next chunk header */
                set_flen(0);
                id = getU4(fp);
                sz = getU2(fp);
                if (6 != get_flen()) {
                    break Fail;
                }
            }

            return clip;
        }
//        Fail:
        lwFreeClip.getInstance().run(clip);
        return null;
    }


    /*
     ======================================================================
     lwFindClip()

     Returns an lwClip pointer, given a clip index.
     ====================================================================== */
    public static lwClip lwFindClip(lwClip list, int index) {
        lwClip clip;

        clip = list;
        while (clip != null) {
            if (clip.index == index) {
                break;
            }
            clip = clip.next;
        }
        return clip;
    }

    @Deprecated
    public static class lwFree extends LW {

        private static final LW instance = new lwFree();

        private lwFree() {
        }

        public static LW getInstance() {
            return instance;
        }

        @Override
        public void run(Object p) {
//        Mem_Free(ptr);
        }
    }

    /*
     ======================================================================
     lwFreeEnvelope()

     Free the memory used by an lwEnvelope.
     ====================================================================== */
    @Deprecated
    public static class lwFreeEnvelope extends LW {

        private static final LW instance = new lwFreeEnvelope();

        private lwFreeEnvelope() {
        }

        public static LW getInstance() {
            return instance;
        }

        @Override
        public void run(Object p) {
            lwEnvelope env = (lwEnvelope) p;
            if (env != null) {
                if (env.name != null) {
                    env.name = null;
                }
//                lwListFree(env.key, lwFree.getInstance());
//                lwListFree(env.cfilter, lwFreePlugin.getInstance());
                env = null;
            }
        }
    };

    public static class compare_keys implements cmp_t<lwKey> {

        @Override
        public int compare(final lwKey k1, final lwKey k2) {
            return k1.time > k2.time ? 1 : k1.time < k2.time ? -1 : 0;
        }
    }


    /*
     ======================================================================
     lwGetEnvelope()

     Read an ENVL chunk from an LWO2 file.
     ====================================================================== */
    public static lwEnvelope lwGetEnvelope(idFile fp, int cksize) {
        lwEnvelope env;
        lwKey key = null;
        lwPlugin plug;
        int id;
        short sz;
        float[] f = new float[4];
        int i, nparams, pos, rlen;


        /* allocate the Envelope structure */
        Fail:
        if (true) {
            env = new lwEnvelope();// Mem_ClearedAlloc(sizeof(lwEnvelope));
            if (NOT(env)) {
                break Fail;
            }

            /* remember where we started */
            set_flen(0);
            pos = fp.Tell();

            /* index */
            env.index = getVX(fp);

            /* first subchunk header */
            id = getU4(fp);
            sz = getU2(fp);
            if (0 > get_flen()) {
                break Fail;
            }

            /* process subchunks as they're encountered */
            while (true) {
                sz += sz & 1;
                set_flen(0);

                switch (id) {
                    case ID_TYPE:
                        env.type = getU2(fp);
                        break;

                    case ID_NAME:
                        env.name = getS0(fp);
                        break;

                    case ID_PRE:
                        env.behavior[0] = getU2(fp);
                        break;

                    case ID_POST:
                        env.behavior[1] = getU2(fp);
                        break;

                    case ID_KEY:
                        key = new lwKey();// Mem_ClearedAlloc(sizeof(lwKey));
                        if (NOT(key)) {//TODO:unnecessary?
                            break Fail;
                        }

                        key.time = getF4(fp);
                        key.value = getF4(fp);
                        lwListInsert(env.key, key, new compare_keys());
                        env.nkeys++;
                        break;

                    case ID_SPAN:
                        if (NOT(key)) {
                            break Fail;
                        }
                        key.shape = getU4(fp);

                        nparams = (sz - 4) / 4;
                        if (nparams > 4) {
                            nparams = 4;
                        }
                        for (i = 0; i < nparams; i++) {
                            f[i] = getF4(fp);
                        }

                        switch ((int) key.shape) {
                            case ID_TCB:
                                key.tension = f[0];
                                key.continuity = f[1];
                                key.bias = f[2];
                                break;

                            case ID_BEZI:
                            case ID_HERM:
                            case ID_BEZ2:
                                for (i = 0; i < nparams; i++) {
                                    key.param[i] = f[i];
                                }
                                break;
                        }
                        break;

                    case ID_CHAN:
                        plug = new lwPlugin();// Mem_ClearedAlloc(sizeof(lwPlugin));
                        if (NOT(plug)) {
                            break Fail;
                        }

                        plug.name = getS0(fp);
                        plug.flags = getU2(fp);
                        plug.data = getbytes(fp, sz - get_flen());

                        env.cfilter = lwListAdd(env.cfilter, plug);
                        env.ncfilters++;
                        break;

                    default:
                        break;
                }

                /* error while reading current subchunk? */
                rlen = get_flen();
                if (rlen < 0 || rlen > sz) {
                    break Fail;
                }

                /* skip unread parts of the current subchunk */
                if (rlen < sz) {
                    fp.Seek(sz - rlen, FS_SEEK_CUR);
                }

                /* end of the ENVL chunk? */
                rlen = fp.Tell() - pos;
                if (cksize < rlen) {
                    break Fail;
                }
                if (cksize == rlen) {
                    break;
                }

                /* get the next subchunk header */
                set_flen(0);
                id = getU4(fp);
                sz = getU2(fp);
                if (6 != get_flen()) {
                    break Fail;
                }
            }

            return env;
        }
//        Fail:
        lwFreeEnvelope.getInstance().run(env);
        return null;
    }


    /*
     ======================================================================
     lwFindEnvelope()

     Returns an lwEnvelope pointer, given an envelope index.
     ====================================================================== */
    public static lwEnvelope lwFindEnvelope(lwEnvelope list, int index) {
        lwEnvelope env;

        env = list;
        while (env != null) {
            if (env.index == index) {
                break;
            }
            env = env.next;
        }
        return env;
    }


    /*
     ======================================================================
     range()

     Given the value v of a periodic function, returns the equivalent value
     v2 in the principal interval [lo, hi].  If i isn't null, it receives
     the number of wavelengths between v and v2.

     v2 = v - i * (hi - lo)

     For example, range( 3 pi, 0, 2 pi, i ) returns pi, with i = 1.
     ====================================================================== */
    public static float range(float v, float lo, float hi, int[] i) {
        float v2, r = hi - lo;

        if (r == 0.0) {
            if (i[0] != 0) {
                i[0] = 0;
            }
            return lo;
        }

        v2 = lo + v - r * (float) Math.floor((double) v / r);
        if (i[0] != 0) {
            i[0] = -(int) ((v2 - v) / r + (v2 > v ? 0.5 : -0.5));
        }

        return v2;
    }


    /*
     ======================================================================
     hermite()

     Calculate the Hermite coefficients.
     ====================================================================== */
    public static void hermite(float t, float[] h1, float[] h2, float[] h3, float[] h4) {
        float t2, t3;

        t2 = t * t;
        t3 = t * t2;

        h2[0] = 3.0f * t2 - t3 - t3;
        h1[0] = 1.0f - h2[0];
        h4[0] = t3 - t2;
        h3[0] = h4[0] - t2 + t;
    }


    /*
     ======================================================================
     bezier()

     Interpolate the value of a 1D Bezier curve.
     ====================================================================== */
    public static float bezier(float x0, float x1, float x2, float x3, float t) {
        float a, b, c, t2, t3;

        t2 = t * t;
        t3 = t2 * t;

        c = 3.0f * (x1 - x0);
        b = 3.0f * (x2 - x1) - c;
        a = x3 - x0 - c - b;

        return a * t3 + b * t2 + c * t + x0;
    }


    /*
     ======================================================================
     bez2_time()

     Find the t for which bezier() returns the input time.  The handle
     endpoints of a BEZ2 curve represent the control points, and these have
     (time, value) coordinates, so time is used as both a coordinate and a
     parameter for this curve type.
     ====================================================================== */
    public static float bez2_time(float x0, float x1, float x2, float x3, float time,
            float[] t0, float[] t1) {
        float v, t;

        t = t0[0] + (t1[0] - t0[0]) * 0.5f;
        v = bezier(x0, x1, x2, x3, t);
        if (idMath.Fabs(time - v) > .0001f) {
            if (v > time) {
                t1[0] = t;
            } else {
                t0[0] = t;
            }
            return bez2_time(x0, x1, x2, x3, time, t0, t1);
        } else {
            return t;
        }
    }


    /*
     ======================================================================
     bez2()

     Interpolate the value of a BEZ2 curve.
     ====================================================================== */
    public static float bez2(lwKey key0, lwKey key1, float time) {
        float x, y, t;
        float[] t0 = {0.0f}, t1 = {1.0f};

        if (key0.shape == ID_BEZ2) {
            x = key0.time + key0.param[2];
        } else {
            x = key0.time + (key1.time - key0.time) / 3.0f;
        }

        t = bez2_time(key0.time, x, key1.time + key1.param[0], key1.time, time, t0, t1);

        if (key0.shape == ID_BEZ2) {
            y = key0.value + key0.param[3];
        } else {
            y = key0.value + key0.param[1] / 3.0f;
        }

        return bezier(key0.value, y, key1.param[1] + key1.value, key1.value, t);
    }


    /*
     ======================================================================
     outgoing()

     Return the outgoing tangent to the curve at key0.  The value returned
     for the BEZ2 case is used when extrapolating a linear pre behavior and
     when interpolating a non-BEZ2 span.
     ====================================================================== */
    public static float outgoing(lwKey key0, lwKey key1) {
        float a, b, d, t, out;

        switch ((int) key0.shape) {
            case ID_TCB:
                a = (1.0f - key0.tension)
                        * (1.0f + key0.continuity)
                        * (1.0f + key0.bias);
                b = (1.0f - key0.tension)
                        * (1.0f - key0.continuity)
                        * (1.0f - key0.bias);
                d = key1.value - key0.value;

                if (key0.prev != null) {
                    t = (key1.time - key0.time) / (key1.time - key0.prev.time);
                    out = t * (a * (key0.value - key0.prev.value) + b * d);
                } else {
                    out = b * d;
                }
                break;

            case ID_LINE:
                d = key1.value - key0.value;
                if (key0.prev != null) {
                    t = (key1.time - key0.time) / (key1.time - key0.prev.time);
                    out = t * (key0.value - key0.prev.value + d);
                } else {
                    out = d;
                }
                break;

            case ID_BEZI:
            case ID_HERM:
                out = key0.param[1];
                if (key0.prev != null) {
                    out *= (key1.time - key0.time) / (key1.time - key0.prev.time);
                }
                break;

            case ID_BEZ2:
                out = key0.param[3] * (key1.time - key0.time);
                if (idMath.Fabs(key0.param[2]) > 1e-5f) {
                    out /= key0.param[2];
                } else {
                    out *= 1e5f;
                }
                break;

            case ID_STEP:
            default:
                out = 0.0f;
                break;
        }

        return out;
    }


    /*
     ======================================================================
     incoming()

     Return the incoming tangent to the curve at key1.  The value returned
     for the BEZ2 case is used when extrapolating a linear post behavior.
     ====================================================================== */
    public static float incoming(lwKey key0, lwKey key1) {
        float a, b, d, t, in;

        switch ((int) key1.shape) {
            case ID_LINE:
                d = key1.value - key0.value;
                if (key1.next != null) {
                    t = (key1.time - key0.time) / (key1.next.time - key0.time);
                    in = t * (key1.next.value - key1.value + d);
                } else {
                    in = d;
                }
                break;

            case ID_TCB:
                a = (1.0f - key1.tension)
                        * (1.0f - key1.continuity)
                        * (1.0f + key1.bias);
                b = (1.0f - key1.tension)
                        * (1.0f + key1.continuity)
                        * (1.0f - key1.bias);
                d = key1.value - key0.value;

                if (key1.next != null) {
                    t = (key1.time - key0.time) / (key1.next.time - key0.time);
                    in = t * (b * (key1.next.value - key1.value) + a * d);
                } else {
                    in = a * d;
                }
                break;

            case ID_BEZI:
            case ID_HERM:
                in = key1.param[0];
                if (key1.next != null) {
                    in *= (key1.time - key0.time) / (key1.next.time - key0.time);
                }
//                break;
                return in;

            case ID_BEZ2:
                in = key1.param[1] * (key1.time - key0.time);
                if (idMath.Fabs(key1.param[0]) > 1e-5f) {
                    in /= key1.param[0];
                } else {
                    in *= 1e5f;
                }
                break;

            case ID_STEP:
            default:
                in = 0.0f;
                break;
        }

        return in;
    }


    /*
     ======================================================================
     evalEnvelope()

     Given a list of keys and a time, returns the interpolated value of the
     envelope at that time.
     ====================================================================== */
    public static float evalEnvelope(lwEnvelope env, float time) {
        lwKey key0, key1, skey, ekey;
        float t, in, out, offset = 0.0f;
        float[] h1 = new float[1], h2 = new float[1], h3 = new float[1], h4 = new float[1];
        int[] noff = new int[1];


        /* if there's no key, the value is 0 */
        if (env.nkeys == 0) {
            return 0.0f;
        }

        /* if there's only one key, the value is constant */
        if (env.nkeys == 1) {
            return env.key.value;
        }

        /* find the first and last keys */
        skey = ekey = env.key;
        while (ekey.next != null) {
            ekey = ekey.next;
        }

        /* use pre-behavior if time is before first key time */
        if (time < skey.time) {
            switch (env.behavior[0]) {
                case BEH_RESET:
                    return 0.0f;

                case BEH_CONSTANT:
                    return skey.value;

                case BEH_REPEAT:
                    time = range(time, skey.time, ekey.time, null);
                    break;

                case BEH_OSCILLATE:
                    time = range(time, skey.time, ekey.time, noff);
                    if ((noff[0] % 2) != 0) {
                        time = ekey.time - skey.time - time;
                    }
                    break;

                case BEH_OFFSET:
                    time = range(time, skey.time, ekey.time, noff);
                    offset = noff[0] * (ekey.value - skey.value);
                    break;

                case BEH_LINEAR:
                    out = outgoing(skey, skey.next)
                            / (skey.next.time - skey.time);
                    return out * (time - skey.time) + skey.value;
            }
        } /* use post-behavior if time is after last key time */ else if (time > ekey.time) {
            switch (env.behavior[1]) {
                case BEH_RESET:
                    return 0.0f;

                case BEH_CONSTANT:
                    return ekey.value;

                case BEH_REPEAT:
                    time = range(time, skey.time, ekey.time, null);
                    break;

                case BEH_OSCILLATE:
                    time = range(time, skey.time, ekey.time, noff);
                    if ((noff[0] % 2) != 0) {
                        time = ekey.time - skey.time - time;
                    }
                    break;

                case BEH_OFFSET:
                    time = range(time, skey.time, ekey.time, noff);
                    offset = noff[0] * (ekey.value - skey.value);
                    break;

                case BEH_LINEAR:
                    in = incoming(ekey.prev, ekey)
                            / (ekey.time - ekey.prev.time);
                    return in * (time - ekey.time) + ekey.value;
            }
        }

        /* get the endpoints of the interval being evaluated */
        key0 = env.key;
        while (time > key0.next.time) {
            key0 = key0.next;
        }
        key1 = key0.next;

        /* check for singularities first */
        if (time == key0.time) {
            return key0.value + offset;
        } else if (time == key1.time) {
            return key1.value + offset;
        }

        /* get interval length, time in [0, 1] */
        t = (time - key0.time) / (key1.time - key0.time);

        /* interpolate */
        switch ((int) key1.shape) {
            case ID_TCB:
            case ID_BEZI:
            case ID_HERM:
                out = outgoing(key0, key1);
                in = incoming(key0, key1);
                hermite(t, h1, h2, h3, h4);
                return h1[0] * key0.value + h2[0] * key1.value + h3[0] * out + h4[0] * in + offset;

            case ID_BEZ2:
                return bez2(key0, key1, time) + offset;

            case ID_LINE:
                return key0.value + t * (key1.value - key0.value) + offset;

            case ID_STEP:
                return key0.value + offset;

            default:
                return offset;
        }
    }

    /*
     ======================================================================
     lwListFree()

     Free the items in a list.
     ====================================================================== */
    @Deprecated
    public static void lwListFree(Object list, LW freeNode) {
        lwNode node, next;

        node = (lwNode) list;
        while (node != null) {
            next = node.getNext();
            freeNode.run(node);
            node = next;
        }
    }


    /*
     ======================================================================
     lwListAdd()

     Append a node to a list.
     ====================================================================== */
    static <T> T lwListAdd(T list, lwNode node) {
        lwNode head, tail = null;

        head = (lwNode) list;
        if (null == head) {
            return (T) node;
        }
        while (head != null) {
            tail = head;
            head = head.getNext();
        }
        tail.setNext(node);
        node.setPrev(tail);

        return list;
    }

//    @Deprecated
//    public static Object lwListAdd(Object list, Object node) {
//        lwNode head, tail = new lwNode();
//
//        head = (lwNode) list;
//        if (null == head) {
//            return node;
//        }
//        while (head != null) {
//            tail = head;
//            head = head.next;
//        }
//        tail.next = (lwNode) node;
//        ((lwNode) node).prev = tail;
//
//        return list;
//    }
    /*
     ======================================================================
     lwListInsert()

     Insert a node into a list in sorted order.
     ====================================================================== */
    public static void lwListInsert(lwNode vList, lwNode vItem, cmp_t compare) {
        lwNode list;
        lwNode item, node, prev;

        if (vList.isNULL()) {
            vList.oSet(vItem);
            return;
        }

        list = vList;
        item = (lwNode) vItem;
        node = list;
        prev = null;

        while (node != null) {
            if (0 < compare.compare(node, item)) {
                break;
            }
            prev = node;
            node = node.getNext();
        }

        if (null == prev) {
            vList.oSet(item);
            node.setPrev(item);
            item.setNext(node);
        } else if (null == node) {
            prev.setNext(item);
            item.setPrev(prev);
        } else {
            item.setNext(node);
            item.setPrev(prev);
            prev.setNext(item);
            node.setPrev(item);
        }
    }
    /*
     ======================================================================
     flen

     This accumulates a count of the number of bytes read.  Callers can set
     it at the beginning of a sequence of reads and then retrieve it to get
     the number of bytes actually read.  If one of the I/O functions fails,
     flen is set to an error code, after which the I/O functions ignore
     read requests until flen is reset.
     ====================================================================== */
    public static final int FLEN_ERROR = -9999;
    public static int flen;

    public static void set_flen(int i) {
        flen = i;
    }

    public static int get_flen() {
        return flen;
    }

    public static byte[] getbytes(idFile fp, int size) {
        ByteBuffer data;

        if (flen == FLEN_ERROR) {
            return null;
        }
        if (size < 0) {
            flen = FLEN_ERROR;
            return null;
        }
        data = ByteBuffer.allocate(size);//Mem_ClearedAlloc(size);
        if (null == data) {
            flen = FLEN_ERROR;
            return null;
        }
        if (size != fp.Read(data, size)) {
            flen = FLEN_ERROR;
//            Mem_Free(data);
            data = null;
            return null;
        }

        flen += size;
        return data.array();
    }

    public static void skipbytes(idFile fp, int n) {
        if (flen == FLEN_ERROR) {
            return;
        }
        if (!fp.Seek(n, FS_SEEK_CUR)) {
            flen = FLEN_ERROR;
        } else {
            flen += n;
        }
    }

    public static int getI1(idFile fp) {
        int i;
        byte[] c = {0};

        if (flen == FLEN_ERROR) {
            return 0;
        }
//        c[0] = 0;
        i = fp.Read(ByteBuffer.wrap(c));
        if (i < 0) {
            flen = FLEN_ERROR;
            return 0;
        }
        flen += 1;

        if (c[0] > 127) {
            return c[0] - 256;
        }
        return c[0];
    }

    public static short getI2(idFile fp) {
        ByteBuffer i = ByteBuffer.allocate(2);

        if (flen == FLEN_ERROR) {
            return 0;
        }
        if (2 != fp.Read(i)) {
            flen = FLEN_ERROR;
            return 0;
        }
        BigRevBytes(i, /*2,*/ 1);
        flen += 2;
        return i.getShort();
    }

    public static int getI4(idFile fp) {
        ByteBuffer i = ByteBuffer.allocate(4);

        if (flen == FLEN_ERROR) {
            return 0;
        }
        if (4 != fp.Read(i, 4)) {
            flen = FLEN_ERROR;
            return 0;
        }
        BigRevBytes(i, /*4,*/ 1);
        flen += 4;
        return i.getInt();
    }

    public static char getU1(idFile fp) {
        int i;
        byte[] c = {0};

        if (flen == FLEN_ERROR) {
            return 0;
        }
        c[0] = 0;
        i = fp.Read(ByteBuffer.wrap(c), 1);
        if (i < 0) {
            flen = FLEN_ERROR;
            return 0;
        }
        flen += 1;
        return (char) c[0];
    }

    public static short getU2(idFile fp) {
        ByteBuffer i = ByteBuffer.allocate(2);

        if (flen == FLEN_ERROR) {
            return 0;
        }
        if (2 != fp.Read(i)) {
            flen = FLEN_ERROR;
            return 0;
        }
        BigRevBytes(i, /*2*,*/ 1);
        flen += 2;
        return i.getShort();
    }

    public static int getU4(idFile fp) {
        ByteBuffer i = ByteBuffer.allocate(4);

        if (flen == FLEN_ERROR) {
            return 0;
        }
        if (4 != fp.Read(i)) {
            flen = FLEN_ERROR;
            return 0;
        }
        BigRevBytes(i, /*4,*/ 1);
        flen += 4;
        return i.getInt();
    }

    public static int getVX(idFile fp) {
        ByteBuffer c = ByteBuffer.allocate(1);
        int i;

        if (flen == FLEN_ERROR) {
            return 0;
        }

        c.clear();
        if (fp.Read(c) == -1) {
            return 0;
        }

        if (c.get(0) != 0xFF) {
            i = btoi(c) << 8;
            c.clear();
            if (fp.Read(c) == -1) {
                return 0;
            }
            i |= btoi(c);
            flen += 2;
        } else {
            c.clear();
            if (fp.Read(c) == -1) {
                return 0;
            }
            i = btoi(c) << 16;
            c.clear();
            if (fp.Read(c) == -1) {
                return 0;
            }
            i |= btoi(c) << 8;
            c.clear();
            if (fp.Read(c) == -1) {
                return 0;
            }
            i |= btoi(c);
            flen += 4;
        }

        return i;
    }

    public static float getF4(idFile fp) {
        ByteBuffer f = ByteBuffer.allocate(4);

        if (flen == FLEN_ERROR) {
            return 0.0f;
        }
        if (4 != fp.Read(f)) {
            flen = FLEN_ERROR;
            return 0.0f;
        }
        BigRevBytes(f, /*4,*/ 1);
        flen += 4;

        if (FLOAT_IS_DENORMAL(f.getFloat(0))) {
            return 0;
        }
        return f.getFloat(0);
    }

    public static String getS0(idFile fp) {
        ByteBuffer s;
        int i, len, pos;
        ByteBuffer c = ByteBuffer.allocate(1);

        if (flen == FLEN_ERROR) {
            return null;
        }

        pos = fp.Tell();
        for (i = 1;; i++) {
            c.clear();
            if (fp.Read(c) == -1) {
                flen = FLEN_ERROR;
                return null;
            }
            if (c.get(0) == 0) {
                break;
            }
        }

        if (i == 1) {
            if (!fp.Seek(pos + 2, FS_SEEK_SET)) {
                flen = FLEN_ERROR;
            } else {
                flen += 2;
            }
            return null;
        }

        len = i + (i & 1);
        s = ByteBuffer.allocate(len);// Mem_ClearedAlloc(len);
        if (NOT(s)) {
            flen = FLEN_ERROR;
            return null;
        }

        if (!fp.Seek(pos, FS_SEEK_SET)) {
            flen = FLEN_ERROR;
            return null;
        }
        if (len != fp.Read(s, len)) {
            flen = FLEN_ERROR;
            return null;
        }

        flen += len;
        return bbtocb(s).toString().trim();//TODO:check output(my tests return chinese characters).
    }

    @Deprecated//UNUSED
    public static int sgetI1(String[] bp) {
        int i;

        if (flen == FLEN_ERROR) {
            return 0;
        }
        i = bp[0].charAt(0);
        if (i > 127) {
            i -= 256;
        }
        flen += 1;
        bp[0] = bp[0].substring(1);
        return i;
    }

    public static short sgetI2(ByteBuffer bp) {
        short i;

        if (flen == FLEN_ERROR) {
            return 0;
        }
//   memcpy( i, bp, 2 );
        BigRevBytes(bp, /*bp.position(), 2,*/ 1);
        flen += 2;
        i = bp.getShort();
        bp.position(bp.position() + 2);
        return i;
    }

    @Deprecated//UNUSED
    public static int sgetI4(String[] bp) {
        throw new UnsupportedOperationException();
//        int[] i = {0};
//
//        if (flen == FLEN_ERROR) {
//            return 0;
//        }
////   memcpy( &i, *bp, 4 );
//        i[0] |= bp[0].charAt(0) << 24;
//        i[0] |= bp[0].charAt(1) << 16;
//        i[0] |= bp[0].charAt(2) << 8;
//        i[0] |= bp[0].charAt(3) << 0;//TODO:check endianess
//        BigRevBytes(i, /*4,*/ 1);
//        flen += 4;
//        bp[0] = bp[0].substring(4);
//        return i[0];
    }

    @Deprecated//UNUSED
    public static char sgetU1(String[] bp) {
        char c;

        if (flen == FLEN_ERROR) {
            return 0;
        }
        c = bp[0].charAt(0);
        flen += 1;
        bp[0] = bp[0].substring(1);
        return c;
    }

    public static short sgetU2(ByteBuffer bp) {
        short i;

        if (flen == FLEN_ERROR) {
            return 0;
        }
//        i = (short) ((bp.get() << 8) | bp.get());//TODO: &0xFF???
        i = bp.getShort();
        flen += 2;
//        *bp += 2;
        return i;
    }

    public static int sgetU4(ByteBuffer bp) {
        int i;

        if (flen == FLEN_ERROR) {
            return 0;
        }
//   memcpy( &i, *bp, 4 );
        BigRevBytes(bp, /*bp.position(), 4,*/ 1);
        flen += 4;
        i = bp.getInt();
//        bp.position(bp.position() + 4);
        return i;
    }

    public static int sgetVX(ByteBuffer bp) {
        int i;
        int pos = bp.position();

        if (flen == FLEN_ERROR) {
            return 0;
        }

        if (bp.get(pos) != 0xFF) {
            i = btoi(bp.get(pos)) << 8 | btoi(bp.get(pos + 1));
            flen += 2;
            bp.position(pos + 2);
        } else {
            i = btoi(bp.get(pos + 1)) << 16 | btoi(bp.get(pos + 2)) << 8 | btoi(bp.get(pos + 3));
            flen += 4;
            bp.position(pos + 4);
        }
        return i;
    }

    public static float sgetF4(ByteBuffer bp) {
        float f;
        int i = 0;

        if (flen == FLEN_ERROR) {
            return 0.0f;
        }
//   memcpy( &f, *bp, 4 );
        BigRevBytes(bp, /*bp.position(), 4,*/ 1);
        flen += 4;
        f = bp.getFloat();
//        bp.position(bp.position() + 4);

        if (FLOAT_IS_DENORMAL(f)) {
            f = 0.0f;
        }
        return f;
    }

    public static String sgetS0(ByteBuffer bp) {
        String s;
//   unsigned char *buf = *bp;
        int len;
        int pos = bp.position();

        if (flen == FLEN_ERROR) {
            return null;
        }

        //   len = strlen( (const char*)buf ) + 1;
        s = new String(bp.array()).substring(pos);
        len = strLen(s) + 1;//TODO:check 
        if (1 == len) {
            flen += 2;
            bp.position(pos + 2);
            return null;
        }
        len += len & 1;
//        s =  Mem_ClearedAlloc(len);
//        if (null == s) {
//            flen = FLEN_ERROR;
//            return null;
//        }
//
//   memcpy( s, buf, len );
        s = s.substring(0, len);
        flen += s.length();
        bp.position(pos + s.length());
        return s;
    }

    /*
     ======================================================================
     lwFreeLayer()

     Free memory used by an lwLayer.
     ====================================================================== */
    public static class lwFreeLayer extends LW {

        private static final LW instance = new lwFreeLayer();

        private lwFreeLayer() {
        }

        public static LW getInstance() {
            return instance;
        }

        @Override
        public void run(Object p) {
            lwLayer layer = (lwLayer) p;
            if (layer != null) {
                if (layer.name != null) {
                    layer.name = null;
                }
                lwFreePoints(layer.point);
                lwFreePolygons(layer.polygon);
//                lwListFree(layer.vmap, lwFreeVMap.getInstance());
                layer = null;
            }
        }
    };


    /*
     ======================================================================
     lwFreeObject()

     Free memory used by an lwObject.
     ====================================================================== */
    @Deprecated
    public static void lwFreeObject(lwObject object) {
        if (object != null) {
//            lwListFree(object.layer, lwFreeLayer.getInstance());
//            lwListFree(object.env, lwFreeEnvelope.getInstance());
//            lwListFree(object.clip, lwFreeClip.getInstance());
//            lwListFree(object.surf, lwFreeSurface.getInstance());
//            lwFreeTags(object.taglist);
            object = null;
        }
    }


    /*
     ======================================================================
     lwGetObject()

     Returns the contents of a LightWave object, given its filename, or
     null if the file couldn't be loaded.  On failure, failID and failpos
     can be used to diagnose the cause.

     1.  If the file isn't an LWO2 or an LWOB, failpos will contain 12 and
     failID will be unchanged.

     2.  If an error occurs while reading, failID will contain the most
     recently read IFF chunk ID, and failpos will contain the value
     returned by fp.Tell() at the time of the failure.

     3.  If the file couldn't be opened, or an error occurs while reading
     the first 12 bytes, both failID and failpos will be unchanged.

     If you don't need this information, failID and failpos can be null.
     ====================================================================== */
    public static lwObject lwGetObject(final String filename, int[] failID, int[] failpos) {
        idFile fp;// = null;
        lwObject object;
        lwLayer layer;
        lwNode node;
        int id, formsize, type, cksize;
        int i, rlen;

        fp = fileSystem.OpenFileRead(filename);
        if (null == fp) {
            return null;
        }

        /* read the first 12 bytes */
        set_flen(0);
        id = getU4(fp);
        formsize = getU4(fp);
        type = getU4(fp);
        if (12 != get_flen()) {
            fileSystem.CloseFile(fp);
            return null;
        }

        /* is this a LW object? */
        if (id != ID_FORM) {
            fileSystem.CloseFile(fp);
            if (failpos != null) {
                failpos[0] = 12;
            }
            return null;
        }

        if (type != ID_LWO2) {
            fileSystem.CloseFile(fp);
            if (type == ID_LWOB) {
                return lwGetObject5(filename, failID, failpos);
            } else {
                if (failpos != null) {
                    failpos[0] = 12;
                }
                return null;
            }
        }

        Fail:
        /* allocate an object and a default layer */
        if (true) {
            object = new lwObject();// Mem_ClearedAlloc(sizeof(lwObject));
//            if (null == object) {
//                break Fail;
//            }

            layer = new lwLayer();// Mem_ClearedAlloc(sizeof(lwLayer));
//            if (null == layer) {
//                break Fail;
//            }
            object.layer = layer;

            object.timeStamp[0] = fp.Timestamp();

            /* get the first chunk header */
            id = getU4(fp);
            cksize = getU4(fp);
            if (0 > get_flen()) {
                break Fail;
            }

            /* process chunks as they're encountered */
            int j = 0;
            while (true) {
                j++;
                cksize += cksize & 1;

                switch (id) {
                    case ID_LAYR:
                        if (object.nlayers > 0) {
                            object.layer = layer = new lwLayer();// Mem_ClearedAlloc(sizeof(lwLayer));
//                            if (null == layer) {
//                                break Fail;
//                            }
                            object.layer = lwListAdd(object.layer, layer);
                        }
                        object.nlayers++;

                        set_flen(0);
                        layer.index = getU2(fp);
                        layer.flags = getU2(fp);
                        layer.pivot[0] = getF4(fp);
                        layer.pivot[1] = getF4(fp);
                        layer.pivot[2] = getF4(fp);
                        layer.name = getS0(fp);

                        rlen = get_flen();
                        if (rlen < 0 || rlen > cksize) {
                            break Fail;
                        }
                        if (rlen <= cksize - 2) {
                            layer.parent = getU2(fp);
                        }
                        rlen = get_flen();
                        if (rlen < cksize) {
                            fp.Seek(cksize - rlen, FS_SEEK_CUR);
                        }
                        break;

                    case ID_PNTS:
                        if (!lwGetPoints(fp, cksize, layer.point)) {
                            break Fail;
                        }
                        break;

                    case ID_POLS:
                        if (!lwGetPolygons(fp, cksize, layer.polygon, layer.point.offset)) {
                            break Fail;
                        }
                        break;

                    case ID_VMAP:
                    case ID_VMAD:
                        node = lwGetVMap(fp, cksize, layer.point.offset, layer.polygon.offset, id == ID_VMAD ? 1 : 0);
                        if (null == node) {
                            break Fail;
                        }
                        layer.vmap = lwListAdd(layer.vmap, node);
                        layer.nvmaps++;
                        break;

                    case ID_PTAG:
                        if (!lwGetPolygonTags(fp, cksize, object.taglist, layer.polygon)) {
                            break Fail;
                        }
                        break;

                    case ID_BBOX:
                        set_flen(0);
                        for (i = 0; i < 6; i++) {
                            layer.bbox[i] = getF4(fp);
                        }
                        rlen = get_flen();
                        if (rlen < 0 || rlen > cksize) {
                            break Fail;
                        }
                        if (rlen < cksize) {
                            fp.Seek(cksize - rlen, FS_SEEK_CUR);
                        }
                        break;

                    case ID_TAGS:
                        if (!lwGetTags(fp, cksize, object.taglist)) {
                            break Fail;
                        }
                        break;

                    case ID_ENVL:
                        node = (lwNode) lwGetEnvelope(fp, cksize);
                        if (null == node) {
                            break Fail;
                        }
                        object.env = lwListAdd(object.env, node);
                        object.nenvs++;
                        break;

                    case ID_CLIP:
                        node = (lwNode) lwGetClip(fp, cksize);
                        if (null == node) {
                            break Fail;
                        }
                        object.clip = lwListAdd(object.clip, node);
                        object.nclips++;
                        break;

                    case ID_SURF:
                        node = (lwNode) lwGetSurface(fp, cksize);
                        if (null == node) {
                            break Fail;
                        }
                        object.surf = lwListAdd(object.surf, node);
                        object.nsurfs++;
                        break;

                    case ID_DESC:
                    case ID_TEXT:
                    case ID_ICON:
                    default:
                        fp.Seek(cksize, FS_SEEK_CUR);
                        break;
                }

                /* end of the file? */
                if (formsize <= fp.Tell() - 8) {
                    break;
                }

                /* get the next chunk header */
                set_flen(0);
                id = getU4(fp);
                cksize = getU4(fp);
                if (8 != get_flen()) {
                    break Fail;
                }
            }

            fileSystem.CloseFile(fp);
            fp = null;

            if (object.nlayers == 0) {
                object.nlayers = 1;
            }

            layer = object.layer;
            while (layer != null) {
                lwGetBoundingBox(layer.point, layer.bbox);
                lwGetPolyNormals(layer.point, layer.polygon);
                if (!lwGetPointPolygons(layer.point, layer.polygon)) {
                    break Fail;
                }
                if (!lwResolvePolySurfaces(layer.polygon, object)) {
                    break Fail;
                }
                lwGetVertNormals(layer.point, layer.polygon);
                if (!lwGetPointVMaps(layer.point, layer.vmap)) {
                    break Fail;
                }
                if (!lwGetPolyVMaps(layer.polygon, layer.vmap)) {
                    break Fail;
                }
                layer = layer.next;
            }

            return object;
        }

//        Fail:
        if (failID != null) {
            failID[0] = id;
        }
        if (fp != null) {
            if (failpos != null) {
                failpos[0] = fp.Tell();
            }
            fileSystem.CloseFile(fp);
        }
//        lwFreeObject(object);
        return null;
    }

    /* IDs specific to LWOB */
    static final int ID_SRFS = ('S' << 24 | 'R' << 16 | 'F' << 8 | 'S');
//    static final int ID_FLAG = ('F' << 24 | 'L' << 16 | 'A' << 8 | 'G');
    static final int ID_VLUM = ('V' << 24 | 'L' << 16 | 'U' << 8 | 'M');
    static final int ID_VDIF = ('V' << 24 | 'D' << 16 | 'I' << 8 | 'F');
    static final int ID_VSPC = ('V' << 24 | 'S' << 16 | 'P' << 8 | 'C');
    static final int ID_RFLT = ('R' << 24 | 'F' << 16 | 'L' << 8 | 'T');
    static final int ID_BTEX = ('B' << 24 | 'T' << 16 | 'E' << 8 | 'X');
    static final int ID_CTEX = ('C' << 24 | 'T' << 16 | 'E' << 8 | 'X');
    static final int ID_DTEX = ('D' << 24 | 'T' << 16 | 'E' << 8 | 'X');
    static final int ID_LTEX = ('L' << 24 | 'T' << 16 | 'E' << 8 | 'X');
    static final int ID_RTEX = ('R' << 24 | 'T' << 16 | 'E' << 8 | 'X');
    static final int ID_STEX = ('S' << 24 | 'T' << 16 | 'E' << 8 | 'X');
    static final int ID_TTEX = ('T' << 24 | 'T' << 16 | 'E' << 8 | 'X');
    static final int ID_TFLG = ('T' << 24 | 'F' << 16 | 'L' << 8 | 'G');
    static final int ID_TSIZ = ('T' << 24 | 'S' << 16 | 'I' << 8 | 'Z');
    static final int ID_TCTR = ('T' << 24 | 'C' << 16 | 'T' << 8 | 'R');
    static final int ID_TFAL = ('T' << 24 | 'F' << 16 | 'A' << 8 | 'L');
    static final int ID_TVEL = ('T' << 24 | 'V' << 16 | 'E' << 8 | 'L');
    static final int ID_TCLR = ('T' << 24 | 'C' << 16 | 'L' << 8 | 'R');
    static final int ID_TVAL = ('T' << 24 | 'V' << 16 | 'A' << 8 | 'L');
//    static final int ID_TAMP = ('T' << 24 | 'A' << 16 | 'M' << 8 | 'P');
//    static final int ID_TIMG = ('T' << 24 | 'I' << 16 | 'M' << 8 | 'G');
    static final int ID_TAAS = ('T' << 24 | 'A' << 16 | 'A' << 8 | 'S');
    static final int ID_TREF = ('T' << 24 | 'R' << 16 | 'E' << 8 | 'F');
    static final int ID_TOPC = ('T' << 24 | 'O' << 16 | 'P' << 8 | 'C');
    static final int ID_SDAT = ('S' << 24 | 'D' << 16 | 'A' << 8 | 'T');
    static final int ID_TFP0 = ('T' << 24 | 'F' << 16 | 'P' << 8 | '0');
    static final int ID_TFP1 = ('T' << 24 | 'F' << 16 | 'P' << 8 | '1');

//    static {
//        ID_SRFS = LWID_('S', 'R', 'F', 'S');
////ID_FLAG= LWID_('F','L','A','G');
//        ID_VLUM = LWID_('V', 'L', 'U', 'M');
//        ID_VDIF = LWID_('V', 'D', 'I', 'F');
//        ID_VSPC = LWID_('V', 'S', 'P', 'C');
//        ID_RFLT = LWID_('R', 'F', 'L', 'T');
//        ID_BTEX = LWID_('B', 'T', 'E', 'X');
//        ID_CTEX = LWID_('C', 'T', 'E', 'X');
//        ID_DTEX = LWID_('D', 'T', 'E', 'X');
//        ID_LTEX = LWID_('L', 'T', 'E', 'X');
//        ID_RTEX = LWID_('R', 'T', 'E', 'X');
//        ID_STEX = LWID_('S', 'T', 'E', 'X');
//        ID_TTEX = LWID_('T', 'T', 'E', 'X');
//        ID_TFLG = LWID_('T', 'F', 'L', 'G');
//        ID_TSIZ = LWID_('T', 'S', 'I', 'Z');
//        ID_TCTR = LWID_('T', 'C', 'T', 'R');
//        ID_TFAL = LWID_('T', 'F', 'A', 'L');
//        ID_TVEL = LWID_('T', 'V', 'E', 'L');
//        ID_TCLR = LWID_('T', 'C', 'L', 'R');
//        ID_TVAL = LWID_('T', 'V', 'A', 'L');
////ID_TAMP= LWID_('T','A','M','P');
////ID_TIMG= LWID_('T','I','M','G');
//        ID_TAAS = LWID_('T', 'A', 'A', 'S');
//        ID_TREF = LWID_('T', 'R', 'E', 'F');
//        ID_TOPC = LWID_('T', 'O', 'P', 'C');
//        ID_SDAT = LWID_('S', 'D', 'A', 'T');
//        ID_TFP0 = LWID_('T', 'F', 'P', '0');
//        ID_TFP1 = LWID_('T', 'F', 'P', '1');
//    }
    /*
     ======================================================================
     add_clip()

     Add a clip to the clip list.  Used to store the contents of an RIMG or
     TIMG surface subchunk.
     ====================================================================== */
    public static int add_clip(String[] s, lwClip clist, int[] nclips) {
        lwClip clip;
        int p;

        clip = new lwClip();// Mem_ClearedAlloc(sizeof(lwClip));
        if (null == clip) {
            return 0;
        }

        clip.contrast.val = 1.0f;
        clip.brightness.val = 1.0f;
        clip.saturation.val = 1.0f;
        clip.gamma.val = 1.0f;

        if ((p = s[0].indexOf("(sequence)")) != 0) {
//      p[ -1 ] = 0;
            s[0] = replaceByIndex('\0', p, s[0]);
            clip.type = ID_ISEQ;
            clip.source.seq.prefix = s[0];
            clip.source.seq.digits = 3;
        } else {
            clip.type = ID_STIL;
            clip.source.still.name = s[0];
        }

        nclips[0]++;
        clip.index = nclips[0];

        clist = lwListAdd(clist, clip);

        return clip.index;
    }


    /*
     ======================================================================
     add_tvel()

     Add a triple of envelopes to simulate the old texture velocity
     parameters.
     ====================================================================== */
    public static int add_tvel(float pos[], float vel[], lwEnvelope elist, int[] nenvs) {
        lwEnvelope env = null;
        lwKey key0, key1;
        int i;

        for (i = 0; i < 3; i++) {
            env = new lwEnvelope();// Mem_ClearedAlloc(sizeof(lwEnvelope));
            key0 = new lwKey();// Mem_ClearedAlloc(sizeof(lwKey));
            key1 = new lwKey();// Mem_ClearedAlloc(sizeof(lwKey));
            if (null == env || null == key0 || null == key1) {
                return 0;
            }

            key0.next = key1;
            key0.value = pos[i];
            key0.time = 0.0f;
            key1.prev = key0;
            key1.value = pos[i] + vel[i] * 30.0f;
            key1.time = 1.0f;
            key0.shape = key1.shape = ID_LINE;

            env.index = nenvs[0] + i + 1;
            env.type = 0x0301 + i;
            env.name = "";//(String) Mem_ClearedAlloc(11);
            if (env.name != null) {
                env.name = "Position." + ('X' + i);
//                env.name = "Position.X";
//                env.name[9] += i;
            }
            env.key = key0;
            env.nkeys = 2;
            env.behavior[0] = BEH_LINEAR;
            env.behavior[1] = BEH_LINEAR;

            elist = lwListAdd(elist, env);
        }

        nenvs[0] += 3;
        return env.index - 2;
    }


    /*
     ======================================================================
     get_texture()

     Create a new texture for BTEX, CTEX, etc. subchunks.
     ====================================================================== */
    public static lwTexture get_texture(String s) {
        lwTexture tex;

        tex = new lwTexture();// Mem_ClearedAlloc(sizeof(lwTexture));
        if (null == tex) {
            return null;
        }

        tex.tmap.size.val[0]
                = tex.tmap.size.val[1]
                = tex.tmap.size.val[2] = 1.0f;
        tex.opacity.val = 1.0f;
        tex.enabled = 1;

        if (s.contains("Image Map")) {
            tex.type = ID_IMAP;
            if (s.contains("Planar")) {
                tex.param.imap.projection = 0;
            } else if (s.contains("Cylindrical")) {
                tex.param.imap.projection = 1;
            } else if (s.contains("Spherical")) {
                tex.param.imap.projection = 2;
            } else if (s.contains("Cubic")) {
                tex.param.imap.projection = 3;
            } else if (s.contains("Front")) {
                tex.param.imap.projection = 4;
            }
            tex.param.imap.aa_strength = 1.0f;
            tex.param.imap.amplitude.val = 1.0f;
//            Mem_Free(s);
        } else {
            tex.type = ID_PROC;
            tex.param.proc.name = s;
        }

        return tex;
    }


    /*
     ======================================================================
     lwGetSurface5()

     Read an lwSurface from an LWOB file.
     ====================================================================== */
    public static lwSurface lwGetSurface5(idFile fp, int cksize, lwObject obj) {
        lwSurface surf;
        lwTexture tex = new lwTexture();
        lwPlugin shdr = new lwPlugin();
        String[] s = {null};
        float[] v = new float[3];
        int id, flags;
        short sz;
        int pos, rlen, i = 0;


        /* allocate the Surface structure */
        surf = new lwSurface();// Mem_ClearedAlloc(sizeof(lwSurface));
        Fail:
        if (true) {
            if (NOT(surf)) {
                break Fail;
            }

            /* non-zero defaults */
            surf.color.rgb[0] = 0.78431f;
            surf.color.rgb[1] = 0.78431f;
            surf.color.rgb[2] = 0.78431f;
            surf.diffuse.val = 1.0f;
            surf.glossiness.val = 0.4f;
            surf.bump.val = 1.0f;
            surf.eta.val = 1.0f;
            surf.sideflags = 1;

            /* remember where we started */
            set_flen(0);
            pos = fp.Tell();

            /* name */
            surf.name = getS0(fp);

            /* first subchunk header */
            id = getU4(fp);
            sz = getU2(fp);
            if (0 > get_flen()) {
                break Fail;
            }

            /* process subchunks as they're encountered */
            while (true) {
                sz += sz & 1;
                set_flen(0);

                switch (id) {
                    case ID_COLR:
                        surf.color.rgb[0] = getU1(fp) / 255.0f;
                        surf.color.rgb[1] = getU1(fp) / 255.0f;
                        surf.color.rgb[2] = getU1(fp) / 255.0f;
                        break;

                    case ID_FLAG:
                        flags = getU2(fp);
                        if ((flags & 4) == 4) {
                            surf.smooth = 1.56207f;
                        }
                        if ((flags & 8) == 8) {
                            surf.color_hilite.val = 1.0f;
                        }
                        if ((flags & 16) == 16) {
                            surf.color_filter.val = 1.0f;
                        }
                        if ((flags & 128) == 128) {
                            surf.dif_sharp.val = 0.5f;
                        }
                        if ((flags & 256) == 256) {
                            surf.sideflags = 3;
                        }
                        if ((flags & 512) == 512) {
                            surf.add_trans.val = 1.0f;
                        }
                        break;

                    case ID_LUMI:
                        surf.luminosity.val = getI2(fp) / 256.0f;
                        break;

                    case ID_VLUM:
                        surf.luminosity.val = getF4(fp);
                        break;

                    case ID_DIFF:
                        surf.diffuse.val = getI2(fp) / 256.0f;
                        break;

                    case ID_VDIF:
                        surf.diffuse.val = getF4(fp);
                        break;

                    case ID_SPEC:
                        surf.specularity.val = getI2(fp) / 256.0f;
                        break;

                    case ID_VSPC:
                        surf.specularity.val = getF4(fp);
                        break;

                    case ID_GLOS:
                        surf.glossiness.val = (float) log(getU2(fp)) / 20.7944f;
                        break;

                    case ID_SMAN:
                        surf.smooth = getF4(fp);
                        break;

                    case ID_REFL:
                        surf.reflection.val.val = getI2(fp) / 256.0f;
                        break;

                    case ID_RFLT:
                        surf.reflection.options = getU2(fp);
                        break;

                    case ID_RIMG:
                        s[0] = getS0(fp);
                         {
                            int[] nclips = {obj.nclips};
                            surf.reflection.cindex = add_clip(s, obj.clip, nclips);
                            obj.nclips = nclips[0];
                        }
                        surf.reflection.options = 3;
                        break;

                    case ID_RSAN:
                        surf.reflection.seam_angle = getF4(fp);
                        break;

                    case ID_TRAN:
                        surf.transparency.val.val = getI2(fp) / 256.0f;
                        break;

                    case ID_RIND:
                        surf.eta.val = getF4(fp);
                        break;

                    case ID_BTEX:
                        s[0] = new String(getbytes(fp, sz));
                        tex = get_texture(s[0]);
                        surf.bump.tex = lwListAdd(surf.bump.tex, tex);
                        break;

                    case ID_CTEX:
                        s[0] = new String(getbytes(fp, sz));
                        tex = get_texture(s[0]);
                        surf.color.tex = lwListAdd(surf.color.tex, tex);
                        break;

                    case ID_DTEX:
                        s[0] = new String(getbytes(fp, sz));
                        tex = get_texture(s[0]);
                        surf.diffuse.tex = lwListAdd(surf.diffuse.tex, tex);
                        break;

                    case ID_LTEX:
                        s[0] = new String(getbytes(fp, sz));
                        tex = get_texture(s[0]);
                        surf.luminosity.tex = lwListAdd(surf.luminosity.tex, tex);
                        break;

                    case ID_RTEX:
                        s[0] = new String(getbytes(fp, sz));
                        tex = get_texture(s[0]);
                        surf.reflection.val.tex = lwListAdd(surf.reflection.val.tex, tex);
                        break;

                    case ID_STEX:
                        s[0] = new String(getbytes(fp, sz));
                        tex = get_texture(s[0]);
                        surf.specularity.tex = lwListAdd(surf.specularity.tex, tex);
                        break;

                    case ID_TTEX:
                        s[0] = new String(getbytes(fp, sz));
                        tex = get_texture(s[0]);
                        surf.transparency.val.tex = lwListAdd(surf.transparency.val.tex, tex);
                        break;

                    case ID_TFLG:
                        flags = getU2(fp);

                        if ((flags & 1) == 1) {
                            i = 0;
                        }
                        if ((flags & 2) == 2) {
                            i = 1;
                        }
                        if ((flags & 4) == 4) {
                            i = 2;
                        }
                        tex.axis = (short) i;
                        if (tex.type == ID_IMAP) {
                            tex.param.imap.axis = i;
                        } else {
                            tex.param.proc.axis = i;
                        }

                        if ((flags & 8) == 8) {
                            tex.tmap.coord_sys = 1;
                        }
                        if ((flags & 16) == 16) {
                            tex.negative = 1;
                        }
                        if ((flags & 32) == 32) {
                            tex.param.imap.pblend = 1;
                        }
                        if ((flags & 64) == 64) {
                            tex.param.imap.aa_strength = 1.0f;
                            tex.param.imap.aas_flags = 1;
                        }
                        break;

                    case ID_TSIZ:
                        for (i = 0; i < 3; i++) {
                            tex.tmap.size.val[i] = getF4(fp);
                        }
                        break;

                    case ID_TCTR:
                        for (i = 0; i < 3; i++) {
                            tex.tmap.center.val[i] = getF4(fp);
                        }
                        break;

                    case ID_TFAL:
                        for (i = 0; i < 3; i++) {
                            tex.tmap.falloff.val[i] = getF4(fp);
                        }
                        break;

                    case ID_TVEL:
                        for (i = 0; i < 3; i++) {
                            v[i] = getF4(fp);
                        }
                         {
                            int[] nenvs = {obj.nenvs};
                            tex.tmap.center.eindex = add_tvel(tex.tmap.center.val, v, obj.env, nenvs);
                            obj.nenvs = nenvs[0];
                        }
                        break;

                    case ID_TCLR:
                        if (tex.type == ID_PROC) {
                            for (i = 0; i < 3; i++) {
                                tex.param.proc.value[i] = getU1(fp) / 255.0f;
                            }
                        }
                        break;

                    case ID_TVAL:
                        tex.param.proc.value[0] = getI2(fp) / 256.0f;
                        break;

                    case ID_TAMP:
                        if (tex.type == ID_IMAP) {
                            tex.param.imap.amplitude.val = getF4(fp);
                        }
                        break;

                    case ID_TIMG:
                        s[0] = getS0(fp);
                         {
                            int[] nClips = {obj.nclips};
                            tex.param.imap.cindex = add_clip(s, obj.clip, nClips);
                            obj.nclips = nClips[0];
                        }
                        break;

                    case ID_TAAS:
                        tex.param.imap.aa_strength = getF4(fp);
                        tex.param.imap.aas_flags = 1;
                        break;

                    case ID_TREF:
                        tex.tmap.ref_object = new String(getbytes(fp, sz));
                        break;

                    case ID_TOPC:
                        tex.opacity.val = getF4(fp);
                        break;

                    case ID_TFP0:
                        if (tex.type == ID_IMAP) {
                            tex.param.imap.wrapw.val = getF4(fp);
                        }
                        break;

                    case ID_TFP1:
                        if (tex.type == ID_IMAP) {
                            tex.param.imap.wraph.val = getF4(fp);
                        }
                        break;

                    case ID_SHDR:
                        shdr = new lwPlugin();// Mem_ClearedAlloc(sizeof(lwPlugin));
                        if (null == shdr) {
                            break Fail;
                        }
                        shdr.name = new String(getbytes(fp, sz));
                        surf.shader = lwListAdd(surf.shader, shdr);
                        surf.nshaders++;
                        break;

                    case ID_SDAT:
                        shdr.data = getbytes(fp, sz);
                        break;

                    default:
                        break;
                }

                /* error while reading current subchunk? */
                rlen = get_flen();
                if (rlen < 0 || rlen > sz) {
                    break Fail;
                }

                /* skip unread parts of the current subchunk */
                if (rlen < sz) {
                    fp.Seek(sz - rlen, FS_SEEK_CUR);
                }

                /* end of the SURF chunk? */
                if (cksize <= fp.Tell() - pos) {
                    break;
                }

                /* get the next subchunk header */
                set_flen(0);
                id = getU4(fp);
                sz = getU2(fp);
                if (6 != get_flen()) {
                    break Fail;
                }
            }

            return surf;
        }

//        Fail:
        if (surf != null) {
            lwFreeSurface.getInstance().run(surf);
        }
        return null;
    }


    /*
     ======================================================================
     lwGetPolygons5()

     Read polygon records from a POLS chunk in an LWOB file.  The polygons
     are added to the array in the lwPolygonList.
     ====================================================================== */
    public static boolean lwGetPolygons5(idFile fp, int cksize, lwPolygonList plist, int ptoffset) {
        lwPolygon pp;
//        lwPolVert pv;
        ByteBuffer buf;
        int i, j, nv, nverts, npols;
        int p, v;

        if (cksize == 0) {
            return true;
        }

        /* read the whole chunk */
        set_flen(0);
        buf = ByteBuffer.wrap(getbytes(fp, cksize));
        Fail:
        if (true) {
            if (null == buf) {
                break Fail;
            }

            /* count the polygons and vertices */
            nverts = 0;
            npols = 0;
//            buf = buf;

            while (buf.position() < cksize) {
                nv = sgetU2(buf);
                nverts += nv;
                npols++;
                buf.position(buf.position() + 2 * nv);
                i = sgetI2(buf);
                if (i < 0) {
                    buf.position(buf.position() + 2);      // detail polygons 
                }
            }

            if (!lwAllocPolygons(plist, npols, nverts)) {
                break Fail;
            }

            /* fill in the new polygons */
//            buf = buf;
            pp = plist.pol[p = plist.offset];
//            pv = plist.pol[0].v[v = plist.voffset];
            v = plist.voffset;

            for (i = 0; i < npols; i++) {
                nv = sgetU2(buf);

                pp.nverts = nv;
                pp.type = ID_FACE;
                if (null == pp.v) {
                    pp.setV(plist.pol[0].v, v);
                }
                for (j = 0; j < nv; j++) {
                    plist.pol[0].getV(v + j).index = sgetU2(buf) + ptoffset;
                }
                j = sgetI2(buf);
                if (j < 0) {
                    j = -j;
                    buf.position(buf.position() + 2);
                }
                j -= 1;
                pp.surf = (lwSurface) lwNode.getPosition(pp.surf, j);

                pp = plist.pol[p++];
//                pv = plist.pol[0].v[v += nv];
                v += nv;
            }

//            buf=null
            return true;
        }

//        Fail:
//        if (buf != null) {
//            buf=null
//        }
        lwFreePolygons(plist);
        return false;
    }


    /*
     ======================================================================
     getLWObject5()

     Returns the contents of an LWOB, given its filename, or null if the
     file couldn't be loaded.  On failure, failID and failpos can be used
     to diagnose the cause.

     1.  If the file isn't an LWOB, failpos will contain 12 and failID will
     be unchanged.

     2.  If an error occurs while reading an LWOB, failID will contain the
     most recently read IFF chunk ID, and failpos will contain the
     value returned by fp.Tell() at the time of the failure.

     3.  If the file couldn't be opened, or an error occurs while reading
     the first 12 bytes, both failID and failpos will be unchanged.

     If you don't need this information, failID and failpos can be null.
     ====================================================================== */
    public static lwObject lwGetObject5(final String filename, int[] failID, int[] failpos) {
        idFile fp;
        lwObject object;
        lwLayer layer;
        lwNode node;
        int id, formsize, type, cksize;


        /* open the file */
        //fp = fopen( filename, "rb" );
        //if ( !fp ) return null;

        /* read the first 12 bytes */
        fp = fileSystem.OpenFileRead(filename);
        if (null == fp) {
            return null;
        }

        set_flen(0);
        id = getU4(fp);
        formsize = getU4(fp);
        type = getU4(fp);
        if (12 != get_flen()) {
            fileSystem.CloseFile(fp);
            return null;
        }

        /* LWOB? */
        if (id != ID_FORM || type != ID_LWOB) {
            fileSystem.CloseFile(fp);
            if (failpos != null) {
                failpos[0] = 12;
            }
            return null;
        }

        Fail:
        /* allocate an object and a default layer */
        if (true) {
            object = new lwObject();// Mem_ClearedAlloc(sizeof(lwObject));
            if (null == object) {
                break Fail;
            }

            layer = new lwLayer();// Mem_ClearedAlloc(sizeof(lwLayer));
            if (null == layer) {
                break Fail;
            }
            object.layer = layer;
            object.nlayers = 1;

            /* get the first chunk header */
            id = getU4(fp);
            cksize = getU4(fp);
            if (0 > get_flen()) {
                break Fail;
            }

            /* process chunks as they're encountered */
            while (true) {
                cksize += cksize & 1;

                switch (id) {
                    case ID_PNTS:
                        if (!lwGetPoints(fp, cksize, layer.point)) {
                            break Fail;
                        }
                        break;

                    case ID_POLS:
                        if (!lwGetPolygons5(fp, cksize, layer.polygon,
                                layer.point.offset)) {
                            break Fail;
                        }
                        break;

                    case ID_SRFS:
                        if (!lwGetTags(fp, cksize, object.taglist)) {
                            break Fail;
                        }
                        break;

                    case ID_SURF:
                        node = (lwNode) lwGetSurface5(fp, cksize, object);
                        if (null == node) {
                            break Fail;
                        }
                        object.surf = lwListAdd(object.surf, node);
                        object.nsurfs++;
                        break;

                    default:
                        fp.Seek(cksize, FS_SEEK_CUR);
                        break;
                }

                /* end of the file? */
                if (formsize <= fp.Tell() - 8) {
                    break;
                }

                /* get the next chunk header */
                set_flen(0);
                id = getU4(fp);
                cksize = getU4(fp);
                if (8 != get_flen()) {
                    break Fail;
                }
            }

            fileSystem.CloseFile(fp);
            fp = null;

            lwGetBoundingBox(layer.point, layer.bbox);
            lwGetPolyNormals(layer.point, layer.polygon);
            if (!lwGetPointPolygons(layer.point, layer.polygon)) {
                break Fail;
            }
            if (!lwResolvePolySurfaces(layer.polygon, object)) {
                break Fail;
            }
            lwGetVertNormals(layer.point, layer.polygon);

            return object;
        }

//        Fail2:
        if (failID != null) {
            failID[0] = id;
        }
        if (fp != null) {
            if (failpos != null) {
                failpos[0] = fp.Tell();
            }
            fileSystem.CloseFile(fp);
        }
//        lwFreeObject(object);
        return null;
    }

    /*
     ======================================================================
     lwFreePoints()

     Free the memory used by an lwPointList.
     ====================================================================== */
    public static void lwFreePoints(lwPointList point) {
        int i;

        if (point != null) {
            if (point.pt != null) {
//                for (i = 0; i < point.count; i++) {
//                    if (point.pt[ i].pol != null) {
//                        Mem_Free(point.pt[ i].pol);
//                    }
//                    if (point.pt[ i].vm != null) {
//                        Mem_Free(point.pt[ i].vm);
//                    }
//                }
//                Mem_Free(point.pt);
                point.pt = null;
            }
//            memset(point, 0, sizeof(lwPointList));
        }
    }


    /*
     ======================================================================
     lwFreePolygons()

     Free the memory used by an lwPolygonList.
     ====================================================================== */
    public static void lwFreePolygons(lwPolygonList plist) {
        int i, j;

        if (plist != null) {
            if (plist.pol != null) {
//                for (i = 0; i < plist.count; i++) {
//                    if (plist.pol[ i].v != null) {
//                        for (j = 0; j < plist.pol[ i].nverts; j++) {
//                            if (plist.pol[ i].v[ j].vm != null) {
//                                Mem_Free(plist.pol[ i].v[ j].vm);
//                            }
//                        }
//                    }
//                }
//                if (plist.pol[ 0].v != null) {
//                    Mem_Free(plist.pol[ 0].v);
//                }
//                Mem_Free(plist.pol);
                plist.pol = null;
            }
//            memset(plist, 0, sizeof(lwPolygonList));
        }
    }


    /*
     ======================================================================
     lwGetPoints()

     Read point records from a PNTS chunk in an LWO2 file.  The points are
     added to the array in the lwPointList.
     ====================================================================== */
    public static boolean lwGetPoints(idFile fp, int cksize, lwPointList point) {
        ByteBuffer f;
        int np, i, j;

        if (cksize == 1) {
            return true;
        }

        /* extend the point array to hold the new points */
        np = cksize / 12;
        point.offset = point.count;
        point.count += np;
        lwPoint[] oldpt = point.pt;
        point.pt = new lwPoint[point.count];// Mem_Alloc(point.count);
        if (null == point.pt) {
            return false;
        }
        if (oldpt != null) {
//            memcpy(point.pt, oldpt, point.offset * sizeof(lwPoint));
            System.arraycopy(oldpt, 0, point.pt, 0, point.offset);
//            Mem_Free(oldpt);
            oldpt = null;
        }
//	memset( &point.pt[ point.offset ], 0, np * sizeof( lwPoint ) );
        for (int n = point.offset; n < np; n++) {
            point.pt[n] = new lwPoint();
        }

        /* read the whole chunk */
        f = ByteBuffer.wrap(getbytes(fp, cksize));
        if (null == f) {
            return false;
        }
        BigRevBytes(f, /*4,*/ np * 3);

        /* assign position values */
        for (i = 0, j = 0; i < np; i++, j += 3) {
            point.pt[i].pos[0] = f.getFloat();//f[ j ];
            point.pt[i].pos[1] = f.getFloat();//f[ j + 1 ];
            point.pt[i].pos[2] = f.getFloat();//f[ j + 2 ];
        }

//        Mem_Free(f);
        return true;
    }


    /*
     ======================================================================
     lwGetBoundingBox()

     Calculate the bounding box for a point list, but only if the bounding
     box hasn't already been initialized.
     ====================================================================== */
    public static void lwGetBoundingBox(lwPointList point, float[] bbox) {
        int i, j;

        if (point.count == 0) {
            return;
        }

        for (i = 0; i < 6; i++) {
            if (bbox[i] != 0.0f) {
                return;
            }
        }

        bbox[0] = bbox[1] = bbox[2] = 1e20f;
        bbox[3] = bbox[4] = bbox[5] = -1e20f;
        for (i = 0; i < point.count; i++) {
            for (j = 0; j < 3; j++) {
                if (bbox[j] > point.pt[i].pos[j]) {
                    bbox[j] = point.pt[i].pos[j];
                }
                if (bbox[j + 3] < point.pt[i].pos[j]) {
                    bbox[j + 3] = point.pt[i].pos[j];
                }
            }
        }
    }


    /*
     ======================================================================
     lwAllocPolygons()

     Allocate or extend the polygon arrays to hold new records.
     ====================================================================== */
    public static boolean lwAllocPolygons(lwPolygonList plist, int npols, int nverts) {
        int i;

        plist.offset = plist.count;
        plist.count += npols;
        lwPolygon[] oldpol = plist.pol;
        plist.pol = new lwPolygon[plist.count];// Mem_Alloc(plist.count);
//        if (null == plist.pol) {
//            return false;
//        }
        if (oldpol != null) {
//            memcpy(plist.pol, oldpol, plist.offset);
            System.arraycopy(oldpol, 0, plist.pol, 0, plist.offset);
//            Mem_Free(oldpol);
            oldpol = null;
        }
//        memset(plist.pol + plist.offset, 0, npols);
        for (i = 0; i < npols; i++) {
            plist.pol[plist.offset + i] = new lwPolygon();
        }

        plist.voffset = plist.vcount;
        plist.vcount += nverts;
        lwPolVert[] oldpolv = plist.pol[0].v;
        plist.pol[0].v = new lwPolVert[plist.vcount];// Mem_Alloc(plist.vcount);
        if (null == plist.pol[0].v) {
            return false;
        }
        if (oldpolv != null) {
//            memcpy(plist.pol[0].v, oldpolv, plist.voffset);
            System.arraycopy(oldpolv, 0, plist.pol[0].v, 0, plist.voffset);
            oldpolv = null;//Mem_Free(oldpolv);
        }
//        memset(plist.pol[ 0].v + plist.voffset, 0, nverts);
        for (i = 0; i < nverts; i++) {
            plist.pol[0].v[plist.voffset + i] = new lwPolVert();
        }

        /* fix up the old vertex pointers */
        for (i = 1; i < plist.offset; i++) {
            for (int j = 0; j < plist.pol[i].v.length; j++) {
//            plist.pol[i].v = plist.pol[i - 1].v + plist.pol[i - 1].nverts;
                plist.pol[i].v[j] = new lwPolVert();//TODO:simplify.
            }
        }

        return true;
    }


    /*
     ======================================================================
     lwGetPolygons()

     Read polygon records from a POLS chunk in an LWO2 file.  The polygons
     are added to the array in the lwPolygonList.
     ====================================================================== */
    public static boolean lwGetPolygons(idFile fp, int cksize, lwPolygonList plist, int ptoffset) {
        lwPolygon pp;
//        lwPolVert pv;
        ByteBuffer buf;
        int i, j, flags, nv, nverts, npols;
        int p, v;
        int type;

        if (cksize == 0) {
            return true;
        }

        /* read the whole chunk */
        set_flen(0);
        type = getU4(fp);
        buf = ByteBuffer.wrap(getbytes(fp, cksize - 4));

        Fail:
        if (true) {
            if (cksize != get_flen()) {
                break Fail;
            }

            /* count the polygons and vertices */
            nverts = 0;
            npols = 0;
//            buf = buf;

            while (buf.hasRemaining()) {//( bp < buf + cksize - 4 ) {
                nv = sgetU2(buf);
                nv &= 0x03FF;
                nverts += nv;
                npols++;
                for (i = 0; i < nv; i++) {
                    j = sgetVX(buf);
                }
            }

            if (!lwAllocPolygons(plist, npols, nverts)) {
                break Fail;
            }

            /* fill in the new polygons */
            buf.rewind();//bp = buf;
            p = plist.offset;
//            pv = plist.pol[0].v[v = plist.voffset];
            v = plist.voffset;

            for (i = 0; i < npols; i++) {
                nv = sgetU2(buf);
                flags = nv & 0xFC00;
                nv &= 0x03FF;

                pp = plist.pol[p++];
                pp.nverts = nv;
                pp.flags = flags;
                pp.type = type;
                if (null == pp.v) {
                    pp.setV(plist.pol[0].v, v);
                }
                for (j = 0; j < nv; j++) {
                    pp.getV(j).index = sgetVX(buf) + ptoffset;
//                    System.out.println(pp.getV(j).index);
                }

//                pv = plist.pol[0].v[v += nv];
                v += nv;
            }

            buf = null;
            return true;
        }

        Fail:
        if (buf != null) {
            buf = null;
        }
        lwFreePolygons(plist);
        return false;
    }


    /*
     ======================================================================
     lwGetPolyNormals()

     Calculate the polygon normals.  By convention, LW's polygon normals
     are found as the cross product of the first and last edges.  It's
     undefined for one- and two-point polygons.
     ====================================================================== */
    public static void lwGetPolyNormals(lwPointList point, lwPolygonList polygon) {
        int i, j;
        float[] p1 = new float[3], p2 = new float[3], pn = new float[3],
                v1 = new float[3], v2 = new float[3];

        for (i = 0; i < polygon.count; i++) {
            if (polygon.pol[i].nverts < 3) {
                continue;
            }
            for (j = 0; j < 3; j++) {

                // FIXME: track down why indexes are way out of range
                p1[j] = point.pt[polygon.pol[i].getV(0).index].pos[j];
                p2[j] = point.pt[polygon.pol[i].getV(1).index].pos[j];
                pn[j] = point.pt[polygon.pol[i].getV(polygon.pol[i].nverts - 1).index].pos[j];
            }

            for (j = 0; j < 3; j++) {
                v1[j] = p2[j] - p1[j];
                v2[j] = pn[j] - p1[j];
            }

            cross(v1, v2, polygon.pol[i].norm);
            normalize(polygon.pol[i].norm);
        }
    }


    /*
     ======================================================================
     lwGetPointPolygons()

     For each point, fill in the indexes of the polygons that share the
     point.  Returns 0 if any of the memory allocations fail, otherwise
     returns 1.
     ====================================================================== */
    public static boolean lwGetPointPolygons(lwPointList point, lwPolygonList polygon) {
        int i, j, k;

        /* count the number of polygons per point */
        for (i = 0; i < polygon.count; i++) {
            for (j = 0; j < polygon.pol[i].nverts; j++) {
                ++point.pt[polygon.pol[i].getV(j).index].npols;
            }
        }

        /* alloc per-point polygon arrays */
        for (i = 0; i < point.count; i++) {
            if (point.pt[i].npols == 0) {
                continue;
            }
            point.pt[i].pol = new int[point.pt[i].npols];// Mem_ClearedAlloc(point.pt[ i].npols);
            if (null == point.pt[i].pol) {
                return false;
            }
            point.pt[i].npols = 0;
        }

        /* fill in polygon array for each point */
        for (i = 0; i < polygon.count; i++) {
            for (j = 0; j < polygon.pol[i].nverts; j++) {
                k = polygon.pol[i].getV(j).index;
                point.pt[k].pol[point.pt[k].npols] = i;
                ++point.pt[k].npols;
            }
        }

        return true;
    }


    /*
     ======================================================================
     lwResolvePolySurfaces()

     Convert tag indexes into actual lwSurface pointers.  If any polygons
     point to tags for which no corresponding surface can be found, a
     default surface is created.
     ====================================================================== */
    public static boolean lwResolvePolySurfaces(lwPolygonList polygon, lwObject object) {
        lwSurface[] s;
        lwSurface st;
        int i, index;
        lwTagList tlist = object.taglist;
        lwSurface surf = object.surf;

        if (tlist.count == 0) {
            return true;
        }

        s = new lwSurface[tlist.count];// Mem_ClearedAlloc(tlist.count);
//        if (null == s) {
//            return 0;
//        }

        for (i = 0; i < tlist.count; i++) {
            st = surf;
            while (st != null) {
                if (st.name != null && st.name.equals(tlist.tag[i])) {
                    s[i] = st;
                    break;
                }
                st = st.next;
            }
        }

        for (i = 0; i < polygon.count; i++) {
            index = polygon.pol[i].part;
            if (index < 0 || index > tlist.count) {
                return false;
            }
            if (null == s[index]) {
                s[index] = lwDefaultSurface();
                if (null == s[index]) {
                    return false;
                }
                s[index].name = "";//(String) Mem_ClearedAlloc(tlist.tag[ index].length() + 1);
                if (null == s[index].name) {
                    return false;
                }
                s[index].name = tlist.tag[index];
                surf = lwListAdd(surf, s[index]);
                object.nsurfs++;
            }
//            polygon.pol[ i].surf.oSet(s[ index]);
            polygon.pol[i].surf = s[index];//TODO:should this be an oSet() to preserve the refs?
        }

        s = null;
        return true;
    }


    /*
     ======================================================================
     lwGetVertNormals()

     Calculate the vertex normals.  For each polygon vertex, sum the
     normals of the polygons that share the point.  If the normals of the
     current and adjacent polygons form an angle greater than the max
     smoothing angle for the current polygon's surface, the normal of the
     adjacent polygon is excluded from the sum.  It's also excluded if the
     polygons aren't in the same smoothing group.

     Assumes that lwGetPointPolygons(), lwGetPolyNormals() and
     lwResolvePolySurfaces() have already been called.
     ====================================================================== */
    public static void lwGetVertNormals(lwPointList point, lwPolygonList polygon) {
        int j, k, n, g, h, p;
        float a;

        for (j = 0; j < polygon.count; j++) {
            for (n = 0; n < polygon.pol[j].nverts; n++) {
                for (k = 0; k < 3; k++) {
                    polygon.pol[j].getV(n).norm[k] = polygon.pol[j].norm[k];
                }

                if (polygon.pol[j].surf.smooth <= 0) {
                    continue;
                }

                p = polygon.pol[j].getV(n).index;

                for (g = 0; g < point.pt[p].npols; g++) {
                    h = point.pt[p].pol[g];
                    if (h == j) {
                        continue;
                    }

                    if (polygon.pol[j].smoothgrp != polygon.pol[h].smoothgrp) {
                        continue;
                    }
                    a = idMath.ACos(dot(polygon.pol[j].norm, polygon.pol[h].norm));
                    if (a > polygon.pol[j].surf.smooth) {
                        continue;
                    }

                    for (k = 0; k < 3; k++) {
                        polygon.pol[j].getV(n).norm[k] += polygon.pol[h].norm[k];
                    }
                }

                normalize(polygon.pol[j].getV(n).norm);
            }
        }
    }


    /*
     ======================================================================
     lwFreeTags()

     Free memory used by an lwTagList.
     ====================================================================== */
    public static void lwFreeTags(lwTagList tlist) {
        int i;

        if (tlist != null) {
            if (tlist.tag != null) {
//                for (i = 0; i < tlist.count; i++) {
//                    if (tlist.tag[ i] != null) {
//                        Mem_Free(tlist.tag[ i]);
//                    }
//                }
                tlist.tag = null;
            }
//            memset(tlist, 0, sizeof(lwTagList));
        }
    }


    /*
     ======================================================================
     lwGetTags()

     Read tag strings from a TAGS chunk in an LWO2 file.  The tags are
     added to the lwTagList array.
     ====================================================================== */
    public static boolean lwGetTags(idFile fp, int ckSize, lwTagList tList) {
        final ByteBuffer buf;
        final int nTags;
        final String bp;
        final String[] tags;

        if (ckSize == 0) {
            return true;
        }

        /* read the whole chunk */
        set_flen(0);
        buf = ByteBuffer.wrap(getbytes(fp, ckSize));
        if (null == buf) {
            return false;
        }

        /* count the strings */
        bp = new String(buf.array());
        tags = bp.split("\0+");//TODO:make sure we don't need the \0?
        nTags = tags.length;

        /* expand the string array to hold the new tags */
        tList.offset = tList.count;
        tList.count += nTags;
        final String[] oldtag = tList.tag;
        tList.tag = new String[tList.count];
        if (tList.count == 0) {
            return false;
        }
        if (oldtag != null) {
            System.arraycopy(oldtag, 0, tList.tag, 0, tList.offset);
        }

        /* copy the new tags to the tag array */
        System.arraycopy(tags, 0, tList.tag, tList.offset, nTags);

        return true;
    }


    /*
     ======================================================================
     lwGetPolygonTags()

     Read polygon tags from a PTAG chunk in an LWO2 file.
     ====================================================================== */
    public static boolean lwGetPolygonTags(idFile fp, int cksize, lwTagList tlist, lwPolygonList plist) {
        int type;
        int rlen = 0, i, j;
        Map<Integer, lwNode> nodeMap = new HashMap<>(2);

        set_flen(0);
        type = getU4(fp);
        rlen = get_flen();
        if (rlen < 0) {
            return false;
        }

        if (type != ID_SURF && type != ID_PART && type != ID_SMGP) {
            fp.Seek(cksize - 4, FS_SEEK_CUR);
            return true;
        }

        while (rlen < cksize) {
            i = getVX(fp) + plist.offset;
            j = getVX(fp) + tlist.offset;
            rlen = get_flen();
            if (rlen < 0 || rlen > cksize) {
                return false;
            }

            //add static reference if it doesthnt exist.
            if (!nodeMap.containsKey(j)) {
                nodeMap.put(j, new lwSurface());
            }

            switch (type) {
                case ID_SURF:
                    plist.pol[i].surf = (lwSurface) nodeMap.get(j);
//                    break;//use the part instead of "(int)plist.pol[i].surf".
                case ID_PART:
                    plist.pol[i].part = j;
                    break;
                case ID_SMGP:
                    plist.pol[i].smoothgrp = j;
            }
        }

        return true;
    }


    /*
     ======================================================================
     lwFreePlugin()

     Free the memory used by an lwPlugin.
     ====================================================================== */
    public static class lwFreePlugin extends LW {

        private static final LW instance = new lwFreePlugin();

        private lwFreePlugin() {
        }

        public static LW getInstance() {
            return instance;
        }

        @Override
        public void run(Object o) {
            throw new TODO_Exception();
//            lwPlugin p = (lwPlugin) o;
//            if (p != null) {
//                if (p.ord != null) {
//                    Mem_Free(p.ord);
//                }
//                if (p.name != null) {
//                    Mem_Free(p.name);
//                }
//                if (p.data != null) {
//                    Mem_Free(p.data);
//                }
//                Mem_Free(p);
//            }
        }
    };


    /*
     ======================================================================
     lwFreeTexture()

     Free the memory used by an lwTexture.
     ====================================================================== */
    public static class lwFreeTexture extends LW {

        private static final LW instance = new lwFreeTexture();

        private lwFreeTexture() {
        }

        public static LW getInstance() {
            return instance;
        }

        @Override
        public void run(Object p) {
            lwTexture t = (lwTexture) p;
            if (t != null) {
                if (t.ord != null) {
//                    Mem_Free(t.ord);
                    t.ord = null;
                }
                switch ((int) t.type) {
                    case ID_IMAP:
                        if (t.param.imap.vmap_name != null) {
//                            Mem_Free(t.param.imap.vmap_name);
                            t.param.imap.vmap_name = null;
                        }
                        break;
                    case ID_PROC:
                        if (t.param.proc.name != null) {
//                            Mem_Free(t.param.proc.name);
                            t.param.proc.name = null;
                        }
                        if (t.param.proc.data != null) {
//                            Mem_Free(t.param.proc.data);
                            t.param.proc.data = null;
                        }
                        break;
                    case ID_GRAD:
                        if (t.param.grad.key != null) {
//                            Mem_Free(t.param.grad.key);
                            t.param.grad.key = null;
                        }
                        if (t.param.grad.ikey != null) {
//                            Mem_Free(t.param.grad.ikey);
                            t.param.grad.ikey = null;
                        }
                        break;
                }
                if (t.tmap.ref_object != null) {
//                    Mem_Free(t.tmap.ref_object);
                    t.tmap.ref_object = null;
                }
                t = null;
            }
        }
    };


    /*
     ======================================================================
     lwFreeSurface()

     Free the memory used by an lwSurface.
     ====================================================================== */
    public static class lwFreeSurface extends LW {

        private static final LW instance = new lwFreeSurface();

        private lwFreeSurface() {
        }

        public static LW getInstance() {
            return instance;
        }

        @Override
        public void run(Object p) {
            lwSurface surf = (lwSurface) p;
            if (surf != null) {
                if (surf.name != null) {
                    surf.name = null;
                }
                if (surf.srcname != null) {
                    surf.srcname = null;
                }
//
//                lwListFree(surf.shader, lwFreePlugin.getInstance());
//
//                lwListFree(surf.color.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.luminosity.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.diffuse.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.specularity.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.glossiness.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.reflection.val.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.transparency.val.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.eta.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.translucency.tex, lwFreeTexture.getInstance());
//                lwListFree(surf.bump.tex, lwFreeTexture.getInstance());
//
                surf = null;
            }
        }
    };


    /*
     ======================================================================
     lwGetTHeader()

     Read a texture map header from a SURF.BLOK in an LWO2 file.  This is
     the first subchunk in a BLOK, and its contents are common to all three
     texture types.
     ====================================================================== */
    public static int lwGetTHeader(idFile fp, int hsz, lwTexture tex) {
        int id;
        short sz;
        int pos, rlen;


        /* remember where we started */
        set_flen(0);
        pos = fp.Tell();

        /* ordinal string */
        tex.ord = getS0(fp);

        /* first subchunk header */
        id = getU4(fp);
        sz = getU2(fp);
        if (0 > get_flen()) {
            return 0;
        }

        /* process subchunks as they're encountered */
        while (true) {
            sz += sz & 1;
            set_flen(0);

            switch (id) {
                case ID_CHAN:
                    tex.chan = getU4(fp);
                    break;

                case ID_OPAC:
                    tex.opac_type = getU2(fp);
                    tex.opacity.val = getF4(fp);
                    tex.opacity.eindex = getVX(fp);
                    break;

                case ID_ENAB:
                    tex.enabled = getU2(fp);
                    break;

                case ID_NEGA:
                    tex.negative = getU2(fp);
                    break;

                case ID_AXIS:
                    tex.axis = getU2(fp);
                    break;

                default:
                    break;
            }

            /* error while reading current subchunk? */
            rlen = get_flen();
            if (rlen < 0 || rlen > sz) {
                return 0;
            }

            /* skip unread parts of the current subchunk */
            if (rlen < sz) {
                fp.Seek(sz - rlen, FS_SEEK_CUR);
            }

            /* end of the texture header subchunk? */
            if (hsz <= fp.Tell() - pos) {
                break;
            }

            /* get the next subchunk header */
            set_flen(0);
            id = getU4(fp);
            sz = getU2(fp);
            if (6 != get_flen()) {
                return 0;
            }
        }

        set_flen(fp.Tell() - pos);
        return 1;
    }


    /*
     ======================================================================
     lwGetTMap()

     Read a texture map from a SURF.BLOK in an LWO2 file.  The TMAP
     defines the mapping from texture to world or object coordinates.
     ====================================================================== */
    public static int lwGetTMap(idFile fp, int tmapsz, lwTMap tmap) {
        int id;
        short sz;
        int rlen, pos, i;

        pos = fp.Tell();
        id = getU4(fp);
        sz = getU2(fp);
        if (0 > get_flen()) {
            return 0;
        }

        while (true) {
            sz += sz & 1;
            set_flen(0);

            switch (id) {
                case ID_SIZE:
                    for (i = 0; i < 3; i++) {
                        tmap.size.val[i] = getF4(fp);
                    }
                    tmap.size.eindex = getVX(fp);
                    break;

                case ID_CNTR:
                    for (i = 0; i < 3; i++) {
                        tmap.center.val[i] = getF4(fp);
                    }
                    tmap.center.eindex = getVX(fp);
                    break;

                case ID_ROTA:
                    for (i = 0; i < 3; i++) {
                        tmap.rotate.val[i] = getF4(fp);
                    }
                    tmap.rotate.eindex = getVX(fp);
                    break;

                case ID_FALL:
                    tmap.fall_type = getU2(fp);
                    for (i = 0; i < 3; i++) {
                        tmap.falloff.val[i] = getF4(fp);
                    }
                    tmap.falloff.eindex = getVX(fp);
                    break;

                case ID_OREF:
                    tmap.ref_object = getS0(fp);
                    break;

                case ID_CSYS:
                    tmap.coord_sys = getU2(fp);
                    break;

                default:
                    break;
            }

            /* error while reading the current subchunk? */
            rlen = get_flen();
            if (rlen < 0 || rlen > sz) {
                return 0;
            }

            /* skip unread parts of the current subchunk */
            if (rlen < sz) {
                fp.Seek(sz - rlen, FS_SEEK_CUR);
            }

            /* end of the TMAP subchunk? */
            if (tmapsz <= fp.Tell() - pos) {
                break;
            }

            /* get the next subchunk header */
            set_flen(0);
            id = getU4(fp);
            sz = getU2(fp);
            if (6 != get_flen()) {
                return 0;
            }
        }

        set_flen(fp.Tell() - pos);
        return 1;
    }


    /*
     ======================================================================
     lwGetImageMap()

     Read an lwImageMap from a SURF.BLOK in an LWO2 file.
     ====================================================================== */
    public static int lwGetImageMap(idFile fp, int rsz, lwTexture tex) {
        int id;
        short sz;
        int rlen, pos;

        pos = fp.Tell();
        id = getU4(fp);
        sz = getU2(fp);
        if (0 > get_flen()) {
            return 0;
        }

        while (true) {
            sz += sz & 1;
            set_flen(0);

            switch (id) {
                case ID_TMAP:
                    if (0 == lwGetTMap(fp, sz, tex.tmap)) {
                        return 0;
                    }
                    break;

                case ID_PROJ:
                    tex.param.imap.projection = getU2(fp);
                    break;

                case ID_VMAP:
                    tex.param.imap.vmap_name = getS0(fp);
                    break;

                case ID_AXIS:
                    tex.param.imap.axis = getU2(fp);
                    break;

                case ID_IMAG:
                    tex.param.imap.cindex = getVX(fp);
                    break;

                case ID_WRAP:
                    tex.param.imap.wrapw_type = getU2(fp);
                    tex.param.imap.wraph_type = getU2(fp);
                    break;

                case ID_WRPW:
                    tex.param.imap.wrapw.val = getF4(fp);
                    tex.param.imap.wrapw.eindex = getVX(fp);
                    break;

                case ID_WRPH:
                    tex.param.imap.wraph.val = getF4(fp);
                    tex.param.imap.wraph.eindex = getVX(fp);
                    break;

                case ID_AAST:
                    tex.param.imap.aas_flags = getU2(fp);
                    tex.param.imap.aa_strength = getF4(fp);
                    break;

                case ID_PIXB:
                    tex.param.imap.pblend = getU2(fp);
                    break;

                case ID_STCK:
                    tex.param.imap.stck.val = getF4(fp);
                    tex.param.imap.stck.eindex = getVX(fp);
                    break;

                case ID_TAMP:
                    tex.param.imap.amplitude.val = getF4(fp);
                    tex.param.imap.amplitude.eindex = getVX(fp);
                    break;

                default:
                    break;
            }

            /* error while reading the current subchunk? */
            rlen = get_flen();
            if (rlen < 0 || rlen > sz) {
                return 0;
            }

            /* skip unread parts of the current subchunk */
            if (rlen < sz) {
                fp.Seek(sz - rlen, FS_SEEK_CUR);
            }

            /* end of the image map? */
            if (rsz <= fp.Tell() - pos) {
                break;
            }

            /* get the next subchunk header */
            set_flen(0);
            id = getU4(fp);
            sz = getU2(fp);
            if (6 != get_flen()) {
                return 0;
            }
        }

        set_flen(fp.Tell() - pos);
        return 1;
    }


    /*
     ======================================================================
     lwGetProcedural()

     Read an lwProcedural from a SURF.BLOK in an LWO2 file.
     ====================================================================== */
    public static int lwGetProcedural(idFile fp, int rsz, lwTexture tex) {
        int id;
        short sz;
        int rlen, pos;

        pos = fp.Tell();
        id = getU4(fp);
        sz = getU2(fp);
        if (0 > get_flen()) {
            return 0;
        }

        while (true) {
            sz += sz & 1;
            set_flen(0);

            switch (id) {
                case ID_TMAP:
                    if (0 == lwGetTMap(fp, sz, tex.tmap)) {
                        return 0;
                    }
                    break;

                case ID_AXIS:
                    tex.param.proc.axis = getU2(fp);
                    break;

                case ID_VALU:
                    tex.param.proc.value[0] = getF4(fp);
                    if (sz >= 8) {
                        tex.param.proc.value[1] = getF4(fp);
                    }
                    if (sz >= 12) {
                        tex.param.proc.value[2] = getF4(fp);
                    }
                    break;

                case ID_FUNC:
                    tex.param.proc.name = getS0(fp);
                    rlen = get_flen();
                    tex.param.proc.data = getbytes(fp, sz - rlen);
                    break;

                default:
                    break;
            }

            /* error while reading the current subchunk? */
            rlen = get_flen();
            if (rlen < 0 || rlen > sz) {
                return 0;
            }

            /* skip unread parts of the current subchunk */
            if (rlen < sz) {
                fp.Seek(sz - rlen, FS_SEEK_CUR);
            }

            /* end of the procedural block? */
            if (rsz <= fp.Tell() - pos) {
                break;
            }

            /* get the next subchunk header */
            set_flen(0);
            id = getU4(fp);
            sz = getU2(fp);
            if (6 != get_flen()) {
                return 0;
            }
        }

        set_flen(fp.Tell() - pos);
        return 1;
    }


    /*
     ======================================================================
     lwGetGradient()

     Read an lwGradient from a SURF.BLOK in an LWO2 file.
     ====================================================================== */
    public static int lwGetGradient(idFile fp, int rsz, lwTexture tex) {
        int id;
        short sz;
        int rlen, pos, i, j, nkeys;

        pos = fp.Tell();
        id = getU4(fp);
        sz = getU2(fp);
        if (0 > get_flen()) {
            return 0;
        }

        while (true) {
            sz += sz & 1;
            set_flen(0);

            switch (id) {
                case ID_TMAP:
                    if (0 == lwGetTMap(fp, sz, tex.tmap)) {
                        return 0;
                    }
                    break;

                case ID_PNAM:
                    tex.param.grad.paramname = getS0(fp);
                    break;

                case ID_INAM:
                    tex.param.grad.itemname = getS0(fp);
                    break;

                case ID_GRST:
                    tex.param.grad.start = getF4(fp);
                    break;

                case ID_GREN:
                    tex.param.grad.end = getF4(fp);
                    break;

                case ID_GRPT:
                    tex.param.grad.repeat = getU2(fp);
                    break;

                case ID_FKEY:
                    nkeys = sz; // sizeof(lwGradKey);
                    tex.param.grad.key = new lwGradKey[nkeys];// Mem_ClearedAlloc(nkeys);
                    if (null == tex.param.grad.key) {
                        return 0;
                    }
                    for (i = 0; i < nkeys; i++) {
                        tex.param.grad.key[i].value = getF4(fp);
                        for (j = 0; j < 4; j++) {
                            tex.param.grad.key[i].rgba[j] = getF4(fp);
                        }
                    }
                    break;

                case ID_IKEY:
                    nkeys = sz / 2;
                    tex.param.grad.ikey = new short[nkeys];// Mem_ClearedAlloc(nkeys);
                    if (null == tex.param.grad.ikey) {
                        return 0;
                    }
                    for (i = 0; i < nkeys; i++) {
                        tex.param.grad.ikey[i] = getU2(fp);
                    }
                    break;

                default:
                    break;
            }

            /* error while reading the current subchunk? */
            rlen = get_flen();
            if (rlen < 0 || rlen > sz) {
                return 0;
            }

            /* skip unread parts of the current subchunk */
            if (rlen < sz) {
                fp.Seek(sz - rlen, FS_SEEK_CUR);
            }

            /* end of the gradient? */
            if (rsz <= fp.Tell() - pos) {
                break;
            }

            /* get the next subchunk header */
            set_flen(0);
            id = getU4(fp);
            sz = getU2(fp);
            if (6 != get_flen()) {
                return 0;
            }
        }

        set_flen(fp.Tell() - pos);
        return 1;
    }


    /*
     ======================================================================
     lwGetTexture()

     Read an lwTexture from a SURF.BLOK in an LWO2 file.
     ====================================================================== */
    public static lwTexture lwGetTexture(idFile fp, int bloksz, int type) {
        lwTexture tex;
        short sz;
        int ok;

        tex = new lwTexture();// Mem_ClearedAlloc(sizeof(lwTexture));
        if (null == tex) {
            return null;
        }

        tex.type = type;
        tex.tmap.size.val[0]
                = tex.tmap.size.val[1]
                = tex.tmap.size.val[2] = 1.0f;
        tex.opacity.val = 1.0f;
        tex.enabled = 1;

        sz = getU2(fp);
        if (0 == lwGetTHeader(fp, sz, tex)) {
            tex = null;
            return null;
        }

        sz = (short) (bloksz - sz - 6);
        switch (type) {
            case ID_IMAP:
                ok = lwGetImageMap(fp, sz, tex);
                break;
            case ID_PROC:
                ok = lwGetProcedural(fp, sz, tex);
                break;
            case ID_GRAD:
                ok = lwGetGradient(fp, sz, tex);
                break;
            default:
                ok = btoi(!fp.Seek(sz, FS_SEEK_CUR));
        }

        if (0 == ok) {
            lwFreeTexture.getInstance().run(tex);
            return null;
        }

        set_flen(bloksz);
        return tex;
    }


    /*
     ======================================================================
     lwGetShader()

     Read a shader record from a SURF.BLOK in an LWO2 file.
     ====================================================================== */
    public static lwPlugin lwGetShader(idFile fp, int bloksz) {
        lwPlugin shdr;
        int id;
        short sz;
        int hsz, rlen, pos;

        shdr = new lwPlugin();//Mem_ClearedAlloc(sizeof(lwPlugin));
        if (null == shdr) {
            return null;
        }

        pos = fp.Tell();
        set_flen(0);
        hsz = getU2(fp);
        shdr.ord = getS0(fp);
        id = getU4(fp);
        sz = getU2(fp);

        Fail:
        if (true) {
            if (0 > get_flen()) {
                break Fail;
            }

            while (hsz > 0) {
                sz += sz & 1;
                hsz -= sz;
                if (id == ID_ENAB) {
                    shdr.flags = getU2(fp);
                    break;
                } else {
                    fp.Seek(sz, FS_SEEK_CUR);
                    id = getU4(fp);
                    sz = getU2(fp);
                }
            }

            id = getU4(fp);
            sz = getU2(fp);
            if (0 > get_flen()) {
                break Fail;
            }

            while (true) {
                sz += sz & 1;
                set_flen(0);

                switch (id) {
                    case ID_FUNC:
                        shdr.name = getS0(fp);
                        rlen = get_flen();
                        shdr.data = getbytes(fp, sz - rlen);
                        break;

                    default:
                        break;
                }

                /* error while reading the current subchunk? */
                rlen = get_flen();
                if (rlen < 0 || rlen > sz) {
                    break Fail;
                }

                /* skip unread parts of the current subchunk */
                if (rlen < sz) {
                    fp.Seek(sz - rlen, FS_SEEK_CUR);
                }

                /* end of the shader block? */
                if (bloksz <= fp.Tell() - pos) {
                    break;
                }

                /* get the next subchunk header */
                set_flen(0);
                id = getU4(fp);
                sz = getU2(fp);
                if (6 != get_flen()) {
                    break Fail;
                }
            }

            set_flen(fp.Tell() - pos);
            return shdr;
        }

//        Fail:
        lwFreePlugin.getInstance().run(shdr);
        return null;
    }


    /*
     ======================================================================
     compare_textures()
     compare_shaders()

     Callbacks for the lwListInsert() function, which is called to add
     textures to surface channels and shaders to surfaces.
     ====================================================================== */
    public static class compare_textures implements cmp_t<lwTexture> {

        @Override
        public int compare(final lwTexture a, final lwTexture b) {
            return a.ord.compareTo(b.ord);
        }
    }

    public static class compare_shaders implements cmp_t<lwPlugin> {

        @Override
        public int compare(final lwPlugin a, final lwPlugin b) {
            return a.ord.compareTo(b.ord);
        }
    }

    /*
     ======================================================================
     add_texture()

     Finds the surface channel (lwTParam or lwCParam) to which a texture is
     applied, then calls lwListInsert().
     ====================================================================== */
    public static int add_texture(lwSurface surf, lwTexture tex) {
        lwTexture list;

        switch ((int) tex.chan) {
            case ID_COLR:
                list = surf.color.tex;
                break;
            case ID_LUMI:
                list = surf.luminosity.tex;
                break;
            case ID_DIFF:
                list = surf.diffuse.tex;
                break;
            case ID_SPEC:
                list = surf.specularity.tex;
                break;
            case ID_GLOS:
                list = surf.glossiness.tex;
                break;
            case ID_REFL:
                list = surf.reflection.val.tex;
                break;
            case ID_TRAN:
                list = surf.transparency.val.tex;
                break;
            case ID_RIND:
                list = surf.eta.tex;
                break;
            case ID_TRNL:
                list = surf.translucency.tex;
                break;
            case ID_BUMP:
                list = surf.bump.tex;
                break;
            default:
                return 0;
        }

        lwListInsert(list, tex, new compare_textures());
        return 1;
    }


    /*
     ======================================================================
     lwDefaultSurface()

     Allocate and initialize a surface.
     ====================================================================== */
    public static lwSurface lwDefaultSurface() {
        lwSurface surf;

        surf = new lwSurface();// Mem_ClearedAlloc(sizeof(lwSurface));
        if (null == surf) {
            return null;
        }

        surf.color.rgb[0] = 0.78431f;
        surf.color.rgb[1] = 0.78431f;
        surf.color.rgb[2] = 0.78431f;
        surf.diffuse.val = 1.0f;
        surf.glossiness.val = 0.4f;
        surf.bump.val = 1.0f;
        surf.eta.val = 1.0f;
        surf.sideflags = 1;

        return surf;
    }


    /*
     ======================================================================
     lwGetSurface()

     Read an lwSurface from an LWO2 file.
     ====================================================================== */
    public static lwSurface lwGetSurface(idFile fp, int cksize) {
        lwSurface surf;
        lwTexture tex;
        lwPlugin shdr;
        int id, type;
        short sz;
        int pos, rlen;


        /* allocate the Surface structure */
        surf = new lwSurface();// Mem_ClearedAlloc(sizeof(lwSurface));

        Fail:
        if (true) {
//            if (null == surf) {
//                break Fail;
//            }

            /* non-zero defaults */
            surf.color.rgb[0] = 0.78431f;
            surf.color.rgb[1] = 0.78431f;
            surf.color.rgb[2] = 0.78431f;
            surf.diffuse.val = 1.0f;
            surf.glossiness.val = 0.4f;
            surf.bump.val = 1.0f;
            surf.eta.val = 1.0f;
            surf.sideflags = 1;

            /* remember where we started */
            set_flen(0);
            pos = fp.Tell();

            /* names */
            surf.name = getS0(fp);
            surf.srcname = getS0(fp);

            /* first subchunk header */
            id = getU4(fp);
            sz = getU2(fp);
            if (0 > get_flen()) {
                break Fail;
            }

            /* process subchunks as they're encountered */
            while (true) {
                sz += sz & 1;
                set_flen(0);

                switch (id) {
                    case ID_COLR:
                        surf.color.rgb[0] = getF4(fp);
                        surf.color.rgb[1] = getF4(fp);
                        surf.color.rgb[2] = getF4(fp);
                        surf.color.eindex = getVX(fp);
                        break;

                    case ID_LUMI:
                        surf.luminosity.val = getF4(fp);
                        surf.luminosity.eindex = getVX(fp);
                        break;

                    case ID_DIFF:
                        surf.diffuse.val = getF4(fp);
                        surf.diffuse.eindex = getVX(fp);
                        break;

                    case ID_SPEC:
                        surf.specularity.val = getF4(fp);
                        surf.specularity.eindex = getVX(fp);
                        break;

                    case ID_GLOS:
                        surf.glossiness.val = getF4(fp);
                        surf.glossiness.eindex = getVX(fp);
                        break;

                    case ID_REFL:
                        surf.reflection.val.val = getF4(fp);
                        surf.reflection.val.eindex = getVX(fp);
                        break;

                    case ID_RFOP:
                        surf.reflection.options = getU2(fp);
                        break;

                    case ID_RIMG:
                        surf.reflection.cindex = getVX(fp);
                        break;

                    case ID_RSAN:
                        surf.reflection.seam_angle = getF4(fp);
                        break;

                    case ID_TRAN:
                        surf.transparency.val.val = getF4(fp);
                        surf.transparency.val.eindex = getVX(fp);
                        break;

                    case ID_TROP:
                        surf.transparency.options = getU2(fp);
                        break;

                    case ID_TIMG:
                        surf.transparency.cindex = getVX(fp);
                        break;

                    case ID_RIND:
                        surf.eta.val = getF4(fp);
                        surf.eta.eindex = getVX(fp);
                        break;

                    case ID_TRNL:
                        surf.translucency.val = getF4(fp);
                        surf.translucency.eindex = getVX(fp);
                        break;

                    case ID_BUMP:
                        surf.bump.val = getF4(fp);
                        surf.bump.eindex = getVX(fp);
                        break;

                    case ID_SMAN:
                        surf.smooth = getF4(fp);
                        break;

                    case ID_SIDE:
                        surf.sideflags = getU2(fp);
                        break;

                    case ID_CLRH:
                        surf.color_hilite.val = getF4(fp);
                        surf.color_hilite.eindex = getVX(fp);
                        break;

                    case ID_CLRF:
                        surf.color_filter.val = getF4(fp);
                        surf.color_filter.eindex = getVX(fp);
                        break;

                    case ID_ADTR:
                        surf.add_trans.val = getF4(fp);
                        surf.add_trans.eindex = getVX(fp);
                        break;

                    case ID_SHRP:
                        surf.dif_sharp.val = getF4(fp);
                        surf.dif_sharp.eindex = getVX(fp);
                        break;

                    case ID_GVAL:
                        surf.glow.val = getF4(fp);
                        surf.glow.eindex = getVX(fp);
                        break;

                    case ID_LINE:
                        surf.line.enabled = 1;
                        if (sz >= 2) {
                            surf.line.flags = getU2(fp);
                        }
                        if (sz >= 6) {
                            surf.line.size.val = getF4(fp);
                        }
                        if (sz >= 8) {
                            surf.line.size.eindex = getVX(fp);
                        }
                        break;

                    case ID_ALPH:
                        surf.alpha_mode = getU2(fp);
                        surf.alpha = getF4(fp);
                        break;

                    case ID_AVAL:
                        surf.alpha = getF4(fp);
                        break;

                    case ID_BLOK:
                        type = getU4(fp);

                        switch (type) {
                            case ID_IMAP:
                            case ID_PROC:
                            case ID_GRAD:
                                tex = lwGetTexture(fp, sz - 4, type);
                                if (null == tex) {
                                    break Fail;
                                }
                                if (0 == add_texture(surf, tex)) {
                                    lwFreeTexture.getInstance().run(tex);
                                }
                                set_flen(4 + get_flen());
                                break;
                            case ID_SHDR:
                                shdr = lwGetShader(fp, sz - 4);
                                if (null == shdr) {
                                    break Fail;
                                }
                                lwListInsert(surf.shader, shdr, new compare_shaders());
                                ++surf.nshaders;
                                set_flen(4 + get_flen());
                                break;
                        }
                        break;

                    default:
                        break;
                }

                /* error while reading current subchunk? */
                rlen = get_flen();
                if (rlen < 0 || rlen > sz) {
                    break Fail;
                }

                /* skip unread parts of the current subchunk */
                if (rlen < sz) {
                    fp.Seek(sz - rlen, FS_SEEK_CUR);
                }

                /* end of the SURF chunk? */
                if (cksize <= fp.Tell() - pos) {
                    break;
                }

                /* get the next subchunk header */
                set_flen(0);
                id = getU4(fp);
                sz = getU2(fp);
                if (6 != get_flen()) {
                    break Fail;
                }
            }

            return surf;
        }

//        Fail:
        if (surf != null) {
            lwFreeSurface.getInstance().run(surf);
        }
        return null;
    }

    public static float dot(float a[], float b[]) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static void cross(float a[], float b[], float c[]) {
        c[0] = a[1] * b[2] - a[2] * b[1];
        c[1] = a[2] * b[0] - a[0] * b[2];
        c[2] = a[0] * b[1] - a[1] * b[0];
    }

    public static void normalize(float v[]) {
        float r;

        r = (float) idMath.Sqrt(dot(v, v));
        if (r > 0) {
            v[0] /= r;
            v[1] /= r;
            v[2] /= r;
        }
    }

    /*
     ======================================================================
     lwFreeVMap()

     Free memory used by an lwVMap.
     ====================================================================== */
    public static class lwFreeVMap extends LW {

        private static final LW instance = new lwFreeVMap();

        private lwFreeVMap() {
        }

        public static LW getInstance() {
            return instance;
        }

        @Override
        public void run(Object p) {
            lwVMap vmap = (lwVMap) p;
            if (vmap != null) {
                if (vmap.name != null) {
                    vmap.name = null;
                }
                if (vmap.vindex != null) {
                    vmap.vindex = null;
                }
                if (vmap.pindex != null) {
                    vmap.pindex = null;
                }
                if (vmap.val != null) {
//                    if (vmap.val[0] != 0f) {
//                        Mem_Free(vmap.val[0]);
//                    }
                    vmap.val = null;
                }
                vmap.clear();
            }
        }
    };


    /*
     ======================================================================
     lwGetVMap()

     Read an lwVMap from a VMAP or VMAD chunk in an LWO2.
     ====================================================================== */
    public static lwVMap lwGetVMap(idFile fp, int cksize, int ptoffset, int poloffset, int perpoly) {
        ByteBuffer buf;
//        String b[];
        lwVMap vmap;
        float[] f;
        int i, j, npts, rlen;


        /* read the whole chunk */
        set_flen(0);
        buf = ByteBuffer.wrap(getbytes(fp, cksize));
        if (null == buf) {
            return null;
        }

        vmap = new lwVMap();// Mem_ClearedAlloc(sizeof(lwVMap));
//        if (null == vmap) {
//            buf = null
//            return null;
//        }

        /* initialize the vmap */
        vmap.perpoly = perpoly;

//        buf = buf;
        set_flen(0);
        vmap.type = sgetU4(buf);
        vmap.dim = sgetU2(buf);
        vmap.name = sgetS0(buf);
        rlen = get_flen();

        /* count the vmap records */
        npts = 0;
        while (buf.hasRemaining()) {//( bp < buf + cksize ) {
            i = sgetVX(buf);
            if (perpoly != 0) {
                i = sgetVX(buf);
            }
            buf.position(buf.position() + vmap.dim * (Float.SIZE / Byte.SIZE));
            ++npts;
        }

        /* allocate the vmap */
        vmap.nverts = npts;
        vmap.vindex = new int[npts];// Mem_ClearedAlloc(npts);

        Fail:
        if (true) {
//            if (null == vmap.vindex) {
//                break Fail;
//            }
            if (perpoly != 0) {
                vmap.pindex = new int[npts];// Mem_ClearedAlloc(npts);
//                if (null == vmap.pindex) {
//                    break Fail;
//                }
            }

            if (vmap.dim > 0) {
                vmap.val = new float[npts][];// Mem_ClearedAlloc(npts);
//                if (null == vmap.val) {
//                    break Fail;
//                }
//                f = new float[npts];// Mem_ClearedAlloc(npts);
//                if (null == f) {
//                    break Fail;
//                }
                for (i = 0; i < npts; i++) {
//                    vmap.val[i] = f[i] * vmap.dim;
                    vmap.val[i] = new float[vmap.dim];
                }
            }

            /* fill in the vmap values */
            buf.position(rlen);
            for (i = 0; i < npts; i++) {
                vmap.vindex[i] = sgetVX(buf);
                if (perpoly != 0) {
                    vmap.pindex[i] = sgetVX(buf);
                }
                for (j = 0; j < vmap.dim; j++) {
                    vmap.val[i][j] = sgetF4(buf);
                }
            }

            buf = null;
            return vmap;
        }

//        Fail:
        if (buf != null) {
            buf = null;
        }
        lwFreeVMap.getInstance().run(vmap);
        return null;
    }


    /*
     ======================================================================
     lwGetPointVMaps()

     Fill in the lwVMapPt structure for each point.
     ====================================================================== */
    public static boolean lwGetPointVMaps(lwPointList point, lwVMap vmap) {
        lwVMap vm;
        int i, j, n;

        /* count the number of vmap values for each point */
        vm = vmap;
        while (vm != null) {
            if (0 == vm.perpoly) {
                for (i = 0; i < vm.nverts; i++) {
                    ++point.pt[vm.vindex[i]].nvmaps;
                }
            }
            vm = vm.next;
        }

        /* allocate vmap references for each mapped point */
        for (i = 0; i < point.count; i++) {
            if (point.pt[i].nvmaps != 0) {
                point.pt[i].vm = Stream.generate(() -> new lwVMapPt()).
                        limit(point.pt[i].nvmaps).
                        toArray(lwVMapPt[]::new);// Mem_ClearedAlloc(point.pt[ i].nvmaps);
                if (null == point.pt[i].vm) {
                    return false;
                }
                point.pt[i].nvmaps = 0;
            }
        }

        /* fill in vmap references for each mapped point */
        vm = vmap;
        while (vm != null) {
            if (0 == vm.perpoly) {
                for (i = 0; i < vm.nverts; i++) {
                    j = vm.vindex[i];
                    n = point.pt[j].nvmaps;
                    point.pt[j].vm[n].vmap = vm;
                    point.pt[j].vm[n].index = i;
                    ++point.pt[j].nvmaps;
                }
            }
            vm = vm.next;
        }

        return true;
    }


    /*
     ======================================================================
     lwGetPolyVMaps()

     Fill in the lwVMapPt structure for each polygon vertex.
     ====================================================================== */
    public static boolean lwGetPolyVMaps(lwPolygonList polygon, lwVMap vmap) {
        lwVMap vm;
        lwPolVert pv;
        int i, j;

        /* count the number of vmap values for each polygon vertex */
        vm = vmap;
        while (vm != null) {
            if (vm.perpoly != 0) {
                for (i = 0; i < vm.nverts; i++) {
                    for (j = 0; j < polygon.pol[vm.pindex[i]].nverts; j++) {
                        pv = polygon.pol[vm.pindex[i]].getV(j);
                        if (vm.vindex[i] == pv.index) {
                            ++pv.nvmaps;
                            break;
                        }
                    }
                }
            }
            vm = vm.next;
        }

        /* allocate vmap references for each mapped vertex */
        for (i = 0; i < polygon.count; i++) {
            for (j = 0; j < polygon.pol[i].nverts; j++) {
                pv = polygon.pol[i].getV(j);
                if (pv.nvmaps != 0) {
                    pv.vm = lwVMapPt.generateArray(pv.nvmaps);
                    if (null == pv.vm) {
                        return false;
                    }
                    pv.nvmaps = 0;
                }
            }
        }

        /* fill in vmap references for each mapped point */
        vm = vmap;
        while (vm != null) {
            if (vm.perpoly != 0) {
                for (i = 0; i < vm.nverts; i++) {
                    for (j = 0; j < polygon.pol[vm.pindex[i]].nverts; j++) {
                        pv = polygon.pol[vm.pindex[i]].getV(j);
                        if (vm.vindex[i] == pv.index) {
                            pv.vm[pv.nvmaps].vmap = vm;
                            pv.vm[pv.nvmaps].index = i;
                            ++pv.nvmaps;
                            break;
                        }
                    }
                }
            }
            vm = vm.next;
        }

        return true;
    }
}
