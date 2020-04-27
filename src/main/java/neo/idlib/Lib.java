package neo.idlib;

import static neo.TempDump.ctos;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import neo.Game.Entity.idEntity.entityFlags_s;
import neo.Game.Projectile.idProjectile.projectileFlags_s;
import neo.framework.CVarSystem.idCVarSystem;
import neo.framework.Common.idCommon;
import neo.framework.FileSystem_h.idFileSystem;
import neo.idlib.Dict_h.idDict;
import neo.idlib.BV.Bounds.idBounds;
import neo.idlib.Text.Str.idStr;
import neo.idlib.math.Angles.idAngles;
import neo.idlib.math.Math_h;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Polynomial.idPolynomial;
import neo.idlib.math.Simd.idSIMD;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Vector.idVec4;
import neo.idlib.math.Vector.idVec5;
import neo.idlib.math.Matrix.idMatX;
import neo.sys.sys_public.idSys;

/**
 *
 */
public class Lib {

    /*
     ===============================================================================

     idLib contains stateless support classes and concrete types. Some classes
     do have static variables, but such variables are initialized once and
     read-only after initialization (they do not maintain a modifiable state).

     The interface pointers idSys, idCommon, idCVarSystem and idFileSystem
     should be set before using idLib. The pointers stored here should not
     be used by any part of the engine except for idLib.

     The frameNumber should be continuously set to the number of the current
     frame if frame base memory logging is required.

     ===============================================================================
     */
    public static class idLib {

        public static idSys        sys         = null;
        public static idCommon     common      = null;
        public static idCVarSystem cvarSystem  = null;
        public static idFileSystem fileSystem  = null;
        public static int          frameNumber = 0;

        public static void Init() {

//	assert( sizeof( bool ) == 1 );
            // initialize little/big endian conversion
            Lib.Swap_Init();
//
//            // initialize memory manager
//            Heap.Mem_Init();
//
            // init string memory allocator
            idStr.InitMemory();

            // initialize generic SIMD implementation
            idSIMD.Init();

            // initialize math
            idMath.Init();

            // test idMatX
            idMatX.Test();

            // test idPolynomial
            idPolynomial.Test();

            // initialize the dictionary string pools
            idDict.Init();
        }

        public static void ShutDown() {

            // shut down the dictionary string pools
            idDict.Shutdown();

            // shut down the string memory allocator
            idStr.ShutdownMemory();

            // shut down the SIMD engine
            idSIMD.Shutdown();

//            // shut down the memory manager
//            Heap.Mem_Shutdown();
        }

        // wrapper to idCommon functions 
        public static void Error(final String... fmt) {
//	va_list		argptr;
//	char		text[MAX_STRING_CHARS];
//
//	va_start( argptr, fmt );
//	idStr::vsnPrintf( text, sizeof( text ), fmt, argptr );
//	va_end( argptr );
//
//	common->Error( "%s", text );
        }

        public static void Warning(final String... fmt) {
        }
    };
//
//
//
///*
//===============================================================================
//
//	Types and defines used throughout the engine.
//
//===============================================================================
//*/
//
////typedef unsigned char			byte;		// 8 bits
////typedef unsigned short			word;		// 16 bits
////typedef unsigned int			dword;		// 32 bits
////typedef unsigned int			uint;
////typedef unsigned long			ulong;
//
////typedef int						qhandle_t;

    public static int BIT(int num) {//TODO:is int voldoende?
        return (1 << num);
    }

    //
    public static final int    MAX_STRING_CHARS = 1024;        // max length of a string
    //
// maximum world size
    public static final int    MAX_WORLD_COORD  = (128 * 1024);
    public static final int    MIN_WORLD_COORD  = (-128 * 1024);
    public static final int    MAX_WORLD_SIZE   = (MAX_WORLD_COORD - MIN_WORLD_COORD);
    //
    // basic colors
   /*
     ===============================================================================

     Colors

     ===============================================================================
     */
    public static final idVec4 colorBlack       = new idVec4(0.00f, 0.00f, 0.00f, 1.00f);
    public static final idVec4 colorWhite       = new idVec4(1.00f, 1.00f, 1.00f, 1.00f);
    public static final idVec4 colorRed         = new idVec4(1.00f, 0.00f, 0.00f, 1.00f);
    public static final idVec4 colorGreen       = new idVec4(0.00f, 1.00f, 0.00f, 1.00f);
    public static final idVec4 colorBlue        = new idVec4(0.00f, 0.00f, 1.00f, 1.00f);
    public static final idVec4 colorYellow      = new idVec4(1.00f, 1.00f, 0.00f, 1.00f);
    public static final idVec4 colorMagenta     = new idVec4(1.00f, 0.00f, 1.00f, 1.00f);
    public static final idVec4 colorCyan        = new idVec4(0.00f, 1.00f, 1.00f, 1.00f);
    public static final idVec4 colorOrange      = new idVec4(1.00f, 0.50f, 0.00f, 1.00f);
    public static final idVec4 colorPurple      = new idVec4(0.60f, 0.00f, 0.60f, 1.00f);
    public static final idVec4 colorPink        = new idVec4(0.73f, 0.40f, 0.48f, 1.00f);
    public static final idVec4 colorBrown       = new idVec4(0.40f, 0.35f, 0.08f, 1.00f);
    public static final idVec4 colorLtGrey      = new idVec4(0.75f, 0.75f, 0.75f, 1.00f);
    public static final idVec4 colorMdGrey      = new idVec4(0.50f, 0.50f, 0.50f, 1.00f);
    public static final idVec4 colorDkGrey      = new idVec4(0.25f, 0.25f, 0.25f, 1.00f);
    //
    static              int[]  colorMask        = {255, 0};

    /*
     ================
     ColorFloatToByte
     ================
     */
    static byte ColorFloatToByte(float c) {
        return (byte) (((long) (c * 255.0f)) & colorMask[Math_h.FLOATSIGNBITSET(c)]);
    }

    // packs color floats in the range [0,1] into an integer
    /*
     ================
     PackColor
     ================
     */
    public static long PackColor(final idVec4 color) {
        long dw, dx, dy, dz;

        dx = ColorFloatToByte(color.x);
        dy = ColorFloatToByte(color.y);
        dz = ColorFloatToByte(color.z);
        dw = ColorFloatToByte(color.w);

        return (dx << 0) | (dy << 8) | (dz << 16) | (dw << 24);
    }

    /*
     ================
     UnpackColor
     ================
     */
    public static void UnpackColor(final long color, idVec4 unpackedColor) {
        unpackedColor.Set(
                ((color >> 0) & 255) * (1.0f / 255.0f),
                ((color >> 8) & 255) * (1.0f / 255.0f),
                ((color >> 16) & 255) * (1.0f / 255.0f),
                ((color >> 24) & 255) * (1.0f / 255.0f));
    }

    /*
     ================
     PackColor
     ================
     */
    public static long PackColor(final idVec3 color) {
        long dx, dy, dz;

        dx = ColorFloatToByte(color.x);
        dy = ColorFloatToByte(color.y);
        dz = ColorFloatToByte(color.z);

        return (dx << 0) | (dy << 8) | (dz << 16);
    }

    /*
     ================
     UnpackColor
     ================
     */
    public static void UnpackColor(final long color, idVec3 unpackedColor) {
        unpackedColor.Set(
                ((color >> 0) & 255) * (1.0f / 255.0f),
                ((color >> 8) & 255) * (1.0f / 255.0f),
                ((color >> 16) & 255) * (1.0f / 255.0f));
    }

    /*
     ===============================================================================

     Byte order functions

     ===============================================================================
     */
    short BigShort(short l) {
        if (SWAP_TEST) {
            return ShortSwap(l);
        } else {
            return ShortNoSwap(l);
        }
    }

    public static short LittleShort(short l) {
        if (SWAP_TEST) {
            return ShortSwap(l);
        } else {
            return ShortNoSwap(l);
        }
    }

    int BigLong(int l) {
        if (SWAP_TEST) {
            return LongSwap(l);
        } else {
            return LongNoSwap(l);
        }
    }

    public static int LittleLong(int l) {
        if (SWAP_TEST) {
            return LongSwap(l);
        } else {
            return LongNoSwap(l);
        }
    }

    public static int LittleLong(long l) {
        return LittleLong((int) l);//TODO:little or long?
    }

    public static int LittleLong(byte[] b) {
        int l = new BigInteger(b).intValue();

        return LittleLong(l);
    }

    public static float BigFloat(float l) {
        if (SWAP_TEST) {
            return FloatSwap(l);
        } else {
            return FloatNoSwap(l);
        }
    }

    public static float LittleFloat(float l) {
        if (SWAP_TEST) {
            return FloatSwap(l);
        } else {
            return FloatNoSwap(l);
        }
    }

    public static void BigRevBytes(ByteBuffer buffer, int elcount) {
        if (SWAP_TEST) {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    public static void LittleRevBytes(float[] bp, int elcount) {
        if (SWAP_TEST) {
            int[] pb = new int[bp.length];
            for (int a = 0; a < bp.length; a++) {
                pb[a] = Float.floatToIntBits(bp[a]);
            }
            RevBytesSwap(pb,/*elsize,*/ elcount);
            for (int b = 0; b < pb.length; b++) {
                bp[b] = Float.intBitsToFloat(pb[b]);
            }
        }
    }

    public static void LittleRevBytes(byte[] bp, int offset, int elcount) {
        if (SWAP_TEST) {
            RevBytesSwap(bp, 0,/*elsize,*/ elcount);
        } else {
            RevBytesNoSwap(bp, /*elsize,*/ elcount);
        }
    }

    public static void LittleRevBytes(byte[] bp/*, int elsize*/, int elcount) {
        LittleRevBytes(bp, 0, elcount);
    }

    public static void LittleRevBytes(idVec5 v) {
        if (SWAP_TEST) {
            final float x = v.x;
            final float y = v.y;

            v.x = v.t;
            v.y = v.s;
            v.s = y;
            v.t = x;
        }
    }

    public static void LittleRevBytes(idBounds bounds) {
        if (SWAP_TEST) {
            final idVec3 a = bounds.oGet(0);
            final idVec3 b = bounds.oGet(1);
            bounds.oSet(0, b);
            bounds.oSet(1, a);
        }
    }

    public static void LittleRevBytes(idAngles angles) {
        if (SWAP_TEST) {
            final float pitch = angles.pitch;
            angles.pitch = angles.roll;
            angles.roll = pitch;
        }
    }

    public static void LittleBitField(byte[] bp, int elsize) {
        if (SWAP_TEST) {
            RevBitFieldSwap(bp, elsize);
        } else {
            RevBitFieldNoSwap(bp, elsize);
        }
    }

    public static void LittleBitField(entityFlags_s flags) {
        if (SWAP_TEST) {//TODO:expand this in the morning.
            flags.notarget = flags.networkSync | (false & (flags.networkSync = flags.notarget));
            flags.noknockback = flags.hasAwakened | (false & (flags.hasAwakened = flags.noknockback));
            flags.takedamage = flags.isDormant | (false & (flags.isDormant = flags.takedamage));
            flags.hidden = flags.neverDormant | (false & (flags.neverDormant = flags.hidden));
            flags.bindOrientated = flags.selected | (false & (flags.selected = flags.bindOrientated));
            flags.solidForTeam = flags.forcePhysicsUpdate | (false & (flags.forcePhysicsUpdate = flags.solidForTeam));
        }
    }

    public static void LittleBitField(projectileFlags_s flags) {
        if (SWAP_TEST) {
            flags.detonate_on_world = flags.detonate_on_actor | (false & (flags.detonate_on_actor = flags.detonate_on_world));
            flags.isTracer = flags.noSplashDamage | (false & (flags.noSplashDamage = flags.isTracer));
        }
    }

    public static void SixtetsForInt(byte[] out, int src) //TODO:primitive byte cannot be passed by reference????
    {
        if (SWAP_TEST) {
            SixtetsForIntLittle(out, src);
        } else {
            SixtetsForIntBig(out, src);
        }
    }

    public static int IntForSixtets(byte[] in) {
        if (SWAP_TEST) {
            return IntForSixtetsLittle(in);
        } else {
            return IntForSixtetsBig(in);
        }
    }

    /*
     ================
     ShortSwap
     ================
     */
    static short ShortSwap(short l) {
        byte b1, b2;

        b1 = (byte) (l & 255);
        b2 = (byte) ((l >> 8) & 255);

        return (short) ((b1 << 8) + b2);
    }

    /*
     ================
     ShortNoSwap
     ================
     */
    static short ShortNoSwap(short l) {
        return l;
    }

    /*
     ================
     LongSwap
     ================
     */
    static int LongSwap(int l) {
        byte b1, b2, b3, b4;

        b1 = (byte) ((l >> 0) & 255);
        b2 = (byte) ((l >> 8) & 255);
        b3 = (byte) ((l >> 16) & 255);
        b4 = (byte) ((l >> 24) & 255);

        return ((int) b1 << 24) + ((int) b2 << 16) + ((int) b3 << 8) + b4;
    }

    /*
     ================
     LongNoSwap
     ================
     */
    static int LongNoSwap(int l) {
        return l;
    }

    /*
     ================
     FloatSwap
     ================
     */
    static float FloatSwap(float f) {
//	union {
//		float	f;
//		byte	b[4];
//	} dat1, dat2;
//	
//	
//	dat1.f = f;
//	dat2.b[0] = dat1.b[3];
//	dat2.b[1] = dat1.b[2];
//	dat2.b[2] = dat1.b[1];
//	dat2.b[3] = dat1.b[0];
//        
//	return dat2.f;

        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putFloat(f).order(ByteOrder.BIG_ENDIAN).getFloat(0);
    }

    /*
     ================
     FloatNoSwap
     ================
     */
    static float FloatNoSwap(float f) {
        return f;
    }

    /*
     =====================================================================
     RevBytesSwap

     Reverses byte order in place.

     INPUTS
     bp       bytes to reverse
     elsize   size of the underlying data type
     elcount  number of elements to swap

     RESULTS
     Reverses the byte order in each of elcount elements.
     ===================================================================== */
    static void RevBytesSwap(byte[] bp, int offset/*, int elsize*/, int elcount) {
        int p, q;
        int elsize;

        p = offset;
        elsize = bp.length;

        if (elsize == 2) {
            q = p + 1;
            while (elcount-- != 0) {
                bp[p] ^= bp[q];
                bp[q] ^= bp[p];
                bp[p] ^= bp[q];
                p += 2;
                q += 2;
            }
            return;
        }

        while (elcount-- != 0) {
            q = p + elsize - 1;
            while (p < q) {
                bp[p] ^= bp[q];
                bp[q] ^= bp[p];
                bp[p] ^= bp[q];
                ++p;
                --q;
            }
            p += elsize >> 1;
        }
    }

    static void RevBytesSwap(int[] bp/*, int elsize*/, int elcount) {
        int p, q;
        int elsize;//TODO:elsize is the number of bytes?

        p = 0;
        elsize = bp.length;

        if (elsize == 2) {
            q = p + 1;
            while (elcount-- != 0) {
                bp[p] ^= bp[q];
                bp[q] ^= bp[p];
                bp[p] ^= bp[q];
                p += 2;
                q += 2;
            }
            return;
        }

        while (elcount-- != 0) {
            q = p + elsize - 1;
            while (p < q) {
                bp[p] ^= bp[q];
                bp[q] ^= bp[p];
                bp[p] ^= bp[q];
                ++p;
                --q;
            }
            p += elsize >> 1;
        }
    }

    /*
     =====================================================================
     RevBytesSwap
 
     Reverses byte order in place, then reverses bits in those bytes
 
     INPUTS
     bp       bitfield structure to reverse
     elsize   size of the underlying data type
 
     RESULTS
     Reverses the bitfield of size elsize.
     ===================================================================== */
    static void RevBitFieldSwap(byte[] bp, int elsize) {
        int i;
        int p, t, v;

        LittleRevBytes(bp, /*elsize,*/ 1);

        p = 0;
        while (elsize-- != 0) {
            v = bp[p];
            t = 0;
            for (i = 7; i != 0; i--) {
                t <<= 1;
                v >>= 1;
                t |= v & 1;
            }
            bp[p++] = (byte) t;
        }
    }

    /*
     ================
     RevBytesNoSwap
     ================
     */
    static void RevBytesNoSwap(byte[] bp,/*int elsize,*/ int elcount) {
        return;
    }

    /*
     ================
     RevBytesNoSwap
     ================
     */
    static void RevBitFieldNoSwap(byte[] bp, int elsize) {
        return;
    }

    /*
     ================
     SixtetsForIntLittle
     ================
     */
    static void SixtetsForIntLittle(byte[] out, int src) {
        int[] b = {
            (src >> 0) & 0xff,//TODO:check order
            (src >> 8) & 0xff,
            (src >> 16) & 0xff,
            (src >> 24) & 0xff
        };
        out[0] = (byte) ((b[0] & 0xfc) >> 2);
        out[1] = (byte) (((b[0] & 0x3) << 4) + ((b[1] & 0xf0) >> 4));
        out[2] = (byte) (((b[1] & 0xf) << 2) + ((b[2] & 0xc0) >> 6));
        out[3] = (byte) (b[2] & 0x3f);
    }

    /*
     ================
     SixtetsForIntBig
     TTimo: untested - that's the version from initial base64 encode
     ================
     */
    static void SixtetsForIntBig(byte[] out, int src) {
        for (int i = 0; i < 4; i++) {
            out[0] += (src & 0x3f);
            src >>= 6;
        }
    }

    /*
     ================
     IntForSixtetsLittle
     ================
     */
    static int IntForSixtetsLittle(byte[] in) {
        int[] b = new int[4];
        b[0] |= in[0] << 2;
        b[0] |= (in[1] & 0x30) >> 4;
        b[1] |= (in[1] & 0xf) << 4;
        b[1] |= (in[2] & 0x3c) >> 2;
        b[2] |= (in[2] & 0x3) << 6;
        b[2] |= in[3];
        return (b[0] << 24)
                + (b[1] << 16)
                + (b[2] << 8)
                + (b[3] << 0);
    }

    /*
     ================
     IntForSixtetsBig
     TTimo: untested - that's the version from initial base64 decode
     ================
     */
    static int IntForSixtetsBig(byte[] in) {
        int ret = 0;
        ret |= in[0];
        ret |= in[1] << 6;
        ret |= in[2] << 2 * 6;
        ret |= in[3] << 3 * 6;
        return ret;
    }

    public static class idException extends RuntimeException {//TODO:to exception or to runtimeException!!

        public String error;//[MAX_STRING_CHARS];

        public idException() {
            super();
        }

        public idException(final String text) {
//            strcpy(error, text);
            super(text);
            this.error = text;
        }

        public idException(final char[] text) {
//            strcpy(error, text);
            super(ctos(text));
            this.error = ctos(text);
        }

        public idException(Throwable cause) {
            super(cause);
        }       
        
    };

    // move from Math.h to keep gcc happy
    public static double Max(double x, double y) {
        return (x > y) ? x : y;
    }

    public static double Min(double x, double y) {
        return (x < y) ? x : y;
    }

    public static float Max(float x, float y) {
        return (x > y) ? x : y;
    }

    public static float Min(float x, float y) {
        return (x < y) ? x : y;
    }

    public static int Max(int x, int y) {
        return (x > y) ? x : y;
    }

    public static int Min(int x, int y) {
        return (x < y) ? x : y;
    }
    /*
     ================
     Swap_Init
     ================
     */
    private static final boolean SWAP_TEST = Swap_IsBigEndian();

    static void Swap_Init() {
////	byte	swaptest[2] = {1,0};
//
//	// set the byte swapping variables in a portable manner	
//	if ( !Swap_IsBigEndian() ) {
////	if ( *(short *)swaptest == 1) {
//		// little endian ex: x86
//		_BigShort = ShortSwap;
//		_LittleShort = ShortNoSwap;
//		_BigLong = LongSwap;
//		_LittleLong = LongNoSwap;
//		_BigFloat = FloatSwap;
//		_LittleFloat = FloatNoSwap;
//		_BigRevBytes = RevBytesSwap;
//		_LittleRevBytes = RevBytesNoSwap;
//		_LittleBitField = RevBitFieldNoSwap;
//		_SixtetsForInt = SixtetsForIntLittle;
//		_IntForSixtets = IntForSixtetsLittle;
//	} else {
//		// big endian ex: ppc
//		_BigShort = ShortNoSwap;
//		_LittleShort = ShortSwap;
//		_BigLong = LongNoSwap;
//		_LittleLong = LongSwap;
//		_BigFloat = FloatNoSwap;
//		_LittleFloat = FloatSwap;
//		_BigRevBytes = RevBytesNoSwap;
//		_LittleRevBytes = RevBytesSwap;
//		_LittleBitField = RevBitFieldSwap;
//		_SixtetsForInt = SixtetsForIntBig;
//		_IntForSixtets = IntForSixtetsBig;
//	}
    }

    /*
     ==========
     Swap_IsBigEndian
     ==========
     */
    static boolean Swap_IsBigEndian() {
//	byte	swaptest[2] = {1,0};
//	return *(short *)swaptest != 1;
        return ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder());
    }

    /*
     ===============================================================================

     Assertion

     ===============================================================================
     */
    void AssertFailed(final String file, int line, final String expression) {
        idLib.sys.DebugPrintf("\n\nASSERTION FAILED!\n%s(%d): '%s'\n", file, line, expression);
//#ifdef _WIN32
//	__asm int 0x03
//#elif defined( __linux__ )
//	__asm__ __volatile__ ("int $0x03");
//#elif defined( MACOS_X )
//	kill( getpid(), SIGINT );
//#endif
    }
}
