package neo.Renderer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import neo.Renderer.Cinematic.idCinematic;
import neo.Renderer.Cinematic.idSndWindow;
import static neo.Renderer.Image.MAX_IMAGE_NAME;
import neo.Renderer.Image.cubeFiles_t;
import static neo.Renderer.Image.cubeFiles_t.CF_2D;
import static neo.Renderer.Image.cubeFiles_t.CF_CAMERA;
import static neo.Renderer.Image.cubeFiles_t.CF_NATIVE;
import static neo.Renderer.Image.globalImages;
import neo.Renderer.Image.idImage;
import neo.Renderer.Image.textureDepth_t;
import static neo.Renderer.Image.textureDepth_t.TD_BUMP;
import static neo.Renderer.Image.textureDepth_t.TD_DEFAULT;
import static neo.Renderer.Image.textureDepth_t.TD_DIFFUSE;
import static neo.Renderer.Image.textureDepth_t.TD_HIGH_QUALITY;
import static neo.Renderer.Image.textureDepth_t.TD_SPECULAR;
import static neo.Renderer.Image_program.R_ParsePastImageProgram;
import static neo.Renderer.Material.cullType_t.CT_BACK_SIDED;
import static neo.Renderer.Material.cullType_t.CT_FRONT_SIDED;
import static neo.Renderer.Material.cullType_t.CT_TWO_SIDED;
import static neo.Renderer.Material.deform_t.DFRM_EXPAND;
import static neo.Renderer.Material.deform_t.DFRM_EYEBALL;
import static neo.Renderer.Material.deform_t.DFRM_FLARE;
import static neo.Renderer.Material.deform_t.DFRM_MOVE;
import static neo.Renderer.Material.deform_t.DFRM_NONE;
import static neo.Renderer.Material.deform_t.DFRM_PARTICLE;
import static neo.Renderer.Material.deform_t.DFRM_PARTICLE2;
import static neo.Renderer.Material.deform_t.DFRM_SPRITE;
import static neo.Renderer.Material.deform_t.DFRM_TUBE;
import static neo.Renderer.Material.deform_t.DFRM_TURB;
import static neo.Renderer.Material.dynamicidImage_t.DI_MIRROR_RENDER;
import static neo.Renderer.Material.dynamicidImage_t.DI_REMOTE_RENDER;
import static neo.Renderer.Material.dynamicidImage_t.DI_XRAY_RENDER;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_ADD;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_AND;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_DIVIDE;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_EQ;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_GE;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_GT;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_LE;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_LT;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_MOD;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_MULTIPLY;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_NE;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_OR;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_SOUND;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_SUBTRACT;
import static neo.Renderer.Material.expOpType_t.OP_TYPE_TABLE;
import neo.Renderer.Material.expOp_t;
import static neo.Renderer.Material.expRegister_t.EXP_REG_GLOBAL0;
import static neo.Renderer.Material.expRegister_t.EXP_REG_GLOBAL1;
import static neo.Renderer.Material.expRegister_t.EXP_REG_GLOBAL2;
import static neo.Renderer.Material.expRegister_t.EXP_REG_GLOBAL3;
import static neo.Renderer.Material.expRegister_t.EXP_REG_GLOBAL4;
import static neo.Renderer.Material.expRegister_t.EXP_REG_GLOBAL5;
import static neo.Renderer.Material.expRegister_t.EXP_REG_GLOBAL6;
import static neo.Renderer.Material.expRegister_t.EXP_REG_GLOBAL7;
import static neo.Renderer.Material.expRegister_t.EXP_REG_NUM_PREDEFINED;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM0;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM1;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM10;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM11;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM2;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM3;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM4;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM5;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM6;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM7;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM8;
import static neo.Renderer.Material.expRegister_t.EXP_REG_PARM9;
import static neo.Renderer.Material.expRegister_t.EXP_REG_TIME;
import neo.Renderer.Material.idMaterial;
import static neo.Renderer.Material.materialCoverage_t.MC_BAD;
import static neo.Renderer.Material.materialCoverage_t.MC_OPAQUE;
import static neo.Renderer.Material.materialCoverage_t.MC_PERFORATED;
import static neo.Renderer.Material.materialCoverage_t.MC_TRANSLUCENT;
import neo.Renderer.Material.newShaderStage_t;
import neo.Renderer.Material.shaderStage_t;
import static neo.Renderer.Material.stageLighting_t.SL_AMBIENT;
import static neo.Renderer.Material.stageLighting_t.SL_BUMP;
import static neo.Renderer.Material.stageLighting_t.SL_DIFFUSE;
import static neo.Renderer.Material.stageLighting_t.SL_SPECULAR;
import static neo.Renderer.Material.stageVertexColor_t.SVC_INVERSE_MODULATE;
import static neo.Renderer.Material.stageVertexColor_t.SVC_MODULATE;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_10;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_11;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_12;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_13;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_14;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_15;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_CARDBOARD;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_FLESH;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_GLASS;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_LIQUID;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_METAL;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_NONE;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_PLASTIC;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_RICOCHET;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_STONE;
import static neo.Renderer.Material.surfTypes_t.SURFTYPE_WOOD;
import static neo.Renderer.Material.texgen_t.TG_DIFFUSE_CUBE;
import static neo.Renderer.Material.texgen_t.TG_EXPLICIT;
import static neo.Renderer.Material.texgen_t.TG_GLASSWARP;
import static neo.Renderer.Material.texgen_t.TG_REFLECT_CUBE;
import static neo.Renderer.Material.texgen_t.TG_SCREEN;
import static neo.Renderer.Material.texgen_t.TG_SCREEN2;
import static neo.Renderer.Material.texgen_t.TG_SKYBOX_CUBE;
import static neo.Renderer.Material.texgen_t.TG_WOBBLESKY_CUBE;
import neo.Renderer.Material.textureFilter_t;
import static neo.Renderer.Material.textureFilter_t.TF_DEFAULT;
import static neo.Renderer.Material.textureFilter_t.TF_LINEAR;
import static neo.Renderer.Material.textureFilter_t.TF_NEAREST;
import neo.Renderer.Material.textureRepeat_t;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP_TO_ZERO;
import static neo.Renderer.Material.textureRepeat_t.TR_CLAMP_TO_ZERO_ALPHA;
import static neo.Renderer.Material.textureRepeat_t.TR_REPEAT;
import neo.Renderer.MegaTexture.idMegaTexture;
import static neo.Renderer.draw_arb2.R_FindARBProgram;
import static neo.Renderer.tr_local.GLS_ALPHAMASK;
import static neo.Renderer.tr_local.GLS_BLUEMASK;
import static neo.Renderer.tr_local.GLS_COLORMASK;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_EQUAL;
import static neo.Renderer.tr_local.GLS_DEPTHFUNC_LESS;
import static neo.Renderer.tr_local.GLS_DEPTHMASK;
import static neo.Renderer.tr_local.GLS_DSTBLEND_BITS;
import static neo.Renderer.tr_local.GLS_DSTBLEND_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE_MINUS_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ONE_MINUS_SRC_COLOR;
import static neo.Renderer.tr_local.GLS_DSTBLEND_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_DSTBLEND_SRC_COLOR;
import static neo.Renderer.tr_local.GLS_DSTBLEND_ZERO;
import static neo.Renderer.tr_local.GLS_GREENMASK;
import static neo.Renderer.tr_local.GLS_REDMASK;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ALPHA_SATURATE;
import static neo.Renderer.tr_local.GLS_SRCBLEND_BITS;
import static neo.Renderer.tr_local.GLS_SRCBLEND_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_DST_COLOR;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE_MINUS_DST_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE_MINUS_DST_COLOR;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ONE_MINUS_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_SRC_ALPHA;
import static neo.Renderer.tr_local.GLS_SRCBLEND_ZERO;
import static neo.Renderer.tr_local.backEnd;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import neo.Renderer.tr_local.viewDef_s;
import neo.Sound.sound.idSoundEmitter;
import neo.TempDump;
import neo.TempDump.CPP_class;
import neo.TempDump.CPP_class.Bool;
import neo.TempDump.CPP_class.Pointer;
import static neo.TempDump.NOT;
import static neo.TempDump.atoi;
import static neo.TempDump.ctos;
import static neo.TempDump.etoi;
import static neo.TempDump.strLen;
import static neo.framework.CVarSystem.cvarSystem;
import static neo.framework.Common.common;
import static neo.framework.DeclManager.DECL_LEXER_FLAGS;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_PARTICLE;
import static neo.framework.DeclManager.declType_t.DECL_TABLE;
import neo.framework.DeclManager.idDecl;
import neo.framework.DeclTable.idDeclTable;
import static neo.idlib.Lib.BIT;
import static neo.idlib.Text.Lexer.LEXFL_ALLOWPATHNAMES;
import static neo.idlib.Text.Lexer.LEXFL_NOFATALERRORS;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGCONCAT;
import static neo.idlib.Text.Lexer.LEXFL_NOSTRINGESCAPECHARS;
import neo.idlib.Text.Lexer.idLexer;
import neo.idlib.Text.Str.idStr;
import static neo.idlib.Text.Token.TT_NUMBER;
import neo.idlib.Text.Token.idToken;
import neo.idlib.containers.List.idList;
import static neo.idlib.precompiled.MAX_EXPRESSION_OPS;
import static neo.idlib.precompiled.MAX_EXPRESSION_REGISTERS;
import neo.ui.UserInterface.idUserInterface;
import static neo.ui.UserInterface.uiManager;
import static org.lwjgl.opengl.ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB;
import static org.lwjgl.opengl.ARBVertexProgram.GL_VERTEX_PROGRAM_ARB;

/*
 ===============================================================================

 Material

 ===============================================================================
 */
public class Material {

    // moved from image.h for default parm
    enum textureFilter_t {

        TF_LINEAR,
        TF_NEAREST,
        TF_DEFAULT			// use the user-specified r_textureFilter
    };

    enum textureRepeat_t {

        TR_REPEAT,
        TR_CLAMP,
        TR_CLAMP_TO_BORDER, // this should replace TR_CLAMP_TO_ZERO and TR_CLAMP_TO_ZERO_ALPHA, but I don't want to risk changing it right now
        //        
        TR_CLAMP_TO_ZERO, // guarantee 0,0,0,255 edge for projected textures, set AFTER image format selection
        //        
        TR_CLAMP_TO_ZERO_ALPHA	// guarantee 0 alpha edge for projected textures, set AFTER image format selection
    };

    static class decalInfo_t {

        public static final transient int SIZE
                = Integer.SIZE
                + Integer.SIZE
                + (Float.SIZE * 4)
                + (Float.SIZE * 4);

        int stayTime;                           // msec for no change
        int fadeTime;                           // msec to fade vertex colors over
        final float[] start = new float[4];	// vertex color at spawn (possibly out of 0.0 - 1.0 range, will clamp after calc)
        final float[] end = new float[4];	// vertex color at fade-out (possibly out of 0.0 - 1.0 range, will clamp after calc)
    };

    enum deform_t {

        DFRM_NONE,
        DFRM_SPRITE,
        DFRM_TUBE,
        DFRM_FLARE,
        DFRM_EXPAND,
        DFRM_MOVE,
        DFRM_EYEBALL,
        DFRM_PARTICLE,
        DFRM_PARTICLE2,
        DFRM_TURB
    };

    enum dynamicidImage_t {

        DI_STATIC,
        DI_SCRATCH, // video, screen wipe, etc
        DI_CUBE_RENDER,
        DI_MIRROR_RENDER,
        DI_XRAY_RENDER,
        DI_REMOTE_RENDER
    };

// note: keep opNames[] in sync with changes
    enum expOpType_t {

        OP_TYPE_ADD,
        OP_TYPE_SUBTRACT,
        OP_TYPE_MULTIPLY,
        OP_TYPE_DIVIDE,
        OP_TYPE_MOD,
        OP_TYPE_TABLE,
        OP_TYPE_GT,
        OP_TYPE_GE,
        OP_TYPE_LT,
        OP_TYPE_LE,
        OP_TYPE_EQ,
        OP_TYPE_NE,
        OP_TYPE_AND,
        OP_TYPE_OR,
        OP_TYPE_SOUND
    };

    @Deprecated
    static final String opNames[] = {
        "OP_TYPE_ADD",
        "OP_TYPE_SUBTRACT",
        "OP_TYPE_MULTIPLY",
        "OP_TYPE_DIVIDE",
        "OP_TYPE_MOD",
        "OP_TYPE_TABLE",
        "OP_TYPE_GT",
        "OP_TYPE_GE",
        "OP_TYPE_LT",
        "OP_TYPE_LE",
        "OP_TYPE_EQ",
        "OP_TYPE_NE",
        "OP_TYPE_AND",
        "OP_TYPE_OR"
    };

    enum expRegister_t {

        EXP_REG_TIME,
        //
        EXP_REG_PARM0,
        EXP_REG_PARM1,
        EXP_REG_PARM2,
        EXP_REG_PARM3,
        EXP_REG_PARM4,
        EXP_REG_PARM5,
        EXP_REG_PARM6,
        EXP_REG_PARM7,
        EXP_REG_PARM8,
        EXP_REG_PARM9,
        EXP_REG_PARM10,
        EXP_REG_PARM11,
        //
        EXP_REG_GLOBAL0,
        EXP_REG_GLOBAL1,
        EXP_REG_GLOBAL2,
        EXP_REG_GLOBAL3,
        EXP_REG_GLOBAL4,
        EXP_REG_GLOBAL5,
        EXP_REG_GLOBAL6,
        EXP_REG_GLOBAL7,
        //
        EXP_REG_NUM_PREDEFINED
    };

    static class expOp_t {

        public static final transient int SIZE
                = CPP_class.Enum.SIZE
                + (Integer.SIZE * 3);

        expOpType_t opType;
        int a, b, c;

        public expOp_t() {
        }

        private expOp_t(expOp_t op) {
            this.opType = op.opType;
            this.a = op.a;
            this.b = op.b;
            this.c = op.c;
        }
    };

    static class colorStage_t {

        public static final transient int SIZE = 4 * Integer.SIZE;

        final int[] registers = new int[4];

        public colorStage_t() {
        }

        private colorStage_t(colorStage_t color) {
            System.arraycopy(color.registers, 0, this.registers, 0, this.registers.length);
        }
    };

    enum texgen_t {

        TG_EXPLICIT,
        TG_DIFFUSE_CUBE,
        TG_REFLECT_CUBE,
        TG_SKYBOX_CUBE,
        TG_WOBBLESKY_CUBE,
        TG_SCREEN, // screen aligned, for mirrorRenders and screen space temporaries
        TG_SCREEN2,
        TG_GLASSWARP
    };

    public static class textureStage_t {

        public static final transient int SIZE
                = Pointer.SIZE//idCinematic
                + idImage.SIZE
                + CPP_class.Enum.SIZE//texgen_t
                + Bool.SIZE
                + (Integer.SIZE * 2 * 3)
                + CPP_class.Enum.SIZE//dynamicidImage_t
                + (Integer.SIZE * 2)
                + Integer.SIZE;

        public final idCinematic[] cinematic;
        public final idImage[] image;
        public texgen_t texgen = texgen_t.values()[0];
        public boolean hasMatrix;
        public final int[][] matrix = new int[2][3];	// we only allow a subset of the full projection matrix
        // dynamic image variables
        public dynamicidImage_t dynamic = dynamicidImage_t.values()[0];
        public int width, height;
        public int dynamicFrameCount;

        private static int blaCounter = 0;

        public textureStage_t() {

            blaCounter++;
            this.cinematic = new idCinematic[1];
            this.image = new idImage[1];
        }

        private textureStage_t(textureStage_t texture) {
            this.cinematic = texture.cinematic;//pointer
            this.image = texture.image;//pointer
            this.hasMatrix = texture.hasMatrix;
            System.arraycopy(texture.matrix[0], 0, this.matrix[0], 0, this.matrix[0].length);
            System.arraycopy(texture.matrix[1], 0, this.matrix[1], 0, this.matrix[1].length);
            this.dynamic = texture.dynamic;
            this.width = texture.width;
            this.height = texture.height;
            this.dynamicFrameCount = texture.dynamicFrameCount;
        }
    };

// the order BUMP / DIFFUSE / SPECULAR is necessary for interactions to draw correctly on low end cards
    enum stageLighting_t {

        SL_AMBIENT, // execute after lighting
        SL_BUMP,
        SL_DIFFUSE,
        SL_SPECULAR
    };

// cross-blended terrain textures need to modulate the color by
// the vertex color to smoothly blend between two textures
    enum stageVertexColor_t {

        SVC_IGNORE,
        SVC_MODULATE,
        SVC_INVERSE_MODULATE
    };
    static final int MAX_FRAGMENT_IMAGES = 8;
    static final int MAX_VERTEX_PARMS = 4;

    static class newShaderStage_t {

        public static final transient int SIZE
                = Integer.SIZE
                + Integer.SIZE
                + (Integer.SIZE * MAX_VERTEX_PARMS * 4)
                + Integer.SIZE
                + Integer.SIZE
                + (idImage.SIZE * MAX_FRAGMENT_IMAGES)//TODO:pointer
                + idMegaTexture.SIZE;

        int vertexProgram;
        int numVertexParms;
        final int[][] vertexParms = new int[MAX_VERTEX_PARMS][4];	// evaluated register indexes
        int fragmentProgram;
        int numFragmentProgramImages;
        idImage[] fragmentProgramImages = new idImage[MAX_FRAGMENT_IMAGES];
        idMegaTexture megaTexture;      // handles all the binding and parameter setting 
    };

    public static class shaderStage_t {

        public static final transient int SIZE
                = Integer.SIZE
                + Pointer.SIZE//stageLighting_t
                + Integer.SIZE
                + colorStage_t.SIZE
                + Integer.SIZE
                + Bool.SIZE
                + Integer.SIZE
                + textureStage_t.SIZE
                + Pointer.SIZE//stageVertexColor_t
                + Bool.SIZE
                + Float.SIZE
                + Pointer.SIZE;//newShaderStage_t

        int conditionRegister;          // if registers[conditionRegister] == 0, skip stage
        stageLighting_t lighting;	// determines which passes interact with lights
        int drawStateBits;
        colorStage_t color;
        boolean hasAlphaTest;
        int alphaTestRegister;
        public final textureStage_t texture;
        stageVertexColor_t vertexColor = stageVertexColor_t.values()[0];
        boolean ignoreAlphaTest;	// this stage should act as translucent, even if the surface is alpha tested
//        
        float privatePolygonOffset;	// a per-stage polygon offset
//
        newShaderStage_t newStage;	// vertex / fragment program based stage

        public shaderStage_t() {
            this.lighting = stageLighting_t.values()[0];
            this.color = new colorStage_t();
            this.texture = new textureStage_t();
        }

        /**
         * copy constructor
         *
         * @param shader
         */
        private shaderStage_t(shaderStage_t shader) {

            this.conditionRegister = shader.conditionRegister;
            this.lighting = shader.lighting;
            this.drawStateBits = shader.drawStateBits;
            this.color = new colorStage_t(shader.color);
            this.hasAlphaTest = shader.hasAlphaTest;
            this.alphaTestRegister = shader.alphaTestRegister;
            this.texture = new textureStage_t(shader.texture);
            this.vertexColor = shader.vertexColor;
            this.ignoreAlphaTest = shader.ignoreAlphaTest;
            this.privatePolygonOffset = shader.privatePolygonOffset;
            this.newStage = shader.newStage;//pointer
        }

    };

    public enum materialCoverage_t {

        MC_BAD,
        MC_OPAQUE, // completely fills the triangle, will have black drawn on fillDepthBuffer
        MC_PERFORATED, // may have alpha tested holes
        MC_TRANSLUCENT  	// blended with background
    };
    //typedef enum {
    static final        int SS_SUBVIEW        = -3;             // mirrors, viewscreens, etc
    public static final int SS_GUI            = -2;             // guis
    static final        int SS_BAD            = -1;
    static final        int SS_OPAQUE         = 0;              // opaque
    //
    static final        int SS_PORTAL_SKY     = 1;
    static final        int SS_DECAL          = 2;              // scorch marks, etc.
    //
    static final        int SS_FAR            = 3;
    static final        int SS_MEDIUM         = 4;              // normal translucent
    static final        int SS_CLOSE          = 5;
    //
    static final        int SS_ALMOST_NEAREST = 6;              // gun smoke puffs
    //
    static final        int SS_NEAREST        = 7;              // screen blood blobs
    //
    static final        int SS_POST_PROCESS   = 100;            // after a screen copy to texture
//} materialSort_t;

    public enum cullType_t {

        CT_FRONT_SIDED,
        CT_BACK_SIDED,
        CT_TWO_SIDED
    };
    // these don't effect per-material storage, so they can be very large
    static final        int MAX_SHADER_STAGES           = 256;
    //
    static final        int MAX_TEXGEN_REGISTERS        = 4;
    //
    public static final int MAX_ENTITY_SHADER_PARMS     = 12;
    //
//    
    // material flags
//typedef enum {
    public static final int MF_DEFAULTED                = BIT(0);
    public static final int MF_POLYGONOFFSET            = BIT(1);
    public static final int MF_NOSHADOWS                = BIT(2);
    public static final int MF_FORCESHADOWS             = BIT(3);
    public static final int MF_NOSELFSHADOW             = BIT(4);
    public static final int MF_NOPORTALFOG              = BIT(5);   // this fog volume won't ever consider a portal fogged out
    public static final int MF_EDITOR_VISIBLE           = BIT(6);   // in use (visible) per editor
    //} materialFlags_t;
//    
//    
    // contents flags; NOTE: make sure to keep the defines in doom_defs.script up to date with these!
// typedef enum {
    public static final int CONTENTS_SOLID              = BIT(0);   // an eye is never valid in a solid
    public static final int CONTENTS_OPAQUE             = BIT(1);   // blocks visibility (for ai)
    public static final int CONTENTS_WATER              = BIT(2);   // used for water
    public static final int CONTENTS_PLAYERCLIP         = BIT(3);   // solid to players
    public static final int CONTENTS_MONSTERCLIP        = BIT(4);   // solid to monsters
    public static final int CONTENTS_MOVEABLECLIP       = BIT(5);   // solid to moveable entities
    public static final int CONTENTS_IKCLIP             = BIT(6);   // solid to IK
    public static final int CONTENTS_BLOOD              = BIT(7);   // used to detect blood decals
    public static final int CONTENTS_BODY               = BIT(8);   // used for actors
    public static final int CONTENTS_PROJECTILE         = BIT(9);   // used for projectiles
    public static final int CONTENTS_CORPSE             = BIT(10);  // used for dead bodies
    public static final int CONTENTS_RENDERMODEL        = BIT(11);  // used for render models for collision detection
    public static final int CONTENTS_TRIGGER            = BIT(12);  // used for triggers
    public static final int CONTENTS_AAS_SOLID          = BIT(13);  // solid for AAS
    public static final int CONTENTS_AAS_OBSTACLE       = BIT(14);  // used to compile an obstacle into AAS that can be enabled/disabled
    public static final int CONTENTS_FLASHLIGHT_TRIGGER = BIT(15);  // used for triggers that are activated by the flashlight
    //
    // contents used by utils
    public static final int CONTENTS_AREAPORTAL         = BIT(20);  // portal separating renderer areas
    public static final int CONTENTS_NOCSG              = BIT(21);  // don't cut this brush with CSG operations in the editor
    //
    public static final int CONTENTS_REMOVE_UTIL        = ~(CONTENTS_AREAPORTAL | CONTENTS_NOCSG);
    // } contentsFlags_t;
//    
    // surface types
    public static final int NUM_SURFACE_BITS            = 4;
    public static final int MAX_SURFACE_TYPES           = 1 << NUM_SURFACE_BITS;

    public enum surfTypes_t {

        SURFTYPE_NONE, // default type
        SURFTYPE_METAL,
        SURFTYPE_STONE,
        SURFTYPE_FLESH,
        SURFTYPE_WOOD,
        SURFTYPE_CARDBOARD,
        SURFTYPE_LIQUID,
        SURFTYPE_GLASS,
        SURFTYPE_PLASTIC,
        SURFTYPE_RICOCHET,
        SURFTYPE_10,
        SURFTYPE_11,
        SURFTYPE_12,
        SURFTYPE_13,
        SURFTYPE_14,
        SURFTYPE_15
    };
    //
    // surface flags
// typedef enum {
    public static final int SURF_TYPE_BIT0  = BIT(0);    // encodes the material type (metal; flesh; concrete; etc.)
    public static final int SURF_TYPE_BIT1  = BIT(1);    // "
    public static final int SURF_TYPE_BIT2  = BIT(2);    // "
    public static final int SURF_TYPE_BIT3  = BIT(3);    // "
    public static final int SURF_TYPE_MASK  = (1 << NUM_SURFACE_BITS) - 1;
    //
    public static final int SURF_NODAMAGE   = BIT(4);    // never give falling damage
    public static final int SURF_SLICK      = BIT(5);    // effects game physics
    public static final int SURF_COLLISION  = BIT(6);    // collision surface
    public static final int SURF_LADDER     = BIT(7);    // player can climb up this surface
    public static final int SURF_NOIMPACT   = BIT(8);    // don't make missile explosions
    public static final int SURF_NOSTEPS    = BIT(9);    // no footstep sounds
    public static final int SURF_DISCRETE   = BIT(10);   // not clipped or merged by utilities
    public static final int SURF_NOFRAGMENT = BIT(11);   // dmap won't cut surface at each bsp boundary
    public static final int SURF_NULLNORMAL = BIT(12);   // renderbump will draw this surface as 0x80 0x80 0x80; which won't collect light from any angle
// } surfaceFlags_t;

    // keep all of these on the stack, when they are static it makes material parsing non-reentrant
    static class mtrParsingData_s {

        public static final transient int SIZE
                = (Bool.SIZE * MAX_EXPRESSION_REGISTERS)
                + (Float.SIZE * MAX_EXPRESSION_REGISTERS)
                + (expOp_t.SIZE * MAX_EXPRESSION_OPS)
                + (shaderStage_t.SIZE * MAX_SHADER_STAGES)
                + Bool.SIZE
                + Bool.SIZE;

        boolean[]       registerIsTemporary = new boolean[MAX_EXPRESSION_REGISTERS];
        float[]         shaderRegisters     = new float[MAX_EXPRESSION_REGISTERS];
        expOp_t[]       shaderOps           = new expOp_t[MAX_EXPRESSION_OPS];
        shaderStage_t[] parseStages         = new shaderStage_t[MAX_SHADER_STAGES];
        //
        boolean registersAreConstant;
        boolean forceOverlays;

        mtrParsingData_s() {

            for (int s = 0; s < shaderOps.length; s++) {
                shaderOps[s] = new expOp_t();
            }

            for (int p = 0; p < parseStages.length; p++) {
                parseStages[p] = new shaderStage_t();
            }
        }
    };

    public static class idMaterial extends neo.framework.DeclManager.idDecl implements neo.TempDump.SERiAL {

        public static final transient int SIZE
                = idStr.SIZE
                + idStr.SIZE
                + Pointer.SIZE //idImage.SIZE //pointer
                + Integer.SIZE
                + 1 //boolean
                + Integer.SIZE
                + Float.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + Integer.SIZE
                + decalInfo_t.SIZE
                + Float.SIZE
                + CPP_class.Enum.SIZE// deform_t.SIZE
                + (Integer.SIZE * 4)
                + idDecl.SIZE//TODO:what good is a pointer in serialization?
                + (Integer.SIZE * MAX_TEXGEN_REGISTERS)
                + CPP_class.Enum.SIZE//materialCoverage_t.SIZE
                + CPP_class.Enum.SIZE//cullType_t.SIZE
                + 7//7 booleans
                + Integer.SIZE
                + Pointer.SIZE //expOp_t.SIZE//pointer
                + Integer.SIZE
                + Float.SIZE//point
                + Float.SIZE//point
                + Integer.SIZE
                + Integer.SIZE
                + Pointer.SIZE //shaderStage_t.SIZE//pointer
                + mtrParsingData_s.SIZE
                + Float.SIZE
                + idStr.SIZE
                + Pointer.SIZE //idImage.SIZE//pointer
                + Float.SIZE
                + 2//2 booleans
                + Integer.SIZE;

        private idStr           desc;            // description
        private idStr           renderBump;      // renderbump command options, without the "renderbump" at the start
        //
        private idImage         lightFalloffImage;
        //
        private int             entityGui;       // draw a gui with the idUserInterface from the renderEntity_t non zero will draw gui, gui2, or gui3 from renderEnitty_t
        //        
        private idUserInterface gui;             // non-custom guis are shared by all users of a material
        //
        private boolean         noFog;           // surface does not create fog interactions
        //
        private int             spectrum;        // for invisible writing, used for both lights and surfaces
        //
        private float           polygonOffset;
        //
        private int             contentFlags;    // content flags
        private int             surfaceFlags;    // surface flags
        private int             materialFlags;   // material flags
        //	
        private decalInfo_t     decalInfo;
        //
        //
        private float           sort;            // lower numbered shaders draw before higher numbered
        private deform_t        deform;
        private final int[] deformRegisters = new int[4];// numeric parameter for deforms
        private idDecl deformDecl;               // for surface emitted particle deforms and tables
        //
        private final int[] texGenRegisters = new int[MAX_TEXGEN_REGISTERS];// for wobbleSky
        //
        private materialCoverage_t coverage;
        private cullType_t         cullType;     // CT_FRONT_SIDED, CT_BACK_SIDED, or CT_TWO_SIDED
        private boolean            shouldCreateBackSides;
        //	
        private boolean            fogLight;
        private boolean            blendLight;
        private boolean            ambientLight;
        private boolean            unsmoothedTangents;
        private boolean            hasSubview;   // mirror, remote render, etc
        private boolean            allowOverlays;
        //
        private int                numOps;
        private expOp_t[]          ops;          // evaluate to make expressionRegisters
        //																										
        private int                numRegisters;                                                                            //
        private float[]            expressionRegisters;
        //
        private float[]            constantRegisters;// NULL if ops ever reference globalParms or entityParms
        //
        private int                numStages;
        private int                numAmbientStages;
        //																										
        private shaderStage_t[]    stages;
        //
        private mtrParsingData_s   pd;           // only used during parsing
        //
        private float              surfaceArea;  // only for listSurfaceAreas
        //
        // we defer loading of the editor image until it is asked for, so the game doesn't load up
        // all the invisible and uncompressed images.
        // If editorImage is NULL, it will atempt to load editorImageName, and set editorImage to that or defaultImage
        private idStr              editorImageName;
        private idImage            editorImage;  // image used for non-shaded preview
        private float              editorAlpha;
        //
        private boolean            suppressInSubview;
        private boolean            portalSky;
        private int                refCount;
        //
        //
        private static int debug_creation_counter = 0;
        private final int dbg_count;

        public idMaterial() {
            dbg_count = debug_creation_counter++;
            this.decalInfo = new decalInfo_t();
            CommonInit();

            // we put this here instead of in CommonInit, because
            // we don't want it cleared when a material is purged
            surfaceArea = 0;
        }
//	virtual				~idMaterial();

        idMaterial(idMaterial shader) {//TODO:clone?
            throw new UnsupportedOperationException();
//            this.desc =shader. desc;
//            this.renderBump =shader. renderBump;
//            this.lightFalloffImage =shader. lightFalloffImage;
//            this.entityGui =shader. entityGui;
//            this.gui =shader. gui;
//            this.noFog =shader. noFog;
//            this.spectrum =shader. spectrum;
//            this.polygonOffset =shader. polygonOffset;
//            this.contentFlags =shader. contentFlags;
//            this.surfaceFlags =shader. surfaceFlags;
//            this.materialFlags =shader. materialFlags;
//            this.decalInfo =shader. decalInfo;
//            this.sort =shader. sort;
//            this.deform =shader. deform;
//            this.deformDecl =shader. deformDecl;
//            this.coverage =shader. coverage;
//            this.cullType =shader. cullType;
//            this.shouldCreateBackSides =shader. shouldCreateBackSides;
//            this.fogLight =shader. fogLight;
//            this.blendLight =shader. blendLight;
//            this.ambientLight =shader. ambientLight;
//            this.unsmoothedTangents =shader. unsmoothedTangents;
//            this.hasSubview =shader. hasSubview;
//            this.allowOverlays =shader. allowOverlays;
//            this.numOps =shader. numOps;
//            this.ops =shader. ops;
//            this.numRegisters =shader. numRegisters;
//            this.expressionRegisters =shader. expressionRegisters;
//            this.constantRegisters =shader. constantRegisters;
//            this.numStages =shader. numStages;
//            this.numAmbientStages =shader. numAmbientStages;
//            this.stages =shader. stages;
//            this.pd =shader. pd;
//            this.surfaceArea =shader. surfaceArea;
//            this.editorImageName =shader. editorImageName;
//            this.editorImage =shader. editorImage;
//            this.editorAlpha =shader. editorAlpha;
//            this.suppressInSubview =shader. suppressInSubview;
//            this.portalSky =shader. portalSky;
//            this.refCount =shader. refCount;
        }

        @Override
        public long Size() {
//            return sizeof( idMaterial );
            return super.Size();
        }

        @Override
        public boolean SetDefaultText() {
            // if there exists an image with the same name
            if (true) { //fileSystem->ReadFile( GetName(), NULL ) != -1 ) {
                StringBuilder generated = new StringBuilder(2048);
                idStr.snPrintf(generated, generated.capacity(),
                        "material %s // IMPLICITLY GENERATED\n"
                                + "{\n"
                                + "{\n"
                                + "blend blend\n"
                                + "colored\n"
                                + "map \"%s\"\n"
                                + "clamp\n"
                                + "}\n"
                                + "}\n", GetName(), GetName());
                SetText(generated.toString());
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String DefaultDefinition() {
            return "{\n"
                    + "\t" + "{\n"
                    + "\t\t" + "blend\tblend\n"
                    + "\t\t" + "map\t\t_default\n"
                    + "\t" + "}\n"
                    + "}";
        }

        /*
         =========================
         idMaterial::Parse

         Parses the current material definition and finds all necessary images.
         =========================
         */        private static int DEBUG_Parse = 0;

        @Override
        public boolean Parse(final String text, final int textLength) {
            idLexer src = new idLexer();
//	idToken	token;
            mtrParsingData_s parsingData = new mtrParsingData_s();

            src.LoadMemory(text, textLength, GetFileName(), GetLineNum());
            src.SetFlags(DECL_LEXER_FLAGS);
            src.SkipUntilString("{");

            // reset to the unparsed state
            CommonInit();

//	memset( &parsingData, 0, sizeof( parsingData ) );
            pd = parsingData;    // this is only valid during parse

            // parse it
            ParseMaterial(src);

            // if we are doing an fs_copyfiles, also reference the editorImage
            if (cvarSystem.GetCVarInteger("fs_copyFiles") != 0) {
                GetEditorImage();
            }

            //
            // count non-lit stages
            numAmbientStages = 0;
            int i;
            for (i = 0; i < numStages; i++) {
                if (pd.parseStages[i].lighting == SL_AMBIENT) {
                    numAmbientStages++;
                }
            }

            // see if there is a subview stage
            if (sort == SS_SUBVIEW) {
                hasSubview = true;
            } else {
                hasSubview = false;
                for (i = 0; i < numStages; i++) {
                    if (etoi(pd.parseStages[i].texture.dynamic) != 0) {
                        hasSubview = true;
                    }
                }
            }

            // automatically determine coverage if not explicitly set
            if (coverage == MC_BAD) {
                // automatically set MC_TRANSLUCENT if we don't have any interaction stages and 
                // the first stage is blended and not an alpha test mask or a subview
                if (0 == numStages) {
                    // non-visible
                    coverage = MC_TRANSLUCENT;
                } else if (numStages != numAmbientStages) {
                    // we have an interaction draw
                    coverage = MC_OPAQUE;
                } else if ((pd.parseStages[0].drawStateBits & GLS_DSTBLEND_BITS) != GLS_DSTBLEND_ZERO
                        || (pd.parseStages[0].drawStateBits & GLS_SRCBLEND_BITS) == GLS_SRCBLEND_DST_COLOR
                        || (pd.parseStages[0].drawStateBits & GLS_SRCBLEND_BITS) == GLS_SRCBLEND_ONE_MINUS_DST_COLOR
                        || (pd.parseStages[0].drawStateBits & GLS_SRCBLEND_BITS) == GLS_SRCBLEND_DST_ALPHA
                        || (pd.parseStages[0].drawStateBits & GLS_SRCBLEND_BITS) == GLS_SRCBLEND_ONE_MINUS_DST_ALPHA) {
                    // blended with the destination
                    coverage = MC_TRANSLUCENT;
                } else {
                    coverage = MC_OPAQUE;
                }
            }

            // translucent automatically implies noshadows
            if (coverage == MC_TRANSLUCENT) {
                SetMaterialFlag(MF_NOSHADOWS);
            } else {
                // mark the contents as opaque
                contentFlags |= CONTENTS_OPAQUE;
            }

            // if we are translucent, draw with an alpha in the editor
            if (coverage == MC_TRANSLUCENT) {
                editorAlpha = 0.5f;
            } else {
                editorAlpha = 1.0f;
            }

            // the sorts can make reasonable defaults
            if (sort == SS_BAD) {
                if (TestMaterialFlag(MF_POLYGONOFFSET)) {
                    sort = SS_DECAL;
                } else if (coverage == MC_TRANSLUCENT) {
                    sort = SS_MEDIUM;
                } else {
                    sort = SS_OPAQUE;
                }
            }

            // anything that references _currentRender will automatically get sort = SS_POST_PROCESS
            // and coverage = MC_TRANSLUCENT
            for (i = 0; i < numStages; i++) {
                shaderStage_t pStage = pd.parseStages[i];
                if (pStage.texture.image[0] == globalImages.currentRenderImage) {
                    if (sort != SS_PORTAL_SKY) {
                        sort = SS_POST_PROCESS;
                        coverage = MC_TRANSLUCENT;
                    }
                    break;
                }
                if (pStage.newStage != null) {
                    for (int j = 0; j < pStage.newStage.numFragmentProgramImages; j++) {
                        if (pStage.newStage.fragmentProgramImages[j] == globalImages.currentRenderImage) {
                            if (sort != SS_PORTAL_SKY) {
                                sort = SS_POST_PROCESS;
                                coverage = MC_TRANSLUCENT;
                            }
                            i = numStages;
                            break;
                        }
                    }
                }
            }

            // set the drawStateBits depth flags
            for (i = 0; i < numStages; i++) {
                shaderStage_t pStage = pd.parseStages[i];
                if (sort == SS_POST_PROCESS) {
                    // post-process effects fill the depth buffer as they draw, so only the
                    // topmost post-process effect is rendered
                    pStage.drawStateBits |= GLS_DEPTHFUNC_LESS;
                } else if (coverage == MC_TRANSLUCENT || pStage.ignoreAlphaTest) {
                    // translucent surfaces can extend past the exactly marked depth buffer
                    pStage.drawStateBits |= GLS_DEPTHFUNC_LESS | GLS_DEPTHMASK;
                } else {
                    // opaque and perforated surfaces must exactly match the depth buffer,
                    // which gets alpha test correct
                    pStage.drawStateBits |= GLS_DEPTHFUNC_EQUAL | GLS_DEPTHMASK;
                }
            }

            // determine if this surface will accept overlays / decals
            if (pd.forceOverlays) {
                // explicitly flaged in material definition
                allowOverlays = true;
            } else {
                if (!IsDrawn()) {
                    allowOverlays = false;
                }
                if (Coverage() != MC_OPAQUE) {
                    allowOverlays = false;
                }
                if ((GetSurfaceFlags() & SURF_NOIMPACT) != 0) {
                    allowOverlays = false;
                }
            }

            // add a tiny offset to the sort orders, so that different materials
            // that have the same sort value will at least sort consistantly, instead
            // of flickering back and forth
/* this messed up in-game guis
             if ( sort != SS_SUBVIEW ) {
             int	hash, l;

             l = name.Length();
             hash = 0;
             for ( int i = 0 ; i < l ; i++ ) {
             hash ^= name[i];
             }
             sort += hash * 0.01;
             }
             */
            if (numStages != 0) {
                stages = new shaderStage_t[numStages];// R_StaticAlloc(numStages* sizeof( stages[0] )
//		memcpy( stages, pd.parseStages, numStages * sizeof( stages[0] ) );
                DEBUG_Parse++;
//                System.out.printf("%d-->%s\n", DEBUG_Parse, text);
                for (int a = 0; a < numStages; a++) {
                    stages[a] = new shaderStage_t(pd.parseStages[a]);
                }
            }

            if (numOps != 0) {
                ops = new expOp_t[numOps];// R_StaticAlloc(numOps * sizeof( ops[0] )
//		memcpy( ops, pd.shaderOps, numOps * sizeof( ops[0] ) );
                for (int a = 0; a < ops.length; a++) {
                    ops[a] = new expOp_t(pd.shaderOps[a]);
                }
            }

            if (numRegisters != 0) {
                expressionRegisters = new float[numRegisters];//R_StaticAlloc(numRegisters *sizeof( expressionRegisters[0] )
//		memcpy( expressionRegisters, pd.shaderRegisters, numRegisters * sizeof( expressionRegisters[0] ) );
                System.arraycopy(pd.shaderRegisters, 0, expressionRegisters, 0, numRegisters);
            }

            // see if the registers are completely constant, and don't need to be evaluated
            // per-surface
            CheckForConstantRegisters();

            pd = null;    // the pointer will be invalid after exiting this function

            // finish things up
            if (TestMaterialFlag(MF_DEFAULTED)) {
                MakeDefault();
                return false;
            }
            return true;
        }

        @Override
        public void FreeData() {
            int i;

            if (stages != null) {
                // delete any idCinematic textures
                for (i = 0; i < numStages; i++) {//TODO:for loop is unnecessary
                    if (stages[i].texture.cinematic[0] != null) {
//				delete stages[i].texture.cinematic;
                        stages[i].texture.cinematic[0] = null;
                    }
                    if (stages[i].newStage != null) {
                        stages[i].newStage = null;
                        stages[i].newStage = null;
                    }
                }
//                R_StaticFree(stages);
                stages = null;
            }
            if (expressionRegisters != null) {
//                R_StaticFree(expressionRegisters);
                expressionRegisters = null;
            }
            if (constantRegisters != null) {
//                R_StaticFree(constantRegisters);
                constantRegisters = null;
            }
            if (ops != null) {
//                R_StaticFree(ops);
                ops = null;
            }
        }

        @Override
        public void Print() {
            int i;

            for (i = EXP_REG_NUM_PREDEFINED.ordinal(); i < GetNumRegisters(); i++) {
                common.Printf("register %d: %f\n", i, expressionRegisters[i]);
            }
            common.Printf("\n");
            for (i = 0; i < numOps; i++) {
                final expOp_t op = ops[i];
                if (op.opType == OP_TYPE_TABLE) {
                    common.Printf("%d = %s[ %d ]\n", op.c, declManager.DeclByIndex(DECL_TABLE, op.a).GetName(), op.b);
                } else {
                    common.Printf("%d = %d %s %d\n", op.c, op.a, op.opType.toString(), op.b);
                }
            }
        }

        //BSM Nerve: Added for material editor
        public boolean Save(final String fileName /*= NULL*/) {
            return ReplaceSourceFileText();
        }

        public boolean Save() {
            return Save(null);
        }

        // returns the internal image name for stage 0, which can be used
        // for the renderer CaptureRenderToImage() call
        // I'm not really sure why this needs to be virtual...
        public String ImageName() {
            if (numStages == 0) {
                return "_scratch";
            }
            idImage image = stages[0].texture.image[0];
            if (image != null) {
                return image.imgName.toString();
            }
            return "_scratch";
        }

        public void ReloadImages(boolean force) {
            for (int i = 0; i < numStages; i++) {
                if (stages[i].newStage != null) {
                    for (int j = 0; j < stages[i].newStage.numFragmentProgramImages; j++) {
                        if (stages[i].newStage.fragmentProgramImages[j] != null) {
                            stages[i].newStage.fragmentProgramImages[j].Reload(false, force);
                        }
                    }
                } else if (stages[i].texture.image != null) {
                    stages[i].texture.image[0].Reload(false, force);
                }
            }
        }

        // returns number of stages this material contains
        public int GetNumStages() {
            return numStages;
        }

        // get a specific stage
        public shaderStage_t GetStage(final int index) {
            assert (index >= 0 && index < numStages);
            return stages[index];
        }

        // get the first bump map stage, or NULL if not present.
        // used for bumpy-specular
        public shaderStage_t GetBumpStage() {
            for (int i = 0; i < numStages; i++) {
                if (stages[i].lighting == SL_BUMP) {
                    return stages[i];
                }
            }
            return null;
        }

        // returns true if the material will draw anything at all.  Triggers, portals,
        // etc, will not have anything to draw.  A not drawn surface can still castShadow,
        // which can be used to make a simplified shadow hull for a complex object set
        // as noShadow
        public boolean IsDrawn() {
            return (numStages > 0 || entityGui != 0 || gui != null);
        }

        // returns true if the material will draw any non light interaction stages
        public boolean HasAmbient() {
            return (numAmbientStages > 0);
        }

        // returns true if material has a gui
        public boolean HasGui() {
            return (entityGui != 0 || gui != null);
        }

        // returns true if the material will generate another view, either as
        // a mirror or dynamic rendered image
        public boolean HasSubview() {
            return hasSubview;
        }

        // returns true if the material will generate shadows, not making a
        // distinction between global and no-self shadows
        public boolean SurfaceCastsShadow() {
            return TestMaterialFlag(MF_FORCESHADOWS) || !TestMaterialFlag(MF_NOSHADOWS);
        }

        // returns true if the material will generate interactions with fog/blend lights
        // All non-translucent surfaces receive fog unless they are explicitly noFog
        public boolean ReceivesFog() {
            return (IsDrawn() && !noFog && coverage != MC_TRANSLUCENT);
        }

        // returns true if the material will generate interactions with normal lights
        // Many special effect surfaces don't have any bump/diffuse/specular
        // stages, and don't interact with lights at all
        public boolean ReceivesLighting() {
            return numAmbientStages != numStages;
        }

        // returns true if the material should generate interactions on sides facing away
        // from light centers, as with noshadow and noselfshadow options
        public boolean ReceivesLightingOnBackSides() {
            return (materialFlags & (MF_NOSELFSHADOW | MF_NOSHADOWS)) != 0;
        }

        // Standard two-sided triangle rendering won't work with bump map lighting, because
        // the normal and tangent vectors won't be correct for the back sides.  When two
        // sided lighting is desired. typically for alpha tested surfaces, this is
        // addressed by having CleanupModelSurfaces() create duplicates of all the triangles
        // with apropriate order reversal.
        public boolean ShouldCreateBackSides() {
            return shouldCreateBackSides;
        }

        // characters and models that are created by a complete renderbump can use a faster
        // method of tangent and normal vector generation than surfaces which have a flat
        // renderbump wrapped over them.
        public boolean UseUnsmoothedTangents() {
            return unsmoothedTangents;
        }

        // by default, monsters can have blood overlays placed on them, but this can
        // be overrided on a per-material basis with the "noOverlays" material command.
        // This will always return false for translucent surfaces
        public boolean AllowOverlays() {
            return allowOverlays;
        }

        // MC_OPAQUE, MC_PERFORATED, or MC_TRANSLUCENT, for interaction list linking and
        // dmap flood filling
        // The depth buffer will not be filled for MC_TRANSLUCENT surfaces
        // FIXME: what do nodraw surfaces return?
        public materialCoverage_t Coverage() {
            return coverage;
        }

        // returns true if this material takes precedence over other in coplanar cases
        public boolean HasHigherDmapPriority(final idMaterial other) {
            return (IsDrawn() && !other.IsDrawn())
                    || (Coverage().ordinal() < other.Coverage().ordinal());
        }

        // returns a idUserInterface if it has a global gui, or NULL if no gui
        public idUserInterface GlobalGui() {
            return gui;
        }

        // a discrete surface will never be merged with other surfaces by dmap, which is
        // necessary to prevent mutliple gui surfaces, mirrors, autosprites, and some other
        // special effects from being combined into a single surface
        // guis, merging sprites or other effects, mirrors and remote views are always discrete
        public boolean IsDiscrete() {
            return (entityGui != 0 || gui != null || deform != DFRM_NONE || sort == SS_SUBVIEW
                    || (surfaceFlags & SURF_DISCRETE) != 0);
        }

        // Normally, dmap chops each surface by every BSP boundary, then reoptimizes.
        // For gigantic polygons like sky boxes, this can cause a huge number of planar
        // triangles that make the optimizer take forever to turn back into a single
        // triangle.  The "noFragment" option causes dmap to only break the polygons at
        // area boundaries, instead of every BSP boundary.  This has the negative effect
        // of not automatically fixing up interpenetrations, so when this is used, you
        // should manually make the edges of your sky box exactly meet, instead of poking
        // into each other.
        public boolean NoFragment() {
            return (surfaceFlags & SURF_NOFRAGMENT) != 0;
        }

        //------------------------------------------------------------------
        // light shader specific functions, only called for light entities
        // lightshader option to fill with fog from viewer instead of light from center
        public boolean IsFogLight() {
            return fogLight;
        }

        // perform simple blending of the projection, instead of interacting with bumps and textures
        public boolean IsBlendLight() {
            return blendLight;
        }

        // an ambient light has non-directional bump mapping and no specular
        public boolean IsAmbientLight() {
            return ambientLight;
        }

        // implicitly no-shadows lights (ambients, fogs, etc) will never cast shadows
        // but individual light entities can also override this value
        public boolean LightCastsShadows() {
            return TestMaterialFlag(MF_FORCESHADOWS)
                    || (!fogLight && !ambientLight && !blendLight && !TestMaterialFlag(MF_NOSHADOWS));
        }

        // fog lights, blend lights, ambient lights, etc will all have to have interaction
        // triangles generated for sides facing away from the light as well as those
        // facing towards the light.  It is debatable if noshadow lights should effect back
        // sides, making everything "noSelfShadow", but that would make noshadow lights
        // potentially slower than normal lights, which detracts from their optimization
        // ability, so they currently do not.
        public boolean LightEffectsBackSides() {
            return fogLight || ambientLight || blendLight;
        }

        // NULL unless an image is explicitly specified in the shader with "lightFalloffShader <image>"
        public idImage LightFalloffImage() {
            return lightFalloffImage;
        }

        //------------------------------------------------------------------
        // returns the renderbump command line for this shader, or an empty string if not present
        public String GetRenderBump() {
            return renderBump.toString();
        }

        // set specific material flag(s)
        public void SetMaterialFlag(final int flag) {
            materialFlags |= flag;
        }

        // clear specific material flag(s)
        public void ClearMaterialFlag(final int flag) {
            materialFlags &= ~flag;
        }

        // test for existance of specific material flag(s)
        public boolean TestMaterialFlag(final int flag) {
            return (materialFlags & flag) != 0;
        }

        // get content flags
        public int GetContentFlags() {
            return contentFlags;
        }

        // get surface flags
        public int GetSurfaceFlags() {
            return surfaceFlags;
        }

        // gets name for surface type (stone, metal, flesh, etc.)
        public surfTypes_t GetSurfaceType() {
            return surfTypes_t.values()[surfaceFlags & SURF_TYPE_MASK];
        }

        // get material description
        public String GetDescription() {
            return desc.toString();
        }

        // get sort order
        public float GetSort() {
            return sort;
        }

        // this is only used by the gui system to force sorting order
        // on images referenced from tga's instead of materials. 
        // this is done this way as there are 2000 tgas the guis use
        public void SetSort(float s) {
            sort = s;
        }

        // DFRM_NONE, DFRM_SPRITE, etc
        public deform_t Deform() {
            return deform;
        }

        // flare size, expansion size, etc
        public int GetDeformRegister(int index) {
            return deformRegisters[index];
        }

        // particle system to emit from surface and table for turbulent
        public idDecl GetDeformDecl() {
            return deformDecl;
        }

        // currently a surface can only have one unique texgen for all the stages
        public texgen_t Texgen() {
            if (stages != null) {
                for (int i = 0; i < numStages; i++) {
                    if (stages[i].texture.texgen != TG_EXPLICIT) {
                        return stages[i].texture.texgen;
                    }
                }
            }

            return TG_EXPLICIT;
        }

        // wobble sky parms
        public int[] GetTexGenRegisters() {
            return texGenRegisters;
        }

        // get cull type
        public cullType_t GetCullType() {
            return cullType;
        }

        public float GetEditorAlpha() {
            return editorAlpha;
        }

        public int GetEntityGui() {
            return entityGui;
        }

        public decalInfo_t GetDecalInfo() {
            return decalInfo;
        }

        // spectrums are used for "invisible writing" that can only be
        // illuminated by a light of matching spectrum
        public int Spectrum() {
            return spectrum;
        }

        public float GetPolygonOffset() {
            return polygonOffset;
        }

        public float GetSurfaceArea() {
            return surfaceArea;
        }

        public void AddToSurfaceArea(float area) {
            surfaceArea += area;
        }

        //------------------------------------------------------------------
        // returns the length, in milliseconds, of the videoMap on this material,
        // or zero if it doesn't have one
        public int CinematicLength() {
            if (NOT(stages) || NOT(stages[0].texture.cinematic[0])) {
                return 0;
            }
            return stages[0].texture.cinematic[0].AnimationLength();
        }

        public void CloseCinematic() {
            for (int i = 0; i < numStages; i++) {
                if (stages[i].texture.cinematic[0] != null) {
                    stages[i].texture.cinematic[0].Close();
//			delete stages[i].texture.cinematic;
                    stages[i].texture.cinematic[0] = null;
                }
            }
        }

        public void ResetCinematicTime(int time) {
            for (int i = 0; i < numStages; i++) {
                if (stages[i].texture.cinematic[0] != null) {
                    stages[i].texture.cinematic[0].ResetTime(time);
                }
            }
        }

        public void UpdateCinematic(int time) {
            if (NOT(stages) || NOT(stages[0].texture.cinematic[0]) || NOT(backEnd.viewDef)) {
                return;
            }
            stages[0].texture.cinematic[0].ImageForTime(tr.primaryRenderView.time);
        }
//
//	//------------------------------------------------------------------
//

        // gets an image for the editor to use
        public idImage GetEditorImage() {
            if (editorImage != null) {
                return editorImage;
            }

            // if we don't have an editorImageName, use the first stage image
            if (0 == editorImageName.Length()) {
                // _D3XP :: First check for a diffuse image, then use the first
                if (numStages != 0 && stages != null) {
                    int i;
                    for (i = 0; i < numStages; i++) {
                        if (stages[i].lighting == SL_DIFFUSE) {
                            editorImage = stages[i].texture.image[0];
                            break;
                        }
                    }
                    if (null == editorImage) {
                        editorImage = stages[0].texture.image[0];
                    }
                } else {
                    editorImage = globalImages.defaultImage;
                }
            } else {
                // look for an explicit one
                editorImage = globalImages.ImageFromFile(editorImageName.toString(), TF_DEFAULT, true, TR_REPEAT, TD_DEFAULT);
            }

            if (null == editorImage) {
                editorImage = globalImages.defaultImage;
            }

            return editorImage;
        }

        public int GetImageWidth() {
            assert (GetStage(0) != null && GetStage(0).texture.image[0] != null);
            return GetStage(0).texture.image[0].uploadWidth;
        }

        public int GetImageHeight() {
            assert (GetStage(0) != null && GetStage(0).texture.image[0] != null);
            return GetStage(0).texture.image[0].uploadHeight;
        }

        public void SetGui(final String _gui) {
            gui = uiManager.FindGui(_gui, true, false, true);
        }


        /*
         ===================
         idMaterial::SetImageClassifications

         Just for image resource tracking.
         ===================
         */
        public void SetImageClassifications(int tag) {
            for (int i = 0; i < numStages; i++) {
                idImage image = stages[i].texture.image[0];
                if (image != null) {
                    image.SetClassification(tag);
                }
            }
        }
        //------------------------------------------------------------------

        // returns number of registers this material contains
        public int GetNumRegisters() {
            return numRegisters;
        }


        /*
         ===============
         idMaterial::EvaluateRegisters

         Parameters are taken from the localSpace and the renderView,
         then all expressions are evaluated, leaving the material registers
         set to their apropriate values.
         ===============
         */
        // regs should point to a float array large enough to hold GetNumRegisters() floats
        public void EvaluateRegisters(float[] regs, final float[] shaderParms/*[MAX_ENTITY_SHADER_PARMS]*/,
                final viewDef_s view, idSoundEmitter soundEmitter /*= NULL*/) {
            int i, b;
            /*expOp_t*/ int op;

            // copy the material constants
            for (i = etoi(EXP_REG_NUM_PREDEFINED); i < numRegisters; i++) {
                regs[i] = expressionRegisters[i];
            }

            // copy the local and global parameters
            regs[etoi(EXP_REG_TIME)] = view.floatTime;
            regs[etoi(EXP_REG_PARM0)] = shaderParms[0];
            regs[etoi(EXP_REG_PARM1)] = shaderParms[1];
            regs[etoi(EXP_REG_PARM2)] = shaderParms[2];
            regs[etoi(EXP_REG_PARM3)] = shaderParms[3];
            regs[etoi(EXP_REG_PARM4)] = shaderParms[4];
            regs[etoi(EXP_REG_PARM5)] = shaderParms[5];
            regs[etoi(EXP_REG_PARM6)] = shaderParms[6];
            regs[etoi(EXP_REG_PARM7)] = shaderParms[7];
            regs[etoi(EXP_REG_PARM8)] = shaderParms[8];
            regs[etoi(EXP_REG_PARM9)] = shaderParms[9];
            regs[etoi(EXP_REG_PARM10)] = shaderParms[10];
            regs[etoi(EXP_REG_PARM11)] = shaderParms[11];
            regs[etoi(EXP_REG_GLOBAL0)] = view.renderView.shaderParms[0];
            regs[etoi(EXP_REG_GLOBAL1)] = view.renderView.shaderParms[1];
            regs[etoi(EXP_REG_GLOBAL2)] = view.renderView.shaderParms[2];
            regs[etoi(EXP_REG_GLOBAL3)] = view.renderView.shaderParms[3];
            regs[etoi(EXP_REG_GLOBAL4)] = view.renderView.shaderParms[4];
            regs[etoi(EXP_REG_GLOBAL5)] = view.renderView.shaderParms[5];
            regs[etoi(EXP_REG_GLOBAL6)] = view.renderView.shaderParms[6];
            regs[etoi(EXP_REG_GLOBAL7)] = view.renderView.shaderParms[7];

            op = 0;// = ops;
            for (i = 0; i < numOps; i++, op++) {
                switch (ops[op].opType) {
                    case OP_TYPE_ADD:
                        regs[ops[op].c] = regs[ops[op].a] + regs[ops[op].b];
                        break;
                    case OP_TYPE_SUBTRACT:
                        regs[ops[op].c] = regs[ops[op].a] - regs[ops[op].b];
                        break;
                    case OP_TYPE_MULTIPLY:
                        regs[ops[op].c] = regs[ops[op].a] * regs[ops[op].b];
                        break;
                    case OP_TYPE_DIVIDE:
                        regs[ops[op].c] = regs[ops[op].a] / regs[ops[op].b];
                        break;
                    case OP_TYPE_MOD:
                        b = (int) regs[ops[op].b];
                        b = b != 0 ? b : 1;
                        regs[ops[op].c] = (int) regs[ops[op].a] % b;
                        break;
                    case OP_TYPE_TABLE: {
                        final idDeclTable table = (idDeclTable) (declManager.DeclByIndex(DECL_TABLE, ops[op].a));
                        regs[ops[op].c] = table.TableLookup(regs[ops[op].b]);
                    }
                    break;
                    case OP_TYPE_SOUND:
                        if (soundEmitter != null) {
                            regs[ops[op].c] = soundEmitter.CurrentAmplitude();
                        } else {
                            regs[ops[op].c] = 0;
                        }
                        break;
                    case OP_TYPE_GT:
                        regs[ops[op].c] = regs[ops[op].a] > regs[ops[op].b] ? 1 : 0;
                        break;
                    case OP_TYPE_GE:
                        regs[ops[op].c] = regs[ops[op].a] >= regs[ops[op].b] ? 1 : 0;
                        break;
                    case OP_TYPE_LT:
                        regs[ops[op].c] = regs[ops[op].a] < regs[ops[op].b] ? 1 : 0;
                        break;
                    case OP_TYPE_LE:
                        regs[ops[op].c] = regs[ops[op].a] <= regs[ops[op].b] ? 1 : 0;
                        break;
                    case OP_TYPE_EQ:
                        regs[ops[op].c] = regs[ops[op].a] == regs[ops[op].b] ? 1 : 0;
                        break;
                    case OP_TYPE_NE:
                        regs[ops[op].c] = regs[ops[op].a] != regs[ops[op].b] ? 1 : 0;
                        break;
                    case OP_TYPE_AND:
                        regs[ops[op].c] = (regs[ops[op].a] != 0 && regs[ops[op].b] != 0) ? 1 : 0;
                        break;
                    case OP_TYPE_OR:
                        regs[ops[op].c] = (regs[ops[op].a] != 0 || regs[ops[op].b] != 0) ? 1 : 0;
                        break;
                    default:
                        common.FatalError("R_EvaluateExpression: bad opcode");
                }
            }

        }

        // if a material only uses constants (no entityParm or globalparm references), this
        // will return a pointer to an internal table, and EvaluateRegisters will not need
        // to be called.  If NULL is returned, EvaluateRegisters must be used.
        public float[] ConstantRegisters() {
            if (!RenderSystem_init.r_useConstantMaterials.GetBool()) {
                return null;
            }
            return constantRegisters;
        }

        public boolean SuppressInSubview() {
            return suppressInSubview;
        }

        public boolean IsPortalSky() {
            return portalSky;
        }

        public void AddReference() {
            refCount++;

            for (int i = 0; i < numStages; i++) {
                shaderStage_t s = stages[i];

                if (s.texture.image[0] != null) {
                    s.texture.image[0].AddReference();
                }
            }
        }

        // parse the entire material
        private void CommonInit() {
            desc = new idStr("<none>");
            renderBump = new idStr("");
            contentFlags = CONTENTS_SOLID;
            surfaceFlags = etoi(SURFTYPE_NONE);
            materialFlags = 0;
            sort = SS_BAD;
            coverage = MC_BAD;
            cullType = CT_FRONT_SIDED;
            deform = DFRM_NONE;
            numOps = 0;
            ops = null;
            numRegisters = 0;
            expressionRegisters = null;
            constantRegisters = null;
            numStages = 0;
            numAmbientStages = 0;
            stages = null;
            editorImage = null;
            lightFalloffImage = null;
            shouldCreateBackSides = false;
            entityGui = 0;
            fogLight = false;
            blendLight = false;
            ambientLight = false;
            noFog = false;
            hasSubview = false;
            allowOverlays = true;
            unsmoothedTangents = false;
            gui = null;
//	memset( deformRegisters, 0, sizeof( deformRegisters ) );
            Arrays.fill(deformRegisters, 0);
            editorAlpha = 1.0f;
            spectrum = 0;
            polygonOffset = 0;
            suppressInSubview = false;
            refCount = 0;
            portalSky = false;

            decalInfo.stayTime = 10000;
            decalInfo.fadeTime = 4000;
            decalInfo.start[0] = 1;
            decalInfo.start[1] = 1;
            decalInfo.start[2] = 1;
            decalInfo.start[3] = 1;
            decalInfo.end[0] = 0;
            decalInfo.end[1] = 0;
            decalInfo.end[2] = 0;
            decalInfo.end[3] = 0;
        }

        /*
         =================
         idMaterial::ParseMaterial

         The current text pointer is at the explicit text definition of the
         Parse it into the global material variable. Later functions will optimize it.

         If there is any error during parsing, defaultShader will be set.
         =================
         */
        private void ParseMaterial(idLexer src) {
            idToken token = new idToken();
            int s;
            char[] buffer = new char[1024];
            String str;
            idLexer newSrc = new idLexer();
            int i;

            s = 0;

            numOps = 0;
            numRegisters = EXP_REG_NUM_PREDEFINED.ordinal();// leave space for the parms to be copied in
            for (i = 0; i < numRegisters; i++) {
                pd.registerIsTemporary[i] = true;// they aren't constants that can be folded
            }

            numStages = 0;

            textureRepeat_t trpDefault = TR_REPEAT;// allow a global setting for repeat

            while (true) {
                if (TestMaterialFlag(MF_DEFAULTED)) {// we have a parse error
                    return;
                }
                if (!src.ExpectAnyToken(token)) {
                    SetMaterialFlag(MF_DEFAULTED);
                    return;
                }

                // end of material definition
                if (token.equals("}")) {
                    break;
                } else if (0 == token.Icmp("qer_editorimage")) {
                    src.ReadTokenOnLine(token);
                    editorImageName = new idStr(token.toString());
                    src.SkipRestOfLine();
                    continue;
                } // description
                else if (0 == token.Icmp("description")) {
                    src.ReadTokenOnLine(token);
                    desc = new idStr(token.toString());
                    continue;
                } // check for the surface / content bit flags
                else if (CheckSurfaceParm(token)) {
                    continue;
                } // polygonOffset
                else if (0 == token.Icmp("polygonOffset")) {
                    SetMaterialFlag(MF_POLYGONOFFSET);
                    if (!src.ReadTokenOnLine(token)) {
                        polygonOffset = 1;
                        continue;
                    }
                    // explict larger (or negative) offset
                    polygonOffset = token.GetFloatValue();
                    continue;
                } // noshadow
                else if (0 == token.Icmp("noShadows")) {
                    SetMaterialFlag(MF_NOSHADOWS);
                    continue;
                } else if (0 == token.Icmp("suppressInSubview")) {
                    suppressInSubview = true;
                    continue;
                } else if (0 == token.Icmp("portalSky")) {
                    portalSky = true;
                    continue;
                } // noSelfShadow
                else if (0 == token.Icmp("noSelfShadow")) {
                    SetMaterialFlag(MF_NOSELFSHADOW);
                    continue;
                } // noPortalFog
                else if (0 == token.Icmp("noPortalFog")) {
                    SetMaterialFlag(MF_NOPORTALFOG);
                    continue;
                } // forceShadows allows nodraw surfaces to cast shadows
                else if (0 == token.Icmp("forceShadows")) {
                    SetMaterialFlag(MF_FORCESHADOWS);
                    continue;
                } // overlay / decal suppression
                else if (0 == token.Icmp("noOverlays")) {
                    allowOverlays = false;
                    continue;
                } // moster blood overlay forcing for alpha tested or translucent surfaces
                else if (0 == token.Icmp("forceOverlays")) {
                    pd.forceOverlays = true;
                    continue;
                } // translucent
                else if (0 == token.Icmp("translucent")) {
                    coverage = MC_TRANSLUCENT;
                    continue;
                } // global zero clamp
                else if (0 == token.Icmp("zeroclamp")) {
                    trpDefault = TR_CLAMP_TO_ZERO;
                    continue;
                } // global clamp
                else if (0 == token.Icmp("clamp")) {
                    trpDefault = TR_CLAMP;
                    continue;
                } // global clamp
                else if (0 == token.Icmp("alphazeroclamp")) {
                    trpDefault = TR_CLAMP_TO_ZERO;
                    continue;
                } // forceOpaque is used for skies-behind-windows
                else if (0 == token.Icmp("forceOpaque")) {
                    coverage = MC_OPAQUE;
                    continue;
                } // twoSided
                else if (0 == token.Icmp("twoSided")) {
                    cullType = CT_TWO_SIDED;
                    // twoSided implies no-shadows, because the shadow
                    // volume would be coplanar with the surface, giving depth fighting
                    // we could make this no-self-shadows, but it may be more important
                    // to receive shadows from no-self-shadow monsters
                    SetMaterialFlag(MF_NOSHADOWS);
                } // backSided
                else if (0 == token.Icmp("backSided")) {
                    cullType = CT_BACK_SIDED;
                    // the shadow code doesn't handle this, so just disable shadows.
                    // We could fix this in the future if there was a need.
                    SetMaterialFlag(MF_NOSHADOWS);
                } // foglight
                else if (0 == token.Icmp("fogLight")) {
                    fogLight = true;
                    continue;
                } // blendlight
                else if (0 == token.Icmp("blendLight")) {
                    blendLight = true;
                    continue;
                } // ambientLight
                else if (0 == token.Icmp("ambientLight")) {
                    ambientLight = true;
                    continue;
                } // mirror
                else if (0 == token.Icmp("mirror")) {
                    sort = SS_SUBVIEW;
                    coverage = MC_OPAQUE;
                    continue;
                } // noFog
                else if (0 == token.Icmp("noFog")) {
                    noFog = true;
                    continue;
                } // unsmoothedTangents
                else if (0 == token.Icmp("unsmoothedTangents")) {
                    unsmoothedTangents = true;
                    continue;
                } // lightFallofImage <imageprogram>
                // specifies the image to use for the third axis of projected
                // light volumes
                else if (0 == token.Icmp("lightFalloffImage")) {
                    str = R_ParsePastImageProgram(src);
                    String copy;

                    copy = str;	// so other things don't step on it
                    lightFalloffImage = globalImages.ImageFromFile(copy, TF_DEFAULT, false, TR_CLAMP /* TR_CLAMP_TO_ZERO */, TD_DEFAULT);
                    continue;
                } // guisurf <guifile> | guisurf entity
                // an entity guisurf must have an idUserInterface
                // specified in the renderEntity
                else if (0 == token.Icmp("guisurf")) {
                    src.ReadTokenOnLine(token);
                    if (0 == token.Icmp("entity")) {
                        entityGui = 1;
                    } else if (0 == token.Icmp("entity2")) {
                        entityGui = 2;
                    } else if (0 == token.Icmp("entity3")) {
                        entityGui = 3;
                    } else {
                        gui = uiManager.FindGui(token.toString(), true);
                    }
                    continue;
                } // sort
                else if (0 == token.Icmp("sort")) {
                    ParseSort(src);
                    continue;
                } // spectrum <integer>
                else if (0 == token.Icmp("spectrum")) {
                    src.ReadTokenOnLine(token);
                    spectrum = atoi(token.toString());
                    continue;
                } // deform < sprite | tube | flare >
                else if (0 == token.Icmp("deform")) {
                    ParseDeform(src);
                    continue;
                } // decalInfo <staySeconds> <fadeSeconds> ( <start rgb> ) ( <end rgb> )
                else if (0 == token.Icmp("decalInfo")) {
                    ParseDecalInfo(src);
                    continue;
                } // renderbump <args...>
                else if (0 == token.Icmp("renderbump")) {
                    src.ParseRestOfLine(renderBump);
                    continue;
                } // diffusemap for stage shortcut
                else if (0 == token.Icmp("diffusemap")) {
                    str = R_ParsePastImageProgram(src);
                    idStr.snPrintf(buffer, buffer.length, "blend diffusemap\nmap %s\n}\n", str);
                    newSrc.LoadMemory(ctos(buffer), strLen(buffer), "diffusemap");
                    newSrc.SetFlags(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);
                    ParseStage(newSrc, trpDefault);
                    newSrc.FreeSource();
                    continue;
                } // specularmap for stage shortcut
                else if (0 == token.Icmp("specularmap")) {
                    str = R_ParsePastImageProgram(src);
                    idStr.snPrintf(buffer, buffer.length, "blend specularmap\nmap %s\n}\n", str);
                    newSrc.LoadMemory(ctos(buffer), strLen(buffer), "specularmap");
                    newSrc.SetFlags(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);
                    ParseStage(newSrc, trpDefault);
                    newSrc.FreeSource();
                    continue;
                } // normalmap for stage shortcut
                else if (0 == token.Icmp("bumpmap")) {
                    str = R_ParsePastImageProgram(src);
                    idStr.snPrintf(buffer, buffer.length, "blend bumpmap\nmap %s\n}\n", str);
                    newSrc.LoadMemory(ctos(buffer), strLen(buffer), "bumpmap");
                    newSrc.SetFlags(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);
                    ParseStage(newSrc, trpDefault);
                    newSrc.FreeSource();
                    continue;
                } // DECAL_MACRO for backwards compatibility with the preprocessor macros
                else if (0 == token.Icmp("DECAL_MACRO")) {
                    // polygonOffset
                    SetMaterialFlag(MF_POLYGONOFFSET);
                    polygonOffset = 1;

                    // discrete
                    surfaceFlags |= SURF_DISCRETE;
                    contentFlags &= ~CONTENTS_SOLID;

                    // sort decal
                    sort = SS_DECAL;

                    // noShadows
                    SetMaterialFlag(MF_NOSHADOWS);
                    continue;
                } else if (token.equals("{")) {
                    // create the new stage
                    DBG_ParseStage++;
                    if (DBG_ParseStage == 41) {//
//                        continue;
                    }
                    ParseStage(src, trpDefault);
                    continue;
                } else {
                    common.Warning("unknown general material parameter '%s' in '%s'", token.toString(), GetName());
                    SetMaterialFlag(MF_DEFAULTED);
                    return;
                }
            }

            // add _flat or _white stages if needed
            AddImplicitStages();

            // order the diffuse / bump / specular stages properly
            SortInteractionStages();

            // if we need to do anything with normals (lighting or environment mapping)
            // and two sided lighting was asked for, flag
            // shouldCreateBackSides() and change culling back to single sided,
            // so we get proper tangent vectors on both sides
            // we can't just call ReceivesLighting(), because the stages are still
            // in temporary form
            if (cullType == CT_TWO_SIDED) {
                for (i = 0; i < numStages; i++) {
                    if (pd.parseStages[i].lighting != SL_AMBIENT || pd.parseStages[i].texture.texgen != TG_EXPLICIT) {
                        if (cullType == CT_TWO_SIDED) {
                            cullType = CT_FRONT_SIDED;
                            shouldCreateBackSides = true;
                        }
                        break;
                    }
                }
            }

            // currently a surface can only have one unique texgen for all the stages on old hardware
            texgen_t firstGen = TG_EXPLICIT;
            for (i = 0; i < numStages; i++) {
                if (pd.parseStages[i].texture.texgen != TG_EXPLICIT) {
                    if (firstGen == TG_EXPLICIT) {
                        firstGen = pd.parseStages[i].texture.texgen;
                    } else if (firstGen != pd.parseStages[i].texture.texgen) {
                        common.Warning("material '%s' has multiple stages with a texgen", GetName());
                        break;
                    }
                }
            }
        }
        public static int DBG_ParseStage = 0;

        /*
         ===============
         idMaterial::MatchToken

         Sets defaultShader and returns false if the next token doesn't match
         ===============
         */
        private boolean MatchToken(idLexer src, final String match) {
            if (!src.ExpectTokenString(match)) {
                SetMaterialFlag(MF_DEFAULTED);
                return false;
            }
            return true;
        }

        private void ParseSort(idLexer src) {
            idToken token = new idToken();

            if (!src.ReadTokenOnLine(token)) {
                src.Warning("missing sort parameter");
                SetMaterialFlag(MF_DEFAULTED);
                return;
            }

            if (0 == token.Icmp("subview")) {
                sort = SS_SUBVIEW;
            } else if (0 == token.Icmp("opaque")) {
                sort = SS_OPAQUE;
            } else if (0 == token.Icmp("decal")) {
                sort = SS_DECAL;
            } else if (0 == token.Icmp("far")) {
                sort = SS_FAR;
            } else if (0 == token.Icmp("medium")) {
                sort = SS_MEDIUM;
            } else if (0 == token.Icmp("close")) {
                sort = SS_CLOSE;
            } else if (0 == token.Icmp("almostNearest")) {
                sort = SS_ALMOST_NEAREST;
            } else if (0 == token.Icmp("nearest")) {
                sort = SS_NEAREST;
            } else if (0 == token.Icmp("postProcess")) {
                sort = SS_POST_PROCESS;
            } else if (0 == token.Icmp("portalSky")) {
                sort = SS_PORTAL_SKY;
            } else {
                sort = Float.parseFloat(token.toString());
            }
        }

        private static int DBG_ParseBlend = 0;
        private void ParseBlend(idLexer src, shaderStage_t stage) {
            idToken token = new idToken();
            int srcBlend, dstBlend;
            
//            System.out.printf("ParseBlend(%d)\n", DBG_ParseBlend++);

            if (!src.ReadToken(token)) {
                return;
            }

            // blending combinations
            if (0 == token.Icmp("blend")) {DBG_ParseBlend++;
                stage.drawStateBits = GLS_SRCBLEND_SRC_ALPHA | GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA;
                return;
            }
            if (0 == token.Icmp("add")) {
                stage.drawStateBits = GLS_SRCBLEND_ONE | GLS_DSTBLEND_ONE;
                return;
            }
            if (0 == token.Icmp("filter") || 0 == token.Icmp("modulate")) {
                stage.drawStateBits = GLS_SRCBLEND_DST_COLOR | GLS_DSTBLEND_ZERO;
                return;
            }
            if (0 == token.Icmp("none")) {
                // none is used when defining an alpha mask that doesn't draw
                stage.drawStateBits = GLS_SRCBLEND_ZERO | GLS_DSTBLEND_ONE;
                return;
            }
            if (0 == token.Icmp("bumpmap")) {
                stage.lighting = SL_BUMP;
                return;
            }
            if (0 == token.Icmp("diffusemap")) {
                stage.lighting = SL_DIFFUSE;
                return;
            }
            if (0 == token.Icmp("specularmap")) {
                stage.lighting = SL_SPECULAR;
                return;
            }

            srcBlend = NameToSrcBlendMode(token);

            MatchToken(src, ",");
            if (!src.ReadToken(token)) {
                return;
            }
            dstBlend = NameToDstBlendMode(token);

            stage.drawStateBits = srcBlend | dstBlend;
        }

        /*
         ================
         idMaterial::ParseVertexParm

         If there is a single value, it will be repeated across all elements
         If there are two values, 3 = 0.0, 4 = 1.0
         if there are three values, 4 = 1.0
         ================
         */
        private void ParseVertexParm(idLexer src, newShaderStage_t newStage) {
            idToken token = new idToken();

            src.ReadTokenOnLine(token);
            int parm = token.GetIntValue();
            if (!token.IsNumeric() || parm < 0 || parm >= MAX_VERTEX_PARMS) {
                common.Warning("bad vertexParm number\n");
                SetMaterialFlag(MF_DEFAULTED);
                return;
            }
            if (parm >= newStage.numVertexParms) {
                newStage.numVertexParms = parm + 1;
            }

            newStage.vertexParms[parm][0] = ParseExpression(src);

            src.ReadTokenOnLine(token);
            if (token.IsEmpty() || token.Icmp(",") != 0) {
                newStage.vertexParms[parm][1]
                        = newStage.vertexParms[parm][2]
                        = newStage.vertexParms[parm][3] = newStage.vertexParms[parm][0];
                return;
            }

            newStage.vertexParms[parm][1] = ParseExpression(src);

            src.ReadTokenOnLine(token);
            if (token.IsEmpty() || token.Icmp(",") != 0) {
                newStage.vertexParms[parm][2] = GetExpressionConstant(0);
                newStage.vertexParms[parm][3] = GetExpressionConstant(1);
                return;
            }

            newStage.vertexParms[parm][2] = ParseExpression(src);

            src.ReadTokenOnLine(token);
            if (token.IsEmpty() || token.Icmp(",") != 0) {
                newStage.vertexParms[parm][3] = GetExpressionConstant(1);
                return;
            }

            newStage.vertexParms[parm][3] = ParseExpression(src);
        }

        private void ParseFragmentMap(idLexer src, newShaderStage_t newStage) {
            String str;
            textureFilter_t tf;
            textureRepeat_t trp;
            textureDepth_t td;
            cubeFiles_t cubeMap;
            boolean allowPicmip;
            idToken token = new idToken();

            tf = TF_DEFAULT;
            trp = TR_REPEAT;
            td = TD_DEFAULT;
            allowPicmip = true;
            cubeMap = CF_2D;

            src.ReadTokenOnLine(token);
            int unit = token.GetIntValue();
            if (!token.IsNumeric() || unit < 0 || unit >= MAX_FRAGMENT_IMAGES) {
                common.Warning("bad fragmentMap number\n");
                SetMaterialFlag(MF_DEFAULTED);
                return;
            }

            // unit 1 is the normal map.. make sure it gets flagged as the proper depth
            if (unit == 1) {
                td = TD_BUMP;
            }

            if (unit >= newStage.numFragmentProgramImages) {
                newStage.numFragmentProgramImages = unit + 1;
            }

            while (true) {
                src.ReadTokenOnLine(token);

                if (0 == token.Icmp("cubeMap")) {
                    cubeMap = CF_NATIVE;
                    continue;
                }
                if (0 == token.Icmp("cameraCubeMap")) {
                    cubeMap = CF_CAMERA;
                    continue;
                }
                if (0 == token.Icmp("nearest")) {
                    tf = TF_NEAREST;
                    continue;
                }
                if (0 == token.Icmp("linear")) {
                    tf = TF_LINEAR;
                    continue;
                }
                if (0 == token.Icmp("clamp")) {
                    trp = TR_CLAMP;
                    continue;
                }
                if (0 == token.Icmp("noclamp")) {
                    trp = TR_REPEAT;
                    continue;
                }
                if (0 == token.Icmp("zeroclamp")) {
                    trp = TR_CLAMP_TO_ZERO;
                    continue;
                }
                if (0 == token.Icmp("alphazeroclamp")) {
                    trp = TR_CLAMP_TO_ZERO_ALPHA;
                    continue;
                }
                if (0 == token.Icmp("forceHighQuality")) {
                    td = TD_HIGH_QUALITY;
                    continue;
                }

                if (0 == token.Icmp("uncompressed") || 0 == token.Icmp("highquality")) {
                    if (0 == globalImages.image_ignoreHighQuality.GetInteger()) {
                        td = TD_HIGH_QUALITY;
                    }
                    continue;
                }
                if (0 == token.Icmp("nopicmip")) {
                    allowPicmip = false;
                    continue;
                }

                // assume anything else is the image name
                src.UnreadToken(token);
                break;
            }
            str = R_ParsePastImageProgram(src);

            newStage.fragmentProgramImages[unit]
                    = globalImages.ImageFromFile(str, tf, allowPicmip, trp, td, cubeMap);
            if (null == newStage.fragmentProgramImages[unit]) {
                newStage.fragmentProgramImages[unit] = globalImages.defaultImage;
            }
        }

        /*
         =================
         idMaterial::ParseStage

         An open brace has been parsed


         {
         if <expression>
         map <imageprogram>
         "nearest" "linear" "clamp" "zeroclamp" "uncompressed" "highquality" "nopicmip"
         scroll, scale, rotate
         }

         =================
         */ static int DEBUG_imageName = 0;
         private static int DEBUG_ParseStage = 0;

        private void ParseStage(idLexer src, final textureRepeat_t trpDefault /*= TR_REPEAT */) {
            DEBUG_imageName++;
            idToken token = new idToken();
            String str;
            shaderStage_t ss;
            textureStage_t ts;
            textureFilter_t tf;
            textureRepeat_t trp;
            textureDepth_t td;
            cubeFiles_t cubeMap;
            boolean allowPicmip;
            char[] imageName = new char[MAX_IMAGE_NAME];
            int a, b;
            int[][] matrix = new int[2][3];
            newShaderStage_t newStage = new newShaderStage_t();

            if (numStages >= MAX_SHADER_STAGES) {
                SetMaterialFlag(MF_DEFAULTED);
                common.Warning("material '%s' exceeded %d stages", GetName(), MAX_SHADER_STAGES);
            }

            tf = TF_DEFAULT;
            trp = trpDefault;
            td = TD_DEFAULT;
            allowPicmip = true;
            cubeMap = CF_2D;

            imageName[0] = 0;

//	memset( &newStage, 0, sizeof( newStage ) );
            ss = pd.parseStages[numStages];
            ts = ss.texture;

            ClearStage(ss);

            int asdasdasdasd = 0;
            if (DBG_ParseStage == 41) {
                asdasdasdasd = 0;
            }
            while (true) {
                if (TestMaterialFlag(MF_DEFAULTED)) {	// we have a parse error
                    return;
                }
                if (!src.ExpectAnyToken(token)) {
                    SetMaterialFlag(MF_DEFAULTED);
                    return;
                }

                // the close brace for the entire material ends the draw block
                if (token.equals("}")) {
                    break;
                }

                //BSM Nerve: Added for stage naming in the material editor
                if (0 == token.Icmp("name")) {
                    src.SkipRestOfLine();
                    continue;
                }

                // image options
                if (0 == token.Icmp("blend")) {
                    ParseBlend(src, ss);
                    continue;
                }

                if (0 == token.Icmp("map")) {
                    str = R_ParsePastImageProgram(src);
                    idStr.Copynz(imageName, str, imageName.length);
                    continue;
                }

                if (0 == token.Icmp("remoteRenderMap")) {
                    ts.dynamic = DI_REMOTE_RENDER;
                    ts.width = src.ParseInt();
                    ts.height = src.ParseInt();
                    continue;
                }

                if (0 == token.Icmp("mirrorRenderMap")) {
                    ts.dynamic = DI_MIRROR_RENDER;
                    ts.width = src.ParseInt();
                    ts.height = src.ParseInt();
                    ts.texgen = TG_SCREEN;
                    continue;
                }

                if (0 == token.Icmp("xrayRenderMap")) {
                    ts.dynamic = DI_XRAY_RENDER;
                    ts.width = src.ParseInt();
                    ts.height = src.ParseInt();
                    ts.texgen = TG_SCREEN;
                    continue;
                }
                if (0 == token.Icmp("screen")) {
                    ts.texgen = TG_SCREEN;
                    continue;
                }
                if (0 == token.Icmp("screen2")) {
                    ts.texgen = TG_SCREEN2;
                    continue;
                }
                if (0 == token.Icmp("glassWarp")) {
                    ts.texgen = TG_GLASSWARP;
                    continue;
                }

                if (0 == token.Icmp("videomap")) {
                    // note that videomaps will always be in clamp mode, so texture
                    // coordinates had better be in the 0 to 1 range
                    if (!src.ReadToken(token)) {
                        common.Warning("missing parameter for 'videoMap' keyword in material '%s'", GetName());
                        continue;
                    }
                    boolean loop = false;
                    if (0 == token.Icmp("loop")) {
                        loop = true;
                        if (!src.ReadToken(token)) {
                            common.Warning("missing parameter for 'videoMap' keyword in material '%s'", GetName());
                            continue;
                        }
                    }
                    ts.cinematic[0] = idCinematic.Alloc();
                    ts.cinematic[0].InitFromFile(token.toString(), loop);
                    continue;
                }

                if (0 == token.Icmp("soundmap")) {
                    if (!src.ReadToken(token)) {
                        common.Warning("missing parameter for 'soundmap' keyword in material '%s'", GetName());
                        continue;
                    }
                    ts.cinematic[0] = new idSndWindow();
                    ts.cinematic[0].InitFromFile(token.toString(), true);
                    continue;
                }

                if (0 == token.Icmp("cubeMap")) {
                    str = R_ParsePastImageProgram(src);
                    idStr.Copynz(imageName, str, imageName.length);
                    cubeMap = CF_NATIVE;
                    continue;
                }

                if (0 == token.Icmp("cameraCubeMap")) {
                    str = R_ParsePastImageProgram(src);
                    idStr.Copynz(imageName, str, imageName.length);
                    cubeMap = CF_CAMERA;
                    continue;
                }

                if (0 == token.Icmp("ignoreAlphaTest")) {
                    ss.ignoreAlphaTest = true;
                    continue;
                }
                if (0 == token.Icmp("nearest")) {
                    tf = TF_NEAREST;
                    continue;
                }
                if (0 == token.Icmp("linear")) {
                    tf = TF_LINEAR;
                    continue;
                }
                if (0 == token.Icmp("clamp")) {
                    trp = TR_CLAMP;
                    continue;
                }
                if (0 == token.Icmp("noclamp")) {
                    trp = TR_REPEAT;
                    continue;
                }
                if (0 == token.Icmp("zeroclamp")) {
                    trp = TR_CLAMP_TO_ZERO;
                    continue;
                }
                if (0 == token.Icmp("alphazeroclamp")) {
                    trp = TR_CLAMP_TO_ZERO_ALPHA;
                    continue;
                }
                if (0 == token.Icmp("uncompressed") || 0 == token.Icmp("highquality")) {
                    if (0 == globalImages.image_ignoreHighQuality.GetInteger()) {
                        td = TD_HIGH_QUALITY;
                    }
                    continue;
                }
                if (0 == token.Icmp("forceHighQuality")) {
                    td = TD_HIGH_QUALITY;
                    continue;
                }
                if (0 == token.Icmp("nopicmip")) {
                    allowPicmip = false;
                    continue;
                }
                if (0 == token.Icmp("vertexColor")) {
                    ss.vertexColor = SVC_MODULATE;
                    continue;
                }
                if (0 == token.Icmp("inverseVertexColor")) {
                    ss.vertexColor = SVC_INVERSE_MODULATE;
                    continue;
                } // privatePolygonOffset
                else if (0 == token.Icmp("privatePolygonOffset")) {
                    if (!src.ReadTokenOnLine(token)) {
                        ss.privatePolygonOffset = 1;
                        continue;
                    }
                    // explict larger (or negative) offset
                    src.UnreadToken(token);
                    ss.privatePolygonOffset = src.ParseFloat();
                    continue;
                }

                // texture coordinate generation
                if (0 == token.Icmp("texGen")) {
                    src.ExpectAnyToken(token);
                    if (0 == token.Icmp("normal")) {
                        ts.texgen = TG_DIFFUSE_CUBE;
                    } else if (0 == token.Icmp("reflect")) {
                        ts.texgen = TG_REFLECT_CUBE;
                    } else if (0 == token.Icmp("skybox")) {
                        ts.texgen = TG_SKYBOX_CUBE;
                    } else if (0 == token.Icmp("wobbleSky")) {
                        ts.texgen = TG_WOBBLESKY_CUBE;
                        texGenRegisters[0] = ParseExpression(src);
                        texGenRegisters[1] = ParseExpression(src);
                        texGenRegisters[2] = ParseExpression(src);
                    } else {
                        common.Warning("bad texGen '%s' in material %s", token.toString(), GetName());
                        SetMaterialFlag(MF_DEFAULTED);
                    }
                    continue;
                }
                if (0 == token.Icmp("scroll") || 0 == token.Icmp("translate")) {

                    a = ParseExpression(src);
                    MatchToken(src, ",");
                    if (DBG_ParseStage == 41) {
                        int aa = aa = 0;
                        b = ParseExpression(src);
                    } else {
                        b = ParseExpression(src);
                    }
                    matrix[0][0] = GetExpressionConstant(1);
                    matrix[0][1] = GetExpressionConstant(0);
                    matrix[0][2] = a;
                    matrix[1][0] = GetExpressionConstant(0);
                    matrix[1][1] = GetExpressionConstant(1);
                    matrix[1][2] = b;

                    MultiplyTextureMatrix(ts, matrix);//HACKME::3:scrolling screws up our beloved logo. For now.
                    continue;
                }
                if (0 == token.Icmp("scale")) {
                    a = ParseExpression(src);
                    MatchToken(src, ",");
                    b = ParseExpression(src);
                    // this just scales without a centering
                    matrix[0][0] = a;
                    matrix[0][1] = GetExpressionConstant(0);
                    matrix[0][2] = GetExpressionConstant(0);
                    matrix[1][0] = GetExpressionConstant(0);
                    matrix[1][1] = b;
                    matrix[1][2] = GetExpressionConstant(0);

                    MultiplyTextureMatrix(ts, matrix);
                    continue;
                }
                if (0 == token.Icmp("centerScale")) {
                    a = ParseExpression(src);
                    MatchToken(src, ",");
                    b = ParseExpression(src);
                    // this subtracts 0.5, then scales, then adds 0.5
                    matrix[0][0] = a;
                    matrix[0][1] = GetExpressionConstant(0);
                    matrix[0][2] = EmitOp(GetExpressionConstant(0.5f), EmitOp(GetExpressionConstant(0.5f), a, OP_TYPE_MULTIPLY), OP_TYPE_SUBTRACT);
                    matrix[1][0] = GetExpressionConstant(0);
                    matrix[1][1] = b;
                    matrix[1][2] = EmitOp(GetExpressionConstant(0.5f), EmitOp(GetExpressionConstant(0.5f), b, OP_TYPE_MULTIPLY), OP_TYPE_SUBTRACT);

                    MultiplyTextureMatrix(ts, matrix);
                    continue;
                }
                if (0 == token.Icmp("shear")) {
                    a = ParseExpression(src);
                    MatchToken(src, ",");
                    b = ParseExpression(src);
                    // this subtracts 0.5, then shears, then adds 0.5
                    matrix[0][0] = GetExpressionConstant(1);
                    matrix[0][1] = a;
                    matrix[0][2] = EmitOp(GetExpressionConstant(-0.5f), a, OP_TYPE_MULTIPLY);
                    matrix[1][0] = b;
                    matrix[1][1] = GetExpressionConstant(1);
                    matrix[1][2] = EmitOp(GetExpressionConstant(-0.5f), b, OP_TYPE_MULTIPLY);

                    MultiplyTextureMatrix(ts, matrix);
                    continue;
                }
                if (0 == token.Icmp("rotate")) {
                    idDeclTable table;
                    int sinReg, cosReg;

                    // in cycles
                    a = ParseExpression(src);

                    table = (idDeclTable) declManager.FindType(DECL_TABLE, "sinTable", false);
                    if (null == table) {
                        common.Warning("no sinTable for rotate defined");
                        SetMaterialFlag(MF_DEFAULTED);
                        return;
                    }
                    sinReg = EmitOp(table.Index(), a, OP_TYPE_TABLE);

                    table = (idDeclTable) declManager.FindType(DECL_TABLE, "cosTable", false);
                    if (null == table) {
                        common.Warning("no cosTable for rotate defined");
                        SetMaterialFlag(MF_DEFAULTED);
                        return;
                    }
                    cosReg = EmitOp(table.Index(), a, OP_TYPE_TABLE);

                    // this subtracts 0.5, then rotates, then adds 0.5
                    matrix[0][0] = cosReg;
                    matrix[0][1] = EmitOp(GetExpressionConstant(0), sinReg, OP_TYPE_SUBTRACT);
                    matrix[0][2] = recursiveEmitOp(0.5f, 0.5f, -0.5f, cosReg, sinReg);

                    matrix[1][0] = sinReg;
                    matrix[1][1] = cosReg;
                    matrix[1][2] = recursiveEmitOp(0.5f, -0.5f, -0.5f, sinReg, cosReg);

                    MultiplyTextureMatrix(ts, matrix);
                    continue;
                }

                // color mask options
                if (0 == token.Icmp("maskRed")) {
                    ss.drawStateBits |= GLS_REDMASK;
                    continue;
                }
                if (0 == token.Icmp("maskGreen")) {
                    ss.drawStateBits |= GLS_GREENMASK;
                    continue;
                }
                if (0 == token.Icmp("maskBlue")) {
                    ss.drawStateBits |= GLS_BLUEMASK;
                    continue;
                }
                if (0 == token.Icmp("maskAlpha")) {
                    ss.drawStateBits |= GLS_ALPHAMASK;
                    continue;
                }
                if (0 == token.Icmp("maskColor")) {
                    ss.drawStateBits |= GLS_COLORMASK;
                    continue;
                }
                if (0 == token.Icmp("maskDepth")) {
                    ss.drawStateBits |= GLS_DEPTHMASK;
                    continue;
                }
                if (0 == token.Icmp("alphaTest")) {
                    ss.hasAlphaTest = true;
                    ss.alphaTestRegister = ParseExpression(src);
                    coverage = MC_PERFORATED;
                    continue;
                }

                // shorthand for 2D modulated
                if (0 == token.Icmp("colored")) {
                    ss.color.registers[0] = etoi(EXP_REG_PARM0);
                    ss.color.registers[1] = etoi(EXP_REG_PARM1);
                    ss.color.registers[2] = etoi(EXP_REG_PARM2);
                    ss.color.registers[3] = etoi(EXP_REG_PARM3);
                    pd.registersAreConstant = false;
                    continue;
                }

                if (0 == token.Icmp("color")) {
                    ss.color.registers[0] = ParseExpression(src);
                    MatchToken(src, ",");
                    ss.color.registers[1] = ParseExpression(src);
                    MatchToken(src, ",");
                    ss.color.registers[2] = ParseExpression(src);
                    MatchToken(src, ",");
                    ss.color.registers[3] = ParseExpression(src);
                    continue;
                }
                if (0 == token.Icmp("red")) {
                    ss.color.registers[0] = ParseExpression(src);
                    continue;
                }
                if (0 == token.Icmp("green")) {
                    ss.color.registers[1] = ParseExpression(src);
                    continue;
                }
                if (0 == token.Icmp("blue")) {
                    ss.color.registers[2] = ParseExpression(src);
                    continue;
                }
                if (0 == token.Icmp("alpha")) {
                    DEBUG_ParseStage++;
                    ss.color.registers[3] = ParseExpression(src);
//                    System.out.printf("alpha=>%d\n", ss.color.registers[3]);
                    int s = ss.color.registers[3];                    
                    continue;
                }
                if (0 == token.Icmp("rgb")) {
                    ss.color.registers[0] = ss.color.registers[1]
                            = ss.color.registers[2] = ParseExpression(src);
                    continue;
                }
                if (0 == token.Icmp("rgba")) {
                    ss.color.registers[0] = ss.color.registers[1]
                            = ss.color.registers[2] = ss.color.registers[3] = ParseExpression(src);
                    continue;
                }

                if (0 == token.Icmp("if")) {
                    ss.conditionRegister = ParseExpression(src);
                    continue;
                }
                if (0 == token.Icmp("program")) {
                    if (src.ReadTokenOnLine(token)) {
                        newStage.vertexProgram = R_FindARBProgram(GL_VERTEX_PROGRAM_ARB, token.toString());
                        newStage.fragmentProgram = R_FindARBProgram(GL_FRAGMENT_PROGRAM_ARB, token.toString());
                    }
                    continue;
                }
                if (0 == token.Icmp("fragmentProgram")) {
                    if (src.ReadTokenOnLine(token)) {
                        newStage.fragmentProgram = R_FindARBProgram(GL_FRAGMENT_PROGRAM_ARB, token.toString());
                    }
                    continue;
                }
                if (0 == token.Icmp("vertexProgram")) {
                    if (src.ReadTokenOnLine(token)) {
                        newStage.vertexProgram = R_FindARBProgram(GL_VERTEX_PROGRAM_ARB, token.toString());
                    }
                    continue;
                }
                if (0 == token.Icmp("megaTexture")) {
                    if (src.ReadTokenOnLine(token)) {
                        newStage.megaTexture = new idMegaTexture();
                        if (!newStage.megaTexture.InitFromMegaFile(token.toString())) {
//					delete newStage.megaTexture;
                            newStage.megaTexture = null;
                            SetMaterialFlag(MF_DEFAULTED);
                            continue;
                        }
                        newStage.vertexProgram = R_FindARBProgram(GL_VERTEX_PROGRAM_ARB, "megaTexture.vfp");
                        newStage.fragmentProgram = R_FindARBProgram(GL_FRAGMENT_PROGRAM_ARB, "megaTexture.vfp");
                        continue;
                    }
                }

                if (0 == token.Icmp("vertexParm")) {
                    ParseVertexParm(src, newStage);
                    continue;
                }

                if (0 == token.Icmp("fragmentMap")) {
                    ParseFragmentMap(src, newStage);
                    continue;
                }

                common.Warning("unknown token '%s' in material '%s'", token.toString(), GetName());
                SetMaterialFlag(MF_DEFAULTED);
                return;
            }

            // if we are using newStage, allocate a copy of it
            if (newStage.fragmentProgram != 0 || newStage.vertexProgram != 0) {
///		ss.newStage = (newShaderStage_t )Mem_Alloc( sizeof( newStage ) );
                ss.newStage = newStage;
            }

            // successfully parsed a stage
            numStages++;

            // select a compressed depth based on what the stage is
            if (td == TD_DEFAULT) {
                switch (ss.lighting) {
                    case SL_BUMP:
                        td = TD_BUMP;
                        break;
                    case SL_DIFFUSE:
                        td = TD_DIFFUSE;
                        break;
                    case SL_SPECULAR:
                        td = TD_SPECULAR;
                        break;
                    default:
                        break;
                }
            }

            // now load the image with all the parms we parsed
            if (strLen(imageName) > 0) {
                DEBUG_imageName += 0;
                ts.image[0] = globalImages.ImageFromFile(ctos(imageName), tf, allowPicmip, trp, td, cubeMap);
                if (null == ts.image[0]) {
                    ts.image[0] = globalImages.defaultImage;
                }
            } else if (NOT(ts.cinematic[0]) && NOT(ts.dynamic) && NOT(ss.newStage)) {
                common.Warning("material '%s' had stage with no image", GetName());
                ts.image[0] = globalImages.defaultImage;
            }
        }

        private void ParseStage(idLexer src) {
            ParseStage(src, TR_REPEAT);
        }

        private void ParseDeform(idLexer src) {
            idToken token = new idToken();

            if (!src.ExpectAnyToken(token)) {
                return;
            }

            if (0 == token.Icmp("sprite")) {
                deform = DFRM_SPRITE;
                cullType = CT_TWO_SIDED;
                SetMaterialFlag(MF_NOSHADOWS);
                return;
            }
            if (0 == token.Icmp("tube")) {
                deform = DFRM_TUBE;
                cullType = CT_TWO_SIDED;
                SetMaterialFlag(MF_NOSHADOWS);
                return;
            }
            if (0 == token.Icmp("flare")) {
                deform = DFRM_FLARE;
                cullType = CT_TWO_SIDED;
                deformRegisters[0] = ParseExpression(src);
                SetMaterialFlag(MF_NOSHADOWS);
                return;
            }
            if (0 == token.Icmp("expand")) {
                deform = DFRM_EXPAND;
                deformRegisters[0] = ParseExpression(src);
                return;
            }
            if (0 == token.Icmp("move")) {
                deform = DFRM_MOVE;
                deformRegisters[0] = ParseExpression(src);
                return;
            }
            if (0 == token.Icmp("turbulent")) {
                deform = DFRM_TURB;

                if (!src.ExpectAnyToken(token)) {
                    src.Warning("deform particle missing particle name");
                    SetMaterialFlag(MF_DEFAULTED);
                    return;
                }
                deformDecl = declManager.FindType(DECL_TABLE, token, true);

                deformRegisters[0] = ParseExpression(src);
                deformRegisters[1] = ParseExpression(src);
                deformRegisters[2] = ParseExpression(src);
                return;
            }
            if (0 == token.Icmp("eyeBall")) {
                deform = DFRM_EYEBALL;
                return;
            }
            if (0 == token.Icmp("particle")) {
                deform = DFRM_PARTICLE;
                if (!src.ExpectAnyToken(token)) {
                    src.Warning("deform particle missing particle name");
                    SetMaterialFlag(MF_DEFAULTED);
                    return;
                }
                deformDecl = declManager.FindType(DECL_PARTICLE, token, true);
                return;
            }
            if (0 == token.Icmp("particle2")) {
                deform = DFRM_PARTICLE2;
                if (!src.ExpectAnyToken(token)) {
                    src.Warning("deform particle missing particle name");
                    SetMaterialFlag(MF_DEFAULTED);
                    return;
                }
                deformDecl = declManager.FindType(DECL_PARTICLE, token, true);
                return;
            }
            src.Warning("Bad deform type '%s'", token.toString());
            SetMaterialFlag(MF_DEFAULTED);
        }

        private void ParseDecalInfo(idLexer src) {
//	idToken token;

            decalInfo.stayTime = (int) src.ParseFloat() * 1000;
            decalInfo.fadeTime = (int) src.ParseFloat() * 1000;
            final float[] start = new float[4], end = new float[4];
            src.Parse1DMatrix(4, start);
            src.Parse1DMatrix(4, end);
            for (int i = 0; i < 4; i++) {
                decalInfo.start[i] = start[i];
                decalInfo.end[i] = end[i];
            }
        }

        public void oSet(idMaterial FindMaterial) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        // info parms
        static class infoParm_t {

            public infoParm_t(String name, int clearSolid, int surfaceFlags, int contents) {
                this.name = name;
                this.clearSolid = clearSolid;
                this.surfaceFlags = surfaceFlags;
                this.contents = contents;
            }

            public infoParm_t(String name, int clearSolid, surfTypes_t surfaceFlags, int contents) {
                this.name = name;
                this.clearSolid = clearSolid;
                this.surfaceFlags = surfaceFlags.ordinal();
                this.contents = contents;
            }
            String name;
            int clearSolid, surfaceFlags, contents;
        };
        static final infoParm_t[] infoParms = {
            // game relevant attributes
            new infoParm_t("solid", 0, 0, CONTENTS_SOLID), // may need to override a clearSolid
            new infoParm_t("water", 1, 0, CONTENTS_WATER), // used for water
            new infoParm_t("playerclip", 0, 0, CONTENTS_PLAYERCLIP), // solid to players
            new infoParm_t("monsterclip", 0, 0, CONTENTS_MONSTERCLIP), // solid to monsters
            new infoParm_t("moveableclip", 0, 0, CONTENTS_MOVEABLECLIP),// solid to moveable entities
            new infoParm_t("ikclip", 0, 0, CONTENTS_IKCLIP), // solid to IK
            new infoParm_t("blood", 0, 0, CONTENTS_BLOOD), // used to detect blood decals
            new infoParm_t("trigger", 0, 0, CONTENTS_TRIGGER), // used for triggers
            new infoParm_t("aassolid", 0, 0, CONTENTS_AAS_SOLID), // solid for AAS
            new infoParm_t("aasobstacle", 0, 0, CONTENTS_AAS_OBSTACLE),// used to compile an obstacle into AAS that can be enabled/disabled
            new infoParm_t("flashlight_trigger", 0, 0, CONTENTS_FLASHLIGHT_TRIGGER), // used for triggers that are activated by the flashlight
            new infoParm_t("nonsolid", 1, 0, 0), // clears the solid flag
            new infoParm_t("nullNormal", 0, SURF_NULLNORMAL, 0), // renderbump will draw as 0x80 0x80 0x80
            //
            // utility relevant attributes
            new infoParm_t("areaportal", 1, 0, CONTENTS_AREAPORTAL), // divides areas
            new infoParm_t("qer_nocarve", 1, 0, CONTENTS_NOCSG), // don't cut brushes in editor
            //
            new infoParm_t("discrete", 1, SURF_DISCRETE, 0), // surfaces should not be automatically merged together or
            /////////////////////////////////////////////////// clipped to the world,
            /////////////////////////////////////////////////// because they represent discrete objects like gui shaders
            /////////////////////////////////////////////////// mirrors, or autosprites
            new infoParm_t("noFragment", 0, SURF_NOFRAGMENT, 0),
            //
            new infoParm_t("slick", 0, SURF_SLICK, 0),
            new infoParm_t("collision", 0, SURF_COLLISION, 0),
            new infoParm_t("noimpact", 0, SURF_NOIMPACT, 0), // don't make impact explosions or marks
            new infoParm_t("nodamage", 0, SURF_NODAMAGE, 0), // no falling damage when hitting
            new infoParm_t("ladder", 0, SURF_LADDER, 0), // climbable
            new infoParm_t("nosteps", 0, SURF_NOSTEPS, 0), // no footsteps
            //
            // material types for particle, sound, footstep feedback
            new infoParm_t("metal", 0, SURFTYPE_METAL, 0), // metal
            new infoParm_t("stone", 0, SURFTYPE_STONE, 0), // stone
            new infoParm_t("flesh", 0, SURFTYPE_FLESH, 0), // flesh
            new infoParm_t("wood", 0, SURFTYPE_WOOD, 0), // wood
            new infoParm_t("cardboard", 0, SURFTYPE_CARDBOARD, 0), // cardboard
            new infoParm_t("liquid", 0, SURFTYPE_LIQUID, 0), // liquid
            new infoParm_t("glass", 0, SURFTYPE_GLASS, 0), // glass
            new infoParm_t("plastic", 0, SURFTYPE_PLASTIC, 0), // plastic
            new infoParm_t("ricochet", 0, SURFTYPE_RICOCHET, 0), // behaves like metal but causes a ricochet sound
            //
            // unassigned surface types
            new infoParm_t("surftype10", 0, SURFTYPE_10, 0),
            new infoParm_t("surftype11", 0, SURFTYPE_11, 0),
            new infoParm_t("surftype12", 0, SURFTYPE_12, 0),
            new infoParm_t("surftype13", 0, SURFTYPE_13, 0),
            new infoParm_t("surftype14", 0, SURFTYPE_14, 0),
            new infoParm_t("surftype15", 0, SURFTYPE_15, 0)
        };
        static final int numInfoParms = infoParms.length;

        /*
         ===============
         idMaterial::CheckSurfaceParm

         See if the current token matches one of the surface parm bit flags
         ===============
         */
        private boolean CheckSurfaceParm(idToken token) {

            for (int i = 0; i < numInfoParms; i++) {
                if (0 == token.Icmp(infoParms[i].name)) {
                    if ((infoParms[i].surfaceFlags & SURF_TYPE_MASK) != 0) {
                        // ensure we only have one surface type set
                        surfaceFlags &= ~SURF_TYPE_MASK;
                    }
                    surfaceFlags |= infoParms[i].surfaceFlags;
                    contentFlags |= infoParms[i].contents;
                    if (infoParms[i].clearSolid != 0) {
                        contentFlags &= ~CONTENTS_SOLID;
                    }
                    return true;
                }
            }
            return false;
        }

        private int GetExpressionConstant(float f) {
            int i;

            for (i = EXP_REG_NUM_PREDEFINED.ordinal(); i < numRegisters; i++) {
                if (!pd.registerIsTemporary[i] && pd.shaderRegisters[i] == f) {
                    return i;
                }
            }
            if (numRegisters == MAX_EXPRESSION_REGISTERS) {
                common.Warning("GetExpressionConstant: material '%s' hit MAX_EXPRESSION_REGISTERS", GetName());
                SetMaterialFlag(MF_DEFAULTED);
                return 0;
            }
            pd.registerIsTemporary[i] = false;
            pd.shaderRegisters[i] = f;
//            if(dbg_count==131)
//            TempDump.printCallStack(dbg_count + "****************************" + numRegisters);
            numRegisters++;

            return i;
        }

        private int GetExpressionTemporary() {
            if (numRegisters == MAX_EXPRESSION_REGISTERS) {
                common.Warning("GetExpressionTemporary: material '%s' hit MAX_EXPRESSION_REGISTERS", GetName());
                SetMaterialFlag(MF_DEFAULTED);
                return 0;
            }
//            if(dbg_count==131)
//            TempDump.printCallStack(dbg_count + "****************************" + numRegisters);
            pd.registerIsTemporary[numRegisters] = true;
            numRegisters++;
            return numRegisters - 1;
        }

        private expOp_t GetExpressionOp() {
            if (numOps == MAX_EXPRESSION_OPS) {
                common.Warning("GetExpressionOp: material '%s' hit MAX_EXPRESSION_OPS", GetName());
                SetMaterialFlag(MF_DEFAULTED);
                return pd.shaderOps[0];
            }

            return pd.shaderOps[numOps++];
        }

        private int EmitOp(int a, int b, expOpType_t opType) {
            expOp_t op;

            // optimize away identity operations
            if (opType == OP_TYPE_ADD) {
                if (!pd.registerIsTemporary[a] && pd.shaderRegisters[a] == 0) {
                    return b;
                }
                if (!pd.registerIsTemporary[b] && pd.shaderRegisters[b] == 0) {
                    return a;
                }
                if (!pd.registerIsTemporary[a] && !pd.registerIsTemporary[b]) {
                    return GetExpressionConstant(pd.shaderRegisters[a] + pd.shaderRegisters[b]);
                }
            }
            if (opType == OP_TYPE_MULTIPLY) {
                if (!pd.registerIsTemporary[a] && pd.shaderRegisters[a] == 1) {
                    return b;
                }
                if (!pd.registerIsTemporary[a] && pd.shaderRegisters[a] == 0) {
                    return a;
                }
                if (!pd.registerIsTemporary[b] && pd.shaderRegisters[b] == 1) {
                    return a;
                }
                if (!pd.registerIsTemporary[b] && pd.shaderRegisters[b] == 0) {
                    return b;
                }
                if (!pd.registerIsTemporary[a] && !pd.registerIsTemporary[b]) {
                    return GetExpressionConstant(pd.shaderRegisters[a] * pd.shaderRegisters[b]);
                }
            }

            op = GetExpressionOp();
            op.opType = opType;
            op.a = a;
            op.b = b;
            op.c = GetExpressionTemporary();

            return op.c;
        }

        private int ParseEmitOp(idLexer src, int a, expOpType_t opType, int priority) {
            int b;

            b = ParseExpressionPriority(src, priority);
            return EmitOp(a, b, opType);
        }

        /*
         =================
         idMaterial::ParseTerm

         Returns a register index
         =================
         */
        private int ParseTerm(idLexer src) {
            idToken token = new idToken();
            int a, b;

            src.ReadToken(token);

            if (token.equals("(")) {
                a = ParseExpression(src);
                MatchToken(src, ")");
                return a;
            }

            if (0 == token.Icmp("time")) {
                pd.registersAreConstant = false;
                return EXP_REG_TIME.ordinal();
            }
            if (0 == token.Icmp("parm0")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM0.ordinal();
            }
            if (0 == token.Icmp("parm1")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM1.ordinal();
            }
            if (0 == token.Icmp("parm2")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM2.ordinal();
            }
            if (0 == token.Icmp("parm3")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM3.ordinal();
            }
            if (0 == token.Icmp("parm4")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM4.ordinal();
            }
            if (0 == token.Icmp("parm5")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM5.ordinal();
            }
            if (0 == token.Icmp("parm6")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM6.ordinal();
            }
            if (0 == token.Icmp("parm7")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM7.ordinal();
            }
            if (0 == token.Icmp("parm8")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM8.ordinal();
            }
            if (0 == token.Icmp("parm9")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM9.ordinal();
            }
            if (0 == token.Icmp("parm10")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM10.ordinal();
            }
            if (0 == token.Icmp("parm11")) {
                pd.registersAreConstant = false;
                return EXP_REG_PARM11.ordinal();
            }
            if (0 == token.Icmp("global0")) {
                pd.registersAreConstant = false;
                return EXP_REG_GLOBAL0.ordinal();
            }
            if (0 == token.Icmp("global1")) {
                pd.registersAreConstant = false;
                return EXP_REG_GLOBAL1.ordinal();
            }
            if (0 == token.Icmp("global2")) {
                pd.registersAreConstant = false;
                return EXP_REG_GLOBAL2.ordinal();
            }
            if (0 == token.Icmp("global3")) {
                pd.registersAreConstant = false;
                return EXP_REG_GLOBAL3.ordinal();
            }
            if (0 == token.Icmp("global4")) {
                pd.registersAreConstant = false;
                return EXP_REG_GLOBAL4.ordinal();
            }
            if (0 == token.Icmp("global5")) {
                pd.registersAreConstant = false;
                return EXP_REG_GLOBAL5.ordinal();
            }
            if (0 == token.Icmp("global6")) {
                pd.registersAreConstant = false;
                return EXP_REG_GLOBAL6.ordinal();
            }
            if (0 == token.Icmp("global7")) {
                pd.registersAreConstant = false;
                return EXP_REG_GLOBAL7.ordinal();
            }
            if (0 == token.Icmp("fragmentPrograms")) {
                return GetExpressionConstant(glConfig.ARBFragmentProgramAvailable ? 1 : 0);
            }

            if (0 == token.Icmp("sound")) {
                pd.registersAreConstant = false;
                return EmitOp(0, 0, OP_TYPE_SOUND);
            }

            // parse negative numbers
            if (token.equals("-")) {
                src.ReadToken(token);
                if (token.type == TT_NUMBER || token.equals(".")) {
                    return GetExpressionConstant(-(float) token.GetFloatValue());
                }
                src.Warning("Bad negative number '%s'", token);
                SetMaterialFlag(MF_DEFAULTED);
                return 0;
            }

            if (token.type == TT_NUMBER || token.equals(".") || token.equals("-")) {
                final int dbg_bla = GetExpressionConstant((float) token.GetFloatValue());
//                System.out.printf("TT_NUMBER = %d\n", dbg_bla);
                return dbg_bla;
            }

            // see if it is a table name
            final idDeclTable table = (idDeclTable) declManager.FindType(DECL_TABLE, token, false);
            if (null == table) {
                src.Warning("Bad term '%s'", token);
                SetMaterialFlag(MF_DEFAULTED);
                return 0;
            }

            // parse a table expression
            MatchToken(src, "[");

            b = ParseExpression(src);

            MatchToken(src, "]");

            return EmitOp(table.Index(), b, OP_TYPE_TABLE);
        }

        /*
         =================
         idMaterial::ParseExpressionPriority

         Returns a register index
         =================
         */
        static final int TOP_PRIORITY = 4;
        private static int DBG_ParseExpressionPriority = 0;

        private int ParseExpressionPriority(idLexer src, int priority) {
            idToken token = new idToken();
            int a;

            DBG_ParseExpressionPriority++;
//            if(DBG_ParseExpressionPriority==101)return 0;
            if (priority == 0) {
                return ParseTerm(src);
            }

            a = ParseExpressionPriority(src, priority - 1);

            if (TestMaterialFlag(MF_DEFAULTED)) {	// we have a parse error
                return 0;
            }

            if (!src.ReadToken(token)) {
                // we won't get EOF in a real file, but we can
                // when parsing from generated strings
                return a;
            }

            if (priority == 1 && token.equals("*")) {
                return ParseEmitOp(src, a, OP_TYPE_MULTIPLY, priority);
            }
            if (priority == 1 && token.equals("/")) {
                return ParseEmitOp(src, a, OP_TYPE_DIVIDE, priority);
            }
            if (priority == 1 && token.equals("%")) {	// implied truncate both to integer
                return ParseEmitOp(src, a, OP_TYPE_MOD, priority);
            }
            if (priority == 2 && token.equals("+")) {
                return ParseEmitOp(src, a, OP_TYPE_ADD, priority);
            }
            if (priority == 2 && token.equals("-")) {
                return ParseEmitOp(src, a, OP_TYPE_SUBTRACT, priority);
            }
            if (priority == 3 && token.equals(">=")) {
                return ParseEmitOp(src, a, OP_TYPE_GE, priority);
            }
            if (priority == 3 && token.equals(">")) {
                return ParseEmitOp(src, a, OP_TYPE_GT, priority);
            }
            if (priority == 3 && token.equals("<=")) {
                return ParseEmitOp(src, a, OP_TYPE_LE, priority);
            }
            if (priority == 3 && token.equals("<")) {
                return ParseEmitOp(src, a, OP_TYPE_LT, priority);
            }
            if (priority == 3 && token.equals("==")) {
                return ParseEmitOp(src, a, OP_TYPE_EQ, priority);
            }
            if (priority == 3 && token.equals("!=")) {
                return ParseEmitOp(src, a, OP_TYPE_NE, priority);
            }
            if (priority == 4 && token.equals("&&")) {
                return ParseEmitOp(src, a, OP_TYPE_AND, priority);
            }
            if (priority == 4 && token.equals("||")) {
                return ParseEmitOp(src, a, OP_TYPE_OR, priority);
            }

            // assume that anything else terminates the expression
            // not too robust error checking...
            src.UnreadToken(token);

            return a;
        }

        private int ParseExpression(idLexer src) {
            return ParseExpressionPriority(src, TOP_PRIORITY);
        }

        private void ClearStage(shaderStage_t ss) {
            ss.drawStateBits = 0;
            ss.conditionRegister = GetExpressionConstant(1);
            ss.color.registers[0]
                    = ss.color.registers[1]
                    = ss.color.registers[2]
                    = ss.color.registers[3] = GetExpressionConstant(1);
        }

        private int NameToSrcBlendMode(final idStr name) {
            if (0 == name.Icmp("GL_ONE")) {
                return GLS_SRCBLEND_ONE;
            } else if (0 == name.Icmp("GL_ZERO")) {
                return GLS_SRCBLEND_ZERO;
            } else if (0 == name.Icmp("GL_DST_COLOR")) {
                return GLS_SRCBLEND_DST_COLOR;
            } else if (0 == name.Icmp("GL_ONE_MINUS_DST_COLOR")) {
                return GLS_SRCBLEND_ONE_MINUS_DST_COLOR;
            } else if (0 == name.Icmp("GL_SRC_ALPHA")) {
                return GLS_SRCBLEND_SRC_ALPHA;
            } else if (0 == name.Icmp("GL_ONE_MINUS_SRC_ALPHA")) {
                return GLS_SRCBLEND_ONE_MINUS_SRC_ALPHA;
            } else if (0 == name.Icmp("GL_DST_ALPHA")) {
                return GLS_SRCBLEND_DST_ALPHA;
            } else if (0 == name.Icmp("GL_ONE_MINUS_DST_ALPHA")) {
                return GLS_SRCBLEND_ONE_MINUS_DST_ALPHA;
            } else if (0 == name.Icmp("GL_SRC_ALPHA_SATURATE")) {
                return GLS_SRCBLEND_ALPHA_SATURATE;
            }

            common.Warning("unknown blend mode '%s' in material '%s'", name, GetName());
            SetMaterialFlag(MF_DEFAULTED);

            return GLS_SRCBLEND_ONE;
        }

        private int NameToDstBlendMode(final idStr name) {
            if (0 == name.Icmp("GL_ONE")) {
                return GLS_DSTBLEND_ONE;
            } else if (0 == name.Icmp("GL_ZERO")) {
                return GLS_DSTBLEND_ZERO;
            } else if (0 == name.Icmp("GL_SRC_ALPHA")) {
                return GLS_DSTBLEND_SRC_ALPHA;
            } else if (0 == name.Icmp("GL_ONE_MINUS_SRC_ALPHA")) {
                return GLS_DSTBLEND_ONE_MINUS_SRC_ALPHA;
            } else if (0 == name.Icmp("GL_DST_ALPHA")) {
                return GLS_DSTBLEND_DST_ALPHA;
            } else if (0 == name.Icmp("GL_ONE_MINUS_DST_ALPHA")) {
                return GLS_DSTBLEND_ONE_MINUS_DST_ALPHA;
            } else if (0 == name.Icmp("GL_SRC_COLOR")) {
                return GLS_DSTBLEND_SRC_COLOR;
            } else if (0 == name.Icmp("GL_ONE_MINUS_SRC_COLOR")) {
                return GLS_DSTBLEND_ONE_MINUS_SRC_COLOR;
            }

            common.Warning("unknown blend mode '%s' in material '%s'", name, GetName());
            SetMaterialFlag(MF_DEFAULTED);

            return GLS_DSTBLEND_ONE;
        }

        private void MultiplyTextureMatrix(textureStage_t ts, int[][] registers/*[2][3]*/) {	// FIXME: for some reason the const is bad for gcc and Mac
            int[][] old = new int[2][3];

            if (!ts.hasMatrix) {
                ts.hasMatrix = true;
//		memcpy( ts.matrix, registers, sizeof( ts.matrix ) );
                System.arraycopy(registers[0], 0, ts.matrix[0], 0, ts.matrix[0].length);
                System.arraycopy(registers[1], 0, ts.matrix[1], 0, ts.matrix[1].length);
                return;
            }

//	memcpy( old, ts.matrix, sizeof( old ) );
            System.arraycopy(ts.matrix[0], 0, old[0], 0, old[0].length);
            System.arraycopy(ts.matrix[1], 0, old[1], 0, old[1].length);

            // multiply the two maticies
            ts.matrix[0][0] = EmitOp(
                    EmitOp(old[0][0], registers[0][0], OP_TYPE_MULTIPLY),
                    EmitOp(old[0][1], registers[1][0], OP_TYPE_MULTIPLY), OP_TYPE_ADD);
            ts.matrix[0][1] = EmitOp(
                    EmitOp(old[0][0], registers[0][1], OP_TYPE_MULTIPLY),
                    EmitOp(old[0][1], registers[1][1], OP_TYPE_MULTIPLY), OP_TYPE_ADD);
            ts.matrix[0][2] = EmitOp(
                    EmitOp(
                            EmitOp(old[0][0], registers[0][2], OP_TYPE_MULTIPLY),
                            EmitOp(old[0][1], registers[1][2], OP_TYPE_MULTIPLY), OP_TYPE_ADD),
                    old[0][2], OP_TYPE_ADD);

            ts.matrix[1][0] = EmitOp(
                    EmitOp(old[1][0], registers[0][0], OP_TYPE_MULTIPLY),
                    EmitOp(old[1][1], registers[1][0], OP_TYPE_MULTIPLY), OP_TYPE_ADD);
            ts.matrix[1][1] = EmitOp(
                    EmitOp(old[1][0], registers[0][1], OP_TYPE_MULTIPLY),
                    EmitOp(old[1][1], registers[1][1], OP_TYPE_MULTIPLY), OP_TYPE_ADD);
            ts.matrix[1][2] = EmitOp(
                    EmitOp(
                            EmitOp(old[1][0], registers[0][2], OP_TYPE_MULTIPLY),
                            EmitOp(old[1][1], registers[1][2], OP_TYPE_MULTIPLY), OP_TYPE_ADD),
                    old[1][2], OP_TYPE_ADD);

        }

        /*
         ===============
         idMaterial::SortInteractionStages

         The renderer expects bump, then diffuse, then specular
         There can be multiple bump maps, followed by additional
         diffuse and specular stages, which allows cross-faded bump mapping.

         Ambient stages can be interspersed anywhere, but they are
         ignored during interactions, and all the interaction
         stages are ignored during ambient drawing.
         ===============
         */
        private void SortInteractionStages() {
            int j;

            for (int i = 0; i < numStages; i = j) {
                // find the next bump map
                for (j = i + 1; j < numStages; j++) {
                    if (pd.parseStages[j].lighting == SL_BUMP) {
                        // if the very first stage wasn't a bumpmap,
                        // this bumpmap is part of the first group
                        if (pd.parseStages[i].lighting != SL_BUMP) {
                            continue;
                        }
                        break;
                    }
                }

                // bubble sort everything bump / diffuse / specular
                for (int l = 1; l < j - i; l++) {
                    for (int k = i; k < j - l; k++) {
                        if (pd.parseStages[k].lighting.ordinal() > pd.parseStages[k + 1].lighting.ordinal()) {
                            shaderStage_t temp;

                            temp = pd.parseStages[k];
                            pd.parseStages[k] = pd.parseStages[k + 1];
                            pd.parseStages[k + 1] = temp;
                        }
                    }
                }
            }
        }

        /*
         ==============
         idMaterial::AddImplicitStages

         If a material has diffuse or specular stages without any
         bump stage, add an implicit _flat bumpmap stage.

         If a material has a bump stage but no diffuse or specular
         stage, add a _white diffuse stage.

         It is valid to have either a diffuse or specular without the other.

         It is valid to have a reflection map and a bump map for bumpy reflection
         ==============
         */
        private void AddImplicitStages(final textureRepeat_t trpDefault /*= TR_REPEAT*/) {
            final char[] buffer = new char[1024];
            idLexer newSrc = new idLexer();
            boolean hasDiffuse = false;
            boolean hasSpecular = false;
            boolean hasBump = false;
            boolean hasReflection = false;

            for (int i = 0; i < numStages; i++) {
                if (pd.parseStages[i].lighting == SL_BUMP) {
                    hasBump = true;
                }
                if (pd.parseStages[i].lighting == SL_DIFFUSE) {
                    hasDiffuse = true;
                }
                if (pd.parseStages[i].lighting == SL_SPECULAR) {
                    hasSpecular = true;
                }
                if (pd.parseStages[i].texture.texgen == TG_REFLECT_CUBE) {
                    hasReflection = true;
                }
            }

            // if it doesn't have an interaction at all, don't add anything
            if (!hasBump && !hasDiffuse && !hasSpecular) {
                return;
            }

            if (numStages == MAX_SHADER_STAGES) {
                return;
            }

            if (!hasBump) {
                idStr.snPrintf(buffer, buffer.length, "blend bumpmap\nmap _flat\n}\n");
                newSrc.LoadMemory(ctos(buffer), strLen(buffer), "bumpmap");
                newSrc.SetFlags(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);
                ParseStage(newSrc, trpDefault);
                newSrc.FreeSource();
            }

            if (!hasDiffuse && !hasSpecular && !hasReflection) {
                idStr.snPrintf(buffer, buffer.length, "blend diffusemap\nmap _white\n}\n");
                newSrc.LoadMemory(ctos(buffer), strLen(buffer), "diffusemap");
                newSrc.SetFlags(LEXFL_NOFATALERRORS | LEXFL_NOSTRINGCONCAT | LEXFL_NOSTRINGESCAPECHARS | LEXFL_ALLOWPATHNAMES);
                ParseStage(newSrc, trpDefault);
                newSrc.FreeSource();
            }

        }

        private void AddImplicitStages() {
            AddImplicitStages(TR_REPEAT);
        }

        /*
         ==================
         idMaterial::CheckForConstantRegisters

         As of 5/2/03, about half of the unique materials loaded on typical
         maps are constant, but 2/3 of the surface references are.
         This is probably an optimization of dubious value.
         ==================
         */
        private void CheckForConstantRegisters() {
            if (!pd.registersAreConstant) {
                return;
            }

            // evaluate the registers once, and save them 
            constantRegisters = new float[GetNumRegisters()];// R_ClearedStaticAlloc(GetNumRegisters() /* sizeof( float )*/);

            float[] shaderParms = new float[MAX_ENTITY_SHADER_PARMS];
//	memset( shaderParms, 0, sizeof( shaderParms ) );
            viewDef_s viewDef = new viewDef_s();
//	memset( &viewDef, 0, sizeof( viewDef ) );

            EvaluateRegisters(constantRegisters, shaderParms, viewDef, null);
        }

        /**
         * java seems to have a different order for calling the functions. f1,
         * f2 and f3 are bottom to top. TODO: find out why?
         */
        private int recursiveEmitOp(final float f1, final float f2, final float f3,
                final int b1, final int b2) {
            final int ex1 = GetExpressionConstant(f1);
            final int ex2 = GetExpressionConstant(f2);
            final int em1 = EmitOp(ex2, b1, OP_TYPE_MULTIPLY);
            final int ex3 = GetExpressionConstant(f3);
            final int em2 = EmitOp(ex3, b2, OP_TYPE_MULTIPLY);
            final int em3 = EmitOp(em2, em1, OP_TYPE_ADD);
            return EmitOp(em3, ex1, OP_TYPE_ADD);
        }

        @Override
        public ByteBuffer AllocBuffer() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void Read(ByteBuffer buffer) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ByteBuffer Write() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String toString() {
            return this + " idMaterial{" + "desc=" + desc + ", renderBump=" + renderBump + ", lightFalloffImage=" + lightFalloffImage + ", entityGui=" + entityGui + ", gui=" + gui + ", noFog=" + noFog + ", spectrum=" + spectrum + ", polygonOffset=" + polygonOffset + ", contentFlags=" + contentFlags + ", surfaceFlags=" + surfaceFlags + ", materialFlags=" + materialFlags + ", decalInfo=" + decalInfo + ", sort=" + sort + ", deform=" + deform + ", deformRegisters=" + deformRegisters + ", deformDecl=" + deformDecl + ", texGenRegisters=" + texGenRegisters + ", coverage=" + coverage + ", cullType=" + cullType + ", shouldCreateBackSides=" + shouldCreateBackSides + ", fogLight=" + fogLight + ", blendLight=" + blendLight + ", ambientLight=" + ambientLight + ", unsmoothedTangents=" + unsmoothedTangents + ", hasSubview=" + hasSubview + ", allowOverlays=" + allowOverlays + ", numOps=" + numOps + ", ops=" + ops + ", numRegisters=" + numRegisters + ", expressionRegisters=" + expressionRegisters + ", constantRegisters=" + constantRegisters + ", numStages=" + numStages + ", numAmbientStages=" + numAmbientStages + ", stages=" + stages + ", pd=" + pd + ", surfaceArea=" + surfaceArea + ", editorImageName=" + editorImageName + ", editorImage=" + editorImage + ", editorAlpha=" + editorAlpha + ", suppressInSubview=" + suppressInSubview + ", portalSky=" + portalSky + ", refCount=" + refCount + '}';
        }
    };

    public static class idMatList extends idList<idMaterial> {
    };
}
