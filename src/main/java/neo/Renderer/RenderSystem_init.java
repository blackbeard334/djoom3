package neo.Renderer;

import static java.lang.Math.random;
import static neo.Renderer.Image.globalImages;
import static neo.Renderer.Image.cubeFiles_t.CF_2D;
import static neo.Renderer.Image.textureDepth_t.TD_DEFAULT;
import static neo.Renderer.Image_files.R_LoadImage;
import static neo.Renderer.Image_files.R_WriteTGA;
import static neo.Renderer.Image_program.R_LoadImageProgram;
import static neo.Renderer.Material.textureFilter_t.TF_DEFAULT;
import static neo.Renderer.Material.textureRepeat_t.TR_REPEAT;
import static neo.Renderer.ModelManager.renderModelManager;
import static neo.Renderer.RenderSystem.renderSystem;
import static neo.Renderer.VertexCache.vertexCache;
import static neo.Renderer.draw_arb2.R_ARB2_Init;
import static neo.Renderer.tr_lightrun.R_FreeDerivedData;
import static neo.Renderer.tr_local.MAX_MULTITEXTURE_UNITS;
import static neo.Renderer.tr_local.glConfig;
import static neo.Renderer.tr_local.tr;
import static neo.Renderer.tr_local.backEndName_t.BE_ARB;
import static neo.Renderer.tr_local.backEndName_t.BE_ARB2;
import static neo.Renderer.tr_local.backEndName_t.BE_NV10;
import static neo.Renderer.tr_local.backEndName_t.BE_NV20;
import static neo.Renderer.tr_local.backEndName_t.BE_R200;
import static neo.Renderer.tr_main.R_InitFrameData;
import static neo.Renderer.tr_main.R_ToggleSmpFrame;
import static neo.TempDump.NOT;
import static neo.TempDump.atof;
import static neo.TempDump.btoi;
import static neo.TempDump.ctos;
import static neo.TempDump.isNotNullOrEmpty;
import static neo.TempDump.itob;
import static neo.framework.BuildDefines._WIN32;
import static neo.framework.CVarSystem.CVAR_ARCHIVE;
import static neo.framework.CVarSystem.CVAR_BOOL;
import static neo.framework.CVarSystem.CVAR_CHEAT;
import static neo.framework.CVarSystem.CVAR_FLOAT;
import static neo.framework.CVarSystem.CVAR_INTEGER;
import static neo.framework.CVarSystem.CVAR_NOCHEAT;
import static neo.framework.CVarSystem.CVAR_RENDERER;
import static neo.framework.CmdSystem.CMD_FL_CHEAT;
import static neo.framework.CmdSystem.CMD_FL_RENDERER;
import static neo.framework.CmdSystem.cmdSystem;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_APPEND;
import static neo.framework.CmdSystem.cmdExecution_t.CMD_EXEC_NOW;
import static neo.framework.Console.console;
import static neo.framework.DeclManager.declManager;
import static neo.framework.DeclManager.declType_t.DECL_MATERIAL;
import static neo.framework.FileSystem_h.fileSystem;
import static neo.framework.Session.session;
import static neo.idlib.Lib.idLib.common;
import static neo.idlib.Lib.idLib.cvarSystem;
import static neo.opengl.QGL.qglFinish;
import static neo.opengl.QGL.qglGetError;
import static neo.opengl.QGL.qglGetFloatv;
import static neo.opengl.QGL.qglGetInteger;
import static neo.opengl.QGL.qglGetIntegerv;
import static neo.opengl.QGL.qglGetString;
import static neo.opengl.QGL.qglGetStringi;
import static neo.opengl.QGL.qglReadBuffer;
import static neo.opengl.QGL.qglReadPixels;
import static neo.opengl.QGLConstantsIfc.GL_DECR;
import static neo.opengl.QGLConstantsIfc.GL_DECR_WRAP_EXT;
import static neo.opengl.QGLConstantsIfc.GL_EXTENSIONS;
import static neo.opengl.QGLConstantsIfc.GL_FRONT;
import static neo.opengl.QGLConstantsIfc.GL_INCR;
import static neo.opengl.QGLConstantsIfc.GL_INCR_WRAP_EXT;
import static neo.opengl.QGLConstantsIfc.GL_INVALID_ENUM;
import static neo.opengl.QGLConstantsIfc.GL_INVALID_OPERATION;
import static neo.opengl.QGLConstantsIfc.GL_INVALID_VALUE;
import static neo.opengl.QGLConstantsIfc.GL_MAX_TEXTURE_COORDS_ARB;
import static neo.opengl.QGLConstantsIfc.GL_MAX_TEXTURE_IMAGE_UNITS_ARB;
import static neo.opengl.QGLConstantsIfc.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static neo.opengl.QGLConstantsIfc.GL_MAX_TEXTURE_SIZE;
import static neo.opengl.QGLConstantsIfc.GL_MAX_TEXTURE_UNITS_ARB;
import static neo.opengl.QGLConstantsIfc.GL_NO_ERROR;
import static neo.opengl.QGLConstantsIfc.GL_OUT_OF_MEMORY;
import static neo.opengl.QGLConstantsIfc.GL_RENDERER;
import static neo.opengl.QGLConstantsIfc.GL_RGB;
import static neo.opengl.QGLConstantsIfc.GL_STACK_OVERFLOW;
import static neo.opengl.QGLConstantsIfc.GL_STACK_UNDERFLOW;
import static neo.opengl.QGLConstantsIfc.GL_STENCIL_INDEX;
import static neo.opengl.QGLConstantsIfc.GL_UNSIGNED_BYTE;
import static neo.opengl.QGLConstantsIfc.GL_VENDOR;
import static neo.opengl.QGLConstantsIfc.GL_VERSION;
import static neo.sys.win_glimp.GLimp_Init;
import static neo.sys.win_glimp.GLimp_SetScreenParms;
import static neo.sys.win_glimp.GLimp_Shutdown;
import static neo.sys.win_input.Sys_GrabMouseCursor;
import static neo.sys.win_input.Sys_InitInput;
import static neo.sys.win_input.Sys_ShutdownInput;
import static neo.sys.win_main.Sys_GetProcessorString;
import static neo.sys.win_shared.Sys_Milliseconds;
import static neo.ui.UserInterface.uiManager;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;

import neo.Renderer.Cinematic.cinData_t;
import neo.Renderer.Cinematic.idCinematic;
import neo.Renderer.Image.idImage;
import neo.Renderer.Interaction.R_ShowInteractionMemory_f;
import neo.Renderer.Material.idMaterial;
import neo.Renderer.MegaTexture.idMegaTexture;
import neo.Renderer.RenderWorld.R_ListRenderEntityDefs_f;
import neo.Renderer.RenderWorld.R_ListRenderLightDefs_f;
import neo.Renderer.RenderWorld.modelTrace_s;
import neo.Renderer.RenderWorld.renderView_s;
import neo.Renderer.draw_arb2.R_ReloadARBPrograms_f;
import neo.Renderer.tr_guisurf.R_ListGuis_f;
import neo.Renderer.tr_guisurf.R_ReloadGuis_f;
import neo.Renderer.tr_lightrun.R_ModulateLights_f;
import neo.Renderer.tr_lightrun.R_RegenerateWorld_f;
import neo.Renderer.tr_local.viewDef_s;
import neo.Renderer.tr_trisurf.R_ShowTriSurfMemory_f;
import neo.Sound.snd_system;
import neo.framework.CVarSystem.idCVar;
import neo.framework.CmdSystem.cmdFunction_t;
import neo.framework.CmdSystem.idCmdSystem;
import neo.framework.Common;
import neo.idlib.CmdArgs.idCmdArgs;
import neo.idlib.Text.Str.idStr;
import neo.idlib.containers.List.cmp_t;
import neo.idlib.math.Vector.idVec3;
import neo.idlib.math.Matrix.idMat3;
import neo.sys.win_glimp.glimpParms_t;

/**
 *
 */
public class RenderSystem_init {

    static final String[] r_rendererArgs = {"best", "arb", "arb2", "Cg", "exp", "nv10", "nv20", "r200", null};

    public static final idCVar r_useEntityCallbacks;            // if 0, issue the callback immediately at update time, rather than defering
    public static final idCVar r_skipFogLights;                 // skip all fog lights
    public static final idCVar r_znear;                         // near Z clip plane
    public static final idCVar r_showVertexColor;               // draws all triangles with the solid vertex color
    public static final idCVar r_showNormals;                   // draws wireframe normals
    public static final idCVar r_skipNewAmbient;                // bypasses all vertex/fragment program ambients
    public static final idCVar r_showDepth;                     // display the contents of the depth buffer and the depth range
    public static final idCVar r_showAlloc;                     // report alloc/free counts
    public static final idCVar r_useShadowProjectedCull;        // 1 = discard triangles outside light volume before shadowing
    public static final idCVar r_useEntityScissors;             // 1 = use custom scissor rectangle for each entity
    //
    public static final idCVar r_testGamma;                     // draw a grid pattern to test gamma levels
    //
    public static final idCVar r_cgVertexProfile;               // arbvp1, vp20, vp30
    public static final idCVar r_swapInterval;                  // changes wglSwapIntarval
    public static final idCVar r_showPrimitives;                // report vertex/index/draw counts
    public static final idCVar r_orderIndexes;                  // perform index reorganization to optimize vertex use
    public static final idCVar r_lightAllBackFaces;             // light all the back faces, even when they would be shadowed
    public static final idCVar r_useLightScissors;              // 1 = use custom scissor rectangle for each light
    public static final idCVar r_useLightPortalFlow;            // 1 = do a more precise area reference determination
    public static final idCVar r_useExternalShadows;            // 1 = skip drawing caps when outside the light volume
    public static final idCVar r_useFrustumFarDistance;         // if != 0 force the view frustum far distance to this distance
    //
    public static final idCVar r_checkBounds;                   // compare all surface bounds with precalculated ones
    public static final idCVar r_skipCopyTexture;               // do all rendering, but don't actually copyTexSubImage2D
    public static final idCVar r_brightness;                    // changes gamma tables
    public static final idCVar r_useCombinerDisplayLists;       // if 1, put all nvidia register combiner programming in display lists
    public static final idCVar r_showSmp;                       // show which end (front or back) is blocking
    public static final idCVar r_useStateCaching;               // avoid redundant state changes in GL_*() calls
    public static final idCVar r_showMemory;                    // print frame memory utilization
    public static final idCVar r_testGammaBias;                 // draw a grid pattern to test gamma levels
    public static final idCVar r_showPortals;                   // draw portal outlines in color based on passed / not passed
    //
    public static final idCVar r_inhibitFragmentProgram;
    public static final idCVar r_useTwoSidedStencil;            // 1 = do stencil shadows in one pass with different ops on each side
    public static final idCVar r_useTripleTextureARB;           // 1 = cards with 3+ texture units do a two pass instead of three pass
    public static final idCVar r_shadowPolygonOffset;           // bias value added to depth test for stencil shadow drawing
    public static final idCVar r_showTrace;                     // show the intersection of an eye trace with the world
    public static final idCVar r_showInteractions;              // report interaction generation activity
    public static final idCVar r_useShadowVertexProgram;        // 1 = do the shadow projection in the vertex program on capable cards
    //
    public static final idCVar r_testARBProgram;                // experiment with vertex/fragment programs
    public static final idCVar r_showTextureVectors;            // draw each triangles texture (tangent) vectors
    public static final idCVar r_jointNameOffset;               // offset of joint names when r_showskel is set to 1
    public static final idCVar r_skipBackEnd;                   // don't draw anything
    public static final idCVar r_shadowPolygonFactor;           // scale value for stencil shadow drawing
    public static final idCVar r_skipFrontEnd;                  // bypasses all front end work, but 2D gui rendering still draws
    public static final idCVar r_skipGuiShaders;                // 1 = don't render any gui elements on surfaces
    public static final idCVar r_jointNameScale;                // size of joint names when r_showskel is set to 1
    public static final idCVar r_showTexturePolarity;           // shade triangles by texture area polarity
    public static final idCVar r_showDominantTri;               // draw lines from vertexes to center of dominant triangles
    public static final idCVar r_skipRender;                    // skip 3D rendering, but pass 2D
    public static final idCVar r_skipParticles;                 // 1 = don't render any particles
    public static final idCVar r_usePortals;                    // 1 = use portals to perform area culling, otherwise draw everything
    public static final idCVar r_useShadowSurfaceScissor;       // 1 = scissor shadows by the scissor rect of the interaction surfaces
    public static final idCVar r_showLightCount;                // colors surfaces based on light count
    //
    public static final idCVar r_ignore;                        // used for random debugging without defining new vars
    public static final idCVar r_lightScale;                    // all light intensities are multiplied by this, which is normally 2
    //
    public static final idCVar r_debugRenderToTexture;
    //
    public static final idCVar r_showInteractionScissors;       // show screen rectangle which contains the interaction frustum
    //
    public static final idCVar r_singleLight;                   // suppress all but one light
    public static final idCVar r_showEdges;                     // draw the sil edges
    public static final idCVar r_offsetUnits;                   // polygon offset parameter
    public static final idCVar r_showUpdates;                   // report entity and light updates and ref counts
    public static final idCVar r_useInteractionCulling;         // 1 = cull interactions
    public static final idCVar r_showImages;                    // draw all images to screen instead of rendering
    public static final idCVar r_skipAmbient;                   // bypasses all non-interaction drawing
    public static final idCVar r_useDeferredTangents;           // 1 = don't always calc tangents after deform
    public static final idCVar r_clear;                         // force screen clear every frame
    public static final idCVar r_multiSamples;                  // number of antialiasing samples
    public static final idCVar r_useConstantMaterials;          // 1 = use pre-calculated material registers if possible
    //
    //
    //
    // cvars
    //
    public static final idCVar r_ext_vertex_array_range;
    //
    public static final idCVar r_finish;                        // force a call to glFinish() every frame
    public static final idCVar r_debugArrowStep;                // step size of arrow cone line rotation in degrees
    //
    public static final idCVar r_renderer;                      // arb, nv10, nv20, r200, gl2, etc
    public static final idCVar r_showViewEntitys;               // displays the bounding boxes of all view models and optionally the index
    public static final idCVar r_displayRefresh;                // optional display refresh rate option for vid mode
    public static final idCVar r_showTris;                      // enables wireframe rendering of the world
    public static final idCVar r_singleArea;                    // only draw the portal area the view is actually in
    public static final idCVar r_showDynamic;                   // report stats on dynamic surface generation
    public static final idCVar r_singleSurface;                 // suppress all but one surface on each entity
    public static final idCVar r_skipDeforms;                   // leave all deform materials in their original state
    public static final idCVar r_skipTranslucent;               // skip the translucent interaction rendering
    public static final idCVar r_lightSourceRadius;             // for soft-shadow sampling
    public static final idCVar r_useVertexBuffers;              // if 0, don't use ARB_vertex_buffer_object for vertexes
    public static final idCVar r_skipBump;                      // uses a flat surface instead of the bump map
    public static final idCVar r_showIntensity;                 // draw the screen colors based on intensity, red = 0, green = 128, blue = 255
    public static final idCVar r_demonstrateBug;                // used during development to show IHV's their problems
    public static final idCVar r_showEntityScissors;            // show entity scissor rectangles
    public static final idCVar r_useSilRemap;                   // 1 = consider verts with the same XYZ, but different ST the same for shadows
    public static final idCVar r_useIndexBuffers;               // if 0, don't use ARB_vertex_buffer_object for indexes
    public static final idCVar r_skipDynamicTextures;           // don't dynamically create textures
    public static final idCVar r_showShadowCount;               // colors screen based on shadow volume depth complexity
    public static final idCVar r_useLightCulling;               // 0 = none, 1 = box, 2 = exact clip of polyhedron faces
    public static final idCVar r_skipSubviews;                  // 1 = don't render any mirrors / cameras / etc
    public static final idCVar r_showDefs;                      // report the number of modeDefs and lightDefs in view
    public static final idCVar r_mode;                          // video mode number
    public static final idCVar r_skipRenderContext;             // NULL the rendering context during backend 3D rendering
    public static final idCVar r_ignore2;                       // used for random debugging without defining new vars
    public static final idCVar r_useOptimizedShadows;           // 1 = use the dmap generated static shadow volumes
    //
    public static final idCVar r_debugLineDepthTest;            // perform depth test on debug lines
    public static final idCVar r_showCull;                      // report sphere and box culling stats
    public static final idCVar r_showDemo;                      // report reads and writes to the demo file
    public static final idCVar r_skipUpdates;                   // 1 = don't accept any entity or light updates, making everything static
    public static final idCVar r_screenFraction;                // for testing fill rate, the resolution of the entire screen can be changed
    public static final idCVar r_skipBlendLights;               // skip all blend lights
    //
    public static final idCVar r_showUnsmoothedTangents;        // highlight geometry rendered with unsmoothed tangents
    public static final idCVar r_cgFragmentProfile;             // arbfp1, fp30
    public static final idCVar r_showLights;                    // 1 = print light info, 2 = also draw volumes
    //
    public static final idCVar r_skipPostProcess;               // skip all post-process renderings
    public static final idCVar r_skipDiffuse;                   // use black for diffuse
    public static final idCVar r_customWidth;
    public static final idCVar r_skipOverlays;                  // skip overlay surfaces
    public static final idCVar r_showLightScissors;             // show light scissor rectangles
    public static final idCVar r_useTurboShadow;                // 1 = use the infinite projection with W technique for dynamic shadows
    public static final idCVar r_showSilhouette;                // highlight edges that are casting shadow planes
    public static final idCVar r_showSkel;                      // draw the skeleton when model animates
    public static final idCVar r_debugPolygonFilled;
    public static final idCVar r_showInteractionFrustums;       // show a frustum for each interaction
    public static final idCVar r_showShadows;                   // visualize the stencil shadow volumes
    public static final idCVar r_showTangentSpace;              // shade triangles by tangent space
    public static final idCVar r_useDepthBoundsTest;            // use depth bounds test to reduce shadow fill
    public static final idCVar r_useInteractionTable;           // create a full entityDefs * lightDefs table to make finding interactions faster
    public static final idCVar r_customHeight;
    public static final idCVar r_useNodeCommonChildren;         // stop pushing reference bounds early when possible
    public static final idCVar r_useCulling;                    // 0 = none, 1 = sphere, 2 = sphere + box
    public static final idCVar r_singleTriangle;                // only draw a single triangle per primitive
    public static final idCVar r_fullscreen;                    // 0 = windowed, 1 = full screen
    //
    public static final idCVar r_forceLoadImages;               // draw all images to screen after registration
    public static final idCVar r_offsetFactor;                  // polygon offset parameter
    public static final idCVar r_skipSpecular;                  // use black for specular
    public static final idCVar r_useShadowCulling;              // try to cull shadows from partially visible lights
    public static final idCVar r_useCachedDynamicModels;        // 1 = cache snapshots of dynamic models
    public static final idCVar r_useInteractionScissors;        // 1 = use a custom scissor rectangle for each interaction
    public static final idCVar r_flareSize;                     // scale the flare deforms from the material def
    public static final idCVar r_logFile;                       // number of frames to emit GL logs
    //
    public static final idCVar r_jitter;                        // randomly subpixel jitter the projection matrix
    public static final idCVar r_showLightScale;                // report the scale factor applied to drawing for overbrights
    public static final idCVar r_usePreciseTriangleInteractions;// 1 = do winding clipping to determine if each ambiguous tri should be lit
    public static final idCVar r_frontBuffer;                   // draw to front buffer for debugging
    public static final idCVar r_skipSuppress;                  // ignore the per-view suppressions
    //
    public static final idCVar r_useNV20MonoLights;             // 1 = allow an interaction pass optimization
    public static final idCVar r_useInfiniteFarZ;               // 1 = use the no-far-clip-plane trick
    public static final idCVar r_skipLightScale;                // don't do any post-interaction light scaling, makes things dim on low-dynamic range cards
    public static final idCVar r_skipInteractions;              // skip all light/surface interaction drawing
    public static final idCVar r_subviewOnly;                   // 1 = don't render main view, allowing subviews to be debugged
    public static final idCVar r_showSurfaces;                  // report surface/light/shadow counts
    public static final idCVar r_debugLineWidth;                // width of debug lines
    public static final idCVar r_showSurfaceInfo;               // show surface material name under crosshair
    public static final idCVar r_useScissor;                    // 1 = scissor clip as portals and lights are processed
    public static final idCVar r_glDriver;                      // "opengl32", etc
    public static final idCVar r_skipROQ;
    //
    public static final idCVar r_materialOverride;              // override all materials
    public static final idCVar r_useEntityCulling;              // 0 = none, 1 = box
    public static final idCVar r_singleEntity;                  // suppress all but one entity
    public static final idCVar r_shadows;                       // enable shadows
    //
    public static final idCVar r_gamma;                         // changes gamma tables
    public static final idCVar r_showOverDraw;                  // show overdraw
    public static final idCVar r_lockSurfaces;
    public static final idCVar r_useClippedLightScissors;       // 0 = full screen when near clipped, 1 = exact when near clipped, 2 = exact always
    //
    public static final idCVar r_ignoreGLErrors;
    public static final idCVar r_testStepGamma;                 // draw a grid pattern to test gamma levels

    static {
        r_ext_vertex_array_range = null;

        r_inhibitFragmentProgram = new idCVar("r_inhibitFragmentProgram", "0", CVAR_RENDERER | CVAR_BOOL, "ignore the fragment program extension");
        r_glDriver = new idCVar("r_glDriver", "", CVAR_RENDERER, "\"opengl32\", etc.");
        r_useLightPortalFlow = new idCVar("r_useLightPortalFlow", "1", CVAR_RENDERER | CVAR_BOOL, "use a more precise area reference determination");
        r_multiSamples = new idCVar("r_multiSamples", "0", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_INTEGER, "number of antialiasing samples");
        r_mode = new idCVar("r_mode", "3", CVAR_ARCHIVE | CVAR_RENDERER | CVAR_INTEGER, "video mode number");
        r_displayRefresh = new idCVar("r_displayRefresh", "0", CVAR_RENDERER | CVAR_INTEGER | CVAR_NOCHEAT, "optional display refresh rate option for vid mode", 0.0f, 200.0f);
        r_fullscreen = new idCVar("r_fullscreen", "1", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "0 = windowed, 1 = full screen");
        r_customWidth = new idCVar("r_customWidth", "720", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_INTEGER, "custom screen width. set r_mode to -1 to activate");
        r_customHeight = new idCVar("r_customHeight", "486", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_INTEGER, "custom screen height. set r_mode to -1 to activate");
        r_singleTriangle = new idCVar("r_singleTriangle", "0", CVAR_RENDERER | CVAR_BOOL, "only draw a single triangle per primitive");
        r_checkBounds = new idCVar("r_checkBounds", "0", CVAR_RENDERER | CVAR_BOOL, "compare all surface bounds with precalculated ones");

        r_useNV20MonoLights = new idCVar("r_useNV20MonoLights", "1", CVAR_RENDERER | CVAR_INTEGER, "use pass optimization for mono lights");
        r_useConstantMaterials = new idCVar("r_useConstantMaterials", "1", CVAR_RENDERER | CVAR_BOOL, "use pre-calculated material registers if possible");
        r_useTripleTextureARB = new idCVar("r_useTripleTextureARB", "1", CVAR_RENDERER | CVAR_BOOL, "cards with 3+ texture units do a two pass instead of three pass");
        r_useSilRemap = new idCVar("r_useSilRemap", "1", CVAR_RENDERER | CVAR_BOOL, "consider verts with the same XYZ, but different ST the same for shadows");
        r_useNodeCommonChildren = new idCVar("r_useNodeCommonChildren", "1", CVAR_RENDERER | CVAR_BOOL, "stop pushing reference bounds early when possible");
        r_useShadowProjectedCull = new idCVar("r_useShadowProjectedCull", "1", CVAR_RENDERER | CVAR_BOOL, "discard triangles outside light volume before shadowing");
        r_useShadowVertexProgram = new idCVar("r_useShadowVertexProgram", "1", CVAR_RENDERER | CVAR_BOOL, "do the shadow projection in the vertex program on capable cards");
        r_useShadowSurfaceScissor = new idCVar("r_useShadowSurfaceScissor", "1", CVAR_RENDERER | CVAR_BOOL, "scissor shadows by the scissor rect of the interaction surfaces");
        r_useInteractionTable = new idCVar("r_useInteractionTable", "1", CVAR_RENDERER | CVAR_BOOL, "create a full entityDefs * lightDefs table to make finding interactions faster");
        r_useTurboShadow = new idCVar("r_useTurboShadow", "1", CVAR_RENDERER | CVAR_BOOL, "use the infinite projection with W technique for dynamic shadows");
        r_useTwoSidedStencil = new idCVar("r_useTwoSidedStencil", "1", CVAR_RENDERER | CVAR_BOOL, "do stencil shadows in one pass with different ops on each side");
        r_useDeferredTangents = new idCVar("r_useDeferredTangents", "1", CVAR_RENDERER | CVAR_BOOL, "defer tangents calculations after deform");
        r_useCachedDynamicModels = new idCVar("r_useCachedDynamicModels", "1", CVAR_RENDERER | CVAR_BOOL, "cache snapshots of dynamic models");

        r_useVertexBuffers = new idCVar("r_useVertexBuffers", "1", CVAR_RENDERER | CVAR_INTEGER, "use ARB_vertex_buffer_object for vertexes", 0, 1, new idCmdSystem.ArgCompletion_Integer(0, 1));
        r_useIndexBuffers = new idCVar("r_useIndexBuffers", "0", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_INTEGER, "use ARB_vertex_buffer_object for indexes", 0, 1, new idCmdSystem.ArgCompletion_Integer(0, 1));

        r_useStateCaching = new idCVar("r_useStateCaching", "1", CVAR_RENDERER | CVAR_BOOL, "avoid redundant state changes in GL_*=new idCVar() calls");
        r_useInfiniteFarZ = new idCVar("r_useInfiniteFarZ", "1", CVAR_RENDERER | CVAR_BOOL, "use the no-far-clip-plane trick");

        r_znear = new idCVar("r_znear", "3", CVAR_RENDERER | CVAR_FLOAT, "near Z clip plane distance", 0.001f, 200.0f);

        r_ignoreGLErrors = new idCVar("r_ignoreGLErrors", "1", CVAR_RENDERER | CVAR_BOOL, "ignore GL errors");
        r_finish = new idCVar("r_finish", "0", CVAR_RENDERER | CVAR_BOOL, "force a call to glFinish=new idCVar() every frame");
        r_swapInterval = new idCVar("r_swapInterval", "0", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_INTEGER, "changes wglSwapIntarval");

        r_gamma = new idCVar("r_gamma", "1", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_FLOAT, "changes gamma tables", 0.5f, 3.0f);
        r_brightness = new idCVar("r_brightness", "1", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_FLOAT, "changes gamma tables", 0.5f, 2.0f);

        r_renderer = new idCVar("r_renderer", "best", CVAR_RENDERER | CVAR_ARCHIVE, "hardware specific renderer path to use", r_rendererArgs, new idCmdSystem.ArgCompletion_String(r_rendererArgs));

        r_jitter = new idCVar("r_jitter", "0", CVAR_RENDERER | CVAR_BOOL, "randomly subpixel jitter the projection matrix");

        r_skipSuppress = new idCVar("r_skipSuppress", "0", CVAR_RENDERER | CVAR_BOOL, "ignore the per-view suppressions");
        r_skipPostProcess = new idCVar("r_skipPostProcess", "0", CVAR_RENDERER | CVAR_BOOL, "skip all post-process renderings");
        r_skipLightScale = new idCVar("r_skipLightScale", "0", CVAR_RENDERER | CVAR_BOOL, "don't do any post-interaction light scaling, makes things dim on low-dynamic range cards");
        r_skipInteractions = new idCVar("r_skipInteractions", "0", CVAR_RENDERER | CVAR_BOOL, "skip all light/surface interaction drawing");
        r_skipDynamicTextures = new idCVar("r_skipDynamicTextures", "0", CVAR_RENDERER | CVAR_BOOL, "don't dynamically create textures");
        r_skipCopyTexture = new idCVar("r_skipCopyTexture", "0", CVAR_RENDERER | CVAR_BOOL, "do all rendering, but don't actually copyTexSubImage2D");
        r_skipBackEnd = new idCVar("r_skipBackEnd", "0", CVAR_RENDERER | CVAR_BOOL, "don't draw anything");
        r_skipRender = new idCVar("r_skipRender", "0", CVAR_RENDERER | CVAR_BOOL, "skip 3D rendering, but pass 2D");
        r_skipRenderContext = new idCVar("r_skipRenderContext", "0", CVAR_RENDERER | CVAR_BOOL, "NULL the rendering context during backend 3D rendering");
        r_skipTranslucent = new idCVar("r_skipTranslucent", "0", CVAR_RENDERER | CVAR_BOOL, "skip the translucent interaction rendering");
        r_skipAmbient = new idCVar("r_skipAmbient", "0", CVAR_RENDERER | CVAR_BOOL, "bypasses all non-interaction drawing");
        r_skipNewAmbient = new idCVar("r_skipNewAmbient", "0", CVAR_RENDERER | CVAR_BOOL | CVAR_ARCHIVE, "bypasses all vertex/fragment program ambient drawing");
        r_skipBlendLights = new idCVar("r_skipBlendLights", "0", CVAR_RENDERER | CVAR_BOOL, "skip all blend lights");
        r_skipFogLights = new idCVar("r_skipFogLights", "0", CVAR_RENDERER | CVAR_BOOL, "skip all fog lights");
        r_skipDeforms = new idCVar("r_skipDeforms", "0", CVAR_RENDERER | CVAR_BOOL, "leave all deform materials in their original state");
        r_skipFrontEnd = new idCVar("r_skipFrontEnd", "0", CVAR_RENDERER | CVAR_BOOL, "bypasses all front end work, but 2D gui rendering still draws");
        r_skipUpdates = new idCVar("r_skipUpdates", "0", CVAR_RENDERER | CVAR_BOOL, "1 = don't accept any entity or light updates, making everything static");
        r_skipOverlays = new idCVar("r_skipOverlays", "0", CVAR_RENDERER | CVAR_BOOL, "skip overlay surfaces");
        r_skipSpecular = new idCVar("r_skipSpecular", "0", CVAR_RENDERER | CVAR_BOOL | CVAR_CHEAT | CVAR_ARCHIVE, "use black for specular1");
        r_skipBump = new idCVar("r_skipBump", "0", CVAR_RENDERER | CVAR_BOOL | CVAR_ARCHIVE, "uses a flat surface instead of the bump map");
        r_skipDiffuse = new idCVar("r_skipDiffuse", "0", CVAR_RENDERER | CVAR_BOOL, "use black for diffuse");
        r_skipROQ = new idCVar("r_skipROQ", "0", CVAR_RENDERER | CVAR_BOOL, "skip ROQ decoding");

        r_ignore = new idCVar("r_ignore", "0", CVAR_RENDERER, "used for random debugging without defining new vars");
        r_ignore2 = new idCVar("r_ignore2", "0", CVAR_RENDERER, "used for random debugging without defining new vars");
        r_usePreciseTriangleInteractions = new idCVar("r_usePreciseTriangleInteractions", "0", CVAR_RENDERER | CVAR_BOOL, "1 = do winding clipping to determine if each ambiguous tri should be lit");
        r_useCulling = new idCVar("r_useCulling", "2", CVAR_RENDERER | CVAR_INTEGER, "0 = none, 1 = sphere, 2 = sphere + box", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        r_useLightCulling = new idCVar("r_useLightCulling", "3", CVAR_RENDERER | CVAR_INTEGER, "0 = none, 1 = box, 2 = exact clip of polyhedron faces, 3 = also areas", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
        r_useLightScissors = new idCVar("r_useLightScissors", "1", CVAR_RENDERER | CVAR_BOOL, "1 = use custom scissor rectangle for each light");
        r_useClippedLightScissors = new idCVar("r_useClippedLightScissors", "1", CVAR_RENDERER | CVAR_INTEGER, "0 = full screen when near clipped, 1 = exact when near clipped, 2 = exact always", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        r_useEntityCulling = new idCVar("r_useEntityCulling", "1", CVAR_RENDERER | CVAR_BOOL, "0 = none, 1 = box");
        r_useEntityScissors = new idCVar("r_useEntityScissors", "0", CVAR_RENDERER | CVAR_BOOL, "1 = use custom scissor rectangle for each entity");
        r_useInteractionCulling = new idCVar("r_useInteractionCulling", "1", CVAR_RENDERER | CVAR_BOOL, "1 = cull interactions");
        r_useInteractionScissors = new idCVar("r_useInteractionScissors", "2", CVAR_RENDERER | CVAR_INTEGER, "1 = use a custom scissor rectangle for each shadow interaction, 2 = also crop using portal scissors", -2, 2, new idCmdSystem.ArgCompletion_Integer(-2, 2));
        r_useShadowCulling = new idCVar("r_useShadowCulling", "1", CVAR_RENDERER | CVAR_BOOL, "try to cull shadows from partially visible lights");
        r_useFrustumFarDistance = new idCVar("r_useFrustumFarDistance", "0", CVAR_RENDERER | CVAR_FLOAT, "if != 0 force the view frustum far distance to this distance");
        r_logFile = new idCVar("r_logFile", "0", CVAR_RENDERER | CVAR_INTEGER, "number of frames to emit GL logs");
        r_clear = new idCVar("r_clear", "2", CVAR_RENDERER, "force screen clear every frame, 1 = purple, 2 = black, 'r g b' = custom");
        r_offsetFactor = new idCVar("r_offsetfactor", "0", CVAR_RENDERER | CVAR_FLOAT, "polygon offset parameter");
        r_offsetUnits = new idCVar("r_offsetunits", "-600", CVAR_RENDERER | CVAR_FLOAT, "polygon offset parameter");
        r_shadowPolygonOffset = new idCVar("r_shadowPolygonOffset", "-1", CVAR_RENDERER | CVAR_FLOAT, "bias value added to depth test for stencil shadow drawing");
        r_shadowPolygonFactor = new idCVar("r_shadowPolygonFactor", "0", CVAR_RENDERER | CVAR_FLOAT, "scale value for stencil shadow drawing");
        r_frontBuffer = new idCVar("r_frontBuffer", "0", CVAR_RENDERER | CVAR_BOOL, "draw to front buffer for debugging");
        r_skipSubviews = new idCVar("r_skipSubviews", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = don't render any gui elements on surfaces");
        r_skipGuiShaders = new idCVar("r_skipGuiShaders", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = skip all gui elements on surfaces, 2 = skip drawing but still handle events, 3 = draw but skip events", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
        r_skipParticles = new idCVar("r_skipParticles", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = skip all particle systems", 0, 1, new idCmdSystem.ArgCompletion_Integer(0, 1));
        r_subviewOnly = new idCVar("r_subviewOnly", "0", CVAR_RENDERER | CVAR_BOOL, "1 = don't render main view, allowing subviews to be debugged");
        r_shadows = new idCVar("r_shadows", "1", CVAR_RENDERER | CVAR_BOOL | CVAR_ARCHIVE, "enable shadows");
        r_testARBProgram = new idCVar("r_testARBProgram", "0", CVAR_RENDERER | CVAR_BOOL, "experiment with vertex/fragment programs");
        r_testGamma = new idCVar("r_testGamma", "0", CVAR_RENDERER | CVAR_FLOAT, "if > 0 draw a grid pattern to test gamma levels", 0, 195);
        r_testGammaBias = new idCVar("r_testGammaBias", "0", CVAR_RENDERER | CVAR_FLOAT, "if > 0 draw a grid pattern to test gamma levels");
        r_testStepGamma = new idCVar("r_testStepGamma", "0", CVAR_RENDERER | CVAR_FLOAT, "if > 0 draw a grid pattern to test gamma levels");
        r_lightScale = new idCVar("r_lightScale", "2", CVAR_RENDERER | CVAR_FLOAT, "all light intensities are multiplied by this");
        r_lightSourceRadius = new idCVar("r_lightSourceRadius", "0", CVAR_RENDERER | CVAR_FLOAT, "for soft-shadow sampling");
        r_flareSize = new idCVar("r_flareSize", "1", CVAR_RENDERER | CVAR_FLOAT, "scale the flare deforms from the material def");

        r_useExternalShadows = new idCVar("r_useExternalShadows", "1", CVAR_RENDERER | CVAR_INTEGER, "1 = skip drawing caps when outside the light volume, 2 = force to no caps for testing", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        r_useOptimizedShadows = new idCVar("r_useOptimizedShadows", "1", CVAR_RENDERER | CVAR_BOOL, "use the dmap generated static shadow volumes");
        r_useScissor = new idCVar("r_useScissor", "1", CVAR_RENDERER | CVAR_BOOL, "scissor clip as portals and lights are processed");
        r_useCombinerDisplayLists = new idCVar("r_useCombinerDisplayLists", "1", CVAR_RENDERER | CVAR_BOOL | CVAR_NOCHEAT, "put all nvidia register combiner programming in display lists");
        r_useDepthBoundsTest = new idCVar("r_useDepthBoundsTest", "1", CVAR_RENDERER | CVAR_BOOL, "use depth bounds test to reduce shadow fill");

        r_screenFraction = new idCVar("r_screenFraction", "100", CVAR_RENDERER | CVAR_INTEGER, "for testing fill rate, the resolution of the entire screen can be changed");
        r_demonstrateBug = new idCVar("r_demonstrateBug", "0", CVAR_RENDERER | CVAR_BOOL, "used during development to show IHV's their problems");
        r_usePortals = new idCVar("r_usePortals", "1", CVAR_RENDERER | CVAR_BOOL, " 1 = use portals to perform area culling, otherwise draw everything");
        r_singleLight = new idCVar("r_singleLight", "-1", CVAR_RENDERER | CVAR_INTEGER, "suppress all but one light");
        r_singleEntity = new idCVar("r_singleEntity", "-1", CVAR_RENDERER | CVAR_INTEGER, "suppress all but one entity");
        r_singleSurface = new idCVar("r_singleSurface", "-1", CVAR_RENDERER | CVAR_INTEGER, "suppress all but one surface on each entity");
        r_singleArea = new idCVar("r_singleArea", "0", CVAR_RENDERER | CVAR_BOOL, "only draw the portal area the view is actually in");
        r_forceLoadImages = new idCVar("r_forceLoadImages", "0", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "draw all images to screen after registration");
        r_orderIndexes = new idCVar("r_orderIndexes", "1", CVAR_RENDERER | CVAR_BOOL, "perform index reorganization to optimize vertex use");
        r_lightAllBackFaces = new idCVar("r_lightAllBackFaces", "0", CVAR_RENDERER | CVAR_BOOL, "light all the back faces, even when they would be shadowed");

        // visual debugging info
        r_showPortals = new idCVar("r_showPortals", "0", CVAR_RENDERER | CVAR_BOOL, "draw portal outlines in color based on passed / not passed");
        r_showUnsmoothedTangents = new idCVar("r_showUnsmoothedTangents", "0", CVAR_RENDERER | CVAR_BOOL, "if 1, put all nvidia register combiner programming in display lists");
        r_showSilhouette = new idCVar("r_showSilhouette", "0", CVAR_RENDERER | CVAR_BOOL, "highlight edges that are casting shadow planes");
        r_showVertexColor = new idCVar("r_showVertexColor", "0", CVAR_RENDERER | CVAR_BOOL, "draws all triangles with the solid vertex color");
        r_showUpdates = new idCVar("r_showUpdates", "0", CVAR_RENDERER | CVAR_BOOL, "report entity and light updates and ref counts");
        r_showDemo = new idCVar("r_showDemo", "0", CVAR_RENDERER | CVAR_BOOL, "report reads and writes to the demo file");
        r_showDynamic = new idCVar("r_showDynamic", "0", CVAR_RENDERER | CVAR_BOOL, "report stats on dynamic surface generation");
        r_showLightScale = new idCVar("r_showLightScale", "0", CVAR_RENDERER | CVAR_BOOL, "report the scale factor applied to drawing for overbrights");
        r_showDefs = new idCVar("r_showDefs", "0", CVAR_RENDERER | CVAR_BOOL, "report the number of modeDefs and lightDefs in view");
        r_showTrace = new idCVar("r_showTrace", "0", CVAR_RENDERER | CVAR_INTEGER, "show the intersection of an eye trace with the world", new idCmdSystem.ArgCompletion_Integer(0, 2));
        r_showIntensity = new idCVar("r_showIntensity", "0", CVAR_RENDERER | CVAR_BOOL, "draw the screen colors based on intensity, red = 0, green = 128, blue = 255");
        r_showImages = new idCVar("r_showImages", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = show all images instead of rendering, 2 = show in proportional size", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        r_showSmp = new idCVar("r_showSmp", "0", CVAR_RENDERER | CVAR_BOOL, "show which end (front or back) is blocking");
        r_showLights = new idCVar("r_showLights", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = just print volumes numbers, highlighting ones covering the view, 2 = also draw planes of each volume, 3 = also draw edges of each volume", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
        r_showShadows = new idCVar("r_showShadows", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = visualize the stencil shadow volumes, 2 = draw filled in", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
        r_showShadowCount = new idCVar("r_showShadowCount", "0", CVAR_RENDERER | CVAR_INTEGER, "colors screen based on shadow volume depth complexity, >= 2 = print overdraw count based on stencil index values, 3 = only show turboshadows, 4 = only show static shadows", 0, 4, new idCmdSystem.ArgCompletion_Integer(0, 4));
        r_showLightScissors = new idCVar("r_showLightScissors", "0", CVAR_RENDERER | CVAR_BOOL, "show light scissor rectangles");
        r_showEntityScissors = new idCVar("r_showEntityScissors", "0", CVAR_RENDERER | CVAR_BOOL, "show entity scissor rectangles");
        r_showInteractionFrustums = new idCVar("r_showInteractionFrustums", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = show a frustum for each interaction, 2 = also draw lines to light origin, 3 = also draw entity bbox", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
        r_showInteractionScissors = new idCVar("r_showInteractionScissors", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = show screen rectangle which contains the interaction frustum, 2 = also draw construction lines", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        r_showLightCount = new idCVar("r_showLightCount", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = colors surfaces based on light count, 2 = also count everything through walls, 3 = also print overdraw", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
        r_showViewEntitys = new idCVar("r_showViewEntitys", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = displays the bounding boxes of all view models, 2 = print index numbers");
        r_showTris = new idCVar("r_showTris", "0", CVAR_RENDERER | CVAR_INTEGER, "enables wireframe rendering of the world, 1 = only draw visible ones, 2 = draw all front facing, 3 = draw all", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
        r_showSurfaceInfo = new idCVar("r_showSurfaceInfo", "0", CVAR_RENDERER | CVAR_BOOL, "show surface material name under crosshair");
        r_showNormals = new idCVar("r_showNormals", "0", CVAR_RENDERER | CVAR_FLOAT, "draws wireframe normals");
        r_showMemory = new idCVar("r_showMemory", "0", CVAR_RENDERER | CVAR_BOOL, "print frame memory utilization");
        r_showCull = new idCVar("r_showCull", "0", CVAR_RENDERER | CVAR_BOOL, "report sphere and box culling stats");
        r_showInteractions = new idCVar("r_showInteractions", "0", CVAR_RENDERER | CVAR_BOOL, "report interaction generation activity");
        r_showDepth = new idCVar("r_showDepth", "0", CVAR_RENDERER | CVAR_BOOL, "display the contents of the depth buffer and the depth range");
        r_showSurfaces = new idCVar("r_showSurfaces", "0", CVAR_RENDERER | CVAR_BOOL, "report surface/light/shadow counts");
        r_showPrimitives = new idCVar("r_showPrimitives", "0", CVAR_RENDERER | CVAR_INTEGER, "report drawsurf/index/vertex counts");
        r_showEdges = new idCVar("r_showEdges", "0", CVAR_RENDERER | CVAR_BOOL, "draw the sil edges");
        r_showTexturePolarity = new idCVar("r_showTexturePolarity", "0", CVAR_RENDERER | CVAR_BOOL, "shade triangles by texture area polarity");
        r_showTangentSpace = new idCVar("r_showTangentSpace", "0", CVAR_RENDERER | CVAR_INTEGER, "shade triangles by tangent space, 1 = use 1st tangent vector, 2 = use 2nd tangent vector, 3 = use normal vector", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));
        r_showDominantTri = new idCVar("r_showDominantTri", "0", CVAR_RENDERER | CVAR_BOOL, "draw lines from vertexes to center of dominant triangles");
        r_showAlloc = new idCVar("r_showAlloc", "0", CVAR_RENDERER | CVAR_BOOL, "report alloc/free counts");
        r_showTextureVectors = new idCVar("r_showTextureVectors", "0", CVAR_RENDERER | CVAR_FLOAT, " if > 0 draw each triangles texture =new idCVar(tangent) vectors");
        r_showOverDraw = new idCVar("r_showOverDraw", "0", CVAR_RENDERER | CVAR_INTEGER, "1 = geometry overdraw, 2 = light interaction overdraw, 3 = geometry and light interaction overdraw", 0, 3, new idCmdSystem.ArgCompletion_Integer(0, 3));

        r_lockSurfaces = new idCVar("r_lockSurfaces", "0", CVAR_RENDERER | CVAR_BOOL, "allow moving the view point without changing the composition of the scene, including culling");
        r_useEntityCallbacks = new idCVar("r_useEntityCallbacks", "1", CVAR_RENDERER | CVAR_BOOL, "if 0, issue the callback immediately at update time, rather than defering");

        r_showSkel = new idCVar("r_showSkel", "0", CVAR_RENDERER | CVAR_INTEGER, "draw the skeleton when model animates, 1 = draw model with skeleton, 2 = draw skeleton only", 0, 2, new idCmdSystem.ArgCompletion_Integer(0, 2));
        r_jointNameScale = new idCVar("r_jointNameScale", "0.02", CVAR_RENDERER | CVAR_FLOAT, "size of joint names when r_showskel is set to 1");
        r_jointNameOffset = new idCVar("r_jointNameOffset", "0.5", CVAR_RENDERER | CVAR_FLOAT, "offset of joint names when r_showskel is set to 1");

        r_cgVertexProfile = new idCVar("r_cgVertexProfile", "best", CVAR_RENDERER | CVAR_ARCHIVE, "arbvp1, vp20, vp30");
        r_cgFragmentProfile = new idCVar("r_cgFragmentProfile", "best", CVAR_RENDERER | CVAR_ARCHIVE, "arbfp1, fp30");

        r_debugLineDepthTest = new idCVar("r_debugLineDepthTest", "0", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "perform depth test on debug lines");
        r_debugLineWidth = new idCVar("r_debugLineWidth", "1", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_BOOL, "width of debug lines");
        r_debugArrowStep = new idCVar("r_debugArrowStep", "120", CVAR_RENDERER | CVAR_ARCHIVE | CVAR_INTEGER, "step size of arrow cone line rotation in degrees", 0, 120);
        r_debugPolygonFilled = new idCVar("r_debugPolygonFilled", "1", CVAR_RENDERER | CVAR_BOOL, "draw a filled polygon");

        r_materialOverride = new idCVar("r_materialOverride", "", CVAR_RENDERER, "overrides all materials", new idCmdSystem.ArgCompletion_Decl(DECL_MATERIAL));

        r_debugRenderToTexture = new idCVar("r_debugRenderToTexture", "0", CVAR_RENDERER | CVAR_INTEGER, "");

    }

    /*
     ==================
     GL_CheckErrors
     ==================
     */
    public static void GL_CheckErrors() {
        int err;
        String s;
        int i;

        // check for up to 10 errors pending
        for (i = 0; i < 10; i++) {
            err = qglGetError();
            if (err == GL_NO_ERROR) {
                return;
            }
            switch (err) {
                case GL_INVALID_ENUM:
                    s = "GL_INVALID_ENUM";
                    break;
                case GL_INVALID_VALUE:
                    s = "GL_INVALID_VALUE";
                    break;
                case GL_INVALID_OPERATION:
                    s = "GL_INVALID_OPERATION";
                    break;
                case GL_STACK_OVERFLOW:
                    s = "GL_STACK_OVERFLOW";
                    break;
                case GL_STACK_UNDERFLOW:
                    s = "GL_STACK_UNDERFLOW";
                    break;
                case GL_OUT_OF_MEMORY:
                    s = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    final char[] ss = new char[64];
                    idStr.snPrintf(ss, 64, "%d", err);
                    s = ctos(ss);
                    break;
            }
            if (!r_ignoreGLErrors.GetBool()) {
                Common.common.Printf("GL_CheckErrors: %s\n", s);
            }
        }
    }

    /* 
     ================== 
     R_ScreenshotFilename

     Returns a filename with digits appended
     if we have saved a previous screenshot, don't scan
     from the beginning, because recording demo avis can involve
     thousands of shots
     ================== 
     */
    public static void R_ScreenshotFilename(int[] lastNumber, final String base, idStr fileName) {
        int a, b, c, d, e;

        final boolean restrict = cvarSystem.GetCVarBool("fs_restrict");
        cvarSystem.SetCVarBool("fs_restrict", false);

        lastNumber[0]++;
        if (lastNumber[0] > 99999) {
            lastNumber[0] = 99999;
        }
        for (; lastNumber[0] < 99999; lastNumber[0]++) {
            int frac = lastNumber[0];

            a = frac / 10000;
            frac -= a * 10000;
            b = frac / 1000;
            frac -= b * 1000;
            c = frac / 100;
            frac -= c * 100;
            d = frac / 10;
            frac -= d * 10;
            e = frac;

            fileName.oSet(String.format("%s%d%d%d%d%d.tga", base, a, b, c, d, e));
            if (lastNumber[0] == 99999) {
                break;
            }
            final int len = fileSystem.ReadFile(fileName.getData(), null, null);
            if (len <= 0) {
                break;
            }
            // check again...
        }
        cvarSystem.SetCVarBool("fs_restrict", restrict);
    }

    /*
     =================
     R_InitCvars
     =================
     */
    static void R_InitCvars() {
        // update latched cvars here
    }

    /*
     =================
     R_InitCommands
     =================
     */
    static void R_InitCommands() {
        cmdSystem.AddCommand("MakeMegaTexture", idMegaTexture.MakeMegaTexture_f.getInstance(), CMD_FL_RENDERER | CMD_FL_CHEAT, "processes giant images");
        cmdSystem.AddCommand("sizeUp", R_SizeUp_f.getInstance(), CMD_FL_RENDERER, "makes the rendered view larger");
        cmdSystem.AddCommand("sizeDown", R_SizeDown_f.getInstance(), CMD_FL_RENDERER, "makes the rendered view smaller");
        cmdSystem.AddCommand("reloadGuis", R_ReloadGuis_f.getInstance(), CMD_FL_RENDERER, "reloads guis");
        cmdSystem.AddCommand("listGuis", R_ListGuis_f.getInstance(), CMD_FL_RENDERER, "lists guis");
        cmdSystem.AddCommand("touchGui", R_TouchGui_f.getInstance(), CMD_FL_RENDERER, "touches a gui");
        cmdSystem.AddCommand("screenshot", R_ScreenShot_f.getInstance(), CMD_FL_RENDERER, "takes a screenshot");
        cmdSystem.AddCommand("envshot", R_EnvShot_f.getInstance(), CMD_FL_RENDERER, "takes an environment shot");
        cmdSystem.AddCommand("makeAmbientMap", R_MakeAmbientMap_f.getInstance(), CMD_FL_RENDERER | CMD_FL_CHEAT, "makes an ambient map");
        cmdSystem.AddCommand("benchmark", R_Benchmark_f.getInstance(), CMD_FL_RENDERER, "benchmark");
        cmdSystem.AddCommand("gfxInfo", GfxInfo_f.getInstance(), CMD_FL_RENDERER, "show graphics info");
        cmdSystem.AddCommand("modulateLights", R_ModulateLights_f.getInstance(), CMD_FL_RENDERER | CMD_FL_CHEAT, "modifies shader parms on all lights");
        cmdSystem.AddCommand("testImage", R_TestImage_f.getInstance(), CMD_FL_RENDERER | CMD_FL_CHEAT, "displays the given image centered on screen", idCmdSystem.ArgCompletion_ImageName.getInstance());
        cmdSystem.AddCommand("testVideo", R_TestVideo_f.getInstance(), CMD_FL_RENDERER | CMD_FL_CHEAT, "displays the given cinematic", idCmdSystem.ArgCompletion_VideoName.getInstance());
        cmdSystem.AddCommand("reportSurfaceAreas", R_ReportSurfaceAreas_f.getInstance(), CMD_FL_RENDERER, "lists all used materials sorted by surface area");
        cmdSystem.AddCommand("reportImageDuplication", R_ReportImageDuplication_f.getInstance(), CMD_FL_RENDERER, "checks all referenced images for duplications");
        cmdSystem.AddCommand("regenerateWorld", R_RegenerateWorld_f.getInstance(), CMD_FL_RENDERER, "regenerates all interactions");
        cmdSystem.AddCommand("showInteractionMemory", R_ShowInteractionMemory_f.getInstance(), CMD_FL_RENDERER, "shows memory used by interactions");
        cmdSystem.AddCommand("showTriSurfMemory", R_ShowTriSurfMemory_f.getInstance(), CMD_FL_RENDERER, "shows memory used by triangle surfaces");
        cmdSystem.AddCommand("vid_restart", R_VidRestart_f.getInstance(), CMD_FL_RENDERER, "restarts renderSystem");
        cmdSystem.AddCommand("listRenderEntityDefs", R_ListRenderEntityDefs_f.getInstance(), CMD_FL_RENDERER, "lists the entity defs");
        cmdSystem.AddCommand("listRenderLightDefs", R_ListRenderLightDefs_f.getInstance(), CMD_FL_RENDERER, "lists the light defs");
        cmdSystem.AddCommand("listModes", R_ListModes_f.getInstance(), CMD_FL_RENDERER, "lists all video modes");
        cmdSystem.AddCommand("reloadSurface", R_ReloadSurface_f.getInstance(), CMD_FL_RENDERER, "reloads the decl and images for selected surface");
    }

    /*
     =================
     R_InitMaterials
     =================
     */
    static void R_InitMaterials() {
        tr.defaultMaterial = declManager.FindMaterial("_default", false);
        if (NOT(tr.defaultMaterial)) {
            common.FatalError("_default material not found");
        }
        declManager.FindMaterial("_default", false);

        // needed by R_DeriveLightData
        declManager.FindMaterial("lights/defaultPointLight");
        declManager.FindMaterial("lights/defaultProjectedLight");
    }

    /*
     ====================
     R_GetModeInfo

     r_mode is normally a small non-negative integer that
     looks resolutions up in a table, but if it is set to -1,
     the values from r_customWidth, amd r_customHeight
     will be used instead.
     ====================
     */
    static class vidmode_s {

        String description;
        int width, height;

        public vidmode_s(String description, int width, int height) {
            this.description = description;
            this.width = width;
            this.height = height;
        }

    }

    static final vidmode_s[] r_vidModes = {
        new vidmode_s("Mode  0: 320x240", 320, 240),
        new vidmode_s("Mode  1: 400x300", 400, 300),
        new vidmode_s("Mode  2: 512x384", 512, 384),
        new vidmode_s("Mode  3: 640x480", 640, 480),
        new vidmode_s("Mode  4: 800x600", 800, 600),
        new vidmode_s("Mode  5: 1024x768", 1024, 768),
        new vidmode_s("Mode  6: 1152x864", 1152, 864),
        new vidmode_s("Mode  7: 1280x1024", 1280, 1024),
        new vidmode_s("Mode  8: 1600x1200", 1600, 1200)
    };
    static int s_numVidModes = r_vidModes.length;

//#if MACOS_X
//bool R_GetModeInfo( int *width, int *height, int mode ) {
//#else
    static boolean R_GetModeInfo(int[] width, int[] height, int mode) {
//#endif
        vidmode_s vm;

        if (mode < -1) {
            return false;
        }
        if (mode >= s_numVidModes) {
            return false;
        }

        if (mode == -1) {
            width[0] = r_customWidth.GetInteger();
            height[0] = r_customHeight.GetInteger();
            return true;
        }

        vm = r_vidModes[mode];

        if (width != null) {
            width[0] = vm.width;
        }
        if (height != null) {
            height[0] = vm.height;
        }

        return true;
    }

    /*
     ==================
     R_CheckPortableExtensions

     ==================
     */
    public static void R_CheckPortableExtensions() {
//        throw new TempDump.TODO_Exception();
        glConfig.glVersion = atof(glConfig.version_string.replaceAll("(\\d+).(\\d+).(\\d+)", "$1.$2$3"));// converts openGL version from 1.1.x to 1.1x, which we can parse to float.
//
        // GL_ARB_multitexture
        glConfig.multitextureAvailable = R_CheckExtension("GL_ARB_multitexture");
        if (glConfig.multitextureAvailable) {
            glConfig.maxTextureUnits = qglGetInteger(GL_MAX_TEXTURE_UNITS_ARB);
            if (glConfig.maxTextureUnits > MAX_MULTITEXTURE_UNITS) {
                glConfig.maxTextureUnits = MAX_MULTITEXTURE_UNITS;
            }
            if (glConfig.maxTextureUnits < 2) {
                glConfig.multitextureAvailable = false;	// shouldn't ever happen
            }
            glConfig.maxTextureCoords = qglGetInteger(GL_MAX_TEXTURE_COORDS_ARB);
            glConfig.maxTextureImageUnits = qglGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS_ARB);
        }
//
        // GL_ARB_texture_env_combine
        glConfig.textureEnvCombineAvailable = R_CheckExtension("GL_ARB_texture_env_combine");

        // GL_ARB_texture_cube_map
        glConfig.cubeMapAvailable = R_CheckExtension("GL_ARB_texture_cube_map");

        // GL_ARB_texture_env_dot3
        glConfig.envDot3Available = R_CheckExtension("GL_ARB_texture_env_dot3");

        // GL_ARB_texture_env_add
        glConfig.textureEnvAddAvailable = R_CheckExtension("GL_ARB_texture_env_add");

        // GL_ARB_texture_non_power_of_two
        glConfig.textureNonPowerOfTwoAvailable = R_CheckExtension("GL_ARB_texture_non_power_of_two");
//
        // GL_ARB_texture_compression + GL_S3_s3tc
        // DRI drivers may have GL_ARB_texture_compression but no GL_EXT_texture_compression_s3tc
        if (R_CheckExtension("GL_ARB_texture_compression") && R_CheckExtension("GL_EXT_texture_compression_s3tc")) {
            glConfig.textureCompressionAvailable = true;
        } else {
            glConfig.textureCompressionAvailable = false;
        }
//
        // GL_EXT_texture_filter_anisotropic
        glConfig.anisotropicAvailable = R_CheckExtension("GL_EXT_texture_filter_anisotropic");
        if (glConfig.anisotropicAvailable) {
            final FloatBuffer maxTextureAnisotropy = BufferUtils.createFloatBuffer(16);
            qglGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, maxTextureAnisotropy);
            common.Printf("   maxTextureAnisotropy: %f\n", (glConfig.maxTextureAnisotropy = maxTextureAnisotropy.get()));
        } else {
            glConfig.maxTextureAnisotropy = 1;
        }
//
        // GL_EXT_texture_lod_bias
        // The actual extension is broken as specificed, storing the state in the texture unit instead
        // of the texture object.  The behavior in GL 1.4 is the behavior we use.
        if ((glConfig.glVersion >= 1.4) || R_CheckExtension("GL_EXT_texture_lod")) {
            common.Printf("...using %s\n", "GL_1.4_texture_lod_bias");
            glConfig.textureLODBiasAvailable = true;
        } else {
            common.Printf("X..%s not found\n", "GL_1.4_texture_lod_bias");
            glConfig.textureLODBiasAvailable = false;
        }
//
        // GL_EXT_shared_texture_palette
        glConfig.sharedTexturePaletteAvailable = R_CheckExtension("GL_EXT_shared_texture_palette");
//
        // GL_EXT_texture3D (not currently used for anything)
        glConfig.texture3DAvailable = R_CheckExtension("GL_EXT_texture3D");
//
        // EXT_stencil_wrap
        // This isn't very important, but some pathological case might cause a clamp error and give a shadow bug.
        // Nvidia also believes that future hardware may be able to run faster with this enabled to avoid the
        // serialization of clamping.
        if (R_CheckExtension("GL_EXT_stencil_wrap")) {
            tr.stencilIncr = GL_INCR_WRAP_EXT;
            tr.stencilDecr = GL_DECR_WRAP_EXT;
        } else {
            tr.stencilIncr = GL_INCR;
            tr.stencilDecr = GL_DECR;
        }
//
        // GL_NV_register_combiners
        glConfig.registerCombinersAvailable = R_CheckExtension("GL_NV_register_combiners");
//
        // GL_EXT_stencil_two_side
        glConfig.twoSidedStencilAvailable = R_CheckExtension("GL_EXT_stencil_two_side");
        if (glConfig.twoSidedStencilAvailable) {
        } else {
            glConfig.atiTwoSidedStencilAvailable = R_CheckExtension("GL_ATI_separate_stencil");
        }
//
        // GL_ATI_fragment_shader
        glConfig.atiFragmentShaderAvailable = R_CheckExtension("GL_ATI_fragment_shader");
        if (!glConfig.atiFragmentShaderAvailable) {
            // only on OSX: ATI_fragment_shader is faked through ATI_text_fragment_shader (macosx_glimp.cpp)
            glConfig.atiFragmentShaderAvailable = R_CheckExtension("GL_ATI_text_fragment_shader");
        }
//
        // ARB_vertex_buffer_object
        glConfig.ARBVertexBufferObjectAvailable = R_CheckExtension("GL_ARB_vertex_buffer_object");
//
        // ARB_vertex_program
        glConfig.ARBVertexProgramAvailable = R_CheckExtension("GL_ARB_vertex_program");
//
        // ARB_fragment_program
        if (r_inhibitFragmentProgram.GetBool()) {
            glConfig.ARBFragmentProgramAvailable = false;
        } else {
            glConfig.ARBFragmentProgramAvailable = R_CheckExtension("GL_ARB_fragment_program");
        }
//
        // GL_EXT_depth_bounds_test
        glConfig.depthBoundsTestAvailable = R_CheckExtension("EXT_depth_bounds_test");
    }

    /*
     =================
     R_SizeUp_f

     Keybinding command
     =================
     */
    static class R_SizeUp_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_SizeUp_f();

        private R_SizeUp_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if ((RenderSystem_init.r_screenFraction.GetInteger() + 10) > 100) {
                RenderSystem_init.r_screenFraction.SetInteger(100);
            } else {
                RenderSystem_init.r_screenFraction.SetInteger(RenderSystem_init.r_screenFraction.GetInteger() + 10);
            }
        }
    }


    /*
     =================
     R_SizeDown_f

     Keybinding command
     =================
     */
    static class R_SizeDown_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_SizeDown_f();

        private R_SizeDown_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if ((RenderSystem_init.r_screenFraction.GetInteger() - 10) < 10) {
                RenderSystem_init.r_screenFraction.SetInteger(10);
            } else {
                RenderSystem_init.r_screenFraction.SetInteger(RenderSystem_init.r_screenFraction.GetInteger() - 10);
            }
        }
    }


    /*
     ===============
     TouchGui_f

     this is called from the main thread
     ===============
     */
    static class R_TouchGui_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_TouchGui_f();

        private R_TouchGui_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            final String gui = args.Argv(1);

            if (!isNotNullOrEmpty(gui)) {
                common.Printf("USAGE: touchGui <guiName>\n");
                return;
            }

            common.Printf("touchGui %s\n", gui);
            session.UpdateScreen();
            uiManager.Touch(gui);
        }
    }

    /*
     ================== 
     R_BlendedScreenShot

     screenshot
     screenshot [filename]
     screenshot [width] [height]
     screenshot [width] [height] [samples]
     ================== 
     */
    static final int MAX_BLENDS = 256;	// to keep the accumulation in shorts

    static class R_ScreenShot_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ScreenShot_f();
        private static int[] lastNumber = {0};

        private R_ScreenShot_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            final idStr checkname = new idStr();

            int width = glConfig.vidWidth;
            int height = glConfig.vidHeight;
            final int x = 0;
            final int y = 0;
            int blends = 0;

            switch (args.Argc()) {
                case 1:
                    width = glConfig.vidWidth;
                    height = glConfig.vidHeight;
                    blends = 1;
                    R_ScreenshotFilename(lastNumber, "screenshots/shot", checkname);
                    break;
                case 2:
                    width = glConfig.vidWidth;
                    height = glConfig.vidHeight;
                    blends = 1;
                    checkname.oSet(args.Argv(1));
                    break;
                case 3:
                    width = Integer.parseInt(args.Argv(1));
                    height = Integer.parseInt(args.Argv(2));
                    blends = 1;
                    R_ScreenshotFilename(lastNumber, "screenshots/shot", checkname);
                    break;
                case 4:
                    width = Integer.parseInt(args.Argv(1));
                    height = Integer.parseInt(args.Argv(2));
                    blends = Integer.parseInt(args.Argv(3));
                    if (blends < 1) {
                        blends = 1;
                    }
                    if (blends > MAX_BLENDS) {
                        blends = MAX_BLENDS;
                    }
                    R_ScreenshotFilename(lastNumber, "screenshots/shot", checkname);
                    break;
                default:
                    common.Printf("usage: screenshot\n       screenshot <filename>\n       screenshot <width> <height>\n       screenshot <width> <height> <blends>\n");
                    return;
            }

            // put the console away
            console.Close();

            tr.TakeScreenshot(width, height, checkname.getData(), blends, null);

            common.Printf("Wrote %s\n", checkname);
        }
    }

    /* 
     ================== 
     R_EnvShot_f

     envshot <basename>

     Saves out env/<basename>_ft.tga, etc
     ================== 
     */
    static class R_EnvShot_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_EnvShot_f();

        private R_EnvShot_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String fullname = null;
            String baseName;
            int i;
            final idMat3[] axis = new idMat3[6];
            renderView_s ref;
            viewDef_s primary;
            int blends;
            final String[] extensions/*[6]*/ = {"_px.tga", "_nx.tga", "_py.tga", "_ny.tga", "_pz.tga", "_nz.tga"};
            int size;

            if ((args.Argc() != 2) && (args.Argc() != 3) && (args.Argc() != 4)) {
                common.Printf("USAGE: envshot <basename> [size] [blends]\n");
                return;
            }
            baseName = args.Argv(1);

            blends = 1;
            if (args.Argc() == 4) {
                size = Integer.parseInt(args.Argv(2));
                blends = Integer.parseInt(args.Argv(3));
            } else if (args.Argc() == 3) {
                size = Integer.parseInt(args.Argv(2));
                blends = 1;
            } else {
                size = 256;
                blends = 1;
            }

            if (NOT(tr.primaryView)) {
                common.Printf("No primary view.\n");
                return;
            }

            primary = new viewDef_s(tr.primaryView);

//	memset( &axis, 0, sizeof( axis ) );
            axis[0].oSet(0, 0, 1);
            axis[0].oSet(1, 2, 1);
            axis[0].oSet(2, 1, 1);

            axis[1].oSet(0, 0, -1);
            axis[1].oSet(1, 2, -1);
            axis[1].oSet(2, 1, 1);

            axis[2].oSet(0, 1, 1);
            axis[2].oSet(1, 0, -1);
            axis[2].oSet(2, 2, -1);

            axis[3].oSet(0, 1, -1);
            axis[3].oSet(1, 0, -1);
            axis[3].oSet(2, 2, 1);

            axis[4].oSet(0, 2, 1);
            axis[4].oSet(1, 0, -1);
            axis[4].oSet(2, 1, 1);

            axis[5].oSet(0, 2, -1);
            axis[5].oSet(1, 0, 1);
            axis[5].oSet(2, 1, 1);

            for (i = 0; i < 6; i++) {
                ref = new renderView_s(primary.renderView);
                ref.x = ref.y = 0;
                ref.fov_x = ref.fov_y = 90;
                ref.width = glConfig.vidWidth;
                ref.height = glConfig.vidHeight;
                ref.viewaxis = new idMat3(axis[i]);
                fullname = String.format("env/%s%s", baseName, extensions[i]);
                tr.TakeScreenshot(size, size, fullname, blends, ref);
            }

            common.Printf("Wrote %s, etc\n", fullname);
        }
    }

    //============================================================================
    static final idMat3[] cubeAxis = new idMat3[6];


    /*
     ==================
     R_SampleCubeMap
     ==================
     */
    private static void R_SampleCubeMap(final idVec3 dir, int size, ByteBuffer[] buffers/*[6]*/, byte[] result/*[4]*/) {
        final float[] adir = new float[3];
        int axis, x, y;

        adir[0] = Math.abs(dir.oGet(0));
        adir[1] = Math.abs(dir.oGet(1));
        adir[2] = Math.abs(dir.oGet(2));

        if ((dir.oGet(0) >= adir[1]) && (dir.oGet(0) >= adir[2])) {
            axis = 0;
        } else if ((-dir.oGet(0) >= adir[1]) && (-dir.oGet(0) >= adir[2])) {
            axis = 1;
        } else if ((dir.oGet(1) >= adir[0]) && (dir.oGet(1) >= adir[2])) {
            axis = 2;
        } else if ((-dir.oGet(1) >= adir[0]) && (-dir.oGet(1) >= adir[2])) {
            axis = 3;
        } else if ((dir.oGet(2) >= adir[1]) && (dir.oGet(2) >= adir[2])) {
            axis = 4;
        } else {
            axis = 5;
        }

        float fx = (dir.oMultiply(cubeAxis[axis].oGet(1))) / (dir.oMultiply(cubeAxis[axis].oGet(0)));
        float fy = (dir.oMultiply(cubeAxis[axis].oGet(2))) / (dir.oMultiply(cubeAxis[axis].oGet(0)));

        fx = -fx;
        fy = -fy;
        x = (int) (size * 0.5 * (fx + 1));
        y = (int) (size * 0.5 * (fy + 1));
        if (x < 0) {
            x = 0;
        } else if (x >= size) {
            x = size - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= size) {
            y = size - 1;
        }

        result[0] = buffers[axis].get((((y * size) + x) * 4) + 0);
        result[1] = buffers[axis].get((((y * size) + x) * 4) + 1);
        result[2] = buffers[axis].get((((y * size) + x) * 4) + 2);
        result[3] = buffers[axis].get((((y * size) + x) * 4) + 3);
    }


    /* 
     ================== 
     R_MakeAmbientMap_f

     R_MakeAmbientMap_f <basename> [size]

     Saves out env/<basename>_amb_ft.tga, etc
     ================== 
     */
    static class R_MakeAmbientMap_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_MakeAmbientMap_f();
        private static final idMat3[] cubeAxis = new idMat3[6];

        private R_MakeAmbientMap_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            String fullname;
            String baseName;
            int i;
            final renderView_s ref;
            final viewDef_s primary;
            int downSample;
            final String[] extensions/*[6]*/ = {"_px.tga", "_nx.tga", "_py.tga", "_ny.tga",
                        "_pz.tga", "_nz.tga"};
            int outSize;
            final ByteBuffer[] buffers = new ByteBuffer[6];
            final int[] width = {0}, height = {0};

            if ((args.Argc() != 2) && (args.Argc() != 3)) {
                common.Printf("USAGE: ambientshot <basename> [size]\n");
                return;
            }
            baseName = args.Argv(1);

            downSample = 0;
            if (args.Argc() == 3) {
                outSize = Integer.parseInt(args.Argv(2));
            } else {
                outSize = 32;
            }

//	memset( &cubeAxis, 0, sizeof( cubeAxis ) );
            cubeAxis[0].oSet(0, 0, 1);
            cubeAxis[0].oSet(1, 2, 1);
            cubeAxis[0].oSet(2, 1, 1);

            cubeAxis[1].oSet(0, 0, -1);
            cubeAxis[1].oSet(1, 2, -1);
            cubeAxis[1].oSet(2, 1, 1);

            cubeAxis[2].oSet(0, 1, 1);
            cubeAxis[2].oSet(1, 0, -1);
            cubeAxis[2].oSet(2, 2, -1);

            cubeAxis[3].oSet(0, 1, -1);
            cubeAxis[3].oSet(1, 0, -1);
            cubeAxis[3].oSet(2, 2, 1);

            cubeAxis[4].oSet(0, 2, 1);
            cubeAxis[4].oSet(1, 0, -1);
            cubeAxis[4].oSet(2, 1, 1);

            cubeAxis[5].oSet(0, 2, -1);
            cubeAxis[5].oSet(1, 0, 1);
            cubeAxis[5].oSet(2, 1, 1);

            // read all of the images
            for (i = 0; i < 6; i++) {
                fullname = String.format("env/%s%s", baseName, extensions[i]);
                common.Printf("loading %s\n", fullname);
                session.UpdateScreen();
                buffers[i] = R_LoadImage(fullname, width, height, null, true);
                if (NOT(buffers[i])) {
                    common.Printf("failed.\n");
                    for (i--; i >= 0; i--) {
                        buffers[i] = null;
                    }
                    return;
                }
            }

            // resample with hemispherical blending
            final int samples = 1000;

            final ByteBuffer outBuffer = ByteBuffer.allocate(outSize * outSize * 4);

            for (int map = 0; map < 2; map++) {
                for (i = 0; i < 6; i++) {
                    for (int x = 0; x < outSize; x++) {
                        for (int y = 0; y < outSize; y++) {
                            idVec3 dir;
                            final float[] total = new float[3];

                            dir = cubeAxis[i].oGet(0).oPlus(
                                    cubeAxis[i].oGet(1).oMultiply(-(-1 + ((2.0f * x) / (outSize - 1))))).oPlus(
                                            cubeAxis[i].oGet(2).oMultiply(-(-1 + ((2.0f * y) / (outSize - 1)))));
                            dir.Normalize();
                            total[0] = total[1] = total[2] = 0;
                            //samples = 1;
                            final float limit = itob(map) ? 0.95f : 0.25f;		// small for specular, almost hemisphere for ambient

                            for (int s = 0; s < samples; s++) {
                                // pick a random direction vector that is inside the unit sphere but not behind dir,
                                // which is a robust way to evenly sample a hemisphere
                                final idVec3 test = new idVec3();
                                while (true) {
                                    for (int j = 0; j < 3; j++) {
                                        test.oSet(j, -1 + ((2 * (((int) random()) & 0x7fff)) / (float) 0x7fff));
                                    }
                                    if (test.Length() > 1.0) {
                                        continue;
                                    }
                                    test.Normalize();
                                    if (test.oMultiply(dir) > limit) {	// don't do a complete hemisphere
                                        break;
                                    }
                                }
                                final byte[] result = new byte[4];
                                //test = dir;
                                R_SampleCubeMap(test, width[0], buffers, result);
                                total[0] += result[0];
                                total[1] += result[1];
                                total[2] += result[2];
                            }
                            outBuffer.put((((y * outSize) + x) * 4) + 0, (byte) (total[0] / samples));
                            outBuffer.put((((y * outSize) + x) * 4) + 1, (byte) (total[1] / samples));
                            outBuffer.put((((y * outSize) + x) * 4) + 2, (byte) (total[2] / samples));
                            outBuffer.put((((y * outSize) + x) * 4) + 3, (byte) 255);
                        }
                    }

                    if (map == 0) {
                        fullname = String.format("env/%s_amb%s", baseName, extensions[i]);
                    } else {
                        fullname = String.format("env/%s_spec%s", baseName, extensions[i]);
                    }
                    common.Printf("writing %s\n", fullname);
                    session.UpdateScreen();
                    R_WriteTGA(fullname, outBuffer, outSize, outSize);
                }
            }

//            for (i = 0; i < 6; i++) {
//                if (buffers[i]) {
//                    Mem_Free(buffers[i]);
//                }
//            }
        }
    }

    /* 
     ============================================================================== 
 
     THROUGHPUT BENCHMARKING
 
     ============================================================================== 
     */
    private static final int SAMPLE_MSEC = 1000;

    /*
     ================
     R_RenderingFPS
     ================
     */
    static float R_RenderingFPS(final renderView_s renderView) {
        qglFinish();

        final int start = Sys_Milliseconds();
        int end;
        int count = 0;

        while (true) {
            // render
            renderSystem.BeginFrame(glConfig.vidWidth, glConfig.vidHeight);
            tr.primaryWorld.RenderScene(renderView);
            renderSystem.EndFrame(null, null);
            qglFinish();
            count++;
            end = Sys_Milliseconds();
            if ((end - start) > SAMPLE_MSEC) {
                break;
            }
        }

        final float fps = (float) ((count * 1000.0) / (end - start));

        return fps;
    }

    /*
     ================
     R_Benchmark_f
     ================
     */
    static class R_Benchmark_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_Benchmark_f();

        private R_Benchmark_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            float fps, msec;
            renderView_s view;

            if (NOT(tr.primaryView)) {
                common.Printf("No primaryView for benchmarking\n");
                return;
            }
            view = tr.primaryRenderView;

            for (int size = 100; size >= 10; size -= 10) {
                RenderSystem_init.r_screenFraction.SetInteger(size);
                fps = R_RenderingFPS(view);
                final int kpix = (int) (glConfig.vidWidth * glConfig.vidHeight * (size * 0.01) * (size * 0.01) * 0.001);
                msec = (1000.0f / fps);
                common.Printf("kpix: %4d  msec:%5.1f fps:%5.1f\n", kpix, msec, fps);
            }

            // enable r_singleTriangle 1 while r_screenFraction is still at 10
            RenderSystem_init.r_singleTriangle.SetBool(true);
            fps = R_RenderingFPS(view);
            msec = 1000.0f / fps;
            common.Printf("single tri  msec:%5.1f fps:%5.1f\n", msec, fps);
            RenderSystem_init.r_singleTriangle.SetBool(false);
            RenderSystem_init.r_screenFraction.SetInteger(100);

            // enable r_skipRenderContext 1
            RenderSystem_init.r_skipRenderContext.SetBool(true);
            fps = R_RenderingFPS(view);
            msec = 1000.0f / fps;
            common.Printf("no context  msec:%5.1f fps:%5.1f\n", msec, fps);
            RenderSystem_init.r_skipRenderContext.SetBool(false);
        }
    }

    /*
     ================
     GfxInfo_f
     ================
     */
    static class GfxInfo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new GfxInfo_f();
        private static final String[] fsstrings
                = {
                    "windowed",
                    "fullscreen"
                };

        private GfxInfo_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {

            common.Printf("\nGL_VENDOR: %s\n", glConfig.vendor_string);
            common.Printf("GL_RENDERER: %s\n", glConfig.renderer_string);
            common.Printf("GL_VERSION: %s\n", glConfig.version_string);
            common.Printf("GL_EXTENSIONS: %s\n", glConfig.extensions_string);
            if (glConfig.wgl_extensions_string != null) {
                common.Printf("WGL_EXTENSIONS: %s\n", glConfig.wgl_extensions_string);
            }
            common.Printf("GL_MAX_TEXTURE_SIZE: %d\n", glConfig.maxTextureSize);
            common.Printf("GL_MAX_TEXTURE_UNITS_ARB: %d\n", glConfig.maxTextureUnits);
            common.Printf("GL_MAX_TEXTURE_COORDS_ARB: %d\n", glConfig.maxTextureCoords);
            common.Printf("GL_MAX_TEXTURE_IMAGE_UNITS_ARB: %d\n", glConfig.maxTextureImageUnits);
            common.Printf("\nPIXELFORMAT: color(%d-bits) Z(%d-bit) stencil(%d-bits)\n", glConfig.colorBits, glConfig.depthBits, glConfig.stencilBits);
            common.Printf("MODE: %d, %d x %d %s hz:", RenderSystem_init.r_mode.GetInteger(), glConfig.vidWidth, glConfig.vidHeight, fsstrings[btoi(RenderSystem_init.r_fullscreen.GetBool())]);

            if (glConfig.displayFrequency != 0) {
                common.Printf("%d\n", glConfig.displayFrequency);
            } else {
                common.Printf("N/A\n");
            }
            common.Printf("CPU: %s\n", Sys_GetProcessorString());
            final String[] active/*[2]*/ = {"", " (ACTIVE)"};
            common.Printf("ARB path ENABLED%s\n", active[btoi(tr.backEndRenderer == BE_ARB)]);

            if (glConfig.allowNV10Path) {
                common.Printf("NV10 path ENABLED%s\n", active[btoi(tr.backEndRenderer == BE_NV10)]);
            } else {
                common.Printf("NV10 path disabled\n");
            }

            if (glConfig.allowNV20Path) {
                common.Printf("NV20 path ENABLED%s\n", active[btoi(tr.backEndRenderer == BE_NV20)]);
            } else {
                common.Printf("NV20 path disabled\n");
            }

            if (glConfig.allowR200Path) {
                common.Printf("R200 path ENABLED%s\n", active[btoi(tr.backEndRenderer == BE_R200)]);
            } else {
                common.Printf("R200 path disabled\n");
            }

            if (glConfig.allowARB2Path) {
                common.Printf("ARB2 path ENABLED%s\n", active[btoi(tr.backEndRenderer == BE_ARB2)]);
            } else {
                common.Printf("ARB2 path disabled\n");
            }

            //=============================
            common.Printf("-------\n");

            if (RenderSystem_init.r_finish.GetBool()) {
                common.Printf("Forcing glFinish\n");
            } else {
                common.Printf("glFinish not forced\n");
            }

            if (_WIN32) {
// WGL_EXT_swap_interval
//                typedef BOOL (WINAPI * PFNWGLSWAPINTERVALEXTPROC) (int interval);
//                extern PFNWGLSWAPINTERVALEXTPROC wglSwapIntervalEXT;

                if (RenderSystem_init.r_swapInterval.GetInteger() != 0) {//)  && NativeLibrary.isFunctionAvailableGlobal("wglSwapIntervalEXT")) {
                    common.Printf("Forcing swapInterval %d\n", RenderSystem_init.r_swapInterval.GetInteger());
                } else {
                    common.Printf("swapInterval not forced\n");
                }
            }

            final boolean tss = glConfig.twoSidedStencilAvailable || glConfig.atiTwoSidedStencilAvailable;

            if (!RenderSystem_init.r_useTwoSidedStencil.GetBool() && tss) {
                common.Printf("Two sided stencil available but disabled\n");
            } else if (!tss) {
                common.Printf("Two sided stencil not available\n");
            } else if (tss) {
                common.Printf("Using two sided stencil\n");
            }

            if (vertexCache.IsFast()) {
                common.Printf("Vertex cache is fast\n");
            } else {
                common.Printf("Vertex cache is SLOW\n");
            }
        }
    }

    /*
     =============
     R_TestImage_f

     Display the given image centered on the screen.
     testimage <number>
     testimage <filename>
     =============
     */
    static class R_TestImage_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_TestImage_f();

        private R_TestImage_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int imageNum;

            if (tr.testVideo != null) {
//		delete tr.testVideo;
                tr.testVideo = null;
            }
            tr.testImage = null;

            if (args.Argc() != 2) {
                return;
            }

            if (idStr.IsNumeric(args.Argv(1))) {
                imageNum = Integer.parseInt(args.Argv(1));
                if ((imageNum >= 0) && (imageNum < globalImages.images.Num())) {
                    tr.testImage = globalImages.images.oGet(imageNum);
                }
            } else {
                tr.testImage = globalImages.ImageFromFile(args.Argv(1), TF_DEFAULT, false, TR_REPEAT, TD_DEFAULT);
            }
        }
    }

    /*
     =============
     R_TestVideo_f

     Plays the cinematic file in a testImage
     =============
     */
    static class R_TestVideo_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_TestVideo_f();

        private R_TestVideo_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            if (tr.testVideo != null) {
//		delete tr.testVideo;
                tr.testVideo = null;
            }
            tr.testImage = null;

            if (args.Argc() < 2) {
                return;
            }

            tr.testImage = globalImages.ImageFromFile("_scratch", TF_DEFAULT, false, TR_REPEAT, TD_DEFAULT);
            tr.testVideo = idCinematic.Alloc();
            tr.testVideo.InitFromFile(args.Argv(1), true);

            cinData_t cin;
            cin = tr.testVideo.ImageForTime(0);
            if (NOT(cin.image)) {
//		delete tr.testVideo;
                tr.testVideo = null;
                tr.testImage = null;
                return;
            }

            common.Printf("%d x %d images\n", cin.imageWidth, cin.imageHeight);

            final int len = tr.testVideo.AnimationLength();
            common.Printf("%5.1f seconds of video\n", len * 0.001);

            tr.testVideoStartTime = (float) (tr.primaryRenderView.time * 0.001);

            // try to play the matching wav file
            final idStr wavString = new idStr(args.Argv((args.Argc() == 2) ? 1 : 2));
            wavString.StripFileExtension();
            wavString.oPluSet(".wav");
            session.sw.PlayShaderDirectly(wavString.getData());
        }
    }

    /*
     ===================
     R_ReportSurfaceAreas_f

     Prints a list of the materials sorted by surface area
     ===================
     */
    static class R_ReportSurfaceAreas_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ReportSurfaceAreas_f();

        private R_ReportSurfaceAreas_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i, count;
            idMaterial[] list;

            count = declManager.GetNumDecls(DECL_MATERIAL);
            list = new idMaterial[count];

            for (i = 0; i < count; i++) {
                list[i] = (idMaterial) declManager.DeclByIndex(DECL_MATERIAL, i, false);
            }

//            qsort(list, count, sizeof(list[0]), new R_QsortSurfaceAreas());
            Arrays.sort(list, new R_QsortSurfaceAreas());

            // skip over ones with 0 area
            for (i = 0; i < count; i++) {
                if (list[i].GetSurfaceArea() > 0) {
                    break;
                }
            }

            for (; i < count; i++) {
                // report size in "editor blocks"
                final int blocks = (int) (list[i].GetSurfaceArea() / 4096.0);
                common.Printf("%7i %s\n", blocks, list[i].GetName());
            }
        }
    }

    /*
     ===================
     R_ReportImageDuplication_f

     Checks for images with the same hash value and does a better comparison
     ===================
     */
    static class R_ReportImageDuplication_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ReportImageDuplication_f();

        private R_ReportImageDuplication_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i, j;

            common.Printf("Images with duplicated contents:\n");

            int count = 0;

            for (i = 0; i < globalImages.images.Num(); i++) {
                final idImage image1 = globalImages.images.oGet(i);

                if (image1.isPartialImage) {
                    // ignore background loading stubs
                    continue;
                }
                if (image1.generatorFunction != null) {
                    // ignore procedural images
                    continue;
                }
                if (image1.cubeFiles != CF_2D) {
                    // ignore cube maps
                    continue;
                }
                if (image1.defaulted) {
                    continue;
                }
                final int[] w1 = {0}, h1 = {0};

                final ByteBuffer data1 = R_LoadImageProgram(image1.imgName.getData(), w1, h1, null);

                for (j = 0; j < i; j++) {
                    final idImage image2 = globalImages.images.oGet(j);

                    if (image2.isPartialImage) {
                        continue;
                    }
                    if (image2.generatorFunction != null) {
                        continue;
                    }
                    if (image2.cubeFiles != CF_2D) {
                        continue;
                    }
                    if (image2.defaulted) {
                        continue;
                    }
                    if (!image1.imageHash.equals(image2.imageHash)) {
                        continue;
                    }
                    if ((image2.uploadWidth != image1.uploadWidth)
                            || (image2.uploadHeight != image1.uploadHeight)) {
                        continue;
                    }
                    if (NOT(idStr.Icmp(image1.imgName, image2.imgName))) {
                        // ignore same image-with-different-parms
                        continue;
                    }

                    final int[] w2 = {0}, h2 = {0};

                    final ByteBuffer data2 = R_LoadImageProgram(image2.imgName.getData(), w2, h2, null);

                    if ((w2 != w1) || (h2 != h1)) {
//                        R_StaticFree(data2);
                        continue;
                    }

//                    if (memcmp(data1, data2, w1 * h1 * 4)) {
                    if (data1.equals(data2)) {//TODO: check range?
//                        R_StaticFree(data2);
                        continue;
                    }

//                    R_StaticFree(data2);
                    common.Printf("%s == %s\n", image1.imgName, image2.imgName);
                    session.UpdateScreen(true);
                    count++;
                    break;
                }

//                R_StaticFree(data1);
            }
            common.Printf("%d / %d collisions\n", count, globalImages.images.Num());
        }
    }

    /*
     =================
     R_VidRestart_f
     =================
     */
    static class R_VidRestart_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_VidRestart_f();

        private R_VidRestart_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int err;

            // if OpenGL isn't started, do nothing
            if (!glConfig.isInitialized) {
                return;
            }

            boolean full = true;
            boolean forceWindow = false;
            for (int i = 1; i < args.Argc(); i++) {
                if (idStr.Icmp(args.Argv(i), "partial") == 0) {
                    full = false;
                    continue;
                }
                if (idStr.Icmp(args.Argv(i), "windowed") == 0) {
                    forceWindow = true;
                    continue;
                }
            }

            // this could take a while, so give them the cursor back ASAP
            Sys_GrabMouseCursor(false);

            // dump ambient caches
            renderModelManager.FreeModelVertexCaches();

            // free any current world interaction surfaces and vertex caches
            R_FreeDerivedData();

            // make sure the defered frees are actually freed
            R_ToggleSmpFrame();
            R_ToggleSmpFrame();

            // free the vertex caches so they will be regenerated again
            vertexCache.PurgeAll();

            // sound and input are tied to the window we are about to destroy
            if (full) {
                // free all of our texture numbers
                snd_system.soundSystem.ShutdownHW();
                Sys_ShutdownInput();
                globalImages.PurgeAllImages();
                // free the context and close the window
                GLimp_Shutdown();
                glConfig.isInitialized = false;

                // create the new context and vertex cache
                final boolean latch = cvarSystem.GetCVarBool("r_fullscreen");
                if (forceWindow) {
                    cvarSystem.SetCVarBool("r_fullscreen", false);
                }
                R_InitOpenGL();
                cvarSystem.SetCVarBool("r_fullscreen", latch);

                // regenerate all images
                globalImages.ReloadAllImages();
            } else {
                final glimpParms_t parms = new glimpParms_t();
                parms.width = glConfig.vidWidth;
                parms.height = glConfig.vidHeight;
                parms.fullScreen = (forceWindow) ? false : RenderSystem_init.r_fullscreen.GetBool();
                parms.displayHz = RenderSystem_init.r_displayRefresh.GetInteger();
                parms.multiSamples = RenderSystem_init.r_multiSamples.GetInteger();
                parms.stereo = false;
                GLimp_SetScreenParms(parms);
            }

            // make sure the regeneration doesn't use anything no longer valid
            tr.viewCount++;
//            System.out.println("tr.viewCount::R_VidRestart_f");
            tr.viewDef = null;

            // regenerate all necessary interactions
            R_RegenerateWorld_f.getInstance().run(new idCmdArgs());

            // check for problems
            err = qglGetError();
            if (err != GL_NO_ERROR) {
                common.Printf("glGetError() = 0x%x\n", err);
            }

            // start sound playing again
            snd_system.soundSystem.SetMute(false);
        }
    }

    /*
     ==============
     R_ListModes_f
     ==============
     */
    static class R_ListModes_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ListModes_f();

        private R_ListModes_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            int i;

            common.Printf("\n");
            for (i = 0; i < s_numVidModes; i++) {
                common.Printf("%s\n", r_vidModes[i].description);
            }
            common.Printf("\n");
        }
    }

    /*
     =====================
     R_ReloadSurface_f

     Reload the material displayed by r_showSurfaceInfo
     =====================
     */
    static class R_ReloadSurface_f extends cmdFunction_t {

        private static final cmdFunction_t instance = new R_ReloadSurface_f();

        private R_ReloadSurface_f() {
        }

        public static cmdFunction_t getInstance() {
            return instance;
        }

        @Override
        public void run(idCmdArgs args) {
            final modelTrace_s mt = new modelTrace_s();
            idVec3 start, end;

            // start far enough away that we don't hit the player model
            start = tr.primaryView.renderView.vieworg.oPlus(tr.primaryView.renderView.viewaxis.oGet(0).oMultiply(16));
            end = start.oPlus(tr.primaryView.renderView.viewaxis.oGet(0).oMultiply(1000.0f));
            if (!tr.primaryWorld.Trace(mt, start, end, 0.0f, false)) {
                return;
            }

            common.Printf("Reloading %s\n", mt.material.GetName());

            // reload the decl
            mt.material.base.Reload();

            // reload any images used by the decl
            mt.material.ReloadImages(false);
        }
    }

    static class R_QsortSurfaceAreas implements cmp_t<idMaterial> {

        @Override
        public int compare(final idMaterial a, final idMaterial b) {
            float ac, bc;

            if (!a.EverReferenced()) {
                ac = 0;
            } else {
                ac = a.GetSurfaceArea();
            }

            if (!b.EverReferenced()) {
                bc = 0;
            } else {
                bc = b.GetSurfaceArea();
            }

            if (ac < bc) {
                return -1;
            }
            if (ac > bc) {
                return 1;
            }

            return idStr.Icmp(a.GetName(), b.GetName());
        }
    }

    /*
     ==================
     R_InitOpenGL

     This function is responsible for initializing a valid OpenGL subsystem
     for rendering.  This is done by calling the system specific GLimp_Init,
     which gives us a working OGL subsystem, then setting all necessary openGL
     state, including images, vertex programs, and display lists.

     Changes to the vertex cache size or smp state require a vid_restart.

     If glConfig.isInitialized is false, no rendering can take place, but
     all renderSystem functions will still operate properly, notably the material
     and model information functions.
     ==================
     */
    private static boolean glCheck = false;

    public static void R_InitOpenGL() {
//	GLint			temp;
        final IntBuffer temp = BufferUtils.createIntBuffer(16);
        final glimpParms_t parms = new glimpParms_t();
        int i;

        common.Printf("----- R_InitOpenGL -----\n");

        if (glConfig.isInitialized) {
            common.FatalError("R_InitOpenGL called while active");
        }

        // in case we had an error while doing a tiled rendering
        tr.viewportOffset[0] = 0;
        tr.viewportOffset[1] = 0;

        //
        // initialize OS specific portions of the renderSystem
        //
        for (i = 0; i < 2; i++) {
            // set the parameters we are trying
            {
                final int[] vidWidth = {0}, vidHeight = {0};
                R_GetModeInfo(vidWidth, vidHeight, r_mode.GetInteger());
                glConfig.vidWidth = 1024;//vidWidth[0];HACKME::0
                glConfig.vidHeight = 768;//vidHeight[0];
            }

            parms.width = glConfig.vidWidth;
            parms.height = glConfig.vidHeight;
            parms.fullScreen = r_fullscreen.GetBool();
            parms.displayHz = r_displayRefresh.GetInteger();
            parms.multiSamples = r_multiSamples.GetInteger();
            parms.stereo = false;

             if (GLimp_Init(parms)) {
                // it's ALIVE!
                break;
            }

            if (i == 1) {
                common.FatalError("Unable to initialize OpenGL");
            }

            // if we failed, set everything back to "safe mode"
            // and try again
            r_mode.SetInteger(3);
            r_fullscreen.SetInteger(1);
            r_displayRefresh.SetInteger(0);
            r_multiSamples.SetInteger(0);
        }

        // input and sound systems need to be tied to the new window
        Sys_InitInput();
        snd_system.soundSystem.InitHW();

        // get our config strings
        glConfig.vendor_string = qglGetString(GL_VENDOR);
        glConfig.renderer_string = qglGetString(GL_RENDERER);
        glConfig.version_string = qglGetString(GL_VERSION);

        final StringBuilder bla = new StringBuilder();
        String ext;
        for (int j = 0; (ext = qglGetStringi(GL_EXTENSIONS, j)) != null; j++) {
            bla.append(ext).append(' ');
        }
        glConfig.extensions_string = bla.toString();

        // OpenGL driver constants
        qglGetIntegerv(GL_MAX_TEXTURE_SIZE, temp);
        glConfig.maxTextureSize = temp.get();

        // stubbed or broken drivers may have reported 0...
        if (glConfig.maxTextureSize <= 0) {
            glConfig.maxTextureSize = 256;
        }

        glConfig.isInitialized = true;

        // recheck all the extensions (FIXME: this might be dangerous)
        R_CheckPortableExtensions();

        // parse our vertex and fragment programs, possibly disable support for
        // one of the paths if there was an error
//        R_NV10_Init();
//        R_NV20_Init();
//        R_R200_Init();
        R_ARB2_Init();

        cmdSystem.AddCommand("reloadARBprograms", R_ReloadARBPrograms_f.getInstance(), CMD_FL_RENDERER, "reloads ARB programs");
        R_ReloadARBPrograms_f.getInstance().run(null);

        // allocate the vertex array range or vertex objects
        vertexCache.Init();

        // select which renderSystem we are going to use
        r_renderer.SetModified();
        tr.SetBackEndRenderer();

        // allocate the frame data, which may be more if smp is enabled
        R_InitFrameData();

        // Reset our gamma
        R_SetColorMappings();

        if (_WIN32) {
            if (!glCheck) {// && win32.osversion.dwMajorVersion == 6) {//TODO:should this be applicable?
                glCheck = true;
                if ((0 == idStr.Icmp(glConfig.vendor_string, "Microsoft")) && (idStr.FindText(glConfig.renderer_string, "OpenGL-D3D") != -1)) {
                    if (cvarSystem.GetCVarBool("r_fullscreen")) {
                        cmdSystem.BufferCommandText(CMD_EXEC_NOW, "vid_restart partial windowed\n");
                        Sys_GrabMouseCursor(false);
                    }
                    //TODO: messageBox below.
//                    int ret = MessageBox(null, "Please install OpenGL drivers from your graphics hardware vendor to run " + GAME_NAME + ".\nYour OpenGL functionality is limited.",
//                            "Insufficient OpenGL capabilities", MB_OKCANCEL | MB_ICONWARNING | MB_TASKMODAL);
//                    if (ret == IDCANCEL) {
//                        cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "quit\n");
//                        cmdSystem.ExecuteCommandBuffer();
//                    }
                    if (cvarSystem.GetCVarBool("r_fullscreen")) {
                        cmdSystem.BufferCommandText(CMD_EXEC_APPEND, "vid_restart\n");
                    }
                }
            }
        }
    }

    /*
     ===============
     R_SetColorMappings
     ===============
     */
    public static void R_SetColorMappings() {
//        int i, j;
        final float g, b;
        double inf;

        b = r_brightness.GetFloat();
        g = r_gamma.GetFloat();

//        for (i = 0; i < 256; i++) {
//            j = (int) (i * b);
//            if (j > 255) {
//                j = 255;
//            }
        if (g == 1) {
            inf = (1 << 8) | 1;
        } else {
            inf = (0xffff * Math.pow(b / 255.0f, 1.0f / g)) + 0.5f;
        }

        if (inf < 0) {
            inf = 0;
        } else if (inf > 0xffff) {
            inf = 0xffff;
        }

//            tr.gammaTable[i] = (short) inf;
//        }
//        GLimp_SetGamma(tr.gammaTable, tr.gammaTable, tr.gammaTable);
//        GLimp_SetGamma((float) inf, 0, 0);//TODO:differentiate between gamma and brightness.//TODO: the original function was rgb.
    }
    /*
     ================== 
     R_BlendedScreenShot

     screenshot
     screenshot [filename]
     screenshot [width] [height]
     screenshot [width] [height] [samples]
     ================== 
     */
    private static final int[] lastNumber = {0};

    public static void R_ScreenShot_f(final idCmdArgs args) {
        final idStr checkName = new idStr();

        int width = glConfig.vidWidth;
        int height = glConfig.vidHeight;
        final int x = 0;
        final int y = 0;
        int blends = 0;

        switch (args.Argc()) {
            case 1:
                width = glConfig.vidWidth;
                height = glConfig.vidHeight;
                blends = 1;
                R_ScreenshotFilename(lastNumber, "screenshots/shot", checkName);
                break;
            case 2:
                width = glConfig.vidWidth;
                height = glConfig.vidHeight;
                blends = 1;
                checkName.oSet(args.Argv(1));
                break;
            case 3:
                width = Integer.parseInt(args.Argv(1));
                height = Integer.parseInt(args.Argv(2));
                blends = 1;
                R_ScreenshotFilename(lastNumber, "screenshots/shot", checkName);
                break;
            case 4:
                width = Integer.parseInt(args.Argv(1));
                height = Integer.parseInt(args.Argv(2));
                blends = Integer.parseInt(args.Argv(3));
                if (blends < 1) {
                    blends = 1;
                }
                if (blends > MAX_BLENDS) {
                    blends = MAX_BLENDS;
                }
                R_ScreenshotFilename(lastNumber, "screenshots/shot", checkName);
                break;
            default:
                common.Printf("usage: screenshot\n       screenshot <filename>\n       screenshot <width> <height>\n       screenshot <width> <height> <blends>\n");
                return;
        }

        // put the console away
        console.Close();

        tr.TakeScreenshot(width, height, checkName.getData(), blends, null);

        common.Printf("Wrote %s\n", checkName.getData());
    }

    /*
     ===============
     R_StencilShot
     Save out a screenshot showing the stencil buffer expanded by 16x range
     ===============
     */
    public static void R_StencilShot() {
        ByteBuffer buffer;
        int i, c;

        final int width = tr.GetScreenWidth();
        final int height = tr.GetScreenHeight();

        final int pix = width * height;

        c = (pix * 3) + 18;
        buffer = ByteBuffer.allocate(c);// Mem_Alloc(c);
//        memset(buffer, 0, 18);
//        buffer = new int[18];//TODO:use c?

        ByteBuffer byteBuffer = ByteBuffer.allocate(pix);// Mem_Alloc(pix);

        qglReadPixels(0, 0, width, height, GL_STENCIL_INDEX, GL_UNSIGNED_BYTE, byteBuffer);

        for (i = 0; i < pix; i++) {
            buffer.put(18 + (i * 3), byteBuffer.get(i));
            buffer.put(18 + (i * 3) + 1, byteBuffer.get(i));
            //		buffer[18+i*3+2] = ( byteBuffer[i] & 15 ) * 16;
            buffer.put(18 + (i * 3) + 2, byteBuffer.get(i));
        }

        // fill in the header (this is vertically flipped, which qglReadPixels emits)
        buffer.put(2, (byte) 2);		// uncompressed type
        buffer.put(12, (byte) (width & 255));//TODO: mayhaps use int[] instead of byte[]?
        buffer.put(13, (byte) (width >> 8));
        buffer.put(14, (byte) (height & 255));
        buffer.put(15, (byte) (height >> 8));
        buffer.put(16, (byte) 24);	// pixel size

        fileSystem.WriteFile("screenshots/stencilShot.tga", buffer, c, "fs_savepath");

//        Mem_Free(buffer);
//        Mem_Free(byteBuffer);
        buffer = null;
        byteBuffer = null;
    }

    /*
     =================
     R_CheckExtension
     =================
     */
    public static boolean R_CheckExtension(final String name) {
        if ((null == glConfig.extensions_string)
                || !glConfig.extensions_string.contains(name)) {
            common.Printf("X..%s not found\n", name);
            return false;
        }

        common.Printf("...using %s\n", name);
        return true;
    }

    /* 
     ============================================================================== 
 
     SCREEN SHOTS 
 
     ============================================================================== 
     */

    /*
     ====================
     R_ReadTiledPixels

     Allows the rendering of an image larger than the actual window by
     tiling it into window-sized chunks and rendering each chunk separately

     If ref isn't specified, the full session UpdateScreen will be done.
     ====================
     */
    static void R_ReadTiledPixels(int width, int height, byte[] buffer, final int offset, renderView_s ref /*= NULL*/) {
        // include extra space for OpenGL padding to word boundaries
        byte[] temp = new byte[(glConfig.vidWidth + 3) * glConfig.vidHeight * 3];//R_StaticAlloc( (glConfig.vidWidth+3) * glConfig.vidHeight * 3 );

        final int oldWidth = glConfig.vidWidth;
        final int oldHeight = glConfig.vidHeight;

        tr.tiledViewport[0] = width;
        tr.tiledViewport[1] = height;

        // disable scissor, so we don't need to adjust all those rects
        r_useScissor.SetBool(false);

        for (int xo = 0; xo < width; xo += oldWidth) {
            for (int yo = 0; yo < height; yo += oldHeight) {
                tr.viewportOffset[0] = -xo;
                tr.viewportOffset[1] = -yo;

                if (ref != null) {
                    tr.BeginFrame(oldWidth, oldHeight);
                    tr.primaryWorld.RenderScene(ref);
                    tr.EndFrame(null, null);
                } else {
                    session.UpdateScreen();
                }

                int w = oldWidth;
                if ((xo + w) > width) {
                    w = width - xo;
                }
                int h = oldHeight;
                if ((yo + h) > height) {
                    h = height - yo;
                }

                qglReadBuffer(GL_FRONT);
                qglReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, ByteBuffer.wrap(temp));

                final int row = ((w * 3) + 3) & ~3;		// OpenGL pads to dword boundaries

                for (int y = 0; y < h; y++) {
//				memcpy( buffer + ( ( yo + y )* width + xo ) * 3,
//					temp + y * row, w * 3 );
                    System.arraycopy(temp, y * row, buffer, offset + ((((yo + y) * width) + xo) * 3), w * 3);
                }
            }
        }

        r_useScissor.SetBool(true);

        tr.viewportOffset[0] = 0;
        tr.viewportOffset[1] = 0;
        tr.tiledViewport[0] = 0;
        tr.tiledViewport[1] = 0;

//	R_StaticFree( temp );
        temp = null;

        glConfig.vidWidth = oldWidth;
        glConfig.vidHeight = oldHeight;
    }

}
