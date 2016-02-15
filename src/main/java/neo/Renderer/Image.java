package neo.Renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static neo.Renderer.Image.cubeFiles_t.CF_2D;
import neo.Renderer.Image.ddsFileHeader_t;
import neo.Renderer.Image.idImage;
import static neo.Renderer.Image.idImage.DEFAULT_SIZE;
import static neo.Renderer.Image.textureDepth_t.TD_BUMP;
import static neo.Renderer.Image.textureDepth_t.TD_DEFAULT;
import static neo.Renderer.Image.textureDepth_t.TD_DIFFUSE;
import static neo.Renderer.Image.textureDepth_t.TD_HIGH_QUALITY;
import static neo.Renderer.Image.textureDepth_t.TD_SPECULAR;
import static neo.Renderer.Image.textureType_t.TT_2D;
import static neo.Renderer.Image.textureType_t.TT_3D;
import static neo.Renderer.Image.textureType_t.TT_CUBIC;
import static neo.Renderer.Image.textureType_t.TT_DISABLED;
import static neo.Renderer.Image.textureType_t.TT_RECT;
import static neo.Renderer.Image_files.R_LoadCubeImages;
import static neo.Renderer.Image_files.R_WritePalTGA;
import static neo.Renderer.Image_files.R_WriteTGA;
import neo.Renderer.Image_init.R_AlphaNotchImage;
import neo.Renderer.Image_init.R_AmbientNormalImage;
import neo.Renderer.Image_init.R_BlackImage;
import neo.Renderer.Image_init.R_BorderClampImage;
import neo.Renderer.Image_init.R_CombineCubeImages_f;
import neo.Renderer.Image_init.R_CreateNoFalloffImage;
import neo.Renderer.Image_init.R_DefaultImage;
import neo.Renderer.Image_init.R_FlatNormalImage;
import neo.Renderer.Image_init.R_FogEnterImage;
import neo.Renderer.Image_init.R_FogImage;
import neo.Renderer.Image_init.R_ListImages_f;
import neo.Renderer.Image_init.R_QuadraticImage;
import neo.Renderer.Image_init.R_RGBA8Image;
import neo.Renderer.Image_init.R_RampImage;
import neo.Renderer.Image_init.R_ReloadImages_f;
import neo.Renderer.Image_init.R_Specular2DTableImage;
import neo.Renderer.Image_init.R_SpecularTableImage;
import neo.Renderer.Image_init.R_WhiteImage;
import static neo.Renderer.Image_init.imageFilter;
import neo.Renderer.Image_init.makeNormalizeVectorCubeMap;
import static neo.Renderer.Image_load.FormatIsDXT;
import static neo.Renderer.Image_load.MakePowerOfTwo;
import static neo.Renderer.Image_process.R_BlendOverTexture;
import static neo.Renderer.Image_process.R_MipMap;
import static neo.Renderer.Image_process.R_MipMap3D;
import static neo.Renderer.Image_process.R_SetBorderTexels;
import static neo.Renderer.Image_program.R_LoadImageProgram;
import neo.Renderer.Material.textureFilter_t;
import static neo.Renderer.Material.textureFilter_t.TF_DEFAULT;
import static neo.Renderer.Material.textureFilter_t.TF_LINEAR;
import static neo.Renderer.Material.textureFilter_t.TF_NEAREST;
import neo.Renderer.Material.textureRepeat_t;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP_TO_BORDER;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP_TO_ZERO;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP_TO_ZERO_ALPHA;
import static neo.Renderer.Material.textureRepeat_t.TR_REPEAT;
import static neo.Renderer.RenderSystem_init.GL_CheckErrors;
import static neo.Renderer.qgl.qglBindTexture;
import static neo.Renderer.qgl.qglColorTableEXT;
import static neo.Renderer.qgl.qglCompressedTexImage2DARB;
import static neo.Renderer.qgl.qglCopyTexImage2D;
import static neo.Renderer.qgl.qglCopyTexSubImage2D;
import static neo.Renderer.qgl.qglDeleteTextures;
import static neo.Renderer.qgl.qglDisable;
import static neo.Renderer.qgl.qglEnable;
import static neo.Renderer.qgl.qglGenTextures;
import static neo.Renderer.qgl.qglGetCompressedTexImageARB;
import static neo.Renderer.qgl.qglGetTexImage;
import static neo.Renderer.qgl.qglPixelStorei;
import static neo.Renderer.qgl.qglPrioritizeTextures;
import static neo.Renderer.qgl.qglReadBuffer;
import static neo.Renderer.qgl.qglTexImage2D;
import static neo.Renderer.qgl.qglTexImage3D;
import static neo.Renderer.qgl.qglTexParameterf;
import static neo.Renderer.qgl.qglTexParameteri;
import static neo.Renderer.qgl.qglTexSubImage2D;
import static neo.Renderer.tr_backend.RB_LogComment;
import static neo.Renderer.tr_local.MAX_MULTITEXTURE_UNITS;
import static neo.Renderer.tr_local.backEnd;
import static neo.Renderer.tr_local.glConfig;
import neo.Renderer.tr_local.tmu_t;
import static neo.Renderer.tr_local.tr;
import neo.TempDump.CPP_class;
import neo.TempDump.CPP_class.Bool;
import neo.TempDump.CPP_class.Pointer;
import static neo.TempDump.NOT;
import neo.TempDump.SERiAL;
import static neo.TempDump.ctos;
import static neo.TempDump.flatten;
import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_RENDERER;
import static neo.framework.CVarSystem.cvarSystem;
import neo.framework.CVarSystem.idCVar;
import static neo.framework.CmdSystem.CMD_FL_RENDERER;
import static neo.framework.CmdSystem.cmdSystem;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.Common.MemInfo_t;
import static neo.framework.Common.com_developer;
import static neo.framework.Common.com_machineSpec;
import static neo.framework.Common.com_makingBuild;
import static neo.framework.Common.com_purgeAll;
import static neo.framework.Common.com_videoRam;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.declManager;
import static neo.framework.FileSystem_h.FILE_NOT_FOUND_TIMESTAMP;
import neo.framework.FileSystem_h.backgroundDownload_s;
import static neo.framework.FileSystem_h.dlType_t.DLTYPE_FILE;
import static neo.framework.FileSystem_h.fileSystem;
import neo.framework.File_h.idFile;
import static neo.framework.Session.session;
import neo.idlib.CmdArgs.idCmdArgs;
import static neo.idlib.Lib.LittleLong;
import neo.idlib.Lib.idException;
import static neo.idlib.Text.Str.FILE_HASH_SIZE;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Str.va;
import neo.idlib.containers.HashIndex.idHashIndex;
import neo.idlib.containers.List.idList;
import neo.idlib.containers.StrList.idStrList;
import static neo.idlib.hashing.MD4.MD4_BlockChecksum;
import neo.idlib.math.Math_h.idMath;
import neo.idlib.math.Vector.idVec3;
import static neo.sys.win_shared.Sys_Milliseconds;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.ARBTextureCompression.GL_COMPRESSED_RGBA_ARB;
import static org.lwjgl.opengl.ARBTextureCompression.GL_COMPRESSED_RGB_ARB;
import static org.lwjgl.opengl.EXTBgra.GL_BGRA_EXT;
import static org.lwjgl.opengl.EXTBgra.GL_BGR_EXT;
import static org.lwjgl.opengl.EXTPalettedTexture.GL_COLOR_INDEX8_EXT;
import static org.lwjgl.opengl.EXTSharedTexturePalette.GL_SHARED_TEXTURE_PALETTE_EXT;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.EXTTextureLODBias.GL_TEXTURE_LOD_BIAS_EXT;
import static org.lwjgl.opengl.GL11.GL_ALPHA;
import static org.lwjgl.opengl.GL11.GL_ALPHA8;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_INDEX;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_INTENSITY8;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE8;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE8_ALPHA8;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE_ALPHA;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGB5;
import static org.lwjgl.opengl.GL11.GL_RGB8;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA4;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.NVTextureCompressionVTC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
import static org.lwjgl.opengl.NVTextureCompressionVTC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
import static org.lwjgl.opengl.NVTextureCompressionVTC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
import static org.lwjgl.opengl.NVTextureCompressionVTC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
import static org.lwjgl.opengl.NVTextureRectangle.GL_TEXTURE_RECTANGLE_NV;

/**
 *
 */
public class Image {

    // do this with a pointer, in case we want to make the actual manager
    // a private virtual subclass
    private static final idImageManager imageManager = new idImageManager();
    // pointer to global list for the rest of the system
    public static idImageManager globalImages = imageManager;

    /*
     ====================================================================

     IMAGE

     idImage have a one to one correspondance with OpenGL textures.

     No texture is ever used that does not have a corresponding idImage.

     no code outside this unit should call any of these OpenGL functions:

     qglGenTextures
     qglDeleteTextures
     qglBindTexture

     qglTexParameter

     qglTexImage
     qglTexSubImage

     qglCopyTexImage
     qglCopyTexSubImage

     qglEnable( GL_TEXTURE_* )
     qglDisable( GL_TEXTURE_* )

     ====================================================================
     */
    enum imageState_t {

        IS_UNLOADED, // no gl texture number
        IS_PARTIAL, // has a texture number and the low mip levels loaded
        IS_LOADED		// has a texture number and the full mip hierarchy
    };
    //
    static final int MAX_TEXTURE_LEVELS = 14;
    //
    // surface description flags
    static final int DDSF_CAPS          = 0x00000001;
    static final int DDSF_HEIGHT        = 0x00000002;
    static final int DDSF_WIDTH         = 0x00000004;
    static final int DDSF_PITCH         = 0x00000008;
    static final int DDSF_PIXELFORMAT   = 0x00001000;
    static final int DDSF_MIPMAPCOUNT   = 0x00020000;
    static final int DDSF_LINEARSIZE    = 0x00080000;
    static final int DDSF_DEPTH         = 0x00800000;
    //
    // pixel format flags
    static final int DDSF_ALPHAPIXELS   = 0x00000001;
    static final int DDSF_FOURCC        = 0x00000004;
    static final int DDSF_RGB           = 0x00000040;
    static final int DDSF_RGBA          = 0x00000041;
    //
    // our extended flags
    static final int DDSF_ID_INDEXCOLOR = 0x10000000;
    static final int DDSF_ID_MONOCHROME = 0x20000000;
    //
    // dwCaps1 flags
    static final int DDSF_COMPLEX       = 0x00000008;
    static final int DDSF_TEXTURE       = 0x00001000;
    static final int DDSF_MIPMAP        = 0x00400000;
    //

    static int DDS_MAKEFOURCC(final int a, final int b, final int c, final int d) {
        return ((a << 0) | (b << 8) | (c << 16) | (d << 24));
    }

    private static final int DDS_MAKEFOURCC_DXT1 = (('D' << 0) | ('X' << 8) | ('T' << 16) | ('1' << 24));
    private static final int DDS_MAKEFOURCC_DXT3 = (('D' << 0) | ('X' << 8) | ('T' << 16) | ('3' << 24));
    private static final int DDS_MAKEFOURCC_DXT5 = (('D' << 0) | ('X' << 8) | ('T' << 16) | ('5' << 24));
    private static final int DDS_MAKEFOURCC_RXGB = (('R' << 0) | ('X' << 8) | ('G' << 16) | ('B' << 24));
//

    static class ddsFilePixelFormat_t {

        public static final transient int SIZE
                = CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE;

        int /*long*/ dwSize;
        int /*long*/ dwFlags;
        int /*long*/ dwFourCC;
        int /*long*/ dwRGBBitCount;
        int /*long*/ dwRBitMask;
        int /*long*/ dwGBitMask;
        int /*long*/ dwBBitMask;
        int /*long*/ dwABitMask;

        public ddsFilePixelFormat_t(int dwSize, int dwFlags, int dwFourCC, int dwRGBBitCount, int dwRBitMask, int dwGBitMask, int dwBBitMask, int dwABitMask) {
            this.dwSize = dwSize;
            this.dwFlags = dwFlags;
            this.dwFourCC = dwFourCC;
            this.dwRGBBitCount = dwRGBBitCount;
            this.dwRBitMask = dwRBitMask;
            this.dwGBitMask = dwGBitMask;
            this.dwBBitMask = dwBBitMask;
            this.dwABitMask = dwABitMask;
        }

    }

    ;

    static class ddsFileHeader_t implements SERiAL {

        private static final transient int SIZE
                                                  = CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + (CPP_class.Long.SIZE * 11)
                + ddsFilePixelFormat_t.SIZE
                + CPP_class.Long.SIZE
                + CPP_class.Long.SIZE
                + (CPP_class.Long.SIZE * 3);
        public static final transient  int BYTES = SIZE / 8;

        int /*long*/ dwSize;
        int /*long*/ dwFlags;
        int /*long*/ dwHeight;
        int /*long*/ dwWidth;
        int /*long*/ dwPitchOrLinearSize;
        int /*long*/ dwDepth;
        int /*long*/ dwMipMapCount;
        int /*long*/[] dwReserved1 = new int[11];
        ddsFilePixelFormat_t ddspf;
        int/*long*/ dwCaps1;
        int/*long*/ dwCaps2;
        int/*long*/[] dwReserved2 = new int[3];

        public ddsFileHeader_t() {
        }

        public ddsFileHeader_t(final ByteBuffer data) {
            this.Read(data);
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {

            this.dwSize = buffer.getInt();
            this.dwFlags = buffer.getInt();
            this.dwHeight = buffer.getInt();
            this.dwWidth = buffer.getInt();
            this.dwPitchOrLinearSize = buffer.getInt();
            this.dwDepth = buffer.getInt();
            this.dwMipMapCount = buffer.getInt();
            for (int a = 0; a < dwReserved1.length; a++) {
                dwReserved1[a] = buffer.getInt();
            }
            this.ddspf = new ddsFilePixelFormat_t(
                    buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt(),
                    buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt());
            this.dwCaps1 = buffer.getInt();
            this.dwCaps2 = buffer.getInt();
            for (int b = 0; b < dwReserved2.length; b++) {
                dwReserved2[b] = buffer.getInt();
            }
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    ;

    // increasing numeric values imply more information is stored
    enum textureDepth_t {

        TD_SPECULAR, // may be compressed, and always zeros the alpha channel
        TD_DIFFUSE, // may be compressed
        TD_DEFAULT, // will use compressed formats when possible
        TD_BUMP, // may be compressed with 8 bit lookup
        TD_HIGH_QUALITY            // either 32 bit or a component format, no loss at all
    }

    ;

    enum textureType_t {

        TT_DISABLED,
        TT_2D,
        TT_3D,
        TT_CUBIC,
        TT_RECT
    }

    ;

    enum cubeFiles_t {

        CF_2D, // not a cube map
        CF_NATIVE, // _px, _nx, _py, etc, directly sent to GL
        CF_CAMERA        // _forward, _back, etc, rotated and flipped as needed before sending to GL
    }

    ;
    //
    public static final int MAX_IMAGE_NAME = 256;

    public static abstract class GeneratorFunction {

        public abstract void run(final idImage image);
    }

    public static class idImage {

        public static final transient int SIZE
                = Integer.SIZE
                + CPP_class.Enum.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Pointer.SIZE//idImage
                + Bool.SIZE
                + Bool.SIZE
                + backgroundDownload_s.SIZE
                + Pointer.SIZE//idImage
                + idStr.SIZE
                + Pointer.SIZE//idImage.//TODO:does a function pointer have size?
                + Bool.SIZE
                + CPP_class.Enum.SIZE
                + CPP_class.Enum.SIZE
                + CPP_class.Enum.SIZE
                + CPP_class.Enum.SIZE
                + Bool.SIZE
                + Bool.SIZE
                + Bool.SIZE
                + Bool.SIZE
                + Bool.SIZE
                + Long.SIZE//ID_TIME_T timestamp
                + Integer.SIZE//char * imageHash
                + Integer.SIZE
                + (Integer.SIZE * 3)
                + Integer.SIZE
                + (Pointer.SIZE * 2)//idImage
                + Pointer.SIZE //idImage
                + Integer.SIZE;

        // data commonly accessed is grouped here
        public static final int TEXTURE_NOT_LOADED = -1;
        public /*GLuint*/ int                  texNum;            // gl texture binding, will be TEXTURE_NOT_LOADED if not loaded
        public            textureType_t        type;
        public            int                  frameUsed;         // for texture usage in frame statistics
        public            int                  bindCount;         // incremented each bind
        //
        // background loading information
        public            idImage              partialImage;      // shrunken, space-saving version
        public            boolean              isPartialImage;    // true if this is pointed to by another image
        public            boolean              backgroundLoadInProgress;    // true if another thread is reading the complete d3t file
        public            backgroundDownload_s bgl;
        public            idImage              bglNext;           // linked from tr.backgroundImageLoads
        //
        // parameters that define this image
        public            idStr                imgName;           // game path, including extension (except for cube maps), may be an image program
        public            GeneratorFunction    generatorFunction; // NULL for files           //public void (        *generatorFunction)( idImage *image );	// NULL for files
        public            boolean              allowDownSize;     // this also doubles as a don't-partially-load flag
        public            textureFilter_t      filter;
        public            textureRepeat_t      repeat;
        public            textureDepth_t       depth;
        public            cubeFiles_t          cubeFiles;         // determines the naming and flipping conventions for the six images
        //
        public            boolean              referencedOutsideLevelLoad;
        public            boolean              levelLoadReferenced;             // for determining if it needs to be purged
        public            boolean              precompressedFile; // true when it was loaded from a .d3t file
        public            boolean              defaulted;         // true if the default image was generated because a file couldn't be loaded
        public boolean[] isMonochrome = {false};                  // so the NV20 path can use a reduced pass count
        public/*ID_TIME_T*/ long[] timestamp = {0};               // the most recent of all images used in creation, for reloadImages command
        //
        public String imageHash;            // for identical-image checking
        //
        public int    classification;       // just for resource profiling
        //
        // data for listImages
        public int    uploadWidth, uploadHeight, uploadDepth;     // after power of two, downsample, and MAX_TEXTURE_SIZE
        public int     internalFormat;
        //
        public idImage cacheUsagePrev, cacheUsageNext;            // for dynamic cache purging of old images
        //
        public idImage hashNext;            // for hash chains to speed lookup
        //
        public int     refCount;            // overall ref count
        //
        //
        private static int DEBUG_COUNTER = 0;
        private int _COUNTER;

        public idImage() {
            _COUNTER = DEBUG_COUNTER++;
            texNum = TEXTURE_NOT_LOADED;
            partialImage = null;
            type = TT_DISABLED;
            isPartialImage = false;
            frameUsed = 0;
            classification = 0;
            backgroundLoadInProgress = false;
            bgl = new backgroundDownload_s();
            bgl.opcode = DLTYPE_FILE;
            bgl.f = null;
            bglNext = null;
            imgName = new idStr();
            generatorFunction = null;
            allowDownSize = false;
            filter = TF_DEFAULT;
            repeat = TR_REPEAT;
            depth = TD_DEFAULT;
            cubeFiles = CF_2D;
            referencedOutsideLevelLoad = false;
            levelLoadReferenced = false;
            precompressedFile = false;
            defaulted = false;
//            timestamp[0] = 0;
            bindCount = 0;
            uploadWidth = uploadHeight = uploadDepth = 0;
            internalFormat = 0;
            cacheUsagePrev = cacheUsageNext = null;
            hashNext = null;
//            isMonochrome[0] = false;
            refCount = 0;
        }

        /**
         * copy constructor
         *
         * @param image
         */
        idImage(idImage image) {

            this.texNum = image.texNum;
            this.type = image.type;
            this.frameUsed = image.frameUsed;
            this.bindCount = image.bindCount;
            this.partialImage = image.partialImage;//pointer
            this.isPartialImage = image.isPartialImage;
            this.backgroundLoadInProgress = image.backgroundLoadInProgress;
            this.bgl = new backgroundDownload_s(image.bgl);
            this.bglNext = image.bglNext;//pointer
            this.imgName = new idStr(image.imgName);
            this.generatorFunction = image.generatorFunction;
            this.allowDownSize = image.allowDownSize;
            this.filter = image.filter;
            this.repeat = image.repeat;
            this.depth = image.depth;
            this.cubeFiles = image.cubeFiles;
            this.referencedOutsideLevelLoad = image.referencedOutsideLevelLoad;
            this.levelLoadReferenced = image.levelLoadReferenced;
            this.precompressedFile = image.precompressedFile;
            this.defaulted = image.defaulted;
            this.isMonochrome[0] = image.isMonochrome[0];
            this.timestamp[0] = image.timestamp[0];
            this.imageHash = image.imageHash;
            this.classification = image.classification;
            this.uploadWidth = image.uploadWidth;
            this.uploadHeight = image.uploadHeight;
            this.uploadDepth = image.uploadDepth;
            this.internalFormat = image.internalFormat;
            this.cacheUsagePrev = image.cacheUsagePrev;//pointer
            this.cacheUsageNext = image.cacheUsageNext;//pointer
            this.hashNext = image.hashNext;//pointer
            this.refCount = image.refCount;
        }

        /*
         ==============
         Bind

         Automatically enables 2D mapping, cube mapping, or 3D texturing if needed
         ==============
         */
        // Makes this image active on the current GL texture unit.
        // automatically enables or disables cube mapping or texture3D
        // May perform file loading if the image was not preloaded.
        // May start a background image read.
        public void Bind() {
            if (tr.logFile != null) {
                RB_LogComment("idImage::Bind( %s )\n", imgName.toString());
            }
            
            // if this is an image that we are caching, move it to the front of the LRU chain
            if (partialImage != null) {
                if (cacheUsageNext != null) {
                    // unlink from old position
                    cacheUsageNext.cacheUsagePrev = cacheUsagePrev;
                    cacheUsagePrev.cacheUsageNext = cacheUsageNext;
                }
                // link in at the head of the list
                cacheUsageNext = globalImages.cacheLRU.cacheUsageNext;
                cacheUsagePrev = globalImages.cacheLRU;

                cacheUsageNext.cacheUsagePrev = this;
                cacheUsagePrev.cacheUsageNext = this;
            }

            // load the image if necessary (FIXME: not SMP safe!)
            if (texNum == TEXTURE_NOT_LOADED) {
                if (partialImage != null) {
                    // if we have a partial image, go ahead and use that
                    this.partialImage.Bind();

                    // start a background load of the full thing if it isn't already in the queue
                    if (!backgroundLoadInProgress) {
                        StartBackgroundImageLoad();
                    }
                    return;
                }

                // load the image on demand here, which isn't our normal game operating mode
                ActuallyLoadImage(true, true);	// check for precompressed, load is from back end
            }

            // bump our statistic counters
            frameUsed = backEnd.frameCount;
            bindCount++;

            tmu_t tmu = backEnd.glState.tmu[backEnd.glState.currenttmu];

            // enable or disable apropriate texture modes
            if (tmu.textureType != type && (backEnd.glState.currenttmu < glConfig.maxTextureUnits)) {
                if (tmu.textureType == TT_CUBIC) {
                    qglDisable(GL_TEXTURE_CUBE_MAP/*_EXT*/);
                } else if (tmu.textureType == TT_3D) {
                    qglDisable(GL_TEXTURE_3D);
                } else if (tmu.textureType == TT_2D) {
                    qglDisable(GL_TEXTURE_2D);
                }

                if (type == TT_CUBIC) {
                    qglEnable(GL_TEXTURE_CUBE_MAP/*_EXT*/);
                } else if (type == TT_3D) {
                    qglEnable(GL_TEXTURE_3D);
                } else if (type == TT_2D) {
                    qglEnable(GL_TEXTURE_2D);
                }
                tmu.textureType = type;
            }

            // bind the texture
            if (type == TT_2D) {
                if (tmu.current2DMap != texNum) {
                    tmu.current2DMap = texNum;
                    qglBindTexture(GL_TEXTURE_2D, texNum);
                    if (texNum == 25){
                        System.out.println("Blaaaaaaasphemy!");
            }
        }
    } else if (type == TT_CUBIC) {
        if (tmu.currentCubeMap != texNum) {
            tmu.currentCubeMap = texNum;
            qglBindTexture(GL_TEXTURE_CUBE_MAP/*_EXT*/, texNum);
        }
            } else if (type == TT_3D) {
                if (tmu.current3DMap != texNum) {
                    tmu.current3DMap = texNum;
                    qglBindTexture(GL_TEXTURE_3D, texNum);
                }
            }

            if (com_purgeAll.GetBool()) {
                float/*GLclampf*/ priority = 1.0f;
                qglPrioritizeTextures(1, texNum, priority);
            }
        }
        private static int DBG_Bind = 0;


        /*
         ==============
         BindFragment

         Fragment programs explicitly say which type of map they want, so we don't need to
         do any enable / disable changes
         ==============
         */
        // for use with fragment programs, doesn't change any enable2D/3D/cube states
        public void BindFragment() {
            if (tr.logFile != null) {
                RB_LogComment("idImage::BindFragment %s )\n", imgName.toString());
            }

            // if this is an image that we are caching, move it to the front of the LRU chain
            if (partialImage != null) {
                if (cacheUsageNext != null) {
                    // unlink from old position
                    cacheUsageNext.cacheUsagePrev = cacheUsagePrev;
                    cacheUsagePrev.cacheUsageNext = cacheUsageNext;
                }
                // link in at the head of the list
                cacheUsageNext = globalImages.cacheLRU.cacheUsageNext;
                cacheUsagePrev = globalImages.cacheLRU;

                cacheUsageNext.cacheUsagePrev = this;
                cacheUsagePrev.cacheUsageNext = this;
            }

            // load the image if necessary (FIXME: not SMP safe!)
            if (texNum == TEXTURE_NOT_LOADED) {
                if (partialImage != null) {
                    // if we have a partial image, go ahead and use that
                    this.partialImage.BindFragment();

                    // start a background load of the full thing if it isn't already in the queue
                    if (!backgroundLoadInProgress) {
                        StartBackgroundImageLoad();
                    }
                    return;
                }

                // load the image on demand here, which isn't our normal game operating mode
                ActuallyLoadImage(true, true);	// check for precompressed, load is from back end
            }

            // bump our statistic counters
            frameUsed = backEnd.frameCount;
            bindCount++;

            // bind the texture
            if (type == TT_2D) {
                qglBindTexture(GL_TEXTURE_2D, texNum);
            } else if (type == TT_RECT) {
                qglBindTexture(GL_TEXTURE_RECTANGLE_NV, texNum);
            } else if (type == TT_CUBIC) {
                qglBindTexture(GL_TEXTURE_CUBE_MAP/*_EXT*/, texNum);
            } else if (type == TT_3D) {
                qglBindTexture(GL_TEXTURE_3D, texNum);
            }
        }

//
        // deletes the texture object, but leaves the structure so it can be reloaded
        public void PurgeImage() {
            if (texNum != TEXTURE_NOT_LOADED) {
                // sometimes is NULL when exiting with an error
//                if (qglDeleteTextures) {
                try {
                    qglDeleteTextures(1, texNum);// this should be the ONLY place it is ever called!
                } catch (RuntimeException e) {//TODO:deal with this.
//                    e.printStackTrace();
                }
//                }
                texNum = TEXTURE_NOT_LOADED;
            }

            // clear all the current binding caches, so the next bind will do a real one
            for (int i = 0; i < MAX_MULTITEXTURE_UNITS; i++) {
                backEnd.glState.tmu[i].current2DMap = -1;
                backEnd.glState.tmu[i].current3DMap = -1;
                backEnd.glState.tmu[i].currentCubeMap = -1;
            }
        }
//

        /*
         ================
         GenerateImage

         The alpha channel bytes should be 255 if you don't
         want the channel.

         We need a material characteristic to ask for specific texture modes.

         Designed limitations of flexibility:

         No support for texture borders.

         No support for texture border color.

         No support for texture environment colors or GL_BLEND or GL_DECAL
         texture environments, because the automatic optimization to single
         or dual component textures makes those modes potentially undefined.

         No non-power-of-two images.

         No palettized textures.

         There is no way to specify separate wrap/clamp values for S and T

         There is no way to specify explicit mip map levels

         ================
         */
        // used by callback functions to specify the actual data
        // data goes from the bottom to the top line of the image, as OpenGL expects it
        // These perform an implicit Bind() on the current texture unit
        // FIXME: should we implement cinematics this way, instead of with explicit calls?
        public void GenerateImage(final ByteBuffer pic, int width, int height,
                textureFilter_t filterParm, boolean allowDownSizeParm,
                textureRepeat_t repeatParm, textureDepth_t depthParm) {
            boolean preserveBorder;
            ByteBuffer scaledBuffer;
            int[] scaled_width = {0}, scaled_height = {0};
            ByteBuffer shrunk;

            PurgeImage();

            filter = filterParm;
            allowDownSize = allowDownSizeParm;
            repeat = repeatParm;
            depth = depthParm;

            // if we don't have a rendering context, just return after we
            // have filled in the parms.  We must have the values set, or
            // an image match from a shader before OpenGL starts would miss
            // the generated texture
            if (!glConfig.isInitialized) {
                return;
            }

            // don't let mip mapping smear the texture into the clamped border
            if (repeat == TR_CLAMP_TO_ZERO) {
                preserveBorder = true;
            } else {
                preserveBorder = false;
            }

            // make sure it is a power of 2
            scaled_width[0] = MakePowerOfTwo(width);
            scaled_height[0] = MakePowerOfTwo(height);

            if (scaled_width[0] != width || scaled_height[0] != height) {
                common.Error("R_CreateImage: not a power of 2 image");
            }

            // Optionally modify our width/height based on options/hardware
            GetDownsize(scaled_width, scaled_height);

//            scaledBuffer = null;
            // generate the texture number
            texNum = qglGenTextures();
            System.out.println(imgName + ": " + texNum);

            // select proper internal format before we resample
            internalFormat = SelectInternalFormat(pic, 1, width, height, depth, isMonochrome);

            // copy or resample data as appropriate for first MIP level
            if ((scaled_width[0] == width) && (scaled_height[0] == height)) {
                // we must copy even if unchanged, because the border zeroing
                // would otherwise modify const data
                scaledBuffer = BufferUtils.createByteBuffer(width * height * 4);// R_StaticAlloc(scaled_width[0] * scaled_height[0]);
                byte[] temp = new byte[width * height * 4];
//		memcpy (scaledBuffer, pic, width*height*4);
                pic.rewind();
                pic.get(temp);
                scaledBuffer.put(temp);//System.arraycopy(pic.array(), 0, scaledBuffer, 0, width * height * 4);
            } else {
                // resample down as needed (FIXME: this doesn't seem like it resamples anymore!)
                // scaledBuffer = R_ResampleTexture( pic, width, height, width >>= 1, height >>= 1 );
                scaledBuffer = R_MipMap(pic, width, height, preserveBorder);
                width >>= 1;
                height >>= 1;
                if (width < 1) {
                    width = 1;
                }
                if (height < 1) {
                    height = 1;
                }

                while (width > scaled_width[0] || height > scaled_height[0]) {
                    shrunk = R_MipMap(scaledBuffer, width, height, preserveBorder);
                    scaledBuffer.clear();//R_StaticFree(scaledBuffer);
                    scaledBuffer.put(shrunk);

                    width >>= 1;
                    height >>= 1;
                    if (width < 1) {
                        width = 1;
                    }
                    if (height < 1) {
                        height = 1;
                    }
                }

                // one might have shrunk down below the target size
                scaled_width[0] = width;
                scaled_height[0] = height;
            }

            uploadHeight = scaled_height[0];
            uploadWidth = scaled_width[0];
            type = TT_2D;

            // zero the border if desired, allowing clamped projection textures
            // even after picmip resampling or careless artists.
            if (repeat == TR_CLAMP_TO_ZERO) {
                final byte[] rgba = new byte[4];

                rgba[0] = rgba[1] = rgba[2] = 0;
                rgba[3] = (byte) 255;
                R_SetBorderTexels(scaledBuffer, width, height, rgba);
            }
            if (repeat == TR_CLAMP_TO_ZERO_ALPHA) {
                final byte[] rgba = new byte[4];

                rgba[0] = rgba[1] = rgba[2] = (byte) 255;
                rgba[3] = 0;
                R_SetBorderTexels(scaledBuffer, width, height, rgba);
            }

            if (generatorFunction == null && (depth == TD_BUMP && globalImages.image_writeNormalTGA.GetBool() || depth != TD_BUMP && globalImages.image_writeTGA.GetBool())) {
                // Optionally write out the texture to a .tga
//                String[] filename = {null};
                String[] filename = new String[1];
                ImageProgramStringToCompressedFileName(imgName.toString(), filename);
                final int ext = filename[0].lastIndexOf('.');
                if (ext > -1) {
//			strcpy( ext, ".tga" );
                    filename[0] = filename[0].substring(0, ext) + ".tga";// + filename[0].substring(ext);
                    // swap the red/alpha for the write
			/*
                     if ( depth == TD_BUMP ) {
                     for ( int i = 0; i < scaled_width * scaled_height * 4; i += 4 ) {
                     scaledBuffer[ i ] = scaledBuffer[ i + 3 ];
                     scaledBuffer[ i + 3 ] = 0;
                     }
                     }
                     */
                    R_WriteTGA(filename[0], scaledBuffer, scaled_width[0], scaled_height[0], false);

                    // put it back
			/*
                     if ( depth == TD_BUMP ) {
                     for ( int i = 0; i < scaled_width * scaled_height * 4; i += 4 ) {
                     scaledBuffer[ i + 3 ] = scaledBuffer[ i ];
                     scaledBuffer[ i ] = 0;
                     }
                     }
                     */
                }
            }

            // swap the red and alpha for rxgb support
            // do this even on tga normal maps so we only have to use
            // one fragment program
            // if the image is precompressed ( either in palletized mode or true rxgb mode )
            // then it is loaded above and the swap never happens here
            if (depth == TD_BUMP && globalImages.image_useNormalCompression.GetInteger() != 1) {
                for (int i = 0; i < scaled_width[0] * scaled_height[0] * 4; i += 4) {
                    scaledBuffer.put(i + 3, scaledBuffer.get(i));
                    scaledBuffer.put(i, (byte) 0);
                }
            }
            // upload the main image level
            Bind();

            if (internalFormat == GL_COLOR_INDEX8_EXT) {
                /*
                 if ( depth == TD_BUMP ) {
                 for ( int i = 0; i < scaled_width * scaled_height * 4; i += 4 ) {
                 scaledBuffer[ i ] = scaledBuffer[ i + 3 ];
                 scaledBuffer[ i + 3 ] = 0;
                 }
                 }
                 */
                UploadCompressedNormalMap(scaled_width[0], scaled_height[0], scaledBuffer.array(), 0);
            } else {
                scaledBuffer.rewind();
                qglTexImage2D(GL_TEXTURE_2D, 0, internalFormat, scaled_width[0], scaled_height[0], 0, GL_RGBA, GL_UNSIGNED_BYTE, scaledBuffer);
            }

            // create and upload the mip map levels, which we do in all cases, even if we don't think they are needed
            int miplevel;

            miplevel = 0;
            while (scaled_width[0] > 1 || scaled_height[0] > 1) {
                // preserve the border after mip map unless repeating
                shrunk = R_MipMap(scaledBuffer, scaled_width[0], scaled_height[0], preserveBorder);
                scaledBuffer.clear();//R_StaticFree(scaledBuffer);
                scaledBuffer.put(shrunk);

                scaled_width[0] >>= 1;
                scaled_height[0] >>= 1;
                if (scaled_width[0] < 1) {
                    scaled_width[0] = 1;
                }
                if (scaled_height[0] < 1) {
                    scaled_height[0] = 1;
                }
                miplevel++;

                // this is a visualization tool that shades each mip map
                // level with a different color so you can see the
                // rasterizer's texture level selection algorithm
                // Changing the color doesn't help with lumminance/alpha/intensity formats...
                if (depth == TD_DIFFUSE && globalImages.image_colorMipLevels.GetBool()) {
                    R_BlendOverTexture(scaledBuffer, scaled_width[0] * scaled_height[0], mipBlendColors[miplevel]);
                }

                // upload the mip map
                if (internalFormat == GL_COLOR_INDEX8_EXT) {
                    UploadCompressedNormalMap(scaled_width[0], scaled_height[0], scaledBuffer.array(), miplevel);
                } else {
                    qglTexImage2D(GL_TEXTURE_2D, miplevel, internalFormat, scaled_width[0], scaled_height[0],
                            0, GL_RGBA, GL_UNSIGNED_BYTE, scaledBuffer);
                }
            }
//
//            if (scaledBuffer != null) {
//                R_StaticFree(scaledBuffer);
//            }

            SetImageFilterAndRepeat();

            // see if we messed anything up
            GL_CheckErrors();
        }
        static final int[][] mipBlendColors/*[16][4]*/ = {
                    {0, 0, 0, 0},
                    {255, 0, 0, 128},
                    {0, 255, 0, 128},
                    {0, 0, 255, 128},
                    {255, 0, 0, 128},
                    {0, 255, 0, 128},
                    {0, 0, 255, 128},
                    {255, 0, 0, 128},
                    {0, 255, 0, 128},
                    {0, 0, 255, 128},
                    {255, 0, 0, 128},
                    {0, 255, 0, 128},
                    {0, 0, 255, 128},
                    {255, 0, 0, 128},
                    {0, 255, 0, 128},
                    {0, 0, 255, 128},};

        public void Generate3DImage(final ByteBuffer pic, int width, int height, int picDepth,
                textureFilter_t filterParm, boolean allowDownSizeParm,
                textureRepeat_t repeatParm, textureDepth_t minDepthParm) {
            int scaled_width, scaled_height, scaled_depth;

            PurgeImage();

            filter = filterParm;
            allowDownSize = allowDownSizeParm;
            repeat = repeatParm;
            depth = minDepthParm;

            // if we don't have a rendering context, just return after we
            // have filled in the parms.  We must have the values set, or
            // an image match from a shader before OpenGL starts would miss
            // the generated texture
            if (!glConfig.isInitialized) {
                return;
            }

            // make sure it is a power of 2
            scaled_width = MakePowerOfTwo(width);
            scaled_height = MakePowerOfTwo(height);
            scaled_depth = MakePowerOfTwo(picDepth);
            if (scaled_width != width || scaled_height != height || scaled_depth != picDepth) {
                common.Error("R_Create3DImage: not a power of 2 image");
            }

            // FIXME: allow picmip here
            // generate the texture number
            texNum = qglGenTextures();
            System.out.println(imgName + ": " + texNum);

            // select proper internal format before we resample
            // this function doesn't need to know it is 3D, so just make it very "tall"
            internalFormat = SelectInternalFormat(pic, 1, width, height * picDepth, minDepthParm, isMonochrome);

            uploadHeight = scaled_height;
            uploadWidth = scaled_width;
            uploadDepth = scaled_depth;

            type = TT_3D;

            // upload the main image level
            Bind();

            qglTexImage3D(GL_TEXTURE_3D, 0, internalFormat, scaled_width, scaled_height, scaled_depth,
                    0, GL_RGBA, GL_UNSIGNED_BYTE, pic);

            // create and upload the mip map levels
            int miplevel;
            ByteBuffer scaledBuffer;
            ByteBuffer shrunk;

            scaledBuffer = BufferUtils.createByteBuffer(scaled_width * scaled_height * scaled_depth * 4);
            scaledBuffer.put(pic);// memcpy( scaledBuffer, pic, scaled_width * scaled_height * scaled_depth * 4 );
            miplevel = 0;
            while (scaled_width > 1 || scaled_height > 1 || scaled_depth > 1) {
                // preserve the border after mip map unless repeating
                shrunk = R_MipMap3D(scaledBuffer, scaled_width, scaled_height, scaled_depth, (repeat != TR_REPEAT));
                scaledBuffer.clear();// R_StaticFree(scaledBuffer);
                scaledBuffer.put(shrunk);

                scaled_width >>= 1;
                scaled_height >>= 1;
                scaled_depth >>= 1;
                if (scaled_width < 1) {
                    scaled_width = 1;
                }
                if (scaled_height < 1) {
                    scaled_height = 1;
                }
                if (scaled_depth < 1) {
                    scaled_depth = 1;
                }
                miplevel++;

                // upload the mip map
                qglTexImage3D(GL_TEXTURE_3D, miplevel, internalFormat, scaled_width, scaled_height, scaled_depth,
                        0, GL_RGBA, GL_UNSIGNED_BYTE, scaledBuffer);
            }
            scaledBuffer.clear();// R_StaticFree(scaledBuffer);

            // set the minimize / maximize filtering
            switch (filter) {
                case TF_DEFAULT:
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, globalImages.textureMinFilter);
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, globalImages.textureMaxFilter);
                    break;
                case TF_LINEAR:
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    break;
                case TF_NEAREST:
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                    break;
                default:
                    common.FatalError("R_CreateImage: bad texture filter");
            }

            // set the wrap/clamp modes
            switch (repeat) {
                case TR_REPEAT:
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);
                    break;
                case TR_CLAMP_TO_BORDER:
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
                    break;
                case TR_CLAMP_TO_ZERO:
                case TR_CLAMP_TO_ZERO_ALPHA:
                case TR_CLAMP:
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    qglTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
                    break;
                default:
                    common.FatalError("R_CreateImage: bad texture repeat");
            }

            // see if we messed anything up
            GL_CheckErrors();
        }

        /*
         ====================
         GenerateCubeImage

         Non-square cube sides are not allowed
         ====================
         */
        public void GenerateCubeImage(final ByteBuffer[] pics/*[6]*/, int size,
                textureFilter_t filterParm, boolean allowDownSizeParm,
                textureDepth_t depthParm) {
            int scaled_width, scaled_height;
            int width, height;
            int i;

            PurgeImage();

            filter = filterParm;
            allowDownSize = allowDownSizeParm;
            depth = depthParm;

            type = TT_CUBIC;

            // if we don't have a rendering context, just return after we
            // have filled in the parms.  We must have the values set, or
            // an image match from a shader before OpenGL starts would miss
            // the generated texture
            if (!glConfig.isInitialized) {
                return;
            }

            if (!glConfig.cubeMapAvailable) {
                return;
            }

            width = height = size;

            // generate the texture number
            texNum = qglGenTextures();
            System.out.println(imgName + ": " + texNum);

            // select proper internal format before we resample
            internalFormat = SelectInternalFormat(pics, 6, width, height, depth, isMonochrome);

            // don't bother with downsample for now
            scaled_width = width;
            scaled_height = height;

            uploadHeight = scaled_height;
            uploadWidth = scaled_width;

            Bind();

            // no other clamp mode makes sense
            qglTexParameteri(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            qglTexParameteri(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            // set the minimize / maximize filtering
            switch (filter) {
                case TF_DEFAULT:
                    qglTexParameterf(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_MIN_FILTER, globalImages.textureMinFilter);
                    qglTexParameterf(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_MAG_FILTER, globalImages.textureMaxFilter);
                    break;
                case TF_LINEAR:
                    qglTexParameterf(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    qglTexParameterf(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    break;
                case TF_NEAREST:
                    qglTexParameterf(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                    qglTexParameterf(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                    break;
                default:
                    common.FatalError("R_CreateImage: bad texture filter");
            }

            // upload the base level
            // FIXME: support GL_COLOR_INDEX8_EXT?
            for (i = 0; i < 6; i++) {
                pics[i].rewind();
                qglTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X/*_EXT*/ + i, 0, internalFormat, scaled_width, scaled_height, 0,
                        GL_RGBA, GL_UNSIGNED_BYTE, pics[i]);
            }

            // create and upload the mip map levels
            int miplevel;
            final ByteBuffer[] shrunk = new ByteBuffer[6];

            for (i = 0; i < 6; i++) {
                shrunk[i] = R_MipMap(pics[i], scaled_width, scaled_height, false);
            }

            miplevel = 1;
            while (scaled_width > 1) {
                for (i = 0; i < 6; i++) {
                    ByteBuffer shrunken;

                    qglTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X/*_EXT*/ + i, miplevel, internalFormat,
                            scaled_width / 2, scaled_height / 2, 0,
                            GL_RGBA, GL_UNSIGNED_BYTE, shrunk[i]);

                    if (scaled_width > 2) {
                        shrunken = R_MipMap(shrunk[i], scaled_width / 2, scaled_height / 2, false);
                        // R_StaticFree(shrunk[i]);
                        shrunk[i] = shrunken;
                    } else {
                        shrunk[i].clear();// R_StaticFree(shrunk[i]);
//                        shrunken = null;
                    }

                }

                scaled_width >>= 1;
                scaled_height >>= 1;
                miplevel++;
            }

            // see if we messed anything up
            GL_CheckErrors();
        }

        public void CopyFramebuffer(int x, int y, int[] imageWidth, int[] imageHeight, boolean useOversizedBuffer) {
            Bind();

            if (cvarSystem.GetCVarBool("g_lowresFullscreenFX")) {
                imageWidth[0] = 512;
                imageHeight[0] = 512;
            }

            // if the size isn't a power of 2, the image must be increased in size
            int[] potWidth = {0}, potHeight = {0};

            potWidth[0] = MakePowerOfTwo(imageWidth[0]);
            potHeight[0] = MakePowerOfTwo(imageHeight[0]);

            GetDownsize(imageWidth, imageHeight);
            GetDownsize(potWidth, potHeight);

            qglReadBuffer(GL_BACK);

            // only resize if the current dimensions can't hold it at all,
            // otherwise subview renderings could thrash this
            if ((useOversizedBuffer && (uploadWidth < potWidth[0] || uploadHeight < potHeight[0]))
                    || (!useOversizedBuffer && (uploadWidth != potWidth[0] || uploadHeight != potHeight[0]))) {
                uploadWidth = potWidth[0];
                uploadHeight = potHeight[0];
                if (potWidth[0] == imageWidth[0] && potHeight[0] == imageHeight[0]) {
                    qglCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, x, y, imageWidth[0], imageHeight[0], 0);
                } else {
                    ByteBuffer junk;
                    // we need to create a dummy image with power of two dimensions,
                    // then do a qglCopyTexSubImage2D of the data we want
                    // this might be a 16+ meg allocation, which could fail on _alloca
                    junk = BufferUtils.createByteBuffer(potWidth[0] * potHeight[0] * 4);// Mem_Alloc(potWidth[0] * potHeight[0] * 4);
//			memset( junk, 0, potWidth * potHeight * 4 );		//!@#
//                    if (false) { // Disabling because it's unnecessary and introduces a green strip on edge of _currentRender
//			for ( int i = 0 ; i < potWidth * potHeight * 4 ; i+=4 ) {
//				junk[i+1] = 255;
//			}
//                    }
                    qglTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, potWidth[0], potHeight[0], 0, GL_RGBA, GL_UNSIGNED_BYTE, junk);
//                    Mem_Free(junk);
                    junk = null;

                    qglCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, x, y, imageWidth[0], imageHeight[0]);
                }
            } else {
                // otherwise, just subimage upload it so that drivers can tell we are going to be changing
                // it and don't try and do a texture compression or some other silliness
                qglCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, x, y, imageWidth[0], imageHeight[0]);
            }

            // if the image isn't a full power of two, duplicate an extra row and/or column to fix bilerps
            if (imageWidth[0] != potWidth[0]) {
                qglCopyTexSubImage2D(GL_TEXTURE_2D, 0, imageWidth[0], 0, x + imageWidth[0] - 1, y, 1, imageHeight[0]);
            }
            if (imageHeight[0] != potHeight[0]) {
                qglCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, imageHeight[0], x, y + imageHeight[0] - 1, imageWidth[0], 1);
            }

            qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            backEnd.c_copyFrameBuffer++;
        }


        /*
         ====================
         CopyDepthbuffer

         This should just be part of copyFramebuffer once we have a proper image type field
         ====================
         */
        public void CopyDepthbuffer(int x, int y, int imageWidth, int imageHeight) {
            Bind();

            // if the size isn't a power of 2, the image must be increased in size
            int potWidth, potHeight;

            potWidth = MakePowerOfTwo(imageWidth);
            potHeight = MakePowerOfTwo(imageHeight);

            if (uploadWidth != potWidth || uploadHeight != potHeight) {
                uploadWidth = potWidth;
                uploadHeight = potHeight;
                if (potWidth == imageWidth && potHeight == imageHeight) {
                    qglCopyTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, x, y, imageWidth, imageHeight, 0);
                } else {
                    // we need to create a dummy image with power of two dimensions,
                    // then do a qglCopyTexSubImage2D of the data we want
                    qglTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, potWidth, potHeight, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, (byte[]) null);
                    qglCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, x, y, imageWidth, imageHeight);
                }
            } else {
                // otherwise, just subimage upload it so that drivers can tell we are going to be changing
                // it and don't try and do a texture compression or some other silliness
                qglCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, x, y, imageWidth, imageHeight);
            }

//	qglTexParameterf( GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST );
//	qglTexParameterf( GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST );
            qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
//

        /*
         =============
         RB_UploadScratchImage

         if rows = cols * 6, assume it is a cube map animation
         =============
         */
        public void UploadScratch(final ByteBuffer pic, int cols, int rows) {
            int i;
            final int pos = pic.position();

            // if rows = cols * 6, assume it is a cube map animation
            if (rows == cols * 6) {
                if (type != TT_CUBIC) {
                    type = TT_CUBIC;
                    uploadWidth = -1;	// for a non-sub upload
                }

                Bind();

                rows /= 6;
                // if the scratchImage isn't in the format we want, specify it as a new texture
                if (cols != uploadWidth || rows != uploadHeight) {
                    uploadWidth = cols;
                    uploadHeight = rows;

                    // upload the base level
                    for (i = 0; i < 6; i++) {
                        final int offset = cols * rows * 4 * i;
                        qglTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X/*_EXT*/ + i, 0, GL_RGB8, cols, rows, 0,
                                GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) pic.position(pos + offset));
                    }
                } else {
                    // otherwise, just subimage upload it so that drivers can tell we are going to be changing
                    // it and don't try and do a texture compression
                    for (i = 0; i < 6; i++) {
                        final int offset = cols * rows * 4 * i;
                        qglTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X/*_EXT*/ + i, 0, 0, 0, cols, rows,
                                GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) pic.position(pos + offset));
                    }
                }
                pic.position(pos);//reset position.
                qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                // no other clamp mode makes sense
                qglTexParameteri(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                qglTexParameteri(GL_TEXTURE_CUBE_MAP/*_EXT*/, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            } else {
                // otherwise, it is a 2D image
                if (type != TT_2D) {
                    type = TT_2D;
                    uploadWidth = -1;	// for a non-sub upload
                }

                Bind();

                // if the scratchImage isn't in the format we want, specify it as a new texture
                if (cols != uploadWidth || rows != uploadHeight) {
                    uploadWidth = cols;
                    uploadHeight = rows;
                    qglTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, cols, rows, 0, GL_RGBA, GL_UNSIGNED_BYTE, pic);
                } else {
                    // otherwise, just subimage upload it so that drivers can tell we are going to be changing
                    // it and don't try and do a texture compression
                    qglTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, cols, rows, GL_RGBA, GL_UNSIGNED_BYTE, pic);
                }
                qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                // these probably should be clamp, but we have a lot of issues with editor
                // geometry coming out with texcoords slightly off one side, resulting in
                // a smear across the entire polygon
                if (true) {
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
                } else {
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                }
            }
        }

        // just for resource tracking
        public void SetClassification(int tag) {
            classification = tag;
        }
//

        // estimates size of the GL image based on dimensions and storage type
        public int StorageSize() {
            int baseSize;

            if (texNum == TEXTURE_NOT_LOADED) {
                return 0;
            }

            switch (type) {
                default:
                case TT_2D:
                    baseSize = uploadWidth * uploadHeight;
                    break;
                case TT_3D:
                    baseSize = uploadWidth * uploadHeight * uploadDepth;
                    break;
                case TT_CUBIC:
                    baseSize = 6 * uploadWidth * uploadHeight;
                    break;
            }

            baseSize *= BitsForInternalFormat(internalFormat);

            baseSize /= 8;

            // account for mip mapping
            baseSize = baseSize * 4 / 3;

            return baseSize;
        }
//

        // print a one line summary of the image
        public void Print() {
            if (precompressedFile) {
                common.Printf("P");
            } else if (generatorFunction != null) {
                common.Printf("F");
            } else {
                common.Printf(" ");
            }

            switch (type) {
                case TT_2D:
                    common.Printf(" ");
                    break;
                case TT_3D:
                    common.Printf("3");
                    break;
                case TT_CUBIC:
                    common.Printf("C");
                    break;
                case TT_RECT:
                    common.Printf("R");
                    break;
                default:
                    common.Printf("<BAD TYPE:%d>", type);
                    break;
            }

            common.Printf("%4d %4d ", uploadWidth, uploadHeight);

            switch (filter) {
                case TF_DEFAULT:
                    common.Printf("dflt ");
                    break;
                case TF_LINEAR:
                    common.Printf("linr ");
                    break;
                case TF_NEAREST:
                    common.Printf("nrst ");
                    break;
                default:
                    common.Printf("<BAD FILTER:%d>", filter);
                    break;
            }

            switch (internalFormat) {
                case GL_INTENSITY8:
                case 1:
                    common.Printf("I     ");
                    break;
                case 2:
                case GL_LUMINANCE8_ALPHA8:
                    common.Printf("LA    ");
                    break;
                case 3:
                    common.Printf("RGB   ");
                    break;
                case 4:
                    common.Printf("RGBA  ");
                    break;
                case GL_LUMINANCE8:
                    common.Printf("L     ");
                    break;
                case GL_ALPHA8:
                    common.Printf("A     ");
                    break;
                case GL_RGBA8:
                    common.Printf("RGBA8 ");
                    break;
                case GL_RGB8:
                    common.Printf("RGB8  ");
                    break;
                case GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
                    common.Printf("DXT1  ");
                    break;
                case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
                    common.Printf("DXT1A ");
                    break;
                case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
                    common.Printf("DXT3  ");
                    break;
                case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
                    common.Printf("DXT5  ");
                    break;
                case GL_RGBA4:
                    common.Printf("RGBA4 ");
                    break;
                case GL_RGB5:
                    common.Printf("RGB5  ");
                    break;
                case GL_COLOR_INDEX8_EXT:
                    common.Printf("CI8   ");
                    break;
                case GL_COLOR_INDEX:
                    common.Printf("CI    ");
                    break;
                case GL_COMPRESSED_RGB_ARB:
                    common.Printf("RGBC  ");
                    break;
                case GL_COMPRESSED_RGBA_ARB:
                    common.Printf("RGBAC ");
                    break;
                case 0:
                    common.Printf("      ");
                    break;
                default:
                    common.Printf("<BAD FORMAT:%d>", internalFormat);
                    break;
            }

            switch (repeat) {
                case TR_REPEAT:
                    common.Printf("rept ");
                    break;
                case TR_CLAMP_TO_ZERO:
                    common.Printf("zero ");
                    break;
                case TR_CLAMP_TO_ZERO_ALPHA:
                    common.Printf("azro ");
                    break;
                case TR_CLAMP:
                    common.Printf("clmp ");
                    break;
                default:
                    common.Printf("<BAD REPEAT:%d>", repeat);
                    break;
            }

            common.Printf("%4dk ", StorageSize() / 1024);

            common.Printf(" %s\n", imgName.toString());
        }

//
        // check for changed timestamp on disk and reload if necessary
        public void Reload(boolean checkPrecompressed, boolean force) {
            // always regenerate functional images
            if (generatorFunction != null) {
                common.DPrintf("regenerating %s.\n", imgName);
                generatorFunction.run(this);
                return;
            }

            // check file times
            if (!force) {
                long[]/*ID_TIME_T*/ current = {0};

                if (cubeFiles != CF_2D) {
                    R_LoadCubeImages(imgName.toString(), cubeFiles, null, null, current);
                } else {
                    // get the current values
                    R_LoadImageProgram(imgName.toString(), null, null, current);
                }
                if (current[0] <= timestamp[0]) {
                    return;
                }
            }

            common.DPrintf("reloading %s.\n", imgName.toString());

            PurgeImage();

            // force no precompressed image check, which will cause it to be reloaded
            // from source, and another precompressed file generated.
            // Load is from the front end, so the back end must be synced
            ActuallyLoadImage(checkPrecompressed, false);
        }

        public void AddReference() {
            refCount++;
        }
//
////==========================================================
//

        /*
         ================
         idImage::Downsize
         helper function that takes the current width/height and might make them smaller
         ================
         */
        public void GetDownsize(int[] scaled_width, int[] scaled_height) {
            int size = 0;

            // perform optional picmip operation to save texture memory
            if (depth == TD_SPECULAR && globalImages.image_downSizeSpecular.GetInteger() != 0) {
                size = globalImages.image_downSizeSpecularLimit.GetInteger();
                if (size == 0) {
                    size = 64;
                }
            } else if (depth == TD_BUMP && globalImages.image_downSizeBump.GetInteger() != 0) {
                size = globalImages.image_downSizeBumpLimit.GetInteger();
                if (size == 0) {
                    size = 64;
                }
            } else if ((allowDownSize || globalImages.image_forceDownSize.GetBool()) && globalImages.image_downSize.GetInteger() != 0) {
                size = globalImages.image_downSizeLimit.GetInteger();
                if (size == 0) {
                    size = 256;
                }
            }

            if (size > 0) {
                while (scaled_width[0] > size || scaled_height[0] > size) {
                    if (scaled_width[0] > 1) {
                        scaled_width[0] >>= 1;
                    }
                    if (scaled_height[0] > 1) {
                        scaled_height[0] >>= 1;
                    }
                }
            }

            // clamp to minimum size
            if (scaled_width[0] < 1) {
                scaled_width[0] = 1;
            }
            if (scaled_height[0] < 1) {
                scaled_height[0] = 1;
            }

            // clamp size to the hardware specific upper limit
            // scale both axis down equally so we don't have to
            // deal with a half mip resampling
            // This causes a 512*256 texture to sample down to
            // 256*128 on a voodoo3, even though it could be 256*256
            while (scaled_width[0] > glConfig.maxTextureSize
                    || scaled_height[0] > glConfig.maxTextureSize) {
                scaled_width[0] >>= 1;
                scaled_height[0] >>= 1;
            }
        }
        /*
         ==================
         R_CreateDefaultImage

         the default image will be grey with a white box outline
         to allow you to see the mapping coordinates on a surface
         ==================
         */
        public static final int DEFAULT_SIZE = 16;

        public void MakeDefault() {	// fill with a grid pattern
            int x, y;
            byte[][][] data = new byte[DEFAULT_SIZE][DEFAULT_SIZE][4];

            if (com_developer.GetBool()) {
                // grey center
                for (y = 0; y < DEFAULT_SIZE; y++) {
                    for (x = 0; x < DEFAULT_SIZE; x++) {
                        data[y][x][0] = 32;
                        data[y][x][1] = 32;
                        data[y][x][2] = 32;
                        data[y][x][3] = (byte) 255;
                    }
                }

                // white border
                for (x = 0; x < DEFAULT_SIZE; x++) {
                    data[0][x][0]
                            = data[0][x][1]
                            = data[0][x][2]
                            = data[0][x][3] = (byte) 255;

                    data[x][0][0]
                            = data[x][0][1]
                            = data[x][0][2]
                            = data[x][0][3] = (byte) 255;

                    data[DEFAULT_SIZE - 1][x][0]
                            = data[DEFAULT_SIZE - 1][x][1]
                            = data[DEFAULT_SIZE - 1][x][2]
                            = data[DEFAULT_SIZE - 1][x][3] = (byte) 255;

                    data[x][DEFAULT_SIZE - 1][0]
                            = data[x][DEFAULT_SIZE - 1][1]
                            = data[x][DEFAULT_SIZE - 1][2]
                            = data[x][DEFAULT_SIZE - 1][3] = (byte) 255;
                }
            } else {
                for (y = 0; y < DEFAULT_SIZE; y++) {
                    for (x = 0; x < DEFAULT_SIZE; x++) {
                        data[y][x][0] = 0;
                        data[y][x][1] = 0;
                        data[y][x][2] = 0;
                        data[y][x][3] = 0;
                    }
                }
            }

            GenerateImage(ByteBuffer.wrap(flatten(data)),
                    DEFAULT_SIZE, DEFAULT_SIZE,
                    TF_DEFAULT, true, TR_REPEAT, TD_DEFAULT);

            defaulted = true;
        }

        public void SetImageFilterAndRepeat() {
            // set the minimize / maximize filtering
            switch (filter) {
                case TF_DEFAULT:
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, globalImages.textureMinFilter);
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, globalImages.textureMaxFilter);
                    break;
                case TF_LINEAR:
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    break;
                case TF_NEAREST:
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                    break;
                default:
                    common.FatalError("R_CreateImage: bad texture filter");
            }

            if (glConfig.anisotropicAvailable) {
                // only do aniso filtering on mip mapped images
                if (filter == TF_DEFAULT) {
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, globalImages.textureAnisotropy);
                } else {
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, 1);
                }
            }
            if (glConfig.textureLODBiasAvailable) {
                qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS_EXT, globalImages.textureLODBias);
            }

            // set the wrap/clamp modes
            switch (repeat) {
                case TR_REPEAT:
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
                    break;
                case TR_CLAMP_TO_BORDER:
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
                    break;
                case TR_CLAMP_TO_ZERO:
                case TR_CLAMP_TO_ZERO_ALPHA:
                case TR_CLAMP:
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                    break;
                default:
                    common.FatalError("R_CreateImage: bad texture repeat");
            }
        }

        /*
         ================
         ShouldImageBePartialCached

         Returns true if there is a precompressed image, and it is large enough
         to be worth caching
         ================
         */
        public boolean ShouldImageBePartialCached() {
            if (!glConfig.textureCompressionAvailable) {
                return false;
            }

            if (!globalImages.image_useCache.GetBool()) {
                return false;
            }

            // the allowDownSize flag does double-duty as don't-partial-load
            if (!allowDownSize) {
                return false;
            }

            if (globalImages.image_cacheMinK.GetInteger() <= 0) {
                return false;
            }

            // if we are doing a copyFiles, make sure the original images are referenced
            if (fileSystem.PerformingCopyFiles()) {
                return false;
            }

            final String[] filename = {null};
            final String filename1;
            ImageProgramStringToCompressedFileName(imgName.toString(), filename);
            filename1 = filename[0];

            // get the file timestamp
            fileSystem.ReadFile(filename1, null, timestamp);

            if (timestamp[0] == FILE_NOT_FOUND_TIMESTAMP) {
                return false;
            }

            // open it and get the file size
            idFile f;

            f = fileSystem.OpenFileRead(filename1);
            if (null == f) {
                return false;
            }

            int len = f.Length();
            fileSystem.CloseFile(f);

            if (len <= globalImages.image_cacheMinK.GetInteger() * 1024) {
                return false;
            }

            // we do want to do a partial load
            return true;
        }

        /*
         ================
         WritePrecompressedImage

         When we are happy with our source data, we can write out precompressed
         versions of everything to speed future load times.
         ================
         */
        public void WritePrecompressedImage() {

            // Always write the precompressed image if we're making a build
            if (!com_makingBuild.GetBool()) {
                if (!globalImages.image_writePrecompressedTextures.GetBool() || !globalImages.image_usePrecompressedTextures.GetBool()) {
                    return;
                }
            }

            if (!glConfig.isInitialized) {
                return;
            }

            final String[] filename0 = {null};
            ImageProgramStringToCompressedFileName(imgName.toString(), filename0);
            final String filename = filename0[0];

            int numLevels = NumLevelsForImageSize(uploadWidth, uploadHeight);
            if (numLevels > MAX_TEXTURE_LEVELS) {
                common.Warning("R_WritePrecompressedImage: level > MAX_TEXTURE_LEVELS for image %s", filename);
                return;
            }

            // glGetTexImage only supports a small subset of all the available internal formats
            // We have to use BGRA because DDS is a windows based format
            int altInternalFormat = 0;
            int bitSize = 0;
            switch (internalFormat) {
                case GL_COLOR_INDEX8_EXT:
                case GL_COLOR_INDEX:
                    // this will not work with dds viewers but we need it in this format to save disk
                    // load speed ( i.e. size ) 
                    altInternalFormat = GL_COLOR_INDEX;
                    bitSize = 24;
                    break;
                case 1:
                case GL_INTENSITY8:
                case GL_LUMINANCE8:
                case 3:
                case GL_RGB8:
                    altInternalFormat = GL_BGR_EXT;
                    bitSize = 24;
                    break;
                case GL_LUMINANCE8_ALPHA8:
                case 4:
                case GL_RGBA8:
                    altInternalFormat = GL_BGRA_EXT;
                    bitSize = 32;
                    break;
                case GL_ALPHA8:
                    altInternalFormat = GL_ALPHA;
                    bitSize = 8;
                    break;
                default:
                    if (FormatIsDXT(internalFormat)) {
                        altInternalFormat = internalFormat;
                    } else {
                        common.Warning("Unknown or unsupported format for %s", filename);
                        return;
                    }
            }

            if (globalImages.image_useOffLineCompression.GetBool() && FormatIsDXT(altInternalFormat)) {
                String outFile = fileSystem.RelativePathToOSPath(filename, "fs_basepath");
                idStr inFile = new idStr(outFile);
                inFile.StripFileExtension();
                inFile.SetFileExtension("tga");
                String format = null;
                if (depth == TD_BUMP) {
                    format = "RXGB +red 0.0 +green 0.5 +blue 0.5";
                } else {
                    switch (altInternalFormat) {
                        case GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
                            format = "DXT1";
                            break;
                        case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
                            format = "DXT1 -alpha_threshold";
                            break;
                        case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
                            format = "DXT3";
                            break;
                        case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
                            format = "DXT5";
                            break;
                    }
                }
                globalImages.AddDDSCommand(va("z:/d3xp/compressonator/thecompressonator -convert \"%s\" \"%s\" %s -mipmaps\n", inFile.toString(), outFile, format));
                return;
            }

            ddsFileHeader_t header;
//	memset( &header, 0, sizeof(header) );
            header = new ddsFileHeader_t();
//            header.dwSize = sizeof(header);
            header.dwFlags = DDSF_CAPS | DDSF_PIXELFORMAT | DDSF_WIDTH | DDSF_HEIGHT;
            header.dwHeight = uploadHeight;
            header.dwWidth = uploadWidth;

            // hack in our monochrome flag for the NV20 optimization
            if (isMonochrome[0]) {
                header.dwFlags |= DDSF_ID_MONOCHROME;
            }

            if (FormatIsDXT(altInternalFormat)) {
                // size (in bytes) of the compressed base image
                header.dwFlags |= DDSF_LINEARSIZE;
                header.dwPitchOrLinearSize = ((uploadWidth + 3) / 4) * ((uploadHeight + 3) / 4)
                        * (altInternalFormat <= GL_COMPRESSED_RGBA_S3TC_DXT1_EXT ? 8 : 16);
            } else {
                // 4 Byte aligned line width (from nv_dds)
                header.dwFlags |= DDSF_PITCH;
                header.dwPitchOrLinearSize = ((uploadWidth * bitSize + 31) & -32) >> 3;
            }

            header.dwCaps1 = DDSF_TEXTURE;

            if (numLevels > 1) {
                header.dwMipMapCount = numLevels;
                header.dwFlags |= DDSF_MIPMAPCOUNT;
                header.dwCaps1 |= DDSF_MIPMAP | DDSF_COMPLEX;
            }

//            header.ddspf.dwSize = sizeof(header.ddspf);
            if (FormatIsDXT(altInternalFormat)) {
                header.ddspf.dwFlags = DDSF_FOURCC;
                switch (altInternalFormat) {
                    case GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
                        header.ddspf.dwFourCC = DDS_MAKEFOURCC('D', 'X', 'T', '1');
                        break;
                    case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
                        header.ddspf.dwFlags |= DDSF_ALPHAPIXELS;
                        header.ddspf.dwFourCC = DDS_MAKEFOURCC('D', 'X', 'T', '1');
                        break;
                    case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
                        header.ddspf.dwFourCC = DDS_MAKEFOURCC('D', 'X', 'T', '3');
                        break;
                    case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
                        header.ddspf.dwFourCC = DDS_MAKEFOURCC('D', 'X', 'T', '5');
                        break;
                }
            } else {
                header.ddspf.dwFlags = (internalFormat == GL_COLOR_INDEX8_EXT) ? DDSF_RGB | DDSF_ID_INDEXCOLOR : DDSF_RGB;
                header.ddspf.dwRGBBitCount = bitSize;
                switch (altInternalFormat) {
                    case GL_BGRA_EXT:
                    case GL_LUMINANCE_ALPHA:
                        header.ddspf.dwFlags |= DDSF_ALPHAPIXELS;
                        header.ddspf.dwABitMask = 0xFF000000;
                    // Fall through
                    case GL_BGR_EXT:
                    case GL_LUMINANCE:
                    case GL_COLOR_INDEX:
                        header.ddspf.dwRBitMask = 0x00FF0000;
                        header.ddspf.dwGBitMask = 0x0000FF00;
                        header.ddspf.dwBBitMask = 0x000000FF;
                        break;
                    case GL_ALPHA:
                        header.ddspf.dwFlags = DDSF_ALPHAPIXELS;
                        header.ddspf.dwABitMask = 0xFF000000;
                        break;
                    default:
                        common.Warning("Unknown or unsupported format for %s", filename);
                        return;
                }
            }

            idFile f = fileSystem.OpenFileWrite(filename);
            if (f == null) {
                common.Warning("Could not open %s trying to write precompressed image", filename);
                return;
            }
            common.Printf("Writing precompressed image: %s\n", filename);

            f.WriteString("DDS ");//, 4);
            f.Write(header.Write()/*, sizeof(header) */);

            // bind to the image so we can read back the contents
            Bind();

            qglPixelStorei(GL_PACK_ALIGNMENT, 1);	// otherwise small rows get padded to 32 bits

            int uw = uploadWidth;
            int uh = uploadHeight;

            // Will be allocated first time through the loop
            ByteBuffer data = null;

            for (int level = 0; level < numLevels; level++) {

                int size = 0;
                if (FormatIsDXT(altInternalFormat)) {
                    size = ((uw + 3) / 4) * ((uh + 3) / 4)
                            * (altInternalFormat <= GL_COMPRESSED_RGBA_S3TC_DXT1_EXT ? 8 : 16);
                } else {
                    size = uw * uh * (bitSize / 8);
                }

                if (data == null) {
                    data = ByteBuffer.allocate(size);// R_StaticAlloc(size);
                }

                if (FormatIsDXT(altInternalFormat)) {
                    qglGetCompressedTexImageARB(GL_TEXTURE_2D, level, data);
                } else {
                    qglGetTexImage(GL_TEXTURE_2D, level, altInternalFormat, GL_UNSIGNED_BYTE, data);
                }

                f.Write(data, size);

                uw /= 2;
                uh /= 2;
                if (uw < 1) {
                    uw = 1;
                }
                if (uh < 1) {
                    uh = 1;
                }
            }

//            if (data != null) {
//                R_StaticFree(data);
//            }
//            
            fileSystem.CloseFile(f);
        }

        /*
         ================
         CheckPrecompressedImage

         If fullLoad is false, only the small mip levels of the image will be loaded
         ================
         */        static int DEBUG_CheckPrecompressedImage = 0;

        public boolean CheckPrecompressedImage(boolean fullLoad) {
            if (!glConfig.isInitialized || !glConfig.textureCompressionAvailable) {
                return false;
            }

            if (true) { // ( _D3XP had disabled ) - Allow grabbing of DDS's from original Doom pak files
                // if we are doing a copyFiles, make sure the original images are referenced
                if (fileSystem.PerformingCopyFiles()) {
                    return false;
                }
            }

            if (depth == TD_BUMP && globalImages.image_useNormalCompression.GetInteger() != 2) {
                return false;
            }

            // god i love last minute hacks :-)
            // me too.
            if (com_machineSpec.GetInteger() >= 1 && com_videoRam.GetInteger() >= 128 && imgName.Icmpn("lights/", 7) == 0) {
                return false;//TODO:enable this by using openCL for the values above.
            }

            if (imgName.toString().contains("mars")
                    || imgName.toString().contains("planet")) {
//                System.out.println(">>>>>>>>>>>" + DEBUG_CheckPrecompressedImage);
//                return true;
            }

            DEBUG_CheckPrecompressedImage++;

            final String[] filename = {null};
            ImageProgramStringToCompressedFileName(imgName.toString(), filename);
//            System.out.println("====" + filename[0]);

            // get the file timestamp
            final /*ID_TIME_T */ long[] precompTimestamp = {0};
            fileSystem.ReadFile(filename[0], null, precompTimestamp);

            if (precompTimestamp[0] == FILE_NOT_FOUND_TIMESTAMP) {
                return false;
            }

            if (null == generatorFunction && timestamp[0] != FILE_NOT_FOUND_TIMESTAMP) {
                if (precompTimestamp[0] < timestamp[0]) {
                    // The image has changed after being precompressed
                    return false;
                }
            }

            timestamp[0] = precompTimestamp[0];

            // open it and just read the header
            idFile f;

            f = fileSystem.OpenFileRead(filename[0]);
            if (null == f) {
                return false;
            }

            int len = f.Length();
            if (len < ddsFileHeader_t.BYTES) {
                fileSystem.CloseFile(f);
                return false;
            }

            if (!fullLoad && len > globalImages.image_cacheMinK.GetInteger() * 1024) {
                len = globalImages.image_cacheMinK.GetInteger() * 1024;
            }

            ByteBuffer data = ByteBuffer.allocate(len);// R_StaticAlloc(len);

            f.Read(data);

            fileSystem.CloseFile(f);

            data.order(ByteOrder.LITTLE_ENDIAN);
            long magic = LittleLong(data.getInt());
            data.position(4);//, 4);
            ddsFileHeader_t _header = new ddsFileHeader_t(data);
            int ddspf_dwFlags = LittleLong(_header.ddspf.dwFlags);

            if (magic != DDS_MAKEFOURCC('D', 'D', 'S', ' ')) {
                common.Printf("CheckPrecompressedImage( %s ): magic != 'DDS '\n", imgName.toString());
//                R_StaticFree(data);
                return false;
            }

            // if we don't support color index textures, we must load the full image
            // should we just expand the 256 color image to 32 bit for upload?
            if (((ddspf_dwFlags & DDSF_ID_INDEXCOLOR) != 0) && !glConfig.sharedTexturePaletteAvailable) {
//                R_StaticFree(daDta);
                return false;
            }

            // upload all the levels
            UploadPrecompressedImage(data, len);//TODO:disables all pictures, also makes shit blocky.

//            R_StaticFree(data);
            return true;
        }

        /*
         ===================
         UploadPrecompressedImage

         This can be called by the front end during normal loading,
         or by the backend after a background read of the file
         has completed
         ===================
         */private static ByteBuffer DBG_UploadPrecompressedImage;
        public void UploadPrecompressedImage(ByteBuffer data, int len) {
            data.position(4);//, 4)
            ddsFileHeader_t header = new ddsFileHeader_t(data);

            // ( not byte swapping dwReserved1 dwReserved2 )
            header.dwSize = LittleLong(header.dwSize);
            header.dwFlags = LittleLong(header.dwFlags);
            header.dwHeight = LittleLong(header.dwHeight);
            header.dwWidth = LittleLong(header.dwWidth);
            header.dwPitchOrLinearSize = LittleLong(header.dwPitchOrLinearSize);
            header.dwDepth = LittleLong(header.dwDepth);
            header.dwMipMapCount = LittleLong(header.dwMipMapCount);
            header.dwCaps1 = LittleLong(header.dwCaps1);
            header.dwCaps2 = LittleLong(header.dwCaps2);

            header.ddspf.dwSize = LittleLong(header.ddspf.dwSize);
            header.ddspf.dwFlags = LittleLong(header.ddspf.dwFlags);
            header.ddspf.dwFourCC = LittleLong(header.ddspf.dwFourCC);
            header.ddspf.dwRGBBitCount = LittleLong(header.ddspf.dwRGBBitCount);
            header.ddspf.dwRBitMask = LittleLong(header.ddspf.dwRBitMask);
            header.ddspf.dwGBitMask = LittleLong(header.ddspf.dwGBitMask);
            header.ddspf.dwBBitMask = LittleLong(header.ddspf.dwBBitMask);
            header.ddspf.dwABitMask = LittleLong(header.ddspf.dwABitMask);

            // generate the texture number
            texNum = qglGenTextures();
            System.out.println(imgName + ": " + texNum);

//            if (texNum == 58) {
//                DBG_UploadPrecompressedImage = data.duplicate();
//                DBG_UploadPrecompressedImage.order(data.order());
//            } else 
//            if (texNum == 59) {
//                texNum = null;
//                final int pos = data.position();
//                data = DBG_UploadPrecompressedImage.duplicate();
//                data.order(DBG_UploadPrecompressedImage.order());
//                UploadPrecompressedImage(data, len);
//                return;
//            }
            int externalFormat = 0;

            precompressedFile = true;

            uploadWidth = (int) header.dwWidth;
            uploadHeight = (int) header.dwHeight;
            if ((header.ddspf.dwFlags & DDSF_FOURCC) != 0) {
//                System.out.printf("%d\n", header.ddspf.dwFourCC);
//                switch (bla[DEBUG_dwFourCC++]) {
                switch ((int) header.ddspf.dwFourCC) {
                    case DDS_MAKEFOURCC_DXT1:
                        if ((header.ddspf.dwFlags & DDSF_ALPHAPIXELS) != 0) {
                            internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
//                            System.out.printf("GL_COMPRESSED_RGBA_S3TC_DXT1_EXT\n");
                        } else {
                            internalFormat = GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
//                            System.out.printf("GL_COMPRESSED_RGB_S3TC_DXT1_EXT\n");
                        }
                        break;
                    case DDS_MAKEFOURCC_DXT3:
                        internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
//                        System.out.printf("GL_COMPRESSED_RGBA_S3TC_DXT3_EXT\n");
                        break;
                    case DDS_MAKEFOURCC_DXT5:
                        internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
//                        System.out.printf("GL_COMPRESSED_RGBA_S3TC_DXT5_EXT\n");
                        break;
                    case DDS_MAKEFOURCC_RXGB:
                        internalFormat = GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
//                        System.out.printf("GL_COMPRESSED_RGBA_S3TC_DXT5_EXT\n");
                        break;
                    default:
                        common.Warning("Invalid compressed internal format\n");
                        return;
                }
            } else if (((header.ddspf.dwFlags & DDSF_RGBA) != 0) && header.ddspf.dwRGBBitCount == 32) {
                externalFormat = GL_BGRA_EXT;
                internalFormat = GL_RGBA8;
            } else if (((header.ddspf.dwFlags & DDSF_RGB) != 0) && header.ddspf.dwRGBBitCount == 32) {
                externalFormat = GL_BGRA_EXT;
                internalFormat = GL_RGBA8;
            } else if (((header.ddspf.dwFlags & DDSF_RGB) != 0) && header.ddspf.dwRGBBitCount == 24) {
                if ((header.ddspf.dwFlags & DDSF_ID_INDEXCOLOR) != 0) {
                    externalFormat = GL_COLOR_INDEX;
                    internalFormat = GL_COLOR_INDEX8_EXT;
                } else {
                    externalFormat = GL_BGR_EXT;
                    internalFormat = GL_RGB8;
                }
            } else if (header.ddspf.dwRGBBitCount == 8) {
                externalFormat = GL_ALPHA;
                internalFormat = GL_ALPHA8;
            } else {
                common.Warning("Invalid uncompressed internal format\n");
                return;
            }

            // we need the monochrome flag for the NV20 optimized path
            if ((header.dwFlags & DDSF_ID_MONOCHROME) != 0) {
                isMonochrome[0] = true;
            }

            type = TT_2D;// FIXME: we may want to support pre-compressed cube maps in the future

            Bind();

            int numMipmaps = 1;
            if ((header.dwFlags & DDSF_MIPMAPCOUNT) != 0) {
                numMipmaps = (int) header.dwMipMapCount;
            }

            int uw = uploadWidth;
            int uh = uploadHeight;

            // We may skip some mip maps if we are downsizing
            int skipMip = 0;
            int[] uploadWidth2 = {uploadWidth}, uploadHeight2 = {uploadHeight};
            GetDownsize(uploadWidth2, uploadHeight2);
            uploadWidth = uploadWidth2[0];
            uploadHeight = uploadHeight2[0];

            int offset = ddsFileHeader_t.BYTES + 4;// + sizeof(ddsFileHeader_t) + 4;
            for (int i = 0; i < numMipmaps; i++) {
                final int size;
                if (FormatIsDXT(internalFormat)) {
                    size = ((uw + 3) / 4) * ((uh + 3) / 4)
                            * (internalFormat <= GL_COMPRESSED_RGBA_S3TC_DXT1_EXT ? 8 : 16);
                } else {
                    size = (int) (uw * uh * (header.ddspf.dwRGBBitCount / 8));
                }

                if (uw > uploadWidth || uh > uploadHeight) {
                    skipMip++;
                } else {
                    ByteBuffer imageData = BufferUtils.createByteBuffer(size);
                    imageData.put(data.array(), offset, size);
                    imageData.order(ByteOrder.BIG_ENDIAN);//TODO: should ByteOrder be reverted? <data> uses LITTLE_ENDIAN.
                    imageData.flip();//FUCKME: the lwjgl version of <glCompressedTexImage2DARB> uses bytebuffer.remaining() as size.
                    if (FormatIsDXT(internalFormat)) {//TODO: remove blocky crap!
                        qglCompressedTexImage2DARB(GL_TEXTURE_2D, i - skipMip, internalFormat, uw, uh, 0, size, imageData);
//                        System.out.printf("qglCompressedTexImage2DARB(%d)\n", imageData.get(0) & 0xFF);
                    } else {
                        qglTexImage2D(GL_TEXTURE_2D, i - skipMip, internalFormat, uw, uh, 0, externalFormat, GL_UNSIGNED_BYTE, imageData);
                    }
                }

                offset += size;
                uw /= 2;
                uh /= 2;
                if (uw < 1) {
                    uw = 1;
                }
                if (uh < 1) {
                    uh = 1;
                }
            }

            SetImageFilterAndRepeat();
        }

        /*
         ===============
         ActuallyLoadImage

         Absolutely every image goes through this path
         On exit, the idImage will have a valid OpenGL texture number that can be bound
         ===============
         */private static int DBG_ActuallyLoadImage = 0;
        public void ActuallyLoadImage(boolean checkForPrecompressed, boolean fromBackEnd) {
            final int[] width = {0}, height = {0};
            ByteBuffer pic = null;

            if (imgName.equals("guis/assets/splash/launch")) {
//                return;
            }
//            System.out.println((DBG_ActuallyLoadImage++) + " " + imgName);

            // this is the ONLY place generatorFunction will ever be called
            if (generatorFunction != null) {
                generatorFunction.run(this);
                return;
            }

            // if we are a partial image, we are only going to load from a compressed file
            if (isPartialImage) {
                if (CheckPrecompressedImage(false)) {
                    return;
                }
                // this is an error -- the partial image failed to load
                MakeDefault();
                return;
            }

            //
            // load the image from disk
            //
            if (cubeFiles != CF_2D) {
                ByteBuffer[] pics = new ByteBuffer[6];//TODO:FIXME!

                // we don't check for pre-compressed cube images currently
                R_LoadCubeImages(imgName.toString(), cubeFiles, pics, width, timestamp);

                if (pics[0] == null) {
                    common.Warning("Couldn't load cube image: %s", imgName.toString());
                    MakeDefault();
                    return;
                }

                GenerateCubeImage( /*(const byte **)*/pics, width[0], filter, allowDownSize, depth);
                precompressedFile = false;
//
//                for (int i = 0; i < 6; i++) {
//                    if (pics[0][i] != 0) {
//                        R_StaticFree(pics[i]);
//                    }
//                }
            } else {
                // see if we have a pre-generated image file that is
                // already image processed and compressed
                if (checkForPrecompressed && globalImages.image_usePrecompressedTextures.GetBool()) {
                    if (CheckPrecompressedImage(true)) {
                        // we got the precompressed image
                        return;
                    }
                    // fall through to load the normal image
                }

                {
                    textureDepth_t[] depth = {this.depth};
                    pic = R_LoadImageProgram(imgName.toString(), width, height, timestamp, depth);
                    this.depth = depth[0];
                }

                if (pic == null) {
                    common.Warning("Couldn't load image: %s", imgName);
                    MakeDefault();
                    return;
                }
                /*
                 // swap the red and alpha for rxgb support
                 // do this even on tga normal maps so we only have to use
                 // one fragment program
                 // if the image is precompressed ( either in palletized mode or true rxgb mode )
                 // then it is loaded above and the swap never happens here
                 if ( depth == TD_BUMP && globalImages.image_useNormalCompression.GetInteger() != 1 ) {
                 for ( int i = 0; i < width * height * 4; i += 4 ) {
                 pic[ i + 3 ] = pic[ i ];
                 pic[ i ] = 0;
                 }
                 }
                 */
                // build a hash for checking duplicate image files
                // NOTE: takes about 10% of image load times (SD)
                // may not be strictly necessary, but some code uses it, so let's leave it in
                imageHash = MD4_BlockChecksum(pic, width[0] * height[0] * 4);

                GenerateImage(pic, width[0], height[0], filter, allowDownSize, repeat, depth);
                timestamp = timestamp;//why, because we rock!
                precompressedFile = false;

//                R_StaticFree(pic);
                // write out the precompressed version of this file if needed
                WritePrecompressedImage();
            }
        }

        public void StartBackgroundImageLoad() {
            if (imageManager.numActiveBackgroundImageLoads >= idImageManager.MAX_BACKGROUND_IMAGE_LOADS) {
                return;
            }
            if (globalImages.image_showBackgroundLoads.GetBool()) {
                common.Printf("idImage::StartBackgroundImageLoad: %s\n", imgName.toString());
            }
            backgroundLoadInProgress = true;

            if (!precompressedFile) {
                common.Warning("idImageManager::StartBackgroundImageLoad: %s wasn't a precompressed file", imgName.toString());
                return;
            }

            bglNext = globalImages.backgroundImageLoads;
            globalImages.backgroundImageLoads = this;

            String[] filename = {null};
            ImageProgramStringToCompressedFileName(imgName, filename);

            bgl.completed = false;
            bgl.f = fileSystem.OpenFileRead(filename[0]);
            if (null == bgl.f) {
                common.Warning("idImageManager::StartBackgroundImageLoad: Couldn't load %s", imgName.toString());
                return;
            }
            bgl.file.position = 0;
            bgl.file.length = bgl.f.Length();
            if (bgl.file.length < ddsFileHeader_t.BYTES) {
                common.Warning("idImageManager::StartBackgroundImageLoad: %s had a bad file length", imgName.toString());
                return;
            }

            bgl.file.buffer = ByteBuffer.allocate(bgl.file.length);

            fileSystem.BackgroundDownload(bgl);

            imageManager.numActiveBackgroundImageLoads++;

            // purge some images if necessary
            int totalSize = 0;
            for (idImage check = globalImages.cacheLRU.cacheUsageNext; check != globalImages.cacheLRU; check = check.cacheUsageNext) {
                totalSize += check.StorageSize();
            }
            int needed = this.StorageSize();

            while ((totalSize + needed) > globalImages.image_cacheMegs.GetFloat() * 1024 * 1024) {
                // purge the least recently used
                idImage check = globalImages.cacheLRU.cacheUsagePrev;
                if (check.texNum != TEXTURE_NOT_LOADED) {
                    totalSize -= check.StorageSize();
                    if (globalImages.image_showBackgroundLoads.GetBool()) {
                        common.Printf("purging %s\n", check.imgName.toString());
                    }
                    check.PurgeImage();
                }
                // remove it from the cached list
                check.cacheUsageNext.cacheUsagePrev = check.cacheUsagePrev;
                check.cacheUsagePrev.cacheUsageNext = check.cacheUsageNext;
                check.cacheUsageNext = null;
                check.cacheUsagePrev = null;
            }
        }

        /*
         ================
         BitsForInternalFormat

         Used for determining memory utilization
         ================
         */
        public int BitsForInternalFormat(int internalFormat) {
            switch (internalFormat) {
                case GL_INTENSITY8:
                case 1:
                    return 8;
                case 2:
                case GL_LUMINANCE8_ALPHA8:
                    return 16;
                case 3:
                    return 32;		// on some future hardware, this may actually be 24, but be conservative
                case 4:
                    return 32;
                case GL_LUMINANCE8:
                    return 8;
                case GL_ALPHA8:
                    return 8;
                case GL_RGBA8:
                    return 32;
                case GL_RGB8:
                    return 32;		// on some future hardware, this may actually be 24, but be conservative
                case GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
                    return 4;
                case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
                    return 4;
                case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
                    return 8;
                case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
                    return 8;
                case GL_RGBA4:
                    return 16;
                case GL_RGB5:
                    return 16;
                case GL_COLOR_INDEX8_EXT:
                    return 8;
                case GL_COLOR_INDEX:
                    return 8;
                case GL_COMPRESSED_RGB_ARB:
                    return 4;			// not sure
                case GL_COMPRESSED_RGBA_ARB:
                    return 8;			// not sure
                default:
                    common.Error("R_BitsForInternalFormat: BAD FORMAT:%d", internalFormat);
            }
            return 0;
        }

        /*
         ==================
         UploadCompressedNormalMap

         Create a 256 color palette to be used by compressed normal maps
         ==================
         */
        public void UploadCompressedNormalMap(int width, int height, final byte[] rgba, int mipLevel) {
            byte[] normals;
            int in;
            int out;
            int i, j;
            int x, y, z;
            int row;

            // OpenGL's pixel packing rule
            row = width < 4 ? 4 : width;

            normals = new byte[row * height];
            if (NOT(normals)) {
                common.Error("R_UploadCompressedNormalMap: _alloca failed");
            }

            in = 0;
            out = 0;
            for (i = 0; i < height; i++, out += row, in += width * 4) {
                for (j = 0; j < width; j++) {
                    x = rgba[in + j * 4 + 0];
                    y = rgba[in + j * 4 + 1];
                    z = rgba[in + j * 4 + 2];

                    int c;
                    if (x == 128 && y == 128 && z == 128) {
                        // the "nullnormal" color
                        c = 255;
                    } else {
                        c = (globalImages.originalToCompressed[x] << 4) | globalImages.originalToCompressed[y];
                        if (c == 255) {
                            c = 254;	// don't use the nullnormal color
                        }
                    }
                    normals[out + j] = (byte) c;
                }
            }

            if (mipLevel == 0) {
                // Optionally write out the paletized normal map to a .tga
                if (globalImages.image_writeNormalTGAPalletized.GetBool()) {
                    String[] filename = {null};
                    ImageProgramStringToCompressedFileName(imgName, filename);
                    int ext = filename[0].lastIndexOf('.');
                    if (ext != -1) {
//				strcpy(ext, "_pal.tga");
                        System.arraycopy("_pal.tga".toCharArray(), 0, filename, ext, "_pal.tga".length());
                        R_WritePalTGA(filename[0], normals, globalImages.compressedPalette, width, height);
                    }
                }
            }

            if (glConfig.sharedTexturePaletteAvailable) {
                qglTexImage2D(GL_TEXTURE_2D,
                        mipLevel,
                        GL_COLOR_INDEX8_EXT,
                        width,
                        height,
                        0,
                        GL_COLOR_INDEX,
                        GL_UNSIGNED_BYTE,
                        normals);
            }
        }

        public int/*GLenum*/ SelectInternalFormat(final ByteBuffer dataPtrs, int numDataPtrs, int width, int height,
                        textureDepth_t minimumDepth, boolean[] monochromeResult) {
            return SelectInternalFormat(new ByteBuffer[]{dataPtrs}, numDataPtrs, width, height, minimumDepth, monochromeResult);
        }

        /*
         ===============
         SelectInternalFormat

         This may need to scan six cube map images
         ===============
         */
        public int/*GLenum*/ SelectInternalFormat(final ByteBuffer[] dataPtrs, int numDataPtrs, int width, int height,
                        textureDepth_t minimumDepth, boolean[] monochromeResult) {
            int i, c, pos;
            ByteBuffer scan;
            int rgbOr, rgbAnd, aOr, aAnd;
            int rgbDiffer, rgbaDiffer;

            // determine if the rgb channels are all the same
            // and if either all rgb or all alpha are 255
            c = width * height;
            rgbDiffer = 0;
            rgbaDiffer = 0;
            rgbOr = 0;
            rgbAnd = -1;
            aOr = 0;
            aAnd = -1;

            monochromeResult[0] = true;	// until shown otherwise

            for (int side = 0; side < numDataPtrs; side++) {
                scan = dataPtrs[side];
                for (i = 0, pos = 0; i < c; i++, pos += 4) {
                    int cOr, cAnd;

                    aOr |= scan.get(pos + 3);
                    aAnd &= scan.get(pos + 3);

                    cOr = scan.get(pos + 0) | scan.get(pos + 1) | scan.get(pos + 2);
                    cAnd = scan.get(pos + 0) & scan.get(pos + 1) & scan.get(pos + 2);

                    // if rgb are all the same, the or and and will match
                    rgbDiffer |= (cOr ^ cAnd);

                    // our "isMonochrome" test is more lax than rgbDiffer,
                    // allowing the values to be off by several units and
                    // still use the NV20 mono path
                    if (monochromeResult[0]) {
                        if (Math.abs(scan.get(pos + 0) - scan.get(pos + 1)) > 16
                                || Math.abs(scan.get(pos + 0) - scan.get(pos + 2)) > 16) {
                            monochromeResult[0] = false;
                        }
                    }

                    rgbOr |= cOr;
                    rgbAnd &= cAnd;

                    cOr |= scan.get(pos + 3);
                    cAnd &= scan.get(pos + 3);

                    rgbaDiffer |= (cOr ^ cAnd);
                }
            }

            // we assume that all 0 implies that the alpha channel isn't needed,
            // because some tools will spit out 32 bit images with a 0 alpha instead
            // of 255 alpha, but if the alpha actually is referenced, there will be
            // different behavior in the compressed vs uncompressed states.
            final boolean needAlpha;
            if (aAnd == 255 || aOr == 0) {
                needAlpha = false;
            } else {
                needAlpha = true;
            }

            // catch normal maps first
            if (minimumDepth == TD_BUMP) {
                if (globalImages.image_useCompression.GetBool() && globalImages.image_useNormalCompression.GetInteger() == 1 && glConfig.sharedTexturePaletteAvailable) {
                    // image_useNormalCompression should only be set to 1 on nv_10 and nv_20 paths
                    return GL_COLOR_INDEX8_EXT;
                } else if (globalImages.image_useCompression.GetBool() && (globalImages.image_useNormalCompression.GetInteger() != 0) && glConfig.textureCompressionAvailable) {
                    // image_useNormalCompression == 2 uses rxgb format which produces really good quality for medium settings
                    return GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                } else {
                    // we always need the alpha channel for bump maps for swizzling
                    return GL_RGBA8;
                }
            }

            // allow a complete override of image compression with a cvar
            if (!globalImages.image_useCompression.GetBool()) {
                minimumDepth = TD_HIGH_QUALITY;
            }

            if (minimumDepth == TD_SPECULAR) {
                // we are assuming that any alpha channel is unintentional
                if (glConfig.textureCompressionAvailable) {
                    return GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
                } else {
                    return GL_RGB5;
                }
            }
            if (minimumDepth == TD_DIFFUSE) {
                // we might intentionally have an alpha channel for alpha tested textures
                if (glConfig.textureCompressionAvailable) {
                    if (!needAlpha) {
                        return GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
                    } else {
                        return GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                    }
                } else if ((aAnd == 255 || aOr == 0)) {
                    return GL_RGB5;
                } else {
                    return GL_RGBA4;
                }
            }

            // there will probably be some drivers that don't
            // correctly handle the intensity/alpha/luminance/luminance+alpha
            // formats, so provide a fallback that only uses the rgb/rgba formats
            if (!globalImages.image_useAllFormats.GetBool()) {
                // pretend rgb is varying and inconsistant, which
                // prevents any of the more compact forms
                rgbDiffer = 1;
                rgbaDiffer = 1;
                rgbAnd = 0;
            }

            // cases without alpha
            if (!needAlpha) {
                if (minimumDepth == TD_HIGH_QUALITY) {
                    return GL_RGB8;			// four bytes
                }
                if (glConfig.textureCompressionAvailable) {
                    return GL_COMPRESSED_RGB_S3TC_DXT1_EXT;	// half byte
                }
                return GL_RGB5;			// two bytes
            }

            // cases with alpha
            if (NOT(rgbaDiffer)) {
                if (minimumDepth != TD_HIGH_QUALITY && glConfig.textureCompressionAvailable) {
                    return GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;	// one byte
                }
                return GL_INTENSITY8;	// single byte for all channels
            }

            if (false) {
                // we don't support alpha textures any more, because there
                // is a discrepancy in the definition of TEX_ENV_COMBINE that
                // causes them to be treated as 0 0 0 A, instead of 1 1 1 A as
                // normal texture modulation treats them
                if (rgbAnd == 255) {
                    return GL_ALPHA8;		// single byte, only alpha
                }
            }

            if (minimumDepth == TD_HIGH_QUALITY) {
                return GL_RGBA8;	// four bytes
            }
            if (glConfig.textureCompressionAvailable) {
                return GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;	// one byte
            }
            if (NOT(rgbDiffer)) {
                return GL_LUMINANCE8_ALPHA8;	// two bytes, max quality
            }
            return GL_RGBA4;	// two bytes
        }

        public void ImageProgramStringToCompressedFileName(final String imageProg, String[] fileName) {
//            char s;
            int f;
            int i;
            final char[] ff = new char[imageProg.length() + 10];

//	strcpy( fileName, "dds/" );
            fileName[0] = "dds/";
            f = fileName[0].length();
//            ff = fileName[0].toCharArray();
            System.arraycopy(fileName[0].toCharArray(), 0, ff, 0, f);

            int depth = 0;

            // convert all illegal characters to underscores
            // this could conceivably produce a duplicated mapping, but we aren't going to worry about it
            for (i = 0; i < imageProg.length(); i++) {
                final char s = imageProg.charAt(i);

                if (s == '/' || s == '\\' || s == '(') {
                    if (depth < 4) {
                        ff[f] = '/';
                        depth++;
                    } else {
                        ff[f] = ' ';
                    }
                    f++;
                } else if (s == '<' || s == '>' || s == ':' || s == '|' || s == '"' || s == '.') {
                    ff[f] = '_';
                    f++;
                } else if (s == ' ' && ff[f - 1] == '/') {	// ignore a space right after a slash
                } else if (s == ')' || s == ',') {		// always ignore these
                } else {
                    ff[f] = s;
                    f++;
                }
            }
            ff[f++] = 0;
//	strcat( fileName, ".dds" );
            fileName[0] = ctos(ff) + ".dds";
        }

        public void ImageProgramStringToCompressedFileName(final idStr imageProg, String[] fileName) {
            ImageProgramStringToCompressedFileName(imageProg.toString(), fileName);
        }

        public int NumLevelsForImageSize(int width, int height) {
            int numLevels = 1;

            while (width > 1 || height > 1) {
                numLevels++;
                width >>= 1;
                height >>= 1;
            }

            return numLevels;
        }
    };

    public static class idImageManager {

        public idList<idImage> images = new idList<>();
        public idStrList   ddsList;
        public idHashIndex ddsHash;
        //
        public boolean     insideLevelLoad;                 // don't actually load images now
        //
        public byte[] originalToCompressed = new byte[256]; // maps normal maps to 8 bit textures
        public byte[] compressedPalette    = new byte[768]; // the palette that normal maps use
        //
        // default filter modes for images
        public /*GLenum*/ int   textureMinFilter;
        public /*GLenum*/ int   textureMaxFilter;
        public            float textureAnisotropy;
        public            float textureLODBias;
        //
        public idImage[] imageHashTable = new idImage[FILE_HASH_SIZE];
        //
        public idImage backgroundImageLoads;                // chain of images that have background file loads active
        public idImage cacheLRU;                            // head/tail of doubly linked list
        public int     totalCachedImageSize;                // for determining when something should be purged
        //
        public int     numActiveBackgroundImageLoads;
        public final static int MAX_BACKGROUND_IMAGE_LOADS = 8;
        //
        //

        public idImageManager() {
            this.cacheLRU = new idImage();
        }

        public void Init() throws idException {

//	memset(imageHashTable, 0, sizeof(imageHashTable));
            imageHashTable = new idImage[imageHashTable.length];
            images.Resize(1024, 1024);

            // clear the cached LRU
            cacheLRU.cacheUsageNext = cacheLRU;
            cacheLRU.cacheUsagePrev = cacheLRU;

            // set default texture filter modes
            ChangeTextureFilter();

            // create built in images
            defaultImage = ImageFromFunction("_default", R_DefaultImage.getInstance());
            whiteImage = ImageFromFunction("_white", R_WhiteImage.getInstance());
            blackImage = ImageFromFunction("_black", R_BlackImage.getInstance());
            borderClampImage = ImageFromFunction("_borderClamp", R_BorderClampImage.getInstance());
            flatNormalMap = ImageFromFunction("_flat", R_FlatNormalImage.getInstance());
            ambientNormalMap = ImageFromFunction("_ambient", R_AmbientNormalImage.getInstance());
            specularTableImage = ImageFromFunction("_specularTable", R_SpecularTableImage.getInstance());
            specular2DTableImage = ImageFromFunction("_specular2DTable", R_Specular2DTableImage.getInstance());
            rampImage = ImageFromFunction("_ramp", R_RampImage.getInstance());
            alphaRampImage = ImageFromFunction("_alphaRamp", R_RampImage.getInstance());
            alphaNotchImage = ImageFromFunction("_alphaNotch", R_AlphaNotchImage.getInstance());
            fogImage = ImageFromFunction("_fog", R_FogImage.getInstance());
            fogEnterImage = ImageFromFunction("_fogEnter", R_FogEnterImage.getInstance());
            normalCubeMapImage = ImageFromFunction("_normalCubeMap", makeNormalizeVectorCubeMap.getInstance());
            noFalloffImage = ImageFromFunction("_noFalloff", R_CreateNoFalloffImage.getInstance());
            ImageFromFunction("_quadratic", R_QuadraticImage.getInstance());

            // cinematicImage is used for cinematic drawing
            // scratchImage is used for screen wipes/doublevision etc..
            cinematicImage = ImageFromFunction("_cinematic", R_RGBA8Image.getInstance());
            scratchImage = ImageFromFunction("_scratch", R_RGBA8Image.getInstance());
            scratchImage2 = ImageFromFunction("_scratch2", R_RGBA8Image.getInstance());
            accumImage = ImageFromFunction("_accum", R_RGBA8Image.getInstance());
            scratchCubeMapImage = ImageFromFunction("_scratchCubeMap", makeNormalizeVectorCubeMap.getInstance());
            currentRenderImage = ImageFromFunction("_currentRender", R_RGBA8Image.getInstance());

            cmdSystem.AddCommand("reloadImages", R_ReloadImages_f.getInstance(), CMD_FL_RENDERER, "reloads images");
            cmdSystem.AddCommand("listImages", R_ListImages_f.getInstance(), CMD_FL_RENDERER, "lists images");
            cmdSystem.AddCommand("combineCubeImages", R_CombineCubeImages_f.getInstance(), CMD_FL_RENDERER, "combines six images for roq compression");

            // should forceLoadImages be here?
        }

        public void Shutdown() {
            images.DeleteContents(true);
        }
//		

        // If the exact combination of parameters has been asked for already, an existing
        // image will be returned, otherwise a new image will be created.
        // Be careful not to use the same image file with different filter / repeat / etc parameters
        // if possible, because it will cause a second copy to be loaded.
        // If the load fails for any reason, the image will be filled in with the default
        // grid pattern.
        // Will automatically resample non-power-of-two images and execute image programs if needed.
        public idImage ImageFromFile(final String _name, textureFilter_t filter, boolean allowDownSize,
                                     textureRepeat_t repeat, textureDepth_t depth, cubeFiles_t cubeMap/* = CF_2D */) {
            idStr name;
            idImage image;
            int hash;

            if (null == _name || _name.isEmpty() || idStr.Icmp(_name, "default") == 0 || idStr.Icmp(_name, "_default") == 0) {
                declManager.MediaPrint("DEFAULTED\n");
                return globalImages.defaultImage;
            }

            // strip any .tga file extensions from anywhere in the _name, including image program parameters
            name = new idStr(_name);
            name.Replace(".tga", "");
            name.BackSlashesToSlashes();

            //
            // see if the image is already loaded, unless we
            // are in a reloadImages call
            //
            hash = name.FileNameHash();
            for (image = imageHashTable[hash]; image != null; image = image.hashNext) {
                if (name.Icmp(image.imgName.toString()) == 0) {
                    // the built in's, like _white and _flat always match the other options
                    if (name.oGet(0) == '_') {
                        return image;
                    }
                    if (image.cubeFiles != cubeMap) {
                        common.Error("Image '%s' has been referenced with conflicting cube map states", _name);
                    }

                    if (image.filter != filter || image.repeat != repeat) {
                        // we might want to have the system reset these parameters on every bind and
                        // share the image data
                        continue;
                    }

                    if (image.allowDownSize == allowDownSize && image.depth == depth) {
                        // note that it is used this level load
                        image.levelLoadReferenced = true;
                        if (image.partialImage != null) {
                            image.partialImage.levelLoadReferenced = true;
                        }
                        return image;
                    }

                    // the same image is being requested, but with a different allowDownSize or depth
                    // so pick the highest of the two and reload the old image with those parameters
                    if (!image.allowDownSize) {
                        allowDownSize = false;
                    }
                    if (image.depth.ordinal() > depth.ordinal()) {
                        depth = image.depth;
                    }
                    if (image.allowDownSize == allowDownSize && image.depth == depth) {
                        // the already created one is already the highest quality
                        image.levelLoadReferenced = true;
                        if (image.partialImage != null) {
                            image.partialImage.levelLoadReferenced = true;
                        }
                        return image;
                    }

                    image.allowDownSize = allowDownSize;
                    image.depth = depth;
                    image.levelLoadReferenced = true;
                    if (image.partialImage != null) {
                        image.partialImage.levelLoadReferenced = true;
                    }
                    if (image_preload.GetBool() && !insideLevelLoad) {
                        image.referencedOutsideLevelLoad = true;
                        image.ActuallyLoadImage(true, false);    // check for precompressed, load is from front end
                        declManager.MediaPrint("%dx%d %s (reload for mixed referneces)\n", image.uploadWidth, image.uploadHeight, image.imgName.toString());
                    }
                    return image;
                }
            }

            //
            // create a new image
            //
            image = AllocImage(name.toString());

            // HACK: to allow keep fonts from being mip'd, as new ones will be introduced with localization
            // this keeps us from having to make a material for each font tga
            if (name.Find("fontImage_") >= 0) {
                allowDownSize = false;
            }

            image.allowDownSize = allowDownSize;
            image.repeat = repeat;
            image.depth = depth;
            image.type = TT_2D;
            image.cubeFiles = cubeMap;
            image.filter = filter;

            image.levelLoadReferenced = true;

            // also create a shrunken version if we are going to dynamically cache the full size image
            if (image.ShouldImageBePartialCached()) {
                // if we only loaded part of the file, create a new idImage for the shrunken version
                image.partialImage = new idImage();

                image.partialImage.allowDownSize = allowDownSize;
                image.partialImage.repeat = repeat;
                image.partialImage.depth = depth;
                image.partialImage.type = TT_2D;
                image.partialImage.cubeFiles = cubeMap;
                image.partialImage.filter = filter;

                image.partialImage.levelLoadReferenced = true;

                // we don't bother hooking this into the hash table for lookup, but we do add it to the manager
                // list for listImages
                globalImages.images.Append(image.partialImage);
                image.partialImage.imgName.oSet(image.imgName);
                image.partialImage.isPartialImage = true;

                // let the background file loader know that we can load
                image.precompressedFile = true;

                if (image_preload.GetBool() && !insideLevelLoad) {
                    image.partialImage.ActuallyLoadImage(true, false);    // check for precompressed, load is from front end
                    declManager.MediaPrint("%dx%d %s\n", image.partialImage.uploadWidth, image.partialImage.uploadHeight, image.imgName.toString());
                } else {
                    declManager.MediaPrint("%s\n", image.imgName.toString());
                }
                return image;
            }

            // load it if we aren't in a level preload
            if (image_preload.GetBool() && !insideLevelLoad) {
                image.referencedOutsideLevelLoad = true;
                if (Material.idMaterial.DBG_ParseStage == 41) {
//                    return null;
                }
                image.ActuallyLoadImage(true, false);    // check for precompressed, load is from front end
                declManager.MediaPrint("%dx%d %s\n", image.uploadWidth, image.uploadHeight, image.imgName.toString());
            } else {
                declManager.MediaPrint("%s\n", image.imgName.toString());
            }

            return image;
        }

        /*
         ===============
         ImageFromFile

         Finds or loads the given image, always returning a valid image pointer.
         Loading of the image may be deferred for dynamic loading.
         ==============
         */
        public idImage ImageFromFile(final String name, textureFilter_t filter, boolean allowDownSize,
                                     textureRepeat_t repeat, textureDepth_t depth) {
            return ImageFromFile(name, filter, allowDownSize, repeat, depth, CF_2D);
        }


        // look for a loaded image, whatever the parameters
        public idImage GetImage(final String _name) {
            idStr name;
            idImage image;
            int hash;

            if (null == _name || _name.isEmpty() || idStr.Icmp(_name, "default") == 0 || idStr.Icmp(_name, "_default") == 0) {
                declManager.MediaPrint("DEFAULTED\n");
                return globalImages.defaultImage;
            }

            // strip any .tga file extensions from anywhere in the _name, including image program parameters
            name = new idStr(_name);
            name.Replace(".tga", "");
            name.BackSlashesToSlashes();

            //
            // look in loaded images
            //
            hash = name.FileNameHash();
            for (image = imageHashTable[hash]; image != null; image = image.hashNext) {
                if (name.Icmp(image.imgName.toString()) == 0) {
                    return image;
                }
            }

            return null;
        }
//

        /*
         ==================
         ImageFromFunction

         Images that are procedurally generated are allways specified
         with a callback which must work at any time, allowing the OpenGL
         system to be completely regenerated if needed.
         ==================
         */
        // The callback will be issued immediately, and later if images are reloaded or vid_restart
        // The callback function should call one of the idImage::Generate* functions to fill in the data
        public idImage ImageFromFunction(final String _name, GeneratorFunction generatorFunction) {
            idStr name;
            idImage image;
            int hash;

            if (null == _name) {//tut tut tut
                common.FatalError("idImageManager::ImageFromFunction: NULL name");
            }

            // strip any .tga file extensions from anywhere in the _name
            name = new idStr(_name);
            name.Replace(".tga", "");
            name.BackSlashesToSlashes();

            // see if the image already exists
            hash = name.FileNameHash();
            for (image = imageHashTable[hash]; image != null; image = image.hashNext) {
                if (name.Icmp(image.imgName.toString()) == 0) {
                    if (image.generatorFunction != generatorFunction) {
                        common.DPrintf("WARNING: reused image %s with mixed generators\n", name);
                    }
                    return image;
                }
            }

            // create the image and issue the callback
            image = AllocImage(name.toString());

            image.generatorFunction = generatorFunction;

            if (image_preload.GetBool()) {
                // check for precompressed, load is from the front end
                image.referencedOutsideLevelLoad = true;
                image.ActuallyLoadImage(true, false);
            }

            return image;
        }


        /*
         ==================
         R_CompleteBackgroundImageLoads

         Do we need to worry about vid_restarts here?//TODO:do we indeed?
         ==================
         */
        // called once a frame to allow any background loads that have been completed
        // to turn into textures.
        public void CompleteBackgroundImageLoads() {
            idImage remainingList = null;
            idImage next;

            for (idImage image = backgroundImageLoads; image != null; image = next) {
                next = image.bglNext;
                if (image.bgl.completed) {
                    numActiveBackgroundImageLoads--;
                    fileSystem.CloseFile(image.bgl.f);
                    // upload the image
                    image.UploadPrecompressedImage(image.bgl.file.buffer, image.bgl.file.length);
                    image.bgl.file.buffer = null;//R_StaticFree(image.bgl.file.buffer);
                    if (image_showBackgroundLoads.GetBool()) {
                        common.Printf("R_CompleteBackgroundImageLoad: %s\n", image.imgName);
                    }
                } else {
                    image.bglNext = remainingList;
                    remainingList = image;
                }
            }
            if (image_showBackgroundLoads.GetBool()) {
                if (numActiveBackgroundImageLoads != prev) {
                    prev = numActiveBackgroundImageLoads;
                    common.Printf("background Loads: %d\n", numActiveBackgroundImageLoads);
                }
            }

            backgroundImageLoads = remainingList;
        }

        private static int prev;

        // returns the number of bytes of image data bound in the previous frame
        public int SumOfUsedImages() {
            int total;
            int i;
            idImage image;

            total = 0;
            for (i = 0; i < images.Num(); i++) {
                image = images.oGet(i);
                if (image.frameUsed == backEnd.frameCount) {
                    total += image.StorageSize();
                }
            }

            return total;
        }

        // called each frame to allow some cvars to automatically force changes
        public void CheckCvars() {
            // textureFilter stuff
            if (image_filter.IsModified() || image_anisotropy.IsModified() || image_lodbias.IsModified()) {
                ChangeTextureFilter();
                image_filter.ClearModified();
                image_anisotropy.ClearModified();
                image_lodbias.ClearModified();
            }
        }

        // purges all the images before a vid_restart
        public void PurgeAllImages() {
            int i;
            idImage image;

            for (i = 0; i < images.Num(); i++) {
                image = images.oGet(i);
                image.PurgeImage();
            }
        }

        // reloads all apropriate images after a vid_restart
        public void ReloadAllImages() {
            idCmdArgs args = new idCmdArgs();

            // build the compressed normal map palette
            SetNormalPalette();

            args.TokenizeString("reloadImages reload", false);
            R_ReloadImages_f.getInstance().run(args);
        }

        // disable the active texture unit
        public void BindNull() {
            tmu_t tmu;

            tmu = backEnd.glState.tmu[backEnd.glState.currenttmu];

            RB_LogComment("BindNull()\n");
            if (tmu.textureType == TT_CUBIC) {
                qglDisable(GL_TEXTURE_CUBE_MAP/*_EXT*/);
            } else if (tmu.textureType == TT_3D) {
                qglDisable(GL_TEXTURE_3D);
            } else if (tmu.textureType == TT_2D) {
                qglDisable(GL_TEXTURE_2D);
            }
            tmu.textureType = TT_DISABLED;
        }

        /*
         ====================
         BeginLevelLoad

         Mark all file based images as currently unused,
         but don't free anything.  Calls to ImageFromFile() will
         either mark the image as used, or create a new image without
         loading the actual data.
         ====================
         */
        // Mark all file based images as currently unused,
        // but don't free anything.  Calls to ImageFromFile() will
        // either mark the image as used, or create a new image without
        // loading the actual data.
        // Called only by renderSystem::BeginLevelLoad
        public void BeginLevelLoad() {
            insideLevelLoad = true;

            for (int i = 0; i < images.Num(); i++) {
                idImage image = images.oGet(i);

                // generator function images are always kept around
                if (image.generatorFunction != null) {
                    continue;
                }

                if (com_purgeAll.GetBool()) {
                    image.PurgeImage();
                }

                image.levelLoadReferenced = false;
            }
        }


        /*
         ====================
         EndLevelLoad

         Free all images marked as unused, and load all images that are necessary.
         This architecture prevents us from having the union of two level's
         worth of data present at one time.

         preload everything, never free
         preload everything, free unused after level load
         blocking load on demand
         preload low mip levels, background load remainder on demand
         ====================
         */
        // Free all images marked as unused, and load all images that are necessary.
        // This architecture prevents us from having the union of two level's
        // worth of data present at one time.
        // Called only by renderSystem::EndLevelLoad
        public void EndLevelLoad() {
            int start = Sys_Milliseconds();

            insideLevelLoad = false;
            if (idAsyncNetwork.serverDedicated.GetInteger() != 0) {
                return;
            }

            common.Printf("----- idImageManager.EndLevelLoad -----\n");

            int purgeCount = 0;
            int keepCount = 0;
            int loadCount = 0;

            // purge the ones we don't need
            for (int i = 0; i < images.Num(); i++) {
                idImage image = images.oGet(i);
                if (image.generatorFunction != null) {
                    continue;
                }

                if (!image.levelLoadReferenced && !image.referencedOutsideLevelLoad) {
//			common.Printf( "Purging %s\n", image.imgName.c_str() );
                    purgeCount++;
                    image.PurgeImage();
                } else if (image.texNum != idImage.TEXTURE_NOT_LOADED) {
//			common.Printf( "Keeping %s\n", image.imgName.c_str() );
                    keepCount++;
                }
            }

            // load the ones we do need, if we are preloading
            for (int i = 0; i < images.Num(); i++) {
                idImage image = images.oGet(i);
                if (image.generatorFunction != null) {
                    continue;
                }

                if (image.levelLoadReferenced && image.texNum == idImage.TEXTURE_NOT_LOADED && null == image.partialImage) {
//			common.Printf( "Loading %s\n", image.imgName.c_str() );
                    loadCount++;
                    image.ActuallyLoadImage(true, false);

                    if ((loadCount & 15) == 0) {
                        session.PacifierUpdate();
                    }
                }
            }

            int end = Sys_Milliseconds();
            common.Printf("%5d purged from previous\n", purgeCount);
            common.Printf("%5d kept from previous\n", keepCount);
            common.Printf("%5d new loaded\n", loadCount);
            common.Printf("all images loaded in %5.1f seconds\n", (end - start) * 0.001);
            common.Printf("----------------------------------------\n");
        }

        // used to clear and then write the dds conversion batch file
        public void StartBuild() {
            ddsList.Clear();
            ddsHash.Free();
        }

        public void FinishBuild(boolean removeDups /*= false */) {
            idFile batchFile;
            if (removeDups) {
                ddsList.Clear();
                ByteBuffer[] buffer = {null};
                fileSystem.ReadFile("makedds.bat", buffer);
                if (buffer[0] != null) {
                    idStr str = new idStr(new String(buffer[0].array()));
                    while (str.Length() != 0) {
                        int n = str.Find('\n');
                        if (n > 0) {
                            idStr line = str.Left(n + 1);
                            idStr right = new idStr();
                            str.Right(str.Length() - n - 1, right);
                            str = right;
                            ddsList.AddUnique(line);
                        } else {
                            break;
                        }
                    }
                }
            }
            batchFile = fileSystem.OpenFileWrite((removeDups) ? "makedds2.bat" : "makedds.bat");
            if (batchFile != null) {
                int i;
                int ddsNum = ddsList.Num();

                for (i = 0; i < ddsNum; i++) {
                    batchFile.WriteFloatString("%s", ddsList.oGet(i).toString());
                    batchFile.Printf("@echo Finished compressing %d of %d.  %.1f percent done.\n", i + 1, ddsNum, ((float) (i + 1) / (float) ddsNum) * 100.f);
                }
                fileSystem.CloseFile(batchFile);
            }
            ddsList.Clear();
            ddsHash.Free();
        }

        public void FinishBuild() {
            FinishBuild(false);
        }

        public void AddDDSCommand(final String cmd) {
            int i, key;

            if (!(cmd != null && !cmd.isEmpty())) {//TODO:WdaF?
                return;
            }

            key = ddsHash.GenerateKey(cmd, false);
            for (i = ddsHash.First(key); i != -1; i = ddsHash.Next(i)) {
                if (ddsList.oGet(i).Icmp(cmd) == 0) {
                    break;
                }
            }

            if (i == -1) {
                ddsList.Append(new idStr(cmd));
            }
        }

        public void PrintMemInfo(MemInfo_t mi) {
            int i, j, total = 0;
            int[] sortIndex;
            idFile f;

            f = fileSystem.OpenFileWrite(mi.filebase + "_images.txt");
            if (null == f) {
                return;
            }

            // sort first
            sortIndex = new int[images.Num()];

            for (i = 0; i < images.Num(); i++) {
                sortIndex[i] = i;
            }

            for (i = 0; i < images.Num() - 1; i++) {
                for (j = i + 1; j < images.Num(); j++) {
                    if (images.oGet(sortIndex[i]).StorageSize() < images.oGet(sortIndex[j]).StorageSize()) {
                        int temp = sortIndex[i];
                        sortIndex[i] = sortIndex[j];
                        sortIndex[j] = temp;
                    }
                }
            }

            // print next
            for (i = 0; i < images.Num(); i++) {
                idImage im = images.oGet(sortIndex[i]);
                int size;

                size = im.StorageSize();
                total += size;

                f.Printf("%s %3d %s\n", idStr.FormatNumber(size), im.refCount, im.imgName);
            }

//	delete sortIndex;
            mi.imageAssetsTotal = total;

            f.Printf("\nTotal image bytes allocated: %s\n", idStr.FormatNumber(total));
            fileSystem.CloseFile(f);
        }

        //
        // cvars
        public static idCVar image_roundDown                  = new idCVar("image_roundDown", "1", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "round bad sizes down to nearest power of two");
        public static idCVar image_colorMipLevels             = new idCVar("image_colorMipLevels", "0", CVAR_RENDERER | CVAR_BOOL, "development aid to see texture mip usage");
        public static idCVar image_downSize                   = new idCVar("image_downSize", "0", CVAR_RENDERER | CVAR_ARCHIVE, "controls texture downsampling");
        public static idCVar image_useCompression             = new idCVar("image_useCompression", "1", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "0 = force everything to high quality");
        public static idCVar image_filter                     = new idCVar("image_filter", imageFilter[1], CVAR_RENDERER | CVAR_ARCHIVE, "changes texture filtering on mipmapped images", imageFilter, new idCmdSystem.ArgCompletion_String(imageFilter));
        public static idCVar image_anisotropy                 = new idCVar("image_anisotropy", "1", CVAR_RENDERER | CVAR_ARCHIVE, "set the maximum texture anisotropy if available");
        public static idCVar image_lodbias                    = new idCVar("image_lodbias", "0", CVAR_RENDERER | CVAR_ARCHIVE, "change lod bias on mipmapped images");
        public static idCVar image_useAllFormats              = new idCVar("image_useAllFormats", "1", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "allow alpha/intensity/luminance/luminance+alpha");
        public static idCVar image_usePrecompressedTextures   = new idCVar("image_usePrecompressedTextures", "1", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "use .dds files if present");
        public static idCVar image_writePrecompressedTextures = new idCVar("image_writePrecompressedTextures", "0", CVAR_RENDERER | CVAR_BOOL, "write .dds files if necessary");
        public static idCVar image_writeNormalTGA             = new idCVar("image_writeNormalTGA", "0", CVAR_RENDERER | CVAR_BOOL, "write .tgas of the final normal maps for debugging");
        public static idCVar image_writeNormalTGAPalletized   = new idCVar("image_writeNormalTGAPalletized", "0", CVAR_RENDERER | CVAR_BOOL, "write .tgas of the final palletized normal maps for debugging");
        public static idCVar image_writeTGA                   = new idCVar("image_writeTGA", "0", CVAR_RENDERER | CVAR_BOOL, "write .tgas of the non normal maps for debugging");
        public static idCVar image_useNormalCompression       = new idCVar("image_useNormalCompression", "2", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_INTEGER, "2 = use rxgb compression for normal maps, 1 = use 256 color compression for normal maps if available");
        public static idCVar image_useOffLineCompression      = new idCVar("image_useOfflineCompression", "0", CVAR_RENDERER | CVAR_BOOL, "write a batch file for offline compression of DDS files");
        public static idCVar image_preload                    = new idCVar("image_preload", "1", CVAR_RENDERER | CVAR_BOOL | CVAR_ARCHIVE, "if 0, dynamically load all images");
        public static idCVar image_cacheMinK                  = new idCVar("image_cacheMinK", "200", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_INTEGER, "maximum KB of precompressed files to read at specification time");
        //
        public static idCVar image_cacheMegs                  = new idCVar("image_cacheMegs", "20", CVAR_RENDERER | CVAR_ARCHIVE, "maximum MB set aside for temporary loading of full-sized precompressed images");
        public static idCVar image_useCache                   = new idCVar("image_useCache", "0", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "1 = do background load image caching");
        public static idCVar image_showBackgroundLoads        = new idCVar("image_showBackgroundLoads", "0", CVAR_RENDERER | CVAR_BOOL, "1 = print number of outstanding background loads");
        public static idCVar image_forceDownSize              = new idCVar("image_forceDownSize", "0", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "");
        public static idCVar image_downSizeSpecular           = new idCVar("image_downSizeSpecular", "0", CVAR_RENDERER | CVAR_ARCHIVE, "controls specular downsampling");
        public static idCVar image_downSizeSpecularLimit      = new idCVar("image_downSizeSpecularLimit", "64", CVAR_RENDERER | CVAR_ARCHIVE, "controls specular downsampled limit");
        public static idCVar image_downSizeBump               = new idCVar("image_downSizeBump", "0", CVAR_RENDERER | CVAR_ARCHIVE, "controls normal map downsampling");
        public static idCVar image_downSizeBumpLimit          = new idCVar("image_downSizeBumpLimit", "128", CVAR_RENDERER | CVAR_ARCHIVE, "controls normal map downsample limit");
        public static idCVar image_ignoreHighQuality          = new idCVar("image_ignoreHighQuality", "0", CVAR_RENDERER | CVAR_ARCHIVE, "ignore high quality setting on materials");
        public static idCVar image_downSizeLimit              = new idCVar("image_downSizeLimit", "256", CVAR_RENDERER | CVAR_ARCHIVE, "controls diffuse map downsample limit");
        //
        // built-in images
        public idImage defaultImage;
        public idImage flatNormalMap;                // 128 128 255 in all pixels
        public idImage ambientNormalMap;            // tr.ambientLightVector encoded in all pixels
        public idImage rampImage;                // 0-255 in RGBA in S
        public idImage alphaRampImage;                // 0-255 in alpha, 255 in RGB
        public idImage alphaNotchImage;                         // 2x1 texture with just 1110 and 1111 with point sampling
        public idImage whiteImage;                // full of 0xff
        public idImage blackImage;                // full of 0x00
        public idImage normalCubeMapImage;            // cube map to normalize STR into RGB
        public idImage noFalloffImage;                // all 255, but zero clamped
        public idImage fogImage;                // increasing alpha is denser fog
        public idImage fogEnterImage;                // adjust fogImage alpha based on terminator plane
        public idImage cinematicImage;
        public idImage scratchImage;
        public idImage scratchImage2;
        public idImage accumImage;
        public idImage currentRenderImage;            // for SS_POST_PROCESS shaders
        public idImage scratchCubeMapImage;
        public idImage specularTableImage;            // 1D intensity texture with our specular function
        public idImage specular2DTableImage;                    // 2D intensity texture with our specular function with variable specularity
        public idImage borderClampImage;            // white inside, black outside
//
        //--------------------------------------------------------

        /*
         ==============
         AllocImage

         Allocates an idImage, adds it to the list,
         copies the name, and adds it to the hash chain.
         ==============
         */
        public idImage AllocImage(final String name) {
            idImage image;
            int hash;

            if (name.length() >= MAX_IMAGE_NAME) {
                common.Error("idImageManager::AllocImage: \"%s\" is too long\n", name);
            }

            hash = new idStr(name).FileNameHash();
//            System.out.printf(">>>>>>>>>>>>>>%d--%s\n", hash, name);
//            System.out.printf(">>>>>>>>>>>>>>%d--%s\n", idStr.IHash(name.toCharArray()), name);

            image = new idImage();
            images.Append(image);

            image.hashNext = imageHashTable[hash];
            imageHashTable[hash] = image;

            image.imgName.oSet(name);

            return image;
        }

        /*
         ==================
         SetNormalPalette

         Create a 256 color palette to be used by compressed normal maps
         ==================
         */
        public void SetNormalPalette() {
            int i, j;
            idVec3 v = new idVec3();
            float t;
            //byte temptable[768];
            byte[] temptable = compressedPalette;
            int[] compressedToOriginal = new int[16];

            // make an ad-hoc separable compression mapping scheme
            for (i = 0; i < 8; i++) {
                float f, y;

                f = (i + 1) / 8.5f;
                y = idMath.Sqrt(1.0f - f * f);
                y = 1.0f - y;

                compressedToOriginal[7 - i] = 127 - (int) (y * 127 + 0.5);
                compressedToOriginal[8 + i] = 128 + (int) (y * 127 + 0.5);
            }

            for (i = 0; i < 256; i++) {
                if (i <= compressedToOriginal[0]) {
                    originalToCompressed[i] = 0;
                } else if (i >= compressedToOriginal[15]) {
                    originalToCompressed[i] = 15;
                } else {
                    for (j = 0; j < 14; j++) {
                        if (i <= compressedToOriginal[j + 1]) {
                            break;
                        }
                    }
                    if (i - compressedToOriginal[j] < compressedToOriginal[j + 1] - i) {
                        originalToCompressed[i] = (byte) j;
                    } else {
                        originalToCompressed[i] = (byte) (j + 1);
                    }
                }
            }

            if (false) {
//	for ( i = 0; i < 16; i++ ) {
//		for ( j = 0 ; j < 16 ; j++ ) {
//
//			v.oSet(0,  ( i - 7.5 ) / 8);
//			v.oSet(1,  ( j - 7.5 ) / 8);
//
//			t = 1.0 - ( v.oGet(0)*v.oGet(0) + v.oGet(1)*v.oGet(1) );
//			if ( t < 0 ) {
//				t = 0;
//			}
//			v.oSet(2,  idMath.Sqrt( t ));
//
//			temptable[(i*16+j)*3+0] = 128 + floor( 127 * v.oGet(0) + 0.5 );
//			temptable[(i*16+j)*3+1] = 128 + floor( 127 * v.oGet(1) );
//			temptable[(i*16+j)*3+2] = 128 + floor( 127 * v.oGet(2) );
//		}
//	}
            } else {
                for (i = 0; i < 16; i++) {
                    for (j = 0; j < 16; j++) {

                        v.oSet(0, (compressedToOriginal[i] - 127.5f) / 128f);
                        v.oSet(1, (compressedToOriginal[j] - 127.5f) / 128f);

                        t = 1.0f - (v.oGet(0) * v.oGet(0) + v.oGet(1) * v.oGet(1));
                        if (t < 0) {
                            t = 0;
                        }
                        v.oSet(2, idMath.Sqrt(t));

                        temptable[(i * 16 + j) * 3 + 0] = (byte) (128 + Math.floor(127 * v.oGet(0) + 0.5));
                        temptable[(i * 16 + j) * 3 + 1] = (byte) (128 + Math.floor(127 * v.oGet(1)));
                        temptable[(i * 16 + j) * 3 + 2] = (byte) (128 + Math.floor(127 * v.oGet(2)));
                    }
                }
            }

            // color 255 will be the "nullnormal" color for no reflection
            temptable[255 * 3 + 0]
                    = temptable[255 * 3 + 1]
                    = temptable[255 * 3 + 2] = (byte) 128;

            if (!glConfig.sharedTexturePaletteAvailable) {
                return;
            }

            qglColorTableEXT(GL_SHARED_TEXTURE_PALETTE_EXT,
                    GL_RGB,
                    256,
                    GL_RGB,
                    GL_UNSIGNED_BYTE,
                    temptable);

            qglEnable(GL_SHARED_TEXTURE_PALETTE_EXT);
        }

        static class filterName_t {

            public filterName_t(String name, int minimize, int maximize) {
                this.name = name;
                this.minimize = minimize;
                this.maximize = maximize;
            }

            String name;
            int    minimize, maximize;
        }

        ;
        private static final filterName_t textureFilters[] = {
                new filterName_t("GL_LINEAR_MIPMAP_NEAREST", GL_LINEAR_MIPMAP_NEAREST, GL_LINEAR),
                new filterName_t("GL_LINEAR_MIPMAP_LINEAR", GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR),
                new filterName_t("GL_NEAREST", GL_NEAREST, GL_NEAREST),
                new filterName_t("GL_LINEAR", GL_LINEAR, GL_LINEAR),
                new filterName_t("GL_NEAREST_MIPMAP_NEAREST", GL_NEAREST_MIPMAP_NEAREST, GL_NEAREST),
                new filterName_t("GL_NEAREST_MIPMAP_LINEAR", GL_NEAREST_MIPMAP_LINEAR, GL_NEAREST)
        };

        /*
         ===============
         ChangeTextureFilter

         This resets filtering on all loaded images
         New images will automatically pick up the current values.
         ===============
         */
        public void ChangeTextureFilter() {
            int i;
            idImage glt;
            String string;

            // if these are changed dynamically, it will force another ChangeTextureFilter
            image_filter.ClearModified();
            image_anisotropy.ClearModified();
            image_lodbias.ClearModified();

            string = image_filter.GetString();
            for (i = 0; i < 6; i++) {
                if (0 == idStr.Icmp(textureFilters[i].name, string)) {
                    break;
                }
            }

            if (i == 6) {
                common.Warning("bad r_textureFilter: '%s'", string);
                // default to LINEAR_MIPMAP_NEAREST
                i = 0;
            }

            // set the values for future images
            textureMinFilter = textureFilters[i].minimize;
            textureMaxFilter = textureFilters[i].maximize;
            textureAnisotropy = image_anisotropy.GetFloat();
            if (textureAnisotropy < 1) {
                textureAnisotropy = 1;
            } else if (textureAnisotropy > glConfig.maxTextureAnisotropy) {
                textureAnisotropy = glConfig.maxTextureAnisotropy;
            }
            textureLODBias = image_lodbias.GetFloat();

            // change all the existing mipmap texture objects with default filtering
            for (i = 0; i < images.Num(); i++) {
                int texEnum = GL_TEXTURE_2D;

                glt = images.oGet(i);

                switch (glt.type) {
                    case TT_2D:
                        texEnum = GL_TEXTURE_2D;
                        break;
                    case TT_3D:
                        texEnum = GL_TEXTURE_3D;
                        break;
                    case TT_CUBIC:
                        texEnum = GL_TEXTURE_CUBE_MAP/*_EXT*/;
                        break;
                }

                // make sure we don't start a background load
                if (glt.texNum == idImage.TEXTURE_NOT_LOADED) {
                    continue;
                }
                glt.Bind();
                if (glt.filter == TF_DEFAULT) {
                    qglTexParameterf(texEnum, GL_TEXTURE_MIN_FILTER, globalImages.textureMinFilter);
                    qglTexParameterf(texEnum, GL_TEXTURE_MAG_FILTER, globalImages.textureMaxFilter);
                }
                if (glConfig.anisotropicAvailable) {
                    qglTexParameterf(texEnum, GL_TEXTURE_MAX_ANISOTROPY_EXT, globalImages.textureAnisotropy);
                }
                if (glConfig.textureLODBiasAvailable) {
                    qglTexParameterf(texEnum, GL_TEXTURE_LOD_BIAS_EXT, globalImages.textureLODBias);
                }
            }
        }
    }

    ;
}
