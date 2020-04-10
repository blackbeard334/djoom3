package neo.Renderer;

import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Image.idImage.DEFAULT_SIZE;
import static neo.Renderer.Image.textureDepth_t.TD_DEFAULT;
import static neo.Renderer.Image.textureDepth_t.TD_HIGH_QUALITY;
import static neo.Renderer.Image_files.R_LoadImage;
import static neo.Renderer.Image_files.R_WriteTGA;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_COUNT;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_GUIS;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_ITEMS;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_MODELGEOMETRY;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_MODELSOTHER;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_MONSTER;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_NPC;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_OTHER;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_WEAPON;
import static neo.Renderer.Image_init.IMAGE_CLASSIFICATION.IC_WORLDGEOMETRY;
import static neo.Renderer.Image_process.R_HorizontalFlip;
import static neo.Renderer.Image_process.R_RotatePic;
import static neo.Renderer.Image_process.R_VerticalFlip;
import static neo.Renderer.Material.textureFilter_t.TF_DEFAULT;
import static neo.Renderer.Material.textureFilter_t.TF_LINEAR;
import static neo.Renderer.Material.textureFilter_t.TF_NEAREST;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP_TO_BORDER;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP_TO_ZERO;
import static neo.Renderer.Material.textureRepeat_t.TR_REPEAT;
import static neo.Renderer.qgl.qglTexParameterfv;
import static neo.Renderer.tr_local.FALLOFF_TEXTURE_SIZE;
import static neo.Renderer.tr_local.FOG_ENTER_SIZE;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import static neo.TempDump.NOT;
import static neo.TempDump.flatten;
import static neo.TempDump.wrapToNativeBuffer;
import static neo.idlib.Lib.idLib.common;
import static neo.open.gl.QGLConstantsIfc.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
import static neo.open.gl.QGLConstantsIfc.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_2D;
import static neo.open.gl.QGLConstantsIfc.GL_TEXTURE_BORDER_COLOR;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import neo.Renderer.Image.GeneratorFunction;
import neo.Renderer.Image.idImage;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.containers.List.idList;
import neo.idlib.math.Math_h.idMath;
import neo.open.Nio;

/**
 *
 */
public class Image_init {

    static final String[] imageFilter = {
        "GL_LINEAR_MIPMAP_NEAREST",
        "GL_LINEAR_MIPMAP_LINEAR",
        "GL_NEAREST",
        "GL_LINEAR",
        "GL_NEAREST_MIPMAP_NEAREST",
        "GL_NEAREST_MIPMAP_LINEAR",
        null
    };

    static class IMAGE_CLASSIFICATION {

        static final int IC_NPC = 0;
        static final int IC_WEAPON = 1;
        static final int IC_MONSTER = 2;
        static final int IC_MODELGEOMETRY = 3;
        static final int IC_ITEMS = 4;
        static final int IC_MODELSOTHER = 5;
        static final int IC_GUIS = 6;
        static final int IC_WORLDGEOMETRY = 7;
        static final int IC_OTHER = 8;
        static final int IC_COUNT = 9;
    }

    static class imageClassificate_t {

        String rootPath;
        String desc;
        int type;
        int maxWidth;
        int maxHeight;

        public imageClassificate_t(String rootPath, String desc, int type, int maxWidth, int maxHeight) {
            this.rootPath = rootPath;
            this.desc = desc;
            this.type = type;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }

    }

    static class intList extends idList< Integer> {
    }

    static final imageClassificate_t[] IC_Info = {
        new imageClassificate_t("models/characters", "Characters", IC_NPC, 512, 512),
        new imageClassificate_t("models/weapons", "Weapons", IC_WEAPON, 512, 512),
        new imageClassificate_t("models/monsters", "Monsters", IC_MONSTER, 512, 512),
        new imageClassificate_t("models/mapobjects", "Model Geometry", IC_MODELGEOMETRY, 512, 512),
        new imageClassificate_t("models/items", "Items", IC_ITEMS, 512, 512),
        new imageClassificate_t("models", "Other model textures", IC_MODELSOTHER, 512, 512),
        new imageClassificate_t("guis/assets", "Guis", IC_GUIS, 256, 256),
        new imageClassificate_t("textures", "World Geometry", IC_WORLDGEOMETRY, 256, 256),
        new imageClassificate_t("", "Other", IC_OTHER, 256, 256)
    };

    static int ClassifyImage(final String name) {
        idStr str;
        str = new idStr(name);
        for (int i = 0; i < IC_COUNT; i++) {
            if (str.Find(IC_Info[i].rootPath, false) == 0) {
                return IC_Info[i].type;
            }
        }
        return IC_OTHER;
    }

    /*
     ===============
     R_ListImages_f
     ===============
     */
    static class R_ListImages_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ListImages_f();

        private R_ListImages_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i, j, partialSize;
            idImage image;
            int totalSize;
            int count = 0;
            int matchTag = 0;
            boolean uncompressedOnly = false;
            boolean unloaded = false;
            boolean partial = false;
            boolean cached = false;
            boolean uncached = false;
            boolean failed = false;
            boolean touched = false;
            boolean sorted = false;
            boolean duplicated = false;
            boolean byClassification = false;
            boolean overSized = false;

            if (args.Argc() == 1) {
            } else if (args.Argc() == 2) {
                if (idStr.Icmp(args.Argv(1), "uncompressed") == 0) {
                    uncompressedOnly = true;
                } else if (idStr.Icmp(args.Argv(1), "sorted") == 0) {
                    sorted = true;
                } else if (idStr.Icmp(args.Argv(1), "partial") == 0) {
                    partial = true;
                } else if (idStr.Icmp(args.Argv(1), "unloaded") == 0) {
                    unloaded = true;
                } else if (idStr.Icmp(args.Argv(1), "cached") == 0) {
                    cached = true;
                } else if (idStr.Icmp(args.Argv(1), "uncached") == 0) {
                    uncached = true;
                } else if (idStr.Icmp(args.Argv(1), "tagged") == 0) {
                    matchTag = 1;
                } else if (idStr.Icmp(args.Argv(1), "duplicated") == 0) {
                    duplicated = true;
                } else if (idStr.Icmp(args.Argv(1), "touched") == 0) {
                    touched = true;
                } else if (idStr.Icmp(args.Argv(1), "classify") == 0) {
                    byClassification = true;
                    sorted = true;
                } else if (idStr.Icmp(args.Argv(1), "oversized") == 0) {
                    byClassification = true;
                    sorted = true;
                    overSized = true;
                } else {
                    failed = true;
                }
            } else {
                failed = true;
            }

            if (failed) {
                common.Printf("usage: listImages [ sorted | partial | unloaded | cached | uncached | tagged | duplicated | touched | classify | showOverSized ]\n");
                return;
            }

            final String header = "       -w-- -h-- filt -fmt-- wrap  size --name-------\n";
            common.Printf("\n%s", header);

            totalSize = 0;

//	sortedImage_t	[]sortedArray = (sortedImage_t *)alloca( sizeof( sortedImage_t ) * globalImages.images.Num() );
            final sortedImage_t[] sortedArray = new sortedImage_t[globalImages.images.Num()];

            for (i = 0; i < globalImages.images.Num(); i++) {
                image = globalImages.images.oGet(i);

                if (uncompressedOnly) {
                    if (((image.internalFormat >= GL_COMPRESSED_RGB_S3TC_DXT1_EXT) && (image.internalFormat <= GL_COMPRESSED_RGBA_S3TC_DXT5_EXT))
                            || (image.internalFormat == 0x80E5)) {
                        continue;
                    }
                }

                if ((matchTag != 0) && (image.classification != matchTag)) {
                    continue;
                }
                if (unloaded && (image.texNum != idImage.TEXTURE_NOT_LOADED)) {
                    continue;
                }
                if (partial && !image.isPartialImage) {
                    continue;
                }
                if (cached && ((null == image.partialImage) || (image.texNum == idImage.TEXTURE_NOT_LOADED))) {
                    continue;
                }
                if (uncached && ((null == image.partialImage) || (image.texNum != idImage.TEXTURE_NOT_LOADED))) {
                    continue;
                }

                // only print duplicates (from mismatched wrap / clamp, etc)
                if (duplicated) {
//			int j;
                    for (j = i + 1; j < globalImages.images.Num(); j++) {
                        if (idStr.Icmp(image.imgName, globalImages.images.oGet(j).imgName) == 0) {
                            break;
                        }
                    }
                    if (j == globalImages.images.Num()) {
                        continue;
                    }
                }

                // "listimages touched" will list only images bound since the last "listimages touched" call
                if (touched) {
                    if (image.bindCount == 0) {
                        continue;
                    }
                    image.bindCount = 0;
                }

                if (sorted) {
                    sortedArray[count].image = image;
                    sortedArray[count].size = image.StorageSize();
                } else {
                    common.Printf("%4d:", i);
                    image.Print();
                }
                totalSize += image.StorageSize();
                count++;
            }

            if (sorted) {
                Arrays.sort(sortedArray, 0, count, new R_QsortImageSizes());//qsort(sortedArray, count, sizeof(sortedImage_t), R_QsortImageSizes);
                partialSize = 0;
                for (i = 0; i < count; i++) {
                    common.Printf("%4d:", i);
                    sortedArray[i].image.Print();
                    partialSize += sortedArray[i].image.StorageSize();
                    if (((i + 1) % 10) == 0) {
                        common.Printf("-------- %5.1f of %5.1f megs --------\n",
                                partialSize / (1024 * 1024.0), totalSize / (1024 * 1024.0));
                    }
                }
            }

            common.Printf("%s", header);
            common.Printf(" %d images (%d total)\n", count, globalImages.images.Num());
            common.Printf(" %5.1f total megabytes of images\n\n\n", totalSize / (1024 * 1024.0));

            if (byClassification) {

                final idList<Integer>[] classifications = new idList[IC_COUNT];

                for (i = 0; i < count; i++) {
                    final int cl = ClassifyImage(sortedArray[i].image.imgName.getData());
                    classifications[cl].Append(i);
                }

                for (i = 0; i < IC_COUNT; i++) {
                    partialSize = 0;
                    final idList< Integer> overSizedList = new idList<>();
                    for (j = 0; j < classifications[i].Num(); j++) {
                        partialSize += sortedArray[ classifications[i].oGet(j)].image.StorageSize();
                        if (overSized) {
                            if ((sortedArray[ classifications[ i].oGet(j)].image.uploadWidth > IC_Info[i].maxWidth) && (sortedArray[ classifications[ i].oGet(j)].image.uploadHeight > IC_Info[i].maxHeight)) {
                                overSizedList.Append(classifications[ i].oGet(j));
                            }
                        }
                    }
                    common.Printf(" Classification %s contains %d images using %5.1f megabytes\n", IC_Info[i].desc, classifications[i].Num(), partialSize / (1024 * 1024.0));
                    if (overSized && (overSizedList.Num() != 0)) {
                        common.Printf("  The following images may be oversized\n");
                        for (j = 0; j < overSizedList.Num(); j++) {
                            common.Printf("    ");
                            sortedArray[ overSizedList.oGet(j)].image.Print();
                            common.Printf("\n");
                        }
                    }
                }
            }

        }
    }

    /*
     ===============
     R_CombineCubeImages_f

     Used to combine animations of six separate tga files into
     a serials of 6x taller tga files, for preparation to roq compress
     ===============
     */
    static class R_CombineCubeImages_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_CombineCubeImages_f();

        private R_CombineCubeImages_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (args.Argc() != 2) {
                common.Printf("usage: combineCubeImages <baseName>\n");
                common.Printf(" combines basename[1-6][0001-9999].tga to basenameCM[0001-9999].tga\n");
                common.Printf(" 1: forward 2:right 3:back 4:left 5:up 6:down\n");
                return;
            }

            final idStr baseName = new idStr(args.Argv(1));
            common.SetRefreshOnPrint(true);

            for (int frameNum = 1; frameNum < 10000; frameNum++) {
//		final char	[]filename=new char[MAX_IMAGE_NAME];
                String filename;
                final ByteBuffer[] pics = new ByteBuffer[6];//Good God!
                final int[] width = {0}, height = {0};
                int side;
                final int[] orderRemap = {1, 3, 4, 2, 5, 6};
                for (side = 0; side < 6; side++) {
                    filename = String.format("%s%d%04i.tga", baseName.getData(), orderRemap[side], frameNum);

                    common.Printf("reading %s\n", filename);
                    pics[side] = R_LoadImage(filename, width, height, null, true);

                    if (NOT(pics[side])) {
                        common.Printf("not found.\n");
                        break;
                    }

                    // convert from "camera" images to native cube map images
                    switch (side) {
                        case 0:	// forward
                            R_RotatePic(pics[side], width[0]);
                            break;
                        case 1:	// back
                            R_RotatePic(pics[side], width[0]);
                            R_HorizontalFlip(pics[side], width[0], height[0]);
                            R_VerticalFlip(pics[side], width[0], height[0]);
                            break;
                        case 2:	// left
                            R_VerticalFlip(pics[side], width[0], height[0]);
                            break;
                        case 3:	// right
                            R_HorizontalFlip(pics[side], width[0], height[0]);
                            break;
                        case 4:	// up
                            R_RotatePic(pics[side], width[0]);
                            break;
                        case 5: // down
                            R_RotatePic(pics[side], width[0]);
                            break;
                    }
                }

                if (side != 6) {
                    for (final int i = 0; i < side; side++) {
                        pics[side] = null;//Mem_Free(pics[side]);

                    }
                    break;
                }

                ByteBuffer combined = ByteBuffer.allocate(width[0] * height[0] * 6 * 4);// Mem_Alloc(width[0] * height[0] * 6 * 4);
                final int length = width[0] * height[0] * 4;
                for (side = 0; side < 6; side++) {
//			memcpy( combined+width*height*4*side, pics[side], width*height*4 );
                    combined.position(length * side);
                    combined.put(pics[side].array(), 0, length);
                    pics[side] = null;//Mem_Free(pics[side]);

                }
                filename = String.format("%sCM%04i.tga", baseName.getData(), frameNum);

                common.Printf("writing %s\n", filename);
                R_WriteTGA(filename, combined, width[0], height[0] * 6);

                combined = null;//Mem_Free(combined);
            }
            common.SetRefreshOnPrint(false);
        }
    }

    /*
     ===============
     R_ReloadImages_f

     Regenerate all images that came directly from files that have changed, so
     any saved changes will show up in place.

     New r_texturesize/r_texturedepth variables will take effect on reload

     reloadImages <all>
     ===============
     */
    static class R_ReloadImages_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ReloadImages_f();

        private R_ReloadImages_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;
            idImage image;
            boolean all;
            boolean checkPrecompressed;

            // this probably isn't necessary...
            globalImages.ChangeTextureFilter();

            all = false;
            checkPrecompressed = false;		// if we are doing this as a vid_restart, look for precompressed like normal

            if (args.Argc() == 2) {
                if (0 == idStr.Icmp(args.Argv(1), "all")) {
                    all = true;
                } else if (0 == idStr.Icmp(args.Argv(1), "reload")) {
                    all = true;
                    checkPrecompressed = true;
                } else {
                    common.Printf("USAGE: reloadImages <all>\n");
                    return;
                }
            }

            for (i = 0; i < globalImages.images.Num(); i++) {
                image = globalImages.images.oGet(i);
                image.Reload(checkPrecompressed, all);
//                System.out.printf("%d:%d\n", i, qglGetError());
            }
        }
    }

    static class sortedImage_t {

        idImage image;
        int size;
    }

    /*
   
     ================
     R_RampImage

     Creates a 0-255 ramp image
     ================
     */
    static class R_RampImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_RampImage();

        private R_RampImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            int x;
            final ByteBuffer data = ByteBuffer.allocate(256 * 4);

            for (x = 0; x < 256; x++) {
                data.putInt(x * 4, x);
            }

            image.GenerateImage(data, 256, 1, TF_NEAREST, false, TR_CLAMP, TD_HIGH_QUALITY);
        }
    }

    /*
     ================
     R_SpecularTableImage

     Creates a ramp that matches our fudged specular calculation
     ================
     */
    static class R_SpecularTableImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_SpecularTableImage();

        private R_SpecularTableImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            int x;
            final ByteBuffer data = ByteBuffer.allocate(256 * 4);

            for (x = 0; x < 256; x++) {
                float f = x / 255.f;
                if (false) {
                    f = (float) Math.pow(f, 16);
                } else {
                    // this is the behavior of the hacked up fragment programs that
                    // can't really do a power function
                    f = (f - 0.75f) * 4;
                    if (f < 0) {
                        f = 0;
                    }
                    f = f * f;
                }
                final int b = (int) (f * 255);

                data.putInt(x * 4, b);//TODO:check whether setting 4 bytes to an int is the same as what we're doing here!
            }

            image.GenerateImage(data, 256, 1, TF_LINEAR, false, TR_CLAMP, TD_HIGH_QUALITY);
        }
    }


    /*
     ================
     R_Specular2DTableImage

     Create a 2D table that calculates ( reflection dot , specularity )
     ================
     */
    static class R_Specular2DTableImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_Specular2DTableImage();

        private R_Specular2DTableImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            int x, y;
            final ByteBuffer data = ByteBuffer.allocate(256 * 256 * 4);

//	memset( data, 0, sizeof( data ) );
            for (x = 0; x < 256; x++) {
                final float f = x / 255.0f;
                for (y = 0; y < 256; y++) {

                    final int b = (int) (Math.pow(f, y) * 255.0f);
                    if (b == 0) {
                        // as soon as b equals zero all remaining values in this column are going to be zero
                        // we early out to avoid pow() underflows
                        break;
                    }

                    data.putInt((y * 4) + (x * 256), b);
                }
            }

            image.GenerateImage(data, 256, 256, TF_LINEAR, false, TR_CLAMP, TD_HIGH_QUALITY);
        }
    }

    /*
     ================
     R_AlphaRampImage

     Creates a 0-255 ramp image
     ================
     */
    static class R_AlphaRampImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_AlphaRampImage();

        private R_AlphaRampImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            int x;
            final ByteBuffer data = ByteBuffer.allocate(256 * 4);

            for (x = 0; x < 256; x++) {
                data.putInt(x * 4, x);
            }

            image.GenerateImage(data, 256, 1, TF_NEAREST, false, TR_CLAMP, TD_HIGH_QUALITY);
        }
    }

    static class R_DefaultImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_DefaultImage();

        private R_DefaultImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            image.MakeDefault();
        }
    }

    static class R_WhiteImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_WhiteImage();

        private R_WhiteImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final ByteBuffer data = ByteBuffer.allocate(DEFAULT_SIZE * DEFAULT_SIZE * 4);

            // solid white texture
//	memset( data, 255, sizeof( data ) );
            Arrays.fill(data.array(), (byte) 255);
            image.GenerateImage(data, DEFAULT_SIZE, DEFAULT_SIZE, TF_DEFAULT, false, TR_REPEAT, TD_DEFAULT);
        }
    }

    static class R_BlackImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_BlackImage();

        private R_BlackImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final ByteBuffer data = ByteBuffer.allocate(DEFAULT_SIZE * DEFAULT_SIZE * 4);

            // solid black texture
//	memset( data, 0, sizeof( data ) );
            image.GenerateImage(data, DEFAULT_SIZE, DEFAULT_SIZE, TF_DEFAULT, false, TR_REPEAT, TD_DEFAULT);
        }
    }
// the size determines how far away from the edge the blocks start fading
    static final int BORDER_CLAMP_SIZE = 32;

    static class R_BorderClampImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_BorderClampImage();

        private R_BorderClampImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final byte[][][] data = new byte[BORDER_CLAMP_SIZE][BORDER_CLAMP_SIZE][4];

            // solid white texture with a single pixel black border
//	memset( data, 255, sizeof( data ) );
            for (int a = 0; a < data[0].length; a++) {
                for (int b = 0; b < data[0][0].length; b++) {
                    data[a][b] = new byte[]{-1, -1, -1, -1};
                }
            }

            for (int i = 0; i < BORDER_CLAMP_SIZE; i++) {
                data[i][0][0]
                        = data[i][0][1]
                        = data[i][0][2]
                        = data[i][0][3]
                        = data[i][BORDER_CLAMP_SIZE - 1][0]
                        = data[i][BORDER_CLAMP_SIZE - 1][1]
                        = data[i][BORDER_CLAMP_SIZE - 1][2]
                        = data[i][BORDER_CLAMP_SIZE - 1][3]
                        = data[0][i][0]
                        = data[0][i][1]
                        = data[0][i][2]
                        = data[0][i][3]
                        = data[BORDER_CLAMP_SIZE - 1][i][0]
                        = data[BORDER_CLAMP_SIZE - 1][i][1]
                        = data[BORDER_CLAMP_SIZE - 1][i][2]
                        = data[BORDER_CLAMP_SIZE - 1][i][3] = 0;
            }

            image.GenerateImage(ByteBuffer.wrap(flatten(data)), BORDER_CLAMP_SIZE, BORDER_CLAMP_SIZE, TF_LINEAR /* TF_NEAREST */, false, TR_CLAMP_TO_BORDER, TD_DEFAULT);

            if (!glConfig.isInitialized) {
                // can't call qglTexParameterfv yet
                return;
            }
            // explicit zero border
            final FloatBuffer color = Nio.newFloatBuffer(4);
//            color[0] = color[1] = color[2] = color[3] = 0.0f;
            qglTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, color);
//            qglTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, 0.0f);
        }
    }

    static class R_RGBA8Image extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_RGBA8Image();

        private R_RGBA8Image() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final ByteBuffer data = ByteBuffer.allocate(DEFAULT_SIZE * DEFAULT_SIZE * 4);

//	memset( data, 0, sizeof( data ) );
            data.put(0, (byte) 16);
            data.put(1, (byte) 32);
            data.put(2, (byte) 48);
            data.put(3, (byte) 96);

            image.GenerateImage(data, DEFAULT_SIZE, DEFAULT_SIZE, TF_DEFAULT, false, TR_REPEAT, TD_HIGH_QUALITY);
        }
    }

    static class R_RGB8Image extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_RGB8Image();

        private R_RGB8Image() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final ByteBuffer data = ByteBuffer.allocate(DEFAULT_SIZE * DEFAULT_SIZE * 4);

//	memset( data, 0, sizeof( data ) );
            data.put(0, (byte) 16);
            data.put(1, (byte) 32);
            data.put(2, (byte) 48);
            data.put(3, (byte) 255);

            image.GenerateImage(data, DEFAULT_SIZE, DEFAULT_SIZE, TF_DEFAULT, false, TR_REPEAT, TD_HIGH_QUALITY);
        }
    }

    static class R_AlphaNotchImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_AlphaNotchImage();

        private R_AlphaNotchImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final ByteBuffer data = ByteBuffer.allocate(2 * 4);

            // this is used for alpha test clip planes
            data.put(0, (byte) 255);
            data.put(1, (byte) 255);
            data.put(2, (byte) 255);
            data.put(3, (byte) 0);

            data.put(4, (byte) 255);
            data.put(5, (byte) 255);
            data.put(6, (byte) 255);
            data.put(7, (byte) 255);

            image.GenerateImage(data, 2, 1, TF_NEAREST, false, TR_CLAMP, TD_HIGH_QUALITY);
        }
    }

    static class R_FlatNormalImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_FlatNormalImage();

        private R_FlatNormalImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final byte[][][] data = new byte[DEFAULT_SIZE][DEFAULT_SIZE][4];
            int i;

            final int red = (globalImages.image_useNormalCompression.GetInteger() == 1) ? 0 : 3;
            final int alpha = (red == 0) ? 3 : 0;
            // flat normal map for default bunp mapping
            for (i = 0; i < 4; i++) {
                data[0][i][red] = (byte) 128;
                data[0][i][1] = (byte) 128;
                data[0][i][2] = (byte) 255;
                data[0][i][alpha] = (byte) 255;
            }
            image.GenerateImage(ByteBuffer.wrap(flatten(data)), 2, 2, TF_DEFAULT, true, TR_REPEAT, TD_HIGH_QUALITY);
        }
    }

    static class R_AmbientNormalImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_AmbientNormalImage();

        private R_AmbientNormalImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
//            final byte[][][] data = new byte[DEFAULT_SIZE][DEFAULT_SIZE][4];
            final byte[] data = new byte[DEFAULT_SIZE];
            int i;

            final int red = (globalImages.image_useNormalCompression.GetInteger() == 1) ? 0 : 3;
            final int alpha = (red == 0) ? 3 : 0;
            // flat normal map for default bunp mapping
            for (i = 0; i < DEFAULT_SIZE; i += 4) {
                data[i + red] = (byte) (255 * tr.ambientLightVector.oGet(0));
                data[i + 1] = (byte) (255 * tr.ambientLightVector.oGet(1));
                data[i + 2] = (byte) (255 * tr.ambientLightVector.oGet(2));
                data[i + alpha] = (byte) 255;
            }
            final ByteBuffer[] pics = new ByteBuffer[6];
            for (i = 0; i < 6; i++) {
                pics[i] = wrapToNativeBuffer(data);//TODO: wtf does this data[0][0] do?
            }
            // this must be a cube map for fragment programs to simply substitute for the normalization cube map
            image.GenerateCubeImage(pics, 2, TF_DEFAULT, true, TD_HIGH_QUALITY);
        }
    }
    static final int NORMAL_MAP_SIZE = 32;

    /**
     * * NORMALIZATION CUBE MAP CONSTRUCTION **
     */

    /* Given a cube map face index, cube map size, and integer 2D face position,
     * return the cooresponding normalized vector.
     */
    static void getCubeVector(int i, int cubesize, int x, int y, float[] vector) {
        float s, t, sc, tc, mag;

        s = (x + 0.5f) / cubesize;
        t = (y + 0.5f) / cubesize;
        sc = (s * 2.0f) - 1.0f;
        tc = (t * 2.0f) - 1.0f;

        switch (i) {
            case 0:
                vector[0] = 1.0f;
                vector[1] = -tc;
                vector[2] = -sc;
                break;
            case 1:
                vector[0] = -1.0f;
                vector[1] = -tc;
                vector[2] = sc;
                break;
            case 2:
                vector[0] = sc;
                vector[1] = 1.0f;
                vector[2] = tc;
                break;
            case 3:
                vector[0] = sc;
                vector[1] = -1.0f;
                vector[2] = -tc;
                break;
            case 4:
                vector[0] = sc;
                vector[1] = -tc;
                vector[2] = 1.0f;
                break;
            case 5:
                vector[0] = -sc;
                vector[1] = -tc;
                vector[2] = -1.0f;
                break;
        }

        mag = idMath.InvSqrt((vector[0] * vector[0]) + (vector[1] * vector[1]) + (vector[2] * vector[2]));
        vector[0] *= mag;
        vector[1] *= mag;
        vector[2] *= mag;
    }


    /* Initialize a cube map texture object that generates RGB values
     * that when expanded to a [-1,1] range in the register combiners
     * form a normalized vector matching the per-pixel vector used to
     * access the cube map.
     */
    static class makeNormalizeVectorCubeMap extends GeneratorFunction {

        private static final GeneratorFunction instance = new makeNormalizeVectorCubeMap();

        private makeNormalizeVectorCubeMap() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            final float[] vector = new float[3];
            int i, x, y;
            final ByteBuffer[] pixels = new ByteBuffer[6];//[size*size*4*6];
            int size;

            size = NORMAL_MAP_SIZE;

//	pixels[0] = (GLubyte[]) Mem_Alloc(size*size*4*6);
            for (i = 0; i < 6; i++) {
                pixels[i] = Nio.newByteBuffer(size * size * 4);
                for (y = 0; y < size; y++) {
                    for (x = 0; x < size; x++) {
                        getCubeVector(i, size, x, y, vector);
                        pixels[i].put((4 * ((y * size) + x)) + 0, (byte) (128 + (127 * vector[0])));
                        pixels[i].put((4 * ((y * size) + x)) + 1, (byte) (128 + (127 * vector[1])));
                        pixels[i].put((4 * ((y * size) + x)) + 2, (byte) (128 + (127 * vector[2])));
                        pixels[i].put((4 * ((y * size) + x)) + 3, (byte) 255);
                    }
                }
            }

            image.GenerateCubeImage(pixels, size, TF_LINEAR, false, TD_HIGH_QUALITY);

//            Mem_Free(pixels[0]);
        }
    }

    /*
     ================
     R_CreateNoFalloffImage

     This is a solid white texture that is zero clamped.
     ================
     */
    static class R_CreateNoFalloffImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_CreateNoFalloffImage();

        private R_CreateNoFalloffImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            int x, y;
            final byte[][][] data = new byte[16][FALLOFF_TEXTURE_SIZE][4];

//	memset( data, 0, sizeof( data ) );
            for (x = 1; x < (FALLOFF_TEXTURE_SIZE - 1); x++) {
                for (y = 1; y < 15; y++) {
                    data[y][x][0] = (byte) 255;
                    data[y][x][1] = (byte) 255;
                    data[y][x][2] = (byte) 255;
                    data[y][x][3] = (byte) 255;
                }
            }
            image.GenerateImage(ByteBuffer.wrap(flatten(data)), FALLOFF_TEXTURE_SIZE, 16,
                    TF_DEFAULT, false, TR_CLAMP_TO_ZERO, TD_HIGH_QUALITY);
        }
    }
    /*
     ================
     R_FogImage

     We calculate distance correctly in two planes, but the
     third will still be projection based
     ================
     */
    static final int FOG_SIZE = 128;

    static class R_FogImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_FogImage();

        private R_FogImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            int x, y;
            final byte[][][] data = new byte[FOG_SIZE][FOG_SIZE][4];
            int b;

            final float[] step = new float[256];
            int i;
            float remaining = 1.0f;
            for (i = 0; i < 256; i++) {
                step[i] = remaining;
                remaining *= 0.982f;
            }

            for (x = 0; x < FOG_SIZE; x++) {
                for (y = 0; y < FOG_SIZE; y++) {
                    float d;

                    d = idMath.Sqrt(((x - (FOG_SIZE / 2)) * (x - (FOG_SIZE / 2)))
                            + ((y - (FOG_SIZE / 2)) * (y - (FOG_SIZE / 2))));
                    d /= (FOG_SIZE / 2) - 1;

                    b = (byte) (d * 255);
                    if (b <= 0) {
                        b = 0;
                    } else if (b > 255) {
                        b = 255;
                    }
                    b = (byte) (255 * (1.0 - step[b]));
                    if ((x == 0) || (x == (FOG_SIZE - 1)) || (y == 0) || (y == (FOG_SIZE - 1))) {
                        b = 255;		// avoid clamping issues
                    }
                    data[y][x][0]
                            = data[y][x][1]
                            = data[y][x][2] = (byte) 255;
                    data[y][x][3] = (byte) b;
                }
            }

            image.GenerateImage(ByteBuffer.wrap(flatten(data)), FOG_SIZE, FOG_SIZE,
                    TF_LINEAR, false, TR_CLAMP, TD_HIGH_QUALITY);
        }
    }
    /*
     ================
     FogFraction

     Height values below zero are inside the fog volume
     ================
     */
    static final float RAMP_RANGE = 8;
    static final float DEEP_RANGE = -30;

    static float FogFraction(float viewHeight, float targetHeight) {
        final float total = idMath.Fabs(targetHeight - viewHeight);

//	return targetHeight >= 0 ? 0 : 1.0;
        // only ranges that cross the ramp range are special
        if ((targetHeight > 0) && (viewHeight > 0)) {
            return 0.0f;
        }
        if ((targetHeight < -RAMP_RANGE) && (viewHeight < -RAMP_RANGE)) {
            return 1.0f;
        }

        float above;
        if (targetHeight > 0) {
            above = targetHeight;
        } else if (viewHeight > 0) {
            above = viewHeight;
        } else {
            above = 0;
        }

        float rampTop, rampBottom;

        if (viewHeight > targetHeight) {
            rampTop = viewHeight;
            rampBottom = targetHeight;
        } else {
            rampTop = targetHeight;
            rampBottom = viewHeight;
        }
        if (rampTop > 0) {
            rampTop = 0;
        }
        if (rampBottom < -RAMP_RANGE) {
            rampBottom = -RAMP_RANGE;
        }

        final float rampSlope = 1.0f / RAMP_RANGE;

        if (0.0f == total) {
            return -viewHeight * rampSlope;
        }

        final float ramp = (1.0f - (((rampTop * rampSlope) + (rampBottom * rampSlope)) * -0.5f)) * (rampTop - rampBottom);

        float frac = (total - above - ramp) / total;

        // after it gets moderately deep, always use full value
        final float deepest = viewHeight < targetHeight ? viewHeight : targetHeight;

        final float deepFrac = deepest / DEEP_RANGE;
        if (deepFrac >= 1.0) {
            return 1.0f;
        }

        frac = (frac * (1.0f - deepFrac)) + deepFrac;

        return frac;
    }

    /*
     ================
     R_FogEnterImage

     Modulate the fog alpha density based on the distance of the
     start and end points to the terminator plane
     ================
     */
    static class R_FogEnterImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_FogEnterImage();

        private R_FogEnterImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            int x, y;
            final byte[][][] data = new byte[FOG_ENTER_SIZE][FOG_ENTER_SIZE][4];
            int b;

            for (x = 0; x < FOG_ENTER_SIZE; x++) {
                for (y = 0; y < FOG_ENTER_SIZE; y++) {
                    float d;

                    d = FogFraction(x - (FOG_ENTER_SIZE / 2), y - (FOG_ENTER_SIZE / 2));

                    b = (byte) (d * 255);
                    if (b <= 0) {
                        b = 0;
                    } else if (b > 255) {
                        b = 255;
                    }
                    data[y][x][0]
                            = data[y][x][1]
                            = data[y][x][2] = (byte) 255;
                    data[y][x][3] = (byte) b;
                }
            }

            // if mipmapped, acutely viewed surfaces fade wrong
            image.GenerateImage(ByteBuffer.wrap(flatten(data)), FOG_ENTER_SIZE, FOG_ENTER_SIZE,
                    TF_LINEAR, false, TR_CLAMP, TD_HIGH_QUALITY);
        }
    }
    /*
     ================
     R_QuadraticImage

     ================
     */
    static final int QUADRATIC_WIDTH = 32;
    static final int QUADRATIC_HEIGHT = 4;

    static class R_QuadraticImage extends GeneratorFunction {

        private static final GeneratorFunction instance = new R_QuadraticImage();

        private R_QuadraticImage() {
        }

        public static GeneratorFunction getInstance() {
            return instance;
        }

        @Override
        public void run(idImage image) {
            int x, y;
            final byte[][][] data = new byte[QUADRATIC_HEIGHT][QUADRATIC_WIDTH][4];
            int b;

            for (x = 0; x < QUADRATIC_WIDTH; x++) {
                for (y = 0; y < QUADRATIC_HEIGHT; y++) {
                    float d;

                    d = x - ((QUADRATIC_WIDTH / 2) - 0.5f);
                    d = idMath.Fabs(d);
                    d -= 0.5f;
                    d /= QUADRATIC_WIDTH / 2;

                    d = 1.0f - d;
                    d = d * d;

                    b = (byte) (d * 255);
                    if (b <= 0) {
                        b = 0;
                    } else if (b > 255) {
                        b = 255;
                    }
                    data[y][x][0]
                            = data[y][x][1]
                            = data[y][x][2] = (byte) b;
                    data[y][x][3] = (byte) 255;
                }
            }

            image.GenerateImage(ByteBuffer.wrap(flatten(data)), QUADRATIC_WIDTH, QUADRATIC_HEIGHT,
                    TF_DEFAULT, false, TR_CLAMP, TD_HIGH_QUALITY);
        }
    }

    /*
     =======================
     R_QsortImageSizes

     =======================
     */
    static class R_QsortImageSizes implements cmp_t<sortedImage_t> {

        @Override
        public int compare(final sortedImage_t ea, final sortedImage_t eb) {

            if (ea.size > eb.size) {
                return -1;
            }
            if (ea.size < eb.size) {
                return 1;
            }
            return idStr.Icmp(ea.image.imgName, eb.image.imgName);
        }
    }
}
