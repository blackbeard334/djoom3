package neo.Renderer;

/**
 *
 */
public class cg_explicit {

    // typedef enum
    // {
    static final int CG_UNKNOWN_TYPE          = 0;
    static final int CG_STRUCT                = 1;
    static final int CG_ARRAY                 = 2;
    //     
    static final int CG_TYPE_START_ENUM       = 1024;
    //     
    //     
    static final int CG_HALF                  = 1025;
    static final int CG_HALF2                 = 1026;
    static final int CG_HALF3                 = 1027;
    static final int CG_HALF4                 = 1028;
    static final int CG_HALF1x1               = 1029;
    static final int CG_HALF1x2               = 1030;
    static final int CG_HALF1x3               = 1031;
    static final int CG_HALF1x4               = 1032;
    static final int CG_HALF2x1               = 1033;
    static final int CG_HALF2x2               = 1034;
    static final int CG_HALF2x3               = 1035;
    static final int CG_HALF2x4               = 1036;
    static final int CG_HALF3x1               = 1037;
    static final int CG_HALF3x2               = 1038;
    static final int CG_HALF3x3               = 1039;
    static final int CG_HALF3x4               = 1040;
    static final int CG_HALF4x1               = 1041;
    static final int CG_HALF4x2               = 1042;
    static final int CG_HALF4x3               = 1043;
    static final int CG_HALF4x4               = 1044;
    static final int CG_FLOAT                 = 1045;
    static final int CG_FLOAT2                = 1046;
    static final int CG_FLOAT3                = 1047;
    static final int CG_FLOAT4                = 1048;
    static final int CG_FLOAT1x1              = 1049;
    static final int CG_FLOAT1x2              = 1050;
    static final int CG_FLOAT1x3              = 1051;
    static final int CG_FLOAT1x4              = 1052;
    static final int CG_FLOAT2x1              = 1053;
    static final int CG_FLOAT2x2              = 1054;
    static final int CG_FLOAT2x3              = 1055;
    static final int CG_FLOAT2x4              = 1056;
    static final int CG_FLOAT3x1              = 1057;
    static final int CG_FLOAT3x2              = 1058;
    static final int CG_FLOAT3x3              = 1059;
    static final int CG_FLOAT3x4              = 1060;
    static final int CG_FLOAT4x1              = 1061;
    static final int CG_FLOAT4x2              = 1062;
    static final int CG_FLOAT4x3              = 1063;
    static final int CG_FLOAT4x4              = 1064;
    static final int CG_SAMPLER1D             = 1065;
    static final int CG_SAMPLER2D             = 1066;
    static final int CG_SAMPLER3D             = 1067;
    static final int CG_SAMPLERRECT           = 1068;
    static final int CG_SAMPLERCUBE           = 1069;
    static final int CG_FIXED                 = 1070;
    static final int CG_FIXED2                = 1071;
    static final int CG_FIXED3                = 1072;
    static final int CG_FIXED4                = 1073;
    static final int CG_FIXED1x1              = 1074;
    static final int CG_FIXED1x2              = 1075;
    static final int CG_FIXED1x3              = 1076;
    static final int CG_FIXED1x4              = 1077;
    static final int CG_FIXED2x1              = 1078;
    static final int CG_FIXED2x2              = 1079;
    static final int CG_FIXED2x3              = 1080;
    static final int CG_FIXED2x4              = 1081;
    static final int CG_FIXED3x1              = 1082;
    static final int CG_FIXED3x2              = 1083;
    static final int CG_FIXED3x3              = 1084;
    static final int CG_FIXED3x4              = 1085;
    static final int CG_FIXED4x1              = 1086;
    static final int CG_FIXED4x2              = 1087;
    static final int CG_FIXED4x3              = 1088;
    static final int CG_FIXED4x4              = 1089;
    static final int CG_HALF1                 = 1090;
    static final int CG_FLOAT1                = 1091;
    static final int CG_FIXED1                = 1092;
    // } CGtype;

    // typedef enum
    // {
    static final int CG_TEXUNIT0              = 2048;
    static final int CG_TEXUNIT1              = 2049;
    static final int CG_TEXUNIT2              = 2050;
    static final int CG_TEXUNIT3              = 2051;
    static final int CG_TEXUNIT4              = 2052;
    static final int CG_TEXUNIT5              = 2053;
    static final int CG_TEXUNIT6              = 2054;
    static final int CG_TEXUNIT7              = 2055;
    static final int CG_TEXUNIT8              = 2056;
    static final int CG_TEXUNIT9              = 2057;
    static final int CG_TEXUNIT10             = 2058;
    static final int CG_TEXUNIT11             = 2059;
    static final int CG_TEXUNIT12             = 2060;
    static final int CG_TEXUNIT13             = 2061;
    static final int CG_TEXUNIT14             = 2062;
    static final int CG_TEXUNIT15             = 2063;
    //
    static final int CG_ATTR0                 = 2113;
    static final int CG_ATTR1                 = 2114;
    static final int CG_ATTR2                 = 2115;
    static final int CG_ATTR3                 = 2116;
    static final int CG_ATTR4                 = 2117;
    static final int CG_ATTR5                 = 2118;
    static final int CG_ATTR6                 = 2119;
    static final int CG_ATTR7                 = 2120;
    static final int CG_ATTR8                 = 2121;
    static final int CG_ATTR9                 = 2122;
    static final int CG_ATTR10                = 2123;
    static final int CG_ATTR11                = 2124;
    static final int CG_ATTR12                = 2125;
    static final int CG_ATTR13                = 2126;
    static final int CG_ATTR14                = 2127;
    static final int CG_ATTR15                = 2128;
    //  
    static final int CG_C                     = 2178;
    //
    static final int CG_TEX0                  = 2179;
    static final int CG_TEX1                  = 2180;
    static final int CG_TEX2                  = 2181;
    static final int CG_TEX3                  = 2192;
    static final int CG_TEX4                  = 2193;
    static final int CG_TEX5                  = 2194;
    static final int CG_TEX6                  = 2195;
    static final int CG_TEX7                  = 2196;
    //
    static final int CG_HPOS                  = 2243;
    static final int CG_COL0                  = 2245;
    static final int CG_COL1                  = 2246;
    static final int CG_COL2                  = 2247;
    static final int CG_COL3                  = 2248;
    static final int CG_PSIZ                  = 2309;
    static final int CG_WPOS                  = 2373;
    //
    static final int CG_POSITION0             = 2437;
    static final int CG_POSITION1             = 2438;
    static final int CG_POSITION2             = 2439;
    static final int CG_POSITION3             = 2440;
    static final int CG_POSITION4             = 2441;
    static final int CG_POSITION5             = 2442;
    static final int CG_POSITION6             = 2443;
    static final int CG_POSITION7             = 2444;
    static final int CG_POSITION8             = 2445;
    static final int CG_POSITION9             = 2446;
    static final int CG_POSITION10            = 2447;
    static final int CG_POSITION11            = 2448;
    static final int CG_POSITION12            = 2449;
    static final int CG_POSITION13            = 2450;
    static final int CG_POSITION14            = 2451;
    static final int CG_POSITION15            = 2452;
    static final int CG_DIFFUSE0              = 2501;
    static final int CG_TANGENT0              = 2565;
    static final int CG_TANGENT1              = 2566;
    static final int CG_TANGENT2              = 2567;
    static final int CG_TANGENT3              = 2568;
    static final int CG_TANGENT4              = 2569;
    static final int CG_TANGENT5              = 2570;
    static final int CG_TANGENT6              = 2571;
    static final int CG_TANGENT7              = 2572;
    static final int CG_TANGENT8              = 2573;
    static final int CG_TANGENT9              = 2574;
    static final int CG_TANGENT10             = 2575;
    static final int CG_TANGENT11             = 2576;
    static final int CG_TANGENT12             = 2577;
    static final int CG_TANGENT13             = 2578;
    static final int CG_TANGENT14             = 2579;
    static final int CG_TANGENT15             = 2580;
    static final int CG_SPECULAR0             = 2629;
    static final int CG_BLENDINDICES0         = 2693;
    static final int CG_BLENDINDICES1         = 2694;
    static final int CG_BLENDINDICES2         = 2695;
    static final int CG_BLENDINDICES3         = 2696;
    static final int CG_BLENDINDICES4         = 2697;
    static final int CG_BLENDINDICES5         = 2698;
    static final int CG_BLENDINDICES6         = 2699;
    static final int CG_BLENDINDICES7         = 2700;
    static final int CG_BLENDINDICES8         = 2701;
    static final int CG_BLENDINDICES9         = 2702;
    static final int CG_BLENDINDICES10        = 2703;
    static final int CG_BLENDINDICES11        = 2704;
    static final int CG_BLENDINDICES12        = 2705;
    static final int CG_BLENDINDICES13        = 2706;
    static final int CG_BLENDINDICES14        = 2707;
    static final int CG_BLENDINDICES15        = 2708;
    static final int CG_COLOR0                = 2757;
    static final int CG_COLOR1                = 2758;
    static final int CG_COLOR2                = 2759;
    static final int CG_COLOR3                = 2760;
    static final int CG_COLOR4                = 2761;
    static final int CG_COLOR5                = 2762;
    static final int CG_COLOR6                = 2763;
    static final int CG_COLOR7                = 2764;
    static final int CG_COLOR8                = 2765;
    static final int CG_COLOR9                = 2766;
    static final int CG_COLOR10               = 2767;
    static final int CG_COLOR11               = 2768;
    static final int CG_COLOR12               = 2769;
    static final int CG_COLOR13               = 2770;
    static final int CG_COLOR14               = 2771;
    static final int CG_COLOR15               = 2772;
    static final int CG_PSIZE0                = 2821;
    static final int CG_PSIZE1                = 2822;
    static final int CG_PSIZE2                = 2823;
    static final int CG_PSIZE3                = 2824;
    static final int CG_PSIZE4                = 2825;
    static final int CG_PSIZE5                = 2826;
    static final int CG_PSIZE6                = 2827;
    static final int CG_PSIZE7                = 2828;
    static final int CG_PSIZE8                = 2829;
    static final int CG_PSIZE9                = 2830;
    static final int CG_PSIZE10               = 2831;
    static final int CG_PSIZE11               = 2832;
    static final int CG_PSIZE12               = 2833;
    static final int CG_PSIZE13               = 2834;
    static final int CG_PSIZE14               = 2835;
    static final int CG_PSIZE15               = 2836;
    static final int CG_BINORMAL0             = 2885;
    static final int CG_BINORMAL1             = 2886;
    static final int CG_BINORMAL2             = 2887;
    static final int CG_BINORMAL3             = 2888;
    static final int CG_BINORMAL4             = 2889;
    static final int CG_BINORMAL5             = 2890;
    static final int CG_BINORMAL6             = 2891;
    static final int CG_BINORMAL7             = 2892;
    static final int CG_BINORMAL8             = 2893;
    static final int CG_BINORMAL9             = 2894;
    static final int CG_BINORMAL10            = 2895;
    static final int CG_BINORMAL11            = 2896;
    static final int CG_BINORMAL12            = 2897;
    static final int CG_BINORMAL13            = 2898;
    static final int CG_BINORMAL14            = 2899;
    static final int CG_BINORMAL15            = 2900;
    static final int CG_FOG0                  = 2917;
    static final int CG_FOG1                  = 2918;
    static final int CG_FOG2                  = 2919;
    static final int CG_FOG3                  = 2920;
    static final int CG_FOG4                  = 2921;
    static final int CG_FOG5                  = 2922;
    static final int CG_FOG6                  = 2923;
    static final int CG_FOG7                  = 2924;
    static final int CG_FOG8                  = 2925;
    static final int CG_FOG9                  = 2926;
    static final int CG_FOG10                 = 2927;
    static final int CG_FOG11                 = 2928;
    static final int CG_FOG12                 = 2929;
    static final int CG_FOG13                 = 2930;
    static final int CG_FOG14                 = 2931;
    static final int CG_FOG15                 = 2932;
    static final int CG_DEPTH0                = 2933;
    static final int CG_DEPTH1                = 2934;
    static final int CG_DEPTH2                = 2935;
    static final int CG_DEPTH3                = 2936;
    static final int CG_DEPTH4                = 2937;
    static final int CG_DEPTH5                = 2938;
    static final int CG_DEPTH6                = 2939;
    static final int CG_DEPTH7                = 2940;
    static final int CG_DEPTH8                = 2941;
    static final int CG_DEPTH9                = 29542;
    static final int CG_DEPTH10               = 2943;
    static final int CG_DEPTH11               = 2944;
    static final int CG_DEPTH12               = 2945;
    static final int CG_DEPTH13               = 2946;
    static final int CG_DEPTH14               = 2947;
    static final int CG_DEPTH15               = 2948;
    static final int CG_SAMPLE0               = 2949;
    static final int CG_SAMPLE1               = 2950;
    static final int CG_SAMPLE2               = 2951;
    static final int CG_SAMPLE3               = 2952;
    static final int CG_SAMPLE4               = 2953;
    static final int CG_SAMPLE5               = 2954;
    static final int CG_SAMPLE6               = 2955;
    static final int CG_SAMPLE7               = 2956;
    static final int CG_SAMPLE8               = 2957;
    static final int CG_SAMPLE9               = 2958;
    static final int CG_SAMPLE10              = 2959;
    static final int CG_SAMPLE11              = 2960;
    static final int CG_SAMPLE12              = 2961;
    static final int CG_SAMPLE13              = 2962;
    static final int CG_SAMPLE14              = 2963;
    static final int CG_SAMPLE15              = 2964;
    static final int CG_BLENDWEIGHT0          = 3028;
    static final int CG_BLENDWEIGHT1          = 3029;
    static final int CG_BLENDWEIGHT2          = 3030;
    static final int CG_BLENDWEIGHT3          = 3031;
    static final int CG_BLENDWEIGHT4          = 3032;
    static final int CG_BLENDWEIGHT5          = 3033;
    static final int CG_BLENDWEIGHT6          = 3034;
    static final int CG_BLENDWEIGHT7          = 3035;
    static final int CG_BLENDWEIGHT8          = 3036;
    static final int CG_BLENDWEIGHT9          = 3037;
    static final int CG_BLENDWEIGHT10         = 3038;
    static final int CG_BLENDWEIGHT11         = 3039;
    static final int CG_BLENDWEIGHT12         = 3040;
    static final int CG_BLENDWEIGHT13         = 3041;
    static final int CG_BLENDWEIGHT14         = 3042;
    static final int CG_BLENDWEIGHT15         = 3043;
    static final int CG_NORMAL0               = 3092;
    static final int CG_NORMAL1               = 3093;
    static final int CG_NORMAL2               = 3094;
    static final int CG_NORMAL3               = 3095;
    static final int CG_NORMAL4               = 3096;
    static final int CG_NORMAL5               = 3097;
    static final int CG_NORMAL6               = 3098;
    static final int CG_NORMAL7               = 3099;
    static final int CG_NORMAL8               = 3100;
    static final int CG_NORMAL9               = 3101;
    static final int CG_NORMAL10              = 3102;
    static final int CG_NORMAL11              = 3103;
    static final int CG_NORMAL12              = 3104;
    static final int CG_NORMAL13              = 3105;
    static final int CG_NORMAL14              = 3106;
    static final int CG_NORMAL15              = 3107;
    static final int CG_FOGCOORD              = 3156;
    static final int CG_TEXCOORD0             = 3220;
    static final int CG_TEXCOORD1             = 3221;
    static final int CG_TEXCOORD2             = 3222;
    static final int CG_TEXCOORD3             = 3223;
    static final int CG_TEXCOORD4             = 3224;
    static final int CG_TEXCOORD5             = 3225;
    static final int CG_TEXCOORD6             = 3226;
    static final int CG_TEXCOORD7             = 3227;
    static final int CG_TEXCOORD8             = 3228;
    static final int CG_TEXCOORD9             = 3229;
    static final int CG_TEXCOORD10            = 3230;
    static final int CG_TEXCOORD11            = 3231;
    static final int CG_TEXCOORD12            = 3232;
    static final int CG_TEXCOORD13            = 3233;
    static final int CG_TEXCOORD14            = 3234;
    static final int CG_TEXCOORD15            = 3235;
    static final int CG_COMBINER_CONST0       = 3284;
    static final int CG_COMBINER_CONST1       = 3285;
    static final int CG_COMBINER_STAGE_CONST0 = 3286;
    static final int CG_COMBINER_STAGE_CONST1 = 3287;
    static final int CG_OFFSET_TEXTURE_MATRIX = 3288;
    static final int CG_OFFSET_TEXTURE_SCALE  = 3289;
    static final int CG_OFFSET_TEXTURE_BIAS   = 3290;
    static final int CG_CONST_EYE             = 3291;
    static final int CG_TESSFACTOR            = 3255;
    //
    //
    static final int CG_UNDEFINED             = 3256;
    // } CGresource;

    // typedef enum
    // {
    static final int CG_PROFILE_START   = 6144;
    static final int CG_PROFILE_UNKNOWN = 6145;
    //
    static final int CG_PROFILE_VP20    = 6146;
    static final int CG_PROFILE_FP20    = 6147;
    static final int CG_PROFILE_VP30    = 6148;
    static final int CG_PROFILE_FP30    = 6149;
    static final int CG_PROFILE_ARBVP1  = 6150;
    static final int CG_PROFILE_ARBFP1  = 7000;
    //
    //
    static final int CG_PROFILE_VS_1_1  = 6153;
    static final int CG_PROFILE_VS_2_0  = 6154;
    static final int CG_PROFILE_VS_2_X  = 6155;
    //
    static final int CG_PROFILE_PS_1_1  = 6159;
    static final int CG_PROFILE_PS_1_2  = 6160;
    static final int CG_PROFILE_PS_1_3  = 6161;
    static final int CG_PROFILE_PS_2_0  = 6162;
    static final int CG_PROFILE_PS_2_X  = 6163;
    //
    static final int CG_PROFILE_MAX     = 6164;
    // } CGprofile;

    public enum CGerror {

        CG_NO_ERROR,// = 0,
        CG_COMPILER_ERROR,// = 1,
        CG_INVALID_PARAMETER_ERROR,//= 2,
        CG_INVALID_PROFILE_ERROR,// = 3,
        CG_PROGRAM_LOAD_ERROR,// = 4,
        CG_PROGRAM_BIND_ERROR,// = 5,
        CG_PROGRAM_NOT_LOADED_ERROR,// = 6,
        CG_UNSUPPORTED_GL_EXTENSION_ERROR,// = 7,
        CG_INVALID_VALUE_TYPE_ERROR,// = 8,
        CG_NOT_MATRIX_PARAM_ERROR,// = 9,
        CG_INVALID_ENUMERANT_ERROR,// = 10,
        CG_NOT_4x4_MATRIX_ERROR,// = 11,
        CG_FILE_READ_ERROR,// = 12,
        CG_FILE_WRITE_ERROR,// = 13,
        CG_NVPARSE_ERROR,// = 14,
        CG_MEMORY_ALLOC_ERROR,// = 15,
        CG_INVALID_CONTEXT_HANDLE_ERROR,// = 16,
        CG_INVALID_PROGRAM_HANDLE_ERROR,// = 17,
        CG_INVALID_PARAM_HANDLE_ERROR,// = 18,
        CG_UNKNOWN_PROFILE_ERROR,// = 19,
        CG_VAR_ARG_ERROR,// = 20,
        CG_INVALID_DIMENSION_ERROR,// = 21,
        CG_ARRAY_PARAM_ERROR,// = 22,
        CG_OUT_OF_ARRAY_BOUNDS_ERROR,// = 23,
    };

    // typedef enum
    // {
    static final int CG_UNKNOWN          = 4096;
    static final int CG_IN               = 4097;
    static final int CG_OUT              = 4098;
    static final int CG_INOUT            = 4099;
    static final int CG_MIXED            = 4101;
    static final int CG_VARYING          = 4102;
    static final int CG_UNIFORM          = 4103;
    static final int CG_CONSTANT         = 4104;
    static final int CG_PROGRAM_SOURCE   = 4105;
    static final int CG_PROGRAM_ENTRY    = 4106;
    static final int CG_COMPILED_PROGRAM = 4107;
    static final int CG_PROGRAM_PROFILE  = 4108;
    //
    static final int CG_GLOBAL           = 4109;
    static final int CG_PROGRAM          = 4110;
    //
    static final int CG_DEFAULT          = 4111;
    static final int CG_ERROR            = 4112;
    //
    static final int CG_SOURCE           = 4113;
    static final int CG_OBJECT           = 4114;
    // } CGenum;

    public enum CGGLenum {

        CG_GL_MATRIX_IDENTITY,//= 0,
        CG_GL_MATRIX_TRANSPOSE,//= 1,
        CG_GL_MATRIX_INVERSE,//= 2,
        CG_GL_MATRIX_INVERSE_TRANSPOSE,// = 3,
        //
        CG_GL_MODELVIEW_MATRIX,
        CG_GL_PROJECTION_MATRIX,
        CG_GL_TEXTURE_MATRIX,
        CG_GL_MODELVIEW_PROJECTION_MATRIX,
        //
        CG_GL_VERTEX,
        CG_GL_FRAGMENT,

    };

}
