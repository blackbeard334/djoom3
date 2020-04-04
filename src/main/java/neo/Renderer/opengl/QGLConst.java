package neo.Renderer.opengl;

public interface QGLConst {

	/*
	 * alpha functions
	 */
	int GL_NEVER = 0x0200;

	int GL_LESS = 0x0201;

	int GL_EQUAL = 0x0202;

	int GL_LEQUAL = 0x0203;

	int GL_GREATER = 0x0204;

	int GL_NOTEQUAL = 0x0205;

	int GL_GEQUAL = 0x0206;

	int GL_ALWAYS = 0x0207;

	/*
	 * attribute masks
	 */
	int GL_DEPTH_BUFFER_BIT = 0x00000100;

	int GL_STENCIL_BUFFER_BIT = 0x00000400;

	int GL_COLOR_BUFFER_BIT = 0x00004000;

	/*
	 * begin modes
	 */
	int GL_POINTS = 0x0000;

	int GL_LINES = 0x0001;

	int GL_LINE_LOOP = 0x0002;

	int GL_LINE_STRIP = 0x0003;

	int GL_TRIANGLES = 0x0004;

	int GL_TRIANGLE_STRIP = 0x0005;

	int GL_TRIANGLE_FAN = 0x0006;

	int GL_QUADS = 0x0007;

	int GL_QUAD_STRIP = 0x0008;

	int GL_POLYGON = 0x0009;

	/*
	 * blending factors
	 */
	int GL_ZERO = 0;

	int GL_ONE = 1;

	int GL_SRC_COLOR = 0x0300;

	int GL_ONE_MINUS_SRC_COLOR = 0x0301;

	int GL_SRC_ALPHA = 0x0302;

	int GL_ONE_MINUS_SRC_ALPHA = 0x0303;

	int GL_DST_ALPHA = 0x0304;

	int GL_ONE_MINUS_DST_ALPHA = 0x0305;

	/*
	 * boolean
	 */
	int GL_TRUE = 1;

	int GL_FALSE = 0;

	/*
	 * data types
	 */
	int GL_BYTE = 0x1400;

	int GL_UNSIGNED_BYTE = 0x1401;

	int GL_SHORT = 0x1402;

	int GL_UNSIGNED_SHORT = 0x1403;

	int GL_INT = 0x1404;

	int GL_UNSIGNED_INT = 0x1405;

	int GL_FLOAT = 0x1406;

	/*
	 * draw buffer modes
	 */
	int GL_FRONT = 0x0404;

	int GL_BACK = 0x0405;

	int GL_FRONT_AND_BACK = 0x0408;

	/*
	 * errors
	 */
	int GL_NO_ERROR = 0;

	int GL_POINT_SMOOTH = 0x0B10;

	int GL_CULL_FACE = 0x0B44;

	int GL_DEPTH_TEST = 0x0B71;

	int GL_MODELVIEW_MATRIX = 0x0BA6;

	int GL_ALPHA_TEST = 0x0BC0;

	int GL_BLEND = 0x0BE2;

	int GL_SCISSOR_TEST = 0x0C11;

	int GL_PACK_ALIGNMENT = 0x0D05;

	int GL_TEXTURE_2D = 0x0DE1;

	/*
	 * hints
	 */
	int GL_PERSPECTIVE_CORRECTION_HINT = 0x0C50;

	int GL_DONT_CARE = 0x1100;

	int GL_FASTEST = 0x1101;

	int GL_NICEST = 0x1102;

	/*
	 * matrix modes
	 */
	int GL_MODELVIEW = 0x1700;

	int GL_PROJECTION = 0x1701;

	/*
	 * pixel formats
	 */
	int GL_COLOR_INDEX = 0x1900;

	int GL_RED = 0x1903;

	int GL_GREEN = 0x1904;

	int GL_BLUE = 0x1905;

	int GL_ALPHA = 0x1906;

	int GL_RGB = 0x1907;

	int GL_RGBA = 0x1908;

	int GL_LUMINANCE = 0x1909;

	int GL_LUMINANCE_ALPHA = 0x190A;

	/*
	 * polygon modes
	 */

	int GL_POINT = 0x1B00;

	int GL_LINE = 0x1B01;

	int GL_FILL = 0x1B02;

	/*
	 * shading models
	 */
	int GL_FLAT = 0x1D00;

	int GL_SMOOTH = 0x1D01;

	int GL_REPLACE = 0x1E01;

	/*
	 * string names
	 */
	int GL_VENDOR = 0x1F00;

	int GL_RENDERER = 0x1F01;

	int GL_VERSION = 0x1F02;

	int GL_EXTENSIONS = 0x1F03;

	/*
	 * TextureEnvMode
	 */
	int GL_MODULATE = 0x2100;

	/*
	 * TextureEnvParameter
	 */
	int GL_TEXTURE_ENV_MODE = 0x2200;

	int GL_TEXTURE_ENV_COLOR = 0x2201;

	/*
	 * TextureEnvTarget
	 */
	int GL_TEXTURE_ENV = 0x2300;

	int GL_NEAREST = 0x2600;

	int GL_LINEAR = 0x2601;

	int GL_NEAREST_MIPMAP_NEAREST = 0x2700;

	int GL_LINEAR_MIPMAP_NEAREST = 0x2701;

	int GL_NEAREST_MIPMAP_LINEAR = 0x2702;

	int GL_LINEAR_MIPMAP_LINEAR = 0x2703;

	/*
	 * TextureParameterName
	 */
	int GL_TEXTURE_MAG_FILTER = 0x2800;

	int GL_TEXTURE_MIN_FILTER = 0x2801;

	int GL_TEXTURE_WRAP_S = 0x2802;

	int GL_TEXTURE_WRAP_T = 0x2803;

	/*
	 * TextureWrapMode
	 */
	int GL_CLAMP = 0x2900;

	int GL_REPEAT = 0x2901;

	/*
	 * texture
	 */
	int GL_LUMINANCE8 = 0x8040;

	int GL_INTENSITY8 = 0x804B;

	int GL_R3_G3_B2 = 0x2A10;

	int GL_RGB4 = 0x804F;

	int GL_RGB5 = 0x8050;

	int GL_RGB8 = 0x8051;

	int GL_RGBA2 = 0x8055;

	int GL_RGBA4 = 0x8056;

	int GL_RGB5_A1 = 0x8057;

	int GL_RGBA8 = 0x8058;

	/*
	 * vertex arrays
	 */
	int GL_VERTEX_ARRAY = 0x8074;

	int GL_COLOR_ARRAY = 0x8076;

	int GL_TEXTURE_COORD_ARRAY = 0x8078;

	int GL_T2F_V3F = 0x2A27;

	/*
	 * OpenGL 1.2, 1.3 constants
	 */
	int GL_SHARED_TEXTURE_PALETTE_EXT = 0x81FB;

	int GL_TEXTURE0 = 0x84C0;

	int GL_TEXTURE1 = 0x84C1;

	//int GL_TEXTURE0_ARB = 0x84C0;

	int GL_TEXTURE1_ARB = 0x84C1;

	int GL_BGR = 0x80E0;

	int GL_BGRA = 0x80E1;

	/*
	 * point parameters
	 */
	int GL_POINT_SIZE_MIN_EXT = 0x8126;

	int GL_POINT_SIZE_MAX_EXT = 0x8127;

	int GL_POINT_FADE_THRESHOLD_SIZE_EXT = 0x8128;

	int GL_DISTANCE_ATTENUATION_EXT = 0x8129;

    /** GetTarget */
    int
        GL_CURRENT_COLOR                 = 0xB00,
        GL_CURRENT_INDEX                 = 0xB01,
        GL_CURRENT_NORMAL                = 0xB02,
        GL_CURRENT_TEXTURE_COORDS        = 0xB03,
        GL_CURRENT_RASTER_COLOR          = 0xB04,
        GL_CURRENT_RASTER_INDEX          = 0xB05,
        GL_CURRENT_RASTER_TEXTURE_COORDS = 0xB06,
        GL_CURRENT_RASTER_POSITION       = 0xB07,
        GL_CURRENT_RASTER_POSITION_VALID = 0xB08,
        GL_CURRENT_RASTER_DISTANCE       = 0xB09,
        //GL_POINT_SMOOTH                  = 0xB10,
        GL_POINT_SIZE                    = 0xB11,
        GL_POINT_SIZE_RANGE              = 0xB12,
        GL_POINT_SIZE_GRANULARITY        = 0xB13,
        GL_LINE_SMOOTH                   = 0xB20,
        GL_LINE_WIDTH                    = 0xB21,
        GL_LINE_WIDTH_RANGE              = 0xB22,
        GL_LINE_WIDTH_GRANULARITY        = 0xB23,
        GL_LINE_STIPPLE                  = 0xB24,
        GL_LINE_STIPPLE_PATTERN          = 0xB25,
        GL_LINE_STIPPLE_REPEAT           = 0xB26,
        GL_LIST_MODE                     = 0xB30,
        GL_MAX_LIST_NESTING              = 0xB31,
        GL_LIST_BASE                     = 0xB32,
        GL_LIST_INDEX                    = 0xB33,
        GL_POLYGON_MODE                  = 0xB40,
        GL_POLYGON_SMOOTH                = 0xB41,
        GL_POLYGON_STIPPLE               = 0xB42,
        GL_EDGE_FLAG                     = 0xB43,
        //GL_CULL_FACE                     = 0xB44,
        GL_CULL_FACE_MODE                = 0xB45,
        GL_FRONT_FACE                    = 0xB46,
        GL_LIGHTING                      = 0xB50,
        GL_LIGHT_MODEL_LOCAL_VIEWER      = 0xB51,
        GL_LIGHT_MODEL_TWO_SIDE          = 0xB52,
        GL_LIGHT_MODEL_AMBIENT           = 0xB53,
        GL_SHADE_MODEL                   = 0xB54,
        GL_COLOR_MATERIAL_FACE           = 0xB55,
        GL_COLOR_MATERIAL_PARAMETER      = 0xB56,
        GL_COLOR_MATERIAL                = 0xB57,
        GL_FOG                           = 0xB60,
        GL_FOG_INDEX                     = 0xB61,
        GL_FOG_DENSITY                   = 0xB62,
        GL_FOG_START                     = 0xB63,
        GL_FOG_END                       = 0xB64,
        GL_FOG_MODE                      = 0xB65,
        GL_FOG_COLOR                     = 0xB66,
        GL_DEPTH_RANGE                   = 0xB70,
        //GL_DEPTH_TEST                    = 0xB71,
        GL_DEPTH_WRITEMASK               = 0xB72,
        GL_DEPTH_CLEAR_VALUE             = 0xB73,
        GL_DEPTH_FUNC                    = 0xB74,
        GL_ACCUM_CLEAR_VALUE             = 0xB80,
        GL_STENCIL_TEST                  = 0xB90,
        GL_STENCIL_CLEAR_VALUE           = 0xB91,
        GL_STENCIL_FUNC                  = 0xB92,
        GL_STENCIL_VALUE_MASK            = 0xB93,
        GL_STENCIL_FAIL                  = 0xB94,
        GL_STENCIL_PASS_DEPTH_FAIL       = 0xB95,
        GL_STENCIL_PASS_DEPTH_PASS       = 0xB96,
        GL_STENCIL_REF                   = 0xB97,
        GL_STENCIL_WRITEMASK             = 0xB98,
        GL_MATRIX_MODE                   = 0xBA0,
        GL_NORMALIZE                     = 0xBA1,
        GL_VIEWPORT                      = 0xBA2,
        GL_MODELVIEW_STACK_DEPTH         = 0xBA3,
        GL_PROJECTION_STACK_DEPTH        = 0xBA4,
        GL_TEXTURE_STACK_DEPTH           = 0xBA5,
        //GL_MODELVIEW_MATRIX              = 0xBA6,
        GL_PROJECTION_MATRIX             = 0xBA7,
        GL_TEXTURE_MATRIX                = 0xBA8,
        GL_ATTRIB_STACK_DEPTH            = 0xBB0,
        GL_CLIENT_ATTRIB_STACK_DEPTH     = 0xBB1,
        //GL_ALPHA_TEST                    = 0xBC0,
        GL_ALPHA_TEST_FUNC               = 0xBC1,
        GL_ALPHA_TEST_REF                = 0xBC2,
        GL_DITHER                        = 0xBD0,
        GL_BLEND_DST                     = 0xBE0,
        GL_BLEND_SRC                     = 0xBE1,
        //GL_BLEND                         = 0xBE2,
        GL_LOGIC_OP_MODE                 = 0xBF0,
        GL_INDEX_LOGIC_OP                = 0xBF1,
        GL_LOGIC_OP                      = 0xBF1,
        GL_COLOR_LOGIC_OP                = 0xBF2,
        GL_AUX_BUFFERS                   = 0xC00,
        GL_DRAW_BUFFER                   = 0xC01,
        GL_READ_BUFFER                   = 0xC02,
        GL_SCISSOR_BOX                   = 0xC10,
        //GL_SCISSOR_TEST                  = 0xC11,
        GL_INDEX_CLEAR_VALUE             = 0xC20,
        GL_INDEX_WRITEMASK               = 0xC21,
        GL_COLOR_CLEAR_VALUE             = 0xC22,
        GL_COLOR_WRITEMASK               = 0xC23,
        GL_INDEX_MODE                    = 0xC30,
        GL_RGBA_MODE                     = 0xC31,
        GL_DOUBLEBUFFER                  = 0xC32,
        GL_STEREO                        = 0xC33,
        GL_RENDER_MODE                   = 0xC40,
        //GL_PERSPECTIVE_CORRECTION_HINT   = 0xC50,
        GL_POINT_SMOOTH_HINT             = 0xC51,
        GL_LINE_SMOOTH_HINT              = 0xC52,
        GL_POLYGON_SMOOTH_HINT           = 0xC53,
        GL_FOG_HINT                      = 0xC54,
        GL_TEXTURE_GEN_S                 = 0xC60,
        GL_TEXTURE_GEN_T                 = 0xC61,
        GL_TEXTURE_GEN_R                 = 0xC62,
        GL_TEXTURE_GEN_Q                 = 0xC63,
        GL_PIXEL_MAP_I_TO_I              = 0xC70,
        GL_PIXEL_MAP_S_TO_S              = 0xC71,
        GL_PIXEL_MAP_I_TO_R              = 0xC72,
        GL_PIXEL_MAP_I_TO_G              = 0xC73,
        GL_PIXEL_MAP_I_TO_B              = 0xC74,
        GL_PIXEL_MAP_I_TO_A              = 0xC75,
        GL_PIXEL_MAP_R_TO_R              = 0xC76,
        GL_PIXEL_MAP_G_TO_G              = 0xC77,
        GL_PIXEL_MAP_B_TO_B              = 0xC78,
        GL_PIXEL_MAP_A_TO_A              = 0xC79,
        GL_PIXEL_MAP_I_TO_I_SIZE         = 0xCB0,
        GL_PIXEL_MAP_S_TO_S_SIZE         = 0xCB1,
        GL_PIXEL_MAP_I_TO_R_SIZE         = 0xCB2,
        GL_PIXEL_MAP_I_TO_G_SIZE         = 0xCB3,
        GL_PIXEL_MAP_I_TO_B_SIZE         = 0xCB4,
        GL_PIXEL_MAP_I_TO_A_SIZE         = 0xCB5,
        GL_PIXEL_MAP_R_TO_R_SIZE         = 0xCB6,
        GL_PIXEL_MAP_G_TO_G_SIZE         = 0xCB7,
        GL_PIXEL_MAP_B_TO_B_SIZE         = 0xCB8,
        GL_PIXEL_MAP_A_TO_A_SIZE         = 0xCB9,
        GL_UNPACK_SWAP_BYTES             = 0xCF0,
        GL_UNPACK_LSB_FIRST              = 0xCF1,
        GL_UNPACK_ROW_LENGTH             = 0xCF2,
        GL_UNPACK_SKIP_ROWS              = 0xCF3,
        GL_UNPACK_SKIP_PIXELS            = 0xCF4,
        GL_UNPACK_ALIGNMENT              = 0xCF5,
        GL_PACK_SWAP_BYTES               = 0xD00,
        GL_PACK_LSB_FIRST                = 0xD01,
        GL_PACK_ROW_LENGTH               = 0xD02,
        GL_PACK_SKIP_ROWS                = 0xD03,
        GL_PACK_SKIP_PIXELS              = 0xD04,
        //GL_PACK_ALIGNMENT                = 0xD05,
        GL_MAP_COLOR                     = 0xD10,
        GL_MAP_STENCIL                   = 0xD11,
        GL_INDEX_SHIFT                   = 0xD12,
        GL_INDEX_OFFSET                  = 0xD13,
        GL_RED_SCALE                     = 0xD14,
        GL_RED_BIAS                      = 0xD15,
        GL_ZOOM_X                        = 0xD16,
        GL_ZOOM_Y                        = 0xD17,
        GL_GREEN_SCALE                   = 0xD18,
        GL_GREEN_BIAS                    = 0xD19,
        GL_BLUE_SCALE                    = 0xD1A,
        GL_BLUE_BIAS                     = 0xD1B,
        GL_ALPHA_SCALE                   = 0xD1C,
        GL_ALPHA_BIAS                    = 0xD1D,
        GL_DEPTH_SCALE                   = 0xD1E,
        GL_DEPTH_BIAS                    = 0xD1F,
        GL_MAX_EVAL_ORDER                = 0xD30,
        GL_MAX_LIGHTS                    = 0xD31,
        GL_MAX_CLIP_PLANES               = 0xD32,
        GL_MAX_TEXTURE_SIZE              = 0xD33,
        GL_MAX_PIXEL_MAP_TABLE           = 0xD34,
        GL_MAX_ATTRIB_STACK_DEPTH        = 0xD35,
        GL_MAX_MODELVIEW_STACK_DEPTH     = 0xD36,
        GL_MAX_NAME_STACK_DEPTH          = 0xD37,
        GL_MAX_PROJECTION_STACK_DEPTH    = 0xD38,
        GL_MAX_TEXTURE_STACK_DEPTH       = 0xD39,
        GL_MAX_VIEWPORT_DIMS             = 0xD3A,
        GL_MAX_CLIENT_ATTRIB_STACK_DEPTH = 0xD3B,
        GL_SUBPIXEL_BITS                 = 0xD50,
        GL_INDEX_BITS                    = 0xD51,
        GL_RED_BITS                      = 0xD52,
        GL_GREEN_BITS                    = 0xD53,
        GL_BLUE_BITS                     = 0xD54,
        GL_ALPHA_BITS                    = 0xD55,
        GL_DEPTH_BITS                    = 0xD56,
        GL_STENCIL_BITS                  = 0xD57,
        GL_ACCUM_RED_BITS                = 0xD58,
        GL_ACCUM_GREEN_BITS              = 0xD59,
        GL_ACCUM_BLUE_BITS               = 0xD5A,
        GL_ACCUM_ALPHA_BITS              = 0xD5B,
        GL_NAME_STACK_DEPTH              = 0xD70,
        GL_AUTO_NORMAL                   = 0xD80,
        GL_MAP1_COLOR_4                  = 0xD90,
        GL_MAP1_INDEX                    = 0xD91,
        GL_MAP1_NORMAL                   = 0xD92,
        GL_MAP1_TEXTURE_COORD_1          = 0xD93,
        GL_MAP1_TEXTURE_COORD_2          = 0xD94,
        GL_MAP1_TEXTURE_COORD_3          = 0xD95,
        GL_MAP1_TEXTURE_COORD_4          = 0xD96,
        GL_MAP1_VERTEX_3                 = 0xD97,
        GL_MAP1_VERTEX_4                 = 0xD98,
        GL_MAP2_COLOR_4                  = 0xDB0,
        GL_MAP2_INDEX                    = 0xDB1,
        GL_MAP2_NORMAL                   = 0xDB2,
        GL_MAP2_TEXTURE_COORD_1          = 0xDB3,
        GL_MAP2_TEXTURE_COORD_2          = 0xDB4,
        GL_MAP2_TEXTURE_COORD_3          = 0xDB5,
        GL_MAP2_TEXTURE_COORD_4          = 0xDB6,
        GL_MAP2_VERTEX_3                 = 0xDB7,
        GL_MAP2_VERTEX_4                 = 0xDB8,
        GL_MAP1_GRID_DOMAIN              = 0xDD0,
        GL_MAP1_GRID_SEGMENTS            = 0xDD1,
        GL_MAP2_GRID_DOMAIN              = 0xDD2,
        GL_MAP2_GRID_SEGMENTS            = 0xDD3,
        GL_TEXTURE_1D                    = 0xDE0,
        //GL_TEXTURE_2D                    = 0xDE1,
        GL_FEEDBACK_BUFFER_POINTER       = 0xDF0,
        GL_FEEDBACK_BUFFER_SIZE          = 0xDF1,
        GL_FEEDBACK_BUFFER_TYPE          = 0xDF2,
        GL_SELECTION_BUFFER_POINTER      = 0xDF3,
        GL_SELECTION_BUFFER_SIZE         = 0xDF4;
    /** GetTextureParameter */
    public static final int
        GL_TEXTURE_BORDER_COLOR    = 0x1004;
    /** AttribMask */
    public static final int
        GL_ALL_ATTRIB_BITS     = 0xFFFFF;
    /** PixelFormat */
    public static final int
        GL_STENCIL_INDEX   = 0x1901,
        GL_DEPTH_COMPONENT = 0x1902;
    /** StencilOp */
    public static final int
        GL_KEEP    = 0x1E00,
        GL_INCR    = 0x1E02,
        GL_DECR    = 0x1E03;
    /** polygon_offset */
    public static final int
        GL_POLYGON_OFFSET_LINE   = 0x2A02,
        GL_POLYGON_OFFSET_FILL   = 0x8037;
    /** vertex_array */
    public static final int
        GL_NORMAL_ARRAY                = 0x8075;
    /** TextureGenMode */
    public static final int
        GL_OBJECT_LINEAR = 0x2401;
    /** TextureCoordName */
    public static final int
        GL_S = 0x2000,
        GL_T = 0x2001,
        GL_R = 0x2002,
        GL_Q = 0x2003;
    /** MatrixMode */
    public static final int
        GL_TEXTURE    = 0x1702;
    /** TextureGenParameter */
    public static final int
        GL_TEXTURE_GEN_MODE = 0x2500,
        GL_OBJECT_PLANE     = 0x2501,
        GL_EYE_PLANE        = 0x2502;
    /** AccumOp */
    public static final int
        GL_ADD    = 0x104;
    /** TextureEnvMode */
    public static final int
        GL_DECAL    = 0x2101;
    /** BlendingFactorSrc */
    public static final int
        GL_DST_COLOR           = 0x306,
        GL_ONE_MINUS_DST_COLOR = 0x307,
        GL_SRC_ALPHA_SATURATE  = 0x308;
    /** ErrorCode */
    public static final int
        GL_INVALID_ENUM      = 0x500,
        GL_INVALID_VALUE     = 0x501,
        GL_INVALID_OPERATION = 0x502,
        GL_STACK_OVERFLOW    = 0x503,
        GL_STACK_UNDERFLOW   = 0x504,
        GL_OUT_OF_MEMORY     = 0x505;
    /** texture */
    public static final int
        GL_ALPHA8                 = 0x803C,
        GL_LUMINANCE8_ALPHA8      = 0x8045;

    /*
     * GL12
     */
    /**
     * Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
     * GetDoublev, and by the {@code target} parameter of TexImage3D, GetTexImage, GetTexLevelParameteriv, GetTexLevelParameterfv, GetTexParameteriv, and
     * GetTexParameterfv.
     */
    public static final int GL_TEXTURE_3D = 0x806F;
    /** Accepted by the {@code pname} parameter of TexParameteriv, TexParameterfv, GetTexParameteriv, and GetTexParameterfv. */
    public static final int GL_TEXTURE_WRAP_R = 0x8072;
    /**
     * Accepted by the {@code param} parameter of TexParameteri and TexParameterf, and by the {@code params} parameter of TexParameteriv and TexParameterfv,
     * when their {@code pname} parameter is TEXTURE_WRAP_S, TEXTURE_WRAP_T, or TEXTURE_WRAP_R.
     */
    public static final int GL_CLAMP_TO_EDGE = 0x812F;

    /*
     * GL13
     */
    /**
     * Accepted by the {@code param} parameter of TexParameteri and TexParameterf, and by the {@code params} parameter of TexParameteriv and TexParameterfv,
     * when their {@code pname} parameter is TEXTURE_WRAP_S, TEXTURE_WRAP_T, or TEXTURE_WRAP_R.
     */
    public static final int GL_CLAMP_TO_BORDER = 0x812D;
    /**
     * Accepted by the {@code target} parameter of GetTexImage, GetTexLevelParameteriv, GetTexLevelParameterfv, TexImage2D, CopyTexImage2D, TexSubImage2D, and
     * CopySubTexImage2D.
     */
    public static final int
        GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515;
    /** Accepted by the {@code param} parameters of TexGend, TexGenf, and TexGeni when {@code pname} parameter is TEXTURE_GEN_MODE. */
    public static final int
        GL_NORMAL_MAP     = 0x8511,
        GL_REFLECTION_MAP = 0x8512;
    /** Accepted by the {@code params} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code pname} parameter value is TEXTURE_ENV_MODE. */
    public static final int GL_COMBINE = 0x8570;
    /**
     * When the {@code pname} parameter of TexGendv, TexGenfv, and TexGeniv is TEXTURE_GEN_MODE, then the array {@code params} may also contain NORMAL_MAP
     * or REFLECTION_MAP. Accepted by the {@code cap} parameter of Enable, Disable, IsEnabled, and by the {@code pname} parameter of GetBooleanv,
     * GetIntegerv, GetFloatv, and GetDoublev, and by the {@code target} parameter of BindTexture, GetTexParameterfv, GetTexParameteriv, TexParameterf,
     * TexParameteri, TexParameterfv, and TexParameteriv.
     */
    public static final int GL_TEXTURE_CUBE_MAP = 0x8513;

    /*
     * GL14
     */
    /**
     * When the {@code target} parameter of GetTexEnvfv, GetTexEnviv, TexEnvi, TexEnvf, TexEnviv, and TexEnvfv is TEXTURE_FILTER_CONTROL, then the value of
     * {@code pname} may be.
     */
    public static final int GL_TEXTURE_LOD_BIAS = 0x8501;

    /*
     * GL31
     */
    /**
     * Accepted by the {@code cap} parameter of Enable, Disable and IsEnabled; by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv and
     * GetDoublev; and by the {@code target} parameter of BindTexture, GetTexParameterfv, GetTexParameteriv, TexParameterf, TexParameteri, TexParameterfv and
     * TexParameteriv.
     */
    public static final int GL_TEXTURE_RECTANGLE = 0x84F5;

    /*
     * ARBVertexBufferObject
     */
    /**
     * Accepted by the {@code target} parameters of BindBufferARB, BufferDataARB, BufferSubDataARB, MapBufferARB, UnmapBufferARB, GetBufferSubDataARB,
     * GetBufferParameterivARB, and GetBufferPointervARB.
     */
    public static final int
        GL_ARRAY_BUFFER_ARB         = 0x8892,
        GL_ELEMENT_ARRAY_BUFFER_ARB = 0x8893;
    /** Accepted by the {@code usage} parameter of BufferDataARB. */
    public static final int
        GL_STREAM_DRAW_ARB  = 0x88E0,
        GL_STATIC_DRAW_ARB  = 0x88E4;

    /*
     * ARBMultitexture
     */
    /** Accepted by the {@code texture} parameter of ActiveTexture and MultiTexCoord. */
    public static final int
        GL_TEXTURE0_ARB  = 0x84C0;
    /** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
    public static final int
        GL_MAX_TEXTURE_UNITS_ARB     = 0x84E2;

    /*
     * ARBFragmentProgram
     */
    /**
     * Accepted by the {@code cap} parameter of Disable, Enable, and IsEnabled, by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
     * GetDoublev, and by the {@code target} parameter of ProgramStringARB, BindProgramARB, ProgramEnvParameter4[df][v]ARB, ProgramLocalParameter4[df][v]ARB,
     * GetProgramEnvParameter[df]vARB, GetProgramLocalParameter[df]vARB, GetProgramivARB and GetProgramStringARB.
     */
    public static final int GL_FRAGMENT_PROGRAM_ARB = 0x8804;
    /** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
    public static final int
        GL_MAX_TEXTURE_COORDS_ARB      = 0x8871,
        GL_MAX_TEXTURE_IMAGE_UNITS_ARB = 0x8872;

    /*
     * EXTStencilWrap
     */
    /** Accepted by the {@code sfail}, {@code dpfail}, and {@code dppass} parameter of StencilOp. */
    public static final int
        GL_INCR_WRAP_EXT = 0x8507,
        GL_DECR_WRAP_EXT = 0x8508;

    /*
     * EXTTextureFilterAnisotropic
     */
    /** Accepted by the {@code pname} parameters of GetTexParameterfv, GetTexParameteriv, TexParameterf, TexParameterfv, TexParameteri, and TexParameteriv. */
    public static final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE;
    /** Accepted by the {@code pname} parameters of GetBooleanv, GetDoublev, GetFloatv, and GetIntegerv. */
    public static final int GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF;

    /*
     * ARBVertexProgram
     */
    /**
     * Accepted by the {@code cap} parameter of Disable, Enable, and IsEnabled, by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
     * GetDoublev, and by the {@code target} parameter of ProgramStringARB, BindProgramARB, ProgramEnvParameter4[df][v]ARB, ProgramLocalParameter4[df][v]ARB,
     * GetProgramEnvParameter[df]vARB, GetProgramLocalParameter[df]vARB, GetProgramivARB, and GetProgramStringARB.
     */
    public static final int GL_VERTEX_PROGRAM_ARB = 0x8620;
    /** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
    public static final int
        GL_PROGRAM_ERROR_POSITION_ARB         = 0x864B;
    /** Accepted by the {@code name} parameter of GetString. */
    public static final int GL_PROGRAM_ERROR_STRING_ARB = 0x8874;
    /** Accepted by the {@code format} parameter of ProgramStringARB. */
    public static final int GL_PROGRAM_FORMAT_ASCII_ARB = 0x8875;

    /*
     * ARBTextureCompression
     */
    /** Accepted by the {@code internalformat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, and CopyTexImage2D. */
    public static final int
        GL_COMPRESSED_RGB_ARB             = 0x84ED,
        GL_COMPRESSED_RGBA_ARB            = 0x84EE;

    /*
     * EXTBGRA
     */
    /** Accepted by the {@code format} parameter of DrawPixels, GetTexImage, ReadPixels, TexImage1D, and TexImage2D. */
    public static final int
        GL_BGR_EXT  = 0x80E0,
        GL_BGRA_EXT = 0x80E1;

    /*
     * EXTTextureCompressionS3TC
     */
    /**
     * Accepted by the {@code internalformat} parameter of TexImage2D, CopyTexImage2D, and CompressedTexImage2D and the {@code format} parameter of
     * CompressedTexSubImage2D.
     */
    public static final int
        GL_COMPRESSED_RGB_S3TC_DXT1_EXT  = 0x83F0,
        GL_COMPRESSED_RGBA_S3TC_DXT1_EXT = 0x83F1,
        GL_COMPRESSED_RGBA_S3TC_DXT3_EXT = 0x83F2,
        GL_COMPRESSED_RGBA_S3TC_DXT5_EXT = 0x83F3;

    /*
     * EXTDepthBoundsTest
     */
    /**
     * Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
     * GetDoublev.
     */
    public static final int GL_DEPTH_BOUNDS_TEST_EXT = 0x8890;

    /*
     * ARBTextureEnvCombine
     */
    /** Accepted by the {@code params} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code pname} parameter value is TEXTURE_ENV_MODE. */
    public static final int GL_COMBINE_ARB = 0x8570;
    /** Accepted by the {@code pname} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code target} parameter value is TEXTURE_ENV. */
    public static final int
        GL_COMBINE_RGB_ARB    = 0x8571,
        GL_COMBINE_ALPHA_ARB  = 0x8572,
        GL_SOURCE0_RGB_ARB    = 0x8580,
        GL_SOURCE1_RGB_ARB    = 0x8581,
        GL_SOURCE2_RGB_ARB    = 0x8582,
        GL_SOURCE0_ALPHA_ARB  = 0x8588,
        GL_SOURCE1_ALPHA_ARB  = 0x8589,
        GL_SOURCE2_ALPHA_ARB  = 0x858A,
        GL_OPERAND0_RGB_ARB   = 0x8590,
        GL_OPERAND1_RGB_ARB   = 0x8591,
        GL_OPERAND2_RGB_ARB   = 0x8592,
        GL_OPERAND0_ALPHA_ARB = 0x8598,
        GL_OPERAND1_ALPHA_ARB = 0x8599,
        GL_OPERAND2_ALPHA_ARB = 0x859A,
        GL_RGB_SCALE_ARB      = 0x8573;
    /**
     * Accepted by the {@code params} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code pname} parameter value is SOURCE0_RGB_ARB,
     * SOURCE1_RGB_ARB, SOURCE2_RGB_ARB, SOURCE0_ALPHA_ARB, SOURCE1_ALPHA_ARB, or SOURCE2_ALPHA_ARB.
     */
    public static final int
        GL_CONSTANT_ARB      = 0x8576,
        GL_PRIMARY_COLOR_ARB = 0x8577,
        GL_PREVIOUS_ARB      = 0x8578;

    /*
     * ARBTextureEnvDot3
     */
    /** Accepted by the {@code params} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code pname} parameter value is COMBINE_RGB_ARB. */
    public static final int
        GL_DOT3_RGB_ARB  = 0x86AE,
        GL_DOT3_RGBA_ARB = 0x86AF;
}